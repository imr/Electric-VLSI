/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellTree.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.database;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.CellUsage;
import com.sun.electric.technology.TechPool;

import java.awt.geom.Rectangle2D;
import java.util.BitSet;
import java.util.IdentityHashMap;

/**
 * CellTree consists of top CellBackup and all CellTrees for all subcells.
 * It can compute Cell bounds and shape of Exports.
 */
public class CellTree {
    public final CellBackup top;
    public final TechPool techPool;
    private final CellTree[] subTrees;
    private ERectangle bounds;

    private CellTree(CellBackup top, TechPool techPool, CellTree[] subTrees) {
        this.top = top;
        this.techPool = techPool;
        this.subTrees = subTrees;
    }

    public ERectangle getBounds() {
        if (bounds == null)
            computeBounds(null);
        return bounds;
    }

    private void computeBounds(ERectangle candidateBounds) {
        CellRevision cellRevision = top.cellRevision;

        // Collect subcell bounds
        IdentityHashMap<CellId,ERectangle> subCellBounds = new IdentityHashMap<CellId,ERectangle>(cellRevision.cellUsages.length);
        for (CellTree subTree: subTrees) {
            if (subTree == null) continue;
            subCellBounds.put(subTree.top.cellRevision.d.cellId, subTree.getBounds());
        }

        double cellLowX, cellHighX, cellLowY, cellHighY;
        boolean boundsEmpty = true;
        cellLowX = cellHighX = cellLowY = cellHighY = 0;

        Rectangle2D.Double sb = new Rectangle2D.Double();
        for (ImmutableNodeInst n: top.cellRevision.nodes) {
            if (!(n.protoId instanceof CellId)) continue;

            ERectangle b = subCellBounds.get((CellId)n.protoId);
            n.orient.rectangleBounds(b.getMinX(), b.getMinY(), b.getMaxX(), b.getMaxY(),
                    n.anchor.getX(), n.anchor.getY(), sb);

            double lowx = sb.getMinX();
            double highx = sb.getMaxX();
            double lowy = sb.getMinY();
            double highy = sb.getMaxY();
            if (boundsEmpty) {
                boundsEmpty = false;
                cellLowX = lowx;   cellHighX = highx;
                cellLowY = lowy;   cellHighY = highy;
            } else {
                if (lowx < cellLowX) cellLowX = lowx;
                if (highx > cellHighX) cellHighX = highx;
                if (lowy < cellLowY) cellLowY = lowy;
                if (highy > cellHighY) cellHighY = highy;
            }
        }
        long gridMinX = DBMath.lambdaToGrid(cellLowX);
        long gridMinY = DBMath.lambdaToGrid(cellLowY);
        long gridMaxX = DBMath.lambdaToGrid(cellHighX);
        long gridMaxY = DBMath.lambdaToGrid(cellHighY);

        ERectangle primitiveBounds = top.getPrimitiveBounds();
        if (primitiveBounds != null) {
            if (boundsEmpty) {
                gridMinX = primitiveBounds.getGridMinX();
                gridMaxX = primitiveBounds.getGridMaxX();
                gridMinY = primitiveBounds.getGridMinY();
                gridMaxY = primitiveBounds.getGridMaxY();
            } else {
                gridMinX = Math.min(gridMinX, primitiveBounds.getGridMinX());
                gridMaxX = Math.max(gridMaxX, primitiveBounds.getGridMaxX());
                gridMinY = Math.min(gridMinY, primitiveBounds.getGridMinY());
                gridMaxY = Math.max(gridMaxY, primitiveBounds.getGridMaxY());
            }
        }
        if (candidateBounds != null && gridMinX == candidateBounds.getGridMinX() && gridMinY == candidateBounds.getGridMinY() &&
                gridMaxX == candidateBounds.getGridMaxX() && gridMaxY == candidateBounds.getGridMaxY())
            bounds = candidateBounds;
        else
            bounds = ERectangle.fromGrid(gridMinX, gridMinY, gridMaxX - gridMinX, gridMaxY - gridMinY);
    }

    void check() {
        CellId cellId = top.cellRevision.d.cellId;
        BitSet techUsages = new BitSet();
        techUsages.or(top.cellRevision.techUsages);
        assert subTrees.length == top.cellRevision.cellUsages.length;
        for (int i = 0; i < subTrees.length; i++) {
            CellTree subTree = subTrees[i];
            CellRevision.CellUsageInfo cui = top.cellRevision.cellUsages[i];
            if (cui == null) {
                assert subTree == null;
                continue;
            }
            CellUsage cu = cellId.getUsageIn(i);
            assert subTree.top.cellRevision.d.cellId == cu.protoId;
            BitSet subTechUsage = subTree.techPool.getTechUsages();
            assert subTree.techPool == techPool.restrict(subTechUsage, subTree.techPool);
            techUsages.or(subTechUsage);
        }
        assert top.techPool == techPool.restrict(top.cellRevision.techUsages, top.techPool);
        assert techUsages.equals(techPool.getTechUsages());
    }
}
