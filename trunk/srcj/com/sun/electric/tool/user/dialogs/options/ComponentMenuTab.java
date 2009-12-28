/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ComponentMenuTab.java
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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveNodeGroup;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Xml;
import com.sun.electric.tool.user.dialogs.ComponentMenu;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

/**
 * Class to handle the "Component Menu" tab of the preferences dialog.
 */
public class ComponentMenuTab extends PreferencePanel
{
	private ComponentMenu theMenu;
	private Technology tech;

	/** Creates new form ComponentMenu */
	public ComponentMenuTab(Frame parent, boolean modal)
	{
		super(parent, modal);
		theMenu = new ComponentMenu(parent, false);
	}

	/** return the panel to use for the user preferences. */
	public JPanel getUserPreferencesPanel() { return theMenu.getPanel(); }

	/** return the name of this preferences tab. */
	public String getName() { return "Component Menu"; }

	/**
	 * Method called at the start of the dialog.
	 */
	public void init()
	{
		tech = Technology.getCurrent();

		// make an XML technology to correspond with this one
		Xml.Technology xTech = new Xml.Technology();
		for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			Xml.ArcProto curArc = new Xml.ArcProto();
			curArc.name = ap.getName();
			xTech.arcs.add(curArc);
		}
        HashSet<PrimitiveNodeGroup> groupsDone = new HashSet<PrimitiveNodeGroup>();
        for (Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); ) {
            PrimitiveNode pnp = it.next();
            if (pnp.getFunction() == PrimitiveNode.Function.NODE) continue;
            PrimitiveNodeGroup group = pnp.getPrimitiveNodeGroup();
            if (group != null) {
                if (groupsDone.contains(group))
                    continue;
                Xml.PrimitiveNodeGroup ng = new Xml.PrimitiveNodeGroup();
                for (PrimitiveNode pn: group.getNodes()) {
                    Xml.PrimitiveNode n = new Xml.PrimitiveNode();
                    n.name = pn.getName();
                    ng.nodes.add(n);
                }
                xTech.nodeGroups.add(ng);
                groupsDone.add(group);
            } else {
                Xml.PrimitiveNodeGroup ng = new Xml.PrimitiveNodeGroup();
                ng.isSingleton = true;
                Xml.PrimitiveNode n = new Xml.PrimitiveNode();
                n.name = pnp.getName();
                ng.nodes.add(n);
                xTech.nodeGroups.add(ng);
            }
        }

		// build a set of XML objects that refer to this XML technology
		Object[][] menuArray = makeMenuArray(ComponentMenu.getMenuPalette(tech));
		Object[][] defMenuArray = makeMenuArray(tech.getFactoryMenuPalette());
		theMenu.showTechnology(tech.getTechName(), xTech, menuArray, defMenuArray);
	}

	private Object[][] makeMenuArray(Xml.MenuPalette curMenu)
	{
        int menuWid = curMenu.numColumns;
        int menuHei = (curMenu.menuBoxes.size() + menuWid - 1)/menuWid;
		Object[][] menuArray = new Object[menuHei][menuWid];
		for(int y=0; y<menuHei; y++)
		{
			for(int x=0; x<menuWid; x++)
			{
                int index = y*menuWid + x;
                List<?> menuBoxList = index < curMenu.menuBoxes.size() ? curMenu.menuBoxes.get(index) : null;
                if (menuBoxList == null || menuBoxList.isEmpty()) continue;
                menuArray[y][x] = menuBoxList.size() > 1 ? new ArrayList<Object>(menuBoxList) : menuBoxList.get(0);
			}
		}
		return menuArray;
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the tab.
	 */
	public void term()
	{
		if (!theMenu.isChanged()) return;
		Xml.MenuPalette xmp = new Xml.MenuPalette();
		Object[][] menuArray = theMenu.getMenuInfo();
		int menuHei = menuArray.length;
		int menuWid = menuArray[0].length;
		xmp.numColumns = menuWid;
		xmp.menuBoxes = new ArrayList<List<?>>();
		for(int y=0; y<menuHei; y++)
		{
			for(int x=0; x<menuWid; x++)
			{
				Object item = null;
				if (menuArray[y] != null)
					item = menuArray[y][x];
				if (item instanceof List)
				{
					xmp.menuBoxes.add((List)item);
				} else
				{
					List<Object> subList = new ArrayList<Object>();
					if (item != null) subList.add(item);
					xmp.menuBoxes.add(subList);
				}
			}
		}
        ComponentMenu.ComponentMenuPreferences cmp = new ComponentMenu.ComponentMenuPreferences(false);
        cmp.menuXmls.put(tech, xmp.writeXml());
        putPrefs(cmp);

		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			wf.getPaletteTab().loadForTechnology(tech, wf);
		}
	}

	/**
	 * Method called when the factory reset is requested for just this panel.
	 * @return true if the panel can be reset "in place" without redisplay.
	 */
	public boolean resetThis()
	{
		theMenu.factoryReset();
		return true;
	}

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
        if (tech == null)
            return; // nothing to reset since the dialog hasn't been used.
        
        ComponentMenu.ComponentMenuPreferences cmp = new ComponentMenu.ComponentMenuPreferences(true);
        putPrefs(cmp);
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			wf.getPaletteTab().loadForTechnology(tech, wf);
		}
	}
}
