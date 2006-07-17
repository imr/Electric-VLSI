/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableExport.java
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

import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;

import java.io.IOException;

/**
 * Immutable class ImmutableExport represents an export.
 */
public class ImmutableExport extends ImmutableElectricObject {
    
    public final static ImmutableExport[] NULL_ARRAY = {};
    public final static ImmutableArrayList<ImmutableExport> EMPTY_LIST = new ImmutableArrayList<ImmutableExport>(NULL_ARRAY);
    
    /** id of this Export. */                                       public final ExportId exportId;
	/** name of this ImmutableExport. */							public final Name name;
	/** The text descriptor of name of ImmutableExport. */          public final TextDescriptor nameDescriptor;
    /** The nodeId of original PortInst. */                         public final int originalNodeId;
    /** The PortProtoId of orignal PortInst. */                     public final PortProtoId originalPortId;
    /** True if this ImmutableExport to be always drawn. */         public final boolean alwaysDrawn;
    /** True to exclude this ImmutableExport from the icon. */      public final boolean bodyOnly;
	/** PortCharacteristic of this ImmutableExport. */              public final PortCharacteristic characteristic;
 
	/**
	 * The private constructor of ImmutableExport. Use the factory "newInstance" instead.
     * @param exportId id of new Export.
	 * @param name name of new ImmutableExport.
     * @param nameDescriptor TextDescriptor of name of this ImmutableExport.
     * @param originalNodeId node id of original PortInst.
     * @param originalPortId port proto id of original PortInst.
     * @param alwaysDrawn true if new ImmutableExport is always drawn.
     * @param bodyOnly true to exclude new ImmutableExport from the icon.
     * @param characteristic PortCharacteristic of new ImmutableExport.
     * @param vars array of Variables of this ImmutableNodeInst
	 */
     ImmutableExport(ExportId exportId, Name name, TextDescriptor nameDescriptor,
             int originalNodeId, PortProtoId originalPortId,
             boolean alwaysDrawn, boolean bodyOnly, PortCharacteristic characteristic, Variable[] vars) {
        super(vars, 0);
        this.exportId = exportId;
        this.name = name;
        this.nameDescriptor = nameDescriptor;
        this.originalNodeId = originalNodeId;
        this.originalPortId = originalPortId;
        this.alwaysDrawn = alwaysDrawn;
        this.bodyOnly = bodyOnly;
        this.characteristic = characteristic;
//        check();
    }

	/**
	 * Returns new ImmutableExport object.
     * @param exportId id of new Export.
	 * @param name name of new ImmutableExport.
     * @param nameDescriptor TextDescriptor of name of this ImmutableExport.
     * @param originalNodeId node id of original PortInst.
     * @param originalPortId port proto id of original PortInst.
     * @param alwaysDrawn true if new ImmutableExport is always drawn.
     * @param bodyOnly true to exclude new ImmutableExport from the icon.
     * @param characteristic PortCharacteristic of new ImmutableExport.
	 * @return new ImmutableExport object.
	 * @throws NullPointerException if exportId, name, originalPortId is null.
     * @throws IllegalArgumentException if originalNodeId is bad.
	 */
    public static ImmutableExport newInstance(ExportId exportId, Name name, TextDescriptor nameDescriptor,
             int originalNodeId, PortProtoId originalPortId,
             boolean alwaysDrawn, boolean bodyOnly, PortCharacteristic characteristic) {
		if (exportId == null) throw new NullPointerException("exportId");
		if (name == null) throw new NullPointerException("name");
        if (!name.isValid() || name.hasEmptySubnames() || name.isTempname()) throw new IllegalArgumentException("name");
        if (nameDescriptor != null)
            nameDescriptor = nameDescriptor.withDisplayWithoutParamAndCode();
        if (originalNodeId < 0) throw new IllegalArgumentException("originalNodeId");
        if (originalPortId == null) throw new NullPointerException("orignalPortId");
        if (characteristic == null) characteristic = PortCharacteristic.UNKNOWN;
		return new ImmutableExport(exportId, name, nameDescriptor,
                originalNodeId, originalPortId, alwaysDrawn, bodyOnly, characteristic, Variable.NULL_ARRAY);
    }

	/**
	 * Returns ImmutableExport which differs from this ImmutableExport by name.
	 * @param name export name key.
	 * @return ImmutableExport which differs from this ImmutableExport by name.
	 * @throws NullPointerException if name is null
	 */
	public ImmutableExport withName(Name name) {
		if (this.name.toString().equals(name.toString())) return this;
		if (name == null) throw new NullPointerException("name");
        if (!name.isValid() || name.hasEmptySubnames() || name.isTempname()) throw new IllegalArgumentException("name");
		return new ImmutableExport(this.exportId, name, this.nameDescriptor,
                this.originalNodeId, this.originalPortId, this.alwaysDrawn, this.bodyOnly, this.characteristic, getVars());
	}

