/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ReadableDump.java
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

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.ELIBConstants;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/** 
 * Class to write a library to disk in Readable Dump format.
 */
public class ReadableDump extends Output
{
	private int nodeInstError, portProtoError, arcInstError, typeError;
	private HashMap cellOrdering;
	private HashMap cellGrouping;
	private HashMap nodeMap;
	private HashMap arcMap;
	private HashMap portMap;
	private int cellNumber;
	private Cell [] cells;

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

		// determine proper library order
		cellOrdering = new HashMap();
		cellGrouping = new HashMap();
		for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library oLib = (Library)lIt.next();
			for(Iterator it = oLib.getCells(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				cellOrdering.put(cell, new DBMath.MutableInteger(-1));
			}
		}
		cellNumber = 0;
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			if (cell.getNumUsagesIn() == 0) textRecurse(cell);
		}
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			DBMath.MutableInteger mi = (DBMath.MutableInteger)cellOrdering.get(cell);
			if (mi == null || mi.intValue() < 0) textRecurse(cell);
		}
		if (cellNumber > 0)
		{
			cells = new Cell[cellNumber];
			for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
			{
				Library oLib = (Library)lIt.next();
				for(Iterator it = oLib.getCells(); it.hasNext(); )
				{
					Cell cell = (Cell)it.next();
					DBMath.MutableInteger mi = (DBMath.MutableInteger)cellOrdering.get(cell);
					if (mi.intValue() >= 0)
						cells[mi.intValue()] = cell;
				}
			}
		}

		// determine cell groupings
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			cellGrouping.put(cell, new DBMath.MutableInteger(0));
		}
		int cellGroup = 0;
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			DBMath.MutableInteger mi = (DBMath.MutableInteger)cellGrouping.get(cell);
			if (mi == null || mi.intValue() != 0) continue;
			cellGroup++;
			for(Iterator gIt = cell.getCellGroup().getCells(); gIt.hasNext(); )
			{
				Cell oCell = (Cell)gIt.next();
				mi = (DBMath.MutableInteger)cellGrouping.get(oCell);
				mi.setValue(cellGroup);
			}
		}

		// write header information
		printWriter.print("****library: \"" + lib.getName() + "\"\n");
		printWriter.print("version: " + Version.getVersion() + "\n");
		printWriter.print("aids: " + Tool.getNumTools() + "\n");
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			printWriter.print("aidname: " + tool.getName() + "\n");
			writeVars(tool, null);
		}
		printWriter.print("userbits: " + lib.lowLevelGetUserBits() + "\n");
		printWriter.print("techcount: " + Technology.getNumTechnologies() + "\n");
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			printWriter.print("techname: " + tech.getTechName() + " lambda: " + (int)(tech.getScale()*2) + "\n");
			writeVars(tech, null);
		}
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View v = (View)it.next();
			printWriter.print("view: " + v.getFullName() + "{" + v.getAbbreviation() + "}\n");
		}
		printWriter.print("cellcount: " + cellNumber + "\n");
		Cell curCell = lib.getCurCell();
		if (curCell != null)
		{
			DBMath.MutableInteger mi = (DBMath.MutableInteger)cellOrdering.get(curCell);
			printWriter.print("maincell: " + mi.intValue() + "\n");
		}

		// write variables on the library
		writeVars(lib, null);

		// write the rest of the database
		for(int i = 0; i < cellNumber; i++)
		{
			// write the nodeproto name
			Cell cell = cells[i];
			DBMath.MutableInteger mi = (DBMath.MutableInteger)cellOrdering.get(cell);
			int groupIndex = 0;
			DBMath.MutableInteger mig = (DBMath.MutableInteger)cellGrouping.get(cell);
			if (mig != null) groupIndex = mig.intValue();
			printWriter.print("***cell: " + mi.intValue() + "/" + groupIndex + "\n");
			printWriter.print("name: " + cell.getName());
			if (cell.getView().getAbbreviation().length() > 0)
				printWriter.print("{" + cell.getView().getAbbreviation() + "}");
			printWriter.print("\n");
			printWriter.print("version: " + cell.getVersion() + "\n");
			printWriter.print("creationdate: " + ELIBConstants.dateToSeconds(cell.getCreationDate()) + "\n");
			printWriter.print("revisiondate: " + ELIBConstants.dateToSeconds(cell.getRevisionDate()) + "\n");

			// write the nodeproto bounding box
			Rectangle2D bounds = cell.getBounds();
			Technology tech = cell.getTechnology();
			double scale = tech.getScale() * 2;
			int lowX = (int)(bounds.getMinX() * scale);
			int highX = (int)(bounds.getMaxX() * scale);
			int lowY = (int)(bounds.getMinY() * scale);
			int highY = (int)(bounds.getMaxY() * scale);
			printWriter.print("lowx: " + lowX + " highx: " + highX +
				" lowy: " + lowY + " highy: " + highY + "\n");

			// cells in external libraries mention the library and stop
			if (cell.getLibrary() != lib)
			{
				printWriter.print("externallibrary: \"" + cell.getLibrary().getLibFile().getFile() + "\"\n");
				continue;
			}

			// write tool information
			printWriter.print("aadirty: 0\n");
			printWriter.print("userbits: " + cell.lowLevelGetUserbits() + "\n");

			// count and number the nodes, arcs, and ports
			nodeMap = new HashMap();
			arcMap = new HashMap();
			portMap = new HashMap();
			int nodeCount = 0, arcCount = 0, portCount = 0;
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				nodeMap.put(ni, new Integer(nodeCount++));
			}
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				arcMap.put(ai, new Integer(arcCount++));
			}
			for(Iterator it = cell.getPorts(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				portMap.put(pp, new Integer(portCount++));
			}
			printWriter.print("nodes: " + cell.getNumNodes() + " arcs: " + cell.getNumArcs() +
				" porttypes: " + cell.getNumPorts() + "\n");

			// write variables on the cell
			writeVars(cell, cell);

			// write the nodes in this cell
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				NodeProto np = ni.getProto();
				Integer nodeIndex = (Integer)nodeMap.get(ni);
				printWriter.print("**node: " + nodeIndex.intValue() + "\n");
				if (np instanceof Cell)
				{
					DBMath.MutableInteger subMi = (DBMath.MutableInteger)cellOrdering.get(np);
					printWriter.print("type: [" + subMi.intValue() + "]\n");
				} else
				{
					printWriter.print("type: " + np.getTechnology().getTechName() + ":" +
					np.getName() + "\n");
				}
				if (np instanceof Cell)
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
				printWriter.print("lowx: " + lowX + " highx: " + highX + " lowy: " + lowY + " highy: " + highY + "\n");
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
				printWriter.print("rotation: " + angle + " transpose: " + transpose + "\n");
				if (np instanceof Cell)
				{
					printWriter.print("descript: " + ni.getProtoTextDescriptor().lowLevelGet0() + "/" +
						ni.getProtoTextDescriptor().lowLevelGet0() + "\n");
				}
				printWriter.print("userbits: " + ni.lowLevelGetUserbits() + "\n");
				writeVars(ni, cell);

				for(Iterator pIt = np.getPorts(); pIt.hasNext(); )
				{
					PortProto pp = (PortProto)pIt.next();
					boolean found = false;
					for(Iterator aIt = ni.getConnections(); aIt.hasNext(); )
					{
						Connection con = (Connection)aIt.next();
						if (con.getPortInst().getPortProto() == pp)
						{
							if (!found)
							{
								printWriter.print("*port: " + pp.getName() + "\n");
								found = true;
							}
							Integer aIndex = (Integer)arcMap.get(con.getArc());
							if (aIndex == null) aIndex = new Integer(-1);
							printWriter.print("arc: " + aIndex.intValue() + "\n");
						}
					}
					for(Iterator eIt = ni.getExports(); eIt.hasNext(); )
					{
						Export e = (Export)eIt.next();
						if (e.getOriginalPort().getPortProto() == pp)
						{
							if (!found)
							{
								printWriter.print("*port: " + pp.getName() + "\n");
								found = true;
							}
							Integer pIndex = (Integer)portMap.get(e);
							if (pIndex == null) pIndex = new Integer(-1);
							printWriter.print("exported: " + pIndex.intValue() + "\n");
						}
					}
				}
			}

			// write the portprotos in this cell
			int poc = 0;
			for(Iterator it = cell.getPorts(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				printWriter.print("**porttype: " + poc + "\n");
				poc++;
				NodeInst subNi = pp.getOriginalPort().getNodeInst();
				Integer subNodeIndex = (Integer)nodeMap.get(subNi);
				PortProto subPp = pp.getOriginalPort().getPortProto();
				printWriter.print("subnode: " + subNodeIndex.intValue() + "\n");
				printWriter.print("subport: " + subPp.getName() + "\n");
				printWriter.print("name: " + pp.getName() + "\n");

				// need to write both words
				TextDescriptor td = pp.getTextDescriptor();
				printWriter.print("descript: " + td.lowLevelGet0() + "/" + td.lowLevelGet1() + "\n");
				printWriter.print("userbits: " + pp.lowLevelGetUserbits() + "\n");
				writeVars(pp, cell);
			}

			// write the arcs in this cell
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				Integer arcIndex = (Integer)arcMap.get(ai);
				printWriter.print("**arc: " + arcIndex.intValue() + "\n");
				printWriter.print("type: " + ai.getProto().getTechnology().getTechName() + ":" +
					ai.getProto().getName() + "\n");
				int width = (int)(ai.getWidth() * scale);
				int length = (int)(ai.getLength() * scale);
				printWriter.print("width: " + width + " length: " + length + "\n");

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
				printWriter.print("userbits: " + userBits + "\n");
				for(int e=0; e<2; e++)
				{
					Connection con = ai.getConnection(e);
					NodeInst conNi = con.getPortInst().getNodeInst();
					Integer conNodeIndex = (Integer)nodeMap.get(conNi);
					printWriter.print("*end: " + e + "\n");
					printWriter.print("node: " + conNodeIndex.intValue() + "\n");
					printWriter.print("nodeport: " + con.getPortInst().getPortProto().getName() + "\n");
					int endX = (int)(con.getLocation().getX() * scale);
					int endY = (int)(con.getLocation().getY() * scale);
					printWriter.print("xpos: " + endX + " ypos: " + endY + "\n");
				}
				writeVars(ai, cell);
			}
			printWriter.print("celldone: " + cell.getName() + "\n");
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
		
		// clean up and return
		lib.clearChangedMinor();
		lib.clearChangedMajor();
		System.out.println(filePath + " written");
		return false;
	}

	/**
	 * Method to help order the library for proper nonforward references
	 * in the outout
	 */
	private void textRecurse(Cell cell)
	{
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (!(ni.getProto() instanceof Cell)) continue;
			Cell subCell = (Cell)ni.getProto();
			DBMath.MutableInteger mi = (DBMath.MutableInteger)cellOrdering.get(subCell);
			if (mi == null || mi.intValue() < 0) textRecurse(subCell);
		}

		// add this cell to the list
		DBMath.MutableInteger mi = (DBMath.MutableInteger)cellOrdering.get(cell);
		mi.setValue(cellNumber++);
	}

	/**
	 * Method to write the variables on an object.  The current cell is
	 * "curCell" such that any references to objects in a cell must be in
	 * this cell.
	 */
	private void writeVars(ElectricObject eObj, Cell curCell)
	{
		// count the number of variables
		int i = 0;
		for(Iterator it = eObj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.isDontSave()) i++;
		}

		// add one more for Geometrics with names
		if (eObj instanceof Geometric && ((Geometric)eObj).getNameKey() != null)
			i++;
		if (i == 0) return;

		printWriter.print("variables: " + i + "\n");
		for(Iterator it = eObj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.isDontSave()) continue;
			int type = var.lowLevelGetFlags() & ~(ELIBConstants.VTYPE|ELIBConstants.VISARRAY|ELIBConstants.VLENGTH);
			Object varObj = var.getObject();

			// special case for "trace" information on NodeInsts
			if (eObj instanceof NodeInst && var.getKey() == NodeInst.TRACE && varObj instanceof Object[])
			{
				Object [] objList = (Object [])varObj;
				Point2D [] points = (Point2D [])objList;
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
				printWriter.print("(" + len + ")[0" + Integer.toOctalString(type) + ",0" +
					Integer.toOctalString(td.lowLevelGet0()) + "/0" + Integer.toOctalString(td.lowLevelGet1()) + "]: ");
			} else
			{
				int objType = ELIBConstants.getVarType(varObj);
				if (objType == ELIBConstants.VDOUBLE) objType = ELIBConstants.VFLOAT;
				type |= objType;
				printWriter.print("[0" + Integer.toOctalString(type) + ",0" +
					Integer.toOctalString(td.lowLevelGet0()) + "/0" + Integer.toOctalString(td.lowLevelGet1()) + "]: ");
			}
			printWriter.print(pt + "\n");
		}

		// write the node or arc name
		if (eObj instanceof Geometric && ((Geometric)eObj).getNameKey() != null)
		{
			Geometric geom = (Geometric)eObj;
			Variable.Key key = geom instanceof NodeInst ? NodeInst.NODE_NAME : ArcInst.ARC_NAME;
			int type = ELIBConstants.VSTRING;
			if (geom.isUsernamed()) type |= ELIBConstants.VDISPLAY;
			TextDescriptor td = geom.getNameTextDescriptor();
			printWriter.print("[0" + Integer.toOctalString(type) + ",0" +
				Integer.toOctalString(td.lowLevelGet0()) + "/0" + Integer.toOctalString(td.lowLevelGet1()) + "]: ");
			printWriter.print("\"" + convertString(geom.getName()) + "\"\n");
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
			Integer nodeIndex = (Integer)nodeMap.get(ni);
			int cIndex = -1;
			if (nodeIndex == null) nodeInstError++; else
				cIndex = nodeIndex.intValue();
			infstr.append(Integer.toString(cIndex));
			return;
		}
		if (obj instanceof ArcInst)
		{
			ArcInst ai = (ArcInst)obj;
			Integer arcIndex = (Integer)arcMap.get(ai);
			int cIndex = -1;
			if (arcIndex == null) arcInstError++; else
				cIndex = arcIndex.intValue();
			infstr.append(Integer.toString(cIndex));
			return;
		}
		if (obj instanceof Cell)
		{
			Cell cell = (Cell)obj;
			DBMath.MutableInteger mi = (DBMath.MutableInteger)cellOrdering.get(cell);
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
			Integer portIndex = (Integer)portMap.get(pp);
			int cIndex = -1;
			if (portIndex == null) portProtoError++; else
				cIndex = portIndex.intValue();
			infstr.append(Integer.toString(cIndex));
			return;
		}
		typeError++;
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
			if (pt == '^' || pt == '[' || pt == '(') printWriter.print("^");
			printWriter.print(pt);
		}
	}
}
