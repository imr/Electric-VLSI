/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GDSMap.java
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Foundry;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Class to handle the "import GDS MAP" command.
 */
public class GDSMap extends EDialog
{
	private HashMap<String,JComboBox> assocCombos;
	private List<MapLine> drawingEntries;
	private List<MapLine> pinEntries;
	private Technology tech;

	private static class MapLine
	{
		String name;
		int layer;
		int type;
	}

	public static void importMapFile()
	{
		String fileName = OpenFile.chooseInputFile(FileType.GDSMAP, "GDS Layer Map File");
		if (fileName == null) return;
		HashSet<String> allNames = new HashSet<String>();
		List<MapLine> drawingEntries = new ArrayList<MapLine>();
		List<MapLine> pinEntries = new ArrayList<MapLine>();
		URL url = TextUtils.makeURLToFile(fileName);
		try
		{
			URLConnection urlCon = url.openConnection();
			InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
			LineNumberReader lineReader = new LineNumberReader(is);
			for(;;)
			{
				String buf = lineReader.readLine();
				if (buf == null) break;
				buf = buf.trim();
				if (buf.length() == 0) continue;
				if (buf.charAt(0) == '#') continue;

				// get the layer name
				int spaPos = buf.indexOf(' ');
				if (spaPos < 0) continue;
				String layerName = buf.substring(0, spaPos);
				buf = buf.substring(spaPos+1).trim();

				// get the layer purpose
				spaPos = buf.indexOf(' ');
				if (spaPos < 0) continue;
				String layerPurpose = buf.substring(0, spaPos);
				buf = buf.substring(spaPos+1).trim();

				// get the GDS number and type
				spaPos = buf.indexOf(' ');
				if (spaPos < 0) continue;
				int gdsNumber = TextUtils.atoi(buf.substring(0, spaPos));
				buf = buf.substring(spaPos+1).trim();
				int gdsType = TextUtils.atoi(buf);

				// only want layers whose purpose is "drawing" or "pin"
				if (!layerPurpose.equalsIgnoreCase("drawing") &&
					!layerPurpose.equalsIgnoreCase("pin")) continue;

				// remember this for later
				MapLine ml = new MapLine();
				ml.name = layerName;
				ml.layer = gdsNumber;
				ml.type = gdsType;
				if (layerPurpose.equalsIgnoreCase("drawing")) drawingEntries.add(ml); else
					pinEntries.add(ml);
				allNames.add(layerName);
			}
			lineReader.close();
		} catch (IOException e)
		{
			System.out.println("Error reading " + fileName);
			return;
		}
		GDSMap mapDialog = new GDSMap(TopLevel.getCurrentJFrame(), allNames, drawingEntries, pinEntries);
	}

	/** Creates new form Layer Map Association */
	public GDSMap(Frame parent, HashSet<String> allNames, List<MapLine> drawingEntries, List<MapLine> pinEntries)
	{
		super(parent, true);
		this.drawingEntries = drawingEntries;
		this.pinEntries = pinEntries;
		this.tech = Technology.getCurrent();
        getContentPane().setLayout(new GridBagLayout());
        setTitle("GDS Layer Map Association");
        setName("");
        addWindowListener(new WindowAdapter()
		{
            public void windowClosing(WindowEvent evt) { closeDialog(evt); }
        });

		// make a list of names
		List<String> nameList = new ArrayList<String>();
		for(String name : allNames)
			nameList.add(name);
		Collections.sort(nameList, TextUtils.STRING_NUMBER_ORDER);

		// show the list
		assocCombos = new HashMap<String,JComboBox>();
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		int row = 1;
		for(String name : nameList)
		{
			JLabel lab = new JLabel(name);		
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0; gbc.gridy = row;
			gbc.anchor = GridBagConstraints.WEST;
			panel.add(lab, gbc);

			JComboBox comboBox = new JComboBox();
			comboBox.addItem("<<IGNORE>>");
			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
				comboBox.addItem(lIt.next().getName());
			String savedAssoc = getSavedAssoc(name);
			if (savedAssoc.length() > 0) comboBox.setSelectedItem(savedAssoc);
			gbc = new GridBagConstraints();
			gbc.gridx = 1; gbc.gridy = row;
			panel.add(comboBox, gbc);
			row++;
			assocCombos.put(name, comboBox);
		}

		JScrollPane assocPane = new JScrollPane();
		assocPane.setViewportView(panel);			
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;   gbc.weighty = 1.0;
		gbc.insets = new Insets(4, 4, 4, 4);
        getContentPane().add(assocPane, gbc);

		JLabel lab = new JLabel("Mapping these layer names to the " + tech.getTechName() + " technology:");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(4, 4, 4, 4);
        getContentPane().add(lab, gbc);

		JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener()
		{
            public void actionPerformed(ActionEvent evt) { ok(); }
        });
		gbc = new GridBagConstraints();
		gbc.gridx = 1;   gbc.gridy = 2;
		gbc.insets = new Insets(4, 4, 4, 4);
        getContentPane().add(ok, gbc);
        getRootPane().setDefaultButton(ok);

		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener()
		{
            public void actionPerformed(ActionEvent evt) { closeDialog(null); }
        });
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 2;
		gbc.insets = new Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gbc);

        pack();
		setVisible(true);
	}

	public void termDialog()
	{
		for(String name : assocCombos.keySet())
		{
			JComboBox combo = assocCombos.get(name);
			String layerName = "";
			if (combo.getSelectedIndex() != 0)
				layerName = (String)combo.getSelectedItem();
			setSavedAssoc(name, layerName);
		}

		// wipe out all GDS layer associations
        Foundry foundry = tech.getSelectedFoundry();

        if (foundry == null)
        {
            System.out.println("No foundry associated for the mapping");
            return;
        }

        for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
        {
            Layer layer = it.next();
//                layer.setGDSLayer("");
            foundry.setGDSLayer(layer, "");
        }

		// set the associations
		for(MapLine ml : drawingEntries)
		{
			String layerName = (String)getSavedAssoc(ml.name);
			if (layerName.length() == 0) continue;
			Layer layer = tech.findLayer(layerName);
			if (layer == null) continue;
			String layerInfo = "" + ml.layer;
			if (ml.type != 0) layerInfo += "/" + ml.type;
			for(MapLine pMl : pinEntries)
			{
				if (pMl.name.equals(ml.name))
				{
					if (pMl.layer != -1)
					{
						layerInfo += "," + pMl.layer;
						if (pMl.type != 0) layerInfo += "/" + pMl.type;
						layerInfo += "p";
					}
					break;
				}
			}
            foundry.setGDSLayer(layer, layerInfo);
		}
	}

	private void ok()
	{
		termDialog();
		closeDialog(null);
	}

	/** Closes the dialog */
	private void closeDialog(WindowEvent evt)
	{
		setVisible(false);
		dispose();
	}

	HashMap<String,Pref> savedAssocs = new HashMap<String,Pref>();

	private String getSavedAssoc(String mapName)
	{
		Pref pref = savedAssocs.get(mapName);
		if (pref == null)
		{
			pref = Pref.makeStringPref("GDSMappingFor" + mapName, IOTool.getIOTool().prefs, "");
			savedAssocs.put(mapName, pref);
		}
		return pref.getString();
	}

	private void setSavedAssoc(String mapName, String layerName)
	{
		Pref pref = savedAssocs.get(mapName);
		if (pref == null)
		{
			pref = Pref.makeStringPref("GDSMappingFor" + mapName, IOTool.getIOTool().prefs, "");
			savedAssocs.put(mapName, pref);
		}
		pref.setString(layerName);
	}

}
