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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * ParasiticTab.java
 * @author  Gilda Garreton
 */
public class ParasiticTab extends PreferencePanel {

    /** Creates new form ParasiticTab */
    public ParasiticTab(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

	private HashMap layerResistanceOptions;
	private HashMap layerCapacitanceOptions;
	private HashMap layerEdgeCapacitanceOptions;
	private JList layerList;
    private DefaultListModel layerListModel;
	private double techMinResistanceInitial;
	private double techMinCapacitanceInitial;
    private double gateLengthSubtractionInitial;

	public JPanel getPanel() { return topPanel; }

	public String getName() { return "Parasitic"; }

    /**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Routing tab.
	 */
	public void init()
	{
        maxDistValue.setText(Double.toString(ParasiticTool.getMaxDistance()));

        // the next section: parasitic values
        technology.setText("For technology '" + curTech.getTechName() + "'");

        layerResistanceOptions = new HashMap();
        layerCapacitanceOptions = new HashMap();
        layerEdgeCapacitanceOptions = new HashMap();
        for(Iterator it = curTech.getLayers(); it.hasNext(); )
        {
            Layer layer = (Layer)it.next();
            layerResistanceOptions.put(layer, Pref.makeDoublePref(null, null, layer.getResistance()));
            layerCapacitanceOptions.put(layer, Pref.makeDoublePref(null, null, layer.getCapacitance()));
            layerEdgeCapacitanceOptions.put(layer, Pref.makeDoublePref(null, null, layer.getEdgeCapacitance()));
        }
        layerListModel = new DefaultListModel();
        layerList = new JList(layerListModel);
        layerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        spiceLayer.setViewportView(layerList);
        layerList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent evt) { spiceLayerListClick(); }
        });
        showLayersInTechnology(layerListModel);
        layerList.setSelectedIndex(0);
        spiceLayerListClick();
        resistance.getDocument().addDocumentListener(new ParasiticDocumentListener(layerResistanceOptions, layerList, curTech));
        capacitance.getDocument().addDocumentListener(new ParasiticDocumentListener(layerCapacitanceOptions, layerList, curTech));
        edgeCapacitance.getDocument().addDocumentListener(new ParasiticDocumentListener(layerEdgeCapacitanceOptions, layerList, curTech));

        techMinResistanceInitial = curTech.getMinResistance();
        minResistance.setText(Double.toString(techMinResistanceInitial));

        techMinCapacitanceInitial = curTech.getMinCapacitance();
        minCapacitance.setText(Double.toString(techMinCapacitanceInitial));

        gateLengthSubtractionInitial = curTech.getGateLengthSubtraction();
        gateLengthSubtraction.setText(Double.toString(gateLengthSubtractionInitial));
    }

    private void showLayersInTechnology(DefaultListModel model)
	{
		model.clear();
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			model.addElement(layer.getName());
		}
	}

    private void spiceLayerListClick()
    {
        String layerName = (String)layerList.getSelectedValue();
        Layer layer = curTech.findLayer(layerName);
        if (layer != null)
        {
            Pref resistancePref = (Pref)layerResistanceOptions.get(layer);
            resistance.setText(Double.toString(resistancePref.getDouble()));
            Pref capacitancePref = (Pref)layerCapacitanceOptions.get(layer);
            capacitance.setText(Double.toString(capacitancePref.getDouble()));
            Pref edgeCapacitancePref = (Pref)layerEdgeCapacitanceOptions.get(layer);
            edgeCapacitance.setText(Double.toString(edgeCapacitancePref.getDouble()));
        }
    }

	/**
	 * Class to handle special changes to per-layer parasitics.
	 */
	private static class ParasiticDocumentListener implements DocumentListener
	{
		HashMap optionMap;
		JList list;
		Technology tech;

		ParasiticDocumentListener(HashMap optionMap, JList list, Technology tech)
		{
			this.optionMap = optionMap;
			this.list = list;
			this.tech = tech;
		}

		private void change(DocumentEvent e)
		{
			// get the currently selected layer
			String layerName = (String)list.getSelectedValue();
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
			Pref pref = (Pref)optionMap.get(layer);
			double v = TextUtils.atof(text);

			// update the option
			pref.setDouble(v);
		}

		public void changedUpdate(DocumentEvent e) { change(e); }
		public void insertUpdate(DocumentEvent e) { change(e); }
		public void removeUpdate(DocumentEvent e) { change(e); }
	}

    /**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Routing tab.
	 */
	public void term()
	{
        ParasiticTool.setMaxDistance(Double.parseDouble(maxDistValue.getText()));

        double doubleNow = TextUtils.atof(minResistance.getText());
        if (techMinResistanceInitial != doubleNow) curTech.setMinResistance(doubleNow);
        doubleNow = TextUtils.atof(minCapacitance.getText());
        if (techMinCapacitanceInitial != doubleNow) curTech.setMinCapacitance(doubleNow);

        for(Iterator it = curTech.getLayers(); it.hasNext(); )
        {
            Layer layer = (Layer)it.next();
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

        doubleNow = TextUtils.atof(gateLengthSubtraction.getText());
        if (gateLengthSubtractionInitial != doubleNow) curTech.setGateLengthSubtraction(doubleNow);

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        topPanel = new javax.swing.JPanel();
        parasiticValues = new javax.swing.JPanel();
        spiceLayer = new javax.swing.JScrollPane();
        jLabel7 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        technology = new javax.swing.JLabel();
        resistance = new javax.swing.JTextField();
        capacitance = new javax.swing.JTextField();
        edgeCapacitance = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        minResistance = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        minCapacitance = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        gateLengthSubtraction = new javax.swing.JTextField();
        parasiticPanel = new javax.swing.JPanel();
        maxDist = new javax.swing.JLabel();
        maxDistValue = new javax.swing.JTextField();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        topPanel.setLayout(new java.awt.GridBagLayout());

        parasiticValues.setLayout(new java.awt.GridBagLayout());

        parasiticValues.setBorder(new javax.swing.border.TitledBorder("Parasitic Values"));
        spiceLayer.setMinimumSize(new java.awt.Dimension(200, 50));
        spiceLayer.setPreferredSize(new java.awt.Dimension(200, 50));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        parasiticValues.add(spiceLayer, gridBagConstraints);

        jLabel7.setText("Layer:");
        jLabel7.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        parasiticValues.add(jLabel7, gridBagConstraints);

        jLabel2.setText("Perimeter Cap (fF/um):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        parasiticValues.add(jLabel2, gridBagConstraints);

        jLabel11.setText("Resistance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        parasiticValues.add(jLabel11, gridBagConstraints);

        jLabel12.setText("Area Cap (fF/um^2):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        parasiticValues.add(jLabel12, gridBagConstraints);

        technology.setText("Technology: xxx");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        parasiticValues.add(technology, gridBagConstraints);

        resistance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        parasiticValues.add(resistance, gridBagConstraints);

        capacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        parasiticValues.add(capacitance, gridBagConstraints);

        edgeCapacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        parasiticValues.add(edgeCapacitance, gridBagConstraints);

        jLabel18.setText("Min. Resistance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        parasiticValues.add(jLabel18, gridBagConstraints);

        minResistance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        parasiticValues.add(minResistance, gridBagConstraints);

        jLabel19.setText("Min. Capacitance (fF):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        parasiticValues.add(jLabel19, gridBagConstraints);

        minCapacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        parasiticValues.add(minCapacitance, gridBagConstraints);

        jLabel4.setText("Gate Length Shrink (Subtraction) um:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        parasiticValues.add(jLabel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        parasiticValues.add(gateLengthSubtraction, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        topPanel.add(parasiticValues, gridBagConstraints);

        parasiticPanel.setLayout(new java.awt.GridBagLayout());

        parasiticPanel.setBorder(new javax.swing.border.TitledBorder("Parasitic Searching Options"));
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
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        parasiticPanel.add(maxDistValue, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        topPanel.add(parasiticPanel, gridBagConstraints);

        getContentPane().add(topPanel, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        new ParasiticTab(new javax.swing.JFrame(), true).setVisible(true);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField capacitance;
    private javax.swing.JTextField edgeCapacitance;
    private javax.swing.JTextField gateLengthSubtraction;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel maxDist;
    private javax.swing.JTextField maxDistValue;
    private javax.swing.JTextField minCapacitance;
    private javax.swing.JTextField minResistance;
    private javax.swing.JPanel parasiticPanel;
    private javax.swing.JPanel parasiticValues;
    private javax.swing.JTextField resistance;
    private javax.swing.JScrollPane spiceLayer;
    private javax.swing.JLabel technology;
    private javax.swing.JPanel topPanel;
    // End of variables declaration//GEN-END:variables
    
}
