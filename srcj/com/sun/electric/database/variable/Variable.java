/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Variable.java
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
package com.sun.electric.database.variable;

import com.sun.electric.database.IdMapper;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.ArcProtoId;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.ExportId;
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.ActivityLogger;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

/**
 * The Variable immutable class defines a single attribute-value pair that can be attached to any ElectricObject.
 * <P>
 * This immutable class is thread-safe.
 */
public class Variable implements Serializable {

    /**
     * The Key class caches Variable names.
     */
    public static class Key implements Comparable<Key> {

        private final String name;

        /**
         * Method to create a new Key object with the specified name.
         * @param name the name of the Variable.
         * @throws NullPointerException if name is null.
         */
        private Key(String name) {
            if (name == null) {
                throw new NullPointerException();
            }
            this.name = name;
        }

        /**
         * Method to return the name of this Key object.
         * @return the name of this Key object.
         */
        public String getName() {
            return name;
        }

        /**
         * Method to return if this Variable.Key is a User Attribute.
         * @return true if this Variable.Key is an attribute, false otherwise.
         */
        public boolean isAttribute() {
            return false;
        }

        /**
         * Compares Variable Keys by their names.
         * @param that the other Variable Key.
         * @return a comparison between the Variable Keys.
         */
        public int compareTo(Key that) {
            return TextUtils.STRING_NUMBER_ORDER.compare(name, that.name);
        }

        /**
         * Returns a printable version of this Key.
         * @return a printable version of this Key.
         */
        @Override
        public String toString() {
            return name;
        }

        /**
         * Print statistics about Variable Keys.
         */
        public static void printStatistics() {
            long keyLength = 0;
            for (Key key : varKeys.values()) {
                keyLength += key.getName().length();
            }
//            int canonicCount = 0;
//            long canonicLength = 0;
//            for (String canonic : varCanonicKeys.keySet()) {
//                Key key = varCanonicKeys.get(canonic);
//                if (key != null && key.getName() == canonic) {
//                    continue;
//                }
//                canonicCount++;
//                canonicLength += canonic.length();
//            }
            System.out.println(varKeys.size() + " variable keys with " + keyLength + " chars.");
//                    + " Canonic " + varCanonicKeys.size() + " entries " + canonicCount + " strings with " + canonicLength + " chars.");
        }
    }

    /**
     * The Key class caches attribute Variable names.
     */
    public static class AttrKey extends Key {

        AttrKey(String name) {
            super(name);
        }

        /**
         * Method to return if this Variable.Key is a User Attribute.
         * @return true if this Variable.Key is an attribute, false otherwise.
         */
        @Override
        public boolean isAttribute() {
            return true;
        }
    }
    /** a list of all variable keys */
    private static final HashMap<String, Key> varKeys = new HashMap<String, Key>();
//    /** all variable keys addressed by lower case name */
//    private static final HashMap<String, Key> varCanonicKeys = new HashMap<String, Key>();

    /**
     * Method to return the Key object for a given Variable name.
     * Variable Key objects are caches of the actual string name of the Variable.
     * @return the Key object for a given Variable name.
     */
    public static synchronized Key findKey(String name) {
        Key key = varKeys.get(name);
        if (key != null) {
            return key;
        }
//        if (varKeys.containsKey(name)) {
//            return null;
//        }
//        name = name.intern();
//        varKeys.put(name, null);
//        String canonicName = TextUtils.canonicString(name);
//        key = varCanonicKeys.get(canonicName);
//        if (key != null) {
//            String msg = "WARNING: Variable \"" + name + "\" not found though variable \"" + key.getName() + "\" exists";
//            ActivityLogger.logMessage(msg);
//            System.out.println(msg);
//        }
        return null;
    }

    /**
     * Method to find or create the Key object for a given Variable name.
     * Variable Key objects are caches of the actual string name of the Variable.
     * @param name given Variable name.
     * @return the Key object for a given Variable name.
     */
    public static synchronized Key newKey(String name) {
        Key key = varKeys.get(name);
        if (key != null) {
            return key;
        }
        return newKey(name, null);
    }

    /**
     * Method to find or create the Key object for a given Variable name.
     * Variable Key objects are caches of the actual string name of the Variable.
     * @param name given Variable name.
     * @param parent the object on which this Variable key will reside.
     * @return the Key object for a given Variable name.
     */
    public static synchronized Key newKey(String name, ElectricObject parent) {
        Key key = varKeys.get(name);
        if (key != null) {
            return key;
        }
        name = name.intern();
        key = name.startsWith("ATTR_") ? new AttrKey(name) : new Key(name);
        varKeys.put(name, key);
//        String canonicName = TextUtils.canonicString(name);
//        Key key2 = varCanonicKeys.get(canonicName);
//        if (key2 != null && parent != null) {
//            // find examples of these two variables
//            String ex = null;
//            for (Library lib : Library.getVisibleLibraries()) {
//                if (ex == null) {
//                    ex = findVariable(lib, key2);
//                }
//                for (Iterator<Cell> cIt = lib.getCells(); cIt.hasNext();) {
//                    Cell cell = cIt.next();
//                    if (ex == null) {
//                        ex = findVariable(cell, key2);
//                    }
//                    for (Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext();) {
//                        NodeInst ni = nIt.next();
//                        if (ex == null) {
//                            ex = findVariable(ni, key2);
//                        }
//                    }
//                    for (Iterator<ArcInst> aIt = cell.getArcs(); aIt.hasNext();) {
//                        ArcInst ai = aIt.next();
//                        if (ex == null) {
//                            ex = findVariable(ai, key2);
//                        }
//                    }
//                    for (Iterator<Export> eIt = cell.getExports(); eIt.hasNext();) {
//                        Export e = eIt.next();
//                        if (ex == null) {
//                            ex = findVariable(e, key2);
//                        }
//                    }
//                }
//            }
//            String msg = "WARNING: Variables with similar names are used: \"" + name + "\"";
//            if (parent != null) {
//                msg += " (on " + getObjectName(parent) + ")";
//            }
//            msg += " and \"" + key2.getName() + "\"";
//            if (ex != null) {
//                msg += " (on " + ex + ")";
//            }
//            ActivityLogger.logMessage(msg);
//            System.out.println(msg);
//        } else {
//            varCanonicKeys.put(canonicName.intern(), key);
//        }
        return key;
    }

