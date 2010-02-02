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
 *  Page format:
 *
 *    int: isRightMost (1 or 0)
 *    int: number of buckets
 *    int: pageid of first bucket
 *    S:   summary of first bucket
 *    repeat
 *       int: number of values in (N-1)^th bucket
 *       key: least key in N^th bucket
 *       int: pageid of N^th bucket
 *       S:   summary of N^th bucket
 *
 *   The arrangement above was chosen to deliberately avoid tracking
 *   the number of values in the last bucket.  This ensures that we
 *   can append new values to the last bucket of the rightmost leaf
 *   without touching any interior nodes (unless we're forced to
 *   split, of course, but if the pages are large enough that doesn't
 *   happen very often).  This maintains the O(1)-unless-we-split
 *   fastpath for appends while still keeping enough interior data to
 *   perform ordinal queries.
 *
 *   Note that we still need to keep the summary for the last bucket,
 *   because we don't know as much about its semantics.  This is just
 *   one of the reasons why "number of values below here" is computed
 *   separately rather than being part of the summary.
 */     
class InteriorNodeCursor
    <K extends Serializable & Comparable,
     V extends Serializable,
     S extends Serializable>
    extends NodeCursor<K,V,S> {

    private        final int          INTERIOR_HEADER_SIZE;
    private        final int          INTERIOR_ENTRY_SIZE;
    private        final int          INTERIOR_MAX_BUCKETS;
    private        final int          SIZEOF_SUMMARY;
    private              int          numbuckets;  // just a cache

    public InteriorNodeCursor(BTree<K,V,S> bt) {
        super(bt);
        this.SIZEOF_SUMMARY       = bt.ao==null ? 0 : bt.ao.getSize();
        this.INTERIOR_HEADER_SIZE = 2*SIZEOF_INT;
        this.INTERIOR_ENTRY_SIZE  = bt.uk.getSize() + SIZEOF_SUMMARY + SIZEOF_INT + SIZEOF_INT;
        this.INTERIOR_MAX_BUCKETS = ((ps.getPageSize()-INTERIOR_HEADER_SIZE-SIZEOF_INT-this.SIZEOF_SUMMARY) / INTERIOR_ENTRY_SIZE);
    }

    /**
     *  Creates a new bucket for a child at index "idx" by shifting
     *  over the child previously in that bucket (if any) and all
     *  after it.  Returns the offset in the buffer at which to write
     *  the least key beneath the new child.  Note that the value in
     *  the bucket and the summary for that bucket are not
     *  initialized.
     */
    public int insertNewBucketAt(int idx) {
        assert !isFull();
        assert idx!=0;
        if (idx < getNumBuckets())
            System.arraycopy(getBuf(), INTERIOR_HEADER_SIZE + (idx-1)*INTERIOR_ENTRY_SIZE,
                             getBuf(), INTERIOR_HEADER_SIZE + idx*INTERIOR_ENTRY_SIZE,
                             endOfBuf() - (INTERIOR_HEADER_SIZE + (idx-1)*INTERIOR_ENTRY_SIZE));
        setNumBuckets(getNumBuckets()+1);
        return INTERIOR_HEADER_SIZE + idx*INTERIOR_ENTRY_SIZE - bt.uk.getSize();
    }

    public static boolean isInteriorNode(byte[] buf) { return UnboxedInt.instance.deserializeInt(buf, 1*SIZEOF_INT)!=0; }
    public int getMaxBuckets() { return INTERIOR_MAX_BUCKETS; }
    public void initBuf(CachedPage cp, boolean isRightMost) { 
        super.setBuf(cp);
        setRightMost(isRightMost);
    }
    public int  getNumBuckets() { return numbuckets; }
    protected void setNumBuckets(int num) { bt.ui.serializeInt(numbuckets = num, getBuf(), 1*SIZEOF_INT); }

    public void setBuf(CachedPage cp) {
        super.setBuf(cp);
        numbuckets = bt.ui.deserializeInt(getBuf(), 1*SIZEOF_INT);
    }

    /** Initialize a new root node. */
    public void initRoot() {
        bt.rootpage = ps.createPage();
        super.setBuf(ps.getPage(bt.rootpage, false));
        setNumBuckets(1);
        setRightMost(true);
    }

    public boolean isLeafNode() { return false; }

    protected int endOfBuf() { return INTERIOR_HEADER_SIZE + getNumBuckets()*INTERIOR_ENTRY_SIZE - SIZEOF_SUMMARY - SIZEOF_INT; }

    public int  getBucketPageId(int idx) { return bt.ui.deserializeInt(getBuf(), INTERIOR_HEADER_SIZE+INTERIOR_ENTRY_SIZE*idx); }
    public void setBucketPageId(int idx, int pageid) { bt.ui.serializeInt(pageid, getBuf(), INTERIOR_HEADER_SIZE+INTERIOR_ENTRY_SIZE*idx); }
    public int  compare(byte[] key, int key_ofs, int keynum) {
        if (keynum<=0) return 1;
        if (keynum>=getNumBuckets()) return -1;
        return bt.uk.compare(key, key_ofs, getBuf(), INTERIOR_HEADER_SIZE + keynum*INTERIOR_ENTRY_SIZE - bt.uk.getSize());
    }

    protected void scoot(byte[] oldBuf, int endOfBuf, int splitPoint) {
        int len = INTERIOR_HEADER_SIZE + INTERIOR_ENTRY_SIZE*(splitPoint);
        System.arraycopy(oldBuf, len,
                         getBuf(), INTERIOR_HEADER_SIZE,
                         endOfBuf - len);
    }

    public void getKey(int keynum, byte[] key, int key_ofs) {
        System.arraycopy(getBuf(), INTERIOR_HEADER_SIZE + keynum*INTERIOR_ENTRY_SIZE - bt.uk.getSize(),
                         key, key_ofs, bt.uk.getSize());
    }

    public void setNumValsBelowBucket(int idx, int num) {
        if (idx==getNumBuckets()-1)
            throw new RuntimeException("InteriorNodeCursors don't store numValuesBelowBucket() for their last bucket");
        assert idx>=0 && idx<getNumBuckets()-1;
        bt.ui.serializeInt(num, getBuf(), INTERIOR_HEADER_SIZE+SIZEOF_INT+SIZEOF_SUMMARY+INTERIOR_ENTRY_SIZE*idx);
    }

    public int getNumValsBelowBucket(int bucket) {
        if (bucket>=getNumBuckets()) return 0;

        // FIXME: should be an assertion
        if (bucket==getNumBuckets()-1)
            throw new RuntimeException("InteriorNodeCursors don't store numValuesBelowBucket() for their last bucket");

        return bt.ui.deserializeInt(getBuf(), INTERIOR_HEADER_SIZE+SIZEOF_INT+SIZEOF_SUMMARY+INTERIOR_ENTRY_SIZE*bucket);
    }

    public void multiplySummaryCommutative(int idx, byte[] buf, int ofs) {
        if (idx==getNumBuckets()-1 && isRightMost())
            throw new RuntimeException("RightMost InteriorNodeCursors don't store a summary value for their last bucket");
        assert idx>=0 && idx<getNumBuckets();
        bt.ao.multiply(buf, ofs,
                           getBuf(), INTERIOR_HEADER_SIZE+SIZEOF_INT+INTERIOR_ENTRY_SIZE*idx,
                           getBuf(), INTERIOR_HEADER_SIZE+SIZEOF_INT+INTERIOR_ENTRY_SIZE*idx);
    }

    public void getSummary(int idx, byte[] buf, int ofs) {
        if (idx==getNumBuckets()-1 && isRightMost())
            throw new RuntimeException("RightMost InteriorNodeCursors don't store a summary value for their last bucket");
        assert idx>=0 && idx<getNumBuckets();
        System.arraycopy(getBuf(), INTERIOR_HEADER_SIZE+SIZEOF_INT+INTERIOR_ENTRY_SIZE*idx,
                         buf, ofs,
                         bt.ao.getSize());
    }
}
