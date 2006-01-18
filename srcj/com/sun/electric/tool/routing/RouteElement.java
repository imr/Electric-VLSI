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
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.tool.user.Highlighter;
import java.io.Serializable;

/**
 * A Route describes a new connection to be made.
 * A Route consists of RouteElements, which in
 * one sense are either nodes are arcs.  However,
 * in another sense we can consider them to be actions,
 * such as new node, new arc, or arc delete, or node delete.<p>
 * Arc/node delete happens when an existing arc is replaced
 * by two or more new arcs, or when an existing route
 * needs to be uprooted in order to make room for new routes.
 * 
 * Author: gainsley
 */

public abstract class RouteElement implements Serializable {

    /**
     * RouteElementAction is a type safe enum class for
     * describing the action to be take by a RouteElement
     * object.
     */
    public static enum RouteElementAction { newNode, newArc, deleteNode, deleteArc, existingPortInst; }; 

    /** the action to be taken */                   private RouteElementAction action;
    /** the Cell in which to take the action */     private Cell cell;
    /** If action has been done */                  private boolean done;
    /** Whether or not to highlight */              private boolean showHighlight;


    /**
     * Private Constructor
     * @param action the action this RouteElementAction will do.
     */
    protected RouteElement(RouteElementAction action, Cell cell) {
        this.action = action;
        this.cell = cell;
        this.showHighlight = true;
        this.done = false;
    }

    // ---------------------------- Field Access Methods -------------------------

    /** see if action has been done */
    public boolean isDone() { return done; }

    /** set done to true to indication action has been done */
    public void setDone() { done = true; }

    /** get RouteElementAction */
    public RouteElementAction getAction() { return action; }

    /** Return the cell in which this RouteElement will do it's action */
    public Cell getCell() { return cell; }

    /** Get show highlight property */
    public boolean isShowHighlight() { return showHighlight; }

    /** Set show highlight property */
    public void setShowHighlight(boolean b) { showHighlight = b; }

    // --------------------------- Abstract Methods -------------------------------

    /** Return string decribing the RouteElement */
    public abstract String toString();


    /**
     * Get connecting point of node for arc to connect to. If this was
     * not specified earlier, this returns the center of the portInst.
     * Note that this will return null for a newNode that has not yet been
     * created, as it does not yet have a portInst.
     * @return the connecting port for an arc, or null if none exists yet.
     */
/*
    public Point2D getConnPoint() {
        if (action == RouteElementAction.newNode) {
            if (connPoint != null) return connPoint;
            if (done) {
                Rectangle2D bounds = getPortInst().getBounds();
                return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
            }
        }
        if (action == RouteElementAction.existingPortInst) {
            if (connPoint != null) return connPoint;
            Rectangle2D bounds = getPortInst().getBounds();
            return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
        }
        return null;
    }
*/

    /**
     * Perform the action specified by RouteElementAction <i>action</i>.
     * Note that this method performs database editing, and should only
     * be called from within a Job.
     * @return the object created, or null if deleted or nothing done.
     */
    public abstract ElectricObject doAction();

    /**
     * Adds RouteElement to highlights
     */
    public abstract void addHighlightArea(Highlighter highlighter);
}
