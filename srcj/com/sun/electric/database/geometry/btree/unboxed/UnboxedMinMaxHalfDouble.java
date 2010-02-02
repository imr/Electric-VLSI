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
package com.sun.electric.database.geometry.btree.unboxed;

import java.io.*;

/** an example of an AssociativeCommutativeOperation */
public class UnboxedMinMaxHalfDouble<K extends Serializable>
    extends UnboxedPair<Double,Double>
    implements
        AssociativeCommutativeOperation<Pair<Double,Double>>,
        UnboxedFunction<Pair<K,Double>,Pair<Double,Double>> {

    public static final UnboxedMinMaxHalfDouble instance = new UnboxedMinMaxHalfDouble();

    private final UnboxedHalfDouble uhd = UnboxedHalfDouble.instance;

    public UnboxedMinMaxHalfDouble() { super(UnboxedHalfDouble.instance, UnboxedHalfDouble.instance); }

    public void call(byte[] buf_kv, int ofs_kv,
                     byte[] buf_s, int ofs_s) {
        /*
        System.arraycopy(buf_kv, ofs_v+FIXME, buf_s, ofs_s,               uhd.getSize());
        System.arraycopy(buf_kv, ofs_v+FIXME, buf_s, ofs_s+uhd.getSize(), uhd.getSize());
        */
        throw new RuntimeException("not implemented");
    }
  
    public void multiply(byte[] buf1, int ofs1,
                         byte[] buf2, int ofs2,
                         byte[] buf_dest, int ofs_dest) {
        float min1 = uhd.deserializeFloat(buf1, ofs1);
        float max1 = uhd.deserializeFloat(buf1, ofs1+uhd.getSize());
        float min2 = uhd.deserializeFloat(buf2, ofs2);
        float max2 = uhd.deserializeFloat(buf2, ofs2+uhd.getSize());
        uhd.serializeFloat(Math.min(min1,min2), buf_dest, ofs_dest);
        uhd.serializeFloat(Math.max(max1,max2), buf_dest, ofs_dest+uhd.getSize());
    }

}