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
	/** color to use */										private int red, green, blue;
	/** opacity (0 to 1) of color */						private double opacity;
	/** whether to draw color in foregound */				private int foreground;
	/** stipple pattern to draw */							private int [] pattern;

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
	public EGraphics(int displayMethod, int printMethod, int red, int green, int blue, double opacity, int foreground, int[] pattern)
	{
		this.displayMethod = displayMethod;
		this.printMethod = printMethod;
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.opacity = opacity;
		this.foreground = foreground;
		this.pattern = pattern;
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
	 * Method describes how this layer appears on a printer.
	 * This layer can be drawn as a solid fill or as a pattern.
	 * @return true to draw this layer patterned (on a printer).
	 * False to draw this layer as a solid fill.
	 */
	public boolean isPatternedOnPrinter() { return (printMethod & NATURE) == PATTERNED; }

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
	public void setColor(int red, int green, int blue)
	{
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	public static int findColorIndex(String name)
	{
		if (name.equals("white"))       return WHITE;
		if (name.equals("black"))       return BLACK;
		if (name.equals("red"))         return RED;
		if (name.equals("blue"))        return BLUE;
		if (name.equals("green"))       return GREEN;
		if (name.equals("cyan"))        return CYAN;
		if (name.equals("magenta"))     return MAGENTA;
		if (name.equals("yellow"))      return YELLOW;
		if (name.equals("gray"))        return GRAY;
		if (name.equals("orange"))      return ORANGE;
		if (name.equals("purple"))      return PURPLE;
		if (name.equals("brown"))       return BROWN;
		if (name.equals("light-gray"))  return LGRAY;
		if (name.equals("dark-gray"))   return DGRAY;
		if (name.equals("light-red"))   return LRED;
		if (name.equals("dark-red"))    return DRED;
		if (name.equals("light-green")) return LGREEN;
		if (name.equals("dark-green"))  return DGREEN;
		if (name.equals("light-blue"))  return LBLUE;
		if (name.equals("dark-blue"))   return DBLUE;
		return 0;
	}

	/**
	 * Method to set the color associated with this EGraphics.
	 * @param index the color to set.
	 */
	public void setColor(int index)
	{
		switch (index)
		{
			case WHITE:   this.red = 255;   this.green = 255;   this.blue = 255;   break;
			case BLACK:   this.red =   0;   this.green =   0;   this.blue =   0;   break;
			case RED:     this.red = 255;   this.green =   0;   this.blue =   0;   break;
			case BLUE:    this.red =   0;   this.green =   0;   this.blue = 255;   break;
			case GREEN:   this.red =   0;   this.green = 255;   this.blue =   0;   break;
			case CYAN:    this.red =   0;   this.green = 255;   this.blue = 255;   break;
			case MAGENTA: this.red = 255;   this.green =   0;   this.blue = 255;   break;
			case YELLOW:  this.red = 255;   this.green = 255;   this.blue =   0;   break;
			case CELLTXT: this.red =   0;   this.green =   0;   this.blue =   0;   break;
			case CELLOUT: this.red =   0;   this.green =   0;   this.blue =   0;   break;
			case WINBOR:  this.red =   0;   this.green =   0;   this.blue =   0;   break;
			case HWINBOR: this.red =   0;   this.green = 255;   this.blue =   0;   break;
			case MENBOR:  this.red =   0;   this.green =   0;   this.blue =   0;   break;
			case HMENBOR: this.red = 255;   this.green = 255;   this.blue = 255;   break;
			case MENTXT:  this.red =   0;   this.green =   0;   this.blue =   0;   break;
			case MENGLY:  this.red =   0;   this.green =   0;   this.blue =   0;   break;
			case CURSOR:  this.red =   0;   this.green =   0;   this.blue =   0;   break;
			case GRAY:    this.red = 180;   this.green = 180;   this.blue = 180;   break;
			case ORANGE:  this.red = 255;   this.green = 190;   this.blue =   6;   break;
			case PURPLE:  this.red = 186;   this.green =   0;   this.blue = 255;   break;
			case BROWN:   this.red = 139;   this.green =  99;   this.blue =  46;   break;
			case LGRAY:   this.red = 230;   this.green = 230;   this.blue = 230;   break;
			case DGRAY:   this.red = 100;   this.green = 100;   this.blue = 100;   break;
			case LRED:    this.red = 255;   this.green = 150;   this.blue = 150;   break;
			case DRED:    this.red = 159;   this.green =  80;   this.blue =  80;   break;
			case LGREEN:  this.red = 175;   this.green = 255;   this.blue = 175;   break;
			case DGREEN:  this.red =  89;   this.green = 159;   this.blue =  85;   break;
			case LBLUE:   this.red = 150;   this.green = 150;   this.blue = 255;   break;
			case DBLUE:   this.red =   2;   this.green =  15;   this.blue = 159;   break;
		}
	}

	/**
	 * Method to tell the name of the color with a given index.
	 * @param index the color number.
	 * @return the name of that color.
	 */
	public static String getColorName(int index)
	{
		switch (index)
		{
			case WHITE:   return "white";
			case BLACK:   return "black";
			case RED:     return "red";
			case BLUE:    return "blue";
			case GREEN:   return "green";
			case CYAN:    return "cyan";
			case MAGENTA: return "magenta";
			case YELLOW:  return "yellow";
			case GRAY:    return "gray";
			case ORANGE:  return "orange";
			case PURPLE:  return "purple";
			case BROWN:   return "brown";
			case LGRAY:   return "light-gray";
			case DGRAY:   return "dark-gray";
			case LRED:    return "light-red";
			case DRED:    return "dark-red";
			case LGREEN:  return "light-green";
			case DGREEN:  return "dark-green";
			case LBLUE:   return "light-blue";
			case DBLUE:   return "dark-blue";
		}
		return "Color "+index;
	}

	/**
	 * Method to return the array of colors.
	 * @return an array of the possible colors.
	 */
	public static int [] getColors()
	{
		return new int [] {WHITE, BLACK, RED, BLUE, GREEN, CYAN, MAGENTA, YELLOW,
			GRAY, ORANGE, PURPLE, BROWN, LGRAY, DGRAY, LRED, DRED, LGREEN, DGREEN, LBLUE, DBLUE};
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
