/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TechnologyTab.java
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

import com.sun.electric.technology.Foundry;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Frame;
import java.util.Iterator;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Class to handle the "Technology" tab of the Project Settings dialog.
 */
public class TechnologyTab extends ProjSettingsPanel
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

    public static Foundry.Type setFoundrySelected(Technology tech, javax.swing.JComboBox pulldown)
    {
        String selectedFoundry = tech.getPrefFoundry();
    	Foundry.Type foundry = Foundry.Type.NONE;

        for (Iterator<Foundry> itF = tech.getFoundries(); itF.hasNext();)
        {
            Foundry factory = itF.next();
            Foundry.Type type = factory.getType();
            pulldown.addItem(type);
            if (selectedFoundry.equalsIgnoreCase(factory.getType().name())) foundry = type;
        }

//        double scale = tech.getScale();
//        boolean correctScale = true; // CMOS90 -> 50 and MoCMOS -> 200
//
//        if (tech == MoCMOS.tech)
//            correctScale = DBMath.areEquals(scale, 100);
//
//        if (correctScale)
//        {
//            for (Iterator<Foundry> itF = tech.getFoundries(); itF.hasNext();)
//            {
//                Foundry factory = itF.next();
//                Foundry.Type type = factory.getType();
//                pulldown.addItem(type);
//                if (selectedFoundry.equalsIgnoreCase(factory.getType().name())) foundry = type;
//            }
//        }
//        else
//        {
//            foundry = Foundry.Type.MOSIS; // forces mosis for 180nm or larger.
//            pulldown.addItem(foundry);
//            tech.setPrefFoundry(foundry.name());
//        }

        pulldown.setEnabled(foundry != Foundry.Type.NONE);
        pulldown.setSelectedItem(foundry);
        return foundry;
    }

    /**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Technology tab.
	 */
	public void init()
	{
		// MOCMOS
		int initialTechRules = MoCMOS.getRuleSet();
		if (initialTechRules == MoCMOS.SCMOSRULES) techMOCMOSSCMOSRules.setSelected(true); else
			if (initialTechRules == MoCMOS.SUBMRULES) techMOCMOSSubmicronRules.setSelected(true); else
				techMOCMOSDeepRules.setSelected(true);

		techMetalLayers.addItem("2 Layers");
		techMetalLayers.addItem("3 Layers");
		techMetalLayers.addItem("4 Layers");
		techMetalLayers.addItem("5 Layers");
		techMetalLayers.addItem("6 Layers");
		techMetalLayers.setSelectedIndex(MoCMOS.getNumMetal()-2);
		techMOCMOSSecondPoly.setSelected(MoCMOS.isSecondPolysilicon());
		techMOCMOSDisallowStackedVias.setSelected(MoCMOS.isDisallowStackedVias());
		techMOCMOSAlternateContactRules.setSelected(MoCMOS.isAlternateActivePolyRules());

		// Technologies
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			defaultTechPulldown.addItem(tech.getTechName());
			if (tech.isScaleRelevant()) technologyPopup.addItem(tech.getTechName());
		}
        defaultTechPulldown.setSelectedItem(User.getDefaultTechnology());
		technologyPopup.setSelectedItem(User.getSchematicTechnology().getTechName());

        // Foundry for MoCMOS
        setFoundrySelected(MoCMOS.tech, techDefaultFoundryPulldown);

        // Tabs for extra technologies if available
        jPanel3.remove(cmos90Panel);
        try
        {
            Class extraTechClass = Class.forName("com.sun.electric.plugins.tsmc.CMOS90Tab");
            extraTechClass.getMethod("openTechnologyTab", JPanel.class).invoke(null, jPanel3);
        } catch (Exception e)
        {
            System.out.println("Exceptions while importing extra technologies");
        }

//        mosisPanel.setVisible(Technology.getCurrent() == MoCMOS.tech);
//        double scale = MoCMOS.tech.getScale();
        String extra = " Mosis";
