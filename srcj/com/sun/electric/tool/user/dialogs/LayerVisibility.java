/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayerVisibility.java
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

import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.PixelDrawing;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.MouseAdapter;
import javax.swing.event.ChangeEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;


/**
 * Class to handle the "Layer Visibility" dialog.
 */
public class LayerVisibility extends EDialog
{
	private JList layerList;
	private DefaultListModel layerListModel;
	private HashMap visibility;
	private HashMap highlighted;
	private List layersInList;
	private boolean initialTextOnNode;
	private boolean initialTextOnArc;
	private boolean initialTextOnPort;
	private boolean initialTextOnExport;
	private boolean initialTextOnAnnotation;
	private boolean initialTextOnInstance;
	private boolean initialTextOnCell;
	private boolean showHighlighted;

	// 3D view. Static values to avoid unnecessary calls
	private static final Class view3DClass = Resources.get3DMainClass();
	private static Method setVisibilityMethod = null;

	/** Creates new form Layer Visibility */
	public LayerVisibility(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// initialize text visibility checkboxes
		initialTextOnNode = User.isTextVisibilityOnNode();
		nodeText.setSelected(initialTextOnNode);
		initialTextOnArc = User.isTextVisibilityOnArc();
		arcText.setSelected(initialTextOnArc);
		initialTextOnPort = User.isTextVisibilityOnPort();
		portText.setSelected(initialTextOnPort);
		initialTextOnExport = User.isTextVisibilityOnExport();
		exportText.setSelected(initialTextOnExport);
		initialTextOnAnnotation = User.isTextVisibilityOnAnnotation();
		annotationText.setSelected(initialTextOnAnnotation);
		initialTextOnInstance = User.isTextVisibilityOnInstance();
		instanceNames.setSelected(initialTextOnInstance);
		initialTextOnCell = User.isTextVisibilityOnCell();
		cellText.setSelected(initialTextOnCell);

		// build the change list
		layerListModel = new DefaultListModel();
		layerList = new JList(layerListModel);
		layerList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		layerPane.setViewportView(layerList);
		layerList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e) { apply(e); }
		});

		// cache dimming
		boolean noDimming = true;
		for(Iterator it = Technology.getTechnologiesSortedByName().iterator(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = (Layer)lIt.next();
				if (layer.isDimmed()) noDimming = false;
			}
		}

		// cache visibility
		visibility = new HashMap();
		highlighted = new HashMap();
		for(Iterator it = Technology.getTechnologiesSortedByName().iterator(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = (Layer)lIt.next();
				if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;
				visibility.put(layer, new Boolean(layer.isVisible()));
				if (noDimming) highlighted.put(layer, new Boolean(false)); else
					highlighted.put(layer, new Boolean(!layer.isDimmed()));
			}
		}

		// make a popup of Technologies
		for(Iterator it = Technology.getTechnologiesSortedByName().iterator(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			technology.addItem(tech.getTechName());
		}
		technology.setSelectedItem(Technology.getCurrent().getTechName());

		showLayersForTechnology();
		finishInitialization();
	}

	private void showLayersForTechnology()
	{
		String techName = (String)technology.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);

		layerListModel.clear();
		layersInList = new ArrayList();
		for(Iterator it = tech.getLayersSortedByHeight().iterator(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;
			layersInList.add(layer);

			// add the line to the scroll list
			layerListModel.addElement(lineName(layer));
		}
        layerList.setSelectedIndex(0);
	}

	private String lineName(Layer layer)
	{
		StringBuffer layerName = new StringBuffer();
		Boolean layerVisible = (Boolean)visibility.get(layer);
		if (layerVisible.booleanValue()) layerName.append("\u2713 "); else
			layerName.append("  ");
		if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) layerName.append(" (for pins)");
		Boolean layerHighlighted = (Boolean)highlighted.get(layer);
		layerName.append(layer.getName());
		if (layerHighlighted.booleanValue()) layerName.append(" (HIGHLIGHTED)");
		return layerName.toString();
	}

	/**
	 * Method called when the user clicks on an element of the list.
	 * @param e Event information.
	 */
	private void apply(MouseEvent e)
	{
		if (e.getClickCount() == 2)
		{
			int [] indices = layerList.getSelectedIndices();
			for(int i=0; i<indices.length; i++)
			{
				int line = indices[i];
				setVisibility(line, !isLineChecked(line));
			}
		}
	}

	/**
	 * Method to clear all highlighting.
	 */
	private void clearAllHighlight()
	{
		for(int i=0; i<layerListModel.size(); i++)
		{
			changeHighlighted(i, 0);
		}
	}

	private void toggleHighlight()
	{
		int [] indices = layerList.getSelectedIndices();
		for(int i=0; i<indices.length; i++)
		{
			int line = indices[i];
			changeHighlighted(line, 1);
		}
	}

	/**
	 * Method to make all layers visible or invisible.
	 * @param on true to make all layers visible.
	 */
	private void setAllVisibility(boolean on)
	{
		for(int i=0; i<layerListModel.size(); i++)
		{
			setVisibility(i, on);
		}
	}

	/**
	 * Method to make the selected layers visible or invisible.
	 * @param on true to make selected layers visible.
	 */
	private void setVisibility(boolean on)
	{
		int [] indices = layerList.getSelectedIndices();
		for(int i=0; i<indices.length; i++)
		{
			int line = indices[i];
			setVisibility(line, on);
		}
	}

	private boolean isLineChecked(int i)
	{
		String s = (String)layerListModel.get(i);
		if (s.charAt(0) == ' ') return false;
		return true;
	}

	/**
	 * Method to change a line of the layer list.
	 * @param i the line number to change.
	 * @param on true to make that layer visible.
	 */
	private void setVisibility(int i, boolean on)
	{
		// find the layer on the given line
		String name = (String)layerListModel.get(i);
		if (name != null) name = name.substring(2);
		String techName = (String)technology.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		int spacePos = name.indexOf(' ');
		if (spacePos >= 0) name = name.substring(0, spacePos);
		Layer layer = tech.findLayer(name);
		if (layer == null)
		{
			System.out.println("Can't find "+name);
			return;
		}

		// remember the state of this layer
		visibility.put(layer, new Boolean(on));

		// update the list
		layerListModel.set(i, lineName(layer));
	}

	/**
	 * Method to change a line of the layer list.
	 * @param i the line number to change.
	 * @param how 1: toggle highlighting; 0: clear highlighting.
	 */
	private void changeHighlighted(int i, int how)
	{
		// find the layer on the given line
		String name = (String)layerListModel.get(i);
		if (name != null) name = name.substring(2);
		String techName = (String)technology.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		int spacePos = name.indexOf(' ');
		if (spacePos >= 0) name = name.substring(0, spacePos);
		Layer layer = tech.findLayer(name);
		if (layer == null)
		{
			System.out.println("Can't find "+name);
			return;
		}

		// remember the state of this layer
		boolean newState = false;
		if (how == 1) newState = !((Boolean)highlighted.get(layer)).booleanValue();
		highlighted.put(layer, new Boolean(newState));

		// update the list
		layerListModel.set(i, lineName(layer));
	}

	protected void escapePressed() { cancelActionPerformed(null); }

	private void termDialog()
	{
		// see if anything was highlighted
		boolean anyHighlighted = false;
		for(Iterator it = Technology.getTechnologiesSortedByName().iterator(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = (Layer)lIt.next();
                Boolean layerHighlighted = (Boolean)highlighted.get(layer.getNonPseudoLayer());
                if (layerHighlighted != null && layerHighlighted.booleanValue()) anyHighlighted = true;
			}
		}

		// update visibility and highlighting
		for(Iterator it = Technology.getTechnologiesSortedByName().iterator(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = (Layer)lIt.next();
				Boolean layerVis = (Boolean)visibility.get(layer.getNonPseudoLayer());
                if (layerVis != null)
                {
	                if (layer.isVisible() != layerVis.booleanValue())
	                	layer.setVisible(layerVis.booleanValue());

					// 3D appearance if available
					Object obj3D = layer.getGraphics().get3DAppearance();
					if (obj3D != null)
					{
						try
						{
							if (setVisibilityMethod == null) setVisibilityMethod = view3DClass.getDeclaredMethod("set3DVisibility", new Class[] {Object.class, Boolean.class});
							setVisibilityMethod.invoke(view3DClass, new Object[]{obj3D, layerVis});
						} catch (Exception e) {
							System.out.println("Cannot call 3D plugin method set3DVisibility: " + e.getMessage());
						}
					}
                }

                Boolean layerHighlighted = (Boolean)highlighted.get(layer.getNonPseudoLayer());
                if (layerHighlighted != null)
                {
                	boolean newState = false;
                	if (anyHighlighted && !layerHighlighted.booleanValue()) newState = true;
                	if (newState != layer.isDimmed()) layer.setDimmed(newState);
                }
			}
		}

//System.out.print("Dim layers in "+Technology.getCurrent().getTechName()+":");
//for(Iterator it = Technology.getCurrent().getLayers(); it.hasNext(); )
//{
// Layer l = (Layer)it.next();
// if (l.isDimmed()) System.out.print(" "+l.getName());
//}
//System.out.println();
		// recompute visibility of primitive nodes and arcs
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator nIt = tech.getNodes(); nIt.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)nIt.next();
				Technology.NodeLayer [] layers = np.getLayers();
				boolean invisible = true;
				for(int i=0; i<layers.length; i++)
				{
					Technology.NodeLayer lay = layers[i];
					if (lay.getLayer().isVisible()) { invisible = false;   break; }
				}
				np.setNodeInvisible(invisible);
			}
			for(Iterator aIt = tech.getArcs(); aIt.hasNext(); )
			{
				ArcProto ap = (ArcProto)aIt.next();
				PrimitiveArc pAp = (PrimitiveArc)ap;
				Technology.ArcLayer [] layers = pAp.getLayers();
				boolean invisible = true;
				for(int i=0; i<layers.length; i++)
				{
					Technology.ArcLayer lay = layers[i];
					if (lay.getLayer().isVisible()) { invisible = false;   break; }
				}
				ap.setArcInvisible(invisible);
			}
		}

		boolean currentTextOnNode = nodeText.isSelected();
		if (currentTextOnNode != initialTextOnNode)
			User.setTextVisibilityOnNode(initialTextOnNode = currentTextOnNode);

		boolean currentTextOnArc = arcText.isSelected();
		if (currentTextOnArc != initialTextOnArc)
			User.setTextVisibilityOnArc(initialTextOnArc = currentTextOnArc);

		boolean currentTextOnPort = portText.isSelected();
		if (currentTextOnPort != initialTextOnPort)
			User.setTextVisibilityOnPort(initialTextOnPort = currentTextOnPort);

		boolean currentTextOnExport = exportText.isSelected();
		if (currentTextOnExport != initialTextOnExport)
			User.setTextVisibilityOnExport(initialTextOnExport = currentTextOnExport);

		boolean currentTextOnAnnotation = annotationText.isSelected();
		if (currentTextOnAnnotation != initialTextOnAnnotation)
			User.setTextVisibilityOnAnnotation(initialTextOnAnnotation = currentTextOnAnnotation);

		boolean currentTextOnInstance = instanceNames.isSelected();
		if (currentTextOnInstance != initialTextOnInstance)
			User.setTextVisibilityOnInstance(initialTextOnInstance = currentTextOnInstance);

		boolean currentTextOnCell = cellText.isSelected();
		if (currentTextOnCell != initialTextOnCell)
			User.setTextVisibilityOnCell(initialTextOnCell = currentTextOnCell);

		PixelDrawing.clearSubCellCache();
		EditWindow.repaintAllContents();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        apply = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        layerPane = new javax.swing.JScrollPane();
        technology = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        nodeText = new javax.swing.JCheckBox();
        arcText = new javax.swing.JCheckBox();
        portText = new javax.swing.JCheckBox();
        exportText = new javax.swing.JCheckBox();
        annotationText = new javax.swing.JCheckBox();
        instanceNames = new javax.swing.JCheckBox();
        cellText = new javax.swing.JCheckBox();
        allVisible = new javax.swing.JButton();
        allInvisible = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        cancel = new javax.swing.JButton();
        makeVisible = new javax.swing.JButton();
        makeInvisible = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        toggleHighlight = new javax.swing.JButton();
        unhighlightAll = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Layer Visibility");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        apply.setText("Apply");
        apply.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                apply(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 0.5;
        getContentPane().add(apply, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ok(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 21;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 0.5;
        getContentPane().add(ok, gridBagConstraints);

        layerPane.setMinimumSize(new java.awt.Dimension(100, 300));
        layerPane.setPreferredSize(new java.awt.Dimension(100, 300));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(layerPane, gridBagConstraints);

        technology.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                technologyActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(technology, gridBagConstraints);

        jLabel1.setText("Technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        nodeText.setText("Node text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(nodeText, gridBagConstraints);

        arcText.setText("Arc text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(arcText, gridBagConstraints);

        portText.setText("Port text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(portText, gridBagConstraints);

        exportText.setText("Export text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(exportText, gridBagConstraints);

        annotationText.setText("Annotation text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(annotationText, gridBagConstraints);

        instanceNames.setText("Instance names");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(instanceNames, gridBagConstraints);

        cellText.setText("Cell text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(cellText, gridBagConstraints);

        allVisible.setText("All Visible");
        allVisible.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                allVisibleActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(allVisible, gridBagConstraints);

        allInvisible.setText("All Invisible");
        allInvisible.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                allInvisibleActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(allInvisible, gridBagConstraints);

        jLabel3.setText("Cheked layers are visibile; double-click to toggle.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        getContentPane().add(jLabel3, gridBagConstraints);

        jLabel4.setText("Text visibility options:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel4, gridBagConstraints);

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 21;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        makeVisible.setText("Make Visible");
        makeVisible.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                makeVisibleActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        getContentPane().add(makeVisible, gridBagConstraints);

        makeInvisible.setText("Make Invisible");
        makeInvisible.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                makeInvisibleActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        getContentPane().add(makeInvisible, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jSeparator1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jSeparator2, gridBagConstraints);

        toggleHighlight.setText("Toggle Highlight");
        toggleHighlight.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                toggleHighlightActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        getContentPane().add(toggleHighlight, gridBagConstraints);

        unhighlightAll.setText("Unhighlight All");
        unhighlightAll.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                unhighlightAllActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(unhighlightAll, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jSeparator3, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void toggleHighlightActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_toggleHighlightActionPerformed
	{//GEN-HEADEREND:event_toggleHighlightActionPerformed
		toggleHighlight();
	}//GEN-LAST:event_toggleHighlightActionPerformed

	private void unhighlightAllActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_unhighlightAllActionPerformed
	{//GEN-HEADEREND:event_unhighlightAllActionPerformed
		clearAllHighlight();
	}//GEN-LAST:event_unhighlightAllActionPerformed

	private void makeInvisibleActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_makeInvisibleActionPerformed
	{//GEN-HEADEREND:event_makeInvisibleActionPerformed
		setVisibility(false);
	}//GEN-LAST:event_makeInvisibleActionPerformed

	private void makeVisibleActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_makeVisibleActionPerformed
	{//GEN-HEADEREND:event_makeVisibleActionPerformed
		setVisibility(true);
	}//GEN-LAST:event_makeVisibleActionPerformed

	private void cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelActionPerformed
	{//GEN-HEADEREND:event_cancelActionPerformed
		closeDialog(null);
	}//GEN-LAST:event_cancelActionPerformed

	private void allVisibleActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_allVisibleActionPerformed
	{//GEN-HEADEREND:event_allVisibleActionPerformed
		setAllVisibility(true);
	}//GEN-LAST:event_allVisibleActionPerformed

	private void allInvisibleActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_allInvisibleActionPerformed
	{//GEN-HEADEREND:event_allInvisibleActionPerformed
		setAllVisibility(false);
	}//GEN-LAST:event_allInvisibleActionPerformed

	private void technologyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_technologyActionPerformed
	{//GEN-HEADEREND:event_technologyActionPerformed
		showLayersForTechnology();
	}//GEN-LAST:event_technologyActionPerformed

	private void apply(java.awt.event.ActionEvent evt)//GEN-FIRST:event_apply
	{//GEN-HEADEREND:event_apply
		termDialog();
	}//GEN-LAST:event_apply

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		termDialog();
		closeDialog(null);
	}//GEN-LAST:event_ok

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton allInvisible;
    private javax.swing.JButton allVisible;
    private javax.swing.JCheckBox annotationText;
    private javax.swing.JButton apply;
    private javax.swing.JCheckBox arcText;
    private javax.swing.JButton cancel;
    private javax.swing.JCheckBox cellText;
    private javax.swing.JCheckBox exportText;
    private javax.swing.JCheckBox instanceNames;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JScrollPane layerPane;
    private javax.swing.JButton makeInvisible;
    private javax.swing.JButton makeVisible;
    private javax.swing.JCheckBox nodeText;
    private javax.swing.JButton ok;
    private javax.swing.JCheckBox portText;
    private javax.swing.JComboBox technology;
    private javax.swing.JButton toggleHighlight;
    private javax.swing.JButton unhighlightAll;
    // End of variables declaration//GEN-END:variables
	
}
