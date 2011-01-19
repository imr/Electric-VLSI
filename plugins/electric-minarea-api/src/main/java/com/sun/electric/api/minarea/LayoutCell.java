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

    /**
     * The limit of coordinates.
     * All coordinates of LayoutCell are in the range [-MAX_COORD,MAX_COORD].
     * This includes coordinates of rectangles, polygons, anchors of subcells and coordinates of
     * bounding boxes.
     */
    public static final int MAX_COORD = 0x3FFFFFFF;

    // cell name
    public String getName();

    // rectangles
    public int getNumRectangles();

    // traversal of rectangles
    public interface RectangleHandler {

        /**
         * @param minX
         * @param minY
         * @param maxX
         * @param maxY
         */
        public void apply(int minX, int minY, int maxX, int maxY);
    }

    /**
     * Traverse all rectangles by specified handler 
     * @param h handler
     */
    public void traverseRectangles(RectangleHandler h);

    /**
     * Traverse part of rectangles by specified handler 
     * @param h handler
     * @param offset the first rectangle 
     * @param count the number of rectangles
     */
    public void traverseRectangles(RectangleHandler h, int offset, int count);

    /**
     * Read coordinates of part of rectangles into int array.
     * The length of the result array must be at least 4*count .
     * The coordinates are placed into the result array in such an order:
     * (minX0, minY0, maxX0, maxY0, minX1, minY1, maxX1, maxY1, ...)
     * This is the same layout as in ManhattanOrientation.transoformRects method.
     * @param result
     * @param offset The first rectangle
     * @param count The number of rectangles
     */
    public void readRectangleCoords(int[] result, int offset, int count);

    //  subcells
    public int getNumSubcells();

    // traversal of subcells
    public interface SubcellHandler {

        /**
         * @param cell
         * @param anchorX
         * @param anchorY
         * @param orient
         */
        public void apply(LayoutCell cell, int anchorX, int anchorY, ManhattanOrientation orient);
    }

    /**
     * Traverse all subcell instances by specified handler 
     * @param h handler
     */
    public void traverseSubcellInstances(SubcellHandler h);

    /**
     * Traverse part of  subcell instances by specified handler 
     * @param h handler
     * @param offset the first subcell instance
     * @param count the number of subcell instances
     */
    public void traverseSubcellInstances(SubcellHandler h, int offset, int count);

    // bounding box
    public int getBoundingMinX();

    public int getBoundingMinY();

    public int getBoundingMaxX();

    public int getBoundingMaxY();
}
