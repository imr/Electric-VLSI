/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JELIB.java
 * Input/output tool: JELIB Library output
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
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortOriginal;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Tool;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class to write a library to disk in new Electric-Library format.
 */
public class JELIB extends Output
{
	private static final boolean NEWREVISION = Version.getVersion().compareTo(Version.parseVersion("8.01aw")) >= 0;
	private HashMap abbreviationMap;
	private HashSet externalObjs;
	private static Comparator nodesComparator = new TextUtils.NodesByName();
	private static Comparator arcsComparator = new TextUtils.ArcsByName();
	private static Comparator exportsComparator = new TextUtils.ExportsByName();

	JELIB()
	{
	}

	/**
	 * Method to write a Library in Electric Library (.jelib) format.
	 * @param lib the Library to be written.
	 * @return true on error.
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
	 * Method to write the .jelib file.
	 * @param lib the Library to write.
	 * @return true on error.
	 */
	private boolean writeTheLibrary(Library lib)
		throws IOException
	{
		printWriter.print("# header information:\n");

		// pick up all full names that might become abbreviations
		abbreviationMap = new HashMap();
		externalObjs = new HashSet();
		traverseAndGatherNames(lib);

		// write header information (library, version, main cell)
		printWriter.print("H" + convertString(lib.getName()) + "|" + Version.getVersion());
		writeVars(lib, null);
		printWriter.print("\n");

		// write view information
		TreeSet views = new TreeSet();
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View view = (View)it.next();
			if (externalObjs.contains(view)) views.add(view);
		}
		if (views.size() > 0)
		{
			printWriter.print("\n# Views:\n");
			for(Iterator it = views.iterator(); it.hasNext(); )
			{
				View view = (View)it.next();
				printWriter.print("V" + convertString(view.getFullName()) + "|" + convertString(view.getAbbreviation()) + "\n");
			}
		}

		// write external library information
		TreeMap externalLibs = new TreeMap();
		for (Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library eLib = (Library)it.next();
			if (eLib == lib) continue;
			if (externalObjs.contains(eLib)) externalLibs.put(eLib.getName(), eLib);
		}
		if (externalLibs.size() > 0)
		{
			printWriter.print("\n# External Libraries and cells:\n");
			for(Iterator it = externalLibs.values().iterator(); it.hasNext(); )
			{
				Library eLib = (Library)it.next();
				String libFile = eLib.getLibFile().toString();
//				if (libFile.endsWith(".elib")) libFile = libFile.substring(0, libFile.length()-5) + ".jelib"; else
//				if (libFile.endsWith(".txt")) libFile = libFile.substring(0, libFile.length()-4) + ".jelib";
				if (NEWREVISION) printWriter.println();
				printWriter.print("L" + convertString(eLib.getName()) + "|" + convertString(libFile) + "\n");
				TreeMap externalCells = new TreeMap();
				for(Iterator cIt = eLib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					if (externalObjs.contains(cell)) externalCells.put(cell.getCellName(), cell);
				}
				for(Iterator cIt = externalCells.values().iterator(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					Rectangle2D bounds = cell.getBounds();
					if (NEWREVISION)
					{
						printWriter.print("R" + convertString(cell.getCellName().toString()) +
							"|" + TextUtils.formatDouble(DBMath.round(bounds.getMinX()),0) +
							"|" + TextUtils.formatDouble(DBMath.round(bounds.getMaxX()),0) +
							"|" + TextUtils.formatDouble(DBMath.round(bounds.getMinY()),0) +
							"|" + TextUtils.formatDouble(DBMath.round(bounds.getMaxY()),0) +
							"|" + cell.getCreationDate().getTime() +
							"|" + cell.getRevisionDate().getTime() +
							"\n");
					} else
					{
						printWriter.print("R" + convertString(cell.getName()) + ";" + cell.getVersion() +
							"{" + convertString(cell.getView().getAbbreviation()) + "}" +
							"|" + TextUtils.formatDouble(bounds.getMinX(),0) +
							"|" + TextUtils.formatDouble(bounds.getMaxX(),0) +
							"|" + TextUtils.formatDouble(bounds.getMinY(),0) +
							"|" + TextUtils.formatDouble(bounds.getMaxY(),0) + "\n");
					}
					List sortedExports = new ArrayList();
					for (Iterator eIt = cell.getPorts(); eIt.hasNext(); )
					{
						Export export = (Export)eIt.next();
						if (externalObjs.contains(export)) sortedExports.add(export);
					}
					Collections.sort(sortedExports, exportsComparator);
					for (Iterator eIt = sortedExports.iterator(); eIt.hasNext(); )
					{
						Export export = (Export)eIt.next();
						if (NEWREVISION)
						{
							Poly poly = export.getOriginalPort().getPoly();
							printWriter.println("F" + convertString(export.getName()) +
								"|" + TextUtils.formatDouble(DBMath.round(poly.getCenterX()), 0) +
								"|" + TextUtils.formatDouble(DBMath.round(poly.getCenterY()), 0));
						} else
						{
							printWriter.print("#" + convertString(export.getName()));
							printWriter.print("|" + describeDescriptor(null, export.getTextDescriptor()));
							PortOriginal po = new PortOriginal(export.getOriginalPort());
							PrimitivePort pp = po.getBottomPortProto();
							PrimitiveNode pn = (PrimitiveNode)pp.getParent();
							printWriter.print("|" + pn.getFullName() + "|");
							if (pn.getNumPorts() > 1)
								printWriter.print(pp.getName());
							// port information
							Poly poly = export.getOriginalPort().getPoly();
							printWriter.print("|" + TextUtils.formatDouble(poly.getCenterX(), 0) +
											  "|" + TextUtils.formatDouble(poly.getCenterY(), 0));
							int angle = po.getAngleToTop();
							printWriter.print(angle != 0 ? "|" + angle : "|");
							printWriter.print("|" + export.getCharacteristic().getShortName());
							if (export.isAlwaysDrawn()) printWriter.print("/A");
							if (export.isBodyOnly()) printWriter.print("/B");
							printWriter.print("\n");
						}
					}
				}
			}
		}

		// write tool information
		TreeMap tools = new TreeMap();
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			if (Pref.getMeaningVariables(tool).size() != 0) tools.put(tool.getName(), tool);
		}
		if (tools.size() > 0)
		{
			printWriter.print("\n# Tools:\n");
			for(Iterator it = tools.values().iterator(); it.hasNext(); )
			{
				Tool tool = (Tool)it.next();
				printWriter.print("O" + convertString(tool.getName()));
				writeMeaningPrefs(tool);
				printWriter.print("\n");
			}
		}

