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

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.id.LayerId;
import com.sun.electric.database.text.PrefPackage;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;

import java.awt.Color;
import java.io.Serializable;
import java.util.Arrays;
import java.util.prefs.Preferences;

/**
 *
 */
public class GraphicsPreferences extends PrefPackage {
    // In TECH_NODE
    private static final String KEY_TRANSPARENT_COLOR = "TransparentLayer";

    private static final String KEY_PATTERN_DISPLAY = "UsePatternDisplayFor";
    private static final String KEY_PATTERN_PRINTER = "UsePatternPrinterFor";
    private static final String KEY_OUTLINE = "OutlinePatternFor";
    private static final String KEY_TRANSPARENT = "TransparentLayerFor";
    private static final String KEY_COLOR = "ColorFor";
    private static final String KEY_OPACITY = "OpacityFor";
    private static final String KEY_PATTERN = "PatternFor";
    private static final String KEY_TRANSPARENCY_MODE = "3DTransparencyModeOf";
    private static final String KEY_TRANSPARENCY_FACTOR = "3DTransparencyFactorOf";

    private static final String KEY_TEXT_VISIBILITY = "TextVisibility";

    public static final int RGB_MASK = 0xFFFFFF;

	/**
     * How to display ports.
	 * 0: full port names (the default).
	 * 1: short port names (stopping at the first nonalphabetic character).
	 * 2: ports drawn as crosses.
     */
    @IntegerPref(node = USER_NODE, key = "PortDisplayLevel", factory = DEF_PORT_DISPLAY_LEVEL)
    public int portDisplayLevel;
    private static final int DEF_PORT_DISPLAY_LEVEL = 0;
	/**
     * How to display exports.
	 * 0: full export names (the default).
	 * 1: short export names (stopping at the first nonalphabetic character).
	 * 2: exports drawn as crosses.
     */
    @IntegerPref(node = USER_NODE, key = "ExportDisplayLevel", factory = DEF_EXPORT_DISPLAY_LEVEL)
    public int exportDisplayLevel;
    private static final int DEF_EXPORT_DISPLAY_LEVEL = 0;

	/** The default font to use on the display. The default is "SansSerif". */
    @StringPref(node = USER_NODE, key = "DefaultFont", factory = FACTORY_DEFAULT_FONT)
    public String defaultFont;
    public static final String FACTORY_DEFAULT_FONT = "SansSerif";

    private transient final TechPool techPool;
    private final Color[] defaultColors;
    private final boolean[] textVisibility;
    private final TechData[] techData;

    private class TechData implements Serializable {
        private final Technology tech;
        private final Color[] transparentColors;
        private transient Color[] colorMap;
        private final EGraphics[] layerGraphics;

        private TechData(Technology tech, Preferences techPrefs) {
            this.tech = tech;
            assert techPool.getTech(tech.getId()) == tech;
            transparentColors = tech.getFactoryTransparentLayerColors();
            for (int i = 0; i < transparentColors.length; i++) {
                String key = getKey(KEY_TRANSPARENT_COLOR + (i+1) + "For", tech.getId());
                int factoryRgb = transparentColors[i].getRGB();
                int rgb = techPrefs.getInt(key, factoryRgb);
                if (rgb == factoryRgb) continue;
                transparentColors[i] = new Color(rgb);
            }
            layerGraphics = new EGraphics[tech.getNumLayers()];
            for (int layerIndex = 0; layerIndex < layerGraphics.length; layerIndex++) {
                Layer layer = tech.getLayer(layerIndex);
                LayerId layerId = layer.getId();
                EGraphics factoryGraphics = layer.getFactoryGraphics();
                EGraphics graphics = factoryGraphics;

                // TECH_NODE
                String keyPatternDisplay = getKeyIn(KEY_PATTERN_DISPLAY, layerId);
                boolean factoryPatternDisplay = factoryGraphics.isPatternedOnDisplay();
                boolean patternDisplay = techPrefs.getBoolean(keyPatternDisplay, factoryPatternDisplay);
                graphics = graphics.withPatternedOnDisplay(patternDisplay);

                String keyPatternPrinter = getKeyIn(KEY_PATTERN_PRINTER, layerId);
                boolean factoryPatternPrinter = factoryGraphics.isPatternedOnPrinter();
                boolean patternPrinter = techPrefs.getBoolean(keyPatternPrinter, factoryPatternPrinter);
                graphics = graphics.withPatternedOnPrinter(patternPrinter);

                String keyOutline = getKeyIn(KEY_OUTLINE, layerId);
                EGraphics.Outline factoryOutline = factoryGraphics.getOutlined();
                int outlineIndex = techPrefs.getInt(keyOutline, factoryOutline.getIndex());
                EGraphics.Outline outline = EGraphics.Outline.findOutline(outlineIndex);
                graphics = graphics.withOutlined(outline);

                String keyTransparent = getKeyIn(KEY_TRANSPARENT, layerId);
                int factoryTransparent = factoryGraphics.getTransparentLayer();
                int transparent = techPrefs.getInt(keyTransparent, factoryTransparent);
                graphics = graphics.withTransparentLayer(transparent);

                String keyColor = getKeyIn(KEY_COLOR, layerId);
                int factoryColorRgb = factoryGraphics.getRGB();
                int colorRgb = techPrefs.getInt(keyColor, factoryColorRgb);
                graphics = graphics.withColor(new Color(colorRgb));

                String keyOpacity = getKeyIn(KEY_OPACITY, layerId);
                double factoryOpacity = factoryGraphics.getOpacity();
                double opacity = techPrefs.getDouble(keyOpacity, factoryOpacity);
                graphics = graphics.withOpacity(opacity);

                String keyPattern = getKeyIn(KEY_PATTERN, layerId);
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

                layerGraphics[layerIndex] = graphics;
            }
        }

