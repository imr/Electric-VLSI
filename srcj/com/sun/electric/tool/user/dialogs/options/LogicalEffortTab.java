/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LogicalEffortTab.java
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
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.logicaleffort.LETool;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * Class to handle the "Logical Effort" tab of the Preferences dialog.
 */
public class LogicalEffortTab extends PreferencePanel
{
	/** Creates new form LogicalEffortTab */
	public LogicalEffortTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return logicalEffort; }

	public String getName() { return "LogicalEffort"; }

	private JList leArcList;
	private DefaultListModel leArcListModel;
	private HashMap leArcOptions;
	private boolean leUseLocalSettingsInitial, leDisplayIntermediateCapsInitial, leHighlightComponentsInitial;
	private double leGlobalFanOutInitial, leConvergenceInitial;
	private int leMaxIterationsInitial;
	private double leGateCapacitanceInitial, leDefaultWireCapRatioInitial, leDiffToGateCapRatioInitial;
	private double leKeeperSizeRatioInitial;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Logical Effort tab.
	 */
	public void init()
	{
		leHighlightComponentsInitial = LETool.isHighlightComponents();
		leHighlightComponents.setSelected(leHighlightComponentsInitial);

		leUseLocalSettingsInitial = LETool.isUseLocalSettings();
		leUseLocalSettings.setSelected(leUseLocalSettingsInitial);

		leDisplayIntermediateCapsInitial = LETool.isShowIntermediateCapacitances();
		leDisplayIntermediateCaps.setSelected(leDisplayIntermediateCapsInitial);

		leGlobalFanOutInitial = LETool.getGlobalFanout();
		leGlobalFanOut.setText(TextUtils.formatDouble(leGlobalFanOutInitial));

		leConvergenceInitial = LETool.getConvergenceEpsilon();
		leConvergence.setText(TextUtils.formatDouble(leConvergenceInitial));

		leMaxIterationsInitial = LETool.getMaxIterations();
		leMaxIterations.setText(Integer.toString(leMaxIterationsInitial));

		leGateCapacitanceInitial = LETool.getGateCapacitance();
		leGateCapacitance.setText(TextUtils.formatDouble(leGateCapacitanceInitial));

		leDefaultWireCapRatioInitial = LETool.getWireRatio();
		leDefaultWireCapRatio.setText(TextUtils.formatDouble(leDefaultWireCapRatioInitial));

		leDiffToGateCapRatioInitial = LETool.getDiffAlpha();
		leDiffToGateCapRatio.setText(TextUtils.formatDouble(leDiffToGateCapRatioInitial));

		leKeeperSizeRatioInitial = LETool.getKeeperRatio();
		leKeeperSizeRatio.setText(TextUtils.formatDouble(leKeeperSizeRatioInitial));

		// make an empty list for the layer names
		leArcOptions = new HashMap();
		for(Iterator it = curTech.getArcs(); it.hasNext(); )
		{
			ArcProto arc = (ArcProto)it.next();
			leArcOptions.put(arc, Pref.makeDoublePref(null, null, LETool.tool.prefs.getDouble(arc.toString(), 0.0)));
		}
		leArcListModel = new DefaultListModel();
		leArcList = new JList(leArcListModel);
		leArcList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		leArc.setViewportView(leArcList);
		leArcList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { leArcListClick(evt); }
		});
		showArcsInTechnology(leArcListModel);
		leArcList.setSelectedIndex(0);
		leArcListClick(null);
		leWireRatio.getDocument().addDocumentListener(new ArcDocumentListener(leArcOptions, leArcList, leArcListModel, curTech));
		leHelp.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { leHelpActionPerformed(evt); }
		});
	}

	private void leHelpActionPerformed(ActionEvent evt)
	{
		System.out.println("No help yet");
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Logical Effort tab.
	 */
	public void term()
	{
        boolean nowBoolean = leUseLocalSettings.isSelected();
		if (leUseLocalSettingsInitial != nowBoolean) LETool.setUseLocalSettings(nowBoolean);

		nowBoolean = leDisplayIntermediateCaps.isSelected();
		if (leDisplayIntermediateCapsInitial != nowBoolean) LETool.setShowIntermediateCapacitances(nowBoolean);

		nowBoolean = leHighlightComponents.isSelected();
		if (leHighlightComponentsInitial != nowBoolean) LETool.setHighlightComponents(nowBoolean);

		double nowDouble = TextUtils.atof(leGlobalFanOut.getText());
		if (leGlobalFanOutInitial != nowDouble) LETool.setGlobalFanout(nowDouble);

		nowDouble = TextUtils.atof(leConvergence.getText());
		if (leConvergenceInitial != nowDouble) LETool.setConvergenceEpsilon(nowDouble);

		int nowInt = TextUtils.atoi(leMaxIterations.getText());
		if (leMaxIterationsInitial != nowInt) LETool.setMaxIterations(nowInt);

		nowDouble = TextUtils.atof(leGateCapacitance.getText());
		if (leGateCapacitanceInitial != nowDouble) LETool.setGateCapacitance(nowDouble);

		nowDouble = TextUtils.atof(leDefaultWireCapRatio.getText());
		if (leDefaultWireCapRatioInitial != nowDouble) LETool.setWireRatio(nowDouble);

		nowDouble = TextUtils.atof(leDiffToGateCapRatio.getText());
		if (leDiffToGateCapRatioInitial != nowDouble) LETool.setDiffAlpha(nowDouble);

		nowDouble = TextUtils.atof(leKeeperSizeRatio.getText());
		if (leKeeperSizeRatioInitial != nowDouble) LETool.setKeeperRatio(nowDouble);

		for(Iterator it = curTech.getArcs(); it.hasNext(); )
		{
			ArcProto arc = (ArcProto)it.next();
			Pref pref = (Pref)leArcOptions.get(arc);
			if (pref != null && pref.getDoubleFactoryValue() != pref.getDouble())
                LETool.tool.prefs.putDouble(arc.toString(), pref.getDouble());
		}
	}

	private void leArcListClick(MouseEvent evt)
	{
		String arcName = (String)leArcList.getSelectedValue();
		int firstSpace = arcName.indexOf(' ');
		if (firstSpace > 0) arcName = arcName.substring(0, firstSpace);
		ArcProto arc = curTech.findArcProto(arcName);
		Pref pref = (Pref)leArcOptions.get(arc);
		if (pref == null) return;
		leWireRatio.setText(TextUtils.formatDouble(pref.getDouble()));
	}

	/**
	 * Class to handle special changes to per-arc logical effort.
	 */
	private static class ArcDocumentListener implements DocumentListener
	{
		HashMap optionMap;
		JList list;
		DefaultListModel model;
		Technology tech;

		ArcDocumentListener(HashMap optionMap, JList list, DefaultListModel model, Technology tech)
		{
			this.optionMap = optionMap;
			this.list = list;
			this.model = model;
			this.tech = tech;
		}

		private void change(DocumentEvent e)
		{
			// get the currently selected layer
			String arcName = (String)list.getSelectedValue();
			int firstSpace = arcName.indexOf(' ');
			if (firstSpace > 0) arcName = arcName.substring(0, firstSpace);
			ArcProto arc = tech.findArcProto(arcName);
			Pref pref = (Pref)optionMap.get(arc);
			if (pref == null) return;

			// get the typed value
			Document doc = e.getDocument();
			int len = doc.getLength();
			String text;
			try
			{
				text = doc.getText(0, len);
			} catch (BadLocationException ex) { return; }
			double v = TextUtils.atof(text);

			// update the option
			pref.setDouble(v);
			int index = list.getSelectedIndex();
			String newLine = arc.getName() + " (" + v + ")";
			model.setElementAt(newLine, index);
		}

		public void changedUpdate(DocumentEvent e) { change(e); }
		public void insertUpdate(DocumentEvent e) { change(e); }
		public void removeUpdate(DocumentEvent e) { change(e); }
	}

	private void showArcsInTechnology(DefaultListModel model)
	{
		model.clear();
		for(Iterator it = curTech.getArcs(); it.hasNext(); )
		{
			ArcProto arc = (ArcProto)it.next();
			model.addElement(arc.getName() + " (" + ((Pref)leArcOptions.get(arc)).getDouble() + ")");
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

        logicalEffort = new javax.swing.JPanel();
        leArc = new javax.swing.JScrollPane();
        jLabel4 = new javax.swing.JLabel();
        leDisplayIntermediateCaps = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        leHelp = new javax.swing.JButton();
        jLabel15 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        leUseLocalSettings = new javax.swing.JCheckBox();
        leHighlightComponents = new javax.swing.JCheckBox();
        leGlobalFanOut = new javax.swing.JTextField();
        leConvergence = new javax.swing.JTextField();
        leMaxIterations = new javax.swing.JTextField();
        leGateCapacitance = new javax.swing.JTextField();
        leDefaultWireCapRatio = new javax.swing.JTextField();
        leDiffToGateCapRatio = new javax.swing.JTextField();
        leKeeperSizeRatio = new javax.swing.JTextField();
        leWireRatio = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();

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

        logicalEffort.setLayout(new java.awt.GridBagLayout());

        leArc.setMinimumSize(new java.awt.Dimension(100, 100));
        leArc.setPreferredSize(new java.awt.Dimension(100, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(leArc, gridBagConstraints);

        jLabel4.setText("Global Fan-Out (step-up):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(jLabel4, gridBagConstraints);

        leDisplayIntermediateCaps.setText("Display intermediate capacitances");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(leDisplayIntermediateCaps, gridBagConstraints);

        jLabel5.setText("Wire ratio for each layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(jLabel5, gridBagConstraints);

        jLabel14.setText("Convergence epsilon:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(jLabel14, gridBagConstraints);

        leHelp.setText("Help");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(leHelp, gridBagConstraints);

        jLabel15.setText("Maximum number of iterations:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(jLabel15, gridBagConstraints);

        jLabel20.setText("Gate capacitance (fF/Lambda):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(jLabel20, gridBagConstraints);

        jLabel22.setText("Default wire cap ratio (Cwire / Cgate):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(jLabel22, gridBagConstraints);

        jLabel23.setText("Diffusion to gate cap ratio (alpha) :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(jLabel23, gridBagConstraints);

        jLabel25.setText("Keeper size ratio (keeper size / driver size):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(jLabel25, gridBagConstraints);

        leUseLocalSettings.setText("Use Local (cell) LE Settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(leUseLocalSettings, gridBagConstraints);

        leHighlightComponents.setText("Highlight components");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(leHighlightComponents, gridBagConstraints);

        leGlobalFanOut.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(leGlobalFanOut, gridBagConstraints);

        leConvergence.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(leConvergence, gridBagConstraints);

        leMaxIterations.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(leMaxIterations, gridBagConstraints);

        leGateCapacitance.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(leGateCapacitance, gridBagConstraints);

        leDefaultWireCapRatio.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(leDefaultWireCapRatio, gridBagConstraints);

        leDiffToGateCapRatio.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(leDiffToGateCapRatio, gridBagConstraints);

        leKeeperSizeRatio.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(leKeeperSizeRatio, gridBagConstraints);

        leWireRatio.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        logicalEffort.add(leWireRatio, gridBagConstraints);

        jLabel24.setText("New Value:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        logicalEffort.add(jLabel24, gridBagConstraints);

        getContentPane().add(logicalEffort, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane leArc;
    private javax.swing.JTextField leConvergence;
    private javax.swing.JTextField leDefaultWireCapRatio;
    private javax.swing.JTextField leDiffToGateCapRatio;
    private javax.swing.JCheckBox leDisplayIntermediateCaps;
    private javax.swing.JTextField leGateCapacitance;
    private javax.swing.JTextField leGlobalFanOut;
    private javax.swing.JButton leHelp;
    private javax.swing.JCheckBox leHighlightComponents;
    private javax.swing.JTextField leKeeperSizeRatio;
    private javax.swing.JTextField leMaxIterations;
    private javax.swing.JCheckBox leUseLocalSettings;
    private javax.swing.JTextField leWireRatio;
    private javax.swing.JPanel logicalEffort;
    // End of variables declaration//GEN-END:variables
	
}
