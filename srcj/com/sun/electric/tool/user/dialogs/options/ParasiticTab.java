/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ParasiticTab.java
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

import com.sun.electric.tool.extract.ParasiticTool;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Class to handle the "Parasitics" tab of the Preferences dialog.
 */
public class ParasiticTab extends PreferencePanel {

	private HashMap<Layer,Pref> layerResistanceOptions;
	private HashMap<Layer,Pref> layerCapacitanceOptions;
	private HashMap<Layer,Pref> layerEdgeCapacitanceOptions;
	private HashMap<Technology,Pref> techMinResistance, techMinCapacitance, techGateLengthShrink;
	private HashMap<Technology,Pref> techIncludeGateInResistance, techIncludeGroundNetwork;
	private JList layerList;
	private DefaultListModel layerListModel;
	private boolean changing;

	/** Creates new form ParasiticTab */
	public ParasiticTab(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return topPanel; }

	/** return the name of this preferences tab. */
	public String getName() { return "Parasitic"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Routing tab.
	 */
	public void init()
	{
		changing = false;
		layerListModel = new DefaultListModel();
		layerList = new JList(layerListModel);
		layerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		spiceLayer.setViewportView(layerList);
		layerList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { layerListClick(); }
		});

		layerResistanceOptions = new HashMap<Layer,Pref>();
		layerCapacitanceOptions = new HashMap<Layer,Pref>();
		layerEdgeCapacitanceOptions = new HashMap<Layer,Pref>();
		techMinResistance = new HashMap<Technology,Pref>();
		techMinCapacitance = new HashMap<Technology,Pref>();
		techGateLengthShrink = new HashMap<Technology,Pref>();
		techIncludeGateInResistance = new HashMap<Technology,Pref>();
		techIncludeGroundNetwork = new HashMap<Technology,Pref>();
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			techSelection.addItem(tech.getTechName());
			techMinResistance.put(tech, Pref.makeDoublePref(null, null, tech.getMinResistance()));
			techMinCapacitance.put(tech, Pref.makeDoublePref(null, null, tech.getMinCapacitance()));
			techGateLengthShrink.put(tech, Pref.makeDoublePref(null, null, tech.getGateLengthSubtraction()));
			techIncludeGateInResistance.put(tech, Pref.makeBooleanPref(null, null, tech.isGateIncluded()));
			techIncludeGroundNetwork.put(tech, Pref.makeBooleanPref(null, null, tech.isGroundNetIncluded()));

			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = (Layer)lIt.next();
				layerResistanceOptions.put(layer, Pref.makeDoublePref(null, null, layer.getResistance()));
				layerCapacitanceOptions.put(layer, Pref.makeDoublePref(null, null, layer.getCapacitance()));
				layerEdgeCapacitanceOptions.put(layer, Pref.makeDoublePref(null, null, layer.getEdgeCapacitance()));
			}
		}
		techSelection.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { techChanged(); }
		});
		techSelection.setSelectedItem(Technology.getCurrent().getTechName());

		resistance.getDocument().addDocumentListener(new ParasiticLayerDocumentListener(layerResistanceOptions, this));
		capacitance.getDocument().addDocumentListener(new ParasiticLayerDocumentListener(layerCapacitanceOptions, this));
		edgeCapacitance.getDocument().addDocumentListener(new ParasiticLayerDocumentListener(layerEdgeCapacitanceOptions, this));

		minResistance.getDocument().addDocumentListener(new ParasiticTechDocumentListener(this));
		minCapacitance.getDocument().addDocumentListener(new ParasiticTechDocumentListener(this));
		gateLengthSubtraction.getDocument().addDocumentListener(new ParasiticTechDocumentListener(this));

		includeGate.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { updateTechnologyGlobals(); }
		});
		includeGround.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { updateTechnologyGlobals(); }
		});

		maxSeriesResistance.setText(TextUtils.formatDouble(Simulation.getSpiceMaxSeriesResistance()));
		verboseNaming.setSelected(Simulation.isParasiticsUseVerboseNaming());
		backannotateLayout.setSelected(Simulation.isParasiticsBackAnnotateLayout());
		extractPowerGround.setSelected(Simulation.isParasiticsExtractPowerGround());
		extractPowerGround.setEnabled(false);
        useExemptedNetsFile.setSelected(Simulation.isParasiticsUseExemptedNetsFile());
        ignoreExemptedNets.setEnabled(useExemptedNetsFile.isSelected());
        extractExemptedNets.setEnabled(useExemptedNetsFile.isSelected());
        ignoreExemptedNets.setSelected(Simulation.isParasiticsIgnoreExemptedNets());
        extractExemptedNets.setSelected(!Simulation.isParasiticsIgnoreExemptedNets());

		// the parasitics panel (not visible)
		maxDistValue.setText(TextUtils.formatDouble(ParasiticTool.getMaxDistance()));
		parasiticPanel.setVisible(false);
	}

	private void techChanged()
	{
		String techName = (String)techSelection.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return;

		changing = true;
		Pref pref = (Pref)techMinResistance.get(tech);
		minResistance.setText(TextUtils.formatDouble(pref.getDouble()));
		pref = (Pref)techMinCapacitance.get(tech);
		minCapacitance.setText(TextUtils.formatDouble(pref.getDouble()));
		pref = (Pref)techGateLengthShrink.get(tech);
		gateLengthSubtraction.setText(TextUtils.formatDouble(pref.getDouble()));

		pref = (Pref)techIncludeGateInResistance.get(tech);
		includeGate.setSelected(pref.getBoolean());
		pref = (Pref)techIncludeGroundNetwork.get(tech);
		includeGround.setSelected(pref.getBoolean());

		layerListModel.clear();
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			layerListModel.addElement(layer.getName());
		}
		layerList.setSelectedIndex(0);
		layerListClick();
		changing = false;
	}

	private void layerListClick()
	{
		String techName = (String)techSelection.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return;

		changing = true;
		String layerName = (String)layerList.getSelectedValue();
		Layer layer = tech.findLayer(layerName);
		if (layer != null)
		{
			Pref resistancePref = (Pref)layerResistanceOptions.get(layer);
			resistance.setText(TextUtils.formatDouble(resistancePref.getDouble()));
			Pref capacitancePref = (Pref)layerCapacitanceOptions.get(layer);
			capacitance.setText(TextUtils.formatDouble(capacitancePref.getDouble()));
			Pref edgeCapacitancePref = (Pref)layerEdgeCapacitanceOptions.get(layer);
			edgeCapacitance.setText(TextUtils.formatDouble(edgeCapacitancePref.getDouble()));
		}
		changing = false;
	}

	private void updateTechnologyGlobals()
	{
		if (changing) return;
		String techName = (String)techSelection.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return;

		Pref pref = (Pref)techMinResistance.get(tech);
		pref.setDouble(TextUtils.atof(minResistance.getText()));
		pref = (Pref)techMinCapacitance.get(tech);
		pref.setDouble(TextUtils.atof(minCapacitance.getText()));
		pref = (Pref)techGateLengthShrink.get(tech);
		pref.setDouble(TextUtils.atof(gateLengthSubtraction.getText()));

		pref = (Pref)techIncludeGateInResistance.get(tech);
		pref.setBoolean(includeGate.isSelected());
		pref = (Pref)techIncludeGroundNetwork.get(tech);
		pref.setBoolean(includeGround.isSelected());
	}

	/**
	 * Class to handle special changes to per-layer parasitics.
	 */
	private static class ParasiticLayerDocumentListener implements DocumentListener
	{
		private ParasiticTab dialog;
		private HashMap<Layer,Pref> map;

		ParasiticLayerDocumentListener(HashMap<Layer,Pref> map, ParasiticTab dialog)
		{
			this.dialog = dialog;
			this.map = map;
		}

		private void change(DocumentEvent e)
		{
			if (dialog.changing) return;
			// get the currently selected layer
			String techName = (String)dialog.techSelection.getSelectedItem();
			Technology tech = Technology.findTechnology(techName);
			if (tech == null) return;

			String layerName = (String)dialog.layerList.getSelectedValue();
			Layer layer = tech.findLayer(layerName);
			if (layer == null) return;

			// get the typed value
			Document doc = e.getDocument();
			int len = doc.getLength();
			String text;
			try
			{
				text = doc.getText(0, len);
			} catch (BadLocationException ex) { return; }
			Pref pref = (Pref)map.get(layer);
			double v = TextUtils.atof(text);

			// update the option
			pref.setDouble(v);
		}

		public void changedUpdate(DocumentEvent e) { change(e); }
		public void insertUpdate(DocumentEvent e) { change(e); }
		public void removeUpdate(DocumentEvent e) { change(e); }
	}

	/**
	 * Class to handle special changes to per-layer parasitics.
	 */
	private static class ParasiticTechDocumentListener implements DocumentListener
	{
		private ParasiticTab dialog;

		ParasiticTechDocumentListener(ParasiticTab dialog)
		{
			this.dialog = dialog;
		}

		public void changedUpdate(DocumentEvent e) { dialog.updateTechnologyGlobals(); }
		public void insertUpdate(DocumentEvent e) { dialog.updateTechnologyGlobals(); }
		public void removeUpdate(DocumentEvent e) { dialog.updateTechnologyGlobals(); }
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Routing tab.
	 */
	public void term()
	{
		ParasiticTool.setMaxDistance(Double.parseDouble(maxDistValue.getText()));

		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();

			Pref pref = (Pref)techMinResistance.get(tech);
			if (pref != null && pref.getDoubleFactoryValue() != pref.getDouble())
				tech.setMinResistance(pref.getDouble());
			pref = (Pref)techMinCapacitance.get(tech);
			if (pref != null && pref.getDoubleFactoryValue() != pref.getDouble())
				tech.setMinCapacitance(pref.getDouble());
			pref = (Pref)techGateLengthShrink.get(tech);
			if (pref != null && pref.getDoubleFactoryValue() != pref.getDouble())
				tech.setGateLengthSubtraction(pref.getDouble());

			pref = (Pref)techIncludeGateInResistance.get(tech);
			if (pref != null && pref.getBooleanFactoryValue() != pref.getBoolean())
				tech.setGateIncluded(pref.getBoolean());
			pref = (Pref)techIncludeGroundNetwork.get(tech);
			if (pref != null && pref.getBooleanFactoryValue() != pref.getBoolean())
				tech.setGroundNetIncluded(pref.getBoolean());
			
			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = (Layer)lIt.next();
				Pref resistancePref = (Pref)layerResistanceOptions.get(layer);
				if (resistancePref != null && resistancePref.getDoubleFactoryValue() != resistancePref.getDouble())
					layer.setResistance(resistancePref.getDouble());
				Pref capacitancePref = (Pref)layerCapacitanceOptions.get(layer);
				if (capacitancePref != null && capacitancePref.getDoubleFactoryValue() != capacitancePref.getDouble())
					layer.setCapacitance(capacitancePref.getDouble());
				Pref edgeCapacitancePref = (Pref)layerEdgeCapacitanceOptions.get(layer);
				if (edgeCapacitancePref != null && edgeCapacitancePref.getDoubleFactoryValue() != edgeCapacitancePref.getDouble())
					layer.setEdgeCapacitance(edgeCapacitancePref.getDouble());
			}
		}

		double doubleNow = TextUtils.atof(maxSeriesResistance.getText());
		if (Simulation.getSpiceMaxSeriesResistance() != doubleNow) Simulation.setSpiceMaxSeriesResistance(doubleNow);

		boolean b = verboseNaming.isSelected();
		if (b != Simulation.isParasiticsUseVerboseNaming()) Simulation.setParasiticsUseVerboseNaming(b);
		b = backannotateLayout.isSelected();
		if (b != Simulation.isParasiticsBackAnnotateLayout()) Simulation.setParasiticsBackAnnotateLayout(b);
		b = extractPowerGround.isSelected();
		if (b != Simulation.isParasiticsExtractPowerGround()) Simulation.setParasiticsExtractPowerGround(b);
        b = useExemptedNetsFile.isSelected();
        if (b != Simulation.isParasiticsUseExemptedNetsFile()) Simulation.setParasiticsUseExemptedNetsFile(b);
        b = ignoreExemptedNets.isSelected();
        if (b != Simulation.isParasiticsIgnoreExemptedNets()) Simulation.setParasiticsIgnoreExemptedNets(b);
	}

	private void factoryResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_factoryResetActionPerformed
		int ret = JOptionPane.showConfirmDialog(this,
			"Are you sure you want to reset all layers to their default resistance and capacitance values?",
			"Factory Reset", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (ret == JOptionPane.YES_OPTION) {
			for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
			{
				Technology tech = (Technology)it.next();
				for (Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
				{
					Layer layer = (Layer)lIt.next();
					layer.resetToFactoryParasitics();
				}
			}
			init();
		}
	}//GEN-LAST:event_factoryResetActionPerformed

	/** This method is called from within the constructor to
	 * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        exemptedNetsGroup = new javax.swing.ButtonGroup();
        topPanel = new javax.swing.JPanel();
        techValues = new javax.swing.JPanel();
        spiceLayer = new javax.swing.JScrollPane();
        jLabel7 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        resistance = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        capacitance = new javax.swing.JTextField();
        edgeCapacitance = new javax.swing.JTextField();
        factoryReset = new javax.swing.JButton();
        parasiticPanel = new javax.swing.JPanel();
        maxDist = new javax.swing.JLabel();
        maxDistValue = new javax.swing.JTextField();
        globalValues = new javax.swing.JPanel();
        jLabel20 = new javax.swing.JLabel();
        minResistance = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        minCapacitance = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        gateLengthSubtraction = new javax.swing.JTextField();
        includeGate = new javax.swing.JCheckBox();
        includeGround = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        maxSeriesResistance = new javax.swing.JTextField();
        simpleParasiticOptions = new javax.swing.JPanel();
        verboseNaming = new javax.swing.JCheckBox();
        backannotateLayout = new javax.swing.JCheckBox();
        extractPowerGround = new javax.swing.JCheckBox();
        useExemptedNetsFile = new javax.swing.JCheckBox();
        ignoreExemptedNets = new javax.swing.JRadioButton();
        extractExemptedNets = new javax.swing.JRadioButton();
        jLabel3 = new javax.swing.JLabel();
        techSelection = new javax.swing.JComboBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        topPanel.setLayout(new java.awt.GridBagLayout());

        techValues.setLayout(new java.awt.GridBagLayout());

        techValues.setBorder(new javax.swing.border.TitledBorder("Layer Values"));
        spiceLayer.setMinimumSize(new java.awt.Dimension(200, 50));
        spiceLayer.setPreferredSize(new java.awt.Dimension(200, 50));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        techValues.add(spiceLayer, gridBagConstraints);

        jLabel7.setText("Layer:");
        jLabel7.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        techValues.add(jLabel7, gridBagConstraints);

        jLabel11.setText("Resistance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        techValues.add(jLabel11, gridBagConstraints);

        jLabel2.setText("Perimeter Cap (fF/um):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        techValues.add(jLabel2, gridBagConstraints);

        resistance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        techValues.add(resistance, gridBagConstraints);

        jLabel12.setText("Area Cap (fF/um^2):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        techValues.add(jLabel12, gridBagConstraints);

        capacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        techValues.add(capacitance, gridBagConstraints);

        edgeCapacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        techValues.add(edgeCapacitance, gridBagConstraints);

        factoryReset.setText("Factory Reset");
        factoryReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                factoryResetActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        techValues.add(factoryReset, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        topPanel.add(techValues, gridBagConstraints);

        parasiticPanel.setLayout(new java.awt.GridBagLayout());

        parasiticPanel.setBorder(new javax.swing.border.TitledBorder("Parasitic Coupling Options"));
        parasiticPanel.setEnabled(false);
        maxDist.setText("Maximum distance (lambda)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        parasiticPanel.add(maxDist, gridBagConstraints);

        maxDistValue.setColumns(6);
        maxDistValue.setText("20");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        parasiticPanel.add(maxDistValue, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        topPanel.add(parasiticPanel, gridBagConstraints);

        globalValues.setLayout(new java.awt.GridBagLayout());

        globalValues.setBorder(new javax.swing.border.TitledBorder("Global Values"));
        jLabel20.setText("Min. Resistance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        globalValues.add(jLabel20, gridBagConstraints);

        minResistance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        globalValues.add(minResistance, gridBagConstraints);

        jLabel21.setText("Min. Capacitance (fF):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        globalValues.add(jLabel21, gridBagConstraints);

        minCapacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        globalValues.add(minCapacitance, gridBagConstraints);

        jLabel5.setText("Gate Length Shrink (Subtraction) um:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        globalValues.add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        globalValues.add(gateLengthSubtraction, gridBagConstraints);

        includeGate.setText("Include Gate In Resistance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        globalValues.add(includeGate, gridBagConstraints);

        includeGround.setText("Include Ground Network");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        globalValues.add(includeGround, gridBagConstraints);

        jLabel1.setText("Max. Series Resistance: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        globalValues.add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        globalValues.add(maxSeriesResistance, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        topPanel.add(globalValues, gridBagConstraints);

        simpleParasiticOptions.setLayout(new java.awt.GridBagLayout());

        simpleParasiticOptions.setBorder(new javax.swing.border.TitledBorder("Simple Parasitic Options"));
        verboseNaming.setText("Use Verbose Naming");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(verboseNaming, gridBagConstraints);

        backannotateLayout.setText("Back-Annotate Layout");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(backannotateLayout, gridBagConstraints);

        extractPowerGround.setText("Extract Power/Ground");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(extractPowerGround, gridBagConstraints);

        useExemptedNetsFile.setText("Use exemptedNets.txt file");
        useExemptedNetsFile.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                useExemptedNetsFileStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(useExemptedNetsFile, gridBagConstraints);

        ignoreExemptedNets.setText("Extract everything except exempted nets");
        exemptedNetsGroup.add(ignoreExemptedNets);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(ignoreExemptedNets, gridBagConstraints);

        extractExemptedNets.setText("Extract only exempted nets");
        exemptedNetsGroup.add(extractExemptedNets);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(extractExemptedNets, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        topPanel.add(simpleParasiticOptions, gridBagConstraints);

        jLabel3.setText("Technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        topPanel.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        topPanel.add(techSelection, gridBagConstraints);

        getContentPane().add(topPanel, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

    private void useExemptedNetsFileStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_useExemptedNetsFileStateChanged
        ignoreExemptedNets.setEnabled(useExemptedNetsFile.isSelected());
        extractExemptedNets.setEnabled(useExemptedNetsFile.isSelected());
    }//GEN-LAST:event_useExemptedNetsFileStateChanged

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        new ParasiticTab(new javax.swing.JFrame(), true).setVisible(true);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox backannotateLayout;
    private javax.swing.JTextField capacitance;
    private javax.swing.JTextField edgeCapacitance;
    private javax.swing.ButtonGroup exemptedNetsGroup;
    private javax.swing.JRadioButton extractExemptedNets;
    private javax.swing.JCheckBox extractPowerGround;
    private javax.swing.JButton factoryReset;
    private javax.swing.JTextField gateLengthSubtraction;
    private javax.swing.JPanel globalValues;
    private javax.swing.JRadioButton ignoreExemptedNets;
    private javax.swing.JCheckBox includeGate;
    private javax.swing.JCheckBox includeGround;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel maxDist;
    private javax.swing.JTextField maxDistValue;
    private javax.swing.JTextField maxSeriesResistance;
    private javax.swing.JTextField minCapacitance;
    private javax.swing.JTextField minResistance;
    private javax.swing.JPanel parasiticPanel;
    private javax.swing.JTextField resistance;
    private javax.swing.JPanel simpleParasiticOptions;
    private javax.swing.JScrollPane spiceLayer;
    private javax.swing.JComboBox techSelection;
    private javax.swing.JPanel techValues;
    private javax.swing.JPanel topPanel;
    private javax.swing.JCheckBox useExemptedNetsFile;
    private javax.swing.JCheckBox verboseNaming;
    // End of variables declaration//GEN-END:variables
    
}
