/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CIFTab.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs.projsettings;

import com.sun.electric.database.text.Setting;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.ProjectSettingsFrame;

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
 * Class to handle the "CIF" tab of the Project Settings dialog.
 */
public class CIFTab extends ProjSettingsPanel
{
	/** Creates new form CIFTab */
	public CIFTab(ProjectSettingsFrame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// make all text fields select-all when entered
	    EDialog.makeTextFieldSelectAllOnTab(cifLayer);
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return cif; }

	/** return the name of this preferences tab. */
	public String getName() { return "CIF"; }

	private JList cifLayersList;
	private DefaultListModel cifLayersModel;
	private boolean changingCIF = false;
    
    private Setting cifOutMimicsDisplaySetting = IOTool.getCIFOutMimicsDisplaySetting();
    private Setting cifOutMergesBoxesSetting = IOTool.getCIFOutMergesBoxesSetting();
    private Setting cifOutInstantiatesTopLevleSetting = IOTool.getCIFOutInstantiatesTopLevelSetting();

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the CIF tab.
	 */
	public void init()
	{
		cifOutputMimicsDisplay.setSelected(getBoolean(cifOutMimicsDisplaySetting));
		cifOutputMergesBoxes.setSelected(getBoolean(cifOutMergesBoxesSetting));
		cifOutputInstantiatesTopLevel.setSelected(getBoolean(cifOutInstantiatesTopLevleSetting));

		// build the layers list
		cifLayersModel = new DefaultListModel();
		cifLayersList = new JList(cifLayersModel);
		cifLayersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		cifLayers.setViewportView(cifLayersList);
		cifLayersList.clearSelection();
		cifLayersList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { cifClickLayer(); }
		});
		for(Iterator<Technology> tIt = Technology.getTechnologies(); tIt.hasNext(); )
		{
			Technology tech = tIt.next();
			technologySelection.addItem(tech.getTechName());
		}
		technologySelection.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { techChanged(); }
		});
		cifLayer.getDocument().addDocumentListener(new CIFDocumentListener(this));
		technologySelection.setSelectedItem(Technology.getCurrent().getTechName());
	}

	private void techChanged()
	{
		String techName = (String)technologySelection.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return;

		cifLayersModel.clear();
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
            String str = layer.getName();
            String cifLayer = getString(layer.getCIFLayerSetting());
            if (cifLayer == null) cifLayer = "";
            if (cifLayer.length() > 0) str += " (" + cifLayer + ")";
			cifLayersModel.addElement(str);
		}
		cifLayersList.setSelectedIndex(0);
		cifClickLayer();
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
		String techName = (String)technologySelection.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return null;
		
		int openParen = str.indexOf('(');
		if (openParen < 0) openParen = str.length()+1;
		String layerName = str.substring(0, openParen-1);
		Layer layer = tech.findLayer(layerName);
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
        setString(layer.getCIFLayerSetting(), newLayer);
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
        setBoolean(cifOutMimicsDisplaySetting, cifOutputMimicsDisplay.isSelected());
        setBoolean(cifOutMergesBoxesSetting, cifOutputMergesBoxes.isSelected());
        setBoolean(cifOutInstantiatesTopLevleSetting, cifOutputInstantiatesTopLevel.isSelected());
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

        cif = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        cifLayers = new javax.swing.JScrollPane();
        cifOutputMimicsDisplay = new javax.swing.JCheckBox();
        cifOutputMergesBoxes = new javax.swing.JCheckBox();
        cifOutputInstantiatesTopLevel = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        cifLayer = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        technologySelection = new javax.swing.JComboBox();

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
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(jLabel1, gridBagConstraints);

        cifLayers.setPreferredSize(new java.awt.Dimension(200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(cifLayers, gridBagConstraints);

        cifOutputMimicsDisplay.setText("Output Mimics Display");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(cifOutputMimicsDisplay, gridBagConstraints);

        cifOutputMergesBoxes.setText("Output Merges Boxes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        cif.add(cifOutputMergesBoxes, gridBagConstraints);

        cifOutputInstantiatesTopLevel.setText("Output Instantiates Top Level");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 8, 4);
        cif.add(cifOutputInstantiatesTopLevel, gridBagConstraints);

        jLabel2.setText("(time consuming)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        cif.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(cifLayer, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        cif.add(jPanel1, gridBagConstraints);

        jLabel3.setText("Technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(technologySelection, gridBagConstraints);

        getContentPane().add(cif, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel cif;
    private javax.swing.JTextField cifLayer;
    private javax.swing.JScrollPane cifLayers;
    private javax.swing.JCheckBox cifOutputInstantiatesTopLevel;
    private javax.swing.JCheckBox cifOutputMergesBoxes;
    private javax.swing.JCheckBox cifOutputMimicsDisplay;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JComboBox technologySelection;
    // End of variables declaration//GEN-END:variables

}
