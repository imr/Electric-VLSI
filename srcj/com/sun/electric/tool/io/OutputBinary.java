/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OutputBinary.java
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
package com.sun.electric.tool.io;

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.InputBinary;
import com.sun.electric.tool.io.BinaryConstants;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


/**
 * This class writes files in binary (.elib) format.
 */
public class OutputBinary extends Output
{

	/** cell flag for finding external cell refernces */		private FlagSet externalRefFlag;

	/** all of the names used in variables */					private static HashMap varNames;

	OutputBinary()
	{
	}

	// ----------------------- public methods -------------------------------

	/**
	 * Routine to write a Library in binary (.elib) format.
	 * @param lib the Library to be written.
	 */
	protected boolean writeLib(Library lib)
	{
		try
		{
			return writeTheLibrary(lib);
		} catch (IOException e)
		{
			System.out.println("End of file reached while writing " + filePath);
			return true;
		}
	}

	/**
	 * Routine to write the .elib file.
	 * Returns true on error.
	 */
	private boolean writeTheLibrary(Library lib)
		throws IOException
	{
		writeBigInteger(InputBinary.MAGIC12);
		writeByte((byte)2);		// size of Short
		writeByte((byte)4);		// size of Int
		writeByte((byte)1);		// size of Char

		// write the number of tools
		int toolCount = Tool.getNumTools();
		writeBigInteger(toolCount);
		int techCount = Technology.getNumTechnologies();
		writeBigInteger(techCount);

		// convert cellGroups to "next in cell group" pointers
		FlagSet cellFlag = NodeProto.getFlagSet(1);
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			cell.clearBit(cellFlag);
			cell.setTempObj(null);
		}
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			if (cell.isBit(cellFlag)) continue;

