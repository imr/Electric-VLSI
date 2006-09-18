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

import com.sun.electric.database.hierarchy.View;
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
    /** The group name of this ImmutableCell. */                public final CellName groupName;
	/** The date this ImmutableCell was created. */				public final long creationDate;
	/** This ImmutableCell's Technology. */						public final Technology tech;
    
	/**
	 * The private constructor of ImmutableCell. Use the factory "newInstance" instead.
     * @param cellId id of this ImmutableCell.
     * @param libId library of this ImmutableCell.
     * @param cellName cell name of this ImmutableCell.
     * @param groupName group name of this ImmutableCell.
     * @param creationDate the date this ImmutableCell was created.
     * @param revisionDate the date this ImmutableCell was last modified.
     * @param tech the technology of this ImmutableCell.
     * @param flags flags of this ImmutableCell.
     * @param vars array of Variables of this ImmutableCell
	 */
     private ImmutableCell(CellId cellId, LibId libId, CellName cellName, CellName groupName,
             long creationDate, Technology tech, int flags, Variable[] vars) {
        super(vars, flags);
        this.cellId = cellId;
        this.libId = libId;
        this.cellName = cellName;
        this.groupName = groupName;
        this.creationDate = creationDate;
        this.tech = tech;
        check();
    }

	/**
	 * Returns new ImmutableCell object.
     * @param cellId id of this ImmutableCell.
     * @param libId library of this ImmutableCell.
     * @param cellName cell name of this ImmutableCell.
     * @param creationDate date of this ImmutableCell.
	 * @return new ImmutableCell object.
	 * @throws NullPointerException if cellId or libId is null.
	 */
    public static ImmutableCell newInstance(CellId cellId, LibId libId, CellName cellName, long creationDate) {
        if (cellId == null) throw new NullPointerException("cellId");
        if (libId == null) throw new NullPointerException("libId");
        if (cellName == null) throw new NullPointerException("cellName");
		return new ImmutableCell(cellId, libId, cellName, null, creationDate, null, 0, Variable.NULL_ARRAY);
    }

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by library id.
	 * @param libId new library id.
	 * @return ImmutableCell which differs from this ImmutableCell by library id.
	 * @throws NullPointerException if libId is null.
	 */
	public ImmutableCell withLibrary(LibId libId) {
        if (this.libId == libId) return this;
        if (libId == null) throw new NullPointerException("libId");
		return new ImmutableCell(this.cellId, libId, this.cellName, this.groupName,
                this.creationDate, this.tech, this.flags, getVars());
	}

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by cell name.
	 * @param cellName new cell name.
	 * @return ImmutableCell which differs from this ImmutableCell by cell name.
	 */
	public ImmutableCell withCellName(CellName cellName) {
        if (this.cellName == cellName) return this;
        if (cellName == null) throw new NullPointerException("cellName");
        if (this.cellName.equals(cellName)) return this;
		return new ImmutableCell(this.cellId, this.libId, cellName, this.groupName,
                this.creationDate, this.tech, this.flags, getVars());
	}

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by group name.
	 * @param groupName new group name.
	 * @return ImmutableCell which differs from this ImmutableCell by cell name.
     * @throws IllegalArgumentException if groupName is not schematic view and zero version.
	 */
	public ImmutableCell withGroupName(CellName groupName) {
        if (this.groupName == groupName) return this;
        if (this.groupName != null && this.groupName.equals(groupName)) return this;
        if (groupName != null && (groupName.getVersion() != 0 || groupName.getView() != View.SCHEMATIC))
            throw new IllegalArgumentException(groupName.toString());
        return new ImmutableCell(this.cellId, this.libId, this.cellName, groupName,
                this.creationDate, this.tech, this.flags, getVars());
	}

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by creation date.
	 * @param creationDate new creation date.
	 * @return ImmutableCell which differs from this ImmutableCell by creation date.
	 */
	public ImmutableCell withCreationDate(long creationDate) {
        if (this.creationDate == creationDate) return this;
		return new ImmutableCell(this.cellId, this.libId, this.cellName, this.groupName,
                creationDate, this.tech, this.flags, getVars());
	}

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by technology.
	 * @param tech new technology.
	 * @return ImmutableCell which differs from this ImmutableCell by technology.
	 */
	public ImmutableCell withTech(Technology tech) {
        if (this.tech == tech) return this;
		return new ImmutableCell(this.cellId, this.libId, this.cellName, this.groupName,
                this.creationDate, tech, this.flags, getVars());
	}

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by flags.
	 * @param flags new flags.
	 * @return ImmutableCell which differs from this ImmutableCell by flags.
	 */
	public ImmutableCell withFlags(int flags) {
        if (this.flags == flags) return this;
		return new ImmutableCell(this.cellId, this.libId, this.cellName, this.groupName,
                this.creationDate, this.tech, flags, getVars());
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
		return new ImmutableCell(this.cellId, this.libId, this.cellName, this.groupName,
                this.creationDate, this.tech, this.flags, vars);
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
		return new ImmutableCell(this.cellId, this.libId, this.cellName, this.groupName,
                this.creationDate, this.tech, this.flags, vars);
    }
    
	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by renamed Ids.
	 * @param idMapper a map from old Ids to new Ids.
     * @return ImmutableCell with renamed Ids.
	 */
    ImmutableCell withRenamedIds(IdMapper idMapper) {
        Variable[] vars = arrayWithRenamedIds(idMapper);
        CellId cellId = idMapper.get(this.cellId);
        LibId libId = idMapper.get(this.libId);
        if (getVars() == vars && this.cellId == cellId && this.libId == libId) return this;
		return new ImmutableCell(cellId, libId, this.cellName, this.groupName,
                this.creationDate, this.tech, this.flags, vars);
    }
    
	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by removing all Variables.
     * Returns this ImmutableCell if it hasn't variables.
	 * @return ImmutableCell without Variables.
	 */
    public ImmutableCell withoutVariables() {
        if (this.getNumVariables() == 0) return this;
		return new ImmutableCell(this.cellId, this.libId, this.cellName, this.groupName,
                this.creationDate, this.tech, this.flags, Variable.NULL_ARRAY);
    }
    
    /**
     * Writes this ImmutableCell to SnapshotWriter.
     * @param writer where to write.
     */
    void write(SnapshotWriter writer) throws IOException {
        writer.writeNodeProtoId(cellId);
        writer.writeLibId(libId);
        writer.writeString(cellName.toString());
        writer.writeBoolean(groupName != null);
        if (groupName != null)
            writer.writeString(groupName.toString());
        writer.writeLong(creationDate);
        writer.writeBoolean(tech != null);
        if (tech != null)
            writer.writeTechnology(tech);
        writer.writeInt(flags);
        super.write(writer);
    }
    
    /**
     * Reads ImmutableCell from SnapshotReader.
     * @param reader where to read.
     */
    static ImmutableCell read(SnapshotReader reader) throws IOException {
        CellId cellId = (CellId)reader.readNodeProtoId();
        LibId libId = reader.readLibId();
        String cellNameString = reader.readString();
        CellName cellName = CellName.parseName(cellNameString);
        CellName groupName = null;
        boolean hasGroupName = reader.readBoolean();
        if (hasGroupName) {
            String groupNameString = reader.readString();
            groupName = CellName.parseName(groupNameString);
        }
        long creationDate = reader.readLong();
        boolean hasTech = reader.readBoolean();
        Technology tech = hasTech ? reader.readTechnology() : null;
        int flags = reader.readInt();
        boolean hasVars = reader.readBoolean();
        Variable[] vars = hasVars ? readVars(reader) : Variable.NULL_ARRAY;
        return new ImmutableCell(cellId, libId, cellName, groupName, creationDate, tech, flags, vars);
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
        return this.cellId == that.cellId && this.libId == that.libId && this.cellName == that.cellName && this.groupName == that.groupName &&
                this.creationDate == that.creationDate && this.tech == that.tech && this.flags == that.flags;
    }
    
	/**
	 * Checks invariant of this ImmutableCell.
	 * @throws AssertionError if invariant is broken.
	 */
	public void check() {
        check(true);
        assert cellId != null;
        assert libId != null;
        cellName.check();
        if (groupName != null) {
            assert groupName.getVersion() == 0;
            assert groupName.getView() == View.SCHEMATIC;
        }
	}
}
