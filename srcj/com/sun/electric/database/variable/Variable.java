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

import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;

import java.text.*;

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
		 * Routine to create a new Key object with the specified name.
		 * @param name the name of the Variable.
		 */
		public Key(String name)
		{
			this.name = name;
			this.index = currentIndex++;
		}

		/**
		 * Routine to return the index of this Key object.
		 * @return the index of this Key object.
		 */
		public int getIndex() { return index; }

		/**
		 * Routine to return the name of this Key object.
		 * @return the name of this Key object.
		 */
		public String getName() { return name; }
	}

	private ElectricObject owner;
	private Object addr;
	private Key key;
	private int flags;
	private TextDescriptor descriptor;

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
	Variable(ElectricObject owner, Object addr, TextDescriptor descriptor, Key key)
	{
		this.owner = owner;
		this.addr = addr;
		this.descriptor = descriptor;
		this.key = key;
	}
    
	/**
	 * Clear variable field to catch improper reference of removed variable.
	 */
	void kill()
	{
		owner = null;
		addr = null;
		descriptor = null;
		key = null;
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
     * Set the actual object stored in this Variable.
	 * @param the object to be stored in this variable .
     */
    void setObject(Object addr)
	{
		// owner.checkChanging(); this is not necessary,
		// because this method can be called only from this package
		this.addr = addr;
	}
    
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
	 * Routine to return the Variable Key associated with this Variable.
	 * @return the Variable Key associated with this variable.
	 */
	public Key getKey() { return key; }

	/**
	 * Routine to return a more readable name for this Variable.
	 * The routine adds "Parameter" or "Attribute" as appropriate
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

	private String betterVariableName(String name)
	{
		/* handle standard variable names */
		if (name.equals("ARC_name")) return "Arc Name";
		if (name.equals("ARC_radius")) return "Arc Radius";
		if (name.equals("ART_color")) return "Color";
		if (name.equals("ART_degrees")) return "Number of Degrees";
		if (name.equals("ART_message")) return "Text";
		if (name.equals("NET_ncc_match")) return "NCC equivalence";
		if (name.equals("NET_ncc_forcedassociation")) return "NCC association";
		if (name.equals("NODE_name")) return "Node Name";
		if (name.equals("SCHEM_capacitance")) return "Capacitance";
		if (name.equals("SCHEM_diode")) return "Diode Size";
		if (name.equals("SCHEM_inductance")) return "Inductance";
		if (name.equals("SCHEM_resistance")) return "Resistance";
		if (name.equals("SIM_fall_delay")) return "Fall Delay";
		if (name.equals("SIM_fasthenry_group_name")) return "FastHenry Group";
		if (name.equals("SIM_rise_delay")) return "Rise Delay";
		if (name.equals("SIM_spice_model")) return "SPICE model";
		if (name.equals("transistor_width")) return "Transistor Width";
		if (name.equals("SCHEM_global_name")) return "Global Signal Name";
		return null;
	}

	/**
	 * Routine to return the "true" name for this Variable.
	 * The routine removes the "ATTR_" and "ATTRP_" prefixes.
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

    /** number format */ private static final DecimalFormat df = new DecimalFormat("#######.###");
    /** field position */ private static final FieldPosition fp = new FieldPosition(NumberFormat.INTEGER_FIELD);
    /** Format a float to something sensible that can be printed */
    public static String format(float num) {
        StringBuffer buf = new StringBuffer();
        df.format(num, buf, fp);
        return buf.toString();
    }
    
	/**
	 * Routine to return a description of this Variable.
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
	 * routine to make a printable string from variable "val", array index
	 * "aindex".  If "aindex" is negative, print the entire array.  If "purpose" is
	 * zero, the conversion is for human reading and should be easy to understand.
	 * If "purpose" is positive, the conversion is for machine reading and should
	 * be easy to parse.  If "purpose" is negative, the conversion is for
	 * parameter substitution and should be easy to understand but not hard to
	 * parse (a combination of the two).
	 */
	public String describe(int aindex, int purpose, VarContext context, ElectricObject eobj)
	{
		TextDescriptor.Units units = descriptor.getUnits();
		StringBuffer returnVal = new StringBuffer();
		TextDescriptor.DispPos dispPos = descriptor.getDispPart();
		String whichIndex = "";

		if ((flags & (VCODE1|VCODE2)) != 0)
		{
			/* special case for code: it is a string, the type applies to the result */
			//makeStringVar(VSTRING, var->addr, purpose, units, infstr);
            Object val = context.evalVar(this, eobj);
            if (val == null) 
                returnVal.append("?");
            else
                returnVal.append(makeStringVar(val, purpose, units));
        } else
		{
			if (addr instanceof Object[])
			{
				/* compute the array length */
				Object [] addrArray = (Object []) addr;
				int len = addrArray.length;

				/* if asking for a single entry, get it */
				if (aindex >= 0)
				{
					/* normal array indexing */
					whichIndex = "[" + aindex + "]";
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
		}
		if (dispPos == TextDescriptor.DispPos.NAMEVALUE ||
            dispPos == TextDescriptor.DispPos.NAMEVALINH ||
            dispPos == TextDescriptor.DispPos.NAMEVALINHALL)
		{
			return this.getTrueName() + whichIndex + "=" + returnVal.toString();
		}
        /*
		if (dispPos == TextDescriptor.DispPos.NAMEVALINH)
		{
			return this.getTrueName() + whichIndex + "=?;def=" + returnVal.toString();
		}
		if (dispPos == TextDescriptor.DispPos.NAMEVALINHALL)
		{
			return this.getTrueName() + whichIndex + "=?;def=" + returnVal.toString();
		}*/
		return returnVal.toString();
	}

	/**
	 * Routine to convert object "addr" to a string, given a purpose and a set of units.
	 * For completion of the routine, the units should be treated as in "db_makestringvar()".
	 */
	private String makeStringVar(Object addr, int purpose, TextDescriptor.Units units)
	{
		if (addr instanceof Integer)
			return ((Integer)addr).toString();
		if (addr instanceof Float)
			return (format(((Float)addr).floatValue())); // only display up to 3 significant figures
		if (addr instanceof Double)
			return ((Double)addr).toString();
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
	 * Routine to return the TextDescriptor on this Variable.
	 * The TextDescriptor gives information for displaying the Variable.
	 * @return the TextDescriptor on this Variable.
	 */
	public TextDescriptor getTextDescriptor() { return descriptor; }

	/**
	 * Routine to set the TextDescriptor on this Variable.
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param descriptor the new TextDescriptor on this Variable.
	 */
	public void setDescriptor(TextDescriptor descriptor) { owner.checkChanging(); this.descriptor = descriptor; }

	/**
	 * Low-level routine to get the type bits.
	 * The "type bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the binary ".elib"
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the "type bits".
	 */
	public int lowLevelGetFlags() { return flags; }

	/**
	 * Low-level routine to set the type bits.
	 * The "type bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the binary ".elib"
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @param flags the new "type bits".
	 */
	public void lowLevelSetFlags(int flags)
	{
		owner.checkChanging();
		boolean oldDisplay = (this.flags & VDISPLAY) != 0;
		boolean newDisplay = (flags & VDISPLAY) != 0;
		this.flags = flags;
		if (oldDisplay != newDisplay)
			owner.updateDisplayable(this);
	}

	/**
	 * Routine to set this Variable to be displayable.
	 * Displayable Variables are shown with the object.
	 */
	public void setDisplay()
	{
		owner.checkChanging();
		boolean old = (flags & VDISPLAY) != 0;
		flags |= VDISPLAY;
		if (!old)
			owner.updateDisplayable(this);
	}

	/**
	 * Routine to set this Variable to be not displayable.
	 * Displayable Variables are shown with the object.
	 */
	public void clearDisplay()
	{
		owner.checkChanging();
		boolean old = (flags & VDISPLAY) != 0;
		flags &= ~VDISPLAY;
		if (old)
			owner.updateDisplayable(this);
	}

	/**
	 * Routine to return true if this Variable is displayable.
	 * @return true if this Variable is displayable.
	 */
	public boolean isDisplay() { return (flags & VDISPLAY) != 0; }

	/**
	 * Routine to set this Variable to be Java.
	 * Java Variables contain Java code that is evaluated in order to produce a value.
	 */
	public void setJava() { owner.checkChanging(); flags = (flags & ~(VCODE1|VCODE2)) | VJAVA; }

	/**
	 * Routine to return true if this Variable is Java.
	 * Java Variables contain Java code that is evaluated in order to produce a value.
	 * @return true if this Variable is Java.
	 */
	public boolean isJava() { return (flags & (VCODE1|VCODE2)) == VJAVA; }

	/**
	 * Routine to set this Variable to be Lisp.
	 * Lisp Variables contain Lisp code that is evaluated in order to produce a value.
	 * Although the C version of Electric had a Lisp interpreter in it, the Java version
	 * does not, so this facility is not implemented.
	 */
	public void setLisp() { owner.checkChanging(); flags = (flags & ~(VCODE1|VCODE2)) | VLISP; }

	/**
	 * Routine to return true if this Variable is Lisp.
	 * Lisp Variables contain Lisp code that is evaluated in order to produce a value.
	 * Although the C version of Electric had a Lisp interpreter in it, the Java version
	 * does not, so this facility is not implemented.
	 * @return true if this Variable is Lisp.
	 */
	public boolean isLisp() { return (flags & (VCODE1|VCODE2)) == VLISP; }

	/**
	 * Routine to set this Variable to be TCL.
	 * TCL Variables contain TCL code that is evaluated in order to produce a value.
	 * Although the C version of Electric had a TCL interpreter in it, the Java version
	 * does not, so this facility is not implemented.
	 */
	public void setTCL() { owner.checkChanging(); flags = (flags & ~(VCODE1|VCODE2)) | VTCL; }

	/**
	 * Routine to return true if this Variable is TCL.
	 * TCL Variables contain TCL code that is evaluated in order to produce a value.
	 * Although the C version of Electric had a TCL interpreter in it, the Java version
	 * does not, so this facility is not implemented.
	 * @return true if this Variable is TCL.
	 */
	public boolean isTCL() { return (flags & (VCODE1|VCODE2)) == VTCL; }

	/**
	 * Routine to tell whether this Variable is any code.
	 * @return true if this Variable is any code.
	 */
	public boolean isCode() { return (flags & (VCODE1|VCODE2)) != 0; }

	/**
	 * Routine to set this Variable to be not-code.
	 */
	public void clearCode() { owner.checkChanging(); flags &= ~(VCODE1|VCODE2); }

	/**
	 * Routine to set this Variable to be not-saved.
	 * Variables that are saved are written to disk when libraries are saved.
	 */
	public void setDontSave() { owner.checkChanging(); flags |= VDONTSAVE; }

	/**
	 * Routine to set this Variable to be saved.
	 * Variables that are saved are written to disk when libraries are saved.
	 */
	public void clearDontSave() { owner.checkChanging(); flags &= ~VDONTSAVE; }

	/**
	 * Routine to return true if this Variable is to be saved.
	 * Variables that are saved are written to disk when libraries are saved.
	 * @return true if this Variable is to be saved.
	 */
	public boolean isDontSave() { return (flags & VDONTSAVE) != 0; }

	/**
	 * Routine to set this Variable to be not-settable.
	 * Only Variables that are settable can have their value changed.
	 */
	public void setCantSet() { owner.checkChanging(); flags |= VCANTSET; }

	/**
	 * Routine to set this Variable to be settable.
	 * Only Variables that are settable can have their value changed.
	 */
	public void clearCantSet() { owner.checkChanging(); flags &= ~VCANTSET; }

	/**
	 * Routine to return true if this Variable is settable.
	 * Only Variables that are settable can have their value changed.
	 * @return true if this Variable is settable.
	 */
	public boolean isCantSet() { return (flags & VCANTSET) != 0; }
}

