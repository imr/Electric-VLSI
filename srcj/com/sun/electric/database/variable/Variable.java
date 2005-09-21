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
package com.sun.electric.database.variable;

import com.sun.electric.database.ImmutableVariable;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.technologies.Generic;
import java.util.HashMap;


/**
 * The Variable class defines a single attribute-value pair that can be attached to any ElectricObject.
 * <P>
 * This class should be thread-safe.
 */
public class Variable
{
	/**
	 * The Key class caches Variable names.
	 */
	public static class Key implements Comparable
	{
		private final String name;
		
		/**
		 * Method to create a new Key object with the specified name.
		 * @param name the name of the Variable.
         * @throws NullPointerException if name is null.
		 */
		Key(String name)
		{
            if (name == null) throw new NullPointerException();
			this.name = name;
		}

		/**
		 * Method to return the name of this Key object.
		 * @return the name of this Key object.
		 */
		public String getName() { return name; }

        /**
         * Method to determine if two Keys are equal.
         * Compares by name (case sensitive).
         * @param k the Key to compare to
         * @return true if equal, false otherwise.
         */
        public boolean equals(Key k) { return name.equals(k.getName()); }

		/**
		 * Compares Variable Keys by their names.
		 * @param obj the other Variable Key.
		 * @return a comparison between the Variable Keys.
		 */
		public int compareTo(Object obj)
		{
			Key that = (Key)obj;
			return TextUtils.STRING_NUMBER_ORDER.compare(name, that.name);
		}
	}

	/** a list of all variable keys */						private static final HashMap varKeys = new HashMap();
	/** all variable keys addressed by lower case name */	private static final HashMap varCanonicKeys = new HashMap();

	/**
	 * Method to return the Key object for a given Variable name.
	 * Variable Key objects are caches of the actual string name of the Variable.
	 * @return the Key object for a given Variable name.
	 */
	static synchronized Variable.Key findKey(String name)
	{
		Variable.Key key = (Variable.Key)varKeys.get(name);
		if (key == null)
		{
			String lowCaseName = TextUtils.canonicString(name);
			if (!lowCaseName.equals(name))
				key = (Variable.Key)varCanonicKeys.get(lowCaseName);
            if (key != null)
            {
                System.out.println("WARNING: Variable search may become case-sensitive in future versions. Search: " + name + " found: " + key.getName());
                varKeys.put(name, key);
            }
		}
		return key;
	}

	/**
	 * Method to find or create the Key object for a given Variable name.
	 * Variable Key objects are caches of the actual string name of the Variable.
	 * @param name given Variable name.
	 * @return the Key object for a given Variable name.
	 */
	public static synchronized Variable.Key newKey(String name)
	{
		Variable.Key key = findKey(name);
		if (key != null) return key;
		key = new Variable.Key(name);
		varKeys.put(name, key);
		varCanonicKeys.put(TextUtils.canonicString(name), key);
		return key;
	}

    private final ElectricObject owner;
    private ImmutableVariable d;

    /** true if var is attached to valid electric object */ private boolean linked;

	/**
	 * The constructor builds a Variable from the given parameters.
	 * @param owner the ElectriObject that owns this variable.
	 * @param addr the object that will be stored in the Variable.
	 * @param descriptor a TextDescriptor to control how the Variable will be displayed.
	 * @param key a Key object that identifies this Variable.
	 */
	Variable(ElectricObject owner, Object addr, TextDescriptor descriptor, Key key)
	{
        this.owner = owner;
        ImmutableTextDescriptor td = ImmutableTextDescriptor.newImmutableTextDescriptor(descriptor);
        if (!(owner instanceof Cell))
            td = td.withoutParam();
        this.d = ImmutableVariable.newInstance(key, td, addr);
	}

	/**
	 * This function is to compare Variable elements. Initiative CrossLibCopy
 	 * @param obj Object to compare to
	 * @param buffer To store comparison messages in case of failure
	 * @return True if objects represent same PortInst
	 */
    public boolean compare(Object obj, StringBuffer buffer)
	{
		if (this == obj) return (true);

        // Better if compare classes? but it will crash with obj=null
        if (obj == null || getClass() != obj.getClass())
            return (false);

        Variable var = (Variable)obj;
       	boolean check = var.getTextDescriptor().equals(getTextDescriptor());

		if (!check && buffer != null)
			buffer.append("No same variables detected in " + var + " and " + this + "\n");
        return (check);
    }

