/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ELIB.java
 * Input/output tool: ELIB Library output
 * Written by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.ELIBConstants;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;


/**
 * This class writes files in binary (.elib) format.
 */
public class ELIB extends Output
{

	/** cell flag for finding external cell refernces */		private FlagSet externalRefFlag;
	/** true to write a 6.XX compatible library (MAGIC11) */	private boolean compatibleWith6;
	/** map to assign indices to cell names (for 6.XX) */		private HashMap cellIndexMap;
	/** map to assign indices to node protos (for 6.XX) */		private HashMap nodeProtoIndexMap = new HashMap();

	/** all of the names used in variables */					private static HashMap varNames;
	/** all of the views and their integer values. */			private HashMap viewMap;

	ELIB()
	{
	}

	private class NodeProtoIndices
	{
		private final int nodeProtoIndex;
		private final int portProtoStartIndex;
		private final int nodeInstStartIndex;
		private final int arcInstStartIndex;

		NodeProtoIndices(int nodeProtoIndex, int portProtoStartIndex)
		{
			this.nodeProtoIndex = nodeProtoIndex;
			this.portProtoStartIndex = portProtoStartIndex;
			this.nodeInstStartIndex = -1;
			this.arcInstStartIndex = -1;
		}

		NodeProtoIndices(int nodeProtoIndex, int portProtoStartIndex, int nodeInstStartIndex, int arcInstStartIndex)
		{
			this.nodeProtoIndex = nodeProtoIndex;
			this.portProtoStartIndex = portProtoStartIndex;
			this.nodeInstStartIndex = nodeInstStartIndex;
			this.arcInstStartIndex = arcInstStartIndex;
		}

	}

	private void writeNodeProtoIndex(NodeProto np)
		throws IOException
	{
		NodeProtoIndices inds = (NodeProtoIndices)nodeProtoIndexMap.get(np);
		int i = -1;
		if (inds != null)
			i = inds.nodeProtoIndex;
		writeBigInteger(i);
	}

	private void writePortProtoIndex(PortProto pp)
		throws IOException
	{
		NodeProtoIndices inds = (NodeProtoIndices)nodeProtoIndexMap.get(pp.getParent());
		int i = -1;
		if (inds != null) {
			if (pp instanceof Export)
				i = inds.portProtoStartIndex + pp.getPortIndex();
			else
				i = inds.portProtoStartIndex - pp.getPortIndex();
		}
		writeBigInteger(i);
	}

	private void writeNodeInstIndex(NodeInst ni)
		throws IOException
	{
		NodeProtoIndices inds = (NodeProtoIndices)nodeProtoIndexMap.get(ni.getParent());
		int i = -1;
		if (inds != null && inds.nodeInstStartIndex >= 0)
			i = inds.nodeInstStartIndex + ni.getNodeIndex();
		writeBigInteger(i);
	}

	private int getArcInstIndex(ArcInst ai)
	{
		NodeProtoIndices inds = (NodeProtoIndices)nodeProtoIndexMap.get(ai.getParent());
		if (inds == null || inds.arcInstStartIndex < 0) return -1;
		return inds.arcInstStartIndex + ai.getArcIndex();
	}

	public void write6Compatible() { compatibleWith6 = true; }

	// ----------------------- public methods -------------------------------

