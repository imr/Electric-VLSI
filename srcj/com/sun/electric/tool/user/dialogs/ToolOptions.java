/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ToolOptions.java
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

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.text.TextUtils;
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
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.util.Iterator;
import java.util.HashMap;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;


/**
 * Class to handle the "Tool Options" dialog.
 */
public class ToolOptions extends EDialog
{
	private Technology curTech;
	private Library curLib;

	static class Option
	{
		int type;
		String oldString, newString;
		boolean oldBoolean, newBoolean;
		int oldInt, newInt;
		double oldDouble, newDouble;

		static final int ISSTRING = 1;
		static final int ISINTEGER = 2;
		static final int ISDOUBLE = 3;
		static final int ISBOOLEAN = 4;

		Option() {}

		static Option newStringOption(String oldValue)
		{
			Option option = new Option();
			option.type = ISSTRING;
			option.oldString = new String(oldValue);
			option.newString = new String(oldValue);
			return option;
		}
		void setStringValue(String newString) { this.newString = new String(newString); }
		String getStringValue() { return newString; }

//		static Option newIntOption(int oldValue)
//		{
//			Option option = new Option();
//			option.type = ISINTEGER;
//			option.oldInt = option.newInt = oldValue;
//			return option;
//		}
//		void setIntValue(int newInt) { this.newInt = newInt; }
//		int getIntValue() { return newInt; }

		static Option newDoubleOption(double oldValue)
		{
			Option option = new Option();
			option.type = ISDOUBLE;
			option.oldDouble = option.newDouble = oldValue;
			return option;
		}
		void setDoubleValue(double newDouble) { this.newDouble = newDouble; }
		double getDoubleValue() { return newDouble; }

//		static Option newBooleanOption(boolean oldValue)
//		{
//			Option option = new Option();
//			option.type = ISBOOLEAN;
//			option.oldBoolean = option.newBoolean = oldValue;
//			return option;
//		}
//		void setBooleanValue(boolean newBoolean) { this.newBoolean = newBoolean; }
//		boolean getBooleanValue() { return newBoolean; }

		int getType() { return type; }

		boolean isChanged()
		{
			switch (type)
			{
				case ISSTRING:  return !oldString.equals(newString);
				case ISINTEGER: return oldInt != newInt;
				case ISDOUBLE:  return oldDouble != newDouble;
				case ISBOOLEAN: return oldBoolean != newBoolean;
			}
			return false;
		}
	}

	/** The name of the current tab in this dialog. */	private static String currentTabName = null;

	/**
	 * This method implements the command to show the Tool Options dialog.
	 */
	public static void toolOptionsCommand()
	{
		ToolOptions dialog = new ToolOptions(TopLevel.getCurrentJFrame(), true);
		dialog.show();
	}

