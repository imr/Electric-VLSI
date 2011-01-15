/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DebugLayoutCell.java
 *
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.api.minarea;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class DebugLayoutCell {
    
    private final String name;
    private final List<ERectangle> rects = new ArrayList<ERectangle>(); 
    
    private static class CellInst {
        private final LayoutCell subCell;
        private final EPoint anchor;
        private final ManhattanOrientation orient;
        
        private CellInst(LayoutCell subCell, EPoint anchor, ManhattanOrientation orient) {
            this.subCell = subCell;
            this.anchor = anchor;
            this.orient = orient;
        }
    }
    
    private final List<CellInst> subCells = new ArrayList<CellInst>();
    private ERectangle boundingBox; 
    
    DebugLayoutCell(String name) {
        this.name = name;
    }

    // cell name
    public String getName() {
        return name;
    }

    // rectangles
    public int getNumRectangles() {
        return rects.size();
    }

    public long getRectangleMinX(int rectangleIndex) {
        return rects.get(rectangleIndex).getGridMinX();
    }

    public long getRectangleMinY(int rectangleIndex) {
        return rects.get(rectangleIndex).getGridMinY();
    }

    public long getRectangleMaxX(int rectangleIndex) {
        return rects.get(rectangleIndex).getGridMaxX();
    }

    public long getRectangleMaxY(int rectangleIndex) {
        return rects.get(rectangleIndex).getGridMaxY();
    }

    public void traverseRectangles(LayoutCell.RectangleHandler h) {
        for (ERectangle r: rects) {
            h.apply(r.getGridMinX(), r.getGridMinY(), r.getGridMaxX(), r.getGridMaxY());
        }
    }

    //  subcells
    public int getNumSubcells() {
        return subCells.size();
    }

    public LayoutCell getSubcellCell(int subCellIndex) {
        return subCells.get(subCellIndex).subCell;
    }

    public long getSubcellX(int subCellIndex) {
        return subCells.get(subCellIndex).anchor.getGridX();
    }

    public long getSubcellY(int subCellIndex) {
        return subCells.get(subCellIndex).anchor.getGridY();
    }

    public ManhattanOrientation getSubcellOrientation(int subCellIndex) {
        return subCells.get(subCellIndex).orient;
    }

    public void traverseSubcellInstances(LayoutCell.SubcellHandler h) {
        for (CellInst ci: subCells) {
            h.apply(ci.subCell, ci.anchor.getGridX(), ci.anchor.getGridY(), ci.orient);
        }
    }

    // bounding box
    public long getBoundingMinX() {
        return boundingBox().getGridMinX();
    }

    public long getBoundingMinY() {
        return boundingBox().getGridMinY();
    }

    public long getBoundingMaxX() {
        return boundingBox().getGridMaxX();
    }

    public long getBoundingMaxY() {
        return boundingBox().getGridMaxY();
    }
    
    private ERectangle boundingBox() {
        if (boundingBox == null) {
            long lx = Long.MAX_VALUE;
            long ly = Long.MAX_VALUE;
            long hx = Long.MIN_VALUE;
            long hy = Long.MIN_VALUE;
            for (ERectangle r: rects) {
                lx = Math.min(lx, r.getGridMinX());
                ly = Math.min(ly, r.getGridMinY());
                hx = Math.max(hx, r.getGridMaxX());
                hy = Math.max(hy, r.getGridMaxY());
            }
            long[] bounds = new long[4];
            for (CellInst ci: subCells) {
                long x = ci.anchor.getGridX();
                long y = ci.anchor.getGridY();
                bounds[0] = ci.subCell.getBoundingMinX();
                bounds[1] = ci.subCell.getBoundingMinY();
                bounds[2] = ci.subCell.getBoundingMaxX();
                bounds[3] = ci.subCell.getBoundingMaxY();
                ci.orient.transformRects(bounds, 0, 1);
                lx = Math.min(lx, x + bounds[0]);
                ly = Math.min(ly, y + bounds[1]);
                hx = Math.max(hx, x + bounds[2]);
                hy = Math.max(hy, y + bounds[3]);
            }
            if (lx <= hx && ly <= hy) {
                boundingBox = ERectangle.fromGrid(lx, ly, hx - lx, hy - ly);
            } else {
                boundingBox = ERectangle.ORIGIN;
            }
        }
        return boundingBox;
    }
    
    public void addRectangle(ERectangle r) {
        if (boundingBox != null)
            throw new IllegalStateException();
        rects.add(r);
    }
    
    public void addSubCell(LayoutCell subCell, EPoint anchor, ManhattanOrientation orient) {
        if (boundingBox != null)
            throw new IllegalStateException();
        subCells.add(new CellInst(subCell, anchor, orient));
    }
}
