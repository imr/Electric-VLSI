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


import com.sun.electric.api.minarea.geometry.Point;
import com.sun.electric.api.minarea.geometry.Polygon.Rectangle;

/**
 * Data source for main area DRC
 */
public interface LayoutCell {

    // cell name
    public String getName();

    // rectangles
    public int getNumRectangles();

    @Deprecated
    public int getRectangleMinX(int rectangleIndex);

    @Deprecated
    public int getRectangleMinY(int rectangleIndex);

    @Deprecated
    public int getRectangleMaxX(int rectangleIndex);

    @Deprecated
    public int getRectangleMaxY(int rectangleIndex);
    
    public Rectangle getRectangle(int rectangleIndex);

    // traversal of rectangles
    public interface RectangleHandler {

        /**
         * @param minX
         * @param minY
         * @param maxX
         * @param maxY
         */
    	@Deprecated
        public void apply(int minX, int minY, int maxX, int maxY);
    	
    	public void apply(Rectangle r);
    }

    public void traverseRectangles(RectangleHandler h);

    //  subcells
    public int getNumSubcells();

    public LayoutCell getSubcellCell(int subCellIndex);

    @Deprecated
    public int getSubcellAnchorX(int subCellIndex);

    @Deprecated
    public int getSubcellAnchorY(int subCellIndex);
    
    public Point getSubcellAnchor(int subCellIndex);

    public ManhattanOrientation getSubcellOrientation(int subCellIndex);

    // traversal of subcells
    public interface SubcellHandler {

        /**
         * @param cell
         * @param anchor
         * @param orient
         */
        public void apply(LayoutCell cell, Point anchor, ManhattanOrientation orient);
    }

    public void traverseSubcellInstances(SubcellHandler h);

    // bounding box
    @Deprecated
    public int getBoundingMinX();

    @Deprecated
    public int getBoundingMinY();

    @Deprecated
    public int getBoundingMaxX();

    @Deprecated
    public int getBoundingMaxY();
    
    public Rectangle getBoundingBox();
}
