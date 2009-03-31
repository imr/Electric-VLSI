/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GraphicsPreferences.java
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
package com.sun.electric.tool.user;

import com.sun.electric.database.Environment;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.id.LayerId;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.PrefPackage;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.prefs.Preferences;

/**
 *
 */
public class GraphicsPreferences extends PrefPackage {
    // In TECH_NODE
    private static final String KEY_PATTERN_DISPLAY = "UsePatternDisplay";
    private static final String KEY_PATTERN_PRINTER = "UsePatternPrinter";
    private static final String KEY_OUTLINE = "OutlinePattern";
    private static final String KEY_TRANSPARENT = "TransparentLayer";
    private static final String KEY_COLOR = "Color";
    private static final String KEY_OPACITY = "Opacity";
    private static final String KEY_PATTERN = "Pattern";
    private static final String KEY_TRANSPARENCY_MODE = "3DTransparencyMode";
    private static final String KEY_TRANSPARENCY_FACTOR = "3DTransparencyFactor";

    private final TechPool techPool;
    private final HashMap<LayerId,EGraphics> defaultGraphics;
    private final Color[] defaultColors;

    private GraphicsPreferences(GraphicsPreferences that,
            HashMap<LayerId,EGraphics> defaultGraphics,
            Color[] defaultColors) {
        super(that);
        this.techPool = that.techPool;
        this.defaultGraphics = defaultGraphics;
        this.defaultColors = defaultColors;
    }

    public GraphicsPreferences(boolean factory) {
        this(factory, Environment.getThreadTechPool());
    }

    public GraphicsPreferences(boolean factory, TechPool techPool) {
        super(factory);
        this.techPool = techPool;
        defaultGraphics = new HashMap<LayerId,EGraphics>();
        User.ColorPrefType[] colorPrefTypes = User.ColorPrefType.class.getEnumConstants();
        defaultColors = new Color[colorPrefTypes.length];
        for (int i = 0; i < colorPrefTypes.length; i++)
            defaultColors[i] = colorPrefTypes[i].getFactoryDefaultColor();
        if (factory) return;

        Preferences prefRoot = Pref.getPrefRoot();
        Preferences techPrefs = prefRoot.node(TECH_NODE);
        Preferences userPrefs = prefRoot.node(USER_NODE);
        for (Technology tech: techPool.values()) {
            for (Iterator<Layer> it = tech.getLayers(); it.hasNext(); ) {
                Layer layer = it.next();
                LayerId layerId = layer.getId();
                EGraphics factoryGraphics = layer.getFactoryGraphics();
                EGraphics graphics = factoryGraphics;

                // TECH_NODE
                String keyPatternDisplay = getKey(KEY_PATTERN_DISPLAY, layerId);
                boolean factoryPatternDisplay = factoryGraphics.isPatternedOnDisplay();
                boolean patternDisplay = techPrefs.getBoolean(keyPatternDisplay, factoryPatternDisplay);
                graphics = graphics.withPatternedOnDisplay(patternDisplay);

                String keyPatternPrinter = getKey(KEY_PATTERN_PRINTER, layerId);
                boolean factoryPatternPrinter = factoryGraphics.isPatternedOnPrinter();
                boolean patternPrinter = techPrefs.getBoolean(keyPatternPrinter, factoryPatternPrinter);
                graphics = graphics.withPatternedOnPrinter(patternPrinter);

                String keyOutline = getKey(KEY_OUTLINE, layerId);
                EGraphics.Outline factoryOutline = factoryGraphics.getOutlined();
                int outlineIndex = techPrefs.getInt(keyOutline, factoryOutline.getIndex());
                EGraphics.Outline outline = EGraphics.Outline.findOutline(outlineIndex);
                graphics = graphics.withOutlined(outline);

                String keyTransparent = getKey(KEY_TRANSPARENT, layerId);
                int factoryTransparent = factoryGraphics.getTransparentLayer();
                int transparent = techPrefs.getInt(keyTransparent, factoryTransparent);
                graphics = graphics.withTransparentLayer(transparent);

                String keyColor = getKey(KEY_COLOR, layerId);
                int factoryColorRgb = factoryGraphics.getRGB();
                int colorRgb = techPrefs.getInt(keyColor, factoryColorRgb);
                graphics = graphics.withColor(new Color(colorRgb));

                String keyOpacity = getKey(KEY_OPACITY, layerId);
                double factoryOpacity = factoryGraphics.getOpacity();
                double opacity = techPrefs.getDouble(keyOpacity, factoryOpacity);
                graphics = graphics.withOpacity(opacity);

                String keyPattern = getKey(KEY_PATTERN, layerId);
                String factoryPatternStr = factoryGraphics.getPatternString();
                String patternStr = techPrefs.get(keyPattern, factoryPatternStr);
                graphics = graphics.withPattern(patternStr);

                String keyTransparencyMode = getKey(KEY_TRANSPARENCY_MODE, layerId);
                String factoryTransparencyModeStr = factoryGraphics.getTransparencyMode().name();
                String transparencyModeStr = techPrefs.get(keyTransparencyMode, factoryTransparencyModeStr);
                EGraphics.J3DTransparencyOption transparencyMode = EGraphics.J3DTransparencyOption.valueOf(transparencyModeStr);
                graphics = graphics.withTransparencyMode(transparencyMode);

                String keyTransparencyFactor = getKey(KEY_TRANSPARENCY_FACTOR, layerId);
                double factoryTransparenceyFactor = factoryGraphics.getTransparencyFactor();
                double transparencyFactor = techPrefs.getDouble(keyTransparencyFactor, factoryTransparenceyFactor);
                graphics = graphics.withTransparencyFactor(transparencyFactor);

                if (graphics == factoryGraphics) continue;
                defaultGraphics.put(layerId, graphics);
            }
        }
        for (int i = 0; i < colorPrefTypes.length; i++) {
            User.ColorPrefType e = colorPrefTypes[i];
            int factoryRgb = e.getFactoryDefaultColor().getRGB() & 0xFFFFFF;
            int rgb = userPrefs.getInt(e.getPrefKey(), factoryRgb);
            if (rgb == factoryRgb) continue;
            defaultColors[i] = new Color(rgb);
        }
    }

