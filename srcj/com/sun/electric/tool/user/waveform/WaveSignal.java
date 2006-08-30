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
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.user.User;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

// ************************************* INDIVIDUAL TRACES *************************************

/**
 * This class defines a single trace in a Panel.
 */
public class WaveSignal
{
	/** the panel that holds this signal */			private Panel wavePanel;
	/** the data for this signal */					private Signal sSig;
	/** the color of this signal */					private Color color;
	/** the x values of selected control points */	private double [] controlPointsSelected;
	/** true if this signal is highlighted */		private boolean highlighted;
	/** the button on the left with this signal */	private JButton sigButton;

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
					signal.wavePanel.addHighlightedSignal(signal, true);
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
			signal.wavePanel.repaintContents();
		}
	};

	public WaveSignal(Panel wavePanel, Signal sSig)
	{
		int sigNo = wavePanel.getNumSignals();
		this.wavePanel = wavePanel;
		this.sSig = sSig;
		controlPointsSelected = null;
		highlighted = false;
		String sigName = sSig.getFullName();
		if (wavePanel.isAnalog())
		{
			color = colorArray[sigNo % colorArray.length];
			sigButton = new DragButton(sigName, wavePanel.getPanelNumber());
			sigButton.setBorderPainted(false);
			sigButton.setDefaultCapable(false);
			sigButton.setForeground(color);
			wavePanel.getSignalButtons().add(sigButton);
			wavePanel.addSignal(this, sigButton);
			sigButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { signalNameClicked(evt); }
			});
			sigButton.addMouseListener(new SignalButton(this));
		} else
		{
			color = new Color(User.getColorWaveformStimuli());
			wavePanel.getDigitalSignalButton().setText(sigName);
			wavePanel.addSignal(this, wavePanel.getDigitalSignalButton());
			sigButton = wavePanel.getDigitalSignalButton();
			sigButton.setForeground(color);
		}
	}

	public static WaveSignal addSignalToPanel(Signal sSig, Panel panel)
	{
		// see if the signal is already there
		WaveSignal ws = panel.findWaveSignal(sSig);
		if (ws != null)
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
			JButton but = panel.findButton(ws);
			but.setForeground(colorArray[index]);
			panel.getSignalButtons().repaint();
			panel.repaintContents();
			return null;
		}

		// not found: add it
		int sigNo = panel.getNumSignals();
		WaveSignal wsig = new WaveSignal(panel, sSig);
		wsig.color = colorArray[sigNo % colorArray.length];
		panel.getSignalButtons().validate();
		panel.getSignalButtons().repaint();
		if (panel.getSignalButtonsPane() != null) panel.getSignalButtonsPane().validate();
		if (sigNo == 0)
		{
			// first signal in the panel: resize to fit the data
			panel.fitToSignal(sSig);
		}
		panel.repaintContents();
		return wsig;
	}

	/**
	 * Method to return the actual signal information associated with this line in the waveform window.
	 * @return the actual signal information associated with this line in the waveform window.
	 */
	public Signal getSignal() { return sSig; }

	public void setSignal(Signal sig) { sSig = sig; }

	public Color getColor() { return color; }

	public void setColor(Color c) { color = c; }

	/**
	 * Method to return the X values of selected control points in this WaveSignal.
	 * @return an array of X values of selected control points in this WaveSignal
	 * (returns null if no control points are selected).
	 */
	public double [] getSelectedControlPoints() { return controlPointsSelected; }

	public void clearSelectedControlPoints() { controlPointsSelected = null; }

	public void addSelectedControlPoint(double controlXValue)
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

	public void removeSelectedControlPoint(double controlXValue)
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
		WaveSignal ws = wavePanel.findWaveSignal(signal);
		if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) == 0)
		{
			// standard click: add this as the only trace
			ws.wavePanel.clearHighlightedSignals();
			ws.wavePanel.addHighlightedSignal(ws, true);
			ws.wavePanel.makeSelectedPanel();
		} else
		{
			// shift click: add or remove to list of highlighted traces
			if (ws.highlighted) ws.wavePanel.removeHighlightedSignal(ws, true); else
				ws.wavePanel.addHighlightedSignal(ws, true);
		}

		// show it in the schematic
		ws.wavePanel.getWaveWindow().crossProbeWaveformToEditWindow();
	}

	public Panel getPanel() { return wavePanel; }

	public JButton getButton() { return sigButton; }

	public boolean isHighlighted() { return highlighted; }

	public void setHighlighted(boolean highlighted) { this.highlighted = highlighted; }
}
