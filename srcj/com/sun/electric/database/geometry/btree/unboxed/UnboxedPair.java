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
import java.util.*;

/** an implementation of Pair<A,B> in unboxed form */
public class UnboxedPair<A extends Serializable,B extends Serializable>
    implements Unboxed<Pair<A,B>> {

    private final Unboxed<A> ua;
    private final Unboxed<B> ub;
    public UnboxedPair(Unboxed<A> ua, Unboxed<B> ub) { this.ua = ua; this.ub = ub; }
    public int getSize() { return ua.getSize()+ub.getSize(); }
    public Pair<A,B> deserialize(byte[] buf, int ofs) {
        A a = ua.deserialize(buf, ofs);
        B b = ub.deserialize(buf, ofs+ua.getSize());
        return new Pair<A,B>(a,b);
    }
    public void serialize(Pair<A,B> sme, byte[] buf, int ofs) {
        ua.serialize(sme.getKey(), buf, ofs);
        ub.serialize(sme.getValue(), buf, ofs+ua.getSize());
    }
}
