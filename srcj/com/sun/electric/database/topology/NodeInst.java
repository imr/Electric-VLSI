/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NodeInst.java
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
package com.sun.electric.database.topology;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.constraint.Constraint;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A NodeInst is an instance of a NodeProto (a PrimitiveNode or a Cell).
 * A NodeInst points to its prototype and the Cell in which it has been
 * instantiated.  It also has a name, and contains a list of Connections
 * and Exports.
 * <P>
 * The rotation and transposition of a NodeInst can be confusing, so it is illustrated here.
 * Nodes are transposed when one of their scale factors is negative.
 * <P>
 * <CENTER><IMG SRC="doc-files/NodeInst-1.gif"></CENTER>
 */
public class NodeInst extends Geometric
{
	// -------------------------- constants --------------------------------
//	/** node is not in use */								private static final int DEADN =                     01;
//	/** node has text that is far away */					private static final int NHASFARTEXT =               02;
	/** if on, draw node expanded */						private static final int NEXPAND =                   04;
	/** set if node not drawn due to wiping arcs */			private static final int WIPED =                    010;
	/** set if node is to be drawn shortened */				private static final int NSHORT =                   020;
	//  used by database:                                                                                      0140
//	/** if on, this nodeinst is marked for death */			private static final int KILLN =                   0200;
//	/** nodeinst re-drawing is scheduled */					private static final int REWANTN =                 0400;
//	/** only local nodeinst re-drawing desired */			private static final int RELOCLN =                01000;
//	/** transparent nodeinst re-draw is done */				private static final int RETDONN =                02000;
//	/** opaque nodeinst re-draw is done */					private static final int REODONN =                04000;
//	/** general flag used in spreading and highlighting */	private static final int NODEFLAGBIT =           010000;
//	/** if on, nodeinst wants to be (un)expanded */			private static final int WANTEXP =               020000;
//	/** temporary flag for nodeinst display */				private static final int TEMPFLG =               040000;
	/** set if hard to select */							private static final int HARDSELECTN =          0100000;
	/** set if node only visible inside cell */				private static final int NVISIBLEINSIDE =     040000000;
	/** technology-specific bits for primitives */			private static final int NTECHBITS =          037400000;
	/** right-shift of NTECHBITS */							private static final int NTECHBITSSH =               17;
	/** set if node is locked (can't be changed) */			private static final int NILOCKED =          0100000000;

	// Name of Variable holding instance name.
	private static final String VAR_INST_NAME = "NODE_name";

	// ---------------------- private data ----------------------------------
	/** prototype of this node instance */					private NodeProto protoType;
	/** node usage of this node instance */					private NodeUsage nodeUsage;
	/** labling information for this node instance */		private int textbits;
	/** HashTable of portInsts on this node instance */		private HashMap portMap;
	/** List of connections belonging to this instance */	private List connections;
	/** List of Exports belonging to this node instance */	private List exports;
	/** Text descriptor */									private TextDescriptor descriptor;

	// --------------------- private and protected methods ---------------------

	/**
	 * The constructor is never called.  Use the factory "newInstance" instead.
	 */
	private NodeInst()
	{
		// initialize this object
		this.textbits = 0;
		this.portMap = new HashMap();
		this.connections = new ArrayList();
		this.exports = new ArrayList();
		this.descriptor = TextDescriptor.newInstanceDescriptor();
	}

	/**
	 * Routine to recalculate the Geometric bounds for this NodeInst.
	 */
	void updateGeometric()
	{
		if (sX == 0 && sY == 0)
		{
			visBounds.setRect(cX, cY, 0, 0);
		} else
		{
			// special case for arcs of circles
			if (protoType == Artwork.tech.circleNode || protoType == Artwork.tech.thickCircleNode)
			{
				/* see if there this circle is only a partial one */
				double [] angles = getArcDegrees();
				if (angles[0] != 0.0 || angles[1] != 0.0)
				{
					Point2D.Double [] pointList = Artwork.fillEllipse(cX, cY, Math.abs(sX), Math.abs(sY), angles[0], angles[1]);
					Poly poly = new Poly(pointList);
					poly.setStyle(Poly.Type.OPENED);
					poly.transform(rotateOut());
					visBounds.setRect(poly.getBounds2DDouble());
					return;
				}
			}

			// special case for pins that become steiner points
			if (protoType.isWipeOn1or2() && getNumExports() == 0)
			{
				if (pinUseCount())
				{
					visBounds.setRect(cX, cY, 0, 0);
					return;
				}
			}

			/* special case for polygonally-defined nodes: compute precise geometry */
			if (protoType.isHoldsOutline())
			{
				Float [] outline = getTrace();
				if (outline != null)
				{
					int numPoints = outline.length / 2;
					Point2D.Double [] pointList = new Point2D.Double[numPoints];
					for(int i=0; i<numPoints; i++)
					{
						pointList[i] = new Point2D.Double(cX + outline[i*2].floatValue(), cY + outline[i*2+1].floatValue());
					}
					Poly poly = new Poly(pointList);
					poly.setStyle(Poly.Type.OPENED);
					poly.transform(rotateOut());
					visBounds.setRect(poly.getBounds2DDouble());
					return;
				}
			}

			// start with a unit polygon, centered at the origin
			Poly poly = new Poly(0.0, 0.0, 1.0, 1.0);

			// transform by the relevant amount
			AffineTransform scale = new AffineTransform();
			scale.setToScale(sX, sY);
			AffineTransform rotate = rotateAbout(angle, 0, 0, sX, sY);
			AffineTransform translate = new AffineTransform();
			translate.setToTranslation(cX, cY);
			rotate.concatenate(scale);
			translate.concatenate(rotate);
			poly.transform(translate);

			// return its bounds
			visBounds.setRect(poly.getBounds2DDouble());
		}
	}

	/**
	 * Low-level access routine to create a NodeInst.
	 * @return the newly created NodeInst.
	 */
	public static NodeInst lowLevelAllocate()
	{
		NodeInst ni = new NodeInst();
		ni.parent = null;
		return ni;
	}

	/**
	 * Low-level routine to fill-in the NodeInst information.
	 * @param protoType the NodeProto of which this is an instance.
	 * @param center the center location of this NodeInst.
	 * @param width the width of this NodeInst.
	 * If negative, flip the X coordinate (or flip ABOUT the Y axis).
	 * @param height the height of this NodeInst.
	 * If negative, flip the Y coordinate (or flip ABOUT the X axis).
	 * @param angle the angle of this NodeInst (in tenth-degrees).
	 * @param parent the Cell in which this NodeInst will reside.
	 * @return true on error.
	 */
	public boolean lowLevelPopulate(NodeProto protoType, Point2D.Double center, double width, double height, int angle,
		Cell parent)
	{
		setParent(parent);
		this.protoType = protoType;

		// create all of the portInsts on this node inst
		for (Iterator it = protoType.getPorts(); it.hasNext();)
		{
			PortProto pp = (PortProto) it.next();
			PortInst pi = PortInst.newInstance(pp, this);
			portMap.put(pp.getProtoName(), pi);
		}

		// enumerate the port instances
		int i = 0;
		for(Iterator it = getPortInsts(); it.hasNext();)
		{
			PortInst pi = (PortInst) it.next();
			pi.setIndex(i++);
		}

		this.cX = center.x;   this.cY = center.y;
		this.sX = width;   this.sY = height;
		this.angle = angle;

		// fill in the geometry
		updateGeometric();
		return false;
	}

	/**
	 * Low-level access routine to link the NodeInst into its Cell.
	 * @return true on error.
	 */
	public boolean lowLevelLink()
	{
		// add to linked lists
		linkGeom(parent);
		nodeUsage = parent.addNode(this);
		return false;
	}

	/**
	 * Low-level routine to unlink the NodeInst from its Cell.
	 */
	public void lowLevelUnlink()
	{
		// remove this node from the cell
		unLinkGeom(parent);
		parent.removeNode(this);
		nodeUsage = null;
	}

