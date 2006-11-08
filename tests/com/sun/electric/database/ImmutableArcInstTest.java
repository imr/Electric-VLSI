/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableArcInstTest.java
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

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.AbstractShapeBuilder;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.technologies.MoCMOS;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class ImmutableArcInstTest {
    
    private PrimitiveNode pn;
    private PrimitivePort pp;
    private ArcProto ap;
    private IdManager idManager;
    private LibId libId;
    private CellId cellId;
    private ImmutableNodeInst n0, n1;
    private ImmutableArcInst a0;
    
    @Before public void setUp() throws Exception {
        pn = MoCMOS.tech.findNodeProto("Metal-1-P-Active-Con");
        pp = pn.getPort(0);
        ap = MoCMOS.tech.findArcProto("P-Active");
        idManager = new IdManager();
        libId = idManager.newLibId("lib");
        cellId = libId.newCellId(CellName.parseName("cell;1{lay}"));
        n0 = ImmutableNodeInst.newInstance(0, pn, Name.findName("n0"), null, Orientation.IDENT, EPoint.fromLambda(1, 2), EPoint.fromLambda(17, 17), 0, 0, null);
        n1 = ImmutableNodeInst.newInstance(1, pn, Name.findName("n1"), null, Orientation.IDENT, EPoint.fromLambda(21, 2), EPoint.fromLambda(17, 17), 0, 0, null);
        a0 = ImmutableArcInst.newInstance(0, ap, Name.findName("a0"), null, 0, pp, n0.anchor, 1, pp, n1.anchor, DBMath.lambdaToGrid(15), 0, ImmutableArcInst.DEFAULT_FLAGS);
    }

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(ImmutableArcInstTest.class);
    }


