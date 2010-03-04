/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
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
package com.sun.electric.tool.simulation;

import java.io.*;
import java.util.*;
import com.sun.electric.database.geometry.btree.*;
import com.sun.electric.database.geometry.btree.unboxed.*;
import com.sun.electric.tool.simulation.*;

public class ScalarSignal extends BTreeSignal<ScalarSample> {

    public ScalarSignal(Analysis analysis, String signalName, String signalContext) {
        super(analysis, signalName, signalContext, getTree());
    }

    private static CachingPageStorage ps = null;
    private static BTree<Double,ScalarSample,Serializable> getTree() {
        if (ps==null)
            try {
                long highWaterMarkInBytes = 50 * 1024 * 1024;
                PageStorage fps = FilePageStorage.create();
                PageStorage ops = new OverflowPageStorage(new MemoryPageStorage(fps.getPageSize()), fps, highWaterMarkInBytes);
                ps = new CachingPageStorageWrapper(ops, 16 * 1024, false);
                //ps = new MemoryPageStorage(256);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        return new BTree<Double,ScalarSample,Serializable>
            (ps, UnboxedHalfDouble.instance, ScalarSample.unboxer, null, null);
    }
}
