package com.sun.electric.tool.routing;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.tool.user.Highlight;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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

    // ---- Delete Node/Arc info ----
    /** NodeInst to delete */                       private NodeInst nodeInstToDelete;
    /** ArcInst to delete */                        private ArcInst arcInstToDelete;

    // ---- Existing PortInst info ----
    /** the existing PortInst */                    private PortInst existingPortInst;

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
        e.width = width;
        e.height = height;
        e.newNodeInst = null;
        e.isBisectArcPin = false;
        e.newArcs = new ArrayList();
        return e;
    }

    /**
     * Factory method for making a newArc RouteElement
     * @param ap Type of ArcInst to make
     * @param headRE RouteElement (must be newNode or existingPortInst) at head of arc
     * @param tailRE RouteElement (must be newNode or existingPortInst) at tail or arc
     */
    public static RouteElement newArc(Cell cell, ArcProto ap, double arcWidth, RouteElement headRE, RouteElement tailRE) {
        RouteElement e = new RouteElement(RouteElementAction.newArc);
        e.done = false;
        e.cell = cell;
        e.ap = ap;
        e.arcWidth = arcWidth;
        e.headRE = headRE;
        e.tailRE = tailRE;
        if (headRE.getAction() != RouteElementAction.newNode &&
            headRE.getAction() != RouteElementAction.existingPortInst)
            System.out.println("  ERROR: headRE of newArc RouteElement must be newNode or existingPortInst");
        if (tailRE.getAction() != RouteElementAction.newNode &&
            tailRE.getAction() != RouteElementAction.existingPortInst)
            System.out.println("  ERROR: tailRE of newArc RouteElement must be newNode or existingPortInst");
        headRE.addConnectingNewArc(e);
        tailRE.addConnectingNewArc(e);
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
        return e;
    }

    /**
     * Factory method for making a dummy RouteElement for an
     * existing PortInst. This is usually use to demark the
     * start and/or ends of the route, which exist before
     * we start building the route.
     * @param existingPortInst the already existing portInst to connect to
     */
    public static RouteElement existingPortInst(PortInst existingPortInst) {
        RouteElement e = new RouteElement(RouteElementAction.existingPortInst);
        e.done = true;                           // already exists, so done is true
        e.existingPortInst = existingPortInst;
        e.newArcs = new ArrayList();
        e.cell = existingPortInst.getNodeInst().getParent();
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
            return new Point2D.Double(existingPortInst.getBounds().getCenterX(),
                                      existingPortInst.getBounds().getCenterY());
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
            if (headRE == oldEnd) headRE = newEnd;
            if (tailRE == oldEnd) tailRE = newEnd;
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
     * Get largest arc width of newArc RouteElements attached to this
     * RouteElement.  If none present or this is not a newNode/exisingPortInst
     * RouteElement, returns -1.
     */
    public double getWidestConnectingArc() {
        if (newArcs == null) return -1;
        double width = -1;
        for (Iterator it = newArcs.iterator(); it.hasNext(); ) {
            RouteElement re = (RouteElement)it.next();
            if (re.getArcWidth() > width) width = re.getArcWidth();
        }
        return width;
    }

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
            newNodeInst = NodeInst.makeInstance(np, location, width, height, 0, cell, null);
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
            ArcInst newAi = ArcInst.makeInstance(ap, arcWidth, headPi, tailPi, null);
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
    }
}
