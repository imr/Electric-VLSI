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
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.MenuCommands;

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class WiringListener
	implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
{
	public static WiringListener theOne = new WiringListener();
	private Point2D startPoint;
	private ElectricObject startObj;
	private Geometric startGeom;
	private PortProto startPort;
	private WiringPlan [] wpStartList;
	private Point2D endPoint;
	private ElectricObject endObj;
	private Geometric endGeom;
	private PortProto endPort;
	private Cell cellBeingWired;
	private boolean doingWiringDrag;

	private WiringListener() {}

	public void mousePressed(MouseEvent evt)
	{
		int startx = evt.getX();
		int starty = evt.getY();
		EditWindow wnd = (EditWindow)evt.getSource();
		startPoint = wnd.screenToDatabase(startx, starty);

		boolean another = false;
		if ((evt.getModifiers()&MouseEvent.CTRL_MASK) != 0) another = true;

		// show "get info" on double-click
		if (evt.getClickCount() == 2 && !another)
		{
			if (Highlight.getNumHighlights() >= 1)
			{
				MenuCommands.getInfoCommand();
				return;
			}
		}

		// if over selected things, wire them
		doingWiringDrag = false;

		// object wiring: start by seeing if cursor is over a highlighted object
		if (!another && Highlight.overHighlighted(wnd, startx, starty))
		{
			// over a highlighted object: draw from it
			Highlight high = (Highlight)Highlight.getHighlights().next();
			if (high == null) return;
			if (high.getType() != Highlight.Type.EOBJ) return;
			ElectricObject eobj = high.getElectricObject();
			startPort = null;
			if (eobj instanceof PortInst)
			{
				startPort = ((PortInst)eobj).getPortProto();
				eobj = ((PortInst)eobj).getNodeInst();
			}
			startObj = eobj;
			startGeom = (Geometric)eobj;
			cellBeingWired = startGeom.getParent();
			doingWiringDrag = true;
		} else
		{
			// new selection: see if cursor is over anything
			Highlight.findObject(startPoint, wnd, false, another, false, true, false, false, false);
			if (Highlight.getNumHighlights() == 1)
			{
				// not over anything: bail
				Highlight high = (Highlight)Highlight.getHighlights().next();
				if (high.getType() == Highlight.Type.EOBJ)
				{
					ElectricObject eobj = high.getElectricObject();
					startPort = null;
					if (eobj instanceof PortInst)
					{
						startPort = ((PortInst)eobj).getPortProto();
						eobj = ((PortInst)eobj).getNodeInst();
					}
					startObj = eobj;
					startGeom = (Geometric)eobj;
					cellBeingWired = startGeom.getParent();
					doingWiringDrag = true;
				}
			}
		}

		if (doingWiringDrag)
		{
			wpStartList = WiringPlan.getClosestEnd(startGeom, startPort, startPoint, startPoint);
			if (wpStartList == null)
			{
				System.out.println("ERROR: Cannot run arcs from " + startGeom.describe()); 
				doingWiringDrag = false;
			}
		}
		wnd.repaint();
	}

