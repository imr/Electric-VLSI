/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ArcInst.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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

import com.sun.electric.database.EObjectInputStream;
import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.PrimitivePortId;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.DisplayedText;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.BoundsBuilder;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Comparator;

/**
 * An ArcInst is an instance of an ArcProto (a wire type)
 * An ArcInst points to its prototype, the Cell on which it has been
 * instantiated, and the connection at either end of the wire.
 * The geometry of the wire (width and length) is captured in the
 * bounds of the Geometric portion of this object.
 * <P>
 * ArcInst objects have properties that constrain them.  Here is the notion of "Fixed angle":
 * <P>
 * <CENTER><IMG SRC="doc-files/ArcInst-1.gif"></CENTER>
 * <P>
 * Here is the notion of rigid arcs:
 * <P>
 * <CENTER><IMG SRC="doc-files/ArcInst-2.gif"></CENTER>
 * <P>
 * Here is the notion of slidable arcs:
 * <P>
 * <CENTER><IMG SRC="doc-files/ArcInst-3.gif"></CENTER>
 * <P>
 * Constraints propagate hierarchically:
 * <P>
 * <CENTER><IMG SRC="doc-files/ArcInst-4.gif"></CENTER>
 */
public class ArcInst extends Geometric implements Comparable<ArcInst> {

    /** empty array of ArcInsts. */
    public static final ArcInst[] NULL_ARRAY = {};
    /** The index of the tail of this ArcInst. */
    public static final int TAILEND = ImmutableArcInst.TAILEND;
    /** The index of the head of this ArcInst. */
    public static final int HEADEND = ImmutableArcInst.HEADEND;
    /** Key of the obsolete variable holding arc name.*/
    public static final Variable.Key ARC_NAME = Variable.newKey("ARC_name");
    /** Minimal distance of arc end to port polygon. */
    static final double MINPORTDISTANCE = DBMath.getEpsilon() * 0.71; // sqrt(0.5)
    // -------------------------- private data ----------------------------------
    /** Owner of this ArcInst. */
    private final Topology topology;
    /** persistent data of this ArcInst. */
    ImmutableArcInst d;
    /** bounds after transformation. */
    private final Rectangle2D.Double visBounds = new Rectangle2D.Double();
    /** PortInst on tail end of this arc instance */	/*package*/

    final PortInst tailPortInst;
    /** PortInst on head end of this arc instance */	/*package*/

    final PortInst headPortInst;

//	/** 0-based index of this ArcInst in Cell. */			private int arcIndex = -1; //scanline
    /**
     * Private constructor of ArcInst.
     * @param topology the Topology of the ArcInst.
     * @param d persistent data of ArcInst.
     * @param headPort the head end PortInst.
     * @param tailPort the tail end PortInst.
     */
    public ArcInst(Topology topology, ImmutableArcInst d, PortInst headPort, PortInst tailPort) {
        this.topology = topology;

        // initialize this object
        assert topology == headPort.getNodeInst().topology;
        assert topology == tailPort.getNodeInst().topology;
        assert d.headNodeId == headPort.getNodeInst().getD().nodeId;
        assert d.tailNodeId == tailPort.getNodeInst().getD().nodeId;
        assert d.headPortId == headPort.getPortProto().getId();
        assert d.tailPortId == tailPort.getPortProto().getId();

        this.d = d;

        // create node/arc connections and place them properly
        tailPortInst = tailPort;
//		tailEnd = new TailConnection(this);

        headPortInst = headPort;
//		headEnd = new HeadConnection(this);
    }

    private Object writeReplace() {
        return new ArcInstKey(this);
    }

    private static class ArcInstKey extends EObjectInputStream.Key<ArcInst> {

        public ArcInstKey() {
        }

        private ArcInstKey(ArcInst ai) {
            super(ai);
        }

        @Override
        public void writeExternal(EObjectOutputStream out, ArcInst ai) throws IOException {
            if (ai.getDatabase() != out.getDatabase() || !ai.isLinked()) {
                throw new NotSerializableException(ai + " not linked");
            }
            out.writeObject(ai.topology.cell);
            out.writeInt(ai.getArcId());
        }

        @Override
        public ArcInst readExternal(EObjectInputStream in) throws IOException, ClassNotFoundException {
            Cell cell = (Cell) in.readObject();
            int arcId = in.readInt();
            ArcInst ai = cell.getArcById(arcId);
            if (ai == null) {
                throw new InvalidObjectException("ArcInst from " + cell);
            }
            return ai;
        }
    }

    /**
     * Comparator class for sorting ArcInst by their length.
     */
    public static class ArcsByLength implements Comparator<ArcInst> {

        /**
         * Method to sort ArcInst by their length.
         */
        public int compare(ArcInst a1, ArcInst a2) {
            double len1 = a1.getHeadLocation().distance(a1.getTailLocation());
            double len2 = a2.getHeadLocation().distance(a2.getTailLocation());
            if (len1 == len2) {
                return 0;
            }
            if (len1 < len2) {
                return 1;
            }
            return -1;
        }
    }

    /****************************** CREATE, DELETE, MODIFY ******************************/
    /**
     * Method to create a new ArcInst with appropriate defaults, connecting two PortInsts.
     * Since no coordinates are given, the ArcInst connects to the center of the PortInsts.
     * @param type the prototype of the new ArcInst.
     * @param head the head end PortInst.
     * @param tail the tail end PortInst.
     * @return the newly created ArcInst, or null if there is an error.
     */
    public static ArcInst makeInstance(ArcProto type, PortInst head, PortInst tail) {
        EditingPreferences ep = tail.getEditingPreferences();
        ImmutableArcInst a = type.getDefaultInst(ep);
        return newInstanceBase(type, type.getDefaultLambdaBaseWidth(ep), head, tail, null, null, null, 0, a.flags);
    }

    /**
     * Method to create a new ArcInst with appropriate defaults, connecting two PortInsts.
     * Since no coordinates are given, the ArcInst connects to the center of the PortInsts.
     * @param type the prototype of the new ArcInst.
     * @param baseWidth the base width of the new ArcInst.  The width must be > 0.
     * @param head the head end PortInst.
     * @param tail the tail end PortInst.
     * @return the newly created ArcInst, or null if there is an error.
     */
    public static ArcInst makeInstanceBase(ArcProto type, double baseWidth, PortInst head, PortInst tail) {
        EditingPreferences ep = tail.getEditingPreferences();
        ImmutableArcInst a = type.getDefaultInst(ep);
        return newInstanceBase(type, baseWidth, head, tail, null, null, null, 0, a.flags);
    }

    /**
     * Method to create a new ArcInst with appropriate defaults, connecting two PortInsts at specified locations.
     * This is more general than the version that does not take coordinates.
     * @param type the prototype of the new ArcInst.
     * @param head the head end PortInst.
     * @param tail the tail end PortInst.
     * @param headPt the coordinate of the head end PortInst.
     * @param tailPt the coordinate of the tail end PortInst.
     * @param name the name of the new ArcInst
     * @return the newly created ArcInst, or null if there is an error.
     */
    public static ArcInst makeInstance(ArcProto type, PortInst head, PortInst tail,
            Point2D headPt, Point2D tailPt, String name) {
        EditingPreferences ep = tail.getEditingPreferences();
        ImmutableArcInst a = type.getDefaultInst(ep);
        return newInstanceBase(type, type.getDefaultLambdaBaseWidth(ep), head, tail, headPt, tailPt, name, 0, a.flags);
    }

    /**
     * Method to create a new ArcInst with appropriate defaults, connecting two PortInsts at specified locations.
     * This is more general than the version that does not take coordinates.
     * @param type the prototype of the new ArcInst.
     * @param baseWidth the base width of the new ArcInst.  The width must be > 0.
     * @param head the head end PortInst.
     * @param tail the tail end PortInst.
     * @param headPt the coordinate of the head end PortInst.
     * @param tailPt the coordinate of the tail end PortInst.
     * @param name the name of the new ArcInst
     * @return the newly created ArcInst, or null if there is an error.
     */
    public static ArcInst makeInstanceBase(ArcProto type, double baseWidth, PortInst head, PortInst tail,
            Point2D headPt, Point2D tailPt, String name) {
        EditingPreferences ep = tail.getEditingPreferences();
        ImmutableArcInst a = type.getDefaultInst(ep);
        return newInstanceBase(type, baseWidth, head, tail, headPt, tailPt, name, 0, a.flags);
    }

    /**
     * Method to create a new ArcInst connecting two PortInsts.
     * Since no coordinates are given, the ArcInst connects to the center of the PortInsts.
     * @param type the prototype of the new ArcInst.
     * @param baseWidth the base width of the new ArcInst.  The width must be > 0.
     * @param head the head end PortInst.
     * @param tail the tail end PortInst.
     * @return the newly created ArcInst, or null if there is an error.
     */
    public static ArcInst newInstanceBase(ArcProto type, double baseWidth, PortInst head, PortInst tail) {
        return newInstanceBase(type, baseWidth, head, tail, null, null, null, 0, ImmutableArcInst.DEFAULT_FLAGS);
    }

