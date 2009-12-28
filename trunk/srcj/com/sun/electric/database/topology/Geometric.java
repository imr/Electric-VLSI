/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Geometric.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.topology;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.variable.ElectricObject;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

/**
 * This class is the superclass for the Electric classes that have visual
 * bounds on the screen, specifically NodeInst and ArcInst.
 */
public abstract class Geometric extends ElectricObject implements RTBounds {
    // ------------------------ private and protected methods--------------------
    /**
     * The constructor is only called from subclasses.
     */
    protected Geometric() {
    }

    /**
     * Method to describe this Geometric as a string.
     * This method is overridden by NodeInst and ArcInst.
     * @param withQuotes to wrap description between quotes
     * @return a description of this Geometric as a string.
     */
    public abstract String describe(boolean withQuotes);

    /**
     * Method to determine which page of a multi-page schematic this Geometric is on.
     * @return the page number (0-based).
     */
    public int whichMultiPage() {
        int pageNo = 0;
        if (getParent().isMultiPage()) {
            double cY = getBounds().getCenterY();
            pageNo = (int) ((cY + Cell.FrameDescription.MULTIPAGESEPARATION / 2) / Cell.FrameDescription.MULTIPAGESEPARATION);
        }
        return pageNo;
    }

    /**
     * Method to write a description of this Geometric.
     * Displays the description in the Messages Window.
     */
    @Override
    public void getInfo() {
        Rectangle2D visBounds = getBounds();
        System.out.println(" Bounds: (" + visBounds.getCenterX() + "," + visBounds.getCenterY() + "), size: "
                + visBounds.getWidth() + "x" + visBounds.getHeight());
        System.out.println(" Parent: " + getParent());
        super.getInfo();
    }

    // ------------------------ public methods -----------------------------
    /**
     * Method to return the Cell that contains this Geometric object.
     * @return the Cell that contains this Geometric object.
     */
    public Cell getParent() {
        Topology topology = getTopology();
        return topology != null ? topology.cell : null;
    }

    /**
     * Method to return the Cell Topology that contains this Geometric object.
     * @return the Topology that contains this Geometric object.
     */
    public abstract Topology getTopology();


    /**
     * Returns the polygons that describe this Geometric.
     * @param polyBuilder Poly builder.
     * @return an iterator on Poly objects that describes this Geometric graphically.
     * These Polys include displayable variables on the Geometric.
     */
    public abstract Iterator<Poly> getShape(Poly.Builder polyBuilder);

    /**
     * Method to return the bounds of this Geometric.
     * @return the bounds of this Geometric.
     */
    public abstract Rectangle2D getBounds();

    /**
     * Method to fill the bounds of this Geometric in lambda units into specified Rectangle2D.
     * If specified Rectangle2D is null, a new Rectangle2D.Double is allocated.
     * @param r rectangle to fill
     * @return the bounds of this Geometric.
     */
    public Rectangle2D getLambdaBounds(Rectangle2D r) {
        if (r == null) {
            r = new Rectangle2D.Double();
        }
        r.setRect(getBounds());
        return r;
    }

    /**
     * Method to fill the bounds of this Geometric in grid units into specified Rectangle2D.
     * If specified Rectangle2D is null, a new Rectangle2D.Double is allocated.
     * @param r rectangle to fill
     * @return the bounds of this Geometric.
     */
    public Rectangle2D getGridBounds(Rectangle2D r) {
        if (r == null) {
            r = new Rectangle2D.Double();
        }
        Rectangle2D bounds = getBounds();
        long minX = DBMath.lambdaToGrid(bounds.getMinX());
        long minY = DBMath.lambdaToGrid(bounds.getMinY());
        long maxX = DBMath.lambdaToGrid(bounds.getMaxX());
        long maxY = DBMath.lambdaToGrid(bounds.getMaxY());
        r.setRect(minX, minY, maxX - minX, maxY - minY);
        return r;
    }

    /**
     * Method to return the center X coordinate of this Geometric.
     * @return the center X coordinate of this Geometric.
     */
    public double getTrueCenterX() {
        return getBounds().getCenterX();
    }

    /**
     * Method to return the center Y coordinate of this Geometric.
     * @return the center Y coordinate of this Geometric.
     */
    public double getTrueCenterY() {
        return getBounds().getCenterY();
    }

    /**
     * Method to return the center coordinate of this Geometric.
     * @return the center coordinate of this Geometric.
     */
    public Point2D getTrueCenter() {
        return new Point2D.Double(getTrueCenterX(), getTrueCenterY());
    }

    /**
     * Method to tell whether this Geometric object is connected directly to another
     * (that is, an arcinst connected to a nodeinst).
     * The method returns true if they are connected.
     * @param geom other Geometric object.
     * @return true if this and other Geometric objects are connected.
     */
    public abstract boolean isConnected(Geometric geom);
}