		// write technology information
		TreeMap technologies = new TreeMap();
		for (Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			if (externalObjs.contains(tech) || Pref.getMeaningVariables(tech).size() != 0)
				technologies.put(tech.getTechName(), tech);
		}
		for(Iterator it = technologies.values().iterator(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			printWriter.print("\n# Technology " + tech.getTechName() + "\n");
			printWriter.print("T" + convertString(tech.getTechName()));
			writeMeaningPrefs(tech);
			printWriter.print("\n");
			TreeMap primNodes = new TreeMap();
			for(Iterator nIt = tech.getNodes(); nIt.hasNext(); )
			{
				PrimitiveNode pn = (PrimitiveNode)nIt.next();
				if (externalObjs.contains(pn)) primNodes.put(pn.getName(), pn);
			}
			for(Iterator nIt = primNodes.values().iterator(); nIt.hasNext(); )
			{
				PrimitiveNode pn = (PrimitiveNode)nIt.next();
				printWriter.print("D" + convertString(pn.getName()));
//				writeVars(np, null);
				printWriter.print("\n");
				TreeMap primPorts = new TreeMap();
				for(Iterator pIt = pn.getPorts(); pIt.hasNext(); )
				{
					PrimitivePort pp = (PrimitivePort)pIt.next();
					if (externalObjs.contains(pp)) primPorts.put(pp.getName(), pp);
				}
				for(Iterator pIt = primPorts.values().iterator(); pIt.hasNext(); )
				{
					PrimitivePort pp = (PrimitivePort)pIt.next();
					printWriter.print("P" + convertString(pp.getName()));
//					writeVars(pp, null);
					printWriter.print("\n");
				}
			}
			TreeMap arcProtos = new TreeMap();
			for(Iterator aIt = tech.getArcs(); aIt.hasNext(); )
			{
				ArcProto ap = (ArcProto)aIt.next();
				if (externalObjs.contains(ap)) arcProtos.put(ap.getName(), ap);
			}
			for(Iterator aIt = arcProtos.values().iterator(); aIt.hasNext(); )
			{
				ArcProto ap = (ArcProto)aIt.next();
				printWriter.print("W" + convertString(ap.getName()));
// 				writeVars(ap, null);
				printWriter.print("\n");
			}
		}

		// write the cells of the database
		TreeMap cells = new TreeMap();
		for (Iterator cIt = lib.getCells(); cIt.hasNext(); )
		{
			Cell cell = (Cell)cIt.next();
			cells.put(cell.getCellName(), cell);
		}
		List groups = new ArrayList();
		for(Iterator cIt = cells.values().iterator(); cIt.hasNext(); )
		{
			// write the Cell name
			Cell cell = (Cell)cIt.next();
			if (!groups.contains(cell.getCellGroup()))
				groups.add(cell.getCellGroup());
			printWriter.print("\n# Cell " + cell.describe() + "\n");
			if (NEWREVISION)
			{
				printWriter.print("C" + convertString(cell.getCellName().toString()));
			} else
			{
				printWriter.print("C" + convertString(cell.getName()));
				printWriter.print("|" + convertString(cell.getView().getAbbreviation()));
				printWriter.print("|" + cell.getVersion());
			}
			printWriter.print("|" + convertString(cell.getTechnology().getTechName()));
			printWriter.print("|" + cell.getCreationDate().getTime());
			printWriter.print("|" + cell.getRevisionDate().getTime());
			StringBuffer cellBits = new StringBuffer();
			if (cell.isInCellLibrary()) cellBits.append("C");
			if (cell.isWantExpanded()) cellBits.append("E");
			if (cell.isInstancesLocked()) cellBits.append("I");
			if (cell.isAllLocked()) cellBits.append("L");
			if (cell.isInTechnologyLibrary()) cellBits.append("T");
			printWriter.print("|" + cellBits.toString());
			writeVars(cell, cell);
			printWriter.print("\n");

			// write the nodes in this cell (sorted by node name)
			List sortedNodes = new ArrayList();
			for(Iterator it = cell.getNodes(); it.hasNext(); )
				sortedNodes.add(it.next());
			Collections.sort(sortedNodes, nodesComparator);

			// look for duplicate node names
			HashMap sortedNodeIndices = null;
			for(int i=1; i<sortedNodes.size(); i++)
			{
				NodeInst ni = (NodeInst)sortedNodes.get(i);
				NodeInst lastNi = (NodeInst)sortedNodes.get(i-1);
				if (ni.getName().equals(lastNi.getName()))
				{
					// found duplicate names: make an array of sorted node indices
					sortedNodeIndices = new HashMap();
					for(int j=0; j<sortedNodes.size(); j++)
					{
						ni = (NodeInst)sortedNodes.get(j);
						int start = j;
						for(int k=j+1; k<sortedNodes.size(); k++)
						{
							NodeInst otherNi = (NodeInst)sortedNodes.get(k);
							if (ni.getName().equals(otherNi.getName()))
							{
								if (k == start+1) sortedNodeIndices.put(ni, new Integer(1));
								sortedNodeIndices.put(otherNi, new Integer(k-start+1));
								j++;
							}
						}
					}
					break;
				}
			}

			// write the nodes in this cell
			for(Iterator it = sortedNodes.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				NodeProto np = ni.getProto();
				if (NEWREVISION)
				{
					if (np instanceof Cell)
					{
						printWriter.print("I" + (String)abbreviationMap.get(np));
					} else {
						PrimitiveNode prim = (PrimitiveNode)np;
						if (cell.getTechnology() == prim.getTechnology())
							printWriter.print("N" + convertString(prim.getName()));
						else
							printWriter.print("N" + convertString(prim.getFullName()));
					}
				} else
				{
					if (np instanceof Cell) printWriter.print("I"); else
						printWriter.print("N");
					String nodeTypeName = (String)abbreviationMap.get(np);
					printWriter.print(nodeTypeName);
				}
				printWriter.print("|" + getNodeName(ni, sortedNodeIndices) + "|");
				if (!ni.getNameKey().isTempname())
					printWriter.print(describeDescriptor(null, ni.getNameTextDescriptor()));
				printWriter.print("|" + TextUtils.formatDouble(ni.getAnchorCenterX(), 0));
				printWriter.print("|" + TextUtils.formatDouble(ni.getAnchorCenterY(), 0));
				if (NEWREVISION)
				{
					if (np instanceof PrimitiveNode)
					{
						printWriter.print("|" + TextUtils.formatDouble(ni.getXSize(), 0));
						printWriter.print("|" + TextUtils.formatDouble(ni.getYSize(), 0));
					}
					printWriter.print('|');
					if (ni.isXMirrored()) printWriter.print('X');
					if (ni.isYMirrored()) printWriter.print('Y');
					int angle = ni.getAngle() % 3600;
					if (angle == 900 || angle == -2700) printWriter.print("R");
					else if (angle == 1800 || angle == -1800) printWriter.print("RR");
					else if (angle == 2700 || angle == -900) printWriter.print("RRR");
					else if (angle != 0) printWriter.print(angle);
				} else
				{
					printWriter.print("|" + TextUtils.formatDouble(ni.getXSizeWithMirror(), 0));
					printWriter.print("|" + TextUtils.formatDouble(ni.getYSizeWithMirror(), 0));
					printWriter.print("|" + ni.getAngle());
				}
				StringBuffer nodeBits = new StringBuffer();
				if (ni.isHardSelect()) nodeBits.append("A");
				if (ni.isExpanded()) nodeBits.append("E");
				if (ni.isLocked()) nodeBits.append("L");
				if (ni.isShortened()) nodeBits.append("S");
				if (ni.isVisInside()) nodeBits.append("V");
				if (ni.isWiped()) nodeBits.append("W");
				int ts = ni.getTechSpecific();
				if (ts != 0) nodeBits.append(ts);
				if (NEWREVISION)
				{
					printWriter.print("|" + nodeBits.toString());
					if (np instanceof Cell)
					{
						String tdString = describeDescriptor(null, ni.getProtoTextDescriptor());
						printWriter.print("|" + tdString);
					}
				} else
				{
					printWriter.print("|" + nodeBits.toString() + "|");
					if (np instanceof Cell)
					{
						String tdString = describeDescriptor(null, ni.getProtoTextDescriptor());
						printWriter.print(tdString);
					}
				}
				writeVars(ni, cell);
				printWriter.print("\n");
			}

			// write the arcs in this cell
			List sortedArcs = new ArrayList();
			for(Iterator it = cell.getArcs(); it.hasNext(); )
				sortedArcs.add(it.next());
			Collections.sort(sortedArcs, arcsComparator);
			for(Iterator it = sortedArcs.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				PrimitiveArc ap = (PrimitiveArc)ai.getProto();
				if (NEWREVISION)
				{
					if (cell.getTechnology() == ap.getTechnology())
						printWriter.print("A" + convertString(ap.getName()));
					else
						printWriter.print("A" + convertString(ap.getFullName()));
				} else {
					printWriter.print("A" + convertString(ap.getTechnology().getTechName()) + ":" + convertString(ap.getName()));
				}
				printWriter.print("|" + convertString(ai.getName()) + "|");
				if (!ai.getNameKey().isTempname())
					printWriter.print(describeDescriptor(null, ai.getNameTextDescriptor()));
				printWriter.print("|" + TextUtils.formatDouble(ai.getWidth(), 0));
				StringBuffer arcBits = new StringBuffer();

				if (ai.isHardSelect()) arcBits.append("A");
				if (ai.isDirectional()) arcBits.append("D");
				if (!ai.isExtended()) arcBits.append("E");
				if (!ai.isFixedAngle()) arcBits.append("F");
				if (ai.getHead().isNegated()) arcBits.append("G");
				if (ai.isSkipHead()) arcBits.append("H");
				if (ai.getTail().isNegated()) arcBits.append("N");
				if (ai.isRigid()) arcBits.append("R");
				if (ai.isSlidable()) arcBits.append("S");
				if (ai.isSkipTail()) arcBits.append("T");
				if (ai.isReverseEnds()) arcBits.append("V");
				printWriter.print("|" + arcBits.toString() + ai.getAngle());
				for(int e=0; e<2; e++)
				{
					Connection con = ai.getConnection(e);
					NodeInst ni = con.getPortInst().getNodeInst();
					printWriter.print("|" + getNodeName(ni, sortedNodeIndices) + "|");
					PortProto pp = con.getPortInst().getPortProto();
					if (ni.getProto().getNumPorts() > 1)
						printWriter.print(convertString(pp.getName()));
					printWriter.print("|" + TextUtils.formatDouble(con.getLocation().getX(), 0));
					printWriter.print("|" + TextUtils.formatDouble(con.getLocation().getY(), 0));
				}
				writeVars(ai, cell);
				printWriter.print("\n");
			}

			// write the exports in this cell
			List sortedExports = new ArrayList();
			for(Iterator it = cell.getPorts(); it.hasNext(); )
				sortedExports.add(it.next());
			Collections.sort(sortedExports, exportsComparator);
			for(Iterator it = sortedExports.iterator(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				printWriter.print("E" + convertString(pp.getName()));
				printWriter.print("|" + describeDescriptor(null, pp.getTextDescriptor()));

				PortInst subPI = pp.getOriginalPort();
				NodeInst subNI = subPI.getNodeInst();
				PortProto subPP = subPI.getPortProto();
				printWriter.print("|" + getNodeName(subNI, sortedNodeIndices) + "|");
				if (subNI.getProto().getNumPorts() > 1)
					printWriter.print(convertString(subPP.getName()));

				// port information
				if (!NEWREVISION)
				{
					Poly poly = subPI.getPoly();
					printWriter.print("|" + TextUtils.formatDouble(poly.getCenterX(), 0) +
									  "|" + TextUtils.formatDouble(poly.getCenterY(), 0));
				}

				printWriter.print("|" + pp.getCharacteristic().getShortName());
				if (pp.isAlwaysDrawn()) printWriter.print("/A");
				if (pp.isBodyOnly()) printWriter.print("/B");

				writeVars(pp, cell);
				printWriter.print("\n");
			}

			// write the end-of-cell marker
			printWriter.print("X\n");
		}

		// write groups in alphabetical order
		printWriter.print("\n# Groups:\n");
		Collections.sort(groups, new GroupsByName());
		for(Iterator it = groups.iterator(); it.hasNext(); )
		{
			Cell.CellGroup group = (Cell.CellGroup)it.next();
			printWriter.print("G");

			// if there is a main schematic cell, write that first
			Cell main = group.getMainSchematics();
			if (main != null)
			{
				if (NEWREVISION)
				{
					printWriter.print(convertString(main.getCellName().toString()));
				} else
				{
					printWriter.print(convertString(main.describe()));
				}
			}

			TreeMap groupCells = new TreeMap();
			for(Iterator cIt = group.getCells(); cIt.hasNext(); )
			{
				Cell c = (Cell)cIt.next();
				if (c != main) groupCells.put(c.getCellName(), c);
			}
			for(Iterator cIt = groupCells.values().iterator(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				printWriter.print("|");
				if (NEWREVISION)
				{
					printWriter.print(convertString(cell.getCellName().toString()));
				} else
				{
					printWriter.print(convertString(cell.describe()));
				}
			}
			printWriter.print("\n");
		}

		// clean up and return
		lib.clearChangedMinor();
		lib.clearChangedMajor();
		System.out.println(filePath + " written");
		return false;
	}

	public static class GroupsByName implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Cell.CellGroup g1 = (Cell.CellGroup)o1;
			Cell.CellGroup g2 = (Cell.CellGroup)o2;
			String s1 = g1.getName();
			String s2 = g2.getName();
			return TextUtils.nameSameNumeric(s1, s2);
		}
	}

