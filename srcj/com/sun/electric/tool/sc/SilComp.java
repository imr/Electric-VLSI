/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SilComp.java
 * Silicon compiler tool (QUISC): control
 * Written by Andrew R. Kostiuk, Queen's University.
 * Translated to Java by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.sc;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Listener;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * This is the Silicon Compiler tool.
 */
public class SilComp extends Listener
{
	/***********************************************************************
		General Constants
	------------------------------------------------------------------------*/
	private static final int GND = 0;
	private static final int PWR = 1;

	/***** Directions that ports can be attached to *****/
	/** mask for port direction */		public static final int SCPORTDIRMASK	= 0x0000000F;
	/** port direction up */			public static final int SCPORTDIRUP		= 0x00000001;
	/** port direction down */			public static final int SCPORTDIRDOWN	= 0x00000002;
	/** port direction right */			public static final int SCPORTDIRRIGHT	= 0x00000004;
	/** port direction left */			public static final int SCPORTDIRLEFT	= 0x00000008;
	/** port type mask */				public static final int SCPORTTYPE		= 0x000003F0;
	/** ground port */					public static final int SCGNDPORT		= 0x00000010;
	/** power port */					public static final int SCPWRPORT		= 0x00000020;
	/** bidirectional port */			public static final int SCBIDIRPORT		= 0x00000040;
	/** output port */					public static final int SCOUTPORT		= 0x00000080;
	/** input port */					public static final int SCINPORT		= 0x00000100;
	/** unknown port */					public static final int SCUNPORT		= 0x00000200;

	/***********************************************************************
		QUISC Cell Structure
	------------------------------------------------------------------------*/

	public static class SCCELL
	{
		/** name of complex cell */				String    		name;
		/** maximum number of nodes */			int       		max_node_num;
		/** list of instances for cell */		List      		nilist;
		/** extracted nodes */					SCEXTNODE 		ex_nodes;
		/** flags for processing cell */		int       		bits;
		/** list of power ports */				SCEXTNODE 		power;
		/** list of ground ports */				SCEXTNODE 		ground;
		/** list of ports */					SCPORT    		ports, lastport;
		/** placement information of cell */	Place.SCPLACE   placement;
		/** routing information for cell */		Route.SCROUTE   route;
		/** next in list of SC cells */			SCCELL    		next;
	};

	public static class SCPORT
	{
		/** name of port */						String   name;
		/** special node */						SCNITREE node;
		/** cell on which this port resides */	SCCELL   parent;
		/** port attributes */					int      bits;
		/** pointer to next port */				SCPORT   next;
	};

	public static class SCCELLNUMS
	{
		/** active area from top */				int		top_active;
		/** active are from bottom */			int		bottom_active;
		/** active area from left */			int		left_active;
		/** active are from right */			int		right_active;
	};

	/***********************************************************************
		Instance Tree Structure
	------------------------------------------------------------------------*/

	/***** Types of Instances *****/
	public static final int SCLEAFCELL     = 0;
	public static final int SCCOMPLEXCELL  = 1;
	public static final int SCSPECIALCELL  = 2;
	public static final int SCFEEDCELL     = 3;
	public static final int SCSTITCH       = 4;
	public static final int SCLATERALFEED  = 5;

	public static class SCNITREE
	{
		/** name of instance */					String		name;
		/** alternative number of node */		int			number;
		/** type of instance */					int			type;
		/** leaf cell or SCCELL if complex */	Object		np;
		/** x size if leaf cell */				double		size;
		/** pointer to connection list */		SCCONLIST	connect;
		/** list of io ports and ext nodes */	SCNIPORT	ports;
		/** list of actual power ports */		SCNIPORT	power;
		/** list of actual ground ports */		SCNIPORT	ground;
		/** bits for silicon compiler */		int			flags;
		/** generic temporary pointer */		Object		tp;
	};

	public static class SCNIPORT
	{
		/** leaf port or SCPORT if on cell */	Object		port;
		/** extracted node */					SCEXTNODE	ext_node;
		/** bits for processing */				int			bits;
		/** x position if leaf port */			double		xpos;
		/** list of instance ports */			SCNIPORT	next;
	};

	/***********************************************************************
		Connection Structures
	------------------------------------------------------------------------*/

	private static class SCCONLIST
	{
		/** pointer to port on node A */		SCNIPORT	portA;
		/** pointer to node B */				SCNITREE	nodeB;
		/** pointer to port on node B */		SCNIPORT	portB;
		/** pointer to extracted node */		SCEXTNODE	ext_node;
		/** pointer to next list element */		SCCONLIST   next;
	};

	/***********************************************************************
		Extraction Structures
	------------------------------------------------------------------------*/

	private static class SCEXTPORT
	{
		/** instance of extracted node */		SCNITREE	node;
		/** instance port */					SCNIPORT	port;
		/** next in list of common node */		SCEXTPORT	next;
	};

	public static class SCEXTNODE
	{
		/** optional name of port */			String		name;
		/** link list of ports */				SCEXTPORT	firstport;
		/** flags for processing */				int			flags;
		/** generic pointer for processing */	Object		ptr;
		/** link list of nodes */				SCEXTNODE	next;
	};

	static SCCELL	sc_cells, sc_curcell;
	static Library  sc_celllibrary = null;

	/** the Silicon Compiler tool. */		public static SilComp tool = new SilComp();

	/****************************** TOOL INTERFACE ******************************/

	/**
	 * The constructor sets up the Silicon Compiler tool.
	 */
	private SilComp()
	{
		super("sc");
	}

	/**
	 * Method to initialize the Silicon Compiler tool.
	 */
	public void init()
	{
		sc_cells = null;
		sc_curcell = null;
	}

	/**
	 * Method to return the cell library that will be used for silicon compilation.
	 * @return the cell library that will be used for silicon compilation (null if none).
	 */
	public static Library getCellLib() { return sc_celllibrary; }

	/**
	 * Method to set the cell library that will be used for silicon compilation.
	 * @param lib the cell library that will be used for silicon compilation).
	 */
	public static void setCellLib(Library lib) { sc_celllibrary = lib; }

	/****************************** OPTIONS ******************************/

	private static Pref cacheNumberOfRows = Pref.makeIntPref("NumberOfRows", SilComp.tool.prefs, 4);
	/**
	 * Method to return the number of rows of cells to make.
	 * The default is 4.
	 * @return the number of rows of cells to make.
	 */
	public static int getNumberOfRows() { return cacheNumberOfRows.getInt(); }
	/**
	 * Method to set the number of rows of cells to make.
	 * @param rows the new number of rows of cells to make.
	 */
	public static void setNumberOfRows(int rows) { cacheNumberOfRows.setInt(rows); }


	private static Pref cacheHorizRoutingArc = Pref.makeStringPref("HorizRoutingArc", SilComp.tool.prefs, "Metal-1");
	/**
	 * Method to return the horizontal routing arc.
	 * The default is "Metal-1".
	 * @return the name of the horizontal routing arc.
	 */
	public static String getHorizRoutingArc() { return cacheHorizRoutingArc.getString(); }
	/**
	 * Method to set the horizontal routing arc.
	 * @param arcName name of new horizontal routing arc.
	 */
	public static void setHorizRoutingArc(String arcName) { cacheHorizRoutingArc.setString(arcName); }

	private static Pref cacheHorizRoutingWidth = Pref.makeDoublePref("HorizArcWidth", SilComp.tool.prefs, 4);
	/**
	 * Method to return the width of the horizontal routing arc.
	 * The default is 4.
	 * @return the width of the horizontal routing arc.
	 */
	public static double getHorizArcWidth() { return cacheHorizRoutingWidth.getDouble(); }
	/**
	 * Method to set the width of the horizontal routing arc.
	 * @param wid the new width of the horizontal routing arc.
	 */
	public static void setHorizArcWidth(double wid) { cacheHorizRoutingWidth.setDouble(wid); }


	private static Pref cacheVertRoutingArc = Pref.makeStringPref("VertRoutingArc", SilComp.tool.prefs, "Metal-2");
	/**
	 * Method to return the vertical routing arc.
	 * The default is "Metal-2".
	 * @return the name of the vertical routing arc.
	 */
	public static String getVertRoutingArc() { return cacheVertRoutingArc.getString(); }
	/**
	 * Method to set the vertical routing arc.
	 * @param arcName name of new vertical routing arc.
	 */
	public static void setVertRoutingArc(String arcName) { cacheVertRoutingArc.setString(arcName); }

	private static Pref cacheVertRoutingWidth = Pref.makeDoublePref("VertArcWidth", SilComp.tool.prefs, 4);
	/**
	 * Method to return the width of the vertical routing arc.
	 * The default is 4.
	 * @return the width of the vertical routing arc.
	 */
	public static double getVertArcWidth() { return cacheVertRoutingWidth.getDouble(); }
	/**
	 * Method to set the width of the vertical routing arc.
	 * @param wid the new width of the vertical routing arc.
	 */
	public static void setVertArcWidth(double wid) { cacheVertRoutingWidth.setDouble(wid); }


	private static Pref cachePowerWireWidth = Pref.makeDoublePref("PowerWireWidth", SilComp.tool.prefs, 5);
	/**
	 * Method to return the width of the power and ground arc.
	 * The default is 5.
	 * @return the width of the power and ground arc.
	 */
	public static double getPowerWireWidth() { return cachePowerWireWidth.getDouble(); }
	/**
	 * Method to set the width of the power and ground arc.
	 * @param wid the new width of the power and ground arc.
	 */
	public static void setPowerWireWidth(double wid) { cachePowerWireWidth.setDouble(wid); }

