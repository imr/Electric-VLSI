/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Point.java
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
package com.sun.electric.api.minarea.geometry;

/**
 * Immutable point implementation
 * 
 * @author Felix Schmidt
 *
 */
public class Point {

    private final int xCoord;
    private final int yCoord;

    public Point(int x, int y) {
        this.xCoord = x;
        this.yCoord = y;
    }

    /**
     * @return the xCoord
     */
    public int getX() {
        return xCoord;
    }

    /**
     * 
     * @param xCoord
     * @return
     */
    public Point withX(int xCoord) {
        if (xCoord == this.xCoord) return this;
        return new Point(xCoord, yCoord);
    }

    /**
     * @return the yCoord
     */
    public int getY() {
        return yCoord;
    }

    /**
     * 
     * @param xCoord
     * @return
     */
    public Point withY(int yCoord) {
        if (yCoord == this.yCoord) return this;
        return new Point(xCoord, yCoord);
    }

    // ********************* Some helper functions **********************
    /**
     * 
     */
    public Point add(Point other) {
        return new Point(this.xCoord + other.xCoord, this.yCoord + other.yCoord);
    }

    /**
     * 
     * @param scaleFactor
     * @return
     */
    public Point scale(int scaleFactor) {
        return scale(scaleFactor, scaleFactor);
    }

    /**
     * 
     * @param scaleFactorX
     * @param scaleFactorY
     * @return
     */
    public Point scale(int scaleFactorX, int scaleFactorY) {
        return new Point(this.xCoord * scaleFactorX, this.yCoord * scaleFactorY);
    }

    // ********************* Some helper classes **********************
    public static final class NullPoint extends Point {

        /**
         * @param x
         * @param y
         */
        public NullPoint() {
            super(0, 0);
        }
    }
}
