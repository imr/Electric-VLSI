/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetworkTab.java
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
 * Class to handle the "Network" tab of the Preferences dialog.
 */
public class NetworkTab extends PreferencePanel
{
	/** Creates new form NetworkTab */
	public NetworkTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return network; }

	public String getName() { return "Network"; }

	private boolean netUnifyPwrGndInitial, netUnifyLikeNamedNetsInitial, netIgnoreResistorsInitial;
	private String netUnificationPrefixInitial;
	private boolean netBusBaseZeroInitial, netBusAscendingInitial;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Network tab.
	 */
	public void init()
	{
		netUnifyPwrGndInitial = Network.isUnifyPowerAndGround();
		netUnifyPwrGnd.setSelected(netUnifyPwrGndInitial);

		netUnifyLikeNamedNetsInitial = Network.isUnifyLikeNamedNets();
		netUnifyLikeNamedNets.setSelected(netUnifyLikeNamedNetsInitial);

		netIgnoreResistorsInitial = Network.isIgnoreResistors();
		netIgnoreResistors.setSelected(netIgnoreResistorsInitial);

		netUnificationPrefixInitial = Network.getUnificationPrefix();
		netUnificationPrefix.setText(netUnificationPrefixInitial);

		netBusBaseZeroInitial = Network.isBusBaseZero();
		netStartingIndex.addItem("0");
		netStartingIndex.addItem("1");
		if (!netBusBaseZeroInitial) netStartingIndex.setSelectedIndex(1);

		netBusAscendingInitial = Network.isBusAscending();
		if (netBusAscendingInitial) netAscending.setSelected(true); else
			netDescending.setSelected(true);
	}

	public void term()
	{
		boolean nowBoolean = netUnifyPwrGnd.isSelected();
		if (netUnifyPwrGndInitial != nowBoolean) Network.setUnifyPowerAndGround(nowBoolean);

		nowBoolean = netUnifyLikeNamedNets.isSelected();
		if (netUnifyLikeNamedNetsInitial != nowBoolean) Network.setUnifyLikeNamedNets(nowBoolean);

		nowBoolean = netIgnoreResistors.isSelected();
		if (netIgnoreResistorsInitial != nowBoolean) Network.setIgnoreResistors(nowBoolean);

		String nowString = netUnificationPrefix.getText();
		if (!netUnificationPrefixInitial.equals(nowString)) Network.setUnificationPrefix(nowString);

		nowBoolean = netStartingIndex.getSelectedIndex() == 0;
		if (netBusBaseZeroInitial != nowBoolean) Network.setBusBaseZero(nowBoolean);

		nowBoolean = netAscending.isSelected();
		if (netBusAscendingInitial != nowBoolean) Network.setBusAscending(nowBoolean);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        netDefaultOrder = new javax.swing.ButtonGroup();
        network = new javax.swing.JPanel();
        netUnifyPwrGnd = new javax.swing.JCheckBox();
        netUnifyLikeNamedNets = new javax.swing.JCheckBox();
        netIgnoreResistors = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        netUnificationPrefix = new javax.swing.JTextField();
        jSeparator5 = new javax.swing.JSeparator();
        jLabel26 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        netStartingIndex = new javax.swing.JComboBox();
        jLabel28 = new javax.swing.JLabel();
        netAscending = new javax.swing.JRadioButton();
        netDescending = new javax.swing.JRadioButton();
        jLabel29 = new javax.swing.JLabel();

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

        network.setLayout(new java.awt.GridBagLayout());

        netUnifyPwrGnd.setText("Unify Power and Ground");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        network.add(netUnifyPwrGnd, gridBagConstraints);

        netUnifyLikeNamedNets.setText("Unify all like-named nets");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        network.add(netUnifyLikeNamedNets, gridBagConstraints);

        netIgnoreResistors.setText("Ignore Resistors");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        network.add(netIgnoreResistors, gridBagConstraints);

        jLabel3.setText("Unify Networks that start with:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        network.add(jLabel3, gridBagConstraints);

        netUnificationPrefix.setColumns(20);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 40, 4, 0);
        network.add(netUnificationPrefix, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        network.add(jSeparator5, gridBagConstraints);

        jLabel26.setText("For busses:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        network.add(jLabel26, gridBagConstraints);

        jLabel27.setText("Default starting index:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        network.add(jLabel27, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        network.add(netStartingIndex, gridBagConstraints);

        jLabel28.setText("Default order:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        network.add(jLabel28, gridBagConstraints);

        netAscending.setText("Ascending (0:N)");
        netDefaultOrder.add(netAscending);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 40, 4, 0);
        network.add(netAscending, gridBagConstraints);

        netDescending.setText("Descending (N:0)");
        netDefaultOrder.add(netDescending);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 40, 4, 0);
        network.add(netDescending, gridBagConstraints);

        jLabel29.setText("Network numbering:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        network.add(jLabel29, gridBagConstraints);

        getContentPane().add(network, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JRadioButton netAscending;
    private javax.swing.ButtonGroup netDefaultOrder;
    private javax.swing.JRadioButton netDescending;
    private javax.swing.JCheckBox netIgnoreResistors;
    private javax.swing.JComboBox netStartingIndex;
    private javax.swing.JTextField netUnificationPrefix;
    private javax.swing.JCheckBox netUnifyLikeNamedNets;
    private javax.swing.JCheckBox netUnifyPwrGnd;
    private javax.swing.JPanel network;
    // End of variables declaration//GEN-END:variables
	
}
