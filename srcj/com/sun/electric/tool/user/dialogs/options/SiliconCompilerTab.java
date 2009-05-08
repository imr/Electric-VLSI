/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SiliconCompilerTab.java
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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.sc.SilComp;
import com.sun.electric.tool.user.dialogs.EDialog;

import java.util.Iterator;

import javax.swing.JPanel;

/**
 * Class to handle the "Silicon Compiler" tab of the Preferences dialog.
 */
public class SiliconCompilerTab extends PreferencePanel
{
	/** Creates new form SiliconCompilerTab */
	public SiliconCompilerTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// make all text fields select-all when entered
	    EDialog.makeTextFieldSelectAllOnTab(numRows);
	    EDialog.makeTextFieldSelectAllOnTab(horizWireWidth);
	    EDialog.makeTextFieldSelectAllOnTab(vertWireWidth);
	    EDialog.makeTextFieldSelectAllOnTab(powerWidth);
	    EDialog.makeTextFieldSelectAllOnTab(mainPowerWidth);
	    EDialog.makeTextFieldSelectAllOnTab(pWellHeight);
	    EDialog.makeTextFieldSelectAllOnTab(pWellOffset);
	    EDialog.makeTextFieldSelectAllOnTab(nWellHeight);
	    EDialog.makeTextFieldSelectAllOnTab(nWellOffset);
	    EDialog.makeTextFieldSelectAllOnTab(viaSize);
	    EDialog.makeTextFieldSelectAllOnTab(minMetalSpacing);
	    EDialog.makeTextFieldSelectAllOnTab(feedThruSize);
	    EDialog.makeTextFieldSelectAllOnTab(minPortDist);
	    EDialog.makeTextFieldSelectAllOnTab(minActiveDist);
	}

	/** return the panel to use for user preferences. */
    @Override
	public JPanel getUserPreferencesPanel() { return siliconCompiler; }

	/** return the name of this preferences tab. */
    @Override
	public String getName() { return "Silicon Compiler"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Selection tab.
	 */
    @Override
	public void init()
	{
        SilComp.SilCompPrefs scp = new SilComp.SilCompPrefs(false);

		// the layout information
		numRows.setText(Integer.toString(scp.numRows));

		// the arc information
		for(Iterator<ArcProto> it = Technology.getCurrent().getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			horizRoutingArc.addItem(ap.getName());
			vertRoutingArc.addItem(ap.getName());
			mainPowerArc.addItem(ap.getName());
		}
		horizRoutingArc.setSelectedItem(scp.horizRoutingArc);
		horizWireWidth.setText(TextUtils.formatDistance(scp.horizArcWidth));
		vertRoutingArc.setSelectedItem(scp.vertRoutingArc);
		vertWireWidth.setText(TextUtils.formatDistance(scp.vertArcWidth));

		powerWidth.setText(TextUtils.formatDistance(scp.powerWireWidth));
		mainPowerWidth.setText(TextUtils.formatDistance(scp.mainPowerWireWidth));
		mainPowerArc.setSelectedItem(scp.mainPowerArc);

		// the Well information
		pWellHeight.setText(TextUtils.formatDistance(scp.pWellHeight));
		pWellOffset.setText(TextUtils.formatDistance(scp.pWellOffset));
		nWellHeight.setText(TextUtils.formatDistance(scp.nWellHeight));
		nWellOffset.setText(TextUtils.formatDistance(scp.nWellOffset));

		// the Design Rules
		viaSize.setText(TextUtils.formatDistance(scp.viaSize));
		minMetalSpacing.setText(TextUtils.formatDistance(scp.minMetalSpacing));
		feedThruSize.setText(TextUtils.formatDistance(scp.feedThruSize));
		minPortDist.setText(TextUtils.formatDistance(scp.minPortDistance));
		minActiveDist.setText(TextUtils.formatDistance(scp.minActiveDistance));
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Selection tab.
	 */
    @Override
	public void term()
	{
        SilComp.SilCompPrefs scp = new SilComp.SilCompPrefs(false);

		// layout
        scp.numRows = TextUtils.atoi(numRows.getText());
		// arcs
        scp.horizRoutingArc = (String)horizRoutingArc.getSelectedItem();
        scp.horizArcWidth = TextUtils.atofDistance(horizWireWidth.getText());
        scp.vertRoutingArc = (String)vertRoutingArc.getSelectedItem();
        scp.vertArcWidth = TextUtils.atofDistance(vertWireWidth.getText());
        scp.powerWireWidth = TextUtils.atofDistance(powerWidth.getText());
        scp.mainPowerWireWidth = TextUtils.atofDistance(mainPowerWidth.getText());
        scp.mainPowerArc = (String)mainPowerArc.getSelectedItem();
		// wells
        scp.pWellHeight = TextUtils.atofDistance(pWellHeight.getText());
        scp.pWellOffset = TextUtils.atofDistance(pWellOffset.getText());
        scp.nWellHeight = TextUtils.atofDistance(nWellHeight.getText());
        scp.nWellOffset = TextUtils.atofDistance(nWellOffset.getText());
		// design rules
        scp.viaSize = TextUtils.atofDistance(viaSize.getText());
        scp.minMetalSpacing = TextUtils.atofDistance(minMetalSpacing.getText());
        scp.feedThruSize = TextUtils.atofDistance(feedThruSize.getText());
        scp.minPortDistance = TextUtils.atofDistance(minPortDist.getText());
        scp.minActiveDistance = TextUtils.atofDistance(minActiveDist.getText());

        putPrefs(scp);
	}

	/**
	 * Method called when the factory reset is requested.
	 */
    @Override
	public void reset()
    {
        putPrefs(new SilComp.SilCompPrefs(true));
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        siliconCompiler = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        numRows = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        horizRoutingArc = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        horizWireWidth = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        vertWireWidth = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        vertRoutingArc = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        powerWidth = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        mainPowerWidth = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        mainPowerArc = new javax.swing.JComboBox();
        jSeparator1 = new javax.swing.JSeparator();
        jPanel6 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel20 = new javax.swing.JLabel();
        pWellHeight = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        pWellOffset = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        nWellHeight = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        nWellOffset = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        viaSize = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        minMetalSpacing = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        feedThruSize = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        minPortDist = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        minActiveDist = new javax.swing.JTextField();
        jSeparator2 = new javax.swing.JSeparator();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Silicon Compiler Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        siliconCompiler.setLayout(new java.awt.GridBagLayout());

        jPanel5.setLayout(new javax.swing.BoxLayout(jPanel5, javax.swing.BoxLayout.Y_AXIS));

        jPanel3.setLayout(new java.awt.GridBagLayout());

        jPanel3.setBorder(new javax.swing.border.TitledBorder("Layout"));
        jLabel11.setText("Number of rows of cells:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel3.add(jLabel11, gridBagConstraints);

        numRows.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel3.add(numRows, gridBagConstraints);

        jPanel5.add(jPanel3);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(new javax.swing.border.TitledBorder("Arcs"));
        jLabel2.setText("Horizontal routing arc:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel1.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel1.add(horizRoutingArc, gridBagConstraints);

        jLabel1.setText("Horizontal wire width:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel1.add(jLabel1, gridBagConstraints);

        horizWireWidth.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel1.add(horizWireWidth, gridBagConstraints);

        jLabel3.setText("Vertical routing arc:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel1.add(jLabel3, gridBagConstraints);

        vertWireWidth.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel1.add(vertWireWidth, gridBagConstraints);

        jLabel4.setText("Vertical wire width:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel1.add(jLabel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel1.add(vertRoutingArc, gridBagConstraints);

        jLabel5.setText("Power wire width:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLabel5, gridBagConstraints);

        powerWidth.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(powerWidth, gridBagConstraints);

        jLabel9.setText("Main power wire width:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLabel9, gridBagConstraints);

        mainPowerWidth.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(mainPowerWidth, gridBagConstraints);

        jLabel10.setText("Main power arc:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLabel10, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(mainPowerArc, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(jSeparator1, gridBagConstraints);

        jPanel5.add(jPanel1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        siliconCompiler.add(jPanel5, gridBagConstraints);

        jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6, javax.swing.BoxLayout.Y_AXIS));

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel2.setBorder(new javax.swing.border.TitledBorder("Well"));
        jLabel20.setText("P-Well height (0 for none):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel2.add(jLabel20, gridBagConstraints);

        pWellHeight.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel2.add(pWellHeight, gridBagConstraints);

        jLabel6.setText("P-Well offset from bottom:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel2.add(jLabel6, gridBagConstraints);

        pWellOffset.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel2.add(pWellOffset, gridBagConstraints);

        jLabel7.setText("N-Well height (0 for none):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel2.add(jLabel7, gridBagConstraints);

        nWellHeight.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel2.add(nWellHeight, gridBagConstraints);

        jLabel8.setText("N-Well offset from top:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel2.add(jLabel8, gridBagConstraints);

        nWellOffset.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel2.add(nWellOffset, gridBagConstraints);

        jPanel6.add(jPanel2);

        jPanel4.setLayout(new java.awt.GridBagLayout());

        jPanel4.setBorder(new javax.swing.border.TitledBorder("Design Rules"));
        jLabel12.setText("Via size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel4.add(jLabel12, gridBagConstraints);

        viaSize.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel4.add(viaSize, gridBagConstraints);

        jLabel13.setText("Minimum metal spacing:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel4.add(jLabel13, gridBagConstraints);

        minMetalSpacing.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel4.add(minMetalSpacing, gridBagConstraints);

        jLabel14.setText("Routing feed-through size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel4.add(jLabel14, gridBagConstraints);

        feedThruSize.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel4.add(feedThruSize, gridBagConstraints);

        jLabel15.setText("Routing min. port distance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel4.add(jLabel15, gridBagConstraints);

        minPortDist.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel4.add(minPortDist, gridBagConstraints);

        jLabel16.setText("Routing min. active distance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel4.add(jLabel16, gridBagConstraints);

        minActiveDist.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel4.add(minActiveDist, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel4.add(jSeparator2, gridBagConstraints);

        jPanel6.add(jPanel4);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        siliconCompiler.add(jPanel6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(siliconCompiler, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField feedThruSize;
    private javax.swing.JComboBox horizRoutingArc;
    private javax.swing.JTextField horizWireWidth;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JComboBox mainPowerArc;
    private javax.swing.JTextField mainPowerWidth;
    private javax.swing.JTextField minActiveDist;
    private javax.swing.JTextField minMetalSpacing;
    private javax.swing.JTextField minPortDist;
    private javax.swing.JTextField nWellHeight;
    private javax.swing.JTextField nWellOffset;
    private javax.swing.JTextField numRows;
    private javax.swing.JTextField pWellHeight;
    private javax.swing.JTextField pWellOffset;
    private javax.swing.JTextField powerWidth;
    private javax.swing.JPanel siliconCompiler;
    private javax.swing.JComboBox vertRoutingArc;
    private javax.swing.JTextField vertWireWidth;
    private javax.swing.JTextField viaSize;
    // End of variables declaration//GEN-END:variables

}