	public void mouseDragged(MouseEvent evt)
	{
		// stop now if not wiring
		if (!doingWiringDrag) return;

		EditWindow wnd = (EditWindow)evt.getSource();
		getEndObject(evt.getX(), evt.getY(), wnd);
		WiringPlan [] wpEndList = WiringPlan.getClosestEnd(endGeom, endPort, endPoint, startPoint);

		WiringPlan [] curPath = WiringPlan.getWiringPlan(wpStartList, wpEndList, endPoint);
		if (curPath == null) return;

		Highlight.clear();
		Highlight h = Highlight.addElectricObject(startObj, startGeom.getParent());
		WiringPlan wpEnd = null;
		for(int i=0; i<curPath.length; i++)
		{
			WiringPlan wp = curPath[i];
			if (wp.getType() == Type.ARCADD)
			{
				Point2D headLoc = figureCenter(wp.getArcHeadObject(), wp.getArcHeadPortProto());
				Point2D tailLoc = figureCenter(wp.getArcTailObject(), wp.getArcTailPortProto());
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
			SizeOffset so = wpEnd.getNodeType().getSizeOffset();
			double cx = wpEnd.getNodeLocation().getX();
			double cy = wpEnd.getNodeLocation().getY();
			double sx = wpEnd.getNodeWidth();
			double sy = wpEnd.getNodeHeight();
			double lx = cx - sx / 2 + so.getLowXOffset();
			double hx = cx + sx / 2 - so.getLowXOffset();
			double ly = cy - sy / 2 + so.getLowYOffset();
			double hy = cy + sy / 2 - so.getLowYOffset();
			Rectangle2D highlight = new Rectangle2D.Double(lx, ly, hx-lx, hy-ly);
			Highlight.addArea(highlight, cellBeingWired);
		}
		Highlight.finished();
		wnd.repaint();
	}

	public void mouseReleased(MouseEvent evt)
	{
		if (!doingWiringDrag) return;
		doingWiringDrag = false;

		EditWindow wnd = (EditWindow)evt.getSource();
		getEndObject(evt.getX(), evt.getY(), wnd);
		WiringPlan [] wpEndList = WiringPlan.getClosestEnd(endGeom, endPort, endPoint, startPoint);

		WiringPlan [] curPath = WiringPlan.getWiringPlan(wpStartList, wpEndList, endPoint);
		if (curPath == null) return;
		RealizeWiring job = new RealizeWiring(curPath, cellBeingWired);
		wnd.repaintContents();
	}

	public static List makeConnection(NodeInst fromNi, PortProto fromPP, NodeInst toNi, PortProto toPP, Point2D pt,
		boolean nozigzag, boolean report)
	{
		WiringPlan [] startList = WiringPlan.getClosestEnd(fromNi, fromPP, pt, pt);
		WiringPlan [] endList = WiringPlan.getClosestEnd(toNi, toPP, pt, pt);
		WiringPlan [] curPath = WiringPlan.getWiringPlan(startList, endList, pt);
		if (curPath == null) return null;
		List added = doWiring(curPath, fromNi.getParent(), report);
		return added;
	}

	public void keyPressed(KeyEvent evt)
	{
		int chr = evt.getKeyCode();
		EditWindow wnd = (EditWindow)evt.getSource();
		if (chr == KeyEvent.VK_DELETE || chr == KeyEvent.VK_BACK_SPACE)
		{
			CircuitChanges.deleteSelected();
		}

		// "A" for abort
		if (chr == KeyEvent.VK_A)
		{
			// restore the listener to the former state
			doingWiringDrag = false;
			Highlight.clear();
			Highlight h = Highlight.addElectricObject(startObj, startGeom.getParent());
			Highlight.finished();
			wnd.repaint();
			System.out.println("Aborted");
		}
	}

	public void mouseWheelMoved(MouseWheelEvent evt) {}
	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mouseMoved(MouseEvent evt) {}
	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

	/**
	 * Method to load the field variables from the parameters.
	 * Loads field variables "endPoint", "endObj", "endGeom", and "endPort".
	 */
	private void getEndObject(int endX, int endY, EditWindow wnd)
	{
		endPoint = wnd.screenToDatabase(endX, endY);
		endObj = null;
		endGeom = null;
		endPort = null;

		// see what object it is over
		Rectangle2D bounds = new Rectangle2D.Double(endPoint.getX(), endPoint.getY(), 0, 0);
		List underCursor = Highlight.findAllInArea(cellBeingWired, false, false, true, false, false, false, bounds, wnd);
		for(Iterator it = underCursor.iterator(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.EOBJ) continue;
			ElectricObject eobj = h.getElectricObject();
			if (eobj instanceof PortInst)
			{
				endPort = ((PortInst)eobj).getPortProto();
				eobj = ((PortInst)eobj).getNodeInst();
			}
			endGeom = (Geometric)eobj;
			break;
		}
	}

	private static Point2D figureCenter(Object obj, PortProto pp)
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

	private static List doWiring(WiringPlan [] wpList, Cell cell, boolean report)
	{
		// get information about what is highlighted
		List added = new ArrayList();
		WiringPlan wpEnd = null;
		int arcsCreated = 0, nodesCreated = 0;
		ArcProto defArc = User.tool.getCurrentArcProto();
		ArcProto otherArc = null;
		ArcInst lastPlacedArc = null;
		boolean madeDefArcs = false;
		for(int i=0; i<wpList.length; i++)
		{
			WiringPlan wp = wpList[i];
			if (wp.getType() == WiringListener.Type.NODEADD)
			{
				if (wp.getNodeObject() == null)
				{
					// create the nodeInst
					NodeInst newNi = NodeInst.makeInstance(wp.getNodeType(), wp.getNodeLocation(),
						wp.getNodeWidth(), wp.getNodeHeight(), 0, cell, null);
					if (newNi == null) return null;
					added.add(newNi);
					nodesCreated++;
					wp.setNodeObject(newNi);
					wpEnd = wp;
				}
			}
			if (wp.getType() == WiringListener.Type.ARCADD)
			{
				NodeInst headNi, tailNi;
				Object headObj = wp.getArcHeadObject();
				if (headObj instanceof NodeInst) headNi = (NodeInst)headObj; else
					headNi = (NodeInst)((WiringPlan)headObj).getNodeObject();
				PortInst headPi = headNi.findPortInstFromProto(wp.getArcHeadPortProto());

				Object tailObj = wp.getArcTailObject();
				if (tailObj instanceof NodeInst) tailNi = (NodeInst)tailObj; else
					tailNi = (NodeInst)((WiringPlan)tailObj).getNodeObject();
				PortInst tailPi = tailNi.findPortInstFromProto(wp.getArcTailPortProto());

				ArcProto ap = wp.getArcType();
				if (ap == null)
				{
					System.out.println("Arc proto null");
					return null;
				}
				ArcInst newAi = ArcInst.makeInstance(ap, wp.getArcWidth(), headPi, tailPi, null);
				if (newAi == null) return null;
				arcsCreated++;
				added.add(newAi);
				lastPlacedArc = newAi;
				if (newAi.getProto() == defArc) madeDefArcs = true; else
				{
					otherArc = newAi.getProto();
				}
			}
			if (wp.getType() == WiringListener.Type.ARCDEL)
			{
				ArcInst ai = wp.getArc();
				ai.kill();
			}
		}
		if (otherArc != null && !madeDefArcs)
		{
			// switch default arc to something that was just made
			User.tool.setCurrentArcProto(otherArc);
		}

		/* show the end node */
		if (report)
		{
			Highlight.clear();
			if (wpEnd != null)
			{
				NodeInst ni = (NodeInst)wpEnd.getNodeObject();
				Highlight.addElectricObject(ni, ni.getParent());
			} else
			{
				if (lastPlacedArc != null)
					Highlight.addElectricObject(lastPlacedArc, lastPlacedArc.getParent());
			}
			if (arcsCreated != 0 || nodesCreated != 0)
			{
				String msg = "Created ";
				if (arcsCreated != 0) msg += arcsCreated + " arcs";
				if (nodesCreated != 0) msg += " and " + nodesCreated + " nodes";
				System.out.println(msg);
				playSound(arcsCreated);
			}
			Highlight.finished();
		}
		return added;
	}

	private static AudioClip clickSound = null;

	private static void playSound(int arcsCreated)
	{
		if (User.isPlayClickSoundsWhenCreatingArcs())
		{
			URL url = WiringListener.class.getResource("Click.wav");
			if (url == null) return;
			if (clickSound == null)
				clickSound = Applet.newAudioClip(url);
			clickSound.play();
		}
	}

	/**
	 * Class for scheduling a wiring task.
	 */
	protected static class RealizeWiring extends Job
	{
		WiringPlan [] wpList;
		Cell cell;

		protected RealizeWiring(WiringPlan [] wpList, Cell cell)
		{
			super("Wiring", User.tool, Job.Type.CHANGE, cell, null, Job.Priority.USER);
			this.wpList = wpList;
			this.cell = cell;
			startJob();
		}

		public void doIt()
		{
			doWiring(wpList, cell, true);
		}
	}

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

		/** Describes an added node. */				public static final Type NODEADD = new Type("add node");
		/** Describes an added arc. */				public static final Type ARCADD = new Type("add arc");
		/** Describes a deleted arc. */				public static final Type ARCDEL = new Type("delete arc");
	}

