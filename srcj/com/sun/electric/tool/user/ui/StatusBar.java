/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StatusBar.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.change.Undo;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.border.BevelBorder;


/**
 * This class manages the Electric status bar at the bottom of the edit window.
 */
public class StatusBar extends JPanel implements HighlightListener, DatabaseChangeListener
{
	//private int fillPosition;
	private WindowFrame frame;
	private String coords = null;
	private JLabel fieldSelected, fieldSize, fieldTech, fieldCoords;
    private int fieldSizeNumChars, fieldTechNumChars, fieldCoordsNumChars;

	private static String selectionOverride = null;

	public StatusBar(WindowFrame frame)
	{
		//super(new SpringLayout());
		super(new GridBagLayout());

		setBorder(new BevelBorder(BevelBorder.LOWERED));
        //SpringLayout layout = (SpringLayout)this.getLayout();

		//fillPosition = 0;
		this.frame = frame;
		addField(fieldSelected = new JLabel(), 0);
        Dimension d = new Dimension(140, 16);
		addField(fieldSize = new JLabel(), 1);
        fieldSize.setMinimumSize(d);
        fieldSize.setMaximumSize(d);
        fieldSize.setPreferredSize(d);
		addField(fieldTech = new JLabel(), 2);
        d = new Dimension (200, 16);
        fieldTech.setMinimumSize(d);
        fieldTech.setMaximumSize(d);
        fieldTech.setPreferredSize(d);
		fieldCoords = new JLabel();
        d = new Dimension(100, 16);
        fieldCoords.setMinimumSize(d);
        fieldCoords.setMaximumSize(d);
        fieldCoords.setPreferredSize(d);
		if (User.isShowCursorCoordinates()) addField(fieldCoords, 3);

		/*
		//layout.putConstraint(SpringLayout.NORTH, fieldCoords, 10, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.NORTH, fieldSelected, 10, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.NORTH, fieldSize, 10, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.NORTH, fieldTech, 10, SpringLayout.NORTH, this);
		//layout.putConstraint(SpringLayout.SOUTH, fieldCoords, 5, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.SOUTH, fieldSelected, 5, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.SOUTH, fieldSize, 5, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.SOUTH, fieldTech, 5, SpringLayout.SOUTH, this);

		//layout.putConstraint(SpringLayout.EAST, this, 5, SpringLayout.EAST, fieldCoords);
        layout.putConstraint(SpringLayout.WEST, fieldSelected, 5, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.WEST, fieldSize, 5, SpringLayout.EAST, fieldSelected);
		layout.putConstraint(SpringLayout.WEST, fieldTech, 5, SpringLayout.EAST, fieldSize);
		layout.putConstraint(SpringLayout.WEST, fieldCoords, 5, SpringLayout.EAST, fieldTech);
		*/

		//SpringUtilities.makeCompactGrid(this, 1, getComponentCount(), 5,5,5,5);


        // add myself as listener for highlight changes in SDI mode
        if (TopLevel.isMDIMode()) {
            // do nothing
        } else if (frame.getContent().getHighlighter() != null) {
            frame.getContent().getHighlighter().addHighlightListener(this);
        }
        Undo.addDatabaseChangeListener(this);
	}

    /**
     * Highlighter depends on MDI or SDI mode.
     * @return the highlighter to use. May be null if none.
     */
    private Highlighter getHighlighter() {
        if (TopLevel.isMDIMode()) {
            // get current internal frame highlighter
            EditWindow wnd = EditWindow.getCurrent();
            if (wnd == null) return null;
            return wnd.getHighlighter();
        }
        return frame.getContent().getHighlighter();
    }

	private void addField(JLabel field, int index)
	{
//		JPanel frame = new JPanel();
//		frame.setBorder(new LineBorder(Color.BLACK));
//		frame.add(field);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = index;   gbc.gridy = 0;
		//gbc.weightx = 0.5;
        if (index == 0) { gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; }
        gbc.ipadx = 5;
        gbc.anchor = GridBagConstraints.WEST;
		//gbc.fill = GridBagConstraints.HORIZONTAL;
		add(field, gbc);
	}

