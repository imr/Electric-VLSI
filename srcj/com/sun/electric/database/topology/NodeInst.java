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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A NodeInst is an instance of a NodeProto (a primitive node or a Cell).
 * A NodeInst points to its prototype and the Cell on which it has been
 * instantiated.  It also has a name, and contains a list of Connections
 * and Exports.
 */
public class NodeInst extends Geometric
{
	// -------------------------- constants --------------------------------
//	/** node is not in use */								private static final int DEADN =                     01;
	/** node has text that is far away */					private static final int NHASFARTEXT =               02;
	/** if on, draw node expanded */						private static final int NEXPAND =                   04;
	/** set if node not drawn due to wiping arcs */			private static final int WIPED =                    010;
	/** set if node is to be drawn shortened */				private static final int NSHORT =                   020;
	//  used by database:                                                                                      0140
//	/** if on, this nodeinst is marked for death */			private static final int KILLN =                   0200;
//	/** nodeinst re-drawing is scheduled */					private static final int REWANTN =                 0400;
//	/** only local nodeinst re-drawing desired */			private static final int RELOCLN =                01000;
//	/** transparent nodeinst re-draw is done */				private static final int RETDONN =                02000;
//	/** opaque nodeinst re-draw is done */					private static final int REODONN =                04000;
	/** general flag used in spreading and highlighting */	private static final int NODEFLAGBIT =           010000;
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
	/** flag bits for this node instance */					private int userBits;
	/** labling information for this node instance */		private int textbits;
	/** HashTable of portInsts on this node instance */		private HashMap portMap;
	/** List of connections belonging to this instance */	private List connections;
	/** List of Exports belonging to this node instance */	private List exports;
	/** Text descriptor */									private TextDescriptor descriptor;

	// --------------------- private and protected methods ---------------------
	private NodeInst()
	{
		// initialize this object
		this.userBits = 0;
		this.textbits = 0;
		this.portMap = new HashMap();
		this.connections = new ArrayList();
		this.exports = new ArrayList();
		this.descriptor = new TextDescriptor();
	}

	/**
	 * Recalculate the transform on this arc to get the endpoints
	 * where they should be.
	 */
	void updateGeometric()
	{
		updateGeometricBounds();
	}

	/**
	 * Low-level access routine to create a NodeInst.
	 */
	public static NodeInst lowLevelAllocate()
	{
		NodeInst ni = new NodeInst();
		ni.parent = null;
		return ni;
	}

	/**
	 * Low-level routine to fill-in the NodeInst information.
	 * Returns true on error.
	 */
	public boolean lowLevelPopulate(NodeProto protoType, Point2D.Double center, double width, double height, double angle,
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
		linkGeom(parent);
		return false;
	}

	/**
	 * Low-level access routine to link the NodeInst into its Cell.
	 * Returns true on error.
	 */
	public boolean lowLevelLink()
	{
		// add to linked lists
		protoType.addInstance(this);
		parent.addNode(this);
		return false;
	}

	public static NodeInst newInstance(NodeProto type, Point2D.Double center, double width, double height,
		double angle, Cell parent)
	{
		NodeInst ni = lowLevelAllocate();
		if (ni.lowLevelPopulate(type, center, width, height, angle, parent)) return null;
		if (ni.lowLevelLink()) return null;
		return ni;
	}

	/**
	 * Change this node by (dx,dy) in position, (dxsize,dysize) in size, and drot in rotation.
	 */
	public void modifyInstance(double dx, double dy, double dxsize, double dysize, double drot)
	{
		this.cX += dx;
		this.cY += dy;

		this.sX += dxsize;
		this.sY += dysize;

		this.angle += drot;
		updateGeometric();
		parent.setDirty();
	}

	public void delete()
	{
//		// avoid removing items from the same list we're iterating over
//		ArrayList conns = (ArrayList) connections.clone();
//		for (Iterator it = conns.iterator(); it.hasNext();)
//		{
//			Connection c = (Connection) it.next();
//			ArcInst ai = c.getArc();
//			ai.delete();
//		}
//		super.delete();
	}

	/** Is this NodeInst an icon of its parent?  This occurs because a
	 * schematic may contain an icon of itself */
	public boolean isIconOfParent()
	{
		NodeProto np = getProto();
		if (!(np instanceof Cell))
			return false;

		return getParent().getCellGroup() == ((Cell) np).getCellGroup();
	}

	public void addExport(Export e)
	{
		exports.add(e);
	}

	public void removeExport(Export e)
	{
		if (!exports.contains(e))
		{
			throw new RuntimeException("Tried to remove a non-existant export");
		}
		exports.remove(e);
	}