	/**
	 * Routine to create a NodeInst.
	 * @param protoType the NodeProto of which this is an instance.
	 * @param center the center location of this NodeInst.
	 * @param width the width of this NodeInst.
	 * If negative, flip the X coordinate (or flip ABOUT the Y axis).
	 * @param height the height of this NodeInst.
	 * If negative, flip the Y coordinate (or flip ABOUT the X axis).
	 * @param angle the angle of this NodeInst (in tenth-degrees).
	 * @param parent the Cell in which this NodeInst will reside.
	 * @return the newly created NodeInst, or null on error.
	 */
	public static NodeInst newInstance(NodeProto protoType, Point2D.Double center, double width, double height,
		int angle, Cell parent)
	{
		if (protoType instanceof Cell)
		{
			if (((Cell)protoType).isAChildOf(parent))
			{
				System.out.println("Cannot create instance of " + protoType.describe() + " in cell " + parent.describe() +
					" because it is recursive");
				return null;
			}
		}
		NodeInst ni = lowLevelAllocate();
		if (ni.lowLevelPopulate(protoType, center, width, height, angle, parent)) return null;
		if (ni.lowLevelLink()) return null;

		// handle change control, constraint, and broadcast
		if (Undo.recordChange())
		{
			// tell all tools about this NodeInst
			Undo.Change ch = Undo.newChange(ni, Undo.Type.NODEINSTNEW);

			// tell constraint system about new NodeInst
			Constraint.getCurrent().newObject(ni);
		}
		Undo.clearNextChangeQuiet();
		return ni;
	}

	/**
	 * Routine to delete this NodeInst.
	 */
	public void kill()
	{
		// kill the arcs attached to the connections.  This will also remove the connections themselves
		while (connections.size() > 0)
		{
			Connection con = (Connection)connections.get(connections.size() - 1);
			con.getArc().startChange();
			con.getArc().kill();
		}

		// remove any exports
		while (exports.size() > 0)
		{
			Export pp = (Export)exports.get(exports.size() - 1);
			pp.startChange();
			pp.kill();
		}

		// remove the node
		lowLevelUnlink();

		// handle change control, constraint, and broadcast
		if (Undo.recordChange())
		{
			// tell all tools about this NodeInst
			Undo.Change ch = Undo.newChange(this, Undo.Type.NODEINSTKILL);

			// tell constraint system about killed NodeInst
			Constraint.getCurrent().killObject(this);
		}
	}

	/**
	 * Routine to change this NodeInst.
	 * @param dX the amount to move the NodeInst in X.
	 * @param dY the amount to move the NodeInst in Y.
	 * @param dXSize the amount to scale the NodeInst in X.
	 * @param dYSize the amount to scale the NodeInst in Y.
	 * @param dRot the amount to alter the NodeInst rotation (in tenths of a degree).
	 */
	public void modifyInstance(double dX, double dY, double dXSize, double dYSize, int dRot)
	{
		// make sure the change values are sensible
		dRot = dRot % 3600;
		if (dRot < 0) dRot += 3600;
		if (dX == 0 && dY == 0 && dXSize == 0 && dYSize == 0 && dRot == 0)
		{
			return;
		}

		// make the change
		if (Undo.recordChange())
		{
			Constraint.getCurrent().modifyNodeInst(this, dX, dY, dXSize, dYSize, dRot);
		} else
		{
			adjustInstance(dX, dY, dXSize, dYSize, dRot);

			// change the coordinates of every arc end connected to this
			for(Iterator it = getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				if (con.getPortInst().getNodeInst() == this)
				{
					Point2D.Double oldLocation = con.getLocation();
					if (con.isHeadEnd()) con.getArc().modify(0, dX, dY, 0, 0); else
						con.getArc().modify(0, 0, 0, dX, dY);
				}
			}
			parent.setDirty();
		}

		// if the cell-center changed, notify the cell and fix lots of stuff
		if (protoType instanceof PrimitiveNode && protoType == Generic.tech.cellCenter_node)
		{
			parent.adjustReferencePoint(this);
		}
	}

	/**
	 * Routine to change many NodeInsts.
	 * @param nis the NodeInsts to change.
	 * @param dXs the amount to move the NodeInsts in X.
	 * @param dYs the amount to move the NodeInsts in Y.
	 * @param dXSizes the amount to scale the NodeInsts in X.
	 * @param dYSizes the amount to scale the NodeInsts in Y.
	 * @param dRots the amount to alter the NodeInst rotation (in tenths of a degree).
	 */
	public static void modifyInstances(NodeInst [] nis, double [] dXs, double [] dYs, double [] dXSizes, double [] dYSizes, int [] dRots)
	{
		for(int i=0; i<nis.length; i++)
		{
			nis[i].modifyInstance(dXs[i], dYs[i], dXSizes[i], dYSizes[i], dRots[i]);
		}
	}

	/**
	 * Routine to adjust this NodeInst by the specified deltas.
	 * This routine does not go through change control, and so should not be used unless you know what you are doing.
	 * @param dX the change to the center X coordinate.
	 * @param dY the change to the center Y coordinate.
	 * @param dXSize the change to the X size.
	 * @param dYSize the change to the Y size.
	 * @param dRot the change to the rotation (in tenths of a degree).
	 */
	public void adjustInstance(double dX, double dY, double dXSize, double dYSize, int dRot)
	{
		// remove from the R-Tree structure
		unLinkGeom(parent);

		// make the change
		this.cX = EMath.smooth(this.cX + dX);
		this.cY = EMath.smooth(this.cY + dY);

		this.sX = EMath.smooth(this.sX + dXSize);
		this.sY = EMath.smooth(this.sY + dYSize);

		this.angle += dRot;

		// fill in the Geometric fields
		updateGeometric();

		// link back into the R-Tree
		linkGeom(parent);
	}

	/**
	 * Routine to tell whether this NodeInst is an icon of its parent.
	 * Electric does not allow recursive circuit hierarchies (instances of Cells inside of themselves).
	 * However, it does allow one exception: a schematic may contain its own icon for documentation purposes.
	 * This routine determines whether this NodeInst is such an icon.
	 * @return true if this NodeInst is an icon of its parent.
	 */
	public boolean isIconOfParent()
	{
		NodeProto np = getProto();
		if (!(np instanceof Cell))
			return false;

		return getParent().getCellGroup() == ((Cell) np).getCellGroup();
	}

	/*
	 * Routine to write a description of this NodeInst.
	 * Displays the description in the Messages Window.
	 */
	public void getInfo()
	{
		System.out.println("-------------- NODE INSTANCE " + describe() + ": --------------");
		String xMir = "";
		String yMir = "";
		if (isXMirrored()) xMir = ", MirrorX";
		if (isYMirrored()) yMir = ", MirrorY";
		System.out.println(" Center: (" + cX + "," + cY + "), size: " + getXSize() + "x" + getYSize() + ", rotated " + angle/10.0 +
			xMir + yMir);
		super.getInfo();
		System.out.println(" Ports:");
		for(Iterator it = getPortInsts(); it.hasNext();)
		{
			PortInst pi = (PortInst) it.next();
			Poly p = pi.getPoly();
			System.out.println("     " + pi.getPortProto().getProtoName() + " at (" + p.getCenterX() + "," + p.getCenterY() + ")");
		}
		if (connections.size() != 0)
		{
			System.out.println(" Connections:");
			for (int i = 0; i < connections.size(); i++)
			{
				Connection c = (Connection) connections.get(i);
				System.out.println("     " + c.getArc().describe() +
					" on port " + c.getPortInst().getPortProto().getProtoName());
			}
		}
		if (exports.size() != 0)
		{
			System.out.println(" Exports:");
			for (int i = 0; i < exports.size(); i++)
			{
				Export ex = (Export) exports.get(i);
				System.out.println("     " + ex.getProtoName() +
					" on port " + ex.getOriginalPort().getPortProto().getProtoName() +
					", " + ex.getCharacteristic());
			}
		}
	}

	/**
	 * sanity check function, used by Connection.checkobj
	 */
	boolean containsConnection(Connection c)
	{
		return connections.contains(c);
	}

	// Move exports from oldInst to newInst
	private void moveExports(NodeInst oldInst, NodeInst newInst)
	{
		Cell parent = oldInst.getParent();
		ArrayList toMove = new ArrayList();

		for (Iterator it = parent.getPorts(); it.hasNext();)
		{
			Export e = (Export) it.next();
			if (e.getOriginalPort().getNodeInst() == oldInst)
				toMove.add(e);
		}

		// move these exports to new instance
		for (int i = 0; i < toMove.size(); i++)
		{
			Export e = (Export) toMove.get(i);

			String expNm = e.getProtoName();
			String portNm = e.getOriginalPort().getPortProto().getProtoName();
//			e.delete();
			PortInst newPort = newInst.findPortInst(portNm);
			if (newPort == null) System.out.println("NodeInst.moveExports: can't find port: " + portNm + " on new inst");
			Export.newInstance(parent, newPort, expNm);
		}
	}

