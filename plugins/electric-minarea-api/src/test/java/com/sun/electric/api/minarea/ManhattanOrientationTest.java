/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.electric.api.minarea;

import com.sun.electric.util.math.Orientation;
import java.awt.geom.AffineTransform;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dn146861
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
	final Orientation OR0      = Orientation.fromC(0, false);
	final Orientation OR90     = Orientation.fromC(900, false);
	final Orientation OR180    = Orientation.fromC(1800, false);
	final Orientation OR270    = Orientation.fromC(2700, false);
	final Orientation OMX      = Orientation.fromC(2700, true);
	final Orientation OMY      = Orientation.fromC(900, true);
	final Orientation OMYR90   = Orientation.fromC(0, true);
	final Orientation OMXR90   = Orientation.fromC(1800, true);
        
        electricOrientation = new Orientation[] {OR0, OR90, OR180, OR270, OMY, OMYR90, OMX, OMXR90};
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
        ManhattanOrientation[] expResult = {
            ManhattanOrientation.R0,
            ManhattanOrientation.R90,
            ManhattanOrientation.R180,
            ManhattanOrientation.R270,
            ManhattanOrientation.MY,
            ManhattanOrientation.MYR90,
            ManhattanOrientation.MX,
            ManhattanOrientation.MXR90
        };
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
        for (ManhattanOrientation mor: ManhattanOrientation.class.getEnumConstants()) {
            Orientation eor = electricOrientation[mor.ordinal()];
            long[] mcoords = {1,0,0,1};
            int[] ecoords = {1,0,0,1};
            mor.transformPoints(mcoords, 0, 2);
            eor.transformPoints(2, ecoords);
            for (int i = 0; i < 4; i++) {
                assertEquals((long)ecoords[i], mcoords[i]);
            }
        }
    }

    /**
     * Test of transformRects method, of class ManhattanOrientation.
     */
    @Test
    public void testTransformRects() {
        System.out.println("transformRects");
        for (ManhattanOrientation mor: ManhattanOrientation.class.getEnumConstants()) {
            Orientation eor = electricOrientation[mor.ordinal()];
            long[] mcoords = {1,2,3,4};
            int[] ecoords = {1,2,3,4};
            mor.transformRects(mcoords, 0, 1);
            eor.rectangleBounds(ecoords);
            for (int i = 0; i < 4; i++) {
                assertEquals((long)ecoords[i], mcoords[i]);
            }
        }
    }

    /**
     * Test of concatenate method, of class ManhattanOrientation.
     */
    @Test
    public void testConcatenate() {
        System.out.println("concatenate");
        for (ManhattanOrientation mor1: ManhattanOrientation.class.getEnumConstants()) {
            Orientation or1 = electricOrientation[mor1.ordinal()];
            for (ManhattanOrientation mor2: ManhattanOrientation.class.getEnumConstants()) {
                Orientation or2 = electricOrientation[mor2.ordinal()];
                assertSame(or1.concatenate(or2).canonic(), electricOrientation[mor1.concatenate(mor2).ordinal()]);
            }
            System.out.println();
        }
    }

    /**
     * Test of affineTrasnform method, of class ManhattanOrientation.
     */
    @Test
    public void testAffineTrasnform() {
        System.out.println("affineTrasnform");
        for (ManhattanOrientation mor: ManhattanOrientation.class.getEnumConstants()) {
            Orientation eor = electricOrientation[mor.ordinal()];
            assertEquals(eor.pureRotate(), mor.affineTransform());
        }
    }

}