/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BTree.java
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.btree;

import java.io.*;
import java.util.*;
import com.sun.electric.tool.btree.unboxed.*;

/**
 *  A B+Tree implemented using PageStorage.
 *
 *  http://www.youtube.com/watch?v=coRJrcIYbF4
 *
 *  This is a B-Plus-Tree; values are stored only in leaf nodes.
 *
 *  A value drawn from a monoid is associated with each node of the
 *  tree.  An interior node's value is the monoid-product of its
 *  childrens' values.  A query method is provided to return the
 *  monoid product of arbitrary contiguous ranges within the tree.
 *  This can be used to efficiently implement "min/max value over this
 *  range" queries.
 *
 *  We proactively split nodes as soon as they become full rather than
 *  waiting for them to become overfull.  This has a space overhead of
 *  1/NUM_KEYS_PER_PAGE, but puts an O(1) bound on the number of pages
 *  written per operation (number of pages read is still O(log n)).
 *  It also makes the walk routine tail-recursive.
 *
 *  Each node of the BTree uses one page of the PageStorage; we don't
 *  yet support situations where a single page is too small for one
 *  key and one value.
 *
 *  You must distinguish between insert() and replace() ahead of time;
 *  you can't call insert() on a key that is already in the tree or
 *  replace() on one that isn't.  This lets us update the interior
 *  node invariants as we walk down the tree and avoid having to walk
 *  back up afterwards.  If you're not sure if a key is in the tree,
 *  just do a get() -- the net result is two passes over the tree,
 *  which is what we'd have to do anyways if the user didn't
 *  distinguish insert() from replace().  We're just offering the option
 *  to double the performance in the case where the user already knows
 *  if the key is in the tree or not.
 *
 *  The coding style in this file is pretty unusual; it looks a lot
 *  like "Java as a better C".  This is mainly because Hotspot is
 *  remarkably good at inlining methods, but remarkably bad (still,
 *  even in Java 1.7) at figuring out when it's safe to unbox values.
 *  So I expend a lot of effort trying not to create boxed values, but
 *  don't worry at all about the overhead of method calls,
 *  particularly when made via "final" references (nearly always
 *  inlined).  I don't code this way very often -- I reserve this for
 *  the 2% of my code that bears 98% of the performance burden.
 *
 *  Possible feature: sibling-stealing
 *      http://en.wikipedia.org/wiki/B_sharp_tree
 *
 *  Possible feature: COWTree like Oracle's btrfs:
 *      http://dx.doi.org/10.1145/1326542.1326544
 *
 *  @author Adam Megacz <adam.megacz@sun.com>
 */