	/**
	 * Routine to recomputes the "Wiped" flag bit on this NodeInst.
	 * The Wiped flag is set if the NodeInst is "wipable" and if it is connected to
	 * ArcInsts that wipe.  Wiping means erasing.  Typically, pin NodeInsts can be wiped.
	 * This means that when an arc connects to the pin, it is no longer drawn.
	 */
	public void computeWipeState()
	{
		clearWiped();
		if (getProto().isArcsWipe())
		{
			for(Iterator it = getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				ArcInst ai = con.getArc();
				if (ai.getProto().isWipable())
				{
					setWiped();
					break;
				}
			}
		}
	}

	/**
	 * Routine to return the starting and ending angle of an arc described by this NodeInst.
	 * These values can be found in the "ART_degrees" variable on the NodeInst.
	 * @return a 2-long double array with the starting offset in the first entry (a value in radians)
	 * and the amount of curvature in the second entry (in radians).
	 * If the NodeInst does not have circular information, both values are set to zero.
	 */
	public double [] getArcDegrees()
	{
		double [] returnValues = new double[2];
		returnValues[0] = returnValues[1] = 0.0;

		if (!(protoType instanceof PrimitiveNode)) return returnValues;
		if (protoType != Artwork.tech.circleNode && protoType != Artwork.tech.thickCircleNode) return returnValues;

		Variable var = getVar("ART_degrees");
		if (var != null)
		{
			Object addr = var.getObject();
			if (addr instanceof Integer)
			{
				Integer iAddr = (Integer)addr;
				returnValues[0] = 0.0;
				returnValues[1] = (double)iAddr.intValue() * Math.PI / 1800.0;
			} else if (addr instanceof Float[])
			{
				Float [] fAddr = (Float [])addr;
				returnValues[0] = fAddr[0].doubleValue();
				returnValues[1] = fAddr[1].doubleValue();
			}
		}
		return returnValues;
	}

	// ------------------------ public methods -------------------------------

	/**
	 * Routine to set this NodeInst to be expanded.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * Unexpanded Cell instances are shown as boxes with the node prototype names in them.
	 * The state has no meaning for instances of primitive node prototypes.
	 */
	public void setExpanded() { userBits |= NEXPAND; }

	/**
	 * Routine to set this NodeInst to be unexpanded.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * Unexpanded Cell instances are shown as boxes with the node prototype names in them.
	 * The state has no meaning for instances of primitive node prototypes.
	 */
	public void clearExpanded() { userBits &= ~NEXPAND; }

	/**
	 * Routine to tell whether this NodeInst is expanded.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * Unexpanded Cell instances are shown as boxes with the node prototype names in them.
	 * The state has no meaning for instances of primitive node prototypes.
	 * @return true if this NodeInst is expanded.
	 */
	public boolean isExpanded() { return (userBits & NEXPAND) != 0; }

	/**
	 * Routine to set this NodeInst to be wiped.
	 * Wiped NodeInsts are erased.  Typically, pin NodeInsts can be wiped.
	 * This means that when an arc connects to the pin, it is no longer drawn.
	 * In order for a NodeInst to be wiped, its prototype must have the "setArcsWipe" state,
	 * and the arcs connected to it must have "setWipable" in their prototype.
	 * @see NodeProto#setArcsWipe
	 * @see ArcProto#setWipable
	 */
	public void setWiped() { userBits |= WIPED; }

	/**
	 * Routine to set this NodeInst to be not wiped.
	 * Wiped NodeInsts are erased.  Typically, pin NodeInsts can be wiped.
	 * This means that when an arc connects to the pin, it is no longer drawn.
	 * In order for a NodeInst to be wiped, its prototype must have the "setArcsWipe" state,
	 * and the arcs connected to it must have "setWipable" in their prototype.
	 * @see NodeProto#setArcsWipe
	 * @see ArcProto#setWipable
	 */
	public void clearWiped() { userBits &= ~WIPED; }

	/**
	 * Routine to tell whether this NodeInst is wiped.
	 * Wiped NodeInsts are erased.  Typically, pin NodeInsts can be wiped.
	 * This means that when an arc connects to the pin, it is no longer drawn.
	 * In order for a NodeInst to be wiped, its prototype must have the "setArcsWipe" state,
	 * and the arcs connected to it must have "setWipable" in their prototype.
	 * @return true if this NodeInst is wiped.
	 * @see NodeProto#setArcsWipe
	 * @see ArcProto#setWipable
	 */
	public boolean isWiped() { return (userBits & WIPED) != 0; }

	/**
	 * Routine to set this NodeInst to be shortened.
	 * Shortened NodeInst have been reduced in size to account for the fact that
	 * they are connected at nonManhattan angles and must connect smoothly.
	 * This state can only get set if the node's prototype has the "setCanShrink" state.
	 */
	public void setShortened() { userBits |= NSHORT; }

	/**
	 * Routine to set this NodeInst to be not shortened.
	 * Shortened NodeInst have been reduced in size to account for the fact that
	 * they are connected at nonManhattan angles and must connect smoothly.
	 * This state can only get set if the node's prototype has the "setCanShrink" state.
	 */
	public void clearShortened() { userBits &= ~NSHORT; }

	/**
	 * Routine to tell whether this NodeInst is shortened.
	 * Shortened NodeInst have been reduced in size to account for the fact that
	 * they are connected at nonManhattan angles and must connect smoothly.
	 * This state can only get set if the node's prototype has the "setCanShrink" state.
	 * @return true if this NodeInst is shortened.
	 */
	public boolean isShortened() { return (userBits & NSHORT) != 0; }

	/**
	 * Routine to set this NodeInst to be hard-to-select.
	 * Hard-to-select NodeInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 */
	public void setHardSelect() { userBits |= HARDSELECTN; }

	/**
	 * Routine to set this NodeInst to be easy-to-select.
	 * Hard-to-select NodeInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 */
	public void clearHardSelect() { userBits &= ~HARDSELECTN; }

	/**
	 * Routine to tell whether this NodeInst is hard-to-select.
	 * Hard-to-select NodeInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 * @return true if this NodeInst is hard-to-select.
	 */
	public boolean isHardSelect() { return (userBits & HARDSELECTN) != 0; }

	/**
	 * Routine to set this NodeInst to be visible-inside.
	 * A NodeInst that is "visible inside" is only drawn when viewing inside of the Cell.
	 * It is not visible from outside (meaning from higher-up the hierarchy).
	 */
	public void setVisInside() { userBits |= NVISIBLEINSIDE; }

	/**
	 * Routine to set this NodeInst to be not visible-inside.
	 * A NodeInst that is "visible inside" is only drawn when viewing inside of the Cell.
	 * It is not visible from outside (meaning from higher-up the hierarchy).
	 */
	public void clearVisInside() { userBits &= ~NVISIBLEINSIDE; }

	/**
	 * Routine to tell whether this NodeInst is visible-inside.
	 * A NodeInst that is "visible inside" is only drawn when viewing inside of the Cell.
	 * It is not visible from outside (meaning from higher-up the hierarchy).
	 * @return true if this NodeInst is visible-inside.
	 */
	public boolean isVisInside() { return (userBits & NVISIBLEINSIDE) != 0; }

	/**
	 * Routine to set this NodeInst to be locked.
	 * Locked NodeInsts cannot be modified or deleted.
	 */
	public void setLocked() { userBits |= NILOCKED; }

	/**
	 * Routine to set this NodeInst to be unlocked.
	 * Locked NodeInsts cannot be modified or deleted.
	 */
	public void clearLocked() { userBits &= ~NILOCKED; }

	/**
	 * Routine to tell whether this NodeInst is locked.
	 * Locked NodeInsts cannot be modified or deleted.
	 * @return true if this NodeInst is locked.
	 */
	public boolean isLocked() { return (userBits & NILOCKED) != 0; }

