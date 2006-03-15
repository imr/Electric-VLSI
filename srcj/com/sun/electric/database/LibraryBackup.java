/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LibraryBackup.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.text.ImmutableArrayList;
import java.io.IOException;

/**
 *
 */
public class LibraryBackup {
    public static final LibraryBackup[] NULL_ARRAY = {};
    public static final ImmutableArrayList<LibraryBackup> EMPTY_LIST = new ImmutableArrayList<LibraryBackup>(NULL_ARRAY);
    
    /** Library persistent data. */                                     public final ImmutableLibrary d;
    /** True if library needs saving to disk. */                        public final boolean modified;
    /** Array of referenced libs */                                     public final LibId[] referencedLibs;

    /** Creates a new instance of LibraryBackup */
    public LibraryBackup(ImmutableLibrary d, boolean modified, LibId[] referencedLibs) {
        this.d = d;
        this.modified = modified;
        this.referencedLibs = referencedLibs;
    }
    
    /**
     * Writes this LibraryBackup to SnapshotWriter.
     * @param writer where to write.
     */
    void write(SnapshotWriter writer) throws IOException {
        d.write(writer);
        writer.out.writeBoolean(modified);
        writer.out.writeInt(referencedLibs.length);
        for (int i = 0; i < referencedLibs.length; i++)
            writer.writeLibId(referencedLibs[i]);
    }
    
    /**
     * Reads LibraryBackup from SnapshotReader.
     * @param reader where to read.
     */
    static LibraryBackup read(SnapshotReader reader) throws IOException {
        ImmutableLibrary d = ImmutableLibrary.read(reader);
        boolean modified = reader.in.readBoolean();
        int refsLength = reader.in.readInt();
        LibId[] refs = new LibId[refsLength];
        for (int i = 0; i < refsLength; i++)
            refs[i] = reader.readLibId();
        return new LibraryBackup(d, modified, refs);
    }
    
    /**
	 * Checks invariant of this CellBackup.
	 * @throws AssertionError if invariant is broken.
	 */
    public void check() {
        d.check();
        for (LibId libId: referencedLibs)
            assert libId != null;
    }
}
