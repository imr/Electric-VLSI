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
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.ELIBConstants;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * This class writes files in binary (.elib) format.
 */
public class ELIB extends Output
{
	/** map with "next in cell group" pointers */				private HashMap<Cell,Cell> cellInSameGroup = new HashMap<Cell,Cell>();
	/** true to write a 6.XX compatible library (MAGIC11) */	private boolean compatibleWith6;
	/** map to assign indices to cell names (for 6.XX) */		private TreeMap<String,Integer> cellIndexMap = new TreeMap<String,Integer>(TextUtils.STRING_NUMBER_ORDER);

	ELIB()
	{
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
		gatherReferencedObjects(lib);

		int magic = ELIBConstants.MAGIC13;
		if (compatibleWith6) magic = ELIBConstants.MAGIC11;
		writeBigInteger(magic);
		writeByte((byte)2);		// size of Short
		writeByte((byte)4);		// size of Int
		writeByte((byte)1);		// size of Char

		// count and number the cells, nodes, arcs, and ports in this library
		int nodeIndex = 0;
		int portProtoIndex = 0;
		int nodeProtoIndex = 0;
		int arcIndex = 0;
		HashSet<Cell.CellGroup> cellGroups = new HashSet<Cell.CellGroup>();
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			putObjIndex(cell, nodeProtoIndex++);
			for (Iterator<PortProto> pit = cell.getPorts(); pit.hasNext(); )
			{
				Export e = (Export)pit.next();
				putObjIndex(e, portProtoIndex++);
			}
			for (Iterator<NodeInst> nit = cell.getNodes(); nit.hasNext(); )
			{
				NodeInst ni = nit.next();
				putObjIndex(ni, nodeIndex++);
			}
			for (Iterator<ArcInst> ait = cell.getArcs(); ait.hasNext(); )
			{
				ArcInst ai = ait.next();
				putObjIndex(ai, arcIndex++);
			}

			// convert cellGroups to "next in cell group" pointers
			Cell.CellGroup cellGroup = cell.getCellGroup();
			if (!cellGroups.contains(cellGroup))
			{
				cellGroups.add(cellGroup);
				// mark the group with "next" pointers
				Iterator<Cell> git = cellGroup.getCells();
				Cell firstCellInGroup = git.next();
				Cell lastCellInGroup = firstCellInGroup;
				while (git.hasNext())
				{
					Cell cellInGroup = git.next();
					cellInSameGroup.put(lastCellInGroup, cellInGroup);
					lastCellInGroup = cellInGroup;
				}
				cellInSameGroup.put(lastCellInGroup, firstCellInGroup);
			}

			// gather proto name if creating version-6-compatible output
			if (compatibleWith6)
			{
				String protoName = cell.getName();
				if (!cellIndexMap.containsKey(protoName))
					cellIndexMap.put(protoName, null);
			}
		}
		int cellsHere = nodeProtoIndex;

