/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ProjectSettingsFrame.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.projectSettings.ProjSettings;
import com.sun.electric.tool.user.dialogs.projsettings.CIFTab;
import com.sun.electric.tool.user.dialogs.projsettings.DXFTab;
import com.sun.electric.tool.user.dialogs.projsettings.GDSTab;
import com.sun.electric.tool.user.dialogs.projsettings.LogicalEffortTab;
import com.sun.electric.tool.user.dialogs.projsettings.NetlistsTab;
import com.sun.electric.tool.user.dialogs.projsettings.ParasiticTab;
import com.sun.electric.tool.user.dialogs.projsettings.ProjSettingsPanel;
import com.sun.electric.tool.user.dialogs.projsettings.ScaleTab;
import com.sun.electric.tool.user.dialogs.projsettings.SkillTab;
import com.sun.electric.tool.user.dialogs.projsettings.TechnologyTab;
import com.sun.electric.tool.user.dialogs.projsettings.VerilogTab;
import com.sun.electric.tool.user.help.ManualViewer;
import com.sun.electric.tool.user.ui.TopLevel;

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
import java.util.List;
import java.util.Iterator;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.JOptionPane;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * Class to handle the "ProjectSettings Frame" dialog.
 */
public class ProjectSettingsFrame extends EDialog
{
	private JSplitPane splitPane;
	private JTree optionTree;
	JButton cancel;
	JButton ok;

    List<ProjSettingsPanel> optionPanes = new ArrayList<ProjSettingsPanel>();

	/** The name of the current tab in this dialog. */		private static String currentTabName = "Netlists";
	/** The name of the current section in this dialog. */	private static String currentSectionName = "General ";

	/**
	 * This method implements the command to show the Project Settings dialog.
	 */
	public static void projectSettingsCommand()
	{
		ProjectSettingsFrame dialog = new ProjectSettingsFrame(TopLevel.getCurrentJFrame(), true);
		dialog.setVisible(true);
	}

	/** Creates new form ProjectSettingsFrame */
	public ProjectSettingsFrame(Frame parent, boolean modal)
	{
		super(parent, modal);
		getContentPane().setLayout(new GridBagLayout());
		setTitle("Project Settings");
		setName("");
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent evt)
			{
				closeDialog(evt);
			}
		});

		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Project Settings");
		DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
		optionTree = new JTree(treeModel);
		TreeHandler handler = new TreeHandler(this);
		optionTree.addMouseListener(handler);
		optionTree.addTreeExpansionListener(handler);

		CIFTab cit = new CIFTab(parent, modal);
		optionPanes.add(cit);
		rootNode.add(new DefaultMutableTreeNode(cit.getName()));

		GDSTab gdt = new GDSTab(parent, modal);
		optionPanes.add(gdt);
		rootNode.add(new DefaultMutableTreeNode(gdt.getName()));

		DXFTab dxt = new DXFTab(parent, modal);
		optionPanes.add(dxt);
		rootNode.add(new DefaultMutableTreeNode(dxt.getName()));

		LogicalEffortTab let = new LogicalEffortTab(parent, modal);
		optionPanes.add(let);
		rootNode.add(new DefaultMutableTreeNode(let.getName()));

		NetlistsTab nt = new NetlistsTab(parent, modal);
		optionPanes.add(nt);
		rootNode.add(new DefaultMutableTreeNode(nt.getName()));

		ParasiticTab parat = new ParasiticTab(parent, modal);
		optionPanes.add(parat);
		rootNode.add(new DefaultMutableTreeNode(parat.getName()));

		ScaleTab scat = new ScaleTab(parent, modal);
		optionPanes.add(scat);
		rootNode.add(new DefaultMutableTreeNode(scat.getName()));

		if (IOTool.hasSkill())
		{
			SkillTab skt = new SkillTab(parent, modal);
			optionPanes.add(skt);
			rootNode.add(new DefaultMutableTreeNode(skt.getName()));
		}

		TechnologyTab tect = new TechnologyTab(parent, modal);
		optionPanes.add(tect);
		rootNode.add(new DefaultMutableTreeNode(tect.getName()));

		VerilogTab vet = new VerilogTab(parent, modal);
		optionPanes.add(vet);
		rootNode.add(new DefaultMutableTreeNode(vet.getName()));

		// pre-expand the tree
		TreePath topPath = optionTree.getPathForRow(0);
		optionTree.expandPath(topPath);
		topPath = optionTree.getPathForRow(1);
		optionTree.expandPath(topPath);

        // searching for selected node
        openSelectedPath(rootNode);

		// the left side of the Project Settings dialog: a tree
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new GridBagLayout());

		JScrollPane scrolledTree = new JScrollPane(optionTree);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;   gbc.weighty = 1.0;
		leftPanel.add(scrolledTree, gbc);

