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

import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.output.GDS;

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
	/** Creates new form GDSTab */
	public GDSTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return gds; }

	public String getName() { return "GDS"; }

	private boolean initialGDSOutputMergesBoxes;
	private boolean initialGDSOutputWritesExportPins;
	private boolean initialGDSOutputUpperCase;
	private boolean initialGDSInputIncludesText;
	private boolean initialGDSInputExpandsCells;
	private boolean initialGDSInputInstantiatesArrays;
	private boolean initialGDSInputIgnoresUnknownLayers;
	private int initialGDSTextLayer;
	private JList gdsLayersList;
	private DefaultListModel gdsLayersModel;
	private boolean changingGDS = false;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the GDS tab.
	 */
	public void init()
	{
		gdsTechName.setText("Technology " + curTech.getTechName() + ":");
		initialGDSOutputMergesBoxes = IOTool.isGDSOutMergesBoxes();
		gdsOutputMergesBoxes.setSelected(initialGDSOutputMergesBoxes);
		initialGDSOutputWritesExportPins = IOTool.isGDSOutWritesExportPins();
		gdsOutputWritesExportPins.setSelected(initialGDSOutputWritesExportPins);
		initialGDSOutputUpperCase = IOTool.isGDSOutUpperCase();
		gdsOutputUpperCase.setSelected(initialGDSOutputUpperCase);
		initialGDSTextLayer = IOTool.getGDSOutDefaultTextLayer();
		gdsDefaultTextLayer.setText(Integer.toString(initialGDSTextLayer));

		initialGDSInputIncludesText = IOTool.isGDSInIncludesText();
		gdsInputIncludesText.setSelected(initialGDSInputIncludesText);
		initialGDSInputExpandsCells = IOTool.isGDSInExpandsCells();
		gdsInputExpandsCells.setSelected(initialGDSInputExpandsCells);
		initialGDSInputInstantiatesArrays = IOTool.isGDSInInstantiatesArrays();
		gdsInputInstantiatesArrays.setSelected(initialGDSInputInstantiatesArrays);
		initialGDSInputIgnoresUnknownLayers = IOTool.isGDSInIgnoresUnknownLayers();
		gdsInputIgnoresUnknownLayers.setSelected(initialGDSInputIgnoresUnknownLayers);

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
		gdsLayersModel.clear();
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			String str = layer.getName();
			String gdsLayer = layer.getGDSLayer();
			if (gdsLayer != null) str += " (" + gdsLayer + ")";
			gdsLayersModel.addElement(str);
		}
		gdsLayersList.setSelectedIndex(0);
		gdsClickLayer();

		GDSDocumentListener myDocumentListener = new GDSDocumentListener(this);
		gdsLayerNumber.getDocument().addDocumentListener(myDocumentListener);
		gdsPinLayer.getDocument().addDocumentListener(myDocumentListener);
		gdsTextLayer.getDocument().addDocumentListener(myDocumentListener);
	}

	/**
	 * Method called when the user clicks on a layer name in the scrollable list.
	 */
	private void gdsClickLayer()
	{
		changingGDS = true;
		String str = (String)gdsLayersList.getSelectedValue();
		GDS.GDSLayers numbers = gdsGetNumbers(str);
		if (numbers == null) return;
		if (numbers.normal < 0) gdsLayerNumber.setText(""); else
			gdsLayerNumber.setText(Integer.toString(numbers.normal));
		if (numbers.pin < 0) gdsPinLayer.setText(""); else
			gdsPinLayer.setText(Integer.toString(numbers.pin));
		if (numbers.text < 0) gdsTextLayer.setText(""); else
			gdsTextLayer.setText(Integer.toString(numbers.text));
		changingGDS = false;
	}

	/**
	 * Method to parse the line in the scrollable list and return the GDS layer numbers part
	 * (in parentheses).
	 */
	private GDS.GDSLayers gdsGetNumbers(String str)
	{
		int openParen = str.indexOf('(');
		if (openParen < 0) return null;
		int closeParen = str.lastIndexOf(')');
		if (closeParen < 0) return null;
		String gdsNumbers = str.substring(openParen+1, closeParen);
		GDS.GDSLayers numbers = GDS.parseLayerString(gdsNumbers);
		return numbers;
	}

	/**
	 * Method to parse the line in the scrollable list and return the Layer.
	 */
	private Layer gdsGetLayer(String str)
	{
		int openParen = str.indexOf('(');
		if (openParen < 0) return null;
		String layerName = str.substring(0, openParen-1);
		Layer layer = curTech.findLayer(layerName);
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
		String newLine = layer.getName() + " (" + gdsLayerNumber.getText().trim();
		String pinLayer = gdsPinLayer.getText().trim();
		if (pinLayer.length() > 0) newLine += "," + pinLayer + "p";
		String textLayer = gdsTextLayer.getText().trim();
		if (textLayer.length() > 0) newLine += "," + textLayer + "t";
		newLine += ")";
		int index = gdsLayersList.getSelectedIndex();
		gdsLayersModel.set(index, newLine);
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
		for(int i=0; i<gdsLayersModel.getSize(); i++)
		{
			String str = (String)gdsLayersModel.getElementAt(i);
			Layer layer = gdsGetLayer(str);
			if (layer == null) continue;

			GDS.GDSLayers numbers = gdsGetNumbers(str);
			if (numbers == null) continue;
			String currentGDSNumbers = "";
			if (numbers.normal >= 0) currentGDSNumbers += Integer.toString(numbers.normal);
			if (numbers.pin >= 0) currentGDSNumbers += "," + numbers.pin + "p";
			if (numbers.text >= 0) currentGDSNumbers += "," + numbers.text + "t";
			if (currentGDSNumbers.equalsIgnoreCase(layer.getGDSLayer())) continue;
			layer.setGDSLayer(currentGDSNumbers);
		}
		boolean currentOutputMergesBoxes = gdsOutputMergesBoxes.isSelected();
		if (currentOutputMergesBoxes != initialGDSOutputMergesBoxes)
			IOTool.setGDSOutMergesBoxes(currentOutputMergesBoxes);
		boolean currentOutputWritesExportPins = gdsOutputWritesExportPins.isSelected();
		if (currentOutputWritesExportPins != initialGDSOutputWritesExportPins)
			IOTool.setGDSOutWritesExportPins(currentOutputWritesExportPins);
		boolean currentOutputUpperCase = gdsOutputUpperCase.isSelected();
		if (currentOutputUpperCase != initialGDSOutputUpperCase)
			IOTool.setGDSOutUpperCase(currentOutputUpperCase);
		int currentTextLayer = TextUtils.atoi(gdsDefaultTextLayer.getText());
		if (currentTextLayer != initialGDSTextLayer)
			IOTool.setGDSOutDefaultTextLayer(currentTextLayer);

		boolean currentInputIncludesText = gdsInputIncludesText.isSelected();
		if (currentInputIncludesText != initialGDSInputIncludesText)
			IOTool.setGDSInIncludesText(currentInputIncludesText);
		boolean currentInputExpandsCells = gdsInputExpandsCells.isSelected();
		if (currentInputExpandsCells != initialGDSInputExpandsCells)
			IOTool.setGDSInExpandsCells(currentInputExpandsCells);
		boolean currentInputInstantiatesArrays = gdsInputInstantiatesArrays.isSelected();
		if (currentInputInstantiatesArrays != initialGDSInputInstantiatesArrays)
			IOTool.setGDSInInstantiatesArrays(currentInputInstantiatesArrays);
		boolean currentInputIgnoresUnknownLayers = gdsInputIgnoresUnknownLayers.isSelected();
		if (currentInputIgnoresUnknownLayers != initialGDSInputIgnoresUnknownLayers)
			IOTool.setGDSInIgnoresUnknownLayers(currentInputIgnoresUnknownLayers);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
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
        gdsTechName = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();

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

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsLayerList, gridBagConstraints);

        jLabel6.setText("GDS Layer(s):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gds.add(jLabel6, gridBagConstraints);

        gdsLayerNumber.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsLayerNumber, gridBagConstraints);

        jLabel7.setText("Pin layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        gds.add(jLabel7, gridBagConstraints);

        gdsPinLayer.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsPinLayer, gridBagConstraints);

        jLabel8.setText("Text layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        gds.add(jLabel8, gridBagConstraints);

        gdsTextLayer.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsTextLayer, gridBagConstraints);

        gdsInputIncludesText.setText("Input includes Text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        gds.add(gdsInputIncludesText, gridBagConstraints);

        gdsInputExpandsCells.setText("Input expands cells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsInputExpandsCells, gridBagConstraints);

        gdsInputInstantiatesArrays.setText("Input instantiates Arrays");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsInputInstantiatesArrays, gridBagConstraints);

        gdsInputIgnoresUnknownLayers.setText("Input ignores unknown layers");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 6, 4);
        gds.add(gdsInputIgnoresUnknownLayers, gridBagConstraints);

        gdsOutputMergesBoxes.setText("Output merges Boxes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 4, 2, 4);
        gds.add(gdsOutputMergesBoxes, gridBagConstraints);

        gdsOutputWritesExportPins.setText("Output writes export Pins");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsOutputWritesExportPins, gridBagConstraints);

        gdsOutputUpperCase.setText("Output all upper-case");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        gds.add(gdsOutputUpperCase, gridBagConstraints);

        jLabel9.setText("Output default text layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gds.add(jLabel9, gridBagConstraints);

        gdsDefaultTextLayer.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsDefaultTextLayer, gridBagConstraints);

        gdsTechName.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsTechName, gridBagConstraints);

        jLabel29.setText("Negative layer values generate no GDS");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 3;
        gds.add(jLabel29, gridBagConstraints);

        getContentPane().add(gds, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel gds;
    private javax.swing.JTextField gdsDefaultTextLayer;
    private javax.swing.JCheckBox gdsInputExpandsCells;
    private javax.swing.JCheckBox gdsInputIgnoresUnknownLayers;
    private javax.swing.JCheckBox gdsInputIncludesText;
    private javax.swing.JCheckBox gdsInputInstantiatesArrays;
    private javax.swing.JScrollPane gdsLayerList;
    private javax.swing.JTextField gdsLayerNumber;
    private javax.swing.JCheckBox gdsOutputMergesBoxes;
    private javax.swing.JCheckBox gdsOutputUpperCase;
    private javax.swing.JCheckBox gdsOutputWritesExportPins;
    private javax.swing.JTextField gdsPinLayer;
    private javax.swing.JLabel gdsTechName;
    private javax.swing.JTextField gdsTextLayer;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    // End of variables declaration//GEN-END:variables

}
