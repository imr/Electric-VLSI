/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ERectangle.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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
package com.sun.electric.database.geometry;

import com.sun.electric.database.text.ImmutableArrayList;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

/**
 * The <code>ERectangle</code> immutable class defines a point representing
 * defined by a location (x,&nbsp;y) and dimension (w&nbsp;x&nbsp;h).
 * <p>
 * This class is used in Electric database.
 */
public class ERectangle extends Rectangle2D implements Serializable {
    public static final ERectangle[] NULL_ARRAY = {};
    public static final ImmutableArrayList<ERectangle> EMPTY_LIST = new ImmutableArrayList<ERectangle>(NULL_ARRAY);
    
    private final long gridMinX;
    private final long gridMinY;
    private final long gridMaxX;
    private final long gridMaxY;
    private final double lambdaMinX;
    private final double lambdaMinY;
    private final double lambdaMaxX;
    private final double lambdaMaxY;
    private final double lambdaWidth;
    private final double lambdaHeight;
    private final boolean isSmall;
    
    /**
     * Constructs and initializes a <code>ERectangle</code>
     * from the specified grid coordinates.
     * @param gridX,&nbsp;gridY the coordinates of the upper left corner
     * of the newly constructed <code>ERectangle</code> in grid units.
     * @param gridWidth the width of the
     * newly constructed <code>ERectangle</code> in grid units.
     * @param gridHeight the height of the
     * newly constructed <code>ERectangle</code> in grid units.
     */
    private ERectangle(long gridX, long gridY, long gridWidth, long gridHeight) {
        gridMinX = gridX;
        gridMinY = gridY;
        gridMaxX = gridX + gridWidth;
        gridMaxY = gridY + gridHeight;
        lambdaMinX = DBMath.gridToLambda(gridMinX);
        lambdaMinY = DBMath.gridToLambda(gridMinY);
        lambdaMaxX = DBMath.gridToLambda(gridMaxX);
        lambdaMaxY = DBMath.gridToLambda(gridMaxY);
        lambdaWidth = DBMath.gridToLambda(gridWidth);
        lambdaHeight = DBMath.gridToLambda(gridHeight);
        isSmall = GenMath.isSmallInt(gridMinX) & GenMath.isSmallInt(gridMinY) & GenMath.isSmallInt(gridMaxX) & GenMath.isSmallInt(gridMaxY);
    }
    
    /**
     * Constructs and initializes a <code>ERectangle</code>
     * from the specified long coordinates in lambda units.
     * @param x the X coordinates of the upper left corner
     * of the newly constructed <code>ERectangle</code>
     * @param y the Y coordinates of the upper left corner
     * of the newly constructed <code>ERectangle</code>
     * @param w the width of the newly constructed <code>ERectangle</code>
     * @param h the height of the newly constructed <code>ERectangle</code>
     */
    public static ERectangle fromLambda(double x, double y, double w, double h) {
        return new ERectangle(DBMath.lambdaToGrid(x), DBMath.lambdaToGrid(y), DBMath.lambdaToGrid(w), DBMath.lambdaToGrid(h));
    }
    
    /**
     * Constructs and initializes a <code>ERectangle</code>
     * from the specified long coordinates in grid units.
     * @param x the X coordinates of the upper left corner
     * of the newly constructed <code>ERectangle</code>
     * @param y the Y coordinates of the upper left corner
     * of the newly constructed <code>ERectangle</code>
     * @param w the width of the newly constructed <code>ERectangle</code>
     * @param h the height of the newly constructed <code>ERectangle</code>
     */
    public static ERectangle fromGrid(long x, long y, long w, long h) {
        return new ERectangle(x, y, w, h);
    }
    
