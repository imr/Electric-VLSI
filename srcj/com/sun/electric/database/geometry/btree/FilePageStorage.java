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

/**
 *  PageStorage implemented via a RandomAccessFile.
 *
 *  It would be nice to have an implementation which used asynchronous
 *  I/O for background writes.
 *
 *  It would be nice to move towards a zero-copy architecture.
 */
public class FilePageStorage extends PageStorage {

    public static FilePageStorage create() throws IOException {
        File f = File.createTempFile("pagestorage", ".ebtree");
        // XXX: consider "rws" or "rwd"
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        return new FilePageStorage(raf);
    }

    // just a guess; should be just a bit less than some multiple of the system block size
    private static final int BLOCK_SIZE = 4096 * 4 - 100;

    //////////////////////////////////////////////////////////////////////////////

    private final RandomAccessFile raf;
    private int numpages;

    public int  getNumPages() { return numpages; }

    /** private because the file format is not yet finalized */
    private FilePageStorage(RandomAccessFile raf) {
        try {
            this.raf = raf;
            numpages = (int)(raf.length() % (long)getPageSize());
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public int getPageSize() {
        return BLOCK_SIZE;
    }

    public int  createPage() {
        // note, any pages created but not written will vanish when the file is closed
        return numpages++;
    }

    public void writePage(int pageid, byte[] buf, int ofs) {
        if (pageid >= numpages)
            throw new RuntimeException("invalid pageid "+pageid);
        try {
            raf.seek(pageid * getPageSize());
            raf.write(buf, ofs, getPageSize());
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public byte[] readPage(int pageid, byte[] buf, int ofs) {
        if (buf==null) {
            buf = new byte[getPageSize()];
            ofs = 0;
        }
        if (pageid >= numpages)
            throw new RuntimeException("invalid pageid "+pageid);
        try {
            raf.seek(pageid * getPageSize());
            raf.readFully(buf, ofs, getPageSize());
        } catch (IOException e) { throw new RuntimeException(e); }
        return buf;
    }

    public void flush(int pageid) {
        // FIXME: ugly
        flush();
    }

    public void flush() {
        // do nothing because we currently make no guarantees about when things hit the disk
    }

}