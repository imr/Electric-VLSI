/*
 * CellIdTest.java
 * JUnit based test
 *
 * Created on May 6, 2006, 7:56 PM
 */

package com.sun.electric.database;

import junit.framework.*;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.text.Name;
import java.util.Arrays;

/**
 *
 * @author dn146861
 */
public class CellIdTest extends TestCase {
    
    IdManager idManager;
    CellId cellId0;
    CellId cellId1;
    CellId cellId2;
    CellUsage u0_2;
    CellUsage u0_1;
    CellUsage u1_2;
    Name nameA = Name.findName("A");
    ExportId e1_A;
    
    public CellIdTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        idManager = new IdManager();
        cellId0 = idManager.newCellId();
        cellId1 = idManager.newCellId();
        cellId2 = idManager.newCellId();
        u0_2 = cellId0.getUsageIn(cellId2);
        u0_1 = cellId0.getUsageIn(cellId1);
        u1_2 = cellId1.getUsageIn(cellId2);
        e1_A = cellId1.newExportId(nameA);
    }

    protected void tearDown() throws Exception {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(CellIdTest.class);
        
        return suite;
    }

    /**
     * Test of numUsagesIn method, of class com.sun.electric.database.CellId.
     */
    public void testNumUsagesIn() {
        System.out.println("numUsagesIn");
        
        int expResult = 2;
        int result = cellId0.numUsagesIn();
        assertEquals(expResult, result);
    }

    /**
     * Test of getUsageIn method, of class com.sun.electric.database.CellId.
     */
    public void testGetUsageIn() {
        System.out.println("getUsageIn");
        
        int i = 0;
        CellId instance = cellId0;
        
        CellUsage expResult = u0_2;
        CellUsage result = instance.getUsageIn(i);
        assertEquals(expResult, result);
    }

    /**
     * Test of numUsagesOf method, of class com.sun.electric.database.CellId.
     */
    public void testNumUsagesOf() {
        System.out.println("numUsagesOf");
        
        CellId instance = cellId2;
        
        int expResult = 2;
        int result = instance.numUsagesOf();
        assertEquals(expResult, result);
    }

    /**
     * Test of getUsageOf method, of class com.sun.electric.database.CellId.
     */
    public void testGetUsageOf() {
        System.out.println("getUsageOf");
        
        int i = 1;
        CellId instance = cellId2;
        
        CellUsage expResult = u1_2;
        CellUsage result = instance.getUsageOf(i);
        assertEquals(expResult, result);
    }

    /**
     * Test of numExportIds method, of class com.sun.electric.database.CellId.
     */
    public void testNumExportIds() {
        System.out.println("numExportIds");
        
        CellId instance = cellId1;
        
        int expResult = 1;
        int result = instance.numExportIds();
        assertEquals(expResult, result);
    }

    /**
     * Test of getPortId method, of class com.sun.electric.database.CellId.
     */
    public void testGetPortId() {
        System.out.println("getPortId");
        
        int chronIndex = 0;
        CellId instance = cellId1;
        
        ExportId expResult = e1_A;
        ExportId result = instance.getPortId(chronIndex);
        assertEquals(expResult, result);
    }

    /**
     * Test of getExportIds method, of class com.sun.electric.database.CellId.
     */
    public void testGetExportIds() {
        System.out.println("getExportIds");
        
        CellId instance = cellId1;
        
        ExportId[] expResult = new ExportId[] { e1_A };
        ExportId[] result = instance.getExportIds();
        assertTrue(Arrays.equals(expResult, result));
    }

    /**
     * Test of newExportId method, of class com.sun.electric.database.CellId.
     */
    public void testNewExportId() {
        System.out.println("newExportId");
        
        Name suggestedName = Name.findName("B");
        CellId instance = cellId1;
        
        ExportId result = instance.newExportId(suggestedName);
        assertSame(cellId1, result.parentId);
        assertSame(suggestedName, result.name);
        assertEquals(1, result.chronIndex);
        
        assertEquals(2, cellId1.numExportIds());
        assertSame(e1_A, cellId1.getPortId(0));
        assertSame(result, cellId1.getPortId(1));
    }

    /**
     * Test of newExportIds method, of class com.sun.electric.database.CellId.
     */
    public void testNewExportIds() {
        System.out.println("newExportIds");
        
        Name[] diskNames = { Name.findName("C"), Name.findName("B")};
        cellId1.newExportIds(diskNames);
        
        assertEquals( 3, cellId1.numExportIds() );
        assertSame(e1_A, cellId1.getPortId(0));
        ExportId e1_C = cellId1.getPortId(1);
        ExportId e1_B = cellId1.getPortId(2);
        assertSame(diskNames[0], e1_C.name );
        assertSame(diskNames[1], e1_B.name );
    }

    /**
     * Test of newNodeId method, of class com.sun.electric.database.CellId.
     */
    public void testNewNodeId() {
        System.out.println("newNodeId");
        
        CellId instance = cellId2;
        
        int expResult = 0;
        int result = instance.newNodeId();
        assertEquals(expResult, result);
    }

    /**
     * Test of newArcId method, of class com.sun.electric.database.CellId.
     */
    public void testNewArcId() {
        System.out.println("newArcId");
        
        CellId instance = cellId1;
        
        int expResult = 0;
        int result = instance.newArcId();
        assertEquals(expResult, result);
    }

    /**
     * Test of inDatabase method, of class com.sun.electric.database.CellId.
     */
    public void testInDatabase() {
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
    public void testToString() {
        System.out.println("toString");
        
        CellId instance = cellId0;
        
        String expResult = "";
        String result = instance.toString();
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of check method, of class com.sun.electric.database.CellId.
     */
    public void testCheck() {
        System.out.println("check");
        
        CellId instance = cellId1;
        
        instance.check();
    }
    
}
