/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableLibrary.java
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

import com.sun.electric.database.text.Version;
import com.sun.electric.database.variable.Variable;

import java.io.IOException;
import java.net.URL;

/**
 * Immutable class ImmutableLibrary represents a library.
 */
public class ImmutableLibrary extends ImmutableElectricObject {
    
	/** LibId of this ImmutableLibrary. */                              public final LibId libId;
	/** file location of this ImmutableLibrary */                       public final URL libFile;
	/** version of Electric which wrote the ImmutableLibrary. */		public final Version version;
    
	/**
	 * The private constructor of ImmutableLibrary. Use the factory "newInstance" instead.
     * @param libId id of this ImmutableLibrary.
     * @param libFile file location of this ImmutableLibrary.
     * @param version version of Electric which wrote this ImmutableLibrary.
     * @param flags flags of ImmutableLibrary.
     * @param vars array of Variables of this ImmutableLibrary
	 */
     private ImmutableLibrary(LibId libId, URL libFile, Version version, int flags, Variable[] vars) {
        super(vars, flags);
        this.libId = libId;
        this.libFile = libFile;
        this.version = version;
//        check();
    }

	/**
	 * Returns new ImmutableLibrary object.
     * @param libId id of this ImmutableLibrary.
     * @param libFile file location of this ImmutableLibrary.
     * @param version version of Electric which wrote this ImmutableLibrary.
	 * @return new ImmutableLibrary object.
	 * @throws NullPointerException if libId is null.
	 */
    public static ImmutableLibrary newInstance(LibId libId, URL libFile, Version version) {
        if (libId == null) throw new NullPointerException("libId");
		return new ImmutableLibrary(libId, libFile, version, 0, Variable.NULL_ARRAY);
    }

	/**
	 * Returns ImmutableLibrary which differs from this ImmutableLibrary by file.
     * @param libFile library file.
	 * @return ImmutableLibrary which differs from this ImmutableLibrary by file.
	 * @throws NullPointerException if name is null
	 */
	public ImmutableLibrary withLibFile(URL libFile) {
        if (this.libFile == libFile) return this;
		if (this.libFile != null && this.libFile.equals(libFile)) return this;
		return new ImmutableLibrary(this.libId, libFile, this.version, this.flags, getVars());
	}

	/**
	 * Returns ImmutableLibrary which differs from this ImmutableLibrary by version.
     * @param version version of Electric which wrote this ImmutableLibrary.
	 * @return ImmutableLibrary which differs from this ImmutableExport by version.
	 */
	public ImmutableLibrary withVersion(Version version) {
        if (this.version == version) return this;
		if (this.version != null && version != null && this.version.equals(version)) return this;
		return new ImmutableLibrary(this.libId, this.libFile, version, this.flags, getVars());
	}

	/**
	 * Returns ImmutableLibrary which differs from this ImmutableLibrary by additional Variable.
     * If this ImmutableLibrary has Variable with the same key as new, the old variable will not be in new
     * ImmutableLibrary.
	 * @param var additional Variable.
	 * @return ImmutableLibrary with additional Variable.
	 * @throws NullPointerException if var is null
	 */
    public ImmutableLibrary withVariable(Variable var) {
        Variable[] vars = arrayWithVariable(var.withParam(false));
        if (this.getVars() == vars) return this;
		return new ImmutableLibrary(this.libId, this.libFile, this.version, this.flags, vars);
    }
    
	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by removing Variable
     * with the specified key. Returns this ImmutableCell if it doesn't contain variable with the specified key.
	 * @param key Variable Key to remove.
	 * @return ImmutableCell without Variable with the specified key.
	 * @throws NullPointerException if key is null
	 */
    public ImmutableLibrary withoutVariable(Variable.Key key) {
        Variable[] vars = arrayWithoutVariable(key);
        if (this.getVars() == vars) return this;
		return new ImmutableLibrary(this.libId, this.libFile, this.version, this.flags, vars);
    }
    
	/**
	 * Returns ImmutableLibrary which differs from this ImmutableLibrary by renamed Ids.
	 * @param idMapper a map from old Ids to new Ids.
     * @return ImmutableLibrary with renamed Ids.
	 */
    ImmutableLibrary withRenamedIds(IdMapper idMapper) {
        Variable[] vars = arrayWithRenamedIds(idMapper);
        LibId libId = idMapper.get(this.libId);
        if (getVars() == vars && this.libId == libId) return this;
		return new ImmutableLibrary(libId, this.libFile, this.version, this.flags, vars);
    }
    
	/**
	 * Returns ImmutableLibrary which differs from this ImmutableLibrary by flags.
	 * @param flags new flags.
	 * @return ImmutableLibrary with the specified flags.
	 */
    public ImmutableLibrary withFlags(int flags) {
        if (this.flags == flags) return this;
		return new ImmutableLibrary(this.libId, this.libFile, this.version, flags, getVars());
    }
    
    /**
     * Writes this ImmutableLibrary to SnapshotWriter.
     * @param writer where to write.
     */
    void write(SnapshotWriter writer) throws IOException {
        writer.writeLibId(libId);
        writer.writeString(libFile != null ? libFile.toString() : "");
        writer.writeString(version != null ? version.toString() : "");
        writer.writeInt(flags);
        super.write(writer);
    }
    
    /**
     * Reads ImmutableLibrary from SnapshotReader.
     * @param reader where to read.
     */
    static ImmutableLibrary read(SnapshotReader reader) throws IOException {
        LibId libId = reader.readLibId();
        String libFileString = reader.readString();
        URL libFile = libFileString.length() > 0 ? new URL(libFileString) : null;
        String versionString = reader.readString();
        Version version = versionString.length() > 0 ? Version.parseVersion(versionString) : null;
        int flags = reader.readInt();
        boolean hasVars = reader.readBoolean();
        Variable[] vars = hasVars ? readVars(reader) : Variable.NULL_ARRAY;
        return new ImmutableLibrary(libId, libFile, version, flags, vars);
    }
    
    /**
     * Return a hash code value for fields of this object.
     * Variables of objects are not compared
     */
    public int hashCodeExceptVariables() { return libId.hashCode(); }

    /**
     * Indicates whether fields of other ImmutableElectricObject are equal to fileds of this object.
     * Variables of objects are not compared.
     * @param o other ImmutableElectricObject.
     * @return true if fields of objects are equal.
     */
    public boolean equalsExceptVariables(ImmutableElectricObject o) {
        if (this == o) return true;
        if (!(o instanceof ImmutableLibrary)) return false;
        ImmutableLibrary that = (ImmutableLibrary)o;
        return this.libId == that.libId && this.libFile == that.libFile &&
                this.version == that.version && this.flags == that.flags;
    }
    
	/**
	 * Returns a printable version of this ImmutableLibrary.
	 * @return a printable version of this ImmutableLibrary.
	 */
    public String toString() { return libId.toString(); }
    
	/**
	 * Checks invariant of this ImmutableCell.
	 * @throws AssertionError if invariant is broken.
	 */
	public void check() {
        check(false);
        assert libId != null;
	}
}