    /**
     * Method to create a new ArcInst connecting two PortInsts at specified locations.
     * This is more general than the version that does not take coordinates.
     * @param type the prototype of the new ArcInst.
     * @param baseWidth the base width of the new ArcInst.  The width must be > 0.
     * @param head the head end PortInst.
     * @param tail the tail end PortInst.
     * @param headPt the coordinate of the head end PortInst.
     * @param tailPt the coordinate of the tail end PortInst.
     * @param name the name of the new ArcInst
     * @param defAngle default angle in case port points coincide
     * @return the newly created ArcInst, or null if there is an error.
     */
    public static ArcInst newInstanceBase(ArcProto type, double baseWidth, PortInst head, PortInst tail,
            Point2D headPt, Point2D tailPt, String name, int defAngle) {
        return newInstanceBase(type, baseWidth, head, tail, headPt, tailPt, name, defAngle, ImmutableArcInst.DEFAULT_FLAGS);
    }

    /**
     * Method to create a new ArcInst connecting two PortInsts at specified locations.
     * This is more general than the version that does not take coordinates.
     * @param type the prototype of the new ArcInst.
     * @param baseWidth the base width of the new ArcInst.  The width must be > 0.
     * @param head the head end PortInst.
     * @param tail the tail end PortInst.
     * @param headPt the coordinate of the head end PortInst.
     * @param tailPt the coordinate of the tail end PortInst.
     * @param name the name of the new ArcInst
     * @param defAngle default angle in case port points coincide
     * @param flags flags of thew new ArcInst
     * @return the newly created ArcInst, or null if there is an error.
     */
    public static ArcInst newInstanceBase(ArcProto type, double baseWidth, PortInst head, PortInst tail,
            Point2D headPt, Point2D tailPt, String name, int defAngle, int flags) {
//        if (type.isNotUsed())
//        {
////            System.out.println("Cannot create arc instance of " + type + " because prototype is unused");
////            return null;
//        }

        long gridExtendOverMin = DBMath.lambdaToGrid(0.5 * baseWidth) - type.getGridBaseExtend();
//		if (gridFullWidth < type.getMaxLayerGridOffset())
//			gridFullWidth = type.getDefaultGridFullWidth();

        // if points are null, create them as would newInstance
        EPoint headP;
        if (headPt == null) {
//            Rectangle2D headBounds = head.getBounds();
            headP = head.getCenter(); //new EPoint(headBounds.getCenterX(), headBounds.getCenterY());
        } else {
            headP = EPoint.snap(headPt);
        }
        EPoint tailP;
        if (tailPt == null) {
//            Rectangle2D tailBounds = tail.getBounds();
            tailP = tail.getCenter(); // new EPoint(tailBounds.getCenterX(), tailBounds.getCenterY());
        } else {
            tailP = EPoint.snap(tailPt);
        }

        // make sure points are valid
        Cell parent = head.getNodeInst().topology.cell;
        Poly headPoly = head.getPoly();
        if (!stillInPoly(headP, headPoly)) {
            System.out.println("Error in " + parent + ": head of " + type.getName()
                    + " arc at (" + headP.getX() + "," + headP.getY() + ") does not fit in "
                    + head + " which is centered at (" + headPoly.getCenterX() + "," + headPoly.getCenterY() + ")");
            return null;
        }
        Poly tailPoly = tail.getPoly();
        if (!stillInPoly(tailP, tailPoly)) {
            System.out.println("Error in " + parent + ": tail of " + type.getName()
                    + " arc at (" + tailP.getX() + "," + tailP.getY() + ") does not fit in "
                    + tail + " which is centered at (" + tailPoly.getCenterX() + "," + tailPoly.getCenterY() + ")");
            return null;
        }

        TextDescriptor nameDescriptor = TextDescriptor.getArcTextDescriptor();
        Name nameKey = name != null ? Name.findName(name) : null;
        if (nameKey != null && !nameKey.isTempname()) {
            // adjust the name descriptor for "smart" text placement
            long gridBaseWidth = 2 * (gridExtendOverMin + type.getGridBaseExtend());
            nameDescriptor = getSmartTextDescriptor(defAngle, DBMath.gridToLambda(gridBaseWidth), nameDescriptor, parent.getEditingPreferences());
        }
        return newInstance(parent, type, name, nameDescriptor, head, tail, headP, tailP, gridExtendOverMin, defAngle, flags);
    }

    /**
     * Method to create a new ArcInst connecting two PortInsts at specified locations.
     * This is more general than the version that does not take coordinates.
     * @param parent the parent Cell of this ArcInst
     * @param protoType the ArcProto of this ArcInst.
     * @param name the name of this ArcInst
     * @param nameDescriptor text descriptor of name of this ArcInst
     * @param headPort the head end PortInst.
     * @param tailPort the tail end PortInst.
     * @param headPt the coordinate of the head end PortInst.
     * @param tailPt the coordinate of the tail end PortInst.
     * @param gridExtendOverMin the extend of this ArcInst over minimal-width arc of this type in grid units.
     * @param angle angle in tenth-degrees.
     * @param flags flag bits.
     * @return the newly created ArcInst, or null if there is an error.
     */
    public static ArcInst newInstance(Cell parent, ArcProto protoType, String name, TextDescriptor nameDescriptor,
            PortInst headPort, PortInst tailPort, EPoint headPt, EPoint tailPt, long gridExtendOverMin, int angle, int flags) {
        parent.checkChanging();
        Topology topology = parent.getTopology();

        // make sure fields are valid
        if (protoType == null || headPort == null || tailPort == null || !headPort.isLinked() || !tailPort.isLinked()) {
            return null;
        }
        if (headPt == null || tailPt == null) {
            return null;
        }

        if (topology != headPort.getNodeInst().topology || topology != tailPort.getNodeInst().topology) {
            System.out.println("ArcProto.newInst: the 2 PortInsts are in different Cells!");
            System.out.println("Cell " + parent.getName());
            System.out.println("Head " + headPort.getNodeInst().topology.cell.getName());
            System.out.println("Tail " + tailPort.getNodeInst().topology.cell.getName());
            return null;
        }

        // make sure the arc can connect to these ports
        PortProto headProto = headPort.getPortProto();
        PrimitivePort headPrimPort = headProto.getBasePort();
        if (!headPrimPort.connectsTo(protoType)) {
            System.out.println("Cannot create " + protoType + " from (" + headPt.getX() + "," + headPt.getY()
                    + ") to (" + tailPt.getX() + "," + tailPt.getY() + ") in " + parent
                    + " because it cannot connect to port " + headProto.getName()
                    + " on node " + headPort.getNodeInst().describe(false));
            return null;
        }
        PortProto tailProto = tailPort.getPortProto();
        PrimitivePort tailPrimPort = tailProto.getBasePort();
        if (!tailPrimPort.connectsTo(protoType)) {
            System.out.println("Cannot create " + protoType + " from (" + headPt.getX() + "," + headPt.getY()
                    + ") to (" + tailPt.getX() + "," + tailPt.getY() + ") in " + parent
                    + " because it cannot connect to port " + tailProto.getName()
                    + " on node " + tailPort.getNodeInst().describe(false));
            return null;
        }

        if (nameDescriptor == null) {
            nameDescriptor = TextDescriptor.getArcTextDescriptor();
        }
        Name nameKey = name != null ? Name.findName(name) : null;
        if (nameKey == null || checkNameKey(nameKey, topology)
                || nameKey.isBus() && protoType != Schematics.tech().bus_arc) {
            nameKey = topology.getArcAutoname();
//		} else
//		{
//			// adjust the name descriptor for "smart" text placement
//            long gridBaseWidth = 2*(gridExtendOverMin + protoType.getGridBaseExtend());
//			nameDescriptor = getSmartTextDescriptor(angle, DBMath.gridToLambda(gridBaseWidth), nameDescriptor);
        }
        TechPool techPool = parent.getTechPool();
        if (!(tailProto.getId() instanceof PrimitivePortId && techPool.getPrimitivePort((PrimitivePortId) tailProto.getId()).isNegatable())) {
            flags = ImmutableArcInst.TAIL_NEGATED.set(flags, false);
        }
        if (!(headProto.getId() instanceof PrimitivePortId && techPool.getPrimitivePort((PrimitivePortId) headProto.getId()).isNegatable())) {
            flags = ImmutableArcInst.HEAD_NEGATED.set(flags, false);
        }
        if (protoType.getTechnology().isNoNegatedArcs()) {
            flags = ImmutableArcInst.TAIL_NEGATED.set(flags, false);
            flags = ImmutableArcInst.HEAD_NEGATED.set(flags, false);
        }

        CellId parentId = parent.getId();
        // search for spare arcId
        int arcId;
        do {
            arcId = parentId.newArcId();
        } while (parent.getArcById(arcId) != null);
        ImmutableArcInst d = ImmutableArcInst.newInstance(arcId, protoType.getId(), nameKey, nameDescriptor,
                tailPort.getNodeInst().getD().nodeId, tailProto.getId(), tailPt,
                headPort.getNodeInst().getD().nodeId, headProto.getId(), headPt,
                gridExtendOverMin, angle, flags);
        ArcInst ai = new ArcInst(topology, d, headPort, tailPort);

        // attach this arc to the two nodes it connects
        headPort.getNodeInst().redoGeometric();
        tailPort.getNodeInst().redoGeometric();

        // add this arc to the cell
        topology.addArc(ai);

        // handle change control, constraint, and broadcast
        Constraints.getCurrent().newObject(ai);
        return ai;
    }

