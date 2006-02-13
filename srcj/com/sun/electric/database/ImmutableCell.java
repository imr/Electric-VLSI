/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableCell.java
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

import com.sun.electric.database.text.CellName;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import java.io.IOException;

/**
 * Immutable class ImmutableCell represents a cell.
 */
public class ImmutableCell extends ImmutableElectricObject {
    
	/** CellId of this ImmutableCell. */                        public final CellId cellId;
	/** The library this ImmutableCell belongs to. */			public final LibId libId;
	/** The CellName of this ImmutableCell. */					public final CellName cellName;
	/** The date this ImmutableCell was created. */				public final long creationDate;
	/** The date this ImmutableCell was last modified. */		public final long revisionDate;
	/** This ImmutableCell's Technology. */						public final Technology tech;
	/** ImmutableCell's flags. */								public final int flags;
    /** "Modified" flag of this ImmutableCell. */               public final byte modified;
    
	/**
	 * The private constructor of ImmutableCell. Use the factory "newInstance" instead.
     * @param cellId id of this ImmutableCell.
     * @param libId library of this ImmutableCell.
     * @param cellName cell name of this ImmutableCell.
     * @param creationDate the date this ImmutableCell was created.
     * @param revisionDate the date this ImmutableCell was last modified.
     * @param tech the technology of this ImmutableCell.
     * @param flags flags of this ImmutableCell.
     * @param modified "Modified" flag of this ImmutableCell.
     * @param vars array of Variables of this ImmutableCell
	 */
     private ImmutableCell(CellId cellId, LibId libId, CellName cellName,
             long creationDate, long revisionDate, Technology tech, int flags, byte modified, Variable[] vars) {
        super(vars);
        this.cellId = cellId;
        this.libId = libId;
        this.cellName = cellName;
        this.creationDate = creationDate;
        this.revisionDate = revisionDate;
        this.tech = tech;
        this.flags = flags;
        this.modified = modified;
        check();
    }

	/**
	 * Returns new ImmutableCell object.
     * @param cellId id of this ImmutableCell.
     * @param libId library of this ImmutableCell.
     * @param cellName cell name of this ImmutableCell.
     * @param creation date of this ImmutableCell.
	 * @return new ImmutableCell object.
	 * @throws NullPointerException if cellId or libId is null.
	 */
    public static ImmutableCell newInstance(CellId cellId, LibId libId, CellName cellName, long creationDate) {
        if (cellId == null) throw new NullPointerException("cellId");
        if (libId == null) throw new NullPointerException("libId");
//        if (cellName == null) throw new NullPointerException("cellName");
		return new ImmutableCell(cellId, libId, cellName, creationDate, creationDate, null, 0, (byte)0, Variable.NULL_ARRAY);
    }

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by cell name.
	 * @param cellName new cell name.
	 * @return ImmutableCell which differs from this ImmutableCell by cell name.
	 */
	public ImmutableCell withCellName(CellName cellName) {
        if (this.cellName == cellName) return this;
        if (this.cellName != null && this.cellName.equals(cellName)) return this;
		return new ImmutableCell(this.cellId, this.libId, cellName,
                this.creationDate, this.revisionDate, this.tech, this.flags, this.modified, getVars());
	}

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by creation date.
	 * @param creationDate new creation date.
	 * @return ImmutableCell which differs from this ImmutableCell by creation date.
	 */
	public ImmutableCell withCreationDate(long creationDate) {
        if (this.creationDate == creationDate) return this;
		return new ImmutableCell(this.cellId, this.libId, this.cellName,
                creationDate, this.revisionDate, this.tech, this.flags, this.modified, getVars());
	}

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by revision date.
	 * @param revisionDate new revision date.
	 * @return ImmutableCell which differs from this ImmutableCell by revision date.
	 */
	public ImmutableCell withRevisionDate(long revisionDate) {
        if (this.revisionDate == revisionDate) return this;
		return new ImmutableCell(this.cellId, this.libId, this.cellName,
                this.creationDate, revisionDate, this.tech, this.flags, this.modified, getVars());
	}

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by technology.
	 * @param tech new technology.
	 * @return ImmutableCell which differs from this ImmutableCell by technology.
	 */
	public ImmutableCell withTech(Technology tech) {
        if (this.tech == tech) return this;
		return new ImmutableCell(this.cellId, this.libId, this.cellName,
                this.creationDate, this.revisionDate, tech, this.flags, this.modified, getVars());
	}

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by flags.
	 * @param flags new flags.
	 * @return ImmutableCell which differs from this ImmutableCell by flags.
	 */
	public ImmutableCell withFlags(int flags) {
        if (this.flags == flags) return this;
		return new ImmutableCell(this.cellId, this.libId, this.cellName,
                this.creationDate, this.revisionDate, this.tech, flags, this.modified, getVars());
	}

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by "Modified" flag.
	 * @param modified new "Modified" flags.
	 * @return ImmutableCell which differs from this ImmutableCell by "Modified" flags.
     * @throws IllegalArgumentException if modified is not -1, 0 or 1.
	 */
	public ImmutableCell withModified(byte modified) {
        if (this.modified == modified) return this;
        if (modified < -1 || modified > 1) throw new IllegalArgumentException("modified");
		return new ImmutableCell(this.cellId, this.libId, this.cellName,
                this.creationDate, this.revisionDate, this.tech, this.flags, modified, getVars());
	}

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by additional Variable.
     * If this ImmutableCell has Variable with the same key as new, the old variable will not be in new
     * ImmutableCell.
	 * @param var additional Variable.
	 * @return ImmutableCell with additional Variable.
	 * @throws NullPointerException if var is null
	 */
    public ImmutableCell withVariable(Variable var) {
        Variable[] vars = arrayWithVariable(var);
        if (this.getVars() == vars) return this;
		return new ImmutableCell(this.cellId, this.libId, this.cellName,
                this.creationDate, this.revisionDate, this.tech, this.flags, this.modified, vars);
    }
    
	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by removing Variable
     * with the specified key. Returns this ImmutableCell if it doesn't contain variable with the specified key.
	 * @param key Variable Key to remove.
	 * @return ImmutableCell without Variable with the specified key.
	 * @throws NullPointerException if key is null
	 */
    public ImmutableCell withoutVariable(Variable.Key key) {
        Variable[] vars = arrayWithoutVariable(key);
        if (this.getVars() == vars) return this;
		return new ImmutableCell(this.cellId, this.libId, this.cellName,
                this.creationDate, this.revisionDate, this.tech, this.flags, this.modified, vars);
    }
    
