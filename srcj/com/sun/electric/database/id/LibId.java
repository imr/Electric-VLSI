/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LibId.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
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
package com.sun.electric.database.id;

import com.sun.electric.database.EObjectInputStream;
import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.CellName;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The LibId immutable class identifies library independently of threads.
 * It differs from Library objects, which will be owned by threads in transactional database.
 * This class is thread-safe except inCurrentThread method.
 */
public final class LibId implements Serializable {

    /** Empty LibId array for initialization. */
    public static final LibId[] NULL_ARRAY = {};
    /** IdManager which owns this LibId. */
    public final IdManager idManager;
    /** Library name */
    public final String libName;
    /** Unique index of this lib in the database. */
    public final int libIndex;
    /** HashMap of CellIds in this library by their cell name. */
    private final HashMap<CellName, CellId> cellIdsByCellName = new HashMap<CellName, CellId>();

    /**
     * LibId constructor.
     */
    LibId(IdManager idManager, String libName, int libIndex) {
        if (libName == null) {
            throw new NullPointerException();
        }
        if (legalLibraryName(libName) != libName) {
            throw new IllegalArgumentException(libName);
        }
        this.idManager = idManager;
        this.libName = libName;
        this.libIndex = libIndex;
    }

    private Object writeReplace() {
        return new LibIdKey(this);
    }

    private static class LibIdKey extends EObjectInputStream.Key<LibId> {

        public LibIdKey() {
        }

        private LibIdKey(LibId libId) {
            super(libId);
        }

        @Override
        public void writeExternal(EObjectOutputStream out, LibId libId) throws IOException {
            if (libId.idManager != out.getIdManager()) {
                throw new NotSerializableException(libId + " from other IdManager");
            }
            out.writeInt(libId.libIndex);
        }

        @Override
        public LibId readExternal(EObjectInputStream in) throws IOException, ClassNotFoundException {
            int libIndex = in.readInt();
            return in.getIdManager().getLibId(libIndex);
        }
    }

    /**
     * Returns new CellId with cellIndex unique in this IdManager.
     * @return new CellId.
     */
    public CellId newCellId(CellName cellName) {
        return idManager.newCellId(this, cellName);
    }

    /**
     * Method to return the Library representing LibId in the specified EDatabase.
     * @param database EDatabase where to get from.
     * @return the Library representing LibId in the specified database.
     * This method is not properly synchronized.
     */
    public Library inDatabase(EDatabase database) {
        return database.getLib(this);
    }

    /**
     * Returns a printable version of this LibId.
     * @return a printable version of this LibId.
     */
    public String toString() {
        return libName;
    }

    CellId getCellId(CellName cellName) {
        return cellIdsByCellName.get(cellName);
    }

    void putCellId(CellId cellId) {
        cellIdsByCellName.put(cellId.cellName, cellId);
    }

    /**
     * Checks invariants in this LibId.
     * @exception AssertionError if invariants are not valid
     */
    void check() {
        assert this == idManager.getLibId(libIndex);
        assert libName != null && legalLibraryName(libName) == libName;
        for (Map.Entry<CellName, CellId> e : cellIdsByCellName.entrySet()) {
            CellId cellId = e.getValue();
            assert cellId.libId == this;
            assert cellId.cellName == e.getKey();
            assert idManager.getCellId(cellId.cellIndex) == cellId;
        }
    }

    /**
     * Checks that string is legal library name.
     * If not, tries to convert string to legal library name.
     * @param libName specified library name.
     * @return legal library name obtained from specified library name,
     *   or null if speicifed name is null or empty.
     */
    public static String legalLibraryName(String libName) {
        if (libName == null || libName.length() == 0) {
            return null;
        }
        int i = 0;
        for (; i < libName.length(); i++) {
            char ch = libName.charAt(i);
            if (Character.isWhitespace(ch) || ch == ':') {
                break;
            }
        }
        if (i == libName.length()) {
            return libName;
        }

        char[] chars = libName.toCharArray();
        for (; i < libName.length(); i++) {
            char ch = chars[i];
            chars[i] = Character.isWhitespace(ch) || ch == ':' ? '-' : ch;
        }
        return new String(chars);
    }
}
