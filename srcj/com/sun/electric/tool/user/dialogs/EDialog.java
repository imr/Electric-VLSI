/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EDialog.java
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
package com.sun.electric.tool.user.dialogs;

import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Frame;
import java.util.HashMap;
import javax.swing.JDialog;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;

/**
 * Superclass for all dialogs that handles remembering the last location.
 */
public class EDialog extends JDialog
{
	private static HashMap locations = new HashMap();
	private Class thisClass;

	/** Creates new form Search and Replace */
	protected EDialog(Frame parent, boolean modal)
	{
		super(parent, modal);

		thisClass = this.getClass();
		Point pt = (Point)locations.get(thisClass);
		if (pt == null) pt = new Point(100, 50);
		setLocation(pt.x, pt.y);
		addComponentListener(new MoveComponentListener());
	}

	private static class MoveComponentListener implements ComponentListener
	{
		public void componentHidden(ComponentEvent e) {}
		public void componentShown(ComponentEvent e) {}
		public void componentResized(ComponentEvent e) {}
		public void componentMoved(ComponentEvent e)
		{
			Class cls = e.getSource().getClass();
			Rectangle bound = ((JDialog)e.getSource()).getBounds();
			int x = (int)bound.getMinX();
			int y = (int)bound.getMinY();
			locations.put(cls, new Point(x, y));
		}
	}
}
