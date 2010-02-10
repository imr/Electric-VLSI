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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.PrefPackage;
import com.sun.electric.database.text.Setting;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;

import java.awt.Frame;
import java.util.Map;

import javax.swing.JPanel;

/**
 * This class defines a superstructure for a panel in the Preferences dialog.
 */
public class PreferencePanel extends EDialog
{
	private PreferencesFrame parent;
	protected Technology curTech = Technology.getCurrent();
	protected Library curLib = Library.getCurrent();

	private boolean inited = false;

	public PreferencePanel(Frame parent, boolean modal)
	{
		super(parent, modal);
	}

	public PreferencePanel(PreferencesFrame parent, boolean modal)
	{
		super((Frame)parent.getOwner(), modal);
		this.parent = parent;
	}

	/** return the JPanel to use for the user preferences. */
	public JPanel getUserPreferencesPanel() { return null; }

	/** return the JPanel to use for the project preferences. */
	public JPanel getProjectPreferencesPanel() { return null; }

	/** return the name of this preferences tab. */
	public String getName() { return ""; }

	/** Method to tell whether this preferences tab has been initialized.
	 * @return true if this preferences tab has been initialized.
	 */
	public boolean isInited() { return inited; }

	/**
	 * Method to mark that this preferences tab has been initialized.
	 */
	public void setInited() { inited = true; }

	/**
	 * Method to return the current technology for use in all preferences tabs.
	 * @return the current technology.
	 */
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

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset() {}

	/**
	 * Method called when the factory reset is requested for just one panel.
	 * @return true if the panel could be reset "in place" without redisplay.
	 */
	public boolean resetThis() { return false; }

	/**
	 * Method to Save options from specified PrefPackage into Electric Preferences subtree.
	 * @param pp PrefPackage with option values
	 */
	protected void putPrefs(PrefPackage pp) { PrefPackage.lowLevelPutPrefs(pp, Pref.getPrefRoot(), true); }

	protected TechPool getTechPool() { return TechPool.getThreadTechPool(); }

    protected EditingPreferences getEditingPreferences() { return parent.getEditingPreferences(); }
    protected void setEditingPreferences(EditingPreferences ep) { parent.setEditingPreferences(ep); }

	/**
	 * Method to get the boolean value on the Setting object.
	 * The object must have been created as "boolean".
	 * @param setting setting object.
	 * @return the boolean value on the Setting object.
	 */
    public boolean getBoolean(Setting setting) { return ((Boolean)getValue(setting)).booleanValue(); }

	/**
	 * Method to get the integer value on the Setting object.
	 * The object must have been created as "integer".
	 * @param setting setting object.
	 * @return the integer value on the Setting object.
	 */
	public int getInt(Setting setting) { return ((Integer)getValue(setting)).intValue(); }

	/**
	 * Method to get the long value on the Setting object.
	 * The object must have been created as "long".
	 * @param setting setting object.
	 * @return the long value on the Setting object.
	 */
	protected long getLong(Setting setting) { return ((Long)getValue(setting)).longValue(); }

	/**
	 * Method to get the double value on the Setting object.
	 * The object must have been created as "double".
	 * @param setting setting object.
	 * @return the double value on the Setting object.
	 */
	protected double getDouble(Setting setting) { return ((Double)getValue(setting)).doubleValue(); }

	/**
	 * Method to get string representation of the double value on the Setting object.
	 * The object must have been created as "double".
	 * @param setting setting object.
	 * @return the string representation of the double value on the Setting object.
	 */
	protected String getFormattedDouble(Setting setting) { return Double.toString(getDouble(setting)); }

	/**
	 * Method to get the string value on the Setting object.
	 * The object must have been created as "string".
     * @param setting setting object
	 * @return the string value on the Setting object.
	 */
	public String getString(Setting setting) { return (String)getValue(setting); }

	/**
	 * Method to set a new boolean value on Setting object.
	 * @param setting Setting object.
	 * @param v the new boolean value of Setting object.
	 */
	public void setBoolean(Setting setting, boolean v)
	{
		if (v != getBoolean(setting))
			putValue(setting, Boolean.valueOf(v));
	}

	/**
	 * Method to set a new integer value on Setting object.
	 * @param setting Setting object.
	 * @param v the new integer value of Setting object.
	 */
	public void setInt(Setting setting, int v)
	{
		if (v != getInt(setting))
			putValue(setting, Integer.valueOf(v));
	}

	/**
	 * Method to set a new long value on Setting object.
	 * @param setting Setting object.
	 * @param v the new long value of Setting object.
	 */
	protected void setLong(Setting setting, long v)
	{
		if (v != getLong(setting))
			putValue(setting, Long.valueOf(v));
	}

	/**
	 * Method to set a new double value on Setting object.
	 * @param setting Setting object.
	 * @param v the new double value of Setting object.
	 */
	protected void setDouble(Setting setting, double v)
	{
		if (v != getDouble(setting))
			putValue(setting, Double.valueOf(v));
	}

	/**
	 * Method to set a new string value on Setting object.
	 * @param setting Setting object.
	 * @param str the new string value of Setting object.
	 */
	public void setString(Setting setting, String str)
	{
		if (!str.equals(getString(setting)))
			putValue(setting, str);
	}

	private Object getValue(Setting setting) { return getContext().get(setting); }

	private void putValue(Setting setting, Object value) { getContext().put(setting, value); }

	private Map<Setting,Object> getContext() { return parent.getContext(); }
}