	private static Pref cacheMainPowerWireWidth = Pref.makeDoublePref("MainPowerWireWidth", SilComp.tool.prefs, 8);
	/**
	 * Method to return the width of the main power and ground arc.
	 * The default is 8.
	 * @return the width of the main power and ground arc.
	 */
	public static double getMainPowerWireWidth() { return cacheMainPowerWireWidth.getDouble(); }
	/**
	 * Method to set the width of the main power and ground arc.
	 * @param wid the new width of the main power and ground arc.
	 */
	public static void setMainPowerWireWidth(double wid) { cacheMainPowerWireWidth.setDouble(wid); }

	private static Pref cacheMainPowerArc = Pref.makeStringPref("MainPowerArc", SilComp.tool.prefs, "Horizontal Arc");
	/**
	 * Method to return the main power and ground arc.
	 * The default is "Horizontal Arc".
	 * @return the name of the main power and ground arc.
	 */
	public static String getMainPowerArc() { return cacheMainPowerArc.getString(); }
	/**
	 * Method to set the main power and ground arc.
	 * @param arcName name of new main power and ground arc.
	 */
	public static void setMainPowerArc(String arcName) { cacheMainPowerArc.setString(arcName); }


	private static Pref cachePWellHeight = Pref.makeDoublePref("PWellHeight", SilComp.tool.prefs, 41);
	/**
	 * Method to return the height of the p-well.
	 * The default is 41.
	 * @return the height of the p-well.
	 */
	public static double getPWellHeight() { return cachePWellHeight.getDouble(); }
	/**
	 * Method to set the height of the p-well.
	 * @param hei the new height of the p-well.
	 */
	public static void setPWellHeight(double hei) { cachePWellHeight.setDouble(hei); }

	private static Pref cachePWellOffset = Pref.makeDoublePref("PWellOffset", SilComp.tool.prefs, 0);
	/**
	 * Method to return the offset of the p-well.
	 * The default is 0.
	 * @return the offset of the p-well.
	 */
	public static double getPWellOffset() { return cachePWellOffset.getDouble(); }
	/**
	 * Method to set the offset of the p-well.
	 * @param off the new offset of the p-well.
	 */
	public static void setPWellOffset(double off) { cachePWellOffset.setDouble(off); }

	private static Pref cacheNWellHeight = Pref.makeDoublePref("NWellHeight", SilComp.tool.prefs, 51);
	/**
	 * Method to return the height of the n-well.
	 * The default is 51.
	 * @return the height of the n-well.
	 */
	public static double getNWellHeight() { return cacheNWellHeight.getDouble(); }
	/**
	 * Method to set the height of the n-well.
	 * @param hei the new height of the n-well.
	 */
	public static void setNWellHeight(double hei) { cacheNWellHeight.setDouble(hei); }

	private static Pref cacheNWellOffset = Pref.makeDoublePref("NWellOffset", SilComp.tool.prefs, 0);
	/**
	 * Method to return the offset of the n-well.
	 * The default is 0.
	 * @return the offset of the n-well.
	 */
	public static double getNWellOffset() { return cacheNWellOffset.getDouble(); }
	/**
	 * Method to set the offset of the n-well.
	 * @param off the new offset of the n-well.
	 */
	public static void setNWellOffset(double off) { cacheNWellOffset.setDouble(off); }


	private static Pref cacheViaSize = Pref.makeDoublePref("ViaSize", SilComp.tool.prefs, 4);
	/**
	 * Method to return the size of vias.
	 * The default is 4.
	 * @return the size of vias.
	 */
	public static double getViaSize() { return cacheViaSize.getDouble(); }
	/**
	 * Method to set the size of vias.
	 * @param off the new size of vias.
	 */
	public static void setViaSize(double off) { cacheViaSize.setDouble(off); }

	private static Pref cacheMinMetalSpacing = Pref.makeDoublePref("MinMetalSpacing", SilComp.tool.prefs, 6);
	/**
	 * Method to return the minimum metal spacing.
	 * The default is 6.
	 * @return the minimum metal spacing.
	 */
	public static double getMinMetalSpacing() { return cacheMinMetalSpacing.getDouble(); }
	/**
	 * Method to set the minimum metal spacing.
	 * @param off the new minimum metal spacing.
	 */
	public static void setMinMetalSpacing(double off) { cacheMinMetalSpacing.setDouble(off); }

	private static Pref cacheFeedThruSize = Pref.makeDoublePref("FeedThruSize", SilComp.tool.prefs, 16);
	/**
	 * Method to return the size of feed-throughs.
	 * The default is 16.
	 * @return the size of feed-throughs.
	 */
	public static double getFeedThruSize() { return cacheFeedThruSize.getDouble(); }
	/**
	 * Method to set the size of feed-throughs.
	 * @param off the new size of feed-throughs.
	 */
	public static void setFeedThruSize(double off) { cacheFeedThruSize.setDouble(off); }

	private static Pref cacheMinPortDistance = Pref.makeDoublePref("MinPortDistance", SilComp.tool.prefs, 8);
	/**
	 * Method to return the minimum port distance.
	 * The default is 8.
	 * @return the minimum port distance.
	 */
	public static double getMinPortDistance() { return cacheMinPortDistance.getDouble(); }
	/**
	 * Method to set the minimum port distance.
	 * @param off the new minimum port distance.
	 */
	public static void setMinPortDistance(double off) { cacheMinPortDistance.setDouble(off); }

	private static Pref cacheMinActiveDistance = Pref.makeDoublePref("MinActiveDistance", SilComp.tool.prefs, 8);
	/**
	 * Method to return the minimum active distance.
	 * The default is 8.
	 * @return the minimum active distance.
	 */
	public static double getMinActiveDistance() { return cacheMinActiveDistance.getDouble(); }
	/**
	 * Method to set the minimum active distance.
	 * @param off the new minimum active distance.
	 */
	public static void setMinActiveDistance(double off) { cacheMinActiveDistance.setDouble(off); }

	/**************************************** READ NETLIST IN CURRENT CELL ****************************************/

	/**
	 * Read the netlist associated with the current cell.
	 * @return true on error.
	 */
	public static boolean readNetCurCell(Cell cell)
	{
		if (cell.getView() != View.NETLISTQUISC)
		{
			System.out.println("Current cell must have QUISC Netlist view");
			return true;
		}
		String [] strings = cell.getTextViewContents();
		if (strings == null)
		{
			System.out.println("Cell " + cell.describe() + " has no text in it");
			return true;
		}

		// read entire netlist
		boolean errors = false;
		for(int i=0; i<strings.length; i++)
		{
			String inbuf = strings[i].trim();

			// check for a comment line or empty line
			if (inbuf.length() == 0 || inbuf.charAt(0) == '!') continue;

			// break into keywords
			List parameters = new ArrayList();
			for(int sptr = 0; sptr < inbuf.length(); sptr++)
			{
				// get rid of leading white space
				while (sptr < inbuf.length() && (inbuf.charAt(sptr) == ' ' || inbuf.charAt(sptr) == '\t')) sptr++;
				if (sptr >= inbuf.length()) break;

				// check for string
				if (inbuf.charAt(sptr) == '"')
				{
					sptr++;
					int endQuote = inbuf.indexOf('"', sptr);
					if (endQuote < 0)
					{
						System.out.println("ERROR line " + (i+1) + ": Unbalanced quotes ");
						errors = true;
						break;
					}
					parameters.add(inbuf.substring(sptr, endQuote));
					sptr = endQuote;
				} else
				{
					int endSpace = inbuf.indexOf(' ', sptr);
					int endTab = inbuf.indexOf('\t', sptr);
					if (endSpace < 0) endSpace = endTab;
					if (endSpace < 0) endSpace = inbuf.length();
					if (endTab >= 0 && endTab < endSpace) endSpace = endTab;
					parameters.add(inbuf.substring(sptr, endSpace));
					sptr = endSpace;
				}
			}
			String err = Sc_parse(parameters);
			if (err != null)
			{
				System.out.println("ERROR line " + (i+1) + ": " + err);
				System.out.println("      Line: " + inbuf);
				errors = true;
				break;
			}
		}

		// do the "pull" (extract)
		String err = Sc_pull();
		if (err != null)
		{
			System.out.println(err);
			errors = true;
		}
		return errors;
	}

	/**
	 * Main parsing routine for the Silicon Compiler.
	 */
	private static String Sc_parse(List keywords)
	{
		if (keywords.size() == 0) return null;
		String mainKeyword = (String)keywords.get(0);

		if (mainKeyword.equalsIgnoreCase("connect")) return Sc_connect(keywords);
		if (mainKeyword.equalsIgnoreCase("create")) return Sc_create(keywords);
		if (mainKeyword.equalsIgnoreCase("export")) return Sc_export(keywords);
		if (mainKeyword.equalsIgnoreCase("extract")) return Sc_extract(keywords);
		if (mainKeyword.equalsIgnoreCase("set")) return Sc_xset(keywords);
		return "Unknown keyword: " + mainKeyword;
	}

	/**************************************** NETLIST READING: THE CREATE KEYWORD ****************************************/