    /**
     * Get the number of entries stored in this Variable.
	 * For non-arrayed Variables, this is 1.
     * @return the number of entries stored in this Variable.
     */
    public synchronized int getLength()
	{
        int len = d.getValueLength();
        return len >= 0 ? len : 1;
	}
    
    /**
     * Get the actual object stored in this Variable.
     * @return the object stored in this Variable.
     */
    public Object getObject() { return d.getValueInCurrentThread(); }

    /** 
     * Treat the stored Object as an array of Objects and
     * get the object at index @param index.
     * @param index index into the array of objects.
     * @return the objects stored in this Variable at the index.
     */
    public synchronized Object getObject(int index)
    {
        int len = d.getValueLength();
        return len >= 0 ? d.getValueInCurrentThread(index) : null;
    }
        
	/**
	 * Method to return the Variable Key associated with this Variable.
	 * @return the Variable Key associated with this variable.
	 */
	public Key getKey() { return d.key; }

    /**
     * Set if this variable is linked to an ElectricObject
     */
    void setLinked(boolean linked) { this.linked = linked; }

    /**
     * Returns true if variable is linked to a linked database object, false otherwise.
     * @return true if variable is linked to a linked database object, false otherwise.
     */
    public boolean isLinked() {
        return (linked && owner.isLinked());
    }

    /**
     * Get the Electric object that stores this Variable
     * @return the Owner of this Variable
     */
    public ElectricObject getOwner() { return owner; }

    /**
     * Returns true if this Variable is completely linked into database.
	 * This means that its owner ElectricObject is completely linked into database
	 * and this Variable is in vars list of the owner.
     */
	public boolean isActuallyLinked()
	{
		ElectricObject owner = getOwner();
		return owner != null && owner.isLinked() && owner.getVar(d.key) == this;
	}

