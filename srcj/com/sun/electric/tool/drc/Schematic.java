/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Schematic.java
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
package com.sun.electric.tool.drc;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.RTBounds;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Class to do schematic design-rule checking.
 * Examines artwork of a schematic for sensibility.
 */
public class Schematic
{
    // Cells, nodes and arcs
	private static HashSet<ElectricObject> nodesChecked = new HashSet<ElectricObject>();
    private static ErrorLogger errorLogger = null;
    private static HashMap<Geometric,List<Variable>> newVariables = new HashMap<Geometric,List<Variable>>();

	public static ErrorLogger doCheck(ErrorLogger errorLog, Cell cell, Geometric[] geomsToCheck)
	{
		nodesChecked.clear();
        newVariables.clear();

        errorLogger = errorLog;
        // null when it comes from the regression
        if (errorLogger == null) errorLogger = DRC.getDRCErrorLogger(false, false);

		checkSchematicCellRecursively(cell, geomsToCheck);
		errorLogger.termLogging(true);
        DRC.addDRCUpdate(0, null, null, newVariables);
		return(errorLogger);
	}

    private static Cell isACellToCheck(Geometric geo)
    {
        if (geo instanceof NodeInst)
        {
            NodeInst ni = (NodeInst)geo;

            // ignore documentation icon
            if (ni.isIconOfParent()) return null;

            if (!ni.isCellInstance()) return null;
            Cell subCell = (Cell)ni.getProto();

            Cell contentsCell = subCell.contentsView();
            if (contentsCell == null) contentsCell = subCell;
            if (nodesChecked.contains(contentsCell)) return null;
            return contentsCell;
        }
        return null;
    }

	private static void checkSchematicCellRecursively(Cell cell, Geometric[] geomsToCheck)
	{
		nodesChecked.add(cell);

		// ignore if not a schematic
		if (!cell.isSchematic() && cell.getTechnology() != Schematics.tech)
			return;

		// recursively check contents in case of hierchically checking
        if (geomsToCheck == null)
        {
            for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
            {
                NodeInst ni = it.next();
                Cell contentsCell = isACellToCheck(ni);
                if (contentsCell != null)
                    checkSchematicCellRecursively(contentsCell, geomsToCheck);
            }
        }
        else
        {
            for (Geometric geo : geomsToCheck)
            {
                Cell contentsCell = isACellToCheck(geo);

                if (contentsCell != null)
                    checkSchematicCellRecursively(contentsCell, geomsToCheck);
            }
        }

		// now check this cell
		System.out.println("Checking schematic " + cell);
		checkSchematicCell(cell, false, geomsToCheck);
	}

	private static void checkSchematicCell(Cell cell, boolean justThis, Geometric[] geomsToCheck)
	{
		int initialErrorCount = errorLogger.getNumErrors();
		Netlist netlist = NetworkTool.getUserNetlist(cell);

        // Normal hierarchically geometry
        if (geomsToCheck == null)
        {
            for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
            {
                NodeInst ni = it.next();
                if (!ni.isCellInstance() &&
                    ni.getProto().getTechnology() == Generic.tech) continue;
                schematicDoCheck(netlist, ni);
            }
            for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
            {
                ArcInst ai = it.next();
                schematicDoCheck(netlist, ai);
            }
        }
        else
        {
            for (Geometric geo : geomsToCheck)
                schematicDoCheck(netlist, geo);
        }

		int errorCount = errorLogger.getNumErrors();
		int thisErrors = errorCount - initialErrorCount;
		String indent = "   ";
		if (justThis) indent = "";
		if (thisErrors == 0) System.out.println(indent + "No errors found"); else
			System.out.println(indent + thisErrors + " errors found");
		if (justThis) errorLogger.termLogging(true);
	}

    /**
     * Method to add all variables of a given NodeInst that must be added after Schematics DRC job is done.
     */
    private static void addVariable(NodeInst ni, Variable var)
    {
        List<Variable> list = newVariables.get(ni);

        if (list == null) // first time
        {
            list = new ArrayList<Variable>();
            newVariables.put(ni, list);
        }
        list.add(var);
    }