	/**
	 * Class for determining a plan to run wires between two points.
	 */
	static class WiringPlan
	{
		private Type type;
		private NodeProto pinType;
		private PortProto pinPort;
		private Object pin;
		private Point2D pinLocation;
		private double pinWidth, pinHeight;

		private ArcProto arcType;
		private ArcInst arc;
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
		public ArcInst getArc() { return arc; }
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
			wp.arcWidth = width;
			return wp;
		}

		static WiringPlan killArc(ArcInst ai)
		{
			WiringPlan wp = new WiringPlan();
			wp.type = Type.ARCDEL;
			wp.arc = ai;
			return wp;
		}

		static WiringPlan [] getWiringPath(WiringPlan start, ArcProto startAp, WiringPlan end, ArcProto endAp)
		{
			Point2D startLoc = new Point2D.Double();
			if (start.getNodeObject() == null) startLoc.setLocation(start.getNodeLocation()); else
				startLoc.setLocation(figureCenter(start.getNodeObject(), start.getNodePort()));

			Point2D endLoc = new Point2D.Double();
			if (end.getNodeObject() == null) endLoc.setLocation(end.getNodeLocation()); else
				endLoc.setLocation(figureCenter(end.getNodeObject(), end.getNodePort()));
			if (EMath.doublesEqual(startLoc.getX(), endLoc.getX()) ||
				EMath.doublesEqual(startLoc.getY(), endLoc.getY()))
			{
				// they line up: make one arc
				WiringPlan [] list = new WiringPlan[1];
				list[0] = WiringPlan.makeArc(startAp, start, start.getNodePort(),
					end, end.getNodePort(), startAp.getWidth());
				return list;
			}

			// make one-bend
			WiringPlan [] list = new WiringPlan[3];
			Point2D intermediate = new Point2D.Double(start.getNodeLocation().getX(), end.getNodeLocation().getY());
			PrimitiveNode np = ((PrimitiveArc)startAp).findPinProto();
			PrimitivePort pp = (PrimitivePort)np.getPorts().next();
			list[0] = WiringPlan.makeNode(null, np, pp, intermediate, np.getDefWidth(), np.getDefHeight());
			list[1] = WiringPlan.makeArc(startAp, list[0], pp,
				end, end.getNodePort(), startAp.getWidth());
			list[2] = WiringPlan.makeArc(startAp, start, start.getNodePort(),
				list[0], pp, startAp.getWidth());
			return list;
		}

