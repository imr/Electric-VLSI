/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Highlighter.java
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
package com.sun.electric.tool.user;

import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.DisplayedText;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.routing.Router;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Class for per-window highlighting information.
 */
public class Highlighter implements DatabaseChangeListener {

    private static Highlighter currentHighlighter = null;

    /** Screen offset for display of highlighting. */			private int highOffX; private int highOffY;
    /** the highlighted objects. */								private List<Highlight2> highlightList;
    /** the stack of highlights. */								private List<List<Highlight2>> highlightStack;
    /** true if highlights have changed recently */             private boolean changed;
    /** last object selected before last clear() */             private Highlight2 lastHighlightListEndObj;
    /** what was the last level of "showNetwork" */             private int showNetworkLevel;
	/** the type of highlighter */                              private int type;
	/** the WindowFrame associated with the highlighter */      private WindowFrame wf;

    /** List of HighlightListeners */                           private static Set<HighlightListener> highlightListeners = new HashSet<HighlightListener>();

    /** the selection highlighter type */       public static final int SELECT_HIGHLIGHTER = 0;
    /** the mouse over highlighter type */      public static final int MOUSEOVER_HIGHLIGHTER = 1;
    /** the "measurement" highlighter type */   public static final int RULER_HIGHLIGHTER = 2;

    public static final int EXACTSELECTDISTANCE = 5;

    /**
     * Create a new Highlighter object
     * @param type
     */
    public Highlighter(int type, WindowFrame wf) {
        highOffX = highOffY = 0;
        highlightList = new ArrayList<Highlight2>();
        highlightStack = new ArrayList<List<Highlight2>>();
        changed = false;
        UserInterfaceMain.addDatabaseChangeListener(this);
        if (currentHighlighter == null) currentHighlighter = this;
        lastHighlightListEndObj = null;
        showNetworkLevel = 0;
		this.type = type;
		this.wf = wf;
    }

    void setChanged(boolean c) { changed = c; }

    /**
     * Destructor
     */
    public void delete() {
        UserInterfaceMain.removeDatabaseChangeListener(this);
    }

    /**
	 * Method to add an ElectricObject to the list of highlighted objects.
	 * @param eobj the ElectricObject to add to the list of highlighted objects.
	 * @param cell the Cell in which the ElectricObject resides.
	 * @return the newly created Highlight object.
	 */
    public Highlight2 addElectricObject(ElectricObject eobj, Cell cell)
    {
        return addElectricObject(eobj, cell, true);
    }

    /**
     * Method to add an ElectricObject to the list of highlighted objects.
     * @param eobj the ElectricObject to add to the list of highlighted objects.
     * @param cell the Cell in which the ElectricObject resides.
     * @param highlightConnected if true, highlight all objects that are in some way connected
     * to this object.  If false, do not. This is used by addNetwork to prevent extra
     * things from being highlighted later that are not connected to the network.
     * @return the newly created Highlight object.
     */
	public Highlight2 addElectricObject(ElectricObject eobj, Cell cell, boolean highlightConnected)
	{
        Highlight2 h1 = new HighlightEOBJ(eobj, cell, highlightConnected, -1);
		addHighlight(h1);
		return h1;
	}

    /**
	 * Method to add a text selection to the list of highlighted objects.
	 * @param cell the Cell in which this area resides.
	 * @param varKey the Variable.Key associated with the text (text is then a visual of that variable).
	 * @return the newly created Highlight object.
	 */
	public Highlight2 addText(ElectricObject eobj, Cell cell, Variable.Key varKey)
	{
        HighlightText h1 = new HighlightText(eobj, cell, varKey);
        addHighlight(h1);
		return h1;
	}

    /**
	 * Method to add a message display to the list of highlighted objects.
	 * @param cell the Cell in which this area resides.
	 * @param message the String to display.
	 * @param loc the location of the string (in database units).
	 * @return the newly created Highlight object.
	 */
	public Highlight2 addMessage(Cell cell, String message, Point2D loc)
	{
        Highlight2 h1 = new HighlightMessage(cell, message, loc);
        addHighlight(h1);
		return h1;
	}

    /**
	 * Method to add an area to the list of highlighted objects.
	 * @param area the Rectangular area to add to the list of highlighted objects.
	 * @param cell the Cell in which this area resides.
	 * @return the newly created Highlight object.
	 */
	public Highlight2 addArea(Rectangle2D area, Cell cell)
	{
        Highlight2 h1 = new HighlightArea(cell, area);
        addHighlight(h1);
		return h1;
	}

	/**
	 * Method to generic Object.
	 * @param obj object to add.
	 * @param cell the Cell in which this object resides.
	 * @return the newly created Highlight object.
	 */
	public Highlight2 addObject(Object obj, Cell cell)
	{
        Highlight2 h1 = new HighlightObject(cell, obj);
        addHighlight(h1);
		return h1;
	}

    /**
	 * Method to add a line to the list of highlighted objects.
	 * @param start the start point of the line to add to the list of highlighted objects.
	 * @param end the end point of the line to add to the list of highlighted objects.
	 * @param cell the Cell in which this line resides.
	 * @return the newly created Highlight object.
	 */
	public Highlight2 addLine(Point2D start, Point2D end, Cell cell)
	{
        Highlight2 h1 = new HighlightLine(cell, start, end, null, false);
        addHighlight(h1);
		return h1;
	}

    /**
	 * Method to add a line to the list of highlighted objects.
	 * @param start the start point of the line to add to the list of highlighted objects.
	 * @param end the end point of the line to add to the list of highlighted objects.
	 * @param cell the Cell in which this line resides.
	 * @param thick true for a thick line.
	 * @return the newly created Highlight object.
	 */
	public Highlight2 addLine(Point2D start, Point2D end, Cell cell, boolean thick)
	{
        Highlight2 h1 = new HighlightLine(cell, start, end, null, thick);
        addHighlight(h1);
		return h1;
	}

    /**
	 * Method to add a line to the list of highlighted objects.
	 * @param start the start point of the line to add to the list of highlighted objects.
	 * @param end the end point of the line to add to the list of highlighted objects.
	 * @param cell the Cell in which this line resides.
	 * @return the newly created Highlight object.
	 */
	public Highlight2 addThickLine(Point2D start, Point2D end, Cell cell)
	{
        Highlight2 h1 = new HighlightLine(cell, start, end, null, true);
        addHighlight(h1);
		return h1;
	}

    /**
     * Method to add a Poly to the list of highlighted objects
     * @param poly the poly to add
     * @param cell the cell in which to display the poly
     * @param color the color to draw the poly with (if null, uses default)
     * @return the newly created highlight object
     */
    public Highlight2 addPoly(Poly poly, Cell cell, Color color)
    {
        Highlight2 h1 = new HighlightPoly(cell, poly, color);
        addHighlight(h1);
        return h1;
    }

