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

import java.awt.*;
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
    private static boolean measuring = false;               // true if drawing measure line
    private static Highlight lastMeasure = null;
    private static Highlight lastMeasureLine = null;
	private Point2D dbStart;                 // start of measure in database units

	private MeasureListener() {}

	public static Dimension getLastMeasuredDistance()
	{
		Dimension dim = new Dimension();
		dim.setSize(lastMeasuredDistanceX, lastMeasuredDistanceY);
		return dim;
	}

    private void startMeasure(Point2D dbStart) {
        this.dbStart = dbStart;
        measuring = true;
        lastMeasure = null;
        lastMeasureLine = null;
    }

    private void dragOutMeasure(EditWindow wnd, Point2D dbPoint) {

        if (measuring) {
            //Highlight.clear();
            Point2D start = dbStart;
            Point2D end = dbPoint;
            if (lastMeasureLine != null) Highlight.remove(lastMeasureLine);
            lastMeasureLine = Highlight.addLine(start, end, wnd.getCell());

            lastMeasuredDistanceX = Math.abs(start.getX() - end.getX());
            lastMeasuredDistanceY = Math.abs(start.getY() - end.getY());
            Point2D center = new Point2D.Double((start.getX() + end.getX()) / 2, (start.getY() + end.getY()) / 2);
            double dist = start.distance(end);
            if (lastMeasure != null) Highlight.remove(lastMeasure);
            lastMeasure = Highlight.addMessage(wnd.getCell(), TextUtils.formatDouble(dist), center);

            Highlight.finished();
            wnd.clearDoingAreaDrag();
            wnd.repaint();
        }
    }

    private void finishMeasure(EditWindow wnd) {
        if (measuring)
            measuring = false;
        else {
            // clear measures from the screen if user cancels twice in a row
            Highlight.clear();
            Highlight.finished();
            wnd.repaint();
        }
    }


    // ------------------------ Mouse Listener Stuff -------------------------

    public void mousePressed(MouseEvent evt)
    {
        if (evt.getSource() instanceof EditWindow)
        {
            int newX = evt.getX();
            int newY = evt.getY();
            EditWindow wnd = (EditWindow)evt.getSource();
            Point2D dbMouse = wnd.screenToDatabase(newX, newY);
            EditWindow.gridAlign(dbMouse);
            if (isLeftMouse(evt)) {
                startMeasure(dbMouse);
            }
            if (isRightMouse(evt)) {
                finishMeasure(wnd);
            }
        }
    }

    public void mouseDragged(MouseEvent evt)
    {
        if (evt.getSource() instanceof EditWindow)
        {
            int newX = evt.getX();
            int newY = evt.getY();
            EditWindow wnd = (EditWindow)evt.getSource();
            Point2D dbMouse = wnd.screenToDatabase(newX, newY);
            EditWindow.gridAlign(dbMouse);
            dragOutMeasure(wnd, dbMouse);
        }
    }

    public void mouseMoved(MouseEvent evt) { mouseDragged(evt); }
	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mouseReleased(MouseEvent evt) {}
	public void mouseWheelMoved(MouseWheelEvent evt) {}


	public void keyPressed(KeyEvent evt) {
        int chr = evt.getKeyCode();

        if (evt.getSource() instanceof EditWindow)
        {
            EditWindow wnd = (EditWindow)evt.getSource();

            if (chr == KeyEvent.VK_ESCAPE) {
                finishMeasure(wnd);
            }
        }
    }
	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}


    // mac stuff
    private static final boolean isMac = System.getProperty("os.name").toLowerCase().startsWith("mac");

    /**
     * See if event is a left mouse click.  Platform independent.
     */
    private boolean isLeftMouse(MouseEvent evt) {
        if (isMac) {
            if (!evt.isMetaDown()) {
                if ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK)
                    return true;
            }
        } else {
            if ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK)
                return true;
        }
        return false;
    }

    /**
     * See if event is a right mouse click.  Platform independent.
     * One-button macs: Command + click == right mouse click.
     */
    public static boolean isRightMouse(MouseEvent evt) {
        if (isMac) {
            if (evt.isMetaDown()) {
                if ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK)
                    return true;
            }
            if ((evt.getModifiers() & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK)
                return true;
        } else {
            if ((evt.getModifiers() & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK)
                return true;
        }
        return false;
    }

}
