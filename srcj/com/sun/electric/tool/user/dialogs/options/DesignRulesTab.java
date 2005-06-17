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

import com.sun.electric.technology.*;
import com.sun.electric.technology.technologies.utils.MOSRules;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.database.text.TextUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

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
	public DesignRulesTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return designRules; }

	public String getName() { return "Design Rules"; }

	private JList designRulesFromList, designRulesToList;
	private DefaultListModel designRulesFromModel, designRulesToModel;
	private DRCRules drRules;
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
		DRCRules rules = DRC.getRules(curTech);
		if (rules == null) //|| !(rules instanceof MOSRules))
		{
			if (rules == null)
				drTechName.setText(curTech.getTechName() + " HAS NO DESIGN RULES");
			else
				drTechName.setText(curTech.getTechName() + ": UNDER CONSTRUCTION");
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

        drRules = rules;
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

		factoryReset.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { factoryResetDRCActionPerformed(evt); }
		});

		// load the dialog
		drTechName.setText("'"+curTech.getTechName()+"'");
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

			double w = TextUtils.atof(drMinWidth.getText());
            double h = TextUtils.atof(drMinHeight.getText());
            drRules.setMinNodeSize(lineNo, drMinWidthRule.getText(), w, h);
		} else
		{
			// get layer information
			int layer1 = designRulesGetSelectedLayer(designRulesFromList);
			if (layer1 < 0) return;
			int layer2 = designRulesGetSelectedLayer(designRulesToList);
			if (layer2 < 0) return;
            int dindex = curTech.getRuleIndex(layer1, layer2);

			// get new width limit
			double wideLimit = TextUtils.atof(drWideLimit.getText());
            List list = new ArrayList(7);
            int techMode = DRC.getFoundry();

			// get new normal spacing values
			double value = TextUtils.atof(drNormalConnected.getText());
            list.add(new DRCTemplate(drNormalConnectedRule.getText(), techMode, DRCTemplate.CONSPA,
                    wideLimit, 0, null, null, value, false));
			value = TextUtils.atof(drNormalUnconnected.getText());
            list.add(new DRCTemplate(drNormalUnconnectedRule.getText(), techMode, DRCTemplate.UCONSPA,
                    0, 0, null, null, value, false));
            drRules.setSpacingRules(dindex, list, DRCTemplate.SPACING);

            // get new wide spacing values
            list.clear();
            value = TextUtils.atof(drWideConnected.getText());
            list.add(new DRCTemplate(drWideConnectedRule.getText(), techMode, DRCTemplate.CONSPA,
                    0, 0, null, null, value, false));
			value = TextUtils.atof(drWideUnconnected.getText());
            list.add(new DRCTemplate(drWideUnconnectedRule.getText(), techMode, DRCTemplate.UCONSPA,
                    0, 0, null, null, value, false));
            drRules.setSpacingRules(dindex, list, DRCTemplate.SPACINGW);

			// get new multicut spacing values
            list.clear();
            value = TextUtils.atof(drMultiConnected.getText());
            list.add(new DRCTemplate(drMultiConnectedRule.getText(), techMode, DRCTemplate.CONSPA,
                    0, 0, null, null, value, true));
			value = TextUtils.atof(drMultiUnconnected.getText());
            list.add(new DRCTemplate(drMultiUnconnectedRule.getText(), techMode, DRCTemplate.UCONSPA,
                    0, 0, null, null, value, true));
            drRules.setSpacingRules(dindex, list, DRCTemplate.CUTSPA);

			// get new edge values
            list.clear();
            value = TextUtils.atof(drNormalEdge.getText());
            list.add(new DRCTemplate(drNormalEdgeRule.getText(), techMode, DRCTemplate.CONSPA,
                    0, 0, null, null, value, false));
            drRules.setSpacingRules(dindex, list, DRCTemplate.SPACINGE);

			// redraw the entry in the "to" list
			int lineNo = designRulesToList.getSelectedIndex();
			String line = drMakeToListLine(dindex, lineNo, false);
			designRulesToModel.setElementAt(line, lineNo);

			// update layer width rules
			value = TextUtils.atof(drMinWidth.getText());
            drRules.setMinValue(curTech.getLayer(layer1), drMinWidthRule.getText(), value, DRCTemplate.MINWID, DRCTemplate.ALL);
		}
	}

	private void designRulesSetupForLayersOrNodes()
	{
		designRulesFromModel.clear();
		designRulesUpdating = true;
		if (drNodes.isSelected())
		{
			// list the nodes
            String[] nodesWithRules = drRules.getNodesWithRules();
			for(int i=0; i<nodesWithRules.length; i++)
				designRulesFromModel.addElement(nodesWithRules[i]);
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
            for(int i=0; i<designRulesValidLayers.length; i++)
			{
				if (!designRulesValidLayers[i]) continue;
                designRulesFromModel.addElement(curTech.getLayer(i).getName());
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
            drWideLimit.setText("");
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
            drMinWidth.setText("");
            drMinHeight.setText("");
            drMinWidthRule.setText("");
            DRCRules.DRCNodeRule rule = drRules.getMinNodeSize(j);
            if (rule != null)
            {
                if (rule.getWidth() >= 0) drMinWidth.setText(Double.toString(rule.getWidth()));
                if (rule.getHeight() >= 0) drMinHeight.setText(Double.toString(rule.getHeight()));
                drMinWidthRule.setText(rule.ruleName);
            }
		} else
		{
			// show layer information
			boolean onlyvalid = drShowOnlyLinesWithRules.isSelected();
			int j = designRulesGetSelectedLayer(designRulesFromList);
			if (j >= 0)
			{
                drMinWidth.setText("");
                // @TODO ALL is not OK
                DRCRules.DRCRule rule = drRules.getMinValue(curTech.getLayer(j), DRCTemplate.MINWID, DRCTemplate.ALL);
                if (rule != null)
                {
                    drMinWidth.setText(Double.toString(rule.value));
                    drMinWidthRule.setText(rule.ruleName);
                }
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
		}
		designRulesUpdating = false;
		designRulesShowSelectedLayerRules();
	}

	private String drMakeToListLine(int dindex, int lindex, boolean onlyValid)
	{
		String conDist = "";
		String unConDist = "";
        int techMode = DRC.getFoundry();
        List spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.SPACING, techMode);
        for (int i = 0; i < spacingRules.size(); i++)
        {
            DRCRules.DRCRule tmp = (DRCRules.DRCRule)spacingRules.get(i);
            if (tmp.type == DRCTemplate.CONSPA) conDist = Double.toString(tmp.value);
            else if (tmp.type == DRCTemplate.UCONSPA) unConDist = Double.toString(tmp.value);
        }
		String conDistWide = "";
		String unConDistWide = "";
        spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.SPACINGW, techMode);
        for (int i = 0; i < spacingRules.size(); i++)
        {
            DRCRules.DRCRule tmp = (DRCRules.DRCRule)spacingRules.get(i);
            if (tmp.type == DRCTemplate.CONSPA) conDistWide = Double.toString(tmp.value);
            else if (tmp.type == DRCTemplate.UCONSPA) unConDistWide = Double.toString(tmp.value);
        }
		String conDistMulti = "";
		String unConDistMulti = "";
        spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.CUTSPA, techMode);
        for (int i = 0; i < spacingRules.size(); i++)
        {
            DRCRules.DRCRule tmp = (DRCRules.DRCRule)spacingRules.get(i);
            if (tmp.type == DRCTemplate.CONSPA) conDistMulti = Double.toString(tmp.value);
            else if (tmp.type == DRCTemplate.UCONSPA) unConDistMulti = Double.toString(tmp.value);
        }
		String edgeDist = "";
        spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.SPACINGE, techMode);
        for (int i = 0; i < spacingRules.size(); i++)
        {
            DRCRules.DRCRule tmp = (DRCRules.DRCRule)spacingRules.get(i);
            // Any is fine
            edgeDist = Double.toString(tmp.value);
        }
		if (onlyValid)
		{
			if (conDist.length() == 0 && unConDist.length() == 0 && conDistWide.length() == 0 &&
				unConDistWide.length() == 0 && conDistMulti.length() == 0 && unConDistMulti.length() == 0 &&
				edgeDist.length() == 0) return "";
		}
        String ret = curTech.getLayer(lindex).getName() + " (" +
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

