/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RouteElement.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 1, 2004
 * Time: 12:55:36 PM
 *
 * A Route describes a new connection to be made.
 * A Route consists of RouteElements, which in
 * one sense are either nodes are arcs.  However,
 * in another sense we can consider them to be actions,
 * such as new node, new arc, or arc delete, or node delete.<p>
 * Arc/node delete happens when an existing arc is replaced
 * by two or more new arcs, or when an existing route
 * needs to be uprooted in order to make room for new routes.
 */

public class RouteElement {

    /**
     * RouteElementAction is a type safe enum class for
     * describing the action to be take by a RouteElement
     * object.
     */
    public static class RouteElementAction {
        private String name;

        public RouteElementAction(String name) { this.name = name; }
        public String toString() { return name; }

        public static final RouteElementAction newNode = new RouteElementAction("newNode");
        public static final RouteElementAction newArc = new RouteElementAction("newArc");
        public static final RouteElementAction deleteNode = new RouteElementAction("deleteNode");
        public static final RouteElementAction deleteArc = new RouteElementAction("deleteArc");
        /** used to demark already existing end/start point of route */
        public static final RouteElementAction existingPortInst = new RouteElementAction("existingPortInst");
    }


    /** the action to be taken */                   private RouteElementAction action;
    /** the Cell in which to take the action */     private Cell cell;
    /** If action has been done */                  private boolean done;
    /** Whether or not to highlight */              private boolean showHighlight;

    // ---- New Node info ----
    /** Node type to create */                      private NodeProto np;
    /** Port on node to use */                      private PortProto newNodePort;
    /** location to create Node */                  private Point2D location;
    /** size aspect */                              private double width, height;
    /** if this bisects an arc */                   private boolean isBisectArcPin;
    /** the newly created instance */               private NodeInst newNodeInst;
    /** newArcs connecting to this */               private List newArcs;

    // ---- New Arc info ----
    /** Arc type to create */                       private ArcProto ap;
    /** width of arc */                             private double arcWidth;
    /** Head of arc */                              private RouteElement headRE;
    /** Tail of arc */                              private RouteElement tailRE;
    /** Name of arc */                              private String arcName;
    /** Angle of arc */                             private int arcAngle;

    // ---- Delete Node/Arc info ----
    /** NodeInst to delete */                       private NodeInst nodeInstToDelete;
    /** ArcInst to delete */                        private ArcInst arcInstToDelete;

    // ---- Existing PortInst info ----
    /** the existing PortInst */                    private PortInst existingPortInst;

    // ---- New Node and Existing PortInst info ----
    /** point to connect arc to, if not center of pi */ private Point2D connPoint;

    /**
     * Private Constructor
     * @param action the action this RouteElementAction will do.
     */
    private RouteElement(RouteElementAction action) { this.action = action; }


    // ---------------------------- Factory Methods -----------------------------

    /**
     * Factory method for making a newNode RouteElement
     * @param np Type of NodeInst to make
     * @param location the location of the new NodeInst
     * @param width the width of the new NodeInst
     * @param height the height of the new NodeInst
     */
    public static RouteElement newNode(Cell cell, NodeProto np, PortProto newNodePort, Point2D location,
                                       double width, double height) {
        RouteElement e = new RouteElement(RouteElementAction.newNode);
        e.done = false;
        e.cell = cell;
        e.np = np;
        e.newNodePort = newNodePort;
        e.location = location;
        e.newNodeInst = null;
        e.isBisectArcPin = false;
        e.newArcs = new ArrayList();
        e.setNodeSize(new Dimension2D.Double(width, height));
        e.showHighlight = true;
        e.connPoint = null;
        return e;
    }