    /**
     * Method to delete this ArcInst.
     */
    public void kill() {
        if (!isLinked()) {
            System.out.println("ArcInst already killed");
            return;
        }
        checkChanging();

        // remove this arc from the two nodes it connects
        headPortInst.getNodeInst().redoGeometric();
        tailPortInst.getNodeInst().redoGeometric();

        // remove this arc from the cell
        topology.removeArc(this);

        // handle change control, constraint, and broadcast
        Constraints.getCurrent().killObject(this);
    }

    /**
     * Method to change the width and end locations of this ArcInst.
     * @param dHeadX the change to the X coordinate of the head of this ArcInst.
     * @param dHeadY the change to the Y coordinate of the head of this ArcInst.
     * @param dTailX the change to the X coordinate of the tail of this ArcInst.
     * @param dTailY the change to the Y coordinate of the tail of this ArcInst.
     */
    public void modify(double dHeadX, double dHeadY, double dTailX, double dTailY) {
        // save old arc state
        ImmutableArcInst oldD = d;

        // change the arc
        EPoint tail = d.tailLocation;
        if (dTailX != 0 || dTailY != 0) {
            tail = new EPoint(tail.getX() + dTailX, tail.getY() + dTailY);
        }
        EPoint head = d.headLocation;
        if (dHeadX != 0 || dHeadY != 0) {
            head = new EPoint(head.getX() + dHeadX, head.getY() + dHeadY);
        }
        lowLevelModify(d.withLocations(tail, head));

        // track the change
        Constraints.getCurrent().modifyArcInst(this, oldD);
    }

    /**
     * Method to change the width this ArcInst.
     * @param lambdaBaseWidth new base width of the ArcInst in lambda units.
     */
    public void setLambdaBaseWidth(double lambdaBaseWidth) {
        setGridBaseWidth(DBMath.lambdaToSizeGrid(lambdaBaseWidth));
    }

    /**
     * Method to change the width this ArcInst.
     * @param gridBaseWidth new base width of the ArcInst in lambda units.
     */
    public void setGridBaseWidth(long gridBaseWidth) {
        if (gridBaseWidth == getGridBaseWidth()) {
            return;
        }

        // save old arc state
        ImmutableArcInst oldD = d;

        // change the arc
        lowLevelModify(d.withGridExtendOverMin(gridBaseWidth / 2 - getProto().getGridBaseExtend()));

        // track the change
        Constraints.getCurrent().modifyArcInst(this, oldD);
    }

    /**
     * Method to replace this ArcInst with one of another type.
     * @param ap the new type of arc.
     * @return the new ArcInst (null on error).
     */
    public ArcInst replace(ArcProto ap) {
        // check for connection allowance
        if (!headPortInst.getPortProto().connectsTo(ap) || !tailPortInst.getPortProto().connectsTo(ap)) {
            System.out.println("Cannot replace " + this + " with one of type " + ap.getName()
                    + " because the nodes cannot connect to it");
            return null;
        }

        // first create the new nodeinst in place
        ArcInst newar = ArcInst.newInstanceBase(ap, getLambdaBaseWidth(), headPortInst, tailPortInst, d.headLocation, d.tailLocation, null, 0);
        if (newar == null) {
            System.out.println("Cannot replace " + this + " with one of type " + ap.getName()
                    + " because the new arc failed to create");
            return null;
        }

        // copy all variables on the arcinst
        newar.copyPropertiesFrom(this);

        // now delete the original nodeinst
        kill();
        newar.setName(getName());
        return newar;
    }

    /****************************** LOW-LEVEL IMPLEMENTATION ******************************/
    /**
     * Returns persistent data of this ArcInst.
     * @return persistent data of this ArcInst.
     */
    @Override
    public ImmutableArcInst getD() {
        return d;
    }

    /**
     * Modifies persistend data of this ArcInst.
     * @param newD new persistent data.
     * @param notify true to notify Undo system.
     * @return true if persistent data was modified.
     */
    public boolean setD(ImmutableArcInst newD, boolean notify) {
        checkChanging();
        ImmutableArcInst oldD = d;
        if (newD == oldD) {
            return false;
        }
        topology.cell.setTopologyModified();
        d = newD;
        if (notify) {
            Constraints.getCurrent().modifyArcInst(this, oldD);
        }
        return true;
    }

    public void setDInUndo(ImmutableArcInst newD) {
        checkUndoing();
        d = newD;
    }

    /**
     * Method to add a Variable on this ArcInst.
     * It may add repaired copy of this Variable in some cases.
     * @param var Variable to add.
     */
    public void addVar(Variable var) {
        if (setD(d.withVariable(var), true)) {
            // check for side-effects of the change
            checkPossibleVariableEffects(var.getKey());
        }
    }

    /**
     * Method to handle special case side-effects of setting variables on this NodeInst.
     * Overrides the general method on ElectricObject.
     * Currently it handles changes to the number-of-degrees on a circle node.
     * @param key the Variable key that has changed on this NodeInst.
     */
    public void checkPossibleVariableEffects(Variable.Key key) {
        if (key == ImmutableArcInst.ARC_RADIUS) {
            lowLevelModify(d);
        }
    }

    /**
     * Method to delete a Variable from this ArcInst.
     * @param key the key of the Variable to delete.
     */
    public void delVar(Variable.Key key) {
        setD(d.withoutVariable(key), true);
    }

    /**
     * Low-level method to change the width and end locations of this ArcInst.
     * New persistent data may differ from old one only by width and end locations
     * @param d the new persistent data of this ArcInst.
     */
    public void lowLevelModify(ImmutableArcInst d) {
        // first remove from the R-Tree structure
        boolean renamed = this.d.name != d.name;
        if (renamed) {
            topology.removeArc(this);
        }

        // now make the change
        setD(d, false);
        if (renamed) {
            topology.addArc(this);
        }

        topology.setArcsDirty();

        // update end shrinkage information ?????????????????????
//		headPortInst.getNodeInst().updateShrinkage();
//		tailPortInst.getNodeInst().updateShrinkage();
    }

    /****************************** GRAPHICS ******************************/
    /**
     * Method to return the full width of this ArcInst in grid units.
     * @return the full width of this ArcInst in grid units.
     */
    public long getGridFullWidth() {
        return 2 * (d.getGridExtendOverMin() + getProto().getMaxLayerGridExtend());
    }

    /**
     * Method to return the base width of this ArcInst in lambda units.
     * @return the base width of this ArcInst in lambda units.
     */
    public double getLambdaBaseWidth() {
        return DBMath.gridToLambda(getGridBaseWidth());
    }

    /**
     * Method to return the base width of this ArcInst in grid units.
     * @return the base width of this ArcInst in grid units.
     */
    public long getGridBaseWidth() {
        return 2 * (d.getGridExtendOverMin() + getProto().getGridBaseExtend());
    }

    /**
     * Method to return the length of this ArcInst in lambda units.
     * @return the length of this ArcInst in lambda units.
     */
    public double getLambdaLength() {
        return d.getLambdaLength();
    }

    /**
     * Method to return the length of this ArcInst in grid units.
     * @return the length of this ArcInst in grid units.
     */
    public double getGridLength() {
        return d.getGridLength();
    }

    /**
     * Returns true if length of this ArcInst is zero.
     * @return true if length of this ArcInst is zero.
     */
    public boolean isZeroLength() {
        return d.isZeroLength();
    }

    /**
     * Method to return the rotation angle of this ArcInst.
     * This is an angle of direction from tailLocation to headLocation.
     * @return the rotation angle of this ArcInst (in tenth-degrees).
     */
    public int getAngle() {
        return d.getAngle();
    }

    /**
     * Method to set the rotation angle of this ArcInst.
     * @param angle the rotation angle of this ArcInst (in tenth-degrees).
     * In general, you should not call this method because the
     * constructors and modification methods update this correctly.
     * If, however, you have a zero-length arc and want to explicitly set
     * its angle, then use this method.
     */
    public void setAngle(int angle) {
        checkChanging();
        ImmutableArcInst oldD = d;
        lowLevelModify(d.withAngle(angle));
        assert topology != null;
        Constraints.getCurrent().modifyArcInst(this, oldD);
    }

