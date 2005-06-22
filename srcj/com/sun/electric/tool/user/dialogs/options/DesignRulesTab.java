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
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.DRCRules;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.utils.MOSRules;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * Class to handle the "Design Rules" tab of the Preferences dialog.
 */
public class DesignRulesTab extends PreferencePanel
{
	/** Creates new form DesignRulesTab */
	public DesignRulesTab(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return designRules; }

	public String getName() { return "Design Rules"; }

	private JList designRulesFromList, designRulesToList, designRulesNodeList;
	private DefaultListModel designRulesFromModel, designRulesToModel, designRulesNodeModel;
	private DRCRules drRules;
	private boolean designRulesUpdating = false;
	private boolean designRulesFactoryReset = false;
	private boolean [] designRulesValidLayers;
	private List wideSpacingRules;
	private Technology.Foundry foundry;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Design Rules tab.
	 */
	public void init()
	{
		// get the design rules for the current technology
		DRCRules rules = DRC.getRules(curTech);
		if (rules == null) //|| !(rules instanceof MOSRules))
		{
			drTechName.setText("Technology " + curTech.getTechName() + " HAS NO DESIGN RULES");
			drShowOnlyLinesWithRules.setEnabled(false);
			drNormalConnected.setEnabled(false);
			drNormalConnectedRule.setEnabled(false);
			drNormalUnconnected.setEnabled(false);
			drNormalUnconnectedRule.setEnabled(false);
			drNormalEdge.setEnabled(false);
			drNormalEdgeRule.setEnabled(false);
			drWidths.setEnabled(false);
			drLengths.setEnabled(false);
			drLayerSpacings.setEnabled(false);
			drMultiConnected.setEnabled(false);
			drMultiConnectedRule.setEnabled(false);
			drMultiUnconnected.setEnabled(false);
			drMultiUnconnectedRule.setEnabled(false);
			factoryReset.setEnabled(false);
			return;
		}

        drRules = rules;

		// figure out which layers are valid
        int numLayers = curTech.getNumLayers();
		designRulesValidLayers = new boolean[numLayers];
		for(int i=0; i<numLayers; i++) designRulesValidLayers[i] = false;
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
			ArcProto ap = (ArcProto)it.next();
			if (ap.isNotUsed()) continue;
			Technology.ArcLayer [] layers = ap.getLayers();
			for(int i=0; i<layers.length; i++)
			{
				Technology.ArcLayer al = layers[i];
				Layer layer = al.getLayer();
				designRulesValidLayers[layer.getIndex()] = true;
			}
		}

