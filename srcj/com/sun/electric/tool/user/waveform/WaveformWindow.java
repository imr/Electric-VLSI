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
package com.sun.electric.tool.user.waveform;

import com.sun.electric.Main;
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
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.Simulate;
import com.sun.electric.tool.io.output.PNG;
import com.sun.electric.tool.simulation.AnalogSignal;
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.simulation.DigitalSignal;
import com.sun.electric.tool.simulation.Engine;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.simulation.TimedSignal;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.FindText;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.ElectricPrinter;
import com.sun.electric.tool.user.ui.ErrorLoggerTree;
import com.sun.electric.tool.user.ui.ExplorerTree;
import com.sun.electric.tool.user.ui.JobTree;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * This class defines the a screenful of Panels that make up a waveform display.
 */
public class WaveformWindow implements WindowContent, PropertyChangeListener
{

	/** the window that this lives in */					private WindowFrame wf;
	/** the cell being simulated */							private Stimuli sd;
	/** the simulation engine that runs in this window. */	private Engine se;
	/** the signal on all X axes (null for time) */			private Signal xAxisSignalAll;
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
	/** the main horizontal ruler for all panels. */		private HorizRuler mainHorizRulerPanel;
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
	/** display mode (0=lines, 1=lines&points, 2=points) */	private int linePointMode;
	/** true to show a grid (analog only) */				private boolean showGrid;
	/** the actual screen coordinates of the waveform */	private int screenLowX, screenHighX;
	/** a listener for redraw requests */					private WaveComponentListener wcl;
	/** The highlighter for this waveform window. */		private Highlighter highlighter;

	/** default height of a digital panel */				private static int panelSizeDigital = 25;
	/** default height of an analog panel */				private static int panelSizeAnalog  = 75;
	/** lock for crossprobing */							private static boolean freezeWaveformHighlighting = false;
	/** The global listener for all waveform windows. */	private static WaveformWindowHighlightListener waveHighlighter = new WaveformWindowHighlightListener();
	/** Font for all text in the window */					private static Font waveWindowFont;
	/** For rendering text */								private static FontRenderContext waveWindowFRC;
	/** The colors of signal lines */						private static Color offStrengthColor, nodeStrengthColor, gateStrengthColor, powerStrengthColor;

	/** drop target (for drag and drop) */					public static WaveFormDropTarget waveformDropTarget = new WaveFormDropTarget();

	private static final ImageIcon iconAddPanel = Resources.getResource(WaveformWindow.class, "ButtonSimAddPanel.gif");
	private static final ImageIcon iconLockXAxes = Resources.getResource(WaveformWindow.class, "ButtonSimLockTime.gif");
	private static final ImageIcon iconUnLockXAxes = Resources.getResource(WaveformWindow.class, "ButtonSimUnLockTime.gif");
	private static final ImageIcon iconRefresh = Resources.getResource(WaveformWindow.class, "ButtonSimRefresh.gif");
	private static final ImageIcon iconLineOnPointOn = Resources.getResource(WaveformWindow.class, "ButtonSimLineOnPointOn.gif");
	private static final ImageIcon iconLineOnPointOff = Resources.getResource(WaveformWindow.class, "ButtonSimLineOnPointOff.gif");
	private static final ImageIcon iconLineOffPointOn = Resources.getResource(WaveformWindow.class, "ButtonSimLineOffPointOn.gif");
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

