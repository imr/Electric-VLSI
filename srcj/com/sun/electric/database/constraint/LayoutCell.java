/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayoutCell.java
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
package com.sun.electric.database.constraint;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellUsage;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.user.User;
import java.awt.geom.AffineTransform;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Class to implement the layout-constraint system for a specific Cell.
 * Handles the fixed-angle and rigid constraints.
 */
class LayoutCell {
	/**
	 * The meaning of changeClock for object modification:
	 *
	 * ai.getChangeClock() <  changeClock-2  unmodified         arcs
	 * ai.getChangeClock() == changeClock-2  unmodified rigid   arcs
	 * ai.getChangeClock() == changeClock-1  unmodified unrigid arcs
	 * ai.getChangeClock() == changeClock      modified rigid   arcs
	 * ai.getChangeClock() == changeClock+1    modified unrigid arcs
	 * ni.getChangeClock() <  changeClock-1  unmodified         nodes
	 * ni.getChangeClock() == changeClock-1  size-changed       nodes
	 * ni.getChangeClock() == changeClock    position-changed   nodes
	 */
    private static final Integer AI_RIGID = new Integer(0);
    private static final Integer AI_FLEX = new Integer(1);
    
    final Cell cell;
    private HashMap<Export,PortInst> oldExports;
    private HashMap<ArcInst,ImmutableArcInst> oldArcs;
    private LinkedHashMap<NodeInst,ImmutableNodeInst> oldNodes = new LinkedHashMap<NodeInst,ImmutableNodeInst>();
    
    
    /** True if this Cell needs to be computed. */
    private boolean modified;
    /** True if change of Exports of this Cell causes recomputation of upper Cells. */
    private boolean exportsModified;
    /** True if subcells changed names of Library:Cell:Export */
    private boolean subcellsRenamed;
    /** True if this Cell is computed by Layout constraint system. */
    private boolean computed;
    /** Backup of this Cell before Job */
    private final CellBackup oldBackup;

//    /** Map from ArcInst to its change clock */
//    private HashMap<Geometric,Integer> changeClock;
    /** Set of nodes already moved not to move twice. */
    private HashSet<NodeInst> movedNodes;
    
    LayoutCell(Cell cell, CellBackup oldBackup) {
        this.cell = cell;
        this.oldBackup = oldBackup;
    }
    
    /**
     * Recursively compute this cell and all its subcells in bottom-up order.
     */
    void compute() {
        if (computed) return;
        computed = true;
        for (Iterator<CellUsage> it = cell.getUsagesIn(); it.hasNext(); ) {
            CellUsage u = it.next();
            Layout.getCellInfo(u.getProto()).compute();
        }
        if (modified || exportsModified) {
            doCompute();
            if (exportsModified) {
                for (Iterator<CellUsage> it = cell.getUsagesOf(); it.hasNext(); ) {
                    CellUsage u = it.next();
                    Layout.getCellInfo(u.getParent()).modified = true;
                }
            }
        }
        cell.getTechnology();
        boolean justWritten = Layout.librariesWritten.contains(cell.getLibrary().getId());
        if (!justWritten) {
            CellBackup newBackup = cell.backup();
            if (newBackup != oldBackup) {
                cell.madeRevision(Layout.revisionDate, Layout.userName);
                assert cell.isModified(true);
                cell.getLibrary().setChanged();
            }
        }
        if (Layout.goodDRCCells != null && Layout.goodDRCCells.contains(cell)) {
            cell.addVar(Layout.goodDRCDate);
            cell.addVar(Layout.goodDRCBit);
        }
        cell.getBounds();
        
        // Release unnecessary memory
        oldArcs = null;
//        changeClock = null;
        movedNodes = null;
    }
    
