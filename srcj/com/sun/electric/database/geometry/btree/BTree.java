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
package com.sun.electric.database.geometry.btree;

import java.io.*;
import java.util.*;
import com.sun.electric.database.geometry.btree.unboxed.*;
import com.sun.electric.database.geometry.btree.CachingPageStorage.CachedPage;

/**
 *  A <a href=http://www.youtube.com/watch?v=coRJrcIYbF4>B+Tree</a>
 *  implemented using {@see PageStorage}.<p>
 *
 *  This is a B-Plus-Tree; values are stored only in leaf nodes.<p>
 *
 *  <h3>Usage Notes</h3>
 *
 *  Each element in a BTree is conceptually a triple
 *  &lt;ordinal,key,value&gt; where "key" is a user-supplied key
 *  (belonging to a type that is {@see Comparable}), "value" is a
 *  user-supplied value (no restrictions) and "ordinal" is an integer
 *  indicating the number of keys in the tree less than this one.
 *  Note that the ordinal is not actually stored in the tree, and
 *  inserting a new value can potentially modify the ordinals of all
 *  preexisting elements!  Each of the getXXX() methods takes one of
 *  these three coordinates (<tt>Ord</tt>, <tt>Key</tt>, or
 *  <tt>Val</tt>) and returns one of the others, or else a count
 *  (<tt>Num</tt>).  Additionally, the getXXXFromKey() methods include
 *  floor/ceiling versions that take an upper/lower bound and search
 *  for the largest/smallest key which is less/greater than the one
 *  supplied.<p>
 *
 *  The BTree supports appending a new element (that is, inserting a value
 *  with a key greater than any key in the table) in near-constant time
 *  (Actually log<sup>*</sup>(n)) and all other queries in log(n) time.
 *  All operations are done in a <i>single pass</i> down the BTree from
 *  the root to the leaves; this brings two benefits: the data structure
 *  can be made concurrent with very little lock contention and it can
 *  support copy-on-write shadow versions.<p>
 *  
 *  You must distinguish between insert() and replace() ahead of time;
 *  you can't call insert() on a key that is already in the tree or
 *  replace() on one that isn't.  In order to replace() on a BTree
 *  with a summary the summary product operation must be commutative
 *  and invertible, and you must know the old value which you are
 *  replacing.  This lets us update the interior node invariants as we
 *  walk down the tree and avoid having to walk back up afterwards.
 *  If you're not sure if a key is in the tree, just do a get() -- the
 *  net result is two passes over the tree, which is what we'd have to
 *  do anyways if the user didn't distinguish insert() from replace().
 *  We're just offering the option to double the performance in the
 *  case where the user already knows if the key is in the tree or
 *  not.<p>
 *
 *  You can associate a <i>summary</i> with each leaf node of the BTree.
 *  In order to do this, you must provide an instance of {@see
 *  com.sun.electric.database.geometry.btree.unboxed.AssociativeOperation}
 *  to the BTree when you construct it.  The AssociativeOperation knows
 *  the key and value type of the BTree, and must know two things:
 *  
 *  <ul>
 *    <li> How to calculate the summary of a single (key,value) pair.
 *    <li> How to merge two summaries.
 *  </ul>
 *  
 *  The process of merging two summaries must be associative; if we want
 *  to merge three summaries (ABC) it must not matter if we merge them as
 *  ((AB)C) or (A(BC)).  Technically this makes the merge operation
 *  a <i><a href=http://en.wikipedia.org/wiki/Semigroup>semigroup</a></i>.
 *  In exchange for providing all of this information
 *  you can ask the BTree to calculate the summary of any contiguous
 *  region of the keyspace in log(n) time.  For example, this can be used
 *  to answer "min/max over this range" queries very efficiently.<p>
 *
 *  <h3>Implementation Notes</h3>
 *
 *  We proactively split nodes as soon as they become full rather than
 *  waiting for them to become overfull.  This has a space overhead of
 *  1/NUM_KEYS_PER_PAGE, but puts an O(1) bound on the number of pages
 *  written per operation (number of pages read is still O(log n)).
 *  It also makes the walk routine tail-recursive.<p>
 *
 *  Each node of the BTree uses one page of the PageStorage; we don't
 *  yet support situations where a single page is too small for one
 *  key and one value.<p>
 *
 *  The coding style in this file is pretty unusual; it looks a lot
 *  like "Java as a better C".  This is mainly because Hotspot is
 *  remarkably good at inlining methods, but remarkably bad (still,
 *  even in Java 1.7) at figuring out when it's safe to unbox values.
 *  So I expend a lot of effort trying not to create boxed values, but
 *  don't worry at all about the overhead of method calls,
 *  particularly when made via "final" references (nearly always
 *  inlined).  I don't code this way very often -- I reserve this for
 *  the 2% of my code that bears 98% of the performance burden.  There
 *  is anecdotal evidence that using the server JVM rather than the
 *  client JVM yields a dramatic increase in performance; this coding
 *  style is especially friendly to the server JVM's optimization
 *  techniques.<p>
 *
 *  Keys, values, and summary elements are stored in <i>unboxed</i>
 *  form.  This means that each of these types must know how to
 *  serialize itself to and deserialize itself from a sequence of
 *  bytes, and <i>it must be possible to perform the important
 *  operations (comparison for keys, product for summary values)
 *  on these values in unboxed form.</i> The reasons for this are set
 *  out in the previous paragraph.  See the package btree.unboxed for
 *  further details.
 *
 *  @author Adam Megacz <adam.megacz@sun.com>
 */
