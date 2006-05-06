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
import junit.framework.*;

/**
 * This module tests Snapshots.
 */
public class SnapshotTest extends TestCase {
    
    private static byte[] emptyDiffEmpty;
    private IdManager idManager;
    private Snapshot initialSnapshot;
    
    public SnapshotTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
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

    protected void tearDown() throws Exception {
        idManager = null;
        initialSnapshot = null;
    }

    public static void main(String[] args) {}
    
    public static Test suite() {
        TestSuite suite = new TestSuite(SnapshotTest.class);
        return suite;
    }

    public void testInit() {
        System.out.println("init");
    }
    
    /**
     * Test of with method, of class com.sun.electric.database.Snapshot.
     */
    public void testWith() {
        System.out.println("with");
        
        LibId libId = idManager.newLibId();
        ImmutableLibrary l = ImmutableLibrary.newInstance(libId, "lib", null, null);
        LibraryBackup libBackup = new LibraryBackup(l, false, new LibId[]{});
        CellId cellId = idManager.newCellId();
        CellName cellName = CellName.parseName("cell;1{sch}");
        ImmutableCell c = ImmutableCell.newInstance(cellId, libId, cellName, 0).withTech(Schematics.tech);
        CellBackup cellBackup = new CellBackup(c);
        
        CellBackup[] cellBackupsArray = { cellBackup };
        int[] cellGroups = { 0 }; 
        ERectangle emptyBound = new ERectangle(0, 0, 0, 0);
        ERectangle[] cellBoundsArray = { emptyBound };
        LibraryBackup[] libBackupsArray = { libBackup };
        Snapshot instance = initialSnapshot;
        
        List<CellBackup> expCellBackups = Collections.singletonList(cellBackup);
        List<ERectangle> expCellBounds = Collections.singletonList(emptyBound);
        List<LibraryBackup> expLibBackups = Collections.singletonList(libBackup);
        Snapshot result = instance.with(cellBackupsArray, cellGroups, cellBoundsArray, libBackupsArray);
        assertEquals(1, result.snapshotId);
        assertEquals(expCellBackups, result.cellBackups);
        assertEquals(expCellBounds, result.cellBounds);
        assertTrue(Arrays.equals(cellGroups, result.cellGroups));
        assertEquals(expLibBackups, result.libBackups);
        
//        CellId otherId = new CellId();
//        Variable var = Variable.newInstance(Variable.newKey("ATTR_FOO"), otherId, TextDescriptor.getCellTextDescriptor());
//        ImmutableCell newC = c.withVariable(var);
//        CellBackup newCellBackup = cellBackup.with(newC, 0, (byte)-1, false, null, null, null);
//        CellBackup[] newCellBackups = { newCellBackup };
//        Snapshot newSnapshot = result.with(newCellBackups, cellGroups, cellBoundsArray, libBackupsArray);
    }
    
    /**
     * Test of getChangedLibraries method, of class com.sun.electric.database.Snapshot.
     */
    public void testGetChangedLibraries() {
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
    public void testGetChangedCells() {
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
    public void testGetCell() {
        System.out.println("getCell");
        
        CellId cellId = idManager.newCellId();
        assertEquals(0, cellId.cellIndex);
        Snapshot instance = initialSnapshot;
        
        CellBackup expResult = null;
        CellBackup result = instance.getCell(cellId);
        assertEquals(expResult, result);
    }

    /**
     * Test of getCellBounds method, of class com.sun.electric.database.Snapshot.
     */
    public void testGetCellBounds() {
        System.out.println("getCellBounds");
        
        CellId cellId = idManager.newCellId();
        assertEquals(0, cellId.cellIndex);
        Snapshot instance = initialSnapshot;
        
        ERectangle expResult = null;
        ERectangle result = instance.getCellBounds(cellId);
        assertEquals(expResult, result);
    }

    /**
     * Test of getLib method, of class com.sun.electric.database.Snapshot.
     */
    public void testGetLib() {
        System.out.println("getLib");
        
        LibId libId = idManager.newLibId();
        assertEquals(0, libId.libIndex);
        Snapshot instance = initialSnapshot;
        
        LibraryBackup expResult = null;
        LibraryBackup result = instance.getLib(libId);
        assertEquals(expResult, result);
    }

    /**
     * Test of writeDiffs method, of class com.sun.electric.database.Snapshot.
     */
    public void testWriteDiffs() throws Exception {
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
    public void testReadSnapshot() throws Exception {
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
    public void testCheck() {
        System.out.println("check");
        
        Snapshot instance = initialSnapshot;
        
        instance.check();
    }
}
