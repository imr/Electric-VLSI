package com.sun.electric.tool.user.ui;

import com.sun.electric.tool.io.IOTool;

import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.awt.image.ImageObserver;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.awt.Graphics;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Nov 30, 2004
 * Time: 5:26:47 PM
 * To change this template use File | Settings | File Templates.
 */
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
