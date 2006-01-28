/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GDSLayers.java
 * Input/output tool: GDS layer parsing
 * Written by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.io;

import com.sun.electric.database.text.TextUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class to define GDS layer information.
 */
public class GDSLayers
{
	private List<Integer> normalLayers;
	private int pinLayer;
	private int textLayer;

	public GDSLayers()
	{
		normalLayers = new ArrayList<Integer>();
		pinLayer = -1;
		textLayer = -1;
	}

	public int getNumLayers() { return normalLayers.size(); }

	public Iterator<Integer> getLayers() { return normalLayers.iterator(); }

	public Integer getFirstLayer()
	{
		if (normalLayers.size() == 0) return new Integer(0);
		return normalLayers.get(0);
	}

	public int getPinLayer() { return pinLayer; }

	public int getTextLayer() { return textLayer; }

	/**
	 * Method to determine if the numbers in this GDSLayers are the same as another.
	 * @param other the other GDSLayers being compared with this.
	 * @return true if they have the same values.
	 */
	public boolean equals(GDSLayers other)
	{
		if (pinLayer != other.pinLayer) return false;
		if (textLayer != other.textLayer) return false;
		return normalLayers.equals(other.normalLayers);
	}

	/**
	 * Method to parse the GDS layer string and get the layer numbers and types (plain, text, and pin).
	 * @param string the GDS layer string, of the form [NUM[/TYP]]*[,NUM[/TYP]t][,NUM[/TYP]p]
	 * @return a GDSLayers object with the values filled-in.
	 */
	public static GDSLayers parseLayerString(String string)
	{
		GDSLayers answers = new GDSLayers();
		for(;;)
		{
			String trimmed = string.trim();
			if (trimmed.length() == 0) break;
			int slashPos = trimmed.indexOf('/');
			int endPos = trimmed.indexOf(',');
			if (endPos < 0) endPos = trimmed.length();

			int number = TextUtils.atoi(trimmed);
			if (number != 0 || trimmed.equals("0"))
			{
				int type = 0;
				if (slashPos >= 0 && slashPos < endPos)
					type = TextUtils.atoi(trimmed.substring(slashPos+1));
				char lastCh = trimmed.charAt(endPos-1);
				if (lastCh == 't')
				{
					answers.textLayer = number | (type << 16);
				} else if (lastCh == 'p')
				{
					answers.pinLayer = number | (type << 16);
				} else
				{
					Integer normalLayer = new Integer(number | (type << 16));
					answers.normalLayers.add(normalLayer);
				}
				if (endPos == trimmed.length()) break;
			}
			string = trimmed.substring(endPos+1);
		}
		return answers;
	}
}

