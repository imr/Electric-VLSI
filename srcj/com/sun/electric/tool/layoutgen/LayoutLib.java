/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayoutLib.java
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
package com.sun.electric.tool.layoutgen;

import java.awt.geom.*;
import java.util.*;

import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.technology.*;
import com.sun.electric.tool.io.*;

/**
 * The LayoutLib class provides an assortment of routines that I
 * found to be useful for programatic layout generation.
 */
public class LayoutLib {
	// ---------------------------- public data ------------------------------
	/** Use +/- infinity to mean: "use the default size" */
	public static final double DEF_SIZE = Double.POSITIVE_INFINITY;

	// ---------------------------- public methods ---------------------------
	/**
	 * Print a message, dump a stack trace, and throw a RuntimeException if 
	 * predicate is true.
	 * @param if <code>pred</code> is true an error has occurred.
	 * @param msg the message to print when an error occurs. 
	 */
	public static void error(boolean pred, String msg) {
		if (!pred) return;
		RuntimeException e = new RuntimeException(msg);
		// Oddly enough, the following prints a stack trace in the
		// Electric message window only
		e.printStackTrace();

		// while the following prints a stack trace in the Jose window
		// only.
		throw e;
	}
	/** 
	 * Open a library for reading.
	 * <p> If a library named: <code>libNm</code> is already open then 
	 * return it. Otherwise look for the file named: <code>libFileNm</code>
	 * and open that library.
	 * @param libNm the name of the Library
	 * @param libFileNm the name of the Library file on disk
	 * @return the open Library or null if it can't be found
	 */
	public static Library openLibForRead(String libNm, String libFileNm) {
		Library lib = Library.findLibrary(libNm);
		if (lib==null) {
			lib = Input.readLibrary(libFileNm, Input.ImportType.BINARY);
		}
		error(lib==null, "can't open Library for reading: "+libFileNm);
		return lib;
	}
	/**
	 * Open a library for modification.
	 * <p> If a library named <code>libNm</code> is already open then 
	 * return it. Otherwise look for the file named: <code>libFileNm</code>
	 * and open that library. If the file doesn't exist on disk then create 
	 * a new Library. 
	 * @param libNm the name of the Library
	 * @param libFileNm the name of the Library file on disk
	 * @return the library
	 */
	public static Library openLibForModify(String libNm, String libFileNm) {
		// return an open Library if it exists
		Library lib = Library.findLibrary(libNm);
		if (lib!=null)  return lib;

		// open a Library file if it exists
		lib = Input.readLibrary(libFileNm, Input.ImportType.BINARY);
		if (lib!=null)  return lib;
		
		// create a new Library
		lib = Library.newInstance(libNm, libFileNm);

		error(lib==null, "can't open Library for modify: "+libNm);
		return lib;
	}
	/**
	 * Write a library in binary format.
	 * @param lib the library to be written.
	 */
	public static void writeLibrary(Library lib) {
		Output.writeLibrary(lib, Output.ExportType.BINARY);
	}
	/**
	 * Get the width of an ArcInst.
	 * <p>Subtract off the "width offset" so that we return the same width as 
	 * would be reported by the GUI.
 	 * @param ai the ArcInst whose width is reported.
	 * @return the width of the ArcInst. 
	 */
	public static double getArcInstWidth(ArcInst ai) {
		return ai.getWidth() - ai.getProto().getWidthOffset();
	}
	/**
	 * Get the default width of a NodeProto.
	 * <p>Subtract off the "width offset" so that we return the same width as
	 * would be reported by the GUI.
	 * @param np the NodeProto we want the width of.
	 * @return the width of the NodeProto.
	 */
	public static double getNodeProtoWidth(NodeProto np) {
		SizeOffset so = np.getSizeOffset();
		return np.getDefWidth() - so.getLowXOffset() - so.getHighXOffset();
	}
	/**
	 * Get the default height of a NodeProto.
	 * <p>Subtract off the "height offset" so that we return the same height as
	 * would be reported by the GUI.
	 * @param np the NodeProto we want the height of.
	 * @return the width of the NodeProto.
	 */
	public static double getNodeProtoHeight(NodeProto np) {
		SizeOffset so = np.getSizeOffset();
		return np.getDefHeight() - so.getLowYOffset() - so.getHighYOffset();
	}
	/**
	 * Find the width of the widest wire connected hierarchically to the given
	 * PortInst.  
	 * @param port the PortInst to check for attached wires.
	 * @return the width of the widest wire. This width excludes the
	 * "size_offset" so it matches the GUI's notion of "width". If no wire is 
	 * found then return DEF_SIZE.
	 */
	public static double widestWireWidth(PortInst port) {
		NodeInst ni = port.getNodeInst();
		PortProto pp = port.getPortProto();
		double maxWid = -1;
		for (Iterator arcs=getArcInstsOnPortInst(port); arcs.hasNext();) {
			ArcInst ai = (ArcInst)arcs.next();
			maxWid = Math.max(maxWid, getArcInstWidth(ai));
		}
		if (pp instanceof Export) {
			double check = widestWireWidth(((Export)pp).getOriginalPort());
			maxWid = Math.max(maxWid, check);
		}
		return maxWid;
	}

