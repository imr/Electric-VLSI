/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WiringListener.java
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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.UserMenuCommands;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.EditWindow;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

class WiringListener
	implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
{
	public static WiringListener theOne = new WiringListener();
	private int startx, starty;
	private Geometric startObj;
	private PortProto startPort;
	private Cell cellBeingWired;
	private boolean doingWiringDrag;

	private WiringListener() {}

	public Cell getCellBeingWired() { return cellBeingWired; }

	public void mousePressed(MouseEvent evt)
	{
		startx = evt.getX();
		starty = evt.getY();
		EditWindow wnd = (EditWindow)evt.getSource();

		// if over selected things, wire them
		boolean another = false;
		if ((evt.getModifiers()&MouseEvent.CTRL_MASK) != 0) another = true;
		doingWiringDrag = false;

		// object wiring: start by seeing if cursor is over a highlighted object
		if (!another && Highlight.overHighlighted(wnd, startx, starty))
		{
			// over a highlighted object: move it
			Highlight high = (Highlight)Highlight.getHighlights().next();
			if (high == null) return;
			startObj = high.getGeom();
			cellBeingWired = startObj.getParent();
			if (startObj instanceof NodeInst)
				startPort = high.getPort(); else
					startPort = null;
			doingWiringDrag = true;
			return;
		}

		// new selection: see if cursor is over anything
		Point2D pt = wnd.screenToDatabase(startx, starty);
		Highlight.findObject(pt, wnd, false, another, true, false);
		if (Highlight.getNumHighlights() == 1)
		{
			// not over anything: bail
			Highlight high = (Highlight)Highlight.getHighlights().next();
			if (high.getType() == Highlight.Type.GEOM)
			{
				startObj = high.getGeom();
				if (startObj instanceof NodeInst)
					startPort = high.getPort(); else
						startPort = null;
				cellBeingWired = startObj.getParent();
				doingWiringDrag = true;
			}
		}
		wnd.repaint();
	}

	public void mouseReleased(MouseEvent evt)
	{
		EditWindow wnd = (EditWindow)evt.getSource();
		Cell cell = wnd.getCell();

		// handle showing the intended wiring path during dragging
		if (doingWiringDrag)
		{
			doingWiringDrag = false;
			int newX = evt.getX();
			int newY = evt.getY();
//			Highlight.setHighlightOffset(0, 0);
			Point2D start = wnd.screenToDatabase(startx, starty);
			Point2D end = wnd.screenToDatabase(newX, newY);
			WiringPlan [] curPath = WiringPlan.getWiringPlan(start, startObj, startPort, end, null);
			if (curPath == null) return;
			RealizeWiring job = new RealizeWiring(curPath, this);
			wnd.redraw();
			wnd.redraw();
		}
	}

	protected static class RealizeWiring extends Job
	{
		WiringPlan [] wpList;
		WiringListener wl;

		protected RealizeWiring(WiringPlan [] wpList, WiringListener wl)
		{
			super("Wiring", User.tool, Job.Type.CHANGE, wl.getCellBeingWired(), null, Job.Priority.USER);
			this.wpList = wpList;
			this.wl = wl;
		}

		public void doIt()
		{
			// get information about what is highlighted
			Highlight.clear();
			WiringPlan wpEnd = null;
			for(int i=0; i<wpList.length; i++)
			{
				WiringPlan wp = wpList[i];
				if (wp.getType() == WiringListener.Type.NODEADD)
				{
					if (wp.getNodeObject() == null)
					{
						// create the nodeInst
						Cell cell = wl.getCellBeingWired();
						NodeInst newNi = NodeInst.newInstance(wp.getNodeType(), wp.getNodeLocation(),
							wp.getNodeWidth(), wp.getNodeHeight(), 0, cell, null);
						if (newNi == null) return;
						newNi.endChange();
						wp.setNodeObject(newNi);
						wpEnd = wp;
					}
				}
				if (wp.getType() == WiringListener.Type.ARCADD)
				{
					WiringPlan headWp = (WiringPlan)wp.getArcHeadObject();
					WiringPlan tailWp = (WiringPlan)wp.getArcTailObject();
					NodeInst headNi = (NodeInst)headWp.getNodeObject();
					NodeInst tailNi = (NodeInst)tailWp.getNodeObject();
					PortInst headPi = headNi.findPortInstFromProto(wp.getArcHeadPortProto());
					PortInst tailPi = tailNi.findPortInstFromProto(wp.getArcTailPortProto());
					ArcProto ap = wp.getArcType();
					if (ap == null)
					{
						System.out.println("Arc proto null");
						return;
					}
					ArcInst newAi = ArcInst.newInstance(ap, wp.getArcWidth(), headPi, tailPi, null);
					if (newAi == null) return;
					newAi.endChange();
				}
			}

			/* show the end node */
			if (wpEnd != null)
			{
				Highlight.addGeometric((Geometric)wpEnd.getNodeObject());
			}
		}
	}

	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mouseMoved(MouseEvent evt) {}

