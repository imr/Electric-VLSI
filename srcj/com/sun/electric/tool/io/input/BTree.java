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
package com.sun.electric.tool.io.input;
import java.io.Serializable;

/**
 *  A BTree for holding data too large to fit in memory.
 *
 *  The interface below is basically a stripped-down version of
 *  java.util.SortedMap.  The methods removed are those which aren't
 *  strictly necessary (putAll()) or those which would require a great
 *  deal of effort to implement without having all the data in memory
 *  (keySet()).  There is also no support for custom Comparators; the
 *  key class must serialize its instances into a form that sorts
 *  lexographically (and must implement Comparable).  Lastly, this
 *  class does not distinguish between a key which is not in the
 *  collection and a key which is present but has a null value; this
 *  restriction greatly simplifies the Map interface (no need for
 *  containsKey(), etc).
 *
 *  Subclasses implement this interface; initially we will use JDBM,
 *  but that will be replaced at some point with something we can
 *  upload to GNU.  By ensuring that all BTree use happens through
 *  this interface, we guarantee that the changeover is painless.
 *  Files are stored in ${java.io.tmpdir} and may not be re-used
 *  across sessions.  The file format may change without notice.
 *
 *  Keys may be modified or recycled (they are not retained after the
 *  end of a get/put operation), but Values may not (they may be
 *  cached rather than serialized).
 *
 *  @author Adam Megacz <adam.megacz@sun.com>
 */
public interface BTree<K extends Serializable & Comparable, V extends Serializable> {

    /** returns the first key, or null if tree is empty */                 public K    first();
    /** returns the last key, or null if tree is empty */                  public K    last();
    /** returns the least key which is greater than the one given */       public K    next(K key);
    /** returns the greatest key which is less than the one given */       public K    prev(K key);

    /** overwrites previous entry if key already exists */                 public void put(K key, V val);
    /** returns the value associated with a key, or null if none exists */ public V    get(K key);
    /** no error if key doesn't exist */                                   public void remove(K key);
    /** remove all entries */                                              public void clear();

    /** returns the number of keys in the BTree */                         public int  size();
    /** returns the number of keys strictly after the one given */         public int  sizeAfter(K key);
    /** returns the number of keys strictly after the one given */         public int  sizeBefore(K key);

    /** returns the key with the given ordinal index */                    public K    seek(int ordinal);
    /** returns the ordinal index of the given key */                      public int  ordinal(K key);

}
