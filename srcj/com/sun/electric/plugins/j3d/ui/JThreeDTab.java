/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ThreeDTab.java
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
package com.sun.electric.plugins.j3d.ui;

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.options.ThreeDTab;
import com.sun.electric.plugins.j3d.utils.J3DAppearance;
import com.sun.electric.plugins.j3d.View3DWindow;
import com.sun.electric.plugins.j3d.utils.J3DAppearance;

import java.awt.GridBagConstraints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.media.j3d.TransparencyAttributes;

/**
 * Class to handle the "3D" tab of the Preferences dialog.
 * @author  Gilda Garreton
 * @version 0.1
 */
public class JThreeDTab extends ThreeDTab
{
	/** Creates new form ThreeDTab */
	public JThreeDTab(java.awt.Frame parent, Boolean modal)
	{
		super(parent, modal.booleanValue());
		initComponents();
	}

	public JPanel getPanel() { return threeD; }

	private boolean initial3DTextChanging = false;
	protected JList threeDLayerList;
    private JTextField scaleField, rotXField, rotYField;
	private DefaultListModel threeDLayerModel;
	public HashMap threeDThicknessMap, threeDDistanceMap, transparencyMap;
	private JThreeDSideView threeDSideView;
    private static final HashMap modeMap = new HashMap(5);

    // Filling the data
    static {
        J3DTransparencyOption option = new J3DTransparencyOption(TransparencyAttributes.FASTEST, "FASTEST");
        modeMap.put(option, option);
        option = new J3DTransparencyOption(TransparencyAttributes.NICEST, "NICEST");
        modeMap.put(option, option);
        option = new J3DTransparencyOption(TransparencyAttributes.BLENDED, "BLENDED");
        modeMap.put(option, option);
        option = new J3DTransparencyOption(TransparencyAttributes.SCREEN_DOOR, "SCREEN_DOOR");
        modeMap.put(option, option);
        option = new J3DTransparencyOption(TransparencyAttributes.NONE, "NONE");
        modeMap.put(option, option);
    }