	static Point2D figureCenter(Object obj, PortProto pp)
	{
		if (obj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)obj;
			PortInst pi = ni.findPortInstFromProto(pp);
			Poly poly = pi.getPoly();
			return new Point2D.Double(poly.getCenterX(), poly.getCenterY());
		}
		if (obj instanceof WiringPlan)
		{
			WiringPlan wp = (WiringPlan)obj;
			if (wp.getType() == Type.NODEADD)
			{
				return wp.getNodeLocation();
			}
		}
		return null;
	}

	public void mouseDragged(MouseEvent evt)
	{
		int newX = evt.getX();
		int newY = evt.getY();
		EditWindow wnd = (EditWindow)evt.getSource();

		// handle moving the selected objects
		if (doingWiringDrag)
		{
			Point2D start = wnd.screenToDatabase(startx, starty);
			Point2D end = wnd.screenToDatabase(newX, newY);
			WiringPlan [] curPath = WiringPlan.getWiringPlan(start, startObj, startPort, end, null);
			if (curPath == null) return;

			Highlight.clear();
			Highlight.setHighlightOffset(0, 0);
			WiringPlan wpEnd = null;
			for(int i=0; i<curPath.length; i++)
			{
				WiringPlan wp = curPath[i];
				if (wp.getType() == Type.ARCADD)
				{
					Point2D headLoc = figureCenter(wp.getArcHeadObject(), wp.getArcHeadPortProto());
					Point2D tailLoc = figureCenter(wp.getArcTailObject(), wp.getArcTailPortProto());
//					System.out.println("From "+headLoc +" to "+tailLoc);
					Highlight.addLine(headLoc, tailLoc, cellBeingWired);
				}
				if (wp.getType() == Type.NODEADD)
				{
					wpEnd = wp;
				}
			}

			/* show the end node */
			if (wpEnd != null)
			{
				Rectangle2D highlight = new Rectangle2D.Double(wpEnd.getNodeLocation().getX()-wpEnd.getNodeWidth()/2,
					wpEnd.getNodeLocation().getY()-wpEnd.getNodeHeight()/2, wpEnd.getNodeWidth(), wpEnd.getNodeHeight());
				Highlight.addArea(highlight, cellBeingWired);
			}
			wnd.redraw();
			return;
		}
		wnd.redraw();
	}

	public void mouseWheelMoved(MouseWheelEvent evt)
	{
		int clicks = evt.getWheelRotation();
		System.out.println("Mouse wheel rolled by " + clicks);
	}

	public void keyPressed(KeyEvent evt)
	{
		int chr = evt.getKeyCode();
		EditWindow wnd = (EditWindow)evt.getSource();
		if (chr == KeyEvent.VK_DELETE || chr == KeyEvent.VK_BACK_SPACE)
		{
			CircuitChanges.deleteSelected();
		}
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

	/**
	 * Type is a typesafe enum class that describes the nature of the highlight.
	 */
	public static class Type
	{
		private final String name;

		private Type(String name) { this.name = name; }

		/**
		 * Returns a printable version of this Type.
		 * @return a printable version of this Type.
		 */
		public String toString() { return name; }

		/** Describes a highlighted geometric. */		public static final Type NODEADD = new Type("add node");
		/** Describes highlighted text. */				public static final Type ARCADD = new Type("add arc");
		/** Describes a highlighted area. */			public static final Type NODEDEL = new Type("delete node");
		/** Describes a highlighted line. */			public static final Type ARCDEL = new Type("delete arc");
		/** Describes a highlighted line. */			public static final Type RECONNECT = new Type("add reconnect arc");
	}

	static class WiringPlan
	{
		private Type type;
		private NodeProto pinType;
		private PortProto pinPort;
		private Object pin;
		private Point2D pinLocation;
		private double pinWidth, pinHeight;

		private ArcProto arcType;
		private Object arcHeadObj;
		private PortProto arcHeadPort;
		private Object arcTailObj;
		private PortProto arcTailPort;
		private double arcWidth;

		private WiringPlan() {}

		public Type getType() { return type; }

		public void setNodeObject(Object obj) { this.pin = obj; }
		public Object getNodeObject() { return pin; }
		public PortProto getNodePort() { return pinPort; }
		public NodeProto getNodeType() { return pinType; }
		public Point2D getNodeLocation() { return pinLocation; }
		public double getNodeWidth() { return pinWidth; }
		public double getNodeHeight() { return pinHeight; }

		public ArcProto getArcType() { return arcType; }
		public Object getArcHeadObject() { return arcHeadObj; }
		public PortProto getArcHeadPortProto() { return arcHeadPort; }
		public Object getArcTailObject() { return arcTailObj; }
		public PortProto getArcTailPortProto() { return arcTailPort; }
		public double getArcWidth() { return arcWidth; }

		static WiringPlan makeNode(Object pin, NodeProto type, PortProto port, Point2D location, double width, double height)
		{
			WiringPlan wp = new WiringPlan();
			wp.type = Type.NODEADD;
			wp.pin = pin;
			wp.pinType = type;
			wp.pinPort = port;
			wp.pinLocation = location;
			wp.pinWidth = width;
			wp.pinHeight = height;
			return wp;
		}

		static WiringPlan makeArc(ArcProto type, Object arcHeadObj, PortProto arcHeadPort,
			Object arcTailObj, PortProto arcTailPort, double width)
		{
			WiringPlan wp = new WiringPlan();
			wp.type = Type.ARCADD;
			wp.arcType = type;
			wp.arcHeadObj = arcHeadObj;
			wp.arcHeadPort = arcHeadPort;
			wp.arcTailObj = arcTailObj;
			wp.arcTailPort = arcTailPort;
			wp.pinWidth = width;
			return wp;
		}

		static WiringPlan getClosestEnd(Geometric atStart, PortProto startPort, Point2D start)
		{
			if (atStart instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)atStart;
				if (startPort == null)
				{
					PortInst pi = ni.findClosestPortInst(start);
					startPort = pi.getPortProto();
				}
				return WiringPlan.makeNode(ni, ni.getProto(), startPort, ni.getCenter(), ni.getXSize(), ni.getYSize());
			}
			if (atStart instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)atStart;

				// if point is on the arc, it may be a "t" operation
				Poly poly = ai.makePoly(ai.getXSize(), ai.getWidth(), Poly.Type.FILLED);
				if (poly.isInside(start))
				{
					// clicked on the arc
					if (ai.getHead().getLocation().distance(start) < ai.getWidth())
					{
						PortInst pi = ai.getHead().getPortInst();
						NodeInst ni = pi.getNodeInst();
						return WiringPlan.makeNode(ni, ni.getProto(), pi.getPortProto(), ni.getCenter(), ni.getXSize(), ni.getYSize());
					}
					if (ai.getTail().getLocation().distance(start) < ai.getWidth())
					{
						PortInst pi = ai.getTail().getPortInst();
						NodeInst ni = pi.getNodeInst();
						return WiringPlan.makeNode(ni, ni.getProto(), pi.getPortProto(), ni.getCenter(), ni.getXSize(), ni.getYSize());
					}

					// splitting the arc in the middle
					PrimitiveNode np = ((PrimitiveArc)ai.getProto()).findPinProto();
					PrimitivePort pp = (PrimitivePort)np.getPorts().next();
					Point2D center = EMath.closestPointToSegment(ai.getHead().getLocation(), ai.getTail().getLocation(), start);
					EditWindow.gridAlign(center, 1);
					return WiringPlan.makeNode(null, np, pp, center, np.getDefWidth(), np.getDefHeight());
				}

				// pick the closer end
				if (start.distance(ai.getHead().getLocation()) < start.distance(ai.getTail().getLocation()))
				{
					PortInst pi = ai.getHead().getPortInst();
					NodeInst ni = pi.getNodeInst();
					return WiringPlan.makeNode(ni, ni.getProto(), pi.getPortProto(), ni.getCenter(), ni.getXSize(), ni.getYSize());
				} else
				{
					PortInst pi = ai.getTail().getPortInst();
					NodeInst ni = pi.getNodeInst();
					return WiringPlan.makeNode(ni, ni.getProto(), pi.getPortProto(), ni.getCenter(), ni.getXSize(), ni.getYSize());
				}
			}
			return null;
		}

		public static ArcProto getArcConnectedToPort(PrimitivePort pp)
		{
			// see if current arcproto works
			ArcProto curAp = User.tool.getCurrentArcProto();
			if (pp.connectsTo(curAp)) return curAp;

			// find one that works if current doesn't
			Technology tech = pp.getParent().getTechnology();
			for(Iterator it = tech.getArcs(); it.hasNext(); )
			{
				ArcProto ap = (ArcProto)it.next();
				if (pp.connectsTo(ap)) return ap;
			}

			// none in current technology: try any technology
			for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
			{
				Technology anyTech = (Technology)it.next();
				for(Iterator aIt = anyTech.getArcs(); aIt.hasNext(); )
				{
					PrimitiveArc ap = (PrimitiveArc)aIt.next();
					if (pp.connectsTo(ap)) return ap;
				}
			}
			return null;
		}

		public static WiringPlan [] getWiringPlan(Point2D start, Geometric atStart, PortProto startPort, Point2D end, Geometric atEnd)
		{
			if (atEnd == null)
			{
				// wiring out into space: check out the origin of the wire
				WiringPlan wp = getClosestEnd(atStart, startPort, start);
				if (wp == null) return null;

				NodeProto np = wp.getNodeType();
				PrimitivePort pp = startPort.getBasePort();
				if (pp == null)
					pp = ((PortProto)np.getPorts().next()).getBasePort();

				// determine the type of arc to run: see if default works
				ArcProto useAp = getArcConnectedToPort(pp);
				if (useAp == null)
				{
					// cannot figure out what type of arc connects to this node, perhaps NONE
					return null;
				}

				Point2D trueEnd = new Point2D.Double();
				int angleInc = ((PrimitiveArc)useAp).getAngleIncrement();
				if (angleInc == 0)
				{
					trueEnd.setLocation(end);
				} else
				{
					Point2D center = wp.getNodeLocation();
					double bestDist = Double.MAX_VALUE;
					for(int a=0; a<360; a += angleInc)
					{
						Point2D radialEnd = new Point2D.Double(center.getX() + Math.cos(a*Math.PI/180.0),
							center.getY() + Math.sin(a*Math.PI/180.0));
						Point2D closestToRadial = EMath.closestPointToLine(center, radialEnd, end);
						double thisDist = closestToRadial.distance(end);
						if (thisDist < bestDist)
						{
							bestDist = thisDist;
							trueEnd.setLocation(closestToRadial);
						}
					}
				}
				EditWindow.gridAlign(trueEnd, 1);

				PrimitiveNode pinType = ((PrimitiveArc)useAp).findPinProto();
				PrimitivePort pinPort = (PrimitivePort)pinType.getPorts().next();

				// create the destination pin and run the wire
				WiringPlan [] returnValue = new WiringPlan[3];
				returnValue[0] = wp;
				returnValue[1] = makeNode(null, pinType, pinPort, trueEnd, pinType.getDefWidth(), pinType.getDefHeight());

				returnValue[2] = makeArc(useAp, returnValue[0], wp.getNodePort(),
					returnValue[1], pinPort, useAp.getDefaultWidth());
				return returnValue;
			}
			return null;
		}
	}
}