    /**
     * Compute this Cell.
     **/
    private void doCompute() {
//        changeClock = new HashMap<Geometric,Integer>();
        movedNodes = new HashSet<NodeInst>();
        
        LinkedHashSet<NodeInst> modifiedInsts = new LinkedHashSet<NodeInst>();
        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            boolean portsModified;
            if (ni.isCellInstance()) {
                LayoutCell subCell = Layout.getCellInfo((Cell)ni.getProto());
                portsModified = subCell.exportsModified;
            } else {
                ImmutableNodeInst d = getOldD(ni);
                portsModified = d != null &&
                        (d.width != ni.getXSize() || d.height != ni.getYSize());
            }
            if (portsModified) {
                modifiedInsts.add(ni);
            }
        }
        if (oldNodes != null) {
            for (Map.Entry<NodeInst,ImmutableNodeInst> e : oldNodes.entrySet()) {
                NodeInst ni = (NodeInst)e.getKey();
                ImmutableNodeInst d = (ImmutableNodeInst)e.getValue();
                if (d != null) {
                    modifiedInsts.add(ni);
                    if (!ni.getAnchorCenter().equals(d.anchor) || !ni.getOrient().equals(d.orient))
                        movedNodes.add(ni);
                }
            }
        }
        for (NodeInst ni : modifiedInsts) {
            if (ni.hasExports())
//            if (ni.getNumExports() != 0)
                exportsModified = true;
            ImmutableNodeInst d = getOldD(ni);
            Orientation dOrient = d != null ? ni.getOrient().concatenate(d.orient.inverse()) : Orientation.IDENT;
            modNodeArcs(ni, dOrient);
        }
        if (oldArcs != null) {
            ArcInst[] oldArcsCopy = oldArcs.keySet().toArray(ArcInst.NULL_ARRAY);
            for (ArcInst ai: oldArcsCopy) {
                if (!ai.isLinked()) continue;
                ensureArcInst(ai, Layout.isRigid(ai) ? AI_RIGID : AI_FLEX);
            }
        }
    }
  
	/**
	 * Method to modify nodeinst "ni" by "deltalx" in low X, "deltaly" in low Y,
	 * "deltahx" in high X, "deltahy" in high Y, and "dangle" tenth-degrees.
	 * If the nodeinst is a portproto of the current cell and has any arcs
	 * connected to it, the method returns nonzero to indicate that the outer
	 * cell has ports that moved (the nodeinst has exports).
	 */
	private void alterNodeInst(NodeInst ni, double deltaCX, double deltaCY, Orientation dOrient)
	{
		// reject if this change has already been done
        if (deltaCX == 0 && deltaCY == 0 && dOrient.equals(Orientation.IDENT)) return;
        if (movedNodes.contains(ni)) return;

		if (Layout.DEBUG) System.out.println("Moving "+ni+" [is "+ni.getXSize()+"x"+ni.getYSize()+" at ("+
                ni.getAnchorCenterX()+","+ni.getAnchorCenterY()+") rot "+ni.getOrient()+
			"] change is dx="+deltaCX+" dy="+deltaCY+") dOrient="+dOrient);

        ni.modifyInstance(deltaCX, deltaCY, 0, 0, dOrient);
        movedNodes.add(ni);

		// see if this nodeinst is a port of the current cell
		if (ni.hasExports())
//		if (ni.getNumExports() != 0)
            exportsModified = true;
	}

	/**
	 * Method to modify all of the arcs connected to a NodeInst.
	 * @param ni the NodeInst being examined.
	 * @param dSX the change in the node's X size.
	 * @param dSY the change in the node's Y size.
	 * @param dOrient the change of Orientation of the NodeInst.
	 * @return true if some exports on the current cell have moved.
	 * This indicates that the cell must be re-examined for export locations.
	 */
	private void modNodeArcs(NodeInst ni, Orientation dOrient)
	{
		if (Layout.DEBUG) System.out.println("Updating arcs on "+ni);

		// next look at arcs that run within this nodeinst
		modWithin(ni, dOrient);

		// next look at the rest of the rigid arcs on this nodeinst
		modRigid(ni, dOrient);

		// finally, look at rest of the flexible arcs on this nodeinst
		modFlex(ni, dOrient);
	}

	/*
	 * Method to modify the arcs that run within nodeinst "ni"
	 */
	private void modWithin(NodeInst ni, Orientation dOrient)
	{
		// ignore all this stuff if the node just got created
		if (getOldD(ni) == null) return;

		// build a list of the arcs with both ends on this nodeinst
		List<ArcInst> interiorArcs = new ArrayList<ArcInst>();
		for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = it.next();
			ArcInst ai = con.getArc();

			// ignore if arcinst is not within the node
			if (ai.getHeadPortInst().getNodeInst() != ai.getTailPortInst().getNodeInst()) continue;
//			if (getChangeClock(ai) == AI_RIGID.intValue()) continue;

			// include in the list to be considered here
			interiorArcs.add(ai);
		}

		// look for arcs with both ends on this nodeinst
		for(ArcInst ai : interiorArcs)
		{
			if (!ai.isLinked()) continue;

			// if arcinst has already been changed check its connectivity
			if (arcMoved(ai))
//			if (getChangeClock(ai) == AI_RIGID.intValue())
			{
				if (Layout.DEBUG) System.out.println("    Arc already changed");
				ensureArcInst(ai, AI_RIGID);
				continue;
			}

			// determine the new ends of the arcinst
			AffineTransform trans = transformByPort(ai.getHeadPortInst());
			Point2D newHead = new Point2D.Double();
			trans.transform(ai.getHeadLocation(), newHead);

			trans = transformByPort(ai.getTailPortInst());
			Point2D newTail = new Point2D.Double();
			trans.transform(ai.getTailLocation(), newTail);

			// move the arcinst
			doMoveArcInst(ai, newHead, newTail, AI_RIGID);
		}
	}

	/**
	 * Method to modify the rigid arcs connected to a NodeInst.
	 * @param ni the NodeInst being examined.
	 * @param dOrient the change in the node Orientation.
	 * @return true if any nodes that have exports move.
	 * This indicates that instances of the current cell must be examined for ArcInst motion.
	 */
	private void modRigid(NodeInst ni, Orientation dOrient)
	{
		// build a list of the rigid arcs on this nodeinst
		List<Connection> rigidArcs = new ArrayList<Connection>();
		for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = it.next();
			ArcInst ai = con.getArc();

			// ignore if arcinst is not flexible
//			if (getChangeClock(ai) == AI_FLEX.intValue()) continue;
			if (!Layout.isRigid(ai)) continue;

			// ignore arcs that connect two ports on the same node
			if (ai.getHeadPortInst().getNodeInst() == ai.getTailPortInst().getNodeInst()) continue;

			// include in the list to be considered here
            rigidArcs.add(con);
		}
		if (rigidArcs.size() == 0) return;

		// look for rigid arcs on this nodeinst
        HashSet<ArcInst> rigidModified = new HashSet<ArcInst>();
		for(Connection thisEnd : rigidArcs)
		{
            ArcInst ai = thisEnd.getArc();
			if (!ai.isLinked()) continue;
			if (Layout.DEBUG) System.out.println("  From " + ni + " Modifying Rigid "+ai);

			// if rigid arcinst has already been changed check its connectivity
			if (arcMoved(ai))
//			if (getChangeClock(ai) == AI_RIGID.intValue())
			{
				if (Layout.DEBUG) System.out.println("    Arc already changed");
				ensureArcInst(ai, AI_RIGID);
				continue;
			}

			// find out which end of the arcinst is where, ignore internal arcs
            int thisEndIndex = thisEnd.getEndIndex();
            int otherEndIndex = 1 - thisEndIndex;

			PortInst opi = ai.getPortInst(otherEndIndex);
			NodeInst ono = opi.getNodeInst();
			PortProto opt = opi.getPortProto();
			EPoint otherLocation = ai.getLocation(otherEndIndex);

			// create the two points that will be the new ends of this arc
            AffineTransform trans = transformByPort(thisEnd.getPortInst());
			Point2D [] newPts = new Point2D.Double[2];
			newPts[0] = new Point2D.Double();
			newPts[1] = new Point2D.Double();

			// figure out the new location of this arcinst connection
			trans.transform(thisEnd.getLocation(), newPts[thisEndIndex]);

			// figure out the new location of that arcinst connection
			trans.transform(otherLocation, newPts[otherEndIndex]);

			// see if other nodeinst has changed
			boolean locked = false;
			if (movedNodes.contains(ono)) locked = true; else
			{
				if (ono.isLocked()) locked = true; else
				{
					if (ono.isCellInstance())
					{
						if (ono.getParent().isInstancesLocked()) locked = true;
						if (User.isDisallowModificationComplexNodes()) locked = true;
					} else
					{
						if (User.isDisallowModificationLockedPrims() &&
							((PrimitiveNode)ono.getProto()).isLockedPrim()) locked = true;
						if (User.isDisallowModificationComplexNodes())
						{
							PrimitiveNode.Function fun = ono.getFunction();
							if (fun != PrimitiveNode.Function.PIN && fun != PrimitiveNode.Function.CONTACT &&
								fun != PrimitiveNode.Function.NODE && fun != PrimitiveNode.Function.CONNECT)
									locked = true;
						}
					}
				}
			}
			if (!locked)
			{
				// compute port motion within the other nodeinst (is this right? !!!)
				Poly oldPoly = Layout.oldPortPosition(opi);
				Poly oPoly = opi.getPoly();
                if (oldPoly == null) oldPoly = oPoly;
				double oldX = oldPoly.getCenterX();
				double oldY = oldPoly.getCenterY();
				double dx = oPoly.getCenterX();   double dy = oPoly.getCenterY();
				double othX = dx - oldX;
				double othY = dy - oldY;

				// figure out the new location of the other nodeinst
				Point2D ptD = new Point2D.Double();
				trans.transform(ono.getAnchorCenter(), ptD);
				dx = ptD.getX();   dy = ptD.getY();
				dx = dx - ono.getAnchorCenterX() - othX;
				dy = dy - ono.getAnchorCenterY() - othY;

				// ignore null motion on nodes that have already been examined
				if (dx != 0 || dy != 0 || !dOrient.equals(Orientation.IDENT))
				{
                    rigidModified.add(ai);
					if (Layout.DEBUG) System.out.println("    Moving "+ono+" at other end by ("+dx+","+dy+")");
                    alterNodeInst(ono, dx, dy, dOrient);
				}
			}

			// move the arcinst
			if (Layout.DEBUG) System.out.println("    Altering arc, head moves to "+newPts[ArcInst.HEADEND]+" tail moves to "+newPts[ArcInst.TAILEND]);
			doMoveArcInst(ai, newPts[ArcInst.HEADEND], newPts[ArcInst.TAILEND], AI_RIGID);
		}

		// re-scan rigid arcs and recursively modify arcs on other nodes
		for(Connection thisEnd : rigidArcs)
		{
            ArcInst ai = thisEnd.getArc();
			if (!ai.isLinked()) continue;

			// only want arcinst that was just explored
            if (!rigidModified.contains(ai)) continue;

			// get the other nodeinst
			NodeInst ono = ai.getPortInst(1 - thisEnd.getEndIndex()).getNodeInst();

			if (Layout.DEBUG) System.out.println("  " + ni + " re-examining " + ai + " to other "+ono);
			modNodeArcs(ono, dOrient);
		}
	}

	/**
	 * Method to modify the flexible arcs connected to a NodeInst.
	 * @param ni the NodeInst being examined.
	 * @param dOrient the change in the node Orientation.
	 * @return true if any nodes that have exports move.
	 * This indicates that instances of the current cell must be examined for ArcInst motion.
	 */
	private void modFlex(NodeInst ni, Orientation dOrient)
	{
		// build a list of the flexible arcs on this nodeinst
		List<Connection> flexArcs = new ArrayList<Connection>();
		for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = it.next();
			ArcInst ai = con.getArc();

			// ignore if arcinst is not flexible
//			if (getChangeClock(ai) == AI_RIGID.intValue()) continue;
			if (Layout.isRigid(ai)) continue;

			// ignore arcs that connect two ports on the same node
			if (ai.getHeadPortInst().getNodeInst() == ai.getTailPortInst().getNodeInst()) continue;

			// include in the list to be considered here
            flexArcs.add(con);
		}
		if (flexArcs.size() == 0) return;

		// look at all of the flexible arcs on this nodeinst
		for(Connection thisEnd : flexArcs)
		{
            ArcInst ai = thisEnd.getArc();
			if (!ai.isLinked()) continue;
			if (Layout.DEBUG) System.out.println("  Modifying fixed-angle "+ai);

			// if flexible arcinst has been changed, verify its connectivity
			if (arcMoved(ai))
//			if (getChangeClock(ai) >= AI_FLEX.intValue())
			{
				if (Layout.DEBUG) System.out.println("   Arc already changed");
				ensureArcInst(ai, AI_FLEX);
				continue;
			}

			// figure where each end of the arcinst is
            int thisEndIndex = thisEnd.getEndIndex();
            int thatEndIndex = 1 - thisEndIndex;
            EPoint thisLocation = thisEnd.getLocation();
            EPoint thatLocation = ai.getLocation(thatEndIndex);

			// if nodeinst motion stays within port area, ignore the arcinst
			if (ai.isSlidable() && ai.stillInPort(thisEndIndex, thisLocation, true))
				continue;
            
			// create the two points that will be the new ends of this arc
			Point2D [] newPts = new Point2D.Double[2];
            for (int i = 0; i < 2; i++) {
                newPts[i] = new Point2D.Double();
                AffineTransform trans = transformByPort(ai.getPortInst(i));
                trans.transform(ai.getLocation(i), newPts[i]);
                newPts[i].setLocation(DBMath.round(newPts[i].getX()),
                    DBMath.round(newPts[i].getY()));
            }

			// make sure the arc end is still in the port
			Poly poly = thisEnd.getPortInst().getPoly();
			if (!poly.isInside(newPts[thisEndIndex]))
			{
				Rectangle2D bbox = poly.getBounds2D();
				if (newPts[thisEndIndex].getY() >= bbox.getMinY() && newPts[thisEndIndex].getY() <= bbox.getMaxY())
				{
					// extend arc horizontally to fit in port
					if (newPts[thisEndIndex].getX() < bbox.getMinX())
					{
						newPts[thisEndIndex].setLocation(bbox.getMinX(), newPts[thisEndIndex].getY());
					} else if (newPts[thisEndIndex].getX() > bbox.getMaxX())
					{
						newPts[thisEndIndex].setLocation(bbox.getMaxX(), newPts[thisEndIndex].getY());
					}
				} else if (newPts[thisEndIndex].getX() >= bbox.getMinX() && newPts[thisEndIndex].getX() <= bbox.getMaxX())
				{
					// extend arc vertically to fit in port
					if (newPts[thisEndIndex].getY() < bbox.getMinY())
					{
						newPts[thisEndIndex].setLocation(newPts[thisEndIndex].getX(), bbox.getMinY());
					} else if (newPts[thisEndIndex].getY() > bbox.getMaxY())
					{
						newPts[thisEndIndex].setLocation(newPts[thisEndIndex].getX(), bbox.getMaxY());
					}
				} else
				{
					// extend arc arbitrarily to fit in port
					Point2D pt = poly.closestPoint(newPts[thisEndIndex]);
					newPts[thisEndIndex].setLocation(pt);
				}
			}

			// get other end of arcinst and its position
			NodeInst ono = ai.getPortInst(thatEndIndex).getNodeInst();
//			newPts[thatEndIndex].setLocation(thatLocation);

			// see if other nodeinst has changed
			boolean mangle = true;
            if (movedNodes.contains(ono)) mangle = false;
			if (!ai.isFixedAngle()) mangle = false; else
			{
				if (ono.isLocked()) mangle = false; else
				{
					if (ono.isCellInstance())
					{
						if (ono.getParent().isInstancesLocked()) mangle = false;
					} else
					{
						if (User.isDisallowModificationLockedPrims() &&
							((PrimitiveNode)ono.getProto()).isLockedPrim()) mangle = false;
					}
				}
            }
			if (mangle)
			{
				// other nodeinst untouched, mangle it
				double dx = newPts[thisEndIndex].getX() - thisLocation.getX();
				double dy = newPts[thisEndIndex].getY() - thisLocation.getY();
				double odx = newPts[thatEndIndex].getX() - thatLocation.getX();
				double ody = newPts[thatEndIndex].getY() - thatLocation.getY();
				if (DBMath.doublesEqual(thisLocation.getX(), thatLocation.getX()))
				{
					// null arcinst must not be explicitly horizontal
					if (!DBMath.doublesEqual(thisLocation.getY(), thatLocation.getY()) ||
						ai.getAngle() == 900 || ai.getAngle() == 2700)
					{
						// vertical arcinst: see if it really moved in X
						if (dx == odx) dx = odx = 0;

						// move horizontal, shrink vertical
						newPts[thatEndIndex].setLocation(newPts[thatEndIndex].getX() + dx-odx, newPts[thatEndIndex].getY());

						// see if next nodeinst need not be moved
						if (!DBMath.doublesEqual(dx, odx) && ai.isSlidable() && ai.stillInPort(thatEndIndex, newPts[thatEndIndex], true))
							dx = odx = 0;

						// if other node already moved, don't move it any more
						if (movedNodes.contains(ono)) dx = odx = 0;

						if (dx != odx)
						{
							double xAmount = DBMath.round(dx-odx);
							if (Layout.DEBUG) System.out.println("  Moving "+ono+" by ("+xAmount+",0)");
							alterNodeInst(ono, xAmount, 0, Orientation.IDENT);
						}
						if (Layout.DEBUG) System.out.println("  Moving vertical arc so head=("+newPts[ArcInst.HEADEND].getX()+","+newPts[ArcInst.HEADEND].getY()+
							") and tail=("+newPts[ArcInst.TAILEND].getX()+","+newPts[ArcInst.TAILEND].getY()+")");
						doMoveArcInst(ai, newPts[ArcInst.HEADEND], newPts[ArcInst.TAILEND], AI_FLEX);
						if (!DBMath.doublesEqual(dx, odx))
							modNodeArcs(ono, Orientation.IDENT);
						continue;
					}
				}
				if (DBMath.doublesEqual(thisLocation.getY(), thatLocation.getY()))
				{
					// horizontal arcinst: see if it really moved in Y
					if (DBMath.doublesEqual(dy, ody)) dy = ody = 0;

					// shrink horizontal, move vertical
					newPts[thatEndIndex].setLocation(newPts[thatEndIndex].getX(), newPts[thatEndIndex].getY() + dy-ody);

					// see if next nodeinst need not be moved
					if (!DBMath.doublesEqual(dy, ody) && ai.isSlidable() &&
						ai.stillInPort(thatEndIndex, newPts[thatEndIndex], true))
							dy = ody = 0;

					// if other node already moved, don't move it any more
					if (movedNodes.contains(ono)) dx = odx = 0;

					if (!DBMath.doublesEqual(dy, ody))
					{
						if (Layout.DEBUG) System.out.println("  Moving "+ono+" by (0,"+(dy-ody)+")");
						alterNodeInst(ono, 0, dy-ody, Orientation.IDENT);
					}
					if (Layout.DEBUG) System.out.println("  Moving horizontal arc so head=("+newPts[ArcInst.HEADEND].getX()+","+newPts[ArcInst.HEADEND].getY()+
						") and tail=("+newPts[ArcInst.TAILEND].getX()+","+newPts[ArcInst.TAILEND].getY()+")");
					doMoveArcInst(ai, newPts[ArcInst.HEADEND], newPts[ArcInst.TAILEND], AI_FLEX);
					if (!DBMath.doublesEqual(dy, ody))
					{
						modNodeArcs(ono, Orientation.IDENT);
					}
					continue;
				}

				/***** THIS CODE HANDLES ALL-ANGLE RIGIDITY WITH THE FIXED-ANGLE CONSTRAINT *****/

				// special code to handle nonorthogonal fixed-angles
				nonOrthogFixAng(ai, thisEnd, thisEndIndex, thatEndIndex, ono, newPts);
				dx = newPts[thatEndIndex].getX() - thatLocation.getX();
				dy = newPts[thatEndIndex].getY() - thatLocation.getY();

				// change the arc
				updateArc(ai, newPts[ArcInst.HEADEND], newPts[ArcInst.TAILEND], AI_FLEX);

				// if other node already moved, don't move it any more
				if (movedNodes.contains(ono)) dx = dy = 0;

				if (dx != 0 || dy != 0)
				{
					if (Layout.DEBUG) System.out.println("  Moving "+ono+" by ("+dx+","+dy+")");
					alterNodeInst(ono, dx, dy, Orientation.IDENT);
					modNodeArcs(ono, Orientation.IDENT);
				}
				continue;
			}

			// other node has changed or arc is funny, just use its position
			if (Layout.DEBUG) System.out.println("  Moving nonmanhattan arc so head=("+newPts[ArcInst.HEADEND].getX()+","+newPts[ArcInst.HEADEND].getY()+
				") and tail=("+newPts[ArcInst.TAILEND].getX()+","+newPts[ArcInst.TAILEND].getY()+")");
			doMoveArcInst(ai, newPts[ArcInst.HEADEND], newPts[ArcInst.TAILEND], AI_FLEX);
		}
	}

	/**
	 * Method to determine the motion of a nonorthogonal ArcInst given that one end has moved.
	 * The end that is "thisEnd" has moved to (ax[thisEndIndex],ay[thisEndIndex]), so this method
	 * must determine the coordinates of the other end and set (ax[thatEndIndex],ay[thatEndIndex]).
	 * @param ai the nonorthogonal ArcInst that is adjusting.
	 * @param thisEnd the Connection at one end of the ArcInst.
	 * @param thisEndIndex the index (0 or 1) of "thisEnd" of the ArcInst.
	 * @param thatEnd the Connection at the other end of the ArcInst.
	 * @param thatEndIndex the index (0 or 1) of "thatEnd" of the ArcInst.
	 * @param ono the node at the other end ("thatEnd").
	 * @param newPts an array of 2 points that defines the coordinates of the two ends (0: head, 1: tail).
	 */
	private void nonOrthogFixAng(ArcInst ai, Connection thisEnd, int thisEndIndex, int thatEndIndex,
		NodeInst ono, Point2D [] newPts)
	{
		// look for longest other arc on "ono" to determine proper end position
		double bestDist = Double.MIN_VALUE;
		ArcInst bestAI = null;
		for(Iterator<Connection> it = ono.getConnections(); it.hasNext(); )
		{
			Connection con = it.next();
			ArcInst oai = con.getArc();
			if (oai == ai) continue;
			if (oai.getLength() < bestDist) continue;
			bestDist = oai.getLength();
			bestAI = oai;
		}

		// if no other arcs, allow that end to move the same as this end
		if (bestAI == null)
		{
			newPts[thatEndIndex].setLocation(
				newPts[thatEndIndex].getX() + newPts[thisEndIndex].getX() - thisEnd.getLocation().getX(),
				newPts[thatEndIndex].getY() + newPts[thisEndIndex].getY() - thisEnd.getLocation().getY());
			return;
		}

		// compute intersection of arc "bestai" with new moved arc "ai"
		Point2D inter = DBMath.intersect(newPts[thisEndIndex], ai.getAngle(),
			bestAI.getHeadLocation(), bestAI.getAngle());
		if (inter == null)
		{
			newPts[thatEndIndex].setLocation(
				newPts[thatEndIndex].getX() + newPts[thisEndIndex].getX() - thisEnd.getLocation().getX(),
				newPts[thatEndIndex].getY() + newPts[thisEndIndex].getY() - thisEnd.getLocation().getY());
			return;
		}
		newPts[thatEndIndex].setLocation(inter);
	}

	/**
	 * Method to ensure that an ArcInst is still connected properly at each end.
	 * If it is not, the ArcInst must be jogged or adjusted.
	 * @param ai the ArcInst to check.
	 * @param arctyp the nature of the arc: 0 for rigid, 1 for flexible.
	 */
	private void ensureArcInst(ArcInst ai, Integer arctyp)
	{
		// if nothing is outside port, quit
		Point2D headPoint = ai.getHeadLocation();
		boolean inside0 = ai.headStillInPort(headPoint, true);
		Point2D tailPoint = ai.getTailLocation();
		boolean inside1 = ai.tailStillInPort(tailPoint, true);
		if (inside0 && inside1) return;

		// get area of the ports
		Poly headPoly = ai.getHeadPortInst().getPoly();
		Poly tailPoly = ai.getTailPortInst().getPoly();

		// if arcinst is not fixed-angle, run it directly to the port centers
		if (!ai.isFixedAngle())
		{
			double fx = headPoly.getCenterX();   double fy = headPoly.getCenterY();
			double tx = tailPoly.getCenterX();   double ty = tailPoly.getCenterY();
			doMoveArcInst(ai, new Point2D.Double(fx, fy), new Point2D.Double(tx, ty), arctyp);
			return;
		}

		// get bounding boxes of polygons
		Rectangle2D headBounds = headPoly.getBounds2D();
		Rectangle2D tailBounds = tailPoly.getBounds2D();
		double lx0 = headBounds.getMinX();   double hx0 = headBounds.getMaxX();
		double ly0 = headBounds.getMinY();   double hy0 = headBounds.getMaxY();
		double lx1 = tailBounds.getMinX();   double hx1 = tailBounds.getMaxX();
		double ly1 = tailBounds.getMinY();   double hy1 = tailBounds.getMaxY();

		// if manhattan path runs between the ports, adjust the arcinst
		if (lx0 <= hx1 && lx1 <= hx0)
		{
			// arcinst runs vertically
			double tx = (Math.max(lx0,lx1) + Math.min(hx0,hx1)) / 2;
			double fx = tx;
			double fy = (ly0+hy0) / 2;   double ty = (ly1+hy1) / 2;
			Point2D fPt = headPoly.closestPoint(new Point2D.Double(fx, fy));
			Point2D tPt = tailPoly.closestPoint(new Point2D.Double(tx, ty));
			doMoveArcInst(ai, fPt, tPt, arctyp);
			return;
		}
		if (ly0 <= hy1 && ly1 <= hy0)
		{
			// arcinst runs horizontally
			double ty = (Math.max(ly0,ly1) + Math.min(hy0,hy1)) / 2;
			double fy = ty;
			double fx = (lx0+hx0) / 2;   double tx = (lx1+hx1) / 2;
			Point2D fPt = headPoly.closestPoint(new Point2D.Double(fx, fy));
			Point2D tPt = tailPoly.closestPoint(new Point2D.Double(tx, ty));
			doMoveArcInst(ai, fPt, tPt, arctyp);
			return;
		}

		// give up and jog the arcinst
		double fx = headPoly.getCenterX();   double fy = headPoly.getCenterY();
		double tx = tailPoly.getCenterX();   double ty = tailPoly.getCenterY();
		doMoveArcInst(ai, new Point2D.Double(fx, fy), new Point2D.Double(tx, ty), arctyp);
	}

	/**
	 * Method to update the coordinates of the ends of an ArcInst.
	 * @param ai the ArcInst to adjust
	 * @param headPt the new coordinates of the head of the ArcInst.
	 * @param tailPt the new coordinates of the tail of the ArcInst.
	 * @param arctyp the nature of the arc: 0 for rigid, 1 for flexible.
	 */
	private void updateArc(ArcInst ai, Point2D headPt, Point2D tailPt, Integer arctyp)
	{
		// set the proper arcinst position
//		Point2D oldHeadPt = ai.getHeadLocation();
//		Point2D oldTailPt = ai.getTailLocation();
//		double oldHeadX = oldHeadPt.getX();   double oldHeadY = oldHeadPt.getY();
//		double oldTailX = oldTailPt.getX();   double oldTailY = oldTailPt.getY();
		// now make the change
        ImmutableArcInst oldD = ai.getD();
        ImmutableArcInst d = oldD;
        d = d.withLocations(EPoint.snap(tailPt), EPoint.snap(headPt));
		ai.lowLevelModify(d);
		if (Layout.DEBUG) System.out.println(ai + " now runs from tail ("+
			ai.getTailLocation().getX()+","+ai.getTailLocation().getY()+") to head ("+
			ai.getHeadLocation().getX()+","+ai.getHeadLocation().getY()+")");

		// if the arc hasn't changed yet, record this change
		if (oldArcs == null || !oldArcs.containsKey(ai))
		{
            Constraints.getCurrent().modifyArcInst(ai, oldD); // Is it necessary ?
			setChangeClock(ai, arctyp);
		}
	}

	/**
	 * Method to move the coordinates of the ends of an ArcInst.
	 * If the arc cannot be moved in this way, it will be broken up into 3 jogged arcs.
	 * @param ai the ArcInst to adjust
	 * @param headPt the new coordinates of the head of the ArcInst.
	 * @param tailPt the new coordinates of the tail of the ArcInst.
	 * @param arctyp the nature of the arc: 0 for rigid, 1 for flexible.
	 */
	private void doMoveArcInst(ArcInst ai, Point2D headPt, Point2D tailPt, Integer arctyp)
	{
		// check for null arcinst motion
		if (headPt.equals(ai.getHeadLocation()) && tailPt.equals(ai.getTailLocation()))
		{
			// only ignore null motion on fixed-angle requests
			if (arctyp.intValue() != 0) return;
		}

		// if the angle is the same or doesn't need to be, simply make the change
		if (!ai.isFixedAngle() || Layout.isRigid(ai) ||
			headPt.equals(tailPt) ||
			(ai.getAngle() % 1800) == (DBMath.figureAngle(tailPt, headPt) % 1800))
		{
			updateArc(ai, headPt, tailPt, arctyp);
			return;
		}

		// manhattan arcinst becomes nonmanhattan: remember facts about it
		if (Layout.DEBUG) System.out.println("Jogging arc");
		PortInst fpi = ai.getHeadPortInst();
		NodeInst fno = fpi.getNodeInst();   PortProto fpt = fpi.getPortProto();
		PortInst tpi = ai.getTailPortInst();
		NodeInst tno = tpi.getNodeInst();   PortProto tpt = tpi.getPortProto();

		ArcProto ap = ai.getProto();   Cell pnt = ai.getParent();   double wid = ai.getWidth();

		// figure out what nodeinst proto connects these arcs
		PrimitiveNode np = ap.findOverridablePinProto();
		double psx = np.getDefWidth();
		double psy = np.getDefHeight();

		// replace it with three arcs and two nodes
		NodeInst no1 = null, no2 = null;
		if (DBMath.doublesEqual(ai.getHeadLocation().getX(), ai.getTailLocation().getX()))
		{
			// arcinst was vertical
			double oldyA = (tailPt.getY()+headPt.getY()) / 2;
			double oldyB = oldyA;
			double oldxA = headPt.getX();   double oldxB = tailPt.getX();
			no1 = NodeInst.newInstance(np, new Point2D.Double(oldxB, oldyB),psx, psy, pnt);
			no2 = NodeInst.newInstance(np, new Point2D.Double(oldxA, oldyA),psx, psy, pnt);
		} else
		{
			// assume horizontal arcinst
			double oldyA = headPt.getY();   double oldyB = tailPt.getY();
			double oldxA = (tailPt.getX()+headPt.getX()) / 2;
			double oldxB = oldxA;
			no1 = NodeInst.newInstance(np, new Point2D.Double(oldxB, oldyB),psx, psy, pnt);
			no2 = NodeInst.newInstance(np, new Point2D.Double(oldxA, oldyA),psx, psy, pnt);
		}
		if (no1 == null || no2 == null)
		{
			System.out.println("Problem creating jog pins");
			return;
		}

		PortInst no1pi = no1.getOnlyPortInst();
		Rectangle2D no1Bounds = no1pi.getPoly().getBounds2D();
		Point2D no1Pt = new Point2D.Double(no1Bounds.getCenterX(), no1Bounds.getCenterY());

		PortInst no2pi = no2.getOnlyPortInst();
		Rectangle2D no2Bounds = no2pi.getPoly().getBounds2D();
		Point2D no2Pt = new Point2D.Double(no2Bounds.getCenterX(), no2Bounds.getCenterY());

		ArcInst ar1 = ArcInst.newInstance(ap, wid, fpi, no2pi, headPt, no2Pt, null, 0);
		if (ar1 == null) return;
        ar1.copyConstraintsFrom(ai);
//		ar1.copyStateBits(ai);
//		if (ai.isHeadNegated()) ar1.setHeadNegated(true);
		ArcInst ar2 = ArcInst.newInstance(ap, wid, no2pi, no1pi, no2Pt, no1Pt, null, 0);
		if (ar2 == null) return;
        ar2.copyPropertiesFrom(ai);
//		ar2.copyStateBits(ai);
		ArcInst ar3 = ArcInst.newInstance(ap, wid, no1pi, tpi, no1Pt, tailPt, null, 0);
		if (ar3 == null) return;
        ar3.copyConstraintsFrom(ai);
//		ar3.copyStateBits(ai);
		if (ai.isTailNegated()) ar3.setTailNegated(true);
		if (ar1 == null || ar2 == null || ar3 == null)
		{
			System.out.println("Problem creating jog arcs");
			return;
		}
//		ar2.copyVarsFrom(ai);
//		ar2.copyTextDescriptorFrom(ai, ArcInst.ARC_NAME_TD);
		setChangeClock(ar1, arctyp);
		setChangeClock(ar2, arctyp);
		setChangeClock(ar3, arctyp);

		// now kill the arcinst
		ar2.copyTextDescriptorFrom(ai, ArcInst.ARC_NAME);
		ai.kill();
		String oldName = ai.getName();
		if (oldName != null) ar2.setName(oldName);
	}

	/**
	 * Method to return transformation matrix which
     * transforms old geometry of portinst "pi" to the new one.
	 *
	 * there are only two types of nodeinst changes: internal and external.
	 * The internal changes are scaling and port motion changes that
	 * are usually caused by other changes within the cell.  The external
	 * changes are rotation and transposition.  These two changes never
	 * occur at the same time.  There is also translation change that
	 * can occur at any time and is of no importance here.  What is
	 * important is that the transformation matrix "trans" handles
	 * the external changes and internal changes.  External changes are already
	 * set by the "makeangle" method and internal changes are
	 * built into the matrix here.
	 */
	private AffineTransform transformByPort(PortInst pi)
	{
        NodeInst ni = pi.getNodeInst();
        ImmutableNodeInst d = getOldD(ni);
        
        // Identity transform for newly created nodes
        if (d == null)
            return new AffineTransform();
        
        if (!ni.getOrient().equals(d.orient)) {
            Orientation dOrient = ni.getOrient().concatenate(d.orient.inverse());
            return dOrient.rotateAbout(ni.getAnchorCenterX(), ni.getAnchorCenterY(), -d.anchor.getX(), -d.anchor.getY());
        }
        
        // nodeinst did not rotate or mirror: adjust for port motion or sizing
        PortProto pp = pi.getPortProto();
		Poly oldPoly = Layout.oldPortPosition(pi);
        Poly curPoly = ni.getShapeOfPort(pp);
        if (oldPoly == null) oldPoly = curPoly;
        
		double scaleX = 1;
		double scaleY = 1;
		// Zero means flat port or artwork. Valid for new technology
		if (oldPoly.getBounds2D().getWidth() > 0)
			scaleX = curPoly.getBounds2D().getWidth() / oldPoly.getBounds2D().getWidth();
		if (oldPoly.getBounds2D().getHeight() >0)
			scaleY = curPoly.getBounds2D().getHeight() / oldPoly.getBounds2D().getHeight();

        double newX = curPoly.getCenterX() - oldPoly.getCenterX()*scaleX;
        double newY = curPoly.getCenterY() - oldPoly.getCenterY()*scaleY;
        return new AffineTransform(scaleX, 0, 0, scaleY, newX, newY);
	}

    /*-- Change clock stuff --*/
    private void setChangeClock(Geometric geom, Integer typ) {
//        if (typ != null)
//            changeClock.put(geom, typ);
//        else
//            changeClock.remove(geom);
    }
