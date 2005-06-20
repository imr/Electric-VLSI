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
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * Class to handle the "Design Rules" tab of the Preferences dialog.
 */
public class DesignRulesTab extends PreferencePanel
{
	private static final int MAXSPACINGRULES = 3;

	/** Creates new form DesignRulesTab */
	public DesignRulesTab(Frame parent, boolean modal)
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
	private JTextField [] widths, lengths, spacings, lSpacingRules;

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
			drWidth1.setEnabled(false);
			drWidth2.setEnabled(false);
			drWidth3.setEnabled(false);
			drLength1.setEnabled(false);
			drLength2.setEnabled(false);
			drLength3.setEnabled(false);
			drLayerSpacing1.setEnabled(false);
			drLayerSpacing2.setEnabled(false);
			drLayerSpacing3.setEnabled(false);
			drDefSpacing.setEnabled(false);
			drMultiConnected.setEnabled(false);
			drMultiConnectedRule.setEnabled(false);
			drMultiUnconnected.setEnabled(false);
			drMultiUnconnectedRule.setEnabled(false);
			factoryReset.setEnabled(false);
			return;
		}

		widths = new JTextField[MAXSPACINGRULES];
		widths[0] = drWidth1;           widths[1] = drWidth2;           widths[2] = drWidth3;
		lengths = new JTextField[MAXSPACINGRULES];
		lengths[0] = drLength1;         lengths[1] = drLength2;         lengths[2] = drLength3;
		spacings = new JTextField[MAXSPACINGRULES];
		spacings[0] = drLayerSpacing1;  spacings[1] = drLayerSpacing2;  spacings[2] = drLayerSpacing3;
		lSpacingRules = new JTextField[MAXSPACINGRULES];
		lSpacingRules[0] = drLayerRule1; lSpacingRules[1] = drLayerRule2; lSpacingRules[2] = drLayerRule3;

        drRules = rules;

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

		// have changes to edit fields get detected immediately
		DRCDocumentListener myDocumentListener = new DRCDocumentListener(this);
		drNormalConnected.getDocument().addDocumentListener(myDocumentListener);
		drNormalConnectedRule.getDocument().addDocumentListener(myDocumentListener);
		drNormalUnconnected.getDocument().addDocumentListener(myDocumentListener);
		drNormalUnconnectedRule.getDocument().addDocumentListener(myDocumentListener);
		drNormalEdge.getDocument().addDocumentListener(myDocumentListener);
		drNormalEdgeRule.getDocument().addDocumentListener(myDocumentListener);
		drWidth1.getDocument().addDocumentListener(myDocumentListener);
		drWidth2.getDocument().addDocumentListener(myDocumentListener);
		drWidth3.getDocument().addDocumentListener(myDocumentListener);
		drLength1.getDocument().addDocumentListener(myDocumentListener);
		drLength2.getDocument().addDocumentListener(myDocumentListener);
		drLength3.getDocument().addDocumentListener(myDocumentListener);
		drLayerSpacing1.getDocument().addDocumentListener(myDocumentListener);
		drLayerSpacing2.getDocument().addDocumentListener(myDocumentListener);
		drLayerSpacing3.getDocument().addDocumentListener(myDocumentListener);
		drMultiConnected.getDocument().addDocumentListener(myDocumentListener);
		drMultiConnectedRule.getDocument().addDocumentListener(myDocumentListener);
		drMultiUnconnected.getDocument().addDocumentListener(myDocumentListener);
		drMultiUnconnectedRule.getDocument().addDocumentListener(myDocumentListener);

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
		drTechName.setText("Design Rules for Technology '" + curTech.getTechName() + "'");
		designRulesSetupForLayersOrNodes();
		designRulesGetSelectedLayerLoadDRCToList();

        // Foundry
        for (Iterator it = curTech.getFactories(); it.hasNext(); )
        {
            Technology.Foundry factory = (Technology.Foundry)it.next();
            defaultFoundryPulldown.addItem(factory.name);
        }
        defaultFoundryPulldown.setSelectedItem(curTech.getSelectedFoundry());

        // Resolution
		drcResolutionValue.setText(TextUtils.formatDouble(curTech.getResolution()));
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

		// get layer information
		int layer1 = designRulesGetSelectedLayer(designRulesFromList);
		if (layer1 < 0) return;
		int layer2 = designRulesGetSelectedLayer(designRulesToList);
		if (layer2 < 0) return;
        int dindex = curTech.getRuleIndex(layer1, layer2);

		// get new width limit
        List list = new ArrayList(7);
        int techMode = curTech.getFoundry();

		// get new normal spacing values
		double value = TextUtils.atof(drNormalConnected.getText());
        list.add(new DRCTemplate(drNormalConnectedRule.getText(), techMode, DRCTemplate.CONSPA,
                0, 0, null, null, value, false));
		value = TextUtils.atof(drNormalUnconnected.getText());
        list.add(new DRCTemplate(drNormalUnconnectedRule.getText(), techMode, DRCTemplate.UCONSPA,
                0, 0, null, null, value, false));
        drRules.setSpacingRules(dindex, list, DRCTemplate.SPACING);

        // get new wide spacing values
        list.clear();
		for(int i=0; i<MAXSPACINGRULES; i++)
		{
			value = TextUtils.atof(spacings[i].getText());
			if (value == 0) break;
			double width = TextUtils.atof(widths[i].getText());
			double length = TextUtils.atof(lengths[i].getText());
	        list.add(new DRCTemplate(lSpacingRules[i].getText(), techMode, DRCTemplate.CONSPA,
				width, length, null, null, value, false));
		}
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
	}

	private void designRulesSetupForLayersOrNodes()
	{
		designRulesFromModel.clear();
		designRulesUpdating = true;

		// list the layers
        for(int i=0; i<designRulesValidLayers.length; i++)
		{
			if (!designRulesValidLayers[i]) continue;
            designRulesFromModel.addElement(curTech.getLayer(i).getName());
		}

		designRulesFromList.setSelectedIndex(0);
		designRulesUpdating = false;
	}

	/**
	 * Method to show the detail on the selected layer/node in the upper scroll area.
	 * Also called when the user clicks on the "show only lines with rules" checkbox.
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
		designRulesUpdating = false;
		designRulesShowSelectedLayerRules();

        int techMode = curTech.getFoundry();
        int dindex = curTech.getRuleIndex(j, j);
		List spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.SPACINGW, techMode);
		int rulesFilled = 0;
		for (int i = 0; i < spacingRules.size(); i++)
		{
			DRCRules.DRCRule tmp = (DRCRules.DRCRule)spacingRules.get(i);
            if (tmp.type != DRCTemplate.CONSPA) continue;
			if (rulesFilled >= MAXSPACINGRULES) break;
			if (tmp.maxWidth == 0) widths[rulesFilled].setText(""); else
				widths[rulesFilled].setText(Double.toString(tmp.maxWidth));
			if (tmp.minLength == 0) lengths[rulesFilled].setText(""); else
				lengths[rulesFilled].setText(Double.toString(tmp.minLength));
			spacings[rulesFilled].setText(Double.toString(tmp.value));
			lSpacingRules[rulesFilled].setText(tmp.ruleName);
			rulesFilled++;
		}
		for (int i = rulesFilled; i < MAXSPACINGRULES; i++)
		{
			widths[i].setText("");
			lengths[i].setText("");
			spacings[i].setText("");
			lSpacingRules[i].setText("");
		}

		// fill in the default
		drDefSpacing.setText("");
		spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.SPACING, techMode);
		for (int i = 0; i < spacingRules.size(); i++)
		{
			DRCRules.DRCRule tmp = (DRCRules.DRCRule)spacingRules.get(i);
            if (tmp.type != DRCTemplate.CONSPA) continue;
			drDefSpacing.setText(Double.toString(tmp.value));
			break;
		}
	}

	private String drMakeToListLine(int dindex, int lindex, boolean onlyValid)
	{
		String conDist = "";
		String unConDist = "";
        int techMode = curTech.getFoundry();
        List spacingRules = drRules.getSpacingRules(dindex, DRCTemplate.SPACING, techMode);
        for (int i = 0; i < spacingRules.size(); i++)
        {
            DRCRules.DRCRule tmp = (DRCRules.DRCRule)spacingRules.get(i);
            if (tmp.type == DRCTemplate.CONSPA) conDist = Double.toString(tmp.value);
            else if (tmp.type == DRCTemplate.UCONSPA) unConDist = Double.toString(tmp.value);
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
			if (conDist.length() == 0 && unConDist.length() == 0 &&
				conDistMulti.length() == 0 && unConDistMulti.length() == 0 && edgeDist.length() == 0)
					return "";
		}
        String ret = curTech.getLayer(lindex).getName() + " (" +
			conDist + "/" + unConDist + "/" + conDistMulti + "/" +
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

		// show layer information
		designRulesUpdating = true;
		drNormalConnected.setText("");    drNormalConnectedRule.setText("");
		drNormalUnconnected.setText("");  drNormalUnconnectedRule.setText("");
		drMultiConnected.setText("");     drMultiConnectedRule.setText("");
		drMultiUnconnected.setText("");   drMultiUnconnectedRule.setText("");
		drNormalEdge.setText("");         drNormalEdgeRule.setText("");

		int layer1 = designRulesGetSelectedLayer(designRulesFromList);
		int layer2 = designRulesGetSelectedLayer(designRulesToList);
        int techMode = curTech.getFoundry();

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

        String foundryName = (String)defaultFoundryPulldown.getSelectedItem();
        if (!foundryName.equals(curTech.getSelectedFoundry()))
            curTech.setSelectedFoundry(foundryName);

		double currentResolution = TextUtils.atof(drcResolutionValue.getText());
		if (currentResolution != curTech.getResolution())
			curTech.setResolution(currentResolution);
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
        top = new javax.swing.JPanel();
        drLayerSpacing2 = new javax.swing.JTextField();
        drLength2 = new javax.swing.JTextField();
        drWidth2 = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        drLayerSpacing1 = new javax.swing.JTextField();
        drLength1 = new javax.swing.JTextField();
        drWidth1 = new javax.swing.JTextField();
        drFromList = new javax.swing.JScrollPane();
        drWidth3 = new javax.swing.JTextField();
        drLength3 = new javax.swing.JTextField();
        drLayerSpacing3 = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        drLayerRule1 = new javax.swing.JTextField();
        drLayerRule2 = new javax.swing.JTextField();
        drLayerRule3 = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        drDefSpacing = new javax.swing.JLabel();
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
        drTechName = new javax.swing.JLabel();
        defaultFoundryLabel = new javax.swing.JLabel();
        defaultFoundryPulldown = new javax.swing.JComboBox();
        drcResolutionLabel = new javax.swing.JLabel();
        drcResolutionValue = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
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

        top.setLayout(new java.awt.GridBagLayout());

        top.setBorder(new javax.swing.border.TitledBorder("From Layer"));
        drLayerSpacing2.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drLayerSpacing2, gridBagConstraints);

        drLength2.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drLength2, gridBagConstraints);

        drWidth2.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drWidth2, gridBagConstraints);

        jLabel5.setText("Spacing rules for From layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(jLabel5, gridBagConstraints);

        jLabel4.setText("Min. Spacing");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(jLabel4, gridBagConstraints);

        jLabel3.setText("Length >");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(jLabel3, gridBagConstraints);

        jLabel2.setText("Width >");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(jLabel2, gridBagConstraints);

        drLayerSpacing1.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drLayerSpacing1, gridBagConstraints);

        drLength1.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drLength1, gridBagConstraints);

        drWidth1.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drWidth1, gridBagConstraints);

        drFromList.setPreferredSize(new java.awt.Dimension(75, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        top.add(drFromList, gridBagConstraints);

        drWidth3.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drWidth3, gridBagConstraints);

        drLength3.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drLength3, gridBagConstraints);

        drLayerSpacing3.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drLayerSpacing3, gridBagConstraints);

        jLabel7.setText("Default spacing:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(jLabel7, gridBagConstraints);

        drLayerRule1.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drLayerRule1, gridBagConstraints);

        drLayerRule2.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drLayerRule2, gridBagConstraints);

        drLayerRule3.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drLayerRule3, gridBagConstraints);

        jLabel8.setText("Rule");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        top.add(jLabel8, gridBagConstraints);

        drDefSpacing.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(drDefSpacing, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        designRules.add(top, gridBagConstraints);

        bottom.setLayout(new java.awt.GridBagLayout());

        bottom.setBorder(new javax.swing.border.TitledBorder("To Layer"));
        drMultiUnconnectedRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        bottom.add(drMultiUnconnectedRule, gridBagConstraints);

        drMultiUnconnected.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        bottom.add(drMultiUnconnected, gridBagConstraints);

        drMultiConnectedRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        bottom.add(drMultiConnectedRule, gridBagConstraints);

        drMultiConnected.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        bottom.add(drMultiConnected, gridBagConstraints);

        drNormalEdgeRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        bottom.add(drNormalEdgeRule, gridBagConstraints);

        drNormalEdge.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        bottom.add(drNormalEdge, gridBagConstraints);

        drNormalUnconnectedRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        bottom.add(drNormalUnconnectedRule, gridBagConstraints);

        drNormalUnconnected.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        bottom.add(drNormalUnconnected, gridBagConstraints);

        drNormalConnectedRule.setColumns(9);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        bottom.add(drNormalConnectedRule, gridBagConstraints);

        drNormalConnected.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        bottom.add(drNormalConnected, gridBagConstraints);

        jLabel53.setText("Not connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 0);
        bottom.add(jLabel53, gridBagConstraints);

        jLabel52.setText("When connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 0);
        bottom.add(jLabel52, gridBagConstraints);

        jLabel51.setText("Multiple cuts:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        bottom.add(jLabel51, gridBagConstraints);

        jLabel47.setText("Edge:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 0);
        bottom.add(jLabel47, gridBagConstraints);

        jLabel46.setText("Not connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 0);
        bottom.add(jLabel46, gridBagConstraints);

        jLabel45.setText("When connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 14, 4, 0);
        bottom.add(jLabel45, gridBagConstraints);

        jLabel44.setText("Rule");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        bottom.add(jLabel44, gridBagConstraints);

        jLabel43.setText("Distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        bottom.add(jLabel43, gridBagConstraints);

        jLabel42.setText("Normal:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        bottom.add(jLabel42, gridBagConstraints);

        drToList.setPreferredSize(new java.awt.Dimension(150, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        bottom.add(drToList, gridBagConstraints);

        drShowOnlyLinesWithRules.setText("Show only lines with rules");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        bottom.add(drShowOnlyLinesWithRules, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        designRules.add(bottom, gridBagConstraints);

        drTechName.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
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

        drcResolutionLabel.setText("Minimum resolution:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(drcResolutionLabel, gridBagConstraints);

        drcResolutionValue.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(drcResolutionValue, gridBagConstraints);

        jLabel6.setText("(use 0 to ignore resolution check)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(jLabel6, gridBagConstraints);

        factoryReset.setText("Factory Reset");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
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
    private javax.swing.JLabel drDefSpacing;
    private javax.swing.JScrollPane drFromList;
    private javax.swing.ButtonGroup drLayerOrNode;
    private javax.swing.JTextField drLayerRule1;
    private javax.swing.JTextField drLayerRule2;
    private javax.swing.JTextField drLayerRule3;
    private javax.swing.JTextField drLayerSpacing1;
    private javax.swing.JTextField drLayerSpacing2;
    private javax.swing.JTextField drLayerSpacing3;
    private javax.swing.JTextField drLength1;
    private javax.swing.JTextField drLength2;
    private javax.swing.JTextField drLength3;
    private javax.swing.JTextField drMultiConnected;
    private javax.swing.JTextField drMultiConnectedRule;
    private javax.swing.JTextField drMultiUnconnected;
    private javax.swing.JTextField drMultiUnconnectedRule;
    private javax.swing.JTextField drNormalConnected;
    private javax.swing.JTextField drNormalConnectedRule;
    private javax.swing.JTextField drNormalEdge;
    private javax.swing.JTextField drNormalEdgeRule;
    private javax.swing.JTextField drNormalUnconnected;
    private javax.swing.JTextField drNormalUnconnectedRule;
    private javax.swing.JCheckBox drShowOnlyLinesWithRules;
    private javax.swing.JLabel drTechName;
    private javax.swing.JScrollPane drToList;
    private javax.swing.JTextField drWidth1;
    private javax.swing.JTextField drWidth2;
    private javax.swing.JTextField drWidth3;
    private javax.swing.JLabel drcResolutionLabel;
    private javax.swing.JTextField drcResolutionValue;
    private javax.swing.JButton factoryReset;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel top;
    // End of variables declaration//GEN-END:variables

}
