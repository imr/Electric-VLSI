/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TechnologyTab.java
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
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Frame;
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
 * Class to handle the "Technology" tab of the Preferences dialog.
 */
public class TechnologyTab extends PreferencePanel
{
	/** Creates new form TechnologyTab */
	public TechnologyTab(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return technology; }

	/** return the name of this preferences tab. */
	public String getName() { return "Technology"; }

	private JList schemPrimList;
	private DefaultListModel schemPrimModel;
	private HashMap<PrimitiveNode,String> schemPrimMap;
	private boolean changingVHDL = false;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Technology tab.
	 */
	public void init()
	{
		// Layout
		rotateLayoutTransistors.setSelected(User.isRotateLayoutTransistors());

		// Artwork
		techArtworkArrowsFilled.setSelected(Artwork.isFilledArrowHeads());

		// Schematics
		techSchematicsNegatingSize.setText(TextUtils.formatDouble(Schematics.getNegatingBubbleSize()));

		// VHDL layers list
		schemPrimModel = new DefaultListModel();
		schemPrimList = new JList(schemPrimModel);
		schemPrimList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		vhdlPrimPane.setViewportView(schemPrimList);
		schemPrimList.clearSelection();
		schemPrimList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { schemClickPrim(); }
		});
		schemPrimModel.clear();
		schemPrimMap = new HashMap<PrimitiveNode,String>();
		for(Iterator<PrimitiveNode> it = Schematics.tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = it.next();
			if (np != Schematics.tech.andNode && np != Schematics.tech.orNode &&
				np != Schematics.tech.xorNode && np != Schematics.tech.muxNode &&
				np != Schematics.tech.bufferNode) continue;
			String str = Schematics.getVHDLNames(np);
			schemPrimMap.put(np, str);
			schemPrimModel.addElement(makeLine(np, str));
		}
		schemPrimList.setSelectedIndex(0);
		vhdlName.getDocument().addDocumentListener(new SchemPrimDocumentListener(this));
		vhdlNegatedName.getDocument().addDocumentListener(new SchemPrimDocumentListener(this));
		schemClickPrim();
	}

	private String makeLine(PrimitiveNode np, String vhdlName)
	{
		return np.getName() + "  (" + vhdlName + ")";
	}

	/**
	 * Method called when the user clicks on a layer name in the scrollable list.
	 */
	private void schemClickPrim()
	{
		changingVHDL = true;
		PrimitiveNode np = getSelectedPrim();
		if (np == null) return;
		String vhdlNames = schemPrimMap.get(np);
		int slashPos = vhdlNames.indexOf('/');
		if (slashPos < 0)
		{
		    vhdlName.setText(vhdlNames);
		    vhdlNegatedName.setText("");
		} else
		{
		    vhdlName.setText(vhdlNames.substring(0, slashPos));
		    vhdlNegatedName.setText(vhdlNames.substring(slashPos+1));
		}
		changingVHDL = false;
	}

	private PrimitiveNode getSelectedPrim()
	{
		String str = (String)schemPrimList.getSelectedValue();
		int spacePos = str.indexOf(' ');
		if (spacePos >= 0) str = str.substring(0, spacePos);
		PrimitiveNode np = Schematics.tech.findNodeProto(str);
		return np;
	}

	/**
	 * Class to handle special changes to changes to a CIF layer.
	 */
	private static class SchemPrimDocumentListener implements DocumentListener
	{
		TechnologyTab dialog;

		SchemPrimDocumentListener(TechnologyTab dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.primVHDLChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.primVHDLChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.primVHDLChanged(); }
	}

	/**
	 * Method called when the user types a new VHDL into the schematics tab.
	 */
	private void primVHDLChanged()
	{
		if (changingVHDL) return;
		String str = vhdlName.getText();
		String strNot = vhdlNegatedName.getText();
		String vhdl = "";
		if (str.length() > 0 || strNot.length() > 0) vhdl = str + "/" + strNot;
		PrimitiveNode np = getSelectedPrim();
		if (np == null) return;
		schemPrimMap.put(np, vhdl);

		int index = schemPrimList.getSelectedIndex();
		schemPrimModel.set(index, makeLine(np, vhdl));
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Technology tab.
	 */
	public void term()
	{
		boolean redrawWindows = false;
		boolean redrawMenus = false;

		// Layout
		boolean currentRotateTransistors = rotateLayoutTransistors.isSelected();
		if (currentRotateTransistors != User.isRotateLayoutTransistors())
		{
			User.setRotateLayoutTransistors(currentRotateTransistors);
			redrawMenus = true;
		}

		// Artwork
		boolean currentArrowsFilled = techArtworkArrowsFilled.isSelected();
		if (currentArrowsFilled != Artwork.isFilledArrowHeads())
		{
			Artwork.setFilledArrowHeads(currentArrowsFilled);
			redrawWindows = true;
		}

		// Schematics
		double currentNegatingBubbleSize = TextUtils.atof(techSchematicsNegatingSize.getText());
		if (currentNegatingBubbleSize != Schematics.getNegatingBubbleSize())
		{
			Schematics.setNegatingBubbleSize(currentNegatingBubbleSize);
			redrawWindows = true;
		}

		// updating VHDL names
		for(int i=0; i<schemPrimModel.size(); i++)
		{
			String str = (String)schemPrimModel.get(i);
			int spacePos = str.indexOf(' ');
			if (spacePos < 0) continue;
			String primName = str.substring(0, spacePos);
			PrimitiveNode np = Schematics.tech.findNodeProto(primName);
			if (np == null) continue;
			String newVHDLname = str.substring(spacePos+3, str.length()-1);
			String oldVHDLname = Schematics.getVHDLNames(np);
			if (!newVHDLname.equals(oldVHDLname))
				Schematics.setVHDLNames(np, newVHDLname);
		}

		// update the display
		if (redrawMenus)
		{
			Technology tech = Technology.getCurrent();
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				wf.getPaletteTab().loadForTechnology(tech, wf);
			}
		}
		if (redrawWindows)
		{
			EditWindow.repaintAllContents();
		}
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

        techMOCMOSRules = new javax.swing.ButtonGroup();
        technology = new javax.swing.JPanel();
        artworkPanel = new javax.swing.JPanel();
        techArtworkArrowsFilled = new javax.swing.JCheckBox();
        schematicsPanel = new javax.swing.JPanel();
        techSchematicsNegatingSize = new javax.swing.JTextField();
        jLabel52 = new javax.swing.JLabel();
        vhdlPrimPane = new javax.swing.JScrollPane();
        jLabel1 = new javax.swing.JLabel();
        vhdlName = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        vhdlNegatedName = new javax.swing.JTextField();
        layoutPanel = new javax.swing.JPanel();
        rotateLayoutTransistors = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        technology.setLayout(new java.awt.GridBagLayout());

        artworkPanel.setLayout(new java.awt.GridBagLayout());

        artworkPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Artwork"));
        techArtworkArrowsFilled.setText("Arrows filled");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        artworkPanel.add(techArtworkArrowsFilled, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        technology.add(artworkPanel, gridBagConstraints);

        schematicsPanel.setLayout(new java.awt.GridBagLayout());

        schematicsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Schematics"));
        techSchematicsNegatingSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        schematicsPanel.add(techSchematicsNegatingSize, gridBagConstraints);

        jLabel52.setText("Negating Bubble Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        schematicsPanel.add(jLabel52, gridBagConstraints);

        vhdlPrimPane.setMinimumSize(new java.awt.Dimension(22, 100));
        vhdlPrimPane.setPreferredSize(new java.awt.Dimension(22, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        schematicsPanel.add(vhdlPrimPane, gridBagConstraints);

        jLabel1.setText("VHDL for primitive:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        schematicsPanel.add(jLabel1, gridBagConstraints);

        vhdlName.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        schematicsPanel.add(vhdlName, gridBagConstraints);

        jLabel2.setText("VHDL for negated primitive:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        schematicsPanel.add(jLabel2, gridBagConstraints);

        vhdlNegatedName.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        schematicsPanel.add(vhdlNegatedName, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        technology.add(schematicsPanel, gridBagConstraints);

        layoutPanel.setLayout(new java.awt.GridBagLayout());

        layoutPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Layout Technologies"));
        rotateLayoutTransistors.setText("Rotate transistors in menu");
        rotateLayoutTransistors.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        rotateLayoutTransistors.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        layoutPanel.add(rotateLayoutTransistors, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        technology.add(layoutPanel, gridBagConstraints);

        getContentPane().add(technology, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel artworkPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JPanel layoutPanel;
    private javax.swing.JCheckBox rotateLayoutTransistors;
    private javax.swing.JPanel schematicsPanel;
    private javax.swing.JCheckBox techArtworkArrowsFilled;
    private javax.swing.ButtonGroup techMOCMOSRules;
    private javax.swing.JTextField techSchematicsNegatingSize;
    private javax.swing.JPanel technology;
    private javax.swing.JTextField vhdlName;
    private javax.swing.JTextField vhdlNegatedName;
    private javax.swing.JScrollPane vhdlPrimPane;
    // End of variables declaration//GEN-END:variables

}
