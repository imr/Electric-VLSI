/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ClickDragZoomListener.java
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.MenuCommands;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.routing.InteractiveRouter;
import com.sun.electric.tool.routing.SimpleWirer;

import javax.swing.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.*;
import java.util.List;
import java.util.Iterator;

/**
 * Handles Selection, Zooming, and Wiring.
 * <p>The Left Mouse Button handles Selection and Moving
 * <p>The Right Mouse Button handles Zooming and Wiring
 * <p>The Mouse Wheel handles panning
 *
 * User: gainsley
 * Date: Feb 19, 2004
 * Time: 2:53:33 PM
 */
public class ClickZoomWireListener
    implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener, 
        ActionListener
{
    public static ClickZoomWireListener theOne = new ClickZoomWireListener();

    private static final boolean debug = false; /* for debugging */
    private static final long dragDelayMillis = 200; /* drag start delay in milliseconds */
    private static final long zoomInDelayMillis = 100; /* zoom in delay in milliseconds */

    private int clickX, clickY;                 /* last mouse pressed coords in screen space */
    private int dbMoveStartX, dbMoveStartY;     /* left mouse pressed coords for move in database space */
    private Mode modeLeft = Mode.none;          /* left mouse button context mode */
    private Mode modeRight = Mode.none;         /* right mouse button context mode */
    private boolean specialSelect = false;      /* if hard-to-select objects are to be selected */
    private boolean invertSelection = false;    /* if invert selection */
    private List underCursor = null;            /* objects in popupmenu */
    private String lastPopupMenu = null;        /* last used popupmenu */
    private long leftMousePressedTimeStamp;     /* log of last left mouse pressed time */
    private long rightMousePressedTimeStamp;    /* log of last left mouse pressed time */

    // wiring stuff
    private InteractiveRouter router;           /* router used to connect objects */
    private ElectricObject startObj;            /* object routing from */
    private ElectricObject endObj;              /* object routing to */

    /** Class Mode lets us set a common mode over several types of events,
     *  letting initial events (like a right mouse click) set the context for
     *  later events (such as pressing the CTRL button).
     */
    private static class Mode {
        private final String name;

        public Mode(String name) { this.name = name; }
        public String toString() { return name; }

        public static final Mode none = new Mode("none"); // no context
        public static final Mode move = new Mode("move"); // moving objects
        public static final Mode stickyMove = new Mode("stickyMove"); // second move mode
        public static final Mode drawBox = new Mode("drawBox"); // drawing a box
        public static final Mode zoomBox = new Mode("zoomBox"); // drawing a box to zoom to
        public static final Mode zoomIn = new Mode("zoomIn"); // zoom in mode
        public static final Mode zoomOut = new Mode("zoomOut"); // zoom in mode
        public static final Mode selectBox = new Mode("selectBox"); // box for selection
        public static final Mode wiringConnect = new Mode("wiring"); // drawing a wire between two objects
        public static final Mode wiringFind = new Mode("wiringFind"); // drawing wire with unknown end point
        public static final Mode stickyWiring = new Mode("stickyWiring"); // sticky mode wiring
    }

    /** Constructor is private */
    private ClickZoomWireListener() {
        router = new SimpleWirer();
    }

    /** Set ClickZoomWireListener to include hard to select objects */
    public void setSpecialSelect() { specialSelect = true; }

    /** Set ClickZoomWireListener to exclude hard to select objects */
    public void clearSpecialSelect() { specialSelect = false; }

    /**
     * Returns state of 'stickyMove'.
     * If sticky move is true, after the user clicks and drags to
     * move an object, the user can release the mouse button and the
     * object will continue to move with the mouse.  Clicking the
     * select mouse key again will place the object.
     * If sticky move is false, the user must hold and drag to move
     * the object.  Letting go of the select mouse key will place the
     * object.  This is the C-Electric style.
     * @return state of preference 'stickyMove'
     */
    public boolean getStickyMove() {
        // for now just return true.
        // TODO: make it a preference
        return true;
    }

    public void setRouter(InteractiveRouter router) {
        this.router = router;
    }

    /**
     * Returns state of 'stickyWiring'.
     * If sticky wiring is true, after the user clicks and drags to
     * draw a wire, the user can release the mouse button and the UI
     * will remain in wire-draw mode.  Click the mouse button again
     * will draw the wire.
     * If sticky wiring is false, the user must hold and drag to
     * draw the tentative wire, and the wire gets drawn when the
     * user releases the mouse button.  This is C-Electric style.
     * @return state of preference 'stickyWiring'
     */
    public boolean getStickyWiring() {
        // for now just return true
        // TODO: make it a preference
        return true;
    }

    /** Handle mouse press events.
     * <p>Left Mouse Click: Select
     * <p>Left Mouse Drag: Move Objects (or select area if not on object)
     * <p>Left Mouse Double-Click: Get Info
     * <p>CTRL + Left Mouse Click: Cycle through select
     * <p>SHIFT + Left Mouse Click: invert selection
     * <p>Right Mouse Click/Drag: Connect wire
     * <p>CTRL + Right Mouse Click: zoom out
     * <p>CTRL + Right Mouse Drag: zoom in
     * <p>SHIFT + Right Mouse Click:
     * <p>SHIFT + Right Mouse Drag:
     * <p>CTRL + SHIFT + Right Mouse Click: draw box
     * @param evt the MouseEvent
     * */
    public void mousePressed(MouseEvent evt) {

        if (debug) System.out.println("  ["+modeLeft+","+modeRight+"] "+evt.paramString());

        long currentTime = System.currentTimeMillis();

        EditWindow wnd = (EditWindow)evt.getSource();
        Cell cell = wnd.getCell();
        if (cell == null) return;
        clickX = evt.getX();
        clickY = evt.getY();
        Point2D dbClick = wnd.screenToDatabase(clickX, clickY);

        boolean another = (evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;
        invertSelection = (evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) != 0;

        // ===== left mouse clicks ======

        if (evt.getButton() == MouseEvent.BUTTON1) {

            // if doing sticky move place objects now

            if (modeLeft == Mode.stickyMove) {
                // moving objects
                if (another)
                    dbClick = convertToOrthogonal(new Point2D.Double(dbMoveStartX, dbMoveStartY), dbClick);
                Point2D dbDelta = new Point((int)dbClick.getX() - dbMoveStartX, (int)dbClick.getY() - dbMoveStartY);
                EditWindow.gridAlign(dbDelta);
                if (dbDelta.getX() != 0 || dbDelta.getY() != 0) {
                    Highlight.setHighlightOffset(0, 0);
                    CircuitChanges.manyMove(dbDelta.getX(), dbDelta.getY(), wnd);
                    wnd.repaintContents();
                }
                modeLeft = Mode.none;
                return;
            }

            // new time stamp must occur after checking for sticky move
            leftMousePressedTimeStamp = currentTime;

            // ----- double-click responses -----

            if (evt.getClickCount() == 2) {
                /* if CTRL is being held, user wants to cycle through what's
                under the cursor--pop up menu to let them select */
                /*
                if (another) {
                    Rectangle2D bounds = new Rectangle2D.Double(clickX, clickY, 0, 0);
                    underCursor = Highlight.findAllInArea(cell, false, another, true, specialSelect, true, bounds, wnd);
                    JPopupMenu popup = selectPopupMenu(underCursor);
                    popup.show(wnd, clickX, clickY);
                    return;
                } */
                /* if no modifiers, do "get info" */
                if (!another && !invertSelection) {
                    if (Highlight.getNumHighlights() >= 1) {
                        MenuCommands.getInfoCommand();
                        return;
                    }
                }
            }

            // ----- single click responses -----

            // if already over highlighted object, move it
            if (!another && !invertSelection && Highlight.overHighlighted(wnd, clickX, clickY)) {
                // over something, user may want to move objects
                dbMoveStartX = (int)dbClick.getX();
                dbMoveStartY = (int)dbClick.getY();
                modeLeft = Mode.move;
            } else {
                // findObject handles cycling through objects (another)
                // and inverting selection (invertSelection)
                // and selection special objects (specialSelection)
                int numFound = Highlight.findObject(dbClick, wnd, false, another, invertSelection, true, false, specialSelect, true);
                if (numFound == 0) {
                    // not over anything: drag out a selection rectangle
                    wnd.setStartDrag(clickX, clickY);
                    wnd.setEndDrag(clickX, clickY);
                    wnd.setDoingAreaDrag();
                    modeLeft = Mode.selectBox;
                } else {
                    // over something, user may want to move objects
                    dbMoveStartX = (int)dbClick.getX();
                    dbMoveStartY = (int)dbClick.getY();
                    modeLeft = Mode.move;
                }
            }
            return;
        }

        // ===== right mouse clicks =====

        if (evt.getButton() == MouseEvent.BUTTON3) {

            rightMousePressedTimeStamp = currentTime;

            // draw possible wire connection
            // note that SHIFT (invertSelection) can be held when drawing wires, this is
            // because users tend to hold shift to select things to wire
            // and then hit Right-Click to wire them (while still holding
            // shift).
            if (!another) {
                Iterator hIt = Highlight.getHighlights();
                // if already 2 objects, wire them up
                if (Highlight.getNumHighlights() == 2) {
                    Highlight h1 = (Highlight)hIt.next();
                    Highlight h2 = (Highlight)hIt.next();
                    if (h1.getType() == Highlight.Type.EOBJ && h2.getType() == Highlight.Type.EOBJ) {
                        modeRight = Mode.wiringConnect;
                        startObj = h1.getElectricObject();
                        endObj = h2.getElectricObject();
                        EditWindow.gridAlign(dbClick);
                        router.startInteractiveRoute();
                        router.highlightRoute(wnd, h1.getElectricObject(), h2.getElectricObject(), dbClick);
                        return;
                    }
                }
                // if one object, put into wire find mode
                // which will draw possible wire route.
                if (Highlight.getNumHighlights() == 1) {
                    Highlight h1 = (Highlight)hIt.next();
                    if (h1.getType() == Highlight.Type.EOBJ) {
                        modeRight = Mode.wiringFind;
                        startObj = h1.getElectricObject();
                        EditWindow.gridAlign(dbClick);
                        router.startInteractiveRoute();
                        router.highlightRoute(wnd, h1.getElectricObject(), null, dbClick);
                        return;
                    }
                }
                System.out.println("Must start new arc from one node or arc; or wire two node/arcs together");
                return;
            }
            // drawing some sort of box
            // clear current highlights first
            wnd.setStartDrag(clickX, clickY);
            wnd.setEndDrag(clickX, clickY);
            wnd.setDoingAreaDrag();
            // zoom out and zoom to box mode
            if (!invertSelection && another) {
                // A single click zooms out, but a drag box zooms to box
                // The distinction is if the user starts dragging a box,
                // which we check for in mouseDragged after a set time delay
                modeRight = Mode.zoomOut;
            }
            // draw box
            if (another && invertSelection) {
                // the box we are going to draw is a highlight
                Highlight.clear();
                modeRight = Mode.drawBox;
            }
        }

    }

    /** Handle mouse dragged event.
     * <p>Left Mouse Drag: Move Objects (or select area if not on object)
     * <p>Right Mouse Click/Drag: Connect wire
     * <p>Right Mouse Drag + (later) CTRL: Connect wire in space (ignore objects)
     * <p>SHIFT + Right Mouse Drag: zoom box
     * <p>CTRL + Right Mouse Drag: draw box
     * @param evt the MouseEvent
     */
    public void mouseDragged(MouseEvent evt) {

        if (debug) System.out.println("  ["+modeLeft+","+modeRight+"] "+evt.paramString());
        long currentTime = System.currentTimeMillis();

        EditWindow wnd = (EditWindow)evt.getSource();
        Cell cell = wnd.getCell();
        if (cell == null) return;

        int mouseX = evt.getX();
        int mouseY = evt.getY();
        Point2D dbMouse = wnd.screenToDatabase(mouseX, mouseY);

        boolean another = (evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;

        // ===== Left mouse drags =====

        if ((evt.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK) {

            if (modeLeft == Mode.selectBox) {
                // select objects in box
                wnd.setEndDrag(mouseX, mouseY);
                wnd.repaint();
            }
            if (modeLeft == Mode.move || modeLeft == Mode.stickyMove) {
                // moving objects
                // ignore moving objects drag if not after specified delay
                // this prevents accidental movements on user clicks
                if ((currentTime - leftMousePressedTimeStamp) < dragDelayMillis) return;
                // if CTRL held, can only move orthogonally
                if (another)
                    dbMouse = convertToOrthogonal(new Point2D.Double(dbMoveStartX, dbMoveStartY), dbMouse);
                // relocate highlight to under mouse
                Point2D dbDelta = new Point((int)dbMouse.getX() - dbMoveStartX, (int)dbMouse.getY() - dbMoveStartY);
                EditWindow.gridAlign(dbDelta);              // align to grid
                Point2D screenDelta = wnd.deltaDatabaseToScreen((int)dbDelta.getX(), (int)dbDelta.getY());
                Highlight.setHighlightOffset((int)screenDelta.getX(), (int)screenDelta.getY());
                wnd.repaint();
            }
        }

        // ===== Right mouse drags =====

        if ((evt.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) == MouseEvent.BUTTON3_DOWN_MASK) {

            if (modeRight == Mode.zoomOut) {
                // switch to zoomBox mode if the user is really dragging a box
                // otherwise, we zoom out after specified delay
                if ((currentTime - rightMousePressedTimeStamp) > zoomInDelayMillis)
                    modeRight = Mode.zoomBox;
            }
            if (modeRight == Mode.drawBox || modeRight == Mode.zoomBox) {
                // draw a box
                wnd.setEndDrag(mouseX, mouseY);
                wnd.repaint();
            }
            if (modeRight == Mode.wiringFind || modeRight == Mode.stickyWiring) {
                // see if anything under the pointer
                int numFound = Highlight.findObject(dbMouse, wnd, false, false, false, true, false, specialSelect, true);
                if (numFound == 0) {
                    // not over anything, nothing to connect to
                    EditWindow.gridAlign(dbMouse);
                    router.highlightRoute(wnd, startObj, null, dbMouse);
                } else {
                    Iterator hIt = Highlight.getHighlights();
                    Highlight h2 = (Highlight)hIt.next();
                    EditWindow.gridAlign(dbMouse);
                    router.highlightRoute(wnd, startObj, h2.getElectricObject(), dbMouse);
                }
            }
            if (modeRight == Mode.wiringConnect) {
                EditWindow.gridAlign(dbMouse);
                router.highlightRoute(wnd, startObj, endObj, dbMouse);
            }
        }
        wnd.repaint();
    }

    /** Handle mouse released event
     *
     * @param evt the MouseEvent
     */
    public void mouseReleased(MouseEvent evt) {

        if (debug) System.out.println("  ["+modeLeft+","+modeRight+"] "+evt.paramString());

        EditWindow wnd = (EditWindow)evt.getSource();
        Cell cell = wnd.getCell();
        if (cell == null) return;
        // add back in offset
        int releaseX = evt.getX();
        int releaseY = evt.getY();
        Point2D dbMouse = wnd.screenToDatabase(releaseX, releaseY);
        boolean another = (evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;

        // ===== Left Mouse Release =====

        if (evt.getButton() == MouseEvent.BUTTON1) {

            // if 'stickyMove' is true and we are moving stuff, ignore mouse release
            if (getStickyMove() && (modeLeft == Mode.move)) {
                // only do so after drag delay
                if ((System.currentTimeMillis() - leftMousePressedTimeStamp) < dragDelayMillis)
                    modeLeft = Mode.none;       // user left mouse button single click
                else
                    modeLeft = Mode.stickyMove; // user moving stuff in sticky mode
            } else {

                if (modeLeft == Mode.selectBox) {
                    // select all in box
                    Point2D start = wnd.screenToDatabase((int)wnd.getStartDrag().getX(), (int)wnd.getStartDrag().getY());
                    Point2D end = wnd.screenToDatabase((int)wnd.getEndDrag().getX(), (int)wnd.getEndDrag().getY());
                    double minSelX = Math.min(start.getX(), end.getX());
                    double maxSelX = Math.max(start.getX(), end.getX());
                    double minSelY = Math.min(start.getY(), end.getY());
                    double maxSelY = Math.max(start.getY(), end.getY());
                    if (!invertSelection)
                        Highlight.clear();
                    Highlight.selectArea(wnd, minSelX, maxSelX, minSelY, maxSelY, invertSelection, specialSelect);
                    Highlight.finished();
                    wnd.clearDoingAreaDrag();
                    wnd.repaint();
                }
                if (modeLeft == Mode.move || modeLeft == Mode.stickyMove) {
                    // moving objects
                    if (another)
                        dbMouse = convertToOrthogonal(new Point2D.Double(dbMoveStartX, dbMoveStartY), dbMouse);
                    Point2D dbDelta = new Point((int)dbMouse.getX() - dbMoveStartX, (int)dbMouse.getY() - dbMoveStartY);
                    EditWindow.gridAlign(dbDelta);
                    if (dbDelta.getX() != 0 || dbDelta.getY() != 0) {
                        Highlight.setHighlightOffset(0, 0);
                        CircuitChanges.manyMove(dbDelta.getX(), dbDelta.getY(), wnd);
                        wnd.repaintContents();
                    }
                }
                modeLeft = Mode.none;
            }
        }

        // ===== Right Mouse Release =====

        if (evt.getButton() == MouseEvent.BUTTON3) {

            if (modeRight == Mode.zoomIn) {
                // zoom in by a factor of two
                double scale = wnd.getScale();
                wnd.setScale(scale * 2);
                wnd.clearDoingAreaDrag();
                wnd.repaintContents();
            }
            if (modeRight == Mode.zoomOut) {
                // zoom out by a factor of two
                double scale = wnd.getScale();
                wnd.setScale(scale / 2);
                wnd.clearDoingAreaDrag();
                wnd.repaintContents();
            }
            if (modeRight == Mode.drawBox || modeRight == Mode.zoomBox) {
                // drawing boxes
                Point2D start = wnd.screenToDatabase((int)wnd.getStartDrag().getX(), (int)wnd.getStartDrag().getY());
                Point2D end = wnd.screenToDatabase((int)wnd.getEndDrag().getX(), (int)wnd.getEndDrag().getY());
                double minSelX = Math.min(start.getX(), end.getX());
                double maxSelX = Math.max(start.getX(), end.getX());
                double minSelY = Math.min(start.getY(), end.getY());
                double maxSelY = Math.max(start.getY(), end.getY());

                if (modeRight == Mode.drawBox) {
                    // just draw a highlight box
                    Highlight.addArea(new Rectangle2D.Double(minSelX, minSelY, maxSelX-minSelX, maxSelY-minSelY), cell);
                }
                if (modeRight == Mode.zoomBox) {
                    // zoom to box: focus on box
                    Rectangle2D bounds = new Rectangle2D.Double(minSelX, minSelY, maxSelX-minSelX, maxSelY-minSelY);
                    // don't zoom a box dimension is less than 4 (heuristic to prevent unintended zoom)
                    // note these dimensions are in lambda, the db unit, not screen pixels
                    if (bounds.getHeight() > 4 && bounds.getWidth() > 4)
                        wnd.focusScreen(bounds);
                    else {
                        // modeRight == Mode.zoomOut
                        // if not zoomBox, then user meant to zoomOut
                        double scale = wnd.getScale();
                        wnd.setScale(scale / 2);
                        wnd.clearDoingAreaDrag();
                        wnd.repaintContents();
                    }

                }
                Highlight.finished();
                wnd.clearDoingAreaDrag();
                wnd.repaint();
            }
            if (modeRight == Mode.wiringFind || modeRight == Mode.stickyWiring) {
                // see if anything under the pointer
                int numFound = Highlight.findObject(dbMouse, wnd, false, false, false, true, false, specialSelect, true);
                if (numFound == 0) {
                    // not over anything, nothing to connect to
                    EditWindow.gridAlign(dbMouse);
                    router.makeRoute(wnd, startObj, null, dbMouse);
                } else {
                    // connect objects
                    Iterator hIt = Highlight.getHighlights();
                    Highlight h2 = (Highlight)hIt.next();
                    EditWindow.gridAlign(dbMouse);
                    router.makeRoute(wnd, startObj, h2.getElectricObject(), dbMouse);
                }
            }
            if (modeRight == Mode.wiringConnect) {
                EditWindow.gridAlign(dbMouse);
                router.makeRoute(wnd, startObj, endObj, dbMouse);
            }
            modeRight = Mode.none;
        }
    }

    /**
     * Use to track sticky move of objects
     * @param evt the MouseEvent
     */
    public void mouseMoved(MouseEvent evt) {

        //if (debug) System.out.println("  ["+modeLeft+","+modeRight+"] "+evt.paramString());

        EditWindow wnd = (EditWindow)evt.getSource();
        Cell cell = wnd.getCell();
        if (cell == null) return;

        int mouseX = evt.getX();
        int mouseY = evt.getY();

        boolean another = (evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;

        if (modeLeft == Mode.stickyMove) {
            Point2D dbMouse = wnd.screenToDatabase(mouseX, mouseY);
            if (another)
                dbMouse = convertToOrthogonal(new Point2D.Double(dbMoveStartX, dbMoveStartY), dbMouse);
            Point2D dbDelta = new Point((int)dbMouse.getX() - dbMoveStartX, (int)dbMouse.getY() - dbMoveStartY);
            EditWindow.gridAlign(dbDelta);
            Point2D screenDelta = wnd.deltaDatabaseToScreen((int)dbDelta.getX(), (int)dbDelta.getY());
            Highlight.setHighlightOffset((int)screenDelta.getX(), (int)screenDelta.getY());
            wnd.repaint();
        }
    }

    public void mouseClicked(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void mouseEntered(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void mouseExited(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /** Mouse Wheel Events are used for panning
     * Wheel Forward: scroll up
     * Wheel Back: scroll down
     * SHIFT + Wheel Forward: scroll right
     * SHIFT + Wheel Back: scroll left
     * @param evt the MouseWheelEvent
     */
    public void mouseWheelMoved(MouseWheelEvent evt) {

        if (debug) System.out.println("  "+evt.paramString());

        EditWindow wnd = (EditWindow)evt.getSource();
        Cell cell = wnd.getCell();
        if (cell == null) return;

        boolean sideways = (evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) != 0;
        boolean sideways2 = (evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;

        int rotation = evt.getWheelRotation();
        Point2D wndOffset = wnd.getOffset();
        Point2D newOffset = wndOffset;

        // factor in multiplier
        int mult = (int)(80/wnd.getScale());
        if (mult <= 0) mult = 2;
        if (wnd.getScale() > 70) mult = 1; // we're really zoomed in

        // scroll left right if sideways
        if (sideways || sideways2) {
            // scroll right if roll foward (pos)
            // scroll left if roll back (neg)
            newOffset = new Point2D.Double(wndOffset.getX() - mult*rotation, wndOffset.getY());
        } else {
            // scroll up if roll forward (pos)
            // scroll down if roll back (neg)
            newOffset = new Point2D.Double(wndOffset.getX(), wndOffset.getY() - mult*rotation);
        }
        wnd.setOffset(newOffset);
        wnd.repaintContents();
    }

    /** Key pressed event
     * Delete or Move selected objects
     * @param evt the KeyEvent
     */
    public void keyPressed(KeyEvent evt) {
        int chr = evt.getKeyCode();
        EditWindow wnd = (EditWindow)evt.getSource();
        Cell cell = wnd.getCell();
        if (cell == null) return;

        // delete highlighted objects
        if (chr == KeyEvent.VK_DELETE || chr == KeyEvent.VK_BACK_SPACE) {
            CircuitChanges.deleteSelected();
        }
        // move stuff around
        else if (chr == KeyEvent.VK_LEFT) {
            moveSelected(evt, -1, 0);
        } else if (chr == KeyEvent.VK_RIGHT) {
            moveSelected(evt, 1, 0);
        } else if (chr == KeyEvent.VK_UP) {
            moveSelected(evt, 0, 1);
        } else if (chr == KeyEvent.VK_DOWN) {
            moveSelected(evt, 0, -1);
        }
        // cancel current mode
        else if (chr == KeyEvent.VK_ESCAPE) {
            if (modeRight == Mode.wiringConnect || modeRight == Mode.wiringFind ||
                modeRight == Mode.stickyWiring)
                router.cancelInteractiveRoute();
            modeLeft = Mode.none;
            modeRight = Mode.none;
            Highlight.setHighlightOffset(0, 0);
            wnd.repaint();
        }
    }

    public void keyReleased(KeyEvent evt) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void keyTyped(KeyEvent evt) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    // ********************************* Moving Stuff ********************************

    /** Move selected object(s) via keystroke
     *
     * @param evt the event of the keystroke
     * @param dX amount to move in X in lambda
     * @param dY amount to move in Y in lambda
     */
    public void moveSelected(KeyEvent evt, double dX, double dY) {
        // scale distance according to arrow motion
        EditWindow wnd = (EditWindow)evt.getSource();
		double arrowDistance = ToolBar.getArrowDistance();
		dX *= arrowDistance;
		dY *= arrowDistance;
		int scale = User.getDefGridXBoldFrequency();
		if (evt.isShiftDown()) { dX *= scale;   dY *= scale; }
		if (evt.isControlDown()) { dX *= scale;   dY *= scale; }
		Highlight.setHighlightOffset(0, 0);
		CircuitChanges.manyMove(dX, dY, wnd);
		wnd.repaintContents();
	}

    /**
     * Convert the mousePoint to be orthogonal to the startPoint.
     * Chooses direction which is orthogonally farther from startPoint
     * @param startPoint the reference point
     * @param mousePoint the mouse point
     * @return a new point orthogonal to startPoint
     */
    private Point2D convertToOrthogonal(Point2D startPoint, Point2D mousePoint) {
        // move in direction that is farther
        double xdist, ydist;
        xdist = Math.abs(mousePoint.getX() - startPoint.getX());
        ydist = Math.abs(mousePoint.getY() - startPoint.getY());
        if (ydist > xdist)
            return new Point2D.Double(startPoint.getX(), mousePoint.getY());
        else
            return new Point2D.Double(mousePoint.getX(), startPoint.getY());
    }

    // ********************************* Wiring Stuff ********************************



    // ********************************* Popup Menus *********************************

    /**
     * Popup menu when user is cycling through objects under pointer
     * @param objects list of objects to put in menu
     * @return the popup menu
     */
    public JPopupMenu selectPopupMenu(List objects) {
        JPopupMenu popup = new JPopupMenu("Choose One");
        JMenuItem m;
        for (Iterator it = objects.iterator(); it.hasNext(); ) {
            Highlight obj = (Highlight)it.next();
            m = new JMenuItem(obj.toString()); m.addActionListener(this); popup.add(m);
        }
        lastPopupMenu = "Select";
        return popup;
    }


    /** Select object or Wire to object, depending upon popup menu used */
    public void actionPerformed(ActionEvent e) {
        JMenuItem source = (JMenuItem)e.getSource();
    }

}
