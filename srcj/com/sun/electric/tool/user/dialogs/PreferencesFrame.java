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

import com.sun.electric.database.text.Pref;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.output.CellModelPrefs;
import com.sun.electric.tool.routing.Routing;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.options.*;
import com.sun.electric.tool.user.help.ManualViewer;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.lang.reflect.Constructor;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * Class to handle the "PreferencesFrame" dialog.
 */
public class PreferencesFrame extends EDialog
{
	private JSplitPane splitPane;
	private JTree optionTree;
	JButton cancel;
	JButton ok;

	List<PreferencePanel> optionPanes = new ArrayList<PreferencePanel>();

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
	public PreferencesFrame(Frame parent, boolean modal)
	{
		super(parent, modal);
		getContentPane().setLayout(new GridBagLayout());
		setTitle("Preferences");
		setName("");
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent evt)
			{
				closeDialog(evt);
			}
		});

		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Preferences");
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
        if ((top != null) && (top.getEMenuBar() != null)) {
            EditKeyBindings keys = new EditKeyBindings(top.getEMenuBar(), parent, modal);
            optionPanes.add(keys);
            generalSet.add(new DefaultMutableTreeNode(keys.getName()));
        }

		NewNodesTab nnt = new NewNodesTab(parent, modal);
		optionPanes.add(nnt);
		generalSet.add(new DefaultMutableTreeNode(nnt.getName()));

		NewArcsTab nat = new NewArcsTab(parent, modal);
		optionPanes.add(nat);
		generalSet.add(new DefaultMutableTreeNode(nat.getName()));

		ProjectManagementTab pmt = new ProjectManagementTab(parent, modal);
		optionPanes.add(pmt);
		generalSet.add(new DefaultMutableTreeNode(pmt.getName()));

        CVSTab cvst = new CVSTab(parent, modal);
        optionPanes.add(cvst);
        generalSet.add(new DefaultMutableTreeNode(cvst.getName()));

		PrintingTab prt = new PrintingTab(parent, modal);
		optionPanes.add(prt);
		generalSet.add(new DefaultMutableTreeNode(prt.getName()));

        /// Open test tab only if plugin is available and in debug mode
        // Using reflection to not force the loading of test plugin
        if (Job.getDebug())
        {
            try
            {
                Class testTab = Class.forName("com.sun.electric.plugins.tests.TestTab");
                Constructor tab = testTab.getDeclaredConstructor(new Class[]{Frame.class, Boolean.class});
                PreferencePanel tesT = (PreferencePanel)tab.newInstance(new Object[] {parent, new Boolean(modal)});
                optionPanes.add(tesT);
                generalSet.add(new DefaultMutableTreeNode(tesT.getName()));
            }
            catch (Exception ex) { /* do nothing */ };
        }

		// the "Display" section of the Preferences
		DefaultMutableTreeNode displaySet = new DefaultMutableTreeNode("Display ");
		rootNode.add(displaySet);

		DisplayControlTab dct = new DisplayControlTab(parent, modal);
		optionPanes.add(dct);
		displaySet.add(new DefaultMutableTreeNode(dct.getName()));

		LayersTab lt = new LayersTab(parent, modal);
		optionPanes.add(lt);
		displaySet.add(new DefaultMutableTreeNode(lt.getName()));

		TextTab txtt = new TextTab(parent, modal);
		optionPanes.add(txtt);
		displaySet.add(new DefaultMutableTreeNode(txtt.getName()));

		SmartTextTab stxtt = new SmartTextTab(parent, modal);
		optionPanes.add(stxtt);
		displaySet.add(new DefaultMutableTreeNode(stxtt.getName()));

		GridAndAlignmentTab gat = new GridAndAlignmentTab(parent, modal);
		optionPanes.add(gat);
		displaySet.add(new DefaultMutableTreeNode(gat.getName()));

		PortsAndExportsTab pet = new PortsAndExportsTab(parent, modal);
		optionPanes.add(pet);
		displaySet.add(new DefaultMutableTreeNode(pet.getName()));

		FrameTab ft = new FrameTab(parent, modal);
		optionPanes.add(ft);
		displaySet.add(new DefaultMutableTreeNode(ft.getName()));

		ThreeDTab tdt = ThreeDTab.create3DTab(parent, modal);
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

		if (IOTool.hasDais())
		{
			DaisTab dat = new DaisTab(parent, modal);
			optionPanes.add(dat);
			ioSet.add(new DefaultMutableTreeNode(dat.getName()));
		}

		if (IOTool.hasSkill())
		{
			SkillTab skt = new SkillTab(parent, modal);
			optionPanes.add(skt);
			ioSet.add(new DefaultMutableTreeNode(skt.getName()));
		}

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

        CoverageTab covt = new CoverageTab(parent, modal);
		optionPanes.add(covt);
		toolSet.add(new DefaultMutableTreeNode(covt.getName()));

		DRCTab drct = new DRCTab(parent, modal);
		optionPanes.add(drct);
		toolSet.add(new DefaultMutableTreeNode(drct.getName()));

		FastHenryTab fht = new FastHenryTab(parent, modal);
		optionPanes.add(fht);
		toolSet.add(new DefaultMutableTreeNode(fht.getName()));

		NCCTab nct = new NCCTab(parent, modal);
		optionPanes.add(nct);
		toolSet.add(new DefaultMutableTreeNode(nct.getName()));

		NetworkTab net = new NetworkTab(parent, modal);
		optionPanes.add(net);
		toolSet.add(new DefaultMutableTreeNode(net.getName()));

		ParasiticTab parat = new ParasiticTab(parent, modal);
		optionPanes.add(parat);
		toolSet.add(new DefaultMutableTreeNode(parat.getName()));

		RoutingTab rot = new RoutingTab(parent, modal);
		optionPanes.add(rot);
		toolSet.add(new DefaultMutableTreeNode(rot.getName()));

		SiliconCompilerTab sct = new SiliconCompilerTab(parent, modal);
		optionPanes.add(sct);
		toolSet.add(new DefaultMutableTreeNode(sct.getName()));

		SimulatorsTab smt = new SimulatorsTab(parent, modal);
		optionPanes.add(smt);
		toolSet.add(new DefaultMutableTreeNode(smt.getName()));

		SpiceTab spt = new SpiceTab(parent, modal);
		optionPanes.add(spt);
		toolSet.add(new DefaultMutableTreeNode(spt.getName()));

        CellModelTab spt2 = new CellModelTab(parent, modal, CellModelPrefs.spiceModelPrefs);
        optionPanes.add(spt2);
        toolSet.add(new DefaultMutableTreeNode(spt2.getName()));

		if (Routing.hasSunRouter())
		{
			SunRouterTab srt = new SunRouterTab(parent, modal);
			optionPanes.add(srt);
			toolSet.add(new DefaultMutableTreeNode(srt.getName()));
		}

		//VerilogTab vet = new VerilogTab(parent, modal);
		CellModelTab vet = new CellModelTab(parent, modal, CellModelPrefs.verilogModelPrefs);
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

        // searching for selected node
        openSelectedPath(rootNode);

		// the left side of the preferences dialog: a tree
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new GridBagLayout());

		JScrollPane scrolledTree = new JScrollPane(optionTree);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;   gbc.weighty = 1.0;
		leftPanel.add(scrolledTree, gbc);

		JButton save = new JButton("Export");
		save.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { exportActionPerformed(); }
		});
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 1;
		gbc.insets = new Insets(4, 4, 4, 4);
		leftPanel.add(save, gbc);

		JButton restore = new JButton("Import");
		restore.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { importActionPerformed(); }
		});
		gbc = new GridBagConstraints();
		gbc.gridx = 1;   gbc.gridy = 1;
		gbc.insets = new Insets(4, 4, 4, 4);
		leftPanel.add(restore, gbc);

		JButton help = new JButton("Help");
		help.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { helpActionPerformed(); }
		});
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(4, 4, 4, 4);
		leftPanel.add(help, gbc);

		cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { cancelActionPerformed(); }
		});
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 3;
		gbc.insets = new Insets(4, 4, 4, 4);
		leftPanel.add(cancel, gbc);

		ok = new JButton("OK");
		ok.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { okActionPerformed(); }
		});
		gbc = new GridBagConstraints();
		gbc.gridx = 1;   gbc.gridy = 3;
		gbc.insets = new Insets(4, 4, 4, 4);
		leftPanel.add(ok, gbc);
		getRootPane().setDefaultButton(ok);

		getRootPane().setDefaultButton(ok);

		// build preferences framework
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

		loadOptionPanel();
		splitPane.setLeftComponent(leftPanel);

		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;   gbc.weighty = 1.0;
		getContentPane().add(splitPane, gbc);

		pack();
		finishInitialization();
	}

    private boolean openSelectedPath(DefaultMutableTreeNode rootNode)
    {
        for (int i = 0; i < rootNode.getChildCount(); i++)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)rootNode.getChildAt(i);
            Object o = node.getUserObject();
            if (o.toString().equals(currentTabName))//indexOf(currentTabName) != -1)
            {
                optionTree.scrollPathToVisible(new TreePath(node.getPath()));
                return true;
            }
            if (openSelectedPath(node)) return true;
        }
        return false;
    }

	private void cancelActionPerformed()
	{
		closeDialog(null);
	}

	private void okActionPerformed()
	{
		new OKUpdate(this);
	}

	private void helpActionPerformed()
	{
		ManualViewer.showPreferenceHelp(currentSectionName.trim() + "/" + currentTabName);
		closeDialog(null);
	}

	private void exportActionPerformed()
	{
		Job.getUserInterface().exportPrefs();
	}

	private void importActionPerformed()
	{
		Job.getUserInterface().importPrefs();
        TopLevel top = (TopLevel)TopLevel.getCurrentJFrame();
        top.getEMenuBar().restoreSavedBindings(false); // trying to cache again

		// recache all layers and their graphics
        Technology.cacheTransparentLayerColors();

		// close dialog now because all values are cached badly
		closeDialog(null);

		// redraw everything
		EditWindow.repaintAllContents();
        for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
        {
        	WindowFrame wf = it.next();
        	wf.loadComponentMenuForTechnology();
        }
	}

	private void loadOptionPanel()
	{
		for(PreferencePanel ti : optionPanes)
		{
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

	protected void escapePressed() { cancelActionPerformed(); }

	/**
	 * Class to update primitive node information.
	 */
	private static class OKUpdate extends Job
	{
		private transient PreferencesFrame dialog;
		private Pref.PrefChangeBatch changeBatch;

		private OKUpdate(PreferencesFrame dialog)
		{
			super("Update Preferences", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;

			// gather preference changes on the client
			Pref.gatherPrefChanges();
			for(PreferencePanel ti : dialog.optionPanes)
			{
				if (ti.isInited())
					ti.term();
			}
			changeBatch = Pref.getPrefChanges();
			startJob();
		}

		public boolean doIt() throws JobException
		{
			Pref.implementPrefChanges(changeBatch);
			return true;
		}

		public void terminateOK()
		{
			dialog.closeDialog(null);
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
					if (descentPath.getLastPathComponent().equals(tp.getLastPathComponent()))
					{
						DefaultMutableTreeNode subNode = (DefaultMutableTreeNode)descentPath.getLastPathComponent();
						currentSectionName = (String)subNode.getUserObject();
					} else
					{
						dialog.optionTree.collapsePath(descentPath);
					}
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