			// mark the group with "next" pointers
			Cell firstCellInGroup = null;
			Cell lastCellInGroup = null;
			for(Iterator git = cell.getCellGroup().getCells(); git.hasNext(); )
			{
				Cell cellInGroup = (Cell)git.next();
				if (lastCellInGroup == null) firstCellInGroup = cellInGroup; else
				{
					lastCellInGroup.setTempObj(cellInGroup);
				}
				cellInGroup.setBit(cellFlag);
				lastCellInGroup = cellInGroup;
			}
			if (lastCellInGroup == null || firstCellInGroup == null)
				lastCellInGroup = firstCellInGroup = cell;
			lastCellInGroup.setTempObj(firstCellInGroup);
		}
		cellFlag.freeFlagSet();

		// initialize the number of objects in the database
		int nodeIndex = 0;
		int portProtoIndex = 0;
		int nodeProtoIndex = 0;
		int arcIndex = 0;
		int primNodeProtoIndex = 0;
		int primPortProtoIndex = 0;
		int arcProtoIndex = 0;

		// count and number the cells, nodes, arcs, and ports in this library
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			cell.setTempInt(nodeProtoIndex++);
			for(Iterator pit = cell.getPorts(); pit.hasNext(); )
			{
				Export pp = (Export)pit.next();
				pp.setTempInt(portProtoIndex++);
			}
			for(Iterator ait = cell.getArcs(); ait.hasNext(); )
			{
				ArcInst ai = (ArcInst)ait.next();
				ai.setTempInt(arcIndex++);
			}
			for(Iterator nit = cell.getNodes(); nit.hasNext(); )
			{
				NodeInst ni = (NodeInst)nit.next();
				ni.setTempInt(nodeIndex++);
			}
		}
		int cellsHere = nodeProtoIndex;

		// prepare to locate references to cells in other libraries
		FlagSet externalRefFlag = NodeProto.getFlagSet(1);
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library olib = (Library)it.next();
			for(Iterator cit = olib.getCells(); cit.hasNext(); )
			{
				Cell cell = (Cell)cit.next();
				cell.clearBit(externalRefFlag);
			}
		}

		// scan for all cross-library references
		varNames = new HashMap();
		findXLibVariables(lib);
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			findXLibVariables(tool);
		}
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			findXLibVariables(tech);
			for(Iterator ait = tech.getArcs(); ait.hasNext(); )
			{
				ArcProto ap = (ArcProto)ait.next();
				findXLibVariables(ap);
			}
			for(Iterator nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)nit.next();
				findXLibVariables(np);
				for(Iterator eit = np.getPorts(); eit.hasNext(); )
				{
					PrimitivePort pp = (PrimitivePort)eit.next();
					findXLibVariables(pp);
				}
			}
		}
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View view = (View)it.next();
			findXLibVariables(view);
		}
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			findXLibVariables(cell);
			for(Iterator eit = cell.getPorts(); eit.hasNext(); )
			{
				Export pp = (Export)eit.next();
				findXLibVariables(pp);
			}
			for(Iterator nit = cell.getNodes(); nit.hasNext(); )
			{
				NodeInst ni = (NodeInst)nit.next();
				findXLibVariables(ni);
// 				for(Iterator cit = ni.getConnections(); cit.hasNext(); )
// 				{
// 					Connection con = (Connection)cit.next();
// 					findXLibVariables(con);
// 				}
				for(Iterator eit = ni.getExports(); eit.hasNext(); )
				{
					Export pp = (Export)eit.next();
					findXLibVariables(pp);
				}
				if (ni.getProto() instanceof Cell)
					ni.getProto().setBit(externalRefFlag);
			}
			for(Iterator ait = cell.getArcs(); ait.hasNext(); )
			{
				ArcInst ai = (ArcInst)ait.next();
				findXLibVariables(ai);
			}
		}

		// count and number the cells in other libraries
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library olib = (Library)it.next();
			if (olib == lib) continue;
			for(Iterator cit = olib.getCells(); cit.hasNext(); )
			{
				Cell cell = (Cell)cit.next();
				if (!cell.isBit(externalRefFlag)) continue;
				cell.setTempInt(nodeProtoIndex++);
				for(Iterator eit = cell.getPorts(); eit.hasNext(); )
				{
					Export pp = (Export)eit.next();
					pp.setTempInt(portProtoIndex++);
				}
			}
		}

		// count and number the primitive node and port prototypes
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)nit.next();
				np.setTempInt(-2 - primNodeProtoIndex++);
				for(Iterator eit = np.getPorts(); eit.hasNext(); )
				{
					PrimitivePort pp = (PrimitivePort)eit.next();
					pp.setTempInt(-2 - primPortProtoIndex++);
				}
			}
			for(Iterator ait = tech.getArcs(); ait.hasNext(); )
			{
				ArcProto ap = (ArcProto)ait.next();
				ap.setTempInt(-2 - arcProtoIndex++);
			}
		}

		// write number of objects
		writeBigInteger(primNodeProtoIndex);
		writeBigInteger(primPortProtoIndex);
		writeBigInteger(arcProtoIndex);
		writeBigInteger(nodeProtoIndex);
		writeBigInteger(nodeIndex);
		writeBigInteger(portProtoIndex);
		writeBigInteger(arcIndex);
		writeBigInteger(0);

		// write the current cell
		int curNodeProto = -1;
		if (lib.getCurCell() != null)
			curNodeProto = lib.getCurCell().getTempInt();
		writeBigInteger(curNodeProto);

		// write the version number
		writeString(Version.CURRENT);

		// number the views and write nonstandard ones
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View view = (View)it.next();
			view.setTempInt(0);
		}
		View.UNKNOWN.setTempInt(-1);
		View.LAYOUT.setTempInt(-2);
		View.SCHEMATIC.setTempInt(-3);
		View.ICON.setTempInt(-4);
		View.DOCWAVE.setTempInt(-5);
		View.LAYOUTSKEL.setTempInt(-6);
		View.VHDL.setTempInt(-7);
		View.NETLIST.setTempInt(-8);
		View.DOC.setTempInt(-9);
		View.NETLISTNETLISP.setTempInt(-10);
		View.NETLISTALS.setTempInt(-11);
		View.NETLISTQUISC.setTempInt(-12);
		View.NETLISTRSIM.setTempInt(-13);
		View.NETLISTSILOS.setTempInt(-14);
		View.VERILOG.setTempInt(-15);