	private static String Sc_create(List keywords)
	{
		if (keywords.size() <= 1) return "No keyword for CREATE command";
		String sptr = (String)keywords.get(1);
		if (sptr.equalsIgnoreCase("cell"))
		{
			if (keywords.size() <= 2) return "No name for CREATE CELL command";
			sptr = (String)keywords.get(2);

			// check if cell already exists in cell list
			for(SCCELL cell = sc_cells; cell != null; cell = cell.next)
			{
				if (sptr.equalsIgnoreCase(cell.name))
					return "Cell '" + sptr + "' already exists in current library";
			}

			// generate warning message if a leaf cell of the same name exists
			if (Sc_find_leaf_cell(sptr) != null)
				System.out.println("WARNING - cell " + sptr + " may be overridden by created cell");

			// create new cell
			SCCELL newcell = new SCCELL();
			newcell.name = sptr;
			newcell.max_node_num = 0;
			newcell.nilist = new ArrayList();
			newcell.ex_nodes = null;
			newcell.bits = 0;
			newcell.power = null;
			newcell.ground = null;
			newcell.ports = null;
			newcell.lastport = null;
			newcell.placement = null;
			newcell.route = null;
			newcell.next = sc_cells;
			sc_cells = newcell;
			sc_curcell = newcell;

			// create dummy ground and power nodes
			SCNITREE ntp = Sc_findni(sc_curcell, "ground");
			if (ntp != null) return "Instance 'ground' already exists";
			ntp = Sc_new_instance("ground", SCSPECIALCELL);
			sc_curcell.nilist.add(ntp);
			Sc_new_instance_port(ntp);
			ntp.number = sc_curcell.max_node_num++;

			ntp = Sc_findni(sc_curcell, "power");
			if (ntp != null) return "Instance 'power' already exists";
			ntp = Sc_new_instance("power", SCSPECIALCELL);
			sc_curcell.nilist.add(ntp);
			Sc_new_instance_port(ntp);
			ntp.number = sc_curcell.max_node_num++;
			return null;
		}

		if (sptr.equalsIgnoreCase("instance"))
		{
			if (keywords.size() <= 2) return "No instance name for CREATE INSTANCE command";
			String noden = (String)keywords.get(2);

			if (keywords.size() <= 3) return "No type name for CREATE INSTANCE command";
			String nodep = (String)keywords.get(3);

			// search for cell in cell list
			SCCELL cell = null;
			for(SCCELL c = sc_cells; c != null; c = c.next)
			{
				if (nodep.equalsIgnoreCase(c.name)) { cell = c;   break; }
			}
			Object proto = cell;
			int type = SCCOMPLEXCELL;
			double size = 0;
			if (cell == null)
			{
				// search for leaf cell in library
				Cell bc = Sc_find_leaf_cell(nodep);
				if (bc == null)
					return "There is no '" + nodep + "' in the standard cell library";
				proto = bc;
				type = SCLEAFCELL;
				size = Sc_leaf_cell_xsize(bc);
			}

			// check if currently working in a cell
			if (sc_curcell == null) return "No cell selected";

			// check if instance name already exits
			SCNITREE ntp = Sc_findni(sc_curcell, noden);
			if (ntp != null)
				return "Instance '" + noden + "' already exists";

			// add instance name to tree
			ntp = Sc_new_instance(noden, type);
			sc_curcell.nilist.add(ntp);
			ntp.number = sc_curcell.max_node_num++;
			ntp.np = proto;
			ntp.size = size;

			// create ni port list
			if (type == SCCOMPLEXCELL)
			{
				SCNIPORT oldniport = null;
				for (SCPORT port = ((SCCELL)proto).ports; port != null; port = port.next)
				{
					SCNIPORT niport = new SCNIPORT();
					niport.port = port;
					niport.ext_node = null;
					niport.bits = 0;
					niport.xpos = 0;
					switch (port.bits & SCPORTTYPE)
					{
						case SCGNDPORT:
							niport.next = ntp.ground;
							ntp.ground = niport;
							break;
						case SCPWRPORT:
							niport.next = ntp.power;
							ntp.power = niport;
							break;
						default:
							niport.next = null;
							if (oldniport == null)
							{
								ntp.ports = niport;
							} else
							{
								oldniport.next = niport;
							}
							oldniport = niport;
							break;
					}
				}
			} else
			{
				SCNIPORT oldniport = null;
				Cell realCell = (Cell)proto;
				for(Iterator it = realCell.getPorts(); it.hasNext(); )
				{
					Export bp = (Export)it.next();
					SCNIPORT niport = new SCNIPORT();
					niport.port = bp;
					niport.ext_node = null;
					niport.bits = 0;
					niport.xpos = Sc_leaf_port_xpos(bp);
					switch (Sc_leaf_port_type(bp))
					{
						case SCGNDPORT:
							niport.next = ntp.ground;
							ntp.ground = niport;
							break;
						case SCPWRPORT:
							niport.next = ntp.power;
							ntp.power = niport;
							break;
						default:
							niport.next = null;
							if (oldniport == null)
							{
								ntp.ports = niport;
							} else
							{
								oldniport.next = niport;
							}
							oldniport = niport;
							break;
					}
				}
			}
			return null;
		}
		return "Unknown CREATE command: " + sptr;
	}

	/**
	 * Method to find the location in a SCNITREE where the node should be placed.
	 * If the pointer does not have a value of null, then the object already exits.
	 */
	private static SCNITREE Sc_findni(SCCELL cell, String name)
	{
		for(Iterator it = cell.nilist.iterator(); it.hasNext(); )
		{
			SCNITREE nptr = (SCNITREE)it.next();
			if (nptr.name.equalsIgnoreCase(name)) return nptr;
		}
		return null;
	}

	/**
	 * Method to create a new instance with a given name and type.
	 * @param name name of the instance.
	 * @param type type of the instance.
	 * @return new instance.
	 */
	static SCNITREE Sc_new_instance(String name, int type)
	{
		SCNITREE ninst = new SCNITREE();
		ninst.name = name;
		ninst.type = type;
		ninst.number = 0;
		ninst.np = null;
		ninst.size = 0;
		ninst.connect = null;
		ninst.ports = null;
		ninst.power = null;
		ninst.ground = null;
		ninst.flags = 0;
		ninst.tp = null;
		return ninst;
	}

	/**
	 * Method to create a new instance port and add it to a given instance.
	 * @param intance the instance to which to add a port.
	 * @return the new instance port.
	 */
	static SCNIPORT Sc_new_instance_port(SCNITREE instance)
	{
		SCNIPORT nport = new SCNIPORT();
		nport.port = null;
		nport.ext_node = null;
		nport.bits = 0;
		nport.xpos = 0;
		nport.next = instance.ports;
		instance.ports = nport;
		return nport;
	}

	/**************************************** NETLIST READING: THE CONNECT KEYWORD ****************************************/

	private static String Sc_connect(List keywords)
	{
		if (keywords.size() < 4) return "Not enough parameters for CONNECT command";

		// search for the first node
		String node0Name = (String)keywords.get(1);
		String port0Name = (String)keywords.get(2);
		SCNITREE ntpA = Sc_findni(sc_curcell, node0Name);
		if (ntpA == null) return "Cannot find instance '" + node0Name + "'";
		SCNIPORT portA = Sc_findpp(ntpA, port0Name);
		if (portA == null)
			return "Cannot find port '" + port0Name + "' on instance '" + node0Name + "'";

		// search for the second node
		String node1Name = (String)keywords.get(3);
		SCNITREE ntpB = Sc_findni(sc_curcell, node1Name);
		if (ntpB == null) return "Cannot find instance '" + node1Name + "'";

		// check for special power or ground node
		SCNIPORT portB = ntpB.ports;
		if (ntpB.type != SCSPECIALCELL)
		{
			if (keywords.size() < 5) return "Not enough parameters for CONNECT command";
			String port1Name = (String)keywords.get(4);
			portB = Sc_findpp(ntpB, port1Name);
			if (portB == null)
				return "Cannot find port '" + port1Name + "' on instance '" + node1Name + "'";
		}
		Sc_conlist(ntpA, portA, ntpB, portB);
		return null;
	}

	/**
	 * Method to find the port on the given node instance.
	 * @return null if port is not found.
	 */
	private static SCNIPORT Sc_findpp(SCNITREE ntp, String name)
	{
		SCNIPORT port = null;

		if (ntp == null) return null;
		switch (ntp.type)
		{
			case SCSPECIALCELL:
				return ntp.ports;
			case SCCOMPLEXCELL:
				for (port = ntp.ports; port != null; port = port.next)
				{
					if (((SCPORT)port.port).name.equalsIgnoreCase(name)) break;
				}
				break;
			case SCLEAFCELL:
				for (port = ntp.ports; port != null; port = port.next)
				{
					Export pp = (Export)port.port;
					if (pp.getName().equalsIgnoreCase(name)) break;
				}
				break;
		}
		return port;
	}

	/**
	 * Method to add a connection count for the two node instances indicated.
	 */
	private static void Sc_conlist(SCNITREE ntpA, SCNIPORT portA, SCNITREE ntpB, SCNIPORT portB)
	{
		// add connection to instance A
		SCCONLIST cl = new SCCONLIST();
		cl.portA = portA;
		cl.nodeB = ntpB;
		cl.portB = portB;
		cl.ext_node = null;

		// add to head of the list
		cl.next = ntpA.connect;
		ntpA.connect = cl;

		// add connection to instance B
		cl = new SCCONLIST();
		cl.portA = portB;
		cl.nodeB = ntpA;
		cl.portB = portA;
		cl.ext_node = null;

		// add to head of the list
		cl.next = ntpB.connect;
		ntpB.connect = cl;
	}

	/**************************************** NETLIST READING: THE EXPORT KEYWORD ****************************************/

