/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayerVisibility.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JCheckBox;
import javax.swing.Box;


/**
 * Class to handle the "Layer Visibility" dialog.
 */
public class LayerVisibility extends EDialog
{
	private Box layerBox;
	private List layerList;
	private HashMap visibility;
	private boolean initialTextOnNode;
	private boolean initialTextOnArc;
	private boolean initialTextOnPort;
	private boolean initialTextOnExport;
	private boolean initialTextOnAnnotation;
	private boolean initialTextOnInstance;
	private boolean initialTextOnCell;
	
	/** Creates new form Layer Visibility */
	public LayerVisibility(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// initialize text visibility checkboxes
		initialTextOnNode = User.isTextVisibilityOnNode();
		nodeText.setSelected(initialTextOnNode);
		initialTextOnArc = User.isTextVisibilityOnArc();
		arcText.setSelected(initialTextOnArc);
		initialTextOnPort = User.isTextVisibilityOnPort();
		portText.setSelected(initialTextOnPort);
		initialTextOnExport = User.isTextVisibilityOnExport();
		exportText.setSelected(initialTextOnExport);
		initialTextOnAnnotation = User.isTextVisibilityOnAnnotation();
		annotationText.setSelected(initialTextOnAnnotation);
		initialTextOnInstance = User.isTextVisibilityOnInstance();
		instanceNames.setSelected(initialTextOnInstance);
		initialTextOnCell = User.isTextVisibilityOnCell();
		cellText.setSelected(initialTextOnCell);

		// cache visibility
		visibility = new HashMap();
		for(Iterator it = Technology.getTechnologiesSortedByName().iterator(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = (Layer)lIt.next();
				visibility.put(layer, new Boolean(layer.isVisible()));
			}
		}

		// make a popup of Technologies
		for(Iterator it = Technology.getTechnologiesSortedByName().iterator(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			technology.addItem(tech.getTechName());
		}
		technology.setSelectedItem(Technology.getCurrent().getTechName());
	}

	protected void escapePressed() { cancelActionPerformed(null); }

	private void termDialog()
	{
		// update visibility
		for(Iterator it = Technology.getTechnologiesSortedByName().iterator(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = (Layer)lIt.next();
				Boolean layerVis = (Boolean)visibility.get(layer);
                if (layerVis == null) continue;
				layer.setVisible(layerVis.booleanValue());
			}
		}

		boolean currentTextOnNode = nodeText.isSelected();
		if (currentTextOnNode != initialTextOnNode)
			User.setTextVisibilityOnNode(initialTextOnNode = currentTextOnNode);

		boolean currentTextOnArc = arcText.isSelected();
		if (currentTextOnArc != initialTextOnArc)
			User.setTextVisibilityOnArc(initialTextOnArc = currentTextOnArc);

		boolean currentTextOnPort = portText.isSelected();
		if (currentTextOnPort != initialTextOnPort)
			User.setTextVisibilityOnPort(initialTextOnPort = currentTextOnPort);

		boolean currentTextOnExport = exportText.isSelected();
		if (currentTextOnExport != initialTextOnExport)
			User.setTextVisibilityOnExport(initialTextOnExport = currentTextOnExport);

		boolean currentTextOnAnnotation = annotationText.isSelected();
		if (currentTextOnAnnotation != initialTextOnAnnotation)
			User.setTextVisibilityOnAnnotation(initialTextOnAnnotation = currentTextOnAnnotation);

		boolean currentTextOnInstance = instanceNames.isSelected();
		if (currentTextOnInstance != initialTextOnInstance)
			User.setTextVisibilityOnInstance(initialTextOnInstance = currentTextOnInstance);

		boolean currentTextOnCell = cellText.isSelected();
		if (currentTextOnCell != initialTextOnCell)
			User.setTextVisibilityOnCell(initialTextOnCell = currentTextOnCell);

		EditWindow.repaintAllContents();
	}

	private void showLayersForTechnology()
	{
		String techName = (String)technology.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);

		layerBox = Box.createVerticalBox();
		layerPane.setViewportView(layerBox);
		layerList = new ArrayList();
		for(Iterator it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			layerList.add(layer);
			Boolean layerVisible = (Boolean)visibility.get(layer);
			String layerName = layer.getName();
			if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) layerName += " (for pins)";
			JCheckBox cb = new JCheckBox(layerName);
			cb.setSelected(layerVisible.booleanValue());
			cb.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent evt) { changedVisibilityBox(evt); }
			});
			layerBox.add(cb);
		}
	}

	private void changedVisibilityBox(MouseEvent evt)
	{
		String techName = (String)technology.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);

		JCheckBox cb = (JCheckBox)evt.getSource();
		String name = cb.getText();
		int spacePos = name.indexOf(' ');
		if (spacePos >= 0) name = name.substring(0, spacePos);
		Layer layer = tech.findLayer(name);
		visibility.put(layer, new Boolean(cb.isSelected()));
	}

	private void setAllVisibility(boolean on)
	{
		String techName = (String)technology.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		for(Iterator it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			visibility.put(layer, new Boolean(on));
		}
		showLayersForTechnology();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        apply = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        layerPane = new javax.swing.JScrollPane();
        technology = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        nodeText = new javax.swing.JCheckBox();
        arcText = new javax.swing.JCheckBox();
        portText = new javax.swing.JCheckBox();
        exportText = new javax.swing.JCheckBox();
        annotationText = new javax.swing.JCheckBox();
        instanceNames = new javax.swing.JCheckBox();
        cellText = new javax.swing.JCheckBox();
        allVisible = new javax.swing.JButton();
        allInvisible = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        cancel = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Layer Visibility");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        apply.setText("Apply");
        apply.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                apply(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 0.5;
        getContentPane().add(apply, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ok(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(layerPane, gridBagConstraints);

        technology.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                technologyActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(technology, gridBagConstraints);

        jLabel1.setText("Technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        nodeText.setText("Node text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(nodeText, gridBagConstraints);

        arcText.setText("Arc text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(arcText, gridBagConstraints);

        portText.setText("Port text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(portText, gridBagConstraints);

        exportText.setText("Export text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(exportText, gridBagConstraints);

        annotationText.setText("Annotation text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(annotationText, gridBagConstraints);

        instanceNames.setText("Instance names");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(instanceNames, gridBagConstraints);

        cellText.setText("Cell text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(cellText, gridBagConstraints);

        allVisible.setText("All Visible");
        allVisible.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                allVisibleActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 0.5;
        getContentPane().add(allVisible, gridBagConstraints);

        allInvisible.setText("All Invisible");
        allInvisible.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                allInvisibleActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 0.5;
        getContentPane().add(allInvisible, gridBagConstraints);

        jLabel2.setText("Click to change visibility.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        getContentPane().add(jLabel2, gridBagConstraints);

        jLabel3.setText("Marked layers are visibile.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        getContentPane().add(jLabel3, gridBagConstraints);

        jLabel4.setText("Text visibility options:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel4, gridBagConstraints);

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelActionPerformed
	{//GEN-HEADEREND:event_cancelActionPerformed
		closeDialog(null);
	}//GEN-LAST:event_cancelActionPerformed

	private void allVisibleActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_allVisibleActionPerformed
	{//GEN-HEADEREND:event_allVisibleActionPerformed
		setAllVisibility(true);
	}//GEN-LAST:event_allVisibleActionPerformed

	private void allInvisibleActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_allInvisibleActionPerformed
	{//GEN-HEADEREND:event_allInvisibleActionPerformed
		setAllVisibility(false);
	}//GEN-LAST:event_allInvisibleActionPerformed

	private void technologyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_technologyActionPerformed
	{//GEN-HEADEREND:event_technologyActionPerformed
		showLayersForTechnology();
	}//GEN-LAST:event_technologyActionPerformed

	private void apply(java.awt.event.ActionEvent evt)//GEN-FIRST:event_apply
	{//GEN-HEADEREND:event_apply
		termDialog();
	}//GEN-LAST:event_apply

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		termDialog();
		closeDialog(null);
	}//GEN-LAST:event_ok

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton allInvisible;
    private javax.swing.JButton allVisible;
    private javax.swing.JCheckBox annotationText;
    private javax.swing.JButton apply;
    private javax.swing.JCheckBox arcText;
    private javax.swing.JButton cancel;
    private javax.swing.JCheckBox cellText;
    private javax.swing.JCheckBox exportText;
    private javax.swing.JCheckBox instanceNames;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane layerPane;
    private javax.swing.JCheckBox nodeText;
    private javax.swing.JButton ok;
    private javax.swing.JCheckBox portText;
    private javax.swing.JComboBox technology;
    // End of variables declaration//GEN-END:variables
	
}
