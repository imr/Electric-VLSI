/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EPoint.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import java.awt.geom.Point2D;
import java.io.Serializable;

/**
 * The <code>EPoint</code> immutable class defines a point representing
 * a location in (x,&nbsp;y) coordinate space. This class extends abstract
 * class Point2D. This class is used in Electric database.
 * Coordiates are snapped to grid according to <code>DBMath.round</code> method.
 */
final public class EPoint extends Point2D implements Serializable {

    /** EPoint with both zero coordinates. */
	public static final EPoint ORIGIN = new EPoint(0, 0);

    // ---------- Flat implementation
    
    /**
     * The X coordinate of this <code>EPoint</code> in grid unuts.
     */
    private final int gridX;

    /**
     * The Y coordinate of this <code>EPoint</code> in grid units.
     */
    private final int gridY;

    /**
     * The X coordinate of this <code>EPoint</code> in lambda unuts.
     */
    private final double lambdaX;

    /**
     * The Y coordinate of this <code>EPoint</code> in lambda units.
     */
    private final double lambdaY;

    
    // ---------- ECoord implementation
//    /**
//     * The X coordinate of this <code>EPoint</code>.
//     */
//    private final ECoord x;
//
//    /**
//     * The Y coordinate of this <code>EPoint</code>.
//     */
//    private final ECoord y;

    // ----------
    
    private static int createdEPoints;
    
    /**
     * Constructs and initializes a <code>EPoint</code> with the
     * specified coordinates in lambda units snapped to the grid.
     * @param lambdaX the x-coordinate to which to set the newly
     * constructed <code>EPoint</code> in lambda units.
     * @param lambdaY the y-coordinate to which to set the newly
     * constructed <code>EPoint</code> in lambda units.
     */
    public EPoint(double lambdaX, double lambdaY) {
        this(DBMath.lambdaToGrid(lambdaX), DBMath.lambdaToGrid(lambdaY));
    }

    /**
     * Constructs and initializes a <code>EPoint</code> with the
     * specified coordinates in grid units.
     * @param gridX the x-coordinate to which to set the newly
     * constructed <code>EPoint</code> in grid units.
     * @param gridX the y-coordinate to which to set the newly
     * constructed <code>EPoint</code> in grid units.
     */
    private EPoint(long gridX, long gridY) {
        // ---------- Flat implementation
		this.gridX = (int)gridX;
		this.gridY = (int)gridY;
        if (this.gridX != gridX || this.gridY != gridY)
            throw new IllegalArgumentException("Too large coordinates (" + gridX + "," + gridY + ")");
        lambdaX = DBMath.gridToLambda(gridX);
        lambdaY = DBMath.gridToLambda(gridY);
        // ---------- ECoord implementation
//        x = ECoord.fromGrid(gridX);
//        y = ECoord.fromGrid(gridY);
        // ----------
        
        createdEPoints++;
    }

    /**
     * Returns <code>EPoint</code> with specified grid coordinates.
     * @param lambdaX the x-coordinate in lambda units.
     * @param lambdaY the y-coordinate in lambda units.
	 * @return EPoint with specified grid coordinates.
     */
    public static EPoint fromLambda(double lambdaX, double lambdaY) {
        return lambdaX == 0 && lambdaY == 0 ? ORIGIN : fromGrid(DBMath.lambdaToGrid(lambdaX), DBMath.lambdaToGrid(lambdaY));
    }
    
    /**
     * Returns <code>EPoint</code> with specified grid coordinates.
     * @param gridX the x-coordinate in grid units.
     * @param gridY the y-coordinate in grid units.
	 * @return EPoint with specified grid coordinates.
     */
    public static EPoint fromGrid(long gridX, long gridY) {
        return gridX == 0 && gridY == 0 ? ORIGIN : new EPoint(gridX, gridY);
    }
    
    /**
     * Returns <code>EPoint</code> from specified <code>Point2D</code>
     * snapped to the grid.
     * @param p specified Point2D
	 * @return Snapped EPoint
     */
	public static EPoint snap(Point2D p) {
		return (p instanceof EPoint) ? (EPoint)p : fromLambda(p.getX(), p.getY());
	}

    /**
     * Returns the X coordinate of this <code>EPoint</code> 
     * in lambda units in <code>double</code> precision.
     * @return the X coordinate of this <code>EPoint</code>.
     */
    @Override
    public double getX() {
        return getLambdaX();
    }

    /**
     * Returns the Y coordinate of this <code>EPoint</code> 
     * in lambda unuts in <code>double</code> precision.
     * @return the Y coordinate of this <code>EPoint</code>.
     */
    @Override
    public double getY() {
        return getLambdaY();
    }

