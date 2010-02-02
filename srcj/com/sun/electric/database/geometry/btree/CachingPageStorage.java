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

/**
 *  API for a PageStorage with some sort of cache; the {@see
 *  CachedPage} class provides an interface for interacting with the
 *  cache.
 */
public abstract class CachingPageStorage extends PageStorage {

    public CachingPageStorage(int pageSize) {
        super(pageSize);
    }

    /**
     *  Creates space in the cache for pageid, but only actually reads
     *  the bytes if readBytes is true; if the page was not already
     *  in the cache and readBytes is false, subsequent calls to
     *  setDirty()/flush() will overwrite data previously on the page.
     */
    public abstract CachedPage getPage(int pageid, boolean readBytes);

    /** Write a page <i>through</i> the cache to nonvolatile storage */
    public abstract void writePage(int pageid, byte[] buf, int ofs);

    /** A page which is currently in the cache; pages can be dirty or clean. */
    public abstract class CachedPage {

        /** gets the byte[] for this page; it is okay to manipulate it directly, but you must call setDirty() and flush() afterwards */
        public abstract byte[] getBuf();

        /** the pageid of this cached page */
        public abstract int    getPageId();

        /** indicates that the page has been accessed (either a read or a write) for purposes of least-recently-used calculations */
        public abstract void touch();

        /** marks the page as "dirty"; that is, in need of being written back to nonvolatile storage */
        public abstract void setDirty();

        /** if the page is dirty, write it back to nonvolatile storage and mark it not dirty */
        public abstract void flush();

        /** returns true if the page is marked dirty */
        public abstract boolean isDirty();
    }
}