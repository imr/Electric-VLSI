/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SizeListener.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;

import java.awt.Cursor;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.util.EventListener;
import java.util.List;

public class SizeListener
	implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
{
	private Geometric stretchGeom;
	private EventListener oldListener;
	private Cursor oldCursor;
	private static Cursor sizeCursor = ToolBar.readCursor("CursorSize.gif", 14, 14);

	private SizeListener() {}

	public static void sizeObjects()
	{
		List geomList = Highlight.getHighlighted(true, true);
		if (geomList == null) return;
		if (geomList.size() != 1)
		{
			System.out.println("Select just one object to size");
			return;
		}
		Geometric geom = (Geometric)geomList.get(0);
		if (geom instanceof Geometric)
		{
			EventListener newListener = null;

			// remember the listener that was there before
			EventListener oldListener = WindowFrame.getListener();
			Cursor oldCursor = TopLevel.getCurrentCursor();

			System.out.println("Click to stretch " + geom.describe());
			newListener = oldListener;
			if (newListener == null || !(newListener instanceof SizeListener))
			{
				newListener = new SizeListener();
				WindowFrame.setListener(newListener);
			}
			((SizeListener)newListener).stretchGeom = geom;
			((SizeListener)newListener).oldListener = oldListener;
			((SizeListener)newListener).oldCursor = oldCursor;

			// change the cursor
			TopLevel.setCurrentCursor(sizeCursor);
		}
	}

	public void mousePressed(MouseEvent evt)
	{
		showHighlight(evt);
	}

	public void mouseMoved(MouseEvent evt)
	{
		showHighlight(evt);
	}

	public void mouseDragged(MouseEvent evt)
	{
		showHighlight(evt);
	}

	public void mouseReleased(MouseEvent evt)
	{
		// restore the listener to the former state
		WindowFrame.setListener(oldListener);
		TopLevel.setCurrentCursor(oldCursor);
		showHighlight(null);

		// handle scaling the selected objects
		EditWindow.CircuitPart ecp = (EditWindow.CircuitPart)evt.getSource();
		EditWindow wnd = ecp.getEditWindow();
		Point2D newSize = getNewSize(evt);
		ScaleObject job = new ScaleObject(stretchGeom, newSize);
		wnd.repaint();
	}

	public void keyPressed(KeyEvent evt)
	{
		int chr = evt.getKeyCode();
		EditWindow.CircuitPart ecp = (EditWindow.CircuitPart)evt.getSource();
		EditWindow wnd = ecp.getEditWindow();
		Cell cell = wnd.getCell();
        if (cell == null) return;

		// "A" for abort
		if (chr == KeyEvent.VK_A)
		{
			// restore the listener to the former state
			WindowFrame.setListener(oldListener);
			TopLevel.setCurrentCursor(oldCursor);
			showHighlight(null);
			System.out.println("Aborted");
		}
	}

	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mouseWheelMoved(MouseWheelEvent evt) {}
	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

