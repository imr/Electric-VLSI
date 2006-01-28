/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GetNetlist.java
 * Silicon compiler tool (QUISC): read a netlist
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is the netlist reader of the Silicon Compiler tool.
 */
public class GetNetlist
{
	/***********************************************************************
		General Constants
	------------------------------------------------------------------------*/
	private static final int GND = 0;
	private static final int PWR = 1;

	/***** Directions that ports can be attached to *****/
	/** mask for port direction */		public static final int PORTDIRMASK	= 0x0000000F;
	/** port direction up */			public static final int PORTDIRUP	= 0x00000001;
	/** port direction down */			public static final int PORTDIRDOWN	= 0x00000002;
	/** port direction right */			public static final int PORTDIRRIGHT= 0x00000004;
	/** port direction left */			public static final int PORTDIRLEFT	= 0x00000008;
	/** port type mask */				public static final int PORTTYPE	= 0x000003F0;
	/** ground port */					public static final int GNDPORT		= 0x00000010;
	/** power port */					public static final int PWRPORT		= 0x00000020;
	/** bidirectional port */			public static final int BIDIRPORT	= 0x00000040;
	/** output port */					public static final int OUTPORT		= 0x00000080;
	/** input port */					public static final int INPORT		= 0x00000100;
	/** unknown port */					public static final int UNPORT		= 0x00000200;

	/***********************************************************************
		QUISC Cell Structure
	------------------------------------------------------------------------*/

	static class SCCell
	{
		/** name of complex cell */				String    		name;
		/** maximum number of nodes */			int       		maxNodeNum;
		/** list of instances for cell */		List<SCNiTree>	niList;
		/** extracted nodes */					ExtNode 		exNodes;
		/** flags for processing cell */		int       		bits;
		/** list of power ports */				ExtNode 		power;
		/** list of ground ports */				ExtNode 		ground;
		/** list of ports */					SCPort    		ports, lastPort;
		/** placement information of cell */	Place.SCPlace   placement;
		/** routing information for cell */		Route.SCRoute   route;
		/** next in list of SC cells */			SCCell    		next;
	};

	static class SCPort
	{
		/** name of port */						String   name;
		/** special node */						SCNiTree node;
		/** cell on which this port resides */	SCCell   parent;
		/** port attributes */					int      bits;
		/** pointer to next port */				SCPort   next;
	};

	static class SCCellNums
	{
		/** active area from top */				int		topActive;
		/** active are from bottom */			int		bottomActive;
		/** active area from left */			int		leftActive;
		/** active are from right */			int		rightActive;
	};

	/***********************************************************************
		Instance Tree Structure
	------------------------------------------------------------------------*/

	/***** Types of Instances *****/
	public static final int LEAFCELL     = 0;
	public static final int COMPLEXCELL  = 1;
	public static final int SPECIALCELL  = 2;
	public static final int FEEDCELL     = 3;
	public static final int STITCH       = 4;
	public static final int LATERALFEED  = 5;

	static class SCNiTree
	{
		/** name of instance */					String		name;
		/** alternative number of node */		int			number;
		/** type of instance */					int			type;
		/** leaf cell or SCCell if complex */	Object		np;
		/** x size if leaf cell */				double		size;
		/** pointer to connection list */		ConList		connect;
		/** list of io ports and ext nodes */	SCNiPort	ports;
		/** list of actual power ports */		SCNiPort	power;
		/** list of actual ground ports */		SCNiPort	ground;
		/** bits for silicon compiler */		int			flags;
		/** generic temporary pointer */		Object		tp;

		/**
		 * Method to create a new instance with a given name and type.
		 * @param name name of the instance.
		 * @param type type of the instance.
		 */
		SCNiTree(String name, int type)
		{
			this.name = name;
			this.type = type;
			this.number = 0;
			this.np = null;
			this.size = 0;
			this.connect = null;
			this.ports = null;
			this.power = null;
			this.ground = null;
			this.flags = 0;
			this.tp = null;
		}
	};

	static class SCNiPort
	{
		/** leaf port or SCPort if on cell */	Object		port;
		/** extracted node */					ExtNode		extNode;
		/** bits for processing */				int			bits;
		/** x position if leaf port */			double		xPos;
		/** list of instance ports */			SCNiPort	next;

		SCNiPort() {}

		SCNiPort(SCNiTree instance)
		{
			this.port = null;
			this.extNode = null;
			this.bits = 0;
			this.xPos = 0;
			this.next = instance.ports;
			instance.ports = this;
		}
	};

	/***********************************************************************
		Connection Structures
	------------------------------------------------------------------------*/

	private static class ConList
	{
		/** pointer to port on node A */		SCNiPort	portA;
		/** pointer to node B */				SCNiTree	nodeB;
		/** pointer to port on node B */		SCNiPort	portB;
		/** pointer to extracted node */		ExtNode		extNode;
		/** pointer to next list element */		ConList   	next;
	};

	/***********************************************************************
		Extraction Structures
	------------------------------------------------------------------------*/

	private static class ExtPort
	{
		/** instance of extracted node */		SCNiTree	node;
		/** instance port */					SCNiPort	port;
		/** next in list of common node */		ExtPort		next;
	};

	/**
	 * Class for communicating netlist information to the placer and router.
	 */
	public static class ExtNode
	{
		/** optional name of port */			String		name;
		/** link list of ports */				ExtPort		firstPort;
		/** flags for processing */				int			flags;
		/** generic pointer for processing */	Object		ptr;
		/** link list of nodes */				ExtNode		next;
	};

	SCCell	scCells, curSCCell;

