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

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellTree;
import com.sun.electric.database.EObjectInputStream;
import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableCell;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.ImmutablePortInst;
import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.CellUsage;
import com.sun.electric.database.id.PortProtoId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortOriginal;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.DisplayedText;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.BoundsBuilder;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveNodeSize;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.TransistorSize;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.Clipboard;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

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
public class NodeInst extends Geometric implements Nodable, Comparable<NodeInst> {

    /** key of text descriptor with prototype name. */
    public static final Variable.Key NODE_PROTO = Variable.newKey("NODE_proto");
    /** key of obsolete Variable holding instance name. */
    public static final Variable.Key NODE_NAME = Variable.newKey("NODE_name");
    /** key of Varible holding outline information. */
    public static final Variable.Key TRACE = Variable.newKey("trace");
    /** key of Varible holding serpentine transistor length. */
    public static final Variable.Key TRANSISTOR_LENGTH_KEY = Variable.newKey("transistor_width");
    private static final PortInst[] NULL_PORT_INST_ARRAY = new PortInst[0];
//	private static final Export[] NULL_EXPORT_ARRAY = new Export[0];

    /**
     * Method to detect if np is not relevant for some tool calculation and therefore
     * could be skip. E.g. cellCenter, drcNodes, essential bounds and pins in DRC.
     * Similar for layer generation
     * @param ni the NodeInst in question.
     * @return true if it is a special node (cell center, etc.)
     */
    public static boolean isSpecialNode(NodeInst ni) {
        NodeProto np = ni.getProto();
        return (Generic.isSpecialGenericNode(ni) || np.getFunction().isPin()
                || np.getFunction() == PrimitiveNode.Function.CONNECT);
    }

    /**
     * the PortAssociation class is used when replacing nodes.
     */
    private static class PortAssociation {

        /** the original PortInst being associated. */
        PortInst portInst;
        /** the Poly that describes the original PortInst. */
        Poly poly;
        /** the center point in the original PortInst. */
        Point2D pos;
        /** the associated PortInst on the new NodeInst. */
        PortInst assn;
    }
    // ---------------------- private data ----------------------------------
    /** Owner of this NodeInst. */
    final Topology topology;
    /** persistent data of this NodeInst. */
    private ImmutableNodeInst d;
    /** prototype of this NodeInst. */
    private NodeProto protoType;
    /** 0-based index of this NodeInst in Cell. */
    private int nodeIndex = -1;
    /** Array of PortInsts on this NodeInst. */
    private PortInst[] portInsts = NULL_PORT_INST_ARRAY;
    /** bounds after transformation. */
    private final Rectangle2D.Double visBounds = new Rectangle2D.Double(0, 0, 0, 0);
    /** True, if visBounds are valid. */
    private boolean validVisBounds;

    // --------------------- private and protected methods ---------------------
    /**
     * The constructor of NodeInst. Use the factory "newInstance" instead.
     * @param d persistent data of this NodeInst.
     * @param topology the Topology in which this NodeInst will reside.
     */
    NodeInst(ImmutableNodeInst d, Topology topology) {
        this.topology = topology;
        protoType = d.protoId.inDatabase(getDatabase());
        this.d = d;

        // create all of the portInsts on this node inst
        portInsts = new PortInst[protoType.getNumPorts()];
        for (int i = 0; i < portInsts.length; i++) {
            PortProto pp = protoType.getPort(i);
            portInsts[i] = PortInst.newInstance(pp, this);
        }
    }

    private NodeInst(NodeProto protoType, ImmutableNodeInst d) {
        topology = null;
        assert d.protoId == protoType.getId();
        this.d = d;
        this.protoType = protoType;

        // create all of the portInsts on this node inst
        portInsts = new PortInst[protoType.getNumPorts()];
        for (int i = 0; i < portInsts.length; i++) {
            PortProto pp = protoType.getPort(i);
            portInsts[i] = PortInst.newInstance(pp, this);
        }
    }

    protected Object writeReplace() {
        return new NodeInstKey(this);
    }

    private static class NodeInstKey extends EObjectInputStream.Key<NodeInst> {

        public NodeInstKey() {
        }

        private NodeInstKey(NodeInst ni) {
            super(ni);
        }

        @Override
        public void writeExternal(EObjectOutputStream out, NodeInst ni) throws IOException {
            if (ni.getDatabase() != out.getDatabase() || !ni.isLinked()) {
                throw new NotSerializableException(ni + " not linked");
            }
            out.writeObject(ni.topology.cell);
            out.writeInt(ni.getD().nodeId);
        }

        @Override
        public NodeInst readExternal(EObjectInputStream in) throws IOException, ClassNotFoundException {
            Cell cell = (Cell) in.readObject();
            int nodeId = in.readInt();
            NodeInst ni = cell.getNodeById(nodeId);
            if (ni == null) {
                throw new InvalidObjectException("NodeInst from " + cell);
            }
            return ni;
        }
    }

    /****************************** CREATE, DELETE, MODIFY ******************************/
    /**
     * Short form method to create a NodeInst and do extra things necessary for it. Angle, name
     * and techBits are set to defaults.
     * @param protoType the NodeProto of which this is an instance.
     * @param center the center location of this NodeInst.
     * @param width the width of this NodeInst (can't be negative).
     * @param height the height of this NodeInst (can't be negative).
     * @param parent the Cell in which this NodeInst will reside.
     * @return the newly created NodeInst, or null on error.
     */
    public static NodeInst makeInstance(NodeProto protoType, Point2D center, double width, double height, Cell parent) {
        return makeInstance(protoType, center, width, height, parent, Orientation.IDENT, null);
    }

    /**
     * Short form method to create a NodeInst and do extra things necessary for it. Angle, name
     * and techBits are set to defaults.
     * @param protoType the NodeProto of which this is an instance.
     * @param center the center location of this NodeInst.
     * @param width the width of this NodeInst (can't be negative).
     * @param height the height of this NodeInst (can't be negative).
     * @param parent the Cell in which this NodeInst will reside.
     * @param orient the orientation of this NodeInst.
     * @param name name of new NodeInst
     * @return the newly created NodeInst, or null on error.
     */
    public static NodeInst makeInstance(NodeProto protoType, Point2D center, double width, double height,
            Cell parent, Orientation orient, String name) {
        return makeInstance(protoType, center, width, height, parent, orient, name, 0);
    }

    /**
     * Short form method to create a NodeInst and do extra things necessary for it. Angle, name
     * and techBits are set to defaults.
     * @param protoType the NodeProto of which this is an instance.
     * @param center the center location of this NodeInst.
     * @param width the width of this NodeInst (can't be negative).
     * @param height the height of this NodeInst (can't be negative).
     * @param parent the Cell in which this NodeInst will reside.
     * @param orient the orientation of this NodeInst.
     * @param name name of new NodeInst
     * @return the newly created NodeInst, or null on error.
     */
    public static NodeInst makeInstance(NodeProto protoType, Point2D center, double width, double height,
            Cell parent, Orientation orient, String name, PrimitiveNode.Function function) {
        NodeInst ni = makeInstance(protoType, center, width, height, parent, orient, name);
        if (ni != null) {
            if (!ni.isCellInstance()) {
                ((PrimitiveNode) protoType).getTechnology().setPrimitiveFunction(ni, function);
            }
        }
        return ni;
    }

    /**
     * Long form method to create a NodeInst and do extra things necessary for it.
     * @param protoType the NodeProto of which this is an instance.
     * @param center the center location of this NodeInst.
     * @param width the width of this NodeInst (can't be negative).
     * @param height the height of this NodeInst (can't be negative).
     * @param parent the Cell in which this NodeInst will reside.
     * @param orient the orientation of this NodeInst.
     * @param name name of new NodeInst
     * @param techBits bits associated to different technologies
     * @return the newly created NodeInst, or null on error.
     */
    public static NodeInst makeInstance(NodeProto protoType, Point2D center, double width, double height,
            Cell parent, Orientation orient, String name, int techBits) {
        NodeInst ni = newInstance(protoType, center, width, height, parent, orient, name, techBits);
        if (ni != null) {
            // set default information from the prototype
            if (protoType instanceof Cell) {
                // for cells, use the default expansion on this instance
                if (((Cell) protoType).isWantExpanded()) {
                    ni.setExpanded(true);
                }
            } else {
                // for primitives, set a default outline if appropriate
                protoType.getTechnology().setDefaultOutline(ni);
            }

            // create inheritable variables
            CircuitChangeJobs.inheritAttributes(ni);
        }
        return ni;
    }

    /**
     * Method to create a "dummy" NodeInst for use outside of the database.
     * @param np the prototype of the NodeInst.
     * @return the dummy NodeInst.
     */
    public static NodeInst makeDummyInstance(NodeProto np) {
        return makeDummyInstance(np, EPoint.ORIGIN, np.getDefWidth(), np.getDefHeight(), Orientation.IDENT);
    }

    /**
     * Method to create a "dummy" NodeInst for use outside of the database.
     * @param np the prototype of the NodeInst.
     * @param center the center location of this NodeInst.
     * @param width the width of this NodeInst (can't be negative).
     * @param height the height of this NodeInst (can't be negative).
     * @param orient the orientation of this NodeInst.
     * @return the dummy NodeInst.
     */
    public static NodeInst makeDummyInstance(NodeProto np, EPoint center, double width, double height, Orientation orient) {
        return makeDummyInstance(np, 0, center, width, height, orient);
    }

    /**
     * Method to create a "dummy" NodeInst for use outside of the database.
     * @param np the prototype of the NodeInst.
     * @param techBits tech bits of the NodeInst
     * @param center the center location of this NodeInst.
     * @param width the width of this NodeInst (can't be negative).
     * @param height the height of this NodeInst (can't be negative).
     * @param orient the orientation of this NodeInst.
     * @return the dummy NodeInst.
     */
    public static NodeInst makeDummyInstance(NodeProto np, int techBits, EPoint center, double width, double height, Orientation orient) {
        EPoint size = EPoint.ORIGIN;
        if (np instanceof PrimitiveNode) {
            ERectangle full = ((PrimitiveNode) np).getFullRectangle();
            long gridWidth = DBMath.lambdaToSizeGrid(width - full.getLambdaWidth());
            long gridHeight = DBMath.lambdaToSizeGrid(height - full.getLambdaHeight());
            size = EPoint.fromGrid(gridWidth, gridHeight);
        }
        ImmutableNodeInst d = ImmutableNodeInst.newInstance(0, np.getId(), Name.findName("node@0"), TextDescriptor.getNodeTextDescriptor(),
                orient, center, size, 0, techBits, TextDescriptor.getInstanceTextDescriptor());
        return new NodeInst(np, d);
    }

    /**
     * Short form method to create a NodeInst. Angle, name
     * and techBits are set to defaults.
     * @param protoType the NodeProto of which this is an instance.
     * @param center the center location of this NodeInst.
     * @param width the width of this NodeInst (can't be negative).
     * @param height the height of this NodeInst (can't be negative).
     * If negative, flip the Y coordinate (or flip ABOUT the X axis).
     * @param parent the Cell in which this NodeInst will reside.
     * @return the newly created NodeInst, or null on error.
     */
    public static NodeInst newInstance(NodeProto protoType, Point2D center, double width, double height, Cell parent) {
        return newInstance(protoType, center, width, height, parent, Orientation.IDENT, null);
    }

    /**
     * Long form method to create a NodeInst.
     * @param protoType the NodeProto of which this is an instance.
     * @param center the center location of this NodeInst.
     * @param width the width of this NodeInst (can't be negative).
     * @param height the height of this NodeInst (can't be negative).
     * @param parent the Cell in which this NodeInst will reside.
     * @param orient the oriantation of this NodeInst.
     * @param name name of new NodeInst
     * @return the newly created NodeInst, or null on error.
     */
    public static NodeInst newInstance(NodeProto protoType, Point2D center, double width, double height,
            Cell parent, Orientation orient, String name) {
        return newInstance(protoType, center, width, height, parent, orient, name, 0);
    }

    /**
     * Long form method to create a NodeInst.
     * @param protoType the NodeProto of which this is an instance.
     * @param center the center location of this NodeInst.
     * @param width the width of this NodeInst (can't be negative).
     * @param height the height of this NodeInst (can't be negative).
     * @param parent the Cell in which this NodeInst will reside.
     * @param orient the oriantation of this NodeInst.
     * @param name name of new NodeInst
     * @param techBits bits associated to different technologies
     * @return the newly created NodeInst, or null on error.
     */
    public static NodeInst newInstance(NodeProto protoType, Point2D center, double width, double height,
            Cell parent, Orientation orient, String name, int techBits) {
        if (name != null && parent.findNode(name) != null) {
            System.out.println(parent + " already has NodeInst with name \"" + name + "\"");
            return null;
        }
        EPoint size = EPoint.ORIGIN;
        if (protoType instanceof PrimitiveNode) {
            ERectangle full = ((PrimitiveNode) protoType).getFullRectangle();
            long gridWidth = DBMath.lambdaToSizeGrid(width - full.getLambdaWidth());
            long gridHeight = DBMath.lambdaToSizeGrid(height - full.getLambdaHeight());
            size = EPoint.fromGrid(gridWidth, gridHeight);
        }
        return newInstance(parent, protoType, name, null, center, size, orient, 0, techBits, null, null);
    }

    /**
     * Long form method to create a NodeInst.
     * @param parent the Cell in which this NodeInst will reside.
     * @param protoType the NodeProto of which this is an instance.
     * @param name name of new NodeInst
     * @param nameDescriptor TextDescriptor of name of this NodeInst
     * @param center the center location of this NodeInst.
     * @param size the size of this NodeInst (can't be negative).
     * @param orient the orientation of this NodeInst.
     * @param flags flags of this NodeInst.
     * @param techBits bits associated to different technologies
     * @param protoDescriptor TextDescriptor of name of this NodeInst
     * @param errorLogger error logger to report node rename.
     * @return the newly created NodeInst, or null on error.
     */
    public static NodeInst newInstance(Cell parent, NodeProto protoType,
            String name, TextDescriptor nameDescriptor,
            Point2D center, EPoint size, Orientation orient,
            int flags, int techBits, TextDescriptor protoDescriptor, ErrorLogger errorLogger) {
        if (protoType == null) {
            return null;
        }
        if (parent == null) {
            return null;
        }
        assert parent.isLinked();
        if (protoType instanceof Cell) {
            assert ((Cell) protoType).isLinked();
        }
        Topology topology = parent.getTopology();

        EPoint anchor = EPoint.snap(center);

        Name nameKey = null;
        String msg = null;
        if (name != null) {
            nameKey = Name.findName(name);
            if (checkNameKey(nameKey, parent) || nameKey.isBus() && (!(protoType instanceof Cell) || !((Cell) protoType).isIcon())) {
                nameKey = null;
            } else if (parent.findNode(name) != null) {
                if (!nameKey.isTempname()) {
                    msg = parent + " already has NodeInst with name \"" + name + "\"";
                }
                nameKey = null;
            }
        }
        if (nameKey == null) {
            Name baseName;
            if (protoType instanceof Cell) {
                baseName = ((Cell) protoType).getBasename();
            } else {
                PrimitiveNode np = (PrimitiveNode) protoType;
                baseName = np.getTechnology().getPrimitiveFunction(np, techBits).getBasename();
            }
            nameKey = topology.getNodeAutoname(baseName);
            if (msg != null) {
                msg += ", renamed to \"" + nameKey + "\"";
                System.out.println(msg);
            }
        }
        CellId parentId = parent.getId();

        if (nameDescriptor == null) {
            nameDescriptor = TextDescriptor.getNodeTextDescriptor();
        }
        if (protoDescriptor == null) {
            protoDescriptor = TextDescriptor.getInstanceTextDescriptor();
        }

        // search for spare nodeId
        int nodeId;
        do {
            nodeId = parentId.newNodeId();
        } while (parent.getNodeById(nodeId) != null);
        ImmutableNodeInst d = ImmutableNodeInst.newInstance(nodeId, protoType.getId(), nameKey, nameDescriptor,
                orient, anchor, size, flags, techBits, protoDescriptor);

        NodeInst ni = newInstance(parent, d);
        if (ni != null && msg != null && errorLogger != null) {
            errorLogger.logError(msg, ni, parent, null, 1);
        }
        return ni;
    }

