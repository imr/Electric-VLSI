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

import java.awt.Color;

public class EGraphics
{
	/** appearance on a display */							private int displayMethod;
	/** appearance on paper */								private int printMethod;
	/** transparent layer to use (0 for none) */			private int transparentLayer;
	/** color to use */										private int red, green, blue;
	/** opacity (0 to 1) of color */						private double opacity;
	/** whether to draw color in foregound */				private int foreground;
	/** stipple pattern to draw */							private int [] pattern;

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

	// the opaque colors
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

	// drawing styles
	/** choice between solid and patterned */				private final static int NATURE =     1;
	/** Draw as a solid fill. */							public final static int SOLIDC =      0;
	/** Draw as a stipple pattern. */						public final static int PATTERNED =   1;
	/** Set to draw an outline around stipple pattern. */	public final static int OUTLINEPAT = 02;

	/**
	 * Method to create a graphics object.
	 * @param displayMethod the way to show this EGraphics on a display.
	 * @param printMethod the way to show this EGraphics on paper.
	 * @param red the red component of this EGraphics.
	 * @param green the green component of this EGraphics.
	 * @param blue the blue component of this EGraphics.
	 * @param opacity the opacity of this EGraphics (1 for opaque, 0 for transparent).
	 * @param foreground the foreground factor of this EGraphics (1 for to be in foreground).
	 * @param pattern the 16x16 stipple pattern of this EGraphics (16 integers).
	 * This pattern is tessellated across the polygon.
	 */
	public EGraphics(int displayMethod, int printMethod, int transparentLayer,
		int red, int green, int blue, double opacity, int foreground, int[] pattern)
	{
		this.displayMethod = displayMethod;
		this.printMethod = printMethod;
		this.transparentLayer = transparentLayer;
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.opacity = opacity;
		this.foreground = foreground;
		this.pattern = pattern;
		if (transparentLayer < 0 || transparentLayer >= 12)
		{
			System.out.println("Graphics transparent color bad: " + transparentLayer);
		}
		if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255)
		{
			System.out.println("Graphics color bad: (" + red + "," + green + "," + blue + ")");
		}
	}

	/**
	 * Method describes how this layer appears on a display.
	 * This layer can be drawn as a solid fill or as a pattern.
	 * @return true to draw this layer patterned (on a display).
	 * False to draw this layer as a solid fill.
	 */
	public boolean isPatternedOnDisplay() { return (displayMethod & NATURE) == PATTERNED; }

	/**
	 * Method to tell whether this pattern has an outline around it.
	 * When the layer is drawn as a pattern, the outline can be defined more clearly by drawing a line around the edge.
	 * @return true to dan outline around this pattern (on the display).
	 */
	public boolean isOutlinePatternedOnDisplay() { return (displayMethod & OUTLINEPAT) != 0; }

	/**
	 * Method describes how this layer appears on a printer.
	 * This layer can be drawn as a solid fill or as a pattern.
	 * @return true to draw this layer patterned (on a printer).
	 * False to draw this layer as a solid fill.
	 */
	public boolean isPatternedOnPrinter() { return (printMethod & NATURE) == PATTERNED; }

	/**
	 * Method to tell whether this pattern has an outline around it.
	 * When the layer is drawn as a pattern, the outline can be defined more clearly by drawing a line around the edge.
	 * @return true to dan outline around this pattern (on the printer).
	 */
	public boolean isOutlinePatternedOnPrinter() { return (printMethod & OUTLINEPAT) != 0; }

	/**
	 * Method to return the transparent layer associated with this EGraphics.
	 * @return the transparent layer associated with this EGraphics.
	 * A value of zero means that this Layer is not drawn transparently.
	 * Instead, use the "getColor()" method to get its solid color.
	 */
	public int getTransparentLayer() { return transparentLayer; }

	/**
	 * Method to set the transparent layer associated with this EGraphics.
	 * @param transparentLayer the transparent layer associated with this EGraphics.
	 * A value of zero means that this Layer is not drawn transparently.
	 * Then, use the "setColor()" method to set its solid color.
	 */
	public void setTransparentLayer(int transparentLayer) { this.transparentLayer = transparentLayer; }
	
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
	 * Method to set the color associated with this EGraphics.
	 * @param red the red color to set.
	 * @param green the green color to set.
	 * @param blue the blue color to set.
	 */
	public void setColor(Color color)
	{
		transparentLayer = 0;
		red = color.getRed();
		green = color.getGreen();
		blue = color.getBlue();
	}

	/**
	 * Method to set the color associated with this EGraphics.
	 * @param color the color to set.
	 */
	public void setColor(int color)
	{
		if ((color&OPAQUEBIT) != 0)
		{
			// an opaque color
			transparentLayer = 0;
			switch (color)
			{
				case WHITE:   red = 255;   green = 255;   blue = 255;   break;
				case BLACK:   red =   0;   green =   0;   blue =   0;   break;
				case RED:     red = 255;   green =   0;   blue =   0;   break;
				case BLUE:    red =   0;   green =   0;   blue = 255;   break;
				case GREEN:   red =   0;   green = 255;   blue =   0;   break;
				case CYAN:    red =   0;   green = 255;   blue = 255;   break;
				case MAGENTA: red = 255;   green =   0;   blue = 255;   break;
				case YELLOW:  red = 255;   green = 255;   blue =   0;   break;
				case CELLTXT: red =   0;   green =   0;   blue =   0;   break;
				case CELLOUT: red =   0;   green =   0;   blue =   0;   break;
				case WINBOR:  red =   0;   green =   0;   blue =   0;   break;
				case HWINBOR: red =   0;   green = 255;   blue =   0;   break;
				case MENBOR:  red =   0;   green =   0;   blue =   0;   break;
				case HMENBOR: red = 255;   green = 255;   blue = 255;   break;
				case MENTXT:  red =   0;   green =   0;   blue =   0;   break;
				case MENGLY:  red =   0;   green =   0;   blue =   0;   break;
				case CURSOR:  red =   0;   green =   0;   blue =   0;   break;
				case GRAY:    red = 180;   green = 180;   blue = 180;   break;
				case ORANGE:  red = 255;   green = 190;   blue =   6;   break;
				case PURPLE:  red = 186;   green =   0;   blue = 255;   break;
				case BROWN:   red = 139;   green =  99;   blue =  46;   break;
				case LGRAY:   red = 230;   green = 230;   blue = 230;   break;
				case DGRAY:   red = 100;   green = 100;   blue = 100;   break;
				case LRED:    red = 255;   green = 150;   blue = 150;   break;
				case DRED:    red = 159;   green =  80;   blue =  80;   break;
				case LGREEN:  red = 175;   green = 255;   blue = 175;   break;
				case DGREEN:  red =  89;   green = 159;   blue =  85;   break;
				case LBLUE:   red = 150;   green = 150;   blue = 255;   break;
				case DBLUE:   red =   2;   green =  15;   blue = 159;   break;
			}
			return;
		}
		if ((color&FULLRGBBIT) != 0)
		{
			// a full RGB color (opaque)
			transparentLayer = 0;
			red =   (color >> 24) & 0xFF;
			green = (color >> 16) & 0xFF;
			blue =  (color >> 8) & 0xFF;
			return;
		}

		// a transparent color
		if ((color&LAYERT1) != 0) transparentLayer = 1; else
		if ((color&LAYERT2) != 0) transparentLayer = 2; else
		if ((color&LAYERT3) != 0) transparentLayer = 3; else
		if ((color&LAYERT4) != 0) transparentLayer = 4; else
		if ((color&LAYERT5) != 0) transparentLayer = 5; else
		if ((color&LAYERT6) != 0) transparentLayer = 6; else
		if ((color&LAYERT7) != 0) transparentLayer = 7; else
		if ((color&LAYERT8) != 0) transparentLayer = 8; else
		if ((color&LAYERT9) != 0) transparentLayer = 9; else
		if ((color&LAYERT10) != 0) transparentLayer = 10; else
		if ((color&LAYERT11) != 0) transparentLayer = 11; else
		if ((color&LAYERT12) != 0) transparentLayer = 12;
	}

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
	 * @param index the color number.
	 * @return the name of that color.
	 */
	public static String getColorName(int color)
	{
		if ((color&FULLRGBBIT) != 0)
		{
			int red =   (color >> 24) & 0xFF;
			int green = (color >> 16) & 0xFF;
			int blue =  (color >> 8) & 0xFF;
			return "Color (" + red + "," + green + "," + blue + ")";
		}
		switch (color)
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
		return "Color "+color;
	}

	/**
	 * Method to return the array of colors.
	 * @return an array of the possible colors.
	 */
	public static int [] getColors()
	{
		return new int [] {WHITE, BLACK, RED, BLUE, GREEN, CYAN, MAGENTA, YELLOW,
			GRAY, ORANGE, PURPLE, BROWN, LGRAY, DGRAY, LRED, DRED, LGREEN, DGREEN, LBLUE, DBLUE,
			LAYERT1, LAYERT2, LAYERT3, LAYERT4, LAYERT5, LAYERT6, LAYERT7, LAYERT8, LAYERT9,
			LAYERT10, LAYERT11, LAYERT12};
	}

	/**
	 * Method to get the opacity of this Layer.
	 * Opacity runs from 0 (transparent) to 1 (opaque).
	 * @return the opacity of this Layer.
	 */
	public double getOpacity() { return opacity; }

	/**
	 * Method to get whether this Layer should be drawn in the foreground.
	 * The foreground is the main "mix" of layers, such as metals and polysilicons.
	 * The background is typically used by implant and well layers.
	 * @return the whether this Layer should be drawn in the foreground.
	 */
	public boolean getForeground() { return foreground != 0; }

	/**
	 * Method to get the stipple pattern of this Layer.
	 * The stipple pattern is a 16 x 16 pattern that is stored in 16 integers.
	 * @return the stipple pattern of this Layer.
	 */
	public int [] getPattern() { return pattern; }
}