	/**
	 * Method to write a Library in binary (.elib) format.
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
	 * Method to write the .elib file.
	 * Returns true on error.
	 */
	private boolean writeTheLibrary(Library lib)
		throws IOException
	{
		int magic = ELIBConstants.MAGIC13;
		if (compatibleWith6) magic = ELIBConstants.MAGIC11;
		writeBigInteger(magic);
		writeByte((byte)2);		// size of Short
		writeByte((byte)4);		// size of Int
		writeByte((byte)1);		// size of Char

		// write the number of tools
		int toolCount = Tool.getNumTools();
		writeBigInteger(toolCount);
		int techCount = Technology.getNumTechnologies();
		writeBigInteger(techCount);

		// convert cellGroups to "next in cell group" pointers
		FlagSet cellFlag = Cell.getFlagSet(1);
		HashMap cellInSameGroup = new HashMap();
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			cell.clearBit(cellFlag);
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
					cellInSameGroup.put(lastCellInGroup, cellInGroup);
				}
				cellInGroup.setBit(cellFlag);
				lastCellInGroup = cellInGroup;
			}
			if (lastCellInGroup == null || firstCellInGroup == null)
				lastCellInGroup = firstCellInGroup = cell;
			cellInSameGroup.put(lastCellInGroup, firstCellInGroup);
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
			nodeProtoIndexMap.put(cell, new NodeProtoIndices(nodeProtoIndex, portProtoIndex, nodeIndex, arcIndex));
			nodeProtoIndex++;
			portProtoIndex += cell.getNumPorts();
			nodeIndex += cell.getNumNodes();
			arcIndex += cell.getNumArcs();
		}
		int cellsHere = nodeProtoIndex;

		// prepare to locate references to cells in other libraries
		FlagSet externalRefFlag = Cell.getFlagSet(1);
		externalRefFlag.clearOnAllCells();

		// scan for all cross-library references
		varNames = new HashMap();
		findXLibVariables(lib);
// 		for(Iterator it = Tool.getTools(); it.hasNext(); )
// 		{
// 			Tool tool = (Tool)it.next();
// 			findXLibVariables(tool);
// 		}
// 		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
// 		{
// 			Technology tech = (Technology)it.next();
// 			findXLibVariables(tech);
// 			for(Iterator ait = tech.getArcs(); ait.hasNext(); )
// 			{
// 				ArcProto ap = (ArcProto)ait.next();
// 				findXLibVariables(ap);
// 			}
// 			for(Iterator nit = tech.getNodes(); nit.hasNext(); )
// 			{
// 				PrimitiveNode np = (PrimitiveNode)nit.next();
//				findXLibVariables(np);
// 				for(Iterator eit = np.getPorts(); eit.hasNext(); )
// 				{
// 					PrimitivePort pp = (PrimitivePort)eit.next();
// 					findXLibVariables(pp);
// 				}
// 			}
// 		}
// 		for(Iterator it = View.getViews(); it.hasNext(); )
// 		{
// 			View view = (View)it.next();
// 			findXLibVariables(view);
// 		}
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
					((Cell)ni.getProto()).setBit(externalRefFlag);
			}
			for(Iterator ait = cell.getArcs(); ait.hasNext(); )
			{
				ArcInst ai = (ArcInst)ait.next();
				findXLibVariables(ai);
			}
		}

		// count and number the cells in other libraries
