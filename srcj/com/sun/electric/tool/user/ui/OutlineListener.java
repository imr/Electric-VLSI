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
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.MenuCommands;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.Point;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

class OutlineListener
	implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
{
	public static OutlineListener theOne = new OutlineListener();
	private int oldx, oldy;
	private boolean doingMotionDrag;
	private int point;
	private NodeInst outlineNode;
	private Highlight high;

	private OutlineListener() {}

	public void setNode(NodeInst ni)
	{
		outlineNode = ni;
		high = Highlight.getOneHighlight();
		high.setPoint(0);
		point = 0;
		EditWindow wnd = EditWindow.getCurrent();
		if (wnd != null) wnd.repaintContents();
	}

	public void mousePressed(MouseEvent evt)
	{
		oldx = evt.getX();
		oldy = evt.getY();
		EditWindow wnd = (EditWindow)evt.getSource();
		Cell cell = wnd.getCell();
        if (cell == null) return;

		// show "get info" on double-click
		if (evt.getClickCount() == 2)
		{
			if (Highlight.getNumHighlights() >= 1)
			{
				MenuCommands.getInfoCommand();
				return;
			}
		}

		// standard click-and-drag: see if cursor is over anything
		Point2D pt = wnd.screenToDatabase(oldx, oldy);
		int numFound = Highlight.findObject(pt, wnd, true, false, false, false, true, false, false);
		doingMotionDrag = false;
		if (numFound != 0)
		{
			high = Highlight.getOneHighlight();
			outlineNode = (NodeInst)Highlight.getOneElectricObject(NodeInst.class);
			if (high != null && outlineNode != null)
			{
				doingMotionDrag = true;
				point = high.getPoint();
			}
		}
		wnd.repaint();
	}

	public void mouseReleased(MouseEvent evt)
	{
		EditWindow wnd = (EditWindow)evt.getSource();
		Cell cell = wnd.getCell();
        if (cell == null) return;

		// handle moving the selected point
		if (doingMotionDrag)
		{
			doingMotionDrag = false;
			int newX = evt.getX();
			int newY = evt.getY();
			Point2D delta = wnd.deltaScreenToDatabase(newX - oldx, newY - oldy);
			EditWindow.gridAlign(delta);
			if (delta.getX() == 0 && delta.getY() == 0) return;
			Highlight.setHighlightOffset(0, 0);

			moveSelectedPoint(delta.getX(), delta.getY());
			wnd.repaintContents();
		}
	}

	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mouseMoved(MouseEvent evt) {}

