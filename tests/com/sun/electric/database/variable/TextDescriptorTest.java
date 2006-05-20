/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TextDescriptorTest.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.variable;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.tool.user.User;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class TextDescriptorTest {
    
    AbstractTextDescriptor emptyDescriptor;
    
    @Before public void setUp() throws Exception {
        emptyDescriptor = new MutableTextDescriptor();
    }

    protected void tearDown() throws Exception {
    }

    public static class PositionTest {

        protected void setUp() throws Exception {
        }

        protected void tearDown() throws Exception {
        }

//        public static Test suite() {
//            TestSuite suite = new TestSuite(PositionTest.class);
//            
//            return suite;
//        }

        /**
         * Test of getIndex method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Position.
         */
        public void testGetIndex() {
            System.out.println("getIndex");
            
            AbstractTextDescriptor.Position instance = null;
            
            int expResult = 0;
            int result = instance.getIndex();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getPolyType method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Position.
         */
        public void testGetPolyType() {
            System.out.println("getPolyType");
            
            AbstractTextDescriptor.Position instance = null;
            
            Poly.Type expResult = null;
            Poly.Type result = instance.getPolyType();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of align method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Position.
         */
        public void testAlign() {
            System.out.println("align");
            
            int dx = 0;
            int dy = 0;
            AbstractTextDescriptor.Position instance = null;
            
            AbstractTextDescriptor.Position expResult = null;
            AbstractTextDescriptor.Position result = instance.align(dx, dy);
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getPosition method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Position.
         */
        public void testGetPosition() {
            System.out.println("getPosition");
            
            Poly.Type type = null;
            
            AbstractTextDescriptor.Position expResult = null;
            AbstractTextDescriptor.Position result = AbstractTextDescriptor.Position.getPosition(type);
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getNumPositions method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Position.
         */
        public void testGetNumPositions() {
            System.out.println("getNumPositions");
            
            int expResult = 0;
            int result = AbstractTextDescriptor.Position.getNumPositions();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getPositionAt method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Position.
         */
        public void testGetPositionAt() {
            System.out.println("getPositionAt");
            
            int index = 0;
            
            AbstractTextDescriptor.Position expResult = null;
            AbstractTextDescriptor.Position result = AbstractTextDescriptor.Position.getPositionAt(index);
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getPositions method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Position.
         */
        public void testGetPositions() {
            System.out.println("getPositions");
            
            Iterator<AbstractTextDescriptor.Position> expResult = null;
            Iterator<AbstractTextDescriptor.Position> result = AbstractTextDescriptor.Position.getPositions();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of toString method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Position.
         */
        public void testToString() {
            System.out.println("toString");
            
            AbstractTextDescriptor.Position instance = null;
            
            String expResult = "";
            String result = instance.toString();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }
    }


    public static class DispPosTest {

        protected void setUp() throws Exception {
        }

        protected void tearDown() throws Exception {
        }

//        public static Test suite() {
//            TestSuite suite = new TestSuite(DispPosTest.class);
//            
//            return suite;
//        }

        /**
         * Test of getIndex method, of class com.sun.electric.database.variable.AbstractTextDescriptor.DispPos.
         */
        public void testGetIndex() {
            System.out.println("getIndex");
            
            AbstractTextDescriptor.DispPos instance = null;
            
            int expResult = 0;
            int result = instance.getIndex();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getName method, of class com.sun.electric.database.variable.AbstractTextDescriptor.DispPos.
         */
        public void testGetName() {
            System.out.println("getName");
            
            AbstractTextDescriptor.DispPos instance = null;
            
            String expResult = "";
            String result = instance.getName();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getNumShowStyles method, of class com.sun.electric.database.variable.AbstractTextDescriptor.DispPos.
         */
        public void testGetNumShowStyles() {
            System.out.println("getNumShowStyles");
            
            int expResult = 0;
            int result = AbstractTextDescriptor.DispPos.getNumShowStyles();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getShowStylesAt method, of class com.sun.electric.database.variable.AbstractTextDescriptor.DispPos.
         */
        public void testGetShowStylesAt() {
            System.out.println("getShowStylesAt");
            
            int index = 0;
            
            AbstractTextDescriptor.DispPos expResult = null;
            AbstractTextDescriptor.DispPos result = AbstractTextDescriptor.DispPos.getShowStylesAt(index);
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getShowStyles method, of class com.sun.electric.database.variable.AbstractTextDescriptor.DispPos.
         */
        public void testGetShowStyles() {
            System.out.println("getShowStyles");
            
            Iterator<AbstractTextDescriptor.DispPos> expResult = null;
            Iterator<AbstractTextDescriptor.DispPos> result = AbstractTextDescriptor.DispPos.getShowStyles();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of toString method, of class com.sun.electric.database.variable.AbstractTextDescriptor.DispPos.
         */
        public void testToString() {
            System.out.println("toString");
            
            AbstractTextDescriptor.DispPos instance = null;
            
            String expResult = "";
            String result = instance.toString();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }
    }


    public static class SizeTest {

        protected void setUp() throws Exception {
        }

        protected void tearDown() throws Exception {
        }

//        public static Test suite() {
//            TestSuite suite = new TestSuite(SizeTest.class);
//            
//            return suite;
//        }

        /**
         * Test of getBits method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Size.
         */
        public void testGetBits() {
            System.out.println("getBits");
            
            AbstractTextDescriptor.Size instance = null;
            
            int expResult = 0;
            int result = instance.getBits();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of newAbsSize method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Size.
         */
        public void testNewAbsSize() {
            System.out.println("newAbsSize");
            
            int size = 0;
            
            AbstractTextDescriptor.Size expResult = null;
            AbstractTextDescriptor.Size result = AbstractTextDescriptor.Size.newAbsSize(size);
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of newRelSize method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Size.
         */
        public void testNewRelSize() {
            System.out.println("newRelSize");
            
            double size = 0.0;
            
            AbstractTextDescriptor.Size expResult = null;
            AbstractTextDescriptor.Size result = AbstractTextDescriptor.Size.newRelSize(size);
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getSize method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Size.
         */
        public void testGetSize() {
            System.out.println("getSize");
            
            AbstractTextDescriptor.Size instance = null;
            
            double expResult = 0.0;
            double result = instance.getSize();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of isAbsolute method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Size.
         */
        public void testIsAbsolute() {
            System.out.println("isAbsolute");
            
            AbstractTextDescriptor.Size instance = null;
            
            boolean expResult = true;
            boolean result = instance.isAbsolute();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of equals method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Size.
         */
        public void testEquals() {
            System.out.println("equals");
            
            AbstractTextDescriptor.Size other = null;
            AbstractTextDescriptor.Size instance = null;
            
            boolean expResult = true;
            boolean result = instance.equals(other);
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of toString method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Size.
         */
        public void testToString() {
            System.out.println("toString");
            
            AbstractTextDescriptor.Size instance = null;
            
            String expResult = "";
            String result = instance.toString();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }
    }


    public static class RotationTest {

        protected void setUp() throws Exception {
        }

        protected void tearDown() throws Exception {
        }

//        public static Test suite() {
//            TestSuite suite = new TestSuite(RotationTest.class);
//            
//            return suite;
//        }

        /**
         * Test of getIndex method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Rotation.
         */
        public void testGetIndex() {
            System.out.println("getIndex");
            
            AbstractTextDescriptor.Rotation instance = null;
            
            int expResult = 0;
            int result = instance.getIndex();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getDescription method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Rotation.
         */
        public void testGetDescription() {
            System.out.println("getDescription");
            
            AbstractTextDescriptor.Rotation instance = null;
            
            String expResult = "";
            String result = instance.getDescription();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getAngle method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Rotation.
         */
        public void testGetAngle() {
            System.out.println("getAngle");
            
            AbstractTextDescriptor.Rotation instance = null;
            
            int expResult = 0;
            int result = instance.getAngle();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getRotation method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Rotation.
         */
        public void testGetRotation() {
            System.out.println("getRotation");
            
            int angle = 0;
            
            AbstractTextDescriptor.Rotation expResult = null;
            AbstractTextDescriptor.Rotation result = AbstractTextDescriptor.Rotation.getRotation(angle);
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getNumRotations method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Rotation.
         */
        public void testGetNumRotations() {
            System.out.println("getNumRotations");
            
            int expResult = 0;
            int result = AbstractTextDescriptor.Rotation.getNumRotations();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getRotationAt method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Rotation.
         */
        public void testGetRotationAt() {
            System.out.println("getRotationAt");
            
            int index = 0;
            
            AbstractTextDescriptor.Rotation expResult = null;
            AbstractTextDescriptor.Rotation result = AbstractTextDescriptor.Rotation.getRotationAt(index);
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getRotations method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Rotation.
         */
        public void testGetRotations() {
            System.out.println("getRotations");
            
            Iterator<AbstractTextDescriptor.Rotation> expResult = null;
            Iterator<AbstractTextDescriptor.Rotation> result = AbstractTextDescriptor.Rotation.getRotations();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of toString method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Rotation.
         */
        public void testToString() {
            System.out.println("toString");
            
            AbstractTextDescriptor.Rotation instance = null;
            
            String expResult = "";
            String result = instance.toString();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }
    }


    public static class UnitTest {

        protected void setUp() throws Exception {
        }

        protected void tearDown() throws Exception {
        }

//        public static Test suite() {
//            TestSuite suite = new TestSuite(UnitTest.class);
//            
//            return suite;
//        }

        /**
         * Test of getIndex method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Unit.
         */
        public void testGetIndex() {
            System.out.println("getIndex");
            
            AbstractTextDescriptor.Unit instance = null;
            
            int expResult = 0;
            int result = instance.getIndex();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getDescription method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Unit.
         */
        public void testGetDescription() {
            System.out.println("getDescription");
            
            AbstractTextDescriptor.Unit instance = null;
            
            String expResult = "";
            String result = instance.getDescription();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getNumUnits method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Unit.
         */
        public void testGetNumUnits() {
            System.out.println("getNumUnits");
            
            int expResult = 0;
            int result = AbstractTextDescriptor.Unit.getNumUnits();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getUnitAt method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Unit.
         */
        public void testGetUnitAt() {
            System.out.println("getUnitAt");
            
            int index = 0;
            
            AbstractTextDescriptor.Unit expResult = null;
            AbstractTextDescriptor.Unit result = AbstractTextDescriptor.Unit.getUnitAt(index);
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getUnits method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Unit.
         */
        public void testGetUnits() {
            System.out.println("getUnits");
            
            Iterator<AbstractTextDescriptor.Unit> expResult = null;
            Iterator<AbstractTextDescriptor.Unit> result = AbstractTextDescriptor.Unit.getUnits();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of toString method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Unit.
         */
        public void testToString() {
            System.out.println("toString");
            
            AbstractTextDescriptor.Unit instance = null;
            
            String expResult = "";
            String result = instance.toString();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }
    }


    public static class ActiveFontTest {

        protected void setUp() throws Exception {
        }

        protected void tearDown() throws Exception {
        }

//        public static Test suite() {
//            TestSuite suite = new TestSuite(ActiveFontTest.class);
//            
//            return suite;
//        }

        /**
         * Test of getMaxIndex method, of class com.sun.electric.database.variable.AbstractTextDescriptor.ActiveFont.
         */
        public void testGetMaxIndex() {
            System.out.println("getMaxIndex");
            
            int expResult = 0;
            int result = AbstractTextDescriptor.ActiveFont.getMaxIndex();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getIndex method, of class com.sun.electric.database.variable.AbstractTextDescriptor.ActiveFont.
         */
        public void testGetIndex() {
            System.out.println("getIndex");
            
            AbstractTextDescriptor.ActiveFont instance = null;
            
            int expResult = 0;
            int result = instance.getIndex();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of getName method, of class com.sun.electric.database.variable.AbstractTextDescriptor.ActiveFont.
         */
        public void testGetName() {
            System.out.println("getName");
            
            AbstractTextDescriptor.ActiveFont instance = null;
            
            String expResult = "";
            String result = instance.getName();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of findActiveFont method, of class com.sun.electric.database.variable.AbstractTextDescriptor.ActiveFont.
         */
        public void testFindActiveFont() {
            System.out.println("findActiveFont");
            
            String fontName = "";
            
            AbstractTextDescriptor.ActiveFont expResult = null;
            AbstractTextDescriptor.ActiveFont result = AbstractTextDescriptor.ActiveFont.findActiveFont(fontName);
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of toString method, of class com.sun.electric.database.variable.AbstractTextDescriptor.ActiveFont.
         */
        public void testToString() {
            System.out.println("toString");
            
            AbstractTextDescriptor.ActiveFont instance = null;
            
            String expResult = "";
            String result = instance.toString();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }
    }


    /**
     * Test of getCFlags method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Code.
     */
    @Test public void testCodeGetCFlags() {
        System.out.println("getCFlags");
        
        TextDescriptor.Code instance = TextDescriptor.Code.NONE;
        
        int expResult = 0;
        int result = instance.getCFlags();
        assertEquals(expResult, result);
    }
    
    /**
     * Test of toString method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Code.
     */
    @Test public void testCodeToString() {
        System.out.println("toString");
        
        AbstractTextDescriptor.Code instance = TextDescriptor.Code.NONE;
        
        String expResult = "Not Code";
        String result = instance.toString();
        assertEquals(expResult, result);
    }
    
    /**
     * Test of getCodes method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Code.
     */
    @Test public void testCodeGetCodes() {
        System.out.println("getCodes");
        
        List<TextDescriptor.Code> expResult = Arrays.asList(
                TextDescriptor.Code.JAVA,
                TextDescriptor.Code.LISP,
                TextDescriptor.Code.TCL,
                TextDescriptor.Code.NONE
                );
        Iterator<TextDescriptor.Code> result = TextDescriptor.Code.getCodes();
        int i = 0;
        while (result.hasNext())
            assertEquals(expResult.get(i++), result.next());
        assertEquals(expResult.size(), i);
    }
    
    /**
     * Test of getByCBits method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Code.
     */
    @Test public void testCodeGetByCBits() {
        System.out.println("getByCBits");
        
        int cBits = 0;
        
        TextDescriptor.Code expResult = TextDescriptor.Code.NONE;
        TextDescriptor.Code result = TextDescriptor.Code.getByCBits(cBits);
        assertEquals(expResult, result);
    }
    
    
    public static class DescriptorPrefTest {

        protected void setUp() throws Exception {
        }

        protected void tearDown() throws Exception {
        }

//        public static Test suite() {
//            TestSuite suite = new TestSuite(DescriptorPrefTest.class);
//            
//            return suite;
//        }
//
        /**
         * Test of newTextDescriptor method, of class com.sun.electric.database.variable.AbstractTextDescriptor.DescriptorPref.
         */
        public void testNewTextDescriptor() {
            System.out.println("newTextDescriptor");
            
            boolean display = true;
            AbstractTextDescriptor.DescriptorPref instance = null;
            
            TextDescriptor expResult = null;
            TextDescriptor result = instance.newTextDescriptor(display);
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of newMutableTextDescriptor method, of class com.sun.electric.database.variable.AbstractTextDescriptor.DescriptorPref.
         */
        public void testNewMutableTextDescriptor() {
            System.out.println("newMutableTextDescriptor");
            
            AbstractTextDescriptor.DescriptorPref instance = null;
            
            MutableTextDescriptor expResult = null;
            MutableTextDescriptor result = instance.newMutableTextDescriptor();
            assertEquals(expResult, result);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }

        /**
         * Test of setTextDescriptor method, of class com.sun.electric.database.variable.AbstractTextDescriptor.DescriptorPref.
         */
        public void testSetTextDescriptor() {
            System.out.println("setTextDescriptor");
            
            AbstractTextDescriptor td = null;
            AbstractTextDescriptor.DescriptorPref instance = null;
            
            instance.setTextDescriptor(td);
            
            // TODO review the generated test code and remove the default call to fail.
            fail("The test case is a prototype.");
        }
    }


    public static junit.framework.Test suite() {
        junit.framework.Test suite = new junit.framework.JUnit4TestAdapter(TextDescriptorTest.class);
//        suite.addTestSuite(CodeTest.class);
        
        return suite;
    }

//    /**
//     * Test of setNodeTextDescriptor method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
//     */
//    public void testSetNodeTextDescriptor() {
//        System.out.println("setNodeTextDescriptor");
//        
//        AbstractTextDescriptor td = null;
//        
//        AbstractTextDescriptor.setNodeTextDescriptor(td);
//        
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setArcTextDescriptor method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
//     */
//    public void testSetArcTextDescriptor() {
//        System.out.println("setArcTextDescriptor");
//        
//        AbstractTextDescriptor td = null;
//        
//        AbstractTextDescriptor.setArcTextDescriptor(td);
//        
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setExportTextDescriptor method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
//     */
//    public void testSetExportTextDescriptor() {
//        System.out.println("setExportTextDescriptor");
//        
//        AbstractTextDescriptor td = null;
//        
//        AbstractTextDescriptor.setExportTextDescriptor(td);
//        
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setAnnotationTextDescriptor method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
//     */
//    public void testSetAnnotationTextDescriptor() {
//        System.out.println("setAnnotationTextDescriptor");
//        
//        AbstractTextDescriptor td = null;
//        
//        AbstractTextDescriptor.setAnnotationTextDescriptor(td);
//        
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setInstanceTextDescriptor method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
//     */
//    public void testSetInstanceTextDescriptor() {
//        System.out.println("setInstanceTextDescriptor");
//        
//        AbstractTextDescriptor td = null;
//        
//        AbstractTextDescriptor.setInstanceTextDescriptor(td);
//        
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setCellTextDescriptor method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
//     */
//    public void testSetCellTextDescriptor() {
//        System.out.println("setCellTextDescriptor");
//        
//        AbstractTextDescriptor td = null;
//        
//        AbstractTextDescriptor.setCellTextDescriptor(td);
//        
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of hashCode method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
//     */
//    public void testHashCode() {
//        System.out.println("hashCode");
//        
//        int expResult = TextDescriptor.Code.NONE.hashCode();
//        int result = emptyDescriptor.hashCode();
//        assertEquals(expResult, result);
//        
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of equals method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testEquals() {
        System.out.println("equals");
        
        Object anObject = new MutableTextDescriptor();
        
        boolean expResult = true;
        boolean result = emptyDescriptor.equals(anObject);
        assertEquals(expResult, result);
    }

//    /**
//     * Test of lowLevelGet method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
//     */
//    public void testLowLevelGet() {
//        System.out.println("lowLevelGet");
//        
//        long expResult = 0L;
//        long result = emptyDescriptor.lowLevelGet();
//        assertEquals(expResult, result);
//    }

    @Test public void testSerialization() {
        TextDescriptor emptyImmutableDescriptor = TextDescriptor.newTextDescriptor(emptyDescriptor);
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteStream);
            out.writeObject(emptyDescriptor);
            out.writeObject(emptyImmutableDescriptor);
            out.close();
            byte[] serializedRect = byteStream.toByteArray();
            
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serializedRect));
            MutableTextDescriptor mtd = (MutableTextDescriptor)in.readObject();
            TextDescriptor td = (TextDescriptor)in.readObject();
            in.close();
            
            assertEquals(emptyDescriptor, mtd);
            assertSame(emptyImmutableDescriptor, td);
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (ClassNotFoundException e) {
            fail(e.getMessage());
        }
    }
    
    /**
     * Test of lowLevelGet0 method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testLowLevelGet0() {
        System.out.println("lowLevelGet0");
        
        int expResult = (int)emptyDescriptor.lowLevelGet();
        int result = emptyDescriptor.lowLevelGet0();
        assertEquals(expResult, result);
    }

    /**
     * Test of lowLevelGet1 method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testLowLevelGet1() {
        System.out.println("lowLevelGet1");
        
        int expResult = (int)(emptyDescriptor.lowLevelGet() >>> 32);
        int result = emptyDescriptor.lowLevelGet1();
        assertEquals(expResult, result);
    }

    /**
     * Test of isDisplay method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testIsDisplay() {
        System.out.println("isDisplay");
        
        boolean expResult = true;
        boolean result = emptyDescriptor.isDisplay();
        assertEquals(expResult, result);
    }

    /**
     * Test of getPos method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testGetPos() {
        System.out.println("getPos");
        
        TextDescriptor.Position expResult = TextDescriptor.Position.CENT;
        TextDescriptor.Position result = emptyDescriptor.getPos();
        assertEquals(expResult, result);
    }

    /**
     * Test of getSize method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testGetSize() {
        System.out.println("getSize");
        
        TextDescriptor.Size expResult = TextDescriptor.Size.newRelSize(1);
        TextDescriptor.Size result = emptyDescriptor.getSize();
        assertEquals(expResult, result);
    }

    /**
     * Test of getTrueSize method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testGetTrueSize() {
        System.out.println("getTrueSize");
        
        EditWindow0 wnd = null;
        
        double expResult = 14 * User.getGlobalTextScale();
        double result = emptyDescriptor.getTrueSize(wnd);
        assertEquals(expResult, result);
    }

    /**
     * Test of getFace method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testGetFace() {
        System.out.println("getFace");
        
        int expResult = 0;
        int result = emptyDescriptor.getFace();
        assertEquals(expResult, result);
    }

    /**
     * Test of getRotation method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testGetRotation() {
        System.out.println("getRotation");
        
        TextDescriptor.Rotation expResult = TextDescriptor.Rotation.ROT0;
        TextDescriptor.Rotation result = emptyDescriptor.getRotation();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDispPart method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testGetDispPart() {
        System.out.println("getDispPart");
        
        TextDescriptor.DispPos expResult = TextDescriptor.DispPos.VALUE;
        TextDescriptor.DispPos result = emptyDescriptor.getDispPart();
        assertEquals(expResult, result);
    }

    /**
     * Test of isItalic method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testIsItalic() {
        System.out.println("isItalic");
        
        boolean expResult = false;
        boolean result = emptyDescriptor.isItalic();
        assertEquals(expResult, result);
    }

    /**
     * Test of isBold method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testIsBold() {
        System.out.println("isBold");
        
        boolean expResult = false;
        boolean result = emptyDescriptor.isBold();
        assertEquals(expResult, result);
    }

    /**
     * Test of isUnderline method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testIsUnderline() {
        System.out.println("isUnderline");
        
        boolean expResult = false;
        boolean result = emptyDescriptor.isUnderline();
        assertEquals(expResult, result);
    }

    /**
     * Test of isInterior method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testIsInterior() {
        System.out.println("isInterior");
        
        boolean expResult = false;
        boolean result = emptyDescriptor.isInterior();
        assertEquals(expResult, result);
    }

    /**
     * Test of isInherit method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testIsInherit() {
        System.out.println("isInherit");
        
        boolean expResult = false;
        boolean result = emptyDescriptor.isInherit();
        assertEquals(expResult, result);
    }

    /**
     * Test of isParam method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testIsParam() {
        System.out.println("isParam");
        
        boolean expResult = false;
        boolean result = emptyDescriptor.isParam();
        assertEquals(expResult, result);
    }

    /**
     * Test of getXOff method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testGetXOff() {
        System.out.println("getXOff");
        
        double expResult = 0.0;
        double result = emptyDescriptor.getXOff();
        assertEquals(expResult, result);
    }

    /**
     * Test of getYOff method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testGetYOff() {
        System.out.println("getYOff");
        
        double expResult = 0.0;
        double result = emptyDescriptor.getYOff();
        assertEquals(expResult, result);
    }

    /**
     * Test of getUnit method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testGetUnit() {
        System.out.println("getUnit");
        
        TextDescriptor.Unit expResult = TextDescriptor.Unit.NONE;
        TextDescriptor.Unit result = emptyDescriptor.getUnit();
        assertEquals(expResult, result);
    }

    /**
     * Test of getColorIndex method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testGetColorIndex() {
        System.out.println("getColorIndex");
        
        int expResult = 0;
        int result = emptyDescriptor.getColorIndex();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCode method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testGetCode() {
        System.out.println("getCode");
        
        TextDescriptor.Code expResult = TextDescriptor.Code.NONE;
        TextDescriptor.Code result = emptyDescriptor.getCode();
        assertEquals(expResult, result);
    }

    /**
     * Test of isJava method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testIsJava() {
        System.out.println("isJava");
        
        boolean expResult = false;
        boolean result = emptyDescriptor.isJava();
        assertEquals(expResult, result);
    }

    /**
     * Test of isCode method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testIsCode() {
        System.out.println("isCode");
        
        boolean expResult = false;
        boolean result = emptyDescriptor.isCode();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCFlags method, of class com.sun.electric.database.variable.AbstractTextDescriptor.
     */
    @Test public void testGetCFlags() {
        System.out.println("getCFlags");
        
        int expResult = TextDescriptor.VDISPLAY;
        int result = emptyDescriptor.getCFlags();
        assertEquals(expResult, result);
    }
}
