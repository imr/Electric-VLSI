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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.awt.geom.AffineTransform;

/**
 *
 */
public class ManhattanOrientationTest {

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
            int[] mcoords = {1, 0, 0, 1};
            double[] acoords = {1, 0, 0, 1};
            mor.transformPoints(mcoords, 0, 2);
            mor.affineTransform().transform(acoords, 0, acoords, 0, 2);

            for (int i = 0; i < 4; i++) {
                assertEquals(acoords[i], mcoords[i], 0.0);
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
            int[] mcoords = {1, 2, 3, 4};
            mor.transformRects(mcoords, 0, 1);
            double[] acoords = {1, 2, 3, 4};
            mor.affineTransform().transform(acoords, 0, acoords, 0, 2);
            assertEquals(Math.min(acoords[0], acoords[2]), mcoords[0], 0.0);
            assertEquals(Math.min(acoords[1], acoords[3]), mcoords[1], 0.0);
            assertEquals(Math.max(acoords[0], acoords[2]), mcoords[2], 0.0);
            assertEquals(Math.max(acoords[1], acoords[3]), mcoords[3], 0.0);
        }
    }

    /**
     * Test of concatenate method, of class ManhattanOrientation.
     */
    @Test
    public void testConcatenate() {
        System.out.println("concatenate");
        for (ManhattanOrientation mor1 : ManhattanOrientation.class.getEnumConstants()) {
            for (ManhattanOrientation mor2 : ManhattanOrientation.class.getEnumConstants()) {
                AffineTransform expected = mor1.affineTransform();
                expected.concatenate(mor2.affineTransform());
                assertEquals(expected, mor1.concatenate(mor2).affineTransform());
            }
        }
    }

    /**
     * Test of affineTrasnform method, of class ManhattanOrientation.
     */
    @Test
    public void testAffineTrasnform() {
        System.out.println("affineTrasnform");
        AffineTransform aR0 = new AffineTransform();
        aR0.quadrantRotate(0);
        AffineTransform aR90 = new AffineTransform();
        aR90.quadrantRotate(1);
        AffineTransform aR180 = new AffineTransform();
        aR180.quadrantRotate(2);
        AffineTransform aR270 = new AffineTransform();
        aR270.quadrantRotate(3);
        AffineTransform aMY = new AffineTransform();
        aMY.quadrantRotate(0);
        aMY.scale(-1, 1);
        AffineTransform aMYR90 = new AffineTransform();
        aMYR90.quadrantRotate(1);
        aMYR90.scale(-1, 1);
        AffineTransform aMX = new AffineTransform();
        aMX.quadrantRotate(2);
        aMX.scale(-1, 1);
        AffineTransform aMXR90 = new AffineTransform();
        aMXR90.quadrantRotate(3);
        aMXR90.scale(-1, 1);

        assertEquals(aR0, ManhattanOrientation.R0.affineTransform());
        assertEquals(aR90, ManhattanOrientation.R90.affineTransform());
        assertEquals(aR180, ManhattanOrientation.R180.affineTransform());
        assertEquals(aR270, ManhattanOrientation.R270.affineTransform());
        assertEquals(aMY, ManhattanOrientation.MY.affineTransform());
        assertEquals(aMYR90, ManhattanOrientation.MYR90.affineTransform());
        assertEquals(aMX, ManhattanOrientation.MX.affineTransform());
        assertEquals(aMXR90, ManhattanOrientation.MXR90.affineTransform());
    }
}
