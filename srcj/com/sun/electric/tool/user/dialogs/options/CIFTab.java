/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CIFTab.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.output.GDS;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.HashMap;
import javax.print.PrintServiceLookup;
import javax.print.PrintService;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Class to handle the "CIF" tab of the Preferences dialog.
 */
public class CIFTab extends PreferencePanel
{
	private Technology curTech = Technology.getCurrent();

	/** Creates new form CIFTab */
	public CIFTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return cif; }

	public String getName() { return "CIF"; }

	private boolean initialCIFOutputMimicsDisplay;
	private boolean initialCIFOutputMergesPolygons;
	private boolean initialCIFOutputInstantiatesTopLevel;
	private boolean initialCIFOutputCheckResolution;
	private double initialCIFOutputResolution;
	private JList cifLayersList;
	private DefaultListModel cifLayersModel;
	private boolean changingCIF = false;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the CIF tab.
	 */
	public void init()
	{
		initialCIFOutputMimicsDisplay = IOTool.isCIFOutMimicsDisplay();
		cifOutputMimicsDisplay.setSelected(initialCIFOutputMimicsDisplay);

		initialCIFOutputMergesPolygons = IOTool.isCIFOutMergesBoxes();
		cifOutputMergesBoxes.setSelected(initialCIFOutputMergesPolygons);

		initialCIFOutputInstantiatesTopLevel = IOTool.isCIFOutInstantiatesTopLevel();
		cifOutputInstantiatesTopLevel.setSelected(initialCIFOutputInstantiatesTopLevel);

		initialCIFOutputCheckResolution = IOTool.isCIFOutCheckResolution();
		cifCheckResolution.setSelected(initialCIFOutputCheckResolution);

		initialCIFOutputResolution = IOTool.getCIFOutResolution();
		cifResolutionValue.setText(Double.toString(initialCIFOutputResolution));

		// build the layers list
		cifTechnology.setText("Technology " + curTech.getTechName() + ":");
		cifLayersModel = new DefaultListModel();
		cifLayersList = new JList(cifLayersModel);
		cifLayersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		cifLayers.setViewportView(cifLayersList);
		cifLayersList.clearSelection();
		cifLayersList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { cifClickLayer(); }
		});
		cifLayersModel.clear();
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			String str = layer.getName();
			String cifLayer = layer.getCIFLayer();
			if (cifLayer == null) cifLayer = "";
			if (cifLayer.length() > 0) str += " (" + cifLayer + ")";
			cifLayersModel.addElement(str);
		}
		cifLayersList.setSelectedIndex(0);
		cifLayer.getDocument().addDocumentListener(new CIFDocumentListener(this));
		cifClickLayer();

		// not yet
		cifInputSquaresWires.setEnabled(false);
	}

	/**
	 * Method called when the user clicks on a layer name in the scrollable list.
	 */
	private void cifClickLayer()
	{
		changingCIF = true;
		String str = (String)cifLayersList.getSelectedValue();
		cifLayer.setText(cifGetLayerName(str));
		changingCIF = false;
	}

	/**
	 * Method to parse the line in the scrollable list and return the CIF layer name part
	 * (in parentheses).
	 */
	private String cifGetLayerName(String str)
	{
		int openParen = str.indexOf('(');
		if (openParen < 0) return "";
		int closeParen = str.lastIndexOf(')');
		if (closeParen < 0) return "";
		String cifLayer = str.substring(openParen+1, closeParen);
		return cifLayer;
	}

	/**
	 * Method to parse the line in the scrollable list and return the Layer.
	 */
	private Layer cifGetLayer(String str)
	{
		int openParen = str.indexOf('(');
		if (openParen < 0) openParen = str.length()+1;
		String layerName = str.substring(0, openParen-1);
		Layer layer = curTech.findLayer(layerName);
		return layer;
	}

	/**
	 * Method called when the user types a new layer name into the edit field.
	 */
	private void cifLayerChanged()
	{
		if (changingCIF) return;
		String str = (String)cifLayersList.getSelectedValue();
		Layer layer = cifGetLayer(str);
		if (layer == null) return;
		String newLine = layer.getName();
		String newLayer = cifLayer.getText().trim();
		if (newLayer.length() > 0) newLine += " (" + newLayer + ")";
		int index = cifLayersList.getSelectedIndex();
		cifLayersModel.set(index, newLine);
	}

	/**
	 * Class to handle special changes to changes to a CIF layer.
	 */
	private static class CIFDocumentListener implements DocumentListener
	{
		CIFTab dialog;

		CIFDocumentListener(CIFTab dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.cifLayerChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.cifLayerChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.cifLayerChanged(); }
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the CIF tab.
	 */
	public void term()
	{
		for(int i=0; i<cifLayersModel.getSize(); i++)
		{
			String str = (String)cifLayersModel.getElementAt(i);
			Layer layer = cifGetLayer(str);
			if (layer == null) continue;

			String currentCIFNumbers = cifGetLayerName(str);
			if (currentCIFNumbers.equalsIgnoreCase(layer.getCIFLayer())) continue;
			layer.setCIFLayer(currentCIFNumbers);
		}
		boolean currentMimicsDisplay = cifOutputMimicsDisplay.isSelected();
		if (currentMimicsDisplay != initialCIFOutputMimicsDisplay)
			IOTool.setCIFOutMimicsDisplay(currentMimicsDisplay);

		boolean currentMergesPolygons = cifOutputMergesBoxes.isSelected();
		if (currentMergesPolygons != initialCIFOutputMergesPolygons)
			IOTool.setCIFOutMergesBoxes(currentMergesPolygons);

		boolean currentInstantiatesTopLevel = cifOutputInstantiatesTopLevel.isSelected();
		if (currentInstantiatesTopLevel != initialCIFOutputInstantiatesTopLevel)
			IOTool.setCIFOutInstantiatesTopLevel(currentInstantiatesTopLevel);

		boolean currentCheckResolution = cifCheckResolution.isSelected();
		if (currentCheckResolution != initialCIFOutputCheckResolution)
			IOTool.setCIFOutCheckResolution(currentCheckResolution);

		double currentResolution = TextUtils.atof(cifResolutionValue.getText());
		if (currentResolution != initialCIFOutputResolution)
			IOTool.setCIFOutResolution(currentResolution);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        cif = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        cifLayers = new javax.swing.JScrollPane();
        cifOutputMimicsDisplay = new javax.swing.JCheckBox();
        cifOutputMergesBoxes = new javax.swing.JCheckBox();
        cifOutputInstantiatesTopLevel = new javax.swing.JCheckBox();
        cifInputSquaresWires = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        cifLayer = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        cifResolutionValue = new javax.swing.JTextField();
        cifCheckResolution = new javax.swing.JCheckBox();
        cifTechnology = new javax.swing.JLabel();

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

        cif.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("CIF Layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(jLabel1, gridBagConstraints);

        cifLayers.setPreferredSize(new java.awt.Dimension(200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(cifLayers, gridBagConstraints);

        cifOutputMimicsDisplay.setText("Output Mimics Display");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(cifOutputMimicsDisplay, gridBagConstraints);

        cifOutputMergesBoxes.setText("Output Merges Boxes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        cif.add(cifOutputMergesBoxes, gridBagConstraints);

        cifOutputInstantiatesTopLevel.setText("Output Instantiates Top Level");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 8, 4);
        cif.add(cifOutputInstantiatesTopLevel, gridBagConstraints);

        cifInputSquaresWires.setText("Input Squares Wires");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 4, 4);
        cif.add(cifInputSquaresWires, gridBagConstraints);

        jLabel2.setText("(time consuming)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        cif.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(cifLayer, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel3.setText("Output resolution:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        jPanel1.add(jLabel3, gridBagConstraints);

        cifResolutionValue.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(cifResolutionValue, gridBagConstraints);

        cifCheckResolution.setText("Find resolution errors");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(cifCheckResolution, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        cif.add(jPanel1, gridBagConstraints);

        cifTechnology.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(cifTechnology, gridBagConstraints);

        getContentPane().add(cif, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel cif;
    private javax.swing.JCheckBox cifCheckResolution;
    private javax.swing.JCheckBox cifInputSquaresWires;
    private javax.swing.JTextField cifLayer;
    private javax.swing.JScrollPane cifLayers;
    private javax.swing.JCheckBox cifOutputInstantiatesTopLevel;
    private javax.swing.JCheckBox cifOutputMergesBoxes;
    private javax.swing.JCheckBox cifOutputMimicsDisplay;
    private javax.swing.JTextField cifResolutionValue;
    private javax.swing.JLabel cifTechnology;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
	
}
