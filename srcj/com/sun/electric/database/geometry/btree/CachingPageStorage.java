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

/**
 *  A caching wrapper around PageStorage.
 *
 *  This class is thread-safe; overlapped read/write and write/write
 *  pairs may produce undefined data, but are otherwise safe.
 *
 */
public class CachingPageStorage extends PageStorage {

   /*
    *  There are two levels of locks: one on the CachingPageStorage
    *  object (the "global lock") and one on each CachedPage (the
    *  "local lock").  Locking rules:
    *
    *  0. Never manipulate the allCachedPages or cache objects unless
    *     holding the global lock.
    *
    *  1. If you are holding a local lock, do not attempt to acquire
    *     the global lock (this ensures that whomever holds the global
    *     lock can always safely attempt to acquire the lock on any
    *     individual page).
    *
    *  2. Never invoke a method on the underlying PageStorage while
    *     holding the global lock (this will introduce a concurrency
    *     bottleneck.  This means you shouldn't call getPage(),
    *     writePage(), readPage(), CachedPage.flush(),
    *     CachedPage.evict(), CachedPage.touch(), or
    *     CachedPage.setDirty() while holding the global lock.
    */

    // FEATURE: use instances of SoftReference to react to memory pressure.  Probably only usable for non-dirty pages.
    // FEATURE: use ARC http://en.wikipedia.org/wiki/Adaptive_Replacement_Cache
    // FEATURE: implement asyncFlush
    // FEATURE: detect new CachingPageStorage(new CachingPageStorage(...), 0)
    // FEATURE: attempt to evict non-dirty pages first?

    /** underlying PageStorage */                 private final PageStorage                       ps;
    /** all CachedPage instances, even evicted */ private final WeakHashMap<Integer,CachedPage>   allCachedPages = new WeakHashMap<Integer,CachedPage>();
    /** non-evicted CachedPage instances */       private final LinkedHashMap<Integer,CachedPage> cache;
    /** limit on cache.size() */                  private       int                               cacheSize = 0;

    /**
     *  The cacheSize limit applies only to pages which have not been
     *  evicted.  An evicted page will be freed from memory (garbage
     *  collected) if the user of this class has not retained a
     *  reference to it; otherwise it simply no longer counts towards
     *  the maximum cache size.  For this reason, applications should
     *  avoid holding a reference to a CachedPage for a long time.
     *
     *  A CachingPageStorage with a limit of zero is useful; all of its
     *  pages are always in the evicted state and it will act as a
     *  "buffer manager" for classes that do not want to manage their
     *  own byte[] pools.
     *
     *  If asyncFlush is true, a background thread will make a
     *  best-effort attempt to flush dirty pages even before a flush()
     *  is explicitly requested.
     */
    public CachingPageStorage(PageStorage ps, int cacheSize, boolean asyncFlush) {
        this.ps = ps;
        this.cacheSize = cacheSize;
        this.cache = new LinkedHashMap<Integer,CachedPage>(cacheSize+3, 0.75f, true);
        if (asyncFlush)
            throw new RuntimeException("asyncFlush is not yet supported");
    }

    public int getPageSize() { return ps.getPageSize(); }
    public int createPage() { return ps.createPage(); }
    public int getNumPages() { return ps.getNumPages(); }

    /**
     *  Creates space in the cache for pageid, but only actually reads
     *  the bytes if readBytes is true.  If the page was not already
     *  in the cache and readBytes is false, subsequent calls to
     *  setDirty()/flush() will overwrite data previously on the page.
     */
    public CachedPage getPage(int pageid, boolean readBytes) {
        CachedPage page = null;
        boolean doWait = false;
        synchronized(CachingPageStorage.this) { do {
                page = cache.get(pageid);
                if (page!=null) { readBytes = false; break; }
                page = allCachedPages.get(pageid);
                if (page!=null) { readBytes = false; doWait = true; break; }
                page = new CachedPage(pageid, new byte[ps.getPageSize()]);
            } while(false); }

        //
        // unfortunately we'd like to acquire a local lock before
        // releasing the global lock.  Java's monitors can't do
        // hand-over-hand locking, so instead we do this:
        //
        if (doWait) synchronized(page) { if (!page.initialized)
                            try { page.wait(); } catch (Exception e) { throw new RuntimeException(e); } }

        if (readBytes) ps.readPage(pageid, page.buf, 0);
        page.touch();

        synchronized(page) {
            if (!page.initialized) {
                page.initialized = true;
                page.notifyAll();
            }
        }
        return page;
    }

