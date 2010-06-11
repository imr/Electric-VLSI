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
 *  An associative operation on unboxed values.
 *
 *  http://en.wikipedia.org/wiki/Monoid
 *  http://en.wikipedia.org/wiki/Semigroup
 */
public class MinMaxOperation<K extends Serializable, V extends Serializable & Comparable>
    extends
        UnboxedPair< Pair<K,V> , Pair<K,V> >
    implements
        Serializable,
        AssociativeOperation< Pair< Pair<K,V> , Pair<K,V> > >,
        AssociativeCommutativeOperation< Pair< Pair<K,V> , Pair<K,V> > >,
        UnboxedFunction< Pair<K,V> , Pair< Pair<K,V> , Pair<K,V> > > {

    private final Unboxed<K> uk;
    private final UnboxedComparable<V> uv;

    public MinMaxOperation(Unboxed<K> uk, UnboxedComparable<V> uv) {
        super( new UnboxedPair(uk, uv),
               new UnboxedPair(uk, uv) );
        this.uk = uk;
        this.uv = uv;
    }

    public void call(byte[] buf_arg, int ofs_arg,
                     byte[] buf_result, int ofs_result) {
        System.arraycopy(buf_arg, ofs_arg, buf_result,
                         ofs_result, uk.getSize()+uv.getSize());
        System.arraycopy(buf_arg, ofs_arg, buf_result,
                         ofs_result+uk.getSize()+uv.getSize(), uk.getSize()+uv.getSize());
    }

    public void multiply(byte[] buf1, int ofs1,
                         byte[] buf2, int ofs2,
                         byte[] buf_dest, int ofs_dest) {
        int compareMin = uv.compare(buf1, ofs1+uk.getSize(), buf2, ofs2+uk.getSize());
        int compareMax = uv.compare(buf1, ofs1+2*uk.getSize()+uv.getSize(), buf2, ofs2+2*uk.getSize()+uv.getSize());
        System.arraycopy(compareMin<0 ? buf1 : buf2,
                         compareMin<0 ? ofs1 : ofs2,
                         buf_dest,
                         ofs_dest,
                         uk.getSize()+uv.getSize());
        System.arraycopy(compareMax>=0 ? buf1 : buf2,
                         (compareMax>=0 ? ofs1 : ofs2)+uk.getSize()+uv.getSize(),
                         buf_dest,
                         ofs_dest+uk.getSize()+uv.getSize(),
                         uk.getSize()+uv.getSize());
    }

}