    @Override
    public void putPrefs(Preferences prefRoot, boolean removeDefaults) {
        putPrefs(prefRoot, removeDefaults, null);
    }

    public void putPrefs(Preferences prefRoot, boolean removeDefaults, GraphicsPreferences oldGp) {
        super.putPrefs(prefRoot, removeDefaults);

        if (oldGp != null && oldGp.techPool != techPool)
            oldGp = null;

        Preferences techPrefs = prefRoot.node(TECH_NODE);
        Preferences userPrefs = prefRoot.node(USER_NODE);
        for (Technology tech: techPool.values()) {
            for (Iterator<Layer> it = tech.getLayers(); it.hasNext(); ) {
                Layer layer = it.next();
                LayerId layerId = layer.getId();
                EGraphics graphics = defaultGraphics.get(layerId);
                if (oldGp == null || graphics != oldGp.defaultGraphics.get(layerId)) {
                    EGraphics factoryGraphics = layer.getFactoryGraphics();
                    if (graphics == null)
                        graphics = factoryGraphics;

                    String keyPatternDisplay = getKey(KEY_PATTERN_DISPLAY, layerId);
                    if (removeDefaults && graphics.isPatternedOnDisplay() == factoryGraphics.isPatternedOnDisplay())
                        techPrefs.remove(keyPatternDisplay);
                    else
                        techPrefs.putBoolean(keyPatternDisplay, graphics.isPatternedOnDisplay());

                    String keyPatternPrinter = getKey(KEY_PATTERN_PRINTER, layerId);
                    if (removeDefaults && graphics.isPatternedOnPrinter() == factoryGraphics.isPatternedOnPrinter())
                        techPrefs.remove(keyPatternPrinter);
                    else
                        techPrefs.putBoolean(keyPatternPrinter, graphics.isPatternedOnPrinter());

                    String keyOutline = getKey(KEY_OUTLINE, layerId);
                    if (removeDefaults && graphics.getOutlined() == factoryGraphics.getOutlined())
                        techPrefs.remove(keyOutline);
                    else
                        techPrefs.putInt(keyOutline, graphics.getOutlined().getIndex());

                    String keyColor = getKey(KEY_COLOR, layerId);
                    if (removeDefaults && graphics.getColor().equals(factoryGraphics.getColor()))
                        techPrefs.remove(keyColor);
                    else
                        techPrefs.putInt(keyColor, graphics.getRGB());

                    String keyOpacity = getKey(KEY_OPACITY, layerId);
                    if (removeDefaults && graphics.getOpacity() == factoryGraphics.getOpacity())
                        techPrefs.remove(keyOpacity);
                    else
                        techPrefs.putDouble(keyOpacity, graphics.getOpacity());

                    String keyPattern = getKey(KEY_PATTERN, layerId);
                    if (removeDefaults && Arrays.equals(graphics.getPattern(), factoryGraphics.getPattern()))
                        techPrefs.remove(keyPattern);
                    else
                        techPrefs.put(keyPattern, graphics.getPatternString());

                    String keyTrancparencyMode = getKey(KEY_TRANSPARENCY_MODE, layerId);
                    if (removeDefaults && graphics.getTransparencyMode() == factoryGraphics.getTransparencyMode())
                        techPrefs.remove(keyTrancparencyMode);
                    else
                        techPrefs.put(keyTrancparencyMode, graphics.getTransparencyMode().name());

                    String keyTrancparencyFactor = getKey(KEY_TRANSPARENCY_FACTOR, layerId);
                    if (removeDefaults && graphics.getTransparencyFactor() == factoryGraphics.getTransparencyFactor())
                        techPrefs.remove(keyTrancparencyFactor);
                    else
                        techPrefs.putDouble(keyTrancparencyFactor, graphics.getTransparencyFactor());
                }
            }
        }
        if (oldGp == null || defaultColors != oldGp.defaultColors) {
            User.ColorPrefType[] colorPrefTypes = User.ColorPrefType.class.getEnumConstants();
            for (int i = 0; i < colorPrefTypes.length; i++) {
                User.ColorPrefType t = colorPrefTypes[i];
                if (removeDefaults && defaultColors[i].equals(t.getFactoryDefaultColor()))
                    userPrefs.remove(t.getPrefKey());
                else
                    userPrefs.putInt(t.getPrefKey(), defaultColors[i].getRGB() & 0xFFFFFF);
            }
        }
    }