	/**
	 * Returns ImmutableExport which differs from this ImmutableExport by name descriptor.
     * @param nameDescriptor TextDescriptor of name
	 * @return ImmutableExport which differs from this ImmutableExport by name descriptor.
	 */
	public ImmutableExport withNameDescriptor(TextDescriptor nameDescriptor) {
        if (nameDescriptor != null)
            nameDescriptor = nameDescriptor.withDisplayWithoutParamAndCode();
        if (this.nameDescriptor == nameDescriptor) return this;
		return new ImmutableExport(this.exportId, this.name, nameDescriptor,
                this.originalNodeId, this.originalPortId, this.alwaysDrawn, this.bodyOnly, this.characteristic, getVars());
	}

	/**
	 * Returns ImmutableExport which differs from this ImmutableExport by original port.
     * @param originalNodeId node id of original PortInst.
     * @param originalPortId port proto id of original PortInst.
	 * @return ImmutableExport which differs from this ImmutableExport by original port.
     * @throws NullPointerException if originalPortId is null.
	 */
	public ImmutableExport withOriginalPort(int originalNodeId, PortProtoId originalPortId) {
        if (this.originalNodeId == originalNodeId && this.originalPortId == originalPortId) return this;
        if (originalPortId == null) throw new NullPointerException("originalPortId");
		return new ImmutableExport(this.exportId, this.name, this.nameDescriptor,
                originalNodeId, originalPortId, this.alwaysDrawn, this.bodyOnly, this.characteristic, getVars());
	}

	/**
	 * Returns ImmutableExport which differs from this ImmutableExport by alwaysDrawn flag.
     * @param alwaysDrawn true if new ImmutableExport is always drawn.
	 * @return ImmutableExport which differs from this ImmutableExport by alwaysDrawn flag.
	 */
	public ImmutableExport withAlwaysDrawn(boolean alwaysDrawn) {
        if (this.alwaysDrawn == alwaysDrawn) return this;
		return new ImmutableExport(this.exportId, this.name, this.nameDescriptor,
                this.originalNodeId, this.originalPortId, alwaysDrawn, this.bodyOnly, this.characteristic, getVars());
	}

	/**
	 * Returns ImmutableExport which differs from this ImmutableExport by bodyOnly flag.
     * @param bodyOnly true to exclude new ImmutableExport from the icon.
	 * @return ImmutableExport which differs from this ImmutableExport by bodyOnly flag.
	 */
	public ImmutableExport withBodyOnly(boolean bodyOnly) {
        if (this.bodyOnly == bodyOnly) return this;
		return new ImmutableExport(this.exportId, this.name, this.nameDescriptor,
                this.originalNodeId, this.originalPortId, this.alwaysDrawn, bodyOnly, this.characteristic, getVars());
	}

	/**
	 * Returns ImmutableExport which differs from this ImmutableExport by port characteristic.
     * @param characteristic PortCharacteristic of new ImmutableExport.
	 * @return ImmutableExport which differs from this ImmutableExport by port characteristic.
	 */
	public ImmutableExport withCharacteristic(PortCharacteristic characteristic) {
        if (characteristic == null) characteristic = PortCharacteristic.UNKNOWN;
        if (this.characteristic == characteristic) return this;
		return new ImmutableExport(this.exportId, this.name, this.nameDescriptor,
                this.originalNodeId, this.originalPortId, this.alwaysDrawn, this.bodyOnly, characteristic, getVars());
	}

	/**
	 * Returns ImmutableExport which differs from this ImmutableExport by additional Variable.
     * If this ImmutableExport has Variable with the same key as new, the old variable will not be in new
     * ImmutableExport.
	 * @param var additional Variable.
	 * @return ImmutableExport with additional Variable.
	 * @throws NullPointerException if var is null
	 */
    public ImmutableExport withVariable(Variable var) {
        Variable[] vars = arrayWithVariable(var.withParam(false));
        if (this.getVars() == vars) return this;
		return new ImmutableExport(this.exportId, this.name, this.nameDescriptor,
                this.originalNodeId, this.originalPortId, this.alwaysDrawn, this.bodyOnly, this.characteristic, vars);
    }
    
	/**
	 * Returns ImmutableExport which differs from this ImmutableExport by removing Variable
     * with the specified key. Returns this ImmutableExport if it doesn't contain variable with the specified key.
	 * @param key Variable Key to remove.
	 * @return ImmutableExport without Variable with the specified key.
	 * @throws NullPointerException if key is null
	 */
    public ImmutableExport withoutVariable(Variable.Key key) {
        Variable[] vars = arrayWithoutVariable(key);
        if (this.getVars() == vars) return this;
		return new ImmutableExport(this.exportId, this.name, this.nameDescriptor,
                this.originalNodeId, this.originalPortId, this.alwaysDrawn, this.bodyOnly, this.characteristic, vars);
    }
    
