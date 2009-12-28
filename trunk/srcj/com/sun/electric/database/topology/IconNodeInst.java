/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IconNodeInst.java
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
package com.sun.electric.database.topology;

import com.sun.electric.database.ImmutableIconInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.CodeExpression;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class defines NodeInsts that are icons.
 */
public class IconNodeInst extends NodeInst {

    private IconNodable[] nodables;

    /**
     * The constructor of IconNodeInst. Use the factory "newInstance" instead.
     * @param d persistent data of this IconNodeInst.
     * @param topology the Topology in which this IconNodeInst will reside.
     */
    IconNodeInst(ImmutableNodeInst d, Topology topology) {
        super(d, topology);
    }

    /**
     * Returns persistent data of this IconNodeInst.
     * @return persistent data of this IconNodeInst.
     */
    @Override
    public ImmutableIconInst getD() {
        return (ImmutableIconInst) super.getD();
    }

    /**
     * Modifies persistend data of this NodeInst.
     * @param newD new persistent data.
     * @param notify true to notify Undo system.
     * @return true if persistent data was modified.
     */
    @Override
    public boolean setD(ImmutableNodeInst newD, boolean notify) {
        ImmutableIconInst oldD = getD();
        if (newD.name != oldD.name) {
            nodables = null;
        }
        return super.setD(newD, notify);
    }

    @Override
    public void setDInUndo(ImmutableNodeInst newD) {
        ImmutableIconInst oldD = getD();
        if (newD.name != oldD.name) {
            nodables = null;
        }
        super.setDInUndo(newD);
    }

    /**
     * Method to return the prototype of this IconNodeInst.
     * @return the prototype of this IconNodeInst.
     */
    @Override
    public Cell getProto() {
        return (Cell) super.getProto();
    }

    /**
     * Method to add a Variable on this IconNodeInst.
     * It may add a repaired copy of this Variable in some cases.
     * @param var Variable to add.
     */
    @Override
    public void addVar(Variable var) {
        if (isParam(var.getKey())) {
            throw new IllegalArgumentException(this + " already has a variable with name " + var);
        }
        super.addVar(var.withParam(false).withInherit(false));
    }

    /**
     * Method to return the Variable on this ElectricObject with a given key.
     * @param key the key of the Variable.
     * @return the Variable with that key and type, or null if there is no such Variable
     * or default Variable value.
     * @throws NullPointerException if key is null
     */
    @Override
    public Variable getVar(Variable.Key key) {
        checkExamine();
        if (key.isAttribute()) {
            // ToDo: delete
            Variable param = getParameter((Variable.AttrKey) key);
            if (param != null) {
                return param;
            }
        }
        return getD().getVar(key);
    }

    /**
     * Method to return the Parameter or Variable on this ElectricObject with a given key.
     * @param key the key of the Parameter or Variable.
     * @return the Parameter or Variable with that key, or null if there is no such Parameter or Variable Variable.
     * @throws NullPointerException if key is null
     */
    @Override
    public Variable getParameterOrVariable(Variable.Key key) {
        checkExamine();
        if (key.isAttribute()) {
            Variable param = getParameter((Variable.AttrKey) key);
            if (param != null) {
                return param;
            }
        }
        return getD().getVar(key);
    }

    /**
     * Method to return an Iterator over all Parameters and Variables on this ElectricObject.
     * @return an Iterator over all Parameters and Variables on this ElectricObject.
     */
    @Override
    public Iterator<Variable> getParametersAndVariables() {
        if (getD().getNumDefinedParameters() == 0) {
            return getVariables();
        }

        ArrayList<Variable> vars = new ArrayList<Variable>();
        for (Iterator<Variable> it = getDefinedParameters(); it.hasNext();) {
            vars.add(it.next());
        }
        for (Iterator<Variable> it = getVariables(); it.hasNext();) {
            vars.add(it.next());
        }
        return vars.iterator();
    }

    /**
     * Method to return the Parameter on this IconNodeInst with the given key.
     * If the Parameter is not found on this IconNodeInst, it
     * is also searched for on the default var owner.
     * @param key the key of the parameter
     * @return the Parameter with that key, that may exist either on this IconNodeInst
     * or the default owner.  Returns null if none found.
     */
    @Override
    public Variable getParameter(Variable.Key key) {
        if (!(key instanceof Variable.AttrKey)) {
            return null;
        }
        Variable instParam = getD().getDefinedParameter((Variable.AttrKey) key);
        if (instParam != null) {
            return instParam;
        }
        Cell icon = (Cell) getProto();
        Variable iconParam = icon.getParameter(key);
        return iconParam != null ? composeInstParam(iconParam, null) : null;
    }