	public void mouseDragged(MouseEvent evt)
	{
		int newX = evt.getX();
		int newY = evt.getY();
		EditWindow wnd = (EditWindow)evt.getSource();

		Point2D delta = wnd.deltaScreenToDatabase(newX - oldx, newY - oldy);
		wnd.gridAlign(delta);
		Point pt = wnd.deltaDatabaseToScreen(delta.getX(), delta.getY());
		newX = oldx + pt.x;   newY = oldy + pt.y;

		// show moving of the selected point
		if (doingMotionDrag)
		{
			Highlight.setHighlightOffset(newX - oldx, newY - oldy);
			wnd.repaint();
			return;
		}
		oldx = newX;
		oldy = newY;
		wnd.repaint();
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
				newPoints[j] = new Point2D.Double(outlineNode.getGrabCenterX() + origPoints[i].getX(),
					outlineNode.getGrabCenterY() + origPoints[i].getY());
				trans.transform(newPoints[j], newPoints[j]);
				j++;
			}
			if (pt > 0) pt--;
			setNewPoints(newPoints, pt);
		} else if (chr == KeyEvent.VK_LEFT)
		{
			double arrowDistance = ToolBar.getArrowDistance();
			moveSelectedPoint(-arrowDistance, 0);
		} else if (chr == KeyEvent.VK_RIGHT)
		{
			double arrowDistance = ToolBar.getArrowDistance();
			moveSelectedPoint(arrowDistance, 0);
		} else if (chr == KeyEvent.VK_UP)
		{
			double arrowDistance = ToolBar.getArrowDistance();
			moveSelectedPoint(0, arrowDistance);
		} else if (chr == KeyEvent.VK_DOWN)
		{
			double arrowDistance = ToolBar.getArrowDistance();
			moveSelectedPoint(0, -arrowDistance);
		} else if (chr == KeyEvent.VK_A)
		{
			// add a point
			AffineTransform trans = outlineNode.rotateOutAboutTrueCenter();
			Point2D [] origPoints = outlineNode.getTrace();
			if (origPoints == null)
			{
				Point2D [] newPoints = new Point2D[1];
				newPoints[0] = new Point2D.Double(outlineNode.getGrabCenterX(), outlineNode.getGrabCenterY());
				setNewPoints(newPoints, 0);
				return;
			}
			if (origPoints.length == 1)
			{
				Point2D [] newPoints = new Point2D[2];
				newPoints[0] = new Point2D.Double(outlineNode.getGrabCenterX() + origPoints[0].getX(),
					outlineNode.getGrabCenterY() + origPoints[0].getY());
				trans.transform(newPoints[0], newPoints[0]);
				newPoints[1] = new Point2D.Double(newPoints[0].getX() + 2, newPoints[0].getY() + 2);
				setNewPoints(newPoints, 1);
				return;
			}
			Point2D [] newPoints = new Point2D[origPoints.length+1];
			int j = 0;
			for(int i=0; i<origPoints.length; i++)
			{
				// copy the original point
				newPoints[j++] = new Point2D.Double(outlineNode.getGrabCenterX() + origPoints[i].getX(),
					outlineNode.getGrabCenterY() + origPoints[i].getY());
				if (i == point)
				{
					// found the selected point, make the insertion
					if (i+1 >= origPoints.length)
					{
						// insertion point at the end: figure out what to do
						if (outlineNode.traceWraps())
						{
							// outline wraps: make this between here and first point
							newPoints[j++] = new Point2D.Double(outlineNode.getGrabCenterX() + (origPoints[i].getX() + origPoints[0].getX()) / 2,
								outlineNode.getGrabCenterY() + (origPoints[i].getY() + origPoints[0].getY()) / 2);
						} else
						{
							// outline does not wrap: make new one be relative to previous one
							newPoints[j++] = new Point2D.Double(outlineNode.getGrabCenterX() + origPoints[i].getX()*2 + origPoints[i-1].getX(),
								outlineNode.getGrabCenterY() + origPoints[i].getY()*2 + origPoints[i-1].getY());
						}
					} else
					{
						// there is a "next" point, make this one between here and there
						newPoints[j++] = new Point2D.Double(outlineNode.getGrabCenterX() + (origPoints[i].getX() + origPoints[i+1].getX()) / 2,
							outlineNode.getGrabCenterY() + (origPoints[i].getY() + origPoints[i+1].getY()) / 2);
					}
				}
			}
			trans.transform(newPoints, 0, newPoints, 0, newPoints.length);
			setNewPoints(newPoints, point+1);
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
			newPoints[i] = new Point2D.Double(outlineNode.getGrabCenterX() + origPoints[i].getX(),
				outlineNode.getGrabCenterY() + origPoints[i].getY());
		}
		AffineTransform trans = outlineNode.rotateOutAboutTrueCenter();
		trans.transform(newPoints, 0, newPoints, 0, newPoints.length);
		newPoints[point].setLocation(newPoints[point].getX()+dx, newPoints[point].getY()+dy);
		setNewPoints(newPoints, point);
	}

	private void setNewPoints(Point2D [] newPoints, int newPoint)
	{
		SetPoints job = new SetPoints(this, newPoints, newPoint);
	}

	/**
	 * Class to change the points on an outline node.
	 */
	protected static class SetPoints extends Job
	{
		OutlineListener listener;
		Point2D [] newPoints;
		int newPoint;

		protected SetPoints(OutlineListener listener, Point2D [] newPoints, int newPoint)
		{
			super("Change Outline Points", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.listener = listener;
			this.newPoints = newPoints;
			this.newPoint = newPoint;
			startJob();
		}

		public void doIt()
		{
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
			ni.modifyInstance(newCX-ni.getGrabCenterX(),newCY-ni.getGrabCenterY(), newSX-ni.getXSize(),
				newSY-ni.getYSize(), -ni.getAngle());
			listener.high.setPoint(listener.point = newPoint);
		}
	}

}