	private static String Sc_export(List keywords)
	{
		// check to see if working in a cell
		if (sc_curcell == null) return "No cell selected";

		// search current cell for node
		if (keywords.size() <= 1) return "No instance specified for EXPORT command";
		String instName = (String)keywords.get(1);
		SCNITREE nptr = Sc_findni(sc_curcell, instName);
		if (nptr == null) return "Cannot find instance '" + instName + "' for EXPORT command";

		// search for port
		if (keywords.size() <= 2) return "No port specified for EXPORT command";
		String portName = (String)keywords.get(2);
		SCNIPORT port = Sc_findpp(nptr, portName);
		if (port == null)
			return "Cannot find port '" + portName + "' on instance '" + instName + "' for EXPORT command";

		// check for export name
		if (keywords.size() <= 3) return "No export name specified for EXPORT command";
		String exportName = (String)keywords.get(3);

		// check possible port type
		int type = SCUNPORT;
		if (keywords.size() >= 5)
		{
			String typeName = (String)keywords.get(4);
			if (typeName.equalsIgnoreCase("input"))
			{
				type = SCINPORT;
			} else if (typeName.equalsIgnoreCase("output"))
			{
				type = SCOUTPORT;
			} else if (typeName.equalsIgnoreCase("bidirectional"))
			{
				type = SCBIDIRPORT;
			} else
			{
				return "Unknown port type '" + typeName + "' for EXPORT command";
			}
		}

		// create special node
		SCNITREE searchnptr = Sc_findni(sc_curcell, exportName);
		if (searchnptr != null) return "Export name '" + exportName + "' is not unique";
		SCNITREE newnptr = Sc_new_instance(exportName, SCSPECIALCELL);
		sc_curcell.nilist.add(newnptr);
		newnptr.number = sc_curcell.max_node_num++;
		searchnptr = newnptr;
		SCNIPORT niport = new SCNIPORT();
		niport.port = null;
		niport.ext_node = null;
		niport.next = null;
		newnptr.ports = niport;

		// add to export port list
		SCPORT newport = new SCPORT();
		niport.port = newport;
		newport.name = exportName;
		newport.node = newnptr;
		newport.parent = sc_curcell;
		newport.bits = type;
		newport.next = null;
		if (sc_curcell.lastport == null)
		{
			sc_curcell.ports = sc_curcell.lastport = newport;
		} else
		{
			sc_curcell.lastport.next = newport;
			sc_curcell.lastport = newport;
		}

		// add to connect list
		Sc_conlist(nptr, port, newnptr, niport);
		return null;
	}

	/**************************************** NETLIST READING: THE SET KEYWORD ****************************************/

	/**
	 * Method to handle the "SET" keyword.
	 * Current options are:
	 * 1.  leaf-cell-numbers = Magic numbers for leaf cells.
	 * 2.  node-name	     = Name an extracted node.
	 * 3.  port-direction    = Direction allowed to attach to a port.
	 */
	private static String Sc_xset(List keywords)
	{
		if (keywords.size() <= 1) return "No option for SET command";
		String whatToSet = (String)keywords.get(1);

		if (whatToSet.equalsIgnoreCase("leaf-cell-numbers"))
		{
			String cellName = (String)keywords.get(2);
			Cell leafcell = Sc_find_leaf_cell(cellName);
			if (leafcell == null) return "Cannot find cell '" + cellName + "'";
			SCCELLNUMS cNums = Sc_leaf_cell_get_nums(leafcell);
			int numpar = 3;
			while (numpar < keywords.size())
			{
				String parName = (String)keywords.get(numpar);
				if (parName.equalsIgnoreCase("top-active"))
				{
					numpar++;
					if (numpar < keywords.size())
						cNums.top_active = TextUtils.atoi((String)keywords.get(numpar++));
					continue;
				}
				if (parName.equalsIgnoreCase("bottom-active"))
				{
					numpar++;
					if (numpar < keywords.size())
						cNums.bottom_active = TextUtils.atoi((String)keywords.get(numpar++));
					continue;
				}
				if (parName.equalsIgnoreCase("left-active"))
				{
					numpar++;
					if (numpar < keywords.size())
						cNums.left_active = TextUtils.atoi((String)keywords.get(numpar++));
					continue;
				}
				if (parName.equalsIgnoreCase("right-active"))
				{
					numpar++;
					if (numpar < keywords.size())
						cNums.right_active = TextUtils.atoi((String)keywords.get(numpar++));
					continue;
				}
				return "Unknown option '" + parName + "' for SET LEAF-CELL-NUMBERS command";
			}
			Sc_leaf_cell_set_nums(leafcell, cNums);
			return null;
		}

		if (whatToSet.equalsIgnoreCase("node-name"))
		{
			// check for sufficient parameters
			if (keywords.size() <= 4) return "Insufficent parameters for SET NODE-NAME command";

			// search for instance
			String instName = (String)keywords.get(2);
			SCNITREE instptr = Sc_findni(sc_curcell, instName);
			if (instptr == null)
				return "Cannot find instance '" + instName + "' in SET NODE-NAME command";

			// search for port on instance
			String portName = (String)keywords.get(3);
			SCNIPORT iport;
			for (iport = instptr.ports; iport != null; iport = iport.next)
			{
				if (instptr.type == SCLEAFCELL)
				{
					Export e = (Export)iport.port;
					if (e.getName().equalsIgnoreCase(portName)) break;
				} else if (instptr.type == SCCOMPLEXCELL)
				{
					SCPORT scp = (SCPORT)iport.port;
					if (scp.name.equalsIgnoreCase(portName)) break;
				}
			}
			if (iport == null)
				return "Cannot find port '" + portName + "' on instance '" + instName + "' in SET NODE-NAME command";

			// set extracted node name if possible
			if (iport.ext_node == null) return "Cannot find extracted node to set name in SET NODE-NAME command";
			iport.ext_node.name = (String)keywords.get(4);
			return null;
		}

		if (whatToSet.equalsIgnoreCase("port-direction"))
		{
			String cellName = (String)keywords.get(2);
			String portName = (String)keywords.get(3);
			SCCELL cell;
			for (cell = sc_cells; cell != null; cell = cell.next)
			{
				if (cell.name.equalsIgnoreCase(cellName)) break;
			}
			int bits = 0;
			if (cell == null)
			{
				Cell leafcell = Sc_find_leaf_cell(cellName);
				if (leafcell == null)
					return "Cannot find cell '" + cellName + "'";
				Export leafport = leafcell.findExport(portName);
				if (leafport  == null) return "Cannot find port '" + portName + "' on cell '" + cellName + "'";
			} else
			{
				SCPORT port;
				for (port = cell.ports; port != null; port = port.next)
				{
					if (port.name.equalsIgnoreCase(portName)) break;
				}
				if (port == null)
					return "Cannot find port '" + portName + "' on cell '" + cellName + "'";
				bits = port.bits;
			}
			bits &= ~SCPORTDIRMASK;
			String dir = (String)keywords.get(4);
			for(int i=0; i<dir.length(); i++)
			{
				char dirCh = dir.charAt(i);
				switch (dirCh)
				{
					case 'u': bits |= SCPORTDIRUP;   break;
					case 'd': bits |= SCPORTDIRDOWN;   break;
					case 'r': bits |= SCPORTDIRRIGHT;   break;
					case 'l': bits |= SCPORTDIRLEFT;   break;
					default:
						return "Unknown port direction specifier '" + dir + "'";
				}
			}
			return null;
		}
		return "Unknown option '" + whatToSet+ "' for SET command";
	}

	/**
	 * Method to fill in the cell_nums structure for the indicated leaf cell.
	 */
	static SCCELLNUMS Sc_leaf_cell_get_nums(Cell leafcell)
	{
		SCCELLNUMS sNums = new SCCELLNUMS();
//		// check if variable exits
//		var = getvalkey((INTBIG)leafcell, VNODEPROTO, VINTEGER|VISARRAY, sc_numskey);
//		if (var != NOVARIABLE)
//		{
//			iarray = (int *)nums;
//			i = sizeof(SCCELLNUMS) / sizeof(int);
//			iarray = (int *)nums;
//			jarray = (int *)var->addr;
//			for (j = 0; j < i; j++) iarray[j] = jarray[j];
//		}
		return sNums;
	}

	/**
	 * Method to set the cell_nums variable for the indicated leaf cell.
	 */
	static void Sc_leaf_cell_set_nums(Cell leafcell, SCCELLNUMS nums)
	{
//		VARIABLE	*var;
//		int		i, j, *iarray;
//		INTBIG   *jarray;
//
//		i = sizeof(SCCELLNUMS) / sizeof(int);
//
//		// check if variable exits
//		var = getvalkey((INTBIG)leafcell, VNODEPROTO, VINTEGER|VISARRAY, sc_numskey);
//		if (var == NOVARIABLE)
//		{
//			if ((jarray = emalloc((i + 1) * sizeof(INTBIG), sc_tool->cluster)) == 0)
//				return(Sc_seterrmsg(SC_NOMEMORY));
//			iarray = (int *)nums;
//			for (j = 0; j < i; j++) jarray[j] = iarray[j];
//			jarray[j] = -1;
//			if (setvalkey((INTBIG)leafcell, VNODEPROTO, sc_numskey, (INTBIG)jarray,
//				VINTEGER | VISARRAY) == NOVARIABLE)
//					return(Sc_seterrmsg(SC_NOSET_CELL_NUMS));
//			return(SC_NOERROR);
//		}
//		iarray = (int *)nums;
//		jarray = (INTBIG *)var->addr;
//		for (j = 0; j < i; j++) jarray[j] = iarray[j];
//		return(SC_NOERROR);
	}

	/**
	 * Method to find the named cell.
	 * Looks in the main cell library, too.
	 */
	private static Cell Sc_find_leaf_cell(String name)
	{
		Cell cell = (Cell)Cell.findNodeProto(name);
		if (cell == null && sc_celllibrary != null)
		{
			cell = sc_celllibrary.findNodeProto(name);
			if (cell == null) return null;
		}
		if (cell != null)
		{
			Cell layCell = cell.otherView(View.LAYOUT);
			if (layCell != null) cell = layCell;
		}
		return cell;
	}

	private static Export Sc_find_leaf_port(Cell leafcell, String portname)
	{
		return leafcell.findExport(portname);
	}


	/**************************************** NETLIST READING: THE EXTRACT KEYWORD ****************************************/

