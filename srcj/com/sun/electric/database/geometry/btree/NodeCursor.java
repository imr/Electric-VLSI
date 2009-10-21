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

/**
 *   Internal use only; kind of a hack.  This is just a "parser"
 *   for the page format.
 *
 *   Possible feature: store the buckets of an interior node
 *   internally as a simple balanced tree (a splay tree?).  The
 *   System.arraycopy()'s are scaling very poorly as the page size
 *   increases.
 *
 *     - once we do this, we can probably afford to move to 32Kb
 *       pages, which will give us great performance on nearly any
 *       filesystem or storage device, even RAID storage (which has
 *       huge block sizes).
 */
abstract class NodeCursor
    <K extends Serializable & Comparable,
     V extends Serializable,
     S extends Serializable> {

    protected              byte[]       buf;
    protected              int          pageid;
    protected              boolean      dirty = false;
    protected              PageStorage  ps;
    protected static final int          SIZEOF_INT = 4;
    protected        final BTree<K,V,S> bt;

    protected NodeCursor(BTree<K,V,S> bt) {
        this.bt = bt;
        this.ps = bt.ps;
    }
    public abstract void initBuf(int pageid, byte[] buf);
    public abstract void setNumBuckets(int num);
    public void setBuf(int pageid, byte[] buf) {
        assert !dirty;
        this.buf = buf;
        this.pageid = pageid;
    }
    public void writeBack() {
        //if (!dirty) return;
        dirty = false;
        ps.writePage(pageid, buf, 0);
    }

    /**
     *  This method writes back the first half of the node's contents,
     *  deposits the second half on a new page, and (if key!=null)
     *  writes the least key beneath the right half into key[key_ofs];
     *  it then returns the number of values appearing anywhere below
     *  the left page.
     */
    public int split(byte[] key, int key_ofs, int splitPoint) {
        assert isFull();
        int endOfBuf = endOfBuf();

        int ret = 0;
        for(int i=0; i<splitPoint; i++)
            ret += getNumValsBelowBucket(i);

        // chop off our second half, point our parent at the page-to-be, and write back
        setNumBuckets(splitPoint);
        writeBack();

        if (key!=null)
            getKey(splitPoint, key, key_ofs);

        // move the second half of our entries to the front of the block, and write back
        byte[] oldbuf = buf;
        int parent = getParentPageId();
        initBuf(ps.createPage(), new byte[buf.length]);
        setNumBuckets(getMaxBuckets()-splitPoint);
        scoot(oldbuf, endOfBuf, splitPoint);
        setParentPageId(parent);
        writeBack();
        return ret;
    }

    public boolean isFull() { return getNumBuckets() >= getMaxBuckets(); }
    public abstract int  getParentPageId();
    public abstract void setParentPageId(int pageid);
    public int getPageId() { return pageid; }
    public byte[] getBuf() { return buf; }

    /**
     *  Each node has a number of buckets, separated by keys; keys
     *  appear between buckets as well as before the first bucket and
     *  after the last.  All values in a bucket are greater than or
     *  equal to the key to the left of the bucket and strictly less
     *  than the key to the right.
     *
     *  Leaf nodes have a real key immediately before each entry --
     *  this is the entry's actual key -- plus an additional imaginary
     *  key after the last child.
     *
     *  Interior nodes have an imaginary key before the first node, a
     *  real key between each pair of nodes, and an imaginary key
     *  after the last node.
     *
     *  Imaginary keys are either infinitely large or infinitely
     *  small, depending on which end of the sequence they appear on.
     *
     *  This numbering scheme might seem strange, but it really helps
     *  avoid fencepost bugs -- I tried several conventions before
     *  settling on this one.
     *
     *  Key zero is to the left of bucket zero, and the last key is to
     *  the right of the last child (and has an index one greater than
     *  it).
     */
    public abstract int getNumBuckets();

    public abstract int getMaxBuckets();

    /**
     *  Compares the key at position keynum to the key represented by
     *  the bytes provided.  Same semantics as Comparable.compareTo().
     *  To simplify other codepaths, if keynum<0, the return value is
     *  always positive and if keynum>=getNumBuckets()+1 the return value is
     *  always negative.
     */
    public abstract int compare(byte[] key, int key_ofs, int keynum);

    public abstract boolean isLeafNode();

    /** i is the position of the leftmost key which is less than or equal to the key we are testing */
    public int search(byte[] key, int key_ofs) {
        int left = -1;
        int right = getNumBuckets();
        while(left+1<right) {
            assert compare(key, key_ofs, left) >= 0;
            int i = (left+right)/2;
            int comp = compare(key, key_ofs, i);
            if      (comp==0)  return i;
            else if (comp>0)   left = i;
            else if (comp<0)   right = i;
        }
        return left;
    }

    protected abstract int endOfBuf();
    public abstract void getKey(int keynum, byte[] key, int key_ofs);

    /** kludge */
    protected abstract void scoot(byte[] oldbuf, int endOfBuf, int splitPoint);

    /** the total number of values stored in bucket or any descendent thereof */
    public abstract int getNumValsBelowBucket(int bucket);

}
