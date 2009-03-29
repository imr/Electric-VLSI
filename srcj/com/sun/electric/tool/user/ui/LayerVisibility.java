/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayerVisibility.java
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.Environment;
import com.sun.electric.database.text.PrefPackage;
import com.sun.electric.database.text.PrefPackage;
import com.sun.electric.database.text.PrefPackage;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;

/**
 * Class represents visibility of Layers.
 * It is possible to have multiple instances of this class,
 * for example for each EditWindow.
 */
public class LayerVisibility extends PrefPackage {
    private static final String KEY_VISIBILITY = "Visibility";
    private static final String KEY_LAYER_ORDER = "LayerOrderfor";

    /**
     * The "standard" LayerVisibility
     */
    private static LayerVisibility stdLayerVisibility;

    private final TechPool techPool;
    private HashSet<Layer> invisibleLayers = new HashSet<Layer>();
    private HashSet<PrimitiveNode> visibleNodes;
    private HashSet<ArcProto> visibleArcs;
    private boolean visibilityChanged;
    private HashSet<Layer> highlightedLayers = new HashSet<Layer>();
    private HashMap<Technology,String> layerOrders = new HashMap<Technology,String>();

    public LayerVisibility(boolean factory) {
        this(factory, Environment.getThreadTechPool());
    }

    LayerVisibility(boolean factory, TechPool techPool) {
        super(factory);
        this.techPool = techPool;
        if (factory) return;

        Preferences techPrefs = getPrefRoot().node(TECH_NODE);
        for (Technology tech: techPool.values()) {
            String layerOrderKey = getKey(KEY_LAYER_ORDER, tech.getId());
            String layerOrder = techPrefs.get(layerOrderKey, "");
            if (layerOrder.length() > 0)
                layerOrders.put(tech, layerOrder);
            for (Iterator<Layer> it = tech.getLayers(); it.hasNext(); ) {
                Layer layer = it.next();
                assert !layer.isPseudoLayer();
                String visibilityKey = getKey(KEY_VISIBILITY, layer.getId());
                boolean visible = techPrefs.getBoolean(visibilityKey, true);
                if (!visible)
                    invisibleLayers.add(layer);
            }
        }
    }

    @Override
    public void putPrefs(Preferences prefRoot, boolean removeDefaults) {
        putPrefs(prefRoot, removeDefaults, null);
    }

    public void putPrefs(Preferences prefRoot, boolean removeDefaults, LayerVisibility oldLv) {
        super.putPrefs(prefRoot, removeDefaults);

        if (oldLv != null && oldLv.techPool != techPool)
            oldLv = null;

        Preferences techPrefs = prefRoot.node(TECH_NODE);
        for (Technology tech: techPool.values()) {
            String layerOrder = layerOrders.get(tech);
            if (layerOrder == null)
                layerOrder = "";
            String layerOrderKey = getKey(KEY_LAYER_ORDER, tech.getId());
            if (removeDefaults && layerOrder.length() == 0)
                techPrefs.remove(layerOrderKey);
            else
                techPrefs.put(layerOrderKey, layerOrder);

            for (Iterator<Layer> it = tech.getLayers(); it.hasNext(); ) {
                Layer layer = it.next();
                boolean visible = isVisible(layer);
                if (oldLv == null || visible != oldLv.isVisible(layer)) {
                    String visibilityKey = getKey(KEY_VISIBILITY, layer.getId());
                    if (removeDefaults && visible)
                        techPrefs.remove(visibilityKey);
                    else
                        techPrefs.putBoolean(visibilityKey, visible);
                }
            }
        }
    }

	/**
	 * Method to set whether this Layer is visible
     * @param layer the Layer
	 * @param visible true if the Layer is to be visible.
	 */
    void setVisible(Layer layer, boolean visible) {
        Map<Layer,Boolean> visibilityChange = Collections.singletonMap(layer, Boolean.valueOf(visible));
        setVisible(visibilityChange);
    }

	/**
	 * Method to update visibility of some Layers
     * @param visibilityChange a map with new values of Layers to update.
	 */
    void setVisible(Map<Layer,Boolean> visibilityChange) {
        HashSet<Layer> newInvisibleLayers = new HashSet<Layer>(invisibleLayers);
        boolean changed = false;
        for (Map.Entry<Layer,Boolean> e: visibilityChange.entrySet()) {
            Layer layer = e.getKey();
            boolean visible = e.getValue().booleanValue();
            if (visible == isVisible(layer)) continue;
            layer.getGraphics().notifyVisibility(e.getValue());
            changed = true;
           if (visible)
                newInvisibleLayers.remove(layer);
            else
                newInvisibleLayers.add(layer);
        }
        if (!changed) return;
        visibilityChanged = true;
        invisibleLayers = newInvisibleLayers;
        visibleNodes = null;
        visibleArcs = null;
    }

	/**
	 * Method to set whether this Layer is highlighted.
	 * Highlighted layers are drawn brighter.
     * @param layer the Layer
	 * @param highlighted true if the Layer is to be highlighteded.
	 */
    void setHighlighted(Layer layer, boolean highlighted) {
        if (highlighted == isHighlighted(layer)) return;
        visibilityChanged = true;
        HashSet<Layer> newHighlightedLayers = new HashSet<Layer>(highlightedLayers);
        if (highlighted)
            newHighlightedLayers.add(layer);
        else
            newHighlightedLayers.remove(layer);
        highlightedLayers = newHighlightedLayers;
    }

