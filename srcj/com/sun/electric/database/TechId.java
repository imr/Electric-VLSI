/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TechId.java
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

import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.technology.Technology;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * The TechId immutable class identifies technology independently of threads.
 * It differs from Technology objects, which will be owned by threads in transactional database.
 * This class is thread-safe except inCurrentThread method.
 */
public final class TechId implements Serializable {
    /** Empty TechId array for initialization. */
    public static final TechId[] NULL_ARRAY = {};
    
    /** IdManager which owns this TechId. */
    public final IdManager idManager;
    /** Technology name */
    public final String techName;
    /** Unique index of this TechId. */
    public final int techIndex;
//    /** HashMap of CellIds in this library by their cell name. */
//    private final HashMap<CellName,CellId> cellIdsByCellName = new HashMap<CellName,CellId>();
    
    /**
     * TechId constructor.
     */
    TechId(IdManager idManager, String techName, int techIndex) {
        if (techName == null)
            throw new NullPointerException();
        if (legalTechnologyName(techName) != techName)
            throw new IllegalArgumentException(techName);
        this.idManager = idManager;
        this.techName = techName;
        this.techIndex = techIndex;
    }
    
    private Object writeReplace() { return new TechIdKey(this); }
    private Object readResolve() throws ObjectStreamException { throw new InvalidObjectException("TechId"); }
    
    private static class TechIdKey extends EObjectInputStream.Key {
        private final int techIndex;
        
        private TechIdKey(TechId techId) {
            techIndex = techId.techIndex;
        }
        
        protected Object readResolveInDatabase(EDatabase database) throws InvalidObjectException {
            return database.getIdManager().getTechId(techIndex);
        }
    }
         
//    /**
//     * Returns new CellId with cellIndex unique in this IdManager.
//     * @return new CellId.
//     */
//    public CellId newCellId(CellName cellName) {
//        return idManager.newCellId(this, cellName);
//    }
    
    /**
     * Method to return the Technology representing TechId in the specified EDatabase.
     * @param database EDatabase where to get from.
     * @return the Technology representing TechId in the specified database.
     * This method is not properly synchronized.
     */
    public Technology inDatabase(EDatabase database) { return database.getTech(this); }
    
	/**
	 * Returns a printable version of this LibId.
	 * @return a printable version of this LibId.
	 */
    public String toString() { return techName; }
    
//    CellId getCellId(CellName cellName) { return cellIdsByCellName.get(cellName); }
//    void putCellId(CellId cellId) { cellIdsByCellName.put(cellId.cellName, cellId); }
    
	/**
	 * Checks invariants in this LibId.
     * @exception AssertionError if invariants are not valid
	 */
    void check() {
        assert this == idManager.getTechId(techIndex);
        assert techName != null && legalTechnologyName(techName) == techName;
//        for (Map.Entry<CellName,CellId> e: cellIdsByCellName.entrySet()) {
//            CellId cellId = e.getValue();
//            assert cellId.libId == this;
//            assert cellId.cellName == e.getKey();
//            assert idManager.getCellId(cellId.cellIndex) == cellId;
//        }
    }

    
    /**
     * Checks that string is legal technology name.
     * If not, tries to convert string to legal technology name.
     * @param techName specified library name.
     * @return legal technology name obtained from specified technology name,
     *   or null if speicifed name is null or empty.
     */
    public static String legalTechnologyName(String techName) {
        if (techName == null || techName.length() == 0) return null;
        int i = 0;
        for (; i < techName.length(); i++) {
            char ch = techName.charAt(i);
            if (Character.isWhitespace(ch) || ch == ':') break;
        }
        if (i == techName.length()) return techName;
        
        char[] chars = techName.toCharArray();
        for (; i < techName.length(); i++) {
            char ch = chars[i];
            chars[i] = Character.isWhitespace(ch) || ch == ':' ? '-' : ch;
        }
        return new String(chars);
    }
}
