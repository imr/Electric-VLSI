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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.menus.MenuCommands;
import com.sun.electric.tool.user.menus.EditMenu;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Highlighter;

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

class OutlineListener
	implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
{
	public static OutlineListener theOne = new OutlineListener();
	private double oldX, oldY;
	private boolean doingMotionDrag;
	private int point;
	private NodeInst outlineNode;
	private Highlight high;

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
		if (wnd != null) wnd.repaintContents(null);
	}

	/**
	 * Class to initialize the points on an outline node that has no points.
	 */
	private static class InitializePoints extends Job
	{
		OutlineListener listener;
		NodeInst ni;

		protected InitializePoints(OutlineListener listener, NodeInst ni)
		{
			super("Initialize Outline Points", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.listener = listener;
			this.ni = ni;
			startJob();
		}

		public boolean doIt()
		{
			Point2D [] points = new Point2D[4];
			double halfWid = ni.getXSize() / 2;
			double halfHei = ni.getYSize() / 2;
			points[0] = new Point2D.Double(-halfWid, -halfHei);
			points[1] = new Point2D.Double(-halfWid,  halfHei);
			points[2] = new Point2D.Double( halfWid,  halfHei);
			points[3] = new Point2D.Double( halfWid, -halfHei);
			ni.newVar(NodeInst.TRACE, points);
			listener.high.setPoint(0);
			EditWindow wnd = EditWindow.getCurrent();
			if (wnd != null) wnd.repaintContents(null);
			return true;
		}
	}

	public void mousePressed(MouseEvent evt)
	{
		int x = evt.getX();
		int y = evt.getY();
		EditWindow wnd = (EditWindow)evt.getSource();
        Highlighter highlighter = wnd.getHighlighter();
		Cell cell = wnd.getCell();
        if (cell == null) return;

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
				setNewPoints(newPoints, 0, cell);
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
				setNewPoints(newPoints, 1, cell);
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
						if (i+1 >= origPoints.length)
						{
							// insertion point at the end: figure out what to do
							if (outlineNode.traceWraps())
							{
								// outline wraps: make this between here and first point
								newPoints[j] = new Point2D.Double(outlineNode.getAnchorCenterX() + (origPoints[i].getX() + origPoints[0].getX()) / 2,
									outlineNode.getAnchorCenterY() + (origPoints[i].getY() + origPoints[0].getY()) / 2);
								EditWindow.gridAlign(newPoints[j]);
								j++;
							} else
							{
								// outline does not wrap: make new one be relative to previous one
								newPoints[j] = new Point2D.Double(outlineNode.getAnchorCenterX() + origPoints[i].getX()*2 + origPoints[i-1].getX(),
									outlineNode.getAnchorCenterY() + origPoints[i].getY()*2 + origPoints[i-1].getY());
								EditWindow.gridAlign(newPoints[j]);
								j++;
							}
						} else
						{
							// there is a "next" point, make this one between here and there
							newPoints[j] = new Point2D.Double(outlineNode.getAnchorCenterX() + (origPoints[i].getX() + origPoints[i+1].getX()) / 2,
								outlineNode.getAnchorCenterY() + (origPoints[i].getY() + origPoints[i+1].getY()) / 2);
							EditWindow.gridAlign(newPoints[j]);
							j++;
						}
					}
				}
				trans.transform(newPoints, 0, newPoints, 0, newPoints.length);
				setNewPoints(newPoints, point+1, cell);
				oldX = newPoints[point+1].getX();
				oldY = newPoints[point+1].getY();
			}
			doingMotionDrag = true;
			wnd.repaint();
			return;
		}

		// standard click-and-drag: see if cursor is over anything
		Point2D pt = wnd.screenToDatabase(x, y);
		int numFound = highlighter.findObject(pt, wnd, true, false, false, false, true, false, false);
		doingMotionDrag = false;
		if (numFound != 0)
		{
			high = highlighter.getOneHighlight();
			outlineNode = (NodeInst)highlighter.getOneElectricObject(NodeInst.class);
			if (high != null && outlineNode != null)
			{
				point = high.getPoint();
				Point2D [] origPoints = outlineNode.getTrace();
				if (origPoints != null)
				{
					doingMotionDrag = true;
//					oldX = outlineNode.getAnchorCenterX() + origPoints[point].getX();
//					oldY = outlineNode.getAnchorCenterY() + origPoints[point].getY();
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
		Cell cell = wnd.getCell();
        if (cell == null) return;

		// handle moving the selected point
		if (doingMotionDrag)
		{
			doingMotionDrag = false;

			int newX = evt.getX();
			int newY = evt.getY();
			Point2D curPt = wnd.screenToDatabase(newX, newY);
			EditWindow.gridAlign(curPt);
			highlighter.setHighlightOffset(0, 0);

			moveSelectedPoint(curPt.getX() - oldX, curPt.getY() - oldY, cell);
			wnd.repaintContents(null);
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
			setNewPoints(newPoints, pt, cell);
		} else if (chr == KeyEvent.VK_LEFT)
		{
			double arrowDistance = User.getAlignmentToGrid();
			moveSelectedPoint(-arrowDistance, 0, cell);
		} else if (chr == KeyEvent.VK_RIGHT)
		{
			double arrowDistance = User.getAlignmentToGrid();
			moveSelectedPoint(arrowDistance, 0, cell);
		} else if (chr == KeyEvent.VK_UP)
		{
			double arrowDistance = User.getAlignmentToGrid();
			moveSelectedPoint(0, arrowDistance, cell);
		} else if (chr == KeyEvent.VK_DOWN)
		{
			double arrowDistance = User.getAlignmentToGrid();
			moveSelectedPoint(0, -arrowDistance, cell);
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

	private void moveSelectedPoint(double dx, double dy, Cell cell)
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
		setNewPoints(newPoints, point, cell);
	}

	private void setNewPoints(Point2D [] newPoints, int newPoint, Cell cell)
	{
		SetPoints job = new SetPoints(this, newPoints, newPoint, cell);
	}

	/**
	 * Class to change the points on an outline node.
	 */
	private static class SetPoints extends Job
	{
		private OutlineListener listener;
		private Point2D [] newPoints;
		private int newPoint;
		private Cell cell;

		protected SetPoints(OutlineListener listener, Point2D [] newPoints, int newPoint, Cell cell)
		{
			super("Change Outline Points", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.listener = listener;
			this.newPoints = newPoints;
			this.newPoint = newPoint;
			this.cell = cell;
			startJob();
		}

		public boolean doIt()
		{
			// make sure outline adjustment is allowed
			if (CircuitChanges.cantEdit(cell, null, true) != 0) return false;

			// get the extent of the data
			NodeInst ni = listener.outlineNode;
			double lX = newPoints[0].getX();
			double hX = lX;
			double lY = newPoints[0].getY();
			double hY = lY;
			for(int i=1; i<newPoints.length; i++)
			{
				double x = newPoints[i].getX();
				if (x < lX) lX = x;
				if (x > hX) hX = x;
				double y = newPoints[i].getY();
				if (y < lY) lY = y;
				if (y > hY) hY = y;
			}
			double newCX = (lX + hX) / 2;
			double newCY = (lY + hY) / 2;
			double newSX = hX - lX;
			double newSY = hY - lY;
			for(int i=0; i<newPoints.length; i++)
				newPoints[i].setLocation(newPoints[i].getX() - newCX, newPoints[i].getY() - newCY);

			// update the points
			ni.newVar(NodeInst.TRACE, newPoints);
			ni.modifyInstance(newCX-ni.getAnchorCenterX(),newCY-ni.getAnchorCenterY(), newSX-ni.getXSize(),
				newSY-ni.getYSize(), -ni.getAngle());
			listener.high.setPoint(listener.point = newPoint);
			return true;
		}
	}

}
