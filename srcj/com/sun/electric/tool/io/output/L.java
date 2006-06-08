/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: L.java
 * Input/output tool: L Netlist output
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

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.TransistorSize;

import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This is the netlister for L.
 */
public class L extends Output
{
	// node types returned by "getNodeType"
	private static final int TRUEPIN    = 1;
	private static final int TRANSISTOR = 2;
	private static final int INSTANCE   = 3;
	private static final int OTHERNODE  = 4;
	private Set<Cell> cellsSeen;
	/** the results of calling "transistorPorts". */	private PortInst gateLeft, gateRight, activeTop, activeBottom;

	/**
	 * The main entry point for L deck writing.
     * @param cell the top-level cell to write.
	 * @param filePath the disk file to create.
	 */
	public static void writeLFile(Cell cell, String filePath)
	{
		L out = new L();
		if (out.openTextOutputStream(filePath)) return;
		out.writeLCells(cell);
		if (out.closeTextOutputStream()) return;
		System.out.println(filePath + " written");
	}

	/**
	 * Creates a new instance of the L netlister.
	 */
	L()
	{
	}

	/**
	 * Method to write all cells below a given Cell.
	 * @param cell the top Cell of the hierarchy to write.
	 */
	private void writeLCells(Cell cell)
	{
		printWriter.println("L:: TECH ANY");
		cellsSeen = new HashSet<Cell>();
		writeLCell(cell);
	}

	/**
	 * Method to write "L" for a cell.
	 * @param cell the Cell to write.
	 */
	private void writeLCell(Cell cell)
	{
		// if there are any sub-cells that have not been written, write them
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (!ni.isCellInstance()) continue;
			if (ni.isIconOfParent()) continue;
			Cell np = (Cell)ni.getProto();

			// convert body cells to contents cells
			Cell oNp = np.contentsView();
			if (oNp != null) np = oNp;

			// don't recurse if this cell has already been written
			if (cellsSeen.contains(np)) continue;

			// recurse to the bottom
			writeLCell(np);
		}
		cellsSeen.add(cell);

		// write the cell header
		printWriter.println("");
		if (cell.getView() == View.LAYOUT) printWriter.print("LAYOUT ");
		if (cell.isSchematic()) printWriter.print("SCHEMATIC ");
		if (cell.isIcon()) printWriter.print("ICON ");
		if (cell.getView() == View.LAYOUTSKEL) printWriter.print("BBOX ");
		printWriter.println("CELL " + getLegalName(cell.getName()) + " ( )\n{");

		// write the bounding box
		Rectangle2D bounds = cell.getBounds();
		printWriter.println("#bbox: ll= (" + TextUtils.formatDouble(bounds.getMinX()) + "," +
			TextUtils.formatDouble(bounds.getMinY()) + ") ur= (" +
			TextUtils.formatDouble(bounds.getMaxX()) + "," +
			TextUtils.formatDouble(bounds.getMaxY()) + ")");

		// write the ports
		for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
		{
			Export e = (Export)it.next();
			Poly poly = e.getOriginalPort().getPoly();
			double xPos = poly.getCenterX();
			double yPos = poly.getCenterY();
			String type = "";
			if (e.getCharacteristic() == PortCharacteristic.GND) type = "GND"; else
				if (e.getCharacteristic() == PortCharacteristic.PWR) type = "VDD"; else
					if (e.getCharacteristic() == PortCharacteristic.IN) type = "IN"; else
						if (e.getCharacteristic() == PortCharacteristic.OUT) type = "OUT"; else
							if (e.getCharacteristic() == PortCharacteristic.BIDIR) type = "INOUT";
			ArcProto ap = e.getBasePort().getConnections()[0];
			String lay = getArcFunctionName(ap, ap.getName());
			printWriter.println("\t" + type + " " + lay + " " + getLegalName(e.getName()) +
				" (" + TextUtils.formatDouble(xPos) + "," + TextUtils.formatDouble(yPos) + ") ;");
		}
		printWriter.println("");

		// write the components
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (getNodeType(ni) == TRUEPIN) continue;
			NodeProto np = ni.getProto();
			PrimitiveNode.Function fun = ni.getFunction();