    /**
     * Returns the polygons that describe this ArcInst.
     * @param polyBuilder Poly builder.
     * @return an iterator on Poly objects that describes this ArcInst graphically.
     * These Polys include displayable variables on the ArcInst.
     */
    @Override
    public Iterator<Poly> getShape(Poly.Builder polyBuilder) {
        return polyBuilder.getShape(this);
    }

    /**
     * Method to return the bounds of this ArcInst.
     * TODO: dangerous to give a pointer to our internal field; should make a copy of visBounds
     * @return the bounds of this ArcInst.
     */
    @Override
    public Rectangle2D getBounds() {
        if (!topology.validArcBounds) {
            topology.computeArcBounds();
        }
        return visBounds;
    }

    void computeBounds(BoundsBuilder b, int[] intCoords) {
        if (b.genBoundsEasy(d, intCoords)) {
            double x = intCoords[0];
            double y = intCoords[1];
            double w = intCoords[2] - x;
            double h = intCoords[3] - y;
            x = DBMath.gridToLambda(x);
            y = DBMath.gridToLambda(y);
            w = DBMath.gridToLambda(w);
            h = DBMath.gridToLambda(h);
            if (x == visBounds.getX() && y == visBounds.getY() && w == visBounds.getWidth() && h == visBounds.getHeight()) {
                return;
            }
            visBounds.setRect(x, y, w, h);
//            parent.setDirty();
        } else {
            b.clear();
            b.genShapeOfArc(d);
            b.makeBounds(null, visBounds);
//            if (b.makeBounds(null, visBounds))
//                parent.setDirty();
        }
    }

    /**
     * Method to create a Poly object that describes an ArcInst in lambda units.
     * The ArcInst is described by its width and style.
     * @param gridWidth the width of the Poly in grid units.
     * @param style the style of the ArcInst.
     * @return a Poly that describes the ArcInst in lambda units.
     */
    public Poly makeLambdaPoly(long gridWidth, Poly.Type style) {
        Poly.Builder polyBuilder = Poly.threadLocalLambdaBuilder();
        polyBuilder.setup(topology.cell);
        return polyBuilder.makePoly(getD(), gridWidth, style);
    }

//	/**
//	 * Method to create a Poly object that describes an ArcInst in grid units.
//	 * The ArcInst is described by its width and style.
//	 * @param gridWidth the width of the Poly in grid units.
//	 * @param style the style of the ArcInst.
//	 * @return a Poly that describes the ArcInst in grid units.
//	 */
//    public Poly makeGridPoly(long gridWidth, Poly.Type style) {
//        CellBackup.Memoization m = parent != null ? parent.getMemoization() : null;
//        return getD().makeGridPoly(m, gridWidth, style);
//    }
    /**
     * Method to fill polygon "poly" with the outline in lambda units of the curved arc in
     * this ArcInst whose width in grid units is "gridWidth".  The style of the polygon is set to "style".
     * If there is no curvature information in the arc, the routine returns null,
     * otherwise it returns the curved polygon.
     */
    public Poly curvedArcLambdaOutline(Poly.Type style, long gridWidth, long gridRadius) {
        Poly.Builder polyBuilder = Poly.threadLocalLambdaBuilder();
        polyBuilder.setup(topology.cell);
        Variable radius = Variable.newInstance(ImmutableArcInst.ARC_RADIUS, new Double(DBMath.gridToLambda(gridRadius)), TextDescriptor.getArcTextDescriptor());
        return polyBuilder.makePoly(getD().withVariable(radius), gridWidth, style);
    }

//	/**
//	 * Method to return a list of Polys that describes all text on this ArcInst.
//	 * @param hardToSelect is true if considering hard-to-select text.
//	 * @param wnd the window in which the text will be drawn.
//	 * @return an array of Polys that describes the text.
//	 */
//	public Poly [] getAllText(boolean hardToSelect, EditWindow0 wnd)
//	{
//		int dispVars = numDisplayableVariables(false);
//		int totalText = dispVars;
//		if (totalText == 0) return null;
//		Poly [] polys = new Poly[totalText];
//		addDisplayableVariables(getBounds(), polys, 0, wnd, false);
//		return polys;
//	}
    /**
     * Method to return the number of displayable Variables on this ArcInst.
     * A displayable Variable is one that will be shown with its object.
     * @return the number of displayable Variables on this ArcInst.
     */
    public int numDisplayableVariables(boolean multipleStrings) {
        return super.numDisplayableVariables(multipleStrings) + (isUsernamed() ? 1 : 0);
    }

    /**
     * Method to add all displayable Variables on this Electric object to an array of Poly objects.
     * @param rect a rectangle describing the bounds of the object on which the Variables will be displayed.
     * @param polys an array of Poly objects that will be filled with the displayable Variables.
     * @param start the starting index in the array of Poly objects to fill with displayable Variables.
     * @return the number of Variables that were added.
     */
    public int addDisplayableVariables(Rectangle2D rect, Poly[] polys, int start, EditWindow0 wnd, boolean multipleStrings) {
        int numVars = 0;
        if (isUsernamed()) {
            double cX = rect.getCenterX();
            double cY = rect.getCenterY();
            TextDescriptor td = d.nameDescriptor;
            double offX = td.getXOff();
            double offY = td.getYOff();
            TextDescriptor.Position pos = td.getPos();
            Poly.Type style = pos.getPolyType();

            Point2D[] pointList = null;
            if (style == Poly.Type.TEXTBOX) {
                pointList = Poly.makePoints(rect);
            } else {
                pointList = new Point2D.Double[1];
                pointList[0] = new Point2D.Double(cX + offX, cY + offY);
            }
            polys[start] = new Poly(pointList);
            polys[start].setStyle(style);
            polys[start].setString(getNameKey().toString());
            polys[start].setTextDescriptor(td);
            polys[start].setLayer(null);
            polys[start].setDisplayedText(new DisplayedText(this, ARC_NAME));
            numVars = 1;
        }
        return super.addDisplayableVariables(rect, polys, start + numVars, wnd, multipleStrings) + numVars;
    }

    /**
     * Method to get all displayable Variables on this ArcInst to an array of Poly objects.
     * @param wnd window in which the Variables will be displayed.
     * @return an array of Poly objects with displayable variables.
     */
    public Poly[] getDisplayableVariables(EditWindow0 wnd) {
        return getDisplayableVariables(getBounds(), wnd, true);
    }

    /****************************** CONNECTIONS ******************************/

    /**
     * Method to return the Connection on the tail end of this ArcInst.
     * @return the Connection on the tail end of this ArcInst.
     */
    public TailConnection getTail() {
        return new TailConnection(this);
    }

    /**
     * Method to return the Connection on the head end of this ArcInst.
     * @return the Connection on the head end of this ArcInst.
     */
    public HeadConnection getHead() {
        return new HeadConnection(this);
    }

    /**
     * Method to return the connection at an end of this ArcInst.
     * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
     */
    public Connection getConnection(int connIndex) {
        switch (connIndex) {
            case ImmutableArcInst.TAILEND:
                return new TailConnection(this);
            case ImmutableArcInst.HEADEND:
                return new HeadConnection(this);
            default:
                throw new IllegalArgumentException("Bad end " + connIndex);
        }
    }

    /**
     * Method to return the PortInst on tail of this ArcInst.
     * @return the PortInst on tail.
     */
    public PortInst getTailPortInst() {
        return tailPortInst;
    }

    /**
     * Method to return the PortInst on head of this ArcInst.
     * @return the PortInst on head.
     */
    public PortInst getHeadPortInst() {
        return headPortInst;
    }

    /**
     * Method to tell whether this ArcInst is connected directly to another
     * Geometric object (that is, an arcinst connected to a nodeinst).
     * The method returns true if they are connected.
     * @param geom other Geometric object.
     * @return true if this and other Geometric objects are connected.
     */
    @Override
    public boolean isConnected(Geometric geom) {
        return tailPortInst.getNodeInst() == geom || headPortInst.getNodeInst() == geom;
    }

    /**
     * Method to return the PortInst on an end of this ArcInst.
     * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
     * @return the PortInst at an end.
     */
    public PortInst getPortInst(int connIndex) {
        switch (connIndex) {
            case ImmutableArcInst.TAILEND:
                return tailPortInst;
            case ImmutableArcInst.HEADEND:
                return headPortInst;
            default:
                throw new IllegalArgumentException("Bad end " + connIndex);
        }
    }

    /**
     * Method to return the Location on tail of this ArcInst.
     * @return the Location on tail.
     */
    public EPoint getTailLocation() {
        return d.tailLocation;
    }

