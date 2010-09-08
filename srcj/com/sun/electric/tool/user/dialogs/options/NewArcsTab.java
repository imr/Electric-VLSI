/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NewArcsTab.java
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

import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.id.ArcProtoId;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.util.TextUtils;
import com.sun.electric.util.math.DBMath;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Class to handle the "New Arcs" tab of the Preferences dialog.
 */
public class NewArcsTab extends PreferencePanel
{
	/** Creates new form NewArcsTab */
	public NewArcsTab(PreferencesFrame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// make all text fields select-all when entered
	    EDialog.makeTextFieldSelectAllOnTab(arcWidth);
	    EDialog.makeTextFieldSelectAllOnTab(arcAngle);
	}

	/** return the panel to use for user preferences. */
    @Override
	public JPanel getUserPreferencesPanel() { return newArc; }

	/** return the name of this preferences tab. */
    @Override
	public String getName() { return "Arcs"; }

	private boolean newArcsDataChanging = false;
	private boolean techChanging = false;
	private Technology selectedTech;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the New Arcs tab.
	 */
    @Override
	public void init()
	{
		// gather information about the ArcProtos in the current Technology
		for(Iterator<Technology> tIt = Technology.getTechnologies(); tIt.hasNext(); )
		{
			Technology tech = tIt.next();
			technologySelection.addItem(tech.getTechName());
		}
		technologySelection.setSelectedItem(Technology.getCurrent().getTechName());
		selectedTech = null;
		newArcsPrimPopupChanged();

		// setup listeners to react to a change of the selected arc
		technologySelection.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { newArcsPrimPopupChanged(); }
		});
		arcProtoList.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { newArcsPrimPopupChanged(); }
		});

		// setup listeners to react to any changes to the arc values
        arcRigid.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { newArcsPrimDataChanged(); }
        });
        arcFixedAngle.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { newArcsPrimDataChanged(); }
        });
        arcSlidable.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { newArcsPrimDataChanged(); }
        });
        arcDirectional.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { newArcsPrimDataChanged(); }
        });
        arcEndsExtend.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { newArcsPrimDataChanged(); }
        });
		arcWidth.getDocument().addDocumentListener(new NewArcDocumentListener(this));
		arcAngle.getDocument().addDocumentListener(new NewArcDocumentListener(this));
        arcPin.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { newArcsPrimDataChanged(); }
        });

		playClickSounds.setSelected(User.isPlayClickSoundsWhenCreatingArcs());
		incrementArcNames.setSelected(User.isArcsAutoIncremented());

        EditingPreferences ep = EditingPreferences.getThreadEditingPreferences();
		enableFatWiring.setSelected(ep.isFatWires());
	}

    /**
	 * Method called when the primitive arc popup is changed.
	 */
	private void newArcsPrimPopupChanged()
	{
		if (techChanging) return;
		String techName = (String)technologySelection.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return;
		newArcsDataChanging = true;
		if (tech != selectedTech)
		{
			// reload the arcs
			selectedTech = tech;
			techChanging = true;
			arcProtoList.removeAllItems();
			arcPin.removeAllItems();
			for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
			{
				ArcProto ap = it.next();
				arcProtoList.addItem(ap.getName());
			}
			arcProtoList.setSelectedIndex(0);
			techChanging = false;

			// setup popup of possible pins
			for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
			{
				PrimitiveNode np = it.next();
				arcPin.addItem(np.getName());
			}
		}

        EditingPreferences ep = getEditingPreferences();
		String primName = (String)arcProtoList.getSelectedItem();
		ArcProto ap = tech.findArcProto(primName);
        ImmutableArcInst a = ap.getDefaultInst(ep);
        int angleIncrement = ap.getAngleIncrement(ep);
        PrimitiveNode pin = ap.findOverridablePinProto(ep);

		arcRigid.setSelected(a.isRigid());
		arcFixedAngle.setSelected(a.isFixedAngle());
		arcSlidable.setSelected(a.isSlidable());
		arcDirectional.setSelected(a.isTailArrowed());
		arcEndsExtend.setSelected(a.isHeadExtended());
        double wid = DBMath.gridToLambda(2*(ap.getGridBaseExtend() + a.getGridExtendOverMin()));
		arcWidth.setText(TextUtils.formatDistance(wid, tech));

		arcAngle.setText(Integer.toString(angleIncrement));
		arcPin.setSelectedItem(pin.getName());
		newArcsDataChanging = false;
	}

	/**
	 * Class to handle special changes to per-primitive arc options.
	 */
	private static class NewArcDocumentListener implements DocumentListener
	{
		NewArcsTab dialog;

		NewArcDocumentListener(NewArcsTab dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.newArcsPrimDataChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.newArcsPrimDataChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.newArcsPrimDataChanged(); }
	}

	/**
	 * Method called when any of the primitive data changes.
	 * Caches all values for the selected primitive arc.
	 */
	private void newArcsPrimDataChanged()
	{
		if (newArcsDataChanging) return;
        EditingPreferences ep = getEditingPreferences();
		String techName = (String)technologySelection.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return;
		String primName = (String)arcProtoList.getSelectedItem();
		ArcProto ap = tech.findArcProto(primName);
        ArcProtoId apId = ap.getId();

        int flags = ap.getDefaultInst(ep).flags;
        flags = ImmutableArcInst.RIGID.set(flags, arcRigid.isSelected());
        flags = ImmutableArcInst.FIXED_ANGLE.set(flags, arcFixedAngle.isSelected());
        flags = ImmutableArcInst.SLIDABLE.set(flags, arcSlidable.isSelected());
        flags = ImmutableArcInst.TAIL_EXTENDED.set(flags, arcEndsExtend.isSelected());
        flags = ImmutableArcInst.HEAD_EXTENDED.set(flags, arcEndsExtend.isSelected());
        flags = ImmutableArcInst.HEAD_ARROWED.set(flags, arcDirectional.isSelected());
        flags = ImmutableArcInst.BODY_ARROWED.set(flags, arcDirectional.isSelected());
        ep = ep.withArcFlags(apId, flags);

        double wid = TextUtils.atofDistance(arcWidth.getText(), tech);
        long gridExtend = DBMath.lambdaToGrid(wid*0.5 - ap.getLambdaBaseExtend());
        ep = ep.withArcGridExtend(apId, gridExtend);

        int angleIncrement = TextUtils.atoi(arcAngle.getText());
        ep = ep.withArcAngleIncrement(apId, angleIncrement);

        PrimitiveNode pin = tech.findNodeProto((String)arcPin.getSelectedItem());
		PortProto pp = pin.getPorts().next();
		if (!pp.connectsTo(ap))
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"Cannot use " + pin.getName() + " as a pin because it does not connect to " + ap.getName() + " arcs");
			pin = ap.findOverridablePinProto(ep);
			arcPin.setSelectedItem(pin.getName());
		}
        ep = ep.withArcPin(apId, pin.getId());
        setEditingPreferences(ep);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the New Arcs tab.
	 */
    @Override
	public void term()
	{
		boolean currBoolean = playClickSounds.isSelected();
		if (currBoolean != User.isPlayClickSoundsWhenCreatingArcs())
			User.setPlayClickSoundsWhenCreatingArcs(currBoolean);

		currBoolean = incrementArcNames.isSelected();
		if (currBoolean != User.isArcsAutoIncremented())
			User.setArcsAutoIncremented(currBoolean);

        EditingPreferences ep = getEditingPreferences();
        ep = ep.withFatWires(enableFatWiring.isSelected());
        setEditingPreferences(ep);
	}

	/**
	 * Method called when the factory reset is requested.
	 */
    @Override
	public void reset()
	{
        setEditingPreferences(getEditingPreferences().withArcsReset());
		if (User.isFactoryPlayClickSoundsWhenCreatingArcs() != User.isPlayClickSoundsWhenCreatingArcs())
			User.setPlayClickSoundsWhenCreatingArcs(User.isFactoryPlayClickSoundsWhenCreatingArcs());
		if (User.isFactoryArcsAutoIncremented() != User.isArcsAutoIncremented())
			User.setArcsAutoIncremented(User.isFactoryArcsAutoIncremented());
        setEditingPreferences(getEditingPreferences().withFatWiresReset());
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        newArc = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        arcWidthLabel = new javax.swing.JLabel();
        pinLabel = new javax.swing.JLabel();
        arcWidth = new javax.swing.JTextField();
        angleLabel = new javax.swing.JLabel();
        arcAngle = new javax.swing.JTextField();
        arcPin = new javax.swing.JComboBox();
        arcProtoList = new javax.swing.JComboBox();
        arcName = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        arcRigid = new javax.swing.JCheckBox();
        arcFixedAngle = new javax.swing.JCheckBox();
        arcDirectional = new javax.swing.JCheckBox();
        arcSlidable = new javax.swing.JCheckBox();
        arcEndsExtend = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        technologySelection = new javax.swing.JComboBox();
        arcName1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        playClickSounds = new javax.swing.JCheckBox();
        incrementArcNames = new javax.swing.JCheckBox();
        enableFatWiring = new javax.swing.JCheckBox();

        setTitle("Edit Options");
        setName(""); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        newArc.setLayout(new java.awt.GridBagLayout());

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder("For New Arcs"));
        jPanel8.setDoubleBuffered(false);
        jPanel8.setLayout(new java.awt.GridBagLayout());

        arcWidthLabel.setText("Default width:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(arcWidthLabel, gridBagConstraints);

        pinLabel.setText("Pin:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(pinLabel, gridBagConstraints);

        arcWidth.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(arcWidth, gridBagConstraints);

        angleLabel.setText("Placement angle:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(angleLabel, gridBagConstraints);

        arcAngle.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(arcAngle, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(arcPin, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(arcProtoList, gridBagConstraints);

        arcName.setText("Arc Type:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(arcName, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(jSeparator1, gridBagConstraints);

        arcRigid.setText("Rigid");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(arcRigid, gridBagConstraints);

        arcFixedAngle.setText("Fixed-angle");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(arcFixedAngle, gridBagConstraints);

        arcDirectional.setText("Directional");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(arcDirectional, gridBagConstraints);

        arcSlidable.setText("Slidable");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(arcSlidable, gridBagConstraints);

        arcEndsExtend.setText("Ends extended");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(arcEndsExtend, gridBagConstraints);

        jLabel1.setText("Default State");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(jLabel1, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(technologySelection, gridBagConstraints);

        arcName1.setText("Technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(arcName1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        newArc.add(jPanel8, gridBagConstraints);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("For All Arcs"));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        playClickSounds.setText("Play \"click\" sounds when arcs are created");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(playClickSounds, gridBagConstraints);

        incrementArcNames.setText("Duplicate/Array/Paste increments arc names");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(incrementArcNames, gridBagConstraints);

        enableFatWiring.setText("Draw arcs as wide as their connecting nodes");
        enableFatWiring.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableFatWiringActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(enableFatWiring, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        newArc.add(jPanel1, gridBagConstraints);

        getContentPane().add(newArc, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

        private void enableFatWiringActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enableFatWiringActionPerformed
            // TODO add your handling code here:
        }//GEN-LAST:event_enableFatWiringActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel angleLabel;
    private javax.swing.JTextField arcAngle;
    private javax.swing.JCheckBox arcDirectional;
    private javax.swing.JCheckBox arcEndsExtend;
    private javax.swing.JCheckBox arcFixedAngle;
    private javax.swing.JLabel arcName;
    private javax.swing.JLabel arcName1;
    private javax.swing.JComboBox arcPin;
    private javax.swing.JComboBox arcProtoList;
    private javax.swing.JCheckBox arcRigid;
    private javax.swing.JCheckBox arcSlidable;
    private javax.swing.JTextField arcWidth;
    private javax.swing.JLabel arcWidthLabel;
    private javax.swing.JCheckBox enableFatWiring;
    private javax.swing.JCheckBox incrementArcNames;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPanel newArc;
    private javax.swing.JLabel pinLabel;
    private javax.swing.JCheckBox playClickSounds;
    private javax.swing.JComboBox technologySelection;
    // End of variables declaration//GEN-END:variables

}
