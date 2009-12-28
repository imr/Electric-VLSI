/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SnapshotTest.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.database;

import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.technology.TechFactory;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;

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
    private TechId genericTechId;
    private TechId schematicTechId;

    @Before
    public void setUp() throws Exception {
        idManager = new IdManager();
        initialSnapshot = idManager.getInitialSnapshot();
        genericTechId = idManager.newTechId("generic");
        schematicTechId = idManager.newTechId("schematic");

        // Init emptyDiffEmpty
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IdWriter writer = new IdWriter(idManager, new DataOutputStream(out));
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

    @After
    public void tearDown() throws Exception {
        idManager = null;
        initialSnapshot = null;
    }

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(SnapshotTest.class);
    }

    /**
     * Test of with method, of class com.sun.electric.database.Snapshot.
     */
    @Test
    public void testWith() {
        System.out.println("with");

        LibId libId = idManager.newLibId("libId0");
        Generic generic = Generic.newInstance(idManager);
        Technology schematic = TechFactory.getTechFactory("schematic").newInstance(generic);
        Environment env = idManager.getInitialEnvironment().addTech(generic).addTech(schematic);
        TechPool techPool = env.techPool;
        ImmutableLibrary l = ImmutableLibrary.newInstance(libId, null, null);
        LibraryBackup libBackup = new LibraryBackup(l, false, new LibId[]{});
        CellName cellName = CellName.parseName("cell;1{sch}");
        CellId cellId = libId.newCellId(cellName);
        ImmutableCell c = ImmutableCell.newInstance(cellId, 0).withTechId(schematicTechId);
        CellBackup cellBackup = CellBackup.newInstance(c, techPool);
        CellTree cellTree = CellTree.newInstance(c, techPool).with(cellBackup, CellTree.NULL_ARRAY, techPool);

        CellTree[] cellTreesArray = {cellTree};
        LibraryBackup[] libBackupsArray = {libBackup};
        Snapshot instance = initialSnapshot.with(null, env, null, null);
        assertEquals(1, instance.snapshotId);

        List<CellTree> expCellTrees = Collections.singletonList(cellTree);
        List<CellBackup> expCellBackups = Collections.singletonList(cellBackup);
        List<LibraryBackup> expLibBackups = Collections.singletonList(libBackup);
        Snapshot result = instance.with(null, null, cellTreesArray, libBackupsArray);
        assertEquals(2, result.snapshotId);
        assertEquals(expCellTrees, result.cellTrees);
        assertEquals(expCellBackups, result.cellBackups);
        assertEquals(expLibBackups, result.libBackups);

//        CellId otherId = new CellId();
//        Variable var = Variable.newInstance(Variable.newKey("ATTR_FOO"), otherId, TextDescriptor.getCellTextDescriptor());
//        ImmutableCell newC = c.withVariable(var);
//        CellBackup newCellBackup = cellBackup.with(newC, 0, (byte)-1, false, null, null, null);
//        CellBackup[] newCellBackups = { newCellBackup };
//        Snapshot newSnapshot = result.with(newCellBackups, cellGroups, cellBoundsArray, libBackupsArray);
    }

    @Test
    public void testWithRenamedIds() {
        System.out.println("withReanmedIds");

        LibId libIdX = idManager.newLibId("X");
        Generic generic = Generic.newInstance(idManager);
        Technology schematic = TechFactory.getTechFactory("schematic").newInstance(generic);
        Environment env = idManager.getInitialEnvironment().addTech(generic).addTech(schematic);
        TechPool techPool = env.techPool;
        ImmutableLibrary libX = ImmutableLibrary.newInstance(libIdX, null, null);
        LibraryBackup libBackupX = new LibraryBackup(libX, false, new LibId[0]);
        LibId libIdY = idManager.newLibId("Y");
        ImmutableLibrary libY = ImmutableLibrary.newInstance(libIdY, null, null);
        LibraryBackup libBackupY = new LibraryBackup(libY, false, new LibId[0]);
        LibraryBackup[] libBackupArray = new LibraryBackup[]{libBackupX, libBackupY};
        CellName cellNameA = CellName.parseName("A;1{sch}");
        CellId cellId0 = libIdX.newCellId(cellNameA);
        ImmutableCell cellA = ImmutableCell.newInstance(cellId0, 0).withTechId(schematicTechId);
        CellBackup cellBackupA = CellBackup.newInstance(cellA, techPool);
        CellTree cellTreeA = CellTree.newInstance(cellA, techPool).with(cellBackupA, CellTree.NULL_ARRAY, techPool);
        CellTree[] cellTreeArray = new CellTree[]{cellTreeA};
        LibId libIdA = idManager.newLibId("A");

        Snapshot oldSnapshot = initialSnapshot.with(null, env, cellTreeArray, libBackupArray);
        IdMapper idMapper = IdMapper.renameLibrary(oldSnapshot, libIdX, libIdA);
        Snapshot newSnapshot = oldSnapshot.withRenamedIds(idMapper, null, null);

        assertEquals(3, newSnapshot.libBackups.size());
        assertNull(newSnapshot.libBackups.get(0));
        assertSame(libBackupY, newSnapshot.libBackups.get(1));
        LibraryBackup libBackupA = newSnapshot.libBackups.get(2);
        assertSame(libIdA, libBackupA.d.libId);
        assertTrue(libBackupA.modified);

        assertEquals(2, newSnapshot.cellBackups.size());
        assertNull(newSnapshot.cellBackups.get(0));
        CellBackup newCellA = newSnapshot.cellBackups.get(1);
        assertSame(libIdA, newCellA.cellRevision.d.cellId.libId);
        assertSame(cellNameA, newCellA.cellRevision.d.cellId.cellName);
        assertSame(libIdA, newCellA.cellRevision.d.getLibId());
        assertEquals(idManager.getCellId(1), newCellA.cellRevision.d.cellId);
    }

    /**
     * Test of getChangedLibraries method, of class com.sun.electric.database.Snapshot.
     */
    @Test
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
    @Test
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
    @Test
    public void testGetCell() {
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
    @Test
    public void testGetCellBounds() {
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
    @Test
    public void testGetLib() {
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
    @Test
    public void testWriteDiffs() throws Exception {
        System.out.println("writeDiffs");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IdWriter writer = new IdWriter(idManager, new DataOutputStream(out));
        Snapshot oldSnapshot = initialSnapshot;
        Snapshot instance = initialSnapshot;

        instance.writeDiffs(writer, oldSnapshot);
        assertTrue(Arrays.equals(out.toByteArray(), emptyDiffEmpty));
    }

    /**
     * Test of readSnapshot method, of class com.sun.electric.database.Snapshot.
     */
    @Test
    public void testReadSnapshot() throws Exception {
        System.out.println("readSnapshot");

        IdManager idManager = new IdManager();
        IdReader reader = new IdReader(new DataInputStream(new ByteArrayInputStream(emptyDiffEmpty)), idManager);
        Snapshot oldSnapshot = idManager.getInitialSnapshot();

        ImmutableArrayList<CellBackup> expCellBackups = new ImmutableArrayList<CellBackup>(CellBackup.NULL_ARRAY);
        Snapshot result = Snapshot.readSnapshot(reader, oldSnapshot);
        assertEquals(expCellBackups, result.cellBackups);
    }

    /**
     * Test of check method, of class com.sun.electric.database.Snapshot.
     */
    @Test
    public void testCheck() {
        System.out.println("check");

        Snapshot instance = initialSnapshot;

        instance.check();
    }
}