//    
//    private int getChangeClock(Geometric geom) {
//        Integer i = changeClock.get(geom);
//        return i != null ? i.intValue() : Integer.MIN_VALUE;
//    }
  
	/**
	 * Method to announce a change to a NodeInst.
	 * @param ni the NodeInst that was changed.
	 * @param oD the old contents of the NodeInst.
	 */
    void modifyNodeInst(NodeInst ni, ImmutableNodeInst oldD) {
        modified = true;
        if (!oldNodes.containsKey(ni))
            oldNodes.put(ni, oldD);
    }
    
	/**
	 * Method to announce a change to an ArcInst.
	 * @param ai the ArcInst that changed.
	 * @param oD the old contents of the ArcInst.
	 */
	void modifyArcInst(ArcInst ai, ImmutableArcInst oldD) {
        modified = true;
        if (oldArcs == null) oldArcs = new HashMap<ArcInst,ImmutableArcInst>();
        if (!oldArcs.containsKey(ai))
            oldArcs.put(ai, oldD);
    }
    
	/**
	 * Method to announce a change to an Export.
	 * @param pp the Export that moved.
	 * @param oldPi the old PortInst on which it resided.
	 */
	void modifyExport(Export pp, PortInst oldPi) {
        exportsModified = true;
        if (oldExports == null) oldExports = new HashMap<Export,PortInst>();
        if (!oldExports.containsKey(pp))
            oldExports.put(pp, oldPi);
    }
    
	/**
	 * Method to announce the creation of a new ElectricObject.
	 * @param obj the ElectricObject that was just created.
	 */
	void newObject(ElectricObject obj) {
        if (obj instanceof Export) {
            if (oldExports == null) oldExports = new HashMap<Export,PortInst>();
            assert !oldExports.containsKey(obj);
            oldExports.put((Export)obj, null);
        } else if (obj instanceof NodeInst) {
            assert !oldNodes.containsKey(obj);
            oldNodes.put((NodeInst)obj, null);
        } else if (obj instanceof ArcInst) {
            if (oldArcs == null) oldArcs = new HashMap<ArcInst,ImmutableArcInst>();
            assert !oldArcs.containsKey(obj);
            oldArcs.put((ArcInst)obj, null);
        }
    }

    PortInst getOldOriginalPort(Export e) {
        if (oldExports == null || !oldExports.containsKey(e))
            return e.getOriginalPort();
        return oldExports.get(e);
    }

    ImmutableNodeInst getOldD(NodeInst ni) {
        if (oldNodes == null || !oldNodes.containsKey(ni))
            return ni.getD();
        return oldNodes.get(ni);
    }
    
    boolean arcMoved(ArcInst ai) {
        if (oldArcs == null || !oldArcs.containsKey(ai))
            return false;
        ImmutableArcInst oldD = oldArcs.get(ai);
        if (oldD == null) return false;
        return ai.getHeadLocation() != oldD.headLocation || ai.getTailLocation() != oldD.tailLocation;
    }
}

