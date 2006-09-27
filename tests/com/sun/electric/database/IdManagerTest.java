/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IdManagerTest.java
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
import com.sun.electric.database.text.CellName;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class IdManagerTest {
    
    private IdManager idManager;
    private Snapshot initialSnapshot;
    private LibId libId0;
    private LibId libId1;
    private CellId cellId0;
    private CellId cellId1;
    
    @Before public void setUp() throws Exception {
        idManager = new IdManager();
        initialSnapshot = idManager.getInitialSnapshot();
        libId0 = idManager.newLibId("libId0");
        libId1 = idManager.newLibId("libId1");
        CellName cellName0 = CellName.parseName("cell0;1{sch}");
        CellName cellName1 = CellName.parseName("cell1;1{sch}");
        cellId0 = libId1.newCellId(cellName0);
        cellId1 = libId0.newCellId(cellName1);
    }

    @After public void tearDown() throws Exception {
        idManager = null;
        initialSnapshot = null;
        libId0 = libId1 = null;
        cellId0 = cellId1 = null;
    }

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(IdManagerTest.class);
    }

    /**
     * Test of newLibId method, of class com.sun.electric.database.IdManager.
     */
    @Test public void testNewLibId() {
        System.out.println("newLibId");
        
        assertLibId(0, libId0);
        assertLibId(1, libId1);
        LibId libId2 = idManager.newLibId("libId2");
        assertLibId(2, libId2);
        assertSame(libId1, idManager.newLibId("libId1"));
    }

    /**
     * Test of getLibId method, of class com.sun.electric.database.IdManager.
     */
    @Test public void testGetLibId() {
        System.out.println("getLibId");
        
        assertSame(libId1, idManager.getLibId(1));
        LibId libId2 = idManager.newLibId("libId2");
        LibId libId3 = idManager.newLibId("libId3");
        LibId libId4 = idManager.newLibId("libId4");
        LibId libId5 = idManager.newLibId("libId5");
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
    @Test public void testNewCellId() {
        System.out.println("newCellId");
        
        assertCellId(0, cellId0);
        assertCellId(1, cellId1);
        CellName cellName2 = CellName.parseName("cell2;1{sch}");
        CellId cellId2 = libId1.newCellId(cellName2);
        assertCellId(2, cellId2);
    }

    /**
     * Test of getCellId method, of class com.sun.electric.database.IdManager.
     */
    @Test public void testGetCellId() {
        System.out.println("getCellId");
        
        assertSame(cellId1, idManager.getCellId(1));
        CellName cellName2 = CellName.parseName("cell2;1{sch}");
        CellName cellName3 = CellName.parseName("cell3;1{sch}");
        CellName cellName4 = CellName.parseName("cell4;1{sch}");
        CellName cellName5 = CellName.parseName("cell5;1{sch}");
        CellId cellId2 = libId1.newCellId(cellName2);
        CellId cellId3 = libId1.newCellId(cellName3);
        CellId cellId4 = libId1.newCellId(cellName4);
        CellId cellId5 = libId1.newCellId(cellName5);
        assertSame(cellId1, idManager.getCellId(1));
        assertSame(cellId4, idManager.getCellId(4));
        assertSame(cellId5, idManager.getCellId(5));
    }
    
    private void assertCellId(int cellIndex, CellId cellId) {
        assertSame(idManager, cellId.getIdManager());
        assertEquals(cellIndex, cellId.cellIndex);
    }

    /**
     * Test of getInitialSnapshot method, of class com.sun.electric.database.IdManager.
     */
    @Test public void testGetInitialSnapshot() {
        System.out.println("getInitialSnapshot");
        
        assertSame(idManager, initialSnapshot.idManager);
        assertEquals(0, initialSnapshot.snapshotId);
        assertSame(CellBackup.EMPTY_LIST, initialSnapshot.cellBackups);
        assertEquals(0, initialSnapshot.cellGroups.length);
//        assertSame(ERectangle.EMPTY_LIST, initialSnapshot.cellBounds);
        assertSame(LibraryBackup.EMPTY_LIST, initialSnapshot.libBackups);
        
        assertSame(initialSnapshot, idManager.getInitialSnapshot());
    }
    
    /**
     * Test of newSnapshotId method, of class com.sun.electric.database.IdManager.
     */
    @Test public void testNewSnapshotId() {
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
    @Test public void testReadWrite() {
        System.out.println("readWrite");
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
            assertEquals( libId0.libName, mirrorIdManager.getLibId(0).libName );
            assertEquals( libId1.libName, mirrorIdManager.getLibId(1).libName );
            CellId cellId0 = mirrorIdManager.getCellId(0);
            assertEquals( 0, cellId0.numExportIds() );
            CellId cellId1 = mirrorIdManager.getCellId(1);
            assertEquals( 1, cellId1.numExportIds() );
            assertEquals( nameA, cellId1.getPortId(0).externalId );
            
            // Add new staff to database
            assertEquals( 2, idManager.newLibId("libId2").libIndex );
            assertEquals( 3, idManager.newLibId("libId3").libIndex );
            assertEquals( 2, libId1.newCellId(CellName.parseName("cellId2;1")).cellIndex );
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
            
            assertEquals( libId0.libName, mirrorIdManager.getLibId(0).libName );
            assertEquals( libId1.libName, mirrorIdManager.getLibId(1).libName );
            assertEquals( "libId2", mirrorIdManager.getLibId(2).libName );
            assertEquals( "libId3", mirrorIdManager.getLibId(3).libName );
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
