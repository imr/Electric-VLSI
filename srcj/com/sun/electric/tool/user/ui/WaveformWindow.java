/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WaveformWindow.java
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

import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.Simulate;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import java.util.EventListener;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This class defines the a screenful of Panels that make up a waveform display.
 */
public class WaveformWindow
{
	/**
	 * Test method to build a waveform with fake data.
	 */
	public static void makeFakeWaveformCommand()
	{
		Color [] colorArray = new Color [] {
			Color.RED, Color.GREEN, Color.BLUE, Color.PINK, Color.ORANGE, Color.YELLOW, Color.CYAN, Color.MAGENTA};
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;

		// make the waveform data
		Simulate.SimData sd = new Simulate.SimData();
		double timeStep = 0.0000000001;
		sd.commonTime = new double[100];
		for(int i=0; i<100; i++)
			sd.commonTime[i] = i * timeStep;
		for(int i=0; i<18; i++)
		{
			Simulate.SimAnalogSignal as = new Simulate.SimAnalogSignal();
			as.signalName = "Signal"+(i+1);
			as.signalColor = colorArray[i % colorArray.length];
			as.useCommonTime = true;
			as.values = new double[100];
			for(int k=0; k<100; k++)
			{
				as.values[k] = Math.sin((k+i*10) / (2.0+i*2)) * 4;
			}
			sd.signals.add(as);
		}
		sd.cell = null;

		// make the waveform window
		WaveformWindow ww = new WaveformWindow(sd, wf);
		ww.setMainTimeCursor(timeStep*22);
		ww.setExtensionTimeCursor(timeStep*77);

		// make some waveform panels and put signals in them
		for(int i=0; i<6; i++)
		{
			Panel wp = new Panel(ww);
			wp.setTimeRange(0, timeStep*100);
			wp.setValueRange(-5, 5);
			for(int j=0; j<(i+1)*3; j++)
			{
				Simulate.SimAnalogSignal as = (Simulate.SimAnalogSignal)sd.signals.get(j);
				Signal wsig = new Signal(wp, as);
			}
		}
	}

	/**
	 * This class defines a single panel of Signals with an associated list of signal names.
	 */
	public static class Panel extends JPanel
		implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
	{
		/** the main waveform window this is part of */			private WaveformWindow waveWindow;
		/** maps signal buttons to the actual Signal */			private HashMap waveSignals;
		/** the list of signal name buttons on the left */		private JPanel signalButtons;
		/** the left side: with signal names etc. */			private JPanel leftHalf;
		/** the right side: with signal traces */				private JPanel rightHalf;
		/** the button to close this panel. */					private JButton close;
		/** the button to overlay a signal. */					private JButton overlaySignal;
		/** the button to delete selected signal. */			private JButton deleteSignal;
		/** the button to delete all signals. */				private JButton deleteAllSignals;
		/** displayed range along horozintal axis */			private double minTime, maxTime;
		/** low vertical axis for this trace (analog) */		private double analogLowValue;
		/** high vertical axis for this trace (analog) */		private double analogHighValue;
		/** vertical range for this trace (analog) */			private double analogRange;
		/** the size of the window (in pixels) */				private Dimension sz;
		/** the cell that is in the window */					private Cell cell;
		/** true if a time cursor is being dragged */			private boolean draggingMain, draggingExt;
		/** true if an area is being dragged */					private boolean draggingArea;
		private double dragStartX, dragStartY;
		private double dragEndX, dragEndY;

		private static final int VERTLABELWIDTH = 60;
		private static Color background = null;

		private static final ImageIcon iconAddSignal = new ImageIcon(WaveformWindow.class.getResource("ButtonSimAdd.gif"));
		private static final ImageIcon iconClosePanel = new ImageIcon(WaveformWindow.class.getResource("ButtonSimClose.gif"));
		private static final ImageIcon iconDeleteSignal = new ImageIcon(WaveformWindow.class.getResource("ButtonSimDelete.gif"));
		private static final ImageIcon iconDeleteAllSignals = new ImageIcon(WaveformWindow.class.getResource("ButtonSimDeleteAll.gif"));

