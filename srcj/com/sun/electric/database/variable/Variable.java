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

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.technologies.Generic;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * The Variable class defines a single attribute-value pair that can be attached to any ElectricObject.
 */
public class Variable
{
	/**
	 * The Key class caches Variable names.
	 */
	public static class Key
	{
		private String name;
		private int    index;
		private static int currentIndex = 0;
		
		/**
		 * Method to create a new Key object with the specified name.
		 * @param name the name of the Variable.
		 */
		public Key(String name)
		{
			this.name = name;
			this.index = currentIndex++;
		}

		/**
		 * Method to return the index of this Key object.
		 * @return the index of this Key object.
		 */
		public int getIndex() { return index; }

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
	}

    /**
     * The type of Code that determines how this Variable's
     * value should be evaluated. If NONE, no evaluation is done.
     */
    public static class Code {
        private final String name;
        private static final ArrayList allCodes = new ArrayList();

        public Code(String name) {
            this.name = name;
            allCodes.add(this);
        }

        public String toString() { return name; }

        /** Get an iterator over all Code types */
        public static Iterator getCodes() { return allCodes.iterator(); }

        public static final Code JAVA = new Code("Java");
        public static final Code LISP = new Code("Lisp (not avail.)");
        public static final Code TCL = new Code("TCL (not avail.)");
        public static final Code NONE = new Code("Not Code");
    }


	private Object addr;
	private Key key;
	private int flags;
	private TextDescriptor descriptor;

    /** true if var is attached to valid electric object */ private boolean linked;

	/** variable is interpreted code (with VCODE2) */	private static final int VCODE1 =                040;
	/** display variable (uses textdescript field) */	private static final int VDISPLAY =             0100;
//	/** variable points into C structure */				private static final int VCREF =                0400;
	/** variable is interpreted code (with VCODE1) */	private static final int VCODE2 =        04000000000;
	/** variable is LISP */								private static final int VLISP =              VCODE1;
	/** variable is TCL */								private static final int VTCL =               VCODE2;
	/** variable is Java */								private static final int VJAVA =      (VCODE1|VCODE2);
	/** set to prevent saving on disk */				private static final int VDONTSAVE =    010000000000;
	/** set to prevent changing value */				private static final int VCANTSET =     020000000000;

	/**
	 * The constructor builds a Variable from the given parameters.
	 * @param owner the ElectriObject that owns this variable.
	 * @param addr the object that will be stored in the Variable.
	 * @param descriptor a TextDescriptor to control how the Variable will be displayed.
	 * @param key a Key object that identifies this Variable.
	 */
	protected Variable(ElectricObject owner, Object addr, TextDescriptor descriptor, Key key)
	{
        // user input text may describe a number that is not parsable by the Java Bean Shell
        // (such as 0.01p, which equals 0.01E-12). To prevent this being a problem, any String
        // that is convertible to a Number via the above definition is done so here.
/*
        if (addr instanceof String) {
            try {
                Number n = TextUtils.parsePostFixNumber((String)addr);
                String s = (String)addr;
                addr = n;
                System.out.print("For Variable "+key.getName()+": ");
                if (n instanceof Integer) System.out.println("Converted "+s+" to Integer "+(Integer)n);
                if (n instanceof Long) System.out.println("Converted "+s+" to Long "+(Long)n);
                if (n instanceof Double) System.out.println("Converted "+s+" to Double "+(Double)n);
            } catch (java.lang.NumberFormatException e) {}
        }
*/
		this.addr = addr;
		this.descriptor = new TextDescriptor(owner, descriptor);
		this.key = key;
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
       	boolean check = var.getTextDescriptor().compare(getTextDescriptor());

		if (!check && buffer != null)
			buffer.append("No same variables detected in " + var + " and " + this + "\n");
        return (check);
    }

	/**
	 * Method to check if this Variable can be changed.
	 */
	public final void checkChanging()
	{
		ElectricObject owner = descriptor.owner;
		if (owner.isDummyObject()) return;
		owner.checkChanging();

		// handle change control, constraint, and broadcast
		Undo.modifyVariableFlags(owner, this, flags);
	}

    /**
     * Get the number of entries stored in this Variable.
	 * For non-arrayed Variables, this is 1.
     * @return the number of entries stored in this Variable.
     */
    public int getLength()
	{
		if (addr instanceof Object[])
			return ((Object [])addr).length;
		return 1;
	}
    
