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
    private TechId techId0;
    private TechId techId1;
    private ArcProtoId aId1_a;
    private PrimitiveNodeId nId1_b;
    private PrimitivePortId pId1_b_p;
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
        techId0 = idManager.newTechId("techId0");
        techId1 = idManager.newTechId("techId1");
        aId1_a = techId1.newArcProtoId("a");
        nId1_b = techId1.newPrimitiveNodeId("b");
        pId1_b_p = nId1_b.newPortId("p");
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
            idManager.getCellId(1).newPortId(nameA);

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            IdWriter writer = new IdWriter(idManager, new DataOutputStream(byteStream));
            writer.writeDiffs();
            writer.flush();
            byte[] diffs1 = byteStream.toByteArray();
            byteStream.reset();

            writer.writeDiffs();
            writer.flush();
            assertEquals(20, byteStream.toByteArray().length); // No writing when no changes
            byteStream.reset();

            IdManager mirrorIdManager = new IdManager();

            // First update of mirrorIdManager
            IdReader reader1 = new IdReader(new DataInputStream(new ByteArrayInputStream(diffs1)), mirrorIdManager);
            reader1.readDiffs();

            // Check mirrorIdManager after first update
            assertEquals(techId0.techName, mirrorIdManager.getTechId(0).techName);
            assertEquals(techId1.techName, mirrorIdManager.getTechId(1).techName);
            assertEquals(aId1_a.fullName, mirrorIdManager.getTechId(1).getArcProtoId(0).fullName);
            assertEquals(nId1_b.fullName, mirrorIdManager.getTechId(1).getPrimitiveNodeId(0).fullName);
            assertEquals(pId1_b_p.toString(), mirrorIdManager.getTechId(1).getPrimitiveNodeId(0).getPortId(0).toString());
            assertEquals(libId0.libName, mirrorIdManager.getLibId(0).libName);
            assertEquals(libId1.libName, mirrorIdManager.getLibId(1).libName);
            CellId cellId0 = mirrorIdManager.getCellId(0);
            assertEquals(0, cellId0.numExportIds());
            CellId cellId1 = mirrorIdManager.getCellId(1);
            assertEquals(1, cellId1.numExportIds());
            assertEquals(nameA, cellId1.getPortId(0).externalId);

            // Add new staff to database
            assertEquals(2, idManager.newTechId("techId2").techIndex);
            assertEquals(0, techId0.newArcProtoId("a").chronIndex);
            assertEquals(1, techId0.newArcProtoId("b").chronIndex);
            assertEquals(1, nId1_b.newPortId("Q").chronIndex);
            assertEquals(2, idManager.newLibId("libId2").libIndex);
            assertEquals(3, idManager.newLibId("libId3").libIndex);
            assertEquals(2, libId1.newCellId(CellName.parseName("cellId2;1")).cellIndex);
            String nameB = "B";
            idManager.getCellId(1).newPortId(nameB);
            String nameC = "C";
            idManager.getCellId(2).newPortId(nameC);

            // Second update of mirrorIdManager
            writer.writeDiffs();
            writer.flush();
            byte[] diffs2 = byteStream.toByteArray();
            byteStream.reset();
            IdReader reader2 = new IdReader(new DataInputStream(new ByteArrayInputStream(diffs2)), mirrorIdManager);
            reader2.readDiffs();

            writer.writeDiffs();
            writer.flush();
            assertEquals(20, byteStream.toByteArray().length); // No writing when no changes
            byteStream.reset();

            assertEquals(techId0.techName, mirrorIdManager.getTechId(0).techName);
            assertEquals(techId1.techName, mirrorIdManager.getTechId(1).techName);
            assertEquals("techId2", mirrorIdManager.getTechId(2).techName);
            assertEquals("techId0:a", mirrorIdManager.getTechId(0).getArcProtoId(0).fullName);
            assertEquals("techId0:b", mirrorIdManager.getTechId(0).getArcProtoId(1).fullName);
            assertEquals(aId1_a.fullName, mirrorIdManager.getTechId(1).getArcProtoId(0).fullName);
            assertEquals(nId1_b.fullName, mirrorIdManager.getTechId(1).getPrimitiveNodeId(0).fullName);
            assertEquals(pId1_b_p.toString(), mirrorIdManager.getTechId(1).getPrimitiveNodeId(0).getPortId(0).toString());
            assertEquals("techId1:b:Q", mirrorIdManager.getTechId(1).getPrimitiveNodeId(0).getPortId(1).toString());

            assertEquals(libId0.libName, mirrorIdManager.getLibId(0).libName);
            assertEquals(libId1.libName, mirrorIdManager.getLibId(1).libName);
            assertEquals("libId2", mirrorIdManager.getLibId(2).libName);
            assertEquals("libId3", mirrorIdManager.getLibId(3).libName);
            assertSame(cellId0, mirrorIdManager.getCellId(0));
            assertEquals(0, cellId0.numExportIds());
            assertSame(cellId1, mirrorIdManager.getCellId(1));
            assertEquals(2, cellId1.numExportIds());
            assertEquals(nameA, cellId1.getPortId(0).externalId);
            assertEquals(nameB, cellId1.getPortId(1).externalId);
            CellId cellId2 = mirrorIdManager.getCellId(2);
            assertEquals(1, cellId2.numExportIds());
            assertEquals(nameC, cellId2.getPortId(0).externalId);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