	/** Return a list of ArcInsts attached to this PortInst.
	 * @param pi PortInst on which to find attached ArcInsts.
	 * @param list of ArcInsts. */
	public static Iterator getArcInstsOnPortInst(PortInst pi) {
		ArrayList arcs = new ArrayList();
		NodeInst ni = pi.getNodeInst();
		for (Iterator it=ni.getConnections(); it.hasNext();) {
			Connection c = (Connection) it.next();
			if (c.getPortInst()==pi)  arcs.add(c.getArc());
		}
		return arcs.iterator();
	}

	/**
	 * Create a new NodeInst.
	 *
	 * <p>The specified dimensions will match those reported by the
	 * GUI.
	 * @param np the NodeProto to instantiate.
	 * @param width the width of the NodeInst. Add "size_offset" to
	 * <code>width</code> so that we end up with a NodeInst that the
	 * GUI says is <code>width</code> wide. The <code>width</code>
	 * argument is ignored if the NodeProto is a Cell since the width
	 * of a NodeInst of a Cell must match the width of the Cell.
	 * @param height the height of the NodeInst. Add "size_offset" to
	 * <code>height</code> so we end up with a NodeInst that the GUI
	 * says is <code>height</code> high. The <code>height</code>
	 * argument is ignored if the NodeProto is a Cell since the height
	 * of a NodeInst of a Cell must match the height of the Cell.
	 * @param x the desired X coordinate of the NodeProto's reference
	 * point in the coordinate space of the parent.
	 * @param y the desired Y coordinate of the NodeProto's reference
	 * point in the coordinate space of the parent.
	 * @param angle the angle of rotation about the NodeProto's
	 * reference point.
	 * @param parent the Cell that will contain the NodeInst.
	 * @return the new NodeInst. 
	 */
	public static NodeInst newNodeInst(NodeProto np, 
	                                   double width, double height,
		                        	   double x, double y,
		                        	   double angle, Cell parent) {
		if (np instanceof Cell) {
			width = (width<0 ? -1 : 1) * np.getDefWidth();
			height = (height<0 ? -1 : 1) * np.getDefHeight();
		} else {
			SizeOffset so = np.getSizeOffset();
			// Take the default width or height if that's what the user wants.
			// Otherwise adjust the user-specified width or height by the 
			// SizeOffset.
			double signW = width<0 ? -1 : 1;
			if (width==DEF_SIZE || width==-DEF_SIZE) {
				width = signW * np.getDefWidth();
			} else {
				double hi = so.getHighXOffset();
				double lo = so.getLowXOffset();
				error(lo!=hi, "asymmetric X offset");
				width = signW * (Math.abs(width) + hi+lo);
			}
			double signH = height<0 ? -1 : 1;
			if (height==DEF_SIZE || height==-DEF_SIZE) {
				height = signH * np.getDefHeight();
			} else {
				double hi = so.getHighYOffset();
				double lo = so.getLowYOffset();
				error(lo!=hi, "asymmetric X offset");
				height = signH * (Math.abs(height) + hi+lo);
			}
			
		}
		NodeInst ni = NodeInst.newInstance(np, new Point2D.Double(x, y),
										   width, height,
										   (int)Math.round(angle*10),
										   parent, null);
		error(ni==null, "newNodeInst failed");								
		return ni;
	}
	/**
	 * Create a new ArcInst.
	 *
	 * <p> The specified width will match that reported by the GUI.
	 * @param ap the ArcProto to instantiate.
	 * @param width the desired width of the ArcInst. Add the
	 * "width_offset" to <code>width</code> so we end up with an
	 * ArcInst that the GUI says is <code>width</code> wide.
	 * @param head the head PortInst.
	 * @param hX the X coordinate of the head PortInst.
	 * @param hY the Y coordinate of the head PortInst.
	 * @param tail the tail PortInst.
	 * @param tX the X coordinate of the tail PortInst.
	 * @param tY the Y coordinate of the tail PortInst.
	 * @return the new ArcInst.
	 */
	public static ArcInst newArcInst(ArcProto ap, double width,
							  		 PortInst head, double hX, double hY,
							  		 PortInst tail, double tX, double tY) {
		// Take the default width if that's what the user wants.
		// Otherwise adjust the user-specified width or height by the 
		// SizeOffset.
		if (width==DEF_SIZE) {
			width = ap.getDefaultWidth();
		} else {
			width += ap.getWidthOffset();
		} 
		ArcInst ai = ArcInst.newInstance(ap, width, 
		                                 head, new Point2D.Double(hX, hY),
										 tail, new Point2D.Double(tX, tY), 
										 null);
		error(ai==null, "newArcInst failed");
		return ai;
	}

