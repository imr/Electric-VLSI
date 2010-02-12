/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SVG.java
 * Input/output tool: Scalable Vector Graphics output
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.GraphicsPreferences;
import com.sun.electric.tool.user.ui.LayerVisibility;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This class writes files in SVG format.
 *
 * Things to finish:
 *    grid
 *    text underlining
 *    boxed text
 */
public class SVG extends Output
{
	/** scale factor for SVG text */				private static final double SVGTEXTSCALE    =  0.75;

	/** the Cell being written. */										private Cell cell;
	/** current layer number (-1: do all; 0: cleanup). */				private int currentLayer;
	/** matrix from database units to SVG units. */						private AffineTransform matrix;
	/** fake graphics for drawing outlines and text. */					private static EGraphics blackGraphics =
        new EGraphics(false, false, null, 0, 100,100,100,1.0,true, new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
	private SVGPreferences localPrefs;

	public static class SVGPreferences extends OutputPreferences
    {
		boolean printForPlotter = IOTool.isFactoryPrintForPlotter();
		double pageWidth = IOTool.getFactoryPrintWidth();
		double pageHeight = IOTool.getFactoryPrintHeight();
		double printMargin = IOTool.getFactoryPrintMargin();
		int printRotation = IOTool.getFactoryPrintRotation();
        GraphicsPreferences gp;
        EditWindow0.EditWindowSmall wnd;
    	ERectangle printBounds;
        Set<Layer> invisibleLayers = new HashSet<Layer>();
        boolean isGrid = false;
        double gridXSpacing, gridYSpacing;

		SVGPreferences(boolean factory)
		{
            super(factory);

            gp = new GraphicsPreferences(factory);
            LayerVisibility lv = new LayerVisibility(factory);
            for (Technology tech: TechPool.getThreadTechPool().values()) {
                for (Iterator<Layer> it = tech.getLayers(); it.hasNext(); ) {
                    Layer layer = it.next();
                    if (!lv.isVisible(layer))
                        invisibleLayers.add(layer);
                }
            }
            if (!factory)
                fillPrefs();
		}

		private void fillPrefs()
        {
			printForPlotter = IOTool.isPrintForPlotter();
			pageWidth = IOTool.getPrintWidth();
			pageHeight = IOTool.getPrintHeight();
			printMargin = IOTool.getPrintMargin();
			printRotation = IOTool.getPrintRotation();
			UserInterface ui = Job.getUserInterface();
			EditWindow_ localWnd = ui.getCurrentEditWindow_();
			wnd = new EditWindow0.EditWindowSmall(localWnd);
	        isGrid = localWnd.isGrid();
	        gridXSpacing = localWnd.getGridXSpacing();
	        gridYSpacing = localWnd.getGridYSpacing();

	        // determine the area of interest
			Cell cell = localWnd.getCell();
			printBounds = ERectangle.fromLambda(getAreaToPrint(cell, false, localWnd));
		}

        public Output doOutput(Cell cell, VarContext context, String filePath)
        {
        	SVG out = new SVG(this, cell);
    		out.writeCellToFile(filePath);
            return out.finishWrite();
        }
    }

	/**
	 * SVG constructor.
	 */
	private SVG(SVGPreferences pp, Cell cell)
	{
		localPrefs = pp;
        this.cell = cell;
	}

	/**
	 * Internal method for SVG output.
	 * @param filePath the disk file to create.
	 */
	private boolean writeCellToFile(String filePath)
	{
		if (localPrefs.printBounds == null) return true;
		boolean error = false;
		if (openTextOutputStream(filePath)) error = true; else
		{
			// write out the cell
			if (start())
			{
				scanCircuit();
				done();
			}

			if (closeTextOutputStream()) error = true;
		}
		if (!error)
		{
			System.out.println(filePath + " written");
		}
		return error;
	}

	/**
	 * Method to initialize for writing a cell.
	 * @return false to abort the process.
	 */
	private boolean start()
	{
		// get control options
		double pageWid = localPrefs.pageWidth * 75;
		double pageHei = localPrefs.pageHeight * 75;
		double pageMarginSVG = localPrefs.printMargin * 75;
		double pageMargin = pageMarginSVG;		// not right!!!

		boolean rotatePlot = false;
		switch (localPrefs.printRotation)
		{
			case 1:		// rotate 90 degrees
				rotatePlot = true;
				break;
			case 2:		// auto-rotate
				if (((pageHei > pageWid || localPrefs.printForPlotter) &&
					localPrefs.printBounds.getWidth() > localPrefs.printBounds.getHeight()) ||
					(pageWid > pageHei && localPrefs.printBounds.getHeight() > localPrefs.printBounds.getWidth()))
						rotatePlot = true;
				break;
		}

		// if plotting, compute height from width
		if (localPrefs.printForPlotter)
		{
			if (rotatePlot)
			{
				pageHei = pageWid * localPrefs.printBounds.getWidth() / localPrefs.printBounds.getHeight();
			} else
			{
				pageHei = pageWid * localPrefs.printBounds.getHeight() / localPrefs.printBounds.getWidth();
			}
		}

		// compute the transformation matrix
		double cX = localPrefs.printBounds.getCenterX();
		double cY = localPrefs.printBounds.getCenterY();
		double unitsX = pageWid - pageMargin*2;
		double unitsY = pageHei - pageMargin*2;

		double i, j;
		if (localPrefs.printForPlotter)
		{
			i = unitsX / localPrefs.printBounds.getWidth();
			j = unitsX / localPrefs.printBounds.getHeight();
		} else
		{
			i = Math.min(unitsX / localPrefs.printBounds.getWidth(), unitsY / localPrefs.printBounds.getHeight());
			j = Math.min(unitsX / localPrefs.printBounds.getHeight(), unitsY / localPrefs.printBounds.getWidth());
		}
		if (rotatePlot) i = j;
		double matrix00 = i;   double matrix01 = 0;
		double matrix10 = 0;   double matrix11 = -i;
		double matrix20 = - i * cX + unitsX / 2 + pageMarginSVG;
		double matrix21;
		if (localPrefs.printForPlotter)
		{
			matrix21 = i * localPrefs.printBounds.getMinY() - pageMarginSVG;
		} else
		{
			matrix21 = i * cY + unitsY / 2 - pageMarginSVG;
		}
		matrix = new AffineTransform(matrix00, matrix01, matrix10, matrix11, matrix20, matrix21);

		// write SVG header		
		printWriter.println("<?xml version=\"1.0\" standalone=\"no\"?>");
		printWriter.println("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
		printWriter.println();
		printWriter.println("<!-- Title: " + cell.describe(false) + "-->");
		if (localPrefs.includeDateAndVersionInOutput)
		{
			printWriter.println("<!-- Creator: Electric VLSI Design System version " + Version.getVersion() + "-->");
			Date now = new Date();
			printWriter.println("<!-- Date: " + TextUtils.formatDate(now) + "-->");
		} else
		{
			printWriter.println("<!-- Creator: Electric VLSI Design System -->");
		}
		emitCopyright("<!-- ", "-->");
		printWriter.println("<svg width=\"100%\" height=\"100%\" version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\">");
		printWriter.println();
		return true;
	}

	/**
	 * Method to clean-up writing a cell.
	 */
	private void done()
	{
		// draw the grid if requested
//		printWriter.println("0 0 0 setrgbcolor");
//		if (localPrefs.wnd != null && localPrefs.isGrid)
//		{
//			int gridx = (int)localPrefs.gridXSpacing;
//			int gridy = (int)localPrefs.gridYSpacing;
//			int lx = (int)cell.getBounds().getMinX();
//			int ly = (int)cell.getBounds().getMinY();
//			int hx = (int)cell.getBounds().getMaxX();
//			int hy = (int)cell.getBounds().getMaxY();
//			int gridlx = lx / gridx * gridx;
//			int gridly = ly / gridy * gridy;
//
//			// adjust to ensure that the first point is inside the range
//			if (gridlx > lx) gridlx -= (gridlx - lx) / gridx * gridx;
//			if (gridly > ly) gridly -= (gridly - ly) / gridy * gridy;
//			while (gridlx < lx) gridlx += gridx;
//			while (gridly < ly) gridly += gridy;
//
//			// SVG: write the grid loop
//			double matrix00 = matrix.getScaleX();
//			double matrix01 = matrix.getShearX();
//			double matrix10 = matrix.getShearY();
//			double matrix11 = matrix.getScaleY();
//			double matrix20 = matrix.getTranslateX();
//			double matrix21 = matrix.getTranslateY();
//			printWriter.println(gridlx + " " + gridx + " " + hx);
//			printWriter.println("{");
//			printWriter.println("    " + gridly + " " + gridy + " " + hy);	// x y
//			printWriter.println("    {");
//			printWriter.println("        dup 3 -1 roll dup dup");				// y y x x x
//			printWriter.println("        5 1 roll 3 1 roll");					// x y x y x
//			printWriter.println("        " + matrix00 + " mul exch " + matrix10 + " mul add " + matrix20 + " add");		// x y x x'
//			printWriter.println("        3 1 roll");							// x x' y x
//			printWriter.println("        " + matrix01 + " mul exch " + matrix11 + " mul add " + matrix21 + " add");		// x x' y'
//			printWriter.println("        newpath moveto 0 0 rlineto stroke");
//			printWriter.println("    } for");
//			printWriter.println("} for");
//		}

		// draw frame if it is there
		SVGFrame pf = new SVGFrame(cell, this);
		pf.renderFrame();

		printWriter.println();
		printWriter.println("</svg>");
	}

	/**
	 * Class for rendering a cell frame to the SVG.
	 * Extends Cell.FrameDescription and provides hooks for drawing to a Graphics.
	 */
	private static class SVGFrame extends Cell.FrameDescription
	{
		private SVG writer;

		/**
		 * Constructor for cell frame rendering.
		 * @param cell the Cell that is having a frame drawn.
		 * @param writer the SVG object for access to field variables.
		 */
		public SVGFrame(Cell cell, SVG writer)
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
			writer.svgLine(from, to, Color.BLACK, 0);
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
			writer.svgText(poly, Color.BLACK);
		}
	}

	/****************************** TRAVERSING THE HIERARCHY ******************************/

	/**
	 * Method to write the body of the SVG.
	 */
	private void scanCircuit()
	{
		// figure out the size of the job for progress display (and gather patterns)
		Job.getUserInterface().startProgressDialog("Writing SVG", null);
		Job.getUserInterface().setProgressNote("Counting SVG objects...");
		printWriter.println("<defs>");
		long totalObjects = recurseCircuitLevel(cell, DBMath.MATID, true, false, 0);
		printWriter.println("</defs>");

		// color: plot layers in proper order
		List<Layer> layerList = cell.getTechnology().getLayersSortedByHeight();
		List<Layer> contactLayers = new ArrayList<Layer>();
		List<Layer> regularLayers = new ArrayList<Layer>();
		for(Layer layer : layerList)
		{
			if (layer.getFunction().isContact()) contactLayers.add(layer); else
				regularLayers.add(layer);
		}
		for(Layer layer : contactLayers) regularLayers.add(layer);
		for(Layer layer : regularLayers)
		{
			if (localPrefs.invisibleLayers.contains(layer)) continue;
			Job.getUserInterface().setProgressNote("Writing layer " + layer.getName() + " (" + totalObjects + " objects...");

           	EGraphics gra = localPrefs.gp.getGraphics(layer);
           	Color col = gra.getColor();
           	boolean opaque = gra.getTransparentLayer() == 0;
			String patternName = null;
			if (gra.isPatternedOnDisplay()) patternName = layer.getName();

			printWriter.print("<g id=\"Layer-" + layer.getName() + "\"");
			if (patternName != null) printWriter.print(" fill=\"url(#" + patternName + ")\""); else
			{
				printWriter.print(" fill=\"" + getColorDescription(col) + "\"");
				if (!opaque) printWriter.print(" opacity=\"0.5\"");
			}
			printWriter.println(">");

			currentLayer = layer.getIndex() + 1;
			recurseCircuitLevel(cell, DBMath.MATID, true, true, totalObjects);
			printWriter.println("</g>");
		}
		currentLayer = 0;
		Job.getUserInterface().setProgressNote("Writing cell information (" + totalObjects + " objects...");
		recurseCircuitLevel(cell, DBMath.MATID, true, true, totalObjects);
		Job.getUserInterface().stopProgressDialog();
	}

	/**
	 * Method to recursively write a Cell to the SVG file.
	 * @param cell the Cell to write.
	 * @param trans the transformation matrix from the Cell to the top level.
	 * @param topLevel true if this is the top level.
	 * @param real true to really write SVG (false when counting layers).
	 * @param progressTotal nonzero to display progress (in which case, this is the total).
	 * @return the number of objects processed.
	 */
	private int recurseCircuitLevel(Cell cell, AffineTransform trans, boolean topLevel, boolean real, long progressTotal)
	{
		int numObjects = 0;

		// write the nodes
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			AffineTransform subRot = ni.rotateOut();
			subRot.preConcatenate(trans);

			if (!ni.isCellInstance())
			{
				if (!topLevel)
				{
					if (ni.isVisInside()) continue;
					if (ni.getProto() == Generic.tech().essentialBoundsNode ||
						ni.getProto() == Generic.tech().cellCenterNode) continue;
				}
				PrimitiveNode prim = (PrimitiveNode)ni.getProto();
				Technology tech = prim.getTechnology();
				Poly [] polys = tech.getShapeOfNode(ni);
				for (int i=0; i<polys.length; i++)
				{
					polys[i].transform(subRot);
					svgPoly(polys[i], real);
				}
				numObjects++;
				if (progressTotal != 0 && (numObjects%100) == 0)
				{
                    long pct = numObjects*100/progressTotal;
					Job.getUserInterface().setProgressValue((int)pct);
                }
			} else
			{
				// a cell instance
				Cell subCell = (Cell)ni.getProto();
				AffineTransform subTrans = ni.translateOut();
				subTrans.preConcatenate(subRot);
				if (!ni.isExpanded())
				{
					Rectangle2D bounds = subCell.getBounds();
					Poly poly = new Poly(bounds.getCenterX(), bounds.getCenterY(), ni.getXSize(), ni.getYSize());
					poly.transform(subTrans);
					poly.setStyle(Poly.Type.CLOSED);
                    poly.setLayer(null);
                    poly.setGraphicsOverride(blackGraphics);
					svgPoly(poly, real);

					// Only when the instance names flag is on
					if (real)
					{
						if (localPrefs.gp.isTextVisibilityOn(TextDescriptor.TextType.INSTANCE))
						{
							poly.setStyle(Poly.Type.TEXTBOX);
							TextDescriptor td = TextDescriptor.getInstanceTextDescriptor().withAbsSize(24);
							poly.setTextDescriptor(td);
							poly.setString(ni.getProto().describe(false));
							svgPoly(poly, true);
						}
						if (topLevel) showCellPorts(ni, trans, null);
					}
					numObjects++;
					if (progressTotal != 0 && (numObjects%100) == 0) {
                        long pct = numObjects*100/progressTotal;
        				Job.getUserInterface().setProgressValue((int)pct);
                    }
				} else
				{
					recurseCircuitLevel(subCell, subTrans, false, real, progressTotal);
					if (topLevel && real) showCellPorts(ni, trans, Color.BLACK);
				}
			}

			// draw any displayable variables on the node
			if (real && localPrefs.gp.isTextVisibilityOn(TextDescriptor.TextType.NODE))
			{
				Poly [] textPolys = ni.getDisplayableVariables(localPrefs.wnd);
				for (int i=0; i<textPolys.length; i++)
				{
					textPolys[i].transform(subRot);
					svgPoly(textPolys[i], true);
				}
			}

			// draw any exports from the node
			if (topLevel && localPrefs.gp.isTextVisibilityOn(TextDescriptor.TextType.EXPORT))
			{
				for(Iterator<Export> eIt = ni.getExports(); eIt.hasNext(); )
				{
					Export e = eIt.next();
					if (real)
					{
						Poly poly = e.getNamePoly();
						if (localPrefs.gp.exportDisplayLevel == 2)
						{
							// draw port as a cross
							drawCross(poly.getCenterX(), poly.getCenterY(), Color.BLACK, false);
						} else
						{
							// draw port as text
							if (localPrefs.gp.exportDisplayLevel == 1)
							{
								// use shorter port name
								String portName = e.getShortName();
								poly.setString(portName);
							}

							// rotate the descriptor
							TextDescriptor descript = poly.getTextDescriptor();
							Poly.Type style = descript.getPos().getPolyType();
							style = Poly.rotateType(style, ni);
							poly.setStyle(style);
							svgPoly(poly, true);
						}

						// draw variables on the export
						Rectangle2D rect = (Rectangle2D)poly.getBounds2D().clone();
						Poly[] polys = e.getDisplayableVariables(rect, localPrefs.wnd, true);
						for (int i=0; i<polys.length; i++)
						{
							svgPoly(polys[i], true);
						}
					}
					numObjects++;
					if (progressTotal != 0 && (numObjects%100) == 0) {
                        long pct = numObjects*100/progressTotal;
        				Job.getUserInterface().setProgressValue((int)pct);
                    }
				}
			}
		}

		// write the arcs
		for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext();)
		{
			ArcInst ai = it.next();
			Technology tech = ai.getProto().getTechnology();
			Poly[] polys = tech.getShapeOfArc(ai);
			for (int i=0; i<polys.length; i++)
			{
				polys[i].transform(trans);
				svgPoly(polys[i], real);
			}
			if (real)
			{
				// draw any displayable variables on the arc
				if (topLevel && localPrefs.gp.isTextVisibilityOn(TextDescriptor.TextType.ARC))
				{
					Poly[] textPolys = ai.getDisplayableVariables(localPrefs.wnd);
					for (int i=0; i<textPolys.length; i++)
					{
						textPolys[i].transform(trans);
						svgPoly(textPolys[i], true);
					}
				}
			}

			numObjects++;
			if (progressTotal != 0 && (numObjects%100) == 0) {
                long pct = numObjects*100/progressTotal;
				Job.getUserInterface().setProgressValue((int)pct);
            }
		}

