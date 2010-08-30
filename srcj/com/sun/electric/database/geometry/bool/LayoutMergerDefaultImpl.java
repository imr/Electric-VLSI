/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayoutMergerDefaultImpl.java
 * Written by Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.geometry.bool;

import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.technology.Layer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 *
 * @author dn146861
 */
class LayoutMergerDefaultImpl implements LayoutMerger {

    private final VectorCache vectorCache;
    private final Cell topCell;

    private static final int TMP_FILE_THRESHOLD = 1000000;

    static class Factory extends LayoutMergerFactory {
        @Override
        public LayoutMerger newMerger(Cell topCell) {
            return new LayoutMergerDefaultImpl(topCell);
        }
    }

    private LayoutMergerDefaultImpl(Cell topCell) {
        vectorCache = new VectorCache(topCell.getDatabase().backup());
        vectorCache.scanLayers(topCell.getId());
        this.topCell = topCell;
    }

    public Collection<Layer> getLayers() {
        return vectorCache.getLayers();
    }

    public boolean canMerge(Layer layer) {
        return !vectorCache.isBadLayer(layer);
    }

    public Iterable<PolyBase.PolyBaseTree> merge(Layer layer) {
        Iterable<PolyBase.PolyBaseTree> trees = null;
        try {
            boolean inMemory = vectorCache.getNumFlatBoxes(topCell.getId(), layer) <= TMP_FILE_THRESHOLD;
            ByteArrayOutputStream bout = null;
            File file = null;
            DataOutputStream out;
            if (inMemory) {
                bout = new ByteArrayOutputStream();
                out = new DataOutputStream(bout);
            } else {
                file = File.createTempFile("Electric", "DRC");
                out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            }
            DeltaMerge dm = collectLayer(vectorCache, layer);
            dm.loop(out);
            out.close();
            dm = null;
            DataInputStream inpS;
            if (inMemory) {
                byte[] ba = bout.toByteArray();
                bout = null;
                inpS = new DataInputStream(new ByteArrayInputStream(ba));
            } else {
                inpS = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            }

            UnloadPolys up = new UnloadPolys();
            trees = up.loop(inpS, false);
            inpS.close();
            if (!inMemory) {
                file.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return trees;

    }
    
    private DeltaMerge collectLayer(VectorCache vce, Layer layer) {
        final DeltaMerge dm = new DeltaMerge();
        VectorCache.PutRectangle putRectangle = new VectorCache.PutRectangle() {
            public void put(int lx, int ly, int hx, int hy) {
                dm.put(lx, ly, hx, hy);
            }
        };
        vce.collectLayer(layer, topCell.getId(), false, putRectangle);
        return dm;
    }
}