    /**
	 * Method to return a list of layers that are saved for specified Technology.
	 * The saved layers are used in the "Layers" tab (which can be user-rearranged).
     * @param tech specified Technology
	 * @return a list of layers for this Technology in the saved order.
	 */
	public List<Layer> getSavedLayerOrder(Technology tech)
	{
		String order = layerOrders.get(tech);
		if (order == null || order.length() == 0) return null;
		int pos = 0;
		List<Layer> layers = new ArrayList<Layer>();
		while (pos < order.length())
		{
			// get the next layer name in the string
			int end = order.indexOf(',', pos);
			if (end < 0) break;
			String layerName = order.substring(pos, end);
			pos = end + 1;

			// find the layer and add it to the list
			int colonPos = layerName.indexOf(':');
			Technology t = tech;
			if (colonPos >= 0)
			{
				String techName = layerName.substring(0, colonPos);
				t = techPool.findTechnology(techName);
				if (t == null) continue;
				layerName = layerName.substring(colonPos+1);
			}
			Layer layer = t.findLayer(layerName);
			if (layer != null)
				layers.add(layer);
		}
		return layers;
	}

	/**
	 * Method to save a list of layers for this Technology in a preferred order.
	 * This ordering is managed by the "Layers" tab which users can rearrange.
	 * @param layers a list of layers for this Technology in a preferred order.
	 */
	public void setSavedLayerOrder(Technology tech, List<Layer> layers)
	{
        HashMap<Technology,String> newLayerOrders = new HashMap<Technology,String>(layerOrders);
        if (layers.isEmpty()) {
            newLayerOrders.remove(tech);
        } else {
            StringBuffer sb = new StringBuffer();
    		for(Layer lay : layers)
        	{
            	if (lay.getTechnology() != tech) sb.append(lay.getTechnology().getTechName() + ":");
                sb.append(lay.getName() + ",");
            }
            newLayerOrders.put(tech, sb.toString());
        }
        visibilityChanged = true;
        layerOrders = newLayerOrders;
	}

    private void reset() {
        visibilityChanged = true;
        invisibleLayers = new HashSet<Layer>();
        visibleNodes = null;
        visibleArcs = null;
        highlightedLayers = new HashSet<Layer>();
        layerOrders = new HashMap<Technology,String>();
    }

    /**
     * Report change status and clear it
     * @return true if some Layer has changed visibility or highlight status
     */
    boolean clearChanged() {
        boolean oldChanged = visibilityChanged;
        visibilityChanged = false;
        return oldChanged;
    }

	/**
	 * Method to tell whether a Layer is visible.
     * @param layer specified layer
	 * @return true if this Layer is visible.
	 */
    public boolean isVisible(Layer layer) {
        return !invisibleLayers.contains(layer);
    }

	/**
	 * Method to tell whether a PrimitiveNode is visible.
     * @param pn specified PrimitiveNode
	 * @return true if the PrimitiveNode is visible.
	 */
    public boolean isVisible(PrimitiveNode pn) {
        if (visibleNodes == null)
            gatherVisiblePrims();
        return visibleNodes.contains(pn);
    }

	/**
	 * Method to tell whether an ArcProto is visible.
     * @param ap specified ArcProto
	 * @return true if the ArcProto is visible.
	 */
    public boolean isVisible(ArcProto ap) {
        if (visibleArcs == null)
            gatherVisiblePrims();
        return visibleArcs.contains(ap);
    }

	/**
	 * Method to tell whether a Layer is highlighted.
	 * Highlighted layers are drawn brighter
     * @param layer specified layer
	 * @return true if this Layer is highlighted.
	 */
    public boolean isHighlighted(Layer layer) {
        return highlightedLayers.contains(layer);
    }

    private void gatherVisiblePrims() {
        visibleNodes = new HashSet<PrimitiveNode>();
        visibleArcs = new HashSet<ArcProto>();
		for (Technology tech: techPool.values()) {
			for(Iterator<PrimitiveNode> nIt = tech.getNodes(); nIt.hasNext(); ) {
				PrimitiveNode pn = nIt.next();
                boolean visible = false;
                for (Iterator<Layer> lIt = pn.getLayerIterator(); lIt.hasNext(); ) {
                    Layer layer = lIt.next();
                    if (isVisible(layer)) {
                        visible = true;
                        break;
                    }
                }
                if (visible)
                    visibleNodes.add(pn);
            }
			for(Iterator<ArcProto> aIt = tech.getArcs(); aIt.hasNext(); ) {
				ArcProto ap = aIt.next();
				boolean visible = false;
				for(Iterator<Layer> lIt = ap.getLayerIterator(); lIt.hasNext(); ) {
					Layer layer = lIt.next();
					if (isVisible(layer)) {
                        visible = true;
                        break;
                    }
				}
                if (visible)
                    visibleArcs.add(ap);
			}
		}
    }

    /**
     * Returns "standard" LayerVisibility
     * @return "standard" LayerVisibility
     */
    public static LayerVisibility getLayerVisibility() {
        assert SwingUtilities.isEventDispatchThread();
        return stdLayerVisibility;
    }

    /**
     * Save "standard" LayerVisibility in Preferences
     */
    public static void preserveVisibility() {
        assert SwingUtilities.isEventDispatchThread();
        if (stdLayerVisibility == null) return;
        stdLayerVisibility.putPrefs(getPrefRoot(), true, null);
    }

    /**
     * Reset "standard" LayerVisibility to factory values.
     */
    public static void factoryReset() {
        assert SwingUtilities.isEventDispatchThread();
        if (stdLayerVisibility == null) return;
        stdLayerVisibility.reset();
    }

    /**
     * Reload standard LayerVisibility from Preferences
     * @param techPool new TechPool
     */
    public static void setTechPool(TechPool techPool) {
        assert SwingUtilities.isEventDispatchThread();
        if (stdLayerVisibility != null)
            stdLayerVisibility.putPrefs(getPrefRoot(), true, null);
        stdLayerVisibility = new LayerVisibility(false, techPool);
    }
}