	public void remove()
	{
		// kill the arcs attached to the connections.  This will also
		// remove the connections themselves
		while (connections.size() > 0)
		{
			((Connection) connections.get(connections.size() - 1))
				.getArc()
				.remove();
		}
		// remove any exports
		while (exports.size() > 0)
		{
			((Export) exports.get(exports.size() - 1)).remove();
		}

		// disconnect from the parent
		getParent().removeNode(this);
		// remove instance from the prototype
		protoType.removeInstance(this);
		super.remove();
	}

	public void getInfo()
	{
		System.out.println("--------- NODE INSTANCE: ---------");
		System.out.println(" Prototype: " + protoType.describe());
		super.getInfo();
		System.out.println(" Ports:");
		for(Iterator it = getPortInsts(); it.hasNext();)
		{
			PortInst pi = (PortInst) it.next();
			Poly p = pi.getPoly();
			System.out.println("     " + pi.getPortProto() + " at " + p.getCenterX() + "," + p.getCenterY());
		}
		if (connections.size() != 0)
		{
			System.out.println(" Connections (" + connections.size() + ")");
			for (int i = 0; i < connections.size(); i++)
			{
				Connection c = (Connection) connections.get(i);
				System.out.println("     " + c.getArc().getProto().getProtoName()
					+ " on " + c.getPortInst().getPortProto().getProtoName());
			}
		}
		if (exports.size() != 0)
		{
			System.out.println(" Exports (" + exports.size() + ")");
			for (int i = 0; i < exports.size(); i++)
			{
				Export ex = (Export) exports.get(i);
				System.out.println("     " + ex);
			}
		}
	}

