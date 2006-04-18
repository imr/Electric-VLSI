/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RouteElementPort.java
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

package com.sun.electric.tool.routing;

import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.routing.RouteElement.RouteElementAction;
import com.sun.electric.tool.user.Highlighter;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class for defining RouteElements that are ports.
 */
public class RouteElementPort extends RouteElement {

    // ---- New Port info ----
    /** Node type to create */                      private NodeProto np;
    /** Port on node to use */                      private PortProto portProto;
    /** location to create Node */                  private EPoint location;
    /** size aspect that is seen on screen */       private double width, height;
    /** if this bisects an arc */                   private boolean isBisectArcPin;
    /** RouteElementArcs connecting to this */      private List<RouteElementArc> newArcs;
    /** port site spatial extents (db units) */     private transient Poly portInstSite;

    /** This contains the newly created instance, or the instance to delete */ private NodeInst nodeInst;
    /** This contains the newly created portinst, or the existing port inst */ private PortInst portInst;

    /**
     * Private Constructor
     * @param action the action this RouteElementAction will do.
     */
    private RouteElementPort(RouteElementAction action, Cell cell) { super(action, cell); }

    /**
     * Factory method for making a newNode RouteElement
     * @param np Type of NodeInst to make
     * @param location the location of the new NodeInst
     * @param width the width of the new NodeInst
     * @param height the height of the new NodeInst
     */
    public static RouteElementPort newNode(Cell cell, NodeProto np, PortProto newNodePort, Point2D location,
                                       double width, double height) {
        RouteElementPort e = new RouteElementPort(RouteElement.RouteElementAction.newNode, cell);
        e.np = np;
        e.portProto = newNodePort;
        e.location = EPoint.snap(location);
        e.isBisectArcPin = false;
        e.newArcs = new ArrayList<RouteElementArc>();
        e.setNodeSize(new Dimension2D.Double(width, height));
        e.nodeInst = null;
        e.portInst = null;
        Point2D [] points = {location};
        e.portInstSite = new Poly(points);
        return e;
    }

    /**
     * Factory method for making a deleteNode RouteElement
     * @param nodeInstToDelete the nodeInst to delete
     */
    public static RouteElementPort deleteNode(NodeInst nodeInstToDelete) {
        RouteElementPort e = new RouteElementPort(RouteElement.RouteElementAction.deleteNode, nodeInstToDelete.getParent());
        e.np = nodeInstToDelete.getProto();
        e.portProto = null;
        e.location = EPoint.snap(nodeInstToDelete.getTrueCenter());
        e.isBisectArcPin = false;
        e.newArcs = new ArrayList<RouteElementArc>();
        e.setNodeSize(new Dimension2D.Double(nodeInstToDelete.getXSize(), nodeInstToDelete.getYSize()));
        e.nodeInst = nodeInstToDelete;
        e.portInst = null;
        e.portInstSite = null;
        return e;
    }

    /**
     * Factory method for making a dummy RouteElement for an
     * existing PortInst. This is usually use to demark the
     * start and/or ends of the route, which exist before
     * we start building the route.
     * @param existingPortInst the already existing portInst to connect to
     */
    public static RouteElementPort existingPortInst(PortInst existingPortInst, EPoint portInstSite) {
        Point2D [] points = {portInstSite};
        Poly poly = new Poly(points);
        return existingPortInst(existingPortInst, poly);
    }

    /**
     * Factory method for making a dummy RouteElement for an
     * existing PortInst. This is usually use to demark the
     * start and/or ends of the route, which exist before
     * we start building the route.
     * @param existingPortInst the already existing portInst to connect to
     */
    public static RouteElementPort existingPortInst(PortInst existingPortInst, Poly portInstSite) {
        RouteElementPort e = new RouteElementPort(RouteElement.RouteElementAction.existingPortInst, existingPortInst.getNodeInst().getParent());
        NodeInst nodeInst = existingPortInst.getNodeInst();
        e.np = nodeInst.getProto();
        e.portProto = existingPortInst.getPortProto();
        e.location = EPoint.snap(nodeInst.getTrueCenter());
        e.isBisectArcPin = false;
        e.newArcs = new ArrayList<RouteElementArc>();
        e.setNodeSize(new Dimension2D.Double(nodeInst.getXSize(), nodeInst.getYSize()));
        e.nodeInst = nodeInst;
        e.portInst = existingPortInst;
        e.portInstSite = portInstSite;
        return e;

   }

