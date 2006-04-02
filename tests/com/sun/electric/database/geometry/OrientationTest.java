/*
 * OrientationTest.java
 * JUnit based test
 *
 * Created on August 10, 2005, 12:24 PM
 */

package com.sun.electric.database.geometry;

import junit.framework.*;
import java.awt.geom.AffineTransform;
import java.util.HashMap;

/**
 *
 * @author dn146861
 */
public class OrientationTest extends TestCase {
    
    public OrientationTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(OrientationTest.class);
        
        return suite;
    }

    /**
     * Test of fromJava method, of class com.sun.electric.database.geometry.Orientation.
     */
    public void testFromJava() {
        System.out.println("testFromJava");
        
        for (int iX = 0; iX <= 1; iX++) {
            boolean mirrorX = iX != 0;
            for (int iY = 0; iY <= 1; iY++) {
                boolean mirrorY = iY != 0;
                for (int iA = -200; iA < 3800; iA++) {
                    Orientation or = Orientation.fromJava(iA, mirrorX, mirrorY);
                    assertEquals((iA + 3600)%3600, or.getAngle());
                    assertEquals(mirrorX, or.isXMirrored());
                    assertEquals(mirrorY, or.isYMirrored());
                    
                    Orientation orC = Orientation.fromC(or.getCAngle(), or.isCTranspose());
                    assertEquals(or.getCAngle(), orC.getCAngle());
                    assertEquals(or.isCTranspose(), orC.isCTranspose());
                    assertEquals(or.getAngle(), mirrorX ? (orC.getAngle() + 1800) % 3600 : orC.getAngle());
                    assertEquals(false, orC.isXMirrored());
                    assertEquals(mirrorX^mirrorY, orC.isYMirrored());
                    
                    assertTrue(or.pureRotate().equals(orC.pureRotate()));
                    AffineTransform af = AffineTransform.getScaleInstance(mirrorX ? -1 : 1, mirrorY ? -1 : 1);
                    af.rotate(iA * Math.PI / 1800.0);
//                    System.out.println("angle=" + iA + " mX=" + mirrorX + " mY=" + mirrorY);
                    assertEquals(af, or.pureRotate(), Math.ulp(1));
                }
            }
        }
    }

    /**
     * Test of fromC method, of class com.sun.electric.database.geometry.Orientation.
     */
    public void testFromC() {
        System.out.println("testFromC");
        
        for (int iT = 0; iT <= 1; iT++) {
            boolean cTranspose = iT != 0;
            for (int cAngle = -200; cAngle < 3800; cAngle++) {
                Orientation or = Orientation.fromC(cAngle, cTranspose);
                assertEquals((cAngle + 3600) % 3600, or.getCAngle());
                assertEquals(cTranspose, or.isCTranspose());
                assertEquals(false, or.isXMirrored());
                assertEquals(cTranspose, or.isYMirrored());
                assertEquals(cTranspose ? (cAngle + 900) % 3600 : (cAngle + 3600) % 3600, or.getAngle());
                
                Orientation orJ = Orientation.fromJava(or.getAngle(), or.isXMirrored(), or.isYMirrored());
                assertEquals(or.getCAngle(), orJ.getCAngle());
                assertEquals(or.isCTranspose(), orJ.isCTranspose());
            }
        }
    }

    /**
     * Test of concatenate method, of class com.sun.electric.database.geometry.Orientation.
     */
    public void testGetConcatenate() {
        System.out.println("testGetConcatenate");
        
        Orientation[] or = makeConcatTests();
        
        for (int i = 0; i < or.length; i++) {
            Orientation orI = or[i];
            AffineTransform afI = orI.pureRotate();
            for (int j = 0; j < or.length; j++) {
                Orientation orJ = or[j];
                AffineTransform afJ = orJ.pureRotate();
                Orientation orC = orI.concatenate(orJ);
                AffineTransform afC = (AffineTransform)afI.clone();
                afC.concatenate(afJ);
//                System.out.println("i=" + i + " j=" + j);
//                System.out.println("orI=" + orI + " afI=" + afI);
//                System.out.println("orJ=" + orJ + " afJ=" + afJ);
//                System.out.println("orC=" + orC + " afC=" + orC.trans);
//                System.out.println("" + afC);
                assertEquals(afC, orC.pureRotate(), Math.ulp(1));
            }
        }
    }