public class BTree
    <K extends Serializable & Comparable,
     V extends Serializable,
     S extends Serializable> {

    final PageStorage          ps;
    final UnboxedComparable<K> uk;
    final UnboxedMonoid<S>     monoid;    // XXX: would be nice if the monoid had acesss to the K-value too...
    final Unboxed<V>           uv;
    final UnboxedInt           ui = UnboxedInt.instance;

    private LeafNodeCursor<K,V,S>       leafNodeCursor;
    private InteriorNodeCursor<K,V,S>   interiorNodeCursor1;
    private InteriorNodeCursor<K,V,S>   interiorNodeCursor2;

    int                  rootpage;
    private final byte[] keybuf;

    private final byte[] largestKey;
    private       int    largestKeyPage = -1;  // or -1 if unknown

    public BTree(PageStorage ps, UnboxedComparable<K> uk, UnboxedMonoid<S> monoid, Unboxed<V> uv) {
        this.ps = ps;
        this.uk = uk;
        this.monoid = monoid;
        this.uv = uv;
        this.leafNodeCursor = new LeafNodeCursor<K,V,S>(this);
        this.interiorNodeCursor1 = new InteriorNodeCursor<K,V,S>(this);
        this.interiorNodeCursor2 = new InteriorNodeCursor<K,V,S>(this);
        this.rootpage = ps.createPage();
        this.keybuf = new byte[uk.getSize()];
        this.largestKey = new byte[uk.getSize()];
        leafNodeCursor.initBuf(rootpage, new byte[ps.getPageSize()]);
        leafNodeCursor.setParentPageId(rootpage);
        leafNodeCursor.writeBack();
    }

    private int size = 0;
    public int  size() {
        return size;
    }

    /** returns the value in the tree, or null if not found */
    public V get(K key) {
        uk.serialize(key, keybuf, 0);
        return walk(keybuf, 0, null, Op.GET);
    }

    /** returns the least key greater than  */
    public V getNext(K key) {
        throw new RuntimeException("not implemented");
    }

    public V getPrev(K key) {
        throw new RuntimeException("not implemented");
    }

    /** returns the i^th key in the tree */
    /*
    public K get(int i) {
        throw new RuntimeException("not implemented");
    }
    */

    /** will throw an exception if the key is already in the tree */
    public void insert(K key, V val) {
        uk.serialize(key, keybuf, 0);
        walk(keybuf, 0, val, Op.INSERT);
        size++;
    }

    /** returns value previously in the tree; will throw an exception if the key is not already in the tree */
    public V remove(K key) {
        throw new RuntimeException("not implemented");
    }

    /** returns value previously in the tree; will throw an exception if the key is not already in the tree */
    public V replace(K key, V val) {
        uk.serialize(key, keybuf, 0);
        return walk(keybuf, 0, val, Op.REPLACE);
    }
    
    private static enum Op {
        GET, GET_NEXT, GET_PREV, REMOVE, INSERT, REPLACE
    }
    

    /**
     *  B+Tree walking routine.
     *
     *  This is the hairiest part, so I arranged things to share a single
     *  codepath across all four operations (insert/replace/delete/find).
     *
     *  The routine is implemented using a loop rather than recursive
     *  calls because the JVM does not support tail recursion (and
     *  probably never will, because its lame security model is based
     *  on stack inspection).
     *
     *  On writes/deletes, this returns the previous value.
     *
     */
    private V walk(byte[] key, int key_ofs, V val, Op op) {
        int pageid = rootpage;
        int idx = -1;

        LeafNodeCursor<K,V,S>       leafNodeCursor = this.leafNodeCursor;
        InteriorNodeCursor<K,V,S>   interiorNodeCursor = this.interiorNodeCursor1;
        InteriorNodeCursor<K,V,S>   parentNodeCursor = this.interiorNodeCursor2;
        NodeCursor cur = null;

        boolean cheat = false;
        int comp = 0;

        if (largestKeyPage != -1 && op==Op.INSERT) {
            byte[] buf = ps.readPage(largestKeyPage, null, 0);
            leafNodeCursor.setBuf(largestKeyPage, buf);
            comp = uk.compare(key, key_ofs, largestKey, 0);
            if (comp >= 0 && !leafNodeCursor.isFull()) {
                pageid = largestKeyPage;
                buf = ps.readPage(leafNodeCursor.getParentPageId(), null, 0);
                if (leafNodeCursor.getParentPageId()!=leafNodeCursor.getPageId())
                    parentNodeCursor.setBuf(leafNodeCursor.getParentPageId(), buf);
                cheat = true;
            }
        }

        while(true) {
            byte[] buf = ps.readPage(pageid, null, 0);
            cur = LeafNodeCursor.isLeafNode(buf) ? leafNodeCursor : interiorNodeCursor;
            cur.setBuf(pageid, buf);

            if ((op==Op.INSERT || op==Op.REPLACE) && cur.isFull()) {
                assert cur!=parentNodeCursor;
                if (pageid == rootpage) {
                    parentNodeCursor.initRoot(new byte[ps.getPageSize()]);
                    parentNodeCursor.setChildPageId(0, pageid);
                    cur.setParentPageId(rootpage);
                    idx = 0;
                }
                int ofs = parentNodeCursor.insertNewChildAt(idx+1);
                int oldpage = cur.getPageId();
                int newpage = cur.split(parentNodeCursor.getBuf(), ofs);
                if (largestKeyPage==oldpage) largestKeyPage = newpage;
                parentNodeCursor.setChildPageId(idx+1, newpage);
                cur.writeBack();
                parentNodeCursor.writeBack();
                pageid = rootpage;
                cheat = false;
                continue;
            }

            if (cheat) {
                idx = leafNodeCursor.getNumEntries()-1;
            } else {
                idx = cur.search(key, key_ofs);
                comp = cur.compare(key, key_ofs, idx);
            }
            if (cur.isLeafNode()) {
                if (op==Op.GET) return comp==0 ? leafNodeCursor.getVal(idx) : null;
                if (op==Op.INSERT && comp==0) throw new RuntimeException("attempt to re-insert a value");
                if (op==Op.REPLACE && comp!=0) throw new RuntimeException("attempt to replace a value that did not exist");
                if (cheat) hits++; else misses++;
                if (largestKeyPage==-1 || cheat)
                    System.arraycopy(key, key_ofs, largestKey, 0, largestKey.length);
                if (largestKeyPage==-1) largestKeyPage = pageid;
                if (comp==0) {
                    if (val==null) throw new RuntimeException("deletion is not yet implemented");
                    return leafNodeCursor.setVal(idx, val);
                }
                leafNodeCursor.insertVal(idx+1, key, key_ofs, val);
                return null;
            } else {
                if (op!=Op.GET && val==null)
                    throw new RuntimeException("need to adjust 'least value under X' on the way down for deletions");
                pageid = interiorNodeCursor.getChildPageId(idx);
                InteriorNodeCursor<K,V,S> ic = interiorNodeCursor; interiorNodeCursor = parentNodeCursor; parentNodeCursor = ic;
                assert interiorNodeCursor!=parentNodeCursor;
                continue;
            }
        }
    }

    public static int hits = 0;
    public static int misses = 0;

    //////////////////////////////////////////////////////////////////////////////

    /** remove all entries */                                              public void clear() { throw new RuntimeException("not implemented"); }


    /** returns the number of keys strictly after the one given */         public int  sizeAfter(K key) { throw new RuntimeException("not implemented"); }
    /** returns the number of keys strictly after the one given */         public int  sizeBefore(K key) { throw new RuntimeException("not implemented"); }

    /** returns the key with the given ordinal index */                    public K    seek(int ordinal) { throw new RuntimeException("not implemented"); }
    /** returns the ordinal index of the given key */                      public int  ordinal(K key) { throw new RuntimeException("not implemented"); }

    //////////////////////////////////////////////////////////////////////////////

    /**
     *  A simple regression test
     *
     *  Feature: test the case where we delete all the entries; there
     *  are a lot of corner cases there.
     */
    public static void main(String[] s) throws Exception {
        if (s.length != 4) {
            System.err.println("");
            System.err.println("usage: java " + BTree.class.getName() + " <maxsize> <numops> <cachesize> <seed>");
            System.err.println("");
            System.err.println("  Creates a BTree and runs random operations on both it and an in-memory TreeMap.");
            System.err.println("  Reports any disagreements.");
            System.err.println("");
            System.err.println("    <maxsize>   maximum number of entries in the tree, or 0 for no limit");
            System.err.println("    <numops>    number of operations to perform, or 0 for no limit");
            System.err.println("    <cachesize> number of pages to cache in memory, or 0 for no cache");
            System.err.println("    <seed>      seed for random number generator, in hex");
            System.err.println("");
            System.exit(-1);
        }
        Random rand = new Random(Integer.parseInt(s[3], 16));
        int cachesize = Integer.parseInt(s[2]);
        int numops = Integer.parseInt(s[1]);
        int maxsize = Integer.parseInt(s[0]);
        int size = 0;
        PageStorage ps = cachesize==0
            ? FilePageStorage.create()
            : new CachingPageStorage(FilePageStorage.create(), cachesize);
        BTree<Integer,Integer,Integer> btree =
            new BTree<Integer,Integer,Integer>(ps, UnboxedInt.instance, null, UnboxedInt.instance);
        TreeMap<Integer,Integer> tm = 
            new TreeMap<Integer,Integer>();

        int puts=0, gets=0, deletes=0, misses=0, inserts=0;
        long lastprint=0;

        // you can switch one of these off to gather crude performance measurements and compare them to TreeMap
        boolean do_tm = true;
        boolean do_bt = true;

        for(int i=0; numops==0 || i<numops; i++) {
            if (System.currentTimeMillis()-lastprint > 200) {
                lastprint = System.currentTimeMillis();
                System.out.print("\r puts="+puts+" gets="+(gets-misses)+"/"+gets+" deletes="+deletes);
            }
            int key = rand.nextInt() % 1000000;
            switch(rand.nextInt() % 2) {
                case 0: { // get
                    Integer tget = do_tm ? tm.get(key) : null;
                    Integer bget = do_bt ? btree.get(key) : null;
                    gets++;
                    if (do_tm && do_bt) {
                        if (tget==null && bget==null) { misses++; break; }
                        if (tget!=null && bget!=null && tget.equals(bget)) break;
                        System.out.print("\r puts="+puts+" gets="+(gets-misses)+"/"+gets+" deletes="+deletes);
                        System.out.println();
                        System.out.println();
                        throw new RuntimeException("  disagreement on key " + key + ": btree="+bget+", treemap="+tget);
                    }
                }

                case 1: { // put
                    int val = rand.nextInt();
                    boolean already_there = false;
                    if (do_tm) {
                        if (do_bt) already_there = tm.get(key)!=null;
                        tm.put(key, val);
                    }
                    if (do_bt) {
                        if (!do_tm) already_there = btree.get(key)!=null;
                        if (already_there)
                            btree.replace(key, val);
                        else
                            btree.insert(key, val);
                    }
                    puts++;
                    break;
                }

                case 2: // delete
                    deletes++;
                    break;
            }
        }
    }

}