	/**
	 * Method to return a more readable name for this Variable.
	 * The method adds "Parameter" or "Attribute" as appropriate
	 * and uses sensible names such as "Diode Size" instead of "SCHEM_diode".
	 * @return a more readable name for this Variable.
	 */
	public String getReadableName()
	{
		String trueName = "";
		String name = d.key.getName();
		if (name.startsWith("ATTR_"))
		{
			if (isParam())
				trueName +=  "Parameter '" + name.substring(5) + "'"; else
					trueName +=  "Attribute '" + name.substring(5) + "'";
		} else
		{
			String betterName = betterVariableName(name);
			if (betterName != null) trueName += betterName; else
				trueName +=  "Variable '" + name + "'";
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
	public String getFullDescription(ElectricObject eobj)
	{
		String trueName = getReadableName();
		String description = null;
		if (eobj instanceof Export)
		{
			description = trueName + " on " + eobj;
		} else if (eobj instanceof PortInst)
		{
			PortInst pi = (PortInst)eobj;
			description = trueName + " on " + pi.getPortProto() +
				" of " + pi.getNodeInst().describe(true);
		} else if (eobj instanceof ArcInst)
		{
			description = trueName + " on " + eobj;
		} else if (eobj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)eobj;
			description = trueName + " on " + ni;
			if (ni.getProto() == Generic.tech.invisiblePinNode)
			{
				String varName = getKey().getName();
				String betterName = betterVariableName(varName);
				if (betterName != null) description = betterName;
			}
		} else if (eobj instanceof Cell)
		{
			description = trueName + " of " + eobj;
		}
		return description;
	}

	/**
	 * Method to convert the standard Variable names to more readable strings.
	 * @param name the actual Variable name.
	 * @return a better name for it (returns the same name if no better one exists).
	 */
	public static String betterVariableName(String name)
	{
		// handle standard variable names
		if (name.equals("ARC_name")) return "Arc Name";
		if (name.equals("ARC_radius")) return "Arc Radius";
		if (name.equals("ART_color")) return "Color";
		if (name.equals("ART_degrees")) return "Number of Degrees";
		if (name.equals("ART_message")) return "Annotation text";
		if (name.equals("NET_ncc_match")) return "NCC equivalence";
		if (name.equals("NET_ncc_forcedassociation")) return "NCC association";
		if (name.equals("NODE_name")) return "Node Name";
		if (name.equals("SCHEM_capacitance")) return "Capacitance";
		if (name.equals("SCHEM_diode")) return "Diode Size";
		if (name.equals("SCHEM_global_name")) return "Global Signal Name";
		if (name.equals("SCHEM_inductance")) return "Inductance";
		if (name.equals("SCHEM_resistance")) return "Resistance";
		if (name.equals("SIM_fall_delay")) return "Fall Delay";
		if (name.equals("SIM_fasthenry_group_name")) return "FastHenry Group";
		if (name.equals("SIM_rise_delay")) return "Rise Delay";
		if (name.equals("SIM_spice_card")) return "SPICE code";
		if (name.equals("SIM_spice_declaration")) return "SPICE declaration";
		if (name.equals("SIM_spice_model")) return "SPICE model";
		if (name.equals("SIM_verilog_wire_type")) return "Verilog Wire type";
		if (name.equals("SIM_weak_node")) return "Transistor Strength";
		if (name.equals("transistor_width")) return "Transistor Width";
		if (name.equals("VERILOG_code")) return "Verilog code";
		if (name.equals("VERILOG_declaration")) return "Verilog declaration";
		return null;
	}

	/**
	 * Method to return the "true" name for this Variable.
	 * The method removes the "ATTR_" and "ATTRP_" prefixes.
	 * @return the "true" name for this Variable.
	 */
	public String getTrueName()
	{
		String name = d.key.getName();
		if (name.startsWith("ATTR_"))
			return name.substring(5);
		if (name.startsWith("ATTRP_"))
		{
			int i = name.lastIndexOf('_');
			return name.substring(i);
		}
		return name;
	}

	/**
	 * Method to return a description of this Variable.
	 * @return a description of this Variable.
	 */
	public String describe(VarContext context, Object eobj)
	{
		return describe(-1, context, eobj);
	}

    /** 
     * Return a description of this Variable without any context
     * or helper object info
     */
    public String describe(int aindex)
    {
        return describe(aindex, VarContext.globalContext, null);
    }

	/**
	 * Method to return a String describing this Variable.
	 * @param aindex if negative, print the entire array.
	 * @param context the VarContext for this Variable.
	 * @param eobj the Object on which this Variable resides.
	 * @return a String desribing this Variable.
	 */
	public String describe(int aindex, VarContext context, Object eobj)
	{
		TextDescriptor.Unit units = d.descriptor.getUnit();
		StringBuffer returnVal = new StringBuffer();
		TextDescriptor.DispPos dispPos = d.descriptor.getDispPart();
        if (isCode())
		{
			// special case for code: it is a string, the type applies to the result
            if (context == null) context = VarContext.globalContext;
            Object val = null;
            try {
                val = context.evalVarRecurse(this, eobj);
            } catch (VarContext.EvalException e) {
                val = e.getMessage();
            }
            if (val == null) val = "?";
            returnVal.append(makeStringVar(val, units));
        } else
		{
			returnVal.append(getPureValue(aindex));
		}
        if (dispPos == TextDescriptor.DispPos.NAMEVALUE && (aindex < 0 || getLength() == 1))
		{
			return this.getTrueName() + "=" + returnVal.toString();
		}
		return returnVal.toString();
	}

	/**
	 * Method to convert this Variable to a String without any evaluation of code.
	 * @param aindex if negative, print the entire array.
	 * @return a String desribing this Variable.
	 */
	public String getPureValue(int aindex)
	{
		TextDescriptor.Unit units = d.descriptor.getUnit();
		StringBuffer returnVal = new StringBuffer();
        Object thisAddr = getObject();
		if (thisAddr instanceof Object[])
		{
			// compute the array length
			Object [] addrArray = (Object [])thisAddr;
			int len = addrArray.length;

			// if asking for a single entry, get it
			if (aindex >= 0)
			{
				// normal array indexing
				if (aindex < len)
					returnVal.append(makeStringVar(addrArray[aindex], units));
			} else
			{
				// in an array, quote strings
				if (len > 1) returnVal.append("[");
				for(int i=0; i<len; i++)
				{
					if (i != 0) returnVal.append(",");
					returnVal.append(makeStringVar(addrArray[i], units));
				}
				if (len > 1) returnVal.append("]");
			}
		} else
		{
			returnVal.append(makeStringVar(thisAddr, units));
		}
		return returnVal.toString();
	}

	/**
	 * Method to convert object "addr" to a string, given a set of units.
	 * For completion of the method, the units should be treated as in "makeStringVar()".
	 */
	private String makeStringVar(Object addr, TextDescriptor.Unit units)
	{
		if (addr instanceof Integer)
		{
			return ((Integer)addr).toString();
		}
		if (addr instanceof Float)
		{
			return TextUtils.makeUnits(((Float)addr).floatValue(), units);
		}
		if (addr instanceof Double)
		{
			return TextUtils.makeUnits(((Double)addr).doubleValue(), units);
		}
		if (addr instanceof Short)
			return ((Short)addr).toString();
		if (addr instanceof Byte)
			return ((Byte)addr).toString();
		if (addr instanceof String)
			return (String)addr;
		if (addr instanceof NodeInst)
			return ((NodeInst)addr).describe(false);
		if (addr instanceof ArcInst)
			return ((ArcInst)addr).describe(false);
        if (addr instanceof Object[]) {
            StringBuffer buf = new StringBuffer();
            buf.append("[");
            Object [] objects = (Object[])addr;
            for (int i=0; i<objects.length; i++) {
                buf.append(makeStringVar(objects[i], units));
                buf.append(", ");
            }
            buf.replace(buf.length()-2, buf.length(), "]");
            return buf.toString();
        }
		return "?";
	}

	/**
	 * Method to return the TextDescriptor on this Variable.
	 * The TextDescriptor gives information for displaying the Variable.
	 * @return the TextDescriptor on this Variable.
	 */
	public ImmutableTextDescriptor getTextDescriptor() { return d.descriptor; }

	/**
	 * Method to set the TextDescriptor on this Variable.
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param descriptor the new TextDescriptor on this Variable.
	 */
	public void setTextDescriptor(TextDescriptor descriptor)
    {
        owner.checkChanging();

        ImmutableTextDescriptor oldDescriptor = lowLevelSetTextDescriptor(ImmutableTextDescriptor.newImmutableTextDescriptor(descriptor));
       
		// handle change control, constraint, and broadcast
        if (owner.isDatabaseObject())
            Undo.modifyTextDescript(owner, d.key.getName(), oldDescriptor);
     }

	/**
	 * Method to set the TextDescriptor on this Variable.
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param descriptor the new TextDescriptor on this Variable.
     * @return old text descriptor
	 */
	ImmutableTextDescriptor lowLevelSetTextDescriptor(ImmutableTextDescriptor descriptor)
    {
        ImmutableTextDescriptor oldDescriptor = this.d.descriptor;
        if (!(owner instanceof Cell))
            descriptor = descriptor.withoutParam();
        this.d = d.withDescriptor(descriptor);
        return oldDescriptor;
    }

	/**
	 * Method to set this Variable to be displayable.
	 * Displayable Variables are shown with the object.
	 */
	public synchronized void setDisplay(boolean state)
    {
		MutableTextDescriptor td = new MutableTextDescriptor(d.descriptor);
		td.setDisplay(state);
		setTextDescriptor(td);
    }

	/**
	 * Method to return true if this Variable is displayable.
	 * @return true if this Variable is displayable.
	 */
	public boolean isDisplay() { return d.descriptor.isDisplay(); }

    /**
     * Determine what code type this variable has, if any
     * @return the code type
     */
    public TextDescriptor.Code getCode() { return d.descriptor.getCode(); }

    /**
     * Sets the code type of this Variable
     * @param code the code to set to
     */
    public void setCode(TextDescriptor.Code code) {
		MutableTextDescriptor td = new MutableTextDescriptor(d.descriptor);
		td.setCode(code);
		setTextDescriptor(td);
    }

	/**
	 * Method to return true if this Variable is Java.
	 * Java Variables contain Java code that is evaluated in order to produce a value.
	 * @return true if this Variable is Java.
	 */
	public boolean isJava() { return d.descriptor.isJava(); }

	/**
	 * Method to tell whether this Variable is any code.
	 * @return true if this Variable is any code.
	 */
	public boolean isCode() { return d.descriptor.isCode(); }

    /**
     * Method to return if this is Variable is a User Attribute.
     * @return true if this Variable is an attribute, false otherwise.
     */
    public boolean isAttribute() { return getKey().getName().startsWith("ATTR_"); }

	/**
	 * Returns a printable version of this Variable.
	 * @return a printable version of this Variable.
	 */
	public String toString()
	{
		return d.key.getName();
	}

	// TextDescriptor

	/**
	 * Method to return the color index of the Variable's TextDescriptor.
	 * Color indices are more general than colors, because they can handle
	 * transparent layers, C-Electric-style opaque layers, and full color values.
	 * Methods in "EGraphics" manipulate color indices.
	 * @return the color index of the Variables's TextDescriptor.
	 */
	public int getColorIndex() { return d.descriptor.getColorIndex(); }

	/**
	 * Method to set the color index of the Variable's TextDescriptor.
	 * Color indices are more general than colors, because they can handle
	 * transparent layers, C-Electric-style opaque layers, and full color values.
	 * Methods in "EGraphics" manipulate color indices.
	 * @param colorIndex the color index of the Variable's TextDescriptor.
	 */
	public void setColorIndex(int colorIndex)
	{
		MutableTextDescriptor td = new MutableTextDescriptor(d.descriptor);
		td.setColorIndex(colorIndex);
		setTextDescriptor(td);
	}

	/**
	 * Method to return the text position of the Variable's TextDescriptor.
	 * The text position describes the "anchor point" of the text,
	 * which is the point on the text that is attached to the object and does not move.
	 * @return the text position of the Variable's TextDescriptor.
	 */
	public TextDescriptor.Position getPos() { return d.descriptor.getPos(); }

	/**
	 * Method to set the text position of the Variable's TextDescriptor.
	 * The text position describes the "anchor point" of the text,
	 * which is the point on the text that is attached to the object and does not move.
	 * @param p the text position of the Variable's TextDescriptor.
	 */
	public synchronized void setPos(TextDescriptor.Position p)
	{
		MutableTextDescriptor td = new MutableTextDescriptor(d.descriptor);
		td.setPos(p);
		setTextDescriptor(td);
	}

	/**
	 * Method to return the text size of the text in the Variable's TextDescriptor.
	 * This is a Size object that can describe either absolute text (in points)
	 * or relative text (in quarter units).
	 * @return the text size of the text in the Variable's TextDescriptor.
	 */
	public synchronized TextDescriptor.Size getSize() { return d.descriptor.getSize(); }

	/**
	 * Method to find the true size in points for the Variable's TextDescriptor in a given EditWindow.
	 * If the TextDescriptor is already Absolute (in points) nothing needs to be done.
	 * Otherwise, the scale of the EditWindow is used to determine the acutal point size.
	 * @param wnd the EditWindow in which drawing will occur.
	 * @return the point size of the text described by the Variable's TextDescriptor.
	 */
	public double getTrueSize(EditWindow_ wnd) { return d.descriptor.getTrueSize(wnd); }

	/**
	 * Method to set the text size of Variable's TextDescriptor to an absolute size (in points).
	 * The size must be between 1 and 63 points.
	 * @param s the point size of the Variable's TextDescriptor.
	 */
	public synchronized void setAbsSize(int s)
	{
		MutableTextDescriptor td = new MutableTextDescriptor(d.descriptor);
		td.setAbsSize(s);
		setTextDescriptor(td);
	}

	/**
	 * Method to set the text size of the Variable's TextDescriptor to a relative size (in units).
	 * The size must be between 0.25 and 127.75 grid units (in .25 increments).
	 * @param s the unit size of the Variable's TextDescriptor.
	 */
	public synchronized void setRelSize(double s)
	{
		MutableTextDescriptor td = new MutableTextDescriptor(d.descriptor);
		td.setRelSize(s);
		setTextDescriptor(td);
	}

	/**
	 * Method to return the text font of the Variable's TextDescriptor.
	 * @return the text font of the Variable's TextDescriptor.
	 */
	public int getFace() { return d.descriptor.getFace(); }

	/**
	 * Method to set the text font of the Variable's TextDescriptor.
	 * @param f the text font of the Variable's TextDescriptor.
	 */
	public synchronized void setFace(int f)
	{
		MutableTextDescriptor td = new MutableTextDescriptor(d.descriptor);
		td.setFace(f);
		setTextDescriptor(td);
	}

	/**
	 * Method to return the text rotation of the Variable's TextDescriptor.
	 * There are only 4 rotations: 0, 90 degrees, 180 degrees, and 270 degrees.
	 * @return the text rotation of the Variable's TextDescriptor.
	 */
	public TextDescriptor.Rotation getRotation() { return d.descriptor.getRotation(); }

	/**
	 * Method to set the text rotation of the Variable's TextDescriptor.
	 * There are only 4 rotations: 0, 90 degrees, 180 degrees, and 270 degrees.
	 * @param r the text rotation of the Variable's TextDescriptor.
	 */
	public synchronized void setRotation(TextDescriptor.Rotation r)
	{
		MutableTextDescriptor td = new MutableTextDescriptor(d.descriptor);
		td.setRotation(r);
		setTextDescriptor(td);
	}

	/**
	 * Method to return the text display part of the Variable's TextDescriptor.
	 * @return the text display part of the Variable's TextDescriptor.
	 */
	public TextDescriptor.DispPos getDispPart() { return d.descriptor.getDispPart(); }

	/**
	 * Method to set the text display part of the Variable's TextDescriptor.
	 * @param d the text display part of the Variable's TextDescriptor.
	 */
	public synchronized void setDispPart(TextDescriptor.DispPos dispPos)
	{
		MutableTextDescriptor td = new MutableTextDescriptor(d.descriptor);
		td.setDispPart(dispPos);
		setTextDescriptor(td);
	}

	/**
	 * Method to return true if the text in the Variable's TextDescriptor is italic.
	 * @return true if the text in the Variable's TextDescriptor is italic.
	 */
	public boolean isItalic() { return d.descriptor.isItalic(); }

	/**
	 * Method to set the text in the Variabl's TextDescriptor to be italic.
	 */
	public synchronized void setItalic(boolean state)
	{
		MutableTextDescriptor td = new MutableTextDescriptor(d.descriptor);
		td.setItalic(state);
		setTextDescriptor(td);
	}

	/**
	 * Method to return true if the text in the Variable's TextDescriptor is bold.
	 * @return true if the text in the Variable's TextDescriptor is bold.
	 */
	public boolean isBold() { return d.descriptor.isBold(); }

	/**
	 * Method to set the text in the TextDescriptor to be bold.
	 */
	public synchronized void setBold(boolean state)
	{
		MutableTextDescriptor td = new MutableTextDescriptor(d.descriptor);
		td.setBold(state);
		setTextDescriptor(td);
	}

	/**
	 * Method to return true if the text in the Variable's TextDescriptor is underlined.
	 * @return true if the text in the Variable's TextDescriptor is underlined.
	 */
	public boolean isUnderline() { return d.descriptor.isUnderline(); }

	/**
	 * Method to set the text in the Variable's TextDescriptor to be underlined.
	 */
	public synchronized void setUnderline(boolean state)
	{
		MutableTextDescriptor td = new MutableTextDescriptor(d.descriptor);
		td.setUnderline(state);
		setTextDescriptor(td);
	}

	/**
	 * Method to return true if the text in the Variable's TextDescriptor is interior.
	 * Interior text is not seen at higher levels of the hierarchy.
	 * @return true if the text in the Variable's TextDescriptor is interior.
	 */
	public boolean isInterior() { return d.descriptor.isInterior(); }

	/**
	 * Method to set the text in the Variable's TextDescriptor to be interior.
	 * Interior text is not seen at higher levels of the hierarchy.
	 */
	public synchronized void setInterior(boolean state)
	{
		MutableTextDescriptor td = new MutableTextDescriptor(d.descriptor);
		td.setInterior(state);
		setTextDescriptor(td);
	}

	/**
	 * Method to return true if the text in the Variable's TextDescriptor is inheritable.
	 * Inheritable variables copy their contents from prototype to instance.
	 * Only Variables on NodeProto and PortProto objects can be inheritable.
	 * When a NodeInst is created, any inheritable Variables on its NodeProto are automatically
	 * created on that NodeInst.
	 * @return true if the text in the Variable's TextDescriptor is inheritable.
	 */
	public boolean isInherit() { return d.descriptor.isInherit(); }

	/**
	 * Method to set the text in the Variable's TextDescriptor to be inheritable.
	 * Inheritable variables copy their contents from prototype to instance.
	 * Only Variables on NodeProto and PortProto objects can be inheritable.
	 * When a NodeInst is created, any inheritable Variables on its NodeProto are automatically
	 * created on that NodeInst.
	 */
	public synchronized void setInherit(boolean state)
	{
		MutableTextDescriptor td = new MutableTextDescriptor(d.descriptor);
		td.setInherit(state);
		setTextDescriptor(td);
	}

	/**
	 * Method to return true if the text in the Variable's TextDescriptor is a parameter.
	 * Parameters are those Variables that have values on instances which are
	 * passed down the hierarchy into the contents.
	 * Parameters can only exist on NodeInst objects.
	 * @return true if the text in the Variable's TextDescriptor is a parameter.
	 */
	public boolean isParam() {
        if (d.descriptor.isParam()) return true;
        // invariant: all attributes on nodeinsts that have a same
        // named parameter on their content views must be parameters.
        // It is possible for the user to create a case where an attributee
        // is not marked as a param even though it should be,
        // so we check it here
        if (getOwner() instanceof NodeInst) {
            NodeInst ni = (NodeInst)getOwner();
            if (ni.getProto() instanceof Cell) {
                Cell icon = (Cell)ni.getProto();
                Cell sch = icon.contentsView();
                if (sch == null) sch = icon;
                Variable var = sch.getVar(d.key);
                if (var != null && var.d.descriptor.isParam()) {
                    //this.setParam(true);
                    return true;
                }
            }
            //System.out.println("Var "+key.getName()+" on "+no.getName()+" is not a param");
        }
        return false;
    }

	/**
	 * Method to set the text in the Variable's TextDescriptor to be a parameter.
	 * Parameters are those Variables that have values on instances which are
	 * passed down the hierarchy into the contents.
	 * Parameters can only exist on NodeInst objects.
	 */
	public synchronized void setParam(boolean state)
	{
		MutableTextDescriptor td = new MutableTextDescriptor(d.descriptor);
		td.setParam(state);
		setTextDescriptor(td);
	}

	/**
	 * Method to return the X offset of the text in the Variable's TextDescriptor.
	 * @return the X offset of the text in the Variable's TextDescriptor.
	 */
	public synchronized double getXOff() { return d.descriptor.getXOff(); }

	/**
	 * Method to return the Y offset of the text in the Variable's TextDescriptor.
	 * @return the Y offset of the text in the Variable's TextDescriptor.
	 */
	public synchronized double getYOff() { return d.descriptor.getYOff(); }

	/**
	 * Method to set the X and Y offsets of the text in the Variable's TextDescriptor.
	 * The values are scaled by 4, so a value of 3 indicates a shift of 0.75 and a value of 4 shifts by 1.
	 * @param xd the X offset of the text in the Variable's TextDescriptor.
	 * @param yd the Y offset of the text in the Variable's TextDescriptor.
	 */
	public synchronized void setOff(double xd, double yd)
	{
		MutableTextDescriptor td = new MutableTextDescriptor(d.descriptor);
		td.setOff(xd, yd);
		setTextDescriptor(td);
	}

	/**
	 * Method to return the Unit of the Variable's TextDescriptor.
	 * Unit describes the type of real-world unit to apply to the value.
	 * For example, if this value is in volts, the Unit tells whether the value
	 * is volts, millivolts, microvolts, etc.
	 * @return the Unit of the Variable's TextDescriptor.
	 */
	public TextDescriptor.Unit getUnit() { return d.descriptor.getUnit(); }

	/**
	 * Method to set the Unit of the Variable's TextDescriptor.
	 * Unit describe the type of real-world unit to apply to the value.
	 * For example, if this value is in volts, the Unit tells whether the value
	 * is volts, millivolts, microvolts, etc.
	 * @param u the Unit of the Variable's TextDescriptor.
	 */
	public synchronized void setUnit(TextDescriptor.Unit u)
	{
		MutableTextDescriptor td = new MutableTextDescriptor(d.descriptor);
		td.setUnit(u);
		setTextDescriptor(td);
	}
}

