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
import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.Dimension;
import java.awt.geom.Dimension2D;
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
public class NodeInst extends Geometric implements Nodable
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

	/** key of obsolete Varible holding instance name. */	public static final Variable.Key NODE_NAME = ElectricObject.newKey("NODE_name");
	/** key of Varible holding outline information. */		public static final Variable.Key TRACE = ElectricObject.newKey("trace");

	/**
	 * the PortAssociation class is used when replacing nodes.
	 */
	static class PortAssociation
	{
		/** the original PortInst being associated. */			PortInst portInst;
		/** the Poly that describes the original PortInst. */	Poly poly;
		/** the center point in the original PortInst. */		Point2D pos;
		/** the associated PortInst on the new NodeInst. */		PortInst assn;
	}

	// ---------------------- private data ----------------------------------
	/** prototype of this NodeInst. */						private NodeProto protoType;
	/** node usage of this NodeInst. */						private NodeUsage nodeUsage;
	/** 0-based index of this NodeInst in Cell. */			private int nodeIndex;
	/** labling information for this NodeInst. */			private int textbits;
	/** HashTable of portInsts on this NodeInst. */			private List portInsts;
	/** List of connections belonging to this NodeInst. */	private List connections;
	/** List of Exports belonging to this NodeInst. */		private List exports;
	/** Text descriptor of prototype name. */				private TextDescriptor protoDescriptor;

	// The internal representation of position and orientation is the 2D transformation matrix:
	// -----------------------------------------
	// |   sX cos(angle)    sY sin(angle)   0  |
	// |  -sX sin(angle)    sY cos(angle)   0  |
	// |    center.x         center.y       1  |
	// -----------------------------------------
	/** center coordinate of this NodeInst. */				private Point2D center;
	/** size of this NodeInst (negative to mirror). */		private double sX, sY;
	/** angle of this NodeInst (in tenth-degrees). */		private int angle;

	// --------------------- private and protected methods ---------------------

	/**
	 * The constructor is never called.  Use the factory "newInstance" instead.
	 */
	private NodeInst()
	{
		// initialize this object
		this.nodeIndex = -1;
		this.textbits = 0;
		this.portInsts = new ArrayList();
		this.connections = new ArrayList();
		this.exports = new ArrayList();
		this.protoDescriptor = TextDescriptor.newInstanceDescriptor(this);
		center = new Point2D.Double();
	}

	/****************************** CREATE, DELETE, MODIFY ******************************/

	/**
	 * Method to create a NodeInst and do extra things necessary for it.
	 * @param protoType the NodeProto of which this is an instance.
	 * @param center the center location of this NodeInst.
	 * @param width the width of this NodeInst.
	 * If negative, flip the X coordinate (or flip ABOUT the Y axis).
	 * @param height the height of this NodeInst.
	 * If negative, flip the Y coordinate (or flip ABOUT the X axis).
	 * @param angle the angle of this NodeInst (in tenth-degrees).
	 * @param parent the Cell in which this NodeInst will reside.
	 * @param name name of new NodeInst
	 * @return the newly created NodeInst, or null on error.
	 */
	public static NodeInst makeInstance(NodeProto protoType, Point2D center, double width, double height,
		int angle, Cell parent, String name)
	{
		NodeInst ni = newInstance(protoType, center, width, height, angle, parent, name);
		if (ni != null)
		{
			// set default information from the prototype
			if (protoType instanceof Cell)
			{
				// for cells, use the default expansion on this instance
				if (protoType.isWantExpanded()) ni.setExpanded();
			} else
			{
				// for primitives, set a default outline if appropriate
				protoType.getTechnology().setDefaultOutline(ni);
			}
		}
		return ni;
	}

	/**
	 * Method to create a "dummy" NodeInst for use outside of the database.
	 * @param np the prototype of the NodeInst.
	 * @return the dummy NodeInst.
	 */
	public static NodeInst makeDummyInstance(NodeProto np)
	{
		NodeInst ni = NodeInst.lowLevelAllocate();
		ni.lowLevelPopulate(np, new Point2D.Double(0,0), np.getDefWidth(), np.getDefHeight(), 0, null);
		return ni;
	}

	/**
	 * Method to create a NodeInst.
	 * @param protoType the NodeProto of which this is an instance.
	 * @param center the center location of this NodeInst.
	 * @param width the width of this NodeInst.
	 * If negative, flip the X coordinate (or flip ABOUT the Y axis).
	 * @param height the height of this NodeInst.
	 * If negative, flip the Y coordinate (or flip ABOUT the X axis).
	 * @param angle the angle of this NodeInst (in tenth-degrees).
	 * @param parent the Cell in which this NodeInst will reside.
	 * @param name name of new NodeInst
	 * @return the newly created NodeInst, or null on error.
	 */
	public static NodeInst newInstance(NodeProto protoType, Point2D center, double width, double height,
		int angle, Cell parent, String name)
	{
        if (parent == null) return null;
        
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
		if (name != null) ni.setName(name);
		if (ni.lowLevelLink()) return null;

		// handle change control, constraint, and broadcast
		Undo.newObject(ni);
		return ni;
	}

	/**
	 * Method to delete this NodeInst.
	 */
	public void kill()
	{
		// kill the arcs attached to the connections.  This will also remove the connections themselves
		while (connections.size() > 0)
		{
			Connection con = (Connection)connections.get(connections.size() - 1);
			con.getArc().kill();
		}

		// remove any exports
		while (exports.size() > 0)
		{
			Export pp = (Export)exports.get(exports.size() - 1);
			pp.kill();
		}

		// remove the node
		lowLevelUnlink();

		// handle change control, constraint, and broadcast
		Undo.killObject(this);
	}

	/**
	 * Method to change this NodeInst.
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
//		if (dX == 0 && dY == 0 && dXSize == 0 && dYSize == 0 && dRot == 0)
//		{
//			return;
//		}

		// make the change
		if (Undo.recordChange())
		{
			Constraints.getCurrent().modifyNodeInst(this, dX, dY, dXSize, dYSize, dRot);
		} else
		{
			lowLevelModify(dX, dY, dXSize, dYSize, dRot);

			// change the coordinates of every arc end connected to this
			for(Iterator it = getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				if (con.getPortInst().getNodeInst() == this)
				{
					Point2D oldLocation = con.getLocation();
					if (con.isHeadEnd()) con.getArc().modify(0, dX, dY, 0, 0); else
						con.getArc().modify(0, 0, 0, dX, dY);
				}
			}
		}
		parent.setDirty();

		// if the cell-center changed, notify the cell and fix lots of stuff
		if (protoType instanceof PrimitiveNode && protoType == Generic.tech.cellCenterNode)
		{
			parent.adjustReferencePoint(this);
		}
	}

	/**
	 * Method to change many NodeInsts.
	 * @param nis the NodeInsts to change.
	 * @param dXs the amount to move the NodeInsts in X.
	 * @param dYs the amount to move the NodeInsts in Y.
	 * @param dXSizes the amount to scale the NodeInsts in X.
	 * @param dYSizes the amount to scale the NodeInsts in Y.
	 * @param dRots the amount to alter the NodeInst rotation (in tenths of a degree).
	 */
	public static void modifyInstances(NodeInst [] nis, double [] dXs, double [] dYs, double [] dXSizes, double [] dYSizes, int [] dRots)
	{
		// make the change
		if (Undo.recordChange())
		{
			Constraints.getCurrent().modifyNodeInsts(nis, dXs, dYs, dXSizes, dYSizes, dRots);
		} else
		{
			for(int i=0; i<nis.length; i++)
			{
				nis[i].lowLevelModify(dXs[i], dYs[i], dXSizes[i], dYSizes[i], dRots[i]);

				// change the coordinates of every arc end connected to this
				for(Iterator it = nis[i].getConnections(); it.hasNext(); )
				{
					Connection con = (Connection)it.next();
					if (con.getPortInst().getNodeInst() == nis[i])
					{
						Point2D oldLocation = con.getLocation();
						if (con.isHeadEnd()) con.getArc().modify(0, dXs[i], dYs[i], 0, 0); else
							con.getArc().modify(0, 0, 0, dXs[i], dYs[i]);
					}
				}
			}
		}

		// if the cell-center changed, notify the cell and fix lots of stuff
		for(int i=0; i<nis.length; i++)
		{
			nis[i].getParent().setDirty();
			if (nis[i].getProto() instanceof PrimitiveNode && nis[i].getProto() == Generic.tech.cellCenterNode)
			{
				nis[i].getParent().adjustReferencePoint(nis[i]);
			}
		}
	}

	/*
	 * Method to replace this NodeInst with one of another type.
	 * All arcs and exports on this NodeInst are moved to the new one.
	 * @param np the new type to put in place of this NodeInst.
	 * @param ignorePortNames true to not use port names when determining association between old and new prototype.
	 * @param allowMissingPorts true to allow replacement to have missing ports and, therefore, delete the arcs that used to be there.
	 * @return the new NodeInst that replaces this one.
	 * Returns null if there is an error doing the replacement.
	 */
	public NodeInst replace(NodeProto np, boolean ignorePortNames, boolean allowMissingPorts)
	{
		// check for recursion
		if (np instanceof Cell)
		{
			if (getParent().isAChildOf((Cell)np)) return null;
		}

		// get the location of the cell-center on the old NodeInst
		Point2D oldCenter = getGrabCenter();

		// create the new NodeInst
		double newXS = np.getDefWidth();
		double newYS = np.getDefHeight();
		if (np instanceof PrimitiveNode && getProto() instanceof PrimitiveNode)
		{
			// replacing one primitive with another: adjust sizes accordingly
			SizeOffset oldSO = getProto().getSizeOffset();
			SizeOffset newSO = np.getSizeOffset();
			newXS = getXSize() - oldSO.getLowXOffset() - oldSO.getHighXOffset() + newSO.getLowXOffset() + newSO.getHighXOffset();
			newYS = getYSize() - oldSO.getLowYOffset() - oldSO.getHighYOffset() + newSO.getLowYOffset() + newSO.getHighYOffset();
		}
		NodeInst newNi = NodeInst.newInstance(np, oldCenter, newXS, newYS, getAngle(), getParent(), null);
		if (newNi == null) return null;

//		// adjust position of the new NodeInst to align centers
//		Point2D newCenter = newNi.getGrabCenter();
//		newNi.modifyInstance(oldCenter.getX()-newCenter.getX(), oldCenter.getY()-newCenter.getY(), 0, 0, 0);

		// draw new node expanded if appropriate
		if (np instanceof Cell)
		{
			if (getProto() instanceof Cell)
			{
				// replacing an instance: copy the expansion information
				if (isExpanded()) newNi.setExpanded(); else
					newNi.clearExpanded();
			} else
			{
				// replacing a primitive: use default expansion for the cell
				if (np.isWantExpanded()) newNi.setExpanded(); else
					newNi.clearExpanded();
			}
		}

		// associate the ports between these nodes
		PortAssociation [] oldAssoc = portAssociate(this, newNi, ignorePortNames);

		// see if the old arcs can connect to ports
		double arcDx = 0, arcDy = 0;
		int arcCount = 0;
		for(Iterator it = getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			// make sure there is an association for this port

			int index = 0;
			for( ; index<oldAssoc.length; index++)
				if (oldAssoc[index].portInst == con.getPortInst()) break;
			if (index >= oldAssoc.length || oldAssoc[index].assn == null)
			{
				if (allowMissingPorts) continue;
				System.out.println("No port on new node corresponds to old port: " + con.getPortInst().getPortProto().getProtoName());
				newNi.kill();
				return null;
			}

			// make sure the arc can connect to this type of port
			PortInst opi = oldAssoc[index].assn;
			ArcInst ai = con.getArc();
			if (!opi.getPortProto().connectsTo(ai.getProto()))
			{
				if (allowMissingPorts) continue;
				System.out.println(ai.describe() + " arc on old port " + con.getPortInst().getPortProto().getProtoName() +
					" cannot connect to new port " + opi.getPortProto().getProtoName());
				newNi.kill();
				return null;
			}

			// see if the arc fits in the new port
			Poly poly = opi.getPoly();
			if (!poly.isInside(con.getLocation()))
			{
				// arc doesn't fit: accumulate error distance
				double xp = poly.getCenterX();
				double yp = poly.getCenterY();
				arcDx += xp - con.getLocation().getX();
				arcDy += yp - con.getLocation().getY();
			}
			arcCount++;
		}

		// see if the old exports have the same connections
		for(Iterator it = getExports(); it.hasNext(); )
		{
			Export pp = (Export)it.next();

			// make sure there is an association for this port
			int index = 0;
			for( ; index<oldAssoc.length; index++)
				if (oldAssoc[index].portInst == pp.getOriginalPort()) break;
			if (index >= oldAssoc.length || oldAssoc[index].assn == null)
			{
				System.out.println("No port on new node corresponds to old port: " +
					pp.getOriginalPort().getPortProto().getProtoName());
				newNi.kill();
				return null;
			}
			PortInst opi = oldAssoc[index].assn;

			// ensure that all arcs connected at exports still connect
			if (pp.doesntConnect(opi.getPortProto().getBasePort()))
			{
				newNi.kill();
				return null;
			}
		}

		// now replace all of the arcs
		List arcList = new ArrayList();
		for(Iterator it = getConnections(); it.hasNext(); )
		{
			arcList.add(it.next());
		}
		for(Iterator it = arcList.iterator(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			int index = 0;
			for( ; index<oldAssoc.length; index++)
				if (oldAssoc[index].portInst == con.getPortInst()) break;
			if (index >= oldAssoc.length || oldAssoc[index].assn == null)
			{
				if (allowMissingPorts) continue;
				System.out.println("No port on new node corresponds to old port: " + con.getPortInst().getPortProto().getProtoName());
				newNi.kill();
				return null;
			}

			// make sure the arc can connect to this type of port
			PortInst opi = oldAssoc[index].assn;
			PortInst [] newPortInst = new PortInst[2];
			Point2D [] newPoint = new Point2D.Double[2];
			ArcInst ai = con.getArc();
			for(int i=0; i<2; i++)
			{
				Connection oneCon = ai.getConnection(i);
				if (oneCon == con)
				{
					newPortInst[i] = opi;
					if (newPortInst[i] == null) break;
					newPoint[i] = new Point2D.Double(con.getLocation().getX(), con.getLocation().getY());
					Poly poly = opi.getPoly();
					if (!poly.isInside(newPoint[i]))
						newPoint[i].setLocation(poly.getCenterX(), poly.getCenterY());
				} else
				{
					newPortInst[i] = oneCon.getPortInst();
					newPoint[i] = oneCon.getLocation();
				}
			}
			if (newPortInst[0] == null || newPortInst[1] == null)
			{
				if (!allowMissingPorts)
				{
					System.out.println("Cannot re-connect " + ai.describe() + " arc");
				} else
				{
					ai.kill();
				}
				continue;
			}

			// see if a bend must be made in the wire
			boolean zigzag = false;
			if (ai.isFixedAngle())
			{
				if (newPoint[0].getX() != newPoint[1].getX() || newPoint[0].getY() != newPoint[1].getY())
				{
					int ii = EMath.figureAngle(newPoint[0], newPoint[1]);
					int ang = ai.getAngle();
					if ((ii%1800) != (ang%1800)) zigzag = true;
				}
			}
			ArcInst newAi;
			if (zigzag)
			{
				// make that two wires
				double cX = newPoint[0].getX();
				double cY = newPoint[1].getY();
				NodeProto pinNp = ((PrimitiveArc)ai.getProto()).findPinProto();
				double psx = pinNp.getDefWidth();
				double psy = pinNp.getDefHeight();
				NodeInst pinNi = NodeInst.newInstance(pinNp, new Point2D.Double(cX, cY), psx, psy, 0, getParent(), null);
				PortInst pinPi = pinNi.getOnlyPortInst();
				newAi = ArcInst.newInstance(ai.getProto(), ai.getWidth(), newPortInst[0], newPoint[0],
					pinPi, new Point2D.Double(cX, cY), null);
				if (newAi == null) return null;
				newAi.lowLevelSetUserbits(ai.lowLevelGetUserbits());
				ArcInst newAi2 = ArcInst.newInstance(ai.getProto(), ai.getWidth(), pinPi, new Point2D.Double(cX, cY),
					newPortInst[1], newPoint[1], null);
				if (newAi2 == null) return null;
				newAi2.lowLevelSetUserbits(ai.lowLevelGetUserbits());
				newAi2.clearNegated();
				if (newPortInst[1].getNodeInst() == this)
				{
					ArcInst aiSwap = newAi;   newAi = newAi2;   newAi2 = aiSwap;
				}
			} else
			{
				// replace the arc with another arc
				newAi = ArcInst.newInstance(ai.getProto(), ai.getWidth(), newPortInst[0], newPoint[0], newPortInst[1], newPoint[1], null);
				if (newAi == null)
				{
					newNi.kill();
					return null;
				}
				newAi.lowLevelSetUserbits(ai.lowLevelGetUserbits());
			}
			newAi.copyVars(ai);
			ai.kill();
			newAi.setName(ai.getName());
		}

		// now replace all of the exports
		List exportList = new ArrayList();
		for(Iterator it = getExports(); it.hasNext(); )
		{
			exportList.add(it.next());
		}
		for(Iterator it = exportList.iterator(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			int index = 0;
			for( ; index<oldAssoc.length; index++)
				if (oldAssoc[index].portInst == pp.getOriginalPort()) break;
			if (index >= oldAssoc.length || oldAssoc[index].assn == null) continue;
			PortInst newPi = oldAssoc[index].assn;
			pp.move(newPi);
		}

		// copy all variables on the nodeinst
		newNi.copyVars(this);
		newNi.setNameTextDescriptor(getNameTextDescriptor());
		newNi.setProtoTextDescriptor(getProtoTextDescriptor());
		newNi.lowLevelSetUserbits(lowLevelGetUserbits());

		// now delete the original nodeinst
		kill();
		newNi.setName(getName());
		return newNi;
	}

	/****************************** LOW-LEVEL IMPLEMENTATION ******************************/

	/**
	 * Low-level access method to create a NodeInst.
	 * @return the newly created NodeInst.
	 */
	public static NodeInst lowLevelAllocate()
	{
		NodeInst ni = new NodeInst();
		ni.parent = null;
		return ni;
	}

	/**
	 * Low-level method to fill-in the NodeInst information.
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
	public boolean lowLevelPopulate(NodeProto protoType, Point2D center, double width, double height, int angle,
		Cell parent)
	{
		setParent(parent);
		this.protoType = protoType;

		// create all of the portInsts on this node inst
		for (Iterator it = protoType.getPorts(); it.hasNext();)
		{
			PortProto pp = (PortProto) it.next();
			addPortInst(pp);
		}

		this.center.setLocation(center);
		this.sX = width;   this.sY = height;
		this.angle = angle;

		// fill in the geometry
		redoGeometric();
		return false;
	}

	/**
	 * Low-level access method to link the NodeInst into its Cell.
	 * @return true on error.
	 */
	public boolean lowLevelLink()
	{
		if (!inDatabase()) {
			System.out.println("NodeInst can't be linked because it is not in database");
			return true;
		}
		if (!isUsernamed())
		{
			if (getName() == null || !parent.isUniqueName(name, getClass(), this))
				if (setNameKey(parent.getAutoname(getBasename()))) return true;
		}
		if (checkAndRepair() > 0) return true;

		// add to linked lists
		linkGeom(parent);
		nodeUsage = parent.addNode(this);
		return false;
	}

	/**
	 * Low-level method to unlink the NodeInst from its Cell.
	 */
	public void lowLevelUnlink()
	{
		// remove this node from the cell
		unLinkGeom(parent);
		parent.removeNode(this);
		nodeUsage = null;
	}

	/**
	 * Method to adjust this NodeInst by the specified deltas.
	 * This method does not go through change control, and so should not be used unless you know what you are doing.
	 * @param dX the change to the center X coordinate.
	 * @param dY the change to the center Y coordinate.
	 * @param dXSize the change to the X size.
	 * @param dYSize the change to the Y size.
	 * @param dRot the change to the rotation (in tenths of a degree).
	 */
	public void lowLevelModify(double dX, double dY, double dXSize, double dYSize, int dRot)
	{
		// remove from the R-Tree structure
		unLinkGeom(parent);

		// make the change
		center.setLocation(EMath.smooth(getGrabCenterX() + dX), EMath.smooth(getGrabCenterY() + dY));

		sX = EMath.smooth(this.sX + dXSize);
		sY = EMath.smooth(this.sY + dYSize);

		angle = (angle +dRot) % 3600;

		// fill in the Geometric fields
		redoGeometric();

		// link back into the R-Tree
		linkGeom(parent);
	}

	/**
	 * Method to tell whether this NodeInst is an icon of its parent.
	 * Electric does not allow recursive circuit hierarchies (instances of Cells inside of themselves).
	 * However, it does allow one exception: a schematic may contain its own icon for documentation purposes.
	 * This method determines whether this NodeInst is such an icon.
	 * @return true if this NodeInst is an icon of its parent.
	 */
	public boolean isIconOfParent()
	{
		NodeProto np = getProto();
		if (!(np instanceof Cell))
			return false;

		return getParent().getCellGroup() == ((Cell) np).getCellGroup();
	}

	/**
	 * Method to set an index of this NodeInst in Cell nodes.
	 * This is a zero-based index of nodes on the Cell.
	 * @param nodeIndex an index of this NodeInst in Cell nodes.
	 */
	public void setNodeIndex(int nodeIndex) { this.nodeIndex = nodeIndex; }

	/**
	 * Method to get the index of this NodeInst.
	 * This is a zero-based index of nodes on the Cell.
	 * @return the index of this NodeInst.
	 */
	public final int getNodeIndex() { return nodeIndex; }

	/**
	 * Method tells if this NodeInst is linked to parent Cell.
	 * @return true if this NodeInst is linked to parent Cell.
	 */
	public boolean isLinked() { return nodeIndex >= 0; }

	/****************************** GRAPHICS ******************************/

	/**
	 * Method to return the rotation angle of this NodeInst.
	 * @return the rotation angle of this NodeInst (in tenth-degrees).
	 */
	public int getAngle() { return angle; }

	/**
	 * Method to return the center point of this NodeInst object.
	 * @return the center point of this NodeInst object.
	 */
	public Point2D getGrabCenter() { return center; }

	/**
	 * Method to return the center X coordinate of this NodeInst.
	 * @return the center X coordinate of this NodeInst.
	 */
	public double getGrabCenterX() { return center.getX(); }

	/**
	 * Method to return the center Y coordinate of this NodeInst.
	 * @return the center Y coordinate of this NodeInst.
	 */
	public double getGrabCenterY() { return center.getY(); }

	/**
	 * Method to return the X size of this NodeInst.
	 * @return the X size of this NodeInst.
	 */
	public double getXSize() { return Math.abs(sX); }

	/**
	 * Method to return the Y size of this NodeInst.
	 * @return the Y size of this NodeInst.
	 */
	public double getYSize() { return Math.abs(sY); }
	
	/**
	 * Method to return whether NodeInst is mirrored about a 
	 * horizontal line running through its center.
	 * @return true if mirrored 
	 */
	public boolean isMirroredAboutXAxis() { return sY<0;}
	
	/** 
	 * Method to return whether NodeInst is mirrored about a
	 * vertical line running through its center.
	 * @return true if mirrored
	 */
	public boolean isMirroredAboutYAxis() { return sX<0; }

	/**
	 * Method to return the starting and ending angle of an arc described by this NodeInst.
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

		Variable var = getVar(Artwork.ART_DEGREES);
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

	/**
	 * Method to set the starting and ending angle of an arc described by this NodeInst.
	 * These values are stored in the "ART_degrees" variable on the NodeInst.
	 * @param start the starting offset of the angle (typically 0)
	 * @param curvature the the amount of curvature
	 */
	public void setArcDegrees(double start, double curvature)
	{
		Float [] fAddr = new Float[2];
		fAddr[0] = new Float(start);
		fAddr[1] = new Float(curvature);
		this.newVar(Artwork.ART_DEGREES, fAddr);
	}

	/**
	 * Method to recalculate the Geometric bounds for this NodeInst.
	 */
	void redoGeometric()
	{
		if (sX == 0 && sY == 0)
		{
			visBounds.setRect(getGrabCenterX(), getGrabCenterY(), 0, 0);
		} else
		{
			// special case for arcs of circles
			if (protoType == Artwork.tech.circleNode || protoType == Artwork.tech.thickCircleNode)
			{
				// see if there this circle is only a partial one */
				double [] angles = getArcDegrees();
				if (angles[0] != 0.0 || angles[1] != 0.0)
				{
					Point2D [] pointList = Artwork.fillEllipse(getGrabCenter(), Math.abs(sX), Math.abs(sY), angles[0], angles[1]);
					Poly poly = new Poly(pointList);
					poly.setStyle(Poly.Type.OPENED);
					poly.transform(rotateOut());
					visBounds.setRect(poly.getBounds2D());
					return;
				}
			}

			// special case for pins that become steiner points
			if (protoType.isWipeOn1or2() && getNumExports() == 0)
			{
				if (pinUseCount())
				{
					visBounds.setRect(getGrabCenterX(), getGrabCenterY(), 0, 0);
					return;
				}
			}

			// special case for polygonally-defined nodes: compute precise geometry */
			if (protoType.isHoldsOutline())
			{
				Float [] outline = getTrace();
				if (outline != null)
				{
					int numPoints = outline.length / 2;
					Point2D [] pointList = new Point2D.Double[numPoints];
					for(int i=0; i<numPoints; i++)
					{
						pointList[i] = new Point2D.Double(getGrabCenterX() + outline[i*2].floatValue(),
							getGrabCenterY() + outline[i*2+1].floatValue());
					}
					Poly poly = new Poly(pointList);
					poly.setStyle(Poly.Type.OPENED);
					poly.transform(rotateOut());
					visBounds.setRect(poly.getBounds2D());
					return;
				}
			}

			// convert the center, rotation, and size into a bounds
			double cX = center.getX(), cY = center.getY();
			Poly poly = null;
			if (protoType instanceof Cell)
			{
				// offset by distance from cell-center to the true center
				Cell subCell = (Cell)protoType;
				Rectangle2D bounds = subCell.getBounds();
				Point2D shift = new Point2D.Double(-bounds.getCenterX(), -bounds.getCenterY());
				AffineTransform trans = pureRotate(angle, sX, sY);
				trans.transform(shift, shift);
				cX -= shift.getX();
				cY -= shift.getY();
				poly = new Poly(cX, cY, Math.abs(sX), Math.abs(sY));
				trans = rotateAbout(angle, cX, cY, sX, sY);
				poly.transform(trans);
			} else
			{
				poly = new Poly(cX, cY, sX, sY);
				AffineTransform trans = rotateOut();
				poly.transform(trans);
			}

			// return its bounds
			visBounds.setRect(poly.getBounds2D());
		}
	}

	/**
	 * Method to return a list of Polys that describes all text on this NodeInst.
	 * @param hardToSelect is true if considering hard-to-select text.
	 * @param wnd the window in which the text will be drawn.
	 * @return an array of Polys that describes the text.
	 */
	public Poly [] getAllText(boolean hardToSelect, EditWindow wnd)
	{
		int nodeNameText = 0;
		if (protoType instanceof Cell && !isExpanded()) nodeNameText = 1;
		if (!User.isTextVisibilityOnInstance()) nodeNameText = 0;
		if (!User.isEasySelectionOfAnnotationText()) nodeNameText = 0;
		int dispVars = numDisplayableVariables(false);
		int numExports = 0;
		int numExportVariables = 0;
		if (User.isTextVisibilityOnExport())
		{
			numExports = getNumExports();
			for(Iterator it = getExports(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				numExportVariables += pp.numDisplayableVariables(false);
			}
		}
		if (protoType == Generic.tech.invisiblePinNode &&
			!User.isTextVisibilityOnAnnotation())
		{
			nodeNameText = dispVars = numExports = numExportVariables = 0;
		}
		if (!User.isTextVisibilityOnNode())
		{
			nodeNameText = dispVars = numExports = numExportVariables = 0;
		}
		int totalText = nodeNameText + dispVars + numExports + numExportVariables;
		if (totalText == 0) return null;
		Poly [] polys = new Poly[totalText];
		int start = 0;

		// add in the cell name if appropriate
		if (nodeNameText != 0)
		{
			double cX = getTrueCenterX();
			double cY = getTrueCenterY();
			TextDescriptor td = getProtoTextDescriptor();
			double offX = td.getXOff();
			double offY = td.getYOff();
			TextDescriptor.Position pos = td.getPos();
			Poly.Type style = pos.getPolyType();
			Point2D [] pointList = new Point2D.Double[1];
			pointList[0] = new Point2D.Double(cX+offX, cY+offY);
			polys[start] = new Poly(pointList);
			polys[start].setStyle(style);
			polys[start].setString(getProto().describe());
			polys[start].setTextDescriptor(td);
			start++;
		}

		// add in the exports
		if (numExports > 0)
		{
			for(Iterator it = getExports(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				polys[start] = pp.getNamePoly();
				start++;

				// add in variables on the exports
				Poly poly = pp.getOriginalPort().getPoly();
				int numadded = pp.addDisplayableVariables(poly.getBounds2D(), polys, start, wnd, false);
				for(int i=0; i<numadded; i++)
					polys[start+i].setPort(pp);
				start += numadded;
			}
		}

		// add in the displayable variables
		if (dispVars > 0) addDisplayableVariables(getBounds(), polys, start, wnd, false);
		return polys;
	}

	/**
	 * Method to return the number of displayable Variables on this NodeInst and all of its PortInsts.
	 * A displayable Variable is one that will be shown with its object.
	 * Displayable Variables can only sensibly exist on NodeInst, ArcInst, and PortInst objects.
	 * @return the number of displayable Variables on this NodeInst and all of its PortInsts.
	 */
	public int numDisplayableVariables(boolean multipleStrings)
	{
		int numVarsOnNode = super.numDisplayableVariables(multipleStrings);

		for(Iterator it = getPortInsts(); it.hasNext(); )
		{
			PortInst pi = (PortInst)it.next();
			numVarsOnNode += pi.numDisplayableVariables(multipleStrings);
		}
		return numVarsOnNode;
	}

	/**
	 * Method to add all displayable Variables on this NodeInst and its PortInsts to an array of Poly objects.
	 * @param rect a rectangle describing the bounds of the NodeInst on which the Variables will be displayed.
	 * @param polys an array of Poly objects that will be filled with the displayable Variables.
	 * @param start the starting index in the array of Poly objects to fill with displayable Variables.
	 * @param wnd window in which the Variables will be displayed.
	 * @param multipleStrings true to break multiline text into multiple Polys.
	 * @return the number of Polys that were added.
	 */
	public int addDisplayableVariables(Rectangle2D rect, Poly [] polys, int start, EditWindow wnd, boolean multipleStrings)
	{
		int numAddedVariables = super.addDisplayableVariables(rect, polys, start, wnd, multipleStrings);

		for(Iterator it = getPortInsts(); it.hasNext(); )
		{
			PortInst pi = (PortInst)it.next();
			int justAdded = pi.addDisplayableVariables(rect, polys, start+numAddedVariables, wnd, multipleStrings);
			for(int i=0; i<justAdded; i++)
				polys[start+numAddedVariables+i].setPort(pi.getPortProto());
			numAddedVariables += justAdded;
		}
		return numAddedVariables;
	}

	/**
	 * Method to return a transformation that moves up the hierarchy.
	 * Presuming that this NodeInst is a Cell instance, the
	 * transformation goes from the space of that Cell to the space of
	 * this NodeInst's parent Cell.  The transformation includes the
	 * rotation of this NodeInst.
	 * @return a transformation that moves up the hierarchy.
	 */
	public AffineTransform transformOut()
	{
		// to transform out of this node instance, first translate
		// inner coordinates to outer
		Cell lowerCell = (Cell)protoType;
		Rectangle2D bounds = lowerCell.getBounds();
		double dx = getGrabCenterX() - bounds.getCenterX();
		double dy = getGrabCenterY() - bounds.getCenterY();
		AffineTransform transform = new AffineTransform();
		transform.setToRotation(angle*Math.PI/1800, getGrabCenterX(), getGrabCenterY());
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
		double dx = getGrabCenterX() - bounds.getCenterX();
		double dy = getGrabCenterY() - bounds.getCenterY();
		AffineTransform transform = new AffineTransform();
		boolean transpose = sX<0 ^ sY<0;
		if (transpose) {
			transform.translate(getGrabCenterX(), getGrabCenterY());
			transform.scale(1, -1);
			transform.rotate(Math.PI/2);
			transform.translate(-getGrabCenterX(), -getGrabCenterY());
		}
		transform.rotate(angle*Math.PI/1800, getGrabCenterX(), getGrabCenterY());
		transform.translate(dx, dy);
//		System.out.println("rkTransformOut: {\n"
//		                   + "    lowerCellBounds: " + bounds + "\n"
//						   + "    (cX, cY): " + cX + " " + cY + "\n"
//						   + "    angle: " + (angle/10.0) + "\n"
//						   + "    (sX, sY): " + sX + " " + sY + "\n"
//						   + "    xform: " + transform + "\n"
//						   + "}\n");
		return transform;
	}

	/**
	 * Method to return a transformation that moves up the hierarchy,
	 * combined with a previous transformation.  Presuming that this
	 * NodeInst is a Cell instance, the transformation goes from the
	 * space of that Cell to the space of this NodeInst's parent Cell.
	 * The transformation includes the rotation of this NodeInst.
	 * @param prevTransform the previous transformation to the
	 * NodeInst's Cell.
	 * @return a transformation that moves up the hierarchy, including
	 * the previous transformation.
	 */
//	public AffineTransform transformOut(AffineTransform prevTransform)
//	{
//		AffineTransform transform = transformOut();
//		AffineTransform returnTransform = new AffineTransform(prevTransform);
//		returnTransform.concatenate(transform);
//		return returnTransform;
//	}

	/**
	 * Method to return a transformation that translates down the hierarchy.
	 * Presuming that this NodeInst is a Cell instance, the transformation goes
	 * from the space of this NodeInst's parent Cell to the space of the contents of the Cell.
	 * However, it does not account for the rotation of this NodeInst...it only
	 * translates from one space to another.
	 * @return a transformation that translates down the hierarchy.
	 */
	public AffineTransform translateIn()
	{
		// to transform out of this node instance, translate inner coordinates to outer
		Cell lowerCell = (Cell)protoType;
		double dx = getGrabCenterX();
		double dy = getGrabCenterY();
		AffineTransform transform = new AffineTransform();
		transform.translate(-dx, -dy);
		return transform;
	}

	/**
	 * Method to return a transformation that translates up the hierarchy.
	 * Presuming that this NodeInst is a Cell instance, the transformation goes
	 * from the space of that Cell to the space of this NodeInst's parent Cell.
	 * However, it does not account for the rotation of this NodeInst...it only
	 * translates from one space to another.
	 * @return a transformation that translates up the hierarchy.
	 */
	public AffineTransform translateOut()
	{
		// to transform out of this node instance, translate inner coordinates to outer
		Cell lowerCell = (Cell)protoType;
		double dx = getGrabCenterX();
		double dy = getGrabCenterY();
		AffineTransform transform = new AffineTransform();
		transform.translate(dx, dy);
		return transform;
	}

	/**
	 * Method to return a transformation that translates up the
	 * hierarchy, combined with a previous transformation.  Presuming
	 * that this NodeInst is a Cell instance, the transformation goes
	 * from the space of that Cell to the space of this NodeInst's
	 * parent Cell.  However, it does not account for the rotation of
	 * this NodeInst...it only translates from one space to another.
	 * @param prevTransform the previous transformation to the NodeInst's Cell.
	 * @return a transformation that translates up the hierarchy,
	 * including the previous transformation.
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
	 * Method to return a transformation that rotates an object about a point.
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
	 * Method to return a transformation that rotates an object.
	 * @param angle the amount to rotate (in tenth-degrees).
	 * @param sX the scale in X (negative to flip the X coordinate, or
	 * flip ABOUT the Y axis).
	 * @param sY the scale in Y (negative to flip the Y coordinate, or
	 * flip ABOUT the X axis).
	 * @return a transformation that rotates by this amount.
	 */
	public static AffineTransform pureRotate(int angle, double sX, double sY)
	{
		AffineTransform transform = new AffineTransform();
		transform.setToRotation(angle * Math.PI / 1800.0);

		// add mirroring
		if (sX < 0) transform.preConcatenate(mirrorX);
		if (sY < 0) transform.preConcatenate(mirrorY);
		return transform;
	}

	/**
	 * Method to return a transformation that unrotates this NodeInst.
	 * It transforms points on this NodeInst that have been rotated with the node
	 * so that they appear in the correct location on the unrotated node.
	 * The rotation happens about the node's Grab Point (the location of the cell-center inside of cell definitions).
	 * @return a transformation that unrotates this NodeInst.
	 * If this NodeInst is not rotated, the returned transformation is identity.
	 */
	public AffineTransform rotateIn()
	{
		int numFlips = 0;
		if (sX < 0) numFlips++;
		if (sY < 0) numFlips++;
		int rotAngle = angle;
		if (numFlips != 1) rotAngle = -rotAngle;
		return rotateAbout(rotAngle, getGrabCenterX(), getGrabCenterY(), sX, sY);
	}

	/**
	 * Method to return a transformation that rotates this NodeInst.
	 * It transforms points on this NodeInst to account for the NodeInst's rotation.
	 * The rotation happens about the node's Grab Point (the location of the cell-center inside of cell definitions).
	 * @return a transformation that rotates this NodeInst.
	 * If this NodeInst is not rotated, the returned transformation is identity.
	 */
	public AffineTransform rotateOut()
	{
		return rotateAbout(angle, getGrabCenterX(), getGrabCenterY(), sX, sY);
	}

	/**
	 * Method to return a transformation that rotates this NodeInst.
	 * It transforms points on this NodeInst to account for the NodeInst's rotation.
	 * The rotation happens about the node's true geometric center.
	 * @return a transformation that rotates this NodeInst.
	 * If this NodeInst is not rotated, the returned transformation is identity.
	 */
	public AffineTransform rotateOutAboutTrueCenter()
	{
		return rotateAbout(angle, getTrueCenterX(), getTrueCenterY(), sX, sY);
	}

	/**
	 * Method to return a transformation that rotates this NodeInst,
	 * combined with a previous transformation.  It transforms points
	 * on this NodeInst to account for the NodeInst's rotation.
	 * The rotation happens about the node's Grab Point (the location of the cell-center inside of cell definitions).
	 * @param prevTransform the previous transformation to be applied.
	 * @return a transformation that rotates this NodeInst, combined
	 * with a previous transformation..  If this NodeInst is not
	 * rotated, the returned transformation is identity.
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
	 * Method to return a transformation that rotates this NodeInst,
	 * combined with a previous transformation.  It transforms points
	 * on this NodeInst to account for the NodeInst's rotation.
	 * The rotation happens about the node's true geometric center.
	 * @param prevTransform the previous transformation to be applied.
	 * @return a transformation that rotates this NodeInst, combined
	 * with a previous transformation..  If this NodeInst is not
	 * rotated, the returned transformation is identity.
	 */
	public AffineTransform rotateOutAboutTrueCenter(AffineTransform prevTransform)
	{
		// if there is no transformation, stop now
		if (angle == 0 && sX >= 0 && sY >= 0) return prevTransform;

		AffineTransform transform = rotateOutAboutTrueCenter();
		AffineTransform returnTransform = new AffineTransform(prevTransform);
		returnTransform.concatenate(transform);
		return returnTransform;
	}

//	/**
//	 * Method to return the lower-left corner of this NodeInst.  The
//	 * corner considers both the highlight offset and the rotation, so
//	 * the coordinate is that of the lower-left valid part, as placed
//	 * in the database.
//	 * @return a Point with the location of this NodeInst's lower-left
//	 * corner.
//	 */
//	public Point2D getLowLeft()
//	{
//		SizeOffset so = getProto().getSizeOffset();
//
//		double width = getXSize() / 2;
//		double height = getYSize() / 2;
//		double lX = getTrueCenterX() - width + so.getLowXOffset();
//		double hX = getTrueCenterX() + width - so.getHighXOffset();
//		double lY = getTrueCenterY() - height + so.getLowYOffset();
//		double hY = getTrueCenterY() + height - so.getHighYOffset();
//		Poly poly = new Poly((lX+hX)/2, (lY+hY)/2, hX-lX, hY-lY);
//		if (getAngle() != 0 || isXMirrored() || isYMirrored())
//		{
//			AffineTransform trans = this.rotateOut();
//			poly.transform(trans);
//		}
//		Rectangle2D bounds = poly.getBounds2D();
//		return new Point2D.Double(bounds.getMinX(), bounds.getMinY());
//	}

	/**
	 * Method to return a Poly that describes the location of a port
	 * on this NodeInst.
	 * @param thePort the port on this NodeInst.
	 * @return a Poly that describes the location of the Export.
	 * The Poly is transformed to account for rotation on this NodeInst.
	 */
	public Poly getShapeOfPort(PortProto thePort)
	{
		NodeInst ni = this;
		PortProto pp = thePort;

		// look down to the bottom level node/port
		AffineTransform trans = ni.rotateOut();
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
	 * Method to return the "outline" information on this NodeInst.
	 * Outline information is a set of coordinate points that further
	 * refines the NodeInst description.  It is typically used in
	 * Artwork primitives to give them a precise shape.  It is also
	 * used by pure-layer nodes in all layout technologies to allow
	 * them to take any shape.  It is even used by many MOS
	 * transistors to allow a precise gate path to be specified.
	 * @return an array of Floats, organized as X/Y/X/Y.
	 */
	public Float [] getTrace()
	{
		Variable var = getVar(TRACE, Float[].class);
		if (var == null) return null;
		Object obj = var.getObject();
		if (obj instanceof Object[]) return (Float []) obj;
		return null;
	}

	/****************************** PORTS ******************************/

	/**
	 * Method to return an Iterator for all PortInsts on this NodeInst.
	 * @return an Iterator for all PortInsts on this NodeInst.
	 */
	public Iterator getPortInsts()
	{
		return portInsts.iterator();
	}

	/**
	 * Method to return the number of PortInsts on this NodeInst.
	 * @return the number of PortInsts on this NodeInst.
	 */
	public int getNumPortInsts()
	{
		return portInsts.size();
	}

	/**
	 * Method to return the PortInst at specified position.
	 * @param portIndex specified position of PortInst.
	 * @return the PortProto at specified position..
	 */
	public PortInst getPortInst(int portIndex)
	{
		return (PortInst)portInsts.get(portIndex);
	}

	/**
	 * Method to return the only PortInst on this NodeInst.
	 * This is quite useful for vias and pins which have only one PortInst.
	 * @return the only PortInst on this NodeInst.
	 * If there are more than 1 PortInst, then return null.
	 */
	public PortInst getOnlyPortInst()
	{
		int sz = portInsts.size();
		if (sz != 1)
		{
			System.out.println("NodeInst.getOnlyPort: there isn't exactly one port: " + sz);
			return null;
		}
		return (PortInst) portInsts.get(0);
	}

	/**
	 * Method to return the named PortInst on this NodeInst.
	 * @param name the name of the PortInst.
	 * @return the selected PortInst.  If the name is not found, return null.
	 */
	public PortInst findPortInst(String name)
	{
		PortProto pp = protoType.findPortProto(name);
		if (pp == null) return null;
		return (PortInst) portInsts.get(pp.getPortIndex());
	}

	/**
	 * Method to return the PortInst on this NodeInst that is closest to a point.
	 * @param w the point of interest.
	 * @return the closest PortInst to that point.
	 */
	public PortInst findClosestPortInst(Point2D w)
	{
		double bestDist = Double.MAX_VALUE;
		PortInst bestPi = null;
		for (int i = 0; i < portInsts.size(); i++)
		{
			PortInst pi = (PortInst) portInsts.get(i);
			Poly piPoly = pi.getPoly();
			Point2D piPt = new Point2D.Double(piPoly.getCenterX(), piPoly.getCenterY());
			double thisDist = piPt.distance(w);
			if (thisDist < bestDist)
			{
				bestDist = thisDist;
				bestPi = pi;
			}
		}
		return bestPi;
	}

	/**
	 * Method to return the Portinst on this NodeInst with a given prototype.
	 * @param pp the PortProto to find.
	 * @return the selected PortInst.  If the PortProto is not found,
	 * return null.
	 */
	public PortInst findPortInstFromProto(PortProto pp)
	{
		return (PortInst) portInsts.get(pp.getPortIndex());
	}

	/**
	 * Method to create a new PortInst on this NodeInst.
	 * @param pp the prototype of the new PortInst.
	 */
	public void addPortInst(PortProto pp)
	{
		PortInst pi = PortInst.newInstance(pp, this);
		portInsts.add(pp.getPortIndex(), pi);
	}

	/**
	 * Method to delete a new PortInst from this NodeInst.
	 * @param pp the prototype of the PortInst to remove.
	 */
	public void removePortInst(PortProto pp)
	{
		PortInst pi = (PortInst) portInsts.get(pp.getPortIndex());

		// kill the arcs attached to the connections to this port instance.
		// This will also remove the connections themselves
		for (int i = connections.size() - 1; i >= 0; i--)
		{
			Connection con = (Connection)connections.get(i);
			if (con.getPortInst() == pi) con.getArc().kill();
			
		}

		// remove connected exports
		for (int i = exports.size() - 1; i >= 0; i--)
		{
			Export export = (Export)exports.get(i);
			if (export.getOriginalPort() == pi) export.kill();
		}

		portInsts.remove(pp.getPortIndex());
	}

    /** 
     * Method to get the Schematic Cell from a NodeInst icon
     * @return the equivalent view of the prototype, or null if none
     * (such as for primitive)
     */
    public Cell getProtoEquivalent()
    {
        if (!(protoType instanceof Cell)) return null;            // primitive
        return ((Cell)protoType).getEquivalent();
    }

	/**
	 * Method to add an Export to this NodeInst.
	 * @param e the Export to add.
	 */
	public void addExport(Export e)
	{
		exports.add(e);
		redoGeometric();
	}

	/**
	 * Method to remove an Export from this NodeInst.
	 * @param e the Export to remove.
	 */
	public void removeExport(Export e)
	{
		if (!exports.contains(e))
		{
			throw new RuntimeException("Tried to remove a non-existant export");
		}
		exports.remove(e);
		redoGeometric();
	}

	/**
	 * Method to return an Iterator over all Exports on this NodeInst.
	 * @return an Iterator over all Exports on this NodeInst.
	 */
	public Iterator getExports()
	{
		return exports.iterator();
	}

	/**
	 * Method to return the number of Exports on this NodeInst.
	 * @return the number of Exports on this NodeInst.
	 */
	public int getNumExports() { return exports.size(); }

	/**
	 * Method to associate the ports on this NodeInst with another.
	 * @param niOther the other NodeInst to associate with this.
	 * @param ignorePortNames true to ignore port names and use only positions.
	 * @return an array of PortAssociation objects that associates ports on this NodeInst
	 * with those on the other one.  returns null if there is an error.
	 */
	private PortAssociation [] portAssociate(NodeInst ni1, NodeInst ni2, boolean ignorePortNames)
	{
		// gather information about NodeInst 1 (ports, Poly, location, association)
		int total1 = ni1.getProto().getNumPorts();
		PortAssociation [] portInfo1 = new PortAssociation[total1];
		int k = 0;
		for(Iterator it1 = ni1.getPortInsts(); it1.hasNext(); )
		{
			PortInst pi1 = (PortInst)it1.next();
			portInfo1[k] = new PortAssociation();
			portInfo1[k].portInst = pi1;
			portInfo1[k].poly = pi1.getPoly();
			portInfo1[k].pos = new Point2D.Double(portInfo1[k].poly.getCenterX(), portInfo1[k].poly.getCenterY());
			portInfo1[k].assn = null;
			k++;
		}

		// gather information about NodeInst 2 (ports, Poly, location, association)
		int total2 = ni2.getProto().getNumPorts();
		PortAssociation [] portInfo2 = new PortAssociation[total2];
		k = 0;
		for(Iterator it2 = ni2.getPortInsts(); it2.hasNext(); )
		{
			PortInst pi2 = (PortInst)it2.next();
			portInfo2[k] = new PortAssociation();
			portInfo2[k].portInst = pi2;
			portInfo2[k].poly = pi2.getPoly();
			portInfo2[k].pos = new Point2D.Double(portInfo2[k].poly.getCenterX(), portInfo2[k].poly.getCenterY());
			portInfo2[k].assn = null;
			k++;
		}

		// associate on port name matches
		if (!ignorePortNames)
		{
			for(int i1 = 0; i1 < total1; i1++)
			{
				PortInst pi1 = portInfo1[i1].portInst;
				for(int i2 = 0; i2 < total2; i2++)
				{
					PortInst pi2 = portInfo2[i2].portInst;
					if (portInfo2[i2].assn != null) continue;

					// stop if the ports have different name
					if (!pi2.getPortProto().getProtoName().equalsIgnoreCase(pi1.getPortProto().getProtoName())) continue;

					// store the correct association of ports
					portInfo1[i1].assn = pi2;
					portInfo2[i2].assn = pi1;
				}
			}
		}

		// make two passes, the first stricter
		for(int pass=0; pass<2; pass++)
		{
			// associate ports that are in the same position
			for(int i1 = 0; i1 < total1; i1++)
			{
				PortInst pi1 = portInfo1[i1].portInst;
				if (portInfo1[i1].assn != null) continue;

				for(int i2 = 0; i2 < total2; i2++)
				{
					// if this port is already associated, ignore it
					PortInst pi2 = portInfo2[i2].portInst;
					if (portInfo2[i2].assn != null) continue;

					// if the port centers are different, go no further
					if (portInfo2[i2].pos.getX() != portInfo1[i1].pos.getX() ||
						portInfo2[i2].pos.getY() != portInfo1[i1].pos.getY()) continue;

					// compare actual polygons to be sure
					if (pass == 0)
					{
						if (!portInfo1[i1].poly.polySame(portInfo2[i2].poly)) continue;
					}

					// handle confusion if multiple ports have the same polygon
//					if (assn[i1] != null)
//					{
//						PortProto mpt = assn[i1];
//
//						// see if one of the associations has the same connectivity
//						for(j=0; mpt->connects[j] != NOARCPROTO && pp1->connects[j] != NOARCPROTO; j++)
//							if (mpt->connects[j] != pp1->connects[j]) break;
//						if (mpt->connects[j] == NOARCPROTO && pp1->connects[j] == NOARCPROTO) continue;
//					}

					// store the correct association of ports
					portInfo1[i1].assn = pi2;
					portInfo2[i2].assn = pi1;
				}
			}
		}
		return portInfo1;
	}

	/****************************** CONNECTIONS ******************************/

	/**
	 * sanity check function, used by Connection.checkobj
	 */
	boolean containsConnection(Connection c)
	{
		return connections.contains(c);
	}

	/**
	 * Method to recomputes the "Wiped" flag bit on this NodeInst.
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
	 * Method to determine whether the display of this pin NodeInst should be supressed.
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
	 * Method to tell whether this NodeInst is a pin that is "inline".
	 * An inline pin is one that connects in the middle between two arcs.
	 * The arcs must line up such that the pin can be removed and the arcs replaced with a single
	 * arc that is in the same place as the former two.
	 * @return true if this NodeInst is an inline pin.
	 */
	public boolean isInlinePin()
	{
		if (protoType.getFunction() != NodeProto.Function.PIN) return false;

		// see if the pin is connected to two arcs along the same slope
		int j = 0;
		ArcInst [] reconAr = new ArcInst[2];
		Point2D [] delta = new Point2D.Double[2];
		for(Iterator it = getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			if (j >= 2) { j = 0;   break; }
			ArcInst ai = con.getArc();
			reconAr[j] = ai;
			Connection thisCon = ai.getHead();
			Connection thatCon = ai.getTail();
			if (thatCon == con)
			{
				thisCon = ai.getTail();
				thatCon = ai.getHead();
			}
			delta[j] = new Point2D.Double(thatCon.getLocation().getX() - thisCon.getLocation().getX(),
				thatCon.getLocation().getY() - thisCon.getLocation().getY());
			j++;
		}
		if (j != 2) return false;

		// must connect to two arcs of the same type and width
		if (reconAr[0].getProto() != reconAr[1].getProto()) return false;
		if (reconAr[0].getWidth() != reconAr[1].getWidth()) return false;

		// arcs must be along the same angle, and not be curved
		if (delta[0].getX() != 0 || delta[0].getY() != 0 || delta[1].getX() != 0 || delta[1].getY() != 0)
		{
			Point2D zero = new Point2D.Double(0, 0);
			if ((delta[0].getX() != 0 || delta[0].getY() != 0) && (delta[1].getX() != 0 || delta[1].getY() != 0) &&
				EMath.figureAngle(zero, delta[0]) !=
				EMath.figureAngle(delta[1], zero)) return false;
		}
		if (reconAr[0].getVar("ARC_radius") != null) return false;
		if (reconAr[1].getVar("ARC_radius") != null) return false;

		// the arcs must not have network names on them
		Name name0 = reconAr[0].getNameKey();
		Name name1 = reconAr[1].getNameKey();
		if (name0 != null && name1 != null)
		{
			if (!name0.isTempname() && !name1.isTempname()) return false;
		}
		return true;
	}

	/**
	 * Method to tell whether this NodeInst can connect to a given ArcProto.
	 * @param arc the type of arc to test for.
	 * @return the first port that can connect to this node, or
	 * null, if no such port on this node exists.
	 */
	public PortProto connectsTo(ArcProto arc)
	{
		return protoType.connectsTo(arc);
	}

	/**
	 * Method to add an Connection to this NodeInst.
	 * @param c the Connection to add.
	 */
	void addConnection(Connection c)
	{
		connections.add(c);
		NodeInst ni = c.getPortInst().getNodeInst();
		ni.computeWipeState();
		redoGeometric();
	}

	/**
	 * Method to remove an Connection from this NodeInst.
	 * @param c the Connection to remove.
	 */
	void removeConnection(Connection c)
	{
		connections.remove(c);
		NodeInst ni = c.getPortInst().getNodeInst();
		ni.computeWipeState();
		redoGeometric();
	}

	/**
	 * Method to return an Iterator over all Connections on this NodeInst.
	 * @return an Iterator over all Connections on this NodeInst.
	 */
	public Iterator getConnections()
	{
		return connections.iterator();
	}

	/**
	 * Method to return the number of Connections on this NodeInst.
	 * @return the number of Connections on this NodeInst.
	 */
	public int getNumConnections() { return connections.size(); }

	/****************************** TEXT ******************************/

	/**
	 * Method to return the Text Descriptor associated with this NodeInst.
	 * The only NodeInsts that need Text Descriptors are instances of Cells that are unexpanded.
	 * In this situation, the Cell instance is drawn as a box with a name.
	 * The Text Descriptor applies to the display of that name.
	 * @return the Text Descriptor for this NodeInst.
	 */
	public TextDescriptor getProtoTextDescriptor() { return protoDescriptor; }

	/**
	 * Method to set the Text Descriptor associated with this NodeInst.
	 * The only NodeInsts that need Text Descriptors are instances of Cells that are unexpanded.
	 * In this situation, the Cell instance is drawn as a box with a name.
	 * The Text Descriptor applies to the display of that name.
	 * @param descriptor the Text Descriptor for this NodeInst.
	 */
	public void setProtoTextDescriptor(TextDescriptor descriptor) { this.protoDescriptor.copy(descriptor); }

	/*
	 * Method to write a description of this NodeInst.
	 * Displays the description in the Messages Window.
	 */
//	public void getInfo()
//	{
//		System.out.println("-------------- NODE INSTANCE " + describe() + ": --------------");
//		String xMir = "";
//		String yMir = "";
//		if (isXMirrored()) xMir = ", MirrorX";
//		if (isYMirrored()) yMir = ", MirrorY";
//		System.out.println(" Center: (" + getCenterX() + "," + getCenterY() + "), size: " + getXSize() + "x" + getYSize() + ", rotated " + angle/10.0 +
//			xMir + yMir);
//		super.getInfo();
//		System.out.println(" Ports:");
//		for(Iterator it = getPortInsts(); it.hasNext();)
//		{
//			PortInst pi = (PortInst) it.next();
//			Poly p = pi.getPoly();
//			System.out.println("     " + pi.getPortProto().getProtoName() + " at (" + p.getCenterX() + "," + p.getCenterY() + ")");
//		}
//		if (connections.size() != 0)
//		{
//			System.out.println(" Connections:");
//			for (int i = 0; i < connections.size(); i++)
//			{
//				Connection c = (Connection) connections.get(i);
//				System.out.println("     " + c.getArc().describe() +
//					" on port " + c.getPortInst().getPortProto().getProtoName());
//			}
//		}
//		if (exports.size() != 0)
//		{
//			System.out.println(" Exports:");
//			for (int i = 0; i < exports.size(); i++)
//			{
//				Export ex = (Export) exports.get(i);
//				System.out.println("     " + ex.getProtoName() +
//					" on port " + ex.getOriginalPort().getPortProto().getProtoName() +
//					", " + ex.getCharacteristic());
//			}
//		}
//	}

	/**
	 * Method to determine whether a variable key on NodeInst is deprecated.
	 * Deprecated variable keys are those that were used in old versions of Electric,
	 * but are no longer valid.
	 * @param key the key of the variable.
	 * @return true if the variable key is deprecated.
	 */
	public boolean isDeprecatedVariable(Variable.Key key) { return key == NODE_NAME; }

	/**
	 * Method to determine whether this is an Invisible Pin with text.
	 * If so, it should not be selected, but its text should be instead.
	 * @return true if this is an Invisible Pin with text.
	 */
	public boolean isInvisiblePinWithText()
	{
		if (getProto() != Generic.tech.invisiblePinNode) return false;
		if (getNumExports() != 0) return true;
		if (numDisplayableVariables(false) != 0) return true;
		return false;
	}

	/*
	 * Method to tell if this NodeInst is an invisible-pin with text that is offset away from the pin center.
	 * Since invisible pins with text are never shown, their text should not be offset.
	 * @return the coordinates of the pin, if it has offset text.
	 * Returns null if the pin is valid (or if it isn't a pin or doesn't have text).
	 */
	public Point2D invisiblePinWithOffsetText(boolean repair)
	{
		// look for pins that are invisible and have text in different location
		if (protoType.getFunction() != NodeProto.Function.PIN) return null;
		if (this.getNumConnections() != 0) return null;

		// stop now if this isn't invisible
		if (protoType != Generic.tech.invisiblePinNode)
		{
			Technology tech = protoType.getTechnology();
			Poly [] polyList = tech.getShapeOfNode(this, null);
			if (polyList.length > 0)
			{
				Poly.Type style = polyList[0].getStyle();
				if (style != Poly.Type.TEXTCENT && style != Poly.Type.TEXTTOP &&
					style != Poly.Type.TEXTBOT && style != Poly.Type.TEXTLEFT &&
					style != Poly.Type.TEXTRIGHT && style != Poly.Type.TEXTTOPLEFT &&
					style != Poly.Type.TEXTBOTLEFT && style != Poly.Type.TEXTTOPRIGHT &&
					style != Poly.Type.TEXTBOTRIGHT && style != Poly.Type.TEXTBOX) return null;
			}
		}

		// invisible: look for offset text
		for(Iterator it = getExports(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			TextDescriptor td = pp.getTextDescriptor();
			if (td.getXOff() != 0 || td.getYOff() != 0)
			{
				Point2D retVal = new Point2D.Double(getGrabCenterX() + td.getXOff(), getGrabCenterY() +td.getYOff());
//				if (repair)
//				{
//					(void)startobjectchange((INTBIG)ni, VNODEINST);
//					TDSETOFF(pp->textdescript, 0, 0);
//					(void)setind((INTBIG)pp, VPORTPROTO, x_("textdescript"), 0, pp->textdescript[0]);
//					(void)setind((INTBIG)pp, VPORTPROTO, x_("textdescript"), 1, pp->textdescript[1]);
//					(void)endobjectchange((INTBIG)ni, VNODEINST);
//				}
				return retVal;
			}
		}

		for(Iterator it = this.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			TextDescriptor td = var.getTextDescriptor();
			if (var.isDisplay() && (td.getXOff() != 0 || td.getYOff() != 0))
			{
				Point2D retVal = new Point2D.Double(getGrabCenterX() + td.getXOff(), getGrabCenterY() +td.getYOff());
//				if (repair)
//				{
//					(void)startobjectchange((INTBIG)ni, VNODEINST);
//					TDSETOFF(var->textdescript, 0, 0);
//					modifydescript((INTBIG)ni, VNODEINST, var, var->textdescript);
//					(void)endobjectchange((INTBIG)ni, VNODEINST);
//				}
				return retVal;
			}
		}
		return null;
	}

	/**
	 * Method to describe this NodeInst as a string.
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

	/****************************** MISCELLANEOUS ******************************/

	/**
	 * Method to return the prototype of this NodeInst.
	 * @return the prototype of this NodeInst.
	 */
	public NodeProto getProto() { return protoType; }

	/**
	 * Method to return the function of this NodeProto.
	 * The Function is a technology-independent description of the behavior of this NodeProto.
	 * @return function the function of this NodeProto.
	 */
	public NodeProto.Function getFunction()
	{
		if (protoType instanceof Cell) return NodeProto.Function.UNKNOWN;

		PrimitiveNode np = (PrimitiveNode)protoType;
		return np.getTechnology().getPrimitiveFunction(this);
	}

    /** 
     * Method to see if this NodeInst is a Primitive Transistor.
     * Use getFunction() to determine what specific transitor type it is,
     * if any.
     * @return true if NodeInst represents Primitive Transistor
     */
    public boolean isPrimitiveTransistor()
    {
        NodeProto.Function func = protoType.getFunction(); // note bypasses ni.getFunction() call
        if (func == NodeProto.Function.TRANS ||         // covers all Schematic trans
            func == NodeProto.Function.TRANS4 ||        // covers all Schematic trans4
            func == NodeProto.Function.TRANMOS ||       // covers all MoCMOS nmos gates
            func == NodeProto.Function.TRAPMOS          // covers all MoCMOS pmos gates
            )
            return true;
        return false;
    }
   
	/*
	 * Method to tell whether this NodeInst is a field-effect transtor.
	 * This includes the nMOS, PMOS, and DMOS transistors, as well as the DMES and EMES transistors.
	 * @return true if this NodeInst is a field-effect transtor.
	 */
	public boolean isFET()
	{
		NodeProto.Function fun = getFunction();
		if (fun == NodeProto.Function.TRANMOS || fun == NodeProto.Function.TRA4NMOS ||
			fun == NodeProto.Function.TRAPMOS || fun == NodeProto.Function.TRA4PMOS ||
			fun == NodeProto.Function.TRADMOS || fun == NodeProto.Function.TRA4DMOS ||
			fun == NodeProto.Function.TRADMES || fun == NodeProto.Function.TRA4DMES ||
			fun == NodeProto.Function.TRAEMES || fun == NodeProto.Function.TRA4EMES)
				return true;
		return false;
	}

	/**
	 * Method to return the size of this transistor NodeInst.
     * @param context the VarContext in which any evaluations take place,
     * pass in VarContext.globalContext if no context needed.
	 * @return the size of the NodeInst.
	 */
	public Dimension getTransistorSize(VarContext context)
	{
		PrimitiveNode np = (PrimitiveNode)protoType;
		return np.getTechnology().getTransistorSize(this, context);
	}

    /**
     * Method to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.
     * @return a PortInst for the gate of the transistor
     */
    public PortInst getTransistorGatePort()
    {
		PrimitiveNode np = (PrimitiveNode)protoType;
		return np.getTechnology().getTransistorGatePort(this);
    }
    
    /**
     * Method to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.
     * @return a PortInst for the gate of the transistor
     */
    public PortInst getTransistorSourcePort()
    {
		PrimitiveNode np = (PrimitiveNode)protoType;
		return np.getTechnology().getTransistorSourcePort(this);
    }
    
    /**
     * Method to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.
     * @return a PortInst for the gate of the transistor
     */
    public PortInst getTransistorDrainPort()
    {
		PrimitiveNode np = (PrimitiveNode)protoType;
		return np.getTechnology().getTransistorDrainPort(this);
    }

	/**
	 * Method to check and repair data structure errors in this NodeInst.
	 */
	public int checkAndRepair()
	{
		int errorCount = 0;
		if (portInsts.size() != protoType.getNumPorts())
		{
			System.out.println("Library " + parent.getLibrary().getLibName() +
				", cell " + parent.describe() + ", node " + describe() +
				" has number of PortInsts " + portInsts.size() + " , but prototype " + protoType +
				" has " + protoType.getNumPorts() + " ports");
			return 1;
		}
		int i = 0;
		for (Iterator it = protoType.getPorts(); it.hasNext(); i++)
		{
			PortProto pp = (PortProto)it.next();
			PortInst pi = (PortInst)portInsts.get(i);
			if (pp.getPortIndex() != i || pi.getPortProto() != pp)
			{
 				System.out.println("Library " + parent.getLibrary().getLibName() +
 					", cell " + parent.describe() + ", node " + describe() +
 					" has mismatches between PortInsts and PortProtos (" + pp.getProtoName() + ")");
				errorCount++;
			}
		}
		return errorCount;
	}
// 	{
// 		int errorCount = 0;

// 		// make sure there is a PortInst for every PortProto
// 		FlagSet fs = PortProto.getFlagSet(1);
// 		for(Iterator it = protoType.getPorts(); it.hasNext(); )
// 		{
// 			PortProto pp = (PortProto)it.next();
// 			pp.clearBit(fs);
// 		}
// 		for(Iterator it = getPortInsts(); it.hasNext(); )
// 		{
// 			PortInst pi = (PortInst)it.next();
// 			PortProto pp = pi.getPortProto();
// 			if (pp.isBit(fs))
// 			{
// 				System.out.println("Library " + parent.getLibrary().getLibName() +
// 					", cell " + parent.describe() + ", node " + describe() +
// 					" has multiple PortInsts pointing to the same PortProto (" + pp.getProtoName() + ")");
// 				errorCount++;
// 			}
// 			pp.setBit(fs);
// 		}
// 		for(Iterator it = protoType.getPorts(); it.hasNext(); )
// 		{
// 			PortProto pp = (PortProto)it.next();
// 			if (!pp.isBit(fs))
// 			{
// 				System.out.println("Library " + parent.getLibrary().getLibName() +
// 					", cell " + parent.describe() + ", node " + describe() +
// 					" port " + pp.getProtoName() + " has no PortInst");
// 				errorCount++;
// 			}
// 		}
// 		fs.freeFlagSet();
// 		return errorCount;
// 	}

	/**
	 * Returns the basename for autonaming.
	 * @return the basename for autonaming.
	 */
	private Name getBasename()
	{
		return protoType instanceof Cell ? ((Cell)protoType).getBasename() : getFunction().getBasename();
	}

	/**
	 * Method to return the NodeUsage of this NodeInst.
	 * @return the NodeUsage of this NodeInst.
	 */
	public NodeUsage getNodeUsage() { return nodeUsage; }

	/**
	 * Method to set this NodeInst to be expanded.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * Unexpanded Cell instances are shown as boxes with the node prototype names in them.
	 * The state has no meaning for instances of primitive node prototypes.
	 */
	public void setExpanded() { userBits |= NEXPAND; }

	/**
	 * Method to set this NodeInst to be unexpanded.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * Unexpanded Cell instances are shown as boxes with the node prototype names in them.
	 * The state has no meaning for instances of primitive node prototypes.
	 */
	public void clearExpanded() { userBits &= ~NEXPAND; }

	/**
	 * Method to tell whether this NodeInst is expanded.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * Unexpanded Cell instances are shown as boxes with the node prototype names in them.
	 * The state has no meaning for instances of primitive node prototypes.
	 * @return true if this NodeInst is expanded.
	 */
	public boolean isExpanded() { return (userBits & NEXPAND) != 0; }

	/**
	 * Method to set this NodeInst to be wiped.
	 * Wiped NodeInsts are erased.  Typically, pin NodeInsts can be wiped.
	 * This means that when an arc connects to the pin, it is no longer drawn.
	 * In order for a NodeInst to be wiped, its prototype must have the "setArcsWipe" state,
	 * and the arcs connected to it must have "setWipable" in their prototype.
	 * @see NodeProto#setArcsWipe
	 * @see ArcProto#setWipable
	 */
	public void setWiped() { userBits |= WIPED; }

	/**
	 * Method to set this NodeInst to be not wiped.
	 * Wiped NodeInsts are erased.  Typically, pin NodeInsts can be wiped.
	 * This means that when an arc connects to the pin, it is no longer drawn.
	 * In order for a NodeInst to be wiped, its prototype must have the "setArcsWipe" state,
	 * and the arcs connected to it must have "setWipable" in their prototype.
	 * @see NodeProto#setArcsWipe
	 * @see ArcProto#setWipable
	 */
	public void clearWiped() { userBits &= ~WIPED; }

	/**
	 * Method to tell whether this NodeInst is wiped.
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
	 * Method to set this NodeInst to be shortened.
	 * Shortened NodeInst have been reduced in size to account for the fact that
	 * they are connected at nonManhattan angles and must connect smoothly.
	 * This state can only get set if the node's prototype has the "setCanShrink" state.
	 */
	public void setShortened() { userBits |= NSHORT; }

	/**
	 * Method to set this NodeInst to be not shortened.
	 * Shortened NodeInst have been reduced in size to account for the fact that
	 * they are connected at nonManhattan angles and must connect smoothly.
	 * This state can only get set if the node's prototype has the "setCanShrink" state.
	 */
	public void clearShortened() { userBits &= ~NSHORT; }

	/**
	 * Method to tell whether this NodeInst is shortened.
	 * Shortened NodeInst have been reduced in size to account for the fact that
	 * they are connected at nonManhattan angles and must connect smoothly.
	 * This state can only get set if the node's prototype has the "setCanShrink" state.
	 * @return true if this NodeInst is shortened.
	 */
	public boolean isShortened() { return (userBits & NSHORT) != 0; }

	/**
	 * Method to set this NodeInst to be hard-to-select.
	 * Hard-to-select NodeInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 */
	public void setHardSelect() { userBits |= HARDSELECTN; }

	/**
	 * Method to set this NodeInst to be easy-to-select.
	 * Hard-to-select NodeInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 */
	public void clearHardSelect() { userBits &= ~HARDSELECTN; }

	/**
	 * Method to tell whether this NodeInst is hard-to-select.
	 * Hard-to-select NodeInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 * @return true if this NodeInst is hard-to-select.
	 */
	public boolean isHardSelect() { return (userBits & HARDSELECTN) != 0; }

	/**
	 * Method to set this NodeInst to be visible-inside.
	 * A NodeInst that is "visible inside" is only drawn when viewing inside of the Cell.
	 * It is not visible from outside (meaning from higher-up the hierarchy).
	 */
	public void setVisInside() { userBits |= NVISIBLEINSIDE; }

	/**
	 * Method to set this NodeInst to be not visible-inside.
	 * A NodeInst that is "visible inside" is only drawn when viewing inside of the Cell.
	 * It is not visible from outside (meaning from higher-up the hierarchy).
	 */
	public void clearVisInside() { userBits &= ~NVISIBLEINSIDE; }

	/**
	 * Method to tell whether this NodeInst is visible-inside.
	 * A NodeInst that is "visible inside" is only drawn when viewing inside of the Cell.
	 * It is not visible from outside (meaning from higher-up the hierarchy).
	 * @return true if this NodeInst is visible-inside.
	 */
	public boolean isVisInside() { return (userBits & NVISIBLEINSIDE) != 0; }

	/**
	 * Method to set this NodeInst to be locked.
	 * Locked NodeInsts cannot be modified or deleted.
	 */
	public void setLocked() { userBits |= NILOCKED; }

	/**
	 * Method to set this NodeInst to be unlocked.
	 * Locked NodeInsts cannot be modified or deleted.
	 */
	public void clearLocked() { userBits &= ~NILOCKED; }

	/**
	 * Method to tell whether this NodeInst is locked.
	 * Locked NodeInsts cannot be modified or deleted.
	 * @return true if this NodeInst is locked.
	 */
	public boolean isLocked() { return (userBits & NILOCKED) != 0; }

	/**
	 * Method to set a Technology-specific value on this NodeInst.
	 * This is mostly used by the Schematics technology which allows variations
	 * on a NodeInst to be stored.
	 * For example, the Transistor primitive uses these bits to distinguish nMOS, pMOS, etc.
	 * @param value the Technology-specific value to store on this NodeInst.
	 */
	public void setTechSpecific(int value) { userBits = (userBits & ~NTECHBITS) | (value << NTECHBITSSH); }

	/**
	 * Method to return the Technology-specific value on this NodeInst.
	 * This is mostly used by the Schematics technology which allows variations
	 * on a NodeInst to be stored.
	 * For example, the Transistor primitive uses these bits to distinguish nMOS, pMOS, etc.
	 * @return the Technology-specific value on this NodeInst.
	 */
	public int getTechSpecific() { return (userBits & NTECHBITS) >> NTECHBITSSH; }

	/**
	 * Method to tell whether this NodeInst is mirrored in the X axis.
	 * Mirroring in the X axis implies that X coordinates are negated.
	 * Thus, it is equivalent to mirroring ABOUT the Y axis.
	 * @return true if this NodeInst is mirrored in the X axis.
	 */
	public boolean isXMirrored() { return sX < 0; }

	/**
	 * Method to tell whether this NodeInst is mirrored in the Y axis.
	 * Mirroring in the Y axis implies that Y coordinates are negated.
	 * Thus, it is equivalent to mirroring ABOUT the X axis.
	 * @return true if this NodeInst is mirrored in the Y axis.
	 */
	public boolean isYMirrored() { return sY < 0; }

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
		at.setToTranslation(getGrabCenterX(), getGrabCenterY());
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
	 * not preserve right angles this method will print a warning
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

		modifyInstance(centX-getGrabCenterX(), centY-getGrabCenterY(), sizeX-sX,
					   sizeY-sY, (int)Math.round(newAngle*10)-angle);
	}

	/**
	 * Return the Essential Bounds of this NodeInst.
	 *
	 * <p>If this is a NodeInst of a Cell, and if that Cell has
	 * Essential Bounds, then map that Cell's Essential Bounds into
	 * the coordinate space of the Cell that contains this NodeInst,
	 * and return the Rectangle2D that contains those
	 * bounds. Otherwise return null.
	 * @return the Rectangle2D containing the essential bounds or null
	 * if the essential bounds don't exist.
	 */
	public Rectangle2D findEssentialBounds() 
	{
		NodeProto np = getProto();
		if (!(np instanceof Cell)) return null;
		Rectangle2D eb = ((Cell)np).findEssentialBounds();
		if (eb==null)  return null;
		AffineTransform xForm = translateOut();
		Point2D ll = new Point2D.Double(eb.getMinX(), eb.getMinY());
		ll = xForm.transform(ll, null);
		Point2D ur = new Point2D.Double(eb.getMaxX(), eb.getMaxY());
		ur = xForm.transform(ur, null);
		double minX = Math.min(ll.getX(), ur.getX());
		double minY = Math.min(ll.getY(), ur.getY());
		double maxX = Math.max(ll.getX(), ur.getX());
		double maxY = Math.max(ll.getY(), ur.getY());
		return new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
	}
    
}
