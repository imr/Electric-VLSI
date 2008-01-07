/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IdReaderWriterTest.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.Snapshot;
import com.sun.electric.database.text.CellName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests of IdReader and IdWriter
 */
public class IdReaderWriterTest {

    private IdManager idManager;
    private Snapshot initialSnapshot;
    private LibId libId0;
    private LibId libId1;
    private CellId cellId0;
    private CellId cellId1;
    
    public IdReaderWriterTest() {
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
        initialSnapshot = idManager.getInitialSnapshot();
        libId0 = idManager.newLibId("libId0");
        libId1 = idManager.newLibId("libId1");
        CellName cellName0 = CellName.parseName("cell0;1{sch}");
        CellName cellName1 = CellName.parseName("cell1;1{sch}");
        cellId0 = libId1.newCellId(cellName0);
        cellId1 = libId0.newCellId(cellName1);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of writeDiffs and readDiff method, of class com.sun.electric.database.IdManager.
     */
    @Test
    public void testReadWrite() {
        System.out.println("readWrite");
        try {
            String nameA = "A";
            idManager.getCellId(1).newExportId(nameA);
            
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            IdWriter writer = new IdWriter(idManager, new DataOutputStream(byteStream));
            writer.writeDiffs();
            writer.flush();
            byte[] diffs1 = byteStream.toByteArray();
            byteStream.reset();
            
            IdManager mirrorIdManager = new IdManager();
            
            // First update of mirrorIdManager
            IdReader reader1 = new IdReader(new DataInputStream(new ByteArrayInputStream(diffs1)), mirrorIdManager);
            reader1.readDiffs();
            
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
            
            // Second update of mirrorIdManager
            writer.writeDiffs();
            writer.flush();
            byte[] diffs2 = byteStream.toByteArray();
            IdReader reader2 = new IdReader(new DataInputStream(new ByteArrayInputStream(diffs2)), mirrorIdManager);
            reader2.readDiffs();
            
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