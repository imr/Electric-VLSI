/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DesignRulesTab.java
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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * Class to handle the "Design Rules" tab of the Preferences dialog.
 */
public class DesignRulesTab extends PreferencePanel
{
	/** Creates new form DesignRulesTab */
	public DesignRulesTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return designRules; }

	public String getName() { return "Design Rules"; }

	private JList designRulesFromList, designRulesToList;
	private DefaultListModel designRulesFromModel, designRulesToModel;
	private DRC.Rules drRules;
	private boolean designRulesUpdating = false;
	private boolean designRulesFactoryReset = false;
	private boolean [] designRulesValidLayers;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Design Rules tab.
	 */
	public void init()
	{
		// get the design rules for the current technology
		drRules = DRC.getRules(curTech);
		if (drRules == null || drRules.displayMessage != null)
		{
			if (drRules == null)
				drTechName.setText(curTech.getTechName() + " HAS NO DESIGN RULES");
			else
				drTechName.setText(curTech.getTechName() + ":" + drRules.displayMessage);
			drLayers.setEnabled(false);
			drNodes.setEnabled(false);
			drShowOnlyLinesWithRules.setEnabled(false);
			drMinWidth.setEnabled(false);
			drMinWidthRule.setEnabled(false);
			drMinHeight.setEnabled(false);
			drNormalConnected.setEnabled(false);
			drNormalConnectedRule.setEnabled(false);
			drNormalUnconnected.setEnabled(false);
			drNormalUnconnectedRule.setEnabled(false);
			drNormalEdge.setEnabled(false);
			drNormalEdgeRule.setEnabled(false);
			drWideConnected.setEnabled(false);
			drWideConnectedRule.setEnabled(false);
			drWideUnconnected.setEnabled(false);
			drWideUnconnectedRule.setEnabled(false);
			drMultiConnected.setEnabled(false);
			drMultiConnectedRule.setEnabled(false);
			drMultiUnconnected.setEnabled(false);
			drMultiUnconnectedRule.setEnabled(false);
			drWideLimit.setEnabled(false);
			factoryReset.setEnabled(false);
			return;
		}

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
		for(Iterator it = curTech.getNodes(); it.hasNext(); )
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
		for(Iterator it = curTech.getArcs(); it.hasNext(); )
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

		factoryReset.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { factoryResetDRCActionPerformed(evt); }
		});

		// load the dialog
		drTechName.setText("'"+drRules.techName+"'");
		designRulesSetupForLayersOrNodes();
		designRulesGetSelectedLayerLoadDRCToList();
	}

	private void factoryResetDRCActionPerformed(ActionEvent evt)
	{
		int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
			"Are you sure you want to do a factory reset of these design rules?");
		if (response != JOptionPane.YES_OPTION) return;
		designRulesFactoryReset = true;
//		okActionPerformed(null);
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
	 * Class to handle special changes to design rules.
	 */
	private static class DRCDocumentListener implements DocumentListener
	{
		DesignRulesTab dialog;

		DRCDocumentListener(DesignRulesTab dialog) { this.dialog = dialog; }

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
					drRules.minNodeSize[node*2] = new Double(TextUtils.atof(a));
				a = drMinHeight.getText();
				if (a.length() == 0) drRules.minNodeSize[node*2+1] = new Double(-1); else
					drRules.minNodeSize[node*2+1] = new Double(TextUtils.atof(a));
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
				drRules.conList[dindex] = new Double(TextUtils.atof(a));
			drRules.conListRules[dindex] = drNormalConnectedRule.getText();
			a = drNormalUnconnected.getText();
			if (a.length() == 0) drRules.unConList[dindex] = new Double(-1); else
				drRules.unConList[dindex] = new Double(TextUtils.atof(a));
			drRules.unConListRules[dindex] = drNormalUnconnectedRule.getText();

			// get new wide values
			a = drWideConnected.getText();
			if (a.length() == 0) drRules.conListWide[dindex] = new Double(-1); else
				drRules.conListWide[dindex] = new Double(TextUtils.atof(a));
			drRules.conListWideRules[dindex] = drWideConnectedRule.getText();
			a = drWideUnconnected.getText();
			if (a.length() == 0) drRules.unConListWide[dindex] = new Double(-1); else
				drRules.unConListWide[dindex] = new Double(TextUtils.atof(a));
			drRules.unConListWideRules[dindex] = drWideUnconnectedRule.getText();

			// get new multicut values
			a = drMultiConnected.getText();
			if (a.length() == 0) drRules.conListMulti[dindex] = new Double(-1); else
				drRules.conListMulti[dindex] = new Double(TextUtils.atof(a));
			drRules.conListMultiRules[dindex] = drMultiConnectedRule.getText();
			a = drMultiUnconnected.getText();
			if (a.length() == 0) drRules.unConListMulti[dindex] = new Double(-1); else
				drRules.unConListMulti[dindex] = new Double(TextUtils.atof(a));
			drRules.unConListMultiRules[dindex] = drMultiUnconnectedRule.getText();

			// get new edge values
			a = drNormalEdge.getText();
			if (a.length() == 0) drRules.edgeList[dindex] = new Double(-1); else
				drRules.edgeList[dindex] = new Double(TextUtils.atof(a));
			drRules.edgeListRules[dindex] = drNormalEdgeRule.getText();

			// get new width limit
			a = drWideLimit.getText();
			double value = TextUtils.atof(a);
			if (value > 0)
				drRules.setWideLimits(new double[] {value});

			// redraw the entry in the "to" list
			int lineNo = designRulesToList.getSelectedIndex();
			String line = drMakeToListLine(dindex, lineNo, false);
			designRulesToModel.setElementAt(line, lineNo);

			// update layer width rules
			a = drMinWidth.getText();
			if (a.length() == 0) drRules.minWidth[layer1] = new Double(-1); else
				drRules.minWidth[layer1] = new Double(TextUtils.atof(a));
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
			Object[] set = drRules.getWideLimits().toArray();
			if (set.length > 0)
			{
				Double wideLimit = ((Double)set[0]);
				drWideLimit.setText(TextUtils.formatDouble(wideLimit.doubleValue()));
			}
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
				drMinWidth.setText(TextUtils.formatDouble(wid));
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
		int layer2 = designRulesGetSelectedLayer(designRulesToList);
		if (layer1 >= 0 && layer2 >= 0)
		{
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
		}
		designRulesUpdating = false;
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Design Rules tab.
	 */
	public void term()
	{
		if (designRulesFactoryReset)
		{
			DRC.resetDRCDates();
			drRules = curTech.getFactoryDesignRules(null);
		}
		DRC.setRules(curTech, drRules);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        drLayerOrNode = new javax.swing.ButtonGroup();
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
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
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
        gridBagConstraints.gridheight = 11;
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

        factoryReset.setText("Factory Reset");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 3;
        designRules.add(factoryReset, gridBagConstraints);

        getContentPane().add(designRules, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
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
    private javax.swing.JButton factoryReset;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
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
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    // End of variables declaration//GEN-END:variables
	
}
