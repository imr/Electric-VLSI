package com.sun.electric.database.variable;

import com.sun.electric.database.hierarchy.VarContext;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.tool.user.Electric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * This class is the superclass of all Electric objects that can be extended with "Variables".
 */
public class ElectricObject
{
	/** The Integer class */							public static Class INTEGERCLASS      = (new Integer(0)).getClass();
	/** The String class */								public static Class STRINGCLASS       = (new String()).getClass();
	/** The Short class */								public static Class SHORTCLASS        = (new Short((short)0)).getClass();
	/** The Float class */								public static Class FLOATCLASS        = (new Float(0)).getClass();
	/** The Double class */								public static Class DOUBLECLASS       = (new Double(0)).getClass();
	/** The Byte class */								public static Class BYTECLASS         = (new Byte((byte)0)).getClass();
	/** The Integer class */							public static Class INTEGERARRAYCLASS = (new Integer[0]).getClass();
	/** The String class */								public static Class STRINGARRAYCLASS  = (new String[0]).getClass();
	/** The Short class */								public static Class SHORTARRAYCLASS   = (new Short[0]).getClass();
	/** The Float class */								public static Class FLOATARRAYCLASS   = (new Float[0]).getClass();
	/** The Double class */								public static Class DOUBLEARRAYCLASS  = (new Double[0]).getClass();
	/** The Byte class */								public static Class BYTEARRAYCLASS    = (new Byte[0]).getClass();

	/** variable is interpreted code (with VCODE2) */	private static final int VCODE1 =                040;
	/** display variable (uses textdescript field) */	private static final int VDISPLAY =             0100;
//	/** variable points into C structure */				private static final int VCREF =                0400;
	/** variable is interpreted code (with VCODE1) */	private static final int VCODE2 =        04000000000;
	/** variable is LISP */								private static final int VLISP =              VCODE1;
	/** variable is TCL */								private static final int VTCL =               VCODE2;
	/** variable is Java */								private static final int VJAVA =      (VCODE1|VCODE2);
	/** set to prevent saving on disk */				private static final int VDONTSAVE =    010000000000;
	/** set to prevent changing value */				private static final int VCANTSET =     020000000000;

	public static class Variable
	{
		private Object addr;
		private int flags;
		private TextDescriptor descriptor;

		public Variable(Object addr, TextDescriptor descriptor)
		{
			this.addr = addr;
			this.descriptor = descriptor;
		}
		public Object getObject() { return addr; }

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

//			if ((flags & (VCODE1|VCODE2)) != 0)
//			{
//				/* special case for code: it is a string, the type applies to the result */
//				makeStringVar(VSTRING, var->addr, purpose, units, infstr);
//			} else
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

//		String trueVariableName()
//		{
//			name = makename(var->key);
//			if (estrncmp(name, x_("ATTR_"), 5) == 0)
//				return(name + 5);
//			if (estrncmp(name, x_("ATTRP_"), 6) == 0)
//			{
//				len = estrlen(name);
//				for(i=len-1; i>=0; i--) if (name[i] == '_') break;
//				return(name + i);
//			}
//			return(name);
//		}
		public TextDescriptor getTextDescriptor() { return descriptor; }
		public void setDescriptor(TextDescriptor descriptor) { this.descriptor = descriptor; }
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

	// ------------------------ private data ------------------------------------
	/** extra variables (null if no variables yet) */		private HashMap vars;

	// ------------------------ private and protected methods -------------------

	// Create an object that represents a C-side Electric structure
	protected ElectricObject()
	{
	}

	// ------------------------ public methods -------------------

	/** Retrieve the object that is the value of a variable on this Electric object.
	 * @param name the name of the variable
	 * @return the object stored at the variable, or null if there is no such object.
	 */
	public Variable getVal(String name)
	{
		if (vars == null) return null;
		Variable val = (Variable)vars.get(name);
		return val;
	}

	/** Retrieve the object that is the value of a variable on this Electric object.
	 * @param name the name of the variable
	 * @return the object stored at the variable, or null if there is no such object.
	 */
	public Variable getVal(String name, Class type)
	{
		Variable val = getVal(name);
		if (val == null) return null;
		if (!type.isInstance(val.getObject())) return null;
		return val;
	}

