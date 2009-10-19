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

import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Foundry;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.io.GDSLayers;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
	private JList gdsLayersList;
	private DefaultListModel gdsLayersModel;
	private boolean changingGDS = false;
    private Setting gdsOutMergesBoxesSetting = IOTool.getGDSOutMergesBoxesSetting();
    private Setting gdsOutWritesExportPinsSetting = IOTool.getGDSOutWritesExportPinsSetting();
    private Setting gdsOutUpperCaseSetting = IOTool.getGDSOutUpperCaseSetting();
    private Setting gdsOutDefaultTextLayerSetting = IOTool.getGDSOutDefaultTextLayerSetting();
    private Setting gdsOutputConvertsBracketsInExportsSetting = IOTool.getGDSOutputConvertsBracketsInExportsSetting();
    private Setting gdsCellNameLenMaxSetting = IOTool.getGDSCellNameLenMaxSetting();
    private Setting gdsInputScaleSetting = IOTool.getGDSInputScaleSetting();
    private Setting gdsOutputScaleSetting = IOTool.getGDSOutputScaleSetting();

    /** Creates new form GDSTab */
	public GDSTab(PreferencesFrame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// make all text fields select-all when entered
	    EDialog.makeTextFieldSelectAllOnTab(gdsLayerNumber);
	    EDialog.makeTextFieldSelectAllOnTab(gdsLayerType);
	    EDialog.makeTextFieldSelectAllOnTab(gdsPinLayer);
	    EDialog.makeTextFieldSelectAllOnTab(gdsPinType);
	    EDialog.makeTextFieldSelectAllOnTab(gdsTextLayer);
	    EDialog.makeTextFieldSelectAllOnTab(gdsTextType);
	    EDialog.makeTextFieldSelectAllOnTab(gdsCellNameLenMax);
	    EDialog.makeTextFieldSelectAllOnTab(gdsDefaultTextLayer);
	    EDialog.makeTextFieldSelectAllOnTab(gdsInputScale);
	}

	/** return the JPanel to use for the user preferences. */
	public JPanel getUserPreferencesPanel() { return preferences; }

	/** return the JPanel to use for the project preferences. */
	public JPanel getProjectPreferencesPanel() { return projectSettings; }

	/** return the name of this preferences tab. */
	public String getName() { return "GDS"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the GDS tab.
	 */
	public void init()
	{
		// user preferences
        gdsConvertNCCExportsConnectedByParentPins.setSelected(IOTool.getGDSConvertNCCExportsConnectedByParentPins());
        gdsInputMergesBoxes.setSelected(IOTool.isGDSInMergesBoxes());
		gdsInputIncludesText.setSelected(IOTool.isGDSInIncludesText());
		gdsInputExpandsCells.setSelected(IOTool.isGDSInExpandsCells());
		gdsInputInstantiatesArrays.setSelected(IOTool.isGDSInInstantiatesArrays());
        gdsUnknownLayers.addItem("Ignore");
        gdsUnknownLayers.addItem("Convert to DRC Exclusion layer");
        gdsUnknownLayers.addItem("Convert to random layer");
        gdsUnknownLayers.setSelectedIndex(IOTool.getGDSInUnknownLayerHandling());
        gdsSimplifyCells.setSelected(IOTool.isGDSInSimplifyCells());
        gdsColapseNames.setSelected(IOTool.isGDSColapseVddGndPinNames());
        gdsArraySimplification.addItem("None");
        gdsArraySimplification.addItem("Merge individual arrays");
        gdsArraySimplification.addItem("Merge all arrays");
        gdsArraySimplification.setSelectedIndex(IOTool.getGDSArraySimplification());
        gdsCadenceCompatibility.setSelected(IOTool.isGDSCadenceCompatibility());

        // project preferences
		gdsOutputMergesBoxes.setSelected(getBoolean(gdsOutMergesBoxesSetting));
		gdsOutputWritesExportPins.setSelected(getBoolean(gdsOutWritesExportPinsSetting));
		gdsOutputUpperCase.setSelected(getBoolean(gdsOutUpperCaseSetting));
		gdsDefaultTextLayer.setText(Integer.toString(getInt(gdsOutDefaultTextLayerSetting)));
        gdsOutputConvertsBracketsInExports.setSelected(getBoolean(gdsOutputConvertsBracketsInExportsSetting));
        gdsCellNameLenMax.setText(Integer.toString(getInt(gdsCellNameLenMaxSetting)));
        gdsInputScale.setText(TextUtils.formatDouble(getDouble(gdsInputScaleSetting), 0));
        gdsOutputScale.setText(TextUtils.formatDouble(getDouble(gdsOutputScaleSetting), 0));

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
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			technologySelection.addItem(new TechGDSTab(tech));
		}
		technologySelection.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { techChanged(); }
		});
		technologySelection.setSelectedItem(Technology.getCurrent());

        foundrySelection.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { foundryChanged(); }
		});


        // to set foundry the first time
        techChanged();

		GDSDocumentListener myDocumentListener = new GDSDocumentListener(this);
		gdsLayerNumber.getDocument().addDocumentListener(myDocumentListener);
		gdsLayerType.getDocument().addDocumentListener(myDocumentListener);
		gdsPinLayer.getDocument().addDocumentListener(myDocumentListener);
		gdsPinType.getDocument().addDocumentListener(myDocumentListener);
		gdsTextLayer.getDocument().addDocumentListener(myDocumentListener);
		gdsTextType.getDocument().addDocumentListener(myDocumentListener);
    }

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the GDS tab.
	 */
	public void term()
	{
		// user preferences
		boolean currentValue = gdsConvertNCCExportsConnectedByParentPins.isSelected();
        if (currentValue != IOTool.getGDSConvertNCCExportsConnectedByParentPins())
            IOTool.setGDSConvertNCCExportsConnectedByParentPins(currentValue);

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
		int currentI = gdsUnknownLayers.getSelectedIndex();
		if (currentI != IOTool.getGDSInUnknownLayerHandling())
			IOTool.setGDSInUnknownLayerHandling(currentI);
        currentValue = gdsSimplifyCells.isSelected();
        if (currentValue != IOTool.isGDSInSimplifyCells())
			IOTool.setGDSInSimplifyCells(currentValue);
        currentValue = gdsColapseNames.isSelected();
        if (currentValue != IOTool.isGDSColapseVddGndPinNames())
			IOTool.setGDSColapseVddGndPinNames(currentValue);
        currentI = gdsArraySimplification.getSelectedIndex();
        if (currentI != IOTool.getGDSArraySimplification())
        	IOTool.setGDSArraySimplification(currentI);
		currentValue = gdsCadenceCompatibility.isSelected();
		if (currentValue != IOTool.isGDSCadenceCompatibility())
			IOTool.setGDSCadenceCompatibility(currentValue);

        // project preferences
        setBoolean(gdsOutMergesBoxesSetting, gdsOutputMergesBoxes.isSelected());
        setBoolean(gdsOutWritesExportPinsSetting, gdsOutputWritesExportPins.isSelected());
        setBoolean(gdsOutUpperCaseSetting, gdsOutputUpperCase.isSelected());
        setInt(gdsOutDefaultTextLayerSetting, TextUtils.atoi(gdsDefaultTextLayer.getText()));
        setBoolean(gdsOutputConvertsBracketsInExportsSetting, gdsOutputConvertsBracketsInExports.isSelected());
        setInt(gdsCellNameLenMaxSetting, TextUtils.atoi(gdsCellNameLenMax.getText()));
        setDouble(gdsInputScaleSetting, TextUtils.atof(gdsInputScale.getText()));
        setDouble(gdsOutputScaleSetting, TextUtils.atof(gdsOutputScale.getText()));
    }

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
		// user preferences
		if (IOTool.getFactoryGDSConvertNCCExportsConnectedByParentPins() != IOTool.getGDSConvertNCCExportsConnectedByParentPins())
			IOTool.setGDSConvertNCCExportsConnectedByParentPins(IOTool.getFactoryGDSConvertNCCExportsConnectedByParentPins());
		if (IOTool.isFactoryGDSInMergesBoxes() != IOTool.isGDSInMergesBoxes())
			IOTool.setGDSInMergesBoxes(IOTool.isFactoryGDSInMergesBoxes());
		if (IOTool.isFactoryGDSInIncludesText() != IOTool.isGDSInIncludesText())
			IOTool.setGDSInIncludesText(IOTool.isFactoryGDSInIncludesText());
		if (IOTool.isFactoryGDSInExpandsCells() != IOTool.isGDSInExpandsCells())
			IOTool.setGDSInExpandsCells(IOTool.isFactoryGDSInExpandsCells());
		if (IOTool.isFactoryGDSInInstantiatesArrays() != IOTool.isGDSInInstantiatesArrays())
			IOTool.setGDSInInstantiatesArrays(IOTool.isFactoryGDSInInstantiatesArrays());
		if (IOTool.getFactoryGDSInUnknownLayerHandling() != IOTool.getGDSInUnknownLayerHandling())
			IOTool.setGDSInUnknownLayerHandling(IOTool.getFactoryGDSInUnknownLayerHandling());
		if (IOTool.isFactoryGDSInSimplifyCells() != IOTool.isGDSInSimplifyCells())
			IOTool.setGDSInSimplifyCells(IOTool.isFactoryGDSInSimplifyCells());
		if (IOTool.isFactoryGDSColapseVddGndPinNames() != IOTool.isGDSColapseVddGndPinNames())
			IOTool.setGDSColapseVddGndPinNames(IOTool.isFactoryGDSColapseVddGndPinNames());
		if (IOTool.getFactoryGDSArraySimplification() != IOTool.getGDSArraySimplification())
			IOTool.setGDSArraySimplification(IOTool.getFactoryGDSArraySimplification());
		if (IOTool.isFactoryGDSCadenceCompatibility() != IOTool.isGDSCadenceCompatibility())
			IOTool.setGDSCadenceCompatibility(IOTool.isFactoryGDSCadenceCompatibility());
	}

    private void foundryChanged()
    {
        Foundry foundry = (Foundry)foundrySelection.getSelectedItem();
        if (foundry == null) return;
        Technology tech = ((TechGDSTab)technologySelection.getSelectedItem()).tech;
		// show the list of layers in the technology
		gdsLayersModel.clear();

        for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
        {
            Layer layer = lIt.next();
            String str = layer.getName();
            String gdsLayer = getString(foundry.getGDSLayerSetting(layer));
            if (gdsLayer != null && gdsLayer.length() > 0) str += " (" + gdsLayer + ")";
			gdsLayersModel.addElement(str);
        }
		gdsLayersList.setSelectedIndex(0);
		gdsClickLayer();
    }

    private void setFoundries(Technology tech)
    {
        foundrySelection.removeAllItems();
        // Foundry
        for (Iterator<Foundry> itF = tech.getFoundries(); itF.hasNext();)
        {
            foundrySelection.addItem(itF.next());
        }
        foundrySelection.setSelectedItem(tech.getSelectedFoundry());
        foundryChanged();
    }

	private void techChanged()
	{
		Technology tech = ((TechGDSTab)technologySelection.getSelectedItem()).tech;
		if (tech == null) return;

		// set the foundries for the technology
        setFoundries(tech);
	}

    /**
	 * Method called when the user clicks on a layer name in the scrollable list.
	 */
	private void gdsClickLayer()
	{
		changingGDS = true;
		String str = (String)gdsLayersList.getSelectedValue();
		GDSLayers numbers = gdsGetNumbers(str);
		if (numbers == null) numbers = GDSLayers.EMPTY;
		if (numbers.getNumLayers() == 0)
		{
			gdsLayerNumber.setText("");
			gdsLayerType.setText("");
		} else
		{
			Integer gdsValue = numbers.getFirstLayer();
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
		String layerName = openParen >= 0 ? str.substring(0, openParen-1) : str;

        Technology tech = ((TechGDSTab)technologySelection.getSelectedItem()).tech;
		if (tech == null) return null;

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
		String wholeLine = layer.getName();
        if (newLine.length() > 0) wholeLine = wholeLine + " (" + newLine + ")";
		int index = gdsLayersList.getSelectedIndex();
		gdsLayersModel.set(index, wholeLine);
        Foundry foundry = (Foundry)foundrySelection.getSelectedItem();
        setString(foundry.getGDSLayerSetting(layer), newLine);
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

	// To have ability to store directly the technology and not
    // to depende on names to search the technology instance
    private static class TechGDSTab
    {
        public Technology tech;

        TechGDSTab(Technology t) { tech = t; }

        // This avoids to call Technology.toString() and get extra text.
        public String toString() { return tech.getTechName(); }
    }

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        preferences = new javax.swing.JPanel();
        inputPanel1 = new javax.swing.JPanel();
        gdsInputMergesBoxes = new javax.swing.JCheckBox();
        gdsInputIncludesText = new javax.swing.JCheckBox();
        gdsInputExpandsCells = new javax.swing.JCheckBox();
        gdsInputInstantiatesArrays = new javax.swing.JCheckBox();
        gdsSimplifyCells = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        gdsArraySimplification = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        gdsUnknownLayers = new javax.swing.JComboBox();
        gdsColapseNames = new javax.swing.JCheckBox();
        gdsConvertNCCExportsConnectedByParentPins = new javax.swing.JCheckBox();
        gdsCadenceCompatibility = new javax.swing.JCheckBox();
        projectSettings = new javax.swing.JPanel();
        gdsLayerList = new javax.swing.JScrollPane();
        jLabel6 = new javax.swing.JLabel();
        gdsLayerNumber = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        gdsPinLayer = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        gdsTextLayer = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        gdsLayerType = new javax.swing.JTextField();
        gdsPinType = new javax.swing.JTextField();
        gdsTextType = new javax.swing.JTextField();
        technologySelection = new javax.swing.JComboBox();
        jLabel10 = new javax.swing.JLabel();
        gdsFoundryName = new javax.swing.JLabel();
        foundrySelection = new javax.swing.JComboBox();
        jLabel13 = new javax.swing.JLabel();
        outputPanel = new javax.swing.JPanel();
        gdsOutputMergesBoxes = new javax.swing.JCheckBox();
        gdsOutputWritesExportPins = new javax.swing.JCheckBox();
        gdsOutputUpperCase = new javax.swing.JCheckBox();
        gdsOutputConvertsBracketsInExports = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        gdsCellNameLenMax = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        gdsDefaultTextLayer = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        gdsOutputScale = new javax.swing.JTextField();
        inputPanel = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        gdsInputScale = new javax.swing.JTextField();

        setTitle("IO Options");
        setName(""); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        preferences.setLayout(new java.awt.GridBagLayout());

        inputPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Import"));
        inputPanel1.setLayout(new java.awt.GridBagLayout());

        gdsInputMergesBoxes.setText("Merge boxes (slow)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        inputPanel1.add(gdsInputMergesBoxes, gridBagConstraints);

        gdsInputIncludesText.setText("Include text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        inputPanel1.add(gdsInputIncludesText, gridBagConstraints);

        gdsInputExpandsCells.setText("Expand cells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        inputPanel1.add(gdsInputExpandsCells, gridBagConstraints);

        gdsInputInstantiatesArrays.setText("Instantiate arrays");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        inputPanel1.add(gdsInputInstantiatesArrays, gridBagConstraints);

        gdsSimplifyCells.setText("Simplify contact vias");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        inputPanel1.add(gdsSimplifyCells, gridBagConstraints);

        jLabel3.setText("Array simplification:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        inputPanel1.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 4);
        inputPanel1.add(gdsArraySimplification, gridBagConstraints);

        jLabel1.setText("Unknown layers:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        inputPanel1.add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 4);
        inputPanel1.add(gdsUnknownLayers, gridBagConstraints);

        gdsColapseNames.setText("Collapse VDD/GND pin names");
        gdsColapseNames.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gdsColapseNamesActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        inputPanel1.add(gdsColapseNames, gridBagConstraints);

        gdsConvertNCCExportsConnectedByParentPins.setText("Use NCC annotations for exports");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        inputPanel1.add(gdsConvertNCCExportsConnectedByParentPins, gridBagConstraints);

        gdsCadenceCompatibility.setText("Cadence compatibility");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        inputPanel1.add(gdsCadenceCompatibility, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        preferences.add(inputPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(preferences, gridBagConstraints);

        projectSettings.setLayout(new java.awt.GridBagLayout());

        gdsLayerList.setMinimumSize(new java.awt.Dimension(200, 200));
        gdsLayerList.setOpaque(false);
        gdsLayerList.setPreferredSize(new java.awt.Dimension(200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        projectSettings.add(gdsLayerList, gridBagConstraints);

        jLabel6.setText("Normal:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        projectSettings.add(jLabel6, gridBagConstraints);

        gdsLayerNumber.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        projectSettings.add(gdsLayerNumber, gridBagConstraints);

        jLabel7.setText("Pin:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        projectSettings.add(jLabel7, gridBagConstraints);

        gdsPinLayer.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        projectSettings.add(gdsPinLayer, gridBagConstraints);

        jLabel8.setText("Text:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        projectSettings.add(jLabel8, gridBagConstraints);

        gdsTextLayer.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        projectSettings.add(gdsTextLayer, gridBagConstraints);

        jLabel2.setText("Layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        projectSettings.add(jLabel2, gridBagConstraints);

        jLabel4.setText("Type:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        projectSettings.add(jLabel4, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        projectSettings.add(gdsLayerType, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        projectSettings.add(gdsPinType, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        projectSettings.add(gdsTextType, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        projectSettings.add(technologySelection, gridBagConstraints);

        jLabel10.setText("Technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        projectSettings.add(jLabel10, gridBagConstraints);

        gdsFoundryName.setText("Foundry:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        projectSettings.add(gdsFoundryName, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        projectSettings.add(foundrySelection, gridBagConstraints);

        jLabel13.setText("Clear these field to ignore the layer");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        projectSettings.add(jLabel13, gridBagConstraints);

        outputPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Export"));
        outputPanel.setLayout(new java.awt.GridBagLayout());

        gdsOutputMergesBoxes.setText("Output merges Boxes (slow)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 4, 2, 4);
        outputPanel.add(gdsOutputMergesBoxes, gridBagConstraints);

        gdsOutputWritesExportPins.setText("Output writes export Pins");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        outputPanel.add(gdsOutputWritesExportPins, gridBagConstraints);

        gdsOutputUpperCase.setText("Output all upper-case");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        outputPanel.add(gdsOutputUpperCase, gridBagConstraints);

        gdsOutputConvertsBracketsInExports.setText("Output converts brackets in exports");
        gdsOutputConvertsBracketsInExports.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gdsOutputConvertsBracketsInExportsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        outputPanel.add(gdsOutputConvertsBracketsInExports, gridBagConstraints);

        jLabel5.setText("Max chars in output cell name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        outputPanel.add(jLabel5, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        outputPanel.add(gdsCellNameLenMax, gridBagConstraints);

        jLabel9.setText("Output default text layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        outputPanel.add(jLabel9, gridBagConstraints);

        gdsDefaultTextLayer.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        outputPanel.add(gdsDefaultTextLayer, gridBagConstraints);

        jLabel12.setText("Scale by:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        outputPanel.add(jLabel12, gridBagConstraints);

        gdsOutputScale.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        outputPanel.add(gdsOutputScale, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        projectSettings.add(outputPanel, gridBagConstraints);

        inputPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Import"));
        inputPanel.setLayout(new java.awt.GridBagLayout());

        jLabel11.setText("Scale by:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        inputPanel.add(jLabel11, gridBagConstraints);

        gdsInputScale.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        inputPanel.add(gdsInputScale, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        projectSettings.add(inputPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(projectSettings, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void gdsOutputConvertsBracketsInExportsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gdsOutputConvertsBracketsInExportsActionPerformed
// Add your handling code here:
    }//GEN-LAST:event_gdsOutputConvertsBracketsInExportsActionPerformed

    private void gdsColapseNamesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gdsColapseNamesActionPerformed
// TODO add your handling code here:
    }//GEN-LAST:event_gdsColapseNamesActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox foundrySelection;
    private javax.swing.JComboBox gdsArraySimplification;
    private javax.swing.JCheckBox gdsCadenceCompatibility;
    private javax.swing.JTextField gdsCellNameLenMax;
    private javax.swing.JCheckBox gdsColapseNames;
    private javax.swing.JCheckBox gdsConvertNCCExportsConnectedByParentPins;
    private javax.swing.JTextField gdsDefaultTextLayer;
    private javax.swing.JLabel gdsFoundryName;
    private javax.swing.JCheckBox gdsInputExpandsCells;
    private javax.swing.JCheckBox gdsInputIncludesText;
    private javax.swing.JCheckBox gdsInputInstantiatesArrays;
    private javax.swing.JCheckBox gdsInputMergesBoxes;
    private javax.swing.JTextField gdsInputScale;
    private javax.swing.JScrollPane gdsLayerList;
    private javax.swing.JTextField gdsLayerNumber;
    private javax.swing.JTextField gdsLayerType;
    private javax.swing.JCheckBox gdsOutputConvertsBracketsInExports;
    private javax.swing.JCheckBox gdsOutputMergesBoxes;
    private javax.swing.JTextField gdsOutputScale;
    private javax.swing.JCheckBox gdsOutputUpperCase;
    private javax.swing.JCheckBox gdsOutputWritesExportPins;
    private javax.swing.JTextField gdsPinLayer;
    private javax.swing.JTextField gdsPinType;
    private javax.swing.JCheckBox gdsSimplifyCells;
    private javax.swing.JTextField gdsTextLayer;
    private javax.swing.JTextField gdsTextType;
    private javax.swing.JComboBox gdsUnknownLayers;
    private javax.swing.JPanel inputPanel;
    private javax.swing.JPanel inputPanel1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel outputPanel;
    private javax.swing.JPanel preferences;
    private javax.swing.JPanel projectSettings;
    private javax.swing.JComboBox technologySelection;
    // End of variables declaration//GEN-END:variables
}
