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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

/**
 * Class to define a printer interface.
 */
public class ElectricPrinter implements Printable, ImageObserver
{
	private WindowContent context;
	private PageFormat pageFormat;
	private PrinterJob printJob;
	private BufferedImage img = null;
	private Graphics graphics;
	private Dimension oldSize;
	private int desiredDPI = IOTool.getPrintResolution();
	/** text printing: the strings to print */								private String [] allStrings;
	/** text printing: the starting line in the strings */					private int startLine;
	/** text printing: the starting character on the current line */		private int startChar;
	/** text printing: the font to use */									private Font printFont;
	/** text printing: the page height */									private int pageHeight;
	/** text printing: text height */										private int fontHeight;
	/** text printing: the offset for baselines */							private int yOffset;
	/** text printing: the number of lines per page */						private int linesPerPage;
	/** text printing: remembered starting line on remembered page */		private int startLineAtPage;
	/** text printing: remembered starting character on remembered page  */	private int startCharAtPage;
	/** text printing: the remembered page  */								private int startPageNumber;
	/** text printing: context for getting text width */					private FontRenderContext frc;


	public ElectricPrinter(WindowContent context, PageFormat pageFormat, PrinterJob printJob)
	{
		this.context = context;
		this.pageFormat = pageFormat;
		this.printJob = printJob;
	}

	public BufferedImage getBufferedImage() { return img; }
	public void setBufferedImage(BufferedImage img) { this.img = img; }
	public Graphics getGraphics() { return graphics; }
	public PageFormat getPageFormat() { return pageFormat; }
	public PrinterJob getPrintJob() { return printJob; }
	public int getDesiredDPI() { return desiredDPI; }
	public void setOldSize(Dimension oldSize) { this.oldSize = oldSize; }
	public Dimension getOldSize() { return oldSize; }

	public int print(Graphics g, PageFormat pf, int page)
		throws PrinterException
	{
		if (context instanceof TextWindow)
		{
			TextWindow tw = (TextWindow)context;
			if (page == 0)
			{
				allStrings = tw.convertToStrings();
				printFont = new Font("Helvetica", Font.PLAIN, 10);
				FontMetrics fm = g.getFontMetrics(printFont);
				pageHeight = (int)pf.getImageableHeight();
				fontHeight = fm.getHeight();
				yOffset = fm.getAscent();
				linesPerPage = pageHeight / fontHeight;
				startLine = startChar = 0;
				startPageNumber = 0;
				startLineAtPage = startLine;
				startCharAtPage = startChar;
				frc = new FontRenderContext(null, true, true);
			}
			if (page == startPageNumber)
			{
				startLine = startLineAtPage;
				startChar = startCharAtPage;
			} else
			{
				startPageNumber = page;
				startLineAtPage = startLine;
				startCharAtPage = startChar;
			}
			if (startLine < allStrings.length)
			{
				g.setColor(Color.WHITE);
				g.fillRect((int)pf.getImageableX(), (int)pf.getImageableY(),
					(int)pf.getImageableWidth(), (int)pf.getImageableHeight());
				g.setFont(printFont);
				g.setColor(Color.BLACK);
				for(int i=0; i<linesPerPage; i++)
				{
					if (startLine >= allStrings.length) break;
					String fullLine = allStrings[startLine];

					// figure bounding box of text
					int endChar = fullLine.length();
					String theLine = null;
					for(;;)
					{
						theLine = fullLine.substring(startChar, endChar);
						GlyphVector gv = printFont.createGlyphVector(frc, theLine);
						Rectangle2D rasRect = gv.getLogicalBounds();
						if (rasRect.getWidth() <= pf.getImageableWidth()) break;
						endChar--;
						if (endChar <= startChar) break;
					}

					g.drawString(theLine, (int)pf.getImageableX(), (int)pf.getImageableY() + yOffset + (i*fontHeight));
					if (endChar < fullLine.length())
					{
						// only did partial line
						startChar = endChar;
					} else
					{
						// completed the line
						startLine++;
						startChar = 0;
					}
				}
				return Printable.PAGE_EXISTS;
			}			
		} else if (context instanceof EditWindow)
		{
			// only 1 page
			if (page == 0)
			{
				graphics = g;
				pageFormat = pf;
				if (context.getPrintImage(this) != null) return Printable.PAGE_EXISTS;
			}
		}
		return Printable.NO_SUCH_PAGE;
	}

	/** This function is required for 3D view */
	public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
	{
		return false;
	}
}