//    public static class FlagTest {
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
//         * Test of is method, of class com.sun.electric.database.ImmutableArcInst.Flag.
//         */
//        public void testIs() {
//            System.out.println("is");
//            
//            int userBits = 0;
//            ImmutableArcInst.Flag instance = null;
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
//         * Test of set method, of class com.sun.electric.database.ImmutableArcInst.Flag.
//         */
//        public void testSet() {
//            System.out.println("set");
//            
//            int userBits = 0;
//            boolean value = true;
//            ImmutableArcInst.Flag instance = null;
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
     * Test of getLambdaFullWidth method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testGetLambdaFullWidth() {
        System.out.println("getLambdaFullWidth");
        assertEquals(15.0, a0.getLambdaFullWidth());
    }

    /**
     * Test of getGridFullWidth method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testGetGridFullWidth() {
        System.out.println("getGridFullWidth");
        assertEquals(15*(long)DBMath.GRID, a0.getGridFullWidth());
    }

    /**
     * Test of getLambdaBaseWidth method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testGetLambdaBaseWidth() {
        System.out.println("getLambdaBaseWidth");
        assertEquals(3.0, a0.getLambdaBaseWidth());
    }

    /**
     * Test of getGridBaseWidth method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testGetGridBaseWidth() {
        System.out.println("getGridBaseWidth");
        assertEquals(3*(long)DBMath.GRID, a0.getGridBaseWidth());
    }

    /**
     * Test of getLambdaLength method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testGetLambdaLength() {
        System.out.println("getLambdaLength");
        assertEquals(20.0, a0.getLambdaLength());
    }

    /**
     * Test of getGridLength method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testGetGridLength() {
        System.out.println("getGridLength");
        assertEquals(20*DBMath.GRID, a0.getGridLength());
    }

    /**
     * Test of getAngle method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testGetAngle() {
        System.out.println("getAngle");
        assertEquals(0, a0.getAngle());
    }

    /**
     * Test of is method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testIs() {
        System.out.println("is");
        assertFalse(a0.is(ImmutableArcInst.RIGID));
        assertFalse(a0.is(ImmutableArcInst.FIXED_ANGLE));
        assertTrue(a0.is(ImmutableArcInst.SLIDABLE));
        assertFalse(a0.is(ImmutableArcInst.HARD_SELECT));
        assertFalse(a0.is(ImmutableArcInst.BODY_ARROWED));
        assertFalse(a0.is(ImmutableArcInst.TAIL_ARROWED));
        assertFalse(a0.is(ImmutableArcInst.HEAD_ARROWED));
        assertFalse(a0.is(ImmutableArcInst.TAIL_NEGATED));
        assertFalse(a0.is(ImmutableArcInst.HEAD_NEGATED));
        assertTrue(a0.is(ImmutableArcInst.TAIL_EXTENDED));
        assertTrue(a0.is(ImmutableArcInst.HEAD_EXTENDED));
    }

    /**
     * Test of isRigid method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testIsRigid() {
        System.out.println("isRigid");
        assertFalse(a0.isRigid());
    }

    /**
     * Test of isFixedAngle method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testIsFixedAngle() {
        System.out.println("isFixedAngle");
        assertFalse(a0.isFixedAngle());
    }

    /**
     * Test of isSlidable method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testIsSlidable() {
        System.out.println("isSlidable");
        assertTrue(a0.isSlidable());
    }

    /**
     * Test of isHardSelect method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testIsHardSelect() {
        System.out.println("isHardSelect");
        assertFalse(a0.isHardSelect());
    }

    /**
     * Test of isArrowed method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testIsArrowed() {
        System.out.println("isArrowed");
        assertFalse(a0.isArrowed(0));
        assertFalse(a0.isArrowed(1));
    }

    /**
     * Test of isArrowed method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = IllegalArgumentException.class) public void testIsArrowedFail() {
        System.out.println("isArrowedFail");
        a0.isArrowed(2);
    }
    
    /**
     * Test of isTailArrowed method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testIsTailArrowed() {
        System.out.println("isTailArrowed");
        assertFalse(a0.isTailArrowed());
    }

    /**
     * Test of isHeadArrowed method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testIsHeadArrowed() {
        System.out.println("isHeadArrowed");
        assertFalse(a0.isHeadArrowed());
    }

    /**
     * Test of isBodyArrowed method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testIsBodyArrowed() {
        System.out.println("isBodyArrowed");
        assertFalse(a0.isBodyArrowed());
    }

    /**
     * Test of isExtended method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testIsExtended() {
        System.out.println("isExtended");
        assertTrue(a0.isExtended(0));
        assertTrue(a0.isExtended(1));
    }

    /**
     * Test of isExtended method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = IllegalArgumentException.class) public void testIsExtendedFail() {
        System.out.println("isExtendedFail");
        a0.isExtended(-1);
    }
    
    /**
     * Test of isTailExtended method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testIsTailExtended() {
        System.out.println("isTailExtended");
        assertTrue(a0.isTailExtended());
    }

    /**
     * Test of isHeadExtended method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testIsHeadExtended() {
        System.out.println("isHeadExtended");
        assertTrue(a0.isHeadExtended());
    }

    /**
     * Test of isNegated method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testIsNegated() {
        System.out.println("isNegated");
        assertFalse(a0.isNegated(0));
        assertFalse(a0.isNegated(1));
    }

    /**
     * Test of isNegated method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = IllegalArgumentException.class) public void testIsNegatedFail() {
        System.out.println("isNegatedFail");
        a0.isExtended(-2);
    }
    
    /**
     * Test of isTailNegated method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testIsTailNegated() {
        System.out.println("isTailNegated");
        assertFalse(a0.isTailNegated());
    }

    /**
     * Test of isHeadNegated method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testIsHeadNegated() {
        System.out.println("isHeadNegated");
        assertFalse(a0.isHeadNegated());
    }

    /**
     * Test of isEasyShape method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testIsEasyShape() {
        System.out.println("isEasyShape");
        assertTrue(a0.isEasyShape());
    }

    /**
     * Test of explainEasyShape method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testExplainEasyShape() {
        System.out.println("explainEasyShape");
        
        ImmutableArcInst instance = null;
        
        instance.explainEasyShape();
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testNewInstance() {
        System.out.println("newInstance");
        
        int arcId = 0;
        ArcProto protoType = null;
        Name name = null;
        TextDescriptor nameDescriptor = null;
        int tailNodeId = 0;
        PortProtoId tailPortId = null;
        EPoint tailLocation = null;
        int headNodeId = 0;
        PortProtoId headPortId = null;
        EPoint headLocation = null;
        long gridFullWidth = 0L;
        int angle = 0;
        int flags = 0;
        
        ImmutableArcInst expResult = null;
        ImmutableArcInst result = ImmutableArcInst.newInstance(arcId, protoType, name, nameDescriptor, tailNodeId, tailPortId, tailLocation, headNodeId, headPortId, headLocation, gridFullWidth, angle, flags);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withName method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testWithName() {
        System.out.println("withName");
        
        Name name = null;
        ImmutableArcInst instance = null;
        
        ImmutableArcInst expResult = null;
        ImmutableArcInst result = instance.withName(name);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withNameDescriptor method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testWithNameDescriptor() {
        System.out.println("withNameDescriptor");
        
        TextDescriptor nameDescriptor = null;
        ImmutableArcInst instance = null;
        
        ImmutableArcInst expResult = null;
        ImmutableArcInst result = instance.withNameDescriptor(nameDescriptor);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withLocations method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testWithLocations() {
        System.out.println("withLocations");
        
        EPoint tailLocation = null;
        EPoint headLocation = null;
        ImmutableArcInst instance = null;
        
        ImmutableArcInst expResult = null;
        ImmutableArcInst result = instance.withLocations(tailLocation, headLocation);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withGridFullWidth method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testWithGridFullWidth() {
        System.out.println("withGridFullWidth");
        
        long gridFullWidth = 0L;
        ImmutableArcInst instance = null;
        
        ImmutableArcInst expResult = null;
        ImmutableArcInst result = instance.withGridFullWidth(gridFullWidth);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withAngle method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testWithAngle() {
        System.out.println("withAngle");
        
        int angle = 0;
        ImmutableArcInst instance = null;
        
        ImmutableArcInst expResult = null;
        ImmutableArcInst result = instance.withAngle(angle);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withFlags method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testWithFlags() {
        System.out.println("withFlags");
        
        int flags = 0;
        ImmutableArcInst instance = null;
        
        ImmutableArcInst expResult = null;
        ImmutableArcInst result = instance.withFlags(flags);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withFlag method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testWithFlag() {
        System.out.println("withFlag");
        
        ImmutableArcInst.Flag flag = null;
        boolean value = true;
        ImmutableArcInst instance = null;
        
        ImmutableArcInst expResult = null;
        ImmutableArcInst result = instance.withFlag(flag, value);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withVariable method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testWithVariable() {
        System.out.println("withVariable");
        
        Variable var = null;
        ImmutableArcInst instance = null;
        
        ImmutableArcInst expResult = null;
        ImmutableArcInst result = instance.withVariable(var);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withoutVariable method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testWithoutVariable() {
        System.out.println("withoutVariable");
        
        Variable.Key key = null;
        ImmutableArcInst instance = null;
        
        ImmutableArcInst expResult = null;
        ImmutableArcInst result = instance.withoutVariable(key);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withRenamedIds method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testWithRenamedIds() {
        System.out.println("withRenamedIds");
        
        IdMapper idMapper = null;
        ImmutableArcInst instance = null;
        
        ImmutableArcInst expResult = null;
        ImmutableArcInst result = instance.withRenamedIds(idMapper);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of write method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testWrite() throws Exception {
        System.out.println("write");
        
        SnapshotWriter writer = null;
        ImmutableArcInst instance = null;
        
        instance.write(writer);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of read method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testRead() throws Exception {
        System.out.println("read");
        
        SnapshotReader reader = null;
        
        ImmutableArcInst expResult = null;
        ImmutableArcInst result = ImmutableArcInst.read(reader);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of hashCodeExceptVariables method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testHashCodeExceptVariables() {
        System.out.println("hashCodeExceptVariables");
        
        ImmutableArcInst instance = null;
        
        int expResult = 0;
        int result = instance.hashCodeExceptVariables();
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of equalsExceptVariables method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testEqualsExceptVariables() {
        System.out.println("equalsExceptVariables");
        
        ImmutableElectricObject o = null;
        ImmutableArcInst instance = null;
        
        boolean expResult = true;
        boolean result = instance.equalsExceptVariables(o);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of makeGridPoly method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testMakeGridPoly() {
        System.out.println("makeGridPoly");
        
        CellBackup.Memoization m = null;
        long gridWidth = 0L;
        Poly.Type style = null;
        ImmutableArcInst instance = null;
        
        Poly expResult = null;
        Poly result = instance.makeGridPoly(m, gridWidth, style);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of makeGridBoxInt method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testMakeGridBoxInt() {
        System.out.println("makeGridBoxInt");
        
        AbstractShapeBuilder b = null;
        long gridWidth = 0L;
        ImmutableArcInst instance = null;
        
        instance.makeGridBoxInt(b, gridWidth);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of registerShrinkage method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testRegisterShrinkage() {
        System.out.println("registerShrinkage");
        
        int[] shrinkageState = null;
        ImmutableArcInst instance = null;
        
        instance.registerShrinkage(shrinkageState);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of computeShrink method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testComputeShrink() {
        System.out.println("computeShrink");
        
        int angs = 0;
        
        short expResult = 0;
        short result = ImmutableArcInst.computeShrink(angs);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getRadius method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testGetRadius() {
        System.out.println("getRadius");
        
        ImmutableArcInst instance = null;
        
        Double expResult = null;
        Double result = instance.getRadius();
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of curvedArcGridOutline method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testCurvedArcGridOutline() {
        System.out.println("curvedArcGridOutline");
        
        Poly.Type style = null;
        long gridWidth = 0L;
        long gridRadius = 0L;
        ImmutableArcInst instance = null;
        
        Poly expResult = null;
        Poly result = instance.curvedArcGridOutline(style, gridWidth, gridRadius);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of check method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testCheck() {
        System.out.println("check");
        
        ImmutableArcInst instance = null;
        
        instance.check();
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getElibBits method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testGetElibBits() {
        System.out.println("getElibBits");
        
        ImmutableArcInst instance = null;
        
        int expResult = 0;
        int result = instance.getElibBits();
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of flagsFromElib method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testFlagsFromElib() {
        System.out.println("flagsFromElib");
        
        int elibBits = 0;
        
        int expResult = 0;
        int result = ImmutableArcInst.flagsFromElib(elibBits);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of angleFromElib method, of class com.sun.electric.database.ImmutableArcInst.
     */
    public void testAngleFromElib() {
        System.out.println("angleFromElib");
        
        int elibBits = 0;
        
        int expResult = 0;
        int result = ImmutableArcInst.angleFromElib(elibBits);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
