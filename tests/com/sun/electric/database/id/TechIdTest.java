/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.electric.database.id;

import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.technology.Technology;

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
public class TechIdTest {
    
    IdManager idManager;
    String techName = "tech";
    TechId techId0;
    TechId techId2;
    ArcProtoId aId0_a;
    ArcProtoId aId2_a;
    ArcProtoId aId0_A;

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
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of numArcProtoIds method, of class TechId.
     */
    @Test
    public void numArcProtoIds() {
        System.out.println("numArcProtoIds");
        assertEquals(2, techId0.numArcProtoIds());
        assertEquals(1, techId2.numArcProtoIds());
    }

    /**
     * Test of getArcProtoId method, of class TechId.
     */
    @Test
    public void getArcProtoId() {
        System.out.println("getArcProtoId");
        assertSame(aId0_a, techId0.getArcProtoId(0));
        assertSame(aId0_A, techId0.getArcProtoId(1));
        assertSame(aId2_a, techId2.getArcProtoId(0));
    }

    /**
     * Test of getArcProtoId method, of class TechId.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void geyArcProtoIdBad() {
        System.out.println("getArcProtoId bad");
        techId0.getArcProtoId(2);
    }
    /**
     * Test of newArcProtoId method, of class TechId.
     */
    @Test
    public void newArcProtoId() {
        System.out.println("newArcProtoId");
        assertSame(aId0_A, techId0.newArcProtoId("A"));
    }

    /**
     * Test of inDatabase method, of class TechId.
     */
    @Test
    public void inDatabase() {
        System.out.println("inDatabase");
        EDatabase database = new EDatabase(idManager);
        assertNull(techId2.inDatabase(database));
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
        System.out.println("jelibSafeName null");
        TechId.jelibSafeName(null);
    }
}