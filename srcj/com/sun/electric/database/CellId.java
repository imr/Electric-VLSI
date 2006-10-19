/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellId.java
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

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.prototype.NodeProtoId;
import com.sun.electric.database.text.CellName;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

/**
 * The CellId class identifies a type of NodeInst independently of threads.
 * It differs from Cell objects, which will be owned by threads in transactional database.
 * This class is thread-safe except inCurrentThread method in 1.5, but not thread-safe in 1.4  .
 */
public final class CellId implements NodeProtoId, Serializable {
    /** Empty CellId array for initialization. */
    public static final CellId[] NULL_ARRAY = {};
    
    /** IdManager which owns this LibId. */
    public final IdManager idManager;
    /** LibId which owns this CellId. */
    public final LibId libId;
    /** CellName of this CellId. */
    public final CellName cellName;
    /** Unique index of this cell in the database. */
    public final int cellIndex;
    /**
     * Usages of other proto subcells in this parent cell.
     * CellUsages are in chronological order by time of their creation.
     */
    private volatile CellUsage[] usagesIn = CellUsage.NULL_ARRAY;
    /**
     * Hash of usagesIn.
     * The size of nonempty hash is a prime number.
     * i-th entry of entry search sequence for a given protoId is ((protoId.hashCode() & 0x7FFFFFFF) + i*i) % hashUsagesIn.length .
     * This first (1 + hashUsagesIn.length/2) entries of this sequence are unique.
     * Invariant hashUsagesIn.length >= usagesIn.length*2 + 1 guaranties that there is at least one empty entry
     * in the search sequence.
     */
    private volatile CellUsage[] hashUsagesIn = EMPTY_USAGE_HASH;
    
    /**
     * Usages of this proto cell in other parent cells.
     * CellUsages are in chronological order by time of their creation.
     */
    private volatile CellUsage[] usagesOf = CellUsage.NULL_ARRAY;
    
    /**
     * Number of exportIds allocated so far.
     */
    private volatile int numExportIds = 0;
    /**
     * ExportIds of this cell. ExportIds have unique naems.
     * ExportIds are in cronological order by time of its creation.
     */
    private volatile ExportId[] exportIds = new ExportId[10];
    /**
     * Hash of ExportIds.
     * The size of nonempty hash is a prime number.
     * i-th entry of entry search sequence for a given exportId is ((exportId.hashCode() & 0x7FFFFFFF) + i*i) % hashExportIds.length .
     * This first (1 + hashExportIds.length/2) entries of this sequence are unique.
     * Invariant hashExportIds.length >= exportIds.length*2 + 1 guaranties that there is at least one empty entry
     * in the search sequence.
     */
    private volatile int[] hashExportIds = EMPTY_EXPORT_HASH;
    
    /**
     * Number of nodeIds returned by newNodeId.
     **/
    private volatile int numNodeIds = 0;
    
    /**
     * Number of arcIds returned by newArcId.
     **/
    private volatile int numArcIds = 0;
    
    /** Empty usage hash for initialization. */
    private static final CellUsage[] EMPTY_USAGE_HASH = { null };
    /** Empty usage hash for initialization. */
    private static final int[] EMPTY_EXPORT_HASH = { -1 };
    
    /**
     * CellId constructor.
     */
    CellId(LibId libId, CellName cellName, int cellIndex) {
        if (cellName.getVersion() <= 0)
            throw new IllegalArgumentException("cell version");
        idManager = libId.idManager;
        this.libId = libId;
        this.cellName = cellName;
        this.cellIndex = cellIndex;
    }
    
    private Object writeReplace() throws ObjectStreamException { return new CellIdKey(this); }
    private Object readResolve() throws ObjectStreamException { throw new InvalidObjectException("CellId"); }
    
    private static class CellIdKey extends EObjectInputStream.Key {
        private final int cellIndex;
        
        private CellIdKey(CellId cellId) {
            cellIndex = cellId.cellIndex;
        }
        
        protected Object readResolveInDatabase(EDatabase database) throws InvalidObjectException {
            return database.getIdManager().getCellId(cellIndex);
        }
    }
    
    /**
     * Returns IdManager which is owner of this CellId.
     * @return IdManager which is owner of this CellId.
     */
    public IdManager getIdManager() { return idManager; }
    
