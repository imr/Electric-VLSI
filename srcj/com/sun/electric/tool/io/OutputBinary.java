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
import com.sun.electric.tool.user.ui.UITopLevel;
import com.sun.electric.tool.io.InputBinary;

import java.io.IOException;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


public class OutputBinary extends Output
{

	/** cell flag for finding external cell refernces */		private FlagSet externalRefFlag;

	/** all of the names used in variables */					private static HashMap varNames;

	OutputBinary()
	{
	}

	// ----------------------- public methods -------------------------------

	public boolean WriteLib(Library lib)
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
		NodeProto.freeFlagSet(cellFlag);

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
				for(Iterator cit = ni.getConnections(); cit.hasNext(); )
				{
					Connection con = (Connection)cit.next();
					findXLibVariables(con);
				}
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
		View.SIMSNAP.setTempInt(-5);
		View.SKELETON.setTempInt(-6);
		View.VHDL.setTempInt(-7);
		View.NETLIST.setTempInt(-8);
		View.DOC.setTempInt(-9);
		View.NETLISTNETLISP.setTempInt(-10);
		View.NETLISTALS.setTempInt(-11);
		View.NETLISTQUISC.setTempInt(-12);
		View.NETLISTRSIM.setTempInt(-13);
		View.NETLISTSILOS.setTempInt(-14);
		View.VERILOG.setTempInt(-15);
//		View.COMP.setTempInt(-16);
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
			writeString(view.getShortName());
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
		writeVariables(lib);

