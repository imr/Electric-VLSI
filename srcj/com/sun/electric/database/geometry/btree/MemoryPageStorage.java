/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MemoryPageStorage.java
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

/** A PageStorage that uses plain old memory. */
public class MemoryPageStorage extends PageStorage {

    private       byte[][] pages;
    private       int      numpages;

    public MemoryPageStorage(int pagesize) {
        super(pagesize);
        this.numpages = 0;
        this.pages = new byte[1][];
    }

    public int getNumPages() { return numpages; }
    public int createPage() {
        if (numpages >= pages.length) {
            byte[][] newpages = new byte[pages.length*2][];
            System.arraycopy(pages, 0, newpages, 0, pages.length);
            pages = newpages;
        }
        pages[numpages] = new byte[getPageSize()];
        return numpages++;
    }
    public void fsync(int pageid) { }
    public void fsync() { }
    public void writePage(int pageid, byte[] buf, int ofs) {
        System.arraycopy(buf, ofs, pages[pageid], 0, getPageSize());
    }
    public void readPage(int pageid, byte[] buf, int ofs) {
        System.arraycopy(pages[pageid], 0, buf, ofs, getPageSize());
    }
    public synchronized void close() { pages = null; }
}