    /**
     * Returns a number CellUsages with this CellId as a parent cell.
     * This number may grow in time.
     * @return a number CellUsages with this CellId as a parent cell.
     */
    public int numUsagesIn() {
        // synchronized because usagesIn is volatile.
        return usagesIn.length;
    }
    
    /**
     * Returns the i-th in cronological order CellUsage with this CellId as a parent cell.
     * @param i chronological number of CellUsage.
     * @return i-th CellUsage with this CellId as a parent cell.
     * @throws ArrayIndexOutOfBoundsException if no such CellUsage.
     */
    public CellUsage getUsageIn(int i) {
        // synchronized because usagesIn is volatile and its entries are final.
        return usagesIn[i];
    }
    
    /**
     * Returns a number CellUsages whith this CellId as a proto subcell.
     * This mumber may grow in time.
     * @return a number CellUsages whith this CellId as a proto subcell.
     */
    public int numUsagesOf() {
        // synchronized because usagesOf is volatile.
        return usagesOf.length;
    }
    
    /**
     * Returns the i-th in cronological order CellUsage with this CellId as a proto subcell.
     * @param i chronological number of CellUsage.
     * @return i-th CellUsage with this CellId as a proto subcell.
     * @throws ArrayIndexOutOfBoundsException if no such CellUsage.
     */
    public CellUsage getUsageOf(int i) {
        // synchronized because usagesOf is volatile and its entries are final.
        return usagesOf[i];
    }
    
    /**
     * Returns CellUsage with this CellId as a parent cell and with given
     * CellId as a proto subcell. If this pair of cells is requested at a first time,
     * the new CellUsage is created, otherwise the early created CellUsage retruned.
     * @param protoId CellId of proto subcell.
     * @return CellUsage with this CellId as parent and protoId as a proto subcell.
     * @throws NullPointerException if prootId is null.
     */
    public CellUsage getUsageIn(CellId protoId) { return getUsageIn(protoId, true); }
    
    /**
     * Returns a number ExportIds in this parent cell.
     * This number may grow in time.
     * @return a number of ExportIds.
     */
    public int numExportIds() {
        // synchronized because numExportIds is volatile.
        return numExportIds;
    }
    
    /**
     * Returns ExportId in this parent cell with specified chronological index.
     * @param chronIndex chronological index of ExportId.
     * @return ExportId with specified chronological index.
     * @throws ArrayIndexOutOfBoundsException if no such ExportId.
     */
    public ExportId getPortId(int chronIndex) {
        // synchronized because exportIds is volatile and its entries are final.
        return exportIds[chronIndex];
    }
    
    /**
     * Returns ExportId in this parent cell with specified external id.
     * If this external id was requested earlier, the previously created ExportId returned,
     * otherwise the new ExportId is created.
     * @param externalId external id of ExportId.
     * @return ExportId with specified external id.
     * @throws NullPointerException if externalId is null.
     */
    public ExportId newExportId(String externalId) { return newExportId(externalId, true); }
    
    /**
     * Creates new random exportId, unique in this session for this parent CellId.
     * @param suggestedId suggested external id
     * @return new exportId.
     */
    public ExportId randomExportId(String suggestedId) {
        // Create random id
        String prefix = suggestedId;
        int ind = prefix.indexOf('@');
        if (ind >= 0)
            prefix = prefix.substring(0, ind + 1);
        else
            prefix = prefix + '@';
        Random random = new Random();
        for (;;) {
            int suffix = random.nextInt() & 0x3FFFFFFF;
            String s = prefix + suffix;
            synchronized (this) {
                if (newExportId(s, false) == null) {
                    return newExportId(s, true);
                }
            }
        }
    }
    
    /**
     * Returns new nodeId unique for this CellId.
     * @return new nodeId unique for this CellId.
     */
    public int newNodeId() { return numNodeIds++; }
    
    /**
     * Returns new arcId unique for this CellId.
     * @return new arcId unique for this CellId.
     */
    public int newArcId() { return numArcIds++; }
    
    /**
     * Method to return the Cell representing CellId in the specified EDatabase.
     * @param database EDatabase where to get from.
     * @return the Cell representing CellId in the specified database.
     * This method is not properly synchronized.
     */
    public Cell inDatabase(EDatabase database) { return database.getCell(this); }
    
