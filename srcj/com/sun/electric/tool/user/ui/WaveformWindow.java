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
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.Simulate;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.HighlightListener;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This class defines the a screenful of Panels that make up a waveform display.
 */
public class WaveformWindow implements WindowContent, HighlightListener
{
	private static int panelSizeDigital = 30;
	private static int panelSizeAnalog = 150;

	/**
	 * Test method to build a waveform with fake data.
	 */
	public static void makeFakeWaveformCommand()
	{
		Color [] colorArray = new Color [] {
			Color.RED, Color.GREEN, Color.BLUE, Color.PINK, Color.ORANGE, Color.YELLOW, Color.CYAN, Color.MAGENTA};

		// make the waveform data
		Simulate.SimData sd = new Simulate.SimData();
		double timeStep = 0.0000000001;
		sd.buildCommonTime(100);
		for(int i=0; i<100; i++)
			sd.setCommonTime(i, i * timeStep);
		for(int i=0; i<18; i++)
		{
			Simulate.SimAnalogSignal as = new Simulate.SimAnalogSignal(sd);
			as.setSignalName("Signal"+(i+1));
			as.setSignalColor(colorArray[i % colorArray.length]);
			as.setCommonTimeUse(true);
			as.buildValues(100);
			for(int k=0; k<100; k++)
			{
				as.setValue(k, Math.sin((k+i*10) / (2.0+i*2)) * 4);
			}
		}
		sd.setCell(null);

		// make the waveform window
		WindowFrame wf = WindowFrame.createWaveformWindow(sd);
		WaveformWindow ww = (WaveformWindow)wf.getContent();
		ww.setMainTimeCursor(timeStep*22);
		ww.setExtensionTimeCursor(timeStep*77);
		ww.setDefaultTimeRange(0, timeStep*100);

		// make some waveform panels and put signals in them
		for(int i=0; i<6; i++)
		{
			Panel wp = new Panel(ww, true);
			wp.setValueRange(-5, 5);
			for(int j=0; j<(i+1)*3; j++)
			{
				Simulate.SimAnalogSignal as = (Simulate.SimAnalogSignal)sd.getSignals().get(j);
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
		/** the JScrollPane with of signal name buttons */		private JScrollPane signalButtonsPane;
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
//		/** the cell that is in the window */					private Cell cell;
		/** true if a time cursor is being dragged */			private boolean draggingMain, draggingExt;
		/** true if an area is being dragged */					private boolean draggingArea;
		/** true if this waveform panel is selected */			private boolean selected;
		/** true if this waveform panel is analog */			private boolean isAnalog;
		/** the time panel at the top of this panel. */			private TimeTickPanel timePanel;

		private int dragStartX, dragStartY;
		private int dragEndX, dragEndY;

		private static final int VERTLABELWIDTH = 60;
		private static Color background = null;

		private static final ImageIcon iconAddSignal = new ImageIcon(WaveformWindow.class.getResource("ButtonSimAdd.gif"));
		private static final ImageIcon iconClosePanel = new ImageIcon(WaveformWindow.class.getResource("ButtonSimClose.gif"));
		private static final ImageIcon iconDeleteSignal = new ImageIcon(WaveformWindow.class.getResource("ButtonSimDelete.gif"));
		private static final ImageIcon iconDeleteAllSignals = new ImageIcon(WaveformWindow.class.getResource("ButtonSimDeleteAll.gif"));

	    // constructor
		public Panel(WaveformWindow waveWindow, boolean isAnalog)
		{
			// remember state
			this.waveWindow = waveWindow;
			this.isAnalog = isAnalog;
			this.selected = false;

			// setup this panel window
			int height = panelSizeDigital;
			if (isAnalog) height = panelSizeAnalog;
			sz = new Dimension(500, height);
			setSize(sz.width, sz.height);
			setPreferredSize(sz);
			setLayout(new FlowLayout());
			// add listeners --> BE SURE to remove listeners in finished()
			addKeyListener(this);
			addMouseListener(this);
			addMouseMotionListener(this);
			addMouseWheelListener(this);
			waveSignals = new HashMap();

			setTimeRange(waveWindow.minTime, waveWindow.maxTime);

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
			gbc.weightx = 0.25;  gbc.weighty = 0;
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
			gbc.weightx = 0.25;  gbc.weighty = 0;
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
			gbc.weightx = 0.25;  gbc.weighty = 0;
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
			gbc.weightx = 0.25;  gbc.weighty = 0;
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
			signalButtonsPane = new JScrollPane(signalButtons);
			signalButtonsPane.setPreferredSize(new Dimension(100, height));
			gbc.gridx = 0;       gbc.gridy = 2;
			gbc.gridwidth = 4;   gbc.gridheight = 1;
			gbc.weightx = 1;     gbc.weighty = 1;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.insets = new Insets(0, 0, 0, 0);
			leftHalf.add(signalButtonsPane, gbc);

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
			timePanel = new TimeTickPanel(this);
			gbc.gridx = 0;       gbc.gridy = 1;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 1;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(0, 0, 0, 0);
			rightHalf.add(timePanel, gbc);

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

		private Font waveWindowFont;
		private FontRenderContext waveWindowFRC = new FontRenderContext(null, false, false);

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
			waveWindowFont = new Font(User.getDefaultFont(), Font.PLAIN, 12);

			// look at all traces in this panel
			for(Iterator it = waveSignals.values().iterator(); it.hasNext(); )
			{
				Signal ws = (Signal)it.next();
				g.setColor(ws.sSig.getSignalColor());
				if (ws.sSig instanceof Simulate.SimAnalogSignal)
				{
					// draw analog trace
					Simulate.SimAnalogSignal as = (Simulate.SimAnalogSignal)ws.sSig;
					int lx = 0, ly = 0;
					int numEvents = as.getNumEvents();
					for(int i=0; i<numEvents; i++)
					{
						double time = ws.sSig.getTime(i);
						int x = scaleTimeToX(time);
						int y = scaleValueToY(as.getValue(i));
						if (i != 0)
						{
							drawALine(g, lx, ly, x, y, ws.highlighted);
						}
						lx = x;   ly = y;
					}
					continue;
				}
				if (ws.sSig instanceof Simulate.SimDigitalSignal)
				{
					// draw digital traces
					Simulate.SimDigitalSignal ds = (Simulate.SimDigitalSignal)ws.sSig;
					List bussedSignals = ds.getBussedSignals();
					if (bussedSignals != null)
					{
						// a digital bus trace
						int busWidth = bussedSignals.size();
						long curValue = 0;
						double curTime = 0;
						int lastX = VERTLABELWIDTH;
						for(;;)
						{
							double nextTime = Double.MAX_VALUE;
							int bit = 0;
							boolean curDefined = true;
							for(Iterator bIt = bussedSignals.iterator(); bIt.hasNext(); )
							{
								Simulate.SimDigitalSignal subDS = (Simulate.SimDigitalSignal)bIt.next();
								int numEvents = subDS.getNumEvents();
								boolean undefined = false;
								for(int i=0; i<numEvents; i++)
								{
									double time = subDS.getTime(i);
									if (time <= curTime)
									{
										switch (subDS.getState(i) & Simulate.SimData.LOGIC)
										{
											case Simulate.SimData.LOGIC_LOW:  curValue &= ~(1<<bit);   undefined = false;   break;
											case Simulate.SimData.LOGIC_HIGH: curValue |= (1<<bit);    undefined = false;   break;
											case Simulate.SimData.LOGIC_X:
											case Simulate.SimData.LOGIC_Z: undefined = true;    break;
										}
									} else
									{
										if (time < nextTime) nextTime = time;
										break;
									}
								}
								if (undefined) { curDefined = false;   break; }
								bit++;
							}
							int x = scaleTimeToX(curTime);
							if (x >= VERTLABELWIDTH)
							{
								if (x < VERTLABELWIDTH+5)
								{
									// on the left edge: just draw the "<"
									drawALine(g, x, hei/2, x+5, hei-5, ws.highlighted);
									drawALine(g, x, hei/2, x+5, 5, ws.highlighted);
								} else
								{
									// bus change point: draw the "X"
									drawALine(g, x-5, 5, x+5, hei-5, ws.highlighted);
									drawALine(g, x+5, 5, x-5, hei-5, ws.highlighted);
								}
								if (lastX+5 < x-5)
								{
									// previous bus change point: draw horizontal bars to connect
									drawALine(g, lastX+5, 5, x-5, 5, ws.highlighted);
									drawALine(g, lastX+5, hei-5, x-5, hei-5, ws.highlighted);
								}
								String valString = "XX";
								if (curDefined) valString = Long.toString(curValue);
								g.setFont(waveWindowFont);
								GlyphVector gv = waveWindowFont.createGlyphVector(waveWindowFRC, valString);
								Rectangle2D glyphBounds = gv.getVisualBounds();
								int textHei = (int)glyphBounds.getHeight();
								g.drawString(valString, x+2, hei/2+textHei/2);
							}
							curTime = nextTime;
							lastX = x;
							if (nextTime == Double.MAX_VALUE) break;
						}
						if (lastX+5 < wid)
						{
							// run horizontal bars to the end
							drawALine(g, lastX+5, 5, wid, 5, ws.highlighted);
							drawALine(g, lastX+5, hei-5, wid, hei-5, ws.highlighted);
						}
						continue;
					}

					// a simple digital signal
					int lastx = VERTLABELWIDTH;
					int lastState = 0;
					if (ds.getStateVector() == null) continue;
					int numEvents = ds.getNumEvents();
					for(int i=0; i<numEvents; i++)
					{
						double time = ds.getTime(i);
						int x = scaleTimeToX(time);
						int lowy = 0, highy = 0;
						int state = ds.getState(i) & Simulate.SimData.LOGIC;
						switch (state)
						{
							case Simulate.SimData.LOGIC_LOW:  lowy = highy = 5;            break;
							case Simulate.SimData.LOGIC_HIGH: lowy = highy = hei-5;        break;
							case Simulate.SimData.LOGIC_X:    lowy = 5;   highy = hei-5;   break;
							case Simulate.SimData.LOGIC_Z:    lowy = 5;   highy = hei-5;   break;
						}
						if (i != 0)
						{
							if (state != lastState)
							{
								drawALine(g, x, 5, x, hei-5, ws.highlighted);
							}
						}
						if (lowy == highy)
						{
							drawALine(g, lastx, lowy, x, lowy, ws.highlighted);
						} else
						{
							g.fillRect(lastx, lowy, x-lastx, highy-lowy);
						}
						lastx = x;
						lastState = state;
					}
				}
			}

			// draw the vertical label
			g.setColor(Color.WHITE);
			g.drawLine(VERTLABELWIDTH, 0, VERTLABELWIDTH, hei);
			if (selected)
			{
				g.drawLine(VERTLABELWIDTH-1, 0, VERTLABELWIDTH-1, hei);
				g.drawLine(VERTLABELWIDTH-2, 0, VERTLABELWIDTH-2, hei-1);
				g.drawLine(VERTLABELWIDTH-3, 0, VERTLABELWIDTH-3, hei-2);
			}
			if (isAnalog)
			{
				double displayedLow = scaleYToValue(hei);
				double displayedHigh = scaleYToValue(0);
				StepSize ss = getSensibleValues(displayedHigh, displayedLow, 5);
				if (ss.separation != 0.0)
				{
					double value = ss.low;
					g.setFont(waveWindowFont);
					for(;;)
					{
						if (value >= displayedLow)
						{
							if (value > displayedHigh) break;
							int y = scaleValueToY(value);
							g.drawLine(VERTLABELWIDTH-10, y, VERTLABELWIDTH, y);
							String yValue = prettyPrint(value, ss.rangeScale, ss.stepScale);
							GlyphVector gv = waveWindowFont.createGlyphVector(waveWindowFRC, yValue);
							Rectangle2D glyphBounds = gv.getVisualBounds();
							int height = (int)glyphBounds.getHeight();
							int yPos = y + height / 2;
							if (yPos-height <= 0) yPos = height+1;
							if (yPos >= hei) yPos = hei;
							g.drawString(yValue, VERTLABELWIDTH-10-(int)glyphBounds.getWidth()-2, yPos);
						}
						value += ss.separation;
					}
				}
			}

			// draw the time cursors
			int x = scaleTimeToX(waveWindow.mainTime);
			if (x >= VERTLABELWIDTH)
				g.drawLine(x, 0, x, hei);
			g.setColor(Color.YELLOW);
			x = scaleTimeToX(waveWindow.extTime);
			if (x >= VERTLABELWIDTH)
				g.drawLine(x, 0, x, hei);
			
			// show dragged area if there
			if (draggingArea)
			{
				g.setColor(Color.WHITE);
				int lowX = Math.min(dragStartX, dragEndX);
				int highX = Math.max(dragStartX, dragEndX);
				int lowY = Math.min(dragStartY, dragEndY);
				int highY = Math.max(dragStartY, dragEndY);
				g.drawLine(lowX, lowY, lowX, highY);
				g.drawLine(lowX, highY, highX, highY);
				g.drawLine(highX, highY, highX, lowY);
				g.drawLine(highX, lowY, lowX, lowY);
				if (ToolBar.getCursorMode() == ToolBar.CursorMode.MEASURE)
				{
					// show dimensions while dragging
					double lowTime = scaleXToTime(lowX);
					double highTime = scaleXToTime(highX);
					double lowValue = scaleYToValue(highY);
					double highValue = scaleYToValue(lowY);
					g.setFont(waveWindowFont);

					// show the low time value
					String lowTimeString = convertToEngineeringNotation(lowTime, "s", 9999);
					GlyphVector gv = waveWindowFont.createGlyphVector(waveWindowFRC, lowTimeString);
					Rectangle2D glyphBounds = gv.getVisualBounds();
					int textWid = (int)glyphBounds.getWidth();
					int textHei = (int)glyphBounds.getHeight();
					g.drawString(lowTimeString, lowX-textWid-2, (lowY+highY)/2+textHei/2);

					// show the high time value
					String highTimeString = convertToEngineeringNotation(highTime, "s", 9999);
					gv = waveWindowFont.createGlyphVector(waveWindowFRC, highTimeString);
					glyphBounds = gv.getVisualBounds();
					textWid = (int)glyphBounds.getWidth();
					textHei = (int)glyphBounds.getHeight();
					g.drawString(highTimeString, highX+2, (lowY+highY)/2+textHei/2);

					// show the difference time value
					String timeDiffString = convertToEngineeringNotation(highTime-lowTime, "s", 9999);
					gv = waveWindowFont.createGlyphVector(waveWindowFRC, timeDiffString);
					glyphBounds = gv.getVisualBounds();
					textWid = (int)glyphBounds.getWidth();
					textHei = (int)glyphBounds.getHeight();
					g.drawString(timeDiffString, lowX+(highX-lowX)/4 - textWid/2, highY-2);
					if (isAnalog)
					{
						// show the low value
						String lowValueString = TextUtils.formatDouble(lowValue);
						gv = waveWindowFont.createGlyphVector(waveWindowFRC, lowValueString);
						glyphBounds = gv.getVisualBounds();
						textWid = (int)glyphBounds.getWidth();
						textHei = (int)glyphBounds.getHeight();
						g.drawString(lowValueString, (lowX+highX)/2 - textWid/2, highY + textHei + 3);
	
						// show the high value
						String highValueString = TextUtils.formatDouble(highValue);
						gv = waveWindowFont.createGlyphVector(waveWindowFRC, highValueString);
						glyphBounds = gv.getVisualBounds();
						textWid = (int)glyphBounds.getWidth();
						textHei = (int)glyphBounds.getHeight();
						g.drawString(highValueString, (lowX+highX)/2 - textWid/2, lowY - 2);
	
						// show the value difference
						String valueDiffString = TextUtils.formatDouble(highValue - lowValue);
						gv = waveWindowFont.createGlyphVector(waveWindowFRC, valueDiffString);
						glyphBounds = gv.getVisualBounds();
						textWid = (int)glyphBounds.getWidth();
						textHei = (int)glyphBounds.getHeight();
						g.drawString(valueDiffString, lowX + 2, lowY+(highY-lowY)/4+textHei/2);
					}
				}
			}
		}

		private void drawALine(Graphics g, int fX, int fY, int tX, int tY, boolean highlighted)
		{
			// clip to left edge
			if (fX < VERTLABELWIDTH || tX < VERTLABELWIDTH)
			{
				Point2D from = new Point2D.Double(fX, fY);
				Point2D to = new Point2D.Double(tX, tY);
				sz = getSize();
				if (EMath.clipLine(from, to, VERTLABELWIDTH, sz.width, 0, sz.height)) return;
				fX = (int)from.getX();
				fY = (int)from.getY();
				tX = (int)to.getX();
				tY = (int)to.getY();
			}

			// draw the line
			g.drawLine(fX, fY, tX, tY);

			// highlight the line if requested
			if (highlighted)
			{
				if (fX == tX)
				{
					// vertical line
					g.drawLine(fX-1, fY, tX-1, tY);
					g.drawLine(fX+1, fY, tX+1, tY);
				} else if (fY == tY)
				{
					// horizontal line
					g.drawLine(fX, fY+1, tX, tY+1);
					g.drawLine(fX, fY-1, tX, tY-1);
				} else
				{
					int xDelta = 0, yDelta = 1;
					if (Math.abs(fX-tX) < Math.abs(fY-tY))
					{
						xDelta = 1;   yDelta = 0;
					}
					g.drawLine(tX+xDelta, tY+yDelta, fX+xDelta, fY+yDelta);
					g.drawLine(tX-xDelta, tY-yDelta, fX-xDelta, fY-yDelta);
				}
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
		 * Method to scale a delta-X to a delta-time in this window.
		 * @param dx the delta-X.
		 * @return the delta-time value corresponding to that coordinate.
		 */
		private double scaleDeltaXToTime(int dx)
		{
			double dTime = ((double)dx) / (sz.width - VERTLABELWIDTH) * (maxTime - minTime);
			return dTime;
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
		 * Method to scale a delta-yY in this window to a delta-value.
		 * @param dy the delta-Y.
		 * @return the delta-value corresponding to that Y change.
		 */
		private double scaleDeltaYToValue(int dy)
		{
			double dValue = - ((double)dy) / (sz.height-1) * analogRange;
			return dValue;
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
			double lXd = Math.min(lX, hX)-2;
			double hXd = Math.max(lX, hX)+2;
			double hYd = Math.min(lY, hY)-2;
			double lYd = Math.max(lY, hY)+2;
			if (lXd > hXd) { double swap = lXd;   lXd = hXd;   hXd = swap; }
			if (lYd > hYd) { double swap = lYd;   lYd = hYd;   hYd = swap; }
			List foundList = new ArrayList();
			for(Iterator it = waveSignals.values().iterator(); it.hasNext(); )
			{
				Signal ws = (Signal)it.next();
				if (ws.sSig instanceof Simulate.SimAnalogSignal)
				{
					// search analog trace
					Simulate.SimAnalogSignal as = (Simulate.SimAnalogSignal)ws.sSig;
					double lastXd = 0, lastYd = 0;
					int numEvents = as.getNumEvents();
					for(int i=0; i<numEvents; i++)
					{
						double time = ws.sSig.getTime(i);
						double x = scaleTimeToX(time);
						double y = scaleValueToY(as.getValue(i));
						if (i != 0)
						{
							// should see if the line is in the area
							Point2D from = new Point2D.Double(lastXd, lastYd);
							Point2D to = new Point2D.Double(x, y);
							if (!EMath.clipLine(from, to, lXd, hXd, lYd, hYd))
							{
								foundList.add(ws);
								break;
							}
						}
						lastXd = x;   lastYd = y;
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
			this.repaint();
		}

		private void addHighlightedSignal(Signal ws)
		{
			if (background == null) background = ws.sigButton.getBackground();
			ws.highlighted = true;
			ws.sigButton.setBackground(Color.BLACK);
			this.repaint();
		}

		private void removeHighlightedSignal(Signal ws)
		{
			ws.highlighted = false;
			ws.sigButton.setBackground(background);
			this.repaint();
		}

		// the MouseListener events
		public void mousePressed(MouseEvent evt)
		{
			// set this to be the selected panel
			for(Iterator it = waveWindow.wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				if (wp.selected && wp != this)
				{
					wp.selected = false;
					wp.repaint();
				}
			}
			if (!selected)
			{
				selected = true;
				this.repaint();
			}
	
			ToolBar.CursorMode mode = ToolBar.getCursorMode();
			if (mode == ToolBar.CursorMode.ZOOM) mousePressedZoom(evt); else
				if (mode == ToolBar.CursorMode.PAN) mousePressedPan(evt); else
					mousePressedSelect(evt);
		}

		public void mouseReleased(MouseEvent evt)
		{
			ToolBar.CursorMode mode = ToolBar.getCursorMode();
			if (mode == ToolBar.CursorMode.ZOOM) mouseReleasedZoom(evt); else
				if (mode == ToolBar.CursorMode.PAN) mouseReleasedPan(evt); else
					mouseReleasedSelect(evt);
		}

		public void mouseClicked(MouseEvent evt) {}
		public void mouseEntered(MouseEvent evt) {}
		public void mouseExited(MouseEvent evt) {}

		// the MouseMotionListener events
		public void mouseMoved(MouseEvent evt)
		{
			ToolBar.CursorMode mode = ToolBar.getCursorMode();
			if (mode == ToolBar.CursorMode.ZOOM) mouseMovedZoom(evt); else
				if (mode == ToolBar.CursorMode.PAN) mouseMovedPan(evt); else
					mouseMovedSelect(evt);
		}
		public void mouseDragged(MouseEvent evt)
		{
			ToolBar.CursorMode mode = ToolBar.getCursorMode();
			if (mode == ToolBar.CursorMode.ZOOM) mouseDraggedZoom(evt); else
				if (mode == ToolBar.CursorMode.PAN) mouseDraggedPan(evt); else
					mouseDraggedSelect(evt);
		}

		// the MouseWheelListener events
		public void mouseWheelMoved(MouseWheelEvent evt) {}

		// the KeyListener events
		public void keyPressed(KeyEvent evt) {}
		public void keyReleased(KeyEvent evt) {}
		public void keyTyped(KeyEvent evt) {}

		// ****************************** SELECTION IN WAVEFORM WINDOW ******************************

		/**
		 * Method to implement the Mouse Pressed event for selection.
		 */ 
		public void mousePressedSelect(MouseEvent evt)
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
	
			// drag area
			draggingArea = true;
			dragStartX = dragEndX = evt.getX();
			dragStartY = dragEndY = evt.getY();
		}
	
		/**
		 * Method to implement the Mouse Released event for selection.
		 */ 
		public void mouseReleasedSelect(MouseEvent evt)
		{
			if (draggingArea)
			{
				Panel wp = (Panel)evt.getSource();
				if (ToolBar.getCursorMode() != ToolBar.CursorMode.MEASURE &&
					ToolBar.getSelectMode() == ToolBar.SelectMode.OBJECTS)
				{
					draggingArea = false;
					List foundList = wp.findSignalsInArea(dragStartX, dragEndX, dragStartY, dragEndY);
					if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0)
					{
						// standard click: add this as the only trace
						clearHighlightedSignals();
						for(Iterator it = foundList.iterator(); it.hasNext(); )
						{
							Signal ws = (Signal)it.next();
							wp.addHighlightedSignal(ws);
						}
						if (foundList.size() == 1)
						{
							// a single signal: show it in the schematic
							Signal sig = (Signal)foundList.iterator().next();
							sig.showNetworkInSchematic();
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
				} else
				{
					// just leave this highlight and show dimensions
				}
			}
			this.repaint();
		}

		/**
		 * Method to implement the Mouse Dragged event for selection.
		 */ 
		public void mouseDraggedSelect(MouseEvent evt)
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
				dragEndX = evt.getX();
				dragEndY = evt.getY();
				this.repaint();
			}
		}

		public void mouseMovedSelect(MouseEvent evt) {}

		// ****************************** ZOOMING IN WAVEFORM WINDOW ******************************
	
		/**
		 * Method to implement the Mouse Pressed event for zooming.
		 */ 
		public void mousePressedZoom(MouseEvent evt)
		{
			dragStartX = evt.getX();
			dragStartY = evt.getY();
			ZoomAndPanListener.setProperCursor(evt);
			draggingArea = true;
		}
		
		/**
		 * Method to implement the Mouse Released event for zooming.
		 */ 
		public void mouseReleasedZoom(MouseEvent evt)
		{
			ZoomAndPanListener.setProperCursor(evt);
			draggingArea = false;
			double lowTime = this.scaleXToTime(Math.min(dragEndX, dragStartX));
			double highTime = this.scaleXToTime(Math.max(dragEndX, dragStartX));
			double timeRange = highTime - lowTime;
			lowTime -= timeRange / 8;
			highTime += timeRange / 8;
			double lowValue = this.scaleYToValue(Math.max(dragEndY, dragStartY));
			double highValue = this.scaleYToValue(Math.min(dragEndY, dragStartY));
			double valueRange = highValue - lowValue;
			lowValue -= valueRange / 8;
			highValue += valueRange / 8;
			for(Iterator it = waveWindow.wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				if (!waveWindow.timeLocked && wp != this) continue;
				if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0)
				{
					// standard click: zoom in
					wp.minTime = lowTime;
					wp.maxTime = highTime;
					if (wp == this)
					{
						wp.setValueRange(lowValue, highValue);
					}
				} else
				{
					// shift-click: zoom out
					double oldRange = wp.maxTime - wp.minTime;
					wp.minTime = (lowTime + highTime) / 2 - oldRange;
					wp.maxTime = (lowTime + highTime) / 2 + oldRange;
					if (wp == this)
					{
						wp.setValueRange((lowValue + highValue) / 2 - wp.analogRange,
							(lowValue + highValue) / 2 + wp.analogRange);
					}
				}
				wp.timePanel.repaint();
				wp.repaint();
			}
		}
		
		/**
		 * Method to implement the Mouse Dragged event for zooming.
		 */ 
		public void mouseDraggedZoom(MouseEvent evt)
		{
			ZoomAndPanListener.setProperCursor(evt);
			if (draggingArea)
			{
				dragEndX = evt.getX();
				dragEndY = evt.getY();
				this.repaint();
			}
		}

		public void mouseMovedZoom(MouseEvent evt)
		{
			ZoomAndPanListener.setProperCursor(evt);
		}

		// ****************************** PANNING IN WAVEFORM WINDOW ******************************
	
		/**
		 * Method to implement the Mouse Pressed event for panning.
		 */ 
		public void mousePressedPan(MouseEvent evt)
		{
			dragStartX = evt.getX();
			dragStartY = evt.getY();
		}
		
		/**
		 * Method to implement the Mouse Released event for panning.
		 */ 
		public void mouseReleasedPan(MouseEvent evt)
		{
		}
		
		/**
		 * Method to implement the Mouse Dragged event for panning.
		 */ 
		public void mouseDraggedPan(MouseEvent evt)
		{
			dragEndX = evt.getX();
			dragEndY = evt.getY();
			double dTime = scaleDeltaXToTime(dragEndX - dragStartX);
			double dValue = scaleDeltaYToValue(dragEndY - dragStartY);

			for(Iterator it = waveWindow.wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				if (!waveWindow.timeLocked && wp != this) continue;
				wp.minTime -= dTime;
				wp.maxTime -= dTime;
				if (wp == this)
				{
					setValueRange(analogLowValue - dValue, analogHighValue - dValue);
				}
				wp.timePanel.repaint();
				wp.repaint();
			}
			dragStartX = dragEndX;
			dragStartY = dragEndY;
		}

		public void mouseMovedPan(MouseEvent evt) {}
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
			g.drawLine(WaveformWindow.Panel.VERTLABELWIDTH, hei-1, wid, hei-1);
			double displayedLow = wavePanel.scaleXToTime(WaveformWindow.Panel.VERTLABELWIDTH);
			double displayedHigh = wavePanel.scaleXToTime(wid);
			StepSize ss = getSensibleValues(displayedHigh, displayedLow, 10);
			if (ss.separation == 0.0) return;
			double time = ss.low;
			for(;;)
			{
				if (time >= displayedLow)
				{
					if (time > ss.high) break;
					int x = wavePanel.scaleTimeToX(time);
					g.drawLine(x, 0, x, hei);
					String timeVal = convertToEngineeringNotation(time, "s", ss.stepScale);
					g.drawString(timeVal, x+2, hei-2);
				}
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

		public Signal(Panel wavePanel, Simulate.SimSignal sSig)
		{
			this.wavePanel = wavePanel;
			this.sSig = sSig;
			this.highlighted = false;
			String sigName = sSig.getSignalName();
			if (sSig.getSignalContext() != null) sigName = sSig.getSignalContext() + "." + sigName;
			sigButton = new JButton(sigName);
			sigButton.setBorderPainted(false);
			sigButton.setDefaultCapable(false);
			sigButton.setForeground(sSig.getSignalColor());
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

				// a single signal: show it in the schematic
				ws.showNetworkInSchematic();
			} else
			{
				// shift click: add or remove to list of highlighted traces
				if (ws.highlighted) ws.wavePanel.removeHighlightedSignal(ws); else
					ws.wavePanel.addHighlightedSignal(ws);
			}
		}

		/**
		 * Method called when a signal is selected in the waveform and should be crossprobed to the schematic.
		 */
		public void showNetworkInSchematic()
		{
			// if this waveform has no cell associated with it, bail
			Cell cell = wavePanel.waveWindow.getCell();
			if (cell == null) return;

			// look for the original cell to highlight it
			for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)it.next();
				if (wf.getContent().getCell() != cell) continue;
				if (wf.getContent() instanceof EditWindow)
				{
					Netlist netlist = cell.getUserNetlist();
					String want = sSig.getSignalName();

					JNetwork net = findNetwork(netlist, want);
					if (net != null)
					{
						Highlight.clear();
						Highlight.addNetwork(net, cell);
						Highlight.finished();
					}
					return;	
				}
			}
		}
	}

	public void highlightChanged()
	{
		Set highSet = Highlight.getHighlightedNetworks();
		if (highSet.size() == 1)
		{
			JNetwork net = (JNetwork)highSet.iterator().next();
			String netName = net.describe();

			// look at all signal names in the cell
			for(Iterator it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();

				// look at all traces in this panel
				// Should use code from "network.cpp:net_parsenetwork()" on the names of highlighted signals
				for(Iterator sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
				{
					Signal ws = (Signal)sIt.next();
					if (netName.equals(ws.sSig.getSignalName()))
					{
						for(Iterator oIt = wavePanels.iterator(); oIt.hasNext(); )
						{
							Panel oWp = (Panel)oIt.next();
							oWp.clearHighlightedSignals();
						}

						wp.addHighlightedSignal(ws);
						repaint();
						return;
					}
				}
			}
		}
	}

	private static JNetwork findNetwork(Netlist netlist, String name)
	{
		/*
		 * Should really use extended code, found in "simspicerun.cpp:sim_spice_signalname()"
		 */
		for(Iterator nIt = netlist.getNetworks(); nIt.hasNext(); )
		{
			JNetwork net = (JNetwork)nIt.next();
			if (net.describe().equals(name)) return net;
		}
		return null;
	}

	/**
	 * Method to get a Set of currently highlighted networks in this WaveformWindow.
	 */
	public Set getHighlightedNetworks()
	{
		// make empty set
		Set nets = new HashSet();

		// if no cell in the window, stop now
		Cell cell = sd.getCell();
		if (cell == null) return nets;
		Netlist netlist = cell.getUserNetlist();

		// look at all signal names in the cell
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();

			// look at all traces in this panel
			for(Iterator sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
			{
				Signal ws = (Signal)sIt.next();
				JNetwork net = findNetwork(netlist, ws.sSig.getSignalName());
				nets.add(net);
			}
		}
		return nets;
	}

	/** the window that this lives in */					private WindowFrame wf;
	/** the cell being simulated */							private Simulate.SimData sd;
	/** the top-level panel of the waveform window. */		private JPanel overall;
	/** let panel: the signal names */						private JPanel left;
	/** right panel: the signal traces */					private JPanel right;
	/** the "lock time" button. */							private JButton timeLock;
	/** the "grow panel" button for widening. */			private JButton growPanel;
	/** the "shrink panel" button for narrowing. */			private JButton shrinkPanel;
	/** the main scroll of all panels. */					private JScrollPane scrollAll;
	/** the split between signal names and traces. */		private JSplitPane split;
	/** labels for the text at the top */					private JLabel mainPos, extPos, delta;
	/** a list of panels in this window */					private List wavePanels;
	/** current "main" time cursor */						private double mainTime;
	/** current "extension" time cursor */					private double extTime;
	/** default range along horozintal axis */				private double minTime, maxTime;
	/** true if the time axis is the same in each panel */	private boolean timeLocked;

	private static final ImageIcon iconAddPanel = new ImageIcon(WaveformWindow.class.getResource("ButtonSimAddPanel.gif"));
	private static final ImageIcon iconLockTime = new ImageIcon(WaveformWindow.class.getResource("ButtonSimLockTime.gif"));
	private static final ImageIcon iconUnLockTime = new ImageIcon(WaveformWindow.class.getResource("ButtonSimUnLockTime.gif"));
	private static final ImageIcon iconGrowPanel = new ImageIcon(WaveformWindow.class.getResource("ButtonSimGrow.gif"));
	private static final ImageIcon iconShrinkPanel = new ImageIcon(WaveformWindow.class.getResource("ButtonSimShrink.gif"));

    // ************************************* CONTROL *************************************

	public WaveformWindow(Simulate.SimData sd, WindowFrame wf)
	{
		// initialize the structure
		this.wf = wf;
		this.sd = sd;
		wavePanels = new ArrayList();
		this.timeLocked = true;

		Highlight.addHighlightListener(this);

		// the total panel in the waveform window
		overall = new JPanel();
		overall.setLayout(new GridBagLayout());

		// the main part of the waveform window: a split-pane between names and traces
		left = new JPanel();
		left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
		right = new JPanel();
		right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
		split.setResizeWeight(0.1);
		scrollAll = new JScrollPane(split);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;       gbc.gridy = 1;
		gbc.gridwidth = 7;   gbc.gridheight = 1;
		gbc.weightx = 1;     gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(scrollAll, gbc);

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

		timeLock = new JButton(iconLockTime);
		timeLock.setBorderPainted(false);
		timeLock.setDefaultCapable(false);
		gbc.gridx = 1;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(timeLock, gbc);
		timeLock.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { togglePanelTimeLock(); }
		});

		growPanel = new JButton(iconGrowPanel);
		growPanel.setBorderPainted(false);
		growPanel.setDefaultCapable(false);
		gbc.gridx = 2;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(growPanel, gbc);
		growPanel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { growPanels(1.25); }
		});

		shrinkPanel = new JButton(iconShrinkPanel);
		shrinkPanel.setBorderPainted(false);
		shrinkPanel.setDefaultCapable(false);
		gbc.gridx = 3;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(shrinkPanel, gbc);
		shrinkPanel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { growPanels(0.8); }
		});

		mainPos = new JLabel("Main:");
		gbc.gridx = 4;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.3;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(mainPos, gbc);
		extPos = new JLabel("Ext:");
		gbc.gridx = 5;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.3;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(extPos, gbc);
		delta = new JLabel("Delta:");
		gbc.gridx = 6;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.3;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(delta, gbc);
	}

	/**
	 * Method to return the top-level JPanel for this WaveformWindow.
	 * The actual WaveformWindow object is below the top level, surrounded by scroll bars and other display artifacts.
	 * @return the top-level JPanel for this WaveformWindow.
	 */
	public JPanel getPanel() { return overall; }

	public void setCell(Cell cell, VarContext context)
	{
		sd.setCell(cell);
		setWindowTitle();
	}

	/**
	 * Method to return the cell that is shown in this window.
	 * @return the cell that is shown in this window.
	 */
	public Cell getCell() { return sd.getCell(); }

	public void loadExplorerTree(DefaultMutableTreeNode rootNode)
	{
		wf.libraryExplorerNode = null;
		wf.jobExplorerNode = Job.getExplorerTree();
		wf.errorExplorerNode = ErrorLogger.getExplorerTree();
		wf.signalExplorerNode = getSignalsForExplorer();
		rootNode.add(wf.signalExplorerNode);
		rootNode.add(wf.jobExplorerNode);
		rootNode.add(wf.errorExplorerNode);
	}

	public void bottomScrollChanged(int e) {}

	public void rightScrollChanged(int e) {}

	public boolean cellHistoryCanGoBack() { return false; }

	public boolean cellHistoryCanGoForward() { return false; }

	public void cellHistoryGoBack() {}

	public void cellHistoryGoForward() {}

	private DefaultMutableTreeNode getSignalsForExplorer()
	{
		DefaultMutableTreeNode signalsExplorerTree = new DefaultMutableTreeNode("SIGNALS");
		HashMap contextMap = new HashMap();
		contextMap.put("", signalsExplorerTree);
		for(Iterator it = sd.getSignals().iterator(); it.hasNext(); )
		{
			Simulate.SimSignal sSig = (Simulate.SimSignal)it.next();
			DefaultMutableTreeNode thisTree = signalsExplorerTree;
			if (sSig.getSignalContext() != null)
			{
				thisTree = (DefaultMutableTreeNode)contextMap.get(sSig.getSignalContext());
				if (thisTree == null)
				{
					String branchName = sSig.getSignalContext();
					String parent = "";
					int dotPos = branchName.lastIndexOf('.');
					if (dotPos >= 0)
					{
						parent = branchName.substring(0, dotPos);
						branchName = branchName.substring(dotPos+1);
					}
					thisTree = new DefaultMutableTreeNode(branchName);
					contextMap.put(sSig.getSignalContext(), thisTree);
					DefaultMutableTreeNode parentTree = (DefaultMutableTreeNode)contextMap.get(parent);
					if (parentTree != null)
						parentTree.add(thisTree);
				}
			}
			thisTree.add(new DefaultMutableTreeNode(sSig));
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

	/**
	 * Method to set the time range in all panels.
	 * @param minTime the low time value.
	 * @param maxTime the high time value.
	 */
	public void setDefaultTimeRange(double minTime, double maxTime)
	{
		this.minTime = minTime;
		this.maxTime = maxTime;
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
	 * Method to set the window title.
	 */
	public void setWindowTitle()
	{
		if (wf == null) return;
		/*
		if (sd.getCell() == null)
		{
			wf.setTitle("***WAVEFORM WITH NO CELL***");
			return;
		}

		String title = "Waveform for " + sd.getCell().describe();
		if (sd.getCell().getLibrary() != Library.getCurrent())
			title += " - Current library: " + Library.getCurrent().getName();
			*/
		wf.setTitle(wf.composeTitle(sd.getCell(), "Waveform for "));
	}

	private static class StepSize
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
		if (time < 1.0E-15 || time >= 1000.0) return negative + TextUtils.formatDouble(time) + unit;

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
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"Cannot delete the last waveform panel");
			return;
		}
		left.remove(wp.leftHalf);
		right.remove(wp.rightHalf);
		wavePanels.remove(wp);
		overall.validate();
		redrawAll();
	}
	
	/**
	 * Method called to grow or shrink the panels vertically.
	 */
	public void growPanels(double scale)
	{
		panelSizeDigital = (int)(panelSizeDigital * scale);
		panelSizeAnalog = (int)(panelSizeAnalog * scale);
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			Dimension sz = wp.getSize();
			sz.height = (int)(sz.height * scale);
			wp.setSize(sz.width, sz.height);
			wp.setPreferredSize(sz);

			sz = wp.signalButtonsPane.getSize();
			sz.height = (int)(sz.height * scale);
			wp.signalButtonsPane.setPreferredSize(sz);
			wp.signalButtonsPane.setSize(sz.width, sz.height);
		}
		overall.validate();
		redrawAll();
	}

	/**
	 * Method called to toggle the panel time lock.
	 */
	public void togglePanelTimeLock()
	{
		timeLocked = ! timeLocked;
		if (timeLocked)
		{
			timeLock.setIcon(iconLockTime);
			double minTime = 0, maxTime = 0;
			boolean first = true;
			for(Iterator it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				if (first)
				{
					first = false;
					minTime = wp.minTime;
					maxTime = wp.maxTime;
				} else
				{
					if (wp.minTime < minTime)
					{
						minTime = wp.minTime;
						maxTime = wp.maxTime;
					}
				}
			}
			for(Iterator it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				if (wp.minTime != minTime || wp.maxTime != maxTime)
				{
					wp.minTime = minTime;
					wp.maxTime = maxTime;
					wp.timePanel.repaint();
					wp.repaint();
				}
			}
		} else
		{
			timeLock.setIcon(iconUnLockTime);
		}
	}

	/**
	 * Method called when a new Panel is to be created.
	 * This is typically overridden by the specific simulation module
	 * which knows the signal names.
	 */
	public void addSignalInNewPanel()
	{
		ExplorerTree tree = wf.getExplorerTree();
		Object obj = tree.getCurrentlySelectedObject();
		if (obj instanceof Simulate.SimSignal)
		{
			Simulate.SimSignal sig = (Simulate.SimSignal)obj;
			boolean isAnalog = false;
			if (sig instanceof Simulate.SimAnalogSignal) isAnalog = true;
			Panel wp = new Panel(this, isAnalog);
			if (isAnalog)
			{
				Simulate.SimAnalogSignal as = (Simulate.SimAnalogSignal)sig;
				double lowValue = 0, highValue = 0;
				for(int i=0; i<as.getNumEvents(); i++)
				{
					double val = as.getValue(i);
					if (i == 0) lowValue = highValue = val; else
					{
						if (val < lowValue) lowValue = val;
						if (val > highValue) highValue = val;
					}
				}
				double range = highValue - lowValue;
				if (range == 0) range = 2;
				double rangeExtra = range / 10;
				wp.setValueRange(lowValue - rangeExtra, highValue + rangeExtra);
			}
			Signal wsig = new Signal(wp, sig);
			overall.validate();
			wp.repaint();
			return;
		}
		JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
			"First select a signal from the explorer tree");
	}

	/**
	 * Method called when a signal is to be added to a Panel.
	 * This is typically overridden by the specific simulation module
	 * which knows the signal names.
	 * @param wp the Panel in which to add the signal.
	 */
	public void overlaySignalInPanel(Panel wp)
	{
		if (!wp.isAnalog)
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"Can only add signals to an analog panel");
			return;
		}
		ExplorerTree tree = wf.getExplorerTree();
		Object obj = tree.getCurrentlySelectedObject();
		if (obj instanceof Simulate.SimSignal)
		{
			Simulate.SimSignal sig = (Simulate.SimSignal)obj;
			if (sig instanceof Simulate.SimDigitalSignal)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Can only add analog signals to this panel");
				return;
			}
			Signal wsig = new Signal(wp, sig);
			wp.signalButtons.validate();
			wp.signalButtons.repaint();
			wp.signalButtonsPane.validate();
			wp.repaint();
			return;
		}
		JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
			"First select a signal from the explorer tree");
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

	public void fillScreen()
	{
		Rectangle2D bounds = sd.getBounds();
		double lowTime = bounds.getMinX();
		double highTime = bounds.getMaxX();
		double lowValue = bounds.getMinY();
		double highValue = bounds.getMaxY();
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!timeLocked && !wp.selected) continue;

			boolean repaint = false;
			if (wp.minTime != lowTime || wp.maxTime != highTime)
			{
				wp.minTime = lowTime;
				wp.maxTime = highTime;
				repaint = true;
			}
			if (wp.isAnalog)
			{
				if (wp.minTime != lowValue || wp.maxTime != highValue)
				{
					wp.setValueRange(lowValue, highValue);
					repaint = true;
				}
			}
			if (repaint)
			{
				wp.timePanel.repaint();
				wp.repaint();
			}
		}
	}

	public void zoomOutContents()
	{
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!timeLocked && !wp.selected) continue;

			boolean repaint = false;
			double range = wp.maxTime - wp.minTime;
			wp.minTime -= range/2;
			wp.maxTime += range/2;
			wp.timePanel.repaint();
			wp.repaint();
		}
	}

	public void zoomInContents()
	{
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!timeLocked && !wp.selected) continue;

			boolean repaint = false;
			double range = wp.maxTime - wp.minTime;
			wp.minTime += range/4;
			wp.maxTime -= range/4;
			wp.timePanel.repaint();
			wp.repaint();
		}
	}

	public void focusOnHighlighted()
	{
		if (mainTime == extTime) return;
		double maxTime, minTime;
		if (mainTime > extTime)
		{
			double size = (mainTime-extTime) / 20.0;
			maxTime = mainTime + size;
			minTime = extTime - size;
		} else
		{
			double size = (extTime-mainTime) / 20.0;
			maxTime = extTime + size;
			minTime = mainTime - size;
		}
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!timeLocked && !wp.selected) continue;
			if (wp.minTime != minTime || wp.maxTime != maxTime)
			{
				wp.minTime = minTime;
				wp.maxTime = maxTime;
				wp.timePanel.repaint();
				wp.repaint();
			}
		}
	}

	public void finished()
	{
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.finished();
		}
	}


	public void fullRepaint() { repaint(); }

	public void repaint()
	{
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.repaint();
		}
	}

	public void fireCellHistoryStatus()
	{
	}

	/**
	 * Method to initialize for a new text search.
	 * @param search the string to locate.
	 * @param caseSensitive true to match only where the case is the same.
	 */
	public void initTextSearch(String search, boolean caseSensitive) {}

	/**
	 * Method to find the next occurrence of a string.
	 * @param reverse true to find in the reverse direction.
	 * @return true if something was found.
	 */
	public boolean findNextText(boolean reverse) { return false; }

	/**
	 * Method to replace the text that was just selected with findNextText().
	 * @param replace the new text to replace.
	 */
	public void replaceText(String replace) {}

	/**
	 * Method to replace all selected text.
	 * @param replace the new text to replace everywhere.
	 */
	public void replaceAllText(String replace) {}

}
