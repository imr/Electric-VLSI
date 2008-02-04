/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TechEditWizardPanel.java
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.tecEditWizard;

import com.sun.electric.tool.user.dialogs.EDialog;

import java.awt.Frame;

import javax.swing.JPanel;

/**
 * This class defines a superstructure for a panel in the Technology Editor Wizard dialog.
 */
public class TechEditWizardPanel extends EDialog
{
    private final TechEditWizard parent;

	public TechEditWizardPanel(TechEditWizard parent, boolean modal)
	{
		super((Frame)parent.getOwner(), modal);
        this.parent = parent;
	}

	/** return the panel to use for this Technology Editor Wizard tab. */
	public JPanel getPanel() { return null; }

	/** return the name of this Technology Editor Wizard tab. */
	public String getName() { return ""; }

    protected TechEditWizardData getNumericData() { return parent.getTechEditData(); }
    
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