    /**
     * Returns a printable version of this CellId.
     * @return a printable version of this CellId.
     */
    public String toString() {
        return libId + ":" + cellName.toString();
    }
    
    /**
     * Returns CellUsage with this CellId as a parent cell and with given
     * CellId as a proto subcell. If CellUsage with this pair of cells was already created,
     * it is returned. Otherwise the null is retruned when create=false and new CellUsage is
     * returned if create=true .
     * @param protoId CellId of proto subcell.
     * @return CellUsage with this CellId as parent and protoId as a proto subcell.
     * @throws NullPointerException if protoId is null.
     */
    CellUsage getUsageIn(CellId protoId, boolean create) {
        // The hashUsagesIn array is created in "rehashUsagesIn" method inside synchronized block.
        // "rehash" fills some entris leaving null in others.
        // All entries filled in rehashUsagesIn() are final.
        // However other threads may change initially null entries to non-null value.
        // This non-null value is final.
        // First we scan a sequence of non-null entries out of synchronized block.
        // It is guaranteed that we see the correct values of non-null entries.
        
        // Get poiner to hash array locally once to avoid many reads of volatile variable.
        CellUsage[] hash = hashUsagesIn;
        
        // We shall try to search a sequence of non-null entries for CellUsage with our protoId.
        int i = protoId.hashCode() & 0x7FFFFFFF;
        i %= hash.length;
        for (int j = 1; hash[i] != null; j += 2) {
            CellUsage u = hash[i];
            
            // We scanned a seqence of non-null entries and found the result.
            // It is correct to return it without synchronization.
            if (u.protoId == protoId) return u;
            
            i += j;
            if (i >= hash.length) i -= hash.length;
        }
        
        // Need to enter into the synchronized mode.
        synchronized (CellUsage.class) {
            
            if (hash == hashUsagesIn && hash[i] == null) {
                // There we no rehash during our search and the last null entry is really null.
                // So we can safely use results of unsynchronized search.
                if (!create) return null;
                
                if (usagesIn.length*2 <= hash.length - 3) {
                    // create a new CellUsage, if enough space in the hash
                    CellUsage u = new CellUsage(this, protoId, usagesIn.length);
                    hash[i] = u;
                    usagesIn = appendUsage(usagesIn, u);
                    protoId.usagesOf = appendUsage(protoId.usagesOf, u);
                    check();
                    return u;
                }
                // enlarge hash if not
                rehashUsagesIn();
            }
            // retry in synchronized mode.
            return getUsageIn(protoId, create);
        }
    }
    
    /**
     * Rehash the usagesIn hash.
     * @throws IndexOutOfBoundsException on hash overflow.
     * This method may be called only inside synchronized block.
     */
    private void rehashUsagesIn() {
        CellUsage[] usagesIn = this.usagesIn;
        int newSize = usagesIn.length*2 + 3;
        if (newSize < 0) throw new IndexOutOfBoundsException();
        CellUsage[] newHash = new CellUsage[GenMath.primeSince(newSize)];
        for (int k = usagesIn.length - 1; k >= 0; k--) {
            CellUsage u = usagesIn[k];
            int i = u.protoId.hashCode() & 0x7FFFFFFF;
            i %= newHash.length;
            for (int j = 1; newHash[i] != null; j += 2) {
                i += j;
                if (i >= newHash.length) i -= newHash.length;
            }
            newHash[i] = u;
        }
        hashUsagesIn = newHash;
        check();
    }
    