//		if (!compatibleWith6)
		{
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library olib = (Library)it.next();
				if (olib == lib) continue;
				for(Iterator cit = olib.getCells(); cit.hasNext(); )
				{
					Cell cell = (Cell)cit.next();
					if (!cell.isBit(externalRefFlag)) continue;
					nodeProtoIndexMap.put(cell, new NodeProtoIndices(nodeProtoIndex, portProtoIndex));
					nodeProtoIndex++;
					portProtoIndex += cell.getNumPorts();
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
				nodeProtoIndexMap.put(np, new NodeProtoIndices(-2 - primNodeProtoIndex, -2 - primPortProtoIndex));
				primNodeProtoIndex++;
				primPortProtoIndex += np.getNumPorts();
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

		// write count of cells if creating version-6-compatible output
		int cellCount = 0;
		if (compatibleWith6)
		{
			cellIndexMap = new HashMap();
			for(Iterator it = lib.getCells(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				String cellName = cell.getName();
				Integer cellIndex = (Integer)cellIndexMap.get(cellName);
				if (cellIndex == null)
				{
					cellIndex = new Integer(cellCount++);
					cellIndexMap.put(cellName, cellIndex);
				}
				for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
				{
					NodeInst ni = (NodeInst)nIt.next();
					if (ni.getProto() instanceof Cell)
					{
						Cell subCell = (Cell)ni.getProto();
						if (subCell.getLibrary() != lib)
						{
							// external cell reference: include it in the cell names list
							cellName = subCell.getName();
							cellIndex = (Integer)cellIndexMap.get(cellName);
							if (cellIndex == null)
							{
								cellIndex = new Integer(cellCount++);
								cellIndexMap.put(cellName, cellIndex);
							}

						}
					}
				}
			}
			writeBigInteger(cellCount);
		}

		// write the current cell
		writeNodeProtoIndex(lib.getCurCell());

		// write the version number
		writeString(Version.getVersion().toString());

		// number the views and write nonstandard ones
		viewMap = new HashMap();
		viewMap.put(View.UNKNOWN, new Integer(-1));
		viewMap.put(View.LAYOUT, new Integer(-2));
		viewMap.put(View.SCHEMATIC, new Integer(-3));
		viewMap.put(View.ICON, new Integer(-4));
		viewMap.put(View.DOCWAVE, new Integer(-5));				// unknown in C
		viewMap.put(View.LAYOUTSKEL, new Integer(-6));			// unknown in C
		viewMap.put(View.VHDL, new Integer(-7));
		viewMap.put(View.NETLIST, new Integer(-8));
		viewMap.put(View.DOC, new Integer(-9));
		viewMap.put(View.NETLISTNETLISP, new Integer(-10));		// unknown in C
		viewMap.put(View.NETLISTALS, new Integer(-11));			// unknown in C
		viewMap.put(View.NETLISTQUISC, new Integer(-12));		// unknown in C
		viewMap.put(View.NETLISTRSIM, new Integer(-13));		// unknown in C
		viewMap.put(View.NETLISTSILOS, new Integer(-14));		// unknown in C
		viewMap.put(View.VERILOG, new Integer(-15));
		List viewsToSave = new ArrayList();
		int i = 1;
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View view = (View)it.next();
			Integer found = (Integer)viewMap.get(view);
			if (found != null) continue;
			viewMap.put(view, new Integer(i++));
			viewsToSave.add(view);
		}
		writeBigInteger(viewsToSave.size());
		for(Iterator it = viewsToSave.iterator(); it.hasNext(); )
		{
			View view = (View)it.next();
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
				writeString(np.getName());
				writeBigInteger(np.getNumPorts());
				for(Iterator pit = np.getPorts(); pit.hasNext(); )
				{
					PrimitivePort pp = (PrimitivePort)pit.next();
					writeString(pp.getName());
				}
			}

			// write the primitive arc prototype names
			writeBigInteger(tech.getNumArcs());
			for(Iterator ait = tech.getArcs(); ait.hasNext(); )
			{
				PrimitiveArc ap = (PrimitiveArc)ait.next();
				writeString(ap.getName());
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

		// write the tool scale values
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			writeBigInteger((int)Math.round(tech.getScale()*2));
		}

		// convert any PortInst variables to NodeInst Variables
		for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
		{
			Cell cell = (Cell)cIt.next();
			for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
			{
				NodeInst ni = (NodeInst)nIt.next();
				for(Iterator pIt = ni.getPortInsts(); pIt.hasNext(); )
				{
					PortInst pi = (PortInst)pIt.next();
					for(Iterator it = pi.getVariables(); it.hasNext(); )
					{
						Variable var = (Variable)it.next();
						if (var.isDontSave()) continue;

						// convert this PortInst variable to a NodeInst variable
						StringBuffer sb = new StringBuffer();
						String portName = pi.getPortProto().getName();
						int len = portName.length();
						for(int j=0; j<len; j++)
						{
							char ch = portName.charAt(j);
							if (ch == '\\' || ch == '_') sb.append('\\');
							sb.append(ch);
						}
						String newVarName = "ATTRP_" + sb.toString() + "_" + var.getKey().getName();
						Undo.setNextChangeQuiet(true);
						Variable newVar = ni.newVar(newVarName, var.getObject());
						if (var.isDisplay()) newVar.setDisplay(true);
						newVar.setCode(var.getCode());
						newVar.setTextDescriptor(var.getTextDescriptor());
					}
				}
			}
		}

		// write the global namespace
		writeNameSpace();

		// convert font indices to a variable that preserves the font names
		Output.createFontAssociationVariable(lib);

		// write the library variables
		writeVariables(lib, 0);

		// write the tool variables
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			writeMeaningPrefs(tool);
		}

		// write the variables on technologies
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			writeMeaningPrefs(tech);
		}

		// write the arcproto variables
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator ait = tech.getArcs(); ait.hasNext(); )
			{
				PrimitiveArc ap = (PrimitiveArc)ait.next();
				// write zero number of Variables
				writeBigInteger(0);
//				writeVariables(ap, 0);
			}
		}

		// write the variables on primitive node prototypes
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)nit.next();
				// write zero number of Variables
				writeBigInteger(0);