    /**
     * Method to create a NodeInst by ImmutableNodeInst.
     * @param parent the Cell in which this NodeInst will reside.
     * @param d ImmutableNodeInst of new NodeInst
     * @return the newly created NodeInst, or null on error.
     */
    public static NodeInst newInstance(Cell parent, ImmutableNodeInst d) {
        if (d.protoId instanceof CellId) {
            Cell subCell = parent.getDatabase().getCell((CellId) d.protoId);
            if (Cell.isInstantiationRecursive(subCell, parent)) {
                System.out.println("Cannot create instance of " + subCell + " in " + parent
                        + " because it would be a recursive case");
                return null;
            }
            subCell.getTechnology();
        }

        if (ImmutableNodeInst.isCellCenter(d.protoId) && parent.alreadyCellCenter()) {
            System.out.println("Can only be one cell-center in " + parent + ": new one ignored");
            return null;
        }

        if (parent.findNode(d.name.toString()) != null) {
            System.out.println(parent + " already has NodeInst with name \"" + d.name + "\"");
            return null;
        }

        NodeInst ni = lowLevelNewInstance(parent.getTopology(), d);

        if (ni.checkAndRepair(true, null, null) > 0) {
            return null;
        }

        // add to linked lists
        if (parent.addNode(ni)) {
            return null;
        }

        // handle change control, constraint, and broadcast
        Constraints.getCurrent().newObject(ni);
        if (ImmutableNodeInst.isCellCenter(d.protoId)) {
            parent.adjustReferencePoint(d.anchor.getX(), d.anchor.getY());
        }
        return ni;
    }

    public static NodeInst lowLevelNewInstance(Topology topology, ImmutableNodeInst d) {
        if (d.protoId instanceof CellId && ((CellId) d.protoId).isIcon()) {
            return new IconNodeInst(d, topology);
        }
        return new NodeInst(d, topology);
    }

    /**
     * Method to delete this NodeInst.
     */
    public void kill() {
        if (!isLinked()) {
            System.out.println("NodeInst already killed");
            return;
        }
        topology.cell.killNodes(Collections.singleton(this));
    }

    /**
     * Method to move this NodeInst.
     * @param dX the amount to move the NodeInst in X.
     * @param dY the amount to move the NodeInst in Y.
     */
    public void move(double dX, double dY) {
//		System.out.println("Moving "+this+" [is "+getXSize()+"x"+getYSize()+" at ("+
//                getAnchorCenterX()+","+getAnchorCenterY()+") rot "+getAngle()+
//			"] change is dx="+dX+" dy="+dY+") drot=0");

        modifyInstance(dX, dY, 0, 0, Orientation.IDENT);
    }

    /**
     * Method to resize this NodeInst.
     * @param dXSize the amount to scale the NodeInst in X.
     * @param dYSize the amount to scale the NodeInst in Y.
     */
    public void resize(double dXSize, double dYSize) {
        modifyInstance(0, 0, dXSize, dYSize, Orientation.IDENT);
    }

    /**
     * Method to rotate and/or mirror this NodeInst.
     * @param dOrient the change in Orientation of the NodeInst.
     */
    public void rotate(Orientation dOrient) {
        modifyInstance(0, 0, 0, 0, dOrient);
    }

    /**
     * Method to change this NodeInst.
     * @param dX the amount to move the NodeInst in X.
     * @param dY the amount to move the NodeInst in Y.
     * @param dXSize the amount to scale the NodeInst in X.
     * @param dYSize the amount to scale the NodeInst in Y.
     * @param dOrient the change of Orientation of the NodeInst.
     */
    public void modifyInstance(double dX, double dY, double dXSize, double dYSize, Orientation dOrient) {
        if (ImmutableNodeInst.isCellCenter(protoType.getId())) {
            topology.cell.adjustReferencePoint(dX, dY);
            return;
        }

        // make the change
        ImmutableNodeInst oldD = getD();
        ImmutableNodeInst d = oldD;
        if (dX != 0 || dY != 0) {
            d = d.withAnchor(new EPoint(d.anchor.getX() + dX, d.anchor.getY() + dY));
        }
        if (protoType instanceof PrimitiveNode) {
            double lambdaX = d.size.getLambdaX() + dXSize;
            double lambdaY = d.size.getLambdaY() + dYSize;
            d = d.withSize(EPoint.fromLambda(lambdaX, lambdaY));
        }
        d = d.withOrient(dOrient.concatenate(d.orient));
        lowLevelModify(d);
        if (topology != null) {
            Constraints.getCurrent().modifyNodeInst(this, oldD);
        }

//        // change the coordinates of every arc end connected to this
//        for(Iterator<Connection> it = getConnections(); it.hasNext(); ) {
//            Connection con = it.next();
//            if (con.getPortInst().getNodeInst() == this) {
//                Point2D oldLocation = con.getLocation();
//                switch (con.getEndIndex()) {
//                    case ArcInst.HEADEND:
//                        con.getArc().modify(0, dX, dY, 0, 0);
//                        break;
//                    case ArcInst.TAILEND:
//                        con.getArc().modify(0, 0, 0, dX, dY);
//                        break;
//                }
//            }
//        }
    }

    /**
     * Method to change many NodeInsts.
     * @param nis the NodeInsts to change.
     * @param dXs the amount to move the NodeInsts in X, or null.
     * @param dYs the amount to move the NodeInsts in Y, or null.
     * @param dXSizes the amount to scale the NodeInsts in X, or null.
     * @param dYSizes the amount to scale the NodeInsts in Y, or null.
     */
    public static void modifyInstances(NodeInst[] nis, double[] dXs, double[] dYs, double[] dXSizes, double[] dYSizes) {
        // make the change
        for (int i = 0; i < nis.length; i++) {
            NodeInst ni = nis[i];
            if (ni == null) {
                continue;
            }
            double dX = dXs != null ? dXs[i] : 0;
            double dY = dYs != null ? dYs[i] : 0;
            double dXSize = dXSizes != null ? dXSizes[i] : 0;
            double dYSize = dYSizes != null ? dYSizes[i] : 0;
            if (ni.getProto() == Generic.tech().cellCenterNode) {
                continue;
            }
            ni.modifyInstance(dX, dY, dXSize, dYSize, Orientation.IDENT);
        }


        for (int i = 0; i < nis.length; i++) {
            NodeInst ni = nis[i];
            if (ni == null) {
                continue;
            }
            if (ni.getProto() != Generic.tech().cellCenterNode) {
                continue;
            }
            double dX = dXs != null ? dXs[i] : 0;
            double dY = dYs != null ? dYs[i] : 0;
            ni.topology.cell.adjustReferencePoint(dX, dY);
        }
    }

    /**
     * Method to replace this NodeInst with one of another type.
     * All arcs and exports on this NodeInst are moved to the new one.
     * @param np the new type to put in place of this NodeInst.
     * @param ignorePortNames true to not use port names when determining association between old and new prototype.
     * @param allowMissingPorts true to allow replacement to have missing ports and, therefore, delete the arcs that used to be there.
     * @return the new NodeInst that replaces this one.
     * Returns null if there is an error doing the replacement.
     */
    public NodeInst replace(NodeProto np, boolean ignorePortNames, boolean allowMissingPorts) {
        // check for recursion
        if (np instanceof Cell) {
            if (Cell.isInstantiationRecursive((Cell) np, topology.cell)) {
                System.out.println("Cannot replace because it would be recursive");
                return null;
            }
        }

        // get the location of the cell-center on the old NodeInst
        Point2D oldCenter = getAnchorCenter();

        // create the new NodeInst
        double newXS = np.getDefWidth();
        double newYS = np.getDefHeight();
        if ((np instanceof PrimitiveNode) && (getProto() instanceof PrimitiveNode)) {
            // replacing one primitive with another: adjust sizes accordingly
            SizeOffset oldSO = getProto().getProtoSizeOffset();
            SizeOffset newSO = np.getProtoSizeOffset();
            newXS = getXSize() - oldSO.getLowXOffset() - oldSO.getHighXOffset() + newSO.getLowXOffset() + newSO.getHighXOffset();
            newYS = getYSize() - oldSO.getLowYOffset() - oldSO.getHighYOffset() + newSO.getLowYOffset() + newSO.getHighYOffset();

//			// test for minimum sizes if not dealing with pure-layer nodes
//			if (np.getFunction() != PrimitiveNode.Function.NODE)
//			{
//				// if less than min size, set it to min size
//	            if (newXS < np.getDefWidth()) newXS = np.getDefWidth();
//	            if (newYS < np.getDefHeight()) newYS = np.getDefHeight();
//
//	            // if old prim is min size, set new prim to min size
//	            if (getXSize() == getProto().getDefWidth()) newXS = np.getDefWidth();
//	            if (getYSize() == getProto().getDefHeight()) newYS = np.getDefHeight();
//			}
        }

        // see if nodeinst is mirrored
        NodeInst newNi = NodeInst.newInstance(np, oldCenter, newXS, newYS, topology.cell, getOrient(), null);
        if (newNi == null) {
            return null;
        }

        // draw new node expanded if appropriate
        if (np instanceof Cell) {
            if (getProto() instanceof Cell) {
                // replacing an instance: copy the expansion information
                newNi.setExpanded(isExpanded());
            }
        }

        // associate the ports between these nodes
        PortAssociation[] oldAssoc = portAssociate(this, newNi, ignorePortNames);

        // see if the old arcs can connect to ports
        double arcDx = 0, arcDy = 0;
        int arcCount = 0;
        String portMismatchError = null;
        List<String> arcMismatchErrors = null;
        for (Iterator<Connection> it = getConnections(); it.hasNext();) {
            Connection con = it.next();

            // make sure there is an association for this port
            int index = 0;
            for (; index < oldAssoc.length; index++) {
                if (oldAssoc[index].portInst == con.getPortInst()) {
                    break;
                }
            }
            if (index >= oldAssoc.length || oldAssoc[index].assn == null) {
                if (allowMissingPorts) {
                    continue;
                }
                if (portMismatchError != null) {
                    portMismatchError += ",";
                } else {
                    portMismatchError = "No port on new node has same name and location as old node port(s):";
                }
                portMismatchError += " " + con.getPortInst().getPortProto().getName();
                continue;
            }

            // make sure the arc can connect to this type of port
            PortInst opi = oldAssoc[index].assn;
            ArcInst ai = con.getArc();
            if (!opi.getPortProto().connectsTo(ai.getProto())) {
                if (allowMissingPorts) {
                    continue;
                }
                if (arcMismatchErrors == null) {
                    arcMismatchErrors = new ArrayList<String>();
                }
                arcMismatchErrors.add(ai + " on old port " + con.getPortInst().getPortProto().getName()
                        + " cannot connect to new port " + opi.getPortProto().getName());
                continue;
            }

            // see if the arc fits in the new port
            Poly poly = opi.getPoly();
            if (!poly.isInside(con.getLocation())) {
                // arc doesn't fit: accumulate error distance
                double xp = poly.getCenterX();
                double yp = poly.getCenterY();
                arcDx += xp - con.getLocation().getX();
                arcDy += yp - con.getLocation().getY();
            }
            arcCount++;
        }
        if (portMismatchError != null || arcMismatchErrors != null) {
            if (portMismatchError != null) {
                System.out.println(portMismatchError);
            }
            if (arcMismatchErrors != null) {
                for (String err : arcMismatchErrors) {
                    System.out.println(err);
                }
            }
            newNi.kill();
            return null;
        }

        // see if the old exports have the same connections
        List<String> exportErrors = null;
        for (Iterator<Export> it = getExports(); it.hasNext();) {
            Export pp = it.next();

            // make sure there is an association for this port
            int index = 0;
            for (; index < oldAssoc.length; index++) {
                if (oldAssoc[index].portInst == pp.getOriginalPort()) {
                    break;
                }
            }
            if (index >= oldAssoc.length || oldAssoc[index].assn == null) {
                if (exportErrors == null) {
                    exportErrors = new ArrayList<String>();
                }
                exportErrors.add("No port on new node has same name and location as old node port: "
                        + pp.getOriginalPort().getPortProto().getName());
                continue;
            }
            PortInst opi = oldAssoc[index].assn;

            // ensure that all arcs connected at exports still connect
            if (pp.doesntConnect(opi.getPortProto().getBasePort())) {
                newNi.kill();
                return null;
            }
        }
        if (exportErrors != null) {
            for (String msg : exportErrors) {
                System.out.println(msg);
            }
            newNi.kill();
            return null;
        }

        // now replace all of the arcs
        Set<ArcInst> arcList = new HashSet<ArcInst>();
        for (Iterator<Connection> it = getConnections(); it.hasNext();) {
            arcList.add(it.next().getArc());
        }
        for (ArcInst ai : arcList) {
            PortInst[] newPortInst = new PortInst[2];
            Point2D[] newPoint = new Point2D[2];
            int otherEnd = 0;
            for (int e = 0; e < 2; e++) {
                PortInst pi = ai.getPortInst(e);
                if (pi.getNodeInst() != this) {
                    // end of arc connected elsewhere: keep the information
                    newPortInst[e] = pi;
                    newPoint[e] = ai.getLocation(e);
                    otherEnd = e;
                } else {
                    // end of arc connected to replaced node: translate to new node
                    int index = 0;
                    for (; index < oldAssoc.length; index++) {
                        if (oldAssoc[index].portInst == pi) {
                            break;
                        }
                    }
                    if (index >= oldAssoc.length || oldAssoc[index].assn == null) {
                        if (allowMissingPorts) {
                            continue;
                        }
                        System.out.println("No port on new node has same name and location as old node port: "
                                + pi.getPortProto().getName());
                        newNi.kill();
                        return null;
                    }

                    // make sure the arc can connect to this type of port
                    PortInst opi = oldAssoc[index].assn;
                    if (opi == null) {
                        if (!allowMissingPorts) {
                            System.out.println("Cannot re-connect " + ai);
                        } else {
                            ai.kill();
                        }
                        continue;
                    }
                    newPortInst[e] = opi;
                    Poly poly = opi.getPoly();
                    EPoint newLoc = ai.getLocation(e);
                    newPoint[e] = poly.isInside(newLoc) ? newLoc : new EPoint(poly.getCenterX(), poly.getCenterY());
                }
            }
            if (newPortInst[ArcInst.TAILEND] == null || newPortInst[ArcInst.HEADEND] == null) {
                continue;
            }

            // see if a bend must be made in the wire
            boolean zigzag = false;
            if (ai.isFixedAngle()) {
                if (newPoint[0].getX() != newPoint[1].getX() || newPoint[0].getY() != newPoint[1].getY()) {
                    int ii = DBMath.figureAngle(newPoint[0], newPoint[1]);
                    int ang = ai.getAngle();
                    if ((ii % 1800) != (ang % 1800)) {
                        zigzag = true;
                    }
                }
            }

            // see if a bend can be a straight by some simple manipulations
            if (zigzag && !ai.isRigid() && (ai.getAngle() % 900) == 0) {
                // find the node at the other end
                NodeInst adjustThisNode = ai.getPortInst(otherEnd).getNodeInst();
                if (!adjustThisNode.hasExports()) {
                    // other end not exported, see if all arcs can be adjusted
                    boolean adjustable = true;
                    for (Iterator<Connection> oIt = adjustThisNode.getConnections(); oIt.hasNext();) {
                        Connection otherCon = oIt.next();
                        ArcInst otherArc = otherCon.getArc();
                        if (otherArc == ai) {
                            continue;
                        }
                        if (otherArc.isRigid()) {
                            adjustable = false;
                            break;
                        }
                        if (otherArc.getAngle() % 900 != 0) {
                            adjustable = false;
                            break;
                        }
                        if (((ai.getAngle() / 900) & 1) == ((otherArc.getAngle() / 900) & 1)) {
                            adjustable = false;
                            break;
                        }
                    }
                    if (adjustable) {
                        double dX = 0, dY = 0;
                        if ((ai.getAngle() % 1800) == 0) {
                            // horizontal arc: move the other node vertically
                            dY = newPoint[1 - otherEnd].getY() - newPoint[otherEnd].getY();
                            newPoint[otherEnd] = new Point2D.Double(newPoint[otherEnd].getX(), newPoint[1 - otherEnd].getY());
                        } else {
                            // vertical arc: move the other node horizontaly
                            dX = newPoint[1 - otherEnd].getX() - newPoint[otherEnd].getX();
                            newPoint[otherEnd] = new Point2D.Double(newPoint[1 - otherEnd].getX(), newPoint[otherEnd].getY());
                        }

                        // special case where the old arc must be deleted first so that the other node can move
                        ai.kill();
                        adjustThisNode.move(dX, dY);
                        ArcInst newAi = ArcInst.newInstanceBase(ai.getProto(), ai.getLambdaBaseWidth(), newPortInst[ArcInst.HEADEND], newPortInst[ArcInst.TAILEND],
                                newPoint[ArcInst.HEADEND], newPoint[ArcInst.TAILEND], ai.getName(), 0);
                        if (newAi == null) {
                            newNi.kill();
                            return null;
                        }
                        newAi.copyPropertiesFrom(ai);
                        continue;
                    }
                }
            }

            ArcInst newAi;
            if (zigzag) {
                // make that two wires
                double cX = newPoint[0].getX();
                double cY = newPoint[1].getY();
                EditingPreferences ep = getEditingPreferences();
                NodeProto pinNp = ai.getProto().findOverridablePinProto(ep);
                double psx = pinNp.getDefWidth();
                double psy = pinNp.getDefHeight();
                NodeInst pinNi = NodeInst.newInstance(pinNp, new Point2D.Double(cX, cY), psx, psy, topology.cell);
                PortInst pinPi = pinNi.getOnlyPortInst();
                newAi = ArcInst.newInstanceBase(ai.getProto(), ai.getLambdaBaseWidth(), newPortInst[ArcInst.HEADEND], pinPi, newPoint[ArcInst.HEADEND],
                        new Point2D.Double(cX, cY), null, 0);
                if (newAi == null) {
                    return null;
                }
                newAi.copyPropertiesFrom(ai);

                ArcInst newAi2 = ArcInst.newInstanceBase(ai.getProto(), ai.getLambdaBaseWidth(), pinPi, newPortInst[ArcInst.TAILEND], new Point2D.Double(cX, cY),
                        newPoint[ArcInst.TAILEND], null, 0);
                if (newAi2 == null) {
                    return null;
                }
                newAi2.copyConstraintsFrom(ai);
                if (newPortInst[ArcInst.TAILEND].getNodeInst() == this) {
                    ArcInst aiSwap = newAi;
                    newAi = newAi2;
                    newAi2 = aiSwap;
                }
            } else {
                // replace the arc with another arc
                newAi = ArcInst.newInstanceBase(ai.getProto(), ai.getLambdaBaseWidth(), newPortInst[ArcInst.HEADEND], newPortInst[ArcInst.TAILEND],
                        newPoint[ArcInst.HEADEND], newPoint[ArcInst.TAILEND], null, 0);
                if (newAi == null) {
                    newNi.kill();
                    return null;
                }
                newAi.copyPropertiesFrom(ai);
            }
            ai.kill();
            newAi.setName(ai.getName());
        }

        // now replace all of the exports
        List<Export> exportList = new ArrayList<Export>();
        for (Iterator<Export> it = getExports(); it.hasNext();) {
            exportList.add(it.next());
        }
        for (Export pp : exportList) {
            int index = 0;
            for (; index < oldAssoc.length; index++) {
                if (oldAssoc[index].portInst == pp.getOriginalPort()) {
                    break;
                }
            }
            if (index >= oldAssoc.length || oldAssoc[index].assn == null) {
                continue;
            }
            PortInst newPi = oldAssoc[index].assn;
            pp.move(newPi);
        }

        // copy all variables on the nodeinst
        newNi.copyVarsFrom(this);
        newNi.copyTextDescriptorFrom(this, NodeInst.NODE_NAME);
        newNi.copyTextDescriptorFrom(this, NodeInst.NODE_PROTO);
        newNi.copyStateBitsAndExpandedFlag(this);
        if (!getNameKey().isTempname()) {
            String savedName = getName();
            String tempName = ElectricObject.uniqueObjectName(savedName, topology.cell, NodeInst.class, true, true);
            setName(tempName);
            newNi.setName(savedName);
        }

        // now delete the original nodeinst
        kill();
        return newNi;
    }