    /**
     * Method to tell if the Variable.Key is a defined parameters of this IconNodeInst.
     * Parameters which are not defined on IconNodeInst take default values from Icon Cell.
     * @param key the key of the parameter
     * @return true if the key is a definded parameter of this IconNodeInst
     */
    @Override
    public boolean isDefinedParameter(Variable.Key key) {
        if (!(key instanceof Variable.AttrKey)) {
            return false;
        }
        return getD().getDefinedParameter((Variable.AttrKey) key) != null;
    }

    /**
     * Method to return an Iterator over all Parameters on this IconNodeInst.
     * This may also include any parameters on the defaultVarOwner object that are not on this object.
     * @return an Iterator over all Parameters on this IconNodeInst.
     */
    @Override
    public Iterator<Variable> getParameters() {
        Cell icon = (Cell) getProto();
        if (!icon.hasParameters()) {
            return ArrayIterator.emptyIterator();
        }

        ArrayList<Variable> params = new ArrayList<Variable>();
        // get all parameters on this object
        for (Iterator<Variable> it = icon.getParameters(); it.hasNext();) {
            Variable iconParam = it.next();
            Variable instVar = getD().getDefinedParameter((Variable.AttrKey) iconParam.getKey());
            params.add(composeInstParam(iconParam, instVar));
        }
        return params.iterator();
    }

    /**
     * Method to return an Iterator over defined Parameters on this IconNodeInst.
     * This doesn't include any parameters on the defaultVarOwner object that are not on this object.
     * @return an Iterator over defined Parameters on this IconNodeInst.
     */
    @Override
    public Iterator<Variable> getDefinedParameters() {
        return getD().getDefinedParameters();
    }

    /**
     * Method to add a Parameter to this NodeInst.
     * Overridden in IconNodeInst
     * @param param the Variable to delete.
     */
    public void addParameter(Variable param) {
        if (!isParam(param.getKey())) {
            throw new IllegalArgumentException("Parameter " + param + " is not defined on " + getProto());
        }
        Cell icon = (Cell) getProto();
        Variable iconParam = icon.getParameter(param.getKey());
        param = composeInstParam(iconParam, param);
        if (setD(getD().withParam(param), true)) // check for side-effects of the change
        {
            checkPossibleVariableEffects(param.getKey());
        }
    }

    /**
     * Method to delete a defined Parameter from this IconNodeInst.
     * The Parameter becomes a default parameter with value inherited from the default owner
     * @param key the key of the Variable to delete.
     */
    @Override
    public void delParameter(Variable.Key key) {
        if (key instanceof Variable.AttrKey && setD(getD().withoutParam((Variable.AttrKey) key), true)) // check for side-effects of the change
        {
            checkPossibleVariableEffects(key);
        }
    }

    /**
     * Method to return true if the Variable on this NodeInst with given key is a parameter.
     * Parameters are those Variables that have values on instances which are
     * passed down the hierarchy into the contents.
     * @param varKey key to test
     * @return true if the Variable with given key is a parameter.
     */
    @Override
    public boolean isParam(Variable.Key varKey) {
        Cell icon = (Cell) getProto();
        return icon.isParam(varKey);
    }

    /**
     * Updates the TextDescriptor on this NodeInst selected by varKey.
     * The varKey may be a key of variable on this NodeInst or one of the
     * special keys:
     * NodeInst.NODE_NAME
     * NodeInst.NODE_PROTO
     * If varKey doesn't select any text descriptor, no action is performed.
     * The TextDescriptor gives information for displaying the Variable.
     * @param varKey key of variable or special key.
     * @param td new value TextDescriptor
     */
    @Override
    public void setTextDescriptor(Variable.Key varKey, TextDescriptor td) {
        Variable param = getParameter(varKey);
        if (param != null) {
            td = td.withParam(true).withInherit(false).withUnit(param.getUnit());
            addParameter(param.withTextDescriptor(td));
            return;
        }
        super.setTextDescriptor(varKey, td);
    }

