/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Highlight2.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.variable.DisplayedText;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.text.Name;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.Job;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.util.*;
import java.util.List;

/**
 * Super class for all types of highlighting.
 */
public abstract class Highlight2 implements Cloneable{

    /** for drawing solid lines */		public static final BasicStroke solidLine = new BasicStroke(0);
    /** for drawing dotted lines */		public static final BasicStroke dottedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {1}, 0);
    /** for drawing dashed lines */		public static final BasicStroke dashedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] {10}, 0);
    /** for drawing dashed lines */		public static final BasicStroke boldLine = new BasicStroke(3);

	/** The Cell containing the selection. */					protected Cell cell;
    private static final int CROSSSIZE = 3;

    Highlight2(Cell c)
    {
        this.cell = c;
    }

    public Cell getCell() { return cell; }

    boolean isValid()
    {
        if (cell != null)
            if (!cell.isLinked()) return false;
        return true;
    }

    // creating so HighlightEOBJ is not a public class
    public boolean isHighlightEOBJ() { return false; }

    // creating so HighlightText is not a public class
    public boolean isHighlightText() { return false; }

    public Object getObject() { return null; }

    public Variable.Key getVarKey() { return null; }

    // point variable, only useful for HighlightEOBJ?
    public void setPoint(int p) {;}
    public int getPoint() { return -1; }

    public Object clone()
    {
        try {
			return super.clone();
		}
		catch (CloneNotSupportedException e) {
            e.printStackTrace();
		}
        return null;
    }

    /**
	 * Method to tell whether two Highlights are the same.
	 * @param obj the Highlight to compare to this one.
	 * @return true if the two refer to the same thing.
	 */
    boolean sameThing(Highlight2 obj)
    {
        return false;
    }

    /**
	 * Method to tell whether this Highlight is text that stays with its node.
	 * The two possibilities are (1) text on invisible pins
	 * (2) export names, when the option to move exports with their labels is requested.
	 * @return true if this Highlight is text that should move with its node.
	 */
    public boolean nodeMovesWithText()
	{
		return false;
	}

    /**
	 * Method to display this Highlight in a window.
	 * @param wnd the window in which to draw this highlight.
	 * @param g the Graphics associated with the window.
	 */
	public void showHighlight(EditWindow wnd, Graphics g, int highOffX, int highOffY, boolean onlyHighlight,
                              Color mainColor, Stroke primaryStroke, boolean setConnected)
    {
        if (!isValid()) return;
		g.setColor(mainColor);
        Graphics2D g2 = (Graphics2D)g;
        g2.setStroke(primaryStroke);
        showInternalHighlight(wnd, g, highOffX, highOffY, onlyHighlight, setConnected);
    }

    abstract void showInternalHighlight(EditWindow wnd, Graphics g, int highOffX, int highOffY,
                                        boolean onlyHighlight, boolean setConnected);

    /**
	 * Method to populate a List of all highlighted Geometrics.
     * @param list the list to populate
	 * @param wantNodes true if NodeInsts should be included in the list.
	 * @param wantArcs true if ArcInsts should be included in the list.
	 */
    void getHighlightedEObjs(Highlighter highlighter, List<Geometric> list, boolean wantNodes, boolean wantArcs) {;}

    static void getHighlightedEObjsInternal(Geometric geom, List<Geometric> list, boolean wantNodes, boolean wantArcs)
    {
        if (geom == null) return;
        if (!wantNodes && geom instanceof NodeInst) return;
        if (!wantArcs && geom instanceof ArcInst) return;

        if (list.contains(geom)) return;
        list.add(geom);
    }

    /**
	 * Method to return the Geometric object that is in this Highlight.
	 * If the highlight is a PortInst, an Export, or annotation text, its base NodeInst is returned.
	 * @return the Geometric object that is in this Highlight.
	 * Returns null if this Highlight is not on a Geometric.
	 */
    public Geometric getGeometric() { return null; }

    /**
	 * Method to return a List of all highlighted NodeInsts.
	 * Return a list with the highlighted NodeInsts.
	 */
	void getHighlightedNodes(Highlighter highlighter, List<NodeInst> list) {;}

    static void getHighlightedNodesInternal(Geometric geom, List<NodeInst> list)
    {
        if (geom == null || !(geom instanceof NodeInst)) return;
        NodeInst ni = (NodeInst)geom;
        if (list.contains(ni)) return;
        list.add(ni);
    }

    /**
	 * Method to return a List of all highlighted ArcInsts.
	 * Return a list with the highlighted ArcInsts.
	 */
    void getHighlightedArcs(Highlighter highlighter, List<ArcInst> list) {;}

    static void getHighlightedArcsInternal(Geometric geom, List<ArcInst> list)
    {
        if (geom == null || !(geom instanceof ArcInst)) return;
        ArcInst ai = (ArcInst)geom;

        if (list.contains(ai)) return;
        list.add(ai);
    }

    /**
	 * Method to return a set of the currently selected networks.
	 * Return a set of the currently selected networks.
	 * If there are no selected networks, the list is empty.
	 */
    void getHighlightedNetworks(Set<Network> nets, Netlist netlist) {;}

    /**
	 * Method to return a List of all highlighted text.
     * @param list list to populate.
	 * @param unique true to request that the text objects be unique,
	 * and not attached to another object that is highlighted.
	 * For example, if a node and an export on that node are selected,
	 * the export text will not be included if "unique" is true.
	 * Return a list with the Highlight objects that point to text.
	 */
    void getHighlightedText(List<DisplayedText> list, boolean unique, List<Highlight2> getHighlights) {;}

    /**
	 * Method to return the bounds of the highlighted objects.
	 * @param wnd the window in which to get bounds.
	 * @return the bounds of the highlighted objects (null if nothing is highlighted).
	 */
    Rectangle2D getHighlightedArea(EditWindow wnd) { return null; }

    /**
	 * Method to return the ElectricObject associated with this Highlight object.
	 * @return the ElectricObject associated with this Highlight object.
	 */
    public ElectricObject getElectricObject() { return null; }

    /**
	 * Method to tell whether a point is over this Highlight.
	 * @param wnd the window being examined.
	 * @param x the X screen coordinate of the point.
	 * @param y the Y screen coordinate of the point.
	 * @return true if the point is over this Highlight.
	 */
    boolean overHighlighted(EditWindow wnd, int x, int y, Highlighter highlighter) { return false; }

    public String getInfo() { return null;}

    /**
     * Method to load an array of counts with the number of highlighted objects in a list.
     * arc = 0, node = 1, export = 2, text = 3, graphics = 4
     * @param list the list of highlighted objects.
     * @param counts the array of counts to set.
     * @return a NodeInst, if it is in the list.
     */
    public static NodeInst getInfoCommand(List<Highlight2> list, int[] counts)
    {
        // information about the selected items
        NodeInst theNode = null;
        for(Highlight2 h : list)
        {
            ElectricObject eobj = h.getElectricObject();
            if (h.isHighlightEOBJ())
            {
                if (eobj instanceof NodeInst || eobj instanceof PortInst)
                {
                    counts[1]++;
                    if (eobj instanceof NodeInst) theNode = (NodeInst)eobj; else
                        theNode = ((PortInst)eobj).getNodeInst();
                } else if (eobj instanceof ArcInst)
                {
                    counts[0]++;
                }
            } else if (h.isHighlightText())
            {
            	if (h.getVarKey() == Export.EXPORT_NAME) counts[2]++; else
            	{
            		if (h.getElectricObject() instanceof NodeInst)
            			theNode = (NodeInst)h.getElectricObject();
                    counts[3]++;
            	}
            } else if (h instanceof HighlightArea)
            {
                counts[4]++;
            } else if (h instanceof HighlightLine)
            {
                counts[4]++;
            }
        }
        return theNode;
    }

    /**
	 * Method to draw an array of points as highlighting.
	 * @param wnd the window in which drawing is happening.
     @param g the Graphics for the window.
     @param points the array of points being drawn.
     @param offX the X offset of the drawing.
     @param offY the Y offset of the drawing.
     @param opened true if the points are drawn "opened".
     @param thickLine
     */
	public static void drawOutlineFromPoints(EditWindow wnd, Graphics g, Point2D[] points, int offX, int offY,
                                             boolean opened, boolean thickLine)
	{
//        Dimension screen = wnd.getScreenSize();
		boolean onePoint = true;
		if (points.length <= 0)
			return;
		Point firstP = wnd.databaseToScreen(points[0].getX(), points[0].getY());
		for(int i=1; i<points.length; i++)
		{
			Point p = wnd.databaseToScreen(points[i].getX(), points[i].getY());
			if (DBMath.doublesEqual(p.getX(), firstP.getX()) &&
				DBMath.doublesEqual(p.getY(), firstP.getY())) continue;
			onePoint = false;
			break;
		}
		if (onePoint)
		{
			drawLine(g, wnd, firstP.x + offX-CROSSSIZE, firstP.y + offY, firstP.x + offX+CROSSSIZE, firstP.y + offY);
			drawLine(g, wnd, firstP.x + offX, firstP.y + offY-CROSSSIZE, firstP.x + offX, firstP.y + offY+CROSSSIZE);
			return;
		}

		// find the center
		int cX = 0, cY = 0;
//		if (thickCenter != null)
//		{
//			Point lp = wnd.databaseToScreen(thickCenter.getX(), thickCenter.getY());
//			cX = lp.x;
//			cY = lp.y;
//		}

		for(int i=0; i<points.length; i++)
		{
			int lastI = i-1;
			if (lastI < 0)
			{
				if (opened) continue;
				lastI = points.length - 1;
			}
			Point lp = wnd.databaseToScreen(points[lastI].getX(), points[lastI].getY());
			Point p = wnd.databaseToScreen(points[i].getX(), points[i].getY());
			int fX = lp.x + offX;   int fY = lp.y + offY;
			int tX = p.x + offX;    int tY = p.y + offY;
			drawLine(g, wnd, fX, fY, tX, tY);
			if (thickLine)
			{
				if (fX < cX) fX--; else fX++;
				if (fY < cY) fY--; else fY++;
				if (tX < cX) tX--; else tX++;
				if (tY < cY) tY--; else tY++;
				drawLine(g, wnd, fX, fY, tX, tY);
			}
		}
	}

    void internalDescribe(StringBuffer desc) {;}

    /**
     * Describe the Highlight
     * @return a string describing the highlight
     */
    public String describe() {
        StringBuffer desc = new StringBuffer();
        desc.append(this.getClass().getName());
        if (cell != null)
        {
	        desc.append(" in ");
	        desc.append(cell);
        }
        desc.append(": ");
        internalDescribe(desc);
        return desc.toString();
    }

    /**
     * Gets a poly that describes the Highlight for the NodeInst.
     * @param ni the nodeinst to get a poly that will be used to highlight it
     * @return a poly outlining the nodeInst.
     */
    public static Poly getNodeInstOutline(NodeInst ni) {

        AffineTransform trans = ni.rotateOutAboutTrueCenter();

        Poly poly = null;
        if (!ni.isCellInstance())
        {
        	PrimitiveNode pn = (PrimitiveNode)ni.getProto();

        	// special case for outline nodes
            if (pn.isHoldsOutline())
            {
                Point2D [] outline = ni.getTrace();
                if (outline != null)
                {
                    int numPoints = outline.length;
                    Point2D [] pointList = new Point2D.Double[numPoints];
                    for(int i=0; i<numPoints; i++)
                    {
                        pointList[i] = new Point2D.Double(ni.getAnchorCenterX() + outline[i].getX(),
                            ni.getAnchorCenterY() + outline[i].getY());
                    }
                    trans.transform(pointList, 0, pointList, 0, numPoints);
                    poly = new Poly(pointList);
    				if (ni.getFunction() == PrimitiveNode.Function.NODE)
    				{
    					poly.setStyle(Poly.Type.FILLED);
    				} else
    				{
    					poly.setStyle(Poly.Type.OPENED);
    				}
                }
            }

            // special case for circular nodes
    		if (pn == Artwork.tech.circleNode || pn == Artwork.tech.thickCircleNode)
    		{
    			// see if this circle is only a partial one
    			double [] angles = ni.getArcDegrees();
    			if (angles[0] != 0.0 || angles[1] != 0.0)
    			{
    				Point2D [] pointList = Artwork.fillEllipse(ni.getAnchorCenter(), ni.getXSize(), ni.getYSize(), angles[0], angles[1]);
    				poly = new Poly(pointList);
    				poly.setStyle(Poly.Type.OPENED);
    				poly.transform(ni.rotateOut());
    			}
    		}
        }

        // setup outline of node with standard offset
        if (poly == null)
        {
            SizeOffset so = ni.getSizeOffset();
            double nodeLowX = ni.getTrueCenterX() - ni.getXSize()/2 + so.getLowXOffset();
            double nodeHighX = ni.getTrueCenterX() + ni.getXSize()/2 - so.getHighXOffset();
            double nodeLowY = ni.getTrueCenterY() - ni.getYSize()/2 + so.getLowYOffset();
            double nodeHighY = ni.getTrueCenterY() + ni.getYSize()/2 - so.getHighYOffset();
            if (nodeLowX == nodeHighX && nodeLowY == nodeHighY)
            {
                float x = (float)nodeLowX;
                float y = (float)nodeLowY;
                Point2D [] outline = new Point2D[1];
                outline[0] = new Point2D.Double(x, y);
                poly = new Poly(outline);
            } else
            {
                double nodeX = (nodeLowX + nodeHighX) / 2;
                double nodeY = (nodeLowY + nodeHighY) / 2;
                poly = new Poly(nodeX, nodeY, nodeHighX-nodeLowX, nodeHighY-nodeLowY);
                poly.transform(trans);
            }
        }

        return poly;
    }

    /**
     * Implementing clipping here speeds things up a lot if there are
     * many large highlights off-screen
     */
    public static void drawLine(Graphics g, EditWindow wnd, int x1, int y1, int x2, int y2)
    {
        Dimension size = wnd.getScreenSize();
		// first clip the line
        Point pt1 = new Point(x1, y1);
        Point pt2 = new Point(x2, y2);
		if (GenMath.clipLine(pt1, pt2, 0, size.width-1, 0, size.height-1)) return;
		g.drawLine(pt1.x, pt1.y, pt2.x, pt2.y);
//        if (((x1 >= 0) && (x1 <= size.getWidth())) || ((x2 >= 0) && (x2 <= size.getWidth())) ||
//            ((y1 >= 0) && (y1 <= size.getHeight())) || ((y2 >= 0) && (y2 <= size.getHeight()))) {
//                g.drawLine(x1, y1, x2, y2);
//        }
    }
}

