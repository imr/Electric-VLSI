/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BTree.java
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.database.geometry.btree;

import java.io.*;
import java.util.*;
import com.sun.electric.database.geometry.btree.unboxed.*;
import com.sun.electric.database.geometry.btree.CachingPageStorage.CachedPage;

/**
 *  A B+Tree implemented using PageStorage.
 *
 *  http://www.youtube.com/watch?v=coRJrcIYbF4
 *
 *  This is a B-Plus-Tree; values are stored only in leaf nodes.
 *
 *  Each element in a BTree is conceptually a triple:
 *
 *      < ordinal, key, value >
 *
 *  Where "key" is a user-supplied key (belonging to a type that is
 *  Comparable), "value" is a user-supplied value (no restrictions)
 *  and "ordinal" is an integer indicating the number of keys in the
 *  tree less than this one.  Note that the ordinal is not actually
 *  stored on disk, and inserting a new value can potentially modify
 *  the ordinals of all preexisting elements!  Each of the getXXX()
 *  methods takes one of these three coordinates (ordinal, key, or
 *  value) and returns one of the others.  Additionally, the
 *  getXXXFromKey() methods include floor/ceiling versions that take
 *  an upper/lower bound and search for the largest/smallest key which
 *  is less/greater than the one supplied.
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
 *  Possible feature: use extents rather than blocks?
 *
 *  Possible feature: delete-range
 *
 *  Possible feature: two btrees-of-extents, one indexed by starting
 *  offset, one indexed by size.  Lets you very efficiently respond to
 *  allocation requests of varying sizes.
 *
 *  Sub-block allocation using extents.
 *
 *  Crazy: you can even store the free extents in the very tree that
 *  uses those extents as its underlying storage!  The trick here is
 *  that performing an extent allocation merely reduces the length of
 *  some existing free extent (and perhaps also advances it), but does
 *  not change the number of free extents (it might reduce that number
 *  by one, unless you add a "I am not really free" bit to the free
 *  extents).  This means that you can always perform an allocation
 *  without modifying the structure of the btree, which might in turn
 *  require an allocation.  So allocations are never circular.  If you
 *  bootstrap all of this by initializing a tree that contains one big
 *  extent, you're all set.
 *
 *    So you have one big massive tree with three kinds of entries:
 *
 *       - Actual items
 *       - Free extent, indexed by starting offset
 *       - Free extent, indexed by length
 *
 *  @author Adam Megacz <adam.megacz@sun.com>
 */
