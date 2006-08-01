/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EGraphics.java
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
package com.sun.electric.database.geometry;

import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.Job;
import com.sun.electric.technology.technologies.Schematics;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.lang.reflect.Method;

import javax.swing.ImageIcon;

/**
 * Class to define the appearance of a piece of geometry.
 */
public class EGraphics extends Observable
        implements Cloneable
{
	/**
	 * Class to define the type of outline around a stipple pattern.
	 */
	public static class Outline
	{
		private static final int SAMPLEWID = 60;
		private static final int SAMPLEHEI = 11;

		private String name;
		private String constName;
		private int pattern, len;
		private int thickness;
		private int index;
		private boolean solid;
		private ImageIcon sample;

		private static List<Outline> allOutlines = new ArrayList<Outline>();
		private static HashMap<Integer,Outline> outlineByIndex = new HashMap<Integer,Outline>();
		private static HashMap<String,Outline> outlineByName = new HashMap<String,Outline>();

		private Outline(String name, String constName, int pattern, int len, int thickness, int index)
		{
			this.name = name;
			this.constName = constName;
			this.pattern = pattern;
			this.len = len;
			this.thickness = thickness;
			this.index = index;
			this.solid = (pattern == -1);
			allOutlines.add(this);
			outlineByIndex.put(new Integer(index), this);
			outlineByName.put(name, this);

            if (!Job.BATCHMODE)
            {
                // construct a sample of this outline texture
                BufferedImage bi = new BufferedImage(SAMPLEWID+SAMPLEHEI, SAMPLEHEI, BufferedImage.TYPE_INT_RGB);
                int startX = SAMPLEHEI / 2;
                int startY = (SAMPLEHEI-thickness) / 2;
                for(int y=0; y<SAMPLEHEI; y++)
                    for(int x=0; x<SAMPLEWID+SAMPLEHEI; x++)
                        bi.setRGB(x, y, 0xFFFFFF);
                for(int x=0; x<SAMPLEWID+SAMPLEHEI; x++)
                {
                    bi.setRGB(x, 0, 0);
                    bi.setRGB(x, SAMPLEHEI-1, 0);
                }
                for(int y=0; y<SAMPLEHEI; y++)
                {
                    bi.setRGB(0, y, 0);
                    bi.setRGB(SAMPLEWID+SAMPLEHEI-1, y, 0);
                }
                for(int y=0; y<thickness; y++)
                {
                    int patPos = 0;
                    for(int x=0; x<SAMPLEWID; x++)
                    {
                        if ((pattern & (1<<patPos)) != 0) bi.setRGB(x+startX, y+startY, 0); else
                            bi.setRGB(x+startX, y+startY, 0xFFFFFF);
                        patPos++;
                        if (patPos >= len) patPos = 0;
                    }
                }
                sample = new ImageIcon(bi);
            }
		}

		public String getName() { return name; }

		public String getConstName() { return constName; }

		public int getIndex() { return index; }

		public boolean isSolidPattern() { return solid; }

		public int getPattern() { return pattern; }

		public int getLen() { return len; }

		public int getThickness() { return thickness; }

		public ImageIcon getSample() { return sample; }

		public static Outline findOutline(int index)
		{
			Outline o = outlineByIndex.get(new Integer(index));
			return o;
		}

		public static Outline findOutline(String name)
		{
			Outline o = outlineByName.get(name);
			return o;
		}

		public static List<Outline> getOutlines() { return allOutlines; }

		public String toString() { return name; }

		/** Draw stipple pattern with no outline. */
		public final static Outline NOPAT      = new Outline("None", "NOPAT", 0, 32, 1, 0);
		/** Draw stipple pattern with solid outline. */
		public final static Outline PAT_S      = new Outline("Solid", "PAT_S", -1, 32, 1, 1);
		/** Draw stipple pattern with solid thick outline. */
		public final static Outline PAT_T1     = new Outline("Solid-Thick", "PAT_T1", -1, 32, 3, 2);
		/** Draw stipple pattern with solid thicker outline. */
		public final static Outline PAT_T2     = new Outline("Solid-Thicker", "PAT_T2", -1, 32, 5, 3);
		/** Draw stipple pattern with close dotted outline. */
		public final static Outline PAT_DO1    = new Outline("Dotted-Close", "PAT_DO1", 0x55, 8, 1, 4);
		/** Draw stipple pattern with far dotted outline. */
		public final static Outline PAT_DO2    = new Outline("Dotted-Far", "PAT_DO2", 0x11, 8, 1, 5);
		/** Draw stipple pattern with short dashed outline. */
		public final static Outline PAT_DA1    = new Outline("Dashed-Short", "PAT_DA1", 0x33, 8, 1, 6);
		/** Draw stipple pattern with long dashed outline. */
		public final static Outline PAT_DA2    = new Outline("Dashed-Long", "PAT_DA2", 0xF, 6, 1, 7);
		/** Draw stipple pattern with short dotted-dashed outline. */
		public final static Outline PAT_DD1    = new Outline("Dotted-Dashed-Short", "PAT_DD1", 0x39, 8, 1, 8);
		/** Draw stipple pattern with long dotted-dashed outline. */
		public final static Outline PAT_DD2    = new Outline("Dotted-Dashed-Long", "PAT_DD2", 0xF3, 10, 1, 9);
		/** Draw stipple pattern with close dotted thick outline. */
		public final static Outline PAT_DO1_T1 = new Outline("Dotted-Close-Thick", "PAT_DO1_T1", 0xF, 6, 3, 10);
		/** Draw stipple pattern with far dotted thick outline. */
		public final static Outline PAT_DO2_T1 = new Outline("Dotted-Far-Thick", "PAT_DO2_T1", 0xF, 8, 3, 11);
		/** Draw stipple pattern with dashed thick outline. */
		public final static Outline PAT_DA1_T1 = new Outline("Dashed-Thick", "PAT_DA1_T1", 0x1FFFF, 19, 3, 12);
		/** Draw stipple pattern with close dotted thicker outline. */
		public final static Outline PAT_DO1_T2 = new Outline("Dotted-Close-Thicker", "PAT_DO1_T2", 0x1F, 8, 5, 13);
		/** Draw stipple pattern with far dotted thicker outline. */
		public final static Outline PAT_DO2_T2 = new Outline("Dotted-Far-Thicker", "PAT_DO2_T2", 0x7F, 9, 5, 14);
	}

	/** the Layer associated with this graphics. */			private Layer layer;
	/** display: true to use patterns; false for solid */	private boolean displayPatterned;
	/** printer: true to use patterns; false for solid */	private boolean printPatterned;
	/** the outline pattern */								private Outline patternOutline;
	/** transparent layer to use (0 for none) */			private int transparentLayer;
	/** color to use */										private int red, green, blue;
	/** opacity (0 to 1) of color */						private double opacity;
	/** whether to draw color in foregound */				private boolean foreground;
	/** stipple pattern to draw */							private int [] pattern;
    /** stipple pattern to draw with proper bit order */    private int [] reversedPattern;
	/** 3D appearance */                                    private Object appearance3D;

	private static HashMap<Layer,Pref> usePatternDisplayMap = new HashMap<Layer,Pref>();
	private static HashMap<Layer,Pref> usePatternPrinterMap = new HashMap<Layer,Pref>();
	private static HashMap<Layer,Pref> outlinePatternMap = new HashMap<Layer,Pref>();
	private static HashMap<Layer,Pref> transparentLayerMap = new HashMap<Layer,Pref>();
	private static HashMap<Layer,Pref> opacityMap = new HashMap<Layer,Pref>();
	private static HashMap<Layer,Pref> colorMap = new HashMap<Layer,Pref>();
	private static HashMap<Layer,Pref> patternMap = new HashMap<Layer,Pref>();

	/**
	 * There are 3 ways to encode color in an integer.
	 * If the lowest bit (FULLRGBBIT) is set, this is a full RGB color in the high 3 bytes.
	 * If the next lowest bit (OPAQUEBIT) is set, this is an "old C Electric" opaque color
	 * (such as WHITE, BLACK, etc., listed below).
	 * If neither of these bits is set, this is a transparent layer
	 * (LAYERT1, LAYERT2, etc., listed below).
	 */
	/** Describes the full RGB escape bit. */				public final static int FULLRGBBIT = 01;
	/** Describes opaque color escape bit. */				public final static int OPAQUEBIT =  02;

	// the opaque colors (all have the OPAQUEBIT set in them)
	/** Describes the color white. */						public final static int WHITE =    0002;
	/** Describes the color black. */						public final static int BLACK =    0006;
	/** Describes the color red. */							public final static int RED =      0012;
	/** Describes the color blue. */						public final static int BLUE =     0016;
	/** Describes the color green. */						public final static int GREEN =    0022;
	/** Describes the color cyan. */						public final static int CYAN =     0026;
	/** Describes the color magenta. */						public final static int MAGENTA =  0032;
	/** Describes the color yellow. */						public final static int YELLOW =   0036;
	/** Describes the cell and port names. */				public final static int CELLTXT =  0042;
	/** Describes the cell outline. */						public final static int CELLOUT =  0046;
	/** Describes the window border color. */				public final static int WINBOR =   0052;
	/** Describes the highlighted window border color. */	public final static int HWINBOR =  0056;
	/** Describes the menu border color. */					public final static int MENBOR =   0062;
	/** Describes the highlighted menu border color. */		public final static int HMENBOR =  0066;
	/** Describes the menu text color. */					public final static int MENTXT =   0072;
	/** Describes the menu glyph color. */					public final static int MENGLY =   0076;
	/** Describes the cursor color. */						public final static int CURSOR =   0102;
	/** Describes the color gray. */						public final static int GRAY =     0106;
	/** Describes the color orange. */						public final static int ORANGE =   0112;
	/** Describes the color purple. */						public final static int PURPLE =   0116;
	/** Describes the color brown. */						public final static int BROWN =    0122;
	/** Describes the color light gray. */					public final static int LGRAY =    0126;
	/** Describes the color dark gray. */					public final static int DGRAY =    0132;
	/** Describes the color light red. */					public final static int LRED =     0136;
	/** Describes the color dark red. */					public final static int DRED =     0142;
	/** Describes the color light green. */					public final static int LGREEN =   0146;
	/** Describes the color dark green. */					public final static int DGREEN =   0152;
	/** Describes the color light blue. */					public final static int LBLUE =    0156;
	/** Describes the color dark blue. */					public final static int DBLUE =    0162;

	// the transparent layers
	/** Describes transparent layer 1. */					public final static int LAYERT1 =      04;
	/** Describes transparent layer 2. */					public final static int LAYERT2 =     010;
	/** Describes transparent layer 3. */					public final static int LAYERT3 =     020;
	/** Describes transparent layer 4. */					public final static int LAYERT4 =     040;
	/** Describes transparent layer 5. */					public final static int LAYERT5 =    0100;
	/** Describes transparent layer 6. */					public final static int LAYERT6 =    0200;
	/** Describes transparent layer 7. */					public final static int LAYERT7 =    0400;
	/** Describes transparent layer 8. */					public final static int LAYERT8 =   01000;
	/** Describes transparent layer 9. */					public final static int LAYERT9 =   02000;
	/** Describes transparent layer 10. */					public final static int LAYERT10 =  04000;
	/** Describes transparent layer 11. */					public final static int LAYERT11 = 010000;
	/** Describes transparent layer 12. */					public final static int LAYERT12 = 020000;

	// Constants used in technologies and in creating an EGraphics
	/** defines the 1st transparent layer. */				public static final int TRANSPARENT_1  =  1;
	/** defines the 2nd transparent layer. */				public static final int TRANSPARENT_2  =  2;
	/** defines the 3rd transparent layer. */				public static final int TRANSPARENT_3  =  3;
	/** defines the 4th transparent layer. */				public static final int TRANSPARENT_4  =  4;
	/** defines the 5th transparent layer. */				public static final int TRANSPARENT_5  =  5;
	/** defines the 6th transparent layer. */				public static final int TRANSPARENT_6  =  6;
	/** defines the 7th transparent layer. */				public static final int TRANSPARENT_7  =  7;
	/** defines the 8th transparent layer. */				public static final int TRANSPARENT_8  =  8;
	/** defines the 9th transparent layer. */				public static final int TRANSPARENT_9  =  9;
	/** defines the 10th transparent layer. */				public static final int TRANSPARENT_10 = 10;
	/** defines the 11th transparent layer. */				public static final int TRANSPARENT_11 = 11;
	/** defines the 12th transparent layer. */				public static final int TRANSPARENT_12 = 12;

	/**
	 * Method to create a graphics object.
	 * @param displayPatterned true if drawn with a pattern on the display.
	 * @param printPatterned true if drawn with a pattern on a printer.
	 * @param outlineWhenPatterned the outline texture to use when patterned.
	 * @param transparentLayer the transparent layer number (0 for none).
	 * @param red the red component of this EGraphics.
	 * @param green the green component of this EGraphics.
	 * @param blue the blue component of this EGraphics.
	 * @param opacity the opacity of this EGraphics (1 for opaque, 0 for transparent).
	 * @param foreground the foreground factor of this EGraphics (1 for to be in foreground).
	 * @param pattern the 16x16 stipple pattern of this EGraphics (16 integers).
	 */
	public EGraphics(boolean displayPatterned, boolean printPatterned, Outline outlineWhenPatterned,
		int transparentLayer, int red, int green, int blue, double opacity, boolean foreground, int[] pattern)
	{
		this.layer = null;
		this.displayPatterned = displayPatterned;
		this.printPatterned = printPatterned;
		this.patternOutline = (outlineWhenPatterned != null) ? outlineWhenPatterned : Outline.NOPAT;
		this.transparentLayer = transparentLayer;
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.opacity = opacity;
		this.foreground = foreground;
        setPatternLow(pattern);
		if (transparentLayer < 0 || transparentLayer > TRANSPARENT_12)
		{
			System.out.println("Graphics transparent color bad: " + transparentLayer);
		}
		if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255)
		{
			System.out.println("Graphics color bad: (" + red + "," + green + "," + blue + ")");
		}
	}

	/**
	 * Method to easily copy graphics between similar layers
	 * @param g graphics to copy data from
	 */
	public EGraphics(EGraphics g)
	{
		this.layer = null;
		this.displayPatterned = g.isPatternedOnDisplay();
		this.printPatterned = g.isPatternedOnPrinter();
		this.patternOutline = g.getOutlined();
		this.transparentLayer = g.getTransparentLayer();
		Color gColor = g.getColor();
		this.red = gColor.getRed();
		this.green = gColor.getGreen();
		this.blue = gColor.getBlue();
		this.opacity = g.getOpacity();
		this.foreground = g.getForeground();
		setPatternLow((int[])g.getPattern().clone());
		if (transparentLayer < 0 || transparentLayer > TRANSPARENT_12)
		{
			System.out.println("Graphics transparent color bad: " + transparentLayer);
		}
		if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255)
		{
			System.out.println("Graphics color bad: (" + red + "," + green + "," + blue + ")");
		}
	}
    
    private static void setPattern() {
        
    }

	/**
	 * Method to return the Layer associated with this EGraphics.
	 * @return the Layer associated with this EGraphics.
	 */
	public Layer getLayer() { return layer; }

	/**
	 * Method to set which Layer is associated with this EGraphics.
	 * Since this is called only during initialization, it also examines preferences
	 * and uses them to override the graphics information.
	 * @param layer the Layer to associate with this EGraphics.
	 */
	public void setLayer(Layer layer)
	{
		this.layer = layer;
		Technology tech = layer.getTechnology();
		if (tech == null) return;
        String layerTechMsg = layer.getName() + "In" + tech.getTechName();

		Pref usePatternDisplayPref = Pref.makeBooleanPref("UsePatternDisplayFor" + layerTechMsg,
			Technology.getTechnologyPreferences(), displayPatterned);
		displayPatterned = usePatternDisplayPref.getBoolean();
		usePatternDisplayMap.put(layer, usePatternDisplayPref);

		Pref usePatternPrinterPref = Pref.makeBooleanPref("UsePatternPrinterFor" + layerTechMsg,
			Technology.getTechnologyPreferences(), printPatterned);
		printPatterned = usePatternPrinterPref.getBoolean();
		usePatternPrinterMap.put(layer, usePatternPrinterPref);

		// read any previous outline information and apply it
		Pref oldOutlinePatternDisplayPref = Pref.makeBooleanPref("OutlinePatternDisplayFor" + layerTechMsg,
			Technology.getTechnologyPreferences(), false);
		if (oldOutlinePatternDisplayPref.getBoolean()) patternOutline = Outline.PAT_S;

		Pref outlinePatternPref = Pref.makeIntPref("OutlinePatternFor" + layerTechMsg,
				Technology.getTechnologyPreferences(), patternOutline.index);
		patternOutline = Outline.findOutline(outlinePatternPref.getInt());
		outlinePatternMap.put(layer, outlinePatternPref);

		Pref transparentLayerPref = Pref.makeIntPref("TransparentLayerFor" + layerTechMsg,
			Technology.getTechnologyPreferences(), transparentLayer);
		transparentLayer = transparentLayerPref.getInt();
		transparentLayerMap.put(layer, transparentLayerPref);

		Pref opacityPref = Pref.makeDoublePref("OpacityFor" + layerTechMsg,
			Technology.getTechnologyPreferences(), opacity);
		opacity = opacityPref.getDouble();
		opacityMap.put(layer, opacityPref);
		
		Pref colorPref = Pref.makeIntPref("ColorFor" + layerTechMsg,
			Technology.getTechnologyPreferences(), (red<<16) | (green << 8) | blue);
		int color = colorPref.getInt();
		red = (color >> 16) & 0xFF;
		green = (color >> 8) & 0xFF;
		blue = color & 0xFF;
		colorMap.put(layer, colorPref);

		String pat = makePatString(pattern);
		Pref patternPref = Pref.makeStringPref("PatternFor" + layerTechMsg,
			Technology.getTechnologyPreferences(), pat);
		pat = patternPref.getString();
		parsePatString(pat, pattern);
        setPatternLow(pattern);
		patternMap.put(layer, patternPref);
	}

	/**
	 * Method to recache the graphics information from the preferences.
	 * Called after new preferences have been imported.
	 */
	public void recachePrefs()
	{
		Technology tech = layer.getTechnology();
		if (tech == null) return;

		Pref usePatternDisplayPref = usePatternDisplayMap.get(layer);
		displayPatterned = usePatternDisplayPref.getBoolean();

		Pref usePatternPrinterPref = usePatternPrinterMap.get(layer);
		printPatterned = usePatternPrinterPref.getBoolean();

		Pref outlinePatternPref = outlinePatternMap.get(layer);
		patternOutline = Outline.findOutline(outlinePatternPref.getInt());

		Pref transparentLayerPref = transparentLayerMap.get(layer);
		transparentLayer = transparentLayerPref.getInt();

		Pref opacityPref = opacityMap.get(layer);
		opacity = opacityPref.getDouble();
		
		Pref colorPref = colorMap.get(layer);
		int color = colorPref.getInt();
		red = (color >> 16) & 0xFF;
		green = (color >> 8) & 0xFF;
		blue = color & 0xFF;

		Pref patternPref = patternMap.get(layer);
		String pat = patternPref.getString();
		parsePatString(pat, pattern);
        setPatternLow(pattern);
	}

	private String makePatString(int [] pattern)
	{
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<16; i++)
		{
			if (i > 0) sb.append("/");
			sb.append(Integer.toString(pattern[i]));
		}
		return sb.toString();
	}

	private void parsePatString(String patString, int [] pattern)
	{
		int pos = 0;
		for(int i=0; i<16; i++)
		{
			pattern[i] = TextUtils.atoi(patString.substring(pos));
			pos = patString.indexOf('/', pos) + 1;
		}
	}

	/**
	 * Method describes how this EGraphics appears on a display.
	 * This EGraphics can be drawn as a solid fill or as a pattern.
	 * @return true to draw this EGraphics patterned on a display.
	 * False to draw this EGraphics as a solid fill on a display.
	 */
	public boolean isPatternedOnDisplay() { return displayPatterned; }

	/**
	 * Method describes how this EGraphics appears on a display by factory default.
	 * This EGraphics can be drawn as a solid fill or as a pattern.
	 * @return true to draw this EGraphics patterned on a display by factory default.
	 * False to draw this EGraphics as a solid fill on a display by factory default.
	 */
	public boolean isFactoryPatternedOnDisplay()
	{
		if (layer != null)
		{
			Pref pref = usePatternDisplayMap.get(layer);
			return pref.getBooleanFactoryValue();
		}
		return false;
	}

	/**
	 * Method to set how this EGraphics appears on a display.
	 * This EGraphics can be drawn as a solid fill or as a pattern.
	 * @param p true to draw this EGraphics patterned on a display.
	 * False to draw this EGraphics as a solid fill on a display.
	 */
	public void setPatternedOnDisplay(boolean p)
	{
		displayPatterned = p;

		if (layer != null)
		{
			Pref pref = usePatternDisplayMap.get(layer);
			if (pref != null) pref.setBoolean(p);
		}
	}

	/**
	 * Method describes how this EGraphics appears on a printer.
	 * This EGraphics can be drawn as a solid fill or as a pattern.
	 * @return true to draw this EGraphics patterned on a printer.
	 * False to draw this EGraphics as a solid fill on a printer.
	 */
	public boolean isPatternedOnPrinter() { return printPatterned; }

	/**
	 * Method describes how this EGraphics appears on a printer by factory default.
	 * This EGraphics can be drawn as a solid fill or as a pattern.
	 * @return true to draw this EGraphics patterned on a printer by factory default.
	 * False to draw this EGraphics as a solid fill on a printer by factory default.
	 */
	public boolean isFactoryPatternedOnPrinter()
	{
		if (layer != null)
		{
			Pref pref = usePatternPrinterMap.get(layer);
			return pref.getBooleanFactoryValue();
		}
		return false;
	}

	/**
	 * Method to set how this EGraphics appears on a printer.
	 * This EGraphics can be drawn as a solid fill or as a pattern.
	 * @param p true to draw this EGraphics patterned on a printer.
	 * False to draw this EGraphics as a solid fill on a printer.
	 */
	public void setPatternedOnPrinter(boolean p)
	{
		this.printPatterned = p;

		if (layer != null)
		{
			Pref pref = usePatternPrinterMap.get(layer);
			if (pref != null) pref.setBoolean(p);
		}
	}

	/**
	 * Method to tell the type of outline pattern.
	 * When the EGraphics is drawn as a pattern, the outline can be defined more clearly by drawing a line around the edge.
	 * @return the type of outline pattern.
	 */
	public Outline getOutlined() { return patternOutline; }

	/**
	 * Method describes the type of outline pattern by factory default.
	 * When the EGraphics is drawn as a pattern, the outline can be defined more clearly by drawing a line around the edge.
	 * @return the type of outline pattern by factory default.
	 */
	public Outline getFactoryOutlined()
	{
		if (layer == null) return null;
		Pref pref = outlinePatternMap.get(layer);
		return Outline.findOutline(pref.getIntFactoryValue());
	}

	/**
	 * Method to set whether this pattern has an outline around it.
	 * When the EGraphics is drawn as a pattern, the outline can be defined more clearly by drawing a line around the edge.
	 * @param o the outline pattern.
	 */
	public void setOutlined(Outline o)
	{
		if (o == null) o = Outline.NOPAT;
		patternOutline = o;

		if (layer != null)
		{
			Pref pref = outlinePatternMap.get(layer);
			if (pref != null) pref.setInt(o.index);
		}
	}

	/**
	 * Method to return the transparent layer number associated with this EGraphics.
	 * @return the transparent layer number associated with this EGraphics.
	 * A value of zero means that this EGraphics is not drawn transparently.
	 * Instead, use the "getColor()" method to get its solid color.
	 */
	public int getTransparentLayer() { return transparentLayer; }

	/**
	 * Method to return the transparent layer number by factory default.
	 * @return the transparent layer number by factory default.
	 * A value of zero means that this EGraphics is not drawn transparently.
	 */
	public int getFactoryTransparentLayer()
	{
		if (layer == null) return 0;
		Pref pref = transparentLayerMap.get(layer);
		return pref.getIntFactoryValue();
	}

	/**
	 * Method to set the transparent layer number associated with this EGraphics.
	 * @param transparentLayer the transparent layer number associated with this EGraphics.
	 * A value of zero means that this EGraphics is not drawn transparently.
	 * Then, use the "setColor()" method to set its solid color.
	 */
	public void setTransparentLayer(int transparentLayer)
	{
        if (transparentLayer < 0 || transparentLayer > TRANSPARENT_12)
		{
			System.out.println("Graphics transparent color bad: " + transparentLayer);
		}
		this.transparentLayer = transparentLayer;

		if (layer != null)
		{
			Pref pref = transparentLayerMap.get(layer);
			if (pref != null) pref.setInt(transparentLayer);
		}
	}

	/**
	 * Method to get the stipple pattern of this EGraphics.
	 * The stipple pattern is a 16 x 16 pattern that is stored in 16 integers.
	 * @return the stipple pattern of this EGraphics.
	 */
	public int [] getPattern() { return pattern; }

	/**
	 * Method to get the reversed stipple pattern of this EGraphics.
	 * The reversed stipple pattern is a 16 x 32 pattern that is stored in 16 integers.
	 * @return the stipple pattern of this EGraphics.
	 */
	public int [] getReversedPattern() { return reversedPattern; }

	/**
	 * Method to return the stipple pattern by factory default.
	 * The stipple pattern is a 16 x 16 pattern that is stored in 16 integers.
	 * @return the stipple pattern by factory default.
	 */
	public int [] getFactoryPattern()
	{
		if (layer == null) return null;
		Pref pref = patternMap.get(layer);
		int [] retPat = new int[16];
		parsePatString(pref.getStringFactoryValue(), retPat);
		return retPat;
	}

	/**
	 * Method to set the stipple pattern of this EGraphics.
	 * The stipple pattern is a 16 x 16 pattern that is stored in 16 integers.
	 * @param pattern the stipple pattern of this EGraphics.
	 */
	public void setPattern(int [] pattern)
	{
        setPatternLow(pattern);

		if (layer != null)
		{
			Pref pref = patternMap.get(layer);
			if (pref != null) pref.setString(makePatString(pattern));
		}
	}
    
    private void setPatternLow(int [] pattern) {
		if (pattern.length != 16)
		{
			System.out.println("Graphics bad: has " + pattern.length + " pattern entries instead of 16");
		}
        this.pattern = pattern;
        reversedPattern = new int[16];
        for (int i = 0; i < reversedPattern.length; i++) {
            int shortPattern = pattern[i];
            for (int j = 0; j < 16; j++) {
                if ((shortPattern & (1 << (15 - j))) != 0)
                    reversedPattern[i] |= 0x10001 << j;
            }
        }
    }

	/**
	 * Method to get the opacity of this EGraphics.
	 * Opacity runs from 0 (transparent) to 1 (opaque).
	 * @return the opacity of this EGraphics.
	 */
	public double getOpacity() { return opacity; }

	/**
	 * Method to return the opacity by factory default.
	 * Opacity runs from 0 (transparent) to 1 (opaque).
	 * @return the opacity by factory default.
	 */
	public double getFactoryOpacity()
	{
		if (layer == null) return 0;
		Pref pref = opacityMap.get(layer);
		return pref.getDoubleFactoryValue();
	}

	/**
	 * Method to set the opacity of this EGraphics.
	 * Opacity runs from 0 (transparent) to 1 (opaque).
	 * @param opacity the opacity of this EGraphics.
	 */
	public void setOpacity(double opacity)
	{
		this.opacity = opacity;

		if (layer != null)
		{
			Pref pref = opacityMap.get(layer);
			if (pref != null) pref.setDouble(opacity);
		}
	}

	/**
	 * Method to get whether this EGraphics should be drawn in the foreground.
	 * The foreground is the main "mix" of layers, such as metals and polysilicons.
	 * The background is typically used by implant and well layers.
	 * @return the whether this EGraphics should be drawn in the foreground.
	 */
	public boolean getForeground() { return foreground; }

	/**
	 * Method to return the color associated with this EGraphics.
	 * @return the color associated with this EGraphics.
	 */
	public Color getColor()
	{
		int alpha = (int)(opacity * 255.0);
		Color color = new Color(red, green, blue, alpha);
		return color;
	}

	/**
	 * Method to return the color associated with this EGraphics with full opacity.
	 * @return the color associated with this EGraphics.
	 */
	public Color getOpaqueColor()
	{
		Color color = new Color(red, green, blue, 255);
		return color;
	}

	/**
	 * Method to get the RGB value representing the color by factory default.
     * (Bits 16-23 are red, 8-15 are green, 0-7 are blue).
     * Alpha/opacity component is not returned 
	 * @return the RGB value representing the color by factory default.
	 */
	public int getFactoryColor()
	{
		if (layer == null) return 0;
		Pref pref = colorMap.get(layer);
		return pref.getIntFactoryValue();
	}

    /**
     * Returns the RGB value representing the color associated with this EGraphics.
     * (Bits 16-23 are red, 8-15 are green, 0-7 are blue).
     * Alpha/opacity component is not returned 
     * @return the RGB value of the color
     */
    public int getRGB() {
        return (red << 16) | (green << 8) | blue;
    }
    
	/**
	 * Method to set the color associated with this EGraphics.
	 * @param color the color to set.
	 */
	public void setColor(Color color)
	{
		transparentLayer = 0;
		red = color.getRed();
		green = color.getGreen();
		blue = color.getBlue();
		if (layer != null)
		{
			Pref pref = colorMap.get(layer);
			if (pref != null) pref.setInt((red << 16) | (green << 8) | blue);
		}

		// update any color used in 3D view if available
        Object obj3D = get3DAppearance();

        if (obj3D != null)
        {
            Class app3DClass = Resources.get3DClass("utils.J3DAppearance");
            try
            {
                Method setColorMethod3DClass = app3DClass.getDeclaredMethod("set3DColor", new Class[] {Object.class, Color.class});
                setColorMethod3DClass.invoke(obj3D, new Object[]{null, color});
            } catch (Exception e) {
                System.out.println("Cannot call 3D plugin method set3DColor: " + e.getMessage());
            }
        }
	}

	/**
	 * Method to convert a color index into a Color.
	 * Color indices are more general than colors, because they can handle
	 * transparent layers, C-Electric-style opaque layers, and full color values.
	 * @param colorIndex the color index to convert.
	 * @return a Color that describes the index
	 * Returns null if the index is a transparent layer.
	 */
	public static Color getColorFromIndex(int colorIndex)
	{
		if ((colorIndex&OPAQUEBIT) != 0)
		{
			// an opaque color
			switch (colorIndex)
			{
				case WHITE:   return new Color(255, 255, 255);
				case BLACK:   return new Color(  0,   0,   0);
				case RED:     return new Color(255,   0,   0);
				case BLUE:    return new Color(  0,   0, 255);
				case GREEN:   return new Color(  0, 255,   0);
				case CYAN:    return new Color(  0, 255, 255);
				case MAGENTA: return new Color(255,   0, 255);
				case YELLOW:  return new Color(255, 255,   0);
				case CELLTXT: return new Color(  0,   0,   0);
				case CELLOUT: return new Color(  0,   0,   0);
				case WINBOR:  return new Color(  0,   0,   0);
				case HWINBOR: return new Color(  0, 255,   0);
				case MENBOR:  return new Color(  0,   0,   0);
				case HMENBOR: return new Color(255, 255, 255);
				case MENTXT:  return new Color(  0,   0,   0);
				case MENGLY:  return new Color(  0,   0,   0);
				case CURSOR:  return new Color(  0,   0,   0);
				case GRAY:    return new Color(180, 180, 180);
				case ORANGE:  return new Color(255, 190,   6);
				case PURPLE:  return new Color(186,   0, 255);
				case BROWN:   return new Color(139,  99,  46);
				case LGRAY:   return new Color(230, 230, 230);
				case DGRAY:   return new Color(100, 100, 100);
				case LRED:    return new Color(255, 150, 150);
				case DRED:    return new Color(159,  80,  80);
				case LGREEN:  return new Color(175, 255, 175);
				case DGREEN:  return new Color( 89, 159,  85);
				case LBLUE:   return new Color(150, 150, 255);
				case DBLUE:   return new Color(  2,  15, 159);
			}
			return null;
		}
		if ((colorIndex&FULLRGBBIT) != 0)
		{
			// a full RGB color (opaque)
			return new Color((colorIndex >> 24) & 0xFF, (colorIndex >> 16) & 0xFF, (colorIndex >> 8) & 0xFF);
		}

		// handle transparent colors
		Technology curTech = Technology.getCurrent();
		Color [] colorMap = curTech.getColorMap();
		if (colorMap == null)
		{
			Technology altTech = Schematics.getDefaultSchematicTechnology();
			if (altTech != curTech)
			{
				colorMap = altTech.getColorMap();
			}
			if (colorMap == null) return null;
		}
		int trueIndex = colorIndex >> 2;
		if (trueIndex < colorMap.length) return colorMap[trueIndex];
		return null;
	}

	/**
	 * Method to set the color of this EGraphics from a "color index".
	 * Color indices are more general than colors, because they can handle
	 * transparent layers, C-Electric-style opaque layers, and full color values.
	 * Artwork nodes and arcs represent individualized color by using color indices.
	 * @param colorIndex the color index to set.
	 */
	public void setColorIndex(int colorIndex)
	{
		if ((colorIndex&(OPAQUEBIT|FULLRGBBIT)) != 0)
		{
			// an opaque or full RGB color
			transparentLayer = 0;
			setColor(getColorFromIndex(colorIndex));
			return;
		}

		// a transparent color
		if ((colorIndex&LAYERT1) != 0) transparentLayer = TRANSPARENT_1; else
		if ((colorIndex&LAYERT2) != 0) transparentLayer = TRANSPARENT_2; else
		if ((colorIndex&LAYERT3) != 0) transparentLayer = TRANSPARENT_3; else
		if ((colorIndex&LAYERT4) != 0) transparentLayer = TRANSPARENT_4; else
		if ((colorIndex&LAYERT5) != 0) transparentLayer = TRANSPARENT_5; else
		if ((colorIndex&LAYERT6) != 0) transparentLayer = TRANSPARENT_6; else
		if ((colorIndex&LAYERT7) != 0) transparentLayer = TRANSPARENT_7; else
		if ((colorIndex&LAYERT8) != 0) transparentLayer = TRANSPARENT_8; else
		if ((colorIndex&LAYERT9) != 0) transparentLayer = TRANSPARENT_9; else
		if ((colorIndex&LAYERT10) != 0) transparentLayer = TRANSPARENT_10; else
		if ((colorIndex&LAYERT11) != 0) transparentLayer = TRANSPARENT_11; else
		if ((colorIndex&LAYERT12) != 0) transparentLayer = TRANSPARENT_12;
	}

	/**
	 * Method to convert a Color to a color index.
	 * Color indices are more general than colors, because they can handle
	 * transparent layers, C-Electric-style opaque layers, and full color values.
	 * Artwork nodes and arcs represent individualized color by using color indices.
	 * @param color a Color object
	 * @return the color index that describes that color.
	 */
	public static int makeIndex(Color color)
	{
		int red   = color.getRed();
		int green = color.getGreen();
		int blue  = color.getBlue();
		int index = (red << 24) | (green << 16) | (blue << 8) | FULLRGBBIT;
		return index;
	}

	/**
	 * Method to convert a transparent layer to a color index.
	 * Color indices are more general than colors, because they can handle
	 * transparent layers, C-Electric-style opaque layers, and full color values.
	 * @param transparentLayer the transparent layer number.
	 * @return the color index that describes that transparent layer.
	 */
	public static int makeIndex(int transparentLayer)
	{
		switch (transparentLayer)
		{
			case TRANSPARENT_1: return LAYERT1;
			case TRANSPARENT_2: return LAYERT2;
			case TRANSPARENT_3: return LAYERT3;
			case TRANSPARENT_4: return LAYERT4;
			case TRANSPARENT_5: return LAYERT5;
			case TRANSPARENT_6: return LAYERT6;
			case TRANSPARENT_7: return LAYERT7;
			case TRANSPARENT_8: return LAYERT8;
			case TRANSPARENT_9: return LAYERT9;
			case TRANSPARENT_10: return LAYERT10;
			case TRANSPARENT_11: return LAYERT11;
			case TRANSPARENT_12: return LAYERT12;
		}
		return 0;
	}

	/**
	 * Method to find the index of a color, given its name.
	 * @param name the name of the color.
	 * @return the index of the color.
	 */
	public static int findColorIndex(String name)
	{
		if (name.equals("white"))          return WHITE;
		if (name.equals("black"))          return BLACK;
		if (name.equals("red"))            return RED;
		if (name.equals("blue"))           return BLUE;
		if (name.equals("green"))          return GREEN;
		if (name.equals("cyan"))           return CYAN;
		if (name.equals("magenta"))        return MAGENTA;
		if (name.equals("yellow"))         return YELLOW;
		if (name.equals("gray"))           return GRAY;
		if (name.equals("orange"))         return ORANGE;
		if (name.equals("purple"))         return PURPLE;
		if (name.equals("brown"))          return BROWN;
		if (name.equals("light-gray"))     return LGRAY;
		if (name.equals("dark-gray"))      return DGRAY;
		if (name.equals("light-red"))      return LRED;
		if (name.equals("dark-red"))       return DRED;
		if (name.equals("light-green"))    return LGREEN;
		if (name.equals("dark-green"))     return DGREEN;
		if (name.equals("light-blue"))     return LBLUE;
		if (name.equals("dark-blue"))      return DBLUE;
		if (name.equals("transparent-1"))  return LAYERT1;
		if (name.equals("transparent-2"))  return LAYERT2;
		if (name.equals("transparent-3"))  return LAYERT3;
		if (name.equals("transparent-4"))  return LAYERT4;
		if (name.equals("transparent-5"))  return LAYERT5;
		if (name.equals("transparent-6"))  return LAYERT6;
		if (name.equals("transparent-7"))  return LAYERT7;
		if (name.equals("transparent-8"))  return LAYERT8;
		if (name.equals("transparent-9"))  return LAYERT9;
		if (name.equals("transparent-10")) return LAYERT10;
		if (name.equals("transparent-11")) return LAYERT11;
		if (name.equals("transparent-12")) return LAYERT12;
		return 0;
	}

	/**
	 * Method to tell the name of the color with a given index.
	 * Color indices are more general than colors, because they can handle
	 * transparent layers, C-Electric-style opaque layers, and full color values.
	 * @param colorIndex the color number.
	 * @return the name of that color.
	 */
	public static String getColorIndexName(int colorIndex)
	{
		if ((colorIndex&FULLRGBBIT) != 0)
		{
			int red =   (colorIndex >> 24) & 0xFF;
			int green = (colorIndex >> 16) & 0xFF;
			int blue =  (colorIndex >> 8) & 0xFF;
			return "Color (" + red + "," + green + "," + blue + ")";
		}
		switch (colorIndex)
		{
			case WHITE:    return "white";
			case BLACK:    return "black";
			case RED:      return "red";
			case BLUE:     return "blue";
			case GREEN:    return "green";
			case CYAN:     return "cyan";
			case MAGENTA:  return "magenta";
			case YELLOW:   return "yellow";
			case GRAY:     return "gray";
			case ORANGE:   return "orange";
			case PURPLE:   return "purple";
			case BROWN:    return "brown";
			case LGRAY:    return "light-gray";
			case DGRAY:    return "dark-gray";
			case LRED:     return "light-red";
			case DRED:     return "dark-red";
			case LGREEN:   return "light-green";
			case DGREEN:   return "dark-green";
			case LBLUE:    return "light-blue";
			case DBLUE:    return "dark-blue";
			case LAYERT1:  return "transparent-1";
			case LAYERT2:  return "transparent-2";
			case LAYERT3:  return "transparent-3";
			case LAYERT4:  return "transparent-4";
			case LAYERT5:  return "transparent-5";
			case LAYERT6:  return "transparent-6";
			case LAYERT7:  return "transparent-7";
			case LAYERT8:  return "transparent-8";
			case LAYERT9:  return "transparent-9";
			case LAYERT10: return "transparent-10";
			case LAYERT11: return "transparent-11";
			case LAYERT12: return "transparent-12";
		}
		return "ColorIndex "+colorIndex;
	}

	/**
	 * Method to return the array of color indices.
	 * @return an array of the possible color indices.
	 */
	public static int [] getColorIndices()
	{
		return new int [] {WHITE, BLACK, RED, BLUE, GREEN, CYAN, MAGENTA, YELLOW,
			GRAY, ORANGE, PURPLE, BROWN, LGRAY, DGRAY, LRED, DRED, LGREEN, DGREEN, LBLUE, DBLUE,
			LAYERT1, LAYERT2, LAYERT3, LAYERT4, LAYERT5, LAYERT6, LAYERT7, LAYERT8, LAYERT9,
			LAYERT10, LAYERT11, LAYERT12};
	}

	/**
	 * Method to return the array of transparent color indices.
	 * @return an array of the possible transparent color indices.
	 */
	public static int [] getTransparentColorIndices()
	{
		return new int [] {LAYERT1, LAYERT2, LAYERT3, LAYERT4, LAYERT5, LAYERT6, LAYERT7, LAYERT8, LAYERT9,
			LAYERT10, LAYERT11, LAYERT12};
	}

	/**
	 * Method to set 3D appearance. If Java3D, Appearance class will be the type.
	 * @param obj
	 */
	public void set3DAppearance(Object obj) {appearance3D = obj;}

	/**
	 * Method to retrieve current 3D appearance.
	 * @return the current 3D appearance.
	 */
	public Object get3DAppearance() {return appearance3D;}

    /**
     * Method to notify 3D observers
     * @param layerVis
     */
    public void notifyVisibility(Boolean layerVis)
    {
        setChanged();
        notifyObservers(layerVis);
        clearChanged();
    }
}
