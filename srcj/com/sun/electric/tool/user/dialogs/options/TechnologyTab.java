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
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.HashMap;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
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
	public TechnologyTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	public JPanel getPanel() { return technology; }

	public String getName() { return "Technology"; }

	private int initialTechRules;
	private int initialTechNumMetalLayers;
	private boolean initialTechSecondPolyLayers;
	private String initialSchematicTechnology;
    private String initialDefaultTechnology;
	private boolean initialTechNoStackedVias;
	private boolean initialTechAlternateContactRules;
	private boolean initialTechSpecialTransistors;
	private boolean initialTechArtworkArrowsFilled;
	private double initialTechNegatingBubbleSize;
	private JList schemPrimList;
	private DefaultListModel schemPrimModel;
	private HashMap schemPrimMap;
	private boolean changingVHDL = false;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Technology tab.
	 */
	public void init()
	{
		// MOCMOS
		initialTechRules = MoCMOS.getRuleSet();
		if (initialTechRules == MoCMOS.SCMOSRULES) techMOCMOSSCMOSRules.setSelected(true); else
			if (initialTechRules == MoCMOS.SUBMRULES) techMOCMOSSubmicronRules.setSelected(true); else
				techMOCMOSDeepRules.setSelected(true);

		techMOCMOSMetalLayers.addItem("2 Layers");
		techMOCMOSMetalLayers.addItem("3 Layers");
		techMOCMOSMetalLayers.addItem("4 Layers");
		techMOCMOSMetalLayers.addItem("5 Layers");
		techMOCMOSMetalLayers.addItem("6 Layers");
		initialTechNumMetalLayers = MoCMOS.getNumMetal();
		techMOCMOSMetalLayers.setSelectedIndex(initialTechNumMetalLayers-2);

		initialTechSecondPolyLayers = MoCMOS.isSecondPolysilicon();
		techMOCMOSSecondPoly.setSelected(initialTechSecondPolyLayers);

		initialTechNoStackedVias = MoCMOS.isDisallowStackedVias();
		techMOCMOSDisallowStackedVias.setSelected(initialTechNoStackedVias);

		initialTechAlternateContactRules = MoCMOS.isAlternateActivePolyRules();
		techMOCMOSAlternateContactRules.setSelected(initialTechAlternateContactRules);

		initialTechSpecialTransistors = MoCMOS.isSpecialTransistors();
		techMOCMOSShowSpecialTrans.setSelected(initialTechSpecialTransistors);

		// Artwork
		initialTechArtworkArrowsFilled = Artwork.isFilledArrowHeads();
		techArtworkArrowsFilled.setSelected(initialTechArtworkArrowsFilled);

		// Schematics
		initialSchematicTechnology = User.getSchematicTechnology();
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			technologyPopup.addItem(tech.getTechName());
            defaultTechPulldown.addItem(tech.getTechName());
		}
		technologyPopup.setSelectedItem(initialSchematicTechnology);

		// build the layers list
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
		schemPrimMap = new HashMap();
		for(Iterator it = Schematics.tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
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

		// Default technology
        initialDefaultTechnology = User.getDefaultTechnology();
        defaultTechPulldown.setSelectedItem(initialDefaultTechnology);

		initialTechNegatingBubbleSize = Schematics.getNegatingBubbleSize();
		techSchematicsNegatingSize.setText(TextUtils.formatDouble(initialTechNegatingBubbleSize));
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
		String vhdlNames = (String)schemPrimMap.get(np);
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
		boolean redrawPalette = false;
		boolean redrawWindows = false;

		// MOCMOS
		int currentNumMetals = techMOCMOSMetalLayers.getSelectedIndex() + 2;
		int currentRules = MoCMOS.SCMOSRULES;
		if (techMOCMOSSubmicronRules.isSelected()) currentRules = MoCMOS.SUBMRULES; else
			if (techMOCMOSDeepRules.isSelected()) currentRules = MoCMOS.DEEPRULES;

		switch (currentNumMetals)
		{
			// cannot use deep rules if less than 5 layers of metal
			case 2:
			case 3:
			case 4:
				if (currentRules == MoCMOS.DEEPRULES)
				{
					JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
						"Cannot use Deep rules if there are less than 5 layers of metal...using SubMicron rules.");
					currentRules = MoCMOS.SUBMRULES;
				}
				break;

			// cannot use scmos rules if more than 4 layers of metal
			case 5:
			case 6:
				if (currentRules == MoCMOS.SCMOSRULES)
				{
					JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
						"Cannot use SCMOS rules if there are more than 4 layers of metal...using SubMicron rules.");
					currentRules = MoCMOS.SUBMRULES;
				}
				break;
		}

		if (currentNumMetals != initialTechNumMetalLayers)
			MoCMOS.setNumMetal(currentNumMetals);
		if (currentRules != initialTechRules)
			MoCMOS.setRuleSet(currentRules);

		boolean currentSecondPolys = techMOCMOSSecondPoly.isSelected();
		if (currentSecondPolys != initialTechSecondPolyLayers)
			MoCMOS.setSecondPolysilicon(currentSecondPolys);

		boolean currentNoStackedVias = techMOCMOSDisallowStackedVias.isSelected();
		if (currentNoStackedVias != initialTechNoStackedVias)
			MoCMOS.setDisallowStackedVias(currentNoStackedVias);

		boolean currentAlternateContact = techMOCMOSAlternateContactRules.isSelected();
		if (currentAlternateContact != initialTechAlternateContactRules)
			MoCMOS.setAlternateActivePolyRules(currentAlternateContact);

		boolean currentSpecialTransistors = techMOCMOSShowSpecialTrans.isSelected();
		if (currentSpecialTransistors != initialTechSpecialTransistors)
		{
			MoCMOS.setSpecialTransistors(currentSpecialTransistors);
			redrawPalette = true;
		}

		// Artwork
		boolean currentArrowsFilled = techArtworkArrowsFilled.isSelected();
		if (currentArrowsFilled != initialTechArtworkArrowsFilled)
		{
			Artwork.setFilledArrowHeads(currentArrowsFilled);
			redrawWindows = true;
		}

		// Schematics
		String currentTech = (String)technologyPopup.getSelectedItem();
		if (!currentTech.equals(initialSchematicTechnology))
			User.setSchematicTechnology(currentTech);

        // Getting default tech
        String defaultTech = (String)defaultTechPulldown.getSelectedItem();
		if (!defaultTech.equals(initialDefaultTechnology))
			User.setDefaultTechnology(defaultTech);

		double currentNegatingBubbleSize = TextUtils.atof(techSchematicsNegatingSize.getText());
		if (currentNegatingBubbleSize != initialTechNegatingBubbleSize)
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
		if (redrawPalette)
		{
			WindowFrame wf = WindowFrame.getCurrentWindowFrame(false);
			if (wf != null) wf.loadComponentMenuForTechnology();
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
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        techMOCMOSRules = new javax.swing.ButtonGroup();
        technology = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        defaultTechLabel = new javax.swing.JLabel();
        defaultTechPulldown = new javax.swing.JComboBox();
        jPanel1 = new javax.swing.JPanel();
        jLabel49 = new javax.swing.JLabel();
        techMOCMOSMetalLayers = new javax.swing.JComboBox();
        techMOCMOSSCMOSRules = new javax.swing.JRadioButton();
        techMOCMOSSubmicronRules = new javax.swing.JRadioButton();
        techMOCMOSDeepRules = new javax.swing.JRadioButton();
        techMOCMOSSecondPoly = new javax.swing.JCheckBox();
        techMOCMOSDisallowStackedVias = new javax.swing.JCheckBox();
        techMOCMOSAlternateContactRules = new javax.swing.JCheckBox();
        techMOCMOSShowSpecialTrans = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        techArtworkArrowsFilled = new javax.swing.JCheckBox();
        jPanel10 = new javax.swing.JPanel();
        techSchematicsNegatingSize = new javax.swing.JTextField();
        jLabel52 = new javax.swing.JLabel();
        jLabel59 = new javax.swing.JLabel();
        technologyPopup = new javax.swing.JComboBox();
        vhdlPrimPane = new javax.swing.JScrollPane();
        jLabel1 = new javax.swing.JLabel();
        vhdlName = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        vhdlNegatedName = new javax.swing.JTextField();

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

        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.Y_AXIS));

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel2.setBorder(new javax.swing.border.TitledBorder("Default Technology"));
        defaultTechLabel.setText("Technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(defaultTechLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(defaultTechPulldown, gridBagConstraints);

        jPanel3.add(jPanel2);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(new javax.swing.border.TitledBorder("MOSIS CMOS"));
        jLabel49.setText("Metal layers:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLabel49, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(techMOCMOSMetalLayers, gridBagConstraints);

        techMOCMOSSCMOSRules.setText("SCMOS rules (4 metal or less)");
        techMOCMOSRules.add(techMOCMOSSCMOSRules);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel1.add(techMOCMOSSCMOSRules, gridBagConstraints);

        techMOCMOSSubmicronRules.setText("Submicron rules");
        techMOCMOSRules.add(techMOCMOSSubmicronRules);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel1.add(techMOCMOSSubmicronRules, gridBagConstraints);

        techMOCMOSDeepRules.setText("Deep rules (5 metal or more)");
        techMOCMOSRules.add(techMOCMOSDeepRules);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel1.add(techMOCMOSDeepRules, gridBagConstraints);

        techMOCMOSSecondPoly.setText("Second Polysilicon Layer");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(techMOCMOSSecondPoly, gridBagConstraints);

        techMOCMOSDisallowStackedVias.setText("Disallow stacked vias");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(techMOCMOSDisallowStackedVias, gridBagConstraints);

        techMOCMOSAlternateContactRules.setText("Alternate Active and Poly contact rules");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(techMOCMOSAlternateContactRules, gridBagConstraints);

        techMOCMOSShowSpecialTrans.setText("Show Special transistors");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(techMOCMOSShowSpecialTrans, gridBagConstraints);

        jPanel3.add(jPanel1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        technology.add(jPanel3, gridBagConstraints);

        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.Y_AXIS));

        jPanel9.setLayout(new java.awt.GridBagLayout());

        jPanel9.setBorder(new javax.swing.border.TitledBorder("Artwork"));
        techArtworkArrowsFilled.setText("Arrows filled");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel9.add(techArtworkArrowsFilled, gridBagConstraints);

        jPanel4.add(jPanel9);

        jPanel10.setLayout(new java.awt.GridBagLayout());

        jPanel10.setBorder(new javax.swing.border.TitledBorder("Schematics"));
        techSchematicsNegatingSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel10.add(techSchematicsNegatingSize, gridBagConstraints);

        jLabel52.setText("Negating Bubble Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel10.add(jLabel52, gridBagConstraints);

        jLabel59.setText("Use scale values from this technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel10.add(jLabel59, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel10.add(technologyPopup, gridBagConstraints);

        vhdlPrimPane.setMinimumSize(new java.awt.Dimension(22, 100));
        vhdlPrimPane.setPreferredSize(new java.awt.Dimension(22, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel10.add(vhdlPrimPane, gridBagConstraints);

        jLabel1.setText("VHDL for primitive:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel10.add(jLabel1, gridBagConstraints);

        vhdlName.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel10.add(vhdlName, gridBagConstraints);

        jLabel2.setText("VHDL for negated primitive:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel10.add(jLabel2, gridBagConstraints);

        vhdlNegatedName.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel10.add(vhdlNegatedName, gridBagConstraints);

        jPanel4.add(jPanel10);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        technology.add(jPanel4, gridBagConstraints);

        getContentPane().add(technology, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel defaultTechLabel;
    private javax.swing.JComboBox defaultTechPulldown;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JCheckBox techArtworkArrowsFilled;
    private javax.swing.JCheckBox techMOCMOSAlternateContactRules;
    private javax.swing.JRadioButton techMOCMOSDeepRules;
    private javax.swing.JCheckBox techMOCMOSDisallowStackedVias;
    private javax.swing.JComboBox techMOCMOSMetalLayers;
    private javax.swing.ButtonGroup techMOCMOSRules;
    private javax.swing.JRadioButton techMOCMOSSCMOSRules;
    private javax.swing.JCheckBox techMOCMOSSecondPoly;
    private javax.swing.JCheckBox techMOCMOSShowSpecialTrans;
    private javax.swing.JRadioButton techMOCMOSSubmicronRules;
    private javax.swing.JTextField techSchematicsNegatingSize;
    private javax.swing.JPanel technology;
    private javax.swing.JComboBox technologyPopup;
    private javax.swing.JTextField vhdlName;
    private javax.swing.JTextField vhdlNegatedName;
    private javax.swing.JScrollPane vhdlPrimPane;
    // End of variables declaration//GEN-END:variables

}