    /**
     * Get the actual object stored in this Variable.
     * @return the object stored in this Variable.
     */
    public Object getObject() { return addr; }

    /** 
     * Treat the stored Object as an array of Objects and
     * get the object at index @param index.
     * @param index index into the array of objects.
     * @return the objects stored in this Variable at the index.
     */
    public Object getObject(int index)
    {
		if (!(addr instanceof Object[])) return null;
		return ((Object[]) addr)[index];
    }
        
	/**
	 * Low-level method to insert to object array.
	 * This should not normally be called by any other part of the system.
	 * @param index insertion index
	 * @param value object to insert
	 */
	public void lowLevelInsert(int index, Object value)
	{
		Object[] oldArr = (Object[])addr;
		Object[] newArr = new Object[oldArr.length+1];
		for (int i = 0; i < index; i++) newArr[i] = oldArr[i];
		newArr[index] = value;
		for (int i = index; i < oldArr.length; i++) newArr[i+1] = oldArr[i];
		addr = newArr;
	}

	/**
	 * Low-level method to delete from object array.
	 * This should not normally be called by any other part of the system.
	 * This should not normally be called by any other part of the system.
	 * @param index deletion index
	 */
	public void lowLevelDelete(int index)
	{
		Object[] oldArr = (Object[])addr;
		Object[] newArr = new Object[oldArr.length-1];
		for (int i = 0; i < index; i++) newArr[i] = oldArr[i];
		for (int i = index; i < newArr.length; i++) newArr[i] = oldArr[i+1];
		addr = newArr;
	}

	/**
	 * Method to return the Variable Key associated with this Variable.
	 * @return the Variable Key associated with this variable.
	 */
	public Key getKey() { return key; }

    /**
     * Set if this variable is linked to an ElectricObject
     */
    public void setLinked(boolean linked) { this.linked = linked; }

    /**
     * Returns true if variable is linked to a linked database object, false otherwise.
     * @return true if variable is linked to a linked database object, false otherwise.
     */
    public boolean isLinked() { return (linked && descriptor.owner.isLinked()); }

    /**
     * Get the Electric object that stores this Variable
     * @return the Owner of this Variable
     */
    public ElectricObject getOwner() { return descriptor.owner; }

