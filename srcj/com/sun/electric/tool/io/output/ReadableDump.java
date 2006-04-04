/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ReadableDump.java
 * Input/output tool: "Readable-Dump" Library output
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.ELIBConstants;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 
 * Class to write a library to disk in Readable Dump format.
 */
public class ReadableDump extends Output
{
	private int nodeInstError, portProtoError, arcInstError, typeError;
	private LinkedHashMap<Cell,Integer> cellOrdering = new LinkedHashMap<Cell,Integer>();
	private HashMap<Cell,DBMath.MutableInteger> cellGrouping;
	private HashMap<NodeInst,Integer> nodeMap;
	private HashMap<ArcInst,Integer> arcMap;
	private HashMap<Export,Integer> portMap;
//	private int cellNumber;
//	private Cell [] cells;

	ReadableDump()
	{
	}

	/**
	 * Method to write a Library in readable dump (.txt) format.
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
		// clear error counters
		nodeInstError = portProtoError = arcInstError = typeError = 0;

		gatherReferencedObjects(lib);

		// determine proper library order
		cellGrouping = new HashMap<Cell,DBMath.MutableInteger>();
		for(Iterator<Library> lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library oLib = lIt.next();
			if (oLib == lib) continue;
			if (!objInfo.containsKey(oLib)) continue;
			for(Iterator<Cell> it = oLib.getCells(); it.hasNext(); )
			{
				Cell cell = it.next();
				if (!objInfo.containsKey(cell)) continue;
				cellOrdering.put(cell, null);
//				cellOrdering.put(cell, new DBMath.MutableInteger(-1));
			}
		}
//		cellNumber = 0;
// 		for(Iterator it = lib.getCells(); it.hasNext(); )
// 		{
// 			Cell cell = it.next();
// 			if (cell.getNumUsagesIn() == 0) textRecurse(cell);
// 		}
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			if (!cellOrdering.containsKey(cell)) textRecurse(lib, cell);
// 			DBMath.MutableInteger mi = cellOrdering.get(cell);
// 			if (mi == null || mi.intValue() < 0) textRecurse(cell);
		}
		int cellNumber = 0;
		for (Map.Entry<Cell,Integer> e : cellOrdering.entrySet())
		{
			e.setValue(new Integer(cellNumber++));
		}
// 		if (cellNumber > 0)
// 		{
// 			cells = new Cell[cellNumber];
// 			for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
// 			{
// 				Library oLib = lIt.next();
// 				for(Iterator it = oLib.getCells(); it.hasNext(); )
// 				{
// 					Cell cell = it.next();
// 					DBMath.MutableInteger mi = cellOrdering.get(cell);
// 					if (mi.intValue() >= 0)
// 						cells[mi.intValue()] = cell;
// 				}
// 			}
// 		}

		// determine cell groupings
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			cellGrouping.put(cell, new DBMath.MutableInteger(0));
		}
		int cellGroup = 0;
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			DBMath.MutableInteger mi = cellGrouping.get(cell);
			if (mi == null || mi.intValue() != 0) continue;
			cellGroup++;
			for(Iterator<Cell> gIt = cell.getCellGroup().getCells(); gIt.hasNext(); )
			{
				Cell oCell = gIt.next();
				mi = cellGrouping.get(oCell);
				mi.setValue(cellGroup);
			}
		}

		int toolCount = 0;
		for(Iterator<Tool> it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = it.next();
			if (!objInfo.containsKey(tool)) continue;
			toolCount++;
		}
		int techCount = 0;
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			if (!objInfo.containsKey(tech)) continue;
			techCount++;
		}

		// write header information
		printWriter.println("****library: \"" + lib.getName() + "\"");
		printWriter.println("version: " + Version.getVersion());
		printWriter.println("aids: " + toolCount);
		for(Iterator<Tool> it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = it.next();
			if (!objInfo.containsKey(tool)) continue;
			printWriter.println("aidname: " + tool.getName());
			writeMeaningPrefs(tool);
		}
//		printWriter.println("userbits: " + lib.lowLevelGetUserBits());
		printWriter.println("techcount: " + techCount);
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			if (!objInfo.containsKey(tech)) continue;
			printWriter.println("techname: " + tech.getTechName() + " lambda: " + (int)(tech.getScale()*2));
			writeMeaningPrefs(tech);
		}
		for(Iterator<View> it = View.getViews(); it.hasNext(); )
		{
			View v = it.next();
			if (!objInfo.containsKey(v)) continue;
			printWriter.println("view: " + v.getFullName() + "{" + v.getAbbreviation() + "}");
		}
		printWriter.println("cellcount: " + cellNumber);
// 		Cell curCell = lib.getCurCell();
// 		if (curCell != null)
// 		{
// 			printWriter.println("maincell: " + cellOrdering.get(curCell).intValue());
// 			//DBMath.MutableInteger mi = cellOrdering.get(curCell);
// 			//printWriter.println("maincell: " + mi.intValue());
// 		}

		// write variables on the library
		writeVars(lib, null);

		// write the rest of the database
		for (Map.Entry<Cell,Integer> entry : cellOrdering.entrySet())
		{
			Cell cell = (Cell)entry.getKey();
			int groupIndex = 0;
			DBMath.MutableInteger mig = cellGrouping.get(cell);
			if (mig != null) groupIndex = mig.intValue();
			printWriter.println("***cell: " + ((Integer)entry.getValue()).intValue() + "/" + groupIndex);
			printWriter.print("name: " + cell.getName());
			if (cell.getView().getAbbreviation().length() > 0)
				printWriter.print("{" + cell.getView().getAbbreviation() + "}");
			printWriter.println();
			printWriter.println("version: " + cell.getVersion());
			printWriter.println("creationdate: " + ELIBConstants.dateToSeconds(cell.getCreationDate()));
			printWriter.println("revisiondate: " + ELIBConstants.dateToSeconds(cell.getRevisionDate()));

			// write the nodeproto bounding box
			Rectangle2D bounds = cell.getBounds();
			Technology tech = cell.getTechnology();
			double scale = tech.getScale() * 2;
			int lowX = (int)(bounds.getMinX() * scale);
			int highX = (int)(bounds.getMaxX() * scale);
			int lowY = (int)(bounds.getMinY() * scale);
			int highY = (int)(bounds.getMaxY() * scale);
			printWriter.println("lowx: " + lowX + " highx: " + highX +
				" lowy: " + lowY + " highy: " + highY);

			// cells in external libraries mention the library and stop
			if (cell.getLibrary() != lib)
			{
				URL libUrl = cell.getLibrary().getLibFile();
				String libFile = libUrl != null ? libUrl.getFile() : cell.getLibrary().getName();
				printWriter.println("externallibrary: \"" + libFile + "\"");
				continue;
			}

			// write tool information
//			printWriter.println("aadirty: 0");
//			printWriter.println("userbits: " + cell.lowLevelGetUserbits());
			printWriter.println("userbits: " + (cell.lowLevelGetUserbits() & ELIBConstants.CELL_BITS));

			// count and number the nodes, arcs, and ports
			nodeMap = new HashMap<NodeInst,Integer>();
			arcMap = new HashMap<ArcInst,Integer>();
			portMap = new HashMap<Export,Integer>();
			int nodeCount = 0, arcCount = 0, portCount = 0;
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				nodeMap.put(ni, new Integer(nodeCount++));
			}
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				arcMap.put(ai, new Integer(arcCount++));
			}
			for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				portMap.put(pp, new Integer(portCount++));
			}
			printWriter.println("nodes: " + cell.getNumNodes() + " arcs: " + cell.getNumArcs() +
				" porttypes: " + cell.getNumPorts());

			// write variables on the cell
			writeVars(cell, cell);

			// write the nodes in this cell
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				NodeProto np = ni.getProto();
				Integer nodeIndex = nodeMap.get(ni);
				printWriter.println("**node: " + nodeIndex.intValue());
				if (ni.isCellInstance())
				{
					Integer subMi = cellOrdering.get(np);
					//DBMath.MutableInteger subMi = cellOrdering.get(np);
					printWriter.println("type: [" + subMi.intValue() + "]");
				} else
				{
					printWriter.println("type: " + np.getTechnology().getTechName() + ":" + np.getName());
				}
				if (ni.isCellInstance())
				{
					lowX = (int)((ni.getTrueCenterX() - ni.getXSize()/2) * scale);
					highX = (int)((ni.getTrueCenterX() + ni.getXSize()/2) * scale);
					lowY = (int)((ni.getTrueCenterY() - ni.getYSize()/2) * scale);
					highY = (int)((ni.getTrueCenterY() + ni.getYSize()/2) * scale);
				} else
				{
					lowX = (int)((ni.getAnchorCenterX() - ni.getXSize()/2) * scale);
					highX = (int)((ni.getAnchorCenterX() + ni.getXSize()/2) * scale);
					lowY = (int)((ni.getAnchorCenterY() - ni.getYSize()/2) * scale);
					highY = (int)((ni.getAnchorCenterY() + ni.getYSize()/2) * scale);
				}
				printWriter.println("lowx: " + lowX + " highx: " + highX + " lowy: " + lowY + " highy: " + highY);
				int angle = ni.getAngle();
				int transpose = (ni.isXMirrored() != ni.isYMirrored()) ? 1 : 0;
				if (ni.isXMirrored())
				{
					if (ni.isYMirrored())
					{
						angle = (angle + 1800) % 3600;
					} else
					{
						angle = (angle + 900) % 3600;
					}
				} else if (ni.isYMirrored())
				{
					angle = (angle + 2700) % 3600;
				}
				printWriter.println("rotation: " + angle + " transpose: " + transpose);
				if (ni.isCellInstance())
					writeTextDescriptor(-1, ni.getTextDescriptor(NodeInst.NODE_PROTO));
				printWriter.println("userbits: " + ni.getD().getElibBits());
				writeVars(ni, cell);

				for(Iterator<PortProto> pIt = np.getPorts(); pIt.hasNext(); )
				{
					PortProto pp = pIt.next();
					ArrayList<Connection> sortedConnections = new ArrayList<Connection>();
					ArrayList<Export> sortedExports = new ArrayList<Export>();
					for(Iterator<Connection> aIt = ni.getConnections(); aIt.hasNext(); )
					{
						Connection con = aIt.next();
						if (con.getPortInst().getPortProto() == pp)
							sortedConnections.add(con);
					}
					Collections.sort(sortedConnections, TextUtils.CONNECTIONS_ORDER);
					for(Iterator<Export> eIt = ni.getExports(); eIt.hasNext(); )
					{
						Export e = eIt.next();
						if (e.getOriginalPort().getPortProto() == pp)
							sortedExports.add(e);
					}
					Collections.sort(sortedExports, EXPORTS_ORDER);
					if (sortedConnections.size() > 0 || sortedExports.size() > 0)
						printWriter.println("*port: " + pp.getName());
					for(Connection con : sortedConnections)
					{
						Integer aIndex = arcMap.get(con.getArc());
						if (aIndex == null) aIndex = new Integer(-1);
						printWriter.println("arc: " + aIndex.intValue());
					}
					for(Export e : sortedExports)
					{
						Integer pIndex = portMap.get(e);
						if (pIndex == null) pIndex = new Integer(-1);
						printWriter.println("exported: " + pIndex.intValue());
					}
				}
			}

			// write the portprotos in this cell
			int poc = 0;
			for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				printWriter.println("**porttype: " + poc);
				poc++;
				NodeInst subNi = pp.getOriginalPort().getNodeInst();
				Integer subNodeIndex = nodeMap.get(subNi);
				PortProto subPp = pp.getOriginalPort().getPortProto();
				printWriter.println("subnode: " + subNodeIndex.intValue());
				printWriter.println("subport: " + subPp.getName());
				printWriter.println("name: " + pp.getName());

				// need to write both words
				writeTextDescriptor(-1, pp.getTextDescriptor(Export.EXPORT_NAME));
				printWriter.println("userbits: " + pp.getElibBits());
				writeVars(pp, cell);
			}

			// write the arcs in this cell
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				Integer arcIndex = arcMap.get(ai);
				printWriter.println("**arc: " + arcIndex.intValue());
				printWriter.println("type: " + ai.getProto().getTechnology().getTechName() + ":" +
					ai.getProto().getName());
				int width = (int)(ai.getWidth() * scale);
				int length = (int)(ai.getLength() * scale);
				printWriter.println("width: " + width + " length: " + length);

				int userBits = ai.getD().getElibBits();
				printWriter.println("userbits: " + userBits);
				for(int e=0; e<2; e++)
				{
					NodeInst conNi = ai.getPortInst(e).getNodeInst();
					Integer conNodeIndex = nodeMap.get(conNi);
					printWriter.println("*end: " + e);
					printWriter.println("node: " + conNodeIndex.intValue());
					printWriter.println("nodeport: " + ai.getPortInst(e).getPortProto().getName());
					int endX = (int)(ai.getLocation(e).getX() * scale);
					int endY = (int)(ai.getLocation(e).getY() * scale);
					printWriter.println("xpos: " + endX + " ypos: " + endY);
				}
				writeVars(ai, cell);
			}
			printWriter.println("celldone: " + cell.getName());
		}

		// print any variable-related error messages
		if (nodeInstError != 0)
			System.out.println("Warning: " + nodeInstError + " node pointers point outside cell: not saved");
		if (arcInstError != 0)
			System.out.println("Warning: " + arcInstError + " arc pointers point outside cell: not saved");
		if (portProtoError != 0)
			System.out.println("Warning: " + portProtoError + " export pointers point outside cell: not saved");
		if (typeError != 0)
			System.out.println("Warning: " + typeError + " objects of unknown type could not be saved");
		
		return false;
	}

	/**
	 * Method to help order the library for proper nonforward references
	 * in the outout
	 */
	private void textRecurse(Library lib, Cell cell)
	{
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (!ni.isCellInstance()) continue;
			Cell subCell = (Cell)ni.getProto();
			if (subCell.getLibrary() != lib) continue;
			if (!cellOrdering.containsKey(subCell)) textRecurse(lib, subCell);
// 			DBMath.MutableInteger mi = cellOrdering.get(subCell);
// 			if (mi == null || mi.intValue() < 0) textRecurse(subCell);
		}

