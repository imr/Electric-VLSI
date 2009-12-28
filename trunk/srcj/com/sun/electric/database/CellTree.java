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
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.technology.TechPool;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * CellTree consists of top CellBackup and all CellTrees for all subcells.
 * It can compute Cell bounds and shape of Exports.
 */
public class CellTree {

    public static final CellTree[] NULL_ARRAY = {};
    public static final ImmutableArrayList<CellTree> EMPTY_LIST = new ImmutableArrayList<CellTree>(NULL_ARRAY);
    /**
     * top CellBackup
     */
    public final CellBackup top;
    final CellTree[] subTrees;
    /**
     * TechPool containing all Technologies used in this CellTree
     */
    public final TechPool techPool;
    /**
     * All cells used in this CellTree
     */
    public final Set<CellId> allCells;
    private ERectangle bounds;

    private EquivPorts equivPorts;

    private CellTree(CellBackup top, CellTree[] subTrees, TechPool techPool, Set<CellId> allCells) {
        this.top = top;
        this.subTrees = subTrees;
        this.techPool = techPool;
        this.allCells = allCells;
    }

    public static CellTree newInstance(ImmutableCell d, TechPool techPool) {
        CellBackup top = CellBackup.newInstance(d, techPool);
        assert top.cellRevision.cellUsages.length == 0;
        return new CellTree(top, NULL_ARRAY, top.techPool, Collections.singleton(top.cellRevision.d.cellId));
    }

    /**
     * Returns CellTree which differs from this CellTree.
     * @param top new top CellBackup
     * @param subTrees new subCellTrees
     * @param superPool TechPool which contains
     * @return CellTree which differs from this CellTree.
     */
    public CellTree with(CellBackup top, CellTree[] subTrees, TechPool superPool) {
        // Canonize subTrees
        if (Arrays.equals(this.subTrees, subTrees)) {
            subTrees = this.subTrees;
        } else {
            subTrees = subTrees.clone();
            int l = subTrees.length;
            while (l > 0 && subTrees[l - 1] == null) {
                l--;
            }
            if (l == 0) {
                subTrees = NULL_ARRAY;
            } else if (l != subTrees.length) {
                CellTree[] newSubTrees = null;
                if (l == this.subTrees.length) {
                    newSubTrees = this.subTrees;
                    for (int i = 0; i < l; i++) {
                        if (newSubTrees[i] != subTrees[i]) {
                            newSubTrees = null;
                            break;
                        }
                    }
                }
                if (newSubTrees == null) {
                    newSubTrees = new CellTree[l];
                    System.arraycopy(subTrees, 0, newSubTrees, 0, l);
                }
                subTrees = newSubTrees;
            }
        }
        // Check if unchanged
        if (this.top == top && this.subTrees == subTrees) {
            return this;
        }

        // Check if subTrees match the top backup
        // Check technologies against superPool
        // Compute cells and technologies used in new tree
        CellRevision cellRevision = top.cellRevision;
        CellId cellId = cellRevision.d.cellId;
        BitSet techUsages = new BitSet();
        if (top.techPool != superPool.restrict(cellRevision.techUsages, top.techPool)) {
            throw new IllegalArgumentException();
        }
        techUsages.or(cellRevision.techUsages);
        HashSet<CellId> allCellsAccum = new HashSet<CellId>();
        for (int i = 0; i < cellRevision.cellUsages.length; i++) {
            CellRevision.CellUsageInfo cui = cellRevision.cellUsages[i];
            CellTree subTree = subTrees[i];
            if (cui == null) {
                if (subTree != null) {
                    throw new IllegalArgumentException();
                }
                continue;
            }
            BitSet subTechUsages = subTree.techPool.getTechUsages();
            if (subTree.techPool != superPool.restrict(subTechUsages, subTree.techPool)) {
                throw new IllegalArgumentException();
            }
            techUsages.or(subTechUsages);
            allCellsAccum.addAll(subTree.allCells);
            CellRevision subCellRevision = subTree.top.cellRevision;
            if (subCellRevision.d.cellId != cellId.getUsageIn(i).protoId) {
                throw new IllegalArgumentException();
            }
            cui.checkUsage(subCellRevision);
        }

        // Check for recursion
        if (allCellsAccum.contains(cellId)) {
            throw new IllegalArgumentException("Recursive " + cellId);
        }
        // Canonize new allCells
        allCellsAccum.add(cellId);
        Set<CellId> allCells;
        if (allCellsAccum.equals(this.allCells)) {
            allCells = this.allCells;
        } else if (allCellsAccum.size() == 1) {
            allCells = Collections.singleton(cellId);
        } else {
            allCells = Collections.unmodifiableSet(allCellsAccum);
        }
        assert allCellsAccum.equals(allCells);

        // Construct new CellTree
        TechPool techPool = superPool.restrict(techUsages, this.techPool);
        CellTree newCellTree = new CellTree(top, subTrees, techPool, allCells);

        if (this.top == top) {
            // Try to reuse cell bounds
            if (this.bounds != null) {
                assert newCellTree.subTrees.length == this.subTrees.length;
                ERectangle cellBounds = this.bounds;
                for (int i = 0; i < this.subTrees.length; i++) {
                    CellTree oldSubTree = this.subTrees[i];
                    if (oldSubTree == null) continue;
                    assert oldSubTree.bounds != null;
                    if (!newCellTree.subTrees[i].getBounds().equals(oldSubTree.bounds)) {
                        cellBounds = null;
                        break;
                    }
                }
                if (cellBounds != null) {
                    newCellTree.bounds = cellBounds;
                }
            }
            // Try to reuse NetCell
            if (this.equivPorts != null) {
                assert newCellTree.subTrees.length == this.subTrees.length;
                EquivPorts netCell = this.equivPorts;
                for (int i = 0; i < this.subTrees.length; i++) {
                    CellTree oldSubTree = this.subTrees[i];
                    if (oldSubTree == null) continue;
                    assert oldSubTree.equivPorts != null;
                    if (!newCellTree.subTrees[i].getEquivPorts().equalsPorts(oldSubTree.equivPorts)) {
                        netCell = null;
                        break;
                    }
                }
                if (netCell != null) {
                    newCellTree.equivPorts = netCell;
                }
            }
        }

        // Return the new CellTree
        return newCellTree;
    }

