package com.sun.electric.database;

import java.util.HashMap;

/**
 * A view is simply an object with a name that represents a representation
 * style, e.g. schematic, layout, etc.
 */
public class View extends ElectricObject
{
	private String fullName;
	private String shortName;
	private int type;
	private static HashMap views = new HashMap();

	/** the unknown view */									public static View el_unknownview;
	/** the simulation snapshot view */						public static View el_simsnapview;
	/** the NetLisp (netlist) view */						public static View el_netlistnetlispview;
	/** the RSIM (netlist) view */							public static View el_netlistrsimview;
	/** the SILOS (netlist) view */							public static View el_netlistsilosview;
	/** the QUISC (netlist) view */							public static View el_netlistquiscview;
	/** the ALS (netlist) view */							public static View el_netlistalsview;
	/** the general Netlist view */							public static View el_netlistview;
	/** the VHDL view */									public static View el_vhdlview;
	/** the Verilog view */									public static View el_verilogview;
	/** the Skeleton view */								public static View el_skeletonview;
	/** the Compensated view */								public static View el_compview;
	/** the Documentation view */							public static View el_docview;
	/** the Icon view */									public static View el_iconview;
	/** the Schematic view */								public static View el_schematicview;
	/** the Layout view */									public static View el_layoutview;

	/** view contains only text */							public final static int TEXTVIEW   =01;	
	/** view is one of multiple pages  */					public final static int MULTIPAGEVIEW   =02;	
	/** view is statically defined and cannot be deleted */ public final static int PERMANENTVIEW   =04;	

	static void buildViews()
	{
		el_unknownview = newView("unknown", "", 0);
		el_simsnapview = newView("simulation-snapshot", "sim", 0);
		el_netlistnetlispview = newView("netlist-netlisp-format", "net-netlisp", TEXTVIEW);
		el_netlistrsimview = newView("netlist-rsim-format", "net-rsim", TEXTVIEW);
		el_netlistsilosview = newView("netlist-silos-format", "net-silos", TEXTVIEW);
		el_netlistquiscview = newView("netlist-quisc-format", "net-quisc", TEXTVIEW);
		el_netlistalsview = newView("netlist-als-format", "net-als", TEXTVIEW);
		el_netlistview = newView("netlist", "net", TEXTVIEW);
		el_vhdlview = newView("VHDL", "vhdl", TEXTVIEW);
		el_verilogview = newView("Verilog", "ver", TEXTVIEW);
		el_skeletonview = newView("skeleton", "sk", 0);
		el_compview = newView("compensated", "comp", 0);
		el_docview = newView("documentation", "doc", TEXTVIEW);
		el_iconview = newView("icon", "ic", 0);
		el_schematicview = newView("schematic", "sch", 0);
		el_layoutview = newView("layout", "lay", 0);
	}

	private View(String fullName, String shortName, int type)
	{
		this.fullName = fullName;
		this.shortName = shortName;
		this.type = type;

		// enter both the full and short names into the hash table
		views.put(fullName, this);
		views.put(shortName, this);
	}

	public static View newView(String fullName, String shortName, int type)
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
	public String getFullName()
	{
		return fullName;
	}

	/**
	 * get the short name of this view.  This is the short sequence of
	 * characters you usually see inside the {} in Facet descriptions,
	 * e.g. sch
	 */
	public String getShortName()
	{
		return shortName;
	}

	public String toString()
	{
		return "View " + fullName + " (" + shortName + ")";
	}

} // end of class View
