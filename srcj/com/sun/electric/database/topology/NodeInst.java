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
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.NodeInstProxy;
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
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
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

	// Name of Variable holding instance name.
	public static final String NODE_NAME = "NODE_name";

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

	/**
	 * the Subinst class is used when icon node has arrayed name.
	 */
	public class Subinst
	{
		/** index in icon NodeInst. */							int index;
		/** NodeInstProxy of this subinstance */				NodeInstProxy proxy;

		Subinst(int index) { this.index = index; }

		public Name getName() { return name != null ? name.subname(index) : null; }
		public NodeInstProxy getProxy() { return proxy; }
		public void setProxy(NodeInstProxy proxy) { this.proxy = proxy; }
	}

	// ---------------------- private data ----------------------------------
	/** prototype of this node instance */					private NodeProto protoType;
	/** node usage of this node instance */					private NodeUsage nodeUsage;
	/** labling information for this node instance */		private int textbits;
	/** HashTable of portInsts on this node instance */		private HashMap portMap;
	/** List of connections belonging to this instance */	private List connections;
	/** List of Exports belonging to this node instance */	private List exports;
	/** Text descriptor of prototype name */				private TextDescriptor protoDescriptor;
	/** array of subinstances of icon node instance */		private Subinst[] subs;

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
		this.protoDescriptor = TextDescriptor.newInstanceDescriptor();
	}

	/****************************** CREATE, DELETE, MODIFY ******************************/

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
	 * @param name name of new NodeInst
	 * @return the newly created NodeInst, or null on error.
	 */
	public static NodeInst newInstance(NodeProto protoType, Point2D center, double width, double height,
		int angle, Cell parent, String name)
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
		if (name != null) ni.setName(name);
		if (ni.lowLevelLink()) return null;

		// handle change control, constraint, and broadcast
		if (Undo.recordChange())
		{
			// tell all tools about this NodeInst
			Undo.Change ch = Undo.newChange(ni, Undo.Type.NODEINSTNEW);

			// tell constraint system about new NodeInst
			Constraint.getCurrent().newObject(ni);
		}
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
		// make the change
		if (Undo.recordChange())
		{
			Constraint.getCurrent().modifyNodeInsts(nis, dXs, dYs, dXSizes, dYSizes, dRots);
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
			if (nis[i].getProto() instanceof PrimitiveNode && nis[i].getProto() == Generic.tech.cellCenterNode)
			{
				nis[i].getParent().adjustReferencePoint(nis[i]);
			}
		}
	}

	/*
	 * Routine to replace this NodeInst with one of another type.
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
		Point2D oldCenter = getTrueCenter();

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
		NodeInst newNi = NodeInst.newInstance(np, new Point2D.Double(0, 0), newXS, newYS, getAngle(), getParent(), null);
		if (newNi == null) return null;

		// adjust position of the new NodeInst to align centers
		Point2D newCenter = newNi.getTrueCenter();
		newNi.modifyInstance(oldCenter.getX()-newCenter.getX(), oldCenter.getY()-newCenter.getY(), 0, 0, 0);

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
	public boolean lowLevelPopulate(NodeProto protoType, Point2D center, double width, double height, int angle,
		Cell parent)
	{
		setParent(parent);
		this.protoType = protoType;
		if (protoType instanceof Cell)
		{
			Cell protoCell = (Cell)protoType;
			if (protoCell.getView() == View.ICON)
			{
				subs = new Subinst[1];
				subs[0] = new Subinst(0);
			}
		}

		// create all of the portInsts on this node inst
		int i = 0;
		for (Iterator it = protoType.getPorts(); it.hasNext();)
		{
			PortProto pp = (PortProto) it.next();
			PortInst pi = PortInst.newInstance(pp, this);
			pi.setIndex(i++);
			portMap.put(pp.getProtoName(), pi);
		}

		// enumerate the port instances
//		int i = 0;
//		for(Iterator it = getPortInsts(); it.hasNext();)
//		{
//			PortInst pi = (PortInst) it.next();
//			pi.setIndex(i++);
//		}

		this.center.setLocation(center);
		this.sX = width;   this.sY = height;
		this.angle = angle;

		// fill in the geometry
		Geometric();
		return false;
	}

	/**
	 * Low-level access routine to link the NodeInst into its Cell.
	 * @return true on error.
	 */
	public boolean lowLevelLink()
	{
		if (isUsernamed())
		{
			if (!parent.isUniqueName(name, getClass(), this))
			{
				System.out.println("NodeInst "+name+" already exists in "+parent);
				return true;
			}
		} else
		{
			if (getName() == null || !parent.isUniqueName(name, getClass(), this))
				if (setNameLow(parent.getAutoname(getBasename()))) return true;
		}

		// add to linked lists
		linkGeom(parent);
		nodeUsage = parent.addNode(this);
		parent.addNodables(this, null);
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
		parent.removeNodables(this, null);
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
	public void lowLevelModify(double dX, double dY, double dXSize, double dYSize, int dRot)
	{
		// remove from the R-Tree structure
		unLinkGeom(parent);

		// make the change
		this.center.setLocation(EMath.smooth(getCenterX() + dX), EMath.smooth(getCenterY() + dY));

		this.sX = EMath.smooth(this.sX + dXSize);
		this.sY = EMath.smooth(this.sY + dYSize);

		this.angle += dRot;

		// fill in the Geometric fields
		Geometric();

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

	/****************************** GRAPHICS ******************************/

	/**
	 * Routine to return the true center of this NodeInst.
	 * For primitives, this is the same as the geometric center.
	 * For cell instances, this is the location of the cell-center inside of the cell definition.
	 * @return the true center of this NodeInst.
	 */
	public Point2D getTrueCenter()
	{
		if (getProto() instanceof PrimitiveNode) return center;

		// build a transformation for the center point
		AffineTransform transOut = transformOut();
		AffineTransform trans = rotateOut(transOut);
		Point2D trueCenter = new Point2D.Double(0, 0);
		trans.transform(trueCenter, trueCenter);
		return trueCenter;
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

	/**
	 * Routine to recalculate the Geometric bounds for this NodeInst.
	 */
	void Geometric()
	{
		if (sX == 0 && sY == 0)
		{
			visBounds.setRect(getCenterX(), getCenterY(), 0, 0);
		} else
		{
			// special case for arcs of circles
			if (protoType == Artwork.tech.circleNode || protoType == Artwork.tech.thickCircleNode)
			{
				// see if there this circle is only a partial one */
				double [] angles = getArcDegrees();
				if (angles[0] != 0.0 || angles[1] != 0.0)
				{
					Point2D [] pointList = Artwork.fillEllipse(getCenter(), Math.abs(sX), Math.abs(sY), angles[0], angles[1]);
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
					visBounds.setRect(getCenterX(), getCenterY(), 0, 0);
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
						pointList[i] = new Point2D.Double(getCenterX() + outline[i*2].floatValue(), getCenterY() + outline[i*2+1].floatValue());
					}
					Poly poly = new Poly(pointList);
					poly.setStyle(Poly.Type.OPENED);
					poly.transform(rotateOut());
					visBounds.setRect(poly.getBounds2D());
					return;
				}
			}

			// The old way to do it:
			// start with a unit polygon, centered at the origin
//			Poly poly = new Poly(0.0, 0.0, 1.0, 1.0);
//			AffineTransform scale = new AffineTransform();
//			scale.setToScale(sX, sY);
//			AffineTransform rotate = rotateAbout(angle, 0, 0, sX, sY);
//			AffineTransform translate = new AffineTransform();
//			translate.setToTranslation(getCenterX(), getCenterY());
//			rotate.concatenate(scale);
//			translate.concatenate(rotate);
//			poly.transform(translate);

			// the simpler way to do it:
			Poly poly = new Poly(center.getX(), center.getY(), sX, sY);
			AffineTransform trans = rotateOut();
			poly.transform(trans);

			// return its bounds
			visBounds.setRect(poly.getBounds2D());
		}
	}

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
			TextDescriptor td = getProtoTextDescriptor();
			double offX = (double)td.getXOff() / 4;
			double offY = (double)td.getYOff() / 4;
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
		for(Iterator it = getExports(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			polys[start] = pp.getNamePoly();
			start++;

			// add in variables on the exports
			Poly poly = getShapeOfPort(pp.getOriginalPort().getPortProto());
			int numadded = pp.addDisplayableVariables(poly.getBounds2D(), polys, start, wnd, false);
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
		double dx = getCenterX() - bounds.getCenterX();
		double dy = getCenterY() - bounds.getCenterY();
		AffineTransform transform = new AffineTransform();
		transform.setToRotation(angle, getCenterX(), getCenterY());
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
		double dx = getCenterX() - bounds.getCenterX();
		double dy = getCenterY() - bounds.getCenterY();
		AffineTransform transform = new AffineTransform();
		boolean transpose = sX<0 ^ sY<0;
		if (transpose) {
			transform.translate(getCenterX(), getCenterY());
			transform.scale(1, -1);
			transform.rotate(Math.PI/2);
			transform.translate(-getCenterX(), -getCenterY());
		}
		transform.rotate(angle*Math.PI/1800, getCenterX(), getCenterY());
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
	 * Routine to return a transformation that moves up the hierarchy,
	 * combined with a previous transformation.  Presuming that this
	 * NodeInst is a Cell instance, the transformation goes from the
	 * space of that Cell to the space of this NodeInst's parent Cell.
	 * The transformation includes the rotation of this NodeInst.
	 * @param prevTransform the previous transformation to the
	 * NodeInst's Cell.
	 * @return a transformation that moves up the hierarchy, including
	 * the previous transformation.
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
		// to transform out of this node instance, first translate
		// inner coordinates to outer
		Cell lowerCell = (Cell)protoType;
		Rectangle2D bounds = lowerCell.getBounds();
		double dx = getCenterX() - bounds.getCenterX();
		double dy = getCenterY() - bounds.getCenterY();
		AffineTransform transform = new AffineTransform();
		transform.translate(dx, dy);
		return transform;
	}

	/**
	 * Routine to return a transformation that translates up the
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
	 * Routine to return a transformation that rotates an object about a point.
	 * @param angle the amount to rotate (in tenth-degrees).
	 * @param cX the center X coordinate about which to rotate.
	 * @param cY the center Y coordinate about which to rotate.
	 * @param sX the scale in X (negative to flip the X coordinate, or
	 * flip ABOUT the Y axis).
	 * @param sY the scale in Y (negative to flip the Y coordinate, or
	 * flip ABOUT the X axis).
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
		if (sX < 0) transform.concatenate(mirrorX);
		if (sY < 0) transform.concatenate(mirrorY);
		return transform;
	}

	/**
	 * Routine to return a transformation that rotates this NodeInst.
	 * It transforms points on this NodeInst to account for the
	 * NodeInst's rotation.
	 * @return a transformation that rotates this NodeInst.
	 * If this NodeInst is not rotated, the returned transformation is identity.
	 */
	public AffineTransform rotateOut()
	{
		return rotateAbout(angle, getCenterX(), getCenterY(), sX, sY);
	}

	/**
	 * Routine to return a transformation that rotates this NodeInst,
	 * combined with a previous transformation.  It transforms points
	 * on this NodeInst to account for the NodeInst's rotation.
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
	 * Routine to return the lower-left corner of this NodeInst.  The
	 * corner considers both the highlight offset and the rotation, so
	 * the coordinate is that of the lower-left valid part, as placed
	 * in the database.
	 * @return a Point with the location of this NodeInst's lower-left
	 * corner.
	 */
	public Point2D getLowLeft()
	{
		SizeOffset so = getProto().getSizeOffset();

		double width = getXSize() / 2;
		double height = getYSize() / 2;
		double lX = getCenterX() - width + so.getLowXOffset();
		double hX = getCenterX() + width - so.getHighXOffset();
		double lY = getCenterY() - height + so.getLowYOffset();
		double hY = getCenterY() + height - so.getHighYOffset();
		Poly poly = new Poly((lX+hX)/2, (lY+hY)/2, hX-lX, hY-lY);
		if (getAngle() != 0 || isXMirrored() || isYMirrored())
		{
			AffineTransform trans = this.rotateOut();
			poly.transform(trans);
		}
		Rectangle2D bounds = poly.getBounds2D();
		return new Point2D.Double(bounds.getMinX(), bounds.getMinY());
	}

	/**
	 * Routine to return a Poly that describes the location of a port
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
	 * Routine to return the "outline" information on this NodeInst.
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
		Variable var = getVar("trace", Float[].class);
		if (var == null) return null;
		Object obj = var.getObject();
		if (obj instanceof Object[]) return (Float []) obj;
		return null;
	}

	/****************************** PORTS ******************************/

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
	 * Routine to return the PortInst on this NodeInst that is closest to a point.
	 * @param w the point of interest.
	 * @return the closest PortInst to that point.
	 */
	public PortInst findClosestPortInst(Point2D w)
	{
		double bestDist = Double.MAX_VALUE;
		PortInst bestPi = null;
		for(Iterator it = portMap.values().iterator(); it.hasNext(); )
		{
			PortInst pi = (PortInst)it.next();
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
	 * Routine to return the Portinst on this NodeInst with a given prototype.
	 * @param pp the PortProto to find.
	 * @return the selected PortInst.  If the PortProto is not found,
	 * return null.
	 */
	public PortInst findPortInstFromProto(PortProto pp)
	{
		for(Iterator it = portMap.values().iterator(); it.hasNext(); )
		{
			PortInst pi = (PortInst)it.next();
			if (pi.getPortProto() == pp) return pi;
		}
		return null;
	}

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
	 * Routine to add an Export to this NodeInst.
	 * @param e the Export to add.
	 */
	public void addExport(Export e)
	{
		exports.add(e);
		Geometric();
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
		Geometric();
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
	 * Routine to associate the ports on this NodeInst with another.
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
		Geometric();
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
		Geometric();
	}

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

	/****************************** TEXT ******************************/

	/**
	 * Routine to return the Text Descriptor associated with this NodeInst.
	 * The only NodeInsts that need Text Descriptors are instances of Cells that are unexpanded.
	 * In this situation, the Cell instance is drawn as a box with a name.
	 * The Text Descriptor applies to the display of that name.
	 * @return the Text Descriptor for this NodeInst.
	 */
	public TextDescriptor getProtoTextDescriptor() { return protoDescriptor; }

	/**
	 * Routine to set the Text Descriptor associated with this NodeInst.
	 * The only NodeInsts that need Text Descriptors are instances of Cells that are unexpanded.
	 * In this situation, the Cell instance is drawn as a box with a name.
	 * The Text Descriptor applies to the display of that name.
	 * @param descriptor the Text Descriptor for this NodeInst.
	 */
	public void setProtoTextDescriptor(TextDescriptor descriptor) { this.protoDescriptor = descriptor; }

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
		System.out.println(" Center: (" + getCenterX() + "," + getCenterY() + "), size: " + getXSize() + "x" + getYSize() + ", rotated " + angle/10.0 +
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
	 * Routine to set the name of this NodeInst.
	 * The name is a local string that can be set by the user.
	 * @param name the new name of this NodeInst.
	 */
	public boolean setNameLow(Name name)
	{
		if (name == getNameLow()) return false;
		if (checkNodeName(name)) return true;

		if (nodeUsage != null && getNameLow() != null)
		{
			parent.removeNodables(this, null);
		}
		super.setNameLow(name);
		if (nodeUsage != null)
		{
			if (name != null) parent.addNodables(this, null);
			parent.updateMaxSuffix(this);
		}

		// create subs
		if (subs == null) return false;
		int width = name.busWidth();
		if (width != subs.length) subs = new Subinst[width];
		for (int i = 0; i < width; i++)
			subs[i] = new Subinst(i);
		return false;
	}

	/**
	 * Routine to check the name of this NodeInst.
	 * @param name the new name of this NodeInst.
	 */
	public boolean checkNodeName(Name name)
	{
		if (!name.isValid())
		{
			System.out.println("Invalid node name <"+name+"> :"+Name.checkName(name.toString()));
			return true;
		}
		if (name.hasEmptySubnames())
		{
			System.out.println("Name <"+name+"> has empty subnames");
			return true;
		}
		if (name.hasDuplicates())
		{
			System.out.println("Name <"+name+"> has duplicate subnames");
			return true;
		}
		if (name.isBus() && !protoType.isIcon())
		{
			System.out.println("Bus name <"+name+"> can be assigned only to icon nodes");
			return true;
		}
		if (nodeUsage != null && !parent.isUniqueName(name, NodeInst.class, this))
		{
			System.out.println("Node name <"+name+"> is duplicated in "+parent);
			return true;
		}
		return false;
	}

	/**
	 * Routine is called when node name is updated.
	 */
	private void updateName()
	{
		Variable var = getVar(NODE_NAME, String.class);
		/*
		if (var != null)
		{
			System.out.println((var.isDisplay()?"":"Invisible ")+"nodename "+(String) var.getObject()+" parent="+parent+" proto="+protoType);
		}
		*/
		Name newName = var != null ? Name.findName( (String) var.getObject() ) : null;
		if (newName == name) return;
		name = newName;

		// If linked, update max suffix
		if (nodeUsage != null)
		{
			parent.updateMaxSuffix(this);
		}

		// create subs
		if (subs == null) return;
		int width = newName != null ? newName.busWidth() : 1;
		if (width != subs.length) subs = new Subinst[width];
		for (int i = 0; i < width; i++)
			subs[i] = new Subinst(i);
	}

	/**
	 * Routine to determine whether a variable name on NodeInst is deprecated.
	 * Deprecated variable names are those that were used in old versions of Electric,
	 * but are no longer valid.
	 * @param name the name of the variable.
	 * @return true if the variable name is deprecated.
	 */
	public boolean isDeprecatedVariable(String name) { return name.equals(NODE_NAME); }

	/**
	 * Routine to determine whether this is an Invisible Pin with text.
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

	/****************************** MISCELLANEOUS ******************************/

	/**
	 * Routine to return the prototype of this NodeInst.
	 * @return the prototype of this NodeInst.
	 */
	public NodeProto getProto() { return protoType; }

	/**
	 * Routine to return the function of this NodeProto.
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
     * Routine to see if this NodeInst is a Primitive Transistor.
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
    
	/**
	 * Routine to return the size of this transistor NodeInst.
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
     * Routine to return a gate PortInst for this transistor NodeInst.
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
     * Routine to return a gate PortInst for this transistor NodeInst.
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
     * Routine to return a gate PortInst for this transistor NodeInst.
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
	 * Returns the basename for autonaming.
	 * @return the basename for autonaming.
	 */
	private Name getBasename()
	{
		return protoType instanceof Cell ? ((Cell)protoType).getBasename() : getFunction().getBasename();
	}

	/**
	 * Routine to return the NodeUsage of this NodeInst.
	 * @return the NodeUsage of this NodeInst.
	 */
	public NodeUsage getNodeUsage() { return nodeUsage; }

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
		at.setToTranslation(getCenterX(), getCenterY());
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

		modifyInstance(centX-getCenterX(), centY-getCenterY(), sizeX-sX,
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