    public boolean sameNetlist(CellTree that) {
        if (this.top != that.top) {
            return false;
        }
        for (int i = 0; i < this.subTrees.length; i++) {
            CellTree thisSubTree = this.subTrees[i];
            if (thisSubTree == null) continue;
            if (!this.subTrees[i].getEquivPorts().equalsPorts(that.subTrees[i].getEquivPorts())) {
                return false;
            }
        }
        return true;
    }

    public CellTree[] getSubTrees() {
        return subTrees.clone();
    }

    /**
     * Returns cell bounds of this CellTree
     * @return cell bounds of this CellTree
     */
    public ERectangle getBounds() {
        if (bounds == null) {
            bounds = computeBounds(null);
        }
        return bounds;
    }

    private ERectangle computeBounds(ERectangle candidateBounds) {
        CellRevision cellRevision = top.cellRevision;

        // Collect subcell bounds
        IdentityHashMap<CellId, ERectangle> subCellBounds = new IdentityHashMap<CellId, ERectangle>(cellRevision.cellUsages.length);
        for (CellTree subTree : subTrees) {
            if (subTree == null) {
                continue;
            }
            subCellBounds.put(subTree.top.cellRevision.d.cellId, subTree.getBounds());
        }

        double cellLowX, cellHighX, cellLowY, cellHighY;
        boolean boundsEmpty = true;
        cellLowX = cellHighX = cellLowY = cellHighY = 0;

        Rectangle2D.Double sb = new Rectangle2D.Double();
        for (ImmutableNodeInst n : top.cellRevision.nodes) {
            if (!(n.protoId instanceof CellId)) {
                continue;
            }

            ERectangle b = subCellBounds.get((CellId) n.protoId);
            n.orient.rectangleBounds(b.getMinX(), b.getMinY(), b.getMaxX(), b.getMaxY(),
                    n.anchor.getX(), n.anchor.getY(), sb);

            double lowx = sb.getMinX();
            double highx = sb.getMaxX();
            double lowy = sb.getMinY();
            double highy = sb.getMaxY();
            if (boundsEmpty) {
                boundsEmpty = false;
                cellLowX = lowx;
                cellHighX = highx;
                cellLowY = lowy;
                cellHighY = highy;
            } else {
                if (lowx < cellLowX) {
                    cellLowX = lowx;
                }
                if (highx > cellHighX) {
                    cellHighX = highx;
                }
                if (lowy < cellLowY) {
                    cellLowY = lowy;
                }
                if (highy > cellHighY) {
                    cellHighY = highy;
                }
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
        if (candidateBounds != null && gridMinX == candidateBounds.getGridMinX() && gridMinY == candidateBounds.getGridMinY()
                && gridMaxX == candidateBounds.getGridMaxX() && gridMaxY == candidateBounds.getGridMaxY()) {
            return candidateBounds;
        }
        return ERectangle.fromGrid(gridMinX, gridMinY, gridMaxX - gridMinX, gridMaxY - gridMinY);
    }

    public EquivPorts getEquivPorts() {
        if (equivPorts == null) {
            equivPorts = new EquivPorts(this);
        }
        return equivPorts;
    }

    public EquivPorts computeEquivPorts() {
        return new EquivPorts(this);
    }

    public void check() {
        top.check();
        CellId cellId = top.cellRevision.d.cellId;
        BitSet techUsages = new BitSet();
        techUsages.or(top.cellRevision.techUsages);
        assert subTrees.length == top.cellRevision.cellUsages.length;
        HashSet<CellId> allCells = new HashSet<CellId>();
        for (int i = 0; i < subTrees.length; i++) {
            CellTree subTree = subTrees[i];
            CellRevision.CellUsageInfo cui = top.cellRevision.cellUsages[i];
            if (cui == null) {
                assert subTree == null;
                continue;
            }
            CellRevision subCellRevision = subTree.top.cellRevision;
            CellUsage cu = cellId.getUsageIn(i);
            assert subCellRevision.d.cellId == cu.protoId;
            cui.checkUsage(subCellRevision);
            BitSet subTechUsage = subTree.techPool.getTechUsages();
            assert subTree.techPool == techPool.restrict(subTechUsage, subTree.techPool);
            techUsages.or(subTechUsage);
            allCells.addAll(subTree.allCells);
        }
        assert top.techPool == techPool.restrict(top.cellRevision.techUsages, top.techPool);
        assert techUsages.equals(techPool.getTechUsages());
        assert !allCells.contains(cellId);
        allCells.add(cellId);
        assert allCells.equals(this.allCells);
        if (bounds != null) {
            assert bounds == computeBounds(bounds);
        }
    }

    @Override
    public String toString() {
        return top.toString();
    }
}
