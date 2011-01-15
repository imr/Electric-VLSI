/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayoutCell.java
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

/**
 * Data source for main area DRC
 */
public interface LayoutCell {

    // cell name
    public String getName();

    // rectangles
    public int getNumRectangles();

    public long getRectangleMinX(int rectangleIndex);

    public long getRectangleMinY(int rectangleIndex);

    public long getRectangleMaxX(int rectangleIndex);

    public long getRectangleMaxY(int rectangleIndex);

    // traversal of rectangles
    public interface RectangleHandler {

        /**
         * @param minX
         * @param minY
         * @param maxX
         * @param maxY
         */
        public void apply(long minX, long minY, long maxX, long maxY);
    }

    public void traverseRectangles(RectangleHandler h);

    //  subcells
    public int getNumSubcells();

    public LayoutCell getSubcellCell(int subCellIndex);

    public long getSubcellX(int subCellIndex);

    public long getSubcellY(int subCellIndex);

    public ManhattanOrientation getSubcellOrientation(int subCellIndex);

    // traversal of subcells
    public interface SubcellHandler {

        /**
         * @param cell
         * @param x
         * @param y
         * @param orient
         */
        public void apply(LayoutCell cell, long x, long y, ManhattanOrientation orient);
    }

    public void traverseSubcellInstances(SubcellHandler h);

    // bounding box
    public long getBoundingMinX();

    public long getBoundingMinY();

    public long getBoundingMaxX();

    public long getBoundingMaxY();
}