class HighlightPoly extends Highlight2
{
    /** The highlighted polygon */                              private Poly polygon;
    /** The color used when drawing polygons */                 private Color color;
    HighlightPoly(Cell c, Poly p, Color col)
    {
        super(c);
        this.polygon = p;
        this.color = col;
    }

    public void showInternalHighlight(EditWindow wnd, Graphics g, int highOffX, int highOffY,
                                      boolean onlyHighlight, boolean setConnected)
    {
        // switch colors if specified
        Color oldColor = null;
        if (color != null) {
            oldColor = g.getColor();
            g.setColor(color);
        }
        // draw outline of poly
        boolean opened = (polygon.getStyle() == Poly.Type.OPENED);
        drawOutlineFromPoints(wnd, g, polygon.getPoints(), highOffX, highOffY, opened, false);
        // switch back to old color if switched
        if (oldColor != null)
            g.setColor(oldColor);
    }
}

class HighlightLine extends Highlight2
{
	/** The highlighted line. */								protected Point2D start, end, center;
    /** The highlighted line is thick. */					    protected boolean thickLine;
    HighlightLine(Cell c, Point2D s, Point2D e, Point2D cen, boolean thick)
    {
        super(c);
        this.start = s;
        this.end = e;
        this.center = cen;
        this.thickLine = thick;
    }