    private static String findVariable(ElectricObject eObj, Key key) {
        for (Iterator<Variable> it = eObj.getParametersAndVariables(); it.hasNext();) {
            Variable var = it.next();
            if (var.getKey() == key) {
                return getObjectName(eObj);
            }
        }
        return null;
    }

    private static String getObjectName(ElectricObject eObj) {
        if (eObj instanceof Library) {
            return "Library " + ((Library) eObj).getName();
        }
        if (eObj instanceof Cell) {
            return "Cell " + ((Cell) eObj).describe(false);
        }
        if (eObj instanceof Export) {
            return "Export " + ((Export) eObj).getName() + " in Cell " + ((Export) eObj).getParent().describe(false);
        }
        if (eObj instanceof NodeInst) {
            return "Node " + ((NodeInst) eObj).describe(false) + " in Cell " + ((NodeInst) eObj).getParent().describe(false);
        }
        if (eObj instanceof ArcInst) {
            return "Arc " + ((ArcInst) eObj).describe(false) + " in Cell " + ((ArcInst) eObj).getParent().describe(false);
        }
        return eObj.toString();
    }
    /** empty array of Variables. */
    public static final Variable[] NULL_ARRAY = {};
    /** type if value. */
    private final static byte ARRAY = 1;
    private final static byte LIBRARY = 2;
    private final static byte CELL = 4;
    private final static byte EXPORT = 6;
    private final static byte STRING = 8;
    private final static byte CODE = 10;
    private final static byte DOUBLE = 12;
    private final static byte FLOAT = 14;
    private final static byte LONG = 16;
    private final static byte INTEGER = 18;
    private final static byte SHORT = 20;
    private final static byte BYTE = 22;
    private final static byte BOOLEAN = 24;
    private final static byte EPOINT = 26;
    private final static byte TOOL = 28;
    private final static byte TECHNOLOGY = 30;
    private final static byte PRIM_NODE = 32;
    private final static byte ARC_PROTO = 34;
    /** Valid type of value. */
    private static final HashMap<Class, Byte> validClasses = new HashMap<Class, Byte>();

    static {
        validClasses.put(LibId.class, Byte.valueOf(LIBRARY));
        validClasses.put(CellId.class, Byte.valueOf(CELL));
        validClasses.put(ExportId.class, Byte.valueOf(EXPORT));
        validClasses.put(String.class, Byte.valueOf(STRING));
        validClasses.put(CodeExpression.class, Byte.valueOf(CODE));
        validClasses.put(Double.class, Byte.valueOf(DOUBLE));
        validClasses.put(Float.class, Byte.valueOf(FLOAT));
        validClasses.put(Long.class, Byte.valueOf(LONG));
        validClasses.put(Integer.class, Byte.valueOf(INTEGER));
        validClasses.put(Short.class, Byte.valueOf(SHORT));
        validClasses.put(Byte.class, Byte.valueOf(BYTE));
        validClasses.put(Boolean.class, Byte.valueOf(BOOLEAN));
        validClasses.put(EPoint.class, Byte.valueOf(EPOINT));
        validClasses.put(Tool.class, Byte.valueOf(TOOL));
        validClasses.put(TechId.class, Byte.valueOf(TECHNOLOGY));
        validClasses.put(PrimitiveNodeId.class, Byte.valueOf(PRIM_NODE));
        validClasses.put(ArcProtoId.class, Byte.valueOf(ARC_PROTO));
    }
    /** key of this Variable. */
    private final Key key;
    /** Value of this Variable. */
    private final Object value;
    /** Text descriptor of this Variable. */
    private final TextDescriptor descriptor;
    private final byte type;

    /**
     * Constructor of Variable.
     * @param key key of this Variable.
     * @param descriptor text descriptor of this Variable.
     * @param value value of this Variable.
     * @param type type of the value
     */
    private Variable(Key key, Object value, TextDescriptor descriptor, byte type) {
        this.key = key;
        this.value = value;
        this.descriptor = descriptor;
        this.type = type;
        check(true, true);
    }