			// determine type of component
			String type = np.getName();
			if (ni.isCellInstance())
			{
				// ignore recursive references (showing icon in contents)
				if (ni.isIconOfParent()) continue;

				// convert body cells to contents cells
				Cell oNp = ((Cell)np).contentsView();
				if (oNp != null) np = oNp;
				type = np.getName();
				printWriter.print("\tINST " + type + " " + getLegalName(ni.getName()));
			} else
			{
				PrimitiveNode npPrim = (PrimitiveNode)np;
				if (fun == PrimitiveNode.Function.PIN)
				{
					// if pin is an export, don't write separate node statement
					if (ni.hasExports()) continue;
//					if (ni.getNumExports() > 0) continue;
					PrimitivePort primPp = (PrimitivePort)npPrim.getPort(0);
					ArcProto ap = primPp.getConnections()[0];
					type = "NODE " + getArcFunctionName(ap, "???");
				}

				// special type names for well/substrate contacts
				if (fun == PrimitiveNode.Function.WELL) type = "MNSUB";
				if (fun == PrimitiveNode.Function.SUBSTRATE) type = "MPSUB";

				// special type names for contacts
				if (fun == PrimitiveNode.Function.CONTACT || fun == PrimitiveNode.Function.CONNECT)
				{
					boolean conMetal1 = false, conMetal2 = false, conPActive = false, conNActive = false, conPoly = false;
					for(int j=0; j<npPrim.getNumPorts(); j++)
					{
						PrimitivePort primPp = (PrimitivePort)npPrim.getPort(j);
						ArcProto [] arcs = primPp.getConnections();
						for(int k=0; k<arcs.length; k++)
						{
							ArcProto ap = arcs[k];
							ArcProto.Function aFun = ap.getFunction();
							if (aFun == ArcProto.Function.METAL1) conMetal1 = true;
							if (aFun == ArcProto.Function.METAL2) conMetal2 = true;
							if (aFun == ArcProto.Function.POLY1) conPoly = true;
							if (aFun == ArcProto.Function.DIFFP) conPActive = true;
							if (aFun == ArcProto.Function.DIFFN) conNActive = true;
						}
					}
					if (conMetal1)
					{
						if (conMetal2) type = "M1M2"; else
							if (conPoly) type = "MPOLY"; else
								if (conPActive) type = "MPDIFF"; else
									if (conNActive) type = "MNDIFF";
					}
				}

				// special type names for transistors
				if (fun == PrimitiveNode.Function.TRANMOS) type = "TN";
				if (fun == PrimitiveNode.Function.TRADMOS) type = "TD";
				if (fun == PrimitiveNode.Function.TRAPMOS) type = "TP";

				// write the type and name
				printWriter.print("\t" + type + " " + getLegalName(ni.getName()));
			}

			// write rotation
			Orientation or = ni.getOrient();
			int oldRotation = or.getCAngle();
			int oldTranspose = or.isCTranspose() ? 1 : 0;

			if (oldRotation != 0 || oldTranspose != 0)
			{
				if (oldTranspose != 0)
				{
					printWriter.print(" RX");
					oldRotation = (oldRotation+2700) % 3600;
				}
				printWriter.print(" R" + TextUtils.formatDouble(oldRotation/10));
			}

			// write size if nonstandard
			if (ni.getXSize() != np.getDefWidth() || ni.getYSize() != np.getDefHeight())
			{
				SizeOffset so = ni.getSizeOffset();
				double wid = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
				double len = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();
				if (fun == PrimitiveNode.Function.TRANMOS || fun == PrimitiveNode.Function.TRADMOS || fun == PrimitiveNode.Function.TRAPMOS)
				{
					TransistorSize ts = ni.getTransistorSize(null);
					len = ts.getDoubleLength();
					wid = ts.getDoubleWidth();
				}
				printWriter.print(" W=" + TextUtils.formatDouble(wid) + " L=" + TextUtils.formatDouble(len));
			}

