/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PreferencePanel.java
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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.dialogs.EDialog;

import javax.swing.JPanel;

/**
 * This class defines a superstructure for a panel in the Preferences dialog.
 */
public class PreferencePanel extends EDialog
{
	protected Technology curTech = Technology.getCurrent();
	protected Library curLib = Library.getCurrent();

	private boolean inited = false;

	public PreferencePanel(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
	}

	public JPanel getPanel() { return null; }

	public String getName() { return ""; }

	public boolean isInited() { return inited; }

	public void setInited() { inited = true; }

    public Technology getTech() { return curTech; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Frame tab.
	 */
	public void init() {}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Frame tab.
	 */
	public void term() {}

}
