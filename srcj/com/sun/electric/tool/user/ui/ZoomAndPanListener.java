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
 		if (evt.getSource() instanceof EditWindow.CircuitPart)
		{
			EditWindow.CircuitPart dispPart = (EditWindow.CircuitPart)evt.getSource();
			EditWindow wnd = dispPart.getEditWindow();

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
				wnd.repaintContents();
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
 		if (evt.getSource() instanceof EditWindow.CircuitPart)
		{
			EditWindow.CircuitPart dispPart = (EditWindow.CircuitPart)evt.getSource();
			EditWindow wnd = dispPart.getEditWindow();

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
				wnd.repaintContents();
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
		wnd.repaintContents();
	}

    // --------------------------- Pan Commands -------------------------------

    /**
     * Pan in X direction.  if ticks is positive, pan right. If ticks is
     * negative, pan left. A ticks value of 1 is a short pan (10% of screen).
     * Make ticks a higher value to pan more.
     * @param wf the WindowFrame
     * @param ticks the amount and direction of pan
     */
    public static void panX(WindowFrame wf, int ticks) {

		if (wf.getContent() instanceof EditWindow)
		{
			Cell cell = wf.getContent().getCell();
			if (cell == null) return;
            EditWindow wnd = (EditWindow)wf.getContent();

	        // heuristic: factor in multiplier
	        int mult = (int)(80/wnd.getScale());
	        if (mult <= 0) mult = 2;
	        if (wnd.getScale() > 70) mult = 1; // we're really zoomed in
	
	        Point2D wndOffset = wnd.getOffset();
	        Point2D newOffset = new Point2D.Double(wndOffset.getX() - mult*ticks, wndOffset.getY());
	        wnd.setOffset(newOffset);
	        wnd.repaintContents();
		}
    }

    /**
     * Pan in Y direction.  if ticks is positive, pan up. If ticks is
     * negative, pan down. A ticks value of 1 is a short pan (10% of screen).
     * Make ticks a higher value to pan more.
     * @param wf the WindowFrame
     * @param ticks the amount and direction of pan
     */
    public static void panY(WindowFrame wf, int ticks) {

		if (wf.getContent() instanceof EditWindow)
		{
	        Cell cell = wf.getContent().getCell();
	        if (cell == null) return;
			EditWindow wnd = (EditWindow)wf.getContent();

	        // heuristic: factor in multiplier
	        int mult = (int)(80/wnd.getScale());
	        if (mult <= 0) mult = 2;
	        if (wnd.getScale() > 70) mult = 1; // we're really zoomed in
	
	        Point2D wndOffset = wnd.getOffset();
	        Point2D newOffset = new Point2D.Double(wndOffset.getX(), wndOffset.getY() - mult*ticks);
	        wnd.setOffset(newOffset);
	        wnd.repaintContents();
		}
    }


}
