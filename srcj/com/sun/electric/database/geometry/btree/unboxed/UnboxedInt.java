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

/** A 32-bit <tt>int</tt> in unboxed form */
public class UnboxedInt implements UnboxedComparable<Integer> {
    public static final UnboxedInt instance = new UnboxedInt();
    public int getSize() { return 4; }
    public Integer deserialize(byte[] buf, int ofs) { return new Integer(deserializeInt(buf, ofs)); }
    public void serialize(Integer k, byte[] buf, int ofs) { serializeInt(k.intValue(), buf, ofs); }
    public int deserializeInt(byte[] buf, int ofs) {
        return
            ((buf[ofs+0] & 0xff) <<  0) |
            ((buf[ofs+1] & 0xff) <<  8) |
            ((buf[ofs+2] & 0xff) << 16) |
            ((buf[ofs+3] & 0xff) << 24);
    }
    public void serializeInt(int i, byte[] buf, int ofs) {
        buf[ofs+0] = (byte)((i >>  0) & 0xff);
        buf[ofs+1] = (byte)((i >>  8) & 0xff);
        buf[ofs+2] = (byte)((i >> 16) & 0xff);
        buf[ofs+3] = (byte)((i >> 24) & 0xff);
    }
    public int compare(byte[] buf1, int ofs1, byte[] buf2, int ofs2) {
        int buf1msb = buf1[ofs1+3] & 0xff;
        int buf2msb = buf2[ofs2+3] & 0xff;
        boolean buf1negative = (buf1msb >> 7) != 0;
        boolean buf2negative = (buf2msb >> 7) != 0;
        if ( buf1negative && !buf2negative) return -1;
        if (!buf1negative &&  buf2negative) return  1;
        buf1msb &= 127;
        buf2msb &= 127;
        int i;
        i = (buf1msb            ) - (buf2msb            ); if (i!=0) return i;
        i = (buf1[ofs1+2] & 0xff) - (buf2[ofs2+2] & 0xff); if (i!=0) return i;
        i = (buf1[ofs1+1] & 0xff) - (buf2[ofs2+1] & 0xff); if (i!=0) return i;
        i = (buf1[ofs1+0] & 0xff) - (buf2[ofs2+0] & 0xff); if (i!=0) return i;
        return 0;
    }
}
