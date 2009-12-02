/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayersTab.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.ColorPatternPanel;
import com.sun.electric.tool.user.ui.LayerVisibility;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

/**
 * Class to handle the "Colors and Layers" tab of the Preferences dialog.
 */
public class LayersTab extends PreferencePanel
{
	private Map<Layer,ColorPatternPanel.Info> layerMap;
	private Map<String,ColorPatternPanel.Info> transAndSpecialMap;
    private Map<User.ColorPrefType, String> nameTypeSpecialMap;
    private Map<Technology,Color []> colorMapMap;
	private ColorPatternPanel colorAndPatternPanel;

    private Layer defaultArtworkLayer;
    private final static String DEFAULT_ARTWORK = "Special: DEFAULT ARTWORK";

    private void resetColorPanelInfo(ColorPatternPanel.Info cpi)
    {
        int factoryColor = -1;
        if (cpi.graphics != null)
        {
            cpi.useStippleDisplay = cpi.graphics.isPatternedOnDisplay();
            cpi.useStipplePrinter = cpi.graphics.isPatternedOnPrinter();
            cpi.outlinePatternDisplay = cpi.graphics.getOutlined();
            cpi.transparentLayer = cpi.graphics.getTransparentLayer();
            cpi.pattern = cpi.graphics.getPattern();
            cpi.opacity = cpi.graphics.getOpacity();
            factoryColor = cpi.graphics.getColor().getRGB();  // color given by graphics for the rest of layers
        }
        else
            factoryColor = cpi.theColor.getFactoryDefaultColor().getRGB(); // factory color for special layers
        cpi.red = (factoryColor>>16) & 0xFF;
        cpi.green = (factoryColor>>8) & 0xFF;
        cpi.blue = factoryColor & 0xFF;
    }

	/** Creates new form LayerTab */
	public LayersTab(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// make the color/pattern panel
		colorAndPatternPanel = new ColorPatternPanel(true);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;      gbc.gridy = 1;
		gbc.weightx = 1;    gbc.weighty = 1;
		gbc.gridwidth = 4;  gbc.gridheight = 1;
		gbc.insets = new java.awt.Insets(4, 4, 4, 4);
		layers.add(colorAndPatternPanel, gbc);

		layerMap = new HashMap<Layer,ColorPatternPanel.Info>();
		transAndSpecialMap = new HashMap<String,ColorPatternPanel.Info>();
        nameTypeSpecialMap = new HashMap<User.ColorPrefType, String>();
        colorMapMap = new HashMap<Technology,Color []>();
		layerName.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { layerSelected(); }
		});
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			technology.addItem(tech.getTechName());
		}
		technology.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { setTechnology(); }
		});
	}

	/** return the panel to use for user preferences. */
	public JPanel getUserPreferencesPanel() { return layers; }

	/** return the name of this preferences tab. */
	public String getName() { return "Layers"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the tab.
	 */
	public void init()
	{
		// make a map of all layers
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = lIt.next();
				if (layer.isPseudoLayer() && layer.getNonPseudoLayer() != layer) continue;
//				layerName.addItem(layer.getName());
                if (tech instanceof Artwork) {
                    assert layer.getName().equals("Graphics");
                    defaultArtworkLayer = layer;
                }
//                ColorPatternPanel.Info li = new ColorPatternPanel.Info(layer.getGraphics());
//                layerMap.put(layer, li);
 			}

//			// make an entry for the technology's color map
//			Color [] map = tech.getTransparentLayerColors();
//			colorMapMap.put(tech, map);
		}

		// add the special layers
        nameTypeSpecialMap.put(User.ColorPrefType.BACKGROUND, "Special: BACKGROUND");
        nameTypeSpecialMap.put(User.ColorPrefType.GRID, "Special: GRID");
        nameTypeSpecialMap.put(User.ColorPrefType.HIGHLIGHT, "Special: HIGHLIGHT");
        nameTypeSpecialMap.put(User.ColorPrefType.NODE_HIGHLIGHT, "Special: NODE HIGHLIGHT");
        nameTypeSpecialMap.put(User.ColorPrefType.MOUSEOVER_HIGHLIGHT, "Special: MOUSE-OVER HIGHLIGHT");
        nameTypeSpecialMap.put(User.ColorPrefType.PORT_HIGHLIGHT, "Special: PORT HIGHLIGHT");
        nameTypeSpecialMap.put(User.ColorPrefType.TEXT, "Special: TEXT");
        nameTypeSpecialMap.put(User.ColorPrefType.INSTANCE, "Special: INSTANCE OUTLINES");
        nameTypeSpecialMap.put(User.ColorPrefType.DOWNINPLACEBORDER, "Special: DOWN-IN-PLACE BORDER");
        nameTypeSpecialMap.put(User.ColorPrefType.WAVE_BACKGROUND, "Special: WAVEFORM BACKGROUND");
        nameTypeSpecialMap.put(User.ColorPrefType.WAVE_FOREGROUND, "Special: WAVEFORM FOREGROUND");
        nameTypeSpecialMap.put(User.ColorPrefType.WAVE_STIMULI, "Special: WAVEFORM STIMULI");
        nameTypeSpecialMap.put(User.ColorPrefType.WAVE_OFF_STRENGTH, "Special: WAVEFORM OFF STRENGTH");
        nameTypeSpecialMap.put(User.ColorPrefType.WAVE_NODE_STRENGTH, "Special: WAVEFORM NODE (WEAK) STRENGTH");
        nameTypeSpecialMap.put(User.ColorPrefType.WAVE_GATE_STRENGTH, "Special: WAVEFORM GATE STRENGTH");
        nameTypeSpecialMap.put(User.ColorPrefType.WAVE_POWER_STRENGTH, "Special: WAVEFORM POWER STRENGTH");
        nameTypeSpecialMap.put(User.ColorPrefType.WAVE_CROSS_LOW, "Special: WAVEFORM CROSSPROBE LOW");
        nameTypeSpecialMap.put(User.ColorPrefType.WAVE_CROSS_HIGH, "Special: WAVEFORM CROSSPROBE HIGH");
        nameTypeSpecialMap.put(User.ColorPrefType.WAVE_CROSS_UNDEF, "Special: WAVEFORM CROSSPROBE UNDEFINED");
        nameTypeSpecialMap.put(User.ColorPrefType.WAVE_CROSS_FLOAT, "Special: WAVEFORM CROSSPROBE FLOATING");

        // 3D Stuff
        try
        {
            Class<?> j3DUtilsClass = Resources.get3DClass("utils.J3DUtils");
            if (j3DUtilsClass != null)
            {
                Method setMethod = j3DUtilsClass.getDeclaredMethod("get3DColorsInTab", new Class[] {Map.class});
                setMethod.invoke(j3DUtilsClass, new Object[]{nameTypeSpecialMap});
            }
//            else System.out.println("Cannot call 3D plugin method get3DColorsInTab");
        } catch (Exception e) {
            System.out.println("Error calling 3D plugin method get3DColorsInTab");
            e.printStackTrace();
        }

        cacheLayerInfo(false);
