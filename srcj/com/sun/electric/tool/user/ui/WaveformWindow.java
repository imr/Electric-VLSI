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
import com.sun.electric.Main;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.Simulate;
import com.sun.electric.tool.io.output.PNG;
import com.sun.electric.tool.simulation.AnalogSignal;
import com.sun.electric.tool.simulation.DigitalSignal;
import com.sun.electric.tool.simulation.Engine;
import com.sun.electric.tool.simulation.Measurement;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.FindText;
import com.sun.electric.tool.user.dialogs.WaveformZoom;

import java.awt.BasicStroke;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
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
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * This class defines the a screenful of Panels that make up a waveform display.
 */
public class WaveformWindow implements WindowContent, PropertyChangeListener
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
	/** the smallest nonzero value (for log drawing) */		private double smallestXValue;
	/** the signal on all X axes (null for time) */			private AnalogSignal xAxisSignalAll;
	/** the top-level panel of the waveform window. */		private JPanel overall;
	/** left panel: the signal names */						private JPanel left;
	/** right panel: the signal traces */					private JPanel right;
	/** the "lock X axis" button. */						private JButton xAxisLockButton;
	/** the "refresh" button. */							private JButton refresh;
	/** the "show points" button. */						private JButton showPoints;
	/** the "grow panel" button for widening. */			private JButton growPanel;
	/** the "shrink panel" button for narrowing. */			private JButton shrinkPanel;
	/** the list of panels. */								private JComboBox signalNameList;
	/** mapping from signals to entries in "SIGNALS" tree */private HashMap<Signal,DefaultMutableTreeNode> treeNodeFromSignal;
	/** true if rebuilding the list of panels */			private boolean rebuildingSignalNameList = false;
	/** the main scroll of all panels. */					private JScrollPane scrollAll;
	/** the split between signal names and traces. */		private JSplitPane split;
	/** labels for the text at the top */					private JLabel mainPos, extPos, delta, diskLabel;
	/** buttons for centering the X-axis cursors. */		private JButton centerMain, centerExt;
	/** a list of panels in this window */					private List<Panel> wavePanels;
	/** a list of sweep signals in this window */			private List<SweepSignal> sweepSignals;
	/** the main horizontal ruler for all panels. */		private HorizRulerPanel mainHorizRulerPanel;
	/** true to repaint the main horizontal ruler. */		private boolean mainHorizRulerPanelNeedsRepaint;
	/** true if the main horizontal ruler is logarithmic */	private boolean mainHorizRulerPanelLogarithmic;
	/** the VCR timer, when running */						private Timer vcrTimer;
	/** true to run VCR backwards */						private boolean vcrPlayingBackwards = false;
	/** time the VCR last advanced */						private long vcrLastAdvance;
	/** speed of the VCR (in screen pixels) */				private int vcrAdvanceSpeed = 3;
	/** current "main" x-axis cursor */						private double mainXPosition;
	/** current "extension" x-axis cursor */				private double extXPosition;
	/** default range along horozintal axis */				private double minXPosition, maxXPosition;
	/** true if the X axis is the same in each panel */		private boolean xAxisLocked;
	/** the sweep signal that is highlighted */				private int highlightedSweep = -1;
	/** true to show points on vertices (analog only) */	private boolean showVertexPoints;
	/** true to show a grid (analog only) */				private boolean showGrid;
	/** the actual screen coordinates of the waveform */	private int screenLowX, screenHighX;
	/** a listener for redraw requests */					private WaveComponentListener wcl;
	/** The highlighter for this waveform window. */		private Highlighter highlighter;
	private static boolean freezeWaveformHighlighting = false;
	/** The global listener for all waveform windows. */	private static WaveformWindowHighlightListener waveHighlighter = new WaveformWindowHighlightListener();
	/** The color of the grid (a gray) */					private static Color gridColor = new Color(0x808080);
    /** for drawing far-dotted lines */						private static final BasicStroke farDottedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {4,12}, 0);

	/** Font for all text in the window */					private static Font waveWindowFont;
	/** For rendering text */								private static FontRenderContext waveWindowFRC;
	/** The colors of signal lines */						private static Color offStrengthColor, nodeStrengthColor, gateStrengthColor, powerStrengthColor;

	private static WaveFormDropTarget waveformDropTarget = new WaveFormDropTarget();

	private static final ImageIcon iconAddPanel = Resources.getResource(WaveformWindow.class, "ButtonSimAddPanel.gif");
	private static final ImageIcon iconLockXAxes = Resources.getResource(WaveformWindow.class, "ButtonSimLockTime.gif");
	private static final ImageIcon iconUnLockXAxes = Resources.getResource(WaveformWindow.class, "ButtonSimUnLockTime.gif");
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
	private static final Cursor dragXPositionCursor = ToolBar.readCursor("CursorDragTime.gif", 8, 8);

	/**
	 * This class defines a single panel of Signals with an associated list of signal names.
	 */
	public static class Panel extends JPanel
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
		/** the horizontal ruler at the top of this panel. */	private HorizRulerPanel horizRulerPanel;
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
			xAxisSignal = null;
			waveSignals = new HashMap<JButton,WaveSignal>();

			setXAxisRange(waveWindow.minXPosition, waveWindow.maxXPosition);

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
			DropTarget dropTargetRight = new DropTarget(this, DnDConstants.ACTION_LINK, waveformDropTarget, true);

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
			if (!waveWindow.xAxisLocked)
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
			waveWindow.left.add(leftHalf);
			waveWindow.right.add(rightHalf);

			// add to list of wave panels
			waveWindow.wavePanels.add(this);
			if (waveWindow.wavePanels.size() == 1)
			{
				// on the first real addition, redraw any main horizontal ruler panel
				if (waveWindow.mainHorizRulerPanel != null)
				{
					waveWindow.mainHorizRulerPanel.repaint();
					waveWindow.mainHorizRulerPanelNeedsRepaint = true;
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
				for(Iterator<Panel> it = waveWindow.wavePanels.iterator(); it.hasNext(); )
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
			waveWindow.crossProbeWaveformToEditWindow();
		}

		private void addHorizRulerPanel()
		{
			horizRulerPanel = new HorizRulerPanel(this, waveWindow);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;       gbc.gridy = 1;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 1;     gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(0, 0, 0, 0);
			rightHalf.add(horizRulerPanel, gbc);
		}

		private void removeHorizRulerPanel()
		{
			rightHalf.remove(horizRulerPanel);
			horizRulerPanel = null;
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

		private WaveSignal addSignalToPanel(Signal sSig)
		{
			// see if the signal is already there
			for(Iterator<JButton> it = waveSignals.keySet().iterator(); it.hasNext(); )
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
			Collection<WaveSignal> theSignals = waveSignals.values();
			if (theSignals.size() != 1) return;

			// the only signal must be digital
			WaveSignal ws = (WaveSignal)theSignals.iterator().next();
			if (!(ws.sSig instanceof DigitalSignal)) return;

			// the digital signal must be a bus
			DigitalSignal sDSig = (DigitalSignal)ws.sSig;
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
				for(Iterator<Panel> it = waveWindow.wavePanels.iterator(); it.hasNext(); )
					allPanels.add(it.next());

				for(Iterator<Signal> bIt = bussedSignals.iterator(); bIt.hasNext(); )
				{
					DigitalSignal subDS = (DigitalSignal)bIt.next();
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
				int increment = 1;
				for(Iterator<Signal> bIt = bussedSignals.iterator(); bIt.hasNext(); )
				{
					DigitalSignal subDS = (DigitalSignal)bIt.next();
					Panel wp = waveWindow.makeNewPanel(false);
					WaveSignal wsig = new WaveSignal(wp, subDS);

					// remove the panels and put them in the right place
					waveWindow.left.remove(wsig.wavePanel.leftHalf);
					waveWindow.right.remove(wsig.wavePanel.rightHalf);

					Component [] lefts = waveWindow.left.getComponents();
					int destIndex = 0;
					for( ; destIndex < lefts.length; destIndex++)
					{
						if (lefts[destIndex] == leftHalf) break;
					}
					waveWindow.left.add(wsig.wavePanel.leftHalf, destIndex+increment);
					waveWindow.right.add(wsig.wavePanel.rightHalf, destIndex+increment);
					increment++;
				}
			}
			waveWindow.overall.validate();
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
				waveWindow.mainHorizRulerPanel.repaint();
			}
			repaint();
		}

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
					waveWindow.mainHorizRulerPanelNeedsRepaint = true;
			waveWindow.screenLowX = screenLoc.x;
			waveWindow.screenHighX = waveWindow.screenLowX + wid;

			// show the image
			g.setColor(new Color(User.getColorWaveformBackground()));
			g.fillRect(0, 0, wid, hei);

			// draw the grid first (behind the signals)
			if (isAnalog && waveWindow.showGrid)
			{
				Graphics2D g2 = (Graphics2D)g;
				g2.setStroke(Highlight.dottedLine);
				g.setColor(gridColor);

				// draw the vertical grid lines
				double displayedXLow = convertXScreenToData(vertAxisPos);
				double displayedXHigh = convertXScreenToData(wid);
				StepSize ss = getSensibleValues(displayedXHigh, displayedXLow, 10);
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

				ss = getSensibleValues(analogHighValue, analogLowValue, 5);
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
				StepSize ss = getSensibleValues(displayedHigh, displayedLow, 5);
				if (ss.separation != 0.0)
				{
					double value = ss.low;
					g.setFont(waveWindowFont);
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
							GlyphVector gv = waveWindowFont.createGlyphVector(waveWindowFRC, yValue);
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
			int x = convertXDataToScreen(waveWindow.mainXPosition);
			if (x >= vertAxisPos)
				g.drawLine(x, 0, x, hei);
			g2.setStroke(farDottedLine);
			x = convertXDataToScreen(waveWindow.extXPosition);
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
					g.setFont(waveWindowFont);

					// show the low X value and arrow
					String lowXValueString = TextUtils.convertToEngineeringNotation(lowXValue, "s");
					GlyphVector gv = waveWindowFont.createGlyphVector(waveWindowFRC, lowXValueString);
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
					gv = waveWindowFont.createGlyphVector(waveWindowFRC, highXValueString);
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
					gv = waveWindowFont.createGlyphVector(waveWindowFRC, xDiffString);
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
						gv = waveWindowFont.createGlyphVector(waveWindowFRC, lowValueString);
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
						gv = waveWindowFont.createGlyphVector(waveWindowFRC, highValueString);
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
						gv = waveWindowFont.createGlyphVector(waveWindowFRC, valueDiffString);
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

		private static final int CONTROLPOINTSIZE = 6;

		private List<WaveSelection> processSignals(Graphics g, Rectangle2D bounds)
		{
			List<WaveSelection> selectedObjects = null;
			if (bounds != null) selectedObjects = new ArrayList<WaveSelection>();
			sz = getSize();
			int wid = sz.width;
			int hei = sz.height;
			AnalogSignal xSignal = xAxisSignal;
			if (waveWindow.xAxisLocked) xSignal = waveWindow.xAxisSignalAll;
            double[] result = new double[3];
            double[] result2 = new double[3];

			for(Iterator<WaveSignal> it = waveSignals.values().iterator(); it.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)it.next();
				if (g != null) g.setColor(ws.color);
				if (ws.sSig instanceof AnalogSignal)
				{
					// draw analog trace
					AnalogSignal as = (AnalogSignal)ws.sSig;
                    for (int s = 0, numSweeps = as.getNumSweeps(); s < numSweeps; s++)
					{
                        SweepSignal ss = null;
                        if (s < waveWindow.sweepSignals.size())
                            ss = (SweepSignal)waveWindow.sweepSignals.get(s);
                        if (ss != null && !ss.included) continue;
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
								if (waveWindow.showVertexPoints)
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
				if (ws.sSig instanceof DigitalSignal)
				{
					// draw digital traces
					DigitalSignal ds = (DigitalSignal)ws.sSig;
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
									g.setFont(waveWindowFont);
									GlyphVector gv = waveWindowFont.createGlyphVector(waveWindowFRC, valString);
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
								case Stimuli.OFF_STRENGTH:  g.setColor(offStrengthColor);    break;
								case Stimuli.NODE_STRENGTH: g.setColor(nodeStrengthColor);   break;
								case Stimuli.GATE_STRENGTH: g.setColor(gateStrengthColor);   break;
								case Stimuli.VDD_STRENGTH:  g.setColor(powerStrengthColor);  break;
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
				if (g != null) g.setColor(ws.color);

				double [] points = ws.sSig.getControlPoints();
				if (points == null) continue;
				if (g != null) g.setColor(ws.color);
				for(int i=0; i<points.length; i++)
				{
					double xValue = points[i];
					int x = convertXDataToScreen(xValue);
					if (processABox(g, x-CONTROLPOINTSIZE, hei-CONTROLPOINTSIZE*2, x+CONTROLPOINTSIZE, hei,
						bounds, selectedObjects, ws, true, xValue)) break;

					// see if the control point is selected
					boolean found = false;
					if (bounds == null && ws.controlPointsSelected != null)
					{
						for(int j=0; j<ws.controlPointsSelected.length; j++)
							if (ws.controlPointsSelected[j] == xValue) { found = true;   break; }
					}
					if (found)
					{
						g.setColor(Color.GREEN);
						if (processABox(g, x-CONTROLPOINTSIZE+2, hei-CONTROLPOINTSIZE*2+2, x+CONTROLPOINTSIZE-2, hei-2,
							bounds, selectedObjects, ws, true, xValue)) break;
						g.setColor(ws.color);
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
		 * Method to scale a simulation X value to the X coordinate in this window.
		 * @param value the simulation X value.
		 * @return the X coordinate of that simulation value on the screen.
		 */
		private int convertXDataToScreen(double value)
		{
			// see if doing logarithmic axes
			boolean log = waveWindow.mainHorizRulerPanelLogarithmic;
			if (!waveWindow.xAxisLocked) log = horizRulerPanelLogarithmic;
			if (log)
			{
				// logarithmic axes
				if (value <= waveWindow.smallestXValue) value = waveWindow.smallestXValue;
				double logValue = Math.log10(value);
				double winMinX = minXPosition;
				if (winMinX <= 0) winMinX = waveWindow.smallestXValue;
				double logWinMinX = Math.log10(winMinX);
				double winMaxX = maxXPosition;
				if (winMaxX <= 0) winMaxX = waveWindow.smallestXValue;
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
		private double convertXScreenToData(int x)
		{
			// see if doing logarithmic axes
			boolean log = waveWindow.mainHorizRulerPanelLogarithmic;
			if (!waveWindow.xAxisLocked) log = horizRulerPanelLogarithmic;
			if (log)
			{
				// logarithmic axes
				double winMinX = minXPosition;
				if (winMinX <= 0) winMinX = waveWindow.smallestXValue;
				double logWinMinX = Math.log10(winMinX);
				double winMaxX = maxXPosition;
				if (winMaxX <= 0) winMaxX = waveWindow.smallestXValue;
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

		private void clearHighlightedSignals()
		{
			for(Iterator<WaveSignal> it = waveSignals.values().iterator(); it.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)it.next();
				if (!ws.highlighted) continue;
				ws.highlighted = false;
				ws.controlPointsSelected = null;
				if (ws.sigButton != null)
					ws.sigButton.setBackground(background);
			}
			waveWindow.highlightedSweep = -1;
			repaint();
		}

		private void addHighlightedSignal(WaveSignal ws)
		{
			if (ws.sigButton != null)
			{
				if (background == null) background = ws.sigButton.getBackground();
				ws.sigButton.setBackground(new Color(User.getColorWaveformBackground()));
			}
			ws.highlighted = true;
			waveWindow.highlightedSweep = -1;
			repaint();
		}

		private void removeHighlightedSignal(WaveSignal ws)
		{
			ws.highlighted = false;
			if (ws.sigButton != null)
				ws.sigButton.setBackground(background);
			waveWindow.highlightedSweep = -1;
			repaint();
		}

		/**
		 * Method to make this the highlighted Panel.
		 */
		public void makeSelectedPanel()
		{
			for(Iterator<Panel> it = waveWindow.wavePanels.iterator(); it.hasNext(); )
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
						item = new JMenuItem("Logarithmic");
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
			int mainX = convertXDataToScreen(waveWindow.mainXPosition);
			if (Math.abs(mainX - evt.getX()) < 5)
			{
				draggingMain = true;
				return;
			}
			int extX = convertXDataToScreen(waveWindow.extXPosition);
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
				if (!(ws.sSig instanceof AnalogSignal)) continue;

				// draw analog trace
				AnalogSignal as = (AnalogSignal)ws.sSig;
                double[] result = new double[3];
				for(int s=0, numSweeps = as.getNumSweeps(); s<numSweeps; s++)
				{
                    SweepSignal ss = null;
                    if (s < waveWindow.sweepSignals.size())
                        ss = (SweepSignal)waveWindow.sweepSignals.get(s);
                    if (ss != null && !ss.included) continue;
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
				if (!(ws.sSig instanceof AnalogSignal)) continue;

				// draw analog trace
				AnalogSignal as = (AnalogSignal)ws.sSig;
                double[] result = new double[3];
                double[] lastResult = new double[3];
				for(int s=0, numSweeps = as.getNumSweeps(); s<numSweeps; s++)
				{
                    SweepSignal ss = null;
                    if (s < waveWindow.sweepSignals.size())
                        ss = (SweepSignal)waveWindow.sweepSignals.get(s);
                    if (ss != null && !ss.included) continue;
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
							for(Iterator<Panel> it = waveWindow.wavePanels.iterator(); it.hasNext(); )
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
							if (ws.highlighted)
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
				if (waveWindow.xAxisLocked)
				{
					for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
					{
						Panel wp = (Panel)it.next();
						wp.vertAxisPos = evt.getX();
					}
					waveWindow.redrawAllPanels();
					waveWindow.mainHorizRulerPanel.repaint();
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
			int mainX = convertXDataToScreen(waveWindow.mainXPosition);
			int extX = convertXDataToScreen(waveWindow.extXPosition);
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
			for(Iterator<Panel> it = waveWindow.wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				if (!waveWindow.xAxisLocked && wp != this) continue;
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

			for(Iterator<Panel> it = waveWindow.wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				if (!waveWindow.xAxisLocked && wp != this) continue;
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
			// get information about the drop (such as the signal name)
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

			// see if the signal was dropped onto a ruler panel (setting x-axis)
			DropTarget dt = (DropTarget)dtde.getSource();
			if (dt.getComponent() instanceof HorizRulerPanel)
			{
				// dragged a signal to the ruler panel: make that signal the X axis
				if (!sigName.startsWith("PANEL "))
				{
					HorizRulerPanel ttp = (HorizRulerPanel)dt.getComponent();
					Signal sSig = ttp.waveWindow.findSignal(sigName);
					if (sSig != null && sSig instanceof AnalogSignal)
					{
						Rectangle2D bounds = sSig.getBounds();
						if (ttp.wavePanel != null)
						{
							ttp.wavePanel.xAxisSignal = (AnalogSignal)sSig;
							ttp.wavePanel.setXAxisRange(bounds.getMinY(), bounds.getMaxY());
							ttp.wavePanel.repaint();
						} else
						{
							ttp.waveWindow.xAxisSignalAll = (AnalogSignal)sSig;
							ttp.waveWindow.redrawAllPanels();
							for(Iterator<Panel> it = ttp.waveWindow.wavePanels.iterator(); it.hasNext(); )
							{
								Panel wp = (Panel)it.next();
								wp.setXAxisRange(bounds.getMinY(), bounds.getMaxY());
							}
						}
						ttp.repaint();
					}
				}
				dtde.dropComplete(false);
				return;
			}

			// determine which panel was the target of the drop
			WaveformWindow ww = null;
			Panel panel = null;
			if (dt.getComponent() instanceof Panel)
			{
				panel = (Panel)dt.getComponent();
				ww = panel.waveWindow;
			}
			if (dt.getComponent() instanceof OnePanel)
			{
				OnePanel op = (OnePanel)dt.getComponent();
				ww = op.getWaveformWindow();
				panel = op.getPanel();
			}
			if (panel == null)
			{
				dtde.dropComplete(false);
				return;
			}

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
					Signal sSig = null;
					Color oldColor = null;
					for(Iterator<WaveSignal> it = sourcePanel.waveSignals.values().iterator(); it.hasNext(); )
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
			Signal sSig = ww.findSignal(sigName);
			if (sSig == null)
			{
				dtde.dropComplete(false);
				return;
			}

			// digital signals are always added in new panels
			if (sSig instanceof DigitalSignal) panel = null;
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
			if (sSig instanceof AnalogSignal) isAnalog = true;
			panel = ww.makeNewPanel(isAnalog);
			if (isAnalog)
			{
				AnalogSignal as = (AnalogSignal)sSig;
				Rectangle2D rangeBounds = as.getBounds();
				double lowValue = rangeBounds.getMinY();
				double highValue = rangeBounds.getMaxY();
				double range = highValue - lowValue;
				if (range == 0) range = 2;
				double rangeExtra = range / 10;
				panel.setYAxisRange(lowValue - rangeExtra, highValue + rangeExtra);
			}
			WaveSignal wsig = new WaveSignal(panel, sSig);
			ww.overall.validate();
			panel.repaint();
			dtde.dropComplete(true);
		}
	}

	// ************************************* RULER ALONG THE TOP OF EACH PANEL *************************************

	/**
	 * This class defines the horizontal ruler display at the top of each Panel.
	 */
	private static class HorizRulerPanel extends JPanel implements MouseListener
	{
		Panel wavePanel;
		WaveformWindow waveWindow;

		// constructor
		HorizRulerPanel(Panel wavePanel, WaveformWindow waveWindow)
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
			new DropTarget(this, DnDConstants.ACTION_LINK, waveformDropTarget, true);
		}

		/**
		 * Method to repaint this HorizRulerPanel.
		 */
		public void paint(Graphics g)
		{
			Dimension sz = getSize();
			int wid = sz.width;
			int hei = sz.height;
			int offX = 0;
			Panel drawHere = wavePanel;
			Signal xAxisSig = waveWindow.xAxisSignalAll;
			if (drawHere != null)
			{
				xAxisSig = drawHere.xAxisSignal;
			} else
			{
				// this is the main horizontal ruler panel for all panels
				Point screenLoc = getLocationOnScreen();
				offX = waveWindow.screenLowX - screenLoc.x;
				int newWid = waveWindow.screenHighX - waveWindow.screenLowX;

				// because the main horizontal ruler panel needs a Panel (won't work if there aren't any)
				// have to do complex things to request a repaint after adding the first Panel
				if (newWid == 0 || waveWindow.wavePanels.size() == 0)
				{
					if (waveWindow.mainHorizRulerPanelNeedsRepaint)
						repaint();
					return;
				}

				if (offX + newWid > wid) newWid = wid - offX;
				wid = newWid;

				drawHere = (Panel)waveWindow.wavePanels.get(0);
				waveWindow.mainHorizRulerPanelNeedsRepaint = false;
				g.setClip(offX, 0, wid, hei);
			}

			// draw the background
			g.setColor(new Color(User.getColorWaveformBackground()));
			g.fillRect(offX, 0, wid, hei);

			// draw the name of the signal on the horizontal ruler axis
			g.setColor(new Color(User.getColorWaveformForeground()));
			g.setFont(waveWindowFont);
			String xAxisName = "Time";
			if (xAxisSig != null) xAxisName = xAxisSig.getSignalName();
			g.drawLine(drawHere.vertAxisPos + offX, hei-1, wid+offX, hei-1);
			g.drawString(xAxisName, offX+1, hei-6);

			// draw the ruler ticks
			double displayedLow = drawHere.convertXScreenToData(drawHere.vertAxisPos);
			double displayedHigh = drawHere.convertXScreenToData(wid);
			StepSize ss = getSensibleValues(displayedHigh, displayedLow, 10);
			if (ss.separation == 0.0) return;
			double xValue = ss.low;
			int lastX = -1;
			for(;;)
			{
				if (xValue > ss.high) break;
				if (xValue >= displayedLow)
				{
					int x = drawHere.convertXDataToScreen(xValue) + offX;
					g.drawLine(x, 0, x, hei);
					if (lastX >= 0)
					{
						if (x - lastX > 100)
						{
							// add 5 tick marks
							for(int i=1; i<5; i++)
							{
								int intX = (x - lastX) / 5 * i + lastX;
								g.drawLine(intX, hei/2, intX, hei);
							}
						} else if (x - lastX > 25)
						{
							// add 1 tick mark
							int intX = (x - lastX) / 2 + lastX;
							g.drawLine(intX, hei/2, intX, hei);
						}
					}
					String xValueVal = TextUtils.convertToEngineeringNotation(xValue, "s", ss.stepScale);
					g.drawString(xValueVal, x+2, hei-2);
					lastX = x;
				}
				xValue += ss.separation;
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
			item = new JMenuItem("Logarithmic (not yet)");
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
			if (waveWindow.xAxisLocked)
			{
				waveWindow.mainHorizRulerPanelLogarithmic = false;
				waveWindow.mainHorizRulerPanel.repaint();
				waveWindow.redrawAllPanels();
			} else
			{
				wavePanel.horizRulerPanelLogarithmic = false;
				wavePanel.horizRulerPanel.repaint();
				wavePanel.repaint();
			}
		}

		/**
		 * Make this panel show a logarithmic X axis.
		 */
		private void makeLogarithmic()
		{
			if (waveWindow.xAxisLocked)
			{
				waveWindow.mainHorizRulerPanelLogarithmic = true;
				waveWindow.mainHorizRulerPanel.repaint();
				waveWindow.redrawAllPanels();
			} else
			{
				wavePanel.horizRulerPanelLogarithmic = true;
				wavePanel.horizRulerPanel.repaint();
				wavePanel.repaint();
			}
		}

		/**
		 * Make this panel show a time in the X axis.
		 */
		private void restoreTime()
		{
			Rectangle2D dataBounds = waveWindow.sd.getBounds();
			double lowXValue = dataBounds.getMinX();
			double highXValue = dataBounds.getMaxX();

			for(Iterator<Panel> it = waveWindow.wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				if (!waveWindow.xAxisLocked && wp != wavePanel) continue;
				wp.xAxisSignal = null;
				wp.setXAxisRange(lowXValue, highXValue);
				if (wp.horizRulerPanel != null) wp.horizRulerPanel.repaint();
				wp.repaint();
			}
			if (waveWindow.xAxisLocked)
			{
				waveWindow.xAxisSignalAll = null;
				waveWindow.mainHorizRulerPanel.repaint();
				waveWindow.redrawAllPanels();
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
		/** the data for this signal */					private Signal sSig;
		/** the color of this signal */					private Color color;
		/** the x values of selected control points */	private double [] controlPointsSelected;
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

		public WaveSignal(Panel wavePanel, Signal sSig)
		{
			int sigNo = wavePanel.waveSignals.size();
			this.wavePanel = wavePanel;
			this.sSig = sSig;
			controlPointsSelected = null;
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
				color = new Color(User.getColorWaveformStimuli());
				wavePanel.digitalSignalButton.setText(sigName);
				wavePanel.waveSignals.put(wavePanel.digitalSignalButton, this);
				sigButton = wavePanel.digitalSignalButton;
				sigButton.setForeground(color);
			}
		}

		/**
		 * Method to return the actual signal information associated with this line in the waveform window.
		 * @return the actual signal information associated with this line in the waveform window.
		 */
		public Signal getSignal() { return sSig; }

		/**
		 * Method to return the X values of selected control points in this WaveSignal.
		 * @return an array of X values of selected control points in this WaveSignal
		 * (returns null if no control points are selected).
		 */
		public double [] getSelectedControlPoints() { return controlPointsSelected; }

		/**
		 * Method to tell whether this WaveSignal is highlighted in the waveform window.
		 * @return true if this WaveSignal is highlighted in the waveform window.
		 */
		public boolean isSelected() { return highlighted; }

		private void addSelectedControlPoint(double controlXValue)
		{
			if (controlPointsSelected == null)
			{
				// no control points: set this as the only one
				controlPointsSelected = new double[1];
				controlPointsSelected[0] = controlXValue;
				return;
			}

			// see if this X value is already in the list
			for(int i=0; i<controlPointsSelected.length; i++)
				if (controlPointsSelected[i] == controlXValue) return;

			// expand the list and add this X value
			double [] newPoints = new double[controlPointsSelected.length+1];
			for(int i=0; i<controlPointsSelected.length; i++)
				newPoints[i] = controlPointsSelected[i];
			newPoints[controlPointsSelected.length] = controlXValue;
			controlPointsSelected = newPoints;
		}

		private void removeSelectedControlPoint(double controlXValue)
		{
			if (controlPointsSelected == null) return;

			// see if this X value is in the list
			boolean found = false;
			for(int i=0; i<controlPointsSelected.length; i++)
				if (controlPointsSelected[i] == controlXValue) { found = true;   break; }
			if (!found) return;

			// shrink the list and remove this X value
			double [] newPoints = new double[controlPointsSelected.length-1];
			int j = 0;
			for(int i=0; i<controlPointsSelected.length; i++)
			{
				if (controlPointsSelected[i] == controlXValue) continue;
				newPoints[j++] = controlPointsSelected[i];
			}
			controlPointsSelected = newPoints;
		}

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
			ws.wavePanel.waveWindow.crossProbeWaveformToEditWindow();
		}
	}

	/**
	 * Class to define a swept signal.
	 */
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
			included = true;
			sweepIndex = ww.sweepSignals.size();
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
			for(Iterator<Panel> it = ww.wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				wp.repaintWithRulers();
			}
		}

		public void highlight()
		{
			ww.highlightedSweep = sweepIndex;
			for(Iterator<Panel> it = ww.wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				wp.repaintWithRulers();
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
			Stack<Nodable> contextStack = new Stack<Nodable>();
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
			Stack<Nodable> contextStack = new Stack<Nodable>();
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

	private static class WaveformWindowHighlightListener implements HighlightListener
	{
		/**
		 * Method to highlight waveform signals corresponding to circuit networks that are highlighted.
		 * Method is called when any edit window changes its highlighting.
		 */
		public void highlightChanged(Highlighter which)
		{
			// if this is a response to crossprobing from waveform to schematic, stop now
			if (freezeWaveformHighlighting) return;

			// find the EditWindow that this change comes from
			WindowFrame highWF = which.getWindowFrame();
			if (highWF == null) return;
			if (!(highWF.getContent() instanceof EditWindow)) return;
			EditWindow wnd = (EditWindow)highWF.getContent();

			// loop through all windows, looking for waveform windows
			for(Iterator<WindowFrame> wIt = WindowFrame.getWindows(); wIt.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)wIt.next();
				if (!(wf.getContent() instanceof WaveformWindow)) continue;
				WaveformWindow ww = (WaveformWindow)wf.getContent();
				ww.crossProbeEditWindowToWaveform(wnd, which);
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

	public void propertyChange(PropertyChangeEvent e)
	{
		if (e.getPropertyName().equals("dividerLocation"))
		{
			if (mainHorizRulerPanel != null)
			{
				mainHorizRulerPanel.repaint();
//				overall.repaint();
			}
		}
	}

	public WaveformWindow(Stimuli sd, WindowFrame wf)
	{
		// initialize the structure
		this.wf = wf;
		this.sd = sd;
		sd.setWaveformWindow(this);
		resetSweeps();
		wavePanels = new ArrayList<Panel>();
		xAxisLocked = true;
		showVertexPoints = false;
		showGrid = false;
		xAxisSignalAll = null;
		mainHorizRulerPanelLogarithmic = false;

		waveWindowFont = new Font(User.getDefaultFont(), Font.PLAIN, 12);
		waveWindowFRC = new FontRenderContext(null, false, false);
		offStrengthColor = new Color(User.getColorWaveformStrengthOff());
		nodeStrengthColor = new Color(User.getColorWaveformStrengthNode());
		gateStrengthColor = new Color(User.getColorWaveformStrengthGate());
		powerStrengthColor = new Color(User.getColorWaveformStrengthPower());

		highlighter = new Highlighter(Highlighter.SELECT_HIGHLIGHTER, wf);

		// the total panel in the waveform window
		overall = new OnePanel(null, this);
		overall.setLayout(new GridBagLayout());

		wcl = new WaveComponentListener(overall);
		overall.addComponentListener(wcl);

		// the main part of the waveform window: a split-pane between names and traces, put into a scrollpane
		left = new JPanel();
		left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
		right = new JPanel();
		right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
		split.setResizeWeight(0.1);
		split.addPropertyChangeListener(this);

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

		xAxisLockButton = new JButton(iconLockXAxes);
		xAxisLockButton.setBorderPainted(false);
		xAxisLockButton.setDefaultCapable(false);
		xAxisLockButton.setToolTipText("Lock all panels horizontally");
		minWid = new Dimension(iconLockXAxes.getIconWidth()+4, iconLockXAxes.getIconHeight()+4);
		xAxisLockButton.setMinimumSize(minWid);
		xAxisLockButton.setPreferredSize(minWid);
		gbc.gridx = 2;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		overall.add(xAxisLockButton, gbc);
		xAxisLockButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { togglePanelXAxisLock(); }
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

		// the X axis section that shows the value of the main and extension cursors
		JPanel xAxisLabelPanel = new JPanel();
		xAxisLabelPanel.setLayout(new GridBagLayout());
		gbc.gridx = 10;      gbc.gridy = 0;
		gbc.gridwidth = 3;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 4, 0, 4);
		overall.add(xAxisLabelPanel, gbc);

		mainPos = new JLabel("Main:", JLabel.RIGHT);
		mainPos.setToolTipText("The main (dashed) X axis cursor");
		gbc.gridx = 0;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.2;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		xAxisLabelPanel.add(mainPos, gbc);

		centerMain = new JButton("Center");
		centerMain.setToolTipText("Center the main (dashed) X axis cursor");
		gbc.gridx = 1;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.insets = new Insets(2, 4, 2, 0);
		xAxisLabelPanel.add(centerMain, gbc);
		centerMain.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { centerCursor(true); }
		});

		extPos = new JLabel("Ext:", JLabel.RIGHT);
		extPos.setToolTipText("The extension (dotted) X axis cursor");
		gbc.gridx = 2;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.2;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		xAxisLabelPanel.add(extPos, gbc);

		centerExt = new JButton("Center");
		centerExt.setToolTipText("Center the extension (dotted) X axis cursor");
		gbc.gridx = 3;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.insets = new Insets(2, 4, 2, 0);
		xAxisLabelPanel.add(centerExt, gbc);
		centerExt.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { centerCursor(false); }
		});

		delta = new JLabel("Delta:", JLabel.CENTER);
		delta.setToolTipText("X distance between cursors");
		gbc.gridx = 4;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.2;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		xAxisLabelPanel.add(delta, gbc);

		// the name of the waveform disk file
		if (sd.getFileURL() != null)
		{
			String fileName = TextUtils.getFileNameWithoutExtension(sd.getFileURL());
			String ext = TextUtils.getExtension(sd.getFileURL());
			if (ext.length() > 0) fileName += "." + ext;
			diskLabel = new JLabel("File: " + fileName, JLabel.CENTER);
			diskLabel.setToolTipText("The disk file that is being displayed");
			gbc.gridx = 5;       gbc.gridy = 0;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0.4;   gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(0, 10, 0, 0);
			xAxisLabelPanel.add(diskLabel, gbc);
		}

		// add VCR controls
		JButton vcrButtonRewind = new JButton(iconVCRRewind);
		vcrButtonRewind.setBorderPainted(false);
		vcrButtonRewind.setDefaultCapable(false);
		vcrButtonRewind.setToolTipText("Rewind main X axis cursor to start");
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
		vcrButtonPlayBackwards.setToolTipText("Play main X axis cursor backwards");
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
		vcrButtonStop.setToolTipText("Stop moving main X axis cursor");
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
		vcrButtonPlay.setToolTipText("Play main X axis cursor");
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
		vcrButtonToEnd.setToolTipText("Move main X axis cursor to end");
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
		vcrButtonFaster.setToolTipText("Move main X axis cursor faster");
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
		vcrButtonSlower.setToolTipText("Move main X axis cursor slower");
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

		// the single horizontal ruler panel (when the X axes are locked)
		if (xAxisLocked)
		{
			addMainHorizRulerPanel();
		}

		Rectangle2D dataBounds = sd.getBounds();
		smallestXValue = dataBounds.getWidth() / 1000;

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
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
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
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
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
	public Iterator<Panel> getPanels() { return wavePanels.iterator(); }

	/**
	 * Method to return a Panel, given its number.
	 * @param panelNumber the number of the desired Panel.
	 * @return the Panel with that number (null if not found).
	 */
	private Panel getPanelFromNumber(int panelNumber)
	{
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (wp.panelNumber == panelNumber) return wp;
		}
		return null;
	}

	public Engine getSimEngine() { return se; }

	public void setSimEngine(Engine se) { this.se = se; }

	private void addMainHorizRulerPanel()
	{
		mainHorizRulerPanel = new HorizRulerPanel(null, this);
		mainHorizRulerPanel.setToolTipText("One X axis ruler applies to all signals when the X axes are locked");

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 10;      gbc.gridy = 1;
		gbc.gridwidth = 3;   gbc.gridheight = 1;
		gbc.weightx = 1;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		overall.add(mainHorizRulerPanel, gbc);
	}

	private void resetSweeps()
	{
		sweepSignals = new ArrayList<SweepSignal>();
		List<Object> sweeps = sd.getSweepList();
		for(Iterator<Object> it = sweeps.iterator(); it.hasNext(); )
		{
			Object obj = it.next();
			SweepSignal ss = new SweepSignal(obj, this);
		}
	}

	public List<SweepSignal> getSweepSignals() { return sweepSignals; }

	private void removeMainHorizRulerPanel()
	{
		overall.remove(mainHorizRulerPanel);
		mainHorizRulerPanel = null;
	}

	private void togglePanelName()
	{
		if (rebuildingSignalNameList) return;
		String panelName = (String)signalNameList.getSelectedItem();
		int spacePos = panelName.indexOf(' ');
		if (spacePos >= 0) panelName = panelName.substring(spacePos+1);
		int index = TextUtils.atoi(panelName);

		// toggle its state
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
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
		// see if it is time to advance the VCR
		long curtime = System.currentTimeMillis();
		if (curtime - vcrLastAdvance < 100) return;
		vcrLastAdvance = curtime;

		if (wavePanels.size() == 0) return;
		Panel wp = (Panel)wavePanels.iterator().next();
		int xValueScreen = wp.convertXDataToScreen(mainXPosition);
		Rectangle2D bounds = sd.getBounds();
		if (vcrPlayingBackwards)
		{
			int newXValueScreen = xValueScreen - vcrAdvanceSpeed;
			double newXValue = wp.convertXScreenToData(newXValueScreen);
			double lowXValue = bounds.getMinX();
			if (newXValue <= lowXValue)
			{
				newXValue = lowXValue;
				vcrClickStop();
			}
			setMainXPositionCursor(newXValue);
		} else
		{
			int newXValueScreen = xValueScreen + vcrAdvanceSpeed;
			double newXValue = wp.convertXScreenToData(newXValueScreen);
			double highXValue = bounds.getMaxX();
			if (newXValue >= highXValue)
			{
				newXValue = highXValue;
				vcrClickStop();
			}
			setMainXPositionCursor(newXValue);
		}
		redrawAllPanels();
	}

	private void vcrClickRewind()
	{
		vcrClickStop();
		Rectangle2D bounds = sd.getBounds();
		double lowXValue = bounds.getMinX();
		setMainXPositionCursor(lowXValue);
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
		double highXValue = bounds.getMaxX();
		setMainXPositionCursor(highXValue);
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

		List<Panel> panelList = new ArrayList<Panel>();
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
			panelList.add(it.next());
		for(Iterator<Panel> it = panelList.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			boolean redoPanel = false;
			for(Iterator<WaveSignal> pIt = wp.waveSignals.values().iterator(); pIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)pIt.next();
				Signal ss = ws.sSig;
				if (ss.getBussedSignals() != null)
				{
					List<Signal> inBus = ss.getBussedSignals();
					for(int b=0; b<inBus.size(); b++)
					{
						Signal subDS = (Signal)inBus.get(b);
						String oldSigName = subDS.getFullName();
						Signal newBus = null;
						for(Iterator<Signal> sIt = sd.getSignals().iterator(); sIt.hasNext(); )
						{
							Signal newSs = (Signal)sIt.next();
							String newSigName = newSs.getFullName();
							if (!newSigName.equals(oldSigName)) continue;
							newBus = newSs;
							break;
						}
						if (newBus == null)
						{
							inBus.remove(b);
							b--;
							System.out.println("Could not find signal " + oldSigName + " in the new data");
							redoPanel = true;
							continue;
						}
						inBus.set(b, newBus);
					}
				} else
				{
					// single signal: find the name in the new list
					String oldSigName = ss.getFullName();
					ws.sSig = null;
					for(Iterator<Signal> sIt = sd.getSignals().iterator(); sIt.hasNext(); )
					{
						Signal newSs = (Signal)sIt.next();
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
			}
			while (redoPanel)
			{
				redoPanel = false;
				for(Iterator<WaveSignal> pIt = wp.waveSignals.values().iterator(); pIt.hasNext(); )
				{
					WaveSignal ws = (WaveSignal)pIt.next();
					if (ws.sSig == null ||
						(ws.sSig.getBussedSignals() != null && ws.sSig.getBussedSignals().size() == 0))
					{
						redoPanel = true;
if (wp.signalButtons != null)
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
	 * Method to return the stimulus information associated with this WaveformWindow.
	 * @return the stimulus information associated with this WaveformWindow.
	 */
	public Stimuli getSimData() { return sd; }

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
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
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
		wf.measurementExplorerNode = getMeasurementsForExplorer();
		rootNode.add(wf.signalExplorerNode);
		if (wf.sweepExplorerNode != null) rootNode.add(wf.sweepExplorerNode);
		if (wf.measurementExplorerNode != null) rootNode.add(wf.measurementExplorerNode);
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
		HashMap<String,DefaultMutableTreeNode> contextMap = new HashMap<String,DefaultMutableTreeNode>();
		contextMap.put("", signalsExplorerTree);
		List<Signal> signals = sd.getSignals();
		Collections.sort(signals, new SignalsByName());

		treeNodeFromSignal = new HashMap<Signal,DefaultMutableTreeNode>();

		// add branches first
		char separatorChar = sd.getSeparatorChar();
		for(Iterator<Signal> it = signals.iterator(); it.hasNext(); )
		{
			Signal sSig = (Signal)it.next();
			if (sSig.getSignalContext() != null)
				makeContext(sSig.getSignalContext(), contextMap, separatorChar);
		}

		// add all signals to the tree
		for(Iterator<Signal> it = signals.iterator(); it.hasNext(); )
		{
			Signal sSig = (Signal)it.next();
			DefaultMutableTreeNode thisTree = signalsExplorerTree;
			if (sSig.getSignalContext() != null)
				thisTree = makeContext(sSig.getSignalContext(), contextMap, separatorChar);
			DefaultMutableTreeNode sigLeaf = new DefaultMutableTreeNode(sSig);
			thisTree.add(sigLeaf);
			treeNodeFromSignal.put(sSig, sigLeaf);
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
	private DefaultMutableTreeNode makeContext(String branchName, HashMap<String,DefaultMutableTreeNode> contextMap, char separatorChar)
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
	private static class SignalsByName implements Comparator<Signal>
	{
		public int compare(Signal s1, Signal s2)
		{
			return TextUtils.STRING_NUMBER_ORDER.compare(s1.getFullName(), s2.getFullName());
		}
	}

	private DefaultMutableTreeNode getSweepsForExplorer()
	{
		if (sweepSignals.size() <= 0) return null;
		DefaultMutableTreeNode sweepsExplorerTree = new DefaultMutableTreeNode("SWEEPS");
		for(Iterator<SweepSignal> it = sweepSignals.iterator(); it.hasNext(); )
		{
			SweepSignal ss = (SweepSignal)it.next();
			sweepsExplorerTree.add(new DefaultMutableTreeNode(ss));
		}
		return sweepsExplorerTree;
	}

	private DefaultMutableTreeNode getMeasurementsForExplorer()
	{
		List<Measurement> meas = sd.getMeasurements();
		if (meas == null) return null;
		DefaultMutableTreeNode measExplorerTree = new DefaultMutableTreeNode("MEASUREMENTS");
		for(Iterator<Measurement> it = meas.iterator(); it.hasNext(); )
		{
			Measurement m = (Measurement)it.next();
			measExplorerTree.add(new DefaultMutableTreeNode(m.getName()));
		}
		return measExplorerTree;
	}

	private Signal findSignal(String name)
	{
		for(Iterator<Signal> it = sd.getSignals().iterator(); it.hasNext(); )
		{
			Signal sSig = (Signal)it.next();
			String sigName = sSig.getFullName();
			if (sigName.equals(name)) return sSig;
		}
		return null;
	}

	/**
	 * Method to add a selection to the waveform display.
	 * @param h a Highlighter of what is selected.
	 * @param context the context of these networks
	 * (a string to prepend to them to get the actual simulation signal name).
	 * @param newPanel true to create new panels for each signal.
	 */
	public void showSignals(Highlighter h, VarContext context, boolean newPanel)
	{
		List<Signal> found = findSelectedSignals(h, context);

		// determine the current panel
		Panel wp = null;
		for(Iterator<Panel> pIt = wavePanels.iterator(); pIt.hasNext(); )
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
		for(Iterator<Signal> it = found.iterator(); it.hasNext(); )
		{
			Signal sSig = (Signal)it.next();

			// add the signal
			if (newPanel)
			{
				boolean isAnalog = false;
				if (sSig instanceof AnalogSignal) isAnalog = true;
				wp = makeNewPanel(isAnalog);
				if (isAnalog)
				{
					AnalogSignal as = (AnalogSignal)sSig;
					Rectangle2D rangeBounds = as.getBounds();
					double lowValue = rangeBounds.getMinY();
					double highValue = rangeBounds.getMaxY();
					double range = highValue - lowValue;
					if (range == 0) range = 2;
					double rangeExtra = range / 10;
					wp.setYAxisRange(lowValue - rangeExtra, highValue + rangeExtra);
					wp.makeSelectedPanel();
				}
			}

			// check if signal already in panel
			boolean alreadyPlotted = false;
			for(Iterator<WaveSignal> pIt = wp.waveSignals.values().iterator(); pIt.hasNext(); )
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

	/**
	 * Method to move a set of Networks from the waveform display.
	 * @param nets the Set of Networks to remove.
	 * @param context the context of these networks
	 * (a string to prepend to them to get the actual simulation signal name).
	 */
	public void removeSignals(Set<Network> nets, VarContext context)
	{
		for(Iterator<Network> nIt = nets.iterator(); nIt.hasNext(); )
		{
			Network net = (Network)nIt.next();
			String netName = getSpiceNetName(context, net);
			Signal sSig = sd.findSignalForNetwork(netName);

			boolean found = true;
			while (found)
			{
				found = false;
				for(Iterator<Panel> pIt = getPanels(); pIt.hasNext(); )
				{
					Panel wp = (Panel)pIt.next();
					for(Iterator<WaveSignal> it = wp.waveSignals.values().iterator(); it.hasNext(); )
					{
						WaveSignal ws = (WaveSignal)it.next();
						if (ws.sSig != sSig) continue;
						wp.removeHighlightedSignal(ws);
						wp.signalButtons.remove(ws.sigButton);
						wp.waveSignals.remove(ws.sigButton);
						wp.signalButtons.validate();
						wp.signalButtons.repaint();
						wp.repaint();
						found = true;
						break;
					}
					if (found) break;
				}
			}
		}
	}

	public static String getSpiceNetName(Network net)
	{
        return net.getName();
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
        boolean isGlobal = false;

		if (net != null) {
            Netlist netlist = net.getNetlist();
            Network originalNet = net;
			while (net.isExported() && (context != VarContext.globalContext)) {
				// net is exported, find net in parent
				net = getNetworkInParent(net, context.getNodable());
				if (net == null) break;
				context = context.pop();
			}
            // searching in globals
            // Code taken from NCC
            if (net == null)
            {
                Global.Set globNets = netlist.getGlobals();
                for (int i=0; i<globNets.size(); i++)
                {
                    Global g = globNets.get(i);
			        Network netG = netlist.getNetwork(g);
                    if (netG == originalNet)
                    {
                        context = context.pop();
                        net = netG;
                        isGlobal = true;
                        break;
                    }
                }
            }
		}
		// create net name
		String contextStr = context.getInstPath(".");
		contextStr = TextUtils.canonicString(contextStr);
		if (net == null)
			return contextStr;
		else {
			if (context == VarContext.globalContext || isGlobal)
                return getSpiceNetName(net);
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
		for (Iterator<PortProto> it = childCell.getPorts(); it.hasNext(); )
		{
			export = (Export)it.next();
			for (i=0; i<export.getNameKey().busWidth(); i++) {
				Netlist netlist = childCell.acquireUserNetlist();
				if (netlist == null)
				{
					System.out.println("Sorry, a deadlock aborted crossprobing (network information unavailable).  Please try again");
					return null;
				}
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
		Network parentNet = netlist.getNetwork(childNodable, pp, i);
		return parentNet;
	}

	/**
	 * Method to locate a simulation signal in the waveform.
	 * @param sSig the Signal to locate.
	 * @return the displayed WaveSignal where it is in the waveform window.
	 * Returns null if the signal is not being displayed.
	 */
	public WaveSignal findDisplayedSignal(Signal sSig)
	{
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			for(Iterator<WaveSignal> sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
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
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();

			// look at all traces in this panel
			boolean changed = false;
			for(Iterator<WaveSignal> sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
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
	public List<Signal> getHighlightedNetworkNames()
	{
		List<Signal> highlightedSignals = new ArrayList<Signal>();

		// look at all signal names in the cell
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();

			// look at all traces in this panel
			for(Iterator<WaveSignal> sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				if (ws.highlighted) highlightedSignals.add(ws.sSig);
			}
		}

		// also include what is in the SIGNALS tree
		ExplorerTree sigTree = wf.getExplorerTab();
		Object nodeInfo = sigTree.getCurrentlySelectedObject();
		if (nodeInfo != null && nodeInfo instanceof Signal)
		{
			Signal sig = (Signal)nodeInfo;
			highlightedSignals.add(sig);
		}

		return highlightedSignals;
	}

	/**
	 * Method to get a Set of currently highlighted networks in this WaveformWindow.
	 */
	public Set<Network> getHighlightedNetworks()
	{
		// make empty set
		Set<Network> nets = new HashSet<Network>();

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
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();

			// look at all traces in this panel
			for(Iterator<WaveSignal> sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				Network net = findNetwork(netlist, ws.sSig.getSignalName());
				if (net != null) nets.add(net);
			}
		}

		// also include what is in the SIGNALS tree
		ExplorerTree sigTree = wf.getExplorerTab();
		Object nodeInfo = sigTree.getCurrentlySelectedObject();
		if (nodeInfo != null && nodeInfo instanceof Signal)
		{
			Signal sig = (Signal)nodeInfo;
			Network net = findNetwork(netlist, sig.getSignalName());
			if (net != null) nets.add(net);
		}
		return nets;
	}

	// ************************************ THE X AXIS ************************************

	public double getMainXPositionCursor() { return mainXPosition; }

	public void setMainXPositionCursor(double value)
	{
		mainXPosition = value;
		String amount = TextUtils.convertToEngineeringNotation(mainXPosition, "s");
		mainPos.setText("Main: " + amount);
		String diff = TextUtils.convertToEngineeringNotation(Math.abs(mainXPosition - extXPosition), "s");
		delta.setText("Delta: " + diff);
		updateAssociatedLayoutWindow();
	}

	public double getExtensionXPositionCursor() { return extXPosition; }

	public void setExtensionXPositionCursor(double value)
	{
		extXPosition = value;
		String amount = TextUtils.convertToEngineeringNotation(extXPosition, "s");
		extPos.setText("Ext: " + amount);
		String diff = TextUtils.convertToEngineeringNotation(Math.abs(mainXPosition - extXPosition), "s");
		delta.setText("Delta: " + diff);
	}

	/**
	 * Method to create a new panel with an X range similar to others on the display.
	 * @param isAnalog true if the new panel holds analog signals.
	 * @return the newly created Panel.
	 */
	private Panel makeNewPanel(boolean isAnalog)
	{
		// get some other panel to match the X scale
		Panel oPanel = null;
		Iterator<Panel> pIt = getPanels();
		if (pIt.hasNext())
		{
			oPanel = (Panel)pIt.next();
		}
	
		// add this signal in a new panel
		Panel panel = new Panel(this, isAnalog);

		// make its X range match the other panel
		if (oPanel != null) panel.setXAxisRange(oPanel.minXPosition, oPanel.maxXPosition);
		return panel;
	}

	/**
	 * Method to set the X range in all panels.
	 * @param minXPosition the low X value.
	 * @param maxXPosition the high X value.
	 */
	public void setDefaultHorizontalRange(double minXPosition, double maxXPosition)
	{
		this.minXPosition = minXPosition;
		this.maxXPosition = maxXPosition;
	}

	/**
	 * Method to set the zoom extents for this waveform window.
	 * @param lowVert the low value of the vertical axis (for the given panel only).
	 * @param highVert the high value of the vertical axis (for the given panel only).
	 * @param lowHoriz the low value of the horizontal axis (for the given panel only unless X axes are locked).
	 * @param highHoriz the high value of the horizontal axis (for the given panel only unless X axes are locked).
	 * @param thePanel the panel being zoomed.
	 */
	public void setZoomExtents(double lowVert, double highVert, double lowHoriz, double highHoriz, Panel thePanel)
	{
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			boolean changed = false;
			if (wp == thePanel)
			{
				wp.setYAxisRange(lowVert, highVert);
				changed = true;
			}
			if (xAxisLocked || wp == thePanel)
			{
				wp.minXPosition = lowHoriz;
				wp.maxXPosition = highHoriz;
				changed = true;
			}
			if (changed) wp.repaintWithRulers();
		}
	}

	private void redrawAllPanels()
	{
		left.repaint();
		right.repaint();
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
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
		String title = "";
		if (sd.getEngine() != null) title = "Simulation of"; else title = "Waveforms of ";
		if (sd != null && sd.getDataType() != null)
		{
			if (sd.getEngine() != null) title = sd.getDataType().getName() + " simulation of "; else
				title = sd.getDataType().getName() + " of ";
		}
		wf.setTitle(wf.composeTitle(sd.getCell(), title, 0));
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
		if (Math.abs(d/(h+l)) < 0.0000001) d = 0.1;
		int mp = 0;
		while ( d >= 10.0 ) { d /= 10.0;   mp++;   ss.stepScale++; }
		while ( d <= 1.0  ) { d *= 10.0;   mp--;   ss.stepScale--; }
		double m = Math.pow(10, mp);

		int di = (int)d;
		if (di == 0 || m == 0)
		{
			int ww = 9;
		}
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
		if (p <= 0) p = 1;
		String s = TextUtils.formatDouble(v/d, p);
		return s + "e" + i2;
	}

	// ************************************ CROSS-PROBING ************************************

	/**
	 * Method to crossprobe from an EditWindow to this WaveformWindow.
	 * @param wnd the EditWindow that changed.
	 * @param which the Highlighter in that window with current selection.
	 */
	private void crossProbeEditWindowToWaveform(EditWindow wnd, Highlighter which)
	{
		// make sure the windows are associated with each other
		Locator loc = new Locator(wnd, this);
		if (loc.getWaveformWindow() != this) return;

		// start by removing all highlighting in the waveform
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.clearHighlightedSignals();
		}

		// also clear "Signals" tree highlighting
		ExplorerTree tree = wf.getExplorerTab();
		tree.setSelectionPath(null);
		tree.setCurrentlySelectedObject(null);

		// find the signal to show in the waveform window
		List<Signal> found = findSelectedSignals(which, loc.getContext());

		// show it in every panel
		boolean foundSignal = false;
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			for(Iterator<WaveSignal> sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				for(Iterator<Signal> fIt = found.iterator(); fIt.hasNext(); )
				{
					Signal sSig = (Signal)fIt.next();
					if (ws.sSig == sSig)
					{
						wp.addHighlightedSignal(ws);
						foundSignal = true;
					}
				}
			}
		}
		if (foundSignal) repaint();

		// show only one in the "Signals" tree
		Collections.sort(found, new SignalsByName());
		DefaultTreeModel model = (DefaultTreeModel)tree.getTreeModel();
		for(Iterator<Signal> fIt = found.iterator(); fIt.hasNext(); )
		{
			Signal sSig = (Signal)fIt.next();
			Object treeNode = (Object)treeNodeFromSignal.get(sSig);
			if (treeNode != null)
			{
				if (treeNode instanceof DefaultMutableTreeNode)
				{
					DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode)treeNode;
					TreePath selTP = new TreePath(model.getPathToRoot(dmtn));
					tree.setSelectionPath(selTP);
					break;
				}
			}
		}
	}

	/**
	 * Method to return a list of signals that are selected in an EditWindow.
	 * @param h a Highlighter with a selection in an EditWindow.
	 * @param context the VarContext of that window.
	 * @return a List of Signal objects in this WaveformWindow.
	 */
	private List<Signal> findSelectedSignals(Highlighter h, VarContext context)
	{
		List<Signal> found = new ArrayList<Signal>();

		// special case if a current source is selected
		List<Geometric> highlightedObjects = h.getHighlightedEObjs(true, true);
		if (highlightedObjects.size() == 1)
		{
			// if a node is highlighted that has current measured on it, use that
			Geometric geom = (Geometric)highlightedObjects.get(0);
			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				String nodeName = "I(v" + ni.getName();
				Signal sSig = sd.findSignalForNetworkQuickly(nodeName);
				if (sSig != null)
				{
					found.add(sSig);
					return found;
				}
			}
		}

		// convert all networks to signals
		Set<Network> nets = h.getHighlightedNetworks();
		for(Iterator<Network> it = nets.iterator(); it.hasNext(); )
		{
			Network net = (Network)it.next();
			String netName = getSpiceNetName(context, net);
			Signal sSig = sd.findSignalForNetworkQuickly(netName);
			if (sSig == null)
			{
				String netNamePatched = netName.replace('@', '_');
				sSig = sd.findSignalForNetworkQuickly(netNamePatched);
			}
            if (sSig == null) {
                sSig = sd.findSignalForNetwork(netName);
            }
			if (sSig != null) found.add(sSig);
		}
		return found;
	}

	private static Network findNetwork(Netlist netlist, String name)
	{
		// Should really use extended code, found in "simspicerun.cpp:sim_spice_signalname()"
		for(Iterator<Network> nIt = netlist.getNetworks(); nIt.hasNext(); )
		{
			Network net = (Network)nIt.next();
			if (getSpiceNetName(net).equalsIgnoreCase(name)) return net;
		}

		// try converting "@" in network names
		for(Iterator<Network> nIt = netlist.getNetworks(); nIt.hasNext(); )
		{
			Network net = (Network)nIt.next();
			String convertedName = getSpiceNetName(net).replace('@', '_');
			if (convertedName.equalsIgnoreCase(name)) return net;
		}
		return null;
	}

	/**
	 * Method called when signal waveforms change, and equivalent should be shown in the edit window.
	 */
	public void crossProbeWaveformToEditWindow()
	{
		// highlight the net in any associated edit windows
		freezeWaveformHighlighting = true;
		for(Iterator<WindowFrame> wIt = WindowFrame.getWindows(); wIt.hasNext(); )
		{
			WindowFrame wfr = (WindowFrame)wIt.next();
			if (!(wfr.getContent() instanceof EditWindow)) continue;
			EditWindow wnd = (EditWindow)wfr.getContent();
			Locator loc = new Locator(wnd, this);
			if (loc.getWaveformWindow() != this) continue;
			VarContext context = loc.getContext();

			Cell cell = wnd.getCell();
			if (cell == null) continue;
			Highlighter hl = wnd.getHighlighter();
			Netlist netlist = cell.acquireUserNetlist();
			if (netlist == null)
			{
				System.out.println("Sorry, a deadlock aborted crossprobing (network information unavailable).  Please try again");
				return;
			}

			hl.clear();
			for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				for(Iterator<WaveSignal> pIt = wp.waveSignals.values().iterator(); pIt.hasNext(); )
				{
					WaveSignal ws = (WaveSignal)pIt.next();
					if (!ws.highlighted) continue;
					String want = ws.sSig.getFullName();
					Stack<Nodable> upNodables = new Stack<Nodable>();
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
						String desired = want.substring(contextStr.length());
						net = findNetwork(netlist, desired);
						if (net != null)
						{
							// found network
							while (!upNodables.isEmpty())
							{
								Nodable no = (Nodable)upNodables.pop();
								net = HierarchyEnumerator.getNetworkInChild(net, no);
								if (net == null) break;
							}
							if (net != null)
								hl.addNetwork(net, cell);
							break;
						}

						// see if this name is really a current source
						if (desired.startsWith("I(v"))
						{
							NodeInst ni = cell.findNode(desired.substring(3));
							if (ni != null)
								hl.addElectricObject(ni, cell);
						}

						if (context == VarContext.globalContext) break;

						cell = context.getNodable().getParent();
						upNodables.push(context.getNodable());
						context = context.pop();
					}
				}
			}

			// also highlight anything selected in the "SIGNALS" tree
			ExplorerTree sigTree = wf.getExplorerTab();
			Object nodeInfo = sigTree.getCurrentlySelectedObject();
			if (nodeInfo != null && nodeInfo instanceof Signal)
			{
				Signal sig = (Signal)nodeInfo;
				String desired = sig.getSignalName();
				Network net = findNetwork(netlist, desired);
				if (net != null)
				{
					hl.addNetwork(net, cell);
				} else
				{
					// see if this name is really a current source
					if (desired.startsWith("I(v"))
					{
						NodeInst ni = cell.findNode(desired.substring(3));
						if (ni != null)
							hl.addElectricObject(ni, cell);
					}
				}
			}

			hl.finished();
		}
		freezeWaveformHighlighting = false;
	}

	private HashMap<Network,Integer> netValues;

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
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted crossprobing (network information unavailable).  Please try again");
			return;
		}

		// reset all values on networks
		netValues = new HashMap<Network,Integer>();

		// assign values from simulation window traces to networks
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (wp.hidden) continue;
			for(Iterator<WaveSignal> sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				DigitalSignal ds = (DigitalSignal)ws.sSig;
				List<Signal> bussedSignals = ds.getBussedSignals();
				if (bussedSignals != null)
				{
					// a digital bus trace
					int busWidth = bussedSignals.size();
					for(Iterator<Signal> bIt = bussedSignals.iterator(); bIt.hasNext(); )
					{
						DigitalSignal subDS = (DigitalSignal)bIt.next();
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
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() != Generic.tech.simProbeNode) continue;
			Network net = null;
			for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
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
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			int width = netlist.getBusWidth(ai);
			for(int i=0; i<width; i++)
			{
				Network net = netlist.getNetwork(ai, i);
				Integer state = (Integer)netValues.get(net);
				if (state == null) continue;
				Color col = getHighlightColor(state.intValue());
				schemWnd.addCrossProbeLine(ai.getHeadLocation(), ai.getTailLocation(), col);
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
		// determine trace color
		switch (state & Stimuli.LOGIC)
		{
			case Stimuli.LOGIC_LOW:  return new Color(User.getColorWaveformCrossProbeLow());
			case Stimuli.LOGIC_HIGH: return new Color(User.getColorWaveformCrossProbeHigh());
			case Stimuli.LOGIC_X:    return new Color(User.getColorWaveformCrossProbeX());
			case Stimuli.LOGIC_Z:    return new Color(User.getColorWaveformCrossProbeZ());
		}
		return Color.RED;
	}

	private void putValueOnTrace(DigitalSignal ds, Cell cell, HashMap<Network,Integer> netValues, Netlist netlist)
	{
		// set simulation value on the network in the associated layout/schematic window
		Network net = findNetwork(netlist, ds.getSignalName());
		if (net == null) return;

		// find the proper data for the main cursor
		int numEvents = ds.getNumEvents();
		int state = Stimuli.LOGIC_X;
		for(int i=numEvents-1; i>=0; i--)
		{
			double xValue = ds.getTime(i);
			if (xValue <= mainXPosition)
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
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
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
		double lowXValue = 0, highXValue = 0;
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			double low = wp.getMinXAxis();
			double high = wp.getMaxXAxis();
			if (havePanel)
			{
				lowXValue = Math.max(lowXValue, low);
				highXValue = Math.min(highXValue, high);
			} else
			{
				lowXValue = low;
				highXValue = high;
				havePanel = true;
			}
		}
		if (!havePanel) return;
		double center = (lowXValue + highXValue) / 2;
		if (main) setMainXPositionCursor(center); else
			setExtensionXPositionCursor(center);
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.repaintWithRulers();
		}
	}

	/**
	 * Method called to toggle the lock on the horizontal axes.
	 */
	public void togglePanelXAxisLock()
	{
		xAxisLocked = ! xAxisLocked;
		if (xAxisLocked)
		{
			// X axes now locked: add main ruler, remove individual rulers
			xAxisLockButton.setIcon(iconLockXAxes);
			addMainHorizRulerPanel();
			double minXPosition = 0, maxXPosition = 0;
			int vertAxis = 0;
			boolean first = true;
			for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				wp.removeHorizRulerPanel();
				if (first)
				{
					first = false;
					minXPosition = wp.minXPosition;
					maxXPosition = wp.maxXPosition;
					vertAxis = wp.vertAxisPos;
				} else
				{
					if (wp.minXPosition < minXPosition)
					{
						minXPosition = wp.minXPosition;
						maxXPosition = wp.maxXPosition;
					}
					wp.vertAxisPos = vertAxis;
				}
			}

			// force all panels to be at the same X position
			for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				wp.minXPosition = minXPosition;
				wp.maxXPosition = maxXPosition;
			}
		} else
		{
			// X axes are unlocked: put a ruler in each panel, remove main ruler
			xAxisLockButton.setIcon(iconUnLockXAxes);
			for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				wp.addHorizRulerPanel();
			}
			removeMainHorizRulerPanel();
		}
		overall.validate();
		overall.repaint();
	}

	/**
	 * Method to refresh the simulation data from disk.
	 */
	public void refreshData()
	{
		if (se != null)
		{
			se.refresh();
			return;
		}

		if (sd.getDataType() == null)
		{
			System.out.println("This simulation data did not come from disk...cannot refresh");
			return;
		}
		Simulate.plotSimulationResults(sd.getDataType(), sd.getCell(), sd.getFileURL(), this);
	}

	private static HashMap<String,String> savedSignalOrder = new HashMap<String,String>();

	/**
	 * Method to save the signal ordering on the cell.
	 */
	private void saveSignalOrder()
	{
		Cell cell = getCell();
		if (cell == null) return;
		int total = right.getComponentCount();
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<total; i++)
		{
			JPanel rightPart = (JPanel)right.getComponent(i);
			for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				if (wp.rightHalf == rightPart)
				{
					boolean first = true;
					for(Iterator<WaveSignal> sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
					{
						WaveSignal ws = (WaveSignal)sIt.next();
						String sigName = ws.sSig.getFullName();
						if (first) first = false; else
							sb.append("\t");
						sb.append(sigName);
					}
					break;
				}
			}
			sb.append("\n");
		}
		savedSignalOrder.put(cell.getLibrary().getName() + ":" + cell.getName(), sb.toString());
	}

	/**
	 * Method called when the program exits to preserve signal ordering in cells.
	 */
	public static void preserveSignalOrder()
	{
		for(Iterator<String> it = savedSignalOrder.keySet().iterator(); it.hasNext(); )
		{
			String cellName = (String)it.next();
			String savedOrder = (String)savedSignalOrder.get(cellName);
			int colonPos = cellName.indexOf(':');
			if (colonPos < 0) continue;
			Library lib = Library.findLibrary(cellName.substring(0, colonPos));
			if (lib == null) continue;
			cellName = cellName.substring(colonPos+1);
			Pref savedSignalPref = Pref.makeStringPref("SavedSignalsForCell" + cellName, lib.getPrefs(), "");
			savedSignalPref.setString(savedOrder);
		    if (Main.getDebug()) System.err.println("Save waveform signals for cell " + cellName);
		}
	}

	/**
	 * Method to get the saved signal information for a cell.
	 * @param cell the Cell to query.
	 * @return a list of strings, one per waveform window panel, with tab-separated signal names in that panel.
	 * Returns an empty array if nothing is saved.
	 */
	public static String [] getSignalOrder(Cell cell)
	{
		String savedOrder = (String)savedSignalOrder.get(cell.getLibrary().getName() + ":" + cell.getName());
		if (savedOrder == null)
		{
			Pref savedSignalPref = Pref.makeStringPref("SavedSignalsForCell" + cell.getName(), cell.getLibrary().getPrefs(), "");
			savedOrder = savedSignalPref.getString();
			if (savedOrder.length() == 0) return new String[0];
		}

		// convert a single string into an array of strings
		List<String> panels = new ArrayList<String>();
		int startPos = 0;
		for(;;)
		{
			int endCh = savedOrder.indexOf('\n', startPos);
			if (endCh < 0) break;
			String panel = savedOrder.substring(startPos, endCh);
			panels.add(panel);
			startPos = endCh + 1;
		}
		String [] ret = new String[panels.size()];
		int i=0;
		for(Iterator<String> it = panels.iterator(); it.hasNext(); )
			ret[i++] = (String)it.next();
		return ret;
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
			double lowXValue = bounds.getMinX();
			double highXValue = bounds.getMaxX();
			if (xAxisLocked)
			{
				if (wavePanels.size() > 0)
				{
					Panel aPanel = (Panel)wavePanels.get(0);
					lowXValue = aPanel.minXPosition;
					highXValue = aPanel.maxXPosition;
				}
			}
			WaveformWindow.Panel wp = new WaveformWindow.Panel(this, isAnalog);
			wp.setYAxisRange(lowValue, highValue);
			wp.setXAxisRange(lowXValue, highXValue);
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
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.repaintWithRulers();
		}
	}

	/**
	 * Method called to toggle the display of a grid.
	 */
	public void toggleGridPoints()
	{
		showGrid = !showGrid;
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.repaintWithRulers();
		}
	}

	public void addSignal(Signal sig)
	{
		if (sig instanceof AnalogSignal)
		{
			// add analog signal on top of current panel
			for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
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
			Panel wp = makeNewPanel(false);
			WaveSignal wsig = new WaveSignal(wp, sig);
			overall.validate();
			wp.repaint();
		}
		saveSignalOrder();
	}

	/**
	 * Method called when "delete" command (or key) is given.
	 * If a control point is selected, delete it.
	 * If a single signal of an analog window is selected, remove it.
	 */
	public void deleteSelectedSignals()
	{
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!wp.selected) continue;

			for(Iterator<WaveSignal> sIt = wp.getSignals().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				if (ws.controlPointsSelected != null)
				{
					if (se != null)
						se.removeSelectedStimuli();
				}
			}
			if (wp.isAnalog) deleteSignalFromPanel(wp); else
			{
				// do not delete the panel: make them use the "X" button
//				saveSignalOrder();
//				wp.closePanel();
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
			for(Iterator<WaveSignal> it = wp.waveSignals.values().iterator(); it.hasNext(); )
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
		Rectangle2D dataBounds = sd.getBounds();
		double lowXValue = dataBounds.getMinX();
		double highXValue = dataBounds.getMaxX();
		if (xAxisSignalAll != null)
		{
			Rectangle2D sigBounds = xAxisSignalAll.getBounds();
			lowXValue = sigBounds.getMinY();
			highXValue = sigBounds.getMaxY();
		}
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!xAxisLocked && !wp.selected) continue;

			Rectangle2D bounds = new Rectangle2D.Double();
			boolean first = true;
			for(Iterator<WaveSignal> sIt = wp.waveSignals.values().iterator(); sIt.hasNext(); )
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
			if (wp.minXPosition != lowXValue || wp.maxXPosition != highXValue)
			{
				wp.minXPosition = lowXValue;
				wp.maxXPosition = highXValue;
				repaint = true;
			}
			if (wp.isAnalog)
			{
				if (wp.analogLowValue != lowValue || wp.analogHighValue != highValue)
				{
					wp.setYAxisRange(lowValue, highValue);
					repaint = true;
				}
			}
			if (repaint)
			{
				wp.repaintWithRulers();
			}
		}
	}

	public void zoomOutContents()
	{
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!xAxisLocked && !wp.selected) continue;

			boolean timeInXAxis = true;
			if (xAxisLocked)
			{
				if (xAxisSignalAll != null) timeInXAxis = false;
			} else
			{
				if (wp.xAxisSignal != null) timeInXAxis = false;
			}
			boolean repaint = false;
			double range = wp.maxXPosition - wp.minXPosition;
			wp.minXPosition -= range/2;
			wp.maxXPosition += range/2;
			if (wp.minXPosition < 0 && timeInXAxis)
			{
				wp.maxXPosition -= wp.minXPosition;
				wp.minXPosition = 0;
			}
			wp.repaintWithRulers();
		}
	}

	public void zoomInContents()
	{
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!xAxisLocked && !wp.selected) continue;

			boolean repaint = false;
			double range = wp.maxXPosition - wp.minXPosition;
			wp.minXPosition += range/4;
			wp.maxXPosition -= range/4;
			wp.repaintWithRulers();
		}
	}

	public void focusOnHighlighted()
	{
		if (mainXPosition == extXPosition) return;
		double maxXPosition, minXPosition;
		if (mainXPosition > extXPosition)
		{
			double size = (mainXPosition-extXPosition) / 20.0;
			maxXPosition = mainXPosition + size;
			minXPosition = extXPosition - size;
		} else
		{
			double size = (extXPosition-mainXPosition) / 20.0;
			maxXPosition = extXPosition + size;
			minXPosition = mainXPosition - size;
		}
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!xAxisLocked && !wp.selected) continue;
			if (wp.minXPosition != minXPosition || wp.maxXPosition != maxXPosition)
			{
				wp.minXPosition = minXPosition;
				wp.maxXPosition = maxXPosition;
				wp.repaintWithRulers();
			}
		}
	}

	/**
	 * Method to get rid of this WaveformWindow.  Called by WindowFrame when
	 * that windowFrame gets closed.
	 */
	public void finished()
	{
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.finished();
		}
		overall.removeComponentListener(wcl);
        highlighter.delete();
	}

	public void fullRepaint() { repaint(); }

	public void repaint()
	{
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.repaint();
		}
		if (mainHorizRulerPanel != null)
			mainHorizRulerPanel.repaint();
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
		boolean regExp, Set<FindText.WhatToSearch> whatToSearch) {
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
		double hRange = maxXPosition - minXPosition;
		double vRange = -1;
		double vRangeAny = -1;
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			vRangeAny = wp.analogRange;
			if (wp.selected)
			{
				hRange = wp.maxXPosition - wp.minXPosition;
				vRange = wp.analogRange;
				break;
			}
		}
		if (vRange < 0) vRange = vRangeAny;

		double distance = ticks * panningAmounts[User.getPanningDistance()];
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (direction == 0)
			{
				// pan horizontally
				if (!xAxisLocked && !wp.selected) continue;
				wp.minXPosition -= hRange * distance;
				wp.maxXPosition -= hRange * distance;
			} else
			{
				// pan vertically
				if (!wp.selected) continue;
				wp.analogLowValue -= vRange * distance;
				wp.analogHighValue -= vRange * distance;
			}
			wp.repaintWithRulers();
		}
	}
}