	/**
	 * Routine to set a Technology-specific value on this NodeInst.
	 * This is mostly used by the Schematics technology which allows variations
	 * on a NodeInst to be stored.
	 * For example, the Transistor primitive uses these bits to distinguish nMOS, pMOS, etc.
	 * @param value the Technology-specific value to store on this NodeInst.
	 */
	public void setTechSpecific(int value) { userBits = (userBits & ~NTECHBITS) | (value << NTECHBITSSH); }

	/**
	 * Routine to return the Technology-specific value on this NodeInst.
	 * This is mostly used by the Schematics technology which allows variations
	 * on a NodeInst to be stored.
	 * For example, the Transistor primitive uses these bits to distinguish nMOS, pMOS, etc.
	 * @return the Technology-specific value on this NodeInst.
	 */
	public int getTechSpecific() { return (userBits & NTECHBITS) >> NTECHBITSSH; }

	/**
	 * Routine to tell whether this NodeInst is mirrored in the X axis.
	 * Mirroring in the X axis implies that X coordinates are negated.
	 * Thus, it is equivalent to mirroring ABOUT the Y axis.
	 * @return true if this NodeInst is mirrored in the X axis.
	 */
	public boolean isXMirrored() { return sX < 0; }

	/**
	 * Routine to tell whether this NodeInst is mirrored in the Y axis.
	 * Mirroring in the Y axis implies that Y coordinates are negated.
	 * Thus, it is equivalent to mirroring ABOUT the X axis.
	 * @return true if this NodeInst is mirrored in the Y axis.
	 */
	public boolean isYMirrored() { return sY < 0; }

