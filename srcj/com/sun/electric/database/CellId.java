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
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.NodeProtoId;
import java.io.ObjectStreamException;

import java.io.Serializable;
import java.util.ArrayList;


/**
 * The CellId class identifies a type of NodeInst independently of threads.
 * It differs from Cell objects, which will be owned by threads in transactional database.
 * This class is thread-safe except inCurrentThread method in 1.5, but not thread-safe in 1.4  .
 */
public final class CellId implements NodeProtoId, Serializable
{
    /** Unique index of this cell in the database. */
    public final int cellIndex;
    /**
     * Usages of other proto subcells in this parent cell.
     * CellUsages are in chronological order by time of their creation.
     */
    private transient volatile CellUsage[] usagesIn = CellUsage.NULL_ARRAY;
    /**
	 * Hash of usagesIn.
	 * The size of nonempty hash is a prime number.
	 * i-th entry of entry search sequence for a given protoId is ((protoId.hashCode() & 0x7FFFFFFF) + i*i) % hashUsagesIn.length .
	 * This first (1 + hashUsagesIn.length/2) entries of this sequence are unique.
	 * Invariant hashUsagesIn.length >= usagesIn.length*2 + 1 guaranties that there is at least one empty entry
	 * in the search sequence. 
	 */
    private transient volatile CellUsage[] hashUsagesIn = EMPTY_HASH;
    
    /** 
     * Usages of this proto cell in other parent cells.
     * CellUsages are in chronological order by time of their creation.
     */
    private transient volatile CellUsage[] usagesOf = CellUsage.NULL_ARRAY;
    
    private transient volatile ExportId[] exportIds = ExportId.NULL_ARRAY;
    
    /**
     * Number of nodeIds returned by newNodeId.
     **/
    private transient volatile int numNodeIds = 0;
    
    /**
     * Number of arcIds returned by newArcId.
     **/
    private transient volatile int numArcIds = 0;
    
    /** List of CellIds created so far. */
    private static final ArrayList<CellId> cellIds = new ArrayList<CellId>();
    /** Empty hash for initialization. */
    private static final CellUsage[] EMPTY_HASH = { null };
    
    /**
     * CellId constructor.
     * Creates CellId with unique cellIndex.
     */
    public CellId() {
        synchronized(cellIds) {
            cellIndex = cellIds.size();
            cellIds.add(this);
        }
    }
    
    /*
     * Resolve method for deserialization.
     */
    private Object readResolve() throws ObjectStreamException {
        return getByIndex(cellIndex);
    }
    
    /**
     * Returns CellId by given index.
     * @param cellIndex given index.
     * @return CellId with given index.
     */
    public static CellId getByIndex(int cellIndex) {
        synchronized(cellIds) {
           while (cellIndex >= cellIds.size())
               new CellId();
           return cellIds.get(cellIndex);
        }
    }
    
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
        // synchronized because exportIds is volatile.
        return exportIds.length;
    }

    /**
     * Returns ExportId in this parent cell with specified chronological index.
     * @param chronIndex chronological index of ExportId.
     * @return ExportId whith specified chronological index.
     * @throws ArrayIndexOutOfBoundsException if no such ExportId.
     */
    public ExportId getExportId(int chronIndex) {
        // synchronized because exportIds is volatile and its entries are final.
        return exportIds[chronIndex];
    }
    
    ExportId getExportIdByChronIndex(int chronIndex) {
        while (chronIndex >= numExportIds())
            newExportId();
        return getExportId(chronIndex);
    }
    
    /**
     * Creates new exportId unique for this parent CellId.
     * @return new exportId.
     */
     public synchronized ExportId newExportId() {
        ExportId[] oldExportIds = exportIds;
        ExportId[] newExportIds = new ExportId[oldExportIds.length + 1];
        System.arraycopy(oldExportIds, 0, newExportIds, 0, oldExportIds.length);
        ExportId e = new ExportId(this, oldExportIds.length);
        newExportIds[oldExportIds.length] = e;
        exportIds = newExportIds;
        return e;
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
     * Method to return the NodeProto representiong NodeProtoId in the current thread.
     * @return the NodeProto representing NodeProtoId in the current thread.
     * This method is not properly synchronized.
     */
    public NodeProto inCurrentThread() { return Cell.inCurrentThread(this); }
    
	/**
	 * Returns a printable version of this CellId.
	 * @return a printable version of this CellId.
	 */
    public String toString() {
        String s = "CellId#" + cellIndex;
        Cell cell = Cell.inCurrentThread(this);
        if (cell != null) s += "(" + cell.libDescribe() + ")";
        return s;
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
        // The hashUsagesIn array is created in "rehash" method inside synchronized block.
        // "rehash" fills some entris leaving null in others.
        // All entries filled in rehash() are final.
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
                rehash();
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
    private void rehash() {
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
	 * Method to check invariants in all Libraries.
	 */
	public static void checkInvariants() {
        int numCellIds;
        synchronized (cellIds) { numCellIds = cellIds.size(); }
        for (int i = 0; i < numCellIds; i++) {
            CellId cellId;
            synchronized (cellIds) { cellId = (CellId)cellIds.get(i); }
            assert cellId.cellIndex == i;
            cellId.check();
        }
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

        CellUsage[] usagesIn = this.usagesIn;
        for (int k = 0; k < usagesIn.length; k++) {
            CellUsage u = usagesIn[k];
            
            // Check that this CellUsage is properly owned and has proper indexInParent.
            // This also guarantees that all elements of usagesIn are distinct.
            assert u.parentId == this;
            assert u.indexInParent == k;
            u.protoId.checkLinked();
            
            // Check the CellUsage invariants.
            // One of them guarantees that all CellUsages
            // in usagesIn have distinct parentIds.
            // It checks also that "u" is in hashUsageIn
            u.check();
        }
        
        // Check that hashUsagesIn contains without duplicates the same set of CellUsages as
        checkHash();
        
        // Check CellUsages in usagesOf array.
        // No need synchronization because usagesOf is volatile field.
        CellUsage[] usagesOf = this.usagesOf; 
        for (int k = 0; k < usagesOf.length; k++) {
            CellUsage u = usagesOf[k];
            
            // Check that this CellUsage is properly owned by its parentId and
            // the parentId is in static list of all CellIds.
            u.parentId.checkLinked();
            assert u == u.parentId.usagesIn[u.indexInParent];
        }
        
        ExportId[] exportIds = this.exportIds;
        for (int k = 0; k < exportIds.length; k++) {
            ExportId e = exportIds[k];
            assert e.parentId == this;
            assert e.chronIndex == k;
        }
    }

    /**
     * Check that usagesIn and hashUsagesIn contains the same number of CellUsages.
     * It checks also that each CellUsage from hashUsagesIn is in usagesIn.
     */
    private void checkHash() {
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
     * Check that this CellId is linked in static list of all CellIds.
     * @throws AssertionError if this CellId is not linked.
     */
    private void checkLinked() {
        synchronized (cellIds) {
            assert this == cellIds.get(cellIndex);
        }
    }
         
}
