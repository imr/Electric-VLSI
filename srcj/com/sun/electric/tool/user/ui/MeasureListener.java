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
 * the Free Software Foundation; either version 3 of the License, or
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

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Client;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.waveform.Panel;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;

/**
 * Class to make measurements in a window.
 */
public class MeasureListener implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
{
	public static MeasureListener theOne = new MeasureListener();

	private static double lastMeasuredDistanceX = 0, lastMeasuredDistanceY = 0;
	private static double lastValidMeasuredDistanceX = 0, lastValidMeasuredDistanceY = 0;
	private static boolean measuring = false; // true if drawing measure line
	private static List<Highlight> lastHighlights = new ArrayList<Highlight>();
	private Point2D dbStart; // start of measure in database units
	private static final boolean CADENCEMODE = true;

	private MeasureListener() {}

	public static Dimension2D getLastMeasuredDistance()
	{
		Dimension2D dim = new Dimension2D.Double(lastValidMeasuredDistanceX,
			lastValidMeasuredDistanceY);
		return dim;
	}

	private void startMeasure(Point2D dbStart)
	{
		lastValidMeasuredDistanceX = lastMeasuredDistanceX;
		lastValidMeasuredDistanceY = lastMeasuredDistanceY;
		this.dbStart = dbStart;
		measuring = true;
		lastHighlights.clear();
	}

	private void dragOutMeasure(EditWindow wnd, Point2D dbPoint)
	{
		if (measuring && dbStart != null)
		{
			// Highlight.clear();
			Point2D start = dbStart;
			Point2D end = dbPoint;
			Highlighter highlighter = wnd.getRulerHighlighter();

			for (Highlight h : lastHighlights)
			{
				highlighter.remove(h);
			}
			lastHighlights.clear();

			// show coords at start and end point
            Cell cell = wnd.getCell();
            if (cell == null) return; // nothing available

            Technology tech = cell.getTechnology();
            if (CADENCEMODE)
            {
    			Point stScreen = wnd.databaseToScreen(start);
    			Point enScreen = wnd.databaseToScreen(end);
    			double dbDist = start.distance(end);
    			double screenDist = Math.sqrt((stScreen.x-enScreen.x)*(stScreen.x-enScreen.x) +
    				(stScreen.y-enScreen.y)*(stScreen.y-enScreen.y));
    			if (dbDist > 0 && screenDist > 0)
    			{
	    			double dbLargeTickSize = 8 / screenDist * dbDist;
	    			double dbSmallTickSize = 3 / screenDist * dbDist;
	    			int lineAngle = DBMath.figureAngle(start, end);
	    			int tickAngle = (lineAngle + 900) % 3600;

	    			// draw main line and starting/ending tick marks
	    			lastHighlights.add(highlighter.addLine(start, end, cell));
	    			double tickX = start.getX() + DBMath.cos(tickAngle) * dbLargeTickSize;
	    			double tickY = start.getY() + DBMath.sin(tickAngle) * dbLargeTickSize;
	    			Point2D tickLocation = new Point2D.Double(tickX, tickY);
	    			lastHighlights.add(highlighter.addLine(start, tickLocation, cell));
					lastHighlights.add(highlighter.addMessage(cell, "0", tickLocation));
	    			tickX = end.getX() + DBMath.cos(tickAngle) * dbLargeTickSize;
	    			tickY = end.getY() + DBMath.sin(tickAngle) * dbLargeTickSize;
	    			tickLocation = new Point2D.Double(tickX, tickY);
	    			lastHighlights.add(highlighter.addLine(end, tickLocation, cell));
					lastHighlights.add(highlighter.addMessage(cell, TextUtils.formatDistance(dbDist, tech), tickLocation));

					// now do intermediate ticks
					double minorTickDist = Math.min(wnd.getGridXSpacing(), wnd.getGridYSpacing());
					int numMinorTicks = (int)(dbDist / minorTickDist);
					while (numMinorTicks >= screenDist/10)
					{
						minorTickDist *= 2;
						numMinorTicks = (int)(dbDist / minorTickDist);
					}
					for(int i=1; i<=numMinorTicks; i++)
					{
						double distSoFar = minorTickDist * i;
						double minorTickX = start.getX() + distSoFar*DBMath.cos(lineAngle);
						double minorTickY = start.getY() + distSoFar*DBMath.sin(lineAngle);
		    			tickX = minorTickX + DBMath.cos(tickAngle) * dbSmallTickSize;
		    			tickY = minorTickY + DBMath.sin(tickAngle) * dbSmallTickSize;
		    			tickLocation = new Point2D.Double(tickX, tickY);
		    			lastHighlights.add(highlighter.addLine(new Point2D.Double(minorTickX, minorTickY),
		    				tickLocation, cell));
						if (distSoFar > dbDist-minorTickDist) break;
		    			if ((i%5) == 0)
		    			{
							lastHighlights.add(highlighter.addMessage(cell, TextUtils.formatDistance(distSoFar, tech), tickLocation));
		    			}
					}
    			}
            } else
            {
				lastHighlights.add(highlighter.addMessage(cell, "("
					+ TextUtils.formatDistance(start.getX(), tech) + "," + TextUtils.formatDistance(start.getY(), tech)
					+ ")", start));
				lastHighlights.add(highlighter.addMessage(cell, "("
					+ TextUtils.formatDistance(end.getX(), tech) + "," + TextUtils.formatDistance(end.getY(), tech)
					+ ")", end));
				// add in line
				lastHighlights.add(highlighter.addLine(start, end, cell));

				lastMeasuredDistanceX = Math.abs(start.getX() - end.getX());
				lastMeasuredDistanceY = Math.abs(start.getY() - end.getY());
				Point2D center = new Point2D.Double((start.getX() + end.getX()) / 2,
					(start.getY() + end.getY()) / 2);
				double dist = start.distance(end);
				String show = TextUtils.formatDistance(dist, tech) + " (dX="
					+ TextUtils.formatDistance(lastMeasuredDistanceX, tech) + " dY="
					+ TextUtils.formatDistance(lastMeasuredDistanceY, tech) + ")";
				lastHighlights.add(highlighter.addMessage(cell, show, center, 1));
            }
			highlighter.finished();
			wnd.clearDoingAreaDrag();
			wnd.repaint();
		}
	}