	/**
	 * Method to extract the node netlist for a given cell.
	 */
	private static String Sc_extract(List keywords)
	{
		if (sc_curcell == null) return "No cell selected";
		Sc_extract_clear_flag(sc_curcell);
		sc_curcell.ex_nodes = null;

		Sc_extract_find_nodes(sc_curcell);

		// get ground nodes
		sc_curcell.ground = new SCEXTNODE();
		sc_curcell.ground.name = "ground";
		sc_curcell.ground.flags = 0;
		sc_curcell.ground.ptr = null;
		sc_curcell.ground.firstport = null;
		sc_curcell.ground.next = null;
		SCEXTNODE oldnlist = sc_curcell.ex_nodes;
		for (SCEXTNODE nlist = sc_curcell.ex_nodes; nlist != null; nlist = nlist.next)
		{
			SCEXTPORT oldplist = nlist.firstport;
			SCEXTPORT plist;
			for (plist = nlist.firstport; plist != null; plist = plist.next)
			{
				if (plist.node.number == GND)
				{
					sc_curcell.ground.firstport = nlist.firstport;
					if (oldnlist == nlist)
					{
						sc_curcell.ex_nodes = nlist.next;
					} else
					{
						oldnlist.next = nlist.next;
					}
					if (oldplist == plist)
					{
						sc_curcell.ground.firstport = plist.next;
					} else
					{
						oldplist.next = plist.next;
					}
					break;
				}
				oldplist = plist;
			}
			if (plist != null) break;
			oldnlist = nlist;
		}
		for (SCEXTPORT plist = sc_curcell.ground.firstport; plist != null; plist = plist.next)
			plist.port.ext_node = sc_curcell.ground;

		// get power nodes
		sc_curcell.power = new SCEXTNODE();
		sc_curcell.power.name = "power";
		sc_curcell.power.flags = 0;
		sc_curcell.power.ptr = null;
		sc_curcell.power.firstport = null;
		sc_curcell.power.next = null;
		oldnlist = sc_curcell.ex_nodes;
		for (SCEXTNODE nlist = sc_curcell.ex_nodes; nlist != null; nlist = nlist.next)
		{
			SCEXTPORT oldplist = nlist.firstport;
			SCEXTPORT plist;
			for (plist = nlist.firstport; plist != null; plist = plist.next)
			{
				if (plist.node.number == PWR)
				{
					sc_curcell.power.firstport = nlist.firstport;
					if (oldnlist == nlist)
					{
						sc_curcell.ex_nodes = nlist.next;
					} else
					{
						oldnlist.next = nlist.next;
					}
					if (oldplist == plist)
					{
						sc_curcell.power.firstport = plist.next;
					} else
					{
						oldplist.next = plist.next;
					}
					break;
				}
				oldplist = plist;
			}
			if (plist != null) break;
			oldnlist = nlist;
		}
		for (SCEXTPORT plist = sc_curcell.power.firstport; plist != null; plist = plist.next)
			plist.port.ext_node = sc_curcell.power;

		Sc_extract_find_power(sc_curcell, sc_curcell);

		Sc_extract_collect_unconnected(sc_curcell);

		// give the names of the cell ports to the extracted node
		for (SCPORT port = sc_curcell.ports; port != null; port = port.next)
		{
			switch (port.bits & SCPORTTYPE)
			{
				case SCPWRPORT:
				case SCGNDPORT:
					break;
				default:
					// Note that special nodes only have one niport
					port.node.ports.ext_node.name = port.name;
					break;
			}
		}

		// give arbitrary names to unnamed extracted nodes
		int nodenum = 2;
		for (SCEXTNODE ext = sc_curcell.ex_nodes; ext != null; ext = ext.next)
		{
			if (ext.name == null) ext.name = "n" + (nodenum++);
		}
		return null;
	}

	/**
	 * Method to clear the extract pointer on all node instance ports.
	 */
	private static void Sc_extract_clear_flag(SCCELL cell)
	{
		for (Iterator it = cell.nilist.iterator(); it.hasNext(); )
		{
			SCNITREE ntp = (SCNITREE)it.next();
			ntp.flags &= Place.SCBITS_EXTRACT;
			for (SCNIPORT port = ntp.ports; port != null; port = port.next)
				port.ext_node = null;
		}
	}

	/**
	 * Method to go though the INSTANCE list, finding all resultant connections.
	 */
	private static void Sc_extract_find_nodes(SCCELL cell)
	{
		for (Iterator it = cell.nilist.iterator(); it.hasNext(); )
		{
			SCNITREE nitree = (SCNITREE)it.next();
			nitree.flags |= Place.SCBITS_EXTRACT;
			for (SCCONLIST cl = nitree.connect; cl != null; cl = cl.next)
			{
				Sc_extract_snake(nitree, cl.portA, cl);
			}
		}
	}

	/**
	 * Method to snake through connection list extracting common connections.
	 */
	private static void Sc_extract_snake(SCNITREE nodeA, SCNIPORT portA, SCCONLIST cl)
	{
		for ( ; cl != null; cl = cl.next)
		{
			if (cl.portA != portA) continue;
			if (portA != null && portA.ext_node != null)
			{
				if (!(cl.portB != null && cl.portB.ext_node != null))
				{
					SCEXTNODE common = Sc_extract_add_node(portA.ext_node, cl.nodeB, cl.portB);
					if ((cl.nodeB.flags & Place.SCBITS_EXTRACT) == 0)
					{
						cl.nodeB.flags |= Place.SCBITS_EXTRACT;
						Sc_extract_snake(cl.nodeB, cl.portB, cl.nodeB.connect);
						cl.nodeB.flags ^= Place.SCBITS_EXTRACT;
					}
				}
			} else
			{
				if (cl.portB != null && cl.portB.ext_node != null)
				{
					SCEXTNODE common = Sc_extract_add_node(cl.portB.ext_node, nodeA, portA);
				} else
				{
					SCEXTNODE common = Sc_extract_add_node(null, nodeA, portA);
					common = Sc_extract_add_node(common, cl.nodeB, cl.portB);
					if ((cl.nodeB.flags & Place.SCBITS_EXTRACT) == 0)
					{
						cl.nodeB.flags |= Place.SCBITS_EXTRACT;
						Sc_extract_snake(cl.nodeB, cl.portB, cl.nodeB.connect);
						cl.nodeB.flags ^= Place.SCBITS_EXTRACT;
					}
				}
			}
		}
	}

	/**
	 * Method to add a node and port to a SCEXTNODE list.
	 * Modify the root if necessary.
	 */
	private static SCEXTNODE Sc_extract_add_node(SCEXTNODE simnode, SCNITREE node, SCNIPORT port)
	{
		SCEXTPORT newport = new SCEXTPORT();
		if (simnode == null)
		{
			simnode = new SCEXTNODE();
			simnode.firstport = newport;
			simnode.flags = 0;
			simnode.ptr = null;
			simnode.name = null;
			newport.node = node;
			newport.port = port;
			if (port != null)
				port.ext_node = simnode;
			newport.next = null;
			simnode.next = sc_curcell.ex_nodes;
			sc_curcell.ex_nodes = simnode;
		} else
		{
			newport.node = node;
			newport.port = port;
			if (port != null)
				port.ext_node = simnode;
			newport.next = simnode.firstport;
			simnode.firstport = newport;
		}
		return simnode;
	}

	/**
	 * Method to find the implicit power and ground ports.
	 * Does a search of the instance tree and adds to the appropriate port list.
	 * Skips over the dummy ground and power instances and special cells.
	 */
	private static void Sc_extract_find_power(SCCELL cell, SCCELL vars)
	{
		for (Iterator it = cell.nilist.iterator(); it.hasNext(); )
		{
			SCNITREE ntp = (SCNITREE)it.next();

			// process node
			if (ntp.number > PWR)
			{
				switch (ntp.type)
				{
					case SCCOMPLEXCELL:
						break;
					case SCSPECIALCELL:
						break;
					case SCLEAFCELL:
						for (SCNIPORT port = ntp.ground; port != null; port = port.next)
						{
							SCEXTPORT plist = new SCEXTPORT();
							plist.node = ntp;
							plist.port = port;
							port.ext_node = vars.ground;
							plist.next = vars.ground.firstport;
							vars.ground.firstport = plist;
						}
						for (SCNIPORT port = ntp.power; port != null; port = port.next)
						{
							SCEXTPORT plist = new SCEXTPORT();
							plist.node = ntp;
							plist.port = port;
							port.ext_node = vars.power;
							plist.next = vars.power.firstport;
							vars.power.firstport = plist;
						}
						break;
					default:
						break;
				}
			}
		}
	}

	/**************************************** PLACEMENT ****************************************/

	/**
	 * Method to flatten all complex cells in the current cell.
	 * It does this by creating instances of all instances from the
	 * complex cells in the current cell.  To insure the uniqueness of all
	 * instance names, the new instances have names "parent_inst.inst"
	 * where "parent_inst" is the name of the instance being expanded and
	 * "inst" is the name of the subinstance being pulled up.
	 */
	private static String Sc_pull()
	{
		// check if a cell is currently selected
		if (sc_curcell == null) return "No cell selected";

		// remember the original ones and delete them later
		List cellList = new ArrayList();
		for (Iterator it = sc_curcell.nilist.iterator(); it.hasNext(); )
		{
			SCNITREE inst = (SCNITREE)it.next();
			if (inst.type == SCCOMPLEXCELL)
				cellList.add(inst);
		}

		// expand all instances of complex cell type
		for (Iterator it = cellList.iterator(); it.hasNext(); )
		{
			SCNITREE inst = (SCNITREE)it.next();
			String err = Sc_pull_inst(inst, sc_curcell);
			if (err != null) return err;
		}

		// now remove the original ones
		List deleteList = new ArrayList();
		for (Iterator it = sc_curcell.nilist.iterator(); it.hasNext(); )
		{
			SCNITREE inst = (SCNITREE)it.next();
			if (inst.type == SCCOMPLEXCELL)
				deleteList.add(inst);
		}
		for (Iterator it = deleteList.iterator(); it.hasNext(); )
		{
			sc_curcell.nilist.remove(it.next());
		}
		return null;
	}

