/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: InvisibleLayerConfiguration.java
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.text.Pref;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.menus.WindowMenu;
import com.sun.electric.util.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to manage saved collections of invisible layers.
 * Each configuration has a name, a list of Layers that are invisible, and an associated Technology
 * (all Layers must be in the same Technology).
 */
public class InvisibleLayerConfiguration
{
    public static final int NUM_CONFIGS = 13;
	private static InvisibleLayerConfiguration onlyOne = new InvisibleLayerConfiguration();

	/** Maps configuration name to a List of Technology-configurations */
	private Map<String,List<String>> configurations;
	private Pref savedConfigurations;

	/**
	 * Method to return the singleton of this class.
	 * @return the only instance of this class.
	 */
	public static InvisibleLayerConfiguration getOnly() { return onlyOne; }

    /**
	 * Constructor gets the saved configurations from the Preferences.
	 */
	private InvisibleLayerConfiguration()
	{
		configurations = new HashMap<String,List<String>>();
		Pref.Group prefs = Pref.groupForPackage(InvisibleLayerConfiguration.class);
		savedConfigurations = Pref.makeStringPref("LayerVisibilityConfigurations", prefs, "");
		String sc = savedConfigurations.getString();
		boolean [] overridden = new boolean[NUM_CONFIGS];
		for(;;)
		{
			int openPos = sc.indexOf('[');
			if (openPos < 0) break;
			int closePos = sc.indexOf(']');
			if (closePos < 0) break;
			String config = sc.substring(openPos+1, closePos);
			sc = sc.substring(closePos+1);

			String[] techParts = config.split("\t");
			if (techParts.length <= 1) continue;
			String cName = techParts[0];
			List<String> techPartsList = new ArrayList<String>();
			for(int i=1; i<techParts.length; i++)
			{
				if (techParts[i].startsWith("("))
				{
					int index = TextUtils.atoi(techParts[i].substring(1));
					overridden[index] = true;
				}
				techPartsList.add(techParts[i]);
			}
			configurations.put(cName, techPartsList);
		}
		for(int i=0; i<NUM_CONFIGS; i++)
		{
			if (overridden[i]) continue;
			String menuName = getDefaultHardwiredName(i);
			String value = "(" + i + ")";
			List<String> techPartsList = new ArrayList<String>();
			techPartsList.add(value);
			configurations.put(menuName, techPartsList);
		}
	}

	private String getDefaultHardwiredName(int index)
	{
		String menuName = "Set ";
		if (index == 0) menuName += "All"; else
			menuName += "M" + index;
		menuName += " Visible";
		return menuName;
	}

	public String getMenuName(int index)
	{
		String cName = findHardWiredConfiguration(index);
		if (cName == null) return getDefaultHardwiredName(index);
		return cName;
	}

	/**
	 * Method to save the configuration state to the Preferences.
	 */
	private void saveConfigurations()
	{
		StringBuffer sb = new StringBuffer();
		for(String cName : configurations.keySet())
		{
			List<String> techParts = configurations.get(cName);
			sb.append('[');
			sb.append(cName);
			for(String techPart : techParts)
			{
				sb.append('\t');
				sb.append(techPart);
			}
			sb.append(']');
		}
		savedConfigurations.setString(sb.toString());
		WindowMenu.setDynamicVisibleLayerMenus();
	}

	/**
	 * Method to tell whether a invisible layer configuration name exists.
	 * @param cName the name of the invisible layer configuration.
	 * @return true if there is a invisible layer configuration with that name.
	 */
	public boolean exists(String cName)
	{
		List<String> techParts = configurations.get(cName);
		return techParts != null;
	}

	/**
	 * Method to add a invisible layer configuration.
	 * @param cName the name of the new invisible layer configuration.
	 * @param hardWiredIndex the hard-wired value (from 0-9) for pre-bound configurations.
	 * @param tech the Technology in which these layers reside.
	 * @param layers the list of invisible layers in the configuration.
	 */
	public void addConfiguration(String cName, int hardWiredIndex, Technology tech, List<Layer> layers)
	{
		StringBuffer sb = new StringBuffer();
		if (hardWiredIndex >= 0) sb.append("(" + hardWiredIndex + ")");
		sb.append(tech.getTechName());
		for(Layer layer : layers)
		{
			sb.append(',');
			if (layer.getTechnology() != tech) sb.append(layer.getTechnology().getTechName() + ":");
			sb.append(layer.getName());
		}
		List<String> techParts = configurations.get(cName);
		List<String> newTechParts = new ArrayList<String>();
		boolean found = false;
		if (techParts != null)
		{
			for(String techPart : techParts)
			{
				// make a set of all invisible layers
				String[] iLayers = techPart.split(",");
				if (iLayers.length == 0) continue;
				String techName = iLayers[0];
				int endPos = techName.indexOf(')');
				if (endPos >= 0) techName = techName.substring(endPos+1);
				Technology thisTech = Technology.findTechnology(techName);
				if (thisTech == null) continue;
				if (thisTech == tech) { techPart = sb.toString(); found = true; }
				newTechParts.add(techPart);
			}
		}
		if (!found) newTechParts.add(sb.toString());
		configurations.put(cName, newTechParts);
		saveConfigurations();
	}

	/**
	 * Method to rename an invisible layer configuration.
	 * @param cName the name of the configuration to rename.
	 * @param newName the new configuration name.
	 */
	public void renameConfiguration(String cName, String newName)
	{
		List<String> techParts = configurations.get(cName);
		if (techParts == null) return;
		configurations.remove(cName);
		configurations.put(newName, techParts);
		saveConfigurations();
	}

