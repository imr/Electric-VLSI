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
package com.sun.electric.tool.btree.unboxed;

import java.io.*;

/**
 *  An Unboxed for some type, paired with an associative operator on
 *  that type and an identity value for the operator.
 *
 *  http://en.wikipedia.org/wiki/Monoid
 */
public interface UnboxedMonoid<V extends Serializable>
    extends Unboxed<V> {

    /** Write the monoid's identity value into the buffer at ofs */
    public void identity(byte[] buf, int ofs);
  
    /**
     *  Compute (buf1,ofs1)*(buf2,ofs2) and write it to (buf_dest,ofs_dest).
     *  MUST support the case where (buf1,ofs1)==(buf_dest,ofs_dest)
     *  or (buf2,ofs2)==(buf_dest,ofs_dest) or ther is some overlap.
     */
    public void multiply(byte[] buf1, int ofs1,
                         byte[] buf2, int ofs2,
                         byte[] buf_dest, int ofs_dest);
}