	/**
	 * Constructor creates a Waveform window in a given WindowFrame with given Stimuli data.
	 * @param sd the Stimuli data to show in the window.
	 * @param wf the WindowFrame in which to place the window.
	 */
	public WaveformWindow(Stimuli sd, WindowFrame wf)
	{
		// initialize the structure
		this.wf = wf;
		this.sd = sd;
		sd.setWaveformWindow(this);
		resetSweeps();
		wavePanels = new ArrayList<Panel>();
		xAxisLocked = true;
		linePointMode = 0;
		showGrid = false;
		xAxisSignalAll = null;
		mainHorizRulerPanelLogarithmic = false;

		// compute static fields used in graphics
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

		// a drop target for the overall waveform window
		DropTarget dropTarget = new DropTarget(overall, DnDConstants.ACTION_LINK, waveformDropTarget, true);

		// the left half has signal names; the right half has waveforms
		left = new JPanel();
		left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
		right = new JPanel();
		right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

		// the main part of the waveform window: a split-pane between names and waveforms, put into a scrollpane
		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
		split.setResizeWeight(0.1);
		split.addPropertyChangeListener(this);
		scrollAll = new JScrollPane(split);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;       gbc.gridy = 2;
		gbc.gridwidth = 11;  gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.BOTH;
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
			gbc = new GridBagConstraints();
			gbc.gridx = 0;       gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.NONE;
			overall.add(addPanel, gbc);
			addPanel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { makeNewPanel(); }
			});

			showPoints = new JButton(iconLineOnPointOff);
			showPoints.setBorderPainted(false);
			showPoints.setDefaultCapable(false);
			showPoints.setToolTipText("Toggle display of vertex points and lines");
			minWid = new Dimension(iconLineOnPointOff.getIconWidth()+4, iconLineOnPointOff.getIconHeight()+4);
			showPoints.setMinimumSize(minWid);
			showPoints.setPreferredSize(minWid);
			gbc = new GridBagConstraints();
			gbc.gridx = 1;       gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.NONE;
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
			gbc = new GridBagConstraints();
			gbc.gridx = 0;       gbc.gridy = 1;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.NONE;
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
		gbc = new GridBagConstraints();
		gbc.gridx = 1;       gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
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
		gbc = new GridBagConstraints();
		gbc.gridx = 2;       gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		overall.add(xAxisLockButton, gbc);
		xAxisLockButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { togglePanelXAxisLock(); }
		});

		signalNameList = new JComboBox();
		signalNameList.setToolTipText("Show or hide waveform panels");
		signalNameList.setLightWeightPopupEnabled(false);
		gbc = new GridBagConstraints();
		gbc.gridx = 3;       gbc.gridy = 0;
		gbc.gridwidth = 5;   gbc.gridheight = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
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
		gbc = new GridBagConstraints();
		gbc.gridx = 8;       gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
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
		gbc = new GridBagConstraints();
		gbc.gridx = 9;       gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		overall.add(shrinkPanel, gbc);
		shrinkPanel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { growPanels(0.8); }
		});

		// the X axis section that shows the value of the main and extension cursors
		JPanel xAxisLabelPanel = new JPanel();
		xAxisLabelPanel.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 10;      gbc.gridy = 0;
		gbc.weightx = 1;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 4, 0, 4);
		overall.add(xAxisLabelPanel, gbc);

		mainPos = new JLabel("Main:", JLabel.RIGHT);
		mainPos.setToolTipText("The main (dashed) X axis cursor");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;       gbc.gridy = 0;
		gbc.weightx = 0.2;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		xAxisLabelPanel.add(mainPos, gbc);

		centerMain = new JButton("Center");
		centerMain.setToolTipText("Center the main (dashed) X axis cursor");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;       gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(2, 4, 2, 0);
		xAxisLabelPanel.add(centerMain, gbc);
		centerMain.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { centerCursor(true); }
		});

		extPos = new JLabel("Ext:", JLabel.RIGHT);
		extPos.setToolTipText("The extension (dotted) X axis cursor");
		gbc = new GridBagConstraints();
		gbc.gridx = 2;       gbc.gridy = 0;
		gbc.weightx = 0.2;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		xAxisLabelPanel.add(extPos, gbc);

		centerExt = new JButton("Center");
		centerExt.setToolTipText("Center the extension (dotted) X axis cursor");
		gbc = new GridBagConstraints();
		gbc.gridx = 3;       gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(2, 4, 2, 0);
		xAxisLabelPanel.add(centerExt, gbc);
		centerExt.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { centerCursor(false); }
		});

		delta = new JLabel("Delta:", JLabel.CENTER);
		delta.setToolTipText("X distance between cursors");
		gbc = new GridBagConstraints();
		gbc.gridx = 4;       gbc.gridy = 0;
		gbc.weightx = 0.2;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
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
			gbc = new GridBagConstraints();
			gbc.gridx = 5;       gbc.gridy = 0;
			gbc.weightx = 0.4;   gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.HORIZONTAL;
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
		gbc = new GridBagConstraints();
		gbc.gridx = 3;       gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
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
		gbc = new GridBagConstraints();
		gbc.gridx = 4;       gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
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
		gbc = new GridBagConstraints();
		gbc.gridx = 5;       gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
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
		gbc = new GridBagConstraints();
		gbc.gridx = 6;       gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
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
		gbc = new GridBagConstraints();
		gbc.gridx = 7;       gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
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
		gbc = new GridBagConstraints();
		gbc.gridx = 8;       gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
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
		gbc = new GridBagConstraints();
		gbc.gridx = 9;       gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
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

		// set bounds of the window from extent of the data
		Rectangle2D dataBounds = sd.getBounds();
		if (dataBounds != null)
		{
			double lowTime = dataBounds.getMinX();
			double highTime = dataBounds.getMaxX();
			double lowValue = dataBounds.getMinY();
			double highValue = dataBounds.getMaxY();
			double timeRange = highTime - lowTime;
			setMainXPositionCursor(timeRange*0.2 + lowTime);
			setExtensionXPositionCursor(timeRange*0.8 + lowTime);
			setDefaultHorizontalRange(lowTime, highTime);
		}
	}

	// ************************************* REQUIRED IMPLEMENTATION METHODS *************************************

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
        if (sd != null)
            sd.finished();
	}

	public void fullRepaint() { repaint(); }

	public void repaint()
	{
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.repaintContents();
		}
		if (mainHorizRulerPanel != null)
			mainHorizRulerPanel.repaint();
	}

	public void fireCellHistoryStatus() {}

	/**
	 * Method to initialize for a new text search.
	 * @param search the string to locate.
	 * @param caseSensitive true to match only where the case is the same.
	 */
	public void initTextSearch(String search, boolean caseSensitive,
		boolean regExp, Set<FindText.WhatToSearch> whatToSearch)
	{
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
			vRangeAny = wp.getYAxisRange();
			if (wp.isSelected())
			{
				hRange = wp.getMaxXAxis() - wp.getMinXAxis();
				vRange = wp.getYAxisRange();
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
				if (!xAxisLocked && !wp.isSelected()) continue;
				double low = wp.getMinXAxis() - hRange * distance;
				double high = wp.getMaxXAxis() - hRange * distance;
				wp.setXAxisRange(low, high);
			} else
			{
				// pan vertically
				if (!wp.isSelected()) continue;
				double low = wp.getYAxisLowValue() - vRange * distance;
				double high = wp.getYAxisHighValue() - vRange * distance;
				wp.setYAxisRange(low, high);
			}
			wp.repaintWithRulers();
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

	public void bottomScrollChanged(int e) {}

	public void rightScrollChanged(int e) {}

	public boolean cellHistoryCanGoBack() { return false; }

	public boolean cellHistoryCanGoForward() { return false; }

	public void cellHistoryGoBack() {}

	public void cellHistoryGoForward() {}

	// ************************************* WINDOW CONTROL *************************************
	
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
	 * Method to return the WindowFrame in which this WaveformWindow lives.
	 * @return the WindowFrame in which this WaveformWindow lives.
	 */
	public WindowFrame getWindowFrame() { return wf; }

	public int getScreenLowX() { return screenLowX; }

	public int getScreenHighX() { return screenHighX; }

	public void setScreenXSize(int lowX, int highX) { screenLowX = lowX;   screenHighX = highX; }

	public JPanel getSignalNamesPanel() { return left; }

	public JPanel getSignalTracesPanel() { return right; }

	public Engine getSimEngine() { return se; }

	public void setSimEngine(Engine se) { this.se = se; }

	// ************************************* CONTROL OF PANELS IN THE WINDOW *************************************

	/**
	 * Method to create a new panel with an X range similar to others on the display.
	 * @param isAnalog true if the new panel holds analog signals.
	 * @return the newly created Panel.
	 */
	public Panel makeNewPanel()
	{
		// determine the X and Y ranges
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
				lowXValue = aPanel.getMinXAxis();
				highXValue = aPanel.getMaxXAxis();
			}
		}

		// determine panel's analysis type
		Analysis.AnalysisType analysisType = null;
		if (sd.isAnalog())
		{
			analysisType = Analysis.ANALYSIS_SIGNALS;
			if (sd.getNumAnalyses() > 0)
				analysisType = sd.getAnalyses().next().getAnalysisType();
			if (xAxisLocked)
			{
				if (xAxisSignalAll != null)
				{
					AnalogSignal as = (AnalogSignal)xAxisSignalAll;
					analysisType = as.getAnalysis().getAnalysisType();
				}
			}
		}
	
		// create the new panel
		Panel panel = new Panel(this, analysisType);

		// set the X and Y ranges
		panel.setXAxisRange(lowXValue, highXValue);
		if (analysisType != null)
			panel.setYAxisRange(lowValue, highValue);

		// show and return the panel
		panel.makeSelectedPanel();
		getPanel().validate();
		return panel;
	}

	/**
	 * Method to return the number of Panels in this WaveformWindow.
	 * @return the number of Panels in this WaveformWindow.
	 */
	public int getNumPanels() { return wavePanels.size(); }

	/**
	 * Method to return the List of Panels in this window.
	 * @return the List of Panels in this window.
	 */
	public void addPanel(Panel panel)
	{
		wavePanels.add(panel);
		if (wavePanels.size() == 1)
		{
			// on the first real addition, redraw any main horizontal ruler panel
			if (getMainHorizRuler() != null)
			{
				getMainHorizRuler().repaint();
				setMainHorizRulerNeedsRepaint(true);
			}
		}
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
			if (wp.getPanelNumber() == panelNumber) return wp;
		}
		return null;
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
			if (wp.getPanelNumber() == index)
			{
				if (wp.isHidden())
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

	public void validatePanel() { overall.validate(); }

	public void rebuildPanelList()
	{
		rebuildingSignalNameList = true;
		signalNameList.removeAllItems();
		boolean hasSignals = false;
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next(); 
			signalNameList.addItem("Panel " + Integer.toString(wp.getPanelNumber()) + (wp.isHidden() ? " (HIDDEN)" : ""));
			hasSignals = true;
		}
		if (hasSignals) signalNameList.setSelectedIndex(0);
		rebuildingSignalNameList = false;
	}

	public void redrawAllPanels()
	{
		left.repaint();
		right.repaint();
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.repaintContents();
		}
	}

	/**
	 * Method called when a Panel is to be closed.
	 * @param wp the Panel to close.
	 */
	public void closePanel(Panel wp)
	{
		left.remove(wp.getLeftHalf());
		right.remove(wp.getRightHalf());
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
		if (wp.isHidden()) return;
		wp.setHidden(true);
		left.remove(wp.getLeftHalf());
		right.remove(wp.getRightHalf());
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
		if (!wp.isHidden()) return;
		wp.setHidden(false);
		left.add(wp.getLeftHalf());
		right.add(wp.getRightHalf());
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

			if (wp.getSignalButtonsPane() != null)
			{
				sz = wp.getSignalButtonsPane().getSize();
				sz.height = (int)(sz.height * scale);
				wp.getSignalButtonsPane().setPreferredSize(sz);
				wp.getSignalButtonsPane().setSize(sz.width, sz.height);
			} else
			{
				sz = wp.getLeftHalf().getSize();
				sz.height = (int)(sz.height * scale);
				wp.getLeftHalf().setPreferredSize(sz);
				wp.getLeftHalf().setMinimumSize(sz);
				wp.getLeftHalf().setSize(sz.width, sz.height);
			}
		}
		overall.validate();
		redrawAllPanels();
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
			for(Iterator<WaveSignal> it = wp.getSignals().iterator(); it.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)it.next();
				if (!ws.isHighlighted()) continue;
				wp.removeHighlightedSignal(ws);
				wp.removeSignal(ws.getButton());
				found = true;
				break;
			}
		}
		wp.getSignalButtons().validate();
		wp.getSignalButtons().repaint();
		wp.repaintContents();
		saveSignalOrder();
	}

	/**
	 * Method called to delete all signals from a Panel.
	 * @param wp the Panel to clear.
	 */
	public void deleteAllSignalsFromPanel(Panel wp)
	{
		wp.clearHighlightedSignals();
		wp.getSignalButtons().removeAll();
		wp.getSignalButtons().validate();
		wp.getSignalButtons().repaint();
		wp.removeAllSignals();
		wp.repaintContents();
		saveSignalOrder();
	}

	/**
	 * Method to return the default height of a digital panel.
	 * @return the default height of a digital panel.
	 */
	public int getPanelSizeDigital() { return panelSizeDigital; }

	/**
	 * Method to return the default height of an analog panel.
	 * @return the default height of an analog panel.
	 */
	public int getPanelSizeAnalog() { return panelSizeAnalog; }

	// ************************************* THE HORIZONTAL RULER *************************************

	public HorizRuler getMainHorizRuler() { return mainHorizRulerPanel; }

	public boolean isMainHorizRulerNeedsRepaint() { return mainHorizRulerPanelNeedsRepaint; }

	public void setMainHorizRulerNeedsRepaint(boolean r) { mainHorizRulerPanelNeedsRepaint = r; }

	public Signal getXAxisSignalAll() { return xAxisSignalAll; }

	public void setXAxisSignalAll(Signal sig) { xAxisSignalAll = sig; }

	private void addMainHorizRulerPanel()
	{
		mainHorizRulerPanel = new HorizRuler(null, this);
		mainHorizRulerPanel.setToolTipText("One X axis ruler applies to all signals when the X axes are locked");

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 10;      gbc.gridy = 1;
		gbc.weightx = 1;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		overall.add(mainHorizRulerPanel, gbc);
	}

	private void removeMainHorizRulerPanel()
	{
		overall.remove(mainHorizRulerPanel);
		mainHorizRulerPanel = null;
	}

	// ************************************* SWEEP CONTROL *************************************

	private void resetSweeps()
	{
		sweepSignals = new ArrayList<SweepSignal>();
		for(Iterator<Analysis> it = sd.getAnalyses(); it.hasNext(); )
		{
			Analysis an = it.next();
			addSweepsForAnalysis(an);
		}
	}

	private void addSweepsForAnalysis(Analysis an)
	{
		List<Object> sweeps = an.getSweepList();
		if (sweeps == null) return;
		for(Iterator<Object> it = sweeps.iterator(); it.hasNext(); )
		{
			Object obj = it.next();
			SweepSignal ss = new SweepSignal(obj, this, an);
		}
	}

	public List<SweepSignal> getSweepSignals() { return sweepSignals; }

	public int getHighlightedSweep() { return highlightedSweep; }

	public void setHighlightedSweep(int sweep) { highlightedSweep = sweep; }

	// ************************************* VCR CONTROL *************************************

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

	public void vcrClickStop()
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

	// ************************************* HIGHLIGHTING *************************************

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
			for(Iterator<WaveSignal> sIt = wp.getSignals().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				if (ws.isHighlighted()) changed = true;
				ws.setHighlighted(false);
			}
			if (changed) wp.repaintContents();
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
			for(Iterator<WaveSignal> sIt = wp.getSignals().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				if (ws.isHighlighted()) highlightedSignals.add(ws.getSignal());
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
			for(Iterator<WaveSignal> sIt = wp.getSignals().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				Network net = findNetwork(netlist, ws.getSignal().getSignalName());
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

	// ************************************* THE EXPLORER TREE *************************************

	public void loadExplorerTree(DefaultMutableTreeNode rootNode)
	{
		wf.libraryExplorerNode = null;

		for(Iterator<Analysis> it = sd.getAnalyses(); it.hasNext(); )
		{
			Analysis an = it.next();
			if (an.getAnalysisType() == Analysis.ANALYSIS_SIGNALS)
			{
				wf.genSignalExplorerNode = getSignalsForExplorer(an, "SIGNALS");
				if (wf.genSignalExplorerNode != null) rootNode.add(wf.genSignalExplorerNode);
			} else if (an.getAnalysisType() == Analysis.ANALYSIS_TRANS)
			{
				wf.transSignalExplorerNode = getSignalsForExplorer(an, "TRANS SIGNALS");
				if (wf.transSignalExplorerNode != null) rootNode.add(wf.transSignalExplorerNode);
				wf.transSweepExplorerNode = getSweepsForExplorer(an, "TRANS SWEEPS");
				if (wf.transSweepExplorerNode != null) rootNode.add(wf.transSweepExplorerNode);
			} else if (an.getAnalysisType() == Analysis.ANALYSIS_AC)
			{
				wf.acSignalExplorerNode = getSignalsForExplorer(an, "AC SIGNALS");
				if (wf.acSignalExplorerNode != null) rootNode.add(wf.acSignalExplorerNode);
				wf.acSweepExplorerNode = getSweepsForExplorer(an, "AC SWEEPS");
				if (wf.acSweepExplorerNode != null) rootNode.add(wf.acSweepExplorerNode);
			} else if (an.getAnalysisType() == Analysis.ANALYSIS_DC)
			{
				wf.dcSignalExplorerNode = getSignalsForExplorer(an, "DC SIGNALS");
				if (wf.dcSignalExplorerNode != null) rootNode.add(wf.dcSignalExplorerNode);
				wf.dcSweepExplorerNode = getSweepsForExplorer(an, "DC SWEEPS");
				if (wf.dcSweepExplorerNode != null) rootNode.add(wf.dcSweepExplorerNode);
			} else if (an.getAnalysisType() == Analysis.ANALYSIS_MEAS)
			{
				wf.measurementExplorerNode = getSignalsForExplorer(an, "MEASUREMENTS");
				if (wf.measurementExplorerNode != null) rootNode.add(wf.measurementExplorerNode);
			}
		}

		// show standard things in the explorer (jobs, errors)
		wf.jobExplorerNode = JobTree.getExplorerTree();
		rootNode.add(wf.jobExplorerNode);
		wf.errorExplorerNode = ErrorLoggerTree.getExplorerTree();
		rootNode.add(wf.errorExplorerNode);
	}

	private DefaultMutableTreeNode getSignalsForExplorer(Analysis an, String analysis)
	{
		List<Signal> signals = an.getSignals();
		if (signals.size() == 0) return null;
		DefaultMutableTreeNode signalsExplorerTree = new DefaultMutableTreeNode(analysis);
		HashMap<String,DefaultMutableTreeNode> contextMap = new HashMap<String,DefaultMutableTreeNode>();
		contextMap.put("", signalsExplorerTree);
		Collections.sort(signals, new SignalsByName());

		treeNodeFromSignal = new HashMap<Signal,DefaultMutableTreeNode>();

		// add branches first
		char separatorChar = sd.getSeparatorChar();
		for(Iterator<Signal> it = signals.iterator(); it.hasNext(); )
		{
			Signal sSig = (Signal)it.next();
			if (!(sSig instanceof TimedSignal)) continue;
			if (sSig.getSignalContext() != null)
				makeContext(sSig.getSignalContext(), contextMap, separatorChar);
		}

		// add all signals to the tree
		for(Iterator<Signal> it = signals.iterator(); it.hasNext(); )
		{
			Signal sSig = (Signal)it.next();
			if (!(sSig instanceof TimedSignal)) continue;
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

	private DefaultMutableTreeNode getSweepsForExplorer(Analysis an, String analysis)
	{
		DefaultMutableTreeNode sweepsExplorerTree = null;
		boolean first = true;
		for(Iterator<SweepSignal> it = sweepSignals.iterator(); it.hasNext(); )
		{
			SweepSignal ss = (SweepSignal)it.next();
			if (ss.getAnalysis() != an) continue;
			if (first)
			{
				first = false;
				sweepsExplorerTree = new DefaultMutableTreeNode(analysis);
			}
			sweepsExplorerTree.add(new DefaultMutableTreeNode(ss));
		}
		return sweepsExplorerTree;
	}

	// ************************************* SIGNALS *************************************

	private Signal findSignal(String name, Analysis an)
	{
		for(Iterator<Signal> it = an.getSignals().iterator(); it.hasNext(); )
		{
			Signal sSig = (Signal)it.next();
			String sigName = sSig.getFullName();
			if (sigName.equals(name)) return sSig;
		}
		return null;
	}

	/**
	 * Method to add an array of signals to the waveform display.
	 * @param sigs the signals to add
	 * @param newPanel true to create new panels for each signal.
	 */
	public void showSignals(Signal [] sigs, boolean newPanel)
	{
		List<Signal> these = new ArrayList<Signal>();
		for(int i=0; i<sigs.length; i++)
			these.add(sigs[i]);
		showTheseSignals(these, newPanel);
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
		showTheseSignals(found, newPanel);
	}

	private void showTheseSignals(List<Signal> found, boolean newPanel)
	{
		// determine the current panel
		Panel wp = null;
		for(Iterator<Panel> pIt = wavePanels.iterator(); pIt.hasNext(); )
		{
			Panel oWp = (Panel)pIt.next();
			if (oWp.isSelected())
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
				wp = makeNewPanel();
				boolean isAnalog = false;
				if (sSig instanceof AnalogSignal) isAnalog = true;
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
					newPanel = false;
				}
			}

			// check if signal already in panel
			boolean alreadyPlotted = false;
			for(Iterator<WaveSignal> pIt = wp.getSignals().iterator(); pIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)pIt.next();
				String name = ws.getSignal().getFullName();
				if (name.equals(sSig.getFullName())) {
					alreadyPlotted = true;
					// add it again, this will increment colors
					WaveSignal.addSignalToPanel(ws.getSignal(), wp);
				}
			}
			if (!alreadyPlotted) {
				WaveSignal wsig = new WaveSignal(wp, sSig);
			}
			added = true;
			wp.repaintContents();
		}
		if (added)
		{
			overall.validate();
			saveSignalOrder();
		}
	}

	/**
	 * Method to remove a set of Networks from the waveform display.
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
			for(Iterator<Analysis> aIt = sd.getAnalyses(); aIt.hasNext(); )
			{
				Analysis an = aIt.next();
				Signal sSig = an.findSignalForNetwork(netName);
				if (sSig == null) continue;
	
				boolean found = true;
				while (found)
				{
					found = false;
					for(Iterator<Panel> pIt = getPanels(); pIt.hasNext(); )
					{
						Panel wp = (Panel)pIt.next();
						if (wp.getAnalysisType() != an.getAnalysisType()) continue;
						for(Iterator<WaveSignal> it = wp.getSignals().iterator(); it.hasNext(); )
						{
							WaveSignal ws = (WaveSignal)it.next();
							if (ws.getSignal() != sSig) continue;
							wp.removeHighlightedSignal(ws);
							wp.removeSignal(ws.getButton());
							wp.getSignalButtons().validate();
							wp.getSignalButtons().repaint();
							wp.repaintContents();
							found = true;
							break;
						}
						if (found) break;
					}
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
			WaveSignal ws = wp.findWaveSignal(sSig);
			if (ws != null) return ws;
		}
		return null;
	}

	// ************************************* THE X AXIS *************************************

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
	 * Method to set the X range in all panels.
	 * @param minXPosition the low X value.
	 * @param maxXPosition the high X value.
	 */
	public void setDefaultHorizontalRange(double minXPosition, double maxXPosition)
	{
		this.minXPosition = minXPosition;
		this.maxXPosition = maxXPosition;
	}

	public double getLowDefaultHorizontalRange() { return minXPosition; }

	public double getHighDefaultHorizontalRange() { return maxXPosition; }

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
				wp.setXAxisRange(lowHoriz, highHoriz);
				changed = true;
			}
			if (changed) wp.repaintWithRulers();
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
					minXPosition = wp.getMinXAxis();
					maxXPosition = wp.getMaxXAxis();
					vertAxis = wp.getVertAxisPos();
				} else
				{
					if (wp.getMinXAxis() < minXPosition)
					{
						minXPosition = wp.getMinXAxis();
						maxXPosition = wp.getMaxXAxis();
					}
					wp.setVertAxisPos(vertAxis);
				}
			}

			// force all panels to be at the same X position
			for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel wp = (Panel)it.next();
				wp.setXAxisRange(minXPosition, maxXPosition);
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

	public boolean isXAxisLocked() { return xAxisLocked; }

	// ************************************* CROSS-PROBING *************************************

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
			for(Iterator<WaveSignal> sIt = wp.getSignals().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				for(Iterator<Signal> fIt = found.iterator(); fIt.hasNext(); )
				{
					Signal sSig = (Signal)fIt.next();
					if (ws.getSignal() == sSig)
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
				for(Iterator<Analysis> it = sd.getAnalyses(); it.hasNext(); )
				{
					Analysis an = it.next();
					Signal sSig = an.findSignalForNetworkQuickly(nodeName);
					if (sSig != null)
					{
						found.add(sSig);
						return found;
					}
				}
			}
		}

		// convert all networks to signals
		Set<Network> nets = h.getHighlightedNetworks();
		for(Iterator<Network> it = nets.iterator(); it.hasNext(); )
		{
			Network net = (Network)it.next();
			String netName = getSpiceNetName(context, net);
			for(Iterator<Analysis> aIt = sd.getAnalyses(); aIt.hasNext(); )
			{
				Analysis an = aIt.next();
				Signal sSig = an.findSignalForNetworkQuickly(netName);
				if (sSig == null)
				{
					String netNamePatched = netName.replace('@', '_');
					sSig = an.findSignalForNetworkQuickly(netNamePatched);
				}
	            if (sSig == null) {
	                sSig = an.findSignalForNetwork(netName);
	            }
				if (sSig != null) found.add(sSig);
			}
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
				for(Iterator<WaveSignal> pIt = wp.getSignals().iterator(); pIt.hasNext(); )
				{
					WaveSignal ws = (WaveSignal)pIt.next();
					if (!ws.isHighlighted()) continue;
					String want = ws.getSignal().getFullName();
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
			if (wp.isHidden()) continue;
			for(Iterator<WaveSignal> sIt = wp.getSignals().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				DigitalSignal ds = (DigitalSignal)ws.getSignal();
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

	// ************************************* STIMULI CONTROL *************************************

	/**
	 * Method to update the Simulation data for this waveform window.
	 * When new data is read from disk, this is used.
	 * @param sd new simulation data for this window.
	 */
	public void setSimData(Stimuli sd)
	{
        if (this.sd != null)
            this.sd.finished();
		this.sd = sd;

		// reload the sweeps
		resetSweeps();

		List<Panel> panelList = new ArrayList<Panel>();
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
			panelList.add(it.next());
		for(Iterator<Panel> it = panelList.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			Analysis an = sd.findAnalysis(wp.getAnalysisType());
			boolean redoPanel = false;
			for(Iterator<WaveSignal> pIt = wp.getSignals().iterator(); pIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)pIt.next();
				Signal ss = ws.getSignal();
				if (ss.getBussedSignals() != null)
				{
					List<Signal> inBus = ss.getBussedSignals();
					for(int b=0; b<inBus.size(); b++)
					{
						Signal subDS = (Signal)inBus.get(b);
						String oldSigName = subDS.getFullName();
						Signal newBus = null;
						for(Iterator<Signal> sIt = an.getSignals().iterator(); sIt.hasNext(); )
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
					ws.setSignal(null);
					for(Iterator<Signal> sIt = an.getSignals().iterator(); sIt.hasNext(); )
					{
						Signal newSs = (Signal)sIt.next();
						String newSigName = newSs.getFullName();
						if (!newSigName.equals(oldSigName)) continue;
						ws.setSignal(newSs);
						break;
					}
					if (ws.getSignal() == null)
					{
						System.out.println("Could not find signal " + oldSigName + " in the new data");
						redoPanel = true;
					}
				}
			}
			while (redoPanel)
			{
				redoPanel = false;
				for(Iterator<WaveSignal> pIt = wp.getSignals().iterator(); pIt.hasNext(); )
				{
					WaveSignal ws = (WaveSignal)pIt.next();
					if (ws.getSignal() == null ||
						(ws.getSignal().getBussedSignals() != null && ws.getSignal().getBussedSignals().size() == 0))
					{
						redoPanel = true;
if (wp.getSignalButtons() != null)
						wp.removeSignal(ws.getButton());
						break;
					}
				}
			}
			if (wp.getNumSignals() == 0)
			{
				// removed all signals: delete the panel
				wp.getWaveWindow().closePanel(wp);
			} else
			{
				if (wp.getSignalButtons() != null)
				{
					wp.getSignalButtons().validate();
					wp.getSignalButtons().repaint();
				}
				wp.repaintContents();
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

	/**
	 * Method to save the waveform window configuration to a disk file.
	 */
	public static void saveConfiguration()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		WaveformWindow ww = findWaveformWindow(cell);
		if (ww == null)
		{
			System.out.println("There is no waveform window to save");
			return;
		}

		String configurationFileName = OpenFile.chooseOutputFile(FileType.TEXT, "Waveform Configuration File", "waveform.txt");
		if (configurationFileName == null) return;
		try
		{
			PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(configurationFileName)));
			int total = ww.right.getComponentCount();
			for(int i=0; i<total; i++)
			{
				JPanel rightPart = (JPanel)ww.right.getComponent(i);
				for(Iterator<Panel> it = ww.wavePanels.iterator(); it.hasNext(); )
				{
					Panel wp = (Panel)it.next();
					if (wp.getRightHalf() == rightPart)
					{
						boolean first = true;
						for(Iterator<WaveSignal> sIt = wp.getSignals().iterator(); sIt.hasNext(); )
						{
							WaveSignal ws = (WaveSignal)sIt.next();
							String sigName = ws.getSignal().getFullName();
							if (first)
							{
								// header
								first = false;
								String analysisName = "";
								if (wp.getAnalysisType() != null) analysisName = " " + wp.getAnalysisType();
								String log = "";
								if (wp.isPanelLogarithmicHorizontally()) log = " xlog";
								if (wp.isPanelLogarithmicVertically()) log += " ylog";
								if (i > 0) printWriter.println();
								printWriter.println("panel" + analysisName + log);
								printWriter.println("zoom " + wp.getYAxisLowValue() + " " + wp.getYAxisHighValue() +
									" " + wp.getMinXAxis() + " " + wp.getMaxXAxis());
								Signal signalInX = ww.xAxisSignalAll;
								if (!ww.xAxisLocked) signalInX = wp.getXAxisSignal();
								if (signalInX != null) printWriter.println("x-axis " + signalInX.getFullName());
							}
							Color color = ws.getColor();
							printWriter.println("signal " + sigName + " " + color.getRed() + "," + color.getGreen() + "," + color.getBlue());
						}
						break;
					}
				}
			}
			printWriter.close();
		} catch (IOException e)
		{
			System.out.println("Error writing configuration");
			return;
		}
		System.out.println("Wrote " + configurationFileName);
	}

	/**
	 * Method to restore the waveform window configuration from a disk file.
	 */
	public static void restoreConfiguration()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		WaveformWindow ww = findWaveformWindow(cell);
		if (ww == null)
		{
			System.out.println("There is no waveform window to restore");
			return;
		}

		String configurationFileName = OpenFile.chooseInputFile(FileType.TEXT, "Waveform Configuration File");
		if (configurationFileName == null) return;

		// clear the display
		List<Panel> closeList = new ArrayList<Panel>();
		for(Iterator<Panel> it = ww.wavePanels.iterator(); it.hasNext(); )
			closeList.add(it.next());
		for(Iterator<Panel> it = closeList.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			ww.closePanel(wp);
		}

		// read the file
		URL url = TextUtils.makeURLToFile(configurationFileName);
		Panel curPanel = null;
		Analysis.AnalysisType oneType = null;
		try
		{
			URLConnection urlCon = url.openConnection();
			InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
			LineNumberReader lineReader = new LineNumberReader(is);
			for(;;)
			{
				String buf = lineReader.readLine();
				if (buf == null) break;
				String [] keywords = buf.split(" ");
				if (keywords.length == 0) continue;
				if (keywords[0].equals("panel"))
				{
					Analysis.AnalysisType analysisType = null;
					boolean xLog = false, yLog = false;
					for(int i=1; i<keywords.length; i++)
					{
						if (keywords[i].equals("xlog")) xLog = true; else
						if (keywords[i].equals("ylog")) yLog = true; else
						{
							analysisType = Analysis.AnalysisType.findAnalysisType(keywords[i]);
							if (analysisType != null)
							{
								if (oneType == null) oneType = analysisType;
								if (oneType != analysisType && ww.isXAxisLocked()) ww.togglePanelXAxisLock();
							}
						}
					}
					curPanel = new Panel(ww, analysisType);
					if (xLog)
					{
						if (ww.isXAxisLocked()) ww.togglePanelXAxisLock();
						curPanel.setPanelLogarithmicHorizontally(true);
					}
					if (yLog) curPanel.setPanelLogarithmicVertically(true);
					continue;
				}
				if (keywords[0].equals("zoom"))
				{
					if (curPanel == null) continue;
					double lowYValue = TextUtils.atof(keywords[1]);
					double highYValue = TextUtils.atof(keywords[2]);
					double lowXValue = TextUtils.atof(keywords[3]);
					double highXValue = TextUtils.atof(keywords[4]);
					curPanel.setXAxisRange(lowXValue, highXValue);
					if (curPanel.getAnalysisType() != null)
						curPanel.setYAxisRange(lowYValue, highYValue);
					continue;
				}
				if (keywords[0].equals("x-axis"))
				{
					if (curPanel == null) continue;
					Stimuli sd = ww.getSimData();
					Analysis an = sd.getAnalyses().next();
					if (curPanel.getAnalysisType() != null) an = sd.findAnalysis(curPanel.getAnalysisType());
					if (an == null) continue;
					Signal sig = an.findSignalForNetwork(keywords[1]);
					if (sig == null) continue;
					if (ww.isXAxisLocked()) ww.togglePanelXAxisLock();
					curPanel.setXAxisSignal(sig);
					continue;					
				}
				if (keywords[0].equals("signal"))
				{
					if (curPanel == null) continue;
					Stimuli sd = ww.getSimData();
					Analysis an = sd.getAnalyses().next();
					if (curPanel.getAnalysisType() != null) an = sd.findAnalysis(curPanel.getAnalysisType());
					if (an == null) continue;
					Signal sig = an.findSignalForNetwork(keywords[1]);
					if (sig == null) continue;
					String [] colorNames = keywords[2].split(",");
					int red = TextUtils.atoi(colorNames[0]);
					int green = TextUtils.atoi(colorNames[1]);
					int blue = TextUtils.atoi(colorNames[2]);
					Color color = new Color(red, green, blue);
					WaveSignal ws = new WaveSignal(curPanel, sig);
					ws.setColor(color);
					continue;
				}
			}
			lineReader.close();
			for(Iterator<Panel> it = ww.wavePanels.iterator(); it.hasNext(); )
			{
				Panel panel = it.next();
				panel.repaintWithRulers();
			}
			ww.saveSignalOrder();
		} catch (IOException e)
		{
			System.out.println("Error reading " + configurationFileName);
			return;
		}
	}

	private static HashMap<String,String> savedSignalOrder = new HashMap<String,String>();

	/**
	 * Method to save the signal ordering on the cell.
	 */
	public void saveSignalOrder()
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
				if (wp.getRightHalf() == rightPart)
				{
					boolean first = true;
					for(Iterator<WaveSignal> sIt = wp.getSignals().iterator(); sIt.hasNext(); )
					{
						WaveSignal ws = (WaveSignal)sIt.next();
						String sigName = ws.getSignal().getFullName();
						if (first)
						{
							// header begins with a tab
							sb.append("\t" + wp.getAnalysisType());
							Signal signalInX = xAxisSignalAll;
							if (!xAxisLocked) signalInX = wp.getXAxisSignal();
							first = false;
							if (signalInX != null) sb.append("(" + signalInX.getFullName() + ")");
						}
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

	// ************************************* DISPLAY CONTROL *************************************

	public Font getFont() { return waveWindowFont; }

	public FontRenderContext getFontRenderContext() { return waveWindowFRC; }

	public Color getOffStrengthColor() { return offStrengthColor; }

	public Color getNodeStrengthColor() { return nodeStrengthColor; }

	public Color getGateStrengthColor() { return gateStrengthColor; }

	public Color getPowerStrengthColor() { return powerStrengthColor; }

	/**
	 * Method called to toggle the display of vertex points.
	 */
	private void toggleShowPoints()
	{
		linePointMode = (linePointMode+1) % 3;
		switch (linePointMode)
		{
			case 0: showPoints.setIcon(iconLineOnPointOff);   break;
			case 1: showPoints.setIcon(iconLineOnPointOn);    break;
			case 2: showPoints.setIcon(iconLineOffPointOn);   break;
		}
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			wp.repaintWithRulers();
		}
	}

	/**
	 * Method to return the drawing mode for analog waves.
	 * @return the drawing mode for analog waves.
	 * 0 means draw lines only; 1 means draw lines and points; 2 means draw points only.
	 */
	public int getLinePointMode() { return linePointMode;}

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

	public boolean isShowGrid() { return showGrid; }

	/**
	 * Method to add a signal to the display.
	 * Called when the user double-clicks on the signal in the explorer tree.
	 * @param sig the Signal to add to the display
	 */
	public void addSignal(Signal sig)
	{
		if (sig instanceof AnalogSignal)
		{
			// add analog signal on top of current panel
			AnalogSignal as = (AnalogSignal)sig;
			for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
			{
				Panel panel = (Panel)it.next();
				if (panel.isSelected())
				{
					if (as.getAnalysis().getAnalysisType() != panel.getAnalysisType())
					{
						JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
							"Cannot drop a " + as.getAnalysis().getAnalysisType() + " signal onto a " + panel.getAnalysisType() + " panel.  " +
							"First convert the panel with the popup in the upper-left.",
							"Error Displaying Signals", JOptionPane.ERROR_MESSAGE);
						return;					
					}
					WaveSignal.addSignalToPanel(sig, panel);
					break;
				}
			}
		} else
		{
			// add digital signal in new panel
			Panel wp = makeNewPanel();
			WaveSignal wsig = new WaveSignal(wp, sig);
			overall.validate();
			wp.repaintContents();
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
			if (!wp.isSelected()) continue;

			for(Iterator<WaveSignal> sIt = wp.getSignals().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				if (ws.getSelectedControlPoints() != null)
				{
					if (se != null)
						se.removeSelectedStimuli();
				}
			}
			if (wp.getAnalysisType() != null) deleteSignalFromPanel(wp); else
			{
				// do not delete the panel: make them use the "X" button
//				saveSignalOrder();
//				wp.closePanel();
			}
			break;
		}
	}

	public void fillScreen()
	{
		// accumulate bounds for all displayed panels
		Rectangle2D xBounds = null;
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (wp.getXAxisSignal() != null) continue;
			Analysis an = sd.findAnalysis(wp.getAnalysisType());
			Rectangle2D bounds = an.getBounds();
			if (bounds == null) continue;
			if (xBounds == null)
			{
				xBounds = new Rectangle2D.Double(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
			} else
			{
				Rectangle2D.union(xBounds, bounds, xBounds);
			}
		}

		// if there is an overriding signal on the X axis, use its bounds
		if (xAxisSignalAll != null)
		{
			Rectangle2D sigBounds = xAxisSignalAll.getBounds();
			xBounds.setRect(sigBounds.getMinY(), sigBounds.getMinX(), sigBounds.getHeight(), sigBounds.getWidth());
		}

		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!xAxisLocked)
			{
				if (!wp.isSelected()) continue;

				// when time is not locked, compute bounds for this panel only
				Analysis an = sd.findAnalysis(wp.getAnalysisType());
				Rectangle2D anBounds = an.getBounds();
				if (anBounds != null)
					xBounds.setRect(anBounds.getMinX(), anBounds.getMinY(), anBounds.getWidth(), anBounds.getHeight());
				if (wp.getXAxisSignal() != null)
				{
					Rectangle2D sigBounds = wp.getXAxisSignal().getBounds();
					xBounds.setRect(sigBounds.getMinY(), sigBounds.getMinX(), sigBounds.getHeight(), sigBounds.getWidth());
				}
			}

			Rectangle2D yBounds = null;
			for(Iterator<WaveSignal> sIt = wp.getSignals().iterator(); sIt.hasNext(); )
			{
				WaveSignal ws = (WaveSignal)sIt.next();
				Rectangle2D sigBounds = ws.getSignal().getBounds();
				if (yBounds == null)
				{
					yBounds = new Rectangle2D.Double(sigBounds.getMinX(), sigBounds.getMinY(), sigBounds.getWidth(), sigBounds.getHeight());
				} else
				{
					Rectangle2D.union(yBounds, sigBounds, yBounds);
				}
			}
			if (yBounds == null)
			{
				Analysis an = sd.findAnalysis(wp.getAnalysisType());
				Rectangle2D anBounds = an.getBounds();
				if (anBounds != null)
					yBounds = new Rectangle2D.Double(anBounds.getMinX(), anBounds.getMinY(), anBounds.getWidth(), anBounds.getHeight());
			}
			boolean repaint = false;
			if (xBounds != null)
			{
				if (wp.getMinXAxis() != xBounds.getMinX() || wp.getMaxXAxis() != xBounds.getMaxX())
				{
					wp.setXAxisRange(xBounds.getMinX(), xBounds.getMaxX());
					repaint = true;
				}
			}
			if (yBounds != null)
			{
				double lowValue = yBounds.getMinY();
				double highValue = yBounds.getMaxY();
				double valueRange = (highValue - lowValue) / 8;
				if (valueRange == 0) valueRange = 0.5;
				lowValue -= valueRange;
				highValue += valueRange;
				if (wp.getYAxisLowValue() != lowValue || wp.getYAxisHighValue() != highValue)
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
			if (!xAxisLocked && !wp.isSelected()) continue;

			boolean timeInXAxis = true;
			if (xAxisLocked)
			{
				if (xAxisSignalAll != null) timeInXAxis = false;
			} else
			{
				if (wp.getXAxisSignal() != null) timeInXAxis = false;
			}
			boolean repaint = false;
			double range = wp.getMaxXAxis() - wp.getMinXAxis();
			wp.setXAxisRange(wp.getMinXAxis() - range/2, wp.getMaxXAxis() + range/2);
			if (wp.getMinXAxis() < 0 && timeInXAxis)
			{
				wp.setXAxisRange(0, wp.getMaxXAxis() - wp.getMinXAxis());
			}
			wp.repaintWithRulers();
		}
	}

	public void zoomInContents()
	{
		for(Iterator<Panel> it = wavePanels.iterator(); it.hasNext(); )
		{
			Panel wp = (Panel)it.next();
			if (!xAxisLocked && !wp.isSelected()) continue;

			boolean repaint = false;
			double range = wp.getMaxXAxis() - wp.getMinXAxis();
			wp.setXAxisRange(wp.getMinXAxis() + range/4, wp.getMaxXAxis() - range/4);
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
			if (!xAxisLocked && !wp.isSelected()) continue;
			if (wp.getMinXAxis() != minXPosition || wp.getMaxXAxis() != maxXPosition)
			{
				wp.setXAxisRange(minXPosition, maxXPosition);
				wp.repaintWithRulers();
			}
		}
	}

	public boolean isWaveWindowLogarithmic() { return mainHorizRulerPanelLogarithmic; }

	public void setWaveWindowLogarithmic(boolean logarithmic)
	{
		mainHorizRulerPanelLogarithmic = logarithmic;
		mainHorizRulerPanel.repaint();
	}

	// ************************************* DRAG AND DROP CLASSES *************************************

	/**
	 * This class extends JPanel so that components of the Waveform window can be identified by the Drag and Drop system.
	 */
	public static class OnePanel extends JPanel
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
			String sigNameData = (String)data;
			String [] sigNames = sigNameData.split("\n");
			Analysis.AnalysisType analysisType = null;
			for(int i=0; i<sigNames.length; i++)
			{
				Analysis.AnalysisType anAnalysisType = Analysis.ANALYSIS_SIGNALS;
				String aSigName = sigNames[i];
				if (aSigName.startsWith("TRANS "))
				{
					sigNames[i] = aSigName.substring(6);
					anAnalysisType = Analysis.ANALYSIS_TRANS;
				} else if (aSigName.startsWith("MEASUREMENT "))
				{
					sigNames[i] = aSigName.substring(12);
					anAnalysisType = Analysis.ANALYSIS_MEAS;
				} else if (aSigName.startsWith("AC "))
				{
					sigNames[i] = aSigName.substring(3);
					anAnalysisType = Analysis.ANALYSIS_AC;
				} else if (aSigName.startsWith("DC "))
				{
					sigNames[i] = aSigName.substring(3);
					anAnalysisType = Analysis.ANALYSIS_DC;
				}
				if (analysisType == null) analysisType = anAnalysisType; else
				{
					if (analysisType != anAnalysisType)
					{
						Main.getUserInterface().showErrorMessage("All signals must be the same type", "Incorrect Signal Selection");
						dtde.dropComplete(false);
						return;
					}
				}
			}
			if (analysisType == null)
			{
				dtde.dropComplete(false);
				return;
			}

			// see if the signal was dropped onto a ruler panel (setting x-axis)
			DropTarget dt = (DropTarget)dtde.getSource();
			if (dt.getComponent() instanceof HorizRuler)
			{
				// make sure only one signal was selected
				if (sigNames.length != 1)
				{
					Main.getUserInterface().showErrorMessage("Only one signal can be dragged to a ruler", "Too Much Selected");
					dtde.dropComplete(false);
					return;			
				}

				// dragged a signal to the ruler panel: make that signal the X axis
				if (!sigNames[0].startsWith("PANEL "))
				{
					HorizRuler hr = (HorizRuler)dt.getComponent();
					Panel panel = hr.getPanel();
					WaveformWindow ww = hr.getWaveformWindow();
					Analysis an = ww.getSimData().findAnalysis(analysisType);
					if (an == null)
					{
						System.out.println("Cannot find " + analysisType + " data");
						dtde.dropComplete(true);
						return;
					}
					Signal sSig = ww.findSignal(sigNames[0], an);
					if (sSig != null)
					{
						Rectangle2D bounds = sSig.getBounds();
						if (panel == null)
						{
							// dropped signal onto main time ruler: make sure it is the right type
							boolean warn = false;
							for(Iterator<Panel> it = ww.wavePanels.iterator(); it.hasNext(); )
							{
								Panel wp = (Panel)it.next();
								if (wp.getAnalysisType() != analysisType && wp.getNumSignals() > 0) warn = true;
							}
							if (warn)
							{
								String warning = "The waveform window is not showing " + analysisType +
									" data.  Remove all traces and convert panels to show " + analysisType + " data?";
								int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(), warning);
								if (response != JOptionPane.YES_OPTION)
								{
									dtde.dropComplete(true);
									return;
								}
								for(Iterator<Panel> it = ww.wavePanels.iterator(); it.hasNext(); )
								{
									Panel wp = (Panel)it.next();
									ww.deleteAllSignalsFromPanel(wp);
								}
							}
							ww.xAxisSignalAll = sSig;
							for(Iterator<Panel> it = ww.wavePanels.iterator(); it.hasNext(); )
							{
								Panel wp = (Panel)it.next();
								wp.setAnalysisType(analysisType);
								wp.setXAxisRange(bounds.getMinY(), bounds.getMaxY());
							}
							ww.redrawAllPanels();
						} else
						{
							// dropped signal onto a single panel's time ruler
							if (panel.getAnalysisType() != analysisType)
							{
								JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
									"Cannot drop a " + analysisType + " signal onto the horizontal ruler of a " +
									panel.getAnalysisType() + " panel.  " +
									"First convert the panel with the popup in the upper-left.",
									"Error Displaying Signals", JOptionPane.ERROR_MESSAGE);
								dtde.dropComplete(true);
								return;
							}
							panel.setXAxisSignal(sSig);
							panel.setXAxisRange(bounds.getMinY(), bounds.getMaxY());
							panel.repaintContents();
						}
						hr.repaint();
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
				ww = panel.getWaveWindow();
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
			if (sigNames[0].startsWith("PANEL "))
			{
				// rearranging signals and panels
				int panelNumber = TextUtils.atoi(sigNames[0].substring(6));
				Panel sourcePanel = ww.getPanelFromNumber(panelNumber);
				if (sourcePanel == panel)
				{
					// moved to same panel
					dtde.dropComplete(false);
					return;
				}

				// see if a signal button was grabbed
				int sigPos = sigNames[0].indexOf("BUTTON ");
				if (panel.getAnalysisType() == null) sigPos = -1;
				if (sigPos < 0)
				{
					// moving the entire panel
					ww.left.remove(sourcePanel.getLeftHalf());
					ww.right.remove(sourcePanel.getRightHalf());

					int destIndex = 0;
					Component [] lefts = ww.left.getComponents();
					for(destIndex=0; destIndex < lefts.length; destIndex++)
					{
						if (lefts[destIndex] == panel.getLeftHalf()) break;
					}
					if (dtde.getLocation().y > panel.getBounds().height/2)
					{
						destIndex++;
					}
					ww.left.add(sourcePanel.getLeftHalf(), destIndex);
					ww.right.add(sourcePanel.getRightHalf(), destIndex);

					ww.getPanel().validate();
					dtde.dropComplete(true);
					ww.saveSignalOrder();
					return;
				} else
				{
					// moving a signal (analog only)
					String signalName = sigNames[0].substring(sigPos + 7);
					Signal sSig = null;
					Color oldColor = null;
					for(Iterator<WaveSignal> it = sourcePanel.getSignals().iterator(); it.hasNext(); )
					{
						WaveSignal ws = (WaveSignal)it.next();
						if (!ws.getSignal().getFullName().equals(signalName)) continue;
						sSig = ws.getSignal();
						if (sSig.getAnalysis().getAnalysisType() != panel.getAnalysisType())
						{
							JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
								"Cannot drop a " + sSig.getAnalysis().getAnalysisType() + " signal onto a " + panel.getAnalysisType() + " panel.  " +
								"First convert the panel with the popup in the upper-left.",
								"Error Displaying Signals", JOptionPane.ERROR_MESSAGE);
							dtde.dropComplete(true);
							return;					
						}
						oldColor = ws.getColor();
						sourcePanel.removeHighlightedSignal(ws);
						sourcePanel.removeSignal(ws.getButton());
						break;
					}
					if (sSig != null)
					{
						sourcePanel.getSignalButtons().validate();
						sourcePanel.getSignalButtons().repaint();
						sourcePanel.repaintContents();
						WaveSignal newSig = WaveSignal.addSignalToPanel(sSig, panel);
						if (newSig != null)
						{
							newSig.setColor(oldColor);
							newSig.getButton().setForeground(oldColor);
						}
					}
					ww.saveSignalOrder();
					dtde.dropComplete(true);
					return;
				}
			}

			// not rearranging: dropped a signal onto a panel
			Analysis an = ww.getSimData().findAnalysis(analysisType);
			for(int i=0; i<sigNames.length; i++)
			{
				Signal sSig = ww.findSignal(sigNames[i], an);
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
					AnalogSignal as = (AnalogSignal)sSig;
					if (as.getAnalysis().getAnalysisType() != panel.getAnalysisType())
					{
						JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
							"Cannot drop a " + as.getAnalysis().getAnalysisType() + " signal onto a " + panel.getAnalysisType() + " panel.  " +
							"First convert the panel with the popup in the upper-left.",
							"Error Displaying Signals", JOptionPane.ERROR_MESSAGE);
						dtde.dropComplete(true);
						return;					
					}
					WaveSignal.addSignalToPanel(sSig, panel);
					panel.getWaveWindow().saveSignalOrder();
					panel.makeSelectedPanel();
					dtde.dropComplete(true);
					return;
				}

				// add this signal in a new panel
				panel = ww.makeNewPanel();
				boolean isAnalog = false;
				if (sSig instanceof AnalogSignal) isAnalog = true;
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
			}
			ww.overall.validate();
			panel.repaintContents();
			dtde.dropComplete(true);
		}
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

	// ************************************* HELPER CLASS FOR WAVEFORM WINDOW *************************************

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
}
