/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HorizRuler.java
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
package com.sun.electric.tool.user.waveform;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 * This class defines the horizontal ruler display at the top of each Panel.
 */
public class HorizRuler extends JPanel implements MouseListener
{
	private Panel wavePanel;
	private WaveformWindow waveWindow;

	// constructor
	HorizRuler(Panel wavePanel, WaveformWindow waveWindow)
	{
		// remember state
		this.wavePanel = wavePanel;
		this.waveWindow = waveWindow;

		// setup this panel window
		Dimension sz = new Dimension(16, 20);
		setMinimumSize(sz);
		setPreferredSize(sz);

		addMouseListener(this);

		// a drop target for the ruler panel
		new DropTarget(this, DnDConstants.ACTION_LINK, WaveformWindow.waveformDropTarget, true);
	}

	/**
	 * Method to return the Panel associated with this HorizRuler.
	 * If the ruler is the "main" ruler, associated with all panels
	 * (because time is locked) then this is null.
	 * @return the Panel associated with this HorizRuler.
	 */
	public Panel getPanel() { return wavePanel; }

	/**
	 * Method to return the WaveformWindow in which this ruler lives.
	 * @return the WaveformWindow in which this ruler lives.
	 */
	public WaveformWindow getWaveformWindow() { return waveWindow; }

	/**
	 * Method to repaint this HorizRulerPanel.
	 */
	public void paint(Graphics g)
	{
		renderRuler(g, null, wavePanel);
	}

	/**
	 * Method to get a list of polygons describing this ruler.
	 * @param drawHere the Panel associated with the ruler.
	 * @return a List of PolyBase objects that describe this ruler.
	 */
	public List<PolyBase> getPolysForPrinting(Panel drawHere)
	{
		List<PolyBase> polys = new ArrayList<PolyBase>();
		renderRuler(null, polys, drawHere);
		return polys;
	}

	/**
	 * Method to render a ruler (to the screen or for printing).
	 * @param g the Graphics that will be drawn (null if not rendering to the screen).
	 * @param polys the list of PolyBases to fill (null if not printing).
	 * @param drawHere the Panel associated with the ruler.
	 */
	private void renderRuler(Graphics g, List<PolyBase> polys, Panel drawHere)
	{
		Dimension sz = getSize();
		int wid = sz.width;
		int hei = sz.height;
		int offX = 0;
		Signal xAxisSig = waveWindow.getXAxisSignalAll();
		if (drawHere != null)
		{
			xAxisSig = drawHere.getXAxisSignal();
		}
		if (g != null)
		{
			// this is the main horizontal ruler panel for all panels
			Point screenLoc = getLocationOnScreen();
			offX = waveWindow.getScreenLowX() - screenLoc.x;
			int newWid = waveWindow.getScreenHighX() - waveWindow.getScreenLowX();

			// the main horizontal ruler panel needs a Panel (won't work if there aren't any)
			if (newWid == 0 || waveWindow.getNumPanels() == 0) return;

			if (offX + newWid > wid) newWid = wid - offX;
			wid = newWid;

			if (drawHere == null)
				drawHere = waveWindow.getPanels().next();
			g.setClip(offX, 0, wid, hei);

			// draw the background
			g.setColor(new Color(User.getColorWaveformBackground()));
			g.fillRect(offX, 0, wid, hei);

			// draw the name of the signal on the horizontal ruler axis
			g.setColor(new Color(User.getColorWaveformForeground()));
			g.setFont(waveWindow.getFont());
		}

		String xAxisName = "Time";
		String xAxisPostfix = "s";
		if (xAxisSig != null)
		{
			xAxisName = xAxisSig.getSignalName();
			xAxisPostfix = null;
		}
		if (polys != null)
		{
			Poly poly = new Poly(new Point2D[] {
				new Point2D.Double(drawHere.getVertAxisPos() + offX, hei-1),
				new Point2D.Double(wid+offX, hei-1)});
			polys.add(poly);

			poly = new Poly(new Point2D[] {new Point2D.Double(wid/2, 0)});
			poly.setStyle(Poly.Type.TEXTBOT);
			poly.setTextDescriptor(TextDescriptor.EMPTY.withAbsSize(12));
			poly.setString(xAxisName);
			polys.add(poly);
		} else
		{
			g.drawLine(drawHere.getVertAxisPos() + offX, hei-1, wid+offX, hei-1);
			g.drawString(xAxisName, offX+1, hei-6);
		}

		// draw the ruler ticks
		double displayedLow = drawHere.convertXScreenToData(drawHere.getVertAxisPos());
		double displayedHigh = drawHere.convertXScreenToData(wid);
		StepSize ss = new StepSize(displayedHigh, displayedLow, 10);
		if (ss.getSeparation() == 0.0) return;
		double xValue = ss.getLowValue();
		int lastX = -1;
		for(;;)
		{
			if (xValue > ss.getHighValue()) break;
			if (xValue >= displayedLow)
			{
				int x = drawHere.convertXDataToScreen(xValue) + offX;
				if (polys != null)
				{
					polys.add(new Poly(new Point2D[]{
						new Point2D.Double(x, 0),
						new Point2D.Double(x, hei)}));
				} else
				{
					g.drawLine(x, 0, x, hei);
				}
				if (lastX >= 0)
				{
					if (x - lastX > 100)
					{
						// add 5 tick marks
						for(int i=1; i<5; i++)
						{
							int intX = (x - lastX) / 5 * i + lastX;
							if (polys != null)
							{
								polys.add(new Poly(new Point2D[]{
									new Point2D.Double(intX, hei/2),
									new Point2D.Double(intX, hei)}));
							} else
							{
								g.drawLine(intX, hei/2, intX, hei);
							}
						}
					} else if (x - lastX > 25)
					{
						// add 1 tick mark
						int intX = (x - lastX) / 2 + lastX;
						if (polys != null)
						{
							polys.add(new Poly(new Point2D[]{
								new Point2D.Double(intX, hei/2),
								new Point2D.Double(intX, hei)}));
						} else
						{
							g.drawLine(intX, hei/2, intX, hei);
						}
					}
				}
				String xValueVal = TextUtils.convertToEngineeringNotation(xValue, xAxisPostfix, ss.getStepScale());
				if (polys != null)
				{
					Poly poly = new Poly(new Point2D[]{
						new Point2D.Double(x+2, 4)});
					poly.setStyle(Poly.Type.TEXTLEFT);
					poly.setTextDescriptor(TextDescriptor.EMPTY.withAbsSize(6));
					poly.setString(xValueVal);
					polys.add(poly);
				} else
				{
					g.drawString(xValueVal, x+2, 10);
				}
				lastX = x;
			}
			xValue += ss.getSeparation();
		}
	}