		/**
		 * Method to create a wiring plan for one end of the wiring route.
		 */
		static WiringPlan [] getClosestEnd(Geometric geom, PortProto port, Point2D point, Point2D startPoint)
		{
			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				if (port == null)
				{
					PortInst pi = ni.findClosestPortInst(point);
					if (pi == null) return null;
					port = pi.getPortProto();
				}
				PortInst pi = ni.findPortInstFromProto(port);
				Poly portPoly = pi.getPoly();
				Point2D portCenter = new Point2D.Double(portPoly.getCenterX(), portPoly.getCenterY());
				WiringPlan [] list = new WiringPlan[1];
				list[0] = WiringPlan.makeNode(ni, ni.getProto(), port, portCenter, ni.getXSize(), ni.getYSize());
				return list;
			}
			if (geom instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)geom;

				// if point is on the arc, it may be a "t" operation
				Poly poly = ai.makePoly(ai.getLength(), ai.getWidth(), Poly.Type.FILLED);
				if (poly.isInside(point))
				{
					// clicked on the arc
					if (ai.getHead().getLocation().distance(point) < ai.getWidth())
					{
						PortInst pi = ai.getHead().getPortInst();
						NodeInst ni = pi.getNodeInst();
						WiringPlan [] list = new WiringPlan[1];
						list[0] = WiringPlan.makeNode(ni, ni.getProto(), pi.getPortProto(), ni.getTrueCenter(), ni.getXSize(), ni.getYSize());
						return list;
					}
					if (ai.getTail().getLocation().distance(point) < ai.getWidth())
					{
						PortInst pi = ai.getTail().getPortInst();
						NodeInst ni = pi.getNodeInst();
						WiringPlan [] list = new WiringPlan[1];
						list[0] = WiringPlan.makeNode(ni, ni.getProto(), pi.getPortProto(), ni.getTrueCenter(), ni.getXSize(), ni.getYSize());
						return list;
					}

					// splitting the arc in the middle
					PrimitiveNode np = ((PrimitiveArc)ai.getProto()).findPinProto();
					PrimitivePort pp = (PrimitivePort)np.getPorts().next();
					Point2D center = EMath.closestPointToSegment(ai.getHead().getLocation(), ai.getTail().getLocation(), startPoint);
					EditWindow.gridAlign(center);
					WiringPlan [] list = new WiringPlan[4];
					list[0] = WiringPlan.makeNode(null, np, pp, center, np.getDefWidth(), np.getDefHeight());
					list[1] = WiringPlan.makeArc(ai.getProto(), ai.getHead().getPortInst().getNodeInst(),
						ai.getHead().getPortInst().getPortProto(), list[0], pp, ai.getWidth());
					list[2] = WiringPlan.makeArc(ai.getProto(), list[0], pp, ai.getTail().getPortInst().getNodeInst(),
						ai.getTail().getPortInst().getPortProto(), ai.getWidth());
					list[3] = WiringPlan.killArc(ai);
					return list;
				}