    /**
     * Get the PortProto for connecting to this RouteElementPort.
     * This is not the same as getPortInst().getPortProto(),
     * because if the action has not yet been done the PortInst
     * returned by getPortInst() may have not yet been created.
     * For a deleteNode, this will return null.
     * @return a PortProto of port to connect to this RouteElement.
     */
    public PortProto getPortProto() { return portProto; }

    /**
     * Get Connecting Port on RouteElement.
     * @return the PortInst, or null on error
     */
    public PortInst getPortInst() { return portInst; }

    /**
     * Get Connecting Node on RouteElement.
     * @return the NodeInst, or null on error
     */
    public NodeInst getNodeInst() { return nodeInst; }

    /** Returns location of newNode, existingPortInst, or deleteNode,
     * or null otherwise */
    public Point2D getLocation() { return location; }

    /** Set true by Interactive router if pin used to bisect arc
     * Router may want to remove this pin later if it places a
     * connecting contact cut in the same position.
     */
    public void setBisectArcPin(boolean state) { isBisectArcPin = state; }

    /** see setBisectArcPin */
    public boolean isBisectArcPin() { return isBisectArcPin; }

    /**
     * Book-keeping: Adds a newArc RouteElement to a list to keep
     * track of what newArc elements use this object as an end point.
     * This must be a RouteElement of type newNode or existingPortInst.
     * @param re the RouteElement to add.
     */
    public void addConnectingNewArc(RouteElementArc re) {
        if (re.getAction() != RouteElementAction.newArc) return;
        newArcs.add(re);
    }

    /**
     * Reomve a newArc that connects to this newNode or existingPortInst.
     * @param re the RouteElement to remove
     */
    public void removeConnectingNewArc(RouteElementArc re) {
        if (re.getAction() != RouteElementAction.newArc) return;
        newArcs.remove(re);
    }

    /**
     * Get largest arc width of newArc RouteElements attached to this
     * RouteElement.  If none present returns -1.
     * <p>Note that these width values should have been pre-adjusted for
     * the arc width offset, so these values have had the offset subtracted away.
     */
    public double getWidestConnectingArc(ArcProto ap) {
        double width = -1;

        if (getAction() == RouteElementAction.existingPortInst) {
            // find all arcs of type ap connected to this
            for (Iterator<Connection> it = portInst.getConnections(); it.hasNext(); ) {
                Connection conn = it.next();
                ArcInst arc = conn.getArc();
                if (arc.getProto() == ap) {
                    double newWidth = arc.getWidth() - arc.getProto().getWidthOffset();
                    if (newWidth > width) width = newWidth;
                }
            }
        }

        if (getAction() == RouteElementAction.newNode) {
            if (newArcs == null) return -1;
            for (RouteElementArc re : newArcs) {
                if (re.getArcProto() == ap) {
                    if (re.getOffsetArcWidth() > width) width = re.getOffsetArcWidth();
                }
            }
        }

        return width;
    }

    /**
     * Get an iterator over any newArc RouteElements connected to this
     * newNode RouteElement.  Returns an iterator over an empty list
     * if no new arcs.
     */
    public Iterator<RouteElement> getNewArcs() {
        ArrayList<RouteElement> list = new ArrayList<RouteElement>();
        list.addAll(newArcs);
        return list.iterator();
    }

    /**
     * Get the size of a newNode, or the NodeInst an existingPortInst
     * is attached to.
     * @return the width,height of the node, or (-1, -1) if not a node
     */
    public Dimension2D.Double getNodeSize() {
        return new Dimension2D.Double(width, height);
    }

