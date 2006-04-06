/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OutlineListener.java
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

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.menus.EditMenu;
import com.sun.electric.tool.user.*;

import java.awt.Point;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;

/**
 * Class to make changes to the outline information on a node.
 */
public class OutlineListener
	implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
{
	public static OutlineListener theOne = new OutlineListener();
	private double oldX, oldY;
	private boolean doingMotionDrag;
	private int point;
	private NodeInst outlineNode;
	private Highlight2 high;

	private OutlineListener() {}

	public void setNode(NodeInst ni)
	{
		EditWindow wnd = EditWindow.getCurrent();
        Highlighter highlighter = wnd.getHighlighter();
        high = highlighter.getOneHighlight();
        point = 0;

		outlineNode = ni;
		Point2D [] origPoints = outlineNode.getTrace();
		if (origPoints == null)
		{
			// node has no points: fake some
			if (ni.getFunction() == PrimitiveNode.Function.NODE)
			{
				InitializePoints job = new InitializePoints(this, ni);
				return;
			}
		}

		high.setPoint(0);
		if (wnd != null) wnd.repaintContents(null, false);
	}

	/**
	 * Class to initialize the points on an outline node that has no points.
	 */
	private static class InitializePoints extends Job
	{
		private transient OutlineListener listener;
		private NodeInst ni;

		protected InitializePoints(OutlineListener listener, NodeInst ni)
		{
			super("Initialize Outline Points", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.listener = listener;
			this.ni = ni;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			EPoint [] points = new EPoint[4];
			double halfWid = ni.getXSize() / 2;
			double halfHei = ni.getYSize() / 2;
			points[0] = new EPoint(-halfWid, -halfHei);
			points[1] = new EPoint(-halfWid,  halfHei);
			points[2] = new EPoint( halfWid,  halfHei);
			points[3] = new EPoint( halfWid, -halfHei);
			ni.newVar(NodeInst.TRACE, points);
			return true;
		}

        public void terminateOK()
        {
			listener.high.setPoint(0);
			EditWindow wnd = EditWindow.getCurrent();
			if (wnd != null) wnd.repaintContents(null, false);
        }
	}

	public void mousePressed(MouseEvent evt)
	{
		int x = evt.getX();
		int y = evt.getY();
		EditWindow wnd = (EditWindow)evt.getSource();
        Highlighter highlighter = wnd.getHighlighter();

		// show "get info" on double-click
		if (evt.getClickCount() == 2)
		{
			if (highlighter.getNumHighlights() >= 1)
			{
				EditMenu.getInfoCommand(true);
				return;
			}
		}

		// right click: add a point
		if (ClickZoomWireListener.isRightMouse(evt))
		{
			// add a point
			AffineTransform trans = outlineNode.rotateOutAboutTrueCenter();
			Point2D [] origPoints = outlineNode.getTrace();
			if (origPoints == null)
			{
				Point2D [] newPoints = new Point2D[1];
				newPoints[0] = new Point2D.Double(outlineNode.getAnchorCenterX(), outlineNode.getAnchorCenterY());
				EditWindow.gridAlign(newPoints[0]);
				setNewPoints(newPoints, 0);
				point = 0;
				oldX = newPoints[point].getX();
				oldY = newPoints[point].getY();
			} else if (origPoints.length == 1)
			{
				Point2D [] newPoints = new Point2D[2];
				newPoints[0] = new Point2D.Double(outlineNode.getAnchorCenterX() + origPoints[0].getX(),
					outlineNode.getAnchorCenterY() + origPoints[0].getY());
				trans.transform(newPoints[0], newPoints[0]);
				EditWindow.gridAlign(newPoints[0]);
				newPoints[1] = new Point2D.Double(newPoints[0].getX() + 2, newPoints[0].getY() + 2);
				EditWindow.gridAlign(newPoints[1]);
				setNewPoints(newPoints, 1);
				point = 1;
				oldX = newPoints[point].getX();
				oldY = newPoints[point].getY();
			} else
			{
				Point2D [] newPoints = new Point2D[origPoints.length+1];
				int j = 0;
				for(int i=0; i<origPoints.length; i++)
				{
					// copy the original point
					newPoints[j++] = new Point2D.Double(outlineNode.getAnchorCenterX() + origPoints[i].getX(),
						outlineNode.getAnchorCenterY() + origPoints[i].getY());
					if (i == point)
					{
						// found the selected point, make the insertion
						newPoints[j] = wnd.screenToDatabase(x, y);
						EditWindow.gridAlign(newPoints[j]);
						j++;
					}
				}
				trans.transform(newPoints, 0, newPoints, 0, newPoints.length);
				setNewPoints(newPoints, point+1);
				oldX = newPoints[point+1].getX();
				oldY = newPoints[point+1].getY();
			}
			doingMotionDrag = true;
			wnd.repaint();
			return;
		}

		// standard click-and-drag: see if cursor is over anything
		Point2D pt = wnd.screenToDatabase(x, y);
		Highlight2 found = highlighter.findObject(pt, wnd, true, false, false, false, true, false, false);
		doingMotionDrag = false;
		if (found != null)
		{
			high = highlighter.getOneHighlight();
            assert (high == found);
			outlineNode = (NodeInst)highlighter.getOneElectricObject(NodeInst.class);
			if (high != null && outlineNode != null)
			{
				point = high.getPoint();
				Point2D [] origPoints = outlineNode.getTrace();
				if (origPoints != null)
				{
					doingMotionDrag = true;
					EditWindow.gridAlign(pt);
					oldX = pt.getX();
					oldY = pt.getY();
				}
			}
		}
		wnd.repaint();
	}