				// pick the closer end
				PortInst pi = null;
				if (point.distance(ai.getHead().getLocation()) < point.distance(ai.getTail().getLocation()))
				{
					pi = ai.getHead().getPortInst();
				} else
				{
					pi = ai.getTail().getPortInst();
				}
				NodeInst ni = pi.getNodeInst();
				WiringPlan [] list = new WiringPlan[1];
				list[0] = WiringPlan.makeNode(ni, ni.getProto(), pi.getPortProto(), ni.getTrueCenter(), ni.getXSize(), ni.getYSize());
				return list;
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

		public static ArcProto getArcConnectedToPorts(PrimitivePort pp1, PrimitivePort pp2)
		{
			// see if current arcproto works
			ArcProto curAp = User.tool.getCurrentArcProto();
			if (pp1.connectsTo(curAp) && pp2.connectsTo(curAp)) return curAp;

			// find one that works if current doesn't
			Technology tech = pp1.getParent().getTechnology();
			for(Iterator it = tech.getArcs(); it.hasNext(); )
			{
				ArcProto ap = (ArcProto)it.next();
				if (pp1.connectsTo(ap) && pp2.connectsTo(ap)) return ap;
			}

			// none in current technology: try any technology
			for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
			{
				Technology anyTech = (Technology)it.next();
				for(Iterator aIt = anyTech.getArcs(); aIt.hasNext(); )
				{
					PrimitiveArc ap = (PrimitiveArc)aIt.next();
					if (pp1.connectsTo(ap) && pp2.connectsTo(ap)) return ap;
				}
			}
			return null;
		}