			// write location
			if (ni.isCellInstance())
			{
				Rectangle2D cellBounds = ((Cell)ni.getProto()).getBounds();
				printWriter.println(" AT (" + TextUtils.formatDouble(ni.getTrueCenterX() - cellBounds.getCenterX()) + "," +
					TextUtils.formatDouble(ni.getTrueCenterY() - cellBounds.getCenterY()) + ") ;");
			} else
			{
				printWriter.println(" AT (" + TextUtils.formatDouble(ni.getTrueCenterX()) + "," +
					TextUtils.formatDouble(ni.getTrueCenterY()) + ") ;");
			}
		}
		printWriter.println("");

		// write all arcs connected to nodes
		Set<ArcInst> arcsSeen = new HashSet<ArcInst>();
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			int nature = getNodeType(ni);
			if (nature == TRUEPIN) continue;
			for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
			{
				Connection con = (Connection)cIt.next();
				ArcInst ai = con.getArc();
				if (arcsSeen.contains(ai)) continue;
				printWriter.print("\tWIRE");
				String alt = getArcFunctionName(ai.getProto(), null);
				if (alt != null) printWriter.print(" " + alt);

				// write the wire width if nonstandard
				if (ai.getWidth() != ai.getProto().getDefaultWidth())
					printWriter.print(" W=" + TextUtils.formatDouble(ai.getWidth()-ai.getProto().getWidthOffset()));

				// write the starting node name (use port name if pin is an export)
				if (ni.hasExports() && ni.getFunction() == PrimitiveNode.Function.PIN)
//				if (ni.getNumExports() > 0 && ni.getFunction() == PrimitiveNode.Function.PIN)
				{
					Export e = (Export)ni.getExports().next();
					printWriter.print(" " + getLegalName(e.getName()));
				} else
				{
					printWriter.print(" "+ getLegalName(ni.getName()));
				}

				// qualify node name with port name if a transistor or instance
				PortInst pi = con.getPortInst();
				if (nature == TRANSISTOR)
				{
					transistorPorts(ni);
					if (pi == gateLeft) printWriter.print(".gl");
					if (pi == activeTop) printWriter.print(".d");
					if (pi == gateRight) printWriter.print(".gr");
					if (pi == activeBottom) printWriter.print(".s");
				} else if (nature == INSTANCE)
					printWriter.print("." + getLegalName(pi.getPortProto().getName()));

				// prepare to run along the wire to a terminating node
                int thatEnd = 1 - con.getEndIndex();
//				int thatEnd = 0;
//				if (ai.getConnection(0) == con) thatEnd = 1;
				String lastDir = "";
				double segDist = -1;
				int segCount = 0;
				int eNature = 0;
				NodeInst oNi = null;
				for(;;)
				{
					// get information about this segment (arc "ai")
					arcsSeen.add(ai);
					int thisEnd = 1 - thatEnd;
					String dir = lastDir;
					if (ai.getLocation(thatEnd).getX() == ai.getLocation(thisEnd).getX())
					{
						if (ai.getLocation(thatEnd).getY() > ai.getLocation(thisEnd).getY()) dir = "UP"; else
							if (ai.getLocation(thatEnd).getY() < ai.getLocation(thisEnd).getY()) dir = "DOWN";
					} else if (ai.getLocation(thatEnd).getY() == ai.getLocation(thisEnd).getY())
					{
						if (ai.getLocation(thatEnd).getX() > ai.getLocation(thisEnd).getX()) dir = "RIGHT"; else
							if (ai.getLocation(thatEnd).getX() < ai.getLocation(thisEnd).getX()) dir = "LEFT";
					}

					// if segment is different from last, write out last one
					if (!dir.equals(lastDir) && lastDir.length() > 0)
					{
						printWriter.print(" " + lastDir);
						if (segDist >= 0) printWriter.print("=" + TextUtils.formatDouble(segDist));
						segDist = -1;
						segCount++;
					}

					// remember this segment's direction and length
					lastDir = dir;
					oNi = ai.getPortInst(thatEnd).getNodeInst();
					eNature = getNodeType(oNi);
					if ((nature != TRANSISTOR || segCount > 0) && eNature != TRANSISTOR)
					{
						if (segDist < 0) segDist = 0;
						segDist += ai.getLength();
					}

					// if other node not a pin, stop now
					if (eNature != TRUEPIN) break;

					// end the loop if more than 1 wire out of next node "oNi"
					int tot = 0;
    				int ot = 0;
					ArcInst oAi = null;
					for(Iterator<Connection> oCIt = oNi.getConnections(); oCIt.hasNext(); )
					{
						Connection oCon = (Connection)oCIt.next();
						if (arcsSeen.contains(oCon.getArc())) continue;
						oAi = oCon.getArc();
						tot++;
                        ot = 1 - oCon.getEndIndex();
//						if (oAi.getConnection(0) == oCon) ot = 1; else ot = 0;
					}
					if (tot != 1) break;
					ai = oAi;
       				thatEnd = ot;
				}
				if (lastDir.length() > 0)
				{
					printWriter.print(" " + lastDir);
					if (segDist >= 0) printWriter.print("=" + TextUtils.formatDouble(segDist));
				} else printWriter.print(" TO");

				// write the terminating node name (use port name if pin is an export)
				if (oNi.hasExports() && oNi.getFunction() == PrimitiveNode.Function.PIN)
//				if (oNi.getNumExports() > 0 && oNi.getFunction() == PrimitiveNode.Function.PIN)
				{
					Export e = (Export)oNi.getExports().next();
					printWriter.print(" " + getLegalName(e.getName()));
				} else
				{
					printWriter.print(" " + getLegalName(oNi.getName()));
				}

				// qualify node name with port name if a transistor or an instance
				PortInst oPi = ai.getPortInst(thatEnd);
				if (eNature == TRANSISTOR)
				{
					transistorPorts(oNi);
					if (oPi == gateLeft) printWriter.print(".gl");
					if (oPi == activeTop) printWriter.print(".d");
					if (oPi == gateRight) printWriter.print(".gr");
					if (oPi == activeBottom) printWriter.print(".s");
				} else if (eNature == INSTANCE)
					printWriter.print("." + getLegalName(oPi.getPortProto().getName()));
				printWriter.println(" ;");
			}
		}

		// write any unmentioned wires (shouldn't be any)
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			if (arcsSeen.contains(ai)) continue;
			printWriter.println("# WIRE " + ai.describe(true) + " not described!!");
		}
		printWriter.println("}");
	}

	/**
	 * Method to return the ports of a transistor in field variables "gateLeft", "gateRight",
	 * "activeTop", and "activeBottom".  If "gateRight" is null, there is only
	 * one gate port.
	 * @param ni the NodeInst to analyze.
	 */
	private void transistorPorts(NodeInst ni)
	{
		PrimitiveNode.Function fun = ni.getFunction();
		gateLeft = activeTop = gateRight = activeBottom = null;
		if (ni.getNumPortInsts() < 3) return;
		gateLeft = ni.getPortInst(0);
		activeTop = ni.getPortInst(1);
		gateRight = ni.getPortInst(2);
		if (ni.getNumPortInsts() == 3 || fun == PrimitiveNode.Function.TRANPN ||
			fun == PrimitiveNode.Function.TRAPNP || fun == PrimitiveNode.Function.TRA4NMOS || fun == PrimitiveNode.Function.TRA4DMOS ||
			fun == PrimitiveNode.Function.TRA4PMOS || fun == PrimitiveNode.Function.TRA4NPN || fun == PrimitiveNode.Function.TRA4PNP ||
			fun == PrimitiveNode.Function.TRA4NJFET || fun == PrimitiveNode.Function.TRA4PJFET ||
			fun == PrimitiveNode.Function.TRA4DMES || fun == PrimitiveNode.Function.TRA4EMES)
		{
			activeBottom = gateRight;
			gateRight = null;
		} else
		{
			activeBottom = ni.getPortInst(3);
		}
	}

	/**
	 * Method to convert a name to a legal L name.
	 * @param name the name to convert.
	 * @return the legal L name to use.
	 */
	private String getLegalName(String name)
	{
		// check for reserved names
		if (name.equals("VDD")) return "VDDXXX";
		if (name.equals("GND")) return "GNDXXX";

		// check for special characters
		boolean badChars = false;
		for(int i=0; i<name.length(); i++)
			if (!TextUtils.isLetterOrDigit(name.charAt(i))) badChars = true;
		if (!badChars) return name;

		// name has special characters: remove them
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<name.length(); i++)
			if (TextUtils.isLetterOrDigit(name.charAt(i))) sb.append(name.charAt(i));
		return sb.toString();
	}

	/**
	 * Method to determine the type of a NodeInst.
	 * @param ni the NodeInst to analyze.
	 * @return
	 *    TRUEPIN    if a true pin (exactly two connections)<BR>
	 *    TRANSISTOR if a transistor<BR>
	 *    INSTANCE   if a cell instance<BR>
	 *    OTHERNODE  otherwise.
	 */
	private int getNodeType(NodeInst ni)
	{
		if (ni.isCellInstance()) return INSTANCE;
		PrimitiveNode.Function fun = ni.getFunction();
		if (fun == PrimitiveNode.Function.TRANMOS || fun == PrimitiveNode.Function.TRADMOS || fun == PrimitiveNode.Function.TRAPMOS) return TRANSISTOR;
		if (fun != PrimitiveNode.Function.PIN) return OTHERNODE;
		if (ni.getNumConnections() != 2) return OTHERNODE;
		return TRUEPIN;
	}

	/**
	 * Method to return the name of an arc prototype's function.
	 * @param ap the ArcProto to analyze.
	 * @param def the default name to return if nothing can be determined.
	 * @return the name of the ArcProto's function.
	 */
	private String getArcFunctionName(ArcProto ap, String def)
	{
		ArcProto.Function fun = ap.getFunction();
		if (fun.isMetal())
		{
			return "MET" + fun.getLevel();
		}
		if (fun.isPoly()) return "POLY";
		if (fun == ArcProto.Function.DIFFP) return "PDIFF";
		if (fun == ArcProto.Function.DIFFN) return "NDIFF";
		if (fun == ArcProto.Function.DIFFS) return "NWELL";
		if (fun == ArcProto.Function.DIFFW) return "PWELL";
		return def;
	}
}