    public GraphicsPreferences withLayerGraphics(LayerId layerId, EGraphics graphics) {
        Layer layer = techPool.getLayer(layerId);
        if (layer == null) return this;
        EGraphics currentGraphics = getGraphics(layer);
        if (currentGraphics.equals(graphics)) return this;
        HashMap<LayerId,EGraphics> newDefaultGraphics = new HashMap<LayerId,EGraphics>(defaultGraphics);
        if (graphics.equals(layer.getFactoryGraphics()))
            newDefaultGraphics.remove(layerId);
        else
            newDefaultGraphics.put(layerId, graphics);
        return new GraphicsPreferences(this,
                newDefaultGraphics,
                defaultColors);
    }

    public GraphicsPreferences withLayerColor(LayerId layerId, Color color) {
        Layer layer = techPool.getLayer(layerId);
        if (layer == null) return this;
        return withLayerGraphics(layerId, getGraphics(layer).withColor(color));
    }

    public GraphicsPreferences withLayersReset() {
        if (defaultGraphics.isEmpty()) return this;
        return new GraphicsPreferences(this,
                new HashMap<LayerId,EGraphics>(),
                defaultColors);
    }

    public GraphicsPreferences withColor(User.ColorPrefType t, Color color) {
        if (color.equals(defaultColors[t.ordinal()])) return this;
        Color[] newDefaultColors = defaultColors.clone();
        newDefaultColors[t.ordinal()] = color;
        return new GraphicsPreferences(this,
                defaultGraphics,
                newDefaultColors);
    }

    public GraphicsPreferences withFactoryColor(User.ColorPrefType t) {
        return withColor(t, t.getFactoryDefaultColor());
    }

    public EGraphics getDefaultGraphics(LayerId layerId) { return defaultGraphics.get(layerId); }
    public Color getColor(User.ColorPrefType t) { return defaultColors[t.ordinal()]; }
    private EGraphics getGraphics(Layer layer) {
        EGraphics graphics = getDefaultGraphics(layer.getId());
        return graphics != null ? graphics : layer.getFactoryGraphics();
    }
}
