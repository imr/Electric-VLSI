/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellIdTest.java
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
package com.sun.electric.database;


import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.text.CellName;

import java.util.Arrays;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class CellIdTest {
    
    IdManager idManager;
    CellId cellId0;
    CellId cellId1;
    CellId cellId2;
    CellUsage u0_2;
    CellUsage u0_1;
    CellUsage u1_2;
    String nameA = "A";
    ExportId e1_A;
    
    @Before public void setUp() throws Exception {
        idManager = new IdManager();
        LibId libId = idManager.newLibId("lib");
        CellName cellName0 = CellName.parseName("cell0;1{sch}");
        CellName cellName1 = CellName.parseName("cell1;1{sch}");
        CellName cellName2 = CellName.parseName("cell2;1{sch}");
        cellId0 = libId.newCellId(cellName0);
        cellId1 = libId.newCellId(cellName1);
        cellId2 = libId.newCellId(cellName2);
        u0_2 = cellId0.getUsageIn(cellId2);
        u0_1 = cellId0.getUsageIn(cellId1);
        u1_2 = cellId1.getUsageIn(cellId2);
        e1_A = cellId1.newExportId(nameA);
    }

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(CellIdTest.class);
    }

    /**
     * Test of getIdManager method, of class com.sun.electric.database.CellId.
     */
    @Test public void testGetIdManager() {
        System.out.println("getIdManager");
        
        CellId instance = cellId0;
        
        IdManager expResult = idManager;
        IdManager result = instance.getIdManager();
        assertSame(expResult, result);
    }

    /**
     * Test of numUsagesIn method, of class com.sun.electric.database.CellId.
     */
    @Test public void testNumUsagesIn() {
        System.out.println("numUsagesIn");
        
        int expResult = 2;
        int result = cellId0.numUsagesIn();
        assertEquals(expResult, result);
    }

    /**
     * Test of getUsageIn method, of class com.sun.electric.database.CellId.
     */
    @Test public void testGetUsageIn() {
        System.out.println("getUsageIn");
        
        int i = 0;
        CellId instance = cellId0;
        
        CellUsage expResult = u0_2;
        CellUsage result = instance.getUsageIn(i);
        assertSame(expResult, result);
    }

    /**
     * Test of numUsagesOf method, of class com.sun.electric.database.CellId.
     */
    @Test public void testNumUsagesOf() {
        System.out.println("numUsagesOf");
        
        CellId instance = cellId2;
        
        int expResult = 2;
        int result = instance.numUsagesOf();
        assertEquals(expResult, result);
    }

    /**
     * Test of getUsageOf method, of class com.sun.electric.database.CellId.
     */
    @Test public void testGetUsageOf() {
        System.out.println("getUsageOf");
        
        int i = 1;
        CellId instance = cellId2;
        
        CellUsage expResult = u1_2;
        CellUsage result = instance.getUsageOf(i);
        assertSame(expResult, result);
    }

    /**
     * Test of numExportIds method, of class com.sun.electric.database.CellId.
     */
    @Test public void testNumExportIds() {
        System.out.println("numExportIds");
        
        CellId instance = cellId1;
        
        int expResult = 1;
        int result = instance.numExportIds();
        assertEquals(expResult, result);
    }

    /**
     * Test of getPortId method, of class com.sun.electric.database.CellId.
     */
    @Test public void testGetPortId() {
        System.out.println("getPortId");
        
        int chronIndex = 0;
        CellId instance = cellId1;
        
        ExportId expResult = e1_A;
        ExportId result = instance.getPortId(chronIndex);
        assertSame(expResult, result);
    }

    /**
     * Test of newExportId method, of class com.sun.electric.database.CellId.
     */
    @Test public void testNewExportId() {
        System.out.println("newExportId");
        
        String name = nameA;
        CellId instance = cellId1;
        assertEquals(1, instance.numExportIds());
        
        ExportId expResult = e1_A;
        ExportId result = instance.newExportId(name);
        assertSame(expResult, result);
        assertEquals(1, instance.numExportIds());
        
        String nameB = "B";
        ExportId idB = instance.newExportId(nameB);
        assertSame(instance, idB.parentId);
        assertEquals(1, idB.chronIndex);
        assertSame( nameB, idB.externalId );
        assertEquals(2, cellId1.numExportIds());
        
        idManager.checkInvariants();
    }

    /**
     * Test of newExportIds method, of class com.sun.electric.database.CellId.
     */
    @Test(expected = NullPointerException.class) public void testNewExportIdNull() {
        System.out.println("newExportId null");
        
        cellId0.newExportId(null);
    }

    /**
     * Test of randomExportId method, of class com.sun.electric.database.CellId.
     */
    @Test public void testRandomExportId() {
        System.out.println("randomExportId");
        
        String suggestedId = "A";
        CellId instance = cellId1;
        assertEquals(1, instance.numExportIds());
        
        ExportId result = instance.randomExportId(suggestedId);
        assertNotSame(e1_A, result );
        assertEquals(2, instance.numExportIds());
        assertSame(instance.getPortId(1), result);
        assertTrue(result.externalId.startsWith("A@"));
        
        int numCopies = 100000;
        for (int i = 0; i < numCopies; i++)
            instance.randomExportId(suggestedId);
        assertEquals(numCopies + 2, instance.numExportIds() );
        
        idManager.checkInvariants();
    }
    
    /**
     * Test of newNodeId method, of class com.sun.electric.database.CellId.
     */
    @Test public void testNewNodeId() {
        System.out.println("newNodeId");
        
        CellId instance = cellId2;
        
        int expResult = 0;
        int result = instance.newNodeId();
        assertEquals(expResult, result);
    }

    /**
     * Test of newArcId method, of class com.sun.electric.database.CellId.
     */
    @Test public void testNewArcId() {
        System.out.println("newArcId");
        
        CellId instance = cellId1;
        
        int expResult = 0;
        int result = instance.newArcId();
        assertEquals(expResult, result);
    }

    /**
     * Test of inDatabase method, of class com.sun.electric.database.CellId.
     */
    @Test public void testInDatabase() {
        System.out.println("inDatabase");
        
        EDatabase database = new EDatabase(idManager);
        CellId instance = cellId0;
        
        Cell expResult = null;
        Cell result = instance.inDatabase(database);
        assertEquals(expResult, result);
    }

    /**
     * Test of toString method, of class com.sun.electric.database.CellId.
     */
    @Test public void testToString() {
        System.out.println("toString");
        
        CellId instance = cellId0;
        
        String expResult = "lib:cell0;1{sch}";
        String result = instance.toString();
        assertEquals(expResult, result);
    }

    /**
     * Test of check method, of class com.sun.electric.database.CellId.
     */
    @Test public void testCheck() {
        System.out.println("check");
        
        CellId instance = cellId1;
        
        instance.check();
    }
}