    /**
     * Method to create a Variable on this ElectricObject with the specified values.
     * @param key the key of the Variable.
     * @param value the object to store in the Variable.
     * @param td text descriptor of the Variable
     * @return the Variable that has been created.
     */
    @Override
    public Variable newVar(Variable.Key key, Object value, TextDescriptor td) {
        if (isParam(key)) {
            addParameter(getParameter(key).withObject(value).withTextDescriptor(td));
            return getParameter(key);
        }
        return super.newVar(key, value, td);
    }

    /**
     * Method to update a Variable on this ElectricObject with the specified values.
     * If the Variable already exists, only the value is changed; the displayable attributes are preserved.
     * @param key the key of the Variable.
     * @param value the object to store in the Variable.
     * @return the Variable that has been updated.
     */
    @Override
    public Variable updateVar(Variable.Key key, Object value) {
        if (isParam(key)) {
            Variable param = getParameter(key);
            addParameter(getParameter(key).withObject(value));
            return getParameter(key);
        }
        return super.updateVar(key, value);
    }

    /**
     * Method to update a Parameter on this ElectricObject with the specified values.
     * If the Variable already exists, only the value is changed; the displayable attributes are preserved.
     * @param key the key of the Variable.
     * @param value the object to store in the Variable.
     * @return the Variable that has been updated.
     */
    @Override
    public Variable updateParam(Variable.Key key, Object value) {
        if (isParam(key)) {
            Variable param = getParameter(key);
            addParameter(getParameter(key).withObject(value));
            return getParameter(key);
        }
        return super.updateVar(key, value);
    }

    /**
     * Method to update a text Variable on this ElectricObject with the specified values.
     * If the Variable already exists, only the value is changed;
     * the displayable attributes and Code are preserved.
     * @param key the key of the Variable.
     * @param text the text to store in the Variable.
     * @return the Variable that has been updated.
     */
    @Override
    public Variable updateVarText(Variable.Key key, String text) {
        if (isParam(key)) {
            Variable param = getParameter(key);
            addParameter(getParameter(key).withText(text));
            return getParameter(key);
        }
        return super.updateVarText(key, text);
    }

    /**
     * Method to update a Variable on this ElectricObject with the specified code.
     * If the Variable already exists, only the code is changed;
     * the displayable attributes and value are preserved.
     * @param key the key of the Variable.
     * @param code the new code of the Variable.
     * @return the Variable that has been updated.
     */
    @Override
    public Variable updateVarCode(Variable.Key key, CodeExpression.Code code) {
        if (isParam(key)) {
            Variable param = getParameter(key);
            addParameter(getParameter(key).withCode(code));
            return getParameter(key);
        }
        return super.updateVarCode(key, code);
    }

    /**
     * Method to copy all variables from another ElectricObject to this ElectricObject.
     * @param other the other ElectricObject from which to copy Variables.
     */
    @Override
    public void copyVarsFrom(ElectricObject other) {
        checkChanging();
        for (Iterator<Variable> it = other.getParametersAndVariables(); it.hasNext();) {
            Variable var = it.next();
            if (isParam(var.getKey())) {
                addParameter(var.withParam(true));
            } else {
                addVar(var.withParam(false));
            }
        }
    }

//    public void addParam(Variable var) {
//        assert var.getTextDescriptor().isParam() && var.isInherit();
//        if (isIcon()) {
//            // Remove variables with the same name as new parameter
//            for (Iterator<NodeInst> it = getInstancesOf(); it.hasNext(); ) {
//                NodeInst ni = it.next();
//                ni.delVar(var.getKey());
//            }
//        }
//        setD(getD().withoutVariable(var.getKey()).withParam(var));
//    }
//
    private static Variable composeInstParam(Variable iconParam, Variable instVar) {
        boolean display = !iconParam.isInterior();
        if (instVar != null) {
            return instVar.withParam(true).withInherit(false).withInterior(false).withDisplay(display).withUnit(iconParam.getUnit());
        }
        return iconParam.withInherit(false).withInterior(false).withDisplay(display);
    }

