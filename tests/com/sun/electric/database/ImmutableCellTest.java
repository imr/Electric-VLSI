/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableCellTest.java
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

import com.sun.electric.database.text.CellName;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class ImmutableCellTest {
    
    private IdManager idManager;
    private LibId libId;
    private CellId cellId;
    private CellName fooName;
    private CellName groupName;
    private ImmutableCell d;
    private Variable var;
    
    @Before public void setUp() {
        idManager = new IdManager();
        libId = idManager.newLibId("libId0");
        fooName = CellName.parseName("foo;1{lay}");
        cellId = libId.newCellId(fooName);
        groupName = CellName.parseName("foo{sch}");
        d = ImmutableCell.newInstance(cellId, 12345).withGroupName(groupName).withTech(Generic.tech);
        var = Variable.newInstance(Variable.newKey("A"), "foo", TextDescriptor.EMPTY.withParam(true) );
    }
    
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(ImmutableCellTest.class);
    }

    /**
     * Test of newInstance method, of class com.sun.electric.database.ImmutableCell.
     */
    @Test(expected= NullPointerException.class) public void testNewInstance() {
        System.out.println("newInstance");
        
        long creationDate = 0L;
        ImmutableCell.newInstance(null, creationDate).check();
    }

    /**
     * Test of withGroupName method, of class com.sun.electric.database.ImmutableCell.
     */
    @Test public void testWithGroupName() {
        System.out.println("withGroupName");
        
        assertSame(d, d.withGroupName(CellName.parseName("foo{sch}")));
        
        CellName groupName = CellName.parseName("bar{sch}");
        ImmutableCell d1 = d.withGroupName(groupName);
        d1.check();
        assertSame(d.cellId, d1.cellId);
        assertSame(groupName, d1.groupName);
        assertEquals(d.creationDate, d1.creationDate);
        assertSame(d.tech, d1.tech);
        assertSame(d.getVars(), d1.getVars());
        assertEquals(d.flags, d1.flags);
    }

    /**
     * Test of withGroupName method, of class com.sun.electric.database.ImmutableCell.
     */
    @Test(expected= NullPointerException.class) public void testWithGroupNameNull() {
        System.out.println("withGroupNameNull");
      
        d.withGroupName(CellName.parseName(null));
    }

    /**
     * Test of withGroupName method, of class com.sun.electric.database.ImmutableCell.
     */
    @Test(expected= IllegalArgumentException.class) public void testWithGroupNameBad() {
        System.out.println("withGroupNameBad");
      
        d.withGroupName(CellName.parseName("foo{ic}"));
    }

    /**
     * Test of withCreationDate method, of class com.sun.electric.database.ImmutableCell.
     */
    @Test public void testWithCreationDate() {
        System.out.println("withCreationDate");
        
        assertSame(d, d.withCreationDate(12345) );
        
        long creationDate = 2006L;
        ImmutableCell d1 = d.withCreationDate(creationDate);
        d1.check();
        assertSame(d.cellId, d1.cellId);
        assertSame(d.groupName, d1.groupName);
        assertEquals(creationDate, d1.creationDate);
        assertSame(d.tech, d1.tech);
        assertSame(d.getVars(), d1.getVars());
        assertEquals(d.flags, d1.flags);
    }

    /**
     * Test of withTech method, of class com.sun.electric.database.ImmutableCell.
     */
    @Test public void testWithTech() {
        System.out.println("withTech");
        
        assertSame(d, d.withTech(Generic.tech));
        
        Technology tech = Schematics.tech;
        ImmutableCell d1 = d.withTech(tech);
        d1.check();
        assertSame(d.cellId, d1.cellId);
        assertSame(d.groupName, d1.groupName);
        assertEquals(d.creationDate, d1.creationDate);
        assertSame(tech, d1.tech);
        assertSame(d.getVars(), d1.getVars());
        assertEquals(d.flags, d1.flags);
        
        assertNull( d.withTech(null).tech );
    }

    /**
     * Test of withFlags method, of class com.sun.electric.database.ImmutableCell.
     */
    @Test public void testWithFlags() {
        System.out.println("withFlags");

        assertSame(d, d.withFlags(0));
        
        int flags = 1;
        ImmutableCell d1 = d.withFlags(flags);
        d1.check();
        assertSame(d.cellId, d1.cellId);
        assertSame(d.groupName, d1.groupName);
        assertEquals(d.creationDate, d1.creationDate);
        assertSame(d.tech, d1.tech);
        assertSame(d.getVars(), d1.getVars());
        assertEquals(flags, d1.flags);
    }

    /**
     * Test of withVariable method, of class com.sun.electric.database.ImmutableCell.
     */
    @Test public void testWithVariable() {
        System.out.println("withVariable");
        
        ImmutableCell d1 = d.withVariable(var);
        d1.check();
        assertSame(d.cellId, d1.cellId);
        assertSame(d.groupName, d1.groupName);
        assertEquals(d.creationDate, d1.creationDate);
        assertSame(d.tech, d1.tech);
        assertEquals(d.flags, d1.flags);
        
        Variable[] vars = d1.getVars();
        assertEquals(1, vars.length);
        assertSame(var, vars[0]);
    }

    /**
     * Test of withoutVariable method, of class com.sun.electric.database.ImmutableCell.
     */
    @Test public void testWithoutVariable() {
        System.out.println("withoutVariable");
        
        assertSame( d, d.withoutVariable(Variable.newKey("A")) );
    }

    /**
     * Test of withoutVariables method, of class com.sun.electric.database.ImmutableCell.
     */
    @Test public void testWithoutVariables() {
        System.out.println("withoutVariables");
        
        assertSame( d, d.withoutVariables() );
    }

    /**
     * Test of writeDiffs and readDiff method, of class com.sun.electric.database.ImmutableCell.
     */
    @Test public void testReadWrite() {
        System.out.println("readReadWrite");
        
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            SnapshotWriter writer = new SnapshotWriter(new DataOutputStream(byteStream));
            d.write(writer);
            writer.flush();
            byte[] bytes = byteStream.toByteArray();
            byteStream.reset();
            
            // First update of mirrorIdManager
            SnapshotReader reader = new SnapshotReader(new DataInputStream(new ByteArrayInputStream(bytes)), idManager);
            
            // Check mirrorIdManager after first update
            ImmutableCell d1 = ImmutableCell.read(reader);
            d1.check();
            assertSame(d.cellId, d1.cellId);
            assertEquals(d.groupName, d1.groupName);
            assertEquals(d.creationDate, d1.creationDate);
            assertSame(d.tech, d1.tech);
            assertEquals(d.flags, d1.flags);
            assertTrue( Arrays.equals(d.getVars(), d1.getVars()) );
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
    
    /**
     * Test of hashCodeExceptVariables method, of class com.sun.electric.database.ImmutableCell.
     */
    @Test public void testHashCodeExceptVariables() {
        System.out.println("hashCodeExceptVariables");
        
        assertEquals( cellId.hashCode(), d.hashCodeExceptVariables() );
    }

    /**
     * Test of equalsExceptVariables method, of class com.sun.electric.database.ImmutableCell.
     */
    @Test public void testEqualsExceptVariables() {
        System.out.println("equalsExceptVariables");
        
        assertTrue( d.equalsExceptVariables(d) );
        assertFalse( d.equalsExceptVariables(d.withGroupName(CellName.parseName("bar{sch}"))) );
        assertFalse( d.equalsExceptVariables(d.withCreationDate(1)) );
        assertFalse( d.equalsExceptVariables(d.withTech(Schematics.tech)) );
        assertFalse( d.equalsExceptVariables(d.withFlags(1)) );
        assertTrue( d.equalsExceptVariables(d.withVariable(var)) );
    }

    /**
     * Test of check method, of class com.sun.electric.database.ImmutableCell.
     */
    public void testCheck() {
        System.out.println("check");
        
        ImmutableCell instance = null;
        
        instance.check();
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}