//        for (Map.Entry<User.ColorPrefType,String> e: nameTypeSpecialMap.entrySet())
//        {
//            User.ColorPrefType type = e.getKey();
//            String title = e.getValue();
//            transAndSpecialMap.put(title, new ColorPatternPanel.Info(type));
//        }

		technology.setSelectedItem(Technology.getCurrent().getTechName());
	}

	public void cacheLayerInfo(boolean redraw)
	{
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = lIt.next();
				if (layer.isPseudoLayer() && layer.getNonPseudoLayer() != layer) continue;
                ColorPatternPanel.Info li = new ColorPatternPanel.Info(layer.getGraphics());
                layerMap.put(layer, li);
 			}

			// make an entry for the technology's color map
			Color [] map = tech.getTransparentLayerColors();
			colorMapMap.put(tech, map);
		}
        for (Map.Entry<User.ColorPrefType,String> e: nameTypeSpecialMap.entrySet())
        {
            User.ColorPrefType type = e.getKey();
            String title = e.getValue();
            transAndSpecialMap.put(title, new ColorPatternPanel.Info(type));
        }
        if (redraw) layerSelected();
	}

	/**
	 * Method called when the Technology popup changes.
	 */
	private void setTechnology()
	{
		String techName = (String)technology.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return;

		// report the map for the technology
		Color [] map = colorMapMap.get(tech);
		colorAndPatternPanel.setColorMap(map);

		layerName.removeAllItems();

		// add all layers in the technology
		for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
		{
			Layer layer = lIt.next();
			if (layer.isPseudoLayer() && layer.getNonPseudoLayer() != layer) continue;
			layerName.addItem(layer.getName());
		}

		// add special layer names
		List<String> specialList = new ArrayList<String>();
        specialList.add(DEFAULT_ARTWORK);
		for(String name : transAndSpecialMap.keySet())
			specialList.add(name);
		Collections.sort(specialList, TextUtils.STRING_NUMBER_ORDER);
		for(String name : specialList)
		{
			layerName.addItem(name);
		}
		layerSelected();
	}

	/**
	 * Method called when the Layer popup changes.
	 */
	private void layerSelected()
	{
		String techName = (String)technology.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return;

		String name = (String)layerName.getSelectedItem();
        if (name == null) return;
		ColorPatternPanel.Info li = transAndSpecialMap.get(name);
		Layer layer = null;
		if (li == null)
		{
			layer = name.equals(DEFAULT_ARTWORK) ? defaultArtworkLayer : tech.findLayer(name);
			li = layerMap.get(layer);
		}
		if (li == null) return;
		colorAndPatternPanel.setColorPattern(li);

		// see if this layer is transparent and shares with another layer
		String otherLayers = null;
		if (li.transparentLayer > 0 && layer != null)
		{
			for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
			{
				Layer oLayer = it.next();
				if (oLayer == layer) continue;
				ColorPatternPanel.Info oLi = layerMap.get(oLayer);
				if (oLi != null && oLi.transparentLayer == li.transparentLayer)
				{
					if (otherLayers == null) otherLayers = oLayer.getName(); else
						otherLayers += ", " + oLayer.getName();
				}
			}
		}
		colorAndPatternPanel.setOtherTransparentLayerNames(otherLayers);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Layers tab.
	 */
	public void term()
	{
		boolean changed = false;
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = lIt.next();
				ColorPatternPanel.Info li = layerMap.get(layer);
				EGraphics graphics = layer.getGraphics();
				if (layer.isPseudoLayer())
				{
					ColorPatternPanel.Info altLI = layerMap.get(layer.getNonPseudoLayer());
					if (altLI != null) li = altLI;
				}
                EGraphics newGraphics = li.updateGraphics(graphics);
				if (newGraphics != graphics)
                {
					changed = true;
                    layer.setGraphics(newGraphics);
                }
			}

			// determine the original colors for this technology
			Color [] origMap = tech.getTransparentLayerColors();

			// see if any colors changed
			boolean mapChanged = false;
			Color [] map = colorMapMap.get(tech);
			for(int i=0; i<map.length; i++)
				if (map[i].getRGB() != origMap[i].getRGB()) mapChanged = true;
			if (mapChanged)
				tech.setColorMapFromLayers(map);
		}

		// also get any changes to special layers
        for (Map.Entry<User.ColorPrefType,String> e: nameTypeSpecialMap.entrySet())
        {
            User.ColorPrefType type = e.getKey();
            String title = e.getValue();
            int c = specialMapColor(title, User.getColor(type));
            if (c >= 0)
            {
                User.setColor(type, c);
                changed = true;
            }
        }
		// redisplay if changes were made
		if (changed)
		{
            WindowFrame.repaintAllWindows();
		}
	}

	/**
	 * Method called when the factory reset is requested for just this panel.
	 * @return true if the panel can be reset "in place" without redisplay.
	 */
	public boolean resetThis()
	{
		for(Layer layer : layerMap.keySet())
		{
			ColorPatternPanel.Info cpi = layerMap.get(layer);
            resetColorPanelInfo(cpi);
		}

		// Special layers
        for(ColorPatternPanel.Info cpi: transAndSpecialMap.values())
        {
            resetColorPanelInfo(cpi);
        }

        for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			Color [] map = tech.getFactoryTransparentLayerColors();
			colorMapMap.put(tech, map);
		}

		colorAndPatternPanel.setColorPattern(null);
		setTechnology();
		return true;
	}

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();

			// reload color map for technology if necessary
			Color [] transColorsFactory = tech.getFactoryTransparentLayerColors();
			Color [] transColors = tech.getTransparentLayerColors();
			boolean reload = transColorsFactory.length != transColors.length;
			if (!reload)
			{
				for(int i=0; i<transColors.length; i++)
					if (transColorsFactory[i].getRGB() != transColors[i].getRGB()) reload = true;
			}
			if (reload)
				tech.setColorMapFromLayers(transColorsFactory);

			// reload individual layer graphics
			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = lIt.next();
				if (layer.isPseudoLayer() && layer.getNonPseudoLayer() != layer) continue;
				layer.setGraphics(layer.getFactoryGraphics());
			}
		}
        LayerVisibility.factoryReset();

		// Special layers
		for (User.ColorPrefType type : nameTypeSpecialMap.keySet())
		{
			String name = nameTypeSpecialMap.get(type);
			ColorPatternPanel.Info cpi = transAndSpecialMap.get(name);
			int factory = cpi.theColor.getFactoryDefaultColor().getRGB() & 0xFFFFFF;
			if (factory != User.getColor(cpi.theColor))
			{
				User.setColor(type, factory);
			}
		}
	}

	public int specialMapColor(String title, int curColor)
	{
		ColorPatternPanel.Info li = transAndSpecialMap.get(title);
		if (li == null) return -1;
		int newColor = (li.red << 16) | (li.green << 8) | li.blue;
		if (newColor != curColor) return newColor;
		return -1;
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

        layers = new javax.swing.JPanel();
        layerName = new javax.swing.JComboBox();
        layerTechName = new javax.swing.JLabel();
        technology = new javax.swing.JComboBox();
        layerTechName1 = new javax.swing.JLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        layers.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        layers.add(layerName, gridBagConstraints);

        layerTechName.setText("Layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        layers.add(layerTechName, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        layers.add(technology, gridBagConstraints);

        layerTechName1.setText("Technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        layers.add(layerTechName1, gridBagConstraints);

        getContentPane().add(layers, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox layerName;
    private javax.swing.JLabel layerTechName;
    private javax.swing.JLabel layerTechName1;
    private javax.swing.JPanel layers;
    private javax.swing.JComboBox technology;
    // End of variables declaration//GEN-END:variables

}
