package com.sun.electric.database;

import java.util.HashMap;

/**
 * A view is simply an object with a name that represents a representation
 * style, e.g. schematic, layout, etc.
 */
public class View extends ElectricObject
{
	// -------------------------- private data -----------------------------

	/** view contains only text */							private final static int TEXTVIEW   =01;	
	/** view is one of multiple pages  */					private final static int MULTIPAGEVIEW   =02;	
	/** view is statically defined and cannot be deleted */ private final static int PERMANENTVIEW   =04;	

	/** the full name of the view */						private String fullName;
	/** the abbreviation of the view */						private String shortName;
	/** flag bits for the view */							private int type;
	/** a list of all views in existence */					private static HashMap views = new HashMap();

	// -------------------------- public data -----------------------------

	/** the unknown view */
		public static final View unknown = newInstance("unknown", "");
	/** the simulation snapshot view */
		public static final View simsnap = newInstance("simulation-snapshot", "sim");
	/** the NetLisp (netlist) view */
		public static final View netlistNetlisp = newTextInstance("netlist-netlisp-format", "net-netlisp");
	/** the RSIM (netlist) view */
		public static final View netlistRsim = newTextInstance("netlist-rsim-format", "net-rsim");
	/** the SILOS (netlist) view */
		public static final View netlistSilos = newTextInstance("netlist-silos-format", "net-silos");
	/** the QUISC (netlist) view */
		public static final View netlistQuisc = newTextInstance("netlist-quisc-format", "net-quisc");
	/** the ALS (netlist) view */
		public static final View netlistAls = newTextInstance("netlist-als-format", "net-als");
	/** the general Netlist view */
		public static final View netlist = newTextInstance("netlist", "net");
	/** the VHDL view */
		public static final View vhdl = newTextInstance("VHDL", "vhdl");
	/** the Verilog view */
		public static final View verilog = newTextInstance("Verilog", "ver");
	/** the Skeleton view */
		public static final View skeleton = newInstance("skeleton", "sk");
	/** the Compensated view */
		public static final View comp = newInstance("compensated", "comp");
	/** the Documentation view */
		public static final View doc = newTextInstance("documentation", "doc");
	/** the Icon view */
		public static final View icon = newInstance("icon", "ic");
	/** the Schematic view */
		public static final View schematic = newInstance("schematic", "sch");
	/** the Layout view */
		public static final View layout = newInstance("layout", "lay");

	// -------------------------- private methods -----------------------------

	private View(String fullName, String shortName, int type)
	{
		this.fullName = fullName;
		this.shortName = shortName;
		this.type = type;

		// enter both the full and short names into the hash table
		views.put(fullName, this);
		views.put(shortName, this);
	}

	private static View makeInstance(String fullName, String shortName, int type)
	{
		// make sure the view doesn't already exist
		if (views.get(shortName) != null)
		{
			System.out.println("multiple views with same name: " + shortName);
			return null;
		}
		if (views.get(fullName) != null)
		{
			System.out.println("multiple views with same name: " + fullName);
			return null;
		}

		// create the view
		View v = new View(fullName, shortName, type);
		return v;
	}

	// -------------------------- public methods -----------------------------

	public static View newInstance(String fullName, String shortName)
	{
		return makeInstance(fullName, shortName, 0);
	}

	public static View newTextInstance(String fullName, String shortName)
	{
		return makeInstance(fullName, shortName, TEXTVIEW);
	}

	/**
	 * Retrieve a view using its full or short name.  Return null if
	 * no such view.
	 */
	public static View getView(String name)
	{
		return (View) views.get(name);
	}

	/**
	 * get the full name of this view.  This is a complete word, like
	 * schematic
	 */
	public String getFullName() { return fullName; }

	/**
	 * get the short name of this view.  This is the short sequence of
	 * characters you usually see inside the {} in Facet descriptions,
	 * e.g. sch
	 */
	public String getShortName() { return shortName; }

	/** Get the Text-view bit */
	public boolean isTextView() { return (type & TEXTVIEW) != 0; }

	/** Get the Multipage-view bit */
	public boolean isMultiPageView() { return (type & MULTIPAGEVIEW) != 0; }

	/** Get the ermanent-view bit */
	public boolean isPermanentView() { return (type & PERMANENTVIEW) != 0; }

	public String toString()
	{
		return "View " + fullName;
	}

} // end of class View