    /**
     * Writes this ImmutableCell to SnapshotWriter.
     * @param writer where to write.
     */
    void write(SnapshotWriter writer) throws IOException {
        writer.writeNodeProtoId(cellId);
        writer.writeLibId(libId);
        writer.out.writeBoolean(cellName != null);
        if (cellName != null)
            writer.out.writeUTF(cellName.toString());
        writer.out.writeLong(creationDate);
        writer.out.writeLong(revisionDate);
        writer.out.writeBoolean(tech != null);
        if (tech != null)
            writer.writeTechnology(tech);
        writer.out.writeInt(flags);
        writer.out.writeByte(modified);
        super.write(writer);
    }
    
    /**
     * Reads ImmutableCell from SnapshotReader.
     * @param reader where to read.
     */
    static ImmutableCell read(SnapshotReader reader) throws IOException {
        CellId cellId = (CellId)reader.readNodeProtoId();
        LibId libId = reader.readLibId();
        CellName cellName = null;
        boolean hasCellName = reader.in.readBoolean();
        if (hasCellName) {
            String cellNameString = reader.in.readUTF();
            cellName = CellName.parseName(cellNameString);
        }
        long creationDate = reader.in.readLong();
        long revisionDate = reader.in.readLong();
        boolean hasTech = reader.in.readBoolean();
        Technology tech = hasTech ? reader.readTechnology() : null;
        int flags = reader.in.readInt();
        byte modified = reader.in.readByte();
        boolean hasVars = reader.in.readBoolean();
        Variable[] vars = hasVars ? readVars(reader) : Variable.NULL_ARRAY;
        return new ImmutableCell(cellId, libId, cellName,
                creationDate, revisionDate, tech, flags, modified, vars);
    }
    
    /**
     * Return a hash code value for fields of this object.
     * Variables of objects are not compared
     */
    public int hashCodeExceptVariables() { return cellId.hashCode(); }

    /**
     * Indicates whether fields of other ImmutableElectricObject are equal to fileds of this object.
     * Variables of objects are not compared.
     * @param o other ImmutableElectricObject.
     * @return true if fields of objects are equal.
     */
    public boolean equalsExceptVariables(ImmutableElectricObject o) {
        if (this == o) return true;
        if (!(o instanceof ImmutableCell)) return false;
        ImmutableCell that = (ImmutableCell)o;
        return this.cellId == that.cellId && this.libId == that.libId && this.cellName == that.cellName &&
                this.creationDate == that.creationDate && this.revisionDate == that.revisionDate &&
                this.tech == that.tech && this.flags == that.flags && this.modified == that.modified;
    }
    
	/**
	 * Checks invariant of this ImmutableCell.
	 * @throws AssertionError if invariant is broken.
	 */
	public void check() {
        check(true);
        assert cellId != null;
        assert libId != null;
        assert -1 <= modified && modified <= 1;
	}
}
