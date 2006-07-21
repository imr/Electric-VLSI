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
package com.sun.electric.tool.generator.layout;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.dialogs.OpenFile;

/*
 * The LayoutLib class provides an assortment of methods that I
 * found to be useful for programatic layout generation.
 */
public class LayoutLib {
	// ---------------------------- public data ------------------------------
	/** Use the default size. When a width or height argument has this
	 * value the object should be created with its default
	 * dimension. Note that -DEF_SIZE is also a legal
	 * constant. Negative dimensions specify mirroring for certain
	 * methods. */
	public static final double DEF_SIZE = Double.POSITIVE_INFINITY;

	// ---------------------------- public methods ---------------------------
	/**
	 * Print a message, dump a stack trace, and throw a RuntimeException if 
	 * errorHasOccurred argument is true.
	 *
	 * @param errorHasOccurred indicates a runtime error has been detected
	 * @param msg the message to print when an error occurs
	 * @throws RuntimeException if errorHasOccurred is true
	 */
	public static void error(boolean errorHasOccurred, String msg) {
		if (!errorHasOccurred) return;
		RuntimeException e = new RuntimeException(msg);
		// The following prints a stack trace on the console
        ActivityLogger.logException(e);

		// The following prints a stack trace in the Electric messages window
		throw e;
	}

	/** 
	 * Open a library for reading. If a library is
	 * already open then return it. Otherwise look for the library
	 * file named libFileName and open that library.
	 * 
	 * @param libFileName the fully qualified path name of the Library
	 * file on disk
	 * @return the open Library or null if it can't be found
	 */
	public static Library openLibForRead(String libFileName) {
		URL libFileURL = TextUtils.makeURLToFile(libFileName);
		String libName = TextUtils.getFileNameWithoutExtension(libFileURL);
		Library lib = Library.findLibrary(libName);
		FileType type = OpenFile.getOpenFileType(libFileName, FileType.DEFAULTLIB);
		if (lib==null) {
			lib = LibraryFiles.readLibrary(libFileURL, null, type, false);
		}
		error(lib==null, "can't open Library for reading: "+libFileName);
		return lib;
	}
	/**
	 * Open a library for modification. If a library named libName is
	 * already open then return it. Otherwise look for the file named:
	 * libFileName and open that library. Finally, if all else fails
	 * create a new Library and return it.
	 *
	 * @param libName the name of the Library
	 * file on disk
	 * @return the desired library
	 */
	// This doesn't work anymore.
//	public static Library openLibForModify(String libName, String libFileName) {
//		// return an open Library if it exists
//		Library lib = Library.findLibrary(libName);
//		if (lib!=null)  return lib;
//
//		// open a Library file if it exists
//		URL libFileURL = TextUtils.makeURLToFile(libFileName);
//		lib = Input.readLibrary(libFileURL, OpenFile.Type.ELIB);
//		if (lib!=null)  return lib;
//		
//		// create a new Library
//		lib = Library.newInstance(libName, libFileURL);
//
//		error(lib==null, "can't open Library for modify: "+libName);
//		return lib;
//	}
	public static Library openLibForWrite(String libName) {
		// return an open Library if it exists
		Library lib = Library.findLibrary(libName);
		if (lib!=null)  return lib;

		// create a new Library
		lib = Library.newInstance(libName, null);

		error(lib==null, "can't open Library for modify: "+libName);
		return lib;
	}
	/**
	 * Write a library in ELIB format.
	 * @param lib the library to be written.
	 */
	public static void writeLibrary(Library lib) {
		Output.writeLibrary(lib, FileType.ELIB, false, false, false);
	}
	/** Get the width of an ArcInst. The getArcInstWidth method differs
	 * from ArcInst.getWidth() in that it subtracts off the "width
	 * offset". Hence, getArcInstWidth returns a width that matches
	 * that reported by the GUI.
	 *
 	 * @param ai the ArcInst whose width is reported
	 * @return the width of the ArcInst. */
	public static double getArcInstWidth(ArcInst ai) {
		double w = ai.getWidth() - ai.getProto().getWidthOffset();
		return DBMath.round(w);
	}
	/** Get the default width of a NodeProto. The getNodeProtoWidth
	 * method differs from NodeProto.getDefWidth in that it subtracts
	 * off the "width offset". Hence getNodeProtoWidth returns a width
	 * that matches that reported by the GUI.
	 *
	 * @param np the NodeProto we want the width of.
	 * @return the width of the NodeProto. */
	public static double getNodeProtoWidth(NodeProto np) {
		SizeOffset so = np.getProtoSizeOffset();
		double w = np.getDefWidth() - so.getLowXOffset() - so.getHighXOffset();
		return DBMath.round(w);
	}
	/** Get the default height of a NodeProto. The getNodeProtoHeight
	 * method differs from NodeProto.getDefHeight in that it subtracts
	 * off the "height offset". Hence getNodeProtoHeight returns a
	 * height that matches that reported by the GUI.
	 *
	 * @param np the NodeProto we want the height of
	 * @return the height of the NodeProto */
	public static double getNodeProtoHeight(NodeProto np) {
		SizeOffset so = np.getProtoSizeOffset();
		double h = np.getDefHeight() - so.getLowYOffset() - so.getHighYOffset();
		return DBMath.round(h);
	}
	/**
	 * Find the width of the widest wire connected hierarchically to port.
	 *
	 * @param port the PortInst to check for attached wires.
	 * @return the width of the widest wire. This width excludes the
	 * "width offset" so it matches the width reported by the GUI.
	 * If no wire is attached to port then return DEF_SIZE.
	 */
	public static double widestWireWidth(PortInst port) {
//		NodeInst ni = port.getNodeInst();
		PortProto pp = port.getPortProto();
		double maxWid = -1;
		for (Iterator<ArcInst> arcs=getArcInstsOnPortInst(port); arcs.hasNext();) {
			ArcInst ai = arcs.next();
			maxWid = Math.max(maxWid, getArcInstWidth(ai));
		}
		if (pp instanceof Export) {
			double check = widestWireWidth(((Export)pp).getOriginalPort());
			maxWid = Math.max(maxWid, check);
		}
		return DBMath.round(maxWid);
	}

