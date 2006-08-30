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
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.simulation.AnalogSignal;
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.simulation.DigitalSignal;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.Highlight2;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.WaveformZoom;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.TopLevel;
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
import java.awt.image.BufferedImage;
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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

/**
 * This class defines a single panel of WaveSignals with an associated list of signal names.
 */
public class Panel extends JPanel
	implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
{
	/** the main waveform window this is part of */			private WaveformWindow waveWindow;
	/** the size of the window (in pixels) */				private Dimension sz;
	/** true if the size field is valid */					private boolean szValid;
	/** the signal on the X axis (null for time) */			private Signal xAxisSignal;
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
	/** for selecting the type of data in this panel */		private JComboBox analysisCombo;
	/** displayed range along horozintal axis */			private double minXPosition, maxXPosition;
	/** low value displayed in this panel (analog) */		private double analogLowValue;
	/** high value displayed in this panel (analog) */		private double analogHighValue;
	/** vertical range displayed in this panel (analog) */	private double analogRange;
	/** true if an X axis cursor is being dragged */		private boolean draggingMain, draggingExt, draggingVertAxis;
	/** true if an area is being dragged */					private boolean draggingArea;
	/** true if this waveform panel is selected */			private boolean selected;
	/** true if this waveform panel is hidden */			private boolean hidden;
	/** the type of analysis shown in this panel */			private Analysis.AnalysisType analysisType;
	/** true for analog panel; false for digital */			private boolean analog;
	/** the horizontal ruler at the top of this panel. */	private HorizRuler horizRulerPanel;
	/** true if the horizontal ruler is logarithmic */		private boolean horizRulerPanelLogarithmic;
	/** true if this panel is logarithmic in Y */			private boolean vertPanelLogarithmic;
	/** the number of this panel. */						private int panelNumber;
	/** all panels that the "measure" tool crosses into */	private HashSet<Panel> measureWindows;
	/** extent of area dragged-out by cursor */				private int dragStartX, dragStartY;
	/** extent of area dragged-out by cursor */				private int dragEndX, dragEndY;
	/** the location of the Y axis vertical line */			private int vertAxisPos;
	/** the smallest nonzero X value (for log drawing) */	private double smallestXValue;
	/** the smallest nonzero Y value (for log drawing) */	private double smallestYValue;

	/** the background color of a button */					private static Color background = null;
	/** The color of the grid (a gray) */					private static Color gridColor = new Color(0x808080);
	/** the panel numbering index */						private static int nextPanelNumber = 1;
	/** for determining double-clicks */					private static long lastClick = 0;

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
	 * @param analysisType the type of data shown in this Panel.
	 */
	public Panel(WaveformWindow waveWindow, boolean analog, Analysis.AnalysisType analysisType)
	{
		// remember state
		this.waveWindow = waveWindow;
		setAnalysisType(analysisType);
		this.analog = analog;
		selected = false;
		panelNumber = nextPanelNumber++;
		vertAxisPos = VERTLABELWIDTH;
		horizRulerPanelLogarithmic = false;
		vertPanelLogarithmic = false;
		xAxisSignal = null;
		waveSignals = new HashMap<JButton,WaveSignal>();

		// setup this panel window
		int height = waveWindow.getPanelSizeDigital();
		if (analog) height = waveWindow.getPanelSizeAnalog();
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
		if (analog)
		{
			// analog panel
			JLabel label = new DragLabel(Integer.toString(panelNumber));
			label.setToolTipText("Identification number of this waveform panel");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;       gbc.gridy = 1;
			gbc.weightx = 0.2;  gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.NORTHWEST;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = new Insets(4, 4, 4, 4);
			leftHalf.add(label, gbc);
		} else
		{
			// digital panel
			digitalSignalButton = new DragButton(Integer.toString(panelNumber), panelNumber);
			digitalSignalButton.setBorderPainted(false);
			digitalSignalButton.setForeground(Color.BLACK);
			digitalSignalButton.setToolTipText("Name of this waveform panel");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;       gbc.gridy = 1;
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
		gbc = new GridBagConstraints();
		gbc.gridx = 1;       gbc.gridy = 1;
		gbc.weightx = 0.2;  gbc.weighty = 0;
		if (analog) gbc.anchor = GridBagConstraints.NORTH; else
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
		gbc = new GridBagConstraints();
		gbc.gridx = 2;       gbc.gridy = 1;
		gbc.weightx = 0.2;  gbc.weighty = 0;
		if (analog) gbc.anchor = GridBagConstraints.NORTH; else
			gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		leftHalf.add(hide, gbc);
		hide.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { hidePanel(); }
		});