	    // constructor
		public Panel(WaveformWindow waveWindow)
		{
			// remember state
			this.waveWindow = waveWindow;

			// setup this panel window
			sz = new Dimension(500, 150);
			setSize(sz.width, sz.height);
			setPreferredSize(sz);
			setLayout(new FlowLayout());
			// add listeners --> BE SURE to remove listeners in finished()
			addKeyListener(this);
			addMouseListener(this);
			addMouseMotionListener(this);
			addMouseWheelListener(this);
			waveSignals = new HashMap();

			// the left side with signal names
			leftHalf = new JPanel();
			leftHalf.setLayout(new GridBagLayout());

			// a separator at the top
			JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;       gbc.gridy = 0;
			gbc.gridwidth = 4;   gbc.gridheight = 1;
			gbc.weightx = 1;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(4, 0, 4, 0);
			leftHalf.add(sep, gbc);

			// the close button for this panel
			close = new JButton(iconClosePanel);
			close.setBorderPainted(false);
			close.setDefaultCapable(false);
			gbc.gridx = 0;       gbc.gridy = 1;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = new Insets(0, 0, 0, 0);
			leftHalf.add(close, gbc);
			close.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { closePanel(); }
			});

			// the "add signals" button for this panel
			overlaySignal = new JButton(iconAddSignal);
			overlaySignal.setBorderPainted(false);
			overlaySignal.setDefaultCapable(false);
			gbc.gridx = 1;       gbc.gridy = 1;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = new Insets(0, 0, 0, 0);
			leftHalf.add(overlaySignal, gbc);
			overlaySignal.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { overlaySignalInPanel(); }
			});

			// the "delete signal" button for this panel
			deleteSignal = new JButton(iconDeleteSignal);
			deleteSignal.setBorderPainted(false);
			deleteSignal.setDefaultCapable(false);
			gbc.gridx = 2;       gbc.gridy = 1;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = new Insets(0, 0, 0, 0);
			leftHalf.add(deleteSignal, gbc);
			deleteSignal.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { deleteSignalFromPanel(); }
			});

			// the "delete all signal" button for this panel
			deleteAllSignals = new JButton(iconDeleteAllSignals);
			deleteAllSignals.setBorderPainted(false);
			deleteAllSignals.setDefaultCapable(false);
			gbc.gridx = 3;       gbc.gridy = 1;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = new Insets(0, 0, 0, 0);
			leftHalf.add(deleteAllSignals, gbc);
			deleteAllSignals.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { deleteAllSignalsFromPanel(); }
			});

			// the list of signals in this panel
			signalButtons = new JPanel();
			signalButtons.setLayout(new BoxLayout(signalButtons, BoxLayout.Y_AXIS));
			JScrollPane signalList = new JScrollPane(signalButtons);
			signalList.setPreferredSize(new Dimension(100, 150));
			gbc.gridx = 0;       gbc.gridy = 2;
			gbc.gridwidth = 4;   gbc.gridheight = 1;
			gbc.weightx = 1;     gbc.weighty = 1;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.insets = new Insets(0, 0, 0, 0);
			leftHalf.add(signalList, gbc);

			// the right side with signal traces
			rightHalf = new JPanel();
			rightHalf.setLayout(new GridBagLayout());

			// a separator at the top
			sep = new JSeparator(SwingConstants.HORIZONTAL);
			gbc.gridx = 0;       gbc.gridy = 0;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 1;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new java.awt.Insets(4, 0, 4, 0);
			rightHalf.add(sep, gbc);

			// the time tick panel
			TimeTickPanel ttp = new TimeTickPanel(this);
			gbc.gridx = 0;       gbc.gridy = 1;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 1;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(0, 0, 0, 0);
			rightHalf.add(ttp, gbc);

			// the waveform display for this panel
			gbc.gridx = 0;       gbc.gridy = 2;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 1;     gbc.weighty = 1;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.insets = new Insets(0, 0, 0, 0);
			rightHalf.add(this, gbc);

			// put the left and right sides into the window
			waveWindow.left.add(leftHalf);
			waveWindow.right.add(rightHalf);
			waveWindow.wavePanels.add(this);
		}

		public void closePanel()
		{
			waveWindow.closePanel(this);
		}

		private void overlaySignalInPanel()
		{
			waveWindow.overlaySignalInPanel(this);
		}

		private void deleteSignalFromPanel()
		{
			waveWindow.deleteSignalFromPanel(this);
		}

		private void deleteAllSignalsFromPanel()
		{
			waveWindow.deleteAllSignalsFromPanel(this);
		}

		/**
		 * Method to set the time range in this panel.
		 * @param minTime the low time value.
		 * @param maxTime the high time value.
		 */
		public void setTimeRange(double minTime, double maxTime)
		{
			this.minTime = minTime;
			this.maxTime = maxTime;
		}

		/**
		 * Method to set the value range in this panel.
		 * @param low the low value.
		 * @param high the high value.
		 */
		public void setValueRange(double low, double high)
		{
			analogLowValue = low;
			analogHighValue = high;
			analogRange = analogHighValue - analogLowValue;
		}

		/**
		 * Method to get rid of this WaveformWindow.  Called by WindowFrame when
		 * that windowFrame gets closed.
		 */
		public void finished()
		{
			// remove myself from listener list
			removeKeyListener(this);
			removeMouseListener(this);
			removeMouseMotionListener(this);
			removeMouseWheelListener(this);
		}

		/**
		 * Method to repaint this WaveformWindow.
		 */
		public void paint(Graphics g)
		{
			// to enable keys to be received
			requestFocus();

			sz = getSize();

			// show the image
			int wid = sz.width;
			int hei = sz.height;
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, wid, hei);

			// look at all traces in this panel
			for(Iterator it = waveSignals.values().iterator(); it.hasNext(); )
			{
				Signal ws = (Signal)it.next();
				g.setColor(ws.sSig.signalColor);
				if (ws.sSig instanceof Simulate.SimAnalogSignal)
				{
					// draw analog trace
					Simulate.SimAnalogSignal as = (Simulate.SimAnalogSignal)ws.sSig;
					int lx = 0, ly = 0;
					int numEvents = as.values.length;
					for(int i=0; i<numEvents; i++)
					{
						double time = 0;
						if (ws.sSig.useCommonTime) time = waveWindow.sd.commonTime[i]; else
							time = ws.sSig.time[i];
						int x = scaleTimeToX(time);
						int y = scaleValueToY(as.values[i]);
						if (i != 0)
						{
							g.drawLine(lx, ly, x, y);
							if (ws.highlighted)
							{
								int xDelta = 0, yDelta = 1;
								if (Math.abs(x-lx) < Math.abs(y-ly))
								{
									xDelta = 1;   yDelta = 0;
								}
								g.drawLine(lx+xDelta, ly+yDelta, x+xDelta, y+yDelta);
								g.drawLine(lx-xDelta, ly-yDelta, x-xDelta, y-yDelta);
							}
						}
						lx = x;   ly = y;
					}
				}
			}

			// draw the vertical label
			g.setColor(Color.WHITE);
			int yLow = scaleValueToY(analogLowValue);
			int yHigh = scaleValueToY(analogHighValue);
			g.drawLine(VERTLABELWIDTH, yLow, VERTLABELWIDTH, yHigh);
			StepSize ss = getSensibleValues(analogHighValue, analogLowValue, 5);
			if (ss.separation != 0.0)
			{
				double value = ss.low;
				Font font = new Font(User.getDefaultFont(), Font.PLAIN, 12);
				g.setFont(font);
				FontRenderContext frc = new FontRenderContext(null, false, false);
				for(;;)
				{
					if (value > ss.high) break;
					int y = scaleValueToY(value);
					g.drawLine(VERTLABELWIDTH-10, y, VERTLABELWIDTH, y);
					String yValue = prettyPrint(value, ss.rangeScale, ss.stepScale);
					GlyphVector gv = font.createGlyphVector(frc, yValue);
					Rectangle2D glyphBounds = gv.getVisualBounds();
					int height = (int)glyphBounds.getHeight();
					int yPos = y + height / 2;
					if (yPos-height <= 0) yPos = height+1;
					if (yPos >= hei) yPos = hei;
					g.drawString(yValue, VERTLABELWIDTH-10-(int)glyphBounds.getWidth()-2, yPos);
					value += ss.separation;
				}
			}

			// draw the time cursors
			int x = scaleTimeToX(waveWindow.mainTime);
			g.drawLine(x, 0, x, hei);
			g.setColor(Color.YELLOW);
			x = scaleTimeToX(waveWindow.extTime);
			g.drawLine(x, 0, x, hei);
			
			// show dragged area if there
			if (draggingArea)
			{
				g.setColor(Color.WHITE);
				int sX = scaleTimeToX(dragStartX);
				int sY = scaleValueToY(dragStartY);
				int eX = scaleTimeToX(dragEndX);
				int eY = scaleValueToY(dragEndY);
				int lowX = Math.min(sX, eX);
				int highX = Math.max(sX, eX);
				int lowY = Math.min(sY, eY);
				int highY = Math.max(sY, eY);
				g.drawLine(lowX, lowY, lowX, highY);
				g.drawLine(lowX, highY, highX, highY);
				g.drawLine(highX, highY, highX, lowY);
				g.drawLine(highX, lowY, lowX, lowY);
			}
		}

		/**
		 * Method to scale a time value to the X coordinate in this window.
		 * @param time the time value.
		 * @return the X coordinate of that time value.
		 */
		private int scaleTimeToX(double time)
		{
			double x = (time - minTime) / (maxTime - minTime) * (sz.width - VERTLABELWIDTH) + VERTLABELWIDTH;
			return (int)x;
		}

		/**
		 * Method to scale an X coordinate to a time value in this window.
		 * @param x the X coordinate.
		 * @return the time value corresponding to that coordinate.
		 */
		private double scaleXToTime(int x)
		{
			double time = ((double)(x - VERTLABELWIDTH)) / (sz.width - VERTLABELWIDTH) * (maxTime - minTime) + minTime;
			return time;
		}

		/**
		 * Method to scale a value to the Y coordinate in this window.
		 * @param value the value in Y.
		 * @return the Y coordinate of that value.
		 */
		private int scaleValueToY(double value)
		{
			double y = sz.height - 1 - (value - analogLowValue) / analogRange * (sz.height-1);
			return (int)y;
		}

		/**
		 * Method to scale a Y coordinate in this window to a value.
		 * @param y the Y coordinate.
		 * @return the value corresponding to that coordinate.
		 */
		private double scaleYToValue(int y)
		{
			double value = analogLowValue - ((double)(y - sz.height + 1)) / (sz.height-1) * analogRange;
			return value;
		}

		/**
		 * Method to find the Signals in an area.
		 * @param lX the low X coordinate of the area.
		 * @param hX the high X coordinate of the area.
		 * @param lY the low Y coordinate of the area.
		 * @param hY the high Y coordinate of the area.
		 * @return a List of signals in that area.
		 */
		private List findSignalsInArea(int lX, int hX, int lY, int hY)
		{
			Point2D cursor = new Point2D.Double((lX+hX)/2, (lY+hY)/2);
			List foundList = new ArrayList();
			for(Iterator it = waveSignals.values().iterator(); it.hasNext(); )
			{
				Signal ws = (Signal)it.next();
				if (ws.sSig instanceof Simulate.SimAnalogSignal)
				{
					// search analog trace
					Simulate.SimAnalogSignal as = (Simulate.SimAnalogSignal)ws.sSig;
					int lx = 0, ly = 0;
					int numEvents = as.values.length;
					for(int i=0; i<numEvents; i++)
					{
						double time = 0;
						if (ws.sSig.useCommonTime) time = waveWindow.sd.commonTime[i]; else
							time = as.time[i];
						int x = scaleTimeToX(time);
						int y = scaleValueToY(as.values[i]);
						if (i != 0)
						{
							double dist = EMath.distToLine(new Point2D.Double(lx, ly),  new Point2D.Double(x, y), cursor);
							if (dist <= 5)
							{
								foundList.add(ws);
								break;
							}
						}
						lx = x;   ly = y;
					}
				}
			}
			return foundList;
		}

		private void clearHighlightedSignals()
		{
			for(Iterator it = waveSignals.values().iterator(); it.hasNext(); )
			{
				Signal ws = (Signal)it.next();
				if (!ws.highlighted) continue;
				ws.highlighted = false;
				ws.sigButton.setBackground(background);
			}
			repaint();
		}

		private void addHighlightedSignal(Signal ws)
		{
			if (background == null) background = ws.sigButton.getBackground();
			ws.highlighted = true;
			ws.sigButton.setBackground(Color.BLACK);
			repaint();
		}

		private void removeHighlightedSignal(Signal ws)
		{
			ws.highlighted = false;
			ws.sigButton.setBackground(background);
			repaint();
		}

		// the MouseListener events
		public void mousePressed(MouseEvent evt)
		{
			// see if the time cursors are selected
			draggingMain = draggingExt = draggingArea = false;
			int mainX = scaleTimeToX(waveWindow.mainTime);
			if (Math.abs(mainX - evt.getX()) < 5)
			{
				draggingMain = true;
				return;
			}
			int extX = scaleTimeToX(waveWindow.extTime);
			if (Math.abs(extX - evt.getX()) < 5)
			{
				draggingExt = true;
				return;
			}

			// look for a selected signal
			Panel wp = (Panel)evt.getSource();
			List foundList = wp.findSignalsInArea(evt.getX(), evt.getX(), evt.getY(), evt.getY());
			if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0)
			{
				// standard click: add this as the only trace
				clearHighlightedSignals();
				for(Iterator it = foundList.iterator(); it.hasNext(); )
				{
					Signal ws = (Signal)it.next();
					wp.addHighlightedSignal(ws);
				}
			} else
			{
				// shift click: add or remove to list of highlighted traces
				for(Iterator it = foundList.iterator(); it.hasNext(); )
				{
					Signal ws = (Signal)it.next();
					if (ws.highlighted) removeHighlightedSignal(ws); else
						wp.addHighlightedSignal(ws);
				}
			}
			if (foundList.size() == 0)
			{
				// drag area
				draggingArea = true;
				dragStartX = dragEndX = scaleXToTime(evt.getX());
				dragStartY = dragEndY = scaleYToValue(evt.getY());
			}
		}

		public void mouseReleased(MouseEvent evt)
		{
			draggingArea = false;
			repaint();
		}

		public void mouseClicked(MouseEvent evt) {}
		public void mouseEntered(MouseEvent evt) {}
		public void mouseExited(MouseEvent evt) {}

		// the MouseMotionListener events
		public void mouseMoved(MouseEvent evt) {}
		public void mouseDragged(MouseEvent evt)
		{
			if (draggingMain)
			{
				double time = scaleXToTime(evt.getX());
				waveWindow.setMainTimeCursor(time);
				waveWindow.redrawAll();
			} else if (draggingExt)
			{
				double time = scaleXToTime(evt.getX());
				waveWindow.setExtensionTimeCursor(time);
				waveWindow.redrawAll();
			} else if (draggingArea)
			{
				dragEndX = scaleXToTime(evt.getX());
				dragEndY = scaleYToValue(evt.getY());
				repaint();
			}
		}

		// the MouseWheelListener events
		public void mouseWheelMoved(MouseWheelEvent evt) {}

		// the KeyListener events
		public void keyPressed(KeyEvent evt) {}
		public void keyReleased(KeyEvent evt) {}
		public void keyTyped(KeyEvent evt) {}
	}

	// ************************************* TIME GRID ALONG THE TOP OF EACH PANEL *************************************

	/**
	 * This class defines the horizontal time tick display at the top of each Panel.
	 */
	private static class TimeTickPanel extends JPanel
	{
		Panel wavePanel;

		// constructor
		TimeTickPanel(Panel wavePanel)
		{
			// remember state
			this.wavePanel = wavePanel;

			// setup this panel window
			Dimension sz = new Dimension(16, 20);
			this.setMinimumSize(sz);
			setPreferredSize(sz);
		}

		/**
		 * Method to repaint this WaveformWindow.
		 * Composites the image (taken from the PixelDrawing object)
		 * with the grid, highlight, and any dragging rectangle.
		 */
		public void paint(Graphics g)
		{
			Dimension sz = getSize();
			int wid = sz.width;
			int hei = sz.height;
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, wid, hei);

			// draw the time ticks
			g.setColor(Color.WHITE);
			int xLow = wavePanel.scaleTimeToX(wavePanel.minTime);
			int xHigh = wavePanel.scaleTimeToX(wavePanel.maxTime);
			g.drawLine(xLow, hei-1, xHigh, hei-1);
			StepSize ss = getSensibleValues(wavePanel.maxTime, wavePanel.minTime, 10);
			if (ss.separation == 0.0) return;
			double time = ss.low;
			for(;;)
			{
				if (time > ss.high) break;
				int x = wavePanel.scaleTimeToX(time);
				g.drawLine(x, 0, x, hei);
				String timeVal = convertToEngineeringNotation(time, "s", ss.stepScale);
				g.drawString(timeVal, x+2, hei-2);
				time += ss.separation;
			}
		}
	}

	// ************************************* INDIVIDUAL TRACES *************************************

	/**
	 * This class defines a single trace in a Panel.
	 */
	public static class Signal
	{
		/** the panel that holds this signal */			private Panel wavePanel;
		/** the data for this signal */					private Simulate.SimSignal sSig;
		/** true if this signal is highlighted */		private boolean highlighted;
		/** the button on the left with this signal */	private JButton sigButton;
//		CHAR     *origname;			/* original name of this trace */
//		INTBIG    nodeptr;			/* "user" data for this trace */
//		INTBIG    busindex;			/* index of this trace in the bus (when part of a bus) */
//		struct Itrace *buschannel;

		public Signal(Panel wavePanel, Simulate.SimSignal sSig)
		{
			this.wavePanel = wavePanel;
			this.sSig = sSig;
			this.highlighted = false;
			sigButton = new JButton(sSig.signalName);
			sigButton.setBorderPainted(false);
			sigButton.setDefaultCapable(false);
			sigButton.setForeground(sSig.signalColor);
			wavePanel.signalButtons.add(sigButton);
			wavePanel.waveSignals.put(sigButton, this);
			sigButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { signalNameClicked(evt); }
			});
		}

		private void signalNameClicked(ActionEvent evt)
		{
			JButton signal = (JButton)evt.getSource();
			Signal ws = (Signal)wavePanel.waveSignals.get(signal);
			if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0)
			{
				// standard click: add this as the only trace
				ws.wavePanel.clearHighlightedSignals();
				ws.wavePanel.addHighlightedSignal(ws);
			} else
			{
				// shift click: add or remove to list of highlighted traces
				if (ws.highlighted) ws.wavePanel.removeHighlightedSignal(ws); else
					ws.wavePanel.addHighlightedSignal(ws);
			}
		}
	}

	/** the window that this lives in */					private WindowFrame wf;
	/** the cell being simulated */							private Simulate.SimData sd;
	/** let panel: the signal names */						private JPanel left;
	/** right panel: the signal traces */					private JPanel right;
	/** labels for the text at the top */					private JLabel mainPos, extPos, delta;
	/** a list of panels in this window */					private List wavePanels;
	/** current "main" time cursor */						private double mainTime;
	/** current "extension" time cursor */					private double extTime;

	private static final ImageIcon iconAddPanel = new ImageIcon(WaveformWindow.class.getResource("ButtonSimAddPanel.gif"));

    // ************************************* CONTROL *************************************

	public WaveformWindow(Simulate.SimData sd, WindowFrame wf)
	{
		// initialize the structure
		this.wf = wf;
		this.sd = sd;
		wavePanels = new ArrayList();

		// the total panel in the waveform window
		JPanel overall = new JPanel();
		overall.setLayout(new GridBagLayout());

		// the main part of the waveform window: a split-pane between names and traces
		left = new JPanel();
		left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
		right = new JPanel();
		right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
		JScrollPane scroll = new JScrollPane(split);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;       gbc.gridy = 1;
		gbc.gridwidth = 4;   gbc.gridheight = 1;
		gbc.weightx = 1;     gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(scroll, gbc);

		// the top part of the waveform window: status information
		JButton addPanel = new JButton(iconAddPanel);
		addPanel.setBorderPainted(false);
		addPanel.setDefaultCapable(false);
		gbc.gridx = 0;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(addPanel, gbc);
		addPanel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { addSignalInNewPanel(); }
		});
		mainPos = new JLabel("Main:");
		gbc.gridx = 1;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.3;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(mainPos, gbc);
		extPos = new JLabel("Ext:");
		gbc.gridx = 2;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.3;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(extPos, gbc);
		delta = new JLabel("Delta:");
		gbc.gridx = 3;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.3;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(delta, gbc);

		// put this panel into the window
		JPanel panel = wf.getWaveformWindow();
		gbc.gridx = 0;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 1;     gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 0, 0, 0);
		panel.add(overall, gbc);
		wf.setWaveformExplorerData(getSignalsForExplorer());
		wf.setContent(WindowFrame.WAVEFORMWINDOW);
	}

	private DefaultMutableTreeNode getSignalsForExplorer()
	{
		DefaultMutableTreeNode signalsExplorerTree = new DefaultMutableTreeNode("SIGNALS");
		for(Iterator it = sd.signals.iterator(); it.hasNext(); )
		{
			Simulate.SimSignal sSig = (Simulate.SimSignal)it.next();
			signalsExplorerTree.add(new DefaultMutableTreeNode(sSig.signalName));
		}
		return signalsExplorerTree;
	}

	public void setMainTimeCursor(double time)
	{
		mainTime = time;
		String amount = convertToEngineeringNotation(mainTime, "s", 9999);
		mainPos.setText("Main: " + amount);
		String diff = convertToEngineeringNotation(Math.abs(mainTime - extTime), "s", 9999);
		delta.setText("Delta: " + diff);
	}

	public void setExtensionTimeCursor(double time)
	{
		extTime = time;
		String amount = convertToEngineeringNotation(extTime, "s", 9999);
		extPos.setText("Ext: " + amount);
		String diff = convertToEngineeringNotation(Math.abs(mainTime - extTime), "s", 9999);
		delta.setText("Delta: " + diff);
	}

	public void redrawAll()
	{
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.repaint();
		}
	}

	/**
	 * Method to return the cell that is shown in this window.
	 * @return the cell that is shown in this window.
	 */
	public Cell getCell() { return sd.cell; }

	/**
	 * Method to set the window title.
	 */
	public void setWindowTitle()
	{
		if (wf == null) return;
		if (sd.cell == null)
		{
			wf.setTitle("***WAVEFORM WITH NO CELL***");
			return;
		}

		String title = "Waveform for " + sd.cell.describe();
		if (sd.cell.getLibrary() != Library.getCurrent())
			title += " - Current library: " + Library.getCurrent().getLibName();
		wf.setTitle(title);
	}

	static class StepSize
	{
		double separation;
		double low, high;
		int rangeScale;
		int stepScale;
	}

	/**
	 * Method to analyze a range of values and determine sensible displayable values.
	 * @param h the high value in the range.
	 * @param l the low value in the range.
	 * @param n the number of steps in the range.
	 * @return a structure that contains the adjusted values of "l" and "h"
	 * as well as the integers rangeScale and stepScale, which are the
	 * powers of 10 that belong to the largest value in the interval and the step size.
	 */
	private static StepSize getSensibleValues(double h, double l, int n)
	{
		StepSize ss = new StepSize();
		ss.low = l;   ss.high = h;
		ss.rangeScale = ss.stepScale = 0;

		double range = Math.max(Math.abs(l), Math.abs(h));
		if (range == 0.0)
		{
			ss.separation = 0;
			return ss;
		}

		// determine powers of ten in the range
		while ( range >= 10.0 ) { range /= 10.0;   ss.rangeScale++; }
		while ( range <= 1.0  ) { range *= 10.0;   ss.rangeScale--; }

		// determine powers of ten in the step size
		double d = Math.abs(h - l)/(double)n;
		if (Math.abs(d/(h+l)) < 0.0000001) d = 0.1f;
		int mp = 0;
		while ( d >= 10.0 ) { d /= 10.0;   mp++;   ss.stepScale++; }
		while ( d <= 1.0  ) { d *= 10.0;   mp--;   ss.stepScale--; }
		double m = Math.pow(10, mp);

		int di = (int)d;
		if (di > 2 && di <= 5) di = 5; else 
			if (di > 5) di = 10;
		int li = (int)(l / m);
		int hi = (int)(h / m);
		li = (li/di) * di;
		hi = (hi/di) * di;
		if (li < 0) li -= di;
		if (hi > 0) hi += di;
		ss.low = (double)li * m;
		ss.high = (double)hi * m;
		ss.separation = di * m;
		return ss;
	}

	private static String prettyPrint(double v, int i1, int i2)
	{
		double d = 1.0;
		if (i2 > 0)
			for(int i = 0; i < i2; i++) d *= 10.0;
		if (i2 < 0)
			for(int i = 0; i > i2; i--) d /= 10.0;

		if (Math.abs(v)*100.0 < d) return "0";

		if (i1 <= 4 && i1 >= 0 && i2 >= 0)
		{
			String s = TextUtils.formatDouble(v, 1);
			return s;
		}
		if (i1 <= 4 && i1 >= -2 && i2 < 0)
		{
			String s = TextUtils.formatDouble(v, -i2);
			return s;
		}

		int p = i1 - 12 - 1;
		if (p < 0) p = 0;
		String s = TextUtils.formatDouble(v/d, p);
		return s + "e" + i2;
	}

	/*
	 * Method to converts a floating point number into engineering units such as pico, micro, milli, etc.
	 * @param time floating point value to be converted to engineering notation.
	 * @param precpower decimal power of necessary time precision.
	 * Use a very large number to ignore this factor (9999).
	 */
	private static String convertToEngineeringNotation(double time, String unit, int precpower)
	{
		String negative = "";
		if (time < 0.0)
		{
			negative = "-";
			time = -time;
		}
		if (EMath.doublesEqual(time, 0.0)) return "0" + unit;
		if (time < 1.0E-15 || time >= 1000.0) return negative + time + unit;

		// get proper time unit to use
		double scaled = time * 1.0E17;
		long intTime = (long)scaled;
		String secType = null;
		int scalePower = 0;
		if (scaled < 200000.0 && intTime < 100000)
		{
			secType = "f" + unit;
			scalePower = -15;
		} else
		{
			scaled = time * 1.0E14;   intTime = (long)scaled;
			if (scaled < 200000.0 && intTime < 100000)
			{
				secType = "p" + unit;
				scalePower = -12;
			} else
			{
				scaled = time * 1.0E11;   intTime = (long)scaled;
				if (scaled < 200000.0 && intTime < 100000)
				{
					secType = "n" + unit;
					scalePower = -9;
				} else
				{
					scaled = time * 1.0E8;   intTime = (long)scaled;
					if (scaled < 200000.0 && intTime < 100000)
					{
						secType = "u" + unit;
						scalePower = -6;
					} else
					{
						scaled = time * 1.0E5;   intTime = (long)scaled;
						if (scaled < 200000.0 && intTime < 100000)
						{
							secType = "m" + unit;
							scalePower = -3;
						} else
						{
							scaled = time * 1.0E2;  intTime = (long)scaled;
							secType = unit;
							scalePower = 0;
						}
					}
				}
			}
		}
		if (precpower >= scalePower)
		{
			long timeleft = intTime / 100;
			long timeright = intTime % 100;
			if (timeright == 0)
			{
				return negative + timeleft + secType;
			} else
			{
				if ((timeright%10) == 0)
				{
					return negative + timeleft + "." + timeright/10 + secType;
				} else
				{
					return negative + timeleft + "." + timeright + secType;
				}
			}
		}
		scaled /= 1.0E2;
		String numPart = TextUtils.formatDouble(scaled, scalePower - precpower);
		while (numPart.endsWith("0")) numPart = numPart.substring(0, numPart.length()-1);
		if (numPart.endsWith(".")) numPart = numPart.substring(0, numPart.length()-1);
		return negative + numPart + secType;
	}

	/**
	 * Method called when a Panel is to be closed.
	 * @param wp the Panel to close.
	 */
	public void closePanel(Panel wp)
	{
		// cannot delete the last panel
		if (wavePanels.size() <= 1)
		{
			System.out.println("Cannot delete the last waveform panel");
			return;
		}
		left.remove(wp.leftHalf);
		right.remove(wp.rightHalf);
		left.validate();
		right.validate();
		wavePanels.remove(wp);
		redrawAll();
	}

	/**
	 * Method called when a new Panel is to be created.
	 * This is typically overridden by the specific simulation module
	 * which knows the signal names.
	 */
	public void addSignalInNewPanel()
	{
		System.out.println("Cannot add a new signal panel");
	}

	/**
	 * Method called when a signal is to be added to a Panel.
	 * This is typically overridden by the specific simulation module
	 * which knows the signal names.
	 * @param wp the Panel in which to add the signal.
	 */
	public void overlaySignalInPanel(Panel wp)
	{
		System.out.println("Cannot add a signal to this panel");
	}

	/**
	 * Method called to delete the highlighted signal from its Panel.
	 * @param wp the Panel with the signal to be deleted.
	 */
	public void deleteSignalFromPanel(Panel wp)
	{
		boolean found = true;
		while (found)
		{
			found = false;
			for(Iterator it = wp.waveSignals.values().iterator(); it.hasNext(); )
			{
				Signal ws = (Signal)it.next();
				if (!ws.highlighted) continue;
				wp.removeHighlightedSignal(ws);
				wp.signalButtons.remove(ws.sigButton);
				wp.waveSignals.remove(ws.sigButton);
				found = true;
				break;
			}
		}
		wp.signalButtons.validate();
		wp.signalButtons.repaint();
		wp.repaint();
	}

	/**
	 * Method called to delete all signals from a Panel.
	 * @param wp the Panel to clear.
	 */
	public void deleteAllSignalsFromPanel(Panel wp)
	{
		wp.clearHighlightedSignals();
		wp.signalButtons.removeAll();
		wp.signalButtons.validate();
		wp.signalButtons.repaint();
		wp.waveSignals.clear();
		wp.repaint();
	}
}
