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

import java.awt.*;

public class EGraphics
{
	/** bit-planes to consider */					int bits;
	/** bits to set on the bit-planes */			int color;
	/** appearance on a display */					int displayMethod;
	/** appearance on paper */						int printMethod;
	/** color to use on paper */					int printRed, printGreen, printBlue;
	/** opacity (0 to 1) of paper color */			double printOpacity;
	/** whether to draw paper color in foregound */	int printForeground;
	/** stipple pattern to draw */					int [] pattern;

	// bit map colors
	/** nothing */									public final static int LAYERN =   0000;
	/** highlight color and bit plane */			public final static int LAYERH =   0001;
	/** opaque layer escape bit */					public final static int LAYEROE =  0002;
	/** opaque layers */							public final static int LAYERO =   0176;
	/** transparent layer 1 */						public final static int LAYERT1 =  0004;
	/** transparent layer 2 */						public final static int LAYERT2 =  0010;
	/** transparent layer 3 */						public final static int LAYERT3 =  0020;
	/** transparent layer 4 */						public final static int LAYERT4 =  0040;
	/** transparent layer 5 */						public final static int LAYERT5 =  0100;
	/** grid line color and bit plane */			public final static int LAYERG =   0200;
	/** everything */								public final static int LAYERA =   0777;

	// color map colors
	/** no color */									public final static int ALLOFF =   0000;
	/** highlight color and bit plane */			public final static int HIGHLIT =  0001;
	/** transparent color 1 */						public final static int COLORT1 =  0004;
	/** transparent color 2 */						public final static int COLORT2 =  0010;
	/** transparent color 3 */						public final static int COLORT3 =  0020;
	/** transparent color 4 */						public final static int COLORT4 =  0040;
	/** transparent color 5 */						public final static int COLORT5 =  0100;
	/** grid line color and bit plane */			public final static int GRID =     0200;

	/** white */									public final static int WHITE =    0002;
	/** black */									public final static int BLACK =    0006;
	/** red */										public final static int RED =      0012;
	/** blue */										public final static int BLUE =     0016;
	/** green  */									public final static int GREEN =    0022;
	/** cyan */										public final static int CYAN =     0026;
	/** magenta */									public final static int MAGENTA =  0032;
	/** yellow */									public final static int YELLOW =   0036;
	/** cell and port names */						public final static int CELLTXT =  0042;
	/** cell outline */								public final static int CELLOUT =  0046;
	/** window border color */						public final static int WINBOR =   0052;
	/** highlighted window border color */			public final static int HWINBOR =  0056;
	/** menu border color */						public final static int MENBOR =   0062;
	/** highlighted menu border color */			public final static int HMENBOR =  0066;
	/** menu text color */							public final static int MENTXT =   0072;
	/** menu glyph color */							public final static int MENGLY =   0076;
	/** cursor color */								public final static int CURSOR =   0102;
	/** gray */										public final static int GRAY =     0106;
	/** orange */									public final static int ORANGE =   0112;
	/** purple */									public final static int PURPLE =   0116;
	/** brown */									public final static int BROWN =    0122;
	/** light gray */								public final static int LGRAY =    0126;
	/** dark gray */								public final static int DGRAY =    0132;
	/** light red */								public final static int LRED =     0136;
	/** dark red */									public final static int DRED =     0142;
	/** light green */								public final static int LGREEN =   0146;
	/** dark green */								public final static int DGREEN =   0152;
	/** light blue */								public final static int LBLUE =    0156;
	/** dark blue */								public final static int DBLUE =    0162;

	// drawing styles
	/** choice between solid and patterned */		public final static int NATURE =       1;
	/**   solid colors */							public final static int SOLIDC =       0;
	/**   stippled with "raster" */					public final static int PATTERNED =    1;
	/** don't draw this layer */					public final static int INVISIBLE =    2;
	/** temporary for INVISIBLE bit */				public final static int INVTEMP =      4;
	/** if NATURE is PATTERNED, outline it */		public final static int OUTLINEPAT = 010;