    public void showInternalHighlight(EditWindow wnd, Graphics g, int highOffX, int highOffY,
                                      boolean onlyHighlight, boolean setConnected)
    {
        Point2D [] points = new Point2D.Double[2];
        points[0] = new Point2D.Double(start.getX(), start.getY());
        points[1] = new Point2D.Double(end.getX(), end.getY());
        drawOutlineFromPoints(wnd, g, points, highOffX, highOffY, false, thickLine);
    }

    Rectangle2D getHighlightedArea(EditWindow wnd)
    {
        double cX = (start.getX() + end.getX()) / 2;
        double cY = (start.getY() + end.getY()) / 2;
        double sX = Math.abs(start.getX() - end.getX());
        double sY = Math.abs(start.getY() - end.getY());
		return new Rectangle2D.Double(cX, cY, sX, sY);
    }

    public String getInfo()
    {
        String description = "Line from (" + start.getX() + "," + start.getY() + ") to (" +
            end.getX() + "," + end.getY() + ")";
        return description;
    }
}

class HighlightObject extends Highlight2
{
	/** The highlighted generic object */                       private Object object;
    HighlightObject(Cell c, Object obj)
    {
        super(c);
        this.object = obj;
    }

    public Object getObject() { return object; }

    public void showInternalHighlight(EditWindow wnd, Graphics g, int highOffX, int highOffY,
                                      boolean onlyHighlight, boolean setConnected)
    {
        System.out.println("SHould call this one?");
    }
}

class HighlightArea extends Highlight2
{
    /** The highlighted area. */								protected Rectangle2D bounds;
    HighlightArea(Cell c, Rectangle2D area)
    {
        super(c);
		bounds = new Rectangle2D.Double();
		bounds.setRect(area);
    }

    public void showInternalHighlight(EditWindow wnd, Graphics g, int highOffX, int highOffY,
                                      boolean onlyHighlight, boolean setConnected)
    {
        Point2D [] points = new Point2D.Double[5];
        points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
        points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
        points[2] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
        points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
        points[4] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
        drawOutlineFromPoints(wnd, g, points, highOffX, highOffY, false, false);
    }

    void getHighlightedEObjs(Highlighter highlighter, List<Geometric> list, boolean wantNodes, boolean wantArcs)
    {
        List<Highlight2> inArea = Highlighter.findAllInArea(highlighter, cell, false, false, false, false, false, false, bounds, null);
        for(Highlight2 ah : inArea)
        {
            if (!(ah instanceof HighlightEOBJ)) continue;
            ElectricObject eobj = ((HighlightEOBJ)ah).eobj;
            if (eobj instanceof ArcInst) {
                if (wantArcs)
                    list.add((ArcInst)eobj);
            } else if (eobj instanceof NodeInst) {
                if (wantNodes)
                    list.add((NodeInst)eobj);
            } else if (eobj instanceof PortInst) {
                if (wantNodes)
                    list.add(((PortInst)eobj).getNodeInst());
            }
//					if (!wantNodes)
//					{
//						if (eobj instanceof NodeInst || eobj instanceof PortInst) continue;
//					}
//					if (!wantArcs && eobj instanceof ArcInst) continue;
//					if (eobj instanceof PortInst) eobj = ((PortInst)eobj).getNodeInst();
//					highlightedGeoms.add(eobj);
        }
    }

    void getHighlightedNodes(Highlighter highlighter, List<NodeInst> list)
    {
        List<Highlight2> inArea = Highlighter.findAllInArea(highlighter, cell, false, false, false, false, false, false,
                bounds, null);
        for(Highlight2 ah : inArea)
        {
            if (!(ah instanceof HighlightEOBJ)) continue;
            ElectricObject eobj = ((HighlightEOBJ)ah).eobj;
            if (eobj instanceof NodeInst)
                list.add((NodeInst)eobj);
            else if (eobj instanceof PortInst)
                list.add(((PortInst)eobj).getNodeInst());
        }
    }

    void getHighlightedArcs(Highlighter highlighter, List<ArcInst> list)
    {
        List<Highlight2> inArea = Highlighter.findAllInArea(highlighter, cell, false, false, false, false, false, false,
                bounds, null);
        for(Highlight2 ah : inArea)
        {
            if (!(ah instanceof HighlightEOBJ)) continue;
            ElectricObject eobj = ((HighlightEOBJ)ah).eobj;
            if (eobj instanceof ArcInst)
                list.add((ArcInst)eobj);
        }
    }

    Rectangle2D getHighlightedArea(EditWindow wnd)
    {
        return bounds;
    }

    public String getInfo()
    {
        String description = "Area from " + bounds.getMinX() + "<=X<=" + bounds.getMaxX() +
            " and " + bounds.getMinY() + "<=Y<=" + bounds.getMaxY();
        return description;
    }
}

class HighlightMessage extends Highlight2
{                                  
	/** The highlighted message. */								protected String msg;
    /** Location of the message highlight */                    protected Point2D loc;
    HighlightMessage(Cell c, String m, Point2D p)
    {
        super(c);
        this.msg = m;
        this.loc = p;
    }

    void internalDescribe(StringBuffer desc)
    {
        desc.append(", ");
        desc.append(msg);
    }

