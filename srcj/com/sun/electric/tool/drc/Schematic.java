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
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Iterator;

public class Schematic
{
	private static HashSet cellsChecked;
    private static ErrorLogger errorLogger = null;

	public static ErrorLogger doCheck(Cell cell)
	{
		cellsChecked = new HashSet();

        if (errorLogger != null) errorLogger.delete();
		errorLogger = ErrorLogger.newInstance("Schematic DRC");
		checkSchematicCellRecursively(cell);
		errorLogger.termLogging(true);
		cellsChecked = null;
		return(errorLogger);
	}

	private static void checkSchematicCellRecursively(Cell cell)
	{
		cellsChecked.add(cell);

		// ignore if not a schematic
		if (!cell.isSchematic() && cell.getTechnology() != Schematics.tech)
			return;

		// recursively check contents
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			if (!(np instanceof Cell)) continue;
			Cell subCell = (Cell)np;

			Cell contentsCell = subCell.contentsView();
			if (contentsCell == null) contentsCell = subCell;
			if (cellsChecked.contains(contentsCell)) continue;

			// ignore documentation icon
			if (ni.isIconOfParent()) continue;

			checkSchematicCellRecursively(contentsCell);
		}

		// now check this cell
		System.out.println("Checking schematic cell " + cell.describe());
		checkSchematicCell(cell, false);
	}

	private static void checkSchematicCell(Cell cell, boolean justThis)
	{
		if (justThis) errorLogger = ErrorLogger.newInstance("Schematic DRC");
		int initialErrorCount = errorLogger.getNumErrors();
		Netlist netlist = NetworkTool.getUserNetlist(cell);
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() instanceof PrimitiveNode &&
				ni.getProto().getTechnology() == Generic.tech) continue;
			schematicDoCheck(netlist, ni);
		}
		for(Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			schematicDoCheck(netlist, ai);
		}
		int errorCount = errorLogger.getNumErrors();
		int thisErrors = errorCount - initialErrorCount;
		String indent = "   ";
		if (justThis) indent = "";
		if (thisErrors == 0) System.out.println(indent + "No errors found"); else
			System.out.println(indent + thisErrors + " errors found");
		if (justThis) errorLogger.termLogging(true);
	}

	/*
	 * Method to check schematic object "geom".
	 */
	private static void schematicDoCheck(Netlist netlist, Geometric geom)
	{
		Cell cell = geom.getParent();
		if (geom instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)geom;
			NodeProto np = ni.getProto();

			// check for bus pins that don't connect to any bus arcs
			if (np == Schematics.tech.busPinNode)
			{
				// proceed only if it has no exports on it
				if (ni.getNumExports() == 0)
				{
					// must not connect to any bus arcs
					boolean found = false;
					for(Iterator it = ni.getConnections(); it.hasNext(); )
					{
						Connection con = (Connection)it.next();
						if (con.getArc().getProto() == Schematics.tech.bus_arc) { found = true;   break; }
					}
					if (!found)
					{
						ErrorLogger.MessageLog err = errorLogger.logError("Bus pin does not connect to any bus arcs", cell, 0);
						err.addGeom(geom, true, cell, null);
						return;
					}
				}

				// flag bus pin if more than 1 wire is connected
				int i = 0;
				for(Iterator it = ni.getConnections(); it.hasNext(); )
				{
					Connection con = (Connection)it.next();
					if (con.getArc().getProto() == Schematics.tech.wire_arc) i++;
				}
				if (i > 1)
				{
					ErrorLogger.MessageLog err = errorLogger.logError("Wire arcs cannot connect through a bus pin", cell, 0);
					err.addGeom(geom, true, cell, null);
					for(Iterator it = ni.getConnections(); it.hasNext(); )
					{
						Connection con = (Connection)it.next();
						if (con.getArc().getProto() == Schematics.tech.wire_arc) i++;
							err.addGeom(con.getArc(), true, cell, null);
					}
					return;
				}
			}

			// check all pins
			if (np.getFunction() == PrimitiveNode.Function.PIN)
			{
				// may be stranded if there are no exports or arcs
				if (ni.getNumExports() == 0 && ni.getNumConnections() == 0)
				{
					// see if the pin has displayable variables on it
					boolean found = false;
					for(Iterator it = ni.getVariables(); it.hasNext(); )
					{
						Variable var = (Variable)it.next();
						if (var.isDisplay()) { found = true;   break; }
					}
					if (!found)
					{
						ErrorLogger.MessageLog err = errorLogger.logError("Stranded pin (not connected or exported)", cell, 0);
						err.addGeom(geom, true, cell, null);
						return;
					}
				}

				if (ni.isInlinePin())
				{
					ErrorLogger.MessageLog err = errorLogger.logError("Unnecessary pin (between 2 arcs)", cell, 0);
					err.addGeom(geom, true, cell, null);
					return;
				}

				Point2D pinLoc = ni.invisiblePinWithOffsetText(false);
				if (pinLoc != null)
				{
					ErrorLogger.MessageLog err = errorLogger.logError("Invisible pin has text in different location", cell, 0);
					err.addGeom(geom, true, cell, null);
					err.addLine(ni.getAnchorCenterX(), ni.getAnchorCenterY(), pinLoc.getX(), pinLoc.getY(), cell);
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
				for(Iterator it = ni.getVariables(); it.hasNext(); )
				{
					Variable var = (Variable)it.next();
					TextDescriptor td = var.getTextDescriptor();
					if (!td.isParam()) continue;

					TextDescriptor foundTD = null;
					for(Iterator cIt = contentsCell.getVariables(); cIt.hasNext(); )
					{
						Variable fVar = (Variable)cIt.next();
						TextDescriptor fTd = fVar.getTextDescriptor();
						if (!fTd.isParam()) continue;
						if (var.getKey() == fVar.getKey()) { foundTD = fTd;   break; }
					}
					if (foundTD == null)
					{
						// this node's parameter is no longer on the cell: delete from instance
						String trueVarName = var.getTrueName();
						ErrorLogger.MessageLog err = errorLogger.logError("Parameter '" + trueVarName + "' on node " + ni.describe() +
//							" is invalid and has been deleted", cell, 0);
							" is invalid", cell, 0);
						err.addGeom(geom, true, cell, null);

						// this is broken:
//						ni.delVar(var.getKey());
//						i--;
					} else
					{
						// this node's parameter is still on the cell: make sure units are OK
						if (td.getUnit() != foundTD.getUnit())
						{
							String trueVarName = var.getTrueName();
							ErrorLogger.MessageLog err = errorLogger.logError("Parameter '" + trueVarName + "' on node " + ni.describe() +
								" had incorrect units (now fixed)", cell, 0);
							err.addGeom(geom, true, cell, null);
							td.setUnit(foundTD.getUnit());
						}

						// make sure visibility is OK
						if (foundTD.isInterior())
						{
							if (var.isDisplay())
							{
								String trueVarName = var.getTrueName();
								ErrorLogger.MessageLog err = errorLogger.logError("Parameter '" + trueVarName + "' on node " + ni.describe() +
									" should not be visible (now fixed)", cell, 0);
								err.addGeom(geom, true, cell, null);
								var.setDisplay(false);
							}
						} else
						{
							if (!var.isDisplay())
							{
								String trueVarName = var.getTrueName();
								ErrorLogger.MessageLog err = errorLogger.logError("Parameter '" + trueVarName + "' on node " + ni.describe() +
									" should be visible (now fixed)", cell, 0);
								err.addGeom(geom, true, cell, null);
								var.setDisplay(true);
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
					Connection con = ai.getConnection(i);
					NodeInst ni = con.getPortInst().getNodeInst();

					// OK if not a pin
					if (ni.getProto().getFunction() != PrimitiveNode.Function.PIN) continue;

					// OK if it has exports on it
					if (ni.getNumExports() != 0) continue;

					// OK if it connects to more than 1 arc
					if (ni.getNumConnections() != 1) continue;

					// the arc dangles
					ErrorLogger.MessageLog err = errorLogger.logError("Arc dangles", cell, 0);
					err.addGeom(geom, true, cell, null);
					return;
				}
			}

			// check to see if its width is sensible
			int signals = netlist.getBusWidth(ai);
			if (signals < 1) signals = 1;
			for(int i=0; i<2; i++)
			{
				Connection con = ai.getConnection(i);
				PortInst pi = con.getPortInst();
				NodeInst ni = pi.getNodeInst();
				if (!(ni.getProto() instanceof Cell)) continue;
				Cell subNp = (Cell)ni.getProto();
				PortProto pp = pi.getPortProto();

				Cell np = subNp.contentsView();
				if (np != null)
				{
					pp = ((Export)pi.getPortProto()).getEquivalent();
					if (pp == null || pp == pi.getPortProto())
					{
						ErrorLogger.MessageLog err = errorLogger.logError("Arc " + ai.describe() + " connects to port " +
							pi.getPortProto().getName() + " of node " + ni.describe() +
							", but there is no equivalent port in cell " + np.describe(), cell, 0);
						err.addGeom(geom, true, cell, null);
						err.addGeom(ni, true, cell, null);
						continue;
					}
				}

				int portWidth = netlist.getBusWidth((Export)pp);
				if (portWidth < 1) portWidth = 1;
				int nodeSize = ni.getNameKey().busWidth();
				if (nodeSize <= 0) nodeSize = 1;
				if (signals != portWidth && signals != portWidth*nodeSize)
				{
					ErrorLogger.MessageLog err = errorLogger.logError("Arc " + ai.describe() + " (" + signals + " wide) connects to port " +
						pp.getName() + " of node " + ni.describe() +
						" (" + portWidth + " wide)", cell, 0);
					err.addGeom(geom, true, cell, null);
					err.addGeom(ni, true, cell, null);
				}
			}
		}

		// check for overlap
		checkObjectVicinity(netlist, geom, geom, DBMath.MATID);
	}

	/*
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
			if (np instanceof Cell)
			{
				if (ni.isExpanded())
				{
					// expand the instance
					AffineTransform subRot = ni.translateOut();
					subRot.preConcatenate(localTrans);
					Cell subCell = (Cell)np;
					for(Iterator it = subCell.getNodes(); it.hasNext(); )
					{
						NodeInst subNi = (NodeInst)it.next();
						checkObjectVicinity(netlist, topGeom, subNi, subRot); 
					}
					for(Iterator it = subCell.getArcs(); it.hasNext(); )
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
		for(Iterator sIt = cell.searchIterator(bounds); sIt.hasNext(); )
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
				if (oNi.getProto() instanceof PrimitiveNode &&
					oNi.getProto().getTechnology() == Generic.tech) continue;
				if (geom instanceof NodeInst)
				{
					// this is node, nearby is node: see if two nodes touch
					boolean found = false;
					for(Iterator it = ni.getConnections(); it.hasNext(); )
					{
						Connection con = (Connection)it.next();
						for(Iterator oIt = oNi.getConnections(); oIt.hasNext(); )
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
					for(Iterator oIt = oNi.getConnections(); oIt.hasNext(); )
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
					for(Iterator it = ni.getConnections(); it.hasNext(); )
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
						NodeInst headNi = ai.getHead().getPortInst().getNodeInst();
						NodeInst tailNi = ai.getTail().getPortInst().getNodeInst();
						if (headNi.getProto() != Schematics.tech.wireConNode) headNi = null;
						if (tailNi.getProto() != Schematics.tech.wireConNode) tailNi = null;
						NodeInst oHeadNi = oAi.getHead().getPortInst().getNodeInst();
						NodeInst oTailNi = oAi.getTail().getPortInst().getNodeInst();
						if (headNi == oHeadNi || headNi == oTailNi || tailNi == oHeadNi || tailNi == oTailNi) connected = true;
					}
					if (!connected)
					{
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
			NodeInst ni = (NodeInst)oGeom;
			NodeProto np = ni.getProto();
			AffineTransform thisTrans = ni.rotateOut();
			thisTrans.preConcatenate(oTrans);
			if (np instanceof Cell)
			{
				AffineTransform subRot = ni.translateOut();
				subRot.preConcatenate(thisTrans);
				Cell subCell = (Cell)np;
				for(Iterator it = subCell.getNodes(); it.hasNext(); )
				{
					NodeInst subNi = (NodeInst)it.next();
					if (checkPoly(geom, poly, oTopGeom, subNi, subRot, canCross))
						return true;
				}
				for(Iterator it = subCell.getArcs(); it.hasNext(); )
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
			ArcInst ai = (ArcInst)oGeom;
			Technology tech = ai.getProto().getTechnology();
			Poly [] polyList = tech.getShapeOfArc(ai);
			int total = polyList.length;
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
		ErrorLogger.MessageLog err = errorLogger.logError("Objects touch", geom.getParent(), 0);
		err.addGeom(geom, true, geom.getParent(), null);
		err.addGeom(oGeom, true, geom.getParent(), null);
		return true;
	}

	/**
	 * Method to check whether arc "ai" is colinear with another.
	 * Returns TRUE if an error was found.
	 */
	private static boolean checkColinear(ArcInst ai, ArcInst oAi)
	{
		// get information about the other line
		double fx = ai.getHead().getLocation().getX();
		double fy = ai.getHead().getLocation().getY();
		double tx = ai.getTail().getLocation().getX();
		double ty = ai.getTail().getLocation().getY();
		double oFx = oAi.getHead().getLocation().getX();
		double oFy = oAi.getHead().getLocation().getY();
		double oTx = oAi.getTail().getLocation().getX();
		double oTy = oAi.getTail().getLocation().getY();
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
		ErrorLogger.MessageLog err = errorLogger.logError("Arcs overlap", cell, 0);
		err.addGeom(ai, true, cell, null);
		err.addGeom(oAi, true, cell, null);

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
		err.addLine(frX, frY, toX, toY, cell);
		err.addLine(frX, frY, fx, fy, cell);
		err.addLine(tx, ty, toX, toY, cell);

		frX = oFx - dist * ca;
		frY = oFy - dist * sa;
		toX = oTx - dist * ca;
		toY = oTy - dist * sa;
		oFx = oFx - gDist * ca;
		oFy = oFy - gDist * sa;
		oTx = oTx - gDist * ca;
		oTy = oTy - gDist * sa;
		err.addLine(frX, frY, toX, toY, cell);
		err.addLine(frX, frY, oFx, oFy, cell);
		err.addLine(oTx, oTy, toX, toY, cell);
		return true;
	}
}
