/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ManhattanOrientationTest.java
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.electric.api.minarea.geometry.Point;
import com.sun.electric.util.math.Orientation;

/**
 *
 */
public class ManhattanOrientationTest {

    private Orientation[] electricOrientation;

    public ManhattanOrientationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        final Orientation OR0 = Orientation.fromC(0, false);
        final Orientation OR90 = Orientation.fromC(900, false);
        final Orientation OR180 = Orientation.fromC(1800, false);
        final Orientation OR270 = Orientation.fromC(2700, false);
        final Orientation OMX = Orientation.fromC(2700, true);
        final Orientation OMY = Orientation.fromC(900, true);
        final Orientation OMYR90 = Orientation.fromC(0, true);
        final Orientation OMXR90 = Orientation.fromC(1800, true);

        electricOrientation = new Orientation[]{OR0, OR90, OR180, OR270, OMY, OMYR90, OMX, OMXR90};
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of values method, of class ManhattanOrientation.
     */
    @Test
    public void testValues() {
        System.out.println("values");
        ManhattanOrientation[] expResult = {ManhattanOrientation.R0, ManhattanOrientation.R90,
            ManhattanOrientation.R180, ManhattanOrientation.R270, ManhattanOrientation.MY,
            ManhattanOrientation.MYR90, ManhattanOrientation.MX, ManhattanOrientation.MXR90};
        ManhattanOrientation[] result = ManhattanOrientation.values();
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of valueOf method, of class ManhattanOrientation.
     */
    @Test
    public void testValueOf() {
        System.out.println("valueOf");
        String name = "R90";
        ManhattanOrientation expResult = ManhattanOrientation.R90;
        ManhattanOrientation result = ManhattanOrientation.valueOf(name);
        assertEquals(expResult, result);
    }

    /**
     * Test of transformPoints method, of class ManhattanOrientation.
     */
    @Test
    public void testTransformPoints() {
        System.out.println("transformPoints");
        for (ManhattanOrientation mor : ManhattanOrientation.class.getEnumConstants()) {
            System.out.println("Test: " + mor.toString());
            Orientation eor = electricOrientation[mor.ordinal()];
            long[] mcoords = {1, 0, 0, 1};
            int[] ecoords = {1, 0, 0, 1};
            mor.transformPoints(mcoords, 0, 2);
            eor.transformPoints(2, ecoords);

            for (int i = 0; i < 4; i++) {
                assertEquals((long) ecoords[i], mcoords[i]);
            }
        }
    }

    /**
     * Test of transformPoints method, of class ManhattanOrientation.
     */
    @Test
    public void testTransformPoint() {
        System.out.println("transformPoints");
        for (ManhattanOrientation mor : ManhattanOrientation.class.getEnumConstants()) {
            System.out.println("Test: " + mor.toString());
            Orientation eor = electricOrientation[mor.ordinal()];
            Point[] mpoints = {new Point(1, 0), new Point(0, 1)};
            int[] ecoords = {1, 0, 0, 1};
            eor.transformPoints(2, ecoords);

            for (int i = 0; i < 2; i++) {
                Point p = mor.transformPoint(mpoints[i]);
                assertEquals(ecoords[i * 2 + 0], p.getX());
                assertEquals(ecoords[i * 2 + 1], p.getY());
            }
        }
    }

    /**
     * Test of transformRects method, of class ManhattanOrientation.
     */
    @Test
    public void testTransformRects() {
        System.out.println("transformRects");
        for (ManhattanOrientation mor : ManhattanOrientation.class.getEnumConstants()) {
            Orientation eor = electricOrientation[mor.ordinal()];
            int[] mcoords = {1, 2, 3, 4};
            int[] ecoords = {1, 2, 3, 4};
            mor.transformRects(mcoords, 0, 1);
            eor.rectangleBounds(ecoords);
            assertArrayEquals(ecoords, mcoords);
        }
    }

    /**
     * Test of concatenate method, of class ManhattanOrientation.
     */
    @Test
    public void testConcatenate() {
        System.out.println("concatenate");
        for (ManhattanOrientation mor1 : ManhattanOrientation.class.getEnumConstants()) {
            Orientation or1 = electricOrientation[mor1.ordinal()];
            for (ManhattanOrientation mor2 : ManhattanOrientation.class.getEnumConstants()) {
                Orientation or2 = electricOrientation[mor2.ordinal()];
                assertSame(or1.concatenate(or2).canonic(), electricOrientation[mor1.concatenate(mor2).ordinal()]);
            }
        }
    }

    /**
     * Test of affineTrasnform method, of class ManhattanOrientation.
     */
    @Test
    public void testAffineTrasnform() {
        System.out.println("affineTrasnform");
        for (ManhattanOrientation mor : ManhattanOrientation.class.getEnumConstants()) {
            Orientation eor = electricOrientation[mor.ordinal()];
            assertEquals(eor.pureRotate(), mor.affineTransform());
        }
    }
}
