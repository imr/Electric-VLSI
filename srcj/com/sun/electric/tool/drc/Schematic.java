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

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

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

//		errorLogger = ErrorLogger.newInstance("Schematic DRC");
        errorLogger = errorLog;
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

//                // ignore documentation icon
//                if (ni.isIconOfParent()) continue;
//
//                if (!ni.isCellInstance()) continue;
//                Cell subCell = (Cell)ni.getProto();
//
//                Cell contentsCell = subCell.contentsView();
//                if (contentsCell == null) contentsCell = subCell;
//                if (cellsChecked.contains(contentsCell)) continue;

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
//		if (justThis) errorLogger = ErrorLogger.newInstance("Schematic DRC");
		int initialErrorCount = errorLogger.getNumErrors();
		Netlist netlist = NetworkTool.getUserNetlist(cell);

        // Normal hierarchically geometry
        if (geomsToCheck == null)
        {
            for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
            {
                NodeInst ni = (NodeInst)it.next();
                if (!ni.isCellInstance() &&
                    ni.getProto().getTechnology() == Generic.tech) continue;
                schematicDoCheck(netlist, ni);
            }
            for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
            {
                ArcInst ai = (ArcInst)it.next();
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
     * @param ni
     * @param var
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
//				if (ni.getNumExports() == 0)
				{
					// must not connect to any bus arcs
					boolean found = false;
					for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
					{
						Connection con = (Connection)it.next();
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
					Connection con = (Connection)it.next();
					if (con.getArc().getProto() == Schematics.tech.wire_arc) i++;
				}
				if (i > 1)
				{
					List<Geometric> geomList = new ArrayList<Geometric>();
					geomList.add(geom);
					for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
					{
						Connection con = (Connection)it.next();
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
//				if (ni.getNumExports() == 0 && ni.getNumConnections() == 0)
				{
					// see if the pin has displayable variables on it
					boolean found = false;
					for(Iterator<Variable> it = ni.getVariables(); it.hasNext(); )
					{
						Variable var = (Variable)it.next();
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
					Variable var = (Variable)it.next();
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
//							" is invalid and has been deleted", geom, true, cell, null, 0);
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
//							ni.addVar(var.withUnit(foundVar.getUnit()));
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
//                                ni.addVar(var.withDisplay(false));
                                addVariable(ni, var.withDisplay(false));
							}
						} else
						{
							if (!var.isDisplay())
							{
								String trueVarName = var.getTrueName();
								errorLogger.logError("Parameter '" + trueVarName + "' on " + ni +
									" should be visible (now fixed)", geom, cell, null, 0);
//                                ni.addVar(var.withDisplay(true));
                                addVariable(ni, var.withDisplay(true));
							}
						}
					}
				}
			}
		} else
		{
			ArcInst ai = (ArcInst)geom;

			// check for being floating if it does not have a visible name on it
			boolean checkDangle = false;
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
//					if (ni.getNumExports() != 0) continue;

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

		// check for overlap
		// this checks to make sure no graphics overlaps another that it is not connected to.
		// Designers don't want this check
		// checkObjectVicinity(netlist, geom, geom, DBMath.MATID);
	}

	/**
	 * Method to check whether object "geom" has a DRC violation with a neighboring object.
	 */
	private static void checkObjectVicinity(Netlist netlist, Geometric topGeom, Geometric geom, AffineTransform trans)
	{
		if (geom instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)geom;
			NodeProto np = ni.getProto();
			AffineTransform localTrans = ni.rotateOut();
			localTrans.preConcatenate(trans);
			if (ni.isCellInstance())
			{
				if (ni.isExpanded())
				{
					// expand the instance
					AffineTransform subRot = ni.translateOut();
					subRot.preConcatenate(localTrans);
					Cell subCell = (Cell)np;
					for(Iterator<NodeInst> it = subCell.getNodes(); it.hasNext(); )
					{
						NodeInst subNi = (NodeInst)it.next();
						checkObjectVicinity(netlist, topGeom, subNi, subRot); 
					}
					for(Iterator<ArcInst> it = subCell.getArcs(); it.hasNext(); )
					{
						ArcInst subAi = (ArcInst)it.next();
						checkObjectVicinity(netlist, topGeom, subAi, subRot); 
					}
				}
			} else
			{
				// primitive
				Technology tech = np.getTechnology();
				Poly [] polyList = tech.getShapeOfNode(ni);
				int total = polyList.length;
				for(int i=0; i<total; i++)
				{
					Poly poly = polyList[i];
					poly.transform(localTrans);
					checkPolygonVicinity(netlist, topGeom, poly);
				}
			}
		} else
		{
			ArcInst ai = (ArcInst)geom;
			Technology tech = ai.getProto().getTechnology();
			Poly [] polyList = tech.getShapeOfArc(ai);
			int total = polyList.length;
			for(int i=0; i<total; i++)
			{
				Poly poly = polyList[i];
				poly.transform(trans);
				checkPolygonVicinity(netlist, topGeom, poly);
			}
		}
	}

	/**
	 * Method to check whether polygon "poly" from object "geom" has a DRC violation
	 * with a neighboring object.  Returns TRUE if an error was found.
	 */
	private static boolean checkPolygonVicinity(Netlist netlist, Geometric geom, Poly poly)
	{
		// don't check text
		Poly.Type style = poly.getStyle();
		if (style.isText()) return false;

		Cell cell = geom.getParent();
		NodeInst ni = null;
		ArcInst ai = null;
		if (geom instanceof NodeInst) ni = (NodeInst)geom; else ai = (ArcInst)geom;
		Rectangle2D bounds = geom.getBounds();
		for(Iterator<Geometric> sIt = cell.searchIterator(bounds); sIt.hasNext(); )
		{
			Geometric oGeom = (Geometric)sIt.next();

			// canonicalize so that errors are found only once
//			if ((INTBIG)geom <= (INTBIG)oGeom) continue;
			if (geom == oGeom) continue;

			// what type of object was found in area
			if (oGeom instanceof NodeInst)
			{
				// found node nearby
				NodeInst oNi = (NodeInst)oGeom;
				if (!oNi.isCellInstance() &&
					oNi.getProto().getTechnology() == Generic.tech) continue;
				if (geom instanceof NodeInst)
				{
					// this is node, nearby is node: see if two nodes touch
					boolean found = false;
					for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
					{
						Connection con = (Connection)it.next();
						for(Iterator<Connection> oIt = oNi.getConnections(); oIt.hasNext(); )
						{
							Connection oCon = (Connection)oIt.next();
							if (netlist.sameNetwork(con.getArc(), oCon.getArc()))
							{
								found = true;
								break;
							}
						}
						if (found) break;
					}
					if (found) continue;
				} else
				{			
					// this is arc, nearby is node: see if electrically connected
					boolean found = false;
					for(Iterator<Connection> oIt = oNi.getConnections(); oIt.hasNext(); )
					{
						Connection oCon = (Connection)oIt.next();
						if (netlist.sameNetwork(ai, oCon.getArc()))
						{
							found = true;
							break;
						}
					}
					if (found) continue;
				}

				// no connection: check for touching another
				if (checkPoly(geom, poly, oGeom, oGeom, DBMath.MATID, false))
				{
					return true;
				}
			} else
			{
				// found arc nearby
				ArcInst oAi = (ArcInst)oGeom;
				if (geom instanceof NodeInst)
				{
					// this is node, nearby is arc: see if electrically connected
					boolean found = false;
					for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
					{
						Connection con = (Connection)it.next();
						if (netlist.sameNetwork(oAi, con.getArc()))
						{
							found = true;
							break;
						}
					}
					if (found) continue;

					if (checkPoly(geom, poly, oGeom, oGeom, DBMath.MATID, false))
					{
						return true;
					}
				} else
				{
					// this is arc, nearby is arc: check for colinearity
					if (checkColinear(ai, oAi))
					{
						return true;
					}

					// if not connected, check to see if they touch
					boolean connected = false;
					if (netlist.sameNetwork(ai, oAi)) connected = true; else
					{
						int aiBusWidth = netlist.getBusWidth(ai);
						int oAiBusWidth = netlist.getBusWidth(oAi);
						if (aiBusWidth > 1 && oAiBusWidth <= 1)
						{
							for(int i=0; i<aiBusWidth; i++)
							{
								if (netlist.getNetwork(ai, i) == netlist.getNetwork(oAi, 0)) { connected = true;   break; }
							}
						} else if (oAiBusWidth > 1 && aiBusWidth <= 1)
						{
							for(int i=0; i<oAiBusWidth; i++)
							{
								if (netlist.getNetwork(oAi, i) == netlist.getNetwork(ai, 0)) { connected = true;   break; }
							}
						}
					}
					if (!connected)
					{
						// if the arcs connect at a WireCon, they are connected
						NodeInst headNi = ai.getHeadPortInst().getNodeInst();
						NodeInst tailNi = ai.getTailPortInst().getNodeInst();
						if (headNi.getProto() != Schematics.tech.wireConNode) headNi = null;
						if (tailNi.getProto() != Schematics.tech.wireConNode) tailNi = null;
						NodeInst oHeadNi = oAi.getHeadPortInst().getNodeInst();
						NodeInst oTailNi = oAi.getTailPortInst().getNodeInst();
						if (headNi == oHeadNi || headNi == oTailNi || tailNi == oHeadNi || tailNi == oTailNi) connected = true;
					}
					if (!connected)
					{
						// the last parameter was "false", changed it to "true" for bug #376
						if (checkPoly(geom, poly, oGeom, oGeom, DBMath.MATID, true))
						{
							return true;
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Check polygon "poly" from object "geom" against
	 * geom "oGeom" transformed by "otrans" (and really on top-level object "oTopGeom").
	 * Returns TRUE if an error was found.
	 */
	private static boolean checkPoly(Geometric geom, Poly poly, Geometric oTopGeom, Geometric oGeom, AffineTransform oTrans,
		boolean canCross)
	{
		if (oGeom instanceof NodeInst)
		{
			if (geom instanceof ArcInst) return false;
			NodeInst ni = (NodeInst)oGeom;
			NodeProto np = ni.getProto();
			AffineTransform thisTrans = ni.rotateOut();
			thisTrans.preConcatenate(oTrans);
			if (ni.isCellInstance())
			{
				AffineTransform subRot = ni.translateOut();
				subRot.preConcatenate(thisTrans);
				Cell subCell = (Cell)np;
				for(Iterator<NodeInst> it = subCell.getNodes(); it.hasNext(); )
				{
					NodeInst subNi = (NodeInst)it.next();
					if (checkPoly(geom, poly, oTopGeom, subNi, subRot, canCross))
						return true;
				}
				for(Iterator<ArcInst> it = subCell.getArcs(); it.hasNext(); )
				{
					ArcInst subAi = (ArcInst)it.next();
					if (checkPoly(geom, poly, oTopGeom, subAi, subRot, canCross))
						return true;
				}
			} else
			{
				Technology tech = np.getTechnology();
				Poly [] polyList = tech.getShapeOfNode(ni);
				int total = polyList.length;
				for(int i=0; i<total; i++)
				{
					Poly nodePoly = polyList[i];
					nodePoly.transform(thisTrans);
					if (checkPolyAgainstPoly(geom, poly, oTopGeom, nodePoly, canCross))
						return true;
				}
			}
		} else
		{
			if (geom instanceof NodeInst) return false;
			ArcInst ai = (ArcInst)geom;
			ArcInst oAi = (ArcInst)oGeom;
			Technology tech = oAi.getProto().getTechnology();
			Poly [] polyList = tech.getShapeOfArc(oAi);
			int total = polyList.length;
			if ((oAi.getProto() == Schematics.tech.bus_arc && ai.getProto() == Schematics.tech.wire_arc) ||
				(oAi.getProto() == Schematics.tech.wire_arc && ai.getProto() == Schematics.tech.bus_arc)) canCross = true;
			for(int i=0; i<total; i++)
			{
				Poly oPoly = polyList[i];
				oPoly.transform(oTrans);
				if (checkPolyAgainstPoly(geom, poly, oTopGeom, oPoly, canCross))
					return true;
			}
		}
		return false;
	}

	/**
	 * Check polygon "poly" from object "geom" against
	 * polygon "opoly" from object "oGeom".
	 * If "canCross" is TRUE, they can cross each other (but an endpoint cannot touch).
	 * Returns TRUE if an error was found.
	 */
	private static boolean checkPolyAgainstPoly(Geometric geom, Poly poly, Geometric oGeom, Poly opoly, boolean canCross)
	{
		if (canCross)
		{
			Point2D [] pointList = poly.getPoints();
			Rectangle2D pointRect = new Rectangle2D.Double();
			boolean found = false;
			for(int i=0; i<pointList.length; i++)
			{
				pointRect.setRect(pointList[i].getX(), pointList[i].getY(), 0, 0);
				if (opoly.polyDistance(pointRect) <= 0) { found = true;   break; }
			}
			if (!found)
			{
				// none in "poly" touched one in "opoly", try other way
				pointList = opoly.getPoints();
				found = false;
				for(int i=0; i<pointList.length; i++)
				{
					pointRect.setRect(pointList[i].getX(), pointList[i].getY(), 0, 0);
					if (poly.polyDistance(pointRect) <= 0) { found = true;   break; }
				}
				if (!found) return false;
			}
		} else
		{
			if (!poly.intersects(opoly)) return false;
		}

		// report the error
		List<Geometric> geomList = new ArrayList<Geometric>();
		geomList.add(geom);
		geomList.add(oGeom);
		errorLogger.logError("Objects '" + geom + "' '" + oGeom + "' touch", geomList, null, geom.getParent(), 0);
		return true;
	}

	/**
	 * Method to check whether arc "ai" is colinear with another.
	 * Returns TRUE if an error was found.
	 */
	private static boolean checkColinear(ArcInst ai, ArcInst oAi)
	{
		// get information about the other line
		double fx = ai.getHeadLocation().getX();
		double fy = ai.getHeadLocation().getY();
		double tx = ai.getTailLocation().getX();
		double ty = ai.getTailLocation().getY();
		double oFx = oAi.getHeadLocation().getX();
		double oFy = oAi.getHeadLocation().getY();
		double oTx = oAi.getTailLocation().getX();
		double oTy = oAi.getTailLocation().getY();
		if (oFx == oTx && oFy == oTy) return false;

		// see if they are colinear
		double lowX = Math.min(fx, tx);
		double highX = Math.max(fx, tx);
		double lowY = Math.min(fy, ty);
		double highY = Math.max(fy, ty);
		int ang = 0;
		if (fx == tx)
		{
			// vertical line
			double oLow = Math.min(oFy, oTy);
			double oHigh = Math.max(oFy, oTy);
			if (oFx != fx || oTx != fx) return false;
			if (lowY >= oHigh || highY <= oLow) return false;
			ang = 900;
		} else if (fy == ty)
		{
			// horizontal line
			double oLow = Math.min(oFx, oTx);
			double oHigh = Math.max(oFx, oTx);
			if (oFy != fy || oTy != fy) return false;
			if (lowX >= oHigh || highX <= oLow) return false;
			ang = 0;
		} else
		{
			// general case
			ang = DBMath.figureAngle(new Point2D.Double(fx, fy), new Point2D.Double(tx, ty));
			int oAng = DBMath.figureAngle(new Point2D.Double(oFx, oFy), new Point2D.Double(oTx, oTy));
			if (ang != oAng && Math.min(ang, oAng) + 1800 != Math.max(ang, oAng)) return false;
			if ((oFx-fx) * (ty-fy) / (tx-fx) != oFy-fy) return false;
			if ((oTx-fx) * (ty-fy) / (tx-fx) != oTy-fy) return false;
			double oLow = Math.min(oFy, oTy);
			double oHigh = Math.max(oFy, oTy);
			if (lowY >= oHigh || highY <= oLow) return false;
			oLow = Math.min(oFx, oTx);
			oHigh = Math.max(oFx, oTx);
			if (lowX >= oHigh || highX <= oLow) return false;
		}
        Cell cell = ai.getParent();
		List<Geometric> geomList = new ArrayList<Geometric>();
		List<EPoint> ptList = new ArrayList<EPoint>();
		geomList.add(ai);
		geomList.add(oAi);

		// add information that shows the arcs
		ang = (ang + 900) % 3600;
		double dist = 2;
		double gDist = dist / 2;
		double ca = Math.cos(ang);   double sa = Math.sin(ang);
		double frX = fx + dist * ca;
		double frY = fy + dist * sa;
		double toX = tx + dist * ca;
		double toY = ty + dist * sa;
		fx = fx + gDist * ca;
		fy = fy + gDist * sa;
		tx = tx + gDist * ca;
		ty = ty + gDist * sa;
		ptList.add(new EPoint(frX, frY));   ptList.add(new EPoint(toX, toY));
		ptList.add(new EPoint(frX, frY));   ptList.add(new EPoint(fx, fy));
		ptList.add(new EPoint(tx, ty));     ptList.add(new EPoint(toX, toY));

		frX = oFx - dist * ca;
		frY = oFy - dist * sa;
		toX = oTx - dist * ca;
		toY = oTy - dist * sa;
		oFx = oFx - gDist * ca;
		oFy = oFy - gDist * sa;
		oTx = oTx - gDist * ca;
		oTy = oTy - gDist * sa;
		ptList.add(new EPoint(frX, frY));   ptList.add(new EPoint(toX, toY));
		ptList.add(new EPoint(frX, frY));   ptList.add(new EPoint(oFx, oFy));
		ptList.add(new EPoint(oTx, oTy));   ptList.add(new EPoint(toX, toY));
		errorLogger.logError("Arcs overlap", geomList, null, ptList, null, null, cell, 0);
		return true;
	}
}