    /**
     * Factory method for making a newArc RouteElement
     * @param ap Type of ArcInst to make
     * @param headRE RouteElement (must be newNode or existingPortInst) at head of arc
     * @param tailRE RouteElement (must be newNode or existingPortInst) at tail or arc
     */
    public static RouteElement newArc(Cell cell, ArcProto ap, double arcWidth, RouteElement headRE, RouteElement tailRE, String name) {
        RouteElement e = new RouteElement(RouteElementAction.newArc);
        e.done = false;
        e.cell = cell;
        e.ap = ap;
        e.arcWidth = arcWidth;
        e.headRE = headRE;
        e.tailRE = tailRE;
        e.arcName = name;
        if (headRE.getAction() != RouteElementAction.newNode &&
            headRE.getAction() != RouteElementAction.existingPortInst)
            System.out.println("  ERROR: headRE of newArc RouteElement must be newNode or existingPortInst");
        if (tailRE.getAction() != RouteElementAction.newNode &&
            tailRE.getAction() != RouteElementAction.existingPortInst)
            System.out.println("  ERROR: tailRE of newArc RouteElement must be newNode or existingPortInst");
        headRE.addConnectingNewArc(e);
        tailRE.addConnectingNewArc(e);
        e.arcAngle = 0;
        e.showHighlight = true;
        return e;
    }

    /**
     * Factory method for making a deleteNode RouteElement
     * @param nodeInstToDelete the nodeInst to delete
     */
    public static RouteElement deleteNode(NodeInst nodeInstToDelete) {
        RouteElement e = new RouteElement(RouteElementAction.deleteNode);
        e.done = false;
        e.nodeInstToDelete = nodeInstToDelete;
        e.showHighlight = true;
        e.cell = nodeInstToDelete.getParent();
        return e;
    }

    /**
     * Factory method for making a deleteArc RouteElement
     * @param arcInstToDelete the arcInst to delete
     */
    public static RouteElement deleteArc(ArcInst arcInstToDelete) {
        RouteElement e = new RouteElement(RouteElementAction.deleteArc);
        e.done = false;
        e.arcInstToDelete = arcInstToDelete;
        e.cell = arcInstToDelete.getParent();
        e.showHighlight = true;
        return e;
    }

    /**
     * Factory method for making a dummy RouteElement for an
     * existing PortInst. This is usually use to demark the
     * start and/or ends of the route, which exist before
     * we start building the route.
     * @param existingPortInst the already existing portInst to connect to
     * @param connPoint the end point of the arc, if not the center of the portInst
     */
    public static RouteElement existingPortInst(PortInst existingPortInst, Point2D connPoint) {
        RouteElement e = new RouteElement(RouteElementAction.existingPortInst);
        e.done = true;                           // already exists, so done is true
        e.existingPortInst = existingPortInst;
        e.newArcs = new ArrayList();
        e.cell = existingPortInst.getNodeInst().getParent();
        e.showHighlight = true;
        e.connPoint = connPoint;
        return e;
    }


    // ---------------------------- Field Access Methods -------------------------

    /** see if action has been done */
    public boolean isDone() { return done; }

    /** set done to true to indication action has been done */
    public void setDone() { done = true; }

    /** get RouteElementAction */
    public RouteElementAction getAction() { return action; }

    /**
     * Get the PortProto for connecting to this RouteElement.
     * This is not the same as getConnectingPort().getPortProto(),
     * because if the action has not yet been done the PortInst
     * returned by getConnectingPort() may have not yet been created.
     * This method is only valid for actions newNode and existingPortInst.
     * @return a PortProto of port to connect to this RouteElement.
     */
    public PortProto getPortProto() {
        if (action == RouteElementAction.newNode)
            return newNodePort;
        if (action == RouteElementAction.existingPortInst)
            return existingPortInst.getPortProto();
        return null;
    }

    /**
     * Get Connecting Port on RouteElement.  RouteElement
     * must be a existingPortInst or newNode.  If it is
     * a newNode, that newNode must be been created (i.e.
     * action is done).
     * @return the PortInst, or null on error
     */
    public PortInst getConnectingPort() {
        if (!done) return null;
        if (action == RouteElementAction.existingPortInst)
            return existingPortInst;
        if (action == RouteElementAction.newNode)
            return newNodeInst.findPortInstFromProto(newNodePort);
        return null;
    }

