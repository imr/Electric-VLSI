/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PreferencesFrame.java
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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.dialogs.options.*;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;

/**
 * Class to handle the "PreferencesFrame" dialog.
 */
public class PreferencesFrame extends EDialog
{
	private JSplitPane splitPane;
	private JTree optionTree;
	JButton cancel;
	JButton ok;

	List optionPanes = new ArrayList();

	/** The name of the current tab in this dialog. */		private static String currentTabName = "General";
	/** The name of the current section in this dialog. */	private static String currentSectionName = "General ";

	/**
	 * This method implements the command to show the PreferencesFrame dialog.
	 */
	public static void preferencesCommand()
	{
		PreferencesFrame dialog = new PreferencesFrame(TopLevel.getCurrentJFrame(), true);
		dialog.setVisible(true);
	}

	/** Creates new form PreferencesFrame */
	public PreferencesFrame(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		getContentPane().setLayout(new java.awt.GridBagLayout());
		setTitle("Preferences");
		setName("");
		addWindowListener(new java.awt.event.WindowAdapter()
		{
			public void windowClosing(java.awt.event.WindowEvent evt)
			{
				closeDialog(evt);
			}
		});

		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Options");
		DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
		optionTree = new JTree(treeModel);
		TreeHandler handler = new TreeHandler(this);
		optionTree.addMouseListener(handler);
		optionTree.addTreeExpansionListener(handler);


		// the "General" section of the Preferences
		DefaultMutableTreeNode generalSet = new DefaultMutableTreeNode("General ");
		rootNode.add(generalSet);

		GeneralTab gt = new GeneralTab(parent, modal);
		optionPanes.add(gt);
		generalSet.add(new DefaultMutableTreeNode(gt.getName()));

		SelectionTab st = new SelectionTab(parent, modal);
		optionPanes.add(st);
		generalSet.add(new DefaultMutableTreeNode(st.getName()));

        TopLevel top = (TopLevel)TopLevel.getCurrentJFrame();
        if ((top != null) && (top.getTheMenuBar() != null)) {
            EditKeyBindings keys = new EditKeyBindings(top.getTheMenuBar(), parent, modal);
            optionPanes.add(keys);
            generalSet.add(new DefaultMutableTreeNode(keys.getName()));
        }

		NewNodesTab nnt = new NewNodesTab(parent, modal);
		optionPanes.add(nnt);
		generalSet.add(new DefaultMutableTreeNode(nnt.getName()));

		NewArcsTab nat = new NewArcsTab(parent, modal);
		optionPanes.add(nat);
		generalSet.add(new DefaultMutableTreeNode(nat.getName()));

		CopyrightTab cot = new CopyrightTab(parent, modal);
		optionPanes.add(cot);
		generalSet.add(new DefaultMutableTreeNode(cot.getName()));

		PrintingTab prt = new PrintingTab(parent, modal);
		optionPanes.add(prt);
		generalSet.add(new DefaultMutableTreeNode(prt.getName()));


		// the "Display" section of the Preferences
		DefaultMutableTreeNode displaySet = new DefaultMutableTreeNode("Display ");
		rootNode.add(displaySet);

		LayersTab lt = new LayersTab(parent, modal);
		optionPanes.add(lt);
		displaySet.add(new DefaultMutableTreeNode(lt.getName()));

		ColorsTab ct = new ColorsTab(parent, modal);
		optionPanes.add(ct);
		displaySet.add(new DefaultMutableTreeNode(ct.getName()));

		TextTab txtt = new TextTab(parent, modal);
		optionPanes.add(txtt);
		displaySet.add(new DefaultMutableTreeNode(txtt.getName()));

		GridAndAlignmentTab gat = new GridAndAlignmentTab(parent, modal);
		optionPanes.add(gat);
		displaySet.add(new DefaultMutableTreeNode(gat.getName()));

		PortsAndExportsTab pet = new PortsAndExportsTab(parent, modal);
		optionPanes.add(pet);
		displaySet.add(new DefaultMutableTreeNode(pet.getName()));

		FrameTab ft = new FrameTab(parent, modal);
		optionPanes.add(ft);
		displaySet.add(new DefaultMutableTreeNode(ft.getName()));

		ThreeDTab tdt = new ThreeDTab(parent, modal);
		optionPanes.add(tdt);
		displaySet.add(new DefaultMutableTreeNode(tdt.getName()));


		// the "I/O" section of the Preferences
		DefaultMutableTreeNode ioSet = new DefaultMutableTreeNode("I/O ");
		rootNode.add(ioSet);

		CIFTab cit = new CIFTab(parent, modal);
		optionPanes.add(cit);
		ioSet.add(new DefaultMutableTreeNode(cit.getName()));

		GDSTab gdt = new GDSTab(parent, modal);
		optionPanes.add(gdt);
		ioSet.add(new DefaultMutableTreeNode(gdt.getName()));

		EDIFTab edt = new EDIFTab(parent, modal);
		optionPanes.add(edt);
		ioSet.add(new DefaultMutableTreeNode(edt.getName()));

		DEFTab det = new DEFTab(parent, modal);
		optionPanes.add(det);
		ioSet.add(new DefaultMutableTreeNode(det.getName()));

		CDLTab cdt = new CDLTab(parent, modal);
		optionPanes.add(cdt);
		ioSet.add(new DefaultMutableTreeNode(cdt.getName()));

		DXFTab dxt = new DXFTab(parent, modal);
		optionPanes.add(dxt);
		ioSet.add(new DefaultMutableTreeNode(dxt.getName()));

		SUETab sut = new SUETab(parent, modal);
		optionPanes.add(sut);
		ioSet.add(new DefaultMutableTreeNode(sut.getName()));

		LibraryTab lit = new LibraryTab(parent, modal);
		optionPanes.add(lit);
		ioSet.add(new DefaultMutableTreeNode(lit.getName()));


		// the "Tools" section of the Preferences
		DefaultMutableTreeNode toolSet = new DefaultMutableTreeNode("Tools ");
		rootNode.add(toolSet);

		AntennaRulesTab art = new AntennaRulesTab(parent, modal);
		optionPanes.add(art);
		toolSet.add(new DefaultMutableTreeNode(art.getName()));

		CompactionTab comt = new CompactionTab(parent, modal);
		optionPanes.add(comt);
		toolSet.add(new DefaultMutableTreeNode(comt.getName()));

		DRCTab drct = new DRCTab(parent, modal);
		optionPanes.add(drct);
		toolSet.add(new DefaultMutableTreeNode(drct.getName()));

		FastHenryTab fht = new FastHenryTab(parent, modal);
		optionPanes.add(fht);
		toolSet.add(new DefaultMutableTreeNode(fht.getName()));

		LogicalEffortTab let = new LogicalEffortTab(parent, modal);
		optionPanes.add(let);
		toolSet.add(new DefaultMutableTreeNode(let.getName()));

		NCCTab nct = new NCCTab(parent, modal);
		optionPanes.add(nct);
		toolSet.add(new DefaultMutableTreeNode(nct.getName()));

		NetworkTab net = new NetworkTab(parent, modal);
		optionPanes.add(net);
		toolSet.add(new DefaultMutableTreeNode(net.getName()));

		RoutingTab rot = new RoutingTab(parent, modal);
		optionPanes.add(rot);
		toolSet.add(new DefaultMutableTreeNode(rot.getName()));

		SpiceTab spt = new SpiceTab(parent, modal);
		optionPanes.add(spt);
		toolSet.add(new DefaultMutableTreeNode(spt.getName()));

		VerilogTab vet = new VerilogTab(parent, modal);
		optionPanes.add(vet);
		toolSet.add(new DefaultMutableTreeNode(vet.getName()));

		WellCheckTab wct = new WellCheckTab(parent, modal);
		optionPanes.add(wct);
		toolSet.add(new DefaultMutableTreeNode(wct.getName()));


		// the "Technology" section of the Preferences
		DefaultMutableTreeNode techSet = new DefaultMutableTreeNode("Technology ");
		rootNode.add(techSet);

		TechnologyTab tect = new TechnologyTab(parent, modal);
		optionPanes.add(tect);
		techSet.add(new DefaultMutableTreeNode(tect.getName()));

		DesignRulesTab drt = new DesignRulesTab(parent, modal);
		optionPanes.add(drt);
		techSet.add(new DefaultMutableTreeNode(drt.getName()));

		UnitsTab ut = new UnitsTab(parent, modal);
		optionPanes.add(ut);
		techSet.add(new DefaultMutableTreeNode(ut.getName()));

		ScaleTab sct = new ScaleTab(parent, modal);
		optionPanes.add(sct);
		techSet.add(new DefaultMutableTreeNode(sct.getName()));

		IconTab ict = new IconTab(parent, modal);
		optionPanes.add(ict);
		techSet.add(new DefaultMutableTreeNode(ict.getName()));

		// pre-expand the tree
		TreePath topPath = optionTree.getPathForRow(0);
		optionTree.expandPath(topPath);
		topPath = optionTree.getPathForRow(1);
		optionTree.expandPath(topPath);
		topPath = optionTree.getNextMatch(currentSectionName, 0, null);
		optionTree.expandPath(topPath);

		// the left side of the options dialog: a tree
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new java.awt.GridBagLayout());

