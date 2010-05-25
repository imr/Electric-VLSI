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

import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Setting;
import com.sun.electric.technology.TechPool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.output.CellModelPrefs;
import com.sun.electric.tool.routing.Routing;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.dialogs.options.AddedTechnologiesTab;
import com.sun.electric.tool.user.dialogs.options.AntennaRulesTab;
import com.sun.electric.tool.user.dialogs.options.CDLTab;
import com.sun.electric.tool.user.dialogs.options.CIFTab;
import com.sun.electric.tool.user.dialogs.options.CVSTab;
import com.sun.electric.tool.user.dialogs.options.CellModelTab;
import com.sun.electric.tool.user.dialogs.options.CompactionTab;
import com.sun.electric.tool.user.dialogs.options.ComponentMenuTab;
import com.sun.electric.tool.user.dialogs.options.CopyrightTab;
import com.sun.electric.tool.user.dialogs.options.CoverageTab;
import com.sun.electric.tool.user.dialogs.options.DEFTab;
import com.sun.electric.tool.user.dialogs.options.DRCTab;
import com.sun.electric.tool.user.dialogs.options.DXFTab;
import com.sun.electric.tool.user.dialogs.options.DaisTab;
import com.sun.electric.tool.user.dialogs.options.DesignRulesTab;
import com.sun.electric.tool.user.dialogs.options.DisplayControlTab;
import com.sun.electric.tool.user.dialogs.options.EDIFTab;
import com.sun.electric.tool.user.dialogs.options.FastHenryTab;
import com.sun.electric.tool.user.dialogs.options.FrameTab;
import com.sun.electric.tool.user.dialogs.options.GDSTab;
import com.sun.electric.tool.user.dialogs.options.GeneralTab;
import com.sun.electric.tool.user.dialogs.options.GerberTab;
import com.sun.electric.tool.user.dialogs.options.GridAndAlignmentTab;
import com.sun.electric.tool.user.dialogs.options.IconTab;
import com.sun.electric.tool.user.dialogs.options.LayersTab;
import com.sun.electric.tool.user.dialogs.options.LibraryTab;
import com.sun.electric.tool.user.dialogs.options.LogicalEffortTab;
import com.sun.electric.tool.user.dialogs.options.NCCTab;
import com.sun.electric.tool.user.dialogs.options.NetworkTab;
import com.sun.electric.tool.user.dialogs.options.NewArcsTab;
import com.sun.electric.tool.user.dialogs.options.NewNodesTab;
import com.sun.electric.tool.user.dialogs.options.ParasiticTab;
import com.sun.electric.tool.user.dialogs.options.PlacementTab;
import com.sun.electric.tool.user.dialogs.options.PortsAndExportsTab;
import com.sun.electric.tool.user.dialogs.options.PreferencePanel;
import com.sun.electric.tool.user.dialogs.options.PrintingTab;
import com.sun.electric.tool.user.dialogs.options.ProjectManagementTab;
import com.sun.electric.tool.user.dialogs.options.RoutingTab;
import com.sun.electric.tool.user.dialogs.options.SUETab;
import com.sun.electric.tool.user.dialogs.options.ScaleTab;
import com.sun.electric.tool.user.dialogs.options.SelectionTab;
import com.sun.electric.tool.user.dialogs.options.SiliconCompilerTab;
import com.sun.electric.tool.user.dialogs.options.SimulatorsTab;
import com.sun.electric.tool.user.dialogs.options.SkillTab;
import com.sun.electric.tool.user.dialogs.options.SmartTextTab;
import com.sun.electric.tool.user.dialogs.options.SpiceTab;
import com.sun.electric.tool.user.dialogs.options.SunRouterTab;
import com.sun.electric.tool.user.dialogs.options.TechnologyTab;
import com.sun.electric.tool.user.dialogs.options.TextTab;
import com.sun.electric.tool.user.dialogs.options.ThreeDTab;
import com.sun.electric.tool.user.dialogs.options.ToolbarTab;
import com.sun.electric.tool.user.dialogs.options.UnitsTab;
import com.sun.electric.tool.user.dialogs.options.VerilogTab;
import com.sun.electric.tool.user.dialogs.options.WellCheckTab;
import com.sun.electric.tool.user.help.ManualViewer;
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.tool.user.projectSettings.ProjSettings;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * Class to handle the "Preferences Frame" dialog.
 */
