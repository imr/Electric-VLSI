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

import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.PrefPackage;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.prefs.Preferences;

/**
 * Class represents visibility of Layers.
 */
public class LayerVisibility extends PrefPackage {
    public static final String KEY_VISIBILITY = "Visibility";
    
    private final TechPool techPool;
    private final HashMap<Layer,Layer> visibleLayers = new HashMap<Layer,Layer>();
    private HashSet<PrimitiveNode> visibleNodes;
    private HashSet<ArcProto> visibleArcs;
    
    LayerVisibility(boolean factory, TechPool techPool) {
        super(factory);
        this.techPool = techPool;
        for (Technology tech: techPool.values()) {
            for (Iterator<Layer> it = tech.getLayers(); it.hasNext(); ) {
                Layer layer = it.next();
                assert !layer.isPseudoLayer();
//                String visibilityKey = getKey(KEY_VISIBILITY, layer.getId());
                if (layer.isVisible())
                    visibleLayers.put(layer, layer);
            }
        }
    }

    boolean isVisible(Layer layer) {
        return visibleLayers.containsKey(layer);
    }
    
    boolean isVisible(PrimitiveNode pn) {
        if (visibleNodes == null)
            gatherVisiblePrims();
        return visibleNodes.contains(pn);
    }
    
    boolean isVisible(ArcProto ap) {
        if (visibleArcs == null)
            gatherVisiblePrims();
        return visibleArcs.contains(ap);
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
                    if (visibleLayers.containsKey(layer)) {
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
					if (layer.isVisible()) {
                        visible = true;
                        break;
                    }
				}
                if (visible)
                    visibleArcs.add(ap);
			}
		}
    }
    
    public static void preserveVisibility() {
		Pref.delayPrefFlushing();
        Preferences techPrefs = PrefPackage.getPrefRoot().node(PrefPackage);
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = lIt.next();
				Pref visPref = layer.layerVisibilityPref;
				boolean savedVis = visPref.getBoolean();
				if (savedVis != layer.visible)
				{
					visPref.setBoolean(layer.visible);
			        if (Job.getDebug()) System.err.println("Save visibility of " + layer.getName());
				}
			}
		}
		Pref.resumePrefFlushing();
    }
}