    /**
     * Method to return the Location on head of this ArcInst.
     * @return the Location on head.
     */
    public EPoint getHeadLocation() {
        return d.headLocation;
    }

    /**
     * Method to return the Location on an end of this ArcInst.
     * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
     * @return the Location on an end.
     */
    public EPoint getLocation(int connIndex) {
        switch (connIndex) {
            case ImmutableArcInst.TAILEND:
                return d.tailLocation;
            case ImmutableArcInst.HEADEND:
                return d.headLocation;
            default:
                throw new IllegalArgumentException("Bad end " + connIndex);
        }
    }

    /**
     * Method to tell whether a tail connection on this ArcInst contains a port location.
     * @param pt the point in question.
     * @param reduceForArc if true reduce width by width offset of it proto.
     * @return true if the point is inside of the port.
     */
    public boolean tailStillInPort(Point2D pt, boolean reduceForArc) {
        return stillInPort(ImmutableArcInst.TAILEND, pt, reduceForArc);
    }

    /**
     * Method to tell whether a head connection on this ArcInst contains a port location.
     * @param pt the point in question.
     * @param reduceForArc if true reduce width by width offset of it proto.
     * @return true if the point is inside of the port.
     */
    public boolean headStillInPort(Point2D pt, boolean reduceForArc) {
        return stillInPort(ImmutableArcInst.HEADEND, pt, reduceForArc);
    }

    /**
     * Method to tell whether a connection on this ArcInst contains a port location.
     * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
     * @param pt the point in question.
     * @param reduceForArc if true reduce width by width offset of it proto.
     * @return true if the point is inside of the port.
     */
    public boolean stillInPort(int connIndex, Point2D pt, boolean reduceForArc) {
        // determine the area of the nodeinst
        PortInst pi = getPortInst(connIndex);
        Poly poly = pi.getPoly();
        if (reduceForArc) {
            double wid = getLambdaBaseWidth();
            poly.reducePortPoly(pi, wid, getAngle());
        }
        return stillInPoly(pt, poly);
//		if (poly.isInside(pt)) return true;
//		if (poly.polyDistance(pt.getX(), pt.getY()) < MINPORTDISTANCE) return true;
//
//		// no good
//		return false;
    }

    private static boolean stillInPoly(Point2D pt, Poly poly) {
        return poly.isInside(pt) || poly.polyDistance(pt.getX(), pt.getY()) < MINPORTDISTANCE;
    }

    /****************************** TEXT ******************************/
    /**
     * Method to return the name of this ArcInst.
     * @return the name of this ArcInst.
     */
    public String getName() {
        return d.name.toString();
    }

    /**
     * Retruns true if this ArcInst was named by user.
     * @return true if this ArcInst was named by user.
     */
    public boolean isUsernamed() {
        return d.isUsernamed();
    }

    /**
     * Method to return the name key of this ArcInst.
     * @return the name key of this ArcInst, null if there is no name.
     */
    public Name getNameKey() {
        return d.name;
    }

    /**
     * Method to rename this ArcInst.
     * This ArcInst must be linked to database.
     * @param name new name of this geometric.
     * @return true on error
     */
    public boolean setName(String name) {
        assert isLinked();
        Name key;
        boolean doSmart = false;
        if (name != null && name.length() > 0) {
            if (name.equals(getName())) {
                return false;
            }
            if (!isUsernamed()) {
                doSmart = true;
            }
            key = Name.findName(name);
        } else {
            if (!isUsernamed()) {
                return false;
            }
            key = topology.getArcAutoname();
        }
        if (checkNameKey(key, topology) || key.isBus() && getProto() != Schematics.tech().bus_arc) {
            return true;
        }
        ImmutableArcInst oldD = d;
        lowLevelModify(d.withName(key));
        if (doSmart) {
            TextDescriptor td = TextDescriptor.getArcTextDescriptor();
            TextDescriptor smartDescriptor = getSmartTextDescriptor(getAngle(), getLambdaBaseWidth(), td, getEditingPreferences());
            setTextDescriptor(ARC_NAME, smartDescriptor);
        }

        // apply constraints
        Constraints.getCurrent().modifyArcInst(this, oldD);
        return false;
    }

    /**
     * Method to return a "smart" text descriptor for an arc.
     * @param angle the angle of the arc (in tenths of a degree).
     * @param width the width of the arc.
     * @param prev the former text descriptor of the arc.
     * @return a new text descriptor that handles smart placement.
     */
    private static TextDescriptor getSmartTextDescriptor(int angle, double width, TextDescriptor prev, EditingPreferences ep) {
        // assigning valid name: do smart text placement
        if ((angle % 1800) == 0) {
            // horizontal arc
            int smart = ep.smartHorizontalPlacementArc;
            if (smart == 1) {
                // arc text above
                return prev.withPos(TextDescriptor.Position.UP).withOff(0, width / 2);
            } else if (smart == 2) {
                // arc text below
                return prev.withPos(TextDescriptor.Position.DOWN).withOff(0, -width / 2);
            }
        } else if ((angle % 1800) == 900) {
            // vertical arc
            int smart = ep.smartVerticalPlacementArc;
            if (smart == 1) {
                // arc text to the left
                return prev.withPos(TextDescriptor.Position.LEFT).withOff(-width / 2, 0);
            } else if (smart == 2) {
                // arc text to the right
                return prev.withPos(TextDescriptor.Position.RIGHT).withOff(width / 2, 0);
            }
        }
        return prev;
    }

    /**
     * Method to check the new name key of an ArcInst.
     * @param name new name key of this ArcInst.
     * @param parent parent Cell used for error message
     * @return true on error.
     */
    private static boolean checkNameKey(Name name, Topology topology) {
        Cell parent = topology.cell;
        if (!name.isValid()) {
            System.out.println(parent + ": Invalid name \"" + name + "\" wasn't assigned to arc" + " :" + Name.checkName(name.toString()));
            return true;
        }
        if (name.isBus()) {
            if (name.isTempname()) {
                System.out.println(parent + ": Temporary name \"" + name + "\" can't be bus");
                return true;
            }
            if (!parent.busNamesAllowed()) {
                System.out.println(parent + ": Bus name \"" + name + "\" can be in icons and schematics only");
                return true;
            }
        }
        if (name.isTempname() && name.getBasename() != ImmutableArcInst.BASENAME) {
            System.out.println(parent + ": Temporary arc name \"" + name + "\" must have prefix net@");
            return true;
        }
        if (name.hasEmptySubnames()) {
            if (name.isBus()) {
                System.out.println(parent + ": Name \"" + name + "\" with empty subnames wasn't assigned to arc");
            } else {
                System.out.println(parent + ": Cannot assign empty name \"" + name + "\" to arc");
            }
            return true;
        }
        if (topology.hasTempArcName(name)) {
            System.out.println(parent + " already has ArcInst with temporary name \"" + name + "\"");
            return true;
        }
        return false;
    }

    /**
     * Returns the TextDescriptor on this ArcInst selected by variable key.
     * This key may be a key of variable on this ArcInst or the
     * special keys:
     * <code>ArcInst.ARC_NAME</code>
     * The TextDescriptor gives information for displaying the Variable.
     * @param varKey key of variable or special key.
     * @return the TextDescriptor on this ArcInst.
     */
    public TextDescriptor getTextDescriptor(Variable.Key varKey) {
        if (varKey == ARC_NAME) {
            return d.nameDescriptor;
        }
        return super.getTextDescriptor(varKey);
    }

    /**
     * Updates the TextDescriptor on this ArcInst selected by varKey.
     * The varKey may be a key of variable on this ArcInst or
     * the special key ArcInst.ARC_NAME.
     * If varKey doesn't select any text descriptor, no action is performed.
     * The TextDescriptor gives information for displaying the Variable.
     * @param varKey key of variable or special key.
     * @param td new value TextDescriptor
     */
    @Override
    public void setTextDescriptor(Variable.Key varKey, TextDescriptor td) {
        if (varKey == ARC_NAME) {
            setD(d.withNameDescriptor(td), true);
            return;
        }
        super.setTextDescriptor(varKey, td);
    }

    /**
     * Method to determine whether a variable key on ArcInst is deprecated.
     * Deprecated variable keys are those that were used in old versions of Electric,
     * but are no longer valid.
     * @param key the key of the variable.
     * @return true if the variable key is deprecated.
     */
    public boolean isDeprecatedVariable(Variable.Key key) {
        if (key == ARC_NAME) {
            return true;
        }
        return super.isDeprecatedVariable(key);
    }

    /**
     * Method to describe this ArcInst as a string.
     * @param withQuotes to wrap description between quotes
     * @return a description of this ArcInst.
     */
    public String describe(boolean withQuotes) {
        String description = getProto().describe();
        String name = (withQuotes) ? "'" + getName() + "'" : getName();
        if (name != null) {
            description += "[" + name + "]";
        }
        return description;
    }

