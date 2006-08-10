/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayerTab.java
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Class to handle the "Layers tab" of a window.
 */
public class LayerTab extends JPanel
{
	private JList layerList;
	private DefaultListModel layerListModel;
	private HashMap<Layer,Boolean> highlighted;
	private List<Layer> layersInList;
	private boolean loading;
    private boolean layerDrawing;

	private static HashMap<Layer,Boolean> visibility;

	/**
	 * Constructor creates a new panel for the Layers tab.
	 */
	public LayerTab(WindowFrame wf)
	{
		initComponents();

		// build the change list
		layerListModel = new DefaultListModel();
		layerList = new JList(layerListModel);
		layerList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		layerPane.setViewportView(layerList);
		layerList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e) { apply(e); }
		});
		nodeText.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { update(); }
        });
		arcText.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { update(); }
        });
		portText.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { update(); }
        });
		exportText.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { update(); }
        });
		annotationText.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { update(); }
        });
		instanceNames.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { update(); }
        });
		cellText.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { update(); }
        });
//		if (Job.getDebug())
//		{
			opacitySlider.addChangeListener(new ChangeListener()
	        {
	            public void stateChanged(ChangeEvent evt) { sliderChanged(); }
	        });
//		} else
//		{
//			remove(opacitySlider);
//		}

		technology.setLightWeightPopupEnabled(false);

        // Getting default tech stored
        loadTechnologies(true);
		updateLayersTab();
		technology.addActionListener(new WindowFrame.CurTechControlListener(wf));
	}

    /**
     * Free allocated resources before closing.
     */
    public void finished()
    {
        // making memory available for GC
        layersInList.clear(); layersInList = null;
        highlighted.clear(); highlighted = null;
    }

	/**
	 * Method to update the technology popup selector in the Layers tab.
	 * Called at initialization or when a new technology has been created.
	 * @param makeCurrent true to keep the current technology selected,
	 * false to set to the current technology.
	 */
	public void loadTechnologies(boolean makeCurrent)
	{
        Technology cur = Technology.getCurrent();
        if (!makeCurrent) cur = Technology.findTechnology((String)technology.getSelectedItem());
		loading = true;
		technology.removeAllItems();
        for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
        {
            Technology tech = it.next();
            if (tech == Generic.tech && !Job.getDebug()) continue;
			technology.addItem(tech.getTechName());
        }
        setSelectedTechnology(cur);
		loading = false;

		// cache visibility
		visibility = new HashMap<Layer,Boolean>();
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = lIt.next();
				if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;
				visibility.put(layer, new Boolean(layer.isVisible()));
			}
		}
        visibility.put(Generic.tech.drcLay, new Boolean(Generic.tech.drcLay.isVisible()));
        visibility.put(Generic.tech.afgLay, new Boolean(Generic.tech.afgLay.isVisible()));
	}

    /**
     * Method to set the technology in the pull down menu of this Layers tab.
     * @param tech the technology to set.
     */
    public void setSelectedTechnology(Technology tech) { technology.setSelectedItem(tech.getTechName()); }

    public void setDisplayAlgorithm(boolean layerDrawing) {
        boolean changed = this.layerDrawing != layerDrawing;
        this.layerDrawing = layerDrawing;
        if (changed)
            updateLayersTab();
    }
    
	/**
	 * Method to update this LayersTab.
	 * Called when any of the values in the tab have changed.
	 */
	public void updateLayersTab()
	{
		if (loading) return;

		// initialize text visibility checkboxes
		nodeText.setSelected(User.isTextVisibilityOnNode());
		arcText.setSelected(User.isTextVisibilityOnArc());
		portText.setSelected(User.isTextVisibilityOnPort());
		exportText.setSelected(User.isTextVisibilityOnExport());
		annotationText.setSelected(User.isTextVisibilityOnAnnotation());
		instanceNames.setSelected(User.isTextVisibilityOnInstance());
		cellText.setSelected(User.isTextVisibilityOnCell());

		// cache dimming
		boolean noDimming = true;
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = lIt.next();
				if (layer.isDimmed()) noDimming = false;
			}
		}

		// cache highlighting
		highlighted = new HashMap<Layer,Boolean>();
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = lIt.next();
				if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;
				if (noDimming) highlighted.put(layer, new Boolean(false)); else
					highlighted.put(layer, new Boolean(!layer.isDimmed()));
			}
		}

		Technology tech = Technology.getCurrent();
		technology.setSelectedItem(tech.getTechName());
		layerListModel.clear();
		layersInList = new ArrayList<Layer>();
		for(Layer layer : tech.getLayersSortedByHeight())
		{
			if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;
			layersInList.add(layer);

			// add the line to the scroll list
			layerListModel.addElement(lineName(layer));
		}
        // Adding special layers in case of layout technologies
        if (tech.isLayout())
        {
            layersInList.add(Generic.tech.drcLay);
            layerListModel.addElement(lineName(Generic.tech.drcLay));
            layersInList.add(Generic.tech.afgLay);
            layerListModel.addElement(lineName(Generic.tech.afgLay));
        }

        layerList.setSelectedIndex(0);
        opacitySlider.setVisible(layerDrawing);
        resetOpacity.setVisible(layerDrawing);
	}

	private String lineName(Layer layer)
	{
		StringBuffer layerName = new StringBuffer();
		Boolean layerVisible = visibility.get(layer);
		if (layerVisible.booleanValue()) layerName.append("\u2713 "); else
			layerName.append("  ");
		if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) layerName.append(" (for pins)");
		Boolean layerHighlighted = highlighted.get(layer);
		layerName.append(layer.getName());
		if (layerHighlighted.booleanValue()) layerName.append(" (HIGHLIGHTED)");
		if (/*Job.getDebug()*/layerDrawing)
			layerName.append(" (" + TextUtils.formatDouble(layer.getGraphics().getOpacity(),2) + ")");
		return layerName.toString();
	}

	/**
	 * Method called when the user clicks on an element of the list.
	 * @param e Event information.
	 */
	private void apply(MouseEvent e)
	{
		int [] indices = layerList.getSelectedIndices();
		if (indices.length == 1)
		{
			// single layer selected: show opacity
			if (/*Job.getDebug()*/layerDrawing)
			{
				Layer layer = getSelectedLayer(indices[0]);
				if (layer != null)
				{
					double opacity = layer.getGraphics().getOpacity();
					double range = opacitySlider.getMaximum() - opacitySlider.getMinimum();
					int newValue = opacitySlider.getMinimum() + (int)(range * opacity);
					opacitySlider.setValue(newValue);
				}
			}
		}
		if (e.getClickCount() == 2)
		{
			for(int i=0; i<indices.length; i++)
			{
				int line = indices[i];
				setVisibility(line, !isLineChecked(line), true);
			}
		}
	}

	/**
	 * Method called when the opacity slider is changed.
	 */
	private void sliderChanged()
	{
		// single layer selected: show opacity
		int [] indices = layerList.getSelectedIndices();
		if (indices.length != 1) return;
		Layer layer = getSelectedLayer(indices[0]);
		if (layer == null) return;

		int sliderValue = opacitySlider.getValue() - opacitySlider.getMinimum();
		double range = opacitySlider.getMaximum() - opacitySlider.getMinimum();
		double newOpacity = sliderValue / range;
		layer.getGraphics().setOpacity(newOpacity);
		layerListModel.set(indices[0], lineName(layer));

        opacityChanged();
	}
    
    private void opacityChanged() {
        for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); ) {
            WindowFrame wf = it.next();
            WindowContent content = wf.getContent();
            if (!(content instanceof EditWindow)) continue;
            EditWindow wnd = (EditWindow)content;
            wnd.opacityChanged();
            LayerTab layerTab = wf.getLayersTab();
//            layerTab.updateLayersTab();
            if (layerTab == this)
                wnd.repaint();
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
	 * Method to select all layers.
	 */
	private void selectAll()
	{
		int len = layerListModel.size();
		int [] indices = new int[len];
		for(int i=0; i<len; i++) indices[i] = i;
		layerList.setSelectedIndices(indices);
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
			setVisibility(line, on, false);
		}

		// update the display
		update();
	}

	private boolean isLineChecked(int i)
	{
		String s = (String)layerListModel.get(i);
		if (s.charAt(0) == ' ') return false;
		return true;
	}

	private Layer getSelectedLayer(int i)
	{
		String name = (String)layerListModel.get(i);
		if (name != null) name = name.substring(2);
		String techName = (String)technology.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		int spacePos = name.indexOf(' ');
		if (spacePos >= 0) name = name.substring(0, spacePos);
		Layer layer = tech.findLayer(name);
		if (layer == null)
		{
            layer = Generic.tech.findLayer(name);
            if (layer == null)
			    System.out.println("Can't find "+name);
		}
		return layer;
	}

	/**
	 * Method to change a line of the layer list.
	 * @param i the line number to change.
	 * @param on true to make that layer visible.
	 */
	private void setVisibility(int i, boolean on, boolean doUpdate)
	{
		// find the layer on the given line
		Layer layer = getSelectedLayer(i);
		if (layer == null) return;

		// remember the state of this layer
		visibility.put(layer, new Boolean(on));

		// update the list
		layerListModel.set(i, lineName(layer));

		// update the display
		if (doUpdate) update();
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
		if (how == 1) newState = !highlighted.get(layer).booleanValue();
		highlighted.put(layer, new Boolean(newState));

		// update the list
		layerListModel.set(i, lineName(layer));

		// update the display
		update();
	}

	private void update()
	{
		// see if anything was highlighted
		boolean changed = false;
		boolean anyHighlighted = false;
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = lIt.next();
                Boolean layerHighlighted = highlighted.get(layer.getNonPseudoLayer());
                if (layerHighlighted != null && layerHighlighted.booleanValue()) anyHighlighted = true;
			}
		}

		// update visibility and highlighting
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = lIt.next();
				Boolean layerVis = visibility.get(layer.getNonPseudoLayer());
                if (layerVis != null)
                {
	                if (layer.isVisible() != layerVis.booleanValue())
	                {
                		changed = true;
	                	layer.setVisible(layerVis.booleanValue());

                        // graphics notifies to all 3D observers if available
                        layer.getGraphics().notifyVisibility(layerVis);
	                }
                }

                Boolean layerHighlighted = highlighted.get(layer.getNonPseudoLayer());
                if (layerHighlighted != null)
                {
                	boolean newState = false;
                	if (anyHighlighted && !layerHighlighted.booleanValue()) newState = true;
                	if (newState != layer.isDimmed())
                	{
                		layer.setDimmed(newState);
                		changed = true;
                	}
                }
			}
		}

		// recompute visibility of primitive nodes and arcs
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			for(Iterator<PrimitiveNode> nIt = tech.getNodes(); nIt.hasNext(); )
			{
				PrimitiveNode np = nIt.next();
				Technology.NodeLayer [] layers = np.getLayers();
				boolean invisible = true;
				for(int i=0; i<layers.length; i++)
				{
					Technology.NodeLayer lay = layers[i];
					if (lay.getLayer().isVisible()) { invisible = false;   break; }
				}
				np.setNodeInvisible(invisible);
			}
			for(Iterator<ArcProto> aIt = tech.getArcs(); aIt.hasNext(); )
			{
				ArcProto ap = aIt.next();
				Technology.ArcLayer [] layers = ap.getLayers();
				boolean invisible = true;
				for(int i=0; i<layers.length; i++)
				{
					Technology.ArcLayer lay = layers[i];
					if (lay.getLayer().isVisible()) { invisible = false;   break; }
				}
				ap.setArcInvisible(invisible);
			}
		}

		boolean textVisChanged = false;
		boolean currentTextOnNode = nodeText.isSelected();
		if (currentTextOnNode != User.isTextVisibilityOnNode())
		{
			textVisChanged = true;
			User.setTextVisibilityOnNode(currentTextOnNode);
		}

		boolean currentTextOnArc = arcText.isSelected();
		if (currentTextOnArc != User.isTextVisibilityOnArc())
		{
			textVisChanged = true;
			User.setTextVisibilityOnArc(currentTextOnArc);
		}

		boolean currentTextOnPort = portText.isSelected();
		if (currentTextOnPort != User.isTextVisibilityOnPort())
		{
			textVisChanged = true;
			User.setTextVisibilityOnPort(currentTextOnPort);
		}

		boolean currentTextOnExport = exportText.isSelected();
		if (currentTextOnExport != User.isTextVisibilityOnExport())
		{
			textVisChanged = true;
			User.setTextVisibilityOnExport(currentTextOnExport);
		}

		boolean currentTextOnAnnotation = annotationText.isSelected();
		if (currentTextOnAnnotation != User.isTextVisibilityOnAnnotation())
		{
			textVisChanged = true;
			User.setTextVisibilityOnAnnotation(currentTextOnAnnotation);
		}

		boolean currentTextOnInstance = instanceNames.isSelected();
		if (currentTextOnInstance != User.isTextVisibilityOnInstance())
		{
			textVisChanged = true;
			User.setTextVisibilityOnInstance(currentTextOnInstance);
		}

		boolean currentTextOnCell = cellText.isSelected();
		if (currentTextOnCell != User.isTextVisibilityOnCell())
		{
			textVisChanged = true;
			User.setTextVisibilityOnCell(currentTextOnCell);
		}

		// make sure all other visibility panels are in sync
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			LayerTab lt = wf.getLayersTab();
			if (lt != this)
				lt.updateLayersTab();
		}

        
		if (changed || textVisChanged)
            User.layerVisibilityChanged(!changed);