    /**
     * Get Nodable by array index.
     * @param arrayIndex the Nodable index.
     * @return the desired Nodable.
     */
    @Override
    public Nodable getNodable(int arrayIndex) {
        if (nodables == null) {
            nodables = new IconNodable[getNameKey().busWidth()];
            for (int i = 0; i < nodables.length; i++) {
                nodables[i] = new IconNodable(i);
            }
        }
        return nodables[arrayIndex];
    }

    private class IconNodable implements Nodable {

        int arrayIndex;

        IconNodable(int arrayIndex) {
            this.arrayIndex = arrayIndex;
        }

        /**
         * Method to return the prototype of this Nodable.
         * @return the prototype of this Nodable.
         */
        public NodeProto getProto() {
            Cell iconCell = IconNodeInst.this.getProto();
            Cell mainSchematics = iconCell.getCellGroup().getMainSchematics();
            return mainSchematics != null ? mainSchematics : iconCell;
        }

        /**
         * Method to tell whether this is a cell instance.
         * @return true becaue NetSchem objects are always cell instances.
         */
        public boolean isCellInstance() {
            return true;
        }

        /**
         * Method to return the Cell that contains this Nodable.
         * @return the Cell that contains this Nodable.
         */
        public Cell getParent() {
            return IconNodeInst.this.getParent();
        }

        /**
         * Method to return the name of this Nodable.
         * @return the name of this Nodable.
         */
        public String getName() {
            return getNameKey().toString();
        }

        /**
         * Method to return the name key of this Nodable.
         * @return the name key of this Nodable.
         */
        public Name getNameKey() {
            return IconNodeInst.this.getNameKey().subname(arrayIndex);
        }

        /**
         * Method to return the Variable on this ElectricObject with a given key.
         * @param key the key of the Variable.
         * @return the Variable with that key, or null if there is no such Variable.
         */
        public Variable getVar(Variable.Key key) {
            return IconNodeInst.this.getVar(key);
        }

        /**
         * Method to return the Parameter on this Nodable with the given key.
         * If the parameter is not found on this Nodable, it
         * is also searched for on the default var owner.
         * @param key the key of the parameter
         * @return the Parameter with that key, that may exist either on this Nodable
         * or the default owner.  Returns null if none found.
         */
        public Variable getParameter(Variable.Key key) {
            return IconNodeInst.this.getParameter(key);
        }

        /**
         * Method to return the Parameter or Variable on this Nodable with a given key.
         * @param key the key of the Parameter or Variable.
         * @return the Parameter or Variable with that key, or null if there is no such Parameter or Variable Variable.
         * @throws NullPointerException if key is null
         */
        public Variable getParameterOrVariable(Variable.Key key) {
            return IconNodeInst.this.getParameterOrVariable(key);
        }

        /**
         * Method to tell if the Variable.Key is a defined parameters of this Nodable.
         * Parameters which are not defined on Nodable take default values from Icon Cell.
         * @param key the key of the parameter
         * @return true if the key is a definded parameter of this Nodable
         */
        public boolean isDefinedParameter(Variable.Key key) {
            return IconNodeInst.this.isDefinedParameter(key);
        }

        /**
         * Method to return an Iterator over all Parameters on this Nodable.
         * This may also include any Parameters on the defaultVarOwner object that are not on this Nodable.
         * @return an Iterator over all Parameters on this Nodable.
         */
        public Iterator<Variable> getParameters() {
            return IconNodeInst.this.getParameters();
        }

        /**
         * Method to return an Iterator over defined Parameters on this Nodable.
         * This doesn't include any Parameters on the defaultVarOwner object that are not on this Nodable.
         * @return an Iterator over defined Parameters on this Nodable.
         */
        public Iterator<Variable> getDefinedParameters() {
            return IconNodeInst.this.getDefinedParameters();
        }

        /**
         * Returns a printable version of this Nodable.
         * @return a printable version of this Nodable.
         */
        @Override
        public String toString() {
            return "IconNodable " + getName();
        }

        // JKG: trying this out
        public boolean contains(NodeInst ni, int arrayIndex) {
            if (IconNodeInst.this == ni && this.arrayIndex == arrayIndex) {
                return true;
            }
            return false;
        }

        public NodeInst getNodeInst() {
            return IconNodeInst.this;
        }

        /**
         * Get array index of this Nodable
         * @return the array index of this Nodable
         */
        public int getNodableArrayIndex() {
            return arrayIndex;
        }
    }
}
