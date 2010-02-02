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

/** A 32-bit <tt>float</tt> in unboxed form */
public class UnboxedFloat implements UnboxedComparable<Float> {
    public int getSize() { return 4; }
    public Float deserialize(byte[] buf, int ofs) { return new Float(deserializeFloat(buf, ofs)); }
    public void serialize(Float k, byte[] buf, int ofs) { serialize(k.floatValue(), buf, ofs); }
    public float deserializeFloat(byte[] buf, int ofs) {
        return
            Float.intBitsToFloat(((buf[ofs+0] & 0xff) <<  0) |
                                 ((buf[ofs+1] & 0xff) <<  8) |
                                 ((buf[ofs+2] & 0xff) << 16) |
                                 ((buf[ofs+3] & 0xff) << 24));
    }
    public void serializeFloat(float f, byte[] buf, int ofs) {
        int i = Float.floatToRawIntBits(f);
        buf[ofs+0] = (byte)((i >>  0) & 0xff);
        buf[ofs+1] = (byte)((i >>  8) & 0xff);
        buf[ofs+2] = (byte)((i >> 16) & 0xff);
        buf[ofs+3] = (byte)((i >> 24) & 0xff);
    }
    public int compare(byte[] buf1, int ofs1, byte[] buf2, int ofs2) {
        float f1 = deserializeFloat(buf1, ofs1);
        float f2 = deserializeFloat(buf2, ofs2);
        return Float.compare(f1, f2);
    }
}