	/**
	 * Routine to return a list of Polys that describes all text on this NodeInst.
	 * @param hardToSelect is true if considering hard-to-select text.
	 * @param wnd the window in which the text will be drawn.
	 * @return an array of Polys that describes the text.
	 */
	public Poly [] getAllText(boolean hardToSelect, EditWindow wnd)
	{
		int nodeNameText = 0;
		if (hardToSelect && protoType instanceof Cell && !isExpanded()) nodeNameText = 1;
		int dispVars = numDisplayableVariables(false);
		int numExports = getNumExports();
		int numExportVariables = 0;
		for(Iterator it = getExports(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			numExportVariables += pp.numDisplayableVariables(false);
		}
		int totalText = nodeNameText + dispVars + numExports + numExportVariables;
		if (totalText == 0) return null;
		Poly [] polys = new Poly[totalText];
		int start = 0;

		// add in the cell name if appropriate
		if (nodeNameText != 0)
		{
			double cX = getCenterX();
			double cY = getCenterY();
			TextDescriptor td = getTextDescriptor();
			double offX = (double)td.getXOff() / 4;
			double offY = (double)td.getYOff() / 4;
			TextDescriptor.Position pos = td.getPos();
			Poly.Type style = pos.getPolyType();
			Point2D.Double [] pointList = new Point2D.Double[1];
			pointList[0] = new Point2D.Double(cX+offX, cY+offY);
			polys[start] = new Poly(pointList);
			polys[start].setStyle(style);
			polys[start].setString(getProto().describe());
			polys[start].setTextDescriptor(td);
			start++;
		}

		// add in the exports
		for(Iterator it = getExports(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			polys[start] = pp.getNamePoly();
			start++;

			// add in variables on the exports
			Poly poly = getShapeOfPort(pp.getOriginalPort().getPortProto());
			int numadded = pp.addDisplayableVariables(poly.getBounds2DDouble(), polys, start, wnd, false);
			for(int i=0; i<numadded; i++)
				polys[start+i].setPort(pp);
			start += numadded;
		}

		// add in the displayable variables
		addDisplayableVariables(getBounds(), polys, start, wnd, false);
		return polys;
	}

	/**
	 * Routine to return a transformation that moves up the hierarchy.
	 * Presuming that this NodeInst is a Cell instance, the transformation goes
	 * from the space of that Cell to the space of this NodeInst's parent Cell.
	 * The transformation includes the rotation of this NodeInst.
	 * @return a transformation that moves up the hierarchy.
	 */
	public AffineTransform transformOut()
	{
		// to transform out of this node instance, first translate inner coordinates to outer
		Cell lowerCell = (Cell)protoType;
		Rectangle2D bounds = lowerCell.getBounds();
		double dx = getCenterX() - bounds.getCenterX();
		double dy = getCenterY() - bounds.getCenterY();
		AffineTransform transform = new AffineTransform();
		transform.setToRotation(angle, cX, cY);
		transform.translate(dx, dy);
		return transform;
	}

	/**
	 * RKao: temporary Hack to help me debug my HierarchyEnumerator.
	 * Note: This is the C Electric positioning system.
	 * 1) Translate and scale to occupy bounding box of width: sX,
	 *    and height: sY centered at (cX, cY).
	 * 2) Rotate about (cX, cY) by angle radians
	 * 3) If sX<0 xor sY<0 then transpose. This means reflect about
	 *    a line of slope -1 passing through (cX, cY). Note that 
	 *    transpose is equivalent to rotating by 90 degrees followed
	 *    by mirroring about the X axis.  
	 */
	public AffineTransform rkTransformOut()
	{
		Cell lowerCell = (Cell)protoType;
		Rectangle2D bounds = lowerCell.getBounds();
		double dx = cX - bounds.getCenterX();
		double dy = cY - bounds.getCenterY();
		AffineTransform transform = new AffineTransform();
		boolean transpose = sX<0 ^ sY<0;
		if (transpose) {
			transform.translate(cX, cY);
			transform.scale(1, -1);
			transform.rotate(Math.PI/2);
			transform.translate(-cX, -cY);
		}
		transform.rotate(angle*Math.PI/1800, cX, cY);
		transform.translate(dx, dy);
		System.out.println("rkTransformOut: {\n"
		                   + "    lowerCellBounds: " + bounds + "\n"
						   + "    (cX, cY): " + cX + " " + cY + "\n"
						   + "    angle: " + (angle/10.0) + "\n"
						   + "    (sX, sY): " + sX + " " + sY + "\n"
						   + "    xform: " + transform + "\n"
						   + "}\n");
		return transform;
	}

	/**
	 * Routine to return a transformation that moves up the hierarchy, combined with a previous transformation.
	 * Presuming that this NodeInst is a Cell instance, the transformation goes
	 * from the space of that Cell to the space of this NodeInst's parent Cell.
	 * The transformation includes the rotation of this NodeInst.
	 * @param prevTransform the previous transformation to the NodeInst's Cell.
	 * @return a transformation that moves up the hierarchy, including the previous transformation.
	 */
	public AffineTransform transformOut(AffineTransform prevTransform)
	{
		AffineTransform transform = transformOut();
		AffineTransform returnTransform = new AffineTransform(prevTransform);
		returnTransform.concatenate(transform);
		return returnTransform;
	}

	/**
	 * Routine to return a transformation that translates up the hierarchy.
	 * Presuming that this NodeInst is a Cell instance, the transformation goes
	 * from the space of that Cell to the space of this NodeInst's parent Cell.
	 * However, it does not account for the rotation of this NodeInst...it only
	 * translates from one space to another.
	 * @return a transformation that translates up the hierarchy.
	 */
	public AffineTransform translateOut()
	{
		// to transform out of this node instance, first translate inner coordinates to outer
		Cell lowerCell = (Cell)protoType;
		Rectangle2D bounds = lowerCell.getBounds();
		double dx = getCenterX() - bounds.getCenterX();
		double dy = getCenterY() - bounds.getCenterY();
		AffineTransform transform = new AffineTransform();
		transform.translate(dx, dy);
		return transform;
	}

	/**
	 * Routine to return a transformation that translates up the hierarchy, combined with a previous transformation.
	 * Presuming that this NodeInst is a Cell instance, the transformation goes
	 * from the space of that Cell to the space of this NodeInst's parent Cell.
	 * However, it does not account for the rotation of this NodeInst...it only
	 * translates from one space to another.
	 * @param prevTransform the previous transformation to the NodeInst's Cell.
	 * @return a transformation that translates up the hierarchy, including the previous transformation.
	 */
	public AffineTransform translateOut(AffineTransform prevTransform)
	{
		AffineTransform transform = translateOut();
		AffineTransform returnTransform = new AffineTransform(prevTransform);
		returnTransform.concatenate(transform);
		return returnTransform;
	}

	private static AffineTransform rotateTranspose = new AffineTransform();
	private static AffineTransform mirrorX = new AffineTransform(-1, 0, 0, 1, 0, 0);
	private static AffineTransform mirrorY = new AffineTransform(1, 0, 0, -1, 0, 0);

	/**
	 * Routine to return a transformation that rotates an object about a point.
	 * @param angle the amount to rotate (in tenth-degrees).
	 * @param cX the center X coordinate about which to rotate.
	 * @param cY the center Y coordinate about which to rotate.
	 * @param sX the scale in X (negative to flip the X coordinate, or flip ABOUT the Y axis).
	 * @param sY the scale in Y (negative to flip the Y coordinate, or flip ABOUT the X axis).
	 * @return a transformation that rotates about that point.
	 */
	public static AffineTransform rotateAbout(int angle, double cX, double cY, double sX, double sY)
	{
		AffineTransform transform = new AffineTransform();
		if (sX < 0 || sY < 0)
		{
			// must do mirroring, so it is trickier
			rotateTranspose.setToRotation(angle * Math.PI / 1800.0);
			transform.setToTranslation(cX, cY);
			if (sX < 0) transform.concatenate(mirrorX);
			if (sY < 0) transform.concatenate(mirrorY);
			transform.concatenate(rotateTranspose);
			transform.translate(-cX, -cY);
		} else
		{
			transform.setToRotation(angle * Math.PI / 1800.0, cX, cY);
		}
		return transform;
	}

	/**
	 * Routine to return a transformation that rotates an object.
	 * @param angle the amount to rotate (in tenth-degrees).
	 * @param sX the scale in X (negative to flip the X coordinate, or flip ABOUT the Y axis).
	 * @param sY the scale in Y (negative to flip the Y coordinate, or flip ABOUT the X axis).
	 * @return a transformation that rotates by this amount.
	 */
	public static AffineTransform pureRotate(int angle, double sX, double sY)
	{
		AffineTransform transform = new AffineTransform();
		transform.setToRotation(angle * Math.PI / 1800.0);

		// add mirroring
		if (sX < 0) transform.concatenate(mirrorX);
		if (sY < 0) transform.concatenate(mirrorY);
		return transform;
	}

	/**
	 * Routine to return a transformation that rotates this NodeInst.
	 * It transforms points on this NodeInst to account for the NodeInst's rotation.
	 * @return a transformation that rotates this NodeInst.
	 * If this NodeInst is not rotated, the returned transformation is identity.
	 */
	public AffineTransform rotateOut()
	{
		return rotateAbout(angle, cX, cY, sX, sY);
	}

	/**
	 * Routine to return a transformation that rotates this NodeInst, combined with a previous transformation.
	 * It transforms points on this NodeInst to account for the NodeInst's rotation.
	 * @param prevTransform the previous transformation to be applied.
	 * @return a transformation that rotates this NodeInst, combined with a previous transformation..
	 * If this NodeInst is not rotated, the returned transformation is identity.
	 */
	public AffineTransform rotateOut(AffineTransform prevTransform)
	{
		// if there is no transformation, stop now
		if (angle == 0 && sX >= 0 && sY >= 0) return prevTransform;

		AffineTransform transform = rotateOut();
		AffineTransform returnTransform = new AffineTransform(prevTransform);
		returnTransform.concatenate(transform);
		return returnTransform;
	}

	/**
	 * Routine to return the lower-left corner of this NodeInst.
	 * The corner considers both the highlight offset and the rotation, so the
	 * coordinate is that of the lower-left valid part, as placed in the database.
	 * @return a Point with the location of this NodeInst's lower-left corner.
	 */
	public Point2D.Double getLowLeft()
	{
		SizeOffset so = getProto().getSizeOffset();

		double width = getXSize() / 2;
		double height = getYSize() / 2;
		double lX = cX - width + so.getLowXOffset();
		double hX = cX + width - so.getHighXOffset();
		double lY = cY - height + so.getLowYOffset();
		double hY = cY + height - so.getHighYOffset();
		Poly poly = new Poly((lX+hX)/2, (lY+hY)/2, hX-lX, hY-lY);
		if (getAngle() != 0 || isXMirrored() || isYMirrored())
		{
			AffineTransform trans = this.rotateOut();
			poly.transform(trans);
		}
		Rectangle2D.Double bounds = poly.getBounds2DDouble();
		return new Point2D.Double(bounds.getMinX(), bounds.getMinY());
	}

	/**
	 * Routine to return an Iterator for all PortInsts on this NodeInst.
	 * @return an Iterator for all PortInsts on this NodeInst.
	 */
	public Iterator getPortInsts()
	{
		return portMap.values().iterator();
	}

	/**
	 * Routine to return the number of PortInsts on this NodeInst.
	 * @return the number of PortInsts on this NodeInst.
	 */
	public int getNumPortInsts()
	{
		return portMap.size();
	}

	/**
	 * Routine to return the only PortInst on this NodeInst.
	 * This is quite useful for vias and pins which have only one PortInst.
	 * @return the only PortInst on this NodeInst.
	 * If there are more than 1 PortInst, then return null.
	 */
	public PortInst getOnlyPortInst()
	{
		int sz = portMap.size();
		if (sz != 1)
		{
			System.out.println("NodeInst.getOnlyPort: there isn't exactly one port: " + sz);
			return null;
		}
		return (PortInst) portMap.values().iterator().next();
	}

	/**
	 * Routine to return the named PortInst on this NodeInst.
	 * @param name the name of the PortInst.
	 * @return the selected PortInst.  If the name is not found, return null.
	 */
	public PortInst findPortInst(String name)
	{
		return (PortInst) portMap.get(name);
	}

	/**
	 * Routine to return the Portinst on this NodeInst with a given prototype.
	 * @param pp the PortProto to find.
	 * @return the selected PortInst.  If the PortProto is not found, return null.
	 */
	public PortInst getPortInstFromProto(PortProto pp)
	{
		for(Iterator it = portMap.values().iterator(); it.hasNext(); )
		{
			PortInst pi = (PortInst)it.next();
			if (pi.getPortProto() == pp) return pi;
		}
		return null;
	}

	/**
	 * Routine to determine whether this is an Invisible Pin with text.
	 * If so, it should not be selected, but its text should be instead.
	 * @return true if this is an Invisible Pin with text.
	 */
	public boolean isInvisiblePinWithText()
	{
		if (getProto() != Generic.tech.invisiblePin_node) return false;
		if (getNumExports() != 0) return true;
		if (numDisplayableVariables(false) != 0) return true;
		return false;
	}

	/**
	 * Routine to return a Poly that describes the location of a port on this NodeInst.
	 * @param thePort the port on this NodeInst.
	 * @return a Poly that describes the location of the Export.
	 * The Poly is not transformed for any rotation on this NodeInst.
	 */
	public Poly getShapeOfPort(PortProto thePort)
	{
		NodeInst ni = this;
		PortProto pp = thePort;

		/* look down to the bottom level node/port */
		AffineTransform trans = new AffineTransform();
		while (ni.getProto() instanceof Cell)
		{
			trans = ni.translateOut(trans);
			ni = ((Export)pp).getOriginalPort().getNodeInst();
			pp = ((Export)pp).getOriginalPort().getPortProto();
			trans = ni.rotateOut(trans);
		}

		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		Technology tech = np.getTechnology();
		Poly poly = tech.getShapeOfPort(ni, (PrimitivePort)pp);
		poly.transform(trans);
		return poly;
	}

	/**
	 * Routine to return the "outline" information on this NodeInst.
	 * Outline information is a set of coordinate points that further refines the NodeInst description.
	 * It is typically used in Artwork primitives to give them a precise shape.
	 * It is also used by pure-layer nodes in all layout technologies to allow them to take any shape.
	 * It is even used by many MOS transistors to allow a precise gate path to be specified.
	 * @return an array of Floats, organized as X/Y/X/Y.
	 */
	public Float [] getTrace()
	{
		Variable var = this.getVar("trace", Float[].class);
		if (var == null) return null;
		Object obj = var.getObject();
		if (obj instanceof Object[]) return (Float []) obj;
		return null;
	}

	/**
	 * Routine to determine whether the display of this pin NodeInst should be supressed.
	 * In Schematics technologies, pins are not displayed if there are 1 or 2 connections,
	 * but are shown for 0 or 3 or more connections (called "Steiner points").
	 * @return true if this pin NodeInst should be supressed.
	 */
	public boolean pinUseCount()
	{
		if (connections.size() > 2) return false;
		if (exports.size() != 0) return true;
		if (connections.size() == 0) return false;
		return true;
	}

	/**
	 * Routine to tell whether this NodeInst can connect to a given ArcProto.
	 * @param arc the type of arc to test for.
	 * @return the first port that can connect to this node, or
	 * null, if no such port on this node exists.
	 */
	public PortProto connectsTo(ArcProto arc)
	{
		return protoType.connectsTo(arc);
	}

	/**
	 * Routine to add an Connection to this NodeInst.
	 * @param c the Connection to add.
	 */
	void addConnection(Connection c)
	{
		connections.add(c);
		NodeInst ni = c.getPortInst().getNodeInst();
		ni.computeWipeState();
		updateGeometric();
	}

	/**
	 * Routine to remove an Connection from this NodeInst.
	 * @param c the Connection to remove.
	 */
	void removeConnection(Connection c)
	{
		connections.remove(c);
		NodeInst ni = c.getPortInst().getNodeInst();
		ni.computeWipeState();
		updateGeometric();
	}

	/**
	 * Routine to add an Export to this NodeInst.
	 * @param e the Export to add.
	 */
	public void addExport(Export e)
	{
		exports.add(e);
		updateGeometric();
	}

	/**
	 * Routine to remove an Export from this NodeInst.
	 * @param e the Export to remove.
	 */
	public void removeExport(Export e)
	{
		if (!exports.contains(e))
		{
			throw new RuntimeException("Tried to remove a non-existant export");
		}
		exports.remove(e);
		updateGeometric();
	}

	/**
	 * Routine to return an Iterator over all Exports on this NodeInst.
	 * @return an Iterator over all Exports on this NodeInst.
	 */
	public Iterator getExports()
	{
		return exports.iterator();
	}

	/**
	 * Routine to return the number of Exports on this NodeInst.
	 * @return the number of Exports on this NodeInst.
	 */
	public int getNumExports() { return exports.size(); }

	/**
	 * Routine to return an Iterator over all Connections on this NodeInst.
	 * @return an Iterator over all Connections on this NodeInst.
	 */
	public Iterator getConnections()
	{
		return connections.iterator();
	}

	/**
	 * Routine to return the number of Connections on this NodeInst.
	 * @return the number of Connections on this NodeInst.
	 */
	public int getNumConnections() { return connections.size(); }

	/**
	 * Routine to return the prototype of this NodeInst.
	 * @return the prototype of this NodeInst.
	 */
	public NodeProto getProto() { return protoType; }

    /** 
     * Routine to get the Schematic Cell from a NodeInst icon
     * @return the equivalent view of the prototype, or null if none
     * (such as for primitive)
     */
    public Cell getProtoEquivalent()
    {
        if (!(protoType instanceof Cell)) return null;            // primitive
        return ((Cell)protoType).getEquivalent();
    }
    
	/**
	 * Routine to return the NodeUsage of this NodeInst.
	 * @return the NodeUsage of this NodeInst.
	 */
	public NodeUsage getNodeUsage() { return nodeUsage; }

	/**
	 * Routine to return the Text Descriptor associated with this NodeInst.
	 * The only NodeInsts that need Text Descriptors are instances of Cells that are unexpanded.
	 * In this situation, the Cell instance is drawn as a box with a name.
	 * The Text Descriptor applies to the display of that name.
	 * @return the Text Descriptor for this NodeInst.
	 */
	public TextDescriptor getTextDescriptor() { return descriptor; }

	/**
	 * Routine to set the Text Descriptor associated with this NodeInst.
	 * The only NodeInsts that need Text Descriptors are instances of Cells that are unexpanded.
	 * In this situation, the Cell instance is drawn as a box with a name.
	 * The Text Descriptor applies to the display of that name.
	 * @param descriptor the Text Descriptor for this NodeInst.
	 */
	public void setTextDescriptor(TextDescriptor descriptor) { this.descriptor = descriptor; }

	/**
	 * Routine to return the name of this NodeInst.
	 * The name is a local string that can be set by the user.
	 * @return the name of this NodeInst, null if there is no name.
	 */
	public String getName()
	{
		Variable var = getVar(VAR_INST_NAME, String.class);
		if (var == null) return null;
		return (String) var.getObject();
	}

	/**
	 * Routine to set the name of this NodeInst.
	 * The name is a local string that can be set by the user.
	 * @param name the new name of this NodeInst.
	 */
	public Variable setName(String name)
	{
		Variable var = setVal(VAR_INST_NAME, name);
		if (var != null) var.setDisplay();
		return var;
	}

	/**
	 * Routine to describe this NodeInst as a string.
	 * @return a description of this NodeInst as a string.
	 */
	public String describe()
	{
		String description = protoType.describe();
		String name = getName();
		if (name != null) description += "[" + name + "]";
		return description;
	}

	/**
	 * Returns a printable version of this NodeInst.
	 * @return a printable version of this NodeInst.
	 */
	public String toString()
	{
		return "NodeInst " + protoType.getProtoName();
	}

	// Orientation of a line
//	private static final int HORIZONTAL = 0;
//	private static final int VERTICAL = 1;
//	private static final int POINT = 2;
//	private static final int DIAGONAL = 3;

	// Really rough implementation that works for automatically
	// generated layout.  Assumes that we always connect to center of
	// Ports. Deletes connections to oldInst.
//	private void moveConnections(NodeInst oldInst, NodeInst newInst)
//	{
//		for (Iterator pit = oldInst.getPorts(); pit.hasNext();)
//		{
//			PortInst oldP = (PortInst) pit.next();
//			String portNm = oldP.getName();
//			PortInst newP = newInst.findPort(portNm);
//			error(newP == null, "new NodeProto missing port: " + portNm);
//			for (Iterator ait = oldP.getArcs(); ait.hasNext();)
//			{
//				ArcInst oldA = (ArcInst) ait.next();
//				PortInst otherP = oldA.getConnection(false).getPortInst();
//				if (otherP == oldP)
//				{
//					otherP = oldA.getConnection(true).getPortInst();
//				}
//				double w = oldA.getWidth();
//				String nm = oldA.getName();
//				ArcProto ap = oldA.getProto();
//
//				int portOrient = portOrientation(oldP, newP);
//				int arcOrient = arcOrientation(oldA);
//				oldA.delete(); // I might want to move otherP.
//
//				ArcInst newA;
//				if (portOrient == POINT)
//				{
//					// old and new ports are in the same position.
//					newA = ap.newInst(w, newP, otherP);
//				} else if (arcOrient == DIAGONAL)
//				{
//					// diagonal arc? preserve connectivity
//					newA = ap.newInst(w, newP, otherP);
//				} else if (portOrient == arcOrient)
//				{
//					// port moves horizontally and wire is horizontal or
//					// port moves vertically and wire is vertical
//					newA = ap.newInst(w, newP, otherP);
//				} else if (arcOrient == VERTICAL)
//				{
//					// move other port horizontally to line up with new port X
//					NodeInst otherInst = otherP.getNodeInst();
//					otherInst.alterShapeRel(
//						1,
//						1,
//						newP.getCenterX() - otherP.getCenterX(),
//						0,
//						0);
//					newA = ap.newInst(w, newP, otherP);
//				} else if (arcOrient == HORIZONTAL)
//				{
//					// move other port vertically to line up with new port Y
//					NodeInst otherInst = otherP.getNodeInst();
//					otherInst.alterShapeRel(
//						1,
//						1,
//						0,
//						newP.getCenterY() - otherP.getCenterY(),
//						0);
//					newA = ap.newInst(w, newP, otherP);
//				} else
//				{
//					// if all else fails, preserve connectivity
//					newA = ap.newInst(w, newP, otherP);
//				}
//
//				if (nm != null)
//					newA.setName(nm);
//			}
//		}
//	}

//	// return orientation of line through two points
//	private int orientation(double x0, double y0, double x1, double y1)
//	{
//		if (x0 == x1 && y0 == y1)
//			return POINT;
//		if (x0 != x1 && y0 != y1)
//			return DIAGONAL;
//		if (x0 == x1)
//			return VERTICAL;
//		if (y0 == y1)
//			return HORIZONTAL;
//		error(true, "should have enumerated all cases");
//		return -1;
//	}

//	private int portOrientation(PortInst p0, PortInst p1)
//	{
//		return orientation(
//			p0.getCenterX(),
//			p0.getCenterY(),
//			p1.getCenterX(),
//			p1.getCenterY());
//	}

//	private int arcOrientation(ArcInst ai)
//	{
//		Point2D.Double p0 = ai.getConnection(false).getLocation();
//		Point2D.Double p1 = ai.getConnection(true).getLocation();
//		return orientation(p0.getX(), p0.getY(), p1.getX(), p1.getY());
//	}

	/** Change the size, angle, and position of this NodeInst.<br> See
	 * <code>NodeProto.newInst()</code> for details. */
//	public void alterShape(
//		double scaleX,
//		double scaleY,
//		double x,
//		double y,
//		double angle)
//	{
//		NodeProto p = protoType;
////		if (p instanceof Cell)
////			 ((Cell) p).updateBounds();
//
//		Electric.modifyNodeInst(this.getAddr(), ep.lx, ep.ly, ep.hx, ep.hy, ep.angle, ep.transpose);
//	}

	/** Make incremental changes to the size, angle, and position of
	 * this NodeInst.  For example: alterShapeRel(1,1,0,0,0) changes
	 * nothing.
	 * @param dScaleX multiply x scale by dScaleX
	 * @param dScaleY multiple y scale by dScaleY
	 * @param dX add dX to current x position.
	 * @param dY add dY to current y position.
	 * @param dAngle add dAngle to current angle. Degree units. */
//	public void alterShapeRel(
//		double dScaleX,
//		double dScaleY,
//		double dX,
//		double dY,
//		double dAngle)
//	{
//		double x = getReferencePoint().getX();
//		double y = getReferencePoint().getY();
//		double a = getAngle();
//		double sx = getScaleX();
//		double sy = getScaleY();
//		alterShape(sx * dScaleX, sy * dScaleY, x + dX, y + dY, a + dAngle);
//	}

	/** Change the size, angle, and position of this NodeInst.<br> See
	 * <code>NodeProto.newInstWH()</code> for details. */
//	public void alterShapeWH(
//		double width,
//		double height,
//		double x,
//		double y,
//		double angle)
//	{
//		NodeProto p = protoType;
//		error(
//			p instanceof Cell,
//			"alterShapeWH only handles primitiveNodes. "
//				+ "use alterShape instead");
//		int w = lambdaToBase(width), h = lambdaToBase(height);
//
//		NodeProto.ElectricPosition ep =
//			p.joseToElecPosition(
//				p.hideInvisWidToScale(w),
//				p.hideInvisHeiToScale(h),
//				lambdaToBase(x),
//				lambdaToBase(y),
//				angle,
//				getParent());
////		Electric.modifyNodeInst(this.getAddr(), ep.lx, ep.ly, ep.hx, ep.hy, ep.angle, ep.transpose);
//	}

	/** Replace this NodeInst with a new NodeInst built from
	 * newProto. Copy all of this NodeInst's connections to 
	 * the new NodeInst. Finally, delete this NodeInst.
	 *
	 * <p> Warning: this is a really rough implementation specialized
	 * for layout. It uses heuristics to accomodate changes in the
	 * positions of ports between the old and new NodeProtos that occur
	 * during layout resizing.
	 * @param newProto the new NodeProto to use to build the new
	 * NodeInst
	 * @returns the new NodeInst */
//	public NodeInst replaceProto(NodeProto newProto)
//	{
//		NodeInst newInst =
//			newProto.newInst(
//				getScaleX(),
//				getScaleY(),
//				getReferencePoint().getX(),
//				getReferencePoint().getY(),
//				getAngle(),
//				getParent());
//		moveConnections(this, newInst);
//		moveExports(this, newInst);
//		String nm = getName();
//		delete();
//		if (nm != null)
//			newInst.setName(nm);
//		return newInst;
//	}

	/** Translate NodeInst so it's left edge is at x and it's
	 * reference point has the specified y coordinate. Don't alter the
	 * NodeInst's scale or rotation.
	 * @param x desired x coordinate of left edge of NodeInst.
	 * Lambda units.
	 * @param y desired y coordinate of NodeInst's reference point.
	 * Lambda units. */
//	public void abutLeft(double x, double y)
//	{
//		Point2D rp = getReferencePoint();
//		Rectangle2D bd = getBounds();
//		alterShape(sX, sY, x - bd.getX() + rp.getX(), y, angle);
//	}

	/** Translate first NodeInst so it's left edge is at x and it's
	 * reference point has the specified y coordinate. Abut remaining
	 * nodes left to right.  Don't alter any NodeInst's scale or
	 * rotation.
	 * @param x desired x coordinate of left edge of first NodeInst.
	 * Lambda units.
	 * @param y desired y coordinate of all NodeInst reference points.
	 * Lambda units. */
//	public static void abutLeftRight(double x, double y, ArrayList nodeInsts)
//	{
//		for (int i = 0; i < nodeInsts.size(); i++)
//		{
//			NodeInst ni = (NodeInst) nodeInsts.get(i);
//			if (i == 0)
//			{
//				ni.abutLeft(x, y);
//			} else
//			{
//				abutLeftRight((NodeInst) nodeInsts.get(i - 1), ni);
//			}
//		}
//	}

	/** Translate rightNode so rightNode's left edge coincides with
	 * leftNode's right edge, and rightNode's reference point lies on a
	 * horizontal line through leftNode's reference point. leftNode
	 * isn't moved. Don't alter any node's scale or rotation. */
//	public static void abutLeftRight(NodeInst leftNode, NodeInst rightNode)
//	{
//		rightNode.abutLeft(
//			leftNode.getBounds().getMaxX(),
//			leftNode.getReferencePoint().getY());
//	}

	/** Don't alter position of first node. Abut remaining nodes left to
	 * right.  Don't alter any NodeInst's scale or rotation. */
//	public static void abutLeftRight(ArrayList nodeInsts)
//	{
//		for (int i = 1; i < nodeInsts.size(); i++)
//		{
//			abutLeftRight(
//				(NodeInst) nodeInsts.get(i - 1),
//				(NodeInst) nodeInsts.get(i));
//		}
//	}

	/** Translate NodeInst so it's bottom edge is at bottomY and it's
	 * reference point has the specified x coordinate. Don't alter the
	 * NodeInst's scale or rotation.
	 * @param x desired x coordinate of NodeInst's reference point.
	 * Lambda units.
	 * @param y desired y coordinate of bottom edge of NodeInst.
	 * Lambda units. */
//	public void abutBottom(double x, double y)
//	{
//		Point2D rp = getReferencePoint();
//		Rectangle2D bd = getBounds();
//		alterShape(sX, sY, x, y - bd.getY() + rp.getY(), angle);
//	}

	/** Translate topNode so topNode's bottom edge coincides with
	 * bottomNode's top edge, and topNode's reference point lies on a
	 * vertical line through bottomNode's reference point. bottomNode
	 * isn't moved. Don't alter any node's scale or rotation. */
//	public static void abutBottomTop(NodeInst bottomNode, NodeInst topNode)
//	{
//		topNode.abutBottom(
//			bottomNode.getReferencePoint().getX(),
//			bottomNode.getBounds().getMaxY());
//	}

	/** Translate first NodeInst so it's bottom edge is at bottomY and it's
	 * reference point has the specified x coordinate. Abut remaining
	 * nodes bottom to top.  Don't alter any NodeInst's scale or
	 * rotation.
	 * @param x desired x coordinate of all NodeInst reference points.
	 * Lambda units.
	 * @param y desired y coordinate of bottom edge of first NodeInst.
	 * Lambda units. */
//	public static void abutBottomTop(double x, double y, ArrayList nodeInsts)
//	{
//		for (int i = 0; i < nodeInsts.size(); i++)
//		{
//			NodeInst ni = (NodeInst) nodeInsts.get(i);
//			if (i == 0)
//			{
//				ni.abutBottom(x, y);
//			} else
//			{
//				abutBottomTop((NodeInst) nodeInsts.get(i - 1), ni);
//			}
//		}
//	}

	/** Don't alter position of first node. Abut remaining nodes bottom to
	 * top.  Don't alter any NodeInst's scale or rotation. */
//	public static void abutBottomTop(ArrayList nodeInsts)
//	{
//		for (int i = 1; i < nodeInsts.size(); i++)
//		{
//			abutBottomTop(
//				(NodeInst) nodeInsts.get(i - 1),
//				(NodeInst) nodeInsts.get(i));
//		}
//	}

	// Translate Electric's position parameters into Jose's internal
	// position parameters.
	// See SML# 2003-0379 for description of Jose position mathematics.
//	protected void updateAspect(
//		int lx,
//		int ly,
//		int hx,
//		int hy,
//		int rot,
//		int trans,
//		Rectangle pbounds)
//	{
//		voidBounds();
//
//		// RKao debug
//		/*
//		System.out.println("updateAspect inst bounds: "+
//			       "("+lx+", "+ly+"), ("+hx+", "+hy+")");
//		*/
//		// Electric's NodeInst center (Memo's equation 1)
//		double cX = (lx + hx) / 2.0;
//		double cY = (ly + hy) / 2.0;
//
//		// Electric's NodeInst scale (Memo's equation 7)
//		double seX =
//			pbounds.width == 0 ? 1 : (hx - lx) / (double) pbounds.width;
//		double seY =
//			pbounds.height == 0 ? 1 : (hy - ly) / (double) pbounds.height;
//
//		// proto's center
//		double pX = pbounds.x + pbounds.width / 2.0;
//		double pY = pbounds.y + pbounds.height / 2.0;
//
//		// Jose's variables (Memo's equation 28)
//		double signT = trans == 1 ? -1 : 1;
//		sX = signT * seX;
//		sY = seY;
//		double angleE = rot / 10.0; // Electric's angle in degrees
//		angleJ = trans == 1 ? (90 - angleE) : angleE;
//
//		// cache cos(angleJ) and sin(angleJ)
//		cos = Math.cos(angleJ * Math.PI / 180);
//		sin = Math.sin(angleJ * Math.PI / 180);
//
//		/* RKao debug
//		System.out.println("updateAspect: protoCent, instCent: "+
//			       "("+pX+", "+pY+"), ("+cX+", "+cY+")");
//		*/
//
//		dX = round(-signT * pX * cos + pY * sin + cX);
//		dY = round(-signT * pX * sin - pY * cos + cY);
//	}

	/** Same as newInst except it takes width and height instead of
	 * scaleX and scaleY. <p> Width and height are in Lambda units. <p>
	 * If width or height is 0 then use the default width or height.
	 * <p> Note: since Cells can only be scaled by either 1 or -1, it's
	 * more convenient to use <code>newInst</code> for Cells
	 **/
//	public NodeInst newInstWH(
//		double width,
//		double height,
//		double x,
//		double y,
//		double angle,
//		Cell parent)
//	{
//		error(this instanceof Cell,
//			"newInstWH only handles primitiveNodes. " + "use newInst instead");
//		if (sizeOffset.lx != sizeOffset.hx || sizeOffset.ly != sizeOffset.hy)
//		{
//			System.out.println("in newInstWH: " + sizeOffset);
//			error(true, "newInstWH.init: unimplemented: asymmetric offsets");
//		}
//
//		int w = lambdaToBase(width), h = lambdaToBase(height);
//
//		ElectricPosition ep =
//			joseToElecPosition(
//				hideInvisWidToScale(w),
//				hideInvisHeiToScale(h),
//				lambdaToBase(x),
//				lambdaToBase(y),
//				angle,
//				parent);
//
//		return Electric.newNodeInst(this.getAddr(), ep.lx, ep.ly, ep.hx, ep.hy, ep.transpose, ep.angle, parent.getAddr());
//	}
	/**
	 * Temporary for testing the HierarchyEnumerator.
	 * 
	 * <p>Return an AffineTransform that encodes the size, rotation, and 
	 * center of this NodeInst.
	 *  
	 * <p>The returned AffineTransform has the property that when
	 * it is applied to a unit square centered at the origin the
	 * result is the bounding box of the NodeInst.
	 * This transform is useful because it can be used to 
	 * map the position of a NodeInst through levels of the design 
	 * hierarchy.
	 *  
	 * <p>Note that the user can set the position of a NodeInst 
	 * using NodeInst.setPositionFromTransform(). For example, the 
	 * following operations make no change to a NodeInst's position:
	 * 
	 * <code>
	 * ni.setPositionFromTransform(ni.getPositionFromTransform());
	 * </code>
	 */
	public AffineTransform getPositionAsTransform() 
	{
		AffineTransform at = new AffineTransform();
		at.setToTranslation(cX, cY);
		boolean transpose = sX<0 ^ sY<0;
		if (transpose){
			at.scale(1, -1);
			at.rotate(Math.PI/2);
		}
		at.rotate(angle*Math.PI/1800);
		at.scale(sX, sY);
		return at;
	}

	private double angleFromXY(double x, double y) {
		double ans = Math.atan2(y, x) * 180/Math.PI;
		//System.out.println("(x, y): ("+x+", "+y+")  angle: "+ans);
		return ans;
	}

	private double angle0To360(double a) {
		while (a >= 360) a -= 360;
		while (a < 0)	 a += 360;
		return a;
	}

	/**
	 * Temporary for testing the HierarchyEnumerator.
	 * 
	 * <p>Set the size, angle, and center of this NodeInst based upon an
	 * affine transformation. 
	 * 
	 * <p>The AffineTransform must map a unit square centered at the 
	 * origin to the desired bounding box for the NodeInst. 
	 *
	 * <p>Note that this operation cannot succeed for all affine
	 * transformations.  The reason is that Electric's transformations
	 * always preserve right angles whereas, in general, affine
	 * transformations do not.  If the given affine transformation does
	 * not preserve right angles this routine will print a warning
	 * displaying the angle that results when a right angle is
	 * transformed.
	 * 
	 * <p>Warning: this code is experimental
	 * @param xForm the affine transformation. xForm must yield the 
	 * bounding box of the NodeInst when applied to a unit square 
	 * centered at the origin. 
	 */
	public void setPositionFromTransform(AffineTransform xForm) {
		double sizeX, sizeY, newAngle, centX, centY;
		boolean debug = false;

		if (debug) System.out.println(xForm);

		Point2D a = new Point2D.Double(0, 1); // along Y axis
		Point2D b = new Point2D.Double(0, 0); // origin
		Point2D c = new Point2D.Double(1, 0); // along X axis

		Point2D aP = new Point2D.Double();
		Point2D bP = new Point2D.Double();
		Point2D cP = new Point2D.Double();

		xForm.transform(a, aP);
		xForm.transform(b, bP);
		xForm.transform(c, cP);

		if (debug) {
			System.out.println("aP: " + aP);
			System.out.println("bP: " + bP);
			System.out.println("cP: " + cP);
		}

		sizeX = bP.distance(cP);
		sizeY = bP.distance(aP);
		centX = bP.getX();
		centY = bP.getY();
		
		double angleA = angleFromXY(aP.getX() - bP.getX(), aP.getY() - bP.getY());
		double angleC = angleFromXY(cP.getX() - bP.getX(), cP.getY() - bP.getY());
		double angleAC = angle0To360(angleA - angleC);

		if (debug) {
			System.out.println("angleC: " + angleC);
			System.out.println("angleA: " + angleA);
			System.out.println("angleAC: " + angleAC);
		}
		// round to 1/10 degrees
		angleAC = Math.rint(angleAC * 10) / 10;
		if (angleAC == 90) {
			newAngle = angle0To360(angleC);
		} else if (angleAC == 270) {
			// By using geometric constructions on paper I determined that I 
			// need to rotate by (270 degrees - angleC) and then transpose. 
			newAngle = angle0To360(270 - angleC);
			sizeX = -sizeX; // Negative size means transpose (not mirror)
		} else {
			System.out.println("error in NodeInst.setPositionFromTransform: "+
							   "angle not 90 or 270: " + angleAC);
		    newAngle = angleC;
		}

		if (debug) System.out.println(
						"setPositionFromTransform: new position {\n"
							+ "    sizeX: " + sizeX + "\n"
							+ "    sizeY: " + sizeY + "\n"
							+ "    angle: "  + newAngle + "\n"
							+ "    dx: " + centX + "\n"
							+ "    dy: " + centY + "\n"
							+ "}\n");

		modifyInstance(centX-cX, centY-cY, sizeX-sX, sizeY-sY, 
		               (int)Math.round(newAngle*10)-angle);
	}
}
