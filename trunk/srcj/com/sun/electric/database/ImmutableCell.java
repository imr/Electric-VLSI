/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableCell.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.variable.Variable;

import java.io.IOException;
import java.util.Iterator;

/**
 * Immutable class ImmutableCell represents a cell.
 */
public class ImmutableCell extends ImmutableElectricObject {

    /** CellId of this ImmutableCell. */
    public final CellId cellId;
    /** The group name of this ImmutableCell. */
    public final CellName groupName;
    /** The date this ImmutableCell was created. */
    public final long creationDate;
    /** The date this ImmutableCell was modified. */
    public final long revisionDate;
    /** This ImmutableCell's TechId. */
    public final TechId techId;
    /** Parameters of this ImmutableCell. */
    final Variable[] params;

    /**
     * The private constructor of ImmutableCell. Use the factory "newInstance" instead.
     * @param cellId id of this ImmutableCell.
     * @param groupName group name of this ImmutableCell.
     * @param creationDate the date this ImmutableCell was created.
     * @param revisionDate the date this ImmutableCell was last modified.
     * @param techId the technology of this ImmutableCell.
     * @param flags flags of this ImmutableCell.
     * @param vars array of Variables of this ImmutableCell
     */
    private ImmutableCell(CellId cellId, CellName groupName,
            long creationDate, long revisionDate, TechId techId, int flags, Variable[] vars, Variable[] params) {
        super(vars, flags);
        this.cellId = cellId;
        this.groupName = groupName;
        this.creationDate = creationDate;
        this.revisionDate = revisionDate;
        this.techId = techId;
        this.params = params;
        check();
    }

    /**
     * Returns new ImmutableCell object.
     * @param cellId id of this ImmutableCell.
     * @param creationDate creation date of this ImmutableCell.
     * @return new ImmutableCell object.
     * @throws NullPointerException if cellId or libId is null.
     */
    public static ImmutableCell newInstance(CellId cellId, long creationDate) {
        if (cellId == null) {
            throw new NullPointerException("cellId");
        }
        CellName cellName = CellName.newName(cellId.cellName.getName(), View.SCHEMATIC, 0);
        return new ImmutableCell(cellId, cellName, creationDate, creationDate, null, 0, Variable.NULL_ARRAY, Variable.NULL_ARRAY);
    }

    /**
     * Returns ImmutableCell which differs from this ImmutableCell by group name.
     * @param groupName new group name.
     * @return ImmutableCell which differs from this ImmutableCell by cell name.
     * @throws IllegalArgumentException if groupName is not schematic view and zero version.
     */
    public ImmutableCell withGroupName(CellName groupName) {
        if (this.groupName.equals(groupName)) {
            return this;
        }
        if (groupName.getVersion() != 0 || groupName.getView() != View.SCHEMATIC) {
            throw new IllegalArgumentException(groupName.toString());
        }
        return new ImmutableCell(this.cellId, groupName,
                this.creationDate, this.revisionDate, this.techId, this.flags, getVars(), this.params);
    }

    /**
     * Returns ImmutableCell which differs from this ImmutableCell by creation date.
     * @param creationDate new creation date.
     * @return ImmutableCell which differs from this ImmutableCell by creation date.
     */
    public ImmutableCell withCreationDate(long creationDate) {
        if (this.creationDate == creationDate) {
            return this;
        }
        return new ImmutableCell(this.cellId, this.groupName,
                creationDate, this.revisionDate, this.techId, this.flags, getVars(), this.params);
    }

    /**
     * Returns ImmutableCell which differs from this ImmutableCell by revision date.
     * @param revisionDate new revision date.
     * @return ImmutableCell which differs from this ImmutableCell by revision date.
     */
    public ImmutableCell withRevisionDate(long revisionDate) {
        if (this.revisionDate == revisionDate) {
            return this;
        }
        return new ImmutableCell(this.cellId, this.groupName,
                this.creationDate, revisionDate, this.techId, this.flags, getVars(), this.params);
    }

    /**
     * Returns ImmutableCell which differs from this ImmutableCell by technology.
     * @param techId new technology Id.
     * @return ImmutableCell which differs from this ImmutableCell by technology.
     */
    public ImmutableCell withTechId(TechId techId) {
        if (this.techId == techId) {
            return this;
        }
        if (techId != null && techId.idManager != cellId.idManager) {
            throw new IllegalArgumentException("techId");
        }
        return new ImmutableCell(this.cellId, this.groupName,
                this.creationDate, this.revisionDate, techId, this.flags, getVars(), this.params);
    }

    /**
     * Returns ImmutableCell which differs from this ImmutableCell by flags.
     * @param flags new flags.
     * @return ImmutableCell which differs from this ImmutableCell by flags.
     */
    public ImmutableCell withFlags(int flags) {
        if (this.flags == flags) {
            return this;
        }
        return new ImmutableCell(this.cellId, this.groupName,
                this.creationDate, this.revisionDate, this.techId, flags, getVars(), this.params);
    }

