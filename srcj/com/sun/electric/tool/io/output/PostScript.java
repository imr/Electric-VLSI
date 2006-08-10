/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PostScript.java
 * Input/output tool: PostScript output
 * Written by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.User;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;



/**
 * This class writes files in PostScript format.
 */
public class PostScript extends Output
{
	/** scale factor for PostScript */				private static final int    PSSCALE        =  4;
//	/** scale factor for PostScript text */			private static final double PSTEXTSCALE    =  0.45;
	/** scale factor for PostScript text */			private static final double PSTEXTSCALE    =  0.75;
	/** size of text in the corner */				private static final int    CORNERDATESIZE = 14;

	/** write macros for dot drawing */				private static final int HEADERDOT      =  1;
	/** write macros for line drawing */			private static final int HEADERLINE     =  2;
	/** write macros for polygon drawing */			private static final int HEADERPOLYGON  =  3;
	/** write macros for filled polygon drawing */	private static final int HEADERFPOLYGON =  4;
	/** write macros for text drawing */			private static final int HEADERSTRING   =  5;

	/** true if the "dot" header code has been written. */				private boolean putHeaderDot;
	/** true if the "line" header code has been written. */				private boolean putHeaderLine;
	/** true if the "polygon" header code has been written. */			private boolean putHeaderPolygon;
	/** true if the "filled polygon" header code has been written. */	private boolean putHeaderFilledPolygon;
	/** true if the "string" header code has been written. */			private boolean putHeaderString;
	/** true to generate color PostScript. */							private boolean psUseColor;
	/** true to generate stippled color PostScript. */					private boolean psUseColorStip;
	/** true to generate merged color PostScript. */					private boolean psUseColorMerge;
	/** the Cell being written. */										private Cell cell;
    private Rectangle2D printBounds;
    /** list of Polys to use instead of cell contents. */				private List<PolyBase> override;
	/** the EditWindow_ in which the cell resides. */					private EditWindow_ wnd;
	/** number of patterns emitted so far. */							private int psNumPatternsEmitted;
	/** list of patterns emitted so far. */								private HashMap<EGraphics,Integer> patternsEmitted;
	/** current layer number (-1: do all; 0: cleanup). */				private int currentLayer;
	/** the last color written out. */									private int lastColor;
	/** the normal width of lines. */									private int lineWidth;
	/** true to plot date information in the corner. */					private boolean plotDates;
	/** matrix from database units to PS units. */						private AffineTransform matrix;
	/** fake layer for drawing outlines and text. */					private static Layer blackLayer = Layer.newInstance("black",
		new EGraphics(false, false, null, 0, 100,100,100,1.0,true, new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

	/**
	 * Main entry point for PostScript output.
     * @param cell the top-level cell to write.
	 * @param filePath the disk file to create.
	 * @param override a list of overriding polygons to write.
	 */
	public static void writePostScriptFile(Cell cell, String filePath, List<PolyBase> override)
	{
		// just do this file
		writeCellToFile(cell, filePath, override);
	}

	private static boolean writeCellToFile(Cell cell, String filePath, List<PolyBase> override)
	{
		boolean error = false;
		PostScript out = new PostScript(cell, override);
		if (out.openTextOutputStream(filePath)) error = true; else
        {
			// write out the cell
			if (cell.getView().isTextView())
			{
				// text cell
				out.printWriter.println("Library: " + cell.getLibrary().getName() + "   Cell: " + cell.noLibDescribe());

				if (User.isIncludeDateAndVersionInOutput())
				{
					out.printWriter.println("   Created: " + TextUtils.formatDate(cell.getCreationDate()) +
						"   Revised: " + TextUtils.formatDate(cell.getRevisionDate()));
				}
				out.printWriter.println("\n\n");

				// print the text of the cell
				Variable var = cell.getVar(Cell.CELL_TEXT_KEY);
				if (var != null)
				{
					String [] strings = (String [])var.getObject();
					for(int i=0; i<strings.length; i++)
						out.printWriter.println(strings[i]);
				}
			} else
			{
				// layout/schematics cell
	            if (out.start())
	            {
		            out.scanCircuit();
		            out.done();
	            }
			}

            if (out.closeTextOutputStream()) error = true;
        }
		if (!error)
		{
			System.out.println(filePath + " written");
		}
		return error;
	}

	/** Creates a new instance of PostScript */
	private PostScript(Cell cell, List<PolyBase> override)
	{
        this.cell = cell;
        this.override = override;
	}

	/**
	 * Method to initialize for writing a cell.
	 * @return false to abort the process.
	 */
	private boolean start()
	{
		// find the edit window
		UserInterface ui = Job.getUserInterface();
		wnd = ui.getCurrentEditWindow_();
		if (wnd != null && wnd.getCell() != cell) wnd = null;

		// clear flags that tell whether headers have been included
		putHeaderDot = false;
		putHeaderLine = false;
		putHeaderPolygon = false;
		putHeaderFilledPolygon = false;
		putHeaderString = false;

		// get control options
		psUseColor = psUseColorStip = psUseColorMerge = false;
		switch (IOTool.getPrintColorMethod())
		{
			case 1:		// color
				psUseColor = true;
				break;
			case 2:		// color stippled
				psUseColor = psUseColorStip = true;
				break;
			case 3:		// color merged
				psUseColor = psUseColorMerge = true;
				break;
		}
		boolean usePlotter = IOTool.isPrintForPlotter();
		plotDates = IOTool.isPlotDate();
		boolean epsFormat = IOTool.isPrintEncapsulated();

		double pageWid = IOTool.getPrintWidth() * 75;
		double pageHei = IOTool.getPrintHeight() * 75;
		double pageMarginPS = IOTool.getPrintMargin() * 75;
		double pageMargin = pageMarginPS;		// not right!!!

		// determine the area of interest
		printBounds = null;
		if (override != null)
		{
			double lX=0, hX=0, lY=0, hY=0;
			boolean first = true;
			for(PolyBase poly : override)
			{
				Point2D [] points = poly.getPoints();
				for(int i=0; i<points.length; i++)
				{
					double x = points[i].getX();
					double y = points[i].getY();
					if (first)
					{
						first = false;
						lX = hX = x;
						lY = hY = y;
					} else
					{
						if (x < lX) lX = x;
						if (x > hX) hX = x;
						if (y < lY) lY = y;
						if (y > hY) hY = y;
					}
				}
			}
			printBounds = new Rectangle2D.Double(lX, lY, hX-lX, hY-lY);
		} else
		{
			printBounds = getAreaToPrint(cell, false, wnd);
		}
		if (printBounds == null) return false;

		boolean rotatePlot = false;
		switch (IOTool.getPrintRotation())
		{
			case 1:		// rotate 90 degrees
				rotatePlot = true;
				break;
			case 2:		// auto-rotate
				if (((pageHei > pageWid || usePlotter) && printBounds.getWidth() > printBounds.getHeight()) ||
					(pageWid > pageHei && printBounds.getHeight() > printBounds.getWidth()))
						rotatePlot = true;
				break;
		}

		// if plotting, compute height from width
		if (usePlotter)
		{
			if (rotatePlot)
			{
				pageHei = pageWid * printBounds.getWidth() / printBounds.getHeight();
			} else
			{
				pageHei = pageWid * printBounds.getHeight() / printBounds.getWidth();
			}
		}

		// for pure color plotting, use special merging code
		if (psUseColorMerge && override == null)
		{
			PostScriptColor.psColorPlot(this, cell, epsFormat, usePlotter, pageWid, pageHei, pageMarginPS);
			return false;
		}

		// PostScript: compute the transformation matrix
		double cX = printBounds.getCenterX();
		double cY = printBounds.getCenterY();
		double unitsX = (pageWid-pageMargin*2) * PSSCALE;
		double unitsY = (pageHei-pageMargin*2) * PSSCALE;
		if (epsFormat)
		{
			double scale = IOTool.getPrintEPSScale(cell);
			if (scale != 0)
			{
				unitsX *= scale;
				unitsY *= scale;
			}
		}
		double i, j;
		if (usePlotter)
		{
			i = unitsX / printBounds.getWidth();
			j = unitsX / printBounds.getHeight();
		} else
		{
			i = Math.min(unitsX / printBounds.getWidth(), unitsY / printBounds.getHeight());
			j = Math.min(unitsX / printBounds.getHeight(), unitsY / printBounds.getWidth());
		}
		if (rotatePlot) i = j;
		double matrix00 = i;   double matrix01 = 0;
		double matrix10 = 0;   double matrix11 = i;
		double matrix20 = - i * cX + unitsX / 2 + pageMarginPS * PSSCALE;
		double matrix21;
		if (usePlotter)
		{
			matrix21 = - i * printBounds.getMinY() + pageMarginPS * PSSCALE;
		} else
		{
			matrix21 = - i * cY + unitsY / 2 + pageMarginPS * PSSCALE;
		}
		matrix = new AffineTransform(matrix00, matrix01, matrix10, matrix11, matrix20, matrix21);

		// write PostScript header
		if (epsFormat) printWriter.println("%!PS-Adobe-2.0 EPSF-2.0"); else
			printWriter.println("%!PS-Adobe-1.0");
		printWriter.println("%%Title: " + cell.describe(false));
		if (User.isIncludeDateAndVersionInOutput())
		{
			printWriter.println("%%Creator: Electric VLSI Design System version " + Version.getVersion());
			Date now = new Date();
			printWriter.println("%%CreationDate: " + TextUtils.formatDate(now));
		} else
		{
			printWriter.println("%%Creator: Electric VLSI Design System");
		}
		if (epsFormat) printWriter.println("%%Pages: 0"); else
			printWriter.println("%%Pages: 1");
		emitCopyright("% ", "");

		// transform to PostScript units
		double bblx = printBounds.getMinX();
		double bbhx = printBounds.getMaxX();
		double bbly = printBounds.getMinY();
		double bbhy = printBounds.getMaxY();

		Point2D bbCorner1 = psXform(new Point2D.Double(bblx, bbly));
		Point2D bbCorner2 = psXform(new Point2D.Double(bbhx, bbhy));
		bblx = bbCorner1.getX();
		bbly = bbCorner1.getY();
		bbhx = bbCorner2.getX();
		bbhy = bbCorner2.getY();

		if (rotatePlot)
		{
			/*
			 * fiddle with the bbox if image rotated on page
			 * (at this point, bbox coordinates are absolute printer units)
			 */
			double t1 = bblx;
			double t2 = bbhx;
			bblx = -bbhy + pageHei * 300 / 75;
			bbhx = -bbly + pageHei * 300 / 75;
			bbly = t1 + pageMargin*2 * 300 / 75;		// this may not work because "pageMargin" is badly defined
			bbhy = t2 + pageMargin*2 * 300 / 75;
		}

		if (bblx > bbhx) { double s = bblx;  bblx = bbhx;  bbhx = s; }
		if (bbly > bbhy) { double s = bbly;  bbly = bbhy;  bbhy = s; }
		bblx = bblx / (PSSCALE * 75.0) * 72.0 * (bblx>=0 ? 1 : -1);
		bbly = bbly / (PSSCALE * 75.0) * 72.0 * (bbly>=0 ? 1 : -1);
		bbhx = bbhx / (PSSCALE * 75.0) * 72.0 * (bbhx>=0 ? 1 : -1);
		bbhy = bbhy / (PSSCALE * 75.0) * 72.0 * (bbhy>=0 ? 1 : -1);

		/*
		 * Increase the size of the bbox by one "pixel" to
		 * prevent the edges from being obscured by some drawing tools
		 */
		printWriter.println("%%BoundingBox: " + (int)(bblx-1) + " " + (int)(bbly-1) + " " + (int)(bbhx+1) + " " + (int)(bbhy+1));
		printWriter.println("%%DocumentFonts: Times-Roman");
		printWriter.println("%%EndComments");
		if (!epsFormat) printWriter.println("%%Page: 1 1");

		// PostScript: add some debugging info
		if (cell != null)
		{
			Rectangle2D bounds = cell.getBounds();
			printWriter.println("% cell dimensions: " + bounds.getWidth() + " wide x " + bounds.getHeight() + " high (database units)");
			printWriter.println("% origin: " + bounds.getMinX() + " " + bounds.getMinY());
		}

		// disclaimers
		if (epsFormat)
		{
			printWriter.println("% The EPS header should declare a private dictionary.");
		} else
		{
			printWriter.println("% The non-EPS header does not claim conformance to Adobe-2.0");
			printWriter.println("% because the structure may not be exactly correct.");
		}
		printWriter.println("%");

		// set the page size if this is a plotter
		if (usePlotter)
		{
			printWriter.println("<< /PageSize [" + (int)(pageWid * 72 / 75) + " " + (int)(pageHei * 72 / 75) + "] >> setpagedevice");
		}

		// make the scale be exactly equal to one page pixel
		printWriter.println("72 " + PSSCALE*75 + " div 72 " + PSSCALE*75 + " div scale");

		// set the proper typeface
		printWriter.println("/DefaultFont /Times-Roman def");
		printWriter.println("/scaleFont {");
		printWriter.println("    DefaultFont findfont");
		printWriter.println("    exch scalefont setfont} def");

		// make the line width proper
		lineWidth = (int)(PSSCALE/2 * IOTool.getPrintPSLineWidth());
		printWriter.println(lineWidth + " setlinewidth");

		// make the line ends look right
		printWriter.println("1 setlinecap");

		// rotate the image if requested
		if (rotatePlot)
		{
			if (usePlotter)
			{
				printWriter.println((pageWid/75) + " 300 mul " + ((pageHei-pageWid)/2/75) + " 300 mul translate 90 rotate");
			} else
			{
				printWriter.println((pageHei+pageWid)/2/75 + " 300 mul " + (pageHei-pageWid)/2/75 + " 300 mul translate 90 rotate");
			}
		}


		// initialize list of EGraphics modules that have been put out
		patternsEmitted = new HashMap<EGraphics,Integer>();
		psNumPatternsEmitted = 0;
		return true;
	}

	/**
	 * Method to clean-up writing a cell.
	 */
	private void done()
	{
		// draw the grid if requested
		if (psUseColor) printWriter.println("0 0 0 setrgbcolor");
		if (wnd != null && wnd.isGrid())
		{
			int gridx = (int)wnd.getGridXSpacing();
			int gridy = (int)wnd.getGridYSpacing();
			int lx = (int)cell.getBounds().getMinX();
			int ly = (int)cell.getBounds().getMinY();
			int hx = (int)cell.getBounds().getMaxX();
			int hy = (int)cell.getBounds().getMaxY();
			int gridlx = lx / gridx * gridx;
			int gridly = ly / gridy * gridy;

			// adjust to ensure that the first point is inside the range
			if (gridlx > lx) gridlx -= (gridlx - lx) / gridx * gridx;
			if (gridly > ly) gridly -= (gridly - ly) / gridy * gridy;
			while (gridlx < lx) gridlx += gridx;
			while (gridly < ly) gridly += gridy;

			// PostScript: write the grid loop
			double matrix00 = matrix.getScaleX();
			double matrix01 = matrix.getShearX();
			double matrix10 = matrix.getShearY();
			double matrix11 = matrix.getScaleY();
			double matrix20 = matrix.getTranslateX();
			double matrix21 = matrix.getTranslateY();
			printWriter.println(gridlx + " " + gridx + " " + hx);
			printWriter.println("{");
			printWriter.println("    " + gridly + " " + gridy + " " + hy);	// x y
			printWriter.println("    {");
			printWriter.println("        dup 3 -1 roll dup dup");				// y y x x x
			printWriter.println("        5 1 roll 3 1 roll");					// x y x y x
			printWriter.println("        " + matrix00 + " mul exch " + matrix10 + " mul add " + matrix20 + " add");		// x y x x'
			printWriter.println("        3 1 roll");							// x x' y x
			printWriter.println("        " + matrix01 + " mul exch " + matrix11 + " mul add " + matrix21 + " add");		// x x' y'
			printWriter.println("        newpath moveto 0 0 rlineto stroke");
			printWriter.println("    } for");
			printWriter.println("} for");
		}

		// draw frame if it is there
		PostScriptFrame pf = new PostScriptFrame(cell, this);
		pf.renderFrame();

		// put out dates if requested
		if (plotDates)
		{
			putPSHeader(HEADERSTRING);
			printWriter.print("0 " + (int)(2 * CORNERDATESIZE * PSSCALE) + " ");
			writePSString("Cell: " + cell.describe(false));
			printWriter.println(" " + (int)(CORNERDATESIZE * PSSCALE) + " Botleftstring");

			printWriter.print("0 " + (int)(CORNERDATESIZE * PSSCALE) + " ");
			writePSString("Created: " + TextUtils.formatDate(cell.getCreationDate()));
			printWriter.println(" " + (int)(CORNERDATESIZE * PSSCALE) + " Botleftstring");

			printWriter.print("0 0 ");
			writePSString("Revised: " + TextUtils.formatDate(cell.getRevisionDate()));
			printWriter.println(" " + (int)(CORNERDATESIZE * PSSCALE) + " Botleftstring");
		}

		printWriter.println("showpage");
		printWriter.println("%%Trailer");
	}

	/**
	 * Class for rendering a cell frame to the PostScript.
	 * Extends Cell.FrameDescription and provides hooks for drawing to a Graphics.
	 */
	private static class PostScriptFrame extends Cell.FrameDescription
	{
		private PostScript writer;

		/**
		 * Constructor for cell frame rendering.
		 * @param cell the Cell that is having a frame drawn.
		 * @param writer the PostScript object for access to field variables.
		 */
		public PostScriptFrame(Cell cell, PostScript writer)
		{
			super(cell, 0);
			this.writer = writer;
		}

		/**
		 * Method to draw a line in a frame.
		 * @param from the starting point of the line (in database units).
		 * @param to the ending point of the line (in database units).
		 */
		public void showFrameLine(Point2D from, Point2D to)
		{
			writer.psLine(from, to, 0);
		}

		/**
		 * Method to draw text in a frame.
		 * @param ctr the anchor point of the text.
		 * @param size the size of the text (in database units).
		 * @param maxWid the maximum width of the text (ignored if zero).
		 * @param maxHei the maximum height of the text (ignored if zero).
		 * @param string the text to be displayed.
		 */
		public void showFrameText(Point2D ctr, double size, double maxWid, double maxHei, String string)
		{
			Poly poly = null;
			if (maxWid > 0 && maxHei > 0)
			{
				poly = new Poly(ctr.getX(), ctr.getY(), maxWid, maxHei);
				poly.setStyle(Poly.Type.TEXTBOX);
			} else
			{
				Point2D [] points = new Point2D[1];
				points[0] = ctr;
				poly = new Poly(points);
				poly.setStyle(Poly.Type.TEXTCENT);
			}
			poly.setString(string);
			TextDescriptor td = TextDescriptor.getNodeTextDescriptor().withRelSize(size * 0.75);
			poly.setTextDescriptor(td);
			writer.psText(poly);
		}
	}

	/****************************** TRAVERSING THE HIERARCHY ******************************/

	/**
	 * Method to write a the entire cell.
	 */
	private void scanCircuit()
	{
		lastColor = -1;

        if (override != null)
        {
			for (PolyBase poly : override)
			{
				Point2D [] pts = poly.getPoints();
				for(int i=0; i<pts.length; i++)
					pts[i].setLocation(pts[i].getX(), printBounds.getHeight() - pts[i].getY());
			}
        }
		if (psUseColor)
		{
			// color: plot layers in proper order
			List<Layer> layerList = Technology.getCurrent().getLayersSortedByHeight();
			for(Layer layer : layerList)
			{
				currentLayer = layer.getIndex() + 1;
				recurseCircuitLevel(cell, DBMath.MATID, true);
			}
			currentLayer = 0;
			recurseCircuitLevel(cell, DBMath.MATID, true);
		} else
		{
			// gray-scale: just plot it once
			currentLayer = -1;
			recurseCircuitLevel(cell, DBMath.MATID, true);
		}
	}

	private void recurseCircuitLevel(Cell cell, AffineTransform trans, boolean topLevel)
	{
        if (override != null)
        {
			for (PolyBase poly : override)
			{
				psPoly(poly);
			}
			return;
        }

        // write the cells
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			AffineTransform subRot = ni.rotateOut();
			subRot.preConcatenate(trans);

			if (!ni.isCellInstance())
			{
				if (!topLevel && ni.isVisInside()) continue;
				PrimitiveNode prim = (PrimitiveNode)ni.getProto();
				Technology tech = prim.getTechnology();
				Poly [] polys = tech.getShapeOfNode(ni, wnd);
				for (int i=0; i<polys.length; i++)
				{
					polys[i].transform(subRot);
					psPoly(polys[i]);
				}
			} else
			{
				// a cell
				Cell subCell = (Cell)ni.getProto();
				AffineTransform subTrans = ni.translateOut();
				subTrans.preConcatenate(subRot);
				if (!ni.isExpanded())
				{
					Rectangle2D bounds = subCell.getBounds();
					Poly poly = new Poly(bounds.getCenterX(), bounds.getCenterY(), ni.getXSize(), ni.getYSize());
					poly.transform(subTrans);
					poly.setStyle(Poly.Type.CLOSED);
					poly.setLayer(blackLayer);
					psPoly(poly);

                    // Only when the instance names flag is on
                    if (User.isTextVisibilityOnInstance())
                    {
                        poly.setStyle(Poly.Type.TEXTBOX);
                        TextDescriptor td = TextDescriptor.getInstanceTextDescriptor().withAbsSize(24);
                        poly.setTextDescriptor(td);
                        poly.setString(ni.getProto().describe(false));
                    }
					psPoly(poly);
					showCellPorts(ni, trans, null);
				} else
				{
					recurseCircuitLevel(subCell, subTrans, false);
					showCellPorts(ni, trans, Color.BLACK);
				}

				// draw any displayable variables on the instance
				if (User.isTextVisibilityOnNode())
				{
					int numPolys = ni.numDisplayableVariables(true);
					Poly [] polys = new Poly[numPolys];
					Rectangle2D rect = ni.getUntransformedBounds();
//					Rectangle2D rect = ni.getBounds();
					ni.addDisplayableVariables(rect, polys, 0, wnd, true);
					for (int i=0; i<polys.length; i++)
					{
						if (polys[i] == null) break;
						polys[i].transform(subRot);
						psPoly(polys[i]);
					}
				}
			}

			// draw any exports from the node
			if (topLevel && User.isTextVisibilityOnExport())
			{
				int exportDisplayLevel = User.getExportDisplayLevel();
				for(Iterator<Export> eIt = ni.getExports(); eIt.hasNext(); )
				{
					Export e = eIt.next();
					Poly poly = e.getNamePoly();
					if (exportDisplayLevel == 2)
					{
						// draw port as a cross
						drawCross(poly.getCenterX(), poly.getCenterY(), false);
					} else
					{
						// draw port as text
						if (exportDisplayLevel == 1)
						{
							// use shorter port name
							String portName = e.getShortName();
							poly.setString(portName);
						}
						psPoly(poly);
					}

					// draw variables on the export
					int numPolys = e.numDisplayableVariables(true);
					if (numPolys > 0)
					{
						Rectangle2D rect = (Rectangle2D)poly.getBounds2D().clone();
						Poly [] polys = new Poly[numPolys];
						e.addDisplayableVariables(rect, polys, 0, wnd, true);
						for (int i=0; i<polys.length; i++)
						{
							psPoly(polys[i]);
						}
					}
				}
			}
		}

		// write the arcs
		for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext();)
		{
    		ArcInst ai = it.next();
			ArcProto ap = ai.getProto();
			Technology tech = ap.getTechnology();
			Poly [] polys = tech.getShapeOfArc(ai, wnd);
			for (int i=0; i<polys.length; i++)
			{
				polys[i].transform(trans);
				psPoly(polys[i]);
			}
        }

		// show cell variables if at the top level
		if (topLevel && User.isTextVisibilityOnCell())
		{
			// show displayable variables on the instance
			int numPolys = cell.numDisplayableVariables(true);
			Poly [] polys = new Poly[numPolys];
			Rectangle2D CENTERRECT = new Rectangle2D.Double(0, 0, 0, 0);
			cell.addDisplayableVariables(CENTERRECT, polys, 0, wnd, true);
			for (int i=0; i<polys.length; i++)
			{
				psPoly(polys[i]);
			}
		}
	}

