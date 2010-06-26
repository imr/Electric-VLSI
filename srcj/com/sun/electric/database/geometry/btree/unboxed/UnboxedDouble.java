/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UnboxedDouble.java
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

/** A 64-bit <tt>double</tt>  */
public class UnboxedDouble implements UnboxedComparable<Double> {
    public static final UnboxedDouble instance = new UnboxedDouble();
    public int getSize() { return 8; }
    public Double deserialize(byte[] buf, int ofs) { return new Double(deserializeDouble(buf, ofs)); }
    public void serialize(Double k, byte[] buf, int ofs) { serializeDouble(k.doubleValue(), buf, ofs); }
    public Double deserializeDouble(byte[] buf, int ofs) {
        return
            Double.longBitsToDouble(((buf[ofs+0] & 0xffL) <<  0) |
                                    ((buf[ofs+1] & 0xffL) <<  8) |
                                    ((buf[ofs+2] & 0xffL) << 16) |
                                    ((buf[ofs+3] & 0xffL) << 24) |
                                    ((buf[ofs+4] & 0xffL) << 32) |
                                    ((buf[ofs+5] & 0xffL) << 40) |
                                    ((buf[ofs+6] & 0xffL) << 48) |
                                    ((buf[ofs+7] & 0xffL) << 56));
    }
    public void serializeDouble(double f, byte[] buf, int ofs) {
        long i = Double.doubleToRawLongBits(f);
        buf[ofs+0] = (byte)((i >>  0) & 0xffL);
        buf[ofs+1] = (byte)((i >>  8) & 0xffL);
        buf[ofs+2] = (byte)((i >> 16) & 0xffL);
        buf[ofs+3] = (byte)((i >> 24) & 0xffL);
        buf[ofs+4] = (byte)((i >> 32) & 0xffL);
        buf[ofs+5] = (byte)((i >> 40) & 0xffL);
        buf[ofs+6] = (byte)((i >> 48) & 0xffL);
        buf[ofs+7] = (byte)((i >> 56) & 0xffL);
    }
    public int compare(byte[] buf1, int ofs1, byte[] buf2, int ofs2) {
        double f1 = deserializeDouble(buf1, ofs1);
        double f2 = deserializeDouble(buf2, ofs2);
        return Double.compare(f1, f2);
    }
}