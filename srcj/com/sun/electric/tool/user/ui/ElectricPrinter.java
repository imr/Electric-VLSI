/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ElectricPrinter.java
 * Written by: Gilda Garreton
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.tool.io.IOTool;

import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.awt.image.ImageObserver;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.awt.Graphics;

public class ElectricPrinter implements Printable, ImageObserver
{
	private WindowContent context;
	private BufferedImage img = null;
	private Graphics graphics;
	private PageFormat pageFormat;
	private int desiredDPI = IOTool.getPrintResolution();

	public ElectricPrinter (WindowContent context, PageFormat pageFormat)
	{
		this.context = context;
		this.pageFormat = pageFormat;
	}

	public BufferedImage getBufferedImage() {return img;}
	public void setBufferedImage(BufferedImage img) {this.img = img;}
	public Graphics getGraphics() {return graphics;}
	public PageFormat getPageFormat() {return pageFormat;}
	public int getDesiredDPI() {return desiredDPI;}

	public int print(Graphics g, PageFormat pf, int page)
		throws java.awt.print.PrinterException
	{
		if (page != 0) return Printable.NO_SUCH_PAGE;

		graphics = g;
		pageFormat = pf;
		BufferedImage img = context.getOffScreenImage(this);
		return ((img != null) ? Printable.PAGE_EXISTS : Printable.NO_SUCH_PAGE);
	}

	/** This function is required for 3D view */
	public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
	{
		return false;
	}
}
