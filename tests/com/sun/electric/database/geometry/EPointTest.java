/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EPointTest.java
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

import java.awt.geom.Point2D;
import java.io.Serializable;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class EPointTest {
    
    private EPoint p0;
    
    @Before public void setUp() throws Exception {
        p0 = EPoint.fromGrid(10, 20);
    }

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(EPointTest.class);
    }

    /**
     * Test of fromLambda method, of class com.sun.electric.database.geometry.EPoint.
     */
    @Test public void testFromLambda() {
        System.out.println("fromLambda");
        assertEquals(p0, EPoint.fromLambda(10/DBMath.GRID, 20/DBMath.GRID));
    }

    /**
     * Test of fromGrid method, of class com.sun.electric.database.geometry.EPoint.
     */
    @Test public void testFromGrid() {
        System.out.println("fromGrid");
        assertSame(EPoint.ORIGIN, EPoint.fromGrid(0,0));
    }

    /**
     * Test of snap method, of class com.sun.electric.database.geometry.EPoint.
     */
    @Test public void testSnap() {
        System.out.println("snap");
        assertSame(p0, EPoint.snap(p0));
    }

    /**
     * Test of getX method, of class com.sun.electric.database.geometry.EPoint.
     */
    @Test public void testGetX() {
        System.out.println("getX");
        assertEquals(10/DBMath.GRID, p0.getX());
    }

    /**
     * Test of getY method, of class com.sun.electric.database.geometry.EPoint.
     */
    @Test public void testGetY() {
        System.out.println("getY");
        assertEquals(20/DBMath.GRID, p0.getY());
    }

    /**
     * Test of getLambdaX method, of class com.sun.electric.database.geometry.EPoint.
     */
    @Test public void testGetLambdaX() {
        System.out.println("getLambdaX");
        assertEquals(10/DBMath.GRID, p0.getLambdaX());
    }

    /**
     * Test of getLambdaY method, of class com.sun.electric.database.geometry.EPoint.
     */
    @Test public void testGetLambdaY() {
        System.out.println("getLambdaY");
        assertEquals(20/DBMath.GRID, p0.getLambdaY());
    }

    /**
     * Test of getGridX method, of class com.sun.electric.database.geometry.EPoint.
     */
    @Test public void testGetGridX() {
        System.out.println("getGridX");
        assertEquals(10L, p0.getGridX());
    }

    /**
     * Test of getGridY method, of class com.sun.electric.database.geometry.EPoint.
     */
    @Test public void testGetGridY() {
        System.out.println("getGridY");
        assertEquals(20L, p0.getGridY());
    }

    /**
     * Test of setLocation method, of class com.sun.electric.database.geometry.EPoint.
     */
    @Test(expected = UnsupportedOperationException.class) public void testSetLocation() {
        System.out.println("setLocation");
        p0.setLocation(1, 2);
    }

    /**
     * Test of lambdaMutable method, of class com.sun.electric.database.geometry.EPoint.
     */
    @Test public void testLambdaMutable() {
        System.out.println("lambdaMutable");
        Point2D.Double result = p0.lambdaMutable();
        assertTrue(result instanceof Point2D.Double);
        assertEquals(new Point2D.Double(10/DBMath.GRID, 20/DBMath.GRID), result);
    }

    /**
     * Test of gridMutable method, of class com.sun.electric.database.geometry.EPoint.
     */
    @Test public void testGridMutable() {
        System.out.println("gridMutable");
        Point2D.Double result = p0.gridMutable();
        assertTrue(result instanceof Point2D.Double);
        assertEquals(new Point2D.Double(10, 20), result);
    }

    /**
     * Test of lambdaDistance method, of class com.sun.electric.database.geometry.EPoint.
     */
    @Test public void testLambdaDistance() {
        System.out.println("lambdaDistance");
        assertEquals(Math.sqrt(500)/DBMath.GRID, p0.lambdaDistance(EPoint.ORIGIN));
    }

    /**
     * Test of gridDistance method, of class com.sun.electric.database.geometry.EPoint.
     */
    @Test public void testGridDistance() {
        System.out.println("gridDistance");
        assertEquals(Math.sqrt(500), p0.gridDistance(EPoint.ORIGIN));
    }

    /**
     * Test of isSmall method, of class com.sun.electric.database.geometry.EPoint.
     */
    @Test public void testIsSmall() {
        System.out.println("isSmall");
        assertTrue(p0.isSmall());
        assertFalse((EPoint.fromGrid(-(1 << 30) - 1, 0)).isSmall());
        assertTrue((EPoint.fromGrid(-(1 << 30), 0)).isSmall());
        assertTrue(EPoint.fromGrid((1 << 30) - 1, 0).isSmall());
        assertFalse(EPoint.fromGrid((1 << 30), 0).isSmall());
        assertFalse((EPoint.fromGrid(0, -(1 << 30) - 1)).isSmall());
        assertTrue((EPoint.fromGrid(0, -(1 << 30))).isSmall());
        assertTrue(EPoint.fromGrid(0, (1 << 30) - 1).isSmall());
        assertFalse(EPoint.fromGrid(0, (1 << 30)).isSmall());
    }

    /**
     * Test of toString method, of class com.sun.electric.database.geometry.EPoint.
     */
    @Test public void testToString() {
        System.out.println("toString");
        assertEquals("EPoint["+(10/DBMath.GRID)+", "+(20/DBMath.GRID)+"]", p0.toString());
    }
}
