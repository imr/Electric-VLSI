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
import com.sun.electric.database.variable.MutableTextDescriptor;
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
    private Name nameA0;
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
        nameA0 = Name.findName("a0");
        a0 = ImmutableArcInst.newInstance(0, ap, nameA0, null, 0, pp, n0.anchor, 1, pp, n1.anchor, DBMath.lambdaToGrid(15), 0, ImmutableArcInst.DEFAULT_FLAGS);
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
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testNewInstance() {
        System.out.println("newInstance");
        
        TextDescriptor td = TextDescriptor.newTextDescriptor(new MutableTextDescriptor()).withCode(TextDescriptor.Code.JAVA).withParam(true);
        ImmutableArcInst a1 = ImmutableArcInst.newInstance(0, ap, nameA0, td, 0, pp, n0.anchor, 1, pp, n1.anchor, DBMath.lambdaToGrid(15), 0, ImmutableArcInst.DEFAULT_FLAGS);
        a1.check();
        assertTrue(a1.nameDescriptor.isDisplay());
        assertFalse(a1.nameDescriptor.isCode());
        assertFalse(a1.nameDescriptor.isParam());
        
        ImmutableArcInst a2 = ImmutableArcInst.newInstance(0, ap, nameA0, null, 0, pp, n0.anchor, 0, pp, n0.anchor, DBMath.lambdaToGrid(15), -1, ImmutableArcInst.DEFAULT_FLAGS);
        a2.check();
        assertEquals(3599, a2.getAngle());
        
        ImmutableArcInst a3 = ImmutableArcInst.newInstance(0, ap, nameA0, null, 1, pp, n1.anchor, 0, pp, n0.anchor, DBMath.lambdaToGrid(15), 0, ImmutableArcInst.DEFAULT_FLAGS);
        a3.check();
        assertEquals(1800, a3.getAngle());
        
        int flags = ImmutableArcInst.TAIL_NEGATED.set(ImmutableArcInst.DEFAULT_FLAGS, true);
        ImmutableArcInst a4 = ImmutableArcInst.newInstance(0, ap, nameA0, null, 0, pp, n0.anchor, 1, pp, n1.anchor, DBMath.lambdaToGrid(15), 0, flags);
        a4.check();
        assertFalse(a4.isTailNegated());
        assertFalse(a4.is(ImmutableArcInst.TAIL_NEGATED));
    }

    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = IllegalArgumentException.class) public void testNewInstanceBadArcId() {
        System.out.println("newInstanceBadArcId");
        ImmutableArcInst.newInstance(-1, ap, nameA0, null, 0, pp, n0.anchor, 1, pp, n1.anchor, DBMath.lambdaToGrid(15), 0, ImmutableArcInst.DEFAULT_FLAGS);
    }
    
    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = NullPointerException.class) public void testNewInstanceBadProto() {
        System.out.println("newInstanceBadProto");
        ImmutableArcInst.newInstance(0, null, nameA0, null, 0, pp, n0.anchor, 1, pp, n1.anchor, DBMath.lambdaToGrid(15), 0, ImmutableArcInst.DEFAULT_FLAGS);
    }
    
    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = NullPointerException.class) public void testNewInstanceBadName1() {
        System.out.println("newInstanceBadName1");
        ImmutableArcInst.newInstance(0, ap, null, null, 0, pp, n0.anchor, 1, pp, n1.anchor, DBMath.lambdaToGrid(15), 0, ImmutableArcInst.DEFAULT_FLAGS);
    }
    
    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = IllegalArgumentException.class) public void testNewInstanceBadName2() {
        System.out.println("newInstanceBadName2");
        ImmutableArcInst.newInstance(0, ap, Name.findName("a:"), null, 0, pp, n0.anchor, 1, pp, n1.anchor, DBMath.lambdaToGrid(15), 0, ImmutableArcInst.DEFAULT_FLAGS);
    }
    
    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = IllegalArgumentException.class) public void testNewInstanceBadName3() {
        System.out.println("newInstanceBadName3");
        ImmutableArcInst.newInstance(0, ap, Name.findName("a,"), null, 0, pp, n0.anchor, 1, pp, n1.anchor, DBMath.lambdaToGrid(15), 0, ImmutableArcInst.DEFAULT_FLAGS);
    }
    
    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = IllegalArgumentException.class) public void testNewInstanceBadName4() {
        System.out.println("newInstanceBadName4");
        ImmutableArcInst.newInstance(0, ap, Name.findName("Net@0"), null, 0, pp, n0.anchor, 1, pp, n1.anchor, DBMath.lambdaToGrid(15), 0, ImmutableArcInst.DEFAULT_FLAGS);
    }
    
    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = IllegalArgumentException.class) public void testNewInstanceBadName5() {
        System.out.println("newInstanceBadName5");
        ImmutableArcInst.newInstance(0, ap, Name.findName("net@0[0:1]"), null, 0, pp, n0.anchor, 1, pp, n1.anchor, DBMath.lambdaToGrid(15), 0, ImmutableArcInst.DEFAULT_FLAGS);
    }
    
    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = IllegalArgumentException.class) public void testNewInstanceBadTailNodeId() {
        System.out.println("newInstanceBadTailNodeId");
        ImmutableArcInst.newInstance(0, ap, nameA0, null, -1, pp, n0.anchor, 1, pp, n1.anchor, DBMath.lambdaToGrid(15), 0, ImmutableArcInst.DEFAULT_FLAGS);
    }
    
    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = NullPointerException.class) public void testNewInstanceBadTailPortId() {
        System.out.println("newInstanceBadTailPortId");
        ImmutableArcInst.newInstance(0, ap, nameA0, null, 0, null, n0.anchor, 1, pp, n1.anchor, DBMath.lambdaToGrid(15), 0, ImmutableArcInst.DEFAULT_FLAGS);
    }
    
    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = NullPointerException.class) public void testNewInstanceBadTailLocation() {
        System.out.println("newInstanceBadTailLocation");
        ImmutableArcInst.newInstance(0, ap, nameA0, null, 0, pp, null, 1, pp, n1.anchor, DBMath.lambdaToGrid(15), 0, ImmutableArcInst.DEFAULT_FLAGS);
    }
    
    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = IllegalArgumentException.class) public void testNewInstanceBadHeadNodeId() {
        System.out.println("newInstanceBadHeadNodeId");
        ImmutableArcInst.newInstance(0, ap, nameA0, null, 0, pp, n0.anchor, -1, pp, n1.anchor, DBMath.lambdaToGrid(15), 0, ImmutableArcInst.DEFAULT_FLAGS);
    }
    
    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = NullPointerException.class) public void testNewInstanceBadHeadPortId() {
        System.out.println("newInstanceBadHeadPortId");
        ImmutableArcInst.newInstance(0, ap, nameA0, null, 0, pp, n0.anchor, 1, null, n1.anchor, DBMath.lambdaToGrid(15), 0, ImmutableArcInst.DEFAULT_FLAGS);
    }
    
    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = NullPointerException.class) public void testNewInstanceBadHeadLocation() {
        System.out.println("newInstanceBadHeadLocation");
        ImmutableArcInst.newInstance(0, ap, nameA0, null, 0, pp, n0.anchor, 1, pp, null, DBMath.lambdaToGrid(15), 0, ImmutableArcInst.DEFAULT_FLAGS);
    }
    
    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = IllegalArgumentException.class) public void testNewInstanceBadWidth1() {
        System.out.println("newInstanceBadWidth1");
        ImmutableArcInst.newInstance(0, ap, nameA0, null, 0, pp, n0.anchor, 1, pp, n1.anchor, DBMath.lambdaToGrid(15) + 1, 0, ImmutableArcInst.DEFAULT_FLAGS);
    }
    
    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = IllegalArgumentException.class) public void testNewInstanceBadWidth2() {
        System.out.println("newInstanceBadWidth2");
        ImmutableArcInst.newInstance(0, ap, nameA0, null, 0, pp, n0.anchor, 1, pp, n1.anchor, DBMath.lambdaToGrid(12) - 2, 0, ImmutableArcInst.DEFAULT_FLAGS);
    }
    
    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = IllegalArgumentException.class) public void testNewInstanceBadWidth3() {
        System.out.println("newInstanceBadWidth3");
        ImmutableArcInst.newInstance(0, ap, nameA0, null, 0, pp, n0.anchor, 1, pp, n1.anchor, DBMath.lambdaToGrid(6000000) - 2, 0, ImmutableArcInst.DEFAULT_FLAGS);
    }
    
    /**
     * Test of withName method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testWithName() {
        System.out.println("withName");
        
        assertSame(a0, a0.withName(a0.name));
        a0.check();
        
        Name nameB = Name.findName("b");
        ImmutableArcInst ab = a0.withName(nameB);
        assertSame(nameA0, a0.name);
        assertSame(nameB, ab.name);
        ab.check();
    }

    /**
     * Test of withName method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = NullPointerException.class) public void testWithNameBad1() {
        System.out.println("withNameBad1");
        a0.withName(null);
    }
    
     /**
     * Test of withName method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = IllegalArgumentException.class) public void testWithNameBad2() {
        System.out.println("withNameBad2");
        a0.withName(Name.findName("a:"));
    }
    
    /**
     * Test of withName method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = IllegalArgumentException.class) public void testWithNameBad3() {
        System.out.println("withNameBad3");
        a0.withName(Name.findName("a,"));
    }
    
    /**
     * Test of withName method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test(expected = IllegalArgumentException.class) public void testWithNameBad4() {
        System.out.println("withNameBad4");
        a0.withName(Name.findName("Net@0"));
    }
    
    /**
     * Test of withName method, of class com.sun.electric.database.ImmutableArcInst.
     */
   @Test(expected = IllegalArgumentException.class) public void testWithNameBad5() {
        System.out.println("withNameBad5");
        a0.withName(Name.findName("net@0[0:1]"));
    }
    
     /**
     * Test of withNameDescriptor method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testWithNameDescriptor() {
        System.out.println("withNameDescriptor");
        assertSame(a0, a0.withNameDescriptor(a0.nameDescriptor));
        
        TextDescriptor td = TextDescriptor.newTextDescriptor(new MutableTextDescriptor()).withCode(TextDescriptor.Code.JAVA).withParam(true).withUnderline(true);
        ImmutableArcInst a1 = a0.withNameDescriptor(td);
        a1.check();
        assertTrue(a1.nameDescriptor.isDisplay());
        assertFalse(a1.nameDescriptor.isCode());
        assertFalse(a1.nameDescriptor.isParam());
        assertTrue(a1.nameDescriptor.isUnderline());
    }
        
    /**
     * Test of withLocations method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testWithLocations() {
        System.out.println("withLocations");
        assertSame(a0, a0.withLocations(a0.tailLocation, a0.headLocation));
    }

    /**
     * Test of withGridFullWidth method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testWithGridFullWidth() {
        System.out.println("withGridFullWidth");
        assertSame(a0, a0.withGridFullWidth(a0.getGridFullWidth()));
    }

    /**
     * Test of withAngle method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testWithAngle() {
        System.out.println("withAngle");
        assertSame(a0, a0.withAngle(900)); // If locations are different, angle is calcualted from them
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
    @Test public void testWithFlag() {
        System.out.println("withFlag");
        assertSame(a0, a0.withFlag(ImmutableArcInst.TAIL_NEGATED, true)); // layout arc can't have negated end
        assertSame(a0, a0.withFlag(ImmutableArcInst.HEAD_NEGATED, true)); // layout arc can't have negated end
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
    @Test public void testGetRadius() {
        System.out.println("getRadius");
        assertNull(a0.getRadius());
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
    @Test public void testGetElibBits() {
        System.out.println("getElibBits");
        // DEFAULT
        toElib(000000000000, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED, ImmutableArcInst.SLIDABLE);

        // single bits
        toElib(000040400000);
        toElib(000040400001, ImmutableArcInst.RIGID);
        toElib(000040400002, ImmutableArcInst.FIXED_ANGLE);
        toElib(000000400000, ImmutableArcInst.SLIDABLE);
        toElib(020040400000, ImmutableArcInst.HARD_SELECT);
        
        // NOEXTEND
        toElib(000040000000, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        toElib(000040400000);
        toElib(000044400000, ImmutableArcInst.TAIL_EXTENDED);
        toElib(000050400000, ImmutableArcInst.HEAD_EXTENDED);
        
        // ISDIRECTIONAL
        toElib(000052000000, ImmutableArcInst.BODY_ARROWED,                                                                ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        toElib(000042000000, ImmutableArcInst.HEAD_ARROWED,                                                                ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        toElib(000042000000, ImmutableArcInst.BODY_ARROWED, ImmutableArcInst.HEAD_ARROWED,                                 ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        toElib(000062000000, ImmutableArcInst.TAIL_ARROWED,                                                                ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        toElib(000062000000, ImmutableArcInst.BODY_ARROWED, ImmutableArcInst.TAIL_ARROWED,                                 ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        toElib(000062000000, ImmutableArcInst.BODY_ARROWED, ImmutableArcInst.TAIL_ARROWED, ImmutableArcInst.HEAD_ARROWED,  ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        toElib(000062000000, ImmutableArcInst.TAIL_ARROWED, ImmutableArcInst.HEAD_ARROWED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
    }
    
    private void toElib(int elibFlags, ImmutableArcInst.Flag... flags) {
        int flagBits = 0;
        for (ImmutableArcInst.Flag f: flags)
            flagBits = f.set(flagBits, true);
        assertEquals(elibFlags, a0.withFlags(flagBits).getElibBits());
    }

    /**
     * Test of flagsFromElib method, of class com.sun.electric.database.ImmutableArcInst.
     */
    @Test public void testFlagsFromElib() {
        System.out.println("flagsFromElib");
        // default
        fromElib(000000000000, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED, ImmutableArcInst.SLIDABLE);

        // single bits
        fromElib(000040400001, ImmutableArcInst.RIGID);
        fromElib(000040400002, ImmutableArcInst.FIXED_ANGLE);
        fromElib(000000400000, ImmutableArcInst.SLIDABLE);
        fromElib(020040400000, ImmutableArcInst.HARD_SELECT);
        
        // REVERSEEND, NOTEND0, NOTEND1
        fromElib(000040000000, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000044000000, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000050000000, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000054000000, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000060000000, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000064000000, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000070000000, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000074000000, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        
        // NOEXTEND
        fromElib(000040400000);
        fromElib(000044400000, ImmutableArcInst.TAIL_EXTENDED);
        fromElib(000050400000, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000054400000, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000060400000);
        fromElib(000064400000, ImmutableArcInst.TAIL_EXTENDED);
        fromElib(000070400000, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000074400000, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        
        // ISHEADNEGATED
        fromElib(000040200000, ImmutableArcInst.HEAD_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000044200000, ImmutableArcInst.HEAD_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000050200000, ImmutableArcInst.HEAD_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000054200000, ImmutableArcInst.HEAD_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000060200000, ImmutableArcInst.TAIL_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000064200000, ImmutableArcInst.TAIL_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000070200000, ImmutableArcInst.TAIL_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000074200000, ImmutableArcInst.TAIL_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        
        // ISTAILNEGATED
        fromElib(000041000000, ImmutableArcInst.TAIL_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000045000000, ImmutableArcInst.TAIL_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000051000000, ImmutableArcInst.TAIL_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000055000000, ImmutableArcInst.TAIL_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000061000000, ImmutableArcInst.HEAD_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000065000000, ImmutableArcInst.HEAD_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000071000000, ImmutableArcInst.HEAD_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000075000000, ImmutableArcInst.HEAD_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        
        // ISHEADNEGATED | ISTAILNEGATED
        fromElib(000041200000, ImmutableArcInst.TAIL_NEGATED, ImmutableArcInst.HEAD_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000045200000, ImmutableArcInst.TAIL_NEGATED, ImmutableArcInst.HEAD_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000051200000, ImmutableArcInst.TAIL_NEGATED, ImmutableArcInst.HEAD_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000055200000, ImmutableArcInst.TAIL_NEGATED, ImmutableArcInst.HEAD_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000061200000, ImmutableArcInst.TAIL_NEGATED, ImmutableArcInst.HEAD_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000065200000, ImmutableArcInst.TAIL_NEGATED, ImmutableArcInst.HEAD_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000071200000, ImmutableArcInst.TAIL_NEGATED, ImmutableArcInst.HEAD_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000075200000, ImmutableArcInst.TAIL_NEGATED, ImmutableArcInst.HEAD_NEGATED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        
        // ISDIRECTIONAL
        fromElib(000042000000, ImmutableArcInst.BODY_ARROWED, ImmutableArcInst.HEAD_ARROWED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000046000000, ImmutableArcInst.BODY_ARROWED, ImmutableArcInst.HEAD_ARROWED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000052000000, ImmutableArcInst.BODY_ARROWED,                                ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000056000000, ImmutableArcInst.BODY_ARROWED,                                ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000062000000, ImmutableArcInst.BODY_ARROWED, ImmutableArcInst.TAIL_ARROWED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000066000000, ImmutableArcInst.BODY_ARROWED,                                ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000072000000, ImmutableArcInst.BODY_ARROWED, ImmutableArcInst.TAIL_ARROWED, ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
        fromElib(000076000000, ImmutableArcInst.BODY_ARROWED,                                ImmutableArcInst.TAIL_EXTENDED, ImmutableArcInst.HEAD_EXTENDED);
    }
    
    private void fromElib(int elibFlags, ImmutableArcInst.Flag... flags) {
        int flagBits = 0;
        for (ImmutableArcInst.Flag f: flags)
            flagBits = f.set(flagBits, true);
        assertEquals(flagBits, ImmutableArcInst.flagsFromElib(elibFlags));
    }

    /**
     * Test of angleFromElib method, of class com.sun.electric.database.ImmutableArcInst.
     */
   @Test public void testAngleFromElib() {
        System.out.println("angleFromElib");
        assertEquals(0, ImmutableArcInst.angleFromElib(0));
        assertEquals(0, ImmutableArcInst.angleFromElib((1 << 5) -  1));
        assertEquals(0, ImmutableArcInst.angleFromElib(1 << 14));
        assertEquals(10, ImmutableArcInst.angleFromElib(1 << 5));
        assertEquals(3590, ImmutableArcInst.angleFromElib(359 << 5));
        assertEquals(0, ImmutableArcInst.angleFromElib(360 << 5));
    }
    
}
