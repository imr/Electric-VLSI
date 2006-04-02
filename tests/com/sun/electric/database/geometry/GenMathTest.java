/*
 * GenMathTest.java
 * JUnit based test
 *
 * Created on July 3, 2005, 11:24 AM
 */

package com.sun.electric.database.geometry;

import junit.framework.*;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author dn146861
 */
public class GenMathTest extends TestCase {
    
    public GenMathTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

//    public static class MutableIntegerTest extends TestCase {
//
//        public MutableIntegerTest(java.lang.String testName) {
//
//            super(testName);
//        }
//
//        protected void setUp() throws Exception {
//        }
//
//        protected void tearDown() throws Exception {
//        }
//
//        public static Test suite() {
//            TestSuite suite = new TestSuite(MutableIntegerTest.class);
//            
//            return suite;
//        }
//
//        /**
//         * Test of setValue method, of class com.sun.electric.database.geometry.GenMath.MutableInteger.
//         */
//        public void testSetValue() {
//            System.out.println("testSetValue");
//            
//            // TODO add your test code below by replacing the default call to fail.
//            fail("The test case is empty.");
//        }
//
//        /**
//         * Test of increment method, of class com.sun.electric.database.geometry.GenMath.MutableInteger.
//         */
//        public void testIncrement() {
//            System.out.println("testIncrement");
//            
//            // TODO add your test code below by replacing the default call to fail.
//            fail("The test case is empty.");
//        }
//
//        /**
//         * Test of intValue method, of class com.sun.electric.database.geometry.GenMath.MutableInteger.
//         */
//        public void testIntValue() {
//            System.out.println("testIntValue");
//            
//            // TODO add your test code below by replacing the default call to fail.
//            fail("The test case is empty.");
//        }
//
//        /**
//         * Test of toString method, of class com.sun.electric.database.geometry.GenMath.MutableInteger.
//         */
//        public void testToString() {
//            System.out.println("testToString");
//            
//            // TODO add your test code below by replacing the default call to fail.
//            fail("The test case is empty.");
//        }
//    }
//
//
//    public static class MutableDoubleTest extends TestCase {
//
//        public MutableDoubleTest(java.lang.String testName) {
//
//            super(testName);
//        }
//
//        protected void setUp() throws Exception {
//        }
//
//        protected void tearDown() throws Exception {
//        }
//
//        public static Test suite() {
//            TestSuite suite = new TestSuite(MutableDoubleTest.class);
//            
//            return suite;
//        }
//
//        /**
//         * Test of setValue method, of class com.sun.electric.database.geometry.GenMath.MutableDouble.
//         */
//        public void testSetValue() {
//            System.out.println("testSetValue");
//            
//            // TODO add your test code below by replacing the default call to fail.
//            fail("The test case is empty.");
//        }
//
//        /**
//         * Test of doubleValue method, of class com.sun.electric.database.geometry.GenMath.MutableDouble.
//         */
//        public void testDoubleValue() {
//            System.out.println("testDoubleValue");
//            
//            // TODO add your test code below by replacing the default call to fail.
//            fail("The test case is empty.");
//        }
//
//        /**
//         * Test of toString method, of class com.sun.electric.database.geometry.GenMath.MutableDouble.
//         */
//        public void testToString() {
//            System.out.println("testToString");
//            
//            // TODO add your test code below by replacing the default call to fail.
//            fail("The test case is empty.");
//        }
//    }
//
//
//    public static Test suite() {
//        TestSuite suite = new TestSuite(GenMathTest.class);
//        
//        return suite;
//    }
//
//    /**
//     * Test of addToBag method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testAddToBag() {
//        System.out.println("testAddToBag");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of countInBag method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testCountInBag() {
//        System.out.println("testCountInBag");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of objectsReallyEqual method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testObjectsReallyEqual() {
//        System.out.println("testObjectsReallyEqual");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of figureAngle method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testFigureAngle() {
//        System.out.println("testFigureAngle");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of figureAngleRadians method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testFigureAngleRadians() {
//        System.out.println("testFigureAngleRadians");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of addPoints method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testAddPoints() {
//        System.out.println("testAddPoints");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of isOnLine method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testIsOnLine() {
//        System.out.println("testIsOnLine");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of closestPointToSegment method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testClosestPointToSegment() {
//        System.out.println("testClosestPointToSegment");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of closestPointToLine method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testClosestPointToLine() {
//        System.out.println("testClosestPointToLine");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of arcconnects method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testArcconnects() {
//        System.out.println("testArcconnects");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of distToLine method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testDistToLine() {
//        System.out.println("testDistToLine");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of computeArcCenter method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testComputeArcCenter() {
//        System.out.println("testComputeArcCenter");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of findCenters method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testFindCenters() {
//        System.out.println("testFindCenters");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of pointInRect method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testPointInRect() {
//        System.out.println("testPointInRect");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of transformRect method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testTransformRect() {
//        System.out.println("testTransformRect");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of rectsIntersect method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testRectsIntersect() {
//        System.out.println("testRectsIntersect");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of intersect method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testIntersect() {
//        System.out.println("testIntersect");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of intersectRadians method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testIntersectRadians() {
//        System.out.println("testIntersectRadians");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of arcBBox method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testArcBBox() {
//        System.out.println("testArcBBox");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of toNearest method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testToNearest() {
//        System.out.println("testToNearest");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of doublesEqual method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testDoublesEqual() {
//        System.out.println("testDoublesEqual");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of doublesLessThan method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testDoublesLessThan() {
//        System.out.println("testDoublesLessThan");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of doublesClose method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testDoublesClose() {
//        System.out.println("testDoublesClose");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of clipLine method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testClipLine() {
//        System.out.println("testClipLine");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of clipPoly method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testClipPoly() {
//        System.out.println("testClipPoly");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of sin method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testSin() {
//        System.out.println("testSin");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of cos method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testCos() {
//        System.out.println("testCos");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }
//
//    /**
//     * Test of unsignedIntValue method, of class com.sun.electric.database.geometry.GenMath.
//     */
//    public void testUnsignedIntValue() {
//        System.out.println("testUnsignedIntValue");
//        
//        // TODO add your test code below by replacing the default call to fail.
//        fail("The test case is empty.");
//    }

    /**
     * Test of primeSince method, of class com.sun.electric.database.geometry.GenMath.
     */
    public void testPrimeSince() {
        System.out.println("testPrimeSince");
        
        int prime = GenMath.primeSince(Integer.MIN_VALUE);
        for (;;) {
            assertPrime(prime);
            assertEquals(prime, GenMath.primeSince(prime - 1));
            assertEquals(prime, GenMath.primeSince(prime));
            if (prime == Integer.MAX_VALUE) break;
            int newPrime = GenMath.primeSince(prime + 1);
            assertTrue(newPrime >= prime + 1);
            prime = newPrime;
        }
    }
    
    private void assertPrime(int p) {
        assertTrue(p >= 2);
        if (p == 2) return;
        assertTrue(p%2 != 0);
        for (int i = 3; i <= p/i; i += 2)
            assertTrue(p%i != 0);
    }
}