    /**
     * Set the size of a newNode.  Does not make it smaller
     * than the default size if this is a PrimitiveNode.
     * Does nothing for other RouteElements.
     * @param size the new size
     */
    public void setNodeSize(Dimension2D size) {
        SizeOffset so = np.getProtoSizeOffset();
        double widthoffset = so.getLowXOffset() + so.getHighXOffset();
        double heightoffset = so.getLowYOffset() + so.getHighYOffset();

        double defWidth = np.getDefWidth() - widthoffset;       // this is width we see on the screen
        double defHeight = np.getDefHeight() - heightoffset;    // this is height we see on the screen
        if (size.getWidth() > defWidth) width = size.getWidth(); else width = defWidth;
        if (size.getHeight() > defHeight) height = size.getHeight(); else height = defHeight;
    }

    /**
     * Get a polygon that defines the port dimensions.
     * May return null.
     */
    public Poly getConnectingSite() { return portInstSite; }

    /**
     * Perform the action specified by RouteElementAction <i>action</i>.
     * Note that this method performs database editing, and should only
     * be called from within a Job.
     * @return the object created, or null if deleted or nothing done.
     */
    public ElectricObject doAction() {

        EDatabase.serverDatabase().checkChanging();

        if (isDone()) return null;
        ElectricObject returnObj = null;

        if (getAction() == RouteElementAction.newNode) {
            // create new Node
            SizeOffset so = np.getProtoSizeOffset();
            double widthso = width +  so.getLowXOffset() + so.getHighXOffset();
            double heightso = height + so.getLowYOffset() + so.getHighYOffset();
            nodeInst = NodeInst.makeInstance(np, location, widthso, heightso, getCell());
            if (nodeInst == null) return null;
            portInst = nodeInst.findPortInstFromProto(portProto);
            returnObj = nodeInst;
        }
        if (getAction() == RouteElementAction.deleteNode) {
            // delete existing arc
            nodeInst.kill();
        }
        setDone();
        return returnObj;
    }

    /**
     * Adds RouteElement to highlights
     */
    public void addHighlightArea(Highlighter highlighter) {

        if (!isShowHighlight()) return;

        if (getAction() == RouteElementAction.newNode) {
            // create box around new Node
            Rectangle2D bounds = new Rectangle2D.Double(location.getX()-0.5*width,
                    location.getY()-0.5*height, width, height);
            highlighter.addArea(bounds, getCell());
        }
        if (getAction() == RouteElementAction.existingPortInst) {
            highlighter.addElectricObject(portInst, getCell());
        }
    }

    /** Return string decribing the RouteElement */
    public String toString() {
        if (getAction() == RouteElementAction.newNode) {
            return "RouteElementPort newNode "+np+" size "+width+","+height+" at "+location;
        }
        else if (getAction() == RouteElementAction.deleteNode) {
            return "RouteElementPort deleteNode "+nodeInst;
        }
        else if (getAction() == RouteElementAction.existingPortInst) {
            return "RouteElementPort existingPortInst "+portInst;
        }
        return "RouteElement bad action";
    }

    /**
     * Save the state of the <tt>RouteElementPort</tt> instance to a stream (that
     * is, serialize it).
     *
     * @serialData The numnber of points in portInstSite polygon is emitted (int),
     * followed by all of its coordianates ( as pair of doubles ) in the proper order.
     */
    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
        s.defaultWriteObject();
        if (portInstSite != null) {
            Point2D[] points = portInstSite.getPoints();
            s.writeInt(points.length);
            for (int i = 0; i < points.length; i++) {
                s.writeDouble(points[i].getX());
                s.writeDouble(points[i].getY());
            }
        } else {
            s.writeInt(-1);
        }
    }

    /**
     * Reconstitute the <tt>RouteElementPort</tt> instance from a stream (that is,
     * deserialize it).
     */
    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        int len = s.readInt();
        if (len >= 0) {
            Point2D[] points = new Point2D[len];
            for (int i = 0; i < len; i++) {
                double x = s.readDouble();
                double y = s.readDouble();
                points[i] = new Point2D.Double(x, y);
            }
        }
    }
}
