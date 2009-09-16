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

/**
 *  An LRU-caching wrapper around PageStorage; currently does
 *  write-through caching only.  It would be nice to have write-back
 *  caching.
 *
 *  Possible feature: background flushing of dirty pages in a
 *  low-priority thread.
 *
 *  Possible feature: use instances of SoftReference to react to
 *  memory pressure.  Probably only usable for non-dirty pages.
 *  Probably requires previous feature.
 */
public class CachingPageStorage extends PageStorage {

    private final PageStorage                    ps;
    private       byte[][]                       cache = null;
    private       boolean[]                      dirty = null;
    private       LinkedHashMap<Integer,Integer> cacheMap = null;
    private       int                            cacheSize;
    private       int                            numCacheEntries = 0;

    public CachingPageStorage(PageStorage ps, int cacheSize) {
        this.ps = ps;
        setCacheSize(cacheSize);
    }

    public int getPageSize() { return ps.getPageSize(); }

    public int createPage() { return ps.createPage(); }

    public void writePage(int pageid, byte[] buf, int ofs) {
        putCache(pageid, buf, ofs, true);
    }

    public byte[] readPage(int pageid, byte[] buf, int ofs) {
        Integer cachePos = cacheMap.get(pageid);
        if (cachePos!=null)
            return cache[cachePos.intValue()];
        buf = new byte[ps.getPageSize()];
        ps.readPage(pageid, buf, ofs);
        putCache(pageid, buf, ofs, false);
        return buf;
    }

    private void putCache(int pageid, byte[] buf, int ofs, boolean makeDirty) {
        if (cacheMap.get(pageid)!=null) {
            int pos = cacheMap.get(pageid);
            assert !(!makeDirty && dirty[pos]);
            assert buf==cache[pos];
            assert ofs==0;
            dirty[pos] = makeDirty;
        } else if (numCacheEntries >= cacheSize) {
            Map.Entry<Integer,Integer> ent = cacheMap.entrySet().iterator().next();
            int oldKey = ent.getKey();
            int oldVal = ent.getValue();
            // have to remove-then-put in order to be sure we alter the access order
            cacheMap.remove(oldKey);
            cacheMap.put(pageid, oldVal);
            if (dirty[oldVal]) ps.writePage(oldKey, cache[oldVal], 0);
            dirty[oldVal] = makeDirty;
            cache[oldVal] = buf;
        } else {
            //cache[numCacheEntries] = new byte[getPageSize()];
            assert ofs==0;
            cache[numCacheEntries] = buf;
            dirty[numCacheEntries] = makeDirty;
            cacheMap.put(pageid, numCacheEntries);
            numCacheEntries++;
        }
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
        LinkedHashMap<Integer,Integer> newCacheMap =
            new LinkedHashMap<Integer,Integer>(cacheSize+3, 0.75f, true);
        byte[][] newCache = new byte[cacheSize][];
        boolean[] newDirty = new boolean[cacheSize];
        numCacheEntries = 0;
        // this is actually wrong, because the iteration order is LEAST recently accessed first
        if (cacheMap!=null)
            for(Map.Entry<Integer,Integer> e : cacheMap.entrySet()) {
                if (numCacheEntries >= cacheSize) break;
                newCacheMap.put(e.getKey(), numCacheEntries);
                newCache[numCacheEntries] = cache[e.getValue()];
                dirty[numCacheEntries] = dirty[e.getValue()];
                numCacheEntries++;
            }
        this.dirty = newDirty;
        this.cache = newCache;
        this.cacheMap = newCacheMap;
    }

    public void flush(int pageid) { ps.flush(pageid); }
    public void flush() { ps.flush(); }
}