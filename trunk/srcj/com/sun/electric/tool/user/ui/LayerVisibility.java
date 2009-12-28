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

import com.sun.electric.database.text.PrefPackage;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Class represents visibility of Layers.
 * It is possible to have multiple instances of this class,
 * for example for each EditWindow.
 */
public class LayerVisibility extends PrefPackage {
    private static final String KEY_VISIBILITY = "VisibilityOf";
    private static final String KEY_LAYER_ORDER = "LayerOrderfor";

    /**
     * The "standard" LayerVisibility
     */
    private static LayerVisibility stdLayerVisibility;

    private final TechPool techPool;
    private HashSet<PrimitiveNode> visibleNodes;
    private HashSet<ArcProto> visibleArcs;
    private boolean visibilityChanged;
    private final TechData[] techData;

    private class TechData {
        private final Technology tech;
        private String layerOrder = "";
        private final boolean[] visibleLayers;
        private final boolean[] highlightedLayers;
        private final float[] layerDrawingOpacity;

        TechData(Technology tech) {
            this.tech = tech;
            assert techPool.getTech(tech.getId()) == tech;
            int numLayers = tech.getNumLayers();
            visibleLayers = new boolean[numLayers];
            Arrays.fill(visibleLayers, true);
            highlightedLayers = new boolean[numLayers];
            layerDrawingOpacity = new float[numLayers];
            setDefaultOpacity();
        }