	public void mouseReleased(MouseEvent evt)
	{
		EditWindow wnd = (EditWindow)evt.getSource();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

		// handle moving the selected point
		if (doingMotionDrag)
		{
			doingMotionDrag = false;

			int newX = evt.getX();
			int newY = evt.getY();
			Point2D curPt = wnd.screenToDatabase(newX, newY);
			EditWindow.gridAlign(curPt);
			highlighter.setHighlightOffset(0, 0);

			moveSelectedPoint(curPt.getX() - oldX, curPt.getY() - oldY);
			wnd.repaintContents(null, false);
		}
	}

	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mouseMoved(MouseEvent evt) {}

	public void mouseDragged(MouseEvent evt)
	{
		EditWindow wnd = (EditWindow)evt.getSource();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

		int newX = evt.getX();
		int newY = evt.getY();
		Point2D curPt = wnd.screenToDatabase(newX, newY);
		EditWindow.gridAlign(curPt);
		Point pt = wnd.databaseToScreen(curPt.getX(), curPt.getY());

		Point gridPt = wnd.databaseToScreen(oldX, oldY);

		// show moving of the selected point
		if (doingMotionDrag)
		{
			highlighter.setHighlightOffset(pt.x - gridPt.x, pt.y - gridPt.y);
			wnd.repaint();
			return;
		}
		wnd.repaint();
	}

	public void mouseWheelMoved(MouseWheelEvent evt)
	{
		int clicks = evt.getWheelRotation();
	}

