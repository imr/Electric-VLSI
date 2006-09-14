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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.technology.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * Class to handle the "Design Rules" tab of the Preferences frame.
 */
public class DesignRulesPanel extends JPanel
{
    private Technology curTech;
	private JList designRulesFromList, designRulesToList, designRulesNodeList;
	private DefaultListModel designRulesToModel;
	private DRCRules drRules;
	private boolean designRulesUpdating = false;
	private boolean designRulesFactoryReset = false;
	private boolean [] designRulesValidLayers;
	private List<DRCTemplate> wideSpacingRules;
	private Foundry.Type foundry;

	/** Creates new form DesignRulesTab */
	public DesignRulesPanel()
	{
		initComponents();
	}

	/**
	 * Method called at the start of the frame.
	 * Caches current values and displays them in the Design Rules tab.
	 */
	public void init(Technology tech, Foundry.Type foun, DRCRules drcR)
	{
        curTech = tech;
        foundry = foun;
        drRules = drcR;
		if (drRules == null)
		{
			drShowOnlyLinesWithRules.setEnabled(false);
			drNormalConnected.setEnabled(false);
			drNormalConnectedRule.setEnabled(false);
			drNormalUnconnected.setEnabled(false);
			drNormalUnconnectedRule.setEnabled(false);
			drNormalEdge.setEnabled(false);
			drNormalEdgeRule.setEnabled(false);
			drWidths.setEnabled(false);
			drLengths.setEnabled(false);
			drSpacings.setEnabled(false);
			drMultiUnconnected.setEnabled(false);
			drMultiUnconnectedRule.setEnabled(false);
			return;
		}

		// figure out which layers are valid
        int numLayers = curTech.getNumLayers();
		designRulesValidLayers = new boolean[numLayers];
		for(int i=0; i<numLayers; i++) designRulesValidLayers[i] = false;
		for(Iterator<PrimitiveNode> it = curTech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = it.next();
			if (np.isNotUsed()) continue;
			Technology.NodeLayer [] layers = np.getLayers();
			for(Technology.NodeLayer nl : layers)
			{
				Layer layer = nl.getLayer();
				designRulesValidLayers[layer.getIndex()] = true;
			}
		}
		for(Iterator<ArcProto> it = curTech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			if (ap.isNotUsed()) continue;
			Technology.ArcLayer [] layers = ap.getLayers();
			for(Technology.ArcLayer al : layers)
			{
				Layer layer = al.getLayer();
				designRulesValidLayers[layer.getIndex()] = true;
			}
		}

		// build the "node" list
		DefaultListModel designRulesNodeModel = new DefaultListModel();
		designRulesNodeList = new JList(designRulesNodeModel);
		designRulesNodeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		drNodeList.setViewportView(designRulesNodeList);
		designRulesNodeList.clearSelection();
		designRulesNodeList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { designRulesGetSelectedNode(); }
		});
		for(Iterator<PrimitiveNode> it = curTech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = it.next();
			designRulesNodeModel.addElement(np.getName());
		}
		designRulesNodeList.setSelectedIndex(0);

		// build the "from" layer list
		DefaultListModel designRulesFromModel = new DefaultListModel();
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
		drSpacingsList.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { widePopupChanged(false); }
		});

		// have changes to edit fields get detected immediately
		designRulesUpdating = false;

        DRCDocumentListener listener = new DRCDocumentListener(this, drNormalConnected, DRCDocType.SPACING);
		drNormalConnected.getDocument().addDocumentListener(listener);
		drNormalConnectedRule.getDocument().addDocumentListener(listener);

        listener = new DRCDocumentListener(this, drNormalUnconnected, DRCDocType.SPACING);
		drNormalUnconnected.getDocument().addDocumentListener(listener);
		drNormalUnconnectedRule.getDocument().addDocumentListener(listener);

        listener = new DRCDocumentListener(this, drNormalEdge, DRCDocType.SPACING);
		drNormalEdge.getDocument().addDocumentListener(listener);
		drNormalEdgeRule.getDocument().addDocumentListener(listener);

        listener = new DRCDocumentListener(this, drLayerWidth, DRCDocType.MINIMUM);
		drLayerWidth.getDocument().addDocumentListener(listener);
		drLayerWidthRule.getDocument().addDocumentListener(listener);

        listener = new DRCDocumentListener(this, drLayerArea, DRCDocType.MINIMUM);
		drLayerArea.getDocument().addDocumentListener(listener);
		drLayerAreaRule.getDocument().addDocumentListener(listener);

        listener = new DRCDocumentListener(this, drLayerEnclosure, DRCDocType.MINIMUM);
		drLayerEnclosure.getDocument().addDocumentListener(listener);
		drLayerEAreaRule.getDocument().addDocumentListener(listener);

        listener = new DRCDocumentListener(this, drSpacings, DRCDocType.SPACING);
		drWidths.getDocument().addDocumentListener(listener);
		drLengths.getDocument().addDocumentListener(listener);
		drSpacingsRule.getDocument().addDocumentListener(listener);
		drSpacings.getDocument().addDocumentListener(listener);

        listener = new DRCDocumentListener(this, drMultiUnconnected, DRCDocType.SPACING);
		drMultiUnconnected.getDocument().addDocumentListener(listener);
		drMultiUnconnectedRule.getDocument().addDocumentListener(listener);

        listener = new DRCDocumentListener(this, drNodeWidth, DRCDocType.NODE);
		drNodeWidth.getDocument().addDocumentListener(listener);
		drNodeHeight.getDocument().addDocumentListener(listener);
		drNodeRule.getDocument().addDocumentListener(listener);

		designRulesGetSelectedLayerLoadDRCToList();
	}

	/**
	 * Class to handle special changes to design rules.
	 */
    private enum DRCDocType {NODE, MINIMUM, SPACING}

	private static class DRCDocumentListener implements DocumentListener
	{
		private DesignRulesPanel frame;
        private JTextField field;
        private DRCDocType type;

		DRCDocumentListener(DesignRulesPanel dialog, JTextField text, DRCDocType type)
        {
            this.frame = dialog;
            this.field = text;
            this.type = type;
        }

        private void update()
        {
            switch (type)
            {
                case NODE:
                    frame.editChangedOnNodeRules(field);
                    break;
                case MINIMUM:
                    frame.editChangedOnMinRules(field);
                    break;
                case SPACING:
                    frame.designRulesEditChanged(field);
                    break;
            }
        }
		public void changedUpdate(DocumentEvent e) { update(); }
		public void insertUpdate(DocumentEvent e) { update(); }
		public void removeUpdate(DocumentEvent e) { update(); }
	}

    private int getLayerFromToIndex()
    {
        // get layer information
		int layer1 = designRulesGetSelectedLayer(designRulesFromList);
		if (layer1 < 0) return -1;
		int layer2 = designRulesGetSelectedLayer(designRulesToList);
		if (layer2 < 0) return -1;
        return (drRules.getRuleIndex(layer1, layer2));
    }

    /**
	 * Method called when the user changes any edit field handling node rules
     * @param field
     */
	private void editChangedOnNodeRules(JTextField field)
	{
        assert(drNodeWidth == field || drNodeHeight == field || drNodeRule == field);

		if (designRulesUpdating) return;

        designRulesUpdating = true;

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
        DRCTemplate tmp = new DRCTemplate(drNodeRule.getText(),
                DRCTemplate.DRCMode.ALL.mode(), DRCTemplate.DRCRuleType.NODSIZ,
                0, 0, null, null, width, -1);
        tmp.value2 = height;
        drRules.addRule(nodeIndex, tmp, DRCTemplate.DRCRuleType.NONE, false);

        designRulesUpdating = false;
    }

    /**
	 * Method called when the user changes any edit field.
     * @param field
     */
	private void editChangedOnMinRules(JTextField field)
	{
		if (designRulesUpdating) return;

        designRulesUpdating = true;

		// get layer information
		int layer1 = designRulesGetSelectedLayer(designRulesFromList);
		if (layer1 < 0) return;
		Layer layer = curTech.getLayer(layer1);

        String minSizeText, minSizeRuleName;
        double minSize;

        // width related fields touched
        if (field == drLayerWidth)
        {
            // pickup changes to layer minimum size rule
            minSizeText = drLayerWidth.getText().trim();
            minSize = TextUtils.atof(minSizeText);
            minSizeRuleName = drLayerWidthRule.getText().trim();
            if (minSizeText.length() > 0 && minSizeRuleName.length() > 0)
                drRules.setMinValue(layer, minSizeRuleName, minSize, DRCTemplate.DRCRuleType.MINWID);
        }
        else if (field == drLayerArea)
        {
            // pickup changes to layer min area rule
            minSizeText = drLayerArea.getText().trim();
            minSize = TextUtils.atof(minSizeText);
            minSizeRuleName = drLayerAreaRule.getText().trim();
            if (minSizeText.length() > 0)
                drRules.setMinValue(layer, minSizeRuleName, minSize, DRCTemplate.DRCRuleType.MINAREA);
        }
        else if (field == drLayerEnclosure)
        {
            // pickup changes to layer min enclose area rule
            minSizeText = drLayerEnclosure.getText().trim();
            minSize = TextUtils.atof(minSizeText);
            minSizeRuleName = drLayerEAreaRule.getText().trim();
            if (minSizeText.length() > 0)
                drRules.setMinValue(layer, minSizeRuleName, minSize, DRCTemplate.DRCRuleType.MINENCLOSEDAREA);
        }
        else
            System.out.println("Invalid field in editChangedOnMinRules");

        designRulesUpdating = false;
    }

	/**
	 * Method called when the user changes any edit field.
     * @param field
     */
	private void designRulesEditChanged(JTextField field)
	{
		if (designRulesUpdating) return;

        designRulesUpdating = true;

		// get layer information
		int layer1 = designRulesGetSelectedLayer(designRulesFromList);
		if (layer1 < 0) return;

        int dindex = getLayerFromToIndex();
        if (dindex == -1) return;

        List<DRCTemplate> list = new ArrayList<DRCTemplate>();
        double value;

        if (field == drNormalConnected)
        {
            // get new normal spacing values
            value = TextUtils.atof(drNormalConnected.getText());
            list.add(new DRCTemplate(drNormalConnectedRule.getText(), foundry.mode(), DRCTemplate.DRCRuleType.CONSPA,
                    0, 0, null, null, value, -1));
            drRules.setSpacingRules(dindex, list, DRCTemplate.DRCRuleType.SPACING, false);
        }
        else if (field == drNormalUnconnected)
        {
            value = TextUtils.atof(drNormalUnconnected.getText());
            list.add(new DRCTemplate(drNormalUnconnectedRule.getText(), foundry.mode(), DRCTemplate.DRCRuleType.UCONSPA,
                    0, 0, null, null, value, -1));
            drRules.setSpacingRules(dindex, list, DRCTemplate.DRCRuleType.SPACING, false);
        }
        else if (field == drMultiUnconnected)
        {
            value = TextUtils.atof(drMultiUnconnected.getText());
            list.add(new DRCTemplate(drMultiUnconnectedRule.getText(), foundry.mode(), DRCTemplate.DRCRuleType.UCONSPA2D,
                    0, 0, null, null, value, 1));
            drRules.setSpacingRules(dindex, list, DRCTemplate.DRCRuleType.UCONSPA2D, false);
        }
        else if (field == drNormalEdge)
        {
            // get new edge values
            value = TextUtils.atof(drNormalEdge.getText());
            list.add(new DRCTemplate(drNormalEdgeRule.getText(), foundry.mode(), DRCTemplate.DRCRuleType.CONSPA,
                    0, 0, null, null, value, -1));
            drRules.setSpacingRules(dindex, list, DRCTemplate.DRCRuleType.SPACINGE, false);
        }
        else if (field == drSpacings)
        {
            // get new wide spacing values
            int curWid = drSpacingsList.getSelectedIndex();
//            int curWid = drSpacingsList.getItemCount() - 1;
            if (curWid >= 0 && curWid < wideSpacingRules.size())
            {
                DRCTemplate wr = wideSpacingRules.get(curWid);
                double maxW = TextUtils.atof(drWidths.getText().trim());
                double minL = TextUtils.atof(drLengths.getText().trim());
                double minV = TextUtils.atof(drSpacings.getText().trim());
                String newName = drSpacingsRule.getText().trim();
                boolean diffW = (DBMath.isGreaterThan(maxW, 0) && !DBMath.areEquals(wr.maxWidth, maxW));
                boolean diffL = (DBMath.isGreaterThan(minL, 0) && !DBMath.areEquals(wr.minLength, minL));
                boolean diffV = (DBMath.isGreaterThan(minV, 0) && !DBMath.areEquals(wr.value1, minV));
                boolean diffN = !wr.ruleName.equals(newName);
                if (diffW || diffL || diffV || diffN) // they have to be > 0
                {
                    if (diffW)
                        wr.maxWidth = maxW;
                    if (diffL)
                        wr.minLength = minL;
                    if (diffV)
                        wr.value1 = minV;
                    if (diffN)
                        wr.ruleName = newName;
                    drRules.setSpacingRules(dindex, wideSpacingRules, DRCTemplate.DRCRuleType.SPACING, true);
                }
            }
        }
        designRulesUpdating = false;
    }

	/**
	 * Method to handle selection of a different node in the top scroll list.
	 */
	private void designRulesGetSelectedNode()
	{
		designRulesUpdating = true;
		int nodeIndex = designRulesNodeList.getSelectedIndex();
		DRCTemplate nr = drRules.getRule(nodeIndex, DRCTemplate.DRCRuleType.NODSIZ);

        drNodeWidth.setText("");
	    drNodeHeight.setText("");
		drNodeRule.setText("");
		if (nr != null)
		{
			drNodeWidth.setText(TextUtils.formatDouble(nr.value1));
			drNodeHeight.setText(TextUtils.formatDouble(nr.value2));
			drNodeRule.setText(nr.ruleName);
		}
		designRulesUpdating = false;
	}

	/**
	 * Method called when the wide rules popup changes.
	 */
	private void widePopupChanged(boolean delete)
	{
		int index = drSpacingsList.getSelectedIndex();
		if (index < 0 || index >= wideSpacingRules.size()) return;
		designRulesUpdating = true;
		DRCTemplate tmp = wideSpacingRules.get(index);
		drWidths.setText("");
		drLengths.setText("");
		drSpacingsRule.setText("");
		drSpacings.setText("");

        // Delete the wde rule
        if (delete)
        {
            drRules.deleteRule(index, tmp);
            wideSpacingRules.remove(tmp);
            drSpacingsList.removeItemAt(index);
			if (wideSpacingRules.size() != 0)
				drSpacingsList.setSelectedIndex(0);
        }
        else
        {
            if (tmp.maxWidth != 0) drWidths.setText(Double.toString(tmp.maxWidth));
            if (tmp.minLength != 0) drLengths.setText(Double.toString(tmp.minLength));
            drSpacings.setText(Double.toString(tmp.value1));
            drSpacingsRule.setText(tmp.ruleName);
        }
		designRulesUpdating = false;
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
                int dindex = drRules.getRuleIndex(j, i);
				String line = drMakeToListLine(dindex, i, onlyvalid);

				if (line.length() == 0) continue;
				designRulesToModel.addElement(line);
				count++;
			}
			if (count > 0) designRulesToList.setSelectedIndex(0);
		}

		// show minimum layer size
		Layer layer = curTech.getLayer(j);
        DRCTemplate lr = drRules.getMinValue(layer, DRCTemplate.DRCRuleType.MINWID);
        if (lr != null)
		{
			drLayerWidth.setText(TextUtils.formatDouble(lr.value1));
		    drLayerWidthRule.setText(lr.ruleName);
		} else
		{
			drLayerWidth.setText("");
		    drLayerWidthRule.setText("");
		}

        // Show min area
        lr = drRules.getMinValue(layer, DRCTemplate.DRCRuleType.MINAREA);
		if (lr != null)
		{
			drLayerArea.setText(TextUtils.formatDouble(lr.value1));
		    drLayerAreaRule.setText(lr.ruleName);
		} else
		{
			drLayerArea.setText("");
		    drLayerAreaRule.setText("");
		}

        // Show min enclosure area
        lr = drRules.getMinValue(layer, DRCTemplate.DRCRuleType.MINENCLOSEDAREA);
		if (lr != null)
		{
			drLayerEnclosure.setText(TextUtils.formatDouble(lr.value1));
		    drLayerEAreaRule.setText(lr.ruleName);
		} else
		{
			drLayerEnclosure.setText("");
		    drLayerEAreaRule.setText("");
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
		drMultiUnconnected.setText("");   drMultiUnconnectedRule.setText("");
		drNormalEdge.setText("");         drNormalEdgeRule.setText("");

        int dindex = getLayerFromToIndex();
		if (dindex != -1)
		{
//            double wideLimit = 0;
            List<DRCTemplate> spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.DRCRuleType.SPACING, false);
            for (DRCTemplate tmp : spacingRules)
            {
                if (tmp.ruleType == DRCTemplate.DRCRuleType.CONSPA)
                {
                    drNormalConnected.setText(Double.toString(tmp.value1));
                    drNormalConnectedRule.setText(tmp.ruleName);
                }
                else if (tmp.ruleType == DRCTemplate.DRCRuleType.UCONSPA)
                {
                    drNormalUnconnected.setText(Double.toString(tmp.value1));
                    drNormalUnconnectedRule.setText(tmp.ruleName);
                }
//                if (tmp.maxWidth > 0) wideLimit = tmp.maxWidth;
            }

			spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.DRCRuleType.SPACING, true);
            Collections.sort(spacingRules, DRCTemplate.templateSort);
			wideSpacingRules = new ArrayList<DRCTemplate>();
			drSpacingsList.removeAllItems();
            // Not iterator otherwise the order is lost
			for (DRCTemplate tmp : spacingRules)
			{
	            if (tmp.ruleType != DRCTemplate.DRCRuleType.UCONSPA) continue;
				wideSpacingRules.add(tmp);
                drSpacingsList.addItem("Rule " + wideSpacingRules.size());
			}

            // To stop the update of blank information
            designRulesUpdating = true;
            drWidths.setText("");
			drLengths.setText("");
			drSpacingsRule.setText("");
			drSpacings.setText("");
			designRulesUpdating = false;
			if (wideSpacingRules.size() != 0)
				drSpacingsList.setSelectedIndex(0);
			
            spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.DRCRuleType.UCONSPA2D, false);
            assert(spacingRules.size() <= 1);
            if (spacingRules.size() == 1)
            {
                DRCTemplate cutTmp = spacingRules.get(0);
                drMultiUnconnected.setText(Double.toString(cutTmp.value1));
                drMultiUnconnectedRule.setText(cutTmp.ruleName);
            }
            spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.DRCRuleType.SPACINGE, false);
            for (DRCTemplate tmp : spacingRules)
            {
                // Any is fine
                drNormalEdge.setText(Double.toString(tmp.value1));
                drNormalEdgeRule.setText(tmp.ruleName);
            }
            drAddRule.setEnabled(drRules.doesAllowMultipleWideRules(dindex));
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
		List<DRCTemplate> spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.DRCRuleType.SPACING, false);
        for (DRCTemplate tmp : spacingRules)
		{
			if (tmp.value1 > 0) gotRule = true;
		}

        spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.DRCRuleType.SPACING, true);
        for (DRCTemplate tmp : spacingRules)
		{
			if (tmp.value1 > 0) gotRule = true;
		}

        spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.DRCRuleType.UCONSPA2D, false);
        assert(spacingRules.size() <= 1);
        for (DRCTemplate tmp : spacingRules)
		{
			if (tmp.value1 > 0) gotRule = true;
		}

        spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.DRCRuleType.SPACINGE, false);
		for (DRCTemplate tmp : spacingRules)
		{
			if (tmp.value1 > 0) gotRule = true;
		}
		if (onlyValid && !gotRule) return "";
        String ret = curTech.getLayer(lindex).getName();
		return ret;
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Design Rules tab.
	 */