public class PreferencesFrame extends EModelessDialog
{
	private JSplitPane splitPane1, splitPane2;
	private JTree optionTree;
	private JButton cancel, ok;
	private Map<Setting,Object> originalContext;
	private Map<Setting,Object> currentContext;
    private EditingPreferences editingPreferences;
	private List<PreferencePanel> optionPanes = new ArrayList<PreferencePanel>();
	private DefaultMutableTreeNode initialDMTN;
	private static PreferencesFrame currentOne;

	/** The name of the current tab in this dialog. */		private static String currentTabName = "General";
	/** The name of the current section in this dialog. */	private static String currentSectionName = "General ";

	private static String staClass = "com.sun.electric.plugins.sctiming.STAOptionsDialog";

	/**
	 * This method implements the command to show the Preferences dialog.
	 */
	public static void preferencesCommand()
	{
		if (currentOne == null)
		{
			currentOne = new PreferencesFrame(TopLevel.getCurrentJFrame());
		}
		currentOne.setVisible(true);
	}

	/**
	 * Method to redisplay the Layers tab (if it is up) when color schemes are changed.
	 */
	public static void updateLayerPreferencesColors()
	{
		if (currentOne == null) return;
		if (!currentTabName.equals("Layers")) return;
		for(PreferencePanel ti : currentOne.optionPanes)
			if (ti.getName().equals(currentTabName))
				((LayersTab)ti).cacheLayerInfo(true);
	}

	/**
	 * This method implements the command to show the Preferences dialog,
	 * and chooses a panel.
	 * @param tabName the name of the panel ("Grid", for example).
	 * @param sectionName the name of the section in which the panel lives ("Display", for example).
	 */
	public static void preferencesCommand(String tabName, String sectionName)
	{
		currentTabName = tabName;
		currentSectionName = sectionName + " ";
		preferencesCommand();
	}

