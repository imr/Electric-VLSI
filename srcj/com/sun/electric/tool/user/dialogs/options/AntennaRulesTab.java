/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AntennaRulesTab.java
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

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.erc.ERC;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.logicaleffort.LETool;
import com.sun.electric.tool.routing.Routing;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.util.Iterator;
import java.util.HashMap;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;


/**
 * Class to handle the "Antenna Rules" tab of the Preferences dialog.
 */
public class AntennaRulesTab extends PreferencePanel
{
	private Technology curTech = Technology.getCurrent();

	/** Creates new form AntennaRulesTab */
	public AntennaRulesTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return antennaRules; }

	public String getName() { return "Antenna Rules"; }

	private JList antennaArcList;
	private DefaultListModel antennaArcListModel;
	private HashMap antennaOptions;
	private boolean antennaRatioChanging = false;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Antenna Rules tab.
	 */
	public void init()
	{
		antennaArcListModel = new DefaultListModel();
		antennaArcList = new JList(antennaArcListModel);
		antennaArcList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		antArcList.setViewportView(antennaArcList);
		antennaArcList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { antennaArcListClick(); }
		});
		antMaxRatio.getDocument().addDocumentListener(new AntennaRatioDocumentListener(this));

		antTechnology.setText(curTech.getTechName());
		antennaOptions = new HashMap();
		for(Iterator it = curTech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = (ArcProto)it.next();
			ArcProto.Function fun = ap.getFunction();
			if (!fun.isMetal() && fun != ArcProto.Function.POLY1) continue;
			double ratio = ap.getAntennaRatio();
			Pref pref = Pref.makeDoublePref(null, null, ratio);
			antennaOptions.put(ap, pref);
			antennaArcListModel.addElement(ap.describe() + " (" + ratio + ")");
		}
		antennaArcList.setSelectedIndex(0);
		antennaArcListClick();
	}

	private void antennaArcListClick()
	{
		String arcName = (String)antennaArcList.getSelectedValue();
		int spacePos = arcName.indexOf(' ');
		if (spacePos >= 0) arcName = arcName.substring(0, spacePos);
		ArcProto ap = curTech.findArcProto(arcName);
		if (ap != null)
		{
			Pref pref = (Pref)antennaOptions.get(ap);
			if (pref == null) return;
			antennaRatioChanging = true;
			antMaxRatio.setText(Double.toString(pref.getDouble()));
			antennaRatioChanging = false;
		}
	}

	private void antennaValueChanged()
	{
		if (antennaRatioChanging) return;
		String arcName = (String)antennaArcList.getSelectedValue();
		int spacePos = arcName.indexOf(' ');
		if (spacePos >= 0) arcName = arcName.substring(0, spacePos);
		ArcProto ap = curTech.findArcProto(arcName);
		if (ap == null) return;
		Pref pref = (Pref)antennaOptions.get(ap);
		if (pref == null) return;
		double ratio = TextUtils.atof(antMaxRatio.getText());
		pref.setDouble(ratio);

		int lineNo = antennaArcList.getSelectedIndex();
		antennaArcListModel.setElementAt(ap.describe() + " (" + ratio + ")", lineNo);
	}

	/**
	 * Class to handle changes to the antenna ratio field.
	 */
	private static class AntennaRatioDocumentListener implements DocumentListener
	{
		AntennaRulesTab dialog;

		AntennaRatioDocumentListener(AntennaRulesTab dialog)
		{
			this.dialog = dialog;
		}

		public void changedUpdate(DocumentEvent e) { dialog.antennaValueChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.antennaValueChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.antennaValueChanged(); }
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Antenna Rules tab.
	 */
	public void term()
	{
		for(Iterator it = antennaOptions.keySet().iterator(); it.hasNext(); )
		{
			ArcProto ap = (ArcProto)it.next();
			Pref pref = (Pref)antennaOptions.get(ap);
			if (pref.getDoubleFactoryValue() != pref.getDouble())
				ap.setAntennaRatio(pref.getDouble());
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

        antennaRules = new javax.swing.JPanel();
        jLabel66 = new javax.swing.JLabel();
        antTechnology = new javax.swing.JLabel();
        antArcList = new javax.swing.JScrollPane();
        jLabel68 = new javax.swing.JLabel();
        antMaxRatio = new javax.swing.JTextField();

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

        antennaRules.setLayout(new java.awt.GridBagLayout());

        jLabel66.setText("Arcs in technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        antennaRules.add(jLabel66, gridBagConstraints);

        antTechnology.setText("mocmos");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        antennaRules.add(antTechnology, gridBagConstraints);

        antArcList.setPreferredSize(new java.awt.Dimension(300, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        antennaRules.add(antArcList, gridBagConstraints);

        jLabel68.setText("Maximum antenna ratio:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        antennaRules.add(jLabel68, gridBagConstraints);

        antMaxRatio.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        antennaRules.add(antMaxRatio, gridBagConstraints);

        getContentPane().add(antennaRules, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane antArcList;
    private javax.swing.JTextField antMaxRatio;
    private javax.swing.JLabel antTechnology;
    private javax.swing.JPanel antennaRules;
    private javax.swing.JLabel jLabel66;
    private javax.swing.JLabel jLabel68;
    // End of variables declaration//GEN-END:variables
	
}
