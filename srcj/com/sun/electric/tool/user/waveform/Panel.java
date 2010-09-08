/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Panel.java
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
package com.sun.electric.tool.user.waveform;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.simulation.BusSample;
import com.sun.electric.tool.simulation.DigitalSample;
import com.sun.electric.tool.simulation.RangeSample;
import com.sun.electric.tool.simulation.Sample;
import com.sun.electric.tool.simulation.ScalarSample;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.simulation.SweptSample;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.WaveformZoom;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;
import com.sun.electric.tool.user.ui.ElectricPrinter;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.ZoomAndPanListener;
import com.sun.electric.util.TextUtils;
import com.sun.electric.util.math.GenMath;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * This class defines a single panel of WaveSignals with an associated list of signal names.
 */
public class Panel extends JPanel
	implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
{
	/** Use VolatileImage for offscreen buffer */           private static final boolean USE_VOLATILE_IMAGE = false;
	/** Use anti-aliasing for lines */                      private static final boolean USE_ANTIALIASING = false;
	/** Spacing above and below each panel */               private static final int PANELGAP = 2;

	/** the main waveform window this is part of */			private WaveformWindow waveWindow;
	/** the size of the window (in pixels) */				private Dimension sz;
	/** true if the size field is valid */					private boolean szValid;
	/** the signal on the X axis (null for time) */			private Signal<?> xAxisSignal;
	/** maps signal buttons to the actual Signal */			private LinkedHashMap<JButton,WaveSignal> waveSignals = new LinkedHashMap<JButton,WaveSignal>();
	/** the list of signal name buttons on the left */		private JPanel signalButtons;
	/** the JScrollPane with of signal name buttons */		private JScrollPane signalButtonsPane;
	/** the left side: with signal names etc. */			private JPanel leftHalf;
	/** the right side: with signal traces */				private JPanel rightHalf;
	/** the title of the panel */							private DragButton panelTitle;
	/** the button to close this panel. */					private JButton close;
	/** the button to hide this panel. */					private JButton hide;
	/** the button to delete selected signal. */			private JButton deleteSignal;
	/** the button to delete all signals. */				private JButton deleteAllSignals;
	/** displayed range along horizontal axis */			private double minXPosition, maxXPosition;
	/** low value displayed in this panel (analog) */		private double analogLowValue;
	/** high value displayed in this panel (analog) */		private double analogHighValue;
	/** vertical range displayed in this panel (analog) */	private double analogRange;
	/** true if an X axis cursor is being dragged */		private boolean draggingMain, draggingExt, draggingVertAxis;
	/** true if an area is being dragged */					private boolean draggingArea;
	/** list of measurements being displayed */				private List<Rectangle2D> measurementList;
	/** current measurement being displayed */				private Rectangle2D curMeasurement;
	/** true if this waveform panel is selected */			private boolean selected;
	/** true if this waveform panel is hidden */			private boolean hidden;
	/** the horizontal ruler at the top of this panel. */	private HorizRuler horizRulerPanel;
	/** true if the horizontal ruler is logarithmic */		private boolean horizRulerPanelLogarithmic;
	/** true if this panel is logarithmic in Y */			private boolean vertPanelLogarithmic;
	/** the number of this panel. */						private int panelNumber;
	/** extent of area dragged-out by cursor */				private double dragStartXD, dragStartYD;
	/** extent of area dragged-out by cursor */				private double dragEndXD, dragEndYD;
	/** the location of the Y axis vertical line */			private int vertAxisPos;
	/** the smallest nonzero X value (for log drawing) */	private double smallestXValue;
	/** the smallest nonzero Y value (for log drawing) */	private double smallestYValue;

	/** the background color of a button */					private static Color background = null;
	/** The color of the grid (a gray) */					private static Color gridColor = new Color(0x808080);
	/** for determining double-clicks */					private static long lastClick = 0;
	/** current panel */									private static Panel curPanel;
	/** current X coordinate in the panel */				private static int curXPos;

	/** for drawing far-dotted lines */						private static final BasicStroke farDottedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {4,12}, 0);
	/** the size of control point squares */				private static final int CONTROLPOINTSIZE = 6;
	/** the width of the panel label on the left */			private static final int VERTLABELWIDTH = 60;
	private static final ImageIcon iconHidePanel = Resources.getResource(WaveformWindow.class, "ButtonSimHide.gif");
	private static final ImageIcon iconClosePanel = Resources.getResource(WaveformWindow.class, "ButtonSimClose.gif");
	private static final ImageIcon iconDeleteSignal = Resources.getResource(WaveformWindow.class, "ButtonSimDelete.gif");
	private static final ImageIcon iconDeleteAllSignals = Resources.getResource(WaveformWindow.class, "ButtonSimDeleteAll.gif");
	private static final Cursor dragXPositionCursor = ToolBar.readCursor("CursorDragTime.gif", 8, 8);

	/**
	 * Constructor creates a panel in a WaveformWindow.
	 * @param waveWindow the WaveformWindow in which to place this Panel.
	 */
	public Panel(WaveformWindow waveWindow, int height)
	{
		// remember state
		this.waveWindow = waveWindow;
		selected = false;
		panelNumber = waveWindow.getNewPanelNumber();
		vertAxisPos = VERTLABELWIDTH;
		horizRulerPanelLogarithmic = false;
		vertPanelLogarithmic = false;
		xAxisSignal = null;
		measurementList = new ArrayList<Rectangle2D>();
		curMeasurement = null;

		// setup this panel window
		sz = new Dimension(50, height);
		szValid = false;
		setSize(sz.width, sz.height);
		setPreferredSize(sz);
		setLayout(new FlowLayout());

		// add listeners --> BE SURE to remove listeners in finished()
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);

		setXAxisRange(waveWindow.getLowDefaultHorizontalRange(), waveWindow.getHighDefaultHorizontalRange());

		// the left side with signal names
		leftHalf = new WaveformWindow.OnePanel(this, waveWindow);
		leftHalf.setLayout(new GridBagLayout());
		leftHalf.setPreferredSize(new Dimension(100, height));

		// a drop target for the signal panel
		new DropTarget(leftHalf, DnDConstants.ACTION_LINK, WaveformWindow.waveformDropTarget, true);

//		// a separator at the top
//		JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
//		GridBagConstraints gbc = new GridBagConstraints();
//		gbc.gridx = 0;       gbc.gridy = 0;
//		gbc.gridwidth = 5;   gbc.gridheight = 1;
//		gbc.weightx = 1;     gbc.weighty = 0;
//		gbc.anchor = GridBagConstraints.NORTH;
//		gbc.fill = GridBagConstraints.HORIZONTAL;
//		gbc.insets = new Insets(4, 0, 4, 0);
//		leftHalf.add(sep, gbc);

		// the name of this panel
		panelTitle = new DragButton("" + Integer.toString(panelNumber), panelNumber);
		panelTitle.setToolTipText("Identification number of this waveform panel (drag the number to rearrange panels)");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;      gbc.gridy = 1;
		gbc.weightx = 1;    gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(2, 4, 2, 1);
		panelTitle.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { panelTitleClicked(evt); }
		});
		leftHalf.add(panelTitle, gbc);

		// the close button for this panel
		close = new JButton(iconClosePanel);
		close.setBorderPainted(false);
		close.setDefaultCapable(false);
		close.setToolTipText("Close this waveform panel");
		Dimension minWid = new Dimension(iconClosePanel.getIconWidth()+4, iconClosePanel.getIconHeight()+4);
		close.setMinimumSize(minWid);
		close.setPreferredSize(minWid);
		gbc = new GridBagConstraints();
		gbc.gridx = 1;       gbc.gridy = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 1, 0, 2);
		leftHalf.add(close, gbc);
		close.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { closePanel(); }
		});

		// the hide button for this panel
		hide = new JButton(iconHidePanel);
		hide.setBorderPainted(false);
		hide.setDefaultCapable(false);
		hide.setToolTipText("Hide this waveform panel");
		minWid = new Dimension(iconHidePanel.getIconWidth()+4, iconHidePanel.getIconHeight()+4);
		hide.setMinimumSize(minWid);
		hide.setPreferredSize(minWid);
		gbc = new GridBagConstraints();
		gbc.gridx = 2;       gbc.gridy = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 2, 0, 2);
		leftHalf.add(hide, gbc);
		hide.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { hidePanel(); }
		});

		// the "delete signal" button for this panel
		deleteSignal = new JButton(iconDeleteSignal);
		deleteSignal.setBorderPainted(false);
		deleteSignal.setDefaultCapable(false);
		deleteSignal.setToolTipText("Remove selected signals from this panel");
		minWid = new Dimension(iconDeleteSignal.getIconWidth()+4, iconDeleteSignal.getIconHeight()+4);
		deleteSignal.setMinimumSize(minWid);
		deleteSignal.setPreferredSize(minWid);
		gbc = new GridBagConstraints();
		gbc.gridx = 3;      gbc.gridy = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 2, 0, 2);
		leftHalf.add(deleteSignal, gbc);
		deleteSignal.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { deleteSignalFromPanel(); }
		});

		// the "delete all signal" button for this panel
		deleteAllSignals = new JButton(iconDeleteAllSignals);
		deleteAllSignals.setBorderPainted(false);
		deleteAllSignals.setDefaultCapable(false);
		deleteAllSignals.setToolTipText("Remove all signals from this panel");
		minWid = new Dimension(iconDeleteAllSignals.getIconWidth()+4, iconDeleteAllSignals.getIconHeight()+4);
		deleteAllSignals.setMinimumSize(minWid);
		deleteAllSignals.setPreferredSize(minWid);
		gbc = new GridBagConstraints();
		gbc.gridx = 4;       gbc.gridy = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 2, 0, 4);
		leftHalf.add(deleteAllSignals, gbc);
		deleteAllSignals.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { deleteAllSignalsFromPanel(); }
		});

		// the list of signals in this panel
		signalButtons = new JPanelX();
		signalButtons.setLayout(new BoxLayout(signalButtons, BoxLayout.Y_AXIS));
		signalButtonsPane = new JScrollPane(signalButtons);
        signalButtonsPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        signalButtons.setAlignmentX(1.0f);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;       gbc.gridy = 3;
		gbc.gridwidth = 5;   gbc.gridheight = 1;
		gbc.weightx = 1;     gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.BOTH;
		leftHalf.add(signalButtonsPane, gbc);

		// the right side with signal traces
		rightHalf = new JPanel();
		rightHalf.setLayout(new GridBagLayout());
		rightHalf.setPreferredSize(new Dimension(100, height));

		// a drop target for the signal panel
		new DropTarget(this, DnDConstants.ACTION_LINK, WaveformWindow.waveformDropTarget, true);