	/**
	 * Method to pull the indicated instance of a complex cell into the indicated parent cell.
	 * @param inst instance to be pulled up.
	 * @param cell parent cell.
	 */
	private static String Sc_pull_inst(SCNITREE inst, SCCELL cell)
	{
		SCCELL subcell = (SCCELL)inst.np;

		// first create components
		for (Iterator it = subcell.nilist.iterator(); it.hasNext(); )
		{
			SCNITREE subinst = (SCNITREE)it.next();
			if (subinst.type != SCSPECIALCELL)
			{
				List createPars = new ArrayList();
				createPars.add("create");
				createPars.add("instance");
				createPars.add(inst.name + "." + subinst.name);
				if (subinst.type == SCLEAFCELL)
				{
					createPars.add(((Cell)subinst.np).getName());
				} else
				{
					createPars.add(((SCCELL)subinst.np).name);
				}
				String err = Sc_create(createPars);
				if (err != null) return err;
			}
		}

		// create connections among these subinstances by using the
		// subcell's extracted node list.  Also resolve connections
		// to the parent cell instances by using exported port info.
		for (SCEXTNODE enode = subcell.ex_nodes; enode != null; enode = enode.next)
		{
			SCEXTNODE bnode = null;

			// check if the extracted node is an exported node
			for (SCNIPORT iport = inst.ports; iport != null; iport = iport.next)
			{
				if (((SCPORT)(iport.port)).node.ports.ext_node == enode)
				{
					bnode = iport.ext_node;
					break;
				}
			}
			if (bnode == null)
			{
				// this is a new internal node
				bnode = new SCEXTNODE();
				bnode.name = null;
				bnode.firstport = null;
				bnode.ptr = null;
				bnode.flags = 0;
				bnode.next = cell.ex_nodes;
				cell.ex_nodes = bnode;
			}

			// add ports to extracted node bnode
			for (SCEXTPORT eport = enode.firstport; eport != null; eport = eport.next)
			{
				// only add leaf cells or complex cells
				if (eport.node.type != SCSPECIALCELL)
				{
					SCEXTPORT nport = new SCEXTPORT();
					nport.node = Sc_findni(cell, inst.name + "." + eport.node.name);

					// add reference to extracted node to instance port list
					for (SCNIPORT iport = nport.node.ports; iport != null; iport = iport.next)
					{
						if (iport.port == eport.port.port)
						{
							nport.port = iport;
							iport.ext_node = bnode;
							nport.next = bnode.firstport;
							bnode.firstport = nport;
							break;
						}
					}
				}
			}
		}

		// add power ports for new instances
		for (SCEXTPORT eport = subcell.power.firstport; eport != null; eport = eport.next)
		{
			if (eport.node.type == SCSPECIALCELL) continue;
			SCEXTPORT nport = new SCEXTPORT();
			nport.node = Sc_findni(cell, inst.name + "." + eport.node.name);

			// add reference to extracted node to instance port list
			SCNIPORT iport;
			for (iport = nport.node.ports; iport != null; iport = iport.next)
			{
				if (iport.port == eport.port.port)
				{
					nport.port = iport;
					iport.ext_node = cell.power;
					break;
				}
			}
			if (iport == null)
			{
				for (iport = nport.node.power; iport != null; iport = iport.next)
				{
					if (iport.port == eport.port.port)
					{
						nport.port = iport;
						iport.ext_node = cell.power;
						break;
					}
				}
			}
			nport.next = cell.power.firstport;
			cell.power.firstport = nport;
		}

		// remove references to original instance in power list
		SCEXTPORT nport = cell.power.firstport;
		for (SCEXTPORT eport = cell.power.firstport; eport != null; eport = eport.next)
		{
			if (eport.node == inst)
			{
				if (eport == nport)
				{
					cell.power.firstport = eport.next;
					nport = eport.next;
				} else
				{
					nport.next = eport.next;
				}
			} else
			{
				nport = eport;
			}
		}

		// add ground ports
		for (SCEXTPORT eport = subcell.ground.firstport; eport != null; eport = eport.next)
		{
			if (eport.node.type == SCSPECIALCELL) continue;
			nport = new SCEXTPORT();
			nport.node = Sc_findni(cell, inst.name + "." + eport.node.name);

			// add reference to extracted node to instance port list
			SCNIPORT iport;
			for (iport = nport.node.ports; iport != null; iport = iport.next)
			{
				if (iport.port == eport.port.port)
				{
					nport.port = iport;
					iport.ext_node = cell.ground;
					break;
				}
			}
			if (iport == null)
			{
				for (iport = nport.node.ground; iport != null; iport = iport.next)
				{
					if (iport.port == eport.port.port)
					{
						nport.port = iport;
						iport.ext_node = cell.ground;
						break;
					}
				}
			}
			nport.next = cell.ground.firstport;
			cell.ground.firstport = nport;
		}

		// remove references to original instance in ground list
		nport = cell.ground.firstport;
		for (SCEXTPORT eport = cell.ground.firstport; eport != null; eport = eport.next)
		{
			if (eport.node == inst)
			{
				if (eport == nport)
				{
					cell.ground.firstport = eport.next;
					nport = eport.next;
				} else
				{
					nport.next = eport.next;
				}
			} else
			{
				nport = eport;
			}
		}

		// remove references to instance in exported node list
		for (SCEXTNODE enode = cell.ex_nodes; enode != null; enode = enode.next)
		{
			nport = enode.firstport;
			for (SCEXTPORT eport = enode.firstport; eport != null; eport = eport.next)
			{
				if (eport.node == inst)
				{
					if (eport == nport)
					{
						enode.firstport = eport.next;
						nport = eport.next;
					} else
					{
						nport.next = eport.next;
					}
				} else
				{
					nport = eport;
				}
			}
		}

		// find the value of the largest generically named extracted node
		int oldnum = 0;
		for (SCEXTNODE enode = cell.ex_nodes; enode != null; enode = enode.next)
		{
			if (enode.name != null)
			{
				int sptr = 0;
				char firstCh = enode.name.charAt(0);
				if (Character.toUpperCase(firstCh) == 'N')
				{
					sptr++;
					while (sptr < enode.name.length())
					{
						if (!Character.isDigit(enode.name.charAt(sptr))) break;
						sptr++;
					}
					if (sptr >= enode.name.length())
					{
						int newnum = TextUtils.atoi(enode.name.substring(1));
						if (newnum > oldnum)
							oldnum = newnum;
					}
				}
			}
		}

		// set the name of any unnamed nodes
		for (SCEXTNODE enode = cell.ex_nodes; enode != null; enode = enode.next)
		{
			if (enode.name == null)
			{
				enode.name = "n" + (++oldnum);
			}
		}

		// flatten any subinstances which are also complex cells
		for (Iterator it = subcell.nilist.iterator(); it.hasNext(); )
		{
			SCNITREE subinst = (SCNITREE)it.next();
			if (subinst.type == SCCOMPLEXCELL)
			{
				SCNITREE ninst = Sc_findni(cell, inst.name + "." + subinst.name);
				String err = Sc_pull_inst(ninst, cell);
				if (err != null) return err;
			}
		}
		return null;
	}
//
//	/***********************************************************************
//	Module:  Sc_extract_print_nodes
//	------------------------------------------------------------------------
//	Description:
//		Print the common nodes found.
//	------------------------------------------------------------------------
//	*/
//
//	void Sc_extract_print_nodes(SCCELL *vars)
//	{
//		int		i;
//		SCEXTNODE	*simnode;
//		SCEXTPORT	*plist;
//		CHAR	*portname;
//
//		i = 0;
//		if (vars->ground)
//		{
//			ttyputmsg(M_("Node %d  %s:"), i, vars->ground->name);
//			for (plist = vars->ground->firstport; plist; plist = plist->next)
//			{
//				switch (plist->node->type)
//				{
//					case SCSPECIALCELL:
//						portname = M_("Special");
//						break;
//					case SCCOMPLEXCELL:
//						portname = ((SCPORT *)(plist->port->port))->name;
//						break;
//					case SCLEAFCELL:
//						portname = Sc_leaf_port_name(plist->port->port);
//						break;
//					default:
//						portname = M_("Unknown");
//						break;
//				}
//				ttyputmsg(x_("    %-20s    %s"), plist->node->name, portname);
//			}
//		}
//		i++;
//
//		if (vars->power)
//		{
//			ttyputmsg(M_("Node %d  %s:"), i, vars->power->name);
//			for (plist = vars->power->firstport; plist; plist = plist->next)
//			{
//				switch (plist->node->type)
//				{
//					case SCSPECIALCELL:
//						portname = M_("Special");
//						break;
//					case SCCOMPLEXCELL:
//						portname = ((SCPORT *)(plist->port->port))->name;
//						break;
//					case SCLEAFCELL:
//						portname = Sc_leaf_port_name(plist->port->port);
//						break;
//					default:
//						portname = M_("Unknown");
//						break;
//				}
//				ttyputmsg(x_("    %-20s    %s"), plist->node->name, portname);
//			}
//		}
//		i++;
//
//		for (simnode = vars->ex_nodes; simnode; simnode = simnode->next)
//		{
//			ttyputmsg(M_("Node %d  %s:"), i, simnode->name);
//			for (plist = simnode->firstport; plist; plist = plist->next)
//			{
//				switch (plist->node->type)
//				{
//					case SCSPECIALCELL:
//						portname = M_("Special");
//						break;
//					case SCCOMPLEXCELL:
//						portname = ((SCPORT *)(plist->port->port))->name;
//						break;
//					case SCLEAFCELL:
//						portname = Sc_leaf_port_name(plist->port->port);
//						break;
//					default:
//						portname = M_("Unknown");
//						break;
//				}
//				ttyputmsg(x_("    %-20s    %s"), plist->node->name, portname);
//			}
//			i++;
//		}
//	}
//

