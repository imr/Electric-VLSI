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

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.Simulate;
import com.sun.electric.tool.io.output.PNG;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.simulation.Engine;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.WaveformZoom;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
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
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This class defines the a screenful of Panels that make up a waveform display.
 */
public class WaveformWindow implements WindowContent
{
	private static int panelSizeDigital = 25;
	private static int panelSizeAnalog  = 75;
	private static Color [] colorArray = new Color [] {
		new Color(255,   0,   0),		// red
		new Color(255, 127,   0),
		new Color(255, 255,   0),		// yellow
		new Color(127, 255,   0),
		new Color(0,   235,   0),		// green
		new Color(0,   255, 102),
		new Color(0,   255, 255),		// cyan
		new Color(0,   127, 255),
		new Color(80,   80, 255),		// blue
		new Color(127,   0, 255),
		new Color(255,   0, 255),		// magenta
		new Color(255,   0, 127)};

	/** the window that this lives in */					private WindowFrame wf;
	/** the cell being simulated */							private Stimuli sd;
	/** the simulation engine that runs in this window. */	private Engine se;
	/** the top-level panel of the waveform window. */		private JPanel overall;
	/** left panel: the signal names */						private JPanel left;
	/** right panel: the signal traces */					private JPanel right;
	/** the "lock time" button. */							private JButton timeLock;
	/** the "refresh" button. */							private JButton refresh;
	/** the "show points" button. */						private JButton showPoints;
	/** the "grow panel" button for widening. */			private JButton growPanel;
	/** the "shrink panel" button for narrowing. */			private JButton shrinkPanel;
	/** the list of panels. */								private JComboBox signalNameList;
	/** true if rebuilding the list of panels */			private boolean rebuildingSignalNameList = false;
	/** the main scroll of all panels. */					private JScrollPane scrollAll;
	/** the split between signal names and traces. */		private JSplitPane split;
	/** labels for the text at the top */					private JLabel mainPos, extPos, delta;
	/** buttons for centering the time cursors. */			private JButton centerMain, centerExt;
	/** a list of panels in this window */					private List wavePanels;
	/** a list of sweep signals in this window */			private List sweepSignals;
	/** the time panel at the top of the wave window. */	private TimeTickPanel mainTimePanel;
	/** true to repaint the main time panel. */				private boolean mainTimePanelNeedsRepaint;
	/** the VCR timer, when running */						private Timer vcrTimer;
	/** true to run VCR backwards */						private boolean vcrPlayingBackwards = false;
	/** time the VCR last advanced */						private long vcrLastAdvance;
	/** speed of the VCR (in screen pixels) */				private int vcrAdvanceSpeed = 3;
	/** current "main" time cursor */						private double mainTime;
	/** current "extension" time cursor */					private double extTime;
	/** default range along horozintal axis */				private double minTime, maxTime;
	/** true if the time axis is the same in each panel */	private boolean timeLocked;
	private int highlightedSweep = -1;
	/** true to show points on vertices (analog only) */	private boolean showVertexPoints;
	/** true to show a grid (analog only) */				private boolean showGrid;
	/** the actual screen coordinates of the waveform */	private int screenLowX, screenHighX;
	/** Varible key for true library of fake cell. */		public static final Variable.Key WINDOW_SIGNAL_ORDER = ElectricObject.newKey("SIM_window_signalorder");
	/** The highlighter for this waveform window. */		private Highlighter highlighter;
	private static boolean freezeWaveformHighlighting = false;
	/** The global listener for all waveform windows. */	private static WaveformWindowHighlightListener waveHighlighter = new WaveformWindowHighlightListener();
	/** The color of the grid (a gray) */					private static Color gridColor = new Color(0x808080);

	private static WaveFormDropTarget waveformDropTarget = new WaveFormDropTarget();

	private static final ImageIcon iconAddPanel = Resources.getResource(WaveformWindow.class, "ButtonSimAddPanel.gif");
	private static final ImageIcon iconLockTime = Resources.getResource(WaveformWindow.class, "ButtonSimLockTime.gif");
	private static final ImageIcon iconUnLockTime = Resources.getResource(WaveformWindow.class, "ButtonSimUnLockTime.gif");
	private static final ImageIcon iconRefresh = Resources.getResource(WaveformWindow.class, "ButtonSimRefresh.gif");
	private static final ImageIcon iconPointsOn = Resources.getResource(WaveformWindow.class, "ButtonSimPointsOn.gif");
	private static final ImageIcon iconPointsOff = Resources.getResource(WaveformWindow.class, "ButtonSimPointsOff.gif");
	private static final ImageIcon iconToggleGrid = Resources.getResource(WaveformWindow.class, "ButtonSimGrid.gif");
	private static final ImageIcon iconGrowPanel = Resources.getResource(WaveformWindow.class, "ButtonSimGrow.gif");
	private static final ImageIcon iconShrinkPanel = Resources.getResource(WaveformWindow.class, "ButtonSimShrink.gif");
	private static final ImageIcon iconVCRRewind = Resources.getResource(WaveformWindow.class, "ButtonVCRRewind.gif");
	private static final ImageIcon iconVCRPlayBackward = Resources.getResource(WaveformWindow.class, "ButtonVCRPlayBackward.gif");
	private static final ImageIcon iconVCRStop = Resources.getResource(WaveformWindow.class, "ButtonVCRStop.gif");
	private static final ImageIcon iconVCRPlay = Resources.getResource(WaveformWindow.class, "ButtonVCRPlay.gif");
	private static final ImageIcon iconVCRToEnd = Resources.getResource(WaveformWindow.class, "ButtonVCRToEnd.gif");
	private static final ImageIcon iconVCRFaster = Resources.getResource(WaveformWindow.class, "ButtonVCRFaster.gif");
	private static final ImageIcon iconVCRSlower = Resources.getResource(WaveformWindow.class, "ButtonVCRSlower.gif");
	private static final Cursor dragTimeCursor = ToolBar.readCursor("CursorDragTime.gif", 8, 8);

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
		/** the button to hide this panel. */					private JButton hide;
		/** the button to delete selected signal (analog). */	private JButton deleteSignal;
		/** the button to delete all signals (analog). */		private JButton deleteAllSignals;
		/** the button to toggle bus display (digital). */		private JButton toggleBusSignals;
		/** the signal name button (digital). */				private JButton digitalSignalButton;
		/** displayed range along horozintal axis */			private double minTime, maxTime;
		/** low value displayed in this panel (analog) */		private double analogLowValue;
		/** high value displayed in this panel (analog) */		private double analogHighValue;
		/** vertical range displayed in this panel (analog) */	private double analogRange;
		/** the size of the window (in pixels) */				private Dimension sz;
		/** true if a time cursor is being dragged */			private boolean draggingMain, draggingExt;
		/** true if an area is being dragged */					private boolean draggingArea;
		/** true if this waveform panel is selected */			private boolean selected;
		/** true if this waveform panel is hidden */			private boolean hidden;
		/** true if this waveform panel is analog */			private boolean isAnalog;
		/** the time panel at the top of this panel. */			private TimeTickPanel timePanel;
		/** the number of this panel. */						private int panelNumber;
		/** all panels that the "measure" tool crosses into */	private HashSet measureWindows;
		/** extent of area dragged-out by cursor */				private int dragStartX, dragStartY;
		/** extent of area dragged-out by cursor */				private int dragEndX, dragEndY;

		private static final int VERTLABELWIDTH = 60;
		private static Color background = null;
		private static int nextPanelNumber = 1;

		private static final ImageIcon iconHidePanel = Resources.getResource(WaveformWindow.class, "ButtonSimHide.gif");
		private static final ImageIcon iconClosePanel = Resources.getResource(WaveformWindow.class, "ButtonSimClose.gif");
		private static final ImageIcon iconDeleteSignal = Resources.getResource(WaveformWindow.class, "ButtonSimDelete.gif");
		private static final ImageIcon iconDeleteAllSignals = Resources.getResource(WaveformWindow.class, "ButtonSimDeleteAll.gif");
		private static final ImageIcon iconToggleBus = Resources.getResource(WaveformWindow.class, "ButtonSimToggleBus.gif");

	    // constructor
		public Panel(WaveformWindow waveWindow, boolean isAnalog)
		{
			// remember state
			this.waveWindow = waveWindow;
			this.isAnalog = isAnalog;
			this.selected = false;
			this.panelNumber = nextPanelNumber++;

			// setup this panel window
			int height = panelSizeDigital;
			if (isAnalog) height = panelSizeAnalog;
			sz = new Dimension(50, height);
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
			leftHalf = new OnePanel(this, waveWindow);
			leftHalf.setLayout(new GridBagLayout());

			// a drop target for the signal panel
			DropTarget dropTargetLeft = new DropTarget(leftHalf, DnDConstants.ACTION_LINK, waveformDropTarget, true);

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
			} else
			{
//				// the "toggle bus" button for this panel
//				toggleBusSignals = new JButton(iconToggleBus);
//				toggleBusSignals.setBorderPainted(false);
//				toggleBusSignals.setDefaultCapable(false);
//				toggleBusSignals.setToolTipText("View or hide the individual signals on this bus");
//				minWid = new Dimension(iconToggleBus.getIconWidth()+4, iconToggleBus.getIconHeight()+4);
//				toggleBusSignals.setMinimumSize(minWid);
//				toggleBusSignals.setPreferredSize(minWid);
//				gbc.gridx = 3;       gbc.gridy = 1;
//				gbc.gridwidth = 1;   gbc.gridheight = 1;
//				gbc.weightx = 0.2;  gbc.weighty = 0;
//				gbc.anchor = GridBagConstraints.CENTER;
//				gbc.fill = GridBagConstraints.NONE;
//				leftHalf.add(toggleBusSignals, gbc);
//				toggleBusSignals.addActionListener(new ActionListener()
//				{
//					public void actionPerformed(ActionEvent evt) { toggleBusContents(); }
//				});
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
			rightHalf = new OnePanel(this, waveWindow);
			rightHalf.setLayout(new GridBagLayout());

			// a drop target for the signal panel
			DropTarget dropTargetRight = new DropTarget(rightHalf, DnDConstants.ACTION_LINK, waveformDropTarget, true);

			// a separator at the top
			sep = new JSeparator(SwingConstants.HORIZONTAL);
			gbc.gridx = 0;       gbc.gridy = 0;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 1;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new java.awt.Insets(4, 0, 4, 0);
			rightHalf.add(sep, gbc);

			// the time tick panel (if separate time in each panel)
			if (!waveWindow.timeLocked)
				addTimePanel();

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

			// add to list of wave panels
			waveWindow.wavePanels.add(this);
			if (waveWindow.wavePanels.size() == 1)
			{
				// on the first real addition, redraw any main time panel
				if (waveWindow.mainTimePanel != null)
				{
					waveWindow.mainTimePanel.repaint();
					waveWindow.mainTimePanelNeedsRepaint = true;
				}
			}

			// rebuild list of panels
			waveWindow.rebuildPanelList();
			waveWindow.redrawAllPanels();
		}

		/**
		 * Method to return a List of WaveSignals in this panel.
		 * @return a List of WaveSignals in this panel.
		 */
		public List getSignals()
		{
			List signals = new ArrayList();
			for(Iterator it = waveSignals.keySet().iterator(); it.hasNext(); )
			{
				JButton but = (JButton)it.next();
				WaveSignal ws = (WaveSignal)waveSignals.get(but);
				signals.add(ws);
			}
			return signals;
		}

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

			Set set = waveSignals.keySet();
			if (set.size() == 0) return;
			JButton but = (JButton)set.iterator().next();
			WaveSignal ws = (WaveSignal)waveSignals.get(but);