		JScrollPane scrolledTree = new JScrollPane(optionTree);

		java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.fill = java.awt.GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		leftPanel.add(scrolledTree, gbc);

		cancel = new javax.swing.JButton();
		cancel.setText("Cancel");
		cancel.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt) { cancelActionPerformed(evt); }
		});
		gbc = new java.awt.GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.insets = new java.awt.Insets(4, 4, 4, 4);
		leftPanel.add(cancel, gbc);

		ok = new javax.swing.JButton();
		ok.setText("OK");
		ok.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt) { okActionPerformed(evt); }
		});
		gbc = new java.awt.GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.insets = new java.awt.Insets(4, 4, 4, 4);
		leftPanel.add(ok, gbc);
		getRootPane().setDefaultButton(ok);

		// build options framework
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

		loadOptionPanel();
		splitPane.setLeftComponent(leftPanel);

		gbc = new java.awt.GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.fill = java.awt.GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		getContentPane().add(splitPane, gbc);

		pack();
	}

	private void cancelActionPerformed(ActionEvent evt)
	{
		closeDialog(null);
	}

	private void okActionPerformed(ActionEvent evt)
	{
		OKUpdate job = new OKUpdate(this);
	}

	private void loadOptionPanel()
	{
		for(Iterator it = optionPanes.iterator(); it.hasNext(); )
		{
			PreferencePanel ti = (PreferencePanel)it.next();
			if (ti.getName().equals(currentTabName))
			{
				if (!ti.isInited())
				{
					ti.init();
					ti.setInited();
				}
				splitPane.setRightComponent(ti.getPanel());
				return;
			}
		}
	}

	protected void escapePressed() { cancelActionPerformed(null); }

	/**
	 * Class to update primitive node information.
	 */
	private static class OKUpdate extends Job
	{
		PreferencesFrame dialog;

		protected OKUpdate(PreferencesFrame dialog)
		{
			super("Update Preferences", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
			startJob();
		}

		public boolean doIt()
		{
			for(Iterator it = dialog.optionPanes.iterator(); it.hasNext(); )
			{
				PreferencePanel ti = (PreferencePanel)it.next();
				if (ti.isInited())
					ti.term();
			}
			dialog.closeDialog(null);
			return true;
		}
	}

	private static class TreeHandler implements MouseListener, TreeExpansionListener
	{
		private PreferencesFrame dialog;

		TreeHandler(PreferencesFrame dialog) { this.dialog = dialog; }

		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}

		public void mousePressed(MouseEvent e)
		{
			TreePath currentPath = dialog.optionTree.getPathForLocation(e.getX(), e.getY());
			if (currentPath == null) return;
			dialog.optionTree.setSelectionPath(currentPath);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)currentPath.getLastPathComponent();
			currentTabName = (String)node.getUserObject();
			dialog.optionTree.expandPath(currentPath);
			if (currentTabName.endsWith(" "))
			{
				currentSectionName = currentTabName;
			} else
			{
				dialog.loadOptionPanel();
			}
			dialog.pack();
		}

		public void treeCollapsed(TreeExpansionEvent e)
		{
			dialog.pack();
		}
		public void treeExpanded(TreeExpansionEvent e)
		{
			TreePath tp = e.getPath();
			if (tp.getPathCount() == 2)
			{
				// opened a path down to the bottom: close all others
				TreePath topPath = dialog.optionTree.getPathForRow(0);
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)topPath.getLastPathComponent();
				int numChildren = node.getChildCount();
				for(int i=0; i<numChildren; i++)
				{
					DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
					TreePath descentPath = topPath.pathByAddingChild(child);
					if (!descentPath.getLastPathComponent().equals(tp.getLastPathComponent()))
						dialog.optionTree.collapsePath(descentPath);
				}
			}
			dialog.pack();
		}
	}

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)
	{
		setVisible(false);
		dispose();
	}
}