//		JButton save = new JButton("Export");
//		save.addActionListener(new ActionListener()
//		{
//			public void actionPerformed(ActionEvent evt) { exportActionPerformed(); }
//		});
//		gbc = new GridBagConstraints();
//		gbc.gridx = 0;   gbc.gridy = 1;
//		gbc.insets = new Insets(4, 4, 4, 4);
//		leftPanel.add(save, gbc);

//		JButton restore = new JButton("Import");
//		restore.addActionListener(new ActionListener()
//		{
//			public void actionPerformed(ActionEvent evt) { importActionPerformed(); }
//		});
//		gbc = new GridBagConstraints();
//		gbc.gridx = 1;   gbc.gridy = 1;
//		gbc.insets = new Insets(4, 4, 4, 4);
//		leftPanel.add(restore, gbc);

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

        // build Project Settings framework
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
		new OKUpdate(this, false);
	}

    private void helpActionPerformed()
	{
		ManualViewer.showProjectSettingHelp(currentTabName);
		closeDialog(null);
	}

//	private void exportActionPerformed()
//	{
//		Job.getUserInterface().exportPrefs();
//	}

//	private void importActionPerformed()
//	{
//		Job.getUserInterface().importPrefs();
//        TopLevel top = (TopLevel)TopLevel.getCurrentJFrame();
//        top.getTheMenuBar().restoreSavedBindings(false); // trying to cache again
//
//		// recache all layers and their graphics
//		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
//		{
//			Technology tech = it.next();
//			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
//			{
//				Layer layer = lIt.next();
//				layer.getGraphics().recachePrefs();
//			}
//		}
//
//		// close dialog now because all values are cached badly
//		closeDialog(null);
//
//		// redraw everything
//		EditWindow.repaintAllContents();
//        for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
//        {
//        	WindowFrame wf = it.next();
//        	wf.loadComponentMenuForTechnology();
//        }
//	}

	private void loadOptionPanel()
	{
		for(ProjSettingsPanel ti : optionPanes)
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
		private transient ProjectSettingsFrame dialog;
		private Pref.PrefChangeBatch changeBatch;
        private boolean issueWarning;

        private OKUpdate(ProjectSettingsFrame dialog, boolean issueWarning)
		{
			super("Update Project Settings", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
            this.issueWarning = issueWarning;

            // gather preference changes on the client
			Pref.gatherPrefChanges();
            Pref.clearChangedAllPrefs();
            for(ProjSettingsPanel ti : dialog.optionPanes)
			{
				if (ti.isInited())
					ti.term();
			}
            if (Pref.anyPrefChanged()) this.issueWarning = true;
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
            if (issueWarning) {
                if (ProjSettings.getLastProjectSettingsFile() != null) {
                    Job.getUserInterface().showInformationMessage("Warning: These changes are only valid for this session of Electric."+
                    "\nTo save them permanently, use File -> Export -> Project Settings", "Saving Project Setting Changes");
                } else {
                	// see if any libraries are not marked for saving
                	boolean saveAny = false;
                    for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
                    {
                        Library lib = it.next();
                        if (lib.isHidden()) continue;
                        if (!lib.isChanged()) saveAny = true;
                    }
                    if (saveAny)
                    {
                    	// some libraries may need to be marked for saving
	                    Library curLib = Library.getCurrent();
	                    String [] options;
	                    String defaultOption;
	                    int markCurrent, saveSettings;
	                    if (curLib.isChanged())
	                    {
	                    	options = new String [] { "Mark All Libs", "Write Proj Settings file", "Do nothing"};
	                      	defaultOption = options[2];
	                      	markCurrent = 1000;
	                      	saveSettings = 1;
	                    } else
	                    {
	                      	options = new String [] { "Mark All Libs", "Mark Lib \""+curLib.getName()+"\"", "Write Proj Settings file", "Do nothing"};
	                      	defaultOption = options[0];
	                      	markCurrent = 1;
	                      	saveSettings = 2;
	                    }
	                    int i = JOptionPane.showOptionDialog(dialog, "Warning: Changed settings must be saved to Library or Project Settings file.\nPlease choose which Libraries to mark for saving, or write project settings file:",
	                    	"Saving Project Setting Changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, defaultOption);
	                    if (i == 0) {
	                        CircuitChangeJobs.markAllLibrariesForSavingCommand();
	                    } else if (i == markCurrent) {
	                        CircuitChangeJobs.markCurrentLibForSavingCommand();
	                    } else if (i == saveSettings) {
	                        ProjSettings.exportSettings();
	                    }
                    }
                }
            }
            dialog.closeDialog(null);
		}
	}

	private static class TreeHandler implements MouseListener, TreeExpansionListener
	{
		private ProjectSettingsFrame dialog;

		TreeHandler(ProjectSettingsFrame dialog) { this.dialog = dialog; }

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
	private void closeDialog(WindowEvent evt)
	{
		setVisible(false);
		dispose();
	}
}
