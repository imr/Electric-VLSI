/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EModelessDialog.java
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

/**
 * Superclass for all modeless dialogs.
 * Remembers the last location, handles escape, etc.
 */
public class EModelessDialog extends JFrame
{
	/** Creates new form */
	public EModelessDialog(Frame parent)
	{
		super("Title", (parent == null) ? (TopLevel.getCurrentJFrame() == null ? null :
			TopLevel.getCurrentJFrame().getGraphicsConfiguration()) : parent.getGraphicsConfiguration());

        final String CANCEL_DIALOG = "cancel-dialog";
		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(accel, CANCEL_DIALOG);
		getRootPane().getActionMap().put(CANCEL_DIALOG, new AbstractAction()
		{
			public void actionPerformed(ActionEvent event) { escapePressed(); }
		});

		Point pt = EDialog.getDialogLocation(getClass());
		setLocation(pt.x, pt.y);

		setIconImage(TopLevel.getFrameIcon().getImage());
		addComponentListener(new EDialog.MoveComponentListener());

		// manage keeping dialog on top
		TopLevel.addModelessDialog(this);
	}

	/**
	 * Method to complete initialization of a dialog.
	 * Restores the size from last time.
	 */
	protected void finishInitialization()
	{
		Dimension sz = EDialog.getDialogSize(getClass());
		if (sz != null)
			setSize(sz);
	}

	/**
	 * Method to ensure that the dialog is the proper size.
	 * It must be at least as large as the user-specified size.
	 * However, it cannot grow to be larger than the display.
	 */
	protected void ensureProperSize()
	{
		Dimension sz = EDialog.getDialogSize(getClass());
		if (sz == null) return;
		Dimension curSz = getSize();
		if (curSz.width < sz.width || curSz.height < sz.height)
		{
			curSz = sz;
			setSize(sz);
		}

		// get the overall area in which to work
		Point p = getLocation();
		Rectangle [] areas = TopLevel.getDisplays();
		for(int i=0; i<areas.length; i++)
		{
			if (areas[i].contains(p))
			{
				boolean tooBig = false;
				if (p.x + curSz.width >= areas[i].x + areas[i].width)
				{
					curSz.width = areas[i].x + areas[i].width - p.x - 100;
					tooBig = true;
				}
				if (p.y + curSz.height >= areas[i].y + areas[i].height)
				{
					curSz.height = areas[i].y + areas[i].height - p.y - 100;
					tooBig = true;
				}
				if (tooBig) setSize(curSz);
			}
		}
	}

	/**
	 * Method called when the ESCAPE key is pressed.
	 * Override it to cancel the dialog.
	 */
	protected void escapePressed() {}

    protected void closeDialog()
    {
        setVisible(false);
    }

    public void toFront()
    {
    	super.toFront();
    	setState(Frame.NORMAL);
    }
}