//    /**
//     * Test of getPreConcatenate method, of class com.sun.electric.database.geometry.Orientation.
//     */
//    public void testGetPreConcatenate() {
//        System.out.println("testGetPreConcatenate");
//        
//        Orientation[] or = makeConcatTests();
//        
//        for (int i = 0; i < or.length; i++) {
//            Orientation orI = or[i];
//            AffineTransform afI = orI.pureRotate();
//            for (int j = 0; j < or.length; j++) {
//                Orientation orJ = or[j];
//                AffineTransform afJ = orJ.pureRotate();
//                Orientation orC = orI.getPreConcatenate(orJ);
//                AffineTransform afC = (AffineTransform)afI.clone();
//                afC.preConcatenate(afJ);
//                assertEquals(afC, orC.pureRotate(), Math.ulp(1));
//            }
//        }
//    }

    private Orientation[] makeConcatTests() {
        Orientation[] or = new Orientation[36*4];
        for (int iX = 0; iX <= 1; iX++) {
            boolean mirrorX = iX != 0;
            for (int iY = 0; iY <= 1; iY++) {
                boolean mirrorY = iY != 0;
                for (int iA = 0; iA < 36; iA++)
                    or[iA*4 + iX*2 + iY*1] = Orientation.fromJava(iA*100, mirrorX, mirrorY);
            }
        }
        return or;
    }
    
    private void assertEquals(AffineTransform expected, AffineTransform actual, double delta) {
//        System.out.println("expected=" + expected);
//        System.out.println("actual=" + actual);
        double[] expectedM = new double[6];
        double[] actualM = new double[expectedM.length];
        expected.getMatrix(expectedM);
        actual.getMatrix(actualM);
        for (int i = 0; i < expectedM.length; i++)
            assertEquals(expectedM[i], actualM[i], delta);
    }
    
//    /**
//     * Test of getCAngle method, of class com.sun.electric.database.geometry.Orientation.
//     */
//    public void testGetCAngle() {
//        System.out.println("testGetCAngle");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of isCTranspose method, of class com.sun.electric.database.geometry.Orientation.
//     */
//    public void testIsCTranspose() {
//        System.out.println("testIsCTranspose");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of getAngle method, of class com.sun.electric.database.geometry.Orientation.
//     */
//    public void testGetAngle() {
//        System.out.println("testGetAngle");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of isXMirrored method, of class com.sun.electric.database.geometry.Orientation.
//     */
//    public void testIsXMirrored() {
//        System.out.println("testIsXMirrored");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of isYMirrored method, of class com.sun.electric.database.geometry.Orientation.
//     */
//    public void testIsYMirrored() {
//        System.out.println("testIsYMirrored");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
    /**
     * Test of toJelibString method, of class com.sun.electric.database.geometry.Orientation.
     */
    public void testToJelibString() {
        System.out.println("testToJelibString");

        assertEquals("", Orientation.fromJava(0, false, false).toJelibString());
        assertEquals("R", Orientation.fromJava(900, false, false).toJelibString());
        assertEquals("RR", Orientation.fromJava(1800, false, false).toJelibString());
        assertEquals("RRR", Orientation.fromJava(2700, false, false).toJelibString());
        assertEquals("X", Orientation.fromJava(0, true, false).toJelibString());
        assertEquals("XR", Orientation.fromJava(900, true, false).toJelibString());
        assertEquals("XRR", Orientation.fromJava(1800, true, false).toJelibString());
        assertEquals("XRRR", Orientation.fromJava(2700, true, false).toJelibString());
        assertEquals("Y", Orientation.fromJava(0, false, true).toJelibString());
        assertEquals("YR", Orientation.fromJava(900, false, true).toJelibString());
        assertEquals("YRR", Orientation.fromJava(1800, false, true).toJelibString());
        assertEquals("YRRR", Orientation.fromJava(2700, false, true).toJelibString());
        assertEquals("XY", Orientation.fromJava(0, true, true).toJelibString());
        assertEquals("XYR", Orientation.fromJava(900, true, true).toJelibString());
        assertEquals("XYRR", Orientation.fromJava(1800, true, true).toJelibString());
        assertEquals("XYRRR", Orientation.fromJava(2700, true, true).toJelibString());
    }

    /**
     * Test of toString method, of class com.sun.electric.database.geometry.Orientation.
     */
    public void testToString() {
        System.out.println("testToString");
        
        assertEquals("", Orientation.fromJava(0, false, false).toString());
        assertEquals("R", Orientation.fromJava(900, false, false).toString());
        assertEquals("RR", Orientation.fromJava(1800, false, false).toString());
        assertEquals("RRR", Orientation.fromJava(2700, false, false).toString());
        assertEquals("X", Orientation.fromJava(0, true, false).toString());
        assertEquals("XR", Orientation.fromJava(900, true, false).toString());
        assertEquals("XRR", Orientation.fromJava(1800, true, false).toString());
        assertEquals("XRRR", Orientation.fromJava(2700, true, false).toString());
        assertEquals("Y", Orientation.fromJava(0, false, true).toString());
        assertEquals("YR", Orientation.fromJava(900, false, true).toString());
        assertEquals("YRR", Orientation.fromJava(1800, false, true).toString());
        assertEquals("YRRR", Orientation.fromJava(2700, false, true).toString());
        assertEquals("XY", Orientation.fromJava(0, true, true).toString());
        assertEquals("XYR", Orientation.fromJava(900, true, true).toString());
        assertEquals("XYRR", Orientation.fromJava(1800, true, true).toString());
        assertEquals("XYRRR", Orientation.fromJava(2700, true, true).toString());
    }
}