	private void showCellPorts(NodeInst ni, AffineTransform trans, Color col)
	{
		// show the ports that are not further exported or connected
		int numPorts = ni.getProto().getNumPorts();
		boolean[] shownPorts = new boolean[numPorts];
		for(Iterator<Connection> it = ni.getConnections(); it.hasNext();)
		{
			Connection con = it.next();
			PortInst pi = con.getPortInst();
			shownPorts[pi.getPortIndex()] = true;
		}
		for(Iterator<Export> it = ni.getExports(); it.hasNext();)
		{
			Export exp = it.next();
			PortInst pi = exp.getOriginalPort();
			shownPorts[pi.getPortIndex()] = true;
		}
		int portDisplayLevel = User.getPortDisplayLevel();
		for(int i = 0; i < numPorts; i++)
		{
			if (shownPorts[i]) continue;
			Export pp = (Export)ni.getProto().getPort(i);
	
			Poly portPoly = ni.getShapeOfPort(pp);
			if (portPoly == null) continue;
			portPoly.transform(trans);
			Color portColor = col;
			if (portColor == null) portColor = pp.getBasePort().getPortColor();
			setColor(portColor);
			if (portDisplayLevel == 2)
			{
				// draw port as a cross
				drawCross(portPoly.getCenterX(), portPoly.getCenterY(), false);
			} else
			{
				// draw port as text
				if (User.isTextVisibilityOnPort())
				{
					// combine all features of port text with color of the port
					TextDescriptor descript = portPoly.getTextDescriptor();
					TextDescriptor portDescript = pp.getTextDescriptor(Export.EXPORT_NAME).withColorIndex(descript.getColorIndex());
					Poly.Type type = descript.getPos().getPolyType();
					portPoly.setStyle(type);
					String portName = pp.getName();
					if (portDisplayLevel == 1)
					{
						// use shorter port name
						portName = pp.getShortName();
					}
					portPoly.setString(portName);
					portPoly.setTextDescriptor(portDescript);
					psText(portPoly);
				}
			}
		}
	}