	/**
	 * Read the netlist associated with the current cell.
	 * @return true on error.
	 */
	public boolean readNetCurCell(Cell cell)
	{
		scCells = null;
		curSCCell = null;
		if (cell.getView() != View.NETLISTQUISC)
		{
			System.out.println("Current cell must have QUISC Netlist view");
			return true;
		}
		String [] strings = cell.getTextViewContents();
		if (strings == null)
		{
			System.out.println("Cell " + cell.describe(true) + " has no text in it");
			return true;
		}

		// read entire netlist
		boolean errors = false;
		for(int i=0; i<strings.length; i++)
		{
			String inBuf = strings[i].trim();

			// check for a comment line or empty line
			if (inBuf.length() == 0 || inBuf.charAt(0) == '!') continue;

			// break into keywords
			List<String> parameters = new ArrayList<String>();
			for(int sPtr = 0; sPtr < inBuf.length(); sPtr++)
			{
				// get rid of leading white space
				while (sPtr < inBuf.length() && (inBuf.charAt(sPtr) == ' ' || inBuf.charAt(sPtr) == '\t')) sPtr++;
				if (sPtr >= inBuf.length()) break;

				// check for string
				if (inBuf.charAt(sPtr) == '"')
				{
					sPtr++;
					int endQuote = inBuf.indexOf('"', sPtr);
					if (endQuote < 0)
					{
						System.out.println("ERROR line " + (i+1) + ": Unbalanced quotes ");
						errors = true;
						break;
					}
					parameters.add(inBuf.substring(sPtr, endQuote));
					sPtr = endQuote;
				} else
				{
					int endSpace = inBuf.indexOf(' ', sPtr);
					int endTab = inBuf.indexOf('\t', sPtr);
					if (endSpace < 0) endSpace = endTab;
					if (endSpace < 0) endSpace = inBuf.length();
					if (endTab >= 0 && endTab < endSpace) endSpace = endTab;
					parameters.add(inBuf.substring(sPtr, endSpace));
					sPtr = endSpace;
				}
			}
			String err = parse(parameters);
			if (err != null)
			{
				System.out.println("ERROR line " + (i+1) + ": " + err);
				System.out.println("      Line: " + inBuf);
				errors = true;
				break;
			}
		}

		// do the "pull" (extract)
		String err = pull();
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
	private String parse(List<String> keywords)
	{
		if (keywords.size() == 0) return null;
		String mainKeyword = keywords.get(0);

		if (mainKeyword.equalsIgnoreCase("connect")) return connect(keywords);
		if (mainKeyword.equalsIgnoreCase("create")) return create(keywords);
		if (mainKeyword.equalsIgnoreCase("export")) return export(keywords);
		if (mainKeyword.equalsIgnoreCase("extract")) return extract(keywords);
		if (mainKeyword.equalsIgnoreCase("set")) return xSet(keywords);
		return "Unknown keyword: " + mainKeyword;
	}

	/**************************************** NETLIST READING: THE CREATE KEYWORD ****************************************/

	private String create(List<String> keywords)
	{
		if (keywords.size() <= 1) return "No keyword for CREATE command";
		String sPtr = keywords.get(1);
		if (sPtr.equalsIgnoreCase("cell"))
		{
			if (keywords.size() <= 2) return "No name for CREATE CELL command";
			sPtr = keywords.get(2);

			// check if cell already exists in cell list
			for(SCCell cell = scCells; cell != null; cell = cell.next)
			{
				if (sPtr.equalsIgnoreCase(cell.name))
					return "Cell '" + sPtr + "' already exists in current library";
			}

			// generate warning message if a leaf cell of the same name exists
			if (findLeafCell(sPtr) != null)
				System.out.println("WARNING - cell " + sPtr + " may be overridden by created cell");

			// create new cell
			SCCell newCell = new SCCell();
			newCell.name = sPtr;
			newCell.maxNodeNum = 0;
			newCell.niList = new ArrayList<SCNiTree>();
			newCell.exNodes = null;
			newCell.bits = 0;
			newCell.power = null;
			newCell.ground = null;
			newCell.ports = null;
			newCell.lastPort = null;
			newCell.placement = null;
			newCell.route = null;
			newCell.next = scCells;
			scCells = newCell;
			curSCCell = newCell;

			// create dummy ground and power nodes
			SCNiTree ntp = findNi(curSCCell, "ground");
			if (ntp != null) return "Instance 'ground' already exists";
			ntp = new SCNiTree("ground", SPECIALCELL);
			curSCCell.niList.add(ntp);
			new SCNiPort(ntp);
			ntp.number = curSCCell.maxNodeNum++;

			ntp = findNi(curSCCell, "power");
			if (ntp != null) return "Instance 'power' already exists";
			ntp = new SCNiTree("power", SPECIALCELL);
			curSCCell.niList.add(ntp);
			new SCNiPort(ntp);
			ntp.number = curSCCell.maxNodeNum++;
			return null;
		}

		if (sPtr.equalsIgnoreCase("instance"))
		{
			if (keywords.size() <= 2) return "No instance name for CREATE INSTANCE command";
			String noden = keywords.get(2);

			if (keywords.size() <= 3) return "No type name for CREATE INSTANCE command";
			String nodep = keywords.get(3);

			// search for cell in cell list
			SCCell cell = null;
			for(SCCell c = scCells; c != null; c = c.next)
			{
				if (nodep.equalsIgnoreCase(c.name)) { cell = c;   break; }
			}
			Object proto = cell;
			int type = COMPLEXCELL;
			double size = 0;
			if (cell == null)
			{
				// search for leaf cell in library
				Cell bc = findLeafCell(nodep);
				if (bc == null)
					return "There is no '" + nodep + "' in the standard cell library";
				proto = bc;
				type = LEAFCELL;
				size = SilComp.leafCellXSize(bc);
			}

			// check if currently working in a cell
			if (curSCCell == null) return "No cell selected";

			// check if instance name already exits
			SCNiTree ntp = findNi(curSCCell, noden);
			if (ntp != null)
				return "Instance '" + noden + "' already exists";

			// add instance name to tree
			ntp = new SCNiTree(noden, type);
			curSCCell.niList.add(ntp);
			ntp.number = curSCCell.maxNodeNum++;
			ntp.np = proto;
			ntp.size = size;

			// create ni port list
			if (type == COMPLEXCELL)
			{
				SCNiPort oldNiPort = null;
				for (SCPort port = ((SCCell)proto).ports; port != null; port = port.next)
				{
					SCNiPort niPort = new SCNiPort();
					niPort.port = port;
					niPort.extNode = null;
					niPort.bits = 0;
					niPort.xPos = 0;
					switch (port.bits & PORTTYPE)
					{
						case GNDPORT:
							niPort.next = ntp.ground;
							ntp.ground = niPort;
							break;
						case PWRPORT:
							niPort.next = ntp.power;
							ntp.power = niPort;
							break;
						default:
							niPort.next = null;
							if (oldNiPort == null)
							{
								ntp.ports = niPort;
							} else
							{
								oldNiPort.next = niPort;
							}
							oldNiPort = niPort;
							break;
					}
				}
			} else
			{
				SCNiPort oldNiPort = null;
				Cell realCell = (Cell)proto;
				for(Iterator<PortProto> it = realCell.getPorts(); it.hasNext(); )
				{
					Export bp = (Export)it.next();
					SCNiPort niPort = new SCNiPort();
					niPort.port = bp;
					niPort.extNode = null;
					niPort.bits = 0;
					niPort.xPos = SilComp.leafPortXPos(bp);
					switch (getLeafPortType(bp))
					{
						case GNDPORT:
							niPort.next = ntp.ground;
							ntp.ground = niPort;
							break;
						case PWRPORT:
							niPort.next = ntp.power;
							ntp.power = niPort;
							break;
						default:
							niPort.next = null;
							if (oldNiPort == null)
							{
								ntp.ports = niPort;
							} else
							{
								oldNiPort.next = niPort;
							}
							oldNiPort = niPort;
							break;
					}
				}
			}
			return null;
		}
		return "Unknown CREATE command: " + sPtr;
	}

	/**
	 * Method to find the location in a SCNiTree where the node should be placed.
	 * If the pointer does not have a value of null, then the object already exits.
	 */
	private SCNiTree findNi(SCCell cell, String name)
	{
		for(SCNiTree nPtr : cell.niList)
		{
			if (nPtr.name.equalsIgnoreCase(name)) return nPtr;
		}
		return null;
	}

	/**************************************** NETLIST READING: THE CONNECT KEYWORD ****************************************/

	private String connect(List<String> keywords)
	{
		if (keywords.size() < 4) return "Not enough parameters for CONNECT command";

		// search for the first node
		String node0Name = keywords.get(1);
		String port0Name = keywords.get(2);
		SCNiTree ntpA = findNi(curSCCell, node0Name);
		if (ntpA == null) return "Cannot find instance '" + node0Name + "'";
		SCNiPort portA = findPp(ntpA, port0Name);
		if (portA == null)
			return "Cannot find port '" + port0Name + "' on instance '" + node0Name + "'";

		// search for the second node
		String node1Name = keywords.get(3);
		SCNiTree ntpB = findNi(curSCCell, node1Name);
		if (ntpB == null) return "Cannot find instance '" + node1Name + "'";

		// check for special power or ground node
		SCNiPort portB = ntpB.ports;
		if (ntpB.type != SPECIALCELL)
		{
			if (keywords.size() < 5) return "Not enough parameters for CONNECT command";
			String port1Name = keywords.get(4);
			portB = findPp(ntpB, port1Name);
			if (portB == null)
				return "Cannot find port '" + port1Name + "' on instance '" + node1Name + "'";
		}
		conList(ntpA, portA, ntpB, portB);
		return null;
	}

	/**
	 * Method to find the port on the given node instance.
	 * @return null if port is not found.
	 */
	private SCNiPort findPp(SCNiTree ntp, String name)
	{
		SCNiPort port = null;

		if (ntp == null) return null;
		switch (ntp.type)
		{
			case SPECIALCELL:
				return ntp.ports;
			case COMPLEXCELL:
				for (port = ntp.ports; port != null; port = port.next)
				{
					if (((SCPort)port.port).name.equalsIgnoreCase(name)) break;
				}
				break;
			case LEAFCELL:
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
	private void conList(SCNiTree ntpA, SCNiPort portA, SCNiTree ntpB, SCNiPort portB)
	{
		// add connection to instance A
		ConList cl = new ConList();
		cl.portA = portA;
		cl.nodeB = ntpB;
		cl.portB = portB;
		cl.extNode = null;

		// add to head of the list
		cl.next = ntpA.connect;
		ntpA.connect = cl;

		// add connection to instance B
		cl = new ConList();
		cl.portA = portB;
		cl.nodeB = ntpA;
		cl.portB = portA;
		cl.extNode = null;

		// add to head of the list
		cl.next = ntpB.connect;
		ntpB.connect = cl;
	}

	/**************************************** NETLIST READING: THE EXPORT KEYWORD ****************************************/

	private String export(List<String> keywords)
	{
		// check to see if working in a cell
		if (curSCCell == null) return "No cell selected";

		// search current cell for node
		if (keywords.size() <= 1) return "No instance specified for EXPORT command";
		String instName = keywords.get(1);
		SCNiTree nPtr = findNi(curSCCell, instName);
		if (nPtr == null) return "Cannot find instance '" + instName + "' for EXPORT command";

		// search for port
		if (keywords.size() <= 2) return "No port specified for EXPORT command";
		String portName = keywords.get(2);
		SCNiPort port = findPp(nPtr, portName);
		if (port == null)
			return "Cannot find port '" + portName + "' on instance '" + instName + "' for EXPORT command";

		// check for export name
		if (keywords.size() <= 3) return "No export name specified for EXPORT command";
		String exportName = keywords.get(3);

		// check possible port type
		int type = UNPORT;
		if (keywords.size() >= 5)
		{
			String typeName = keywords.get(4);
			if (typeName.equalsIgnoreCase("input"))
			{
				type = INPORT;
			} else if (typeName.equalsIgnoreCase("output"))
			{
				type = OUTPORT;
			} else if (typeName.equalsIgnoreCase("bidirectional"))
			{
				type = BIDIRPORT;
			} else
			{
				return "Unknown port type '" + typeName + "' for EXPORT command";
			}
		}

		// create special node
		SCNiTree searchNPtr = findNi(curSCCell, exportName);
		if (searchNPtr != null) return "Export name '" + exportName + "' is not unique";
		SCNiTree newNPtr = new SCNiTree(exportName, SPECIALCELL);
		curSCCell.niList.add(newNPtr);
		newNPtr.number = curSCCell.maxNodeNum++;
		searchNPtr = newNPtr;
		SCNiPort niPort = new SCNiPort();
		niPort.port = null;
		niPort.extNode = null;
		niPort.next = null;
		newNPtr.ports = niPort;

		// add to export port list
		SCPort newPort = new SCPort();
		niPort.port = newPort;
		newPort.name = exportName;
		newPort.node = newNPtr;
		newPort.parent = curSCCell;
		newPort.bits = type;
		newPort.next = null;
		if (curSCCell.lastPort == null)
		{
			curSCCell.ports = curSCCell.lastPort = newPort;
		} else
		{
			curSCCell.lastPort.next = newPort;
			curSCCell.lastPort = newPort;
		}

		// add to connect list
		conList(nPtr, port, newNPtr, niPort);
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
	private String xSet(List<String> keywords)
	{
		if (keywords.size() <= 1) return "No option for SET command";
		String whatToSet = keywords.get(1);

		if (whatToSet.equalsIgnoreCase("leaf-cell-numbers"))
		{
			String cellName = keywords.get(2);
			Cell leafCell = findLeafCell(cellName);
			if (leafCell == null) return "Cannot find cell '" + cellName + "'";
			SCCellNums cNums = getLeafCellNums(leafCell);
			int numPar = 3;
			while (numPar < keywords.size())
			{
				String parName = keywords.get(numPar);
				if (parName.equalsIgnoreCase("top-active"))
				{
					numPar++;
					if (numPar < keywords.size())
						cNums.topActive = TextUtils.atoi(keywords.get(numPar++));
					continue;
				}
				if (parName.equalsIgnoreCase("bottom-active"))
				{
					numPar++;
					if (numPar < keywords.size())
						cNums.bottomActive = TextUtils.atoi(keywords.get(numPar++));
					continue;
				}
				if (parName.equalsIgnoreCase("left-active"))
				{
					numPar++;
					if (numPar < keywords.size())
						cNums.leftActive = TextUtils.atoi(keywords.get(numPar++));
					continue;
				}
				if (parName.equalsIgnoreCase("right-active"))
				{
					numPar++;
					if (numPar < keywords.size())
						cNums.rightActive = TextUtils.atoi(keywords.get(numPar++));
					continue;
				}
				return "Unknown option '" + parName + "' for SET LEAF-CELL-NUMBERS command";
			}
			setLeafCellNums(leafCell, cNums);
			return null;
		}

		if (whatToSet.equalsIgnoreCase("node-name"))
		{
			// check for sufficient parameters
			if (keywords.size() <= 4) return "Insufficent parameters for SET NODE-NAME command";

			// search for instance
			String instName = keywords.get(2);
			SCNiTree instPtr = findNi(curSCCell, instName);
			if (instPtr == null)
				return "Cannot find instance '" + instName + "' in SET NODE-NAME command";

			// search for port on instance
			String portName = keywords.get(3);
			SCNiPort iPort;
			for (iPort = instPtr.ports; iPort != null; iPort = iPort.next)
			{
				if (instPtr.type == LEAFCELL)
				{
					Export e = (Export)iPort.port;
					if (e.getName().equalsIgnoreCase(portName)) break;
				} else if (instPtr.type == COMPLEXCELL)
				{
					SCPort scp = (SCPort)iPort.port;
					if (scp.name.equalsIgnoreCase(portName)) break;
				}
			}
			if (iPort == null)
				return "Cannot find port '" + portName + "' on instance '" + instName + "' in SET NODE-NAME command";

			// set extracted node name if possible
			if (iPort.extNode == null) return "Cannot find extracted node to set name in SET NODE-NAME command";
			iPort.extNode.name = keywords.get(4);
			return null;
		}

		if (whatToSet.equalsIgnoreCase("port-direction"))
		{
			String cellName = keywords.get(2);
			String portName = keywords.get(3);
			SCCell cell;
			for (cell = scCells; cell != null; cell = cell.next)
			{
				if (cell.name.equalsIgnoreCase(cellName)) break;
			}
			int bits = 0;
			if (cell == null)
			{
				Cell leafCell = findLeafCell(cellName);
				if (leafCell == null)
					return "Cannot find cell '" + cellName + "'";
				Export leafPort = leafCell.findExport(portName);
				if (leafPort  == null) return "Cannot find port '" + portName + "' on cell '" + cellName + "'";
			} else
			{
				SCPort port;
				for (port = cell.ports; port != null; port = port.next)
				{
					if (port.name.equalsIgnoreCase(portName)) break;
				}
				if (port == null)
					return "Cannot find port '" + portName + "' on cell '" + cellName + "'";
				bits = port.bits;
			}
			bits &= ~PORTDIRMASK;
			String dir = keywords.get(4);
			for(int i=0; i<dir.length(); i++)
			{
				char dirCh = dir.charAt(i);
				switch (dirCh)
				{
					case 'u': bits |= PORTDIRUP;   break;
					case 'd': bits |= PORTDIRDOWN;   break;
					case 'r': bits |= PORTDIRRIGHT;   break;
					case 'l': bits |= PORTDIRLEFT;   break;
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
	static SCCellNums getLeafCellNums(Cell leafCell)
	{
		SCCellNums sNums = new SCCellNums();
//		// check if variable exits
//		var = getvalkey((INTBIG)leafCell, VNODEPROTO, VINTEGER|VISARRAY, sc_numskey);
//		if (var != NOVARIABLE)
//		{
//			iarray = (int *)nums;
//			i = sizeof(SCCellNums) / sizeof(int);
//			iarray = (int *)nums;
//			jarray = (int *)var->addr;
//			for (j = 0; j < i; j++) iarray[j] = jarray[j];
//		}
		return sNums;
	}

	/**
	 * Method to set the cell_nums variable for the indicated leaf cell.
	 */
	static void setLeafCellNums(Cell leafCell, SCCellNums nums)
	{
//		VARIABLE	*var;
//		int		i, j, *iarray;
//		INTBIG   *jarray;
//
//		i = sizeof(SCCellNums) / sizeof(int);
//
//		// check if variable exits
//		var = getvalkey((INTBIG)leafCell, VNODEPROTO, VINTEGER|VISARRAY, sc_numskey);
//		if (var == NOVARIABLE)
//		{
//			if ((jarray = emalloc((i + 1) * sizeof(INTBIG), sc_tool->cluster)) == 0)
//				return(Sc_seterrmsg(SC_NOMEMORY));
//			iarray = (int *)nums;
//			for (j = 0; j < i; j++) jarray[j] = iarray[j];
//			jarray[j] = -1;
//			if (setvalkey((INTBIG)leafCell, VNODEPROTO, sc_numskey, (INTBIG)jarray,
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
	private Cell findLeafCell(String name)
	{
		NodeProto np = Cell.findNodeProto(name);
		if (!(np instanceof Cell)) np = null;
		Cell cell = (Cell)np;
		Library lib = Library.findLibrary(SilComp.SCLIBNAME);
		if (cell == null && lib != null)
		{
			cell = lib.findNodeProto(name);
			if (cell == null) return null;
		}
		if (cell != null)
		{
			Cell layCell = cell.otherView(View.LAYOUT);
			if (layCell != null) cell = layCell;
		}
		return cell;
	}

	/**************************************** NETLIST READING: THE EXTRACT KEYWORD ****************************************/

	/**
	 * Method to extract the node netlist for a given cell.
	 */
	private String extract(List keywords)
	{
		if (curSCCell == null) return "No cell selected";
		extractClearFlag(curSCCell);
		curSCCell.exNodes = null;

		extractFindNodes(curSCCell);

		// get ground nodes
		curSCCell.ground = new ExtNode();
		curSCCell.ground.name = "ground";
		curSCCell.ground.flags = 0;
		curSCCell.ground.ptr = null;
		curSCCell.ground.firstPort = null;
		curSCCell.ground.next = null;
		ExtNode oldNList = curSCCell.exNodes;
		for (ExtNode nList = curSCCell.exNodes; nList != null; nList = nList.next)
		{
			ExtPort oldPList = nList.firstPort;
			ExtPort pList;
			for (pList = nList.firstPort; pList != null; pList = pList.next)
			{
				if (pList.node.number == GND)
				{
					curSCCell.ground.firstPort = nList.firstPort;
					if (oldNList == nList)
					{
						curSCCell.exNodes = nList.next;
					} else
					{
						oldNList.next = nList.next;
					}
					if (oldPList == pList)
					{
						curSCCell.ground.firstPort = pList.next;
					} else
					{
						oldPList.next = pList.next;
					}
					break;
				}
				oldPList = pList;
			}
			if (pList != null) break;
			oldNList = nList;
		}
		for (ExtPort pList = curSCCell.ground.firstPort; pList != null; pList = pList.next)
			pList.port.extNode = curSCCell.ground;

		// get power nodes
		curSCCell.power = new ExtNode();
		curSCCell.power.name = "power";
		curSCCell.power.flags = 0;
		curSCCell.power.ptr = null;
		curSCCell.power.firstPort = null;
		curSCCell.power.next = null;
		oldNList = curSCCell.exNodes;
		for (ExtNode nList = curSCCell.exNodes; nList != null; nList = nList.next)
		{
			ExtPort oldPList = nList.firstPort;
			ExtPort pList;
			for (pList = nList.firstPort; pList != null; pList = pList.next)
			{
				if (pList.node.number == PWR)
				{
					curSCCell.power.firstPort = nList.firstPort;
					if (oldNList == nList)
					{
						curSCCell.exNodes = nList.next;
					} else
					{
						oldNList.next = nList.next;
					}
					if (oldPList == pList)
					{
						curSCCell.power.firstPort = pList.next;
					} else
					{
						oldPList.next = pList.next;
					}
					break;
				}
				oldPList = pList;
			}
			if (pList != null) break;
			oldNList = nList;
		}
		for (ExtPort pList = curSCCell.power.firstPort; pList != null; pList = pList.next)
			pList.port.extNode = curSCCell.power;

		extractFindPower(curSCCell, curSCCell);

		extractCollectUnconnected(curSCCell);

		// give the names of the cell ports to the extracted node
		for (SCPort port = curSCCell.ports; port != null; port = port.next)
		{
			switch (port.bits & PORTTYPE)
			{
				case PWRPORT:
				case GNDPORT:
					break;
				default:
					// Note that special nodes only have one niport
					port.node.ports.extNode.name = port.name;
					break;
			}
		}

		// give arbitrary names to unnamed extracted nodes
		int nodenum = 2;
		for (ExtNode ext = curSCCell.exNodes; ext != null; ext = ext.next)
		{
			if (ext.name == null) ext.name = "n" + (nodenum++);
		}
		return null;
	}

	/**
	 * Method to clear the extract pointer on all node instance ports.
	 */
	private void extractClearFlag(SCCell cell)
	{
		for (SCNiTree ntp : cell.niList)
		{
			ntp.flags &= Place.BITS_EXTRACT;
			for (SCNiPort port = ntp.ports; port != null; port = port.next)
				port.extNode = null;
		}
	}

	/**
	 * Method to go though the INSTANCE list, finding all resultant connections.
	 */
	private void extractFindNodes(SCCell cell)
	{
		for (SCNiTree niTree : cell.niList)
		{
			niTree.flags |= Place.BITS_EXTRACT;
			for (ConList cl = niTree.connect; cl != null; cl = cl.next)
			{
				extractSnake(niTree, cl.portA, cl);
			}
		}
	}

	/**
	 * Method to snake through connection list extracting common connections.
	 */
	private void extractSnake(SCNiTree nodeA, SCNiPort portA, ConList cl)
	{
		for ( ; cl != null; cl = cl.next)
		{
			if (cl.portA != portA) continue;
			if (portA != null && portA.extNode != null)
			{
				if (!(cl.portB != null && cl.portB.extNode != null))
				{
					ExtNode common = extractAddNode(portA.extNode, cl.nodeB, cl.portB);
					if ((cl.nodeB.flags & Place.BITS_EXTRACT) == 0)
					{
						cl.nodeB.flags |= Place.BITS_EXTRACT;
						extractSnake(cl.nodeB, cl.portB, cl.nodeB.connect);
						cl.nodeB.flags ^= Place.BITS_EXTRACT;
					}
				}
			} else
			{
				if (cl.portB != null && cl.portB.extNode != null)
				{
					ExtNode common = extractAddNode(cl.portB.extNode, nodeA, portA);
				} else
				{
					ExtNode common = extractAddNode(null, nodeA, portA);
					common = extractAddNode(common, cl.nodeB, cl.portB);
					if ((cl.nodeB.flags & Place.BITS_EXTRACT) == 0)
					{
						cl.nodeB.flags |= Place.BITS_EXTRACT;
						extractSnake(cl.nodeB, cl.portB, cl.nodeB.connect);
						cl.nodeB.flags ^= Place.BITS_EXTRACT;
					}
				}
			}
		}
	}

	/**
	 * Method to add a node and port to a ExtNode list.
	 * Modify the root if necessary.
	 */
	private ExtNode extractAddNode(ExtNode simNode, SCNiTree node, SCNiPort port)
	{
		ExtPort newPort = new ExtPort();
		if (simNode == null)
		{
			simNode = new ExtNode();
			simNode.firstPort = newPort;
			simNode.flags = 0;
			simNode.ptr = null;
			simNode.name = null;
			newPort.node = node;
			newPort.port = port;
			if (port != null)
				port.extNode = simNode;
			newPort.next = null;
			simNode.next = curSCCell.exNodes;
			curSCCell.exNodes = simNode;
		} else
		{
			newPort.node = node;
			newPort.port = port;
			if (port != null)
				port.extNode = simNode;
			newPort.next = simNode.firstPort;
			simNode.firstPort = newPort;
		}
		return simNode;
	}

	/**
	 * Method to find the implicit power and ground ports.
	 * Does a search of the instance tree and adds to the appropriate port list.
	 * Skips over the dummy ground and power instances and special cells.
	 */
	private void extractFindPower(SCCell cell, SCCell vars)
	{
		for (SCNiTree ntp : cell.niList)
		{
			// process node
			if (ntp.number > PWR)
			{
				switch (ntp.type)
				{
					case COMPLEXCELL:
						break;
					case SPECIALCELL:
						break;
					case LEAFCELL:
						for (SCNiPort port = ntp.ground; port != null; port = port.next)
						{
							ExtPort pList = new ExtPort();
							pList.node = ntp;
							pList.port = port;
							port.extNode = vars.ground;
							pList.next = vars.ground.firstPort;
							vars.ground.firstPort = pList;
						}
						for (SCNiPort port = ntp.power; port != null; port = port.next)
						{
							ExtPort pList = new ExtPort();
							pList.node = ntp;
							pList.port = port;
							port.extNode = vars.power;
							pList.next = vars.power.firstPort;
							vars.power.firstPort = pList;
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
	private String pull()
	{
		// check if a cell is currently selected
		if (curSCCell == null) return "No cell selected";

		// remember the original ones and delete them later
		List<SCNiTree> cellList = new ArrayList<SCNiTree>();
		for (SCNiTree inst : curSCCell.niList)
		{
			if (inst.type == COMPLEXCELL)
				cellList.add(inst);
		}

		// expand all instances of complex cell type
		for (SCNiTree inst : cellList)
		{
			String err = pullInst(inst, curSCCell);
			if (err != null) return err;
		}

		// now remove the original ones
		List<SCNiTree> deleteList = new ArrayList<SCNiTree>();
		for (SCNiTree inst : curSCCell.niList)
		{
			if (inst.type == COMPLEXCELL)
				deleteList.add(inst);
		}
		for (SCNiTree inst : deleteList)
		{
			curSCCell.niList.remove(inst);
		}
		return null;
	}

	/**
	 * Method to pull the indicated instance of a complex cell into the indicated parent cell.
	 * @param inst instance to be pulled up.
	 * @param cell parent cell.
	 */
	private String pullInst(SCNiTree inst, SCCell cell)
	{
		SCCell subCell = (SCCell)inst.np;

		// first create components
		for (SCNiTree subInst : subCell.niList)
		{
			if (subInst.type != SPECIALCELL)
			{
				List<String> createPars = new ArrayList<String>();
				createPars.add("create");
				createPars.add("instance");
				createPars.add(inst.name + "." + subInst.name);
				if (subInst.type == LEAFCELL)
				{
					createPars.add(((Cell)subInst.np).getName());
				} else
				{
					createPars.add(((SCCell)subInst.np).name);
				}
				String err = create(createPars);
				if (err != null) return err;
			}
		}

		// create connections among these subinstances by using the
		// subCell's extracted node list.  Also resolve connections
		// to the parent cell instances by using exported port info.
		for (ExtNode eNode = subCell.exNodes; eNode != null; eNode = eNode.next)
		{
			ExtNode bNode = null;

			// check if the extracted node is an exported node
			for (SCNiPort iPort = inst.ports; iPort != null; iPort = iPort.next)
			{
				if (((SCPort)(iPort.port)).node.ports.extNode == eNode)
				{
					bNode = iPort.extNode;
					break;
				}
			}
			if (bNode == null)
			{
				// this is a new internal node
				bNode = new ExtNode();
				bNode.name = null;
				bNode.firstPort = null;
				bNode.ptr = null;
				bNode.flags = 0;
				bNode.next = cell.exNodes;
				cell.exNodes = bNode;
			}

			// add ports to extracted node bNode
			for (ExtPort ePort = eNode.firstPort; ePort != null; ePort = ePort.next)
			{
				// only add leaf cells or complex cells
				if (ePort.node.type != SPECIALCELL)
				{
					ExtPort nport = new ExtPort();
					nport.node = findNi(cell, inst.name + "." + ePort.node.name);

					// add reference to extracted node to instance port list
					for (SCNiPort iPort = nport.node.ports; iPort != null; iPort = iPort.next)
					{
						if (iPort.port == ePort.port.port)
						{
							nport.port = iPort;
							iPort.extNode = bNode;
							nport.next = bNode.firstPort;
							bNode.firstPort = nport;
							break;
						}
					}
				}
			}
		}

		// add power ports for new instances
		for (ExtPort ePort = subCell.power.firstPort; ePort != null; ePort = ePort.next)
		{
			if (ePort.node.type == SPECIALCELL) continue;
			ExtPort nport = new ExtPort();
			nport.node = findNi(cell, inst.name + "." + ePort.node.name);

			// add reference to extracted node to instance port list
			SCNiPort iPort;
			for (iPort = nport.node.ports; iPort != null; iPort = iPort.next)
			{
				if (iPort.port == ePort.port.port)
				{
					nport.port = iPort;
					iPort.extNode = cell.power;
					break;
				}
			}
			if (iPort == null)
			{
				for (iPort = nport.node.power; iPort != null; iPort = iPort.next)
				{
					if (iPort.port == ePort.port.port)
					{
						nport.port = iPort;
						iPort.extNode = cell.power;
						break;
					}
				}
			}
			nport.next = cell.power.firstPort;
			cell.power.firstPort = nport;
		}

		// remove references to original instance in power list
		ExtPort nPort = cell.power.firstPort;
		for (ExtPort ePort = cell.power.firstPort; ePort != null; ePort = ePort.next)
		{
			if (ePort.node == inst)
			{
				if (ePort == nPort)
				{
					cell.power.firstPort = ePort.next;
					nPort = ePort.next;
				} else
				{
					nPort.next = ePort.next;
				}
			} else
			{
				nPort = ePort;
			}
		}

		// add ground ports
		for (ExtPort ePort = subCell.ground.firstPort; ePort != null; ePort = ePort.next)
		{
			if (ePort.node.type == SPECIALCELL) continue;
			nPort = new ExtPort();
			nPort.node = findNi(cell, inst.name + "." + ePort.node.name);

			// add reference to extracted node to instance port list
			SCNiPort iPort;
			for (iPort = nPort.node.ports; iPort != null; iPort = iPort.next)
			{
				if (iPort.port == ePort.port.port)
				{
					nPort.port = iPort;
					iPort.extNode = cell.ground;
					break;
				}
			}
			if (iPort == null)
			{
				for (iPort = nPort.node.ground; iPort != null; iPort = iPort.next)
				{
					if (iPort.port == ePort.port.port)
					{
						nPort.port = iPort;
						iPort.extNode = cell.ground;
						break;
					}
				}
			}
			nPort.next = cell.ground.firstPort;
			cell.ground.firstPort = nPort;
		}

		// remove references to original instance in ground list
		nPort = cell.ground.firstPort;
		for (ExtPort ePort = cell.ground.firstPort; ePort != null; ePort = ePort.next)
		{
			if (ePort.node == inst)
			{
				if (ePort == nPort)
				{
					cell.ground.firstPort = ePort.next;
					nPort = ePort.next;
				} else
				{
					nPort.next = ePort.next;
				}
			} else
			{
				nPort = ePort;
			}
		}

		// remove references to instance in exported node list
		for (ExtNode eNode = cell.exNodes; eNode != null; eNode = eNode.next)
		{
			nPort = eNode.firstPort;
			for (ExtPort ePort = eNode.firstPort; ePort != null; ePort = ePort.next)
			{
				if (ePort.node == inst)
				{
					if (ePort == nPort)
					{
						eNode.firstPort = ePort.next;
						nPort = ePort.next;
					} else
					{
						nPort.next = ePort.next;
					}
				} else
				{
					nPort = ePort;
				}
			}
		}

		// find the value of the largest generically named extracted node
		int oldNum = 0;
		for (ExtNode eNode = cell.exNodes; eNode != null; eNode = eNode.next)
		{
			if (eNode.name != null)
			{
				int sPtr = 0;
				char firstCh = eNode.name.charAt(0);
				if (Character.toUpperCase(firstCh) == 'N')
				{
					sPtr++;
					while (sPtr < eNode.name.length())
					{
						if (!Character.isDigit(eNode.name.charAt(sPtr))) break;
						sPtr++;
					}
					if (sPtr >= eNode.name.length())
					{
						int newnum = TextUtils.atoi(eNode.name.substring(1));
						if (newnum > oldNum)
							oldNum = newnum;
					}
				}
			}
		}

		// set the name of any unnamed nodes
		for (ExtNode eNode = cell.exNodes; eNode != null; eNode = eNode.next)
		{
			if (eNode.name == null)
			{
				eNode.name = "n" + (++oldNum);
			}
		}

		// flatten any subinstances which are also complex cells
		for (SCNiTree subInst : subCell.niList)
		{
			if (subInst.type == COMPLEXCELL)
			{
				SCNiTree ninst = findNi(cell, inst.name + "." + subInst.name);
				String err = pullInst(ninst, cell);
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
//	void Sc_extract_print_nodes(SCCell *vars)
//	{
//		int		i;
//		ExtNode	*simNode;
//		ExtPort	*pList;
//		CHAR	*portname;
//
//		i = 0;
//		if (vars->ground)
//		{
//			ttyputmsg(M_("Node %d  %s:"), i, vars->ground->name);
//			for (pList = vars->ground->firstPort; pList; pList = pList->next)
//			{
//				switch (pList->node->type)
//				{
//					case SPECIALCELL:
//						portname = M_("Special");
//						break;
//					case COMPLEXCELL:
//						portname = ((SCPort *)(pList->port->port))->name;
//						break;
//					case LEAFCELL:
//						portname = Sc_leaf_port_name(pList->port->port);
//						break;
//					default:
//						portname = M_("Unknown");
//						break;
//				}
//				ttyputmsg(x_("    %-20s    %s"), pList->node->name, portname);
//			}
//		}
//		i++;
//
//		if (vars->power)
//		{
//			ttyputmsg(M_("Node %d  %s:"), i, vars->power->name);
//			for (pList = vars->power->firstPort; pList; pList = pList->next)
//			{
//				switch (pList->node->type)
//				{
//					case SPECIALCELL:
//						portname = M_("Special");
//						break;
//					case COMPLEXCELL:
//						portname = ((SCPort *)(pList->port->port))->name;
//						break;
//					case LEAFCELL:
//						portname = Sc_leaf_port_name(pList->port->port);
//						break;
//					default:
//						portname = M_("Unknown");
//						break;
//				}
//				ttyputmsg(x_("    %-20s    %s"), pList->node->name, portname);
//			}
//		}
//		i++;
//
//		for (simNode = vars->exNodes; simNode; simNode = simNode->next)
//		{
//			ttyputmsg(M_("Node %d  %s:"), i, simNode->name);
//			for (pList = simNode->firstPort; pList; pList = pList->next)
//			{
//				switch (pList->node->type)
//				{
//					case SPECIALCELL:
//						portname = M_("Special");
//						break;
//					case COMPLEXCELL:
//						portname = ((SCPort *)(pList->port->port))->name;
//						break;
//					case LEAFCELL:
//						portname = Sc_leaf_port_name(pList->port->port);
//						break;
//					default:
//						portname = M_("Unknown");
//						break;
//				}
//				ttyputmsg(x_("    %-20s    %s"), pList->node->name, portname);
//			}
//			i++;
//		}
//	}
//

	/**
	 * Method to collect the unconnected ports and create an extracted node for each.
	 */
	private void extractCollectUnconnected(SCCell cell)
	{
		for (SCNiTree nPtr : cell.niList)
		{
			// process node
			switch (nPtr.type)
			{
				case COMPLEXCELL:
				case LEAFCELL:
					for (SCNiPort port = nPtr.ports; port != null; port = port.next)
					{
						if (port.extNode == null)
						{
							ExtNode ext = new ExtNode();
							ext.name = null;
							ExtPort ePort = new ExtPort();
							ePort.node = nPtr;
							ePort.port = port;
							ePort.next = null;
							ext.firstPort = ePort;
							ext.flags = 0;
							ext.ptr = null;
							ext.next = cell.exNodes;
							cell.exNodes = ext;
							port.extNode = ext;
						}
					}
					break;
				default:
					break;
			}
		}
	}

	/**
	 * Method to return the type of the leaf port.
	 * @param leafPort pointer to leaf port.
	 * @return type of port.
	 */
	static int getLeafPortType(Export leafPort)
	{
		if (leafPort.isPower()) return PWRPORT;
		if (leafPort.isGround()) return GNDPORT;
		if (leafPort.getCharacteristic() == PortCharacteristic.BIDIR) return BIDIRPORT;
		if (leafPort.getCharacteristic() == PortCharacteristic.OUT) return OUTPORT;
		if (leafPort.getCharacteristic() == PortCharacteristic.IN) return INPORT;
		return UNPORT;
	}

	/**
	 * Method to return the directions that a port can be attached to.
	 * Values can be up, down, left, right.
	 */
	static int getLeafPortDirection(PortProto port)
	{
		return PORTDIRUP | PORTDIRDOWN;
	}

}