	/**
	 * Method to delete an invisible layer configuration for a given Technology.
	 * @param cName the name of the invisible layer configuration to delete.
	 * @param tech the Technology to delete.
	 */
	public void deleteConfiguration(String cName, Technology tech)
	{
		List<String> techParts = configurations.get(cName);
		List<String> newTechParts = new ArrayList<String>();
		int hardIndex = -1;
		if (techParts != null)
		{
			for(String techPart : techParts)
			{
				String[] iLayers = techPart.split(",");
				if (iLayers.length == 0) continue;
				String techName = iLayers[0];
				int endPos = techName.indexOf(')');
				if (endPos >= 0)
				{
					hardIndex = TextUtils.atoi(techName.substring(1));
					techName = techName.substring(endPos+1);
				}
				Technology thisTech = Technology.findTechnology(techName);
				if (thisTech == tech) continue;
				newTechParts.add(techPart);
			}
		}
		if (newTechParts.size() == 0 && hardIndex >= 0)
		{
			String value = "(" + hardIndex + ")";
			newTechParts.add(value);
		}
		if (newTechParts.size() == 0) configurations.remove(cName); else
			configurations.put(cName, newTechParts);
		saveConfigurations();
	}

	/**
	 * Method to return the names of all invisible layer configurations.
	 * @return a List of invisible layer configuration names.
	 */
	public List<String> getConfigurationNames()
	{
		List<String> configs = new ArrayList<String>();
		for(String key : configurations.keySet())
			configs.add(key);
        Collections.sort(configs);
		return configs;
	}

	/**
	 * Method to get the Technologies associated with a invisible layer configuration.
	 * @param cName the name of the invisible layer configuration.
	 * @return a List of Technologies associated with that invisible layer configuration
	 * (may be empty).
	 */
	public List<Technology> getConfigurationTechnology(String cName)
	{
		List<Technology> techs = new ArrayList<Technology>();
		List<String> techParts = configurations.get(cName);
		if (techParts != null)
		{
			for(String techPart : techParts)
			{
				// make a set of all invisible layers
				String[] iLayers = techPart.split(",");
				if (iLayers.length != 0)
				{
					String techName = iLayers[0];
					int endPos = techName.indexOf(')');
					if (endPos >= 0) techName = techName.substring(endPos+1);
					Technology tech = Technology.findTechnology(techName);
					if (tech != null) techs.add(tech);
				}
			}
		}
		return techs;
	}

	/**
	 * Method to find the configuration that is hard-wired to a given index.
	 * @param index the index (from 0 to 9).
	 * @return the name of the configuration bound to that index (null if none).
	 */
	public String findHardWiredConfiguration(int index)
	{
		for(String cName : configurations.keySet())
		{
			List<String> techLayers = configurations.get(cName);
			if (techLayers == null) continue;
			for(String techLayer : techLayers)
			{
				if (techLayer.startsWith("("))
				{
					if (index != TextUtils.atoi(techLayer.substring(1))) continue;
					return cName;
				}
			}
		}
		return null;
	}

	/**
	 * Method to get the "hard wired" index of this visibility configuration name.
	 * Some visibility configurations are hard-wired to the SHIFT-0 through SHIFT-9
	 * keys, and these will return a value of 0 through 9.
	 * @param cName the name of the invisible layer configuration.
	 * @return the hard-wired index (-1 if none).
	 */
	public int getConfigurationHardwiredIndex(String cName)
	{
		List<String> techLayers = configurations.get(cName);
		if (techLayers != null)
		{
			// make a set of all invisible layers
			for(String techLayer : techLayers)
			{
				String[] iLayers = techLayer.split(",");
				if (iLayers.length != 0)
				{
					String techName = iLayers[0];
					if (techName.startsWith("("))
						return TextUtils.atoi(techName.substring(1));
				}
			}
		}
		return -1;
	}

	/**
	 * Method to return the invisible layers in an invisible layer configuration.
	 * @param cName the name of the invisible layer configuration.
	 * @return a Map from each Technology to a set of invisible Layers.
	 * Any technology not in the map has all layers visible.
	 */
	public Map<Technology,List<Layer>> getConfigurationValue(String cName)
	{
		Map<Technology,List<Layer>> invisibleLayers = new HashMap<Technology,List<Layer>>();
		List<String> techParts = configurations.get(cName);
		if (techParts != null)
		{
			for(String techPart : techParts)
			{
				// make a set of all invisible layers
				String[] iLayers = techPart.split(",");
				if (iLayers.length != 0)
				{
					String techName = iLayers[0];
					int endPos = techName.indexOf(')');
					if (endPos >= 0) techName = techName.substring(endPos+1);
					Technology tech = Technology.findTechnology(techName);
					if (tech == null) continue;
					for(int i=1; i<iLayers.length; i++)
					{
						String iLayer = iLayers[i];
						int colonPos = iLayer.indexOf(':');
						Technology findTech = tech;
						if (colonPos >= 0)
						{
							findTech = Technology.findTechnology(iLayer.substring(0, colonPos));
							iLayer = iLayer.substring(colonPos+1);
						}
						Layer lay = findTech.findLayer(iLayer);
						if (lay == null) continue;
						List<Layer> curLays = invisibleLayers.get(tech);
						if (curLays == null) invisibleLayers.put(tech, curLays = new ArrayList<Layer>());
						curLays.add(lay);
					}
				}
			}
		}
		return invisibleLayers;
	}
}