		public static WiringPlan [] getWiringPlan(WiringPlan [] wpStartList,
			WiringPlan [] wpEndList, Point2D endPt)
		{
			WiringPlan wpStart = wpStartList[0];
			if (wpEndList == null)
			{
				// wiring out into space: check out the origin of the wire
				WiringPlan wp = wpStartList[0];

				NodeProto np = wp.getNodeType();
				PrimitivePort pp = null;
				if (wp.getNodePort() != null)
					pp = wp.getNodePort().getBasePort();
				if (pp == null)
					pp = ((PortProto)np.getPorts().next()).getBasePort();

				// determine the type of arc to run: see if default works
				ArcProto useAp = getArcConnectedToPort(pp);
				if (useAp == null) return null;

				Point2D trueEnd = new Point2D.Double();
				int angleInc = ((PrimitiveArc)useAp).getAngleIncrement();
				if (angleInc == 0)
				{
					trueEnd.setLocation(endPt);
				} else
				{
					Point2D center = wpStart.getNodeLocation();
					double bestDist = Double.MAX_VALUE;
					for(int a=0; a<360; a += angleInc)
					{
						Point2D radialEnd = new Point2D.Double(center.getX() + Math.cos(a*Math.PI/180.0),
							center.getY() + Math.sin(a*Math.PI/180.0));
						Point2D closestToRadial = EMath.closestPointToLine(center, radialEnd, endPt);
						double thisDist = closestToRadial.distance(endPt);
						if (thisDist < bestDist)
						{
							bestDist = thisDist;
							trueEnd.setLocation(closestToRadial);
						}
					}
				}
				EditWindow.gridAlign(trueEnd);

				PrimitiveNode pinType = ((PrimitiveArc)useAp).findPinProto();
				PrimitivePort pinPort = (PrimitivePort)pinType.getPorts().next();

				// create the destination pin and run the wire
				WiringPlan [] returnValue = new WiringPlan[wpStartList.length + 2];
				int i = 0;
				for(int j=0; j<wpStartList.length; j++)
					returnValue[i++] = wpStartList[j];
				WiringPlan endNode = makeNode(null, pinType, pinPort, trueEnd, pinType.getDefWidth(), pinType.getDefHeight());
				returnValue[i++] = endNode;
				returnValue[i++] = makeArc(useAp, returnValue[0], wp.getNodePort(),
					endNode, pinPort, useAp.getDefaultWidth());
				return returnValue;
			}

			// ignore if wiring from an object to itself
			WiringPlan wpEnd = wpEndList[0];
			if (wpStart.getNodeObject() == wpEnd.getNodeObject() && wpStart.getNodeObject() != null &&
				wpStart.getNodePort() == wpEnd.getNodePort()) return null;

			// over another object: connect to it
			NodeProto npStart = wpStart.getNodeType();
			PrimitivePort ppStart = null;
			if (wpStart.getNodePort() != null)
				ppStart = wpStart.getNodePort().getBasePort();
			if (ppStart == null)
				ppStart = ((PortProto)npStart.getPorts().next()).getBasePort();

			NodeProto npEnd = wpEnd.getNodeType();
			PrimitivePort ppEnd = null;
			if (wpEnd.getNodePort() != null)
				ppEnd = wpEnd.getNodePort().getBasePort();
			if (ppEnd == null)
				ppEnd = ((PortProto)npEnd.getPorts().next()).getBasePort();

			// determine the type of arc to run: see if default works
			ArcProto useAp = getArcConnectedToPorts(ppStart, ppEnd);
			if (useAp == null)
			{
				// no single arc can connect these: may need a contact
				return null;
			}

			WiringPlan [] inbetween = getWiringPath(wpStartList[0], useAp, wpEndList[0], useAp);
			if (inbetween == null) return null;

			WiringPlan [] returnValue = new WiringPlan[wpStartList.length + inbetween.length + wpEndList.length];
			int i=0;
			for(int j=0; j<wpStartList.length; j++)
				returnValue[i++] = wpStartList[j];
			for(int j=0; j<wpEndList.length; j++)
				returnValue[i++] = wpEndList[j];
			for(int j=0; j<inbetween.length; j++)
				returnValue[i++] = inbetween[j];
			return returnValue;
		}
	}
}
