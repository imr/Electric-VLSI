/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SelectObject.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Comparator;
import java.util.Collections;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;


/**
 * Class to handle the "Search and Replace" dialog.
 */
public class SelectObject extends EDialog
{
	private static final int NODES   = 1;
	private static final int ARCS    = 2;
	private static final int EXPORTS = 3;
	private static final int NETS    = 4;
	private static int what = NODES;
	private Cell cell;
	private JList list;
	private DefaultListModel model;

	public static void selectObjectDialog()
	{
		SelectObject dialog = new SelectObject(TopLevel.getCurrentJFrame(), false);
		dialog.show();
	}

	/** Creates new form Search and Replace */
	private SelectObject(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
		getRootPane().setDefaultButton(done);

		switch (what)
		{
			case NODES:   nodes.setSelected(true);      break;
			case ARCS:    arcs.setSelected(true);       break;
			case EXPORTS: exports.setSelected(true);    break;
			case NETS:    networks.setSelected(true);   break;
		}

		model = new DefaultListModel();
		list = new JList(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		objectPane.setViewportView(list);
		list.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt) { listClicked(); }
		});

		done.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { closeDialog(null); }
		});

		nodes.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { buttonClicked(); }
		});
		arcs.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { buttonClicked(); }
		});
		exports.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { buttonClicked(); }
		});
		networks.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { buttonClicked(); }
		});
		buttonClicked();
	}

	protected void escapePressed() { closeDialog(null); }

	private void listClicked()
	{
		String s = (String)list.getSelectedValue();
		if (nodes.isSelected())
		{
			// find nodes
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (s.equals(ni.getName()))
				{
					Highlight.clear();
					Highlight.addElectricObject(ni, cell);
					Highlight.finished();
					return;
				}
			}
		} else if (arcs.isSelected())
		{
			// find arcs
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				if (s.equals(ai.getName()))
				{
					Highlight.clear();
					Highlight.addElectricObject(ai, cell);
					Highlight.finished();
					return;
				}
			}
		} else if (exports.isSelected())
		{
			// find exports
			for(Iterator it = cell.getPorts(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				if (s.equals(pp.getName()))
				{
					Highlight.clear();
					Highlight.addText(pp, cell, null, null);
					Highlight.finished();
					return;
				}
			}
		} else
		{
			// find networks
			Netlist netlist = cell.getUserNetlist();
			for(Iterator it = netlist.getNetworks(); it.hasNext(); )
			{
				JNetwork net = (JNetwork)it.next();
				String netName = net.describe();
				if (netName.length() == 0) continue;
				if (s.equals(netName))
				{
					Highlight.clear();
					Highlight.addNetwork(net, cell);
					Highlight.finished();
					return;
				}
			}
		}
	}

	private void buttonClicked()
	{
		model.clear();
		cell = WindowFrame.getCurrentCell();
		if (cell == null) return;
		List allNames = new ArrayList();
		if (nodes.isSelected())
		{
			// show all nodes
			what = NODES;
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				allNames.add(ni.getName());
			}
		} else if (arcs.isSelected())
		{
			// show all arcs
			what = ARCS;
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				allNames.add(ai.getName());
			}
		} else if (exports.isSelected())
		{
			// show all exports
			what = EXPORTS;
			for(Iterator it = cell.getPorts(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				allNames.add(pp.getName());
			}
		} else
		{
			// show all networks
			what = NETS;
			Netlist netlist = cell.getUserNetlist();
			for(Iterator it = netlist.getNetworks(); it.hasNext(); )
			{
				JNetwork net = (JNetwork)it.next();
				String netName = net.describe();
				if (netName.length() == 0) continue;
				allNames.add(netName);
			}
		}
		Collections.sort(allNames, new SortStringsInsensitive());
		for(Iterator it = allNames.iterator(); it.hasNext(); )
		{
			String s = (String)it.next();
			model.addElement(s);
		}
	}

	private static class SortStringsInsensitive implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			String s1 = (String)o1;
			String s2 = (String)o2;
			return TextUtils.nameSameNumeric(s1, s2);
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

        whatGroup = new javax.swing.ButtonGroup();
        done = new javax.swing.JButton();
        objectPane = new javax.swing.JScrollPane();
        nodes = new javax.swing.JRadioButton();
        exports = new javax.swing.JRadioButton();
        arcs = new javax.swing.JRadioButton();
        networks = new javax.swing.JRadioButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Select Object");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        done.setText("Done");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(done, gridBagConstraints);

        objectPane.setMinimumSize(new java.awt.Dimension(200, 200));
        objectPane.setPreferredSize(new java.awt.Dimension(200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(objectPane, gridBagConstraints);

        nodes.setText("Nodes");
        whatGroup.add(nodes);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(nodes, gridBagConstraints);

        exports.setText("Exports");
        whatGroup.add(exports);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(exports, gridBagConstraints);

        arcs.setText("Arcs");
        whatGroup.add(arcs);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(arcs, gridBagConstraints);

        networks.setText("Networks");
        whatGroup.add(networks);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(networks, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton arcs;
    private javax.swing.JButton done;
    private javax.swing.JRadioButton exports;
    private javax.swing.JRadioButton networks;
    private javax.swing.JRadioButton nodes;
    private javax.swing.JScrollPane objectPane;
    private javax.swing.ButtonGroup whatGroup;
    // End of variables declaration//GEN-END:variables
}