    /**
     * Get the arc proto to be created/deleted.  RouteElement
     * must be a newArc or deleteArc.  Otherwise, it returns null.
     * @return the arc proto if newArc or deleteArc, otherwise null.
     */
    public ArcProto getArcProto() {
        if (action == RouteElementAction.newArc)
            return ap;
        if (action == RouteElementAction.deleteArc)
            return arcInstToDelete.getProto();
        return null;
    }

    /** Return the cell in which this RouteElement will do it's action */
    public Cell getCell() { return cell; }

    /** Returns location of newNode, existingPortInst, or deleteNode,
     * or null otherwise */
    public Point2D getLocation() {
        if (action == RouteElementAction.newNode) return location;
        if (action == RouteElementAction.existingPortInst)
            return getConnPoint();
        if (action == RouteElementAction.deleteNode)
            return nodeInstToDelete.getTrueCenter();
        return null;
    }

    /**
     * Return arc width if this is a newArc RouteElement, otherwise returns -1.
     */
    public double getArcWidth() {
        if (action != RouteElementAction.newArc) return -1;
        return arcWidth;
    }

    /**
     * Set the arc width if this is a newArc RouteElement, otherwise does nothing.
     */
    public void setArcWidth(double width) {
        if (action == RouteElementAction.newArc) {
            arcWidth = width;
        }
    }

    /**
     * Set a newArc's angle. This only does something if both the
     * head and tail of the arc are coincident points. This does
     * nothing if the RouteElement is not a newArc
     * @param angle the angle, in tenth degrees
     */
    public void setArcAngle(int angle) {
        if (action == RouteElementAction.newArc) arcAngle = angle;
    }

    /**
     * Return true if the new arc is a vertical arc, false otherwise
     */
    public boolean isNewArcVertical() {
        if (action == RouteElementAction.newArc) {
            // check if arc is vertical
            Point2D head = headRE.getLocation();
            Point2D tail = tailRE.getLocation();
            if ((head == null) || (tail == null)) return false;
            if (head.getX() == tail.getX()) return true;
        }
        return false;
    }

    /**
     * Return true if the new arc is a horizontal arc, false otherwise
     */
    public boolean isNewArcHorizontal() {
        if (action == RouteElementAction.newArc) {
            // check if arc is vertical
            Point2D head = headRE.getLocation();
            Point2D tail = tailRE.getLocation();
            if ((head == null) || (tail == null)) return false;
            if (head.getY() == tail.getY()) return true;
        }
        return false;
    }



    /** Return string decribing the RouteElement */
    public String toString() {
        if (action == RouteElementAction.newNode) {
            return "RouteElement newNode "+np+" size "+width+","+height+" at "+location;
        }
        else if (action == RouteElementAction.deleteNode) {
            return "RouteElement deleteNode "+nodeInstToDelete;
        }
        else if (action == RouteElementAction.existingPortInst) {
            return "RouteElement existingPortInst "+existingPortInst;
        }
        else if (action == RouteElementAction.newArc) {
            return "RouteElement newArc "+ap+",\nhead: "+headRE+"\ntail: "+tailRE;
        }
        else if (action == RouteElementAction.deleteArc) {
            return "RouteElement deleteArc "+arcInstToDelete;
        }
        return "RouteElement bad action";
    }

    /** Set true by Interactive router if pin used to bisect arc
     * Router may want to remove this pin later if it places a
     * connecting contact cut in the same position.
     */
    public void setIsBisectArcPin(boolean state) {
        if (action == RouteElementAction.newNode)
            isBisectArcPin = state;
    }

    /** see setIsBisectArcPin */
    public boolean isBisectArcPin() {
        if (action == RouteElementAction.newNode)
            return isBisectArcPin;
        else
            return false;
    }

    /** Used to update end points of new arc if they change
     * Only valid if called on newArcs, does nothing otherwise.
     */
    public void replaceArcEnd(RouteElement oldEnd, RouteElement newEnd) {
        if (action == RouteElementAction.newArc) {
            if (headRE == oldEnd) {
                headRE = newEnd;
                // update book-keeping
                oldEnd.removeConnectingNewArc(this);
                newEnd.addConnectingNewArc(this);
            }
            if (tailRE == oldEnd) {
                tailRE = newEnd;
                // update book-keeping
                oldEnd.removeConnectingNewArc(this);
                newEnd.addConnectingNewArc(this);
            }
        }
    }

