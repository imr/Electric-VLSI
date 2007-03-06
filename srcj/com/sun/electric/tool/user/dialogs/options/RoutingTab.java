/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RoutingTab.java
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
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.routing.Routing;
import com.sun.electric.tool.user.menus.MenuCommands;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

/**
 * Class to handle the "Routing" tab of the Preferences dialog.
 */
public class RoutingTab extends PreferencePanel
{
	/** Creates new form RoutingTab */
	public RoutingTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return routing; }

	/** return the name of this preferences tab. */
	public String getName() { return "Routing"; }

	private ArcProto initRoutDefArc;
	private JPanel sogArcList;
	private Map<ArcProto,JCheckBox> sogFavorChecks, sogProhibitChecks;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Routing tab.
	 */
	public void init()
	{
		// initilze for the stitcher that is running
		boolean initRoutMimicOn = Routing.isMimicStitchOn();
		boolean initRoutAutoOn = Routing.isAutoStitchOn();
		if (!initRoutMimicOn && !initRoutAutoOn) routNoStitcher.setSelected(true);
        else
		{
			if (initRoutMimicOn) routMimicStitcher.setSelected(true); else
				routAutoStitcher.setSelected(true);
		}

		// initialize the "default arc" setting
		for(Iterator<Technology> tIt = Technology.getTechnologies(); tIt.hasNext(); )
		{
			Technology tech = tIt.next();
			routTechnology.addItem(tech.getTechName());
			sogRouteTechnology.addItem(tech.getTechName());
		}
		routTechnology.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { techChanged(); }
		});

		// the sea-of-gates section
		sogFavorChecks = new HashMap<ArcProto,JCheckBox>();
		sogProhibitChecks = new HashMap<ArcProto,JCheckBox>();
		sogArcList = new JPanel();
		sogRouteArcOptions.setViewportView(sogArcList);
		sogRouteTechnology.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { sogTechChanged(); }
		});
		sogRouteTechnology.setSelectedItem(Technology.getCurrent().getTechName());
		sogMaxArcWidth.setText(TextUtils.formatDouble(Routing.getSeaOfGatesMaxWidth()));
		sogComplexityLimit.setText(Integer.toString(Routing.getSeaOfGatesComplexityLimit()));

		routTechnology.setSelectedItem(Technology.getCurrent().getTechName());
		routOverrideArc.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { overrideChanged(); }
		});
		String prefArcName = Routing.getPreferredRoutingArc();
		initRoutDefArc = null;
		if (prefArcName.length() > 0)
		{
			initRoutDefArc = ArcProto.findArcProto(prefArcName);
			routOverrideArc.setSelected(true);
		} else
		{
			routOverrideArc.setSelected(false);
		}
		overrideChanged();
		if (initRoutDefArc != null)
		{
			routTechnology.setSelectedItem(initRoutDefArc.getTechnology().getTechName());
			routDefaultArc.setSelectedItem(initRoutDefArc.getName());
		}

		routMimicPortsMustMatch.setSelected(Routing.isMimicStitchMatchPorts());
		routMimicPortsWidthMustMatch.setSelected(Routing.isMimicStitchMatchPortWidth());
		routMimicNumArcsMustMatch.setSelected(Routing.isMimicStitchMatchNumArcs());
		routMimicNodeSizesMustMatch.setSelected(Routing.isMimicStitchMatchNodeSize());
		routMimicNodeTypesMustMatch.setSelected(Routing.isMimicStitchMatchNodeType());
		routMimicNoOtherArcs.setSelected(Routing.isMimicStitchNoOtherArcsSameDir());
		routMimicOnlyNewTopology.setSelected(Routing.isMimicStitchOnlyNewTopology());
		routMimicInteractive.setSelected(Routing.isMimicStitchInteractive());
        routMimicKeepPins.setSelected(Routing.isMimicStitchPinsKept());
	}

	private void overrideChanged()
	{
		boolean enableRest = routOverrideArc.isSelected();

		// set other fields
		routTechnology.setEnabled(enableRest);
		routTechLabel.setEnabled(enableRest);
		routDefaultArc.setEnabled(enableRest);
		routArcLabel.setEnabled(enableRest);
	}

	/**
	 * Method called when the technology (for default arcs) has changed.
	 */
	private void techChanged()
	{
		String techName = (String)routTechnology.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return;

		routDefaultArc.removeAllItems();
		for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			routDefaultArc.addItem(ap.getName());
		}
		routDefaultArc.setSelectedIndex(0);
	}

	/**
	 * Method called when the "Sea of gates" technology has changed.
	 */
	private void sogTechChanged()
	{
		String techName = (String)sogRouteTechnology.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return;

		sogArcList = new JPanel();
		sogRouteArcOptions.setViewportView(sogArcList);
		sogArcList.setLayout(new GridBagLayout());
		int i=0;
		for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			if (!ap.getFunction().isMetal()) continue;
			JLabel arcName = new JLabel(ap.getName());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = i;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.weightx = 1;
			sogArcList.add(arcName, gbc);

			JCheckBox favorArc = sogFavorChecks.get(ap);
			if (favorArc == null)
			{
				favorArc = new JCheckBox("Favor");
				sogFavorChecks.put(ap, favorArc);
				if (Routing.isSeaOfGatesFavor(ap)) favorArc.setSelected(true);
			}
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = i;
			sogArcList.add(favorArc, gbc);

			JCheckBox prohibitArc = sogProhibitChecks.get(ap);
			if (prohibitArc == null)
			{
				prohibitArc = new JCheckBox("Prohibit");
				sogProhibitChecks.put(ap, prohibitArc);
				if (Routing.isSeaOfGatesPrevent(ap)) prohibitArc.setSelected(true);
			}
			gbc = new GridBagConstraints();
			gbc.gridx = 2;   gbc.gridy = i;
			sogArcList.add(prohibitArc, gbc);

			i++;
		}
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Routing tab.
	 */
	public void term()
	{
		boolean curMimic = routMimicStitcher.isSelected();
		if (curMimic != Routing.isMimicStitchOn()) {
			Routing.setMimicStitchOn(curMimic);
            MenuCommands.menuBar().updateAllButtons();
        }
		boolean curAuto = routAutoStitcher.isSelected();
		if (curAuto != Routing.isAutoStitchOn()) {
			Routing.setAutoStitchOn(curAuto);
            MenuCommands.menuBar().updateAllButtons();
        }

		// pick up sea-of-gates preferences
		for(Iterator<ArcProto> it = sogFavorChecks.keySet().iterator(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			JCheckBox check = sogFavorChecks.get(ap);
			if (check.isSelected() != Routing.isSeaOfGatesFavor(ap))
				Routing.setSeaOfGatesFavor(ap, check.isSelected());
		}
		for(Iterator<ArcProto> it = sogProhibitChecks.keySet().iterator(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			JCheckBox check = sogProhibitChecks.get(ap);
			if (check.isSelected() != Routing.isSeaOfGatesPrevent(ap))
				Routing.setSeaOfGatesPrevent(ap, check.isSelected());
		}
		double curSOGMaxWid = TextUtils.atof(sogMaxArcWidth.getText());
		if (curSOGMaxWid != Routing.getSeaOfGatesMaxWidth())
			Routing.setSeaOfGatesMaxWidth(curSOGMaxWid);
		int curSOGComplexity = TextUtils.atoi(sogComplexityLimit.getText());
		if (curSOGComplexity != Routing.getSeaOfGatesComplexityLimit())
			Routing.setSeaOfGatesComplexityLimit(curSOGComplexity);

		ArcProto ap = null;
		if (routOverrideArc.isSelected())
		{
			String techName = (String)routTechnology.getSelectedItem();
			Technology tech = Technology.findTechnology(techName);
			if (tech != null)
			{
				String curArcName = (String)routDefaultArc.getSelectedItem();
				ap = tech.findArcProto(curArcName);
			}
		}
		if (ap != initRoutDefArc)
		{
			String newArcName = "";
			if (ap != null) newArcName = ap.getTechnology().getTechName() + ":" + ap.getName();
			Routing.setPreferredRoutingArc(newArcName);
		}

		boolean cur = routMimicPortsMustMatch.isSelected();
		if (cur != Routing.isMimicStitchMatchPorts())
			Routing.setMimicStitchMatchPorts(cur);

		cur = routMimicPortsWidthMustMatch.isSelected();
		if (cur != Routing.isMimicStitchMatchPortWidth())
			Routing.setMimicStitchMatchPortWidth(cur);

		cur = routMimicNumArcsMustMatch.isSelected();
		if (cur != Routing.isMimicStitchMatchNumArcs())
			Routing.setMimicStitchMatchNumArcs(cur);

		cur = routMimicNodeSizesMustMatch.isSelected();
		if (cur != Routing.isMimicStitchMatchNodeSize())
			Routing.setMimicStitchMatchNodeSize(cur);

		cur = routMimicNodeTypesMustMatch.isSelected();
		if (cur != Routing.isMimicStitchMatchNodeType())
			Routing.setMimicStitchMatchNodeType(cur);

		cur = routMimicNoOtherArcs.isSelected();
		if (cur != Routing.isMimicStitchNoOtherArcsSameDir())
			Routing.setMimicStitchNoOtherArcsSameDir(cur);

		cur = routMimicOnlyNewTopology.isSelected();
		if (cur != Routing.isMimicStitchOnlyNewTopology())
			Routing.setMimicStitchOnlyNewTopology(cur);

		cur = routMimicInteractive.isSelected();
		if (cur != Routing.isMimicStitchInteractive())
			Routing.setMimicStitchInteractive(cur);

		cur = routMimicKeepPins.isSelected();
		if (cur != Routing.isMimicStitchPinsKept())
			Routing.setMimicStitchPinsKept(cur);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        routStitcher = new javax.swing.ButtonGroup();
        routing = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jLabel70 = new javax.swing.JLabel();
        routMimicPortsMustMatch = new javax.swing.JCheckBox();
        routMimicInteractive = new javax.swing.JCheckBox();
        routMimicNumArcsMustMatch = new javax.swing.JCheckBox();
        routMimicNodeSizesMustMatch = new javax.swing.JCheckBox();
        routMimicNodeTypesMustMatch = new javax.swing.JCheckBox();
        routMimicNoOtherArcs = new javax.swing.JCheckBox();
        routMimicPortsWidthMustMatch = new javax.swing.JCheckBox();
        routMimicKeepPins = new javax.swing.JCheckBox();
        routMimicOnlyNewTopology = new javax.swing.JCheckBox();
        jPanel8 = new javax.swing.JPanel();
        routTechLabel = new javax.swing.JLabel();
        routDefaultArc = new javax.swing.JComboBox();
        routNoStitcher = new javax.swing.JRadioButton();
        routAutoStitcher = new javax.swing.JRadioButton();
        routMimicStitcher = new javax.swing.JRadioButton();
        routTechnology = new javax.swing.JComboBox();
        routArcLabel = new javax.swing.JLabel();
        routOverrideArc = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        sogRouteTechnology = new javax.swing.JComboBox();
        sogRouteArcOptions = new javax.swing.JScrollPane();
        jLabel2 = new javax.swing.JLabel();
        sogMaxArcWidth = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        sogComplexityLimit = new javax.swing.JTextField();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Tool Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        routing.setLayout(new java.awt.GridBagLayout());

        jPanel7.setLayout(new java.awt.GridBagLayout());

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Mimic Stitcher"));
        jLabel70.setText("Restrictions (when non-interactive):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        jPanel7.add(jLabel70, gridBagConstraints);

        routMimicPortsMustMatch.setText("Ports must match");
        routMimicPortsMustMatch.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 20, 1, 4);
        jPanel7.add(routMimicPortsMustMatch, gridBagConstraints);

        routMimicInteractive.setText("Interactive mimicking");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel7.add(routMimicInteractive, gridBagConstraints);

        routMimicNumArcsMustMatch.setText("Number of existing arcs must match");
        routMimicNumArcsMustMatch.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 20, 1, 4);
        jPanel7.add(routMimicNumArcsMustMatch, gridBagConstraints);

        routMimicNodeSizesMustMatch.setText("Node sizes must match");
        routMimicNodeSizesMustMatch.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 20, 1, 4);
        jPanel7.add(routMimicNodeSizesMustMatch, gridBagConstraints);

        routMimicNodeTypesMustMatch.setText("Node types must match");
        routMimicNodeTypesMustMatch.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 20, 1, 4);
        jPanel7.add(routMimicNodeTypesMustMatch, gridBagConstraints);

        routMimicNoOtherArcs.setText("Cannot have other arcs in the same direction");
        routMimicNoOtherArcs.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 20, 1, 4);
        jPanel7.add(routMimicNoOtherArcs, gridBagConstraints);

        routMimicPortsWidthMustMatch.setText("Bus ports must have same width");
        routMimicPortsWidthMustMatch.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 20, 1, 4);
        jPanel7.add(routMimicPortsWidthMustMatch, gridBagConstraints);

        routMimicKeepPins.setText("Keep pins");
        routMimicKeepPins.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        routMimicKeepPins.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel7.add(routMimicKeepPins, gridBagConstraints);

        routMimicOnlyNewTopology.setText("Ignore if already connected elsewhere");
        routMimicOnlyNewTopology.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 20, 4, 4);
        jPanel7.add(routMimicOnlyNewTopology, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        routing.add(jPanel7, gridBagConstraints);

        jPanel8.setLayout(new java.awt.GridBagLayout());

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder("All Routers"));
        routTechLabel.setText("Technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 20, 1, 4);
        jPanel8.add(routTechLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        jPanel8.add(routDefaultArc, gridBagConstraints);

        routStitcher.add(routNoStitcher);
        routNoStitcher.setText("No stitcher running");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        jPanel8.add(routNoStitcher, gridBagConstraints);

        routStitcher.add(routAutoStitcher);
        routAutoStitcher.setText("Auto-stitcher running");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        jPanel8.add(routAutoStitcher, gridBagConstraints);

        routStitcher.add(routMimicStitcher);
        routMimicStitcher.setText("Mimic-stitcher running");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        jPanel8.add(routMimicStitcher, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        jPanel8.add(routTechnology, gridBagConstraints);

        routArcLabel.setText("Arc:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 20, 4, 4);
        jPanel8.add(routArcLabel, gridBagConstraints);

        routOverrideArc.setText("Use this arc in stitching routers:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        jPanel8.add(routOverrideArc, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        routing.add(jPanel8, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Sea-of-Gates Router"));
        jLabel1.setText("Technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        jPanel1.add(jLabel1, gridBagConstraints);

        sogRouteTechnology.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        jPanel1.add(sogRouteTechnology, gridBagConstraints);

        sogRouteArcOptions.setMinimumSize(new java.awt.Dimension(100, 100));
        sogRouteArcOptions.setOpaque(false);
        sogRouteArcOptions.setPreferredSize(new java.awt.Dimension(100, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        jPanel1.add(sogRouteArcOptions, gridBagConstraints);

        jLabel2.setText("Maximum arc width:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        jPanel1.add(jLabel2, gridBagConstraints);

        sogMaxArcWidth.setColumns(10);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        jPanel1.add(sogMaxArcWidth, gridBagConstraints);

        jLabel3.setText("Search complexity limit:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        jPanel1.add(jLabel3, gridBagConstraints);

        sogComplexityLimit.setColumns(10);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        jPanel1.add(sogComplexityLimit, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        routing.add(jPanel1, gridBagConstraints);

        getContentPane().add(routing, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel70;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JLabel routArcLabel;
    private javax.swing.JRadioButton routAutoStitcher;
    private javax.swing.JComboBox routDefaultArc;
    private javax.swing.JCheckBox routMimicInteractive;
    private javax.swing.JCheckBox routMimicKeepPins;
    private javax.swing.JCheckBox routMimicNoOtherArcs;
    private javax.swing.JCheckBox routMimicNodeSizesMustMatch;
    private javax.swing.JCheckBox routMimicNodeTypesMustMatch;
    private javax.swing.JCheckBox routMimicNumArcsMustMatch;
    private javax.swing.JCheckBox routMimicOnlyNewTopology;
    private javax.swing.JCheckBox routMimicPortsMustMatch;
    private javax.swing.JCheckBox routMimicPortsWidthMustMatch;
    private javax.swing.JRadioButton routMimicStitcher;
    private javax.swing.JRadioButton routNoStitcher;
    private javax.swing.JCheckBox routOverrideArc;
    private javax.swing.ButtonGroup routStitcher;
    private javax.swing.JLabel routTechLabel;
    private javax.swing.JComboBox routTechnology;
    private javax.swing.JPanel routing;
    private javax.swing.JTextField sogComplexityLimit;
    private javax.swing.JTextField sogMaxArcWidth;
    private javax.swing.JScrollPane sogRouteArcOptions;
    private javax.swing.JComboBox sogRouteTechnology;
    // End of variables declaration//GEN-END:variables

}
