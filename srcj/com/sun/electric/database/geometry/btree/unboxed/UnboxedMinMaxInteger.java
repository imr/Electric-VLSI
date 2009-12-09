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
package com.sun.electric.database.geometry.btree.unboxed;

import java.io.*;

public class UnboxedMinMaxInteger<K extends Serializable>
    extends UnboxedPair<Integer,Integer>
    implements UnboxedCommutativeMonoid<K,Integer,Pair<Integer,Integer>> {

    public static final UnboxedMinMaxInteger instance = new UnboxedMinMaxInteger();

    private final UnboxedInt uhd = UnboxedInt.instance;

    public UnboxedMinMaxInteger() { super(UnboxedInt.instance, UnboxedInt.instance); }

    public void identity(byte[] buf, int ofs) {
        uhd.serializeInt(Integer.MAX_VALUE, buf, ofs);
        uhd.serializeInt(Integer.MIN_VALUE, buf, ofs+uhd.getSize());
    }

    public void inject(byte[] buf_k, int ofs_k,
                       byte[] buf_v, int ofs_v,
                       byte[] buf_s, int ofs_s) {
        System.arraycopy(buf_v, ofs_v, buf_s, ofs_s,               uhd.getSize());
        System.arraycopy(buf_v, ofs_v, buf_s, ofs_s+uhd.getSize(), uhd.getSize());
    }
  
    public void multiply(byte[] buf1, int ofs1,
                         byte[] buf2, int ofs2,
                         byte[] buf_dest, int ofs_dest) {
        int min1 = uhd.deserializeInt(buf1, ofs1);
        int max1 = uhd.deserializeInt(buf1, ofs1+uhd.getSize());
        int min2 = uhd.deserializeInt(buf2, ofs2);
        int max2 = uhd.deserializeInt(buf2, ofs2+uhd.getSize());
        uhd.serializeInt(Math.min(min1,min2), buf_dest, ofs_dest);
        uhd.serializeInt(Math.max(max1,max2), buf_dest, ofs_dest+uhd.getSize());
    }

}