/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SkillTab.java
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
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.io.IOTool;

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
 * Class to handle the "Skill" tab of the Preferences dialog.
 */
public class SkillTab extends PreferencePanel
{
	private boolean initialExcludesSubcells;
	private boolean initialFlattensHierarchy;
    private boolean initialGDSNameLimit;
	private JList skillLayerList;
	private DefaultListModel skillLayerModel;
	private HashMap skillLayers;
	private Technology curTech;

	/** Creates new form SkillTab */
	public SkillTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return skill; }

	public String getName() { return "Skill"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Skill tab.
	 */
	public void init()
	{
		// build the layers list
		skillLayerModel = new DefaultListModel();
		skillLayerList = new JList(skillLayerModel);
		skillLayerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		skillLayerPane.setViewportView(skillLayerList);
		skillLayerList.clearSelection();
		skillLayerList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { skillClickLayer(); }
		});
		skillLayerModel.clear();
		skillLayers = new HashMap();
		curTech = Technology.getCurrent();
		skillTechnology.setText("Skill layers for technology: " + curTech.getTechName());
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			String skillLayerName = layer.getSkillLayer();
			if (skillLayerName == null) skillLayerName = "";
			skillLayers.put(layer, skillLayerName);
			skillLayerModel.addElement(layer.getName() + " (" + skillLayerName + ")");
		}
		skillLayerList.setSelectedIndex(0);
		skillClickLayer();

		skillLayerName.getDocument().addDocumentListener(new LayerDocumentListener(this));

		initialExcludesSubcells = IOTool.isSkillExcludesSubcells();
		initialFlattensHierarchy = IOTool.isSkillFlattensHierarchy();
        initialGDSNameLimit = IOTool.isSkillGDSNameLimit();
		skillNoSubCells.setSelected(initialExcludesSubcells);
		skillFlattenHierarchy.setSelected(initialFlattensHierarchy);

		if (!IOTool.hasSkill())
			skillNoSkill.setText("SKILL OUTPUT IS NOT INSTALLED!");
	}

	/**
	 * Class to handle special changes to changes to a Technology in the Skill panel.
	 */
	private static class LayerDocumentListener implements DocumentListener
	{
		SkillTab dialog;

		LayerDocumentListener(SkillTab dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.skillLayerChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.skillLayerChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.skillLayerChanged(); }
	}

	/**
	 * Method called when the user types a new value into the Skill layer field.
	 */
	private void skillLayerChanged()
	{
		String str = (String)skillLayerList.getSelectedValue();
		int spacePos = str.indexOf(" ");
		if (spacePos >= 0) str = str.substring(0, spacePos);
		Layer layer = curTech.findLayer(str);
		if (layer == null) return;

		String layerName = skillLayerName.getText();
		skillLayers.put(layer, layerName);
		String newLine = layer.getName() + " (" + layerName + ")";
		int index = skillLayerList.getSelectedIndex();
		skillLayerModel.set(index, newLine);
	}

	/**
	 * Method called when the user clicks on a layer name in the scrollable list.
	 */
	private void skillClickLayer()
	{
		String str = (String)skillLayerList.getSelectedValue();
		int spacePos = str.indexOf(" ");
		if (spacePos >= 0) str = str.substring(0, spacePos);
		Layer layer = curTech.findLayer(str);
		if (layer == null) return;
		String shownValue = (String)skillLayers.get(layer);
		skillLayerName.setText(shownValue);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Skill tab.
	 */
	public void term()
	{
		boolean currentExcludesSubcells = skillNoSubCells.isSelected();
		if (currentExcludesSubcells != initialExcludesSubcells)
			IOTool.setSkillExcludesSubcells(currentExcludesSubcells);

		boolean currentFlattensHierarchy = skillFlattenHierarchy.isSelected();
		if (currentFlattensHierarchy != initialFlattensHierarchy)
			IOTool.setSkillFlattensHierarchy(currentFlattensHierarchy);

        boolean currentGDSNameLimit = skillGDSNameLimit.isSelected();
        if (currentGDSNameLimit != initialGDSNameLimit)
            IOTool.setSkillGDSNameLimit(currentGDSNameLimit);

		for(Iterator it = skillLayers.keySet().iterator(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			String layerName = (String)skillLayers.get(layer);
			if (!layer.getSkillLayer().equals(layerName))
			{
				layer.setSkillLayer(layerName);
			}
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        skill = new javax.swing.JPanel();
        skillLayerPane = new javax.swing.JScrollPane();
        skillLayerName = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        skillNoSubCells = new javax.swing.JCheckBox();
        skillFlattenHierarchy = new javax.swing.JCheckBox();
        skillNoSkill = new javax.swing.JLabel();
        skillTechnology = new javax.swing.JLabel();
        skillGDSNameLimit = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("IO Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        skill.setLayout(new java.awt.GridBagLayout());

        skillLayerPane.setMinimumSize(new java.awt.Dimension(150, 150));
        skillLayerPane.setPreferredSize(new java.awt.Dimension(150, 150));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        skill.add(skillLayerPane, gridBagConstraints);

        skillLayerName.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        skill.add(skillLayerName, gridBagConstraints);

        jLabel11.setText("SKILL Layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        skill.add(jLabel11, gridBagConstraints);

        skillNoSubCells.setText("Do not include subcells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        skill.add(skillNoSubCells, gridBagConstraints);

        skillFlattenHierarchy.setText("Flatten hierarchy");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        skill.add(skillFlattenHierarchy, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        skill.add(skillNoSkill, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        skill.add(skillTechnology, gridBagConstraints);

        skillGDSNameLimit.setText("GDS name limit (32 chars)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        skill.add(skillGDSNameLimit, gridBagConstraints);

        getContentPane().add(skill, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel11;
    private javax.swing.JPanel skill;
    private javax.swing.JCheckBox skillFlattenHierarchy;
    private javax.swing.JCheckBox skillGDSNameLimit;
    private javax.swing.JTextField skillLayerName;
    private javax.swing.JScrollPane skillLayerPane;
    private javax.swing.JLabel skillNoSkill;
    private javax.swing.JCheckBox skillNoSubCells;
    private javax.swing.JLabel skillTechnology;
    // End of variables declaration//GEN-END:variables

}
