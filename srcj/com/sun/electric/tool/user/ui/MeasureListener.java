/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MeasureListener.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.user.Highlight;

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

public class MeasureListener
	implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
{
	public static MeasureListener theOne = new MeasureListener();
	private static double lastMeasuredDistanceX = 0, lastMeasuredDistanceY = 0;
	private int startX, startY;

	private MeasureListener() {}

	public static Dimension getLastMeasuredDistance()
	{
		Dimension dim = new Dimension();
		dim.setSize(lastMeasuredDistanceX, lastMeasuredDistanceY);
		return dim;
	}

	public void mousePressed(MouseEvent evt)
	{
		startX = evt.getX();
		startY = evt.getY();
	}

	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mouseMoved(MouseEvent evt) {}
	public void mouseReleased(MouseEvent evt) {}
	public void mouseWheelMoved(MouseWheelEvent evt) {}

	public void mouseDragged(MouseEvent evt)
	{
		int newX = evt.getX();
		int newY = evt.getY();
 		if (evt.getSource() instanceof EditWindow)
		{
			EditWindow wnd = (EditWindow)evt.getSource();

			Highlight.clear();
			Point2D start = wnd.screenToDatabase(startX, startY);
			EditWindow.gridAlign(start);
			Point2D end = wnd.screenToDatabase(newX, newY);
			EditWindow.gridAlign(end);
			Highlight.addLine(start, end, wnd.getCell());
			lastMeasuredDistanceX = Math.abs(start.getX() - end.getX());
			lastMeasuredDistanceY = Math.abs(start.getY() - end.getY());
			Point2D center = new Point2D.Double((start.getX() + end.getX()) / 2, (start.getY() + end.getY()) / 2);
			double dist = start.distance(end);
			Highlight.addMessage(wnd.getCell(), TextUtils.formatDouble(dist), center);
			Highlight.finished();
			wnd.clearDoingAreaDrag();
			wnd.repaint();
		}
	}

	public void keyPressed(KeyEvent evt) {}
	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
}