		// count and number the cells in other libraries
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library olib = it.next();
			if (olib == lib) continue;
			if (!objInfo.containsKey(olib));
			for(Iterator<Cell> cit = olib.getCells(); cit.hasNext(); )
			{
				Cell cell = cit.next();
				if (!objInfo.containsKey(cell)) continue;
				putObjIndex(cell, nodeProtoIndex++);
				for (Iterator<PortProto> pit = cell.getPorts(); pit.hasNext(); )
				{
					Export e = (Export)pit.next();
					putObjIndex(e, portProtoIndex++);
				}

				// gather proto name if creating version-6-compatible output
				if (compatibleWith6)
				{
					String protoName = cell.getName();
					if (!cellIndexMap.containsKey(protoName))
						cellIndexMap.put(protoName, null);
				}
			}
		}

		// count the number of technologies and primitives
		int techCount = 0;
		int primNodeProtoIndex = 0;
		int primPortProtoIndex = 0;
		int arcProtoIndex = 0;
		int[] primNodeCounts = new int[Technology.getNumTechnologies()];
		int[] primArcCounts = new int[Technology.getNumTechnologies()];
		for (Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			if (!objInfo.containsKey(tech)) continue;
			int primNodeStart = primNodeProtoIndex;
			for (Iterator<PrimitiveNode> nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode np = nit.next();
				if (!objInfo.containsKey(np)) continue;
				putObjIndex(np, -2 - primNodeProtoIndex++);
				for (Iterator<PortProto> pit = np.getPorts(); pit.hasNext(); )
				{
					PrimitivePort pp = (PrimitivePort)pit.next();
					putObjIndex(pp, -2 - primPortProtoIndex++);
				}
			}
			primNodeCounts[techCount] = primNodeProtoIndex - primNodeStart;
			int primArcStart = arcProtoIndex;
			for(Iterator<ArcProto> ait = tech.getArcs(); ait.hasNext(); )
			{
				ArcProto ap = ait.next();
				if (!objInfo.containsKey(ap)) continue;
				putObjIndex(ap, -2 - arcProtoIndex++);
			}
			primArcCounts[techCount] = arcProtoIndex - primArcStart;
			techCount++;
		}

		// count the number of tools
		int toolCount = 0;
		for (Iterator<Tool> it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = it.next();
			if (!objInfo.containsKey(tool)) continue;
			toolCount++;
		}

		// write number of objects
		writeBigInteger(toolCount);
		writeBigInteger(techCount);
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
			for(Map.Entry<String,Integer> e : cellIndexMap.entrySet())
			{
				e.setValue(new Integer(cellCount++));
			}
			writeBigInteger(cellCount);
		}

		// write the current cell
		writeObj(null);
