package com.sun.electric.database;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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
//	/** node is not in use */								private static final int DEADN=                     01;
	/** node has text that is far away */					private static final int NHASFARTEXT=               02;
	/** if on, draw node expanded */						private static final int NEXPAND=                   04;
	/** set if node not drawn due to wiping arcs */			private static final int WIPED=                    010;
	/** set if node is to be drawn shortened */				private static final int NSHORT=                   020;
	//  used by database:                                                                                     0140
//	/** if on, this nodeinst is marked for death */			private static final int KILLN=                   0200;
//	/** nodeinst re-drawing is scheduled */					private static final int REWANTN=                 0400;
//	/** only local nodeinst re-drawing desired */			private static final int RELOCLN=                01000;
//	/** transparent nodeinst re-draw is done */				private static final int RETDONN=                02000;
//	/** opaque nodeinst re-draw is done */					private static final int REODONN=                04000;
	/** general flag used in spreading and highlighting */	private static final int NODEFLAGBIT=           010000;
//	/** if on, nodeinst wants to be (un)expanded */			private static final int WANTEXP=               020000;
//	/** temporary flag for nodeinst display */				private static final int TEMPFLG=               040000;
	/** set if hard to select */							private static final int HARDSELECTN=          0100000;
	/** set if node only visible inside cell */				private static final int NVISIBLEINSIDE=     040000000;
	/** technology-specific bits for primitives */			private static final int NTECHBITS=          037400000;
	/** right-shift of NTECHBITS */							private static final int NTECHBITSSH=               17;
	/** set if node is locked (can't be changed) */			private static final int NILOCKED=          0100000000;

	// Name of Variable holding instance name.
	private static final String VAR_INST_NAME = "NODE_name";

	// ---------------------- private data ----------------------------------
	/** prototype of this node instance */					private NodeProto prototype;
	/** flag bits for this node instance */					private int userBits;
	/** labling information for this node instance */		private int textbits;
	/** HashTable of portInsts on this node instance */		private HashMap portMap;
	/** List of connections belonging to this instance */	private List connections;
	/** List of Exports belonging to this node instance */	private List exports;

	// --------------------- private and protected methods ---------------------
	private NodeInst(NodeProto prototype, Point2D.Double center, double width, double height, double angle, Cell parent)
	{
		// initialize parent class (Geometric)
		this.cX = center.x;   this.cY = center.y;
		this.sX = width;   this.sY = height;
		this.angle = angle;
		this.cos = Math.cos(angle);
		this.sin = Math.sin(angle);
		updateGeometric();
		setParent(parent);

		// initialize this object
		this.prototype = prototype;
		this.userBits = 0;
		this.textbits = 0;
		portMap = new HashMap();
		connections = new ArrayList();
		exports = new ArrayList();

		// add to linked lists
		prototype.addInstance(this);
		parent.addNode(this);
	}

	/** recalculate the transform on this arc to get the endpoints
	 * where they should be. */
	void updateGeometric()
	{
		updateGeometricBounds();
	}

	/** Make a new instance of this NodeProto.
	 *
	 * <p> Jose positions the NodeInst by performing the following 2D
	 * transformations, in order, on the NodeProto: <br>
	 *
	 * 1) scaling by scaleX and scaleY <br>
	 * 2) rotating counter-clockwise by angle <br>
	 * 3) translating by x and y. <br>
	 *
	 * <p> All scaling and rotation is performed about the NodeProto's
	 * Reference-Point. After the 2D transformations, the NodeProto's
	 * Reference-Point ends up at (x, y) with respect to the
	 * Reference-Point of the parent.
	 * 
	 * <p> The Reference-Point for PrimitiveNodes is always at (0,
	 * 0). The reference point for a Cell is (0, 0) unless it contain
	 * an instance of the Cell-Center PrimitiveNode in which case the
	 * location of the Cell-Center instance determines the Cell's
	 * Reference-Point.
	 * @param scaleX the horizontal scale factor. If negative then Jose
	 * mirrors about the y axis.  If this is a Cell then scaleX must be
	 * 1 or -1.
	 * @param scaleY the vertical scale factor. If negative then Jose
	 * mirrors about the x axis. If this is a Cell then scaleY must be
	 * 1 or -1.
	 * @param x the horizontal coordinate of the origin of the
	 * NodeInst.
	 * @param y the vertical coordinate of the origin of the
	 * NodeInst.
	 * @param angle the rotation of the NodeInst. This is in units of
	 * degrees.
	 * @param parent the Cell to contain the new instance.
	 * @return the instance created
	 */
	public static NodeInst newInstance(NodeProto type, Point2D.Double center, double width, double height,
		double angle, Cell parent)
	{
//		if (this instanceof Cell)
//			 ((Cell) this).updateBounds();

		NodeInst ni = new NodeInst(type, center, width, height, angle, parent);
		
		// create all of the portInsts on this node inst
		for (Iterator it = type.getPorts(); it.hasNext();)
		{
			PortProto pp = (PortProto) it.next();
			PortInst pi = PortInst.newInstance(pp, ni);
			ni.portMap.put(pp.getName(), pi);
		}
		int sz = ni.portMap.size();
		return ni;
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

	protected boolean putPrivateVar(String var, Object value)
	{
		if (var.equals("userbits"))
		{
			userBits = ((Integer) value).intValue();
			return true;
		}
		return false;
	}


	void addExport(Export e)
	{
		if (exports == null)
		{
			exports = new ArrayList();
		}
		exports.add(e);
	}

	void removeExport(Export e)
	{
		if (exports == null || !exports.contains(e))
		{
			throw new RuntimeException("Tried to remove a non-existant export");
		}
		exports.remove(e);
	}

	void remove()
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
		if (exports != null)
		{
			while (exports.size() > 0)
			{
				((Export) exports.get(exports.size() - 1)).remove();
			}
		}
		// disconnect from the parent
		getParent().removeNode(this);
		// remove instance from the prototype
		prototype.removeInstance(this);
		super.remove();
	}

	public void getInfo()
	{
		System.out.println("--------- NODE INSTANCE: ---------");
		System.out.println(" Prototype: " + prototype.describe());
		super.getInfo();
		System.out.println(" Ports:");
		for(Iterator it = prototype.getPorts(); it.hasNext();)
		{
			PortProto pp = (PortProto) it.next();
			Poly p = pp.getPoly(this);
			System.out.println("     " + pp + " at " + p.getCenterX() + "," + p.getCenterY());
		}
		if (connections.size() != 0)
		{
			System.out.println(" Connections (" + connections.size() + ")");
			for (int i = 0; i < connections.size(); i++)
			{
				Connection c = (Connection) connections.get(i);
				System.out.println("     " + c.getArc().getProto().getProtoName()
					+ " on " + c.getPortInst().getPortProto().getName());
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
			if (e.getPortInst().getNodeInst() == oldInst)
				toMove.add(e);
		}

		// move these exports to new instance
		for (int i = 0; i < toMove.size(); i++)
		{
			Export e = (Export) toMove.get(i);

			String expNm = e.getName();
			String portNm = e.getPortInst().getPortProto().getName();
			e.delete();
			PortInst newPort = newInst.findPort(portNm);
			error(
				newPort == null,
				"NodeInst.moveExports: can't find port: "
					+ portNm
					+ " on new inst");
			parent.newExport(expNm, newPort);
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

	public Poly [] getShape()
	{
		if (!(prototype instanceof PrimitiveNode)) return null;
		PrimitiveNode np = (PrimitiveNode)prototype;
		Technology.NodeLayer [] primLayers = np.getLayers();
		for(int i = 0; i < primLayers.length; i++)
		{
			Technology.NodeLayer primLayer = primLayers[i];
			int representation = primLayer.getRepresentation();
			Poly.Type style = primLayer.getStyle();
			if (representation == Technology.NodeLayer.BOX)
			{
				if (style == Poly.Type.FILLEDRECT || style == Poly.Type.CLOSEDRECT)
				{
				} else
				{
			//		double lowX = 
//					subrange(ni->lowx, ni->highx, lay->points[0], lay->points[1],
//						lay->points[4], lay->points[5], &poly->xv[0], &poly->xv[2], lambda);
//					subrange(ni->lowy, ni->highy, lay->points[2], lay->points[3],
//						lay->points[6], lay->points[7], &poly->yv[0], &poly->yv[1], lambda);
				}
			}
		}
		return null;
	}

	/** Get an iterator for all PortInst's */
	public Iterator getPorts()
	{
		return portMap.values().iterator();
	}

	/** Get the only port. <br> This is quite useful for vias and pins
	 * which have only one port. If there's more or less than one port
	 * then die. */
	public PortInst getPort()
	{
		int sz = portMap.size();
		error(sz != 1, "NodeInst.getPort: there isn't exactly one port: " + sz);
		return (PortInst) portMap.values().iterator().next();
	}

	/** Find the PortInst with the given name. If no PortInst has this
	 * name then return null. */
	public PortInst findPort(String name)
	{
		return (PortInst) portMap.get(name);
	}

	/**
	 * can this node connect to a particular arc? (forwarded to NodeProto)
	 * @param arc the type of arc to test for
	 * @return the first port that can connect to this node, or
	 * null, if no such port on this node exists
	 */
	public PortProto connectsTo(ArcProto arc)
	{
		return prototype.connectsTo(arc);
	}

	void addConnection(Connection c)
	{
		connections.add(c);
	}

	void removeConnection(Connection c)
	{
		connections.remove(c);
	}

	public Iterator getExports()
	{
		if (exports == null)
		{
			return (new ArrayList()).iterator();
		} else
		{
			return exports.iterator();
		}
	}

	public NodeProto getProto()
	{
		return prototype;
	}

	/** Is there a label attached to this node inst? <br>
	 * RKao I need to figure out what a "label" is. */
	public boolean hasLabel()
	{
		return (textbits & 060) != 0;
	}

	/** Return the instance name. <p> If there is no name return null. */
	public String getName()
	{
		Object val = getVar(VAR_INST_NAME);
		error(
			val != null && !(val instanceof String),
			"NodeInst NODE_name variable isn't a string.");
		return (String) val;
	}

	/** Set the instance name. */
	public void setName(String val)
	{
		setVar(VAR_INST_NAME, val);
	}

	public String describe()
	{
		return prototype.getProtoName();
	}

	public String toString()
	{
		return "NodeInst " + prototype.getProtoName();
	}

	/** Get the location of the NodeProto's reference point in the
	 * coordinate space of the Cell containing this NodeInst.
	 */
//	public Point2D getReferencePoint()
//	{
//		// Tricky! First transform the reference point in BASE units.
//		Point2D.Double p = prototype.getRefPointBase();
//		Poly poly =
//			new Poly(new double[] { p.x }, new double[] { p.y });
//		Poly xformedPoly = xformPoly(poly);
//		Rectangle2D xr = xformedPoly.getBounds2D();
//
//		// Then convert the transformed point
//		double x = xr.getX();
//		double y = xr.getY();
//
//		// return coordinates relative to the reference point in the parent Cell
//		Point2D rpPar = getParent().getReferencePoint();
//		return new Point2D.Double(x - rpPar.getX(), y - rpPar.getY());
//	}

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
//		NodeProto p = prototype;
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
//		NodeProto p = prototype;
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