		// show cell variables if at the top level
		if (topLevel && real && localPrefs.gp.isTextVisibilityOn(TextDescriptor.TextType.CELL))
		{
			// show displayable variables on the instance
			Rectangle2D CENTERRECT = new Rectangle2D.Double(0, 0, 0, 0);
			Poly[] polys = cell.getDisplayableVariables(CENTERRECT, localPrefs.wnd, true);
			for (int i=0; i<polys.length; i++)
				svgPoly(polys[i], true);
		}
		return numObjects;
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
		for(int i = 0; i < numPorts; i++)
		{
			if (shownPorts[i]) continue;
			Export pp = (Export)ni.getProto().getPort(i);

			Poly portPoly = ni.getShapeOfPort(pp);
			if (portPoly == null) continue;
			portPoly.transform(trans);
			Color portColor = col;
			if (portColor == null) portColor = pp.getBasePort().getPortColor(localPrefs.gp);
			if (localPrefs.gp.portDisplayLevel == 2)
			{
				// draw port as a cross
				drawCross(portPoly.getCenterX(), portPoly.getCenterY(), portColor, false);
			} else
			{
				// draw port as text
				if (localPrefs.gp.isTextVisibilityOn(TextDescriptor.TextType.PORT))
				{
					// combine all features of port text with color of the port
					TextDescriptor descript = portPoly.getTextDescriptor();
                    if (descript == null)
                        descript = TextDescriptor.TextType.EXPORT.getFactoryTextDescriptor();//TextDescriptor.EMPTY;
                    TextDescriptor portDescript = pp.getTextDescriptor(Export.EXPORT_NAME).withColorIndex(descript.getColorIndex());
					Poly.Type type = descript.getPos().getPolyType();
					portPoly.setStyle(type);
					String portName = pp.getName();
					if (localPrefs.gp.portDisplayLevel == 1)
					{
						// use shorter port name
						portName = pp.getShortName();
					}
					portPoly.setString(portName);
					portPoly.setTextDescriptor(portDescript);
					svgText(portPoly, portColor);
				}
			}
		}
	}

	/****************************** SVG OUTPUT METHODS ******************************/

	/**
	 * Method to plot a polygon.
	 * @param poly the polygon to plot.
	 */
	private void svgPoly(PolyBase poly, boolean real)
	{
		// ignore null layers
		Layer layer = poly.getLayer();
		EGraphics gra = null;
		int index = 0;
		Technology tech = cell.getTechnology();
		if (layer != null)
		{
			tech = layer.getTechnology();
			index = layer.getIndex();
			if (localPrefs.invisibleLayers.contains(layer)) return;
			if (!real || currentLayer == 0) gra = localPrefs.gp.getGraphics(layer);
            if (poly instanceof Poly)
            {
            	EGraphics overGra = ((Poly)poly).getGraphicsOverride();
            	if (overGra != null) gra = overGra;
            }
		}
		Color col = null;
		boolean opaque = true;
		String patternName = null;
        if (gra != null)
        {
			col = gra.getColor();
			opaque = gra.getTransparentLayer() == 0;
			if (gra.isPatternedOnDisplay()) patternName = layer.getName();
        }

		// use solid color if solid pattern or no pattern
		if (patternName != null && !real)
		{
			int [] pattern = gra.getPattern();
			boolean sol = true;
			for(int i=0; i<8; i++)
				if (pattern[i] != 0xFFFF) { sol = false;   break; }
			if (sol) patternName = null; else
			{
				printWriter.println("  <pattern id=\"" + patternName + "\" x=\"0\" y=\"0\" width=\"16\" height=\"8\" patternUnits=\"userSpaceOnUse\">");
				for(int i=0; i<8; i++)
				{
					for (int k=0; k<16; k++)
					{
						if (((pattern[i]>>k)&1) != 0)
						{
							printWriter.println("    <rect x=\"" + (15-k) + "\" y=\"" + i + "\" width=\"1\" height=\"1\" fill=\"" + getColorDescription(col) + "\"/>");
						}
					}
				}
				printWriter.println("  </pattern>");
			}
		}

		// ignore layers that are not supposed to be dumped at this time
		if (currentLayer >= 0)
		{
			if (currentLayer == 0)
			{
				if (tech == cell.getTechnology()) return;
			} else
			{
				if (tech != cell.getTechnology() || currentLayer-1 != index)
					return;
			}
		}

		Poly.Type type = poly.getStyle();
		Point2D [] points = poly.getPoints();
		if (type == Poly.Type.FILLED)
		{
			Rectangle2D polyBox = poly.getBox();
			if (polyBox != null)
			{
				if (polyBox.getWidth() == 0)
				{
					if (!real) return;
					if (polyBox.getHeight() == 0)
					{
						svgDot(new Point2D.Double(polyBox.getCenterX(), polyBox.getCenterY()), col, opaque, patternName);
					} else
					{
						svgLine(new Point2D.Double(polyBox.getCenterX(), polyBox.getMinY()),
							new Point2D.Double(polyBox.getCenterX(), polyBox.getMaxY()), col, 0);
					}
					return;
				} else if (polyBox.getHeight() == 0)
				{
					if (real) svgLine(new Point2D.Double(polyBox.getMinX(), polyBox.getCenterY()),
						new Point2D.Double(polyBox.getMaxX(), polyBox.getCenterY()), col, 0);
					return;
				}
				svgBox(polyBox, col, opaque, patternName);
				return;
			}
			if (points.length == 1)
			{
				if (real) svgDot(points[0], col, opaque, patternName);
				return;
			}
			if (points.length == 2)
			{
				if (real) svgLine(points[0], points[1], col, 0);
				return;
			}
			svgPolygon(poly, col, opaque, patternName);
			return;
		}
		if (!real) return;
		if (type == Poly.Type.CLOSED)
		{
			Point2D lastPt = points[points.length-1];
			for (int k = 0; k < points.length; k++)
			{
				svgLine(lastPt, points[k], col, 0);
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
				svgLine(points[k-1], points[k], col, lineType);
			return;
		}
		if (type == Poly.Type.VECTORS)
		{
			for(int k=0; k<points.length; k += 2)
				svgLine(points[k], points[k+1], col, 0);
			return;
		}
		if (type == Poly.Type.CROSS || type == Poly.Type.BIGCROSS)
		{
			double x = poly.getCenterX();
			double y = poly.getCenterY();
			drawCross(x, y, col, type == Poly.Type.BIGCROSS);
			return;
		}
		if (type == Poly.Type.CROSSED)
		{
			Rectangle2D bounds = poly.getBounds2D();
			svgLine(new Point2D.Double(bounds.getMinX(), bounds.getMinY()), new Point2D.Double(bounds.getMinX(), bounds.getMaxY()), col, 0);
			svgLine(new Point2D.Double(bounds.getMinX(), bounds.getMaxY()), new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()), col, 0);
			svgLine(new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()), new Point2D.Double(bounds.getMaxX(), bounds.getMinY()), col, 0);
			svgLine(new Point2D.Double(bounds.getMaxX(), bounds.getMinY()), new Point2D.Double(bounds.getMinX(), bounds.getMinY()), col, 0);
			svgLine(new Point2D.Double(bounds.getMinX(), bounds.getMinY()), new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()), col, 0);
			svgLine(new Point2D.Double(bounds.getMinX(), bounds.getMaxY()), new Point2D.Double(bounds.getMaxX(), bounds.getMinY()), col, 0);
			return;
		}
		if (type == Poly.Type.DISC)
		{
			svgDisc(points[0], points[1], col, opaque, patternName);
			return;
		}
		if (type == Poly.Type.CIRCLE || type == Poly.Type.THICKCIRCLE)
		{
			svgCircle(points[0], points[1], col, opaque, patternName);
			return;
		}
		if (type == Poly.Type.CIRCLEARC || type == Poly.Type.THICKCIRCLEARC)
		{
			svgArc(points[0], points[1], points[2], col);
			return;
		}

		// text
		svgText((Poly)poly, col== null ? Color.black : col);
	}

	/**
	 * Method to draw a cross.
	 * @param x X center of the cross.
	 * @param y Y Center of the cross.
	 * @param bigCross true for a big cross, false for a small one
	 */
	private void drawCross(double x, double y, Color col, boolean bigCross)
	{
		double amount = 0.25;
		if (bigCross) amount = 0.5;
		svgLine(new Point2D.Double(x-amount, y), new Point2D.Double(x+amount, y), col, 0);
		svgLine(new Point2D.Double(x, y+amount), new Point2D.Double(x, y-amount), col, 0);
	}

	/**
	 * Method to draw a dot
	 * @param pt the center of the dot.
	 */
	private void svgDot(Point2D pt, Color col, boolean opaque, String patternName)
	{
		Point2D ps = svgXform(pt);
		String style = getStyleDescription(col, opaque, patternName);
		printWriter.println("<rect x=\"" + TextUtils.formatDouble(ps.getX()) + "\" y=\"" + TextUtils.formatDouble(ps.getY()) +
			"\" width=\"1\" height=\"1\" " + style + "/>");
	}

	/**
	 * Method to draw a line.
	 * @param from the starting point of the line.
	 * @param to the ending point of the line.
	 * @param pattern the line texture (0 for solid, positive for dot/dash patterns).
	 */
	private void svgLine(Point2D from, Point2D to, Color col, int pattern)
	{
		Point2D pt1 = svgXform(from);
		Point2D pt2 = svgXform(to);
		printWriter.print("<line x1=\"" + pt1.getX() + "\" y1=\"" + pt1.getY() +
			"\" x2=\"" + pt2.getX() + "\" y2=\"" + pt2.getY() + "\"");
		switch (pattern)
		{
			case 0:
				printWriter.print(" style=\"stroke:" + getColorDescription(col) + "\"");
				break;
			case 1:		// OPENEDT1 (dotted)
				printWriter.print(" style=\"stroke-dasharray:1,5;stroke:" + getColorDescription(col) + "\"");
				break;
			case 2:		// OPENEDT2 (dashed)
				printWriter.print(" style=\"stroke-dasharray:8,4;stroke:" + getColorDescription(col) + "\"");
				break;
			case 3:		// OPENEDT3 (thick)
				printWriter.print(" style=\"stroke-width:3;stroke:" + getColorDescription(col) + "\"");
				break;
		}
		printWriter.println("/>");
	}

	/**
	 * Method to draw an arc of a circle.
	 * @param center the center of the arc's circle.
	 * @param pt1 the starting point of the arc.
	 * @param pt2 the ending point of the arc.
	 */
	private void svgArc(Point2D center, Point2D pt1, Point2D pt2, Color col)
	{
		// Emit: A<sx,sy> <r1,r2> <a> <l>,<p> <ex,ey>
		// <sx,sy> is starting point of arc
		// <r1,r2> are the two radii
		// <a> is angle of starting point from center
		// <l> is 1 if arc is more than 180 degrees, 0 otherwise
		// <p> is 1 if arc goes in positive angle, 0 otherwise
		// <ex,ey> is ending point of arc

		Point2D pc = svgXform(center);
		Point2D ps1 = svgXform(pt1);
		Point2D ps2 = svgXform(pt2);
		double radius = pc.distance(ps1);
		double startAngle = ((3600 - DBMath.figureAngle(pc, ps2)) % 3600) / 10;
		double endAngle = ((3600 - DBMath.figureAngle(pc, ps1)) % 3600) / 10;
		double angleDiff = endAngle - startAngle;
		if (angleDiff < 0) angleDiff += 360;
		int largeAngle = angleDiff >= 180 ? 1 : 0;
		int positive = 1;
		printWriter.println("<path d=\"M " + ps1.getX() + "," + ps1.getY() + " A" + radius + "," + radius + " " + startAngle +
			" " + largeAngle + "," + positive + " " + ps2.getX() + "," + ps2.getY() + "\" fill=\"none\" stroke=\"" +
			getColorDescription(col) + "\" />");
	}

	/**
	 * Method to draw an unfilled circle.
	 * @param center the center of the circle.
	 * @param pt a point on the circle.
	 */
	private void svgCircle(Point2D center, Point2D pt, Color col, boolean opaque, String patternName)
	{
		Point2D pc = svgXform(center);
		Point2D ps = svgXform(pt);
		double radius = pc.distance(ps);
		String style = "style=\"stroke:" + getColorDescription(col) + (opaque ? "" : ";opacity:0.5") + ";fill:none\"";
		printWriter.println("<circle cx=\"" + TextUtils.formatDouble(pc.getX()) + "\" cy=\"" + TextUtils.formatDouble(pc.getY()) +
			"\" r=\"" + radius + "\" " + style + "/>");
	}

	/**
	 * Method to draw a filled circle.
	 * @param center the center of the circle.
	 * @param pt a point on the circle.
	 */
	private void svgDisc(Point2D center, Point2D pt, Color col, boolean opaque, String patternName)
	{
		Point2D pc = svgXform(center);
		Point2D ps = svgXform(pt);
		double radius = pc.distance(ps);
		String style = getStyleDescription(col, opaque, patternName);
		printWriter.println("<circle cx=\"" + TextUtils.formatDouble(pc.getX()) + "\" cy=\"" + TextUtils.formatDouble(pc.getY()) +
			"\" r=\"" + radius + "\" " + style + "/>");
	}

	/**
	 * Method to draw a rectangle
	 * @param polyBox the rectangle to draw.
	 */
	private void svgBox(Rectangle2D polyBox, Color col, boolean opaque, String patternName)
	{
		Point2D pLow = svgXform(new Point2D.Double(polyBox.getMinX(), polyBox.getMaxY()));
		Point2D pHigh = svgXform(new Point2D.Double(polyBox.getMaxX(), polyBox.getMinY()));
		String style = getStyleDescription(col, opaque, patternName);
		printWriter.println("<rect x=\"" + pLow.getX() + "\" y=\"" + pLow.getY() +
			"\" width=\"" + (pHigh.getX()-pLow.getX()) + "\" height=\"" + (pHigh.getY()-pLow.getY()) +
			"\" " + style + "/>");
	}

	/**
	 * Method to draw an irregular polygon.
	 * @param poly the polygon to draw.
	 */
	private void svgPolygon(PolyBase poly, Color col, boolean opaque, String patternName)
	{
		Point2D [] points = poly.getPoints();
		if (points.length == 0) return;

		printWriter.print("<polygon points=\"");
		for(int i=0; i<points.length; i++)
		{
			if (i != 0) printWriter.print(" ");
			Point2D ps = svgXform(points[i]);
			printWriter.print(TextUtils.formatDouble(ps.getX()) + "," + TextUtils.formatDouble(ps.getY()));
		}
		printWriter.println("\" " + getStyleDescription(col, opaque, patternName) + "/>");
	}

	private String getStyleDescription(Color col, boolean opaque, String patternName)
	{
		if (patternName != null) return "fill=\"url(#" + patternName + ")\"";
		if (col == null) return "";
		String style = "style=\"fill:" + getColorDescription(col) + (opaque ? "" : ";opacity:0.5") + "\"";
		return style;
	}

	private String getColorDescription(Color col)
	{
		if (col == null) return "";
		String style = "rgb(" + col.getRed() + "," + col.getGreen() + "," + col.getBlue() + ")";
		return style;
	}

	/**
	 * Method to draw text.
	 * @param poly the text polygon to draw.
	 */
	private void svgText(Poly poly, Color col)
	{
		Poly.Type style = poly.getStyle();
		TextDescriptor td = poly.getTextDescriptor();
		if (td == null) return;
		int size = (int)(td.getTrueSize(localPrefs.wnd) * SVGTEXTSCALE);
		Rectangle2D bounds = poly.getBounds2D();

		// get the font size
		if (size <= 0) return;

		// make sure the string is valid
		String text = poly.getString().trim();
		if (text.length() == 0) return;

		// TODO: finish boxed text
		if (poly.getStyle() == Poly.Type.TEXTBOX)
		{
			// treat like centered text
			style = Poly.Type.TEXTCENT;
		}

		Point2D p;
		String styleMsg = null;
		double x, y;
		if (style == Poly.Type.TEXTCENT)
		{
			p = svgXform(new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()));
			x = p.getX();   y = p.getY()+size/2;
			styleMsg = "text-anchor:middle";
		} else if (style == Poly.Type.TEXTTOP)
		{
			p = svgXform(new Point2D.Double(bounds.getCenterX(), bounds.getMinY()));
			x = p.getX();   y = p.getY()+size;
			styleMsg = "text-anchor:middle";
		} else if (style == Poly.Type.TEXTBOT)
		{
			p = svgXform(new Point2D.Double(bounds.getCenterX(), bounds.getMaxY()));
			x = p.getX();   y = p.getY();
			styleMsg = "text-anchor:middle";
		} else if (style == Poly.Type.TEXTLEFT)
		{
			p = svgXform(new Point2D.Double(bounds.getMinX(), bounds.getCenterY()));
			x = p.getX();   y = p.getY()+size/2;
		} else if (style == Poly.Type.TEXTRIGHT)
		{
			p = svgXform(new Point2D.Double(bounds.getMaxX(), bounds.getCenterY()));
			x = p.getX();   y = p.getY()+size/2;
			styleMsg = "text-anchor:end";
		} else if (style == Poly.Type.TEXTTOPLEFT)
		{
			p = svgXform(new Point2D.Double(bounds.getMinX(), bounds.getMinY()));
			x = p.getX();   y = p.getY()+size;
		} else if (style == Poly.Type.TEXTTOPRIGHT)
		{
			p = svgXform(new Point2D.Double(bounds.getMaxX(), bounds.getMinY()));
			x = p.getX();   y = p.getY()+size;
			styleMsg = "text-anchor:end";
		} else if (style == Poly.Type.TEXTBOTLEFT)
		{
			p = svgXform(new Point2D.Double(bounds.getMinX(), bounds.getMaxY()));
			x = p.getX();   y = p.getY();
		} else if (style == Poly.Type.TEXTBOTRIGHT)
		{
			p = svgXform(new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()));
			x = p.getX();   y = p.getY();
			styleMsg = "text-anchor:end";
		} else return;
		if (td.isBold()) styleMsg += "; font-weight: bold";
		if (td.isItalic()) styleMsg += "; font-style: italic";

		printWriter.print("<text x=\"" + x + "\" y=\"" + y +
			"\" fill=\"" + getColorDescription(col) + "\" font-size=\"" + size + "\"");
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
			printWriter.println(" font-family=\"" + fixedFaceName + "\"");
		}
		if (styleMsg != null) printWriter.print(" style=\"" + styleMsg + "\"");
		TextDescriptor.Rotation rot = td.getRotation();
		if (rot != TextDescriptor.Rotation.ROT0)
		{
			int amt = 270;
			if (rot == TextDescriptor.Rotation.ROT180) amt = 180; else
			if (rot == TextDescriptor.Rotation.ROT270) amt = 90;
			printWriter.println(" transform=\"rotate(" + amt + " " + x + "," + y + ")\"");
		}
		printWriter.println(">");
		printWriter.println("  " + text);
		printWriter.println("</text>");
	}

	/****************************** SUPPORT ******************************/

	/**
	 * Method to convert coordinates for display.
	 * @param pt the Electric coordinates.
	 * @return the SVG coordinates.
	 */
	private Point2D svgXform(Point2D pt)
	{
		Point2D result = new Point2D.Double();
		matrix.transform(pt, result);
		return result;
	}
}
