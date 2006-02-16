/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VectorDrawing.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import com.sun.electric.Main;
import com.sun.electric.database.CellUsage;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.GenMath.MutableDouble;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.Job;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class to do rapid redraw by caching the vector coordinates of all objects.
 */
public class VectorDrawing
{
	private static final boolean TAKE_STATS = false;
	private static final boolean DEBUGIMAGES = false;
	private static final int MAXGREEKSIZE = 25;

	/** the EditWindow being drawn */						private EditWindow wnd;
	/** the rendering object */								private PixelDrawing offscreen;
	/** the window scale */									private float scale;
	/** the window offset */								private float offX, offY;
	/** the window scale and pan factor */					private float factorX, factorY;
	/** true if "peeking" and expanding to the bottom */	private boolean fullInstantiate;
	/** time that rendering started */						private long startTime;
	/** true if the user has been told of delays */			private boolean takingLongTime;
	/** true to stop rendering */							private boolean stopRendering;
	/** the half-sizes of the window (in pixels) */			private int szHalfWidth, szHalfHeight;
	/** the screen clipping */								private int screenLX, screenHX, screenLY, screenHY;
	/** statistics */										private int boxCount, tinyBoxCount, lineBoxCount, lineCount, polygonCount;
	/** statistics */										private int crossCount, textCount, circleCount, arcCount;
	/** statistics */										private int subCellCount, tinySubCellCount;
	/** the threshold of object sizes */					private float maxObjectSize;
	/** the threshold of text sizes */						private float maxTextSize;
	/** the maximum cell size above which no greeking */	private float maxCellSize;
	/** temporary objects (saves allocation) */				private Point tempPt1 = new Point(), tempPt2 = new Point();
	/** temporary objects (saves allocation) */				private Point tempPt3 = new Point(), tempPt4 = new Point();
	/** temporary object (saves allocation) */				private Rectangle tempRect = new Rectangle();
	/** the color of text */								private Color textColor;

	/** list of cell expansions. */							private static HashMap<Cell,VectorCellGroup> cachedCells = new HashMap<Cell,VectorCellGroup>();
	/** list of polygons to include in cells */				private static HashMap<Cell,List<VectorBase>> addPolyToCell = new HashMap<Cell,List<VectorBase>>();
	/** list of instances to include in cells */			private static HashMap<Cell,List<VectorSubCell>> addInstToCell = new HashMap<Cell,List<VectorSubCell>>();
	/** the object that draws the rendered screen */		private static VectorDrawing topVD;
	/** location for debugging icon displays */				private static int debugXP, debugYP;

