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

import java.awt.Cursor;
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
import java.awt.event.MouseEvent;

import javax.swing.JButton;

/**
 * Class to extend a JButton so that it is draggable.
 */
public class DragButton extends JButton implements DragGestureListener, DragSourceListener
{
	private DragSource dragSource;
	private int panelNumber;
//    private JLabel label;

	public DragButton(String s, int panelNumber)
	{
//        this.label = new JLabel(s, SwingConstants.RIGHT);
//        // workaround from http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4870187
//        this.label.setUI(new BasicLabelUI() {
//                protected String layoutCL(JLabel label, 
//                                          FontMetrics fontMetrics, 
//                                          String text, 
//                                          Icon icon, 
//                                          Rectangle viewR, 
//                                          Rectangle iconR, 
//                                          Rectangle textR) {
//                    return rev(SwingUtilities.layoutCompoundLabel(
//                                                                  (JComponent) label, 
//                                                                  fontMetrics, 
//                                                                  rev(text), 
//                                                                  icon,
//                                                                  label.getVerticalAlignment(),
//                                                                  label.getHorizontalAlignment(),
//                                                                  label.getVerticalTextPosition(),
//                                                                  label.getHorizontalTextPosition(),
//                                                                  viewR, 
//                                                                  iconR, 
//                                                                  textR,
//                                                                  label.getIconTextGap()));
//                }
//            });
//        add(label);
		super(s);
		this.panelNumber = panelNumber;
		dragSource = DragSource.getDefaultDragSource();
		dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
	}

//	public void setText(String txt)
//	{
//		super.setText(panelNumber + ": " + txt);
//	}

//	public void setForeground(Color c) {
//        if (label != null) label.setForeground(c);
//    }

//    private static String rev(String s) {
//        StringBuffer sb = new StringBuffer();
//        for(int i=s.length()-1; i>=0; i--)
//            sb.append(s.charAt(i));
//        return sb.toString();
//    }

	public void dragGestureRecognized(DragGestureEvent e)
	{
		Cursor style = DragSource.DefaultMoveDrop;
		String command = "MOVEBUTTON";

		if ((e.getTriggerEvent().getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) != 0)
//		if (ClickZoomWireListener.isRightMouse(e.getTriggerEvent()))
		{
			style = DragSource.DefaultCopyDrop;
			command = "COPYBUTTON";
		}

		// make the Transferable Object
		Transferable transferable = new StringSelection("PANEL " + panelNumber + " " + command+ " " + getText());

		// begin the drag
		dragSource.startDrag(e, style, transferable, this);
	}

	public void dragEnter(DragSourceDragEvent e) {}
	public void dragOver(DragSourceDragEvent e) {}
	public void dragExit(DragSourceEvent e) {}
	public void dragDropEnd(DragSourceDropEvent e) {}
	public void dropActionChanged (DragSourceDragEvent e) {}
}
