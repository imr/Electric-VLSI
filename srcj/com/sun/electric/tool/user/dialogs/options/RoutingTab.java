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

import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.routing.Routing;

import java.util.Iterator;

import javax.swing.JPanel;

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
	public JPanel getPanel() { return routing; }

	public String getName() { return "Routing"; }

	private boolean initRoutMimicOn, initRoutAutoOn;
	private ArcProto initRoutDefArc;
	private boolean initRoutMimicCanUnstitch;
	private boolean initRoutMimicInteractive;
	private boolean initRoutMimicMatchPorts;
	private boolean initRoutMimicMatchNumArcs;
	private boolean initRoutMimicMatchNodeSize;
	private boolean initRoutMimicMatchNodeType;
	private boolean initRoutMimicNoArcsSameDir;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Routing tab.
	 */
	public void init()
	{
		initRoutMimicOn = Routing.isMimicStitchOn();
		initRoutAutoOn = Routing.isAutoStitchOn();
		if (!initRoutMimicOn && !initRoutAutoOn) routNoStitcher.setSelected(true); else
		{
			if (initRoutMimicOn) routMimicStitcher.setSelected(true); else
				routAutoStitcher.setSelected(true);
		}

		routDefaultArc.addItem("DEFAULT ARC");
		for(Iterator it = curTech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = (ArcProto)it.next();
			routDefaultArc.addItem(ap.describe());
		}
		String prefArcName = Routing.getPreferredRoutingArc();
		initRoutDefArc = null;
		if (prefArcName.length() > 0)
			initRoutDefArc = ArcProto.findArcProto(prefArcName);
		if (initRoutDefArc != null)
			routDefaultArc.setSelectedItem(initRoutDefArc.describe());

		initRoutMimicCanUnstitch = Routing.isMimicStitchCanUnstitch();
		routMimicCanUnstitch.setSelected(initRoutMimicCanUnstitch);

		initRoutMimicMatchPorts = Routing.isMimicStitchMatchPorts();
		routMimicPortsMustMatch.setSelected(initRoutMimicMatchPorts);

		initRoutMimicMatchNumArcs = Routing.isMimicStitchMatchNumArcs();
		routMimicNumArcsMustMatch.setSelected(initRoutMimicMatchNumArcs);

		initRoutMimicMatchNodeSize = Routing.isMimicStitchMatchNodeSize();
		routMimicNodeSizesMustMatch.setSelected(initRoutMimicMatchNodeSize);

		initRoutMimicMatchNodeType = Routing.isMimicStitchMatchNodeType();
		routMimicNodeTypesMustMatch.setSelected(initRoutMimicMatchNodeType);

		initRoutMimicNoArcsSameDir = Routing.isMimicStitchNoOtherArcsSameDir();
		routMimicNoOtherArcs.setSelected(initRoutMimicNoArcsSameDir);

		initRoutMimicInteractive = Routing.isMimicStitchInteractive();
		routMimicInteractive.setSelected(initRoutMimicInteractive);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Routing tab.
	 */
	public void term()
	{
		boolean curMimic = routMimicStitcher.isSelected();
		if (curMimic != initRoutMimicOn)
			Routing.setMimicStitchOn(curMimic);
		boolean curAuto = routAutoStitcher.isSelected();
		if (curAuto != initRoutAutoOn)
			Routing.setAutoStitchOn(curAuto);

		ArcProto ap = null;
		int curArcIndex = routDefaultArc.getSelectedIndex();
		if (curArcIndex > 0)
		{
			String curArcName = (String)routDefaultArc.getSelectedItem();
			ap = ArcProto.findArcProto(curArcName);
		}
		if (ap != initRoutDefArc)
		{
			String newArcName = "";
			if (ap != null) newArcName = ap.getTechnology().getTechName() + ":" + ap.getName();
			Routing.setPreferredRoutingArc(newArcName);
		}
		
		boolean cur = routMimicCanUnstitch.isSelected();
		if (cur != initRoutMimicCanUnstitch)
			Routing.setMimicStitchCanUnstitch(cur);

		cur = routMimicPortsMustMatch.isSelected();
		if (cur != initRoutMimicMatchPorts)
			Routing.setMimicStitchMatchPorts(cur);

		cur = routMimicNumArcsMustMatch.isSelected();
		if (cur != initRoutMimicMatchNumArcs)
			Routing.setMimicStitchMatchNumArcs(cur);

		cur = routMimicNodeSizesMustMatch.isSelected();
		if (cur != initRoutMimicMatchNodeSize)
			Routing.setMimicStitchMatchNodeSize(cur);

		cur = routMimicNodeTypesMustMatch.isSelected();
		if (cur != initRoutMimicMatchNodeType)
			Routing.setMimicStitchMatchNodeType(cur);

		cur = routMimicNoOtherArcs.isSelected();
		if (cur != initRoutMimicNoArcsSameDir)
			Routing.setMimicStitchNoOtherArcsSameDir(cur);

		cur = routMimicInteractive.isSelected();
		if (cur != initRoutMimicInteractive)
			Routing.setMimicStitchInteractive(cur);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        routStitcher = new javax.swing.ButtonGroup();
        routing = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jLabel70 = new javax.swing.JLabel();
        routMimicPortsMustMatch = new javax.swing.JCheckBox();
        routMimicInteractive = new javax.swing.JCheckBox();
        routMimicCanUnstitch = new javax.swing.JCheckBox();
        routMimicNumArcsMustMatch = new javax.swing.JCheckBox();
        routMimicNodeSizesMustMatch = new javax.swing.JCheckBox();
        routMimicNodeTypesMustMatch = new javax.swing.JCheckBox();
        routMimicNoOtherArcs = new javax.swing.JCheckBox();
        jPanel8 = new javax.swing.JPanel();
        jLabel67 = new javax.swing.JLabel();
        jLabel69 = new javax.swing.JLabel();
        routDefaultArc = new javax.swing.JComboBox();
        routNoStitcher = new javax.swing.JRadioButton();
        routAutoStitcher = new javax.swing.JRadioButton();
        routMimicStitcher = new javax.swing.JRadioButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Tool Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        routing.setLayout(new java.awt.GridBagLayout());

        jPanel7.setLayout(new java.awt.GridBagLayout());

        jPanel7.setBorder(new javax.swing.border.TitledBorder("Mimic Stitcher"));
        jLabel70.setText("Mimic stitching restrictions (non-interactive):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel7.add(jLabel70, gridBagConstraints);

        routMimicPortsMustMatch.setText("Ports must match");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        jPanel7.add(routMimicPortsMustMatch, gridBagConstraints);

        routMimicInteractive.setText("Mimic stitching runs interactively");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel7.add(routMimicInteractive, gridBagConstraints);

        routMimicCanUnstitch.setText("Mimic stitching can unstitch");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel7.add(routMimicCanUnstitch, gridBagConstraints);

        routMimicNumArcsMustMatch.setText("Number of existing arcs must match");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        jPanel7.add(routMimicNumArcsMustMatch, gridBagConstraints);

        routMimicNodeSizesMustMatch.setText("Node sizes must match");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        jPanel7.add(routMimicNodeSizesMustMatch, gridBagConstraints);

        routMimicNodeTypesMustMatch.setText("Node types must match");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        jPanel7.add(routMimicNodeTypesMustMatch, gridBagConstraints);

        routMimicNoOtherArcs.setText("Cannot have other arcs in the same direction");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        jPanel7.add(routMimicNoOtherArcs, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        routing.add(jPanel7, gridBagConstraints);

        jPanel8.setLayout(new java.awt.GridBagLayout());

        jPanel8.setBorder(new javax.swing.border.TitledBorder("All Routers"));
        jLabel67.setText("Arc to use in stitching routers:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(jLabel67, gridBagConstraints);

        jLabel69.setText("Currently:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(jLabel69, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(routDefaultArc, gridBagConstraints);

        routNoStitcher.setText("No stitcher running");
        routStitcher.add(routNoStitcher);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel8.add(routNoStitcher, gridBagConstraints);

        routAutoStitcher.setText("Auto-stitcher running");
        routStitcher.add(routAutoStitcher);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel8.add(routAutoStitcher, gridBagConstraints);

        routMimicStitcher.setText("Mimic-stitcher running");
        routStitcher.add(routMimicStitcher);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel8.add(routMimicStitcher, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        routing.add(jPanel8, gridBagConstraints);

        getContentPane().add(routing, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel67;
    private javax.swing.JLabel jLabel69;
    private javax.swing.JLabel jLabel70;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JRadioButton routAutoStitcher;
    private javax.swing.JComboBox routDefaultArc;
    private javax.swing.JCheckBox routMimicCanUnstitch;
    private javax.swing.JCheckBox routMimicInteractive;
    private javax.swing.JCheckBox routMimicNoOtherArcs;
    private javax.swing.JCheckBox routMimicNodeSizesMustMatch;
    private javax.swing.JCheckBox routMimicNodeTypesMustMatch;
    private javax.swing.JCheckBox routMimicNumArcsMustMatch;
    private javax.swing.JCheckBox routMimicPortsMustMatch;
    private javax.swing.JRadioButton routMimicStitcher;
    private javax.swing.JRadioButton routNoStitcher;
    private javax.swing.ButtonGroup routStitcher;
    private javax.swing.JPanel routing;
    // End of variables declaration//GEN-END:variables
	
}