    public void showInternalHighlight(EditWindow wnd, Graphics g, int highOffX, int highOffY,
                                      boolean onlyHighlight, boolean setConnected)
    {
        Point location = wnd.databaseToScreen(loc.getX(), loc.getY());
        Color oldColor = g.getColor();
        g.setColor(new Color(255-oldColor.getRed(), 255-oldColor.getGreen(), 255-oldColor.getBlue()));
        g.drawString(msg, location.x+1, location.y+1);
        g.setColor(oldColor);
        g.drawString(msg, location.x, location.y);
    }

    Rectangle2D getHighlightedArea(EditWindow wnd)
    {
        return new Rectangle2D.Double(loc.getX(), loc.getY(), 0, 0);
    }
}

class HighlightEOBJ extends Highlight2
{                                                           
	/** The highlighted object. */								protected ElectricObject eobj;
    /** For Highlighted networks, this prevents excess highlights */ private boolean highlightConnected;
	/** The highlighted outline point (only for NodeInst). */	protected int point;

    public HighlightEOBJ(ElectricObject e, Cell c, boolean connected, int p)
    {
        super(c);  
        this.eobj = e;
        this.highlightConnected = connected;
        this.point = p;
    }

    void internalDescribe(StringBuffer desc)
    {
        desc.append(", ");
        if (eobj instanceof PortInst) {
            desc.append(((PortInst)eobj).describe(true));
        }
        if (eobj instanceof NodeInst) {
            desc.append(((NodeInst)eobj).describe(true));
        }
        if (eobj instanceof ArcInst) {
            desc.append(((ArcInst)eobj).describe(true));
        }
    }

    public ElectricObject getElectricObject() { return eobj; }

    public boolean isHighlightEOBJ() { return true; }

    public void setPoint(int p) { point = p;}
    public int getPoint() { return point; }

    boolean isValid()
    {
        if (!super.isValid()) return false;

        if (eobj instanceof PortInst)
            return ((PortInst)eobj).getNodeInst().isLinked();
        return eobj.isLinked();
    }

    boolean sameThing(Highlight2 obj)
    {
        if (this == obj) return (true);

		// Consider already obj==null
        if (obj == null || getClass() != obj.getClass())
            return (false);

        ElectricObject realEObj = eobj;
        if (realEObj instanceof PortInst) realEObj = ((PortInst)realEObj).getNodeInst();

        HighlightEOBJ other = (HighlightEOBJ)obj;
        ElectricObject realOtherEObj = other.eobj;
        if (realOtherEObj instanceof PortInst) realOtherEObj = ((PortInst)realOtherEObj).getNodeInst();
        if (realEObj != realOtherEObj) return false;
        return true;
    }