//		// a separator at the top
//		sep = new JSeparator(SwingConstants.HORIZONTAL);
//		gbc = new GridBagConstraints();
//		gbc.gridx = 0;       gbc.gridy = 0;
//		gbc.weightx = 1;     gbc.weighty = 0;
//		gbc.anchor = GridBagConstraints.NORTH;
//		gbc.fill = GridBagConstraints.HORIZONTAL;
//		gbc.insets = new Insets(4, 0, 4, 0);
//		rightHalf.add(sep, gbc);

		// the horizontal ruler (if separate rulers in each panel)
		if (!waveWindow.isXAxisLocked())
			addHorizRulerPanel();

		// the waveform display for this panel
		gbc = new GridBagConstraints();
		gbc.gridx = 0;       gbc.gridy = 2;
		gbc.weightx = 1;     gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(PANELGAP, 0, PANELGAP, 0);
		rightHalf.add(this, gbc);

		// add to list of wave panels
		waveWindow.addPanel(this);

		// put the left and right sides into the window
		waveWindow.getWaveformTable().repaint();
		waveWindow.getWaveformTable().doLayout();
		waveWindow.getWaveformTable().updateUI();

		// rebuild list of panels
		waveWindow.rebuildPanelList();
		waveWindow.redrawAllPanels();
	}

	// ************************************* MISCELLANEOUS *************************************

    /** A subclass of JPanel which implements Scrollable and always tracks its JScrollPane's height */
    private static class JPanelX extends JPanel implements Scrollable {
        public JPanelX() { }
        public boolean getScrollableTracksViewportWidth()  { return true; }
        public boolean getScrollableTracksViewportHeight()  { return false; }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle visibleRect,
                                              int orientation,
                                              int direction) {
            return 1;
        }
        public int getScrollableBlockIncrement(Rectangle visibleRect,
                                               int orientation,
                                               int direction) {
            return 1;
        }
    }

	public WaveformWindow getWaveWindow() { return waveWindow; }

	/**
	 * Method to get rid of this Panel.
	 */
	public void finished()
	{
		// remove myself from listener list
		removeKeyListener(this);
		removeMouseListener(this);
		removeMouseMotionListener(this);
		removeMouseWheelListener(this);
	}

	public JPanel getLeftHalf() { return leftHalf; }

	public JPanel getRightHalf() { return rightHalf; }

	/**
	 * Make this panel show a linear Y axis.
	 */
	private void makeLinear() { setPanelLogarithmicVertically(false); }

	/**
	 * Make this panel show a logarithmic Y axis.
	 */
	private void makeLogarithmic() { setPanelLogarithmicVertically(true); }

	public void setPanelLogarithmicVertically(boolean logarithmic)
	{
		vertPanelLogarithmic = logarithmic;
		repaintContents();
	}

	public boolean isAnalog()
	{
		for(JButton but : waveSignals.keySet())
		{
			WaveSignal ws = waveSignals.get(but);
			Signal<?> sig = ws.getSignal();
			if (!sig.isDigital()) return true;
		}
		return false;
	}

	public JPanel getSignalButtons() { return signalButtons; };

	public JScrollPane getSignalButtonsPane() { return signalButtonsPane; };

	public int getPanelNumber() { return panelNumber; }

	public void setPanelLogarithmicHorizontally(boolean logarithmic)
	{
		horizRulerPanelLogarithmic = logarithmic;
		horizRulerPanel.repaint();
	}

	public boolean isPanelLogarithmicHorizontally()
	{
		if (waveWindow.isXAxisLocked()) return waveWindow.isWaveWindowLogarithmic();
		return horizRulerPanelLogarithmic;
	}

	public boolean isPanelLogarithmicVertically() { return vertPanelLogarithmic; }

	public int getVertAxisPos() { return vertAxisPos; }

	public void setVertAxisPos(int x) { vertAxisPos = x; }

	public static Panel getCurrentPanel() { return curPanel; }

	public static int getCurrentXPos() { return curXPos; }

	public Dimension getSz() { return sz; }

	// ************************************* SIGNALS IN THE PANEL *************************************

	public void addSignal(WaveSignal sig, JButton but)
	{
		waveSignals.put(but, sig);
		updatePanelTitle();
	}

	/**
	 * Method to change the title of the panel depending on the contents and size.
	 */
	public void updatePanelTitle()
	{
		String panelTitleName = "" + panelNumber;
		if (waveSignals.size() != 1)
		{
			if (waveSignals.size() != 0) panelTitleName += ": (" + waveSignals.size() + " SIGNALS)"; else
				panelTitleName += ": (NO SIGNALS)";
		} else
		{
			boolean signalsVisible = false;
			JTable table = waveWindow.getWaveformTable();
			for(int i=0; i<table.getRowCount(); i++)
			{
				if (this == waveWindow.getPanel(i))
				{
					int rowHeight = table.getRowHeight(i);
					if (rowHeight >= 55) signalsVisible = true;
				}
			}
			if (!signalsVisible)
			{
				Collection<WaveSignal> justOneWS = waveSignals.values();
				WaveSignal ws = justOneWS.iterator().next();
				Signal<?> sig = ws.getSignal();
				panelTitleName += ": " + sig.getSignalName();
			}
		}
		panelTitle.setText(panelTitleName);
	}

	public void removeSignal(JButton but)
	{
		if (signalButtons != null) signalButtons.remove(but);
		waveSignals.remove(but);
		updatePanelTitle();
	}

	public void removeAllSignals()
	{
		waveSignals.clear();
		updatePanelTitle();
	}

	/**
	 * Method to return a List of WaveSignals in this panel.
	 * @return a List of WaveSignals in this panel.
	 */
	public List<WaveSignal> getSignals()
	{
		List<WaveSignal> signals = new ArrayList<WaveSignal>();
		for(JButton but : waveSignals.keySet())
		{
			WaveSignal ws = waveSignals.get(but);
			signals.add(ws);
		}
		return signals;
	}

	public int getNumSignals() { return waveSignals.size(); }

	public WaveSignal findWaveSignal(Signal<?> sig)
	{
		for(JButton but : waveSignals.keySet())
		{
			WaveSignal ws = waveSignals.get(but);
			if (ws.getSignal() == sig) return ws;
		}
		return null;
	}

	public WaveSignal findWaveSignal(JButton but)
	{
		WaveSignal sig = waveSignals.get(but);
		return sig;
	}

	public JButton findButton(WaveSignal ws)
	{
		for(JButton but : waveSignals.keySet())
		{
			WaveSignal oWs = waveSignals.get(but);
			if (oWs == ws) return but;
		}
		return null;
	}

	private void deleteSignalFromPanel()
	{
		waveWindow.deleteSignalFromPanel(this);
	}

	private void deleteAllSignalsFromPanel()
	{
		waveWindow.deleteAllSignalsFromPanel(this);
	}

	// ************************************* THE HORIZONTAL RULER *************************************

	public void addHorizRulerPanel()
	{
		horizRulerPanel = new HorizRuler(this, waveWindow);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 1;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		rightHalf.add(horizRulerPanel, gbc);
	}

	public void removeHorizRulerPanel()
	{
		rightHalf.remove(horizRulerPanel);
		horizRulerPanel = null;
	}

	public HorizRuler getHorizRuler() { return horizRulerPanel; }

	public Signal<?> getXAxisSignal() { return xAxisSignal; }

	public void setXAxisSignal(Signal<?> sig) { xAxisSignal = sig; }

	// ************************************* PANEL DISPLAY CONTROL *************************************

	public void hidePanel()
	{
		waveWindow.hidePanel(this);
	}

	public void closePanel()
	{
		waveWindow.closePanel(this);
		waveWindow.saveSignalOrder();
	}

	public void toggleBusContents()
	{
		// this panel must have one signal
		Collection<WaveSignal> theSignals = waveSignals.values();
		if (theSignals.size() != 1) return;

		// the only signal must be digital
		WaveSignal ws = theSignals.iterator().next();
		if (!(ws.getSignal().isDigital())) return;

		// the digital signal must be a bus
		Signal<DigitalSample> sDSig = (Signal<DigitalSample>)ws.getSignal();
		Signal<?>[] bussedSignals = sDSig.getBusMembers();
		if (bussedSignals == null) return;

		// see if any of the bussed signals are displayed
		boolean opened = false;
		for(Signal<?> subSig : bussedSignals)
		{
			WaveSignal subWs = waveWindow.findDisplayedSignal(subSig);
			if (subWs != null)
			{
				opened = true;
				break;
			}
		}

		// now open or close the bus
		if (opened)
		{
			// opened: remove all entries on the bus
			List<Panel> allPanels = new ArrayList<Panel>();
			for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
				allPanels.add(it.next());

			for(Signal<?> subSig : bussedSignals)
			{
				WaveSignal subWs = waveWindow.findDisplayedSignal(subSig);
				if (subWs != null)
				{
					Panel wp = subWs.getPanel();
					waveWindow.closePanel(wp);
					allPanels.remove(wp);
				}
			}
		} else
		{
			// closed: add all entries on the bus
			int increment = 1;
			waveWindow.stopEditing();
			for(Signal<?> subSig : bussedSignals)
			{
				Panel wp = waveWindow.makeNewPanel(-1);
				WaveSignal wsig = new WaveSignal(wp, subSig);

				// remove the panels and put them in the right place
				waveWindow.removePanel(wsig.getPanel());

				int destIndex = waveWindow.getPanelIndex(this);
				waveWindow.addPanel(wsig.getPanel(), destIndex+increment);
				increment++;
			}
			waveWindow.reloadTable();
		}
		waveWindow.validatePanel();
		waveWindow.saveSignalOrder();
	}

	// ************************************* X AND Y AXIS CONTROL *************************************

	/**
	 * Method to set the X axis range in this panel.
	 * Since the panel may go backwards in time, these values aren't
	 * guaranteed to run from left to right.
	 * @param leftEdge the X axis value on the left side of the panel.
	 * @param rightEdge the X axis value on the right side of the panel.
	 */
	public void setXAxisRange(double leftEdge, double rightEdge)
	{
		this.minXPosition = leftEdge;
		this.maxXPosition = rightEdge;
	}

	/**
	 * Method to return the low X axis value shown in this panel.
	 * @return the low X axis value shown in this panel.
	 */
	public double getMinXAxis() { return minXPosition; }

	/**
	 * Method to return the high X axis value shown in this panel.
	 * @return the high X axis value shown in this panel.
	 */
	public double getMaxXAxis() { return maxXPosition; }

	/**
	 * Method to make this Panel show a signal fully.
	 * @param sSig the signal to show or null to fit to all signals
	 */
	public void fitToSignal(Signal<?> sig)
	{
        double lowValue = Double.MAX_VALUE;
        double highValue = -Double.MAX_VALUE;
        for(WaveSignal wSig : getSignals())
        {
            Signal<Sample> sSig = (Signal<Sample>)wSig.getSignal();
            if (sig != null && sig != sSig) continue;
        	lowValue = Math.min(lowValue, sSig.getMinValue());
        	highValue = Math.max(highValue, sSig.getMaxValue());
//            if (sSig.isDigital())
//            {
//            	lowValue = Math.min(lowValue, 0);
//            	highValue = Math.max(highValue, 1);
//            } else
//            {
//            	Signal.View<RangeSample<Sample>> view = sSig.getRasterView(sSig.getMinTime(), sSig.getMaxTime(), 2);
//	            for(int i=0; i<view.getNumEvents(); i++)
//	            {
//	                RangeSample<?> rs = view.getSample(i);
//	                if (rs==null) continue;
//	                Sample min = rs.getMin();
//	                if (min != null)
//	                {
//	                	if (min instanceof ScalarSample) lowValue = Math.min(lowValue, ((ScalarSample)min).getValue()); else
//	                		if (min instanceof SweptSample<?>) lowValue = Math.min(lowValue, ((SweptSample<ScalarSample>)min).getMin());
//	                }
//	                Sample max = rs.getMax();
//	                if (max != null)
//	                {
//	                	if (max instanceof ScalarSample) highValue = Math.max(highValue, ((ScalarSample)max).getValue()); else
//	                		if (max instanceof SweptSample<?>) highValue = Math.max(highValue, ((SweptSample<ScalarSample>)max).getMax());
//	                }
//	            }
//            }
        }
        double range = highValue - lowValue;
        if (range == 0) range = 2;
        double rangeExtra = range / 10;
        setYAxisRange(lowValue - rangeExtra, highValue + rangeExtra);
        makeSelectedPanel(-1, -1);
        repaintWithRulers();
	}

	/**
	 * Method to set the Y axis range in this panel.
	 * @param low the low Y axis value.
	 * @param high the high Y axis value.
	 */
	public void setYAxisRange(double low, double high)
	{
		if (low == high)
		{
			low -= 0.5;
			high += 0.5;
		}
		analogLowValue = low;
		analogHighValue = high;
		analogRange = analogHighValue - analogLowValue;
	}

	public double getYAxisRange() { return analogRange; }

	public double getYAxisLowValue() { return analogLowValue; }

	public double getYAxisHighValue() { return analogHighValue; }

	/**
	 * Method to scale a simulation X value to the X coordinate in this window.
	 * @param value the simulation X value.
	 * @return the X coordinate of that simulation value on the screen.
	 */
	public int convertXDataToScreen(double value)
	{
		// see if doing logarithmic axes
		boolean log = waveWindow.isWaveWindowLogarithmic();
		if (!waveWindow.isXAxisLocked()) log = horizRulerPanelLogarithmic;
		if (log)
		{
			// logarithmic axes
			if (value <= smallestXValue) value = smallestXValue;
			double logValue = Math.log10(value);
			double winMinX = minXPosition;
			if (winMinX <= 0) winMinX = smallestXValue;
			double logWinMinX = Math.log10(winMinX);
			double winMaxX = maxXPosition;
			if (winMaxX <= 0) winMaxX = smallestXValue;
			double logWinMaxX = Math.log10(winMaxX);
			double x = (logValue - logWinMinX) / (logWinMaxX - logWinMinX) * (sz.width - vertAxisPos) + vertAxisPos;
			return (int)x;
		}

		// linear axes
		double x = (value - minXPosition) / (maxXPosition - minXPosition) * (sz.width - vertAxisPos) + vertAxisPos;
		return (int)x;
	}

	/**
	 * Method to scale an X coordinate from screen space to data space.
	 * @param x the X coordinate on the screen.
	 * @return the X value in the simulation corresponding to that screen coordinate.
	 */
	public double convertXScreenToData(int x)
	{
		// see if doing logarithmic axes
		boolean log = waveWindow.isWaveWindowLogarithmic();
		if (!waveWindow.isXAxisLocked()) log = horizRulerPanelLogarithmic;
		if (log)
		{
			// logarithmic axes
			double winMinX = minXPosition;
			if (winMinX <= 0) winMinX = smallestXValue;
			double logWinMinX = Math.log10(winMinX);
			double winMaxX = maxXPosition;
			if (winMaxX <= 0) winMaxX = smallestXValue;
			double logWinMaxX = Math.log10(winMaxX);
			double xValue = Math.pow(10, ((double)(x - vertAxisPos)) / (sz.width - vertAxisPos) * (logWinMaxX - logWinMinX) + logWinMinX);
			return xValue;
		}

		// linear axes
		double xValue = ((double)(x - vertAxisPos)) / (sz.width - vertAxisPos) * (maxXPosition - minXPosition) + minXPosition;
		return xValue;
	}

	/**
	 * Method to scale a simulation Y value to the Y coordinate in this window.
	 * @param value the simulation Y value.
	 * @return the Y coordinate of that simulation value on the screen
	 */
	public int convertYDataToScreen(double value)
	{
		if (vertPanelLogarithmic)
		{
			// logarithmic axes
			if (value <= smallestYValue) value = smallestYValue;
			double logValue = Math.log10(value);
			double winMinY = analogLowValue;
			if (winMinY <= 0) winMinY = smallestYValue;
			double logWinMinY = Math.log10(winMinY);
			double winMaxY = analogHighValue;
			if (winMaxY <= 0) winMaxY = smallestYValue;
			double logWinMaxY = Math.log10(winMaxY);
			double y = sz.height - 1 - (logValue - logWinMinY) / (logWinMaxY - logWinMinY) * (sz.height-1);
			return (int)y;
		}

		// linear axes
		double y = sz.height - 1 - (value - analogLowValue) / analogRange * (sz.height-1);
		return (int)y;
	}

	/**
	 * Method to scale a Y coordinate from screen space to data space.
	 * @param y the Y coordinate on the screen.
	 * @return the Y value in the simulation corresponding to that screen coordinate.
	 */
	public double convertYScreenToData(int y)
	{
		if (vertPanelLogarithmic)
		{
			// logarithmic axes
			double winMinY = analogLowValue;
			if (winMinY <= 0) winMinY = smallestYValue;
			double logWinMinY = Math.log10(winMinY);
			double winMaxY = analogHighValue;
			if (winMaxY <= 0) winMaxY = smallestYValue;
			double logWinMaxY = Math.log10(winMaxY);
			double yValue = Math.pow(10, logWinMinY - (y - sz.height + 1) * (logWinMaxY - logWinMinY) / (sz.height-1));
			return yValue;
		}

		// linear axes
		double value = 0;
		if (sz.height > 1) value = analogLowValue - (y - sz.height + 1) * analogRange / (sz.height-1);
		return value;
	}

	// ************************************* DISPLAY CONTROL *************************************

	/**
	 * Method to repaint this window and its associated ruler panel.
	 */
	public void repaintWithRulers()
	{
		if (horizRulerPanel != null) horizRulerPanel.repaint(); else
		{
			waveWindow.getMainHorizRuler().repaint();
		}
		repaintContents();
	}

	/**
	 * Method to repaint the panel.
	 * Rebuilds the offscreen image and schedules a repaint.
	 */
	public void repaintContents()
	{
		needRepaintOffscreenImage = true;
		waveWindow.getWaveformTable().repaint();
	}

	private boolean needRepaintOffscreenImage;
	private Image offscreen;

	public Image getWaveImage() { return offscreen; }

	/**
	 * Method to repaint this Panel.
	 */
	public void paint(Graphics g)
	{
		// requestFocus moved to mousePressed().
		// to enable keys to be received
		//if (waveWindow.getWindowFrame() == WindowFrame.getCurrentWindowFrame())
		//	requestFocus();

		sz = getSize();
		szValid = true;
		int wid = sz.width;
		int hei = sz.height;
		if (USE_VOLATILE_IMAGE) {
			VolatileImage offscreen = (VolatileImage)this.offscreen;
			do {
				int returnCode = VolatileImage.IMAGE_INCOMPATIBLE;
				if (offscreen != null && offscreen.getWidth() == wid && offscreen.getHeight() == hei)
					returnCode = offscreen.validate(getGraphicsConfiguration());
				if (returnCode == VolatileImage.IMAGE_INCOMPATIBLE)
				{
					// old offscreen doesn't work with new GraphicsConfig; re-create it
					if (offscreen != null)
						offscreen.flush();
					this.offscreen = offscreen = createVolatileImage(wid, hei);
					needRepaintOffscreenImage = true;
				}
				if (returnCode != VolatileImage.IMAGE_OK || needRepaintOffscreenImage)
				{
					// Contents need to be restored
					repaintOffscreenImage(wid, hei);
				}
				if (offscreen.contentsLost())
					continue;
				g.drawImage(offscreen, 0, 0, null);
			} while (offscreen.contentsLost());

		} else {
			BufferedImage offscreen = (BufferedImage)this.offscreen;
			if (offscreen == null || offscreen.getWidth() != wid || offscreen.getHeight() != hei) {
				this.offscreen = offscreen = new BufferedImage(wid, hei, BufferedImage.TYPE_INT_RGB);
				needRepaintOffscreenImage = true;
			}
			if (needRepaintOffscreenImage) {
				repaintOffscreenImage(wid, hei);
			}
			g.drawImage(offscreen, 0, 0, null);
		}
		Dimension tableSz = waveWindow.getWaveformTable().getSize();
		Point screenLoc = waveWindow.getWaveformTable().getLocationOnScreen();
		waveWindow.setScreenXSize(screenLoc.x + tableSz.width - wid, screenLoc.x + tableSz.width);

		paintDragging((Graphics2D)g, wid, hei);
	}

	private void repaintOffscreenImage(int wid, int hei) {
		needRepaintOffscreenImage = false;
		Graphics2D offscreenGraphics = (Graphics2D)offscreen.getGraphics();

		// clear the buffer
		offscreenGraphics.setColor(new Color(User.getColor(User.ColorPrefType.WAVE_BACKGROUND)));
		offscreenGraphics.fillRect(0, 0, wid, hei);

		drawPanelContents(wid, hei, offscreenGraphics, null, null);
		offscreenGraphics.dispose();
	}

	private void paintDragging(Graphics2D g, int wid, int hei) {

		g.setColor(new Color(User.getColor(User.ColorPrefType.WAVE_FOREGROUND)));

		// draw the X position cursors
		g.setStroke(Highlight.dashedLine);
		int x = convertXDataToScreen(waveWindow.getMainXPositionCursor());
		if (x >= vertAxisPos)
			g.drawLine(x, 0, x, hei);
		g.setStroke(farDottedLine);
		x = convertXDataToScreen(waveWindow.getExtensionXPositionCursor());
		if (x >= vertAxisPos)
			g.drawLine(x, 0, x, hei);
		g.setStroke(Highlight.solidLine);

		// show dragged area if there
		if (draggingArea)
		{
			int lowX = Math.min(convertXDataToScreen(dragStartXD), convertXDataToScreen(dragEndXD));
			int highX = Math.max(convertXDataToScreen(dragStartXD), convertXDataToScreen(dragEndXD));
			int lowY = Math.min(convertYDataToScreen(dragStartYD), convertYDataToScreen(dragEndYD));
			int highY = Math.max(convertYDataToScreen(dragStartYD), convertYDataToScreen(dragEndYD));
			g.drawLine(lowX, lowY, lowX, highY);
			g.drawLine(lowX, highY, highX, highY);
			g.drawLine(highX, highY, highX, lowY);
			g.drawLine(highX, lowY, lowX, lowY);
		}
		for(Rectangle2D meas : measurementList)
		{
			int lowX = Math.min(convertXDataToScreen(meas.getMinX()), convertXDataToScreen(meas.getMaxX()));
			int highX = Math.max(convertXDataToScreen(meas.getMinX()), convertXDataToScreen(meas.getMaxX()));
			int lowY = Math.min(convertYDataToScreen(meas.getMinY()), convertYDataToScreen(meas.getMaxY()));
			int highY = Math.max(convertYDataToScreen(meas.getMinY()), convertYDataToScreen(meas.getMaxY()));
			g.drawLine(lowX, lowY, lowX, highY);
			g.drawLine(lowX, highY, highX, highY);
			g.drawLine(highX, highY, highX, lowY);
			g.drawLine(highX, lowY, lowX, lowY);

			// show dimensions while dragging
			double lowXValue = convertXScreenToData(lowX);
			double highXValue = convertXScreenToData(highX);
			double lowValue = convertYScreenToData(highY);
			double highValue = convertYScreenToData(lowY);
			g.setFont(waveWindow.getFont());

			// show the low X value and arrow
			String lowXValueString = TextUtils.convertToEngineeringNotation(lowXValue, "s");
			GlyphVector gv = waveWindow.getFont().createGlyphVector(waveWindow.getFontRenderContext(), lowXValueString);
			Rectangle2D glyphBounds = gv.getLogicalBounds();
			int textWid = (int)glyphBounds.getWidth();
			int textHei = (int)glyphBounds.getHeight();
			int textY = (lowY+highY)/2;
			g.drawString(lowXValueString, lowX-textWid-6, textY+textHei/2-10);
			g.drawLine(lowX-1, textY, lowX-textWid, textY);
			g.drawLine(lowX-1, textY, lowX-6, textY+4);
			g.drawLine(lowX-1, textY, lowX-6, textY-4);

			// show the high X value and arrow
			String highXValueString = TextUtils.convertToEngineeringNotation(highXValue, "s");
			gv = waveWindow.getFont().createGlyphVector(waveWindow.getFontRenderContext(), highXValueString);
			glyphBounds = gv.getLogicalBounds();
			textWid = (int)glyphBounds.getWidth();
			textHei = (int)glyphBounds.getHeight();
			int highXValueTextWid = textWid;
			g.drawString(highXValueString, highX+6, textY+textHei/2-10);
			g.drawLine(highX+1, textY, highX+textWid, textY);
			g.drawLine(highX+1, textY, highX+6, textY+4);
			g.drawLine(highX+1, textY, highX+6, textY-4);

			// show the difference X value
			String xDiffString = TextUtils.convertToEngineeringNotation(highXValue-lowXValue, "s");
			gv = waveWindow.getFont().createGlyphVector(waveWindow.getFontRenderContext(), xDiffString);
			glyphBounds = gv.getLogicalBounds();
			textWid = (int)glyphBounds.getWidth();
			textHei = (int)glyphBounds.getHeight();
			if (textWid + 24 < highX - lowX)
			{
				// fits inside: draw arrows around text
				int yPosText = highY + textHei*5;
				int yPos = yPosText - textHei/2;
				int xCtr = (highX+lowX)/2;
				g.drawString(xDiffString, xCtr - textWid/2, yPosText);
				g.drawLine(lowX, yPos, xCtr - textWid/2 - 2, yPos);
				g.drawLine(highX, yPos, xCtr + textWid/2 + 2, yPos);
				g.drawLine(lowX, yPos, lowX+5, yPos+4);
				g.drawLine(lowX, yPos, lowX+5, yPos-4);
				g.drawLine(highX, yPos, highX-5, yPos+4);
				g.drawLine(highX, yPos, highX-5, yPos-4);
			} else
			{
				// does not fit inside: draw outside of arrows
				int yPosText = highY + textHei*5;
				int yPos = yPosText - textHei/2;
				g.drawString(xDiffString, highX + 12, yPosText);
				g.drawLine(lowX, yPos, lowX-10, yPos);
				g.drawLine(highX, yPos, highX+10, yPos);
				g.drawLine(lowX, yPos, lowX-5, yPos+4);
				g.drawLine(lowX, yPos, lowX-5, yPos-4);
				g.drawLine(highX, yPos, highX+5, yPos+4);
				g.drawLine(highX, yPos, highX+5, yPos-4);
			}

			if (isAnalog())
			{
				// show the low value
				String lowValueString = TextUtils.convertToEngineeringNotation(highValue, null);
				gv = waveWindow.getFont().createGlyphVector(waveWindow.getFontRenderContext(), lowValueString);
				glyphBounds = gv.getLogicalBounds();
				textWid = (int)glyphBounds.getWidth();
				textHei = (int)glyphBounds.getHeight();
				int xP = (lowX+highX)/2;
				int yText = lowY - 10 - textHei;
				g.drawString(lowValueString, xP, yText - 2);
				g.drawLine(xP, lowY-1, xP, yText);
				g.drawLine(xP, lowY-1, xP+4, lowY-5);
				g.drawLine(xP, lowY-1, xP-4, lowY-5);

				// show the high value
				String highValueString = TextUtils.convertToEngineeringNotation(lowValue, null);
				gv = waveWindow.getFont().createGlyphVector(waveWindow.getFontRenderContext(), highValueString);
				glyphBounds = gv.getLogicalBounds();
				textWid = (int)glyphBounds.getWidth();
				textHei = (int)glyphBounds.getHeight();
				yText = highY + 10 + textHei;
				g.drawString(highValueString, xP, yText + textHei + 2);
				g.drawLine(xP, highY+1, xP, yText);
				g.drawLine(xP, highY+1, xP+4, highY+5);
				g.drawLine(xP, highY+1, xP-4, highY+5);

				// show the value difference
				String valueDiffString = TextUtils.convertToEngineeringNotation(highValue - lowValue, null);
				gv = waveWindow.getFont().createGlyphVector(waveWindow.getFontRenderContext(), valueDiffString);
				glyphBounds = gv.getLogicalBounds();
				textWid = (int)glyphBounds.getWidth();
				textHei = (int)glyphBounds.getHeight();
				if (textHei + 12 < highY - lowY)
				{
					// fits inside: draw arrows around text
					int xPos = highX + highXValueTextWid + 30;
					int yCtr = (highY+lowY)/2;
					g.drawString(valueDiffString, xPos+2, yCtr + textHei/2);
					g.drawLine(xPos, lowY, xPos, highY);
					g.drawLine(xPos, lowY, xPos+4, lowY+5);
					g.drawLine(xPos, lowY, xPos-4, lowY+5);
					g.drawLine(xPos, highY, xPos+4, highY-5);
					g.drawLine(xPos, highY, xPos-4, highY-5);
				} else
				{
					// does not fit inside: draw outside of arrows
					int xPos = highX + highXValueTextWid + 30;
					g.drawString(valueDiffString, xPos+4, lowY - textHei/2 - 4);
					g.drawLine(xPos, lowY, xPos, lowY-10);
					g.drawLine(xPos, highY, xPos, highY+10);
					g.drawLine(xPos, lowY, xPos+4, lowY-5);
					g.drawLine(xPos, lowY, xPos-4, lowY-5);
					g.drawLine(xPos, highY, xPos+4, highY+5);
					g.drawLine(xPos, highY, xPos-4, highY+5);
				}
			}
		}
	}

	private void drawPanelContents(int wid, int hei, Graphics2D localGraphics, Rectangle2D bounds, List<PolyBase> polys)
	{
		// draw the grid first (behind the signals)
		if (waveWindow.isShowGrid())
		{
			if (localGraphics != null)
			{
				localGraphics.setStroke(Highlight.dottedLine);
				localGraphics.setColor(gridColor);
			}

			// draw the vertical grid lines
			double displayedXLow = convertXScreenToData(vertAxisPos);
			double displayedXHigh = convertXScreenToData(wid);
			StepSize ss = new StepSize(displayedXHigh, displayedXLow, 10);
			if (ss.getSeparation() != 0.0)
			{
				double value = ss.getLowValue();
				for(;;)
				{
					if (value >= displayedXLow)
					{
						if (value > ss.getHighValue()) break;
						int x = convertXDataToScreen(value);
						if (polys != null)
						{
							polys.add(new Poly(new Point2D[] {
								new Point2D.Double(x, 0),
								new Point2D.Double(x, hei)}));
						} else
						{
							localGraphics.drawLine(x, 0, x, hei);
						}
					}
					value += ss.getSeparation();
				}
			}

			ss = new StepSize(analogHighValue, analogLowValue, 5);
			if (ss.getSeparation() != 0.0)
			{
				double value = ss.getLowValue();
				for(;;)
				{
					if (value >= analogLowValue)
					{
						if (value > analogHighValue || value > ss.getHighValue()) break;
						int y = convertYDataToScreen(value);
						if (polys != null)
						{
							polys.add(new Poly(new Point2D[] {
								new Point2D.Double(vertAxisPos, y),
								new Point2D.Double(wid, y)}));
						} else
						{
							localGraphics.drawLine(vertAxisPos, y, wid, y);
						}
					}
					value += ss.getSeparation();
				}
			}
			if (localGraphics != null)
			{
				localGraphics.setStroke(Highlight.solidLine);
			}
		}

		// draw all of the signals
		if (USE_ANTIALIASING && localGraphics != null) {
			Object oldAntialiasing = localGraphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
			localGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			processSignals(localGraphics, bounds, polys);
			localGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
		} else {
			processSignals(localGraphics, bounds, polys);
		}

		// draw all of the control points
		if (localGraphics != null) processControlPoints(localGraphics, bounds);

		// draw the vertical label
		if (polys != null)
		{
			polys.add(new Poly(new Point2D[] {
				new Point2D.Double(vertAxisPos, 0),
				new Point2D.Double(vertAxisPos, hei)}));
		} else
		{
			localGraphics.setColor(new Color(User.getColor(User.ColorPrefType.WAVE_FOREGROUND)));
			localGraphics.drawLine(vertAxisPos, 0, vertAxisPos, hei);
			if (selected)
			{
				localGraphics.drawLine(vertAxisPos-1, 0, vertAxisPos-1, hei);
				localGraphics.drawLine(vertAxisPos-2, 0, vertAxisPos-2, hei-1);
				localGraphics.drawLine(vertAxisPos-3, 0, vertAxisPos-3, hei-2);
			}
		}
		if (isAnalog())
		{
			double displayedLow = convertYScreenToData(hei);
			double displayedHigh = convertYScreenToData(0);
			StepSize ss = new StepSize(displayedHigh, displayedLow, 5);
			if (ss.getSeparation() != 0.0)
			{
				double value = ss.getLowValue();
				if (localGraphics != null) localGraphics.setFont(waveWindow.getFont());
				int lastY = -1;
				int ySeparation = convertYDataToScreen(value) - convertYDataToScreen(value+ss.getSeparation());
				int textSkip = 100;
				if (ySeparation > 0) textSkip = 20 / ySeparation;
				int textSkipPos = 0;
				for(int i=0; ; i++)
				{
					if (value > displayedHigh) break;
					if (value >= displayedLow)
					{
						int y = convertYDataToScreen(value);
						if (lastY >= 0)
						{
							// add extra tick marks
							int addedTicks = (lastY - y) / 20;
							for(int j=1; j<addedTicks; j++)
							{
								int intY = (lastY - y) / addedTicks * j + y;
								if (polys != null)
								{
									polys.add(new Poly(new Point2D[] {
										new Point2D.Double(vertAxisPos-5, intY),
										new Point2D.Double(vertAxisPos, intY)}));
								} else
								{
									localGraphics.drawLine(vertAxisPos-5, intY, vertAxisPos, intY);
								}
							}
						}

						if (polys != null)
						{
							polys.add(new Poly(new Point2D[] {
								new Point2D.Double(vertAxisPos-10, y),
								new Point2D.Double(vertAxisPos, y)}));
						} else
						{
							localGraphics.drawLine(vertAxisPos-10, y, vertAxisPos, y);
						}

						// skip text if spaced too closely
						textSkipPos--;
						if (textSkipPos <= 0)
						{
							textSkipPos = textSkip;
							String yValue = TextUtils.convertToEngineeringNotation(value, null, ss.getStepScale()+3);
							if (polys != null)
							{
								Poly poly = new Poly(new Point2D[] { new Point2D.Double(vertAxisPos-12, y) });
								poly.setStyle(Poly.Type.TEXTRIGHT);
								poly.setTextDescriptor(TextDescriptor.EMPTY.withAbsSize(6));
								poly.setString(yValue);
								polys.add(poly);
							} else
							{
								GlyphVector gv = waveWindow.getFont().createGlyphVector(waveWindow.getFontRenderContext(), yValue);
								Rectangle2D glyphBounds = gv.getLogicalBounds();
								int height = (int)glyphBounds.getHeight();
								int yPos = y + height / 2;
								if (yPos-height <= 0) yPos = height+1;
								if (yPos >= hei) yPos = hei;
								int xPos = vertAxisPos-10-(int)glyphBounds.getWidth()-2;
								if (xPos < 0) xPos = 0;
								localGraphics.drawString(yValue, xPos, yPos);
							}
						}
						lastY = y;
					}
					value += ss.getSeparation();
				}
			}
		}
	}

	public void dumpDataCSV(PrintWriter pw)
	{
		for(WaveSignal ws : waveSignals.values())
		{
            Signal<?> as = ws.getSignal();
            Signal.View<?> waveform = ((Signal<?>)as).getExactView();
            int numEvents = waveform.getNumEvents();
            for(int i=0; i<numEvents; i++)
            {
        		Sample samp = waveform.getSample(i);
        		double time = waveform.getTime(i);
			    if (samp instanceof SweptSample<?>)
			    {
			    	SweptSample<?> sws = (SweptSample<?>)samp;
			    	for(int s=0; s<sws.getWidth(); s++)
			    	{
			    		Sample ss = sws.getSweep(s);
                        pw.println("\"" + time + "\",\"" + s + "\",\"" + ((ScalarSample)ss).getValue() + "\"");
			    	}
			    } else if (samp instanceof DigitalSample)
			    {
			    	DigitalSample ds = (DigitalSample)samp;
			    	String value;
			        if (ds.isLogic0()) value = "0"; else
			        if (ds.isLogic1()) value = "1"; else
			        if (ds.isLogicX()) value = "X"; else
			        if (ds.isLogicZ()) value = "Z"; else
			        	value = "?";
                    pw.println("\"" + time + "\",\"" + value + "\"");
			    } else if (samp instanceof BusSample<?>)
			    {
			    	BusSample<?> bs = (BusSample<?>)samp;
			    	boolean isX = false, isZ = false;
			    	StringBuffer sb = new StringBuffer();
			    	for(int j=0; j<bs.getWidth(); j++)
			    	{
			    		DigitalSample ds = (DigitalSample)bs.getTrace(j);
				        if (ds.isLogicX()) { isX = true;   break; }
				        if (ds.isLogicZ()) { isZ = true;   break; }
				        if (ds.isLogic0()) sb.append("0"); else
					        if (ds.isLogic1()) sb.append("1"); else
					        	sb.append("?");
			    	}
			    	String number = sb.toString();
			    	if (isX) number = "X"; else
			    		if (isZ) number = "Z";
                    pw.println("\"" + time + "\",\"" + number + "\"");
			    } else
			    {
			    	ScalarSample ss = (ScalarSample)samp;
                    pw.println("\"" + time + "\",\"" + ss.getValue() + "\"");
			    }
            }
            pw.println();
        }
    }

    private static String pad2(String s) { return s.length()>=2 ? s : pad2("0"+s); }
	void dumpDataForGnuplot(PrintWriter pw) { dumpDataForGnuplot(pw, -Double.MAX_VALUE, Double.MAX_VALUE, ""); }
	void dumpDataForGnuplot(PrintWriter pw, double min, double max, String sep) {
        boolean first = true;
        int linetype = 1;
		for(WaveSignal ws : waveSignals.values())
		{
            boolean used = false;
            String [] sweepNames = ws.getSignal().getSignalCollection().getSweepNames();
            int numSweeps = (sweepNames == null) ? 1 : sweepNames.length;
            for (int s = 0; s < numSweeps; s++)
            {
                if (!first) pw.print(sep);
                pw.print(" \'-\' with lines ");
                Color c = ws.getColor();
                pw.print(" lt "+linetype+" ");
                pw.print("lc rgb \"#"+
                         pad2(Integer.toString(c.getRed()   & 0xff, 16))+
                         pad2(Integer.toString(c.getGreen() & 0xff, 16))+
                         pad2(Integer.toString(c.getBlue()  & 0xff, 16))+
                         "\" ");
                pw.print(" title \""+ws.getSignal().getFullName()+"\" ");
                first = false;
                used = true;
            }
            if (used) linetype++;
        }
		for(WaveSignal ws : waveSignals.values())
		{
            Signal<?> as = ws.getSignal();
            String [] sweepNames = ws.getSignal().getSignalCollection().getSweepNames();
            int numSweeps = (sweepNames == null) ? 1 : sweepNames.length;
            Signal.View<?> waveform = ((Signal<?>)as).getExactView();
            int numEvents = waveform.getNumEvents();
            for (int s = 0; s < numSweeps; s++)
            {
                pw.println();
                for(int i=0; i<numEvents; i++)
                {
            		Sample samp = waveform.getSample(i);
            		double time = waveform.getTime(i);
                    if (time < min || time > max) continue;
    			    if (samp instanceof SweptSample<?>)
    			    {
    			    	SweptSample<?> sws = (SweptSample<?>)samp;
			    		Sample ss = sws.getSweep(s);
                        pw.println(time + " " + ss);
    			    } else
    			    {
                        pw.println(time + " " + samp);
    			    }
                }
                pw.println("e");
                pw.println();
            }
        }
    }

	private List<WaveSelection> processSignals(Graphics g, Rectangle2D bounds, List<PolyBase> forPs)
	{
		List<WaveSelection> selectedObjects = null;
		if (bounds != null) selectedObjects = new ArrayList<WaveSelection>();
		Signal<?> xSignal = xAxisSignal;
		if (waveWindow.isXAxisLocked()) xSignal = waveWindow.getXAxisSignalAll();
		Collection<WaveSignal> sigs = waveSignals.values();
		int sigIndex = 0;
        Color light = null;
		for(WaveSignal ws : sigs)
		{
			if (g != null)
			{
				if (waveWindow.getPrintingMode() == 2) g.setColor(Color.BLACK); else
					g.setColor(ws.getColor());
                Color c = ws.getColor();
                light = new Color(c.getRed(), c.getGreen(), c.getBlue(), 0x55);
			}

			if (forPs != null)
			{
                int hei = sz.height;
				double yPos = hei / 2;
				Poly.Type style = Poly.Type.TEXTRIGHT;
				if (sigs.size() > 1)
				{
					if (sigIndex == sigs.size()-1) style = Poly.Type.TEXTBOTRIGHT; else
						if (sigIndex == 0) style = Poly.Type.TEXTTOPRIGHT;
					yPos = ((double)(hei * sigIndex)) / (sigs.size()-1);
				}
				Poly poly = new Poly(new Point2D[] {new Point2D.Double(0, yPos)});
				poly.setStyle(style);
				poly.setTextDescriptor(TextDescriptor.EMPTY.withAbsSize(12));
				poly.setString(ws.getSignal().getFullName());
				forPs.add(poly);
			}
			sigIndex++;
            ws.getSignal().plot(this, g, ws, light, forPs, bounds, selectedObjects, xSignal);
		}
		return selectedObjects;
	}

	private List<WaveSelection> processControlPoints(Graphics g, Rectangle2D bounds)
	{
		List<WaveSelection> selectedObjects = null;
		if (bounds != null) selectedObjects = new ArrayList<WaveSelection>();

		// show control points
		for(WaveSignal ws : waveSignals.values())
		{
			if (g != null) g.setColor(ws.getColor());

			Double [] points = ws.getSignal().getControlPoints();
			if (points == null) continue;
			if (g != null) g.setColor(ws.getColor());
			for(int i=0; i<points.length; i++)
			{
				double xValue = points[i].doubleValue();
				int x = convertXDataToScreen(xValue);
				if (processABox(g, x-CONTROLPOINTSIZE, sz.height-CONTROLPOINTSIZE*2, x+CONTROLPOINTSIZE, sz.height,
					bounds, null, selectedObjects, ws, true, xValue)) break;

				// see if the control point is selected
				boolean found = false;
				if (bounds == null && ws.getSelectedControlPoints() != null)
				{
					for(int j=0; j<ws.getSelectedControlPoints().length; j++)
						if (ws.getSelectedControlPoints()[j] == xValue) { found = true;   break; }
				}
				if (found)
				{
					g.setColor(Color.GREEN);
					if (processABox(g, x-CONTROLPOINTSIZE+2, sz.height-CONTROLPOINTSIZE*2+2, x+CONTROLPOINTSIZE-2, sz.height-2,
						bounds, null, selectedObjects, ws, true, xValue)) break;
					g.setColor(ws.getColor());
				}
			}
		}
		return selectedObjects;
	}

	public boolean processABox(Graphics g, int lX, int lY, int hX, int hY, Rectangle2D bounds, List<PolyBase> forPs,
		List<WaveSelection> result, WaveSignal ws, boolean controlPoint, double controlXValue)
	{
		// bounds is non-null if doing hit-testing
		if (bounds != null)
		{
			// do bounds checking for hit testing
			if (hX > bounds.getMinX() && lX < bounds.getMaxX() && hY > bounds.getMinY() && lY < bounds.getMaxY())
			{
				if (forPs != null)
				{
					PolyBase poly = new PolyBase((lX+hX)/2, (lY+hY)/2, hX-lX, hY-lY);
					poly.setStyle(Poly.Type.FILLED);
					poly.setLayer(Artwork.tech().defaultLayer);
					forPs.add(poly);
					return false;
				}

				WaveSelection wSel = new WaveSelection();
				wSel.ws = ws;
				wSel.controlPoint = controlPoint;
				wSel.controlXValue = controlXValue;
				result.add(wSel);
				return true;
			}
			return false;
		}

		// clip to left edge
		if (hX <= vertAxisPos) return false;
		if (lX < vertAxisPos) lX = vertAxisPos;

		// not doing hit-testing, just doing drawing
		g.fillRect(lX, lY, hX-lX, hY-lY);
		return false;
	}

	public boolean processALine(Graphics g, int fX, int fY, int tX, int tY, Rectangle2D bounds,
		List<PolyBase> forPs, List<WaveSelection> result, WaveSignal ws, int sweepNum)
	{
		if (bounds != null)
		{
			// do bounds checking for hit testing
			Point2D from = new Point2D.Double(fX, fY);
			Point2D to = new Point2D.Double(tX, tY);
			if (!GenMath.clipLine(from, to, bounds.getMinX(), bounds.getMaxX(), bounds.getMinY(), bounds.getMaxY()))
			{
				if (forPs != null)
				{
					forPs.add(new PolyBase(new Point2D[] {from, to}));
					return false;
				}

				WaveSelection wSel = new WaveSelection();
				wSel.ws = ws;
				wSel.controlPoint = false;
				result.add(wSel);
				return true;
			}
			return false;
		}

		// clip to left edge
		if (fX < vertAxisPos || tX < vertAxisPos)
		{
			Point2D from = new Point2D.Double(fX, fY);
			Point2D to = new Point2D.Double(tX, tY);
			if (GenMath.clipLine(from, to, vertAxisPos, sz.width, 0, sz.height)) return false;
			fX = (int)from.getX();
			fY = (int)from.getY();
			tX = (int)to.getX();
			tY = (int)to.getY();
		}

		// draw the line
		g.drawLine(fX, fY, tX, tY);

		// highlight the line if requested
		boolean highlighted = ws.isHighlighted();
		if (ws.getPanel().waveWindow.getHighlightedSweep() >= 0)
		{
			highlighted = ws.getPanel().waveWindow.getHighlightedSweep() == sweepNum;
		}
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
		return false;
	}

	// ************************************* SIGNAL SELECTION *************************************

	/**
	 * the MouseListener events
	 */
	public void mousePressed(MouseEvent evt)
	{
		requestFocus();
		waveWindow.vcrClickStop();

		// set this to be the selected panel
		makeSelectedPanel(evt.getX(), evt.getY());

		// reset dragging from last time
		for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
		{
			Panel wp = it.next();
			if (wp.draggingArea) wp.repaintContents();
			wp.draggingArea = false;
		}

		if (evt.getClickCount() == 2 && evt.getX() < vertAxisPos)
		{
			new WaveformZoom(TopLevel.getCurrentJFrame(), analogLowValue, analogHighValue, minXPosition, maxXPosition, waveWindow, this);
			return;
		}
		ToolBar.CursorMode mode = ToolBar.getCursorMode();
		if (ClickZoomWireListener.isRightMouse(evt))
		{
			if ((evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) != 0) mode = ToolBar.CursorMode.ZOOM; else
			{
				if (evt.getX() < vertAxisPos)
				{
					// right click in ruler area: show popup of choices
					JPopupMenu menu = new JPopupMenu();
					JMenuItem item = new JMenuItem("Linear");
					item.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeLinear(); } });
					menu.add(item);
					item = new JMenuItem("Logarithmic");
					item.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeLogarithmic(); } });
					menu.add(item);
					menu.show(this, evt.getX(), evt.getY());
					return;
				}
			}
		}
		for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
			it.next().curMeasurement = null;
		if (mode == ToolBar.CursorMode.ZOOM) mousePressedZoom(evt); else
			if (mode == ToolBar.CursorMode.PAN) mousePressedPan(evt); else
				mousePressedSelect(evt);
	}

	public void mouseReleased(MouseEvent evt)
	{
		ToolBar.CursorMode mode = ToolBar.getCursorMode();
		if (ClickZoomWireListener.isRightMouse(evt) && (evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) != 0)
			mode = ToolBar.CursorMode.ZOOM;
		if (mode == ToolBar.CursorMode.ZOOM) mouseReleasedZoom(evt); else
			if (mode == ToolBar.CursorMode.PAN) mouseReleasedPan(evt); else
				mouseReleasedSelect(evt);

		for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
		{
			Panel panel = it.next();
			panel.curMeasurement = null;
			if (mode == ToolBar.CursorMode.MEASURE) panel.draggingArea = false;
		}
	}

	public void mouseClicked(MouseEvent evt) {}

	public void mouseEntered(MouseEvent evt) { curPanel = this;   curXPos = evt.getX(); }

	public void mouseExited(MouseEvent evt) { curPanel = null; }

	/**
	 * the MouseMotionListener events
	 */
	public void mouseMoved(MouseEvent evt)
	{
		curXPos = evt.getX();
		ToolBar.CursorMode mode = ToolBar.getCursorMode();
		if (mode == ToolBar.CursorMode.ZOOM) mouseMovedZoom(evt);
        else if (mode == ToolBar.CursorMode.PAN) mouseMovedPan(evt);
        else mouseMovedSelect(evt);
	}

	public void mouseDragged(MouseEvent evt)
	{
		curXPos = evt.getX();
		ToolBar.CursorMode mode = ToolBar.getCursorMode();
		if (ClickZoomWireListener.isRightMouse(evt) && (evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) != 0)
			mode = ToolBar.CursorMode.ZOOM;
		if (mode == ToolBar.CursorMode.ZOOM) mouseDraggedZoom(evt);
        else if (mode == ToolBar.CursorMode.PAN) mouseDraggedPan(evt);
        else mouseDraggedSelect(evt);
	}

	/**
	 * the MouseWheelListener events
	 */
	public void mouseWheelMoved(MouseWheelEvent evt) {}

	/**
	 * the KeyListener events
	 */
	public void keyPressed(KeyEvent evt)
	{
		waveWindow.vcrClickStop();
	}
	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

	private void panelTitleClicked(ActionEvent evt)
	{
		long delay = evt.getWhen() - lastClick;
		lastClick = evt.getWhen();
		if (delay < TopLevel.getDoubleClickSpeed())
		{
			toggleBusContents();
			return;
		}

		Set<JButton> set = waveSignals.keySet();
		if (set.size() != 1) return;
		JButton but = set.iterator().next();
		WaveSignal ws = waveSignals.get(but);

		if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0)
		{
			// standard click: add this as the only trace
			for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
			{
				Panel wp = it.next();
				wp.clearHighlightedSignals();
			}
			addHighlightedSignal(ws, true);
			makeSelectedPanel(-1, -1);
		} else
		{
			// TODO: this never gets called
			// shift click: add or remove to list of highlighted traces
			if (ws.isHighlighted()) removeHighlightedSignal(ws, true); else
				addHighlightedSignal(ws, true);
		}

		// show it in the schematic
		waveWindow.crossProbeWaveformToEditWindow();
	}

	/**
	 * Method to find the Signals in an area.
	 * @param lX the low X coordinate of the area.
	 * @param hX the high X coordinate of the area.
	 * @param lY the low Y coordinate of the area.
	 * @param hY the high Y coordinate of the area.
	 * @return a list of WaveSelection objects.
	 */
	private List<WaveSelection> findSignalsInArea(int lX, int hX, int lY, int hY)
	{
		double lXd = Math.min(lX, hX)-2;
		double hXd = Math.max(lX, hX)+2;
		double hYd = Math.min(lY, hY)-2;
		double lYd = Math.max(lY, hY)+2;
		if (lXd > hXd) { double swap = lXd;   lXd = hXd;   hXd = swap; }
		if (lYd > hYd) { double swap = lYd;   lYd = hYd;   hYd = swap; }
		Rectangle2D bounds = new Rectangle2D.Double(lXd, lYd, hXd-lXd, hYd-lYd);
		List<WaveSelection> sigs = processSignals(null, bounds, null);
		List<WaveSelection> cps = processControlPoints(null, bounds);
		for(WaveSelection ws : sigs)
			cps.add(ws);
		return cps;
	}

	/**
	 * Method to find a list of PolyBase objects that describe Signals in this panel.
	 * @return a list of PolyBase objects.
	 */
	public List<PolyBase> getPolysForPrinting()
	{
		if (!szValid)
		{
			for(Iterator<Panel> it = this.waveWindow.getPanels(); it.hasNext(); )
			{
				Panel wp = it.next();
				if (wp.szValid)
				{
					sz = wp.sz;
					szValid = true;
					break;
				}
			}
		}
		sz = getSize();
		List<PolyBase> polys = new ArrayList<PolyBase>();
		drawPanelContents(sz.width, sz.height, null, new Rectangle2D.Double(vertAxisPos, 0, sz.width, sz.height), polys);
		return polys;
	}

	public static class WaveSelection
	{
		/** Selected signal in Waveform Window */		WaveSignal ws;
		/** true if this is a control point */			boolean    controlPoint;
		/** X value of the control point (if a CP) */	double     controlXValue;
	}

	/**
	 * Method to remove all displayed measurements from the panel
	 */
	public void clearMeasurements()
	{
		measurementList.clear();
		curMeasurement = null;
		repaintContents();
	}

	/**
	 * Method to implement the Mouse Pressed event for selection.
	 */
	public void mousePressedSelect(MouseEvent evt)
	{
		// see if the horizontal cursors are selected
		draggingMain = draggingExt = draggingVertAxis = false;
		int mainX = convertXDataToScreen(waveWindow.getMainXPositionCursor());
		if (Math.abs(mainX - evt.getX()) < 5)
		{
			draggingMain = true;
			return;
		}
		int extX = convertXDataToScreen(waveWindow.getExtensionXPositionCursor());
		if (Math.abs(extX - evt.getX()) < 5)
		{
			draggingExt = true;
			return;
		}
		if (Math.abs(vertAxisPos - evt.getX()) < 5)
		{
			draggingVertAxis = true;
			return;
		}

		// drag area
		Point pt = new Point(evt.getX(), evt.getY());
		if (ToolBar.getCursorMode() == ToolBar.CursorMode.MEASURE)
		{
			pt = snapPoint(pt);
		}
		double xV = convertXScreenToData(pt.x);
		double yV = convertYScreenToData(pt.y);
		if (ToolBar.getCursorMode() == ToolBar.CursorMode.MEASURE)
		{
			if (ClickZoomWireListener.isRightMouse(evt))
			{
				measurementList.clear();
				curMeasurement = null;
				return;
			}
			curMeasurement = new Rectangle2D.Double(xV, yV, 0, 0);
			measurementList.add(curMeasurement);
		}
		dragEndXD = dragStartXD = xV;
		dragEndYD = dragStartYD = yV;
		draggingArea = true;
	}

	private Point getPointIfClose(int x, int y, Point pt)
	{
		if (Math.abs(x - pt.x) < 8 && Math.abs(y - pt.y) < 8)
		{
			pt.x = x;   pt.y = y;
			return pt;
		}
		return null;
	}

	private Point getPointIfCloseToLine(Point2D lstPt, Point2D thisPt, Point2D snap)
	{
		Point2D closest = GenMath.closestPointToSegment(lstPt, thisPt, snap);
		if (closest.distance(snap) < 5)
		{
			Point pt = new Point((int)Math.round(closest.getX()), (int)Math.round(closest.getY()));
            return pt;
		}
		return null;
	}

	private Point snapPoint(Point pt)
	{
		// get list of views in this panel
		List<Signal.View<RangeSample<Sample>>> allViews = new ArrayList<Signal.View<RangeSample<Sample>>>();
		for(WaveSignal ws : waveSignals.values())
		{
			Signal<Sample> sig = (Signal<Sample>)ws.getSignal();
			if (sig.isDigital()) continue;
			allViews.add(sig.getRasterView(sig.getMinTime(), sig.getMaxTime(), sz.width));
		}

		// snap to any waveform points
		for(Signal.View<RangeSample<Sample>> view : allViews)
		{
            for(int i=0; i<view.getNumEvents(); i++)
            {
                RangeSample<?> rs = view.getSample(i);
                if (rs == null) continue;
        		int x = convertXDataToScreen(view.getTime(i));

        		// see if point on minimum is close
        		Sample min = rs.getMin();
			    if (min instanceof SweptSample<?>)
			    {
			    	SweptSample<?> ss = (SweptSample<?>)min;
			    	for(int s=0; s<ss.getWidth(); s++)
			    	{
			    		Sample sweepSample = ss.getSweep(s);
			    		int y = convertYDataToScreen(((ScalarSample)sweepSample).getValue());
			    		Point closePoint = getPointIfClose(x, y, pt);
			    		if (closePoint != null) return closePoint;
			    	}
			    } else
			    {
			    	int y = convertYDataToScreen(((ScalarSample)min).getValue());
		    		Point closePoint = getPointIfClose(x, y, pt);
		    		if (closePoint != null) return closePoint;
			    }

        		// see if point on maximum is close
        		Sample max = rs.getMax();
			    if (max instanceof SweptSample<?>)
			    {
			    	SweptSample<?> ss = (SweptSample<?>)max;
			    	for(int s=0; s<ss.getWidth(); s++)
			    	{
			    		Sample sweepSample = ss.getSweep(s);
			    		int y = convertYDataToScreen(((ScalarSample)sweepSample).getValue());
			    		Point closePoint = getPointIfClose(x, y, pt);
			    		if (closePoint != null) return closePoint;
			    	}
			    } else
			    {
			    	int y = convertYDataToScreen(((ScalarSample)max).getValue());
		    		Point closePoint = getPointIfClose(x, y, pt);
		    		if (closePoint != null) return closePoint;
			    }
            }
		}

		// snap to any waveform lines
		Point2D snap = new Point2D.Double(pt.x, pt.y);
		for(Signal.View<RangeSample<Sample>> view : allViews)
		{
			Point2D lastPt = null;
			List<Point2D> lastSweepMinPts = new ArrayList<Point2D>();
			List<Point2D> lastSweepMaxPts = new ArrayList<Point2D>();
            for(int i=0; i<view.getNumEvents(); i++)
            {
                RangeSample<?> rs = view.getSample(i);
                if (rs == null) continue;
        		int x = convertXDataToScreen(view.getTime(i));

        		// see if point on minimum is close
        		Sample min = rs.getMin();
			    if (min instanceof SweptSample<?>)
			    {
			    	SweptSample<?> ss = (SweptSample<?>)min;
			    	for(int s=0; s<ss.getWidth(); s++)
			    	{
			    		Sample sweepSample = ss.getSweep(s);
			    		int y = convertYDataToScreen(((ScalarSample)sweepSample).getValue());
						Point2D thisPt = new Point2D.Double(x, y);
			    		if (s < lastSweepMinPts.size())
			    		{
			    			Point2D lstPt = lastSweepMinPts.get(s);
			    			Point close = getPointIfCloseToLine(lstPt, thisPt, snap);
			    			if (close != null) return close;
			    		}
			    		while (lastSweepMinPts.size() <= s) lastSweepMinPts.add(null);
			    		lastSweepMinPts.set(s, thisPt);
			    	}
			    } else
			    {
			    	int y = convertYDataToScreen(((ScalarSample)min).getValue());
					Point2D thisPt = new Point2D.Double(x, y);
		    		if (lastPt != null)
		    		{
		    			Point close = getPointIfCloseToLine(lastPt, thisPt, snap);
		    			if (close != null) return close;
		    		}
		    		lastPt = thisPt;
			    }

        		// see if point on maximum is close
        		Sample max = rs.getMax();
			    if (max instanceof SweptSample<?>)
			    {
			    	SweptSample<?> ss = (SweptSample<?>)max;
			    	for(int s=0; s<ss.getWidth(); s++)
			    	{
			    		Sample sweepSample = ss.getSweep(s);
			    		int y = convertYDataToScreen(((ScalarSample)sweepSample).getValue());
						Point2D thisPt = new Point2D.Double(x, y);
			    		if (s < lastSweepMaxPts.size())
			    		{
			    			Point2D lstPt = lastSweepMaxPts.get(s);
			    			Point close = getPointIfCloseToLine(lstPt, thisPt, snap);
			    			if (close != null) return close;
			    		}
			    		while (lastSweepMaxPts.size() <= s) lastSweepMaxPts.add(null);
			    		lastSweepMaxPts.set(s, thisPt);
			    	}
			    } else
			    {
			    	int y = convertYDataToScreen(((ScalarSample)max).getValue());
					Point2D thisPt = new Point2D.Double(x, y);
		    		if (lastPt != null)
		    		{
		    			Point close = getPointIfCloseToLine(lastPt, thisPt, snap);
		    			if (close != null) return close;
		    		}
		    		lastPt = thisPt;
			    }
            }
		}

		// no snapping: return the original point
		return pt;
	}

	/**
	 * Method to implement the Mouse Released event for selection.
	 */
	public void mouseReleasedSelect(MouseEvent evt)
	{
		if (draggingArea)
		{
			draggingArea = false;
			Panel wp = (Panel)evt.getSource();
			if (ToolBar.getCursorMode() != ToolBar.CursorMode.MEASURE &&
				ToolBar.getSelectMode() == ToolBar.SelectMode.OBJECTS)
			{
				List<WaveSelection> selectedObjects = wp.findSignalsInArea(convertXDataToScreen(dragStartXD),
					convertXDataToScreen(dragEndXD), convertYDataToScreen(dragStartYD), convertYDataToScreen(dragEndYD));

				if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0)
				{
					// standard click: add this as the only trace
					clearHighlightedSignals();
					for(WaveSelection wSel : selectedObjects)
					{
						if (wSel.controlPoint)
						{
							wSel.ws.addSelectedControlPoint(wSel.controlXValue);
						}
						wp.addHighlightedSignal(wSel.ws, false);
					}
				} else
				{
					// shift click: add or remove to list of highlighted traces
					for(WaveSelection wSel : selectedObjects)
					{
						WaveSignal ws = wSel.ws;
						if (ws.isHighlighted())
						{
							if (wSel.controlPoint) ws.removeSelectedControlPoint(wSel.controlXValue);
							removeHighlightedSignal(ws, false);
						} else
						{
							if (wSel.controlPoint) ws.addSelectedControlPoint(wSel.controlXValue);
							wp.addHighlightedSignal(ws, false);
						}
					}
				}

				// show it in the schematic
				wp.waveWindow.crossProbeWaveformToEditWindow();
			} else
			{
				// just leave this highlight and show dimensions
			}
		}
		repaintContents();
	}

	/**
	 * Method to implement the Mouse Dragged event for selection.
	 */
	public void mouseDraggedSelect(MouseEvent evt)
	{
		if (draggingMain)
		{
			if (evt.getX() <= 0) return;
			double value = convertXScreenToData(evt.getX());
			waveWindow.setMainXPositionCursor(value);
            waveWindow.repaintAllPanels();
		} else if (draggingExt)
		{
			if (evt.getX() <= 0) return;
			double value = convertXScreenToData(evt.getX());
			waveWindow.setExtensionXPositionCursor(value);
            waveWindow.repaintAllPanels();
		} else if (draggingVertAxis)
		{
			if (evt.getX() <= 0) return;
			if (waveWindow.isXAxisLocked())
			{
				for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
				{
					Panel wp = it.next();
					wp.vertAxisPos = evt.getX();
				}
				waveWindow.redrawAllPanels();
				waveWindow.getMainHorizRuler().repaint();
			} else
			{
				vertAxisPos = evt.getX();
				repaintWithRulers();
			}
		} else if (draggingArea)
		{
			Set<Panel> measureWindows = new HashSet<Panel>();
			Point cPt = new Point();
			Panel curPanel = this;
			measureWindows.add(curPanel);
			JTable table = waveWindow.getWaveformTable();

			// find the current Panel
			int startPanel = 0;
			for(int i=0; i<table.getRowCount(); i++)
			{
				if (this == waveWindow.getPanel(i)) { startPanel = i;   break; }
			}

			// find the panel with the coordinates
			int yp = evt.getY();
			Rectangle bou = evt.getComponent().getBounds();
			if (yp >= bou.y && yp < bou.y+bou.height)
			{
				cPt.setLocation(evt.getX(), evt.getY());
			} else
			{
				int curPanelNum = startPanel;
				if (yp < bou.y)
				{
					while (yp < bou.y)
					{
						// negative coordinate: try a previous panel
						if (curPanelNum <= 0) break;
						curPanelNum--;
						measureWindows.add(waveWindow.getPanel(curPanelNum));
						yp += table.getRowHeight(curPanelNum);
					}
				} else if (yp >= bou.y+bou.height)
				{
					while (yp >= bou.y+bou.height)
					{
						// coordinate too large: try a subsequent panel
						if (curPanelNum+1 >= table.getRowCount()) break;
						yp -= table.getRowHeight(curPanelNum);
						curPanelNum++;
						measureWindows.add(waveWindow.getPanel(curPanelNum));
					}
				}
				curPanel = waveWindow.getPanel(curPanelNum);
				cPt.setLocation(evt.getX(), yp);
			}

			// snap to waveform point
			cPt = curPanel.snapPoint(cPt);

			dragEndXD = curPanel.convertXScreenToData(cPt.x);
			dragEndYD = curPanel.convertYScreenToData(cPt.y);
			if (ToolBar.getCursorMode() == ToolBar.CursorMode.MEASURE &&
				!ClickZoomWireListener.isRightMouse(evt))
			{
				// reset all panels
				for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
					it.next().draggingArea = false;

				// update all windows the measurement may have crossed over
				for(Panel wp : measureWindows)
				{
					if (wp.curMeasurement == null)
					{
						wp.curMeasurement = new Rectangle2D.Double();
						wp.measurementList.add(wp.curMeasurement);
					}
					wp.curMeasurement.setRect(dragStartXD, dragStartYD, dragEndXD-dragStartXD, dragEndYD-dragStartYD);
					wp.dragStartXD = dragStartXD;
					wp.dragStartYD = dragStartYD;
					wp.dragEndXD = dragEndXD;
					wp.dragEndYD = dragEndYD;
					wp.draggingArea = true;
                    wp.repaint();
				}
			}
            repaint();
		}
	}

	public void mouseMovedSelect(MouseEvent evt)
	{
		// see if over horizontal cursors
		int mainX = convertXDataToScreen(waveWindow.getMainXPositionCursor());
		int extX = convertXDataToScreen(waveWindow.getExtensionXPositionCursor());
		if (Math.abs(mainX - evt.getX()) < 5 || Math.abs(extX - evt.getX()) < 5 ||
			Math.abs(vertAxisPos - evt.getX()) < 5)
		{
			setCursor(dragXPositionCursor);
		} else
		{
			setCursor(Cursor.getDefaultCursor());
		}
	}

	// ************************************* HIGHLIGHTING *************************************

	public void clearHighlightedSignals()
	{
		for(WaveSignal ws : waveSignals.values())
		{
			if (!ws.isHighlighted()) continue;
			ws.setHighlighted(false);
			ws.clearSelectedControlPoints();
			if (ws.getButton() != null)
				ws.getButton().setBackground(background);
		}
		waveWindow.setHighlightedSweep(-1);
		repaintContents();
	}

	public void addHighlightedSignal(WaveSignal ws, boolean repaintContents)
	{
		if (ws.getButton() != null)
		{
			if (background == null) background = ws.getButton().getBackground();
			ws.getButton().setBackground(new Color(User.getColor(User.ColorPrefType.WAVE_BACKGROUND)));
		}
		ws.setHighlighted(true);
		waveWindow.setHighlightedSweep(-1);
		if (repaintContents) repaintContents();
	}

	public void removeHighlightedSignal(WaveSignal ws, boolean repaintContents)
	{
		ws.setHighlighted(false);
		if (ws.getButton() != null)
			ws.getButton().setBackground(background);
		waveWindow.setHighlightedSweep(-1);
        if (repaintContents) repaintContents();
	}

	public boolean isHidden() { return hidden; }

	public void setHidden(boolean hidden) { this.hidden = hidden; }

	/**
	 * Method to make this the highlighted Panel.
	 */
	public void makeSelectedPanel(int x, int y)
	{
		for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
		{
			Panel wp = it.next();
			if (wp.selected && wp != this)
			{
				wp.selected = false;
				wp.repaintContents();
			}
		}
		if (!selected)
		{
			selected = true;
			repaintContents();
		}
		curPanel = this;
		curXPos = x;
	}

	public boolean isSelected() { return selected; }

	// ****************************** ZOOMING AND PANNING ******************************

	/**
	 * Method to implement the Mouse Pressed event for zooming.
	 */
	public void mousePressedZoom(MouseEvent evt)
	{
		dragStartXD = convertXScreenToData(evt.getX());
		dragStartYD = convertYScreenToData(evt.getY());
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
		double lowXValue = Math.min(dragEndXD, dragStartXD);
		double highXValue = Math.max(dragEndXD, dragStartXD);
		double xRange = highXValue - lowXValue;
		lowXValue -= xRange / 8;
		highXValue += xRange / 8;
		double lowValue = Math.min(dragEndYD, dragStartYD);
		double highValue = Math.max(dragEndYD, dragStartYD);
		double valueRange = highValue - lowValue;
		lowValue -= valueRange / 8;
		highValue += valueRange / 8;
		for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
		{
			Panel wp = it.next();
			if (!waveWindow.isXAxisLocked() && wp != this) continue;
			if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0 || ClickZoomWireListener.isRightMouse(evt))
			{
				// standard click: zoom in
				if (wp.getMinXAxis() > wp.getMaxXAxis()) wp.setXAxisRange(highXValue, lowXValue); else
					wp.setXAxisRange(lowXValue, highXValue);
				if (wp == this)
					wp.setYAxisRange(lowValue, highValue);
			} else
			{
				// shift-click: zoom out
				double oldRange = wp.maxXPosition - wp.minXPosition;
				double min = (lowXValue + highXValue) / 2 - oldRange;
				double max = (lowXValue + highXValue) / 2 + oldRange;
				if (wp.getMinXAxis() > wp.getMaxXAxis()) wp.setXAxisRange(max, min); else
					wp.setXAxisRange(min, max);
				if (wp == this)
					wp.setYAxisRange((lowValue + highValue) / 2 - wp.analogRange,
						(lowValue + highValue) / 2 + wp.analogRange);
			}
			wp.repaintWithRulers();
		}
	}

	/**
	 * Method to implement the Mouse Dragged event for zooming.
	 */
	public void mouseDraggedZoom(MouseEvent evt)
	{
//		ZoomAndPanListener.setProperCursor(evt);
		if (draggingArea)
		{
			dragEndXD = convertXScreenToData(evt.getX());
			dragEndYD = convertYScreenToData(evt.getY());
            repaint();
//            repaintContents();
		}
	}

	public void mouseMovedZoom(MouseEvent evt)
	{
//		ZoomAndPanListener.setProperCursor(evt);
	}

	/**
	 * Method to implement the Mouse Pressed event for panning.
	 */
	public void mousePressedPan(MouseEvent evt)
	{
		dragStartXD = convertXScreenToData(evt.getX());
		dragStartYD = convertYScreenToData(evt.getY());
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
		dragEndXD = convertXScreenToData(evt.getX());
		double dXValue = dragEndXD - dragStartXD;

		dragEndYD = convertYScreenToData(evt.getY());
		double dYValue = dragEndYD - dragStartYD;

		for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
		{
			Panel wp = it.next();
			if (!waveWindow.isXAxisLocked() && wp != this) continue;
			wp.setXAxisRange(wp.minXPosition - dXValue, wp.maxXPosition - dXValue);
			if (wp == this)
				setYAxisRange(analogLowValue - dYValue, analogHighValue - dYValue);
			wp.repaintWithRulers();
		}
		dragStartXD = dragEndXD;
		dragStartYD = dragEndYD;
	}

	public void mouseMovedPan(MouseEvent evt) {}
}
