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

/**
 *  PageStorage implemented via a RandomAccessFile.  No
 *  PageStorage(RandomAccessFile) constructor is provided because the
 *  on-disk format is not yet stable.
 */
public class FilePageStorage extends PageStorage {

    /**
     *  Create a new FilePageStorage; no PageStorage(RandomAccessFile)
     *  constructor is provided because the on-disk format is not yet
     *  stable.
     */
    public static FilePageStorage create() {
        return new FilePageStorage();
    }

    // just a guess; should be some multiple of the system block size
    private static final int BLOCK_SIZE = 4096 * 4;

    //////////////////////////////////////////////////////////////////////////////

    private RandomAccessFile raf;
    private int numpages;

    public synchronized int getNumPages() { return numpages; }

    private FilePageStorage() {
        super(BLOCK_SIZE);
        this.raf = null;
        this.numpages = 0;
    }

    /** private because the file format is not yet finalized */
    private FilePageStorage(RandomAccessFile raf) {
        super(BLOCK_SIZE);
        try {
            this.raf = raf;
            numpages = (int)(raf.length() % (long)getPageSize());
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public synchronized int createPage() {
        if (raf == null)
            try {
                // FEATURE: consider "rws" or "rwd"
                this.raf = new RandomAccessFile(File.createTempFile("pagestorage", ".ebtree"), "rw");
            } catch (Exception e) { throw new RuntimeException(e); }

        // note, any pages created but not written will vanish when the file is closed
        return numpages++;
    }

    public void writePage(int pageid, byte[] buf, int ofs) {
        try {
            raf.seek(pageid * getPageSize());
            raf.write(buf, ofs, getPageSize());
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public void readPage(int pageid, byte[] buf, int ofs) {
        try {
            raf.seek(pageid * getPageSize());
            raf.readFully(buf, ofs, getPageSize());
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public void fsync(int pageid) {
        // FIXME: ugly
        fsync();
    }

    public void fsync() {
        // do nothing because we currently make no guarantees about when things hit the disk
    }

    public synchronized void close() { }
}