	/**
	 * Method to collect the unconnected ports and create an extracted node for each.
	 */
	private static void Sc_extract_collect_unconnected(SCCELL cell)
	{
		for (Iterator it = cell.nilist.iterator(); it.hasNext(); )
		{
			SCNITREE nptr = (SCNITREE)it.next();

			// process node
			switch (nptr.type)
			{
				case SCCOMPLEXCELL:
				case SCLEAFCELL:
					for (SCNIPORT port = nptr.ports; port != null; port = port.next)
					{
						if (port.ext_node == null)
						{
							SCEXTNODE ext = new SCEXTNODE();
							ext.name = null;
							SCEXTPORT eport = new SCEXTPORT();
							eport.node = nptr;
							eport.port = port;
							eport.next = null;
							ext.firstport = eport;
							ext.flags = 0;
							ext.ptr = null;
							ext.next = cell.ex_nodes;
							cell.ex_nodes = ext;
							port.ext_node = ext;
						}
					}
					break;
				default:
					break;
			}
		}
	}
	static double Sc_leaf_cell_xsize(Cell cell)
	{
		Rectangle2D bounds = cell.getBounds();
		return bounds.getWidth();
	}

	static double Sc_leaf_cell_ysize(Cell cell)
	{
		Rectangle2D bounds = cell.getBounds();
		return bounds.getHeight();
	}

	/**
	 * Method to return the type of the leaf port.
	 * @param leafport pointer to leaf port.
	 * @return type of port.
	 */
	static int Sc_leaf_port_type(Export leafport)
	{
		if (leafport.isPower()) return SilComp.SCPWRPORT;
		if (leafport.isGround()) return SilComp.SCGNDPORT;
		if (leafport.getCharacteristic() == PortCharacteristic.BIDIR) return SilComp.SCBIDIRPORT;
		if (leafport.getCharacteristic() == PortCharacteristic.OUT) return SilComp.SCOUTPORT;
		if (leafport.getCharacteristic() == PortCharacteristic.IN) return SilComp.SCINPORT;
		return SilComp.SCUNPORT;
	}

	/**
	 * Method to return the directions that a port can be attached to.
	 * Values can be up, down, left, right.
	 */
	static int Sc_leaf_port_direction(PortProto port)
	{
		return SilComp.SCPORTDIRUP | SilComp.SCPORTDIRDOWN;
	}

	/**
	 * Method to return the xpos of the indicated leaf port from the left side of it's parent leaf cell.
	 * @param port the leaf port.
	 * @return position from left side of cell.
	 */
	static double Sc_leaf_port_xpos(Export port)
	{
		if (port == null)
			return 0;
		Poly poly = port.getOriginalPort().getPoly();
		Rectangle2D bounds = ((Cell)port.getParent()).getBounds();
		return poly.getCenterX() - bounds.getMinX();
	}