//    private HashMap/*<NodeInst,RigidCluster>*/ makeRigidClusters() {
//       TransitiveRelation rel = new TransitiveRelation();
//       for (Iterator it = cell.getArcs(); it.hasNext(); ) {
//           ArcInst ai = it.next();
//           if (Layout.isRigid(ai))
//               rel.theseAreRelated(ai.getHeadPortInst().getNodeInst(), ai.getTailPortInst().getNodeInst());
//       }
//       HashMap/*<NodeInst,RigidCluster>*/ nodeClusters = new HashMap/*<NodeInst,RigidCluster>*/();
//       for (Iterator it = cell.getNodes(); it.hasNext(); ) {
//           NodeInst ni = it.next();
//           rel.theseAreRelated(ni, ni);
//           RigidCluster rc = nodeClusters.get(ni);
//           if (rc == null) {
//               rc = new RigidCluster();
//               for (Iterator rIt = rel.getSetsOfRelatives(); rIt.hasNext(); ) {
//                   NodeInst rNi = rIt.next();
//                   rc.add(rNi);
//                   nodeClusters.put(rNi, rc);
//               }
//           }
//           ImmutableNodeInst d = ni.getOldD();
//           if (d != null && !(d.anchor.equals(ni.getAnchorCenter()) && d.orient.equals(ni.getOrient()))) {
//               rc.touched = true;
//               rc.locked = true;
//           }
//           if (ni.isCellInstance()) {
//                LayoutCell ci = Layout.getCellInfo((Cell)ni.getProto());
//                if (ci.exportsModified) rc.touched = true;
//           } else if (d != null && (d.width != ni.getXSize() || d.height != ni.getYSize())) {
//               rc.touched = true;
//           }
//       }
//       return nodeClusters;
//    }
//    
//class RigidCluster {
//    boolean touched;
//    boolean locked;
//    HashSet/*<NodeInst>*/ nodes = new HashSet/*<NodeInst>*/();
//    
//    void add(NodeInst ni) { nodes.add(ni); }
//}