	/** Creates new form Preferences Frame */
	public PreferencesFrame(Frame parent)
	{
		super(parent);
		getContentPane().setLayout(new GridBagLayout());
		setTitle("Preferences");
		setName("");
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent evt) { closeDialog(evt); }
		});

		EDatabase database = EDatabase.clientDatabase();
		originalContext = database.getSettings();
		currentContext = new HashMap<Setting,Object>(originalContext);
        editingPreferences = UserInterfaceMain.getEditingPreferences();

		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Categories");
		DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
		optionTree = new JTree(treeModel);
		TreeHandler handler = new TreeHandler(this);
		optionTree.addMouseListener(handler);
		optionTree.addTreeExpansionListener(handler);

		// the "General" section of the Preferences
		DefaultMutableTreeNode generalSet = new DefaultMutableTreeNode("General ");
		rootNode.add(generalSet);
		addTreeNode(new GeneralTab(parent, true), generalSet);
		addTreeNode(new SelectionTab(parent, true), generalSet);
		TopLevel top = TopLevel.getCurrentJFrame();
		if (top != null && top.getEMenuBar() != null)
			addTreeNode(new EditKeyBindings(top.getEMenuBar(), parent, true), generalSet);
		addTreeNode(new NewNodesTab(this, true), generalSet);
		addTreeNode(new NewArcsTab(this, true), generalSet);
		addTreeNode(new ProjectManagementTab(parent, true), generalSet);
		addTreeNode(new CVSTab(parent, true), generalSet);
		addTreeNode(new PrintingTab(parent, true), generalSet);
		if (Job.getDebug())
		{
			// Open test tab only if plugin is available and in debug mode
			try
			{
				Class<?> testTab = Class.forName("com.sun.electric.plugins.tests.TestTab");
				Constructor<?> tab = testTab.getDeclaredConstructor(new Class[]{Frame.class, Boolean.class});
				addTreeNode((PreferencePanel)tab.newInstance(new Object[] {parent, Boolean.TRUE}), generalSet);
			}
			catch (Exception ex) { /* do nothing */ };
		}

		// the "Display" section of the Preferences
		DefaultMutableTreeNode displaySet = new DefaultMutableTreeNode("Display ");
		rootNode.add(displaySet);
		addTreeNode(new DisplayControlTab(parent, true), displaySet);
		addTreeNode(new ComponentMenuTab(parent, true), displaySet);
		addTreeNode(new LayersTab(parent, true), displaySet);
		addTreeNode(new ToolbarTab(parent, true, this), displaySet);
		addTreeNode(new TextTab(this, true), displaySet);
		addTreeNode(new SmartTextTab(this, true), displaySet);
		addTreeNode(new GridAndAlignmentTab(this, true), displaySet);
		addTreeNode(new PortsAndExportsTab(parent, true), displaySet);
		addTreeNode(new FrameTab(parent, true), displaySet);
		addTreeNode(ThreeDTab.create3DTab(this, true), displaySet);

		// the "I/O" section of the Preferences
		DefaultMutableTreeNode ioSet = new DefaultMutableTreeNode("I/O ");
		rootNode.add(ioSet);
		addTreeNode(new CIFTab(this, true), ioSet);
		addTreeNode(new GDSTab(this, true), ioSet);
		addTreeNode(new EDIFTab(parent, true), ioSet);
		addTreeNode(new DEFTab(parent, true), ioSet);
		addTreeNode(new CDLTab(parent, true), ioSet);
		addTreeNode(new DXFTab(this, true), ioSet);
		addTreeNode(new GerberTab(parent, true), ioSet);
		addTreeNode(new SUETab(parent, true), ioSet);
		if (IOTool.hasDais())
			addTreeNode(new DaisTab(parent, true), ioSet);
		if (IOTool.hasSkill())
			addTreeNode(new SkillTab(this, true), ioSet);
		addTreeNode(new LibraryTab(parent, true), ioSet);
		addTreeNode(new CopyrightTab(this, true), ioSet);

		// the "Tools" section of the Preferences
		DefaultMutableTreeNode toolSet = new DefaultMutableTreeNode("Tools ");
		rootNode.add(toolSet);
		addTreeNode(new AntennaRulesTab(parent, true), toolSet);
		addTreeNode(new CompactionTab(parent, true), toolSet);
		addTreeNode(new CoverageTab(parent, true), toolSet);
		addTreeNode(new DRCTab(this, true), toolSet);
		addTreeNode(new FastHenryTab(parent, true), toolSet);
		addTreeNode(new LogicalEffortTab(this, true), toolSet);
		addTreeNode(new NCCTab(parent, true), toolSet);
		addTreeNode(new NetworkTab(this, true), toolSet);
		addTreeNode(new ParasiticTab(this, true), toolSet);
		addTreeNode(new PlacementTab(this, true), toolSet);
		addTreeNode(new RoutingTab(this, true), toolSet);
		addTreeNode(new SiliconCompilerTab(parent, true), toolSet);
		addTreeNode(new SimulatorsTab(parent, true), toolSet);
		addTreeNode(new SpiceTab(parent, true), toolSet);
		addTreeNode(new CellModelTab(parent, true, CellModelPrefs.spiceModelPrefs), toolSet);

        if (getPluginPanel(staClass, this, true) != null)
            addTreeNode(getPluginPanel(staClass, this, true), toolSet);

		if (Routing.hasSunRouter())
			addTreeNode(new SunRouterTab(parent, true), toolSet);
		addTreeNode(new VerilogTab(this, true), toolSet);
		addTreeNode(new CellModelTab(parent, true, CellModelPrefs.verilogModelPrefs), toolSet);
		addTreeNode(new WellCheckTab(parent, true), toolSet);

		// the "Technology" section of the Preferences
		DefaultMutableTreeNode techSet = new DefaultMutableTreeNode("Technology ");
		rootNode.add(techSet);
		addTreeNode(new AddedTechnologiesTab(this, true), techSet);
		addTreeNode(new TechnologyTab(this, true), techSet);
		addTreeNode(new DesignRulesTab(this, true), techSet);
		addTreeNode(new ScaleTab(this, true), techSet);
		addTreeNode(new UnitsTab(parent, true), techSet);
		addTreeNode(new IconTab(this, true), techSet);

		// pre-expand the tree
		TreePath topPath = optionTree.getPathForRow(0);
		optionTree.expandPath(topPath);
		topPath = optionTree.getPathForRow(1);
		optionTree.expandPath(topPath);
		topPath = optionTree.getNextMatch(currentSectionName, 0, null);
		optionTree.expandPath(topPath);

		// searching for selected node
		openSelectedPath(rootNode);

		// the left side of the Preferences dialog: a tree
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

		JButton reset = new JButton("Reset");
		reset.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { resetActionPerformed(); }
		});
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 2;
		gbc.insets = new Insets(4, 4, 1, 4);
		leftPanel.add(reset, gbc);

		JButton resetAll = new JButton("Reset All");
		resetAll.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { resetAllActionPerformed(); }
		});
		gbc = new GridBagConstraints();
		gbc.gridx = 1;   gbc.gridy = 2;
		gbc.insets = new Insets(4, 4, 1, 4);
		leftPanel.add(resetAll, gbc);

		JLabel explainReset = new JLabel("(Only resets USER Preferences)");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 3;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(0, 4, 4, 4);
		leftPanel.add(explainReset, gbc);

		JButton help = new JButton("Help");
		help.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { helpActionPerformed(); }
		});
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 4;
		gbc.insets = new Insets(4, 4, 4, 4);
		leftPanel.add(help, gbc);

		JButton apply = new JButton("Apply");
		apply.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { applyActionPerformed(); }
		});
		gbc = new GridBagConstraints();
		gbc.gridx = 1;   gbc.gridy = 4;
		gbc.insets = new Insets(4, 4, 4, 4);
		leftPanel.add(apply, gbc);

		cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { cancelActionPerformed(); }
		});
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 5;
		gbc.insets = new Insets(4, 4, 4, 4);
		leftPanel.add(cancel, gbc);

		ok = new JButton("OK");
		ok.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { okActionPerformed(); }
		});
		gbc = new GridBagConstraints();
		gbc.gridx = 1;   gbc.gridy = 5;
		gbc.insets = new Insets(4, 4, 4, 4);
		leftPanel.add(ok, gbc);
		getRootPane().setDefaultButton(ok);

		getRootPane().setDefaultButton(ok);

		// build Preferences framework
		splitPane1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

		loadOptionPanel();
		splitPane1.setLeftComponent(leftPanel);
		splitPane2.setRightComponent(splitPane1);
		EDialog.recursivelyHighlight(optionTree, rootNode, initialDMTN, optionTree.getPathForRow(0));

		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;   gbc.weighty = 1.0;
		getContentPane().add(splitPane2, gbc);