	/**
	 * Method to return a more readable name for this Variable.
	 * The method adds "Parameter" or "Attribute" as appropriate
	 * and uses sensible names such as "Diode Size" instead of "SCHEM_diode".
	 * @return a more readable name for this Variable.
	 */
	public String getReadableName()
	{
		String trueName = "";
		String name = key.getName();
		if (name.startsWith("ATTR_"))
		{
			if (getTextDescriptor().isParam())
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
			description = trueName + " on export '" + ((Export)eobj).getName() + "'";
		} else if (eobj instanceof PortInst)
		{
			PortInst pi = (PortInst)eobj;
			description = trueName + " on port " + pi.getPortProto().getName() +
				" of " + pi.getNodeInst().describe();
		} else if (eobj instanceof ArcInst)
		{
			description = trueName + " on " + ((ArcInst)eobj).describe();
		} else if (eobj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)eobj;
			description = trueName + " on " + ni.describe();
			if (ni.getProto() == Generic.tech.invisiblePinNode)
			{
				String varName = getKey().getName();
				String betterName = betterVariableName(varName);
				if (betterName != null) description = betterName;
			}
		} else if (eobj instanceof Cell)
		{
			description = trueName + " of cell " + ((Cell)eobj).describe();
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
		/* handle standard variable names */
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
		String name = key.getName();
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
	public String describe(VarContext context, ElectricObject eobj)
	{
		return describe(-1, -1, context, eobj);
	}

    /** 
     * Return a description of this Variable without any context
     * or helper object info
     */
    public String describe(int aindex, int purpose)
    {
        return describe(aindex, purpose, VarContext.globalContext, null);
    }

	/**
	 * Method to return a String describing this Variable.
	 * @param aindex if negative, print the entire array.
	 * @param purpose if zero, the conversion is for human reading and should be easy to understand.
	 * If positive, the conversion is for machine reading and should be easy to parse.
	 * If negative, the conversion is for parameter substitution and should be easy to understand
	 * but not hard to parse (a combination of the two).
	 * @param context the VarContext for this Variable.
	 * @param eobj the ElectricObject on which this Variable resides.
	 * @return a String desribing this Variable.
	 */
	public String describe(int aindex, int purpose, VarContext context, ElectricObject eobj)
	{
		TextDescriptor.Unit units = descriptor.getUnit();
		StringBuffer returnVal = new StringBuffer();
		TextDescriptor.DispPos dispPos = descriptor.getDispPart();
		String whichIndex = "";

		if ((flags & (VCODE1|VCODE2)) != 0)
		{
			/* special case for code: it is a string, the type applies to the result */
			//makeStringVar(VSTRING, var->addr, purpose, units, infstr);
            if (context == null) context = VarContext.globalContext;
            Object val = null;
            try {
                val = context.evalVarRecurse(this, eobj);
            } catch (VarContext.EvalException e) {
                val = e.getMessage();
            }
            if (val == null) val = "?";
            returnVal.append(makeStringVar(val, purpose, units));
        } else
		{
			returnVal.append(getPureValue(aindex, purpose));
			if (addr instanceof Object[] && aindex >= 0)
			{
				/* normal array indexing */
				whichIndex = "[" + aindex + "]";
			}
		}
        if (dispPos == TextDescriptor.DispPos.NAMEVALUE)
		{
			return this.getTrueName() + whichIndex + "=" + returnVal.toString();
		}
		return returnVal.toString();
	}

	/**
	 * Method to convert this Variable to a String without any evaluation of code.
	 * @param aindex if negative, print the entire array.
	 * @param purpose if zero, the conversion is for human reading and should be easy to understand.
	 * If positive, the conversion is for machine reading and should be easy to parse.
	 * If negative, the conversion is for parameter substitution and should be easy to understand
	 * but not hard to parse (a combination of the two).
	 * @return a String desribing this Variable.
	 */
	public String getPureValue(int aindex, int purpose)
	{
		TextDescriptor.Unit units = descriptor.getUnit();
		StringBuffer returnVal = new StringBuffer();
		if (addr instanceof Object[])
		{
			/* compute the array length */
			Object [] addrArray = (Object [])addr;
			int len = addrArray.length;

			/* if asking for a single entry, get it */
			if (aindex >= 0)
			{
				/* normal array indexing */
				if (aindex < len)
					returnVal.append(makeStringVar(addrArray[aindex], purpose, units));
			} else
			{
				/* in an array, quote strings */
				if (purpose < 0) purpose = 0;
				if (len > 1) returnVal.append("[");
				for(int i=0; i<len; i++)
				{
					if (i != 0) returnVal.append(",");
					returnVal.append(makeStringVar(addrArray[i], purpose, units));
				}
				if (len > 1) returnVal.append("]");
			}
		} else
		{
			returnVal.append(makeStringVar(addr, purpose, units));
		}
		return returnVal.toString();
	}

	/**
	 * Method to convert object "addr" to a string, given a purpose and a set of units.
	 * For completion of the method, the units should be treated as in "makeStringVar()".
	 */
	private String makeStringVar(Object addr, int purpose, TextDescriptor.Unit units)
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
			return ((NodeInst)addr).describe();
		if (addr instanceof ArcInst)
			return ((ArcInst)addr).describe();
		return "?";
	}

	/**
	 * Method to return the TextDescriptor on this Variable.
	 * The TextDescriptor gives information for displaying the Variable.
	 * @return the TextDescriptor on this Variable.
	 */
	public TextDescriptor getTextDescriptor() { return descriptor; }

	/**
	 * Method to set the TextDescriptor on this Variable.
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param descriptor the new TextDescriptor on this Variable.
	 */
	public void setTextDescriptor(TextDescriptor descriptor) { this.descriptor.copy(descriptor); }

	/**
	 * Low-level method to get the type bits.
	 * The "type bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the "type bits".
	 */
	public int lowLevelGetFlags() { return flags; }

	/**
	 * Low-level method to set the type bits.
	 * The "type bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @param flags the new "type bits".
	 */
	public void lowLevelSetFlags(int flags) { this.flags = flags; }

	/**
	 * Method to copy flags from another variable.
	 * @param var another variable.
	 */
	public void copyFlags(Variable var) { checkChanging(); this.flags = var.flags; }

	/**
	 * Method to set this Variable to be displayable.
	 * Displayable Variables are shown with the object.
	 */
	public void setDisplay(boolean state)
    {
        checkChanging();
        if (state)
            flags |= VDISPLAY;
        else
            flags &= ~VDISPLAY;
    }

	/**
	 * Method to return true if this Variable is displayable.
	 * @return true if this Variable is displayable.
	 */
	public boolean isDisplay() { return (flags & VDISPLAY) != 0; }