	public void reset()
    {
        if (measuring) measuring = false;

        // clear measurements in the current window
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf.getContent() instanceof EditWindow)
        {
            EditWindow wnd = (EditWindow)wf.getContent();
	        Highlighter highlighter = wnd.getRulerHighlighter();
	        highlighter.clear();
	        highlighter.finished();
	        wnd.repaint();
        } else if (wf.getContent() instanceof WaveformWindow)
        {
        	WaveformWindow ww = (WaveformWindow)wf.getContent();
        	for(Iterator<Panel> it = ww.getPanels(); it.hasNext(); )
        	{
        		Panel p = it.next();
        		p.clearMeasurements();
        	}
        }
    }

	private void finishMeasure(EditWindow wnd)
	{
		Highlighter highlighter = wnd.getRulerHighlighter();
		if (measuring)
		{
			for (Highlight h : lastHighlights)
			{
				highlighter.remove(h);
			}
			lastHighlights.clear();
			measuring = false;
		} else
		{
			// clear measures from the screen if user cancels twice in a row
			highlighter.clear();
		}
		highlighter.finished();
		wnd.repaint();
	}

	// ------------------------ Mouse Listener Stuff -------------------------

	public void mousePressed(MouseEvent evt)
	{
		boolean ctrl = (evt.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0;

		if (evt.getSource() instanceof EditWindow)
		{
			int newX = evt.getX();
			int newY = evt.getY();
			EditWindow wnd = (EditWindow)evt.getSource();
			Point2D dbMouse = wnd.screenToDatabase(newX, newY);
			EditWindow.gridAlign(dbMouse);
			if (isLeftMouse(evt))
			{
				if (measuring && ctrl && dbStart != null)
				{
					// orthogonal only
					dbMouse = convertToOrthogonal(dbStart, dbMouse);
				}
				startMeasure(dbMouse);
			}
			if (ClickZoomWireListener.isRightMouse(evt))
			{
				finishMeasure(wnd);
			}
		}
	}

	public void mouseReleased(MouseEvent evt)
	{
		// uncomment this to do measurements by click, drag, release instead of click/click
//		lastValidMeasuredDistanceX = lastMeasuredDistanceX;
//		lastValidMeasuredDistanceY = lastMeasuredDistanceY;
//		measuring = false;
//		lastHighlights.clear();
	}

	public void mouseDragged(MouseEvent evt)
	{
		gridMouse(evt);
		boolean ctrl = (evt.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0;

		if (evt.getSource() instanceof EditWindow)
		{
			int newX = evt.getX();
			int newY = evt.getY();
			EditWindow wnd = (EditWindow)evt.getSource();
			Point2D dbMouse = wnd.screenToDatabase(newX, newY);
			if (ctrl && dbStart != null)
			{
				dbMouse = convertToOrthogonal(dbStart, dbMouse);
			}
			EditWindow.gridAlign(dbMouse);
			dragOutMeasure(wnd, dbMouse);
		}
	}

	private int gridOffX = 0, gridOffY = 0;
	private Robot robot = null;

	public void mouseMoved(MouseEvent evt)
	{
		mouseDragged(evt);
	}
	
	private void gridMouse(MouseEvent evt)
	{
		// snap the cursor to the grid
		if (User.isGridAlignMeasurementCursor() && evt.getSource() instanceof EditWindow)
		{
			EditWindow wnd = (EditWindow)evt.getSource();
	        int mouseX = evt.getX() + gridOffX;
	        int mouseY = evt.getY() + gridOffY;
	        Point2D dbMouse = wnd.screenToDatabase(mouseX, mouseY);
	        Point2D align = new Point2D.Double(dbMouse.getX(), dbMouse.getY());
			EditWindow.gridAlign(align);
			Point newPos = wnd.databaseToScreen(align);
			gridOffX = mouseX - newPos.x;
			gridOffY = mouseY - newPos.y;
			try {
				if (robot == null) robot = new Robot();
			} catch (AWTException e) {}
			SwingUtilities.convertPointToScreen(newPos, wnd); 
			if (robot != null) robot.mouseMove(newPos.x, newPos.y);
		}
	}

	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mouseWheelMoved(MouseWheelEvent evt) {}
	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

	public void keyPressed(KeyEvent evt)
	{
		int chr = evt.getKeyCode();

		if (evt.getSource() instanceof EditWindow)
		{
			EditWindow wnd = (EditWindow)evt.getSource();

			if (chr == KeyEvent.VK_ESCAPE)
			{
				finishMeasure(wnd);
			}
		}
	}

	/**
	 * See if event is a left mouse click. Platform independent.
	 */
	private boolean isLeftMouse(MouseEvent evt)
	{
		if (Client.isOSMac())
		{
			if (!evt.isMetaDown())
			{
				if ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK) return true;
			}
		} else
		{
			if ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK) return true;
		}
		return false;
	}

	/**
	 * Convert the mousePoint to be orthogonal to the startPoint. Chooses
	 * direction which is orthogonally farther from startPoint
	 * @param startPoint the reference point
	 * @param mousePoint the mouse point
	 * @return a new point orthogonal to startPoint
	 */
	public static Point2D convertToOrthogonal(Point2D startPoint, Point2D mousePoint)
	{
		// move in direction that is farther
		double xdist, ydist;
		xdist = Math.abs(mousePoint.getX() - startPoint.getX());
		ydist = Math.abs(mousePoint.getY() - startPoint.getY());
		if (ydist > xdist)
			return new Point2D.Double(startPoint.getX(), mousePoint.getY());
		return new Point2D.Double(mousePoint.getX(), startPoint.getY());
	}

}