	/**
	 * Create a new ArcInst.
	 *
	 * <p> The specified width will match that reported by the GUI.
	 *
	 * <p> Connect the new ArcInst to the centers of the PortInsts.
	 * If the centers don't share an X or Y coordinate then connect the head
	 * and the tail using two ArcInsts. The ArcInst attached to the head is
	 * horizontal and the ArcInst attached to the tail is vertical.
	 * @param ap the head PortInst.
	 * @param width the desired width of the ArcInst. Add the
	 * "width_offset" to <code>width</code> so we end up with an
	 * ArcInst that the GUI says is <code>width</code> wide.
	 * @param head the head ArcInst
	 * @param tail the tail ArcInst
	 * @return the ArcInst connected to the tail.
	 */
	public static ArcInst newArcInst(ArcProto ap, double width,
							         PortInst head, PortInst tail) {
		double hX = head.getBounds().getCenterX();
		double hY = head.getBounds().getCenterY();
		double tX = tail.getBounds().getCenterX();
		double tY = tail.getBounds().getCenterY();
		if (hX==tX || hY==tY) {
			// no jog necessary						         	
			return newArcInst(ap, width, head, hX, hY, tail, tX, tY);
		} else {
			Cell parent = head.getNodeInst().getParent();
			NodeProto pinProto = ((PrimitiveArc)ap).findPinProto();
			PortInst pin = newNodeInst(pinProto, DEF_SIZE, DEF_SIZE, tX, hY, 0, 
			                           parent).getOnlyPortInst(); 
			newArcInst(ap, width, head, pin);
			return newArcInst(ap, width, pin, tail);
		}
	}