    /**
     * Book-keeping: Adds a newArc RouteElement to a list to keep
     * track of what newArc elements use this object as an end point.
     * This must be a RouteElement of type newNode or existingPortInst.
     * @param re the RouteElement to add.
     */
    private void addConnectingNewArc(RouteElement re) {
        if (re.getAction() != RouteElementAction.newArc) return;
        if (action == RouteElementAction.newNode)
            newArcs.add(re);
        if (action == RouteElementAction.existingPortInst)
            newArcs.add(re);
    }

    /**
     * Reomve a newArc that connects to this newNode or existingPortInst.
     * @param re the RouteElement to remove
     */
    private void removeConnectingNewArc(RouteElement re) {
        if (re.getAction() != RouteElementAction.newArc) return;
        newArcs.remove(re);
    }

    /**
     * Get largest arc width of newArc RouteElements attached to this
     * RouteElement.  If none present returns -1.
     */
    public double getWidestConnectingArc(ArcProto ap) {
        double width = -1;

        if (action == RouteElementAction.existingPortInst) {
            // find all arcs of type ap connected to this
            for (Iterator it = existingPortInst.getConnections(); it.hasNext(); ) {
                Connection conn = (Connection)it.next();
                ArcInst arc = conn.getArc();
                if (arc.getProto() == ap) {
                    if (arc.getWidth() > width) width = arc.getWidth();
                }
            }
        }

        if (action == RouteElementAction.newNode) {
            if (newArcs == null) return -1;
            for (Iterator it = newArcs.iterator(); it.hasNext(); ) {
                RouteElement re = (RouteElement)it.next();
                if (re.getArcProto() == ap) {
                    if (re.getArcWidth() > width) width = re.getArcWidth();
                }
            }
        }

        if (action == RouteElementAction.newArc) {
            if (getArcProto() == ap) {
                if (getArcWidth() > width) return getArcWidth();
            }
        }
        return width;
    }

    /**
     * Get an iterator over any newArc RouteElements connected to this
     * newNode RouteElement.  Returns an iterator over an empty list
     * if no new arcs.
     */
    public Iterator getNewArcs() {
        if (action == RouteElementAction.newNode) {
            return newArcs.iterator();
        }
        ArrayList list = new ArrayList();
        return list.iterator();
    }

    /**
     * Get the size of a newNode, or the NodeInst an existingPortInst
     * is attached to.
     * @return the width,height of the node, or (-1, -1) if not a node
     */
    public Dimension2D.Double getNodeSize() {
        if (action == RouteElementAction.newNode) {
            return new Dimension2D.Double(width, height);
        }
        if (action == RouteElementAction.existingPortInst) {
            NodeInst ni = existingPortInst.getNodeInst();
            return new Dimension2D.Double(ni.getXSize(), ni.getYSize());
        }
        else return new Dimension2D.Double(-1, -1);
    }

    /**
     * Set the size of a newNode.  Does not make it smaller
     * than the default size if this is a PrimitiveNode.
     * Does nothing for other RouteElements.
     * @param size the new size
     */
    public void setNodeSize(Dimension2D size) {
        if (action == RouteElementAction.newNode) {
            SizeOffset so = np.getSizeOffset();
            double widthoffset = so.getLowXOffset() + so.getHighXOffset();
            double heightoffset = so.getLowYOffset() + so.getHighYOffset();

            double defWidth = np.getDefWidth() - widthoffset;       // this is width we see on the screen
            double defHeight = np.getDefHeight() - heightoffset;    // this is height we see on the screen
            if (size.getWidth() > defWidth) width = size.getWidth(); else width = defWidth;
            if (size.getHeight() > defHeight) height = size.getHeight(); else height = defHeight;
        }
    }