public class BTree
    <K extends Serializable & Comparable,
     V extends Serializable,
     S extends Serializable> {

    final CachingPageStorage   ps;
    final UnboxedComparable<K> uk;
    final UnboxedMonoid<K,V,S> monoid;    // XXX: would be nice if the monoid had acesss to the K-value too...
    final Unboxed<V>           uv;
    final UnboxedInt           ui = UnboxedInt.instance;

    private LeafNodeCursor<K,V,S>       leafNodeCursor;
    private InteriorNodeCursor<K,V,S>   interiorNodeCursor1;
    private InteriorNodeCursor<K,V,S>   interiorNodeCursor2;

    int                  rootpage;
    private final byte[] keybuf;

    private final byte[] largestKey;
    private       int    largestKeyPage = -1;  // or -1 if unknown

    public BTree(CachingPageStorage ps, UnboxedComparable<K> uk, UnboxedMonoid<K,V,S> monoid, Unboxed<V> uv) {
        if (monoid!=null) {
            if (!(monoid instanceof UnboxedCommutativeMonoid))
                throw new RuntimeException("Only commutative monoids are supported (allows one-pass insertion)");
            // FIXME: if the monoid is not invertible (ie a group) and commutative, we cannot do DELETE in a single pass
            // I don't think we can ever do REPLACE in one pass unless we knew the previous value
        }
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
        leafNodeCursor.initBuf(ps.getPage(rootpage, false), true);
        leafNodeCursor.writeBack();
    }

    private int size = 0;
    public int  size() {
        return size;
    }

    /** returns the value in the tree, or null if not found */
    public V getValFromKey(K key) {
        uk.serialize(key, keybuf, 0);
        return (V)walk(keybuf, 0, null, Op.GET_VAL_FROM_KEY, 0);
    }

    /** returns the value of the largest key less than or equal to the one supplied */
    public V getValFromKeyFloor(K key) {
        uk.serialize(key, keybuf, 0);
        return (V)walk(keybuf, 0, null, Op.GET_VAL_FROM_KEY_FLOOR, 0);
    }

    /** returns the value of the smallest key greater than or equal to the one supplied */
    public V getValFromKeyCeiling(K key) {
        uk.serialize(key, keybuf, 0);
        return (V)walk(keybuf, 0, null, Op.GET_VAL_FROM_KEY_CEIL, 0);
    }

    /** returns the ordinal of the given key, or -1 if not found */
    public int getOrdFromKey(K key) {
        uk.serialize(key, keybuf, 0);
        return ((Integer)walk(keybuf, 0, null, Op.GET_ORD_FROM_KEY, 0)).intValue();
    }

    /** returns the ordinal of the largest key less than or equal to the one supplied */
    public int getOrdFromKeyFloor(K key) {
        uk.serialize(key, keybuf, 0);
        return ((Integer)walk(keybuf, 0, null, Op.GET_ORD_FROM_KEY_FLOOR, 0)).intValue();
    }

    /** returns the ordinal of the smallest key greater than or equal to the one supplied */
    public int getOrdFromKeyCeiling(K key) {
        uk.serialize(key, keybuf, 0);
        return ((Integer)walk(keybuf, 0, null, Op.GET_ORD_FROM_KEY_CEIL, 0)).intValue();
    }

    /** returns the least key greater than  */
    public V getNext(K key) {
        throw new RuntimeException("not implemented");
    }

    public V getPrev(K key) {
        throw new RuntimeException("not implemented");
    }

    /** returns the i^th value in the tree */
    public V getValFromOrd(int ord) {
        return (V)walk(null, 0, null, Op.GET_VAL_FROM_ORD, ord);
    }

    /** returns the i^th key in the tree */
    public K getKeyFromOrd(int ord) {
        return (K)walk(null, 0, null, Op.GET_KEY_FROM_ORD, ord);
    }

    /** will throw an exception if the key is already in the tree */
    public void insert(K key, V val) {
        uk.serialize(key, keybuf, 0);
        walk(keybuf, 0, val, Op.INSERT, 0);
        size++;
    }

    /** returns value previously in the tree; will throw an exception if the key is not already in the tree */
    public V remove(K key) {
        throw new RuntimeException("not implemented");
        // size--;
    }

    /** returns value previously in the tree; will throw an exception if the key is not already in the tree */
    public V replace(K key, V val) {
        uk.serialize(key, keybuf, 0);
        return (V)walk(keybuf, 0, val, Op.REPLACE, 0);
    }
    
    private static enum Op {
        GET_VAL_FROM_KEY,
        GET_VAL_FROM_KEY_FLOOR,
        GET_VAL_FROM_KEY_CEIL,
        GET_ORD_FROM_KEY,
        GET_ORD_FROM_KEY_FLOOR,
        GET_ORD_FROM_KEY_CEIL,
        GET_VAL_FROM_ORD,
        GET_KEY_FROM_ORD,
        GET_NEXT,
        GET_PREV,
        REMOVE,
        INSERT,
        REPLACE;
        public boolean isGetFromOrd() {
            switch(this) {
                case GET_VAL_FROM_ORD:
                case GET_KEY_FROM_ORD:
                    return true;
                default:
                    return false;
            }
        }
        public boolean isGetOrd() {
            switch(this) {
                case GET_ORD_FROM_KEY:
                case GET_ORD_FROM_KEY_FLOOR:
                case GET_ORD_FROM_KEY_CEIL:
                    return true;
                default:
                    return false;
            }
        }
        public boolean isGetFromKey() {
            switch(this) {
                case GET_VAL_FROM_KEY:
                case GET_VAL_FROM_KEY_FLOOR:
                case GET_VAL_FROM_KEY_CEIL:
                case GET_ORD_FROM_KEY:
                case GET_ORD_FROM_KEY_FLOOR:
                case GET_ORD_FROM_KEY_CEIL:
                    return true;
                default:
                    return false;
            }
        }
        public boolean isGetFromKeyFloor() {
            switch(this) {
                case GET_VAL_FROM_KEY_FLOOR:
                case GET_ORD_FROM_KEY_FLOOR:
                    return true;
                default:
                    return false;
            }
        }
        public boolean isGetFromKeyCeil() {
            switch(this) {
                case GET_VAL_FROM_KEY_CEIL:
                case GET_ORD_FROM_KEY_CEIL:
                    return true;
                default:
                    return false;
            }
        }
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
    private Object walk(byte[] key, int key_ofs, V val, Op op, int ord) {
        int pageid = rootpage;
        int idx = -1;
        int global_ord = 0;

        LeafNodeCursor<K,V,S>       leafNodeCursor = this.leafNodeCursor;
        InteriorNodeCursor<K,V,S>   interiorNodeCursor = this.interiorNodeCursor1;
        InteriorNodeCursor<K,V,S>   parentNodeCursor = this.interiorNodeCursor2;
        NodeCursor cur = null;

        boolean rightEdge = true;
        boolean cheat = false;
        int comp = 0;

        if (largestKeyPage != -1 && op==Op.INSERT) {
            leafNodeCursor.setBuf(ps.getPage(largestKeyPage, true));
            comp = uk.compare(key, key_ofs, largestKey, 0);
            if (comp >= 0 && !leafNodeCursor.isFull()) {
                pageid = largestKeyPage;
                parentNodeCursor.forgetCachedPage();
                cheat = true;
                cur = leafNodeCursor;
            }
        }

        while(true) {
            if (cur==null || cur.getCachedPage()==null || cur.getPageId() != pageid) {
                CachedPage cp = ps.getPage(pageid, true);
                cur = LeafNodeCursor.isLeafNode(cp) ? leafNodeCursor : interiorNodeCursor;
                cur.setBuf(cp);
            }

            if ((op==Op.INSERT || op==Op.REPLACE) && cur.isFull()) {
                assert cur!=parentNodeCursor;
                int old;

                // is the node we're splitting the last child of its parent or the root node?
                boolean splitting_last_or_root = false;
                if (pageid == rootpage) {
                    parentNodeCursor.initRoot();
                    parentNodeCursor.setBucketPageId(0, pageid);
                    idx = 0;
                    old = size;
                    splitting_last_or_root = true;
                } else {
                    assert !parentNodeCursor.isFull();
                    splitting_last_or_root = idx>=parentNodeCursor.getNumBuckets()-1;
                    old = splitting_last_or_root ? -1 : parentNodeCursor.getNumValsBelowBucket(idx);
                }
                if (op==Op.INSERT && old!=-1) old -= 1;
                int ofs = parentNodeCursor.insertNewBucketAt(idx+1);
                int oldpage = cur.getPageId();

                // optimization: if we're splitting a node on the
                // "right edge" of the tree, make the split uneven --
                // put everything on the left side.

                int splitPoint = rightEdge ? cur.getNumBuckets()-1 : cur.getMaxBuckets()/2;
                if (rightEdge) splitUnEven++; else splitEven++;

                if (monoid!=null) {
                    parentNodeCursor.clearMonoid(idx);
                    byte[] monbuf = new byte[monoid.getSize()];
                    for(int i=0; i<splitPoint; i++) {
                        cur.getMonoid(i, monbuf, 0);
                        parentNodeCursor.multiplyMonoidCommutative(idx, monbuf, 0);
                    }
                }
                int num = cur.split(parentNodeCursor.getBuf(), ofs, splitPoint);
                parentNodeCursor.setNumValsBelowBucket(idx, num);
                int newpage = cur.getPageId();
                if (largestKeyPage==oldpage) largestKeyPage = newpage;
                parentNodeCursor.setBucketPageId(idx+1, newpage);
                if (!splitting_last_or_root)
                    parentNodeCursor.setNumValsBelowBucket(idx+1, old-num);
                if (monoid!=null && (!parentNodeCursor.isRightMost() || idx+1<parentNodeCursor.getNumBuckets()-1)) {
                    parentNodeCursor.clearMonoid(idx+1);
                    byte[] monbuf = new byte[monoid.getSize()];
                    for(int i=0; i<cur.getNumBuckets() - (cur.isRightMost() ? 1 : 0); i++) {
                        cur.getMonoid(i, monbuf, 0);
                        parentNodeCursor.multiplyMonoidCommutative(idx+1, monbuf, 0);
                    }
                }

                cur.writeBack();
                parentNodeCursor.writeBack();
                pageid = rootpage;
                cheat = false;
                continue;
            }


            if (cheat) {
                idx = leafNodeCursor.getNumBuckets()-1;
                comp = 1;
            } else if (!op.isGetFromOrd()) {
                idx = cur.search(key, key_ofs);
                comp = cur.compare(key, key_ofs, idx);
            } else if (!cur.isLeafNode()) {
                // FIXME: linear scan => bad
                for(idx = 0; idx < interiorNodeCursor.getNumBuckets()-1; idx++) {
                    int k = interiorNodeCursor.getNumValsBelowBucket(idx);
                    if (ord < k) break;
                    ord -= k;
                }
            }
            if (cur.isLeafNode()) {
                switch(op) {
                    case GET_VAL_FROM_ORD:       return ord >= leafNodeCursor.getNumBuckets() ? null : leafNodeCursor.getVal(ord);
                    case GET_KEY_FROM_ORD:       return ord >= leafNodeCursor.getNumBuckets() ? null : leafNodeCursor.getKey(ord);
                    case GET_VAL_FROM_KEY:       return comp==0 ? leafNodeCursor.getVal(idx) : null;
                    case GET_VAL_FROM_KEY_FLOOR: return leafNodeCursor.getVal(idx);
                    case GET_VAL_FROM_KEY_CEIL:  /* FIXME: might need to backtrack one step */ throw new RuntimeException("not implemented");
                    case GET_ORD_FROM_KEY:       return comp==0 ? new Integer(idx+global_ord) : new Integer(-1);
                    case GET_ORD_FROM_KEY_FLOOR: return new Integer(idx+global_ord /*FIXME: off the end?*/);
                    case GET_ORD_FROM_KEY_CEIL:  return comp==0 ? new Integer(idx+global_ord) : new Integer(idx+global_ord+1 /*FIXME: off the end?*/);
                    default: /* INSERT or REPLACE; fall through */
                }
                if (op==Op.INSERT && comp==0) throw new RuntimeException("attempt to re-insert a value at key " + leafNodeCursor.getKey(idx));
                if (op==Op.REPLACE && comp!=0) throw new RuntimeException("attempt to replace a value that did not exist");
                if (op==Op.INSERT) { if (cheat) insertionFastPath++; else insertionSlowPath++; }
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
                if (op==Op.REMOVE)
                    throw new RuntimeException("need to adjust 'least value under X' on the way down for deletions");
                if (op==Op.INSERT) {
                    boolean wb = false;
                    if (idx < interiorNodeCursor.getNumBuckets()-1) {
                        interiorNodeCursor.setNumValsBelowBucket(idx, interiorNodeCursor.getNumValsBelowBucket(idx)+1);
                        wb = true;
                    }
                    if (monoid != null && (idx < interiorNodeCursor.getNumBuckets()-1 || !interiorNodeCursor.isRightMost())) {
                        // ugly..
                        byte[] monbuf = new byte[monoid.getSize()];
                        byte[] vbuf = new byte[uv.getSize()];
                        uv.serialize(val, vbuf, 0);
                        monoid.inject(key, key_ofs, vbuf, 0, monbuf, 0);
                        interiorNodeCursor.multiplyMonoidCommutative(idx, monbuf, 0);
                    }
                    if (wb) interiorNodeCursor.writeBack();
                }
                if (op.isGetOrd())
                    for(int i = 0; i < idx; i++)
                        global_ord += interiorNodeCursor.getNumValsBelowBucket(i);
                rightEdge &= idx==interiorNodeCursor.getNumBuckets()-1;
                pageid = interiorNodeCursor.getBucketPageId(idx);
                InteriorNodeCursor<K,V,S> ic = interiorNodeCursor; interiorNodeCursor = parentNodeCursor; parentNodeCursor = ic;
                assert interiorNodeCursor!=parentNodeCursor;
                continue;
            }
        }
    }

    public static long insertionFastPath = 0;
    public static long insertionSlowPath = 0;
    public static long splitEven = 0;
    public static long splitUnEven = 0;

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
        CachingPageStorage ps = new CachingPageStorageWrapper(FilePageStorage.create(), cachesize, false);
        BTree<Integer,Integer,Pair<Integer,Integer>> btree =
            new BTree<Integer,Integer,Pair<Integer,Integer>>(ps, UnboxedInt.instance,
                                                             UnboxedMinMaxInteger.instance,
                                                             UnboxedInt.instance);
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
            switch(rand.nextInt() % 3) {
                case 0: { // get
                    Integer tget = do_tm ? tm.get(key) : null;
                    Integer bget = do_bt ? btree.getValFromKey(key) : null;
                    gets++;
                    if (do_tm && do_bt) {
                        if (tget==null && bget==null) { misses++; break; }
                        if (tget!=null && bget!=null && tget.equals(bget)) break;
                        System.out.print("\r puts="+puts+" gets="+(gets-misses)+"/"+gets+" deletes="+deletes);
                        System.out.println();
                        System.out.println();
                        throw new RuntimeException("  disagreement on key " + key + ": btree="+bget+", treemap="+tget);
                    }
                    break;
                }

                case 1: { // get ordinal
                    int sz = do_bt ? btree.size() : tm.size();
                    int ord = sz==0 ? 0 : Math.abs(rand.nextInt()) % sz;
                    Integer tget = do_tm ? (sz==0 ? null : tm.values().toArray(new Integer[0])[ord]) : null;
                    Integer bget = do_bt ? btree.getValFromOrd(ord) : null;
                    gets++;
                    if (do_tm && do_bt) {
                        if (tget==null && bget==null) break;
                        if (tget!=null && bget!=null && tget.equals(bget)) break;
                        System.out.print("\r puts="+puts+" gets="+(gets-misses)+"/"+gets+" deletes="+deletes);
                        System.out.println();
                        System.out.println();
                        System.out.println("dump:");
                        throw new RuntimeException("  disagreement on ordinal " + ord + ": btree="+bget+", treemap="+tget);
                    }
                    break;
                }

                case 2: { // put
                    int val = rand.nextInt();
                    boolean already_there = false;
                    if (do_tm) {
                        if (do_bt) already_there = tm.get(key)!=null;
                        tm.put(key, val);
                    }
                    if (do_bt) {
                        if (!do_tm) already_there = btree.getValFromKey(key)!=null;
                        if (already_there)
                            btree.replace(key, val);
                        else
                            btree.insert(key, val);
                    }
                    puts++;
                    break;
                }

                case 3: // delete
                    deletes++;
                    break;
            }
        }
    }

}