//		View.LAYOUTCOMP.setTempInt(-16);
		int i = 1;
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View view = (View)it.next();
			if (view.getTempInt() == 0) view.setTempInt(i++);
		}
		i--;
		writeBigInteger(i);
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View view = (View)it.next();
			if (view.getTempInt() < 0) continue;
			writeString(view.getFullName());
			writeString(view.getAbbreviation());
		}

		// write total number of arcinsts, nodeinsts, and ports in each cell
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			writeBigInteger(cell.getNumArcs());
			writeBigInteger(cell.getNumNodes());
			writeBigInteger(cell.getNumPorts());
		}

		// write dummy numbers of arcinsts and nodeinst; count ports for external cells
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library olib = (Library)it.next();
			if (olib == lib) continue;
			for(Iterator cit = olib.getCells(); cit.hasNext(); )
			{
				Cell cell = (Cell)cit.next();
				if (!cell.isBit(externalRefFlag)) continue;
				writeBigInteger(-1);
				writeBigInteger(-1);
				writeBigInteger(cell.getNumPorts());
			}
		}

		// write the names of technologies and primitive prototypes
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();

			// write the technology name
			writeString(tech.getTechName());

			// write the primitive node prototypes
			writeBigInteger(tech.getNumNodes());
			for(Iterator nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)nit.next();

				// write the primitive node prototype name
				writeString(np.getProtoName());
				writeBigInteger(np.getNumPorts());
				for(Iterator pit = np.getPorts(); pit.hasNext(); )
				{
					PrimitivePort pp = (PrimitivePort)pit.next();
					writeString(pp.getProtoName());
				}
			}

			// write the primitive arc prototype names
			writeBigInteger(tech.getNumArcs());
			for(Iterator ait = tech.getArcs(); ait.hasNext(); )
			{
				PrimitiveArc ap = (PrimitiveArc)ait.next();
				writeString(ap.getProtoName());
			}
		}

		// write the names of the tools
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			writeString(tool.getName());
		}

		// write the userbits for the library
		writeBigInteger(lib.lowLevelGetUserBits());

		// write the tool lambda values
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			writeBigInteger((int)tech.getScale());
		}

		// write the global namespace
		writeNameSpace();

		// write the library variables
		writeVariables(lib, 0);

		// write the tool variables
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			writeVariables(tool, 0);
		}

		// write the variables on technologies
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			writeVariables(tech, 0);
		}

		// write the arcproto variables
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator ait = tech.getArcs(); ait.hasNext(); )
			{
				PrimitiveArc ap = (PrimitiveArc)ait.next();
				writeVariables(ap, 0);
			}
		}

		// write the variables on primitive node prototypes
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)nit.next();
				writeVariables(np, 0);
			}
		}

		// write the variables on primitive port prototypes
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)nit.next();
				for(Iterator pit = np.getPorts(); pit.hasNext(); )
				{
					PrimitivePort pp = (PrimitivePort)pit.next();
					writeVariables(pp, 0);
				}
			}
		}

		// write the view variables
		writeBigInteger(View.getNumViews());
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View view = (View)it.next();
			writeBigInteger(view.getTempInt());
			writeVariables(view, 0);
		}

		// write all of the cells in this library
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			writeNodeProto(cell, true);
		}

		// write all of the cells in external libraries
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library olib = (Library)it.next();
			if (olib == lib) continue;
			for(Iterator cit = olib.getCells(); cit.hasNext(); )
			{
				Cell cell = (Cell)cit.next();
				if (!cell.isBit(externalRefFlag)) continue;
				writeNodeProto(cell, false);
			}
		}

		// write all of the arcs and nodes in this library
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			for(Iterator ait = cell.getArcs(); ait.hasNext(); )
			{
				ArcInst ai = (ArcInst)ait.next();
				writeArcInst(ai);
			}
			for(Iterator nit = cell.getNodes(); nit.hasNext(); )
			{
				NodeInst ni = (NodeInst)nit.next();
				writeNodeInst(ni);
			}
		}

		if (!lib.isHidden())
		{
			System.out.println(filePath + " written (" + cellsHere + " cells)");
		}
		lib.clearChangedMinor();
		lib.clearChangedMajor();
		lib.setFromDisk();
		externalRefFlag.freeFlagSet();

		// library written successfully
		return false;
	}

	// --------------------------------- OBJECT CONVERSION ---------------------------------

	private void writeNodeProto(Cell cell, boolean thislib)
		throws IOException
	{
		// write cell information
		writeString(cell.getProtoName());

		// write the "next in cell group" pointer
		int nextGrp = -1;
		Object obj = cell.getTempObj();
		if (obj != null && obj instanceof Cell)
		{
			Cell nextInGroup = (Cell)obj;
			nextGrp = nextInGroup.getTempInt();
		}
		writeBigInteger(nextGrp);

		// write the "next in continuation" pointer
		int nextCont = -1;
		writeBigInteger(nextCont);
		writeBigInteger(cell.getView().getTempInt());
		writeBigInteger(cell.getVersion());
		writeBigInteger((int)BinaryConstants.toElectricDate(cell.getCreationDate()));
		writeBigInteger((int)BinaryConstants.toElectricDate(cell.getRevisionDate()));

		// write the nodeproto bounding box
		Technology tech = cell.getTechnology();
		Rectangle2D bounds = cell.getBounds();
		int lowX = (int)(bounds.getMinX() * tech.getScale());
		int highX = (int)(bounds.getMaxX() * tech.getScale());
		int lowY = (int)(bounds.getMinY() * tech.getScale());
		int highY = (int)(bounds.getMaxY() * tech.getScale());
		writeBigInteger(lowX);
		writeBigInteger(highX);
		writeBigInteger(lowY);
		writeBigInteger(highY);

		if (!thislib)
		{
			Library instlib = cell.getLibrary();
			writeString(instlib.getLibFile());
		}

		// write the number of portprotos on this nodeproto
		writeBigInteger(cell.getNumPorts());

		// write the portprotos on this nodeproto
		for(Iterator it = cell.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			if (thislib)
			{
				// write the connecting subnodeinst for this portproto
				int i = -1;
				PortInst pi = pp.getOriginalPort();
				if (pi != null)
				{
					i = pi.getNodeInst().getTempInt();
				} else
				{
					System.out.println("ERROR: cell " + cell.describe() + " export " + pp.getProtoName() + " has no subnode");
				}
				writeBigInteger(i);

				// write the portproto index in the subnodeinst
				i = -1;
				if (pi != null)
				{
					i = pi.getPortProto().getTempInt();
				} else
				{
					System.out.println("ERROR: cell " + cell.describe() + " export " + pp.getProtoName() + " has no subport");
				}
				writeBigInteger(i);
			}

			// write the portproto name
			writeString(pp.getProtoName());

			if (thislib)
			{
				// write the text descriptor
				TextDescriptor td = pp.getTextDescriptor();
				writeBigInteger(td.lowLevelGet0());
				writeBigInteger(td.lowLevelGet1());

				// write the portproto tool information
				writeBigInteger(pp.lowLevelGetUserbits());

				// write variable information
				writeVariables(pp, 0);
			}
		}

		if (thislib)
		{
			// write tool information
			writeBigInteger(0);		// was "adirty"
			writeBigInteger(cell.lowLevelGetUserbits());

			// write variable information
			writeVariables(cell, 0);
		}
	}

	private void writeNodeInst(NodeInst ni)
		throws IOException
	{
		// write the nodeproto pointer
		writeBigInteger(ni.getProto().getTempInt());

		// write descriptive information
		Technology tech = ni.getParent().getTechnology();
		int lowX = (int)((ni.getCenterX() - ni.getXSize()/2) * tech.getScale());
		int highX = (int)((ni.getCenterX() + ni.getXSize()/2) * tech.getScale());
		int lowY = (int)((ni.getCenterY() - ni.getYSize()/2) * tech.getScale());
		int highY = (int)((ni.getCenterY() + ni.getYSize()/2) * tech.getScale());
		writeBigInteger(lowX);
		writeBigInteger(lowY);
		writeBigInteger(highX);
		writeBigInteger(highY);
		int transpose = 0;
		int rotation = ni.getAngle();
		if (ni.isXMirrored())
		{
			if (ni.isYMirrored()) rotation = (rotation + 1800) % 3600; else
			{
				rotation = (rotation + 900) % 3600;
				transpose = 1 - transpose;
			}
		} else if (ni.isYMirrored())
		{
			rotation = (rotation + 2700) % 3600;
			transpose = 1 - transpose;
		}
		writeBigInteger(transpose);
		writeBigInteger(rotation);

		TextDescriptor td = ni.getProtoTextDescriptor();
		writeBigInteger(td.lowLevelGet0());
		writeBigInteger(td.lowLevelGet1());

		// sort the arc connections by their PortInst ordering
		int numConnections = ni.getNumConnections();
		writeBigInteger(numConnections);
		if (numConnections > 0)
		{
			// must write connections in proper order
			List sortedList = new ArrayList();
			for(Iterator it = ni.getConnections(); it.hasNext(); )
				sortedList.add(it.next());
			Collections.sort(sortedList, new OrderedConnections());

			for(Iterator it = sortedList.iterator(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				ArcInst ai = con.getArc();
				int i = ai.getTempInt() << 1;
				if (ai.getHead() == con) i += 0; else i += 1;
				writeBigInteger(i);

				// write the portinst prototype
				PortInst pi = con.getPortInst();
				int protoIndex = pi.getPortProto().getTempInt();
				writeBigInteger(protoIndex);

				// write the variable information
				writeNoVariables();
			}
		}

		// write the exports
		int numExports = ni.getNumExports();
		writeBigInteger(numExports);
		if (numExports > 0)
		{
			// must write exports in proper order
			List sortedList = new ArrayList();
			for(Iterator it = ni.getExports(); it.hasNext(); )
				sortedList.add(it.next());
			Collections.sort(sortedList, new OrderedExports());

			for(Iterator it = sortedList.iterator(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				writeBigInteger(pp.getTempInt());

				// write the portinst prototype
				writeBigInteger(pp.getOriginalPort().getPortProto().getTempInt());

				// write the variable information
				writeNoVariables();
			}
		}

		// write the tool information
		writeBigInteger(ni.lowLevelGetUserbits());

		// write variable information
		writeVariables(ni, tech.getScale());
	}

	static class OrderedConnections implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Connection c1 = (Connection)o1;
			Connection c2 = (Connection)o2;
			int i1 = c1.getPortInst().getPortProto().getPortIndex();
			int i2 = c2.getPortInst().getPortProto().getPortIndex();
			return i1 - i2;
		}
	}

	static class OrderedExports implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Export e1 = (Export)o1;
			Export e2 = (Export)o2;
			int i1 = e1.getOriginalPort().getPortProto().getPortIndex();
			int i2 = e2.getOriginalPort().getPortProto().getPortIndex();
			return i1 - i2;
		}
	}

	private void writeArcInst(ArcInst ai)
		throws IOException
	{
		// write the arcproto pointer
		writeBigInteger(ai.getProto().getTempInt());

		// write basic arcinst information
		Technology tech = ai.getParent().getTechnology();
		writeBigInteger((int)(ai.getWidth() * tech.getScale()));

		// write the arcinst head information
		Point2D location = ai.getHead().getLocation();
		writeBigInteger((int)(location.getX() * tech.getScale()));
		writeBigInteger((int)(location.getY() * tech.getScale()));
		writeBigInteger(ai.getHead().getPortInst().getNodeInst().getTempInt());

		// write the arcinst tail information
		location = ai.getTail().getLocation();
		writeBigInteger((int)(location.getX() * tech.getScale()));
		writeBigInteger((int)(location.getY() * tech.getScale()));
		writeBigInteger(ai.getTail().getPortInst().getNodeInst().getTempInt());

		// write the arcinst's tool information
		int arcAngle = ai.getAngle() / 10;
		ai.lowLevelSetArcAngle(arcAngle);
		int userBits = ai.lowLevelGetUserbits();
		writeBigInteger(userBits);

		// write variable information
		writeVariables(ai, 0);
	}

	// --------------------------------- VARIABLES ---------------------------------

	/**
	 * routine to write the global namespace.  returns true upon error
	 */
	private boolean writeNameSpace()
		throws IOException
	{
		int numVariableNames = ElectricObject.getNumVariableKeys();
		writeBigInteger(numVariableNames);

		Variable.Key [] nameList = new Variable.Key[numVariableNames];
		for(Iterator it = ElectricObject.getVariableKeys(); it.hasNext(); )
		{
			Variable.Key key = (Variable.Key)it.next();
			nameList[key.getIndex()] = key;
		}
		for(int i=0; i<numVariableNames; i++)
		{
			Variable.Key key = nameList[i];
			if (key == null) writeString(""); else
				writeString(key.getName());
		}
		return false;
	}

	/**
	 * routine to write an empty set of variables.
	 */
	private void writeNoVariables()
		throws IOException
	{
		writeBigInteger(0);
	}

	/**
	 * routine to write a set of object variables.  returns negative upon error and
	 * otherwise returns the number of variables write
	 */
	private int writeVariables(ElectricObject obj, double scale)
		throws IOException
	{
		int count = 0;
		if (obj instanceof Geometric && ((Geometric)obj).getNameKey() != null)
			count++;
		for(Iterator it = obj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.isDontSave()) count++;
		}
		writeBigInteger(count);
		if (obj instanceof Geometric && ((Geometric)obj).getNameKey() != null)
		{
			Geometric geom = (Geometric)obj;
			Variable.Key key = geom instanceof NodeInst ? NodeInst.NODE_NAME : ArcInst.ARC_NAME;
			writeSmallInteger((short)key.getIndex());
			int type = BinaryConstants.VSTRING;
			if (geom.isUsernamed()) type |= BinaryConstants.VDISPLAY;
			writeBigInteger(type);

			// write the text descriptor of name
			writeBigInteger(geom.getNameTextDescriptor().lowLevelGet0());
			writeBigInteger(geom.getNameTextDescriptor().lowLevelGet1());
			putOutVar(geom.getName());
		}
		for(Iterator it = obj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.isDontSave()) continue;
			Variable.Key key = var.getKey();
			short index = (short)key.getIndex();
			writeSmallInteger(index);

			// create the "type" field
			Object varObj = var.getObject();
			int type = var.lowLevelGetFlags() & ~(BinaryConstants.VTYPE|BinaryConstants.VISARRAY|BinaryConstants.VLENGTH);
			if (varObj instanceof Object[])
			{
				Object [] objList = (Object [])varObj;
				type |= getVarType(objList[0]) | BinaryConstants.VISARRAY | (objList.length << BinaryConstants.VLENGTHSH);
			} else
			{
				type |= getVarType(varObj);
			}
			writeBigInteger(type);

			// write the text descriptor
			TextDescriptor td = var.getTextDescriptor();
			writeBigInteger(td.lowLevelGet0());
			writeBigInteger(td.lowLevelGet1());

			if (varObj instanceof Object[])
			{
				int len = ((Object[])varObj).length;
				writeBigInteger(len);

				for(int i=0; i<len; i++)
				{
					Object oneObj = ((Object[])varObj)[i];
					putOutVar(oneObj);
				}
			} else
			{
				putOutVar(varObj);
			}
		}
		return(count);
	}

	private int getVarType(Object obj)
	{
		if (obj instanceof Integer) return BinaryConstants.VINTEGER;
		if (obj instanceof Short) return BinaryConstants.VSHORT;
		if (obj instanceof Byte) return BinaryConstants.VCHAR;
		if (obj instanceof String) return BinaryConstants.VSTRING;
		if (obj instanceof Float) return BinaryConstants.VFLOAT;
		if (obj instanceof Double) return BinaryConstants.VDOUBLE;
		if (obj instanceof Technology) return BinaryConstants.VTECHNOLOGY;
		if (obj instanceof Library) return BinaryConstants.VLIBRARY;
		if (obj instanceof Tool) return BinaryConstants.VTOOL;
		if (obj instanceof NodeInst) return BinaryConstants.VNODEINST;
		if (obj instanceof ArcInst) return BinaryConstants.VARCINST;
		if (obj instanceof NodeProto) return BinaryConstants.VNODEPROTO;
		if (obj instanceof ArcProto) return BinaryConstants.VARCPROTO;
		if (obj instanceof PortProto) return BinaryConstants.VPORTPROTO;
		return BinaryConstants.VUNKNOWN;
	}

	/**
	 * Helper routine to write a variable at address "addr" of type "ty".
	 * Returns zero if OK, negative on memory error, positive if there were
	 * correctable problems in the write.
	 */
	private void putOutVar(Object obj)
		throws IOException
	{
		if (obj instanceof Integer)
		{
			writeBigInteger(((Integer)obj).intValue());
			return;
		}
		if (obj instanceof Short)
		{
			writeSmallInteger(((Short)obj).shortValue());
			return;
		}
		if (obj instanceof Byte)
		{
			writeByte(((Byte)obj).byteValue());
			return;
		}
		if (obj instanceof String)
		{
			writeString((String)obj);
			return;
		}
		if (obj instanceof Float)
		{
			writeFloat(((Float)obj).floatValue());
			return;
		}
		if (obj instanceof Double)
		{
			writeDouble(((Double)obj).doubleValue());
			return;
		}
		if (obj instanceof Technology)
		{
			Technology tech = (Technology)obj;
			if (tech == null) writeBigInteger(-1); else
				writeBigInteger(tech.getIndex());
			return;
		}
		if (obj instanceof Library)
		{
			Library lib = (Library)obj;
			if (lib == null) writeString("noname"); else
				writeString(lib.getLibName());
			return;
		}
		if (obj instanceof Tool)
		{
			Tool tool = (Tool)obj;
			if (tool == null) writeBigInteger(-1); else
				writeBigInteger(tool.getIndex());
			return;
		}
		if (obj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)obj;
			if (ni == null) writeBigInteger(-1); else
				writeBigInteger(ni.getTempInt());
			return;
		}
		if (obj instanceof ArcInst)
		{
			ArcInst ai = (ArcInst)obj;
			if (ai == null) writeBigInteger(-1); else
				writeBigInteger(ai.getTempInt());
			return;
		}
		if (obj instanceof NodeProto)
		{
			NodeProto np = (NodeProto)obj;
			if (np == null) writeBigInteger(-1); else
				writeBigInteger(np.getTempInt());
			return;
		}
		if (obj instanceof ArcProto)
		{
			ArcProto ap = (ArcProto)obj;
			if (ap == null) writeBigInteger(-1); else
				writeBigInteger(ap.getTempInt());
			return;
		}
		if (obj instanceof PortProto)
		{
			PortProto pp = (PortProto)obj;
			if (pp == null) writeBigInteger(-1); else
				writeBigInteger(pp.getTempInt());
			return;
		}
	}

	/*
	 * Routine to scan the variables on an object (which are in "firstvar" and "numvar")
	 * for NODEPROTO references.  Any found are marked (by setting the "externalRefFlag" bit).
	 * This is used to gather cross-library references.
	 */
	private void findXLibVariables(ElectricObject obj)
	{
		for(Iterator it = obj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.isDontSave()) continue;
			Object refObj = var.getObject();
			if (refObj instanceof NodeProto || refObj instanceof NodeProto[])
			{
				if (refObj instanceof NodeProto[])
				{
					Object [] npArray = (Object [])refObj;
					int len = npArray.length;
					for(int j=0; j<len; j++)
					{
						NodeProto np = (NodeProto)npArray[j];
						if (np instanceof Cell)
							np.setBit(externalRefFlag);
					}
				} else
				{
					NodeProto np = (NodeProto)refObj;
					if (np instanceof Cell)
						np.setBit(externalRefFlag);
				}
			}
		}
	}

	// --------------------------------- LOW-LEVEL OUTPUT ---------------------------------

	/**
	 * routine to write a single byte from the input stream and return it.
	 */
	private void writeByte(byte b)
		throws IOException
	{
		dataOutputStream.write(b);
	}

	/**
	 * routine to write an integer (4 bytes) from the input stream and return it.
	 */
	private void writeBigInteger(int i)
		throws IOException
	{
		dataOutputStream.writeInt(i);
	}

	/**
	 * routine to write a float (4 bytes) from the input stream and return it.
	 */
	private void writeFloat(float f)
		throws IOException
	{
		dataOutputStream.writeFloat(f);
	}

	/**
	 * routine to write a double (8 bytes) from the input stream and return it.
	 */
	private void writeDouble(double d)
		throws IOException
	{
		dataOutputStream.writeDouble(d);
	}

	/**
	 * routine to write an short (2 bytes) from the input stream and return it.
	 */
	private void writeSmallInteger(short s)
		throws IOException
	{
		dataOutputStream.writeShort(s);
	}

	/**
	 * routine to write a string from the input stream and return it.
	 */
	private void writeString(String s)
		throws IOException
	{
		// disk and memory match: write the data
		int len = s.length();
		writeBigInteger(len);
		dataOutputStream.write(s.getBytes(), 0, len);
	}
}
