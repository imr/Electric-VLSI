/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MinMaxOperation.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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

/**
 *  Maintains the least upper bound and greatest lower bound of a
 *  collection of values.
 */
public abstract class LatticeOperation<V extends Serializable>
    extends
        UnboxedPair< V, V >
    implements
        Serializable {

    private final Unboxed<V> uv;

    public LatticeOperation(Unboxed<V> uv) {
        super(uv, uv);
        this.uv = uv;
    }

    public abstract void lub(byte[] buf1, int ofs1, byte[] buf2, int ofs2, byte[] dest, int dest_ofs);
    public abstract void glb(byte[] buf1, int ofs1, byte[] buf2, int ofs2, byte[] dest, int dest_ofs);

}
