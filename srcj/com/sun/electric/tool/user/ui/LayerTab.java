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
 * the Free Software Foundation; either version 3 of the License, or
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
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.GraphicsPreferences;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.redisplay.AbstractDrawing;
import com.sun.electric.tool.user.redisplay.VectorCache;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class to handle the "Layers tab" of a window.
 */
public class LayerTab extends JPanel implements DragSourceListener, DragGestureListener
{
	private JList layerList, configurationList;
	private DefaultListModel layerListModel, configurationModel;
	private List<Layer> layersInList;
	private DragSource dragSource;
	private boolean loading;
	private boolean layerDrawing;
    private LayerVisibility lv;
	private InvisibleLayerConfiguration invLayerConfigs = InvisibleLayerConfiguration.getOnly();

	private static final ImageIcon iconVisNew = Resources.getResource(LayerTab.class, "IconVisNew.gif");
	private static final ImageIcon iconVisSet = Resources.getResource(LayerTab.class, "IconVisSet.gif");
	private static final ImageIcon iconVisRename = Resources.getResource(LayerTab.class, "IconVisRename.gif");
	private static final ImageIcon iconVisDelete = Resources.getResource(LayerTab.class, "IconVisDelete.gif");

	/**
	 * Constructor creates a new panel for the Layers tab.
	 */
	public LayerTab(WindowFrame wf)
	{
		initComponents();
		newConfiguration.setIcon(iconVisNew);
		newConfiguration.setText(null);
		Dimension minWid = new Dimension(iconVisNew.getIconWidth()+4, iconVisNew.getIconHeight()+4);
		newConfiguration.setMinimumSize(minWid);
		newConfiguration.setPreferredSize(minWid);

		setConfiguration.setIcon(iconVisSet);
		setConfiguration.setText(null);
		minWid = new Dimension(iconVisSet.getIconWidth()+4, iconVisSet.getIconHeight()+4);
		setConfiguration.setMinimumSize(minWid);
		setConfiguration.setPreferredSize(minWid);

		renameConfiguration.setIcon(iconVisRename);
		renameConfiguration.setText(null);
		minWid = new Dimension(iconVisRename.getIconWidth()+4, iconVisRename.getIconHeight()+4);
		renameConfiguration.setMinimumSize(minWid);
		renameConfiguration.setPreferredSize(minWid);

		deleteConfiguration.setIcon(iconVisDelete);
		deleteConfiguration.setText(null);
		minWid = new Dimension(iconVisDelete.getIconWidth()+4, iconVisDelete.getIconHeight()+4);
		deleteConfiguration.setMinimumSize(minWid);
		deleteConfiguration.setPreferredSize(minWid);

		// build the change list
		layerListModel = new DefaultListModel();
		layerList = new JList(layerListModel);
		layerList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		layerPane.setViewportView(layerList);
		layerList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e) { apply(e); }
		});

		// build the configuration list
		configurationModel = new DefaultListModel();
		configurationList = new JList(configurationModel);
		configurationPane.setViewportView(configurationList);
		configurationList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e) { useConfiguration(e); }
		});
		showConfigurations();

		// setup drag-and-drop in the layers tab
		dragSource = new DragSource();
		dragSource.createDefaultDragGestureRecognizer(layerList, DnDConstants.ACTION_COPY, this);
		new DropTarget(layerList, DnDConstants.ACTION_LINK, new LayerTabTreeDropTarget(), true);

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
		opacitySlider.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent evt) { sliderChanged(); }
		});

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
	}

	/**
	 * Method to update the technology popup selector in the Layers tab.
	 * Called at initialization or when a new technology has been created.
	 */
	public void loadTechnologies(boolean makeCurrent)
	{
		Technology cur = Technology.getCurrent();
		if (!makeCurrent || cur == null)
			cur = Technology.findTechnology((String)technology.getSelectedItem());
		loading = true;
		technology.removeAllItems();
        lv = LayerVisibility.getLayerVisibility();
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			technology.addItem(tech.getTechName());
		}

		setSelectedTechnology(cur);
		loading = false;
	}

	private void renameConfiguration()
	{
		int index = configurationList.getSelectedIndex();
		if (index < 0)
		{
			Job.getUserInterface().showErrorMessage("First select a configuration name to rename.",
				"No Configuration Selected");
			return;
		}
		String cName = (String)configurationList.getSelectedValue();
		if (cName == null) return;
		if (cName.startsWith("* ")) cName = cName.substring(2);
		String newName = Job.getUserInterface().askForInput("New Name for Configuration:", "Rename Visibility Configuration", cName);
		if (newName == null) return;
		if (invLayerConfigs.exists(newName))
		{
			Job.getUserInterface().showErrorMessage("There is already a configuration with that name.  Choose another.",
				"Duplicate Configuration Name");
			return;
		}
		invLayerConfigs.renameConfiguration(cName, newName);
		showConfigurations();
	}

	private void setConfiguration()
	{
		int index = configurationList.getSelectedIndex();
		if (index < 0)
		{
			Job.getUserInterface().showErrorMessage("First select a configuration name to set.",
				"No Configuration Selected");
			return;
		}
		String cName = (String)configurationList.getSelectedValue();
		if (cName == null) return;
		if (cName.startsWith("* ")) cName = cName.substring(2);
		updateConfiguration(cName, invLayerConfigs.getConfigurationHardwiredIndex(cName));
	}

	private void newConfiguration()
	{
		String cName = Job.getUserInterface().askForInput("New Configuration Name:", "Save New Visibility Configuration", "Config");
		if (cName == null) return;
		if (invLayerConfigs.exists(cName))
		{
			Job.getUserInterface().showErrorMessage("There is already a configuration with that name.  Choose another.",
				"Duplicate Configuration Name");
			return;
		}
		updateConfiguration(cName, -1);
	}

	private void updateConfiguration(String cName, int hardWiredIndex)
	{
		String techName = (String)technology.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		List<Layer> invis = new ArrayList<Layer>();
		for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
		{
			Layer layer = lIt.next();
            if (lv.isVisible(layer)) continue;
			invis.add(layer);
		}
		invLayerConfigs.addConfiguration(cName, hardWiredIndex, tech, invis);
		showConfigurations();
	}

	private void deleteThisConfiguration()
	{
		int index = configurationList.getSelectedIndex();
		if (index < 0)
		{
			Job.getUserInterface().showErrorMessage("First select a configuration name to delete.",
				"No Configuration Selected");
			return;
		}
		String cName = (String)configurationList.getSelectedValue();
		if (cName == null) return;
		if (cName.startsWith("* ")) cName = cName.substring(2);
		invLayerConfigs.deleteConfiguration(cName);
		showConfigurations();
	}

	/**
	 * Method called when the user clicks on a configuration name.
	 * @param e Event information.
	 */
	private void useConfiguration(MouseEvent e)
	{
		String cName = (String)configurationList.getSelectedValue();
		if (cName == null) return;
		if (cName.startsWith("* ")) cName = cName.substring(2);
		if (e.getClickCount() == 2)
		{
			// double-click: use this configuration
			setInvisibleLayerConfiguration(cName);
		}
	}

	/**
	 * Method to load an invisible layer configuration into the visible layers.
	 * @param cName the name of the invisible layer configuration.
	 */
	public void setInvisibleLayerConfiguration(String cName)
	{
		// get a set of all invisible layers in the selected configuration
		int hardIndex = invLayerConfigs.getConfigurationHardwiredIndex(cName);
		Technology tech = invLayerConfigs.getConfigurationTechnology(cName);
		if (tech == null)
		{
			if (hardIndex >= 0)
				setVisibilityLevel(hardIndex);
			return;
		}
		Set<Layer> invisibleLayers = invLayerConfigs.getConfigurationValue(cName);

        HashMap<Layer,Boolean> visibilityChange = new HashMap<Layer,Boolean>();
		for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
		{
			Layer layer = lIt.next();
			// remember the state of this layer
            visibilityChange.put(layer, Boolean.valueOf(!invisibleLayers.contains(layer)));
		}
        lv.setVisible(visibilityChange);
		updateLayersTab();
		update();
	}

	private void showConfigurations()
	{
		configurationModel.clear();
		List<String> configs = invLayerConfigs.getConfigurationNames();
		for(String key : configs)
		{
			if (invLayerConfigs.getConfigurationHardwiredIndex(key) >= 0)
			{
				if (invLayerConfigs.getConfigurationTechnology(key) != null)
					key = "* " + key;
			}
			configurationModel.addElement(key);
		}
	}

	/**
	 * Method to set the technology in the pull down menu of this Layers tab.
	 * @param tech the technology to set.
	 */
	public void setSelectedTechnology(Technology tech)
	{
		if (tech == null)
			System.out.println("Selecting a null technology");
		else
			technology.setSelectedItem(tech.getTechName());
	}

	public void setDisplayAlgorithm(boolean layerDrawing)
	{
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
        GraphicsPreferences gp = UserInterfaceMain.getGraphicsPreferences();
		nodeText.setSelected(gp.isTextVisibilityOn(TextDescriptor.TextType.NODE));
		arcText.setSelected(gp.isTextVisibilityOn(TextDescriptor.TextType.ARC));
		portText.setSelected(gp.isTextVisibilityOn(TextDescriptor.TextType.PORT));
		exportText.setSelected(gp.isTextVisibilityOn(TextDescriptor.TextType.EXPORT));
		annotationText.setSelected(gp.isTextVisibilityOn(TextDescriptor.TextType.ANNOTATION));
		instanceNames.setSelected(gp.isTextVisibilityOn(TextDescriptor.TextType.INSTANCE));
		cellText.setSelected(gp.isTextVisibilityOn(TextDescriptor.TextType.CELL));

		Technology tech = Technology.getCurrent();
		setSelectedTechnology(tech);
		layerListModel.clear();
		layersInList = new ArrayList<Layer>();
		if (tech != null)
		{
			// see if a preferred order has been saved
			List<Layer> savedOrder = lv.getSavedLayerOrder(tech);
			List<Layer> allLayers = tech.getLayersSortedByHeight();

			// put the dummy layers at the end of the list
			List<Layer> dummyLayers = new ArrayList<Layer>();
			for(Layer lay : allLayers)
			{
				if (lay.getFunction().isDummy() || lay.getFunction().isDummyExclusion())
					dummyLayers.add(lay);
			}
			for(Layer lay : dummyLayers) allLayers.remove(lay);
			for(Layer lay : dummyLayers) allLayers.add(lay);

			// add special layers in layout technologies
			if (tech.isLayout())
			{
				allLayers.add(Generic.tech().drcLay);
				allLayers.add(Generic.tech().afgLay);
			}
			if (savedOrder == null) savedOrder = allLayers;

			for(Layer layer : savedOrder)
			{
				if (layer.getTechnology() != Generic.tech() && layer.isPseudoLayer()) continue;

				layersInList.add(layer);
				layerListModel.addElement(lineName(layer));
			}

			// add any layers not saved
			for(Layer layer : allLayers)
			{
				if (savedOrder.contains(layer)) continue;
				if (layer.getTechnology() != Generic.tech() && layer.isPseudoLayer()) continue;

				layersInList.add(layer);
				layerListModel.addElement(lineName(layer));
			}
			layerList.setSelectedIndex(0);
		}
		opacitySlider.setVisible(layerDrawing);
		resetOpacity.setVisible(layerDrawing);
	}

	private String lineName(Layer layer)
	{
		StringBuffer layerName = new StringBuffer();
		if (lv.isVisible(layer)) layerName.append("\u2713 "); else
			layerName.append("  ");
		if (layer.isPseudoLayer()) layerName.append(" (for pins)");
		boolean layerHighlighted = lv.isHighlighted(layer);
		layerName.append(layer.getName());
		if (layerHighlighted) layerName.append(" (HIGHLIGHTED)");
		if (layerDrawing)
			layerName.append(" (" + TextUtils.formatDouble(lv.getOpacity(layer),2) + ")");
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
			if (layerDrawing)
			{
				Layer layer = getSelectedLayer(indices[0]);
				if (layer != null)
				{
					double opacity = lv.getOpacity(layer);
					double range = opacitySlider.getMaximum() - opacitySlider.getMinimum();
					int newValue = opacitySlider.getMinimum() + (int)(range * opacity);
					opacitySlider.setValue(newValue);
				}
			}
		}
		if (e.getClickCount() == 2)
		{
            boolean[] visible = new boolean[indices.length];
			for(int i=0; i<indices.length; i++)
			{
				int line = indices[i];
                visible[i] = !isLineChecked(line);
			}
            setVisibility(indices, visible);
            update();
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
        lv.setOpacity(layer, newOpacity);
		layerListModel.set(indices[0], lineName(layer));

		opacityChanged();
	}

	private void opacityChanged()
	{
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow)) continue;
			EditWindow wnd = (EditWindow)content;
			wnd.opacityChanged();
			LayerTab layerTab = wf.getLayersTab();
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
        boolean[] visible = new boolean[indices.length];
        Arrays.fill(visible, on);
        setVisibility(indices, visible);
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
			layer = Generic.tech().findLayer(name);
			if (layer == null)
				System.out.println("Can't find "+name);
		}
		return layer;
	}

	/**
	 * Method to change a line of the layer list.
	 * @param indices the line numbers to change.
	 * @param visible new visible values
	 */
	private void setVisibility(int[] indices, boolean[] visible)
	{
        Map<Layer,Boolean> visibilityChange = new HashMap<Layer,Boolean>();
        assert indices.length == visible.length;
        for (int i = 0; i < indices.length; i++) {
            // find the layer on the given line
            int line = indices[i];
            Layer layer = getSelectedLayer(line);
            if (layer == null) continue;
            visibilityChange.put(layer, Boolean.valueOf(visible[i]));
            // remember the state of this layer
        }
        lv.setVisible(visibilityChange);

		// update the list
        for (int i = 0; i < indices.length; i++) {
            // find the layer on the given line
            int line = indices[i];
            Layer layer = getSelectedLayer(line);
            if (layer == null) continue;
    		layerListModel.set(line, lineName(layer));
        }
	}

	/**
	 * Set the metal layer at the given level visible,
	 * and turn off all other layers.  Layer 0 turns on all layers.
	 * @param level metal level
	 */
	public void setVisibilityLevel(int level)
	{
        Technology tech = Technology.getCurrent();
        int numMetals = tech.getNumMetals();
        System.out.println("Setting for level "+level);
        int len = layerListModel.size();
        int[] indices = new int[len];
        boolean[] visible = new boolean[len];
        Layer metalLayer = null;
        if (level == 0) {
            for (int i=0; i<len; i++) {
                indices[i] = i;
                visible[i] = true;
            }
        }
        else
        {
            for (int i=0; i<len; i++)
            {
                indices[i] = i;
                Layer layer = getSelectedLayer(i);
                Layer.Function func = layer.getFunction();
                if (func.isContact() || func.isDiff() || func.isGatePoly() || func.isImplant() ||
                    func.isMetal() || func.isPoly() || func.isWell() || func.isDummy() || func.isDummyExclusion()) {
                    // handle layers that have a valid level
                    boolean b = false;
                    if (level == 2 && layer.getFunction() == Layer.Function.GATE)
                        b = true;
                    if (level == 2 && (layer.getFunction() == Layer.Function.DIFF || layer.getFunction() == Layer.Function.DIFFN || layer.getFunction() == Layer.Function.DIFFP))
                        b = true;
                    if (level == 1 && (layer.getFunction().getLevel() <= 1 || layer.getFunction() == Layer.Function.ART))
                        b = true;
                    if (layer.getFunction().getLevel() == level) {
                        b = true;
                        if (layer.getFunction().isMetal())
                            metalLayer = layer;
                    }
                    if (layer.getFunction().getLevel() == (level-1) || level == 0)
                        b = true;
                    if (layer.getFunction().isContact() && layer.getFunction().getLevel() == (level-1))
                        b = false;
                    visible[i] = b;
                } else if (func == Layer.Function.ART || func == Layer.Function.OVERGLASS)
                {
                    // handle layers that do not have a valid level
                    boolean b = false;
                    if (level == 0) b = true;          // turn on for ALL setting

                    if (func == Layer.Function.ART) {
                        if (level <= 1) b = true;   // turn on for transistor-level
                    }
                    if (func == Layer.Function.OVERGLASS) {
                        if (level >= numMetals) b = true; // turn on passivation for top-metal
                    }
                    visible[i] = b;
                } else {
                    visible[i] = lv.isVisible(layer);
                }
            }
        }
        setVisibility(indices, visible);
        // turn on all layers with the same height as the main metal layer
        if (metalLayer != null) {
//            for (int i=0; i<len; i++) {
//                Layer layer = getSelectedLayer(i);
//                if (layer.getFunction().getHeight() == metalLayer.getFunction().getHeight()) {
//                    visibility.put(layer, true);
//                    layerListModel.set(i, lineName(layer));
//                }
//            }
        }

        update();
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
		if (how == 1) newState = !lv.isHighlighted(layer);
		lv.setHighlighted(layer, newState);

		// update the list
		layerListModel.set(i, lineName(layer));

		// update the display
		update();
	}

	private void update()
	{
		// see if anything was highlighted
		boolean visibilityChanged = lv.clearChanged();

        GraphicsPreferences oldGp = UserInterfaceMain.getGraphicsPreferences();
        GraphicsPreferences gp = oldGp;
		gp = gp.withTextVisibilityOn(TextDescriptor.TextType.NODE, nodeText.isSelected());
		gp = gp.withTextVisibilityOn(TextDescriptor.TextType.ARC, arcText.isSelected());
		gp = gp.withTextVisibilityOn(TextDescriptor.TextType.PORT, portText.isSelected());
		gp = gp.withTextVisibilityOn(TextDescriptor.TextType.EXPORT, exportText.isSelected());
		gp = gp.withTextVisibilityOn(TextDescriptor.TextType.ANNOTATION, annotationText.isSelected());
		gp = gp.withTextVisibilityOn(TextDescriptor.TextType.INSTANCE, instanceNames.isSelected());
		gp = gp.withTextVisibilityOn(TextDescriptor.TextType.CELL, cellText.isSelected());
		boolean textVisChanged = gp != oldGp;
        UserInterfaceMain.setGraphicsPreferences(gp);

		// make sure all other visibility panels are in sync
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			LayerTab lt = wf.getLayersTab();
			if (lt != this)
				lt.updateLayersTab();
		}

		if (visibilityChanged || textVisChanged)
			layerVisibilityChanged(lv, !visibilityChanged);
	}

	/**
	 * Method called when visible layers have changed.
	 * Removes all "greeked images" from cached cells.
	 */
	private static void layerVisibilityChanged(LayerVisibility lv, boolean onlyText) {
		if (!onlyText)
			VectorCache.theCache.clearFadeImages();
		AbstractDrawing.clearSubCellCache(false);
		EditWindow.setLayerVisibilityAll(lv);
	}

	/************************** DRAG AND DROP **************************/

	public void dragGestureRecognized(DragGestureEvent dge)
	{
		StringSelection transferable = new StringSelection("" + layerList.getSelectedIndex());
		dragSource.startDrag(dge, DragSource.DefaultCopyDrop, transferable, this);
	}

	public void dragEnter(DragSourceDragEvent dsde) {}

	public void dragExit(DragSourceEvent dse) {}

	public void dragOver(DragSourceDragEvent dsde) {}

	public void dragDropEnd(DragSourceDropEvent dsde) {}

	public void dropActionChanged(DragSourceDragEvent dsde) {}

	/**
	 * Class for catching drags in the list of layers.
	 * These drags come from elsewhere in the layers tab.
	 */
	private class LayerTabTreeDropTarget implements DropTargetListener
	{
		private Rectangle lastDrawn = null;

		public void dragEnter(DropTargetDragEvent e)
		{
			DropTarget dt = (DropTarget)e.getSource();
			if (dt.getComponent() == layerList)
				e.acceptDrag(e.getDropAction());
		}

		public void dragOver(DropTargetDragEvent e)
		{
			DropTarget dt = (DropTarget)e.getSource();
			if (dt.getComponent() != layerList) return;
			e.acceptDrag(e.getDropAction());

			// erase former drawing
			eraseDragImage();

			// highlight the destination
			int index = layerList.locationToIndex(e.getLocation());
			Rectangle path = layerList.getCellBounds(index, index);
			if (path == null) return;
			Graphics2D g2 = (Graphics2D)layerList.getGraphics();
			g2.setColor(Color.RED);
			g2.drawRect(path.x, path.y, path.width-1, 1);
			lastDrawn = path;
		}

		public void dropActionChanged(DropTargetDragEvent e)
		{
			e.acceptDrag(e.getDropAction());
		}

		public void dragExit(DropTargetEvent e)
		{
			eraseDragImage();
		}

		public void drop(DropTargetDropEvent dtde)
		{
			dtde.acceptDrop(DnDConstants.ACTION_LINK);

			// erase former drawing
			eraseDragImage();

			// get the original index that was dragged
			String text = null;
			DataFlavor [] flavors = dtde.getCurrentDataFlavors();
			if (flavors.length > 0)
			{
				if (flavors[0].isFlavorTextType())
				{
					try {
						text = (String)dtde.getTransferable().getTransferData(flavors[0]);
					} catch (Exception e) {}
				}
			}
			if (text == null) return;
			int start = TextUtils.atoi(text);
			int end = layerList.locationToIndex(dtde.getLocation());
			Layer moveIt = layersInList.get(start);
			layersInList.remove(start);
			if (start < end) end--;
			layersInList.add(end, moveIt);

			layerListModel.clear();
			for(Layer layer : layersInList)
				layerListModel.addElement(lineName(layer));
			layerList.setSelectedIndex(end);
			Technology tech = Technology.getCurrent();
			lv.setSavedLayerOrder(tech, layersInList);

			dtde.dropComplete(false);
		}

		private void eraseDragImage()
		{
			if (lastDrawn == null) return;
			layerList.paintImmediately(lastDrawn);
			lastDrawn = null;
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
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
        jPanel3 = new javax.swing.JPanel();
        newConfiguration = new javax.swing.JButton();
        deleteConfiguration = new javax.swing.JButton();
        configurationPane = new javax.swing.JScrollPane();
        setConfiguration = new javax.swing.JButton();
        renameConfiguration = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        setName("");
        layerPane.setMinimumSize(new java.awt.Dimension(100, 150));
        layerPane.setOpaque(false);
        layerPane.setPreferredSize(new java.awt.Dimension(100, 150));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.75;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(layerPane, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        add(technology, gridBagConstraints);

        selectAll.setText("Select All");
        selectAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        add(selectAll, gridBagConstraints);

        makeVisible.setText("Visible");
        makeVisible.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                makeVisibleActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 1);
        add(makeVisible, gridBagConstraints);

        makeInvisible.setText("Invisible");
        makeInvisible.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                makeInvisibleActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 4);
        add(makeInvisible, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Highlighting"));
        unhighlightAll.setText("Clear");
        unhighlightAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unhighlightAllActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        jPanel1.add(unhighlightAll, gridBagConstraints);

        toggleHighlight.setText("Toggle");
        toggleHighlight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleHighlightActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        jPanel1.add(toggleHighlight, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        add(jPanel1, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Text Visibility"));
        cellText.setText("Cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(cellText, gridBagConstraints);

        arcText.setText("Arc");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(arcText, gridBagConstraints);

        annotationText.setText("Annotation");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(annotationText, gridBagConstraints);

        instanceNames.setText("Instance names");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(instanceNames, gridBagConstraints);

        exportText.setText("Export");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(exportText, gridBagConstraints);

        portText.setText("Port");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(portText, gridBagConstraints);

        nodeText.setText("Node");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        jPanel2.add(nodeText, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        add(jPanel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        add(opacitySlider, gridBagConstraints);

        resetOpacity.setText("Reset Opacity");
        resetOpacity.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetOpacityActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 1, 4);
        add(resetOpacity, gridBagConstraints);

        jPanel3.setLayout(new java.awt.GridBagLayout());

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Visibility Configurations"));
        newConfiguration.setText("N");
        newConfiguration.setToolTipText("Create New Visibility Configuration");
        newConfiguration.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newConfigurationActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.25;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        jPanel3.add(newConfiguration, gridBagConstraints);

        deleteConfiguration.setText("D");
        deleteConfiguration.setToolTipText("Delete or Reset Selected Visibility Configuration");
        deleteConfiguration.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteConfigurationActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.25;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        jPanel3.add(deleteConfiguration, gridBagConstraints);

        configurationPane.setMinimumSize(new java.awt.Dimension(100, 90));
        configurationPane.setPreferredSize(new java.awt.Dimension(100, 90));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel3.add(configurationPane, gridBagConstraints);

        setConfiguration.setText("S");
        setConfiguration.setToolTipText("Save Visibility in Currently Selected Configuration");
        setConfiguration.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setConfigurationActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.25;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        jPanel3.add(setConfiguration, gridBagConstraints);

        renameConfiguration.setText("R");
        renameConfiguration.setToolTipText("Rename Selected Visibility Configuration");
        renameConfiguration.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                renameConfigurationActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.25;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        jPanel3.add(renameConfiguration, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.25;
        add(jPanel3, gridBagConstraints);

    }// </editor-fold>//GEN-END:initComponents

    private void renameConfigurationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_renameConfigurationActionPerformed
    	renameConfiguration();
    }//GEN-LAST:event_renameConfigurationActionPerformed

    private void setConfigurationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setConfigurationActionPerformed
    	setConfiguration();
    }//GEN-LAST:event_setConfigurationActionPerformed

    private void deleteConfigurationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteConfigurationActionPerformed
    	deleteThisConfiguration();
    }//GEN-LAST:event_deleteConfigurationActionPerformed

    private void newConfigurationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newConfigurationActionPerformed
    	newConfiguration();
    }//GEN-LAST:event_newConfigurationActionPerformed

    private void resetOpacityActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_resetOpacityActionPerformed
    {//GEN-HEADEREND:event_resetOpacityActionPerformed
		String techName = (String)technology.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return;
		lv.resetOpacity(tech);
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox annotationText;
    private javax.swing.JCheckBox arcText;
    private javax.swing.JCheckBox cellText;
    private javax.swing.JScrollPane configurationPane;
    private javax.swing.JButton deleteConfiguration;
    private javax.swing.JCheckBox exportText;
    private javax.swing.JCheckBox instanceNames;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane layerPane;
    private javax.swing.JButton makeInvisible;
    private javax.swing.JButton makeVisible;
    private javax.swing.JButton newConfiguration;
    private javax.swing.JCheckBox nodeText;
    private javax.swing.JSlider opacitySlider;
    private javax.swing.JCheckBox portText;
    private javax.swing.JButton renameConfiguration;
    private javax.swing.JButton resetOpacity;
    private javax.swing.JButton selectAll;
    private javax.swing.JButton setConfiguration;
    private javax.swing.JComboBox technology;
    private javax.swing.JButton toggleHighlight;
    private javax.swing.JButton unhighlightAll;
    // End of variables declaration//GEN-END:variables

}