public class BTree
    <K extends Serializable & Comparable,
     V extends Serializable,
     S extends Serializable> {

    final CachingPageStorage   ps;
    final UnboxedComparable<K> uk;
    final AssociativeOperation<S> ao;
    final UnboxedFunction<Pair<K,V>,S> summarize;
    final Unboxed<V>           uv;
    final UnboxedInt           ui = UnboxedInt.instance;

    private LeafNodeCursor<K,V,S>       leafNodeCursor;
    private InteriorNodeCursor<K,V,S>   interiorNodeCursor1;
    private InteriorNodeCursor<K,V,S>   interiorNodeCursor2;

    int                  rootpage;
    private final byte[] keybuf;
    private final byte[] keybuf2;
    private final byte[] sbuf;

    private final byte[] largestKey;
    private       int    largestKeyPage = -1;  // or -1 if unknown

    private       int    size = 0;

    /**
     *  Create a BTree.
     *  @param ps the PageStorage to hold the underlying bytes
     *  @param uk the unboxed type for keys (must be comparable)
     *  @param uv the unboxed type for values
     *  @param summarize the function which summarizes a single (key,value) pair
     *  @param combine the function which associatively combines two summaries
     */
    public BTree(CachingPageStorage ps,
                 UnboxedComparable<K> uk,
                 Unboxed<V> uv,
                 UnboxedFunction<Pair<K,V>,S> summarize,
                 AssociativeOperation<S> combine) {
        AssociativeOperation<S> ao = combine;
        this.summarize = summarize;
        if (ao!=null) {
            if (!(ao instanceof AssociativeCommutativeOperation))
                throw new RuntimeException("Only commutative summary operations are supported (allows one-pass insertion)");
            // FIXME: if the summary is not invertible (ie a group) and commutative, we cannot do DELETE in a single pass
            // I don't think we can ever do REPLACE in one pass unless we knew the previous value
        }
        this.ps = ps;
        this.uk = uk;
        this.ao = ao;
        this.uv = uv;
        this.leafNodeCursor = new LeafNodeCursor<K,V,S>(this);
        this.interiorNodeCursor1 = new InteriorNodeCursor<K,V,S>(this);
        this.interiorNodeCursor2 = new InteriorNodeCursor<K,V,S>(this);
        this.rootpage = ps.createPage();
        this.keybuf = new byte[uk.getSize()];
        this.keybuf2 = new byte[uk.getSize()];
        this.sbuf = ao==null ? null : new byte[ao.getSize()];
        this.largestKey = new byte[uk.getSize()];
        leafNodeCursor.initBuf(ps.getPage(rootpage, false), true);
        leafNodeCursor.writeBack();
    }

    /**
     *  Returns the number of entries in the tree with a key between
     *  min and max inclusive; if either min or max is null it is treated
     *  as negative or positive infinity (respectively)
     */
    public int getNumFromKeys(K min, K max) {
        if (min==null && max==null) return size;
        throw new RuntimeException("not implemented");
    }

    /** same as getNumFromKeys(null,null) */
    public int  size() { return getNumFromKeys(null,null); }

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

    /** returns the least key <i>strictly</i> greater than the argument */
    public V getKeyFromKeyNext(K key) {
        throw new RuntimeException("not implemented");
    }

    /** returns the greatest key <i>strictly</i> less than the argument */
    public V getKeyFromKeyPrev(K key) {
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
    public V replace(K key, V val) {
        uk.serialize(key, keybuf, 0);
        return (V)walk(keybuf, 0, val, Op.REPLACE, 0);
    }

    /** returns value previously in the tree; will throw an exception if the key is not already in the tree */
    public V remove(K key) {
        throw new RuntimeException("not implemented");
        // size--;
    }

    /** remove all entries */
    public void clear() {
        throw new RuntimeException("not implemented");
    }

    /** compute the summary of all (key,value) pairs between min and max, inclusive */
    public S getSummaryFromKeys(K min, K max) {
        uk.serialize(min, keybuf, 0);
        uk.serialize(max, keybuf2, 0);
        walk(keybuf, 0, null, Op.SUMMARIZE_LEFT,  0, keybuf2, 0, sbuf, 0);
        walk(keybuf, 0, null, Op.SUMMARIZE_MID,   0, keybuf2, 0, sbuf, 0);
        walk(keybuf, 0, null, Op.SUMMARIZE_RIGHT, 0, keybuf2, 0, sbuf, 0);
        return (S)ao.deserialize(sbuf, 0);
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
        REPLACE,
        SUMMARIZE_LEFT,
        SUMMARIZE_MID,
        SUMMARIZE_RIGHT,
            ;
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
    

    private Object walk(byte[] key, int key_ofs, V val, Op op, int ord) {
        return walk(key, key_ofs, val, op, ord, null, 0, null, 0);
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
    private Object walk(byte[] key, int key_ofs, V val, Op op, int ord, byte[] key2, int key2_ofs, byte[] ret, int ret_ofs) {
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

                if (ao!=null) {
                    byte[] monbuf = new byte[ao.getSize()];
                    cur.getSummary(0, monbuf, 0);
                    for(int i=1; i<splitPoint; i++) {
                        cur.getSummary(i, monbuf, 0);
                        parentNodeCursor.multiplySummaryCommutative(idx, monbuf, 0);
                    }
                }
                int num = cur.split(parentNodeCursor.getBuf(), ofs, splitPoint);
                parentNodeCursor.setNumValsBelowBucket(idx, num);
                int newpage = cur.getPageId();
                if (largestKeyPage==oldpage) largestKeyPage = newpage;
                parentNodeCursor.setBucketPageId(idx+1, newpage);
                if (!splitting_last_or_root)
                    parentNodeCursor.setNumValsBelowBucket(idx+1, old-num);
                if (ao!=null && (!parentNodeCursor.isRightMost() || idx+1<parentNodeCursor.getNumBuckets()-1)) {
                    byte[] monbuf = new byte[ao.getSize()];
                    cur.getSummary(0, monbuf, 0);
                    for(int i=1; i<cur.getNumBuckets() - (cur.isRightMost() ? 1 : 0); i++) {
                        cur.getSummary(i, monbuf, 0);
                        parentNodeCursor.multiplySummaryCommutative(idx+1, monbuf, 0);
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
                    if (ao != null && (idx < interiorNodeCursor.getNumBuckets()-1 || !interiorNodeCursor.isRightMost())) {
                        throw new RuntimeException("not implemented");
                        /*
                          // FIXME
                        byte[] monbuf = new byte[ao.getSize()];
                        byte[] vbuf = new byte[uv.getSize()];
                        uv.serialize(val, vbuf, 0);
                        summarize.call(key, key_ofs, vbuf, 0, monbuf, 0);
                        interiorNodeCursor.multiplySummaryCommutative(idx, monbuf, 0);
                        */
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

    static long insertionFastPath = 0;
    static long insertionSlowPath = 0;
    static long splitEven = 0;
    static long splitUnEven = 0;

    /** debugging method; may go away in future releases */
    public static void clearStats() {
        BTree.splitUnEven = 0;
        BTree.splitEven = 0;
        BTree.insertionFastPath = 0;
        BTree.insertionSlowPath = 0;
    }

    /** debugging method; may go away in future releases */
    public static void dumpStats(PrintStream pw) {
        pw.println("BTree stats: insertion fastpath = " +
                   BTree.insertionFastPath + "/" + (BTree.insertionFastPath+BTree.insertionSlowPath) + " = " +
                   (int)(( BTree.insertionFastPath * 100 )/(float)(BTree.insertionFastPath+BTree.insertionSlowPath)) + "%");
        pw.println("             intelligent splits = " +
                    BTree.splitUnEven + "/" + (BTree.splitUnEven+BTree.splitEven) + " = " +
                    (int)(( BTree.splitUnEven * 100 )/(float)(BTree.splitUnEven+BTree.splitEven)) + "%");
    }

}