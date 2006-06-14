/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JThreeDTab.java
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
import com.sun.electric.tool.user.dialogs.options.ThreeDTab;
import com.sun.electric.plugins.j3d.utils.J3DAppearance;
import com.sun.electric.plugins.j3d.View3DWindow;
import com.sun.electric.plugins.j3d.utils.J3DUtils;

import java.awt.GridBagConstraints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Vector3f;

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
	private DefaultListModel threeDLayerModel;
	public HashMap<Layer,GenMath.MutableDouble> threeDThicknessMap, threeDDistanceMap;
    public HashMap<Layer,J3DAppearance> transparencyMap;
	private JThreeDSideView threeDSideView;
    private static final HashMap<J3DTransparencyOption,J3DTransparencyOption> modeMap = new HashMap<J3DTransparencyOption,J3DTransparencyOption>(5);

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
		threeDThicknessMap = new HashMap<Layer,GenMath.MutableDouble>();
		threeDDistanceMap = new HashMap<Layer,GenMath.MutableDouble>();
        transparencyMap = new HashMap<Layer,J3DAppearance>();
        // Sorted by Height to be consistent with LayersTab
		for(Layer layer : curTech.getLayersSortedByHeight())
		{
			if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;
			threeDLayerModel.addElement(layer.getName());
			threeDThicknessMap.put(layer, new GenMath.MutableDouble(layer.getThickness()));
			threeDDistanceMap.put(layer, new GenMath.MutableDouble(layer.getDistance()));
            // Get a copy of JAppearance to set values temporarily
            // this function will generate JAppearance if doesn't exist yet
            J3DAppearance app = J3DAppearance.getAppearance(layer.getGraphics());
            // forcing visibility
            J3DAppearance newApp = new J3DAppearance(app);
            newApp.getRenderingAttributes().setVisible(true);
            transparencyMap.put(layer, newApp);

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

        scaleField.setText(TextUtils.formatDouble(J3DUtils.get3DFactor()));
        double[] rot = GenMath.transformVectorIntoValues(J3DUtils.get3DRotation());
        xRotField.setText(TextUtils.formatDouble(rot[0]));
        yRotField.setText(TextUtils.formatDouble(rot[1]));
        zRotField.setText(TextUtils.formatDouble(rot[2]));

		threeDPerspective.setSelected(J3DUtils.is3DPerspective());
        // to turn on antialising if available. No by default because of performance.
        threeDAntialiasing.setSelected(J3DUtils.is3DAntialiasing());
        threeDZoom.setText(TextUtils.formatDouble(J3DUtils.get3DOrigZoom()));
        threeDCellBnd.setSelected(J3DUtils.is3DCellBndOn());
        threeDAxes.setSelected(J3DUtils.is3DAxesOn());
        maxNodeField.setText(String.valueOf(J3DUtils.get3DMaxNumNodes()));
        alphaField.setText(String.valueOf(J3DUtils.get3DAlpha()));

        for (J3DTransparencyOption op : modeMap.keySet())
        {
            transparencyMode.addItem(op);
        }
        // Add listener after creating the list
        transparencyMode.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { threeDValuesChanged(true); }
        });

        // Light boxes
        String lights = J3DUtils.get3DLightDirs();
        Vector3f[] dirs = J3DUtils.transformIntoVectors(lights);
        dirOneBox.setSelected(dirs[0] != null);
        if (dirOneBox.isSelected())
        {
            xDirOneField.setText(String.valueOf(dirs[0].x));
            yDirOneField.setText(String.valueOf(dirs[0].y));
            zDirOneField.setText(String.valueOf(dirs[0].z));
        }
        dirTwoBox.setSelected(dirs[1] != null);
        if (dirTwoBox.isSelected())
        {
            xDirTwoField.setText(String.valueOf(dirs[1].x));
            yDirTwoField.setText(String.valueOf(dirs[1].y));
            zDirTwoField.setText(String.valueOf(dirs[1].z));
        }

        // Setting the initial values
		threeDValuesChanged(false);
        dirOneBoxStateChanged(null);
        dirTwoBoxStateChanged(null);
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
		GenMath.MutableDouble thickness = threeDThicknessMap.get(layer);
		GenMath.MutableDouble height = threeDDistanceMap.get(layer);
        J3DAppearance app = transparencyMap.get(layer);
        TransparencyAttributes ta = app.getTransparencyAttributes();
        if (set)
        {
            thickness.setValue(TextUtils.atof(threeDThickness.getText()));
            height.setValue(TextUtils.atof(threeDHeight.getText()));
            ta.setTransparency((float)TextUtils.atof(transparancyField.getText()));
            J3DTransparencyOption op = (J3DTransparencyOption)transparencyMode.getSelectedItem();
            ta.setTransparencyMode(op.mode);
            app.getRenderingAttributes().setDepthBufferEnable(op.mode != TransparencyAttributes.NONE);
            threeDSideView.updateZValues(layer, thickness.doubleValue(), height.doubleValue());
        }
        else
        {
            threeDHeight.setText(TextUtils.formatDouble(height.doubleValue()));
            threeDThickness.setText(TextUtils.formatDouble(thickness.doubleValue()));
            transparancyField.setText(TextUtils.formatDouble(ta.getTransparency()));
            for (J3DTransparencyOption op : modeMap.keySet())
            {
                if (op.mode == ta.getTransparencyMode())
                {
                    transparencyMode.setSelectedItem(op);
                    break;  // found
                }
            }
        threeDSideView.showLayer(layer);
        }
        if (!set) initial3DTextChanging = false;
    }

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the 3D tab.
	 */
	public void term()
	{
		for(Iterator<Layer> it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
			if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;
			GenMath.MutableDouble thickness = threeDThicknessMap.get(layer);
			GenMath.MutableDouble height = threeDDistanceMap.get(layer);
            J3DAppearance newApp = transparencyMap.get(layer);
            J3DAppearance oldApp = (J3DAppearance)layer.getGraphics().get3DAppearance();
            oldApp.setTransparencyAndRenderingAttributes(newApp.getTransparencyAttributes(), newApp.getRenderingAttributes().getDepthBufferEnable());
			if (thickness.doubleValue() != layer.getThickness())
				layer.setThickness(thickness.doubleValue());
			if (height.doubleValue() != layer.getDistance())
				layer.setDistance(height.doubleValue());
		}

		boolean currentBoolean = threeDPerspective.isSelected();
		if (currentBoolean != J3DUtils.is3DPerspective())
			J3DUtils.set3DPerspective(currentBoolean);

        currentBoolean = threeDAntialiasing.isSelected();
		if (currentBoolean != J3DUtils.is3DAntialiasing())
		{
            View3DWindow.setAntialiasing(currentBoolean);
			J3DUtils.set3DAntialiasing(currentBoolean);
		}
        currentBoolean = threeDCellBnd.isSelected();
		if (currentBoolean != J3DUtils.is3DCellBndOn())
		{
            J3DAppearance.cellApp.getRenderingAttributes().setVisible(currentBoolean);
			J3DUtils.set3DCellBndOn(currentBoolean);
		}
        currentBoolean = threeDAxes.isSelected();
		if (currentBoolean != J3DUtils.is3DAxesOn())
		{
            J3DAppearance.axisApps[0].getRenderingAttributes().setVisible(currentBoolean);
            J3DAppearance.axisApps[1].getRenderingAttributes().setVisible(currentBoolean);
            J3DAppearance.axisApps[2].getRenderingAttributes().setVisible(currentBoolean);
			J3DUtils.set3DAxesOn(currentBoolean);
		}

        double currentValue = TextUtils.atof(scaleField.getText());
        if (currentValue != J3DUtils.get3DFactor())
        {
            View3DWindow.setScaleFactor(currentValue);
            J3DUtils.set3DFactor(currentValue);
        }

        String rotationValue = "(" +
                xRotField.getText() + " " +
                yRotField.getText() + " " +
                zRotField.getText() + ")";
        if (!rotationValue.equals(J3DUtils.get3DRotation()))
            J3DUtils.set3DRotation(rotationValue);

        currentValue = TextUtils.atof(threeDZoom.getText());
        if (GenMath.doublesEqual(currentValue, 0))
            System.out.println(currentValue + " is an invalid zoom factor.");
        else if (currentValue != J3DUtils.get3DOrigZoom())
            J3DUtils.set3DOrigZoom(currentValue);

        StringBuffer dir = new StringBuffer();
        if (dirOneBox.isSelected())
            dir.append(GenMath.transformStringsIntoVector(TextUtils.atof(xDirOneField.getText()),
                    TextUtils.atof(yDirOneField.getText()),
                    TextUtils.atof(zDirOneField.getText())));
        else
            dir.append(GenMath.transformStringsIntoVector(0, 0, 0));
        if (dirTwoBox.isSelected())
            dir.append(GenMath.transformStringsIntoVector(TextUtils.atof(xDirTwoField.getText()),
                    TextUtils.atof(yDirTwoField.getText()),
                    TextUtils.atof(zDirTwoField.getText())));
        else
            dir.append(GenMath.transformStringsIntoVector(0, 0, 0));
        
        if (!dir.equals(J3DUtils.get3DLightDirs()))
            J3DUtils.set3DLightDirs(dir.toString());
        int currentInt = TextUtils.atoi(maxNodeField.getText());
        if (currentInt != J3DUtils.get3DMaxNumNodes())
            J3DUtils.set3DMaxNumNodes(currentInt);
        currentInt = TextUtils.atoi(alphaField.getText());
        if (currentInt != J3DUtils.get3DAlpha())
            J3DUtils.set3DAlpha(currentInt);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        threeD = new javax.swing.JPanel();
        threeDTechnology = new javax.swing.JLabel();
        threeDLayerPane = new javax.swing.JScrollPane();
        thickLabel = new javax.swing.JLabel();
        distanceLabel = new javax.swing.JLabel();
        threeDThickness = new javax.swing.JTextField();
        threeDHeight = new javax.swing.JTextField();
        threeDPerspective = new javax.swing.JCheckBox();
        threeDAntialiasing = new javax.swing.JCheckBox();
        transparencyPanel = new javax.swing.JPanel();
        transparencyMode = new javax.swing.JComboBox();
        transparancyField = new javax.swing.JTextField();
        transparencyLabel = new javax.swing.JLabel();
        transparencyModeLabel = new javax.swing.JLabel();
        separator = new javax.swing.JSeparator();
        directionPanel = new javax.swing.JPanel();
        dirOneBox = new javax.swing.JCheckBox();
        dirTwoBox = new javax.swing.JCheckBox();
        dirOnePanel = new javax.swing.JPanel();
        xDirOne = new javax.swing.JLabel();
        yDirOne = new javax.swing.JLabel();
        zDirOne = new javax.swing.JLabel();
        xDirOneField = new javax.swing.JTextField();
        yDirOneField = new javax.swing.JTextField();
        zDirOneField = new javax.swing.JTextField();
        dirTwoPanel = new javax.swing.JPanel();
        xDirTwo = new javax.swing.JLabel();
        yDirTwo = new javax.swing.JLabel();
        zDirTwo = new javax.swing.JLabel();
        xDirTwoField = new javax.swing.JTextField();
        yDirTwoField = new javax.swing.JTextField();
        zDirTwoField = new javax.swing.JTextField();
        threeDCellBnd = new javax.swing.JCheckBox();
        initialViewPanel = new javax.swing.JPanel();
        xRotLabel = new javax.swing.JLabel();
        xRotField = new javax.swing.JTextField();
        yRotField = new javax.swing.JTextField();
        yRotLabel = new javax.swing.JLabel();
        threeDZoom = new javax.swing.JTextField();
        initZoomLabel = new javax.swing.JLabel();
        zRotField = new javax.swing.JTextField();
        zRotLabel = new javax.swing.JLabel();
        scaleField = new javax.swing.JTextField();
        scaleLabel = new javax.swing.JLabel();
        threeDAxes = new javax.swing.JCheckBox();
        maxNodeLabel = new javax.swing.JLabel();
        maxNodeField = new javax.swing.JTextField();
        alphaLabel = new javax.swing.JLabel();
        alphaField = new javax.swing.JTextField();

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

        thickLabel.setText("Thickness:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(thickLabel, gridBagConstraints);

        distanceLabel.setText("Distance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(distanceLabel, gridBagConstraints);

        threeDThickness.setColumns(6);
        threeDThickness.setMinimumSize(new java.awt.Dimension(70, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(threeDThickness, gridBagConstraints);

        threeDHeight.setColumns(6);
        threeDHeight.setMinimumSize(new java.awt.Dimension(70, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(threeDHeight, gridBagConstraints);

        threeDPerspective.setText("Use Perspective");
        threeDPerspective.setToolTipText("Perspective or Parallel View");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(threeDPerspective, gridBagConstraints);

        threeDAntialiasing.setText("Use Antialiasing");
        threeDAntialiasing.setToolTipText("Turn on Antialiasing if available");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(threeDAntialiasing, gridBagConstraints);

        transparencyPanel.setLayout(new java.awt.GridBagLayout());

        transparencyPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Transparency Options"));
        transparencyMode.setToolTipText("Java3D transparency model");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        transparencyPanel.add(transparencyMode, gridBagConstraints);

        transparancyField.setToolTipText("Transparency alpha factor (0 is opaque)");
        transparancyField.setMinimumSize(new java.awt.Dimension(20, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        transparencyPanel.add(transparancyField, gridBagConstraints);

        transparencyLabel.setText("Factor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        transparencyPanel.add(transparencyLabel, gridBagConstraints);

        transparencyModeLabel.setText("Mode:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        transparencyPanel.add(transparencyModeLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        threeD.add(transparencyPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        threeD.add(separator, gridBagConstraints);

        directionPanel.setLayout(new java.awt.GridBagLayout());

        directionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Light Information"));
        dirOneBox.setSelected(true);
        dirOneBox.setText("Enable Light 1");
        dirOneBox.setToolTipText("Direction of first environment light");
        dirOneBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                dirOneBoxStateChanged(evt);
            }
        });

        directionPanel.add(dirOneBox, new java.awt.GridBagConstraints());

        dirTwoBox.setText("Enable Light 2");
        dirTwoBox.setToolTipText("Direction of second environment light");
        dirTwoBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                dirTwoBoxStateChanged(evt);
            }
        });

        directionPanel.add(dirTwoBox, new java.awt.GridBagConstraints());

        dirOnePanel.setLayout(new java.awt.GridBagLayout());

        xDirOne.setText("X:");
        dirOnePanel.add(xDirOne, new java.awt.GridBagConstraints());

        yDirOne.setText("Y:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        dirOnePanel.add(yDirOne, gridBagConstraints);

        zDirOne.setText("Z:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        dirOnePanel.add(zDirOne, gridBagConstraints);

        xDirOneField.setText(null);
        xDirOneField.setMinimumSize(new java.awt.Dimension(50, 21));
        xDirOneField.setPreferredSize(new java.awt.Dimension(50, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        dirOnePanel.add(xDirOneField, gridBagConstraints);

        yDirOneField.setText(null);
        yDirOneField.setMinimumSize(new java.awt.Dimension(50, 21));
        yDirOneField.setPreferredSize(new java.awt.Dimension(50, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        dirOnePanel.add(yDirOneField, gridBagConstraints);

        zDirOneField.setText(null);
        zDirOneField.setMinimumSize(new java.awt.Dimension(50, 21));
        zDirOneField.setPreferredSize(new java.awt.Dimension(50, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        dirOnePanel.add(zDirOneField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        directionPanel.add(dirOnePanel, gridBagConstraints);

        dirTwoPanel.setLayout(new java.awt.GridBagLayout());

        dirTwoPanel.setEnabled(false);
        xDirTwo.setText("X:");
        dirTwoPanel.add(xDirTwo, new java.awt.GridBagConstraints());

        yDirTwo.setText("Y:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        dirTwoPanel.add(yDirTwo, gridBagConstraints);

        zDirTwo.setText("Z:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        dirTwoPanel.add(zDirTwo, gridBagConstraints);

        xDirTwoField.setText(null);
        xDirTwoField.setMinimumSize(new java.awt.Dimension(50, 21));
        xDirTwoField.setPreferredSize(new java.awt.Dimension(50, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        dirTwoPanel.add(xDirTwoField, gridBagConstraints);

        yDirTwoField.setText(null);
        yDirTwoField.setMinimumSize(new java.awt.Dimension(50, 21));
        yDirTwoField.setPreferredSize(new java.awt.Dimension(50, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        dirTwoPanel.add(yDirTwoField, gridBagConstraints);

        zDirTwoField.setText(null);
        zDirTwoField.setMinimumSize(new java.awt.Dimension(50, 21));
        zDirTwoField.setPreferredSize(new java.awt.Dimension(50, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        dirTwoPanel.add(zDirTwoField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        directionPanel.add(dirTwoPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(directionPanel, gridBagConstraints);

        threeDCellBnd.setText("Cell  Bounding Box");
        threeDCellBnd.setToolTipText("Display cell bounding box");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(threeDCellBnd, gridBagConstraints);

        initialViewPanel.setLayout(new java.awt.GridBagLayout());

        initialViewPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Initial Transformation"));
        xRotLabel.setText("Rotation X:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        initialViewPanel.add(xRotLabel, gridBagConstraints);

        xRotField.setColumns(6);
        xRotField.setToolTipText("X component of rotation vector");
        xRotField.setMinimumSize(new java.awt.Dimension(70, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        initialViewPanel.add(xRotField, gridBagConstraints);

        yRotField.setColumns(6);
        yRotField.setToolTipText("Y component of rotation vector");
        yRotField.setMinimumSize(new java.awt.Dimension(70, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        initialViewPanel.add(yRotField, gridBagConstraints);

        yRotLabel.setText("Rotation Y:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        initialViewPanel.add(yRotLabel, gridBagConstraints);

        threeDZoom.setColumns(6);
        threeDZoom.setToolTipText("Initial zoom in case of JMouseZoom behavior");
        threeDZoom.setMinimumSize(new java.awt.Dimension(70, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        initialViewPanel.add(threeDZoom, gridBagConstraints);

        initZoomLabel.setText("Initial Zoom:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        initialViewPanel.add(initZoomLabel, gridBagConstraints);

        zRotField.setColumns(6);
        zRotField.setToolTipText("Z component of rotation vector");
        zRotField.setMinimumSize(new java.awt.Dimension(70, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        initialViewPanel.add(zRotField, gridBagConstraints);

        zRotLabel.setText("Rotation Z:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        initialViewPanel.add(zRotLabel, gridBagConstraints);

        scaleField.setColumns(6);
        scaleField.setToolTipText("Scale along Z to shrink or expand elements");
        scaleField.setMinimumSize(new java.awt.Dimension(70, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        initialViewPanel.add(scaleField, gridBagConstraints);

        scaleLabel.setText("Z Scale:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        initialViewPanel.add(scaleLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(initialViewPanel, gridBagConstraints);

        threeDAxes.setSelected(true);
        threeDAxes.setText("Show Axes");
        threeDAxes.setToolTipText("Turn on Axes if Java3D plugin is available");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(threeDAxes, gridBagConstraints);

        maxNodeLabel.setText("Max. # Nodes:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(maxNodeLabel, gridBagConstraints);

        maxNodeField.setColumns(6);
        maxNodeField.setToolTipText("Recommended maximum number of nodes in scene graph");
        maxNodeField.setMinimumSize(new java.awt.Dimension(70, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(maxNodeField, gridBagConstraints);

        alphaLabel.setText("Alpha:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(alphaLabel, gridBagConstraints);

        alphaField.setColumns(6);
        alphaField.setToolTipText("Alpha speed value for demos");
        alphaField.setMinimumSize(new java.awt.Dimension(70, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(alphaField, gridBagConstraints);

        getContentPane().add(threeD, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void dirTwoBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_dirTwoBoxStateChanged
        dirTwoPanel.setVisible(dirTwoBox.isSelected());
        //xDirTwo.setEnabled(dirTwoBox.isSelected());
    }//GEN-LAST:event_dirTwoBoxStateChanged

    private void dirOneBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_dirOneBoxStateChanged
        dirOnePanel.setVisible(dirOneBox.isSelected());
    }//GEN-LAST:event_dirOneBoxStateChanged

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField alphaField;
    private javax.swing.JLabel alphaLabel;
    private javax.swing.JCheckBox dirOneBox;
    private javax.swing.JPanel dirOnePanel;
    private javax.swing.JCheckBox dirTwoBox;
    private javax.swing.JPanel dirTwoPanel;
    private javax.swing.JPanel directionPanel;
    private javax.swing.JLabel distanceLabel;
    private javax.swing.JLabel initZoomLabel;
    private javax.swing.JPanel initialViewPanel;
    private javax.swing.JTextField maxNodeField;
    private javax.swing.JLabel maxNodeLabel;
    private javax.swing.JTextField scaleField;
    private javax.swing.JLabel scaleLabel;
    private javax.swing.JSeparator separator;
    private javax.swing.JLabel thickLabel;
    private javax.swing.JPanel threeD;
    private javax.swing.JCheckBox threeDAntialiasing;
    private javax.swing.JCheckBox threeDAxes;
    private javax.swing.JCheckBox threeDCellBnd;
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
    private javax.swing.JPanel transparencyPanel;
    private javax.swing.JLabel xDirOne;
    private javax.swing.JTextField xDirOneField;
    private javax.swing.JLabel xDirTwo;
    private javax.swing.JTextField xDirTwoField;
    private javax.swing.JTextField xRotField;
    private javax.swing.JLabel xRotLabel;
    private javax.swing.JLabel yDirOne;
    private javax.swing.JTextField yDirOneField;
    private javax.swing.JLabel yDirTwo;
    private javax.swing.JTextField yDirTwoField;
    private javax.swing.JTextField yRotField;
    private javax.swing.JLabel yRotLabel;
    private javax.swing.JLabel zDirOne;
    private javax.swing.JTextField zDirOneField;
    private javax.swing.JLabel zDirTwo;
    private javax.swing.JTextField zDirTwoField;
    private javax.swing.JTextField zRotField;
    private javax.swing.JLabel zRotLabel;
    // End of variables declaration//GEN-END:variables

}
