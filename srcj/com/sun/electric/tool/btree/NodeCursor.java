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

abstract class NodeCursor
    <K extends Serializable & Comparable,
     V extends Serializable,
     S extends Serializable> {
    protected byte[] buf;
    protected int pageid;
    protected boolean dirty = false;
    protected PageStorage ps;
    protected static final int          SIZEOF_INT = 4;
    protected        final BTree<K,V,S> bt;
    protected NodeCursor(BTree<K,V,S> bt) {
        this.bt = bt;
        this.ps = bt.ps;
    }
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
     *  it then returns the pageid of the newly-created page.
     */
    public abstract int split(byte[] key, int key_ofs);

    public abstract boolean isFull();
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
            if      (comp==0)  { left=i; right=i+1; }
            else if (comp>0)   left = i;
            else if (comp<0)   right = i;
        }
        return left;
    }

    protected abstract int endOfBuf();
    public abstract void getKey(int keynum, byte[] key, int key_ofs);
}
