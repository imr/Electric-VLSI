package com.sun.electric.database.variable;

import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;

/**
 * The Variable class defines a single attribute-value pair that can be attached to any ElectricObject.
 */
public class Variable
{
	/**
	 * The Name class caches Variable names.
	 */
	public static class Name
	{
		private String name;
		private int    index;
		private static int currentIndex = 0;
		
		/**
		 * Routine to create a new Name object with the specified name.
		 * @param name the name of the Variable.
		 */
		public Name(String name)
		{
			this.name = name;
			this.index = currentIndex++;
		}

		/**
		 * Routine to return the index of this Name object.
		 * @return the index of this Name object.
		 */
		public int getIndex() { return index; }

		/**
		 * Routine to return the name of this Name object.
		 * @return the name of this Name object.
		 */
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

	/**
	 * The constructor builds a Variable from the given parameters.
	 * @param addr the object that will be stored in the Variable.
	 * @param descriptor a TextDescriptor to control how the Variable will be displayed.
	 * @param vn a Name object that identifies this Variable.
	 */
	public Variable(Object addr, TextDescriptor descriptor, Name vn)
	{
		this.addr = addr;
		this.descriptor = descriptor;
		this.vn = vn;
	}
	
	/**
	 * Routine to return the actual object stored in this variable.
	 * @return the actual object stored in this variable.
	 */
	public Object getObject() { return addr; }
	
	/**
	 * Routine to return the Variable Name associated with this variable.
	 * @return the Variable Name associated with this variable.
	 */
	public Name getName() { return vn; }
	
	/**
	 * Routine to return a description of this Variable.
	 * @return a description of this Variable.
	 */
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
	private String describe(int aindex, int purpose)
	{
		TextDescriptor.Units units = descriptor.getUnits();
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
					if (len > 1) returnVal += "[";
					for(int i=0; i<len; i++)
					{
						if (i != 0) returnVal += ",";
						returnVal += makeStringVar(addrArray[i], purpose, units);
					}
					if (len > 1) returnVal += "]";
				}
			} else returnVal += makeStringVar(addr, purpose, units);
		}
		return returnVal;
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
	public void setDescriptor(TextDescriptor descriptor) { this.descriptor = descriptor; }

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
	public void lowLevelSetFlags(int flags) { this.flags = flags; }

	/**
	 * Routine to set this Variable to be displayable.
	 * Displayable Variables are shown with the object.
	 */
	public void setDisplay() { flags |= VDISPLAY; }

	/**
	 * Routine to set this Variable to be not displayable.
	 * Displayable Variables are shown with the object.
	 */
	public void clearDisplay() { flags &= ~VDISPLAY; }

	/**
	 * Routine to return true if this Variable is displayable.
	 * @return true if this Variable is displayable.
	 */
	public boolean isDisplay() { return (flags & VDISPLAY) != 0; }

	/**
	 * Routine to set this Variable to be Java.
	 * Java Variables contain Java code that is evaluated in order to produce a value.
	 */
	public void setJava() { flags = (flags & ~(VCODE1|VCODE2)) | VJAVA; }

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
	public void setLisp() { flags = (flags & ~(VCODE1|VCODE2)) | VLISP; }

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
	public void setTCL() { flags = (flags & ~(VCODE1|VCODE2)) | VTCL; }

	/**
	 * Routine to return true if this Variable is TCL.
	 * TCL Variables contain TCL code that is evaluated in order to produce a value.
	 * Although the C version of Electric had a TCL interpreter in it, the Java version
	 * does not, so this facility is not implemented.
	 * @return true if this Variable is TCL.
	 */
	public boolean isTCL() { return (flags & (VCODE1|VCODE2)) == VTCL; }

	/**
	 * Routine to set this Variable to be not-code.
	 */
	public void clearCode() { flags &= ~(VCODE1|VCODE2); }

	/**
	 * Routine to set this Variable to be not-saved.
	 * Variables that are saved are written to disk when libraries are saved.
	 */
	public void setDontSave() { flags |= VDONTSAVE; }

	/**
	 * Routine to set this Variable to be saved.
	 * Variables that are saved are written to disk when libraries are saved.
	 */
	public void clearDontSave() { flags &= ~VDONTSAVE; }

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
	public void setCantSet() { flags |= VCANTSET; }

	/**
	 * Routine to set this Variable to be settable.
	 * Only Variables that are settable can have their value changed.
	 */
	public void clearCantSet() { flags &= ~VCANTSET; }

	/**
	 * Routine to return true if this Variable is settable.
	 * Only Variables that are settable can have their value changed.
	 * @return true if this Variable is settable.
	 */
	public boolean isCantSet() { return (flags & VCANTSET) != 0; }
}