		if (analog)
		{
			// the "delete signal" button for this panel (analog only)
			deleteSignal = new JButton(iconDeleteSignal);
			deleteSignal.setBorderPainted(false);
			deleteSignal.setDefaultCapable(false);
			deleteSignal.setToolTipText("Remove selected signals from this panel");
			minWid = new Dimension(iconDeleteSignal.getIconWidth()+4, iconDeleteSignal.getIconHeight()+4);
			deleteSignal.setMinimumSize(minWid);
			deleteSignal.setPreferredSize(minWid);
			gbc = new GridBagConstraints();
			gbc.gridx = 3;      gbc.gridy = 1;
			gbc.weightx = 0.2;  gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.NONE;
			leftHalf.add(deleteSignal, gbc);
			deleteSignal.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { deleteSignalFromPanel(); }
			});

			// the "delete all signal" button for this panel (analog only)
			deleteAllSignals = new JButton(iconDeleteAllSignals);
			deleteAllSignals.setBorderPainted(false);
			deleteAllSignals.setDefaultCapable(false);
			deleteAllSignals.setToolTipText("Remove all signals from this panel");
			minWid = new Dimension(iconDeleteAllSignals.getIconWidth()+4, iconDeleteAllSignals.getIconHeight()+4);
			deleteAllSignals.setMinimumSize(minWid);
			deleteAllSignals.setPreferredSize(minWid);
			gbc = new GridBagConstraints();
			gbc.gridx = 4;       gbc.gridy = 1;
			gbc.weightx = 0.2;  gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.NONE;
			leftHalf.add(deleteAllSignals, gbc);
			deleteAllSignals.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { deleteAllSignalsFromPanel(); }
			});

			// the "signal type" selector for this panel (analog only)
			boolean hasACData = waveWindow.getSimData().findAnalysis(Analysis.ANALYSIS_AC) != null;
			boolean hasDCData = waveWindow.getSimData().findAnalysis(Analysis.ANALYSIS_DC) != null;
			boolean hasMeasData = waveWindow.getSimData().findAnalysis(Analysis.ANALYSIS_MEAS) != null;
			if (hasACData || hasDCData || hasMeasData)
			{
				analysisCombo = new JComboBox();
				analysisCombo.addItem(Analysis.ANALYSIS_TRANS.toString());
				if (hasACData) analysisCombo.addItem(Analysis.ANALYSIS_AC.toString());
				if (hasDCData) analysisCombo.addItem(Analysis.ANALYSIS_DC.toString());
				if (hasMeasData) analysisCombo.addItem(Analysis.ANALYSIS_MEAS.toString());
				analysisCombo.setToolTipText("Sets the type of data seen in this panel");
				analysisCombo.setSelectedItem(analysisType.toString());
				gbc = new GridBagConstraints();
				gbc.gridx = 0;       gbc.gridy = 2;
				gbc.gridwidth = 5;   gbc.gridheight = 1;
				gbc.anchor = GridBagConstraints.CENTER;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				leftHalf.add(analysisCombo, gbc);
				analysisCombo.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent evt) { setPanelSignalType(); }
				});
			}

			// the list of signals in this panel (analog only)
			signalButtons = new JPanel();
			signalButtons.setLayout(new BoxLayout(signalButtons, BoxLayout.Y_AXIS));
			signalButtonsPane = new JScrollPane(signalButtons);
			gbc = new GridBagConstraints();
			gbc.gridx = 0;       gbc.gridy = 3;
			gbc.gridwidth = 5;   gbc.gridheight = 1;
			gbc.weightx = 1;     gbc.weighty = 1;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.BOTH;
			leftHalf.add(signalButtonsPane, gbc);
		}

		// the right side with signal traces
		rightHalf = new JPanel();
		rightHalf.setLayout(new GridBagLayout());
		rightHalf.setPreferredSize(new Dimension(100, height));

		// a drop target for the signal panel
		DropTarget dropTargetRight = new DropTarget(this, DnDConstants.ACTION_LINK, WaveformWindow.waveformDropTarget, true);

		// a separator at the top
		sep = new JSeparator(SwingConstants.HORIZONTAL);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;       gbc.gridy = 0;
		gbc.weightx = 1;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(4, 0, 4, 0);
		rightHalf.add(sep, gbc);

		// the horizontal ruler (if separate rulers in each panel)
		if (!waveWindow.isXAxisLocked())
			addHorizRulerPanel();

		// the waveform display for this panel
		gbc = new GridBagConstraints();
		gbc.gridx = 0;       gbc.gridy = 2;
		gbc.weightx = 1;     gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 0, 0, 0);
		rightHalf.add(this, gbc);

		// put the left and right sides into the window
		if (!WaveformWindow.USETABLES)
		{
			waveWindow.getSignalNamesPanel().add(leftHalf);
			waveWindow.getSignalTracesPanel().add(rightHalf);
		}

		// add to list of wave panels
		waveWindow.addPanel(this);

		// rebuild list of panels
		waveWindow.rebuildPanelList();
		waveWindow.redrawAllPanels();
	}

	// ************************************* MISCELLANEOUS *************************************

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

	public boolean isAnalog() { return analog; }

	public Analysis.AnalysisType getAnalysisType() { return analysisType; };

	public void setAnalysisType(Analysis.AnalysisType a)
	{
		analysisType = a;
		computeSmallestValues();
	}

	public JPanel getSignalButtons() { return signalButtons; };

	public JScrollPane getSignalButtonsPane() { return signalButtonsPane; };

	public JButton getDigitalSignalButton() { return digitalSignalButton; }

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


	// ************************************* SIGNALS IN THE PANEL *************************************

	public void addSignal(WaveSignal sig, JButton but)
	{
		waveSignals.put(but, sig);
	}

	public void removeSignal(JButton but)
	{
		signalButtons.remove(but);
		waveSignals.remove(but);
	}

	public void removeAllSignals()
	{
		waveSignals.clear();
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

	public WaveSignal findWaveSignal(Signal sig)
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

	private void setPanelSignalType()
	{		
		String typeName = (String)analysisCombo.getSelectedItem();
		Analysis.AnalysisType analysisType = Analysis.AnalysisType.findAnalysisType(typeName);
		if (getAnalysisType() != analysisType && getNumSignals() > 0)
		{
			String warning = "The signals in this panel are not " + analysisType +
				" data.  Remove them from the panel?";
			int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(), warning);
			if (response != JOptionPane.YES_OPTION)
			{
				// aborted: reset panel type
				analysisCombo.setSelectedItem(getAnalysisType().toString());
				return;
			}
			xAxisSignal = null;
			if (waveWindow.isXAxisLocked()) waveWindow.setXAxisSignalAll(null);
			waveWindow.deleteAllSignalsFromPanel(this);
		}
		setAnalysisType(analysisType);

		// redo X scale if time unlocked or this is the only panel
		Analysis an = waveWindow.getSimData().findAnalysis(analysisType);
		if (an != null)
		{
			Rectangle2D bounds = an.getBounds(); 
			double lowValue = bounds.getMinY();
			double highValue = bounds.getMaxY();
			setYAxisRange(lowValue, highValue);
			if (!waveWindow.isXAxisLocked() || waveWindow.getNumPanels() == 1)
			{
				// set the X range
				double lowXValue = bounds.getMinX();
				double highXValue = bounds.getMaxX();
				setXAxisRange(lowXValue, highXValue);
			}
		}
		
		repaintWithRulers();
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

	public Signal getXAxisSignal() { return xAxisSignal; }

	public void setXAxisSignal(Signal sig) { xAxisSignal = sig; }

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

	private void toggleBusContents()
	{
		// this panel must have one signal
		Collection<WaveSignal> theSignals = waveSignals.values();
		if (theSignals.size() != 1) return;

		// the only signal must be digital
		WaveSignal ws = theSignals.iterator().next();
		if (!(ws.getSignal() instanceof DigitalSignal)) return;

		// the digital signal must be a bus
		DigitalSignal sDSig = (DigitalSignal)ws.getSignal();
		List<Signal> bussedSignals = sDSig.getBussedSignals();
		if (bussedSignals == null) return;

		// see if any of the bussed signals are displayed
		boolean opened = false;
		for(Signal subSig : bussedSignals)
		{
			DigitalSignal subDS = (DigitalSignal)subSig;
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

			for(Signal subSig : bussedSignals)
			{
				DigitalSignal subDS = (DigitalSignal)subSig;
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
			for(Signal subSig : bussedSignals)
			{
				DigitalSignal subDS = (DigitalSignal)subSig;
				Panel wp = waveWindow.makeNewPanel();
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

	// ************************************* X AND Y AXIS CONTROL *************************************

	/**
	 * Method to compute the smallest X and Y values (for log display).
	 */
	private void computeSmallestValues()
	{
		if (analysisType == null) return;
		Stimuli sd = waveWindow.getSimData();
		Analysis an = sd.findAnalysis(analysisType);
		if (an == null) return;
		Rectangle2D anBounds = an.getBounds();
        if (anBounds == null) return; // no signals
		smallestXValue = anBounds.getWidth() / 1000;
		smallestYValue = anBounds.getHeight() / 1000;
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
	 * Method to make this Panel show a signal fully.
	 * @param sSig the signal to show (must be analog)
	 */
	public void fitToSignal(Signal sSig)
	{
		if (sSig instanceof AnalogSignal)
		{
			AnalogSignal as = (AnalogSignal)sSig;
			Rectangle2D rangeBounds = as.getBounds();
			double lowValue = rangeBounds.getMinY();
			double highValue = rangeBounds.getMaxY();
			double range = highValue - lowValue;
			if (range == 0) range = 2;
			double rangeExtra = range / 10;
			setYAxisRange(lowValue - rangeExtra, highValue + rangeExtra);
		}
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
			if (winMinX <= 0) winMinX = smallestXValue;
			double logWinMinX = Math.log10(winMinX);
			double winMaxX = maxXPosition;
			if (winMaxX <= 0) winMaxX = smallestXValue;
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
		} else
		{
			// linear axes
			double y = sz.height - 1 - (value - analogLowValue) / analogRange * (sz.height-1);
			return (int)y;
		}
	}

	/**
	 * Method to scale a Y coordinate from screen space to data space.
	 * @param y the Y coordinate on the screen.
	 * @return the Y value in the simulation corresponding to that screen coordinate.
	 */
	private double convertYScreenToData(int y)
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
			double yValue = Math.pow(10, logWinMinY - ((double)(y - sz.height + 1)) * (logWinMaxY - logWinMinY) / (sz.height-1));
			return yValue;
		} else
		{
			// linear axes
			double value = analogLowValue - ((double)(y - sz.height + 1)) * analogRange / (sz.height-1);
			return value;
		}
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
		repaintOffscreenImage();
		repaint();
	}

	private BufferedImage offscreen;
	private Graphics offscreenGraphics;
	private int offscreenWid, offscreenHei;

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
		if (offscreen == null || offscreenWid != wid | offscreenHei != hei)
		{
			repaintOffscreenImage();
		}
		Point screenLoc = getLocationOnScreen();
		waveWindow.setScreenXSize(screenLoc.x, screenLoc.x + wid);

		g.drawImage(offscreen, 0, 0, null);
	}

	private void repaintOffscreenImage()
	{
		// rebuild the offscreen image if necessary
		int wid = sz.width;
		int hei = sz.height;
		if (offscreen == null || offscreenWid != wid | offscreenHei != hei)
		{
			offscreen = new BufferedImage(wid, hei, BufferedImage.TYPE_INT_RGB);
			offscreenGraphics = offscreen.getGraphics();
			offscreenWid = wid;
			offscreenHei = hei;
		}

		// clear the buffer
		offscreenGraphics.setColor(new Color(User.getColorWaveformBackground()));
		offscreenGraphics.fillRect(0, 0, wid, hei);

		drawPanelContents(wid, hei, null, null);

		// draw the X position cursors
		Graphics2D g2 = (Graphics2D)offscreenGraphics;
		g2.setStroke(Highlight2.dashedLine);
		int x = convertXDataToScreen(waveWindow.getMainXPositionCursor());
		if (x >= vertAxisPos)
			offscreenGraphics.drawLine(x, 0, x, hei);
		g2.setStroke(farDottedLine);
		x = convertXDataToScreen(waveWindow.getExtensionXPositionCursor());
		if (x >= vertAxisPos)
			offscreenGraphics.drawLine(x, 0, x, hei);
		g2.setStroke(Highlight2.solidLine);

		// show dragged area if there
		if (draggingArea)
		{
			offscreenGraphics.setColor(new Color(User.getColorWaveformForeground()));
			int lowX = Math.min(dragStartX, dragEndX);
			int highX = Math.max(dragStartX, dragEndX);
			int lowY = Math.min(dragStartY, dragEndY);
			int highY = Math.max(dragStartY, dragEndY);
			offscreenGraphics.drawLine(lowX, lowY, lowX, highY);
			offscreenGraphics.drawLine(lowX, highY, highX, highY);
			offscreenGraphics.drawLine(highX, highY, highX, lowY);
			offscreenGraphics.drawLine(highX, lowY, lowX, lowY);
			if (ToolBar.getCursorMode() == ToolBar.CursorMode.MEASURE)
			{
				// show dimensions while dragging
				double lowXValue = convertXScreenToData(lowX);
				double highXValue = convertXScreenToData(highX);
				double lowValue = convertYScreenToData(highY);
				double highValue = convertYScreenToData(lowY);
				offscreenGraphics.setFont(waveWindow.getFont());

				// show the low X value and arrow
				String lowXValueString = TextUtils.convertToEngineeringNotation(lowXValue, "s");
				GlyphVector gv = waveWindow.getFont().createGlyphVector(waveWindow.getFontRenderContext(), lowXValueString);
				Rectangle2D glyphBounds = gv.getLogicalBounds();
				int textWid = (int)glyphBounds.getWidth();
				int textHei = (int)glyphBounds.getHeight();
				int textY = (lowY+highY)/2;
				offscreenGraphics.drawString(lowXValueString, lowX-textWid-6, textY+textHei/2-10);
				offscreenGraphics.drawLine(lowX-1, textY, lowX-textWid, textY);
				offscreenGraphics.drawLine(lowX-1, textY, lowX-6, textY+4);
				offscreenGraphics.drawLine(lowX-1, textY, lowX-6, textY-4);

				// show the high X value and arrow
				String highXValueString = TextUtils.convertToEngineeringNotation(highXValue, "s");
				gv = waveWindow.getFont().createGlyphVector(waveWindow.getFontRenderContext(), highXValueString);
				glyphBounds = gv.getLogicalBounds();
				textWid = (int)glyphBounds.getWidth();
				textHei = (int)glyphBounds.getHeight();
				int highXValueTextWid = textWid;
				offscreenGraphics.drawString(highXValueString, highX+6, textY+textHei/2-10);
				offscreenGraphics.drawLine(highX+1, textY, highX+textWid, textY);
				offscreenGraphics.drawLine(highX+1, textY, highX+6, textY+4);
				offscreenGraphics.drawLine(highX+1, textY, highX+6, textY-4);

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
					offscreenGraphics.drawString(xDiffString, xCtr - textWid/2, yPosText);
					offscreenGraphics.drawLine(lowX, yPos, xCtr - textWid/2 - 2, yPos);
					offscreenGraphics.drawLine(highX, yPos, xCtr + textWid/2 + 2, yPos);
					offscreenGraphics.drawLine(lowX, yPos, lowX+5, yPos+4);
					offscreenGraphics.drawLine(lowX, yPos, lowX+5, yPos-4);
					offscreenGraphics.drawLine(highX, yPos, highX-5, yPos+4);
					offscreenGraphics.drawLine(highX, yPos, highX-5, yPos-4);
				} else
				{
					// does not fit inside: draw outside of arrows
					int yPosText = highY + textHei*5;
					int yPos = yPosText - textHei/2;
					int xCtr = (highX+lowX)/2;
					offscreenGraphics.drawString(xDiffString, highX + 12, yPosText);
					offscreenGraphics.drawLine(lowX, yPos, lowX-10, yPos);
					offscreenGraphics.drawLine(highX, yPos, highX+10, yPos);
					offscreenGraphics.drawLine(lowX, yPos, lowX-5, yPos+4);
					offscreenGraphics.drawLine(lowX, yPos, lowX-5, yPos-4);
					offscreenGraphics.drawLine(highX, yPos, highX+5, yPos+4);
					offscreenGraphics.drawLine(highX, yPos, highX+5, yPos-4);
				}

				if (analog)
				{
					// show the low value
					String lowValueString = TextUtils.convertToEngineeringNotation(highValue, null);
					gv = waveWindow.getFont().createGlyphVector(waveWindow.getFontRenderContext(), lowValueString);
					glyphBounds = gv.getLogicalBounds();
					textWid = (int)glyphBounds.getWidth();
					textHei = (int)glyphBounds.getHeight();
					int xP = (lowX+highX)/2;
					int yText = lowY - 10 - textHei;
					offscreenGraphics.drawString(lowValueString, xP, yText - 2);
					offscreenGraphics.drawLine(xP, lowY-1, xP, yText);
					offscreenGraphics.drawLine(xP, lowY-1, xP+4, lowY-5);
					offscreenGraphics.drawLine(xP, lowY-1, xP-4, lowY-5);

					// show the high value
					String highValueString = TextUtils.convertToEngineeringNotation(lowValue, null);
					gv = waveWindow.getFont().createGlyphVector(waveWindow.getFontRenderContext(), highValueString);
					glyphBounds = gv.getLogicalBounds();
					textWid = (int)glyphBounds.getWidth();
					textHei = (int)glyphBounds.getHeight();
					yText = highY + 10 + textHei;
					offscreenGraphics.drawString(highValueString, xP, yText + textHei + 2);
					offscreenGraphics.drawLine(xP, highY+1, xP, yText);
					offscreenGraphics.drawLine(xP, highY+1, xP+4, highY+5);
					offscreenGraphics.drawLine(xP, highY+1, xP-4, highY+5);

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
						offscreenGraphics.drawString(valueDiffString, xPos+2, yCtr + textHei/2);
						offscreenGraphics.drawLine(xPos, lowY, xPos, highY);
						offscreenGraphics.drawLine(xPos, lowY, xPos+4, lowY+5);
						offscreenGraphics.drawLine(xPos, lowY, xPos-4, lowY+5);
						offscreenGraphics.drawLine(xPos, highY, xPos+4, highY-5);
						offscreenGraphics.drawLine(xPos, highY, xPos-4, highY-5);
					} else
					{
						// does not fit inside: draw outside of arrows
						int xPos = highX + highXValueTextWid + 30;
						int yCtr = (highY+lowY)/2;
						offscreenGraphics.drawString(valueDiffString, xPos+4, lowY - textHei/2 - 4);
						offscreenGraphics.drawLine(xPos, lowY, xPos, lowY-10);
						offscreenGraphics.drawLine(xPos, highY, xPos, highY+10);
						offscreenGraphics.drawLine(xPos, lowY, xPos+4, lowY-5);
						offscreenGraphics.drawLine(xPos, lowY, xPos-4, lowY-5);
						offscreenGraphics.drawLine(xPos, highY, xPos+4, highY+5);
						offscreenGraphics.drawLine(xPos, highY, xPos-4, highY+5);
					}
				}
			}
		}
	}
	
	private void drawPanelContents(int wid, int hei, Rectangle2D bounds, List<PolyBase> polys)
	{
		// draw the grid first (behind the signals)
		Graphics localGraphics = null;
		if (polys == null)
		{
			localGraphics = offscreenGraphics;
		}
		if (analog && waveWindow.isShowGrid())
		{
			if (localGraphics != null)
			{
				Graphics2D g2 = (Graphics2D)offscreenGraphics;
				g2.setStroke(Highlight2.dottedLine);
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
				Graphics2D g2 = (Graphics2D)localGraphics;
				g2.setStroke(Highlight2.solidLine);
			}
		}

		// draw all of the signals
		processSignals(localGraphics, bounds, polys);

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
			localGraphics.setColor(new Color(User.getColorWaveformForeground()));
			localGraphics.drawLine(vertAxisPos, 0, vertAxisPos, hei);
			if (selected)
			{
				localGraphics.drawLine(vertAxisPos-1, 0, vertAxisPos-1, hei);
				localGraphics.drawLine(vertAxisPos-2, 0, vertAxisPos-2, hei-1);
				localGraphics.drawLine(vertAxisPos-3, 0, vertAxisPos-3, hei-2);
			}
		}
		if (analog)
		{
			double displayedLow = convertYScreenToData(hei);
			double displayedHigh = convertYScreenToData(0);
			StepSize ss = new StepSize(displayedHigh, displayedLow, 5);
			if (ss.getSeparation() != 0.0)
			{
				double value = ss.getLowValue();
				if (localGraphics != null)
					localGraphics.setFont(waveWindow.getFont());
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
							} else if (lastY - y > 25)
							{
								// add 1 tick mark
								int intY = (lastY - y) / 2 + y;
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
						String yValue = TextUtils.convertToEngineeringNotation(value, null);
						if (polys != null)
						{
							Poly poly = new Poly(new Point2D[] {
								new Point2D.Double(vertAxisPos-12, y)});
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
						lastY = y;
					}
					value += ss.getSeparation();
				}
			}
		}
	}

	private List<WaveSelection> processSignals(Graphics g, Rectangle2D bounds, List<PolyBase> forPs)
	{
		List<WaveSelection> selectedObjects = null;
		if (bounds != null) selectedObjects = new ArrayList<WaveSelection>();
		int wid = sz.width;
		int hei = sz.height;
		Signal xSignal = xAxisSignal;
		if (waveWindow.isXAxisLocked()) xSignal = waveWindow.getXAxisSignalAll();
        double[] result = new double[3];

        int linePointMode = waveWindow.getLinePointMode();
        Collection<WaveSignal> sigs = waveSignals.values();
        int sigIndex = 0;
		for(WaveSignal ws : sigs)
		{
			if (g != null)
			{
				if (waveWindow.getPrintingMode() == 2) g.setColor(Color.BLACK); else
					g.setColor(ws.getColor());
			}

			if (forPs != null)
			{
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
			if (ws.getSignal() instanceof AnalogSignal)
			{
				// draw analog trace
				AnalogSignal as = (AnalogSignal)ws.getSignal();
                Analysis an = as.getAnalysis();
                for (int s = 0, numSweeps = as.getNumSweeps(); s < numSweeps; s++)
				{
                    boolean included = waveWindow.isSweepSignalIncluded(an, s);
                    if (!included)
                        continue;
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
							AnalogSignal oSig = (AnalogSignal)xSignal;
							oSig.getEvent(s, i, result);
							x = convertXDataToScreen(result[1]);
						}
						if ((x >= vertAxisPos && x <= sz.width) ||
							(lastX > vertAxisPos && lastX < sz.width))
						{
	                        if (i != 0)
	                        {
	                        	if (linePointMode <= 1)
	                        	{
	                        		// drawing has lines
		                            if (processALine(g, lastX, lastLY, x, lowY, bounds, forPs, selectedObjects, ws, s)) break;
		                            if (lastLY != lastHY || lowY != highY)
		                            {
		        						if (processALine(g, lastX, lastHY, x, highY, bounds, forPs, selectedObjects, ws, s)) break;
		        						if (processALine(g, lastX, lastHY, x, lowY, bounds, forPs, selectedObjects, ws, s)) break;
		        						if (processALine(g, lastX, lastLY, x, highY, bounds, forPs, selectedObjects, ws, s)) break;
		                            }
	                        	}
							}
	                    	if (i == numEvents-1 && linePointMode <= 1)
	                    	{
	                    		// process extrapolated line from the last data point
	                            if (processALine(g, x, lowY, sz.width, lowY, bounds, forPs, selectedObjects, ws, s)) break;
	                            if (lastLY != lastHY || lowY != highY)
	                            {
	        						if (processALine(g, x, highY, sz.width, highY, bounds, forPs, selectedObjects, ws, s)) break;
	                            }
	                    	}
	                    	if (linePointMode >= 1)
							{
	                    		// drawing has points
								if (processABox(g, x-2, lowY-2, x+2, lowY+2, bounds, forPs, selectedObjects, ws, false, 0)) break;
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
						for(Signal subSig : bussedSignals)
						{
							DigitalSignal subDS = (DigitalSignal)subSig;
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
								if (processALine(g, x, hei/2, x+5, hei-5, bounds, forPs, selectedObjects, ws, -1)) return selectedObjects;
								if (processALine(g, x, hei/2, x+5, 5, bounds, forPs, selectedObjects, ws, -1)) return selectedObjects;
							} else
							{
								// bus change point: draw the "X"
								if (processALine(g, x-5, 5, x+5, hei-5, bounds, forPs, selectedObjects, ws, -1)) return selectedObjects;
								if (processALine(g, x+5, 5, x-5, hei-5, bounds, forPs, selectedObjects, ws, -1)) return selectedObjects;
							}
							if (lastX+5 < x-5)
							{
								// previous bus change point: draw horizontal bars to connect
								if (processALine(g, lastX+5, 5, x-5, 5, bounds, forPs, selectedObjects, ws, -1)) return selectedObjects;
								if (processALine(g, lastX+5, hei-5, x-5, hei-5, bounds, forPs, selectedObjects, ws, -1)) return selectedObjects;
							}
							String valString = "XX";
							if (curDefined) valString = Long.toString(curYValue);
							if (g != null)
							{
								g.setFont(waveWindow.getFont());
								GlyphVector gv = waveWindow.getFont().createGlyphVector(waveWindow.getFontRenderContext(), valString);
								Rectangle2D glyphBounds = gv.getLogicalBounds();
								int textHei = (int)glyphBounds.getHeight();
								g.drawString(valString, x+2, hei/2+textHei/2);
							}
							if (forPs != null)
							{
								Point2D [] pts = new Point2D[1];
								pts[0] = new Point2D.Double(x+2, hei/2);
								Poly poly = new Poly(pts);
								poly.setStyle(Poly.Type.TEXTLEFT);
								poly.setTextDescriptor(TextDescriptor.EMPTY.withAbsSize(8));
								poly.setString(valString);
								forPs.add(poly);
							}
						}
						curXValue = nextXValue;
						lastX = x;
						if (nextXValue == Double.MAX_VALUE) break;
					}
					if (lastX+5 < wid)
					{
						// run horizontal bars to the end
						if (processALine(g, lastX+5, 5, wid, 5, bounds, forPs, selectedObjects, ws, -1)) return selectedObjects;
						if (processALine(g, lastX+5, hei-5, wid, hei-5, bounds, forPs, selectedObjects, ws, -1)) return selectedObjects;
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
						if (waveWindow.getPrintingMode() == 2) g.setColor(Color.BLACK); else
						{
							switch (ds.getState(i) & Stimuli.STRENGTH)
							{
								case Stimuli.OFF_STRENGTH:  g.setColor(waveWindow.getOffStrengthColor());    break;
								case Stimuli.NODE_STRENGTH: g.setColor(waveWindow.getNodeStrengthColor());   break;
								case Stimuli.GATE_STRENGTH: g.setColor(waveWindow.getGateStrengthColor());   break;
								case Stimuli.VDD_STRENGTH:  g.setColor(waveWindow.getPowerStrengthColor());  break;
							}
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
							if (processALine(g, x, 5, x, hei-5, bounds, forPs, selectedObjects, ws, -1)) return selectedObjects;
						}
					}
					if (lastLowy == lastHighy)
					{
						if (processALine(g, lastx, lastLowy, x, lastLowy, bounds, forPs, selectedObjects, ws, -1)) return selectedObjects;
					} else
					{
						if (processABox(g, lastx, lastLowy, x, lastHighy, bounds, forPs, selectedObjects, ws, false, 0)) return selectedObjects;
					}
					if (i >= numEvents-1)
					{
						if (lowy == highy)
						{
							if (processALine(g, x, lowy, wid-1, lowy, bounds, forPs, selectedObjects, ws, -1)) return selectedObjects;
						} else
						{
							if (processABox(g, x, lowy, wid-1, highy, bounds, forPs, selectedObjects, ws, false, 0)) return selectedObjects;
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

	private boolean processABox(Graphics g, int lX, int lY, int hX, int hY, Rectangle2D bounds, List<PolyBase> forPs,
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
					poly.setLayer(Artwork.tech.defaultLayer);
					forPs.add(poly);
					return false;
				} else
				{
					WaveSelection wSel = new WaveSelection();
					wSel.ws = ws;
					wSel.controlPoint = controlPoint;
					wSel.controlXValue = controlXValue;
					result.add(wSel);
					return true;
				}
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

	private boolean processALine(Graphics g, int fX, int fY, int tX, int tY, Rectangle2D bounds,
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
				} else
				{
					WaveSelection wSel = new WaveSelection();
					wSel.ws = ws;
					wSel.controlPoint = false;
					result.add(wSel);
					return true;
				}
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
		makeSelectedPanel();

		// reset dragging from last time
		for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
		{
			Panel wp = it.next();
			if (wp.draggingArea) wp.repaintContents();
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
		if (mode == ToolBar.CursorMode.ZOOM) mouseMovedZoom(evt);
        else if (mode == ToolBar.CursorMode.PAN) mouseMovedPan(evt);
        else mouseMovedSelect(evt);
	}

	public void mouseDragged(MouseEvent evt)
	{
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
			makeSelectedPanel();
		} else
		{
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
		drawPanelContents(sz.width, sz.height, new Rectangle2D.Double(vertAxisPos, 0, sz.width, sz.height), polys);
		return polys;
	}

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
		for(WaveSignal ws : waveSignals.values())
		{
			if (!(ws.getSignal() instanceof AnalogSignal)) continue;

			// draw analog trace
			AnalogSignal as = (AnalogSignal)ws.getSignal();
            double[] result = new double[3];
            Analysis an = as.getAnalysis();

			for(int s=0, numSweeps = as.getNumSweeps(); s<numSweeps; s++)
			{
                if (!waveWindow.isSweepSignalIncluded(an, s)) continue;
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
		for(WaveSignal ws : waveSignals.values())
		{
			if (!(ws.getSignal() instanceof AnalogSignal)) continue;

			// draw analog trace
			AnalogSignal as = (AnalogSignal)ws.getSignal();
            double[] result = new double[3];
            double[] lastResult = new double[3];
            Analysis an = as.getAnalysis();

			for(int s=0, numSweeps = as.getNumSweeps(); s<numSweeps; s++)
			{
                if (!waveWindow.isSweepSignalIncluded(an, s)) continue;
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
					if (analog) clearHighlightedSignals(); else
					{
						for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
						{
							Panel oWp = it.next();
							oWp.clearHighlightedSignals();
						}
					}
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
						Panel wp = it.next();
						Point oPt = wp.getLocationOnScreen();
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
				for(Panel wp : measureWindows)
				{
					if (wp == this) continue;
					Point oPt = wp.getLocationOnScreen();
					wp.dragStartX = dragStartX;
					wp.dragStartY = dragStartY + scPt.y - oPt.y;
					wp.dragEndX = pt.x;
					wp.dragEndY = pt.y + scPt.y - oPt.y;
					wp.repaintContents();
				}
			}
			dragEndX = pt.x;
			dragEndY = pt.y;
			repaintContents();
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
			ws.getButton().setBackground(new Color(User.getColorWaveformBackground()));
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
	public void makeSelectedPanel()
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
	}

	public boolean isSelected() { return selected; }

	// ****************************** ZOOMING AND PANNING ******************************

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
			Panel wp = it.next();
			if (!waveWindow.isXAxisLocked() && wp != this) continue;
			if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0 || ClickZoomWireListener.isRightMouse(evt))
			{
				// standard click: zoom in
				wp.setXAxisRange(lowXValue, highXValue);
				if (wp == this)
					wp.setYAxisRange(lowValue, highValue);
			} else
			{
				// shift-click: zoom out
				double oldRange = wp.maxXPosition - wp.minXPosition;
				wp.setXAxisRange((lowXValue + highXValue) / 2 - oldRange,
					(lowXValue + highXValue) / 2 + oldRange);
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
			dragEndX = evt.getX();
			dragEndY = evt.getY();
			repaintContents();
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
		double dragEndYData = convertYScreenToData(dragEndY);
		double dragStartYData = convertYScreenToData(dragStartY);
		double dYValue = dragEndYData - dragStartYData;

		for(Iterator<Panel> it = waveWindow.getPanels(); it.hasNext(); )
		{
			Panel wp = it.next();
			if (!waveWindow.isXAxisLocked() && wp != this) continue;
			wp.setXAxisRange(wp.minXPosition - dXValue, wp.maxXPosition - dXValue);
			if (wp == this)
				setYAxisRange(analogLowValue - dYValue, analogHighValue - dYValue);
			wp.repaintWithRulers();
		}
		dragStartX = dragEndX;
		dragStartY = dragEndY;
	}

	public void mouseMovedPan(MouseEvent evt) {}

	// ************************************* DRAG AND DROP *************************************

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

}
