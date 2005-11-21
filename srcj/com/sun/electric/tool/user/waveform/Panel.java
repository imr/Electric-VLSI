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
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.AnalogSignal;
import com.sun.electric.tool.simulation.DigitalSignal;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.WaveformZoom;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.ZoomAndPanListener;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

/**
 * This class defines a single panel of Signals with an associated list of signal names.
 */
public class Panel extends JPanel
	implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
{
	/** the main waveform window this is part of */			private WaveformWindow waveWindow;
	/** the signal on the X axis (null for time) */			private AnalogSignal xAxisSignal;
	/** maps signal buttons to the actual Signal */			private HashMap<JButton,WaveSignal> waveSignals;
	/** the list of signal name buttons on the left */		private JPanel signalButtons;
	/** the JScrollPane with of signal name buttons */		private JScrollPane signalButtonsPane;
	/** the left side: with signal names etc. */			private JPanel leftHalf;
	/** the right side: with signal traces */				private JPanel rightHalf;
	/** the button to close this panel. */					private JButton close;
	/** the button to hide this panel. */					private JButton hide;
	/** the button to delete selected signal (analog). */	private JButton deleteSignal;
	/** the button to delete all signals (analog). */		private JButton deleteAllSignals;
	/** the signal name button (digital). */				private JButton digitalSignalButton;
	/** displayed range along horozintal axis */			private double minXPosition, maxXPosition;
	/** low value displayed in this panel (analog) */		private double analogLowValue;
	/** high value displayed in this panel (analog) */		private double analogHighValue;
	/** vertical range displayed in this panel (analog) */	private double analogRange;
	/** the size of the window (in pixels) */				private Dimension sz;
	/** true if an X axis cursor is being dragged */		private boolean draggingMain, draggingExt, draggingVertAxis;
	/** true if an area is being dragged */					private boolean draggingArea;
	/** true if this waveform panel is selected */			private boolean selected;
	/** true if this waveform panel is hidden */			private boolean hidden;
	/** true if this waveform panel is analog */			private boolean isAnalog;
	/** the horizontal ruler at the top of this panel. */	private HorizRuler horizRulerPanel;
	/** true if the horizontal ruler is logarithmic */		private boolean horizRulerPanelLogarithmic;
	/** the number of this panel. */						private int panelNumber;
	/** all panels that the "measure" tool crosses into */	private HashSet<Panel> measureWindows;
	/** extent of area dragged-out by cursor */				private int dragStartX, dragStartY;
	/** extent of area dragged-out by cursor */				private int dragEndX, dragEndY;
	/** the location of the Y axis vertical line */			private int vertAxisPos;

	private static final int VERTLABELWIDTH = 60;
	private static Color background = null;
	private static int nextPanelNumber = 1;