	/****************************** EPS SYNCHRONIZATION ******************************/

	/**
	 * Method to synchronize all PostScript files that need it.
	 * Examines all of the synchronization paths in all libraries.
	 * If they exist, the user is asked if synchronization should be done.
	 */
	public static boolean syncAll()
	{
		// see if there are synchronization links
		boolean syncOther = false;
		for(Iterator<Library> lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = lIt.next();
			for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell aCell = cIt.next();
				Variable var = aCell.getVar(IOTool.POSTSCRIPT_FILENAME);
				if (var != null)
				{
					String fileName = (String)var.getObject();
					if (fileName.trim().length() > 0)
					{
						syncOther = true;
						break;
					}
				}
			}
			if (syncOther) break;
		}
		if (syncOther)
		{
			String [] options = {"Yes", "No"};
			int ret = Job.getUserInterface().askForChoice("Would you like to synchronize all PostScript drawings?",
				"Synchronize EPS files", options, options[1]);
			if (ret == 1) syncOther = false;
		}
		if (syncOther)
		{
			synchronizeEPSFiles();
			return true;
		}
		return false;
	}

	private static boolean synchronizeEPSFiles()
	{
		// synchronize all cells
		int numSyncs = 0;
		for(Iterator<Library> lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library oLib = lIt.next();
			for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
			{
				Cell oCell = cIt.next();
				String syncFileName = IOTool.getPrintEPSSynchronizeFile(oCell);
				if (syncFileName.length() == 0) continue;

				// existing file: check the date to see if it should be overwritten
				Date lastSavedDate = IOTool.getPrintEPSSavedDate(oCell);
				if (lastSavedDate != null)
				{
					Date lastChangeDate = oCell.getRevisionDate();
					if (lastSavedDate.after(lastChangeDate)) continue;
				}
				boolean err = writeCellToFile(oCell, syncFileName, null);
				if (err) return true;

				// mark the synchronized date
				IOTool.setPrintEPSSavedDate(oCell, new Date());

				numSyncs++;
			}
		}
		if (numSyncs == 0)
			System.out.println("No PostScript files needed to be written");
		return false;
	}
	
	/****************************** WRITE POLYGON ******************************/

	private void setColor(Color col)
	{
		if (psUseColor)
		{
			if (col.getRGB() != lastColor)
			{
				lastColor = col.getRGB();
				printWriter.println(col.getRed()/255.0f + " " + col.getGreen()/255.0f + " " + col.getBlue()/255.0f + " setrgbcolor");
			}
		}
	}

	/**
	 * Method to plot the polygon "poly"
	 */
	private void psPoly(PolyBase poly)
	{
		// ignore null layers
		Layer layer = poly.getLayer();
		EGraphics gra = null;
		int index = 0;
		Color col = Color.BLACK;
		Technology tech = Technology.getCurrent();
		if (layer == null)
		{
		} else
		{
			tech = layer.getTechnology();
			index = layer.getIndex();
			if (!layer.isVisible()) return;
			gra = layer.getGraphics();
			col = gra.getColor();
		}

		// ignore layers that are not supposed to be dumped at this time
		if (currentLayer >= 0)
		{
			if (currentLayer == 0)
			{
				if (tech == Technology.getCurrent()) return;
			} else
			{
				if (tech != Technology.getCurrent() || currentLayer-1 != index)
					return;
			}
		}

		// set color if requested
		setColor(col);

		Poly.Type type = poly.getStyle();
		Point2D [] points = poly.getPoints();
		if (type == Poly.Type.FILLED)
		{
			Rectangle2D polyBox = poly.getBox();
			if (polyBox != null)
			{
				if (polyBox.getWidth() == 0)
				{
					if (polyBox.getHeight() == 0) psDot(new Point2D.Double(polyBox.getCenterX(), polyBox.getCenterY())); else
						psLine(new Point2D.Double(polyBox.getCenterX(), polyBox.getMinY()),
							new Point2D.Double(polyBox.getCenterX(), polyBox.getMaxY()), 0);
					return;
				} else if (polyBox.getHeight() == 0)
				{
					psLine(new Point2D.Double(polyBox.getMinX(), polyBox.getCenterY()),
						new Point2D.Double(polyBox.getMaxX(), polyBox.getCenterY()), 0);
					return;
				}
				psPolygon(poly);
				return;
			}
			if (points.length == 1)
			{
				psDot(points[0]);
				return;
			}
			if (points.length == 2)
			{
				psLine(points[0], points[1], 0);
				return;
			}
			psPolygon(poly);
			return;
		}
		if (type == Poly.Type.CLOSED)
		{
			Point2D lastPt = points[points.length-1];
			for (int k = 0; k < points.length; k++)
			{
				psLine(lastPt, points[k], 0);
				lastPt = points[k];
			}
			return;
		}
		if (type == Poly.Type.OPENED || type == Poly.Type.OPENEDT1 ||
			type == Poly.Type.OPENEDT2 || type == Poly.Type.OPENEDT3)
		{
			int lineType = 0;
			if (type == Poly.Type.OPENEDT1) lineType = 1; else
			if (type == Poly.Type.OPENEDT2) lineType = 2; else
			if (type == Poly.Type.OPENEDT3) lineType = 3;
			for (int k = 1; k < points.length; k++)
				psLine(points[k-1], points[k], lineType);
			return;
		}
		if (type == Poly.Type.VECTORS)
		{
			for(int k=0; k<points.length; k += 2)
				psLine(points[k], points[k+1], 0);
			return;
		}
		if (type == Poly.Type.CROSS || type == Poly.Type.BIGCROSS)
		{
			double x = poly.getCenterX();
			double y = poly.getCenterY();
			drawCross(x, y, type == Poly.Type.BIGCROSS);
			return;
		}
		if (type == Poly.Type.CROSSED)
		{
			Rectangle2D bounds = poly.getBounds2D();
			psLine(new Point2D.Double(bounds.getMinX(), bounds.getMinY()), new Point2D.Double(bounds.getMinX(), bounds.getMaxY()), 0);
			psLine(new Point2D.Double(bounds.getMinX(), bounds.getMaxY()), new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()), 0);
			psLine(new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()), new Point2D.Double(bounds.getMaxX(), bounds.getMinY()), 0);
			psLine(new Point2D.Double(bounds.getMaxX(), bounds.getMinY()), new Point2D.Double(bounds.getMinX(), bounds.getMinY()), 0);
			psLine(new Point2D.Double(bounds.getMinX(), bounds.getMinY()), new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()), 0);
			psLine(new Point2D.Double(bounds.getMinX(), bounds.getMaxY()), new Point2D.Double(bounds.getMaxX(), bounds.getMinY()), 0);
			return;
		}
		if (type == Poly.Type.DISC)
		{
			psDisc(points[0], points[1]);
			type = Poly.Type.CIRCLE;
		}
		if (type == Poly.Type.CIRCLE || type == Poly.Type.THICKCIRCLE)
		{
			psCircle(points[0], points[1]);
			return;
		}
		if (type == Poly.Type.CIRCLEARC || type == Poly.Type.THICKCIRCLEARC)
		{
			psArc(points[0], points[1], points[2]);
			return;
		}

		// text
		psText((Poly)poly);
	}

	/**
	 * draw a cross.
	 * @param x
	 * @param y
	 * @param bigCross
	 */
	private void drawCross(double x, double y, boolean bigCross)
	{
		double amount = 0.25;
		if (bigCross) amount = 0.5;
		psLine(new Point2D.Double(x-amount, y), new Point2D.Double(x+amount, y), 0);
		psLine(new Point2D.Double(x, y+amount), new Point2D.Double(x, y-amount), 0);
	}

	/**
	 * draw a dot
	 */
	private void psDot(Point2D pt)
	{
		Point2D ps = psXform(pt);
		putPSHeader(HEADERDOT);
		printWriter.println(TextUtils.formatDouble(ps.getX()) + " " + TextUtils.formatDouble(ps.getY()) + " Putdot");
	}

	/**
	 * draw a line
	 */
	private void psLine(Point2D from, Point2D to, int pattern)
	{
		Point2D pt1 = psXform(from);
		Point2D pt2 = psXform(to);
		putPSHeader(HEADERLINE);
		int i = PSSCALE / 2;
		switch (pattern)
		{
			case 0:
				printWriter.println(TextUtils.formatDouble(pt1.getX()) + " " + TextUtils.formatDouble(pt1.getY()) + " " +
					TextUtils.formatDouble(pt2.getX()) + " " + TextUtils.formatDouble(pt2.getY()) + " Drawline");
				break;
			case 1:
				printWriter.print("[" + i + " " + i*3 + "] 0 setdash ");
				printWriter.println(TextUtils.formatDouble(pt1.getX()) + " " + TextUtils.formatDouble(pt1.getY()) + " " +
					TextUtils.formatDouble(pt2.getX()) + " " + TextUtils.formatDouble(pt2.getY()) + " Drawline");
				printWriter.println(" [] 0 setdash");
				break;
			case 2:
				printWriter.print("[" + i*6 + " " + i*3 + "] 0 setdash ");
				printWriter.println(TextUtils.formatDouble(pt1.getX()) + " " + TextUtils.formatDouble(pt1.getY()) + " " +
					TextUtils.formatDouble(pt2.getX()) + " " + TextUtils.formatDouble(pt2.getY()) + " Drawline");
				printWriter.println(" [] 0 setdash");
				break;
			case 3:
				printWriter.print((lineWidth*2) + " setlinewidth ");
				printWriter.println(TextUtils.formatDouble(pt1.getX()) + " " + TextUtils.formatDouble(pt1.getY()) + " " +
					TextUtils.formatDouble(pt2.getX()) + " " + TextUtils.formatDouble(pt2.getY()) + " Drawline");
				printWriter.println(lineWidth + " setlinewidth");
				break;
		}
	}

	/**
	 * draw an arc of a circle
	 */
	private void psArc(Point2D center, Point2D pt1, Point2D pt2)
	{
		Point2D pc = psXform(center);
		Point2D ps1 = psXform(pt1);
		Point2D ps2 = psXform(pt2);
		double radius = pc.distance(ps1);
		int startAngle = (DBMath.figureAngle(pc, ps2) + 5) / 10;
		int endAngle = (DBMath.figureAngle(pc, ps1) + 5) / 10;
		printWriter.println("newpath " + TextUtils.formatDouble(pc.getX()) + " " + TextUtils.formatDouble(pc.getY()) + " " + radius + " " +
			startAngle + " " + endAngle + " arc stroke");
	}

	/**
	 * draw a circle (unfilled)
	 */
	private void psCircle(Point2D center, Point2D pt)
	{
		Point2D pc = psXform(center);
		Point2D ps = psXform(pt);
		double radius = pc.distance(ps);
		printWriter.println("newpath " + TextUtils.formatDouble(pc.getX()) + " " + TextUtils.formatDouble(pc.getY()) + " " + radius + " 0 360 arc stroke");
	}

	/**
	 * draw a filled circle
	 */
	private void psDisc(Point2D center, Point2D pt)
	{
		Point2D pc = psXform(center);
		Point2D ps = psXform(pt);
		double radius = pc.distance(ps);
		printWriter.println("newpath " + TextUtils.formatDouble(pc.getX()) + " " + TextUtils.formatDouble(pc.getY()) + " " + radius + " 0 360 arc fill");
	}

	/**
	 * draw a polygon
	 */
	private void psPolygon(PolyBase poly)
	{
		Point2D [] points = poly.getPoints();
		if (points.length == 0) return;
		// ignore if too small
		Rectangle2D polyBounds = null;
		for(int i=0; i<points.length; i++)
		{
			Point2D pu = psXform(points[i]);
			if (polyBounds == null) polyBounds = new Rectangle2D.Double(pu.getX(), pu.getY(), 0, 0); else
				polyBounds.add(pu);
		}
		if (polyBounds.getWidth() < 1 || polyBounds.getHeight() < 1) return;

		EGraphics desc = poly.getLayer().getGraphics();

		// use solid color if solid pattern or no pattern
		boolean stipplePattern = desc.isPatternedOnPrinter();

		// if stipple pattern is solid, just use solid fill
		if (stipplePattern)
		{
			int [] pattern = desc.getPattern();
			boolean solid = true;
			for(int i=0; i<8; i++)
				if (pattern[i] != 0xFFFF) { solid = false;   break; }
			if (solid) stipplePattern = false;
		}

		// put out solid fill if appropriate
		if (!stipplePattern)
		{
			putPSHeader(HEADERPOLYGON);
			printWriter.print("[");
			for(int i=0; i<points.length; i++)
			{
				if (i != 0) printWriter.print(" ");
				Point2D ps = psXform(points[i]);
				printWriter.print(TextUtils.formatDouble(ps.getX()) + " " + TextUtils.formatDouble(ps.getY()));
			}
			printWriter.println("] Polygon fill");
			return;
		}

		/*
		 * patterned fill: the hard one
		 * Generate filled polygons by defining a stipple font and then tiling the
		 * polygon to fill with 128x128 pixel characters, clipping to the polygon edge.
		 */
		putPSHeader(HEADERPOLYGON);
		putPSHeader(HEADERFPOLYGON);
		printWriter.print("(" + psPattern(desc) + ") [");
		Point2D ps = psXform(points[0]);
		double lx = ps.getX();
		double hx = lx;
		double ly = ps.getY();
		double hy = ly;
		for(int i=0; i<points.length; i++)
		{
			if (i != 0) printWriter.print(" ");
			Point2D psi = psXform(points[i]);
			printWriter.print(psi.getX() + " " + psi.getY());
			if (psi.getX() < lx) lx = psi.getX();   if (psi.getX() > hx) hx = psi.getX();
			if (psi.getY() < ly) ly = psi.getY();   if (psi.getY() > hy) hy = psi.getY();
		}
		printWriter.println("] " + TextUtils.formatDouble(hx-lx+1) + " " + TextUtils.formatDouble(hy-ly+1) + " " +
			TextUtils.formatDouble(lx) + " " + TextUtils.formatDouble(ly) + " Filledpolygon");
	}

	String defaultFontName = "Times";

	/**
	 * draw text
	 */
	private void psText(Poly poly)
	{
		Poly.Type style = poly.getStyle();
		TextDescriptor td = poly.getTextDescriptor();
		if (td == null) return;
		int size = (int)(td.getTrueSize(wnd) * PSTEXTSCALE * PSSCALE);
		Rectangle2D bounds = poly.getBounds2D();

		// get the font size
		if (size <= 0) return;

		// make sure the string is valid
		String text = poly.getString().trim();
		if (text.length() == 0) return;

		// write header information
		Point2D psL = psXform(new Point2D.Double(bounds.getMinX(), bounds.getMinY()));
		Point2D psH = psXform(new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()));
		double cX = (psL.getX() + psH.getX()) / 2;
		double cY = (psL.getY() + psH.getY()) / 2;
		double sX = Math.abs(psH.getX() - psL.getX());
		double sY = Math.abs(psH.getY() - psL.getY());