    /**
     * Method to return the Parameter on this ImmuatbleCell with a given key.
     * @param key the key of the Variable.
     * @return the Parameter with that key, or null if there is no such Variable.
     * @throws NullPointerException if key is null
     */
    public Variable getParameter(Variable.AttrKey key) {
        int paramIndex = searchVar(params, key);
        return paramIndex >= 0 ? params[paramIndex] : null;
    }

    /**
     * Method to return an Iterator over all Parameters on this ImmutableCell.
     * @return an Iterator over all Parameters on this ImmutableCell.
     */
    public Iterator<Variable> getParameters() {
        return ArrayIterator.iterator(params);
    }

    /**
     * Method to return the number of Parameters on this ImmutableCell.
     * @return the number of Parametes on this ImmutableCell.
     */
    public int getNumParameters() {
        return params.length;
    }

    /**
     * Method to return the Parameter by its paramIndex.
     * @param paramIndex index of Parameter.
     * @return the Parameter with given paramIndex.
     * @throws ArrayIndexOutOfBoundesException if paramIndex out of bounds.
     */
    public Variable getParameter(int paramIndex) {
        return params[paramIndex];
    }

    /**
     * Returns ImmutableCell which differs from this ImmutableCell by additional parameter.
     * If this ImmutableCell has parameter with the same key as new, the old variable will not be in new
     * ImmutableCell.
     * @param var additional Variable.
     * @return ImmutableCell with additional Variable.
     * @throws NullPointerException if var is null
     */
    public ImmutableCell withParam(Variable var) {
        if (!var.getTextDescriptor().isParam() || !var.isInherit()) {
            throw new IllegalArgumentException("Variable " + var + " is not param");
        }
        if (!paramsAllowed()) {
            throw new IllegalArgumentException("Parameters are not allowed for " + this);
        }
        if (searchVar(var.getKey()) >= 0) {
            throw new IllegalArgumentException(this + " has variable with the same name as parameter " + var);
        }
        Variable[] params = arrayWithVariable(this.params, var);
        if (this.params == params) {
            return this;
        }
        return new ImmutableCell(this.cellId, this.groupName,
                this.creationDate, this.revisionDate, this.techId, this.flags, getVars(), params);
    }

    /**
     * Returns ImmutableCell which differs from this ImmutableCell by removing parameter
     * with the specified key. Returns this ImmutableCell if it doesn't contain parameter with the specified key.
     * @param key Variable Key to remove.
     * @return ImmutableCell without Variable with the specified key.
     * @throws NullPointerException if key is null
     */
    public ImmutableCell withoutParam(Variable.AttrKey key) {
        Variable[] params = arrayWithoutVariable(this.params, key);
        if (this.params == params) {
            return this;
        }
        return new ImmutableCell(this.cellId, this.groupName,
                this.creationDate, this.revisionDate, this.techId, this.flags, getVars(), params);
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
        if (var.getTextDescriptor().isParam()) {
            throw new IllegalArgumentException("Variable " + var + " is param");
        }
        if (var.isAttribute() && searchVar(params, var.getKey()) >= 0) {
            throw new IllegalArgumentException(this + " has parameter with the same name as variable " + var);
        }
        if (!paramsAllowed()) {
            var = var.withParam(false);
        }
        Variable[] vars = arrayWithVariable(var);
        if (this.getVars() == vars) {
            return this;
        }
        return new ImmutableCell(this.cellId, this.groupName,
                this.creationDate, this.revisionDate, this.techId, this.flags, vars, this.params);
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
        if (this.getVars() == vars) {
            return this;
        }
        return new ImmutableCell(this.cellId, this.groupName,
                this.creationDate, this.revisionDate, this.techId, this.flags, vars, params);
    }

    /**
     * Returns ImmutableCell which differs from this ImmutableCell by renamed Ids.
     * @param idMapper a map from old Ids to new Ids.
     * @return ImmutableCell with renamed Ids.
     */
    ImmutableCell withRenamedIds(IdMapper idMapper) {
        Variable[] vars = arrayWithRenamedIds(idMapper);
        Variable[] params = arrayWithRenamedIds(this.params, idMapper);
        CellId cellId = idMapper.get(this.cellId);
        if (getVars() == vars && this.params == params && this.cellId == cellId) {
            return this;
        }
        return new ImmutableCell(cellId, this.groupName,
                this.creationDate, this.revisionDate, this.techId, this.flags, vars, params);
    }

    /**
     * Returns ImmutableCell which differs from this ImmutableCell by removing all Variables.
     * Returns this ImmutableCell if it hasn't variables.
     * @return ImmutableCell without Variables.
     */
    public ImmutableCell withoutVariables() {
        if (this.getNumVariables() == 0 && params.length == 0) {
            return this;
        }
        return new ImmutableCell(this.cellId, this.groupName,
                this.creationDate, this.revisionDate, this.techId, this.flags, Variable.NULL_ARRAY, Variable.NULL_ARRAY);
    }