    /** Write-through */
    public void writePage(int pageid, byte[] buf, int ofs) {
        CachedPage page = getPage(pageid, false);
        System.arraycopy(buf, ofs, page.buf, 0, ps.getPageSize());
        page.setDirty();
        page.flush();
    }

    public void readPage(int pageid, byte[] buf, int ofs) {
        CachedPage page = getPage(pageid, true);
        System.arraycopy(page.buf, 0, buf, ofs, ps.getPageSize());
    }

    /** Sets the cache size, evicting pages if necessary. */
    public void setCacheSize(int cacheSize) {

        synchronized(CachingPageStorage.this) {
            this.cacheSize = cacheSize;
        }
        // In order to avoid holding the global lock during evictions
        // we have to do this in a strange manner, and we might have
        // to try multiple times.
        while(true) {
            CachedPage cp = null;
            synchronized(CachingPageStorage.this) {
                if (cache.size() <= this.cacheSize) return;
                cp = cache.entrySet().iterator().next().getValue();
            }
            // do the evictions while we aren't holding the lock
            cp.evict();
        }
    }

    public void fsync(int pageid) { ps.fsync(pageid); }
    public void fsync() { ps.fsync(); }

    /** A page which is currently in the cache. */
    public class CachedPage {
        private final int     pageid;
        private final byte[]  buf;
        private       boolean isDirty;
        private       boolean initialized = false;

        private CachedPage(int pageid, byte[] buf) {
            this.pageid = pageid;
            this.buf = buf;
            this.isDirty = false;
            synchronized(CachingPageStorage.this) {
                assert !allCachedPages.containsKey(this);
                allCachedPages.put(pageid, this);
            }
        }

        public byte[] getBuf() { return buf; }
        public int    getPageId() { return pageid; }

        /** Keep in mind that calling setDirty()/flush() after eviction is perfectly valid! */
        void evict() {
            boolean needFlush = false;
            synchronized(CachingPageStorage.this) {
                synchronized(this) {
                    // be careful here: we're holding a local lock!
                    cache.remove(pageid);
                    needFlush = isDirty();
                }
            }            
            if (needFlush) flush();
        }

        /** 
         *  Indicate that this page has been "used" for purposes of
         *  eviction.  Touching an evicted page will un-evict it.
         */
        public void touch() {
            synchronized(CachingPageStorage.this) {
                // will fail if evicted, but that's okay
                cache.remove(pageid);
            }
            while(true) {
                // use this spinlock-like approach to avoid evicting while holding the lock
                synchronized(CachingPageStorage.this) {
                    if (cache.size() < cacheSize) {
                        cache.put(pageid, this);
                        return;
                    }
                }
                while (cache.size() >= cacheSize)
                    cache.entrySet().iterator().next().getValue().evict();
            }
        }

        /**
         *  Marks a page as dirty.  Any subsequent writes to this page
         *  must be accompanied by one of the following:
         *
         *    1. A call to isDirty() which returns false before the write.
         *    2. A call to flush() before the write.
         *    3. A call to setDirty() [and flush()] after the write.
         *
         *  Note that in case #3 it is possible for a half-modified
         *  page to be written the disk.  If this is a problem (for
         *  example, if the program crashes or power to the computer
         *  fails between the half-modified page being written and the
         *  next write), use #1 or #2 instead.
         *
         *  Note that these restrictions apply even if asyncFlush is
         *  disabled, because reading in a new page may force the
         *  eviction of any page.
         */
        public void setDirty() {
            synchronized(this) {
                // be careful here: we're holding a local lock!
                this.isDirty = true;
            }
            touch();
        }

        /**
         *  Write the page to disk.  When this method returns,
         *  isDirty() is guaranteed to be false until the next call to
         *  setDirty().
         */
        public void flush() {
            synchronized(this) {
                // be careful here: we're holding a local lock!
                if (!isDirty) return;
                ps.writePage(pageid, buf, 0);
                isDirty = false;
            }
        }

        /** indicates whether or not the page is dirty */
        public boolean isDirty() {
            synchronized(this) {
                // be careful here: we're holding a local lock!
                return isDirty;
            }
        }
    }

}