//				writeVariables(np, 0);
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
					// write zero number of Variables
					writeBigInteger(0);
// 					writeVariables(pp, 0);
				}
			}
		}

		// write the view variables
		writeBigInteger(View.getNumViews());
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View view = (View)it.next();
			Integer viewIndex = (Integer)viewMap.get(view);
			writeBigInteger(viewIndex.intValue());
			// write zero number of Variables
			writeBigInteger(0);
//			writeVariables(view, 0);
		}

		// write cells if creating version-6-compatible output
		if (compatibleWith6)
		{
			String [] cellNames = new String[cellCount];
			for(Iterator it = cellIndexMap.keySet().iterator(); it.hasNext(); )
			{
				String cellName = (String)it.next();
				Integer cellIndex = (Integer)cellIndexMap.get(cellName);
				if (cellIndex == null) continue;
				cellNames[cellIndex.intValue()] = cellName;
			}
			for(int j=0; j<cellCount; j++)
			{
				writeString(cellNames[j]);
				writeBigInteger(0);
			}
		}

		// write all of the cells in this library
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			writeNodeProto(cell, true, cellInSameGroup);
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
				writeNodeProto(cell, false, cellInSameGroup);
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

		// remove any PortInst variables that were converted to NodeInst Variables
		for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
		{
			Cell cell = (Cell)cIt.next();
			for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
			{
				NodeInst ni = (NodeInst)nIt.next();
				boolean found = true;
				while (found)
				{
					found = false;
					for(Iterator it = ni.getVariables(); it.hasNext(); )
					{
						Variable var = (Variable)it.next();
						String varName = var.getKey().getName();
						if (varName.startsWith("ATTRP_"))
						{
							Undo.setNextChangeQuiet(true);
							ni.delVar(var.getKey());
							found = true;
							break;
						}
					}
				}
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

	private void writeNodeProto(Cell cell, boolean thislib, HashMap cellInSameGroup)
		throws IOException
	{
		if (compatibleWith6)
		{
			// write cell index if creating version-6-compatible output
			Integer cellIndex = (Integer)cellIndexMap.get(cell.getName());
			writeBigInteger(cellIndex.intValue());
		} else
		{
			// write cell information
			writeString(cell.getName());
	
			// write the "next in cell group" pointer
			Object obj = cellInSameGroup.get(cell);
			Cell nextInGroup = null;
			if (obj != null && obj instanceof Cell)
				nextInGroup = (Cell)obj;
			writeNodeProtoIndex(nextInGroup);
	
			// write the "next in continuation" pointer
			int nextCont = -1;
			writeBigInteger(nextCont);
		}

		// write the view information
		Integer viewIndex = (Integer)viewMap.get(cell.getView());
		if (viewIndex == null) viewIndex = new Integer(0);
		writeBigInteger(viewIndex.intValue());
		writeBigInteger(cell.getVersion());
		writeBigInteger((int)ELIBConstants.dateToSeconds(cell.getCreationDate()));
		writeBigInteger((int)ELIBConstants.dateToSeconds(cell.getRevisionDate()));

		// write the nodeproto bounding box
		Technology tech = cell.getTechnology();
		Rectangle2D bounds = cell.getBounds();
		int lowX = (int)Math.round((bounds.getMinX() * tech.getScale()*2));
		int highX = (int)Math.round((bounds.getMaxX() * tech.getScale()*2));
		int lowY = (int)Math.round((bounds.getMinY() * tech.getScale()*2));
		int highY = (int)Math.round((bounds.getMaxY() * tech.getScale()*2));
		writeBigInteger(lowX);
		writeBigInteger(highX);
		writeBigInteger(lowY);
		writeBigInteger(highY);

		if (!thislib)
		{
            // TODO: remove this hack once a proper fix is made - JKG 28 Oct 2004
            // this fixes the problem of elibs referencing jelibs, and
            // (a) the two formats are not compatible (as of version 8.01af), and
            // (b) you can't export to ELIB and revert back to an old version that
            // doesn't understand jelib if you reference jelib files.
			Library instlib = cell.getLibrary();
            String filePath = instlib.getLibFile().getPath();
            //filePath = filePath.replaceAll("\\.jelib$", ".elib");
			writeString(filePath);
		}

		// write the number of portprotos on this nodeproto
		writeBigInteger(cell.getNumPorts());

		// write the portprotos on this nodeproto
		for(Iterator it = cell.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			if (thislib)
			{
				PortInst pi = pp.getOriginalPort();
				// write the connecting subnodeinst for this portproto
				writeNodeInstIndex(pi.getNodeInst());
				// write the portproto index in the subnodeinst
				writePortProtoIndex(pi.getPortProto());
			}

			// write the portproto name
			writeString(pp.getName());

			if (thislib)
			{
				// write the text descriptor
				TextDescriptor td = pp.getTextDescriptor(Export.EXPORT_NAME_TD);
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
		NodeProto np = ni.getProto();

		// write descriptive information
		Technology tech = ni.getParent().getTechnology();
		int lowX, highX, lowY, highY;
		writeNodeProtoIndex(np);
		if (np instanceof Cell)
		{
			lowX = (int)Math.round((ni.getTrueCenterX() - ni.getXSize()/2) * tech.getScale()*2);
			highX = (int)Math.round((ni.getTrueCenterX() + ni.getXSize()/2) * tech.getScale()*2);
			lowY = (int)Math.round((ni.getTrueCenterY() - ni.getYSize()/2) * tech.getScale()*2);
			highY = (int)Math.round((ni.getTrueCenterY() + ni.getYSize()/2) * tech.getScale()*2);
		} else
		{
			lowX = (int)Math.round((ni.getAnchorCenterX() - ni.getXSize()/2) * tech.getScale()*2);
			highX = (int)Math.round((ni.getAnchorCenterX() + ni.getXSize()/2) * tech.getScale()*2);
			lowY = (int)Math.round((ni.getAnchorCenterY() - ni.getYSize()/2) * tech.getScale()*2);
			highY = (int)Math.round((ni.getAnchorCenterY() + ni.getYSize()/2) * tech.getScale()*2);
		}
		writeBigInteger(lowX);
		writeBigInteger(lowY);
		writeBigInteger(highX);
		writeBigInteger(highY);

		// write anchor point too
		if (np instanceof Cell && !compatibleWith6)
		{
			int anchorX = (int)Math.round(ni.getAnchorCenterX() * tech.getScale() * 2);
			int anchorY = (int)Math.round(ni.getAnchorCenterY() * tech.getScale() * 2);
			writeBigInteger(anchorX);
			writeBigInteger(anchorY);
		}

		int transpose = 0;
		int rotation = ni.getAngle();
		if (compatibleWith6)
		{
			NodeInst.OldStyleTransform ost = new NodeInst.OldStyleTransform(ni);
			rotation = ost.getCAngle();
			transpose = ost.isCTranspose() ? 1 : 0;

//			Point oldStyle = ni.getOldStyleRotationAndTranspose();
//			rotation = oldStyle.x;
//			transpose = oldStyle.y;
		} else
		{
			if (ni.isXMirrored()) transpose |= 2;
			if (ni.isYMirrored()) transpose |= 4;
		}
		writeBigInteger(transpose);
		writeBigInteger(rotation);

		TextDescriptor td = ni.getTextDescriptor(NodeInst.NODE_PROTO_TD);
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
				int i = getArcInstIndex(ai) << 1;
				if (ai.getHead() == con) i++;
				writeBigInteger(i);

				// write the portinst prototype
				writePortProtoIndex(con.getPortInst().getPortProto());

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
				writePortProtoIndex(pp);

				// write the portinst prototype
				writePortProtoIndex(pp.getOriginalPort().getPortProto());

				// write the variable information
				writeNoVariables();
			}
		}

		// write the tool information
		writeBigInteger(ni.lowLevelGetUserbits());

		// write variable information
		writeVariables(ni, tech.getScale()*2);
	}

	private static class OrderedConnections implements Comparator
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

	private static class OrderedExports implements Comparator
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
		writeBigInteger((int)Math.round(ai.getWidth() * tech.getScale()*2));

		// write the arcinst tail information
		Point2D location = ai.getTail().getLocation();
		writeBigInteger((int)Math.round(location.getX() * tech.getScale()*2));
		writeBigInteger((int)Math.round(location.getY() * tech.getScale()*2));
		writeNodeInstIndex(ai.getTail().getPortInst().getNodeInst());

		// write the arcinst head information
		location = ai.getHead().getLocation();
		writeBigInteger((int)Math.round(location.getX() * tech.getScale()*2));
		writeBigInteger((int)Math.round(location.getY() * tech.getScale()*2));
		writeNodeInstIndex(ai.getHead().getPortInst().getNodeInst());

		// write the arcinst's tool information
		int arcAngle = ai.getAngle() / 10;
		ai.lowLevelSetArcAngle(arcAngle);
		int userBits = ai.lowLevelGetUserbits();

		// add a negated bit if the tail is negated
		userBits &= ~(ELIBConstants.ISNEGATED | ELIBConstants.ISHEADNEGATED);
		if (ai.getTail().isNegated())
		{
			if (ai.isReverseEnds()) userBits |= ELIBConstants.ISHEADNEGATED; else
				userBits |= ELIBConstants.ISNEGATED;
		}
		if (ai.getHead().isNegated())
		{
			if (ai.isReverseEnds()) userBits |= ELIBConstants.ISNEGATED; else
				userBits |= ELIBConstants.ISHEADNEGATED;
		}
		writeBigInteger(userBits);

		// write variable information
		writeVariables(ai, 0);
	}

	// --------------------------------- VARIABLES ---------------------------------

	/**
	 * Method to write the global namespace.  returns true upon error
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
	 * Method to write an empty set of variables.
	 */
	private void writeNoVariables()
		throws IOException
	{
		writeBigInteger(0);
	}

	/**
	 * Method to write a set of object variables.  returns negative upon error and
	 * otherwise returns the number of variables write
	 */
	private int writeVariables(ElectricObject obj, double scale)
		throws IOException
	{
		// count the number of persistent variables
		int count = 0;
		for(Iterator it = obj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.isDontSave()) count++;
		}

		// add one more for Geometrics with names
		if (obj instanceof Geometric && ((Geometric)obj).getNameKey() != null)
			count++;

		// write the number of Variables
		writeBigInteger(count);

		// write the variables
		for(Iterator it = obj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.isDontSave()) continue;
			Variable.Key key = var.getKey();
			short index = (short)key.getIndex();
            if (index < 0) {
                JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), new String [] {"ERROR! Too many unique variable names",
                    "The ELIB format cannot handle this many unique variables names", "Either delete the excess variables, or save to a readable dump"},
                    "Error saving ELIB file", JOptionPane.ERROR_MESSAGE);
                throw new IOException("Variable.Key index too large");
            }
			writeSmallInteger(index);

			// create the "type" field
			Object varObj = var.getObject();
			int type = var.lowLevelGetFlags() & ~(ELIBConstants.VTYPE|ELIBConstants.VISARRAY|ELIBConstants.VLENGTH);
			if (varObj instanceof Object[])
			{
				Object [] objList = (Object [])varObj;
				// This doesn't seem to work properly for trace
				if (objList.length > 0)
					type |= ELIBConstants.getVarType(objList[0]) | ELIBConstants.VISARRAY | (objList.length << ELIBConstants.VLENGTHSH);
			} else
			{
				if (compatibleWith6 && varObj instanceof Double)
					varObj = new Float(((Double)varObj).doubleValue());
				type |= ELIBConstants.getVarType(varObj);
			}
			// Only string variables may have language code bits.
			if ((type&ELIBConstants.VTYPE) != ELIBConstants.VSTRING && (type&(ELIBConstants.VCODE1|ELIBConstants.VCODE2)) != 0)
			{
				System.out.println("Variable " + key.getName() + " on " + obj + " is not a string. Language bits are cleared.");
				type &= ~(ELIBConstants.VCODE1|ELIBConstants.VCODE2);
			}

			// special case for "trace" information on NodeInsts
			if (obj instanceof NodeInst && key == NodeInst.TRACE && varObj instanceof Point2D[])
			{
				Point2D [] points = (Point2D [])varObj;
				type = var.lowLevelGetFlags() & ~(ELIBConstants.VTYPE|ELIBConstants.VISARRAY|ELIBConstants.VLENGTH);
				int len = points.length * 2;
				type |= ELIBConstants.VFLOAT | ELIBConstants.VISARRAY | (len << ELIBConstants.VLENGTHSH);
				Float [] newPoints = new Float[len];
				for(int i=0; i<points.length; i++)
				{
					newPoints[i*2] = new Float(points[i].getX());
					newPoints[i*2+1] = new Float(points[i].getY());
				}
				varObj = newPoints;
			}
            if (type == 0) {
                System.out.println("Wrote Type 0 for Variable "+key.getName()+", value "+varObj);
            }
			writeBigInteger(type);

			// write the text descriptor
			TextDescriptor td = var.getTextDescriptor();
			writeBigInteger(td.lowLevelGet0());
			writeBigInteger(td.lowLevelGet1());

			if (varObj instanceof Object[])
			{
				Object [] objList = (Object [])varObj;
				int len = objList.length;
				writeBigInteger(len);
				for(int i=0; i<len; i++)
				{
					Object oneObj = objList[i];
					putOutVar(oneObj);
				}
			} else
			{
				putOutVar(varObj);
			}
		}

		// write the node or arc name
		if (obj instanceof Geometric && ((Geometric)obj).getNameKey() != null)
		{
			Geometric geom = (Geometric)obj;
			Variable.Key key;
			String varName;
			if (geom instanceof NodeInst)
			{
				key = NodeInst.NODE_NAME;
				varName = NodeInst.NODE_NAME_TD;
			} else
			{
				key = ArcInst.ARC_NAME;
				varName = ArcInst.ARC_NAME_TD;
			}
			writeSmallInteger((short)key.getIndex());
			int type = ELIBConstants.VSTRING;
			if (geom.isUsernamed()) type |= ELIBConstants.VDISPLAY;
			writeBigInteger(type);

			// write the text descriptor of name
			writeBigInteger(geom.getTextDescriptor(varName).lowLevelGet0());
			writeBigInteger(geom.getTextDescriptor(varName).lowLevelGet1());
			putOutVar(geom.getName());
		}

		return(count);
	}

	/**
	 * Method to write a set of meaning preferences.
	 */
	private void writeMeaningPrefs(Object obj)
		throws IOException
	{
		List prefs = Pref.getMeaningVariables(obj);
		writeBigInteger(prefs.size());
		for(Iterator it = prefs.iterator(); it.hasNext(); )
		{
			Pref pref = (Pref)it.next();
			short index = (short)pref.getMeaning().getKey().getIndex();
            if (index < 0) {
                JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), new String [] {"ERROR! Too many unique variable names",
                    "The ELIB format cannot handle this many unique variables names", "Either delete the excess variables, or save to a readable dump"},
                    "Error saving ELIB file", JOptionPane.ERROR_MESSAGE);
                throw new IOException("Variable.Key index too large");
            }
			writeSmallInteger(index);

			// create the "type" field
			Object varObj = pref.getValue();
			int type = ELIBConstants.getVarType(varObj);
            if (type == 0) {
                System.out.println("Wrote Type 0 for Variable "+pref.getPrefName()+", value "+varObj);
            }
			writeBigInteger(type);

			// write zero text descriptor
			writeBigInteger(0);
			writeBigInteger(0);

			putOutVar(varObj);
		}
	}

	/**
	 * Helper method to write a variable at address "addr" of type "ty".
	 * Returns zero if OK, negative on memory error, positive if there were
	 * correctable problems in the write.
	 */
	private void putOutVar(Object obj)
		throws IOException
	{
		if (obj == null)
		{
			writeBigInteger(-1);
			return;
		}
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
		if (obj instanceof Point2D)
		{
			writeFloat((float)((Point2D)obj).getX());
			writeFloat((float)((Point2D)obj).getY());
			return;
		}
		if (obj instanceof Technology)
		{
			Technology tech = (Technology)obj;
			writeBigInteger(tech.getIndex());
			return;
		}
		if (obj instanceof Library)
		{
			Library lib = (Library)obj;
			writeString(lib.getName());
			return;
		}
		if (obj instanceof Tool)
		{
			Tool tool = (Tool)obj;
			writeBigInteger(tool.getIndex());
			return;
		}
		if (obj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)obj;
			writeNodeInstIndex(ni);
			return;
		}
		if (obj instanceof ArcInst)
		{
			ArcInst ai = (ArcInst)obj;
			writeBigInteger(getArcInstIndex(ai));
			return;
		}
		if (obj instanceof NodeProto)
		{
			writeNodeProtoIndex((NodeProto)obj);
			return;
		}
		if (obj instanceof ArcProto)
		{
			ArcProto ap = (ArcProto)obj;
			writeBigInteger(ap.getTempInt());
			return;
		}
		if (obj instanceof PortProto)
		{
			writePortProtoIndex((PortProto)obj);
			return;
		}
		System.out.println("Error: Cannot write objects of type " + obj.getClass());
	}

	/*
	 * Method to scan the variables on an object (which are in "firstvar" and "numvar")
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
			if (refObj instanceof NodeProto[])
			{
				NodeProto [] npArray = (NodeProto[])refObj;
				int len = npArray.length;
				for(int j=0; j<len; j++)
				{
					NodeProto np = npArray[j];
					if (np instanceof Cell)
						((Cell)np).setBit(externalRefFlag);
				}
			} else if (refObj instanceof NodeProto)
			{
				NodeProto np = (NodeProto)refObj;
				if (np instanceof Cell)
					((Cell)np).setBit(externalRefFlag);
			} else if (refObj instanceof PortProto[])
			{
				PortProto [] ppArray = (PortProto [])refObj;
				int len = ppArray.length;
				for(int j=0; j<len; j++)
				{
					NodeProto np = ppArray[j].getParent();
					if (np instanceof Cell)
						((Cell)np).setBit(externalRefFlag);
				}
			} else if (refObj instanceof PortProto)
			{
				NodeProto np = ((PortProto)refObj).getParent();
				if (np instanceof Cell)
					((Cell)np).setBit(externalRefFlag);
			}
		}
	}

	// --------------------------------- LOW-LEVEL OUTPUT ---------------------------------

	/**
	 * Method to write a single byte from the input stream and return it.
	 */
	private void writeByte(byte b)
		throws IOException
	{
		dataOutputStream.write(b);
	}

	/**
	 * Method to write an integer (4 bytes) from the input stream and return it.
	 */
	private void writeBigInteger(int i)
		throws IOException
	{
		dataOutputStream.writeInt(i);
	}

	/**
	 * Method to write a float (4 bytes) from the input stream and return it.
	 */
	private void writeFloat(float f)
		throws IOException
	{
		dataOutputStream.writeFloat(f);
	}

	/**
	 * Method to write a double (8 bytes) from the input stream and return it.
	 */
	private void writeDouble(double d)
		throws IOException
	{
		dataOutputStream.writeDouble(d);
	}

	/**
	 * Method to write an short (2 bytes) from the input stream and return it.
	 */
	private void writeSmallInteger(short s)
		throws IOException
	{
		dataOutputStream.writeShort(s);
	}

	/**
	 * Method to write a string from the input stream and return it.
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
