package com.sun.electric.database.variable;

import com.sun.electric.database.hierarchy.VarContext;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;

/**
 * This class is the superclass of all Electric objects that can be extended with "Variables".
 */
public class Variable
{
	public static class Name
	{
		String name;
		int    index;
		static int currentIndex = 0;
		
		Name(String name)
		{
			this.name = name;
			this.index = currentIndex++;
		}
		public int getIndex() { return index; }
		public String getName() { return name; }
	}

	private Object addr;
	private Name vn;
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

	public Variable(Object addr, TextDescriptor descriptor, Name vn)
	{
		this.addr = addr;
		this.descriptor = descriptor;
		this.vn = vn;
	}
	
	/** Routine to return the actual object stored in this variable. */
	public Object getObject() { return addr; }
	
	/** Routine to return the name associated with this variable. */
	public Name getName() { return vn; }

	public String describe()
	{
		return describe(-1, -1);
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
	String describe(int aindex, int purpose)
	{
		TextDescriptor.Units units = descriptor.TDGetUnits();
		String returnVal = "";

//		if ((flags & (VCODE1|VCODE2)) != 0)
//		{
//			/* special case for code: it is a string, the type applies to the result */
//			makeStringVar(VSTRING, var->addr, purpose, units, infstr);
//		} else
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
					if (aindex < len)
						returnVal += makeStringVar(addrArray[aindex], purpose, units);
				} else
				{
					/* in an array, quote strings */
					if (purpose < 0) purpose = 0;
					returnVal += "[";
					for(int i=0; i<len; i++)
					{
						if (i != 0) returnVal += ",";
						returnVal += makeStringVar(addrArray[i], purpose, units);
					}
					returnVal += "]";
				}
			} else returnVal += makeStringVar(addr, purpose, units);
		}
		return returnVal;
	}

	/**
	 * Routine to convert object "addr" to a string, given a purpose and a set of units.
	 * For completion of the routine, the units should be treated as in "db_makestringvar()".
	 */
	String makeStringVar(Object addr, int purpose, TextDescriptor.Units units)
	{
		if (addr instanceof Integer)
			return ((Integer)addr).toString();
		if (addr instanceof Float)
			return ((Float)addr).toString();
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

//	String trueVariableName()
//	{
//		name = makename(var->key);
//		if (estrncmp(name, x_("ATTR_"), 5) == 0)
//			return(name + 5);
//		if (estrncmp(name, x_("ATTRP_"), 6) == 0)
//		{
//			len = estrlen(name);
//			for(i=len-1; i>=0; i--) if (name[i] == '_') break;
//			return(name + i);
//		}
//		return(name);
//	}
	public TextDescriptor getTextDescriptor() { return descriptor; }
	public void setDescriptor(TextDescriptor descriptor) { this.descriptor = descriptor; }
	public int lowLevelGetFlags() { return flags; }
	public void lowLevelSetFlags(int flags) { this.flags = flags; }

	/** Set the Display bit */
	public void setDisplay() { flags |= VDISPLAY; }
	/** Clear the Display bit */
	public void clearDisplay() { flags &= ~VDISPLAY; }
	/** Get the Display bit */
	public boolean isDisplay() { return (flags & VDISPLAY) != 0; }

	/** Set the Variable to be Java bits */
	public void setJava() { flags = (flags & ~(VCODE1|VCODE2)) | VJAVA; }
	/** Get the is-Java bit */
	public boolean isJava() { return (flags & (VCODE1|VCODE2)) == VJAVA; }
	/** Set the Variable to be Lisp bits */
	public void setLisp() { flags = (flags & ~(VCODE1|VCODE2)) | VLISP; }
	/** Get the is-Java bit */
	public boolean isLisp() { return (flags & (VCODE1|VCODE2)) == VLISP; }
	/** Set the Variable to be TCL bits */
	public void setTCL() { flags = (flags & ~(VCODE1|VCODE2)) | VTCL; }
	/** Get the is-Java bit */
	public boolean isTCL() { return (flags & (VCODE1|VCODE2)) == VTCL; }
	/** Clear the Language bits */
	public void clearCode() { flags &= ~(VCODE1|VCODE2); }

	/** Set the Don't-Save bit */
	public void setDontSave() { flags |= VDONTSAVE; }
	/** Clear the Don't-Save bit */
	public void clearDontSave() { flags &= ~VDONTSAVE; }
	/** Get the Don't-Save bit */
	public boolean isDontSave() { return (flags & VDONTSAVE) != 0; }

	/** Set the Can't-Set bit */
	public void setCantSet() { flags |= VCANTSET; }
	/** Clear the Can't-Set bit */
	public void clearCantSet() { flags &= ~VCANTSET; }
	/** Get the Can't-Set bit */
	public boolean isCantSet() { return (flags & VCANTSET) != 0; }
}

