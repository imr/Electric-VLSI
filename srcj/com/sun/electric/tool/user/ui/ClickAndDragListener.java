/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ClickAndDragListener.java
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
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.menus.MenuCommands;
import com.sun.electric.tool.user.menus.EditMenu;

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

class ClickAndDragListener
	implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
{
	public static ClickAndDragListener theOne = new ClickAndDragListener();
	private int oldx, oldy;
	private boolean doingMotionDrag, invertSelection;

	private ClickAndDragListener() {}

	public void mousePressed(MouseEvent evt)
	{
		oldx = evt.getX();
		oldy = evt.getY();
		EditWindow wnd = (EditWindow)evt.getSource();
		Cell cell = wnd.getCell();
        if (cell == null) return;

		boolean another = (evt.getModifiers()&MouseEvent.CTRL_MASK) != 0;
		invertSelection = (evt.getModifiers()&MouseEvent.SHIFT_MASK) != 0;
		boolean special = ToolBar.getSelectSpecial();

		// show "get info" on double-click
		if (evt.getClickCount() == 2 && !another && !invertSelection)
		{
			if (Highlight.getNumHighlights() >= 1)
			{
				EditMenu.getInfoCommand();
				return;
			}
		}

		// selection: if over selected things, move them
		doingMotionDrag = false;
		if (ToolBar.getSelectMode() == ToolBar.SelectMode.OBJECTS)
		{
			// object selection: start by seeing if cursor is over a highlighted object
			if (!another && !invertSelection && Highlight.overHighlighted(wnd, oldx, oldy))
			{
				// over a highlighted object: move it
				doingMotionDrag = true;
				return;
			}

			// standard selection: see if cursor is over anything
			Point2D pt = wnd.screenToDatabase(oldx, oldy);
			int numFound = Highlight.findObject(pt, wnd, false, another, invertSelection, true, false, special, true);
			if (numFound == 0)
			{
				// not over anything: drag out a selection rectangle
				wnd.setStartDrag(oldx, oldy);
				wnd.setEndDrag(oldx, oldy);
				wnd.setDoingAreaDrag();
			}
		} else
		{
			// area selection: just drag out a rectangle
			wnd.setStartDrag(oldx, oldy);
			wnd.setEndDrag(oldx, oldy);
			wnd.setDoingAreaDrag();
		}
		wnd.repaint();
	}

	public void mouseReleased(MouseEvent evt)
	{
		EditWindow wnd = (EditWindow)evt.getSource();
		Cell cell = wnd.getCell();
        if (cell == null) return;

		// handle dragging out a selection rectangle
		if (wnd.isDoingAreaDrag())
		{
			Point2D start = wnd.screenToDatabase((int)wnd.getStartDrag().getX(), (int)wnd.getStartDrag().getY());
			Point2D end = wnd.screenToDatabase((int)wnd.getEndDrag().getX(), (int)wnd.getEndDrag().getY());
			double minSelX = Math.min(start.getX(), end.getX());
			double maxSelX = Math.max(start.getX(), end.getX());
			double minSelY = Math.min(start.getY(), end.getY());
			double maxSelY = Math.max(start.getY(), end.getY());
			if (!invertSelection)
				Highlight.clear();
			if (ToolBar.getSelectMode() == ToolBar.SelectMode.OBJECTS)
			{
				Highlight.selectArea(wnd, minSelX, maxSelX, minSelY, maxSelY, invertSelection,
					ToolBar.getSelectSpecial());
			} else
			{
				Highlight.addArea(new Rectangle2D.Double(minSelX, minSelY, maxSelX-minSelX, maxSelY-minSelY), cell);
			}
			Highlight.finished();
			wnd.clearDoingAreaDrag();
			wnd.repaint();
		}

		// handle moving the selected objects
		if (doingMotionDrag)
		{
			doingMotionDrag = false;
			int newX = evt.getX();
			int newY = evt.getY();
			Point2D delta = wnd.deltaScreenToDatabase(newX - oldx, newY - oldy);
			EditWindow.gridAlign(delta);
			if (delta.getX() == 0 && delta.getY() == 0) return;
			Highlight.setHighlightOffset(0, 0);
			CircuitChanges.manyMove(delta.getX(), delta.getY());
			wnd.repaint();
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
		EditWindow.gridAlign(delta);
		Point pt = wnd.deltaDatabaseToScreen(delta.getX(), delta.getY());
		newX = oldx + pt.x;   newY = oldy + pt.y;

		Cell cell = wnd.getCell();
        if (cell == null) return;

		// handle moving the selected objects
		if (doingMotionDrag)
		{
			Highlight.setHighlightOffset(newX - oldx, newY - oldy);
			wnd.repaint();
			return;
		}

		// handle dragging out a selection rectangle
		if (wnd.isDoingAreaDrag())
		{
			wnd.setEndDrag(newX, newY);
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
			CircuitChanges.deleteSelected();
		} else if (chr == KeyEvent.VK_LEFT)
		{
			moveSelected(evt, -1, 0);
		} else if (chr == KeyEvent.VK_RIGHT)
		{
			moveSelected(evt, 1, 0);
		} else if (chr == KeyEvent.VK_UP)
		{
			moveSelected(evt, 0, 1);
		} else if (chr == KeyEvent.VK_DOWN)
		{
			moveSelected(evt, 0, -1);
		}

		// "A" for abort
		if (chr == KeyEvent.VK_A && doingMotionDrag)
		{
			// restore the listener to the former state
			doingMotionDrag = false;
			Highlight.setHighlightOffset(0, 0);
			wnd.repaint();
			System.out.println("Aborted");
		}
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

	private void moveSelected(KeyEvent evt, double dX, double dY)
	{
        // scale distance according to arrow motion
		EditWindow wnd = (EditWindow)evt.getSource();
		double arrowDistance = ToolBar.getArrowDistance();
		dX *= arrowDistance;
		dY *= arrowDistance;
		int scale = User.getDefGridXBoldFrequency();
		if (evt.isShiftDown()) { dX *= scale;   dY *= scale; }
		if (evt.isControlDown()) { dX *= scale;   dY *= scale; }
		Highlight.setHighlightOffset(0, 0);
		CircuitChanges.manyMove(dX, dY);
		wnd.repaintContents(null);
	}

}