			if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0)
			{
				// standard click: add this as the only trace
				for(Iterator it = waveWindow.wavePanels.iterator(); it.hasNext(); )
				{
					Panel wp = (Panel)it.next();
					wp.clearHighlightedSignals();
				}
				addHighlightedSignal(ws);
				makeSelectedPanel();
			} else
			{
				// shift click: add or remove to list of highlighted traces
				if (ws.highlighted) removeHighlightedSignal(ws); else
					addHighlightedSignal(ws);
			}

			// show it in the schematic
			waveWindow.showSelectedNetworksInSchematic();
		}

		private void addTimePanel()
		{
			timePanel = new TimeTickPanel(this, waveWindow);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;       gbc.gridy = 1;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 1;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(0, 0, 0, 0);
			rightHalf.add(timePanel, gbc);
		}

		private void removeTimePanel()
		{
			rightHalf.remove(timePanel);
			timePanel = null;
		}

		public void hidePanel()
		{
			waveWindow.hidePanel(this);
		}

		public void closePanel()
		{
			waveWindow.closePanel(this);
			waveWindow.saveSignalOrder();
		}

		private WaveSignal addSignalToPanel(Stimuli.Signal sSig)
		{
			// see if the signal is already there
			for(Iterator it = waveSignals.keySet().iterator(); it.hasNext(); )
			{
				JButton but = (JButton)it.next();
				WaveSignal ws = (WaveSignal)waveSignals.get(but);
				if (ws.sSig == sSig)
				{
					// found it already: just change the color
					Color color = ws.color;
					int index = 0;
					for( ; index<colorArray.length; index++)
					{
						if (color.equals(colorArray[index])) { index++;   break; }
					}
					if (index >= colorArray.length) index = 0;
					ws.color = colorArray[index];
					but.setForeground(colorArray[index]);
					signalButtons.repaint();
					repaint();
					return null;
				}
			}

			// not found: add it
			int sigNo = waveSignals.size();
			WaveSignal wsig = new WaveSignal(this, sSig);
			wsig.color = colorArray[sigNo % colorArray.length];
			signalButtons.validate();
			signalButtons.repaint();
			if (signalButtonsPane != null) signalButtonsPane.validate();
			repaint();
			return wsig;
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
			java.util.Collection theSignals = waveSignals.values();
			if (theSignals.size() != 1) return;

			// the only signal must be digital
			WaveSignal ws = (WaveSignal)theSignals.iterator().next();
			if (!(ws.sSig instanceof Stimuli.DigitalSignal)) return;

			// the digital signal must be a bus
			Stimuli.DigitalSignal sDSig = (Stimuli.DigitalSignal)ws.sSig;
			List bussedSignals = sDSig.getBussedSignals();
			if (bussedSignals == null) return;

			// see if any of the bussed signals are displayed
			boolean opened = false;
			for(Iterator bIt = bussedSignals.iterator(); bIt.hasNext(); )
			{
				Stimuli.DigitalSignal subDS = (Stimuli.DigitalSignal)bIt.next();
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
				List allPanels = new ArrayList();
				for(Iterator it = waveWindow.wavePanels.iterator(); it.hasNext(); )
					allPanels.add(it.next());

				for(Iterator bIt = bussedSignals.iterator(); bIt.hasNext(); )
				{
					Stimuli.DigitalSignal subDS = (Stimuli.DigitalSignal)bIt.next();
					WaveSignal subWs = waveWindow.findDisplayedSignal(subDS);
					if (subWs != null)
					{
						Panel wp = subWs.wavePanel;
						waveWindow.closePanel(wp);
						allPanels.remove(wp);
					}
				}
			} else
			{
				// closed: add all entries on the bus
				for(Iterator bIt = bussedSignals.iterator(); bIt.hasNext(); )
				{
					Stimuli.DigitalSignal subDS = (Stimuli.DigitalSignal)bIt.next();
					Panel wp = new Panel(waveWindow, false);
					WaveSignal wsig = new WaveSignal(wp, subDS);
				}
			}
			waveWindow.overall.validate();
			waveWindow.saveSignalOrder();
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
		 * Method to return the low time range shown in this panel.
		 * @return the low time range shown in this panel.
		 */
		public double getMinTimeRange() { return minTime; }

		/**
		 * Method to return the high time range shown in this panel.
		 * @return the high time range shown in this panel.
		 */
		public double getMaxTimeRange() { return maxTime; }

		/**
		 * Method to set the value range in this panel.
		 * @param low the low value.
		 * @param high the high value.
		 */
		public void setValueRange(double low, double high)
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
		 * Method to repaint this window and its associated time-tick panel.
		 */
		public void repaintWithTime()
		{
			if (timePanel != null) timePanel.repaint(); else
			{
				waveWindow.mainTimePanel.repaint();							
			}
			repaint();
		}

		private Font waveWindowFont;
		private FontRenderContext waveWindowFRC = new FontRenderContext(null, false, false);

		/**
		 * Method to repaint this Panel.
		 */
		public void paint(Graphics g)
		{
			// to enable keys to be received
            if (waveWindow.wf == WindowFrame.getCurrentWindowFrame())
			    requestFocus();

			sz = getSize();
			int wid = sz.width;
			int hei = sz.height;

			Point screenLoc = getLocationOnScreen();
			if (waveWindow.screenLowX != screenLoc.x ||
				waveWindow.screenHighX - waveWindow.screenLowX != wid)
					waveWindow.mainTimePanelNeedsRepaint = true;
			waveWindow.screenLowX = screenLoc.x;
			waveWindow.screenHighX = waveWindow.screenLowX + wid;

			// show the image
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, wid, hei);
			waveWindowFont = new Font(User.getDefaultFont(), Font.PLAIN, 12);

			// draw the grid first (behind the signals)
			if (isAnalog && waveWindow.showGrid)
			{
				Graphics2D g2 = (Graphics2D)g;
				g2.setStroke(Highlight.dottedLine);
				g.setColor(gridColor);

				// draw the vertical grid lines
				double displayedXLow = scaleXToTime(WaveformWindow.Panel.VERTLABELWIDTH);
				double displayedXHigh = scaleXToTime(wid);
				StepSize ss = getSensibleValues(displayedXHigh, displayedXLow, 10);
				if (ss.separation != 0.0)
				{
					double time = ss.low;
					for(;;)
					{
						if (time >= displayedXLow)
						{
							if (time > ss.high) break;
							int x = scaleTimeToX(time);
							g.drawLine(x, 0, x, hei);
						}
						time += ss.separation;
					}
				}

				// draw the horizontal grid lines
				double displayedLow = scaleYToValue(hei);
				double displayedHigh = scaleYToValue(0);
//				ss = getSensibleValues(displayedHigh, displayedLow, 5);

				// instead of sensible values taken from ticks, base it on the range of numbers
				double lowYData = 0, highYData = 0;
				boolean first = true;
				for(Iterator it = waveSignals.values().iterator(); it.hasNext(); )
				{
					WaveSignal ws = (WaveSignal)it.next();
					if (ws.sSig instanceof Stimuli.AnalogSignal)
					{
						// grid on analog trace
						Rectangle2D bounds = ws.sSig.getBounds();
						if (first)
						{
							lowYData = bounds.getMinY();
							highYData = bounds.getMaxY();
							first = false;
						} else
						{
							if (bounds.getMinY() < lowYData) lowYData = bounds.getMinY();
							if (bounds.getMaxY() > highYData) highYData = bounds.getMaxY();
						}
					}
				}
				ss.separation = (highYData-lowYData) / 5;
				ss.low = (highYData-lowYData) / 10 + lowYData;
				ss.high = highYData - (highYData-lowYData) / 10;
				if (ss.separation != 0.0)
				{
					double value = ss.low;
					for(;;)
					{
						if (value >= displayedLow)
						{
							if (value > displayedHigh || value > ss.high) break;
							int y = scaleValueToY(value);
							g.drawLine(VERTLABELWIDTH, y, wid, y);
						}
						value += ss.separation;
					}
				}
				g2.setStroke(Highlight.solidLine);
			}

			// look at all traces in this panel
			processSignals(g, null);

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
					Graphics2D g2 = (Graphics2D)g;
					for(int i=0; ; i++)
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
			Graphics2D g2 = (Graphics2D)g;
			g2.setStroke(Highlight.dashedLine);
			int x = scaleTimeToX(waveWindow.mainTime);
			if (x >= VERTLABELWIDTH)
				g.drawLine(x, 0, x, hei);
			g.setColor(Color.YELLOW);
			x = scaleTimeToX(waveWindow.extTime);
			if (x >= VERTLABELWIDTH)
				g.drawLine(x, 0, x, hei);
			g2.setStroke(Highlight.solidLine);
		
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

					// show the low time value and arrow
					String lowTimeString = convertToEngineeringNotation(lowTime, "s", 9999);
					GlyphVector gv = waveWindowFont.createGlyphVector(waveWindowFRC, lowTimeString);
					Rectangle2D glyphBounds = gv.getVisualBounds();
					int textWid = (int)glyphBounds.getWidth();
					int textHei = (int)glyphBounds.getHeight();
					int textY = (lowY+highY)/2;
					g.drawString(lowTimeString, lowX-textWid-6, textY+textHei/2-10);
					g.drawLine(lowX-1, textY, lowX-textWid, textY);
					g.drawLine(lowX-1, textY, lowX-6, textY+4);
					g.drawLine(lowX-1, textY, lowX-6, textY-4);

					// show the high time value and arrow
					String highTimeString = convertToEngineeringNotation(highTime, "s", 9999);
					gv = waveWindowFont.createGlyphVector(waveWindowFRC, highTimeString);
					glyphBounds = gv.getVisualBounds();
					textWid = (int)glyphBounds.getWidth();
					textHei = (int)glyphBounds.getHeight();
					int highTimeTextWid = textWid;
					g.drawString(highTimeString, highX+6, textY+textHei/2-10);
					g.drawLine(highX+1, textY, highX+textWid, textY);
					g.drawLine(highX+1, textY, highX+6, textY+4);
					g.drawLine(highX+1, textY, highX+6, textY-4);

					// show the difference time value
					String timeDiffString = convertToEngineeringNotation(highTime-lowTime, "s", 9999);
					gv = waveWindowFont.createGlyphVector(waveWindowFRC, timeDiffString);
					glyphBounds = gv.getVisualBounds();
					textWid = (int)glyphBounds.getWidth();
					textHei = (int)glyphBounds.getHeight();
					if (textWid + 24 < highX - lowX)
					{
						// fits inside: draw arrows around text
						int yPosText = highY + textHei*5;
						int yPos = yPosText - textHei/2;
						int xCtr = (highX+lowX)/2;
						g.drawString(timeDiffString, xCtr - textWid/2, yPosText);
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
						g.drawString(timeDiffString, highX + 12, yPosText);
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
						gv = waveWindowFont.createGlyphVector(waveWindowFRC, lowValueString);
						glyphBounds = gv.getVisualBounds();
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
						gv = waveWindowFont.createGlyphVector(waveWindowFRC, highValueString);
						glyphBounds = gv.getVisualBounds();
						textWid = (int)glyphBounds.getWidth();
						textHei = (int)glyphBounds.getHeight();
						yText = highY + 10 + textHei;
						g.drawString(highValueString, xP, yText + textHei + 2);
						g.drawLine(xP, highY+1, xP, yText);
						g.drawLine(xP, highY+1, xP+4, highY+5);
						g.drawLine(xP, highY+1, xP-4, highY+5);

						// show the value difference
						String valueDiffString = TextUtils.formatDouble(highValue - lowValue);
						gv = waveWindowFont.createGlyphVector(waveWindowFRC, valueDiffString);
						glyphBounds = gv.getVisualBounds();
						textWid = (int)glyphBounds.getWidth();
						textHei = (int)glyphBounds.getHeight();
						if (textHei + 12 < highY - lowY)
						{
							// fits inside: draw arrows around text
							int xPos = highX + highTimeTextWid + 30;
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
							int xPos = highX + highTimeTextWid + 30;
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
	
		private List processSignals(Graphics g, Rectangle2D bounds)
		{
			List result = null;
			if (bounds != null) result = new ArrayList();

			sz = getSize();
			int wid = sz.width;
			int hei = sz.height;

			for(Iterator it = waveSignals.values().iterator(); it.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)it.next();
				if (g != null) g.setColor(ws.color);
				if (ws.sSig instanceof Stimuli.AnalogSignal)
				{
					// draw analog trace
					Stimuli.AnalogSignal as = (Stimuli.AnalogSignal)ws.sSig;
					int numEvents = as.getNumEvents();
					if (as.isBasic())
					{
						// basic signal
						int lastX = 0, lastY = 0;
						for(int i=0; i<numEvents; i++)
						{
							double time = ws.sSig.getTime(i);
							int x = scaleTimeToX(time);
							int y = scaleValueToY(as.getValue(i));
							if (i != 0)
							{
								if (processALine(g, lastX, lastY, x, y, bounds, result, ws, -1)) break;
								if (waveWindow.showVertexPoints)
								{
									if (i < numEvents-1)
									{
										if (processABox(g, x-2, y-2, x+2, y+2, bounds, result, ws)) break;
									}
								}
							}
							lastX = x;   lastY = y;
						}
					} else if (as.isSweep())
					{
						// swept signal
						List sweepSignals = waveWindow.sweepSignals;
						for(int s=0; s<as.getNumSweeps(); s++)
						{
							SweepSignal ss = (SweepSignal)sweepSignals.get(s);
							if (ss != null && !ss.included) continue;
							int lastX = 0, lastY = 0;
							numEvents = as.getNumEvents(s);
							for(int i=0; i<numEvents; i++)
							{
								double time = ws.sSig.getTime(i, s);
								int x = scaleTimeToX(time);
								int y = scaleValueToY(as.getSweepValue(s, i));
								if (i != 0)
								{
									if (processALine(g, lastX, lastY, x, y, bounds, result, ws, s)) break;
									if (waveWindow.showVertexPoints)
									{
										if (i < numEvents-1)
										{
											if (processABox(g, x-2, y-2, x+2, y+2, bounds, result, ws)) break;
										}
									}
								}
								lastX = x;   lastY = y;
							}
						}
					} else if (as.isInterval())
					{
						// interval signal
						int lastX = 0, lastLY = 0, lastHY = 0;
						for(int i=0; i<numEvents; i++)
						{
							double time = ws.sSig.getTime(i);
							int x = scaleTimeToX(time);
							int lowY = scaleValueToY(as.getIntervalLowValue(i));
							int highY = scaleValueToY(as.getIntervalHighValue(i));
							if (i != 0)
							{
								if (processALine(g, lastX, lastLY, x, lowY, bounds, result, ws, -1)) break;
								if (processALine(g, lastX, lastHY, x, highY, bounds, result, ws, -1)) break;
								if (processALine(g, lastX, lastHY, x, lowY, bounds, result, ws, -1)) break;
								if (processALine(g, lastX, lastLY, x, highY, bounds, result, ws, -1)) break;
							}
							lastX = x;
							lastLY = lowY;
							lastHY = highY;
						}
					}
					continue;
				}
				if (ws.sSig instanceof Stimuli.DigitalSignal)
				{
					// draw digital traces
					Stimuli.DigitalSignal ds = (Stimuli.DigitalSignal)ws.sSig;
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
								Stimuli.DigitalSignal subDS = (Stimuli.DigitalSignal)bIt.next();
								int numEvents = subDS.getNumEvents();
								boolean undefined = false;
								for(int i=0; i<numEvents; i++)
								{
									double time = subDS.getTime(i);
									if (time <= curTime)
									{
										switch (subDS.getState(i) & Stimuli.LOGIC)
										{
											case Stimuli.LOGIC_LOW:  curValue &= ~(1<<bit);   undefined = false;   break;
											case Stimuli.LOGIC_HIGH: curValue |= (1<<bit);    undefined = false;   break;
											case Stimuli.LOGIC_X:
											case Stimuli.LOGIC_Z: undefined = true;    break;
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
									if (processALine(g, x, hei/2, x+5, hei-5, bounds, result, ws, -1)) return result;
									if (processALine(g, x, hei/2, x+5, 5, bounds, result, ws, -1)) return result;
								} else
								{
									// bus change point: draw the "X"
									if (processALine(g, x-5, 5, x+5, hei-5, bounds, result, ws, -1)) return result;
									if (processALine(g, x+5, 5, x-5, hei-5, bounds, result, ws, -1)) return result;
								}
								if (lastX+5 < x-5)
								{
									// previous bus change point: draw horizontal bars to connect
									if (processALine(g, lastX+5, 5, x-5, 5, bounds, result, ws, -1)) return result;
									if (processALine(g, lastX+5, hei-5, x-5, hei-5, bounds, result, ws, -1)) return result;
								}
								if (g != null)
								{
									String valString = "XX";
									if (curDefined) valString = Long.toString(curValue);
									g.setFont(waveWindowFont);
									GlyphVector gv = waveWindowFont.createGlyphVector(waveWindowFRC, valString);
									Rectangle2D glyphBounds = gv.getVisualBounds();
									int textHei = (int)glyphBounds.getHeight();
									g.drawString(valString, x+2, hei/2+textHei/2);
								}
							}
							curTime = nextTime;
							lastX = x;
							if (nextTime == Double.MAX_VALUE) break;
						}
						if (lastX+5 < wid)
						{
							// run horizontal bars to the end
							if (processALine(g, lastX+5, 5, wid, 5, bounds, result, ws, -1)) return result;
							if (processALine(g, lastX+5, hei-5, wid, hei-5, bounds, result, ws, -1)) return result;
						}
						continue;
					}

					// a simple digital signal
					int lastx = VERTLABELWIDTH;
					int lastState = 0;
					if (ds.getStateVector() == null) continue;
					int numEvents = ds.getNumEvents();
					int lastLowy = 0, lastHighy = 0;
					for(int i=0; i<numEvents; i++)
					{
						double time = ds.getTime(i);
						int x = scaleTimeToX(time);
						int state = ds.getState(i) & Stimuli.LOGIC;
						int lowy = 0, highy = 0;
						switch (state)
						{
							case Stimuli.LOGIC_HIGH: lowy = highy = 5;            break;
							case Stimuli.LOGIC_LOW:  lowy = highy = hei-5;        break;
							case Stimuli.LOGIC_X:    lowy = 5;   highy = hei-5;   break;
							case Stimuli.LOGIC_Z:    lowy = 5;   highy = hei-5;   break;
						}
						if (i != 0)
						{
							if (state != lastState)
							{
								if (processALine(g, x, 5, x, hei-5, bounds, result, ws, -1)) return result;
							}
						}
						if (lastLowy == lastHighy)
						{
							if (processALine(g, lastx, lastLowy, x, lastLowy, bounds, result, ws, -1)) return result;
						} else
						{
							if (processABox(g, lastx, lastLowy, x, lastHighy, bounds, result, ws)) return result;
						}
						if (i >= numEvents-1)
						{
							if (lowy == highy)
							{
								if (processALine(g, x, lowy, wid-1, lowy, bounds, result, ws, -1)) return result;
							} else
							{
								if (processABox(g, x, lowy, wid-1, highy, bounds, result, ws)) return result;
							}
						}
						lastx = x;
						lastLowy = lowy;
						lastHighy = highy;
						lastState = state;
					}
				}
			}
			return result;
		}

		private boolean processABox(Graphics g, int lX, int lY, int hX, int hY, Rectangle2D bounds, List result, WaveSignal ws)
		{
			if (bounds != null)
			{
				// do bounds checking for hit testing
				if (hX > bounds.getMinX() && lX < bounds.getMaxX() && hY > bounds.getMinY() && lY < bounds.getMaxY())
				{
					result.add(ws);
					return true;
				}
				return false;
			}
			g.fillRect(lX, lY, hX-lX, hY-lY);
			return false;
		}

		private boolean processALine(Graphics g, int fX, int fY, int tX, int tY, Rectangle2D bounds, List result, WaveSignal ws, int sweepNum)
		{
			if (bounds != null)
			{
				// do bounds checking for hit testing
				Point2D from = new Point2D.Double(fX, fY);
				Point2D to = new Point2D.Double(tX, tY);
				if (!GenMath.clipLine(from, to, bounds.getMinX(), bounds.getMaxX(), bounds.getMinY(), bounds.getMaxY()))
				{
					result.add(ws);
					return true;
				}
				return false;
			}

			// clip to left edge
			if (fX < VERTLABELWIDTH || tX < VERTLABELWIDTH)
			{
				Point2D from = new Point2D.Double(fX, fY);
				Point2D to = new Point2D.Double(tX, tY);
				sz = getSize();
				if (GenMath.clipLine(from, to, VERTLABELWIDTH, sz.width, 0, sz.height)) return false;
				fX = (int)from.getX();
				fY = (int)from.getY();
				tX = (int)to.getX();
				tY = (int)to.getY();
			}

			// draw the line
			g.drawLine(fX, fY, tX, tY);

			// highlight the line if requested
			boolean highlighted = ws.highlighted;
			if (ws.wavePanel.waveWindow.highlightedSweep >= 0)
			{
				highlighted = ws.wavePanel.waveWindow.highlightedSweep == sweepNum;
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
			Rectangle2D bounds = new Rectangle2D.Double(lXd, lYd, hXd-lXd, hYd-lYd);
			List foundList = processSignals(null, bounds);
			return foundList;
		}
	
		private void clearHighlightedSignals()
		{
			for(Iterator it = waveSignals.values().iterator(); it.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)it.next();
				if (!ws.highlighted) continue;
				ws.highlighted = false;
				if (ws.sigButton != null)
					ws.sigButton.setBackground(background);
			}
			waveWindow.highlightedSweep = -1;
			this.repaint();
		}

		private void addHighlightedSignal(WaveSignal ws)
		{
			if (ws.sigButton != null)
			{
				if (background == null) background = ws.sigButton.getBackground();
				ws.sigButton.setBackground(Color.BLACK);
			}
			ws.highlighted = true;
			waveWindow.highlightedSweep = -1;
			this.repaint();
		}

		private void removeHighlightedSignal(WaveSignal ws)
		{
			ws.highlighted = false;
			if (ws.sigButton != null)
				ws.sigButton.setBackground(background);
			waveWindow.highlightedSweep = -1;
			this.repaint();
		}

		/**
		 * Method to make this the highlighted Panel.
		 */
		public void makeSelectedPanel()
		{
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
			for(Iterator it = waveWindow.getPanels(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				if (wp.draggingArea) wp.repaint();
				wp.draggingArea = false;
			}

			if (evt.getClickCount() == 2 && evt.getX() < VERTLABELWIDTH)
			{
				WaveformZoom dialog = new WaveformZoom(TopLevel.getCurrentJFrame(), analogLowValue, analogHighValue, minTime, maxTime, waveWindow, this);
				return;
			}
			ToolBar.CursorMode mode = ToolBar.getCursorMode();
			if (ClickZoomWireListener.isRightMouse(evt) && (evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) != 0)
				mode = ToolBar.CursorMode.ZOOM;
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

		/**
		 * Method to implement the Mouse Pressed event for selection.
		 */ 
		public void mousePressedSelect(MouseEvent evt)
		{
			// see if the time cursors are selected
			draggingMain = draggingExt = false;
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
			Point pt = new Point(evt.getX(), evt.getY());
			if (ToolBar.getCursorMode() == ToolBar.CursorMode.MEASURE)
			{
				pt = snapPoint(pt);
				measureWindows = new HashSet();
				measureWindows.add(this);
			}
			dragEndX = dragStartX = pt.x;
			dragEndY = dragStartY = pt.y;
		}

		private Point snapPoint(Point pt)
		{
			// snap to any waveform points if measuring
			for(Iterator it = waveSignals.values().iterator(); it.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)it.next();
				if (!(ws.sSig instanceof Stimuli.AnalogSignal)) continue;

				// draw analog trace
				Stimuli.AnalogSignal as = (Stimuli.AnalogSignal)ws.sSig;
				int numEvents = as.getNumEvents();
				if (as.isBasic())
				{
					// basic signal
					for(int i=0; i<numEvents; i++)
					{
						double time = ws.sSig.getTime(i);
						int x = scaleTimeToX(time);
						int y = scaleValueToY(as.getValue(i));
						if (Math.abs(x - pt.x) < 5 && Math.abs(y - pt.y) < 5)
						{
							pt.x = x;
							pt.y = y;
						}
					}
				} else if (as.isSweep())
				{
					// swept signal
					List sweepSignals = waveWindow.sweepSignals;
					for(int s=0; s<as.getNumSweeps(); s++)
					{
						SweepSignal ss = (SweepSignal)sweepSignals.get(s);
						if (ss != null && !ss.included) continue;
						numEvents = as.getNumEvents(s);
						for(int i=0; i<numEvents; i++)
						{
							double time = ws.sSig.getTime(i, s);
							int x = scaleTimeToX(time);
							int y = scaleValueToY(as.getSweepValue(s, i));
							if (Math.abs(x - pt.x) < 5 && Math.abs(y - pt.y) < 5)
							{
								pt.x = x;
								pt.y = y;
							}
						}
					}
				}
			}
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
					List foundList = wp.findSignalsInArea(dragStartX, dragEndX, dragStartY, dragEndY);
					if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0)
					{
						// standard click: add this as the only trace
						if (wp.isAnalog) clearHighlightedSignals(); else
						{
							for(Iterator it = waveWindow.wavePanels.iterator(); it.hasNext(); )
							{
								Panel oWp = (Panel)it.next();
								oWp.clearHighlightedSignals();
							}
						}
						for(Iterator it = foundList.iterator(); it.hasNext(); )
						{
							WaveSignal ws = (WaveSignal)it.next();
							wp.addHighlightedSignal(ws);
						}
					} else
					{
						// shift click: add or remove to list of highlighted traces
						for(Iterator it = foundList.iterator(); it.hasNext(); )
						{
							WaveSignal ws = (WaveSignal)it.next();
							if (ws.highlighted) removeHighlightedSignal(ws); else
								wp.addHighlightedSignal(ws);
						}
					}

					// show it in the schematic
					wp.waveWindow.showSelectedNetworksInSchematic();
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
				waveWindow.redrawAllPanels();
			} else if (draggingExt)
			{
				double time = scaleXToTime(evt.getX());
				waveWindow.setExtensionTimeCursor(time);
				waveWindow.redrawAllPanels();
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
						for(Iterator it = waveWindow.getPanels(); it.hasNext(); )
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
					for(Iterator it = measureWindows.iterator(); it.hasNext(); )
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
				this.repaint();
			}
		}

		public void mouseMovedSelect(MouseEvent evt)
		{
			// see if over time cursors
			int mainX = scaleTimeToX(waveWindow.mainTime);
			int extX = scaleTimeToX(waveWindow.extTime);
			if (Math.abs(mainX - evt.getX()) < 5 || Math.abs(extX - evt.getX()) < 5)
			{
				setCursor(dragTimeCursor);
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
				if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0 || ClickZoomWireListener.isRightMouse(evt))
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
				wp.repaintWithTime();
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
				wp.repaintWithTime();
			}
			dragStartX = dragEndX;
			dragStartY = dragEndY;
		}

		public void mouseMovedPan(MouseEvent evt) {}
	}

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

	/**
	 * Class to extend a JButton so that it is draggable.
	 */
	private static class DragButton extends JButton implements DragGestureListener, DragSourceListener
	{
		private DragSource dragSource;
		private int panelNumber;

		public DragButton(String s, int panelNumber)
		{
			setText(s);
			this.panelNumber = panelNumber;
			dragSource = DragSource.getDefaultDragSource();
			dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
		}

		public void dragGestureRecognized(DragGestureEvent e)
		{
			// make the Transferable Object
			Transferable transferable = new StringSelection("PANEL " + panelNumber + " BUTTON " + getText());

			// begin the drag
			dragSource.startDrag(e, DragSource.DefaultLinkDrop, transferable, this);
		}

		public void dragEnter(DragSourceDragEvent e) {}
		public void dragOver(DragSourceDragEvent e) {}
		public void dragExit(DragSourceEvent e) {}
		public void dragDropEnd(DragSourceDropEvent e) {}
		public void dropActionChanged (DragSourceDragEvent e) {}
	}

	/**
	 * This class extends JPanel so that wavepanels can be identified by the Drag and Drop system.
	 */
	private static class OnePanel extends JPanel
	{
		Panel panel;
		WaveformWindow ww;

		public OnePanel(Panel panel, WaveformWindow ww)
		{
			super();
			this.panel = panel;
			this.ww = ww;
		}

		public Panel getPanel() { return panel; }

		public WaveformWindow getWaveformWindow() { return ww; }
	}

	private static class WaveFormDropTarget implements DropTargetListener
	{
		public void dragEnter(DropTargetDragEvent e)
		{
			e.acceptDrag(e.getDropAction());
		}

		public void dragOver(DropTargetDragEvent e)
		{
			e.acceptDrag(e.getDropAction());
		}

		public void dropActionChanged(DropTargetDragEvent e)
		{
			e.acceptDrag(e.getDropAction());
		}

		public void dragExit(DropTargetEvent e) {}

		public void drop(DropTargetDropEvent dtde)
		{
			Object data = null;
			try
			{
				dtde.acceptDrop(DnDConstants.ACTION_LINK);
				data = dtde.getTransferable().getTransferData(DataFlavor.stringFlavor);
				if (data == null)
				{
					dtde.dropComplete(false);
					return;
				}
			} catch (Throwable t)
			{
                ActivityLogger.logException(t);
				dtde.dropComplete(false);
				return;
			}
			if (!(data instanceof String))
			{
				dtde.dropComplete(false);
				return;
			}
			String sigName = (String)data;
			DropTarget dt = (DropTarget)dtde.getSource();
			if (!(dt.getComponent() instanceof OnePanel))
			{
				dtde.dropComplete(false);
				return;
			}
			OnePanel op = (OnePanel)dt.getComponent();
			WaveformWindow ww = op.getWaveformWindow();
			Panel panel = op.getPanel();

			// see if rearranging the waveform window
			if (sigName.startsWith("PANEL "))
			{
				// rearranging signals and panels
				int panelNumber = TextUtils.atoi(sigName.substring(6));
				Panel sourcePanel = ww.getPanelFromNumber(panelNumber);
				if (sourcePanel == panel)
				{
					// moved to same panel
					dtde.dropComplete(false);
					return;
				}

				// see if a signal button was grabbed
				int sigPos = sigName.indexOf("BUTTON ");
				if (!panel.isAnalog) sigPos = -1;
				if (sigPos < 0)
				{
					// moving the entire panel
					ww.left.remove(sourcePanel.leftHalf);
					ww.right.remove(sourcePanel.rightHalf);

					int destIndex = 0;
					Component [] lefts = ww.left.getComponents();
					for(destIndex=0; destIndex < lefts.length; destIndex++)
					{
						if (lefts[destIndex] == panel.leftHalf) break;
					}
					ww.left.add(sourcePanel.leftHalf, destIndex);
					ww.right.add(sourcePanel.rightHalf, destIndex);

					ww.getPanel().validate();
					dtde.dropComplete(true);
					ww.saveSignalOrder();
					return;
				} else
				{
					// moving a signal (analog only)
					String signalName = sigName.substring(sigPos + 7);
					Stimuli.Signal sSig = null;
					Color oldColor = null;
					for(Iterator it = sourcePanel.waveSignals.values().iterator(); it.hasNext(); )
					{
						WaveSignal ws = (WaveSignal)it.next();
						if (!ws.sSig.getFullName().equals(signalName)) continue;
						sSig = ws.sSig;
						oldColor = ws.color;
						sourcePanel.removeHighlightedSignal(ws);
						sourcePanel.signalButtons.remove(ws.sigButton);
						sourcePanel.waveSignals.remove(ws.sigButton);
						break;
					}
					if (sSig != null)
					{
						sourcePanel.signalButtons.validate();
						sourcePanel.signalButtons.repaint();
						sourcePanel.repaint();
						WaveSignal newSig = panel.addSignalToPanel(sSig);
						if (newSig != null)
						{
							newSig.color = oldColor;
							newSig.sigButton.setForeground(oldColor);
						}
					}
					ww.saveSignalOrder();
					dtde.dropComplete(true);
					return;
				}
			}

			// not rearranging: dropped a signal onto a panel
			Stimuli.Signal sSig = ww.findSignal(sigName);
			if (sSig == null)
			{
				dtde.dropComplete(false);
				return;
			}

			// digital signals are always added in new panels
			if (sSig instanceof Stimuli.DigitalSignal) panel = null;
			if (panel != null)
			{
				// overlay this signal onto an existing panel
				panel.addSignalToPanel(sSig);
				panel.waveWindow.saveSignalOrder();
				panel.makeSelectedPanel();
				dtde.dropComplete(true);
				return;
			}

			// add this signal in a new panel
			boolean isAnalog = false;
			if (sSig instanceof Stimuli.AnalogSignal) isAnalog = true;
			panel = new Panel(ww, isAnalog);
			if (isAnalog)
			{
				Stimuli.AnalogSignal as = (Stimuli.AnalogSignal)sSig;
				Rectangle2D rangeBounds = as.getBounds();
				double lowValue = rangeBounds.getMinY();
				double highValue = rangeBounds.getMaxY();
				double range = highValue - lowValue;
				if (range == 0) range = 2;
				double rangeExtra = range / 10;
				panel.setValueRange(lowValue - rangeExtra, highValue + rangeExtra);
			}
			WaveSignal wsig = new WaveSignal(panel, sSig);
			ww.overall.validate();
			panel.repaint();
			dtde.dropComplete(true);
		}
	}

	// ************************************* TIME GRID ALONG THE TOP OF EACH PANEL *************************************

	/**
	 * This class defines the horizontal time tick display at the top of each Panel.
	 */
	private static class TimeTickPanel extends JPanel
	{
		Panel wavePanel;
		WaveformWindow waveWindow;

		// constructor
		TimeTickPanel(Panel wavePanel, WaveformWindow waveWindow)
		{
			// remember state
			this.wavePanel = wavePanel;
			this.waveWindow = waveWindow;

			// setup this panel window
			Dimension sz = new Dimension(16, 20);
			this.setMinimumSize(sz);
			setPreferredSize(sz);
		}

		/**
		 * Method to repaint this TimeTickPanel.
		 */
		public void paint(Graphics g)
		{
			Dimension sz = getSize();
			int wid = sz.width;
			int hei = sz.height;
			int offX = 0;
			Panel drawHere = wavePanel;
			if (drawHere == null)
			{
				// this is the main time panel for all panels
				Point screenLoc = getLocationOnScreen();
				offX = waveWindow.screenLowX - screenLoc.x;
				int newWid = waveWindow.screenHighX - waveWindow.screenLowX;

				// because the main time panel needs a Panel (won't work if there aren't any)
				// have to do complex things to request a repaint after adding the first Panel
				if (newWid == 0 || waveWindow.wavePanels.size() == 0)
				{
					if (waveWindow.mainTimePanelNeedsRepaint)
						repaint();
					return;
				}

				if (offX + newWid > wid) newWid = wid - offX;
				wid = newWid;

				drawHere = (Panel)waveWindow.wavePanels.get(0);
				waveWindow.mainTimePanelNeedsRepaint = false;
				g.setClip(offX, 0, wid, hei);
			}

			// draw the black background
			g.setColor(Color.BLACK);
			g.fillRect(offX, 0, wid, hei);

			// draw the time ticks
			g.setColor(Color.WHITE);
			if (wavePanel != null)
				g.drawLine(WaveformWindow.Panel.VERTLABELWIDTH + offX, hei-1, wid+offX, hei-1);
			double displayedLow = drawHere.scaleXToTime(WaveformWindow.Panel.VERTLABELWIDTH);
			double displayedHigh = drawHere.scaleXToTime(wid);
			StepSize ss = getSensibleValues(displayedHigh, displayedLow, 10);
			if (ss.separation == 0.0) return;
			double time = ss.low;
			for(;;)
			{
				if (time >= displayedLow)
				{
					if (time > ss.high) break;
					int x = drawHere.scaleTimeToX(time) + offX;
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
	public static class WaveSignal
	{
		/** the panel that holds this signal */			private Panel wavePanel;
		/** the data for this signal */					private Stimuli.Signal sSig;
		/** the color of this signal */					private Color color;
		/** true if this signal is highlighted */		private boolean highlighted;
		/** the button on the left with this signal */	private JButton sigButton;

		private static class SignalButton extends MouseAdapter
		{
			private static final int BUTTON_SIZE = 15;

			private WaveSignal signal;

			SignalButton(WaveSignal signal) { this.signal = signal; }

			public void mouseClicked(MouseEvent e)
			{
				if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
				{
					if (!signal.highlighted)
					{
						signal.wavePanel.clearHighlightedSignals();
						signal.wavePanel.addHighlightedSignal(signal);
						signal.wavePanel.makeSelectedPanel();
					}
					JPopupMenu menu = new JPopupMenu("Color");
					for(int i=0; i < colorArray.length; i++)
						addColoredButton(menu, colorArray[i]);

					menu.show(signal.sigButton, e.getX(), e.getY());
				}
			}
			private void addColoredButton(JPopupMenu menu, Color color)
			{
				BufferedImage bi = new BufferedImage(BUTTON_SIZE, BUTTON_SIZE, BufferedImage.TYPE_INT_RGB);
				for(int y=0; y<BUTTON_SIZE; y++)
				{
					for(int x=0; x<BUTTON_SIZE; x++)
					{
						bi.setRGB(x, y, color.getRGB());
					}
				}
				ImageIcon redIcon = new ImageIcon(bi);
				JMenuItem menuItem = new JMenuItem(redIcon);
				menu.add(menuItem);
				menuItem.addActionListener(new ChangeSignalColorListener(signal, color));
			}
		}

		static class ChangeSignalColorListener implements ActionListener
		{
			WaveSignal signal;
			Color col;

			ChangeSignalColorListener(WaveSignal signal, Color col) { super();  this.signal = signal;   this.col = col; }

			public void actionPerformed(ActionEvent evt)
			{
				signal.color = col;
				signal.sigButton.setForeground(col);
				signal.wavePanel.repaint();
			}
		};

		public WaveSignal(Panel wavePanel, Stimuli.Signal sSig)
		{
			int sigNo = wavePanel.waveSignals.size();
			this.wavePanel = wavePanel;
			this.sSig = sSig;
			highlighted = false;
			String sigName = sSig.getFullName();
			if (wavePanel.isAnalog)
			{
				color = colorArray[sigNo % colorArray.length];
				sigButton = new DragButton(sigName, wavePanel.panelNumber);
				sigButton.setBorderPainted(false);
				sigButton.setDefaultCapable(false);
				sigButton.setForeground(color);
				wavePanel.signalButtons.add(sigButton);
				wavePanel.waveSignals.put(sigButton, this);
				sigButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent evt) { signalNameClicked(evt); }
				});
				sigButton.addMouseListener(new SignalButton(this));
			} else
			{
				this.color = Color.RED;
				wavePanel.digitalSignalButton.setText(sigName);
				wavePanel.waveSignals.put(wavePanel.digitalSignalButton, this);
				sigButton = wavePanel.digitalSignalButton;
				sigButton.setForeground(this.color);
			}
		}

		/**
		 * Method to return the actual signal information associated with this line in the waveform window.
		 * @return the actual signal information associated with this line in the waveform window.
		 */
		public Stimuli.Signal getSignal() { return sSig; }

		private void signalNameClicked(ActionEvent evt)
		{
			JButton signal = (JButton)evt.getSource();
			WaveSignal ws = (WaveSignal)wavePanel.waveSignals.get(signal);
			if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0)
			{
				// standard click: add this as the only trace
				ws.wavePanel.clearHighlightedSignals();
				ws.wavePanel.addHighlightedSignal(ws);
				ws.wavePanel.makeSelectedPanel();
			} else
			{
				// shift click: add or remove to list of highlighted traces
				if (ws.highlighted) ws.wavePanel.removeHighlightedSignal(ws); else
					ws.wavePanel.addHighlightedSignal(ws);
			}

			// show it in the schematic
			ws.wavePanel.waveWindow.showSelectedNetworksInSchematic();
		}
	}

	public static class SweepSignal
	{
		private Object obj;
		private WaveformWindow ww;
		private boolean included;
		private int sweepIndex;

		public SweepSignal(Object obj, WaveformWindow ww)
		{
			this.obj = obj;
			this.ww = ww;
			this.included = true;
			this.sweepIndex = ww.sweepSignals.size();
			ww.sweepSignals.add(this);
		}

		public String toString()
		{
			String name = null;
			if (obj instanceof Double) name = TextUtils.formatDouble(((Double)obj).doubleValue()); else
				name = obj.toString();
			name += (included ? " - INCLUDED" : " - EXCLUDED");
			return name;
		}

		public void setIncluded(boolean included)
		{
			if (this.included == included) return;
			this.included = included;
			for(Iterator it = ww.wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				wp.repaintWithTime();
			}
		}

		public void highlight()
		{
			ww.highlightedSweep = sweepIndex;
			for(Iterator it = ww.wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				wp.repaintWithTime();
			}
		}

		public boolean isIncluded() { return included; }
	}

	// ************************************* CLASS TO ASSOCIATE WAVEFORM WINDOWS WITH EDIT WINDOWS *************************************

	/**
	 * Class to find the WaveformWindow associated with the cell in a given EditWindow.
	 * May have to climb the hierarchy to find the top-level cell that is being simulated.
	 */
	public static class Locator
	{
		private WaveformWindow ww;
		private VarContext context;

		/**
		 * The constructor takes an EditWindow and locates the associated WaveformWindow.
		 * It may have to climb the hierarchy to find it.
		 * @param wnd the EditWindow that is being simulated.
		 */
		public Locator(EditWindow wnd)
		{
			Cell cellInWindow = wnd.getCell();
			VarContext curContext = wnd.getVarContext();
			ww = null;
            Stack contextStack = new Stack();
			for(;;)
			{
				ww = WaveformWindow.findWaveformWindow(cellInWindow);
				if (ww != null) break;
				Nodable no = curContext.getNodable();
				if (no == null) break;
                contextStack.push(no);
				cellInWindow = no.getParent();
				curContext = curContext.pop();
				//context = no.getName() + "." + context;
			}
            context = VarContext.globalContext;
            while (!contextStack.isEmpty()) {
                context = context.push((Nodable)contextStack.pop());
            }
		}

		/**
		 * The constructor takes an EditWindow and a WaveformWindow and determines whether they are associated.
		 * It may have to climb the hierarchy to find out.
		 * @param wnd the EditWindow that is being simulated.
		 * @param wantWW the WaveformWindow that is being associated.
		 */
		public Locator(EditWindow wnd, WaveformWindow wantWW)
		{
			Cell cellInWindow = wnd.getCell();
			VarContext curContext = wnd.getVarContext();
			ww = null;
            Stack contextStack = new Stack();
			for(;;)
			{
				if (wantWW.getCell() == cellInWindow) { ww = wantWW;   break; }
				Nodable no = curContext.getNodable();
				if (no == null) break;
                contextStack.push(no);
				cellInWindow = no.getParent();
				curContext = curContext.pop();
			}
            context = VarContext.globalContext;
            while (!contextStack.isEmpty()) {
                context = context.push((Nodable)contextStack.pop());
            }
		}

		/**
		 * Method to return the WaveformWindow found by this locator class.
		 * @return the WaveformWindow associated with the EditWindow given to the contructor.
		 * Returns null if no WaveformWindow could be found.
		 */
		public WaveformWindow getWaveformWindow() { return ww; }

		/**
		 * Method to return the context of all signals in the EditWindow given to the constructor.
		 * @return the context to prepend to all signals in the EditWindow.
		 * If the EditWindow is directly associated with a WaveformWindow, returns "".
		 */
		public VarContext getContext() { return context; }
	}

	// ************************************* HIGHLIGHT LISTENER FOR ALL WAVEFORM WINDOWS *************************************

	public static class WaveformWindowHighlightListener implements HighlightListener
	{
		/**
		 * Method to highlight waveform signals corresponding to circuit networks that are highlighted.
		 * Method is called when any edit window changes its highlighting.
		 */
		public void highlightChanged(Highlighter which)
		{
			if (freezeWaveformHighlighting) return;
			EditWindow wnd = null;
			WindowFrame highWF = which.getWindowFrame();
			if (highWF != null)
			{
				if (highWF.getContent() instanceof EditWindow)
					wnd = (EditWindow)highWF.getContent();
			}
			if (wnd == null) return;

			// loop through all windows, looking for waveform windows
			for(Iterator wIt = WindowFrame.getWindows(); wIt.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)wIt.next();
				if (!(wf.getContent() instanceof WaveformWindow)) continue;
				WaveformWindow ww = (WaveformWindow)wf.getContent();

				// start by removing all highlighting in the waveform
				for(Iterator it = ww.wavePanels.iterator(); it.hasNext(); )
				{
					Panel wp = (Panel)it.next();
					wp.clearHighlightedSignals();
				}

				Set highSet = which.getHighlightedNetworks();
				if (highSet.size() == 1)
				{
					Locator loc = new Locator(wnd, ww);
					if (loc.getWaveformWindow() != ww) continue;
					Network net = (Network)highSet.iterator().next();
					//String netName = loc.getContext() + net.describe();
                    String netName = WaveformWindow.getSpiceNetName(loc.getContext(), net);
                    Stimuli.Signal sSig = ww.sd.findSignalForNetwork(netName);
					if (sSig == null)
					{
						netName = netName.replace('@', '_');
						sSig = ww.sd.findSignalForNetwork(netName);
						if (sSig == null) return;
					}

					boolean foundSignal = false;
					for(Iterator it = ww.wavePanels.iterator(); it.hasNext(); )
					{
						Panel wp = (Panel)it.next();
						for(Iterator sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
						{
							WaveSignal ws = (WaveSignal)sIt.next();
							if (ws.sSig == sSig)
							{
								wp.addHighlightedSignal(ws);
								foundSignal = true;
							}
						}
					}
					if (foundSignal) ww.repaint();
				}
			}
		}

		/**
		 * Called when by a Highlighter when it loses focus. The argument
		 * is the Highlighter that has gained focus (may be null).
		 * @param highlighterGainedFocus the highlighter for the current window (may be null).
		 */
		public void highlighterLostFocus(Highlighter highlighterGainedFocus) {}
	}

    // ************************************* CONTROL *************************************

	private static class WaveComponentListener implements ComponentListener
	{
		private JPanel panel;

		public WaveComponentListener(JPanel panel) { this.panel = panel; }

		public void componentHidden(ComponentEvent e) {}
		public void componentMoved(ComponentEvent e) {}
		public void componentResized(ComponentEvent e)
		{
			panel.repaint();
		}
		public void componentShown(ComponentEvent e) {}
	}

	public WaveformWindow(Stimuli sd, WindowFrame wf)
	{
		// initialize the structure
		this.wf = wf;
		this.sd = sd;
		sd.setWaveformWindow(this);
		resetSweeps();
		wavePanels = new ArrayList();
		this.timeLocked = true;
		this.showVertexPoints = false;
		this.showGrid = false;

        highlighter = new Highlighter(Highlighter.SELECT_HIGHLIGHTER, wf);

		// the total panel in the waveform window
		overall = new OnePanel(null, this);
		overall.setLayout(new GridBagLayout());

		WaveComponentListener wcl = new WaveComponentListener(overall);
		overall.addComponentListener(wcl);

		// the main part of the waveform window: a split-pane between names and traces, put into a scrollpane
		left = new JPanel();
		left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
		right = new JPanel();
		right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
		split.setResizeWeight(0.1);
		scrollAll = new JScrollPane(split);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;       gbc.gridy = 2;
		gbc.gridwidth = 13;  gbc.gridheight = 1;
		gbc.weightx = 1;     gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.BOTH;
		overall.add(scrollAll, gbc);

		if (sd.isAnalog())
		{
			// the top part of the waveform window: status information
			JButton addPanel = new JButton(iconAddPanel);
			addPanel.setBorderPainted(false);
			addPanel.setDefaultCapable(false);
			addPanel.setToolTipText("Create new waveform panel");
			Dimension minWid = new Dimension(iconAddPanel.getIconWidth()+4, iconAddPanel.getIconHeight()+4);
			addPanel.setMinimumSize(minWid);
			addPanel.setPreferredSize(minWid);
			gbc.gridx = 0;       gbc.gridy = 0;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = java.awt.GridBagConstraints.NONE;
			overall.add(addPanel, gbc);
			addPanel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { addNewPanel(); }
			});

			showPoints = new JButton(iconPointsOff);
			showPoints.setBorderPainted(false);
			showPoints.setDefaultCapable(false);
			showPoints.setToolTipText("Toggle display of vertex points");
			minWid = new Dimension(iconPointsOff.getIconWidth()+4, iconPointsOff.getIconHeight()+4);
			showPoints.setMinimumSize(minWid);
			showPoints.setPreferredSize(minWid);
			gbc.gridx = 1;       gbc.gridy = 0;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = java.awt.GridBagConstraints.NONE;
			overall.add(showPoints, gbc);
			showPoints.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { toggleShowPoints(); }
			});

			JButton toggleGrid = new JButton(iconToggleGrid);
			toggleGrid.setBorderPainted(false);
			toggleGrid.setDefaultCapable(false);
			toggleGrid.setToolTipText("Toggle display of a grid");
			minWid = new Dimension(iconToggleGrid.getIconWidth()+4, iconToggleGrid.getIconHeight()+4);
			toggleGrid.setMinimumSize(minWid);
			toggleGrid.setPreferredSize(minWid);
			gbc.gridx = 0;       gbc.gridy = 1;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = java.awt.GridBagConstraints.NONE;
			overall.add(toggleGrid, gbc);
			toggleGrid.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { toggleGridPoints(); }
			});
		}

		refresh = new JButton(iconRefresh);
		refresh.setBorderPainted(false);
		refresh.setDefaultCapable(false);
		refresh.setToolTipText("Reread stimuli data file and update waveforms");
		Dimension minWid = new Dimension(iconRefresh.getIconWidth()+4, iconRefresh.getIconHeight()+4);
		refresh.setMinimumSize(minWid);
		refresh.setPreferredSize(minWid);
		gbc.gridx = 1;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(refresh, gbc);
		refresh.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { refreshData(); }
		});

		timeLock = new JButton(iconLockTime);
		timeLock.setBorderPainted(false);
		timeLock.setDefaultCapable(false);
		timeLock.setToolTipText("Lock all panels in time");
		minWid = new Dimension(iconLockTime.getIconWidth()+4, iconLockTime.getIconHeight()+4);
		timeLock.setMinimumSize(minWid);
		timeLock.setPreferredSize(minWid);
		gbc.gridx = 2;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(timeLock, gbc);
		timeLock.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { togglePanelTimeLock(); }
		});

		signalNameList = new JComboBox();
		signalNameList.setToolTipText("Show or hide waveform panels");
		signalNameList.setLightWeightPopupEnabled(false);
		gbc.gridx = 3;       gbc.gridy = 0;
		gbc.gridwidth = 5;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(signalNameList, gbc);
		signalNameList.addItem("Panel 1");
		signalNameList.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { togglePanelName(); }
		});

		growPanel = new JButton(iconGrowPanel);
		growPanel.setBorderPainted(false);
		growPanel.setDefaultCapable(false);
		growPanel.setToolTipText("Increase minimum panel height");
		minWid = new Dimension(iconGrowPanel.getIconWidth()+4, iconGrowPanel.getIconHeight()+4);
		growPanel.setMinimumSize(minWid);
		growPanel.setPreferredSize(minWid);
		gbc.gridx = 8;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(growPanel, gbc);
		growPanel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { growPanels(1.25); }
		});

		shrinkPanel = new JButton(iconShrinkPanel);
		shrinkPanel.setBorderPainted(false);
		shrinkPanel.setDefaultCapable(false);
		shrinkPanel.setToolTipText("Decrease minimum panel height");
		minWid = new Dimension(iconShrinkPanel.getIconWidth()+4, iconShrinkPanel.getIconHeight()+4);
		shrinkPanel.setMinimumSize(minWid);
		shrinkPanel.setPreferredSize(minWid);
		gbc.gridx = 9;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(shrinkPanel, gbc);
		shrinkPanel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { growPanels(0.8); }
		});

		// the time section that shows the value of the main and extension cursors
		JPanel timeLabelPanel = new JPanel();
		timeLabelPanel.setLayout(new GridBagLayout());
		gbc.gridx = 10;      gbc.gridy = 0;
		gbc.gridwidth = 3;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 4, 0, 4);
		overall.add(timeLabelPanel, gbc);

		mainPos = new JLabel("Main:", JLabel.RIGHT);
		mainPos.setToolTipText("The main (white) time cursor");
		gbc.gridx = 0;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.3;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		timeLabelPanel.add(mainPos, gbc);

		centerMain = new JButton("Center");
		centerMain.setToolTipText("Center the main (white) time cursor");
		gbc.gridx = 1;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.insets = new Insets(2, 4, 2, 0);
		timeLabelPanel.add(centerMain, gbc);
		centerMain.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { centerCursor(true); }
		});

		extPos = new JLabel("Ext:", JLabel.RIGHT);
		extPos.setToolTipText("The extension (yellow) time cursor");
		gbc.gridx = 2;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.3;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		timeLabelPanel.add(extPos, gbc);

		centerExt = new JButton("Center");
		centerExt.setToolTipText("Center the extension (yellow) time cursor");
		gbc.gridx = 3;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.insets = new Insets(2, 4, 2, 0);
		timeLabelPanel.add(centerExt, gbc);
		centerExt.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { centerCursor(false); }
		});

		delta = new JLabel("Delta:", JLabel.CENTER);
		delta.setToolTipText("Time distance between cursors");
		gbc.gridx = 4;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.3;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		timeLabelPanel.add(delta, gbc);

		// add VCR controls
		JButton vcrButtonRewind = new JButton(iconVCRRewind);
		vcrButtonRewind.setBorderPainted(false);
		vcrButtonRewind.setDefaultCapable(false);
		vcrButtonRewind.setToolTipText("Rewind main time cursor to start");
		minWid = new Dimension(iconVCRRewind.getIconWidth()+4, iconVCRRewind.getIconHeight()+4);
		vcrButtonRewind.setMinimumSize(minWid);
		vcrButtonRewind.setPreferredSize(minWid);
		gbc.gridx = 3;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(vcrButtonRewind, gbc);
		vcrButtonRewind.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickRewind(); }
		});

		JButton vcrButtonPlayBackwards = new JButton(iconVCRPlayBackward);
		vcrButtonPlayBackwards.setBorderPainted(false);
		vcrButtonPlayBackwards.setDefaultCapable(false);
		vcrButtonPlayBackwards.setToolTipText("Play main time cursor backwards");
		minWid = new Dimension(iconVCRPlayBackward.getIconWidth()+4, iconVCRPlayBackward.getIconHeight()+4);
		vcrButtonPlayBackwards.setMinimumSize(minWid);
		vcrButtonPlayBackwards.setPreferredSize(minWid);
		gbc.gridx = 4;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(vcrButtonPlayBackwards, gbc);
		vcrButtonPlayBackwards.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickPlayBackwards(); }
		});

		JButton vcrButtonStop = new JButton(iconVCRStop);
		vcrButtonStop.setBorderPainted(false);
		vcrButtonStop.setDefaultCapable(false);
		vcrButtonStop.setToolTipText("Stop moving main time cursor");
		minWid = new Dimension(iconVCRStop.getIconWidth()+4, iconVCRStop.getIconHeight()+4);
		vcrButtonStop.setMinimumSize(minWid);
		vcrButtonStop.setPreferredSize(minWid);
		gbc.gridx = 5;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(vcrButtonStop, gbc);
		vcrButtonStop.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickStop(); }
		});

		JButton vcrButtonPlay = new JButton(iconVCRPlay);
		vcrButtonPlay.setBorderPainted(false);
		vcrButtonPlay.setDefaultCapable(false);
		vcrButtonPlay.setToolTipText("Play main time cursor");
		minWid = new Dimension(iconVCRPlay.getIconWidth()+4, iconVCRPlay.getIconHeight()+4);
		vcrButtonPlay.setMinimumSize(minWid);
		vcrButtonPlay.setPreferredSize(minWid);
		gbc.gridx = 6;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(vcrButtonPlay, gbc);
		vcrButtonPlay.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickPlay(); }
		});

		JButton vcrButtonToEnd = new JButton(iconVCRToEnd);
		vcrButtonToEnd.setBorderPainted(false);
		vcrButtonToEnd.setDefaultCapable(false);
		vcrButtonToEnd.setToolTipText("Move main time cursor to end");
		minWid = new Dimension(iconVCRToEnd.getIconWidth()+4, iconVCRToEnd.getIconHeight()+4);
		vcrButtonToEnd.setMinimumSize(minWid);
		vcrButtonToEnd.setPreferredSize(minWid);
		gbc.gridx = 7;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(vcrButtonToEnd, gbc);
		vcrButtonToEnd.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickToEnd(); }
		});

		JButton vcrButtonFaster = new JButton(iconVCRFaster);
		vcrButtonFaster.setBorderPainted(false);
		vcrButtonFaster.setDefaultCapable(false);
		vcrButtonFaster.setToolTipText("Move main time cursor faster");
		minWid = new Dimension(iconVCRFaster.getIconWidth()+4, iconVCRFaster.getIconHeight()+4);
		vcrButtonFaster.setMinimumSize(minWid);
		vcrButtonFaster.setPreferredSize(minWid);
		gbc.gridx = 8;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(vcrButtonFaster, gbc);
		vcrButtonFaster.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickFaster(); }
		});

		JButton vcrButtonSlower = new JButton(iconVCRSlower);
		vcrButtonSlower.setBorderPainted(false);
		vcrButtonSlower.setDefaultCapable(false);
		vcrButtonSlower.setToolTipText("Move main time cursor slower");
		minWid = new Dimension(iconVCRSlower.getIconWidth()+4, iconVCRSlower.getIconHeight()+4);
		vcrButtonSlower.setMinimumSize(minWid);
		vcrButtonSlower.setPreferredSize(minWid);
		gbc.gridx = 9;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(vcrButtonSlower, gbc);
		vcrButtonSlower.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickSlower(); }
		});


		// the single time panel (when time is locked)
		if (timeLocked)
		{
			addMainTimePanel();
		}

		// a drop target for the overall waveform window
		DropTarget dropTarget = new DropTarget(overall, DnDConstants.ACTION_LINK, waveformDropTarget, true);
	}

	/**
	 * Method to return the associated schematics or layout window for this WaveformWindow.
	 * @return the other window that is cross-linked to this.
	 * Returns null if none can be found.
	 */
	private WindowFrame findSchematicsWindow()
	{
		Cell cell = getCell();
		if (cell == null) return null;

		// look for the original cell to highlight it
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			if (wf.getContent().getCell() != cell) continue;
			if (wf.getContent() instanceof EditWindow) return wf;
		}
		return null;
	}

	/**
	 * Method to return the associated schematics or layout window for this WaveformWindow.
	 * @return the other window that is cross-linked to this.
	 * Returns null if none can be found.
	 */
	public static WaveformWindow findWaveformWindow(Cell cell)
	{
		// look for the original cell to highlight it
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			if (wf.getContent().getCell() != cell) continue;
			if (wf.getContent() instanceof WaveformWindow)
				return (WaveformWindow)wf.getContent();
		}
		return null;
	}

	/**
	 * Method to return an Iterator over the Panel in this window.
	 * @return an Iterator over the Panel in this window.
	 */
	public Iterator getPanels() { return wavePanels.iterator(); }

	/**
	 * Method to return a Panel, given its number.
	 * @param panelNumber the number of the desired Panel.
	 * @return the Panel with that number (null if not found).
	 */
	private Panel getPanelFromNumber(int panelNumber)
	{
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (wp.panelNumber == panelNumber) return wp;
		}
		return null;
	}

	public Engine getSimEngine() { return se; }

	public void setSimEngine(Engine se) { this.se = se; }

	/**
	 * Method called when signal waveforms change, and equivalent should be shown in the edit window.
	 */
	public void showSelectedNetworksInSchematic()
	{
		// highlight the net in any associated edit windows
		freezeWaveformHighlighting = true;
		for(Iterator wIt = WindowFrame.getWindows(); wIt.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)wIt.next();
			if (!(wf.getContent() instanceof EditWindow)) continue;
			EditWindow wnd = (EditWindow)wf.getContent();
			Cell cell = wnd.getCell();
			if (cell == null) continue;
			Highlighter hl = wnd.getHighlighter();

			Locator loc = new Locator(wnd, this);
			if (loc.getWaveformWindow() != this) continue;
			VarContext context = loc.getContext();

			hl.clear();
			for(Iterator it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				for(Iterator pIt = wp.waveSignals.values().iterator(); pIt.hasNext(); )
				{
					WaveSignal ws = (WaveSignal)pIt.next();
					if (!ws.highlighted) continue;
					String want = ws.sSig.getFullName();
                    Stack upNodables = new Stack();
                    Network net = null;
                    for (;;)
                    {
                        String contextStr = getSpiceNetName(context, null);
                        if (contextStr.length() > 0)
                        {
                            boolean matches = false;
                        	contextStr += ".";
                        	if (want.startsWith(contextStr)) matches = true; else
                        	{
                        		contextStr = contextStr.replace('@', '_');
                            	if (want.startsWith(contextStr)) matches = true;
                        	}
                            if (!matches)
                            {
                                if (context == VarContext.globalContext) break;
                                cell = context.getNodable().getParent();
                                upNodables.push(context.getNodable());
                                context = context.pop();
                                continue;
                            }
                        }
            			Netlist netlist = cell.acquireUserNetlist();
            			if (netlist == null)
            			{
            				System.out.println("Sorry, a deadlock aborted crossprobing (network information unavailable).  Please try again");
            				return;
            			}
                        net = findNetwork(netlist, want.substring(contextStr.length()));
                        if (net != null) break;
                        if (context == VarContext.globalContext) break;

                        cell = context.getNodable().getParent();
                        upNodables.push(context.getNodable());
                        context = context.pop();
                    }
                    if (net != null)
                    {
                        // found network
                        while (!upNodables.isEmpty())
                        {
                            Nodable no = (Nodable)upNodables.pop();
                            net = HierarchyEnumerator.getNetworkInChild(net, no);
                            if (net == null) break;
                        }
                    }
                    if (net != null)
                        hl.addNetwork(net, cell);
				}
			}
			hl.finished();
		}
		freezeWaveformHighlighting = false;
	}

	private void addMainTimePanel()
	{
		mainTimePanel = new TimeTickPanel(null, this);
		mainTimePanel.setToolTipText("One time scale applies to all signals when time is locked");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 10;      gbc.gridy = 1;
		gbc.gridwidth = 3;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		overall.add(mainTimePanel, gbc);
	}

	private void resetSweeps()
	{
		sweepSignals = new ArrayList();
		List sweeps = sd.getSweepList();
		for(Iterator it = sweeps.iterator(); it.hasNext(); )
		{
			Object obj = it.next();
			SweepSignal ss = new SweepSignal(obj, this);
		}
	}

	public List getSweepSignals() { return sweepSignals; }

	private void removeMainTimePanel()
	{
		overall.remove(mainTimePanel);
		mainTimePanel = null;
	}

	private void togglePanelName()
	{
		if (rebuildingSignalNameList) return;
		String panelName = (String)signalNameList.getSelectedItem();
		int spacePos = panelName.indexOf(' ');
		if (spacePos >= 0) panelName = panelName.substring(spacePos+1);
		int index = TextUtils.atoi(panelName);

		// toggle its state
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (wp.panelNumber == index)
			{
				if (wp.hidden)
				{
					showPanel(wp);
				} else
				{
					hidePanel(wp);
				}
				break;
			}
		}
	}

	private void tick()
	{
		/* see if it is time to advance the VCR */
		long curtime = System.currentTimeMillis();
		if (curtime - vcrLastAdvance < 100) return;
		vcrLastAdvance = curtime;

		if (this.wavePanels.size() == 0) return;
		Panel wp = (Panel)wavePanels.iterator().next();
		double dTime = wp.scaleDeltaXToTime(vcrAdvanceSpeed);
		double newTime = mainTime;
		Rectangle2D bounds = sd.getBounds();
		if (vcrPlayingBackwards)
		{
			newTime -= dTime;
			double lowTime = bounds.getMinX();
			if (newTime <= lowTime)
			{
				newTime = lowTime;
				vcrClickStop();
			}	
		} else
		{
			newTime += dTime;
			double highTime = bounds.getMaxX();
			if (newTime >= highTime)
			{
				newTime = highTime;
				vcrClickStop();
			}	
		}
		setMainTimeCursor(newTime);
		redrawAllPanels();
	}

	private void vcrClickRewind()
	{
		vcrClickStop();
		Rectangle2D bounds = sd.getBounds();
		double lowTime = bounds.getMinX();
		setMainTimeCursor(lowTime);
		redrawAllPanels();
	}

	private void vcrClickPlayBackwards()
	{
		if (vcrTimer == null)
		{
			ActionListener taskPerformer = new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { tick(); }
			};
			vcrTimer = new Timer(100, taskPerformer);
			vcrLastAdvance = System.currentTimeMillis();
			vcrTimer.start();
		}
		vcrPlayingBackwards = true;
	}

	private void vcrClickStop()
	{
		if (vcrTimer == null) return;
		vcrTimer.stop();
		vcrTimer = null;
	}

	private void vcrClickPlay()
	{
		if (vcrTimer == null)
		{
			ActionListener taskPerformer = new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { tick(); }
			};
			vcrTimer = new Timer(100, taskPerformer);
			vcrLastAdvance = System.currentTimeMillis();
			vcrTimer.start();
		}
		vcrPlayingBackwards = false;
	}

	private void vcrClickToEnd()
	{
		vcrClickStop();
		Rectangle2D bounds = sd.getBounds();
		double highTime = bounds.getMaxX();
		setMainTimeCursor(highTime);
		redrawAllPanels();
	}

	private void vcrClickFaster()
	{
		int j = vcrAdvanceSpeed / 4;
		if (j <= 0) j = 1;
		vcrAdvanceSpeed += j;
	}

	private void vcrClickSlower()
	{
		int j = vcrAdvanceSpeed / 4;
		if (j <= 0) j = 1;
		vcrAdvanceSpeed -= j;
		if (vcrAdvanceSpeed <= 0) vcrAdvanceSpeed = 1;
	}

	/**
	 * Method to update the Simulation data for this waveform window.
	 * When new data is read from disk, this is used.
	 * @param sd new simulation data for this window.
	 */
	public void setSimData(Stimuli sd)
	{
		this.sd = sd;

		// reload the sweeps
		resetSweeps();

		List panelList = new ArrayList();
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
			panelList.add(it.next());
		for(Iterator it = panelList.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			boolean redoPanel = false;
			for(Iterator pIt = wp.waveSignals.values().iterator(); pIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)pIt.next();
				Stimuli.Signal ss = ws.sSig;
				String oldSigName = ss.getFullName();
				ws.sSig = null;
				for(Iterator sIt = sd.getSignals().iterator(); sIt.hasNext(); )
				{
					Stimuli.Signal newSs = (Stimuli.Signal)sIt.next();
					String newSigName = newSs.getFullName();
					if (!newSigName.equals(oldSigName)) continue;
					ws.sSig = newSs;
					break;
				}
				if (ws.sSig == null)
				{
					System.out.println("Could not find signal " + oldSigName + " in the new data");
					redoPanel = true;
				}
			}
			while (redoPanel)
			{
				redoPanel = false;
				for(Iterator pIt = wp.waveSignals.values().iterator(); pIt.hasNext(); )
				{
					WaveSignal ws = (WaveSignal)pIt.next();
					if (ws.sSig == null)
					{
						redoPanel = true;
						wp.signalButtons.remove(ws.sigButton);
						wp.waveSignals.remove(ws.sigButton);
						break;
					}
				}	
			}
			if (wp.waveSignals.size() == 0)
			{
				// removed all signals: delete the panel
				wp.waveWindow.closePanel(wp);
			} else
			{
				if (wp.signalButtons != null)
				{
					wp.signalButtons.validate();
					wp.signalButtons.repaint();
				}
				wp.repaint();
			}
		}
		wf.wantToRedoSignalTree();
		System.out.println("Simulation data refreshed from disk");
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

    /**
     * Get the highlighter for this window content.
     * @return the highlighter
     */
    public Highlighter getHighlighter() { return highlighter; }

	/**
	 * Method to return the static HighlightListener to use for all waveform windows.
	 * @return the static HighlightListener to use for all waveform windows.
	 */
	public static HighlightListener getStaticHighlightListener() { return waveHighlighter; }

	private void rebuildPanelList()
	{
		rebuildingSignalNameList = true;
		signalNameList.removeAllItems();
		boolean hasSignals = false;
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next(); 
			signalNameList.addItem("Panel " + Integer.toString(wp.panelNumber) + (wp.hidden ? " (HIDDEN)" : ""));
			hasSignals = true;
		}
		if (hasSignals) signalNameList.setSelectedIndex(0);
		rebuildingSignalNameList = false;
	}

	public void loadExplorerTree(DefaultMutableTreeNode rootNode)
	{
		wf.libraryExplorerNode = null;
		wf.jobExplorerNode = Job.getExplorerTree();
		wf.errorExplorerNode = ErrorLogger.getExplorerTree();
		wf.signalExplorerNode = getSignalsForExplorer();
		wf.sweepExplorerNode = getSweepsForExplorer();
		rootNode.add(wf.signalExplorerNode);
		if (wf.sweepExplorerNode != null) rootNode.add(wf.sweepExplorerNode);
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
		List signals = sd.getSignals();
		Collections.sort(signals, new SignalsByName());

		// add branches first
		char separatorChar = sd.getSeparatorChar();
		for(Iterator it = signals.iterator(); it.hasNext(); )
		{
			Stimuli.Signal sSig = (Stimuli.Signal)it.next();
			if (sSig.getSignalContext() != null)
				makeContext(sSig.getSignalContext(), contextMap, separatorChar);
		}

		// add all signals to the tree
		for(Iterator it = signals.iterator(); it.hasNext(); )
		{
			Stimuli.Signal sSig = (Stimuli.Signal)it.next();
			DefaultMutableTreeNode thisTree = signalsExplorerTree;
			if (sSig.getSignalContext() != null)
				thisTree = makeContext(sSig.getSignalContext(), contextMap, separatorChar);
			thisTree.add(new DefaultMutableTreeNode(sSig));
		}
		return signalsExplorerTree;
	}

	/**
	 * Recursive method to locate and create branches in the Signal Explorer tree.
	 * @param branchName the name of a branch to find/create.
	 * The name has dots in it to separate levels of the hierarchy.
	 * @param contextMap a HashMap of branch names to tree nodes.
	 * @return the tree node for the requested branch name.
	 */
	private DefaultMutableTreeNode makeContext(String branchName, HashMap contextMap, char separatorChar)
	{
		DefaultMutableTreeNode branchTree = (DefaultMutableTreeNode)contextMap.get(branchName);
		if (branchTree != null) return branchTree;

		// split the branch name into a leaf and parent
		String parent = "";
		String leaf = branchName;
		int dotPos = leaf.lastIndexOf(separatorChar);
		if (dotPos >= 0)
		{
			parent = leaf.substring(0, dotPos);
			leaf = leaf.substring(dotPos+1);
		}

		DefaultMutableTreeNode parentBranch = makeContext(parent, contextMap, separatorChar);
		DefaultMutableTreeNode thisTree = new DefaultMutableTreeNode(leaf);
		parentBranch.add(thisTree);
		contextMap.put(branchName, thisTree);
		return thisTree;
	}

	/**
	 * Class to sort signals by their name
	 */
	private static class SignalsByName implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Stimuli.Signal s1 = (Stimuli.Signal)o1;
			Stimuli.Signal s2 = (Stimuli.Signal)o2;
			return TextUtils.nameSameNumeric(s1.getFullName(), s2.getFullName());
		}
	}

	private DefaultMutableTreeNode getSweepsForExplorer()
	{
		if (sweepSignals.size() <= 0) return null;
		DefaultMutableTreeNode sweepsExplorerTree = new DefaultMutableTreeNode("SWEEPS");
		for(Iterator it = sweepSignals.iterator(); it.hasNext(); )
		{
			SweepSignal ss = (SweepSignal)it.next();
			sweepsExplorerTree.add(new DefaultMutableTreeNode(ss));
		}
		return sweepsExplorerTree;
	}

	private Stimuli.Signal findSignal(String name)
	{
		for(Iterator it = sd.getSignals().iterator(); it.hasNext(); )
		{
			Stimuli.Signal sSig = (Stimuli.Signal)it.next();
			String sigName = sSig.getFullName();
			if (sigName.equals(name)) return sSig;
		}
		return null;
	}

	private static Network findNetwork(Netlist netlist, String name)
	{
		// Should really use extended code, found in "simspicerun.cpp:sim_spice_signalname()"
		for(Iterator nIt = netlist.getNetworks(); nIt.hasNext(); )
		{
			Network net = (Network)nIt.next();
			if (getSpiceNetName(net).equalsIgnoreCase(name)) return net;
		}

		// try converting "@" in network names
		for(Iterator nIt = netlist.getNetworks(); nIt.hasNext(); )
		{
			Network net = (Network)nIt.next();
			String convertedName = getSpiceNetName(net).replace('@', '_');
			if (convertedName.equalsIgnoreCase(name)) return net;
		}
		return null;
	}

	/**
	 * Method to add a set of Networks to the waveform display.
	 * @param nets the Set of Networks to add.
	 * @param context the context of these networks
	 * (a string to prepend to them to get the actual simulation signal name).
	 * @param newPanel true to create new panels for each signal.
	 */
	public void showSignals(Set nets, VarContext context, boolean newPanel)
	{
		// determine the current panel
		Panel wp = null;
		for(Iterator pIt = wavePanels.iterator(); pIt.hasNext(); )
		{
			Panel oWp = (Panel)pIt.next();
			if (oWp.selected)
			{
				wp = oWp;
				break;
			}
		}
		if (!sd.isAnalog()) newPanel = true;
		if (!newPanel && wp == null)
		{
			System.out.println("No current waveform panel to add signals");
			return;
		}

		boolean added = false;
		for(Iterator it = nets.iterator(); it.hasNext(); )
		{
			Network net = (Network)it.next();
            String netName = WaveformWindow.getSpiceNetName(context, net);
            Stimuli.Signal sSig = sd.findSignalForNetwork(netName);
			if (sSig == null)
			{
				netName = netName.replace('@', '_');
				sSig = sd.findSignalForNetwork(netName);
				if (sSig == null)
				{
					System.out.println("Unable to find a signal named " + netName);
					continue;
				}
			}

			// add the signal
			if (newPanel)
			{
				boolean isAnalog = false;
				if (sSig instanceof Stimuli.AnalogSignal) isAnalog = true;
				wp = new Panel(this, isAnalog);
				if (isAnalog)
				{
					Stimuli.AnalogSignal as = (Stimuli.AnalogSignal)sSig;
					Rectangle2D rangeBounds = as.getBounds();
					double lowValue = rangeBounds.getMinY();
					double highValue = rangeBounds.getMaxY();
					double range = highValue - lowValue;
					if (range == 0) range = 2;
					double rangeExtra = range / 10;
					wp.setValueRange(lowValue - rangeExtra, highValue + rangeExtra);
					wp.makeSelectedPanel();
				}
			}

            // check if signal already in panel
            boolean alreadyPlotted = false;
            for(Iterator pIt = wp.waveSignals.values().iterator(); pIt.hasNext(); )
            {
            	WaveSignal ws = (WaveSignal)pIt.next();
                String name = ws.sSig.getFullName();
                if (name.equals(sSig.getFullName())) {
                    alreadyPlotted = true;
                    // add it again, this will increment colors
                    wp.addSignalToPanel(ws.sSig);
                }
            }
            if (!alreadyPlotted) {
            	WaveSignal wsig = new WaveSignal(wp, sSig);
            }
			added = true;
			wp.repaint();
		}
		if (added)
		{
			overall.validate();
			saveSignalOrder();
		}
	}

    public static String getSpiceNetName(Network net) {
        String name = "";
        if (net.hasNames())
        {
            if (net.getExportedNames().hasNext())
            {
                name = (String)net.getExportedNames().next();
            } else
            {
                name = (String)net.getNames().next();
            }
        } else
        {
            name = net.describe();
            if (name.equals(""))
                name = "UNCONNECTED";
        }
        return name;
    }

    /**
     * Get the spice net name associated with the network and the context.
     * If the network is null, a String describing only the context is returned.
     * @param context the context
     * @param net the network, or null
     * @return a String describing the unique, global spice name for the network,
     * or a String describing the context if net is null
     */
    public static String getSpiceNetName(VarContext context, Network net) {
        if (net != null) {
            while (net.isExported() && (context != VarContext.globalContext)) {
                // net is exported, find net in parent
                net = getNetworkInParent(net, context.getNodable());
//                net = HierarchyEnumerator.getNetworkInParent(net, context.getNodable());
                if (net == null) break;
                context = context.pop();
            }
        }
        // create net name
        String contextStr = context.getInstPath(".");
        contextStr = contextStr.toLowerCase();
        if (net == null)
            return contextStr;
        else {
            if (context == VarContext.globalContext) return getSpiceNetName(net);
            else return contextStr + "." + getSpiceNetName(net);
        }
    }

    /**
     * Get the Network in the childNodable's parent that corresponds to the Network
     * inside the childNodable.
     * @param childNetwork the network in the childNodable
     * @return the network in the parent that connects to the
     * specified network, or null if no such network.
     * null on error.
     */
    public static Network getNetworkInParent(Network childNetwork, Nodable childNodable) {
        if (childNodable == null || childNetwork == null) return null;
        if (!(childNodable.getProto() instanceof Cell)) return null;
        Cell childCell = (Cell)childNodable.getProto();
        if (childCell.contentsView() != null)
            childCell = childCell.contentsView();
        // find export on network
        boolean found = false;
        Export export = null;
        int i = 0;
        for (Iterator it = childCell.getPorts(); it.hasNext(); ) {
            export = (Export)it.next();
            for (i=0; i<export.getNameKey().busWidth(); i++) {
        		Netlist netlist = childCell.acquireUserNetlist();
        		if (netlist == null)
        		{
        			System.out.println("Sorry, a deadlock aborted crossprobing (network information unavailable).  Please try again");
        			return null;
        		}
//                Network net = childCell.getUserNetlist().getNetwork(export, i);
                Network net = netlist.getNetwork(export, i);
                if (net == childNetwork) { found = true; break; }
            }
            if (found) break;
        }
        if (!found) return null;
        // find corresponding port on icon
        //System.out.println("In "+cell.describe()+" JNet "+network.describe()+" is exported as "+export.getName()+"; index "+i);
        Export pp = (Export)childNodable.getProto().findPortProto(export.getNameKey());
        //System.out.println("Found corresponding port proto "+pp.getName()+" on cell "+no.getProto().describe());
        // find corresponding network in parent
        Cell parentCell = childNodable.getParent();
        //if (childNodable instanceof NodeInst) childNodable = Netlist.getNodableFor((NodeInst)childNodable, 0);
		Netlist netlist = parentCell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted crossprobing (network information unavailable).  Please try again");
			return null;
		}
