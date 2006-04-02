/*
 * SnapshotTest.java
 * JUnit based test
 *
 * Created on March 20, 2006, 4:29 PM
 */

package com.sun.electric.database;

import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
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
 *
 * @author dn146861
 */
public class SnapshotTest extends TestCase {
    
    private static byte[] emptyDiffEmpty;
    
    public SnapshotTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        CellId.restart();
        LibId.restart();
        Snapshot.restart();
        
        // Preload technology
        Technology tech = Schematics.tech;
        
        // Init emptyDiffEmpty
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SnapshotWriter writer = new SnapshotWriter(new DataOutputStream(out));
        Snapshot oldSnapshot = Snapshot.EMPTY;
        Snapshot instance = Snapshot.EMPTY;
        try {
            Snapshot.EMPTY.writeDiffs(writer, Snapshot.EMPTY);
            writer.out.close();
            emptyDiffEmpty = out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {}
    
    protected void tearDown() throws Exception {
    }

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
        
        LibId libId = new LibId();
        ImmutableLibrary l = ImmutableLibrary.newInstance(libId, "lib", null, null);
        LibraryBackup libBackup = new LibraryBackup(l, false, new LibId[]{});
        CellId cellId = new CellId();
        CellName cellName = CellName.parseName("cell;1{sch}");
        ImmutableCell c = ImmutableCell.newInstance(cellId, libId, cellName, 0).withTech(Schematics.tech);
        CellBackup cellBackup = new CellBackup(c);
        
        CellBackup[] cellBackupsArray = { cellBackup };
        int[] cellGroups = { 0 }; 
        ERectangle emptyBound = new ERectangle(0, 0, 0, 0);
        ERectangle[] cellBoundsArray = { emptyBound };
        LibraryBackup[] libBackupsArray = { libBackup };
        Snapshot instance = Snapshot.EMPTY;
        
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
        
        Snapshot oldSnapshot = Snapshot.EMPTY;
        Snapshot instance = Snapshot.EMPTY;
        
        List<LibId> expResult = Collections.emptyList();
        List<LibId> result = instance.getChangedLibraries(oldSnapshot);
        assertEquals(expResult, result);
    }

    /**
     * Test of getChangedCells method, of class com.sun.electric.database.Snapshot.
     */
    public void testGetChangedCells() {
        System.out.println("getChangedCells");
        
        Snapshot oldSnapshot = Snapshot.EMPTY;
        Snapshot instance = Snapshot.EMPTY;
        
        List<CellId> expResult = Collections.emptyList();
        List<CellId> result = instance.getChangedCells(oldSnapshot);
        assertEquals(expResult, result);
    }

    /**
     * Test of getCell method, of class com.sun.electric.database.Snapshot.
     */
    public void testGetCell() {
        System.out.println("getCell");
        
        CellId cellId = CellId.getByIndex(0);
        Snapshot instance = Snapshot.EMPTY;
        
        CellBackup expResult = null;
        CellBackup result = instance.getCell(cellId);
        assertEquals(expResult, result);
    }

    /**
     * Test of getCellBounds method, of class com.sun.electric.database.Snapshot.
     */
    public void testGetCellBounds() {
        System.out.println("getCellBounds");
        
        CellId cellId = CellId.getByIndex(0);
        Snapshot instance = Snapshot.EMPTY;
        
        ERectangle expResult = null;
        ERectangle result = instance.getCellBounds(cellId);
        assertEquals(expResult, result);
    }

    /**
     * Test of getLib method, of class com.sun.electric.database.Snapshot.
     */
    public void testGetLib() {
        System.out.println("getLib");
        
        LibId libId = LibId.getByIndex(0);
        Snapshot instance = Snapshot.EMPTY;
        
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
        Snapshot oldSnapshot = Snapshot.EMPTY;
        Snapshot instance = Snapshot.EMPTY;
        
        instance.writeDiffs(writer, oldSnapshot);
        assertTrue(Arrays.equals(out.toByteArray(), emptyDiffEmpty));
    }

    /**
     * Test of readSnapshot method, of class com.sun.electric.database.Snapshot.
     */
    public void testReadSnapshot() throws Exception {
        System.out.println("readSnapshot");
        
        SnapshotReader reader = new SnapshotReader(new DataInputStream(new ByteArrayInputStream(emptyDiffEmpty)));
        Snapshot oldSnapshot = Snapshot.EMPTY;
        
        ImmutableArrayList<CellBackup> expCellBackups = new ImmutableArrayList<CellBackup>(CellBackup.NULL_ARRAY);
        Snapshot result = Snapshot.readSnapshot(reader, oldSnapshot);
        assertEquals(expCellBackups, result.cellBackups);
    }

    /**
     * Test of check method, of class com.sun.electric.database.Snapshot.
     */
    public void testCheck() {
        System.out.println("check");
        
        Snapshot instance = Snapshot.EMPTY;
        
        instance.check();
    }
}