//	public void term()
//	{
//        // Getting last changes
//        designRulesEditChanged(null);
//	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        java.awt.GridBagConstraints gridBagConstraints;

        jSplitPane1 = new javax.swing.JSplitPane();
        bottom = new javax.swing.JPanel();
        drMultiUnconnectedRule = new javax.swing.JTextField();
        drMultiUnconnected = new javax.swing.JTextField();
        drNormalEdgeRule = new javax.swing.JTextField();
        drNormalEdge = new javax.swing.JTextField();
        drNormalUnconnected = new javax.swing.JTextField();
        drNormalConnectedRule = new javax.swing.JTextField();
        drNormalConnected = new javax.swing.JTextField();
        multiCutNameLabel = new javax.swing.JLabel();
        drNormalEdgeLabel = new javax.swing.JLabel();
        drNormalUnconnectedLabel = new javax.swing.JLabel();
        drNormalConnectedLabel = new javax.swing.JLabel();
        normalRuleLabel = new javax.swing.JLabel();
        normalValueLabel = new javax.swing.JLabel();
        normalNameLabel = new javax.swing.JLabel();
        drToList = new javax.swing.JScrollPane();
        drShowOnlyLinesWithRules = new javax.swing.JCheckBox();
        drFromList = new javax.swing.JScrollPane();
        jLabel1 = new javax.swing.JLabel();
        toLabel = new javax.swing.JLabel();
        drWidths = new javax.swing.JTextField();
        drLengths = new javax.swing.JTextField();
        drSpacings = new javax.swing.JTextField();
        drSpacingsRule = new javax.swing.JTextField();
        drLengthsLabel = new javax.swing.JLabel();
        drWidthsLabel = new javax.swing.JLabel();
        drSpacingsList = new javax.swing.JComboBox();
        multiSeparator = new javax.swing.JSeparator();
        wideSeparator = new javax.swing.JSeparator();
        drLayerWLabel = new javax.swing.JLabel();
        drLayerWidth = new javax.swing.JTextField();
        drLayerWidthRule = new javax.swing.JTextField();
        wideNameLabel = new javax.swing.JLabel();
        wideValueLabel = new javax.swing.JLabel();
        wideRuleLabel = new javax.swing.JLabel();
        multiCutValueLabel = new javax.swing.JLabel();
        multiCutRuleLabel = new javax.swing.JLabel();
        drAddRule = new javax.swing.JButton();
        drDeleteRule = new javax.swing.JButton();
        drLayerALabel = new javax.swing.JLabel();
        drLayerAreaRule = new javax.swing.JTextField();
        ruleLabel = new javax.swing.JLabel();
        drLayerArea = new javax.swing.JTextField();
        normalSeparator = new javax.swing.JSeparator();
        valueLabel = new javax.swing.JLabel();
        drLayerEALabel = new javax.swing.JLabel();
        drLayerEAreaRule = new javax.swing.JTextField();
        drLayerEnclosure = new javax.swing.JTextField();
        drNormalUnconnectedRule = new javax.swing.JTextField();
        top = new javax.swing.JPanel();
        drNodeList = new javax.swing.JScrollPane();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        drNodeWidth = new javax.swing.JTextField();
        drNodeRule = new javax.swing.JTextField();
        drNodeHeight = new javax.swing.JTextField();

        setLayout(new java.awt.GridBagLayout());

        setAlignmentX(0.0F);
        setAlignmentY(0.0F);
        setMinimumSize(new java.awt.Dimension(359, 556));
        setPreferredSize(new java.awt.Dimension(359, 556));
        bottom.setLayout(new java.awt.GridBagLayout());

        bottom.setBorder(javax.swing.BorderFactory.createTitledBorder("Layer Rules"));
        bottom.setPreferredSize(new java.awt.Dimension(359, 452));
        drMultiUnconnectedRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 4);
        bottom.add(drMultiUnconnectedRule, gridBagConstraints);

        drMultiUnconnected.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 0);
        bottom.add(drMultiUnconnected, gridBagConstraints);

        drNormalEdgeRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 4);
        bottom.add(drNormalEdgeRule, gridBagConstraints);

        drNormalEdge.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 0);
        bottom.add(drNormalEdge, gridBagConstraints);

        drNormalUnconnected.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        bottom.add(drNormalUnconnected, gridBagConstraints);

        drNormalConnectedRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 4);
        bottom.add(drNormalConnectedRule, gridBagConstraints);

        drNormalConnected.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
        bottom.add(drNormalConnected, gridBagConstraints);

        multiCutNameLabel.setText("Multiple via cuts:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(multiCutNameLabel, gridBagConstraints);

        drNormalEdgeLabel.setText("Edge:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 14, 4, 4);
        bottom.add(drNormalEdgeLabel, gridBagConstraints);

        drNormalUnconnectedLabel.setText("Not connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 14, 0, 4);
        bottom.add(drNormalUnconnectedLabel, gridBagConstraints);

        drNormalConnectedLabel.setText("When connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 0);
        bottom.add(drNormalConnectedLabel, gridBagConstraints);

        normalRuleLabel.setText("Rule");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(normalRuleLabel, gridBagConstraints);

        normalValueLabel.setText("Distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(normalValueLabel, gridBagConstraints);

        normalNameLabel.setText("Normal:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(normalNameLabel, gridBagConstraints);

        drToList.setOpaque(false);
        drToList.setPreferredSize(new java.awt.Dimension(100, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridheight = 13;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(drToList, gridBagConstraints);

        drShowOnlyLinesWithRules.setText("Show only \"to\" entries with rules");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(drShowOnlyLinesWithRules, gridBagConstraints);

        drFromList.setOpaque(false);
        drFromList.setPreferredSize(new java.awt.Dimension(100, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(drFromList, gridBagConstraints);

        jLabel1.setText("From Layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(jLabel1, gridBagConstraints);

        toLabel.setText("To Layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(toLabel, gridBagConstraints);

        drWidths.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(drWidths, gridBagConstraints);

        drLengths.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(drLengths, gridBagConstraints);

        drSpacings.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        bottom.add(drSpacings, gridBagConstraints);

        drSpacingsRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 4);
        bottom.add(drSpacingsRule, gridBagConstraints);

        drLengthsLabel.setText("and Length >");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(drLengthsLabel, gridBagConstraints);

        drWidthsLabel.setText("If Width >");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(drWidthsLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        bottom.add(drSpacingsList, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        bottom.add(multiSeparator, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        bottom.add(wideSeparator, gridBagConstraints);

        drLayerWLabel.setText("Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 14, 0, 0);
        bottom.add(drLayerWLabel, gridBagConstraints);

        drLayerWidth.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        bottom.add(drLayerWidth, gridBagConstraints);

        drLayerWidthRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        bottom.add(drLayerWidthRule, gridBagConstraints);

        wideNameLabel.setText("Wide rules:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(wideNameLabel, gridBagConstraints);

        wideValueLabel.setText("Distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(wideValueLabel, gridBagConstraints);

        wideRuleLabel.setText("Rule");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(wideRuleLabel, gridBagConstraints);

        multiCutValueLabel.setText("Distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(multiCutValueLabel, gridBagConstraints);

        multiCutRuleLabel.setText("Rule");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(multiCutRuleLabel, gridBagConstraints);

        drAddRule.setText("Add Wide Rule");
        drAddRule.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                drAddRuleActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom.add(drAddRule, gridBagConstraints);

        drDeleteRule.setText("Delete Wide Rule");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom.add(drDeleteRule, gridBagConstraints);

        drLayerALabel.setText("Area:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 14, 0, 4);
        bottom.add(drLayerALabel, gridBagConstraints);

        drLayerAreaRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        bottom.add(drLayerAreaRule, gridBagConstraints);

        ruleLabel.setText("Rule");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(ruleLabel, gridBagConstraints);

        drLayerArea.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        bottom.add(drLayerArea, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        bottom.add(normalSeparator, gridBagConstraints);

        valueLabel.setText("Min. Value");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(valueLabel, gridBagConstraints);

        drLayerEALabel.setText("Enclosure Area:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 14, 0, 4);
        bottom.add(drLayerEALabel, gridBagConstraints);

        drLayerEAreaRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        bottom.add(drLayerEAreaRule, gridBagConstraints);

        drLayerEnclosure.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        bottom.add(drLayerEnclosure, gridBagConstraints);

        drNormalUnconnectedRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        bottom.add(drNormalUnconnectedRule, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        add(bottom, gridBagConstraints);

        top.setLayout(new java.awt.GridBagLayout());

        top.setBorder(javax.swing.BorderFactory.createTitledBorder("Node Rules"));
        top.setAlignmentX(0.0F);
        top.setAlignmentY(0.0F);
        top.setPreferredSize(new java.awt.Dimension(167, 104));
        drNodeList.setOpaque(false);
        drNodeList.setPreferredSize(new java.awt.Dimension(200, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.5;
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

        jLabel7.setText("Min. Size");
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
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.25;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drNodeWidth, gridBagConstraints);

        drNodeRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.25;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drNodeRule, gridBagConstraints);

        drNodeHeight.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drNodeHeight, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        add(top, gridBagConstraints);

    }// </editor-fold>//GEN-END:initComponents

    private void drDeleteRuleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drDeleteRuleActionPerformed
        widePopupChanged(true);
        drAddRule.setEnabled(drRules.doesAllowMultipleWideRules(getLayerFromToIndex()));
    }//GEN-LAST:event_drDeleteRuleActionPerformed

    private void drAddRuleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drAddRuleActionPerformed
        int dindex = getLayerFromToIndex();
        if (dindex == -1) return;
		int soFar = drSpacingsList.getItemCount();

        double maxW = TextUtils.atof(drWidths.getText().trim());
        double minLen = TextUtils.atof(drLengths.getText().trim());
        double value = TextUtils.atof(drSpacings.getText());
        String ruleText = drSpacingsRule.getText();
        if (ruleText.length() > 0 && value > 0 && (maxW > 0 || minLen > 0))
        {
		    drSpacingsList.addItem("Rule " + (soFar+1));
		    DRCTemplate wr = new DRCTemplate(drSpacingsRule.getText(), foundry.mode(), DRCTemplate.DRCRuleType.CONSPA,
                    maxW, minLen, null, null, value, -1);
            drRules.addRule(dindex, wr, DRCTemplate.DRCRuleType.SPACING, true);
            wideSpacingRules.add(wr);
            // to be consistent, now adding the unconnected one
            wr.ruleType = DRCTemplate.DRCRuleType.UCONSPA;
            drRules.addRule(dindex, wr, DRCTemplate.DRCRuleType.SPACING, true);
        }
        else
            soFar = 0; // reset to first element ;
        if (wideSpacingRules.size() > 0) drSpacingsList.setSelectedIndex(soFar);
        drAddRule.setEnabled(drRules.doesAllowMultipleWideRules(getLayerFromToIndex()));
    }//GEN-LAST:event_drAddRuleActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel bottom;
    private javax.swing.JButton drAddRule;
    private javax.swing.JButton drDeleteRule;
    private javax.swing.JScrollPane drFromList;
    private javax.swing.JLabel drLayerALabel;
    private javax.swing.JTextField drLayerArea;
    private javax.swing.JTextField drLayerAreaRule;
    private javax.swing.JLabel drLayerEALabel;
    private javax.swing.JTextField drLayerEAreaRule;
    private javax.swing.JTextField drLayerEnclosure;
    private javax.swing.JLabel drLayerWLabel;
    private javax.swing.JTextField drLayerWidth;
    private javax.swing.JTextField drLayerWidthRule;
    private javax.swing.JTextField drLengths;
    private javax.swing.JLabel drLengthsLabel;
    private javax.swing.JTextField drMultiUnconnected;
    private javax.swing.JTextField drMultiUnconnectedRule;
    private javax.swing.JTextField drNodeHeight;
    private javax.swing.JScrollPane drNodeList;
    private javax.swing.JTextField drNodeRule;
    private javax.swing.JTextField drNodeWidth;
    private javax.swing.JTextField drNormalConnected;
    private javax.swing.JLabel drNormalConnectedLabel;
    private javax.swing.JTextField drNormalConnectedRule;
    private javax.swing.JTextField drNormalEdge;
    private javax.swing.JLabel drNormalEdgeLabel;
    private javax.swing.JTextField drNormalEdgeRule;
    private javax.swing.JTextField drNormalUnconnected;
    private javax.swing.JLabel drNormalUnconnectedLabel;
    private javax.swing.JTextField drNormalUnconnectedRule;
    private javax.swing.JCheckBox drShowOnlyLinesWithRules;
    private javax.swing.JTextField drSpacings;
    private javax.swing.JComboBox drSpacingsList;
    private javax.swing.JTextField drSpacingsRule;
    private javax.swing.JScrollPane drToList;
    private javax.swing.JTextField drWidths;
    private javax.swing.JLabel drWidthsLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JLabel multiCutNameLabel;
    private javax.swing.JLabel multiCutRuleLabel;
    private javax.swing.JLabel multiCutValueLabel;
    private javax.swing.JSeparator multiSeparator;
    private javax.swing.JLabel normalNameLabel;
    private javax.swing.JLabel normalRuleLabel;
    private javax.swing.JSeparator normalSeparator;
    private javax.swing.JLabel normalValueLabel;
    private javax.swing.JLabel ruleLabel;
    private javax.swing.JLabel toLabel;
    private javax.swing.JPanel top;
    private javax.swing.JLabel valueLabel;
    private javax.swing.JLabel wideNameLabel;
    private javax.swing.JLabel wideRuleLabel;
    private javax.swing.JSeparator wideSeparator;
    private javax.swing.JLabel wideValueLabel;
    // End of variables declaration//GEN-END:variables

}