    /**
     * Returns the X coordinate of this <code>EPoint</code> 
     * in lambda units in <code>double</code> precision.
     * @return the X coordinate of this <code>EPoint</code>.
     */
    public double getLambdaX() {
        // ---------- Flat implementation
        return lambdaX;
        // ---------- ECoord implementation
//		return x.lambdaValue();
        // ----------
    }

    /**
     * Returns the Y coordinate of this <code>EPoint</code> 
     * in lambda units in <code>double</code> precision.
     * @return the Y coordinate of this <code>EPoint</code>.
     */
    public double getLambdaY() {
        // ---------- Flat implementation
        return lambdaY;
        // ---------- ECoord implementation
//		return y.lambdaValue();
        // ----------
    }

    /**
     * Returns the X coordinate of this <code>EPoint</code> 
     * in grid units in <code>long</code> precision.
     * @return the X coordinate of this <code>EPoint</code>.
     */
    public long getGridX() {
        // ---------- Flat implementation
        return gridX;
        // ---------- ECoord implementation
//		return x.gridValue();
        // ----------
    }

    /**
     * Returns the Y coordinate of this <code>EPoint</code> 
     * in grid units in <code>long</code> precision.
     * @return the Y coordinate of this <code>EPoint</code>.
     */
    public long getGridY() {
        // ---------- Flat implementation
        return gridY;
        // ---------- ECoord implementation
//		return y.gridValue();
        // ----------
    }

    /**
     * This method overrides <code>Point2D.setLocation</code> method.
     * It throws UnsupportedOperationException.
     * @param x the x-coordinate to which to set this <code>EPoint</code>
     * @param y the y-coordinate to which to set this <code>EPoint</code>
     * @throws UnsupportedOperationException
     */
    @Override
    public void setLocation(double x, double y) {
		throw new UnsupportedOperationException();
    }

    /**
     * Creates mutable <code>Point2D.Double</code> from the <code>EPoint</code> in lambda units.
     * @return mutable Point2D in lambda units
     */
	public Point2D.Double lambdaMutable() {
		return new Point2D.Double(getLambdaX(), getLambdaY());
	}

    /**
     * Creates mutable <code>Point2D.Double</code> from the <code>EPoint</code> in grid units.
     * @return mutable Point2D in grid units
     */
	public Point2D.Double gridMutable() {
		return new Point2D.Double(getGridX(), getGridY());
	}

     /**
     * Returns the distance from this <code>EPoint</code> to a
     * specified <code>EPoint</code> in lambda units.
     * @param pt the specified <code>EPoint</code>
     * @return the distance between this <code>EPoint</code> and
     * the specified <code>Point</code> in lambdaUnits.
     */
    public double lambdaDistance(EPoint pt) {
        return DBMath.gridToLambda(gridDistance(pt));
    }

   /**
     * Returns the distance from this <code>EPoint</code> to a
     * specified <code>EPoint</code> in grid units.
     * @param pt the specified <code>EPoint</code>
     * @return the distance between this <code>EPoint</code> and
     * the specified <code>Point</code> in gridUnits.
     */
    public double gridDistance(EPoint pt) {
        long PX = pt.getGridX() - this.getGridX();
        long PY = pt.getGridY() - this.getGridY();
        return PY == 0 ? Math.abs(PX) : PX == 0 ? Math.abs(PY) : Math.hypot(PX, PY);
    }

    /**
     * Returns true if both coordinates of this EPoint are "small ints".
     * @return true if both coordinates of this EPoint are "small ints".
     * @See com.sun.electric.database.geometry.GenMath.MIN_SMALL_INT
     * @See com.sun.electric.database.geometry.GenMath.MAX_SMALL_INT
     */
    public boolean isSmall() {
        // ---------- Flat implementation
        return (((gridX - GenMath.MIN_SMALL_COORD) | (gridY - GenMath.MIN_SMALL_COORD)) & Integer.MIN_VALUE) == 0;
        // ---------- ECoord implementation
//		return x.isSmall() & y.isSmall()
        // ----------
    }
    
	/**
	 * Returns a <code>String</code> that represents the value 
	 * of this <code>EPoint</code>.
	 * @return a string representation of this <code>EPoint</code>.
	 */
	public String toString() {
	    return "EPoint["+getX()+", "+getY()+"]";
	}
    
    /**
     * Prints statistics about EPoint objects.
     */
    public static void printStatistics() {
        System.out.println(createdEPoints + " EPoints created");
        // ---------- ECoord implementation
//        ECoord.printStatistics();
        // ----------
    }
}
