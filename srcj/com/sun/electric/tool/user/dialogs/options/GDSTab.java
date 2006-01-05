/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GDSTab.java
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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.GDSLayers;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Class to handle the "GDS" tab of the Preferences dialog.
 */
public class GDSTab extends PreferencePanel
{
	/** Creates new form GDSTab */
	public GDSTab(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return gds; }

	/** return the name of this preferences tab. */
	public String getName() { return "GDS"; }

	private JList gdsLayersList;
	private DefaultListModel gdsLayersModel;
	private boolean changingGDS = false;
	private HashMap<Layer,String> layerMap;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the GDS tab.
	 */
	public void init()
	{
		gdsOutputMergesBoxes.setSelected(IOTool.isGDSOutMergesBoxes());
		gdsOutputWritesExportPins.setSelected(IOTool.isGDSOutWritesExportPins());
		gdsOutputUpperCase.setSelected(IOTool.isGDSOutUpperCase());
		gdsDefaultTextLayer.setText(Integer.toString(IOTool.getGDSOutDefaultTextLayer()));
        gdsOutputConvertsBracketsInExports.setSelected(IOTool.getGDSOutputConvertsBracketsInExports());
        gdsCellNameLenMax.setText(Integer.toString(IOTool.getGDSCellNameLenMax()));
        gdsConvertNCCExportsConnectedByParentPins.setSelected(IOTool.getGDSConvertNCCExportsConnectedByParentPins());

        gdsInputMergesBoxes.setSelected(IOTool.isGDSInMergesBoxes());
		gdsInputIncludesText.setSelected(IOTool.isGDSInIncludesText());
		gdsInputExpandsCells.setSelected(IOTool.isGDSInExpandsCells());
		gdsInputInstantiatesArrays.setSelected(IOTool.isGDSInInstantiatesArrays());
		gdsInputIgnoresUnknownLayers.setSelected(IOTool.isGDSInIgnoresUnknownLayers());

		// build the layers list
		gdsLayersModel = new DefaultListModel();
		gdsLayersList = new JList(gdsLayersModel);
		gdsLayersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		gdsLayerList.setViewportView(gdsLayersList);
		gdsLayersList.clearSelection();
		gdsLayersList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { gdsClickLayer(); }
		});
		layerMap = new HashMap<Layer,String>();
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			technologySelection.addItem(tech.getTechName());
			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = lIt.next();
				String str = layer.getName();
				String gdsLayer = layer.getGDSLayer();
				if (gdsLayer != null) str += " (" + gdsLayer + ")";
				layerMap.put(layer, str);
			}
		}
		technologySelection.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { techChanged(); }
		});
		technologySelection.setSelectedItem(Technology.getCurrent().getTechName());

		GDSDocumentListener myDocumentListener = new GDSDocumentListener(this);
		gdsLayerNumber.getDocument().addDocumentListener(myDocumentListener);
		gdsLayerType.getDocument().addDocumentListener(myDocumentListener);
		gdsPinLayer.getDocument().addDocumentListener(myDocumentListener);
		gdsPinType.getDocument().addDocumentListener(myDocumentListener);
		gdsTextLayer.getDocument().addDocumentListener(myDocumentListener);
		gdsTextType.getDocument().addDocumentListener(myDocumentListener);
	}

	private void techChanged()
	{
		String techName = (String)technologySelection.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return;
		gdsLayersModel.clear();
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
			String str = (String)layerMap.get(layer);
			gdsLayersModel.addElement(str);
		}
		gdsLayersList.setSelectedIndex(0);
		gdsClickLayer();
	}

	/**
	 * Method called when the user clicks on a layer name in the scrollable list.
	 */
	private void gdsClickLayer()
	{
		changingGDS = true;
		String str = (String)gdsLayersList.getSelectedValue();
		GDSLayers numbers = gdsGetNumbers(str);
		if (numbers == null) return;
		if (numbers.getNumLayers() == 0)
		{
			gdsLayerNumber.setText("");
			gdsLayerType.setText("");
		} else
		{
			Integer gdsValue = (Integer)numbers.getFirstLayer();
			int layerNum = gdsValue.intValue() & 0xFFFF;
			int layerType = (gdsValue.intValue() >> 16) & 0xFFFF;
			gdsLayerNumber.setText(Integer.toString(layerNum));
			gdsLayerType.setText(Integer.toString(layerType));
		}
		if (numbers.getPinLayer() == -1)
		{
			gdsPinLayer.setText("");
			gdsPinType.setText("");
		} else
		{
			gdsPinLayer.setText(Integer.toString(numbers.getPinLayer() & 0xFFFF));
			gdsPinType.setText(Integer.toString((numbers.getPinLayer() >> 16) & 0xFFFF));
		}
		if (numbers.getTextLayer() == -1)
		{
			gdsTextLayer.setText("");
			gdsTextType.setText("");
		} else
		{
			gdsTextLayer.setText(Integer.toString(numbers.getTextLayer() & 0xFFFF));
			gdsTextType.setText(Integer.toString((numbers.getTextLayer() >> 16) & 0xFFFF));
		}
		changingGDS = false;
	}

	/**
	 * Method to parse the line in the scrollable list and return the GDS layer numbers part
	 * (in parentheses).
	 */
	private GDSLayers gdsGetNumbers(String str)
	{
		int openParen = str.indexOf('(');
		if (openParen < 0) return null;
		int closeParen = str.lastIndexOf(')');
		if (closeParen < 0) return null;
		String gdsNumbers = str.substring(openParen+1, closeParen);
		GDSLayers numbers = GDSLayers.parseLayerString(gdsNumbers);
		return numbers;
	}

	/**
	 * Method to parse the line in the scrollable list and return the Layer.
	 */
	private Layer gdsGetLayer(String str)
	{
		int openParen = str.indexOf('(');
		if (openParen < 0) return null;

		String techName = (String)technologySelection.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return null;

		String layerName = str.substring(0, openParen-1);
		Layer layer = tech.findLayer(layerName);
		return layer;
	}

	/**
	 * Method called when the user types a new layer number into one of the 3 edit fields.
	 */
	private void gdsNumbersChanged()
	{
		if (changingGDS) return;
		String str = (String)gdsLayersList.getSelectedValue();
		Layer layer = gdsGetLayer(str);
		if (layer == null) return;

		// the layer information
		String newLine = gdsLayerNumber.getText().trim();
		int layerType = TextUtils.atoi(gdsLayerType.getText().trim());
		if (layerType != 0) newLine += "/" + layerType;

		// the pin information
		String pinLayer = gdsPinLayer.getText().trim();
		int pinType = TextUtils.atoi(gdsPinType.getText().trim());
		if (pinLayer.length() > 0 || pinType != 0)
		{
			newLine += "," + pinLayer;
			if (pinType != 0) newLine += "/" + pinType;
			newLine += "p";
		}

		// the text information
		String textLayer = gdsTextLayer.getText().trim();
		int textType = TextUtils.atoi(gdsTextType.getText().trim());
		if (textLayer.length() > 0 || textType != 0)
		{
			newLine += "," + textLayer;
			if (textType != 0) newLine += "/" + textType;
			newLine += "t";
		}
		String wholeLine = layer.getName() + " (" + newLine + ")";
		int index = gdsLayersList.getSelectedIndex();
		gdsLayersModel.set(index, wholeLine);
		layerMap.put(layer, wholeLine);
	}

	/**
	 * Class to handle special changes to changes to a GDS layer.
	 */
	private static class GDSDocumentListener implements DocumentListener
	{
		GDSTab dialog;

		GDSDocumentListener(GDSTab dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.gdsNumbersChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.gdsNumbersChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.gdsNumbersChanged(); }
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the GDS tab.
	 */
	public void term()
	{
		for(Iterator<Technology> tIt = Technology.getTechnologies(); tIt.hasNext(); )
		{
			Technology tech = tIt.next();
			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = lIt.next();
				String str = (String)layerMap.get(layer);
				GDSLayers numbers = gdsGetNumbers(str);
				if (numbers == null) continue;

				GDSLayers oldNumbers = GDSLayers.parseLayerString(layer.getGDSLayer());
				if (!oldNumbers.equals(numbers))
				{
					String currentGDSNumbers = "";
					for(Iterator<Integer> it = numbers.getLayers(); it.hasNext(); )
					{
						Integer layVal = it.next();
						int layNum = layVal.intValue() & 0xFFFF;
						int layType = (layVal.intValue() >> 16) & 0xFFFF;
						currentGDSNumbers += Integer.toString(layNum);
						if (layType != 0) currentGDSNumbers += "/" + layType;
					}
					if (numbers.getPinLayer() != -1)
					{
						currentGDSNumbers += "," + (numbers.getPinLayer() & 0xFFFF);
						int pinType = (numbers.getPinLayer() >> 16) & 0xFFFF;
						if (pinType != 0) currentGDSNumbers += "/" + pinType;
						currentGDSNumbers += "p";
					}
					if (numbers.getTextLayer() != -1)
					{
						currentGDSNumbers += "," + (numbers.getTextLayer() & 0xFFFF);
						int textType = (numbers.getTextLayer() >> 16) & 0xFFFF;
						if (textType != 0) currentGDSNumbers += "/" + textType;
						currentGDSNumbers += "t";
					}
					layer.setGDSLayer(currentGDSNumbers);
				}
			}
		}
		boolean currentValue = gdsOutputMergesBoxes.isSelected();
		if (currentValue != IOTool.isGDSOutMergesBoxes())
			IOTool.setGDSOutMergesBoxes(currentValue);
		currentValue = gdsOutputWritesExportPins.isSelected();
		if (currentValue != IOTool.isGDSOutWritesExportPins())
			IOTool.setGDSOutWritesExportPins(currentValue);
		currentValue = gdsOutputUpperCase.isSelected();
		if (currentValue != IOTool.isGDSOutUpperCase())
			IOTool.setGDSOutUpperCase(currentValue);
        currentValue = gdsOutputConvertsBracketsInExports.isSelected();
        if (currentValue !=  IOTool.getGDSOutputConvertsBracketsInExports())
            IOTool.setGDSOutputConvertsBracketsInExports(currentValue);
        currentValue = gdsConvertNCCExportsConnectedByParentPins.isSelected();
        if (currentValue != IOTool.getGDSConvertNCCExportsConnectedByParentPins())
            IOTool.setGDSConvertNCCExportsConnectedByParentPins(currentValue);

		int currentTextLayer = TextUtils.atoi(gdsDefaultTextLayer.getText());
		if (currentTextLayer != IOTool.getGDSOutDefaultTextLayer())
			IOTool.setGDSOutDefaultTextLayer(currentTextLayer);
        int currentCellNameLen = TextUtils.atoi(gdsCellNameLenMax.getText());
        if (currentCellNameLen != IOTool.getGDSCellNameLenMax())
            IOTool.setGDSCellNameLenMax(currentCellNameLen);

		currentValue = gdsInputMergesBoxes.isSelected();
		if (currentValue != IOTool.isGDSInMergesBoxes())
			IOTool.setGDSInMergesBoxes(currentValue);
		currentValue = gdsInputIncludesText.isSelected();
		if (currentValue != IOTool.isGDSInIncludesText())
			IOTool.setGDSInIncludesText(currentValue);
		currentValue = gdsInputExpandsCells.isSelected();
		if (currentValue != IOTool.isGDSInExpandsCells())
			IOTool.setGDSInExpandsCells(currentValue);
		currentValue = gdsInputInstantiatesArrays.isSelected();
		if (currentValue != IOTool.isGDSInInstantiatesArrays())
			IOTool.setGDSInInstantiatesArrays(currentValue);
		currentValue = gdsInputIgnoresUnknownLayers.isSelected();
		if (currentValue != IOTool.isGDSInIgnoresUnknownLayers())
			IOTool.setGDSInIgnoresUnknownLayers(currentValue);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        java.awt.GridBagConstraints gridBagConstraints;

        gds = new javax.swing.JPanel();
        gdsLayerList = new javax.swing.JScrollPane();
        jLabel6 = new javax.swing.JLabel();
        gdsLayerNumber = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        gdsPinLayer = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        gdsTextLayer = new javax.swing.JTextField();
        gdsInputIncludesText = new javax.swing.JCheckBox();
        gdsInputExpandsCells = new javax.swing.JCheckBox();
        gdsInputInstantiatesArrays = new javax.swing.JCheckBox();
        gdsInputIgnoresUnknownLayers = new javax.swing.JCheckBox();
        gdsOutputMergesBoxes = new javax.swing.JCheckBox();
        gdsOutputWritesExportPins = new javax.swing.JCheckBox();
        gdsOutputUpperCase = new javax.swing.JCheckBox();
        jLabel9 = new javax.swing.JLabel();
        gdsDefaultTextLayer = new javax.swing.JTextField();
        jLabel29 = new javax.swing.JLabel();
        gdsOutputConvertsBracketsInExports = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        gdsLayerType = new javax.swing.JTextField();
        gdsPinType = new javax.swing.JTextField();
        gdsTextType = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        gdsCellNameLenMax = new javax.swing.JTextField();
        gdsConvertNCCExportsConnectedByParentPins = new javax.swing.JCheckBox();
        technologySelection = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        gdsInputMergesBoxes = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("IO Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        gds.setLayout(new java.awt.GridBagLayout());

        gdsLayerList.setMinimumSize(new java.awt.Dimension(200, 200));
        gdsLayerList.setOpaque(false);
        gdsLayerList.setPreferredSize(new java.awt.Dimension(200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 16;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsLayerList, gridBagConstraints);

        jLabel6.setText("Normal:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(jLabel6, gridBagConstraints);

        gdsLayerNumber.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsLayerNumber, gridBagConstraints);

        jLabel7.setText("Pin:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(jLabel7, gridBagConstraints);

        gdsPinLayer.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsPinLayer, gridBagConstraints);

        jLabel8.setText("Text:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(jLabel8, gridBagConstraints);

        gdsTextLayer.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsTextLayer, gridBagConstraints);

        gdsInputIncludesText.setText("Input includes Text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsInputIncludesText, gridBagConstraints);

        gdsInputExpandsCells.setText("Input expands cells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsInputExpandsCells, gridBagConstraints);

        gdsInputInstantiatesArrays.setText("Input instantiates Arrays");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsInputInstantiatesArrays, gridBagConstraints);

        gdsInputIgnoresUnknownLayers.setText("Input ignores unknown layers");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 6, 4);
        gds.add(gdsInputIgnoresUnknownLayers, gridBagConstraints);

        gdsOutputMergesBoxes.setText("Output merges Boxes (slow)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 4, 2, 4);
        gds.add(gdsOutputMergesBoxes, gridBagConstraints);

        gdsOutputWritesExportPins.setText("Output writes export Pins");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsOutputWritesExportPins, gridBagConstraints);

        gdsOutputUpperCase.setText("Output all upper-case");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsOutputUpperCase, gridBagConstraints);

        jLabel9.setText("Output default text layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gds.add(jLabel9, gridBagConstraints);

        gdsDefaultTextLayer.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsDefaultTextLayer, gridBagConstraints);

        jLabel29.setText("Negative layer values generate no GDS");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 3;
        gds.add(jLabel29, gridBagConstraints);

        gdsOutputConvertsBracketsInExports.setText("Output converts brackets in exports");
        gdsOutputConvertsBracketsInExports.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                gdsOutputConvertsBracketsInExportsActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsOutputConvertsBracketsInExports, gridBagConstraints);

        jLabel1.setText("Layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gds.add(jLabel1, gridBagConstraints);

        jLabel2.setText("Type:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gds.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsLayerType, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsPinType, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsTextType, gridBagConstraints);

        jLabel3.setText("Max chars in output cell name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsCellNameLenMax, gridBagConstraints);

        gdsConvertNCCExportsConnectedByParentPins.setText("Use NCC annotations for Exports");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        gds.add(gdsConvertNCCExportsConnectedByParentPins, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(technologySelection, gridBagConstraints);

        jLabel4.setText("Technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(jLabel4, gridBagConstraints);

        gdsInputMergesBoxes.setText("Input merges Boxes (slow)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        gds.add(gdsInputMergesBoxes, gridBagConstraints);

        getContentPane().add(gds, new java.awt.GridBagConstraints());

        pack();
    }
    // </editor-fold>//GEN-END:initComponents

    private void gdsOutputConvertsBracketsInExportsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gdsOutputConvertsBracketsInExportsActionPerformed
        // Add your handling code here:
    }//GEN-LAST:event_gdsOutputConvertsBracketsInExportsActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel gds;
    private javax.swing.JTextField gdsCellNameLenMax;
    private javax.swing.JCheckBox gdsConvertNCCExportsConnectedByParentPins;
    private javax.swing.JTextField gdsDefaultTextLayer;
    private javax.swing.JCheckBox gdsInputExpandsCells;
    private javax.swing.JCheckBox gdsInputIgnoresUnknownLayers;
    private javax.swing.JCheckBox gdsInputIncludesText;
    private javax.swing.JCheckBox gdsInputInstantiatesArrays;
    private javax.swing.JCheckBox gdsInputMergesBoxes;
    private javax.swing.JScrollPane gdsLayerList;
    private javax.swing.JTextField gdsLayerNumber;
    private javax.swing.JTextField gdsLayerType;
    private javax.swing.JCheckBox gdsOutputConvertsBracketsInExports;
    private javax.swing.JCheckBox gdsOutputMergesBoxes;
    private javax.swing.JCheckBox gdsOutputUpperCase;
    private javax.swing.JCheckBox gdsOutputWritesExportPins;
    private javax.swing.JTextField gdsPinLayer;
    private javax.swing.JTextField gdsPinType;
    private javax.swing.JTextField gdsTextLayer;
    private javax.swing.JTextField gdsTextType;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JComboBox technologySelection;
    // End of variables declaration//GEN-END:variables
}