		// write the tool variables
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			writeVariables(tool);
		}

		// write the variables on technologies
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			writeVariables(tech);
		}

		// write the arcproto variables
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator ait = tech.getArcs(); ait.hasNext(); )
			{
				PrimitiveArc ap = (PrimitiveArc)ait.next();
				writeVariables(ap);
			}
		}

		// write the variables on primitive node prototypes
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)nit.next();
				writeVariables(np);
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
					writeVariables(pp);
				}
			}
		}

		// write the view variables
		writeBigInteger(View.getNumViews());
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View view = (View)it.next();
			writeBigInteger(view.getTempInt());
			writeVariables(view);
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
		NodeProto.freeFlagSet(externalRefFlag);

		// library written successfully
		return false;
	}

	// --------------------------------- OBJECT CONVERSION ---------------------------------

	void writeNodeProto(Cell cell, boolean thislib)
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
		writeBigInteger((int)toElectricDate(cell.getCreationDate()));
		writeBigInteger((int)toElectricDate(cell.getRevisionDate()));

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
				if (pp.getOriginalNode() != null)
				{
					i = pp.getOriginalNode().getTempInt();
				} else
				{
					System.out.println("ERROR: cell " + cell.describe() + " export " + pp.getProtoName() + " has no subnode");
				}
				writeBigInteger(i);

				// write the portproto index in the subnodeinst
				i = -1;
				if (pp.getOriginalNode() != null && pp.getOriginalPort() != null)
				{
					i = pp.getOriginalPort().getPortProto().getTempInt();
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
				writeVariables(pp);
			}
		}

		if (thislib)
		{
			// write tool information
			writeBigInteger(0);		// was "adirty"
			writeBigInteger(cell.lowLevelGetUserbits());

			// write variable information
			writeVariables(cell);
		}
	}

	void writeNodeInst(NodeInst ni)
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
		if (ni.getXSize() < 0 || ni.getYSize() < 0) transpose = 1;
		writeBigInteger(transpose);
		int rotation = (int)(ni.getAngle() * 1800 / Math.PI);
		writeBigInteger(rotation);

		TextDescriptor td = ni.getTextDescriptor();
		writeBigInteger(td.lowLevelGet0());
		writeBigInteger(td.lowLevelGet1());

		// write the arc ports
		writeBigInteger(ni.getNumConnections());
		for(Iterator it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			ArcInst ai = con.getArc();
			PortInst pi = con.getPortInst();
			int i = ai.getTempInt() << 1;
			if (ai.getHead().getPortInst() == pi) i += 0; else i += 1;
			writeBigInteger(i);

			// write the portinst prototype
			writeBigInteger(pi.getPortProto().getTempInt());

			// write the variable information
			writeNoVariables();
		}

		// count the exports
		writeBigInteger(ni.getNumExports());

		// write the exports
		for(Iterator it = ni.getExports(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			writeBigInteger(pp.getTempInt());

			// write the portinst prototype
			writeBigInteger(pp.getOriginalPort().getPortProto().getTempInt());

			// write the variable information
			writeNoVariables();
		}

		// write the tool information
		writeBigInteger(ni.lowLevelGetUserbits());

		// write variable information
		writeVariables(ni);
	}

	void writeArcInst(ArcInst ai)
		throws IOException
	{
		// write the arcproto pointer
		writeBigInteger(ai.getProto().getTempInt());

		// write basic arcinst information
		Technology tech = ai.getProto().getTechnology();
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
		int arcAngle = (int)(ai.getAngle() * 180 / Math.PI);
		if (arcAngle < 0) arcAngle += 360;
		ai.lowLevelSetArcAngle(arcAngle);
		int userBits = ai.lowLevelGetUserbits();
		writeBigInteger(userBits);

		// write variable information
		writeVariables(ai);
	}

	/**
	 * routine to convert the Java Date object to an Electric-format date (seconds since the epoch).
	 */
	long toElectricDate(Date date)
	{
		GregorianCalendar creation = new GregorianCalendar();
		creation.setTime(date);
		return creation.getTimeInMillis();
	}

	// --------------------------------- VARIABLES ---------------------------------

	// this list is also in "InputBinary.java"
	private static final int VUNKNOWN =                  0;		/** undefined variable */
	private static final int VINTEGER =                 01;		/** 32-bit integer variable */
	private static final int VADDRESS =                 02;		/** unsigned address */
	private static final int VCHAR =                    03;		/** character variable */
	private static final int VSTRING =                  04;		/** string variable */
	private static final int VFLOAT =                   05;		/** floating point variable */
	private static final int VDOUBLE =                  06;		/** double-precision floating point */
	private static final int VNODEINST =                07;		/** nodeinst pointer */
	private static final int VNODEPROTO =              010;		/** nodeproto pointer */
	private static final int VPORTARCINST =            011;		/** portarcinst pointer */
	private static final int VPORTEXPINST =            012;		/** portexpinst pointer */
	private static final int VPORTPROTO =              013;		/** portproto pointer */
	private static final int VARCINST =                014;		/** arcinst pointer */
	private static final int VARCPROTO =               015;		/** arcproto pointer */
	private static final int VGEOM =                   016;		/** geometry pointer */
	private static final int VLIBRARY =                017;		/** library pointer */
	private static final int VTECHNOLOGY =             020;		/** technology pointer */
	private static final int VTOOL =                   021;		/** tool pointer */
	private static final int VRTNODE =                 022;		/** R-tree pointer */
	private static final int VFRACT =                  023;		/** fractional integer (scaled by WHOLE) */
	private static final int VNETWORK =                024;		/** network pointer */
	private static final int VVIEW =                   026;		/** view pointer */
	private static final int VWINDOWPART =             027;		/** window partition pointer */
	private static final int VGRAPHICS =               030;		/** graphics object pointer */
	private static final int VSHORT =                  031;		/** 16-bit integer */
	private static final int VCONSTRAINT =             032;		/** constraint solver */
	private static final int VGENERAL =                033;		/** general address/type pairs (used only in fixed-length arrays) */
	private static final int VWINDOWFRAME =            034;		/** window frame pointer */
	private static final int VPOLYGON =                035;		/** polygon pointer */
	private static final int VBOOLEAN =                036;		/** boolean variable */
	private static final int VTYPE =                   037;		/** all above type fields */
	private static final int VCODE1 =                  040;		/** variable is interpreted code (with VCODE2) */
	private static final int VDISPLAY =               0100;		/** display variable (uses textdescript field) */
	private static final int VISARRAY =               0200;		/** set if variable is array of above objects */
	private static final int VLENGTH =         03777777000;		/** array length (0: array is -1 terminated) */
	private static final int VLENGTHSH =                 9;		/** right shift for VLENGTH */
	private static final int VCODE2 =          04000000000;		/** variable is interpreted code (with VCODE1) */

	/**
	 * routine to write the global namespace.  returns true upon error
	 */
	boolean writeNameSpace()
		throws IOException
	{
		int numVariableNames = ElectricObject.getNumVariableNames();
		writeBigInteger(numVariableNames);

		Variable.Name [] nameList = new Variable.Name[numVariableNames];
		for(Iterator it = ElectricObject.getVariableNames(); it.hasNext(); )
		{
			String name = (String)it.next();
			Variable.Name vn = ElectricObject.findName(name);
			nameList[vn.getIndex()] = vn;
		}
		for(int i=0; i<numVariableNames; i++)
		{
			Variable.Name vn = nameList[i];
			if (vn == null) writeString(""); else
				writeString(vn.getName());
		}
		return false;
	}

	/**
	 * routine to write an empty set of variables.
	 */
	void writeNoVariables()
		throws IOException
	{
		writeBigInteger(0);
	}

	/**
	 * routine to write a set of object variables.  returns negative upon error and
	 * otherwise returns the number of variables write
	 */
	int writeVariables(ElectricObject obj)
		throws IOException
	{
		int count = 0;
		for(Iterator it = obj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.isDontSave()) count++;
		}
		writeBigInteger(count);
		for(Iterator it = obj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.isDontSave()) continue;
			Variable.Name vn = var.getName();
			writeSmallInteger((short)vn.getIndex());

			// create the "type" field
			Object varObj = var.getObject();
			int type = var.lowLevelGetFlags() & ~(VTYPE|VISARRAY|VLENGTH);
			if (varObj instanceof Object[])
			{
				Object [] objList = (Object [])varObj;
				type |= getVarType(objList[0]) | VISARRAY | (objList.length << VLENGTHSH);
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
				putOutVar(obj);
			}
		}
		return(count);
	}

	int getVarType(Object obj)
	{
		if (obj instanceof Integer) return VINTEGER;
		if (obj instanceof Short) return VSHORT;
		if (obj instanceof Byte) return VCHAR;
		if (obj instanceof String) return VSTRING;
		if (obj instanceof Float) return VFLOAT;
		if (obj instanceof Double) return VDOUBLE;
		if (obj instanceof Technology) return VTECHNOLOGY;
		if (obj instanceof Library) return VLIBRARY;
		if (obj instanceof Tool) return VTOOL;
		if (obj instanceof NodeInst) return VNODEINST;
		if (obj instanceof ArcInst) return VARCINST;
		if (obj instanceof NodeProto) return VNODEPROTO;
		if (obj instanceof ArcProto) return VARCPROTO;
		if (obj instanceof PortProto) return VPORTPROTO;
		return VUNKNOWN;
	}

	/**
	 * Helper routine to write a variable at address "addr" of type "ty".
	 * Returns zero if OK, negative on memory error, positive if there were
	 * correctable problems in the write.
	 */
	void putOutVar(Object obj)
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
	void findXLibVariables(ElectricObject obj)
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

	// --------------------------------- LOW-LEVEL INPUT ---------------------------------

	/**
	 * routine to write a single byte from the input stream and return it.
	 */
	void writeByte(byte b)
		throws IOException
	{
		dataOutputStream.write(b);
	}

	/**
	 * routine to write an integer (4 bytes) from the input stream and return it.
	 */
	void writeBigInteger(int i)
		throws IOException
	{
		dataOutputStream.writeInt(i);
	}

	/**
	 * routine to write a float (4 bytes) from the input stream and return it.
	 */
	void writeFloat(float f)
		throws IOException
	{
		dataOutputStream.writeFloat(f);
	}

	/**
	 * routine to write a double (8 bytes) from the input stream and return it.
	 */
	void writeDouble(double d)
		throws IOException
	{
		dataOutputStream.writeDouble(d);
	}

	/**
	 * routine to write an short (2 bytes) from the input stream and return it.
	 */
	void writeSmallInteger(short s)
		throws IOException
	{
		dataOutputStream.writeShort(s);
	}

	/**
	 * routine to write a string from the input stream and return it.
	 */
	void writeString(String s)
		throws IOException
	{
		// disk and memory match: write the data
		int len = s.length();
		writeBigInteger(len);
		dataOutputStream.write(s.getBytes(), 0, len);
	}
}