	private String getNodeName(NodeInst ni, HashMap indices)
	{
		if (indices == null) return convertString(ni.getName());
		Integer index = (Integer)indices.get(ni);
		if (index != null)
		{
			if (NEWREVISION)
				return "\"" + convertQuotedString(ni.getName()) + "\"" + index;
			else
				return convertString("\"" + ni.getName() + "\"" + index);
		}
		return convertString(ni.getName());
	}

	/**
	 * Method to help order the library for proper nonforward references
	 * in the outout
	 */
	private void traverseAndGatherNames(Library lib)
	{
		for (Iterator cIt = lib.getCells(); cIt.hasNext(); )
		{
			Cell cell = (Cell)cIt.next();
			gatherCell(cell, !NEWREVISION);
			gatherVariables(cell);

			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				gatherVariables(ni);
				if (ni.getName() == null)
					System.out.println("ERROR: Cell " + cell.describe() + " has node " + ni.describe() + " with no name");
				if (ni.getProto() instanceof Cell)
				{
					Cell subCell = (Cell)ni.getProto();
					if (subCell.getLibrary() != lib)
						gatherCell(subCell, true);
				} else
				{
					PrimitiveNode np = (PrimitiveNode)ni.getProto();
					externalObjs.add(np.getTechnology());
					externalObjs.add(np);
					if (!NEWREVISION)
					{
						if (!abbreviationMap.containsKey(np))
						{
							abbreviationMap.put(np, convertString(np.getTechnology().getTechName()) + ":" + convertString(np.getName()));
						}
					}
				}
			}

			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				gatherVariables(ai);
				for (int i = 0; i < 2; i++)
					externalObjs.add(ai.getConnection(i).getPortInst().getPortProto());
			}

