/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IdManagerTest.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.id;

import com.sun.electric.database.hierarchy.EDatabase;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests of TechId
 */
public class TechIdTest {

    IdManager idManager;
    String techName = "tech";
    TechId techId0;
    TechId techId2;
    ArcProtoId aId0_a;
    ArcProtoId aId2_a;
    ArcProtoId aId0_A;
    PrimitiveNodeId nId2_a;
    PrimitiveNodeId nId0_A;
    PrimitivePortId p0_A_p;

    public TechIdTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        idManager = new IdManager();
        techId0 = idManager.newTechId(techName);
        techId2 = idManager.newTechId("2");
        aId0_a = techId0.newArcProtoId("a");
        aId2_a = techId2.newArcProtoId("a");
        aId0_A = techId0.newArcProtoId("A");
        nId2_a = techId2.newPrimitiveNodeId("a");
        nId0_A = techId0.newPrimitiveNodeId("A");
        p0_A_p = nId0_A.newPortId("p");
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of numArcProtoIds method, of class TechId.
     */
    @Test
    public void basic() {
        System.out.println("hasic");

        assertSame(idManager, techId0.idManager);
        assertSame(0, techId0.techIndex);
        assertSame(techName, techId0.techName);
        assertEquals(4, techId0.modCount);
        assertEquals(2, techId0.numArcProtoIds());
        assertSame(aId0_a, techId0.getArcProtoId(0));
        assertEquals("a", aId0_a.name);
        assertEquals("tech:a", aId0_a.fullName);
        assertSame(aId0_A, techId0.getArcProtoId(1));
        assertEquals("A", aId0_A.name);
        assertEquals("tech:A", aId0_A.fullName);
        assertEquals(1, techId0.numPrimitiveNodeIds());
        assertSame(nId0_A, techId0.getPrimitiveNodeId(0));
        assertEquals("A", nId0_A.name);
        assertEquals("tech:A", nId0_A.fullName);
        assertEquals(1, nId0_A.numPrimitivePortIds());
        assertSame(p0_A_p, nId0_A.getPortId(0));
        assertEquals("p", p0_A_p.externalId);
        assertEquals("tech:A:p", p0_A_p.toString());

        assertSame(idManager, techId2.idManager);
        assertSame(1, techId2.techIndex);
        assertSame("2", techId2.techName);
        assertEquals(2, techId2.modCount);
        assertEquals(1, techId2.numArcProtoIds());
        assertSame(aId2_a, techId2.getArcProtoId(0));
        assertEquals("a", aId2_a.name);
        assertEquals("2:a", aId2_a.fullName);
        assertEquals(1, techId2.numPrimitiveNodeIds());
        assertSame(nId2_a, techId2.getPrimitiveNodeId(0));
        assertEquals("a", nId2_a.name);
        assertEquals("2:a", nId2_a.fullName);
        assertEquals(0, nId2_a.numPrimitivePortIds());

        ArcProtoId aId = techId0.newArcProtoId("A");
        assertSame(aId0_A, aId);
        assertSame(2, techId0.numArcProtoIds());
        assertSame(1, techId0.numPrimitiveNodeIds());
        assertSame(4, techId0.modCount);

        aId = techId0.newArcProtoId("B");
        assertEquals("B", aId.name);
        assertEquals("tech:B", aId.fullName);
        assertSame(3, techId0.numArcProtoIds());
        assertSame(aId, techId0.getArcProtoId(2));
        assertSame(2, aId.chronIndex);
        assertSame(techId0, aId.techId);
        assertSame(1, techId0.numPrimitiveNodeIds());
        assertSame(5, techId0.modCount);
        assertSame(2, techId2.modCount);

        PrimitiveNodeId nId = techId0.newPrimitiveNodeId("a");
        assertSame(2, techId0.numPrimitiveNodeIds());
        assertSame(nId, techId0.getPrimitiveNodeId(1));
        assertSame(1, nId.chronIndex);
        assertSame(techId0, nId.techId);
        assertEquals("a", nId.name);
        assertEquals("tech:a", nId.fullName);
        assertSame(6, techId0.modCount);
        assertSame(2, techId2.modCount);

        nId = techId0.newPrimitiveNodeId("A");
        assertSame(nId0_A, nId);
        assertSame(6, techId0.modCount);

        PrimitivePortId pId = nId0_A.newPortId("q");
        assertSame(7, techId0.modCount);
        assertSame(2, techId0.numPrimitiveNodeIds());
        assertSame(2, nId0_A.numPrimitivePortIds());
        assertSame(pId, nId0_A.getPortId(1));
        assertSame(1, pId.chronIndex);
        assertSame(nId0_A, pId.parentId);
        assertEquals("q", pId.externalId);
        assertEquals("tech:A:q", pId.toString());

        pId = nId0_A.newPortId("p");
        assertSame(7, techId0.modCount);
        assertSame(p0_A_p, pId);
    }

