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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.text.Setting;
import com.sun.electric.technology.Foundry;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.io.output.GenerateVHDL;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
	/** Value for standard SCMOS rules. */		public static final int MOCMOS_SCMOSRULES = 0; // = MoCMOS.SCMOSRULES
	/** Value for submicron rules. */			public static final int MOCMOS_SUBMRULES  = 1; // = MoCMOS.SUBMRULES
	/** Value for deep rules. */				public static final int MOCMOS_DEEPRULES  = 2; // = MoCMOS.DEEPRULES

	private ArrayList<Object> extraTechTabs = new ArrayList<Object>();
	private JList schemPrimList;
	private DefaultListModel schemPrimModel;
	private Map<PrimitiveNode,String> schemPrimMap;
	private boolean changingVHDL = false;

	private Setting defaultTechnologySetting = User.getDefaultTechnologySetting();
	private Setting schematicTechnologySetting = User.getSchematicTechnologySetting();
    private Setting processLayoutTechnologySetting = User.getPSubstrateProcessLayoutTechnologySetting();

    private Technology mocmos = Technology.getMocmosTechnology();
	private Setting mocmosRuleSetSetting                  = mocmos.getSetting("MOCMOS Rule Set");
	private Setting mocmosNumMetalSetting                 = mocmos.getSetting("NumMetalLayers");
	private Setting mocmosSecondPolysiliconSetting        = mocmos.getSetting("UseSecondPolysilicon");
	private Setting mocmosDisallowStackedViasSetting      = mocmos.getSetting("DisallowStackedVias");
	private Setting mocmosAlternateActivePolyRulesSetting = mocmos.getSetting("UseAlternativeActivePolyRules");
	private Setting mocmosAnalogSetting                   = mocmos.getSetting("Analog");

	/** Creates new form TechnologyTab */
	public TechnologyTab(PreferencesFrame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// make all text fields select-all when entered
	    EDialog.makeTextFieldSelectAllOnTab(vhdlName);
	    EDialog.makeTextFieldSelectAllOnTab(vhdlNegatedName);
	}

	/** return the JPanel to use for the user preferences. */
	public JPanel getUserPreferencesPanel() { return preferences; }

	/** return the JPanel to use for the project preferences. */
	public JPanel getProjectPreferencesPanel() { return projectSettings; }

	/** return the name of this preferences tab. */
	public String getName() { return "Technology"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Technology tab.
	 */
	public void init()
	{
		// Layout
		rotateLayoutTransistors.setSelected(User.isRotateLayoutTransistors());

		// VHDL layers list in Schematics
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
        GenerateVHDL.VHDLPreferences vp = new GenerateVHDL.VHDLPreferences(false);
		for(Iterator<PrimitiveNode> it = Schematics.tech().getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = it.next();
			if (np != Schematics.tech().andNode && np != Schematics.tech().orNode &&
				np != Schematics.tech().xorNode && np != Schematics.tech().muxNode &&
				np != Schematics.tech().bufferNode) continue;
			String str = vp.vhdlNames.get(np);
			schemPrimMap.put(np, str);
			schemPrimModel.addElement(makeLine(np, str));
		}
		schemPrimList.setSelectedIndex(0);
		vhdlName.getDocument().addDocumentListener(new SchemPrimDocumentListener(this));
		vhdlNegatedName.getDocument().addDocumentListener(new SchemPrimDocumentListener(this));
		schemClickPrim();

		// MOCMOS Project preferences
		int initialTechRules = getInt(mocmosRuleSetSetting);
		if (initialTechRules == MOCMOS_SCMOSRULES) techMOCMOSSCMOSRules.setSelected(true); else
			if (initialTechRules == MOCMOS_SUBMRULES) techMOCMOSSubmicronRules.setSelected(true); else
				techMOCMOSDeepRules.setSelected(true);

		techMetalLayers.addItem("2 Layers");
		techMetalLayers.addItem("3 Layers");
		techMetalLayers.addItem("4 Layers");
		techMetalLayers.addItem("5 Layers");
		techMetalLayers.addItem("6 Layers");
		techMetalLayers.setSelectedIndex(getInt(mocmosNumMetalSetting)-2);
		techMOCMOSSecondPoly.setSelected(getBoolean(mocmosSecondPolysiliconSetting));
		techMOCMOSDisallowStackedVias.setSelected(getBoolean(mocmosDisallowStackedViasSetting));
		techMOCMOSAlternateContactRules.setSelected(getBoolean(mocmosAlternateActivePolyRulesSetting));
		techMOCMOSAnalog.setSelected(getBoolean(mocmosAnalogSetting));

		// Technology Project preferences
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			defaultTechPulldown.addItem(tech.getTechName());
			if (tech.isScaleRelevant()) technologyPopup.addItem(tech.getTechName());
		}
		defaultTechPulldown.setSelectedItem(getString(defaultTechnologySetting));
		technologyPopup.setSelectedItem(User.getSchematicTechnology().getTechName());
        technologyProcess.setSelected(processLayoutTechnologySetting.getBoolean());

        // Tabs for extra technologies if available
		initExtraTab("com.sun.electric.plugins.tsmc.CMOS90Tab", cmos90Panel);
		initExtraTab("com.sun.electric.plugins.tsmc.TSMC180Tab", tsmc180Panel);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Technology tab.
	 */
	public void term()
	{
		boolean redrawWindows = false;
		boolean redrawMenus = false;

		// Layout Preferences
		boolean currentRotateTransistors = rotateLayoutTransistors.isSelected();
		if (currentRotateTransistors != User.isRotateLayoutTransistors())
		{
			User.setRotateLayoutTransistors(currentRotateTransistors);
			redrawMenus = true;
		}

		// VHDL name Preferences
        GenerateVHDL.VHDLPreferences vp = new GenerateVHDL.VHDLPreferences(false);
		for(int i=0; i<schemPrimModel.size(); i++)
		{
			String str = (String)schemPrimModel.get(i);
			int spacePos = str.indexOf(' ');
			if (spacePos < 0) continue;
			String primName = str.substring(0, spacePos);
			PrimitiveNode np = Schematics.tech().findNodeProto(primName);
			if (np == null) continue;
			String newVHDLname = str.substring(spacePos+3, str.length()-1);
            vp.vhdlNames.put(np, newVHDLname);
		}
        putPrefs(vp);

		// MOCMOS Project preferences
		int currentNumMetals = techMetalLayers.getSelectedIndex() + 2;
		int currentRules = MOCMOS_SCMOSRULES;
		if (techMOCMOSSubmicronRules.isSelected()) currentRules = MOCMOS_SUBMRULES; else
			if (techMOCMOSDeepRules.isSelected()) currentRules = MOCMOS_DEEPRULES;
		boolean secondPoly = techMOCMOSSecondPoly.isSelected();
		boolean alternateContactRules = techMOCMOSAlternateContactRules.isSelected();
		if (techMOCMOSAnalog.isSelected())
		{
			// analog rules don't demand 2 metals, SCMOS  (Dec 09)
			// Keeping the 2 poly, no stacked vias anymore though
			if (/*currentNumMetals != 2 || currentRules != MOCMOS_SCMOSRULES ||*/
                currentRules == MOCMOS_DEEPRULES || !secondPoly || alternateContactRules)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"The Analog setting requires 2 polys, SCM or SUB rules and no alternate contact rules..." +
                        " making these changes with SUB rules");
//				techMetalLayers.setSelectedIndex(0);
				techMOCMOSSubmicronRules.setSelected(true);
				techMOCMOSSecondPoly.setSelected(true);
				techMOCMOSAlternateContactRules.setSelected(true);
//				currentNumMetals = 2;                        
				currentRules = MOCMOS_SUBMRULES;
				secondPoly = true;
				alternateContactRules = false;
			}
		}
		switch (currentNumMetals)
		{
			// cannot use deep rules if less than 5 layers of metal
			case 2:
			case 3:
			case 4:
				if (currentRules == MOCMOS_DEEPRULES)
				{
					JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
						"Cannot use Deep rules if there are less than 5 layers of metal...using SubMicron rules.");
					currentRules = MOCMOS_SUBMRULES;
				}
				break;

			// cannot use scmos rules if more than 4 layers of metal
			case 5:
			case 6:
				if (currentRules == MOCMOS_SCMOSRULES)
				{
					JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
						"Cannot use SCMOS rules if there are more than 4 layers of metal...using SubMicron rules.");
					currentRules = MOCMOS_SUBMRULES;
				}
				break;
		}

		setInt(mocmosNumMetalSetting, currentNumMetals);
		setInt(mocmosRuleSetSetting, currentRules);

		setBoolean(mocmosSecondPolysiliconSetting, secondPoly);
		setBoolean(mocmosDisallowStackedViasSetting, techMOCMOSDisallowStackedVias.isSelected());
		setBoolean(mocmosAlternateActivePolyRulesSetting,alternateContactRules);
		setBoolean(mocmosAnalogSetting, techMOCMOSAnalog.isSelected());

		// Technology Project preferences
		String currentTechName = (String)technologyPopup.getSelectedItem();
		if (Technology.findTechnology(currentTechName) != null)
			setString(schematicTechnologySetting, currentTechName);
		setString(defaultTechnologySetting, (String)defaultTechPulldown.getSelectedItem());

        boolean currentV = technologyProcess.isSelected();
        if (currentV != processLayoutTechnologySetting.getBoolean())
        {
            setBoolean(processLayoutTechnologySetting, currentV);
            // It will force the reload of the factory rules.
            Technology.getCurrent().setCachedRules(null);
        }

        // Tabs for extra technologies if available
		for (Object extraTechTab: extraTechTabs)
		{
			try
			{
				extraTechTab.getClass().getMethod("term").invoke(extraTechTab);
			} catch (Exception e)
			{
				System.out.println("Exceptions while closing extra technologies: " + e.getMessage());
			}
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
			EditWindow.repaintAllContents();
	}

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
		// preferences
		if (User.isFactoryRotateLayoutTransistors() != User.isRotateLayoutTransistors())
			User.setRotateLayoutTransistors(User.isFactoryRotateLayoutTransistors());
        putPrefs(new GenerateVHDL.VHDLPreferences(true));
	}

	private void initExtraTab(String className, JPanel panel)
	{
		try
		{
			Class<?> extraTechClass = Class.forName(className);
			Object extraTechTab = extraTechClass.getConstructor(TechnologyTab.class, JPanel.class).newInstance(this, panel);
			extraTechTabs.add(extraTechTab);
		} catch (Exception e)
		{
			projectSettings.remove(panel);
		}
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
		PrimitiveNode np = Schematics.tech().findNodeProto(str);
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

	public Foundry.Type setFoundrySelected(Technology tech, javax.swing.JComboBox pulldown)
	{
		String selectedFoundry = getString(tech.getPrefFoundrySetting());
		Foundry.Type foundry = Foundry.Type.NONE;

		for (Iterator<Foundry> itF = tech.getFoundries(); itF.hasNext();)
		{
			Foundry factory = itF.next();
			Foundry.Type type = factory.getType();
			pulldown.addItem(type);
			if (selectedFoundry.equalsIgnoreCase(factory.getType().getName())) foundry = type;
		}

		pulldown.setEnabled(foundry != Foundry.Type.NONE);
		pulldown.setSelectedItem(foundry);
		return foundry;
	}

	/**
	 * Method to check whether the foundry value has been changed. If changed, primitives might need resizing.
	 * @param foundry
	 * @param tech
	 */
	public boolean checkFoundry(Foundry.Type foundry, Technology tech)
	{
		if (foundry == null) return false; // technology without design rules.
		boolean changed = false;

		if (!foundry.getName().equalsIgnoreCase(getString(tech.getPrefFoundrySetting())))
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
				setString(tech.getPrefFoundrySetting(), foundry.getName());
			}
		}
		return changed;
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        mocmosGroup = new javax.swing.ButtonGroup();
        preferences = new javax.swing.JPanel();
        vhdlPrimPane = new javax.swing.JScrollPane();
        jLabel2 = new javax.swing.JLabel();
        vhdlName = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        vhdlNegatedName = new javax.swing.JTextField();
        rotateLayoutTransistors = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        projectSettings = new javax.swing.JPanel();
        defaultsPanel = new javax.swing.JPanel();
        defaultTechLabel = new javax.swing.JLabel();
        defaultTechPulldown = new javax.swing.JComboBox();
        jLabel59 = new javax.swing.JLabel();
        technologyPopup = new javax.swing.JComboBox();
        technologyProcess = new javax.swing.JCheckBox();
        mosisPanel = new javax.swing.JPanel();
        techMetalLabel = new javax.swing.JLabel();
        techMetalLayers = new javax.swing.JComboBox();
        techMOCMOSSCMOSRules = new javax.swing.JRadioButton();
        techMOCMOSSubmicronRules = new javax.swing.JRadioButton();
        techMOCMOSDeepRules = new javax.swing.JRadioButton();
        techMOCMOSSecondPoly = new javax.swing.JCheckBox();
        techMOCMOSDisallowStackedVias = new javax.swing.JCheckBox();
        techMOCMOSAlternateContactRules = new javax.swing.JCheckBox();
        techMOCMOSAnalog = new javax.swing.JCheckBox();
        cmos90Panel = new javax.swing.JPanel();
        tsmc180Panel = new javax.swing.JPanel();

        setTitle("Edit Options");
        setName(""); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        preferences.setLayout(new java.awt.GridBagLayout());

        vhdlPrimPane.setMinimumSize(new java.awt.Dimension(22, 70));
        vhdlPrimPane.setPreferredSize(new java.awt.Dimension(22, 70));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        preferences.add(vhdlPrimPane, gridBagConstraints);

        jLabel2.setText("VHDL for primitive:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        preferences.add(jLabel2, gridBagConstraints);

        vhdlName.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        preferences.add(vhdlName, gridBagConstraints);

        jLabel3.setText("VHDL for negated primitive:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        preferences.add(jLabel3, gridBagConstraints);

        vhdlNegatedName.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        preferences.add(vhdlNegatedName, gridBagConstraints);

        rotateLayoutTransistors.setText("Rotate layout transistors in menu");
        rotateLayoutTransistors.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        preferences.add(rotateLayoutTransistors, gridBagConstraints);

        jLabel1.setText("Schematic primitives:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        preferences.add(jLabel1, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        preferences.add(jSeparator1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        getContentPane().add(preferences, gridBagConstraints);

        projectSettings.setLayout(new java.awt.GridBagLayout());

        defaultsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Defaults"));
        defaultsPanel.setLayout(new java.awt.GridBagLayout());

        defaultTechLabel.setText("Startup technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        defaultsPanel.add(defaultTechLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        defaultsPanel.add(defaultTechPulldown, gridBagConstraints);

        jLabel59.setText("Layout technology to use for Schematics:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        defaultsPanel.add(jLabel59, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        defaultsPanel.add(technologyPopup, gridBagConstraints);

        technologyProcess.setText("PSubstrate process in Layout Technology");
        technologyProcess.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                technologyProcessActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        defaultsPanel.add(technologyProcess, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        projectSettings.add(defaultsPanel, gridBagConstraints);

        mosisPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("mocmos Technology"));
        mosisPanel.setLayout(new java.awt.GridBagLayout());

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

        mocmosGroup.add(techMOCMOSSCMOSRules);
        techMOCMOSSCMOSRules.setText("SCMOS rules (4 metal or less)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        mosisPanel.add(techMOCMOSSCMOSRules, gridBagConstraints);

        mocmosGroup.add(techMOCMOSSubmicronRules);
        techMOCMOSSubmicronRules.setText("Submicron rules");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        mosisPanel.add(techMOCMOSSubmicronRules, gridBagConstraints);

        mocmosGroup.add(techMOCMOSDeepRules);
        techMOCMOSDeepRules.setText("Deep rules (5 metal or more)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        mosisPanel.add(techMOCMOSDeepRules, gridBagConstraints);

        techMOCMOSSecondPoly.setText("Second Polysilicon Layer");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        mosisPanel.add(techMOCMOSSecondPoly, gridBagConstraints);

        techMOCMOSDisallowStackedVias.setText("Disallow stacked vias");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        mosisPanel.add(techMOCMOSDisallowStackedVias, gridBagConstraints);

        techMOCMOSAlternateContactRules.setText("Alternate Active and Poly contact rules");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        mosisPanel.add(techMOCMOSAlternateContactRules, gridBagConstraints);

        techMOCMOSAnalog.setText("Analog");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        mosisPanel.add(techMOCMOSAnalog, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        projectSettings.add(mosisPanel, gridBagConstraints);

        cmos90Panel.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        cmos90Panel.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        projectSettings.add(cmos90Panel, gridBagConstraints);

        tsmc180Panel.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        tsmc180Panel.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        projectSettings.add(tsmc180Panel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        getContentPane().add(projectSettings, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

        private void technologyProcessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_technologyProcessActionPerformed
            // TODO add your handling code here:
        }//GEN-LAST:event_technologyProcessActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel cmos90Panel;
    private javax.swing.JLabel defaultTechLabel;
    private javax.swing.JComboBox defaultTechPulldown;
    private javax.swing.JPanel defaultsPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.ButtonGroup mocmosGroup;
    private javax.swing.JPanel mosisPanel;
    private javax.swing.JPanel preferences;
    private javax.swing.JPanel projectSettings;
    private javax.swing.JCheckBox rotateLayoutTransistors;
    private javax.swing.JCheckBox techMOCMOSAlternateContactRules;
    private javax.swing.JCheckBox techMOCMOSAnalog;
    private javax.swing.JRadioButton techMOCMOSDeepRules;
    private javax.swing.JCheckBox techMOCMOSDisallowStackedVias;
    private javax.swing.JRadioButton techMOCMOSSCMOSRules;
    private javax.swing.JCheckBox techMOCMOSSecondPoly;
    private javax.swing.JRadioButton techMOCMOSSubmicronRules;
    private javax.swing.JLabel techMetalLabel;
    private javax.swing.JComboBox techMetalLayers;
    private javax.swing.JComboBox technologyPopup;
    private javax.swing.JCheckBox technologyProcess;
    private javax.swing.JPanel tsmc180Panel;
    private javax.swing.JTextField vhdlName;
    private javax.swing.JTextField vhdlNegatedName;
    private javax.swing.JScrollPane vhdlPrimPane;
    // End of variables declaration//GEN-END:variables

}
