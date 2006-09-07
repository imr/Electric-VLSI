/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ParasiticTab.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs.projsettings;

import com.sun.electric.database.text.TempPref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.simulation.Simulation;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * Class to handle the "Parasitics" tab of the Project Settings dialog.
 */
public class ParasiticTab extends ProjSettingsPanel {

	private HashMap<Layer,TempPref> layerResistanceOptions;
	private HashMap<Layer,TempPref> layerCapacitanceOptions;
	private HashMap<Layer,TempPref> layerEdgeCapacitanceOptions;
	private HashMap<Technology,TempPref> techMinResistance, techMinCapacitance, techGateLengthShrink;
	private HashMap<Technology,TempPref> techIncludeGateInResistance, techIncludeGroundNetwork;
	private HashMap<Technology,TempPref> techMaxSeriesResistance;
	private JList layerList;
	private DefaultListModel layerListModel;
	private boolean changing;

	/** Creates new form ParasiticTab */
	public ParasiticTab(Frame parent, boolean modal) {
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

		layerResistanceOptions = new HashMap<Layer,TempPref>();
		layerCapacitanceOptions = new HashMap<Layer,TempPref>();
		layerEdgeCapacitanceOptions = new HashMap<Layer,TempPref>();
		techMinResistance = new HashMap<Technology,TempPref>();
		techMinCapacitance = new HashMap<Technology,TempPref>();
		techGateLengthShrink = new HashMap<Technology,TempPref>();
		techIncludeGateInResistance = new HashMap<Technology,TempPref>();
		techIncludeGroundNetwork = new HashMap<Technology,TempPref>();
		techMaxSeriesResistance = new HashMap<Technology,TempPref>();
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			techSelection.addItem(tech.getTechName());
			techMinResistance.put(tech, TempPref.makeDoublePref(tech.getMinResistance()));
			techMinCapacitance.put(tech, TempPref.makeDoublePref(tech.getMinCapacitance()));
			techGateLengthShrink.put(tech, TempPref.makeDoublePref(tech.getGateLengthSubtraction()));
			techIncludeGateInResistance.put(tech, TempPref.makeBooleanPref(tech.isGateIncluded()));
			techIncludeGroundNetwork.put(tech, TempPref.makeBooleanPref(tech.isGroundNetIncluded()));
			techMaxSeriesResistance.put(tech, TempPref.makeDoublePref(tech.getMaxSeriesResistance()));

			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = (Layer)lIt.next();
				layerResistanceOptions.put(layer, TempPref.makeDoublePref(layer.getResistance()));
				layerCapacitanceOptions.put(layer, TempPref.makeDoublePref(layer.getCapacitance()));
				layerEdgeCapacitanceOptions.put(layer, TempPref.makeDoublePref(layer.getEdgeCapacitance()));
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
		maxSeriesResistance.getDocument().addDocumentListener(new ParasiticTechDocumentListener(this));
		gateLengthSubtraction.getDocument().addDocumentListener(new ParasiticTechDocumentListener(this));

		includeGate.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { updateTechnologyGlobals(); }
		});
		includeGround.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { updateTechnologyGlobals(); }
		});
	}

	private void techChanged()
	{
		String techName = (String)techSelection.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return;

		changing = true;
		TempPref pref = techMinResistance.get(tech);
		minResistance.setText(TextUtils.formatDouble(pref.getDouble()));
		pref = techMinCapacitance.get(tech);
		minCapacitance.setText(TextUtils.formatDouble(pref.getDouble()));
		pref = techGateLengthShrink.get(tech);
		gateLengthSubtraction.setText(TextUtils.formatDouble(pref.getDouble()));
        pref = techMaxSeriesResistance.get(tech);
        maxSeriesResistance.setText(TextUtils.formatDouble(pref.getDouble()));

		pref = techIncludeGateInResistance.get(tech);
		includeGate.setSelected(pref.getBoolean());
		pref = techIncludeGroundNetwork.get(tech);
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
			TempPref resistancePref = layerResistanceOptions.get(layer);
			resistance.setText(TextUtils.formatDouble(resistancePref.getDouble()));
			TempPref capacitancePref = layerCapacitanceOptions.get(layer);
			capacitance.setText(TextUtils.formatDouble(capacitancePref.getDouble()));
			TempPref edgeCapacitancePref = layerEdgeCapacitanceOptions.get(layer);
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

		TempPref pref = techMinResistance.get(tech);
		pref.setDouble(TextUtils.atof(minResistance.getText()));
		pref = techMinCapacitance.get(tech);
		pref.setDouble(TextUtils.atof(minCapacitance.getText()));
		pref = techGateLengthShrink.get(tech);
		pref.setDouble(TextUtils.atof(gateLengthSubtraction.getText()));
        pref = techMaxSeriesResistance.get(tech);
        pref.setDouble(TextUtils.atof(maxSeriesResistance.getText()));

		pref = techIncludeGateInResistance.get(tech);
		pref.setBoolean(includeGate.isSelected());
		pref = techIncludeGroundNetwork.get(tech);
		pref.setBoolean(includeGround.isSelected());
	}

	/**
	 * Class to handle special changes to per-layer parasitics.
	 */
	private static class ParasiticLayerDocumentListener implements DocumentListener
	{
		private ParasiticTab dialog;
		private HashMap<Layer,TempPref> map;

		ParasiticLayerDocumentListener(HashMap<Layer,TempPref> map, ParasiticTab dialog)
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
			TempPref pref = map.get(layer);
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
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();

			TempPref pref = techMinResistance.get(tech);
			if (pref != null && pref.getDoubleFactoryValue() != pref.getDouble())
				tech.setMinResistance(pref.getDouble());
			pref = techMinCapacitance.get(tech);
			if (pref != null && pref.getDoubleFactoryValue() != pref.getDouble())
				tech.setMinCapacitance(pref.getDouble());
            pref = techMaxSeriesResistance.get(tech);
            if (pref != null && pref.getDoubleFactoryValue() != pref.getDouble())
                tech.setMaxSeriesResistance(pref.getDouble());
			pref = techGateLengthShrink.get(tech);
			if (pref != null && pref.getDoubleFactoryValue() != pref.getDouble())
				tech.setGateLengthSubtraction(pref.getDouble());

			pref = techIncludeGateInResistance.get(tech);
			if (pref != null && pref.getBooleanFactoryValue() != pref.getBoolean())
				tech.setGateIncluded(pref.getBoolean());
			pref = techIncludeGroundNetwork.get(tech);
			if (pref != null && pref.getBooleanFactoryValue() != pref.getBoolean())
				tech.setGroundNetIncluded(pref.getBoolean());
			
			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = (Layer)lIt.next();
				TempPref resistancePref = layerResistanceOptions.get(layer);
				if (resistancePref != null && resistancePref.getDoubleFactoryValue() != resistancePref.getDouble())
					layer.setResistance(resistancePref.getDouble());
				TempPref capacitancePref = layerCapacitanceOptions.get(layer);
				if (capacitancePref != null && capacitancePref.getDoubleFactoryValue() != capacitancePref.getDouble())
					layer.setCapacitance(capacitancePref.getDouble());
				TempPref edgeCapacitancePref = layerEdgeCapacitanceOptions.get(layer);
				if (edgeCapacitancePref != null && edgeCapacitancePref.getDoubleFactoryValue() != edgeCapacitancePref.getDouble())
					layer.setEdgeCapacitance(edgeCapacitancePref.getDouble());
			}
		}
	}

	private void factoryResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_factoryResetActionPerformed
        String techName = (String)techSelection.getSelectedItem();
        Technology tech = Technology.findTechnology(techName);
        if (tech == null) return;
		int ret = JOptionPane.showConfirmDialog(this,
			"Are you sure you want to reset all layers for technology "+techName+" to their default resistance and capacitance values?",
            "Factory Reset", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (ret == JOptionPane.YES_OPTION) {
            for (Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
            {
                Layer layer = (Layer)lIt.next();
                layer.resetToFactoryParasitics();
            }
			init();
		}
	}//GEN-LAST:event_factoryResetActionPerformed

	/** This method is called from within the constructor to
	 * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
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
        jLabel3 = new javax.swing.JLabel();
        techSelection = new javax.swing.JComboBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        topPanel.setLayout(new java.awt.GridBagLayout());

        techValues.setLayout(new java.awt.GridBagLayout());

        techValues.setBorder(javax.swing.BorderFactory.createTitledBorder("Layer Values"));
        spiceLayer.setMinimumSize(new java.awt.Dimension(200, 50));
        spiceLayer.setPreferredSize(new java.awt.Dimension(200, 50));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
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
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        techValues.add(jLabel7, gridBagConstraints);

        jLabel11.setText("Resistance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        techValues.add(jLabel11, gridBagConstraints);

        jLabel2.setText("Perimeter Cap (fF/um):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        techValues.add(jLabel2, gridBagConstraints);

        resistance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        techValues.add(resistance, gridBagConstraints);

        jLabel12.setText("Area Cap (fF/um^2):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        techValues.add(jLabel12, gridBagConstraints);

        capacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        techValues.add(capacitance, gridBagConstraints);

        edgeCapacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        techValues.add(edgeCapacitance, gridBagConstraints);

        factoryReset.setText("Factory Reset");
        factoryReset.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                factoryResetActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        techValues.add(factoryReset, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        topPanel.add(techValues, gridBagConstraints);

        globalValues.setLayout(new java.awt.GridBagLayout());

        globalValues.setBorder(javax.swing.BorderFactory.createTitledBorder("Global Values"));
        jLabel20.setText("Min. Resistance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        globalValues.add(jLabel20, gridBagConstraints);

        minResistance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        globalValues.add(minResistance, gridBagConstraints);

        jLabel21.setText("Min. Capacitance (fF):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        globalValues.add(jLabel21, gridBagConstraints);

        minCapacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        globalValues.add(minCapacitance, gridBagConstraints);

        jLabel5.setText("Gate Length Shrink (Subtraction) um:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        globalValues.add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        globalValues.add(gateLengthSubtraction, gridBagConstraints);

        includeGate.setText("Include Gate In Resistance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        globalValues.add(includeGate, gridBagConstraints);

        includeGround.setText("Include Ground Network");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        globalValues.add(includeGround, gridBagConstraints);

        jLabel1.setText("Max. Series Resistance: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        globalValues.add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        globalValues.add(maxSeriesResistance, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        topPanel.add(globalValues, gridBagConstraints);

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
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        new ParasiticTab(new javax.swing.JFrame(), true).setVisible(true);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField capacitance;
    private javax.swing.JTextField edgeCapacitance;
    private javax.swing.ButtonGroup exemptedNetsGroup;
    private javax.swing.JButton factoryReset;
    private javax.swing.JTextField gateLengthSubtraction;
    private javax.swing.JPanel globalValues;
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
    private javax.swing.JTextField maxSeriesResistance;
    private javax.swing.JTextField minCapacitance;
    private javax.swing.JTextField minResistance;
    private javax.swing.JTextField resistance;
    private javax.swing.JScrollPane spiceLayer;
    private javax.swing.JComboBox techSelection;
    private javax.swing.JPanel techValues;
    private javax.swing.JPanel topPanel;
    // End of variables declaration//GEN-END:variables
    
}
