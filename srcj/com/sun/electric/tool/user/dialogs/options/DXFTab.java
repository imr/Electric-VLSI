/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DXFTab.java
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
 * Class to handle the "DXF" tab of the Preferences dialog.
 */
public class DXFTab extends PreferencePanel
{
	private Technology tech;
	private JList dxfLayersList;
	private DefaultListModel dxfLayersModel;
	private boolean initialInputFlattensHierarchy;
	private boolean initialInputReadsAllLayers;
	private int initialScale;
	private TextUtils.UnitScale [] scales;
	private boolean changingDXF = false;

	/** Creates new form DXFTab */
	public DXFTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return dxf; }

	public String getName() { return "DXF"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the DXF tab.
	 */
	public void init()
	{
		tech = Technology.getCurrent();
		dxfTechnology.setText("DXF layers for technology: " + tech.getTechName());

		// build the layers list
		dxfLayersModel = new DefaultListModel();
		dxfLayersList = new JList(dxfLayersModel);
		dxfLayersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		dxfLayerList.setViewportView(dxfLayersList);
		dxfLayersList.clearSelection();
		dxfLayersList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { dxfClickLayer(); }
		});
		dxfLayersModel.clear();
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			String str = layer.getName();
			String dxfLayer = layer.getDXFLayer();
			if (dxfLayer == null) dxfLayer = "";
			if (dxfLayer.length() > 0) str += " (" + dxfLayer + ")";
			dxfLayersModel.addElement(str);
		}
		dxfLayersList.setSelectedIndex(0);
		dxfLayerName.getDocument().addDocumentListener(new DXFDocumentListener(this));
		dxfClickLayer();

		// initialize the scale popup
		initialScale = IOTool.getDXFScale();
		scales = TextUtils.UnitScale.getUnitScales();
		for(int i=0; i<scales.length; i++)
		{
			dxfScale.addItem(scales[i].getName() + "Meter");
		}
		dxfScale.setSelectedItem(TextUtils.UnitScale.findFromIndex(initialScale).getName() + "Meter");

		initialInputFlattensHierarchy = IOTool.isDXFInputFlattensHierarchy();
		dxfInputFlattensHierarchy.setSelected(initialInputFlattensHierarchy);
		initialInputReadsAllLayers = IOTool.isDXFInputReadsAllLayers();
		dxfInputReadsAllLayers.setSelected(initialInputReadsAllLayers);

		// not yet
		dxfInputFlattensHierarchy.setEnabled(false);
		dxfInputReadsAllLayers.setEnabled(false);
	}

	/**
	 * Method called when the user clicks on a layer name in the scrollable list.
	 */
	private void dxfClickLayer()
	{
		changingDXF = true;
		String str = (String)dxfLayersList.getSelectedValue();
		dxfLayerName.setText(dxfGetLayerName(str));
		changingDXF = false;
	}

	/**
	 * Method to parse the line in the scrollable list and return the DXF layer name part
	 * (in parentheses).
	 */
	private String dxfGetLayerName(String str)
	{
		int openParen = str.indexOf('(');
		if (openParen < 0) return "";
		int closeParen = str.lastIndexOf(')');
		if (closeParen < 0) return "";
		String dxfLayer = str.substring(openParen+1, closeParen);
		return dxfLayer;
	}

	/**
	 * Method to parse the line in the scrollable list and return the Layer.
	 */
	private Layer dxfGetLayer(String str)
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
	private void dxfLayerChanged()
	{
		if (changingDXF) return;
		String str = (String)dxfLayersList.getSelectedValue();
		Layer layer = dxfGetLayer(str);
		if (layer == null) return;
		String newLine = layer.getName();
		String newLayer = dxfLayerName.getText().trim();
		if (newLayer.length() > 0) newLine += " (" + newLayer + ")";
		int index = dxfLayersList.getSelectedIndex();
		dxfLayersModel.set(index, newLine);
	}

	/**
	 * Class to handle special changes to changes to a DXF layer.
	 */
	private static class DXFDocumentListener implements DocumentListener
	{
		DXFTab dialog;

		DXFDocumentListener(DXFTab dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.dxfLayerChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.dxfLayerChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.dxfLayerChanged(); }
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the DXF tab.
	 */
	public void term()
	{
		for(int i=0; i<dxfLayersModel.getSize(); i++)
		{
			String str = (String)dxfLayersModel.getElementAt(i);
			Layer layer = dxfGetLayer(str);
			if (layer == null) continue;

			String currentDXFNumbers = dxfGetLayerName(str);
			if (currentDXFNumbers.equalsIgnoreCase(layer.getDXFLayer())) continue;
			layer.setDXFLayer(currentDXFNumbers);
		}

		int currentScaleIndex = dxfScale.getSelectedIndex();
		int currentScale = scales[currentScaleIndex].getIndex();
		if (currentScale != initialScale)
			IOTool.setDXFScale(currentScale);

		boolean currentInputFlattensHierarchy = dxfInputFlattensHierarchy.isSelected();
		if (currentInputFlattensHierarchy != initialInputFlattensHierarchy)
			IOTool.setDXFInputFlattensHierarchy(currentInputFlattensHierarchy);

		boolean currentInputReadsAllLayers = dxfInputReadsAllLayers.isSelected();
		if (currentInputReadsAllLayers != initialInputReadsAllLayers)
			IOTool.setDXFInputReadsAllLayers(currentInputReadsAllLayers);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        dxf = new javax.swing.JPanel();
        dxfLayerList = new javax.swing.JScrollPane();
        jLabel16 = new javax.swing.JLabel();
        dxfLayerName = new javax.swing.JTextField();
        dxfInputFlattensHierarchy = new javax.swing.JCheckBox();
        dxfInputReadsAllLayers = new javax.swing.JCheckBox();
        jLabel17 = new javax.swing.JLabel();
        dxfScale = new javax.swing.JComboBox();
        dxfTechnology = new javax.swing.JLabel();

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

        dxf.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        dxf.add(dxfLayerList, gridBagConstraints);

        jLabel16.setText("DXF Layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        dxf.add(jLabel16, gridBagConstraints);

        dxfLayerName.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        dxf.add(dxfLayerName, gridBagConstraints);

        dxfInputFlattensHierarchy.setText("Input flattens hierarchy");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        dxf.add(dxfInputFlattensHierarchy, gridBagConstraints);

        dxfInputReadsAllLayers.setText("Input reads all layers");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        dxf.add(dxfInputReadsAllLayers, gridBagConstraints);

        jLabel17.setText("DXF Scale:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        dxf.add(jLabel17, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        dxf.add(dxfScale, gridBagConstraints);

        dxfTechnology.setText("DXF layers for technology: mocmos");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        dxf.add(dxfTechnology, gridBagConstraints);

        getContentPane().add(dxf, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel dxf;
    private javax.swing.JCheckBox dxfInputFlattensHierarchy;
    private javax.swing.JCheckBox dxfInputReadsAllLayers;
    private javax.swing.JScrollPane dxfLayerList;
    private javax.swing.JTextField dxfLayerName;
    private javax.swing.JComboBox dxfScale;
    private javax.swing.JLabel dxfTechnology;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    // End of variables declaration//GEN-END:variables
	
}