    /**
	 * Method to add a network to the list of highlighted objects.
	 * Many arcs may be highlighted as a result.
	 * @param net the network to highlight.
	 * @param cell the Cell in which this line resides.
	 */
	public void addNetwork(Network net, Cell cell)
	{
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted highlighting (network information unavailable).  Please try again");
			return;
		}
        HashSet<Network> nets = new HashSet<Network>();
        nets.add(net);
        List<Highlight2> highlights = NetworkHighlighter.getHighlights(cell, netlist, nets, 0, 0);
        for (Highlight2 h : highlights) {
            addHighlight(h);
        }
	}

    /**
     * This is the show network command. It is similar to addNetwork, however
     * each time it is used without first clearing
     * the highlighter, it shows connections to the network another level down
     * in the hierarchy.
     * @param nets list of Networks in current cell to show
     * @param cell the cell in which to create the highlights
     */
    public void showNetworks(Set<Network> nets, Netlist netlist, Cell cell) {
        int showNetworkLevel;
        synchronized(this) {
            showNetworkLevel = this.showNetworkLevel;
        }
        if (showNetworkLevel == 0) {
            ArrayList<Network> sortedNets = new ArrayList<Network>(nets);
            Collections.sort(sortedNets, new TextUtils.NetworksByName());
            for (Network net : sortedNets) {
                System.out.println("Highlighting "+net);
            }
            clear();
        }
        int count = 0;
        List<Highlight2> highlights = NetworkHighlighter.getHighlights(cell, netlist, nets,
                showNetworkLevel, showNetworkLevel);
        for (Highlight2 h : highlights) {
            addHighlight(h);
            count++;
        }
//        for (Network net : nets) {
//            if (showNetworkLevel == 0) System.out.println("Highlighting "+net);
//            List<Highlight> highlights = NetworkHighlighter.getHighlights(cell, netlist, net,
//                    showNetworkLevel, showNetworkLevel);
//            for (Highlight h : highlights) {
//                addHighlight(h);
//                count++;
//            }
//        }
        synchronized(this) {
            this.showNetworkLevel = showNetworkLevel+1;
        }
        if (count == 0) {
            System.out.println("Nothing more in hierarchy on network(s) to show");
        }
    }

    /**
     * Add a Highlight
     */
    public synchronized void addHighlight(Highlight2 h) {
        if (h == null) return;
        highlightList.add(h);
        changed = true;
    }

    /**
	 * Method to clear the list of all highlighted objects in
	 */
	public void clear()
	{
        clear(true);
    }

    private synchronized void clear(boolean resetLastHighlightListEndObj) {
        highOffX = highOffY = 0;
        showNetworkLevel = 0;

        if (highlightList.size() == 0) return;

        // save last selected
        if (resetLastHighlightListEndObj)
            lastHighlightListEndObj = highlightList.get(highlightList.size()-1);
        // clear
        highlightList.clear();
        changed = true;
	}


    /**
	 * Method to indicate that changes to highlighting are finished.
	 * Call this after any change to highlighting.
	 */
	public void finished()
	{
        // only do something if highlights changed
        synchronized(this) {
            // check to see if any highlights are now invalid
            for (Highlight2 h : getHighlights()) {
                if (!h.isValid()) {
                    // remove
                    remove(h); // we can do this because iterator is iterating over copy
                    changed = true;
                }
            }
            if (!changed) return;
        }

		// see if arcs of a single type were selected
		boolean mixedArc = false;
		ArcProto foundArcProto = null;
		for(Highlight2 h : getHighlights())
		{
            if (h instanceof HighlightEOBJ)
			{
				ElectricObject eobj = ((HighlightEOBJ)h).eobj;
				if (eobj instanceof ArcInst)
				{
					ArcProto ap = ((ArcInst)eobj).getProto();
					if (foundArcProto == null)
					{
						foundArcProto = ap;
					} else
					{
						if (foundArcProto != ap) mixedArc = true;
					}
				}
			}
		}
        if (type == SELECT_HIGHLIGHTER)
		    if (foundArcProto != null && !mixedArc) User.getUserTool().setCurrentArcProto(foundArcProto);

        // notify all listeners that highlights have changed (changes committed).
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { fireHighlightChanged(); }
            });
        } else {
            fireHighlightChanged();
        }
	}

	/**
	 * Method to ensure that the highlighting is visible.
	 * If the highlighting is offscreen, flash an arrow towards it.
	 * If the highlighting is small, flash lines around it.
	 */
	public void ensureHighlightingSeen()
	{
		// must be drawing in an edit window
	    if (wf == null || !(wf.getContent() instanceof EditWindow)) return;
		EditWindow wnd = (EditWindow)wf.getContent();

		// must have something highlighted
		Rectangle2D bounds = getHighlightedArea(wnd);
		if (bounds == null) return;

		// determine the area being highlighted
		double boundsArea = bounds.getWidth() * bounds.getHeight();
		Rectangle2D displayBounds = wnd.displayableBounds();
		double displayArea = displayBounds.getWidth() * displayBounds.getHeight();
		Highlight2 line1 = null, line2 = null, line3 = null, line4 = null;

		// if objects are offscreen, point the way
		if (bounds.getMinX() >= displayBounds.getMaxX() ||
			bounds.getMaxX() <= displayBounds.getMinX() ||
			bounds.getMinY() >= displayBounds.getMaxY() ||
			bounds.getMaxY() <= displayBounds.getMinY())
		{
			Point2D fromPt = new Point2D.Double(displayBounds.getCenterX(), displayBounds.getCenterY());
			Point2D toPt = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
			GenMath.clipLine(fromPt, toPt, displayBounds.getMinX(), displayBounds.getMaxX(),
					displayBounds.getMinY(), displayBounds.getMaxY());
			if (fromPt.getX() != displayBounds.getCenterX() || fromPt.getY() != displayBounds.getCenterY())
			{
				// clipLine may swap points: swap them back
				Point2D swap = fromPt;
				fromPt = toPt;
				toPt = swap;
			}
			line1 = addLine(fromPt, toPt, wnd.getCell());
			int angle = GenMath.figureAngle(fromPt, toPt);
			double headLength = fromPt.distance(toPt) / 10;
			double xLeft = toPt.getX() - headLength * DBMath.cos(angle+150);
			double yLeft = toPt.getY() - headLength * DBMath.sin(angle+150);
			double xRight = toPt.getX() - headLength * DBMath.cos(angle-150);
			double yRight = toPt.getY() - headLength * DBMath.sin(angle-150);
			line2 = addLine(new Point2D.Double(xLeft, yLeft), toPt, wnd.getCell());
			line3 = addLine(new Point2D.Double(xRight, yRight), toPt, wnd.getCell());
		} else
		{
			// if displayed objects are very small, point them out
			if (boundsArea * 500 <  displayArea)
			{
				if (bounds.getMinX() > displayBounds.getMinX() && bounds.getMinY() > displayBounds.getMinY())
					line1 = addLine(new Point2D.Double(displayBounds.getMinX(), displayBounds.getMinY()),
						new Point2D.Double(bounds.getMinX(), bounds.getMinY()), wnd.getCell());

				if (bounds.getMinX() > displayBounds.getMinX() && bounds.getMaxY() < displayBounds.getMaxY())
					line2 = addLine(new Point2D.Double(displayBounds.getMinX(), displayBounds.getMaxY()),
						new Point2D.Double(bounds.getMinX(), bounds.getMaxY()), wnd.getCell());

				if (bounds.getMaxX() < displayBounds.getMaxX() && bounds.getMinY() > displayBounds.getMinY())
					line3 = addLine(new Point2D.Double(displayBounds.getMaxX(), displayBounds.getMinY()),
						new Point2D.Double(bounds.getMaxX(), bounds.getMinY()), wnd.getCell());

				if (bounds.getMaxX() < displayBounds.getMaxX() && bounds.getMaxY() < displayBounds.getMaxY())
					line4 = addLine(new Point2D.Double(displayBounds.getMaxX(), displayBounds.getMaxY()),
						new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()), wnd.getCell());
			}
		}

		// if there was temporary identification, queue a timer to turn it off
		if (line1 != null || line2 != null || line3 != null || line4 != null)
		{
			Timer timer = new Timer(500, new FlashActionListener(this, line1, line2, line3, line4));
			timer.setRepeats(false);
			timer.start();
		}
	}

	private static class FlashActionListener implements ActionListener
	{
		private Highlighter hl;
		private Highlight2 line1, line2, line3, line4;

		FlashActionListener(Highlighter hl, Highlight2 line1, Highlight2 line2, Highlight2 line3, Highlight2 line4)
		{
			this.hl = hl;
			this.line1 = line1;
			this.line2 = line2;
			this.line3 = line3;
			this.line4 = line4;
		}
	    public void actionPerformed(ActionEvent evt)
		{
			if (line1 != null) hl.remove(line1);
			if (line2 != null) hl.remove(line2);
			if (line3 != null) hl.remove(line3);
			if (line4 != null) hl.remove(line4);
			hl.finished();
			hl.getWindowFrame().getContent().repaint();
		}    
	}

    /**
     * Get the last object that was selected. If underCursor is not null,
     * if any of the Highlights in underCursor are currently highlighted, then
     * the last thing highlighted will be the last thing selected before the last
     * clear(). This is to be able to properly cycle through objects under the cursor.
     * @param underCursor a list of Highlights underCursor.
     * @return the last object that was selected
     */
    private synchronized Highlight2 getLastSelected(List<Highlight2> underCursor) {
        List<Highlight2> currentHighlights = getHighlights();               // not that this is a copy

        // check underCursor list
        for (Highlight2 h : underCursor) {
            for (Highlight2 curHigh : currentHighlights) {
                if (h.sameThing(curHigh)) {
                    return lastHighlightListEndObj;
                }
            }
        }

        if (currentHighlights.size() > 0) {
            return currentHighlights.get(currentHighlights.size()-1);
        } else {
            return lastHighlightListEndObj;
        }
    }

    /**
     * Inherits the last selected object from the specified highlighter.
     * This is a hack, don't use it.
     * @param highlighter
     */
    public synchronized void copyState(Highlighter highlighter) {
        clear();
        lastHighlightListEndObj = highlighter.lastHighlightListEndObj;
        for (Highlight2 h : highlighter.getHighlights()) {
            Highlight2 copy = (Highlight2)h.clone();
            addHighlight(copy);
        }
        // don't inherit offset, messes up mouse over highlighter
        //highOffX = highlighter.highOffX;
        //highOffY = highlighter.highOffY;
    }

    /**
     * Shows highlights for the current EditWindow
     * @param wnd
     * @param g
     */
    public void showHighlights(EditWindow wnd, Graphics g) {
        int num = getNumHighlights();
        int highOffX, highOffY;
        synchronized(this) {
            highOffX = this.highOffX;
            highOffY = this.highOffY;
        }

        List<Highlight2> list = highlightList; //getHighlights();
//        long start = System.currentTimeMillis();

        Color colorH = new Color(User.getColorHighlight());
        Color colorM = new Color(User.getColorMouseOverHighlight());
        Stroke stroke = Highlight2.solidLine;

        for (Highlight2 h : list)
        {
            // only show highlights for the current cell
            if (h.getCell() == wnd.getCell())
            {
                boolean setConnected = true;
                Color color = colorH;
                if (type == MOUSEOVER_HIGHLIGHTER)
                {
                    color = colorM;
                    setConnected = false;
                }
                h.showHighlight(wnd, g, highOffX, highOffY, (num == 1), color, stroke, setConnected);
            }
        }
    }

	/**
	 * Method to return the WindowFrame associated with this Highlighter.
	 * @return the WindowFrame associated with this Highlighter.
	 * Returns null if no WindowFrame is associated.
	 */
	public WindowFrame getWindowFrame() { return wf; }

    /** Add a Highlight listener */
    public static synchronized void addHighlightListener(HighlightListener l) {
        highlightListeners.add(l);
    }

    /** Remove a Highlight listener */
    public static synchronized void removeHighlightListener(HighlightListener l) {
        highlightListeners.remove(l);
    }

    /** Notify listeners that highlights have changed */
    private void fireHighlightChanged() {
    	if (type == SELECT_HIGHLIGHTER)
    	{
	        List<HighlightListener> listenersCopy;
	        synchronized(this) {
	            listenersCopy = new ArrayList<HighlightListener>(highlightListeners);
	        }
	        for (HighlightListener l : listenersCopy) {
	            l.highlightChanged(this);
	        }
    	}
        synchronized(this) {
            changed = false;
        }
    }

    /** Notify listeners that the current Highlighter has changed */
    private synchronized void fireHighlighterLostFocus(Highlighter highlighterGainedFocus) {
    	if (type == SELECT_HIGHLIGHTER)
    	{
	        List<HighlightListener> listenersCopy;
	        synchronized(this) {
	            listenersCopy = new ArrayList<HighlightListener>(highlightListeners);
	        }
	        for (HighlightListener l : listenersCopy) {
	            l.highlighterLostFocus(highlighterGainedFocus);
	        }
    	}
    }

    /**
     * Called when the Highlighter owner has gained focus, and the
     * current highlighter switches to this.
     */
    public void gainedFocus() {
        Highlighter oldHighlighter = null;
        synchronized(currentHighlighter) {
            oldHighlighter = currentHighlighter;
            currentHighlighter = this;
        }
        // fire focus changed on old highlighter
        if ((oldHighlighter != null) && (oldHighlighter != this))
            oldHighlighter.fireHighlighterLostFocus(this);
    }

    /**
	 * Method to push the current highlight list onto a stack.
	 */
	public synchronized void pushHighlight()
	{
		// make a copy of the highlighted list
		List<Highlight2> pushable = new ArrayList<Highlight2>();
		for(Highlight2 h : highlightList)
			pushable.add(h);
		highlightStack.add(pushable);
	}

    /**
	 * Method to pop the current highlight list from the stack.
	 */
	public synchronized void popHighlight()
	{
		int stackSize = highlightStack.size();
		if (stackSize <= 0)
		{
			System.out.println("There is no highlighting saved on the highlight stack");
			return;
		}

		// get the stacked highlight
		List<Highlight2> popable = highlightStack.get(stackSize-1);
		highlightStack.remove(stackSize-1);

		// validate each highlight as it is added
		clear();
		for(Highlight2 h : popable)
		{
            Cell cell = h.getCell();
            if (h instanceof HighlightEOBJ)
			{
                HighlightEOBJ hh = (HighlightEOBJ)h;
                ElectricObject eobj = hh.eobj;
				if (cell.objInCell(eobj))
				{
					HighlightEOBJ newH = (HighlightEOBJ)addElectricObject(eobj, cell);
                    newH.point = hh.point;
				}
			} else if (h instanceof HighlightText)
			{
                HighlightText hh = (HighlightText)h;
                ElectricObject eobj = hh.eobj;
				if (cell.objInCell(eobj))
				{
					addText(eobj, cell, hh.varKey);
				}
			} else if (h instanceof HighlightArea)
			{
                HighlightArea hh = (HighlightArea)h;
				addArea(hh.bounds, cell);
			} else if (h instanceof HighlightLine)
			{
                HighlightLine hh = (HighlightLine)h;
                if (hh.thickLine)
                    addThickLine(hh.start, hh.end, cell);
                else
				    addLine(hh.start, hh.end, cell);
			} else if (h instanceof HighlightMessage) //type == Highlight.Type.MESSAGE)
            {
                HighlightMessage hh = (HighlightMessage)h;
				addMessage(cell, hh.msg, hh.loc);
			}
		}
		finished();
	}

    /**
     * Removes a Highlight object from the current set of highlights.
     * @param h the Highlight to remove
     */
    public synchronized void remove(Highlight2 h) {
        highlightList.remove(h);
    }

    /**
	 * Method to return the number of highlighted objects.
	 * @return the number of highlighted objects.
	 */
	public synchronized int getNumHighlights() { return highlightList.size(); }

    /**
	 * Method to return a list that is a copy of the list of current highlights.
	 * @return an list of highlights
	 */
	public synchronized List<Highlight2> getHighlights() {
        ArrayList<Highlight2> highlightsCopy = new ArrayList<Highlight2>(highlightList);
        return highlightsCopy;
    }

    /**
	 * Method to load a list of Highlights into the highlighting.
	 * @param newHighlights a List of Highlight objects.
	 */
	public synchronized void setHighlightListGeneral(List<Highlight2> newHighlights)
	{
        clear();
		for(Highlight2 obj : newHighlights)
		{
			highlightList.add(obj);
		}
        changed = true;
	}

    /**
	 * Method to load a list of Highlights into the highlighting.
	 * @param newHighlights a List of Highlight objects.
	 */
	public synchronized void setHighlightList(List<Highlight2> newHighlights)
	{
        clear();
		for(Highlight2 obj : newHighlights)
		{
			highlightList.add((Highlight2)obj);
		}
        changed = true;
	}

    /**
	 * Method to return a List of all highlighted Geometrics.
	 * @param wantNodes true if NodeInsts should be included in the list.
	 * @param wantArcs true if ArcInsts should be included in the list.
	 * @return a list with the highlighted Geometrics.
	 */
	public List<Geometric> getHighlightedEObjs(boolean wantNodes, boolean wantArcs)
	{
		// now place the objects in the list
		List<Geometric> highlightedGeoms = new ArrayList<Geometric>();
		for(Highlight2 h : getHighlights())
		{
            h.getHighlightedEObjs(this, highlightedGeoms, wantNodes, wantArcs);
		}
		return highlightedGeoms;
	}

    /**
	 * Method to return a List of all highlighted NodeInsts.
	 * @return a list with the highlighted NodeInsts.
	 */
	public List<NodeInst> getHighlightedNodes()
	{
		// now place the objects in the list
		List<NodeInst> highlightedNodes = new ArrayList<NodeInst>();
		for(Highlight2 h : getHighlights())
		{
            h.getHighlightedNodes(this, highlightedNodes);
		}
		return highlightedNodes;
	}

    /**
	 * Method to return a List of all highlighted ArcInsts.
	 * @return a list with the highlighted ArcInsts.
	 */
	public List<ArcInst> getHighlightedArcs()
	{
		// now place the objects in the list
		List<ArcInst> highlightedArcs = new ArrayList<ArcInst>();
		for(Highlight2 h : getHighlights())
		{
            h.getHighlightedArcs(this, highlightedArcs);
		}
		return highlightedArcs;
	}

    /**
	 * Method to return a set of the currently selected networks.
	 * @return a set of the currently selected networks.
	 * If there are no selected networks, the list is empty.
	 */
	public Set<Network> getHighlightedNetworks()
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf.getContent() instanceof WaveformWindow)
		{
			WaveformWindow ww = (WaveformWindow)wf.getContent();
			return ww.getHighlightedNetworks();
		}
		Set<Network> nets = new HashSet<Network>();
		Cell cell = WindowFrame.getCurrentCell();
		if (cell != null)
		{
			Netlist netlist = cell.acquireUserNetlist();
			if (netlist == null)
			{
				String msg = "Selected networks are not ready";
				System.out.println(msg);
				ActivityLogger.logMessage(msg);
				return nets;
			}
			for(Highlight2 h : getHighlights())
			{
                h.getHighlightedNetworks(nets, netlist);
			}
		}
		return nets;
	}

    /**
	 * Method to return a List of all highlighted text.
	 * @param unique true to request that the text objects be unique,
	 * and not attached to another object that is highlighted.
	 * For example, if a node and an export on that node are selected,
	 * the export text will not be included if "unique" is true.
	 * @return a list with the Highlight objects that point to text.
	 */
	public List<DisplayedText> getHighlightedText(boolean unique)
	{
		// now place the objects in the list
		List<DisplayedText> highlightedText = new ArrayList<DisplayedText>();
		for(Highlight2 h : getHighlights())
		{
            h.getHighlightedText(highlightedText, unique, getHighlights());
		}
		return highlightedText;
	}

    /**
	 * Method to return the bounds of the highlighted objects.
	 * @param wnd the window in which to get bounds.
	 * @return the bounds of the highlighted objects (null if nothing is highlighted).
	 */
	public Rectangle2D getHighlightedArea(EditWindow wnd)
	{
		// initially no area
		Rectangle2D bounds = null;

		// look at all highlighted objects
		for(Highlight2 h : getHighlights())
		{
			// find the bounds of this highlight
			Rectangle2D highBounds = h.getHighlightedArea(wnd);

			// combine this highlight's bounds with the overall one
			if (highBounds != null)
			{
				if (bounds == null)
				{
					bounds = new Rectangle2D.Double();
					bounds.setRect(highBounds);
				} else
				{
					Rectangle2D.union(bounds, highBounds, bounds);
				}
			}
		}

		// return the overall bounds
		return bounds;
	}

    /**
	 * Method to return the only highlight that encompases an object in Cell cell.
	 * If there is not one highlighted object, an error is issued.
	 * @return the highlight that selects an object (null if error).
	 */
	public Highlight2 getOneHighlight()
	{
		if (getNumHighlights() == 0)
		{
			System.out.println("Must select an object first");
			return null;
		}
		Highlight2 h = null;
		for(Highlight2 theH : getHighlights())
		{
            if (theH.getElectricObject() != null) return theH;
		}
		if (h == null)
		{
			System.out.println("Must select an object first");
			return null;
		}
		return h;
	}

    /**
	 * Method to return the only highlighted object.
	 * If there is not one highlighted object, an error is issued.
	 * @return the highlighted object (null if error).
	 */
	public ElectricObject getOneElectricObject(Class type)
	{
		Highlight2 high = getOneHighlight();
		if (high == null) return null;
		if (!(high instanceof HighlightEOBJ))
		{
            System.out.println("Must first select an object");
            return null;
        }
        ElectricObject eobj = high.getElectricObject();
		if (type == NodeInst.class)
		{
			if (eobj instanceof PortInst) eobj = ((PortInst)eobj).getNodeInst();
		}
		if (type != eobj.getClass())
		{

            System.out.println("Wrong type of object is selected");
            System.out.println(" (Wanted "+type.toString()+" but got "+eobj.getClass().toString()+")");
            return null;
		}
		return eobj;
	}

    /**
	 * Method to set a screen offset for the display of highlighting.
	 * @param offX the X offset (in pixels) of the highlighting.
	 * @param offY the Y offset (in pixels) of the highlighting.
	 */
	public synchronized void setHighlightOffset(int offX, int offY)
	{
		highOffX = offX;
		highOffY = offY;
	}

    /**
     * Method to return the screen offset for the display of highlighting
     * @return a Point2D containing the x and y offset.
     */
    public synchronized Point2D getHighlightOffset()
    {
        return new Point2D.Double(highOffX, highOffY);
    }

    /**
	 * Method to add everything in an area to the selection.
	 * @param wnd the window being examined.
	 * @param minSelX the low X coordinate of the area in database units.
	 * @param maxSelX the high X coordinate of the area in database units.
	 * @param minSelY the low Y coordinate of the area in database units.
	 * @param maxSelY the high Y coordinate of the area in database units.
	 * @param invertSelection is true to invert the selection (remove what is already highlighted and add what is new).
	 * @param findSpecial is true to find hard-to-select objects.
	 */
	public void selectArea(EditWindow wnd, double minSelX, double maxSelX, double minSelY, double maxSelY,
		boolean invertSelection, boolean findSpecial)
	{
		Rectangle2D searchArea = new Rectangle2D.Double(minSelX, minSelY, maxSelX - minSelX, maxSelY - minSelY);
		List<Highlight2> underCursor = findAllInArea(this, wnd.getCell(), false, false, false, false, findSpecial, true, searchArea, wnd);
		if (invertSelection)
		{
			for(Highlight2 newHigh : underCursor)
			{
				boolean found = false;
                for (Highlight2 oldHigh : getHighlights()) {
                    if (newHigh.sameThing(oldHigh)) {
                        remove(oldHigh);
                        found = true;
                        break;
                    }
                }
				if (found) continue;
				addHighlight(newHigh);
			}
		} else
		{
			setHighlightList(underCursor);
		}
	}

    /**
	 * Method to tell whether a point is over this Highlight.
	 * @param wnd the window being examined.
	 * @param x the X screen coordinate of the point.
	 * @param y the Y screen coordinate of the point.
	 * @return Highlight2 if the point is over this Highlight.
	 */
	public Highlight2 overHighlighted(EditWindow wnd, int x, int y)
	{
		for(Highlight2 h : getHighlights())
		{
            if (h.overHighlighted(wnd, x, y, this)) return h;
		}
		return null;
	}

    /**
	 * Method to convert the Variable to a series of points that describes the text.
	 */
	public static Point2D [] describeHighlightText(EditWindow wnd, ElectricObject eObj, Variable.Key varKey)
	{
        if (!Job.acquireExamineLock(false)) return null;
        Poly.Type style = null;
        Point2D[] points = null;
        Rectangle2D bounds = null;
        try {
            Poly poly = eObj.computeTextPoly(wnd, varKey);
            if (poly == null) {
                Job.releaseExamineLock();
                return null;
            }
            bounds = poly.getBounds2D();
            style = poly.getStyle();
			if (style != Poly.Type.TEXTCENT && style != Poly.Type.TEXTBOX)
			{
	            style = Poly.rotateType(style, eObj);
				TextDescriptor td = poly.getTextDescriptor();
				if (td != null)
				{
					int rotation = td.getRotation().getIndex();
					if (rotation != 0)
					{
						int angle = style.getTextAngle();
						style = Poly.Type.getTextTypeFromAngle((angle+900*rotation) % 3600);
					}
				}
			}
            if (style == Poly.Type.TEXTBOX && (eObj instanceof Geometric))
            {
                bounds = ((Geometric)eObj).getBounds();
            }
            Job.releaseExamineLock();
        } catch (Error e) {
            Job.releaseExamineLock();
            throw e;
        }
        if (style == Poly.Type.TEXTCENT)
        {
            points = new Point2D.Double[4];
            points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
            points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
            points[2] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
            points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
        }
        else if (style == Poly.Type.TEXTBOT)
        {
            points = new Point2D.Double[6];
            points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
            points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
            points[2] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
            points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
            points[4] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
            points[5] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
        }
        else if (style == Poly.Type.TEXTTOP)
        {
            points = new Point2D.Double[6];
            points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
            points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
            points[2] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
            points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
            points[4] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
            points[5] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
        }
        else if (style == Poly.Type.TEXTLEFT)
        {
            points = new Point2D.Double[6];
            points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
            points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
            points[2] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
            points[3] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
            points[4] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
            points[5] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
        }
        else if (style == Poly.Type.TEXTRIGHT)
        {
            points = new Point2D.Double[6];
            points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
            points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
            points[2] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
            points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
            points[4] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
            points[5] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
        }
        else if (style == Poly.Type.TEXTTOPLEFT)
        {
            points = new Point2D.Double[4];
            points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
            points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
            points[2] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
            points[3] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
        }
        else if (style == Poly.Type.TEXTBOTLEFT)
        {
            points = new Point2D.Double[4];
            points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
            points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
            points[2] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
            points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
        }
        else if (style == Poly.Type.TEXTTOPRIGHT)
        {
            points = new Point2D.Double[4];
            points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
            points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
            points[2] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
            points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
        }
        else if (style == Poly.Type.TEXTBOTRIGHT)
        {
            points = new Point2D.Double[4];
            points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
            points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
            points[2] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
            points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
        }
        else if (style == Poly.Type.TEXTBOX)
        {
            points = new Point2D.Double[12];
            double lX = bounds.getMinX();
            double hX = bounds.getMaxX();
            double lY = bounds.getMinY();
            double hY = bounds.getMaxY();
            points[0] = new Point2D.Double(lX, lY);
            points[1] = new Point2D.Double(hX, hY);
            points[2] = new Point2D.Double(lX, hY);
            points[3] = new Point2D.Double(hX, lY);
            double shrinkX = (hX - lX) / 5;
            double shrinkY = (hY - lY) / 5;
            points[4] = new Point2D.Double(lX+shrinkX, lY);
            points[5] = new Point2D.Double(hX-shrinkX, lY);
            points[6] = new Point2D.Double(lX+shrinkX, hY);
            points[7] = new Point2D.Double(hX-shrinkX, hY);
            points[8] = new Point2D.Double(lX, lY+shrinkY);
            points[9] = new Point2D.Double(lX, hY-shrinkY);
            points[10] = new Point2D.Double(hX, lY+shrinkY);
            points[11] = new Point2D.Double(hX, hY-shrinkY);
        }
        return points;
	}

    /**
	 * Method to handle a click in a window and select the appropriate objects.
	 * @param pt the coordinates of the click (in database units).
	 * @param wnd the window being examined.
	 * @param exclusively true if the currently selected object must remain selected.
	 * This happens during "outline edit" when the node doesn't change, just the point on it.
	 * @param another true to find another object under the point (when there are multiple ones).
	 * @param invert true to invert selection (add if not selected, remove if already selected).
	 * @param findPort true to also show the closest port on a selected node.
	 * @param findPoint true to also show the closest point on a selected outline node.
	 * @param findSpecial true to select hard-to-find objects.
	 * @param findText true to select text objects.
	 * The name of an unexpanded cell instance is always hard-to-select.
	 * Other objects are set this way by the user (although the cell-center is usually set this way).
	 */
	public Highlight2 findObject(Point2D pt, EditWindow wnd, boolean exclusively,
		boolean another, boolean invert, boolean findPort, boolean findPoint, boolean findSpecial, boolean findText)
	{
		// initialize
		double bestdist = Double.MAX_VALUE;
		boolean looping = false;

		// search the relevant objects in the circuit
		Cell cell = wnd.getCell();
        Rectangle2D bounds = new Rectangle2D.Double(pt.getX(), pt.getY(), 0, 0);
		List<Highlight2> underCursor = findAllInArea(this, cell, exclusively, another, findPort, findPoint, findSpecial, findText, bounds, wnd);
        Highlight2 found = null;

		// if nothing under the cursor, stop now
		if (underCursor.size() == 0)
		{
			if (!invert)
			{
				clear();
				finished();
			}
			return found;
		}

        // get last selected object. Next selected object should be related
        Highlight2 lastSelected = getLastSelected(underCursor);

        if (lastSelected != null) {
            //printHighlightList(underCursor);
            // sort under cursor by relevance to lastSelected. first object is most relevant.
            List<Highlight2> newUnderCursor = new ArrayList<Highlight2>();
            while (!underCursor.isEmpty()) {
                Highlight2 h = getSimiliarHighlight(underCursor, lastSelected);
                newUnderCursor.add(h);
                underCursor.remove(h);
            }
            underCursor = newUnderCursor;
        }

		// multiple objects under the cursor
		if (underCursor.size() > 1 && another)
		{
            // I don't think you should loop and get getHighlight() every time
                List<Highlight2> highlightList = getHighlights();
			for(int j=0; j<getNumHighlights(); j++)
			{
				Highlight2 oldHigh = highlightList.get(j);
				for(int i=0; i<underCursor.size(); i++)
				{
					if (oldHigh.sameThing(underCursor.get(i)))
					{
						// found the same thing: loop
						if (invert)
						{
							remove(oldHigh);
						} else
						{
							clear(false);
						}
						if (i < underCursor.size()-1)
						{
							found = (underCursor.get(i+1));
						} else
						{
							found = (underCursor.get(0));
						}
                        addHighlight(found);
						finished();
						return found;
					}
				}
			}
		}

		// just use the first in the list
        found = underCursor.get(0);
		if (invert)
		{
            List<Highlight2> highlightList = getHighlights();
            for (Highlight2 h : highlightList)
			{
				if (found.sameThing(h))
				{
					remove(h);
					finished();
					return found; // return 1;
				}
			}
		} else
		{
			clear();
		}
        addHighlight(found);
        finished();

		return found;
	}