    /**
     * Get connecting point of node for arc to connect to. If this was
     * not specified earlier, this returns the center of the portInst.
     * Note that this will return null for a newNode that has not yet been
     * created, as it does not yet have a portInst.
     * @return the connecting port for an arc, or null if none exists yet.
     */
    public Point2D getConnPoint() {
        if (action == RouteElementAction.newNode) {
            if (connPoint != null) return connPoint;
            if (done) {
                Rectangle2D bounds = getConnectingPort().getBounds();
                return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
            }
        }
        if (action == RouteElementAction.existingPortInst) {
            if (connPoint != null) return connPoint;
            Rectangle2D bounds = getConnectingPort().getBounds();
            return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
        }
        return null;
    }

    /** Set show highlight property */
    public void setShowHighlight(boolean b) { showHighlight = b; }

    // -------------------------- Action/Highlight Methods ------------------------

    /**
     * Perform the action specified by RouteElementAction <i>action</i>.
     * Note that this method performs database editing, and should only
     * be called from within a Job.
     * @return the object created, or null if deleted or nothing done.
     */
    public ElectricObject doAction() {

        if (done) return null;

        if (action == RouteElementAction.newNode) {
            // create new Node
            SizeOffset so = np.getSizeOffset();
            double widthso = width +  so.getLowXOffset() + so.getHighXOffset();
            double heightso = height + so.getLowYOffset() + so.getHighYOffset();
            newNodeInst = NodeInst.makeInstance(np, location, widthso, heightso, 0, cell, null);
            if (newNodeInst == null) return null;
            setDone();
            return newNodeInst;
        }
        if (action == RouteElementAction.deleteNode) {
            // delete existing arc
            nodeInstToDelete.kill();
            setDone();
        }
        if (action == RouteElementAction.newArc) {
            PortInst headPi = headRE.getConnectingPort();
            PortInst tailPi = tailRE.getConnectingPort();
            Point2D headPoint = headRE.getConnPoint();
            Point2D tailPoint = tailRE.getConnPoint();
            ArcInst newAi = ArcInst.makeInstance(ap, arcWidth, headPi, headPoint, tailPi, tailPoint, arcName);
            if (arcAngle != 0)
                newAi.setAngle(arcAngle);
            setDone();
            return newAi;
        }
        if (action == RouteElementAction.deleteArc) {
            // delete existing arc
            arcInstToDelete.kill();
            setDone();
        }
        return null;
    }

    /**
     * Adds RouteElement to highlights
     */
    public void addHighlightArea() {

        if (!showHighlight) return;

        if (action == RouteElementAction.newNode) {
            // create box around new Node
            Rectangle2D bounds = new Rectangle2D.Double(location.getX()-0.5*width,
                    location.getY()-0.5*height, width, height);
            Highlight.addArea(bounds, cell);
        }
        if (action == RouteElementAction.newArc) {
            // figure out highlight area based on arc width and start and end locations
            Point2D headPoint = headRE.getLocation();
            Point2D tailPoint = tailRE.getLocation();
            double angle = Math.atan((tailPoint.getY()-headPoint.getY())/(tailPoint.getX()-headPoint.getX()));
            double offsetX = 0.5*width*Math.cos(angle);
            double offsetY = 0.5*width*Math.sin(angle);
            Point2D head1 = new Point2D.Double(headPoint.getX()+offsetX, headPoint.getY()+offsetY);
            Point2D head2 = new Point2D.Double(headPoint.getX()-offsetX, headPoint.getY()-offsetY);
            Point2D tail1 = new Point2D.Double(tailPoint.getX()+offsetX, tailPoint.getY()+offsetY);
            Point2D tail2 = new Point2D.Double(tailPoint.getX()-offsetX, tailPoint.getY()-offsetY);
            Highlight.addLine(head1, tail1, cell);
            Highlight.addLine(headPoint, tailPoint, cell);
            Highlight.addLine(head2, tail2, cell);
            Highlight.addLine(head1, head2, cell);
            Highlight.addLine(tail1, tail2, cell);
        }
        if (action == RouteElementAction.existingPortInst) {
            Highlight.addElectricObject(existingPortInst, cell);
        }
        if (action == RouteElementAction.deleteArc) {
            Highlight.addElectricObject(arcInstToDelete, cell);
        }
    }
}