    void checkSimilarParams(ImmutableCell that) {
        if (this.params.length != that.params.length) {
            throw new IllegalArgumentException("Different params in " + this + " and " + that);
        }
        for (int i = 0; i < this.params.length; i++) {
            Variable thisParam = this.params[i];
            Variable thatParam = that.params[i];
            if (thisParam.getKey() != thatParam.getKey()) {
                throw new IllegalArgumentException("Different params in " + this + " and " + that);
            }
            if (thisParam.getUnit() != thatParam.getUnit()) {
                throw new IllegalArgumentException("Different units of param " + thisParam.getKey() + " in " + this + " and " + that);
            }
            if (thisParam.withObject(thatParam.getObject()) != thisParam) {
                throw new IllegalArgumentException("Different values of param " + thisParam.getKey() + " in " + this + " and " + that);
            }
        }
    }

    /**
     * Returns LibId of the Library to which this ImmutableCell belongs.
     */
    public LibId getLibId() {
        return cellId.libId;
    }

    /**
     * Writes this ImmutableCell to IdWriter.
     * @param writer where to write.
     */
    void write(IdWriter writer) throws IOException {
        writer.writeNodeProtoId(cellId);
        writer.writeString(groupName.toString());
        writer.writeLong(creationDate);
        writer.writeLong(revisionDate);
        writer.writeBoolean(techId != null);
        if (techId != null) {
            writer.writeTechId(techId);
        }
        writer.writeInt(flags);
        super.write(writer);
        writeVars(params, writer);
    }

    /**
     * Reads ImmutableCell from SnapshotReader.
     * @param reader where to read.
     */
    static ImmutableCell read(IdReader reader) throws IOException {
        CellId cellId = (CellId) reader.readNodeProtoId();
        String groupNameString = reader.readString();
        CellName groupName = CellName.parseName(groupNameString);
        long creationDate = reader.readLong();
        long revisionDate = reader.readLong();
        boolean hasTechId = reader.readBoolean();
        TechId techId = hasTechId ? reader.readTechId() : null;
        int flags = reader.readInt();
        boolean hasVars = reader.readBoolean();
        Variable[] vars = hasVars ? readVars(reader) : Variable.NULL_ARRAY;
        Variable[] params = readVars(reader);
        return new ImmutableCell(cellId, groupName, creationDate, revisionDate, techId, flags, vars, params);
    }

    /**
     * Return a hash code value for fields of this object.
     * Variables of objects are not compared
     */
    public int hashCodeExceptVariables() {
        return cellId.hashCode();
    }

    /**
     * Indicates whether fields of other ImmutableElectricObject are equal to fileds of this object.
     * Variables of objects are not compared.
     * @param o other ImmutableElectricObject.
     * @return true if fields of objects are equal.
     */
    public boolean equalsExceptVariables(ImmutableElectricObject o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ImmutableCell)) {
            return false;
        }
        ImmutableCell that = (ImmutableCell) o;
        return this.cellId == that.cellId && this.groupName == that.groupName
                && this.creationDate == that.creationDate && this.revisionDate == that.revisionDate
                && this.techId == that.techId && this.flags == that.flags && this.params == that.params;
    }

    /**
     * Checks invariant of this ImmutableCell.
     * @throws AssertionError if invariant is broken.
     */
    public void check() {
        super.check(true);
        assert cellId != null;
        assert groupName.getVersion() == 0;
        assert groupName.getView() == View.SCHEMATIC;
        assert techId == null || techId.idManager == cellId.idManager;
        for (int i = 0; i < params.length; i++) {
            Variable param = params[i];
            param.check(true, true);
            assert param.getTextDescriptor().isParam() && param.getTextDescriptor().isInherit();
            if (i > 0) {
                assert params[i - 1].getKey().compareTo(param.getKey()) < 0;
            }
            assert searchVar(param.getKey()) < 0;
        }
        if (params.length > 0) {
            assert paramsAllowed();
            for (Variable var : getVars()) {
                if (var.isAttribute()) {
                    assert searchVar(params, var.getKey()) < 0;
                }
            }
        } else {
            assert params == Variable.NULL_ARRAY;
        }
    }

    /**
     * Tells if parameters are allowed on this ImmutableCell.
     * Currently parameters are allowed only on icon and scheamtic cells.
     * @return true if parameters are allowed on this ImmutableCell
     */
    public boolean paramsAllowed() {
        return cellId.isIcon() || cellId.isSchematic();
    }

    /**
     * Method to return true if bus names are allowed in this Cell
     * @return true if bus names are allowed in this Cell
     */
    public boolean busNamesAllowed() {
        // A hack: bus names are allowed in Clipboard until Clipboard becomes a GUI object
        return cellId.isIcon() || cellId.isSchematic() || cellId.libId.libName.equals("Clipboard!!");
    }

    @Override
    public String toString() {
        return cellId.toString();
    }
}