    /**
     * Returns new Variable.
     * @param key key of this Variable.
     * @param value value of this Variable.
     * @param descriptor text descriptor of this Variable.
     * @return new Variable object.
     * @throws NullPointerException if key, descriptor or value is null.
     * @throws IllegalArgumentException if value has invalid type
     */
    public static Variable newInstance(Variable.Key key, Object value, TextDescriptor descriptor) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (descriptor == null) {
            throw new NullPointerException("descriptor");
        }
        if (!key.isAttribute()) {
            descriptor = descriptor.withParam(false);
        }
        byte type = getObjectType(value);
        if ((type & ARRAY) != 0) {
            value = ((Object[]) value).clone();
        }
        return new Variable(key, value, descriptor, type);
    }

    /**
     * Returns Object of specified Code derived from specified Object
     * If code is not NONE, the new Object will be of type CodeExpression.
     * If code is NONE and the specified Object was CodeExpression,
     * the new Object will be a String with expression text.
     * @param value specified Object
     * @param code code of new Object.
     * @return Object of specified Code derived from specified Object
     */
    public static Object withCode(Object value, CodeExpression.Code code) {
        if (code == CodeExpression.Code.NONE) {
            return value instanceof CodeExpression ? value.toString() : value;
        }

        if (value instanceof CodeExpression && ((CodeExpression) value).getCode() == code) {
            return value;
        }

        String expr;
        if (value instanceof Object[]) {
            StringBuilder sb = new StringBuilder();
            for (Object o : (Object[]) value) {
                if (o == null) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(o.toString());
            }
            expr = sb.toString();
        } else {
            expr = value.toString();
        }
        return CodeExpression.valueOf(expr, code);
    }

    /**
     * Checks invariant of this Variable.
     * @param paramAllowed true if paramerer flag is allowed on this Variable
     * @param inheritAllowed true if inherit flag is allowed on this Variable
     * @throws AssertionError or NullPointerException if invariant is broken.
     */
    public void check(boolean paramAllowed, boolean inheritAllowed) {
        assert key != null;
        assert value != null;
        assert type == getObjectType(value);
        assert descriptor != null;
        if (descriptor.isParam()) {
            assert paramAllowed && isAttribute();
        }
        if (descriptor.isInherit()) {
            assert inheritAllowed;
        }
    }

    private static byte getObjectType(Object value) {
        byte type;
        if (value instanceof Object[]) {
            Byte typeByte = validClasses.get(value.getClass().getComponentType());
            if (typeByte == null) {
                throw new IllegalArgumentException(value.getClass().toString());
            }
            type = (byte) (typeByte.byteValue() | ARRAY);
            if (!validValue(type, value)) {
                throw new IllegalArgumentException(value.toString());
            }
        } else {
            Byte typeByte = validClasses.get(value.getClass());
            if (typeByte == null) {
                throw new IllegalArgumentException(value.getClass().toString());
            }
            type = typeByte.byteValue();
        }
        return type;
    }

    private static boolean validValue(byte type, Object value) {
        if ((type & ARRAY) == 0) {
            return value != null;
        }
        type = (byte) (type & ~ARRAY);
        if (type == CODE) {
            return false;
        }
        Object[] valueArr = (Object[]) value;
        if (type >= STRING && type <= BOOLEAN) {
            for (Object o : valueArr) {
                if (o == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns true if the value is array,
     * @return true if the value is array,
     */
    public boolean isArray() {
        return (type & ARRAY) != 0;
    }

    /**
     * Get the number of entries stored in this Variable.
     * For non-arrayed Variables, this is 1.
     * @return the number of entries stored in this Variable.
     */
    public int getLength() {
        return (type & ARRAY) != 0 ? ((Object[]) value).length : 1;
    }

    /**
     * Returns thread-independent value of this Variable.
     * @return thread-independent value of this variable.
     */
    public Object getObject() {
        return (type & ARRAY) != 0 ? ((Object[]) value).clone() : value;
    }

    /**
     * Write this Variable to IdWriter.
     * @param writer where to write.
     */
    public void write(IdWriter writer) throws IOException {
        writer.writeVariableKey(key);
        writer.writeTextDescriptor(descriptor);
        writeObject(writer, value, type);
    }

    /**
     * Write Object to IdWriter.
     * Object must be a valid value of Variable
     * @param writer where to write.
     * @param obj
     */
    public static void writeObject(IdWriter writer, Object obj) throws IOException {
        writeObject(writer, obj, getObjectType(obj));
    }

    private static void writeObject(IdWriter writer, Object obj, byte type) throws IOException {
        writer.writeByte(type);
        if (obj instanceof Object[]) {
            Object[] array = (Object[]) obj;
            writer.writeInt(array.length);
            for (Object o : array) {
                writer.writeBoolean(o != null);
                if (o != null) {
                    writeObj(writer, o, type);
                }
            }
        } else {
            writeObj(writer, obj, type);
        }
    }

    private static void writeObj(IdWriter writer, Object obj, byte type) throws IOException {
        switch (type & ~ARRAY) {
            case LIBRARY:
                writer.writeLibId((LibId) obj);
                break;
            case CELL:
                writer.writeNodeProtoId((CellId) obj);
                break;
            case EXPORT:
                writer.writePortProtoId((ExportId) obj);
                break;
            case STRING:
                writer.writeString((String) obj);
                break;
            case CODE:
                ((CodeExpression) obj).write(writer);
                break;
            case DOUBLE:
                writer.writeDouble(((Double) obj).doubleValue());
                break;
            case FLOAT:
                writer.writeFloat(((Float) obj).floatValue());
                break;
            case LONG:
                writer.writeLong(((Long) obj).longValue());
                break;
            case INTEGER:
                writer.writeInt(((Integer) obj).intValue());
                break;
            case SHORT:
                writer.writeShort(((Short) obj).shortValue());
                break;
            case BYTE:
                writer.writeByte(((Byte) obj).byteValue());
                break;
            case BOOLEAN:
                writer.writeBoolean(((Boolean) obj).booleanValue());
                break;
            case EPOINT:
                writer.writePoint((EPoint) obj);
                break;
            case TOOL:
                writer.writeTool((Tool) obj);
                break;
            case TECHNOLOGY:
                writer.writeTechId((TechId) obj);
                break;
            case PRIM_NODE:
                writer.writeNodeProtoId((PrimitiveNodeId) obj);
                break;
            case ARC_PROTO:
                writer.writeArcProtoId((ArcProtoId) obj);
                break;
        }
    }

    /**
     * Read Variable from IdReader.
     * @param reader from to read.
     * @return Variable read
     */
    public static Variable read(IdReader reader) throws IOException {
        Variable.Key varKey = reader.readVariableKey();
        TextDescriptor td = reader.readTextDescriptor();
        Object value = readObject(reader);
        return Variable.newInstance(varKey, value, td);
    }

    /**
     * Read Object from IdReader. Object will be a valid value of Variable
     * @param reader from to read
     * @return Object read
     */
    public static Object readObject(IdReader reader) throws IOException {
        int type = reader.readByte();
        Object value;
        if ((type & ARRAY) != 0) {
            int length = reader.readInt();
            type &= ~ARRAY;
            Object[] array;
            switch (type) {
                case LIBRARY:
                    array = new LibId[length];
                    break;
                case CELL:
                    array = new CellId[length];
                    break;
                case EXPORT:
                    array = new ExportId[length];
                    break;
                case STRING:
                    array = new String[length];
                    break;
                case DOUBLE:
                    array = new Double[length];
                    break;
                case FLOAT:
                    array = new Float[length];
                    break;
                case LONG:
                    array = new Long[length];
                    break;
                case INTEGER:
                    array = new Integer[length];
                    break;
                case SHORT:
                    array = new Short[length];
                    break;
                case BYTE:
                    array = new Byte[length];
                    break;
                case BOOLEAN:
                    array = new Boolean[length];
                    break;
                case EPOINT:
                    array = new EPoint[length];
                    break;
                case TOOL:
                    array = new Tool[length];
                    break;
                case TECHNOLOGY:
                    array = new TechId[length];
                    break;
                case PRIM_NODE:
                    array = new PrimitiveNodeId[length];
                    break;
                case ARC_PROTO:
                    array = new ArcProtoId[length];
                    break;
                case CODE:
                default:
                    throw new IOException("type");
            }
            for (int i = 0; i < length; i++) {
                boolean hasElem = reader.readBoolean();
                if (hasElem) {
                    array[i] = readObj(reader, type);
                }
            }
            value = array;
        } else {
            value = readObj(reader, type);
        }
        return value;
    }

    private static Object readObj(IdReader reader, int type) throws IOException {
        switch (type) {
            case LIBRARY:
                return reader.readLibId();
            case CELL:
                return reader.readNodeProtoId();
            case EXPORT:
                return reader.readPortProtoId();
            case STRING:
                return reader.readString();
            case CODE:
                return CodeExpression.read(reader);
            case DOUBLE:
                return Double.valueOf(reader.readDouble());
            case FLOAT:
                return Float.valueOf(reader.readFloat());
            case LONG:
                return Long.valueOf(reader.readLong());
            case INTEGER:
                return Integer.valueOf(reader.readInt());
            case SHORT:
                return Short.valueOf(reader.readShort());
            case BYTE:
                return Byte.valueOf(reader.readByte());
            case BOOLEAN:
                return Boolean.valueOf(reader.readBoolean());
            case EPOINT:
                return reader.readPoint();
            case TOOL:
                return reader.readTool();
            case TECHNOLOGY:
                return reader.readTechId();
            case PRIM_NODE:
                return reader.readNodeProtoId();
            case ARC_PROTO:
                return reader.readArcProtoId();
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Returns Variable which differs from this Variable by value.
     * @param value value of new Variable.
     * @return Variable which differs from this Variable by value.
     * @throws NullPointerException if value is null.
     * @throws IllegalArgumentException if value has invalid type
     */
    public Variable withObject(Object value) {
        if (this.value.equals(value)) {
            return this;
        }
        if ((type & ARRAY) != 0 && value instanceof Object[]
                && Arrays.equals((Object[]) this.value, (Object[]) value)
                && this.value.getClass().getComponentType() == value.getClass().getComponentType()) {
            return this;
        }
        return newInstance(this.key, value, this.descriptor);
    }

    /**
     * Returns Variable which differs from this Variable by text value.
     * If this Variable is Code Varibale its Code type is preserved
     * @param text text value of new Variable.
     * @return Variable which differs from this Variable by value.
     * @throws NullPointerException if value is null.
     * @throws IllegalArgumentException if value has invalid type
     */
    public Variable withText(String text) {
        if (value instanceof CodeExpression) {
            CodeExpression ce = (CodeExpression) value;
            if (ce.getExpr().equals(text)) {
                return this;
            }
            return new Variable(this.key, CodeExpression.valueOf(text, ce.getCode()), this.descriptor, this.type);
        }
        if (value.equals(text)) {
            return this;
        }
        return new Variable(this.key, text, this.descriptor, STRING);
    }

    /**
     * Returns Variable which differs from this Variable by code.
     * @param code code of new Variable.
     * @return Variable which differs from this Variable by code
     */
    public Variable withCode(CodeExpression.Code code) {
        if (getCode() == code) {
            return this;
        }
        return withObject(withCode(value, code));
    }

    /**
     * Returns Variable which differs from this Variable by renamed Ids.
     * @param idMapper a mapper from old Ids to new Ids.
     * @return Variable which differs from this Variable by renamed Ids.
     */
    public Variable withRenamedIds(IdMapper idMapper) {
        Object newValue = withRenamedIds(idMapper, value, type);
        return newValue != value ? withObject(newValue) : this;
    }

    private static Object withRenamedIds(IdMapper idMapper, Object value, byte type) {
        Object newValue = value;
        switch (type) {
            case LIBRARY:
                newValue = idMapper.get((LibId) value);
                break;
            case CELL:
                newValue = idMapper.get((CellId) value);
                break;
            case EXPORT:
                newValue = idMapper.get((ExportId) value);
                break;
            case LIBRARY | ARRAY:
                LibId[] libIds = (LibId[]) value;
                for (int i = 0; i < libIds.length; i++) {
                    if (libIds[i] == null) {
                        continue;
                    }
                    LibId libId = idMapper.get(libIds[i]);
                    if (libId != libIds[i]) {
                        libIds[i] = libId;
                        newValue = libIds;
                    }
                }
                break;
            case CELL | ARRAY:
                CellId[] cellIds = (CellId[]) value;
                for (int i = 0; i < cellIds.length; i++) {
                    if (cellIds[i] == null) {
                        continue;
                    }
                    CellId cellId = idMapper.get(cellIds[i]);
                    if (cellId != cellIds[i]) {
                        cellIds[i] = cellId;
                        newValue = cellIds;
                    }
                }
                break;
            case EXPORT | ARRAY:
                ExportId[] exportIds = (ExportId[]) value;
                for (int i = 0; i < exportIds.length; i++) {
                    if (exportIds[i] == null) {
                        continue;
                    }
                    ExportId exportId = idMapper.get(exportIds[i]);
                    if (exportId != exportIds[i]) {
                        exportIds[i] = exportId;
                        newValue = exportIds;
                    }
                }
                break;
        }
        return newValue;
    }

    /**
     * Returns thread-independent element of array value of this Variable.
     * @param index index of array
     * @return element of array value.
     * @throws ArrayIndexOutOfBoundsException if index is scalar of value is out of bounds.
     */
    public Object getObject(int index) {
        if ((type & ARRAY) == 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return ((Object[]) value)[index];
    }

    /**
     * Method to return the Variable Key associated with this Variable.
     * @return the Variable Key associated with this variable.
     */
    public Key getKey() {
        return key;
    }

    /**
     * Returns true if variable is linked to a linked owner, false otherwise.
     * @param owner owner of this variable.
     * @return true if variable is linked to a linked owner, false otherwise.
     */
    public boolean isLinked(ElectricObject owner) {
        return owner.isLinked() && owner.getParameterOrVariable(getKey()) == this;
    }

    /**
     * Method to return a more readable name for this Variable.
     * The method adds "Parameter" or "Attribute" as appropriate
     * and uses sensible names such as "Diode Size" instead of "SCHEM_diode".
     * @return a more readable name for this Variable.
     */
    public String getReadableName(ElectricObject owner) {
        String trueName = "";
        String name = getKey().getName();
        if (isAttribute()) {
            if (owner.isParam(getKey())) {
                trueName += "Parameter '" + name.substring(5) + "'";
            } else {
                trueName += "Attribute '" + name.substring(5) + "'";
            }
        } else {
            String betterName = betterVariableName(name);
            if (betterName != null) {
                trueName += betterName;
            } else {
                trueName += "Variable '" + name + "'";
            }
        }
//		unitname = us_variableunits(var);
//		if (unitname != 0) formatinfstr(infstr, x_(" (%s)"), unitname);
        return trueName;
    }

    /**
     * Method to return a full description of this Variable.
     * The description includes the object on which this Variable resides.
     * @return a full description of this Variable.
     */
    public String getFullDescription(ElectricObject eobj) {
        String trueName = getReadableName(eobj);
        String description = null;
        if (eobj instanceof Export) {
            description = trueName + " on " + eobj;
        } else if (eobj instanceof PortInst) {
            PortInst pi = (PortInst) eobj;
            description = trueName + " on " + pi.getPortProto()
                    + " of " + pi.getNodeInst().describe(true);
        } else if (eobj instanceof ArcInst) {
            description = trueName + " on " + eobj;
        } else if (eobj instanceof NodeInst) {
            NodeInst ni = (NodeInst) eobj;
            description = trueName + " on " + ni;
            if (ni.getProto() == Generic.tech().invisiblePinNode) {
                String varName = getKey().getName();
                String betterName = betterVariableName(varName);
                if (betterName != null) {
                    description = betterName;
                }
            }
        } else if (eobj instanceof Cell) {
            description = trueName + " of " + eobj;
        }
        return description;
    }

    /**
     * Method to convert the standard Variable names to more readable strings.
     * @param name the actual Variable name.
     * @return a better name for it (returns the same name if no better one exists).
     */
    public static String betterVariableName(String name) {
        // handle standard variable names
        if (name.equals("ARC_name")) {
            return "Arc Name";
        }
        if (name.equals("ARC_radius")) {
            return "Arc Radius";
        }
        if (name.equals("ART_color")) {
            return "Color";
        }
        if (name.equals("ART_degrees")) {
            return "Number of Degrees";
        }
        if (name.equals("ART_message")) {
            return "Annotation text";
        }
        if (name.equals("NET_ncc_match")) {
            return "NCC equivalence";
        }
        if (name.equals("NET_ncc_forcedassociation")) {
            return "NCC association";
        }
        if (name.equals("NODE_name")) {
            return "Node Name";
        }
        if (name.equals("SCHEM_capacitance")) {
            return "Capacitance";
        }
        if (name.equals("SCHEM_diode")) {
            return "Diode Size";
        }
        if (name.equals("SCHEM_global_name")) {
            return "Global Signal Name";
        }
        if (name.equals("SCHEM_inductance")) {
            return "Inductance";
        }
        if (name.equals("SCHEM_resistance")) {
            return "Resistance";
        }
        if (name.equals("SIM_fall_delay")) {
            return "Fall Delay";
        }
        if (name.equals("SIM_fasthenry_group_name")) {
            return "FastHenry Group";
        }
        if (name.equals("SIM_rise_delay")) {
            return "Rise Delay";
        }
        if (name.equals("SIM_spice_card")) {
            return "SPICE code";
        }
        if (name.equals("SIM_spice_declaration")) {
            return "SPICE declaration";
        }
        if (name.equals("SIM_spice_model")) {
            return "SPICE model";
        }
        if (name.equals("SIM_verilog_wire_type")) {
            return "Verilog Wire type";
        }
        if (name.equals("SIM_weak_node")) {
            return "Transistor Strength";
        }
        if (name.equals("transistor_width")) {
            return "Transistor Width";
        }
        if (name.equals("VERILOG_code")) {
            return "Verilog code";
        }
        if (name.equals("VERILOG_declaration")) {
            return "Verilog declaration";
        }
        if (name.equals("VERILOG_parameter")) {
            return "Verilog parameter";
        }
        if (name.equals("VERILOG_external_code")) {
            return "Verilog external code";
        }
        return null;
    }

    /**
     * Method to return the "true" name for this Variable.
     * The method removes the "ATTR_" and "ATTRP_" prefixes.
     * @return the "true" name for this Variable.
     */
    public String getTrueName() {
        String name = getKey().getName();
        if (name.startsWith("ATTR_")) {
            return name.substring(5);
        }
        if (name.startsWith("ATTRP_")) {
            int i = name.lastIndexOf('_');
            return name.substring(i);
        }
        return name;
    }

    /**
     * Method to return a description of this Variable.
     * @return a description of this Variable.
     */
    public String describe(VarContext context, Object eobj) {
        return describe(-1, context, eobj);
    }

    /**
     * Return a description of this Variable without any context
     * or helper object info
     */
    public String describe(int aindex) {
        return describe(aindex, VarContext.globalContext, null);
    }

    /**
     * Method to return a String describing this Variable.
     * @param aindex if negative, print the entire array.
     * @param context the VarContext for this Variable.
     * @param eobj the Object on which this Variable resides.
     * @return a String desribing this Variable.
     */
    public String describe(int aindex, VarContext context, Object eobj) {
        TextDescriptor.Unit units = getUnit();
        StringBuffer returnVal = new StringBuffer();
        TextDescriptor.DispPos dispPos = getDispPart();
        if (isCode()) {
            // special case for code: it is a string, the type applies to the result
            if (context == null) {
                context = VarContext.globalContext;
            }
            Object val = null;
            try {
                val = context.evalVarRecurse(this, eobj);
            } catch (VarContext.EvalException e) {
                VarContext.printException(e, this, context, eobj);
                val = e.getMessage();
            }
            if (val == null) {
                val = "?";
            }
            returnVal.append(makeStringVar(val, units));
        } else {
            returnVal.append(getPureValue(aindex));
        }
        if (dispPos == TextDescriptor.DispPos.NAMEVALUE && (aindex < 0 || getLength() == 1)) {
            return this.getTrueName() + "=" + returnVal;
        }
        return returnVal.toString();
    }

    /**
     * Method to convert this Variable to a String without any evaluation of code.
     * @param aindex if negative, print the entire array.
     * @return a String desribing this Variable.
     */
    public String getPureValue(int aindex) {
        TextDescriptor.Unit units = getUnit();
        StringBuffer returnVal = new StringBuffer();
        Object thisAddr = getObject();
        if (thisAddr instanceof Object[]) {
            // compute the array length
            Object[] addrArray = (Object[]) thisAddr;
            int len = addrArray.length;

            // if asking for a single entry, get it
            if (aindex >= 0) {
                // normal array indexing
                if (aindex < len) {
                    returnVal.append(makeStringVar(addrArray[aindex], units));
                }
            } else {
                // in an array, quote strings
                if (len > 1) {
                    returnVal.append("[");
                }
                for (int i = 0; i < len; i++) {
                    if (i != 0) {
                        returnVal.append(",");
                    }
                    returnVal.append(makeStringVar(addrArray[i], units));
                }
                if (len > 1) {
                    returnVal.append("]");
                }
            }
        } else {
            returnVal.append(makeStringVar(thisAddr, units));
        }
        return returnVal.toString();
    }

    /**
     * Method to convert object "addr" to a string, given a set of units.
     * For completion of the method, the units should be treated as in "makeStringVar()".
     */
    private String makeStringVar(Object addr, TextDescriptor.Unit units) {
//		if (addr instanceof Integer)
//		{
//			return ((Integer)addr).toString();
//		}
        if (addr instanceof Float) {
            return TextUtils.makeUnits(((Float) addr).floatValue(), units);
        }
        if (addr instanceof Double) {
            return TextUtils.makeUnits(((Double) addr).doubleValue(), units);
        }
//		if (addr instanceof Short)
//			return ((Short)addr).toString();
//		if (addr instanceof Byte)
//			return ((Byte)addr).toString();
//		if (addr instanceof String)
//			return (String)addr;
        if (addr instanceof Object[]) {
            StringBuffer buf = new StringBuffer();
            buf.append("[");
            Object[] objects = (Object[]) addr;
            for (int i = 0; i < objects.length; i++) {
                if (objects[i] != null) {
                    buf.append(makeStringVar(objects[i], units));
                }
                buf.append(", ");
            }
            buf.replace(buf.length() - 2, buf.length(), "]");
            return buf.toString();
        }
        return addr.toString();
    }

    /**
     * This function is to compare Variable elements. Initiative CrossLibCopy
     * @param obj Object to compare to
     * @param buffer To store comparison messages in case of failure
     * @return True if objects represent same PortInst
     */
    public boolean compare(Object obj, StringBuffer buffer) {
        if (this == obj) {
            return (true);
        }

        // Better if compare classes? but it will crash with obj=null
        if (obj == null || getClass() != obj.getClass()) {
            return (false);
        }

        Variable var = (Variable) obj;
        boolean check = var.getTextDescriptor().equals(getTextDescriptor());

        if (!check && buffer != null) {
            buffer.append("No same variables detected in " + var + " and " + this + "\n");
        }
        return (check);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Variable)) {
            return false;
        }
        Variable that = (Variable) o;
        if (this.key != that.key || !this.descriptor.equals(that.descriptor)) {
            return false;
        }
        if (this.type != that.type) {
            return false;
        }
        if ((this.type & ARRAY) == 0) {
            return this.value.equals(that.value);
        }
        return Arrays.equals((Object[]) this.value, (Object[]) that.value);
    }

    /**
     * Returns a printable version of this Variable.
     * @return a printable version of this Variable.
     */
    @Override
    public String toString() {
        return key.getName();
    }

    /**
     * Method to return the TextDescriptor on this Variable.
     * The TextDescriptor gives information for displaying the Variable.
     * @return the TextDescriptor on this Variable.
     */
    public TextDescriptor getTextDescriptor() {
        return descriptor;
    }

    /**
     * Returns Variable which differs from this Variable by TextDescriptor.
     * The TextDescriptor gives information for displaying the Variable.
     * @param descriptor the new TextDescriptor on this Variable.
     * @return Variable which differs from this Variable by TextDescriptor.
     */
    public Variable withTextDescriptor(TextDescriptor descriptor) {
        if (!isAttribute()) {
            descriptor = descriptor.withParam(false);
        }
        if (this.descriptor == descriptor) {
            return this;
        }
        return new Variable(this.key, this.value, descriptor, this.type);
    }

    /**
     * Returns Variable which differs from this Variable by displayable flag.
     * Displayable Variables are shown with the object.
     * @param state true, if new Variable is displayable.
     * @return Variable which differs from this Variable by displayable flag.
     */
    public Variable withDisplay(boolean state) {
        return withTextDescriptor(getTextDescriptor().withDisplay(state));
    }

    /**
     * Method to return true if this Variable is displayable.
     * @return true if this Variable is displayable.
     */
    public boolean isDisplay() {
        return descriptor.isDisplay();
    }

    /**
     * Returns CodeExpression of this Variable, it it is code variable.
     * Returns null otherwise
     * @return CodeExprssion of this Variable or null
     */
    public CodeExpression getCodeExpression() {
        return value instanceof CodeExpression ? (CodeExpression) value : null;
    }

    /**
     * Determine what code type this variable has, if any
     * @return the code type
     */
    public CodeExpression.Code getCode() {
        CodeExpression ce = getCodeExpression();
        return ce != null ? ce.getCode() : CodeExpression.Code.NONE;
    }

    /**
     * Method to return true if this Variable is Java.
     * Java Variables contain Java code that is evaluated in order to produce a value.
     * @return true if this Variable is Java.
     */
    public boolean isJava() {
        CodeExpression ce = getCodeExpression();
        return ce != null && ce.isJava();
    }

    /**
     * Method to tell whether this Variable is any code.
     * @return true if this Variable is any code.
     */
    public boolean isCode() {
        return value instanceof CodeExpression;
    }

    /**
     * Method to return if this is Variable is a User Attribute.
     * @return true if this Variable is an attribute, false otherwise.
     */
    public boolean isAttribute() {
        return getKey().isAttribute();
    }

    // TextDescriptor
    /**
     * Method to return the color index of the Variable's TextDescriptor.
     * Color indices are more general than colors, because they can handle
     * transparent layers, C-Electric-style opaque layers, and full color values.
     * Methods in "EGraphics" manipulate color indices.
     * @return the color index of the Variables's TextDescriptor.
     */
    public int getColorIndex() {
        return descriptor.getColorIndex();
    }

    /**
     * Returns Variable which differs from this Variable by colorIndex.
     * Color indices are more general than colors, because they can handle
     * transparent layers, C-Electric-style opaque layers, and full color values.
     * Methods in "EGraphics" manipulate color indices.
     * @param colorIndex color index of new Variable.
     * @return Variable which differs from this Variable by colorIndex.
     */
    public Variable withColorIndex(int colorIndex) {
        return withTextDescriptor(getTextDescriptor().withColorIndex(colorIndex));
    }

    /**
     * Method to return the text position of the Variable's TextDescriptor.
     * The text position describes the "anchor point" of the text,
     * which is the point on the text that is attached to the object and does not move.
     * @return the text position of the Variable's TextDescriptor.
     */
    public TextDescriptor.Position getPos() {
        return descriptor.getPos();
    }

    /**
     * Returns Variable which differs from this Variable by position.
     * The text position describes the "anchor point" of the text,
     * which is the point on the text that is attached to the object and does not move.
     * @param p the text position of new Variable.
     * @return Variable which differs from this Variable by position.
     * @throws NullPointerException if p is null.
     */
    public Variable withPos(TextDescriptor.Position p) {
        return withTextDescriptor(getTextDescriptor().withPos(p));
    }

    /**
     * Method to return the text size of the text in the Variable's TextDescriptor.
     * This is a Size object that can describe either absolute text (in points)
     * or relative text (in quarter units).
     * @return the text size of the text in the Variable's TextDescriptor.
     */
    public TextDescriptor.Size getSize() {
        return descriptor.getSize();
    }

    /**
     * Method to find the true size in points for the Variable's TextDescriptor in a given EditWindow0.
     * If the TextDescriptor is already Absolute (in points) nothing needs to be done.
     * Otherwise, the scale of the EditWindow0 is used to determine the acutal point size.
     * @param wnd the EditWindow0 in which drawing will occur.
     * @return the point size of the text described by the Variable's TextDescriptor.
     */
    public double getTrueSize(EditWindow0 wnd) {
        return descriptor.getTrueSize(wnd);
    }

    /**
     * Returns Variable which differs from this Variable by text size.
     * New size is absolute size (in points).
     * The size must be between 1 and 63 points.
     * @param s the point size of new Variable.
     * @return Variable which differs from this Variable by text size.
     */
    public Variable withAbsSize(int s) {
        return withTextDescriptor(getTextDescriptor().withAbsSize(s));
    }

    /**
     * Returns Variable which differs from this Variable by text size.
     * New size is a relative size (in units).
     * The size must be between 0.25 and 127.75 grid units (in .25 increments).
     * @param s the unit size of new Variable.
     * @return Variable which differs from this Variable by text size.
     */
    public Variable withRelSize(double s) {
        return withTextDescriptor(getTextDescriptor().withRelSize(s));
    }

    /**
     * Method to return the text font of the Variable's TextDescriptor.
     * @return the text font of the Variable's TextDescriptor.
     */
    public int getFace() {
        return descriptor.getFace();
    }

    /**
     * Returns Variable which differs from this Variable by text font.
     * @param f the text font of new Variable.
     * @return Variable which differs from this Variable by text font.
     */
    public Variable withFace(int f) {
        return withTextDescriptor(getTextDescriptor().withFace(f));
    }

    /**
     * Method to return the text rotation of the Variable's TextDescriptor.
     * There are only 4 rotations: 0, 90 degrees, 180 degrees, and 270 degrees.
     * @return the text rotation of the Variable's TextDescriptor.
     */
    public TextDescriptor.Rotation getRotation() {
        return descriptor.getRotation();
    }

    /**
     * Returns Variable which differs from this Variable by rotation.
     * There are only 4 rotations: 0, 90 degrees, 180 degrees, and 270 degrees.
     * @param r the text rotation of new Variable.
     * @return Variable which differs from this Variable by rotation.
     */
    public Variable withRotation(TextDescriptor.Rotation r) {
        return withTextDescriptor(getTextDescriptor().withRotation(r));
    }

    /**
     * Method to return the text display part of the Variable's TextDescriptor.
     * @return the text display part of the Variable's TextDescriptor.
     */
    public TextDescriptor.DispPos getDispPart() {
        return descriptor.getDispPart();
    }

    /**
     * Returns Variable which differs from this Variable by dislay part.
     * @param dispPos the text display part of new Variable.
     * @return Variable which differs from this Variable by dislay part.
     * @throws NullPointerException if dispPos is null
     */
    public Variable withDispPart(TextDescriptor.DispPos dispPos) {
        return withTextDescriptor(getTextDescriptor().withDispPart(dispPos));
    }

    /**
     * Method to return true if the text in the Variable's TextDescriptor is italic.
     * @return true if the text in the Variable's TextDescriptor is italic.
     */
    public boolean isItalic() {
        return descriptor.isItalic();
    }

    /**
     * Returns Variable which differs from this Variable by italic flag.
     * @param state true if text of new Variable is italic.
     * @return Variable which differs from this Variable by italic flag.
     */
    public Variable withItalic(boolean state) {
        return withTextDescriptor(getTextDescriptor().withItalic(state));
    }

    /**
     * Method to return true if the text in the Variable's TextDescriptor is bold.
     * @return true if the text in the Variable's TextDescriptor is bold.
     */
    public boolean isBold() {
        return descriptor.isBold();
    }

    /**
     * Returns Variable which differs from this Variable by bold flag.
     * @param state true if text of new Variable is bold.
     * @return Variable which differs from this Variable by bold flag.
     */
    public Variable withBold(boolean state) {
        return withTextDescriptor(getTextDescriptor().withBold(state));
    }

    /**
     * Method to return true if the text in the Variable's TextDescriptor is underlined.
     * @return true if the text in the Variable's TextDescriptor is underlined.
     */
    public boolean isUnderline() {
        return descriptor.isUnderline();
    }

    /**
     * Returns Variable which differs from this Variable by underline flag.
     * @param state true text of new Variable is underlined.
     * @return Variable which differs from this Variable by underline flag.
     */
    public Variable withUnderline(boolean state) {
        return withTextDescriptor(getTextDescriptor().withUnderline(state));
    }

    /**
     * Method to return true if the text in the Variable's TextDescriptor is interior.
     * Interior text is not seen at higher levels of the hierarchy.
     * @return true if the text in the Variable's TextDescriptor is interior.
     */
    public boolean isInterior() {
        return descriptor.isInterior();
    }

    /**
     * Returns Variable which differs from this Variable by interior flag.
     * Interior text is not seen at higher levels of the hierarchy.
     * @param state true if text with new Variable is interior.
     * @return Variable which differs from this Variable by interior flag.
     */
    public Variable withInterior(boolean state) {
        return withTextDescriptor(getTextDescriptor().withInterior(state));
    }

    /**
     * Method to return true if the text in the Variable's TextDescriptor is inheritable.
     * Inheritable variables copy their contents from prototype to instance.
     * Only Variables on NodeProto and PortProto objects can be inheritable.
     * When a NodeInst is created, any inheritable Variables on its NodeProto are automatically
     * created on that NodeInst.
     * @return true if the text in the Variable's TextDescriptor is inheritable.
     */
    public boolean isInherit() {
        return descriptor.isInherit();
    }

    /**
     * Returns Variable which differs from this Variable by inheritable flag.
     * Inheritable variables copy their contents from prototype to instance.
     * Only Variables on NodeProto and PortProto objects can be inheritable.
     * When a NodeInst is created, any inheritable Variables on its NodeProto are automatically
     * created on that NodeInst.
     * @param state true if new Variable is inheritable.
     * @return Variable which differs from this Variable by inheritable flag.
     */
    public Variable withInherit(boolean state) {
        return withTextDescriptor(getTextDescriptor().withInherit(state));
    }

    /**
     * Returns Variable which deffers from this Variable by parameter flag.
     * Parameters are those Variables that have values on instances which are
     * passed down the hierarchy into the contents.
     * Parameters can only exist on Cell objects.
     * @param state true if new Variable is parameter.
     * @return Variable which deffers from this Variable by parameter flag.
     */
    public Variable withParam(boolean state) {
        return withTextDescriptor(getTextDescriptor().withParam(state));
    }

    /**
     * Method to return the X offset of the text in the Variable's TextDescriptor.
     * @return the X offset of the text in the Variable's TextDescriptor.
     */
    public double getXOff() {
        return descriptor.getXOff();
    }

    /**
     * Method to return the Y offset of the text in the Variable's TextDescriptor.
     * @return the Y offset of the text in the Variable's TextDescriptor.
     */
    public double getYOff() {
        return descriptor.getYOff();
    }

    /**
     * Returns Variable which differs from this Variable by
     * X and Y offsets of the text in the Variable's TextDescriptor.
     * The values are scaled by 4, so a value of 3 indicates a shift of 0.75 and a value of 4 shifts by 1.
     * @param xd the X offset of the text in new Variable's TextDescriptor.
     * @param yd the Y offset of the text in new Variable's TextDescriptor.
     * @return Variable which differs from this Variable by
     * X and Y offsets of the text in the Variable's TextDescriptor.
     */
    public Variable withOff(double xd, double yd) {
        return withTextDescriptor(getTextDescriptor().withOff(xd, yd));
    }

    /**
     * Method to return the Unit of the Variable's TextDescriptor.
     * Unit describes the type of real-world unit to apply to the value.
     * For example, if this value is in volts, the Unit tells whether the value
     * is volts, millivolts, microvolts, etc.
     * @return the Unit of the Variable's TextDescriptor.
     */
    public TextDescriptor.Unit getUnit() {
        return descriptor.getUnit();
    }

    /**
     * Returns Variable which differs from this Variable by unit.
     * Unit describe the type of real-world unit to apply to the value.
     * For example, if this value is in volts, the Unit tells whether the value
     * is volts, millivolts, microvolts, etc.
     * @param u the Unit of new Variable.
     * @return Variable which differs from this Variable by unit.
     */
    public Variable withUnit(TextDescriptor.Unit u) {
        return withTextDescriptor(getTextDescriptor().withUnit(u));
    }
}