	/** Retrieve an object from the array variable. <br>
	 * Get the object that is the value of a variable on this Electric
	 * object, with respect to a stack of NodeInsts.  That object must
	 * be an Object[].  Return the specified array element.
	 * @param name the name of the variable
	 * @param idx index into the array that is the value of the variable
	 * @param context the stack of NodeInsts
	 * @return the object at the index
	 * @throws RuntimeException if the variable doesn't exist, or
	 * the value of the variable isn't an object array. */
	public Object getVal(String name, int index)
	{
		Variable v = getVal(name);
		if (v == null) return null;
		Object addr = v.getObject();
		if (!(addr instanceof Object[])) return null;
		return ((Object[]) addr)[index];
	}

	/**
	 * Routine to return the number of displayable variables on this Electric object.
	 */
	public int numDisplayableVariables()
	{
		if (vars == null) return 0;

		int numVars = 0;
		for(Iterator it = vars.values().iterator(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.isDisplay()) numVars++;
		}
		return numVars;
	}
	
	/**
	 * Routine to add displayable variables on this Electric object to the polygon list in "polys",
	 * starting at entry "start".
	 */
	public void addDisplayableVariables(Rectangle2D rect, Poly [] polys, int start)
	{
		if (vars == null) return;
		
		double cX = rect.getCenterX();
		double cY = rect.getCenterY();
		for(Iterator it = vars.values().iterator(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.isDisplay()) continue;
			TextDescriptor td = var.getTextDescriptor();
			double offX = (double)td.TDGetXOff() / 4;
			double offY = (double)td.TDGetYOff() / 4;
			Point2D.Double [] pointList = new Point2D.Double[1];
			pointList[0] = new Point2D.Double(cX+offX, cY+offY);
			polys[start] = new Poly(pointList);
			polys[start].setStyle(Poly.Type.TEXTCENT);
			polys[start].setString(var.describe());
			polys[start].setLayer(null);
			start++;
		}
	}

	/** put an object value associated with a name */
	public Variable setVal(String name, Object value)
	{
		if (vars == null)
		{
			vars = new HashMap();
		}
		Variable v = new Variable(value, new TextDescriptor());
		vars.put(name, v);
		return v;
	}

	/** put an object into an array value associated with a name */
	public void setVal(String name, Object value, int index)
	{
		Variable v = getVal(name);
		if (v == null) return;
		Object addr = v.getObject();
		if (addr instanceof Object[])
		{
			((Object[]) addr)[index] = value;
		}
	}

	/** delete a variable from the vars list */
	public void delVal(String name)
	{
		if (vars == null) return;
		vars.remove(name);
	}

	public boolean isdeprecatedvariable(String name)
	{
		return false;
	}

	/** Return all the Variable names. */
	public Iterator getVariables()
	{
		if (vars == null)
			return (new ArrayList()).iterator();
		return vars.keySet().iterator();
	}

	public static void error(boolean pred, String msg)
	{
		Electric.error(pred, msg);
	}

	public static void error(String msg)
	{
		Electric.error(msg);
	}

	/** Print a full description of this object (in response to an INFO request). */
	protected void getInfo()
	{
		if (vars == null) return;
		System.out.println("Variables:");
		for(Iterator it = vars.keySet().iterator(); it.hasNext() ;)
		{
			String key = (String) it.next();
			Variable val = (Variable)vars.get(key);
			if (val == null) continue;
			Object addr = val.getObject();
			if (addr instanceof Object[])
			{
				Object[] ary = (Object[]) addr;
				System.out.print("   " + key + "= (" + ary.length + ")[");
				for (int i = 0; i < ary.length - 1; i++)
				{
					System.out.print("\"" + ary[i] + "\", ");
					if (i > 2 && ary.length != 4)
					{
						System.out.print("..., ");
						break;
					}
				}
				if (ary.length > 0)
				{
					System.out.print("\"" + ary[ary.length - 1] + "\"");
				}
				System.out.println("]");
			} else
			{
				System.out.println("   " + key + "= " + addr);
			}
		}
	}

	/** This object as a string.  Will always contain the type of the
	 * object and the c-side address as a Hexidecimal number.  May also
	 * include more interesting information.  For objects with names,
	 * use their getName method if you want their name instead. */
	public String toString()
	{
		return getClass().getName();
	}

}