//        if (DBMath.areEquals(scale, 100)) // 180nm
//            extra = ": 180nm";
//        else if (DBMath.areEquals(scale, 200)) // 180nm
//            extra = ": 350nm";

        mosisPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Scalable CMOS" + extra));
    }

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Technology tab.
	 */
	public void term()
	{
        // MOCMOS
        boolean changeInMoCMOS = false; // to determine if tech.setState() will be called
        int currentNumMetals = techMetalLayers.getSelectedIndex() + 2;
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

		if (currentNumMetals != MoCMOS.getNumMetal())
        {
            MoCMOS.setNumMetal(currentNumMetals);
            changeInMoCMOS = true;
        }
        if (currentRules != MoCMOS.getRuleSet())
        {
            MoCMOS.setRuleSet(currentRules);
            changeInMoCMOS = true;
        }

		boolean newVal = techMOCMOSSecondPoly.isSelected();
		if (newVal != MoCMOS.isSecondPolysilicon())
        {
            MoCMOS.setSecondPolysilicon(newVal);
            changeInMoCMOS = true;
        }

		newVal = techMOCMOSDisallowStackedVias.isSelected();
		if (newVal != MoCMOS.isDisallowStackedVias())
        {
            MoCMOS.setDisallowStackedVias(newVal);
            changeInMoCMOS = true;
        }

		newVal = techMOCMOSAlternateContactRules.isSelected();
		if (newVal != MoCMOS.isAlternateActivePolyRules())
        {
            MoCMOS.setAlternateActivePolyRules(newVal);
            changeInMoCMOS = true;
        }

		// Technologies
		String currentTechName = (String)technologyPopup.getSelectedItem();
		Technology currentTech = Technology.findTechnology(currentTechName);
		if (currentTech != User.getSchematicTechnology() && currentTech != null)
			User.setSchematicTechnology(currentTech);

        String defaultTech = (String)defaultTechPulldown.getSelectedItem();
		if (!defaultTech.equals(User.getDefaultTechnology()))
			User.setDefaultTechnology(defaultTech);

        // Foundry for MoCMOS
        if (changeInMoCMOS || checkFoundry((Foundry.Type)techDefaultFoundryPulldown.getSelectedItem(), MoCMOS.tech))
            Technology.TechPref.technologyChanged(MoCMOS.tech, true);  // including update of display

        // Tabs for extra technologies if available
        try
        {
            Class extraTechClass = Class.forName("com.sun.electric.plugins.tsmc.CMOS90Tab");
            extraTechClass.getMethod("closeTechnologyTab").invoke(null);
        } catch (Exception e)
        {
            System.out.println("Exceptions while importing extra technologies: " + e.getMessage());
        }
	}

    /**
     * Method to check whether the foundry value has been changed. If changed, primitives might need resizing.
     * @param foundry
     * @param tech
     */
    public static boolean checkFoundry(Foundry.Type foundry, Technology tech)
    {
        if (foundry == null) return false; // technology without design rules.
        boolean changed = false;

        if (!foundry.name().equalsIgnoreCase(tech.getPrefFoundry()))
        {
            changed = true;
            String [] messages = {
                tech.getTechShortName()+" primitives in database might be resized according to values provided by " + foundry + ".",
                "If you do not resize now, arc widths might not be optimal for " + foundry + ".",
                "If you cancel the operation, the foundry will not be changed.",
                "Do you want to resize the database?"};
            Object [] options = {"Yes", "No", "Cancel"};
            int val = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(), messages,
                "Resize primitive Nodes and Arcs", JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            if (val != 2)
            {
                boolean modifyNodes = val == 0; // user wants to resize the nodes
                tech.setPrefFoundryAndResize(foundry.name(), modifyNodes);
                changed = false; // rules set already updated as side effect of changing the foundry name
            }
        }
        return changed;
    }

    /** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        techMOCMOSRules = new javax.swing.ButtonGroup();
        technology = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        defaultTechLabel = new javax.swing.JLabel();
        defaultTechPulldown = new javax.swing.JComboBox();
        jLabel59 = new javax.swing.JLabel();
        technologyPopup = new javax.swing.JComboBox();
        mosisPanel = new javax.swing.JPanel();
        techMetalLabel = new javax.swing.JLabel();
        techMetalLayers = new javax.swing.JComboBox();
        techMOCMOSSCMOSRules = new javax.swing.JRadioButton();
        techMOCMOSSubmicronRules = new javax.swing.JRadioButton();
        techMOCMOSDeepRules = new javax.swing.JRadioButton();
        techMOCMOSSecondPoly = new javax.swing.JCheckBox();
        techMOCMOSDisallowStackedVias = new javax.swing.JCheckBox();
        techMOCMOSAlternateContactRules = new javax.swing.JCheckBox();
        techDefaultFoundryLabel = new javax.swing.JLabel();
        techDefaultFoundryPulldown = new javax.swing.JComboBox();
        jPanel3 = new javax.swing.JPanel();
        cmos90Panel = new javax.swing.JPanel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        technology.setLayout(new java.awt.GridBagLayout());

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Defaults"));
        defaultTechLabel.setText("Startup technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel2.add(defaultTechLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel2.add(defaultTechPulldown, gridBagConstraints);

        jLabel59.setText("Layout technology to use for Schematics:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel2.add(jLabel59, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel2.add(technologyPopup, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        technology.add(jPanel2, gridBagConstraints);

        mosisPanel.setLayout(new java.awt.GridBagLayout());

        mosisPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Scalable CMOS"));
        techMetalLabel.setText("Metal layers:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        mosisPanel.add(techMetalLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        mosisPanel.add(techMetalLayers, gridBagConstraints);

        techMOCMOSRules.add(techMOCMOSSCMOSRules);
        techMOCMOSSCMOSRules.setText("SCMOS rules (4 metal or less)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        mosisPanel.add(techMOCMOSSCMOSRules, gridBagConstraints);

        techMOCMOSRules.add(techMOCMOSSubmicronRules);
        techMOCMOSSubmicronRules.setText("Submicron rules");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        mosisPanel.add(techMOCMOSSubmicronRules, gridBagConstraints);

        techMOCMOSRules.add(techMOCMOSDeepRules);
        techMOCMOSDeepRules.setText("Deep rules (5 metal or more)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        mosisPanel.add(techMOCMOSDeepRules, gridBagConstraints);

        techMOCMOSSecondPoly.setText("Second Polysilicon Layer");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        mosisPanel.add(techMOCMOSSecondPoly, gridBagConstraints);

        techMOCMOSDisallowStackedVias.setText("Disallow stacked vias");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        mosisPanel.add(techMOCMOSDisallowStackedVias, gridBagConstraints);

        techMOCMOSAlternateContactRules.setText("Alternate Active and Poly contact rules");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        mosisPanel.add(techMOCMOSAlternateContactRules, gridBagConstraints);

        techDefaultFoundryLabel.setText("Current foundry:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        mosisPanel.add(techDefaultFoundryLabel, gridBagConstraints);

        techDefaultFoundryPulldown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                techDefaultFoundryPulldownActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        mosisPanel.add(techDefaultFoundryPulldown, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        technology.add(mosisPanel, gridBagConstraints);

        cmos90Panel.setLayout(new java.awt.GridBagLayout());

        cmos90Panel.setBorder(javax.swing.BorderFactory.createTitledBorder("CMOS90"));
        jPanel3.add(cmos90Panel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        technology.add(jPanel3, gridBagConstraints);

        getContentPane().add(technology, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void techDefaultFoundryPulldownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_techDefaultFoundryPulldownActionPerformed
        Foundry.Type foundry = (Foundry.Type)techDefaultFoundryPulldown.getSelectedItem();
        boolean legacyRules = foundry == Foundry.Type.MOSIS;
        techMOCMOSSCMOSRules.setEnabled(legacyRules);
        techMOCMOSSecondPoly.setEnabled(legacyRules);
        techMOCMOSSubmicronRules.setEnabled(legacyRules);
        techMOCMOSDisallowStackedVias.setEnabled(legacyRules);
        techMOCMOSDeepRules.setEnabled(legacyRules);
        techMOCMOSAlternateContactRules.setEnabled(legacyRules);
    }//GEN-LAST:event_techDefaultFoundryPulldownActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel defaultTechLabel;
    private javax.swing.JComboBox defaultTechPulldown;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel mosisPanel;
    private javax.swing.JLabel techDefaultFoundryLabel;
    private javax.swing.JComboBox techDefaultFoundryPulldown;
    private javax.swing.JCheckBox techMOCMOSAlternateContactRules;
    private javax.swing.JRadioButton techMOCMOSDeepRules;
    private javax.swing.JCheckBox techMOCMOSDisallowStackedVias;
    private javax.swing.ButtonGroup techMOCMOSRules;
    private javax.swing.JRadioButton techMOCMOSSCMOSRules;
    private javax.swing.JCheckBox techMOCMOSSecondPoly;
    private javax.swing.JRadioButton techMOCMOSSubmicronRules;
    private javax.swing.JLabel techMetalLabel;
    private javax.swing.JComboBox techMetalLayers;
    private javax.swing.JPanel technology;
    private javax.swing.JComboBox technologyPopup;
    private javax.swing.JPanel cmos90Panel;
    // End of variables declaration//GEN-END:variables

}