			for(Iterator it = cell.getPorts(); it.hasNext(); )
			{
				Export e = (Export)it.next();
				gatherVariables(e);
				externalObjs.add(e.getOriginalPort().getPortProto());
			}
		}
	}

	private void gatherCell(Cell cell, boolean full)
	{
		if (cell == null) return;
		if (abbreviationMap.containsKey(cell)) return;
		externalObjs.add(cell);
		externalObjs.add(cell.getLibrary());
		externalObjs.add(cell.getView());
		abbreviationMap.put(cell, convertString(getCellName(cell, full)));
	}

	private String getCellName(Cell cell, boolean full)
	{
		if (full)
			return cell.getLibrary().getName() + ":" + cell.getCellName();
		else
			return cell.getCellName().toString();
	}

	/**
	 * Gather external references in variables of ElectricObject.
	 * @param eObj ElectricObject which variables are scanned.
	 */
	private void gatherVariables(ElectricObject eObj)
	{
		for (Iterator it = eObj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			Object value = var.getObject();
			if (value == null) continue;
			int length = value instanceof Object[] ? ((Object[])value).length : 1;
			for (int i = 0; i < length; i++)
			{
				Object v = value instanceof Object[] ? ((Object[])value)[i] : value;
				if (v == null) continue;
				if (v instanceof Technology || v instanceof Tool)
				{
					externalObjs.add(v);
				} else if (v instanceof PrimitiveNode)
				{
					externalObjs.add(v);
					externalObjs.add(((PrimitiveNode)v).getTechnology());
				} else if (v instanceof ArcProto)
				{
					externalObjs.add(v);
					externalObjs.add(((ArcProto)v).getTechnology());
				} else if (v instanceof ElectricObject)
				{
					externalObjs.add(v);
					gatherCell(((ElectricObject)v).whichCell(), true);
				}
			}
		}
	}

	/**
	 * Method to convert a variable to a string that describes its TextDescriptor
	 * @param var the Variable being described (may be null).
	 * @param td the TextDescriptor being described.
	 * @return a String describing the variable/textdescriptor.
	 * The string has these fields:
	 *    Asize; for absolute size
	 *    B if bold
	 *    Cindex; if color index
	 *    Dx for display position (2=bottom 8=top 4=left 6=right 7=upleft 9=upright 1=downleft 3=downright 5=centered 0=boxed)
	 *    FfontName; if a nonstandard font
	 *    Gsize; for relative (grid unit) size
	 *    H if inherit
	 *    I if italic
	 *    L if underline
	 *    N if name=value;
	 *    Ol for language (J=Java L=Lisp T=TCL)
	 *    P if parameter
	 *    R/RR/RRR if rotated (90, 180, 270)
	 *    T if interior
	 *    Ux for units (R=resistance C=capacitance I=inductance A=current V=voltage D=distance T=time)
	 *    Xoffset; for X offset
	 *    Yoffset; for Y offset
	 */
	private String describeDescriptor(Variable var, TextDescriptor td)
	{
		StringBuffer ret = new StringBuffer();
		boolean display = false;
		if (var == null || var.isDisplay()) display = true;

		if (display)
		{
			// write size
			TextDescriptor.Size size = td.getSize();
			if (size.isAbsolute()) ret.append("A" + (int)size.getSize() + ";");

			// write bold
			if (td.isBold()) ret.append("B");

			// write color
			int color = td.getColorIndex();
			if (color != 0)
				ret.append("C" + color + ";");

			// displayable: write display position
			ret.append("D");
			TextDescriptor.Position pos = td.getPos();
			if (pos == TextDescriptor.Position.UP) ret.append("8"); else
			if (pos == TextDescriptor.Position.DOWN) ret.append("2"); else
			if (pos == TextDescriptor.Position.LEFT) ret.append("4"); else
			if (pos == TextDescriptor.Position.RIGHT) ret.append("6"); else
			if (pos == TextDescriptor.Position.UPLEFT) ret.append("7"); else
			if (pos == TextDescriptor.Position.UPRIGHT) ret.append("9"); else
			if (pos == TextDescriptor.Position.DOWNLEFT) ret.append("1"); else
			if (pos == TextDescriptor.Position.DOWNRIGHT) ret.append("3"); else
			if (pos == TextDescriptor.Position.BOXED) ret.append("0"); else
				ret.append("5");

			// write font
			int font = td.getFace();
			if (font != 0)
			{
				TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(font);
				ret.append("F" + convertString(af.toString()) + ";");
			}

			if (!size.isAbsolute()) ret.append("G" + TextUtils.formatDouble(size.getSize()) + ";");
		}

		// write inherit
		if (td.isInherit()) ret.append("H");

		if (display)
		{
			// write italic
			if (td.isItalic()) ret.append("I");

			// write underline
			if (td.isUnderline()) ret.append("L");

			// write display type
			TextDescriptor.DispPos dispPos = td.getDispPart();
			if (dispPos == TextDescriptor.DispPos.NAMEVALUE) ret.append("N");
		}

		// write language
		if (var != null && var.isCode())
		{
			Variable.Code codeType = var.getCode();
			if (codeType == Variable.Code.JAVA) ret.append("OJ"); else
			if (codeType == Variable.Code.LISP) ret.append("OL"); else
			if (codeType == Variable.Code.TCL) ret.append("OT");
		}

		// write parameter
		if (td.isParam()) ret.append("P");

		if (display)
		{
			// write rotation
			TextDescriptor.Rotation rot = td.getRotation();
			if (rot == TextDescriptor.Rotation.ROT90) ret.append("R"); else
			if (rot == TextDescriptor.Rotation.ROT180) ret.append("RR"); else
			if (rot == TextDescriptor.Rotation.ROT270) ret.append("RRR");
		}

		// write interior
		if (td.isInterior()) ret.append("T");

		// write units
		TextDescriptor.Unit unit = td.getUnit();
		if (unit == TextDescriptor.Unit.RESISTANCE) ret.append("UR"); else
		if (unit == TextDescriptor.Unit.CAPACITANCE) ret.append("UC"); else
		if (unit == TextDescriptor.Unit.INDUCTANCE) ret.append("UI"); else
		if (unit == TextDescriptor.Unit.CURRENT) ret.append("UA"); else
		if (unit == TextDescriptor.Unit.VOLTAGE) ret.append("UV"); else
		if (unit == TextDescriptor.Unit.DISTANCE) ret.append("UD"); else
		if (unit == TextDescriptor.Unit.TIME) ret.append("UT");

		if (display)
		{
			// write offset
			double offX = td.getXOff();
			if (offX != 0) ret.append("X" + TextUtils.formatDouble(offX, 0) + ";");
			double offY = td.getYOff();
			if (offY != 0) ret.append("Y" + TextUtils.formatDouble(offY, 0) + ";");
		}

		return ret.toString();
	}

	/**
	 * Method to write the variables on an object.  The current cell is
	 * "curCell" such that any references to objects in a cell must be in
	 * this cell.
	 */
	private void writeVars(ElectricObject eObj, Cell curCell)
	{
		// count the number of variables
		int numVars = 0;
		for(Iterator it = eObj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.isDontSave()) continue;
			numVars++;
		}
		if (numVars == 0) return;

		// sort the variables if there are more than 1
		Iterator varIterator = eObj.getVariables();
		if (numVars > 1)
		{
			// must sort the names
			List sortedVars = new ArrayList();
			for(Iterator it = eObj.getVariables(); it.hasNext(); )
			{
				Variable var = (Variable)it.next();
				if (var.isDontSave()) continue;
				sortedVars.add(var);
			}
			Collections.sort(sortedVars, new TextUtils.VariablesByName());
			varIterator = sortedVars.iterator();
		}

		// write the variables
		for(Iterator it = varIterator; it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.isDontSave()) continue;
			String tdString = describeDescriptor(var, var.getTextDescriptor());
			printWriter.print("|" + convertVariableName(var.getKey().getName()) + "(" + tdString + ")");

			Object varObj = var.getObject();
			String pt = makeString(varObj, curCell);
			if (pt == null) pt = "";
			printWriter.print(pt);
		}
	}

	/**
	 * Method to write the meaning preferences on an object.
	 */
	private void writeMeaningPrefs(Object obj)
	{
		List prefs = Pref.getMeaningVariables(obj);
		for(Iterator it = prefs.iterator(); it.hasNext(); )
		{
			Pref pref = (Pref)it.next();
			Object value = pref.getValue();
			printWriter.print("|" + convertVariableName(pref.getPrefName()) + "()" + makeString(value, null));
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
				if (i != 0) infstr.append(","); else
				{
					infstr.append(getVarType(oneObj));
					infstr.append("[");
				}					
				makeStringVar(infstr, oneObj, curCell, true);
			}
			infstr.append("]");
		} else
		{
			infstr.append(getVarType(obj));
			makeStringVar(infstr, obj, curCell, false);
		}
		return infstr.toString();
	}

	/**
	 * Method to make a string from the value in "addr" which has a type in
	 * "type".
	 */
	private void makeStringVar(StringBuffer infstr, Object obj, Cell curCell, boolean inArray)
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
			if (NEWREVISION)
			{
				infstr.append(convertString((String)obj, inArray));
			} else
			{
				infstr.append("\"");
				infstr.append(convertQuotedString((String)obj));
				infstr.append("\"");
			}
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
			infstr.append(((Boolean)obj).booleanValue() ? "T" : "F");
			return;
		}
		if (obj instanceof Long)
		{
			infstr.append(((Long)obj).longValue());
			return;
		}
		if (obj instanceof Point2D)
		{
			Point2D pt2 = (Point2D)obj;
			infstr.append(TextUtils.formatDouble(pt2.getX(), 0) + "/" + TextUtils.formatDouble(pt2.getY(), 0));
			return;
		}
		if (obj instanceof Technology)
		{
			Technology tech = (Technology)obj;
			infstr.append(convertString(tech.getTechName(), inArray));
			return;
		}
		if (obj instanceof Library)
		{
			Library lib = (Library)obj;
			infstr.append(convertString(lib.getName(), inArray));
			return;
		}
		if (obj instanceof Tool)
		{
			Tool tool = (Tool)obj;
			infstr.append(convertString(tool.getName(), inArray));
			return;
		}
		if (obj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)obj;
			infstr.append(convertString(getCellName(ni.getParent(), true) + ":" + ni.getName(), inArray));
			return;
		}
		if (obj instanceof ArcInst)
		{
			ArcInst ai = (ArcInst)obj;
			String arcName = ai.getName();
			if (arcName == null)
			{
				System.out.println("Cannot save pointer to unnamed ArcInst: " + ai.getParent().describe() + ":" + ai.describe());
			}
			infstr.append(convertString(getCellName(ai.getParent(), true) + ":" + arcName, inArray));
			return;
		}
		if (obj instanceof Cell)
		{
			Cell cell = (Cell)obj;
			infstr.append(convertString(getCellName(cell, true), inArray));
			return;
		}
		if (obj instanceof PrimitiveNode)
		{
			PrimitiveNode np = (PrimitiveNode)obj;
			infstr.append(convertString(np.getFullName(), inArray));
			return;
		}
		if (obj instanceof PrimitiveArc)
		{
			PrimitiveArc ap = (PrimitiveArc)obj;
			infstr.append(convertString(ap.getFullName(), inArray));
			return;
		}
		if (obj instanceof Export)
		{
			Export pp = (Export)obj;
			infstr.append(convertString(getCellName((Cell)pp.getParent(), true) + ":" + pp.getName(), inArray));
			return;
		}
	}

	/**
	 * Method to make a string from the value in "addr" which has a type in
	 * "type".
	 */
	private String getVarType(Object obj)
	{
		if (obj instanceof ArcInst) return "S"; // "A"
		if (obj instanceof Boolean) return "B";
		if (obj instanceof Cell) return "C";
		if (obj instanceof Double) return "D";
		if (obj instanceof Export) return "E";
		if (obj instanceof Float) return "F";
		if (obj instanceof Long) return "G";
		if (obj instanceof Short) return "H";
		if (obj instanceof Integer) return "I";
		if (obj instanceof Library) return "L";
		if (obj instanceof NodeInst) return "S"; // "N"
		if (obj instanceof Tool) return "O";
		if (obj instanceof PrimitiveNode) return "P";
		if (obj instanceof ArcProto) return "R";
		if (obj instanceof String) return "S";
		if (obj instanceof Technology) return "T";
		if (obj instanceof Point2D) return "V";
		if (obj instanceof Byte) return "Y";
		return null;
	}

	/**
	 * Method convert a string that is going to be quoted.
	 * Inserts the quote character (^) before any quotation character (") or quote character (^) in the string.
	 * Converts newlines to "^\n" (makeing the "\" and "n" separate characters).
	 * @param str the string to convert.
	 * @return the string with the appropriate quote characters.
	 * If no conversion is necessary, the input string is returned.
	 */
	private String convertQuotedString(String str)
	{
		StringBuffer infstr = new StringBuffer();
		int len = str.length();
		if (NEWREVISION)
		{
			for(int i=0; i<len; i++)
			{
				char ch = str.charAt(i);
				if (ch == '\n') { infstr.append("\\n");   continue; }
				if (ch == '\r') { infstr.append("\\r");   continue; }
				if (ch == '"' || ch == '\\')
					infstr.append('\\');
				infstr.append(ch);
			}
		} else {
			for(int i=0; i<len; i++)
			{
				char ch = str.charAt(i);
				if (ch == '\n') { infstr.append("^\\n");   continue; }
				if (ch == '"' || ch == '^')
					infstr.append('^');
				infstr.append(ch);
			}
		}
		return infstr.toString();
	}

	/**
	 * Method convert a string that is not going to be quoted.
	 * Inserts a quote character (^) before any separator (|), quotation character (") or quote character (^) in the string.
	 * Converts newlines to spaces.
	 * @param str the string to convert.
	 * @return the string with the appropriate quote characters.
	 * If no conversion is necessary, the input string is returned.
	 */
	private String convertString(String str)
	{
		if (NEWREVISION) return convertString(str, (char)0, (char)0);
		StringBuffer infstr = new StringBuffer();
		int len = str.length();
		for(int i=0; i<len; i++)
		{
			char ch = str.charAt(i);
			if (ch == '\n') ch = ' ';
			if (ch == '|' || ch == '^' || ch == '"')
				infstr.append('^');
			infstr.append(ch);
		}
		return infstr.toString();
	}

	/**
	 * Method convert a string that is not going to be quoted.
	 * Inserts a quote character (^) before any separator (|), quotation character (") or quote character (^) in the string.
	 * Converts newlines to spaces.
	 * @param str the string to convert.
	 * @return the string with the appropriate quote characters.
	 * If no conversion is necessary, the input string is returned.
	 */
	private String convertString(String str, char delim1, char delim2)
	{
		if (str.length() != 0 &&
			str.indexOf('\n') < 0 &&
			str.indexOf('\r') < 0 &&
			str.indexOf('\\') < 0 &&
			str.indexOf('"') < 0 &&
			str.indexOf('|') < 0 &&
			(delim1 == 0 || str.indexOf(delim1) < 0) &&
			(delim2 == 0 || str.indexOf(delim2) < 0))
			return str;
		return '"' + convertQuotedString(str) + '"';
	}

	/**
	 * Method convert a string that is a variable name.
	 * If string contains end-of-lines, backslashes, bars, quotation characters, open parenthesis,
	 * encloses string in quotational characters.
	 * @param str the string to convert.
	 * @return the string with the appropriate quote characters.
	 * If no conversion is necessary, the input string is returned.
	 */
	private String convertVariableName(String str)
	{
		if (NEWREVISION) return convertString(str, '(', (char)0);
		StringBuffer infstr = new StringBuffer();
		int len = str.length();
		for(int i=0; i<len; i++)
		{
			char ch = str.charAt(i);
			if (ch == '\n') ch = ' ';
			if (ch == '|' || ch == '^' || ch == '"' || ch == '(')
				infstr.append('^');
			infstr.append(ch);
		}
		return infstr.toString();
	}

	/**
	 * Method convert a string that is a variable value.
	 * If string contains end-of-lines, backslashes, bars, quotation characters, comma,
	 * close bracket, returns string enclosed in quotational characters.
	 * @param str the string to convert.
	 * @param inArray true if string is element of array/
	 * @return the string with the appropriate quote characters.
	 * If no conversion is necessary, the input string is returned.
	 */
	private String convertString(String str, boolean inArray)
	{
		return NEWREVISION && inArray ? convertString(str, ',', ']') : convertString(str, (char)0, (char)0);
	}
}
