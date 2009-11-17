/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TextDescriptorTest.java
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
package com.sun.electric.database;

import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.id.ArcProtoId;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.ExportId;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.PrimitivePortId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.text.CellName;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.io.Serializable;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test of EObjectInputStrean and EObjectOutputStream
 */
public class SerializationTest {

    private IdManager idManager;
    private TechId techId0;
    private ArcProtoId arcProtoId0;
    private PrimitiveNodeId primitiveNodeId0;
    private PrimitivePortId primitivePortId0;
    private LibId libId0;
    private CellId cellId0;
    private ExportId exportId0;
    private Generic generic;
    private EDatabase database;

    public SerializationTest() {
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
        techId0 = idManager.newTechId("tech0");
        arcProtoId0 = techId0.newArcProtoId("ap0");
        primitiveNodeId0 = techId0.newPrimitiveNodeId("pn0");
        primitivePortId0 = primitiveNodeId0.newPortId("pp0");
        libId0 = idManager.newLibId("lib0");
        cellId0 = libId0.newCellId(CellName.parseName("cell0;1{lay}"));
        exportId0 = cellId0.newPortId("export0");
        generic = Generic.newInstance(idManager);
        Environment env = idManager.getInitialEnvironment().addTech(generic);
        database = new EDatabase(env);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void ids() {
        System.out.println("ids");
        try {
            ArcProto ap0 = generic.invisible_arc;
            PrimitiveNode pn0 = generic.cellCenterNode;
            PrimitivePort pp0 = pn0.getPort(0);
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new EObjectOutputStream(byteStream, database);
            out.writeObject(techId0);
            out.writeObject(arcProtoId0);
            out.writeObject(primitiveNodeId0);
            out.writeObject(primitivePortId0);
            out.writeObject(libId0);
            out.writeObject(cellId0);
            out.writeObject(exportId0);
            out.writeObject(generic);
            out.writeObject(ap0);
            out.writeObject(pn0);
            out.writeObject(pp0);
            out.flush();
            byte[] serialized = byteStream.toByteArray();
            out.close();

            ObjectInputStream in = new EObjectInputStream(new ByteArrayInputStream(serialized), database);
            TechId techId = (TechId) in.readObject();
            ArcProtoId arcProtoId = (ArcProtoId) in.readObject();
            PrimitiveNodeId primitiveNodeId = (PrimitiveNodeId) in.readObject();
            PrimitivePortId primitivePortId = (PrimitivePortId) in.readObject();
            LibId libId = (LibId) in.readObject();
            CellId cellId = (CellId) in.readObject();
            ExportId exportId = (ExportId) in.readObject();
            Technology tech = (Technology) in.readObject();
            ArcProto ap = (ArcProto) in.readObject();
            PrimitiveNode pn = (PrimitiveNode) in.readObject();
            PrimitivePort pp = (PrimitivePort) in.readObject();
            in.close();

            assertSame(techId0, techId);
            assertSame(arcProtoId0, arcProtoId);
            assertSame(primitiveNodeId0, primitiveNodeId);
            assertSame(primitivePortId0, primitivePortId);
            assertSame(libId0, libId);
            assertSame(cellId0, cellId);
            assertSame(exportId0, exportId);
            assertSame(generic, tech);
            assertSame(ap0, ap);
            assertSame(pn0, pn);
            assertSame(pp0, pp);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    @Test(expected = NotSerializableException.class)
    public void techIdOther() throws IOException {
        System.out.println("techIdOther");
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new EObjectOutputStream(byteStream, database);
        IdManager otherIdManager = new IdManager();
        TechId techId = otherIdManager.newTechId("tech");
        out.writeObject(techId);
    }

    @Test(expected = NotSerializableException.class)
    public void arcProtoIdOther() throws IOException {
        System.out.println("arcProtoIdOther");
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new EObjectOutputStream(byteStream, database);
        IdManager otherIdManager = new IdManager();
        TechId techId = otherIdManager.newTechId("tech");
        ArcProtoId arcProtoId = techId.newArcProtoId("ap");
        out.writeObject(arcProtoId);
    }

    @Test(expected = NotSerializableException.class)
    public void primitiveNodeIdOther() throws IOException {
        System.out.println("primitiveNodeIdOther");
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new EObjectOutputStream(byteStream, database);
        IdManager otherIdManager = new IdManager();
        TechId techId = otherIdManager.newTechId("tech");
        PrimitiveNodeId primitiveNodeId = techId.newPrimitiveNodeId("pn");
        out.writeObject(primitiveNodeId);
    }

    @Test(expected = NotSerializableException.class)
    public void primitivePortIdOther() throws IOException {
        System.out.println("primitivePortIdOther");
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new EObjectOutputStream(byteStream, database);
        IdManager otherIdManager = new IdManager();
        TechId techId = otherIdManager.newTechId("tech");
        PrimitiveNodeId primitiveNodeId = techId.newPrimitiveNodeId("pn");
        PrimitivePortId primitivePortId = primitiveNodeId.newPortId("pp");
        out.writeObject(primitivePortId);
    }

    @Test(expected = NotSerializableException.class)
    public void libIdOther() throws IOException {
        System.out.println("libIdOther");
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new EObjectOutputStream(byteStream, database);
        IdManager otherIdManager = new IdManager();
        LibId libId = otherIdManager.newLibId("lib");
        out.writeObject(libId);
    }

    @Test(expected = NotSerializableException.class)
    public void cellIdOther() throws IOException {
        System.out.println("cellIdOther");
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new EObjectOutputStream(byteStream, database);
        IdManager otherIdManager = new IdManager();
        LibId libId = otherIdManager.newLibId("lib");
        CellId cellId = libId.newCellId(CellName.parseName("cell;1{lay}"));
        out.writeObject(cellId);
    }

    @Test(expected = NotSerializableException.class)
    public void exportIdOther() throws IOException {
        System.out.println("exportIdOther");
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new EObjectOutputStream(byteStream, database);
        IdManager otherIdManager = new IdManager();
        LibId libId = otherIdManager.newLibId("lib");
        CellId cellId = libId.newCellId(CellName.parseName("cell;1{lay}"));
        ExportId exportId = cellId.newPortId("export");
        out.writeObject(exportId);
    }

    private static class CellIdPtr implements Serializable {

        private CellId cellId;
    }

    @Test(expected = NotSerializableException.class)
    public void cellIdOtherIndirect() throws IOException {
        System.out.println("cellIdOther");
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new EObjectOutputStream(byteStream, database);
        IdManager otherIdManager = new IdManager();
        LibId libId = otherIdManager.newLibId("lib");
        CellId cellId = libId.newCellId(CellName.parseName("cell;1{lay}"));
        CellIdPtr cellIdPtr = new CellIdPtr();
        cellIdPtr.cellId = cellId;
        out.writeObject(cellIdPtr);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void libIdToOtherIdManager() throws IOException, ClassNotFoundException {
        System.out.println("libIdToOtherIdManager");
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new EObjectOutputStream(byteStream, database);
        out.writeObject(libId0);
        out.flush();
        byte[] serialized = byteStream.toByteArray();
        out.close();

        IdManager otherIdManager = new IdManager();
        EDatabase otherDatabase = new EDatabase(otherIdManager.getInitialSnapshot());

        ObjectInputStream in = new EObjectInputStream(new ByteArrayInputStream(serialized), otherDatabase);
        LibId libId = (LibId) in.readObject();
    }

    @Test(expected = InvalidObjectException.class)
    public void technologyToOtherDatabase() throws IOException, ClassNotFoundException {
        System.out.println("technologyToOtherDatabase");
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new EObjectOutputStream(byteStream, database);
        out.writeObject(generic);
        out.flush();
        byte[] serialized = byteStream.toByteArray();
        out.close();

        EDatabase otherDatabase = new EDatabase(idManager.getInitialSnapshot());
        ObjectInputStream in = new EObjectInputStream(new ByteArrayInputStream(serialized), otherDatabase);
        Technology tech = (Technology) in.readObject();
    }

    @Test(expected = NotSerializableException.class)
    public void techNotLinked() throws IOException, ClassNotFoundException {
        System.out.println("techNotLinked");
        Technology tech0 = Generic.newInstance(idManager);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new EObjectOutputStream(byteStream, database);
        out.writeObject(tech0);
        out.flush();
        byte[] serialized = byteStream.toByteArray();
        out.close();

        ObjectInputStream in = new EObjectInputStream(new ByteArrayInputStream(serialized), database);
        Technology tech = (Technology) in.readObject();
    }
}
