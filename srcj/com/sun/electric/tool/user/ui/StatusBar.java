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
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.User;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;


/**
 * This class manages the Electric status bar at the bottom of the edit window.
 */
public class StatusBar extends JPanel
{
	//private int fillPosition;
	private WindowFrame frame;
	private String coords = null;
	public JLabel fieldSelected, fieldSize, fieldTech, fieldCoords;

	private static String selectionOverride = null;

	public StatusBar(WindowFrame frame)
	{
		super(new GridBagLayout());

		setBorder(new BevelBorder(BevelBorder.LOWERED));

		//fillPosition = 0;
		this.frame = frame;
		addField(fieldSelected = new JLabel(), 0);
		addField(fieldSize = new JLabel(), 1);
		addField(fieldTech = new JLabel(), 2);
		fieldCoords = new JLabel();
		if (User.isShowCursorCoordinates()) addField(fieldCoords, 3);
	}

	private void addField(JLabel field, int index)
	{
//		JPanel frame = new JPanel();
//		frame.setBorder(new LineBorder(Color.BLACK));
//		frame.add(field);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = index;   gbc.gridy = 0;
		gbc.weightx = 0.5;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(field, gbc);
	}

	public static void setShowCoordinates(boolean show)
	{
		User.setShowCursorCoordinates(show);
		if (TopLevel.isMDIMode())
		{
			StatusBar sb = TopLevel.getTopLevel().getStatusBar();
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
			sb = TopLevel.getTopLevel().getStatusBar();
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

	/**
	 * Method to update the status bar from current values.
	 * Call this when any of those values change.
	 */
	public static void updateStatusBar()
	{
		if (TopLevel.isMDIMode())
		{
			StatusBar sb = TopLevel.getTopLevel().getStatusBar();
			sb.redoStatusBar();
		} else
		{
			for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)it.next();
				StatusBar sb = wf.getFrame().getStatusBar();
				sb.redoStatusBar();
			}
		}
	}
	
	private void redoStatusBar()
	{
		String selectedMsg = "NOTHING SELECTED";
		if (selectionOverride != null)
		{
			selectedMsg = selectionOverride;
		} else
		{
			// count the number of nodes and arcs selected
			int nodeCount = 0, arcCount = 0, textCount = 0;
			NodeInst theNode = null;
			ArcInst theArc = null;
            PortInst thePort = null;
			Highlight theText = null;
			for(Iterator hIt = Highlight.getHighlights(); hIt.hasNext(); )
			{
				Highlight h = (Highlight)hIt.next();
				if (h.getType() == Highlight.Type.EOBJ)
				{
					ElectricObject eObj = (ElectricObject)h.getElectricObject();
                    if (eObj instanceof PortInst)
                    {
                        thePort = (PortInst)eObj;
                        theNode = thePort.getNodeInst();
                        nodeCount++;
                    } else if (eObj instanceof NodeInst)
					{
						theNode = (NodeInst)eObj;
						nodeCount++;
					} else if (eObj instanceof ArcInst)
					{
						theArc = (ArcInst)eObj;
						arcCount++;
					}
				} else if (h.getType() == Highlight.Type.TEXT)
				{
					theText = h;
					textCount++;
				}
			}
			if (nodeCount + arcCount + textCount == 1)
			{
				if (nodeCount == 1)
                {
                    if (thePort != null)
                        selectedMsg = "SELECTED NODE: " + theNode.describe() +
							" PORT: \"" + thePort.getPortProto().getProtoName() + "\"";
                    else
                        selectedMsg = "SELECTED NODE: " + theNode.describe();
                } else
				{
					if (arcCount == 1) selectedMsg = "SELECTED ARC: " + theArc.describe(); else
					{
						if (theText.getVar() != null)
						{
							selectedMsg = "SELECTED TEXT: " + theText.getVar().getFullDescription(theText.getElectricObject());
						} else
						{
							if (theText.getName() != null)
							{
								if (theText.getElectricObject() instanceof NodeInst)
									selectedMsg = "SELECTED TEXT: Node name"; else
										selectedMsg = "SELECTED TEXT: Arc name";
							} else
							{
								if (theText.getElectricObject() instanceof Export)
									selectedMsg = "SELECTED TEXT: Export name"; else
										selectedMsg = "SELECTED TEXT: Cell instance name";
							}
						}
					}
				}
			} else
			{
				if (nodeCount + arcCount + textCount > 0)
				{
					selectedMsg = "SELECTED:";
					if (nodeCount > 0) selectedMsg += " " + nodeCount + " NODES";
					if (arcCount > 0)
					{
						if (nodeCount > 0) selectedMsg += ",";
						selectedMsg += " " + arcCount + " ARCS";
					}
					if (textCount > 0)
					{
						if (nodeCount + arcCount > 0) selectedMsg += ",";
						selectedMsg += " " + textCount + " TEXT";
					}
				}
			}
		}
		fieldSelected.setText(selectedMsg);

		Cell cell = null;
		if (frame == null)
		{
			EditWindow wnd = EditWindow.getCurrent();
			if (wnd != null) cell = wnd.getCell();
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
				Variable var = cell.getVar(Cell.CELL_TEXT_KEY);
				if (var != null) len = var.getLength();
				sizeMsg = "LINES: " + len;
			} else
			{
				String width = Double.toString(cell.getBounds().getWidth());
				Rectangle2D bounds = cell.getBounds();
				sizeMsg = "SIZE: " + TextUtils.formatDouble(bounds.getWidth(),3) + "x" +
                        TextUtils.formatDouble(bounds.getHeight(), 3);
			}
		}
		fieldSize.setText(sizeMsg);

		Technology tech = Technology.getCurrent();
		if (tech != null)
			fieldTech.setText("TECHNOLOGY: " + tech.getTechName() + " (scale=" + (tech.getScale()/2.0) + "nm)");

		if (coords == null) fieldCoords.setText(""); else
			fieldCoords.setText(coords);
	}

}