//		writeObj(lib.getCurCell());

		// write the version number
		writeString(Version.getVersion().toString());

		// number the views and write nonstandard ones
		putObjIndex(View.UNKNOWN, -1);
		putObjIndex(View.LAYOUT, -2);
		putObjIndex(View.SCHEMATIC, -3);
		putObjIndex(View.ICON, -4);
		putObjIndex(View.DOCWAVE, -5);				// unknown in C
		putObjIndex(View.LAYOUTSKEL, -6);			// unknown in C
		putObjIndex(View.VHDL, -7);
		putObjIndex(View.NETLIST, -8);
		putObjIndex(View.DOC, -9);
		putObjIndex(View.NETLISTNETLISP, -10);		// unknown in C
		putObjIndex(View.NETLISTALS, -11);			// unknown in C
		putObjIndex(View.NETLISTQUISC, -12);		// unknown in C
		putObjIndex(View.NETLISTRSIM, -13);			// unknown in C
		putObjIndex(View.NETLISTSILOS, -14);		// unknown in C
		putObjIndex(View.VERILOG, -15);
		List<View> viewsToSave = new ArrayList<View>();
		for(Iterator<View> it = View.getViews(); it.hasNext(); )
		{
			View view = it.next();
			if (objInfo.get(view) != null) continue;
			if (!objInfo.containsKey(view)) continue;
			viewsToSave.add(view);
			putObjIndex(view, viewsToSave.size());
		}
		writeBigInteger(viewsToSave.size());
		for(View view : viewsToSave)
		{
			writeString(view.getFullName());
			writeString(view.getAbbreviation());
		}

		// write total number of arcinsts, nodeinsts, and ports in each cell
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			writeBigInteger(cell.getNumArcs());
			writeBigInteger(cell.getNumNodes());
			writeBigInteger(cell.getNumPorts());
		}

		// write dummy numbers of arcinsts and nodeinst; count ports for external cells
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library olib = it.next();
			if (olib == lib) continue;
			if (!objInfo.containsKey(olib)) continue;
			for(Iterator<Cell> cit = olib.getCells(); cit.hasNext(); )
			{
				Cell cell = cit.next();
				if (!objInfo.containsKey(cell)) continue;
				writeBigInteger(-1);
				writeBigInteger(-1);
				writeBigInteger(cell.getNumPorts());
			}
		}

		// write the names of technologies and primitive prototypes
		techCount = 0;
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			if (!objInfo.containsKey(tech)) continue;

			// write the technology name
			writeString(tech.getTechName());

			// write the primitive node prototypes
			writeBigInteger(primNodeCounts[techCount]);
			for(Iterator<PrimitiveNode> nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode np = nit.next();
				if (!objInfo.containsKey(np)) continue;

				// write the primitive node prototype name
				writeString(np.getName());
				writeBigInteger(np.getNumPorts());
				for(Iterator<PortProto> pit = np.getPorts(); pit.hasNext(); )
				{
					PrimitivePort pp = (PrimitivePort)pit.next();
					writeString(pp.getName());
				}
			}

			// write the primitive arc prototype names
			writeBigInteger(primArcCounts[techCount]);
			for(Iterator<ArcProto> ait = tech.getArcs(); ait.hasNext(); )
			{
				ArcProto ap = ait.next();
				if (!objInfo.containsKey(ap)) continue;
				writeString(ap.getName());
			}
			techCount++;
		}

		// write the names of the tools
		for(Iterator<Tool> it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = it.next();
			if (!objInfo.containsKey(tool)) continue;
			writeString(tool.getName());
		}

		// write the userbits for the library
		writeBigInteger(0);
		//writeBigInteger(lib.lowLevelGetUserBits());

		// write the tool scale values
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			if (!objInfo.containsKey(tech)) continue;
			writeBigInteger((int)Math.round(tech.getScale()*2));
		}

		// write the global namespace
		writeNameSpace();

		// write the library variables and font association that preserves the font names
		writeVariables(lib, 0);

		// write the tool variables
		for(Iterator<Tool> it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = it.next();
			if (!objInfo.containsKey(tool)) continue;
			writeMeaningPrefs(tool);
		}

		// write the variables on technologies
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			if (!objInfo.containsKey(tech)) continue;
			writeMeaningPrefs(tech);
		}

		// write the dummy primitive variables
		int numDummyVariables = arcProtoIndex + primNodeProtoIndex + primPortProtoIndex;
		for (int i = 0; i < numDummyVariables; i++) writeNoVariables();

		// write the dummy view variables
		writeBigInteger(0);