    /**
     * Compares ArcInsts by their Cells and names.
     * @param that the other ArcInst.
     * @return a comparison between the ArcInsts.
     */
    public int compareTo(ArcInst that) {
        int cmp;
        if (this.topology != that.topology) {
            cmp = this.topology.cell.compareTo(that.topology.cell);
            if (cmp != 0) {
                return cmp;
            }
        }
        cmp = this.getName().compareTo(that.getName());
        if (cmp != 0) {
            return cmp;
        }
        return this.d.arcId - that.d.arcId;
    }

    /**
     * Returns a printable version of this ArcInst.
     * @return a printable version of this ArcInst.
     */
    public String toString() {
        return "arc " + describe(true);
    }

    /****************************** CONSTRAINTS ******************************/
    private void setFlag(ImmutableArcInst.Flag flag, boolean state) {
        checkChanging();
        if (setD(d.withFlag(flag, state), true)) {
            topology.setArcsDirty();
        }
    }

    /**
     * Method to set this ArcInst to be rigid.
     * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
     * @param state
     */
    public void setRigid(boolean state) {
        setFlag(ImmutableArcInst.RIGID, state);
    }

    /**
     * Method to tell whether this ArcInst is rigid.
     * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
     * @return true if this ArcInst is rigid.
     */
    public boolean isRigid() {
        return d.isRigid();
    }

    /**
     * Method to set this ArcInst to be fixed-angle.
     * Fixed-angle arcs cannot change their angle, so if one end moves,
     * the other may also adjust to keep the arc angle constant.
     * @param state
     */
    public void setFixedAngle(boolean state) {
        setFlag(ImmutableArcInst.FIXED_ANGLE, state);
    }

    /**
     * Method to tell whether this ArcInst is fixed-angle.
     * Fixed-angle arcs cannot change their angle, so if one end moves,
     * the other may also adjust to keep the arc angle constant.
     * @return true if this ArcInst is fixed-angle.
     */
    public boolean isFixedAngle() {
        return d.isFixedAngle();
    }

    /**
     * Method to set this ArcInst to be slidable.
     * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
     * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
     * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
     * @param state
     */
    public void setSlidable(boolean state) {
        setFlag(ImmutableArcInst.SLIDABLE, state);
    }

    /**
     * Method to tell whether this ArcInst is slidable.
     * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
     * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
     * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
     * @return true if this ArcInst is slidable.
     */
    public boolean isSlidable() {
        return d.isSlidable();
    }

    /****************************** PROPERTIES ******************************/
    /**
     * Method to determine whether this ArcInst is directional, with an arrow on one end.
     * Directional arcs have an arrow drawn on them to indicate flow.
     * It is only for documentation purposes and does not affect the circuit.
     * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
     * @return true if that end has a directional arrow on it.
     */
    public boolean isArrowed(int connIndex) {
        return d.isArrowed(connIndex);
    }

    /**
     * Method to determine whether this ArcInst is directional, with an arrow on the tail.
     * Directional arcs have an arrow drawn on them to indicate flow.
     * It is only for documentation purposes and does not affect the circuit.
     * @return true if the arc's tail has a directional arrow on it.
     */
    public boolean isTailArrowed() {
        return d.isTailArrowed();
    }

    /**
     * Method to determine whether this ArcInst is directional, with an arrow on the head.
     * Directional arcs have an arrow drawn on them to indicate flow.
     * It is only for documentation purposes and does not affect the circuit.
     * @return true if the arc's head has a directional arrow on it.
     */
    public boolean isHeadArrowed() {
        return d.isHeadArrowed();
    }

    /**
     * Method to determine whether this ArcInst is directional, with an arrow line drawn down the center.
     * Directional arcs have an arrow drawn on them to indicate flow.
     * It is only for documentation purposes and does not affect the circuit.
     * The body is typically drawn when one of the ends has an arrow on it, but it may be
     * drawin without an arrow head in order to continue an attached arc that has an arrow.
     * @return true if the arc's tail has an arrow line on it.
     */
    public boolean isBodyArrowed() {
        return d.isBodyArrowed();
    }

    /**
     * Method to set this ArcInst to be directional, with an arrow on one end.
     * Directional arcs have an arrow drawn on them to indicate flow.
     * It is only for documentation purposes and does not affect the circuit.
     * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
     * @param state true to show a directional arrow on the specified end.
     */
    public void setArrowed(int connIndex, boolean state) {
        switch (connIndex) {
            case ImmutableArcInst.TAILEND:
                setTailArrowed(state);
                break;
            case ImmutableArcInst.HEADEND:
                setHeadArrowed(state);
                break;
            default:
                throw new IllegalArgumentException("Bad end " + connIndex);
        }
    }

    /**
     * Method to set this ArcInst to be directional, with an arrow on the tail.
     * Directional arcs have an arrow drawn on them to indicate flow.
     * It is only for documentation purposes and does not affect the circuit.
     * @param state true to show a directional arrow on the tail.
     */
    public void setTailArrowed(boolean state) {
        setFlag(ImmutableArcInst.TAIL_ARROWED, state);
    }

    /**
     * Method to set this ArcInst to be directional, with an arrow on the head.
     * Directional arcs have an arrow drawn on them to indicate flow.
     * It is only for documentation purposes and does not affect the circuit.
     * @param state true to show a directional arrow on the head.
     */
    public void setHeadArrowed(boolean state) {
        setFlag(ImmutableArcInst.HEAD_ARROWED, state);
    }

    /**
     * Method to set this ArcInst to be directional, with an arrow line drawn down the center.
     * Directional arcs have an arrow drawn on them to indicate flow.
     * It is only for documentation purposes and does not affect the circuit.
     * The body is typically drawn when one of the ends has an arrow on it, but it may be
     * drawin without an arrow head in order to continue an attached arc that has an arrow.
     * @param state true to show a directional line on this arc.
     */
    public void setBodyArrowed(boolean state) {
        setFlag(ImmutableArcInst.BODY_ARROWED, state);
    }

    /**
     * Method to tell whether an end of ArcInst has its ends extended.
     * Extended arcs continue past their endpoint by half of their width.
     * Most layout arcs want this so that they make clean connections to orthogonal arcs.
     * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
     * @return true if that end of this ArcInst iss extended.
     */
    public boolean isExtended(int connIndex) {
        return d.isExtended(connIndex);
    }

    /**
     * Method to tell whether the tail of this arc is extended.
     * Extended arcs continue past their endpoint by half of their width.
     * Most layout arcs want this so that they make clean connections to orthogonal arcs.
     * @return true if the tail of this arc is extended.
     */
    public boolean isTailExtended() {
        return d.isTailExtended();
    }

    /**
     * Method to tell whether the head of this arc is extended.
     * Extended arcs continue past their endpoint by half of their width.
     * Most layout arcs want this so that they make clean connections to orthogonal arcs.
     * @return true if the head of this arc is extended.
     */
    public boolean isHeadExtended() {
        return d.isHeadExtended();
    }

    /**
     * Method to set whether an end of this arc is extended.
     * Extended arcs continue past their endpoint by half of their width.
     * Most layout arcs want this so that they make clean connections to orthogonal arcs.
     * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
     * @param e true to set that end of this arc to be extended.
     */
    public void setExtended(int connIndex, boolean e) {
        switch (connIndex) {
            case ImmutableArcInst.TAILEND:
                setTailExtended(e);
                break;
            case ImmutableArcInst.HEADEND:
                setHeadExtended(e);
                break;
            default:
                throw new IllegalArgumentException("Bad end " + connIndex);
        }
    }

    /**
     * Method to set whether the tail of this arc is extended.
     * Extended arcs continue past their endpoint by half of their width.
     * Most layout arcs want this so that they make clean connections to orthogonal arcs.
     * @param e true to set the tail of this arc to be extended.
     */
    public void setTailExtended(boolean e) {
        setFlag(ImmutableArcInst.TAIL_EXTENDED, e);
//        if (isLinked()) updateGeometric();
    }

    /**
     * Method to set whether the head of this arc is extended.
     * Extended arcs continue past their endpoint by half of their width.
     * Most layout arcs want this so that they make clean connections to orthogonal arcs.
     * @param e true to set the head of this arc to be extended.
     */
    public void setHeadExtended(boolean e) {
        setFlag(ImmutableArcInst.HEAD_EXTENDED, e);
//        if (isLinked()) updateGeometric();
    }

    /**
     * Method to tell whether an end of this arc is negated.
     * Negated arc have a negating bubble on them to indicate negation.
     * This is only valid in schematics technologies.
     * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
     * @return true if set that end of this arc is negated.
     */
    public boolean isNegated(int connIndex) {
        return d.isNegated(connIndex);
    }