//        Network parentNet = parentCell.getUserNetlist().getNetwork(childNodable, pp, i);
        Network parentNet = netlist.getNetwork(childNodable, pp, i);
        return parentNet;
    }

	/**
	 * Method to locate a simulation signal in the waveform.
	 * @param sSig the Stimuli.Signal to locate.
	 * @return the displayed WaveSignal where it is in the waveform window.
	 * Returns null if the signal is not being displayed.
	 */
	public WaveSignal findDisplayedSignal(Stimuli.Signal sSig)
	{
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			for(Iterator sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				if (ws.sSig == sSig) return ws;
			}
		}
		return null;
	}

	/**
	 * Method to remove all highlighting from waveform window.
	 */
	public void clearHighlighting()
	{
		// look at all signal names in the cell
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();

			// look at all traces in this panel
			boolean changed = false;
			for(Iterator sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				if (ws.highlighted) changed = true;
				ws.highlighted = false;
			}
			if (changed) wp.repaint();
		}
	}

	/**
	 * Method to return a List of highlighted simulation signals.
	 * @return a List of highlighted simulation signals.
	 */
	public List getHighlightedNetworkNames()
	{
		List highlightedSignals = new ArrayList();

		// look at all signal names in the cell
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();

			// look at all traces in this panel
			for(Iterator sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				if (ws.highlighted) highlightedSignals.add(ws.sSig);
			}
		}
		return highlightedSignals;
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
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted crossprobing (network information unavailable).  Please try again");
			return nets;
		}

		// look at all signal names in the cell
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();

			// look at all traces in this panel
			for(Iterator sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				Network net = findNetwork(netlist, ws.sSig.getSignalName());
				if (net != null) nets.add(net);
			}
		}
		return nets;
	}

	// ************************************ TIME ************************************

	public double getMainTimeCursor() { return mainTime; }

	public void setMainTimeCursor(double time)
	{
		mainTime = time;
		String amount = convertToEngineeringNotation(mainTime, "s", 9999);
		mainPos.setText("Main: " + amount);
		String diff = convertToEngineeringNotation(Math.abs(mainTime - extTime), "s", 9999);
		delta.setText("Delta: " + diff);
		updateAssociatedLayoutWindow();
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

	/**
	 * Method to set the zoom extents for this waveform window.
	 * @param lowVert the low value of the vertical axis (for the given panel only).
	 * @param highVert the high value of the vertical axis (for the given panel only).
	 * @param lowHoriz the low value of the horizontal (time) axis (for the given panel only unless time is locked).
	 * @param highHoriz the high value of the horizontal (time) axis (for the given panel only unless time is locked).
	 * @param thePanel the panel being zoomed.
	 */
	public void setZoomExtents(double lowVert, double highVert, double lowHoriz, double highHoriz, Panel thePanel)
	{
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			boolean changed = false;
			if (wp == thePanel)
			{
				wp.setValueRange(lowVert, highVert);
				changed = true;
			}
			if (timeLocked || wp == thePanel)
			{
				wp.minTime = lowHoriz;
				wp.maxTime = highHoriz;
				changed = true;
			}
			if (changed) wp.repaintWithTime();
		}
	}

	private void redrawAllPanels()
	{
		left.repaint();
		right.repaint();
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
		wf.setTitle(wf.composeTitle(sd.getCell(), "Waveform for ", 0));
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

	/**
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
		if (GenMath.doublesEqual(time, 0.0)) return "0" + unit;
		if (time < 1.0E-15 || time >= 1000.0) return negative + TextUtils.formatDouble(time) + unit;

		// get proper time unit to use
		double scaled = time * 1.0E17;
		long intTime = Math.round(scaled);
		String secType = null;
		int scalePower = 0;
		if (scaled < 200000.0 && intTime < 100000)
		{
			secType = "f" + unit;
			scalePower = -15;
		} else
		{
			scaled = time * 1.0E14;   intTime = Math.round(scaled);
			if (scaled < 200000.0 && intTime < 100000)
			{
				secType = "p" + unit;
				scalePower = -12;
			} else
			{
				scaled = time * 1.0E11;   intTime = Math.round(scaled);
				if (scaled < 200000.0 && intTime < 100000)
				{
					secType = "n" + unit;
					scalePower = -9;
				} else
				{
					scaled = time * 1.0E8;   intTime = Math.round(scaled);
					if (scaled < 200000.0 && intTime < 100000)
					{
						secType = "u" + unit;
						scalePower = -6;
					} else
					{
						scaled = time * 1.0E5;   intTime = Math.round(scaled);
						if (scaled < 200000.0 && intTime < 100000)
						{
							secType = "m" + unit;
							scalePower = -3;
						} else
						{
							scaled = time * 1.0E2;  intTime = Math.round(scaled);
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
					String tensDigit = "";
					if (timeright < 10) tensDigit = "0";
					return negative + timeleft + "." + tensDigit + timeright + secType;
				}
			}
		}
		scaled /= 1.0E2;
		String numPart = TextUtils.formatDouble(scaled, scalePower - precpower);
		while (numPart.endsWith("0")) numPart = numPart.substring(0, numPart.length()-1);
		if (numPart.endsWith(".")) numPart = numPart.substring(0, numPart.length()-1);
		return negative + numPart + secType;
	}

	// ************************************ SHOWING CROSS-PROBED LEVELS IN EDITWINDOW ************************************

	private HashMap netValues;

	/**
	 * Method to update associated layout windows when the main cursor changes.
	 */
	private void updateAssociatedLayoutWindow()
	{
		// this only works for digital simulation
		if (sd.isAnalog()) return;

		// make sure there is a layout/schematic window being simulated
		WindowFrame oWf = findSchematicsWindow();
		if (oWf == null) return;
		EditWindow schemWnd = (EditWindow)oWf.getContent();

		boolean crossProbeChanged = schemWnd.hasCrossProbeData();
		schemWnd.clearCrossProbeLevels();

		Cell cell = getCell();
//		Netlist netlist = cell.getUserNetlist();
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted crossprobing (network information unavailable).  Please try again");
			return;
		}

		// reset all values on networks
		netValues = new HashMap();

		// assign values from simulation window traces to networks
		for(Iterator it = this.wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (wp.hidden) continue;
			for(Iterator sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				Stimuli.DigitalSignal ds = (Stimuli.DigitalSignal)ws.sSig;
				List bussedSignals = ds.getBussedSignals();
				if (bussedSignals != null)
				{
					// a digital bus trace
					int busWidth = bussedSignals.size();
					for(Iterator bIt = bussedSignals.iterator(); bIt.hasNext(); )
					{
						Stimuli.DigitalSignal subDS = (Stimuli.DigitalSignal)bIt.next();
						putValueOnTrace(subDS, cell, netValues, netlist);
					}
				} else
				{
					// single signal
					putValueOnTrace(ds, cell, netValues, netlist);
				}
			}
		}

		// light up any simulation-probe objects
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() != Generic.tech.simProbeNode) continue;
			Network net = null;
			for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
			{
				Connection con = (Connection)cIt.next();
				net = netlist.getNetwork(con.getArc(), 0);
				break;
			}

			if (net == null) continue;
			Integer state = (Integer)netValues.get(net);
			if (state == null) continue;
			Color col = getHighlightColor(state.intValue());
			schemWnd.addCrossProbeBox(ni.getBounds(), col);
			crossProbeChanged = true;
			netValues.remove(net);
		}

		// redraw all arcs in the layout/schematic window
		for(Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			int width = netlist.getBusWidth(ai);
			for(int i=0; i<width; i++)
			{
				Network net = netlist.getNetwork(ai, i);
				Integer state = (Integer)netValues.get(net);
				if (state == null) continue;
				Color col = getHighlightColor(state.intValue());
				schemWnd.addCrossProbeLine(ai.getHead().getLocation(), ai.getTail().getLocation(), col);
				crossProbeChanged = true;
			}
		}

		// if anything changed, queue the window for redisplay
		if (crossProbeChanged)
			schemWnd.repaint();
	}

	/**
	 * Method to convert a digital state to a color.
	 * The color is used when showing cross-probed levels in the EditWindow.
	 * The colors used to be user-selectable, but are not yet.
	 * @param state the digital state from the Waveform Window.
	 * @return the color to display in the EditWindow.
	 */
	private Color getHighlightColor(int state)
	{
//		if ((sim_window_state&FULLSTATE) != 0)
//		{
//			/* 12-state display: determine trace texture */
//			strength = state & 0377;
//			if (strength == 0) *texture = -1; else
//				if (strength <= NODE_STRENGTH) *texture = 1; else
//					if (strength <= GATE_STRENGTH) *texture = 0; else
//						*texture = 2;
//
//			/* determine trace color */
//			switch (state >> 8)
//			{
//				case LOGIC_LOW:  *color = sim_colorlevellow;     break;
//				case LOGIC_X:    *color = sim_colorlevelundef;   break;
//				case LOGIC_HIGH: *color = sim_colorlevelhigh;    break;
//				case LOGIC_Z:    *color = sim_colorlevelzdef;    break;
//			}
//		} else
		{
			/* only level display, no strength indications */
			switch (state & Stimuli.LOGIC)
			{
				case Stimuli.LOGIC_LOW:
					return Color.BLACK;
				case Stimuli.LOGIC_HIGH:
					return Color.GREEN;
			}
			return Color.RED;
		}
	}

	private void putValueOnTrace(Stimuli.DigitalSignal ds, Cell cell, HashMap netValues, Netlist netlist)
	{
		// set simulation value on the network in the associated layout/schematic window
		Network net = findNetwork(netlist, ds.getSignalName());
		if (net == null) return;

		// find the proper data for the time of the main cursor
		int numEvents = ds.getNumEvents();
		int state = Stimuli.LOGIC_X;
		for(int i=numEvents-1; i>=0; i--)
		{
			double time = ds.getTime(i);
			if (time <= mainTime)
			{
				state = ds.getState(i) & Stimuli.LOGIC;
				break;
			}
		}
		netValues.put(net, new Integer(state));
	}

	// ************************************ PANEL CONTROL ************************************

	/**
	 * Method called when a Panel is to be closed.
	 * @param wp the Panel to close.
	 */
	public void closePanel(Panel wp)
	{
		left.remove(wp.leftHalf);
		right.remove(wp.rightHalf);
		wavePanels.remove(wp);
		rebuildPanelList();
		overall.validate();
		redrawAllPanels();
	}

	/**
	 * Method called when a Panel is to be hidden.
	 * @param wp the Panel to hide.
	 */
	public void hidePanel(Panel wp)
	{
		if (wp.hidden) return;
		wp.hidden = true;
		left.remove(wp.leftHalf);
		right.remove(wp.rightHalf);
		rebuildPanelList();
		overall.validate();
		redrawAllPanels();
	}

	/**
	 * Method called when a Panel is to be shown.
	 * @param wp the Panel to show.
	 */
	public void showPanel(Panel wp)
	{
		if (!wp.hidden) return;
		wp.hidden = false;
		left.add(wp.leftHalf);
		right.add(wp.rightHalf);
		rebuildPanelList();
		overall.validate();
		redrawAllPanels();
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
			wp.setMinimumSize(sz);
			wp.setPreferredSize(sz);

			if (wp.signalButtonsPane != null)
			{
				sz = wp.signalButtonsPane.getSize();
				sz.height = (int)(sz.height * scale);
				wp.signalButtonsPane.setPreferredSize(sz);
				wp.signalButtonsPane.setSize(sz.width, sz.height);
			} else
			{
				sz = wp.leftHalf.getSize();
				sz.height = (int)(sz.height * scale);
				wp.leftHalf.setPreferredSize(sz);
				wp.leftHalf.setMinimumSize(sz);
				wp.leftHalf.setSize(sz.width, sz.height);
			}
		}
		overall.validate();
		redrawAllPanels();
	}

	/**
	 * Method called when the main or extension cursors should be centered.
	 * @param main true for the main cursor, false for the extension cursor.
	 */
	public void centerCursor(boolean main)
	{
		boolean havePanel = false;
		double lowTime = 0, highTime = 0;
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			double low = wp.getMinTimeRange();
			double high = wp.getMaxTimeRange();
			if (havePanel)
			{
				lowTime = Math.max(lowTime, low);
				highTime = Math.min(highTime, high);
			} else
			{
				lowTime = low;
				highTime = high;
				havePanel = true;
			}
		}
		if (!havePanel) return;
		double center = (lowTime + highTime) / 2;
		if (main) setMainTimeCursor(center); else
			setExtensionTimeCursor(center);
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.repaintWithTime();
		}
	}

	/**
	 * Method called to toggle the panel time lock.
	 */
	public void togglePanelTimeLock()
	{
		timeLocked = ! timeLocked;
		if (timeLocked)
		{
			// time now locked: add main time, remove individual time
			timeLock.setIcon(iconLockTime);
			addMainTimePanel();
			double minTime = 0, maxTime = 0;
			boolean first = true;
			for(Iterator it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				wp.removeTimePanel();
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

			// force all panels to be at the same time
			for(Iterator it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				if (wp.minTime != minTime || wp.maxTime != maxTime)
				{
					wp.minTime = minTime;
					wp.maxTime = maxTime;
				}
			}
		} else
		{
			// time is unlocked: put a time bar in each panel, remove main panel
			timeLock.setIcon(iconUnLockTime);
			for(Iterator it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				wp.addTimePanel();
			}
			removeMainTimePanel();
		}
		overall.validate();
		overall.repaint();
	}

	/**
	 * Method to refresh the simulation data from disk.
	 */
	public void refreshData()
	{
		if (sd.getDataType() == null)
		{
			System.out.println("This simulation data did not come from disk...cannot refresh");
			return;
		}
		Simulate.plotSimulationResults(sd.getDataType(), sd.getCell(), sd.getFileURL(), this);
	}

	/**
	 * Method to save the signal ordering on the cell.
	 */
	private void saveSignalOrder()
	{
		Cell cell = getCell();
		if (cell == null) return;
		new SaveSignalOrder(cell, this);
	}

	/**
	 * This class saves the signal order on the cell.
	 */
	private static class SaveSignalOrder extends Job
	{
		private Cell cell;
		private WaveformWindow ww;

		private SaveSignalOrder(Cell cell, WaveformWindow ww)
		{
			super("Save Signal Order", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.ww = ww;
			startJob();
		}

		public boolean doIt()
		{
			List signalList = new ArrayList();
			int total = ww.right.getComponentCount();
			for(int i=0; i<total; i++)
			{
				JPanel rightPart = (JPanel)ww.right.getComponent(i);
				for(Iterator it = ww.wavePanels.iterator(); it.hasNext(); )
				{
					Panel wp = (Panel)it.next();
					if (wp.rightHalf == rightPart)
					{
						StringBuffer sb = new StringBuffer();
						boolean first = true;
						for(Iterator sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
						{
							WaveSignal ws = (WaveSignal)sIt.next();
							String sigName = ws.sSig.getFullName();
							if (first) first = false; else
								sb.append("\t");
							sb.append(sigName);
						}
						if (!first)
							signalList.add(sb.toString());
						break;
					}
				}
			}

			if (signalList.size() == 0)
			{
				if (cell.getVar(WINDOW_SIGNAL_ORDER) != null)
					cell.delVar(WINDOW_SIGNAL_ORDER);
			} else
			{
				String [] strings = new String[signalList.size()];
				int i = 0;
				for(Iterator it = signalList.iterator(); it.hasNext(); )
				{
					strings[i] = (String)it.next();
					i++;
				}
				cell.newVar(WINDOW_SIGNAL_ORDER, strings);
			}
			return true;
		}
	}

	/**
	 * Method called when a new Panel is to be created.
	 */
	private void addNewPanel()
	{
		boolean isAnalog = sd.isAnalog();
		if (isAnalog)
		{
			Rectangle2D bounds = sd.getBounds();
			double lowValue = bounds.getMinY();
			double highValue = bounds.getMaxY();
			double lowTime = bounds.getMinX();
			double highTime = bounds.getMaxX();
			if (this.timeLocked)
			{
				if (wavePanels.size() > 0)
				{
					Panel aPanel = (Panel)wavePanels.get(0);
					lowTime = aPanel.minTime;
					highTime = aPanel.maxTime;
				}
			}
			WaveformWindow.Panel wp = new WaveformWindow.Panel(this, isAnalog);
			wp.setValueRange(lowValue, highValue);
			wp.setTimeRange(lowTime, highTime);
			wp.makeSelectedPanel();
		}
		getPanel().validate();
	}

	/**
	 * Method called to toggle the display of vertex points.
	 */
	private void toggleShowPoints()
	{
		showVertexPoints = !showVertexPoints;
		if (showVertexPoints) showPoints.setIcon(iconPointsOn); else
			showPoints.setIcon(iconPointsOff);
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.repaintWithTime();
		}
	}

	/**
	 * Method called to toggle the display of a grid.
	 */
	public void toggleGridPoints()
	{
		showGrid = !showGrid;
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.repaintWithTime();
		}
	}

	public void addSignal(Stimuli.Signal sig)
	{
		if (sig instanceof Stimuli.AnalogSignal)
		{
			// add analog signal on top of current panel
			for(Iterator it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				if (wp.selected)
				{
					wp.addSignalToPanel(sig);
					break;
				}
			}
		} else
		{
			// add digital signal in new panel
			Panel wp = new Panel(this, false);
			WaveSignal wsig = new WaveSignal(wp, sig);
			overall.validate();
			wp.repaint();
		}
		saveSignalOrder();
	}

	/**
	 * Method to delete the selected signals.
	 */
	public void deleteSelectedSignals()
	{
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!wp.selected) continue;
			if (wp.isAnalog) deleteSignalFromPanel(wp); else
			{
				saveSignalOrder();
				wp.closePanel();
			}
			break;
		}
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
				WaveSignal ws = (WaveSignal)it.next();
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
		saveSignalOrder();
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
		saveSignalOrder();
	}

	public void fillScreen()
	{
		Rectangle2D timeBounds = sd.getBounds();
		double lowTime = timeBounds.getMinX();
		double highTime = timeBounds.getMaxX();
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!timeLocked && !wp.selected) continue;

			Rectangle2D bounds = new Rectangle2D.Double();
			boolean first = true;
			for(Iterator sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				Rectangle2D sigBounds = ws.sSig.getBounds();
				if (first)
				{
					bounds = sigBounds;
					first = false;
				} else
				{
					Rectangle2D.union(bounds, sigBounds, bounds);
				}
			}
			double lowValue = bounds.getMinY();
			double highValue = bounds.getMaxY();
			double valueRange = (highValue - lowValue) / 8;
			if (valueRange == 0) valueRange = 0.5;
			lowValue -= valueRange;
			highValue += valueRange;
			boolean repaint = false;
			if (wp.minTime != lowTime || wp.maxTime != highTime)
			{
				wp.minTime = lowTime;
				wp.maxTime = highTime;
				repaint = true;
			}
			if (wp.isAnalog)
			{
				if (wp.analogLowValue != lowValue || wp.analogHighValue != highValue)
				{
					wp.setValueRange(lowValue, highValue);
					repaint = true;
				}
			}
			if (repaint)
			{
				wp.repaintWithTime();
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
			if (wp.minTime < 0)
			{
				wp.maxTime -= wp.minTime;
				wp.minTime = 0;
			}
			wp.repaintWithTime();
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
			wp.repaintWithTime();
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
				wp.repaintWithTime();
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
		if (mainTimePanel != null)
			mainTimePanel.repaint();
	}

	public void fireCellHistoryStatus()
	{
	}

	/**
	 * Method to initialize for a new text search.
	 * @param search the string to locate.
	 * @param caseSensitive true to match only where the case is the same.
	 */
	public void initTextSearch(String search, boolean caseSensitive,
	                           boolean regExp, Set whatToSearch) {
		System.out.println("Text search not implemented for waveform windows");
	}

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

    /**
     * Method to export directly PNG file
     * @param ep
     * @param filePath
     */
    public void writeImage(ElectricPrinter ep, String filePath)
    {
        BufferedImage img = getOffScreenImage(ep);
        PNG.writeImage(img, filePath);
    }
    
	/**
	 * Method to print window using offscreen canvas
	 * @param ep Image observer plus printable object
	 * @return Printable.NO_SUCH_PAGE or Printable.PAGE_EXISTS
	 */
	public BufferedImage getOffScreenImage(ElectricPrinter ep)
	{
		Graphics2D g2d = (Graphics2D)ep.getGraphics();
		JPanel printArea = wf.getContent().getPanel();
		int iw = (int)ep.getPageFormat().getImageableWidth() * ep.getDesiredDPI() / 72;
		int ih = (int)ep.getPageFormat().getImageableHeight() * ep.getDesiredDPI() / 72;
		BufferedImage bImage = (BufferedImage)(printArea.createImage(iw,ih));

		if (g2d == null)
			g2d = bImage.createGraphics();
		g2d.translate(ep.getPageFormat().getImageableX(), ep.getPageFormat().getImageableY());
		printArea.paint(g2d);

		return bImage;
	}

	/**
	 * Method to pan along X or Y according to fixed amount of ticks
	 * @param direction 0 for horizontal, 1 for vertical.
	 * @param panningAmounts an array of distances, indexed by the current panning distance index.
	 * @param ticks the number of steps to take (usually 1 or -1).
	 */
	public void panXOrY(int direction, double[] panningAmounts, int ticks)
	{
		// determine the panel extent
		double hRange = maxTime - minTime;
		double vRange = -1;
		double vRangeAny = -1;
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			vRangeAny = wp.analogRange;
			if (wp.selected)
			{
				hRange = wp.maxTime - wp.minTime;
				vRange = wp.analogRange;
				break;
			}
		}
		if (vRange < 0) vRange = vRangeAny;

		double distance = ticks * panningAmounts[User.getPanningDistance()];
		for(Iterator it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (direction == 0)
			{
				// pan horizontally
				if (!timeLocked && !wp.selected) continue;
				wp.minTime -= hRange * distance;
				wp.maxTime -= hRange * distance;
			} else
			{
				// pan vertically
				if (!wp.selected) continue;
				wp.analogLowValue -= vRange * distance;
				wp.analogHighValue -= vRange * distance;
			}
			wp.repaintWithTime();
		}
	}
}