    /**
     * Returns ExportId in this parent cell with specified external id.
     * If such ExportId doesn't exist returns null.
     * @param externalId external id of ExportId.
     * @return ExportId with specified external id or null.
     * @throws ArrayIndexOutOfBoundsException if no such ExportId.
     */
    private ExportId newExportId(String externalId, boolean create) {
        // The hashExportIds array is created in "rehashExportIds" method inside synchronized block.
        // "rehash" fills some entries leaving -1 in others.
        // All entries filled in rehashExportIds() are final.
        // However other threads may change initially -1 entries to positive value.
        // This positive value is final.
        // First we scan a sequence of positive entries out of synchronized block.
        // It is guaranteed that we see the correct values of positive entries.
        // All entries in exportIds are final
        
        // Get pointer to hash array locally once to avoid many reads of volatile variable.
        int[] hash = hashExportIds;
        ExportId[] exportIds = this.exportIds;
        // We shall try to search a sequence of non-null entries for CellUsage with our protoId.
        int i = externalId.hashCode() & 0x7FFFFFFF;
        i %= hash.length;
        try {
            for (int j = 1; hash[i] >= 0; j += 2) {
                ExportId exportId = exportIds[hash[i]];
                if (exportId.externalId.equals(externalId)) return exportId;
                i += j;
                if (i >= hash.length) i -= hash.length;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // This may happen if some hash entries were concurrently filled with index out of ExportId range.
        }
        synchronized (this) {
            if (hash == hashExportIds && hash[i] == -1) {
                // There we no rehash during our search and the last -1 entry is really -1.
                // So we are sure that there is not ExportId with specified external id.
                if (!create) return null;
                
                int chronIndex = numExportIds;
                exportIds = this.exportIds;
                if (chronIndex*2 <= hash.length - 3) {
                    ExportId e = new ExportId(this, chronIndex, externalId);
                    hash[i] = chronIndex;
                    if (chronIndex >= exportIds.length) {
                        assert chronIndex == exportIds.length;
                        int newLength = (int)Math.min(exportIds.length*3L/2 + 1, Integer.MAX_VALUE);
                        ExportId[] newExportIds = new ExportId[newLength];
                        System.arraycopy(exportIds, 0, newExportIds, 0, chronIndex);
                        exportIds = newExportIds;
                    }
                    exportIds[chronIndex] = e;
                    this.exportIds = exportIds;
                    hashExportIds = hash;
                    numExportIds = chronIndex + 1;
                    return e;
                }
                // enlarge hash if not
                rehashExportIds(exportIds, numExportIds);
            }
            // retry in synchronized mode.
            return newExportId(externalId, create);
        }
    }
    
    /**
     * Rehash the exportIds hash.
     * @throws IndexOutOfBoundsException on hash overflow.
     * This method may be called only inside synchronized block.
     */
    private void rehashExportIds(ExportId[] exportIds, int numExports) {
        int newSize = exportIds.length*2 + 3;
        if (newSize < 0) throw new IndexOutOfBoundsException();
        int[] newHash = new int[GenMath.primeSince(newSize)];
        Arrays.fill(newHash, -1);
        for (int k = 0; k < numExports; k++) {
            ExportId exportId = exportIds[k];
            int i = exportId.hashCode() & 0x7FFFFFFF;
            i %= newHash.length;
            for (int j = 1; newHash[i] >= 0; j += 2) {
                assert exportIds[newHash[i]] != exportId;
                i += j;
                if (i >= newHash.length) i -= newHash.length;
            }
            newHash[i] = k;
        }
        hashExportIds = newHash;
        check();
    }
    
    /**
     * Append usage to the end of array.
     * @param usages array of usages.
     * @param newUsage new CellUsage.
     * @return array with new usage appended.
     */
    private static CellUsage[] appendUsage(CellUsage[] usages, CellUsage newUsage) {
        CellUsage[] newUsages = new CellUsage[usages.length + 1];
        System.arraycopy(usages, 0, newUsages, 0, usages.length);
        newUsages[usages.length] = newUsage;
        return newUsages;
    }
    
    /**
     * Checks invariants in this CellId.
     * ALL i: usagesIn[i].parentId == this;
     * ALL i: usagesIn[i].indexInParent == i;
     * ALL i, j: i != j IMPLIES usagesIn[i].protoId != usagesIn[i].protoId;
     *
     * ALL i: usagesOf[i].protoId == this;
     * ALL i, j: i != j IMPLIES usagesOf[i].parentId != usagesIn[j].parentId;
     *
     * hashUsagesIn[*] is a correct hash map CellUsage.protoId -> CellUsage;
     * hashUsagesIn[*] and usagesIn[*] are equal sets of CellUsages;
     *
     * ALL i: exportIds[i].parentId == this;
     * ALL i: exportIds[i].chronIndex == i;
     * This CellId, all usagesIn[*].protoId and all usagesOf[*].parentId are linked in
     *    static list of all CellIds;
     *
     * @exception AssertionError if invariants are not valid
     */
    void check() {
        checkLinked();
        assert idManager == libId.idManager;
        cellName.check();
        assert cellName.getVersion() > 0;
        assert libId.getCellId(cellName) == this;
        
        CellUsage[] usagesIn = this.usagesIn;
        for (int k = 0; k < usagesIn.length; k++) {
            CellUsage u = usagesIn[k];
            
            // Check that this CellUsage is properly owned and has proper indexInParent.
            // This also guarantees that all elements of usagesIn are distinct.
            assert u.parentId == this;
            assert u.indexInParent == k;
            assert u.parentId.getIdManager() == getIdManager();
            u.protoId.checkLinked();
            
            // Check the CellUsage invariants.
            // One of them guarantees that all CellUsages
            // in usagesIn have distinct parentIds.
            // It checks also that "u" is in hashUsageIn
            u.check();
        }
        
        // Check that hashUsagesIn contains without duplicates the same set of CellUsages as
        checkHashUsagesIn();
        // Check that hashExportIds has exportIds.length entries
        checkHashExportIds();
        
        // Check CellUsages in usagesOf array.
        // No need synchronization because usagesOf is volatile field.
        CellUsage[] usagesOf = this.usagesOf;
        for (int k = 0; k < usagesOf.length; k++) {
            CellUsage u = usagesOf[k];
            
            // Check that this CellUsage is properly owned by its parentId and
            // the parentId is in static list of all CellIds.
            assert u.protoId.getIdManager() == getIdManager();
            u.parentId.checkLinked();
            assert u == u.parentId.usagesIn[u.indexInParent];
        }
        
        int numExportIds = this.numExportIds;
        for (int chronIndex = 0; chronIndex < numExportIds; chronIndex++) {
            ExportId e = exportIds[chronIndex];
            assert e.parentId == this;
            assert e.chronIndex == chronIndex;
            assert newExportId(e.externalId, false) == e;
            e.check();
        }
    }
    
    /**
     * Check that usagesIn and hashUsagesIn contains the same number of CellUsages.
     * It checks also that each CellUsage from hashUsagesIn is in usagesIn.
     */
    private void checkHashUsagesIn() {
        CellUsage[] usagesIn;
        CellUsage[] hash;
        synchronized (CellUsage.class) {
            usagesIn = this.usagesIn;
            hash = this.hashUsagesIn;
        }
        int count = 0;
        for (int i = 0; i < hash.length; i++) {
            CellUsage u = hash[i];
            if (u == null) continue;
            assert u.parentId == this;
            assert u == usagesIn[u.indexInParent];
            count++;
        }
        assert usagesIn.length == count;
    }
    
    /**
     * Check that exportIds and hashExportIds contains the same number of ExportIds.
     */
    private void checkHashExportIds() {
        ExportId[] exportIds;
        int[] hash;
        synchronized (this) {
            exportIds = this.exportIds;
            hash = this.hashExportIds;
        }
        int count = 0;
        for (int i = 0; i < hash.length; i++) {
            int k = hash[i];
            if (k == -1) continue;
            assert k >= 0 && k < exportIds.length;
            count++;
        }
        assert numExportIds == count;
    }
    
    /**
     * Check that this CellId is linked in static list of all CellIds.
     * @throws AssertionError if this CellId is not linked.
     */
    private void checkLinked() {
        assert this == getIdManager().getCellId(cellIndex);
    }
    
    public static void main(String[] args) {
        IdManager idManager = new IdManager();
        LibId libId = idManager.newLibId("lib");
        CellName cellName = CellName.parseName("cell;1{sch}");
        CellId cellId = libId.newCellId(cellName);
        cellId.hashExportIds = new int[8];
        Arrays.fill(cellId.hashExportIds, -1);
        cellId.newExportId("A");
        String s = "B";
        cellId.newExportId(s);
        
        long startTime = System.currentTimeMillis();
        int numTries = 100*1000*1000;
        int k = 0;
        for (int i = 0; i < numTries; i++) {
            ExportId eId = cellId.newExportId(s);
            k += eId.chronIndex;
        }
        long stopTime = System.currentTimeMillis();
        System.out.println("k=" + k + " t=" + (stopTime - startTime));
    }
}