	/**
	 *  Create an export for a particular layer.
	 *
	 * <p> At the coordinates <code>(x, y)</code> create an instance of
	 * a pin for the layer <code>ap</code>. Export that layer-pin's
	 * PortInst.
	 *
	 * <p> Attach an arc to the layer-pin.  The arc is useful because
	 * Electric uses the widest arc on a PortInst as a hint for the
	 * width to use for all future arcs. Because Electric doesn't use
	 * the size of layer-pins as width hints, the layer-pin is created
	 * in it's default size.
	 *
	 * <p> <code>newExport</code> seems very specialized, but it's
	 * nearly the only one I use when generating layout.
	 * @param cell the Cell to which to add the Export.
	 * @param name the name of the Export.
	 * @param role the Export's type.
	 * @param ap the ArcProto indicating the layer on which to create
	 * the Export.
	 * @param w width of the ArcInst serving as a hint.
	 * @param x the X coordinate of the layer pin. 
	 * @param y the Y coordinate of the layer pin.
	 */
	public static Export newExport(Cell cell, String name, 
	                               PortProto.Characteristic role,
	                               ArcProto ap, double w, double x, double y) {
		NodeProto np = ((PrimitiveArc)ap).findPinProto();
		error(np==null, "LayoutLib.newExport: This layer has no layer-pin");
		
		double defSz = LayoutLib.DEF_SIZE;
		NodeInst ni = LayoutLib.newNodeInst(np, defSz, defSz, x, y, 0, cell);
        LayoutLib.newArcInst(ap, w, ni.getOnlyPortInst(), ni.getOnlyPortInst());
		
		Export e = Export.newInstance(cell, ni.getOnlyPortInst(), name);
		e.setCharacteristic(role);
		return e;
	}
	
	/**
	 * Get the essential or regular bounds.
	 *
	 * <p>If NodeInst <code>node</code> has an Essential Bounds then
	 * return it. Otherwise return the regular bounds.
	 * @param node the NodeInst.
	 * @return the Rectangle2D representing the bounds.
	 */
	public static Rectangle2D getAnyBounds(NodeInst node) {
		Rectangle2D bounds = node.findEssentialBounds();
		if (bounds!=null) return bounds;
		return node.getBounds();
	}

	/**
	 * Move NodeInst so it's left edge is at <code>leftX</code> and
	 * it's reference point is at <code>refY</code>.
	 *
	 * <p> Don't alter the NodeInst's scale or rotation.
	 * @param node the NodeInst
	 * @param leftX desired x coordinate of left edge of <code>node</code>.
	 * @param refY desired y coordinate of <code>node</code>'s reference
	 * point.
	 */
	public static void abutLeft(NodeInst node, double leftX, double refY) {
		double cY = node.getCenterY();
		Rectangle2D bd = node.findEssentialBounds();
		error(bd==null,
			  "can't abut NodeInsts that don't have essential-bounds");
		node.modifyInstance(leftX-bd.getX(), refY-cY, 0, 0, 0);
	}

	/**
	 * Abut an array of NodeInsts left to right.
	 *
	 * <p> Move the 0th NodeInst so it's left edge is at
	 * <code>leftX</code> and it's reference point is at
	 * <code>refY</code>. Abut the remaining nodes left to right.
	 * Don't alter any NodeInst's scale or rotation.
	 * @param leftX desired x coordinate of left edge of 0th NodeInst.
	 * @param refY desired y coordinate of all NodeInst reference
	 * points.
	 * @param nodeInsts the ArrayList of NodeInsts.
	 */
	public static void abutLeftRight(double leftX, double refY,
									 ArrayList nodeInsts) {
		for (int i=0; i<nodeInsts.size(); i++) {
			NodeInst ni = (NodeInst) nodeInsts.get(i);
			if (i==0) {
				abutLeft(ni, leftX, refY);
			} else {
				abutLeftRight((NodeInst)nodeInsts.get(i-1), ni);
			}
		}
	}

