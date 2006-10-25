/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SnapshotTest.java
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

import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Schematics;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This module tests Snapshots.
 */
public class SnapshotTest {
    
    private static byte[] emptyDiffEmpty;
    private IdManager idManager;
    private Snapshot initialSnapshot;
    
    @Before public void setUp() throws Exception {
        idManager = new IdManager();
        initialSnapshot = idManager.getInitialSnapshot();
        
        // Preload technology
        Technology tech = Schematics.tech;
        
        // Init emptyDiffEmpty
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SnapshotWriter writer = new SnapshotWriter(new DataOutputStream(out));
        Snapshot oldSnapshot = initialSnapshot;
        Snapshot instance = initialSnapshot;
        try {
            initialSnapshot.writeDiffs(writer, initialSnapshot);
            writer.flush();
            emptyDiffEmpty = out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After public void tearDown() throws Exception {
        idManager = null;
        initialSnapshot = null;
    }

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(SnapshotTest.class);
    }

    /**
     * Test of with method, of class com.sun.electric.database.Snapshot.
     */
    @Test public void testWith() {
        System.out.println("with");
        
        LibId libId = idManager.newLibId("libId0");
        ImmutableLibrary l = ImmutableLibrary.newInstance(libId, null, null);
        LibraryBackup libBackup = new LibraryBackup(l, false, new LibId[]{});
        CellName cellName = CellName.parseName("cell;1{sch}");
        CellId cellId = libId.newCellId(cellName);
        ImmutableCell c = ImmutableCell.newInstance(cellId, 0).withTech(Schematics.tech);
        CellBackup cellBackup = new CellBackup(c);
        
        CellBackup[] cellBackupsArray = { cellBackup };
        ERectangle emptyBound = ERectangle.fromGrid(0, 0, 0, 0);
        ERectangle[] cellBoundsArray = { emptyBound };
        LibraryBackup[] libBackupsArray = { libBackup };
        Snapshot instance = initialSnapshot;
        
        List<CellBackup> expCellBackups = Collections.singletonList(cellBackup);
        List<ERectangle> expCellBounds = Collections.singletonList(emptyBound);
        List<LibraryBackup> expLibBackups = Collections.singletonList(libBackup);
        Snapshot result = instance.with(null, cellBackupsArray, cellBoundsArray, libBackupsArray);
        assertEquals(1, result.snapshotId);
        assertEquals(expCellBackups, result.cellBackups);
//        assertEquals(expCellBounds, result.cellBounds);
        assertEquals(expLibBackups, result.libBackups);
        
//        CellId otherId = new CellId();
//        Variable var = Variable.newInstance(Variable.newKey("ATTR_FOO"), otherId, TextDescriptor.getCellTextDescriptor());
//        ImmutableCell newC = c.withVariable(var);
//        CellBackup newCellBackup = cellBackup.with(newC, 0, (byte)-1, false, null, null, null);
//        CellBackup[] newCellBackups = { newCellBackup };
//        Snapshot newSnapshot = result.with(newCellBackups, cellGroups, cellBoundsArray, libBackupsArray);
    }
    
    @Test public void testWithRenamedIds() {
        System.out.println("withReanmedIds");
        
        LibId libIdX = idManager.newLibId("X");
        ImmutableLibrary libX = ImmutableLibrary.newInstance(libIdX, null, null);
        LibraryBackup libBackupX = new LibraryBackup(libX, false, new LibId[0]);
        LibId libIdY = idManager.newLibId("Y");
        ImmutableLibrary libY = ImmutableLibrary.newInstance(libIdY, null, null);
        LibraryBackup libBackupY = new LibraryBackup(libY, false, new LibId[0]);
        LibraryBackup[] libBackupArray = new LibraryBackup[] { libBackupX, libBackupY };
        CellName cellNameA = CellName.parseName("A;1{sch}");
        CellId cellId0 = libIdX.newCellId(cellNameA);
        ImmutableCell cellA = ImmutableCell.newInstance(cellId0, 0).withTech(Schematics.tech);
        CellBackup cellBackupA = new CellBackup(cellA);
        CellBackup[] cellBackupArray = new CellBackup[] { cellBackupA };
        ERectangle[] cellBoundsArray = new ERectangle[] { ERectangle.fromGrid(0, 0, 0, 0) }; 
        LibId libIdA = idManager.newLibId("A");
        
        Snapshot oldSnapshot = initialSnapshot.with(null, cellBackupArray, cellBoundsArray, libBackupArray);
        IdMapper idMapper = IdMapper.renameLibrary(oldSnapshot, libIdX, libIdA);
        Snapshot newSnapshot = oldSnapshot.withRenamedIds(idMapper, null, null);
        
        assertEquals(3, newSnapshot.libBackups.size());
        assertNull( newSnapshot.libBackups.get(0) );
        assertSame( libBackupY, newSnapshot.libBackups.get(1) );
        LibraryBackup libBackupA = newSnapshot.libBackups.get(2);
        assertSame( libIdA, libBackupA.d.libId );
        assertTrue( libBackupA.modified );
        
        assertEquals(2, newSnapshot.cellBackups.size());
        assertNull( newSnapshot.cellBackups.get(0) );
        CellBackup newCellA = newSnapshot.cellBackups.get(1);
        assertSame( libIdA, newCellA.d.cellId.libId );
        assertSame( cellNameA, newCellA.d.cellId.cellName );
        assertSame( libIdA, newCellA.d.getLibId() );
        assertEquals( idManager.getCellId(1), newCellA.d.cellId );
    }
    
    /**
     * Test of getChangedLibraries method, of class com.sun.electric.database.Snapshot.
     */
    @Test public void testGetChangedLibraries() {
        System.out.println("getChangedLibraries");
        
        Snapshot oldSnapshot = initialSnapshot;
        Snapshot instance = initialSnapshot;
        
        List<LibId> expResult = Collections.emptyList();
        List<LibId> result = instance.getChangedLibraries(oldSnapshot);
        assertEquals(expResult, result);
    }

    /**
     * Test of getChangedCells method, of class com.sun.electric.database.Snapshot.
     */
    @Test public void testGetChangedCells() {
        System.out.println("getChangedCells");
        
        Snapshot oldSnapshot = initialSnapshot;
        Snapshot instance = initialSnapshot;
        
        List<CellId> expResult = Collections.emptyList();
        List<CellId> result = instance.getChangedCells(oldSnapshot);
        assertEquals(expResult, result);
    }

    /**
     * Test of getCell method, of class com.sun.electric.database.Snapshot.
     */
    @Test public void testGetCell() {
        System.out.println("getCell");
        
        LibId libId = idManager.newLibId("lib");
        CellId cellId = libId.newCellId(CellName.parseName("cellId0;1"));
        assertEquals(0, cellId.cellIndex);
        Snapshot instance = initialSnapshot;
        
        CellBackup expResult = null;
        CellBackup result = instance.getCell(cellId);
        assertEquals(expResult, result);
    }

    /**
     * Test of getCellBounds method, of class com.sun.electric.database.Snapshot.
     */
    @Test public void testGetCellBounds() {
        System.out.println("getCellBounds");
        
        LibId libId = idManager.newLibId("lib");
        CellId cellId = libId.newCellId(CellName.parseName("cellId0;2"));
        assertEquals(0, cellId.cellIndex);
        Snapshot instance = initialSnapshot;
        
        ERectangle expResult = null;
        ERectangle result = instance.getCellBounds(cellId);
        assertEquals(expResult, result);
    }

    /**
     * Test of getLib method, of class com.sun.electric.database.Snapshot.
     */
    @Test public void testGetLib() {
        System.out.println("getLib");
        
        LibId libId = idManager.newLibId("libId0");
        assertEquals(0, libId.libIndex);
        Snapshot instance = initialSnapshot;
        
        LibraryBackup expResult = null;
        LibraryBackup result = instance.getLib(libId);
        assertEquals(expResult, result);
    }

    /**
     * Test of writeDiffs method, of class com.sun.electric.database.Snapshot.
     */
    @Test public void testWriteDiffs() throws Exception {
        System.out.println("writeDiffs");
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SnapshotWriter writer = new SnapshotWriter(new DataOutputStream(out));
        Snapshot oldSnapshot = initialSnapshot;
        Snapshot instance = initialSnapshot;
        
        instance.writeDiffs(writer, oldSnapshot);
        assertTrue(Arrays.equals(out.toByteArray(), emptyDiffEmpty));
    }

    /**
     * Test of readSnapshot method, of class com.sun.electric.database.Snapshot.
     */
    @Test public void testReadSnapshot() throws Exception {
        System.out.println("readSnapshot");
        
        SnapshotReader reader = new SnapshotReader(new DataInputStream(new ByteArrayInputStream(emptyDiffEmpty)), idManager);
        Snapshot oldSnapshot = initialSnapshot;
        
        ImmutableArrayList<CellBackup> expCellBackups = new ImmutableArrayList<CellBackup>(CellBackup.NULL_ARRAY);
        Snapshot result = Snapshot.readSnapshot(reader, oldSnapshot);
        assertEquals(expCellBackups, result.cellBackups);
    }

    /**
     * Test of check method, of class com.sun.electric.database.Snapshot.
     */
    @Test public void testCheck() {
        System.out.println("check");
        
        Snapshot instance = initialSnapshot;
        
        instance.check();
    }
}