	/** Return a list of ArcInsts attached to PortInst, pi.
	 * @param pi PortInst on which to find attached ArcInsts. */
	public static Iterator<ArcInst> getArcInstsOnPortInst(PortInst pi) {
		ArrayList<ArcInst> arcs = new ArrayList<ArcInst>();
//		NodeInst ni = pi.getNodeInst();
		for (Iterator<Connection> it=pi.getConnections(); it.hasNext();) {
			Connection c = it.next();
			arcs.add(c.getArc());
		}
//		for (Iterator it=ni.getConnections(); it.hasNext();) {
//			Connection c = it.next();
//			if (c.getPortInst()==pi)  arcs.add(c.getArc());
//		}
		return arcs.iterator();
	}
	/** The center returned by bounds might have a slight amount of rounding
	 * error. Compensate for this by always rounding coordinates to a 103-4
	 * lambda grid when reading and writing the database. */
	public static double roundCenterX(PortInst pi) {
        return pi.getCenter().getX();
//		return DBMath.round(pi.getBounds().getCenterX()); new way avoids rounding here
	}
	public static double roundCenterY(PortInst pi) {
        return pi.getCenter().getY();
//		return DBMath.round(pi.getBounds().getCenterY());
	}

    public static Rectangle2D calculateNodeInst(NodeProto np,
                                                double x, double y,
                                                double width, double height)
    {
		if (np instanceof Cell) {
			width = (width<0 ? -1 : 1) * np.getDefWidth();
			height = (height<0 ? -1 : 1) * np.getDefHeight();
		} else {
			SizeOffset so = np.getProtoSizeOffset();
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
				error(lo!=hi, "asymmetric Y offset");
				height = signH * (Math.abs(height) + hi+lo);
			}
		}
		// round all dimensions to a 10e-4 lambda grid
		x = DBMath.round(x);
		y = DBMath.round(y);
		width = DBMath.round(width);
		height = DBMath.round(height);
        return new Rectangle2D.Double(x, y, width, height);
    }

	/**
	 * Create a new NodeInst.  The following geometric transformations
	 * are performed upon the NodeProto in order to arrive at the
	 * final position of the NodeInst in the coordinate space of the
	 * parent:
	 * <ol>
	 * <li> Scale the NodeProto in x and y so that it has dimensions
	 * |width| and |height|. All scaling is performed about the
	 * NodeProto's origin, (0, 0).
	 * <li> If width<0 then mirror the preceding result about the
	 * y-axis.
	 * <li> If height<0 then mirror the preceding result about the
	 * x-axis.
	 * <li> Rotating the preceding result clockwise by angle degrees.
	 * <li> Translate the preceding result by (x, y). Note that the
	 * NodeProto's origin always ends up at (x, y) in the
	 * coordinate space of the parent.
	 * </ol> 
	 * 
	 * The newNodeInst method differs from NodeInst.newInstance in the
	 * following ways:
	 *
	 * <ul>
	 * <li>The "size offset" is added to the width and height
	 * arguments before the object is created. The result is a
	 * NodeInst that the GUI reports has dimensions: width x height.
	 * <li>The angle is in units of degrees but is rounded to the
	 * nearest tenth degree.
	 * <li>If np is a Cell then the width and height are taken from
	 * the Cell's defaults. The width and height arguments only
	 * specify mirroring.
	 * <li>If np is a Cell then rotation and mirroring are performed
	 * relative to the Cell's origin.
	 * <li>If np is a Cell then the NodeInst is positioned using the
	 * Cell's origin.  That is, the resulting NodeInst will map the
	 * Cell's origin to (x, y) in the coordinate space of the parent.
	 * <li>If the width or height arguments are equal to DEF_SIZE then
	 * the NodeInst is created using the NodeProto's default
	 * dimensions. Eventually I will change this to <i>minimum</i>
	 * dimensions.
	 * </ul>
	 * @param np the NodeProto to instantiate
	 * @param width the desired width of the NodeInst
	 * @param height the desired height of the NodeInst
	 * @param x the desired x-coordinate of the NodeProto's origin in
	 * the coordinate space of the parent. If x is negative then the
	 * NodeInst mirrors about the y-axis.
	 * @param y the desired y-coordinate of the NodeProto's origin in
	 * the coordinate space of the parent. If y is negative then the
	 * NodeInst mirrors about the x-axis.
	 * @param angle the angle, in degrees, of rotation about the
	 * NodeProto's origin.
	 * @param parent the Cell that will contain the NodeInst.
	 * @return the new NodeInst. 
	 */
    public static NodeInst newNodeInst(NodeProto np,
		                               double x, double y,
									   double width, double height,
			                           double angle, Cell parent) {
        Rectangle2D rect = calculateNodeInst(np, x, y, width, height);
        return newNodeInst(np, rect, angle, parent);
    }

    public static NodeInst newNodeInst(NodeProto np, Rectangle2D rect,
			                           double angle, Cell parent)
    {
        double x = rect.getX();
        double y = rect.getY();
        double width = rect.getWidth();
        double height = rect.getHeight();

        Orientation orient = Orientation.fromJava((int)Math.round(angle*10), width < 0, height < 0);
		NodeInst ni = NodeInst.newInstance(np, new Point2D.Double(x, y), Math.abs(width), Math.abs(height),
                parent, orient, null, 0);
		error(ni==null, "newNodeInst failed");								

		// adjust position so that translation is Cell-Center relative
		if (np instanceof Cell) {
			Point2D ref = getPosition(ni);
			ni.move(x-ref.getX(), y-ref.getY());
		}
		return ni;
	}
	/**
	 * Modify the position of a NodeInst.  The size and position of a
	 * NodeInst in the coordinate space of its parent cell is
	 * determined by 5 parameters: x, y, width, height, and angle, as
	 * described in the JavaDoc for newNodeInst. The modNodeInst
	 * method modifies those parameters.
	 * <p>The modNodeInst method differs from NodeInst.modifyInstance
	 * in the following ways:
	 * <ul>
	 * <li>If ni is an instance of a Cell then mirroring, rotation,
	 * and positioning are performed relative to the Cell's origin
	 * <li>The arguments dw and dh, are added to the absolute values
	 * of the NodeInst's x-size and y-size.
	 * <li>The arguments mirrorAboutXAxis and mirrorAboutYAxis are
	 * used to mirror the NodeInst about the x and y axes.
	 * </ul>
	 * @param ni the NodeInst to modify
	 * @param dx the amount by which to change the x-coordinate of the
	 * NodeInst's position
	 * @param dy the amount by which to change the y-coordinate of the
	 * NodeInst's position
	 * @param dw the amount by which to change to absolute value of
	 * the NodeInst's width
	 * @param dh the amount by which to change to absolute value of
	 * the NodeInst's height.
	 * @param mirrorAboutYAxis if true then toggle the mirroring of
	 * the NodeInst about the y-axis
	 * @param mirrorAboutXAxis if true then toggle the mirroring of
	 * the NodeInst about the x-axis
	 * @param dAngle the amount by which to change the NodeInst's angle
	 */
	public static void modNodeInst(NodeInst ni, double dx, double dy, 
	                               double dw, double dh, 
	                               boolean mirrorAboutYAxis, 
	                               boolean mirrorAboutXAxis,
								   double dAngle) {
// 	    boolean oldMirX = ni.isMirroredAboutXAxis();
// 	    boolean oldMirY = ni.isMirroredAboutYAxis();
// 		double oldXS = ni.getXSize() * (oldMirY ? -1 : 1);
// 		double oldYS = ni.getYSize() * (oldMirX ? -1 : 1);
		 
		double newX = getPosition(ni).getX() + dx;
		double newY = getPosition(ni).getY() + dy;

// 		double newW = Math.max(ni.getXSize() + dw, 0);
// 		double newH = Math.max(ni.getYSize() + dh, 0);
//		
// 		boolean newMirX = oldMirX ^ mirrorAboutXAxis;
// 		boolean newMirY = oldMirY ^ mirrorAboutYAxis;
//		
// 		double newXS = newW * (newMirY ? -1 : 1);
// 		double newYS = newH * (newMirX ? -1 : 1);
		Orientation dOrient = Orientation.fromJava((int)Math.rint(dAngle*10), mirrorAboutYAxis, mirrorAboutXAxis);
		ni.modifyInstance(0, 0, dw, dh, dOrient);
// 		ni.modifyInstance(0, 0, newXS-oldXS, newYS-oldYS, 
// 		                  (int)Math.rint(dAngle*10));
		ni.move(newX-getPosition(ni).getX(), newY-getPosition(ni).getY());
	}

	/**
	 * Get the position of a NodeInst. In the coordinate space of the
	 * NodeInst's parent, get the x and y-coordinates of the origin of
	 * the NodeInst's NodeProto.
	 * @param ni the NodeInst we want the position of
	 * @return the x and y-coordinates of the origin of the
	 * NodeInst's NodeProto
	 */
	public static Point2D getPosition(NodeInst ni) {
		NodeProto np = ni.getProto();
		Point2D p;
		if (np instanceof Cell) {
			AffineTransform xForm = ni.transformOut();
			p = xForm.transform(new Point2D.Double(0, 0), null);
		} else {
			p = ni.getAnchorCenter();
		}
		double x = DBMath.round(p.getX());
		double y = DBMath.round(p.getY());
		return new Point2D.Double(x,y);
	}
	/**
	 * Create a new ArcInst. This differs from ArcInst.newInstance in that
	 * the "width-offset" is added to the width parameter. The result is an 
	 * ArcInst that the GUI reports is width wide.
	 * @param ap the ArcProto to instantiate
	 * @param width the desired width of the ArcInst
	 * @param head the head PortInst
	 * @param hX the x-coordinate of the head PortInst
	 * @param hY the y-coordinate of the head PortInst
	 * @param tail the tail PortInst
	 * @param tX the x-coordinate of the tail PortInst
	 * @param tY the y-coordinate of the tail PortInst
	 * @return the new ArcInst
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
		
		hX = DBMath.round(hX);
		hY = DBMath.round(hY);
		tX = DBMath.round(tX);
		tY = DBMath.round(tY);
		width = DBMath.round(width);

		ArcInst ai = ArcInst.newInstance(ap, width, 
		                                 head, tail, new Point2D.Double(hX, hY),
										 new Point2D.Double(tX, tY),
										 null, 0);
        error(ai==null, "newArcInst failed");
		ai.setFixedAngle(true);
		return ai;
	}

	/**
	 * Create a new ArcInst. This differs from ArcInst.newInstance in that
	 * the "width-offset" is added to the width parameter. The result is an 
	 * ArcInst that the GUI reports is width wide.
	 *
	 * <p> Connect the new ArcInst to the centers of the PortInsts.
	 * If the centers don't share an X or y-coordinate then connect
	 * the head and the tail using two ArcInsts. The ArcInst attached
	 * to the head is horizontal and the ArcInst attached to the tail
	 * is vertical.
	 * @param ap the head PortInst
	 * @param width the desired width of the ArcInst
	 * @param head the head ArcInst
	 * @param tail the tail ArcInst
	 * @return the ArcInst connected to the tail.
	 */
	public static ArcInst newArcInst(ArcProto ap, double width,
							         PortInst head, PortInst tail) {
        EPoint headP = head.getCenter();
		double hX = headP.getX(); // roundCenterX(head);
		double hY = headP.getY(); // roundCenterY(head);
        EPoint tailP = tail.getCenter();
		double tX = tailP.getX(); // roundCenterX(tail);
		double tY = tailP.getY(); // roundCenterY(tail);
		ArcInst ai;
		if (hX==tX || hY==tY) {
			// no jog necessary						         	
			ai = newArcInst(ap, width, head, hX, hY, tail, tX, tY);
		} else {
			Cell parent = head.getNodeInst().getParent();
			NodeProto pinProto = ap.findOverridablePinProto();
			PortInst pin = newNodeInst(pinProto, tX, hY, DEF_SIZE, DEF_SIZE, 0, 
			                           parent).getOnlyPortInst(); 

			// debug
            EPoint pinP = pin.getCenter();
			double newX = pinP.getX(); // roundCenterX(pin);
			double newY = pinP.getY(); // roundCenterY(pin);
			
			if (newX!=tX || newY!=hY) {
				Rectangle2D r = head.getBounds();
				double loy = r.getMinY();
				double hiy = r.getMaxY();
				System.out.println(loy+" "+hiy);
				error(true, "center moved");
			}
			
			
			ai = newArcInst(ap, width, head, pin);
			ai.setFixedAngle(true);
			ai = newArcInst(ap, width, pin, tail);
		}
		ai.setFixedAngle(true);
		return ai;
	}

	/**
	 *  Create an export for a particular layer.
	 *
	 * <p> At the coordinates <code>(x, y)</code> create a NodeInst of
	 * a layer-pin for the layer <code>ap</code>. Export that
	 * layer-pin's PortInst.
	 *
	 * <p> Attach an ArcInst of ArcProto ap to the layer-pin.  The
	 * ArcInst is useful because Electric uses the widest ArcInst on a
	 * PortInst as a hint for the width to use for all future
	 * arcs. Because Electric doesn't use the size of layer-pins as
	 * width hints, the layer-pin is created in it's default size.
	 *
	 * <p> <code>newExport</code> seems very specialized, but it's
	 * nearly the only one I use when generating layout.
	 * @param cell the Cell to which to add the Export.
	 * @param name the name of the Export.
	 * @param role the Export's type.
	 * @param ap the ArcProto indicating the layer on which to create
	 * the Export.
	 * @param w width of the ArcInst serving as a hint.
	 * @param x the x-coordinate of the layer pin. 
	 * @param y the y-coordinate of the layer pin.
	 */
	public static Export newExport(Cell cell, String name, 
	                               PortCharacteristic role,
	                               ArcProto ap, double w, double x, double y) {
		NodeProto np = ap.findOverridablePinProto();
		error(np==null, "LayoutLib.newExport: This layer has no layer-pin");
		
		double defSz = LayoutLib.DEF_SIZE;
		NodeInst ni = LayoutLib.newNodeInst(np, x, y, defSz, defSz, 0, cell);
        LayoutLib.newArcInst(ap, w, ni.getOnlyPortInst(), ni.getOnlyPortInst());
		
		Export e = Export.newInstance(cell, ni.getOnlyPortInst(), name);
		e.setCharacteristic(role);
		return e;
	}
	public static Rectangle2D roundBounds(Rectangle2D r) {
		double w = DBMath.round(r.getWidth());
		double h = DBMath.round(r.getHeight());
		double x = DBMath.round(r.getX());
		double y = DBMath.round(r.getY());
		return new Rectangle2D.Double(x,y,w,h);
	}
	/**
	 * Get the essential or regular bounds.  If NodeInst
	 * <code>node</code> has an Essential Bounds then return
	 * it. Otherwise return the regular bounds.
	 * @param node the NodeInst.
	 * @return the Rectangle2D representing the bounds.
	 */
	public static Rectangle2D getBounds(NodeInst node) {
		Rectangle2D bounds = node.findEssentialBounds();
		if (bounds==null) bounds = node.getBounds();
		return roundBounds(bounds);
	}
	/**
	 * Get the essential or regular bounds.  If Cell c has an
	 * Essential Bounds then return it. Otherwise return the regular
	 * bounds.
	 * @param c the Cell.
	 * @return the Rectangle2D representing the bounds.
	 */
	public static Rectangle2D getBounds(Cell c) {
		Rectangle2D bounds = c.findEssentialBounds();
		if (bounds==null) bounds = c.getBounds();
		return roundBounds(bounds);
	}

	// --------------------- Abutment methods ---------------------------------
	// There are too many abutment methods. I need to figure out how
	// to eliminate some.

	/**
	 * Move NodeInst so it's left edge is at <code>leftX</code> and
	 * the y-coordinate of it's origin is at
	 * <code>originY</code>. Don't alter the NodeInst's scale or
	 * rotation.
	 * @param node the NodeInst
	 * @param leftX desired x-coordinate of left edge of <code>node</code>.
	 * @param originY desired y-coordinate of <code>node</code>'s origin
	 */
	public static void abutLeft(NodeInst node, double leftX, double originY) {
		double cY = getPosition(node).getY();
		Rectangle2D bd = node.findEssentialBounds();
		error(bd==null,
			  "can't abut NodeInsts that don't have essential-bounds");
		LayoutLib.modNodeInst(node, leftX-bd.getX(), originY-cY, 0, 0, false, 
						      false, 0);
	}

	/**
	 * Abut an array of NodeInsts left to right. Move the 0th NodeInst
	 * so it's left edge is at <code>leftX</code> and it the
	 * y-coordinate of its origin is at <code>originY</code>. Abut the
	 * remaining nodes left to right.  Don't alter any NodeInst's
	 * scale or rotation.
	 * @param leftX desired x-coordinate of left edge of 0th NodeInst.
	 * @param originY desired y-coordinate of all NodeInst origins
	 * @param nodeInsts the ArrayList of NodeInsts.
	 */
	public static void abutLeftRight(double leftX, double originY,
									 ArrayList<NodeInst> nodeInsts) {
		for (int i=0; i<nodeInsts.size(); i++) {
			NodeInst ni = nodeInsts.get(i);
			if (i==0) {
				abutLeft(ni, leftX, originY);
			} else {
				abutLeftRight(nodeInsts.get(i-1), ni);
			}
		}
	}

	/**
	 * Abut two NodeInsts left to right.  Move <code>rightNode</code>
	 * so its left edge coincides with <code>leftNode</code>'s right
	 * edge, and the y-coordinate of <code>rightNode</code>'s is equal
	 * to the y-coordinate of <code>leftNode</code>'s origin. Don't
	 * move <code>leftNode</code>. Don't alter any node's scale or
	 * rotation.
	 * @param leftNode the NodeInst that doesn't move.
	 * @param rightNode the NodeInst that is moved to butt against
	 * leftNode.
	 */
	public static void abutLeftRight(NodeInst leftNode, NodeInst rightNode) {
		abutLeft(rightNode, getBounds(leftNode).getMaxX(),
				 getPosition(leftNode).getY());
	}

	/**
	 * Abut an array of NodeInsts left to right. Don't move the 0th
	 * node. Abut remaining nodes left to right.  Don't alter any
	 * NodeInst's scale or rotation.
	 * @param nodeInsts the ArrayList of NodeInsts */
	public static void abutLeftRight(ArrayList<NodeInst> nodeInsts) {
		for (int i=1; i<nodeInsts.size(); i++) {
			abutLeftRight(nodeInsts.get(i-1), nodeInsts.get(i));
		}
	}
	
	/** Move a NodeInst so it's bottom edge is at <code>botY</code>.
	 *
	 * <p>Place <code>node</code>'s origin at
	 * <code>originX</code>. Don't alter <code>node</code>'s scale or
	 * rotation.
	 * @param node the NodeInst to move
	 * @param originX desired x-coordinate of NodeInst's origin
	 * @param botY desired y-coordinate of bottom edge of NodeInst
	 */
	public static void abutBottom(NodeInst node, double originX, double botY) {
		double cX = getPosition(node).getX();
		Rectangle2D eb = node.findEssentialBounds();
		error(eb==null,
			  "can't abut a NodeInst that doesn't have Essential Bounds");
		LayoutLib.modNodeInst(node, originX-cX, botY-eb.getMinY(), 0, 0, 
		                      false, false, 0);
	}

	/**
	 * Abut two NodeInsts bottom to top. Move <code>topNode</code> so
	 * its bottom edge coincides with <code>bottomNode</code>'s top
	 * edge, and the y-coordinate of <code>topNode</code>'s origin is
	 * equal to the y-coorinate of <code>bottomNode</code>'s
	 * origin. Don't move <code>bottomNode</code>.  Don't alter any
	 * node's scale or rotation. */
	public static void abutBottomTop(NodeInst bottomNode, NodeInst topNode) {
		abutBottom(topNode, getPosition(bottomNode).getX(),
				   getBounds(bottomNode).getMaxY());
	}

	/**
	 * Abut a list of NodeInsts bottom to top. Move 0th NodeInst so
	 * it's bottom edge is at botY and it's origin has the
	 * x-coordinate, originX. Abut remaining nodes bottom to top.
	 * Don't alter any NodeInst's scale or rotation.
	 * @param originX desired x-coordinate of all NodeInst reference points.
	 * Lambda units.
	 * @param botY desired y-coordinate of bottom edge of first NodeInst.
	 * @param nodeInsts the list of NodeInsts to abut.
	 */
	public static void abutBottomTop(double originX, double botY,
									 ArrayList<NodeInst> nodeInsts) {
		for (int i=0; i<nodeInsts.size(); i++) {
			NodeInst ni = nodeInsts.get(i);
			if (i==0) {
				abutBottom(ni, originX, botY);
			} else {
				abutBottomTop(nodeInsts.get(i-1), ni);
			}
		}
	}

	/**
	 * Abut a list of NodeInsts bottom to top.  Don't alter position
	 * of 0th node. Abut the remaining nodes bottom to top.  Don't
	 * alter any NodeInst's scale or rotation.
	 * @param nodeInsts the list of NodeInsts to abut.
	 */
	public static void abutBottomTop(ArrayList<NodeInst> nodeInsts) {
		for (int i=1; i<nodeInsts.size(); i++) {
			abutBottomTop(nodeInsts.get(i-1), nodeInsts.get(i));
		}
	}

    /**
     * Get the bounding box for the layer in the Cell.
     * Note this does not include geometry from sub-cells.
     * @param cell the cell to examine
     * @param function the layer's function
     * @return a bounding box around all instances of the layer in the cell
     */
    public static Rectangle2D getBounds(Cell cell, Layer.Function function) {
        Rectangle2D bounds = null;
        Technology tech = cell.getTechnology();
        List<Layer.Function> list = new ArrayList<Layer.Function>(1);
        list.add(function); // to only query this layer

        // get layer from nodes
        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            AffineTransform trans = ni.rotateOut();
            Poly [] polys = tech.getShapeOfNode(ni, null, null, false, true, list);
            if (polys == null) continue;
            for (int i=0; i<polys.length; i++) {
                if (polys[i] == null) continue;
                if (polys[i].getLayer().getFunction() == function)
                {
                    polys[i].transform(trans);
                    if (bounds == null)
                        bounds = polys[i].getBox();
                    else
                        bounds = bounds.createUnion(polys[i].getBox());
                }
            }
        }
        // get layer from arcs
        for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); ) {
            ArcInst ai = it.next();
            Poly [] polys = tech.getShapeOfArc(ai, null, null, list);
            if (polys == null) continue;
            for (int i=0; i<polys.length; i++) {
                if (polys[i] == null) continue;
                if (polys[i].getLayer().getFunction() == function) {
                    if (bounds == null)
                        bounds = polys[i].getBox();
                    else
                        bounds = bounds.createUnion(polys[i].getBox());
                }
            }
        }
        return bounds;
    }
}
