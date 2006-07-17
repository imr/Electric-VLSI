/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableElectrciObjectTest.java
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

import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import java.util.Iterator;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 */
public class ImmutableElectricObjectTest {
    
    private final ImmutableElectricObject obj0 = new ImmutableElectricObjectImpl(Variable.NULL_ARRAY);
    private final TextDescriptor td = TextDescriptor.EMPTY;
    private final String foo = "foo";
    private final Variable A_F = Variable.newInstance(Variable.newKey("A"), Boolean.FALSE, td);
    private final Variable A_foo = Variable.newInstance(Variable.newKey("A"), foo, td);
    private final Variable B_F = Variable.newInstance(Variable.newKey("B"), Boolean.FALSE, td);
    private final Variable a_foo = Variable.newInstance(Variable.newKey("a"), foo, td.withParam(true));
    private final ImmutableElectricObject obj_a = new ImmutableElectricObjectImpl(new Variable[] { a_foo });
    private final Variable[] Aa = new Variable[] { A_F, a_foo };
    private final ImmutableElectricObject obj_Aa = new ImmutableElectricObjectImpl(Aa);
    
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(ImmutableElectricObjectTest.class);
    }

    /**
     * Test of arrayWithVariable method, of class com.sun.electric.database.ImmutableElectricObject.
     */
    @Test public void testArrayWithVariable() {
        System.out.println("arrayWithVariable");

        assertSame(obj_a.getVars(), obj_a.arrayWithVariable(a_foo));
        
        Variable[] vars2 = obj_a.arrayWithVariable(A_F);
        assertEquals(2, vars2.length);
        assertSame(A_F, vars2[0]);
        assertSame(a_foo, vars2[1]);
        
        Variable[] vars3 = obj_Aa.arrayWithVariable(B_F);
        assertEquals(3, vars3.length);
        assertSame(A_F, vars3[0]);
        assertSame(B_F, vars3[1]);
        assertSame(a_foo, vars3[2]);
    }

    /**
     * Test of arrayWithoutVariable method, of class com.sun.electric.database.ImmutableElectricObject.
     */
    @Test public void testArrayWithoutVariable() {
        System.out.println("arrayWithoutVariable");
        
        assertSame(obj_a.getVars(), obj_a.arrayWithoutVariable(Variable.newKey("A")));
        assertSame(Variable.NULL_ARRAY, obj_a.arrayWithoutVariable(Variable.newKey("a")));
        
        Variable[] vars1 = obj_Aa.arrayWithoutVariable(Variable.newKey("A"));
        assertEquals(1, vars1.length);
        assertSame(a_foo, vars1[0]);
    }

    /**
     * Test of arrayWithRenamedIds method, of class com.sun.electric.database.ImmutableElectricObject.
     */
    @Test public void testArrayWithRenamedIds() {
        System.out.println("arrayWithRenamedIds");
        
        IdMapper idMapper = new IdMapper();
        
        assertSame(obj_Aa.getVars(), obj_Aa.arrayWithRenamedIds(idMapper));
    }

    /**
     * Test of getVar method, of class com.sun.electric.database.ImmutableElectricObject.
     */
    @Test public void testGetVar() {
        System.out.println("getVar");
        
        assertNull( obj_Aa.getVar(Variable.newKey("B")) );
        assertSame( A_F, obj_Aa.getVar(Variable.newKey("A")));
        assertSame( a_foo, obj_Aa.getVar(Variable.newKey("a")));
    }

    /**
     * Test of getVariables method, of class com.sun.electric.database.ImmutableElectricObject.
     */
    @Test public void testGetVariables() {
        System.out.println("getVariables");
        
        Iterator<Variable> it0 = obj0.getVariables();
        assertTrue( !it0.hasNext() );
        
        Iterator<Variable> it = obj_Aa.getVariables();
        assertSame( A_F, it.next() );
        assertSame( a_foo, it.next() );
        assertTrue( !it.hasNext() );
    }

    /**
     * Test of getVariables method, of class com.sun.electric.database.ImmutableElectricObject.
     */
    @Test(expected= UnsupportedOperationException.class) public void testGetVariablesRemove() {
        System.out.println("getVariablesRemove");
        
        Iterator<Variable> it = obj_Aa.getVariables();
        assertSame( A_F, it.next() );
        it.remove();
    }

    /**
     * Test of toVariableArray method, of class com.sun.electric.database.ImmutableElectricObject.
     */
    @Test public void testToVariableArray() {
        System.out.println("toVariableArray");
 
        Variable[] vars2 = obj_Aa.toVariableArray();
        assertNotSame(Aa, vars2);
        
        assertEquals(2, vars2.length);
        assertSame(Aa[0], vars2[0]);
        assertSame(Aa[1], vars2[1]);
    }

    /**
     * Test of getNumVariables method, of class com.sun.electric.database.ImmutableElectricObject.
     */
    @Test public void testGetNumVariables() {
        System.out.println("getNumVariables");

        assertEquals(0, obj0.getNumVariables());
        assertEquals(1, obj_a.getNumVariables());
        assertEquals(2, obj_Aa.getNumVariables());
    }

    /**
     * Test of getVars method, of class com.sun.electric.database.ImmutableElectricObject.
     */
    @Test public void testGetVars() {
        System.out.println("getVars");
        
        assertSame(Aa, obj_Aa.getVars());
    }

    /**
     * Test of searchVar method, of class com.sun.electric.database.ImmutableElectricObject.
     */
    @Test public void testSearchVar() {
        System.out.println("searchVar");
        
        assertEquals(0, obj_Aa.searchVar(Variable.newKey("A")));
        assertEquals(1, obj_Aa.searchVar(Variable.newKey("a")));
        assertEquals(-1, obj_Aa.searchVar(Variable.newKey("0")));
        assertEquals(-2, obj_Aa.searchVar(Variable.newKey("B")));
        assertEquals(-3, obj_Aa.searchVar(Variable.newKey("s")));
    }

    /**
     * Test of searchVar method, of class com.sun.electric.database.ImmutableElectricObject.
     */
    @Test(expected= NullPointerException.class) public void testSearchVarNull() {
        System.out.println("searchVar");
        
        obj0.searchVar(null);
    }

    /**
     * Test of writeDiffs and readDiff method, of class com.sun.electric.database.ImmutableElectricObject.
     */
    @Test public void testReadWrite() {
        System.out.println("readWrite");
        
        try {
            IdManager idManager = new IdManager();
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            SnapshotWriter writer = new SnapshotWriter(new DataOutputStream(byteStream));
            obj0.write(writer);
            obj0.writeVars(writer);
            obj_Aa.write(writer);
            obj_Aa.writeVars(writer);
            writer.flush();
            byte[] bytes = byteStream.toByteArray();
            byteStream.reset();
            
            // First update of mirrorIdManager
            SnapshotReader reader = new SnapshotReader(new DataInputStream(new ByteArrayInputStream(bytes)), idManager);
            
            // Check mirrorIdManager after first update
            assertEquals( false, reader.readBoolean() );
            assertSame( 0, ImmutableElectricObject.readVars(reader).length );
            assertEquals( true, reader.readBoolean() );
            Variable[] vars2 = ImmutableElectricObject.readVars(reader);
            assertTrue( Arrays.equals(Aa, vars2) );
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
    
    /**
     * Test of check method, of class com.sun.electric.database.ImmutableElectricObject.
     */
    @Test public void testCheck() {
        System.out.println("check");
        
        obj_Aa.check(true);
    }

    /**
     * Test of check method, of class com.sun.electric.database.ImmutableElectricObject.
     */
    @Test(expected=AssertionError.class) public void testCheckFailed() {
        System.out.println("checkFailed");
        
        obj_Aa.check(false);
    }

    /**
     * Generated implementation of abstract class com.sun.electric.database.ImmutableElectricObject. Please fill dummy bodies of generated methods.
     */
    private class ImmutableElectricObjectImpl extends ImmutableElectricObject {
        ImmutableElectricObjectImpl(Variable[] vars) { super(vars, 0); }
        public int hashCodeExceptVariables() { return 0; }
        public boolean equalsExceptVariables(ImmutableElectricObject o) { return false; }
    }
}