// 		writeBigInteger(View.getNumViews());
// 		for(Iterator it = View.getViews(); it.hasNext(); )
// 		{
// 			View view = it.next();
// 			writeObj(view);
// 			writeNoVariables();
// 		}

		// write cells if creating version-6-compatible output
		if (compatibleWith6)
		{
			String [] cellNames = new String[cellCount];
			for(String cellName : cellIndexMap.keySet())
			{
				writeString(cellName);
				writeNoVariables();
			}
		}

		// write all of the cells in this library
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			writeNodeProto(cell, true);
		}

		// write all of the cells in external libraries
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library olib = it.next();
			if (olib == lib) continue;
			if (!objInfo.containsKey(olib)) continue;
			for(Iterator<Cell> cit = olib.getCells(); cit.hasNext(); )
			{
				Cell cell = cit.next();
				if (!objInfo.containsKey(cell)) continue;
				writeNodeProto(cell, false);
			}
		}

		// write all of the arcs and nodes in this library
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			for(Iterator<ArcInst> ait = cell.getArcs(); ait.hasNext(); )
			{
				ArcInst ai = ait.next();
				writeArcInst(ai);
			}
			for(Iterator<NodeInst> nit = cell.getNodes(); nit.hasNext(); )
			{
				NodeInst ni = nit.next();
				writeNodeInst(ni);
			}
		}
		// library written successfully
		return false;
	}

	// --------------------------------- OBJECT CONVERSION ---------------------------------

	private void writeNodeProto(Cell cell, boolean thislib)
		throws IOException
	{
		if (compatibleWith6)
		{
			// write cell index if creating version-6-compatible output
			Integer cellIndex = cellIndexMap.get(cell.getName());
			writeBigInteger(cellIndex.intValue());
		} else
		{
			// write cell information
			writeString(cell.getName());
	
			// write the "next in cell group" pointer
			writeObj(cellInSameGroup.get(cell));
	
			// write the "next in continuation" pointer
			writeObj(null);
		}

		// write the view information
		writeObj(cell.getView());
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
			Library instlib = cell.getLibrary();
			URL fileUrl = instlib.getLibFile();
            String filePath = fileUrl != null ? fileUrl.getPath() : instlib.getName();
			writeString(filePath);
		}

		// write the number of portprotos on this nodeproto
		writeBigInteger(cell.getNumPorts());

		// write the portprotos on this nodeproto
		for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			if (thislib)
			{
				PortInst pi = pp.getOriginalPort();
				// write the connecting subnodeinst for this portproto
				writeObj(pi.getNodeInst());
				// write the portproto index in the subnodeinst
				writeObj(pi.getPortProto());
			}

			// write the portproto name
			writeString(pp.getName());

			if (thislib)
			{
				// write the text descriptor
				writeTextDescriptor(pp.getTextDescriptor(Export.EXPORT_NAME), true);

				// write the portproto tool information
				writeBigInteger(pp.getElibBits());

				// write variable information
				writeVariables(pp, 0);
			}
		}

		if (thislib)
		{
			// write tool information
			writeBigInteger(0);		// was "adirty"
			writeBigInteger(cell.lowLevelGetUserbits() & ELIBConstants.CELL_BITS);
//			writeBigInteger(cell.lowLevelGetUserbits());

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
		writeObj(np);
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
			Orientation or = ni.getOrient();
			rotation = or.getCAngle();
			transpose = or.isCTranspose() ? 1 : 0;

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

		TextDescriptor td = np instanceof Cell ? ni.getTextDescriptor(NodeInst.NODE_PROTO) : null;
		writeTextDescriptor(td, true);

		// sort the arc connections by their PortInst ordering
		int numConnections = ni.getNumConnections();
		writeBigInteger(numConnections);
		if (numConnections > 0)
		{
			// must write connections in proper order
			List<Connection> sortedList = new ArrayList<Connection>();
			for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
				sortedList.add(it.next());
			Collections.sort(sortedList, TextUtils.CONNECTIONS_ORDER);

			for(Connection con : sortedList)
			{
				ArcInst ai = con.getArc();
				int i = objInfo.get(ai).intValue() << 1;
				if (con.getEndIndex() == ArcInst.HEADEND) i++;
				writeBigInteger(i);

				// write the portinst prototype
				writeObj(con.getPortInst().getPortProto());

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
			List<Export> sortedList = new ArrayList<Export>();
			for(Iterator<Export> it = ni.getExports(); it.hasNext(); )
				sortedList.add(it.next());
			Collections.sort(sortedList, EXPORTS_ORDER);

			for(Export pp : sortedList)
			{
				writeObj(pp);

				// write the portinst prototype
				writeObj(pp.getOriginalPort().getPortProto());

				// write the variable information
				writeNoVariables();
			}
		}

		// write the tool information
		writeBigInteger(ni.getD().getElibBits());

		// write variable information and arc name
		writeVariables(ni, tech.getScale()*2);
	}

	private void writeArcInst(ArcInst ai)
		throws IOException
	{
		// write the arcproto pointer
		writeObj(ai.getProto());

		// write basic arcinst information
		Technology tech = ai.getParent().getTechnology();
		writeBigInteger((int)Math.round(ai.getWidth() * tech.getScale()*2));

		// write the arcinst tail information
		Point2D location = ai.getTailLocation();
		writeBigInteger((int)Math.round(location.getX() * tech.getScale()*2));
		writeBigInteger((int)Math.round(location.getY() * tech.getScale()*2));
		writeObj(ai.getTailPortInst().getNodeInst());

		// write the arcinst head information
		location = ai.getHeadLocation();
		writeBigInteger((int)Math.round(location.getX() * tech.getScale()*2));
		writeBigInteger((int)Math.round(location.getY() * tech.getScale()*2));
		writeObj(ai.getHeadPortInst().getNodeInst());

		// write the arcinst's tool information
		int userBits = ai.getD().getElibBits();
		writeBigInteger(userBits);

		// write variable information and arc name
		writeVariables(ai, 0);
	}

	// --------------------------------- VARIABLES ---------------------------------

	/**
	 * Method to write the global namespace.  returns true upon error
	 */
	private void writeNameSpace()
		throws IOException
	{
		if (nameSpace.size() > Short.MAX_VALUE)
		{
			Job.getUserInterface().showErrorMessage(new String [] {"ERROR! Too many unique variable names",
               "The ELIB format cannot handle this many unique variables names", "Either delete the excess variables, or save to a readable dump"},
               "Error saving ELIB file");
            throw new IOException("Variable.Key index too large");
		}
		writeBigInteger(nameSpace.size());
		short keyIndex = 0;
		for(String str : nameSpace.keySet())
			writeString(str);
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
	private void writeVariables(ElectricObject obj, double scale)
		throws IOException
	{
		// write the number of Variables
		int count = obj.getNumVariables();
		Variable.Key additionalVarKey = null;
		int additionalVarType = ELIBConstants.VSTRING;
		Object additionalVarValue = null;
		if (obj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)obj;
			for (Iterator<PortInst> pit = ni.getPortInsts(); pit.hasNext(); )
			{
				PortInst pi = pit.next();
                count += pi.getNumVariables();
			}
			additionalVarKey = NodeInst.NODE_NAME;
			if (ni.isUsernamed()) additionalVarType |= ELIBConstants.VDISPLAY;
			additionalVarValue = ni.getName();
		} else if (obj instanceof ArcInst)
		{
			ArcInst ai = (ArcInst)obj;
			additionalVarKey = ArcInst.ARC_NAME;
			if (ai.isUsernamed()) additionalVarType |= ELIBConstants.VDISPLAY;
			additionalVarValue = ai.getName();
		} else if (obj instanceof Library)
		{
			String[] fontAssociation = createFontAssociation();
			if (fontAssociation != null)
			{
				additionalVarKey = Library.FONT_ASSOCIATIONS;
				additionalVarType |= ELIBConstants.VISARRAY | (fontAssociation.length << ELIBConstants.VLENGTHSH);
				additionalVarValue = fontAssociation;
			}
		}
		if (additionalVarKey != null) count++;

		writeBigInteger(count);

		// write the variables
		for(Iterator<Variable> it = obj.getVariables(); it.hasNext(); )
		{
			Variable var = it.next();
			writeVariable(obj, var, scale);
		}

		// write variables on PortInsts
		if (obj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)obj;
			for (Iterator<PortInst> pit = ni.getPortInsts(); pit.hasNext(); )
			{
				PortInst pi = pit.next();
				if (pi.getNumVariables() == 0) continue;
				for (Iterator<Variable> it = pi.getVariables(); it.hasNext(); )
				{
					Variable var = it.next();
						writeVariable(pi, var, scale);
				}
			}
		}

		// write the additional variable
		if (additionalVarKey != null)
		{
			writeVariableName(additionalVarKey.getName());
			writeBigInteger(additionalVarType);
			TextDescriptor td = (additionalVarType & ELIBConstants.VDISPLAY) != 0 ? obj.getTextDescriptor(additionalVarKey) : null;
			writeTextDescriptor(td, true);
            if (additionalVarValue instanceof Object[])
            {
                Object [] objList = (Object [])additionalVarValue;
                int len = objList.length;
                writeBigInteger(len);
                for(int i=0; i<len; i++)
                {
                    Object oneObj = objList[i];
                    putOutVar(oneObj);
                }
            } else
            {
                putOutVar(additionalVarValue);
            }
		}
	}

	/**
	 * Method to write an object variables.
     * @param owner owner of the Variabkle
     * @param var variable
     * @param scale of coordiantes.
	 */
	private void writeVariable(ElectricObject owner, Variable var, double scale)
		throws IOException
	{
		// create the "type" field
		Object varObj = var.getObjectInDatabase(EDatabase.serverDatabase());
		writeVariableName(diskName(owner, var));
		int type = var.getTextDescriptor().getCFlags();
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
			System.out.println("Variable " + var + " on " + owner + " is not a string. Language bits are cleared.");
			type &= ~(ELIBConstants.VCODE1|ELIBConstants.VCODE2);
		}

		// special case for "trace" information on NodeInsts
		if (owner instanceof NodeInst && var.getKey() == NodeInst.TRACE && varObj instanceof EPoint[])
		{
			EPoint [] points = (EPoint [])varObj;
			type = var.getTextDescriptor().getCFlags();
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
			System.out.println("Wrote Type 0 for Variable "+ var + ", value " + varObj);
		}
		writeBigInteger(type);

		// write the text descriptor
		writeTextDescriptor(var.getTextDescriptor(), var.isDisplay());

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

	/**
	 * Method to write a set of meaning preferences.
	 */
	private void writeMeaningPrefs(Object obj)
		throws IOException
	{
		List<Pref> prefs = Pref.getMeaningVariables(obj);
		writeBigInteger(prefs.size());
		for(Pref pref : prefs)
		{
			writeVariableName(pref.getPrefName());

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
			writeObj(obj);
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
			writeObj(obj);
			return;
		}
		if (obj instanceof ArcInst)
		{
			writeObj(obj);
			return;
		}
		if (obj instanceof NodeProto)
		{
			writeObj(obj);
			return;
		}
		if (obj instanceof ArcProto)
		{
			writeObj(obj);
			return;
		}
		if (obj instanceof PortProto)
		{
			writeObj(obj);
			return;
		}
		System.out.println("Error: Cannot write objects of type " + obj.getClass());
	}

	/**
	 * Method to write a text descriptor.
	 * Face of text descriptor is mapped according to "faceMap".
	 * @param td TextDescriptor to write or null
	 * @param isDisplay true of text is displayed
	 */
	private void writeTextDescriptor(TextDescriptor td, boolean isDisplay)
		throws IOException
	{
		int td0;
		int td1;
		if (td != null)
		{
			td0 = td.lowLevelGet0();
			td1 = td.lowLevelGet1();
            // Convert font face
            if ((td1 & ELIBConstants.VTFACE) != 0) {
                int face = (td1 & ELIBConstants.VTFACE) >> ELIBConstants.VTFACESH;
                td1 = (td1 & ~ELIBConstants.VTFACE) | (faceMap[face] << ELIBConstants.VTFACESH);
            }
		} else
		{
			td0 = 0;
			td1 = 0;
		}
		writeBigInteger(td0);
		writeBigInteger(td1);
	}

	/**
	 * Method to write a disk index of Object.
	 * Index is obtained fron objInfo map.
	 * @param obj Object to write
	 */
	private void writeObj(Object obj)
		throws IOException
	{
		int objIndex = -1;
		if (obj != null)
			objIndex = objInfo.get(obj).intValue();
		writeBigInteger(objIndex);
	}

	/**
	 * Method to write a disk index of variable name.
	 * Index is obtained from the nameSpace map.
	 * @param name Variable Key to write
	 */
	private void writeVariableName(String name)
		throws IOException
	{
		short varNameIndex = nameSpace.get(name).shortValue();
		writeSmallInteger(varNameIndex);
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