//        for(int layer=0; layer<drRules.numLayers; layer++)
		for(int layer=0; layer<designRulesValidLayers.length; layer++)
			if (lName.equals(curTech.getLayer(layer).getName())) return layer;
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
        int techMode = DRC.getFoundry();

		if (layer1 >= 0 && layer2 >= 0)
		{
            int dindex = curTech.getRuleIndex(layer1, layer2);
            double wideLimit = 0;
            List spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.SPACING, techMode);
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
            if (wideLimit > 0) drWideLimit.setText(Double.toString(wideLimit)); // for now

            spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.SPACINGW, techMode);
            for (int i = 0; i < spacingRules.size(); i++)
            {
                DRCRules.DRCRule tmp = (DRCRules.DRCRule)spacingRules.get(i);
                if (tmp.type == DRCTemplate.CONSPA)
                {
                    drWideConnected.setText(Double.toString(tmp.value));
                    drWideConnectedRule.setText(tmp.ruleName);
                }
                else if (tmp.type == DRCTemplate.UCONSPA)
                {
                    drWideUnconnected.setText(Double.toString(tmp.value));
                    drWideUnconnectedRule.setText(tmp.ruleName);
                }
            }

            spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.CUTSPA, techMode);
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

            spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.SPACINGE, techMode);
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
