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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.lang.reflect.Method;
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

/**
 * Class to handle the "3D" tab of the Preferences dialog.
 */
public class ThreeDTab extends PreferencePanel
{
	/** Main class for 3D plugin */	                    private static final Class view3DClass = Resources.get3DMainClass();
    /** Set Antialiasing method */                       private static Method set3DClass = null;

	/** Creates new form ThreeDTab */
	public ThreeDTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	public JPanel getPanel() { return threeD; }

	public String getName() { return "3D"; }

	private boolean initial3DTextChanging = false;
	private JList threeDLayerList;
    private JTextField scaleField;
	private DefaultListModel threeDLayerModel;
	private HashMap threeDThicknessMap, threeDDistanceMap;
	private JPanel threeDSideView;

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
			public void mouseClicked(MouseEvent evt) { threeDClickedLayer(); }
		});
		threeDThicknessMap = new HashMap();
		threeDDistanceMap = new HashMap();
        // Sorted by Height to be consistent with LayersTab
		for(Iterator it = curTech.getLayersSortedByHeight().iterator(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;
			threeDLayerModel.addElement(layer.getName());
			threeDThicknessMap.put(layer, new GenMath.MutableDouble(layer.getThickness()));
			threeDDistanceMap.put(layer, new GenMath.MutableDouble(layer.getDistance()));
		}
		threeDLayerList.setSelectedIndex(0);
		threeDHeight.getDocument().addDocumentListener(new ThreeDInfoDocumentListener(this));
		threeDThickness.getDocument().addDocumentListener(new ThreeDInfoDocumentListener(this));

		threeDSideView = new ThreeDSideView(this);
		threeDSideView.setMinimumSize(new java.awt.Dimension(200, 450));
		threeDSideView.setPreferredSize(new java.awt.Dimension(200, 450));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 2;       gbc.gridy = 1;
		gbc.gridwidth = 2;   gbc.gridheight = 4;
		gbc.weightx = 0.5;   gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new java.awt.Insets(4, 4, 4, 4);
		threeD.add(threeDSideView, gbc);

        JLabel scaleLabel = new javax.swing.JLabel("Z Scale:");
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 5;
        gbc.insets = new java.awt.Insets(4, 4, 4, 4);
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        threeD.add(scaleLabel, gbc);

        scaleField = new javax.swing.JTextField();
        scaleField.setColumns(6);
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 5;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        threeD.add(scaleField, gbc);
        scaleField.setText(TextUtils.formatDouble(User.get3DFactor()));


		threeDClickedLayer();

		threeDPerspective.setSelected(User.is3DPerspective());
        // to turn on antialising if available. No by default because of performance.
        threeDAntialiasing.setSelected(User.is3DAntialiasing());
	}

	private class ThreeDSideView extends JPanel
		implements MouseMotionListener, MouseListener
	{
		ThreeDTab dialog;
		double lowHeight = Double.MAX_VALUE, highHeight = Double.MIN_VALUE;

		ThreeDSideView(ThreeDTab dialog)
		{
			this.dialog = dialog;
			addMouseListener(this);
			addMouseMotionListener(this);

			for(Iterator it = dialog.curTech.getLayers(); it.hasNext(); )
			{
				Layer layer = (Layer)it.next();
				if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;
				if (!layer.isVisible()) continue;
				GenMath.MutableDouble thickness = (GenMath.MutableDouble)dialog.threeDThicknessMap.get(layer);
				GenMath.MutableDouble distance = (GenMath.MutableDouble)dialog.threeDDistanceMap.get(layer);
				double dis = distance.doubleValue();
				double thick = thickness.doubleValue() / 2;
				double valLow = dis - thick;
				double valHig = dis + thick;

				if (valLow < lowHeight)
					lowHeight = valLow;
				if (valHig > highHeight)
					highHeight = valHig;
			}
			lowHeight -= 4;
			highHeight += 4;
		}

		/**
		 * Method to repaint this ThreeDSideView.
		 */
		public void paint(Graphics g)
		{
			Dimension dim = getSize();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, dim.width, dim.height);
			g.setColor(Color.BLACK);
			g.drawLine(0, 0, 0, dim.height-1);
			g.drawLine(0, dim.height-1, dim.width-1, dim.height-1);
			g.drawLine(dim.width-1, dim.height-1, dim.width-1, 0);
			g.drawLine(dim.width-1, 0, 0, 0);

			String layerName = (String)dialog.threeDLayerList.getSelectedValue();
			Layer selectedLayer = dialog.curTech.findLayer(layerName);
			for(Iterator it = dialog.curTech.getLayers(); it.hasNext(); )
			{
				Layer layer = (Layer)it.next();
				if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;
				if (!layer.isVisible()) continue;
				if (layer == selectedLayer) g.setColor(Color.RED); else
					g.setColor(Color.BLACK);
				GenMath.MutableDouble thickness = (GenMath.MutableDouble)dialog.threeDThicknessMap.get(layer);
				GenMath.MutableDouble distance = (GenMath.MutableDouble)dialog.threeDDistanceMap.get(layer);
				double dis = distance.doubleValue() + thickness.doubleValue()/2;
				int yValue = dim.height - (int)((dis - lowHeight) / (highHeight - lowHeight) * dim.height + 0.5);
				int yHeight = (int)(thickness.doubleValue() / (highHeight - lowHeight) * dim.height + 0.5);
				if (yHeight == 0)
				{
					g.drawLine(0, yValue, dim.width/3, yValue);
				} else
				{
					//yHeight -= 4;
					int firstPart = dim.width / 6;
					int pointPos = dim.width / 4;
					g.drawLine(0, yValue-yHeight/2, firstPart, yValue-yHeight/2);
					g.drawLine(0, yValue+yHeight/2, firstPart, yValue+yHeight/2);
					g.drawLine(firstPart, yValue-yHeight/2, pointPos, yValue);
					g.drawLine(firstPart, yValue+yHeight/2, pointPos, yValue);
					g.drawLine(pointPos, yValue, dim.width/3, yValue);
				}
				String string = layer.getName();
				Font font = new Font(User.getDefaultFont(), Font.PLAIN, 9);
				g.setFont(font);
				FontRenderContext frc = new FontRenderContext(null, true, true);
				GlyphVector gv = font.createGlyphVector(frc, string);
				LineMetrics lm = font.getLineMetrics(string, frc);
				double txtHeight = lm.getHeight();
				Graphics2D g2 = (Graphics2D)g;
				g2.drawGlyphVector(gv, dim.width/3 + 1, (float)(yValue + txtHeight/2) - lm.getDescent());
			}
		}

		// the MouseListener events
		public void mousePressed(MouseEvent evt)
		{
			Dimension dim = getSize();
			String layerName = (String)dialog.threeDLayerList.getSelectedValue();
			Layer selectedLayer = dialog.curTech.findLayer(layerName);
			GenMath.MutableDouble height = (GenMath.MutableDouble)dialog.threeDDistanceMap.get(selectedLayer);
			int yValue = dim.height - (int)((height.doubleValue() - lowHeight) / (highHeight - lowHeight) * dim.height + 0.5);
			if (Math.abs(yValue - evt.getY()) > 5)
			{
				int bestDist = dim.height;
				for(Iterator it = dialog.curTech.getLayers(); it.hasNext(); )
				{
					Layer layer = (Layer)it.next();
					if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;
					height = (GenMath.MutableDouble)dialog.threeDDistanceMap.get(layer);
					yValue = dim.height - (int)((height.doubleValue() - lowHeight) / (highHeight - lowHeight) * dim.height + 0.5);
					int dist = Math.abs(yValue - evt.getY());
					if (dist < bestDist)
					{
						bestDist = dist;
						selectedLayer = layer;
					}
				}
				dialog.threeDLayerList.setSelectedValue(selectedLayer.getName(), true);
				dialog.threeDClickedLayer();
			}
		}
		public void mouseReleased(MouseEvent evt) {}
		public void mouseClicked(MouseEvent evt) {}
		public void mouseEntered(MouseEvent evt) {}
		public void mouseExited(MouseEvent evt) {}

		// the MouseMotionListener events
		public void mouseMoved(MouseEvent evt) {}
		public void mouseDragged(MouseEvent evt)
		{
			Dimension dim = getSize();
			String layerName = (String)dialog.threeDLayerList.getSelectedValue();
			Layer layer = dialog.curTech.findLayer(layerName);
			GenMath.MutableDouble height = (GenMath.MutableDouble)threeDDistanceMap.get(layer);
			double newHeight = (double)(dim.height - evt.getY()) / dim.height * (highHeight - lowHeight) + lowHeight;
			if (height.doubleValue() != newHeight)
			{
				height.setValue(newHeight);
				dialog.threeDHeight.setText(TextUtils.formatDouble(newHeight));
				repaint();
			}
		}
	}

	/**
	 * Class to handle changes to the thickness or height.
	 */
	private static class ThreeDInfoDocumentListener implements DocumentListener
	{
		ThreeDTab dialog;

		ThreeDInfoDocumentListener(ThreeDTab dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.threeDValuesChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.threeDValuesChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.threeDValuesChanged(); }
	}

	private void threeDValuesChanged()
	{
		if (initial3DTextChanging) return;
		String layerName = (String)threeDLayerList.getSelectedValue();
		Layer layer = curTech.findLayer(layerName);
		if (layer == null) return;
		GenMath.MutableDouble thickness = (GenMath.MutableDouble)threeDThicknessMap.get(layer);
		GenMath.MutableDouble height = (GenMath.MutableDouble)threeDDistanceMap.get(layer);
		thickness.setValue(TextUtils.atof(threeDThickness.getText()));
		height.setValue(TextUtils.atof(threeDHeight.getText()));
		threeDSideView.repaint();
	}

	private void threeDClickedLayer()
	{
		initial3DTextChanging = true;
		String layerName = (String)threeDLayerList.getSelectedValue();
		Layer layer = curTech.findLayer(layerName);
		if (layer == null) return;
		GenMath.MutableDouble thickness = (GenMath.MutableDouble)threeDThicknessMap.get(layer);
		GenMath.MutableDouble height = (GenMath.MutableDouble)threeDDistanceMap.get(layer);
		threeDHeight.setText(TextUtils.formatDouble(height.doubleValue()));
		threeDThickness.setText(TextUtils.formatDouble(thickness.doubleValue()));
		initial3DTextChanging = false;
		threeDSideView.repaint();
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
			// Using reflection to call 3D function
			if (view3DClass != null)

			try
			{
				if (set3DClass == null)
					set3DClass = view3DClass.getDeclaredMethod("setAntialiasing", new Class[] {Boolean.class});
				Boolean value = new Boolean(currentAntialiasing);
				set3DClass.invoke(view3DClass, new Object[]{value});
			} catch (Exception e) {
				System.out.println("Cannot call 3D setAntialiasing plugin method: " + e.getMessage());
			}
			User.set3DAntialiasing(currentAntialiasing);
		}
        double currentScaleFactor = TextUtils.atof(scaleField.getText());
        if (currentScaleFactor != User.get3DFactor())
        {
			// Using reflection to call 3D function
			if (view3DClass != null)

			try
			{
				if (set3DClass == null)
					set3DClass = view3DClass.getDeclaredMethod("setScaleFactor", new Class[] {Double.class});
				Double value = new Double(currentScaleFactor);
				set3DClass.invoke(view3DClass, new Object[]{value});
			} catch (Exception e) {
				System.out.println("Cannot call 3D setScaleFactor plugin method: " + e.getMessage());
			}
            User.set3DFactor(currentScaleFactor);
        }
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
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
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
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(threeDPerspective, gridBagConstraints);

        threeDAntialiasing.setText("Use Antialiasing");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        threeD.add(threeDAntialiasing, gridBagConstraints);

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
    private javax.swing.JPanel threeD;
    private javax.swing.JCheckBox threeDAntialiasing;
    private javax.swing.JTextField threeDHeight;
    private javax.swing.JScrollPane threeDLayerPane;
    private javax.swing.JCheckBox threeDPerspective;
    private javax.swing.JLabel threeDTechnology;
    private javax.swing.JTextField threeDThickness;
    // End of variables declaration//GEN-END:variables

}
