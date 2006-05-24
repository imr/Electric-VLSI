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

import com.sun.electric.tool.user.Highlighter;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class to Zoom and Pan an EditWindow.
 */
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
        if (mode == ToolBar.CursorMode.ZOOM && (evt.getSource() instanceof EditWindow)) {
            EditWindow wnd = (EditWindow)evt.getSource();
			if (!ClickZoomWireListener.isRightMouse(evt))
			{
	            wnd.setStartDrag(startX, startY);
	            wnd.setEndDrag(startX, startY);
	            wnd.setDoingAreaDrag();
			}
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
			if (wnd == null) return;
			Highlighter highlighter = wnd.getHighlighter();

			double scale = wnd.getScale();
			if (mode == ToolBar.CursorMode.ZOOM)
			{
                wnd.setEndDrag(newX, newY);
                wnd.repaint();

				// if dragging the right mouse, zoom smoothly
				if (ClickZoomWireListener.isRightMouse(evt))
				{
					// use hand motion to zoom; cap to maximum of 20 pixels per tick
					int deltaY = newY - lastY;
//					if (deltaY >= 20) deltaY = 19; else
//						if (deltaY <= -10) deltaY = -19;
					double dY = deltaY / 20.0;
					if (dY < 0) scale = scale - scale * dY;
						else scale = scale * Math.exp(-dY)/*scale - scale * dY*/;
					wnd.setScale(scale);
                    wnd.getSavedFocusBrowser().updateCurrentFocus();
					wnd.repaintContents(null, false);
				}
/*
				// zooming the window scale
				highlighter.clear();
				Point2D start = wnd.screenToDatabase(startX, startY);
				Point2D end = wnd.screenToDatabase(newX, newY);
				double minSelX = Math.min(start.getX(), end.getX());
				double maxSelX = Math.max(start.getX(), end.getX());
				double minSelY = Math.min(start.getY(), end.getY());
				double maxSelY = Math.max(start.getY(), end.getY());
				highlighter.addArea(new Rectangle2D.Double(minSelX, minSelY, maxSelX-minSelX, maxSelY-minSelY), wnd.getCell());
				highlighter.finished();
				wnd.clearDoingAreaDrag();
				wnd.repaint();
*/
			} else if (mode == ToolBar.CursorMode.PAN)
			{
				// panning the window location
				Point2D pt = wnd.getScheduledOffset();
				wnd.setOffset(new Point2D.Double(pt.getX() - (newX - lastX) / scale,
					pt.getY() + (newY - lastY) / scale));
                wnd.getSavedFocusBrowser().updateCurrentFocus();
				wnd.repaintContents(null, false);
			}
			lastX = newX;
			lastY = newY;
		}
	}

	public void mouseReleased(MouseEvent evt)
	{
		setProperCursor(evt);
		if (mode != ToolBar.CursorMode.ZOOM) return;
		if (ClickZoomWireListener.isRightMouse(evt)) return;

		int newX = evt.getX();
		int newY = evt.getY();
 		if (evt.getSource() instanceof EditWindow)
		{
			EditWindow wnd = (EditWindow)evt.getSource();
             if (wnd == null) return;
             Point2D start = wnd.screenToDatabase((int)wnd.getStartDrag().getX(), (int)wnd.getStartDrag().getY());
             Point2D end = wnd.screenToDatabase((int)wnd.getEndDrag().getX(), (int)wnd.getEndDrag().getY());
             double minSelX = Math.min(start.getX(), end.getX());
             double maxSelX = Math.max(start.getX(), end.getX());
             double minSelY = Math.min(start.getY(), end.getY());
             double maxSelY = Math.max(start.getY(), end.getY());
/*
             Highlighter highlighter = wnd.getHighlighter();

			// zooming the window scale
			highlighter.clear();
			highlighter.finished();
			Point2D start = wnd.screenToDatabase(startX, startY);
			Point2D end = wnd.screenToDatabase(newX, newY);
			double minSelX = Math.min(start.getX(), end.getX());
			double maxSelX = Math.max(start.getX(), end.getX());
			double minSelY = Math.min(start.getY(), end.getY());
			double maxSelY = Math.max(start.getY(), end.getY());
*/
			if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) != 0)
			{
				wnd.setScale(wnd.getScale() * 0.5);
				Point2D offset = new Point2D.Double((minSelX+maxSelX)/2, (minSelY+maxSelY)/2);
				if (wnd.isInPlaceEdit())
					wnd.getInPlaceTransformOut().transform(offset, offset);
				wnd.setOffset(offset);
                wnd.getSavedFocusBrowser().updateCurrentFocus();
				wnd.repaintContents(null, false);
				TopLevel.setCurrentCursor(ToolBar.zoomCursor);
			} else
			{
                // determine if the user clicked on a single point to prevent unintended zoom-in
                // a single point is 4 lambda or less AND 10 screen pixels or less
                boolean onePoint = true;
                Rectangle2D bounds = new Rectangle2D.Double(minSelX, minSelY, maxSelX-minSelX, maxSelY-minSelY);
                if (bounds.getHeight() > 4 && bounds.getWidth() > 4) onePoint = false;
                if (Math.abs(wnd.getStartDrag().getX()-wnd.getEndDrag().getX()) > 10 ||
                    Math.abs(wnd.getStartDrag().getY()-wnd.getEndDrag().getY()) > 10) onePoint = false;
				//Rectangle2D bounds = new Rectangle2D.Double(minSelX, minSelY, maxSelX-minSelX, maxSelY-minSelY);
				if (!onePoint)
				{
					wnd.focusScreen(bounds);
				} else
				{
					System.out.println("To zoom-in, drag an area");
				}
			}
             wnd.clearDoingAreaDrag();
             wnd.repaint();             
		}
	}

	public void mouseWheelMoved(MouseWheelEvent evt)
	{
		int clicks = evt.getWheelRotation();
		System.out.println("Mouse wheel rolled by " + clicks);
	}

	public void keyPressed(KeyEvent evt)
	{
        int chr = evt.getKeyCode();
		if (chr == KeyEvent.VK_ESCAPE)
		{
			if (evt.getSource() instanceof EditWindow)
			{
				EditWindow wnd = (EditWindow)evt.getSource();
				wnd.clearDoingAreaDrag();
				wnd.repaint();
				mode = null;
			}
		}
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

    // ----------------------------------- redisplay commands -----------------------------------

	public static void redrawDisplay()
	{
		// get the current window
		EditWindow wnd = EditWindow.getCurrent();
		if (wnd == null) return;
		wnd.repaintContents(null, false);
	}

    // --------------------------- Pan Commands -------------------------------

	private static double [] panningAmounts = {0.15, 0.3, 0.6};

    /**
     * Pan in X or Y direction.
     * @param direction 0 if X or 1 if Y
     * @param wf the WindowFrame
     * @param ticks the amount and direction of pan.
     */
    public static void panXOrY(int direction, WindowFrame wf, int ticks) {

	    wf.getContent().panXOrY(direction, panningAmounts, ticks);
    }

	/**
	 * This method implements the command to center the selected objects.
	 */
	public static void centerSelection()
	{
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();
		Rectangle2D bounds = highlighter.getHighlightedArea(wnd);
		if (bounds == null) return;
		wnd.setOffset(new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()));
        wnd.getSavedFocusBrowser().updateCurrentFocus();
		wnd.repaintContents(null, false);
	}

	/**
	 * This method implements the command to center the cursor.
	 */
	public static void centerCursor()
	{
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
		Point pt = wnd.getLastMousePosition();
		Point2D center = wnd.screenToDatabase(pt.x, pt.y);
		wnd.setOffset(center);
        wnd.getSavedFocusBrowser().updateCurrentFocus();
		wnd.repaintContents(null, false);

	}

}