    /**
     * Method to tell whether the tail of this arc is negated.
     * Negated arc have a negating bubble on them to indicate negation.
     * This is only valid in schematics technologies.
     * @return true if set the tail of this arc is negated.
     */
    public boolean isTailNegated() {
        return d.isTailNegated();
    }

    /**
     * Method to tell whether the head of this arc is negated.
     * Negated arc have a negating bubble on them to indicate negation.
     * This is only valid in schematics technologies.
     * @return true if set the head of this arc is negated.
     */
    public boolean isHeadNegated() {
        return d.isHeadNegated();
    }

    /**
     * Method to set whether an end of this arc is negated.
     * Negated arc have a negating bubble on them to indicate negation.
     * This is only valid in schematics technologies.
     * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
     * @param n true to set that end of this arc to be negated.
     */
    public void setNegated(int connIndex, boolean n) {
        switch (connIndex) {
            case ImmutableArcInst.TAILEND:
                setTailNegated(n);
                break;
            case ImmutableArcInst.HEADEND:
                setHeadNegated(n);
                break;
            default:
                throw new IllegalArgumentException("Bad end " + connIndex);
        }
    }

    /**
     * Method to set whether the tail of this arc is negated.
     * Negated arc have a negating bubble on them to indicate negation.
     * This is only valid in schematics technologies.
     * @param n true to set the tail of this arc to be negated.
     */
    public void setTailNegated(boolean n) {
        if (!(d.tailPortId instanceof PrimitivePortId && getTechPool().getPrimitivePort((PrimitivePortId) d.tailPortId).isNegatable())) {
            n = false;
        }
        if (getProto().getTechnology().isNoNegatedArcs()) {
            n = false;
        }
        setFlag(ImmutableArcInst.TAIL_NEGATED, n);
    }

    /**
     * Method to set whether the head of this arc is negated.
     * Negated arc have a negating bubble on them to indicate negation.
     * This is only valid in schematics technologies.
     * @param n true to set the head of this arc to be negated.
     */
    public void setHeadNegated(boolean n) {
        if (!(d.headPortId instanceof PrimitivePortId && getTechPool().getPrimitivePort((PrimitivePortId) d.headPortId).isNegatable())) {
            n = false;
        }
        if (getProto().getTechnology().isNoNegatedArcs()) {
            n = false;
        }
        setFlag(ImmutableArcInst.HEAD_NEGATED, n);
    }

    /****************************** MISCELLANEOUS ******************************/
    /**
     * Method to check and repair data structure errors in this ArcInst.
     */
    public int checkAndRepair(boolean repair, List<Geometric> list, ErrorLogger errorLogger) {
        int errorCount = 0;
        ArcProto ap = getProto();

        Cell parent = topology.cell;
        if (ap.isNotUsed()) {
//            if (repair)
            if (errorLogger != null) {
                String msg = "Prototype of arc " + getName() + " is unused";
                if (repair) {
                    // Can't put this arc into error logger because it will be deleted.
                    Poly poly = makeLambdaPoly(getGridBaseWidth(), Poly.Type.CLOSED);
                    errorLogger.logError(msg, poly, parent, 1);
                } else {
                    errorLogger.logError(msg, this, parent, null, 1);
                }
            }
            if (repair) {
                list.add(this);
            }
            // This counts as 1 error, ignoring other errors
            return 1;
        }

        // see if the ends are in their ports
        if (!headStillInPort(d.headLocation, false)) {
            Poly poly = headPortInst.getPoly();
            String msg = parent + ", " + this
                    + ": head not in port, is at (" + d.headLocation.getX() + "," + d.headLocation.getY()
                    + ") distance to port is " + poly.polyDistance(d.headLocation.getX(), d.headLocation.getY())
                    + " port center is (" + poly.getCenterX() + "," + poly.getCenterY() + ")";
            System.out.println(msg);
            if (errorLogger != null) {
                List<Object> errorList = new ArrayList<Object>(2);
                errorList.add(headPortInst.getNodeInst());
                errorList.add(makeLambdaPoly(getGridBaseWidth(), Poly.Type.CLOSED));
                if (repair) {
                    errorLogger.logMessage(msg, errorList, parent, 1, true);
                } else {
                    List<Geometric> geomList = new ArrayList<Geometric>();
                    geomList.add(this);
                    geomList.add(headPortInst.getNodeInst());
                    errorLogger.logMessage(msg, geomList, parent, 1, true);
                }
            }
            if (repair) {
                Constraints.getCurrent().modifyArcInst(this, getD());
//				setD(d.withLocations(d.tailLocation, new EPoint(poly.getCenterX(), poly.getCenterY())), false);
//				updateGeometric();
            }
            errorCount++;
        }
        if (!tailStillInPort(d.tailLocation, false)) {
            Poly poly = tailPortInst.getPoly();
            String msg = parent + ", " + this
                    + ": tail not in port, is at (" + d.tailLocation.getX() + "," + d.tailLocation.getY()
                    + ") distance to port is " + poly.polyDistance(d.tailLocation.getX(), d.tailLocation.getY())
                    + " port center is (" + poly.getCenterX() + "," + poly.getCenterY() + ")";
            System.out.println(msg);
            if (errorLogger != null) {
                if (repair) {
                    List<Object> errorList = new ArrayList<Object>(2);
                    errorList.add(tailPortInst.getNodeInst());
                    errorList.add(makeLambdaPoly(getGridBaseWidth(), Poly.Type.CLOSED));
                    errorLogger.logMessage(msg, errorList, parent, 1, true);
                } else {
                    List<Geometric> geomList = new ArrayList<Geometric>();
                    geomList.add(this);
                    geomList.add(tailPortInst.getNodeInst());
                    errorLogger.logMessage(msg, geomList, parent, 1, true);
                }
            }
            if (repair) {
                Constraints.getCurrent().modifyArcInst(this, getD());
//				setD(d.withLocations(new EPoint(poly.getCenterX(), poly.getCenterY()), d.headLocation), false);
//				updateGeometric();
            }
            errorCount++;
        }
        return errorCount;
    }

    /**
     * Method to check invariants in this ArcInst.
     * @exception AssertionError if invariants are not valid
     */
    public void check(Poly.Builder polyBuilder) {
        if (topology.validArcBounds && Job.getDebug()) {
            double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            for (Iterator<Poly> it = getShape(polyBuilder); it.hasNext();) {
                Poly poly = it.next();
                Rectangle2D bounds = poly.getBounds2D();
                minX = Math.min(minX, bounds.getMinX());
                minY = Math.min(minY, bounds.getMinY());
                maxX = Math.max(maxX, bounds.getMaxX());
                maxY = Math.max(maxY, bounds.getMaxY());
            }
            minX = GenMath.floorLong(minX);
            minY = GenMath.floorLong(minY);
            maxX = GenMath.ceilLong(maxX);
            maxY = GenMath.ceilLong(maxY);
            assert visBounds.getX() == DBMath.gridToLambda(minX);
            assert visBounds.getY() == DBMath.gridToLambda(minY);
            assert visBounds.getWidth() == DBMath.gridToLambda(maxX - minX);
            assert visBounds.getHeight() == DBMath.gridToLambda(maxY - minY);
        }
    }

