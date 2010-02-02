/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OverflowPageStorage.java
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

/**
 *  Combines two PageStorage objects, using the first until the "high
 *  water mark" is exceeded, then moving to the other.  Generally the
 *  first PageStorage is "small but fast" and the second is "large but
 *  slow".
 *
 *  Might not be thread-safe.
 */
public class OverflowPageStorage extends PageStorage {

    private final PageStorage ps1;
    private final PageStorage ps2;
    private final int highWaterMark;
    private boolean overflowed = false;

    /** Note that highWaterMark is in BYTES, not pages */
    public OverflowPageStorage(PageStorage ps1, PageStorage ps2, long highWaterMark) {
        super(Math.max(ps1.getPageSize(), ps1.getPageSize()));
        this.ps1 = ps1;
        this.ps2 = ps2;
        this.highWaterMark = (int)(highWaterMark / getPageSize());
    } 

    public int getNumPages() { return overflowed ? ps2.getNumPages() : ps1.getNumPages(); }
    public synchronized int createPage() {
        if (overflowed) return ps2.createPage();
        if (ps1.getNumPages() < highWaterMark) return ps1.createPage();
        byte[] buf = new byte[getPageSize()];
        for(int i=0; i<ps1.getNumPages(); i++) {
            while(ps2.getNumPages() < i+1) ps2.createPage();
            ps1.readPage(i, buf, 0);
            ps2.writePage(i, buf, 0);
        }
        overflowed = true;
        ps1.close();
        return ps2.createPage();
    }
    public void fsync(int pageid) { if (overflowed) ps2.fsync(pageid); else ps1.fsync(pageid); }
    public void fsync() { if (overflowed) ps2.fsync(); else ps1.fsync(); }
    public void writePage(int pageid, byte[] buf, int ofs) {
        if (overflowed) ps2.writePage(pageid, buf, ofs);
        else            ps1.writePage(pageid, buf, ofs);
    }
    public void readPage(int pageid, byte[] buf, int ofs) {
        if (overflowed) ps2.readPage(pageid, buf, ofs);
        else            ps1.readPage(pageid, buf, ofs);
    }

    public synchronized void close() { if (overflowed) ps2.close(); else ps1.close(); }
}