	/**
	 * Abut two NodeInsts left to right.
	 *
	 * <p>Move <code>rightNode</code> so its left edge coincides with
	 * <code>leftNode</code>'s right edge, and
	 * <code>rightNode</code>'s reference point lies on a horizontal
	 * line through <code>leftNode</code>'s reference point. Don't
	 * move <code>leftNode</code>. Don't alter any node's scale or
	 * rotation.
	 * @param leftNode the NodeInst that doesn't move.
	 * @param rightNode the NodeInst that is moved to butt against
	 * leftNode.
	 */
	public static void abutLeftRight(NodeInst leftNode, NodeInst rightNode) {
		abutLeft(rightNode, leftNode.getBounds().getMaxX(),
				 leftNode.getCenterY());
	}

	/**
	 * Abut an array of NodeInsts left to right.
	 * 
	 * <p>Don't move the 0th node. Abut remaining nodes left to right.
	 * Don't alter any NodeInst's scale or rotation.
	 * @param nodeInsts the ArrayList of */
	public static void abutLeftRight(ArrayList nodeInsts) {
		for (int i=1; i<nodeInsts.size(); i++) {
			abutLeftRight((NodeInst)nodeInsts.get(i-1),
						  (NodeInst)nodeInsts.get(i));
		}
	}
	
	/** Move a NodeInst so it's bottom edge is at <code>botY</code>.
	 *
	 * <p>Place <code>node</code>'s reference point at
	 * <code>refX</code>. Don't alter <code>node</code>'s scale or
	 * rotation.
	 * @param node the NodeInst to move.
	 * @param refX desired x coordinate of NodeInst's reference point.
	 * @param botY desired y coordinate of bottom edge of NodeInst.
	 */
	public static void abutBottom(NodeInst node, double refX, double botY) {
		double cX = node.getCenterX();
		Rectangle2D eb = node.findEssentialBounds();
		error(eb==null,
			  "can't abut a NodeInst that doesn't have Essential Bounds");
		node.modifyInstance(refX-cX, botY-eb.getMinY(), 0, 0, 0);
	}

	/**
	 * Abut two NodeInsts bottom to top.
	 *
	 * <p>Move <code>topNode</code> so its bottom edge coincides with
	 * <code>bottomNode</code>'s top edge, and <code>topNode</code>'s
	 * reference point lies on a vertical line through
	 * <code>bottomNode</code>'s reference point. Don't move
	 * <code>bottomNode</code>.  Don't alter any node's scale or
	 * rotation. */
	public static void abutBottomTop(NodeInst bottomNode, NodeInst topNode) {
		abutBottom(topNode, bottomNode.getCenterX(),
				   bottomNode.getBounds().getMaxY());
	}

	/**
	 * Abut a list of NodeInsts bottom to top.
	 *
	 * <p>Move first NodeInst so it's bottom edge is at bottomY and
	 * it's reference point has the specified x coordinate. Abut
	 * remaining nodes bottom to top.  Don't alter any NodeInst's
	 * scale or rotation.
	 * @param x desired x coordinate of all NodeInst reference points.
	 * Lambda units.
	 * @param y desired y coordinate of bottom edge of first NodeInst.
	 * @param nodeInsts the list of NodeInsts to abut.
	 */
	public static void abutBottomTop(double x, double y, ArrayList nodeInsts) {
		for (int i=0; i<nodeInsts.size(); i++) {
			NodeInst ni = (NodeInst) nodeInsts.get(i);
			if (i==0) {
				abutBottom(ni, x, y);
			} else {
				abutBottomTop((NodeInst)nodeInsts.get(i-1), ni);
			}
		}
	}

	/**
	 * Abut a list of NodeInsts bottom to top.
	 *
	 * <p>Don't alter position of 0th node. Abut the remaining nodes
	 * bottom to top.  Don't alter any NodeInst's scale or rotation.
	 * @param nodeInsts the list of NodeInsts to abut.
	 */
	public static void abutBottomTop(ArrayList nodeInsts) {
		for (int i=1; i<nodeInsts.size(); i++) {
			abutBottomTop((NodeInst)nodeInsts.get(i-1),
						  (NodeInst)nodeInsts.get(i));
		}
	}
}
