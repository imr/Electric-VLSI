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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.RTBounds;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Client;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.routing.InteractiveRouter;
import com.sun.electric.tool.routing.SimpleWirer;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.menus.EditMenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

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
    private static Preferences prefs = Preferences.userNodeForPackage(ClickZoomWireListener.class);
    private long cancelMoveDelayMillis; /* cancel move delay in milliseconds */
    private long zoomInDelayMillis; /* zoom in delay in milliseconds */
//    private static boolean useFatWiringMode;
    private boolean interactiveDRCDrag;

    private static final boolean debug = false; /* for debugging */

    public static ClickZoomWireListener theOne = new ClickZoomWireListener();

    private int clickX, clickY;                 /* current mouse pressed coords in screen space */
    private int lastX, lastY;                   /* last mouse pressed coords in screen space (for panning) */
    private Cell startCell;
    private double dbMoveStartX, dbMoveStartY;     /* left mouse pressed coords for move in database space */
    private double lastdbMouseX, lastdbMouseY;     /* last location of mouse */
    private Mode modeLeft = Mode.none;          /* left mouse button context mode */
    private Mode modeRight = Mode.none;         /* right mouse button context mode */
    private boolean specialSelect = false;      /* if hard-to-select objects are to be selected */
    private boolean invertSelection = false;    /* if invert selection */
    private boolean another;
    //private List underCursor = null;            /* objects in popupmenu */
    //private String lastPopupMenu = null;        /* last used popupmenu */
    private long leftMousePressedTimeStamp;     /* log of last left mouse pressed time */
    private long rightMousePressedTimeStamp;    /* log of last left mouse pressed time */
    private ElectricObject wiringTarget;        /* last highlight user switched to possibly wire to */

    // wiring stuff
    private InteractiveRouter router;           /* router used to connect objects */
    private ElectricObject startObj;            /* object routing from */
    private ElectricObject endObj;              /* object routing to */
    private ArcProto currentArcWhenWiringPressed; /* current arc proto when mouse pressed to draw wire: later actions of this tool may change current arc */

    private int mouseX, mouseY;                 /* last known location of mouse */
    private Highlight moveDelta;               /* highlight to display move delta */
    private Highlight moveDRC;                 /* highlight to display design rules during a move */

    private EventListener oldListener;          /* used when swtiching back to old listener */

    // mac stuff
    private static final boolean isMac = Client.isOSMac();

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
        public static final Mode zoomBoxSingleShot = new Mode("zoomBoxSingleShot"); // drawing a box to zoom to
        public static final Mode zoomIn = new Mode("zoomIn"); // zoom in mode
        public static final Mode zoomOut = new Mode("zoomOut"); // zoom in mode
        public static final Mode selectBox = new Mode("selectBox"); // box for selection
        public static final Mode wiringConnect = new Mode("wiring"); // drawing a wire between two objects
        public static final Mode wiringFind = new Mode("wiringFind"); // drawing wire with unknown end point
        public static final Mode wiringToSpace = new Mode("wiringToSpace"); // only draw wire to space, not objects
        public static final Mode stickyWiring = new Mode("stickyWiring"); // sticky mode wiring
    }

    /** Constructor is private */
    private ClickZoomWireListener() {
        readPrefs();
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
        return false;
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

    /**
     * Return the last known location of the mouse. Note that these are
     * screen coords, and are in the coordinate system of the current container(?).
     * @return a Point2D containing the last mouse coords.
     */
    public Point2D getLastMouse() { return new Point2D.Double(mouseX, mouseY); }

    /**
     * Sets the mode to zoom box for the next right click only.
     */
    public void zoomBoxSingleShot(EventListener oldListener) {
        modeRight = Mode.zoomBoxSingleShot;
        modeLeft = Mode.zoomBoxSingleShot;
        this.oldListener = oldListener;
    }

    /**
     * See if event is a left mouse click.  Platform independent.
     */
    private boolean isLeftMouse(MouseEvent evt) {
        if (isMac) {
            if (!evt.isMetaDown()) {
                if ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK)
                    return true;
            }
        } else {
            if ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK)
                return true;
        }
        return false;
    }

    /**
     * See if event is a right mouse click.  Platform independent.
     * One-button macs: Command + click == right mouse click.
     */
    public static boolean isRightMouse(InputEvent evt) {
        if (isMac) {
            if (evt.isMetaDown()) {
                if ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK)
                    return true;
            }
            if ((evt.getModifiers() & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK)
                return true;
        } else {
            if ((evt.getModifiers() & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK)
                return true;
        }
        return false;
    }

    /**
     * See if event is a middle mouse click.  Platform independent.
     */
    public static boolean isMiddleMouse(InputEvent evt) {
        if ((evt.getModifiers() & MouseEvent.BUTTON2_MASK) == MouseEvent.BUTTON2_MASK)
            return true;
        return false;
    }

    /** Handle mouse press events.
     * <p>Left Mouse Click: Select
     * <p>Left Mouse Drag: Move Objects (or select area if not on object)
     * <p>Left Mouse Double-Click: Get Info
     * <p>CTRL + Left Mouse Click: Cycle through select
     * <p>SHIFT + Left Mouse Click: invert selection
     * <p>Right Mouse Click/Drag: Connect wire
     * <p>SHIFT + Right Mouse Click: zoom out
     * <p>SHIFT + Right Mouse Drag: zoom in
     * <p>CTRL + SHIFT + Right Mouse Click: draw box
     * @param evt the MouseEvent
     * */
    public void mousePressed(MouseEvent evt) {

        if (debug) System.out.println("  ["+modeLeft+","+modeRight+"] "+evt.paramString());

        long currentTime = System.currentTimeMillis();

		if (evt.getSource() instanceof EditWindow)
		{
			EditWindow wnd = (EditWindow)evt.getSource();
            Highlighter highlighter = wnd.getHighlighter();
	        startCell = wnd.getCell();
	        if (startCell == null) return;
	        clickX = evt.getX();
	        clickY = evt.getY();
	        Point2D dbClick = wnd.screenToDatabase(clickX, clickY);
	        lastdbMouseX = dbClick.getX();
	        lastdbMouseY = dbClick.getY();

	        boolean ctrlPressed = (evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;
	        invertSelection = (evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) != 0;
	        specialSelect = ToolBar.isSelectSpecial();

	        // ===== right mouse clicks =====

	        if (isRightMouse(evt)) {

	            rightMousePressedTimeStamp = currentTime;

                if (modeRight == Mode.zoomBoxSingleShot) {
                    // We will zoom to box
                    wnd.setStartDrag(clickX, clickY);
                    wnd.setEndDrag(clickX, clickY);
                    wnd.setDoingAreaDrag();
                    return;
                }

	            // draw possible wire connection
	            if (!invertSelection) {
                    // ignore anything that can't have a wire drawn to it
                    // (everything except nodes, ports, and arcs)
                    List<Highlight> highlights = new ArrayList<Highlight>();
                    for (Highlight h : highlighter.getHighlights()) {
                        if (h.isHighlightEOBJ()) {
                            ElectricObject eobj = h.getElectricObject();
                            if (eobj instanceof PortInst || eobj instanceof NodeInst || eobj instanceof ArcInst)
                                highlights.add(h);
                        }
                    }
                    Iterator<Highlight> hIt = highlights.iterator();
	                // if already 2 objects, wire them up
	                if (highlights.size() == 2) {
	                    Highlight h1 = hIt.next();
	                    Highlight h2 = hIt.next();
                        ElectricObject eobj1 = h1.getElectricObject();
                        ElectricObject eobj2 = h2.getElectricObject();
	                    if (eobj1 != null && eobj2 != null) {
	                        modeRight = Mode.wiringConnect;
	                        wiringTarget = null;
	                        startObj = h1.getElectricObject();
	                        endObj = h2.getElectricObject();
                            currentArcWhenWiringPressed = User.getUserTool().getCurrentArcProto();
	                        EditWindow.gridAlign(dbClick);
	                        router.highlightRoute(wnd, startCell, h1.getElectricObject(), h2.getElectricObject(), dbClick);
	                        return;
	                    }
	                }
	                // if one object, put into wire find mode
	                // which will draw possible wire route.
	                if (highlights.size() == 1) {
	                    Highlight h1 = hIt.next();
                        ElectricObject eobj1 = h1.getElectricObject();
	                    if (eobj1 != null) {
	                        modeRight = Mode.wiringFind;
                            endObj = null;
	                        wiringTarget = null;
	                        startObj = h1.getElectricObject();
                            router.startInteractiveRoute(wnd);
                            // look for stuff under the mouse
                            Highlight h2 = null;
                            if (!ctrlPressed)
                            	h2 = highlighter.findObject(dbClick, wnd, false, false, false, true, false, specialSelect, false);
                            if (h2 == null) {
                                // not over anything, nothing to connect to
                                endObj = null;
                                wiringTarget = null;
                            } else {
//                                Highlight h2 = highlighter.getHighlights().iterator().next();
                                endObj = h2.getElectricObject();
                            }
                            currentArcWhenWiringPressed = User.getUserTool().getCurrentArcProto();
	                        EditWindow.gridAlign(dbClick);
	                        router.highlightRoute(wnd, startCell, h1.getElectricObject(), endObj, dbClick);
	                        return;
	                    }
	                }
	                System.out.println("Must start new arc from one node or arc; or wire two node/arcs together");
                    modeRight = Mode.none;
	                return;
	            }
	            // drawing some sort of box
	            wnd.setStartDrag(clickX, clickY);
	            wnd.setEndDrag(clickX, clickY);
	            wnd.setDoingAreaDrag();
	            // zoom out and zoom to box mode
	            if (invertSelection && !ctrlPressed) {
	                // A single click zooms out, but a drag box zooms to box
	                // The distinction is if the user starts dragging a box,
	                // which we check for in mouseDragged after a set time delay
	                modeRight = Mode.zoomOut;
	            }
	            // draw box
	            if (ctrlPressed && invertSelection) {
	                // the box we are going to draw is a highlight
	                highlighter.clear();
	                modeRight = Mode.drawBox;
	            }
	            return;
	        }

	        // ===== left mouse clicks ======

	        if (isLeftMouse(evt)) {

                if (modeLeft == Mode.zoomBoxSingleShot) {
                    // We will zoom to box
                    wnd.setStartDrag(clickX, clickY);
                    wnd.setEndDrag(clickX, clickY);
                    wnd.setDoingAreaDrag();
                    return;
                }

	            // if doing sticky move place objects now
	            if (modeLeft == Mode.stickyMove) {
	                // moving objects
	                if (ctrlPressed)
	                    dbClick = convertToOrthogonal(new Point2D.Double(dbMoveStartX, dbMoveStartY), dbClick, highlighter);
	                Point2D dbDelta = new Point2D.Double(dbClick.getX() - dbMoveStartX, dbClick.getY() - dbMoveStartY);
	                EditWindow.gridAlign(dbDelta);
	                if (dbDelta.getX() != 0 || dbDelta.getY() != 0) {
	                    highlighter.setHighlightOffset(0, 0);
	                    CircuitChanges.manyMove(dbDelta.getX(), dbDelta.getY());
	                    wnd.fullRepaint();
	                }
	                modeLeft = Mode.none;
	                return;
	            }

	            // new time stamp must occur after checking for sticky move
	            leftMousePressedTimeStamp = evt.getWhen();

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
	                if (!ctrlPressed && !invertSelection) {
	                    if (highlighter.getNumHighlights() >= 1) {
	                        EditMenu.getInfoCommand(true);
	                        return;
	                    }
	                }
	            }

	            // ----- single click responses -----

	            // if toolbar is in select mode, draw box
	            if (ToolBar.getSelectMode() == ToolBar.SelectMode.AREA) {
	                // select area
	                // area selection: just drag out a rectangle
	                wnd.setStartDrag(clickX, clickY);
	                wnd.setEndDrag(clickX, clickY);
	                wnd.setDoingAreaDrag();
	                highlighter.clear();
	                modeLeft = Mode.drawBox;
	                return;
	            }

	            // if already over highlighted object, move it
	            if (!ctrlPressed && !invertSelection && highlighter.overHighlighted(wnd, clickX, clickY) != null) {
	                highlighter.finished();
                    // over something, user may want to move objects
	                dbMoveStartX = dbClick.getX();
	                dbMoveStartY = dbClick.getY();
                    moveDelta = moveDRC = null;
	                modeLeft = Mode.move;
	            } else {
	                // findObject handles cycling through objects (another)
	                // and inverting selection (invertSelection)
	                // and selection special objects (specialSelection)
	                Highlight h = highlighter.findObject(dbClick, wnd, false, ctrlPressed, invertSelection, true, false, specialSelect, true);
	                if (h == null) {
	                    // not over anything: drag out a selection rectangle
	                    wnd.setStartDrag(clickX, clickY);
	                    wnd.setEndDrag(clickX, clickY);
	                    wnd.setDoingAreaDrag();
	                    modeLeft = Mode.selectBox;
	                } else {
	                    // over something, user may want to move objects
	                    dbMoveStartX = dbClick.getX();
	                    dbMoveStartY = dbClick.getY();
                        moveDelta = moveDRC = null;
	                    modeLeft = Mode.move;
	                }
                    mouseOver(dbClick, wnd);
	            }
	            return;
	        }
	        if (isMiddleMouse(evt)) {
                // We will pan the screen
	    		lastX = evt.getX();
	    		lastY = evt.getY();
                return;
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

 		if (evt.getSource() instanceof EditWindow)
		{
			EditWindow wnd = (EditWindow)evt.getSource();
            Highlighter highlighter = wnd.getHighlighter();
	        Cell cell = wnd.getCell();
	        if (cell == null) return;

	        int mouseX = evt.getX();
	        int mouseY = evt.getY();
	        Point2D dbMouse = wnd.screenToDatabase(mouseX, mouseY);
	        lastdbMouseX = (int)dbMouse.getX();
	        lastdbMouseY = (int)dbMouse.getY();

	        boolean ctrlPressed = (evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;
	        specialSelect = ToolBar.isSelectSpecial();

	        // ===== Right mouse drags =====

	        if (isRightMouse(evt)) {

                if (modeRight == Mode.zoomBoxSingleShot) {
                    // We will zoom to box
                    if (!wnd.isDoingAreaDrag()) {
                        wnd.setStartDrag(mouseX, mouseY);
                        wnd.setEndDrag(mouseX, mouseY);
                        wnd.setDoingAreaDrag();
                    }
                    wnd.setEndDrag(mouseX, mouseY);
                }
	            if (modeRight == Mode.zoomOut) {
	                // switch to zoomBox mode if the user is really dragging a box
	                // otherwise, we zoom out after specified delay
	                if ((currentTime - rightMousePressedTimeStamp) > zoomInDelayMillis)
	                    modeRight = Mode.zoomBox;
	            }
	            if (modeRight == Mode.drawBox || modeRight == Mode.zoomBox) {
	                // draw a box
	                wnd.setEndDrag(mouseX, mouseY);
	            }
	            if (modeRight == Mode.wiringFind || modeRight == Mode.stickyWiring) {
	                // see if anything under the pointer
	                Highlight h3 = null;
	                if (!ctrlPressed)
	                	h3 = highlighter.findObject(dbMouse, wnd, false, false, false, true, false, specialSelect, false);
	                if (h3 == null) {
	                    // not over anything, nothing to connect to
	                    EditWindow.gridAlign(dbMouse);
	                    endObj = null;
	                    wiringTarget = null;
	                } else {
	                    // The user can switch between wiring targets under the cursor using a key stroke
	                    // if wiring target non-null, and still under cursor, use that
	                    endObj = null;
	                    if (wiringTarget != null) {
	                        // check if still valid target
	                        EditWindow.gridAlign(dbMouse);
	                        List<Highlight> underCursor = Highlighter.findAllInArea(highlighter, cell, false, true, false, specialSelect, false,
	                            new Rectangle2D.Double(dbMouse.getX(), dbMouse.getY(), 0, 0), wnd);
	                        for (Highlight h : underCursor) {
	                            ElectricObject eobj = h.getElectricObject();
	                            if (eobj == wiringTarget) {
	                                endObj = wiringTarget;
	                                break;
	                            }
	                        }
	                        // wiringTarget is no longer valid, reset it
	                        if (endObj == null) wiringTarget = null;
	                    }
	                    // if target is null, find new target
	                    if (endObj == null) {
	                        Iterator<Highlight> hIt = highlighter.getHighlights().iterator();
	                        if (hIt.hasNext()){
		                        Highlight h2 = hIt.next();
		                        endObj = h2.getElectricObject();
	                        }
	                    }
	                    EditWindow.gridAlign(dbMouse);
	                }
					User.getUserTool().setCurrentArcProto(currentArcWhenWiringPressed);
	                router.highlightRoute(wnd, cell, startObj, endObj, dbMouse);
	                // clear any previous popup cloud
	                /*
	                wnd.clearShowPopupCloud();
	                // popup list of stuff under mouse to connect to if more than one
	                List underCursor = Highlight.findAllInArea(cell, false, true, true, false, specialSelect, false,
	                    new Rectangle2D.Double(dbMouse.getX(), dbMouse.getY(), 0, 0), wnd);
	                if (underCursor.size() > 1) {
	                    ArrayList text = new ArrayList();
	                    ArrayList portList = new ArrayList();
	                    text.add("Connect to:");
	                    int num = 1;
	                    for (int i=0; i<underCursor.size(); i++) {
	                        Highlight h = underCursor.get(i);
	                        ElectricObject obj = h.getElectricObject();
	                        if (num == 10) {
	                            text.add("...too many to display");
	                            break;
	                        }
	                        if (obj instanceof PortInst) {
	                            // only give option to connect to ports
	                            PortInst pi = (PortInst)obj;
	                            String str = "("+num+"): Port "+pi.getPortProto().getName()+" on "+pi.getNodeInst().getName();
	                            text.add(str);
	                            portList.add(pi);
	                            num++;
	                        }
	                    }
	                    if (num > 2) {
	                        // only show if more than one port to connect to under mouse
	                        wnd.setShowPopupCloud(text, new Point2D.Double(mouseX, mouseY));
	                        wiringPopupCloudUp = true;
	                        wiringPopupCloudList = portList;
	                        wiringLastDBMouse = dbMouse;
	                    }
	                } */
	            }
	            if (modeRight == Mode.wiringConnect) {
	                EditWindow.gridAlign(dbMouse);
					User.getUserTool().setCurrentArcProto(currentArcWhenWiringPressed);
	                router.highlightRoute(wnd, cell, startObj, endObj, dbMouse);
	            }
                if (modeRight == Mode.wiringToSpace) {
                    // wire only to point in space
                    EditWindow.gridAlign(dbMouse);
					User.getUserTool().setCurrentArcProto(currentArcWhenWiringPressed);
                    router.highlightRoute(wnd, cell, startObj, null, dbMouse);
                }
	        }

	        // ===== Left mouse drags =====

	        if (isLeftMouse(evt)) {

	            if (modeLeft == Mode.selectBox || modeLeft == Mode.drawBox || modeLeft == Mode.zoomBoxSingleShot) {
	                // select objects in box
	                wnd.setEndDrag(mouseX, mouseY);
	                wnd.repaint();
	            }
	            if (modeLeft == Mode.move || modeLeft == Mode.stickyMove) {
	                // moving objects
	                // if CTRL held, can only move orthogonally
	                if (ctrlPressed)
	                    dbMouse = convertToOrthogonal(new Point2D.Double(dbMoveStartX, dbMoveStartY), dbMouse, highlighter);
	                // relocate highlight to under mouse
	                Point2D dbDelta = new Point2D.Double(dbMouse.getX() - dbMoveStartX, dbMouse.getY() - dbMoveStartY);
	                EditWindow.gridAlign(dbDelta);              // align to grid
	                Point2D screenDelta = wnd.deltaDatabaseToScreen(dbDelta.getX(), dbDelta.getY());
	                highlighter.setHighlightOffset((int)screenDelta.getX(), (int)screenDelta.getY());

	                // detect worst design-rule violation if moving just one object
	                WorstSpacing ws = new WorstSpacing();
	                List<Geometric> selected = highlighter.getHighlightedEObjs(true, true);
	                if (interactiveDRCDrag && selected.size() == 1)
	                {
	                	Geometric g = selected.get(0);
	                	Netlist nl = g.getParent().getNetlist();
	                	if (g instanceof ArcInst)
	                	{
	                		ArcInst ai = (ArcInst)g;
	                		Network net = nl.getNetwork(ai, 0);
            				if (net != null)
            				{
	                			Poly [] polys = ai.getProto().getTechnology().getShapeOfArc(ai);
	                			for(int i=0; i<polys.length; i++)
	                				ws.findWorstSpacing(g, polys[i], net, dbDelta, nl);
            				}
	                	} else
	                	{
	                		NodeInst ni = (NodeInst)g;
	                		if (!ni.isCellInstance())
	                		{
	                			AffineTransform trans = ni.rotateOut();
	                			Poly [] polys = ni.getProto().getTechnology().getShapeOfNode(ni, true, true, null);
	                			for(int i=0; i<polys.length; i++)
	                			{
	                				PortProto pp = polys[i].getPort();
	                				if (pp == null) continue;
	                				PortInst pi = ni.findPortInstFromProto(pp);
	                				if (pi == null) continue;
	                				Network net = nl.getNetwork(pi);
	                				if (net == null) continue;
	                				polys[i].transform(trans);
	                				ws.findWorstSpacing(g, polys[i], net, dbDelta, nl);
	                			}
	                		}
	                	}
	                }

	                // display DRC if known, otherwise distance moved
                    if (moveDelta != null) highlighter.remove(moveDelta);
                    if (moveDRC != null) highlighter.remove(moveDRC);
                    Technology tech = wnd.getCell().getTechnology();
                    String deltaMessage = "Moved (" + TextUtils.formatDistance(dbDelta.getX(), tech) + "," +
                    	TextUtils.formatDistance(dbDelta.getY(), tech) + ")";
            		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
                    StatusBar.setCoordinates(deltaMessage, wf);
                	if (ws.validSpacing())
                	{
	                	// display worst design rule violation
                		boolean tooClose = ws.getSeparation() < ws.getMinSpacing();
                		String message = ws.getOneLayer().getName();
                		if (ws.getOneLayer() != ws.getOtherLayer()) message += " to " + ws.getOtherLayer().getName();
                		message += " spacing is " + TextUtils.formatDistance(ws.getSeparation(), tech);
                		if (tooClose) message = "ERROR! " + message + " MINIMUM IS " + TextUtils.formatDistance(ws.getMinSpacing(), tech);

                		// compute upper-left corner of moved object and place text there
                		Geometric g = selected.get(0);
	                	Poly hPoly;
	                	if (g instanceof NodeInst)
	                	{
	                		hPoly = Highlight.getNodeInstOutline((NodeInst)g);
	                	} else
	                	{
	                		ArcInst ai = (ArcInst)g;
	                		hPoly = ai.makeLambdaPoly(ai.getGridBaseWidth(), Poly.Type.CLOSED);
	                	}
	                	double minX = 0, maxY = 0;
	                	Point2D[] points = hPoly.getPoints();
	                	for(int i=0; i<points.length; i++)
	                	{
	                		if (i == 0 || points[i].getX() < minX)
	                			minX = points[i].getX();
	                		if (i == 0 || points[i].getY() > maxY)
	                			maxY = points[i].getY();
	                	}
	                    moveDelta = highlighter.addMessage(cell, message,
	                    	new Point2D.Double(minX + dbDelta.getX(), maxY + dbDelta.getY()));
	                    Point2D end1 = new Point2D.Double(ws.getOnePoint().getX() - dbDelta.getX(), ws.getOnePoint().getY() - dbDelta.getY());
	                    Point2D end2 = new Point2D.Double(ws.getOtherPoint().getX() - dbDelta.getX(), ws.getOtherPoint().getY() - dbDelta.getY());
	                    moveDRC = highlighter.addLine(end1, end2, cell, tooClose, true);
                	}
	                wnd.repaint();
	            }
	        }
	        if (isMiddleMouse(evt)) {
                // Continue panning the screen
		        int newX = evt.getX();
		        int newY = evt.getY();
				Point2D pt = wnd.getScheduledOffset();
				double scale = wnd.getScale();
				wnd.setOffset(new Point2D.Double(pt.getX() - (newX - lastX) / scale,
					pt.getY() + (newY - lastY) / scale));
                wnd.getSavedFocusBrowser().updateCurrentFocus();
				wnd.fullRepaint();
				lastX = newX;
				lastY = newY;
                return;
	        }

	        wnd.repaint();
	    }
    }

    /**
     * Class to detect the worst design-rule violation while moving a node or arc.
     */
    private static class WorstSpacing
    {
    	private boolean valid;
    	private double separation;
    	private double worstViolation;
    	private double minSpacing;
    	private Point2D worstFrom, worstTo;
    	private Layer layerFrom, layerTo;

    	public WorstSpacing()
    	{
    		valid = false;
    		worstFrom = new Point2D.Double(0, 0);
    		worstTo = new Point2D.Double(0, 0);
    	}

    	public boolean validSpacing() { return valid; }

    	public double getSeparation() { return separation; }

    	public double getMinSpacing() { return minSpacing; }

    	public Layer getOneLayer() { return layerFrom; }

    	public Layer getOtherLayer() { return layerTo; }

    	public Point2D getOnePoint() { return worstFrom; }

    	public Point2D getOtherPoint() { return worstTo; }

    	/**
    	 * Method to update this WorstSpacing object according to a polygon on a Geometric.
    	 * @param self the Geometric being examined.
    	 * @param poly the Poly on the Geometric.
    	 * @param net the Network connected to the polygon.
    	 * @param delta the offset that is being applied to the polygon (how much it has been dragged).
    	 * @param nl the Netlist for the cell.
    	 */
    	public void findWorstSpacing(Geometric self, Poly poly, Network net, Point2D delta, Netlist nl)
    	{
    		// ignore null layers
    		Layer lay = poly.getLayer();
    		if (lay == null) return;
    		if (lay.isPseudoLayer()) return;
    		if (lay.getFunction().isSubstrate()) return;

    		// move the polygon by the dragged amount
    		Point2D [] points = poly.getPoints();
    		for(int j=0; j<points.length; j++)
    			poly.setPoint(j, points[j].getX() + delta.getX(), points[j].getY() + delta.getY());

    		// find simplest rule for this layer to itself
    		Rectangle2D bounds = poly.getBounds2D();
    		double xS = bounds.getWidth();
    		double yS = bounds.getHeight();
    		double widRule = Math.min(xS, yS);
    		double lenRule = Math.max(xS, yS);
			int multiCut = -1;
			double surround = DRC.getMaxSurround(lay, Double.MAX_VALUE);

    		// search up to 5 times the design-rule distance away
    		double worstInteractionDistance = surround * 5;
    		Rectangle2D searchBounds = new Rectangle2D.Double(
    			bounds.getMinX()-worstInteractionDistance,
    			bounds.getMinY()-worstInteractionDistance,
    			bounds.getWidth() + worstInteractionDistance*2,
    			bounds.getHeight() + worstInteractionDistance*2);
    		for(Iterator<RTBounds> it = self.getParent().searchIterator(searchBounds); it.hasNext(); )
    		{
    			Geometric neighbor = (Geometric)it.next();
    			if (neighbor == self) continue;
    			if (neighbor instanceof NodeInst)
    			{
    				NodeInst otherNi = (NodeInst)neighbor;
    				if (otherNi.isCellInstance()) continue;
    				AffineTransform trans = otherNi.rotateOut();
        			Poly [] otherPolys = otherNi.getProto().getTechnology().getShapeOfNode(otherNi, true, true, null);
        			for(int i=0; i<otherPolys.length; i++)
        			{
        				Poly otherPoly = otherPolys[i];
        				Layer otherLay = otherPoly.getLayer();
        				if (otherLay == null) continue;
        	    		if (otherLay.isPseudoLayer()) continue;
        	    		if (otherLay.getFunction().isSubstrate()) continue;
        	    		if (lay.getTechnology() != otherLay.getTechnology()) continue;
        	    		PortProto pp = otherPoly.getPort();
        	    		if (pp == null) continue;
        	    		PortInst pi = otherNi.findPortInstFromProto(pp);
        	    		if (pi == null) continue;
        	    		Network otherNet = nl.getNetwork(pi);
        	    		if (otherNet == null) continue;
        	    		if (net == otherNet) continue;

        	    		DRCTemplate rule = DRC.getSpacingRule(lay, null, otherLay, null, false, multiCut, widRule, lenRule);
        	    		if (rule == null) continue;
        	    		double dist = rule.getValue(0);

        	    		otherPoly.transform(trans);
        				double sep = poly.separation(otherPoly);
        				if (valid && sep-dist >= worstViolation) continue;

        				// this is a better spacing
    					valid = true;
    					worstViolation = sep-dist;
    					separation = sep;
    					minSpacing = dist;
    					layerFrom = lay;
    					layerTo = otherLay;
    					findClosestPoints(poly, otherPoly);
        			}
    			} else
    			{
    				ArcInst otherAi = (ArcInst)neighbor;
    	    		Network otherNet = nl.getNetwork(otherAi, 0);
    	    		if (otherNet == null) continue;
    	    		if (net == otherNet) continue;
        			Poly [] otherPolys = otherAi.getProto().getTechnology().getShapeOfArc(otherAi);
        			for(int i=0; i<otherPolys.length; i++)
        			{
        				Poly otherPoly = otherPolys[i];
        				Layer otherLay = otherPoly.getLayer();
        				if (otherLay == null) continue;
        	    		if (otherLay.isPseudoLayer()) continue;
        	    		if (otherLay.getFunction().isSubstrate()) continue;

        	    		DRCTemplate rule = DRC.getSpacingRule(lay, null, otherLay, null, true, multiCut, widRule, lenRule);
        	    		if (rule == null) continue;
        	    		double dist = rule.getValue(0);

        				double sep = poly.separation(otherPoly);
        				if (valid && sep-dist >= worstViolation) continue;

        				// this is a better spacing
        				valid = true;
        				worstViolation = sep-dist;
        				separation = sep;
    					minSpacing = dist;
    					layerFrom = lay;
    					layerTo = otherLay;
    					findClosestPoints(poly, otherPoly);
        			}
    			}
    		}
    	}

    	/**
    	 * Method to examine load the "worstFrom" and "worstTo" field variables with the
    	 * closest two points on two polygons.
    	 * @param from one of the Polys to examine.
    	 * @param to the other Poly to examine.
    	 */
    	private void findClosestPoints(Poly from, Poly to)
    	{
    		// if both are manhattan, use special cases
    		Rectangle2D fromBox = from.getBox();
    		Rectangle2D toBox = to.getBox();
    		if (fromBox != null && toBox != null)
    		{
    			if (fromBox.getMinX() < toBox.getMaxX() && fromBox.getMaxX() > toBox.getMinX())
    			{
    				// one above the other: find Y distance
    				double xPos = (Math.max(fromBox.getMinX(), toBox.getMinX()) + Math.min(fromBox.getMaxX(), toBox.getMaxX())) / 2;
    				if (fromBox.getMinY() > toBox.getMaxY())
    				{
    					// from is above to
    					worstFrom.setLocation(xPos, fromBox.getMinY());
    					worstTo.setLocation(xPos, toBox.getMaxY());
    					return;
    				}
    				if (toBox.getMinY() > fromBox.getMaxY())
    				{
    					// to is above from
    					worstFrom.setLocation(xPos, fromBox.getMaxY());
    					worstTo.setLocation(xPos, toBox.getMinY());
    					return;
    				}
    				return;
    			}
    			if (fromBox.getMinY() < toBox.getMaxY() && fromBox.getMaxY() > toBox.getMinY())
    			{
    				// one next to the other: find X distance
    				double yPos = (Math.max(fromBox.getMinY(), toBox.getMinY()) + Math.min(fromBox.getMaxY(), toBox.getMaxY())) / 2;
    				if (fromBox.getMinX() > toBox.getMaxX())
    				{
    					// from is above to
    					worstFrom.setLocation(fromBox.getMinX(), yPos);
    					worstTo.setLocation(toBox.getMaxX(), yPos);
    					return;
    				}
    				if (toBox.getMinX() > fromBox.getMaxX())
    				{
    					// to is above from
    					worstFrom.setLocation(fromBox.getMaxX(), yPos);
    					worstTo.setLocation(toBox.getMinX(), yPos);
    					return;
    				}
    				return;
    			}
    		}

    		// use generalized algorithm
    		double minPD = 0;
    		Point2D [] fromPoints = from.getPoints();
    		for(int f=0; f<fromPoints.length; f++)
    		{
    			Point2D c = to.closestPoint(fromPoints[f]);
    			double pd = c.distance(fromPoints[f]);
    			if (f != 0 && pd >= minPD) continue;
    			minPD = pd;
    			worstFrom.setLocation(fromPoints[f]);
    		}

    		minPD = 0;
    		Point2D [] toPoints = to.getPoints();
    		for(int t=0; t<toPoints.length; t++)
    		{
    			double pd = worstFrom.distance(toPoints[t]);
    			if (t != 0 && pd >= minPD) continue;
    			minPD = pd;
    			worstTo.setLocation(toPoints[t]);
    		}
    	}
    }

	/** Handle mouse released event
     *
     * @param evt the MouseEvent
     */
    public void mouseReleased(MouseEvent evt) {

        if (debug) System.out.println("  ["+modeLeft+","+modeRight+"] "+evt.paramString());

		if (evt.getSource() instanceof EditWindow)
		{
			EditWindow wnd = (EditWindow)evt.getSource();
            Highlighter highlighter = wnd.getHighlighter();
	        Cell cell = wnd.getCell();
	        if (cell == null) return;
            if (cell != startCell) {
                escapePressed(wnd);
                return;
            }
            // add back in offset
	        int releaseX = evt.getX();
	        int releaseY = evt.getY();
	        Point2D dbMouse = wnd.screenToDatabase(releaseX, releaseY);
	        boolean ctrlPressed = (evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;
	        specialSelect = ToolBar.isSelectSpecial();

	        // ===== Right Mouse Release =====

	        if (isRightMouse(evt)) {

	            if (modeRight == Mode.zoomIn) {
	                // zoom in by a factor of two
	                double scale = wnd.getScale();
	                wnd.setScale(scale * 2);
	                wnd.clearDoingAreaDrag();
                    wnd.getSavedFocusBrowser().saveCurrentFocus();
	                wnd.fullRepaint();
	            }
	            if (modeRight == Mode.zoomOut) {
	                // zoom out by a factor of two
	                double scale = wnd.getScale();
	                wnd.setScale(scale / 2);
	    	        if (wnd.isInPlaceEdit())
	    	        	wnd.getInPlaceTransformOut().transform(dbMouse, dbMouse);
                    wnd.setOffset(dbMouse);
	                wnd.clearDoingAreaDrag();
                    wnd.getSavedFocusBrowser().saveCurrentFocus();
	                wnd.fullRepaint();
	            }
	            if (modeRight == Mode.drawBox || modeRight == Mode.zoomBox || modeRight == Mode.zoomBoxSingleShot) {
	                // drawing boxes
	                Point2D start = wnd.screenToDatabase((int)wnd.getStartDrag().getX(), (int)wnd.getStartDrag().getY());
	                Point2D end = wnd.screenToDatabase((int)wnd.getEndDrag().getX(), (int)wnd.getEndDrag().getY());
	                double minSelX = Math.min(start.getX(), end.getX());
	                double maxSelX = Math.max(start.getX(), end.getX());
	                double minSelY = Math.min(start.getY(), end.getY());
	                double maxSelY = Math.max(start.getY(), end.getY());

	                // determine if the user clicked on a single point to prevent unintended zoom-in
	                // a single point is 4 lambda or less AND 10 screen pixels or less
	                boolean onePoint = true;
                    Rectangle2D bounds = new Rectangle2D.Double(minSelX, minSelY, maxSelX-minSelX, maxSelY-minSelY);
	                if (bounds.getHeight() > 4 && bounds.getWidth() > 4) onePoint = false;
	                if (Math.abs(wnd.getStartDrag().getX()-wnd.getEndDrag().getX()) > 10 ||
	                	Math.abs(wnd.getStartDrag().getY()-wnd.getEndDrag().getY()) > 10) onePoint = false;

	                if (modeRight == Mode.drawBox) {
	                    // just draw a highlight box
	                    highlighter.addArea(new Rectangle2D.Double(minSelX, minSelY, maxSelX-minSelX, maxSelY-minSelY), cell);
	                }
                    if (modeRight == Mode.zoomBoxSingleShot) {
                        // zoom to box: focus on box
                        if (!onePoint)
                            wnd.focusScreen(bounds);
                        WindowFrame.setListener(oldListener);
                        if (modeLeft == Mode.zoomBoxSingleShot) modeLeft = Mode.none;
                    }
	                if (modeRight == Mode.zoomBox) {
	                    // zoom to box: focus on box
	                    if (onePoint)
	                    {
	                        // modeRight == Mode.zoomOut
	                        // if not zoomBox, then user meant to zoomOut
	                        double scale = wnd.getScale();
	                        wnd.setScale(scale / 2);
	                        wnd.clearDoingAreaDrag();
                            wnd.getSavedFocusBrowser().saveCurrentFocus();
	                        wnd.fullRepaint();
	                    } else {
	                        wnd.focusScreen(bounds);
	                    }

	                }
	                highlighter.finished();
	                wnd.clearDoingAreaDrag();
	                wnd.repaint();
	            }
	            if (modeRight == Mode.wiringFind || modeRight == Mode.stickyWiring) {
	                // see if anything under the pointer
	                /*
	                int numFound = Highlight.findObject(dbMouse, wnd, false, false, false, true, false, specialSelect, true);
	                if (numFound == 0) {
	                    // not over anything, nothing to connect to
	                    EditWindow.gridAlign(dbMouse);
	                    endObj = null;
	                } else {
	                    // connect objects
	                    Iterator hIt = Highlight.getHighlights().iterator();
	                    Highlight h2 = (Highlight)hIt.next();
	                    EditWindow.gridAlign(dbMouse);
	                    endObj = h2.getElectricObject();
	                }*/
	                EditWindow.gridAlign(dbMouse);
					User.getUserTool().setCurrentArcProto(currentArcWhenWiringPressed);
	                router.makeRoute(wnd, cell, startObj, endObj, dbMouse);
	                // clear any popup cloud we had
	                //wnd.clearShowPopupCloud();
	                // clear last switched to highlight
	                wiringTarget = null;
	            }
	            if (modeRight == Mode.wiringConnect) {
	                EditWindow.gridAlign(dbMouse);
					User.getUserTool().setCurrentArcProto(currentArcWhenWiringPressed);
	                router.makeRoute(wnd, cell, startObj, endObj, dbMouse);
                    wiringTarget = null;
	            }
                if (modeRight == Mode.wiringToSpace) {
                    EditWindow.gridAlign(dbMouse);
					User.getUserTool().setCurrentArcProto(currentArcWhenWiringPressed);
                    router.makeRoute(wnd, cell, startObj, null, dbMouse);
                    wiringTarget = null;
                }
	            modeRight = Mode.none;
	        }

	        // ===== Left Mouse Release =====

	        if (isLeftMouse(evt)) {

	            // ignore move if done within cancelMoveDelayMillis
	            long curTime = evt.getWhen();
	            if (debug) System.out.println("Time diff between click->release is: "+(curTime - leftMousePressedTimeStamp));
	            if (modeLeft == Mode.move || modeLeft == Mode.stickyMove) {
	                if ((curTime - leftMousePressedTimeStamp) < cancelMoveDelayMillis) {
	                    highlighter.setHighlightOffset(0, 0);
	                    modeLeft = Mode.none;
                        if (moveDelta != null) highlighter.remove(moveDelta);
	                    if (moveDRC != null) highlighter.remove(moveDRC);
	                    wnd.repaint();
	                    return;
	                }
	            }

	            // if 'stickyMove' is true and we are moving stuff, ignore mouse release
	            if (getStickyMove() && (modeLeft == Mode.move)) {
	                // only do so if after cancel move delay
	                /*
	                if ((System.currentTimeMillis() - leftMousePressedTimeStamp) < dragDelayMillis)
	                    modeLeft = Mode.none;       // user left mouse button single click
	                else
	                */
	                    modeLeft = Mode.stickyMove; // user moving stuff in sticky mode
	            } else {

	                if (modeLeft == Mode.selectBox || modeLeft == Mode.drawBox || modeLeft == Mode.zoomBoxSingleShot) {
	                    // select all in box
	                    Point2D start = wnd.screenToDatabase((int)wnd.getStartDrag().getX(), (int)wnd.getStartDrag().getY());
	                    Point2D end = wnd.screenToDatabase((int)wnd.getEndDrag().getX(), (int)wnd.getEndDrag().getY());
	                    double minSelX = Math.min(start.getX(), end.getX());
	                    double maxSelX = Math.max(start.getX(), end.getX());
	                    double minSelY = Math.min(start.getY(), end.getY());
	                    double maxSelY = Math.max(start.getY(), end.getY());
                        // determine if the user clicked on a single point to prevent unintended zoom-in
                        // a single point is 4 lambda or less AND 10 screen pixels or less
                        boolean onePoint = true;
                        Rectangle2D bounds = new Rectangle2D.Double(minSelX, minSelY, maxSelX-minSelX, maxSelY-minSelY);
                        if (bounds.getHeight() > 4 && bounds.getWidth() > 4) onePoint = false;
                        if (Math.abs(wnd.getStartDrag().getX()-wnd.getEndDrag().getX()) > 10 ||
                            Math.abs(wnd.getStartDrag().getY()-wnd.getEndDrag().getY()) > 10) onePoint = false;

	                    if (modeLeft == Mode.selectBox) {
	                        if (!invertSelection)
	                            highlighter.clear();
	                        highlighter.selectArea(wnd, minSelX, maxSelX, minSelY, maxSelY, invertSelection, specialSelect);
	                    }
	                    if (modeLeft == Mode.drawBox) {
	                        // just draw a highlight box
	                        highlighter.addArea(new Rectangle2D.Double(minSelX, minSelY, maxSelX-minSelX, maxSelY-minSelY), cell);
	                    }
                        if (modeLeft == Mode.zoomBoxSingleShot) {
                            // zoom to box: focus on box
                            if (!onePoint)
                                wnd.focusScreen(bounds);
                            WindowFrame.setListener(oldListener);
                            if (modeRight == Mode.zoomBoxSingleShot) modeRight = Mode.none;
                        }
	                    highlighter.finished();
	                    wnd.clearDoingAreaDrag();
	                    wnd.repaint();
	                }
	                if (modeLeft == Mode.move || modeLeft == Mode.stickyMove) {
	                    // moving objects
	                    if (ctrlPressed)
	                        dbMouse = convertToOrthogonal(new Point2D.Double(dbMoveStartX, dbMoveStartY), dbMouse, highlighter);
	                    Point2D dbDelta = new Point2D.Double(dbMouse.getX() - dbMoveStartX, dbMouse.getY() - dbMoveStartY);
                        EditWindow.gridAlign(dbDelta);
                        if (moveDelta != null) highlighter.remove(moveDelta);
	                    if (moveDRC != null) highlighter.remove(moveDRC);
	                    if (dbDelta.getX() != 0 || dbDelta.getY() != 0) {
	                        highlighter.setHighlightOffset(0, 0);
	                        CircuitChanges.manyMove(dbDelta.getX(), dbDelta.getY());
	                        wnd.fullRepaint();
	                    }
	                }
	                modeLeft = Mode.none;
	            }
	        }
	    }
    }

    /**
     * Use to track sticky move of objects
     * @param evt the MouseEvent
     */
    public void mouseMoved(MouseEvent evt) {

        mouseX = evt.getX();
        mouseY = evt.getY();

        //if (debug) System.out.println("  ["+modeLeft+","+modeRight+"] "+evt.paramString());
		if (evt.getSource() instanceof EditWindow)
		{
			EditWindow wnd = (EditWindow)evt.getSource();
            Highlighter highlighter = wnd.getHighlighter();
            Cell cell = wnd.getCell();
			if (cell == null) return;

			specialSelect = ToolBar.isSelectSpecial();
            Point2D dbMouse = wnd.screenToDatabase(mouseX, mouseY);

			if (modeLeft == Mode.stickyMove) {
				if (another)
					dbMouse = convertToOrthogonal(new Point2D.Double(dbMoveStartX, dbMoveStartY), dbMouse, highlighter);
				Point2D dbDelta = new Point2D.Double(dbMouse.getX() - dbMoveStartX, dbMouse.getY() - dbMoveStartY);
				EditWindow.gridAlign(dbDelta);
				Point2D screenDelta = wnd.deltaDatabaseToScreen((int)dbDelta.getX(), (int)dbDelta.getY());
				highlighter.setHighlightOffset((int)screenDelta.getX(), (int)screenDelta.getY());
				wnd.repaint();
			}

            mouseOver(dbMouse, wnd);
        }
    }

    /**
     * Draw a mouse over highlight
     * @param dbMouse
     * @param wnd
     */
    private void mouseOver(Point2D dbMouse, EditWindow wnd) {
        if (!User.isMouseOverHighlightingEnabled()) return;
        if (ToolBar.getSelectMode() == ToolBar.SelectMode.AREA) return;

        Highlighter highlighter = wnd.getHighlighter();
        Highlighter mouseOverHighlighter = wnd.getMouseOverHighlighter();

        // create temporary highlighter
        Highlighter tempHighlighter = new Highlighter(Highlighter.MOUSEOVER_HIGHLIGHTER, null);
        tempHighlighter.copyState(highlighter);

        Point2D screenMouse = wnd.databaseToScreen(dbMouse);
        Highlight found = null;
        if (!another && !invertSelection)
            // maintain current selection
            found = tempHighlighter.overHighlighted(wnd, (int)screenMouse.getX(), (int)screenMouse.getY());
        if (found == null)
        {
            // find something that would get selected
            found = tempHighlighter.findObject(dbMouse, wnd, false, another, invertSelection, true, false, specialSelect, true);
        }
        // Checking if found highlight is not found in existing list and then force the update.
        tempHighlighter.clear();
        if (found != null)
            tempHighlighter.addHighlight(found);

        // check if mouse-over highlight needs to change
        boolean changed = false;
        List<Highlight> mouseOld = mouseOverHighlighter.getHighlights();
//        assert(mouseOld.size() <= 1);
        List<Highlight> mouseNew = tempHighlighter.getHighlights();
//        assert(mouseNew.size() <= 1);

        if (mouseOld.size() == mouseNew.size()) {
            for (int i=0; i<mouseOld.size(); i++) {
                Highlight h1 = mouseOld.get(i);
                Highlight h2 = mouseNew.get(i);
                if (!h1.equals(h2)) { changed = true; break; }
            }
        } else
            changed = true;

        if (changed) {
            // copy state into mouse highlighter, signal change (using finished)

            mouseOverHighlighter.copyState(tempHighlighter);
            mouseOverHighlighter.finished();
            wnd.repaint();
        }
        // JFluid results.
        tempHighlighter.delete();  // remove from database
        tempHighlighter = null;
    }

    public void mouseClicked(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates
	    // to detect connection with other WindowContents.
	    if (e.getSource() instanceof EditWindow)
	    {
            EditWindow wnd = (EditWindow)e.getSource();
            Highlighter highlighter = wnd.getHighlighter();
            wnd.getWindowFrame().getFrame().getStatusBar().highlightChanged(highlighter);
	        WindowFrame.show3DHighlight();
	    }
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

		if (evt.getSource() instanceof EditWindow)
		{
			EditWindow wnd = (EditWindow)evt.getSource();
			Cell cell = wnd.getCell();
			if (cell == null) return;

			boolean sideways = (evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) != 0;
			boolean zoom = (evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;
			int rotation = evt.getWheelRotation();

			if (zoom)
			{
				// Control held: zoom in (roll back) or out (roll forward)
				double scale = wnd.getScale();
				double dY = rotation / 10.0;
				if (dY < 0) scale = scale - scale * dY;
					else scale = scale * Math.exp(-dY);
				wnd.setScale(scale);
                wnd.getSavedFocusBrowser().updateCurrentFocus();
				wnd.fullRepaint();
			} else if (sideways)
			{
				// Shift held: scroll left (roll back) or right (roll forward)
				ZoomAndPanListener.panXOrY(0, wnd.getWindowFrame(), rotation > 0 ? 1 : -1);
			} else
			{
				// no shift held: scroll up (roll forward) or down (roll back)
				ZoomAndPanListener.panXOrY(1, wnd.getWindowFrame(), rotation > 0 ? 1 : -1);
			}
		}
    }

    /** Key pressed event
     * Delete or Move selected objects
     * @param evt the KeyEvent
     */
    public void keyPressed(KeyEvent evt) {

        if (debug) System.out.println("  ["+modeLeft+","+modeRight+"] "+evt.paramString());

        int chr = evt.getKeyCode();
		if (evt.getSource() instanceof EditWindow)
		{
			EditWindow wnd = (EditWindow)evt.getSource();
			Cell cell = wnd.getCell();
			if (cell == null) return;

            boolean redrawMouseOver = false;
			// move stuff around
//			if (chr == KeyEvent.VK_LEFT) {
//				moveSelected(-1, 0, evt.isShiftDown(), evt.isControlDown());
//			} else if (chr == KeyEvent.VK_RIGHT) {
//				moveSelected(1, 0, evt.isShiftDown(), evt.isControlDown());
//			} else if (chr == KeyEvent.VK_UP) {
//				moveSelected(0, 1, evt.isShiftDown(), evt.isControlDown());
//			} else if (chr == KeyEvent.VK_DOWN) {
//				moveSelected(0, -1, evt.isShiftDown(), evt.isControlDown());
//			} else
			// cancel current mode
			if (chr == KeyEvent.VK_ESCAPE) {
                escapePressed(wnd);
            }
            else if (chr == KeyEvent.VK_CONTROL) {
                if (!another) redrawMouseOver = true;
                another = true;
            }
            else if (chr == KeyEvent.VK_SHIFT) {
                if (!invertSelection) redrawMouseOver = true;
                invertSelection = true;
            }

            if (redrawMouseOver) {
                mouseOver(wnd.screenToDatabase(mouseX, mouseY), wnd);
            }
			// wiring popup cloud selection
			/*
			if (wiringPopupCloudUp && (modeRight == Mode.stickyWiring || modeRight == Mode.wiringFind)) {
				for (int i=0; i<wiringPopupCloudList.size(); i++) {
					if (chr == (KeyEvent.VK_1 + i)) {
						PortInst pi = wiringPopupCloudList.get(i);
						EditWindow.gridAlign(wiringLastDBMouse);
						router.makeRoute(wnd, startObj, pi, wiringLastDBMouse);
						wnd.clearShowPopupCloud();      // clear popup cloud
						wiringPopupCloudUp = false;
						modeRight = Mode.none;
						return;
					}
				}
			} */
		}
    }

    private void escapePressed(EditWindow wnd) {
        Highlighter highlighter = wnd.getHighlighter();
        if (modeRight == Mode.wiringConnect || modeRight == Mode.wiringFind ||
            modeRight == Mode.stickyWiring)
            router.cancelInteractiveRoute();
        if (modeRight == Mode.zoomBox || modeRight == Mode.zoomBoxSingleShot || modeRight == Mode.zoomOut ||
            modeLeft == Mode.drawBox || modeLeft == Mode.selectBox)
        {
            wnd.clearDoingAreaDrag();
        }
        modeLeft = Mode.none;
        modeRight = Mode.none;
        highlighter.setHighlightOffset(0, 0);
        wnd.repaint();
    }

    public void keyReleased(KeyEvent evt) {

        int chr = evt.getKeyCode();
		if (evt.getSource() instanceof EditWindow)
		{
			EditWindow wnd = (EditWindow)evt.getSource();
			Cell cell = wnd.getCell();
			if (cell == null) return;

            boolean redrawMouseOver = false;
            if (chr == KeyEvent.VK_CONTROL) {
                if (another) redrawMouseOver = true;
                another = false;
            }
            else if (chr == KeyEvent.VK_SHIFT) {
                if (invertSelection) redrawMouseOver = true;
                invertSelection = false;
            }

            if (redrawMouseOver) {
                mouseOver(wnd.screenToDatabase(mouseX, mouseY), wnd);
            }
        }
    }

    public void keyTyped(KeyEvent evt) {
        if (debug) System.out.println("  ["+modeLeft+","+modeRight+"] "+evt.paramString());
    }

    // ********************************* Moving Stuff ********************************

    /** Move selected object(s) via keystroke.  If either scaleMove or scaleMove2
     * is true, the move is multiplied by the grid Bold frequency.  If both are
     * true the move gets multiplied twice.
     * @param dX amount to move in X in lambda
     * @param dY amount to move in Y in lambda
     * @param scaleMove scales move up if true
     * @param scaleMove2 scales move up if true (stacks with scaleMove)
     */
    public static void moveSelected(double dX, double dY, boolean scaleMove, boolean scaleMove2) {
        // scale distance according to arrow motion
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
		Dimension2D arrowDistance = User.getAlignmentToGrid();
		dX *= arrowDistance.getWidth();
		dY *= arrowDistance.getHeight();
		double scaleX = User.getDefGridXBoldFrequency();
        double scaleY = User.getDefGridYBoldFrequency();
		if (scaleMove) { dX *= scaleX;   dY *= scaleY; }
		if (scaleMove2) { dX *= scaleX;   dY *= scaleY; }

		// make sure the movement amount is grid-aligned
		Point2D del = new Point2D.Double(dX, dY);
		EditWindow.gridAlign(del);
		dX = del.getX();   dY = del.getY();

		// for edit windows doing outline editing, move the selected point
        if (WindowFrame.getListener() == OutlineListener.theOne)
        {
			OutlineListener.theOne.moveSelectedPoint(dX, dY);
        	return;
        }

        Highlighter highlighter = wnd.getHighlighter();
		highlighter.setHighlightOffset(0, 0);
		if (wnd.isInPlaceEdit())
		{
			Point2D delta = new Point2D.Double(dX, dY);
			AffineTransform trans = wnd.getInPlaceTransformIn();
	        double m00 = trans.getScaleX();
	        double m01 = trans.getShearX();
	        double m10 = trans.getShearY();
	        double m11 = trans.getScaleY();
			AffineTransform justRot = new AffineTransform(m00, m10, m01, m11, 0, 0);
			justRot.transform(delta, delta);
			dX = delta.getX();
			dY = delta.getY();
		}
		CircuitChanges.manyMove(dX, dY);
		wnd.fullRepaint();
	}

    /**
     * Convert the mousePoint to be orthogonal to the startPoint.
     * Chooses direction which is orthogonally farther from startPoint
     * @param startPoint the reference point.
     * @param mousePoint the mouse point.
     * @param highlighter to find out what is selected.
     * @return a new point orthogonal to startPoint.
     */
    private static Point2D convertToOrthogonal(Point2D startPoint, Point2D mousePoint, Highlighter highlighter)
    {
    	int orthoAngle = 90;
    	List<Geometric> highObjs = highlighter.getHighlightedEObjs(true, true);
    	for(Geometric geom : highObjs)
    	{
    		if (geom instanceof NodeInst)
    		{
    			NodeInst ni = (NodeInst)geom;
    			for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
    			{
    				Connection con = it.next();
    				int thisAngle = getAngleIncrement(con.getArc());
    				if (thisAngle < orthoAngle) orthoAngle = thisAngle;
    			}
    		} else
    		{
				int thisAngle = getAngleIncrement((ArcInst)geom);
				if (thisAngle < orthoAngle) orthoAngle = thisAngle;
    		}
    	}
    	if (orthoAngle == 0) return mousePoint;
        return InteractiveRouter.getClosestAngledPoint(startPoint, mousePoint, orthoAngle);

//    	// move in direction that is farther
//        double xdist, ydist;
//        xdist = Math.abs(mousePoint.getX() - startPoint.getX());
//        ydist = Math.abs(mousePoint.getY() - startPoint.getY());
//        if (ydist > xdist)
//            return new Point2D.Double(startPoint.getX(), mousePoint.getY());
//        return new Point2D.Double(mousePoint.getX(), startPoint.getY());
    }

    private static int getAngleIncrement(ArcInst ai)
    {
    	int angle = ai.getAngle();
    	if ((angle%900) == 0) return 90;
    	if ((angle%450) == 0) return 45;
    	if ((angle%300) == 0) return 30;
    	return 90;
    }

    // ********************************* Wiring Stuff ********************************

    public void switchWiringTarget() {
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();
        Cell cell = wnd.getCell();

        // if in mode wiringToSpace, switch out of it, and drop to next
        // block to find new wiring target
        if (modeRight == Mode.wiringToSpace) {
            modeRight = Mode.wiringFind;
        }

        // this command only valid if in wiring mode
        if (modeRight == Mode.wiringFind || modeRight == Mode.stickyWiring) {
            // can only switch if something under the mouse to wire to
            //if (endObj != null) {
                Point2D dbMouse = new Point2D.Double(DBMath.round(lastdbMouseX), DBMath.round(lastdbMouseY));
                Rectangle2D bounds = new Rectangle2D.Double(lastdbMouseX, lastdbMouseY, 0, 0);
                List<Highlight> targets = Highlighter.findAllInArea(highlighter, wnd.getCell(), false, true, false, specialSelect, false, bounds, wnd);
                Iterator<Highlight> it = targets.iterator();
                // find wiringTarget in list, if it exists
                boolean found = false;
                if (wiringTarget == null) wiringTarget = endObj;
                while (it.hasNext()) {
                    if ((it.next()).getElectricObject() == wiringTarget) {
                        found = true;
                        // get next object
                        if (!it.hasNext()) {
                            // this is the last target in list, switch to wiringToSpace mode
                            modeRight = Mode.wiringToSpace;
                            wiringTarget = null;
                            break;
                        }
                        wiringTarget = (it.next()).getElectricObject();
                        break;
                    }
                }
                // if not found in list, use head of list
                if (!found) {
                    it = targets.iterator();
                    if (it.hasNext()) wiringTarget = (it.next()).getElectricObject();
                    else wiringTarget = null;
                }
                // special case: switching modes to wire to space
                if (modeRight == Mode.wiringToSpace) {
                    endObj = null;
                    System.out.println("Switching to 'ignore all wiring targets'");
                    router.highlightRoute(wnd, cell, startObj, null, dbMouse);
                    return;
                }
                // if same target, do nothing
                if (endObj == wiringTarget)
                    return;
                // draw new route to target
                endObj = wiringTarget;
                if (wiringTarget == null) {
                    System.out.println("Switching to wiring target 'none'");
                } else {
                    System.out.println("Switching to wiring target '"+wiringTarget+"'");
                }
                router.highlightRoute(wnd, cell, startObj, wiringTarget, dbMouse);
            //}
            // nothing under mouse to route to/switch between, return
        } else {
            // toggle fat wiring mode
            EditingPreferences ep = UserInterfaceMain.getEditingPreferences();
            ep = ep.withFatWires(!ep.isFatWires());
            UserInterfaceMain.setEditingPreferences(ep);
            if (ep.isFatWires()) {
                System.out.println("Enabling fat wiring mode");
            } else {
                System.out.println("Disabling fat wiring mode");
            }
        }
    }

    /**
     * Wire to a layer.
     * @param layerNumber
     */
    public void wireTo(int layerNumber) {
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();
        Cell cell = wnd.getCell();
        if (cell == null) return;

        ArcProto ap = null;
        Technology tech = Technology.getCurrent();
        boolean found = false;
        for (Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); ) {
            ap = it.next();
            if (ap.isNotUsed()) continue;               // ignore arcs that aren't used
            switch(layerNumber) {
                case 0: {
                    if (ap.getFunction() == ArcProto.Function.POLY1) { found = true; } break; }
                case 1: {
                    if (ap.getFunction() == ArcProto.Function.METAL1) { found = true; } break; }
                case 2: {
                    if (ap.getFunction() == ArcProto.Function.METAL2
                            || ap.getFunction() == ArcProto.Function.BUS) { found = true; } break; }
                case 3: {
                    if (ap.getFunction() == ArcProto.Function.METAL3) { found = true; } break; }
                case 4: {
                    if (ap.getFunction() == ArcProto.Function.METAL4) { found = true; } break; }
                case 5: {
                    if (ap.getFunction() == ArcProto.Function.METAL5) { found = true; } break; }
                case 6: {
                    if (ap.getFunction() == ArcProto.Function.METAL6) { found = true; } break; }
                case 7: {
                    if (ap.getFunction() == ArcProto.Function.METAL7) { found = true; } break; }
                case 8: {
                    if (ap.getFunction() == ArcProto.Function.METAL8) { found = true; } break; }
                case 9: {
                    if (ap.getFunction() == ArcProto.Function.METAL9) { found = true; } break; }
            }
            if (found) break;
        }
        if (!found) return;

        //if (ap == User.getUserTool().getCurrentArcProto()) return;

        // if a single portinst highlighted, route from that to node that can connect to arc
        if (highlighter.getNumHighlights() == 1 && cell != null) {
            ElectricObject obj = highlighter.getOneHighlight().getElectricObject();
            if (obj instanceof PortInst) {
                PortInst pi = (PortInst)obj;
                router.makeVerticalRoute(wnd, pi, ap);
            }
        }
        // switch palette to arc
		User.getUserTool().setCurrentArcProto(ap);
    }


    // ********************************* Popup Menus *********************************

    /**
     * Popup menu when user is cycling through objects under pointer
     * @param objects list of objects to put in menu
     * @return the popup menu
     */
    public JPopupMenu selectPopupMenu(List<Highlight> objects) {
        JPopupMenu popup = new JPopupMenu("Choose One");
        JMenuItem m;
        for (Highlight obj : objects) {
            m = new JMenuItem(obj.toString()); m.addActionListener(this); popup.add(m);
        }
        //lastPopupMenu = "Select";
        return popup;
    }


    /** Select object or Wire to object, depending upon popup menu used */
    public void actionPerformed(ActionEvent e) {
    }

    // ------------------------------------ Preferences -----------------------------------

    private static final String cancelMoveDelayMillisPref = "cancelMoveDelayMillis";
    private static final String zoomInDelayMillisPref = "zoomInDelayMillis";
//    private static final String useFatWiringModePref = "UseFatWiringMode";

    /**
     * Recached Preferences after change
     */
    public void readPrefs() {
        router = new SimpleWirer(EditingPreferences.getThreadEditingPreferences().isFatWires());
        router.setTool(User.getUserTool());
        cancelMoveDelayMillis = prefs.getLong(cancelMoveDelayMillisPref, getFactoryCancelMoveDelayMillis());
        zoomInDelayMillis = prefs.getLong(zoomInDelayMillisPref, 120);
//        useFatWiringMode = prefs.getBoolean(useFatWiringModePref, true);

        interactiveDRCDrag = new DRC.DRCPreferences(false).interactiveDRCDrag;
    }

    public long getCancelMoveDelayMillis() { return cancelMoveDelayMillis; }

    public static long getFactoryCancelMoveDelayMillis() { return 200; }

    public void setCancelMoveDelayMillis(long delay) {
        cancelMoveDelayMillis = delay;
        prefs.putLong(cancelMoveDelayMillisPref, delay);
    }

    public long getZoomInDelayMillis() { return zoomInDelayMillis; }

    public void setZoomInDelayMillis(long delay) {
        zoomInDelayMillis = delay;
        prefs.putLong(zoomInDelayMillisPref, delay);
    }
//
//    public boolean getUseFatWiringMode() { return useFatWiringMode; }
//
//    public void setUseFatWiringMode(boolean b) {
//        useFatWiringMode = b;
//        prefs.putBoolean(useFatWiringModePref, b);
//    }


}