	/**
	 * Method to return the xpos of the indicated leaf port from the bottom side of it's parent leaf cell.
	 * @param port the leaf port.
	 * @return position from bottom side of cell.
	 */
	static double Sc_leaf_port_ypos(Export port)
	{
		if (port == null)
			return 0;
		Poly poly = port.getOriginalPort().getPoly();
		Rectangle2D bounds = ((Cell)port.getParent()).getBounds();
		return poly.getCenterY() - bounds.getMinY();
	}

//	/***********************************************************************
//	Module:  Sc_delete
//	------------------------------------------------------------------------
//	Description:
//		Delete (free) the entire QUISC allocated database.
//	------------------------------------------------------------------------
//	Calling Sequence:  err = Sc_delete();
//
//	Name		Type		Description
//	----		----		-----------
//	err			int			Returned error code, 0 = no error.
//	------------------------------------------------------------------------
//	*/
//
//	int Sc_delete(void)
//	{
//		SCCELL	*cel, *nextcel;
//		int		err;
//
//		// free all cells
//		for (cel = sc_cells; cel; cel = nextcel)
//		{
//			nextcel = cel->next;
//			err = Sc_free_cell(cel);
//			if (err) return(err);
//		}
//
//		sc_cells = NULL;
//		sc_curcell = NULL;
//		return(SC_NOERROR);
//	}
//
//	/***********************************************************************
//	Module:  Sc_free_cell
//	------------------------------------------------------------------------
//	Description:
//		Free the memory consumed by the indicated cell and its components.
//	------------------------------------------------------------------------
//	Calling Sequence:  err = Sc_free_cell(cell);
//
//	Name		Type		Description
//	----		----		-----------
//	cell		*SCCELL		Pointer to cell to free.
//	err			int			Returned error code, 0 = no error.
//	------------------------------------------------------------------------
//	*/
//
//	int Sc_free_cell(SCCELL *cell)
//	{
//		int			err;
//		SCNITREE	*inst, *nextinst;
//		SCSIM		*siminfo, *nextsiminfo;
//		SCEXTNODE	*enode, *nextenode;
//		SCPORT		*port, *nextport;
//
//		if (cell == NULL)
//			return(SC_NOERROR);
//
//		// free the name
//		err = Sc_free_string(cell->name);
//		if (err) return(err);
//
//		// free all instances
//		for (inst = cell->nilist; inst; inst = nextinst)
//		{
//			nextinst = inst->next;
//			err = Sc_free_instance(inst);
//			if (err) return(err);
//		}
//
//		// free any simulation information
//		for (siminfo = cell->siminfo; siminfo; siminfo = nextsiminfo)
//		{
//			nextsiminfo = siminfo->next;
//			err = Sc_free_string(siminfo->model);
//			// is it a memory leak to NOT free siminfo here !!!
//			if (err) return(err);
//		}
//
//		// free extracted nodes
//		for (enode = cell->ex_nodes; enode; enode = nextenode)
//		{
//			nextenode = enode->next;
//			err = Sc_free_extracted_node(enode);
//			if (err) return(err);
//		}
//
//		// free power and ground
//		if ((err = Sc_free_extracted_node(cell->power)))
//			return(err);
//		if ((err = Sc_free_extracted_node(cell->ground)))
//			return(err);
//
//		// free ports
//		for (port = cell->ports; port; port = nextport)
//		{
//			nextport = port->next;
//			err = Sc_free_port(port);
//			if (err) return(err);
//		}
//
//		// free placement information
//		if ((err = Sc_free_placement(cell->placement)))
//			return(err);
//
//		// free routing information
//		if ((err = Sc_free_route(cell->route)))
//			return(err);
//
//		efree((CHAR *)cell);
//
//		return(SC_NOERROR);
//	}
//
//	/***********************************************************************
//	Module:  Sc_free_string
//	------------------------------------------------------------------------
//	Description:
//		Free the memory consumed by the indicated string.
//	------------------------------------------------------------------------
//	Calling Sequence:  err = Sc_free_string(string);
//
//	Name		Type		Description
//	----		----		-----------
//	string		*char		Pointer to string.
//	err			int			Returned error code, 0 = no error.
//	------------------------------------------------------------------------
//	*/
//
//	int Sc_free_string(CHAR *string)
//	{
//		if (string) efree(string);
//		return(SC_NOERROR);
//	}
//
//	/***********************************************************************
//	Module:  Sc_free_port
//	------------------------------------------------------------------------
//	Description:
//		Free the memory consumed by the indicated port and its components.
//	------------------------------------------------------------------------
//	Calling Sequence:  err = Sc_free_port(port);
//
//	Name		Type		Description
//	----		----		-----------
//	port		*SCPORT		Port to be freed.
//	err			int			Returned error code, 0 = no error.
//	------------------------------------------------------------------------
//	*/
//
//	int Sc_free_port(SCPORT *port)
//	{
//		int		err;
//
//		if (port)
//		{
//			if ((err = Sc_free_string(port->name)))
//				return(err);
//			efree((CHAR *)port);
//		}
//		return(SC_NOERROR);
//	}
//
//	/***********************************************************************
//	Module:  Sc_free_instance
//	------------------------------------------------------------------------
//	Description:
//		Free the memory consumed by the indicated instance and its components.
//	------------------------------------------------------------------------
//	Calling Sequence:  err = Sc_free_instance(inst);
//
//	Name		Type		Description
//	----		----		-----------
//	inst		*SCNITREE	Instance to be freed.
//	err			int			Returned error code, 0 = no error.
//	------------------------------------------------------------------------
//	*/
//
//	int Sc_free_instance(SCNITREE *inst)
//	{
//		int			err;
//		SCCONLIST	*con, *nextcon;
//		SCNIPORT	*nport, *nextnport;
//
//		if (inst)
//		{
//			if ((err = Sc_free_string(inst->name)))
//				return(err);
//			for (con = inst->connect; con; con = nextcon)
//			{
//				nextcon = con->next;
//				efree((CHAR *)con);
//			}
//			for (nport = inst->ports; nport; nport = nextnport)
//			{
//				nextnport = nport->next;
//				if ((err = Sc_free_instance_port(nport)))
//					return(err);
//			}
//			if ((err = Sc_free_instance_port(inst->power)))
//				return(err);
//			if ((err = Sc_free_instance_port(inst->ground)))
//				return(err);
//			efree((CHAR *)inst);
//		}
//
//		return(SC_NOERROR);
//	}
//
//	/***********************************************************************
//	Module:  Sc_free_instance_port
//	------------------------------------------------------------------------
//	Description:
//		Free the memory consumed by the indicated instance port.
//	------------------------------------------------------------------------
//	Calling Sequence:  err = Sc_free_instance_port(port);
//
//	Name		Type		Description
//	----		----		-----------
//	port		*SCNIPORT	Pointer to instance port.
//	err			int			Returned error code, 0 = no error.
//	------------------------------------------------------------------------
//	*/
//
//	int Sc_free_instance_port(SCNIPORT *port)
//	{
//		if (port) efree((CHAR *)port);
//		return(SC_NOERROR);
//	}
//
//	/***********************************************************************
//	Module:  Sc_free_extracted_node
//	------------------------------------------------------------------------
//	Description:
//		Free the indicated extraced node and its contents.
//	------------------------------------------------------------------------
//	Calling Sequence:  err = Sc_free_extracted_node(enode);
//
//	Name		Type		Description
//	----		----		-----------
//	enode		*SCEXTNODE	Pointer to extracted node.
//	err			int			Returned error code, 0 = no error.
//	------------------------------------------------------------------------
//	*/
//
//	int Sc_free_extracted_node(SCEXTNODE *enode)
//	{
//		int			err;
//		SCEXTPORT	*eport, *nexteport;
//
//		if (enode)
//		{
//			if ((err = Sc_free_string(enode->name)))
//				return(err);
//			for (eport = enode->firstport; eport; eport = nexteport)
//			{
//				nexteport = eport->next;
//				efree((CHAR *)eport);
//			}
//			efree((CHAR *)enode);
//		}
//
//		return(SC_NOERROR);
//	}
//
//	/***********************************************************************
//	Module:  Sc_free_placement
//	------------------------------------------------------------------------
//	Description:
//		Free the indicated placement structure and its contents.
//	------------------------------------------------------------------------
//	Calling Sequence:  err = Sc_free_placement(placement);
//
//	Name		Type		Description
//	----		----		-----------
//	placement	*SCPLACE	Pointer to placement structure.
//	err			int			Returned error code, 0 = no error.
//	------------------------------------------------------------------------
//	*/
//
//	int Sc_free_placement(SCPLACE *placement)
//	{
//		int			err;
//		SCROWLIST	*row, *nextrow;
//
//		if (placement)
//		{
//			for (row = placement->rows; row; row = nextrow)
//			{
//				nextrow = row->next;
//				if ((err = Sc_free_row(row)))
//					return(err);
//			}
//			efree((CHAR *)placement);
//		}
//
//		return(SC_NOERROR);
//	}
//
//	/***********************************************************************
//	Module:  Sc_free_row
//	------------------------------------------------------------------------
//	Description:
//		Free the indicated row structure and its contents.
//	------------------------------------------------------------------------
//	Calling Sequence:  err = Sc_free_row(row);
//
//	Name		Type		Description
//	----		----		-----------
//	row			*SCROWLIST	Pointer to row.
//	err			int			Returned error code, 0 = no error.
//	------------------------------------------------------------------------
//	*/
//
//	int Sc_free_row(SCROWLIST *row)
//	{
//		int			err;
//		SCNBPLACE	*place, *nextplace;
//
//		if (row)
//		{
//			for (place = row->start; place; place = nextplace)
//			{
//				nextplace = place->next;
//				if ((err = Sc_free_place(place)))
//					return(err);
//			}
//			efree((CHAR *)row);
//		}
//
//		return(SC_NOERROR);
//	}
//
//	/***********************************************************************
//	Module:  Sc_free_place
//	------------------------------------------------------------------------
//	Description:
//		Free the indicated place structure and its contents.
//	------------------------------------------------------------------------
//	Calling Sequence:  err = Sc_free_place(place);
//
//	Name		Type		Description
//	----		----		-----------
//	place		*SCNBPLACE	Pointer to place.
//	err			int			Returned error code, 0 = no error.
//	------------------------------------------------------------------------
//	*/
//
//	int Sc_free_place(SCNBPLACE *place)
//	{
//		if (place) efree((CHAR *)place);
//
//		return(SC_NOERROR);
//	}
//
//	/***********************************************************************
//	Module:  Sc_free_route
//	------------------------------------------------------------------------
//	Description:
//		Free the indicated route structure and its contents.
//	------------------------------------------------------------------------
//	Calling Sequence:  err = Sc_free_route(route);
//
//	Name		Type		Description
//	----		----		-----------
//	route		*SCROUTE	Pointer to the route structure.
//	err			int			Returned error code, 0 = no error.
//	------------------------------------------------------------------------
//	*/
//
//	int Sc_free_route(SCROUTE *route)
//	{
//		int				err;
//		SCROUTECHANNEL	*chan, *nextchan;
//		SCROUTEEXPORT	*xport, *nextxport;
//		SCROUTEROW		*row, *nextrow;
//
//		if (route)
//		{
//			for (chan = route->channels; chan; chan = nextchan)
//			{
//				nextchan = chan->next;
//				if ((err = Sc_free_route_channel(chan)))
//					return(err);
//			}
//			for (xport = route->exports; xport; xport = nextxport)
//			{
//				nextxport = xport->next;
//				efree((CHAR *)xport);
//			}
//			for (row = route->rows; row; row = nextrow)
//			{
//				nextrow = row->next;
//				if ((err = Sc_free_route_row(row)))
//					return(err);
//			}
//			efree((CHAR *)route);
//		}
//
//		return(SC_NOERROR);
//	}
//
//	/***********************************************************************
//	Module:  Sc_free_route_channel
//	------------------------------------------------------------------------
//	Description:
//		Free the indicated route channel structure and its contents.
//	------------------------------------------------------------------------
//	Calling Sequence:  err = Sc_free_route_channel(chan);
//
//	Name		Type			Description
//	----		----			-----------
//	chan		*SCROUTECHANNEL	Route channel structure.
//	err			int				Returned error code, 0 = no error.
//	------------------------------------------------------------------------
//	*/
//
//	int Sc_free_route_channel(SCROUTECHANNEL *chan)
//	{
//		int				err;
//		SCROUTECHNODE	*node, *nextnode;
//		SCROUTETRACK	*track, *nexttrack;
//
//		if (chan)
//		{
//			for (node = chan->nodes; node; node = nextnode)
//			{
//				nextnode = node->next;
//				if ((err = Sc_free_channel_node(node)))
//					return(err);
//			}
//			for (track = chan->tracks; track; track = nexttrack)
//			{
//				nexttrack = track->next;
//				if ((err = Sc_free_channel_track(track)))
//					return(err);
//			}
//			efree((CHAR *)chan);
//		}
//
//		return(SC_NOERROR);
//	}
//
//	/***********************************************************************
//	Module:  Sc_free_channel_node
//	------------------------------------------------------------------------
//	Description:
//		Free the indicated route channel node structure and its contents.
//	------------------------------------------------------------------------
//	Calling Sequence:  err = Sc_free_route_channel_node(node);
//
//	Name		Type			Description
//	----		----			-----------
//	node		*SCROUTECHNODE	Pointer to route channel node.
//	err			int				Returned error code, 0 = no error.
//	------------------------------------------------------------------------
//	*/
//
//	int Sc_free_channel_node(SCROUTECHNODE *node)
//	{
//		SCROUTECHPORT	*chport, *nextchport;
//
//		if (node)
//		{
//			for (chport = node->firstport; chport; chport = nextchport)
//			{
//				nextchport = chport->next;
//				efree((CHAR *)chport);
//			}
//			efree((CHAR *)node);
//		}
//
//		return(SC_NOERROR);
//	}
//
//	/***********************************************************************
//	Module:  Sc_free_channel_track
//	------------------------------------------------------------------------
//	Description:
//		Free the indicated route channel track structure and its contents.
//	------------------------------------------------------------------------
//	Calling Sequence:  err = Sc_free_route_channel_track(track);
//
//	Name		Type			Description
//	----		----			-----------
//	track		*SCROUTETRACK	Pointer to route channel track.
//	err			int				Returned error code, 0 = no error.
//	------------------------------------------------------------------------
//	*/
//
//	int Sc_free_channel_track(SCROUTETRACK *track)
//	{
//		SCROUTETRACKMEM	*mem, *nextmem;
//
//		if (track)
//		{
//			for (mem = track->nodes; mem; mem = nextmem)
//			{
//				nextmem = mem->next;
//				efree((CHAR *)mem);
//			}
//			efree((CHAR *)track);
//		}
//
//		return(SC_NOERROR);
//	}
//
//	/***********************************************************************
//	Module:  Sc_free_route_row
//	------------------------------------------------------------------------
//	Description:
//		Free the indicated route row structure and its contents.
//	------------------------------------------------------------------------
//	Calling Sequence:  err = Sc_free_route_row(row);
//
//	Name		Type		Description
//	----		----		-----------
//	row			*SCROUTEROW	Pointer to route row.
//	err			int			Returned error code, 0 = no error.
//	------------------------------------------------------------------------
//	*/
//
//	int Sc_free_route_row(SCROUTEROW *row)
//	{
//		int				err;
//		SCROUTENODE		*node, *nextnode;
//
//		if (row)
//		{
//			for (node = row->nodes; node; node = nextnode)
//			{
//				nextnode = node->next;
//				if ((err = Sc_free_route_node(node))) return(err);
//			}
//			efree((CHAR *)row);
//		}
//
//		return(SC_NOERROR);
//	}
//
//	/***********************************************************************
//	Module:  Sc_free_route_node
//	------------------------------------------------------------------------
//	Description:
//		Free the indicated route route node structure and its contents.
//	------------------------------------------------------------------------
//	Calling Sequence:  err = Sc_free_route_route_node(node);
//
//	Name		Type			Description
//	----		----			-----------
//	node		*SCROUTENODE	Pointer to route node.
//	err			int				Returned error code, 0 = no error.
//	------------------------------------------------------------------------
//	*/
//
//	int Sc_free_route_node(SCROUTENODE *node)
//	{
//		SCROUTEPORT		*port, *nextport;
//
//		if (node)
//		{
//			for (port = node->firstport; port; port = nextport)
//			{
//				nextport = port->next;
//				efree((CHAR *)port);
//			}
//			efree((CHAR *)node);
//		}
//
//		return(SC_NOERROR);
//	}

}