	private void showHighlight(MouseEvent evt)
	{
		Highlight.clear();
		Highlight.addElectricObject(stretchGeom, stretchGeom.getParent());
		Highlight.finished();
		if (evt != null)
		{
			EditWindow.CircuitPart ecp = (EditWindow.CircuitPart)evt.getSource();
			EditWindow wnd = ecp.getEditWindow();
			Point2D newSize = getNewSize(evt);
			if (stretchGeom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)stretchGeom;
				SizeOffset so = Technology.getSizeOffset(ni);
				AffineTransform trans = ni.rotateOutAboutTrueCenter();

				double stretchedLowX = ni.getTrueCenterX() - newSize.getX()/2 + so.getLowXOffset();
				double stretchedHighX = ni.getTrueCenterX() + newSize.getX()/2 - so.getHighXOffset();
				double stretchedLowY = ni.getTrueCenterY() - newSize.getY()/2 + so.getLowYOffset();
				double stretchedHighY = ni.getTrueCenterY() + newSize.getY()/2 - so.getHighYOffset();
				Poly stretchedPoly = new Poly((stretchedLowX+stretchedHighX)/2, (stretchedLowY+stretchedHighY)/2,
					stretchedHighX-stretchedLowX, stretchedHighY-stretchedLowY);
				stretchedPoly.transform(trans);
				Point2D [] stretchedPoints = stretchedPoly.getPoints();
				for(int i=0; i<stretchedPoints.length; i++)
				{
					int lastI = i - 1;
					if (lastI < 0) lastI = stretchedPoints.length - 1;
					Highlight.addLine(stretchedPoints[lastI], stretchedPoints[i], ni.getParent());
				}
			} else
			{
				// construct the polygons that describe the basic arc
				ArcInst ai = (ArcInst)stretchGeom;
				Poly stretchedPoly = ai.makePoly(ai.getLength(), newSize.getX() - ai.getProto().getWidthOffset(), Poly.Type.CLOSED);
				if (stretchedPoly == null) return;
				Point2D [] stretchedPoints = stretchedPoly.getPoints();
				for(int i=0; i<stretchedPoints.length; i++)
				{
					int lastI = i - 1;
					if (lastI < 0) lastI = stretchedPoints.length - 1;
					Highlight.addLine(stretchedPoints[lastI], stretchedPoints[i], ai.getParent());
				}
			}
			wnd.repaint();
		}
	}

	/**
	 * Method to determine the proper size for the geometric being stretched, given a cursor location.
	 * @param evt the event with the current cursor location.
	 */
	private Point2D getNewSize(MouseEvent evt)
	{
		// get the coordinates of the cursor in database coordinates
		EditWindow.CircuitPart ecp = (EditWindow.CircuitPart)evt.getSource();
		EditWindow wnd = ecp.getEditWindow();
		int oldx = evt.getX();
		int oldy = evt.getY();
		Point2D pt = wnd.screenToDatabase(oldx, oldy);

		// different code for nodes and arcs
		if (stretchGeom instanceof NodeInst)
		{
			// get information about the node being stretched
			NodeInst ni = (NodeInst)stretchGeom;
			NodeProto np = ni.getProto();
			AffineTransform trans = ni.rotateOutAboutTrueCenter();
			SizeOffset so = Technology.getSizeOffset(ni);

			// setup outline of node with standard offset
			double nodeLowX = ni.getTrueCenterX() - ni.getXSize()/2 + so.getLowXOffset();
			double nodeHighX = ni.getTrueCenterX() + ni.getXSize()/2 - so.getHighXOffset();
			double nodeLowY = ni.getTrueCenterY() - ni.getYSize()/2 + so.getLowYOffset();
			double nodeHighY = ni.getTrueCenterY() + ni.getYSize()/2 - so.getHighYOffset();
			Poly nodePoly = new Poly((nodeLowX+nodeHighX)/2, (nodeLowY+nodeHighY)/2, nodeHighX-nodeLowX, nodeHighY-nodeLowY);
			nodePoly.transform(trans);

			// determine the closest point on the outline
			Point2D [] points = nodePoly.getPoints();
			double closestDist = Double.MAX_VALUE;
			Point2D closest = null;
			for(int i=0; i<points.length; i++)
			{
				double dist = pt.distance(points[i]);
				if (dist < closestDist)
				{
					closestDist = dist;
					closest = points[i];
				}
			}

			// determine the amount of growth of the node
			double ptToCenterX = Math.abs(pt.getX() - ni.getTrueCenterX());
			double closestToCenterX = Math.abs(closest.getX() - ni.getTrueCenterX());
			double ptToCenterY = Math.abs(pt.getY() - ni.getTrueCenterY());
			double closestToCenterY = Math.abs(closest.getY() - ni.getTrueCenterY());
			double growthRatioX = ptToCenterX / closestToCenterX;
			double growthRatioY = ptToCenterY / closestToCenterY;
			AffineTransform transIn = ni.pureRotateIn();
			Point2D delta = new Point2D.Double(growthRatioX, growthRatioY);
			transIn.transform(delta, delta);

			// compute the new node size
			double newXSize = (ni.getXSize() - so.getLowXOffset() - so.getHighXOffset()) * delta.getX();
			double newYSize = (ni.getYSize() - so.getLowYOffset() - so.getHighYOffset()) * delta.getY();
			Point2D newSize = new Point2D.Double(newXSize, newYSize);

			// grid align the new node size
			EditWindow.gridAlign(newSize);
			newSize.setLocation(newSize.getX() + so.getLowXOffset() + so.getHighXOffset(),
				newSize.getY() + so.getLowYOffset() + so.getHighYOffset());
			return newSize;
		}

		// get information about the arc being stretched
		ArcInst ai = (ArcInst)stretchGeom;
		ArcProto ap = ai.getProto();
		double offset = ap.getWidthOffset();

		// determine point on arc that is closest to the cursor
		Point2D ptOnLine = EMath.closestPointToLine(ai.getHead().getLocation(), ai.getTail().getLocation(), pt);
		double newWidth = ptOnLine.distance(pt)*2 + offset;
		Point2D newSize = new Point2D.Double(newWidth, newWidth);
		EditWindow.gridAlign(newSize);
		return newSize;
	}

	protected static class ScaleObject extends Job
	{
		private Geometric stretchGeom;
		private Point2D stretchPt;

		protected ScaleObject(Geometric stretchGeom, Point2D stretchPt)
		{
			super("Scale node", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.stretchGeom = stretchGeom;
			this.stretchPt = stretchPt;
			startJob();
		}

		public void doIt()
		{
			if (stretchGeom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)stretchGeom;
				ni.modifyInstance(0, 0, stretchPt.getX() - ni.getXSize(), stretchPt.getY() - ni.getYSize(), 0);
			} else
			{
				ArcInst ai = (ArcInst)stretchGeom;
				ai.modify(stretchPt.getX() - ai.getWidth(), 0, 0, 0, 0);
			}
		}
	}

}