    public void showInternalHighlight(EditWindow wnd, Graphics g, int highOffX, int highOffY,
                                      boolean onlyHighlight, boolean setConnected)
    {
        Graphics2D g2 = (Graphics2D)g;
        highlightConnected = setConnected;

        // highlight ArcInst
		if (eobj instanceof ArcInst)
		{
			ArcInst ai = (ArcInst)eobj;

            if (!Job.acquireExamineLock(false)) return;
            try {
                // construct the polygons that describe the basic arc
                Poly poly = ai.makePoly(ai.getWidth() - ai.getProto().getWidthOffset(), Poly.Type.CLOSED);
                if (poly == null) return;
                drawOutlineFromPoints(wnd, g, poly.getPoints(), highOffX, highOffY, false, false);

                if (onlyHighlight)
                {
                    // this is the only thing highlighted: give more information about constraints
                    String constraints = "X";
                    if (ai.isRigid()) constraints = "R"; else
                    {
                        if (ai.isFixedAngle())
                        {
                            if (ai.isSlidable()) constraints = "FS"; else
                                constraints = "F";
                        } else if (ai.isSlidable()) constraints = "S";
                    }
                    Point p = wnd.databaseToScreen(ai.getTrueCenterX(), ai.getTrueCenterY());
                    Font font = wnd.getFont(null);
                    if (font != null)
                    {
                        GlyphVector gv = wnd.getGlyphs(constraints, font);
                        Rectangle2D glyphBounds = gv.getVisualBounds();
                        g.drawString(constraints, (int)(p.x - glyphBounds.getWidth()/2 + highOffX),
                            (int)(p.y + font.getSize()/2 + highOffY));
                    }
                }
                Job.releaseExamineLock();
            } catch (Error e) {
                Job.releaseExamineLock();
                throw e;
            }
			return;
		}

		// highlight NodeInst
		PortProto pp = null;
		ElectricObject realEObj = eobj;
        PortInst originalPi = null;
        if (realEObj instanceof PortInst)
		{
            originalPi = ((PortInst)realEObj);
            pp = originalPi.getPortProto();
			realEObj = ((PortInst)realEObj).getNodeInst();
		}
		if (realEObj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)realEObj;
			NodeProto np = ni.getProto();
			AffineTransform trans = ni.rotateOutAboutTrueCenter();

            int offX = highOffX;
            int offY = highOffY;
/*
			boolean drewOutline = false;
			if (!ni.isCellInstance())
			{
				// special case for outline nodes
				if (np.isHoldsOutline())
				{
					Point2D [] outline = ni.getTrace();
					if (outline != null)
					{
						int numPoints = outline.length;
						Point2D [] pointList = new Point2D.Double[numPoints];
						for(int i=0; i<numPoints; i++)
						{
							pointList[i] = new Point2D.Double(ni.getTrueCenterX() + outline[i].getX(),
								ni.getTrueCenterY() + outline[i].getY());
						}
						trans.transform(pointList, 0, pointList, 0, numPoints);
						drawOutlineFromPoints(wnd, g, pointList, 0, 0, true, null);
						drewOutline = true;
					}
				}
			}

			// setup outline of node with standard offset
			if (!drewOutline)
			{
				SizeOffset so = ni.getSizeOffset();
				double nodeLowX = ni.getTrueCenterX() - ni.getXSize()/2 + so.getLowXOffset();
				double nodeHighX = ni.getTrueCenterX() + ni.getXSize()/2 - so.getHighXOffset();
				double nodeLowY = ni.getTrueCenterY() - ni.getYSize()/2 + so.getLowYOffset();
				double nodeHighY = ni.getTrueCenterY() + ni.getYSize()/2 - so.getHighYOffset();
				if (nodeLowX == nodeHighX && nodeLowY == nodeHighY)
				{
					float x = (float)nodeLowX;
					float y = (float)nodeLowY;
					float size = 3 / (float)wnd.getScale();
					Point c1 = wnd.databaseToScreen(x+size, y);
					Point c2 = wnd.databaseToScreen(x-size, y);
					Point c3 = wnd.databaseToScreen(x, y+size);
					Point c4 = wnd.databaseToScreen(x, y-size);
					drawLine(g, wnd, c1.x + offX, c1.y + offY, c2.x + offX, c2.y + offY);
					drawLine(g, wnd, c3.x + offX, c3.y + offY, c4.x + offX, c4.y + offY);
				} else
				{
					double nodeX = (nodeLowX + nodeHighX) / 2;
					double nodeY = (nodeLowY + nodeHighY) / 2;
					Poly poly = new Poly(nodeX, nodeY, nodeHighX-nodeLowX, nodeHighY-nodeLowY);
					poly.transform(trans);
					drawOutlineFromPoints(wnd, g, poly.getPoints(), offX, offY, false, null);
				}
			}
*/

			// draw the selected point
			if (point >= 0)
			{
				Point2D [] points = ni.getTrace();
				if (points != null)
				{
					// if this is a spline, highlight the true shape
					if (ni.getProto() == Artwork.tech.splineNode)
					{
						Point2D [] changedPoints = new Point2D[points.length];
						for(int i=0; i<points.length; i++)
						{
							changedPoints[i] = points[i];
							if (i == point)
							{
								double x = ni.getAnchorCenterX() + points[point].getX();
								double y = ni.getAnchorCenterY() + points[point].getY();
								Point2D thisPt = new Point2D.Double(x, y);
								trans.transform(thisPt, thisPt);
								Point cThis = wnd.databaseToScreen(thisPt);
								Point2D db = wnd.screenToDatabase(cThis.x+offX, cThis.y+offY);
								changedPoints[i] = new Point2D.Double(db.getX() - ni.getAnchorCenterX(), db.getY() - ni.getAnchorCenterY());
							}
						}
						Point2D [] spPoints = Artwork.tech.fillSpline(ni.getAnchorCenterX(), ni.getAnchorCenterY(), changedPoints);
						Point cLast = wnd.databaseToScreen(spPoints[0]);
						for(int i=1; i<spPoints.length; i++)
						{
							Point cThis = wnd.databaseToScreen(spPoints[i]);
							drawLine(g, wnd, cLast.x, cLast.y, cThis.x, cThis.y);
							cLast = cThis;
						}
					}

					// draw an "x" through the selected point
					double x = ni.getAnchorCenterX() + points[point].getX();
					double y = ni.getAnchorCenterY() + points[point].getY();
					Point2D thisPt = new Point2D.Double(x, y);
					trans.transform(thisPt, thisPt);
					Point cThis = wnd.databaseToScreen(thisPt);
					int size = 3;
					drawLine(g, wnd, cThis.x + size + offX, cThis.y + size + offY, cThis.x - size + offX, cThis.y - size + offY);
					drawLine(g, wnd, cThis.x + size + offX, cThis.y - size + offY, cThis.x - size + offX, cThis.y + size + offY);

					// find previous and next point, and draw lines to them
					boolean showWrap = ni.traceWraps();
					Point2D prevPt = null, nextPt = null;
					int prevPoint = point - 1;
					if (prevPoint < 0 && showWrap) prevPoint = points.length - 1;
					if (prevPoint >= 0)
					{
						prevPt = new Point2D.Double(ni.getAnchorCenterX() + points[prevPoint].getX(),
							ni.getAnchorCenterY() + points[prevPoint].getY());
						trans.transform(prevPt, prevPt);
						if (prevPt.getX() == thisPt.getX() && prevPt.getY() == thisPt.getY()) prevPoint = -1; else
						{
							Point cPrev = wnd.databaseToScreen(prevPt);
							drawLine(g, wnd, cThis.x + offX, cThis.y + offY, cPrev.x, cPrev.y);
						}
					}
					int nextPoint = point + 1;
					if (nextPoint >= points.length)
					{
						if (showWrap) nextPoint = 0; else
							nextPoint = -1;
					}
					if (nextPoint >= 0)
					{
						nextPt = new Point2D.Double(ni.getAnchorCenterX() + points[nextPoint].getX(),
							ni.getAnchorCenterY() + points[nextPoint].getY());
						trans.transform(nextPt, nextPt);
						if (nextPt.getX() == thisPt.getX() && nextPt.getY() == thisPt.getY()) nextPoint = -1; else
						{
							Point cNext = wnd.databaseToScreen(nextPt);
							drawLine(g, wnd, cThis.x + offX, cThis.y + offY, cNext.x, cNext.y);
						}
					}

					// draw arrows on the lines
					if (offX == 0 && offY == 0 && points.length > 2)
					{
						double arrowLen = Double.MAX_VALUE;
						if (prevPoint >= 0) arrowLen = Math.min(thisPt.distance(prevPt), arrowLen);
						if (nextPoint >= 0) arrowLen = Math.min(thisPt.distance(nextPt), arrowLen);
						arrowLen /= 10;
						double angleOfArrow = Math.PI * 0.8;
						if (prevPoint >= 0)
						{
							Point2D prevCtr = new Point2D.Double((prevPt.getX()+thisPt.getX()) / 2,
								(prevPt.getY()+thisPt.getY()) / 2);
							double prevAngle = DBMath.figureAngleRadians(prevPt, thisPt);
							Point2D prevArrow1 = new Point2D.Double(prevCtr.getX() + Math.cos(prevAngle+angleOfArrow) * arrowLen,
								prevCtr.getY() + Math.sin(prevAngle+angleOfArrow) * arrowLen);
							Point2D prevArrow2 = new Point2D.Double(prevCtr.getX() + Math.cos(prevAngle-angleOfArrow) * arrowLen,
								prevCtr.getY() + Math.sin(prevAngle-angleOfArrow) * arrowLen);
							Point cPrevCtr = wnd.databaseToScreen(prevCtr);
							Point cPrevArrow1 = wnd.databaseToScreen(prevArrow1);
							Point cPrevArrow2 = wnd.databaseToScreen(prevArrow2);
							drawLine(g, wnd, cPrevCtr.x, cPrevCtr.y, cPrevArrow1.x, cPrevArrow1.y);
							drawLine(g, wnd, cPrevCtr.x, cPrevCtr.y, cPrevArrow2.x, cPrevArrow2.y);
						}

						if (nextPoint >= 0)
						{
							Point2D nextCtr = new Point2D.Double((nextPt.getX()+thisPt.getX()) / 2,
								(nextPt.getY()+thisPt.getY()) / 2);
							double nextAngle = DBMath.figureAngleRadians(thisPt, nextPt);
							Point2D nextArrow1 = new Point2D.Double(nextCtr.getX() + Math.cos(nextAngle+angleOfArrow) * arrowLen,
								nextCtr.getY() + Math.sin(nextAngle+angleOfArrow) * arrowLen);
							Point2D nextArrow2 = new Point2D.Double(nextCtr.getX() + Math.cos(nextAngle-angleOfArrow) * arrowLen,
								nextCtr.getY() + Math.sin(nextAngle-angleOfArrow) * arrowLen);
							Point cNextCtr = wnd.databaseToScreen(nextCtr);
							Point cNextArrow1 = wnd.databaseToScreen(nextArrow1);
							Point cNextArrow2 = wnd.databaseToScreen(nextArrow2);
							drawLine(g, wnd, cNextCtr.x, cNextCtr.y, cNextArrow1.x, cNextArrow1.y);
							drawLine(g, wnd, cNextCtr.x, cNextCtr.y, cNextArrow2.x, cNextArrow2.y);
						}
					}

					// do not offset the node, just this point
					offX = offY = 0;
				}
			}

            // draw nodeInst outline
            if ((offX == 0 && offY == 0) || point < 0)
            {
                Poly niPoly = getNodeInstOutline(ni);
                boolean niOpened = (niPoly.getStyle() == Poly.Type.OPENED);
            	Point2D [] points = niPoly.getPoints();
            	drawOutlineFromPoints(wnd, g, points, offX, offY, niOpened, false);
            }

			// draw the selected port
			if (pp != null)
			{
                //@TODO MAYBE I need the main color
//				g.setColor(mainColor);
				Poly poly = ni.getShapeOfPort(pp);
				boolean opened = true;
				Point2D [] points = poly.getPoints();
				if (poly.getStyle() == Poly.Type.FILLED || poly.getStyle() == Poly.Type.CLOSED) opened = false;
				if (poly.getStyle() == Poly.Type.CIRCLE || poly.getStyle() == Poly.Type.THICKCIRCLE ||
					poly.getStyle() == Poly.Type.DISC)
				{
					double sX = points[0].distance(points[1]) * 2;
					Point2D [] pts = Artwork.fillEllipse(points[0], sX, sX, 0, 360);
					poly = new Poly(pts);
					poly.transform(ni.rotateOut());
					points = poly.getPoints();
				} else if (poly.getStyle() == Poly.Type.CIRCLEARC)
				{
					double [] angles = ni.getArcDegrees();
					double sX = points[0].distance(points[1]) * 2;
					Point2D [] pts = Artwork.fillEllipse(points[0], sX, sX, angles[0], angles[1]);
					poly = new Poly(pts);
					poly.transform(ni.rotateOut());
					points = poly.getPoints();
				}
				drawOutlineFromPoints(wnd, g, points, offX, offY, opened, false);
//				g.setColor(mainColor);

                // show name of port
                if (ni.isCellInstance() && (g instanceof Graphics2D))
				{
					// only show name if port is wired (because all other situations already show the port)
					boolean wired = false;
					for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
					{
						Connection con = cIt.next();
						if (con.getPortInst().getPortProto() == pp) { wired = true;   break; }
					}
					if (wired)
					{
	                    Font font = new Font(User.getDefaultFont(), Font.PLAIN, (int)(1.5*EditWindow.getDefaultFontSize()));
    	                GlyphVector v = wnd.getGlyphs(pp.getName(), font);
        	            Point2D point = wnd.databaseToScreen(poly.getCenterX(), poly.getCenterY());
            	        ((Graphics2D)g).drawGlyphVector(v, (float)point.getX()+offX, (float)point.getY()+offY);
					}
                }

                // if this is a port on an "example icon", show the equivalent port in the cell
//                if (ni.isIconOfParent())
//                {
//                	// find export in parent
//                	Export equiv = (Export)cell.findPortProto(pp.getName());
//                	if (equiv != null)
//                	{
//                		PortInst ePi = equiv.getOriginalPort();
//                		Poly ePoly = ePi.getPoly();
//						Point eP = wnd.databaseToScreen(ePoly.getCenterX(), ePoly.getCenterY());
//						Point p = wnd.databaseToScreen(poly.getCenterX(), poly.getCenterY());
//						drawLine(g, wnd, eP.x, eP.y, p.x + offX, p.y + offY);
//                	}
//                }

                // highlight objects that are electrically connected to this object
                // unless specified not to. HighlightConnected is set to false by addNetwork when
                // it figures out what's connected and adds them manually. Because they are added
                // in addNetwork, we shouldn't try and add connected objects here.
                if (highlightConnected && onlyHighlight) {
                    long start = System.currentTimeMillis();
                    Netlist netlist = cell.acquireUserNetlist();
					if (netlist == null) return;
					NodeInst originalNI = ni;
		            if (ni.isIconOfParent())
		            {
	                	// find export in parent
	                	Export equiv = (Export)cell.findPortProto(pp.getName());
	                	if (equiv != null)
						{
	                		originalPi = equiv.getOriginalPort();
							ni = originalPi.getNodeInst();
							pp = originalPi.getPortProto();
						}
		            }
                    Set<Network> networks = new HashSet<Network>();
                    networks = NetworkTool.getNetworksOnPort(originalPi, netlist, networks);

                    HashSet<Geometric> markObj = new HashSet<Geometric>();
                    for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
                    {
                        ArcInst ai = it.next();
                        Name arcName = ai.getNameKey();
                        for (int i=0; i<arcName.busWidth(); i++) {
                            if (networks.contains(netlist.getNetwork(ai, i))) {
                                markObj.add(ai);
                                markObj.add(ai.getHeadPortInst().getNodeInst());
                                markObj.add(ai.getTailPortInst().getNodeInst());
                                break;
                            }
                        }
                    }

                    for (Iterator<Nodable> it = netlist.getNodables(); it.hasNext(); ) {
                        Nodable no = it.next();
                        NodeInst oNi = no.getNodeInst();
                        if (oNi == originalNI) continue;
                        if (markObj.contains(ni)) continue;

                        boolean highlightNo = false;
                        for(Iterator<PortProto> eIt = no.getProto().getPorts(); eIt.hasNext(); )
                        {
                            PortProto oPp = eIt.next();
                            Name opName = oPp.getNameKey();
                            for (int j=0; j<opName.busWidth(); j++) {
                                if (networks.contains(netlist.getNetwork(no, oPp, j))) {
                                    highlightNo = true;
                                    break;
                                }
                            }
                            if (highlightNo) break;
                        }
                        if (highlightNo)
                            markObj.add(oNi);
                    }
                    //System.out.println("Search took "+com.sun.electric.database.text.TextUtils.getElapsedTime(System.currentTimeMillis()-start));

                    // draw lines along all of the arcs on the network
                    Stroke origStroke = g2.getStroke();
                    g2.setStroke(dashedLine);
                    for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
                    {
                        ArcInst ai = it.next();
                        if (!markObj.contains(ai)) continue;
                        Point c1 = wnd.databaseToScreen(ai.getHeadLocation());
                        Point c2 = wnd.databaseToScreen(ai.getTailLocation());
                        drawLine(g, wnd, c1.x, c1.y, c2.x, c2.y);
                    }

                    // draw dots in all connected nodes
                    g2.setStroke(solidLine);
                    for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
                    {
                        NodeInst oNi = it.next();
                        if (!markObj.contains(oNi)) continue;

                        Point c = wnd.databaseToScreen(oNi.getTrueCenter());
                        g.fillOval(c.x-4, c.y-4, 8, 8);

                        // connect the center dots to the input arcs
                        Point2D nodeCenter = oNi.getTrueCenter();
                        for(Iterator<Connection> pIt = oNi.getConnections(); pIt.hasNext(); )
                        {
                            Connection con = pIt.next();
                            ArcInst ai = con.getArc();
                            if (!markObj.contains(ai)) continue;
                            Point2D arcEnd = con.getLocation();
                            if (arcEnd.getX() != nodeCenter.getX() || arcEnd.getY() != nodeCenter.getY())
                            {
                                Point c1 = wnd.databaseToScreen(arcEnd);
                                Point c2 = wnd.databaseToScreen(nodeCenter);
                                //g2.setStroke(dottedLine);
                                //if (c1.distance(c2) < 15) g2.setStroke(solidLine);
                                drawLine(g, wnd, c1.x, c1.y, c2.x, c2.y);
                            }
                        }
                    }
                    g2.setStroke(origStroke);
                }
			}
		}
    }

    void getHighlightedEObjs(Highlighter highlighter, List<Geometric> list, boolean wantNodes, boolean wantArcs)
    {
        getHighlightedEObjsInternal(getGeometric(), list, wantNodes, wantArcs);
    }

    void getHighlightedNodes(Highlighter highlighter, List<NodeInst> list)
    {
        getHighlightedNodesInternal(getGeometric(), list);
    }

    void getHighlightedArcs(Highlighter highlighter, List<ArcInst> list)
    {
        getHighlightedArcsInternal(getGeometric(), list);
    }

    void getHighlightedNetworks(Set<Network> nets, Netlist netlist)
    {
        ElectricObject eObj = eobj;
        if (eObj instanceof NodeInst)
        {
            NodeInst ni = (NodeInst)eObj;
            if (ni.getNumPortInsts() == 1)
            {
                PortInst pi = ni.getOnlyPortInst();
                if (pi != null) eObj = pi;
            }
        }
        if (eObj instanceof PortInst)
        {
            PortInst pi = (PortInst)eObj;
            nets = NetworkTool.getNetworksOnPort(pi, netlist, nets);
//						boolean added = false;
//						for(Iterator<Connection> aIt = pi.getNodeInst().getConnections(); aIt.hasNext(); )
//						{
//							Connection con = aIt.next();
//							ArcInst ai = con.getArc();
//							int wid = netlist.getBusWidth(ai);
//							for(int i=0; i<wid; i++)
//							{
//								Network net = netlist.getNetwork(ai, i);
//								if (net != null)
//								{
//									added = true;
//									nets.add(net);
//								}
//							}
//						}
//						if (!added)
//						{
//							Network net = netlist.getNetwork(pi);
//							if (net != null) nets.add(net);
//						}
        } else if (eObj instanceof ArcInst)
        {
            ArcInst ai = (ArcInst)eObj;
            int width = netlist.getBusWidth(ai);
            for(int i=0; i<width; i++)
            {
                Network net = netlist.getNetwork((ArcInst)eObj, i);
                if (net != null) nets.add(net);
            }
        }
    }

    Rectangle2D getHighlightedArea(EditWindow wnd)
    {
        ElectricObject eObj = eobj;
        if (eObj instanceof PortInst) eObj = ((PortInst)eObj).getNodeInst();
        if (eObj instanceof Geometric)
        {
            Geometric geom = (Geometric)eObj;
            return geom.getBounds();
        }
        return null;
    }

    public Geometric getGeometric()
    {
    	Geometric retVal = null;
        if (eobj instanceof PortInst) retVal = ((PortInst)eobj).getNodeInst(); else
        	if (eobj instanceof Geometric) retVal = (Geometric)eobj;
        return retVal;
    }

    boolean overHighlighted(EditWindow wnd, int x, int y, Highlighter highlighter)
    {
        Point2D slop = wnd.deltaScreenToDatabase(Highlighter.EXACTSELECTDISTANCE*2, Highlighter.EXACTSELECTDISTANCE*2);
        double directHitDist = slop.getX();
        Point2D start = wnd.screenToDatabase((int)x, (int)y);
        Rectangle2D searchArea = new Rectangle2D.Double(start.getX(), start.getY(), 0, 0);

        ElectricObject eobj = this.eobj;
        if (eobj instanceof PortInst) eobj = ((PortInst)eobj).getNodeInst();
        if (eobj instanceof Geometric)
        {
            Highlight2 got = Highlighter.checkOutObject((Geometric)eobj, true, false, true, searchArea, wnd, directHitDist, false);
            if (got == null) return false;
            if (!(got instanceof HighlightEOBJ))
                System.out.println("Error?");
            ElectricObject hObj = got.getElectricObject();
            ElectricObject hReal = hObj;
            if (hReal instanceof PortInst) hReal = ((PortInst)hReal).getNodeInst();
            for(Highlight2 alreadyDone : highlighter.getHighlights())
            {
                if (!(alreadyDone instanceof HighlightEOBJ)) continue;
                HighlightEOBJ alreadyHighlighted = (HighlightEOBJ)alreadyDone;
                ElectricObject aHObj = alreadyHighlighted.getElectricObject();
                ElectricObject aHReal = aHObj;
                if (aHReal instanceof PortInst) aHReal = ((PortInst)aHReal).getNodeInst();
                if (hReal == aHReal)
                {
                    // found it: adjust the port/point
                    if (hObj != aHObj || alreadyHighlighted.point != ((HighlightEOBJ)got).point)
                    {
                        alreadyHighlighted.eobj = got.getElectricObject();
                        alreadyHighlighted.point = ((HighlightEOBJ)got).point;
                        synchronized(highlighter) {
                            highlighter.setChanged(true);
                        }
                    }
                    break;
                }
            }
            return true;
        }
        return false;
    }

    public String getInfo()
    {
        String description = "";
        ElectricObject realObj = eobj;

        if (realObj instanceof PortInst)
            realObj = ((PortInst)realObj).getNodeInst();
        if (realObj instanceof NodeInst)
        {
            NodeInst ni = (NodeInst)realObj;
            description = "Node " + ni.describe(true);
        } else if (realObj instanceof ArcInst)
        {
            ArcInst ai = (ArcInst)eobj;
            description = "Arc " + ai.describe(true);
        }
        return description;
    }
}

