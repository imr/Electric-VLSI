/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Snapshot.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.text.Name;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import junit.framework.*;

/**
 *
 * @author dn146861
 */
public class IdManagerTest extends TestCase {
    
    private IdManager idManager;
    private Snapshot initialSnapshot;
    private LibId libId0;
    private LibId libId1;
    private CellId cellId0;
    private CellId cellId1;
    
    public IdManagerTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        idManager = new IdManager();
        initialSnapshot = idManager.getInitialSnapshot();
        libId0 = idManager.newLibId();
        libId1 = idManager.newLibId();
        cellId0 = idManager.newCellId();
        cellId1 = idManager.newCellId();
    }

    protected void tearDown() throws Exception {
        idManager = null;
        initialSnapshot = null;
        libId0 = libId1 = null;
        cellId0 = cellId1 = null;
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(IdManagerTest.class);
        return suite;
    }

    /**
     * Test of newLibId method, of class com.sun.electric.database.IdManager.
     */
    public void testNewLibId() {
        System.out.println("newLibId");
        
        assertLibId(0, libId0);
        assertLibId(1, libId1);
        LibId libId2 = idManager.newLibId();
        assertLibId(2, libId2);
    }

    /**
     * Test of getLibId method, of class com.sun.electric.database.IdManager.
     */
    public void testGetLibId() {
        System.out.println("getLibId");
        
        assertSame(libId1, idManager.getLibId(1));
        LibId libId2 = idManager.newLibId();
        LibId libId3 = idManager.newLibId();
        LibId libId4 = idManager.newLibId();
        LibId libId5 = idManager.newLibId();
        assertSame(libId1, idManager.getLibId(1));
        assertSame(libId4, idManager.getLibId(4));
        assertSame(libId5, idManager.getLibId(5));
    }
    
    private void assertLibId(int libIndex, LibId libId) {
        assertSame(idManager, libId.idManager);
        assertEquals(libIndex, libId.libIndex);
    }

    /**
     * Test of newCellId method, of class com.sun.electric.database.IdManager.
     */
    public void testNewCellId() {
        System.out.println("newCellId");
        
        assertCellId(0, cellId0);
        assertCellId(1, cellId1);
        CellId cellId2 = idManager.newCellId();
        assertCellId(2, cellId2);
    }

    /**
     * Test of getCellId method, of class com.sun.electric.database.IdManager.
     */
    public void testGetCellId() {
        System.out.println("getCellId");
        
        assertSame(cellId1, idManager.getCellId(1));
        CellId cellId2 = idManager.newCellId();
        CellId cellId3 = idManager.newCellId();
        CellId cellId4 = idManager.newCellId();
        CellId cellId5 = idManager.newCellId();
        assertSame(cellId1, idManager.getCellId(1));
        assertSame(cellId4, idManager.getCellId(4));
        assertSame(cellId5, idManager.getCellId(5));
    }
    
    private void assertCellId(int cellIndex, CellId cellId) {
        assertSame(idManager, cellId.idManager);
        assertEquals(cellIndex, cellId.cellIndex);
    }

    /**
     * Test of getInitialSnapshot method, of class com.sun.electric.database.IdManager.
     */
    public void testGetInitialSnapshot() {
        System.out.println("getInitialSnapshot");
        
        assertSame(idManager, initialSnapshot.idManager);
        assertEquals(0, initialSnapshot.snapshotId);
        assertSame(CellBackup.EMPTY_LIST, initialSnapshot.cellBackups);
        assertEquals(0, initialSnapshot.cellGroups.length);
        assertSame(ERectangle.EMPTY_LIST, initialSnapshot.cellBounds);
        assertSame(LibraryBackup.EMPTY_LIST, initialSnapshot.libBackups);
        
        assertSame(initialSnapshot, idManager.getInitialSnapshot());
    }
    
    /**
     * Test of newSnapshotId method, of class com.sun.electric.database.IdManager.
     */
    public void testNewSnapshotId() {
        System.out.println("newSnapshotId");
        assertEquals(1, idManager.newSnapshotId());
        assertEquals(2, idManager.newSnapshotId());
//        long startTime = System.currentTimeMillis();
//        int snapshotId = -1;
//        for (int i = 0; i < 10000000; i++)
//            snapshotId = idManager.newSnapshotId();
//        long stopTime = System.currentTimeMillis();
//        System.out.println(snapshotId + " ids in " + (stopTime - startTime) + " msec");
    }
        
    /**
     * Test of writeDiffs and readDiff method, of class com.sun.electric.database.IdManager.
     */
    public void testReadWrite() {
        try {
            String nameA = "A";
            idManager.getCellId(1).newExportId(nameA);
            
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            SnapshotWriter writer = new SnapshotWriter(new DataOutputStream(byteStream));
            idManager.writeDiffs(writer);
            writer.flush();
            byte[] diffs1 = byteStream.toByteArray();
            byteStream.reset();
            
            IdManager mirrorIdManager = new IdManager();
            
            // First update of mirrorIdManager
            SnapshotReader reader1 = new SnapshotReader(new DataInputStream(new ByteArrayInputStream(diffs1)), mirrorIdManager);
            mirrorIdManager.readDiffs(reader1);
            
            // Check mirrorIdManager after first update
            assertTrue( mirrorIdManager.getLibId(0) != null );
            assertTrue( mirrorIdManager.getLibId(1) != null );
            CellId cellId0 = mirrorIdManager.getCellId(0);
            assertEquals( 0, cellId0.numExportIds() );
            CellId cellId1 = mirrorIdManager.getCellId(1);
            assertEquals( 1, cellId1.numExportIds() );
            assertEquals( nameA, cellId1.getPortId(0).externalId );
            
            // Add new staff to database
            assertEquals( 2, idManager.newLibId().libIndex );
            assertEquals( 3, idManager.newLibId().libIndex );
            assertEquals( 2, idManager.newCellId().cellIndex );
            String nameB = "B";
            idManager.getCellId(1).newExportId(nameB);
            String nameC = "C";
            idManager.getCellId(2).newExportId(nameC);
            
            // Second update of mirrirIdManager
            idManager.writeDiffs(writer);
            writer.flush();
            byte[] diffs2 = byteStream.toByteArray();
            SnapshotReader reader2 = new SnapshotReader(new DataInputStream(new ByteArrayInputStream(diffs2)), mirrorIdManager);
            mirrorIdManager.readDiffs(reader2);
            
            assertTrue( mirrorIdManager.getLibId(0) != null );
            assertTrue( mirrorIdManager.getLibId(1) != null );
            assertTrue( mirrorIdManager.getLibId(2) != null );
            assertTrue( mirrorIdManager.getLibId(3) != null );
            assertSame( cellId0, mirrorIdManager.getCellId(0) );
            assertEquals( 0, cellId0.numExportIds() );
            assertSame( cellId1, mirrorIdManager.getCellId(1) );
            assertEquals( 2, cellId1.numExportIds() );
            assertEquals( nameA, cellId1.getPortId(0).externalId );
            assertEquals( nameB, cellId1.getPortId(1).externalId );
            CellId cellId2 = mirrorIdManager.getCellId(2);
            assertEquals( 1, cellId2.numExportIds() );
            assertEquals( nameC, cellId2.getPortId(0).externalId );
        } catch (IOException e) {
            fail(e.getMessage());
        }
        
    }
}
