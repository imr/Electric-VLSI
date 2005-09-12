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

import com.sun.electric.database.CellUsage;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.GenMath.MutableDouble;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.TextDescriptor.Size;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.user.User;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * To Do:
 *    Down-in-place editing
 *    Size computation and elimination on all cached objects
 *    No recache when port/export display is changed
 * Can't do:
 *    Context is lost when caching (was when doing pixel caching, too)
 */
public class VectorDrawing
{
	private static final boolean STATS = true;

	/** the EditWindow being drawn */						private EditWindow wnd;
	/** statistics */										private int boxCount, tinyBoxCount, lineCount, polygonCount;
	/** statistics */										private int crossCount, textCount, circleCount, arcCount;
	/** statistics */										private int subCellCount, tinySubCellCount;
	/** the threshold of object sizes */					private double maxObjectSize;
	/** temporary objects (saves allocation) */				private Point tempPt1 = new Point(), tempPt2 = new Point();
	/** temporary objects (saves allocation) */				private Point tempPt3 = new Point(), tempPt4 = new Point();
	/** temporary object (saves allocation) */				private Rectangle tempRect = new Rectangle();
	/** list of cell expansions. */							private static HashMap cachedCells = new HashMap();
	/** zero rectangle */									private static final Rectangle2D CENTERRECT = new Rectangle2D.Double(0, 0, 0, 0);
	/** no rotation or mirroring */							private static final Orientation NOROTATION = Orientation.fromJava(0, false, false);
	private static EGraphics textGraphics = new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 0,0,0, 1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
	private static EGraphics instanceGraphics = new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 0,0,0, 1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
	private static EGraphics portGraphics = new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 255,0,0, 1.0,true,
		new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});

	/**
	 * Class which defines the common information for all cached displayable objects
	 */
	private static class VectorBase
	{
		Layer layer;
		EGraphics graphics;
		boolean hideOnLowLevel;
		double minSize, maxSize;

		VectorBase(Layer layer, EGraphics graphics, boolean hideOnLowLevel)
		{
			this.layer = layer;
			this.graphics = graphics;
			this.hideOnLowLevel = hideOnLowLevel;
			minSize = maxSize = -1;
			hideOnLowLevel = false;
		}

		void setSize(double size1, double size2)
		{
			this.minSize = Math.min(size1, size2);
			this.maxSize = Math.max(size1, size2);
		}
	}

	/**
	 * Class which defines a cached Manhattan rectangle.
	 */
	private static class VectorManhattan extends VectorBase
	{
		double c1X, c1Y, c2X, c2Y;
		int tinyColor;

		VectorManhattan(double c1X, double c1Y, double c2X, double c2Y, Layer layer, EGraphics graphics, boolean hideOnLowLevel)
		{
			super(layer, graphics, hideOnLowLevel);
			this.c1X = c1X;
			this.c1Y = c1Y;
			this.c2X = c2X;
			this.c2Y = c2Y;
			double dX = Math.abs(c1X - c2X);
			double dY = Math.abs(c1Y - c2Y);
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
		double fX, fY, tX, tY;
		int texture;

		VectorLine(double fX, double fY, double tX, double tY, int texture, Layer layer, EGraphics graphics, boolean hideOnLowLevel)
		{
			super(layer, graphics, hideOnLowLevel);
			this.fX = fX;
			this.fY = fY;
			this.tX = tX;
			this.tY = tY;
			this.texture = texture;
		}
	}

	/**
	 * Class which defines a cached circle (filled, opened, or thick).
	 */
	private static class VectorCircle extends VectorBase
	{
		double cX, cY, eX, eY;
		int nature;

		VectorCircle(double cX, double cY, double eX, double eY, int nature, Layer layer, EGraphics graphics, boolean hideOnLowLevel)
		{
			super(layer, graphics, hideOnLowLevel);
			this.cX = cX;
			this.cY = cY;
			this.eX = eX;
			this.eY = eY;
			this.nature = nature;
		}
	}

	/**
	 * Class which defines a cached arc of a circle (normal or thick).
	 */
	private static class VectorCircleArc extends VectorBase
	{
		double cX, cY, eX1, eY1, eX2, eY2;
		boolean thick;

		VectorCircleArc(double cX, double cY, double eX1, double eY1, double eX2, double eY2, boolean thick,
			Layer layer, EGraphics graphics, boolean hideOnLowLevel)
		{
			super(layer, graphics, hideOnLowLevel);
			this.cX = cX;
			this.cY = cY;
			this.eX1 = eX1;
			this.eY1 = eY1;
			this.eX2 = eX2;
			this.eY2 = eY2;
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
		/** the text height (in display units) */		double height;
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
				if (!tds.isAbsolute()) height = tds.getSize();
			}
		}
	}

	/**
	 * Class which defines a cached cross (a dot, large or small).
	 */
	private static class VectorCross extends VectorBase
	{
		double x, y;
		boolean small;

		VectorCross(double x, double y, boolean small, Layer layer, EGraphics graphics, boolean hideOnLowLevel)
		{
			super(layer, graphics, hideOnLowLevel);
			this.x = x;
			this.y = y;
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
		double offsetX, offsetY;
		Point2D [] outlinePoints;
		double size;
		List portShapes;

		VectorSubCell(NodeInst ni, Point2D offset, Point2D [] outlinePoints)
		{
			this.ni = ni;
			pureRotate = ni.getOrient();
			subCell = (Cell)ni.getProto();
			this.offsetX = offset.getX();
			this.offsetY = offset.getY();
			this.outlinePoints = outlinePoints;
			this.size = Math.min(ni.getXSize(), ni.getYSize());
			portShapes = new ArrayList();
		}
	}

	/**
	 * Class which holds the cell caches for a given cell.
	 * Since each cell is cached many times, once for every orientation on the screen,
	 * this object can hold many cell caches.
	 */
	static class VectorCellGroup
	{
		Cell cell;
		HashMap orientations;
		VectorCell any;

		VectorCellGroup(Cell cell)
		{
			this.cell = cell;
			orientations = new HashMap();
			any = null;
		}

		static VectorCellGroup findCellGroup(Cell cell)
		{
			VectorCellGroup vcg = (VectorCellGroup)cachedCells.get(cell);
			if (vcg == null)
			{
				vcg = new VectorCellGroup(cell);
				cachedCells.put(cell, vcg);
			}
			return vcg;
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
	 * Class which defines a cached cell in a single orientation.
	 */
	static class VectorCell
	{
		List shapes;
		List subCells;
		boolean hasFadeColor;
		int fadeColor;
		double maxFeatureSize;

		VectorCell()
		{
			shapes = new ArrayList();
			subCells = new ArrayList();
			hasFadeColor = false;
			maxFeatureSize = 0;
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
	void render(Cell cell, boolean fullInstantiate, Rectangle2D drawLimitBounds)
	{
		// set colors to use
		textGraphics.setColor(new Color(User.getColorText()));
		instanceGraphics.setColor(new Color(User.getColorInstanceOutline()));

		// see if any layers are being highlighted/dimmed
		wnd.getOffscreen().highlightingLayers = false;
		for(Iterator it = Technology.getCurrent().getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			if (layer.isDimmed())
			{
				wnd.getOffscreen().highlightingLayers = true;
				break;
			}
		}

		// set size limit
		maxObjectSize = 3 / wnd.getScale();

		// statistics
		long startTime = 0;
		if (STATS)
		{
			boxCount = tinyBoxCount = lineCount = polygonCount = 0;
			crossCount = textCount = circleCount = arcCount = 0;
			subCellCount = tinySubCellCount = 0;
			startTime = System.currentTimeMillis();
		}

		// make sure the top level is cached
		VectorCell topVC = drawCell(cell, NOROTATION);

		// draw recursively
		Rectangle screenLimit = null;
		if (drawLimitBounds != null) screenLimit = wnd.databaseToScreen(drawLimitBounds);
		render(topVC, 0, 0, fullInstantiate, NOROTATION, true, screenLimit);

		if (STATS)
		{
			long renderTime = System.currentTimeMillis() - startTime;
	        System.out.println("Time to render: "+TextUtils.getElapsedTime(renderTime));
			System.out.println("   Rendered "+boxCount+" boxes ("+tinyBoxCount+" tiny), "+
				lineCount+" lines, "+polygonCount+" polys, "+crossCount+" crosses, "+
				textCount+" texts, "+circleCount+" circles, "+arcCount+" arcs, "+
				subCellCount+" subcells ("+tinySubCellCount+" tiny)");
		}
	}

	/**
	 * Method called when a cell changes: removes any cached displays of that cell
	 * @param cell the cell that changed
	 */
	public static void cellChanged(Cell cell)
	{
//System.out.println("REMOVING CACHE FOR CELL "+cell);
		cachedCells.remove(cell);
	}

	/**
	 * Method to recursively render a cached cell.
	 * @param vc the cached cell to render
	 * @param oX the X offset for rendering the cell (in database coordinates).
	 * @param oY the Y offset for rendering the cell (in database coordinates).
	 * @param fullInstantiate true to fully-instantiate circuitry (for "peeking").
	 * @param trans the orientation of the cell (this is not a transformation matrix with offsets, just an Orientation with rotation).
	 * @param topLevel true if this is the top level cell in the window.
	 */
	private void render(VectorCell vc, double oX, double oY, boolean fullInstantiate, Orientation trans, boolean topLevel, Rectangle screenLimit)
	{
		PixelDrawing offscreen = wnd.getOffscreen();
		Dimension sz = offscreen.getSize();

		// render main list of shapes
		drawList(oX, oY, vc.shapes, topLevel, screenLimit);

		// now render subcells
		for(Iterator it = vc.subCells.iterator(); it.hasNext(); )
		{
			VectorSubCell vsc = (VectorSubCell)it.next();
			subCellCount++;

			// get instance location
			double p1x = vsc.outlinePoints[0].getX() + oX;
			double p2x = vsc.outlinePoints[1].getX() + oX;
			double p3x = vsc.outlinePoints[2].getX() + oX;
			double p4x = vsc.outlinePoints[3].getX() + oX;
			double p1y = vsc.outlinePoints[0].getY() + oY;
			double p2y = vsc.outlinePoints[1].getY() + oY;
			double p3y = vsc.outlinePoints[2].getY() + oY;
			double p4y = vsc.outlinePoints[3].getY() + oY;
			wnd.databaseToScreen(p1x, p1y, tempPt1);   wnd.databaseToScreen(p2x, p2y, tempPt2);
			wnd.databaseToScreen(p3x, p3y, tempPt3);   wnd.databaseToScreen(p4x, p4y, tempPt4);
			int lX = Math.min(Math.min(tempPt1.x, tempPt2.x), Math.min(tempPt3.x, tempPt4.x));
			int hX = Math.max(Math.max(tempPt1.x, tempPt2.x), Math.max(tempPt3.x, tempPt4.x));
			int lY = Math.min(Math.min(tempPt1.y, tempPt2.y), Math.min(tempPt3.y, tempPt4.y));
			int hY = Math.max(Math.max(tempPt1.y, tempPt2.y), Math.max(tempPt3.y, tempPt4.y));

			// see if it is outside of a peek area
			if (screenLimit != null)
			{
				if (hX < screenLimit.x || lX >= screenLimit.x+screenLimit.width) continue;
				if (hY < screenLimit.y || lY >= screenLimit.y+screenLimit.height) continue;
			}

			// see if the subcell is clipped
			if (hX < 0 || lX >= sz.width) continue;
			if (hY < 0 || lY >= sz.height) continue;

			// see if the cell is too tiny to draw
			if (vsc.size < maxObjectSize)
			{
				VectorCellGroup vcg = VectorCellGroup.findCellGroup(vsc.subCell);
				VectorCell subVC = vcg.getAnyCell();
				if (subVC == null)
					subVC = drawCell(vsc.subCell, NOROTATION);
				int fadeColor = getFadeColor(subVC);
				drawTinyBox(lX, hX, lY, hY, fadeColor, offscreen);
				tinySubCellCount++;
				continue;
			}

			boolean expanded = vsc.ni.isExpanded();
			if (fullInstantiate) expanded = true;

//			// if not expanded, but viewing this cell in-place, expand it
//			if (!expanded)
//			{
//				List path = wnd.getInPlaceEditNodePath();
//				if (path != null)
//				{
//					for(int pathIndex=0; pathIndex<path.size(); pathIndex++)
//					{
//						NodeInst niOnPath = (NodeInst)path.get(pathIndex);
//						if (niOnPath.getProto() == subCell)
//						{
//							expanded = true;
//							break;
//						}
//					}
//				}
//			}

			if (expanded)
			{
				Orientation thisOrient = vsc.ni.getOrient();
				Orientation recurseTrans = thisOrient.concatenate(trans);
				VectorCell subVC = drawCell(vsc.subCell, recurseTrans);

				// may also be "tiny" if all features in the cell are tiny
				if (subVC.maxFeatureSize > 0 && subVC.maxFeatureSize < maxObjectSize)
				{
					boolean allTinyInside = isContentsTiny(vsc.subCell, subVC, recurseTrans, fullInstantiate);
					if (allTinyInside)
					{
						int fadeColor = getFadeColor(subVC);
						drawTinyBox(lX, hX, lY, hY, fadeColor, offscreen);
						tinySubCellCount++;
						continue;
					}
				}
				render(subVC, vsc.offsetX + oX, vsc.offsetY + oY, fullInstantiate, recurseTrans, false, screenLimit);
			} else
			{
				// now draw with the proper line type
				wnd.databaseToScreen(p1x, p1y, tempPt1);   wnd.databaseToScreen(p2x, p2y, tempPt2);
				offscreen.drawLine(tempPt1, tempPt2, null, instanceGraphics, 0, false);
				wnd.databaseToScreen(p2x, p2y, tempPt1);   wnd.databaseToScreen(p3x, p3y, tempPt2);
				offscreen.drawLine(tempPt1, tempPt2, null, instanceGraphics, 0, false);
				wnd.databaseToScreen(p3x, p3y, tempPt1);   wnd.databaseToScreen(p4x, p4y, tempPt2);
				offscreen.drawLine(tempPt1, tempPt2, null, instanceGraphics, 0, false);
				wnd.databaseToScreen(p1x, p1y, tempPt1);   wnd.databaseToScreen(p4x, p4y, tempPt2);
				offscreen.drawLine(tempPt1, tempPt2, null, instanceGraphics, 0, false);

				// draw the instance name
				if (User.isTextVisibilityOnInstance())
				{
					tempRect.setRect(lX, lY, hX-lX, hY-lY);
					TextDescriptor descript = vsc.ni.getTextDescriptor(NodeInst.NODE_PROTO_TD);
					NodeProto np = vsc.ni.getProto();
					offscreen.drawText(tempRect, Poly.Type.TEXTBOX, descript, np.describe(false), null, textGraphics, false);
				}
			}
			drawList(oX, oY, vsc.portShapes, topLevel, screenLimit);
		}
	}

	/**
	 * Method to draw a list of cached shapes.
	 * @param oX the X offset to draw the shapes (in display coordinates).
	 * @param oY the Y offset to draw the shapes (in display coordinates).
	 * @param shapes the List of shapes (VectorBase objects).
	 * @param topLevel true if this is the top cell in the window.
	 */
	private void drawList(double oX, double oY, List shapes, boolean topLevel, Rectangle screenLimit)
	{
		PixelDrawing offscreen = wnd.getOffscreen();
		Dimension sz = offscreen.getSize();

		// render all shapes
		for(Iterator it = shapes.iterator(); it.hasNext(); )
		{
			VectorBase vb = (VectorBase)it.next();
			if (vb.hideOnLowLevel && !topLevel) continue;

			// get visual characteristics of shape
			Layer layer = vb.layer;
			boolean dimmed = false;
			byte [][] layerBitMap = null;
			if (layer != null)
			{
//				if (!forceVisible && !vb.layer.isVisible()) continue;
				if (!layer.isVisible()) continue;
				dimmed = layer.isDimmed();
			}
			EGraphics graphics = vb.graphics;
			if (graphics != null)
			{
				int layerNum = graphics.getTransparentLayer() - 1;
				if (layerNum < offscreen.numLayerBitMaps) layerBitMap = offscreen.getLayerBitMap(layerNum);
			}

			// handle each shape
			if (vb instanceof VectorManhattan)
			{
				boxCount++;
				VectorManhattan vm = (VectorManhattan)vb;

				if (vm.minSize < maxObjectSize)
				{
					tinyBoxCount++;
					int col = vm.tinyColor;
					if (col < 0) continue;
					if (vm.maxSize < maxObjectSize)
					{
						// both dimensions tiny: just draw a dot
						wnd.databaseToScreen(vm.c1X+oX, vm.c1Y+oY, tempPt1);
						int x = tempPt1.x;
						int y = tempPt1.y;

						// see if it is outside of a peek area
						if (screenLimit != null)
						{
							if (x < screenLimit.x || x >= screenLimit.x+screenLimit.width) continue;
							if (y < screenLimit.y || y >= screenLimit.y+screenLimit.height) continue;
						}

						if (x < 0 || x >= sz.width) continue;
						if (y < 0 || y >= sz.height) continue;
						offscreen.drawPoint(x, y, null, col);
					} else
					{
						// one dimension tiny: draw a line
						wnd.databaseToScreen(vm.c1X+oX, vm.c1Y+oY, tempPt1);
						wnd.databaseToScreen(vm.c2X+oX, vm.c2Y+oY, tempPt2);
						int lX, hX, lY, hY;
						if (tempPt1.x < tempPt2.x) { lX = tempPt1.x;   hX = tempPt2.x; } else
							{ lX = tempPt2.x;   hX = tempPt1.x; }
						if (tempPt1.y < tempPt2.y) { lY = tempPt1.y;   hY = tempPt2.y; } else
							{ lY = tempPt2.y;   hY = tempPt1.y; }

						// see if it is outside of a peek area
						if (screenLimit != null)
						{
							if (hX < screenLimit.x || lX >= screenLimit.x+screenLimit.width) continue;
							if (hY < screenLimit.y || lY >= screenLimit.y+screenLimit.height) continue;
						}
						drawTinyBox(lX, hX, lY, hY, col, offscreen);
					}
					continue;
				}

				// determine coordinates of rectangle on the screen
				wnd.databaseToScreen(vm.c1X+oX, vm.c1Y+oY, tempPt1);
				wnd.databaseToScreen(vm.c2X+oX, vm.c2Y+oY, tempPt2);
				int lX, hX, lY, hY;
				if (tempPt1.x < tempPt2.x) { lX = tempPt1.x;   hX = tempPt2.x; } else
					{ lX = tempPt2.x;   hX = tempPt1.x; }
				if (hX < 0 || lX >= sz.width) continue;
				if (lX < 0) lX = 0;
				if (hX >= sz.width) hX = sz.width-1;

				if (tempPt1.y < tempPt2.y) { lY = tempPt1.y;   hY = tempPt2.y; } else
					{ lY = tempPt2.y;   hY = tempPt1.y; }
				if (hY < 0 || lY >= sz.height) continue;
				if (lY < 0) lY = 0;
				if (hY >= sz.height) hY = sz.height-1;

				// see if it is outside of a peek area
				if (screenLimit != null)
				{
					if (hX < screenLimit.x || lX >= screenLimit.x+screenLimit.width) continue;
					if (hY < screenLimit.y || lY >= screenLimit.y+screenLimit.height) continue;
				}

				// draw the box
				offscreen.drawBox(lX, hX, lY, hY, layerBitMap, graphics, dimmed);
			} else if (vb instanceof VectorLine)
			{
				lineCount++;
				VectorLine vl = (VectorLine)vb;

				// determine coordinates of line on the screen
				wnd.databaseToScreen(vl.fX+oX, vl.fY+oY, tempPt1);
				wnd.databaseToScreen(vl.tX+oX, vl.tY+oY, tempPt2);

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
					wnd.databaseToScreen(vp.points[i].getX()+oX, vp.points[i].getY()+oY, intPoints[i]);
	            }
				Point [] clippedPoints = GenMath.clipPoly(intPoints, 0, sz.width-1, 0, sz.height-1);
				offscreen.drawPolygon(clippedPoints, layerBitMap, graphics, dimmed);
			} else if (vb instanceof VectorCross)
			{
				crossCount++;
				VectorCross vcr = (VectorCross)vb;
				wnd.databaseToScreen(vcr.x+oX, vcr.y+oY, tempPt1);
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
				if (vt.height < maxObjectSize) continue;

				String drawString = vt.str;
				wnd.databaseToScreen(vt.bounds.getMinX()+oX, vt.bounds.getMinY()+oY, tempPt1);
				wnd.databaseToScreen(vt.bounds.getMaxX()+oX, vt.bounds.getMaxY()+oY, tempPt2);
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
					if (vt.ni.isExpanded()) portColor = Color.BLACK;
					portGraphics.setColor(portColor);
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
				wnd.databaseToScreen(vci.cX+oX, vci.cY+oY, tempPt1);
				wnd.databaseToScreen(vci.eX+oX, vci.eY+oY, tempPt2);
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
				wnd.databaseToScreen(vca.cX+oX, vca.cY+oY, tempPt1);
				wnd.databaseToScreen(vca.eX1+oX, vca.eY1+oY, tempPt2);
				wnd.databaseToScreen(vca.eX2+oX, vca.eY2+oY, tempPt3);
				offscreen.drawCircleArc(tempPt1, tempPt2, tempPt3, vca.thick, layerBitMap, graphics, dimmed);
			}
		}
	}

	/**
	 * Method to draw a tiny box on the screen in a given color.
	 * Done when the object is too small to draw in full detail.
	 * @param lX the low X coordinate of the box.
	 * @param hX the high X coordinate of the box.
	 * @param lY the low Y coordinate of the box.
	 * @param hY the high Y coordinate of the box.
	 * @param col the color to draw.
	 * @param offscreen the PixelDrawing object that does the drawing.
	 */
	private void drawTinyBox(int lX, int hX, int lY, int hY, int col, PixelDrawing offscreen)
	{
		Dimension sz = offscreen.getSize();
		if (lX < 0) lX = 0;
		if (hX >= sz.width) hX = sz.width-1;
		if (lY < 0) lY = 0;
		if (hY >= sz.height) hY = sz.height-1;
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
	 * @param fullInstantiate true to expand all subcells (for peeking).
	 * @return true if the cell has all tiny contents.
	 */
	private boolean isContentsTiny(Cell cell, VectorCell vc, Orientation trans, boolean fullInstantiate)
	{
		if (vc.maxFeatureSize > maxObjectSize) return false;
		boolean isAllTiny = true;
		for(Iterator it = vc.subCells.iterator(); it.hasNext(); )
		{
			VectorSubCell vsc = (VectorSubCell)it.next();
			boolean expanded = vsc.ni.isExpanded();
			if (fullInstantiate) expanded = true;
			if (expanded)
			{
				Orientation thisOrient = vsc.ni.getOrient();
				Orientation recurseTrans = thisOrient.concatenate(trans);
				VectorCell subVC = drawCell(vsc.subCell, recurseTrans);
				boolean subCellTiny = isContentsTiny(vsc.subCell, subVC, recurseTrans, fullInstantiate);
				if (!subCellTiny) return false;
				continue;
			}
			if (vsc.size > maxObjectSize) return false;
		}
		return true;
	}

	/**
	 * Method to determine the "fade" color for a cached cell.
	 * Fading is done when the cell is too tiny to draw (or all of its contents are too tiny).
	 * Instead of drawing the cell contents, the entire cell is painted with the "fade" color.
	 * @param vc the cached cell.
	 * @return the fade color (an integer with red/green/blue).
	 */
	private int getFadeColor(VectorCell vc)
	{
		if (vc.hasFadeColor) return vc.fadeColor;

		// examine all shapes
		HashMap layerAreas = new HashMap();
		gatherContents(vc, layerAreas);

		// now compute the color
		Set keys = layerAreas.keySet();
		double totalArea = 0;
		for(Iterator it = keys.iterator(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			MutableDouble md = (MutableDouble)layerAreas.get(layer);
			totalArea += md.doubleValue();
		}
		double r = 0, g = 0, b = 0;
		if (totalArea != 0)
		{
			for(Iterator it = keys.iterator(); it.hasNext(); )
			{
				Layer layer = (Layer)it.next();
				MutableDouble md = (MutableDouble)layerAreas.get(layer);
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
	private void gatherContents(VectorCell vc, HashMap layerAreas)
	{
		for(Iterator it = vc.shapes.iterator(); it.hasNext(); )
		{
			VectorBase vb = (VectorBase)it.next();
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
			MutableDouble md = (MutableDouble)layerAreas.get(layer);
			if (md == null)
			{
				md = new MutableDouble(0);
				layerAreas.put(layer, md);
			}
			md.setValue(md.doubleValue() + area);
		}

		for(Iterator it = vc.subCells.iterator(); it.hasNext(); )
		{
			VectorSubCell vsc = (VectorSubCell)it.next();
			VectorCellGroup vcg = VectorCellGroup.findCellGroup(vsc.subCell);
			VectorCell subVC = vcg.getAnyCell();
			if (subVC == null)
				subVC = drawCell(vsc.subCell, NOROTATION);
			gatherContents(subVC, layerAreas);
		}
	}

	// ************************************* CACHE CREATION *************************************

	/**
	 * Method to cache the contents of a cell.
	 * @param cell the Cell to cache
	 * @param prevTrans the orientation of the cell (just a rotation, no offsets here).
	 * @return a cached cell object for the given Cell.
	 */
	private VectorCell drawCell(Cell cell, Orientation prevTrans)
	{
		// see if this cell's vectors are cached
		VectorCellGroup vcg = VectorCellGroup.findCellGroup(cell);
		String orientationName = makeOrientationName(prevTrans);
		VectorCell vc = (VectorCell)vcg.orientations.get(orientationName);
		if (vc != null) return vc;
		vc = new VectorCell();
		vcg.addCell(vc, prevTrans);
		AffineTransform trans = prevTrans.pureRotate();

//System.out.println("CACHING CELL "+cell +" WITH ORIENTATION "+orientationName);
		// draw all arcs
		for(Iterator arcs = cell.getArcs(); arcs.hasNext(); )
		{
			ArcInst ai = (ArcInst)arcs.next();
			drawArc(ai, trans, vc);
		}

		// draw all nodes
		for(Iterator nodes = cell.getNodes(); nodes.hasNext(); )
		{
			NodeInst ni = (NodeInst)nodes.next();
			drawNode(ni, trans, vc);
		}

		// show cell variables
		int numPolys = cell.numDisplayableVariables(true);
		Poly [] polys = new Poly[numPolys];
		cell.addDisplayableVariables(CENTERRECT, polys, 0, wnd, true);
		drawPolys(polys, DBMath.MATID, vc, true, VectorText.TEXTTYPECELL);
		return vc;
	}

	/**
	 * Method to cache a NodeInst.
	 * @param ni the NodeInst to cache.
     * @param trans the transformation of the NodeInst to the parent Cell.
	 * @param vc the cached cell in which to place the NodeInst.
     */
	public void drawNode(NodeInst ni, AffineTransform trans, VectorCell vc)
	{
		NodeProto np = ni.getProto();
		AffineTransform localTrans = ni.rotateOut(trans);
		boolean hideOnLowLevel = false;
		if (ni.isVisInside() || np == Generic.tech.cellCenterNode)
			hideOnLowLevel = true;

		// draw the node
		if (np instanceof Cell)
		{
			// cell instance
			Cell subCell = (Cell)np;

			// compute the outline
			AffineTransform outlineTrans = ni.translateOut(localTrans);
			Rectangle2D cellBounds = subCell.getBounds();
			Poly outlinePoly = new Poly(cellBounds);
			outlinePoly.transform(outlineTrans);
			if (wnd.isInPlaceEdit()) outlinePoly.transform(wnd.getInPlaceTransformIn());

			// record a call to the instance
			Point2D ctrShift = new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY());
			localTrans.transform(ctrShift, ctrShift);
			VectorSubCell vsc = new VectorSubCell(ni, ctrShift, outlinePoly.getPoints());
			vc.subCells.add(vsc);

			showCellPorts(ni, vc, vsc);

			// draw any displayable variables on the instance
			int numPolys = ni.numDisplayableVariables(true);
			Poly [] polys = new Poly[numPolys];
			Rectangle2D rect = ni.getUntransformedBounds();
			ni.addDisplayableVariables(rect, polys, 0, wnd, true);
			drawPolys(polys, localTrans, vc, false, VectorText.TEXTTYPENODE);
		} else
		{
			// primitive: save it
			PrimitiveNode prim = (PrimitiveNode)np;
			int textType = VectorText.TEXTTYPENODE;
			if (prim == Generic.tech.invisiblePinNode) textType = VectorText.TEXTTYPEANNOTATION;
			Technology tech = prim.getTechnology();
			Poly [] polys = tech.getShapeOfNode(ni, wnd, VarContext.globalContext, false, false, null);
			drawPolys(polys, localTrans, vc, hideOnLowLevel, textType);
		}

		// draw any exports from the node
		Iterator it = ni.getExports();
		while (it.hasNext())
		{
			Export e = (Export)it.next();
			Poly poly = e.getNamePoly();
//			if (topWnd != null && topWnd.isInPlaceEdit())
//				poly.transform(topWnd.getInPlaceTransformOut());
			Rectangle2D rect = (Rectangle2D)poly.getBounds2D().clone();
			TextDescriptor descript = poly.getTextDescriptor();
			Poly.Type style = descript.getPos().getPolyType();
//			if (topWnd != null && topWnd.isInPlaceEdit())
//				poly.transform(topWnd.getInPlaceTransformOut());
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
				drawPolys(polys, localTrans, vc, true, VectorText.TEXTTYPEEXPORT);
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
		drawPolys(polys, trans, vc, false, VectorText.TEXTTYPEARC);
	}

	/**
	 * Method to cache the exports on a NodeInst.
	 * @param ni the NodeInst with exports
	 * @param col the color to use.
	 * @param vc the cached cell in which to place the information.
	 * @param vsc the cached subcell reference that defines the NodeInst.
	 */
	private void showCellPorts(NodeInst ni, VectorCell vc, VectorSubCell vsc)
	{
		// show the ports that are not further exported or connected
		int numPorts = ni.getProto().getNumPorts();
		boolean[] shownPorts = new boolean[numPorts];
		for(Iterator it = ni.getConnections(); it.hasNext();)
		{
			Connection con = (Connection) it.next();
			PortInst pi = con.getPortInst();
			shownPorts[pi.getPortIndex()] = true;
		}
		for(Iterator it = ni.getExports(); it.hasNext();)
		{
			Export exp = (Export) it.next();
			PortInst pi = exp.getOriginalPort();
			shownPorts[pi.getPortIndex()] = true;
		}
		for(int i = 0; i < numPorts; i++)
		{
			if (shownPorts[i]) continue;
			Export pp = (Export)ni.getProto().getPort(i);

			Poly portPoly = ni.getShapeOfPort(pp);
			if (portPoly == null) continue;
//			portPoly.transform(trans);
			TextDescriptor descript = portPoly.getTextDescriptor();
			MutableTextDescriptor portDescript = pp.getMutableTextDescriptor(Export.EXPORT_NAME_TD);
			portDescript.setColorIndex(descript.getColorIndex());
			Poly.Type style = descript.getPos().getPolyType();
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
	 */
	private void drawPolys(Poly[] polys, AffineTransform trans, VectorCell vc, boolean hideOnLowLevel, int textType)
	{
		if (polys == null) return;
		for(int i = 0; i < polys.length; i++)
		{
			// get the polygon and transform it
			Poly poly = polys[i];
			if (poly == null) continue;

			// transform the bounds
			poly.transform(trans);
			if (wnd.isInPlaceEdit()) poly.transform(wnd.getInPlaceTransformIn());

			// render the polygon
			renderPoly(poly, vc, hideOnLowLevel, textType);
		}
	}

	/**
	 * Method to cache a Poly.
	 * @param poly the polygon to cache.
	 * @param vc the cached cell in which to place the polygon.
	 * @param hideOnLowLevel true if the polygon should be marked such that it is not visible on lower levels of hierarchy.
	 */
	private void renderPoly(Poly poly, VectorCell vc, boolean hideOnLowLevel, int textType)
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
				double lX = bounds.getMinX();
				double hX = bounds.getMaxX();
				double lY = bounds.getMinY();
				double hY = bounds.getMaxY();
				VectorManhattan vm = new VectorManhattan(lX, lY, hX, hY, layer, graphics, hideOnLowLevel);
				if (layer != null)
				{
					Layer.Function fun = layer.getFunction();
					if (fun.isImplant() || fun.isSubstrate())
					{
						double dX = hX - lX;
						double dY = hY - lY;
						vm.setSize(dX / 10, dY / 10);
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