class HighlightText extends Highlight2
{                                                          
	/** The highlighted object. */								protected ElectricObject eobj;
	/** The highlighted variable. */							protected Variable.Key varKey;

    public HighlightText(ElectricObject e, Cell c, Variable.Key key)
    {
        super(c);  
        this.eobj = e;
        this.varKey = key;
    }

    void internalDescribe(StringBuffer desc)
    {
        if (varKey != null)
        {
        	if (varKey == NodeInst.NODE_NAME)
        	{
	            desc.append(", name: ");
	            desc.append(((NodeInst)eobj).getName());
        	} else if (varKey == NodeInst.NODE_PROTO)
        	{
	            desc.append(", instance: ");
	            desc.append(((NodeInst)eobj).getProto().getName());
        	} else if (varKey == ArcInst.ARC_NAME)
        	{
	            desc.append(", name: ");
	            desc.append(((ArcInst)eobj).getName());
        	} else if (varKey == Export.EXPORT_NAME)
        	{
	            desc.append(", export: ");
	            desc.append(((Export)eobj).getName());
        	} else
        	{
	            desc.append(", var: ");
	            desc.append(eobj.getVar(varKey).describe(-1));
        	}
        }
    }

    public ElectricObject getElectricObject() { return eobj; }

    // creating so HighlightText is not a public class
    public boolean isHighlightText() { return true; }