	/**
	 * Method to check schematic object "geom".
	 */
	private static void schematicDoCheck(Netlist netlist, Geometric geom)
	{
        // Checked already
        if (nodesChecked.contains(geom))
            return;
        nodesChecked.add(geom);

		Cell cell = geom.getParent();
		if (geom instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)geom;
			NodeProto np = ni.getProto();

			// check for bus pins that don't connect to any bus arcs
			if (np == Schematics.tech.busPinNode)
			{
				// proceed only if it has no exports on it
				if (!ni.hasExports())
				{
					// must not connect to any bus arcs
					boolean found = false;
					for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
					{
						Connection con = it.next();
						if (con.getArc().getProto() == Schematics.tech.bus_arc) { found = true;   break; }
					}
					if (!found)
					{
						errorLogger.logError("Bus pin does not connect to any bus arcs", geom, cell, null, 0);
						return;
					}
				}

				// flag bus pin if more than 1 wire is connected
				int i = 0;
				for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
				{
					Connection con = it.next();
					if (con.getArc().getProto() == Schematics.tech.wire_arc) i++;
				}
				if (i > 1)
				{
					List<Geometric> geomList = new ArrayList<Geometric>();
					geomList.add(geom);
					for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
					{
						Connection con = it.next();
						if (con.getArc().getProto() == Schematics.tech.wire_arc) i++;
							geomList.add(con.getArc());
					}
					errorLogger.logError("Wire arcs cannot connect through a bus pin", geomList, null, cell, 0);
					return;
				}
			}

			// check all pins
			if (np.getFunction() == PrimitiveNode.Function.PIN)
			{
				// may be stranded if there are no exports or arcs
				if (!ni.hasExports() && !ni.hasConnections())
				{
					// see if the pin has displayable variables on it
					boolean found = false;
					for(Iterator<Variable> it = ni.getVariables(); it.hasNext(); )
					{
						Variable var = it.next();
						if (var.isDisplay()) { found = true;   break; }
					}
					if (!found)
					{
						errorLogger.logError("Stranded pin (not connected or exported)", geom, cell, null, 0);
						return;
					}
				}

				if (ni.isInlinePin())
				{
					errorLogger.logError("Unnecessary pin (between 2 arcs)", geom, cell, null, 0);
					return;
				}

				Point2D pinLoc = ni.invisiblePinWithOffsetText(false);
				if (pinLoc != null)
				{
					List<Geometric> geomList = new ArrayList<Geometric>();
					List<EPoint> ptList = new ArrayList<EPoint>();
					geomList.add(geom);
					ptList.add(new EPoint(ni.getAnchorCenterX(), ni.getAnchorCenterY()));
					ptList.add(new EPoint(pinLoc.getX(), pinLoc.getY()));
					errorLogger.logError("Invisible pin has text in different location", geomList, null, ptList, null, null, cell, 0);
					return;
				}
			}

			// check parameters
			if (np instanceof Cell)
			{
				Cell instCell = (Cell)np;
				Cell contentsCell = instCell.contentsView();
				if (contentsCell == null) contentsCell = instCell;

				// ensure that this node matches the parameter list
				for(Iterator<Variable> it = ni.getVariables(); it.hasNext(); )
				{
					Variable var = it.next();
                    if (!ni.isParam(var.getKey())) continue;

                    Variable foundVar = contentsCell.getParameter(var.getKey());
//					Variable foundVar = null;
//					for(Iterator cIt = contentsCell.getVariables(); cIt.hasNext(); )
//					{
//						Variable fVar = (Variable)cIt.next();
//						if (!fVar.isParam()) continue;
//						if (var.getKey() == fVar.getKey()) { foundVar = fVar;   break; }
//					}
					if (foundVar == null)
					{
						// this node's parameter is no longer on the cell: delete from instance
						String trueVarName = var.getTrueName();
						errorLogger.logError("Parameter '" + trueVarName + "' on " + ni +
							" is invalid", geom, cell, null, 0);

						// this is broken:
//						ni.delVar(var.getKey());
//						i--;
					} else
					{
						// this node's parameter is still on the cell: make sure units are OK
						if (var.getUnit() != foundVar.getUnit())
						{
							String trueVarName = var.getTrueName();
							errorLogger.logError("Parameter '" + trueVarName + "' on " + ni +
								" had incorrect units (now fixed)", geom, cell, null, 0);
                            addVariable(ni, var.withUnit(foundVar.getUnit()));
						}

						// make sure visibility is OK
						if (foundVar.isInterior())
						{
							if (var.isDisplay())
							{
								String trueVarName = var.getTrueName();
								errorLogger.logError("Parameter '" + trueVarName + "' on " + ni +
									" should not be visible (now fixed)", geom, cell, null, 0);
                                addVariable(ni, var.withDisplay(false));
							}
						} else
						{
							if (!var.isDisplay())
							{
								String trueVarName = var.getTrueName();
								errorLogger.logError("Parameter '" + trueVarName + "' on " + ni +
									" should be visible (now fixed)", geom, cell, null, 0);
                                addVariable(ni, var.withDisplay(true));
							}
						}
					}
				}
			}

			// check for port overlap
			checkPortOverlap(netlist, ni);
		} else
		{
			ArcInst ai = (ArcInst)geom;

			// check for being floating if it does not have a visible name on it
			boolean checkDangle = false;

            if (Artwork.isArtworkArc(ai.getProto()))
                return; // ignore artwork arcs

            Name arcName = ai.getNameKey();
			if (arcName == null || arcName.isTempname()) checkDangle = true;
			if (checkDangle)
			{
				// do not check for dangle when busses are on named networks
				if (ai.getProto() == Schematics.tech.bus_arc)
				{
					Name name = netlist.getBusName(ai);
					if (name != null && !name.isTempname()) checkDangle = false;
				}
			}
			if (checkDangle)
			{
				// check to see if this arc is floating
				for(int i=0; i<2; i++)
				{
					NodeInst ni = ai.getPortInst(i).getNodeInst();

					// OK if not a pin
					if (ni.getProto().getFunction() != PrimitiveNode.Function.PIN) continue;

					// OK if it has exports on it
					if (ni.hasExports()) continue;

					// OK if it connects to more than 1 arc
					if (ni.getNumConnections() != 1) continue;

					// the arc dangles
					errorLogger.logError("Arc dangles", geom, cell, null, 0);
					return;
				}
			}

			// check to see if its width is sensible
			int signals = netlist.getBusWidth(ai);
			if (signals < 1) signals = 1;
			for(int i=0; i<2; i++)
			{
				PortInst pi = ai.getPortInst(i);
				NodeInst ni = pi.getNodeInst();
				if (!ni.isCellInstance()) continue;
				Cell subNp = (Cell)ni.getProto();
				PortProto pp = pi.getPortProto();

				Cell np = subNp.contentsView();
				if (np != null)
				{
					pp = ((Export)pi.getPortProto()).getEquivalent();
					if (pp == null || pp == pi.getPortProto())
					{
						List<Geometric> geomList = new ArrayList<Geometric>();
						geomList.add(geom);
						geomList.add(ni);
						errorLogger.logError("Arc " + ai.describe(true) + " connects to " +
							pi.getPortProto() + " of " + ni + ", but there is no equivalent port in " + np, geomList, null, cell, 0);
						continue;
					}
				}

				int portWidth = netlist.getBusWidth((Export)pp);
				if (portWidth < 1) portWidth = 1;
				int nodeSize = ni.getNameKey().busWidth();
				if (nodeSize <= 0) nodeSize = 1;
				if (signals != portWidth && signals != portWidth*nodeSize)
				{
					List<Geometric> geomList = new ArrayList<Geometric>();
					geomList.add(geom);
					geomList.add(ni);
					errorLogger.logError("Arc " + ai.describe(true) + " (" + signals + " wide) connects to " +
						pp + " of " + ni + " (" + portWidth + " wide)", geomList, null, cell, 0);
				}
			}
		}
	}

	/**
	 * Method to check whether any port on a node overlaps others without connecting.
	 */
	private static void checkPortOverlap(Netlist netlist, NodeInst ni)
	{
		if (ni.getProto().getTechnology() == Generic.tech) return;
		Cell cell = ni.getParent();
		for(Iterator<PortInst> it = ni.getPortInsts(); it.hasNext(); )
		{
			PortInst pi = it.next();
			Network net = netlist.getNetwork(pi);
			Rectangle2D bounds = pi.getPoly().getBounds2D();
			for(Iterator<RTBounds> sIt = cell.searchIterator(bounds); sIt.hasNext(); )
			{
				Geometric oGeom = (Geometric)sIt.next();
				if (!(oGeom instanceof NodeInst)) continue;
				NodeInst oNi = (NodeInst)oGeom;
				if (ni == oNi) continue;
				if (ni.getNodeIndex() > oNi.getNodeIndex()) continue;
				if (oNi.getProto().getTechnology() == Generic.tech) continue;
	
				// see if ports touch
				for(Iterator<PortInst> pIt = oNi.getPortInsts(); pIt.hasNext(); )
				{
					PortInst oPi = pIt.next();
					Rectangle2D oBounds = oPi.getPoly().getBounds2D();
					if (bounds.getMaxX() < oBounds.getMinX()) continue;
					if (bounds.getMinX() > oBounds.getMaxX()) continue;
					if (bounds.getMaxY() < oBounds.getMinY()) continue;
					if (bounds.getMinY() > oBounds.getMaxY()) continue;

					// see if they are connected
					if (net == netlist.getNetwork(oPi)) continue;

					// report the error
					List<Geometric> geomList = new ArrayList<Geometric>();
					geomList.add(ni);
					geomList.add(oNi);
					errorLogger.logError("Nodes '" + ni + "' '" + oNi + "' have touching ports that are not connected",
						geomList, null, cell, 0);
					return;
				}
			}
		}
	}
}