    /****************************** LOW-LEVEL IMPLEMENTATION ******************************/
    /**
     * Returns persistent data of this NodeInst.
     * @return persistent data of this NodeInst.
     */
    @Override
    public ImmutableNodeInst getD() {
        return d;
    }

    /**
     * Modifies persistend data of this NodeInst.
     * @param newD new persistent data.
     * @param notify true to notify Undo system.
     * @return true if persistent data was modified.
     */
    public boolean setD(ImmutableNodeInst newD, boolean notify) {
        checkChanging();
        ImmutableNodeInst oldD = d;
        if (newD == oldD) {
            return false;
        }
        if (topology != null) {
            topology.cell.setTopologyModified();
            d = newD;
            assert protoType == d.protoId.inDatabase(getDatabase());
            if (notify) {
                Constraints.getCurrent().modifyNodeInst(this, oldD);
            }
        } else {
            d = newD;
            assert protoType.getId() == d.protoId;
        }
        return true;
    }

    public void setDInUndo(ImmutableNodeInst newD) {
        checkUndoing();
        assert d.protoId.isIcon() == newD.protoId.isIcon();
        d = newD;
        protoType = d.protoId.inDatabase(getDatabase());
        validVisBounds = false;
    }

    /**
     * Method to add a Variable on this NodeInst.
     * It may add repaired copy of this Variable in some cases.
     * This methood is overriden in IconNodeInst
     * @param var Variable to add.
     */
    public void addVar(Variable var) {
        if (setD(d.withVariable(var), true)) // check for side-effects of the change
        {
            checkPossibleVariableEffects(var.getKey());
        }
    }

    /**
     * Package-private method to add a Variable on PortInst of this NodeInst.
     * It may add repaired copy of this Variable in some cases.
     * @param portProtoId PortProtoId of the PortInst.
     * @param var Variable to add.
     */
    void addVar(PortProtoId portProtoId, Variable var) {
        setD(d.withPortInst(portProtoId, d.getPortInst(portProtoId).withVariable(var)), true);
    }

    /**
     * Method to delete a Variable from this NodeInst.
     * @param key the key of the Variable to delete.
     */
    public void delVar(Variable.Key key) {
        if (setD(d.withoutVariable(key), true)) // check for side-effects of the change
        {
            checkPossibleVariableEffects(key);
        }
    }

    /**
     * Package-private method to delete a Variable from PortInst of this NodeInst.
     * @param portProtoId PortProtoId of the PortInst.
     * @param key the key of the Variable to delete.
     */
    void delVar(PortProtoId portProtoId, Variable.Key key) {
        setD(d.withPortInst(portProtoId, d.getPortInst(portProtoId).withoutVariable(key)), true);
    }

    /**
     * Package-private method to delete all Variables from PortInst of this NodeInst.
     * @param portProtoId PortProtoId of the PortInst.
     */
    void delVars(PortProtoId portProtoId) {
        setD(d.withPortInst(portProtoId, ImmutablePortInst.EMPTY), true);
    }

    /**
     * Method to adjust this NodeInst by the specified deltas.
     * This method does not go through change control, and so should not be used unless you know what you are doing.
     * New persistent data may differ from old one only by anchor, size and orientation
     * @param d the new persistent data of this NodeInst.
     */
    public void lowLevelModify(ImmutableNodeInst d) {
        if (topology != null) {
            checkChanging();
            boolean renamed = this.d.name != d.name;
            if (renamed) {
                topology.removeNodeName(this);
            }

            // make the change
            setD(d, false);
            if (renamed) {
                topology.addNodeName(this);
            }

            // fill in the Geometric fields
            redoGeometric();
        } else {
            this.d = d;
            redoGeometric();
        }
    }

    /**
     * Method to tell whether this NodeInst is an icon of its parent.
     * Electric does not allow recursive circuit hierarchies (instances of Cells inside of themselves).
     * However, it does allow one exception: a schematic may contain its own icon for documentation purposes.
     * This method determines whether this NodeInst is such an icon.
     * @return true if this NodeInst is an icon of its parent.
     */
    public boolean isIconOfParent() {
        return (protoType instanceof Cell) && ((Cell) protoType).isIconOf(topology.cell);
    }