	private static final ImageIcon iconHidePanel = Resources.getResource(WaveformWindow.class, "ButtonSimHide.gif");
	private static final ImageIcon iconClosePanel = Resources.getResource(WaveformWindow.class, "ButtonSimClose.gif");
	private static final ImageIcon iconDeleteSignal = Resources.getResource(WaveformWindow.class, "ButtonSimDelete.gif");
	private static final ImageIcon iconDeleteAllSignals = Resources.getResource(WaveformWindow.class, "ButtonSimDeleteAll.gif");
    /** for drawing far-dotted lines */			private static final BasicStroke farDottedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {4,12}, 0);
	/** The color of the grid (a gray) */		private static Color gridColor = new Color(0x808080);
	private static final Cursor dragXPositionCursor = ToolBar.readCursor("CursorDragTime.gif", 8, 8);

	// ****************************** DRAG AND DROP ******************************

	/**
	 * Class to extend a JLabel so that it is draggable.
	 */
	private static class DragLabel extends JLabel implements DragGestureListener, DragSourceListener
	{
		private DragSource dragSource;

		public DragLabel(String s)
		{
			setText(s);
			dragSource = DragSource.getDefaultDragSource();
			dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
		}

		public void dragGestureRecognized(DragGestureEvent e)
		{
			// make the Transferable Object
			Transferable transferable = new StringSelection("PANEL " + getText());

			// begin the drag
			dragSource.startDrag(e, DragSource.DefaultLinkDrop, transferable, this);
		}

		public void dragEnter(DragSourceDragEvent e) {}
		public void dragOver(DragSourceDragEvent e) {}
		public void dragExit(DragSourceEvent e) {}
		public void dragDropEnd(DragSourceDropEvent e) {}
		public void dropActionChanged (DragSourceDragEvent e) {}
	}

	// constructor
	public Panel(WaveformWindow waveWindow, boolean isAnalog)
	{
		// remember state
		this.waveWindow = waveWindow;
		this.isAnalog = isAnalog;
		selected = false;
		panelNumber = nextPanelNumber++;
		vertAxisPos = VERTLABELWIDTH;
		horizRulerPanelLogarithmic = false;

		// setup this panel window
		int height = waveWindow.getPanelSizeDigital();
		if (isAnalog) height = waveWindow.getPanelSizeAnalog();
		sz = new Dimension(50, height);
		setSize(sz.width, sz.height);
		setPreferredSize(sz);
		setLayout(new FlowLayout());
		// add listeners --> BE SURE to remove listeners in finished()
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		xAxisSignal = null;
		waveSignals = new HashMap<JButton,WaveSignal>();

		setXAxisRange(waveWindow.getLowDefaultHorizontalRange(), waveWindow.getHighDefaultHorizontalRange());

		// the left side with signal names
		leftHalf = new WaveformWindow.OnePanel(this, waveWindow);
		leftHalf.setLayout(new GridBagLayout());

		// a drop target for the signal panel
		DropTarget dropTargetLeft = new DropTarget(leftHalf, DnDConstants.ACTION_LINK, WaveformWindow.waveformDropTarget, true);

		// a separator at the top
		JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;       gbc.gridy = 0;
		gbc.gridwidth = 5;   gbc.gridheight = 1;
		gbc.weightx = 1;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(4, 0, 4, 0);
		leftHalf.add(sep, gbc);

		// the name of this panel
		if (isAnalog)
		{
			JLabel label = new DragLabel(Integer.toString(panelNumber));
			label.setToolTipText("Identification number of this waveform panel");
			gbc.gridx = 0;       gbc.gridy = 1;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0.2;  gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.NORTHWEST;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = new Insets(4, 4, 4, 4);
			leftHalf.add(label, gbc);
		} else
		{
			digitalSignalButton = new DragButton(Integer.toString(panelNumber), panelNumber);
			digitalSignalButton.setBorderPainted(false);
			digitalSignalButton.setForeground(Color.BLACK);
			digitalSignalButton.setToolTipText("Name of this waveform panel");
			gbc.gridx = 0;       gbc.gridy = 1;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 1;     gbc.weighty = 1;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = new Insets(0, 4, 0, 4);
			leftHalf.add(digitalSignalButton, gbc);
			digitalSignalButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { digitalSignalNameClicked(evt); }
			});
		}

		// the close button for this panel
		close = new JButton(iconClosePanel);
		close.setBorderPainted(false);
		close.setDefaultCapable(false);
		close.setToolTipText("Close this waveform panel");
		Dimension minWid = new Dimension(iconClosePanel.getIconWidth()+4, iconClosePanel.getIconHeight()+4);
		close.setMinimumSize(minWid);
		close.setPreferredSize(minWid);
		gbc.gridx = 1;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.2;  gbc.weighty = 0;
		if (isAnalog) gbc.anchor = GridBagConstraints.NORTH; else
			gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
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
		gbc.gridx = 2;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.2;  gbc.weighty = 0;
		if (isAnalog) gbc.anchor = GridBagConstraints.NORTH; else
			gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		leftHalf.add(hide, gbc);
		hide.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { hidePanel(); }
		});

		if (isAnalog)
		{
			// the "delete signal" button for this panel
			deleteSignal = new JButton(iconDeleteSignal);
			deleteSignal.setBorderPainted(false);
			deleteSignal.setDefaultCapable(false);
			deleteSignal.setToolTipText("Remove selected signals from waveform panel");
			minWid = new Dimension(iconDeleteSignal.getIconWidth()+4, iconDeleteSignal.getIconHeight()+4);
			deleteSignal.setMinimumSize(minWid);
			deleteSignal.setPreferredSize(minWid);
			gbc.gridx = 3;       gbc.gridy = 1;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0.2;  gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.NONE;
			leftHalf.add(deleteSignal, gbc);
			deleteSignal.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { deleteSignalFromPanel(); }
			});

			// the "delete all signal" button for this panel
			deleteAllSignals = new JButton(iconDeleteAllSignals);
			deleteAllSignals.setBorderPainted(false);
			deleteAllSignals.setDefaultCapable(false);
			deleteAllSignals.setToolTipText("Remove all signals from waveform panel");
			minWid = new Dimension(iconDeleteAllSignals.getIconWidth()+4, iconDeleteAllSignals.getIconHeight()+4);
			deleteAllSignals.setMinimumSize(minWid);
			deleteAllSignals.setPreferredSize(minWid);
			gbc.gridx = 4;       gbc.gridy = 1;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0.2;  gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.NONE;
			leftHalf.add(deleteAllSignals, gbc);
			deleteAllSignals.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { deleteAllSignalsFromPanel(); }
			});
		}

		// the list of signals in this panel (analog only)
		if (isAnalog)
		{
			signalButtons = new JPanel();
			signalButtons.setLayout(new BoxLayout(signalButtons, BoxLayout.Y_AXIS));
			signalButtonsPane = new JScrollPane(signalButtons);
			signalButtonsPane.setPreferredSize(new Dimension(100, height));
			gbc.gridx = 0;       gbc.gridy = 2;
			gbc.gridwidth = 5;   gbc.gridheight = 1;
			gbc.weightx = 1;     gbc.weighty = 1;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.insets = new Insets(0, 0, 0, 0);
			leftHalf.add(signalButtonsPane, gbc);
		}

		// the right side with signal traces
		rightHalf = new JPanel();
		rightHalf.setLayout(new GridBagLayout());

		// a drop target for the signal panel
		DropTarget dropTargetRight = new DropTarget(this, DnDConstants.ACTION_LINK, WaveformWindow.waveformDropTarget, true);

		// a separator at the top
		sep = new JSeparator(SwingConstants.HORIZONTAL);
		gbc.gridx = 0;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 1;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new java.awt.Insets(4, 0, 4, 0);
		rightHalf.add(sep, gbc);

		// the horizontal ruler (if separate rulers in each panel)
		if (!waveWindow.isXAxisLocked())
			addHorizRulerPanel();

		// the waveform display for this panel
		gbc.gridx = 0;       gbc.gridy = 2;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 1;     gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 0, 0, 0);
		rightHalf.add(this, gbc);

		// put the left and right sides into the window
		waveWindow.getSignalNamesPanel().add(leftHalf);
		waveWindow.getSignalTracesPanel().add(rightHalf);

		// add to list of wave panels
		waveWindow.getPanelList().add(this);
		if (waveWindow.getPanelList().size() == 1)
		{
			// on the first real addition, redraw any main horizontal ruler panel
			if (waveWindow.getMainHorizRuler() != null)
			{
				waveWindow.getMainHorizRuler().repaint();
				waveWindow.setMainHorizRulerNeedsRepaint(true);
			}
		}

		// rebuild list of panels
		waveWindow.rebuildPanelList();
		waveWindow.redrawAllPanels();
	}

	public WaveformWindow getWaveWindow() { return waveWindow; }

	/**
	 * Method to return a List of WaveSignals in this panel.
	 * @return a List of WaveSignals in this panel.
	 */
	public List<WaveSignal> getSignals()
	{
		List<WaveSignal> signals = new ArrayList<WaveSignal>();
		for(Iterator<JButton> it = waveSignals.keySet().iterator(); it.hasNext(); )
		{
			JButton but = (JButton)it.next();
			WaveSignal ws = (WaveSignal)waveSignals.get(but);
			signals.add(ws);
		}
		return signals;
	}

	public HashMap<JButton,WaveSignal> getSignalMap() { return waveSignals; }

	public int getNumSignals() { return waveSignals.size(); }

	static long lastClick = 0;

	private void digitalSignalNameClicked(ActionEvent evt)
	{
		long delay = evt.getWhen() - lastClick;
		lastClick = evt.getWhen();
		if (delay < TopLevel.getDoubleClickSpeed())
		{
			toggleBusContents();
			return;
		}

		Set<JButton> set = waveSignals.keySet();
		if (set.size() == 0) return;
		JButton but = (JButton)set.iterator().next();
		WaveSignal ws = (WaveSignal)waveSignals.get(but);

		if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0)
		{
			// standard click: add this as the only trace
			for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				wp.clearHighlightedSignals();
			}
			addHighlightedSignal(ws);
			makeSelectedPanel();
		} else
		{
			// shift click: add or remove to list of highlighted traces
			if (ws.isHighlighted()) removeHighlightedSignal(ws); else
				addHighlightedSignal(ws);
		}

		// show it in the schematic
		waveWindow.crossProbeWaveformToEditWindow();
	}

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

	public void hidePanel()
	{
		waveWindow.hidePanel(this);
	}

	public void closePanel()
	{
		waveWindow.closePanel(this);
		waveWindow.saveSignalOrder();
	}

	private void deleteSignalFromPanel()
	{
		waveWindow.deleteSignalFromPanel(this);
	}

	private void deleteAllSignalsFromPanel()
	{
		waveWindow.deleteAllSignalsFromPanel(this);
	}

	private void toggleBusContents()
	{
		// this panel must have one signal
		Collection<WaveSignal> theSignals = waveSignals.values();
		if (theSignals.size() != 1) return;

		// the only signal must be digital
		WaveSignal ws = (WaveSignal)theSignals.iterator().next();
		if (!(ws.getSignal() instanceof DigitalSignal)) return;

		// the digital signal must be a bus
		DigitalSignal sDSig = (DigitalSignal)ws.getSignal();
		List<Signal> bussedSignals = sDSig.getBussedSignals();
		if (bussedSignals == null) return;

		// see if any of the bussed signals are displayed
		boolean opened = false;
		for(Iterator<Signal> bIt = bussedSignals.iterator(); bIt.hasNext(); )
		{
			DigitalSignal subDS = (DigitalSignal)bIt.next();
			WaveSignal subWs = waveWindow.findDisplayedSignal(subDS);
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

			for(Iterator<Signal> bIt = bussedSignals.iterator(); bIt.hasNext(); )
			{
				DigitalSignal subDS = (DigitalSignal)bIt.next();
				WaveSignal subWs = waveWindow.findDisplayedSignal(subDS);
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
			for(Iterator<Signal> bIt = bussedSignals.iterator(); bIt.hasNext(); )
			{
				DigitalSignal subDS = (DigitalSignal)bIt.next();
				Panel wp = waveWindow.makeNewPanel(false);
				WaveSignal wsig = new WaveSignal(wp, subDS);

				// remove the panels and put them in the right place
				waveWindow.getSignalNamesPanel().remove(wsig.getPanel().leftHalf);
				waveWindow.getSignalTracesPanel().remove(wsig.getPanel().rightHalf);

				Component [] lefts = waveWindow.getSignalNamesPanel().getComponents();
				int destIndex = 0;
				for( ; destIndex < lefts.length; destIndex++)
				{
					if (lefts[destIndex] == leftHalf) break;
				}
				waveWindow.getSignalNamesPanel().add(wsig.getPanel().leftHalf, destIndex+increment);
				waveWindow.getSignalTracesPanel().add(wsig.getPanel().rightHalf, destIndex+increment);
				increment++;
			}
		}
		waveWindow.validatePanel();
		waveWindow.saveSignalOrder();
	}

	/**
	 * Method to set the X axis range in this panel.
	 * @param minXPosition the low X axis value.
	 * @param maxXPosition the high X axis value.
	 */
	public void setXAxisRange(double minXPosition, double maxXPosition)
	{
		this.minXPosition = minXPosition;
		this.maxXPosition = maxXPosition;
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

	/**
	 * Method to repaint this window and its associated ruler panel.
	 */
	public void repaintWithRulers()
	{
		if (horizRulerPanel != null) horizRulerPanel.repaint(); else
		{
			waveWindow.getMainHorizRuler().repaint();
		}
		repaint();
	}

	/**
	 * Method to repaint this Panel.
	 */
	public void paint(Graphics g)
	{
		// to enable keys to be received
		if (waveWindow.getWindowFrame() == WindowFrame.getCurrentWindowFrame())
			requestFocus();

		sz = getSize();
		int wid = sz.width;
		int hei = sz.height;

		Point screenLoc = getLocationOnScreen();
		if (waveWindow.getScreenLowX() != screenLoc.x ||
			waveWindow.getScreenHighX() - waveWindow.getScreenLowX() != wid)
				waveWindow.setMainHorizRulerNeedsRepaint(true);
		waveWindow.setScreenXSize(screenLoc.x, waveWindow.getScreenLowX() + wid);

		// show the image
		g.setColor(new Color(User.getColorWaveformBackground()));
		g.fillRect(0, 0, wid, hei);

		// draw the grid first (behind the signals)
		if (isAnalog && waveWindow.isShowGrid())
		{
			Graphics2D g2 = (Graphics2D)g;
			g2.setStroke(Highlight.dottedLine);
			g.setColor(gridColor);

			// draw the vertical grid lines
			double displayedXLow = convertXScreenToData(vertAxisPos);
			double displayedXHigh = convertXScreenToData(wid);
			StepSize ss = StepSize.getSensibleValues(displayedXHigh, displayedXLow, 10);
			if (ss.separation != 0.0)
			{
				double value = ss.low;
				for(;;)
				{
					if (value >= displayedXLow)
					{
						if (value > ss.high) break;
						int x = convertXDataToScreen(value);
						g.drawLine(x, 0, x, hei);
					}
					value += ss.separation;
				}
			}

			ss = StepSize.getSensibleValues(analogHighValue, analogLowValue, 5);
			if (ss.separation != 0.0)
			{
				double value = ss.low;
				for(;;)
				{
					if (value >= analogLowValue)
					{
						if (value > analogHighValue || value > ss.high) break;
						int y = convertYDataToScreen(value);
						g.drawLine(vertAxisPos, y, wid, y);
					}
					value += ss.separation;
				}
			}
			g2.setStroke(Highlight.solidLine);
		}

		processSignals(g, null);
		processControlPoints(g, null);

		// draw the vertical label
		g.setColor(new Color(User.getColorWaveformForeground()));
		g.drawLine(vertAxisPos, 0, vertAxisPos, hei);
		if (selected)
		{
			g.drawLine(vertAxisPos-1, 0, vertAxisPos-1, hei);
			g.drawLine(vertAxisPos-2, 0, vertAxisPos-2, hei-1);
			g.drawLine(vertAxisPos-3, 0, vertAxisPos-3, hei-2);
		}
		if (isAnalog)
		{
			double displayedLow = convertYScreenToData(hei);
			double displayedHigh = convertYScreenToData(0);
			StepSize ss = StepSize.getSensibleValues(displayedHigh, displayedLow, 5);
			if (ss.separation != 0.0)
			{
				double value = ss.low;
				g.setFont(waveWindow.getFont());
				Graphics2D g2 = (Graphics2D)g;
				int lastY = -1;
				for(int i=0; ; i++)
				{
					if (value > displayedHigh) break;
					if (value >= displayedLow)
					{
						int y = convertYDataToScreen(value);
						if (lastY >= 0)
						{
							if (lastY - y > 100)
							{
								// add 5 tick marks
								for(int j=1; j<5; j++)
								{
									int intY = (lastY - y) / 5 * j + y;
									g.drawLine(vertAxisPos-5, intY, vertAxisPos, intY);
								}
							} else if (lastY - y > 25)
							{
								// add 1 tick mark
								int intY = (lastY - y) / 2 + y;
								g.drawLine(vertAxisPos-5, intY, vertAxisPos, intY);
							}
						}

						g.drawLine(vertAxisPos-10, y, vertAxisPos, y);
						String yValue = prettyPrint(value, ss.rangeScale, ss.stepScale);
						GlyphVector gv = waveWindow.getFont().createGlyphVector(waveWindow.getFontRenderContext(), yValue);
						Rectangle2D glyphBounds = gv.getLogicalBounds();
						int height = (int)glyphBounds.getHeight();
						int yPos = y + height / 2;
						if (yPos-height <= 0) yPos = height+1;
						if (yPos >= hei) yPos = hei;
						g.drawString(yValue, vertAxisPos-10-(int)glyphBounds.getWidth()-2, yPos);
						lastY = y;
					}
					value += ss.separation;
				}
			}
		}

		// draw the X position cursors
		Graphics2D g2 = (Graphics2D)g;
		g2.setStroke(Highlight.dashedLine);
		int x = convertXDataToScreen(waveWindow.getMainXPositionCursor());
		if (x >= vertAxisPos)
			g.drawLine(x, 0, x, hei);
		g2.setStroke(farDottedLine);
		x = convertXDataToScreen(waveWindow.getExtensionXPositionCursor());
		if (x >= vertAxisPos)
			g.drawLine(x, 0, x, hei);
		g2.setStroke(Highlight.solidLine);

		// show dragged area if there
		if (draggingArea)
		{
			g.setColor(new Color(User.getColorWaveformForeground()));
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
					int xCtr = (highX+lowX)/2;
					g.drawString(xDiffString, highX + 12, yPosText);
					g.drawLine(lowX, yPos, lowX-10, yPos);
					g.drawLine(highX, yPos, highX+10, yPos);
					g.drawLine(lowX, yPos, lowX-5, yPos+4);
					g.drawLine(lowX, yPos, lowX-5, yPos-4);
					g.drawLine(highX, yPos, highX+5, yPos+4);
					g.drawLine(highX, yPos, highX+5, yPos-4);
				}

				if (isAnalog)
				{
					// show the low value
					String lowValueString = TextUtils.formatDouble(highValue);
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
					String highValueString = TextUtils.formatDouble(lowValue);
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
					String valueDiffString = TextUtils.formatDouble(highValue - lowValue);
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
						int yCtr = (highY+lowY)/2;
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
		if (p <= 0) p = 1;
		String s = TextUtils.formatDouble(v/d, p);
		return s + "e" + i2;
	}

	private static final int CONTROLPOINTSIZE = 6;

	private List<WaveSelection> processSignals(Graphics g, Rectangle2D bounds)
	{
		List<WaveSelection> selectedObjects = null;
		if (bounds != null) selectedObjects = new ArrayList<WaveSelection>();
		sz = getSize();
		int wid = sz.width;
		int hei = sz.height;
		AnalogSignal xSignal = xAxisSignal;
		if (waveWindow.isXAxisLocked()) xSignal = waveWindow.getXAxisSignalAll();
        double[] result = new double[3];
        double[] result2 = new double[3];

		for(Iterator<WaveSignal> it = waveSignals.values().iterator(); it.hasNext(); )
		{
			WaveSignal ws = (WaveSignal)it.next();
			if (g != null) g.setColor(ws.getColor());
			if (ws.getSignal() instanceof AnalogSignal)
			{
				// draw analog trace
				AnalogSignal as = (AnalogSignal)ws.getSignal();
                for (int s = 0, numSweeps = as.getNumSweeps(); s < numSweeps; s++)
				{
                    SweepSignal ss = null;
                    if (s < waveWindow.getSweepSignals().size())
                        ss = (SweepSignal)waveWindow.getSweepSignals().get(s);
                    if (ss != null && !ss.isIncluded()) continue;
					int lastX = 0, lastLY = 0, lastHY = 0;
					int numEvents = as.getNumEvents(s);
					for(int i=0; i<numEvents; i++)
					{
                        as.getEvent(s, i, result);
                        int x = convertXDataToScreen(result[0]);
                        int lowY = convertYDataToScreen(result[1]);
                        int highY = convertYDataToScreen(result[2]);
						if (xSignal != null)
						{
							xSignal.getEvent(s, i, result2);
							x = convertXDataToScreen(result2[1]);
						}
                        if (i != 0)
                        {
                            if (processALine(g, lastX, lastLY, x, lowY, bounds, selectedObjects, ws, -1)) break;
                            if (lastLY != lastHY || lowY != highY)
                            {
        						if (processALine(g, lastX, lastHY, x, highY, bounds, selectedObjects, ws, -1)) break;
        						if (processALine(g, lastX, lastHY, x, lowY, bounds, selectedObjects, ws, -1)) break;
        						if (processALine(g, lastX, lastLY, x, highY, bounds, selectedObjects, ws, -1)) break;
                            }
							if (waveWindow.isShowVertexPoints())
							{
								if (i < numEvents-1)
								{
									if (processABox(g, x-2, lowY-2, x+2, lowY+2, bounds, selectedObjects, ws, false, 0)) break;
                                    if (lowY != highY)
                                    {
										if (processABox(g, x-2, highY-2, x+2, highY+2, bounds, selectedObjects, ws, false, 0)) break;
                                    }
                                }
							}
						}
						lastX = x;   lastLY = lowY; lastHY = highY; 
					}
				}
				continue;
            }
			if (ws.getSignal() instanceof DigitalSignal)
			{
				// draw digital traces
				DigitalSignal ds = (DigitalSignal)ws.getSignal();
				List<Signal> bussedSignals = ds.getBussedSignals();
				if (bussedSignals != null)
				{
					// a digital bus trace
					int busWidth = bussedSignals.size();
					long curYValue = 0;
					double curXValue = 0;
					int lastX = vertAxisPos;
					for(;;)
					{
						double nextXValue = Double.MAX_VALUE;
						int bit = 0;
						boolean curDefined = true;
						for(Iterator<Signal> bIt = bussedSignals.iterator(); bIt.hasNext(); )
						{
							DigitalSignal subDS = (DigitalSignal)bIt.next();
							int numEvents = subDS.getNumEvents();
							boolean undefined = false;
							for(int i=0; i<numEvents; i++)
							{
								double xValue = subDS.getTime(i);
								if (xValue <= curXValue)
								{
									switch (subDS.getState(i) & Stimuli.LOGIC)
									{
										case Stimuli.LOGIC_LOW:  curYValue &= ~(1<<bit);   undefined = false;   break;
										case Stimuli.LOGIC_HIGH: curYValue |= (1<<bit);    undefined = false;   break;
										case Stimuli.LOGIC_X:
										case Stimuli.LOGIC_Z: undefined = true;    break;
									}
								} else
								{
									if (xValue < nextXValue) nextXValue = xValue;
									break;
								}
							}
							if (undefined) { curDefined = false;   break; }
							bit++;
						}
						int x = convertXDataToScreen(curXValue);
						if (x >= vertAxisPos)
						{
							if (x < vertAxisPos+5)
							{
								// on the left edge: just draw the "<"
								if (processALine(g, x, hei/2, x+5, hei-5, bounds, selectedObjects, ws, -1)) return selectedObjects;
								if (processALine(g, x, hei/2, x+5, 5, bounds, selectedObjects, ws, -1)) return selectedObjects;
							} else
							{
								// bus change point: draw the "X"
								if (processALine(g, x-5, 5, x+5, hei-5, bounds, selectedObjects, ws, -1)) return selectedObjects;
								if (processALine(g, x+5, 5, x-5, hei-5, bounds, selectedObjects, ws, -1)) return selectedObjects;
							}
							if (lastX+5 < x-5)
							{
								// previous bus change point: draw horizontal bars to connect
								if (processALine(g, lastX+5, 5, x-5, 5, bounds, selectedObjects, ws, -1)) return selectedObjects;
								if (processALine(g, lastX+5, hei-5, x-5, hei-5, bounds, selectedObjects, ws, -1)) return selectedObjects;
							}
							if (g != null)
							{
								String valString = "XX";
								if (curDefined) valString = Long.toString(curYValue);
								g.setFont(waveWindow.getFont());
								GlyphVector gv = waveWindow.getFont().createGlyphVector(waveWindow.getFontRenderContext(), valString);
								Rectangle2D glyphBounds = gv.getLogicalBounds();
								int textHei = (int)glyphBounds.getHeight();
								g.drawString(valString, x+2, hei/2+textHei/2);
							}
						}
						curXValue = nextXValue;
						lastX = x;
						if (nextXValue == Double.MAX_VALUE) break;
					}
					if (lastX+5 < wid)
					{
						// run horizontal bars to the end
						if (processALine(g, lastX+5, 5, wid, 5, bounds, selectedObjects, ws, -1)) return selectedObjects;
						if (processALine(g, lastX+5, hei-5, wid, hei-5, bounds, selectedObjects, ws, -1)) return selectedObjects;
					}
					continue;
				}

				// a simple digital signal
				int lastx = vertAxisPos;
				int lastState = 0;
				if (ds.getStateVector() == null) continue;
				int numEvents = ds.getNumEvents();
				int lastLowy = 0, lastHighy = 0;
				for(int i=0; i<numEvents; i++)
				{
					double xValue = ds.getTime(i);
					int x = convertXDataToScreen(xValue);
					if (Simulation.isWaveformDisplayMultiState() && g != null)
					{
						switch (ds.getState(i) & Stimuli.STRENGTH)
						{
							case Stimuli.OFF_STRENGTH:  g.setColor(waveWindow.getOffStrengthColor());    break;
							case Stimuli.NODE_STRENGTH: g.setColor(waveWindow.getNodeStrengthColor());   break;
							case Stimuli.GATE_STRENGTH: g.setColor(waveWindow.getGateStrengthColor());   break;
							case Stimuli.VDD_STRENGTH:  g.setColor(waveWindow.getPowerStrengthColor());  break;
						}
					}
					int state = ds.getState(i) & Stimuli.LOGIC;
					int lowy = 0, highy = 0;
					switch (state)
					{
						case Stimuli.LOGIC_HIGH:
							lowy = highy = 5;
							break;
						case Stimuli.LOGIC_LOW:
							lowy = highy = hei-5;
							break;
						case Stimuli.LOGIC_X:
							lowy = 5;   highy = hei-5;
							break;
						case Stimuli.LOGIC_Z:
							lowy = 5;   highy = hei-5;
							break;
					}
					if (i != 0)
					{
						if (state != lastState)
						{
							if (processALine(g, x, 5, x, hei-5, bounds, selectedObjects, ws, -1)) return selectedObjects;
						}
					}
					if (lastLowy == lastHighy)
					{
						if (processALine(g, lastx, lastLowy, x, lastLowy, bounds, selectedObjects, ws, -1)) return selectedObjects;
					} else
					{
						if (processABox(g, lastx, lastLowy, x, lastHighy, bounds, selectedObjects, ws, false, 0)) return selectedObjects;
					}
					if (i >= numEvents-1)
					{
						if (lowy == highy)
						{
							if (processALine(g, x, lowy, wid-1, lowy, bounds, selectedObjects, ws, -1)) return selectedObjects;
						} else
						{
							if (processABox(g, x, lowy, wid-1, highy, bounds, selectedObjects, ws, false, 0)) return selectedObjects;
						}
					}
					lastx = x;
					lastLowy = lowy;
					lastHighy = highy;
					lastState = state;
				}
			}
		}
		return selectedObjects;
	}

	private List<WaveSelection> processControlPoints(Graphics g, Rectangle2D bounds)
	{
		List<WaveSelection> selectedObjects = null;
		if (bounds != null) selectedObjects = new ArrayList<WaveSelection>();
		sz = getSize();
		int wid = sz.width;
		int hei = sz.height;

		// show control points
		for(Iterator<WaveSignal> it = waveSignals.values().iterator(); it.hasNext(); )
		{
			WaveSignal ws = (WaveSignal)it.next();
			if (g != null) g.setColor(ws.getColor());

			double [] points = ws.getSignal().getControlPoints();
			if (points == null) continue;
			if (g != null) g.setColor(ws.getColor());
			for(int i=0; i<points.length; i++)
			{
				double xValue = points[i];
				int x = convertXDataToScreen(xValue);
				if (processABox(g, x-CONTROLPOINTSIZE, hei-CONTROLPOINTSIZE*2, x+CONTROLPOINTSIZE, hei,
					bounds, selectedObjects, ws, true, xValue)) break;

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
					if (processABox(g, x-CONTROLPOINTSIZE+2, hei-CONTROLPOINTSIZE*2+2, x+CONTROLPOINTSIZE-2, hei-2,
						bounds, selectedObjects, ws, true, xValue)) break;
					g.setColor(ws.getColor());
				}
			}
		}
		return selectedObjects;
	}

	private boolean processABox(Graphics g, int lX, int lY, int hX, int hY, Rectangle2D bounds, List<WaveSelection> result,
		WaveSignal ws, boolean controlPoint, double controlXValue)
	{
		// bounds is non-null if doing hit-testing
		if (bounds != null)
		{
			// do bounds checking for hit testing
			if (hX > bounds.getMinX() && lX < bounds.getMaxX() && hY > bounds.getMinY() && lY < bounds.getMaxY())
			{
				WaveSelection wSel = new WaveSelection();
				wSel.ws = ws;
				wSel.controlPoint = controlPoint;
				wSel.controlXValue = controlXValue;
				result.add(wSel);
				return true;
			}
			return false;
		}

		// not doing hit-testing, just doing drawing
		g.fillRect(lX, lY, hX-lX, hY-lY);
		return false;
	}

	private boolean processALine(Graphics g, int fX, int fY, int tX, int tY, Rectangle2D bounds, List<WaveSelection> result, WaveSignal ws, int sweepNum)
	{
		if (bounds != null)
		{
			// do bounds checking for hit testing
			Point2D from = new Point2D.Double(fX, fY);
			Point2D to = new Point2D.Double(tX, tY);
			if (!GenMath.clipLine(from, to, bounds.getMinX(), bounds.getMaxX(), bounds.getMinY(), bounds.getMaxY()))
			{
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
			sz = getSize();
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
			if (value <= waveWindow.getSmallestXValue()) value = waveWindow.getSmallestXValue();
			double logValue = Math.log10(value);
			double winMinX = minXPosition;
			if (winMinX <= 0) winMinX = waveWindow.getSmallestXValue();
			double logWinMinX = Math.log10(winMinX);
			double winMaxX = maxXPosition;
			if (winMaxX <= 0) winMaxX = waveWindow.getSmallestXValue();
			double logWinMaxX = Math.log10(winMaxX);
			double x = (logValue - logWinMinX) / (logWinMaxX - logWinMinX) * (sz.width - vertAxisPos) + vertAxisPos;
			return (int)x;
		} else
		{
			// linear axes
			double x = (value - minXPosition) / (maxXPosition - minXPosition) * (sz.width - vertAxisPos) + vertAxisPos;
			return (int)x;
		}
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
			if (winMinX <= 0) winMinX = waveWindow.getSmallestXValue();
			double logWinMinX = Math.log10(winMinX);
			double winMaxX = maxXPosition;
			if (winMaxX <= 0) winMaxX = waveWindow.getSmallestXValue();
			double logWinMaxX = Math.log10(winMaxX);
			double xValue = Math.pow(10, ((double)(x - vertAxisPos)) / (sz.width - vertAxisPos) * (logWinMaxX - logWinMinX) + logWinMinX);
			return xValue;
		} else
		{
			// linear axes
			double xValue = ((double)(x - vertAxisPos)) / (sz.width - vertAxisPos) * (maxXPosition - minXPosition) + minXPosition;
			return xValue;
		}
	}

	/**
	 * Method to scale a simulation Y value to the Y coordinate in this window.
	 * @param value the simulation Y value.
	 * @return the Y coordinate of that simulation value on the screen
	 */
	private int convertYDataToScreen(double value)
	{
		double y = sz.height - 1 - (value - analogLowValue) / analogRange * (sz.height-1);
		return (int)y;
	}

	/**
	 * Method to scale a Y coordinate from screen space to data space.
	 * @param y the Y coordinate on the screen.
	 * @return the Y value in the simulation corresponding to that screen coordinate.
	 */
	private double convertYScreenToData(int y)
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
		List<WaveSelection> sigs = processSignals(null, bounds);
		List<WaveSelection> cps = processControlPoints(null, bounds);
		for(Iterator<WaveSelection> it = sigs.iterator(); it.hasNext(); )
			cps.add(it.next());
		return cps;
	}

	public void clearHighlightedSignals()
	{
		for(Iterator<WaveSignal> it = waveSignals.values().iterator(); it.hasNext(); )
		{
			WaveSignal ws = (WaveSignal)it.next();
			if (!ws.isHighlighted()) continue;
			ws.setHighlighted(false);
			ws.clearSelectedControlPoints();
			if (ws.getButton() != null)
				ws.getButton().setBackground(background);
		}
		waveWindow.setHighlightedSweep(-1);
		repaint();
	}

	public void addHighlightedSignal(WaveSignal ws)
	{
		if (ws.getButton() != null)
		{
			if (background == null) background = ws.getButton().getBackground();
			ws.getButton().setBackground(new Color(User.getColorWaveformBackground()));
		}
		ws.setHighlighted(true);
		waveWindow.setHighlightedSweep(-1);
		repaint();
	}

	public void removeHighlightedSignal(WaveSignal ws)
	{
		ws.setHighlighted(false);
		if (ws.getButton() != null)
			ws.getButton().setBackground(background);
		waveWindow.setHighlightedSweep(-1);
		repaint();
	}

	/**
	 * Method to make this the highlighted Panel.
	 */
	public void makeSelectedPanel()
	{
		for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
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
			repaint();
		}
	}

	/**
	 * Make this panel show a linear Y axis.
	 */
	private void makeLinear()
	{
	}

	/**
	 * Make this panel show a logarithmic Y axis.
	 */
	private void makeLogarithmic()
	{
		System.out.println("CANNOT DRAW LOG SCALES YET");
	}

	/**
	 * the MouseListener events
	 */
	public void mousePressed(MouseEvent evt)
	{
		requestFocus();
		waveWindow.vcrClickStop();

		// set this to be the selected panel
		makeSelectedPanel();

		// reset dragging from last time
		for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (wp.draggingArea) wp.repaint();
			wp.draggingArea = false;
		}

		if (evt.getClickCount() == 2 && evt.getX() < vertAxisPos)
		{
			WaveformZoom dialog = new WaveformZoom(TopLevel.getCurrentJFrame(), analogLowValue, analogHighValue, minXPosition, maxXPosition, waveWindow, this);
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
					item = new JMenuItem("Logarithmic (not yet)");
					item.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeLogarithmic(); } });
					menu.add(item);
					menu.show(this, evt.getX(), evt.getY());
					return;
				}
			}
		}
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
	}

	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}

	/**
	 * the MouseMotionListener events
	 */
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
		if (ClickZoomWireListener.isRightMouse(evt) && (evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) != 0)
			mode = ToolBar.CursorMode.ZOOM;
		if (mode == ToolBar.CursorMode.ZOOM) mouseDraggedZoom(evt); else
			if (mode == ToolBar.CursorMode.PAN) mouseDraggedPan(evt); else
				mouseDraggedSelect(evt);
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

	// ****************************** SELECTION IN WAVEFORM WINDOW ******************************

	private static class WaveSelection
	{
		/** Selected signal in Waveform Window */		WaveSignal ws;
		/** true if this is a control point */			boolean    controlPoint;
		/** X value of the control point (if a CP) */	double     controlXValue;
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
		draggingArea = true;
		Point pt = new Point(evt.getX(), evt.getY());
		if (ToolBar.getCursorMode() == ToolBar.CursorMode.MEASURE)
		{
			pt = snapPoint(pt);
			measureWindows = new HashSet<Panel>();
			measureWindows.add(this);
		}
		dragEndX = dragStartX = pt.x;
		dragEndY = dragStartY = pt.y;
	}

	private Point snapPoint(Point pt)
	{
		// snap to any waveform points
		for(Iterator<WaveSignal> it = waveSignals.values().iterator(); it.hasNext(); )
		{
			WaveSignal ws = (WaveSignal)it.next();
			if (!(ws.getSignal() instanceof AnalogSignal)) continue;

			// draw analog trace
			AnalogSignal as = (AnalogSignal)ws.getSignal();
            double[] result = new double[3];
			for(int s=0, numSweeps = as.getNumSweeps(); s<numSweeps; s++)
			{
                SweepSignal ss = null;
                if (s < waveWindow.getSweepSignals().size())
                    ss = (SweepSignal)waveWindow.getSweepSignals().get(s);
                if (ss != null && !ss.isIncluded()) continue;
				int numEvents = as.getNumEvents(s);
				for(int i=0; i<numEvents; i++)
				{
                    as.getEvent(s, i, result);
					int x = convertXDataToScreen(result[0]);
                    int lowY = convertYDataToScreen(result[1]);
                    int highY = convertYDataToScreen(result[2]);
					if (Math.abs(x - pt.x) < 5 && pt.y > lowY - 5 && pt.y < highY + 5)
					{
						pt.x = x;
                		pt.y = Math.max(Math.min(pt.y, highY), lowY);
						return pt;
					}
				}
			}
		}

		// snap to any waveform lines
		Point2D snap = new Point2D.Double(pt.x, pt.y);
		for(Iterator<WaveSignal> it = waveSignals.values().iterator(); it.hasNext(); )
		{
			WaveSignal ws = (WaveSignal)it.next();
			if (!(ws.getSignal() instanceof AnalogSignal)) continue;

			// draw analog trace
			AnalogSignal as = (AnalogSignal)ws.getSignal();
            double[] result = new double[3];
            double[] lastResult = new double[3];
			for(int s=0, numSweeps = as.getNumSweeps(); s<numSweeps; s++)
			{
                SweepSignal ss = null;
                if (s < waveWindow.getSweepSignals().size())
                    ss = (SweepSignal)waveWindow.getSweepSignals().get(s);
                if (ss != null && !ss.isIncluded()) continue;
				int numEvents = as.getNumEvents(s);
                as.getEvent(s, 0, lastResult);
				Point2D lastPt = new Point2D.Double(convertXDataToScreen(lastResult[0]), convertYDataToScreen((lastResult[1] + lastResult[2]) / 2));
				for(int i=1; i<numEvents; i++)
				{
                    as.getEvent(s, i, result);
					Point2D thisPt = new Point2D.Double(convertXDataToScreen(result[0]), convertYDataToScreen((result[1] + result[2]) / 2));
					Point2D closest = GenMath.closestPointToSegment(lastPt, thisPt, snap);
					if (closest.distance(snap) < 5)
					{
						pt.x = (int)Math.round(closest.getX());
                		pt.y = (int)Math.round(closest.getY());
                        break;
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
			Panel wp = (Panel)evt.getSource();
			if (ToolBar.getCursorMode() != ToolBar.CursorMode.MEASURE &&
				ToolBar.getSelectMode() == ToolBar.SelectMode.OBJECTS)
			{
				draggingArea = false;
				List<WaveSelection> selectedObjects = wp.findSignalsInArea(dragStartX, dragEndX, dragStartY, dragEndY);
				if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0)
				{
					// standard click: add this as the only trace
					if (wp.isAnalog) clearHighlightedSignals(); else
					{
						for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
						{
							Panel oWp = (Panel)it.next();
							oWp.clearHighlightedSignals();
						}
					}
					for(Iterator<WaveSelection> it = selectedObjects.iterator(); it.hasNext(); )
					{
						WaveSelection wSel = (WaveSelection)it.next();
						if (wSel.controlPoint)
						{
							wSel.ws.addSelectedControlPoint(wSel.controlXValue);
						}
						wp.addHighlightedSignal(wSel.ws);
					}
				} else
				{
					// shift click: add or remove to list of highlighted traces
					for(Iterator<WaveSelection> it = selectedObjects.iterator(); it.hasNext(); )
					{
						WaveSelection wSel = (WaveSelection)it.next();
						WaveSignal ws = wSel.ws;
						if (ws.isHighlighted())
						{
							if (wSel.controlPoint) ws.removeSelectedControlPoint(wSel.controlXValue);
							removeHighlightedSignal(ws);
						} else
						{
							if (wSel.controlPoint) ws.addSelectedControlPoint(wSel.controlXValue);
							wp.addHighlightedSignal(ws);
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
		repaint();
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
			waveWindow.redrawAllPanels();
		} else if (draggingExt)
		{
			if (evt.getX() <= 0) return;
			double value = convertXScreenToData(evt.getX());
			waveWindow.setExtensionXPositionCursor(value);
			waveWindow.redrawAllPanels();
		} else if (draggingVertAxis)
		{
			if (evt.getX() <= 0) return;
			if (waveWindow.isXAxisLocked())
			{
				for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
				{
					Panel wp = (Panel)it.next();
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
			Point pt = new Point(evt.getX(), evt.getY());
			if (ToolBar.getCursorMode() == ToolBar.CursorMode.MEASURE)
			{
				Rectangle rect = getBounds();
				Panel curPanel = (Panel)evt.getSource();
				Point scPt = evt.getComponent().getLocationOnScreen();

				// if not in current window, find out where it crossed into
				if (!rect.contains(pt))
				{
					Point globalPt = new Point(scPt.x+evt.getX(), scPt.y+evt.getY());
					for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
					{
						Panel wp = (Panel)it.next();
						Point oPt = wp.getLocationOnScreen();
						Dimension sz = wp.getSize();
						if (globalPt.x < oPt.x) continue;
						if (globalPt.x > oPt.x + sz.width) continue;
						if (globalPt.y < oPt.y) continue;
						if (globalPt.y > oPt.y + sz.height) continue;
						measureWindows.add(wp);
						wp.draggingArea = true;
						curPanel = wp;
					}
				}

				// snap to waveform point
				if (curPanel == this) pt = snapPoint(pt); else
				{
					Point oPt = curPanel.getLocationOnScreen();
					pt.y = pt.y + scPt.y - oPt.y;
					pt = curPanel.snapPoint(pt);
					pt.y = pt.y - scPt.y + oPt.y;
				}

				// update all windows the measurement may have crossed over
				for(Iterator<Panel> it = measureWindows.iterator(); it.hasNext(); )
				{
					Panel wp = (Panel)it.next();
					if (wp == this) continue;
					Point oPt = wp.getLocationOnScreen();
					wp.dragStartX = dragStartX;
					wp.dragStartY = dragStartY + scPt.y - oPt.y;
					wp.dragEndX = pt.x;
					wp.dragEndY = pt.y + scPt.y - oPt.y;
					wp.repaint();
				}
			}
			dragEndX = pt.x;
			dragEndY = pt.y;
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
		double lowXValue = convertXScreenToData(Math.min(dragEndX, dragStartX));
		double highXValue = convertXScreenToData(Math.max(dragEndX, dragStartX));
		double xRange = highXValue - lowXValue;
		lowXValue -= xRange / 8;
		highXValue += xRange / 8;
		double lowValue = convertYScreenToData(Math.max(dragEndY, dragStartY));
		double highValue = convertYScreenToData(Math.min(dragEndY, dragStartY));
		double valueRange = highValue - lowValue;
		lowValue -= valueRange / 8;
		highValue += valueRange / 8;
		for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!waveWindow.isXAxisLocked() && wp != this) continue;
			if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0 || ClickZoomWireListener.isRightMouse(evt))
			{
				// standard click: zoom in
				wp.minXPosition = lowXValue;
				wp.maxXPosition = highXValue;
				if (wp == this)
				{
					wp.setYAxisRange(lowValue, highValue);
				}
			} else
			{
				// shift-click: zoom out
				double oldRange = wp.maxXPosition - wp.minXPosition;
				wp.minXPosition = (lowXValue + highXValue) / 2 - oldRange;
				wp.maxXPosition = (lowXValue + highXValue) / 2 + oldRange;
				if (wp == this)
				{
					wp.setYAxisRange((lowValue + highValue) / 2 - wp.analogRange,
						(lowValue + highValue) / 2 + wp.analogRange);
				}
			}
			wp.repaintWithRulers();
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
			repaint();
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
		double dragEndXData = convertXScreenToData(dragEndX);
		double dragStartXData = convertXScreenToData(dragStartX);
		double dXValue = dragEndXData - dragStartXData;

		dragEndY = evt.getY();
		double dragEndYData = convertXScreenToData(dragEndY);
		double dragStartYData = convertXScreenToData(dragStartY);
		double dYValue = dragEndYData - dragStartYData;

		for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!waveWindow.isXAxisLocked() && wp != this) continue;
			wp.minXPosition -= dXValue;
			wp.maxXPosition -= dXValue;
			if (wp == this)
			{
				setYAxisRange(analogLowValue - dYValue, analogHighValue - dYValue);
			}
			wp.repaintWithRulers();
		}
		dragStartX = dragEndX;
		dragStartY = dragEndY;
	}

	public void mouseMovedPan(MouseEvent evt) {}

	public boolean isAnalog() { return isAnalog; };

	public JPanel getSignalButtons() { return signalButtons; };

	public JScrollPane getSignalButtonsPane() { return signalButtonsPane; };

	public JButton getDigitalSignalButton() { return digitalSignalButton; }

	public int getPanelNumber() { return panelNumber; }

	public boolean isHidden() { return hidden; }

	public void setHidden(boolean hidden) { this.hidden = hidden; }

	public boolean isSelected() { return selected; }

	public AnalogSignal getXAxisSignal() { return xAxisSignal; }

	public void setXAxisSignal(AnalogSignal sig) { xAxisSignal = sig; }

	public void setPanelLogarithmic(boolean logarithmic)
	{
		horizRulerPanelLogarithmic = false;
		horizRulerPanel.repaint();
	}

	public int getVertAxisPos() { return vertAxisPos; }

	public void setVertAxisPos(int x) { vertAxisPos = x; }

	public JPanel getLeftHalf() { return leftHalf; }

	public JPanel getRightHalf() { return rightHalf; }
}
