/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ZoomAndPanListener.java
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

import com.sun.electric.tool.user.Highlight;
import com.sun.electric.database.hierarchy.Cell;

import java.awt.Dimension;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class ZoomAndPanListener
	implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
{
	public static ZoomAndPanListener theOne = new ZoomAndPanListener();
	private int startX, startY, lastX, lastY;
	private ToolBar.CursorMode mode;

	private ZoomAndPanListener() {}

	public void mousePressed(MouseEvent evt)
	{
		lastX = startX = evt.getX();
		lastY = startY = evt.getY();

		mode = ToolBar.getCursorMode();
		if (mode == ToolBar.CursorMode.ZOOM)
		{
			if ((evt.getModifiers()&MouseEvent.CTRL_MASK) != 0) mode = ToolBar.CursorMode.PAN;
		} else if (mode == ToolBar.CursorMode.PAN)
		{
			if ((evt.getModifiers()&MouseEvent.CTRL_MASK) != 0) mode = ToolBar.CursorMode.ZOOM;
		}
		setProperCursor(evt);
	}

	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}

	public void mouseMoved(MouseEvent evt)
	{
		setProperCursor(evt);
	}
	
	public static void setProperCursor(MouseEvent evt)
	{
		if (ToolBar.getCursorMode() == ToolBar.CursorMode.ZOOM)
		{
			if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) != 0)
			{
				TopLevel.setCurrentCursor(ToolBar.zoomOutCursor);
			} else
			{
				TopLevel.setCurrentCursor(ToolBar.zoomCursor);
			}
		}
	}

	public void mouseDragged(MouseEvent evt)
	{
		setProperCursor(evt);
		int newX = evt.getX();
		int newY = evt.getY();
 		if (evt.getSource() instanceof EditWindow)
		{
			EditWindow wnd = (EditWindow)evt.getSource();

			double scale = wnd.getScale();
			if (mode == ToolBar.CursorMode.ZOOM)
			{
				// zooming the window scale
				Highlight.clear();
				Point2D start = wnd.screenToDatabase(startX, startY);
				Point2D end = wnd.screenToDatabase(newX, newY);
				double minSelX = Math.min(start.getX(), end.getX());
				double maxSelX = Math.max(start.getX(), end.getX());
				double minSelY = Math.min(start.getY(), end.getY());
				double maxSelY = Math.max(start.getY(), end.getY());
				Highlight.addArea(new Rectangle2D.Double(minSelX, minSelY, maxSelX-minSelX, maxSelY-minSelY), wnd.getCell());
				Highlight.finished();
				wnd.clearDoingAreaDrag();
				wnd.repaint();
			} else if (mode == ToolBar.CursorMode.PAN)
			{
				// panning the window location
				Point2D pt = wnd.getOffset();
				wnd.setOffset(new Point2D.Double(pt.getX() - (newX - lastX) / scale,
					pt.getY() + (newY - lastY) / scale));
				wnd.repaintContents(null);
			}
			lastX = newX;
			lastY = newY;
		}
	}

	public void mouseReleased(MouseEvent evt)
	{
		setProperCursor(evt);
		if (mode != ToolBar.CursorMode.ZOOM) return;

		int newX = evt.getX();
		int newY = evt.getY();
 		if (evt.getSource() instanceof EditWindow)
		{
			EditWindow wnd = (EditWindow)evt.getSource();

			// zooming the window scale
			Highlight.clear();
			Highlight.finished();
			Point2D start = wnd.screenToDatabase(startX, startY);
			Point2D end = wnd.screenToDatabase(newX, newY);
			double minSelX = Math.min(start.getX(), end.getX());
			double maxSelX = Math.max(start.getX(), end.getX());
			double minSelY = Math.min(start.getY(), end.getY());
			double maxSelY = Math.max(start.getY(), end.getY());
			if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) != 0)
			{
				wnd.setScale(wnd.getScale() * 0.5);
				Point2D offset = new Point2D.Double((minSelX+maxSelX)/2, (minSelY+maxSelY)/2);
				wnd.setOffset(offset);
				wnd.repaintContents(null);
				TopLevel.setCurrentCursor(ToolBar.zoomCursor);
			} else
			{
				Rectangle2D bounds = new Rectangle2D.Double(minSelX, minSelY, maxSelX-minSelX, maxSelY-minSelY);
				if (bounds.getWidth() > 0 || bounds.getHeight() > 0)
				{
					wnd.focusScreen(bounds);
				} else
				{
					System.out.println("To zoom-in, drag an area");
				}
			}
		}
	}

	public void mouseWheelMoved(MouseWheelEvent evt)
	{
		int clicks = evt.getWheelRotation();
		System.out.println("Mouse wheel rolled by " + clicks);
	}

	public void keyPressed(KeyEvent evt) {}
	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

    // ----------------------------------- redisplay commands -----------------------------------

	public static void redrawDisplay()
	{
		// get the current window
		EditWindow wnd = EditWindow.getCurrent();
		if (wnd == null) return;
		wnd.repaintContents(null);
	}

    // --------------------------- Pan Commands -------------------------------

	private static double panningAmount = 0.6;

    /**
     * Pan in X direction.
     * @param wf the WindowFrame
     * @param ticks the amount and direction of pan.
     * If ticks is 1, pan right. If ticks is -1, pan left.
     */
    public static void panX(WindowFrame wf, int ticks) {

		if (wf.getContent() instanceof EditWindow)
		{
			Cell cell = wf.getContent().getCell();
			if (cell == null) return;
            EditWindow wnd = (EditWindow)wf.getContent();
            Dimension dim = wnd.getSize();
            int mult = (int)((double)dim.width * panningAmount / wnd.getScale());
	
	        Point2D wndOffset = wnd.getOffset();
	        Point2D newOffset = new Point2D.Double(wndOffset.getX() - mult*ticks, wndOffset.getY());
	        wnd.setOffset(newOffset);
	        wnd.repaintContents(null);
		}
    }

    /**
     * Pan in Y direction.
     * @param wf the WindowFrame
     * @param ticks the amount and direction of pan.
     * If ticks is 1, pan up. If ticks is -1, pan down.
     */
    public static void panY(WindowFrame wf, int ticks) {

		if (wf.getContent() instanceof EditWindow)
		{
	        Cell cell = wf.getContent().getCell();
	        if (cell == null) return;
			EditWindow wnd = (EditWindow)wf.getContent();
			Dimension dim = wnd.getSize();
			int mult = (int)((double)dim.height * panningAmount / wnd.getScale());
	
	        Point2D wndOffset = wnd.getOffset();
	        Point2D newOffset = new Point2D.Double(wndOffset.getX(), wndOffset.getY() - mult*ticks);
	        wnd.setOffset(newOffset);
	        wnd.repaintContents(null);
		}
    }

	/**
	 * This method implements the command to set the panning distance.
	 * @param amount the amount to pan a window.
	 * A value of 1 will pan by an entire window width or height;
	 * smaller values pan by less.
	 */
	public static void panningDistanceCommand(double amount)
	{
		panningAmount = amount;
	}


}