        private TechData(TechData that, Color[] transparentColors, EGraphics[] layerGraphics) {
            this.tech = that.tech;
            this.transparentColors = transparentColors;
            this.layerGraphics = layerGraphics;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o instanceof TechData) {
                TechData that = (TechData)o;
                return this.tech == that.tech &&
                        Arrays.equals(this.transparentColors, that.transparentColors) &&
                        Arrays.equals(this.layerGraphics, that.layerGraphics);
            }
            return false;
        }

        @Override
        public int hashCode() { return 0; }

        private void putPrefs(Preferences techPrefs, boolean removeDefaults, TechData oldTd) {
            if (oldTd != null && oldTd.tech != tech)
                oldTd = null;

            if (oldTd == null || transparentColors != oldTd.transparentColors) {
                Color[] factoryColors = tech.getFactoryTransparentLayerColors();
                for (int i = 0; i < factoryColors.length; i++) {
                    String key = getKey(KEY_TRANSPARENT_COLOR + (i+1) + "For", tech.getId());
                    int factoryRgb = factoryColors[i].getRGB();
                    int rgb = transparentColors[i].getRGB();
                    if (removeDefaults && rgb == factoryRgb)
                        techPrefs.remove(key);
                    else
                        techPrefs.putInt(key, rgb);
                }
            }

            assert layerGraphics.length == tech.getNumLayers();
            for (int layerIndex = 0; layerIndex < layerGraphics.length; layerIndex++) {
                Layer layer = tech.getLayer(layerIndex);
                LayerId layerId = layer.getId();
                EGraphics graphics = layerGraphics[layerIndex];
                if (oldTd == null || graphics != oldTd.layerGraphics[layerIndex]) {
                    EGraphics factoryGraphics = layer.getFactoryGraphics();

                    String keyPatternDisplay = getKeyIn(KEY_PATTERN_DISPLAY, layerId);
                    if (removeDefaults && graphics.isPatternedOnDisplay() == factoryGraphics.isPatternedOnDisplay())
                        techPrefs.remove(keyPatternDisplay);
                    else
                        techPrefs.putBoolean(keyPatternDisplay, graphics.isPatternedOnDisplay());

                    String keyPatternPrinter = getKeyIn(KEY_PATTERN_PRINTER, layerId);
                    if (removeDefaults && graphics.isPatternedOnPrinter() == factoryGraphics.isPatternedOnPrinter())
                        techPrefs.remove(keyPatternPrinter);
                    else
                        techPrefs.putBoolean(keyPatternPrinter, graphics.isPatternedOnPrinter());

                    String keyOutline = getKeyIn(KEY_OUTLINE, layerId);
                    if (removeDefaults && graphics.getOutlined() == factoryGraphics.getOutlined())
                        techPrefs.remove(keyOutline);
                    else
                        techPrefs.putInt(keyOutline, graphics.getOutlined().getIndex());

                    String keyTransparent = getKeyIn(KEY_TRANSPARENT, layerId);
                    if (removeDefaults && graphics.getTransparentLayer() == factoryGraphics.getTransparentLayer())
                        techPrefs.remove(keyTransparent);
                    else
                        techPrefs.putInt(keyTransparent, graphics.getTransparentLayer());

                    String keyColor = getKeyIn(KEY_COLOR, layerId);
                    if (removeDefaults && graphics.getColor().equals(factoryGraphics.getColor()))
                        techPrefs.remove(keyColor);
                    else
                        techPrefs.putInt(keyColor, graphics.getRGB());

                    String keyOpacity = getKeyIn(KEY_OPACITY, layerId);
                    if (removeDefaults && graphics.getOpacity() == factoryGraphics.getOpacity())
                        techPrefs.remove(keyOpacity);
                    else
                        techPrefs.putDouble(keyOpacity, graphics.getOpacity());

                    String keyPattern = getKeyIn(KEY_PATTERN, layerId);
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

        private TechData withTransparentColor(int transparentLayer, Color transparentColor) {
            if (transparentLayer < 0 || transparentLayer >= transparentColors.length)
                throw new IllegalArgumentException();
            if (transparentColor.equals(transparentColors[transparentLayer])) return this;
            Color[] newTransparentColors = transparentColors.clone();
            newTransparentColors[transparentLayer] = transparentColor;
            return new TechData(this, newTransparentColors, layerGraphics);
        }

        private TechData withTransparentColors(Color[] transparentColors) {
            if (Arrays.equals(this.transparentColors, transparentColors)) return this;
            Color[] factoryColors = tech.getFactoryTransparentLayerColors();
            if (transparentColors.length < factoryColors.length)
                throw new IllegalArgumentException();
            transparentColors = transparentColors.clone();
            for (Color color: transparentColors) {
                if (color.getAlpha() != 255)
                    throw new IllegalArgumentException();
            }
            return new TechData(this, transparentColors, layerGraphics);
        }

        private TechData withGraphics(Layer layer, EGraphics graphics) {
            assert layer.getTechnology() == tech;
            int layerIndex = layer.getIndex();
            EGraphics currentGraphics = layerGraphics[layerIndex];
            if (currentGraphics.equals(graphics)) return this;
            EGraphics[] newLayerGraphics = layerGraphics.clone();
            newLayerGraphics[layerIndex] = graphics;
            return new TechData(this, transparentColors, newLayerGraphics);
        }

        /**
         * Returns the color map for transparent layers in this technology.
         * @return the color map for transparent layers in this technology.
         * The number of entries in this map equals 2 to the power "getNumTransparentLayers()".
         */
        private Color[] getColorMap() {
            if (colorMap == null)
                colorMap = makeColorMap();
            return colorMap.clone();
        }

        private Color [] makeColorMap() {
            int numEntries = 1 << transparentColors.length;
            Color [] map = new Color[numEntries];
            for (int i = 0; i < numEntries; i++) {
                int r=200, g=200, b=200;
                boolean hasPrevious = false;
                for (int j = 0; j < transparentColors.length; j++) {
                    if ((i & (1<<j)) == 0) continue;
                    Color layerColor = transparentColors[j];
                    if (hasPrevious) {
                        // get the previous color
                        double [] lastColor = new double[3];
                        lastColor[0] = r / 255.0;
                        lastColor[1] = g / 255.0;
                        lastColor[2] = b / 255.0;
                        normalizeColor(lastColor);

                        // get the current color
                        double [] curColor = new double[3];
                        curColor[0] = layerColor.getRed() / 255.0;
                        curColor[1] = layerColor.getGreen() / 255.0;
                        curColor[2] = layerColor.getBlue() / 255.0;
                        normalizeColor(curColor);

                        // combine them
                        for(int k=0; k<3; k++) curColor[k] += lastColor[k];
                        normalizeColor(curColor);
                        r = (int)(curColor[0] * 255.0);
                        g = (int)(curColor[1] * 255.0);
                        b = (int)(curColor[2] * 255.0);
                    } else {
                        r = layerColor.getRed();
                        g = layerColor.getGreen();
                        b = layerColor.getBlue();
                        hasPrevious = true;
                    }
                }
                map[i] = new Color(r, g, b);
            }
            return map;
        }

        /**
         * Method to normalize a color stored in a 3-long array.
         * @param a the array of 3 doubles that holds the color.
         * All values range from 0 to 1.
         * The values are adjusted so that they are normalized.
         */
        private void normalizeColor(double [] a)
        {
            double mag = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
            if (mag < 1.0e-11f) return;
            a[0] /= mag;
            a[1] /= mag;
            a[2] /= mag;
        }
    }

    public GraphicsPreferences(boolean factory) {
        this(factory, TechPool.getThreadTechPool());
    }

    public GraphicsPreferences(boolean factory, TechPool techPool) {
        super(factory);
        this.techPool = techPool;
        int maxTechIndex = -1;
        for (Technology tech: techPool.values())
            maxTechIndex = Math.max(maxTechIndex, tech.getId().techIndex);
        techData = new TechData[maxTechIndex+1];

        Preferences prefRoot = factory ? getFactoryPrefRoot() : getPrefRoot();
        Preferences techPrefs = prefRoot.node(TECH_NODE);
        Preferences userPrefs = prefRoot.node(USER_NODE);

        for (Technology tech: techPool.values())
            techData[tech.getId().techIndex] = new TechData(tech, techPrefs);

        User.ColorPrefType[] colorPrefTypes = User.ColorPrefType.class.getEnumConstants();
        defaultColors = new Color[colorPrefTypes.length];
        for (int i = 0; i < colorPrefTypes.length; i++) {
            User.ColorPrefType e = colorPrefTypes[i];
            Color factoryColor = e.getFactoryDefaultColor();
            int factoryRgb = factoryColor.getRGB();
            int rgb = userPrefs.getInt(e.getPrefKey(), factoryRgb);
            Color color = rgb == factoryRgb ? factoryColor : new Color(rgb);
            defaultColors[i] = color;
        }

        TextDescriptor.TextType[] textTypes = TextDescriptor.TextType.class.getEnumConstants();
        textVisibility = new boolean[textTypes.length];
        for (int i = 0; i < textTypes.length; i++)
            textVisibility[i] = userPrefs.getBoolean(textTypes[i].getKey(KEY_TEXT_VISIBILITY), true);
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
        for (int techIndex = 0; techIndex < techData.length; techIndex++) {
            TechData td = techData[techIndex];
            if (td == null) continue;
            TechData oldTd = oldGp != null ? oldGp.techData[techIndex] : null;
            td.putPrefs(techPrefs, removeDefaults, oldTd);
        }

        if (oldGp == null || defaultColors != oldGp.defaultColors) {
            User.ColorPrefType[] colorPrefTypes = User.ColorPrefType.class.getEnumConstants();
            for (int i = 0; i < colorPrefTypes.length; i++) {
                User.ColorPrefType t = colorPrefTypes[i];
                if (removeDefaults && defaultColors[i].equals(t.getFactoryDefaultColor()))
                    userPrefs.remove(t.getPrefKey());
                else
                    userPrefs.putInt(t.getPrefKey(), defaultColors[i].getRGB());
            }
        }

        if (oldGp == null || textVisibility != oldGp.textVisibility) {
            TextDescriptor.TextType[] textTypes = TextDescriptor.TextType.class.getEnumConstants();
            for (int i = 0; i < textTypes.length; i++) {
                TextDescriptor.TextType t = textTypes[i];
                String key = t.getKey(KEY_TEXT_VISIBILITY);
                if (removeDefaults && textVisibility[i])
                    userPrefs.remove(key);
                else
                    userPrefs.putBoolean(key, textVisibility[i]);
            }
        }
    }

    public GraphicsPreferences withTransparentLayerColors(Technology tech, Color[] tranparentColors) {
        TechData td = getTechData(tech);
        return withTechData(td.withTransparentColors(tranparentColors));
    }

    public GraphicsPreferences withTransparentLayerColor(Technology tech, int transparentLayer, Color tranparentColor) {
        TechData td = getTechData(tech);
        return withTechData(td.withTransparentColor(transparentLayer, tranparentColor));
    }

    public GraphicsPreferences withGraphics(Layer layer, EGraphics graphics) {
        TechData td = getTechData(layer.getTechnology());
        return withTechData(td.withGraphics(layer, graphics));
    }

    public GraphicsPreferences withColor(User.ColorPrefType t, Color color) {
        if (color.equals(defaultColors[t.ordinal()])) return this;
        Color[] newDefaultColors = defaultColors.clone();
        newDefaultColors[t.ordinal()] = color;
        return (GraphicsPreferences)withField("defaultColors", newDefaultColors);
    }

    public GraphicsPreferences withFactoryColor(User.ColorPrefType t) { return withColor(t, t.getFactoryDefaultColor()); }

    public GraphicsPreferences withTextVisibilityOn(TextDescriptor.TextType t, boolean b) {
        if (b == textVisibility[t.ordinal()]) return this;
        boolean[] newTextVisibility = textVisibility.clone();
        newTextVisibility[t.ordinal()] = b;
        return (GraphicsPreferences)withField("textVisibility", newTextVisibility);
    }

    public GraphicsPreferences withPortDisplayLevel(int portDisplayLevel) {
        if (portDisplayLevel == this.portDisplayLevel) return this;
        return (GraphicsPreferences)withField("portDisplayLevel", Integer.valueOf(portDisplayLevel));
    }

    public GraphicsPreferences withExportDisplayLevel(int exportDisplayLevel) {
        if (exportDisplayLevel == this.exportDisplayLevel) return this;
        return (GraphicsPreferences)withField("exportDisplayLevel", Integer.valueOf(exportDisplayLevel));
    }

    public GraphicsPreferences withDisplayLevelReset() {
        return withPortDisplayLevel(DEF_PORT_DISPLAY_LEVEL).withExportDisplayLevel(DEF_EXPORT_DISPLAY_LEVEL);
    }

    public GraphicsPreferences withDefaultFont(String defaultFont) {
        if (defaultFont.equals(this.defaultFont)) return this;
        return (GraphicsPreferences)withField("defaultFont", defaultFont);
    }

	/**
	 * Returns the number of transparent layers in specified technology.
	 * Informs the display system of the number of overlapping or transparent layers
	 * in use.
     * @param tech specified Technology
	 * @return the number of transparent layers in specified technology.
	 * There may be 0 transparent layers in technologies that don't do overlapping,
	 * such as Schematics.
	 */
	public int getNumTransparentLayers(Technology tech) { return getTechData(tech).transparentColors.length; }

	/**
	 * Method to return the colors for the transparent layers in specified Technology.
     * @param tech specified Technoology
	 * @return the factory for the transparent layers in this Technology.
	 */
    public Color[] getTransparentLayerColors(Technology tech) { return getTechData(tech).transparentColors.clone(); }

	/**
	 * Returns the color map for transparent layers in specified technology.
     * @param tech specified Technology
	 * @return the color map for transparent layers in specified technology.
	 * The number of entries in this map equals 2 to the power "getNumTransparentLayers()".
	 */
	public Color [] getColorMap(Technology tech) { return getTechData(tech).getColorMap(); }

	/**
	 * Method to return the graphics description of specified Layer.
     * @param layer specified Layer
	 * @return the graphics description of specified Layer.
	 */
    public EGraphics getGraphics(Layer layer) { return getTechData(layer.getTechnology()).layerGraphics[layer.getIndex()]; }

	/**
	 * Method to get the color of a given special layer type on the display.
	 * @param t layer type in question.
	 * @return color of the special layer type.
	 */
    public Color getColor(User.ColorPrefType t) { return defaultColors[t.ordinal()]; }

	/**
	 * Method to tell whether to draw text of particular text type.
	 * The default is "true".
     * @param t specified text type.
	 * @return true if the system should text of specified type.
	 */
    public boolean isTextVisibilityOn(TextDescriptor.TextType t) { return textVisibility[t.ordinal()]; }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o instanceof GraphicsPreferences) {
            GraphicsPreferences that = (GraphicsPreferences)o;
            return this.techPool == that.techPool &&
                    Arrays.equals(this.techData, that.techData) &&
                    Arrays.equals(this.defaultColors, that.defaultColors) &&
                    Arrays.equals(this.textVisibility, that.textVisibility) &&
                    this.portDisplayLevel == that.portDisplayLevel &&
                    this.exportDisplayLevel == that.exportDisplayLevel &&
                    this.defaultFont.equals(that.defaultFont);
        }
        return false;
    }

    @Override
    public int hashCode() { return 0; }

    private GraphicsPreferences withTechData(TechData td) {
        int techIndex = td.tech.getId().techIndex;
        if (td == techData[techIndex]) return this;
        TechData[] newTechData = techData.clone();
        newTechData[techIndex] = td;
        return (GraphicsPreferences)withField("techData", newTechData);
    }

//    public GraphicsPreferences withLayersReset() {
//        if (defaultGraphics.isEmpty()) return this;
//        return new GraphicsPreferences(this,
//                new HashMap<LayerId,EGraphics>(),
//                defaultColors);
//    }

    private TechData getTechData(Technology tech) {
        TechData td = techData[tech.getId().techIndex];
        assert td.tech == tech;
        return td;
    }

    protected String getKeyIn(String what, LayerId layerId) {
        int len = what.length() + layerId.fullName.length() + 1;
        StringBuilder sb = new StringBuilder(len);
        sb.append(what);
        sb.append(layerId.name);
        sb.append("In");
        sb.append(layerId.techId.techName);
        assert sb.length() == len;
        return sb.toString();
    }
}
