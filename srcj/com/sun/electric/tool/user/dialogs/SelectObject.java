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

import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.KeyBindingManager;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ActionMap;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * Class to handle the "Select Object" dialog.
 */
public class SelectObject extends EDialog implements DatabaseChangeListener
{
    private static SelectObject theDialog = null;
	private static final int NODES   = 1;
	private static final int ARCS    = 2;
	private static final int EXPORTS = 3;
	private static final int NETS    = 4;
	private static int what = NODES;
	private Cell cell;
	private JList list;
	private DefaultListModel model;
    private Highlighter highlighter;

	public static void selectObjectDialog(Cell thisCell, boolean updateOnlyIfVisible)
	{
        if (theDialog == null)
        {
            if (updateOnlyIfVisible) return; // it is not previously open
            JFrame jf;
            if (TopLevel.isMDIMode())
			    jf = TopLevel.getCurrentJFrame();
            else
                jf = null;
            theDialog = new SelectObject(jf, false);
        }
        if (updateOnlyIfVisible && !theDialog.isVisible()) return; // it is not previously visible
		theDialog.setVisible(true);
		theDialog.buttonClicked(thisCell);
	}

	/** Creates new form SelectObject */
	private SelectObject(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
		getRootPane().setDefaultButton(done);
		UserInterfaceMain.addDatabaseChangeListener(this);

        switch (what)
		{
			case NODES:   nodes.setSelected(true);      break;
			case ARCS:    arcs.setSelected(true);       break;
			case EXPORTS: exports.setSelected(true);    break;
			case NETS:    networks.setSelected(true);   break;
		}

		model = new DefaultListModel();
		list = new JList(model);
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		objectPane.setViewportView(list);
		list.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { listClicked(); }
		});

		done.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { closeDialog(null); }
		});

		nodes.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { buttonClicked(null); }
		});
		arcs.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { buttonClicked(null); }
		});
		exports.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { buttonClicked(null); }
		});
		networks.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { buttonClicked(null); }
		});

		searchText.getDocument().addDocumentListener(new SelecdtObjectDocumentListener(this));

		// special case for this dialog: allow Electric quick-keys to pass-through
        TopLevel top = (TopLevel)TopLevel.getCurrentJFrame();
        if (top != null && top.getTheMenuBar() != null)
        {
        	KeyBindingManager.KeyMaps km = top.getEMenuBar().getKeyMaps();
        	InputMap im = km.getInputMap();
        	ActionMap am = km.getActionMap();
    		getRootPane().getInputMap().setParent(im);
    		getRootPane().getActionMap().setParent(am);
    		findText.getInputMap().setParent(im);
    		findText.getActionMap().setParent(am);
    		list.getInputMap().setParent(im);
    		list.getActionMap().setParent(am);
        }
		finishInitialization();
	}

	protected void escapePressed() { closeDialog(null); }

	/**
	 * Respond to database changes and reload the list.
	 * @param e database change event
	 */
	public void databaseChanged(DatabaseChangeEvent e)
	{
		if (!isVisible()) return;
		buttonClicked(null);
	}

	private void listClicked()
	{
		int [] si = list.getSelectedIndices();
		if (si.length > 0) highlighter.clear();
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted selection (network information unavailable).  Please try again");
			return;
		}
		for(int i=0; i<si.length; i++)
		{
			int index = si[i];
			String s = (String)model.get(index);
			if (nodes.isSelected())
			{
				// find nodes
				for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = it.next();
					if (s.equals(ni.getName()))
					{
						highlighter.addElectricObject(ni, cell);
						break;
					}
				}
			} else if (arcs.isSelected())
			{
				// find arcs
				for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
				{
					ArcInst ai = it.next();
					if (s.equals(ai.getName()))
					{
						highlighter.addElectricObject(ai, cell);
						break;
					}
				}
			} else if (exports.isSelected())
			{
				// find exports
				for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
				{
					Export pp = (Export)it.next();
					if (s.equals(pp.getName()))
					{
						highlighter.addText(pp, cell, Export.EXPORT_NAME);
						break;
					}
				}
			} else
			{
				// find networks
				for(Iterator<Network> it = netlist.getNetworks(); it.hasNext(); )
				{
					Network net = it.next();
					String netName = net.describe(false);
					if (netName.length() == 0) continue;
					if (s.equals(netName))
					{
						highlighter.addNetwork(net, cell);
						break;
					}
				}
			}
		}
		if (si.length > 0)
		{
			highlighter.ensureHighlightingSeen();
			highlighter.finished();
		}
	}

    /**
     * Method to load the dialog depending on cell selected.
     * It is not getCurrentCell because of down/up hierarchy calls.
     * @param thisCell
     */
	private void buttonClicked(Cell thisCell)
	{
		model.clear();
		cell = (thisCell != null) ? thisCell: WindowFrame.getCurrentCell();
		if (cell == null) return;
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;
        WindowContent wc = wf.getContent();
        if (wc == null) return;
        highlighter = wc.getHighlighter();
        if (highlighter == null) return;


		List<String> allNames = new ArrayList<String>();
		if (nodes.isSelected())
		{
			// show all nodes
			what = NODES;
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				allNames.add(ni.getName());
			}
		} else if (arcs.isSelected())
		{
			// show all arcs
			what = ARCS;
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				allNames.add(ai.getName());
			}
		} else if (exports.isSelected())
		{
			// show all exports
			what = EXPORTS;
			for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				allNames.add(pp.getName());
			}
		} else
		{
			// show all networks
			what = NETS;
			Netlist netlist = cell.getUserNetlist();
			for(Iterator<Network> it = netlist.getNetworks(); it.hasNext(); )
			{
				Network net = it.next();
				String netName = net.describe(false);
				if (netName.length() == 0) continue;
				allNames.add(netName);
			}
		}
		Collections.sort(allNames, TextUtils.STRING_NUMBER_ORDER);
		for(String s: allNames)
		{
			model.addElement(s);
		}
	}

	private void searchTextChanged()
	{
		String currentSearchText = searchText.getText();
		if (currentSearchText.length() == 0) return;
		for(int i=0; i<model.size(); i++)
		{
			String s = (String)model.get(i);
			if (s.startsWith(currentSearchText))
			{
				list.setSelectedIndex(i);
				list.ensureIndexIsVisible(i);
				return;
			}
		}
	}

	/**
	 * Class to handle changes to the search text field.
	 */
	private static class SelecdtObjectDocumentListener implements DocumentListener
	{
		SelectObject dialog;

		SelecdtObjectDocumentListener(SelectObject dialog)
		{
			this.dialog = dialog;
		}

		public void changedUpdate(DocumentEvent e) { dialog.searchTextChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.searchTextChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.searchTextChanged(); }
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        java.awt.GridBagConstraints gridBagConstraints;

        whatGroup = new javax.swing.ButtonGroup();
        done = new javax.swing.JButton();
        objectPane = new javax.swing.JScrollPane();
        jLabel1 = new javax.swing.JLabel();
        searchText = new javax.swing.JTextField();
        findText = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
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
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(done, gridBagConstraints);

        objectPane.setMinimumSize(new java.awt.Dimension(200, 200));
        objectPane.setPreferredSize(new java.awt.Dimension(200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(objectPane, gridBagConstraints);

        jLabel1.setText("Search:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        searchText.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(searchText, gridBagConstraints);

        findText.setText("Find");
        findText.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                findTextActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(findText, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        whatGroup.add(nodes);
        nodes.setText("Nodes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(nodes, gridBagConstraints);

        whatGroup.add(exports);
        exports.setText("Exports");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(exports, gridBagConstraints);

        whatGroup.add(arcs);
        arcs.setText("Arcs");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(arcs, gridBagConstraints);

        whatGroup.add(networks);
        networks.setText("Networks");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(networks, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(jPanel1, gridBagConstraints);

        pack();
    }
    // </editor-fold>//GEN-END:initComponents

	private void findTextActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_findTextActionPerformed
	{//GEN-HEADEREND:event_findTextActionPerformed
		String search = searchText.getText();
        int flags = Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE;
		Pattern p = Pattern.compile(search, flags);
		list.clearSelection();
		List<Integer> selected = new ArrayList<Integer>();
		for(int i = 0; i < model.getSize(); i++)
		{
			String thisLine = (String)model.getElementAt(i);
		    Matcher m = p.matcher(thisLine);
            if (m.find())
//			if (thisLine.matches(search))
				selected.add(new Integer(i));
		}
		if (selected.size() > 0)
		{
			int [] indices = new int[selected.size()];
			int i = 0;
			for(Integer iO: selected)
				indices[i++] = iO.intValue();
			list.setSelectedIndices(indices);
		}
		listClicked();
	}//GEN-LAST:event_findTextActionPerformed

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
    private javax.swing.JButton findText;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JRadioButton networks;
    private javax.swing.JRadioButton nodes;
    private javax.swing.JScrollPane objectPane;
    private javax.swing.JTextField searchText;
    private javax.swing.ButtonGroup whatGroup;
    // End of variables declaration//GEN-END:variables
}
