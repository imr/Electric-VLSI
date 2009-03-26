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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Setting;
import com.sun.electric.technology.TechPool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.projsettings.AddedTechnologiesTab;
import com.sun.electric.tool.user.dialogs.projsettings.GDSTab;
import com.sun.electric.tool.user.dialogs.projsettings.LogicalEffortTab;
import com.sun.electric.tool.user.dialogs.projsettings.NetlistsTab;
import com.sun.electric.tool.user.dialogs.projsettings.ParasiticTab;
import com.sun.electric.tool.user.dialogs.projsettings.ProjSettingsPanel;
import com.sun.electric.tool.user.dialogs.projsettings.SkillTab;
import com.sun.electric.tool.user.help.ManualViewer;
import com.sun.electric.tool.user.projectSettings.ProjSettings;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JOptionPane;
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
 * Class to handle the "Project Settings" dialog.
 */
public class ProjectSettingsFrame extends EDialog
{
	private JSplitPane splitPane;
	private JTree optionTree;
    private Map<Setting,Object> originalContext;
    private HashMap<Setting,Object> currentContext;
    private JButton cancel;
    private JButton ok;
    private ProjSettingsPanel currentOptionPanel;
	private DefaultMutableTreeNode currentDMTN;
	/** The name of the current tab in this dialog. */		private static String currentTabName = "Netlists";

    private static String staClass = "com.sun.electric.plugins.sctiming.STAOptionsDialog";

    /**
	 * This method implements the command to show the Project Settings dialog.
	 */
	public static void projectSettingsCommand()
	{
		ProjectSettingsFrame dialog = new ProjectSettingsFrame(TopLevel.getCurrentJFrame());
		dialog.setVisible(true);
	}

	/** Creates new form ProjectSettingsFrame */
	public ProjectSettingsFrame(Frame parent)
	{
		super(parent, true);
        EDatabase database = EDatabase.clientDatabase();
        originalContext = database.getSettings();
        currentContext = new HashMap<Setting,Object>(originalContext);
		getContentPane().setLayout(new GridBagLayout());
		setTitle("Project Settings");
		setName("");
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent evt) { closeDialog(evt); }
		});

		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Project Settings");
		DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
		optionTree = new JTree(treeModel);
		TreeHandler handler = new TreeHandler(this);
		optionTree.addMouseListener(handler);
		optionTree.addTreeExpansionListener(handler);

		addTreeNode(rootNode, "Added Technologies");
//		addTreeNode(rootNode, "CIF");
		addTreeNode(rootNode, "GDS");
//		addTreeNode(rootNode, "DXF");
		addTreeNode(rootNode, "Logical Effort");
		addTreeNode(rootNode, "Netlists");
		addTreeNode(rootNode, "Parasitic");
//		addTreeNode(rootNode, "Scale");
		if (IOTool.hasSkill()) addTreeNode(rootNode, "Skill");
//		addTreeNode(rootNode, "Technology");
//		addTreeNode(rootNode, "Verilog");
        if (getPluginPanel(staClass, this, true) != null)
            addTreeNode(rootNode, "Static Timing Analysis");

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
		recursivelyHighlight(optionTree, rootNode, currentDMTN, optionTree.getPathForRow(0));
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

	private void addTreeNode(DefaultMutableTreeNode rootNode, String name)
	{
		DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(name);
		rootNode.add(dmtn);
		if (name.equals(currentTabName))
			currentDMTN = dmtn;
	}

    public Map<Setting,Object> getContext() { return currentContext; }

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
        Setting.SettingChangeBatch changeBatch = new Setting.SettingChangeBatch();

        // gather preference changes on the client
        if (currentOptionPanel != null)
        {
            currentOptionPanel.term();
            currentOptionPanel = null;
        }
        for (Map.Entry<Setting,Object> e: originalContext.entrySet())
        {
            Setting setting = e.getKey();
            Object oldVal = e.getValue();
            Object v = currentContext.get(setting);
            if (oldVal.equals(v)) continue;
            changeBatch.add(setting, v);
        }
        if (changeBatch.changesForSettings.isEmpty())
        {
            closeDialog(null);
            return;
        }
        new OKUpdate(this, changeBatch, true);
	}

    private void helpActionPerformed()
	{
		ManualViewer.showProjectSettingHelp(currentTabName);
		closeDialog(null);
	}

	private void loadOptionPanel()
	{
        ProjSettingsPanel ti = createOptionPanel(isModal());
        if (ti == null) return;
        if (currentOptionPanel != null)
            currentOptionPanel.term();
        currentOptionPanel = ti;
        ti.init();
        splitPane.setRightComponent(ti.getPanel());
	}

    private ProjSettingsPanel createOptionPanel(boolean modal)
    {
        if (currentTabName.equals("Added Technologies"))
            return new AddedTechnologiesTab(this, modal);
        if (currentTabName.equals("GDS"))
            return new GDSTab(this, modal);
        if (currentTabName.equals("Logical Effort"))
            return new LogicalEffortTab(this, modal);
        if (currentTabName.equals("Netlists"))
            return new NetlistsTab(this, modal);
        if (currentTabName.equals("Parasitic"))
            return new ParasiticTab(this, modal);
        if (currentTabName.equals("Skill"))
            return new SkillTab(this, modal);
        if (currentTabName.equals("Static Timing Analysis"))
            return getPluginPanel(staClass, this, modal);
        return null;
    }

	protected void escapePressed() { cancelActionPerformed(); }

    /**
     * Change project settings according to changeBatch and close dialog on success.
     * @param changeBatch batch of changed project settings
     * @param dialogToClose dialog to close on success.
     */
    public static void updateProjectSettings(Setting.SettingChangeBatch changeBatch, EDialog dialogToClose) {
        new OKUpdate(dialogToClose, changeBatch, false);
    }

    private ProjSettingsPanel getPluginPanel(String className, ProjectSettingsFrame frame, boolean modal) {
        try {
            Class<?> panelClass = Class.forName(className);
            Object panel = panelClass.getConstructor(ProjectSettingsFrame.class, Boolean.class).newInstance(frame, Boolean.valueOf(modal));
            return (ProjSettingsPanel)panel;
        } catch (Exception e) {
            System.out.println("Exception while loading plugin class "+className+": "+e.getMessage());
        }
        return null;
    }

    /**
	 * Class to update primitive node information.
	 */
	private static class OKUpdate extends Job
	{
		private transient EDialog dialog;
		private Setting.SettingChangeBatch changeBatch;
        private boolean issueWarning;
        private transient TechPool oldTechPool;

        private OKUpdate(EDialog dialog, Setting.SettingChangeBatch changeBatch, boolean issueWarning) {
			super("Update Project Settings", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.dialog = dialog;
            this.changeBatch = changeBatch;
            this.issueWarning = issueWarning;
            oldTechPool = getDatabase().getTechPool();
            startJob();
        }

		public boolean doIt() throws JobException
		{
			getDatabase().implementSettingChanges(changeBatch);
			return true;
		}

        @Override
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
            if (dialog != null) {
                dialog.setVisible(false);
                dialog.dispose();
            }
//            dialog.closeDialog(null);
            if (getDatabase().getTechPool() != oldTechPool) {
                // Repair libraries in case number of layers was changed.
                CircuitChanges.checkAndRepairCommand(true);
            }
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
			if (!currentTabName.endsWith(" "))
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