    /**
     * Method to get the arcId of this ArcInst.
     * The arcId is assign to ArcInst in chronological order
     * The arcId doesn't relate to alpahnumeric ordering of arcs in the Cell.
     * @return the index of this ArcInst.
     */
    public final int getArcId() {
        return d.arcId;
    }

//
//	/**
//	 * Method to return the index of this arcInst
//	 * @return the index of this arcInst
//	 */
//	public final int getArcIndex(){
//		return arcIndex;
//	}
//
//	/**
//	 * Method to set the index of this arcInst
//	 * @param arcIndex index of this arcInst
//	 */
//	public final void setArcIndex( int arcIndex ){
//		this.arcIndex = arcIndex;
//	}
    /**
     * Returns true if this ArcInst is linked into database.
     * @return true if this ArcInst is linked into database.
     */
    public boolean isLinked() {
        try {
            Cell parent = topology.cell;
            return parent.isLinked() && parent.getArcById(getArcId()) == this;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    /**
     * Method to return the Cell Topology that contains this ArcInst.
     * @return the Topology that contains this ArcInst.
     */
    @Override
    public Topology getTopology() {
        return topology;
    }

    /**
     * Routing to check whether changing of this cell allowed or not.
     * By default checks whole database change. Overriden in subclasses.
     */
    @Override
    public void checkChanging() {
        topology.cell.checkChanging();
    }

    /**
     * Method to determine the appropriate Cell associated with this ElectricObject.
     * @return the appropriate Cell associated with this ElectricicObject.
     */
    @Override
    public Cell whichCell() {
        return topology.cell;
    }

    /**
     * Returns database to which this ArcInst belongs.
     * @return database to which this ArcInst belongs.
     */
    @Override
    public EDatabase getDatabase() {
        return topology.cell.getDatabase();
    }

    /**
     * Method to return the prototype of this ArcInst.
     * @return the prototype of this ArcInst.
     */
    public ArcProto getProto() {
        return getTechPool().getArcProto(d.protoId);
    }

    /**
     * Copies all properties (variables, constraints, and textdescriptor)
     * from 'fraomAi' to this arcinst. This is basically the same as calling
     * copyVarsFrom(), copyConstraintsFrom(), and setTextDescriptor().
     * @param fromAi the arc from which to copy all arc properties
     */
    public void copyPropertiesFrom(ArcInst fromAi) {
        if (fromAi == null) {
            return;
        }
        copyVarsFrom(fromAi);
        copyConstraintsFrom(fromAi);
        copyTextDescriptorFrom(fromAi, ArcInst.ARC_NAME);
    }

    /**
     * Copies constraints (Rigid, Ends Extended, etc) from another arcinst to this arcinst
     * It copies also attributes of Connections (arrow/negated/extended)
     * @param fromAi the arcinst from which to copy constraints
     */
    public void copyConstraintsFrom(ArcInst fromAi) {
        checkChanging();
        if (fromAi == null) {
            return;
        }
        ImmutableArcInst oldD = d;
        int flags = fromAi.d.flags;
        if (!(d.tailPortId instanceof PrimitivePortId && getTechPool().getPrimitivePort((PrimitivePortId) d.tailPortId).isNegatable())) {
            flags = ImmutableArcInst.TAIL_NEGATED.set(flags, false);
        }
        if (!(d.headPortId instanceof PrimitivePortId && getTechPool().getPrimitivePort((PrimitivePortId) d.headPortId).isNegatable())) {
            flags = ImmutableArcInst.HEAD_NEGATED.set(flags, false);
        }
        if (getProto().getTechnology().isNoNegatedArcs()) {
            flags = ImmutableArcInst.TAIL_NEGATED.set(flags, false);
            flags = ImmutableArcInst.HEAD_NEGATED.set(flags, false);
        }
        lowLevelModify(d.withFlags(flags).withAngle(fromAi.getAngle()));
        assert topology != null;
        Constraints.getCurrent().modifyArcInst(this, oldD);
    }

//	/**
//	 * Low-level method to get the user bits.
//	 * The "user bits" are a collection of flags that are more sensibly accessed
//	 * through special methods.
//	 * This general access to the bits is required because the ELIB
//	 * file format stores it as a full integer.
//	 * This should not normally be called by any other part of the system.
//	 * @return the "user bits".
//	 */
//    public int lowLevelGetUserbits() { return userBits; }
//
//	/**
//	 * Low-level method to set the user bits.
//	 * The "user bits" are a collection of flags that are more sensibly accessed
//	 * through special methods.
//	 * This general access to the bits is required because the ELIB
//	 * file format stores it as a full integer.
//	 * This should not normally be called by any other part of the system.
//	 * @param diskBits the new "user bits".
//	 */
// 	public void lowLevelSetUserbits(int userBits)
// 	{
// 		boolean extensionChanged = (this.userBits&(TAILNOEXTEND|HEADNOEXTEND)) != (userBits&(TAILNOEXTEND|HEADNOEXTEND));
// 		this.userBits = userBits & DATABASE_BITS;
// 		if (isLinked() && extensionChanged) updateGeometric();
// 	}
//	/**
//	 * Method to copy the various state bits from another ArcInst to this ArcInst.
//	 * @param ai the other ArcInst to copy.
//	 */
//	public void copyStateBits(ArcInst ai) { checkChanging(); this.userBits = ai.userBits; Undo.otherChange(this); }
//	/**
//	 * Method to set default constraint information on this ArcInst.
//	 */
//	private void setDefaultConstraints(ArcProto protoType)
//	{
//        setRigid(protoType.isRigid());
//        setFixedAngle(protoType.isFixedAngle());
//        setSlidable(protoType.isSlidable());
//        setHeadExtended(protoType.isExtended());
//        setTailExtended(protoType.isExtended());
//        setHeadArrowed(protoType.isDirectional());
//        setBodyArrowed(protoType.isDirectional());
//	}
    /**
     * Method to set this ArcInst to be hard-to-select.
     * Hard-to-select ArcInsts cannot be selected by clicking on them.
     * Instead, the "special select" command must be given.
     * @param state
     */
    public void setHardSelect(boolean state) {
        setFlag(ImmutableArcInst.HARD_SELECT, state);
    }

    /**
     * Method to tell whether this ArcInst is hard-to-select.
     * Hard-to-select ArcInsts cannot be selected by clicking on them.
     * Instead, the "special select" command must be given.
     * @return true if this ArcInst is hard-to-select.
     */
    public boolean isHardSelect() {
        return d.isHardSelect();
    }

    /**
     * This function is to compare NodeInst elements. Initiative CrossLibCopy
     * @param obj Object to compare to
     * @param buffer To store comparison messages in case of failure
     * @return True if objects represent same ArcInst
     */
    public boolean compare(Object obj, StringBuffer buffer) {
        if (this == obj) {
            return (true);
        }

        // Better if compare classes? but it will crash with obj=null
        if (obj == null || getClass() != obj.getClass()) {
            return (false);
        }

        ArcInst a = (ArcInst) obj;
        if (getProto().getClass() != a.getProto().getClass()) {
            return (false);
        }

        // Not sure if I should defina myEquals for Geometric
        ArcProto arcType = a.getProto();
        Technology tech = arcType.getTechnology();
        if (getProto().getTechnology() != tech) {
            if (buffer != null) {
                buffer.append("No same technology for arcs " + getName() + " and " + a.getName() + "\n");
            }
            return (false);
        }

        Poly[] polyList = getProto().getTechnology().getShapeOfArc(this);
        Poly[] aPolyList = tech.getShapeOfArc(a);

        if (polyList.length != aPolyList.length) {
            if (buffer != null) {
                buffer.append("No same number of geometries in " + getName() + " and " + a.getName() + "\n");
            }
            return (false);
        }

        // Remove noCheckList if equals is implemented
        // Sort them out by a key so comparison won't be O(n2)
        List<Poly> noCheckAgain = new ArrayList<Poly>();
        for (int i = 0; i < polyList.length; i++) {
            boolean found = false;
            for (int j = 0; j < aPolyList.length; j++) {
                // Already found
                if (noCheckAgain.contains(aPolyList[j])) {
                    continue;
                }
                if (polyList[i].compare(aPolyList[j], buffer)) {
                    found = true;
                    noCheckAgain.add(aPolyList[j]);
                    break;
                }
            }
            // polyList[i] doesn't match any elem in noPolyList
            if (!found) {
                // No message otherwise all comparisons are found in buffer
                /*
                if (buffer != null)
                buffer.append("No corresponding geometry in " + getName() + " found in " + a.getName() + "\n");
                 */
                return (false);
            }
        }
        return (true);
    }

    /**
     * Method to crop given polygon against a connecting transistor. Function similar to Quick.cropActiveArc
     * @param poly
     * @return new polygon if was cropped otherwise the original
     */
    public Poly cropPerLayer(Poly poly) {
        // must be manhattan
        Rectangle2D polyBounds = poly.getBox();
        if (polyBounds == null) {
            return poly;
        }
        polyBounds = new Rectangle2D.Double(polyBounds.getMinX(), polyBounds.getMinY(), polyBounds.getWidth(), polyBounds.getHeight());

        // search for adjoining transistor in the cell
        for (int i = 0; i < 2; i++) {
            PortInst pi = getPortInst(i);
            NodeInst ni = pi.getNodeInst();
            //if (!ni.isFET()) continue;

            // crop the arc against this transistor
            AffineTransform trans = ni.rotateOut();
            Technology tech = ni.getProto().getTechnology();
            Poly[] activeCropPolyList = tech.getShapeOfNode(ni);
            int nTot = activeCropPolyList.length;
            for (int k = 0; k < nTot; k++) {
                Poly nPoly = activeCropPolyList[k];
                if (nPoly.getLayer() != poly.getLayer()) {
                    continue;
                }
                nPoly.transform(trans);
                Rectangle2D nPolyBounds = nPoly.getBox();
                if (nPolyBounds == null) {
                    continue;
                }
                int result = Poly.cropBoxComplete(polyBounds, nPolyBounds);
                if (result == 1) {
                    // Empty polygon
                    return null;
                }
                if (result == -2) {
                    System.out.println("When is this case?");
                }
                Poly newPoly = new Poly(polyBounds);
                newPoly.setLayer(poly.getLayer());
                newPoly.setStyle(poly.getStyle());
                return newPoly;
            }
        }
        return poly;
    }

    /**
     * Method to determin if arc contains active diffusion
     * @return True if contains active diffusion
     */
    public boolean isDiffusionArc() {
        return getProto().getFunction().isDiffusion();
    }
}