//System.out.println("FOR STRING '"+text+"' CENTER IS ("+poly.getCenterX()+","+poly.getCenterY()+") SIZE IS "+poly.getBounds2D().getWidth()+"x"+poly.getBounds2D().getHeight());
		putPSHeader(HEADERSTRING);

		boolean changedFont = false;
		String faceName = null;
		int faceNumber = td.getFace();
		if (faceNumber != 0)
		{
			TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(faceNumber);
			if (af != null) faceName = af.getName();
		}
		if (faceName != null)
		{
			String fixedFaceName = faceName.replace(' ', '-');
			printWriter.println("/DefaultFont /" + fixedFaceName + " def");
			changedFont = true;
		} else
		{
			if (td.isItalic())
			{
				if (td.isBold())
				{
					printWriter.println("/DefaultFont /" + defaultFontName + "-BoldItalic def");
					changedFont = true;
				} else
				{
					printWriter.println("/DefaultFont /" + defaultFontName + "-Italic def");
					changedFont = true;
				}
			} else if (td.isBold())
			{
				printWriter.println("/DefaultFont /" + defaultFontName + "-Bold def");
				changedFont = true;
			}
		}
		if (poly.getStyle() == Poly.Type.TEXTBOX)
		{
			printWriter.print(TextUtils.formatDouble(cX) + " " + TextUtils.formatDouble(cY) + " " +
				TextUtils.formatDouble(sX) + " " + TextUtils.formatDouble(sY) + " ");
			writePSString(text);
			printWriter.println(" " + size + " Boxstring");
		} else
		{
			String opName = null;
			double x = 0, y = 0;
			if (style == Poly.Type.TEXTCENT)
			{
				x = cX;   y = cY;
				opName = "Centerstring";
			} else if (style == Poly.Type.TEXTTOP)
			{
				x = cX;   y = psH.getY();
				opName = "Topstring";
			} else if (style == Poly.Type.TEXTBOT)
			{
				x = cX;   y = psL.getY();
				opName = "Botstring";
			} else if (style == Poly.Type.TEXTLEFT)
			{
				x = psL.getX();   y = cY;
				opName = "Leftstring";
			} else if (style == Poly.Type.TEXTRIGHT)
			{
				x = psH.getX();   y = cY;
				opName = "Rightstring";
			} else if (style == Poly.Type.TEXTTOPLEFT)
			{
				x = psL.getX();   y = psH.getY();
				opName = "Topleftstring";
			} else if (style == Poly.Type.TEXTTOPRIGHT)
			{
				x = psH.getX();   y = psH.getY();
				opName = "Toprightstring";
			} else if (style == Poly.Type.TEXTBOTLEFT)
			{
				x = psL.getX();   y = psL.getY();
				opName = "Botleftstring";
			} else if (style == Poly.Type.TEXTBOTRIGHT)
			{
				x = psH.getX();   y = psL.getY();
				opName = "Botrightstring";
			}
			int xoff = (int)x,  yoff = (int)y;
			double descenderoffset = size / 12;
			TextDescriptor.Rotation rot = td.getRotation();
			if (rot == TextDescriptor.Rotation.ROT0) y += descenderoffset; else
			if (rot == TextDescriptor.Rotation.ROT90) x -= descenderoffset; else
			if (rot == TextDescriptor.Rotation.ROT180) y -= descenderoffset; else
			if (rot == TextDescriptor.Rotation.ROT270) x += descenderoffset;
			if (rot != TextDescriptor.Rotation.ROT0)
			{
				if (rot == TextDescriptor.Rotation.ROT90 || rot == TextDescriptor.Rotation.ROT270)
				{
					if (style == Poly.Type.TEXTTOP) opName = "Rightstring"; else
					if (style == Poly.Type.TEXTBOT) opName = "Leftstring"; else
					if (style == Poly.Type.TEXTLEFT) opName = "Botstring"; else
					if (style == Poly.Type.TEXTRIGHT) opName = "Topstring"; else
					if (style == Poly.Type.TEXTTOPLEFT) opName = "Botrightstring"; else
					if (style == Poly.Type.TEXTBOTRIGHT) opName = "Topleftstring";
				}
				x = y = 0;
				if (rot == TextDescriptor.Rotation.ROT90)
				{
					// 90 degrees counterclockwise
					printWriter.println(xoff + " " + yoff + " translate 90 rotate");
				} else if (rot == TextDescriptor.Rotation.ROT180)
				{
					// 180 degrees
					printWriter.println(xoff + " " + yoff + " translate 180 rotate");
				} else if (rot == TextDescriptor.Rotation.ROT270)
				{
					// 90 degrees clockwise
					printWriter.println(xoff + " " + yoff + " translate 270 rotate");
				}
			}
			printWriter.print(TextUtils.formatDouble(x) + " " + TextUtils.formatDouble(y) + " ");
			writePSString(text);
			printWriter.println(" " + size + " " + opName);
			if (rot != TextDescriptor.Rotation.ROT0)
			{
				if (rot == TextDescriptor.Rotation.ROT90)
				{
					// 90 degrees counterclockwise
					printWriter.println("-90 rotate " + (-xoff) + " " + (-yoff) + " translate");
				} else if (rot == TextDescriptor.Rotation.ROT180)
				{
					// 180 degrees
					printWriter.println("-180 rotate " + (-xoff) + " " + (-yoff) + " translate");
				} else if (rot == TextDescriptor.Rotation.ROT270)
				{
					// 90 degrees clockwise
					printWriter.println("-270 rotate " + (-xoff) + " " + (-yoff) + " translate");
				}
			}
		}

		if (changedFont)
		{
			printWriter.println("/DefaultFont /" + defaultFontName + " def");
		}
	}

	/****************************** SUPPORT ******************************/

	private String [] headerDot =
	{
		"/Putdot {",				// print dot at stack pos
		"    newpath moveto 0 0 rlineto stroke} def"
	};

	private String [] headerLine =
	{
		"/Drawline {",				// draw line on stack
		"    newpath moveto lineto stroke} def"
	};

	private String [] headerPolygon =
	{
		"/Polygon {",				// put array into path
		"    aload",
		"    length 2 idiv /len exch def",
		"    newpath",
		"    moveto",
		"    len 1 sub {lineto} repeat",
		"    closepath",
		"} def"
	};

	private String [] headerFilledPolygon =
	{
		"/BuildCharDict 10 dict def",	// ref Making a User Defined (PostScript Cookbook)

		"/StippleFont1 7 dict def",
		"StippleFont1 begin",
		"    /FontType 3 def",
		"    /FontMatrix [1 0 0 1 0 0] def",
		"    /FontBBox [0 0 1 1] def",
		"    /Encoding 256 array def",
		"    0 1 255 {Encoding exch /.notdef put} for",
		"    /CharacterDefs 40 dict def",
		"    CharacterDefs /.notdef {} put",
		"    /BuildChar",
		"        { BuildCharDict begin",
		"            /char exch def",
		"            /fontdict exch def",
		"            /charname fontdict /Encoding get",
		"            char get def",
		"            /charproc fontdict /CharacterDefs get",
		"            charname get def",
		"            1 0 0 0 1 1 setcachedevice",
		"            gsave charproc grestore",
		"        end",
		"    } def",
		"end",

		"/StippleFont StippleFont1 definefont pop",

		"/StippleCharYSize 128 def",
		"/StippleCharXSize StippleCharYSize def",

		"/Filledpolygon {",
		"    gsave",
		"    /StippleFont findfont StippleCharYSize scalefont setfont",
		"    /LowY exch def /LowX exch def",
		"    /HighY exch LowY add def /HighX exch LowX add def",
		"    Polygon clip",
		"    /Char exch def",
		"    /LowY LowY StippleCharYSize div truncate StippleCharYSize mul def",
		"    /LowX LowX StippleCharXSize div truncate StippleCharXSize mul def",
		"    /HighY HighY StippleCharYSize div 1 add truncate StippleCharYSize mul def",
		"    /HighX HighX StippleCharXSize div 1 add truncate StippleCharXSize mul def",
		"    LowY StippleCharYSize HighY ",
		"    { LowX exch moveto ",
		"        LowX StippleCharXSize HighX ",
		"        { Char show pop ",
		"        } for ",
		"    } for",
		"    grestore",
		"} def"
	};

	String [] headerString =
	{
		/*
		* Code to do super and subscripts:
		*
		* example:
		*	"NORMAL\dSUB\}   NORMAL\ uSUP\}"
		*
		* will subscript "SUB" and superscript "SUP", so "\d"  starts a
		* subscript, "\ u" starts a superscript, "\}" returns to
		* normal.  Sub-subscripts, and super-superscripts are not
		* supported.  To print a "\", use "\\".
		*
		* changes:
		*
		* all calls to stringwidth were changed to calls to StringLength,
		*    which returns the same info (assumes non-rotated text), but
		*    takes sub- and super-scripts into account.
		* all calls to show were changes to calls to StringShow, which
		*    handles sub- and super-scripts.
		* note that TSize is set to the text height, and is passed to
		*    StringLength and StringShow.
		*/
		"/ComStart 92 def",								// "\", enter command mode
		"/ComSub  100 def",								// "d", start subscript
		"/ComSup  117 def",								// "u", start superscript
		"/ComNorm 125 def",								// "}", return to normal
		"/SSSize .70 def",								// sub- and super-script size
		"/SubDy  -.20 def",								// Dy for sub-script
		"/SupDy   .40 def",								// Dy for super-script*/

		"/StringShow {",								// str size StringShow
		"    /ComMode 0 def",							// command mode flag
		"    /TSize exch def",							// text size
		"    /TString exch def",						// string to draw
		"    /NormY currentpoint exch pop def",			// save Y coord of string
		"    TSize scaleFont",
		"    TString {",								// scan string char by char
		"        /CharCode exch def",					// save char
		"        ComMode 1 eq {",
		"            /ComMode 0 def",					// command mode
		"            CharCode ComSub eq {",				// start subscript
		"                TSize SSSize mul scaleFont",
		"                currentpoint pop NormY TSize SubDy mul add moveto",
		"            } if",
		"            CharCode ComSup eq {",				// start superscript
		"                TSize SSSize mul scaleFont",
		"                currentpoint pop NormY TSize SupDy mul add moveto",
		"            } if",
		"            CharCode ComNorm eq {",			// end sub- or super-script
		"                TSize scaleFont",
		"                currentpoint pop NormY moveto",
		"            } if",
		"            CharCode ComStart eq {",			// print a "\"
		"                ( ) dup 0 CharCode put show",
		"            } if",
		"        }",
		"        {",
		"            CharCode ComStart eq {",
		"                /ComMode 1 def",				// enter command mode
		"            }",
		"            {",
		"                ( ) dup 0 CharCode put show",	// print char
		"            } ifelse",
		"        } ifelse",
		"    } forall ",
		"} def",

		"/StringLength {",								// str size StringLength
		"    /ComMode 0 def",							// command mode flag
		"    /StrLen 0 def",							// total string length
		"    /TSize exch def",							// text size
		"    /TString exch def",						// string to draw
		"    TSize scaleFont",
		"    TString {",								// scan string char by char
		"        /CharCode exch def",					// save char
		"        ComMode 1 eq {",
		"            /ComMode 0 def",					// command mode
		"            CharCode ComSub eq {",				// start subscript
		"                TSize SSSize mul scaleFont",
		"            } if",
		"            CharCode ComSup eq {",				// start superscript
		"                TSize SSSize mul scaleFont",
		"            } if",
		"            CharCode ComNorm eq {",			// end sub- or super-script
		"                TSize scaleFont",
		"            } if",
		"            CharCode ComStart eq {",			// add "\" to length
		"                ( ) dup 0 CharCode put stringwidth pop StrLen add",
		"                /StrLen exch def",
		"            } if",
		"        }",
		"        {",
		"            CharCode ComStart eq {",
		"                /ComMode 1 def",				// enter command mode
		"            }",
		"            {",								// add char to length
		"                ( ) dup 0 CharCode put stringwidth pop StrLen add",
		"                /StrLen exch def",
		"            } ifelse",
		"        } ifelse",
		"    } forall ",
		"    StrLen 0",									// return info like stringwidth
		"} def",

		"/Centerstring {",								// x y str sca
		"    dup /TSize exch def",						// save size
		"    dup scaleFont exch dup TSize StringLength", // x y sca str xw yw
		"    pop 3 -1 roll .5 mul",						// x y str xw sca*.5 (was .8)
		"    exch 5 -1 roll exch 2 div sub",			// y str sca*.5 x-xw/2
		"    exch 4 -1 roll exch 2 div sub",			// str x-xw/2 y-sca*.5/2
		"    moveto TSize StringShow",
		"} def",

		"/Topstring {",									// x y str sca
		"    dup /TSize exch def",						// save size
		"    dup scaleFont exch dup TSize StringLength", // x y sca str xw yw
		"    pop 3 -1 roll .5 mul",						// x y str xw sca*.5 (was .8)
		"    exch 5 -1 roll exch 2 div sub",			// y str sca*.5 x-xw/2
		"    exch 4 -1 roll exch sub",					// str x-xw/2 y-sca*.5
		"    moveto TSize StringShow",
		"} def",

		"/Botstring {",									// x y str sca
		"    dup /TSize exch def",						// save size
		"    scaleFont dup TSize StringLength pop",		// x y str xw
		"    4 -1 roll exch 2 div sub",					// y str x-xw/2
		"    3 -1 roll moveto TSize StringShow",
		"} def",

		"/Leftstring {",								// x y str sca
		"    dup /TSize exch def",						// save size
		"    dup scaleFont .4 mul",						// x y str sca*.4
		"    3 -1 roll exch sub",						// x str y-sca*.4
		"    3 -1 roll exch",							// str x y-sca*.4
		"    moveto TSize StringShow",
		"} def",

		"/Rightstring {",								// x y str sca
		"    dup /TSize exch def",						// save size
		"    dup scaleFont exch dup TSize StringLength", // x y sca str xw yw
		"    pop 3 -1 roll .4 mul",						// x y str xw sca*.4
		"    exch 5 -1 roll exch sub",					// y str sca*.4 x-xw
		"    exch 4 -1 roll exch sub",					// str x-xw y-sca*.4
		"    moveto TSize StringShow",
		"} def",

		"/Topleftstring {",								// x y str sca
		"    dup /TSize exch def",						// save size
		"    dup scaleFont .5 mul",						// x y str sca*.5 (was .8)
		"    3 -1 roll exch sub",						// x str y-sca*.5
		"    3 -1 roll exch",							// str x y-sca*.5
		"    moveto TSize StringShow",
		"} def",

		"/Toprightstring {",							// x y str sca
		"    dup /TSize exch def",						// save size
		"    dup scaleFont exch dup TSize StringLength", // x y sca str xw yw
		"    pop 3 -1 roll .5 mul",						// x y str xw sca*.5 (was .8)
		"    exch 5 -1 roll exch sub",					// y str sca*.5 x-xw
		"    exch 4 -1 roll exch sub",					// str x-xw y-sca*.5
		"    moveto TSize StringShow",
		"} def",

		"/Botleftstring {",								// x y str sca
		"    dup /TSize exch def",						// save size
		"    scaleFont 3 1 roll moveto TSize StringShow",
		"} def",

		"/Botrightstring {",							// x y str sca
		"    dup /TSize exch def",						// save size
		"    scaleFont dup TSize StringLength",
		"    pop 4 -1 roll exch",
		"    sub 3 -1 roll",
		"    moveto TSize StringShow",
		"} def",

		"/Min {",										// leave minimum of top two
		"    dup 3 -1 roll dup",
		"    3 1 roll gt",
		"    {exch} if pop",
		"} def",

		"/Boxstring {",									// x y mx my str sca
		"    dup /TSize exch def",						// save size
		"    dup scaleFont",							// x y mx my str sca
		"    exch dup TSize StringLength pop",			// x y mx my sca str xw
		"    3 -1 roll dup",							// x y mx my str xw sca sca
		"    6 -1 roll mul",							// x y my str xw sca sca*mx
		"    3 -1 roll div",							// x y my str sca sca*mx/xw
		"    4 -1 roll",								// x y str sca sca*mx/xw my
		"    Min Min",									// x y str minsca
		"    Centerstring",
		"} def"
	};

	private void putPSHeader(int which)
	{
		switch (which)
		{
			case HEADERDOT:
				if (putHeaderDot) return;
				putHeaderDot = true;
				for(int i=0; i<headerDot.length; i++)
					printWriter.println(headerDot[i]);
				break;
			case HEADERLINE:
				if (putHeaderLine) return;
				putHeaderLine = true;
				for(int i=0; i<headerLine.length; i++)
					printWriter.println(headerLine[i]);
				break;
			case HEADERPOLYGON:
				if (putHeaderPolygon) return;
				putHeaderPolygon = true;
				for(int i=0; i<headerPolygon.length; i++)
					printWriter.println(headerPolygon[i]);
				break;
			case HEADERFPOLYGON:
				if (putHeaderFilledPolygon) return;
				putHeaderFilledPolygon = true;
				for(int i=0; i<headerFilledPolygon.length; i++)
					printWriter.println(headerFilledPolygon[i]);
				break;
			case HEADERSTRING:
				if (putHeaderString) return;
				putHeaderString = true;
				for(int i=0; i<headerString.length; i++)
					printWriter.println(headerString[i]);
				break;
		}
	}

	private char psPattern(EGraphics col)
	{
		// see if this graphics has been seen already
		Integer index = (Integer)patternsEmitted.get(col);
		if (index != null) return (char)('A' + index.intValue());

		// add to list
		patternsEmitted.put(col, new Integer(psNumPatternsEmitted));
		int [] raster = col.getPattern();
		char indexChar = (char)(psNumPatternsEmitted+'A');
		psNumPatternsEmitted++;

		/*
		 * Generate filled polygons by defining a stipple font,
		 * and then tiling the polygon to fill with 128x128 pixel
		 * characters, clipping to the polygon edge.
		 *
		 * Take Electric's 16x8 bit images, double each bit,
		 * and then output 4 times to get 128 bit wide image.
		 * Double vertically by outputting each row twice.
		 * Note that full vertical size need not be generated,
		 * as PostScript will just reuse the lines until the 128
		 * size is reached.
		 *
		 * see: "Making a User Defined Font", PostScript Cookbook
		 */
		printWriter.println("StippleFont1 begin");
		printWriter.println("    Encoding (" + indexChar + ") 0 get /Stipple" + indexChar + " put");
		printWriter.println("    CharacterDefs /Stipple" + indexChar + " {");
		printWriter.println("        128 128 true [128 0 0 -128 0 128]");
		printWriter.println("        { <");
		for(int i=0; i<8; i++)
		{
			int bl = raster[i] & 0x00FF;
			int bh = (raster[i] & 0xFF00) >> 8;
			int bld = 0, bhd = 0;
			for (int k=0; k<8; ++k)
			{
				bld = (bld << 1);
				bld |= (bl & 0x1);
				bld = (bld << 1);
				bld |= (bl & 0x1);
				bl = (bl >> 1);
				bhd = (bhd << 1);
				bhd |= (bh & 0x1);
				bhd = (bhd << 1);
				bhd |= (bh & 0x1);
				bh = (bh >> 1);
			}
			for (int k=0; k<2; k++)
			{
				printWriter.print("            ");
				for(int j=0; j<4; j++)
					printWriter.print((bhd&0xFFFF) + " " + (bld&0xFFFF) + " ");
				printWriter.println();
			}
		}
		printWriter.println("        > } imagemask");
		printWriter.println("    } put");
		printWriter.println("end");
		return indexChar;
	}


	/**
	 * Method to convert the coordinates (x,y) for display.  The coordinates for
	 * printing are placed back into (x,y) and the PostScript coordinates are placed
	 * in (psx,psy).
	 */
	private Point2D psXform(Point2D pt)
	{
		Point2D result = new Point2D.Double();
		matrix.transform(pt, result);
		return result;
	}

	public void writePSString(String str)
	{
		printWriter.print("(");
		for(int i=0; i<str.length(); i++)
		{
			char ca = str.charAt(i);
			if (ca == '(' || ca == ')' || ca == '\\') printWriter.print("\\");
			printWriter.print(ca);
		}
		printWriter.print(")");
	}
}