    /**
     * Inner class to control options based on attribute integers
     */
    private static class J3DTransparencyOption
    {
        int mode;
        String name;
        J3DTransparencyOption(int mode, String name)
        {
            this.mode = mode;
            this.name = name;
        }
        public String toString() {return name;}
        // Careful with toString
        /**
         * Overwriting original function to use mode integer values
         * for hash numbers
         * @return
         */
        public int hashCode() {return mode;}
    }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the 3D tab.
	 */
	public void init()
	{
		threeDTechnology.setText("Layer cross section for technology '" + curTech.getTechName() + "'");
		threeDLayerModel = new DefaultListModel();
		threeDLayerList = new JList(threeDLayerModel);
		threeDLayerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		threeDLayerPane.setViewportView(threeDLayerList);
		threeDLayerList.clearSelection();
		threeDLayerList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { threeDValuesChanged(false); }
		});
		threeDThicknessMap = new HashMap();
		threeDDistanceMap = new HashMap();
        transparencyMap = new HashMap();
        // Sorted by Height to be consistent with LayersTab
		for(Iterator it = curTech.getLayersSortedByHeight().iterator(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;
			threeDLayerModel.addElement(layer.getName());
			threeDThicknessMap.put(layer, new GenMath.MutableDouble(layer.getThickness()));
			threeDDistanceMap.put(layer, new GenMath.MutableDouble(layer.getDistance()));
            // Get a copy of JAppearance to set values temporarily
            // this function will generate JAppearance if doesn't exist yet
            J3DAppearance app = J3DAppearance.getAppearance(layer.getGraphics());
            transparencyMap.put(layer, new J3DAppearance(app));

		}
		threeDLayerList.setSelectedIndex(0);
		threeDHeight.getDocument().addDocumentListener(new ThreeDInfoDocumentListener(this));
		threeDThickness.getDocument().addDocumentListener(new ThreeDInfoDocumentListener(this));
        // Transparency data
        transparancyField.getDocument().addDocumentListener(new ThreeDInfoDocumentListener(this));

		threeDSideView = new JThreeDSideView(this);
		threeDSideView.setMinimumSize(new java.awt.Dimension(200, 450));
		threeDSideView.setPreferredSize(new java.awt.Dimension(200, 450));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 2;       gbc.gridy = 1;
		gbc.gridwidth = 2;   gbc.gridheight = 1;
		//gbc.weightx = 0.5;   gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new java.awt.Insets(4, 4, 4, 4);
		threeD.add(threeDSideView, gbc);

        // Z value
        JLabel scaleLabel = new javax.swing.JLabel("Z Scale:");
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 6;
        gbc.insets = new java.awt.Insets(4, 4, 4, 4);
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        threeD.add(scaleLabel, gbc);

        scaleField = new javax.swing.JTextField();
        scaleField.setColumns(6);
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 6;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        threeD.add(scaleField, gbc);
        scaleField.setText(TextUtils.formatDouble(User.get3DFactor()));

        // Default rotation X
        JLabel rotXLabel = new javax.swing.JLabel("Rotation X:");
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 7;
        gbc.insets = new java.awt.Insets(4, 4, 4, 4);
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        threeD.add(rotXLabel, gbc);

        rotXField = new javax.swing.JTextField();
        rotXField.setColumns(6);
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        threeD.add(rotXField, gbc);
        rotXField.setText(TextUtils.formatDouble(User.get3DRotX()));

        // Default rotation Y
        JLabel rotYLabel = new javax.swing.JLabel("Rotation Y:");
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 8;
        gbc.insets = new java.awt.Insets(4, 4, 4, 4);
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        threeD.add(rotYLabel, gbc);

        rotYField = new javax.swing.JTextField();
        rotYField.setColumns(6);
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 8;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        threeD.add(rotYField, gbc);
        rotYField.setText(TextUtils.formatDouble(User.get3DRotY()));

		threeDPerspective.setSelected(User.is3DPerspective());
        // to turn on antialising if available. No by default because of performance.
        threeDAntialiasing.setSelected(User.is3DAntialiasing());

        threeDZoom.setText(TextUtils.formatDouble(User.get3DOrigZoom()));

        for (Iterator it = modeMap.keySet().iterator(); it.hasNext();)
        {
            J3DTransparencyOption op = (J3DTransparencyOption)it.next();
            transparencyMode.addItem(op);
        }
        // Add listener after creating the list
        transparencyMode.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { threeDValuesChanged(true); }
        });

        // Setting the initial values
		threeDValuesChanged(false);
	}

    /**
	 * Class to handle changes to the thickness or height.
	 */
	private static class ThreeDInfoDocumentListener implements DocumentListener
	{
		JThreeDTab dialog;

		ThreeDInfoDocumentListener(JThreeDTab dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.threeDValuesChanged(true); }
		public void insertUpdate(DocumentEvent e) { dialog.threeDValuesChanged(true); }
		public void removeUpdate(DocumentEvent e) { dialog.threeDValuesChanged(true); }
	}

	private void threeDValuesChanged(boolean set)
	{
		String layerName = (String)threeDLayerList.getSelectedValue();
		Layer layer = curTech.findLayer(layerName);
		if (layer == null) return;
        processDataInFields(layer, set);
	}

    /**
     * To process data in fields either from layer list or from
     * object picked.
     */
    public void processDataInFields(Layer layer, boolean set)
    {
        if (!set) initial3DTextChanging = true;
        else if (initial3DTextChanging) return;
		GenMath.MutableDouble thickness = (GenMath.MutableDouble)threeDThicknessMap.get(layer);
		GenMath.MutableDouble height = (GenMath.MutableDouble)threeDDistanceMap.get(layer);
        J3DAppearance app = (J3DAppearance)transparencyMap.get(layer);
        TransparencyAttributes ta = app.getTransparencyAttributes();
        if (set)
        {
            thickness.setValue(TextUtils.atof(threeDThickness.getText()));
            height.setValue(TextUtils.atof(threeDHeight.getText()));
            ta.setTransparency((float)TextUtils.atof(transparancyField.getText()));
            J3DTransparencyOption op = (J3DTransparencyOption)transparencyMode.getSelectedItem();
            ta.setTransparencyMode(op.mode);
            threeDSideView.updateZValues(layer, thickness.doubleValue(), height.doubleValue());
        }
        else
        {
            threeDHeight.setText(TextUtils.formatDouble(height.doubleValue()));
            threeDThickness.setText(TextUtils.formatDouble(thickness.doubleValue()));
            transparancyField.setText(TextUtils.formatDouble(ta.getTransparency()));
            for (Iterator it = modeMap.keySet().iterator(); it.hasNext();)
            {
                J3DTransparencyOption op = (J3DTransparencyOption)it.next();
                if (op.mode == ta.getTransparencyMode())
                {
                    transparencyMode.setSelectedItem(op);
                    break;  // found
                }
            }
        }
        if (!set) initial3DTextChanging = false;
        threeDSideView.showLayer(layer);
    }

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the 3D tab.
	 */
	public void term()
	{
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;
			GenMath.MutableDouble thickness = (GenMath.MutableDouble)threeDThicknessMap.get(layer);
			GenMath.MutableDouble height = (GenMath.MutableDouble)threeDDistanceMap.get(layer);
            J3DAppearance newApp = (J3DAppearance)transparencyMap.get(layer);
            J3DAppearance oldApp = (J3DAppearance)layer.getGraphics().get3DAppearance();
            oldApp.setTransparencyAttributes(newApp.getTransparencyAttributes());
			if (thickness.doubleValue() != layer.getThickness())
				layer.setThickness(thickness.doubleValue());
			if (height.doubleValue() != layer.getDistance())
				layer.setDistance(height.doubleValue());
		}

		boolean currentPerspective = threeDPerspective.isSelected();
		if (currentPerspective != User.is3DPerspective())
			User.set3DPerspective(currentPerspective);

        boolean currentAntialiasing = threeDAntialiasing.isSelected();
		if (currentAntialiasing != User.is3DAntialiasing())
		{
            View3DWindow.setAntialiasing(currentAntialiasing);
			User.set3DAntialiasing(currentAntialiasing);
		}

        double currentValue = TextUtils.atof(scaleField.getText());
        if (currentValue != User.get3DFactor())
        {
            View3DWindow.setScaleFactor(currentValue);
            User.set3DFactor(currentValue);
        }

        currentValue = TextUtils.atof(rotXField.getText());
        if (currentValue != User.get3DRotX())
            User.set3DRotX(currentValue);

        currentValue = TextUtils.atof(rotYField.getText());
        if (currentValue != User.get3DRotY())
            User.set3DRotY(currentValue);
        currentValue = TextUtils.atof(threeDZoom.getText());
        if (GenMath.doublesEqual(currentValue, 0))
            System.out.println(currentValue + " is an invalid zoom factor.");
        else if (currentValue != User.get3DOrigZoom())
            User.set3DOrigZoom(currentValue);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        threeD = new javax.swing.JPanel();
        threeDTechnology = new javax.swing.JLabel();
        threeDLayerPane = new javax.swing.JScrollPane();
        jLabel45 = new javax.swing.JLabel();
        jLabel47 = new javax.swing.JLabel();
        threeDThickness = new javax.swing.JTextField();
        threeDHeight = new javax.swing.JTextField();
        threeDPerspective = new javax.swing.JCheckBox();
        threeDAntialiasing = new javax.swing.JCheckBox();
        jLabel48 = new javax.swing.JLabel();
        threeDZoom = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        transparencyMode = new javax.swing.JComboBox();
        transparancyField = new javax.swing.JTextField();
        transparencyLabel = new javax.swing.JLabel();
        transparencyModeLabel = new javax.swing.JLabel();
        separator = new javax.swing.JSeparator();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        threeD.setLayout(new java.awt.GridBagLayout());

        threeDTechnology.setText("Layer cross section for technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(threeDTechnology, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(threeDLayerPane, gridBagConstraints);

        jLabel45.setText("Thickness:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(jLabel45, gridBagConstraints);

        jLabel47.setText("Distance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(jLabel47, gridBagConstraints);

        threeDThickness.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(threeDThickness, gridBagConstraints);

        threeDHeight.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(threeDHeight, gridBagConstraints);

        threeDPerspective.setText("Use Perspective");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(threeDPerspective, gridBagConstraints);

        threeDAntialiasing.setText("Use Antialiasing");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(threeDAntialiasing, gridBagConstraints);

        jLabel48.setText("Orig. Zoom:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(jLabel48, gridBagConstraints);

        threeDZoom.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(threeDZoom, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(new javax.swing.border.TitledBorder("Transparency Options"));
        transparencyMode.setToolTipText("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(transparencyMode, gridBagConstraints);

        transparancyField.setMinimumSize(new java.awt.Dimension(20, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(transparancyField, gridBagConstraints);

        transparencyLabel.setText("Factor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(transparencyLabel, gridBagConstraints);

        transparencyModeLabel.setText("Mode:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        jPanel1.add(transparencyModeLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        threeD.add(jPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        threeD.add(separator, gridBagConstraints);

        getContentPane().add(threeD, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator separator;
    private javax.swing.JPanel threeD;
    private javax.swing.JCheckBox threeDAntialiasing;
    private javax.swing.JTextField threeDHeight;
    private javax.swing.JScrollPane threeDLayerPane;
    private javax.swing.JCheckBox threeDPerspective;
    private javax.swing.JLabel threeDTechnology;
    private javax.swing.JTextField threeDThickness;
    private javax.swing.JTextField threeDZoom;
    private javax.swing.JTextField transparancyField;
    private javax.swing.JLabel transparencyLabel;
    private javax.swing.JComboBox transparencyMode;
    private javax.swing.JLabel transparencyModeLabel;
    // End of variables declaration//GEN-END:variables

}