	/** Creates new form ToolOptions */
	public ToolOptions(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// if the last know tab name is available, find that tab again
		if (currentTabName != null)
		{
			int numTabs = tabPane.getTabCount();
			for(int i=0; i<numTabs; i++)
			{
				String tabName = tabPane.getTitleAt(i);
				if (tabName.equals(currentTabName))
				{
					tabPane.setSelectedIndex(i);
					break;
				}
			}
		}

		// listen for changes in the current tab
        tabPane.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent evt)
            {
				currentTabName = tabPane.getTitleAt(tabPane.getSelectedIndex());
            }
        });

		// get current information
		curTech = Technology.getCurrent();
		curLib = Library.getCurrent();

		// factory reset not working yet
		factoryReset.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { factoryResetActionPerformed(evt); }
		});
		factoryReset.setEnabled(false);

		initDRC();				// initialize the DRC Options panel
		initDesignRules();		// initialize the Design Rules panel
		initSpice();			// initialize the SPICE Options panel
		initVerilog();			// initialize the Verilog Options panel
		initFastHenry();		// initialize the Fast Henry Options panel
		initWellCheck();		// initialize the Well Check Options panel
		initAntennaRules();		// initialize the Antenna Rules Options panel
		initNetwork();			// initialize the Network Options panel
		initNCC();				// initialize the NCC Options panel
		initLogicalEffort();	// initialize the Logical Effort Options panel
		initRouting();			// initialize the Routing Options panel
		initCompaction();		// initialize the Compaction Options panel
	}

	/**
	 * Class to apply changes to tool options in a new thread.
	 */
	protected static class ApplyToolOptions extends Job
	{
		ToolOptions dialog;

		protected ApplyToolOptions(ToolOptions dialog)
		{
			super("Apply Tool Options", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
			startJob();
		}

		public void doIt()
		{
			dialog.termDRC();			// terminate the DRC Options panel
			dialog.termDesignRules();	// terminate the Design Rules panel
			dialog.termSpice();			// terminate the SPICE Options panel
			dialog.termVerilog();		// terminate the Verilog Options panel
			dialog.termFastHenry();		// terminate the Fast Henry Options panel
			dialog.termWellCheck();		// terminate the Well Check Options panel
			dialog.termAntennaRules();	// terminate the Antenna Rules Options panel
			dialog.termNetwork();		// terminate the Network Options panel
			dialog.termNCC();			// terminate the NCC Options panel
			dialog.termLogicalEffort();	// terminate the Logical Effort Options panel
			dialog.termRouting();		// terminate the Routing Options panel
			dialog.termCompaction();	// terminate the Compaction Options panel
			dialog.closeDialog(null);
		}
	}

	private void factoryResetActionPerformed(ActionEvent evt)
	{
	}

	private void showLayersInTechnology(DefaultListModel model)
	{
		model.clear();
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			model.addElement(layer.getName());
		}
	}

	//******************************** DRC ********************************

	private boolean initialDRCIncrementalOn;
	private boolean initialDRCOneErrorPerCell;
	private boolean initialDRCUseMultipleThreads;
	private boolean initialDRCIgnoreCenterCuts;
	private int initialDRCNumberOfThreads;
	private boolean requestedDRCClearDates;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the DRC tab.
	 */
	private void initDRC()
	{
		initialDRCIncrementalOn = DRC.isIncrementalDRCOn();
		drcIncrementalOn.setSelected(initialDRCIncrementalOn);

		initialDRCOneErrorPerCell = DRC.isOneErrorPerCell();
		drcOneErrorPerCell.setSelected(initialDRCOneErrorPerCell);

		initialDRCUseMultipleThreads = DRC.isUseMultipleThreads();
		drcUseMultipleThreads.setSelected(initialDRCUseMultipleThreads);

		initialDRCNumberOfThreads = DRC.getNumberOfThreads();
		drcNumberOfThreads.setText(Integer.toString(initialDRCNumberOfThreads));

		initialDRCIgnoreCenterCuts = DRC.isIgnoreCenterCuts();
		drcIgnoreCenterCuts.setSelected(initialDRCIgnoreCenterCuts);

		requestedDRCClearDates = false;
		drcClearValidDates.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				drcClearValidDates.setEnabled(false);		
				requestedDRCClearDates = true;
			}
		});

		// not yet
		drcIncrementalOn.setSelected(false);
		drcIncrementalOn.setEnabled(false);
		drcUseMultipleThreads.setEnabled(false);
		drcNumberOfThreads.setEditable(false);
		drcEditRulesDeck.setEnabled(false);		
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the DRC tab.
	 */
	private void termDRC()
	{
		boolean currentIncrementalOn = drcIncrementalOn.isSelected();
		if (currentIncrementalOn != initialDRCIncrementalOn)
			DRC.setIncrementalDRCOn(currentIncrementalOn);

		boolean currentOneErrorPerCell = drcOneErrorPerCell.isSelected();
		if (currentOneErrorPerCell != initialDRCOneErrorPerCell)
			DRC.setOneErrorPerCell(currentOneErrorPerCell);

		boolean currentUseMultipleThreads = drcUseMultipleThreads.isSelected();
		if (currentUseMultipleThreads != initialDRCUseMultipleThreads)
			DRC.setUseMultipleThreads(currentUseMultipleThreads);

		int currentNumberOfThreads = TextUtils.atoi(drcNumberOfThreads.getText());
		if (currentNumberOfThreads != initialDRCNumberOfThreads)
			DRC.setNumberOfThreads(currentNumberOfThreads);

		boolean currentIgnoreCenterCuts = drcIgnoreCenterCuts.isSelected();
		if (currentIgnoreCenterCuts != initialDRCIgnoreCenterCuts)
			DRC.setIgnoreCenterCuts(currentIgnoreCenterCuts);

		if (requestedDRCClearDates) DRC.resetDRCDates();
	}

	//******************************** DESIGN RULES ********************************

	private JList designRulesFromList, designRulesToList;
	private DefaultListModel designRulesFromModel, designRulesToModel;
	private DRC.Rules drRules;
	private boolean designRulesUpdating = false;
	private boolean [] designRulesValidLayers;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Design Rules tab.
	 */
	private void initDesignRules()
	{
		Technology tech = Technology.getCurrent();
		drRules = DRC.getRules(tech);

		drLayers.setSelected(true);

		// build the "from" layer/node list
		designRulesFromModel = new DefaultListModel();
		designRulesFromList = new JList(designRulesFromModel);
		designRulesFromList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		drFromList.setViewportView(designRulesFromList);
		designRulesFromList.clearSelection();
		designRulesFromList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { designRulesGetSelectedLayerLoadDRCToList(); }
		});

		// build the "to" layer list
		designRulesToModel = new DefaultListModel();
		designRulesToList = new JList(designRulesToModel);
		designRulesToList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		drToList.setViewportView(designRulesToList);
		designRulesToList.clearSelection();
		designRulesToList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { designRulesShowSelectedLayerRules(); }
		});

		// catch the "show only lines with valid rules" checkbox
		drShowOnlyLinesWithRules.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { designRulesGetSelectedLayerLoadDRCToList(); }
		});

		// have the radio buttons trigger redisplay
		drLayers.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { designRulesWhichSetChanged(); }
		});
		drNodes.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { designRulesWhichSetChanged(); }
		});

		// have changes to edit fields get detected immediately
		DRCDocumentListener myDocumentListener = new DRCDocumentListener(this);
		drMinWidth.getDocument().addDocumentListener(myDocumentListener);
		drMinWidthRule.getDocument().addDocumentListener(myDocumentListener);
		drMinHeight.getDocument().addDocumentListener(myDocumentListener);
		drNormalConnected.getDocument().addDocumentListener(myDocumentListener);
		drNormalConnectedRule.getDocument().addDocumentListener(myDocumentListener);
		drNormalUnconnected.getDocument().addDocumentListener(myDocumentListener);
		drNormalUnconnectedRule.getDocument().addDocumentListener(myDocumentListener);
		drNormalEdge.getDocument().addDocumentListener(myDocumentListener);
		drNormalEdgeRule.getDocument().addDocumentListener(myDocumentListener);
		drWideConnected.getDocument().addDocumentListener(myDocumentListener);
		drWideConnectedRule.getDocument().addDocumentListener(myDocumentListener);
		drWideUnconnected.getDocument().addDocumentListener(myDocumentListener);
		drWideUnconnectedRule.getDocument().addDocumentListener(myDocumentListener);
		drMultiConnected.getDocument().addDocumentListener(myDocumentListener);
		drMultiConnectedRule.getDocument().addDocumentListener(myDocumentListener);
		drMultiUnconnected.getDocument().addDocumentListener(myDocumentListener);
		drMultiUnconnectedRule.getDocument().addDocumentListener(myDocumentListener);
		drWideLimit.getDocument().addDocumentListener(myDocumentListener);

		// figure out which layers are valid
		designRulesValidLayers = new boolean[drRules.numLayers];
		for(int i=0; i<drRules.numLayers; i++) designRulesValidLayers[i] = false;
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			if (np.isNotUsed()) continue;
			Technology.NodeLayer [] layers = np.getLayers();
			for(int i=0; i<layers.length; i++)
			{
				Technology.NodeLayer nl = layers[i];
				Layer layer = nl.getLayer();
				designRulesValidLayers[layer.getIndex()] = true;
			}
		}
		for(Iterator it = tech.getArcs(); it.hasNext(); )
		{
			PrimitiveArc ap = (PrimitiveArc)it.next();
			if (ap.isNotUsed()) continue;
			Technology.ArcLayer [] layers = ap.getLayers();
			for(int i=0; i<layers.length; i++)
			{
				Technology.ArcLayer al = layers[i];
				Layer layer = al.getLayer();
				designRulesValidLayers[layer.getIndex()] = true;
			}
		}

		// load the dialog
		drTechName.setText(drRules.techName);
		designRulesSetupForLayersOrNodes();
		designRulesGetSelectedLayerLoadDRCToList();
	}

	/**
	 * Method called when the "Layers" or "Nodes" radio buttons are selected
	 */
	private void designRulesWhichSetChanged()
	{
		designRulesSetupForLayersOrNodes();
		designRulesGetSelectedLayerLoadDRCToList();
	}

	/**
	 * Class to handle special changes to changes to design rules.
	 */
	private static class DRCDocumentListener implements DocumentListener
	{
		ToolOptions dialog;

		DRCDocumentListener(ToolOptions dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.designRulesEditChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.designRulesEditChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.designRulesEditChanged(); }
	}

	/**
	 * Method called when the user changes any edit field.
	 */
	private void designRulesEditChanged()
	{
		if (designRulesUpdating) return;
		if (drNodes.isSelected())
		{
			// show node information
			int lineNo = designRulesFromList.getSelectedIndex();
			if (lineNo < 0) return;
			String nName = (String)designRulesFromList.getSelectedValue();
			for(int node=0; node<drRules.numNodes; node++)
			{
				if (!nName.equals(drRules.nodeNames[node])) continue;
				String a = drMinWidth.getText();
				if (a.length() == 0) drRules.minNodeSize[node*2] = new Double(-1); else
					drRules.minNodeSize[node*2] = new Double(a);
				a = drMinHeight.getText();
				if (a.length() == 0) drRules.minNodeSize[node*2+1] = new Double(-1); else
					drRules.minNodeSize[node*2+1] = new Double(a);
				drRules.minNodeSizeRules[node] = drMinWidthRule.getText();
				break;
			}
		} else
		{
			// get layer information
			int layer1 = designRulesGetSelectedLayer(designRulesFromList);
			if (layer1 < 0) return;
			int layer2 = designRulesGetSelectedLayer(designRulesToList);
			if (layer2 < 0) return;
			int lowLayer = layer1, highLayer = layer2;
			if (lowLayer > highLayer) { int temp = lowLayer; lowLayer = highLayer;  highLayer = temp; }
			int dindex = (lowLayer+1) * (lowLayer/2) + (lowLayer&1) * ((lowLayer+1)/2);
			dindex = highLayer + drRules.numLayers * lowLayer - dindex;

			// get new normal spacing values
			String a = drNormalConnected.getText();
			if (a.length() == 0) drRules.conList[dindex] = new Double(-1); else
				drRules.conList[dindex] = new Double(a);
			drRules.conListRules[dindex] = drNormalConnectedRule.getText();
			a = drNormalUnconnected.getText();
			if (a.length() == 0) drRules.unConList[dindex] = new Double(-1); else
				drRules.unConList[dindex] = new Double(a);
			drRules.unConListRules[dindex] = drNormalUnconnectedRule.getText();

			// get new wide values
			a = drWideConnected.getText();
			if (a.length() == 0) drRules.conListWide[dindex] = new Double(-1); else
				drRules.conListWide[dindex] = new Double(a);
			drRules.conListWideRules[dindex] = drWideConnectedRule.getText();
			a = drWideUnconnected.getText();
			if (a.length() == 0) drRules.unConListWide[dindex] = new Double(-1); else
				drRules.unConListWide[dindex] = new Double(a);
			drRules.unConListWideRules[dindex] = drWideUnconnectedRule.getText();

			// get new multicut values
			a = drMultiConnected.getText();
			if (a.length() == 0) drRules.conListMulti[dindex] = new Double(-1); else
				drRules.conListMulti[dindex] = new Double(a);
			drRules.conListMultiRules[dindex] = drMultiConnectedRule.getText();
			a = drMultiUnconnected.getText();
			if (a.length() == 0) drRules.unConListMulti[dindex] = new Double(-1); else
				drRules.unConListMulti[dindex] = new Double(a);
			drRules.unConListMultiRules[dindex] = drMultiUnconnectedRule.getText();

			// get new edge values
			a = drNormalEdge.getText();
			if (a.length() == 0) drRules.edgeList[dindex] = new Double(-1); else
				drRules.edgeList[dindex] = new Double(a);
			drRules.edgeListRules[dindex] = drNormalEdgeRule.getText();

			// get new width limit
			a = drWideLimit.getText();
			drRules.wideLimit = new Double(a);

			// redraw the entry in the "to" list
			int lineNo = designRulesToList.getSelectedIndex();
			String line = drMakeToListLine(dindex, lineNo, false);
			designRulesToModel.setElementAt(line, lineNo);

			// update layer width rules
			a = drMinWidth.getText();
			if (a.length() == 0) drRules.minWidth[layer1] = new Double(-1); else
				drRules.minWidth[layer1] = new Double(a);
			drRules.minWidthRules[layer1] = drMinWidthRule.getText();
		}
	}

	private void designRulesSetupForLayersOrNodes()
	{
		designRulesFromModel.clear();
		designRulesUpdating = true;
		if (drNodes.isSelected())
		{
			// list the nodes
			for(int i=0; i<drRules.numNodes; i++)
				designRulesFromModel.addElement(drRules.nodeNames[i]);
			drMinHeight.setEditable(true);

			drNormalConnected.setText("");
			drNormalConnected.setEditable(false);
			drNormalConnectedRule.setText("");
			drNormalConnectedRule.setEditable(false);
			drNormalUnconnected.setText("");
			drNormalUnconnected.setEditable(false);
			drNormalUnconnectedRule.setText("");
			drNormalUnconnectedRule.setEditable(false);

			drNormalEdge.setText("");
			drNormalEdge.setEditable(false);
			drNormalEdgeRule.setText("");
			drNormalEdgeRule.setEditable(false);
			drWideLimit.setText("");
			drWideLimit.setEditable(false);

			drWideConnected.setText("");
			drWideConnected.setEditable(false);
			drWideConnectedRule.setText("");
			drWideConnectedRule.setEditable(false);
			drWideUnconnected.setText("");
			drWideUnconnected.setEditable(false);
			drWideUnconnectedRule.setText("");
			drWideUnconnectedRule.setEditable(false);

			drMultiConnected.setText("");
			drMultiConnected.setEditable(false);
			drMultiConnectedRule.setText("");
			drMultiConnectedRule.setEditable(false);
			drMultiUnconnected.setText("");
			drMultiUnconnected.setEditable(false);
			drMultiUnconnectedRule.setText("");
			drMultiUnconnectedRule.setEditable(false);

			drShowOnlyLinesWithRules.setEnabled(false);
			drToList.setEnabled(false);
		} else
		{
			// list the layers
			for(int i=0; i<drRules.numLayers; i++)
			{
				if (!designRulesValidLayers[i]) continue;
				designRulesFromModel.addElement(drRules.layerNames[i]);
			}
			drMinHeight.setText("");
			drMinHeight.setEditable(false);

			drNormalConnected.setEditable(true);
			drNormalConnectedRule.setEditable(true);
			drNormalUnconnected.setEditable(true);
			drNormalUnconnectedRule.setEditable(true);
			drNormalEdge.setEditable(true);
			drNormalEdgeRule.setEditable(true);
			drWideLimit.setEditable(true);
			drWideLimit.setText(drRules.wideLimit.toString());
			drWideConnected.setEditable(true);
			drWideConnectedRule.setEditable(true);
			drWideUnconnected.setEditable(true);
			drWideUnconnectedRule.setEditable(true);
			drMultiConnected.setEditable(true);
			drMultiConnectedRule.setEditable(true);
			drMultiUnconnected.setEditable(true);
			drMultiUnconnectedRule.setEditable(true);
			drShowOnlyLinesWithRules.setEnabled(true);
			drToList.setEnabled(true);
		}
		designRulesFromList.setSelectedIndex(0);
		designRulesUpdating = false;
	}

	/*
	 * Method to show the detail on the selected layer/node in the upper scroll area.
	 * Also called when the user clicks on the "show only lines with rules" checkbox.
	 */
	private void designRulesGetSelectedLayerLoadDRCToList()
	{
		designRulesUpdating = true;
		if (drNodes.isSelected())
		{
			// show node information
			int j = designRulesFromList.getSelectedIndex();
			double wid = drRules.minNodeSize[j*2].doubleValue();
			if (wid < 0) drMinWidth.setText(""); else
				drMinWidth.setText(Double.toString(wid));
			double hei = drRules.minNodeSize[j*2+1].doubleValue();
			if (hei < 0) drMinHeight.setText(""); else
				drMinHeight.setText(Double.toString(hei));
			drMinWidthRule.setText(drRules.minNodeSizeRules[j]);
		} else
		{
			// show layer information
			boolean onlyvalid = drShowOnlyLinesWithRules.isSelected();
			int j = designRulesGetSelectedLayer(designRulesFromList);
			if (j >= 0)
			{
				if (drRules.minWidth[j].doubleValue() < 0) drMinWidth.setText(""); else
					drMinWidth.setText(drRules.minWidth[j].toString());
				drMinWidthRule.setText(drRules.minWidthRules[j]);
				designRulesToModel.clear();
				int count = 0;
				for(int i=0; i<drRules.numLayers; i++)
				{
					if (!designRulesValidLayers[i]) continue;
					int layer1 = j;
					int layer2 = i;
					if (layer1 > layer2) { int temp = layer1; layer1 = layer2;  layer2 = temp; }
					int dindex = (layer1+1) * (layer1/2) + (layer1&1) * ((layer1+1)/2);
					dindex = layer2 + drRules.numLayers * layer1 - dindex;

					String line = drMakeToListLine(dindex, i, onlyvalid);
					if (line.length() == 0) continue;
					designRulesToModel.addElement(line);
					count++;
				}
				if (count > 0) designRulesToList.setSelectedIndex(0);
			}
		}
		designRulesUpdating = false;
		designRulesShowSelectedLayerRules();
	}

	private String drMakeToListLine(int dindex, int lindex, boolean onlyValid)
	{
		String conDist = "";
		if (drRules.conList[dindex].doubleValue() >= 0) conDist = drRules.conList[dindex].toString();
		String unConDist = "";
		if (drRules.unConList[dindex].doubleValue() >= 0) unConDist = drRules.unConList[dindex].toString();
		String conDistWide = "";
		if (drRules.conListWide[dindex].doubleValue() >= 0) conDistWide = drRules.conListWide[dindex].toString();
		String unConDistWide = "";
		if (drRules.unConListWide[dindex].doubleValue() >= 0) unConDistWide = drRules.unConListWide[dindex].toString();
		String conDistMulti = "";
		if (drRules.conListMulti[dindex].doubleValue() >= 0) conDistMulti = drRules.conListMulti[dindex].toString();
		String unConDistMulti = "";
		if (drRules.unConListMulti[dindex].doubleValue() >= 0) unConDistMulti = drRules.unConListMulti[dindex].toString();
		String edgeDist = "";
		if (drRules.edgeList[dindex].doubleValue() >= 0) edgeDist = drRules.edgeList[dindex].toString();
		if (onlyValid)
		{
			if (conDist.length() == 0 && unConDist.length() == 0 && conDistWide.length() == 0 &&
				unConDistWide.length() == 0 && conDistMulti.length() == 0 && unConDistMulti.length() == 0 &&
				edgeDist.length() == 0) return "";
		}
		String ret = drRules.layerNames[lindex] + " (" +
			conDist + "/" + unConDist + "/" + conDistWide + "/" +  unConDistWide + "/" + conDistMulti + "/" +
			unConDistMulti + "/" + edgeDist + ")";
		return ret;
	}

	private int designRulesGetSelectedLayer(JList theList)
	{
		int lineNo = theList.getSelectedIndex();
		if (lineNo < 0) return -1;
		String lName = (String)theList.getSelectedValue();
		int termPos = lName.indexOf(" (");
		if (termPos >= 0) lName = lName.substring(0, termPos);
		for(int layer=0; layer<drRules.numLayers; layer++)
			if (lName.equals(drRules.layerNames[layer])) return layer;
		return -1;
	}

	/**
	 * Method called when the user clicks in the bottom list (the "to layer" list).
	 */
	private void designRulesShowSelectedLayerRules()
	{
		if (designRulesUpdating) return;

		// nothing to show if node information is requested
		if (drNodes.isSelected()) return;

		// show layer information
		designRulesUpdating = true;
		drNormalConnected.setText("");    drNormalConnectedRule.setText("");
		drNormalUnconnected.setText("");  drNormalUnconnectedRule.setText("");
		drWideConnected.setText("");      drWideConnectedRule.setText("");
		drWideUnconnected.setText("");    drWideUnconnectedRule.setText("");
		drMultiConnected.setText("");     drMultiConnectedRule.setText("");
		drMultiUnconnected.setText("");   drMultiUnconnectedRule.setText("");
		drNormalEdge.setText("");         drNormalEdgeRule.setText("");

		int layer1 = designRulesGetSelectedLayer(designRulesFromList);
		if (layer1 < 0) return;
		int layer2 = designRulesGetSelectedLayer(designRulesToList);
		if (layer2 < 0) return;

		if (layer1 > layer2) { int temp = layer1; layer1 = layer2;  layer2 = temp; }
		int dindex = (layer1+1) * (layer1/2) + (layer1&1) * ((layer1+1)/2);
		dindex = layer2 + drRules.numLayers * layer1 - dindex;

		if (drRules.conList[dindex].doubleValue() >= 0)
			drNormalConnected.setText(drRules.conList[dindex].toString());
		if (drRules.unConList[dindex].doubleValue() >= 0)
			drNormalUnconnected.setText(drRules.unConList[dindex].toString());
		if (drRules.conListWide[dindex].doubleValue() >= 0)
			drWideConnected.setText(drRules.conListWide[dindex].toString());
		if (drRules.unConListWide[dindex].doubleValue() >= 0)
			drWideUnconnected.setText(drRules.unConListWide[dindex].toString());
		if (drRules.conListMulti[dindex].doubleValue() >= 0)
			drMultiConnected.setText(drRules.conListMulti[dindex].toString());
		if (drRules.unConListMulti[dindex].doubleValue() >= 0)
			drMultiUnconnected.setText(drRules.unConListMulti[dindex].toString());
		if (drRules.edgeList[dindex].doubleValue() >= 0)
			drNormalEdge.setText(drRules.edgeList[dindex].toString());

		drNormalConnectedRule.setText(drRules.conListRules[dindex]);
		drNormalUnconnectedRule.setText(drRules.unConListRules[dindex]);
		drWideConnectedRule.setText(drRules.conListWideRules[dindex]);
		drWideUnconnectedRule.setText(drRules.unConListWideRules[dindex]);
		drMultiConnectedRule.setText(drRules.conListMultiRules[dindex]);
		drMultiUnconnectedRule.setText(drRules.unConListMultiRules[dindex]);
		drNormalEdgeRule.setText(drRules.edgeListRules[dindex]);
		designRulesUpdating = false;
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Design Rules tab.
	 */
	private void termDesignRules()
	{
		DRC.modifyRules(drRules);
	}

	//******************************** SPICE ********************************

	private JList spiceLayerList;
	private JList spiceCellList;
	private DefaultListModel spiceLayerListModel;
	private DefaultListModel spiceCellListModel;
	private int spiceEngineInitial;
	private String spiceLevelInitial;
	private String spiceOutputFormatInitial;
	private String spicePartsLibraryInitial;
	private boolean spiceUseParasiticsInitial;
	private boolean spiceUseNodeNamesInitial;
	private boolean spiceForceGlobalPwrGndInitial;
	private boolean spiceUseCellParametersInitial;
//	private boolean spiceWriteTransSizesInLambdaInitial;
	private double spiceTechMinResistanceInitial;
	private double spiceTechMinCapacitanceInitial;
	private String spiceHeaderCardInitial;
	private String spiceTrailerCardInitial;
	private HashMap spiceLayerResistanceOptions;
	private HashMap spiceLayerCapacitanceOptions;
	private HashMap spiceLayerEdgeCapacitanceOptions;
	private HashMap spiceCellModelOptions;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Spice tab.
	 */
	private void initSpice()
	{
		// the top section: general controls
		spiceEngineInitial = Simulation.getSpiceEngine();
		spiceEnginePopup.addItem("Spice 2");
		spiceEnginePopup.addItem("Spice 3");
		spiceEnginePopup.addItem("HSpice");
		spiceEnginePopup.addItem("PSpice");
		spiceEnginePopup.addItem("Gnucap");
		spiceEnginePopup.addItem("SmartSpice");
		spiceEnginePopup.setSelectedIndex(spiceEngineInitial);

		spiceLevelInitial = Simulation.getSpiceLevel();
		spiceLevelPopup.addItem("1");
		spiceLevelPopup.addItem("2");
		spiceLevelPopup.addItem("3");
		spiceLevelPopup.setSelectedItem(spiceLevelInitial);

		spiceOutputFormatInitial = Simulation.getSpiceOutputFormat();
		spiceOutputFormatPopup.addItem("Standard");
		spiceOutputFormatPopup.addItem("Raw");
		spiceOutputFormatPopup.addItem("Raw/Smart");
		spiceOutputFormatPopup.setSelectedItem(spiceOutputFormatInitial);

		spiceRunPopup.addItem("Don't Run SPICE");
		spiceRunPopup.addItem("Run SPICE");
		spiceRunPopup.addItem("Run SPICE Quietly");
		spiceRunPopup.addItem("Run SPICE, Read Output");
		spiceRunPopup.addItem("Run SPICE Quietly, Read Output");
		spiceRunPopup.setEnabled(false);

		spiceUseParasiticsInitial = Simulation.isSpiceUseParasitics();
		spiceUseParasitics.setSelected(spiceUseParasiticsInitial);

		spiceUseNodeNamesInitial = Simulation.isSpiceUseNodeNames();
		spiceUseNodeNames.setSelected(spiceUseNodeNamesInitial);

		spiceForceGlobalPwrGndInitial = Simulation.isSpiceForceGlobalPwrGnd();
		spiceForceGlobalPwrGnd.setSelected(spiceForceGlobalPwrGndInitial);

		spiceUseCellParametersInitial = Simulation.isSpiceUseCellParameters();
		spiceUseCellParameters.setSelected(spiceUseCellParametersInitial);

//		spiceWriteTransSizesInLambdaInitial = Simulation.isSpiceWriteTransSizeInLambda();
//		spiceWriteTransSizesInLambda.setSelected(spiceWriteTransSizesInLambdaInitial);
		spiceWriteTransSizesInLambda.setEnabled(false);

		spiceRunParameters.setEnabled(false);

		spicePartsLibraryInitial = Simulation.getSpicePartsLibrary();
		String [] libFiles = LibFile.getSpicePartsLibraries();
		for(int i=0; i<libFiles.length; i++)
			spicePrimitivesetPopup.addItem(libFiles[i]);
		spicePrimitivesetPopup.setSelectedItem(spicePartsLibraryInitial);

		// the next section: parasitic values
		spiceTechnology.setText("For technology " + curTech.getTechName());

		spiceLayerResistanceOptions = new HashMap();
		spiceLayerCapacitanceOptions = new HashMap();
		spiceLayerEdgeCapacitanceOptions = new HashMap();
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			spiceLayerResistanceOptions.put(layer, Option.newDoubleOption(layer.getResistance()));
			spiceLayerCapacitanceOptions.put(layer, Option.newDoubleOption(layer.getCapacitance()));
			spiceLayerEdgeCapacitanceOptions.put(layer, Option.newDoubleOption(layer.getEdgeCapacitance()));
		}
		spiceLayerListModel = new DefaultListModel();
		spiceLayerList = new JList(spiceLayerListModel);
		spiceLayerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		spiceLayer.setViewportView(spiceLayerList);
		spiceLayerList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { spiceLayerListClick(); }
		});
		showLayersInTechnology(spiceLayerListModel);
		spiceLayerList.setSelectedIndex(0);
		spiceLayerListClick();
		spiceResistance.getDocument().addDocumentListener(new LayerDocumentListener(spiceLayerResistanceOptions, spiceLayerList, curTech));
		spiceCapacitance.getDocument().addDocumentListener(new LayerDocumentListener(spiceLayerCapacitanceOptions, spiceLayerList, curTech));
		spiceEdgeCapacitance.getDocument().addDocumentListener(new LayerDocumentListener(spiceLayerEdgeCapacitanceOptions, spiceLayerList, curTech));
		
		spiceTechMinResistanceInitial = curTech.getMinResistance();
		spiceMinResistance.setText(Double.toString(spiceTechMinResistanceInitial));

		spiceTechMinCapacitanceInitial = curTech.getMinCapacitance();
		spiceMinCapacitance.setText(Double.toString(spiceTechMinCapacitanceInitial));

		// the next section: header and trailer cards
		spiceHeaderCardInitial = Simulation.getSpiceHeaderCardInfo();
		if (spiceHeaderCardInitial.length() == 0) spiceNoHeaderCards.setSelected(true); else
		{
			if (spiceHeaderCardInitial.startsWith(":::::"))
			{
				spiceHeaderCardsWithExtension.setSelected(true);
				spiceHeaderCardExtension.setText(spiceHeaderCardInitial.substring(5));
			} else
			{
				spiceHeaderCardsFromFile.setSelected(true);
				spiceHeaderCardFile.setText(spiceHeaderCardInitial);
			}
		}
		spiceTrailerCardInitial = Simulation.getSpiceTrailerCardInfo();
		if (spiceTrailerCardInitial.length() == 0) spiceNoTrailerCards.setSelected(true); else
		{
			if (spiceTrailerCardInitial.startsWith(":::::"))
			{
				spiceTrailerCardsWithExtension.setSelected(true);
				spiceTrailerCardExtension.setText(spiceTrailerCardInitial.substring(5));
			} else
			{
				spiceTrailerCardsFromFile.setSelected(true);
				spiceTrailerCardFile.setText(spiceTrailerCardInitial);
			}
		}
		spiceBrowseHeaderFile.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceBrowseHeaderFileActionPerformed(); }
		});
		spiceBrowseTrailerFile.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceBrowseTrailerFileActionPerformed(); }
		});

		// the last section has cell overrides
		spiceCellModelOptions = new HashMap();
		spiceCellListModel = new DefaultListModel();
		spiceCellList = new JList(spiceCellListModel);
		spiceCellList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		spiceCell.setViewportView(spiceCellList);
		for(Iterator it = curLib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			spiceCellListModel.addElement(cell.noLibDescribe());
			String modelFile = "";
			Variable var = cell.getVar(Spice.SPICE_MODEL_FILE_KEY, String.class);
			if (var != null) modelFile = (String)var.getObject();
			spiceCellModelOptions.put(cell, Option.newStringOption(modelFile));
		}
		spiceCellList.setSelectedIndex(0);
		spiceCellList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { spiceCellListClick(); }
		});
		spiceModelFileBrowse.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceModelFileBrowseActionPerformed(); }
		});
		spiceDeriveModelFromCircuit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceCellModelButtonClick(); }
		});
		spiceUseModelFromFile.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceCellModelButtonClick(); }
		});
		spiceModelCell.getDocument().addDocumentListener(new SpiceModelDocumentListener(this));
		spiceCellListClick();
	}

	private boolean spiceModelFileChanging = false;

	/**
	 * Method called when the user clicks on a model file radio button at in the bottom of the Spice Options dialog.
	 */
	private void spiceCellModelButtonClick()
	{
		if (spiceDeriveModelFromCircuit.isSelected()) spiceModelCell.setText("");
	}

	/**
	 * Method called when the user clicks on a cell name at in the bottom of the Spice Options dialog.
	 */
	private void spiceCellListClick()
	{
		String cellName = (String)spiceCellList.getSelectedValue();
		Cell cell = curLib.findNodeProto(cellName);
		if (cell != null)
		{
			Option option = (Option)spiceCellModelOptions.get(cell);
			String modelFile = option.getStringValue();
			spiceModelFileChanging = true;
			spiceModelCell.setText(modelFile);
			if (modelFile.length() == 0) spiceDeriveModelFromCircuit.setSelected(true); else
				spiceUseModelFromFile.setSelected(true);
			spiceModelFileChanging = false;
		}
	}

	/**
	 * Method called when the user clicks on the "Browse" button in the bottom of the Spice Options dialog.
	 */
	private void spiceModelFileBrowseActionPerformed()
	{
		String fileName = OpenFile.chooseInputFile(OpenFile.Type.ANY, null);
		if (fileName == null) return;
		spiceUseModelFromFile.setSelected(true);
		spiceModelCell.setText(fileName);
	}

	/**
	 * Method called when the user changes the model file name at the bottom of the Spice Options dialog.
	 */
	private void spiceModelFileChanged()
	{
		if (spiceModelFileChanging) return;
		String cellName = (String)spiceCellList.getSelectedValue();
		Cell cell = curLib.findNodeProto(cellName);
		if (cell != null)
		{
			Option option = (Option)spiceCellModelOptions.get(cell);
			String typedString = spiceModelCell.getText();
			if (spiceDeriveModelFromCircuit.isSelected()) typedString = "";
			option.setStringValue(typedString);
		}
	}

	/**
	 * Class to handle changes to per-cell model file names.
	 */
	private static class SpiceModelDocumentListener implements DocumentListener
	{
		ToolOptions dialog;

		SpiceModelDocumentListener(ToolOptions dialog)
		{
			this.dialog = dialog;
		}

		public void changedUpdate(DocumentEvent e) { dialog.spiceModelFileChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.spiceModelFileChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.spiceModelFileChanged(); }
	}

	private void spiceBrowseTrailerFileActionPerformed()
	{
		String fileName = OpenFile.chooseInputFile(OpenFile.Type.ANY, null);
		if (fileName == null) return;
		spiceTrailerCardFile.setText(fileName);
		spiceTrailerCardsFromFile.setSelected(true);
	}

	private void spiceBrowseHeaderFileActionPerformed()
	{
		String fileName = OpenFile.chooseInputFile(OpenFile.Type.ANY, null);
		if (fileName == null) return;
		spiceHeaderCardFile.setText(fileName);
		spiceHeaderCardsFromFile.setSelected(true);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Spice tab.
	 */
	private void termSpice()
	{
		// the top section: general controls
		int intNow = spiceEnginePopup.getSelectedIndex();
		if (spiceEngineInitial != intNow) Simulation.setSpiceEngine(intNow);

		String stringNow = (String)spiceLevelPopup.getSelectedItem();
		if (!spiceLevelInitial.equals(stringNow)) Simulation.setSpiceLevel(stringNow);

		stringNow = (String)spiceOutputFormatPopup.getSelectedItem();
		if (!spiceOutputFormatInitial.equals(stringNow)) Simulation.setSpiceOutputFormat(stringNow);

		stringNow = (String)spicePrimitivesetPopup.getSelectedItem();
		if (!spicePartsLibraryInitial.equals(stringNow)) Simulation.setSpicePartsLibrary(stringNow);

		boolean booleanNow = spiceUseNodeNames.isSelected();
		if (spiceUseNodeNamesInitial != booleanNow) Simulation.setSpiceUseNodeNames(booleanNow);

		booleanNow = spiceForceGlobalPwrGnd.isSelected();
		if (spiceForceGlobalPwrGndInitial != booleanNow) Simulation.setSpiceForceGlobalPwrGnd(booleanNow);

		booleanNow = spiceUseCellParameters.isSelected();
		if (spiceUseCellParametersInitial != booleanNow) Simulation.setSpiceUseCellParameters(booleanNow);

//		booleanNow = spiceWriteTransSizesInLambda.isSelected();
//		if (spiceWriteTransSizesInLambdaInitial != booleanNow) Simulation.setSpiceWriteTransSizeInLambda(booleanNow);

		booleanNow = spiceUseParasitics.isSelected();
		if (spiceUseParasiticsInitial != booleanNow) Simulation.setSpiceUseParasitics(booleanNow);

		// the next section: parasitic values
		double doubleNow = TextUtils.atof(spiceMinResistance.getText());
		if (spiceTechMinResistanceInitial != doubleNow) curTech.setMinResistance(doubleNow);
		doubleNow = TextUtils.atof(spiceMinCapacitance.getText());
		if (spiceTechMinCapacitanceInitial != doubleNow) curTech.setMinCapacitance(doubleNow);

		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			Option resistanceOption = (Option)spiceLayerResistanceOptions.get(layer);
			if (resistanceOption != null && resistanceOption.isChanged())
				layer.setResistance(resistanceOption.getDoubleValue());
			Option capacitanceOption = (Option)spiceLayerCapacitanceOptions.get(layer);
			if (capacitanceOption != null && capacitanceOption.isChanged())
				layer.setCapacitance(capacitanceOption.getDoubleValue());
			Option edgeCapacitanceOption = (Option)spiceLayerEdgeCapacitanceOptions.get(layer);
			if (edgeCapacitanceOption != null && edgeCapacitanceOption.isChanged())
				layer.setEdgeCapacitance(edgeCapacitanceOption.getDoubleValue());
		}

		// the next section: header and trailer cards
		String header = "";
		if (spiceHeaderCardsWithExtension.isSelected())
		{
			header = ":::::" + spiceHeaderCardExtension.getText();
		} else if (spiceHeaderCardsFromFile.isSelected())
		{
			header = spiceHeaderCardFile.getText();
		}
		if (!spiceTrailerCardInitial.equals(header)) Simulation.setSpiceHeaderCardInfo(header);

		String trailer = "";
		if (spiceTrailerCardsWithExtension.isSelected())
		{
			trailer = ":::::" + spiceTrailerCardExtension.getText();
		} else if (spiceTrailerCardsFromFile.isSelected())
		{
			trailer = spiceTrailerCardFile.getText();
		}
		if (spiceTrailerCardInitial.equals(trailer)) Simulation.setSpiceTrailerCardInfo(trailer);

		// bottom section: model file overrides for cells
		for(Iterator it = curLib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			Option opt = (Option)spiceCellModelOptions.get(cell);
			if (opt == null) continue;
			if (opt.isChanged())
			{
				String fileName = opt.getStringValue().trim();
				if (fileName.length() == 0) cell.delVar(Spice.SPICE_MODEL_FILE_KEY); else
					cell.newVar(Spice.SPICE_MODEL_FILE_KEY, fileName);
			}
		}
	}

	private void spiceLayerListClick()
	{
		String layerName = (String)spiceLayerList.getSelectedValue();
		Layer layer = curTech.findLayer(layerName);
		if (layer != null)
		{
			Option resistanceOption = (Option)spiceLayerResistanceOptions.get(layer);
			spiceResistance.setText(Double.toString(resistanceOption.getDoubleValue()));
			Option capacitanceOption = (Option)spiceLayerCapacitanceOptions.get(layer);
			spiceCapacitance.setText(Double.toString(capacitanceOption.getDoubleValue()));
			Option edgeCapacitanceOption = (Option)spiceLayerEdgeCapacitanceOptions.get(layer);
			spiceEdgeCapacitance.setText(Double.toString(edgeCapacitanceOption.getDoubleValue()));
		}
	}

	/**
	 * Class to handle special changes to per-layer parasitics.
	 */
	private static class LayerDocumentListener implements DocumentListener
	{
		HashMap optionMap;
		JList list;
		Technology tech;

		LayerDocumentListener(HashMap optionMap, JList list, Technology tech)
		{
			this.optionMap = optionMap;
			this.list = list;
			this.tech = tech;
		}

		private void change(DocumentEvent e)
		{
			// get the currently selected layer
			String layerName = (String)list.getSelectedValue();
			Layer layer = tech.findLayer(layerName);
			if (layer == null) return;

			// get the typed value
			Document doc = e.getDocument();
			int len = doc.getLength();
			String text;
			try
			{
				text = doc.getText(0, len);
			} catch (BadLocationException ex) { return; }
			Option option = (Option)optionMap.get(layer);
			double v = TextUtils.atof(text);

			// update the option
			option.setDoubleValue(v);
		}

		public void changedUpdate(DocumentEvent e) { change(e); }
		public void insertUpdate(DocumentEvent e) { change(e); }
		public void removeUpdate(DocumentEvent e) { change(e); }
	}

	//******************************** VERILOG ********************************

	private boolean initialVerilogUseAssign;
	private boolean initialVerilogUseTrireg;
	private HashMap initialVerilogBehaveFiles;
	private JList verilogCellList;
	private DefaultListModel verilogCellListModel;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Verilog tab.
	 */
	private void initVerilog()
	{
		initialVerilogUseAssign = Simulation.getVerilogUseAssign();
		verUseAssign.setSelected(initialVerilogUseAssign);

		initialVerilogUseTrireg = Simulation.getVerilogUseTrireg();
		verDefWireTrireg.setSelected(initialVerilogUseTrireg);

		// gather all existing behave file information
		initialVerilogBehaveFiles = new HashMap();
		for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = (Library)lIt.next();
			if (lib.isHidden()) continue;
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				String behaveFile = "";
				Variable var = cell.getVar(Verilog.VERILOG_BEHAVE_FILE_KEY);
				if (var != null) behaveFile = var.getObject().toString();
				initialVerilogBehaveFiles.put(cell, Option.newStringOption(behaveFile));
			}
		}

		// make list of libraries
		for(Iterator it = Library.getVisibleLibrariesSortedByName().iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			verLibrary.addItem(lib.getLibName());
		}
		verLibrary.setSelectedItem(Library.getCurrent().getLibName());
		verLibrary.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { verilogLoadCellList(); }
		});

		// make the list of cells
		verilogCellListModel = new DefaultListModel();
		verilogCellList = new JList(verilogCellListModel);
		verilogCellList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		verCells.setViewportView(verilogCellList);
		verilogCellList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { verilogCellListClick(); }
		});

		verBrowse.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { verModelFileBrowseActionPerformed(); }
		});
		verDeriveModel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { verilogModelClick(); }
		});
		verUseModelFile.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { verilogModelClick(); }
		});
		verFileName.getDocument().addDocumentListener(new VerilogDocumentListener(this));
		verilogLoadCellList();
	}

	private void verModelFileBrowseActionPerformed()
	{
		String fileName = OpenFile.chooseInputFile(OpenFile.Type.VERILOG, null);
		if (fileName == null) return;
		verUseModelFile.setSelected(true);
		verFileName.setEditable(true);
		verFileName.setText(fileName);
	}

	/**
	 * Class to handle special changes to Verilog model file values.
	 */
	private static class VerilogDocumentListener implements DocumentListener
	{
		ToolOptions dialog;

		VerilogDocumentListener(ToolOptions dialog)
		{
			this.dialog = dialog;
		}

		private void change(DocumentEvent e)
		{
			// get the currently selected Cell
			String libName = (String)dialog.verLibrary.getSelectedItem();
			Library lib = Library.findLibrary(libName);
			String cellName = (String)dialog.verilogCellList.getSelectedValue();
			Cell cell = lib.findNodeProto(cellName);
			if (cell == null) return;
			Option behaveOption = (Option)dialog.initialVerilogBehaveFiles.get(cell);
			if (behaveOption == null) return;

			// get the typed value
			Document doc = e.getDocument();
			int len = doc.getLength();
			String text;
			try
			{
				text = doc.getText(0, len);
			} catch (BadLocationException ex) { return; }

			// update the option
			behaveOption.setStringValue(text);
		}

		public void changedUpdate(DocumentEvent e) { change(e); }
		public void insertUpdate(DocumentEvent e) { change(e); }
		public void removeUpdate(DocumentEvent e) { change(e); }
	}

	private void verilogLoadCellList()
	{
		String libName = (String)verLibrary.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		if (lib == null) return;
		verilogCellListModel.clear();
		boolean notEmpty = false;
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			verilogCellListModel.addElement(cell.noLibDescribe());
			notEmpty = true;
		}
		if (notEmpty)
		{
			verilogCellList.setSelectedIndex(0);
			verilogCellListClick();
		}
	}

	private void verilogCellListClick()
	{
		String libName = (String)verLibrary.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		String cellName = (String)verilogCellList.getSelectedValue();
		Cell cell = lib.findNodeProto(cellName);
		if (cell == null) return;
		Option behaveOption = (Option)initialVerilogBehaveFiles.get(cell);
		if (behaveOption == null) return;
		String behaveFile = behaveOption.getStringValue();
		if (behaveFile.length() == 0)
		{
			verDeriveModel.setSelected(true);
			verFileName.setEditable(false);
			verFileName.setText("");
		} else
		{
			verUseModelFile.setSelected(true);
			verFileName.setEditable(true);
			verFileName.setText(behaveFile);
		}
	}

	private void verilogModelClick()
	{
		if (verDeriveModel.isSelected())
		{
			verFileName.setEditable(false);
			verFileName.setText("");
		} else
		{
			verFileName.setEditable(true);
		}
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Verilog tab.
	 */
	private void termVerilog()
	{
		boolean currentUseAssign = verUseAssign.isSelected();
		if (currentUseAssign != initialVerilogUseAssign)
			Simulation.setVerilogUseAssign(currentUseAssign);

		boolean currentUseTrireg = verDefWireTrireg.isSelected();
		if (currentUseTrireg != initialVerilogUseTrireg)
			Simulation.setVerilogUseTrireg(currentUseTrireg);

		for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = (Library)lIt.next();
			if (lib.isHidden()) continue;
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				Option behaveOption = (Option)initialVerilogBehaveFiles.get(cell);
				if (behaveOption == null) continue;
				if (behaveOption.isChanged())
				{
					cell.newVar(Verilog.VERILOG_BEHAVE_FILE_KEY, behaveOption.getStringValue());
				}
			}
		}
	}

	//******************************** FAST HENRY ********************************

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Fast Henry tab.
	 */
	private void initFastHenry()
	{
		fhUseSingleFrequency.setEnabled(false);
		fhMakeMultipole.setEnabled(false);
		fhFrequencyStart.setEditable(false);
		fhFrequencyEnd.setEditable(false);
		fhRunsPerDecade.setEditable(false);
		fhNumberOfPoles.setEditable(false);
		fhDefaultThickness.setEditable(false);
		fhDefaultWidthSubs.setEditable(false);
		fhDefaultHeightSubs.setEditable(false);
		fhMaxSegmentLength.setEditable(false);
		fhMakePostScript.setEnabled(false);
		fhMakeSpice.setEnabled(false);
		fhAfterAction.setEnabled(false);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Fast Henry tab.
	 */
	private void termFastHenry()
	{
	}

	//******************************** WELL CHECK ********************************

	private int initialWellCheckPWellRule;
	private boolean initialWellCheckPWellConnectToGround;
	private int initialWellCheckNWellRule;
	private boolean initialWellCheckNWellConnectToPower;
	private boolean initialWellCheckFindFarthest;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Well Check tab.
	 */
	private void initWellCheck()
	{
		initialWellCheckPWellRule = ERC.getPWellCheck();
		switch (initialWellCheckPWellRule)
		{
			case 0: wellPMustHaveAllContacts.setSelected(true);   break;
			case 1: wellPMustHave1Contact.setSelected(true);      break;
			case 2: wellPNoContactCheck.setSelected(true);        break;
		}

		initialWellCheckPWellConnectToGround = ERC.isMustConnectPWellToGround();
		wellPMustConnectGround.setSelected(initialWellCheckPWellConnectToGround);

		initialWellCheckNWellRule = ERC.getNWellCheck();
		switch (initialWellCheckNWellRule)
		{
			case 0: wellNMustHaveAllContacts.setSelected(true);   break;
			case 1: wellNMustHave1Contact.setSelected(true);      break;
			case 2: wellNNoContactCheck.setSelected(true);        break;
		}

		initialWellCheckNWellConnectToPower = ERC.isMustConnectNWellToPower();
		wellNMustConnectPower.setSelected(initialWellCheckNWellConnectToPower);

		initialWellCheckFindFarthest = ERC.isFindWorstCaseWell();
		wellFindFarthestDistance.setSelected(initialWellCheckFindFarthest);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Well Check tab.
	 */
	private void termWellCheck()
	{
		int currentPWellRule = 0;
		if (wellPMustHave1Contact.isSelected()) currentPWellRule = 1; else
			if (wellPNoContactCheck.isSelected()) currentPWellRule = 2;
		if (currentPWellRule != initialWellCheckPWellRule)
			ERC.setPWellCheck(currentPWellRule);

		boolean currentPWellGroundCheck = wellPMustConnectGround.isSelected();
		if (currentPWellGroundCheck != initialWellCheckPWellConnectToGround)
			ERC.setMustConnectPWellToGround(currentPWellGroundCheck);

		int currentNWellRule = 0;
		if (wellNMustHave1Contact.isSelected()) currentNWellRule = 1; else
			if (wellNNoContactCheck.isSelected()) currentNWellRule = 2;
		if (currentNWellRule != initialWellCheckNWellRule)
			ERC.setNWellCheck(currentNWellRule);

		boolean currentNWellPowerCheck = wellNMustConnectPower.isSelected();
		if (currentNWellPowerCheck != initialWellCheckNWellConnectToPower)
			ERC.setMustConnectNWellToPower(currentNWellPowerCheck);

		boolean currentFarCheck = wellFindFarthestDistance.isSelected();
		if (currentFarCheck != initialWellCheckFindFarthest)
			ERC.setFindWorstCaseWell(currentFarCheck);
	}

	//******************************** ANTENNA RULES ********************************

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Antenna Rules tab.
	 */
	private void initAntennaRules()
	{
		antTechnology.setEnabled(false);
		antArcList.setEnabled(false);
		antMaxRatio.setEditable(false);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Antenna Rules tab.
	 */
	private void termAntennaRules()
	{
	}

	//******************************** NETWORK ********************************

	private boolean netUnifyPwrGndInitial, netUnifyLikeNamedNetsInitial, netIgnoreResistorsInitial;
	private String netUnificationPrefixInitial;
	private boolean netBusBaseZeroInitial, netBusAscendingInitial;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Network tab.
	 */
	private void initNetwork()
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

	private void termNetwork()
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

	//******************************** NCC ********************************

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the NCC tab.
	 */
	private void initNCC()
	{
		nccFirstCell.setEditable(false);
		nccSetFirstCell.setEnabled(false);
		nccNextFirstCell.setEnabled(false);
		nccSecondCell.setEditable(false);
		nccSetSecondCell.setEnabled(false);
		nccNextSecondCell.setEnabled(false);
		nccExpandHierarchy.setEnabled(false);
		nccExpandHierarchyYes.setEnabled(false);
		nccExpandHierarchyNo.setEnabled(false);
		nccExpandHierarchyDefault.setEnabled(false);
		nccMergeParallel.setEnabled(false);
		nccMergeParallelYes.setEnabled(false);
		nccMergeParallelNo.setEnabled(false);
		nccMergeParallelDefault.setEnabled(false);
		nccMergeSeries.setEnabled(false);
		nccMergeSeriesYes.setEnabled(false);
		nccMergeSeriesNo.setEnabled(false);
		nccMergeSeriesDefault.setEnabled(false);
		nccClearDatesThisLibrary.setEnabled(false);
		nccClearDatesAllLibraries.setEnabled(false);
		nccIgnorePwrGnd.setEnabled(false);
		nccCheckExportNames.setEnabled(false);
		nccCheckComponentSizes.setEnabled(false);
		nccSizeTolerancePct.setEditable(false);
		nccSizeToleranceAmt.setEditable(false);
		nccAllowNoCompNets.setEnabled(false);
		nccRecurse.setEnabled(false);
		nccAutomaticResistorExclusion.setEnabled(false);
		nccDebuggingOptions.setEnabled(false);
		nccShowMatchTags.setEnabled(false);
		nccLibraryPopup.setEnabled(false);
		nccCellList.setEnabled(false);
		nccListForcedMatches.setEnabled(false);
		nccRemoveForcedMatches.setEnabled(false);
		nccListOverrides.setEnabled(false);
		nccRemoveOverrides.setEnabled(false);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the NCC tab.
	 */
	private void termNCC()
	{
	}

	//******************************** LOGICAL EFFORT ********************************

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
	private void initLogicalEffort()
	{
		leHighlightComponentsInitial = LETool.isHighlightComponents();
		leHighlightComponents.setSelected(leHighlightComponentsInitial);

		leUseLocalSettingsInitial = LETool.isUseLocalSettings();
		leUseLocalSettings.setSelected(leUseLocalSettingsInitial);

		leDisplayIntermediateCapsInitial = LETool.isShowIntermediateCapacitances();
		leDisplayIntermediateCaps.setSelected(leDisplayIntermediateCapsInitial);

		leGlobalFanOutInitial = LETool.getGlobalFanout();
		leGlobalFanOut.setText(Double.toString(leGlobalFanOutInitial));

		leConvergenceInitial = LETool.getConvergenceEpsilon();
		leConvergence.setText(Double.toString(leConvergenceInitial));

		leMaxIterationsInitial = LETool.getMaxIterations();
		leMaxIterations.setText(Integer.toString(leMaxIterationsInitial));

		leGateCapacitanceInitial = LETool.getGateCapacitance();
		leGateCapacitance.setText(Double.toString(leGateCapacitanceInitial));

		leDefaultWireCapRatioInitial = LETool.getWireRatio();
		leDefaultWireCapRatio.setText(Double.toString(leDefaultWireCapRatioInitial));

		leDiffToGateCapRatioInitial = LETool.getDiffAlpha();
		leDiffToGateCapRatio.setText(Double.toString(leDiffToGateCapRatioInitial));

		leKeeperSizeRatioInitial = LETool.getKeeperRatio();
		leKeeperSizeRatio.setText(Double.toString(leKeeperSizeRatioInitial));

		// make an empty list for the layer names
		leArcOptions = new HashMap();
		for(Iterator it = curTech.getArcs(); it.hasNext(); )
		{
			ArcProto arc = (ArcProto)it.next();
			leArcOptions.put(arc, Option.newDoubleOption(LETool.tool.prefs.getDouble(arc.toString(), 0.0)));
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
	private void termLogicalEffort()
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
			Option option = (Option)leArcOptions.get(arc);
			if (option != null && option.isChanged())
                LETool.tool.prefs.putDouble(arc.toString(), option.getDoubleValue());
		}
	}

	private void leArcListClick(MouseEvent evt)
	{
		String arcName = (String)leArcList.getSelectedValue();
		int firstSpace = arcName.indexOf(' ');
		if (firstSpace > 0) arcName = arcName.substring(0, firstSpace);
		ArcProto arc = curTech.findArcProto(arcName);
		Option option = (Option)leArcOptions.get(arc);
		if (option == null) return;
		leWireRatio.setText(Double.toString(option.getDoubleValue()));
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
			Option option = (Option)optionMap.get(arc);
			if (option == null) return;

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
			option.setDoubleValue(v);
			int index = list.getSelectedIndex();
			String newLine = arc.getProtoName() + " (" + v + ")";
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
			model.addElement(arc.getProtoName() + " (" + ((Option)leArcOptions.get(arc)).getDoubleValue() + ")");
		}
	}

	//******************************** ROUTING ********************************

	private boolean initRoutMimicOn, initRoutAutoOn;
	private ArcProto initRoutDefArc;
	private boolean initRoutMimicCanUnstitch;
	private boolean initRoutMimicInteractive;
	private boolean initRoutMimicMatchPorts;
	private boolean initRoutMimicMatchNumArcs;
	private boolean initRoutMimicMatchNodeSize;
	private boolean initRoutMimicMatchNodeType;
	private boolean initRoutMimicNoArcsSameDir;
	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Routing tab.
	 */
	private void initRouting()
	{
		initRoutMimicOn = Routing.isMimicStitchOn();
		initRoutAutoOn = Routing.isAutoStitchOn();
		if (!initRoutMimicOn && !initRoutAutoOn) routNoStitcher.setSelected(true); else
		{
			if (initRoutMimicOn) routMimicStitcher.setSelected(true); else
				routAutoStitcher.setSelected(true);
		}

		Technology tech = Technology.getCurrent();
		routDefaultArc.addItem("DEFAULT ARC");
		for(Iterator it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = (ArcProto)it.next();
			routDefaultArc.addItem(ap.describe());
		}
		String prefArcName = Routing.getPreferredRoutingArc();
		initRoutDefArc = null;
		if (prefArcName.length() > 0)
			initRoutDefArc = ArcProto.findArcProto(prefArcName);
		if (initRoutDefArc != null)
			routDefaultArc.setSelectedItem(initRoutDefArc.describe());

		initRoutMimicCanUnstitch = Routing.isMimicStitchCanUnstitch();
		routMimicCanUnstitch.setSelected(initRoutMimicCanUnstitch);

		initRoutMimicMatchPorts = Routing.isMimicStitchMatchPorts();
		routMimicPortsMustMatch.setSelected(initRoutMimicMatchPorts);

		initRoutMimicMatchNumArcs = Routing.isMimicStitchMatchNumArcs();
		routMimicNumArcsMustMatch.setSelected(initRoutMimicMatchNumArcs);

		initRoutMimicMatchNodeSize = Routing.isMimicStitchMatchNodeSize();
		routMimicNodeSizesMustMatch.setSelected(initRoutMimicMatchNodeSize);

		initRoutMimicMatchNodeType = Routing.isMimicStitchMatchNodeType();
		routMimicNodeTypesMustMatch.setSelected(initRoutMimicMatchNodeType);

		initRoutMimicNoArcsSameDir = Routing.isMimicStitchNoOtherArcsSameDir();
		routMimicNoOtherArcs.setSelected(initRoutMimicNoArcsSameDir);

		initRoutMimicInteractive = Routing.isMimicStitchInteractive();
		routMimicInteractive.setSelected(initRoutMimicInteractive);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Routing tab.
	 */
	private void termRouting()
	{
		boolean curMimic = routMimicStitcher.isSelected();
		if (curMimic != initRoutMimicOn)
			Routing.setMimicStitchOn(curMimic);
		boolean curAuto = routAutoStitcher.isSelected();
		if (curAuto != initRoutAutoOn)
			Routing.setAutoStitchOn(curAuto);

		ArcProto ap = null;
		int curArcIndex = routDefaultArc.getSelectedIndex();
		if (curArcIndex > 0)
		{
			String curArcName = (String)routDefaultArc.getSelectedItem();
			ap = ArcProto.findArcProto(curArcName);
		}
		if (ap != initRoutDefArc)
		{
			String newArcName = "";
			if (ap != null) newArcName = ap.getTechnology().getTechName() + ":" + ap.getProtoName();
			Routing.setPreferredRoutingArc(newArcName);
		}
		
		boolean cur = routMimicCanUnstitch.isSelected();
		if (cur != initRoutMimicCanUnstitch)
			Routing.setMimicStitchCanUnstitch(cur);

		cur = routMimicPortsMustMatch.isSelected();
		if (cur != initRoutMimicMatchPorts)
			Routing.setMimicStitchMatchPorts(cur);

		cur = routMimicNumArcsMustMatch.isSelected();
		if (cur != initRoutMimicMatchNumArcs)
			Routing.setMimicStitchMatchNumArcs(cur);

		cur = routMimicNodeSizesMustMatch.isSelected();
		if (cur != initRoutMimicMatchNodeSize)
			Routing.setMimicStitchMatchNodeSize(cur);

		cur = routMimicNodeTypesMustMatch.isSelected();
		if (cur != initRoutMimicMatchNodeType)
			Routing.setMimicStitchMatchNodeType(cur);

		cur = routMimicNoOtherArcs.isSelected();
		if (cur != initRoutMimicNoArcsSameDir)
			Routing.setMimicStitchNoOtherArcsSameDir(cur);

		cur = routMimicInteractive.isSelected();
		if (cur != initRoutMimicInteractive)
			Routing.setMimicStitchInteractive(cur);
	}

	//******************************** COMPACTION ********************************

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Compaction tab.
	 */
	private void initCompaction()
	{
		compAllowSpreading.setEnabled(false);
		compVerbose.setEnabled(false);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Compaction tab.
	 */
	private void termCompaction()
	{
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        spiceHeader = new javax.swing.ButtonGroup();
        spiceTrailer = new javax.swing.ButtonGroup();
        spiceModel = new javax.swing.ButtonGroup();
        netDefaultOrder = new javax.swing.ButtonGroup();
        verilogModel = new javax.swing.ButtonGroup();
        drLayerOrNode = new javax.swing.ButtonGroup();
        routStitcher = new javax.swing.ButtonGroup();
        wellCheckPWell = new javax.swing.ButtonGroup();
        wellCheckNWell = new javax.swing.ButtonGroup();
        tabPane = new javax.swing.JTabbedPane();
        drc = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        drcIncrementalOn = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        drcOneErrorPerCell = new javax.swing.JCheckBox();
        drcClearValidDates = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        drcUseMultipleThreads = new javax.swing.JCheckBox();
        jLabel33 = new javax.swing.JLabel();
        drcNumberOfThreads = new javax.swing.JTextField();
        drcIgnoreCenterCuts = new javax.swing.JCheckBox();
        jPanel6 = new javax.swing.JPanel();
        drcEditRulesDeck = new javax.swing.JButton();
        designRules = new javax.swing.JPanel();
        jLabel35 = new javax.swing.JLabel();
        drTechName = new javax.swing.JLabel();
        drLayers = new javax.swing.JRadioButton();
        drNodes = new javax.swing.JRadioButton();
        drFromList = new javax.swing.JScrollPane();
        jLabel37 = new javax.swing.JLabel();
        drShowOnlyLinesWithRules = new javax.swing.JCheckBox();
        drToList = new javax.swing.JScrollPane();
        jLabel38 = new javax.swing.JLabel();
        jLabel39 = new javax.swing.JLabel();
        drMinWidth = new javax.swing.JTextField();
        drMinWidthRule = new javax.swing.JTextField();
        drMinHeight = new javax.swing.JTextField();
        jLabel40 = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        jLabel42 = new javax.swing.JLabel();
        jLabel43 = new javax.swing.JLabel();
        jLabel44 = new javax.swing.JLabel();
        jLabel45 = new javax.swing.JLabel();
        jLabel46 = new javax.swing.JLabel();
        jLabel47 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        jLabel50 = new javax.swing.JLabel();
        jLabel51 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        jLabel53 = new javax.swing.JLabel();
        drNormalConnected = new javax.swing.JTextField();
        drNormalConnectedRule = new javax.swing.JTextField();
        drNormalUnconnected = new javax.swing.JTextField();
        drNormalUnconnectedRule = new javax.swing.JTextField();
        drNormalEdge = new javax.swing.JTextField();
        drNormalEdgeRule = new javax.swing.JTextField();
        drWideLimit = new javax.swing.JTextField();
        drWideConnected = new javax.swing.JTextField();
        drWideConnectedRule = new javax.swing.JTextField();
        drWideUnconnected = new javax.swing.JTextField();
        drWideUnconnectedRule = new javax.swing.JTextField();
        drMultiConnected = new javax.swing.JTextField();
        drMultiConnectedRule = new javax.swing.JTextField();
        drMultiUnconnected = new javax.swing.JTextField();
        drMultiUnconnectedRule = new javax.swing.JTextField();
        spice = new javax.swing.JPanel();
        spice1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        spiceRunPopup = new javax.swing.JComboBox();
        spiceEnginePopup = new javax.swing.JComboBox();
        spiceLevelPopup = new javax.swing.JComboBox();
        spiceOutputFormatPopup = new javax.swing.JComboBox();
        spiceUseParasitics = new javax.swing.JCheckBox();
        spiceUseNodeNames = new javax.swing.JCheckBox();
        spiceForceGlobalPwrGnd = new javax.swing.JCheckBox();
        spiceUseCellParameters = new javax.swing.JCheckBox();
        spiceWriteTransSizesInLambda = new javax.swing.JCheckBox();
        spice2 = new javax.swing.JPanel();
        jLabel21 = new javax.swing.JLabel();
        spiceRunParameters = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        spice3 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        spicePrimitivesetPopup = new javax.swing.JComboBox();
        jSeparator1 = new javax.swing.JSeparator();
        spice4 = new javax.swing.JPanel();
        spiceLayer = new javax.swing.JScrollPane();
        jLabel7 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        spiceTechnology = new javax.swing.JLabel();
        spiceResistance = new javax.swing.JTextField();
        spiceCapacitance = new javax.swing.JTextField();
        spiceEdgeCapacitance = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        spiceMinResistance = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        spiceMinCapacitance = new javax.swing.JTextField();
        jSeparator2 = new javax.swing.JSeparator();
        spice5 = new javax.swing.JPanel();
        spiceHeaderCardExtension = new javax.swing.JTextField();
        spiceNoHeaderCards = new javax.swing.JRadioButton();
        spiceHeaderCardsWithExtension = new javax.swing.JRadioButton();
        spiceHeaderCardsFromFile = new javax.swing.JRadioButton();
        spiceNoTrailerCards = new javax.swing.JRadioButton();
        spiceTrailerCardsWithExtension = new javax.swing.JRadioButton();
        spiceTrailerCardsFromFile = new javax.swing.JRadioButton();
        spiceHeaderCardFile = new javax.swing.JTextField();
        spiceBrowseHeaderFile = new javax.swing.JButton();
        spiceTrailerCardExtension = new javax.swing.JTextField();
        spiceTrailerCardFile = new javax.swing.JTextField();
        spiceBrowseTrailerFile = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        spice6 = new javax.swing.JPanel();
        spiceCell = new javax.swing.JScrollPane();
        jLabel8 = new javax.swing.JLabel();
        spiceDeriveModelFromCircuit = new javax.swing.JRadioButton();
        spiceUseModelFromFile = new javax.swing.JRadioButton();
        spiceModelFileBrowse = new javax.swing.JButton();
        spiceModelCell = new javax.swing.JTextField();
        verilog = new javax.swing.JPanel();
        jLabel54 = new javax.swing.JLabel();
        verLibrary = new javax.swing.JComboBox();
        verCells = new javax.swing.JScrollPane();
        verUseAssign = new javax.swing.JCheckBox();
        verDefWireTrireg = new javax.swing.JCheckBox();
        verDeriveModel = new javax.swing.JRadioButton();
        verUseModelFile = new javax.swing.JRadioButton();
        verBrowse = new javax.swing.JButton();
        verFileName = new javax.swing.JTextField();
        fastHenry = new javax.swing.JPanel();
        fhUseSingleFrequency = new javax.swing.JCheckBox();
        jLabel55 = new javax.swing.JLabel();
        jLabel56 = new javax.swing.JLabel();
        jLabel57 = new javax.swing.JLabel();
        fhMakeMultipole = new javax.swing.JCheckBox();
        jLabel58 = new javax.swing.JLabel();
        fhFrequencyStart = new javax.swing.JTextField();
        fhFrequencyEnd = new javax.swing.JTextField();
        fhRunsPerDecade = new javax.swing.JTextField();
        fhNumberOfPoles = new javax.swing.JTextField();
        jLabel59 = new javax.swing.JLabel();
        jLabel60 = new javax.swing.JLabel();
        jLabel61 = new javax.swing.JLabel();
        jLabel62 = new javax.swing.JLabel();
        fhDefaultThickness = new javax.swing.JTextField();
        fhDefaultWidthSubs = new javax.swing.JTextField();
        fhDefaultHeightSubs = new javax.swing.JTextField();
        fhMaxSegmentLength = new javax.swing.JTextField();
        fhMakePostScript = new javax.swing.JCheckBox();
        fhMakeSpice = new javax.swing.JCheckBox();
        jLabel63 = new javax.swing.JLabel();
        fhAfterAction = new javax.swing.JComboBox();
        jLabel30 = new javax.swing.JLabel();
        wellCheck = new javax.swing.JPanel();
        jLabel64 = new javax.swing.JLabel();
        jLabel65 = new javax.swing.JLabel();
        wellPMustHaveAllContacts = new javax.swing.JRadioButton();
        wellPMustHave1Contact = new javax.swing.JRadioButton();
        wellPNoContactCheck = new javax.swing.JRadioButton();
        wellNMustHaveAllContacts = new javax.swing.JRadioButton();
        wellNMustHave1Contact = new javax.swing.JRadioButton();
        wellNNoContactCheck = new javax.swing.JRadioButton();
        wellPMustConnectGround = new javax.swing.JCheckBox();
        wellNMustConnectPower = new javax.swing.JCheckBox();
        wellFindFarthestDistance = new javax.swing.JCheckBox();
        antennaRules = new javax.swing.JPanel();
        jLabel66 = new javax.swing.JLabel();
        antTechnology = new javax.swing.JLabel();
        antArcList = new javax.swing.JScrollPane();
        jLabel68 = new javax.swing.JLabel();
        antMaxRatio = new javax.swing.JTextField();
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
        ncc = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel71 = new javax.swing.JLabel();
        jLabel72 = new javax.swing.JLabel();
        nccFirstCell = new javax.swing.JTextField();
        nccSecondCell = new javax.swing.JTextField();
        nccSetFirstCell = new javax.swing.JButton();
        nccSetSecondCell = new javax.swing.JButton();
        nccNextFirstCell = new javax.swing.JButton();
        nccNextSecondCell = new javax.swing.JButton();
        jLabel73 = new javax.swing.JLabel();
        jSeparator10 = new javax.swing.JSeparator();
        jPanel2 = new javax.swing.JPanel();
        jLabel74 = new javax.swing.JLabel();
        nccExpandHierarchy = new javax.swing.JCheckBox();
        nccMergeParallel = new javax.swing.JCheckBox();
        nccMergeSeries = new javax.swing.JCheckBox();
        nccClearDatesThisLibrary = new javax.swing.JButton();
        nccClearDatesAllLibraries = new javax.swing.JButton();
        nccIgnorePwrGnd = new javax.swing.JCheckBox();
        nccCheckExportNames = new javax.swing.JCheckBox();
        nccCheckComponentSizes = new javax.swing.JCheckBox();
        jLabel75 = new javax.swing.JLabel();
        jLabel76 = new javax.swing.JLabel();
        nccSizeTolerancePct = new javax.swing.JTextField();
        nccSizeToleranceAmt = new javax.swing.JTextField();
        nccAllowNoCompNets = new javax.swing.JCheckBox();
        nccRecurse = new javax.swing.JCheckBox();
        nccAutomaticResistorExclusion = new javax.swing.JCheckBox();
        nccDebuggingOptions = new javax.swing.JButton();
        nccShowMatchTags = new javax.swing.JCheckBox();
        jSeparator9 = new javax.swing.JSeparator();
        jLabel77 = new javax.swing.JLabel();
        nccExpandHierarchyYes = new javax.swing.JRadioButton();
        nccExpandHierarchyNo = new javax.swing.JRadioButton();
        nccExpandHierarchyDefault = new javax.swing.JRadioButton();
        nccMergeParallelYes = new javax.swing.JRadioButton();
        nccMergeParallelNo = new javax.swing.JRadioButton();
        nccMergeParallelDefault = new javax.swing.JRadioButton();
        nccMergeSeriesYes = new javax.swing.JRadioButton();
        nccMergeSeriesNo = new javax.swing.JRadioButton();
        nccMergeSeriesDefault = new javax.swing.JRadioButton();
        nccLibraryPopup = new javax.swing.JComboBox();
        nccCellList = new javax.swing.JScrollPane();
        nccListForcedMatches = new javax.swing.JButton();
        nccRemoveForcedMatches = new javax.swing.JButton();
        nccListOverrides = new javax.swing.JButton();
        nccRemoveOverrides = new javax.swing.JButton();
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
        jLabel6 = new javax.swing.JLabel();
        leGlobalFanOut = new javax.swing.JTextField();
        leConvergence = new javax.swing.JTextField();
        leMaxIterations = new javax.swing.JTextField();
        leGateCapacitance = new javax.swing.JTextField();
        leDefaultWireCapRatio = new javax.swing.JTextField();
        leDiffToGateCapRatio = new javax.swing.JTextField();
        leKeeperSizeRatio = new javax.swing.JTextField();
        leWireRatio = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        routing = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jLabel70 = new javax.swing.JLabel();
        routMimicPortsMustMatch = new javax.swing.JCheckBox();
        routMimicInteractive = new javax.swing.JCheckBox();
        routMimicCanUnstitch = new javax.swing.JCheckBox();
        routMimicNumArcsMustMatch = new javax.swing.JCheckBox();
        routMimicNodeSizesMustMatch = new javax.swing.JCheckBox();
        routMimicNodeTypesMustMatch = new javax.swing.JCheckBox();
        routMimicNoOtherArcs = new javax.swing.JCheckBox();
        jPanel8 = new javax.swing.JPanel();
        jLabel67 = new javax.swing.JLabel();
        jLabel69 = new javax.swing.JLabel();
        routDefaultArc = new javax.swing.JComboBox();
        routNoStitcher = new javax.swing.JRadioButton();
        routAutoStitcher = new javax.swing.JRadioButton();
        routMimicStitcher = new javax.swing.JRadioButton();
        compaction = new javax.swing.JPanel();
        compAllowSpreading = new javax.swing.JCheckBox();
        compVerbose = new javax.swing.JCheckBox();
        jLabel78 = new javax.swing.JLabel();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        factoryReset = new javax.swing.JButton();

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

        tabPane.setToolTipText("");
        drc.setLayout(new java.awt.GridBagLayout());

        jPanel3.setLayout(new java.awt.GridBagLayout());

        jPanel3.setBorder(new javax.swing.border.TitledBorder("Incremental DRC"));
        drcIncrementalOn.setText("On");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        jPanel3.add(drcIncrementalOn, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drc.add(jPanel3, gridBagConstraints);

        jPanel4.setLayout(new java.awt.GridBagLayout());

        jPanel4.setBorder(new javax.swing.border.TitledBorder("Hierarchical DRC"));
        drcOneErrorPerCell.setText("Just 1 error per cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        jPanel4.add(drcOneErrorPerCell, gridBagConstraints);

        drcClearValidDates.setText("Clear valid DRC dates");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        jPanel4.add(drcClearValidDates, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drc.add(jPanel4, gridBagConstraints);

        jPanel5.setLayout(new java.awt.GridBagLayout());

        jPanel5.setBorder(new javax.swing.border.TitledBorder("Incremental and Hierarchical"));
        drcUseMultipleThreads.setText("Use multiple threads");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        jPanel5.add(drcUseMultipleThreads, gridBagConstraints);

        jLabel33.setText("Number of threads:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        jPanel5.add(jLabel33, gridBagConstraints);

        drcNumberOfThreads.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        jPanel5.add(drcNumberOfThreads, gridBagConstraints);

        drcIgnoreCenterCuts.setText("Ignore center cuts in large contacts");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        jPanel5.add(drcIgnoreCenterCuts, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drc.add(jPanel5, gridBagConstraints);

        jPanel6.setLayout(new java.awt.GridBagLayout());

        jPanel6.setBorder(new javax.swing.border.TitledBorder("Dracula DRC Interface"));
        drcEditRulesDeck.setText("Edit Rules Deck");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        jPanel6.add(drcEditRulesDeck, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drc.add(jPanel6, gridBagConstraints);

        tabPane.addTab("DRC", drc);

        designRules.setLayout(new java.awt.GridBagLayout());

        jLabel35.setText("Technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(jLabel35, gridBagConstraints);

        drTechName.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(drTechName, gridBagConstraints);

        drLayers.setText("Layers:");
        drLayerOrNode.add(drLayers);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(drLayers, gridBagConstraints);

        drNodes.setText("Nodes:");
        drLayerOrNode.add(drNodes);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(drNodes, gridBagConstraints);

        drFromList.setPreferredSize(new java.awt.Dimension(75, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.5;
        designRules.add(drFromList, gridBagConstraints);

        jLabel37.setText("To Layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        designRules.add(jLabel37, gridBagConstraints);

        drShowOnlyLinesWithRules.setText("Show only lines with rules");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(drShowOnlyLinesWithRules, gridBagConstraints);

        drToList.setPreferredSize(new java.awt.Dimension(150, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        designRules.add(drToList, gridBagConstraints);

        jLabel38.setText("Minimum Width:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        designRules.add(jLabel38, gridBagConstraints);

        jLabel39.setText("Minimum Height:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        designRules.add(jLabel39, gridBagConstraints);

        drMinWidth.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        designRules.add(drMinWidth, gridBagConstraints);

        drMinWidthRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        designRules.add(drMinWidthRule, gridBagConstraints);

        drMinHeight.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        designRules.add(drMinHeight, gridBagConstraints);

        jLabel40.setText("Size");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(jLabel40, gridBagConstraints);

        jLabel41.setText("Rule");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(jLabel41, gridBagConstraints);

        jLabel42.setText("Normal:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        designRules.add(jLabel42, gridBagConstraints);

        jLabel43.setText("Distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(jLabel43, gridBagConstraints);

        jLabel44.setText("Rule");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(jLabel44, gridBagConstraints);

        jLabel45.setText("When connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 0);
        designRules.add(jLabel45, gridBagConstraints);

        jLabel46.setText("Not connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 0);
        designRules.add(jLabel46, gridBagConstraints);

        jLabel47.setText("Edge:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 0);
        designRules.add(jLabel47, gridBagConstraints);

        jLabel48.setText("Wide (when bigger than this):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        designRules.add(jLabel48, gridBagConstraints);

        jLabel49.setText("When connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 0);
        designRules.add(jLabel49, gridBagConstraints);

        jLabel50.setText("Not connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 0);
        designRules.add(jLabel50, gridBagConstraints);

        jLabel51.setText("Multiple cuts:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        designRules.add(jLabel51, gridBagConstraints);

        jLabel52.setText("When connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 0);
        designRules.add(jLabel52, gridBagConstraints);

        jLabel53.setText("Not connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 0);
        designRules.add(jLabel53, gridBagConstraints);

        drNormalConnected.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(drNormalConnected, gridBagConstraints);

        drNormalConnectedRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(drNormalConnectedRule, gridBagConstraints);

        drNormalUnconnected.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(drNormalUnconnected, gridBagConstraints);

        drNormalUnconnectedRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(drNormalUnconnectedRule, gridBagConstraints);

        drNormalEdge.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(drNormalEdge, gridBagConstraints);

        drNormalEdgeRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(drNormalEdgeRule, gridBagConstraints);

        drWideLimit.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(drWideLimit, gridBagConstraints);

        drWideConnected.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(drWideConnected, gridBagConstraints);

        drWideConnectedRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(drWideConnectedRule, gridBagConstraints);

        drWideUnconnected.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(drWideUnconnected, gridBagConstraints);

        drWideUnconnectedRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(drWideUnconnectedRule, gridBagConstraints);

        drMultiConnected.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(drMultiConnected, gridBagConstraints);

        drMultiConnectedRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(drMultiConnectedRule, gridBagConstraints);

        drMultiUnconnected.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(drMultiUnconnected, gridBagConstraints);

        drMultiUnconnectedRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        designRules.add(drMultiUnconnectedRule, gridBagConstraints);

        tabPane.addTab("Design Rules", designRules);

        spice.setLayout(new java.awt.GridBagLayout());

        spice.setToolTipText("Options for Spice deck generation");
        spice1.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("SPICE Engine:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        spice1.add(jLabel1, gridBagConstraints);

        jLabel9.setText("SPICE Level:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(jLabel9, gridBagConstraints);

        jLabel10.setText("Output format:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(jLabel10, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        spice1.add(spiceRunPopup, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice1.add(spiceEnginePopup, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice1.add(spiceLevelPopup, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice1.add(spiceOutputFormatPopup, gridBagConstraints);

        spiceUseParasitics.setText("Use Parasitics");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceUseParasitics, gridBagConstraints);

        spiceUseNodeNames.setText("Use Node Names");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceUseNodeNames, gridBagConstraints);

        spiceForceGlobalPwrGnd.setText("Force Global VDD/GND");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceForceGlobalPwrGnd, gridBagConstraints);

        spiceUseCellParameters.setText("Use Cell Parameters");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceUseCellParameters, gridBagConstraints);

        spiceWriteTransSizesInLambda.setText("Write Trans Sizes in Lambda");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceWriteTransSizesInLambda, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        spice.add(spice1, gridBagConstraints);

        spice2.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        spice2.add(jLabel21, gridBagConstraints);

        spiceRunParameters.setMinimumSize(new java.awt.Dimension(100, 20));
        spiceRunParameters.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        spice2.add(spiceRunParameters, gridBagConstraints);

        jLabel16.setText("Run:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        spice2.add(jLabel16, gridBagConstraints);

        jLabel17.setText("With");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice2.add(jLabel17, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        spice.add(spice2, gridBagConstraints);

        spice3.setLayout(new java.awt.GridBagLayout());

        jLabel13.setText("SPICE primitive set:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        spice3.add(jLabel13, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice3.add(spicePrimitivesetPopup, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        spice.add(spice3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        spice.add(jSeparator1, gridBagConstraints);

        spice4.setLayout(new java.awt.GridBagLayout());

        spiceLayer.setMinimumSize(new java.awt.Dimension(200, 50));
        spiceLayer.setPreferredSize(new java.awt.Dimension(200, 50));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice4.add(spiceLayer, gridBagConstraints);

        jLabel7.setText("Layer:");
        jLabel7.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        spice4.add(jLabel7, gridBagConstraints);

        jLabel2.setText("Edge Cap:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        spice4.add(jLabel2, gridBagConstraints);

        jLabel11.setText("Resistance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        spice4.add(jLabel11, gridBagConstraints);

        jLabel12.setText("Capacitance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        spice4.add(jLabel12, gridBagConstraints);

        spiceTechnology.setText("Technology: xxx");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        spice4.add(spiceTechnology, gridBagConstraints);

        spiceResistance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(spiceResistance, gridBagConstraints);

        spiceCapacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(spiceCapacitance, gridBagConstraints);

        spiceEdgeCapacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(spiceEdgeCapacitance, gridBagConstraints);

        jLabel18.setText("Min. Resistance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(jLabel18, gridBagConstraints);

        spiceMinResistance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(spiceMinResistance, gridBagConstraints);

        jLabel19.setText("Min. Capacitance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        spice4.add(jLabel19, gridBagConstraints);

        spiceMinCapacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(spiceMinCapacitance, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        spice.add(spice4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        spice.add(jSeparator2, gridBagConstraints);

        spice5.setLayout(new java.awt.GridBagLayout());

        spiceHeaderCardExtension.setColumns(5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        spice5.add(spiceHeaderCardExtension, gridBagConstraints);

        spiceNoHeaderCards.setText("No Header Cards");
        spiceHeader.add(spiceNoHeaderCards);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceNoHeaderCards, gridBagConstraints);

        spiceHeaderCardsWithExtension.setText("Use Header Cards with extension:");
        spiceHeader.add(spiceHeaderCardsWithExtension);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceHeaderCardsWithExtension, gridBagConstraints);

        spiceHeaderCardsFromFile.setText("Use Header Cards from File:");
        spiceHeader.add(spiceHeaderCardsFromFile);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceHeaderCardsFromFile, gridBagConstraints);

        spiceNoTrailerCards.setText("No Trailer Cards");
        spiceTrailer.add(spiceNoTrailerCards);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceNoTrailerCards, gridBagConstraints);

        spiceTrailerCardsWithExtension.setText("Use Trailer Cards with extension:");
        spiceTrailer.add(spiceTrailerCardsWithExtension);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceTrailerCardsWithExtension, gridBagConstraints);

        spiceTrailerCardsFromFile.setText("Use Trailer Cards from File:");
        spiceTrailer.add(spiceTrailerCardsFromFile);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceTrailerCardsFromFile, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        spice5.add(spiceHeaderCardFile, gridBagConstraints);

        spiceBrowseHeaderFile.setText("Browse");
        spiceBrowseHeaderFile.setMinimumSize(new java.awt.Dimension(78, 20));
        spiceBrowseHeaderFile.setPreferredSize(new java.awt.Dimension(78, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice5.add(spiceBrowseHeaderFile, gridBagConstraints);

        spiceTrailerCardExtension.setColumns(5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        spice5.add(spiceTrailerCardExtension, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        spice5.add(spiceTrailerCardFile, gridBagConstraints);

        spiceBrowseTrailerFile.setText("Browse");
        spiceBrowseTrailerFile.setMinimumSize(new java.awt.Dimension(78, 20));
        spiceBrowseTrailerFile.setPreferredSize(new java.awt.Dimension(78, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice5.add(spiceBrowseTrailerFile, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice5.add(jSeparator4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        spice.add(spice5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        spice.add(jSeparator3, gridBagConstraints);

        spice6.setLayout(new java.awt.GridBagLayout());

        spiceCell.setMinimumSize(new java.awt.Dimension(200, 100));
        spiceCell.setPreferredSize(new java.awt.Dimension(200, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        spice6.add(spiceCell, gridBagConstraints);

        jLabel8.setText("For Cell");
        jLabel8.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        spice6.add(jLabel8, gridBagConstraints);

        spiceDeriveModelFromCircuit.setText("Derive Model from Circuitry");
        spiceModel.add(spiceDeriveModelFromCircuit);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice6.add(spiceDeriveModelFromCircuit, gridBagConstraints);

        spiceUseModelFromFile.setText("Use Model from File:");
        spiceModel.add(spiceUseModelFromFile);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice6.add(spiceUseModelFromFile, gridBagConstraints);

        spiceModelFileBrowse.setText("Browse");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice6.add(spiceModelFileBrowse, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        spice6.add(spiceModelCell, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        spice.add(spice6, gridBagConstraints);

        tabPane.addTab("Spice", spice);

        verilog.setLayout(new java.awt.GridBagLayout());

        jLabel54.setText("Library:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        verilog.add(jLabel54, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verLibrary, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        verilog.add(verCells, gridBagConstraints);

        verUseAssign.setText("Use ASSIGN Construct");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verUseAssign, gridBagConstraints);

        verDefWireTrireg.setText("Default wire is Trireg");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verDefWireTrireg, gridBagConstraints);

        verDeriveModel.setText("Derive Model from Circuitry");
        verilogModel.add(verDeriveModel);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(20, 4, 4, 4);
        verilog.add(verDeriveModel, gridBagConstraints);

        verUseModelFile.setText("Use Model from File:");
        verilogModel.add(verUseModelFile);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verUseModelFile, gridBagConstraints);

        verBrowse.setText("Browse");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verBrowse, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verFileName, gridBagConstraints);

        tabPane.addTab("Verilog", verilog);

        fastHenry.setLayout(new java.awt.GridBagLayout());

        fhUseSingleFrequency.setText("Use single frequency");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhUseSingleFrequency, gridBagConstraints);

        jLabel55.setText("Frequency start:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        fastHenry.add(jLabel55, gridBagConstraints);

        jLabel56.setText("Frequency end:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        fastHenry.add(jLabel56, gridBagConstraints);

        jLabel57.setText("Runs per decade:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        fastHenry.add(jLabel57, gridBagConstraints);

        fhMakeMultipole.setText("Make multipole subcircuit");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhMakeMultipole, gridBagConstraints);

        jLabel58.setText("Number of poles:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        fastHenry.add(jLabel58, gridBagConstraints);

        fhFrequencyStart.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhFrequencyStart, gridBagConstraints);

        fhFrequencyEnd.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhFrequencyEnd, gridBagConstraints);

        fhRunsPerDecade.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhRunsPerDecade, gridBagConstraints);

        fhNumberOfPoles.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhNumberOfPoles, gridBagConstraints);

        jLabel59.setText("Default thickness:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(jLabel59, gridBagConstraints);

        jLabel60.setText("Default width subdivisions:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(jLabel60, gridBagConstraints);

        jLabel61.setText("Default height subdivisions:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(jLabel61, gridBagConstraints);

        jLabel62.setText("Maximum segment length:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(jLabel62, gridBagConstraints);

        fhDefaultThickness.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhDefaultThickness, gridBagConstraints);

        fhDefaultWidthSubs.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhDefaultWidthSubs, gridBagConstraints);

        fhDefaultHeightSubs.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhDefaultHeightSubs, gridBagConstraints);

        fhMaxSegmentLength.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhMaxSegmentLength, gridBagConstraints);

        fhMakePostScript.setText("Make PostScript view");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhMakePostScript, gridBagConstraints);

        fhMakeSpice.setText("Make SPICE subcircuit");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhMakeSpice, gridBagConstraints);

        jLabel63.setText("After writing deck:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(jLabel63, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhAfterAction, gridBagConstraints);

        jLabel30.setText("FASTHENRY OUTPUT IS NOT YET SUPPORTED");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(jLabel30, gridBagConstraints);

        tabPane.addTab("Fast Henry", fastHenry);

        wellCheck.setLayout(new java.awt.GridBagLayout());

        jLabel64.setText("For P-Well:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        wellCheck.add(jLabel64, gridBagConstraints);

        jLabel65.setText("For N-Well:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        wellCheck.add(jLabel65, gridBagConstraints);

        wellPMustHaveAllContacts.setText("Must have contact in every area");
        wellCheckPWell.add(wellPMustHaveAllContacts);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellPMustHaveAllContacts, gridBagConstraints);

        wellPMustHave1Contact.setText("Must have at least 1 contact");
        wellCheckPWell.add(wellPMustHave1Contact);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellPMustHave1Contact, gridBagConstraints);

        wellPNoContactCheck.setText("Do not check for contacts");
        wellCheckPWell.add(wellPNoContactCheck);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellPNoContactCheck, gridBagConstraints);

        wellNMustHaveAllContacts.setText("Must have contact in every area");
        wellCheckNWell.add(wellNMustHaveAllContacts);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellNMustHaveAllContacts, gridBagConstraints);

        wellNMustHave1Contact.setText("Must have at least 1 contact");
        wellCheckNWell.add(wellNMustHave1Contact);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellNMustHave1Contact, gridBagConstraints);

        wellNNoContactCheck.setText("Do not check for contacts");
        wellCheckNWell.add(wellNNoContactCheck);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellNNoContactCheck, gridBagConstraints);

        wellPMustConnectGround.setText("Must connect to Ground");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellPMustConnectGround, gridBagConstraints);

        wellNMustConnectPower.setText("Must connect to Power");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellNMustConnectPower, gridBagConstraints);

        wellFindFarthestDistance.setText("Find farthest distance from contact to edge");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellFindFarthestDistance, gridBagConstraints);

        tabPane.addTab("Well Check", wellCheck);

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

        tabPane.addTab("Antenna Rules", antennaRules);

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

        tabPane.addTab("Network", network);

        ncc.setLayout(new java.awt.BorderLayout());

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel71.setText("Compare cell:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(jLabel71, gridBagConstraints);

        jLabel72.setText("With cell:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(jLabel72, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(nccFirstCell, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(nccSecondCell, gridBagConstraints);

        nccSetFirstCell.setText("Set");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        jPanel1.add(nccSetFirstCell, gridBagConstraints);

        nccSetSecondCell.setText("Set");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        jPanel1.add(nccSetSecondCell, gridBagConstraints);

        nccNextFirstCell.setText("Next");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        jPanel1.add(nccNextFirstCell, gridBagConstraints);

        nccNextSecondCell.setText("Next");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        jPanel1.add(nccNextSecondCell, gridBagConstraints);

        jLabel73.setText("!!! WARNING: These cells are not on the screen !!!");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        jPanel1.add(jLabel73, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(jSeparator10, gridBagConstraints);

        ncc.add(jPanel1, java.awt.BorderLayout.NORTH);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jLabel74.setText("For all cells:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        jPanel2.add(jLabel74, gridBagConstraints);

        nccExpandHierarchy.setText("Expand hierarchy");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel2.add(nccExpandHierarchy, gridBagConstraints);

        nccMergeParallel.setText("Merge parallel components");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(nccMergeParallel, gridBagConstraints);

        nccMergeSeries.setText("Merge series transistors");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel2.add(nccMergeSeries, gridBagConstraints);

        nccClearDatesThisLibrary.setText("Clear NCC dates this library");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        jPanel2.add(nccClearDatesThisLibrary, gridBagConstraints);

        nccClearDatesAllLibraries.setText("Clear NCC dates all libraries");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        jPanel2.add(nccClearDatesAllLibraries, gridBagConstraints);

        nccIgnorePwrGnd.setText("Ignore power and ground");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel2.add(nccIgnorePwrGnd, gridBagConstraints);

        nccCheckExportNames.setText("Check export names");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel2.add(nccCheckExportNames, gridBagConstraints);

        nccCheckComponentSizes.setText("Check component sizes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel2.add(nccCheckComponentSizes, gridBagConstraints);

        jLabel75.setText("Size tolerance (%):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel2.add(jLabel75, gridBagConstraints);

        jLabel76.setText("Size tolerance (amt):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel2.add(jLabel76, gridBagConstraints);

        nccSizeTolerancePct.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        jPanel2.add(nccSizeTolerancePct, gridBagConstraints);

        nccSizeToleranceAmt.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        jPanel2.add(nccSizeToleranceAmt, gridBagConstraints);

        nccAllowNoCompNets.setText("Allow no-component nets");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel2.add(nccAllowNoCompNets, gridBagConstraints);

        nccRecurse.setText("Recurse through hierarchy");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel2.add(nccRecurse, gridBagConstraints);

        nccAutomaticResistorExclusion.setText("Automatic Resistor Exclusion");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel2.add(nccAutomaticResistorExclusion, gridBagConstraints);

        nccDebuggingOptions.setText("NCC Debugging options...");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 2;
        jPanel2.add(nccDebuggingOptions, gridBagConstraints);

        nccShowMatchTags.setText("Show 'NCCMatch' tags");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(nccShowMatchTags, gridBagConstraints);

        jSeparator9.setOrientation(javax.swing.SwingConstants.VERTICAL);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 18;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        jPanel2.add(jSeparator9, gridBagConstraints);

        jLabel77.setText("Individual cell overrides:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        jPanel2.add(jLabel77, gridBagConstraints);

        nccExpandHierarchyYes.setText("Yes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(nccExpandHierarchyYes, gridBagConstraints);

        nccExpandHierarchyNo.setText("No");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(nccExpandHierarchyNo, gridBagConstraints);

        nccExpandHierarchyDefault.setText("Default");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(nccExpandHierarchyDefault, gridBagConstraints);

        nccMergeParallelYes.setText("Yes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(nccMergeParallelYes, gridBagConstraints);

        nccMergeParallelNo.setText("No");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(nccMergeParallelNo, gridBagConstraints);

        nccMergeParallelDefault.setText("Default");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(nccMergeParallelDefault, gridBagConstraints);

        nccMergeSeriesYes.setText("Yes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(nccMergeSeriesYes, gridBagConstraints);

        nccMergeSeriesNo.setText("No");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(nccMergeSeriesNo, gridBagConstraints);

        nccMergeSeriesDefault.setText("Default");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(nccMergeSeriesDefault, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel2.add(nccLibraryPopup, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel2.add(nccCellList, gridBagConstraints);

        nccListForcedMatches.setText("List all forced matches");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 3;
        jPanel2.add(nccListForcedMatches, gridBagConstraints);

        nccRemoveForcedMatches.setText("Remove all forced matches");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.gridwidth = 3;
        jPanel2.add(nccRemoveForcedMatches, gridBagConstraints);

        nccListOverrides.setText("List all overrides");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 3;
        jPanel2.add(nccListOverrides, gridBagConstraints);

        nccRemoveOverrides.setText("Remove all overrides");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 3;
        jPanel2.add(nccRemoveOverrides, gridBagConstraints);

        ncc.add(jPanel2, java.awt.BorderLayout.CENTER);

        tabPane.addTab("NCC", ncc);

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

        jLabel6.setText("Wire ratio:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        logicalEffort.add(jLabel6, gridBagConstraints);

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

        tabPane.addTab("Logical Effort", logicalEffort);

        routing.setLayout(new java.awt.GridBagLayout());

        jPanel7.setLayout(new java.awt.GridBagLayout());

        jPanel7.setBorder(new javax.swing.border.TitledBorder("Mimic Stitcher"));
        jLabel70.setText("Mimic stitching restrictions (non-interactive):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel7.add(jLabel70, gridBagConstraints);

        routMimicPortsMustMatch.setText("Ports must match");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        jPanel7.add(routMimicPortsMustMatch, gridBagConstraints);

        routMimicInteractive.setText("Mimic stitching runs interactively");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel7.add(routMimicInteractive, gridBagConstraints);

        routMimicCanUnstitch.setText("Mimic stitching can unstitch");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel7.add(routMimicCanUnstitch, gridBagConstraints);

        routMimicNumArcsMustMatch.setText("Number of existing arcs must match");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        jPanel7.add(routMimicNumArcsMustMatch, gridBagConstraints);

        routMimicNodeSizesMustMatch.setText("Node sizes must match");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        jPanel7.add(routMimicNodeSizesMustMatch, gridBagConstraints);

        routMimicNodeTypesMustMatch.setText("Node types must match");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        jPanel7.add(routMimicNodeTypesMustMatch, gridBagConstraints);

        routMimicNoOtherArcs.setText("Cannot have other arcs in the same direction");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        jPanel7.add(routMimicNoOtherArcs, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        routing.add(jPanel7, gridBagConstraints);

        jPanel8.setLayout(new java.awt.GridBagLayout());

        jPanel8.setBorder(new javax.swing.border.TitledBorder("All Routers"));
        jLabel67.setText("Arc to use in stitching routers:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(jLabel67, gridBagConstraints);

        jLabel69.setText("Currently:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(jLabel69, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(routDefaultArc, gridBagConstraints);

        routNoStitcher.setText("No stitcher running");
        routStitcher.add(routNoStitcher);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel8.add(routNoStitcher, gridBagConstraints);

        routAutoStitcher.setText("Auto-stitcher running");
        routStitcher.add(routAutoStitcher);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel8.add(routAutoStitcher, gridBagConstraints);

        routMimicStitcher.setText("Mimic-stitcher running");
        routStitcher.add(routMimicStitcher);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel8.add(routMimicStitcher, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        routing.add(jPanel8, gridBagConstraints);

        tabPane.addTab("Routing", routing);

        compaction.setLayout(new java.awt.GridBagLayout());

        compAllowSpreading.setText("Allow spreading");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        compaction.add(compAllowSpreading, gridBagConstraints);

        compVerbose.setText("Verbose");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        compaction.add(compVerbose, gridBagConstraints);

        jLabel78.setText("COMPACTION IS NOT YET SUPPORTED");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        compaction.add(jLabel78, gridBagConstraints);

        tabPane.addTab("Compaction", compaction);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(tabPane, gridBagConstraints);

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                CancelButton(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 40, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                OKButton(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 40);
        getContentPane().add(ok, gridBagConstraints);

        factoryReset.setText("Factory Reset");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(factoryReset, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void CancelButton(java.awt.event.ActionEvent evt)//GEN-FIRST:event_CancelButton
	{//GEN-HEADEREND:event_CancelButton
		closeDialog(null);
	}//GEN-LAST:event_CancelButton

	private void OKButton(java.awt.event.ActionEvent evt)//GEN-FIRST:event_OKButton
	{//GEN-HEADEREND:event_OKButton
		ApplyToolOptions job = new ApplyToolOptions(this);
	}//GEN-LAST:event_OKButton
	
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
    private javax.swing.JButton cancel;
    private javax.swing.JCheckBox compAllowSpreading;
    private javax.swing.JCheckBox compVerbose;
    private javax.swing.JPanel compaction;
    private javax.swing.JPanel designRules;
    private javax.swing.JScrollPane drFromList;
    private javax.swing.ButtonGroup drLayerOrNode;
    private javax.swing.JRadioButton drLayers;
    private javax.swing.JTextField drMinHeight;
    private javax.swing.JTextField drMinWidth;
    private javax.swing.JTextField drMinWidthRule;
    private javax.swing.JTextField drMultiConnected;
    private javax.swing.JTextField drMultiConnectedRule;
    private javax.swing.JTextField drMultiUnconnected;
    private javax.swing.JTextField drMultiUnconnectedRule;
    private javax.swing.JRadioButton drNodes;
    private javax.swing.JTextField drNormalConnected;
    private javax.swing.JTextField drNormalConnectedRule;
    private javax.swing.JTextField drNormalEdge;
    private javax.swing.JTextField drNormalEdgeRule;
    private javax.swing.JTextField drNormalUnconnected;
    private javax.swing.JTextField drNormalUnconnectedRule;
    private javax.swing.JCheckBox drShowOnlyLinesWithRules;
    private javax.swing.JLabel drTechName;
    private javax.swing.JScrollPane drToList;
    private javax.swing.JTextField drWideConnected;
    private javax.swing.JTextField drWideConnectedRule;
    private javax.swing.JTextField drWideLimit;
    private javax.swing.JTextField drWideUnconnected;
    private javax.swing.JTextField drWideUnconnectedRule;
    private javax.swing.JPanel drc;
    private javax.swing.JButton drcClearValidDates;
    private javax.swing.JButton drcEditRulesDeck;
    private javax.swing.JCheckBox drcIgnoreCenterCuts;
    private javax.swing.JCheckBox drcIncrementalOn;
    private javax.swing.JTextField drcNumberOfThreads;
    private javax.swing.JCheckBox drcOneErrorPerCell;
    private javax.swing.JCheckBox drcUseMultipleThreads;
    private javax.swing.JButton factoryReset;
    private javax.swing.JPanel fastHenry;
    private javax.swing.JComboBox fhAfterAction;
    private javax.swing.JTextField fhDefaultHeightSubs;
    private javax.swing.JTextField fhDefaultThickness;
    private javax.swing.JTextField fhDefaultWidthSubs;
    private javax.swing.JTextField fhFrequencyEnd;
    private javax.swing.JTextField fhFrequencyStart;
    private javax.swing.JCheckBox fhMakeMultipole;
    private javax.swing.JCheckBox fhMakePostScript;
    private javax.swing.JCheckBox fhMakeSpice;
    private javax.swing.JTextField fhMaxSegmentLength;
    private javax.swing.JTextField fhNumberOfPoles;
    private javax.swing.JTextField fhRunsPerDecade;
    private javax.swing.JCheckBox fhUseSingleFrequency;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel58;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JLabel jLabel61;
    private javax.swing.JLabel jLabel62;
    private javax.swing.JLabel jLabel63;
    private javax.swing.JLabel jLabel64;
    private javax.swing.JLabel jLabel65;
    private javax.swing.JLabel jLabel66;
    private javax.swing.JLabel jLabel67;
    private javax.swing.JLabel jLabel68;
    private javax.swing.JLabel jLabel69;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel70;
    private javax.swing.JLabel jLabel71;
    private javax.swing.JLabel jLabel72;
    private javax.swing.JLabel jLabel73;
    private javax.swing.JLabel jLabel74;
    private javax.swing.JLabel jLabel75;
    private javax.swing.JLabel jLabel76;
    private javax.swing.JLabel jLabel77;
    private javax.swing.JLabel jLabel78;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator9;
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
    private javax.swing.JPanel ncc;
    private javax.swing.JCheckBox nccAllowNoCompNets;
    private javax.swing.JCheckBox nccAutomaticResistorExclusion;
    private javax.swing.JScrollPane nccCellList;
    private javax.swing.JCheckBox nccCheckComponentSizes;
    private javax.swing.JCheckBox nccCheckExportNames;
    private javax.swing.JButton nccClearDatesAllLibraries;
    private javax.swing.JButton nccClearDatesThisLibrary;
    private javax.swing.JButton nccDebuggingOptions;
    private javax.swing.JCheckBox nccExpandHierarchy;
    private javax.swing.JRadioButton nccExpandHierarchyDefault;
    private javax.swing.JRadioButton nccExpandHierarchyNo;
    private javax.swing.JRadioButton nccExpandHierarchyYes;
    private javax.swing.JTextField nccFirstCell;
    private javax.swing.JCheckBox nccIgnorePwrGnd;
    private javax.swing.JComboBox nccLibraryPopup;
    private javax.swing.JButton nccListForcedMatches;
    private javax.swing.JButton nccListOverrides;
    private javax.swing.JCheckBox nccMergeParallel;
    private javax.swing.JRadioButton nccMergeParallelDefault;
    private javax.swing.JRadioButton nccMergeParallelNo;
    private javax.swing.JRadioButton nccMergeParallelYes;
    private javax.swing.JCheckBox nccMergeSeries;
    private javax.swing.JRadioButton nccMergeSeriesDefault;
    private javax.swing.JRadioButton nccMergeSeriesNo;
    private javax.swing.JRadioButton nccMergeSeriesYes;
    private javax.swing.JButton nccNextFirstCell;
    private javax.swing.JButton nccNextSecondCell;
    private javax.swing.JCheckBox nccRecurse;
    private javax.swing.JButton nccRemoveForcedMatches;
    private javax.swing.JButton nccRemoveOverrides;
    private javax.swing.JTextField nccSecondCell;
    private javax.swing.JButton nccSetFirstCell;
    private javax.swing.JButton nccSetSecondCell;
    private javax.swing.JCheckBox nccShowMatchTags;
    private javax.swing.JTextField nccSizeToleranceAmt;
    private javax.swing.JTextField nccSizeTolerancePct;
    private javax.swing.JRadioButton netAscending;
    private javax.swing.ButtonGroup netDefaultOrder;
    private javax.swing.JRadioButton netDescending;
    private javax.swing.JCheckBox netIgnoreResistors;
    private javax.swing.JComboBox netStartingIndex;
    private javax.swing.JTextField netUnificationPrefix;
    private javax.swing.JCheckBox netUnifyLikeNamedNets;
    private javax.swing.JCheckBox netUnifyPwrGnd;
    private javax.swing.JPanel network;
    private javax.swing.JButton ok;
    private javax.swing.JRadioButton routAutoStitcher;
    private javax.swing.JComboBox routDefaultArc;
    private javax.swing.JCheckBox routMimicCanUnstitch;
    private javax.swing.JCheckBox routMimicInteractive;
    private javax.swing.JCheckBox routMimicNoOtherArcs;
    private javax.swing.JCheckBox routMimicNodeSizesMustMatch;
    private javax.swing.JCheckBox routMimicNodeTypesMustMatch;
    private javax.swing.JCheckBox routMimicNumArcsMustMatch;
    private javax.swing.JCheckBox routMimicPortsMustMatch;
    private javax.swing.JRadioButton routMimicStitcher;
    private javax.swing.JRadioButton routNoStitcher;
    private javax.swing.ButtonGroup routStitcher;
    private javax.swing.JPanel routing;
    private javax.swing.JPanel spice;
    private javax.swing.JPanel spice1;
    private javax.swing.JPanel spice2;
    private javax.swing.JPanel spice3;
    private javax.swing.JPanel spice4;
    private javax.swing.JPanel spice5;
    private javax.swing.JPanel spice6;
    private javax.swing.JButton spiceBrowseHeaderFile;
    private javax.swing.JButton spiceBrowseTrailerFile;
    private javax.swing.JTextField spiceCapacitance;
    private javax.swing.JScrollPane spiceCell;
    private javax.swing.JRadioButton spiceDeriveModelFromCircuit;
    private javax.swing.JTextField spiceEdgeCapacitance;
    private javax.swing.JComboBox spiceEnginePopup;
    private javax.swing.JCheckBox spiceForceGlobalPwrGnd;
    private javax.swing.ButtonGroup spiceHeader;
    private javax.swing.JTextField spiceHeaderCardExtension;
    private javax.swing.JTextField spiceHeaderCardFile;
    private javax.swing.JRadioButton spiceHeaderCardsFromFile;
    private javax.swing.JRadioButton spiceHeaderCardsWithExtension;
    private javax.swing.JScrollPane spiceLayer;
    private javax.swing.JComboBox spiceLevelPopup;
    private javax.swing.JTextField spiceMinCapacitance;
    private javax.swing.JTextField spiceMinResistance;
    private javax.swing.ButtonGroup spiceModel;
    private javax.swing.JTextField spiceModelCell;
    private javax.swing.JButton spiceModelFileBrowse;
    private javax.swing.JRadioButton spiceNoHeaderCards;
    private javax.swing.JRadioButton spiceNoTrailerCards;
    private javax.swing.JComboBox spiceOutputFormatPopup;
    private javax.swing.JComboBox spicePrimitivesetPopup;
    private javax.swing.JTextField spiceResistance;
    private javax.swing.JTextField spiceRunParameters;
    private javax.swing.JComboBox spiceRunPopup;
    private javax.swing.JLabel spiceTechnology;
    private javax.swing.ButtonGroup spiceTrailer;
    private javax.swing.JTextField spiceTrailerCardExtension;
    private javax.swing.JTextField spiceTrailerCardFile;
    private javax.swing.JRadioButton spiceTrailerCardsFromFile;
    private javax.swing.JRadioButton spiceTrailerCardsWithExtension;
    private javax.swing.JCheckBox spiceUseCellParameters;
    private javax.swing.JRadioButton spiceUseModelFromFile;
    private javax.swing.JCheckBox spiceUseNodeNames;
    private javax.swing.JCheckBox spiceUseParasitics;
    private javax.swing.JCheckBox spiceWriteTransSizesInLambda;
    private javax.swing.JTabbedPane tabPane;
    private javax.swing.JButton verBrowse;
    private javax.swing.JScrollPane verCells;
    private javax.swing.JCheckBox verDefWireTrireg;
    private javax.swing.JRadioButton verDeriveModel;
    private javax.swing.JTextField verFileName;
    private javax.swing.JComboBox verLibrary;
    private javax.swing.JCheckBox verUseAssign;
    private javax.swing.JRadioButton verUseModelFile;
    private javax.swing.JPanel verilog;
    private javax.swing.ButtonGroup verilogModel;
    private javax.swing.JPanel wellCheck;
    private javax.swing.ButtonGroup wellCheckNWell;
    private javax.swing.ButtonGroup wellCheckPWell;
    private javax.swing.JCheckBox wellFindFarthestDistance;
    private javax.swing.JCheckBox wellNMustConnectPower;
    private javax.swing.JRadioButton wellNMustHave1Contact;
    private javax.swing.JRadioButton wellNMustHaveAllContacts;
    private javax.swing.JRadioButton wellNNoContactCheck;
    private javax.swing.JCheckBox wellPMustConnectGround;
    private javax.swing.JRadioButton wellPMustHave1Contact;
    private javax.swing.JRadioButton wellPMustHaveAllContacts;
    private javax.swing.JRadioButton wellPNoContactCheck;
    // End of variables declaration//GEN-END:variables
	
}