    /**
     * Test of getArcProtoId method, of class TechId.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void getArcProtoIdBad() {
        System.out.println("getArcProtoId bad");
        techId0.getArcProtoId(2);
    }

    /**
     * Test of newArcProtoId method, of class TechId.
     */
    @Test(expected = NullPointerException.class)
    public void newArcProtoIdNull() {
        System.out.println("newArcProtoIdNull");
        techId0.newArcProtoId(null);
    }

    /**
     * Test of newArcProtoId method, of class TechId.
     */
    @Test(expected = IllegalArgumentException.class)
    public void newArcProtoIdBad() {
        System.out.println("newArcProtoIdBad");
        techId0.newArcProtoId("a^b");
    }

    /**
     * Test of getPrimitiveNodeId method, of class TechId.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void getPrimitiveNodeIdBad() {
        System.out.println("getPrimitiveNodeId bad");
        techId0.getPrimitiveNodeId(1);
    }

    /**
     * Test of newPrimitiveNodeId method, of class TechId.
     */
    @Test(expected = NullPointerException.class)
    public void newPrimitiveNodeIdNull() {
        System.out.println("newPrimitiveNodeIdNull");
        techId0.newPrimitiveNodeId(null);
    }

    /**
     * Test of newPrimitiveNodeId method, of class TechId.
     */
    @Test(expected = IllegalArgumentException.class)
    public void newPrimitiveNodeIdBad() {
        System.out.println("newPrimitiveNodeIdBad");
        techId0.newPrimitiveNodeId("a:b");
    }

    /**
     * Test of getPrimitivePortId method, of class TechId.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void getPrimitivePortIdBad() {
        System.out.println("getPrimitivePortId bad");
        nId0_A.getPortId(1);
    }

    /**
     * Test of newPrimtivePortId method, of class TechId.
     */
    @Test(expected = NullPointerException.class)
    public void newPrimitivePortIdNull() {
        System.out.println("newPrimitivePortIdNull");
        nId0_A.newPortId(null);
    }

    /**
     * Test of newPrimitivePortId method, of class TechId.
     */
    @Test(expected = IllegalArgumentException.class)
    public void newPrimitivePortIdBad() {
        System.out.println("newPrimitivePortIdBad");
        nId0_A.newPortId("a|b");
    }

    /**
     * Test of inDatabase method, of class TechId.
     */
    @Test
    public void inDatabase() {
        System.out.println("inDatabase");
        EDatabase database = new EDatabase(idManager.getInitialSnapshot());
        assertNull(techId2.inDatabase(database));

        TechId genericId = idManager.newTechId("generic");
        assertNull(genericId.inDatabase(database));

//        Generic generic = new Generic(idManager);
//        database.addTech(generic);
//        assertSame(generic, genericId.inDatabase(database));
    }

    /**
     * Test of inDatabase method, of class TechId.
     */
    @Test(expected = NullPointerException.class)
    public void inDatabaseNull() {
        System.out.println("inDatabaseNull");
        techId2.inDatabase(null);
    }

    /**
     * Test of toString method, of class TechId.
     */
    @Test
    public void testToString() {
        System.out.println("toString");
        assertSame(techName, techId0.toString());
        assertSame(techId2.techName, techId2.toString());
    }

    /**
     * Test of check method, of class TechId.
     */
    @Test
    public void check() {
        System.out.println("check");
        techId0.check();
        techId2.check();
    }

    /**
     * Test of jelibSafeName method, of class TechId.
     */
    @Test
    public void jelibSafeName() {
        System.out.println("jelibSafeName");
        assertTrue(TechId.jelibSafeName(""));
        assertTrue(TechId.jelibSafeName("?"));
        assertTrue(TechId.jelibSafeName("/"));
        assertTrue(TechId.jelibSafeName("'"));
        assertFalse(TechId.jelibSafeName(" "));
        assertFalse(TechId.jelibSafeName("\n"));
        assertFalse(TechId.jelibSafeName("\\"));
        assertFalse(TechId.jelibSafeName("\""));
        assertFalse(TechId.jelibSafeName(":"));
        assertFalse(TechId.jelibSafeName("|"));
        assertFalse(TechId.jelibSafeName("^"));
    }

    /**
     * Test of jelibSafeName method, of class com.sun.electric.database.id.TechId.
     */
    @Test(expected = NullPointerException.class)
    public void testJelibSafeNameNull() {
        System.out.println("jelibSafeNameNull");
        TechId.jelibSafeName(null);
    }
}