	/** zero rectangle */									private static final Rectangle2D CENTERRECT = new Rectangle2D.Double(0, 0, 0, 0);
	private static EGraphics textGraphics = new EGraphics(false, false, null, 0, 0,0,0, 1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
	private static EGraphics instanceGraphics = new EGraphics(false, false, null, 0, 0,0,0, 1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
	private static EGraphics portGraphics = new EGraphics(false, false, null, 0, 255,0,0, 1.0,true,
		new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});

	/**
	 * Class which defines the common information for all cached displayable objects
	 */
	private static class VectorBase
	{
		Layer layer;
		EGraphics graphics;
		boolean hideOnLowLevel;
		float minSize, maxSize;

		VectorBase(Layer layer, EGraphics graphics, boolean hideOnLowLevel)
		{
			this.layer = layer;
			this.graphics = graphics;
			this.hideOnLowLevel = hideOnLowLevel;
			minSize = maxSize = -1;
			hideOnLowLevel = false;
		}

		void setSize(float size1, float size2)
		{
			minSize = Math.min(size1, size2);
			maxSize = Math.max(size1, size2);
		}
	}

	/**
	 * Class which defines a cached Manhattan rectangle.
	 */
	private static class VectorManhattan extends VectorBase
	{
		float c1X, c1Y, c2X, c2Y;
		int tinyColor;

		VectorManhattan(float c1X, float c1Y, float c2X, float c2Y, Layer layer, EGraphics graphics, boolean hideOnLowLevel)
		{
			super(layer, graphics, hideOnLowLevel);
			this.c1X = c1X;
			this.c1Y = c1Y;
			this.c2X = c2X;
			this.c2Y = c2Y;
			float dX = Math.abs(c1X - c2X);
			float dY = Math.abs(c1Y - c2Y);
			setSize(dX, dY);
			tinyColor = -1;
			if (layer != null)
			{
				Layer.Function fun = layer.getFunction();
				if (!fun.isImplant() && !fun.isSubstrate())
				{
					if (graphics != null)
					{
						tinyColor = graphics.getColor().getRGB() & 0xFFFFFF;
					}
				}
			}
		}
	}

	/**
	 * Class which defines a cached polygon (nonmanhattan).
	 */
	private static class VectorPolygon extends VectorBase
	{
		Point2D [] points;

		VectorPolygon(Point2D [] points, Layer layer, EGraphics graphics, boolean hideOnLowLevel)
		{
			super(layer, graphics, hideOnLowLevel);
			this.points = points;
		}
	}

	/**
	 * Class which defines a cached line.
	 */
	private static class VectorLine extends VectorBase
	{
		float fX, fY, tX, tY;
		int texture;

		VectorLine(double fX, double fY, double tX, double tY, int texture, Layer layer, EGraphics graphics, boolean hideOnLowLevel)
		{
			super(layer, graphics, hideOnLowLevel);
			this.fX = (float)fX;
			this.fY = (float)fY;
			this.tX = (float)tX;
			this.tY = (float)tY;
			this.texture = texture;
		}
	}

	/**
	 * Class which defines a cached circle (filled, opened, or thick).
	 */
	private static class VectorCircle extends VectorBase
	{
		float cX, cY, eX, eY;
		int nature;

		VectorCircle(double cX, double cY, double eX, double eY, int nature, Layer layer, EGraphics graphics, boolean hideOnLowLevel)
		{
			super(layer, graphics, hideOnLowLevel);
			this.cX = (float)cX;
			this.cY = (float)cY;
			this.eX = (float)eX;
			this.eY = (float)eY;
			this.nature = nature;
		}
	}

	/**
	 * Class which defines a cached arc of a circle (normal or thick).
	 */
	private static class VectorCircleArc extends VectorBase
	{
		float cX, cY, eX1, eY1, eX2, eY2;
		boolean thick;

		VectorCircleArc(double cX, double cY, double eX1, double eY1, double eX2, double eY2, boolean thick,
			Layer layer, EGraphics graphics, boolean hideOnLowLevel)
		{
			super(layer, graphics, hideOnLowLevel);
			this.cX = (float)cX;
			this.cY = (float)cY;
			this.eX1 = (float)eX1;
			this.eY1 = (float)eY1;
			this.eX2 = (float)eX2;
			this.eY2 = (float)eY2;
			this.thick = thick;
		}
	}

	/**
	 * Class which defines cached text.
	 */
	private static class VectorText extends VectorBase
	{
		/** text is on a Cell */			static final int TEXTTYPECELL       = 1;
		/** text is on an Export */			static final int TEXTTYPEEXPORT     = 2;
		/** text is on a Node */			static final int TEXTTYPENODE       = 3;
		/** text is on an Arc */			static final int TEXTTYPEARC        = 4;
		/** text is on an Annotations */	static final int TEXTTYPEANNOTATION = 5;
		/** text is on an Instances */		static final int TEXTTYPEINSTANCE   = 6;
		/** text is on an Ports */			static final int TEXTTYPEPORT       = 7;

		/** the text location */						Rectangle2D bounds;
		/** the text style */							Poly.Type style;
		/** the descriptor of the text */				TextDescriptor descript;
		/** the text to draw */							String str;
		/** the text height (in display units) */		float height;
		/** the type of text (CELL, EXPORT, etc.) */	int textType;
		/** valid for port text on a node */			NodeInst ni;
		/** valid for port text or export text */		Export e;

		VectorText(Rectangle2D bounds, Poly.Type style, TextDescriptor descript, String str, int textType, NodeInst ni, Export e,
			boolean hideOnLowLevel, Layer layer, EGraphics graphics)
		{
			super(layer, graphics, hideOnLowLevel);
			this.bounds = bounds;
			this.style = style;
			this.descript = descript;
			this.str = str;
			this.textType = textType;
			this.ni = ni;
			this.e = e;

			height = 1;
			if (descript != null)
			{
				TextDescriptor.Size tds = descript.getSize();
				if (!tds.isAbsolute()) height = (float)tds.getSize();
			}
		}
	}

	/**
	 * Class which defines a cached cross (a dot, large or small).
	 */
	private static class VectorCross extends VectorBase
	{
		float x, y;
		boolean small;

		VectorCross(double x, double y, boolean small, Layer layer, EGraphics graphics, boolean hideOnLowLevel)
		{
			super(layer, graphics, hideOnLowLevel);
			this.x = (float)x;
			this.y = (float)y;
			this.small = small;
		}
	}

	/**
	 * Class which defines a cached subcell reference.
	 */
	private static class VectorSubCell
	{
		NodeInst ni;
		Orientation pureRotate;
		Cell subCell;
		float offsetX, offsetY;
		Point2D [] outlinePoints;
		float size;
		List<VectorBase> portShapes;

		VectorSubCell(NodeInst ni, Point2D offset, Point2D [] outlinePoints)
		{
			this.ni = ni;
			if (ni != null)
			{
				pureRotate = ni.getOrient();
				subCell = (Cell)ni.getProto();
				size = (float)Math.min(ni.getXSize(), ni.getYSize());
			} else
			{
			}
			if (offset == null) offsetX = offsetY = 0; else
			{
				offsetX = (float)offset.getX();
				offsetY = (float)offset.getY();
			}
			this.outlinePoints = outlinePoints;
			portShapes = new ArrayList<VectorBase>();
		}
	}

	/**
	 * Class which holds the cell caches for a given cell.
	 * Since each cell is cached many times, once for every orientation on the screen,
	 * this object can hold many cell caches.
	 */
	private static class VectorCellGroup
	{
		Cell cell;
		HashMap<String,VectorCell> orientations;
		VectorCell any;
		double sizeX, sizeY;
		List<VectorCellExport> exports;

		VectorCellGroup(Cell cell)
		{
			this.cell = cell;
			orientations = new HashMap<String,VectorCell>();
			any = null;
		}

		static VectorCellGroup findCellGroup(Cell cell)
		{
			VectorCellGroup vcg = cachedCells.get(cell);
			if (vcg == null)
			{
				vcg = new VectorCellGroup(cell);
				cachedCells.put(cell, vcg);
			}
			return vcg;
		}

		void clear()
		{
			orientations.clear();
			exports.clear();
			any = null;
		}

		VectorCell getAnyCell()
		{
			return any;
		}

		void addCell(VectorCell vc, Orientation trans)
		{
			String orientationName = makeOrientationName(trans);
			orientations.put(orientationName, vc);
			any = vc;
		}
	}

	/**
	 * Class which defines the exports on a cell (used to tell if they changed)
	 */
	private static class VectorCellExport
	{
		String exportName;
		Point2D exportCtr;
	}

	/**
	 * Class which defines a cached cell in a single orientation.
	 */
	private static class VectorCell
	{
		List<VectorBase> shapes;
		List<VectorSubCell> subCells;
		boolean hasFadeColor;
		int fadeColor;
		float maxFeatureSize;
		boolean isParameterized;
		float cellSize;
		boolean fadeImage;
		int fadeOffsetX, fadeOffsetY;
		int [] fadeImageColors;
		int fadeImageWid, fadeImageHei;

		VectorCell()
		{
			shapes = new ArrayList<VectorBase>();
			subCells = new ArrayList<VectorSubCell>();
			hasFadeColor = false;
			maxFeatureSize = 0;
			fadeImage = false;
		}
	}

	// ************************************* TOP LEVEL *************************************

	/**
	 * Constructor creates a VectorDrawing object for a given EditWindow.
	 * @param wnd the EditWindow associated with this VectorDrawing.
	 */
	public VectorDrawing(EditWindow wnd)
	{
		this.wnd = wnd;
	}

	/**
	 * Main entry point for drawing a cell.
	 * @param cell the cell to draw
	 * @param fullInstantiate true to draw all the way to the bottom of the hierarchy.
	 * @param drawLimitBounds the area in the cell to display (null to show all).
	 */
	public void render(Cell cell, boolean fullInstantiate, Rectangle2D drawLimitBounds, VarContext context)
	{
		// set colors to use
		textGraphics.setColor(new Color(User.getColorText()));
		instanceGraphics.setColor(new Color(User.getColorInstanceOutline()));
		textColor = new Color(User.getColorText() & 0xFFFFFF);

		// see if any layers are being highlighted/dimmed
		offscreen = wnd.getOffscreen();
		offscreen.highlightingLayers = false;
		for(Iterator<Layer> it = Technology.getCurrent().getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
			if (layer.isDimmed())
			{
				offscreen.highlightingLayers = true;
				break;
			}
		}

		// set size limit
		scale = (float)wnd.getScale();
		maxObjectSize = (float)User.getGreekSizeLimit() / scale;
		maxTextSize = maxObjectSize / (float)User.getGlobalTextScale();
		Rectangle2D screenBounds = wnd.getDisplayedBounds();
		double screenArea = screenBounds.getWidth() * screenBounds.getHeight();
		maxCellSize = (float)(User.getGreekCellSizeLimit() * screenArea);

		// statistics
		startTime = System.currentTimeMillis();
		long initialUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		takingLongTime = false;
		boxCount = tinyBoxCount = lineBoxCount = lineCount = polygonCount = 0;
		crossCount = textCount = circleCount = arcCount = 0;
		subCellCount = tinySubCellCount = 0;

		// draw recursively
		this.fullInstantiate = fullInstantiate;
		Dimension sz = offscreen.getSize();
		szHalfWidth = sz.width / 2;
		szHalfHeight = sz.height / 2;
		screenLX = 0;   screenHX = sz.width;
		screenLY = 0;   screenHY = sz.height;
		Point2D offset = wnd.getOffset();
		offX = (float)offset.getX();
		offY = (float)offset.getY();
		factorX = szHalfWidth - offX*scale;
		factorY = szHalfHeight + offY*scale;
		if (drawLimitBounds != null)
		{
			Rectangle screenLimit = wnd.databaseToScreen(drawLimitBounds);
			screenLX = screenLimit.x;
			if (screenLX < 0) screenLX = 0;
			screenHX = screenLimit.x + screenLimit.width;
			if (screenHX >= sz.width) screenHX = sz.width-1;
			screenLY = screenLimit.y;
			if (screenLY < 0) screenLY = 0;
			screenHY = screenLimit.y + screenLimit.height;
			if (screenHY >= sz.height) screenHY = sz.height-1;
		}

		// draw the screen, starting with the top cell
		stopRendering = false;
		try
		{
			VectorCell topVC = drawCell(cell, Orientation.IDENT, context);
			render(topVC, 0, 0, Orientation.IDENT, context, 0);
		} catch (AbortRenderingException e)
		{
		}
        topVD = null;

		if (takingLongTime)
		{
			TopLevel.setBusyCursor(false);
			System.out.println("Done");
		}

		if (TAKE_STATS && Job.getDebug())
		{
			long curUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			long memUsed = curUsed - initialUsed;
			long renderTime = System.currentTimeMillis() - startTime;
	        System.out.println("Time to render: "+TextUtils.getElapsedTime(renderTime) + "    Memory Used: "+ memUsed);
			System.out.println("   Rendered "+boxCount+" boxes ("+tinyBoxCount+" tiny, "+lineBoxCount+" lines), "+
				lineCount+" lines, "+polygonCount+" polys, "+crossCount+" crosses, "+
				textCount+" texts, "+circleCount+" circles, "+arcCount+" arcs, "+
				subCellCount+" subcells ("+tinySubCellCount+" tiny)");
		}
	}

	/**
	 * Method to insert a manhattan rectangle into the vector cache for a Cell.
	 * @param lX the low X of the manhattan rectangle.
	 * @param lY the low Y of the manhattan rectangle.
	 * @param hX the high X of the manhattan rectangle.
	 * @param hY the high Y of the manhattan rectangle.
	 * @param layer the layer on which to draw the rectangle.
	 * @param cell the Cell in which to insert the rectangle.
	 */
	public static void addBoxToCell(float lX, float lY, float hX, float hY, Layer layer, Cell cell)
	{
		List<VectorBase> addToThisCell = addPolyToCell.get(cell);
		if (addToThisCell == null)
		{
			addToThisCell = new ArrayList<VectorBase>();
			addPolyToCell.put(cell, addToThisCell);
		}
		EGraphics graphics = null;
		if (layer != null)
			graphics = layer.getGraphics();
		VectorManhattan vm = new VectorManhattan(lX, lY, hX, hY, layer, graphics, false);
		addToThisCell.add(vm);
	}

	/**
	 * Method to insert a manhattan rectangle into the vector cache for a Cell.
	 * @param lX the low X of the manhattan rectangle.
	 * @param lY the low Y of the manhattan rectangle.
	 * @param hX the high X of the manhattan rectangle.
	 * @param hY the high Y of the manhattan rectangle.
	 * @param cell the Cell in which to insert the rectangle.
	 */
	public static void addInstanceToCell(float lX, float lY, float hX, float hY, Cell cell)
	{
		List<VectorSubCell> addToThisCell = addInstToCell.get(cell);
		if (addToThisCell == null)
		{
			addToThisCell = new ArrayList<VectorSubCell>();
			addInstToCell.put(cell, addToThisCell);
		}

		// store the subcell
		Point2D [] points = new Point2D[4];
		points[0] = new Point2D.Float(lX, lY);
		points[1] = new Point2D.Float(lX, hY);
		points[2] = new Point2D.Float(hX, hY);
		points[3] = new Point2D.Float(hX, lY);
		VectorSubCell vsc = new VectorSubCell(null, null, points);
		addToThisCell.add(vsc);
	}
	
	private static final Variable.Key NCCKEY = Variable.newKey("ATTR_NCC");

	/**
	 * Method to tell whether a Cell is parameterized.
	 * Code is taken from tool.drc.Quick.checkEnumerateProtos
	 * Could also use the code in tool.io.output.Spice.checkIfParameterized
	 * @param cell the Cell to examine
	 * @return true if the cell has parameters
	 */
	private boolean isCellParameterized(Cell cell)
	{
		for(Iterator<Variable> vIt = cell.getParameters(); vIt.hasNext(); )
		{
			Variable var = vIt.next();
			// this attribute is not a parameter
			if (var.getKey() == NCCKEY) continue;
			return true;
		}
//		for(Iterator<Variable> vIt = cell.getVariables(); vIt.hasNext(); )
//		{
//			Variable var = vIt.next();
//			if (var.isParam())
//			{
//				// this attribute is not a parameter
//				if (var.getKey() == NCCKEY) continue;
//				return true;
//			}
//		}

		// look for any Java coded stuff (Logical Effort calls)
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			for(Iterator<Variable> vIt = ni.getVariables(); vIt.hasNext(); )
			{
				Variable var = vIt.next();
				if (var.getCode() != TextDescriptor.Code.NONE) return true;
			}
		}
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
			for(Iterator<Variable> vIt = ai.getVariables(); vIt.hasNext(); )
			{
				Variable var = vIt.next();
				if (var.getCode() != TextDescriptor.Code.NONE) return true;
			}
		}
		return false;
	}

	/**
	 * Class to define a signal to abort rendering.
	 */
	class AbortRenderingException extends Exception {}

	/**
	 * Method called when a cell changes: removes any cached displays of that cell
	 * @param cell the cell that changed
	 */
	public static void cellChanged(Cell cell)
	{
		if (cell.isLinked())
		{
			// cell still valid: see if it changed from last cache
			VectorCellGroup vcg = cachedCells.get(cell);
			if (vcg != null && vcg.exports != null)
			{
				boolean changed = false;
				Rectangle2D cellBounds = cell.getBounds();
				if (vcg.sizeX != cellBounds.getWidth() || vcg.sizeY != cellBounds.getHeight()) changed = true; else
				{
					Iterator<VectorCellExport> cIt = vcg.exports.iterator();
					for(Iterator<Export> it = cell.getExports(); it.hasNext(); )
					{
						Export e = it.next();
						if (!cIt.hasNext()) { changed = true;   break; }
						VectorCellExport vce = cIt.next();
						if (!vce.exportName.equals(e.getName()))  { changed = true;   break; }
						Poly poly = e.getOriginalPort().getPoly();
						if (vce.exportCtr.getX() != poly.getCenterX() ||
							vce.exportCtr.getY() != poly.getCenterY()) { changed = true;   break; }
					}
					if (cIt.hasNext()) changed = true;
				}

				// queue parent cells for recaching if the bounds or exports changed
				if (changed)
				{
					for(Iterator<CellUsage> it = cell.getUsagesOf(); it.hasNext(); )
					{
	                    CellUsage u = it.next();
						cellChanged(u.getParent());
					}
				}
			}
		}
//System.out.println("REMOVING CACHE FOR CELL "+cell);
		cachedCells.remove(cell);
	}

	/**
	 * Method called when a technology's parameters change.
	 * All cells that use the technology must be recached.
	 * @param tech the technology that changed.
	 */
	public static void technologyChanged(Technology tech)
	{
		for(Cell cell : cachedCells.keySet())
		{
			if (cell.getTechnology() != tech) continue;
			VectorCellGroup vcg = cachedCells.get(cell);
			vcg.clear();
		}
	}

	/**
	 * Method called when visible layers have changed.
	 * Removes all "greeked images" from cached cells.
	 */
	public static void layerVisibilityChanged()
	{
		for(Cell cell : cachedCells.keySet())
		{
			VectorCellGroup vcg = cachedCells.get(cell);
			for(String orientationName : vcg.orientations.keySet())
			{
				VectorCell vc = vcg.orientations.get(orientationName);
				vc.fadeImageColors = null;
				vc.fadeImage = false;
			}
		}
	}

	/**
	 * Method to request that the current rendering be aborted because it must be restarted.
	 *
	 */
	public void abortRendering()
	{
		stopRendering = true;
	}

	/**
	 * Method to recursively render a cached cell.
	 * @param vc the cached cell to render
	 * @param oX the X offset for rendering the cell (in database coordinates).
	 * @param oY the Y offset for rendering the cell (in database coordinates).
	 * @param trans the orientation of the cell (this is not a transformation matrix with offsets, just an Orientation with rotation).
	 * @param context the VarContext for this point in the rendering.
	 * @param level: 0=top-level cell in window; 1=low level cell; -1=greeked cell.
	 */
	private void render(VectorCell vc, float oX, float oY, Orientation trans, VarContext context, int level)
		throws AbortRenderingException
	{
		if (level == 0) topVD = this;

		// render main list of shapes
		drawList(oX, oY, vc.shapes, level);

		// now render subcells
		for(VectorSubCell vsc : vc.subCells)
		{
			if (stopRendering) throw new AbortRenderingException();
			subCellCount++;

			// get instance location
			float p1x = (float)vsc.outlinePoints[0].getX() + oX;
			float p2x = (float)vsc.outlinePoints[1].getX() + oX;
			float p3x = (float)vsc.outlinePoints[2].getX() + oX;
			float p4x = (float)vsc.outlinePoints[3].getX() + oX;
			float p1y = (float)vsc.outlinePoints[0].getY() + oY;
			float p2y = (float)vsc.outlinePoints[1].getY() + oY;
			float p3y = (float)vsc.outlinePoints[2].getY() + oY;
			float p4y = (float)vsc.outlinePoints[3].getY() + oY;
			databaseToScreen(p1x, p1y, tempPt1);   databaseToScreen(p2x, p2y, tempPt2);
			databaseToScreen(p3x, p3y, tempPt3);   databaseToScreen(p4x, p4y, tempPt4);
			int lX = Math.min(Math.min(tempPt1.x, tempPt2.x), Math.min(tempPt3.x, tempPt4.x));
			int hX = Math.max(Math.max(tempPt1.x, tempPt2.x), Math.max(tempPt3.x, tempPt4.x));
			int lY = Math.min(Math.min(tempPt1.y, tempPt2.y), Math.min(tempPt3.y, tempPt4.y));
			int hY = Math.max(Math.max(tempPt1.y, tempPt2.y), Math.max(tempPt3.y, tempPt4.y));

			// see if the subcell is clipped
			if (hX < screenLX || lX >= screenHX) continue;
			if (hY < screenLY || lY >= screenHY) continue;

			// see if the cell is too tiny to draw
			if (vsc.size < maxObjectSize && vsc.ni != null)
			{
				Orientation thisOrient = vsc.ni.getOrient();
				Orientation recurseTrans = trans.concatenate(thisOrient);
				VarContext subContext = context.push(vsc.ni);
				VectorCell subVC = drawCell(vsc.subCell, recurseTrans, subContext);
				makeGreekedImage(vsc, subVC);

				int fadeColor = getFadeColor(subVC, subContext);
				drawTinyBox(lX, hX, lY, hY, fadeColor, subVC);
				tinySubCellCount++;
				continue;
			}

			boolean expanded = false;
			if (vsc.ni != null)
			{
				expanded = vsc.ni.isExpanded();
				if (fullInstantiate) expanded = true;
			}

			// if not expanded, but viewing this cell in-place, expand it
			if (!expanded)
			{
				List<NodeInst> path = wnd.getInPlaceEditNodePath();
				if (path != null)
				{
					for(int pathIndex=0; pathIndex<path.size(); pathIndex++)
					{
						NodeInst niOnPath = path.get(pathIndex);
						if (niOnPath.getProto() == vsc.subCell)
						{
							expanded = true;
							break;
						}
					}
				}
			}

			if (expanded)
			{
				Orientation thisOrient = vsc.ni.getOrient();
				Orientation recurseTrans = trans.concatenate(thisOrient);
				VarContext subContext = context.push(vsc.ni);
				VectorCell subVC = drawCell(vsc.subCell, recurseTrans, subContext);

				// expanded cells may be replaced with greeked versions (not icons)
				if (vsc.subCell.getView() != View.ICON)
				{
					// may also be "tiny" if all features in the cell are tiny
					if (subVC.maxFeatureSize > 0 && subVC.maxFeatureSize < maxObjectSize && subVC.cellSize < maxCellSize)
					{
						boolean allTinyInside = isContentsTiny(vsc.subCell, subVC, recurseTrans, context);
						if (allTinyInside)
						{
							makeGreekedImage(vsc, subVC);
							int fadeColor = getFadeColor(subVC, context);
							drawTinyBox(lX, hX, lY, hY, fadeColor, subVC);
							tinySubCellCount++;
							continue;
						}
					}

					// may also be "tiny" if the cell is smaller than the greeked image
					if (User.isUseCellGreekingImages() && hX-lX <= MAXGREEKSIZE && hY-lY <= MAXGREEKSIZE)
					{
						makeGreekedImage(vsc, subVC);
						int fadeColor = getFadeColor(subVC, context);
						drawTinyBox(lX, hX, lY, hY, fadeColor, subVC);
						tinySubCellCount++;
						continue;
					}
				}

				int subLevel = level;
				if (subLevel == 0) subLevel = 1;
				render(subVC, vsc.offsetX + oX, vsc.offsetY + oY, recurseTrans, subContext, subLevel);
			} else
			{
				// now draw with the proper line type
				databaseToScreen(p1x, p1y, tempPt1);   databaseToScreen(p2x, p2y, tempPt2);
				offscreen.drawLine(tempPt1, tempPt2, null, instanceGraphics, 0, false);
				databaseToScreen(p2x, p2y, tempPt1);   databaseToScreen(p3x, p3y, tempPt2);
				offscreen.drawLine(tempPt1, tempPt2, null, instanceGraphics, 0, false);
				databaseToScreen(p3x, p3y, tempPt1);   databaseToScreen(p4x, p4y, tempPt2);
				offscreen.drawLine(tempPt1, tempPt2, null, instanceGraphics, 0, false);
				databaseToScreen(p1x, p1y, tempPt1);   databaseToScreen(p4x, p4y, tempPt2);
				offscreen.drawLine(tempPt1, tempPt2, null, instanceGraphics, 0, false);

				// draw the instance name
				if (User.isTextVisibilityOnInstance() && vsc.ni != null)
				{
					tempRect.setRect(lX, lY, hX-lX, hY-lY);
					TextDescriptor descript = vsc.ni.getTextDescriptor(NodeInst.NODE_PROTO);
					NodeProto np = vsc.ni.getProto();
					offscreen.drawText(tempRect, Poly.Type.TEXTBOX, descript, np.describe(false), null, textGraphics, false);
				}
			}
			drawList(oX, oY, vsc.portShapes, level);
		}
	}

	/**
	 * Method to draw a list of cached shapes.
	 * @param oX the X offset to draw the shapes (in display coordinates).
	 * @param oY the Y offset to draw the shapes (in display coordinates).
	 * @param shapes the List of shapes (VectorBase objects).
	 * @param level: 0=top-level cell in window; 1=low level cell; -1=greeked cell.
	 */
	private void drawList(float oX, float oY, List<VectorBase> shapes, int level)
		throws AbortRenderingException
	{
		// render all shapes
		for(VectorBase vb : shapes)
		{
			if (stopRendering) throw new AbortRenderingException();
			if (vb.hideOnLowLevel && level != 0) continue;

			// get visual characteristics of shape
			Layer layer = vb.layer;
			boolean dimmed = false;
			if (layer != null)
			{
				if (level < 0)
				{
					// greeked cells ignore cut and implant layers
					Layer.Function fun = layer.getFunction();
					if (fun.isContact() || fun.isWell() || fun.isSubstrate()) continue;
				}
//				if (!forceVisible && !vb.layer.isVisible()) continue;
				if (!layer.isVisible()) continue;
				dimmed = layer.isDimmed();
			}

			// handle each shape
			if (vb instanceof VectorManhattan)
			{
				boxCount++;
				VectorManhattan vm = (VectorManhattan)vb;

				if (vm.minSize < maxObjectSize)
				{
					int col = vm.tinyColor;
					if (col < 0) continue;
					if (vm.maxSize < maxObjectSize)
					{
						// both dimensions tiny: just draw a dot
						databaseToScreen(vm.c1X+oX, vm.c1Y+oY, tempPt1);
						int x = tempPt1.x;
						int y = tempPt1.y;
						if (x < screenLX || x >= screenHX) continue;
						if (y < screenLY || y >= screenHY) continue;
						offscreen.drawPoint(x, y, null, col);
						tinyBoxCount++;
					} else
					{
						// one dimension tiny: draw a line
						databaseToScreen(vm.c1X+oX, vm.c1Y+oY, tempPt1);
						databaseToScreen(vm.c2X+oX, vm.c2Y+oY, tempPt2);
						int lX, hX, lY, hY;
						if (tempPt1.x < tempPt2.x) { lX = tempPt1.x;   hX = tempPt2.x; } else
							{ lX = tempPt2.x;   hX = tempPt1.x; }
						if (hX < screenLX || lX >= screenHX) continue;
						if (tempPt1.y < tempPt2.y) { lY = tempPt1.y;   hY = tempPt2.y; } else
							{ lY = tempPt2.y;   hY = tempPt1.y; }
						if (hY < screenLY || lY >= screenHY) continue;
						drawTinyBox(lX, hX, lY, hY, col, null);
						lineBoxCount++;
					}
					continue;
				}

				// determine coordinates of rectangle on the screen
				databaseToScreen(vm.c1X+oX, vm.c1Y+oY, tempPt1);
				databaseToScreen(vm.c2X+oX, vm.c2Y+oY, tempPt2);

				// reject if completely off the screen
				int lX, hX, lY, hY;
				if (tempPt1.x < tempPt2.x) { lX = tempPt1.x;   hX = tempPt2.x; } else
					{ lX = tempPt2.x;   hX = tempPt1.x; }
				if (hX < screenLX || lX >= screenHX) continue;
				if (tempPt1.y < tempPt2.y) { lY = tempPt1.y;   hY = tempPt2.y; } else
					{ lY = tempPt2.y;   hY = tempPt1.y; }
				if (hY < screenLY || lY >= screenHY) continue;

				// clip to screen
				if (lX < screenLX) lX = screenLX;
				if (hX >= screenHX) hX = screenHX-1;
				if (lY < screenLY) lY = screenLY;
				if (hY >= screenHY) hY = screenHY-1;

				// draw the box
				byte [][] layerBitMap = null;
				EGraphics graphics = vb.graphics;
				if (graphics != null)
				{
					int layerNum = graphics.getTransparentLayer() - 1;
					if (layerNum < offscreen.numLayerBitMaps) layerBitMap = offscreen.getLayerBitMap(layerNum);
				}
				offscreen.drawBox(lX, hX, lY, hY, layerBitMap, graphics, dimmed);
				continue;
			}

			byte [][] layerBitMap = null;
			EGraphics graphics = vb.graphics;
			if (graphics != null)
			{
				int layerNum = graphics.getTransparentLayer() - 1;
				if (layerNum < offscreen.numLayerBitMaps) layerBitMap = offscreen.getLayerBitMap(layerNum);
			}
			if (vb instanceof VectorLine)
			{
				lineCount++;
				VectorLine vl = (VectorLine)vb;

				// determine coordinates of line on the screen
				databaseToScreen(vl.fX+oX, vl.fY+oY, tempPt1);
				databaseToScreen(vl.tX+oX, vl.tY+oY, tempPt2);

				// clip and draw the line
				offscreen.drawLine(tempPt1, tempPt2, layerBitMap, graphics, vl.texture, dimmed);
			} else if (vb instanceof VectorPolygon)
			{
				polygonCount++;
				VectorPolygon vp = (VectorPolygon)vb;
				Point [] intPoints = new Point[vp.points.length];
				for(int i=0; i<vp.points.length; i++)
				{
	                intPoints[i] = new Point();
					databaseToScreen(vp.points[i].getX()+oX, vp.points[i].getY()+oY, intPoints[i]);
	            }
				Point [] clippedPoints = GenMath.clipPoly(intPoints, screenLX, screenHX-1, screenLY, screenHY-1);
				offscreen.drawPolygon(clippedPoints, layerBitMap, graphics, dimmed);
			} else if (vb instanceof VectorCross)
			{
				crossCount++;
				VectorCross vcr = (VectorCross)vb;
				databaseToScreen(vcr.x+oX, vcr.y+oY, tempPt1);
				int size = 5;
				if (vcr.small) size = 3;
				offscreen.drawLine(new Point(tempPt1.x-size, tempPt1.y), new Point(tempPt1.x+size, tempPt1.y), null, graphics, 0, dimmed);
				offscreen.drawLine(new Point(tempPt1.x, tempPt1.y-size), new Point(tempPt1.x, tempPt1.y+size), null, graphics, 0, dimmed);
			} else if (vb instanceof VectorText)
			{
				VectorText vt = (VectorText)vb;
				switch (vt.textType)
				{
					case VectorText.TEXTTYPEARC:
						if (!User.isTextVisibilityOnArc()) continue;
						break;
					case VectorText.TEXTTYPENODE:
						if (!User.isTextVisibilityOnNode()) continue;
						break;
					case VectorText.TEXTTYPECELL:
						if (!User.isTextVisibilityOnCell()) continue;
						break;
					case VectorText.TEXTTYPEEXPORT:
						if (!User.isTextVisibilityOnExport()) continue;
						break;
					case VectorText.TEXTTYPEANNOTATION:
						if (!User.isTextVisibilityOnAnnotation()) continue;
						break;
					case VectorText.TEXTTYPEINSTANCE:
						if (!User.isTextVisibilityOnInstance()) continue;
						break;
					case VectorText.TEXTTYPEPORT:
						if (!User.isTextVisibilityOnPort()) continue;
						break;
				}
				if (vt.height < maxTextSize) continue;

				String drawString = vt.str;
				databaseToScreen(vt.bounds.getMinX()+oX, vt.bounds.getMinY()+oY, tempPt1);
				databaseToScreen(vt.bounds.getMaxX()+oX, vt.bounds.getMaxY()+oY, tempPt2);
				int lX, hX, lY, hY;
				if (tempPt1.x < tempPt2.x) { lX = tempPt1.x;   hX = tempPt2.x; } else
					{ lX = tempPt2.x;   hX = tempPt1.x; }
				if (tempPt1.y < tempPt2.y) { lY = tempPt1.y;   hY = tempPt2.y; } else
					{ lY = tempPt2.y;   hY = tempPt1.y; }

				// for ports, switch between the different port display methods
				if (vt.textType == VectorText.TEXTTYPEPORT)
				{
					int portDisplayLevel = User.getPortDisplayLevel();
					Color portColor = vt.e.getBasePort().getPortColor();
					if (vt.ni.isExpanded()) portColor = textColor;
					if (portColor != null) portGraphics.setColor(portColor);
					int cX = (lX + hX) / 2;
					int cY = (lY + hY) / 2;
					if (portDisplayLevel == 2)
					{
						// draw port as a cross
						int size = 3;
						offscreen.drawLine(new Point(cX-size, cY), new Point(cX+size, cY), null, portGraphics, 0, false);
						offscreen.drawLine(new Point(cX, cY-size), new Point(cX, cY+size), null, portGraphics, 0, false);
						crossCount++;
						continue;
					}

					// draw port as text
					if (portDisplayLevel == 1) drawString = vt.e.getShortName(); else
						drawString = vt.e.getName();
					graphics = portGraphics;
					layerBitMap = null;
					lX = hX = cX;
					lY = hY = cY;
				} else if (vt.textType == VectorText.TEXTTYPEEXPORT && vt.e != null)
				{
					int exportDisplayLevel = User.getExportDisplayLevel();
					if (exportDisplayLevel == 2)
					{
						// draw export as a cross
						int cX = (lX + hX) / 2;
						int cY = (lY + hY) / 2;
						int size = 3;
						offscreen.drawLine(new Point(cX-size, cY), new Point(cX+size, cY), null, textGraphics, 0, false);
						offscreen.drawLine(new Point(cX, cY-size), new Point(cX, cY+size), null, textGraphics, 0, false);
						crossCount++;
						continue;
					}

					// draw export as text
					if (exportDisplayLevel == 1) drawString = vt.e.getShortName(); else
						drawString = vt.e.getName();
					graphics = textGraphics;
					layerBitMap = null;
				}

				textCount++;
				tempRect.setRect(lX, lY, hX-lX, hY-lY);
				offscreen.drawText(tempRect, vt.style, vt.descript, drawString, layerBitMap, graphics, dimmed);
			} else if (vb instanceof VectorCircle)
			{
				circleCount++;
				VectorCircle vci = (VectorCircle)vb;
				databaseToScreen(vci.cX+oX, vci.cY+oY, tempPt1);
				databaseToScreen(vci.eX+oX, vci.eY+oY, tempPt2);
				switch (vci.nature)
				{
					case 0: offscreen.drawCircle(tempPt1, tempPt2, layerBitMap, graphics, dimmed);        break;
					case 1: offscreen.drawThickCircle(tempPt1, tempPt2, layerBitMap, graphics, dimmed);   break;
					case 2: offscreen.drawDisc(tempPt1, tempPt2, layerBitMap, graphics, dimmed);          break;
				}
			} else if (vb instanceof VectorCircleArc)
			{
				arcCount++;
				VectorCircleArc vca = (VectorCircleArc)vb;
				databaseToScreen(vca.cX+oX, vca.cY+oY, tempPt1);
				databaseToScreen(vca.eX1+oX, vca.eY1+oY, tempPt2);
				databaseToScreen(vca.eX2+oX, vca.eY2+oY, tempPt3);
				offscreen.drawCircleArc(tempPt1, tempPt2, tempPt3, vca.thick, layerBitMap, graphics, dimmed);
			}
		}
	}

	/**
	 * Method to convert a database coordinate to screen coordinates.
	 * @param dbX the X coordinate (in database units).
	 * @param dbY the Y coordinate (in database units).
	 * @param result the Point in which to store the screen coordinates.
	 */
	public void databaseToScreen(double dbX, double dbY, Point result)
	{
		double scrX = dbX * scale + factorX;
		double scrY = factorY - dbY * scale;
		result.x = (int)(scrX >= 0 ? scrX + 0.5 : scrX - 0.5);
		result.y = (int)(scrY >= 0 ? scrY + 0.5 : scrY - 0.5);
	}

	/**
	 * Method to draw a tiny box on the screen in a given color.
	 * Done when the object is too small to draw in full detail.
	 * @param lX the low X coordinate of the box.
	 * @param hX the high X coordinate of the box.
	 * @param lY the low Y coordinate of the box.
	 * @param hY the high Y coordinate of the box.
	 * @param col the color to draw.
	 */
	private void drawTinyBox(int lX, int hX, int lY, int hY, int col, VectorCell greekedCell)
	{
		if (lX < screenLX) lX = screenLX;
		if (hX >= screenHX) hX = screenHX-1;
		if (lY < screenLY) lY = screenLY;
		if (hY >= screenHY) hY = screenHY-1;
		if (User.isUseCellGreekingImages())
		{
			if (greekedCell != null && greekedCell.fadeImageColors != null)
			{
				int backgroundColor = User.getColorBackground();
				int backgroundRed = (backgroundColor >> 16) & 0xFF;
				int backgroundGreen = (backgroundColor >> 8) & 0xFF;
				int backgroundBlue = backgroundColor & 0xFF;

				// render the icon properly with scale
				int greekWid = greekedCell.fadeImageWid;
				int greekHei = greekedCell.fadeImageHei;
				int wid = hX - lX;
				int hei = hY - lY;
				float xInc = greekWid / (float)wid;
				float yInc = greekHei / (float)hei;
				float yPos = 0;
				for(int y=0; y<hei; y++)
				{
					float yEndPos = yPos + yInc;
					int yS = (int)yPos;
					int yE = (int)yEndPos;

					float xPos = 0;
					for(int x=0; x<wid; x++)
					{
						float xEndPos = xPos + xInc;
						int xS = (int)xPos;
						int xE = (int)xEndPos;

						float r = 0, g = 0, b = 0;
						float totalArea = 0;
						for(int yGrab = yS; yGrab <= yE; yGrab++)
						{
							if (yGrab >= greekHei) continue;
							float yArea = 1;
							if (yGrab == yS) yArea = (1 - (yPos - yS));
							if (yGrab == yE) yArea *= (yEndPos-yE);

							for(int xGrab = xS; xGrab <= xE; xGrab++)
							{
								if (xGrab >= greekWid) continue;
								int value = greekedCell.fadeImageColors[xGrab + yGrab*greekedCell.fadeImageWid];
								int red = (value >> 16) & 0xFF;
								int green = (value >> 8) & 0xFF;
								int blue = value & 0xFF;
								float area = yArea;
								if (xGrab == xS) area *= (1 - (xPos - xS));
								if (xGrab == xE) area *= (xEndPos-xE);
								if (area <= 0) continue;
								r += red * area;
								g += green * area;
								b += blue * area;
								totalArea += area;
							}
						}
						if (totalArea > 0)
						{
							int red = (int)(r / totalArea);
							if (red > 255) red = 255;
							int green = (int)(g / totalArea);
							if (green > 255) green = 255;
							int blue = (int)(b / totalArea);
							if (blue > 255) blue = 255;
							if (Math.abs(backgroundRed-red) > 2 || Math.abs(backgroundGreen-green) > 2 ||
								Math.abs(backgroundBlue-blue) > 2)
							{
								offscreen.drawPoint(lX+x, lY+y, null, (red << 16) | (green << 8) | blue);
							}
						}
						xPos = xEndPos;
					}
					yPos = yEndPos;
				}
				if (DEBUGIMAGES)
				{
					for(int y=0; y<greekedCell.fadeImageHei; y++)
					{
						for(int x=0; x<greekedCell.fadeImageWid; x++)
						{
							int valToSet = greekedCell.fadeImageColors[x+y*greekedCell.fadeImageWid];
							topVD.offscreen.drawPoint(greekedCell.fadeOffsetX+x+1, greekedCell.fadeOffsetY+y+1, null, valToSet);
						}
						topVD.offscreen.drawPoint(greekedCell.fadeOffsetX, greekedCell.fadeOffsetY+y+1, null, 0);
						topVD.offscreen.drawPoint(greekedCell.fadeOffsetX+greekedCell.fadeImageWid+1, greekedCell.fadeOffsetY+y+1, null, 0);
					}
					for(int x=0; x<greekedCell.fadeImageWid; x++)
					{
						topVD.offscreen.drawPoint(greekedCell.fadeOffsetX+x, greekedCell.fadeOffsetY, null, 0);
						topVD.offscreen.drawPoint(greekedCell.fadeOffsetX+x, greekedCell.fadeOffsetY+greekedCell.fadeImageHei+1, null, 0);
					}
				}
				return;
			}
		}

		// no greeked image: just use the greeked color
		for(int y=lY; y<=hY; y++)
		{
			for(int x=lX; x<=hX; x++)
				offscreen.drawPoint(x, y, null, col);
		}
	}

	/**
	 * Method to determine whether a cell has tiny contents.
	 * Recursively examines the cache of this and all subcells to see if the
	 * maximum feature sizes are all below the global threshold "maxObjectSize".
	 * @param cell the Cell in question.
	 * @param vc the cached representation of the cell.
	 * @param trans the Orientation of the cell.
	 * @return true if the cell has all tiny contents.
	 */
	private boolean isContentsTiny(Cell cell, VectorCell vc, Orientation trans, VarContext context)
		throws AbortRenderingException
	{
		if (vc.maxFeatureSize > maxObjectSize) return false;
		boolean isAllTiny = true;
		for(VectorSubCell vsc : vc.subCells)
		{
			NodeInst ni = vsc.ni;
			boolean expanded = ni.isExpanded();
			if (fullInstantiate) expanded = true;
			if (expanded)
			{
				Orientation thisOrient = ni.getOrient();
				Orientation recurseTrans = thisOrient.concatenate(trans);
				VarContext subContext = context.push(ni);
				VectorCell subVC = drawCell(vsc.subCell, recurseTrans, subContext);
				boolean subCellTiny = isContentsTiny(vsc.subCell, subVC, recurseTrans, subContext);
				if (!subCellTiny) return false;
				continue;
			}
			if (vsc.size > maxObjectSize) return false;
		}
		return true;
	}

	private void makeGreekedImage(VectorSubCell vsc, VectorCell subVC)
		throws AbortRenderingException
	{
		if (subVC.fadeImage) return;
		if (!User.isUseCellGreekingImages()) return;

		// determine size and scale of greeked cell image
		Rectangle2D cellBounds = vsc.subCell.getBounds();
		Rectangle2D ownBounds = new Rectangle2D.Double(cellBounds.getMinX(), cellBounds.getMinY(), cellBounds.getWidth(), cellBounds.getHeight());
		AffineTransform trans = vsc.pureRotate.rotateAbout(0, 0);
		GenMath.transformRect(ownBounds, trans);
		double greekScale = MAXGREEKSIZE / ownBounds.getHeight();
		if (ownBounds.getWidth() > ownBounds.getHeight())
			greekScale = MAXGREEKSIZE / ownBounds.getWidth();
		int greekWid = (int)(ownBounds.getWidth()*greekScale + 0.5);
		if (greekWid <= 0) greekWid = 1;
		int greekHei = (int)(ownBounds.getHeight()*greekScale + 0.5);
		if (greekHei <= 0) greekHei = 1;

		// construct the offscreen buffers for the greeked cell image
		EditWindow fadeWnd = EditWindow.CreateElectricDoc(vsc.subCell, null, null);
		fadeWnd.setScreenSize(new Dimension(greekWid, greekHei));
		fadeWnd.setScale(greekScale);
		Point2D cellCtr = new Point2D.Double(ownBounds.getCenterX(), ownBounds.getCenterY());
		fadeWnd.setOffset(cellCtr);
		VectorDrawing subVD = new VectorDrawing(fadeWnd);

		subVC.fadeOffsetX = debugXP;
		subVC.fadeOffsetY = debugYP;
		debugXP += MAXGREEKSIZE + 5;
		if (topVD != null)
		{
			if (debugXP + MAXGREEKSIZE+2 >= topVD.offscreen.getSize().width)
			{
				debugXP = 0;
				debugYP += MAXGREEKSIZE + 5;
			}
		}

//System.out.println("Making greek for "+vsc.subCell+" "+greekWid+"x"+greekHei);

		// set rendering information for the greeked cell image
		subVD.offscreen = fadeWnd.getOffscreen();
		subVD.screenLX = 0;   subVD.screenHX = greekWid;
		subVD.screenLY = 0;   subVD.screenHY = greekHei;
		subVD.szHalfWidth = greekWid / 2;
		subVD.szHalfHeight = greekHei / 2;
		subVD.maxObjectSize = 0;
		subVD.maxTextSize = 0;
		subVD.scale = (float)greekScale;
		subVD.offX = (float)cellCtr.getX();
		subVD.offY = (float)cellCtr.getY();
		subVD.factorX = subVD.szHalfWidth - subVD.offX*subVD.scale;
		subVD.factorY = subVD.szHalfHeight + subVD.offY*subVD.scale;
		subVD.fullInstantiate = true;
		subVD.takingLongTime = true;

		// render the greeked cell
		subVD.offscreen.clearImage(false, null);
		subVD.render(subVC, 0, 0, vsc.pureRotate, VarContext.globalContext, -1);
		subVD.offscreen.composite(null);

		// remember the greeked cell image
		BufferedImage img = subVD.offscreen.getBufferedImage();
		subVC.fadeImageWid = img.getWidth();
		subVC.fadeImageHei = img.getHeight();
		subVC.fadeImageColors = new int[subVC.fadeImageWid * subVC.fadeImageHei];
		int i = 0;
		for(int y=0; y<subVC.fadeImageHei; y++)
		{
			for(int x=0; x<subVC.fadeImageWid; x++)
			{
				int value = img.getRGB(x, y);
				subVC.fadeImageColors[i++] = value & 0xFFFFFF;
			}
		}
		subVD.wnd.finished();
		subVC.fadeImage = true;
	}

	/**
	 * Method to determine the "fade" color for a cached cell.
	 * Fading is done when the cell is too tiny to draw (or all of its contents are too tiny).
	 * Instead of drawing the cell contents, the entire cell is painted with the "fade" color.
	 * @param vc the cached cell.
	 * @return the fade color (an integer with red/green/blue).
	 */
	private int getFadeColor(VectorCell vc, VarContext context)
		throws AbortRenderingException
	{
		if (vc.hasFadeColor) return vc.fadeColor;

		// examine all shapes
		HashMap<Layer,MutableDouble> layerAreas = new HashMap<Layer,MutableDouble>();
		gatherContents(vc, layerAreas, context);

		// now compute the color
		Set<Layer> keys = layerAreas.keySet();
		double totalArea = 0;
		for(Layer layer : keys)
		{
			MutableDouble md = layerAreas.get(layer);
			totalArea += md.doubleValue();
		}
		double r = 0, g = 0, b = 0;
		if (totalArea != 0)
		{
			for(Layer layer : keys)
			{
				MutableDouble md = layerAreas.get(layer);
				double portion = md.doubleValue() / totalArea;
				EGraphics desc = layer.getGraphics();
				Color col = desc.getColor();
				r += col.getRed() * portion;
				g += col.getGreen() * portion;
				b += col.getBlue() * portion;
			}
		}
		if (r < 0) r = 0;   if (r > 255) r = 255;
		if (g < 0) g = 0;   if (g > 255) g = 255;
		if (b < 0) b = 0;   if (b > 255) b = 255;
		vc.fadeColor = (((int)r) << 16) | (((int)g) << 8) | (int)b;
		vc.hasFadeColor = true;
		return vc.fadeColor;
	}

	/**
	 * Helper method to recursively examine a cached cell and its subcells and compute
	 * the coverage of each layer.
	 * @param vc the cached cell to examine.
	 * @param layerAreas a HashMap of all layers and the areas they cover.
	 */
	private void gatherContents(VectorCell vc, HashMap<Layer,MutableDouble> layerAreas, VarContext context)
		throws AbortRenderingException
	{
		for(VectorBase vb : vc.shapes)
		{
			if (vb.hideOnLowLevel) continue;
			Layer layer = vb.layer;
			if (layer == null) continue;
			Layer.Function fun = layer.getFunction();
			if (fun.isImplant() || fun.isSubstrate()) continue;

			// handle each shape
			double area = 0;
			if (vb instanceof VectorManhattan)
			{
				VectorManhattan vm = (VectorManhattan)vb;
				area = Math.abs((vm.c1X-vm.c2X) * (vm.c1Y-vm.c2Y));
			} else if (vb instanceof VectorLine)
			{
				VectorLine vl = (VectorLine)vb;
				area = new Point2D.Double(vl.fX, vl.fY).distance(new Point2D.Double(vl.tX, vl.tY));
			} else if (vb instanceof VectorPolygon)
			{
				VectorPolygon vp = (VectorPolygon)vb;
				area = GenMath.getAreaOfPoints(vp.points);
			} else if (vb instanceof VectorCircle)
			{
				VectorCircle vci = (VectorCircle)vb;
				double radius = new Point2D.Double(vci.cX, vci.cY).distance(new Point2D.Double(vci.eX, vci.eY));
				area = radius * radius * Math.PI;
			}
			if (area == 0) continue;
			MutableDouble md = layerAreas.get(layer);
			if (md == null)
			{
				md = new MutableDouble(0);
				layerAreas.put(layer, md);
			}
			md.setValue(md.doubleValue() + area);
		}

		for(VectorSubCell vsc : vc.subCells)
		{
			VectorCellGroup vcg = VectorCellGroup.findCellGroup(vsc.subCell);
			VectorCell subVC = vcg.getAnyCell();
			VarContext subContext = context.push(vsc.ni);
			if (subVC == null)
				subVC = drawCell(vsc.subCell, Orientation.IDENT, subContext);
			gatherContents(subVC, layerAreas, subContext);
		}
	}

	// ************************************* CACHE CREATION *************************************

	/**
	 * Method to cache the contents of a cell.
	 * @param cell the Cell to cache
	 * @param prevTrans the orientation of the cell (just a rotation, no offsets here).
	 * @return a cached cell object for the given Cell.
	 */
	private VectorCell drawCell(Cell cell, Orientation prevTrans, VarContext context)
		throws AbortRenderingException
	{
		// see if this cell's vectors are cached
		VectorCellGroup vcg = VectorCellGroup.findCellGroup(cell);
		String orientationName = makeOrientationName(prevTrans);
		VectorCell vc = vcg.orientations.get(orientationName);
		
		// if the cell is parameterized, mark it for recaching
		if (vc != null && vc.isParameterized) vc = null;

		// if the cell is cached, stop now
		if (vc != null) return vc;

		// caching the cell: check for abort and delay reporting
		if (stopRendering) throw new AbortRenderingException();
		if (!takingLongTime)
		{
			long currentTime = System.currentTimeMillis();
			if (currentTime - startTime > 1000)
			{
				System.out.print("Display caching, please wait...");
				TopLevel.setBusyCursor(true);
				takingLongTime = true;
			}
		}

		// make a new cache of the cell
		vc = new VectorCell();
		vcg.addCell(vc, prevTrans);
		vc.isParameterized = isCellParameterized(cell);
		Rectangle2D cellBounds = cell.getBounds();
		vc.cellSize = (float)(cellBounds.getWidth() * cellBounds.getHeight());
		AffineTransform trans = prevTrans.pureRotate();

		// save size and export centers to detect hierarchical changes later
		if (vcg.exports == null)
		{
			vcg.exports = new ArrayList<VectorCellExport>();
			vcg.sizeX = cellBounds.getWidth();
			vcg.sizeY = cellBounds.getHeight();
			for(Iterator<Export> it = cell.getExports(); it.hasNext(); )
			{
				Export e = it.next();
				VectorCellExport vce = new VectorCellExport();
				vce.exportName = e.getName();
				Poly poly = e.getOriginalPort().getPoly();
				vce.exportCtr = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
				vcg.exports.add(vce);
			}
		}

//System.out.println("CACHING CELL "+cell +" WITH ORIENTATION "+orientationName);
		// draw all arcs
		for(Iterator<ArcInst> arcs = cell.getArcs(); arcs.hasNext(); )
		{
			ArcInst ai = arcs.next();
			drawArc(ai, trans, vc);
		}

		// draw all nodes
		for(Iterator<NodeInst> nodes = cell.getNodes(); nodes.hasNext(); )
		{
			NodeInst ni = nodes.next();
			drawNode(ni, trans, context, vc);
		}

		// for schematics, sort the polygons by layer so that busses are drawn first
		if (cell.getView() == View.SCHEMATIC)
		{
            Collections.sort(vc.shapes, new ShapeByLayer());
		}

		// show cell variables
		int numPolys = cell.numDisplayableVariables(true);
		Poly [] polys = new Poly[numPolys];
		cell.addDisplayableVariables(CENTERRECT, polys, 0, wnd, true);
		drawPolys(polys, DBMath.MATID, vc, true, VectorText.TEXTTYPECELL, false);

		// add in anything "snuck" onto the cell
		List<VectorBase> addThesePolys = addPolyToCell.get(cell);
		if (addThesePolys != null)
		{
			for(VectorBase vb : addThesePolys)
				vc.shapes.add(vb);
		}
		List<VectorSubCell> addTheseInsts = addInstToCell.get(cell);
		if (addTheseInsts != null)
		{
			for(VectorSubCell vsc : addTheseInsts)
				vc.subCells.add(vsc);
		}

		// icon cells should not get greeked because of their contents
		if (cell.getView() == View.ICON) vc.maxFeatureSize = 0;

		return vc;
	}

	/**
	 * Comparator class for sorting VectorBase objects by their layer depth.
	 */
    public static class ShapeByLayer implements Comparator<VectorBase>
    {
		/**
		 * Method to sort Objects by their string name.
		 */
/*5*/   public int compare(VectorBase vb1, VectorBase vb2)
//4*/   public int compare(Object o1, Object o2)
        {
//4*/		VectorBase vb1 = (VectorBase)o1;
//4*/		VectorBase vb2 = (VectorBase)o2;
			int level1 = 1000, level2 = 1000;
			if (vb1.layer != null) level1 = vb1.layer.getFunction().getLevel();
			if (vb2.layer != null) level2 = vb2.layer.getFunction().getLevel();
            return level1 - level2;
        }
    }

	/**
	 * Method to cache a NodeInst.
	 * @param ni the NodeInst to cache.
     * @param trans the transformation of the NodeInst to the parent Cell.
	 * @param vc the cached cell in which to place the NodeInst.
     */
	public void drawNode(NodeInst ni, AffineTransform trans, VarContext context, VectorCell vc)
	{
		NodeProto np = ni.getProto();
		AffineTransform localTrans = ni.rotateOut(trans);
		boolean hideOnLowLevel = false;
		if (ni.isVisInside() || np == Generic.tech.cellCenterNode)
			hideOnLowLevel = true;

		// draw the node
		if (ni.isCellInstance())
		{
			// cell instance
			Cell subCell = (Cell)np;

			// compute the outline
			AffineTransform outlineTrans = ni.translateOut(localTrans);
			Rectangle2D cellBounds = subCell.getBounds();
			Poly outlinePoly = new Poly(cellBounds);
			outlinePoly.transform(outlineTrans);

			// record a call to the instance
			Point2D ctrShift = new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY());
			localTrans.transform(ctrShift, ctrShift);
			VectorSubCell vsc = new VectorSubCell(ni, ctrShift, outlinePoly.getPoints());
			vc.subCells.add(vsc);

			showCellPorts(ni, vc, vsc, localTrans);

			// draw any displayable variables on the instance
			int numPolys = ni.numDisplayableVariables(true);
			Poly [] polys = new Poly[numPolys];
			Rectangle2D rect = ni.getUntransformedBounds();
			ni.addDisplayableVariables(rect, polys, 0, wnd, true);
			drawPolys(polys, localTrans, vc, false, VectorText.TEXTTYPENODE, false);
		} else
		{
			// primitive: save it
			PrimitiveNode prim = (PrimitiveNode)np;
			int textType = VectorText.TEXTTYPENODE;
			if (prim == Generic.tech.invisiblePinNode) textType = VectorText.TEXTTYPEANNOTATION;
			Technology tech = prim.getTechnology();
			Poly [] polys = tech.getShapeOfNode(ni, wnd, context, false, false, null);
			boolean pureLayer = (ni.getFunction() == PrimitiveNode.Function.NODE);
			drawPolys(polys, localTrans, vc, hideOnLowLevel, textType, pureLayer);
		}

		// draw any exports from the node
		Iterator<Export> it = ni.getExports();
		while (it.hasNext())
		{
			Export e = it.next();
			Poly poly = e.getNamePoly();
			Rectangle2D rect = (Rectangle2D)poly.getBounds2D().clone();
			TextDescriptor descript = poly.getTextDescriptor();
			Poly.Type style = descript.getPos().getPolyType();
			style = Poly.rotateType(style, ni);
			VectorText vt = new VectorText(poly.getBounds2D(), style, descript, null, VectorText.TEXTTYPEEXPORT, null, e,
				true, null, null);
			vc.shapes.add(vt);

			// draw variables on the export
			int numPolys = e.numDisplayableVariables(true);
			if (numPolys > 0)
			{
				Poly [] polys = new Poly[numPolys];
				e.addDisplayableVariables(rect, polys, 0, wnd, true);
				drawPolys(polys, trans, vc, true, VectorText.TEXTTYPEEXPORT, false);
//				drawPolys(polys, localTrans, vc, true, VectorText.TEXTTYPEEXPORT, false);
			}
		}
	}

	/**
	 * Method to cache an ArcInst.
	 * @param ai the ArcInst to cache.
     * @param trans the transformation of the ArcInst to the parent cell.
	 * @param vc the cached cell in which to place the ArcInst.
     */
	public void drawArc(ArcInst ai, AffineTransform trans, VectorCell vc)
	{
		// if the arc is tiny, just approximate it with a single dot
		Rectangle2D arcBounds = ai.getBounds();

        // see if the arc is completely clipped from the screen
        Rectangle2D dbBounds = new Rectangle2D.Double(arcBounds.getX(), arcBounds.getY(), arcBounds.getWidth(), arcBounds.getHeight());
        Poly p = new Poly(dbBounds);
        p.transform(trans);
        dbBounds = p.getBounds2D();

		// draw the arc
		ArcProto ap = ai.getProto();
		Technology tech = ap.getTechnology();
		Poly [] polys = tech.getShapeOfArc(ai, wnd);
		drawPolys(polys, trans, vc, false, VectorText.TEXTTYPEARC, false);
	}

	/**
	 * Method to cache the exports on a NodeInst.
	 * @param ni the NodeInst with exports
	 * @param col the color to use.
	 * @param vc the cached cell in which to place the information.
	 * @param vsc the cached subcell reference that defines the NodeInst.
	 * @param localTrans the transformation of the port locations.
	 */
	private void showCellPorts(NodeInst ni, VectorCell vc, VectorSubCell vsc, AffineTransform localTrans)
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

		// because "getShapeOfPort" includes the local rotation, it must be undone
		AffineTransform thisTrans = ni.rotateIn();
		for(int i = 0; i < numPorts; i++)
		{
			if (shownPorts[i]) continue;
			Export pp = (Export)ni.getProto().getPort(i);

			Poly portPoly = ni.getShapeOfPort(pp);
			if (portPoly == null) continue;

			// undo local rotation and add total transformation instead
			portPoly.transform(thisTrans);
			portPoly.transform(localTrans);

			TextDescriptor descript = portPoly.getTextDescriptor();
			MutableTextDescriptor portDescript = pp.getMutableTextDescriptor(Export.EXPORT_NAME);
			Poly.Type style = Poly.Type.FILLED;
			if (descript != null)
			{
				portDescript.setColorIndex(descript.getColorIndex());
				style = descript.getPos().getPolyType();
			}
			Rectangle rect = new Rectangle(tempPt1);
			VectorText vt = new VectorText(portPoly.getBounds2D(), style, descript, null, VectorText.TEXTTYPEPORT, ni, pp,
				false, null, null);
			vsc.portShapes.add(vt);
		}
	}

	/**
	 * Method to cache an array of polygons.
	 * @param polys the array of polygons to cache.
	 * @param trans the transformation to apply to each polygon.
	 * @param vc the cached cell in which to place the polygons.
	 * @param hideOnLowLevel true if the polygons should be marked such that they are not visible on lower levels of hierarchy.
	 * @param pureLayer true if these polygons come from a pure layer node.
	 */
	private void drawPolys(Poly[] polys, AffineTransform trans, VectorCell vc, boolean hideOnLowLevel, int textType, boolean pureLayer)
	{
		if (polys == null) return;
		for(int i = 0; i < polys.length; i++)
		{
			// get the polygon and transform it
			Poly poly = polys[i];
			if (poly == null) continue;

			// transform the bounds
			poly.transform(trans);

			// render the polygon
			renderPoly(poly, vc, hideOnLowLevel, textType, pureLayer);
		}
	}

	/**
	 * Method to cache a Poly.
	 * @param poly the polygon to cache.
	 * @param vc the cached cell in which to place the polygon.
	 * @param hideOnLowLevel true if the polygon should be marked such that it is not visible on lower levels of hierarchy.
	 * @param pureLayer true if the polygon comes from a pure layer node.
	 */
	private void renderPoly(Poly poly, VectorCell vc, boolean hideOnLowLevel, int textType, boolean pureLayer)
	{
		// now draw it
		Point2D [] points = poly.getPoints();
		Layer layer = poly.getLayer();
		EGraphics graphics = null;
		if (layer != null)
			graphics = layer.getGraphics();
		Poly.Type style = poly.getStyle();
		if (style == Poly.Type.FILLED)
		{
			Rectangle2D bounds = poly.getBox();
			if (bounds != null)
			{
				// convert coordinates
				float lX = (float)bounds.getMinX();
				float hX = (float)bounds.getMaxX();
				float lY = (float)bounds.getMinY();
				float hY = (float)bounds.getMaxY();
				VectorManhattan vm = new VectorManhattan(lX, lY, hX, hY, layer, graphics, hideOnLowLevel);
				if (layer != null)
				{
					Layer.Function fun = layer.getFunction();
					if (!pureLayer && (fun.isImplant() || fun.isSubstrate()))
					{
						float dX = hX - lX;
						float dY = hY - lY;

						// well and substrate layers are made smaller so that they "greek" sooner
						vm.setSize(dX / 10, dY / 10);
//						vm.setSize(dX / 2, dY / 2);
//						this.size = (float)Math.min(ni.getXSize(), ni.getYSize());
					}
				}
				vm.hideOnLowLevel = hideOnLowLevel;
				vc.shapes.add(vm);
				vc.maxFeatureSize = Math.max(vc.maxFeatureSize, vm.minSize);
				return;
			}
			VectorPolygon vp = new VectorPolygon(points, layer, graphics, hideOnLowLevel);
			vc.shapes.add(vp);
			return;
		}
		if (style == Poly.Type.CROSSED)
		{
			VectorLine vl1 = new VectorLine(points[0].getX(), points[0].getY(),
				points[1].getX(), points[1].getY(), 0, layer, graphics, hideOnLowLevel);
			VectorLine vl2 = new VectorLine(points[1].getX(), points[1].getY(),
				points[2].getX(), points[2].getY(), 0, layer, graphics, hideOnLowLevel);
			VectorLine vl3 = new VectorLine(points[2].getX(), points[2].getY(),
				points[3].getX(), points[3].getY(), 0, layer, graphics, hideOnLowLevel);
			VectorLine vl4 = new VectorLine(points[3].getX(), points[3].getY(),
				points[0].getX(), points[0].getY(), 0, layer, graphics, hideOnLowLevel);
			VectorLine vl5 = new VectorLine(points[0].getX(), points[0].getY(),
				points[2].getX(), points[2].getY(), 0, layer, graphics, hideOnLowLevel);
			VectorLine vl6 = new VectorLine(points[1].getX(), points[1].getY(),
				points[3].getX(), points[3].getY(), 0, layer, graphics, hideOnLowLevel);
			vc.shapes.add(vl1);
			vc.shapes.add(vl2);
			vc.shapes.add(vl3);
			vc.shapes.add(vl4);
			vc.shapes.add(vl5);
			vc.shapes.add(vl6);
			return;
		}
		if (style.isText())
		{
			Rectangle2D bounds = poly.getBounds2D();
			TextDescriptor descript = poly.getTextDescriptor();
			String str = poly.getString();
			VectorText vt = new VectorText(bounds, style, descript, str, textType, null, null,
				hideOnLowLevel, layer, graphics);
			vc.shapes.add(vt);
			vc.maxFeatureSize = Math.max(vc.maxFeatureSize, vt.height);
			return;
		}
		if (style == Poly.Type.CLOSED || style == Poly.Type.OPENED || style == Poly.Type.OPENEDT1 ||
			style == Poly.Type.OPENEDT2 || style == Poly.Type.OPENEDT3)
		{
			int lineType = 0;
			if (style == Poly.Type.OPENEDT1) lineType = 1; else
			if (style == Poly.Type.OPENEDT2) lineType = 2; else
			if (style == Poly.Type.OPENEDT3) lineType = 3;

			for(int j=1; j<points.length; j++)
			{
				Point2D oldPt = points[j-1];
				Point2D newPt = points[j];
				VectorLine vl = new VectorLine(oldPt.getX(), oldPt.getY(),
					newPt.getX(), newPt.getY(), lineType, layer, graphics, hideOnLowLevel);
				vc.shapes.add(vl);
			}
			if (style == Poly.Type.CLOSED)
			{
				Point2D oldPt = points[points.length-1];
				Point2D newPt = points[0];
				VectorLine vl = new VectorLine(oldPt.getX(), oldPt.getY(),
					newPt.getX(), newPt.getY(), lineType, layer, graphics, hideOnLowLevel);
				vc.shapes.add(vl);
			}
			return;
		}
		if (style == Poly.Type.VECTORS)
		{
			for(int j=0; j<points.length; j+=2)
			{
				Point2D oldPt = points[j];
				Point2D newPt = points[j+1];
				VectorLine vl = new VectorLine(oldPt.getX(), oldPt.getY(),
					newPt.getX(), newPt.getY(), 0, layer, graphics, hideOnLowLevel);
				vc.shapes.add(vl);
			}
			return;
		}
		if (style == Poly.Type.CIRCLE)
		{
			VectorCircle vci = new VectorCircle(points[0].getX(), points[0].getY(),
				points[1].getX(), points[1].getY(), 0, layer, graphics, hideOnLowLevel);
			vc.shapes.add(vci);
			return;
		}
		if (style == Poly.Type.THICKCIRCLE)
		{
			VectorCircle vci = new VectorCircle(points[0].getX(), points[0].getY(),
				points[1].getX(), points[1].getY(), 1, layer, graphics, hideOnLowLevel);
			vc.shapes.add(vci);
			return;
		}
		if (style == Poly.Type.DISC)
		{
			VectorCircle vci = new VectorCircle(points[0].getX(), points[0].getY(), points[1].getX(),
				points[1].getY(), 2, layer, graphics, hideOnLowLevel);
			vc.shapes.add(vci);
			return;
		}
		if (style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC)
		{
			VectorCircleArc vca = new VectorCircleArc(points[0].getX(), points[0].getY(),
				points[1].getX(), points[1].getY(), points[2].getX(), points[2].getY(),
				style == Poly.Type.THICKCIRCLEARC, layer, graphics, hideOnLowLevel);
			vc.shapes.add(vca);
			return;
		}
		if (style == Poly.Type.CROSS)
		{
			// draw the cross
			VectorCross vcr = new VectorCross(points[0].getX(), points[0].getY(), true, layer, graphics, hideOnLowLevel);
			vc.shapes.add(vcr);
			return;
		}
		if (style == Poly.Type.BIGCROSS)
		{
			// draw the big cross
			VectorCross vcr = new VectorCross(points[0].getX(), points[0].getY(), false, layer, graphics, hideOnLowLevel);
			vc.shapes.add(vcr);
			return;
		}
	}

	/**
	 * Method to construct a string that describes an orientation.
	 * This method is used instead of "Orientation.toString" because
	 * it uses the old C style method that isn't redundant.
	 * @param orient the orientation.
	 * @return a unique string describing it.
	 */
	private static String makeOrientationName(Orientation orient)
	{
		String oName = Integer.toString(orient.getCAngle());
		if (orient.isCTranspose()) oName += "T";
		return oName;
	}

}