		// add this cell to the list
		cellOrdering.put(cell, null);
// 		DBMath.MutableInteger mi = cellOrdering.get(cell);
// 		mi.setValue(cellNumber++);
	}

	/**
	 * Method to write the variables on an object.  The current cell is
	 * "curCell" such that any references to objects in a cell must be in
	 * this cell.
	 */
	private void writeVars(ElectricObject obj, Cell curCell)
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

		if (count == 0) return;
		printWriter.println("variables: " + count + "");

		// write the variables
		for(Iterator<Variable> it = obj.getVariables(); it.hasNext(); )
		{
			Variable var = it.next();
			writeVar(obj, var, curCell);
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
						writeVar(pi, var, curCell);
				}
			}
		}

		// write the additional variable
		if (additionalVarKey != null)
		{
			printName(additionalVarKey.getName());
			TextDescriptor td = (additionalVarType & ELIBConstants.VDISPLAY) != 0 ? obj.getTextDescriptor(additionalVarKey) : null;
			writeTextDescriptor(additionalVarType, td);
			String pt = makeString(additionalVarValue, curCell);
			if (pt == null) pt = "";
			printWriter.println(pt);
		}
	}

	/**
	 * Method to write the variable.  The current cell is
	 * "curCell" such that any references to objects in a cell must be in
	 * this cell.
	 */
	private void writeVar(ElectricObject owner, Variable var, Cell curCell)
	{
		int type = var.getTextDescriptor().getCFlags();
        
		Object varObj = var.getObjectInDatabase(EDatabase.serverDatabase());

		// special case for "trace" information on NodeInsts
		if (owner instanceof NodeInst && var.getKey() == NodeInst.TRACE && varObj instanceof EPoint[])
		{
			EPoint [] points = (EPoint [])varObj;
			int len = points.length * 2;
			Float [] newPoints = new Float[len];
			for(int j=0; j<points.length; j++)
			{
				newPoints[j*2] = new Float(points[j].getX());
				newPoints[j*2+1] = new Float(points[j].getY());
			}
			varObj = newPoints;
		}

		String pt = makeString(varObj, curCell);
		if (pt == null) pt = "";
		printName(var.getKey().getName());
		TextDescriptor td = var.getTextDescriptor();

		if (varObj instanceof Object[])
		{
			Object [] objList = (Object [])varObj;
			int objType = ELIBConstants.getVarType(objList[0]);
			if (objType == ELIBConstants.VDOUBLE) objType = ELIBConstants.VFLOAT;
			int len = objList.length;
			type |= objType | ELIBConstants.VISARRAY | (len << ELIBConstants.VLENGTHSH);
		} else
		{
			int objType = ELIBConstants.getVarType(varObj);
			if (objType == ELIBConstants.VDOUBLE) objType = ELIBConstants.VFLOAT;
			type |= objType;
		}
		writeTextDescriptor(type, td);
		printWriter.println(pt);
	}

	/**
	 * Method to write the variables on an object.
	 */
	private void writeMeaningPrefs(Object obj)
	{
		// count the number of variables
		List<Pref> prefs = Pref.getMeaningVariables(obj);
		if (prefs.size() == 0) return;
		printWriter.println("variables: " + prefs.size());
		for(Pref pref : prefs)
		{
			Object varObj = pref.getValue();
			int objType = ELIBConstants.getVarType(varObj);
			if (objType == ELIBConstants.VDOUBLE) objType = ELIBConstants.VFLOAT;
			String pt = makeString(varObj, null);
			if (pt == null) pt = "";
			printWriter.println(pref.getPrefName() + "[0" + Integer.toOctalString(objType) + ",0/0]: " + pt);
		}
	}

	/**
	 * Method to convert variable "var" to a string for printing in the text file.
	 * returns zero on error
	 */
	private String makeString(Object obj, Cell curCell)
	{
		StringBuffer infstr = new StringBuffer();
		if (obj instanceof Object[])
		{
			Object [] objArray = (Object [])obj;
			int len = objArray.length;
			for(int i=0; i<len; i++)
			{
				Object oneObj = objArray[i];
				if (i == 0) infstr.append("["); else
					infstr.append(",");
				makeStringVar(infstr, oneObj, curCell);
			}
			infstr.append("]");
		} else makeStringVar(infstr, obj, curCell);
		return infstr.toString();
	}

	/**
	 * Method to make a string from the value in "addr" which has a type in
	 * "type".
	 */
	private void makeStringVar(StringBuffer infstr, Object obj, Cell curCell)
	{
		if (obj instanceof Integer)
		{
			infstr.append(((Integer)obj).intValue());
			return;
		}
		if (obj instanceof Short)
		{
			infstr.append(((Short)obj).shortValue());
			return;
		}
		if (obj instanceof Byte)
		{
			infstr.append(((Byte)obj).byteValue());
			return;
		}
		if (obj instanceof String)
		{
			infstr.append("\"");
			infstr.append(convertString((String)obj));
			infstr.append("\"");
			return;
		}
		if (obj instanceof Float)
		{
			infstr.append(((Float)obj).floatValue());
			return;
		}
		if (obj instanceof Double)
		{
			infstr.append(((Double)obj).doubleValue());
			return;
		}
		if (obj instanceof Boolean)
		{
			infstr.append(((Boolean)obj).booleanValue() ? 1 : 0);
			return;
		}
		if (obj instanceof Long)
		{
			infstr.append(((Long)obj).longValue());
			return;
		}
		if (obj instanceof Technology)
		{
			Technology tech = (Technology)obj;
			infstr.append(tech.getTechName());
			return;
		}
		if (obj instanceof Library)
		{
			Library lib = (Library)obj;
			infstr.append("\"" + lib.getName() + "\"");
			return;
		}
		if (obj instanceof Tool)
		{
			Tool tool = (Tool)obj;
			infstr.append(tool.getName());
			return;
		}
		if (obj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)obj;
			Integer nodeIndex = nodeMap.get(ni);
			int cIndex = -1;
			if (nodeIndex == null) nodeInstError++; else
				cIndex = nodeIndex.intValue();
			infstr.append(Integer.toString(cIndex));
			return;
		}
		if (obj instanceof ArcInst)
		{
			ArcInst ai = (ArcInst)obj;
			Integer arcIndex = arcMap.get(ai);
			int cIndex = -1;
			if (arcIndex == null) arcInstError++; else
				cIndex = arcIndex.intValue();
			infstr.append(Integer.toString(cIndex));
			return;
		}
		if (obj instanceof Cell)
		{
			Cell cell = (Cell)obj;
			Integer mi = cellOrdering.get(cell);
			//DBMath.MutableInteger mi = cellOrdering.get(cell);
			int cIndex = -1;
			if (mi != null) cIndex = mi.intValue();
			infstr.append(Integer.toString(cIndex));
			return;
		}
		if (obj instanceof PrimitiveNode)
		{
			PrimitiveNode np = (PrimitiveNode)obj;
			infstr.append(np.getTechnology().getTechName() + ":" + np.getName());
			return;
		}
		if (obj instanceof ArcProto)
		{
			ArcProto ap = (ArcProto)obj;
			infstr.append(ap.getTechnology().getTechName() + ":" + ap.getName());
			return;
		}
		if (obj instanceof Export)
		{
			Export pp = (Export)obj;
			Integer portIndex = portMap.get(pp);
			int cIndex = -1;
			if (portIndex == null) portProtoError++; else
				cIndex = portIndex.intValue();
			infstr.append(Integer.toString(cIndex));
			return;
		}
		typeError++;
	}

	/**
	 * Method to write a text descriptor (possibly wit variable bits).
	 * Face of text descriptor is mapped according to "faceMap".
	 * @param varBits variableBits or -1.
	 * @param td TextDescriptor to write.
	 */
	private void writeTextDescriptor(int varBits, TextDescriptor td)
	{
		int td0;
		int td1;
		if (td != null)
		{
			td0 = td.lowLevelGet0();
			td1 = td.lowLevelGet1();
            //Convert font face
			if ((td1 & ELIBConstants.VTFACE) != 0)
			{
				int face = (td1 & ELIBConstants.VTFACE) >> ELIBConstants.VTFACESH;
				td1 = (td1 & ~ELIBConstants.VTFACE) | (faceMap[face] << ELIBConstants.VTFACESH);
			}
		} else
		{
			td0 = 0;
			td1 = 0;
		}
		if (varBits == -1)
		{
			printWriter.println("descript: " + td0 + "/" + td1);
			return;
		}
		if ((varBits & ELIBConstants.VISARRAY) != 0)
			printWriter.print("(" + ((varBits & ELIBConstants.VLENGTH) >> ELIBConstants.VLENGTHSH) + ")");
		printWriter.print("[0" + Integer.toOctalString(varBits) +
						  ",0" + Integer.toOctalString(td0) +
						  "/0" + Integer.toOctalString(td1) + "]: ");
	}

	/**
	 * Method to add the string "str" to the infinite string and to quote the
	 * special characters '[', ']', '"', and '^'.
	 */
	private String convertString(String str)
	{
		StringBuffer infstr = new StringBuffer();
		int len = str.length();
		for(int i=0; i<len; i++)
		{
			char ch = str.charAt(i);
			if (ch == '[' || ch == ']' || ch == '"' || ch == '^')
				infstr.append('^');
			infstr.append(ch);
		}
		return infstr.toString();
	}

	/**
	 * Method to print the variable name in "name" on file "file".  The
	 * conversion performed is to quote with a backslash any of the characters
	 * '(', '[', or '^'.
	 */
	private void printName(String name)
	{
		int len = name.length();
		for(int i=0; i<len; i++)
		{
			char pt = name.charAt(i);
			if (pt == '^' || pt == '[' || pt == '(' || pt == ':') printWriter.print("^");
			printWriter.print(pt);
		}
	}
}
