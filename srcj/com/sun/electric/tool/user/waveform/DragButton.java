/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DragButton.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import javax.swing.JButton;

/**
 * Class to extend a JButton so that it is draggable.
 */
public class DragButton extends JButton implements DragGestureListener, DragSourceListener
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
