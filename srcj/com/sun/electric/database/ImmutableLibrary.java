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
import java.net.URL;

/**
 * Immutable class ImmutableLibrary represents a library.
 */
public class ImmutableLibrary extends ImmutableElectricObject {
    
	/** LibId of this ImmutableLibrary. */                              public final LibId libId;
	/** name of this ImmutableLibrary  */                               public final String libName;
	/** file location of this ImmutableLibrary */                       public final URL libFile;
	/** version of Electric which wrote the ImmutableLibrary. */		public final Version version;
    
	/**
	 * The private constructor of ImmutableLibrary. Use the factory "newInstance" instead.
     * @param libId id of this ImmutableLibrary.
     * @param libName name of this ImmutableLibrary.
     * @param libFile file location of this ImmutableLibrary.
     * @param version version of Electric which wrote this ImmutableLibrary.
     * @param vars array of Variables of this ImmutableLibrary
	 */
     private ImmutableLibrary(LibId libId, String libName, URL libFile, Version version, Variable[] vars) {
        super(vars);
        this.libId = libId;
        this.libName = libName;
        this.libFile = libFile;
        this.version = version;
        check();
    }

	/**
	 * Returns new ImmutableLibrary object.
     * @param libId id of this ImmutableLibrary.
     * @param libName name of this ImmutableLibrary.
     * @param libFile file location of this ImmutableLibrary.
     * @param version version of Electric which wrote this ImmutableLibrary.
	 * @return new ImmutableLibrary object.
	 * @throws NullPointerException if libId is null.
	 */
    public static ImmutableLibrary newInstance(LibId libId, String libName, URL libFile, Version version) {
        if (libId == null) throw new NullPointerException("libId");
		return new ImmutableLibrary(libId, libName, libFile, version, Variable.NULL_ARRAY);
    }

	/**
	 * Returns ImmutableLibrary which differs from this ImmutableLibrary by name and file.
	 * @param libName library name.
     * @param libFile library file.
	 * @return ImmutableLibrary which differs from this ImmutableExport by name and file.
	 * @throws NullPointerException if name is null
	 */
	public ImmutableLibrary withName(String libName, URL libFile) {
//		if (this.libName.equals(libName) && this.libFile.equals(libFile)) return this;
		if (libName == null) throw new NullPointerException("libName");
		return new ImmutableLibrary(this.libId, libName, libFile, this.version, getVars());
	}

	/**
	 * Returns ImmutableLibrary which differs from this ImmutableLibrary by version.
     * @param version version of Electric which wrote this ImmutableLibrary.
	 * @return ImmutableLibrary which differs from this ImmutableExport by version.
	 */
	public ImmutableLibrary withVersion(Version version) {
        if (this.version == version) return this;
		if (this.version != null && version != null && this.version.equals(version)) return this;
		return new ImmutableLibrary(this.libId, this.libName, this.libFile, version, getVars());
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
		return new ImmutableLibrary(this.libId, this.libName, this.libFile, this.version, vars);
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
		return new ImmutableLibrary(this.libId, this.libName, this.libFile, this.version, vars);
    }
    
	/**
	 * Checks invariant of this ImmutableCell.
	 * @throws AssertionError if invariant is broken.
	 */
	public void check() {
        check(false);
        assert libId != null;
	}
}