//    private void printHighlightList(List<Highlight> highs) {
//        int i = 0;
//        for (Highlight h : highs) {
//            System.out.println("highlight "+i+": "+h.getElectricObject());
//            i++;
//        }
//    }

    /**
	 * Method to search a Cell for all objects at a point.
	 * @param cell the cell to search.
	 * @param exclusively true if the currently selected object must remain selected.
	 * This happens during "outline edit" when the node doesn't change, just the point on it.
	 * @param another true to find another object under the point (when there are multiple ones).
	 * @param findPort true to also show the closest port on a selected node.
	 * @param findPoint true to also show the closest point on a selected outline node.
	 * @param findSpecial true to select hard-to-find objects.
	 * @param findText true to select text objects.
	 * The name of an unexpanded cell instance is always hard-to-select.
	 * Other objects are set this way by the user (although the cell-center is usually set this way).
	 * @param bounds the area of the search (in database units).
	 * @param wnd the window being examined (null to ignore window scaling).
	 * @return a list of Highlight objects.
	 * The list is ordered by importance, so the deault action is to select the first entry.
	 */
	public static List<Highlight2> findAllInArea(Highlighter highlighter, Cell cell, boolean exclusively, boolean another, boolean findPort,
		 boolean findPoint, boolean findSpecial, boolean findText, Rectangle2D bounds, EditWindow wnd)
	{
		// make a list of things under the cursor
		List<Highlight2> list = new ArrayList<Highlight2>();
		boolean areaMustEnclose = User.isDraggingMustEncloseObjects();

		// this is the distance from an object that is necessary for a "direct hit"
		double directHitDist = Double.MIN_VALUE;
		if (wnd != null)
		{
			Point2D extra = wnd.deltaScreenToDatabase(EXACTSELECTDISTANCE, EXACTSELECTDISTANCE);
			directHitDist = Math.abs(extra.getX()); // + 0.4;
		}

        if (!Job.acquireExamineLock(false)) return list;

        try {
            // look for text if a window was given
            if (findText && wnd != null)
            {
                // start by examining all text on this Cell
                if (User.isTextVisibilityOnCell())
                {
                    Poly [] polys = cell.getAllText(findSpecial, wnd);
                    if (polys != null)
                    {
                        for(int i=0; i<polys.length; i++)
                        {
                            Poly poly = polys[i];
                            if (poly == null) continue;
                            if (poly.setExactTextBounds(wnd, cell)) continue;

                            // ignore areaMustEnclose if bounds is size 0,0
                            if (areaMustEnclose && (bounds.getHeight() > 0 || bounds.getWidth() > 0))
                            {
                                if (!poly.isInside(bounds)) continue;
                            } else
                            {
                                if (poly.polyDistance(bounds) >= directHitDist) continue;
                            }
                            HighlightText h = new HighlightText(cell, cell, poly.getDisplayedText().getVariableKey());
//                            Highlight h = new Highlight(Highlight.Type.TEXT, cell, cell);
//                            h.setVar(poly.getVariable());
                            list.add(h);
                        }
                    }
                }

                // next examine all text on nodes in the cell
                for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
                {
                    NodeInst ni = it.next();
                    if (Job.getDebug() && ni == null)
                    {
                        System.out.println("Something is wrong in Highlighter:findAllInArea");
                        continue;
                    }
                    AffineTransform trans = ni.rotateOut();
                    EditWindow subWnd = wnd;
                    Poly [] polys = ni.getAllText(findSpecial, wnd);
                    if (polys == null) continue;
                    for(int i=0; i<polys.length; i++)
                    {
                        Poly poly = polys[i];
                        if (poly == null) continue;

                        // "transform" used to follow "setExactTextBounds"
                        poly.transform(trans);
                        if (poly.setExactTextBounds(wnd, ni)) continue;
//						poly.transform(trans);

                        // ignore areaMustEnclose if bounds is size 0,0
                        if (areaMustEnclose && (bounds.getHeight() > 0 || bounds.getWidth() > 0))
                        {
                            if (!poly.isInside(bounds)) continue;
                        } else
                        {
                            double hitdist = poly.polyDistance(bounds);
                            if (hitdist >= directHitDist) continue;
                        }
                        ElectricObject obj = ni;
                        if (poly.getPort() != null)
                        {
                            PortProto pp = poly.getPort();
							if (pp instanceof Export)
                               obj = (Export)pp;
                            for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
                            {
                                PortInst pi = pIt.next();
                                if (pi.getPortProto() == pp)
                                {
                                    obj = pi;
                                    break;
                                }
                            }
                        }
                        HighlightText h = new HighlightText(obj, cell, poly.getDisplayedText().getVariableKey());
                        list.add(h);
                    }
                }

                // next examine all text on arcs in the cell
                for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
                {
                    ArcInst ai = it.next();
                    if (User.isTextVisibilityOnArc())
                    {
                        Poly [] polys = ai.getAllText(findSpecial, wnd);
                        if (polys == null) continue;
                        for(int i=0; i<polys.length; i++)
                        {
                            Poly poly = polys[i];
                            if (poly.setExactTextBounds(wnd, ai)) continue;

                            // ignore areaMustEnclose if bounds is size 0,0
                            if (areaMustEnclose && (bounds.getHeight() > 0 || bounds.getWidth() > 0))
                            {
                                if (!poly.isInside(bounds)) continue;
                            } else
                            {
                                if (poly.polyDistance(bounds) >= directHitDist) continue;
                            }
                            HighlightText h = new HighlightText(ai, cell, poly.getDisplayedText().getVariableKey());
                            list.add(h);
                        }
                    }
                }
            }

            if (exclusively)
            {
                // special case: only review what is already highlighted
                for(Highlight2 h : highlighter.getHighlights())
                {
                    if (!(h instanceof HighlightEOBJ)) continue;
                    ElectricObject eobj = h.getElectricObject();
                    if (eobj instanceof PortInst) eobj = ((PortInst)eobj).getNodeInst();
                    if (eobj instanceof NodeInst)
                    {
                        h = checkOutObject((Geometric)eobj, findPort, findPoint, findSpecial, bounds, wnd, Double.MAX_VALUE, areaMustEnclose);
                        if (h != null) list.add(h);
                    }
                }
                Job.releaseExamineLock();
                return list;
            }

            // determine proper area to search
            Rectangle2D searchArea = new Rectangle2D.Double(bounds.getMinX() - directHitDist,
                bounds.getMinY() - directHitDist, bounds.getWidth()+directHitDist*2, bounds.getHeight()+directHitDist*2);

            // now do 3 phases of examination: cells, arcs, then primitive nodes
            for(int phase=0; phase<3; phase++)
            {
                // ignore cells if requested
//                if (phase == 0 && !findSpecial && !User.isEasySelectionOfCellInstances()) continue;

                // examine everything in the area
                for(Iterator<Geometric> it = cell.searchIterator(searchArea); it.hasNext(); )
                {
                    Geometric geom = it.next();
                    Highlight2 h;
                    switch (phase)
                    {
                        case 0:			// check primitive nodes
                            if (!(geom instanceof NodeInst)) break;
                            if (((NodeInst)geom).isCellInstance()) break;
                            h = checkOutObject(geom, findPort, findPoint, findSpecial, bounds, wnd, directHitDist, areaMustEnclose);
                            if (h != null) list.add(h);
                            break;
                        case 1:			// check Cell instances
                            if (!findSpecial && !User.isEasySelectionOfCellInstances()) break; // ignore cells if requested
                            if (!(geom instanceof NodeInst)) break;
                            if (!((NodeInst)geom).isCellInstance()) break;
                            h = checkOutObject(geom, findPort, findPoint, findSpecial, bounds, wnd, directHitDist, areaMustEnclose);
                            if (h != null) list.add(h);
                            break;
                        case 2:			// check arcs
                            if (!(geom instanceof ArcInst)) break;
                            h = checkOutObject(geom, findPort, findPoint, findSpecial, bounds, wnd, directHitDist, areaMustEnclose);
                            if (h != null) list.add(h);
                            break;
                    }
                }
            }
            Job.releaseExamineLock();
        } catch (Error e) {
            Job.releaseExamineLock();
            throw e;
        }
        return list;
	}

    /**
	 * Method to determine whether an object is in a bounds.
	 * @param geom the Geometric being tested for selection.
	 * @param findPort true if a port should be selected with a NodeInst.
	 * @param findPoint true if a point should be selected with an outline NodeInst.
	 * @param findSpecial true if hard-to-select and other special selection is being done.
	 * @param bounds the selected area or point.
	 * @param wnd the window being examined (null to ignore window scaling).
	 * @param directHitDist the slop area to forgive when searching (a few pixels in screen space, transformed to database units).
	 * @param areaMustEnclose true if the object must be completely inside of the selection area.
	 * @return a Highlight that defines the object, or null if the point is not over any part of this object.
	 */
	public static Highlight2 checkOutObject(Geometric geom, boolean findPort, boolean findPoint, boolean findSpecial, Rectangle2D bounds,
		EditWindow wnd, double directHitDist, boolean areaMustEnclose)
	{
		if (geom instanceof NodeInst)
		{
			// examine a node object
			NodeInst ni = (NodeInst)geom;

			// do not "find" hard-to-find nodes if "findSpecial" is not set
			boolean hardToSelect = ni.isHardSelect();
			if (ni.isCellInstance())
			{
				if (!User.isEasySelectionOfCellInstances()) hardToSelect = true;
			} else
			{
				// do not include primitives that have all layers invisible
				PrimitiveNode np = (PrimitiveNode)ni.getProto();
				if (np.isNodeInvisible()) return null;
			}
			if (!findSpecial && hardToSelect) return null;

			// do not "find" Invisible-Pins if they have text or exports
			if (ni.isInvisiblePinWithText()) return null;

			// ignore areaMustEnclose if bounds is size 0,0
	        if (areaMustEnclose && (bounds.getHeight() > 0 || bounds.getWidth() > 0))
			{
	        	Poly poly = Highlight2.getNodeInstOutline(ni);
	            if (poly == null) return null;
	   			if (!poly.isInside(bounds)) return null;
//				return new Highlight(Highlight.Type.EOBJ, geom, geom.getParent());
                return new HighlightEOBJ(geom, geom.getParent(), true, -1);
			}

			// get the distance to the object
			double dist = distToNode(bounds, ni, wnd);

			// direct hit
			if (dist <= directHitDist)
			{
//				Highlight h = new Highlight(Highlight.Type.EOBJ, null, geom.getParent());
                HighlightEOBJ h = new HighlightEOBJ(null, geom.getParent(), true, -1);
				ElectricObject eobj = geom;

				// add the closest port
				if (findPort)
				{
					double bestDist = Double.MAX_VALUE;
					PortInst bestPort = null;
					for(Iterator<PortInst> it = ni.getPortInsts(); it.hasNext(); )
					{
						PortInst pi = it.next();
						Poly poly = pi.getPoly();
						dist = poly.polyDistance(bounds);
						if (dist < bestDist)
						{
							bestDist = dist;
							bestPort = pi;
						}
					}
					if (bestPort != null) eobj = bestPort;
				}

				// add the closest point
				if (findPoint)
				{
					Point2D [] points = ni.getTrace();
					Point2D cursor = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
					if (points != null)
					{
						double bestDist = Double.MAX_VALUE;
						int bestPoint = -1;
						AffineTransform trans = ni.rotateOutAboutTrueCenter();
						for(int i=0; i<points.length; i++)
						{
							Point2D pt = new Point2D.Double(ni.getAnchorCenterX() + points[i].getX(),
								ni.getAnchorCenterY() + points[i].getY());
							trans.transform(pt, pt);
							dist = pt.distance(cursor);
							if (dist < bestDist)
							{
								bestDist = dist;
								bestPoint = i;
							}
						}
						if (bestPoint >= 0) h.point = bestPoint;
					}
				}
				h.eobj = eobj;
				return h;
			}
		} else
		{
			// examine an arc object
			ArcInst ai = (ArcInst)geom;

			// do not "find" hard-to-find arcs if "findSpecial" is not set
			if (!findSpecial && ai.isHardSelect()) return null;

			// do not include arcs that have all layers invisible
			if (ai.getProto().isArcInvisible()) return null;

			// ignore areaMustEnclose if bounds is size 0,0
	        if (areaMustEnclose && (bounds.getHeight() > 0 || bounds.getWidth() > 0))
			{
	        	Poly poly = ai.makePoly(ai.getWidth() - ai.getProto().getWidthOffset(), Poly.Type.CLOSED);
	            if (poly == null) return null;
	   			if (!poly.isInside(bounds)) return null;
//				Highlight h = new Highlight(Highlight.Type.EOBJ, geom, geom.getParent());
                Highlight2 h = new HighlightEOBJ(geom, geom.getParent(), true, -1);
				return h;
			}

			// get distance to arc
			double dist = distToArc(bounds, ai, wnd);

			// direct hit
			if (dist <= directHitDist)
			{
//				Highlight h = new Highlight(Highlight.Type.EOBJ, geom, geom.getParent());
                Highlight2 h = new HighlightEOBJ(geom, geom.getParent(), true, -1);
				return h;
			}
		}
		return null;
	}

    /**
     * Chooses a single Highlight from the list of Highlights 'highlights' that is most
     * similar to Highlight 'exampleHigh'.
     * @param highlights a list of Highlight Objects
     * @param exampleHigh the Highlight that serves as an example of what type
     * of Highlight should be retrieved from the highlights list.
     */
    public static Highlight2 getSimiliarHighlight(List<Highlight2> highlights, Highlight2 exampleHigh) {
        if (highlights.size() == 0) return null;
        if (exampleHigh == null) return highlights.get(0);

        // get Highlights of the same type
        List<Highlight2> sameTypes = new ArrayList<Highlight2>();
        for (Highlight2 h : highlights) {
            if (h.getClass() == exampleHigh.getClass())
            {
                sameTypes.add(h);
            }
        }
        // if only one, just return it
        if (sameTypes.size() == 1) return sameTypes.get(0);
        // if none of same type, just return first in list of all highlights
        if (sameTypes.size() == 0) return highlights.get(0);

        // we have different rules depending on the type
        if (exampleHigh.isHighlightEOBJ())
        {
            // get Highlights of the same electric object
            List<Highlight2> sameEObj = new ArrayList<Highlight2>();
            for (Highlight2 h : sameTypes) {
                if (h.getElectricObject().getClass() == exampleHigh.getElectricObject().getClass())
                    sameEObj.add(h);
            }
            // if only one of same object, return it
            if (sameEObj.size() == 1) return sameEObj.get(0);

            // if more than one of the same ElectricObject, make decisions
            // for some of the common choices
            if (sameEObj.size() > 0) {
                // for PortInsts (Mouse GUI always sets "findPort", so we don't care about NodeInsts, only PortInsts)
                if (exampleHigh.getElectricObject().getClass() == PortInst.class) {
                    // see if we can find a port on the same NodeProto
                    PortInst exPi = (PortInst)exampleHigh.getElectricObject();
                    NodeProto exNp = exPi.getNodeInst().getProto();
                    for (Highlight2 h : sameEObj) {
                        PortInst pi = (PortInst)h.getElectricObject();
                        NodeProto np = pi.getNodeInst().getProto();
                        if (np == exNp) return h;
                    }
                    // nothing with the same prototype, see if we can find a port that can connect to it
                    for (Highlight2 h : sameEObj) {
                        PortInst pi = (PortInst)h.getElectricObject();
                        if (Router.getArcToUse(exPi.getPortProto(), pi.getPortProto()) != null) {
                            return h;
                        }
                    }
                }
                // for ArcInsts, see if we can find an arc with the same ArcProto
                if (exampleHigh.getElectricObject().getClass() == ArcInst.class) {
                    ArcInst exAi = (ArcInst)exampleHigh.getElectricObject();
                    ArcProto exAp = exAi.getProto();
                    for (Highlight2 h : sameEObj) {
                        ArcInst ai = (ArcInst)h.getElectricObject();
                        ArcProto ap = ai.getProto();
                        if (exAp == ap) return h;
                    }
                }
            } else { // (sameEObj.size() == 0)
                // no Highlights of same object. See if we can find another object that will connect
                // one must be an ArcInst and one must be a PortInst. Other combos already handled above.
                ArcInst exAi = null;
                PortInst exPi = null;
                if (exampleHigh.getElectricObject().getClass() == ArcInst.class)
                    exAi = (ArcInst)exampleHigh.getElectricObject();
                if (exampleHigh.getElectricObject().getClass() == PortInst.class)
                    exPi = (PortInst)exampleHigh.getElectricObject();
                for (Highlight2 h : sameTypes) {
                    // reset ai and pi
                    ArcInst ai = exAi;
                    PortInst pi = exPi;
                    if (h.getElectricObject().getClass() == ArcInst.class)
                        ai = (ArcInst)h.getElectricObject();
                    if (h.getElectricObject().getClass() == PortInst.class)
                        pi = (PortInst)h.getElectricObject();
                    // if either null, can't connect these two EObjs
                    if ((ai == null) || (pi == null)) continue;
                    if (pi.getPortProto().connectsTo(ai.getProto())) return h;
                }
            }
            // couldn't find a highlight based on connectivity or same object class
            // return first in list if possible
            if (sameEObj.size() > 0) return sameEObj.get(0);
        }
        // return first in list (list empty case handled above)
        return sameTypes.get(0);
    }

    /**
	 * Method to return the distance from a bound to a NodeInst.
	 * @param bounds the bounds in question.
	 * @param ni the NodeInst.
	 * @param wnd the window being examined (null to ignore text/window scaling).
	 * @return the distance from the bounds to the NodeInst.
	 * Negative values are direct hits.
	 */
	private static double distToNode(Rectangle2D bounds, NodeInst ni, EditWindow wnd)
	{
		AffineTransform trans = ni.rotateOut();

		NodeProto np = ni.getProto();
		Poly nodePoly = null;
		if (!ni.isCellInstance())
		{
			// special case for MOS transistors: examine the gate/active tabs
            // special case for RESIST in layout  (fun == PrimitiveNode.Function.RESIST and PrimitiveNode.POLYGONAL
			PrimitiveNode.Function fun = np.getFunction();
			if (fun == PrimitiveNode.Function.TRANMOS || fun == PrimitiveNode.Function.TRAPMOS || fun == PrimitiveNode.Function.TRADMOS ||
                (!ni.isCellInstance() && fun.isResistor() && ((PrimitiveNode)np).getSpecialType() == PrimitiveNode.POLYGONAL))
			{
				Technology tech = np.getTechnology();
				Poly [] polys = tech.getShapeOfNode(ni, wnd);
				double bestDist = Double.MAX_VALUE;
				for(int box=0; box<polys.length; box++)
				{
					Poly poly = polys[box];
					Layer layer = poly.getLayer();
					if (layer == null) continue;
					Layer.Function lf = layer.getFunction();
					if (!lf.isPoly() && !lf.isDiff()) continue;
					poly.transform(trans);
                    // ignore areaMustEnclose if bounds is size 0,0
//					if (areaMustEnclose && (bounds.getHeight() > 0 || bounds.getWidth() > 0))
//					{
//						if (!poly.isInside(bounds)) continue;
//					} else
//					{
//						if (poly.polyDistance(bounds) >= directHitDist) continue;
//					}
					double dist = poly.polyDistance(bounds);
					if (dist < bestDist) bestDist = dist;
				}
				return bestDist;
			}

			// special case for 1-polygon primitives: check precise distance to cursor
			if (((PrimitiveNode)np).isEdgeSelect())
			{
				Technology tech = np.getTechnology();
				Poly [] polys = tech.getShapeOfNode(ni, wnd);
				double bestDist = Double.MAX_VALUE;
				for(int box=0; box<polys.length; box++)
				{
					Poly poly = polys[box];
					poly.transform(trans);
					double dist = poly.polyDistance(bounds);
					if (dist < bestDist) bestDist = dist;
				}
				return bestDist;
			}

			// get the bounds of the node in a polygon
			SizeOffset so = ni.getSizeOffset();
			double lX = ni.getAnchorCenterX() - ni.getXSize()/2 + so.getLowXOffset();
			double hX = ni.getAnchorCenterX() + ni.getXSize()/2 - so.getHighXOffset();
			double lY = ni.getAnchorCenterY() - ni.getYSize()/2 + so.getLowYOffset();
			double hY = ni.getAnchorCenterY() + ni.getYSize()/2 - so.getHighYOffset();
			nodePoly = new Poly((lX + hX) / 2, (lY + hY) / 2, hX-lX, hY-lY);
		} else
		{
			// cell instance
			Cell subCell = (Cell)np;
			Rectangle2D instBounds = subCell.getBounds();
			nodePoly = new Poly(ni.getAnchorCenterX() + instBounds.getCenterX(),
				ni.getAnchorCenterY() + instBounds.getCenterY(), instBounds.getWidth(), instBounds.getHeight());
		}

		AffineTransform pureTrans = ni.rotateOut();
		nodePoly.transform(pureTrans);
		nodePoly.setStyle(Poly.Type.FILLED);
		double dist = nodePoly.polyDistance(bounds);
		return dist;
	}

    /**
	 * Method to return the distance from a bounds to an ArcInst.
	 * @param bounds the bounds in question.
	 * @param ai the ArcInst.
	 * @param wnd the window being examined.
	 * @return the distance from the bounds to the ArcInst.
	 * Negative values are direct hits or intersections.
	 */
	private static double distToArc(Rectangle2D bounds, ArcInst ai, EditWindow wnd)
	{
		ArcProto ap = ai.getProto();

		// if arc is selectable precisely, check distance to cursor
		if (ap.isEdgeSelect())
		{
			Technology tech = ap.getTechnology();
			Poly [] polys = tech.getShapeOfArc(ai, wnd);
			double bestDist = Double.MAX_VALUE;
			for(int box=0; box<polys.length; box++)
			{
				Poly poly = polys[box];
				double dist = poly.polyDistance(bounds);
				if (dist < bestDist) bestDist = dist;
			}
			return bestDist;
		}

		// standard distance to the arc
		double wid = ai.getWidth() - ai.getProto().getWidthOffset();
		if (DBMath.doublesEqual(wid, 0)) wid = 1;
		Poly poly = ai.makePoly(wid, Poly.Type.FILLED);
		return poly.polyDistance(bounds);
	}

    public void databaseChanged(DatabaseChangeEvent e) {
        // see if anything we care about changed
        finished();
    }

//     public void databaseEndChangeBatch(Undo.ChangeBatch batch) {
//         // see if anything we care about changed
//         finished();
//     }

//     public void databaseChanged(Undo.Change evt) {}

//     public boolean isGUIListener() {
//         return true;
//     }
}