	public void keyPressed(KeyEvent evt)
	{
		int chr = evt.getKeyCode();
		EditWindow wnd = (EditWindow)evt.getSource();
		Cell cell = wnd.getCell();
        if (cell == null) return;

		if (chr == KeyEvent.VK_DELETE || chr == KeyEvent.VK_BACK_SPACE)
		{
			// delete a point
			Point2D [] origPoints = outlineNode.getTrace();
			if (origPoints.length <= 2)
			{
				System.out.println("Cannot delete the last point on an outline");
				return;
			}
			AffineTransform trans = outlineNode.rotateOutAboutTrueCenter();
			Point2D [] newPoints = new Point2D[origPoints.length-1];
			int pt = point;
			int j = 0;
			for(int i=0; i<origPoints.length; i++)
			{
				if (i == pt) continue;
				newPoints[j] = new Point2D.Double(outlineNode.getAnchorCenterX() + origPoints[i].getX(),
					outlineNode.getAnchorCenterY() + origPoints[i].getY());
				trans.transform(newPoints[j], newPoints[j]);
				j++;
			}
			if (pt > 0) pt--;
			setNewPoints(newPoints, pt);
		} else if (chr == KeyEvent.VK_LEFT)
		{
			double arrowDistance = User.getAlignmentToGrid();
			moveSelectedPoint(-arrowDistance, 0);
		} else if (chr == KeyEvent.VK_RIGHT)
		{
			double arrowDistance = User.getAlignmentToGrid();
			moveSelectedPoint(arrowDistance, 0);
		} else if (chr == KeyEvent.VK_UP)
		{
			double arrowDistance = User.getAlignmentToGrid();
			moveSelectedPoint(0, arrowDistance);
		} else if (chr == KeyEvent.VK_DOWN)
		{
			double arrowDistance = User.getAlignmentToGrid();
			moveSelectedPoint(0, -arrowDistance);
		} else if (chr == KeyEvent.VK_PERIOD)
		{
			// advance to next point
			Point2D [] origPoints = outlineNode.getTrace();
			int nextPoint = point + 1;
			if (nextPoint >= origPoints.length) nextPoint = 0;
			high.setPoint(point = nextPoint);
			wnd.repaint();
		} else if (chr == KeyEvent.VK_COMMA)
		{
			// backup to previous point
			Point2D [] origPoints = outlineNode.getTrace();
			int prevPoint = point - 1;
			if (prevPoint < 0) prevPoint = origPoints.length - 1;
			high.setPoint(point = prevPoint);
			wnd.repaint();
		}
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

	private void moveSelectedPoint(double dx, double dy)
	{
		Point2D [] origPoints = outlineNode.getTrace();
		if (origPoints == null) return;

		Point2D [] newPoints = new Point2D[origPoints.length];
		for(int i=0; i<origPoints.length; i++)
		{
			newPoints[i] = new Point2D.Double(outlineNode.getAnchorCenterX() + origPoints[i].getX(),
				outlineNode.getAnchorCenterY() + origPoints[i].getY());
		}
		AffineTransform trans = outlineNode.rotateOutAboutTrueCenter();
		trans.transform(newPoints, 0, newPoints, 0, newPoints.length);
		newPoints[point].setLocation(newPoints[point].getX()+dx, newPoints[point].getY()+dy);
		setNewPoints(newPoints, point);
	}

	private void setNewPoints(Point2D [] newPoints, int newPoint)
	{
		double [] x = new double[newPoints.length];
		double [] y = new double[newPoints.length];
		for(int i=0; i<newPoints.length; i++)
		{
			x[i] = newPoints[i].getX();
			y[i] = newPoints[i].getY();
		}
		SetPoints job = new SetPoints(this, outlineNode, x, y, newPoint);
	}

	/**
	 * Class to change the points on an outline node.
	 */
	private static class SetPoints extends Job
	{
		private transient OutlineListener listener;
		private NodeInst ni;
		private double [] x, y;
		private int newPoint;

		protected SetPoints(OutlineListener listener, NodeInst ni, double [] x, double [] y, int newPoint)
		{
			super("Change Outline Points", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.listener = listener;
			this.ni = ni;
			this.x = x;
			this.y = y;
			this.newPoint = newPoint;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// make sure outline adjustment is allowed
			if (CircuitChangeJobs.cantEdit(ni.getParent(), ni, true) != 0) return false;

			// get the extent of the data
			Point2D [] points = new Point2D[x.length];
			for(int i=0; i<x.length; i++)
			{
				points[i] = new Point2D.Double(x[i], y[i]);
			}
			ni.setTrace(points);
			return true;
		}

        public void terminateOK()
        {
			listener.high.setPoint(listener.point = newPoint);
        }
	}

}
