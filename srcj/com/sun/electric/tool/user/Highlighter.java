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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.change.Undo;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.WaveformWindow;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.routing.Router;
import com.sun.electric.tool.Job;

import javax.swing.*;
import java.util.*;
import java.util.List;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Aug 5, 2004
 * Time: 9:25:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class Highlighter implements DatabaseChangeListener {

    //public static Highlighter global = new Highlighter();
    private static Highlighter currentHighlighter = null;

    /** Screen offset for display of highlighting. */			private int highOffX; private int highOffY;
    /** the highlighted objects. */								private List highlightList;
    /** the stack of highlights. */								private List highlightStack;
    /** true if highlights have changed recently */             private boolean changed;
    /** List of HighlightListeners */                           private List highlightListeners;
    /** last object selected before last clear() */             private Highlight lastHighlightListEndObj;
    /** what was the last level of "showNetwork" */             private int showNetworkLevel;
	/** the type of highlighter */                              private int type;
	/** the WindowFrame associated with the highlighter */      private WindowFrame wf;

    /** the selection highlighter type */       public static final int SELECT_HIGHLIGHTER = 0;
    /** the mouse over highlighter type */      public static final int MOUSEOVER_HIGHLIGHTER = 1;

    private static final int EXACTSELECTDISTANCE = 5;

    /**
     * Create a new Highlighter object
     * @param type
     */
    public Highlighter(int type, WindowFrame wf) {
        highOffX = highOffY = 0;
        highlightList = new ArrayList();
        highlightStack = new ArrayList();
        highlightListeners = new ArrayList();
        changed = false;
        Undo.addDatabaseChangeListener(this);
        if (currentHighlighter == null) currentHighlighter = this;
        lastHighlightListEndObj = null;
        showNetworkLevel = 0;
		this.type = type;
		this.wf = wf;
    }

    /**
     * Destructor
     */
    public void delete() {
        Undo.removeDatabaseChangeListener(this);
    }

    /**
	 * Method to add an ElectricObject to the list of highlighted objects.
	 * @param eobj the ElectricObject to add to the list of highlighted objects.
	 * @param cell the Cell in which the ElectricObject resides.
	 * @return the newly created Highlight object.
	 */
    public Highlight addElectricObject(ElectricObject eobj, Cell cell)
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
	public Highlight addElectricObject(ElectricObject eobj, Cell cell, boolean highlightConnected)
	{
		Highlight h = new Highlight(Highlight.Type.EOBJ, eobj, cell);
        h.setHighlightConnected(highlightConnected);

		addHighlight(h);
		return h;
	}

    /**
	 * Method to add a text selection to the list of highlighted objects.
	 * @param cell the Cell in which this area resides.
	 * @param var the Variable associated with the text (text is then a visual of that variable).
	 * @param name the Name associated with the text (for the name of Nodes and Arcs).
	 * @return the newly created Highlight object.
	 */
	public Highlight addText(ElectricObject eobj, Cell cell, Variable var, Name name)
	{
		Highlight h = new Highlight(Highlight.Type.TEXT, eobj, cell);
        h.setVar(var);
        h.setName(name);

        addHighlight(h);
		return h;
	}

    /**
	 * Method to add a message display to the list of highlighted objects.
	 * @param cell the Cell in which this area resides.
	 * @param message the String to display.
	 * @param loc the location of the string (in database units).
	 * @return the newly created Highlight object.
	 */
	public Highlight addMessage(Cell cell, String message, Point2D loc)
	{
		Highlight h = new Highlight(Highlight.Type.MESSAGE, null, cell);
        h.setMessage(message);
		h.setLocation(loc);

        addHighlight(h);
		return h;
	}

    /**
	 * Method to add an area to the list of highlighted objects.
	 * @param area the Rectangular area to add to the list of highlighted objects.
	 * @param cell the Cell in which this area resides.
	 * @return the newly created Highlight object.
	 */
	public Highlight addArea(Rectangle2D area, Cell cell)
	{
		Highlight h = new Highlight(Highlight.Type.BBOX, null, cell);
		Rectangle2D bounds = new Rectangle2D.Double();
		bounds.setRect(area);
        h.setBounds(bounds);

        addHighlight(h);
		return h;
	}

	/**
	 * Method to generic Object.
	 * @param obj object to add.
	 * @param cell the Cell in which this object resides.
	 * @return the newly created Highlight object.
	 */
	public Highlight addObject(Object obj, Highlight.Type type, Cell cell)
	{
		Highlight h = new Highlight(type, null, cell);
		h.setObject(obj);

        addHighlight(h);
		return h;
	}

    /**
	 * Method to add a line to the list of highlighted objects.
	 * @param start the start point of the line to add to the list of highlighted objects.
	 * @param end the end point of the line to add to the list of highlighted objects.
	 * @param cell the Cell in which this line resides.
	 * @return the newly created Highlight object.
	 */
	public Highlight addLine(Point2D start, Point2D end, Cell cell)
	{
		Highlight h = new Highlight(Highlight.Type.LINE, null, cell);
		h.setLineStart(new Point2D.Double(start.getX(), start.getY()));
		h.setLineEnd(new Point2D.Double(end.getX(), end.getY()));

        addHighlight(h);
		return h;
	}

    /**
	 * Method to add a line to the list of highlighted objects.
	 * @param start the start point of the line to add to the list of highlighted objects.
	 * @param end the end point of the line to add to the list of highlighted objects.
	 * @param cell the Cell in which this line resides.
	 * @return the newly created Highlight object.
	 */
	public Highlight addThickLine(Point2D start, Point2D end, Point2D center, Cell cell)
	{
		Highlight h = new Highlight(Highlight.Type.THICKLINE, null, cell);
		h.setLineStart(new Point2D.Double(start.getX(), start.getY()));
		h.setLineEnd(new Point2D.Double(end.getX(), end.getY()));
		h.setCenter(new Point2D.Double(center.getX(), center.getY()));

        addHighlight(h);
		return h;
	}

    /**
     * Method to add a Poly to the list of highlighted objects
     * @param poly the poly to add
     * @param cell the cell in which to display the poly
     * @param color the color to draw the poly with (if null, uses default)
     * @return the newly created highlight object
     */
    public Highlight addPoly(Poly poly, Cell cell, Color color)
    {
        Highlight h = new Highlight(Highlight.Type.POLY, null, cell);
        h.setPoly(poly);
        h.setColor(color);

        addHighlight(h);
        return h;
    }

    /**
	 * Method to add a network to the list of highlighted objects.
	 * Many arcs may be highlighted as a result.
	 * @param net the network to highlight.
	 * @param cell the Cell in which this line resides.
	 */
	public void addNetwork(JNetwork net, Cell cell)
	{
		Netlist netlist = cell.getUserNetlist();
        List highlights = NetworkHighlighter.getHighlights(cell, netlist, net, 0, 0);
        for (Iterator it = highlights.iterator(); it.hasNext(); ) {
            Highlight h = (Highlight)it.next();
            addHighlight(h);
        }
	}

    /**
     * This is the show network command. It is similar to addNetwork, however
     * each time it is used without first clearing
     * the highlighter, it shows connections to the network another level down
     * in the hierarchy.
     * @param nets list of JNetworks in current cell to show
     * @param cell the cell in which to create the highlights
     */
    public void showNetworks(Set nets, Netlist netlist, Cell cell) {
        if (showNetworkLevel == 0) clear();
        int count = 0;
        for (Iterator netIt = nets.iterator(); netIt.hasNext(); ) {
            JNetwork net = (JNetwork)netIt.next();
            if (showNetworkLevel == 0) System.out.println("Highlighting network "+net.describe());
            List highlights = NetworkHighlighter.getHighlights(cell, netlist, net,
                    showNetworkLevel, showNetworkLevel);
            for (Iterator it = highlights.iterator(); it.hasNext(); ) {
                Highlight h = (Highlight)it.next();
                addHighlight(h);
                count++;
            }
        }
        showNetworkLevel++;
        if (count == 0) {
            System.out.println("Nothing more in hierarchy on network(s) to show");
        }
    }

    /**
     * Add a Highlight
     */
    private synchronized void addHighlight(Highlight h) {
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
            lastHighlightListEndObj = (Highlight)highlightList.get(highlightList.size()-1);
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
            for (Iterator it = getHighlights().iterator(); it.hasNext(); ) {
                Highlight h = (Highlight)it.next();
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
		for(Iterator it = getHighlights().iterator(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() == Highlight.Type.EOBJ)
			{
				ElectricObject eobj = h.getElectricObject();
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
		    if (foundArcProto != null && !mixedArc) User.tool.setCurrentArcProto(foundArcProto);

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
     * Get the last object that was selected. If underCursor is not null,
     * if any of the Highlights in underCursor are currently highlighted, then
     * the last thing highlighted will be the last thing selected before the last
     * clear(). This is to be able to properly cycle through objects under the cursor.
     * @param underCursor a list of Highlights underCursor.
     * @return the last object that was selected
     */
    private synchronized Highlight getLastSelected(List underCursor) {
        List currentHighlights = getHighlights();               // not that this is a copy

        // check underCursor list
        for (Iterator igIt = underCursor.iterator(); igIt.hasNext(); ) {
            Highlight h = (Highlight)igIt.next();

            for (Iterator it = currentHighlights.iterator(); it.hasNext(); ) {
                Highlight curHigh = (Highlight)it.next();
                if (h.sameThing(curHigh)) {
                    return lastHighlightListEndObj;
                }
            }
        }

        if (currentHighlights.size() > 0) {
            return (Highlight)currentHighlights.get(currentHighlights.size()-1);
        } else {
            return lastHighlightListEndObj;
        }
    }

    /**
     * Inherits the last selected object from the specified highlighter.
     * This is a hack, don't use it.
     * @param highlighter
     */
    public void copyState(Highlighter highlighter) {
        clear();
        lastHighlightListEndObj = highlighter.lastHighlightListEndObj;
        for (Iterator it = highlighter.getHighlights().iterator(); it.hasNext(); ) {
            Highlight h = (Highlight)it.next();
            Highlight copy = (Highlight)h.clone();
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

        for (Iterator it = getHighlights().iterator(); it.hasNext(); ) {
            Highlight h = (Highlight)it.next();

            // only show highlights for the current cell
            if (h.getCell() == wnd.getCell()) {
                Color color = new Color(User.getColorHighlight());
                Stroke stroke = Highlight.solidLine;
                if (type == MOUSEOVER_HIGHLIGHTER) {
                    color = new Color(51, 255, 255);
                    stroke = Highlight.solidLine;
                    h.setHighlightConnected(false);
                }
                h.showHighlight(wnd, g, highOffX, highOffY, (num == 1), color, stroke);
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
    public synchronized void addHighlightListener(HighlightListener l) {
        highlightListeners.add(l);
    }

    /** Remove a Highlight listener */
    public synchronized void removeHighlightListener(HighlightListener l) {
        highlightListeners.remove(l);
    }

    /** Notify listeners that highlights have changed */
    private synchronized void fireHighlightChanged() {
        List listenersCopy = new ArrayList(highlightListeners);
        for (Iterator it = listenersCopy.iterator(); it.hasNext(); ) {
            HighlightListener l = (HighlightListener)it.next();
            l.highlightChanged(this);
        }
        changed = false;
    }

    /** Notify listeners that the current Highlighter has changed */
    private synchronized void fireHighlighterLostFocus(Highlighter highlighterGainedFocus) {
        List listenersCopy = new ArrayList(highlightListeners);
        for (Iterator it = listenersCopy.iterator(); it.hasNext(); ) {
            HighlightListener l = (HighlightListener)it.next();
            l.highlighterLostFocus(highlighterGainedFocus);
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
		List pushable = new ArrayList();
		for(Iterator it = highlightList.iterator(); it.hasNext(); )
			pushable.add(it.next());
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
		List popable = (List)highlightStack.get(stackSize-1);
		highlightStack.remove(stackSize-1);

		// validate each highlight as it is added
		clear();
		for(Iterator it = popable.iterator(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
            Highlight.Type type = h.getType();
            Cell cell = h.getCell();
            ElectricObject eobj = h.getElectricObject();
			if (type == Highlight.Type.EOBJ)
			{
				if (cell.objInCell(eobj))
				{
					Highlight newH = addElectricObject(eobj, cell);
					newH.setPoint(h.getPoint());
				}
			} else if (type == Highlight.Type.TEXT)
			{
				if (cell.objInCell(eobj))
				{
					Highlight newH = addText(eobj, cell, h.getVar(), h.getName());
				}
			} else if (type == Highlight.Type.BBOX)
			{
				Highlight newH = addArea(h.getBounds(), cell);
			} else if (type == Highlight.Type.LINE)
			{
				Highlight newH = addLine(h.getLineStart(), h.getLineEnd(), cell);
			} else if (type == Highlight.Type.THICKLINE)
			{
				Highlight newH = addThickLine(h.getLineStart(), h.getLineEnd(), h.getCenter(), cell);
			} else if (type == Highlight.Type.MESSAGE)
			{
				Highlight newH = addMessage(cell, h.getMessage(), h.getCenter());
			}
		}
		finished();
	}

    /**
     * Removes a Highlight object from the current set of highlights.
     * @param h the Highlight to remove
     */
    public synchronized void remove(Highlight h) {
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
	public synchronized List getHighlights() {
        ArrayList highlightsCopy = new ArrayList(highlightList);
        return highlightsCopy;
    }

    /**
	 * Method to load a list of Highlights into the highlighting.
	 * @param newHighlights a List of Highlight objects.
	 */
	public synchronized void setHighlightList(List newHighlights)
	{
        clear();
		for(Iterator it = newHighlights.iterator(); it.hasNext(); )
		{
			highlightList.add(it.next());
		}
        changed = true;
	}

    /**
	 * Method to return a List of all highlighted ElectricObjects.
	 * @param wantNodes true if NodeInsts should be included in the list.
	 * @param wantArcs true if ArcInsts should be included in the list.
	 * @return a list with the highlighted ElectricObjects.
	 */
	public List getHighlightedEObjs(boolean wantNodes, boolean wantArcs)
	{
		// now place the objects in the list
		List highlightedGeoms = new ArrayList();
		for(Iterator it = getHighlights().iterator(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();

			if (h.getType() == Highlight.Type.EOBJ || h.getType() == Highlight.Type.TEXT)
			{
				Geometric geom = h.getGeometric();
				if (geom == null) continue;
				if (!wantNodes && geom instanceof NodeInst) continue;
				if (!wantArcs && geom instanceof ArcInst) continue;

				if (highlightedGeoms.contains(geom)) continue;
				highlightedGeoms.add(geom);
			}
			if (h.getType() == Highlight.Type.BBOX)
			{
				List inArea = findAllInArea(h.getCell(), false, false, false, false, false, false, h.getBounds(), null);
				for(Iterator ait = inArea.iterator(); ait.hasNext(); )
				{
					Highlight ah = (Highlight)ait.next();
					if (ah.getType() != Highlight.Type.EOBJ) continue;
					ElectricObject eobj = ah.getElectricObject();
					if (!wantNodes)
					{
						if (eobj instanceof NodeInst || eobj instanceof PortInst) continue;
					}
					if (!wantArcs && eobj instanceof ArcInst) continue;
					if (eobj instanceof PortInst) eobj = ((PortInst)eobj).getNodeInst();
					highlightedGeoms.add(eobj);
				}
			}
		}
		return highlightedGeoms;
	}

    /**
	 * Method to return a set of the currently selected networks.
	 * @return a set of the currently selected networks.
	 * If there are no selected networks, the list is empty.
	 */
	public Set getHighlightedNetworks()
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf.getContent() instanceof WaveformWindow)
		{
			WaveformWindow ww = (WaveformWindow)wf.getContent();
			return ww.getHighlightedNetworks();
		}
		Set nets = new HashSet();
		Cell cell = WindowFrame.getCurrentCell();
		if (cell != null)
		{
			Netlist netlist = cell.getUserNetlist();
			for(Iterator it = getHighlights().iterator(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() == Highlight.Type.EOBJ)
				{
					ElectricObject eObj = h.getElectricObject();
					if (eObj instanceof PortInst)
					{
						PortInst pi = (PortInst)eObj;
						JNetwork net = netlist.getNetwork(pi);
						if (net != null)
							nets.add(net);
						else
						{
							// if port is isolated, grab all nets
							if (pi.getPortProto().isIsolated())
							{
								for(Iterator aIt = pi.getNodeInst().getConnections(); aIt.hasNext(); )
								{
									Connection con = (Connection)aIt.next();
									ArcInst ai = con.getArc();
									net = netlist.getNetwork(ai, 0);
									if (net != null) nets.add(net);
								}
							}
						}
					} else if (eObj instanceof NodeInst)
					{
						NodeInst ni = (NodeInst)eObj;
                        if (ni.getNumPortInsts() == 1) {
                            PortInst pi = ni.getOnlyPortInst();
                            if (pi != null)
                            {
                                JNetwork net = netlist.getNetwork(pi);
                                if (net != null) nets.add(net);
                            }
                        }
					} else if (eObj instanceof ArcInst)
					{
						ArcInst ai = (ArcInst)eObj;
						int width = netlist.getBusWidth(ai);
						for(int i=0; i<width; i++)
						{
							JNetwork net = netlist.getNetwork((ArcInst)eObj, i);
							if (net != null) nets.add(net);
						}
					}
				} else if (h.getType() == Highlight.Type.TEXT)
				{
					if (h.getVar() == null && h.getName() == null &&
						h.getElectricObject() instanceof Export)
					{
						Export pp = (Export)h.getElectricObject();
						int width = netlist.getBusWidth(pp);
						for(int i=0; i<width; i++)
						{
							JNetwork net = netlist.getNetwork(pp, i);
							if (net != null) nets.add(net);
						}
					}
				}
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
	public List getHighlightedText(boolean unique)
	{
		// now place the objects in the list
		List highlightedText = new ArrayList();
		for(Iterator it = getHighlights().iterator(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();

			if (h.getType() == Highlight.Type.TEXT)
			{
				if (highlightedText.contains(h)) continue;

				// if this text is on a selected object, don't include the text
				if (unique)
				{
					ElectricObject eobj = h.getElectricObject();
					ElectricObject onObj = null;
					if (h.getVar() != null)
					{
						if (eobj instanceof Export)
						{
							onObj = ((Export)eobj).getOriginalPort().getNodeInst();
						} else if (eobj instanceof PortInst)
						{
							onObj = ((PortInst)eobj).getNodeInst();
						} else if (eobj instanceof Geometric)
						{
							onObj = eobj;
						}
					} else
					{
						if (h.getName() != null)
						{
							if (eobj instanceof Geometric) onObj = eobj;
						} else
						{
							if (eobj instanceof Export)
							{
								onObj = ((Export)eobj).getOriginalPort().getNodeInst();
							} else
							{
								if (eobj instanceof NodeInst) onObj = eobj;
							}
						}
					}

					// now see if the object is in the list
					if (eobj != null)
					{
						boolean found = false;
						for(Iterator fIt = getHighlights().iterator(); fIt.hasNext(); )
						{
							Highlight oH = (Highlight)fIt.next();
							if (oH.getType() != Highlight.Type.EOBJ) continue;
							ElectricObject fobj = oH.getElectricObject();
							if (fobj instanceof PortInst) fobj = ((PortInst)fobj).getNodeInst();
							if (fobj == onObj) { found = true;   break; }
						}
						if (found) continue;
					}
				}

				// add this text
				highlightedText.add(h);
			}
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
		for(Iterator it = getHighlights().iterator(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();

			// find the bounds of this highlight
			Rectangle2D highBounds = null;
			if (h.getType() == Highlight.Type.EOBJ)
			{
				ElectricObject eobj = h.getElectricObject();
				if (eobj instanceof PortInst) eobj = ((PortInst)eobj).getNodeInst();
				if (eobj instanceof Geometric)
				{
					Geometric geom = (Geometric)eobj;
					highBounds = geom.getBounds();
				}
			} else if (h.getType() == Highlight.Type.TEXT)
			{
				if (wnd != null)
				{
					Poly poly = h.getElectricObject().computeTextPoly(wnd, h.getVar(), h.getName());
					if (poly != null) highBounds = poly.getBounds2D();
				}
			} else if (h.getType() == Highlight.Type.BBOX)
			{
				highBounds = h.getBounds();
			} else if (h.getType() == Highlight.Type.LINE || h.getType() == Highlight.Type.THICKLINE)
			{
                Point2D pt1 = h.getLineStart();
                Point2D pt2 = h.getLineEnd();
				double cX = (pt1.getX() + pt2.getX()) / 2;
				double cY = (pt1.getY() + pt2.getY()) / 2;
				double sX = Math.abs(pt1.getX() - pt2.getX());
				double sY = Math.abs(pt1.getY() - pt2.getY());
				highBounds = new Rectangle2D.Double(cX, cY, sX, sY);
			} else if (h.getType() == Highlight.Type.MESSAGE)
			{
				highBounds = new Rectangle2D.Double(h.getLocation().getX(), h.getLocation().getY(), 0, 0);
			}

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
	public Highlight getOneHighlight()
	{
		if (getNumHighlights() == 0)
		{
			System.out.println("Must select an object first");
			return null;
		}
		Highlight h = null;
		for(Iterator it = getHighlights().iterator(); it.hasNext(); )
		{
			Highlight theH = (Highlight)it.next();

            if (theH.getElectricObject() != null) return theH;

/*
			if (theH.type == Type.EOBJ)
			{
				if (h != null)
				{
					System.out.println("Must select only one object");
					return null;
				}
				h = theH;
			}
*/
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
		Highlight high = getOneHighlight();
		if (high == null) return null;
		if (high.getType() != Highlight.Type.EOBJ)
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
		List underCursor = findAllInArea(wnd.getCell(), false, false, false, false, findSpecial, true, searchArea, wnd);
		if (invertSelection)
		{
			for(Iterator it = underCursor.iterator(); it.hasNext(); )
			{
				Highlight newHigh = (Highlight)it.next();
				boolean found = false;
                for (Iterator it2 = getHighlights().iterator(); it2.hasNext(); ) {
                    Highlight oldHigh = (Highlight)it2.next();
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
	 * @return true if the point is over this Highlight.
	 */
	public boolean overHighlighted(EditWindow wnd, int x, int y)
	{
		for(Iterator it = getHighlights().iterator(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			Highlight.Type style = h.getType();
			if (style == Highlight.Type.TEXT)
			{
				Point2D start = wnd.screenToDatabase((int)x, (int)y);
				Poly poly = h.getElectricObject().computeTextPoly(wnd, h.getVar(), h.getName());
                if (poly != null)
				    if (poly.isInside(start)) return true;
			} else if (style == Highlight.Type.EOBJ)
			{
				Point2D slop = wnd.deltaScreenToDatabase(EXACTSELECTDISTANCE*2, EXACTSELECTDISTANCE*2);
				double directHitDist = slop.getX();
				Point2D start = wnd.screenToDatabase((int)x, (int)y);
				Rectangle2D searchArea = new Rectangle2D.Double(start.getX(), start.getY(), 0, 0);

				ElectricObject eobj = h.getElectricObject();
				if (eobj instanceof PortInst) eobj = ((PortInst)eobj).getNodeInst();
				if (eobj instanceof Geometric)
				{
					Highlight got = checkOutObject((Geometric)eobj, true, false, true, searchArea, wnd, directHitDist, false);
					if (got == null) continue;
					ElectricObject hObj = got.getElectricObject();
					ElectricObject hReal = hObj;
					if (hReal instanceof PortInst) hReal = ((PortInst)hReal).getNodeInst();
					for(Iterator sIt = getHighlights().iterator(); sIt.hasNext(); )
					{
						Highlight alreadyHighlighted = (Highlight)sIt.next();
						if (alreadyHighlighted.getType() != got.getType()) continue;
						ElectricObject aHObj = alreadyHighlighted.getElectricObject();
						ElectricObject aHReal = aHObj;
						if (aHReal instanceof PortInst) aHReal = ((PortInst)aHReal).getNodeInst();
						if (hReal == aHReal)
						{
							// found it: adjust the port/point
							if (hObj != aHObj || alreadyHighlighted.getPoint() != got.getPoint())
							{
								alreadyHighlighted.setElectricObject(got.getElectricObject());
								alreadyHighlighted.setPoint(got.getPoint());
								changed = true;
							}
							break;
						}
					}
					return true;
				}
			}
		}
		return false;
	}

    /**
	 * Method to convert the Variable to a series of points that describes the text.
	 */
	public static Point2D [] describeHighlightText(EditWindow wnd, ElectricObject eObj, Variable var, Name name)
	{
        if (!Job.acquireExamineLock(false)) return null;
        Poly.Type style = null;
        Point2D[] points = null;
        Rectangle2D bounds = null;
        try {
            Poly poly = eObj.computeTextPoly(wnd, var, name);
            if (poly == null) {
                Job.releaseExamineLock();
                return null;
            }
            bounds = poly.getBounds2D();
            style = poly.getStyle();
            style = Poly.rotateType(style, eObj);
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
	public int findObject(Point2D pt, EditWindow wnd, boolean exclusively,
		boolean another, boolean invert, boolean findPort, boolean findPoint, boolean findSpecial, boolean findText)
	{
		// initialize
		double bestdist = Double.MAX_VALUE;
		boolean looping = false;

		// search the relevant objects in the circuit
		Cell cell = wnd.getCell();
        Rectangle2D bounds = new Rectangle2D.Double(pt.getX(), pt.getY(), 0, 0);
		List underCursor = findAllInArea(cell, exclusively, another, findPort, findPoint, findSpecial, findText, bounds, wnd);

		// if nothing under the cursor, stop now
		if (underCursor.size() == 0)
		{
			if (!invert)
			{
				clear();
				finished();
			}
			return 0;
		}

        // get last selected object. Next selected object should be related
        Highlight lastSelected = getLastSelected(underCursor);

        if (lastSelected != null) {
            //printHighlightList(underCursor);
            // sort under cursor by relevance to lastSelected. first object is most relevant.
            List newUnderCursor = new ArrayList();
            while (!underCursor.isEmpty()) {
                Highlight h = getSimiliarHighlight(underCursor, lastSelected);
                newUnderCursor.add(h);
                underCursor.remove(h);
            }
            underCursor = newUnderCursor;
        }

		// multiple objects under the cursor
		if (underCursor.size() > 1 && another)
		{
			for(int j=0; j<getNumHighlights(); j++)
			{
                List highlightList = getHighlights();
				Highlight oldHigh = (Highlight)highlightList.get(j);
				for(int i=0; i<underCursor.size(); i++)
				{
					if (oldHigh.sameThing((Highlight)underCursor.get(i)))
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
							addHighlight((Highlight)underCursor.get(i+1));
						} else
						{
							addHighlight((Highlight)underCursor.get(0));
						}
						finished();
						return 1;
					}
				}
			}
		}

		// just use the first in the list
		if (invert)
		{
			Highlight newHigh = (Highlight)underCursor.get(0);
            List highlightList = getHighlights();
			for(int i=0; i<highlightList.size(); i++)
			{
				if (newHigh.sameThing((Highlight)highlightList.get(i)))
				{
					remove((Highlight)highlightList.get(i));
					finished();
					return 1;
				}
			}
			addHighlight(newHigh);
			finished();
		} else
		{
			clear();
			addHighlight((Highlight)underCursor.get(0));
			finished();
		}

//		// reevaluate if this is code
//		if ((curhigh->status&HIGHTYPE) == HIGHTEXT && curhigh->fromvar != NOVARIABLE &&
//			curhigh->fromvarnoeval != NOVARIABLE &&
//				curhigh->fromvar != curhigh->fromvarnoeval)
//					curhigh->fromvar = evalvar(curhigh->fromvarnoeval, 0, 0);
		return 1;
	}

    private void printHighlightList(List highs) {
        int i = 0;
        for (Iterator it = highs.iterator(); it.hasNext(); ) {
            Highlight h = (Highlight)it.next();
            System.out.println("highlight "+i+": "+h.getElectricObject());
            i++;
        }
    }

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
	public List findAllInArea(Cell cell, boolean exclusively, boolean another, boolean findPort,
		 boolean findPoint, boolean findSpecial, boolean findText, Rectangle2D bounds, EditWindow wnd)
	{
		// make a list of things under the cursor
		List list = new ArrayList();

		boolean areaMustEnclose = User.isDraggingMustEncloseObjects();

		// this is the distance from an object that is necessary for a "direct hit"
		double directHitDist = 0;
		if (wnd != null)
		{
			Point2D extra = wnd.deltaScreenToDatabase(EXACTSELECTDISTANCE, EXACTSELECTDISTANCE);
			directHitDist = extra.getX() + 0.4;
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
                            Highlight h = new Highlight(Highlight.Type.TEXT, cell, cell);
                            h.setVar(poly.getVariable());
                            list.add(h);
                        }
                    }
                }

                // next examine all text on nodes in the cell
                for(Iterator it = cell.getNodes(); it.hasNext(); )
                {
                    NodeInst ni = (NodeInst)it.next();
                    AffineTransform trans = ni.rotateOut();
                    EditWindow subWnd = wnd;
                    Poly [] polys = ni.getAllText(findSpecial, wnd);
                    if (polys == null) continue;
                    for(int i=0; i<polys.length; i++)
                    {
                        Poly poly = polys[i];
                        if (poly == null) continue;
                        poly.transform(trans);
                        if (poly.setExactTextBounds(wnd, ni)) continue;

                        // ignore areaMustEnclose if bounds is size 0,0
                        if (areaMustEnclose && (bounds.getHeight() > 0 || bounds.getWidth() > 0))
                        {
                            if (!poly.isInside(bounds)) continue;
                        } else
                        {
                            double hitdist = poly.polyDistance(bounds);
                            if (hitdist >= directHitDist) continue;
                        }
                        Highlight h = new Highlight(Highlight.Type.TEXT, null, cell);
                        if (poly.getPort() != null)
                        {
                            PortProto pp = poly.getPort();
                            h.setElectricObject(pp);
                            for(Iterator pIt = ni.getPortInsts(); pIt.hasNext(); )
                            {
                                PortInst pi = (PortInst)pIt.next();
                                if (pi.getPortProto() == pp)
                                {
                                    h.setElectricObject(pi);
                                    break;
                                }
                            }
                        } else
                            h.setElectricObject(ni);
                        h.setVar(poly.getVariable());
                        h.setName(poly.getName());
                        list.add(h);
                    }
                }

                // next examine all text on arcs in the cell
                for(Iterator it = cell.getArcs(); it.hasNext(); )
                {
                    ArcInst ai = (ArcInst)it.next();
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
                            Highlight h = new Highlight(Highlight.Type.TEXT, ai, cell);
                            h.setVar(poly.getVariable());
                            h.setName(poly.getName());
                            list.add(h);
                        }
                    }
                }
            }

            if (exclusively)
            {
                // special case: only review what is already highlighted
                for(Iterator sIt = getHighlights().iterator(); sIt.hasNext(); )
                {
                    Highlight h = (Highlight)sIt.next();
                    if (h.getType() != Highlight.Type.EOBJ) continue;
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
                if (phase == 0 && !findSpecial && !User.isEasySelectionOfCellInstances()) continue;

                // examine everything in the area
                for(Iterator it = cell.searchIterator(searchArea); it.hasNext(); )
                {
                    Geometric geom = (Geometric)it.next();

                    Highlight h;
                    switch (phase)
                    {
                        case 0:			// check primitive nodes
                            if (!(geom instanceof NodeInst)) break;
                            if (((NodeInst)geom).getProto() instanceof Cell) break;
                            h = checkOutObject(geom, findPort, findPoint, findSpecial, bounds, wnd, directHitDist, areaMustEnclose);
                            if (h != null) list.add(h);
                            break;
                        case 1:			// check Cell instances
                            if (!(geom instanceof NodeInst)) break;
                            if (((NodeInst)geom).getProto() instanceof PrimitiveNode) break;
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
	private static Highlight checkOutObject(Geometric geom, boolean findPort, boolean findPoint, boolean findSpecial, Rectangle2D bounds,
		EditWindow wnd, double directHitDist, boolean areaMustEnclose)
	{
        // ignore areaMustEnclose if bounds is size 0,0
        if (areaMustEnclose && (bounds.getHeight() > 0 || bounds.getWidth() > 0))
		{
			Rectangle2D geomBounds = geom.getBounds();
			Poly poly = new Poly(geomBounds);
			if (!poly.isInside(bounds)) return null;
		}

		if (geom instanceof NodeInst)
		{
			// examine a node object
			NodeInst ni = (NodeInst)geom;

			// do not "find" hard-to-find nodes if "findSpecial" is not set
			boolean hardToSelect = ni.isHardSelect();
			boolean ignoreCells = !User.isEasySelectionOfCellInstances();
			if ((ni.getProto() instanceof Cell) && ignoreCells) hardToSelect = true;
			if (!findSpecial && hardToSelect) return null;

			// do not include primitives that have all layers invisible
//			if (ni.getProto() instanceof PrimitiveNode && (ni->proto->userbits&NINVISIBLE) != 0) return;

			// do not "find" Invisible-Pins if they have text or exports
			if (ni.isInvisiblePinWithText())
				return null;

			// get the distance to the object
			double dist = distToNode(bounds, ni, wnd);

			// direct hit
			if (dist < directHitDist)
			{
				Highlight h = new Highlight(Highlight.Type.EOBJ, null, geom.getParent());
				ElectricObject eobj = geom;

				// add the closest port
				if (findPort)
				{
					double bestDist = Double.MAX_VALUE;
					PortInst bestPort = null;
					for(Iterator it = ni.getPortInsts(); it.hasNext(); )
					{
						PortInst pi = (PortInst)it.next();
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
						if (bestPoint >= 0) h.setPoint(bestPoint);
					}
				}
				h.setElectricObject(eobj);
				return h;
			}
		} else
		{
			// examine an arc object
			ArcInst ai = (ArcInst)geom;

			// do not "find" hard-to-find arcs if "findSpecial" is not set
			if (!findSpecial && ai.isHardSelect()) return null;

			// do not include arcs that have all layers invisible
//			if ((ai->proto->userbits&AINVISIBLE) != 0) return;

			// get distance to arc
			double dist = distToArc(bounds, ai, wnd);

			// direct hit
			if (dist < directHitDist)
			{
				Highlight h = new Highlight(Highlight.Type.EOBJ, geom, geom.getParent());
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
    public static Highlight getSimiliarHighlight(List highlights, Highlight exampleHigh) {
        if (highlights.size() == 0) return null;
        if (exampleHigh == null) return (Highlight)highlights.get(0);

        // get Highlights of the same type
        List sameTypes = new ArrayList();
        for (Iterator it = highlights.iterator(); it.hasNext(); ) {
            Highlight h = (Highlight)it.next();
            if (h.getType() == exampleHigh.getType()) {
                sameTypes.add(h);
            }
        }
        // if only one, just return it
        if (sameTypes.size() == 1) return (Highlight)sameTypes.get(0);
        // if none of same type, just return first in list of all highlights
        if (sameTypes.size() == 0) return (Highlight)highlights.get(0);

        // we have different rules depending on the type
        if (exampleHigh.getType() == Highlight.Type.EOBJ) {

            // get Highlights of the same electric object
            List sameEObj = new ArrayList();
            for (Iterator it = sameTypes.iterator(); it.hasNext(); ) {
                Highlight h = (Highlight)it.next();
                if (h.getElectricObject().getClass() == exampleHigh.getElectricObject().getClass())
                    sameEObj.add(h);
            }
            // if only one of same object, return it
            if (sameEObj.size() == 1) return (Highlight)sameEObj.get(0);

            // if more than one of the same ElectricObject, make decisions
            // for some of the common choices
            if (sameEObj.size() > 0) {
                // for PortInsts (Mouse GUI always sets "findPort", so we don't care about NodeInsts, only PortInsts)
                if (exampleHigh.getElectricObject().getClass() == PortInst.class) {
                    // see if we can find a port on the same NodeProto
                    PortInst exPi = (PortInst)exampleHigh.getElectricObject();
                    NodeProto exNp = exPi.getNodeInst().getProto();
                    for (Iterator it = sameEObj.iterator(); it.hasNext(); ) {
                        Highlight h = (Highlight)it.next();
                        PortInst pi = (PortInst)h.getElectricObject();
                        NodeProto np = pi.getNodeInst().getProto();
                        if (np == exNp) return h;
                    }
                    // nothing with the same prototype, see if we can find a port that can connect to it
                    for (Iterator it = sameEObj.iterator(); it.hasNext(); ) {
                        Highlight h = (Highlight)it.next();
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
                    for (Iterator it = sameEObj.iterator(); it.hasNext(); ) {
                        Highlight h = (Highlight)it.next();
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
                for (Iterator it = sameTypes.iterator(); it.hasNext(); ) {
                    Highlight h = (Highlight)it.next();
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
            if (sameEObj.size() > 0) return (Highlight)sameEObj.get(0);
        }
        // return first in list (list empty case handled above)
        return (Highlight)sameTypes.get(0);
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
		if (np instanceof PrimitiveNode)
		{
			// special case for MOS transistors: examine the gate/active tabs
			NodeProto.Function fun = np.getFunction();
			if (fun == NodeProto.Function.TRANMOS || fun == NodeProto.Function.TRAPMOS || fun == NodeProto.Function.TRADMOS)
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
			if (np.isEdgeSelect())
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
		Poly poly = ai.makePoly(ai.getLength(), wid, Poly.Type.FILLED);
		return poly.polyDistance(bounds);
	}

    public void databaseEndChangeBatch(Undo.ChangeBatch batch) {
        // see if anything we care about changed
        finished();
    }

    public void databaseChanged(Undo.Change evt) {}

    public boolean isGUIListener() {
        return true;
    }
}