    /**
     * Returns <code>ERectangle</code> from specified <code>Rectangle2D</code> in lambda units
     * snapped to the grid.
     * @param r specified ERectangle
     * @return Snapped ERectangle
     */
    public static ERectangle fromLambda(Rectangle2D r) {
        if (r instanceof ERectangle) return (ERectangle)r;
        return fromLambda(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }
    
    /**
     * Returns <code>ERectangle</code> from specified <code>Rectangle2D</code> in grid units
     * snapped to the grid.
     * @param r specified ERectangle
     * @return Snapped ERectangle
     */
    public static ERectangle fromGrid(Rectangle2D r) {
//        if (r instanceof ERectangle) return (ERectangle)r;
        long x1 = (long)Math.floor(r.getMinX());
        long y1 = (long)Math.floor(r.getMinY());
        long x2 = (long)Math.ceil(r.getMaxX());
        long y2 = (long)Math.ceil(r.getMaxY());
        return fromGrid(x1, y1, x2 - x1, y2 - y1);
    }
    
    /**
     * Returns the X coordinate of this <code>ERectangle</code>
     * in lambda units in double precision.
     * @return the X coordinate of this <code>ERectangle</code>.
     */
    @Override
    public double getX() {
        return lambdaMinX;
    }
    
    /**
     * Returns the Y coordinate of this <code>ERectangle</code>
     * in lambda units in double precision.
     * @return the X coordinate of this <code>ERectangle</code>.
     */
    @Override
    public double getY() {
        return lambdaMinY;
    }
    
    /**
     * Returns the width of this <code>ERectangle</code>
     * in lambda units in double precision.
     * @return the width of this <code>ERectangle</code>.
     */
    @Override
    public double getWidth() {
        return lambdaWidth;
    }
    
    /**
     * Returns the heigth of this <code>ERectangle</code>
     * in lambda units in double precision.
     * @return the heigth of this <code>ERectangle</code>.
     */
    @Override
    public double getHeight() {
        return lambdaHeight;
    }
    
    /**
     * Returns the largest X coordinate of this <code>ERectangle</code>
     * in lambda units in <code>double</code> precision.
     * @return the largest x coordinate of this <code>ERectangle</code>.
     */
    @Override
    public double getMaxX() {
        return lambdaMaxX;
    }
    
    /**
     * Returns the largest Y coordinate of this <code>ERectangle</code>
     * in lambda units in <code>double</code> precision.
     * @return the largest y coordinate of this <code>ERectangle</code>.
     */
    @Override
    public double getMaxY() {
        return lambdaMaxY;
    }
    
    /**
     * Returns the X coordinate of this <code>ERectangle</code>
     * in lambda units in double precision.
     * @return the X coordinate of this <code>ERectangle</code>.
     */
    public double getLambdaX() {
        return lambdaMinX;
    }
    
    /**
     * Returns the Y coordinate of this <code>ERectangle</code>
     * in lambda units in double precision.
     * @return the X coordinate of this <code>ERectangle</code>.
     */
    public double getLambdaY() {
        return lambdaMinY;
    }
    
    /**
     * Returns the width of this <code>ERectangle</code>
     * in lambda units in double precision.
     * @return the width of this <code>ERectangle</code>.
     */
    public double getLambdaWidth() {
        return lambdaWidth;
    }
    
    /**
     * Returns the heigth of this <code>ERectangle</code>
     * in lambda units in double precision.
     * @return the heigth of this <code>ERectangle</code>.
     */
    public double getLambdaHeight() {
        return lambdaHeight;
    }
    
    /**
     * Returns the smallest X coordinate of this <code>ERectangle</code>
     * in lambda units in <code>double</code> precision.
     * @return the smallest x coordinate of this <code>ERectangle</code>.
     */
    public double getLambdaMinX() {
        return lambdaMinX;
    }
    
    /**
     * Returns the smallest Y coordinate of this <code>ERectangle</code>
     * in lambda units in <code>double</code> precision.
     * @return the smallest y coordinate of this <code>ERectangle</code>.
     */
    public double getLambdaMinY() {
        return lambdaMinY;
    }
    
    /**
     * Returns the largest X coordinate of this <code>ERectangle</code>
     * in lambda units in <code>double</code> precision.
     * @return the largest x coordinate of this <code>ERectangle</code>.
     */
    public double getLambdaMaxX() {
        return lambdaMaxX;
    }
    
    /**
     * Returns the largest Y coordinate of this <code>ERectangle</code>
     * in lambda units in <code>double</code> precision.
     * @return the largest y coordinate of this <code>ERectangle</code>.
     */
    public double getLambdaMaxY() {
        return lambdaMaxY;
    }
    
    /**
     * Returns the X coordinate of the center of this <code>ERectangle</code>
     * in lambda units in <code>double</code> precision.
     * @return the x coordinate of this <code>ERectangle</code> object's center.
     */
    public double getLambdaCenterX() {
        return getCenterX();
    }

    /**
     * Returns the Y coordinate of the center of this <code>ERectangle</code>
     * in lambda units in <code>double</code> precision.
     * @return the y coordinate of this <code>ERectangle</code> object's center.
     */
    public double getLambdaCenterY() {
        return getCenterY();
    }

    /**
     * Returns the X coordinate of this <code>ERectangle</code>
     * in grid units in long precision.
     * @return the X coordinate of this <code>ERectangle</code>.
     */
    public long getGridX() {
        return gridMinX;
    }
    
    /**
     * Returns the Y coordinate of this <code>ERectangle</code>
     * in grid units in long precision.
     * @return the X coordinate of this <code>ERectangle</code>.
     */
    public long getGridY() {
        return gridMinY;
    }
    
    /**
     * Returns the width of this <code>ERectangle</code>
     * in grid units in long precision.
     * @return the width of this <code>ERectangle</code>.
     */
    public long getGridWidth() {
        return gridMaxX - gridMinX;
    }
    
    /**
     * Returns the heigth of this <code>ERectangle</code>
     * in grid units in long precision.
     * @return the heigth of this <code>ERectangle</code>.
     */
    public long getGridHeight() {
        return gridMaxY - gridMinY;
    }
    
    /**
     * Returns the smallest X coordinate of this <code>ERectangle</code>
     * in grid units in <code>long</code> precision.
     * @return the smallest x coordinate of this <code>ERectangle</code>.
     */
    public long getGridMinX() {
        return gridMinX;
    }
    
    /**
     * Returns the smallest Y coordinate of this <code>ERectangle</code>
     * in grid units in <code>long</code> precision.
     * @return the smallest y coordinate of this <code>ERectangle</code>.
     */
    public long getGridMinY() {
        return gridMinY;
    }
    
    /**
     * Returns the largest X coordinate of this <code>ERectangle</code>
     * in grid units in <code>long</code> precision.
     * @return the largest x coordinate of this <code>ERectangle</code>.
     */
    public long getGridMaxX() {
        return gridMaxX;
    }
    
    /**
     * Returns the largest Y coordinate of this <code>ERectangle</code>
     * in grid units in <code>long</code> precision.
     * @return the largest y coordinate of this <code>ERectangle</code>.
     */
    public long getGridMaxY() {
        return gridMaxY;
    }
    
    /**
     * Returns the X coordinate of the center of this <code>ERectangle</code>
     * in grid units in <code>long</code> precision.
     * @return the x coordinate of this <code>ERectangle</code> object's center.
     */
    public double getGridCenterX() {
        return (gridMinX + gridMaxX) >> 1;
    }

    /**
     * Returns the Y coordinate of the center of this <code>ERectangle</code>
     * in grid units in <code>long</code> precision.
     * @return the y coordinate of this <code>ERectangle</code> object's center.
     */
    public double getGridCenterY() {
        return (gridMinY + gridMaxY) >> 1;
    }

    @Override
    public boolean isEmpty() { return gridMinX >= gridMaxX || gridMinY >= gridMaxY; }
    
    /**
     * Returns true if all coordinates of this EPoint are "small ints".
     * @return true if all coordinates of this EPoint are "small ints".
     * @See com.sun.electric.database.geometry.GenMath.MIN_SMALL_INT
     * @See com.sun.electric.database.geometry.GenMath.MAX_SMALL_INT
     */
    public boolean isSmall() { return isSmall; }
    
    @Override
    public void setRect(double x, double y, double w, double h) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public int outcode(double x, double y) {
        int out = 0;
        if (gridMinX >= gridMaxX) {
            out |= OUT_LEFT | OUT_RIGHT;
        } else if (x*DBMath.GRID < gridMinX) {
            out |= OUT_LEFT;
        } else if (x*DBMath.GRID > gridMaxX) {
            out |= OUT_RIGHT;
        }
        if (gridMinY >= gridMaxY) {
            out |= OUT_TOP | OUT_BOTTOM;
        } else if (y*DBMath.GRID < gridMinY) {
            out |= OUT_TOP;
        } else if (y*DBMath.GRID > gridMaxY) {
            out |= OUT_BOTTOM;
        }
        return out;
    }
    
    @Override
    public Rectangle2D getBounds2D() {
        return this;
    }
    
    /**
     * Returns the bounding box of the <code>ERectangle</code>.
     * @return a {@link Rectangle} object that bounds the 
     * 		<code>ERectangle</code>.
     */
    public Rectangle getBounds() {
        long width = getGridWidth();
        long height = getGridHeight();
        if (width < 0 || height < 0) {
            return new Rectangle();
        }
        return new Rectangle((int)getGridX(), (int)getGridY(), (int)width, (int)height);
    }

    @Override
    public Rectangle2D createIntersection(Rectangle2D r) {
        if (r instanceof ERectangle) {
            ERectangle src = (ERectangle)r;
        	long x1 = Math.max(gridMinX, src.gridMinX);
            long y1 = Math.max(gridMinY, src.gridMinY);
            long x2 = Math.min(gridMaxX, src.gridMaxX);
            long y2 = Math.min(gridMaxY, src.gridMaxY);
            if (x1 == gridMinX && y1 == gridMinY && x2 == gridMaxX && y2 == gridMaxY) return this;
            if (x1 == src.gridMinX && y1 == src.gridMinY && x2 == src.gridMaxX && y2 == src.gridMaxY) return src;
            return new ERectangle(x1, y1, x2 - x1, y2 - 1);
        }
        Rectangle2D dest = new Rectangle2D.Double();
        Rectangle2D.intersect(this, r, dest);
        return dest;
    }
    
    @Override
    public Rectangle2D createUnion(Rectangle2D r) {
        if (r instanceof ERectangle) {
            ERectangle src = (ERectangle)r;
            long x1 = Math.min(gridMinX, src.gridMinX);
            long y1 = Math.min(gridMinY, src.gridMinY);
            long x2 = Math.max(gridMaxX, src.gridMaxX);
            long y2 = Math.max(gridMaxY, src.gridMaxY);
            if (x1 == gridMinX && y1 == gridMinY && x2 == gridMaxX && y2 == gridMaxY) return this;
            if (x1 == src.gridMinX && y1 == src.gridMinY && x2 == src.gridMaxX && y2 == src.gridMaxY) return src;
            return new ERectangle(x1, y1, x2 - x1, y2 - 1);
        }
        Rectangle2D dest = new Rectangle2D.Double();
        Rectangle2D.union(this, r, dest);
        return dest;
    }
    
    @Override
    public String toString() {
        return getClass().getName() + "[x=" + getX() + ",y=" + getY() + ",w=" + getWidth() + ",h=" + getHeight() + "]";
    }
}
