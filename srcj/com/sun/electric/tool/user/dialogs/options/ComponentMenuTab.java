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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Xml;
import com.sun.electric.tool.user.dialogs.ComponentMenu;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Frame;
import java.util.ArrayList;
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

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return theMenu.getPanel(); }

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
		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = it.next();
			Xml.PrimitiveNode curNode = new Xml.PrimitiveNode();
			curNode.name = np.getName();
			curNode.function = np.getFunction();
			xTech.nodes.add(curNode);
		}

		// build a set of XML objects that refer to this XML technology
		Object[][] menuArray = makeMenuArray(xTech, tech.getNodesGrouped(null));
		Object[][] defMenuArray = makeMenuArray(xTech, tech.filterNodeGroups(tech.getDefaultNodesGrouped()));
		theMenu.showTechnology(tech.getTechName(), xTech, menuArray, defMenuArray);
	}

	private Object[][] makeMenuArray(Xml.Technology xTech, Object[][] curMenu)
	{
		int menuWid = curMenu[0].length;
		int menuHei = curMenu.length;
		Object[][] menuArray = new Object[menuHei][menuWid];
		for(int y=0; y<menuHei; y++)
		{
			for(int x=0; x<menuWid; x++)
			{
				Object menuEntry = curMenu[y][x];
				if (menuEntry instanceof List)
				{
					List<Object> subList = new ArrayList<Object>();
					for(Object it : (List)menuEntry)
						subList.add(convertToXML(it, xTech));
					menuArray[y][x] = subList;
				} else
				{
					menuArray[y][x] = convertToXML(menuEntry, xTech);
				}
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
		xmp.menuBoxes = new ArrayList<List<Object>>();
		Object[][] convMenuArray = new Object[menuHei][menuWid];
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
					List<Object> subConvList = new ArrayList<Object>();
					for(Object it : (List)item)
						subConvList.add(convertFromXML(it));
					convMenuArray[y][x] = subConvList;
				} else
				{
					List<Object> subList = new ArrayList<Object>();
					if (item != null) subList.add(item);
					xmp.menuBoxes.add(subList);
					convMenuArray[y][x] = convertFromXML(item);
				}
			}
		}
		tech.setNodesGrouped(convMenuArray, xmp.writeXml());

		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			wf.getPaletteTab().loadForTechnology(tech, wf);
		}
	}

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology t = it.next();
			Object [][] menu = t.getDefaultNodesGrouped();
			t.setNodesGrouped(t.filterNodeGroups(menu), "");
		}
	}

	private Object convertToXML(Object obj, Xml.Technology xTech)
	{
		if (obj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)obj;
			Xml.MenuNodeInst xni = new Xml.MenuNodeInst();
			xni.protoName = ni.getProto().getName();
			xni.function = ni.getFunction();
			Variable var = ni.getVar(Technology.TECH_TMPVAR);
			if (var != null)
			{
				xni.text = (String)var.getObject();
				xni.fontSize = var.getTextDescriptor().getSize().getSize();
			}
			xni.rotation = ni.getAngle();
			return xni;
		} else if (obj instanceof NodeProto)
		{
			NodeProto np = (NodeProto)obj;
			if (np instanceof Cell)
			{
				Cell cell = (Cell)np;
				return "LOADCELL " + cell.libDescribe();
			}
			for(Xml.PrimitiveNode xnp : xTech.nodes)
			{
				if (xnp.name.equals(np.getName())) return xnp;
			}
			Xml.PrimitiveNode xnp = new Xml.PrimitiveNode();
			xnp.name = np.getName();
			return xnp;
		} else if (obj instanceof ArcProto)
		{
			ArcProto ap = (ArcProto)obj;
			for(Xml.ArcProto xap : xTech.arcs)
			{
				if (xap.name.equals(ap.getName())) return xap;
			}
			Xml.ArcProto xap = new Xml.ArcProto();
			xap.name = ap.getName();
			return xap;
		}
		return obj;
	}

	private Object convertFromXML(Object obj)
	{
		if (obj instanceof Xml.MenuNodeInst)
		{
			Xml.MenuNodeInst xni = (Xml.MenuNodeInst)obj;
			PrimitiveNode np = tech.findNodeProto(xni.protoName);
			NodeInst ni = Technology.makeNodeInst(np, xni.function, xni.rotation, xni.text != null && xni.text.length() > 0,
				xni.text, xni.fontSize);
			return ni;
		} else if (obj instanceof Xml.PrimitiveNode)
		{
			Xml.PrimitiveNode xnp = (Xml.PrimitiveNode)obj;
			PrimitiveNode np = tech.findNodeProto(xnp.name);
			return np;
		} else if (obj instanceof Xml.ArcProto)
		{
			Xml.ArcProto xap = (Xml.ArcProto)obj;
			ArcProto ap = tech.findArcProto(xap.name);
			return ap;
		}
		return obj;
	}

}