	/**
	 * Returns ImmutableExport which differs from this ImmutableExport by renamed Ids.
	 * @param idMapper a map from old Ids to new Ids.
     * @return ImmutableExport with renamed Ids.
	 */
    ImmutableExport withRenamedIds(IdMapper idMapper) {
        Variable[] vars = arrayWithRenamedIds(idMapper);
        ExportId exportId = idMapper.get(this.exportId);
        PortProtoId originalPortId = this.originalPortId;
        if (originalPortId instanceof ExportId)
            originalPortId = idMapper.get((ExportId)originalPortId);
        if (getVars() == vars && this.exportId == exportId && this.originalPortId == this.originalPortId) return this;
		return new ImmutableExport(exportId, this.name, this.nameDescriptor,
                this.originalNodeId, originalPortId, this.alwaysDrawn, this.bodyOnly, this.characteristic, vars);
    }
    
    /**
     * Writes this ImmutableArcInst to SnapshotWriter.
     * @param writer where to write.
     */
    void write(SnapshotWriter writer) throws IOException {
        writer.writePortProtoId(exportId);
        writer.writeNameKey(name);
        writer.writeTextDescriptor(nameDescriptor);
        writer.writeNodeId(originalNodeId);
        writer.writePortProtoId(originalPortId);
        writer.writeBoolean(alwaysDrawn);
        writer.writeBoolean(bodyOnly);
        writer.writeInt(characteristic.getBits());
        super.write(writer);
    }
    
    /**
     * Reads ImmutableExport from SnapshotReader.
     * @param reader where to read.
     */
    static ImmutableExport read(SnapshotReader reader) throws IOException {
        ExportId exportId = (ExportId)reader.readPortProtoId();
        Name name = reader.readNameKey();
        TextDescriptor nameDescriptor = reader.readTextDescriptor();
        int originalNodeId = reader.readNodeId();
        PortProtoId originalPortId = reader.readPortProtoId();
        boolean alwaysDrawn = reader.readBoolean();
        boolean bodyOnly = reader.readBoolean();
        int bits = reader.readInt();
        PortCharacteristic characteristic = PortCharacteristic.findCharacteristic(bits);
        boolean hasVars = reader.readBoolean();
        Variable[] vars = hasVars ? readVars(reader) : Variable.NULL_ARRAY;
        return new ImmutableExport(exportId, name, nameDescriptor,
                originalNodeId, originalPortId, alwaysDrawn, bodyOnly, characteristic, vars);
    }
    
    /**
     * Return a hash code value for fields of this object.
     * Variables of objects are not compared
     */
    public int hashCodeExceptVariables() { return exportId.hashCode(); }

    /**
     * Indicates whether fields of other ImmutableElectricObject are equal to fileds of this object.
     * Variables of objects are not compared.
     * @param o other ImmutableElectricObject.
     * @return true if fields of objects are equal.
     */
    public boolean equalsExceptVariables(ImmutableElectricObject o) {
        if (this == o) return true;
        if (!(o instanceof ImmutableExport)) return false;
        ImmutableExport that = (ImmutableExport)o;
        return this.exportId == that.exportId && this.name == that.name && this.nameDescriptor == that.nameDescriptor &&
                this.originalNodeId == that.originalNodeId && this.originalPortId == that.originalPortId &&
                this.alwaysDrawn == that.alwaysDrawn && this.bodyOnly == that.bodyOnly &&
                this.characteristic == that.characteristic;
    }
    
    /**
     * Returns name key of string if string is a valid Export name, null if not.
     * @param name string to test.
     * @return name key or null.
     */
    public static Name validExportName(String name) {
        if (name == null) return null;
        Name nameKey = Name.findName(name);
        return nameKey.isValid() && !nameKey.isTempname() && !nameKey.hasEmptySubnames() ? nameKey : null;
    }
    
    /**
	 * Checks invariant of this ImmutableExport.
	 * @throws AssertionError if invariant is broken.
	 */
	public void check() {
        check(false);
		assert exportId != null;
		assert name != null;
        assert name.isValid() && !name.hasEmptySubnames() && !name.isTempname();
        if (nameDescriptor != null)
            assert nameDescriptor.isDisplay() && !nameDescriptor.isCode() && !nameDescriptor.isParam();
        assert originalNodeId >= 0;
        assert originalPortId != null;
        assert characteristic != null;
	}
}
