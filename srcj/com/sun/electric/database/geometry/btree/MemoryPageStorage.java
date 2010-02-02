/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MemoryPageStorage.java
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

/** A PageStorage that uses plain old memory. */
public class MemoryPageStorage extends CachingPageStorage {

    private       CachedPageImpl[] pages;
    private       int              numpages;

    public MemoryPageStorage(int pagesize) {
        super(pagesize);
        this.numpages = 0;
        this.pages = new CachedPageImpl[1];
    }

    public int getNumPages() { return numpages; }
    public int createPage() {
        if (numpages >= pages.length) {
            CachedPageImpl[] newpages = new CachedPageImpl[pages.length*2];
            System.arraycopy(pages, 0, newpages, 0, pages.length);
            pages = newpages;
        }
        pages[numpages] = new CachedPageImpl(numpages);
        return numpages++;
    }
    /** no-op */
    public void fsync(int pageid) { }
    /** no-op */
    public void fsync() { }
    public void writePage(int pageid, byte[] buf, int ofs) {
        System.arraycopy(buf, ofs, pages[pageid].buf, 0, getPageSize());
    }
    public void readPage(int pageid, byte[] buf, int ofs) {
        System.arraycopy(pages[pageid].buf, 0, buf, ofs, getPageSize());
    }
    public synchronized void close() { pages = null; }
    public CachedPage getPage(int pageid, boolean readBytes) { return pages[pageid]; }

    private class CachedPageImpl extends CachedPage {
        private int pageid;
        private byte[] buf;
        private boolean dirty;
        public CachedPageImpl(int pageid) { this.pageid = pageid; this.buf = new byte[getPageSize()]; }
        public byte[] getBuf() { return buf; }
        public int    getPageId() { return pageid; }
        public void touch() { }
        public void setDirty() { this.dirty = true; }
        public void flush() { this.dirty = false; }
        public boolean isDirty() { return dirty; }
    }
}