//		if (changed || textVisChanged)
//		{
//			PixelDrawing.clearSubCellCache();
//			EditWindow.repaintAllContents();
//		}
//		if (changed)
//			VectorDrawing.layerVisibilityChanged();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        java.awt.GridBagConstraints gridBagConstraints;

        layerPane = new javax.swing.JScrollPane();
        technology = new javax.swing.JComboBox();
        selectAll = new javax.swing.JButton();
        makeVisible = new javax.swing.JButton();
        makeInvisible = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        unhighlightAll = new javax.swing.JButton();
        toggleHighlight = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        cellText = new javax.swing.JCheckBox();
        arcText = new javax.swing.JCheckBox();
        annotationText = new javax.swing.JCheckBox();
        instanceNames = new javax.swing.JCheckBox();
        exportText = new javax.swing.JCheckBox();
        portText = new javax.swing.JCheckBox();
        nodeText = new javax.swing.JCheckBox();
        opacitySlider = new javax.swing.JSlider();
        resetOpacity = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        setName("");
        layerPane.setMinimumSize(new java.awt.Dimension(100, 300));
        layerPane.setPreferredSize(new java.awt.Dimension(100, 300));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(layerPane, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        add(technology, gridBagConstraints);

        selectAll.setText("Select All");
        selectAll.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                selectAllActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        add(selectAll, gridBagConstraints);

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
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        add(makeVisible, gridBagConstraints);

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
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 2, 4);
        add(makeInvisible, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Highlighting"));
        unhighlightAll.setText("Clear");
        unhighlightAll.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                unhighlightAllActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 2, 4);
        jPanel1.add(unhighlightAll, gridBagConstraints);

        toggleHighlight.setText("Toggle");
        toggleHighlight.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                toggleHighlightActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 1, 4);
        jPanel1.add(toggleHighlight, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        add(jPanel1, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Text Visibility"));
        cellText.setText("Cell text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(cellText, gridBagConstraints);

        arcText.setText("Arc text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(arcText, gridBagConstraints);

        annotationText.setText("Annotation text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(annotationText, gridBagConstraints);

        instanceNames.setText("Instance names");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(instanceNames, gridBagConstraints);

        exportText.setText("Export text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(exportText, gridBagConstraints);

        portText.setText("Port text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(portText, gridBagConstraints);

        nodeText.setText("Node text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        jPanel2.add(nodeText, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(jPanel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        add(opacitySlider, gridBagConstraints);

        resetOpacity.setText("Reset Opacity");
        resetOpacity.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                resetOpacityActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 1, 4);
        add(resetOpacity, gridBagConstraints);

    }// </editor-fold>//GEN-END:initComponents

    private void resetOpacityActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_resetOpacityActionPerformed
    {//GEN-HEADEREND:event_resetOpacityActionPerformed
		String techName = (String)technology.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
        if (tech == null) return;
        EditWindow.setDefaultOpacity(tech);
        updateLayersTab();
        opacityChanged();
    }//GEN-LAST:event_resetOpacityActionPerformed

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

	private void selectAllActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_selectAllActionPerformed
	{//GEN-HEADEREND:event_selectAllActionPerformed
		selectAll();
	}//GEN-LAST:event_selectAllActionPerformed

//	/** Closes the dialog */
//	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
//	{
//		setVisible(false);
//		dispose();
//	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox annotationText;
    private javax.swing.JCheckBox arcText;
    private javax.swing.JCheckBox cellText;
    private javax.swing.JCheckBox exportText;
    private javax.swing.JCheckBox instanceNames;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane layerPane;
    private javax.swing.JButton makeInvisible;
    private javax.swing.JButton makeVisible;
    private javax.swing.JCheckBox nodeText;
    private javax.swing.JSlider opacitySlider;
    private javax.swing.JCheckBox portText;
    private javax.swing.JButton resetOpacity;
    private javax.swing.JButton selectAll;
    private javax.swing.JComboBox technology;
    private javax.swing.JButton toggleHighlight;
    private javax.swing.JButton unhighlightAll;
    // End of variables declaration//GEN-END:variables

}