//		pack();
		finishInitialization();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() { pack(); }
		});
	}

	private void addTreeNode(PreferencePanel panel, DefaultMutableTreeNode theSet)
	{
		optionPanes.add(panel);
		String sectionName = (String)theSet.getUserObject();
		String name = panel.getName();
		DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(name);
		theSet.add(dmtn);
		if (sectionName.equals(currentSectionName) && name.equals(currentTabName))
			initialDMTN = dmtn;
	}

	private boolean openSelectedPath(DefaultMutableTreeNode rootNode)
	{
		for (int i = 0; i < rootNode.getChildCount(); i++)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)rootNode.getChildAt(i);
			Object o = node.getUserObject();
			if (o.toString().equals(currentTabName))
			{
				optionTree.scrollPathToVisible(new TreePath(node.getPath()));
				return true;
			}
			if (openSelectedPath(node)) return true;
		}
		return false;
	}

    private PreferencePanel getPluginPanel(String className, PreferencesFrame frame, boolean modal)
    {
        try {
            Class<?> panelClass = Class.forName(className);
            Object panel = panelClass.getConstructor(PreferencesFrame.class, Boolean.class).newInstance(frame, Boolean.valueOf(modal));
            return (PreferencePanel)panel;
        } catch (Exception e) {
//            System.out.println("Exception while loading plugin class "+className+": "+e.getMessage());
        }
        return null;
    }


	private void cancelActionPerformed()
	{
		closeDialog(null);
	}

	private void okActionPerformed()
	{
		for(PreferencePanel ti : optionPanes)
		{
			if (ti.isInited()) ti.term();
		}
        UserInterfaceMain.setEditingPreferences(editingPreferences);

		// gather preference changes on the client
		Setting.SettingChangeBatch changeBatch = getChanged();
		if (changeBatch.changesForSettings.isEmpty())
		{
			closeDialog(null);
			return;
		}
		new OKUpdate(this, changeBatch, true);
	}

	private Setting.SettingChangeBatch getChanged()
	{
		Setting.SettingChangeBatch changeBatch = new Setting.SettingChangeBatch();
		for (Map.Entry<Setting,Object> e : originalContext.entrySet())
		{
			Setting setting = e.getKey();
			Object oldVal = e.getValue();
			Object v = currentContext.get(setting);
			if (oldVal.equals(v)) continue;
			changeBatch.add(setting, v);
		}
		return changeBatch;
	}

	/**
	 * Class to update primitive node information.
	 */
	private static class OKUpdate extends Job
	{
		private transient PreferencesFrame dialog;
		private Setting.SettingChangeBatch changeBatch;
		private boolean issueWarning;
		private transient TechPool oldTechPool;

		private OKUpdate(PreferencesFrame dialog, Setting.SettingChangeBatch changeBatch, boolean issueWarning)
		{
			super("Update Project Preferences", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
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
            getDatabase().getEnvironment().saveToPreferences();
			if (issueWarning)
			{
				if (ProjSettings.getLastProjectSettingsFile() != null)
				{
					Job.getUserInterface().showInformationMessage("Warning: These changes are only valid for this session of Electric."+
						"\nTo save them permanently, use File -> Export -> Project Preferences", "Saving Project Preferences Changes");
				} else
				{
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
						if (curLib == null || curLib.isChanged())
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
						int i = JOptionPane.showOptionDialog(dialog, "Warning: Changed settings must be saved to Library or Project Preferences file.\nPlease choose which Libraries to mark for saving, or write project preferences file:",
							"Saving Project Preferences Changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, defaultOption);
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
                dialog.closeDialog(null);
			}
			if (getDatabase().getTechPool() != oldTechPool)
			{
				// Repair libraries in case number of layers was changed.
				CircuitChanges.checkAndRepairCommand(true);
			}
		}
	}

	public Map<Setting,Object> getContext() { return currentContext; }

    public EditingPreferences getEditingPreferences() { return editingPreferences; }
    public void setEditingPreferences(EditingPreferences ep) { editingPreferences = ep; }

	private void applyActionPerformed()
	{
		for(PreferencePanel ti : optionPanes)
		{
			if (ti.isInited())
				ti.term();
		}
        UserInterfaceMain.setEditingPreferences(editingPreferences);
	}

	private void resetActionPerformed()
	{
		for(PreferencePanel ti : optionPanes)
		{
			if (ti.getName().equals(currentTabName))
			{
				boolean response = Job.getUserInterface().confirmMessage(
					"Do you really want to reset the " + ti.getName() + " Preferences to their 'factory' state?");
				if (response)
				{
					Pref.delayPrefFlushing();
					boolean inPlace = ti.resetThis();
					Pref.resumePrefFlushing();
					if (inPlace)
					{
						// panel was reset in place without redisplay
					} else
					{
						// panel unable to reset itself: do hard reset and quit dialog
						ti.reset();
                        UserInterfaceMain.setEditingPreferences(editingPreferences);
						closeDialog(null);
						WindowFrame.repaintAllWindows();
					}

					// gather preference changes on the client
					Setting.SettingChangeBatch changeBatch = getChanged();
					if (!changeBatch.changesForSettings.isEmpty())
						new OKUpdate(null, changeBatch, false);
					return;
				}
				break;
			}
		}
	}

	private void resetAllActionPerformed()
	{
		boolean response = Job.getUserInterface().confirmMessage(
			"Do you really want to reset all Preferences to their 'factory' state?");
		if (response)
		{
			Pref.delayPrefFlushing();
			for(PreferencePanel ti : optionPanes)
				ti.reset();
            UserInterfaceMain.setEditingPreferences(editingPreferences);
			Pref.resumePrefFlushing();
			closeDialog(null);
			WindowFrame.repaintAllWindows();
		}
	}

	private void helpActionPerformed()
	{
		ManualViewer.showPreferenceHelp(currentSectionName.trim() + "/" + currentTabName);
		closeDialog(null);
	}

	private void exportActionPerformed()
	{
		FileMenu.exportPrefsCommand();
	}

	private void importActionPerformed()
	{
		FileMenu.importPrefsCommand();

		// close dialog now because all values are cached badly
		closeDialog(null);
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
				JPanel prefs = ti.getUserPreferencesPanel();
				if (prefs == null) splitPane1.setRightComponent(null); else
				{
					JPanel pane = new JPanel();
					pane.setLayout(new GridBagLayout());
					JLabel prefLab = new JLabel(ti.getName() + " USER Preferences");
					prefLab.setForeground(Color.WHITE);
					prefLab.setHorizontalTextPosition(JLabel.CENTER);
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.gridx = 0;   gbc.gridy = 0;
					gbc.fill = GridBagConstraints.HORIZONTAL;
					gbc.anchor = GridBagConstraints.NORTH;
					gbc.weightx = 1.0;
					JPanel backgroundTitle = new JPanel();
					backgroundTitle.setBackground(Color.BLUE);
					backgroundTitle.add(prefLab);
					pane.add(backgroundTitle, gbc);

					gbc = new GridBagConstraints();
					gbc.gridx = 0;   gbc.gridy = 1;
					gbc.fill = GridBagConstraints.BOTH;
					gbc.weightx = gbc.weighty = 1.0;
					gbc.weightx = gbc.weighty = 1.0;
					pane.add(prefs, gbc);

					splitPane1.setRightComponent(pane);
				}

				JPanel ps = ti.getProjectPreferencesPanel();
				if (ps == null) splitPane2.setLeftComponent(null); else
				{
					JPanel pane = new JPanel();
					pane.setLayout(new GridBagLayout());
					JLabel prefLab = new JLabel(ti.getName() + " PROJECT Preferences");
					prefLab.setForeground(Color.WHITE);
					prefLab.setHorizontalTextPosition(JLabel.CENTER);
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.gridx = 0;   gbc.gridy = 0;
					gbc.fill = GridBagConstraints.HORIZONTAL;
					gbc.anchor = GridBagConstraints.NORTH;
					gbc.weightx = 1.0;
					JPanel backgroundTitle = new JPanel();
					backgroundTitle.setBackground(Color.BLUE);
					backgroundTitle.add(prefLab);
					pane.add(backgroundTitle, gbc);

					gbc = new GridBagConstraints();
					gbc.gridx = 0;   gbc.gridy = 1;
					gbc.fill = GridBagConstraints.BOTH;
					gbc.weightx = gbc.weighty = 1.0;
					gbc.weightx = gbc.weighty = 1.0;
					pane.add(ps, gbc);

					splitPane2.setLeftComponent(pane);
				}
				return;
			}
		}
	}

	protected void escapePressed() { cancelActionPerformed(); }

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
			Point treeLocBefore = dialog.optionTree.getLocation();
			SwingUtilities.convertPointToScreen(treeLocBefore, dialog.optionTree);
			dialog.pack();
			Point treeLocAfter = dialog.optionTree.getLocation();
			SwingUtilities.convertPointToScreen(treeLocAfter, dialog.optionTree);
			Point dialogLocation = dialog.getLocation();
			int newX = dialogLocation.x + treeLocBefore.x - treeLocAfter.x;
			if (newX < 0) newX = 0;
			dialog.setLocation(newX, dialogLocation.y);
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
		currentOne = null;
		setVisible(false);
		dispose();
	}
}
