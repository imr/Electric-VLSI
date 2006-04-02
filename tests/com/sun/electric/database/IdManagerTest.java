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
        LibId libId4 = idManager.getLibId(4);
        assertLibId(4, libId4);
        LibId libId5 = idManager.newLibId();
        assertLibId(5, libId5);
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
        CellId cellId4 = idManager.getCellId(4);
        assertCellId(4, cellId4);
        CellId cellId5 = idManager.newCellId();
        assertCellId(5, cellId5);
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
        long startTime = System.currentTimeMillis();
        int snapshotId = -1;
        for (int i = 0; i < 10000000; i++)
            snapshotId = idManager.newSnapshotId();
        long stopTime = System.currentTimeMillis();
        System.out.println(snapshotId + " ids in " + (stopTime - startTime) + " msec");
    }
        
}