    public Variable.Key getVarKey() { return varKey; }

    boolean isValid()
    {
        if (!super.isValid()) return false;
        if (eobj == null || varKey == null) return false;
        if (!eobj.isLinked()) return false;

    	if (varKey == NodeInst.NODE_NAME ||
			varKey == ArcInst.ARC_NAME ||
			varKey == NodeInst.NODE_PROTO ||
			varKey == Export.EXPORT_NAME) return true;
    	return eobj.getVar(varKey) != null;
    }

    boolean sameThing(Highlight2 obj)
    {
        if (this == obj) return (true);

		// Consider already obj==null
        if (obj == null || getClass() != obj.getClass())
            return (false);

        HighlightText other = (HighlightText)obj;
        if (eobj != other.eobj) return false;
        if (cell != other.cell) return false;
        if (varKey != other.varKey) return false;
        return true;
    }

    public void showInternalHighlight(EditWindow wnd, Graphics g, int highOffX, int highOffY,
                                      boolean onlyHighlight, boolean setConnected)
    {
        Graphics2D g2 = (Graphics2D)g;
        Point2D [] points = Highlighter.describeHighlightText(wnd, eobj, varKey);
        if (points == null) return;
        Point2D [] linePoints = new Point2D[2];
        for(int i=0; i<points.length; i += 2)
        {
            linePoints[0] = points[i];
            linePoints[1] = points[i+1];
            drawOutlineFromPoints(wnd, g, linePoints, highOffX, highOffY, false, false);
        }
        if (onlyHighlight)
        {
            // this is the only thing highlighted: show the attached object
            ElectricObject eObj = eobj;
            if (eObj != null && eObj instanceof Geometric)
            {
                Geometric geom = (Geometric)eObj;
                if (geom instanceof ArcInst || !((NodeInst)geom).isInvisiblePinWithText())
                {
                    Point c = wnd.databaseToScreen(geom.getTrueCenter());
                    int lowX = Integer.MAX_VALUE, highX = Integer.MIN_VALUE;
                    int lowY = Integer.MAX_VALUE, highY = Integer.MIN_VALUE;
                    for(int i=0; i<points.length; i++)
                    {
                        Point a = wnd.databaseToScreen(points[i]);
                        if (a.x < lowX) lowX = a.x;
                        if (a.x > highX) highX = a.x;
                        if (a.y < lowY) lowY = a.y;
                        if (a.y > highY) highY = a.y;
                    }
                    int cX = (lowX+highX)/2;
                    int cY = (lowY+highY)/2;
                    if (Math.abs(cX - c.x) > 4 || Math.abs(cY - c.y) > 4)
                    {
                        g.fillOval(c.x-4, c.y-4, 8, 8);
                        g2.setStroke(dottedLine);
                        drawLine(g, wnd, c.x, c.y, cX, cY);
                        g2.setStroke(solidLine);
                    }
                }
            }
        }
    }