	/**
	 * the MouseListener events for the horizontal ruler panel
	 */
	public void mousePressed(MouseEvent evt)
	{
		if (!ClickZoomWireListener.isRightMouse(evt)) return;
		waveWindow.vcrClickStop();

		// right click in horizontal ruler area: show popup of choices
		JPopupMenu menu = new JPopupMenu();
		JMenuItem item = new JMenuItem("Linear");
		item.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeLinear(); } });
		menu.add(item);
		item = new JMenuItem("Logarithmic");
		item.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeLogarithmic(); } });
		menu.add(item);
		menu.addSeparator();
		item = new JMenuItem("Make the X axis show Time");
		item.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { restoreTime(); } });
		menu.add(item);

		menu.show(this, evt.getX(), evt.getY());
	}

	public void mouseReleased(MouseEvent evt) {}
	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}

	/**
	 * Make this panel show a linear X axis.
	 */
	private void makeLinear()
	{
		if (waveWindow.isXAxisLocked())
		{
			waveWindow.setWaveWindowLogarithmic(false);
			waveWindow.redrawAllPanels();
		} else
		{
			wavePanel.setPanelLogarithmicHorizontally(false);
			wavePanel.repaintContents();
		}
	}

	/**
	 * Make this panel show a logarithmic X axis.
	 */
	private void makeLogarithmic()
	{
		if (waveWindow.isXAxisLocked())
		{
			waveWindow.setWaveWindowLogarithmic(true);
			waveWindow.redrawAllPanels();
		} else
		{
			wavePanel.setPanelLogarithmicHorizontally(true);
			wavePanel.repaintContents();
		}
	}

	/**
	 * Make this panel show a time in the X axis.
	 */
	private void restoreTime()
	{
		Rectangle2D dataBounds = waveWindow.getSimData().getBounds();
		double lowXValue = dataBounds.getMinX();
		double highXValue = dataBounds.getMaxX();

		boolean notWarned = true;
		for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
		{
			Panel wp = it.next();
			if (!waveWindow.isXAxisLocked() && wp != wavePanel) continue;
			if (wp.getAnalysisType() == Analysis.ANALYSIS_MEAS)
			{
				if (wp.getNumSignals() > 0)
				{
					if (notWarned)
					{
						notWarned = true;
						int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
							"Remove all measurement traces in these panels?");
						if (response != JOptionPane.YES_OPTION) return;
					}
					waveWindow.deleteAllSignalsFromPanel(wp);
				}
				Analysis.AnalysisType analysisType = Analysis.ANALYSIS_SIGNALS;
				if (waveWindow.getSimData().getNumAnalyses() > 0)
					analysisType = waveWindow.getSimData().getAnalyses().next().getAnalysisType();
				wp.setAnalysisType(analysisType);
			}
			wp.setXAxisSignal(null);
			wp.setXAxisRange(lowXValue, highXValue);
			if (wp.getHorizRuler() != null) wp.getHorizRuler().repaint();
			wp.repaintContents();
		}
		if (waveWindow.isXAxisLocked())
		{
			waveWindow.setXAxisSignalAll(null);
			waveWindow.getMainHorizRuler().repaint();
			waveWindow.redrawAllPanels();
		}
		waveWindow.saveSignalOrder();
	}
}