    /**
     * Method to set an index of this NodeInst in Cell nodes.
     * This is a zero-based index of nodes on the Cell.
     * @param nodeIndex an index of this NodeInst in Cell nodes.
     */
    public void setNodeIndex(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

    /**
     * Method to get the index of this NodeInst.
     * This is a zero-based index of nodes on the Cell.
     * @return the index of this NodeInst.
     */
    public final int getNodeIndex() {
        return nodeIndex;
    }

    /**
     * Method tells if this NodeInst is linked to parent Cell.
     * @return true if this NodeInst is linked to parent Cell.
     */
    public boolean isLinked() {
        try {
            if (topology == null) {
                return false;
            }
            Cell parent = topology.cell;
            return parent.isLinked() && parent.getNode(nodeIndex) == this;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    /**
     * Method to return the Cell Topology that contains this NodeInst.
     * @return the Topology that contains this NodeInst.
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
        if (topology != null) {
            topology.cell.checkChanging();
        }
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
     * Returns database to which this NodeInst belongs.
     * Some objects are not in database, for example NodeInsts in PaletteFrame.
     * Method returns null for non-database objects.
     * @return database to which this Geometric belongs.
     */
    @Override
    public EDatabase getDatabase() {
        return topology != null ? topology.cell.getDatabase() : null;
    }

    /**
     * Returns CellTree containing this NodeInst.
     * If this node hasn't parent, a dummy CellTree is returned.
     * @return CellTree containing this NodeInst.
     */
    public CellTree getCellTreeUnsafe() {
        if (topology != null) {
            return topology.cell.treeUnsafe();
        }
        CellId clipCellId = Clipboard.getClipCellId();
        ImmutableCell cell = ImmutableCell.newInstance(clipCellId, 0);
        cell = cell.withTechId(getProto().getTechnology().getId());
        TechPool techPool = TechPool.getThreadTechPool();
        CellBackup cellBackup = CellBackup.newInstance(cell, techPool);
        cellBackup = cellBackup.with(cell, new ImmutableNodeInst[]{getD()}, null, null, techPool);
        CellTree[] subTrees = CellTree.NULL_ARRAY;
        if (isCellInstance()) {
            Cell subCell = (Cell) protoType;
            CellUsage cu = clipCellId.getUsageIn(subCell.getId());
            subTrees = new CellTree[cu.indexInParent + 1];
            subTrees[cu.indexInParent] = subCell.treeUnsafe();
        }
        CellTree cellTree = CellTree.newInstance(cell, techPool).with(cellBackup, subTrees, techPool);
        return cellTree;
    }

    /**
     * Returns CellBackup containing this NodeInst.
     * If this node hasn't parent, a dummy CellBackup is returned.
     * @return CellBackup containing this NodeInst.
     */
    public CellBackup getCellBackupUnsafe() {
        if (topology != null) {
            return topology.cell.backupUnsafe();
        }
        CellId clipCellId = Clipboard.getClipCellId();
        ImmutableCell cell = ImmutableCell.newInstance(clipCellId, 0);
        cell = cell.withTechId(getProto().getTechnology().getId());
        TechPool techPool = TechPool.getThreadTechPool();
        CellBackup cellBackup = CellBackup.newInstance(cell, techPool);
        cellBackup = cellBackup.with(cell, new ImmutableNodeInst[]{getD()}, null, null, techPool);
        return cellBackup;
    }

    /****************************** GRAPHICS ******************************/
    /**
     * Method to return the Orientation of this NodeInst.
     * @return the Orientation of this NodeInst.
     */
    public Orientation getOrient() {
        return d.orient;
    }

    /**
     * Method to return the rotation angle of this NodeInst.
     * @return the rotation angle of this NodeInst (in tenth-degrees).
     */
    public int getAngle() {
        return d.orient.getAngle();
    }

    /**
     * Method to return the center point of this NodeInst object.
     * @return the center point of this NodeInst object.
     */
    public EPoint getAnchorCenter() {
        return d.anchor;
    }

    /**
     * Method to return the center X coordinate of this NodeInst.
     * @return the center X coordinate of this NodeInst.
     */
    public double getAnchorCenterX() {
        return d.anchor.getX();
    }

    /**
     * Method to return the center Y coordinate of this NodeInst.
     * @return the center Y coordinate of this NodeInst.
     */
    public double getAnchorCenterY() {
        return d.anchor.getY();
    }

    /**
     * Method to return the X size of this NodeInst.
     * @return the X size of this NodeInst.
     */
    public double getXSize() {
        if (protoType instanceof Cell) {
            return protoType.getDefWidth();
        }
        long fullWidth = ((PrimitiveNode) protoType).getFullRectangle().getGridWidth();
        return DBMath.gridToLambda(d.size.getGridX() + fullWidth);
    }

    /**
     * Method to return the base X size of this NodeInst in lambda units.
     * @return the base X size of this NodeInst.
     */
    public double getLambdaBaseXSize() {
        if (protoType instanceof Cell) {
            return protoType.getDefWidth();
        }
        return DBMath.gridToLambda(d.size.getGridX() + getBaseRectangle().getGridWidth());
    }

    /**
     * Method similar to getXSize() to return the X size of this NodeInst without the offset.
     * @return the X size of this NodeInst.
     */
    public double getXSizeWithoutOffset() {
        return GenMath.isNinetyDegreeRotation(getAngle()) ? getLambdaBaseYSize() : getLambdaBaseXSize();
    }

    /**
     * Method to return the Y size of this NodeInst.
     * @return the Y size of this NodeInst.
     */
    public double getYSize() {
        if (protoType instanceof Cell) {
            return protoType.getDefHeight();
        }
        long fullHeight = ((PrimitiveNode) protoType).getFullRectangle().getGridHeight();
        return DBMath.gridToLambda(d.size.getGridY() + fullHeight);
    }

    /**
     * Method to return the base Y size of this NodeInst in lambda units.
     * @return the base Y size of this NodeInst.
     */
    public double getLambdaBaseYSize() {
        if (protoType instanceof Cell) {
            return protoType.getDefHeight();
        }
        return DBMath.gridToLambda(d.size.getGridY() + getBaseRectangle().getGridHeight());
    }

    /**
     * Method similar to getXSize() to return the X size of this NodeInst without the offset.
     * @return the X size of this NodeInst.
     */
    public double getYSizeWithoutOffset() {
        return GenMath.isNinetyDegreeRotation(getAngle()) ? getLambdaBaseXSize() : getLambdaBaseYSize();
    }

    /**
     * Method to return whether NodeInst is mirrored about a
     * horizontal line running through its center.
     * @return true if mirrored.
     */
    public boolean isMirroredAboutXAxis() {
        return isYMirrored();
    }

    /**
     * Method to return whether NodeInst is mirrored about a
     * vertical line running through its center.
     * @return true if mirrored.
     */
    public boolean isMirroredAboutYAxis() {
        return isXMirrored();
    }

    /**
     * Method to tell whether this NodeInst is mirrored in the X coordinate.
     * Mirroring in the X axis implies that X coordinates are negated.
     * Thus, it is equivalent to mirroring ABOUT the Y axis.
     * @return true if this NodeInst is mirrored in the X coordinate.
     */
    public boolean isXMirrored() {
        return d.orient.isXMirrored();
    }

    /**
     * Method to tell whether this NodeInst is mirrored in the Y coordinate.
     * Mirroring in the Y axis implies that Y coordinates are negated.
     * Thus, it is equivalent to mirroring ABOUT the X axis.
     * @return true if this NodeInst is mirrored in the Y coordinate.
     */
    public boolean isYMirrored() {
        return d.orient.isYMirrored();
    }

    /**
     * Returns the polygons that describe this NodeInst.
     * @param polyBuilder Poly builder.
     * @return an iterator on Poly objects that describes this NodeInst graphically.
     * These objects include displayable variables on the NodeInst.
     */
    @Override
    public Iterator<Poly> getShape(Poly.Builder polyBuilder) {
        return polyBuilder.getShape(this);
    }

    /**
     * Returns the polygon that describe the base highlight of this NodeInst.
     * @return a  Poly object that describes the highlight of this NodeInst graphically.
     */
    public Poly getBaseShape() {
        return getBaseShape(d.anchor, d.size);
    }

    /**
     * Returns the polygon that describe the base highlight of this NodeInst with modified size.
     * @param baseWidth modified base width in lambda units
     * @param baseHeight modified base height in lambda units
     * @return a  Poly object that describes the highlight of this NodeInst graphically.
     */
    public Poly getBaseShape(EPoint anchor, double baseWidth, double baseHeight) {
        EPoint newSize = EPoint.ORIGIN;
        if (protoType instanceof PrimitiveNode) {
            ERectangle base = getBaseRectangle();
            newSize = EPoint.fromLambda(baseWidth - base.getWidth(), baseHeight - base.getHeight());
        }
        return getBaseShape(anchor, newSize);
    }

    /**
     * Returns the polygon that describe the base highlight of this NodeInst with modified size.
     * @param size modified size
     * @return a  Poly object that describes the highlight of this NodeInst graphically.
     */
    private Poly getBaseShape(EPoint anchor, EPoint size) {
        double nodeLowX;
        double nodeHighX;
        double nodeLowY;
        double nodeHighY;
        if (protoType instanceof Cell) {
            ERectangle r = ((Cell) protoType).getBounds();
            nodeLowX = r.getLambdaMinX();
            nodeHighX = r.getLambdaMaxX();
            nodeLowY = r.getLambdaMinY();
            nodeHighY = r.getLambdaMaxY();
        } else {
            ERectangle baseRect = getBaseRectangle();
            long halfW = size.getGridX() >> 1;
            long halfH = size.getGridY() >> 1;
            nodeLowX = DBMath.gridToLambda(-halfW + baseRect.getGridMinX());
            nodeHighX = DBMath.gridToLambda(halfW + baseRect.getGridMaxX());
            nodeLowY = DBMath.gridToLambda(-halfH + baseRect.getGridMinY());
            nodeHighY = DBMath.gridToLambda(halfH + baseRect.getGridMaxY());
        }
        Point2D[] points;
        if (nodeLowX != nodeHighX || nodeLowY != nodeHighY) {
            points = Poly.makePoints(nodeLowX, nodeHighX, nodeLowY, nodeHighY);
        } else {
            points = new Point2D[]{new Point2D.Double(nodeLowX, nodeLowY)};
        }
        Poly poly = new Poly(points);
        AffineTransform trans = getOrient().rotateAbout(anchor.getLambdaX(), anchor.getLambdaY(), 0, 0);
        poly.transform(trans);
        return poly;
    }

    /**
     * Method to return the bounds of this NodeInst.
     * TODO: dangerous to give a pointer to our internal field; should make a copy of visBounds
     * @return the bounds of this NodeInst.
     */
    @Override
    public Rectangle2D getBounds() {
        if (!validVisBounds) {
            computeBounds();
        }
        return visBounds;
    }

    private void computeBounds() {
//        double oldX = visBounds.x;
//        double oldY = visBounds.y;
//        double oldWidth = visBounds.width;
//        double oldHeight = visBounds.height;
        // handle cell bounds
        if (d.protoId instanceof CellId) {
            // offset by distance from cell-center to the true center
            Cell subCell = (Cell) getProto();
            Rectangle2D bounds = subCell.getBounds();
            d.orient.rectangleBounds(bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY(), d.anchor.getX(), d.anchor.getY(), visBounds);
        } else {
            BoundsBuilder b = new BoundsBuilder(getCellBackupUnsafe());
            d.computeBounds(b, visBounds);
        }
//        if ((oldX != visBounds.x || oldY != visBounds.y || oldWidth != visBounds.width || oldHeight != visBounds.height)
//                && parent != null) {
//            parent.setDirty();
//        }
        validVisBounds = true;
    }

    /**
     * Method to recalculate the Geometric bounds for this NodeInst.
     */
    public void redoGeometric() {
        if (topology != null) {
            topology.cell.unfreshRTree();
        }
        validVisBounds = false;
    }

    /**
     * Method to return the starting and ending angle of an arc described by this NodeInst.
     * These values can be found in the "ART_degrees" variable on the NodeInst.
     * @return a 2-long double array with the starting offset in the first entry (a value in radians)
     * and the amount of curvature in the second entry (in radians).
     * If the NodeInst does not have circular information, both values are set to zero.
     */
    public double[] getArcDegrees() {
        double[] returnValues = new double[2];
        if (protoType != Artwork.tech().circleNode && protoType != Artwork.tech().thickCircleNode) {
            return returnValues;
        }
        return getD().getArcDegrees();
    }

    /**
     * Method to set the starting and ending angle of an arc described by this NodeInst.
     * These values are stored in the "ART_degrees" variable on the NodeInst.
     * @param start the starting offset of the angle, in radians (typically 0).
     * @param curvature the the amount of curvature, in radians.
     */
    public void setArcDegrees(double start, double curvature) {
        if (!(protoType instanceof PrimitiveNode)) {
            return;
        }
        if (protoType != Artwork.tech().circleNode && protoType != Artwork.tech().thickCircleNode) {
            return;
        }
        if (start == 0 && curvature == 0) {
            if (getVar(Artwork.ART_DEGREES) == null) {
                return;
            }
            delVar(Artwork.ART_DEGREES);
        } else {
            Float[] fAddr = new Float[2];
            fAddr[0] = new Float(start);
            fAddr[1] = new Float(curvature);
            newVar(Artwork.ART_DEGREES, fAddr);
        }
    }

    /**
     * Method to get the base (highlight) ERectangle associated with a NodeInst
     * in this PrimitiveNode.
     * Base ERectangle is a highlight rectangle of standard-size NodeInst of
     * this PrimtiveNode
     * By having this be a method of Technology, it can be overridden by
     * individual Technologies that need to make special considerations.
     * @param ni the NodeInst to query.
     * @return the base ERectangle of this PrimitiveNode.
     */
    private ERectangle getBaseRectangle() {
        return ((PrimitiveNode) protoType).getBaseRectangle();
//        return ((PrimitiveNode)protoType).getTechnology().getNodeInstBaseRectangle(this);
    }

//	/**
//	 * Method to return a list of Polys that describes all text on this NodeInst.
//	 * @param hardToSelect is true if considering hard-to-select text.
//	 * @param wnd the window in which the text will be drawn.
//	 * @return an array of Polys that describes the text.
//	 */
//	public Poly [] getAllText(boolean hardToSelect, EditWindow0 wnd)
//	{
//		int cellInstanceNameText = 0;
//		if (protoType instanceof Cell && !isExpanded() && hardToSelect) cellInstanceNameText = 1;
//		if (!User.isTextVisibilityOnInstance()) cellInstanceNameText = 0;
//		int dispVars = numDisplayableVariables(false);
//		int numExports = 0;
//		int numExportVariables = 0;
//		if (User.isTextVisibilityOnExport())
//		{
//			numExports = getNumExports();
//			for(Iterator<Export> it = getExports(); it.hasNext(); )
//			{
//				Export pp = it.next();
//				numExportVariables += pp.numDisplayableVariables(false);
//			}
//		}
//		if (protoType == Generic.tech.invisiblePinNode &&
//			!User.isTextVisibilityOnAnnotation())
//		{
//			dispVars = numExports = numExportVariables = 0;
//		}
//		if (!User.isTextVisibilityOnNode())
//		{
//			cellInstanceNameText = dispVars = numExports = numExportVariables = 0;
//		}
//		int totalText = cellInstanceNameText + dispVars + numExports + numExportVariables;
//		if (totalText == 0) return null;
//		Poly [] polys = new Poly[totalText];
//		int start = 0;
//
//		// add in the cell name if appropriate
//		if (cellInstanceNameText != 0)
//		{
//			double cX = getTrueCenterX();
//			double cY = getTrueCenterY();
//			TextDescriptor td = getTextDescriptor(NodeInst.NODE_PROTO);
//			double offX = td.getXOff();
//			double offY = td.getYOff();
//			TextDescriptor.Position pos = td.getPos();
//			Poly.Type style = pos.getPolyType();
//			Point2D [] pointList = new Point2D.Double[1];
//			pointList[0] = new Point2D.Double(cX+offX, cY+offY);
//			polys[start] = new Poly(pointList);
//			polys[start].setStyle(style);
//			polys[start].setString(getProto().describe(false));
//			polys[start].setTextDescriptor(td);
//			polys[start].setDisplayedText(new DisplayedText(this, NODE_PROTO));
//			start++;
//		}
//
//		// add in the exports
//		if (numExports > 0)
//		{
//			AffineTransform unTrans = rotateIn();
//			for(Iterator<Export> it = getExports(); it.hasNext(); )
//			{
//				Export pp = it.next();
//				polys[start] = pp.getNamePoly();
//				polys[start].transform(unTrans);
//				start++;
//
//				// add in variables on the exports
//				Poly poly = pp.getOriginalPort().getPoly();
//				int numadded = pp.addDisplayableVariables(poly.getBounds2D(), polys, start, wnd, false);
//				for(int i=0; i<numadded; i++)
//				{
//					polys[start+i].setPort(pp);
//					polys[start+i].transform(unTrans);
//				}
//				start += numadded;
//			}
//		}
//
//		// add in the displayable variables
//		if (dispVars > 0)
//		{
//			addDisplayableVariables(getUntransformedBounds(), polys, start, wnd, false);
//		}
//		return polys;
//	}
    /**
     * Method to return the bounds of this NodeInst before it is transformed.
     * @return the bounds of this NodeInst before it is transformed.
     */
    public Rectangle2D getUntransformedBounds() {
        long lx, hx, ly, hy;
        if (protoType instanceof PrimitiveNode) {
            // primitive
            ERectangle baseRect = getBaseRectangle();
            long halfW = d.size.getGridX() >> 1;
            long halfH = d.size.getGridY() >> 1;
            lx = -halfW + baseRect.getGridMinX();
            hx = +halfW + baseRect.getGridMaxX();
            ly = -halfH + baseRect.getGridMinY();
            hy = +halfH + baseRect.getGridMaxY();
        } else {
            ERectangle bounds = ((Cell) protoType).getBounds();
            lx = bounds.getGridMinX();
            hx = bounds.getGridMaxX();
            ly = bounds.getGridMinY();
            hy = bounds.getGridMaxY();
        }
        EPoint anchor = getAnchorCenter();
        return ERectangle.fromGrid(lx + anchor.getGridX(), ly + anchor.getGridY(), hx - lx, hy - ly);
    }

    /**
     * Method to return the Parameter on this NodeInst with the given key.
     * Overridden in IconNodeInst.
     * @param key the key of the Parameter
     * @return null
     */
    public Variable getParameter(Variable.Key key) {
        return null;
    }

    /**
     * Method to tell if the Variable.Key is a defined parameters of this NodeInst.
     * Overridden in IconNodeInst.
     * @param key the key of the parameter
     * @return false
     */
    public boolean isDefinedParameter(Variable.Key key) {
        return false;
    }

    /**
     * Method to return an Iterator over all Parameters on this NodeInst.
     * Overridden in IconNodeInst
     * @return an empty Iterator
     */
    public Iterator<Variable> getParameters() {
        return ArrayIterator.emptyIterator();
    }

    /**
     * Method to return an Iterator over defined Parameters on this Nodable.
     * Overridden in IconNodeInst
     * @return an empty Iterator
     */
    public Iterator<Variable> getDefinedParameters() {
        return ArrayIterator.emptyIterator();
    }

    /**
     * Method to add a Parameter to this NodeInst.
     * Overridden in IconNodeInst
     * @param param the Variable to add.
     */
    public void addParameter(Variable param) {
    }

    /**
     * Method to delete a defined Parameter from this NodeInst.
     * Overridden in IconNodeInst
     * @param key the key of the Variable to delete.
     */
    public void delParameter(Variable.Key key) {
    }

    /**
     * Method to update a Parameter on this NodeInst with the specified values.
     * If the Variable already exists, only the value is changed; the displayable attributes are preserved.
     * @param key the key of the Variable.
     * @param value the object to store in the Variable.
     * @return the Variable that has been updated.
     */
    public Variable updateParam(Variable.Key key, Object value) {
        return null;
    }

    /**
     * Method to return the number of displayable Variables on this NodeInst and all of its PortInsts.
     * A displayable Variable is one that will be shown with its object.
     * Displayable Variables can only sensibly exist on NodeInst, ArcInst, and PortInst objects.
     * @return the number of displayable Variables on this NodeInst and all of its PortInsts.
     */
    public int numDisplayableVariables(boolean multipleStrings) {
        int numVarsOnNode = super.numDisplayableVariables(multipleStrings);
        if (isUsernamed()) {
            numVarsOnNode++;
        }

        for (Iterator<PortInst> it = getPortInsts(); it.hasNext();) {
            PortInst pi = it.next();
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
    public int addDisplayableVariables(Rectangle2D rect, Poly[] polys, int start, EditWindow0 wnd, boolean multipleStrings) {
        int numAddedVariables = 0;
        if (isUsernamed()) {
            double cX = rect.getCenterX();
            double cY = rect.getCenterY();
            TextDescriptor td = d.nameDescriptor;
            double offX = td.getXOff();
            double offY = td.getYOff();
            TextDescriptor.Position pos = td.getPos();
            Poly.Type style = pos.getPolyType();

            if (offX != 0 || offY != 0) {
                td = td.withOff(0, 0);
                style = Poly.rotateType(style, this);
            }

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
            polys[start].setDisplayedText(new DisplayedText(this, NODE_NAME));
            numAddedVariables = 1;
        }
        numAddedVariables += super.addDisplayableVariables(rect, polys, start + numAddedVariables, wnd, multipleStrings);

        for (Iterator<PortInst> it = getPortInsts(); it.hasNext();) {
            PortInst pi = it.next();
            int justAdded = pi.addDisplayableVariables(rect, polys, start + numAddedVariables, wnd, multipleStrings);
            for (int i = 0; i < justAdded; i++) {
                polys[start + numAddedVariables + i].setPort(pi.getPortProto());
            }
            numAddedVariables += justAdded;
        }
        return numAddedVariables;
    }

    /**
     * Method to get all displayable Variables on this NodeInst and its PortInsts to an array of Poly objects.
     * This Poly were not transformed by Node transform.
     * @param wnd window in which the Variables will be displayed.
     * @return an array of Poly objects with displayable variables.
     */
    public Poly[] getDisplayableVariables(EditWindow0 wnd) {
        return getDisplayableVariables(getUntransformedBounds(), wnd, true);
    }

    /**
     * Method to return a transformation that moves up the hierarchy.
     * Presuming that this NodeInst is a Cell instance, the
     * transformation maps points in the Cell's coordinate space
     * into this NodeInst's parent Cell's coordinate space.
     * @return a transformation that moves up the hierarchy.
     */
    public AffineTransform transformOut() {
        return d.orient.rotateAbout(getAnchorCenterX(), getAnchorCenterY(), 0, 0);
//		// The transform first translates to the position of the
//		// NodeInst's Anchor point in the parent Cell, and then rotates and
//		// mirrors about the anchor point.
//		AffineTransform xform = rotateOut();
//		xform.concatenate(translateOut());
//		return xform;
    }

    /**
     * Method to return a transformation that moves up the
     * hierarchy, combined with a previous transformation.
     * Presuming that this NodeInst is a Cell instance, the
     * transformation maps points in the Cell's coordinate space
     * into this NodeInst's parent Cell's coordinate space.
     * @param prevTransform the previous transformation to the NodeInst's Cell.
     * @return a transformation that translates up the hierarchy,
     * including the previous transformation.
     */
    public AffineTransform transformOut(AffineTransform prevTransform) {
        AffineTransform transform = transformOut();
        transform.preConcatenate(prevTransform);
        return transform;
    }

    /**
     * Method to return a transformation that moves down the hierarchy.
     * Presuming that this NodeInst is a Cell instance, the
     * transformation maps points in the Cell's coordinate space
     * into this NodeInst's parent Cell's coordinate space.
     * @return a transformation that moves down the hierarchy.
     */
    public AffineTransform transformIn() {
        return d.orient.inverse().rotateAbout(0, 0, -getAnchorCenterX(), -getAnchorCenterY());
//		// The transform first rotates in, and then translates to the position..
//		AffineTransform xform = rotateIn();
//		xform.preConcatenate(translateIn());
//		return xform;
    }

    /**
     * Method to return a transformation that moves down the hierarchy.
     * Presuming that this NodeInst is a Cell instance, the
     * transformation maps points in the Cell's coordinate space
     * into this NodeInst's parent Cell's coordinate space.
     * @param prevTransform
     * @return a transformation that moves down the hierarchy, including the previous down transformation
     */
    public AffineTransform transformIn(AffineTransform prevTransform) {
        AffineTransform transform = transformIn();
        transform.concatenate(prevTransform);
        return transform;
    }

    /**
     * Method to return a transformation that translates down the hierarchy.
     * Transform out of this node instance, translate outer coordinates to inner
     * However, it does not account for the rotation of this NodeInst...it only
     * translates from one space to another.
     * @return a transformation that translates down the hierarchy.
     */
    public AffineTransform translateIn() {
        // to transform out of this node instance, translate outer coordinates to inner
        //Cell lowerCell = (Cell)protoType;
        double dx = getAnchorCenterX();
        double dy = getAnchorCenterY();
        AffineTransform transform = new AffineTransform();
        transform.translate(-dx, -dy);
        return transform;
    }

    /**
     * Method to return a transformation that translates down the
     * hierarchy, combined with a previous transformation.
     * However, it does not account for the rotation of
     * this NodeInst...it only translates from one space to another.
     * @param prevTransform the previous transformation to the NodeInst's Cell.
     * @return a transformation that translates down the hierarchy,
     * including the previous transformation.
     */
    public AffineTransform translateIn(AffineTransform prevTransform) {
        AffineTransform transform = translateIn();
        AffineTransform returnTransform = new AffineTransform(prevTransform);
        returnTransform.concatenate(transform);
        return returnTransform;
    }

    /**
     * Method to return a transformation that translates up the hierarchy.
     * Transform out of this node instance, translate inner coordinates to outer.
     * However, it does not account for the rotation of this NodeInst...it only
     * translates from one space to another.
     * @return a transformation that translates up the hierarchy.
     */
    public AffineTransform translateOut() {
        // to transform out of this node instance, translate inner coordinates to outer
        //Cell lowerCell = (Cell)protoType;
        double dx = getAnchorCenterX();
        double dy = getAnchorCenterY();
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
    public AffineTransform translateOut(AffineTransform prevTransform) {
        AffineTransform transform = translateOut();
        AffineTransform returnTransform = new AffineTransform(prevTransform);
        returnTransform.concatenate(transform);
        return returnTransform;
    }

    /**
     * Method to return a transformation that rotates the same as this NodeInst.
     * It transforms points on this NodeInst to account for the NodeInst's rotation.
     * The rotation happens about the origin.
     * @return a transformation that rotates the same as this NodeInst.
     * If this NodeInst is not rotated, the returned transformation is identity.
     */
    public AffineTransform pureRotateOut() {
        return d.orient.pureRotate();
    }

    /**
     * Method to return a transformation that unrotates the same as this NodeInst.
     * It transforms points on this NodeInst to account for the NodeInst's rotation.
     * The rotation happens about the origin.
     * @return a transformation that unrotates the same as this NodeInst.
     * If this NodeInst is not rotated, the returned transformation is identity.
     */
    public AffineTransform pureRotateIn() {
        return d.orient.inverse().pureRotate();
    }

    /**
     * Method to return a transformation that unrotates this NodeInst.
     * It transforms points on this NodeInst that have been rotated with the node
     * so that they appear in the correct location on the unrotated node.
     * The rotation happens about the node's Anchor Point (the location of the cell-center inside of cell definitions).
     * @return a transformation that unrotates this NodeInst.
     * If this NodeInst is not rotated, the returned transformation is identity.
     */
    public AffineTransform rotateIn() {
        return d.orient.inverse().rotateAbout(getAnchorCenterX(), getAnchorCenterY());
    }

    /**
     * Method to return a transformation that unrotates this NodeInst,
     * combined with a previous transformation.
     * It transforms points on this NodeInst that have been rotated with the node
     * so that they appear in the correct location on the unrotated node.
     * The rotation happens about the node's Anchor Point (the location of the cell-center inside of cell definitions).
     * @param prevTransform the previous transformation to be applied.
     * @return a transformation that unrotates this NodeInst, combined
     * with a previous transformation.  If this NodeInst is not
     * rotated, the returned transformation is the original parameter.
     */
    public AffineTransform rotateIn(AffineTransform prevTransform) {
        // if there is no transformation, stop now
        if (d.orient == Orientation.IDENT) {
            return prevTransform;
        }

        AffineTransform transform = rotateIn();
        AffineTransform returnTransform = new AffineTransform(prevTransform);
        returnTransform.concatenate(transform);
        return returnTransform;
    }

    /**
     * Method to return a transformation that rotates this NodeInst.
     * It transforms points on this NodeInst to account for the NodeInst's rotation.
     * The rotation happens about the node's Anchor Point (the location of the cell-center inside of cell definitions).
     * @return a transformation that rotates this NodeInst.
     * If this NodeInst is not rotated, the returned transformation is identity.
     */
    public AffineTransform rotateOut() {
        return d.orient.rotateAbout(getAnchorCenterX(), getAnchorCenterY());
    }

    /**
     * Method to return a transformation that rotates this NodeInst.
     * It transforms points on this NodeInst to account for the NodeInst's rotation.
     * The rotation happens about the node's true geometric center.
     * @return a transformation that rotates this NodeInst.
     * If this NodeInst is not rotated, the returned transformation is identity.
     */
    public AffineTransform rotateOutAboutTrueCenter() {
        return d.orient.rotateAbout(getTrueCenterX(), getTrueCenterY());
    }

    /**
     * Method to return a transformation that rotates this NodeInst,
     * combined with a previous transformation.  It transforms points
     * on this NodeInst to account for the NodeInst's rotation.
     * The rotation happens about the node's Anchor Point (the location of the cell-center inside of cell definitions).
     * @param prevTransform the previous transformation to be applied.
     * @return a transformation that rotates this NodeInst, combined
     * with a previous transformation..  If this NodeInst is not
     * rotated, the returned transformation is identity.
     */
    public AffineTransform rotateOut(AffineTransform prevTransform) {
        // if there is no transformation, stop now
        if (d.orient == Orientation.IDENT) {
            return prevTransform;
        }

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
    public AffineTransform rotateOutAboutTrueCenter(AffineTransform prevTransform) {
        // if there is no transformation, stop now
        if (d.orient == Orientation.IDENT) {
            return prevTransform;
        }

        AffineTransform transform = rotateOutAboutTrueCenter();
        AffineTransform returnTransform = new AffineTransform(prevTransform);
        returnTransform.concatenate(transform);
        return returnTransform;
    }

    /**
     * Method to return a Poly that describes the location of a port
     * on this NodeInst.
     * @param thePort the port on this NodeInst.
     * @return a Poly that describes the location of the Export.
     * The Poly is transformed to account for rotation on this NodeInst.
     */
    public Poly getShapeOfPort(PortProto thePort) {
        return getShapeOfPort(thePort, null, false, -1);
    }

    /**
     * Method to return a Poly that describes the location of a port
     * on this NodeInst.
     * @param thePort the port on this NodeInst.
     * @param selectPt if not null, it requests a new location on the port,
     * away from existing arcs, and close to this point.
     * This is useful for "area" ports such as the left side of AND and OR gates.
     * @param forWiringTool this is an experimental flag that will reduce the *untransformed*
     * port width to zero if the primitive node's width is the default width. Likewise with
     * the port height. It will also take into account the arc width of the connecting arc
     * for contact cuts. These modifications are for the wiring tool.
     * @param arcWidth of connecting arc, for the wiring tool if 'forWiringTool' is true.
     * set to -1 to ignore.
     * @return a Poly that describes the location of the Export.
     * The Poly is transformed to account for rotation on this NodeInst.
     */
    public Poly getShapeOfPort(PortProto thePort, Point2D selectPt, boolean forWiringTool, double arcWidth) {
        // look down to the bottom level node/port
        PortOriginal fp = new PortOriginal(this, thePort);
        AffineTransform trans = fp.getTransformToTop();
        Cell cell = fp.getBottomCell();
//        NodeInst ni = fp.getBottomNodeInst();
        ImmutableNodeInst n = fp.getBottomImmutableNodeInst();
        PrimitivePort pp = fp.getBottomPortProto();
        PrimitiveNode np = pp.getParent();
//        Technology tech = np.getTechnology();
        Poly.Builder polyBuilder = Poly.threadLocalLambdaBuilder();
        Poly poly = polyBuilder.getShape(getCellTreeUnsafe(), n, pp, selectPt);
//        Poly poly = tech.getShapeOfPort(ni, (PrimitivePort) pp, selectPt);

        // we only compress port if it is a rectangle
        Rectangle2D box = poly.getBox();
        if (forWiringTool && (box != null)) {
            if ((arcWidth != -1) && (np.getFunction().isContact())) {
                // reduce the port size such that the connecting arc's width cannot extend
                // beyond the width of the contact
                ERectangle baseRectangle = np.getBaseRectangle();
                double width = DBMath.gridToLambda(n.size.getGridX() + baseRectangle.getGridWidth());
                double height = DBMath.gridToLambda(n.size.getGridY() + baseRectangle.getGridHeight());
//                SizeOffset so = np.getProtoSizeOffset();
//                double width = ni.getXSize() - so.getHighXOffset() - so.getLowXOffset();
//                double height = ni.getYSize() - so.getHighYOffset() - so.getLowYOffset();
                double newportwidth = width - arcWidth;
                double newportheight = height - arcWidth;
                if (newportwidth < 0) {
                    newportwidth = 0; // if arc bigger than contact, make port size 0 so it's centered
                }
                if (newportheight < 0) {
                    newportheight = 0;
                }
                double offsetX = 0, offsetY = 0;
                if (newportwidth < box.getWidth()) {
                    // port size needs to be reduced if desired width less than actual width
                    offsetX = 0.5 * (newportwidth - box.getWidth());
                    box = new Rectangle2D.Double(box.getX() - offsetX, box.getY(), box.getWidth() + 2 * offsetX, box.getHeight());
                }
                if (newportheight < box.getHeight()) {
                    // port size needs to be reduced if desired height less than actual height
                    offsetY = 0.5 * (newportheight - box.getHeight());
                    box = new Rectangle2D.Double(box.getX(), box.getY() - offsetY, box.getWidth(), box.getHeight() + 2 * offsetY);
                }
            }
            EditingPreferences ep = cell.getEditingPreferences();
            if (n.size.getGridX() >> 1 == np.getDefaultGridExtendX(ep)) {
                double x = poly.getCenterX();
                box = new Rectangle2D.Double(x, box.getMinY(), 0, box.getHeight());
            }
            if (n.size.getGridY() >> 1 == np.getDefaultGridExtendY(ep)) {
                double y = poly.getCenterY();
                box = new Rectangle2D.Double(box.getMinX(), y, box.getWidth(), 0);
            }
            poly = new Poly(box);
        }

        // transform port out to current level
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
     * @return an array of Point2D in database coordinates.
     */
    public EPoint[] getTrace() {
        return getD().getTrace();
    }

    /**
     * Method to set the "outline" information on this NodeInst.
     * Outline information is a set of coordinate points that further
     * refines the NodeInst description.  It is typically used in
     * Artwork primitives to give them a precise shape.  It is also
     * used by pure-layer nodes in all layout technologies to allow
     * them to take any shape.  It is even used by many MOS
     * transistors to allow a precise gate path to be specified.
     * @param points an array of Point2D values in database coordinates.
     * These are not relative to the center of the node, but are actual coordinates of the outline.
     */
    public void setTrace(Point2D[] points) {
        double lX = points[0].getX();
        double hX = lX;
        double lY = points[0].getY();
        double hY = lY;
        for (int i = 1; i < points.length; i++) {
            if (points[i] == null) {
                continue;
            }
            double x = points[i].getX();
            if (x < lX) {
                lX = x;
            }
            if (x > hX) {
                hX = x;
            }
            double y = points[i].getY();
            if (y < lY) {
                lY = y;
            }
            if (y > hY) {
                hY = y;
            }
        }
        double newCX = (lX + hX) / 2;
        double newCY = (lY + hY) / 2;
        double newSX = hX - lX;
        double newSY = hY - lY;
        EPoint[] newPoints = new EPoint[points.length];
        for (int i = 0; i < newPoints.length; i++) {
            if (points[i] != null) {
                newPoints[i] = new EPoint(points[i].getX() - newCX, points[i].getY() - newCY);
            }
        }

        // update the points
        newVar(NodeInst.TRACE, newPoints);

        // Force instance to have IDENT Orientation
        modifyInstance(newCX - getAnchorCenterX(), newCY - getAnchorCenterY(), newSX - getXSize(),
                newSY - getYSize(), getOrient().inverse());
    }

    /**
     * Method to tell whether the outline information on this NodeInst wraps.
     * Wrapping outline information applies to closed figures, such as pure-layer nodes.
     * Nodes that do not wrap include serpentine transistors, splines, and opened polygons.
     * @return true if this node's outline information wraps.
     */
    public boolean traceWraps() {
        if (protoType == Artwork.tech().splineNode
                || protoType == Artwork.tech().openedPolygonNode
                || protoType == Artwork.tech().openedDottedPolygonNode
                || protoType == Artwork.tech().openedDashedPolygonNode
                || protoType == Artwork.tech().openedThickerPolygonNode) {
            return false;
        }
        if (getFunction().isFET()) {
            return false;
        }
        return true;
    }

    /****************************** PORTS ******************************/
    /**
     * Method to return an Iterator for all PortInsts on this NodeInst.
     * @return an Iterator for all PortInsts on this NodeInst.
     */
    public Iterator<PortInst> getPortInsts() {
        return ArrayIterator.iterator(portInsts);
    }

    /**
     * Method to return the number of PortInsts on this NodeInst.
     * @return the number of PortInsts on this NodeInst.
     */
    public int getNumPortInsts() {
        return portInsts.length;
    }

    /**
     * Method to return the PortInst at specified position.
     * @param portIndex specified position of PortInst.
     * @return the PortProto at specified position..
     */
    public PortInst getPortInst(int portIndex) {
        return portInsts[portIndex];
    }

    /**
     * Method to return the only PortInst on this NodeInst.
     * This is quite useful for vias and pins which have only one PortInst.
     * @return the only PortInst on this NodeInst.
     * If there are more than 1 PortInst, then return null.
     */
    public PortInst getOnlyPortInst() {
        int sz = portInsts.length;
        if (sz != 1) {
            System.out.println("NodeInst.getOnlyPortInst: " + topology.cell
                    + ", " + this + " doesn't have just one port, it has " + sz);
            return null;
        }
        return portInsts[0];
    }

    /**
     * Method to return the named PortInst on this NodeInst.
     * @param name the name of the PortInst.
     * @return the selected PortInst.  If the name is not found, return null.
     */
    public PortInst findPortInst(String name) {
        PortProto pp = protoType.findPortProto(name);
        if (pp == null) {
            return null;
        }
        return portInsts[pp.getPortIndex()];
    }

    /**
     * Method to return the PortInst on this NodeInst that is closest to a point.
     * @param w the point of interest.
     * @return the closest PortInst to that point.
     */
    public PortInst findClosestPortInst(Point2D w) {
        double bestDist = Double.MAX_VALUE;
        PortInst bestPi = null;
        for (int i = 0; i < portInsts.length; i++) {
            PortInst pi = portInsts[i];
            Poly piPoly = pi.getPoly();
            Point2D piPt = new Point2D.Double(piPoly.getCenterX(), piPoly.getCenterY());
            double thisDist = piPt.distance(w);
            if (thisDist < bestDist) {
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
    public PortInst findPortInstFromProto(PortProto pp) {
        return portInsts[pp.getPortIndex()];
    }

    /**
     * Update PortInsts of this NodeInst accoding to pattern.
     * Pattern contains an element for each PortProto.
     * If PortProto was just created, the element contains -1.
     * For old PortProtos the element contains old index of the PortProto.
     * @param pattern array with elements describing new PortInsts.
     */
    public void updatePortInsts(int[] pattern) {
        assert pattern.length == protoType.getNumPorts();
        if (pattern.length == 0) {
            portInsts = NULL_PORT_INST_ARRAY;
            return;
        }
        PortInst[] newPortInsts = new PortInst[pattern.length];
        for (int i = 0; i < newPortInsts.length; i++) {
            int p = pattern[i];
            newPortInsts[i] = p >= 0 ? portInsts[p] : PortInst.newInstance(protoType.getPort(i), this);
        }
        portInsts = newPortInsts;
    }

    /**
     * Updaten PortInsts of this NodeInst according to PortProtos in prototype.
     */
    public void updatePortInsts(boolean full) {
        PortInst[] newPortInsts = new PortInst[protoType.getNumPorts()];
        for (int i = 0; i < portInsts.length; i++) {
            PortInst pi = portInsts[i];
            if (full) {
                if (pi.getNodeInst() != this) {
                    continue;
                }
                PortProto pp = pi.getPortProto();
                if (pp.getParent() != getProto()) {
                    continue;
                }
                if (pp instanceof Export && !((Export) pp).isLinked()) {
                    continue;
                }
            }
            int portIndex = pi.getPortIndex();
            if (portIndex < 0) {
                continue;
            }
            newPortInsts[portIndex] = pi;
        }
        for (int i = 0; i < newPortInsts.length; i++) {
            if (newPortInsts[i] != null) {
                continue;
            }
            newPortInsts[i] = PortInst.newInstance(protoType.getPort(i), this);
        }
        portInsts = newPortInsts;
    }

    /**
     * Method to get the Schematic Cell from a NodeInst icon
     * @return the equivalent view of the prototype, or null if none
     * (such as for primitive)
     */
    public Cell getProtoEquivalent() {
        if (!(protoType instanceof Cell)) {
            return null;            // primitive
        }
        return ((Cell) protoType).getEquivalent();
    }

    /**
     * Returns true of there are Exports on this NodeInst.
     * @return true if there are Exports on this NodeInst.
     */
    public boolean hasExports() {
        return topology != null && topology.cell.getMemoization().hasExports(getD());
    }

    /**
     * Method to return an Iterator over all Exports on this NodeInst.
     * @return an Iterator over all Exports on this NodeInst.
     */
    public Iterator<Export> getExports() {
        Iterator<Export> eit = ArrayIterator.emptyIterator();
        if (topology != null) {
            Iterator<ImmutableExport> it = topology.cell.getMemoization().getExports(getD().nodeId);
            if (it.hasNext()) {
                eit = new ExportIterator(it);
            }
        }
        return eit;
    }

    private class ExportIterator implements Iterator<Export> {

        private final Iterator<ImmutableExport> it;

        ExportIterator(Iterator<ImmutableExport> it) {
            this.it = it;
        }

        public boolean hasNext() {
            return it.hasNext();
        }

        public Export next() {
            return topology.cell.getExportChron(it.next().exportId.chronIndex);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Method to return the number of Exports on this NodeInst.
     * @return the number of Exports on this NodeInst.
     */
    public int getNumExports() {
        return topology != null ? topology.cell.getMemoization().getNumExports(getD().nodeId) : 0;
    }

    /**
     * Method to associate the ports between two NodeInsts.
     * @param ni1 the first NodeInst to associate.
     * @param ni2 the second NodeInst to associate.
     * @param ignorePortNames true to ignore port names and use only positions.
     * @return an array of PortAssociation objects that associates ports on this NodeInst
     * with those on the other one.  returns null if there is an error.
     */
    private PortAssociation[] portAssociate(NodeInst ni1, NodeInst ni2, boolean ignorePortNames)
    {
        // gather information about NodeInst 1 (ports, Poly, location, association)
        int total1 = ni1.getProto().getNumPorts();
        PortAssociation[] portInfo1 = new PortAssociation[total1];
        int k = 0;
        for (Iterator<PortInst> it1 = ni1.getPortInsts(); it1.hasNext();)
        {
            PortInst pi1 = it1.next();
            portInfo1[k] = new PortAssociation();
            portInfo1[k].portInst = pi1;
            portInfo1[k].poly = pi1.getPoly();
            portInfo1[k].pos = new Point2D.Double(portInfo1[k].poly.getCenterX(), portInfo1[k].poly.getCenterY());
            portInfo1[k].assn = null;
            k++;
        }

        // gather information about NodeInst 2 (ports, Poly, location, association)
        int total2 = ni2.getProto().getNumPorts();
        PortAssociation[] portInfo2 = new PortAssociation[total2];
        k = 0;
        for (Iterator<PortInst> it2 = ni2.getPortInsts(); it2.hasNext();)
        {
            PortInst pi2 = it2.next();
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
            for (int i1 = 0; i1 < total1; i1++)
            {
                PortInst pi1 = portInfo1[i1].portInst;
                for (int i2 = 0; i2 < total2; i2++)
                {
                    PortInst pi2 = portInfo2[i2].portInst;
                    if (portInfo2[i2].assn != null) continue;

                    // stop if the ports have different name
                    if (!pi2.getPortProto().getName().equals(pi1.getPortProto().getName()))
                        continue;

                    // store the correct association of ports
                    portInfo1[i1].assn = pi2;
                    portInfo2[i2].assn = pi1;
                }
            }
        }

        // make two passes, the first stricter
        for (int pass = 0; pass < 2; pass++)
        {
            // associate ports that are in the same position
            for (int i1 = 0; i1 < total1; i1++)
            {
                PortInst pi1 = portInfo1[i1].portInst;
                if (portInfo1[i1].assn != null) continue;

                for (int i2 = 0; i2 < total2; i2++)
                {
                    // if this port is already associated, ignore it
                    PortInst pi2 = portInfo2[i2].portInst;
                    if (portInfo2[i2].assn != null) continue;

                    // if the port centers are different, go no further
                    if (portInfo2[i2].pos.getX() != portInfo1[i1].pos.getX() ||
                        portInfo2[i2].pos.getY() != portInfo1[i1].pos.getY())
                    		continue;

                    // compare actual polygons to be sure
                    if (pass == 0)
                    {
                        if (!portInfo1[i1].poly.polySame(portInfo2[i2].poly)) continue;
                        if (!connectivityMatches(pi1.getPortProto(), pi2.getPortProto())) continue;
                    }

                    // handle confusion if multiple ports have the same polygon
                    if (portInfo1[i1].assn != null)
                    {
                        PortProto mpt = portInfo1[i1].assn.getPortProto();

                        // see if name match can fix confusion
                        if (ignorePortNames)
                        {
                            if (pi1.getPortProto().getName().equals(mpt.getName())) continue;
                            if (!pi1.getPortProto().getName().equals(pi2.getPortProto().getName())) continue;
                        }

                        // see if one of the associations has the same connectivity
                        boolean matchNew = connectivityMatches(pi1.getPortProto(), pi2.getPortProto());
                        boolean matchOld = connectivityMatches(pi1.getPortProto(), mpt);
                        if (!matchNew) continue;
                        if (matchOld) continue;
                    }

                    // store the correct association of ports
                    portInfo1[i1].assn = pi2;
                    portInfo2[i2].assn = pi1;
                }
            }
        }

        // one final pass allows many-to-one associations when ports are in the same location
        for (int i1 = 0; i1 < total1; i1++)
        {
        	if (portInfo1[i1].assn != null) continue;

        	for (int i2 = 0; i2 < total2; i2++)
            {
            	if (portInfo2[i2].assn != null) continue;

            	// stop if the ports have different location
                if (portInfo2[i2].pos.getX() != portInfo1[i1].pos.getX() ||
                    portInfo2[i2].pos.getY() != portInfo1[i1].pos.getY())
                    	continue;
                if (!connectivityMatches(portInfo1[i1].portInst.getPortProto(), portInfo2[i2].portInst.getPortProto()))
                	continue;

                // store the correct association
                portInfo1[i1].assn = portInfo2[i2].portInst;
            }
        }
        return portInfo1;
    }

    private boolean connectivityMatches(PortProto pp1, PortProto pp2)
    {
        // see if the associations have the same connectivity
        ArcProto[] i1Conn1 = pp1.getBasePort().getConnections();
        ArcProto[] i2Conn2 = pp2.getBasePort().getConnections();
        if (i1Conn1.length != i2Conn2.length) return false;
        for (int j = 0; j < i1Conn1.length; j++)
        {
            if (j >= i2Conn2.length) return false;
            if (i1Conn1[j] != i2Conn2[j])return false;
        }
        return true;
    }

    /****************************** CONNECTIONS ******************************/
    /**
     * Method to determine whether the display of this pin NodeInst should be supressed.
     * In Schematics technologies, pins are not displayed if there are 1 or 2 connections,
     * but are shown for 0 or 3 or more connections (called "Steiner points").
     * @return true if this pin NodeInst should be supressed.
     */
    public boolean pinUseCount() {
        return topology != null && topology.cell.getMemoization().pinUseCount(getD());
    }

    /**
     * Method to tell whether this NodeInst is a pin that is "inline".
     * An inline pin is one that connects in the middle between two arcs.
     * The arcs must line up such that the pin can be removed and the arcs replaced with a single
     * arc that is in the same place as the former two.
     * @return true if this NodeInst is an inline pin.
     */
    public boolean isInlinePin() {
        if (!protoType.getFunction().isPin()) {
            return false;
        }

        // see if the pin is connected to two arcs along the same slope
        int j = 0;
        ArcInst[] reconAr = new ArcInst[2];
        Point2D[] delta = new Point2D.Double[2];
        for (Iterator<Connection> it = getConnections(); it.hasNext();) {
            Connection con = it.next();
            if (j >= 2) {
                j = 0;
                break;
            }
            ArcInst ai = con.getArc();
            reconAr[j] = ai;
            EPoint thisLocation = con.getLocation();
            EPoint thatLocation = ai.getLocation(1 - con.getEndIndex());
//			Connection thisCon = ai.getHead();
//			Connection thatCon = ai.getTail();
//			if (thatCon == con)
//			{
//				thisCon = ai.getTail();
//				thatCon = ai.getHead();
//			}
            delta[j] = new Point2D.Double(thatLocation.getX() - thisLocation.getX(),
                    thatLocation.getY() - thisLocation.getY());
            j++;
        }
        if (j != 2) {
            return false;
        }

        // must connect to two arcs of the same type and width
        if (reconAr[0].getProto() != reconAr[1].getProto()) {
            return false;
        }
        if (reconAr[0].getLambdaBaseWidth() != reconAr[1].getLambdaBaseWidth()) {
            return false;
        }
//		if (reconAr[0].getLambdaFullWidth() != reconAr[1].getLambdaFullWidth()) return false;

        // arcs must be along the same angle, and not be curved
        if (delta[0].getX() != 0 || delta[0].getY() != 0 || delta[1].getX() != 0 || delta[1].getY() != 0) {
            Point2D zero = new Point2D.Double(0, 0);
            if ((delta[0].getX() != 0 || delta[0].getY() != 0) && (delta[1].getX() != 0 || delta[1].getY() != 0)
                    && DBMath.figureAngle(zero, delta[0])
                    != DBMath.figureAngle(delta[1], zero)) {
                return false;
            }
        }
        if (reconAr[0].getVar(ImmutableArcInst.ARC_RADIUS) != null) {
            return false;
        }
        if (reconAr[1].getVar(ImmutableArcInst.ARC_RADIUS) != null) {
            return false;
        }

        // the arcs must not have network names on them
        Name name0 = reconAr[0].getNameKey();
        Name name1 = reconAr[1].getNameKey();
        if (name0 != null && name1 != null) {
            if (!name0.isTempname() && !name1.isTempname()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Method to tell whether this NodeInst can connect to a given ArcProto.
     * @param arc the type of arc to test for.
     * @return the first port that can connect to this node, or
     * null, if no such port on this node exists.
     */
    public PortProto connectsTo(ArcProto arc) {
        for (int i = 0, numPorts = protoType.getNumPorts(); i < numPorts; i++) {
            PortProto pp = protoType.getPort(i);
            if (pp.connectsTo(arc)) {
                return pp;
            }
        }
        return null;
    }

    /**
     * Method to return an Iterator over all Connections on this NodeInst.
     * @return an Iterator over all Connections on this NodeInst.
     */
    public Iterator<Connection> getConnections() {
        if (topology == null) {
            Iterator<Connection> cit = ArrayIterator.emptyIterator();
            return cit;
        }
        return new ConnectionIterator(topology.cell.getMemoization(), getD());
    }

    /**
     * Method to return an Iterator over Connections on this NodeInst since portIndex.
     * @param portId portId to start iterator from/
     * @return an Iterator over Connections on this NodeInst since portIndex.
     */
    Iterator<Connection> getConnections(PortProtoId portId) {
        if (topology == null) {
            Iterator<Connection> cit = ArrayIterator.emptyIterator();
            return cit;
        }
        return new ConnectionIterator(topology.cell.getMemoization(), getD(), portId);
    }

    /**
     * Returns true of there are Connections on this NodeInst.
     * @return true if there are Connections on this NodeInst.
     */
    public boolean hasConnections() {
        return topology != null && topology.cell.getMemoization().hasConnections(getD(), null);
    }

    /**
     * Method to return the number of Connections on this NodeInst.
     * @return the number of Connections on this NodeInst.
     */
    public int getNumConnections() {
        return topology != null ? topology.cell.getMemoization().getNumConnections(getD()) : 0;
    }

    private class ConnectionIterator implements Iterator<Connection> {

        private final List<ImmutableArcInst> arcs;
        private final BitSet headEnds = new BitSet();
        int i;
        Connection nextConn;

        ConnectionIterator(CellBackup.Memoization m, ImmutableNodeInst d) {
            arcs = m.getConnections(headEnds, d, null);
            findNext();
        }

        ConnectionIterator(CellBackup.Memoization m, ImmutableNodeInst d, PortProtoId portId) {
            arcs = m.getConnections(headEnds, d, portId);
            findNext();
        }

        public boolean hasNext() {
            return nextConn != null;
        }

        public Connection next() {
            if (nextConn == null) {
                throw new NoSuchElementException();
            }
            Connection con = nextConn;
            findNext();
            return con;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void findNext() {
            for (; i < arcs.size(); i++) {
                ArcInst ai = topology.cell.getArcById(arcs.get(i).arcId);
                if (ai != null) {
                    nextConn = headEnds.get(i) ? ai.getHead() : ai.getTail();
                    i++;
                    return;
                }
            }
            nextConn = null;
        }
    }

    /**
     * Method to tell whether this NodeInst is connected directly to another
     * Geometric object (that is, an arcinst connected to a nodeinst).
     * The method returns true if they are connected.
     * @param geom other Geometric object.
     * @return true if this and other Geometric objects are connected.
     */
    @Override
    public boolean isConnected(Geometric geom) {
        return geom instanceof ArcInst && ((ArcInst) geom).isConnected(this);
    }

    /****************************** TEXT ******************************/
    /**
     * Method to return the name of this NodeInst.
     * @return the name of this NodeInst.
     */
    public String getName() {
        return d.name.toString();
    }

    /**
     * Retruns true if this NodeInst was named by user.
     * @return true if this NodeInst was named by user.
     */
    public boolean isUsernamed() {
        return d.isUsernamed();
    }

    /**
     * Method to return the name key of this NodeInst.
     * @return the name key of this NodeInst, null if there is no name.
     */
    public Name getNameKey() {
        return d.name;
    }

    /**
     * Method to rename this NodeInst.
     * This NodeInst must be linked to database.
     * @param name new name of this NodeInst.
     * @return true on error
     */
    public boolean setName(String name) {
        assert isLinked();
        Name key = null;
        Cell parent = topology.cell;
        if (name != null && name.length() > 0) {
            if (name.equals(getName())) {
                return false;
            }
            if (parent.findNode(name) != null) {
                System.out.println(parent + " already has NodeInst with name \"" + name + "\"");
                return true;
            }
            key = Name.findName(name);
        } else {
            if (!isUsernamed()) {
                return false;
            }
            key = topology.getNodeAutoname(getBasename());
        }
        if (checkNameKey(key, parent) || key.isBus() && (!(protoType instanceof Cell) || !((Cell) protoType).isIcon())) {
            return true;
        }

        ImmutableNodeInst oldD = d;
        lowLevelModify(d.withName(key));
        Constraints.getCurrent().modifyNodeInst(this, oldD);
        return false;
    }

    /**
     * Method to check the new name key of a NodeInst.
     * @param name new name key of this NodeInst.
     * @param parent parent Cell used for error message
     * @return true on error.
     */
    public static boolean checkNameKey(Name name, Cell parent) {
        String extrMsg = (parent != null) ? parent.toString() : "";
        if (!name.isValid()) {
            System.out.println(extrMsg + ": Invalid name \"" + name + "\" wasn't assigned to node" + " :" + Name.checkName(name.toString()));
            return true;
        }
        if (name.isBus()) {
            if (name.isTempname()) {
                System.out.println(extrMsg + ": Temporary name \"" + name + "\" can't be bus");
                return true;
            }
            if (!parent.busNamesAllowed()) {
                System.out.println(extrMsg + ": Bus name \"" + name + "\" can be in icons and schematics only");
                return true;
            }
        }
        if (name.hasEmptySubnames()) {
            if (name.isBus()) {
                System.out.println(extrMsg + ": Name \"" + name + "\" with empty subnames wasn't assigned to node");
            } else {
                System.out.println(extrMsg + ": Cannot assign empty name \"" + name + "\" to node");
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the TextDescriptor on this NodeInst selected by variable key.
     * This key may be a key of variable on this NodeInst or one of the
     * special keys:
     * <code>NodeInst.NODE_NAME</code>
     * <code>NodeInst.NODE_PROTO</code>
     * The TextDescriptor gives information for displaying the Variable.
     * @param varKey key of variable or special key.
     * @return the TextDescriptor on this ElectricObject.
     */
    public TextDescriptor getTextDescriptor(Variable.Key varKey) {
        if (varKey == NODE_NAME) {
            return d.nameDescriptor;
        }
        if (varKey == NODE_PROTO) {
            return d.protoDescriptor;
        }
        return super.getTextDescriptor(varKey);
    }

    /**
     * Updates the TextDescriptor on this NodeInst selected by varKey.
     * The varKey may be a key of variable on this NodeInst or one of the
     * special keys:
     * NodeInst.NODE_NAME
     * NodeInst.NODE_PROTO
     * If varKey doesn't select any text descriptor, no action is performed.
     * The TextDescriptor gives information for displaying the Variable.
     * @param varKey key of variable or special key.
     * @param td new value TextDescriptor
     */
    public void setTextDescriptor(Variable.Key varKey, TextDescriptor td) {
        if (varKey == NODE_NAME) {
            setD(d.withNameDescriptor(td), true);
            return;
        }
        if (varKey == NODE_PROTO) {
            setD(d.withProtoDescriptor(td), true);
            return;
        }
        super.setTextDescriptor(varKey, td);
    }

    /**
     * Method to determine whether a variable key on NodeInst is deprecated.
     * Deprecated variable keys are those that were used in old versions of Electric,
     * but are no longer valid.
     * @param key the key of the variable.
     * @return true if the variable key is deprecated.
     */
    public boolean isDeprecatedVariable(Variable.Key key) {
        if (key == NODE_NAME || key == NODE_PROTO) {
            return true;
        }
        return super.isDeprecatedVariable(key);
    }

    /**
     * Method to handle special case side-effects of setting variables on this NodeInst.
     * Overrides the general method on ElectricObject.
     * Currently it handles changes to the number-of-degrees on a circle node.
     * @param key the Variable key that has changed on this NodeInst.
     */
    public void checkPossibleVariableEffects(Variable.Key key) {
        if (key == TRACE && protoType instanceof PrimitiveNode) {
            PrimitiveNode pn = (PrimitiveNode) protoType;
            if (ImmutableNodeInst.SIMPLE_TRACE_SIZE) {
                lowLevelModify(d);
            } else if (pn.isHoldsOutline() && getTrace() != null) {
                Poly[] polys = pn.getTechnology().getShapeOfNode(this);
                Rectangle2D bounds = new Rectangle2D.Double();
                for (int i = 0; i < polys.length; i++) {
                    Poly poly = polys[i];
                    if (i == 0) {
                        bounds.setRect(poly.getBounds2D());
                    } else {
                        Rectangle2D.union(poly.getBounds2D(), bounds, bounds);
                    }
                }
                ERectangle full = pn.getFullRectangle();
                double lambdaX = bounds.getWidth() - full.getLambdaWidth();
                double lambdaY = bounds.getHeight() - full.getLambdaHeight();
                lowLevelModify(d.withSize(EPoint.fromLambda(lambdaX, lambdaY)));
            }
        } else if (key == Artwork.ART_DEGREES) {
            lowLevelModify(d);
        }
    }

    /**
     * Method to determine whether this is an Invisible Pin with text.
     * If so, it should not be selected, but its text should be instead.
     * @return true if this is an Invisible Pin with text.
     */
    public boolean isInvisiblePinWithText() {
        if (getProto() != Generic.tech().invisiblePinNode) {
            return false;
        }
        if (hasExports()) {
            return true;
        }
//		if (getNumExports() != 0) return true;
        if (numDisplayableVariables(false) != 0) {
            return true;
        }
        return false;
    }

    /**
     * Method to tell if this NodeInst is an invisible-pin with text that is offset away from the pin center.
     * Since invisible pins with text are never shown, their text should not be offset.
     * @param repair true to fix such text by changing its offset to (0,0).
     * If this is selected, the change is made directly (so this must be called from
     * inside of a job).
     * @return the coordinates of the pin, if it has offset text.
     * Returns null if the pin is valid (or if it isn't a pin or doesn't have text).
     */
    public Point2D invisiblePinWithOffsetText(boolean repair) {
        // look for pins that are invisible and have text in different location
        if (!protoType.getFunction().isPin()) {
            return null;
        }
        if (hasConnections()) {
            return null;
        }
//		if (this.getNumConnections() != 0) return null;

        // stop now if this isn't invisible
        if (protoType != Generic.tech().invisiblePinNode) {
            Technology tech = protoType.getTechnology();
            Poly[] polyList = tech.getShapeOfNode(this);
            if (polyList.length > 0) {
                Poly.Type style = polyList[0].getStyle();
                if (!style.isText()) {
                    return null;
                }
            }
        }

        // invisible: look for offset text
        for (Iterator<Export> it = getExports(); it.hasNext();) {
            Export pp = it.next();
            TextDescriptor td = pp.getTextDescriptor(Export.EXPORT_NAME);
            if (td.getXOff() != 0 || td.getYOff() != 0) {
                Point2D retVal = new Point2D.Double(getAnchorCenterX() + td.getXOff(), getAnchorCenterY() + td.getYOff());
                if (repair) {
                    pp.setOff(Export.EXPORT_NAME, 0, 0);
                }
                return retVal;
            }
        }

        for (Iterator<Variable> it = getVariables(); it.hasNext();) {
            Variable var = it.next();
            if (var.isDisplay() && (var.getXOff() != 0 || var.getYOff() != 0)) {
                Point2D retVal = new Point2D.Double(getAnchorCenterX() + var.getXOff(), getAnchorCenterY() + var.getYOff());
                if (repair) {
                    setOff(var.getKey(), 0, 0);
                }
//				if (repair) var.setOff(0, 0);
                return retVal;
            }
        }
        return null;
    }

    /**
     * Method to return a string that describes any technology-specific additions
     * to this NodeInst's prototype name.
     * @return an extra description of this NodeInst's prototype (may be an empty string).
     */
    public String getTechSpecificAddition() {
        if (protoType instanceof PrimitiveNode) {
            PrimitiveNode pNp = (PrimitiveNode) protoType;
            if (pNp.isTechSpecific()) {
                String description = protoType.describe(false);
                PrimitiveNode.Function fun = getFunction();
                String funName = fun.getName();
                String funNameLC = funName.toLowerCase();
                String descLC = description.toLowerCase();
                if (!descLC.equals(funNameLC)) {
                    if (funNameLC.startsWith(descLC)) {
                        funName = funName.substring(description.length());
                        if (funName.startsWith("-")) {
                            funName = funName.substring(1);
                        }
                    }
                    if (funNameLC.endsWith(descLC)) {
                        funName = funName.substring(0, funName.length() - description.length());
                        if (funName.endsWith("-")) {
                            funName = funName.substring(0, funName.length() - 1);
                        }
                    }
                    return funName;
                }
            }
        }
        return "";
    }

    /**
     * Method to describe this NodeInst as a string.
     * @param withQuotes to wrap description between quotes
     * @return a description of this NodeInst as a string.
     */
    public String describe(boolean withQuotes) {
        String description = protoType.describe(false);
        String extra = getTechSpecificAddition();
        if (extra.length() > 0) {
            description += "(" + extra + ")";
        }
        String name = (withQuotes) ? "'" + getName() + "'" : getName();
        if (name != null) {
            description += "[" + name + "]";
        }
        return description;
    }

    /**
     * Compares NodeInsts by their Cells and names.
     * @param that the other NodeInst.
     * @return a comparison between the NodeInsts.
     */
    public int compareTo(NodeInst that) {
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
        return this.d.nodeId - that.d.nodeId;
    }

    /**
     * Returns a printable version of this NodeInst.
     * @return a printable version of this NodeInst.
     */
    public String toString() {
        if (protoType == null) {
            return "NodeInst no protoType";
        }
//		return "NodeInst '" + protoType.getName() + "'";
        return "node " + describe(true);
    }

    /****************************** MISCELLANEOUS ******************************/
    /**
     * Method to return the prototype of this NodeInst.
     * @return the prototype of this NodeInst.
     */
    public NodeProto getProto() {
        return protoType;
    }

    /**
     * Method to tell whether this NodeInst is a cell instance.
     * @return true if this NodeInst is a cell instance, false if it is a primitive
     */
    public boolean isCellInstance() {
        return protoType instanceof Cell;
    }

    /**
     * Get Nodable by array index.
     * @param arrayIndex the index of the desired Nodable.
     * @return the desired Nodable.
     */
    public Nodable getNodable(int arrayIndex) {
        if (arrayIndex != 0) {
            throw new IndexOutOfBoundsException();
        }
        return this;
    }

    // JKG: trying this out
    /**
     * Implements Nodable.contains(NodeInst ni).
     * True if ni is the same as this.  False otherwise
     */
    public boolean contains(NodeInst ni, int arrayIndex) {
        if (ni == this && arrayIndex == 0) {
            return true;
        }
        return false;
    }

    /**
     * Get the NodeInst associated with this Nodable
     * For NodeInsts, returns this.
     * @return the NodeInst associate with this Nodable
     */
    public NodeInst getNodeInst() {
        return this;
    }

    /**
     * Get array index of this Nodable
     * For NodeInst, return 0
     * @return the array index of this Nodable
     */
    public int getNodableArrayIndex() {
        return 0;
    }

    /**
     * Method to return the function of this NodeProto.
     * The Function is a technology-independent description of the behavior of this NodeProto.
     * @return function the function of this NodeProto.
     */
    public PrimitiveNode.Function getFunction() {
        if (protoType instanceof Cell) {
            return PrimitiveNode.Function.UNKNOWN;
        }

        PrimitiveNode np = (PrimitiveNode) protoType;
        return np.getTechnology().getPrimitiveFunction(np, getTechSpecific());
    }

    /**
     * Method to see if this NodeInst is a Primitive Transistor.
     * Use getFunction() to determine what specific transitor type it is, if any.
     * @return true if NodeInst represents Primitive Transistor
     */
    public boolean isPrimitiveTransistor() {
        PrimitiveNode.Function func = protoType.getFunction(); // note bypasses ni.getFunction() call
        return func.isTransistor();
    }

    /**
     * Method to return true if a PrimitiveNode is acting as implant.
     * This is used for coverage implant function.
     */
    public boolean isPrimtiveSubstrateNode() {
        if (getFunction() != PrimitiveNode.Function.NODE) {
            return false;
        }
        PrimitiveNode np = (PrimitiveNode) protoType;
        if (np.getNodeLayers().length != 1) {
            return false;
        }
        return (np.getNodeLayers()[0].getLayer().getFunction().isSubstrate());
    }

    /**
     * Method to see if this NodeInst is a Serpentine Transistor.
     * @return true if NodeInst is a serpentine transistor.
     */
    public boolean isSerpentineTransistor() {
        if (!isPrimitiveTransistor()) {
            return false;
        }
        PrimitiveNode pn = (PrimitiveNode) getProto();
        if (pn.isHoldsOutline() && (getTrace() != null)) {
            return true;
        }
        return false;
    }

//	/**
//	 * Method to tell whether this NodeInst is a field-effect transtor.
//	 * This includes the nMOS, PMOS, and DMOS transistors, as well as the DMES and EMES transistors.
//	 * @return true if this NodeInst is a field-effect transtor.
//	 */
//	public boolean isFET()
//	{
//		PrimitiveNode.Function fun = getFunction();
//		return fun.isFET();
//	}
//	/**
//	 * Method to tell whether this NodeInst is a bipolar transistor.
//	 * This includes NPN and PNP transistors.
//	 * @return true if this NodeInst is a bipolar transtor.
//	 */
//	public boolean isBipolar()
//	{
//		PrimitiveNode.Function fun = getFunction();
//		return fun.isBipolar();
//	}
    /**
     * Method to return the size of this NodeInst in terms of width and
     * height/length depending on the type of primitive.
     * @param context the VarContext in which any evaluations take place,
     * pass in VarContext.globalContext if no context needed.
     * @return the size of the NodeInst
     */
    public PrimitiveNodeSize getNodeInstSize(VarContext context) {
        PrimitiveNodeSize size = getPrimitiveDependentNodeSize(context);
        if (size == null) // not a special primitive like transistor or resistor
        {
            double x = getLambdaBaseXSize();
            double y = getLambdaBaseYSize();
            size = new PrimitiveNodeSize(new Double(x), new Double(y), true);
        }
        return size;
    }

    /**
     * Method to return the size of this PrimitiveNode-dependend NodeInst
     * like transistors and resistors.
     * @param context the VarContext in which any evaluations take place,
     * pass in VarContext.globalContext if no context needed.
     * @return the size of the NodeInst if it is a PrimitiveNode
     */
    public PrimitiveNodeSize getPrimitiveDependentNodeSize(VarContext context) {
        PrimitiveNodeSize size = getTransistorSize(context);
        if (size == null) // Not a transistor
        {
            size = getResistorSize(context); // It is a resistor
        }
        return size;
    }

    /**
     * Method to return the size of this PrimitiveNode-type NodeInst.
     * @param context the VarContext in which any evaluations take place,
     * pass in VarContext.globalContext if no context needed.
     * @return the size of the NodeInst if it is a PrimitiveNode
     */
    private PrimitiveNodeSize getResistorSize(VarContext context) {
        if (!getFunction().isResistor()) {
            return null;
        }
        PrimitiveNode np = (PrimitiveNode) protoType;
        return np.getTechnology().getResistorSize(this, context);
    }

    /**
     * Method to return the size of this transistor NodeInst.
     * @param context the VarContext in which any evaluations take place,
     * pass in VarContext.globalContext if no context needed.
     * @return the size of the NodeInst if it is a transistor
     */
    public TransistorSize getTransistorSize(VarContext context) {
        if (!isPrimitiveTransistor() /* || !isFET() */) {
            return null;
        }
        PrimitiveNode np = (PrimitiveNode) protoType;
        return np.getTechnology().getTransistorSize(this, context);
    }

    /**
     * Method to set the size of this transistor or resistor NodeInst. Does
     * nothing if this is not a transistor NodeInst.
     * @param width the new width of the transistor
     * @param length the new length of the transistor
     */
    public void setPrimitiveNodeSize(double width, double length) {
        if (!isPrimitiveTransistor() && !getFunction().isFET() && !getFunction().isResistor()) {
            return;
        }
        PrimitiveNode np = (PrimitiveNode) protoType;
        checkChanging();
        np.getTechnology().setPrimitiveNodeSize(this, width, length);
    }

    /**
     * Method to set the size of a transistor or resistor NodeInst in this technology.
     * Width may be the area for non-FET transistors, in which case length is ignored.
     * This does nothing if the NodeInst's technology is not Schematics.
     * @param width the new width
     * @param length the new length
     */
    public void setPrimitiveNodeSize(Object width, Object length) {
        Technology tech = protoType.getTechnology();
        if (tech != Schematics.tech()) {
            return;
        }
        checkChanging();
        Schematics.tech().setPrimitiveNodeSize(this, width, length);
    }

    /**
     * Method to return the length of this serpentine transistor.
     * @return the transistor's length
     * Returns -1 if this is not a serpentine transistor, or if the length cannot be found.
     */
    public double getSerpentineTransistorLength() {
        return getD().getSerpentineTransistorLength();
    }

    /**
     * Method to store a length value on this serpentine transistor.
     * @param length the new length of the transistor.
     */
    public void setSerpentineTransistorLength(double length) {
        updateVar(TRANSISTOR_LENGTH_KEY, new Double(length));
    }

    /**
     * Method to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.
     * @return a PortInst for the gate of the transistor
     */
    public PortInst getTransistorGatePort() {
        PrimitiveNode np = (PrimitiveNode) protoType;
        return np.getTechnology().getTransistorGatePort(this);
    }

    /**
     * Method to return the alternate gate PortInst for this transistor NodeInst.
     * Only useful for layout transistors that have two gate ports.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.
     * @return a PortInst for the alternate gate of the transistor
     */
    public PortInst getTransistorAltGatePort() {
        PrimitiveNode np = (PrimitiveNode) protoType;
        return np.getTechnology().getTransistorAltGatePort(this);
    }

    /**
     * Method to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.
     * @return a PortInst for the gate of the transistor
     */
    public PortInst getTransistorSourcePort() {
        PrimitiveNode np = (PrimitiveNode) protoType;
        return np.getTechnology().getTransistorSourcePort(this);
    }

    /**
     * Method to return the emitter port of this transistor.
     * @return the PortInst of the emitter (presuming that this node is that kind of transistor).
     */
    public PortInst getTransistorEmitterPort() {
        PrimitiveNode np = (PrimitiveNode) protoType;
        return np.getTechnology().getTransistorEmitterPort(this);
    }

    /**
     * Method to return the base port of this transistor.
     * @return the PortInst of the base (presuming that this node is that kind of transistor).
     */
    public PortInst getTransistorBasePort() {
        PrimitiveNode np = (PrimitiveNode) protoType;
        return np.getTechnology().getTransistorBasePort(this);
    }

    /**
     * Method to return the collector port of this transistor.
     * @return the PortInst of the collector (presuming that this node is that kind of transistor).
     */
    public PortInst getTransistorCollectorPort() {
        PrimitiveNode np = (PrimitiveNode) protoType;
        return np.getTechnology().getTransistorCollectorPort(this);
    }

    /**
     * Method to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.
     * @return a PortInst for the gate of the transistor
     */
    public PortInst getTransistorBiasPort() {
        PrimitiveNode np = (PrimitiveNode) protoType;
        return np.getTechnology().getTransistorBiasPort(this);
    }

    /**
     * Method to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.
     * @return a PortInst for the gate of the transistor
     */
    public PortInst getTransistorDrainPort() {
        PrimitiveNode np = (PrimitiveNode) protoType;
        return np.getTechnology().getTransistorDrainPort(this);
    }

    /**
     * Method to check and repair data structure errors in this NodeInst.
     */
    public int checkAndRepair(boolean repair, List<Geometric> list, ErrorLogger errorLogger) {
        int errorCount = 0;
//		int warningCount = 0;
        double width = getXSize();
        double height = getYSize();
        Cell parent = topology != null ? topology.cell : null;
        if (protoType instanceof Cell) {
            Variable var = getVar(NccCellAnnotations.NCC_ANNOTATION_KEY);
            if (var != null) {
                // cleanup NCC cell annotations which were inheritable
                String nccMsg = "Removed extraneous NCC annotations from cell instance " + describe(false) + " in " + topology.cell;
                if (repair) {
                    delVar(var.getKey());
                    nccMsg += " (REPAIRED)";
                }
                System.out.println(nccMsg);
                if (errorLogger != null) {
                    errorLogger.logWarning(nccMsg, this, parent, null, 1);
                }
            }
            return errorCount;
        }
        PrimitiveNode pn = (PrimitiveNode) protoType;
        if (pn.getTechnology().cleanUnusedNodesInLibrary(this, list)) {
            if (errorLogger != null) {
                String msg = "Prototype of node " + getName() + " is unused";
                if (repair) {
                    // Can't put this node into logger because it will be deleted.
                    Poly poly = new Poly(getBounds());
                    errorLogger.logError(msg, poly, parent, 1);
                } else {
                    errorLogger.logError(msg, this, parent, null, 1);

                }
            }
            if (list != null) // doesn't do anything when checkAndRepair is called during reading
            {
                if (repair) {
                    list.add(this);
                }
                // This counts as 1 error, ignoring other errors
                return 1;
            }
        }
        String sizeMsg = null;
        if (getTrace() != null) {
            if (pn.isHoldsOutline()) {
                Rectangle2D bounds = new Rectangle2D.Double();
                if (!ImmutableNodeInst.SIMPLE_TRACE_SIZE) {
                    Poly[] polys = pn.getTechnology().getShapeOfNode(this);
                    for (int i = 0; i < polys.length; i++) {
                        Poly poly = polys[i];
                        if (i == 0) {
                            bounds.setRect(poly.getBounds2D());
                        } else {
                            Rectangle2D.union(poly.getBounds2D(), bounds, bounds);
                        }
                    }
                    width = DBMath.round(bounds.getWidth());
                    height = DBMath.round(bounds.getHeight());
                    if (width != getXSize() || height != getYSize()) {
                        sizeMsg = " but has outline of size ";
                    }
                }
            } else {
                String msg = parent + ", " + this + " has unexpected outline";
                System.out.println(msg);
                if (errorLogger != null) {
                    errorLogger.logError(msg, this, parent, null, 1);
                }
                if (repair) {
                    delVar(TRACE);
                }
            }
        }
        if (sizeMsg != null) {
            assert !ImmutableNodeInst.SIMPLE_TRACE_SIZE;
            sizeMsg = parent + ", " + this
                    + " is " + getXSize() + "x" + getYSize() + sizeMsg + width + "x" + height;
            if (repair) {
                checkChanging();
                sizeMsg += " (REPAIRED)";
            }
            System.out.println(sizeMsg);
            if (errorLogger != null) {
                errorLogger.logWarning(sizeMsg, this, parent, null, 1);
            }
            if (repair) {
                ERectangle full = pn.getFullRectangle();
                double lambdaX = width - full.getLambdaWidth();
                double lambdaY = height - full.getLambdaHeight();
                lowLevelModify(d.withSize(EPoint.fromLambda(lambdaX, lambdaY)));
            }
//			warningCount++;
        }
        return errorCount;
    }

    /**
     * Method to check invariants in this NodeInst.
     * @exception AssertionError if invariants are not valid
     */
    public void check() {
        assert isLinked();
        super.check();

        assert getClass() == (isCellInstance() && ((Cell) getProto()).isIcon() ? IconNodeInst.class : NodeInst.class);
        assert portInsts.length == protoType.getNumPorts();
        for (int i = 0; i < portInsts.length; i++) {
            PortInst pi = portInsts[i];
            assert pi.getNodeInst() == this;
            PortProto pp = pi.getPortProto();
            assert pp == protoType.getPort(i);
        }

        if (validVisBounds && Job.getDebug()) {
            Rectangle2D.Double chkBounds = new Rectangle2D.Double();
            // handle cell bounds
            if (d.protoId instanceof CellId) {
                // offset by distance from cell-center to the true center
                Cell subCell = (Cell) getProto();
                Rectangle2D bounds = subCell.getBounds();
                d.orient.rectangleBounds(bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY(), d.anchor.getX(), d.anchor.getY(), chkBounds);
            } else {
                BoundsBuilder b = new BoundsBuilder(getCellBackupUnsafe());
                d.computeBounds(b, chkBounds);
            }
            assert chkBounds.equals(visBounds);
        }

    }

    /**
     * Returns the basename for autonaming.
     * @return the basename for autonaming.
     */
    public Name getBasename() {
        return protoType instanceof Cell ? ((Cell) protoType).getBasename() : getFunction().getBasename();
    }

    /**
     * Method to copy the various state bits from another NodeInst to this NodeInst.
     * Includes all of the state bits stored in the flags word.
     * @param ni the other NodeInst to copy.
     */
    public void copyStateBits(NodeInst ni) {
        setD(d.withStateBits(ni.d), true);
    }

    private void setFlag(ImmutableNodeInst.Flag flag, boolean value) {
        setD(d.withFlag(flag, value), true);
    }

    /**
     * Method to copy the various state bits from another NodeInst to this NodeInst.
     * Includes all of the state bits stored in the flags word and also the "expanded" state.
     * @param ni the other NodeInst to copy.
     */
    public void copyStateBitsAndExpandedFlag(NodeInst ni) {
        copyStateBits(ni);
        setExpanded(ni.isExpanded());
    }

    /**
     * Method to set this NodeInst to be expanded.
     * Expanded NodeInsts are instances of Cells that show their contents.
     * Unexpanded Cell instances are shown as boxes with the node prototype names in them.
     * The state has no meaning for instances of primitive node prototypes.
     * @param value true if NodeInst is expanded.
     */
    public void setExpanded(boolean value) {
        if (topology != null) {
            topology.cell.setExpanded(getD().nodeId, value);
        }
    }

    /**
     * Method to tell whether this NodeInst is expanded.
     * Expanded NodeInsts are instances of Cells that show their contents.
     * Unexpanded Cell instances are shown as boxes with the node prototype names in them.
     * The state has no meaning for instances of primitive node prototypes.
     * @return true if this NodeInst is expanded.
     */
    public boolean isExpanded() {
        return topology != null && topology.cell.isExpanded(getD().nodeId);
    }

    /**
     * Method to tell whether this NodeInst is wiped.
     * Wiped NodeInsts are erased.  Typically, pin NodeInsts can be wiped.
     * This means that when an arc connects to the pin, it is no longer drawn.
     * In order for a NodeInst to be wiped, its prototype must have the "setArcsWipe" state,
     * and the arcs connected to it must have "setWipable" in their prototype.
     * @return true if this NodeInst is wiped.
     */
    public boolean isWiped() {
        if (topology == null || !(protoType instanceof PrimitiveNode && ((PrimitiveNode) protoType).isArcsWipe())) {
            return false;
        }
        return topology.cell.getMemoization().isWiped(getD());
    }

    /**
     * Method to set this NodeInst to be hard-to-select.
     * Hard-to-select NodeInsts cannot be selected by clicking on them.
     * Instead, the "special select" command must be given.
     */
    public void setHardSelect() {
        setFlag(ImmutableNodeInst.HARD_SELECT, true);
    }

    /**
     * Method to set this NodeInst to be easy-to-select.
     * Hard-to-select NodeInsts cannot be selected by clicking on them.
     * Instead, the "special select" command must be given.
     */
    public void clearHardSelect() {
        setFlag(ImmutableNodeInst.HARD_SELECT, false);
    }

    /**
     * Method to tell whether this NodeInst is hard-to-select.
     * Hard-to-select NodeInsts cannot be selected by clicking on them.
     * Instead, the "special select" command must be given.
     * @return true if this NodeInst is hard-to-select.
     */
    public boolean isHardSelect() {
        return d.is(ImmutableNodeInst.HARD_SELECT);
    }

    /**
     * Method to set this NodeInst to be visible-inside.
     * A NodeInst that is "visible inside" is only drawn when viewing inside of the Cell.
     * It is not visible from outside (meaning from higher-up the hierarchy).
     */
    public void setVisInside() {
        setFlag(ImmutableNodeInst.VIS_INSIDE, true);
    }

    /**
     * Method to set this NodeInst to be not visible-inside.
     * A NodeInst that is "visible inside" is only drawn when viewing inside of the Cell.
     * It is not visible from outside (meaning from higher-up the hierarchy).
     */
    public void clearVisInside() {
        setFlag(ImmutableNodeInst.VIS_INSIDE, false);
    }

    /**
     * Method to tell whether this NodeInst is visible-inside.
     * A NodeInst that is "visible inside" is only drawn when viewing inside of the Cell.
     * It is not visible from outside (meaning from higher-up the hierarchy).
     * @return true if this NodeInst is visible-inside.
     */
    public boolean isVisInside() {
        return d.is(ImmutableNodeInst.VIS_INSIDE);
    }

    /**
     * Method to set this NodeInst to be locked.
     * Locked NodeInsts cannot be modified or deleted.
     */
    public void setLocked() {
        setFlag(ImmutableNodeInst.LOCKED, true);
    }

    /**
     * Method to set this NodeInst to be unlocked.
     * Locked NodeInsts cannot be modified or deleted.
     */
    public void clearLocked() {
        setFlag(ImmutableNodeInst.LOCKED, false);
    }

    /**
     * Method to tell whether this NodeInst is locked.
     * Locked NodeInsts cannot be modified or deleted.
     * @return true if this NodeInst is locked.
     */
    public boolean isLocked() {
        return d.is(ImmutableNodeInst.LOCKED);
    }

    /**
     * Method to set a Technology-specific value on this NodeInst.
     * This is mostly used by the Schematics technology which allows variations
     * on a NodeInst to be stored.
     * For example, the Transistor primitive uses these bits to distinguish nMOS, pMOS, etc.
     * @param value the Technology-specific value to store on this NodeInst.
     */
    public void setTechSpecific(int value) {
        setD(d.withTechSpecific(value), true);
    }

    /**
     * Method to return the Technology-specific value on this NodeInst.
     * This is mostly used by the Schematics technology which allows variations
     * on a NodeInst to be stored.
     * For example, the Transistor primitive uses these bits to distinguish nMOS, pMOS, etc.
     * @return the Technology-specific value on this NodeInst.
     */
    public int getTechSpecific() {
        return d.techBits;
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
    public Rectangle2D findEssentialBounds() {
        NodeProto np = getProto();
        if (!(np instanceof Cell)) {
            return null;
        }
        Rectangle2D eb = ((Cell) np).findEssentialBounds();
        if (eb == null) {
            return null;
        }
        AffineTransform xForm = transformOut();
        Point2D ll = new Point2D.Double(eb.getMinX(), eb.getMinY());
        ll = xForm.transform(ll, null);
        Point2D ur = new Point2D.Double(eb.getMaxX(), eb.getMaxY());
        ur = xForm.transform(ur, null);
        double minX = Math.min(ll.getX(), ur.getX());
        double minY = Math.min(ll.getY(), ur.getY());
        double maxX = Math.max(ll.getX(), ur.getX());
        double maxY = Math.max(ll.getY(), ur.getY());
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * This function is to compare NodeInst elements. Initiative CrossLibCopy
     * @param obj Object to compare to
     * @param buffer To store comparison messages in case of failure
     * @return True if objects represent same NodeInst
     */
    public boolean compare(Object obj, StringBuffer buffer) {
        if (this == obj) {
            return (true);
        }

        // Better if compare classes? but it will crash with obj=null
        if (obj == null || getClass() != obj.getClass()) {
            return (false);
        }

        NodeInst no = (NodeInst) obj;
        if (getFunction() != no.getFunction()) {
            if (buffer != null) {
                buffer.append("Functions are not the same for '" + getName() + "' and '" + no.getName() + "'\n");
            }
            return (false);
        }

        NodeProto noProtoType = no.getProto();
        NodeProto protoType = getProto();

        if (protoType.getClass() != noProtoType.getClass()) {
            if (buffer != null) {
                buffer.append("Not the same node prototypes for '" + getName() + "' and '" + no.getName() + "'\n");
            }
            return (false);
        }

        // Comparing transformation
        if (!rotateOut().equals(no.rotateOut())) {
            if (buffer != null) {
                buffer.append("Not the same rotation for '" + getName() + "' and '" + no.getName() + "'\n");
            }
            return (false);
        }

        // If this is Cell, no is a Cell otherwise class checker would notice
        if (protoType instanceof Cell) {
            // Missing other comparisons
            return (noProtoType instanceof Cell);
        }

        // Technology only valid for PrimitiveNodes?
        PrimitiveNode np = (PrimitiveNode) protoType;
        PrimitiveNode noNp = (PrimitiveNode) noProtoType;
        PrimitiveNode.Function function = getFunction();
        PrimitiveNode.Function noFunc = no.getFunction();
        if (function != noFunc) {
            if (buffer != null) {
                buffer.append("Not the same node prototypes for '" + getName() + "' and '" + no.getName() + "':" + function.getName() + " v/s " + noFunc.getName() + "\n");
            }
            return (false);
        }
        Poly[] polyList = np.getTechnology().getShapeOfNode(this);
        Poly[] noPolyList = noNp.getTechnology().getShapeOfNode(no);

        if (polyList.length != noPolyList.length) {
            if (buffer != null) {
                buffer.append("Not same number of geometries in '" + getName() + "' and '" + no.getName() + "'\n");
            }
            return (false);
        }

        // Compare variables?
        // Has to be another way more eficient
        // Remove noCheckList if equals is implemented
        // Sort them out by a key so comparison won't be O(n2)
        List<Object> noCheckAgain = new ArrayList<Object>();
        for (int i = 0; i < polyList.length; i++) {
            boolean found = false;
            for (int j = 0; j < noPolyList.length; j++) {
                // Already found
                if (noCheckAgain.contains(noPolyList[j])) {
                    continue;
                }
                if (polyList[i].compare(noPolyList[j], buffer)) {
                    found = true;
                    noCheckAgain.add(noPolyList[j]);
                    break;
                }
            }
            // polyList[i] doesn't match any elem in noPolyList
            if (!found) {
                if (buffer != null) {
                    buffer.append("No corresponding geometry in '" + getName() + "' found in '" + no.getName() + "'\n");
                }
                return (false);
            }
        }
        // Ports comparison
        // Not sure if this comparison is necessary
        // @TODO simply these calls by a generic function or template
        noCheckAgain.clear();
        for (Iterator<PortInst> it = getPortInsts(); it.hasNext();) {
            boolean found = false;
            PortInst port = it.next();

            for (Iterator<PortInst> i = no.getPortInsts(); i.hasNext();) {
                PortInst p = i.next();

                if (noCheckAgain.contains(p)) {
                    continue;
                }

                if (port.compare(p, buffer)) {
                    found = true;
                    noCheckAgain.add(p);
                    break;
                }
            }
            // No correspoding PortInst found
            if (!found) {
                // Error messages added in port.compare()
//                if (buffer != null)
//                    buffer.append("No corresponding port '" + port.getPortProto().getName() + "' found in '" + no.getName() + "'\n");
                return (false);
            }
        }

        // Comparing Exports
        noCheckAgain.clear();
        for (Iterator<Export> it = getExports(); it.hasNext();) {
            Export export = it.next();
            boolean found = false;

            for (Iterator<Export> i = no.getExports(); i.hasNext();) {
                Export p = i.next();

                if (noCheckAgain.contains(p)) {
                    continue;
                }

                if (export.compare(p, buffer)) {
                    found = true;
                    noCheckAgain.add(p);
                    break;
                }
            }
            // No correspoding Export found
            if (!found) {
                if (buffer != null) {
                    buffer.append("No corresponding export '" + export.getName() + "' found in '" + no.getName() + "'\n");
                }
                return (false);
            }
        }

        for (Iterator<Variable> it1 = getVariables(), it2 = no.getVariables(); it1.hasNext() || it2.hasNext();) {
            if (!it1.hasNext() || !it2.hasNext()) {
                if (buffer != null) {
                    buffer.append("Different number of variables found in '" + no.getName() + "'\n");
                }
                return false;
            }
            Variable param1 = it1.next();
            Variable param2 = it2.next();
            if (!param1.compare(param2, buffer)) {
                if (buffer != null) {
                    buffer.append("No corresponding parameter '" + param1 + "' found in '" + no.getName() + "'\n");
                }
                return false;
            }
        }
        noCheckAgain.clear();
        for (Iterator<Variable> it = getVariables(); it.hasNext();) {
            Variable var = it.next();
            boolean found = false;

            for (Iterator<Variable> i = no.getVariables(); i.hasNext();) {
                Variable p = i.next();

                if (noCheckAgain.contains(p)) {
                    continue;
                }

                if (var.compare(p, buffer)) {
                    found = true;
                    noCheckAgain.add(p);
                    break;
                }
            }
            // No correspoding Variable found
            if (!found) {
                if (buffer != null) {
                    buffer.append("No corresponding variable '" + var + "' found in '" + no.getName() + "'\n");
                }
                return (false);
            }
        }

        return (true);
    }
}