	/** sanity check function, used by Connection.checkobj */
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
			Export.newInstance(parent, newInst, newPort, expNm);
		}
	}

	/**
	 * Recomputes the "WIPED" flag bit on the node, depending on whether there are wipable arcs.
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

	// ------------------------ public methods -------------------------------

	/** Set the Far-Text bit */
	public void setFarText() { userBits |= NHASFARTEXT; }
	/** Clear the Far-Text bit */
	public void clearFarText() { userBits &= ~NHASFARTEXT; }
	/** Get the Far-Text bit */
	public boolean isFarText() { return (userBits & NHASFARTEXT) != 0; }

	/** Set the Expanded bit */
	public void setExpanded() { userBits |= NEXPAND; }
	/** Clear the Expanded bit */
	public void clearExpanded() { userBits &= ~NEXPAND; }
	/** Get the Expanded bit */
	public boolean isExpanded() { return (userBits & NEXPAND) != 0; }

	/** Set the Wiped bit */
	public void setWiped() { userBits |= WIPED; }
	/** Clear the Wiped bit */
	public void clearWiped() { userBits &= ~WIPED; }
	/** Get the Wiped bit */
	public boolean isWiped() { return (userBits & WIPED) != 0; }

	/** Set the Shortened bit */
	public void setShortened() { userBits |= NSHORT; }
	/** Clear the Shortened bit */
	public void clearShortened() { userBits &= ~NSHORT; }
	/** Get the Shortened bit */
	public boolean isShortened() { return (userBits & NSHORT) != 0; }

	/** Set the general flag bit */
	public void setFlagged() { userBits |= NODEFLAGBIT; }
	/** Clear the general flag bit */
	public void clearFlagged() { userBits &= ~NODEFLAGBIT; }
	/** Get the general flag bit */
	public boolean isFlagged() { return (userBits & NODEFLAGBIT) != 0; }

	/** Set the Hard-to-Select bit */
	public void setHardSelect() { userBits |= HARDSELECTN; }
	/** Clear the Hard-to-Select bit */
	public void clearHardSelect() { userBits &= ~HARDSELECTN; }
	/** Get the Hard-to-Select bit */
	public boolean isHardSelect() { return (userBits & HARDSELECTN) != 0; }

	/** Set the Visible-Inside bit */
	public void setVisInside() { userBits |= NVISIBLEINSIDE; }
	/** Clear the Visible-Inside bit */
	public void clearVisInside() { userBits &= ~NVISIBLEINSIDE; }
	/** Get the Visible-Inside bit */
	public boolean isVisInside() { return (userBits & NVISIBLEINSIDE) != 0; }

	/** Set the Locked bit */
	public void setLocked() { userBits |= NILOCKED; }
	/** Clear the Locked bit */
	public void clearLocked() { userBits &= ~NILOCKED; }
	/** Get the Locked bit */
	public boolean isLocked() { return (userBits & NILOCKED) != 0; }

	/** Set the Technology-specific value */
	public void setTechSpecific(int value) { userBits = (userBits & ~NTECHBITS) | (value << NTECHBITSSH); }
	/** Get the Locked bit */
	public int getTechSpecific() { return (userBits & NTECHBITS) >> NTECHBITSSH; }

	/** Low-level routine to get the user bits.  Should not normally be called. */
	public int lowLevelGetUserbits() { return userBits; }
	/** Low-level routine to set the user bits.  Should not normally be called. */
	public void lowLevelSetUserbits(int userBits) { this.userBits = userBits; }

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

	public AffineTransform transformOut(AffineTransform prevTransform)
	{
		AffineTransform transform = transformOut();
		AffineTransform returnTransform = new AffineTransform(prevTransform);
		returnTransform.concatenate(transform);
		return returnTransform;
	}

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

	public AffineTransform translateOut(AffineTransform prevTransform)
	{
		AffineTransform transform = translateOut();
		AffineTransform returnTransform = new AffineTransform(prevTransform);
		returnTransform.concatenate(transform);
		return returnTransform;
	}

	public AffineTransform rotateOut()
	{
		return rotateAbout(angle, sX, sY, cX, cY);
	}

	public AffineTransform rotateOut(AffineTransform prevTransform)
	{
		// if there is no transformation, stop now
		if (angle == 0 && sX >= 0 && sY >= 0) return prevTransform;

		AffineTransform transform = rotateOut();
		AffineTransform returnTransform = new AffineTransform(prevTransform);
		returnTransform.concatenate(transform);
		return returnTransform;
	}

	/** Get an iterator for all PortInst's */
	public Iterator getPortInsts()
	{
		return portMap.values().iterator();
	}

	/** Get the number of PortInst's */
	public int getNumPortInsts()
	{
		return portMap.size();
	}

	/** Get the only port. <br> This is quite useful for vias and pins
	 * which have only one port. If there's more or less than one port
	 * then die. */
	public PortInst getOnlyPortInst()
	{
		int sz = portMap.size();
		if (sz != 1) System.out.println("NodeInst.getOnlyPort: there isn't exactly one port: " + sz);
		return (PortInst) portMap.values().iterator().next();
	}

	/** Find the PortInst with the given name. If no PortInst has this
	 * name then return null. */
	public PortInst findPortInst(String name)
	{
		return (PortInst) portMap.get(name);
	}
	
	public Integer [] getTrace()
	{
		Variable var = this.getVal("trace", Integer[].class);
		if (var == null) return null;
		Object obj = var.getObject();
		if (obj instanceof Object[]) return (Integer []) obj;
		return null;
	}

	/**
	 * routine to determine whether pin display should be supressed by counting
	 * the number of arcs and seeing if there are one or two and also by seeing if
	 * the node has exports (then draw it if there are three or more).
	 * Returns true if the pin should be supressed.
	 */
	public boolean pinUseCount()
	{
		if (connections.size() > 2) return false;
		if (exports.size() != 0) return true;
		if (connections.size() == 0) return false;
		return true;
	}

	/**
	 * can this node connect to a particular arc? (forwarded to NodeProto)
	 * @param arc the type of arc to test for
	 * @return the first port that can connect to this node, or
	 * null, if no such port on this node exists
	 */
	public PortProto connectsTo(ArcProto arc)
	{
		return protoType.connectsTo(arc);
	}

	void addConnection(Connection c)
	{
		connections.add(c);
		NodeInst ni = c.getPortInst().getNodeInst();
		ni.computeWipeState();
	}

	void removeConnection(Connection c)
	{
		connections.remove(c);
		NodeInst ni = c.getPortInst().getNodeInst();
		ni.computeWipeState();
	}

	public Iterator getExports()
	{
		return exports.iterator();
	}

	public int getNumExports() { return exports.size(); }

	public Iterator getConnections()
	{
		return connections.iterator();
	}

	public int getNumConnections() { return connections.size(); }

	public NodeProto getProto()
	{
		return protoType;
	}

	/** Get the Text Descriptor associated with this port. */
	public TextDescriptor getTextDescriptor() { return descriptor; }

	/** Get the Text Descriptor associated with this port. */
	public void setTextDescriptor(TextDescriptor descriptor) { this.descriptor = descriptor; }

	/** Is there a label attached to this node inst? <br>
	 * RKao I need to figure out what a "label" is. */
	public boolean hasLabel()
	{
		return (textbits & 060) != 0;
	}

	/** Return the instance name. <p> If there is no name return null. */
	public String getName()
	{
		Variable var = getVal(VAR_INST_NAME, String.class);
		if (var == null) return null;
		return (String) var.getObject();
	}

	/** Set the instance name. */
	public Variable setName(String name)
	{
		Variable var = setVal(VAR_INST_NAME, name);
		if (var != null) var.setDisplay();
		return var;
	}

	/**
	 * Routine to describe this NodeInst as a string.
	 */
	public String describe()
	{
		return protoType.getProtoName();
	}

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

}