	public static void setShowCoordinates(boolean show)
	{
		User.setShowCursorCoordinates(show);
		if (TopLevel.isMDIMode())
		{
			StatusBar sb = TopLevel.getCurrentJFrame().getStatusBar();
			if (show) sb.addField(sb.fieldCoords, 3); else
			{
				sb.remove(sb.fieldCoords);
			}
		} else
		{
			for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)it.next();
				StatusBar sb = wf.getFrame().getStatusBar();
				if (show) sb.addField(sb.fieldCoords, 3); else
				{
					sb.remove(sb.fieldCoords);
				}
			}
		}
		updateStatusBar();
	}

	public static void setCoordinates(String coords, WindowFrame wf)
	{
		StatusBar sb = null;
		if (TopLevel.isMDIMode())
		{
			sb = TopLevel.getCurrentJFrame().getStatusBar();
		} else
		{
			sb = wf.getFrame().getStatusBar();
		}
		sb.coords = coords;
		sb.redoStatusBar();
	}

	public static void setSelectionOverride(String ov)
	{
		selectionOverride = ov;
		updateStatusBar();
	}

    public void highlightChanged(Highlighter which)
    {
        updateSelectedText();
    }

    /**
     * Called when by a Highlighter when it loses focus. The argument
     * is the Highlighter that has gained focus (may be null).
     * @param highlighterGainedFocus the highlighter for the current window (may be null).
     */
    public void highlighterLostFocus(Highlighter highlighterGainedFocus) {}

	/**
	 * Method to update the status bar from current values.
	 * Call this when any of those values change.
	 */
	public static void updateStatusBar()
	{
		if (TopLevel.isMDIMode())
		{
			StatusBar sb = TopLevel.getCurrentJFrame().getStatusBar();
			sb.redoStatusBar();
		} else
		{
			for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)it.next();
				StatusBar sb = wf.getFrame().getStatusBar();
                if (sb != null)
				    sb.redoStatusBar();
			}
		}
	}
	
	private void redoStatusBar()
	{
        updateSelectedText();
        
		Cell cell = null;
		if (frame == null)
		{
			WindowFrame wf = WindowFrame.getCurrentWindowFrame(false);
			if (wf != null) cell = wf.getContent().getCell();
		} else
		{
			Cell cellInPanel = frame.getContent().getCell();
			if (cellInPanel != null) cell = cellInPanel;
		}
		String sizeMsg = "";
		if (cell != null)
		{
			if (cell.getView().isTextView())
			{
				int len = 0;
				String [] textLines = cell.getTextViewContents();
				if (textLines != null) len = textLines.length;
				sizeMsg = "LINES: " + len;
			} else
			{
//				String width = Double.toString(cell.getBounds().getWidth());
				Rectangle2D bounds = cell.getBounds();
				sizeMsg = "SIZE: " + TextUtils.formatDouble(bounds.getWidth(),1) + "x" +
                        TextUtils.formatDouble(bounds.getHeight(), 1);
			}
		}
		fieldSize.setText(sizeMsg);

		Technology tech = Technology.getCurrent();
		if (tech != null)
		{
			String message = "TECH: " + tech.getTechName();
			if (tech.isScaleRelevant()) message += " (scale=" + tech.getScale() + "nm)";
			fieldTech.setText(message);
		}

		if (coords == null) fieldCoords.setText(""); else
			fieldCoords.setText(coords);
	}

    private void updateSelectedText() {

        String selectedMsg = "NOTHING SELECTED";
        if (selectionOverride != null)
        {
            selectedMsg = selectionOverride;
        } else
        {
            // count the number of nodes and arcs selected
            int nodeCount = 0, arcCount = 0, textCount = 0;
            Highlight lastHighlight = null;
            Highlighter highlighter = getHighlighter();
            if (highlighter == null) {
                fieldSelected.setText(selectedMsg);
                return;
            }
            for(Iterator hIt = highlighter.getHighlights().iterator(); hIt.hasNext(); )
            {
                Highlight h = (Highlight)hIt.next();
                if (h.getType() == Highlight.Type.EOBJ)
                {
                    ElectricObject eObj = (ElectricObject)h.getElectricObject();
                    if (eObj instanceof PortInst)
                    {
                        lastHighlight = h;
                        nodeCount++;
                    } else if (eObj instanceof NodeInst)
                    {
                        lastHighlight = h;
                        nodeCount++;
                    } else if (eObj instanceof ArcInst)
                    {
                        lastHighlight = h;
                        arcCount++;
                    }
                } else if (h.getType() == Highlight.Type.TEXT)
                {
                    lastHighlight = h;
                    textCount++;
                }
            }
            if (nodeCount + arcCount + textCount == 1)
            {
                selectedMsg = "SELECTED "+getSelectedText(lastHighlight);
            } else
            {
                if (nodeCount + arcCount + textCount > 0)
                {
                    StringBuffer buf = new StringBuffer();
                    buf.append("SELECTED:");
                    if (nodeCount > 0) buf.append(" " + nodeCount + " NODES");
                    if (arcCount > 0)
                    {
                        if (nodeCount > 0) buf.append(",");
                        buf.append(" " + arcCount + " ARCS");
                    }
                    if (textCount > 0)
                    {
                        if (nodeCount + arcCount > 0) buf.append(",");
                        buf.append(" " + textCount + " TEXT");
                    }
                    // add on info for last highlight
                    buf.append(". LAST: "+getSelectedText(lastHighlight));
                    selectedMsg = buf.toString();
                }
            }
        }
        fieldSelected.setText(selectedMsg);
    }

    /**
     * Get a String describing the Highlight, to display in the
     * "Selected" part of the status bar.
     */
    private String getSelectedText(Highlight h) {
        PortInst thePort;
        NodeInst theNode;
        ArcInst theArc;
        if (h.getType() == Highlight.Type.EOBJ)
        {
            ElectricObject eObj = (ElectricObject)h.getElectricObject();
            if (eObj instanceof PortInst)
            {
                thePort = (PortInst)eObj;
                theNode = thePort.getNodeInst();
                return("NODE: " + theNode.describe() +
                        " PORT: \"" + thePort.getPortProto().getName() + "\"");
            } else if (eObj instanceof NodeInst)
            {
                theNode = (NodeInst)eObj;
                return("NODE: " + theNode.describe());
            } else if (eObj instanceof ArcInst)
            {
                theArc = (ArcInst)eObj;
                // TODO: remove try/catch when Netlist is thread safe
                try {
                    Netlist netlist = theArc.getParent().getUserNetlist();
                    JNetwork net = netlist.getNetwork(theArc, 0);
                    String netMsg = (net != null) ? "NETWORK: "+net.describe()+ ", " : "";
                    return(netMsg + "ARC: " + theArc.describe());
                } catch (Exception e) {
                    return("netlist exception! try again");
                }
            }
        } else if (h.getType() == Highlight.Type.TEXT)
        {
            if (h.getVar() != null)
            {
                return "TEXT: " + h.getVar().getFullDescription(h.getElectricObject());
            } else
            {
                if (h.getName() != null)
                {
                    if (h.getElectricObject() instanceof NodeInst)
                        return "TEXT: Node name "+h.getName().toString(); else
                            return "TEXT: Arc name "+h.getName().toString();
                } else
                {
                    if (h.getElectricObject() instanceof Export)
                        return "TEXT: Export name "+((Export)h.getElectricObject()).getName(); else
                            return "TEXT: Cell instance name ";
                }
            }
        }
        return null;
    }

    /**
     * Call when done with this Object. Cleans up references to this object.
     */
    public void finished() {
        if (!TopLevel.isMDIMode())
            frame.getContent().getHighlighter().removeHighlightListener(this);
        Undo.removeDatabaseChangeListener(this);
    }

    public void databaseEndChangeBatch(Undo.ChangeBatch batch) {
        redoStatusBar();
    }

    public void databaseChanged(Undo.Change evt) {}
    public boolean isGUIListener() { return true; }
}
