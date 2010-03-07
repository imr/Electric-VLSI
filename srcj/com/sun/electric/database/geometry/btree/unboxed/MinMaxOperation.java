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
public class MinMaxOperation<K extends Serializable, V extends Serializable>
    extends
        UnboxedPair< Pair<K,V> , Pair<K,V> >
    implements
        Serializable,
        AssociativeOperation< Pair< Pair<K,V> , Pair<K,V> > >,
        AssociativeCommutativeOperation< Pair< Pair<K,V> , Pair<K,V> > >,
        UnboxedFunction< Pair<K,V> , Pair< Pair<K,V> , Pair<K,V> > > {

    public MinMaxOperation(Unboxed<K> uk, Unboxed<V> uv) {
        super( new UnboxedPair(uk, uv),
               new UnboxedPair(uk, uv) );
    }

    public void call(byte[] buf_a, int ofs_a,
                     byte[] buf_b, int ofs_b) {
        // FIXME: no-op for now
    }

    public void multiply(byte[] buf1, int ofs1,
                         byte[] buf2, int ofs2,
                         byte[] buf_dest, int ofs_dest) {
        // FIXME: no-op for now
    }

}