		// build the "node" list
		designRulesNodeModel = new DefaultListModel();
		designRulesNodeList = new JList(designRulesNodeModel);
		designRulesNodeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		drNodeList.setViewportView(designRulesNodeList);
		designRulesNodeList.clearSelection();
		designRulesNodeList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { designRulesGetSelectedNode(); }
		});
		for(Iterator it = curTech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			designRulesNodeModel.addElement(np.getName());
		}
		designRulesNodeList.setSelectedIndex(0);

		// build the "from" layer list
		designRulesFromModel = new DefaultListModel();
		designRulesFromList = new JList(designRulesFromModel);
		designRulesFromList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		drFromList.setViewportView(designRulesFromList);
		designRulesFromList.clearSelection();
		designRulesFromList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { designRulesGetSelectedLayerLoadDRCToList(); }
		});
        for(int i=0; i<designRulesValidLayers.length; i++)
		{
			if (!designRulesValidLayers[i]) continue;
            designRulesFromModel.addElement(curTech.getLayer(i).getName());
		}
		designRulesFromList.setSelectedIndex(0);

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

		// catch changes to the width rules popup
		drWidthLabel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { widePopupChanged(); }
		});
		drAddRule.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { addWideRule(); }
		});

		// have changes to edit fields get detected immediately
		designRulesUpdating = false;
		DRCDocumentListener myDocumentListener = new DRCDocumentListener(this);
		drNormalConnected.getDocument().addDocumentListener(myDocumentListener);
		drNormalConnectedRule.getDocument().addDocumentListener(myDocumentListener);
		drNormalUnconnected.getDocument().addDocumentListener(myDocumentListener);
		drNormalUnconnectedRule.getDocument().addDocumentListener(myDocumentListener);
		drNormalEdge.getDocument().addDocumentListener(myDocumentListener);
		drNormalEdgeRule.getDocument().addDocumentListener(myDocumentListener);
		drWidths.getDocument().addDocumentListener(myDocumentListener);
		drLengths.getDocument().addDocumentListener(myDocumentListener);
		drLayerRules.getDocument().addDocumentListener(myDocumentListener);
		drLayerSpacings.getDocument().addDocumentListener(myDocumentListener);
		drMultiConnected.getDocument().addDocumentListener(myDocumentListener);
		drMultiConnectedRule.getDocument().addDocumentListener(myDocumentListener);
		drMultiUnconnected.getDocument().addDocumentListener(myDocumentListener);
		drMultiUnconnectedRule.getDocument().addDocumentListener(myDocumentListener);
		drNodeWidth.getDocument().addDocumentListener(myDocumentListener);
		drNodeHeight.getDocument().addDocumentListener(myDocumentListener);
		drNodeRule.getDocument().addDocumentListener(myDocumentListener);

		factoryReset.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { factoryResetDRCActionPerformed(evt); }
		});

		// load the dialog
		drTechName.setText("Design Rules for Technology '" + curTech.getTechName() + "'");

        // Foundry
        String selectedFoundry = curTech.getSelectedFoundry();
        for (Iterator it = curTech.getFactories(); it.hasNext(); )
        {
            Technology.Foundry factory = (Technology.Foundry)it.next();
            defaultFoundryPulldown.addItem(factory.name);
            if (selectedFoundry.equals(factory.name)) foundry = factory;
        }
        defaultFoundryPulldown.setSelectedItem(foundry.name);

        // Resolution
		drResolutionValue.setText(TextUtils.formatDouble(curTech.getResolution()));

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
	 * Class to handle special changes to design rules.
	 */
	private static class DRCDocumentListener implements DocumentListener
	{
		private DesignRulesTab dialog;

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

		// get layer information
		int layer1 = designRulesGetSelectedLayer(designRulesFromList);
		if (layer1 < 0) return;
		int layer2 = designRulesGetSelectedLayer(designRulesToList);
		if (layer2 < 0) return;
        int dindex = curTech.getRuleIndex(layer1, layer2);

		// get new normal spacing values
        List list = new ArrayList();
		double value = TextUtils.atof(drNormalConnected.getText());
        list.add(new DRCTemplate(drNormalConnectedRule.getText(), foundry.techMode, DRCTemplate.CONSPA,
                0, 0, null, null, value, false));
		value = TextUtils.atof(drNormalUnconnected.getText());
        list.add(new DRCTemplate(drNormalUnconnectedRule.getText(), foundry.techMode, DRCTemplate.UCONSPA,
                0, 0, null, null, value, false));
        drRules.setSpacingRules(dindex, list, DRCTemplate.SPACING);

		// get new multicut spacing values
        list.clear();
        value = TextUtils.atof(drMultiConnected.getText());
        list.add(new DRCTemplate(drMultiConnectedRule.getText(), foundry.techMode, DRCTemplate.CONSPA,
                0, 0, null, null, value, true));
		value = TextUtils.atof(drMultiUnconnected.getText());
        list.add(new DRCTemplate(drMultiUnconnectedRule.getText(), foundry.techMode, DRCTemplate.UCONSPA,
                0, 0, null, null, value, true));
        drRules.setSpacingRules(dindex, list, DRCTemplate.CUTSPA);

		// get new edge values
        list.clear();
        value = TextUtils.atof(drNormalEdge.getText());
        list.add(new DRCTemplate(drNormalEdgeRule.getText(), foundry.techMode, DRCTemplate.CONSPA,
                0, 0, null, null, value, false));
        drRules.setSpacingRules(dindex, list, DRCTemplate.SPACINGE);

        // get new wide spacing values
		int curWid = drWidthLabel.getItemCount();
		if (curWid >= 0 && curWid < wideSpacingRules.size())
		{
			DRCRules.DRCRule wr = (DRCRules.DRCRule)wideSpacingRules.get(curWid);
			String widText = drWidths.getText().trim();
			String lenText = drLengths.getText().trim();
			if (widText.length() > 0 || lenText.length() > 0)
			{
				wr.maxWidth = TextUtils.atof(widText);
				wr.minLength = TextUtils.atof(lenText);
				wr.value = TextUtils.atof(drLayerSpacings.getText());
				wr.ruleName = drLayerRules.getText();
		        drRules.setSpacingRules(dindex, wideSpacingRules, DRCTemplate.SPACINGW);
			}
		}

		// pickup changes to node rules
		int nodeIndex = designRulesNodeList.getSelectedIndex();
		String widthText = drNodeWidth.getText().trim();
		String heightText = drNodeHeight.getText().trim();
		double width = -1, height = -1;
		if (widthText.length() > 0 || heightText.length() > 0)
		{
			width = TextUtils.atof(widthText);
			height = TextUtils.atof(heightText);
		}
		drRules.setMinNodeSize(nodeIndex, drNodeRule.getText(), width, height);

		// pickup changes to layer minimum size rule
		Layer layer = curTech.getLayer(layer1);
		double minSize = -1;
		String minSizeText = drLayerWidth.getText().trim();
		if (minSizeText.length() > 0) minSize = TextUtils.atof(minSizeText);
		// TODO: how to do this?
//		DRC.setMinValue(layer, drLayerWidthRule.getText(), minSize, DRCTemplate.MINWID, foundry);
	}

	/**
	 * Method to handle selection of a different node in the top scroll list.
	 */
	private void designRulesGetSelectedNode()
	{
		designRulesUpdating = true;
		int nodeIndex = designRulesNodeList.getSelectedIndex();
		DRCRules.DRCNodeRule nr = drRules.getMinNodeSize(nodeIndex);
		drNodeWidth.setText("");
	    drNodeHeight.setText("");
		drNodeRule.setText("");
		if (nr != null)
		{
			if (nr.getWidth() >= 0) drNodeWidth.setText(TextUtils.formatDouble(nr.getWidth()));
			if (nr.getHeight() >= 0) drNodeHeight.setText(TextUtils.formatDouble(nr.getHeight()));
			drNodeRule.setText(nr.ruleName);
		}
		designRulesUpdating = false;
	}

	/**
	 * Method called when the wide rules popup changes.
	 */
	private void widePopupChanged()
	{
		int index = drWidthLabel.getSelectedIndex();
		if (index < 0 || index >= wideSpacingRules.size()) return;
		designRulesUpdating = true;
		DRCRules.DRCRule tmp = (DRCRules.DRCRule)wideSpacingRules.get(index);
		drWidths.setText("");
		drLengths.setText("");
		drLayerRules.setText("");
		drLayerSpacings.setText("");
		if (tmp.maxWidth != 0) drWidths.setText(Double.toString(tmp.maxWidth));
		if (tmp.minLength != 0) drLengths.setText(Double.toString(tmp.minLength));
		drLayerSpacings.setText(Double.toString(tmp.value));
		drLayerRules.setText(tmp.ruleName);
		designRulesUpdating = false;
	}

	/**
	 * Method called when the "add rule" button is pushed to add a new wide rule.
	 */
	private void addWideRule()
	{
		int soFar = drWidthLabel.getItemCount();
		drWidthLabel.addItem("Rule " + (soFar+1));
		DRCRules.DRCRule newRule = new DRCRules.DRCRule("", 0, 0, 0, DRCTemplate.SPACINGW);
		wideSpacingRules.add(newRule);
		drWidthLabel.setSelectedIndex(soFar);
	}

	/**
	 * Method called when the user clicks on the "from layer" list or the "show only lines with rules" checkbox.
	 */
	private void designRulesGetSelectedLayerLoadDRCToList()
	{
		designRulesUpdating = true;

		// show layer information
		boolean onlyvalid = drShowOnlyLinesWithRules.isSelected();
		int j = designRulesGetSelectedLayer(designRulesFromList);
		if (j >= 0)
		{
			designRulesToModel.clear();
			int count = 0;
            for(int i=0; i<designRulesValidLayers.length; i++)
			{
				if (!designRulesValidLayers[i]) continue;
				int layer1 = j;
				int layer2 = i;
                int dindex = curTech.getRuleIndex(layer1, layer2);
				String line = drMakeToListLine(dindex, i, onlyvalid);

				if (line.length() == 0) continue;
				designRulesToModel.addElement(line);
				count++;
			}
			if (count > 0) designRulesToList.setSelectedIndex(0);
		}

		// show minimum layer size
		Layer layer = curTech.getLayer(j);
        DRCRules.DRCRule lr = DRC.getMinValue(layer, DRCTemplate.MINWID, foundry.techMode);
        if (lr != null)
		{
			drLayerWidth.setText(TextUtils.formatDouble(lr.value));
		    drLayerWidthRule.setText(lr.ruleName);
		} else
		{
			drLayerWidth.setText("");
		    drLayerWidthRule.setText("");
		}

		designRulesUpdating = false;
		designRulesShowSelectedLayerRules();
	}

	/**
	 * Method called when the user clicks in the "to layer" list.
	 */
	private void designRulesShowSelectedLayerRules()
	{
		if (designRulesUpdating) return;

		// show layer information
		designRulesUpdating = true;
		drNormalConnected.setText("");    drNormalConnectedRule.setText("");
		drNormalUnconnected.setText("");  drNormalUnconnectedRule.setText("");
		drMultiConnected.setText("");     drMultiConnectedRule.setText("");
		drMultiUnconnected.setText("");   drMultiUnconnectedRule.setText("");
		drNormalEdge.setText("");         drNormalEdgeRule.setText("");

		int layer1 = designRulesGetSelectedLayer(designRulesFromList);
		int layer2 = designRulesGetSelectedLayer(designRulesToList);

		if (layer1 >= 0 && layer2 >= 0)
		{
            int dindex = curTech.getRuleIndex(layer1, layer2);
            double wideLimit = 0;
            List spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.SPACING, foundry.techMode);
            for (int i = 0; i < spacingRules.size(); i++)
            {
                DRCRules.DRCRule tmp = (DRCRules.DRCRule)spacingRules.get(i);
                if (tmp.type == DRCTemplate.CONSPA)
                {
                    drNormalConnected.setText(Double.toString(tmp.value));
                    drNormalConnectedRule.setText(tmp.ruleName);
                }
                else if (tmp.type == DRCTemplate.UCONSPA)
                {
                    drNormalUnconnected.setText(Double.toString(tmp.value));
                    drNormalUnconnectedRule.setText(tmp.ruleName);
                }
                if (tmp.maxWidth > 0) wideLimit = tmp.maxWidth;
            }

			spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.SPACINGW, foundry.techMode);
			wideSpacingRules = new ArrayList();
			for(Iterator it = spacingRules.iterator(); it.hasNext(); )
			{
				DRCRules.DRCRule tmp = (DRCRules.DRCRule)it.next();
	            if (tmp.type != DRCTemplate.UCONSPA) continue;
				wideSpacingRules.add(tmp);
			}
			int rulesFilled = 0;
			drWidthLabel.removeAllItems();
			for (int i = 0; i < wideSpacingRules.size(); i++)
				drWidthLabel.addItem("Rule " + (i+1));
			drWidths.setText("");
			drLengths.setText("");
			drLayerRules.setText("");
			drLayerSpacings.setText("");
			designRulesUpdating = false;
			if (wideSpacingRules.size() != 0)
				drWidthLabel.setSelectedIndex(0);
			
            spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.CUTSPA, foundry.techMode);
            for (int i = 0; i < spacingRules.size(); i++)
            {
                DRCRules.DRCRule tmp = (DRCRules.DRCRule)spacingRules.get(i);
                if (tmp.type == DRCTemplate.CONSPA)
                {
                    drMultiConnected.setText( Double.toString(tmp.value));
                    drMultiConnectedRule.setText(tmp.ruleName);
                }
                else if (tmp.type == DRCTemplate.UCONSPA)
                {
                    drMultiUnconnected.setText(Double.toString(tmp.value));
                    drMultiUnconnectedRule.setText(tmp.ruleName);
                }
            }

            spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.SPACINGE, foundry.techMode);
            for (int i = 0; i < spacingRules.size(); i++)
            {
                DRCRules.DRCRule tmp = (DRCRules.DRCRule)spacingRules.get(i);
                // Any is fine
                drNormalEdge.setText(Double.toString(tmp.value));
                drNormalEdgeRule.setText(tmp.ruleName);
            }
		}
		designRulesUpdating = false;
	}

	private int designRulesGetSelectedLayer(JList theList)
	{
		int lineNo = theList.getSelectedIndex();
		if (lineNo < 0) return -1;
		String lName = (String)theList.getSelectedValue();
		int termPos = lName.indexOf(" (");
		if (termPos >= 0) lName = lName.substring(0, termPos);

		for(int layer=0; layer<designRulesValidLayers.length; layer++)
			if (lName.equals(curTech.getLayer(layer).getName())) return layer;
		return -1;
	}

	private String drMakeToListLine(int dindex, int lindex, boolean onlyValid)
	{
		boolean gotRule = false;
		List spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.SPACING, foundry.techMode);
		for (int i = 0; i < spacingRules.size(); i++)
		{
			DRCRules.DRCRule tmp = (DRCRules.DRCRule)spacingRules.get(i);
			if (tmp.value > 0) gotRule = true;
		}
		spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.SPACINGW, foundry.techMode);
		for (int i = 0; i < spacingRules.size(); i++)
		{
			DRCRules.DRCRule tmp = (DRCRules.DRCRule)spacingRules.get(i);
			if (tmp.value > 0) gotRule = true;
		}
		spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.CUTSPA, foundry.techMode);
		for (int i = 0; i < spacingRules.size(); i++)
		{
			DRCRules.DRCRule tmp = (DRCRules.DRCRule)spacingRules.get(i);
			if (tmp.value > 0) gotRule = true;
		}
		spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.SPACINGE, foundry.techMode);
		for (int i = 0; i < spacingRules.size(); i++)
		{
			DRCRules.DRCRule tmp = (DRCRules.DRCRule)spacingRules.get(i);
			if (tmp.value > 0) gotRule = true;
		}
		if (onlyValid && !gotRule) return "";
        String ret = curTech.getLayer(lindex).getName();
		return ret;
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
            DRCRules rules = curTech.getFactoryDesignRules();
            if (rules instanceof MOSRules)
			    drRules = (MOSRules)rules;
		}
		DRC.setRules(curTech, drRules);

        String foundryName = (String)defaultFoundryPulldown.getSelectedItem();
        if (!foundryName.equals(curTech.getSelectedFoundry()))
            curTech.setSelectedFoundry(foundryName);

		double currentResolution = TextUtils.atof(drResolutionValue.getText());
		if (currentResolution != curTech.getResolution())
			curTech.setResolution(currentResolution);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        drLayerOrNode = new javax.swing.ButtonGroup();
        designRules = new javax.swing.JPanel();
        top = new javax.swing.JPanel();
        drNodeList = new javax.swing.JScrollPane();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        drNodeWidth = new javax.swing.JTextField();
        drNodeRule = new javax.swing.JTextField();
        drNodeHeight = new javax.swing.JTextField();
        bottom = new javax.swing.JPanel();
        drMultiUnconnectedRule = new javax.swing.JTextField();
        drMultiUnconnected = new javax.swing.JTextField();
        drMultiConnectedRule = new javax.swing.JTextField();
        drMultiConnected = new javax.swing.JTextField();
        drNormalEdgeRule = new javax.swing.JTextField();
        drNormalEdge = new javax.swing.JTextField();
        drNormalUnconnectedRule = new javax.swing.JTextField();
        drNormalUnconnected = new javax.swing.JTextField();
        drNormalConnectedRule = new javax.swing.JTextField();
        drNormalConnected = new javax.swing.JTextField();
        jLabel53 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        jLabel51 = new javax.swing.JLabel();
        jLabel47 = new javax.swing.JLabel();
        jLabel46 = new javax.swing.JLabel();
        jLabel45 = new javax.swing.JLabel();
        jLabel44 = new javax.swing.JLabel();
        jLabel43 = new javax.swing.JLabel();
        jLabel42 = new javax.swing.JLabel();
        drToList = new javax.swing.JScrollPane();
        drShowOnlyLinesWithRules = new javax.swing.JCheckBox();
        drFromList = new javax.swing.JScrollPane();
        jLabel1 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        drWidths = new javax.swing.JTextField();
        drLengths = new javax.swing.JTextField();
        drLayerSpacings = new javax.swing.JTextField();
        drLayerRules = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        drWidthLabel = new javax.swing.JComboBox();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel10 = new javax.swing.JLabel();
        drLayerWidth = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        drLayerWidthRule = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        jLabel50 = new javax.swing.JLabel();
        jLabel54 = new javax.swing.JLabel();
        drAddRule = new javax.swing.JButton();
        drTechName = new javax.swing.JLabel();
        defaultFoundryLabel = new javax.swing.JLabel();
        defaultFoundryPulldown = new javax.swing.JComboBox();
        drResolutionLabel = new javax.swing.JLabel();
        drResolutionValue = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        factoryReset = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Tool Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        designRules.setLayout(new java.awt.GridBagLayout());

        top.setLayout(new java.awt.GridBagLayout());

        top.setBorder(new javax.swing.border.TitledBorder("Node Rules"));
        drNodeList.setOpaque(false);
        drNodeList.setPreferredSize(new java.awt.Dimension(200, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drNodeList, gridBagConstraints);

        jLabel4.setText("Width:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(jLabel4, gridBagConstraints);

        jLabel5.setText("Height:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(jLabel5, gridBagConstraints);

        jLabel7.setText("Minimum Size");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(jLabel7, gridBagConstraints);

        jLabel8.setText("Rule");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(jLabel8, gridBagConstraints);

        drNodeWidth.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drNodeWidth, gridBagConstraints);

        drNodeRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drNodeRule, gridBagConstraints);

        drNodeHeight.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drNodeHeight, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        designRules.add(top, gridBagConstraints);

        bottom.setLayout(new java.awt.GridBagLayout());

        bottom.setBorder(new javax.swing.border.TitledBorder("Layer Rules"));
        drMultiUnconnectedRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 4);
        bottom.add(drMultiUnconnectedRule, gridBagConstraints);

        drMultiUnconnected.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        bottom.add(drMultiUnconnected, gridBagConstraints);

        drMultiConnectedRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 4);
        bottom.add(drMultiConnectedRule, gridBagConstraints);

        drMultiConnected.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        bottom.add(drMultiConnected, gridBagConstraints);

        drNormalEdgeRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 4);
        bottom.add(drNormalEdgeRule, gridBagConstraints);

        drNormalEdge.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        bottom.add(drNormalEdge, gridBagConstraints);

        drNormalUnconnectedRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 4);
        bottom.add(drNormalUnconnectedRule, gridBagConstraints);

        drNormalUnconnected.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        bottom.add(drNormalUnconnected, gridBagConstraints);

        drNormalConnectedRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 4);
        bottom.add(drNormalConnectedRule, gridBagConstraints);

        drNormalConnected.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        bottom.add(drNormalConnected, gridBagConstraints);

        jLabel53.setText("Not connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 4);
        bottom.add(jLabel53, gridBagConstraints);

        jLabel52.setText("When connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 4);
        bottom.add(jLabel52, gridBagConstraints);

        jLabel51.setText("Multiple cuts:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(jLabel51, gridBagConstraints);

        jLabel47.setText("Edge:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 4);
        bottom.add(jLabel47, gridBagConstraints);

        jLabel46.setText("Not connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 4);
        bottom.add(jLabel46, gridBagConstraints);

        jLabel45.setText("When connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 4);
        bottom.add(jLabel45, gridBagConstraints);

        jLabel44.setText("Rule");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(jLabel44, gridBagConstraints);

        jLabel43.setText("Distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(jLabel43, gridBagConstraints);

        jLabel42.setText("Normal:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(jLabel42, gridBagConstraints);

        drToList.setOpaque(false);
        drToList.setPreferredSize(new java.awt.Dimension(100, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 13;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(drToList, gridBagConstraints);

        drShowOnlyLinesWithRules.setText("Show only \"to\" entries with rules");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(drShowOnlyLinesWithRules, gridBagConstraints);

        drFromList.setPreferredSize(new java.awt.Dimension(100, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(drFromList, gridBagConstraints);

        jLabel1.setText("From:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(jLabel1, gridBagConstraints);

        jLabel9.setText("To:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(jLabel9, gridBagConstraints);

        drWidths.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(drWidths, gridBagConstraints);

        drLengths.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(drLengths, gridBagConstraints);

        drLayerSpacings.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        bottom.add(drLayerSpacings, gridBagConstraints);

        drLayerRules.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 4);
        bottom.add(drLayerRules, gridBagConstraints);

        jLabel3.setText("and Length >");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(jLabel3, gridBagConstraints);

        jLabel2.setText("If Width >");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        bottom.add(drWidthLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        bottom.add(jSeparator1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        bottom.add(jSeparator2, gridBagConstraints);

        jLabel10.setText("Min. Width:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        bottom.add(jLabel10, gridBagConstraints);

        drLayerWidth.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        bottom.add(drLayerWidth, gridBagConstraints);

        jLabel11.setText("Rule:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        bottom.add(jLabel11, gridBagConstraints);

        drLayerWidthRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        bottom.add(drLayerWidthRule, gridBagConstraints);

        jLabel12.setText("Wide rules:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(jLabel12, gridBagConstraints);

        jLabel48.setText("Distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(jLabel48, gridBagConstraints);

        jLabel49.setText("Rule");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(jLabel49, gridBagConstraints);

        jLabel50.setText("Distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(jLabel50, gridBagConstraints);

        jLabel54.setText("Rule");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(jLabel54, gridBagConstraints);

        drAddRule.setText("Add Rule");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridheight = 2;
        bottom.add(drAddRule, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        designRules.add(bottom, gridBagConstraints);

        drTechName.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(drTechName, gridBagConstraints);

        defaultFoundryLabel.setText("Foundry:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(defaultFoundryLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(defaultFoundryPulldown, gridBagConstraints);

        drResolutionLabel.setText("Minimum resolution:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(drResolutionLabel, gridBagConstraints);

        drResolutionValue.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(drResolutionValue, gridBagConstraints);

        jLabel6.setText("(use 0 to ignore resolution check)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(jLabel6, gridBagConstraints);

        factoryReset.setText("Factory Reset");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
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
    private javax.swing.JPanel bottom;
    private javax.swing.JLabel defaultFoundryLabel;
    private javax.swing.JComboBox defaultFoundryPulldown;
    private javax.swing.JPanel designRules;
    private javax.swing.JButton drAddRule;
    private javax.swing.JScrollPane drFromList;
    private javax.swing.ButtonGroup drLayerOrNode;
    private javax.swing.JTextField drLayerRules;
    private javax.swing.JTextField drLayerSpacings;
    private javax.swing.JTextField drLayerWidth;
    private javax.swing.JTextField drLayerWidthRule;
    private javax.swing.JTextField drLengths;
    private javax.swing.JTextField drMultiConnected;
    private javax.swing.JTextField drMultiConnectedRule;
    private javax.swing.JTextField drMultiUnconnected;
    private javax.swing.JTextField drMultiUnconnectedRule;
    private javax.swing.JTextField drNodeHeight;
    private javax.swing.JScrollPane drNodeList;
    private javax.swing.JTextField drNodeRule;
    private javax.swing.JTextField drNodeWidth;
    private javax.swing.JTextField drNormalConnected;
    private javax.swing.JTextField drNormalConnectedRule;
    private javax.swing.JTextField drNormalEdge;
    private javax.swing.JTextField drNormalEdgeRule;
    private javax.swing.JTextField drNormalUnconnected;
    private javax.swing.JTextField drNormalUnconnectedRule;
    private javax.swing.JLabel drResolutionLabel;
    private javax.swing.JTextField drResolutionValue;
    private javax.swing.JCheckBox drShowOnlyLinesWithRules;
    private javax.swing.JLabel drTechName;
    private javax.swing.JScrollPane drToList;
    private javax.swing.JComboBox drWidthLabel;
    private javax.swing.JTextField drWidths;
    private javax.swing.JButton factoryReset;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
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
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JPanel top;
    // End of variables declaration//GEN-END:variables

}