    /**
     * Determine what code type this variable has, if any
     * @return the code type
     */
    public Code getCode() {
        if (isJava()) return Code.JAVA;
        if (isTCL()) return Code.TCL;
        if (isLisp()) return Code.LISP;
        return Code.NONE;
    }

    /**
     * Sets the code type of this Variable
     * @param code the code to set to
     */
    public void setCode(Code code) {
        if (code == Code.JAVA) setJava();
        if (code == Code.LISP) setLisp();
        if (code == Code.TCL) setTCL();
        if (code == Code.NONE) clearCode();
    }

	/**
	 * Method to set this Variable to be Java.
	 * Java Variables contain Java code that is evaluated in order to produce a value.
	 */
	private void setJava() { checkChanging(); flags = (flags & ~(VCODE1|VCODE2)) | VJAVA; }

	/**
	 * Method to return true if this Variable is Java.
	 * Java Variables contain Java code that is evaluated in order to produce a value.
	 * @return true if this Variable is Java.
	 */
	private boolean isJava() { return (flags & (VCODE1|VCODE2)) == VJAVA; }

	/**
	 * Method to set this Variable to be Lisp.
	 * Lisp Variables contain Lisp code that is evaluated in order to produce a value.
	 * Although the C version of Electric had a Lisp interpreter in it, the Java version
	 * does not, so this facility is not implemented.
	 */
	private void setLisp() { checkChanging(); flags = (flags & ~(VCODE1|VCODE2)) | VLISP; }

	/**
	 * Method to return true if this Variable is Lisp.
	 * Lisp Variables contain Lisp code that is evaluated in order to produce a value.
	 * Although the C version of Electric had a Lisp interpreter in it, the Java version
	 * does not, so this facility is not implemented.
	 * @return true if this Variable is Lisp.
	 */
	private boolean isLisp() { return (flags & (VCODE1|VCODE2)) == VLISP; }

	/**
	 * Method to set this Variable to be TCL.
	 * TCL Variables contain TCL code that is evaluated in order to produce a value.
	 * Although the C version of Electric had a TCL interpreter in it, the Java version
	 * does not, so this facility is not implemented.
	 */
	private void setTCL() { checkChanging(); flags = (flags & ~(VCODE1|VCODE2)) | VTCL; }

	/**
	 * Method to return true if this Variable is TCL.
	 * TCL Variables contain TCL code that is evaluated in order to produce a value.
	 * Although the C version of Electric had a TCL interpreter in it, the Java version
	 * does not, so this facility is not implemented.
	 * @return true if this Variable is TCL.
	 */
	private boolean isTCL() { return (flags & (VCODE1|VCODE2)) == VTCL; }

	/**
	 * Method to tell whether this Variable is any code.
	 * @return true if this Variable is any code.
	 */
	public boolean isCode() { return (flags & (VCODE1|VCODE2)) != 0; }

	/**
	 * Method to set this Variable to be not-code.
	 */
	private void clearCode() { checkChanging(); flags &= ~(VCODE1|VCODE2); }

	/**
	 * Method to set this Variable to be not-saved.
	 * Variables that are saved are written to disk when libraries are saved.
	 */
	public void setDontSave() { checkChanging(); flags |= VDONTSAVE; }

	/**
	 * Method to set this Variable to be saved.
	 * Variables that are saved are written to disk when libraries are saved.
	 */
	public void clearDontSave() { checkChanging(); flags &= ~VDONTSAVE; }

	/**
	 * Method to return true if this Variable is to be saved.
	 * Variables that are saved are written to disk when libraries are saved.
	 * @return true if this Variable is to be saved.
	 */
	public boolean isDontSave() { return (flags & VDONTSAVE) != 0; }

	/**
	 * Method to set this Variable to be not-settable.
	 * Only Variables that are settable can have their value changed.
	 */
	public void setCantSet() { checkChanging(); flags |= VCANTSET; }

	/**
	 * Method to set this Variable to be settable.
	 * Only Variables that are settable can have their value changed.
	 */
	public void clearCantSet() { checkChanging(); flags &= ~VCANTSET; }

	/**
	 * Method to return true if this Variable is settable.
	 * Only Variables that are settable can have their value changed.
	 * @return true if this Variable is settable.
	 */
	public boolean isCantSet() { return (flags & VCANTSET) != 0; }

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
		return key.getName();
	}
}

