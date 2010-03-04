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

/** An 8-bit <tt>byte</tt> in unboxed form */
public class UnboxedByte implements UnboxedComparable<Byte> {
    public static final UnboxedByte instance = new UnboxedByte();
    public int getSize() { return 1; }
    public Byte deserialize(byte[] buf, int ofs) { return new Byte(deserializeByte(buf, ofs)); }
    public void serialize(Byte k, byte[] buf, int ofs) { serializeByte(k.byteValue(), buf, ofs); }
    public byte deserializeByte(byte[] buf, int ofs) { return buf[ofs]; }
    public void serializeByte(byte b, byte[] buf, int ofs) { buf[ofs] = b; }
    public int compare(byte[] buf1, int ofs1, byte[] buf2, int ofs2) {
        return (buf1[ofs1] & 0xff) - (buf2[ofs2] & 0xff);
    }
}
