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

/**
 *  Stores and retrieves fixed-size byte sequences indexed by a page
 *  number.
 */
public abstract class PageStorage {

    /** returns the size, in bytes, of each page */
    public abstract int getPageSize();

    /** creates a new page with undefined contents; returns its pageid */
    public abstract int  createPage();

    /** writes a page; throws an exception if the page did not exist */ 
    public abstract void writePage(int pageid, byte[] buf, int ofs);

    /** reads a page */
    public abstract byte[] readPage(int pageid, byte[] buf, int ofs);

    /** ensure that the designated page is written to permanent storage */
    public abstract void flush(int pageid);
    
    /** ensure that the all pages are written to permanent storage */
    public abstract void flush();

}