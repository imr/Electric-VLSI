/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableNodeInstTest.java
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
package com.sun.electric.database;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.MoCMOS;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class ImmutableNodeInstTest {
    
    private PrimitiveNode pn;
    private Name nameA0;
    private ImmutableNodeInst n0;
    
    @Before public void setUp() throws Exception {
        pn = MoCMOS.tech.findNodeProto("Metal-1-P-Active-Con");
        nameA0 = Name.findName("a0");
        n0 = ImmutableNodeInst.newInstance(0, pn, nameA0, null, Orientation.IDENT, EPoint.fromLambda(1, 2), EPoint.fromLambda(17, 17), 0, 0, null);
    }

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(ImmutableNodeInstTest.class);
    }

//    public static class FlagTest extends TestCase {
//
//        public FlagTest(String testName) {
//            super(testName);
//        }
//
//        protected void setUp() throws Exception {
//        }
//
//        protected void tearDown() throws Exception {
//        }
//
//        /**
//         * Test of is method, of class com.sun.electric.database.ImmutableNodeInst.Flag.
//         */
//        public void testIs() {
//            System.out.println("is");
//            
//            int userBits = 0;
//            ImmutableNodeInst.Flag instance = null;
//            
//            boolean expResult = true;
//            boolean result = instance.is(userBits);
//            assertEquals(expResult, result);
//            
//            // TODO review the generated test code and remove the default call to fail.
//            fail("The test case is a prototype.");
//        }
//
//        /**
//         * Test of set method, of class com.sun.electric.database.ImmutableNodeInst.Flag.
//         */
//        public void testSet() {
//            System.out.println("set");
//            
//            int userBits = 0;
//            boolean value = true;
//            ImmutableNodeInst.Flag instance = null;
//            
//            int expResult = 0;
//            int result = instance.set(userBits, value);
//            assertEquals(expResult, result);
//            
//            // TODO review the generated test code and remove the default call to fail.
//            fail("The test case is a prototype.");
//        }
//    }


    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    @Test public void testNewInstance() {
        System.out.println("newInstance");
        
        TextDescriptor td = TextDescriptor.newTextDescriptor(new MutableTextDescriptor()).withCode(TextDescriptor.Code.JAVA).withParam(true);
        ImmutableNodeInst n1 = ImmutableNodeInst.newInstance(0, Generic.tech.cellCenterNode, nameA0, td, Orientation.R, EPoint.fromLambda(1, 2), EPoint.fromLambda(17, 17), 0, 0, null);
        n1.check();
        assertTrue(n1.nameDescriptor.isDisplay());
        assertFalse(n1.nameDescriptor.isCode());
        assertFalse(n1.nameDescriptor.isParam());
        assertSame(n1.orient, Orientation.IDENT);
        assertSame(n1.size, EPoint.ORIGIN);
    }
    
    @Test(expected = IllegalArgumentException.class) public void testNewInstanceBadNodeId() {
        System.out.println("newInstanceBadNodeId");
        ImmutableNodeInst.newInstance(-1, Generic.tech.cellCenterNode, nameA0, null, Orientation.R, EPoint.fromLambda(1, 2), EPoint.fromLambda(17, 17), 0, 0, null);
    }

    @Test(expected = NullPointerException.class) public void testNewInstanceBadProtoId() {
        System.out.println("newInstanceBadProtoId");
        ImmutableNodeInst.newInstance(0, null, nameA0, null, Orientation.R, EPoint.fromLambda(1, 2), EPoint.fromLambda(17, 17), 0, 0, null);
    }

    @Test(expected = NullPointerException.class) public void testNewInstanceBadName1() {
        System.out.println("newInstanceBadName1");
        ImmutableNodeInst.newInstance(0, Generic.tech.cellCenterNode, null, null, Orientation.R, EPoint.fromLambda(1, 2), EPoint.fromLambda(17, 17), 0, 0, null);
    }

    @Test(expected = IllegalArgumentException.class) public void testNewInstanceBadName2() {
        System.out.println("newInstanceBadName2");
        Name name = Name.findName("a[0]_b");
        ImmutableNodeInst.newInstance(0, Generic.tech.cellCenterNode, name, null, Orientation.R, EPoint.fromLambda(1, 2), EPoint.fromLambda(17, 17), 0, 0, null);
    }

    @Test(expected = IllegalArgumentException.class) public void testNewInstanceBadName3() {
        System.out.println("newInstanceBadName3");
        Name name = Name.findName("i@0[0:1]");
        ImmutableNodeInst.newInstance(0, Generic.tech.cellCenterNode, name, null, Orientation.R, EPoint.fromLambda(1, 2), EPoint.fromLambda(17, 17), 0, 0, null);
    }

    @Test(expected = IllegalArgumentException.class) public void testNewInstanceBadName4() {
        System.out.println("newInstanceBadName4");
        Name name = Name.findName("a[0:5],b,a[5:8]");
        ImmutableNodeInst.newInstance(0, Generic.tech.cellCenterNode, name, null, Orientation.R, EPoint.fromLambda(1, 2), EPoint.fromLambda(17, 17), 0, 0, null);
    }

    /**
     * Test of withName method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testWithName() {
        System.out.println("withName");
        
        Name name = null;
        ImmutableNodeInst instance = null;
        
        ImmutableNodeInst expResult = null;
        ImmutableNodeInst result = instance.withName(name);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withNameDescriptor method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testWithNameDescriptor() {
        System.out.println("withNameDescriptor");
        
        TextDescriptor nameDescriptor = null;
        ImmutableNodeInst instance = null;
        
        ImmutableNodeInst expResult = null;
        ImmutableNodeInst result = instance.withNameDescriptor(nameDescriptor);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withOrient method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testWithOrient() {
        System.out.println("withOrient");
        
        Orientation orient = null;
        ImmutableNodeInst instance = null;
        
        ImmutableNodeInst expResult = null;
        ImmutableNodeInst result = instance.withOrient(orient);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withAnchor method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testWithAnchor() {
        System.out.println("withAnchor");
        
        EPoint anchor = null;
        ImmutableNodeInst instance = null;
        
        ImmutableNodeInst expResult = null;
        ImmutableNodeInst result = instance.withAnchor(anchor);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withSize method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testWithSize() {
        System.out.println("withSize");
        
        EPoint size = null;
        ImmutableNodeInst instance = null;
        
        ImmutableNodeInst expResult = null;
        ImmutableNodeInst result = instance.withSize(size);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withStateBits method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testWithStateBits() {
        System.out.println("withStateBits");
        
        ImmutableNodeInst d = null;
        ImmutableNodeInst instance = null;
        
        ImmutableNodeInst expResult = null;
        ImmutableNodeInst result = instance.withStateBits(d);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withFlag method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testWithFlag() {
        System.out.println("withFlag");
        
        ImmutableNodeInst.Flag flag = null;
        boolean value = true;
        ImmutableNodeInst instance = null;
        
        ImmutableNodeInst expResult = null;
        ImmutableNodeInst result = instance.withFlag(flag, value);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withTechSpecific method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testWithTechSpecific() {
        System.out.println("withTechSpecific");
        
        int techBits = 0;
        ImmutableNodeInst instance = null;
        
        ImmutableNodeInst expResult = null;
        ImmutableNodeInst result = instance.withTechSpecific(techBits);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withProtoDescriptor method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testWithProtoDescriptor() {
        System.out.println("withProtoDescriptor");
        
        TextDescriptor protoDescriptor = null;
        ImmutableNodeInst instance = null;
        
        ImmutableNodeInst expResult = null;
        ImmutableNodeInst result = instance.withProtoDescriptor(protoDescriptor);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withVariable method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testWithVariable() {
        System.out.println("withVariable");
        
        Variable var = null;
        ImmutableNodeInst instance = null;
        
        ImmutableNodeInst expResult = null;
        ImmutableNodeInst result = instance.withVariable(var);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withoutVariable method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testWithoutVariable() {
        System.out.println("withoutVariable");
        
        Variable.Key key = null;
        ImmutableNodeInst instance = null;
        
        ImmutableNodeInst expResult = null;
        ImmutableNodeInst result = instance.withoutVariable(key);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withRenamedIds method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testWithRenamedIds() {
        System.out.println("withRenamedIds");
        
        IdMapper idMapper = null;
        ImmutableNodeInst instance = null;
        
        ImmutableNodeInst expResult = null;
        ImmutableNodeInst result = instance.withRenamedIds(idMapper);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withPortInst method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testWithPortInst() {
        System.out.println("withPortInst");
        
        PortProtoId portProtoId = null;
        ImmutablePortInst portInst = null;
        ImmutableNodeInst instance = null;
        
        ImmutableNodeInst expResult = null;
        ImmutableNodeInst result = instance.withPortInst(portProtoId, portInst);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isUsernamed method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testIsUsernamed() {
        System.out.println("isUsernamed");
        
        ImmutableNodeInst instance = null;
        
        boolean expResult = true;
        boolean result = instance.isUsernamed();
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getPortInst method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testGetPortInst() {
        System.out.println("getPortInst");
        
        PortProtoId portProtoId = null;
        ImmutableNodeInst instance = null;
        
        ImmutablePortInst expResult = null;
        ImmutablePortInst result = instance.getPortInst(portProtoId);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getPortsWithVariables method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testGetPortsWithVariables() {
        System.out.println("getPortsWithVariables");
        
        ImmutableNodeInst instance = null;
        
        Iterator<PortProtoId> expResult = null;
        Iterator<PortProtoId> result = instance.getPortsWithVariables();
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of hasPortInstVariables method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testHasPortInstVariables() {
        System.out.println("hasPortInstVariables");
        
        ImmutableNodeInst instance = null;
        
        boolean expResult = true;
        boolean result = instance.hasPortInstVariables();
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of is method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testIs() {
        System.out.println("is");
        
        ImmutableNodeInst.Flag flag = null;
        ImmutableNodeInst instance = null;
        
        boolean expResult = true;
        boolean result = instance.is(flag);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of write method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testWrite() throws Exception {
        System.out.println("write");
        
        SnapshotWriter writer = null;
        ImmutableNodeInst instance = null;
        
        instance.write(writer);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of read method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testRead() throws Exception {
        System.out.println("read");
        
        SnapshotReader reader = null;
        
        ImmutableNodeInst expResult = null;
        ImmutableNodeInst result = ImmutableNodeInst.read(reader);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of hashCodeExceptVariables method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testHashCodeExceptVariables() {
        System.out.println("hashCodeExceptVariables");
        
        ImmutableNodeInst instance = null;
        
        int expResult = 0;
        int result = instance.hashCodeExceptVariables();
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of equalsExceptVariables method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testEqualsExceptVariables() {
        System.out.println("equalsExceptVariables");
        
        ImmutableElectricObject o = null;
        ImmutableNodeInst instance = null;
        
        boolean expResult = true;
        boolean result = instance.equalsExceptVariables(o);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of check method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testCheck() {
        System.out.println("check");
        
        ImmutableNodeInst instance = null;
        
        instance.check();
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getElibBits method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testGetElibBits() {
        System.out.println("getElibBits");
        
        ImmutableNodeInst instance = null;
        
        int expResult = 0;
        int result = instance.getElibBits();
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of flagsFromElib method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testFlagsFromElib() {
        System.out.println("flagsFromElib");
        
        int elibBits = 0;
        
        int expResult = 0;
        int result = ImmutableNodeInst.flagsFromElib(elibBits);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of techSpecificFromElib method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testTechSpecificFromElib() {
        System.out.println("techSpecificFromElib");
        
        int elibBits = 0;
        
        int expResult = 0;
        int result = ImmutableNodeInst.techSpecificFromElib(elibBits);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of computeBounds method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testComputeBounds() {
        System.out.println("computeBounds");
        
        NodeInst real = null;
        Rectangle2D.Double dstBounds = null;
        ImmutableNodeInst instance = null;
        
        instance.computeBounds(real, dstBounds);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getTrace method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testGetTrace() {
        System.out.println("getTrace");
        
        ImmutableNodeInst instance = null;
        
        EPoint[] expResult = null;
        EPoint[] result = instance.getTrace();
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSerpentineTransistorLength method, of class com.sun.electric.database.ImmutableNodeInst.
     */
    public void testGetSerpentineTransistorLength() {
        System.out.println("getSerpentineTransistorLength");
        
        ImmutableNodeInst instance = null;
        
        double expResult = 0.0;
        double result = instance.getSerpentineTransistorLength();
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
