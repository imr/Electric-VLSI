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
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.sc.SilComp;

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
	}

	public JPanel getPanel() { return siliconCompiler; }

	public String getName() { return "Silicon Compiler"; }

	private int initialNumRows;
	private String initialHorizRoutingArc;
	private double initialHorizArcWidth;
	private String initialVertRoutingArc;
	private double initialVertArcWidth;
	private double initialPowerWidth;
	private double initialMainPowerWidth;
	private String initialMainPowerArc;
	private double initialPWellHeight;
	private double initialPWellOffset;
	private double initialNWellHeight;
	private double initialNWellOffset;
	private double initialViaSize;
	private double initialMinMetalSpacing;
	private double initialFeedThruSize;
	private double initialMinPortDist;
	private double initialMinActiveDist;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Selection tab.
	 */
	public void init()
	{
		// the layout information
		initialNumRows = SilComp.getNumberOfRows();
		numRows.setText(Integer.toString(initialNumRows));

		// the arc information
		for(Iterator it = Technology.getCurrent().getArcs(); it.hasNext(); )
		{
			ArcProto ap = (ArcProto)it.next();
			horizRoutingArc.addItem(ap.getName());
			vertRoutingArc.addItem(ap.getName());
			mainPowerArc.addItem(ap.getName());
		}
		initialHorizRoutingArc = SilComp.getHorizRoutingArc();
		horizRoutingArc.setSelectedItem(initialHorizRoutingArc);
		initialHorizArcWidth = SilComp.getHorizArcWidth();
		horizWireWidth.setText(TextUtils.formatDouble(initialHorizArcWidth));

		initialVertRoutingArc = SilComp.getVertRoutingArc();
		vertRoutingArc.setSelectedItem(initialVertRoutingArc);
		initialVertArcWidth = SilComp.getVertArcWidth();
		vertWireWidth.setText(TextUtils.formatDouble(initialVertArcWidth));

		initialPowerWidth = SilComp.getPowerWireWidth();
		powerWidth.setText(TextUtils.formatDouble(initialPowerWidth));
		initialMainPowerWidth = SilComp.getMainPowerWireWidth();
		mainPowerWidth.setText(TextUtils.formatDouble(initialMainPowerWidth));
		initialMainPowerArc = SilComp.getMainPowerArc();
		mainPowerArc.setSelectedItem(initialMainPowerArc);

		// the Well information
		initialPWellHeight = SilComp.getPWellHeight();
		pWellHeight.setText(TextUtils.formatDouble(initialPWellHeight));
		initialPWellOffset = SilComp.getPWellOffset();
		pWellOffset.setText(TextUtils.formatDouble(initialPWellOffset));
		initialNWellHeight = SilComp.getNWellHeight();
		nWellHeight.setText(TextUtils.formatDouble(initialNWellHeight));
		initialNWellOffset = SilComp.getNWellOffset();
		nWellOffset.setText(TextUtils.formatDouble(initialNWellOffset));

		// the Design Rules
		initialViaSize = SilComp.getViaSize();
		viaSize.setText(TextUtils.formatDouble(initialViaSize));
		initialMinMetalSpacing = SilComp.getMinMetalSpacing();
		minMetalSpacing.setText(TextUtils.formatDouble(initialMinMetalSpacing));
		initialFeedThruSize = SilComp.getFeedThruSize();
		feedThruSize.setText(TextUtils.formatDouble(initialFeedThruSize));
		initialMinPortDist = SilComp.getMinPortDistance();
		minPortDist.setText(TextUtils.formatDouble(initialMinPortDist));
		initialMinActiveDist = SilComp.getMinActiveDistance();
		minActiveDist.setText(TextUtils.formatDouble(initialMinActiveDist));
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Selection tab.
	 */
	public void term()
	{
		// layout
		int currentNumRows = TextUtils.atoi(numRows.getText());
		if (currentNumRows != initialNumRows)
			SilComp.setNumberOfRows(currentNumRows);

		// arcs
		String currentHorizRoutingArc = (String)horizRoutingArc.getSelectedItem();
		if (!currentHorizRoutingArc.equals(initialHorizRoutingArc))
			SilComp.setHorizRoutingArc(currentHorizRoutingArc);
		double currentHorizArcWidth = TextUtils.atof(horizWireWidth.getText());
		if (currentHorizArcWidth != initialHorizArcWidth)
			SilComp.setHorizArcWidth(currentHorizArcWidth);

		String currentVertRoutingArc = (String)vertRoutingArc.getSelectedItem();
		if (!currentVertRoutingArc.equals(initialVertRoutingArc))
			SilComp.setVertRoutingArc(currentVertRoutingArc);
		double currentVertArcWidth = TextUtils.atof(vertWireWidth.getText());
		if (currentVertArcWidth != initialVertArcWidth)
			SilComp.setVertArcWidth(currentVertArcWidth);

		double currentPowerWidth = TextUtils.atof(powerWidth.getText());
		if (currentPowerWidth != initialPowerWidth)
			SilComp.setPowerWireWidth(currentPowerWidth);
		double currentMainPowerWidth = TextUtils.atof(mainPowerWidth.getText());
		if (currentMainPowerWidth != initialMainPowerWidth)
			SilComp.setMainPowerWireWidth(currentMainPowerWidth);
		String currentMainPowerArc = (String)mainPowerArc.getSelectedItem();
		if (!currentMainPowerArc.equals(initialMainPowerArc))
			SilComp.setMainPowerArc(currentMainPowerArc);

		// wells
		double currentPWellHeight = TextUtils.atof(pWellHeight.getText());
		if (currentPWellHeight != initialPWellHeight)
			SilComp.setPWellHeight(currentPWellHeight);
		double currentPWellOffset = TextUtils.atof(pWellOffset.getText());
		if (currentPWellOffset != initialPWellOffset)
			SilComp.setPWellOffset(currentPWellOffset);
		double currentNWellHeight = TextUtils.atof(nWellHeight.getText());
		if (currentNWellHeight != initialNWellHeight)
			SilComp.setNWellHeight(currentNWellHeight);
		double currentNWellOffset = TextUtils.atof(nWellOffset.getText());
		if (currentNWellOffset != initialNWellOffset)
			SilComp.setNWellOffset(currentNWellOffset);

		// design rules
		double currentViaSize = TextUtils.atof(viaSize.getText());
		if (currentViaSize != initialViaSize)
			SilComp.setViaSize(currentViaSize);
		double currentMinMetalSpacing = TextUtils.atof(minMetalSpacing.getText());
		if (currentMinMetalSpacing != initialMinMetalSpacing)
			SilComp.setMinMetalSpacing(currentMinMetalSpacing);
		double currentFeedThruSize = TextUtils.atof(feedThruSize.getText());
		if (currentFeedThruSize != initialFeedThruSize)
			SilComp.setFeedThruSize(currentFeedThruSize);
		double currentMinPortDist = TextUtils.atof(minPortDist.getText());
		if (currentMinPortDist != initialMinPortDist)
			SilComp.setMinPortDistance(currentMinPortDist);
		double currentMinActiveDist = TextUtils.atof(minActiveDist.getText());
		if (currentMinActiveDist != initialMinActiveDist)
			SilComp.setMinActiveDistance(currentMinActiveDist);
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
