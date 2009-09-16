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
 *   Internal use only; kind of a hack.  This is just a "parser"
 *   for the page format.
 *
 *    int: pageid of parent (root points to self)
 *    int: number of buckets
 *    int: pageid of first child
 *    int: number of values below first child
 *    S:   summary of first child
 *    repeat
 *       key: least key somewhere in child X
 *       int: pageid of child X
 *       int: number of values below child X
 *       S:   summary of child X
 *
 *   Possible feature: store the buckets of an interior node
 *   internally as a simple balanced tree (a splay tree?).  The
 *   System.arraycopy()'s are scaling very poorly as the page size
 *   increases.
 */     
class InteriorNodeCursor
    <K extends Serializable & Comparable,
     V extends Serializable,
     S extends Serializable>
    extends NodeCursor<K,V,S> {

    private        final int          INTERIOR_HEADER_SIZE;
    private        final int          INTERIOR_ENTRY_SIZE;
    private        final int          INTERIOR_MAX_BUCKETS;
    private              int          numbuckets;  // just a cache

    public int getMaxBuckets() { return INTERIOR_MAX_BUCKETS; }

    /**
     *  Creates a new slot for a child at index "idx" by shifting over
     *  the child previously in that slot (if any) and all after it.
     *  Returns the offset in the buffer at which to write the least
     *  key beneath the new child.
     */
    public int insertNewBucketAt(int idx) {
        assert !isFull();
        assert idx!=0;
        System.arraycopy(buf, INTERIOR_HEADER_SIZE + (idx-1)*INTERIOR_ENTRY_SIZE,
                         buf, INTERIOR_HEADER_SIZE + idx*INTERIOR_ENTRY_SIZE,
                         endOfBuf() - (INTERIOR_HEADER_SIZE + (idx-1)*INTERIOR_ENTRY_SIZE));
        setNumBuckets(getNumBuckets()+1);
        return INTERIOR_HEADER_SIZE + (idx-1)*INTERIOR_ENTRY_SIZE;
    }

    public InteriorNodeCursor(BTree<K,V,S> bt) {
        super(bt);
        this.INTERIOR_HEADER_SIZE = (bt.monoid==null ? 0 : bt.monoid.getSize()) + 4 * SIZEOF_INT;
        this.INTERIOR_ENTRY_SIZE  = bt.uk.getSize() + (bt.monoid==null ? 0 : bt.monoid.getSize()) + SIZEOF_INT + SIZEOF_INT;
        this.INTERIOR_MAX_BUCKETS = ((ps.getPageSize()-INTERIOR_HEADER_SIZE) / INTERIOR_ENTRY_SIZE);
    }

    public static boolean isInteriorNode(byte[] buf) { return UnboxedInt.instance.deserializeInt(buf, 1*SIZEOF_INT)!=0; }

    public void setBuf(int pageid, byte[] buf) {
        assert pageid==-1 || isInteriorNode(buf);
        super.setBuf(pageid, buf);
        numbuckets = bt.ui.deserializeInt(buf, 1*SIZEOF_INT);
    }

    public void initBuf(int pageid, byte[] buf) {
        super.setBuf(pageid, buf);
    }

    /**
     *  Initialize a new root node whose bucketss' pageids are child1
     *  and child2; buf must already contain the least key under
     *  child2 at position key_ofs.
     */
    public void initRoot(byte[] buf) {
        bt.rootpage = ps.createPage();
        super.setBuf(bt.rootpage, buf);
        setParentPageId(bt.rootpage);
        setNumBuckets(1);
    }

    public int  getParentPageId() { return bt.ui.deserializeInt(buf, 0*SIZEOF_INT); }
    public void setParentPageId(int pageid) { bt.ui.serializeInt(pageid, buf, 0*SIZEOF_INT); }
    public int  getNumBuckets() { return numbuckets; }
    public void setNumBuckets(int num) { bt.ui.serializeInt(numbuckets = num, buf, 1*SIZEOF_INT); }
    public int  getBucketPageId(int idx) { return bt.ui.deserializeInt(buf, 2*SIZEOF_INT+INTERIOR_ENTRY_SIZE*idx); }
    public void setBucketPageId(int idx, int pageid) { bt.ui.serializeInt(pageid, buf, 2*SIZEOF_INT+INTERIOR_ENTRY_SIZE*idx); }
    public int  getNumValsBelowBucket(int idx) { return bt.ui.deserializeInt(buf, 3*SIZEOF_INT+INTERIOR_ENTRY_SIZE*idx); }
    public int  compare(byte[] key, int key_ofs, int keynum) {
        if (keynum<=0) return 1;
        if (keynum>=getNumBuckets()) return -1;
        return bt.uk.compare(key, key_ofs, buf, INTERIOR_HEADER_SIZE + (keynum-1)*INTERIOR_ENTRY_SIZE);
    }

    protected void scoot(byte[] oldbuf, int endOfBuf) {
        int len = 2*SIZEOF_INT + INTERIOR_ENTRY_SIZE*(getMaxBuckets()/2);
        System.arraycopy(oldbuf, len,
                         buf, 2*SIZEOF_INT,
                         endOfBuf - len);
    }

    public boolean isFull() { return getNumBuckets() == getMaxBuckets(); }
    public boolean isLeafNode() { return false; }
    protected int endOfBuf() { return 2*SIZEOF_INT + getNumBuckets()*INTERIOR_ENTRY_SIZE; }
    public void getKey(int keynum, byte[] key, int key_ofs) {
        System.arraycopy(buf, INTERIOR_HEADER_SIZE + INTERIOR_ENTRY_SIZE*(keynum-1),
                         key, key_ofs, bt.uk.getSize());
    }
}