    /**
	 * Method to tell whether this Highlight is text that stays with its node.
	 * The two possibilities are (1) text on invisible pins
	 * (2) export names, when the option to move exports with their labels is requested.
	 * @return true if this Highlight is text that should move with its node.
	 */
    public boolean nodeMovesWithText()
	{
		if (varKey != null)
		{
			// moving variable text
			if (!(eobj instanceof NodeInst)) return false;
			NodeInst ni = (NodeInst)eobj;
			if (ni.isInvisiblePinWithText()) return true;
		} else
		{
			// moving export text
			if (!(eobj instanceof Export)) return false;
			Export pp = (Export)eobj;
			if (pp.getOriginalPort().getNodeInst().getProto() == Generic.tech.invisiblePinNode) return true;
			if (User.isMoveNodeWithExport()) return true;
		}
		return false;
	}

    public Geometric getGeometric()
    {
        if (nodeMovesWithText())
        {
            if (eobj instanceof Export) eobj = ((Export)eobj).getOriginalPort().getNodeInst();
            if (eobj instanceof Geometric) return (Geometric)eobj;
        }
        return null;
    }

    void getHighlightedEObjs(Highlighter highlighter, List<Geometric> list, boolean wantNodes, boolean wantArcs)
    {
        getHighlightedEObjsInternal(getGeometric(), list, wantNodes, wantArcs);
    }

    void getHighlightedNodes(Highlighter highlighter, List<NodeInst> list)
    {
        getHighlightedNodesInternal(getGeometric(), list);
    }

    void getHighlightedArcs(Highlighter highlighter, List<ArcInst> list)
    {
        getHighlightedArcsInternal(getGeometric(), list);
    }

    void getHighlightedNetworks(Set<Network> nets, Netlist netlist)
    {
        if (/*varKey == null &&*/ eobj instanceof Export)
        {
            Export pp = (Export)eobj;
            int width = netlist.getBusWidth(pp);
            for(int i=0; i<width; i++)
            {
                Network net = netlist.getNetwork(pp, i);
                if (net != null) nets.add(net);
            }
        }
    }

    DisplayedText makeDisplayedText()
    {
    	if (varKey != null)
    		return new DisplayedText(eobj, varKey);
    	return null;
    }

    void getHighlightedText(List<DisplayedText> list, boolean unique, List<Highlight2> getHighlights)
    {
    	DisplayedText dt = makeDisplayedText();
    	if (dt == null) return;
        if (list.contains(dt)) return;

        // if this text is on a selected object, don't include the text
        if (unique)
        {
            ElectricObject onObj = null;
            if (varKey != null)
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
            }

            // now see if the object is in the list
            if (eobj != null)
            {
                boolean found = false;
                for(Highlight2 oH : getHighlights)
                {
                    if (!(oH instanceof HighlightEOBJ)) continue;
                    ElectricObject fobj = ((HighlightEOBJ)oH).eobj;
                    if (fobj instanceof PortInst) fobj = ((PortInst)fobj).getNodeInst();
                    if (fobj == onObj) { found = true;   break; }
                }
                if (found) return;
            }
        }

        // add this text
        list.add(dt);
    }

    Rectangle2D getHighlightedArea(EditWindow wnd)
    {
        if (wnd != null)
        {
            Poly poly = eobj.computeTextPoly(wnd, varKey);
            if (poly != null) return poly.getBounds2D();
        }
        return null;
    }

    boolean overHighlighted(EditWindow wnd, int x, int y, Highlighter highlighter)
    {
        Point2D start = wnd.screenToDatabase((int)x, (int)y);
        Poly poly = eobj.computeTextPoly(wnd, varKey);
        if (poly != null)
            if (poly.isInside(start)) return true;
        return false;
    }

    public String describe()
    {
        String description = "Text: unknown";
        if (varKey != null)
        {
        	if (varKey == NodeInst.NODE_NAME)
        	{
        		description = "Node name for " + ((NodeInst)eobj).describe(true);
        	} else if (varKey == ArcInst.ARC_NAME)
        	{
        		description = "Arc name for " + ((ArcInst)eobj).describe(true);
        	} else if (varKey == Export.EXPORT_NAME)
        	{
        		description = "Text: Export '" + ((Export)eobj).getName() + "'";
        	} else if (varKey == NodeInst.NODE_PROTO)
        	{
        		description = "Text: Cell instance name " + ((NodeInst)eobj).describe(true);
        	} else
        	{
        		description = "Text: " + eobj.getVar(varKey).getFullDescription(eobj);
        	}
        }
        return description;
    }

    public String getInfo()
    {
        return (describe());
    }
}