	/**
	 * Routine to create a graphics object.
	 */
	public EGraphics(int bits, int color, int displayMethod, int printMethod,
		int printRed, int printGreen, int printBlue, double printOpacity, int printForeground, int[] pattern)
	{
		this.bits = bits;
		this.color = color;
		this.displayMethod = displayMethod;
		this.printMethod = printMethod;
		this.printRed = printRed;
		this.printGreen = printGreen;
		this.printBlue = printBlue;
		this.printOpacity = printOpacity;
		this.printForeground = printForeground;
		this.pattern = pattern;
	}

	/**
	 * Routine to return the color associated with this graphics.
	 */
	public Color getColor()
	{
		int alpha = (int)(printOpacity * 255.0);
		Color color = new Color(printRed, printGreen, printBlue, alpha);
		return color;
	}
	
	/**
	 * Routine to set the color associated with this graphics to the given red/green/blue.
	 */
	public void setColor(int printRed, int printGreen, int printBlue)
	{
		this.printRed = printRed;
		this.printGreen = printGreen;
		this.printBlue = printBlue;
	}
	
	/**
	 * Routine to set the color associated with this graphics to the mapped "index".
	 */
	public void setColor(int index)
	{
		switch (index)
		{
			case WHITE:   this.printRed = 255;   this.printGreen = 255;   this.printBlue = 255;   break;
			case BLACK:   this.printRed =   0;   this.printGreen =   0;   this.printBlue =   0;   break;
			case RED:     this.printRed = 255;   this.printGreen =   0;   this.printBlue =   0;   break;
			case BLUE:    this.printRed =   0;   this.printGreen =   0;   this.printBlue = 255;   break;
			case GREEN:   this.printRed =   0;   this.printGreen = 255;   this.printBlue =   0;   break;
			case CYAN:    this.printRed =   0;   this.printGreen = 255;   this.printBlue = 255;   break;
			case MAGENTA: this.printRed = 255;   this.printGreen =   0;   this.printBlue = 255;   break;
			case YELLOW:  this.printRed = 255;   this.printGreen = 255;   this.printBlue =   0;   break;
			case CELLTXT: this.printRed =   0;   this.printGreen =   0;   this.printBlue =   0;   break;
			case CELLOUT: this.printRed =   0;   this.printGreen =   0;   this.printBlue =   0;   break;
			case WINBOR:  this.printRed =   0;   this.printGreen =   0;   this.printBlue =   0;   break;
			case HWINBOR: this.printRed =   0;   this.printGreen = 255;   this.printBlue =   0;   break;
			case MENBOR:  this.printRed =   0;   this.printGreen =   0;   this.printBlue =   0;   break;
			case HMENBOR: this.printRed = 255;   this.printGreen = 255;   this.printBlue = 255;   break;
			case MENTXT:  this.printRed =   0;   this.printGreen =   0;   this.printBlue =   0;   break;
			case MENGLY:  this.printRed =   0;   this.printGreen =   0;   this.printBlue =   0;   break;
			case CURSOR:  this.printRed =   0;   this.printGreen =   0;   this.printBlue =   0;   break;
			case GRAY:    this.printRed = 180;   this.printGreen = 180;   this.printBlue = 180;   break;
			case ORANGE:  this.printRed = 255;   this.printGreen = 190;   this.printBlue =   6;   break;
			case PURPLE:  this.printRed = 186;   this.printGreen =   0;   this.printBlue = 255;   break;
			case BROWN:   this.printRed = 139;   this.printGreen =  99;   this.printBlue =  46;   break;
			case LGRAY:   this.printRed = 230;   this.printGreen = 230;   this.printBlue = 230;   break;
			case DGRAY:   this.printRed = 100;   this.printGreen = 100;   this.printBlue = 100;   break;
			case LRED:    this.printRed = 255;   this.printGreen = 150;   this.printBlue = 150;   break;
			case DRED:    this.printRed = 159;   this.printGreen =  80;   this.printBlue =  80;   break;
			case LGREEN:  this.printRed = 175;   this.printGreen = 255;   this.printBlue = 175;   break;
			case DGREEN:  this.printRed =  89;   this.printGreen = 159;   this.printBlue =  85;   break;
			case LBLUE:   this.printRed = 150;   this.printGreen = 150;   this.printBlue = 255;   break;
			case DBLUE:   this.printRed =   2;   this.printGreen =  15;   this.printBlue = 159;   break;
		}
	}
}