        /**
         * Method to automatically set the opacity of each layer in a Technology.
         * @param tech the Technology to set.
         */
        private void setDefaultOpacity() {
            for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); ) {
                Layer layer = it.next();
                int layerIndex = layer.getIndex();
                float opacity = getDefaultOpacity(layer);
                if (opacity == layerDrawingOpacity[layerIndex]) continue;
                visibilityChanged = true;
                layerDrawingOpacity[layerIndex] = opacity;
            }
        }
    }

    public LayerVisibility(boolean factory) {
        this(factory, TechPool.getThreadTechPool());
    }

    LayerVisibility(boolean factory, TechPool techPool) {
        super(factory);
        this.techPool = techPool;
        int maxTechIndex = -1;
        for (Technology tech: techPool.values())
            maxTechIndex = Math.max(maxTechIndex, tech.getId().techIndex);
        techData = new TechData[maxTechIndex+1];
        for (Technology tech: techPool.values())
            techData[tech.getId().techIndex] = new TechData(tech);
        visibilityChanged = false;
        if (factory) return;

        Preferences techPrefs = getPrefRoot().node(TECH_NODE);
        for (Technology tech: techPool.values()) {
            TechData td = getTechData(tech);
            String layerOrderKey = getKey(KEY_LAYER_ORDER, tech.getId());
            td.layerOrder = techPrefs.get(layerOrderKey, "");
            for (Iterator<Layer> it = tech.getLayers(); it.hasNext(); ) {
                Layer layer = it.next();
                assert !layer.isPseudoLayer();
                String visibilityKey = getKey(KEY_VISIBILITY, layer.getId());
                td.visibleLayers[layer.getIndex()] = techPrefs.getBoolean(visibilityKey, true);
            }
        }
    }

    @Override
    public void putPrefs(Preferences prefRoot, boolean removeDefaults) {
        super.putPrefs(prefRoot, removeDefaults);

        Preferences techPrefs = prefRoot.node(TECH_NODE);
        for (Technology tech: techPool.values()) {
            TechData td = getTechData(tech);
            String layerOrder = td.layerOrder;
            String layerOrderKey = getKey(KEY_LAYER_ORDER, tech.getId());
            if (removeDefaults && layerOrder.length() == 0)
                techPrefs.remove(layerOrderKey);
            else
                techPrefs.put(layerOrderKey, layerOrder);

            for (Iterator<Layer> it = tech.getLayers(); it.hasNext(); ) {
                Layer layer = it.next();
                boolean visible = isVisible(layer);
                String visibilityKey = getKey(KEY_VISIBILITY, layer.getId());
                if (removeDefaults && visible)
                    techPrefs.remove(visibilityKey);
                else
                    techPrefs.putBoolean(visibilityKey, visible);
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
        boolean changed = false;
        for (Map.Entry<Layer,Boolean> e: visibilityChange.entrySet()) {
            Layer layer = e.getKey();
            TechData td = getTechData(layer.getTechnology());
            int layerIndex = layer.getIndex();
            boolean visible = e.getValue().booleanValue();
            if (visible == td.visibleLayers[layerIndex]) continue;
            changed = true;
            td.visibleLayers[layerIndex] = visible;
        }
        if (!changed) return;
        visibilityChanged = true;
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
        getTechData(layer.getTechnology()).highlightedLayers[layer.getIndex()] = highlighted;
    }

    /**
	 * Method to return a list of layers that are saved for specified Technology.
	 * The saved layers are used in the "Layers" tab (which can be user-rearranged).
     * @param tech specified Technology
	 * @return a list of layers for this Technology in the saved order.
	 */
	public List<Layer> getSavedLayerOrder(Technology tech)
	{
        TechData td = getTechData(tech);
		String order = td.layerOrder;
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
        TechData td = getTechData(tech);
        if (layers.isEmpty()) {
            td.layerOrder = "";
        } else {
            StringBuffer sb = new StringBuffer();
    		for(Layer lay : layers)
        	{
            	if (lay.getTechnology() != tech) sb.append(lay.getTechnology().getTechName() + ":");
                sb.append(lay.getName() + ",");
            }
            td.layerOrder = sb.toString();
        }
        visibilityChanged = true;
	}

    public void setOpacity(Layer layer, double opacity) {
        TechData td = getTechData(layer.getTechnology());
        float fOpacity = (float)opacity;
        int layerIndex = layer.getIndex();
        if (fOpacity == td.layerDrawingOpacity[layerIndex]) return;
        visibilityChanged = true;
        td.layerDrawingOpacity[layerIndex] = fOpacity;

    }

    private void reset() {
        visibilityChanged = true;
        for (TechData td: techData) {
            if (td == null) continue;
            Arrays.fill(td.visibleLayers, true);
            Arrays.fill(td.highlightedLayers, false);
            td.layerOrder = "";
        }
        visibleNodes = null;
        visibleArcs = null;
    }

    private void resetOpacity() {
        for (TechData td: techData) {
            if (td == null) continue;
            td.setDefaultOpacity();
        }
    }

    void resetOpacity(Technology tech) {
        getTechData(tech).setDefaultOpacity();
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
        TechData td = getTechData(layer.getTechnology());
        return td.visibleLayers[layer.getIndex()];
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
        TechData td = getTechData(layer.getTechnology());
        return td.highlightedLayers[layer.getIndex()];
    }

    public float getOpacity(Layer layer) {
        return getTechData(layer.getTechnology()).layerDrawingOpacity[layer.getIndex()];
    }

    private void gatherVisiblePrims() {
        HashSet<PrimitiveNode> visibleNodes = new HashSet<PrimitiveNode>();
        HashSet<ArcProto> visibleArcs = new HashSet<ArcProto>();
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
        this.visibleNodes = visibleNodes;
        this.visibleArcs = visibleArcs;
    }

    /**
     * Method to automatically compute the opacity of a layer.
     * @param layer the Layer to find opacity.
     */
    private float getDefaultOpacity(Layer layer)
    {
        Layer.Function fun = layer.getFunction();
        int extra = layer.getFunctionExtras();
        double opacity = 0.4;
        if (fun.isMetal())
        {
            opacity = 0.75 - fun.getLevel() * 0.05;
        } else if (fun.isContact())
        {
            if ((extra&Layer.Function.CONMETAL) != 0) opacity = 0.7; else
                opacity = 1;
        } else if (fun == Layer.Function.OVERGLASS)
        {
            opacity = 0.2;
        } else if (fun == Layer.Function.POLY1 ||
                   fun == Layer.Function.POLY2 ||
                   fun == Layer.Function.POLY3 ||
                   fun == Layer.Function.GATE ||
                   fun == Layer.Function.DIFF ||
                   fun == Layer.Function.DIFFP ||
                   fun == Layer.Function.DIFFN ||
                   fun == Layer.Function.WELL ||
                   fun == Layer.Function.WELLP ||
                   fun == Layer.Function.WELLN ||
                   fun == Layer.Function.IMPLANT ||
                   fun == Layer.Function.IMPLANTN ||
                   fun == Layer.Function.IMPLANTP) {
            // lowest level layers should all be opaque
            opacity = 1.0;
        } else if (layer == techPool.getGeneric().glyphLay) {
            opacity = 0.1;		// essential bounds
        }
        return (float)opacity;
    }

    private TechData getTechData(Technology tech) {
        TechData td = techData[tech.getId().techIndex];
        assert td.tech == tech;
        return td;
    }

    /**
     * Returns "standard" LayerVisibility
     * @return "standard" LayerVisibility
     */
    public static LayerVisibility getLayerVisibility() {
        assert Job.isClientThread();
        return stdLayerVisibility;
    }

    /**
     * Save "standard" LayerVisibility in Preferences
     */
    public static void preserveVisibility() {
        assert Job.isClientThread();
        if (stdLayerVisibility == null) return;
        stdLayerVisibility.putPrefs(getPrefRoot(), true);
    }

    /**
     * Reset "standard" LayerVisibility to factory values.
     */
    public static void factoryReset() {
        assert Job.isClientThread();
        if (stdLayerVisibility == null) return;
        stdLayerVisibility.reset();
    }

    /**
     * Reset "standard" LayerVisibility to factory values.
     */
    public static void setDefaultOpacity() {
        assert Job.isClientThread();
        if (stdLayerVisibility == null) return;
        stdLayerVisibility.resetOpacity();
    }

    /**
     * Reload standard LayerVisibility from Preferences
     * @param techPool new TechPool
     */
    public static void setTechPool(TechPool techPool) {
        preserveVisibility();
        stdLayerVisibility = new LayerVisibility(false, techPool);
    }
}
