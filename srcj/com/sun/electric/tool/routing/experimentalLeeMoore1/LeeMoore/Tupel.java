/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Tupel.java
 * Written by: Andreas Uebelhoer, Alexander Bieles, Emre Selegin (Team 6)
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.routing.experimentalLeeMoore1.LeeMoore;

import com.sun.electric.tool.routing.RoutingFrame.RoutePoint;
import com.sun.electric.tool.routing.experimentalLeeMoore1.yana;

import java.awt.geom.Point2D;

/**
 * This object is a point in our routing array. It saves both the coordinates in the routing array as well as
 * the coordinates in electric
 */
public class Tupel {
	static int distanceBetweenWires  = (yana.distanceBetweenWires==0)? 3: yana.distanceBetweenWires;
    static int offsetX, offsetY;
    private int xInsideElectric, yInsideElectric, layer, xInsideRoutingArray, yInsideRoutingArray;

    /**
     * Set offset, as tupel coordinates have to be positive where as electric coordinates are not
     * @param x offset in x direction
     * @param y offset in y direction
     * @param spacing additional space around the cell to allow wires to leave it
     */
    public static void setOffset(double x, double y, int spacing, boolean output) {
        offsetX = (int) (x - spacing);
        offsetY = (int) (y - spacing);
        if(output) System.out.println("Using offset " + offsetX + "," + offsetY);
    }

    /**
     *
     * @param xInsideElectric xInsideElectric value
     * @param yInsideElectric yInsideElectric value
     * @param is_electric_coordinate true, if coordinates are taken from electric (-> offset needs to be added)
     */
    public Tupel(int x, int y, int layer, boolean is_electric_coordinate) {
        if (is_electric_coordinate) {
            this.xInsideElectric = x;
            this.yInsideElectric = y;
            this.xInsideRoutingArray = (x - offsetX) / distanceBetweenWires;
            this.yInsideRoutingArray = (y - offsetY) / distanceBetweenWires;
        } else {
            this.xInsideElectric = (x * distanceBetweenWires + distanceBetweenWires / 2) + offsetX;
            this.yInsideElectric = (y * distanceBetweenWires + distanceBetweenWires / 2) + offsetY;
            this.xInsideRoutingArray = x;
            this.yInsideRoutingArray = y;
        }
        this.layer = layer;
    }

    /**
     * Create tupel out of a Point2D
     * @param p Point2D
     * @param layer layer
     */
    public Tupel(Point2D p, int layer) {
        this.xInsideElectric = (int) p.getX();
        this.yInsideElectric = (int) p.getY();
        this.xInsideRoutingArray = (xInsideElectric - offsetX) / distanceBetweenWires;
        this.yInsideRoutingArray = (yInsideElectric - offsetY) / distanceBetweenWires;
        this.layer = layer;
    }

    /**
     * returns xInsideElectric value as used in electric
     * @return
     */
    public int getX_InsideElectric() {
        return xInsideElectric;
    }

    /**
     * returns xInsideElectric value with offset
     * @return
     */
    public int getX_InsideRoutingArray() {
        return xInsideRoutingArray;
    }

    /**
     * returns yInsideElectric value as used in electric
     * @return
     */
    public int getY_InsideElectric() {
        return yInsideElectric;
    }

    /**
     * returns yInsideElectric value with offset
     * @return
     */
    public int getY_InsideRoutingArray() {
        return yInsideRoutingArray;
    }

    /**
     * 
     * @return layer of the tupel
     */
    public int getLayer() {
        return layer;
    }

    /**
     * print tupel
     */
    public void printTupel() {
        System.out.print(this.toString());
    }

    public String toString() {
        return "[" + xInsideElectric + "," + yInsideElectric + "@Metal-" + (layer + 1) + "(" + xInsideRoutingArray + "," + yInsideRoutingArray + "," + layer + ")]";
    }

    /**
     * Determine whether this tupel is equal to another tupel 
     * @param t the other tupel
     * @return
     */
    public boolean isEqual(Tupel t) {
        return (isEqualInElectric(t) || isEqualInRoutingArray(t));
    }

    /**
     * Check whether the tupel has the same electric coordinates
     * @param t tupel to compare with
     * @return
     */
    private boolean isEqualInElectric(Tupel t) {
        return (xInsideElectric == t.getX_InsideElectric() && yInsideElectric == t.getY_InsideElectric() && layer == t.getLayer());
    }

    /**
     * Check whether the tupel has the same routing array coordinates
     * @param t tupel to compare with
     * @return
     */
    private boolean isEqualInRoutingArray(Tupel t) {
        return (xInsideRoutingArray == t.getX_InsideRoutingArray() && yInsideRoutingArray == t.getY_InsideRoutingArray() && layer == t.getLayer());
    }

    /**
     * Check whether the tupel has the same electric coordinates without checking the layer
     * @param t tupel to compare with
     * @return
     */
    public boolean isEqualPositionInElectric(Tupel t) {
        return (xInsideElectric == t.getX_InsideElectric() && yInsideElectric == t.getY_InsideElectric());
    }

    /**
     * Check whether the route point has the same electric coordinates without checking the layer
     * @param rp1 RoutePoint to compare with
     * @return
     */
    public boolean isEqualPosition(RoutePoint rp1) {
        return (xInsideRoutingArray == convertElectricToRoutingArrayCoordinate_X(rp1.getLocation().getX())
                && yInsideRoutingArray == convertElectricToRoutingArrayCoordinate_Y(rp1.getLocation().getX()));
    }

    /**
     * Check whether the tupel has the same routing array coordinates without checking the layer
     * @param t tupel to compare with
     * @return
     */
    public boolean isEqualPosition(Tupel t) {
        //rp1.getLocation().getY()
        return (xInsideRoutingArray == t.xInsideRoutingArray && yInsideRoutingArray == t.yInsideRoutingArray);
    }
    
    /**
     * convert x coordinate from electric to routing array
     * @param x x coordinate
     * @return
     */
    public static int convertElectricToRoutingArrayCoordinate_X(double x) {
        return (int) (x - offsetX) / distanceBetweenWires;
    }

    /**
     * convert y coordinate from electric to routing array
     * @param y y coordinate
     * @return
     */
    public static int convertElectricToRoutingArrayCoordinate_Y(double y) {
        return (int) (y - offsetY) / distanceBetweenWires;
    }
    
    /**
     * convert x coordinate from routing array to electric
     * @param x x coordinate
     * @return
     */
    public static int convertRoutingArrayToElectricCoordinates_X(int x) {
        return (x * distanceBetweenWires + distanceBetweenWires / 2) + offsetX;
    }

    /**
     * convert y coordinate from routing array to electric
     * @param y y coordinate
     * @return
     */
    public static int convertRoutingArrayToElectricCoordinates_Y(int y) {
        return (y * distanceBetweenWires + distanceBetweenWires / 2) + offsetY;
    }
    
    /**
     * Convert location to Point2D object
     * @return Point2D with the electric coordinates of the tupel
     */
    public Point2D getLocation(){
    	return new Point2D.Double(xInsideElectric, yInsideElectric);
    }
}
