/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PixelDrawing.java
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.CellId;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.Job;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.TextDescriptor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

/**
 * This class manages an offscreen display for an associated EditWindow.
 * It renders an Image for copying to the display.
 * <P>
 * Every offscreen display consists of two parts: the transparent layers and the opaque image.
 * To tell how a layer is displayed, look at the "transparentLayer" field of its "EGraphics" object.
 * When this is nonzero, the layer is drawn transparent.
 * When this is zero, use the "red, green, blue" fields for the opaque color.
 * <P>
 * The opaque image is a full-color Image that is the size of the EditWindow.
 * Any layers that are marked "opaque" are drawn in full color in the image.
 * Colors are not combined in the opaque image: every color placed in it overwrites the previous color.
 * For this reason, opaque colors are often stipple patterns, so that they won't completely obscure other
 * opaque layers.
 * <P>
 * The transparent layers are able to combine with each other.
 * Typically, the more popular layers are made transparent (metal, poly, active, etc.)
 * For every transparent layer, there is a 1-bit deep bitmap that is the size of the EditWindow.
 * The bitmap is an array of "byte []" pointers, one for every Y coordinate in the EditWindow.
 * Each array contains the bits for that row, packed 8 per byte.
 * All of this information is in the "layerBitMaps" field, which is triply indexed.
 * <P>
 * Thus, to find bit (x,y) of transparent layer T, first lookup the appropriate transparent layer,
 * ("layerBitMaps[T]").
 * Then, for that layer, find the array of bytes for the appropriate row
 * (by indexing the the Y coordinate into the rowstart array, "layerBitMaps[T][y]").
 * Next, figure out which byte has the bit (by dividing the X coordinate by 8: "layerBitMaps[T][y][x>>3]").
 * Finally, determine which bit to use (by using the low 3 bits of the X coordinate,
 * layerBitMaps[T][y][x>>3] & (1 << (x&7)) ).
 * <P>
 * Transparent layers are not allocated until needed.  Thus, if there are 5 possible transparent layers,
 * but only 2 are used, then only two bitplanes will be created.
 * <P>
 * Each technology declares the number of possible transparent layers that it can generate.
 * In addition, it must provide a color map for describing every combination of transparent layer.
 * This map is, of course, 2-to-the-number-of-possible-transparent-layers long.
 * <P>
 * The expected number of transparent layers is taken from the current technology.  If the user switches
 * the current technology, but draws something from a different technology, then the drawn circuitry
 * may make use of transparent layers that don't exist in the current technology.  In such a case,
 * the transparent request is made opaque.
 * <P>
 * When all rendering is done, the full-color image is composited with the transparent layers to produce
 * the final image.
 * This is done by scanning the full-color image for any entries that were not filled-in.
 * These are then replaced by the transparent color at that point.
 * The transparent color is computed by looking at the bits in every transparent bitmap and
 * constructing an index.  This is looked-up in the color table and the appropriate color is used.
 * If no transparent layers are set, the background color is used.
 * <P>
 * There are a number of efficiencies implemented here.
 * <UL>
 * <LI><B>Setting bits directly into the offscreen memory</B>.
 * Although Java's Swing package has a rendering model, it was found to be 3 times slower than
 * setting bits directly in the offscreen memory.</LI>
 * <LI><B>Tiny nodes and arcs are approximated</B>.
 * When a node or arc will be only 1 or 2 pixels in size on the screen, it is not necessary
 * to actually compute the edges of all of its parts.  Instead, a single pixel of color is placed.
 * The color is taken from all of the layers that compose the node or arc.
 * For arcs that are long but only 1 pixel wide, a line is drawn in the same manner.
 * This optimization adds another factor of 2 to the speed of display.</LI>
 * <LI><B>Expanded cell contents are cached</B>.
 * When a cell is expanded, and its contents is drawn, the contents are preserved so that they
 * need be rendered only once.  Subsequent instances of that expanded cell are able to be instantly drawn.
 * There are a number of extra considerations here:
 *   <UL>
 *   <LI>Cell instances can appear in any orientation.  Therefore, the cache of drawn cells must
 *   include the orientation.</LI>
 *   <LI>Cached cells are retained as long as the current scale is maintained.  But when zooming
 *   in and out, the cache is cleared.</LI>
 *   <LI>Cell instances may appear at different levels of the hierarchy, with different other circuitry over
 *   them.  For example, an instance may have been rendered at one level of hierarchy, and other items at that
 *   same level then rendered over it. It is then no longer possible to copy those bits when the instance
 *   appears again at another place in the hierarchy because it has been altered by neighboring circuitry.
 *   The same problem happens when cell instances overlap.  Therefore, it is necessary to render each expanded
 *   cell instance into its own offscreen map, with its own separate opaque and transparent layers (which allows
 *   it to be composited properly when re-instantiated).  Thus, a new PixelDrawing" object is created for each
 *   cached cell.</LI>
 *   <LI>Subpixel alignment may not be the same for each cached instance.  This turns out not to be
 *   a problem, because at such zoomed-out scales, it is impossible to see individual objects anyway.</LI>
 *   <LI>Large cell instances should not be cached.  When zoomed-in, an expanded cell instance could
 *   be many megabytes in size, and only a portion of it appears on the screen.  Therefore, large cell
 *   instances are not cached, but drawn directly.  It is assumed that there will be few such instances.
 *   The rule currently is that any cell whose width is greater than half of the display size AND whose
 *   height is greater than half of the display size is too large to cache.</LI>
 *   <LI>If an instance only appears once, it is not cached.  This requires a preprocessing step to scan
 *   the hierarchy and count the number of times that a particular cell-transformation is used.  During
 *   rendering, if the count is only 1, it is not cached.  The exception to this rule is if the screen
 *   is redisplayed without a change of magnification (during panning, for example).  In such a case,
 *   all cells will eventually be cached because, even those used once are being displayed with each redraw. </LI>
 *   <LI>Texture patterns don't line-up.  When drawing texture pattern to the final buffer, it is easy
 *   to use the screen coordinates to index the pattern map, causing all of them to line-up.
 *   Any two adjoining objects that use the same pattern will have their patterns line-up smoothly.
 *   However, when caching cell instances, it is not possible to know where the contents will be placed
 *   on the screen, and so the texture patterns rendered into the cache cannot be aligned globally.
 *   To solve this, there are additional bitmaps created for every Patterned-Opaque-Layer (POL).
 *   When rendering on a layer that is patterned and opaque, the bitmap is dynamically allocated
 *   and filled (all bits are filled on the bitmap, not just those in the pattern).
 *   When combining lower-level cell images with higher-level ones, these POLs are copied, too.
 *   When compositing at the top level, however, the POLs are converted back to patterns, so that they line-up.</LI>
 *   </UL>
 * </UL>
 * 
 */
class PixelDrawing
{
	/** Text smaller than this will not be drawn. */				public static final int MINIMUMTEXTSIZE =   5;
	/** Text larger than this is granular. */						public static final int MAXIMUMTEXTSIZE = 100;
	/** Number of singleton cells to cache when redisplaying. */	public static final int SINGLETONSTOADD =   5;

	private static class PolySeg
	{
		private int fx,fy, tx,ty, direction, increment;
		private PolySeg nextedge;
		private PolySeg nextactive;
	}

	// statistics stuff
	private static final boolean TAKE_STATS = true;
	private static int tinyCells, tinyPrims, totalCells, renderedCells, totalPrims, tinyArcs, linedArcs, totalArcs;
	private static int offscreensCreated, offscreenPixelsCreated, offscreensUsed, offscreenPixelsUsed, cellsRendered;

    private static final boolean DEBUGRENDERTIMING = false;
    private static long renderTextTime;
    private static long renderPolyTime;

    private static class ExpandedCellKey {
        private Cell cell;
        private Orientation orient;
     
        private ExpandedCellKey(Cell cell, Orientation orient) {
            this.cell = cell;
            this.orient = orient;
        }
        
        public boolean equals(Object obj) {
            if (obj instanceof ExpandedCellKey) {
                ExpandedCellKey that = (ExpandedCellKey)obj;
                return this.cell == that.cell && this.orient.equals(that.orient);
            }
            return false;
        }
        
        public int hashCode() { return cell.hashCode()^orient.hashCode(); }
    }
    
	/**
	 * This class holds information about expanded cell instances.
	 * For efficiency, Electric remembers the bits in an expanded cell instance
	 * and uses them when another expanded instance appears elsewhere.
	 * Of course, the orientation of the instance matters, so each combination of
	 * cell and orientation forms a "cell cache".  The Cell Cache is stored in the
	 * "wnd" field (which has its own PixelDrawing object).
	 */
	private static class ExpandedCellInfo
	{
		private boolean singleton;
		private int instanceCount;
        private boolean tooLarge;
		private PixelDrawing offscreen;

		ExpandedCellInfo()
		{
			singleton = true;
			offscreen = null;
		}
	}
    
	/** the size of the EditWindow */						private final Dimension sz;
    /** the scale of the EditWindow */                      private double scale;
    /** the VarContext of the EditWindow */                 private VarContext varContext;
    /** the X origin of the cell in display coordinates. */ private double originX;
    /** the Y origin of the cell in display coordinates. */ private double originY;
	/** 0: color display, 1: color printing, 2: B&W printing */	private int nowPrinting;
    
    /** the area of the cell to draw, in DB units */        private Rectangle2D drawBounds;
	/** whether any layers are highlighted/dimmed */		boolean highlightingLayers;
	/** true if the last display was a full-instantiate */	private boolean lastFullInstantiate = false;
	/** A List of NodeInsts to the cell being in-place edited. */private List<NodeInst> inPlaceNodePath;
	/** true if text can be drawn (not too zoomed-out) */	private boolean canDrawText;
	/** maximum size before an object is too small */		private static double maxObjectSize;
	/** half of maximum object size */						private static double halfMaxObjectSize;
	/** temporary objects (saves reallocation) */			private final Point tempPt1 = new Point(), tempPt2 = new Point();
	/** temporary objects (saves reallocation) */			private final Point tempPt3 = new Point(), tempPt4 = new Point();

	// the full-depth image
    /** the offscreen opaque image of the window */			private final BufferedImage img;
	/** opaque layer of the window */						private final int [] opaqueData;
	/** size of the opaque layer of the window */			private final int total;
	/** the background color of the offscreen image */		private int backgroundColor;
	/** the "unset" color of the offscreen image */			private int backgroundValue;

	// the transparent bitmaps
	/** the offscreen maps for transparent layers */		private byte [][][] layerBitMaps;
	/** row pointers for transparent layers */				private byte [][] compositeRows;
	/** the number of transparent layers */					int numLayerBitMaps;
	/** the number of bytes per row in offscreen maps */	private final int numBytesPerRow;
	/** the number of offscreen transparent maps made */	private int numLayerBitMapsCreated;
	/** the technology of the window */						private Technology curTech;
    /** ERasters for layers of current technology. */       private ERaster[] curTechLayers;

	// the patterned opaque bitmaps
    private static class PatternedOpaqueLayer {
        private byte[][] layerBitMap;
        PatternedOpaqueLayer(int height, int numBytesPerRow) { layerBitMap = new byte[height][numBytesPerRow]; }
    }
	/** the map from layers to Patterned Opaque bitmaps */	private HashMap<Layer,PatternedOpaqueLayer> patternedOpaqueLayers = new HashMap<Layer,PatternedOpaqueLayer>();
	/** the top-level window being rendered */				private boolean renderedWindow;

	/** whether to occasionally update the display. */		private boolean periodicRefresh;
	/** keeps track of when to update the display. */		private int objectCount;
	/** keeps track of when to update the display. */		private long lastRefreshTime;
	/** the EditWindow being drawn */						private EditWindow wnd;

	/** the size of the top-level EditWindow */				private static Dimension topSz;
	/** the last Technology that had transparent layers */	private static Technology techWithLayers = null;
	/** list of cell expansions. */							private static HashMap<ExpandedCellKey,ExpandedCellInfo> expandedCells = null;
    /** Set of changed cells. */                            private static final HashSet<CellId> changedCells = new HashSet<CellId>();
	/** scale of cell expansions. */						private static double expandedScale = 0;
	/** number of extra cells to render this time */		private static int numberToReconcile;
	/** TextDescriptor for empty window text. */			private static TextDescriptor noCellTextDescriptor = null;
	/** zero rectangle */									private static final Rectangle2D CENTERRECT = new Rectangle2D.Double(0, 0, 0, 0);
    private static Color textColor;
	private static EGraphics textGraphics = new EGraphics(false, false, null, 0, 0,0,0, 1.0,true,
		new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
	private static EGraphics gridGraphics = new EGraphics(false, false, null, 0, 0,0,0, 1.0,true,
		new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
	private static EGraphics instanceGraphics = new EGraphics(false, false, null, 0, 0,0,0, 1.0,true,
		new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
	private static EGraphics portGraphics = new EGraphics(false, false, null, 0, 255,0,0, 1.0,true,
		new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});

    private int clipLX, clipHX, clipLY, clipHY;
    
    private final EditWindow0 dummyWnd = new EditWindow0() {
        public VarContext getVarContext() { return varContext; }
        
        public double getScale() { return scale; }
    };

    static class Drawing extends EditWindow.Drawing {
        private final VectorDrawing vd = new VectorDrawing();
        private PixelDrawing offscreen;
        
        Drawing(EditWindow wnd) {
            super(wnd);
        }
        
        void setScreenSize(Dimension sz) {
            offscreen = new PixelDrawing(sz);
        }
        
        boolean paintComponent(Graphics2D g, Dimension sz) {
            if (offscreen == null || !wnd.getSize().equals(sz))
                return false;
            
            // show the image
            BufferedImage img = offscreen.getBufferedImage();
//		synchronized(img) // Do not need synchronization here
            {
                g.drawImage(img, 0, 0, wnd);
            }
            return true;
        }
        
        void render(boolean fullInstantiate, Rectangle2D bounds) {
            if (offscreen == null) return;
            offscreen.drawImage(this, fullInstantiate, bounds);
        }
        
        void abortRendering() {
            if (User.getDisplayAlgorithm() > 0)
                vd.abortRendering();
        }
    }
    
    // ************************************* TOP LEVEL *************************************

	/**
	 * Constructor creates an offscreen PixelDrawing object.
	 * @param sz the size of an offscreen PixelDrawinf object.
	 */
	public PixelDrawing(Dimension sz)
	{
		this.sz = new Dimension(sz);
        clipLX = 0;
        clipHX = sz.width - 1;
        clipLY = 0;
        clipHY = sz.height - 1;
        
		// allocate pointer to the opaque image
		img = new BufferedImage(sz.width, sz.height, BufferedImage.TYPE_INT_RGB);
		WritableRaster raster = ((BufferedImage)img).getRaster();
		DataBufferInt dbi = (DataBufferInt)raster.getDataBuffer();
		opaqueData = dbi.getData();
		total = sz.height * sz.width;
		numBytesPerRow = (sz.width + 7) / 8;
		renderedWindow = true;
	}
    
    public PixelDrawing(double scale, Rectangle screenBounds) {
        this.scale = scale;
        
        this.originX = -screenBounds.x;
        this.originY = screenBounds.y + screenBounds.height;
        this.sz = new Dimension(screenBounds.width, screenBounds.height);
        clipLX = 0;
        clipHX = sz.width - 1;
        clipLY = 0;
        clipHY = sz.height - 1;
        
		// allocate pointer to the opaque image
        img = null;
		total = sz.height * sz.width;
        opaqueData = new int[total];
		numBytesPerRow = (sz.width + 7) / 8;
        
		// initialize the data
		clearImage(null);
    }
    
    void initOrigin(double scale, Point2D offset) {
        this.scale = scale;
        this.originX = sz.width/2 - offset.getX()*scale;
        this.originY = sz.height/2 + offset.getY()*scale;
    }

    void initDrawing(double scale) {
		clearImage(null);
        initOrigin(scale, EPoint.ORIGIN);
    }
    
    /**
     * Method to set the printing mode used for all drawing.
     * @param mode the printing mode:  0=color display (default), 1=color printing, 2=B&W printing.
     */
    public void setPrintingMode(int mode) { nowPrinting = mode; }

    /**
	 * Method to override the background color.
	 * Must be called before "drawImage()".
	 * This is used by printing, which forces the background to be white.
	 * @param bg the background color to use.
	 */
	public void setBackgroundColor(Color bg)
	{
		backgroundColor = bg.getRGB() & 0xFFFFFF;
	}

	/**
	 * Method for obtaining the rendered image after "drawImage" has finished.
	 * @return an Image for this edit window.
	 */
	protected BufferedImage getBufferedImage() { return img; }

	/**
	 * Method for obtaining the RGB array of the rendered image after "drawImage" has finished.
	 * @return an RGB array for this edit window.
	 */
    int[] getOpaqueData() { return opaqueData; }
    
	/**
	 * Method for obtaining the size of the offscreen bitmap.
	 * @return the size of the offscreen bitmap.
	 */
	public Dimension getSize() { return sz; }

	/**
	 * Method to clear the cache of expanded subcells.
	 * This is used by layer visibility which, when changed, causes everything to be redrawn.
	 */
	public static void clearSubCellCache()
	{
		expandedCells = new HashMap<ExpandedCellKey,ExpandedCellInfo>();
	}

	/**
	 * This is the entry point for rendering.
	 * It displays a cell in this offscreen window.
	 * @param fullInstantiate true to display to the bottom of the hierarchy (for peeking).
	 * @param drawLimitBounds the area in the cell to display (null to show all).
	 * The rendered Image can then be obtained with "getImage()".
	 */
	private void drawImage(Drawing drawing, boolean fullInstantiate, Rectangle2D drawLimitBounds)
	{
		long startTime = 0;
		long initialUsed = 0;
		if (TAKE_STATS)
		{
//			Runtime.getRuntime().gc();
			startTime = System.currentTimeMillis();
			initialUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			tinyCells = tinyPrims = totalCells = renderedCells = totalPrims = tinyArcs = linedArcs = totalArcs = 0;
			offscreensCreated = offscreenPixelsCreated = offscreensUsed = offscreenPixelsUsed = cellsRendered = 0;
		}

		if (fullInstantiate != lastFullInstantiate)
		{
			clearSubCellCache();
			lastFullInstantiate = fullInstantiate;
		}

        EditWindow wnd = drawing.wnd;
		Cell cell = wnd.getCell();
    	if (wnd.isInPlaceEdit())
    	{
        	cell = wnd.getInPlaceEditTopCell();
       	}
		inPlaceNodePath = wnd.getInPlaceEditNodePath();

        drawBounds = wnd.getDisplayedBounds();

		// set colors to use
        textColor = new Color(User.getColorText());
		textGraphics.setColor(textColor);
		gridGraphics.setColor(new Color(User.getColorGrid()));
		instanceGraphics.setColor(new Color(User.getColorInstanceOutline()));
		
		// initialize the cache of expanded cell displays
		if (expandedScale != wnd.getScale())
		{
			clearSubCellCache();
			expandedScale = wnd.getScale();
		}
        varContext = wnd.getVarContext();
        initOrigin(expandedScale, wnd.getOffset());
 		canDrawText = expandedScale > 1;
		maxObjectSize = 2 / expandedScale;
		halfMaxObjectSize = maxObjectSize / 2;

		// remember the true window size (since recursive calls may cache individual cells that are smaller)
		topSz = sz;

		// see if any layers are being highlighted/dimmed
		highlightingLayers = false;
		for(Iterator<Layer> it = Technology.getCurrent().getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
			if (layer.isDimmed())
			{
				highlightingLayers = true;
				break;
			}
		}

		// initialize rendering into the offscreen image
		Rectangle renderBounds = null;
		if (drawLimitBounds != null)
		{
			renderBounds = wnd.databaseToScreen(drawLimitBounds);
            clipLX = Math.max(renderBounds.x, 0);
            clipHX = Math.min(renderBounds.x + renderBounds.width, sz.width) - 1;
            clipLY = Math.max(renderBounds.y, 0);
            clipHY = Math.min(renderBounds.y + renderBounds.height, sz.height) - 1;
		} else {
            clipLX = 0;
            clipHX = sz.width - 1;
            clipLY = 0;
            clipHY = sz.height - 1;
        }
		clearImage(renderBounds);
		periodicRefresh = true;
		this.wnd = wnd;
		objectCount = 0;
		lastRefreshTime = System.currentTimeMillis();

        HashSet<CellId> changedCellsCopy;
        synchronized (changedCells) {
            changedCellsCopy = new HashSet<CellId>(changedCells);
            changedCells.clear();
        }
        forceRedraw(changedCellsCopy);
        VectorCache.theCache.forceRedraw(changedCellsCopy);
		if (User.getDisplayAlgorithm() == 0)
		{
			// reset cached cell counts
			numberToReconcile = SINGLETONSTOADD;
			for(ExpandedCellInfo count : expandedCells.values())
				count.instanceCount = 0;

			// determine which cells should be cached (must have at least 2 instances)
			countCell(cell, drawLimitBounds, fullInstantiate, Orientation.IDENT, DBMath.MATID);

			// now render it all
            drawCell(cell, drawLimitBounds, fullInstantiate, Orientation.IDENT, DBMath.MATID, wnd.getCell(), wnd.getVarContext());
		} else
		{
			drawing.vd.render(this, scale, wnd.getOffset(), cell, fullInstantiate, inPlaceNodePath, renderBounds, wnd.getVarContext());
		}

		// merge transparent image into opaque one
		synchronized(img)
		{
			// if a grid is requested, overlay it
			if (cell != null && wnd.isGrid()) drawGrid(wnd);

			// combine transparent and opaque colors into a final image
			composite(renderBounds);
		};

		if (TAKE_STATS && User.getDisplayAlgorithm() == 0)
		{
			long endTime = System.currentTimeMillis();
			long curUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			long memConsumed = curUsed - initialUsed;
			System.out.println("Took "+com.sun.electric.database.text.TextUtils.getElapsedTime(endTime-startTime)+
				", rendered "+cellsRendered+" cells, used "+offscreensUsed+" ("+offscreenPixelsUsed+" pixels) cached cells, created "+
				offscreensCreated+" ("+offscreenPixelsCreated+" pixels) new cell caches (my size is "+total+" pixels), memory used="+memConsumed);
			System.out.println("   Cells ("+totalCells+") "+tinyCells+" are tiny;"+
				" Primitives ("+totalPrims+") "+tinyPrims+" are tiny;"+
				" Arcs ("+totalArcs+") "+tinyArcs+" are tiny, "+linedArcs+" are lines");
		}
	}

	/**
	 * This is the entry point for rendering.
	 * It displays a cell in this offscreen window.
	 * The rendered Image can then be obtained with "getImage()".
	 */
	public void printImage(double scale, Point2D offset, Cell cell, VarContext varContext)
	{
        clearSubCellCache();
        lastFullInstantiate = false;
        expandedScale = this.scale = scale; 

		// set colors to use
        textColor = new Color(User.getColorText());
		textGraphics.setColor(textColor);
		gridGraphics.setColor(new Color(User.getColorGrid()));
		instanceGraphics.setColor(new Color(User.getColorInstanceOutline()));
		
        initOrigin(scale, offset);
 		canDrawText = expandedScale > 1;
		maxObjectSize = 2 / expandedScale;
		halfMaxObjectSize = maxObjectSize / 2;

		// remember the true window size (since recursive calls may cache individual cells that are smaller)
		topSz = sz;

		// see if any layers are being highlighted/dimmed
		highlightingLayers = false;
		for(Iterator<Layer> it = Technology.getCurrent().getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
			if (layer.isDimmed())
			{
				highlightingLayers = true;
				break;
			}
		}

		// initialize rendering into the offscreen image
        clipLX = 0;
        clipHX = sz.width - 1;
        clipLY = 0;
        clipHY = sz.height - 1;
		clearImage(null);

        HashSet<CellId> changedCellsCopy;
        synchronized (changedCells) {
            changedCellsCopy = new HashSet<CellId>(changedCells);
            changedCells.clear();
        }
        forceRedraw(changedCellsCopy);
        VectorCache.theCache.forceRedraw(changedCellsCopy);
		if (User.getDisplayAlgorithm() == 0)
		{
			// reset cached cell counts
			numberToReconcile = SINGLETONSTOADD;
			for(ExpandedCellInfo count : expandedCells.values())
				count.instanceCount = 0;

			// determine which cells should be cached (must have at least 2 instances)
			countCell(cell, null, false, Orientation.IDENT, DBMath.MATID);

			// now render it all
            drawCell(cell, null, false, Orientation.IDENT, DBMath.MATID, cell, varContext);
		} else
		{
            VectorDrawing vd = new VectorDrawing();
			vd.render(this, scale, offset, cell, false, null, null, varContext);
		}

		// merge transparent image into opaque one
		synchronized(img)
		{
			// if a grid is requested, overlay it
//			if (cell != null && wnd.isGrid()) drawGrid(wnd);

			// combine transparent and opaque colors into a final image
			composite(null);
		};
	}

	// ************************************* INTERMEDIATE CONTROL LEVEL *************************************

	/**
	 * Method to erase the offscreen data in this PixelDrawing.
	 * This is called before any rendering is done.
	 * @param bounds the area of the image to actually draw (null to draw all).
	 */
	public void clearImage(Rectangle bounds)
	{
		// pickup new technology if it changed
		initForTechnology();
		backgroundColor = User.getColorBackground() & 0xFFFFFF;
		backgroundValue = backgroundColor | 0xFF000000;

		// erase the transparent bitmaps
		for(int i=0; i<numLayerBitMaps; i++)
		{
			byte [][] layerBitMap = layerBitMaps[i];
			if (layerBitMap == null) continue;
			for(int y=0; y<sz.height; y++)
			{
				byte [] row = layerBitMap[y];
				for(int x=0; x<numBytesPerRow; x++)
					row[x] = 0;
			}
		}

		// erase the patterned opaque layer bitmaps
		for(Iterator<Map.Entry<Layer,PatternedOpaqueLayer>> it = patternedOpaqueLayers.entrySet().iterator(); it.hasNext(); )
		{
			PatternedOpaqueLayer pol = (PatternedOpaqueLayer)it.next();
			byte [][] layerBitMap = pol.layerBitMap;
			for(int y=0; y<sz.height; y++)
			{
				byte [] row = layerBitMap[y];
				for(int x=0; x<numBytesPerRow; x++)
					row[x] = 0;
			}
		}

		// erase opaque image
		if (bounds == null)
		{
			// erase the entire image
			for(int i=0; i<total; i++) opaqueData[i] = backgroundValue;
		} else
		{
			// erase only part of the image
			int lx = bounds.x;
			int hx = lx + bounds.width;
			int ly = bounds.y;
			int hy = ly + bounds.height;
			if (lx < 0) lx = 0;
			if (hx >= sz.width) hx = sz.width - 1;
			if (ly < 0) ly = 0;
			if (hy >= sz.height) hy = sz.height - 1;
			for(int y=ly; y<=hy; y++)
			{
				int baseIndex = y * sz.width;
				for(int x=lx; x<=hx; x++)
					opaqueData[baseIndex + x] = backgroundValue;
			}
		}
	}

	/**
	 * Method to complete rendering by combining the transparent and opaque imagery.
	 * This is called after all rendering is done.
	 * @return the offscreen Image with the final display.
	 */
	public Image composite(Rectangle bounds)
	{
		// merge in the transparent layers
		if (numLayerBitMapsCreated > 0)
		{
			// get the technology's color map
			Color [] colorMap = curTech.getColorMap();

			// adjust the colors if any of the transparent layers are dimmed
			boolean dimmedTransparentLayers = false;
			for(Iterator<Layer> it = curTech.getLayers(); it.hasNext(); )
			{
				Layer layer = it.next();
				if (!layer.isDimmed()) continue;
				if (layer.getGraphics().getTransparentLayer() == 0) continue;
				dimmedTransparentLayers = true;
				break;
			}
			if (dimmedTransparentLayers)
			{
				Color [] newColorMap = new Color[colorMap.length];
				int numTransparents = curTech.getNumTransparentLayers();
				boolean [] dimLayer = new boolean[numTransparents];
				for(int i=0; i<numTransparents; i++) dimLayer[i] = true;
				for(Iterator<Layer> it = curTech.getLayers(); it.hasNext(); )
				{
					Layer layer = it.next();
					if (layer.isDimmed()) continue;
					int tIndex = layer.getGraphics().getTransparentLayer();
					if (tIndex == 0) continue;
					dimLayer[tIndex-1] = false;
				}

				for(int i=0; i<colorMap.length; i++)
				{
					newColorMap[i] = colorMap[i];
					if (i == 0) continue;
					boolean dimThisEntry = true;
					for(int j=0; j<numTransparents; j++)
					{
						if ((i & (1<<j)) != 0)
						{
							if (!dimLayer[j])
							{
								dimThisEntry = false;
								break;
							}
						}
					}
					if (dimThisEntry)
					{
						newColorMap[i] = new Color(dimColor(colorMap[i].getRGB()));
					} else
					{
						newColorMap[i] = new Color(brightenColor(colorMap[i].getRGB()));
					}
				}
				colorMap = newColorMap;
			}

			// determine range
			int lx = 0, hx = sz.width-1;
			int ly = 0, hy = sz.height-1;
			if (bounds != null)
			{
				lx = bounds.x;
				hx = lx + bounds.width;
				ly = bounds.y;
				hy = ly + bounds.height;
				if (lx < 0) lx = 0;
				if (hx >= sz.width) hx = sz.width - 1;
				if (ly < 0) ly = 0;
				if (hy >= sz.height) hy = sz.height - 1;
			}

			for(int y=ly; y<=hy; y++)
			{
				for(int i=0; i<numLayerBitMaps; i++)
				{
					byte [][] layerBitMap = layerBitMaps[i];
					if (layerBitMap == null) compositeRows[i] = null; else
					{
						compositeRows[i] = layerBitMap[y];
					}
				}
				int baseIndex = y * sz.width;
				for(int x=lx; x<=hx; x++)
				{
					int index = baseIndex + x;
					int pixelValue = opaqueData[index];
				
					// the value of Alpha starts at 0xFF, which means "background"
					// opaque drawing typically sets it to 0, which means "filled"
					// Text drawing can antialias by setting the edge values in the range 0-254
					//    where the lower the value, the more saturated the color (so 0 means all color, 254 means little color)
					int alpha = (pixelValue >> 24) & 0xFF;
					if (alpha != 0)
					{
						// aggregate the transparent bitplanes at this pixel
						int bits = 0;
						int entry = x >> 3;
						int maskBit = 1 << (x & 7);
						for(int i=0; i<numLayerBitMaps; i++)
						{
							if (compositeRows[i] == null) continue;
							int byt = compositeRows[i][entry];
							if ((byt & maskBit) != 0) bits |= (1<<i);
						}

						// determine the transparent color to draw
						int newColor = backgroundColor;
						if (bits != 0)
						{
							// set a transparent color
							newColor = colorMap[bits].getRGB() & 0xFFFFFF;
						}

						// if alpha blending, merge with the opaque data
						if (alpha != 0xFF)
						{
							newColor = alphaBlend(pixelValue, newColor, alpha);
						}
						opaqueData[index] = newColor;
					}
				}
			}
		} else
		{
			// nothing in transparent layers: make sure background color is right
			if (bounds == null)
			{
				// handle the entire image
				for(int i=0; i<total; i++)
				{
					int pixelValue = opaqueData[i];
					if (pixelValue == backgroundValue) opaqueData[i] = backgroundColor; else
					{
						if ((pixelValue&0xFF000000) != 0)
						{
							int alpha = (pixelValue >> 24) & 0xFF;
							opaqueData[i] = alphaBlend(pixelValue, backgroundColor, alpha);
						}
					}
				}
			} else
			{
				// handle a partial image
				int lx = bounds.x;
				int hx = lx + bounds.width;
				int ly = bounds.y;
				int hy = ly + bounds.height;
				if (lx < 0) lx = 0;
				if (hx >= sz.width) hx = sz.width - 1;
				if (ly < 0) ly = 0;
				if (hy >= sz.height) hy = sz.height - 1;
				for(int y=ly; y<=hy; y++)
				{
					int baseIndex = y * sz.width;
					for(int x=lx; x<=hx; x++)
					{
						int index = baseIndex + x;
						int pixelValue = opaqueData[index];
						if (pixelValue == backgroundValue) opaqueData[index] = backgroundColor; else
						{
							if ((pixelValue&0xFF000000) != 0)
							{
								int alpha = (pixelValue >> 24) & 0xFF;
								opaqueData[index] = alphaBlend(pixelValue, backgroundColor, alpha);
							}
						}
					}
				}
			}
		}
		return img;
	}

	/**
	 * Method to draw the grid into the offscreen buffer
	 */
	private void drawGrid(EditWindow wnd)
	{
		double spacingX = wnd.getGridXSpacing();
		double spacingY = wnd.getGridYSpacing();
		if (spacingX == 0 || spacingY == 0) return;
		double boldSpacingX = spacingX * User.getDefGridXBoldFrequency();
		double boldSpacingY = spacingY * User.getDefGridYBoldFrequency();
		double boldSpacingThreshX = spacingX / 4;
		double boldSpacingThreshY = spacingY / 4;

		// screen extent
		Rectangle2D displayable = wnd.displayableBounds();
		double lX = displayable.getMinX();  double lY = displayable.getMaxY();
		double hX = displayable.getMaxX();  double hY = displayable.getMinY();
		double scaleX = sz.width / (hX - lX);
		double scaleY = sz.height / (lY - hY);

		// initial grid location
		double x1 = DBMath.toNearest(lX, spacingX);
		double y1 = DBMath.toNearest(lY, spacingY);

		// adjust grid placement according to scale
		boolean allBoldDots = false;
		if (spacingX * scaleX < 5 || spacingY * scaleY < 5)
		{
			// normal grid is too fine: only show the "bold dots"
			x1 = DBMath.toNearest(x1, boldSpacingX);   spacingX = boldSpacingX;
			y1 = DBMath.toNearest(y1, boldSpacingY);   spacingY = boldSpacingY;

			// if even the bold dots are too close, don't draw a grid
			if (spacingX * scaleX < 10 || spacingY * scaleY < 10) return;
		} else if (spacingX * scaleX > 75 && spacingY * scaleY > 75)
		{
			// if zoomed-out far enough, show all bold dots
			allBoldDots = true;
		}

		// draw the grid
		int col = User.getColorGrid() & 0xFFFFFF;
		for(double i = y1; i > hY; i -= spacingY)
		{
			double boldValueY = i;
			if (i < 0) boldValueY -= boldSpacingThreshY/2; else
				boldValueY += boldSpacingThreshY/2;
			boolean everyTenY = Math.abs(boldValueY) % boldSpacingY < boldSpacingThreshY;
			for(double j = x1; j < hX; j += spacingX)
			{
				Point xy = wnd.databaseToScreen(j, i);
				int x = xy.x;
				int y = xy.y;
				if (x < 0 || x > sz.width) continue;
				if (y < 0 || y > sz.height) continue;

				double boldValueX = j;
				if (j < 0) boldValueX -= boldSpacingThreshX/2; else
					boldValueX += boldSpacingThreshX/2;
				boolean everyTenX = Math.abs(boldValueX) % boldSpacingX < boldSpacingThreshX;
				if (allBoldDots && everyTenX && everyTenY)
				{
					int boxLX = x-2;   if (boxLX < 0) boxLX = 0;
					int boxHX = x+2;   if (boxHX >= sz.width) boxHX = sz.width-1;
					int boxLY = y-2;   if (boxLY < 0) boxLY = 0;
					int boxHY = y+2;   if (boxHY >= sz.height) boxHY = sz.height-1;
					drawBox(boxLX, boxHX, boxLY, boxHY, null, gridGraphics, false);
					if (x > 1) opaqueData[y * sz.width + (x-2)] = col;
					if (x < sz.width-2) opaqueData[y * sz.width + (x+2)] = col;
					if (y > 1) opaqueData[(y-2) * sz.width + x] = col;
					if (y < sz.height-2) opaqueData[(y+2) * sz.width + x] = col;
					continue;
				}

				// special case every 10 grid points in each direction
				if (allBoldDots || (everyTenX && everyTenY))
				{
					opaqueData[y * sz.width + x] = col;
					if (x > 0) opaqueData[y * sz.width + (x-1)] = col;
					if (x < sz.width-1) opaqueData[y * sz.width + (x+1)] = col;
					if (y > 0) opaqueData[(y-1) * sz.width + x] = col;
					if (y < sz.height-1) opaqueData[(y+1) * sz.width + x] = col;
					continue;
				}

				// just a single dot
				opaqueData[y * sz.width + x] = col;
			}
		}
		if (User.isGridAxesShown())
		{
			Point xy = wnd.databaseToScreen(0, 0);
			if (xy.x >= 0 && xy.x < sz.width)
				drawSolidLine(xy.x, 0, xy.x, sz.height-1, null, col);
			if (xy.y >= 0 && xy.y < sz.height)
				drawSolidLine(0, xy.y, sz.width-1, xy.y, null, col);
		}
	}

	private void initForTechnology()
	{
		// allocate pointers to the overlappable layers
		Technology tech = Technology.getCurrent();
		if (tech == null) return;
		if (tech == curTech) return;
		int transLayers = tech.getNumTransparentLayers();
		if (transLayers != 0)
		{
			techWithLayers = curTech = tech;
		}
		if (curTech == null) curTech = techWithLayers;
        if (curTech == null) return;

        curTechLayers = new ERaster[curTech.getNumLayers()];
		numLayerBitMaps = curTech.getNumTransparentLayers();
		layerBitMaps = new byte[numLayerBitMaps][][];
		compositeRows = new byte[numLayerBitMaps][];
		for(int i=0; i<numLayerBitMaps; i++) layerBitMaps[i] = null;
		numLayerBitMapsCreated = 0;
	}

    private void periodicRefresh() {
        // handle refreshing
        if (periodicRefresh) {
            objectCount++;
            if (objectCount > 100) {
                objectCount = 0;
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastRefreshTime > 1000) {
                    wnd.repaint();
                }
            }
        }
    }
    
	// ************************************* HIERARCHY TRAVERSAL *************************************

	/**
	 * Method to draw the contents of a cell, transformed through "prevTrans".
	 */
	private void drawCell(Cell cell, Rectangle2D drawLimitBounds, boolean fullInstantiate, Orientation orient,  AffineTransform prevTrans, Cell topCell, VarContext context)
	{
		renderedCells++;
        renderPolyTime = 0;
        renderTextTime = 0;

        // draw all arcs
		for(Iterator<ArcInst> arcs = cell.getArcs(); arcs.hasNext(); )
		{
			ArcInst ai = arcs.next();

			// if limiting drawing, reject when out of area
			if (drawLimitBounds != null)
			{
				Rectangle2D curBounds = ai.getBounds();
				Rectangle2D bounds = new Rectangle2D.Double(curBounds.getX(), curBounds.getY(), curBounds.getWidth(), curBounds.getHeight());
				GenMath.transformRect(bounds, prevTrans);
				if (!GenMath.rectsIntersect(bounds, drawLimitBounds)) continue;
			}
			drawArc(ai, prevTrans, false);
		}

		// draw all nodes
		for(Iterator<NodeInst> nodes = cell.getNodes(); nodes.hasNext(); )
		{
			NodeInst ni = nodes.next();

			// if limiting drawing, reject when out of area
			if (drawLimitBounds != null)
			{
				Rectangle2D curBounds = ni.getBounds();
				Rectangle2D bounds = new Rectangle2D.Double(curBounds.getX(), curBounds.getY(), curBounds.getWidth(), curBounds.getHeight());
				GenMath.transformRect(bounds, prevTrans);
				if (!GenMath.rectsIntersect(bounds, drawLimitBounds)) continue;
			}
			drawNode(ni, orient, prevTrans, topCell, drawLimitBounds, fullInstantiate, false, context);
		}

		// show cell variables if at the top level
		boolean topLevel = true;
		if (topCell != null) topLevel = (cell == topCell);
		if (canDrawText && topLevel && User.isTextVisibilityOnCell())
		{
			// show displayable variables on the instance
			int numPolys = cell.numDisplayableVariables(true);
			Poly [] polys = new Poly[numPolys];
			cell.addDisplayableVariables(CENTERRECT, polys, 0, dummyWnd, true);
			drawPolys(polys, prevTrans, false);
		}

        if (DEBUGRENDERTIMING) {
            System.out.println("Total time to render polys: "+TextUtils.getElapsedTime(renderPolyTime));
            System.out.println("Total time to render text: "+TextUtils.getElapsedTime(renderTextTime));
        }
	}

	/**
	 * Method to draw a NodeInst into the offscreen image.
	 * @param ni the NodeInst to draw.
     * @param trans the transformation of the NodeInst to the display.
     * @param topCell the Cell at the top-level of display.
     * @param drawLimitBounds bounds in which to draw.
     * @param fullInstantiate true to draw to the bottom of the hierarchy ("peek" mode).
     * @param forceVisible true if layer visibility information should be ignored and force the drawing
	 * @param context the VarContext to this node in the hierarchy.
     */
	public void drawNode(NodeInst ni, Orientation orient, AffineTransform trans, Cell topCell, Rectangle2D drawLimitBounds,
		boolean fullInstantiate, boolean forceVisible, VarContext context)
	{
		NodeProto np = ni.getProto();
		AffineTransform localTrans = ni.rotateOut(trans);

		boolean topLevel = true;
		if (topCell != null) topLevel = (ni.getParent() == topCell);

		// draw the node
		if (ni.isCellInstance())
		{
			// cell instance
			totalCells++;
//			if (objWidth < maxObjectSize)
//			{
//				tinyCells++;
//				return;
//			}

			// see if it is on the screen
			Cell subCell = (Cell)np;
            Orientation subOrient = orient.concatenate(ni.getOrient());
			AffineTransform subTrans = ni.translateOut(localTrans);
       		Rectangle2D cellBounds = subCell.getBounds();
			Poly poly = new Poly(cellBounds);
			poly.transform(subTrans);
//			if (wnd.isInPlaceEdit()) poly.transform(wnd.getInPlaceTransformIn());
			cellBounds = poly.getBounds2D();
			Rectangle screenBounds = databaseToScreen(cellBounds);
			if (screenBounds.width <= 0 || screenBounds.height <= 0)
			{
				tinyCells++;
				return;
			}
			if (screenBounds.x >= sz.width || screenBounds.x+screenBounds.width <= 0) return;
			if (screenBounds.y >= sz.height || screenBounds.y+screenBounds.height <= 0) return;

			boolean expanded = ni.isExpanded();
			if (fullInstantiate) expanded = true;

			// if not expanded, but viewing this cell in-place, expand it
			if (!expanded)
			{
				if (inPlaceNodePath != null)
				{
					for(int pathIndex=0; pathIndex<inPlaceNodePath.size(); pathIndex++)
					{
						NodeInst niOnPath = inPlaceNodePath.get(pathIndex);
						if (niOnPath.getProto() == subCell)
						{
							expanded = true;
							break;
						}
					}
				}
			}

			// two ways to draw a cell instance
			if (expanded)
			{
				// show the contents of the cell
				if (!expandedCellCached(subCell, subOrient, subTrans, topCell, context, drawLimitBounds, fullInstantiate))
				{
					// just draw it directly
					cellsRendered++;
					drawCell(subCell, drawLimitBounds, fullInstantiate, subOrient, subTrans, topCell, context.push(ni));
				}
				if (canDrawText) showCellPorts(ni, trans, Color.BLACK);
			} else
			{
				// draw the black box of the instance
				drawUnexpandedCell(ni, poly);
				if (canDrawText) showCellPorts(ni, trans, null);
			}

			// draw any displayable variables on the instance
			if (canDrawText && User.isTextVisibilityOnNode())
			{
				int numPolys = ni.numDisplayableVariables(true);
				Poly [] polys = new Poly[numPolys];
				Rectangle2D rect = ni.getUntransformedBounds();
				ni.addDisplayableVariables(rect, polys, 0, dummyWnd, true);
				drawPolys(polys, localTrans, false);
			}
		} else
		{
			// primitive: see if it can be drawn
			if (topLevel || (!ni.isVisInside() && np != Generic.tech.cellCenterNode))
			{
				// see if the node is completely clipped from the screen
				Point2D ctr = ni.getTrueCenter();
				trans.transform(ctr, ctr);
				double halfWidth = Math.max(ni.getXSize(), ni.getYSize()) / 2;
				double ctrX = ctr.getX();
				double ctrY = ctr.getY();
                if (renderedWindow && drawBounds != null) {
                    Rectangle2D databaseBounds = drawBounds;
//                    Rectangle2D databaseBounds = wnd.getDisplayedBounds();
                    if (ctrX + halfWidth < databaseBounds.getMinX()) return;
                    if (ctrX - halfWidth > databaseBounds.getMaxX()) return;
                    if (ctrY + halfWidth < databaseBounds.getMinY()) return;
                    if (ctrY - halfWidth > databaseBounds.getMaxY()) return;
                }
	
				PrimitiveNode prim = (PrimitiveNode)np;
				totalPrims++;
				if (!prim.isCanBeZeroSize() && halfWidth < halfMaxObjectSize && !forceVisible)
				{
					// draw a tiny primitive by setting a single dot from each layer
					tinyPrims++;
					databaseToScreen(ctrX, ctrY, tempPt1);
					if (tempPt1.x >= 0 && tempPt1.x < sz.width && tempPt1.y >= 0 && tempPt1.y < sz.height)
					{
						drawTinyLayers(prim.getLayerIterator(), tempPt1.x, tempPt1.y);
					}
					return;
				}

				EditWindow0 nodeWnd = dummyWnd;
				if (!forceVisible && (!canDrawText || !User.isTextVisibilityOnNode())) nodeWnd = null;
				if (prim == Generic.tech.invisiblePinNode)
				{
					if (!User.isTextVisibilityOnAnnotation()) nodeWnd = null;
				}
				Technology tech = prim.getTechnology();
				Poly [] polys = tech.getShapeOfNode(ni, nodeWnd, context, false, false, null);
				drawPolys(polys, localTrans, forceVisible);
			}
		}

		// draw any exports from the node
		if (canDrawText && topLevel && User.isTextVisibilityOnExport())
		{
			int exportDisplayLevel = User.getExportDisplayLevel();
			Iterator<Export> it = ni.getExports();
			while (it.hasNext())
			{
				Export e = it.next();
				Poly poly = e.getNamePoly();
                poly.transform(trans);
//				if (topWnd != null && topWnd.isInPlaceEdit())
//					poly.transform(topWnd.getInPlaceTransformOut());
				Rectangle2D rect = (Rectangle2D)poly.getBounds2D().clone();
				if (exportDisplayLevel == 2)
				{
					// draw port as a cross
					drawCross(poly, textGraphics, false);
				} else
				{
					// draw port as text
					TextDescriptor descript = poly.getTextDescriptor();
					Poly.Type type = descript.getPos().getPolyType();
					String portName = e.getName();
					if (exportDisplayLevel == 1)
					{
						// use shorter port name
						portName = e.getShortName();
					}
//					if (topWnd != null && topWnd.isInPlaceEdit())
//						poly.transform(topWnd.getInPlaceTransformOut());
					databaseToScreen(poly.getCenterX(), poly.getCenterY(), tempPt1);
					Rectangle textRect = new Rectangle(tempPt1);
					type = Poly.rotateType(type, ni);
					drawText(textRect, type, descript, portName, null, textGraphics, false);
				}

				// draw variables on the export
				int numPolys = e.numDisplayableVariables(true);
				if (numPolys > 0)
				{
					Poly [] polys = new Poly[numPolys];
					e.addDisplayableVariables(rect, polys, 0, dummyWnd, true);
					drawPolys(polys, localTrans, false);
				}
			}
		}
	}

	/**
	 * Method to render an ArcInst into the offscreen image.
	 * @param ai the ArcInst to draw.
     * @param trans the transformation of the ArcInst to the display.
     * @param forceVisible true to ignore layer visibility and draw all layers.
     */
	public void drawArc(ArcInst ai, AffineTransform trans, boolean forceVisible)
	{
		// if the arc is tiny, just approximate it with a single dot
		Rectangle2D arcBounds = ai.getBounds();
        double arcSize = Math.max(arcBounds.getWidth(), arcBounds.getHeight());
		totalArcs++;
		if (!forceVisible)
		{
			if (arcSize < maxObjectSize)
			{
				tinyArcs++;
				return;
			}
			if (ai.getWidth() > 0)
			{
				arcSize = Math.min(arcBounds.getWidth(), arcBounds.getHeight());
				if (arcSize < maxObjectSize)
				{
					linedArcs++;
	
					// draw a tiny arc by setting a single dot from each layer
					Point2D headEnd = new Point2D.Double(ai.getHeadLocation().getX(), ai.getHeadLocation().getY());
					trans.transform(headEnd, headEnd);
					databaseToScreen(headEnd.getX(), headEnd.getY(), tempPt1);
					Point2D tailEnd = new Point2D.Double(ai.getTailLocation().getX(), ai.getTailLocation().getY());
					trans.transform(tailEnd, tailEnd);
					databaseToScreen(tailEnd.getX(), tailEnd.getY(), tempPt2);
					ArcProto prim = ai.getProto();
					drawTinyArc(prim.getLayerIterator(), tempPt1, tempPt2);
					return;
				}
			}
		}

        // see if the arc is completely clipped from the screen
        Rectangle2D dbBounds = new Rectangle2D.Double(arcBounds.getX(), arcBounds.getY(), arcBounds.getWidth(), arcBounds.getHeight());
        Poly p = new Poly(dbBounds);
        p.transform(trans);
        dbBounds = p.getBounds2D();
        if (drawBounds != null && !GenMath.rectsIntersect(drawBounds, dbBounds)) return;

		// draw the arc
		ArcProto ap = ai.getProto();
		Technology tech = ap.getTechnology();
		EditWindow0 arcWnd = dummyWnd;
		if (!canDrawText || !User.isTextVisibilityOnArc()) arcWnd = null;
		Poly [] polys = tech.getShapeOfArc(ai, arcWnd);
		drawPolys(polys, trans, forceVisible);
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
			portGraphics.setColor(portColor);
			if (portDisplayLevel == 2)
			{
				// draw port as a cross
				drawCross(portPoly, portGraphics, false);
			} else
			{
				// draw port as text
				if (User.isTextVisibilityOnPort())
				{
					// combine all features of port text with color of the port
					TextDescriptor descript = portPoly.getTextDescriptor();
					TextDescriptor portDescript = pp.getTextDescriptor(Export.EXPORT_NAME).withColorIndex(descript.getColorIndex());
					Poly.Type type = descript.getPos().getPolyType();
					String portName = pp.getName();
					if (portDisplayLevel == 1)
					{
						// use shorter port name
						portName = pp.getShortName();
					}
					databaseToScreen(portPoly.getCenterX(), portPoly.getCenterY(), tempPt1);
					Rectangle rect = new Rectangle(tempPt1);
					drawText(rect, type, portDescript, portName, null, portGraphics, false);
				}
			}
		}
	}

	private void drawUnexpandedCell(NodeInst ni, Poly poly)
	{
		// draw the instance outline
		Point2D [] points = poly.getPoints();
		for(int i=0; i<points.length; i++)
		{
			int lastI = i - 1;
			if (lastI < 0) lastI = points.length - 1;
			Point2D lastPt = points[lastI];
			Point2D thisPt = points[i];
			databaseToScreen(lastPt.getX(), lastPt.getY(), tempPt1);
			databaseToScreen(thisPt.getX(), thisPt.getY(), tempPt2);
			drawLine(tempPt1, tempPt2, null, instanceGraphics, 0, false);
		}

		// draw the instance name
		if (canDrawText && User.isTextVisibilityOnInstance())
		{
			Rectangle2D bounds = poly.getBounds2D();
			Rectangle rect = databaseToScreen(bounds);
			TextDescriptor descript = ni.getTextDescriptor(NodeInst.NODE_PROTO);
			NodeProto np = ni.getProto();
			drawText(rect, Poly.Type.TEXTBOX, descript, np.describe(false), null, textGraphics, false);
		}
	}

	private void drawTinyLayers(Iterator<Layer> layerIterator, int x, int y)
	{
		for(Iterator<Layer> it = layerIterator; it.hasNext(); )
		{
			Layer layer = it.next();
			if (layer == null) continue;
			byte [][] layerBitMap = null;
			int col = 0;
			EGraphics graphics = layer.getGraphics();
			if (graphics != null)
			{
				if (nowPrinting != 0 ? graphics.isPatternedOnPrinter() : graphics.isPatternedOnDisplay())
				{
					int [] pattern = graphics.getPattern();
					if (pattern != null)
					{
						int pat = pattern[y&15];
						if (pat == 0 || (pat & (0x8000 >> (x&15))) == 0) continue;
					}
				}
				int layerNum = graphics.getTransparentLayer() - 1;
				if (layerNum < numLayerBitMaps) layerBitMap = getLayerBitMap(layerNum);
				col = graphics.getRGB();
			}

			// set the bit
			if (layerBitMap == null)
			{
				int index = y * sz.width + x;
				int alpha = (opaqueData[index] >> 24) & 0xFF;
				if (alpha == 0xFF) opaqueData[index] = col;
			} else
			{
				layerBitMap[y][x>>3] |= (1 << (x&7));
			}
		}
	}

	private void drawTinyArc(Iterator<Layer> layerIterator, Point head, Point tail)
	{
		for(Iterator<Layer> it = layerIterator; it.hasNext(); )
		{
			Layer layer = it.next();
			if (layer == null) continue;
			EGraphics graphics = layer.getGraphics();
			byte [][] layerBitMap = null;
			if (graphics != null)
			{
				int layerNum = graphics.getTransparentLayer() - 1;
				if (layerNum < numLayerBitMaps) layerBitMap = getLayerBitMap(layerNum);
			}
			drawLine(head, tail, layerBitMap, graphics, 0, layer.isDimmed());
		}
	}

	// ************************************* CELL CACHING *************************************

	/**
	 * @return true if the cell is properly handled and need no further processing.
	 * False to render the contents recursively.
	 */
	private boolean expandedCellCached(Cell subCell, Orientation orient, AffineTransform origTrans, Cell topCell, VarContext context, Rectangle2D drawLimitBounds, boolean fullInstantiate)
	{
		// if there is no global for remembering cached cells, do not cache
		if (expandedCells == null) return false;

		// do not cache icons: they can be redrawn each time
		if (subCell.isIcon()) return false;

        ExpandedCellKey expansionKey = new ExpandedCellKey(subCell, orient);
		ExpandedCellInfo expandedCellCount = expandedCells.get(expansionKey);
		if (expandedCellCount != null)
		{
			// if this combination is not used multiple times, do not cache it
			if (expandedCellCount.singleton && expandedCellCount.instanceCount < 2 && expandedCellCount.offscreen == null)
			{
				if (numberToReconcile > 0)
				{
					numberToReconcile--;
					expandedCellCount.singleton = false;
				} else return false;
			}
		}

		if (expandedCellCount == null || expandedCellCount.offscreen == null)
		{
            // compute the cell's location on the screen
            Rectangle2D cellBounds = new Rectangle2D.Double();
            cellBounds.setRect(subCell.getBounds());
            Rectangle2D textBounds = subCell.getTextBounds(dummyWnd);
            if (textBounds != null)
                cellBounds.add(textBounds);
            AffineTransform rotTrans = orient.pureRotate();
            DBMath.transformRect(cellBounds, rotTrans);
            int lX = (int)Math.floor(cellBounds.getMinX()*scale);
            int hX = (int)Math.ceil(cellBounds.getMaxX()*scale);
            int lY = (int)Math.floor(cellBounds.getMinY()*scale);
            int hY = (int)Math.ceil(cellBounds.getMaxY()*scale);
            Rectangle screenBounds = new Rectangle(lX, lY, hX - lX, hY - lY);

            if (screenBounds.width <= 0 || screenBounds.height <= 0) return true;

			// do not cache if the cell is too large (creates immense offscreen buffers)
			if (screenBounds.width >= topSz.width/2 && screenBounds.height >= topSz.height/2)
				return false;

			// if this is the first use, create the offscreen buffer
			if (expandedCellCount == null)
			{
				expandedCellCount = new ExpandedCellInfo();
				expandedCells.put(expansionKey, expandedCellCount);
			}

            expandedCellCount.offscreen = new PixelDrawing(scale, screenBounds);
			expandedCellCount.offscreen.drawCell(subCell, null, fullInstantiate, orient, rotTrans, topCell, context);
			offscreensCreated++;
            offscreenPixelsCreated += expandedCellCount.offscreen.total;
            
		}

		// copy out of the offscreen buffer into the main buffer
        databaseToScreen(origTrans.getTranslateX(), origTrans.getTranslateY(), tempPt1);
		copyBits(expandedCellCount.offscreen, tempPt1.x, tempPt1.y);
		offscreensUsed++;
        offscreenPixelsUsed += expandedCellCount.offscreen.total;
		return true;
	}

	/**
	 * Recursive method to count the number of times that a cell-transformation is used
	 */
	private void countCell(Cell cell, Rectangle2D drawLimitBounds, boolean fullInstantiate, Orientation orient, AffineTransform prevTrans)
	{
		// look for subcells
		for(Iterator<NodeInst> nodes = cell.getNodes(); nodes.hasNext(); )
		{
			NodeInst ni = nodes.next();
			if (!ni.isCellInstance()) continue;

			// if limiting drawing, reject when out of area
			if (drawLimitBounds != null)
			{
				Rectangle2D curBounds = ni.getBounds();
				Rectangle2D bounds = new Rectangle2D.Double(curBounds.getX(), curBounds.getY(), curBounds.getWidth(), curBounds.getHeight());
				GenMath.transformRect(bounds, prevTrans);
				if (!GenMath.rectsIntersect(bounds, drawLimitBounds)) return;
			}

			countNode(ni, drawLimitBounds, fullInstantiate, orient, prevTrans);
		}
	}

	/**
	 * Recursive method to count the number of times that a cell-transformation is used
	 */
	private void countNode(NodeInst ni, Rectangle2D drawLimitBounds, boolean fullInstantiate, Orientation orient, AffineTransform trans)
	{
		// if the node is tiny, it will be approximated
		double objWidth = Math.max(ni.getXSize(), ni.getYSize());
		if (objWidth < maxObjectSize) return;

		// transform into the subcell
        Orientation subOrient = orient.concatenate(ni.getOrient());
		AffineTransform subTrans = ni.transformOut(trans);
//		AffineTransform localTrans = ni.rotateOut(trans);
//		AffineTransform subTrans = ni.translateOut(localTrans);

		// compute where this cell lands on the screen
		NodeProto np = ni.getProto();
		Cell subCell = (Cell)np;
		Rectangle2D cellBounds = subCell.getBounds();
		Poly poly = new Poly(cellBounds);
		poly.transform(subTrans);
//		if (wnd.isInPlaceEdit()) poly.transform(wnd.getInPlaceTransformIn());
		cellBounds = poly.getBounds2D();
		Rectangle screenBounds = databaseToScreen(cellBounds);
		if (screenBounds.width <= 0 || screenBounds.height <= 0) return;
		if (screenBounds.x > sz.width || screenBounds.x+screenBounds.width < 0) return;
		if (screenBounds.y > sz.height || screenBounds.y+screenBounds.height < 0) return;

		// only interested in expanded instances
		boolean expanded = ni.isExpanded();
		if (fullInstantiate) expanded = true;

		// if not expanded, but viewing this cell in-place, expand it
		if (!expanded)
		{
			if (inPlaceNodePath != null)
			{
				for(int pathIndex=0; pathIndex<inPlaceNodePath.size(); pathIndex++)
				{
					NodeInst niOnPath = inPlaceNodePath.get(pathIndex);
					if (niOnPath.getProto() == subCell)
					{
						expanded = true;
						break;
					}
				}
			}
		}
		if (!expanded) return;

		if (screenBounds.width < sz.width/2 || screenBounds.height <= sz.height/2)
		{
			// construct the cell name that combines with the transformation
			ExpandedCellKey expansionKey = new ExpandedCellKey(subCell, subOrient);
			ExpandedCellInfo expansionCount = expandedCells.get(expansionKey);
			if (expansionCount == null)
			{
				expansionCount = new ExpandedCellInfo();
				expansionCount.instanceCount = 1;
				expandedCells.put(expansionKey, expansionCount);
			} else
			{
				expansionCount.instanceCount++;
				if (expansionCount.instanceCount > 1) return;
			}
		}

		// now recurse

		countCell(subCell, null, fullInstantiate, subOrient, subTrans);
	}

	public static void forceRedraw(Cell cell)
	{
        synchronized (changedCells) {
            changedCells.add(cell.getId());
        }
	}

	private static void forceRedraw(HashSet<CellId> changedCells)
	{
		// if there is no global for remembering cached cells, do not cache
		if (expandedCells == null) return;

		List<ExpandedCellKey> keys = new ArrayList<ExpandedCellKey>();
		for(ExpandedCellKey eck : expandedCells.keySet() )
			keys.add(eck);
		for(ExpandedCellKey expansionKey : keys)
		{
            if (changedCells.contains(expansionKey.cell.getId()))
				expandedCells.remove(expansionKey);
		}
	}

	/**
	 * Method to copy the offscreen bits for a cell into the offscreen bits for the entire screen.
	 */
	private void copyBits(PixelDrawing srcOffscreen, int centerX, int centerY)
	{
		if (srcOffscreen == null) return;
		Dimension dim = srcOffscreen.sz;
        int cornerX = centerX - (int)srcOffscreen.originX;
        int cornerY = centerY - (int)srcOffscreen.originY;
        int minSrcX = Math.max(0, clipLX - cornerX);
        int maxSrcX = Math.min(dim.width - 1, clipHX - cornerX);
        int minSrcY = Math.max(0, clipLY - cornerY);
        int maxSrcY = Math.min(dim.height - 1, clipHY - cornerY);
        if (minSrcX > maxSrcX || minSrcY > maxSrcY) return;

        if (Job.getDebug() && numLayerBitMaps != srcOffscreen.numLayerBitMaps)
            System.out.println("Possible mixture of technologies in PixelDrawing.copyBits");

		// copy the opaque and transparent layers
        for (int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
            int destY = srcY + cornerY;
            assert destY >= clipLY && destY <= clipHY;
            if (destY < 0 || destY >= sz.height) continue;
            int srcBase = srcY * dim.width;
            int destBase = destY * sz.width;
            
            for (int srcX = minSrcX; srcX <= maxSrcX; srcX++) {
                int destX = srcX + cornerX;
                assert destX >= clipLX && destX <= clipHX;
                if (destX < 0 || destX >= sz.width) continue;
                int srcColor = srcOffscreen.opaqueData[srcBase + srcX];
                if (srcColor != backgroundValue)
                    opaqueData[destBase + destX] = srcColor;
            }
        }
        for (int i = 0; i < numLayerBitMaps; i++) {
            // out of range. Possible mixture of technologies.
            if (i >= srcOffscreen.numLayerBitMaps) break;
            byte [][] srcLayerBitMap = srcOffscreen.layerBitMaps[i];
            if (srcLayerBitMap == null) continue;
            byte [][] destLayerBitMap = getLayerBitMap(i);
            
            for (int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
                int destY = srcY + cornerY;
                assert destY >= clipLY && destY <= clipHY;
                if (destY < 0 || destY >= sz.height) continue;
                byte [] srcRow = srcLayerBitMap[srcY];
                byte [] destRow = destLayerBitMap[destY];
                
                for (int srcX = minSrcX; srcX <= maxSrcX; srcX++) {
                    int destX = srcX + cornerX;
                    assert destX >= clipLX && destX <= clipHX;
                    if (destX < 0 || destX >= sz.width) continue;
                    
                    if ((srcRow[srcX>>3] & (1<<(srcX&7))) != 0)
                        destRow[destX>>3] |= (1 << (destX&7));
                }
            }
        }

		// copy the patterned opaque layers
		for(Layer layer : srcOffscreen.patternedOpaqueLayers.keySet())
		{
			PatternedOpaqueLayer polSrc = srcOffscreen.patternedOpaqueLayers.get(layer);
			byte [][] srcLayerBitMap = polSrc.layerBitMap;
			if (srcLayerBitMap == null) continue;

			if (renderedWindow)
			{
				// this is the top-level of display: convert patterned opaque to patterns
				EGraphics desc = layer.getGraphics();
				int col = desc.getRGB();
				int [] pattern = desc.getPattern();

				// setup pattern for this row
				for (int srcY = minSrcY; srcY <= maxSrcY; srcY++)
				{
					int destY = srcY + cornerY;
                    assert destY >= clipLY && destY <= clipHY;
					if (destY < 0 || destY >= sz.height) continue;
					int destBase = destY * sz.width;
					int pat = pattern[destY&15];
					if (pat == 0) continue;
					byte [] srcRow = srcLayerBitMap[srcY];
					for (int srcX = minSrcX; srcX <= maxSrcX; srcX++)
					{
						int destX = srcX + cornerX;
                        assert destX >= clipLX && destX <= clipHX;
						if (destX < 0 || destX >= sz.width) continue;
						if ((srcRow[srcX>>3] & (1<<(srcX&7))) != 0)
						{
							if ((pat & (0x8000 >> (destX&15))) != 0)
								opaqueData[destBase + destX] = col;
						}
					}
				}
			} else
			{
				// a lower level being copied to a low level: just copy the patterned opaque layers
				PatternedOpaqueLayer polDest = patternedOpaqueLayers.get(layer);
				if (polDest == null)
				{
					polDest = new PatternedOpaqueLayer(sz.height, numBytesPerRow);
					patternedOpaqueLayers.put(layer, polDest);
				}
				byte [][] destLayerBitMap = polDest.layerBitMap;
				for(int srcY = minSrcY; srcY <= maxSrcY; srcY++)
				{
					int destY = srcY + cornerY;
                    assert destY >= clipLY && destY <= clipHY;
					if (destY < 0 || destY >= sz.height) continue;
					int destBase = destY * sz.width;
					byte [] srcRow = srcLayerBitMap[srcY];
					byte [] destRow = destLayerBitMap[destY];
					for (int srcX = minSrcX; srcX <= maxSrcX; srcX++)
					{
						int destX = srcX + cornerX;
                        assert destX >= clipLX && destX <= clipHX;
						if (destX < 0 || destX >= sz.width) continue;
						if ((srcRow[srcX>>3] & (1<<(srcX&7))) != 0)
							destRow[destX>>3] |= (1 << (destX&7));
					}
				}
			}
		}
	}

	// ************************************* RENDERING POLY SHAPES *************************************
    
    /**
     * A class representing a rectangular array of pixels. References
     * to pixels outside of the bounding rectangle may result in an
     * exception being thrown, or may result in references to unintended
     * elements of the Raster's associated DataBuffer.  It is the user's
     * responsibility to avoid accessing such pixels.
     */
    static interface ERaster {
        /**
         * Method to fill a box [lX,hX] x [lY,hY].
         * Both low and high coordiantes are inclusive.
         * Filling might be patterned.
         * @param lX left X coordinate
         * @param hX right X coordiante
         * @param lY top Y coordinate
         * @param hY bottom Y coordiante 
         */
        public void fillBox(int lX, int hX, int lY, int hY);
        
        /**
         * Method to fill a horizontal scanline [lX,hX] x [y].
         * Both low and high coordiantes are inclusive.
         * Filling might be patterned.
         * @param y Y coordinate
         * @param lX left X coordinate
         * @param hX right X coordiante
         */
        public void fillHorLine(int y, int lX, int hX);
        
        /**
         * Method to fill a verticaltal scanline [x] x [lY,hY].
         * Both low and bigh coordiantes are inclusive.
         * Filling might be patterned.
         * @param x X coordinate
         * @param lY top Y coordinate
         * @param hY bottom Y coordiante
         */
        public void fillVerLine(int x, int lY, int hY);
        
        /**
         * Method to fill a point.
         * Filling might be patterned.
         * @param x X coordinate
         * @param y Y coordinate
         */
        public void fillPoint(int x, int y);
        
        /**
         * Method to draw a horizontal line [lX,hX] x [y].
         * Both low and high coordiantes are inclusive.
         * Drawing is always solid.
         * @param y Y coordinate
         * @param lX left X coordinate
         * @param hX right X coordiante
         */
        public void drawHorLine(int y, int lX, int hX);
        
        /**
         * Method to draw a vertical line [x] x [lY,hY].
         * Both low and high coordiantes are inclusive.
         * Drawing is always solid.
         * @param x X coordinate
         * @param lY top Y coordinate
         * @param hY bottom Y coordiante
         */
        public void drawVerLine(int x, int lY, int hY);
        
        /**
         * Method to draw a point.
         * @param x X coordinate
         * @param y Y coordinate
         */
        public void drawPoint(int x, int y);
        
        /**
         * Method to return Electric Outline style for this ERaster.
         * @return Electric Outline style for this ERaster or null for no outline.
         */
        public EGraphics.Outline getOutline();
    }
    
    
	// ************************************* RENDERING POLY SHAPES *************************************

	/**
	 * Method to draw polygon "poly", transformed through "trans".
	 */
	private void drawPolys(Poly[] polys, AffineTransform trans, boolean forceVisible)
	{
		if (polys == null) return;
		for(int i = 0; i < polys.length; i++)
		{
			// get the polygon and transform it
			Poly poly = polys[i];
			if (poly == null) continue;
			Layer layer = poly.getLayer();
			EGraphics graphics = null;
			boolean dimmed = false;
			if (layer != null)
			{
				if (!forceVisible && !layer.isVisible()) continue;
				graphics = layer.getGraphics();
				dimmed = layer.isDimmed();
			}

			// transform the bounds
			poly.transform(trans);
//			if (wnd.isInPlaceEdit()) poly.transform(wnd.getInPlaceTransformIn());

			// render the polygon
			if (DEBUGRENDERTIMING)
			{
	            long startTime = System.currentTimeMillis();
				renderPoly(poly, graphics, dimmed);
	            renderPolyTime += (System.currentTimeMillis() - startTime);
			} else
			{
				renderPoly(poly, graphics, dimmed);
			}

			// handle refreshing
			periodicRefresh();
		}
	}

	byte [][] getLayerBitMap(int layerNum)
	{
		if (layerNum < 0) return null;

		byte [][] layerBitMap = layerBitMaps[layerNum];
		if (layerBitMap != null) return layerBitMap;
		// allocate this bitplane dynamically
        return newLayerBitMap(layerNum);
	}

    private byte [][] newLayerBitMap(int layerNum) {
        byte [][] layerBitMap= new byte[sz.height][];
        for(int y=0; y<sz.height; y++) {
            byte [] row = new byte[numBytesPerRow];
            for(int x=0; x<numBytesPerRow; x++) row[x] = 0;
            layerBitMap[y] = row;
        }
        layerBitMaps[layerNum] = layerBitMap;
        numLayerBitMapsCreated++;
        return layerBitMap;
        
    }
    
	/**
	 * Render a Poly to the offscreen buffer.
	 */
	private void renderPoly(Poly poly, EGraphics graphics, boolean dimmed)
	{
		byte [][] layerBitMap = null;
		if (graphics != null)
		{
			int layerNum = graphics.getTransparentLayer() - 1;
			if (layerNum < numLayerBitMaps) layerBitMap = getLayerBitMap(layerNum);
		}
		Poly.Type style = poly.getStyle();

		// only do this for lower-level (cached cells)
		if (!renderedWindow)
		{
			// for fills, handle patterned opaque layers specially
			if (style == Poly.Type.FILLED || style == Poly.Type.DISC)
			{
				// see if it is opaque
				if (layerBitMap == null)
				{
					// see if it is patterned
					if (nowPrinting != 0 ? graphics.isPatternedOnPrinter() : graphics.isPatternedOnDisplay())
					{
						Layer layer = poly.getLayer();
						PatternedOpaqueLayer pol = patternedOpaqueLayers.get(layer);
						if (pol == null)
						{
							pol = new PatternedOpaqueLayer(sz.height, numBytesPerRow);
							patternedOpaqueLayers.put(layer, pol);
						}
						layerBitMap = pol.layerBitMap;
						graphics = null;
					}
				}
			}
		}

		// now draw it
		Point2D [] points = poly.getPoints();
		if (style == Poly.Type.FILLED)
		{
			Rectangle2D bounds = poly.getBox();
			if (bounds != null)
			{
				// convert coordinates
				databaseToScreen(bounds.getMinX(), bounds.getMinY(), tempPt1);
				databaseToScreen(bounds.getMaxX(), bounds.getMaxY(), tempPt2);
				int lX = Math.min(tempPt1.x, tempPt2.x);
				int hX = Math.max(tempPt1.x, tempPt2.x);
				int lY = Math.min(tempPt1.y, tempPt2.y);
				int hY = Math.max(tempPt1.y, tempPt2.y);

				// do clipping
				if (lX < 0) lX = 0;
				if (hX >= sz.width) hX = sz.width-1;
				if (lY < 0) lY = 0;
				if (hY >= sz.height) hY = sz.height-1;

				// draw the box
				drawBox(lX, hX, lY, hY, layerBitMap, graphics, dimmed);
				return;
			}
			Point [] intPoints = new Point[points.length];
			for(int i=0; i<points.length; i++) {
                intPoints[i] = new Point();
				databaseToScreen(points[i].getX(), points[i].getY(), intPoints[i]);
            }
			Point [] clippedPoints = GenMath.clipPoly(intPoints, 0, sz.width-1, 0, sz.height-1);
			drawPolygon(clippedPoints, layerBitMap, graphics, dimmed);
			return;
		}
		if (style == Poly.Type.CROSSED)
		{
			databaseToScreen(points[0].getX(), points[0].getY(), tempPt1);
			databaseToScreen(points[1].getX(), points[1].getY(), tempPt2);
			databaseToScreen(points[2].getX(), points[2].getY(), tempPt3);
			databaseToScreen(points[3].getX(), points[3].getY(), tempPt4);
			drawLine(tempPt1, tempPt2, layerBitMap, graphics, 0, dimmed);
			drawLine(tempPt2, tempPt3, layerBitMap, graphics, 0, dimmed);
			drawLine(tempPt3, tempPt4, layerBitMap, graphics, 0, dimmed);
			drawLine(tempPt4, tempPt1, layerBitMap, graphics, 0, dimmed);
			drawLine(tempPt1, tempPt3, layerBitMap, graphics, 0, dimmed);
			drawLine(tempPt2, tempPt4, layerBitMap, graphics, 0, dimmed);
			return;
		}
		if (style.isText())
		{
			Rectangle2D bounds = poly.getBounds2D();
			Rectangle rect = databaseToScreen(bounds);
			TextDescriptor descript = poly.getTextDescriptor();
			String str = poly.getString();
			drawText(rect, style, descript, str, layerBitMap, graphics, dimmed);
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
				databaseToScreen(oldPt.getX(), oldPt.getY(), tempPt1);
				databaseToScreen(newPt.getX(), newPt.getY(), tempPt2);
				drawLine(tempPt1, tempPt2, layerBitMap, graphics, lineType, dimmed);
			}
			if (style == Poly.Type.CLOSED)
			{
				Point2D oldPt = points[points.length-1];
				Point2D newPt = points[0];
				databaseToScreen(oldPt.getX(), oldPt.getY(), tempPt1);
				databaseToScreen(newPt.getX(), newPt.getY(), tempPt2);
				drawLine(tempPt1, tempPt2, layerBitMap, graphics, lineType, dimmed);
			}
			return;
		}
		if (style == Poly.Type.VECTORS)
		{
			for(int j=0; j<points.length; j+=2)
			{
				Point2D oldPt = points[j];
				Point2D newPt = points[j+1];
				databaseToScreen(oldPt.getX(), oldPt.getY(), tempPt1);
				databaseToScreen(newPt.getX(), newPt.getY(), tempPt2);
				drawLine(tempPt1, tempPt2, layerBitMap, graphics, 0, dimmed);
			}
			return;
		}
		if (style == Poly.Type.CIRCLE)
		{
			Point2D center = points[0];
			Point2D edge = points[1];
			databaseToScreen(center.getX(), center.getY(), tempPt1);
			databaseToScreen(edge.getX(), edge.getY(), tempPt2);
			drawCircle(tempPt1, tempPt2, layerBitMap, graphics, dimmed);
			return;
		}
		if (style == Poly.Type.THICKCIRCLE)
		{
			Point2D center = points[0];
			Point2D edge = points[1];
			databaseToScreen(center.getX(), center.getY(), tempPt1);
			databaseToScreen(edge.getX(), edge.getY(), tempPt2);
			drawThickCircle(tempPt1, tempPt2, layerBitMap, graphics, dimmed);
			return;
		}
		if (style == Poly.Type.DISC)
		{
			Point2D center = points[0];
			Point2D edge = points[1];
			databaseToScreen(center.getX(), center.getY(), tempPt1);
			databaseToScreen(edge.getX(), edge.getY(), tempPt2);
			drawDisc(tempPt1, tempPt2, layerBitMap, graphics, dimmed);
			return;
		}
		if (style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC)
		{
			Point2D center = points[0];
			Point2D edge1 = points[1];
			Point2D edge2 = points[2];
			databaseToScreen(center.getX(), center.getY(), tempPt1);
			databaseToScreen(edge1.getX(), edge1.getY(), tempPt2);
			databaseToScreen(edge2.getX(), edge2.getY(), tempPt3);
			drawCircleArc(tempPt1, tempPt2, tempPt3, style == Poly.Type.THICKCIRCLEARC, layerBitMap, graphics, dimmed);
			return;
		}
		if (style == Poly.Type.CROSS)
		{
			// draw the cross
			drawCross(poly, graphics, dimmed);
			return;
		}
		if (style == Poly.Type.BIGCROSS)
		{
			// draw the big cross
			Point2D center = points[0];
			databaseToScreen(center.getX(), center.getY(), tempPt1);
			int size = 5;
			drawLine(new Point(tempPt1.x-size, tempPt1.y), new Point(tempPt1.x+size, tempPt1.y), layerBitMap, graphics, 0, dimmed);
			drawLine(new Point(tempPt1.x, tempPt1.y-size), new Point(tempPt1.x, tempPt1.y+size), layerBitMap, graphics, 0, dimmed);
			return;
		}
	}

	// ************************************* BOX DRAWING *************************************

	int getTheColor(EGraphics desc, boolean dimmed)
	{
		if (nowPrinting == 2) return 0;
		int col = desc.getRGB();
		if (highlightingLayers)
		{
			if (dimmed) col = dimColor(col); else
				col = brightenColor(col);
		}
		return col;
	}

	private double [] hsvTempArray = new double[3];

	/**
	 * Method to dim a color by reducing its saturation.
	 * @param col the color as a 24-bit integer.
	 * @return the dimmed color, a 24-bit integer.
	 */
	private int dimColor(int col)
	{
		int r = col & 0xFF;
		int g = (col >> 8) & 0xFF;
		int b = (col >> 16) & 0xFF;
		fromRGBtoHSV(r, g, b, hsvTempArray);
		hsvTempArray[1] *= 0.2;
		col = fromHSVtoRGB(hsvTempArray[0], hsvTempArray[1], hsvTempArray[2]);
		return col;
	}

	/**
	 * Method to brighten a color by increasing its saturation.
	 * @param col the color as a 24-bit integer.
	 * @return the brightened color, a 24-bit integer.
	 */
	private int brightenColor(int col)
	{
		int r = col & 0xFF;
		int g = (col >> 8) & 0xFF;
		int b = (col >> 16) & 0xFF;
		fromRGBtoHSV(r, g, b, hsvTempArray);
		hsvTempArray[1] *= 1.5;
		if (hsvTempArray[1] > 1) hsvTempArray[1] = 1;
		col = fromHSVtoRGB(hsvTempArray[0], hsvTempArray[1], hsvTempArray[2]);
		return col;
	}

	/**
	 * Method to convert a red/green/blue color to a hue/saturation/intensity color.
	 * Why not use Color.RGBtoHSB?  It doesn't work as well.
	 */
	private void fromRGBtoHSV(int ir, int ig, int ib, double [] hsi)
	{
		double r = ir / 255.0f;
		double g = ig / 255.0f;
		double b = ib / 255.0f;

		// "i" is maximum of "r", "g", and "b"
		hsi[2] = Math.max(Math.max(r, g), b);

		// "x" is minimum of "r", "g", and "b"
		double x = Math.min(Math.min(r, g), b);

		// "saturation" is (i-x)/i
		if (hsi[2] == 0.0) hsi[1] = 0.0; else hsi[1] = (hsi[2] - x) / hsi[2];

		// hue is quadrant-based
		hsi[0] = 0.0;
		if (hsi[1] != 0.0)
		{
			double rdot = (hsi[2] - r) / (hsi[2] - x);
			double gdot = (hsi[2] - g) / (hsi[2] - x);
			double bdot = (hsi[2] - b) / (hsi[2] - x);
			if (b == x && r == hsi[2]) hsi[0] = (1.0 - gdot) / 6.0; else
			if (b == x && g == hsi[2]) hsi[0] = (1.0 + rdot) / 6.0; else
			if (r == x && g == hsi[2]) hsi[0] = (3.0 - bdot) / 6.0; else
			if (r == x && b == hsi[2]) hsi[0] = (3.0 + gdot) / 6.0; else
			if (g == x && b == hsi[2]) hsi[0] = (5.0 - rdot) / 6.0; else
			if (g == x && r == hsi[2]) hsi[0] = (5.0 + bdot) / 6.0; else
				System.out.println("Cannot convert (" + ir + "," + ig + "," + ib + "), for x=" + x + " i=" + hsi[2] + " s=" + hsi[1]);
		}
	}

	/**
	 * Method to convert a hue/saturation/intensity color to a red/green/blue color.
	 * Why not use Color.HSBtoRGB?  It doesn't work as well.
	 */
	private int fromHSVtoRGB(double h, double s, double v)
	{
		h = h * 6.0;
		int i = (int)h;
		double f = h - (double)i;
		double m = v * (1.0 - s);
		double n = v * (1.0 - s * f);
		double k = v * (1.0 - s * (1.0 - f));
		int r = 0, g = 0, b = 0;
		switch (i)
		{
			case 0: r = (int)(v*255.0); g = (int)(k*255.0); b = (int)(m*255.0);   break;
			case 1: r = (int)(n*255.0); g = (int)(v*255.0); b = (int)(m*255.0);   break;
			case 2: r = (int)(m*255.0); g = (int)(v*255.0); b = (int)(k*255.0);   break;
			case 3: r = (int)(m*255.0); g = (int)(n*255.0); b = (int)(v*255.0);   break;
			case 4: r = (int)(k*255.0); g = (int)(m*255.0); b = (int)(v*255.0);   break;
			case 5: r = (int)(v*255.0); g = (int)(m*255.0); b = (int)(n*255.0);   break;
		}
		if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255)
		{
			System.out.println("(" + h + "," + s + "," + v + ") -> (" + r + "," + g + "," + b + ") (i=" + i + ")");
			if (r < 0) r = 0;
			if (r > 255) r = 255;
			if (g < 0) g = 0;
			if (g > 255) g = 255;
			if (b < 0) b = 0;
			if (b > 255) b = 255;
		}
		return (b << 16) | (g << 8) | r;
	}

	/**
	 * Method to draw a box on the off-screen buffer.
	 */
	void drawBox(int lX, int hX, int lY, int hY, byte [][] layerBitMap, EGraphics desc, boolean dimmed)
	{
		// get color and pattern information
		int col = 0;
		int [] pattern = null;
		if (desc != null)
		{
			col = getTheColor(desc, dimmed);
			if (nowPrinting != 0 ? desc.isPatternedOnPrinter() : desc.isPatternedOnDisplay())
				pattern = desc.getPattern();
		}

		// different code for patterned and solid
		if (pattern == null)
		{
			// solid fill
			if (layerBitMap == null)
			{
				// solid fill in opaque area
				for(int y=lY; y<=hY; y++)
				{
					int baseIndex = y * sz.width + lX;
					for(int x=lX; x<=hX; x++)
					{
						int index = baseIndex++;
						int alpha = (opaqueData[index] >> 24) & 0xFF;
						if (alpha == 0xFF) opaqueData[index] = col;
					}
				}
			} else
			{
				// solid fill in transparent layers
				for(int y=lY; y<=hY; y++)
				{
					byte [] row = layerBitMap[y];
					for(int x=lX; x<=hX; x++)
						row[x>>3] |= (1 << (x&7));
				}
			}
		} else
		{
			// patterned fill
			if (layerBitMap == null)
			{
				// patterned fill in opaque area
				for(int y=lY; y<=hY; y++)
				{
					// setup pattern for this row
					int pat = pattern[y&15];
					if (pat == 0) continue;

					int baseIndex = y * sz.width;
					for(int x=lX; x<=hX; x++)
					{
						if ((pat & (0x8000 >> (x&15))) != 0)
							opaqueData[baseIndex + x] = col;
					}
				}
			} else
			{
				// patterned fill in transparent layers
				for(int y=lY; y<=hY; y++)
				{
					// setup pattern for this row
					int pat = pattern[y&15];
					if (pat == 0) continue;

					byte [] row = layerBitMap[y];
					for(int x=lX; x<=hX; x++)
					{
						if ((pat & (0x8000 >> (x&15))) != 0)
							row[x>>3] |= (1 << (x&7));
					}
				}
			}
			EGraphics.Outline o = desc.getOutlined();
			if (o != EGraphics.Outline.NOPAT)
			{
				drawOutline(lX, lY, lX, hY, layerBitMap, col, o.getPattern(), o.getLen());
				drawOutline(lX, hY, hX, hY, layerBitMap, col, o.getPattern(), o.getLen());
				drawOutline(hX, hY, hX, lY, layerBitMap, col, o.getPattern(), o.getLen());
				drawOutline(hX, lY, lX, lY, layerBitMap, col, o.getPattern(), o.getLen());
				if (o.getThickness() != 1)
				{
					for(int i=1; i<o.getThickness(); i++)
					{
						if (lX+i < sz.width)
							drawOutline(lX+i, lY, lX+i, hY, layerBitMap, col, o.getPattern(), o.getLen());
						if (hY-i >= 0)
							drawOutline(lX, hY-i, hX, hY-i, layerBitMap, col, o.getPattern(), o.getLen());
						if (hX-i >= 0)
							drawOutline(hX-i, hY, hX-i, lY, layerBitMap, col, o.getPattern(), o.getLen());
						if (lY+i < sz.height)
							drawOutline(hX, lY+i, lX, lY+i, layerBitMap, col, o.getPattern(), o.getLen());
					}
				}
			}
		}
	}

	// ************************************* LINE DRAWING *************************************

	/**
	 * Method to draw a line on the off-screen buffer.
	 */
	void drawLine(Point pt1, Point pt2, byte [][] layerBitMap, EGraphics desc, int texture, boolean dimmed)
	{
		// first clip the line
		if (GenMath.clipLine(pt1, pt2, 0, sz.width-1, 0, sz.height-1)) return;

		int col = 0;
		if (desc != null) col = getTheColor(desc, dimmed);

		// now draw with the proper line type
		switch (texture)
		{
			case 0: drawSolidLine(pt1.x, pt1.y, pt2.x, pt2.y, layerBitMap, col);          break;
			case 1: drawPatLine(pt1.x, pt1.y, pt2.x, pt2.y, layerBitMap, col, 0x88, 8);   break;
			case 2: drawPatLine(pt1.x, pt1.y, pt2.x, pt2.y, layerBitMap, col, 0xE7, 8);   break;
			case 3: drawThickLine(pt1.x, pt1.y, pt2.x, pt2.y, layerBitMap, col);          break;
		}
	}

	private void drawCross(Poly poly, EGraphics graphics, boolean dimmed)
	{
		Point2D [] points = poly.getPoints();
		databaseToScreen(points[0].getX(), points[0].getY(), tempPt1);
		int size = 3;
		drawLine(new Point(tempPt1.x-size, tempPt1.y), new Point(tempPt1.x+size, tempPt1.y), null, graphics, 0, dimmed);
		drawLine(new Point(tempPt1.x, tempPt1.y-size), new Point(tempPt1.x, tempPt1.y+size), null, graphics, 0, dimmed);
	}

	private void drawSolidLine(int x1, int y1, int x2, int y2, byte [][] layerBitMap, int col)
	{
		// initialize the Bresenham algorithm
		int dx = Math.abs(x2-x1);
		int dy = Math.abs(y2-y1);
		if (dx > dy)
		{
			// initialize for lines that increment along X
			int incr1 = 2 * dy;
			int incr2 = 2 * (dy - dx);
			int d = incr2;
			int x, y, xend, yend, yincr;
			if (x1 > x2)
			{
				x = x2;   y = y2;   xend = x1;   yend = y1;
			} else
			{
				x = x1;   y = y1;   xend = x2;   yend = y2;
			}
			if (yend < y) yincr = -1; else yincr = 1;
			if (layerBitMap == null) opaqueData[y * sz.width + x] = col; else
				layerBitMap[y][x>>3] |= (1 << (x&7));

			// draw line that increments along X
			while (x < xend)
			{
				x++;
				if (d < 0) d += incr1; else
				{
					y += yincr;   d += incr2;
				}
				if (layerBitMap == null) opaqueData[y * sz.width + x] = col; else
					layerBitMap[y][x>>3] |= (1 << (x&7));
			}
		} else
		{
			// initialize for lines that increment along Y
			int incr1 = 2 * dx;
			int incr2 = 2 * (dx - dy);
			int d = incr2;
			int x, y, xend, yend, xincr;
			if (y1 > y2)
			{
				x = x2;   y = y2;   xend = x1;   yend = y1;
			} else
			{
				x = x1;   y = y1;   xend = x2;   yend = y2;
			}
			if (xend < x) xincr = -1; else xincr = 1;
			if (layerBitMap == null) opaqueData[y * sz.width + x] = col; else
				layerBitMap[y][x>>3] |= (1 << (x&7));

			// draw line that increments along X
			while (y < yend)
			{
				y++;
				if (d < 0) d += incr1; else
				{
					x += xincr;   d += incr2;
				}
				if (layerBitMap == null) opaqueData[y * sz.width + x] = col; else
					layerBitMap[y][x>>3] |= (1 << (x&7));
			}
		}
	}

	private void drawOutline(int x1, int y1, int x2, int y2, byte [][] layerBitMap, int col, int pattern, int len)
	{
        tempPt3.x = x1;
        tempPt3.y = y1;
        tempPt4.x = x2;
        tempPt4.y = y2;
        
		// first clip the line
		if (GenMath.clipLine(tempPt3, tempPt4, 0, sz.width-1, 0, sz.height-1)) return;

        drawPatLine(tempPt3.x, tempPt3.y, tempPt4.x, tempPt4.y, layerBitMap, col, pattern, len);
	}

	private void drawPatLine(int x1, int y1, int x2, int y2, byte [][] layerBitMap, int col, int pattern, int len)
	{
		// initialize counter for line style
		int i = 0;

		// initialize the Bresenham algorithm
		int dx = Math.abs(x2-x1);
		int dy = Math.abs(y2-y1);
		if (dx > dy)
		{
			// initialize for lines that increment along X
			int incr1 = 2 * dy;
			int incr2 = 2 * (dy - dx);
			int d = incr2;
			int x, y, xend, yend, yincr;
			if (x1 > x2)
			{
				x = x2;   y = y2;   xend = x1;   yend = y1;
			} else
			{
				x = x1;   y = y1;   xend = x2;   yend = y2;
			}
			if (yend < y) yincr = -1; else yincr = 1;
			if (layerBitMap == null) opaqueData[y * sz.width + x] = col; else
				layerBitMap[y][x>>3] |= (1 << (x&7));

			// draw line that increments along X
			while (x < xend)
			{
				x++;
				if (d < 0) d += incr1; else
				{
					y += yincr;   d += incr2;
				}
				i++;
				if (i == len) i = 0;
				if ((pattern & (1 << i)) == 0) continue;
				if (layerBitMap == null) opaqueData[y * sz.width + x] = col; else
					layerBitMap[y][x>>3] |= (1 << (x&7));
			}
		} else
		{
			// initialize for lines that increment along Y
			int incr1 = 2 * dx;
			int incr2 = 2 * (dx - dy);
			int d = incr2;
			int x, y, xend, yend, xincr;
			if (y1 > y2)
			{
				x = x2;   y = y2;   xend = x1;   yend = y1;
			} else
			{
				x = x1;   y = y1;   xend = x2;   yend = y2;
			}
			if (xend < x) xincr = -1; else xincr = 1;
			if (layerBitMap == null) opaqueData[y * sz.width + x] = col; else
				layerBitMap[y][x>>3] |= (1 << (x&7));

			// draw line that increments along X
			while (y < yend)
			{
				y++;
				if (d < 0) d += incr1; else
				{
					x += xincr;   d += incr2;
				}
				i++;
				if (i == len) i = 0;
				if ((pattern & (1 << i)) == 0) continue;
				if (layerBitMap == null) opaqueData[y * sz.width + x] = col; else
					layerBitMap[y][x>>3] |= (1 << (x&7));
			}
		}
	}

	private void drawThickLine(int x1, int y1, int x2, int y2, byte [][] layerBitMap, int col)
	{
		// initialize the Bresenham algorithm
		int dx = Math.abs(x2-x1);
		int dy = Math.abs(y2-y1);
		if (dx > dy)
		{
			// initialize for lines that increment along X
			int incr1 = 2 * dy;
			int incr2 = 2 * (dy - dx);
			int d = incr2;
			int x, y, xend, yend, yincr;
			if (x1 > x2)
			{
				x = x2;   y = y2;   xend = x1;   yend = y1;
			} else
			{
				x = x1;   y = y1;   xend = x2;   yend = y2;
			}
			if (yend < y) yincr = -1; else yincr = 1;
			drawThickPoint(x, y, layerBitMap, col);

			// draw line that increments along X
			while (x < xend)
			{
				x++;
				if (d < 0) d += incr1; else
				{
					y += yincr;
					d += incr2;
				}
				drawThickPoint(x, y, layerBitMap, col);
			}
		} else
		{
			// initialize for lines that increment along Y
			int incr1 = 2 * dx;
			int incr2 = 2 * (dx - dy);
			int d = incr2;
			int x, y, xend, yend, xincr;
			if (y1 > y2)
			{
				x = x2;   y = y2;   xend = x1;   yend = y1;
			} else
			{
				x = x1;   y = y1;   xend = x2;   yend = y2;
			}
			if (xend < x) xincr = -1; else xincr = 1;
			drawThickPoint(x, y, layerBitMap, col);

			// draw line that increments along X
			while (y < yend)
			{
				y++;
				if (d < 0) d += incr1; else
				{
					x += xincr;
					d += incr2;
				}
				drawThickPoint(x, y, layerBitMap, col);
			}
		}
	}

	// ************************************* POLYGON DRAWING *************************************

	/**
	 * Method to draw a polygon on the off-screen buffer.
	 */
	void drawPolygon(Point [] points, byte [][] layerBitMap, EGraphics desc, boolean dimmed)
	{
		// get color and pattern information
		int col = 0;
		int [] pattern = null;
		if (desc != null)
		{
			col = getTheColor(desc, dimmed);
			if (nowPrinting != 0 ? desc.isPatternedOnPrinter() : desc.isPatternedOnDisplay())
				pattern = desc.getPattern();
		}

		// fill in internal structures
		PolySeg edgelist = null;
		PolySeg [] polySegs = new PolySeg[points.length];
		for(int i=0; i<points.length; i++)
		{
			polySegs[i] = new PolySeg();
			if (i == 0)
			{
				polySegs[i].fx = points[points.length-1].x;
				polySegs[i].fy = points[points.length-1].y;
			} else
			{
				polySegs[i].fx = points[i-1].x;
				polySegs[i].fy = points[i-1].y;
			}
			polySegs[i].tx = points[i].x;   polySegs[i].ty = points[i].y;
		}
		for(int i=0; i<points.length; i++)
		{
			// compute the direction of this edge
			int j = polySegs[i].ty - polySegs[i].fy;
			if (j > 0) polySegs[i].direction = 1; else
				if (j < 0) polySegs[i].direction = -1; else
					polySegs[i].direction = 0;

			// compute the X increment of this edge
			if (j == 0) polySegs[i].increment = 0; else
			{
				polySegs[i].increment = polySegs[i].tx - polySegs[i].fx;
				if (polySegs[i].increment != 0) polySegs[i].increment =
					(polySegs[i].increment * 65536 - j + 1) / j;
			}
			polySegs[i].tx <<= 16;   polySegs[i].fx <<= 16;

			// make sure "from" is above "to"
			if (polySegs[i].fy > polySegs[i].ty)
			{
				j = polySegs[i].tx;
				polySegs[i].tx = polySegs[i].fx;
				polySegs[i].fx = j;
				j = polySegs[i].ty;
				polySegs[i].ty = polySegs[i].fy;
				polySegs[i].fy = j;
			}

			// insert this edge into the edgelist, sorted by ascending "fy"
			if (edgelist == null)
			{
				edgelist = polySegs[i];
				polySegs[i].nextedge = null;
			} else
			{
				// insert by ascending "fy"
				if (edgelist.fy > polySegs[i].fy)
				{
					polySegs[i].nextedge = edgelist;
					edgelist = polySegs[i];
				} else for(PolySeg a = edgelist; a != null; a = a.nextedge)
				{
					if (a.nextedge == null ||
						a.nextedge.fy > polySegs[i].fy)
					{
						// insert after this
						polySegs[i].nextedge = a.nextedge;
						a.nextedge = polySegs[i];
						break;
					}
				}
			}
		}

		// scan polygon and render
		int ycur = 0;
		PolySeg active = null;
		while (active != null || edgelist != null)
		{
			if (active == null)
			{
				active = edgelist;
				active.nextactive = null;
				edgelist = edgelist.nextedge;
				ycur = active.fy;
			}

			// introduce edges from edge list into active list
			while (edgelist != null && edgelist.fy <= ycur)
			{
				// insert "edgelist" into active list, sorted by "fx" coordinate
				if (active.fx > edgelist.fx ||
					(active.fx == edgelist.fx && active.increment > edgelist.increment))
				{
					edgelist.nextactive = active;
					active = edgelist;
					edgelist = edgelist.nextedge;
				} else for(PolySeg a = active; a != null; a = a.nextactive)
				{
					if (a.nextactive == null ||
						a.nextactive.fx > edgelist.fx ||
							(a.nextactive.fx == edgelist.fx &&
								a.nextactive.increment > edgelist.increment))
					{
						// insert after this
						edgelist.nextactive = a.nextactive;
						a.nextactive = edgelist;
						edgelist = edgelist.nextedge;
						break;
					}
				}
			}

			// generate regions to be filled in on current scan line
			int wrap = 0;
			PolySeg left = active;
			for(PolySeg edge = active; edge != null; edge = edge.nextactive)
			{
				wrap = wrap + edge.direction;
				if (wrap == 0)
				{
					int j = (left.fx + 32768) >> 16;
					int k = (edge.fx + 32768) >> 16;

					if (pattern != null)
					{
						int pat = pattern[ycur & 15];
						if (pat != 0)
						{
							if (layerBitMap == null)
							{
								int baseIndex = ycur * sz.width;
								for(int x=j; x<=k; x++)
								{
									if ((pat & (1 << (15-(x&15)))) != 0)
									{
										int index = baseIndex + x;
										opaqueData[index] = col;
									}
								}
							} else
							{
								byte [] row = layerBitMap[ycur];
								for(int x=j; x<=k; x++)
								{
									if ((pat & (1 << (15-(x&15)))) != 0)
										row[x>>3] |= (1 << (x&7));
								}
							}
						}
					} else
					{
						if (layerBitMap == null)
						{
							int baseIndex = ycur * sz.width;
							for(int x=j; x<=k; x++)
							{
								opaqueData[baseIndex + x] = col;
							}
						} else
						{
							byte [] row = layerBitMap[ycur];
							for(int x=j; x<=k; x++)
							{
								row[x>>3] |= (1 << (x&7));
							}
						}
					}
					left = edge.nextactive;
				}
			}
			ycur++;

			// update edges in active list
			PolySeg lastedge = null;
			for(PolySeg edge = active; edge != null; edge = edge.nextactive)
			{
				if (ycur >= edge.ty)
				{
					if (lastedge == null) active = edge.nextactive;
						else lastedge.nextactive = edge.nextactive;
				} else
				{
					edge.fx += edge.increment;
					lastedge = edge;
				}
			}
		}

		// if outlined pattern, draw the outline
		if (pattern != null)
		{
			EGraphics.Outline o = desc.getOutlined();
			if (o != EGraphics.Outline.NOPAT)
			{
				for(int i=0; i<points.length; i++)
				{
					int last = i-1;
					if (last < 0) last = points.length - 1;
					int fX = points[last].x;   int fY = points[last].y;
					int tX = points[i].x;      int tY = points[i].y;
					drawOutline(fX, fY, tX, tY, layerBitMap, col, o.getPattern(), o.getLen());
					if (o.getThickness() != 1)
					{
						int ang = GenMath.figureAngle(new Point2D.Double(fX, fY), new Point2D.Double(tX, tY));
						double sin = DBMath.sin(ang+900);
						double cos = DBMath.cos(ang+900);
						for(int t=1; t<o.getThickness(); t++)
						{
							int dX = (int)(cos*t + 0.5);
							int dY = (int)(sin*t + 0.5);
							drawOutline(fX+dX, fY+dY, tX+dX, tY+dY, layerBitMap, col, o.getPattern(), o.getLen());
						}
					}
				}
			}
		}
	}

	// ************************************* TEXT DRAWING *************************************

    /**
     * Method to draw a text on the off-screen buffer with default values
     * @param s
     */
    public void drawText(String s, Rectangle rect)
    {
        if (noCellTextDescriptor == null)
            noCellTextDescriptor = TextDescriptor.EMPTY.withAbsSize(18).withBold(true);
        textColor = new Color(User.getColorText());
		textGraphics.setColor(textColor);
        drawText(rect, Poly.Type.TEXTBOX, noCellTextDescriptor, s, null, textGraphics, false);
    }

	/**
	 * Method to draw a text on the off-screen buffer
	 */
	public void drawText(Rectangle rect, Poly.Type style, TextDescriptor descript, String s, byte [][] layerBitMap, EGraphics desc, boolean dimmed)
	{
		// quit if string is null
		if (s == null) return;
		int len = s.length();
		if (len == 0) return;

		// get parameters
		int col = User.getColorText() & 0xFFFFFF;
		if (desc != null) col = getTheColor(desc, dimmed);

		// get text description
		int size = EditWindow.getDefaultFontSize();
		int fontStyle = Font.PLAIN;
		String fontName = User.getDefaultFont();
		boolean italic = false;
		boolean bold = false;
		boolean underline = false;
		int rotation = 0;
		int greekScale = 0;
		int shiftUp = 0;
		if (descript != null)
		{
			rotation = descript.getRotation().getIndex();
			int colorIndex = descript.getColorIndex();
			if (colorIndex != 0)
			{
				Color full = EGraphics.getColorFromIndex(colorIndex);
				if (full != null) col = full.getRGB() & 0xFFFFFF;
			}
			double dSize = descript.getTrueSize(scale);
			size = (int)dSize;
			if (size < MINIMUMTEXTSIZE)
			{
				// text too small: scale it to get proper size
				greekScale = 2;
				for(;;)
				{
					size = (int)(dSize * greekScale);
					if (size >= MINIMUMTEXTSIZE) break;
					greekScale *= 2;
				}
			}

			// prevent exceedingly large text
			while (size > MAXIMUMTEXTSIZE)
			{
				size /= 2;
				shiftUp++;
			}

			italic = descript.isItalic();
			bold = descript.isBold();
			underline = descript.isUnderline();
			int fontIndex = descript.getFace();
			if (fontIndex != 0)
			{
				TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(fontIndex);
				if (af != null) fontName = af.getName();
			}
		}

		// get box information for limiting text size
		if (style == Poly.Type.TEXTBOX)
		{
            if (rect.x >= sz.width || rect.x + rect.width < 0 || rect.y >= sz.height || rect.y + rect.height < 0)
                return;
        }

		// create RenderInfo
		long startTime = 0;
        if (DEBUGRENDERTIMING) System.currentTimeMillis();
		RenderTextInfo renderInfo = new RenderTextInfo();
		if (!renderInfo.buildInfo(s, fontName, size, italic, bold, underline, rect, style, rotation))
			return;

		// if text was made "greek", just draw a line
		if (greekScale != 0)
		{
			// text too small: make it "greek"
	        int width = (int)renderInfo.bounds.getWidth() / greekScale;
			int sizeIndent = (size/greekScale+1) / 4;
			Point pt = getTextCorner(width, size/greekScale, style, rect, rotation);

			// do clipping
			int lX = pt.x;   int hX = lX + width;
			int lY = pt.y + sizeIndent;   int hY = lY;
			if (lX < 0) lX = 0;
			if (hX >= sz.width) hX = sz.width-1;
			if (lY < 0) lY = 0;
			if (hY >= sz.height) hY = sz.height-1;

			drawBox(lX, hX, lY, hY, layerBitMap, desc, dimmed);
			return;
		}

		// check if text is on-screen
        if (renderInfo.bounds.getMinX() >= sz.width || renderInfo.bounds.getMaxX() < 0 ||
            renderInfo.bounds.getMinY() >= sz.height || renderInfo.bounds.getMaxY() < 0)
                return;

		// render the text
		Raster ras = renderText(renderInfo);
		if (DEBUGRENDERTIMING) renderTextTime += (System.currentTimeMillis() - startTime);
		if (ras == null) return;
		int rasWidth = (int)renderInfo.rasBounds.getWidth() << shiftUp;
		int rasHeight = (int)renderInfo.rasBounds.getHeight() << shiftUp;
		Point pt = getTextCorner(rasWidth, rasHeight, style, rect, rotation);
		int atX = pt.x;
		int atY = pt.y;
		DataBufferByte dbb = (DataBufferByte)ras.getDataBuffer();
		byte [] samples = dbb.getData();

		int sx, ex;
		switch (rotation)
		{
			case 0:			// no rotation
				if (atX < 0) sx = -atX; else sx = 0;
				if (atX+rasWidth >= sz.width) ex = sz.width-1 - atX; else
					ex = rasWidth;
				for(int y=0; y<rasHeight; y++)
				{
					int trueY = atY + y;
					if (trueY < 0 || trueY >= sz.height) continue;

					// setup pointers for filling this row
					byte [] row = null;
					int baseIndex = 0;
					if (layerBitMap == null) baseIndex = trueY * sz.width; else
						row = layerBitMap[trueY];
					int samp = (y>>shiftUp) * textImageWidth;
					for(int x=sx; x<ex; x++)
					{
						int trueX = atX + x;
						int alpha = samples[samp + (x>>shiftUp)] & 0xFF;
						if (alpha == 0) continue;
						if (layerBitMap == null)
						{
							// drawing opaque
							int fullIndex = baseIndex + trueX;
							int pixelValue = opaqueData[fullIndex];
							int oldAlpha = (pixelValue >> 24) & 0xFF;
							int color = col;
							if (oldAlpha == 0)
							{
								// blend with opaque
								if (alpha != 0xFF) color = alphaBlend(color, pixelValue, alpha);
							} else if (oldAlpha == 0xFF)
							{
								// blend with background
								if (alpha < 255) color = (color & 0xFFFFFF) | (alpha << 24);
							}
							opaqueData[fullIndex] = color;
						} else
						{
							// draw in a transparent layer
							if (alpha >= 128) row[trueX>>3] |= (1 << (trueX&7));
						}
					}
				}
				break;
			case 1:			// 90 degrees counterclockwise
				if (atX < 0) sx = -atX; else sx = 0;
				if (atX >= sz.width) ex = sz.width - atX; else
					ex = rasHeight;
				for(int y=0; y<rasWidth; y++)
				{
					int trueY = atY - y;
					if (trueY < 0 || trueY >= sz.height) continue;

					// setup pointers for filling this row
					byte [] row = null;
					int baseIndex = 0;
					if (layerBitMap == null) baseIndex = trueY * sz.width; else
						row = layerBitMap[trueY];
					for(int x=sx; x<ex; x++)
					{
						int trueX = atX + x;
						int alpha = samples[(x>>shiftUp) * textImageWidth + (y>>shiftUp)] & 0xFF;
						if (alpha == 0) continue;
						if (layerBitMap == null)
						{
							// drawing opaque
							int fullIndex = baseIndex + trueX;
							int pixelValue = opaqueData[fullIndex];
							int oldAlpha = (pixelValue >> 24) & 0xFF;
							int color = col;
							if (oldAlpha == 0)
							{
								// blend with opaque
								if (alpha != 0xFF) color = alphaBlend(color, pixelValue, alpha);
							} else if (oldAlpha == 0xFF)
							{
								// blend with background
								if (alpha < 255) color = (color & 0xFFFFFF) | (alpha << 24);
							}
							opaqueData[fullIndex] = color;
						} else
						{
							if (alpha >= 128) row[trueX>>3] |= (1 << (trueX&7));
						}
					}
				}
				break;
			case 2:			// 180 degrees
				atX -= rasWidth;
				atY -= rasHeight;
				if (atX < 0) sx = -atX; else sx = 0;
				if (atX+rasWidth >= sz.width) ex = sz.width-1 - atX; else
					ex = rasWidth;

				for(int y=0; y<rasHeight; y++)
				{
					int trueY = atY + y;
					if (trueY < 0 || trueY >= sz.height) continue;

					// setup pointers for filling this row
					byte [] row = null;
					int baseIndex = 0;
					if (layerBitMap == null) baseIndex = trueY * sz.width; else
						row = layerBitMap[trueY];
					for(int x=sx; x<ex; x++)
					{
						int trueX = atX + x;
						int index = ((rasHeight-y-1)>>shiftUp) * textImageWidth + ((rasWidth-x-1)>>shiftUp);
						int alpha = samples[index] & 0xFF;
						if (alpha == 0) continue;
						if (layerBitMap == null)
						{
							// drawing opaque
							int fullIndex = baseIndex + trueX;
							int pixelValue = opaqueData[fullIndex];
							int oldAlpha = (pixelValue >> 24) & 0xFF;
							int color = col;
							if (oldAlpha == 0)
							{
								// blend with opaque
								if (alpha != 0xFF) color = alphaBlend(color, pixelValue, alpha);
							} else if (oldAlpha == 0xFF)
							{
								// blend with background
								if (alpha < 255) color = (color & 0xFFFFFF) | (alpha << 24);
							}
							opaqueData[fullIndex] = color;
						} else
						{
							if (alpha >= 128) row[trueX>>3] |= (1 << (trueX&7));
						}
					}
				}
				break;
			case 3:			// 90 degrees clockwise
				if (atX < 0) sx = -atX; else sx = 0;
				if (atX >= sz.width) ex = sz.width - atX; else
					ex = rasHeight;
				for(int y=0; y<rasWidth; y++)
				{
					int trueY = atY + y;
					if (trueY < 0 || trueY >= sz.height) continue;

					// setup pointers for filling this row
					byte [] row = null;
					int baseIndex = 0;
					if (layerBitMap == null) baseIndex = trueY * sz.width; else
						row = layerBitMap[trueY];
					for(int x=sx; x<ex; x++)
					{
						int trueX = atX - x;
						int alpha = samples[(x>>shiftUp) * textImageWidth + (y>>shiftUp)] & 0xFF;
						if (alpha == 0) continue;
						if (layerBitMap == null)
						{
							// drawing opaque
							int fullIndex = baseIndex + trueX;
							int pixelValue = opaqueData[fullIndex];
							int oldAlpha = (pixelValue >> 24) & 0xFF;
							int color = col;
							if (oldAlpha == 0)
							{
								// blend with opaque
								if (alpha != 0xFF) color = alphaBlend(color, pixelValue, alpha);
							} else if (oldAlpha == 0xFF)
							{
								// blend with background
								if (alpha < 255) color = (color & 0xFFFFFF) | (alpha << 24);
							}
							opaqueData[fullIndex] = color;
						} else
						{
							if (alpha >= 128) row[trueX>>3] |= (1 << (trueX&7));
						}
					}
				}
				break;
		}
	}

	private int alphaBlend(int color, int backgroundColor, int alpha)
	{
		int red = (color >> 16) & 0xFF;
		int green = (color >> 8) & 0xFF;
		int blue = color & 0xFF;
		int inverseAlpha = 254 - alpha;
		int redBack = (backgroundColor >> 16) & 0xFF;
		int greenBack = (backgroundColor >> 8) & 0xFF;
		int blueBack = backgroundColor & 0xFF;
		red = ((red * alpha) + (redBack * inverseAlpha)) / 255;
		green = ((green * alpha) + (greenBack * inverseAlpha)) / 255;
		blue = ((blue * alpha) + (blueBack * inverseAlpha)) / 255;
		color = (red << 16) | (green << 8) + blue;
		return color;
	}

	private static class RenderTextInfo {
		private Font font;
		private GlyphVector gv;
		private LineMetrics lm;
		private Point2D anchorPoint;
	    private Rectangle2D rasBounds;              // the raster bounds of the unrotated text, in pixels (screen units)
	    private Rectangle2D bounds;                 // the real bounds of the rotated, anchored text (in screen units)
	    private boolean underline;

	    private boolean buildInfo(String msg, String fontName, int tSize, boolean italic, boolean bold, boolean underline,
	    	Rectangle probableBoxedBounds, Poly.Type style, int rotation)
	    {
			font = getFont(msg, fontName, tSize, italic, bold, underline);
			this.underline = underline;

			// convert the text to a GlyphVector
			FontRenderContext frc = new FontRenderContext(null, true, true);
			gv = font.createGlyphVector(frc, msg);
			lm = font.getLineMetrics(msg, frc);

			// figure bounding box of text
			Rectangle2D rasRect = gv.getLogicalBounds();
			int width = (int)rasRect.getWidth();
			int height = (int)(lm.getHeight()+0.5);
			if (width <= 0 || height <= 0) return false;
			int fontStyle = font.getStyle();

			int boxedWidth = (int)probableBoxedBounds.getWidth();
			int boxedHeight = (int)probableBoxedBounds.getHeight();

			// if text is to be "boxed", make sure it fits
			if (boxedWidth > 1 && boxedHeight > 1)
            {
                if (width > boxedWidth || height > boxedHeight)
                {
                    double scale = Math.min((double)boxedWidth / width, (double)boxedHeight / height);
                    font = new Font(fontName, fontStyle, (int)(tSize*scale));
                    if (font != null)
                    {
                        // convert the text to a GlyphVector
                        gv = font.createGlyphVector(frc, msg);
                        lm = font.getLineMetrics(msg, frc);
                        rasRect = gv.getLogicalBounds();
                        height = (int)(lm.getHeight()+0.5);
                        if (height <= 0) return false;
                        width = (int)rasRect.getWidth();
                    }
                }
            }
			if (underline) height++;
			rasBounds = new Rectangle2D.Double(0, (float)lm.getAscent()-lm.getLeading(), width, height);

			anchorPoint = getTextCorner(width, height, style, probableBoxedBounds, rotation);
			if (rotation == 1 || rotation == 3)
            {
                bounds = new Rectangle2D.Double(anchorPoint.getX(), anchorPoint.getY(), height, width);
            } else
            {
                bounds = new Rectangle2D.Double(anchorPoint.getX(), anchorPoint.getY(), width, height);
            }
            return true;
        }
    }

	/**
	 * Method to return the coordinates of the lower-left corner of text in this window.
	 * @param rasterWidth the width of the text.
	 * @param rasterHeight the height of the text.
	 * @param style the anchor information for the text.
	 * @param rect the bounds of the polygon containing the text.
	 * @param rotation the rotation of the text (0=normal, 1=90 counterclockwise, 2=180, 3=90 clockwise).
	 * @return the coordinates of the lower-left corner of the text.
	 */
	private static Point getTextCorner(int rasterWidth, int rasterHeight, Poly.Type style, Rectangle rect, int rotation)
	{
		// adjust to place text in the center
		int textWidth = rasterWidth;
		int textHeight = rasterHeight;
		int offX = 0, offY = 0;
		if (style == Poly.Type.TEXTCENT)
		{
			offX = -textWidth/2;
			offY = -textHeight/2;
		} else if (style == Poly.Type.TEXTTOP)
		{
			offX = -textWidth/2;
		} else if (style == Poly.Type.TEXTBOT)
		{
			offX = -textWidth/2;
			offY = -textHeight;
		} else if (style == Poly.Type.TEXTLEFT)
		{
			offY = -textHeight/2;
		} else if (style == Poly.Type.TEXTRIGHT)
		{
			offX = -textWidth;
			offY = -textHeight/2;
		} else if (style == Poly.Type.TEXTTOPLEFT)
		{
		} else if (style == Poly.Type.TEXTBOTLEFT)
		{
			offY = -textHeight;
		} else if (style == Poly.Type.TEXTTOPRIGHT)
		{
			offX = -textWidth;
		} else if (style == Poly.Type.TEXTBOTRIGHT)
		{
			offX = -textWidth;
			offY = -textHeight;
		} if (style == Poly.Type.TEXTBOX)
		{
//			if (textWidth > rect.getWidth())
//			{
//				// text too big for box: scale it down
//				textScale *= rect.getWidth() / textWidth;
//			}
			offX = -textWidth/2;
			offY = -textHeight/2;
		}
		if (rotation != 0)
		{
			int saveOffX = offX;
			switch (rotation)
			{
				case 1:
					offX = offY;
					offY = -saveOffX;
					break;
				case 2:
					offX = -offX;
					offY = -offY;
					break;
				case 3:
					offX = -offY;
					offY = saveOffX;
					break;
			}
		}
		int cX = (int)rect.getCenterX() + offX;
		int cY = (int)rect.getCenterY() + offY;
		return new Point(cX, cY);
	}

	private int textImageWidth = 0, textImageHeight = 0;
	private BufferedImage textImage = null;
	private Graphics2D textImageGraphics;

    private Raster renderText(RenderTextInfo renderInfo) {

        Font theFont = renderInfo.font;
        if (theFont == null) return null;

        int width = (int)renderInfo.rasBounds.getWidth();
        int height = (int)renderInfo.rasBounds.getHeight();
        GlyphVector gv = renderInfo.gv;
        LineMetrics lm = renderInfo.lm;

	    // Too small to appear in the screen
	    if (width <= 0 || height <= 0)
		    return null;

		// if the new image is larger than what is saved, must rebuild
		if (width > textImageWidth || height > textImageHeight)
			textImage = null;
		if (textImage == null)
		{
			// create a new text buffer
			textImageWidth = width;
			textImageHeight = height;
			textImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
			textImageGraphics = textImage.createGraphics();
		} else
		{
			// clear and reuse the existing text buffer
			textImageGraphics.setColor(Color.BLACK);
			textImageGraphics.fillRect(0, 0, width, height);
		}

		// now render it
		Graphics2D g2 = (Graphics2D)textImage.getGraphics();
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g2.setColor(new Color(255,255,255));
		g2.drawGlyphVector(gv, (float)-renderInfo.rasBounds.getX(), (float)(lm.getAscent()-lm.getLeading()));
		if (renderInfo.underline)
		{
			g2.drawLine(0, height-1, width-1, height-1);
		}

		// return the bits
		return textImage.getData();
	}

    public static Font getFont(String msg, String font, int tSize, boolean italic, boolean bold, boolean underline) {
        // get the font
        int fontStyle = Font.PLAIN;
        if (italic) fontStyle |= Font.ITALIC;
        if (bold) fontStyle |= Font.BOLD;
        Font theFont = new Font(font, fontStyle, tSize);
        if (theFont == null)
        {
            System.out.println("Could not find font "+font+" to render text: "+msg);
            return null;
        }
        return theFont;
    }

	// ************************************* CIRCLE DRAWING *************************************

	/**
	 * Method to draw a circle on the off-screen buffer
	 */
	void drawCircle(Point center, Point edge, byte [][] layerBitMap, EGraphics desc, boolean dimmed)
	{
		// get parameters
		int radius = (int)center.distance(edge);
		int col = 0;
		if (desc != null) col = getTheColor(desc, dimmed);

		// set redraw area
		int left = center.x - radius;
		int right = center.x + radius + 1;
		int top = center.y - radius;
		int bottom = center.y + radius + 1;

		int x = 0;   int y = radius;
		int d = 3 - 2 * radius;
		if (left >= 0 && right < sz.width && top >= 0 && bottom < sz.height)
		{
			// no clip version is faster
			while (x <= y)
			{
				if (layerBitMap == null)
				{
					int baseIndex = (center.y + y) * sz.width;
					opaqueData[baseIndex + (center.x+x)] = col;
					opaqueData[baseIndex + (center.x-x)] = col;

					baseIndex = (center.y - y) * sz.width;
					opaqueData[baseIndex + (center.x+x)] = col;
					opaqueData[baseIndex + (center.x-x)] = col;

					baseIndex = (center.y + x) * sz.width;
					opaqueData[baseIndex + (center.x+y)] = col;
					opaqueData[baseIndex + (center.x-y)] = col;

					baseIndex = (center.y - x) * sz.width;
					opaqueData[baseIndex + (center.x+y)] = col;
					opaqueData[baseIndex + (center.x-y)] = col;
				} else
				{
					byte [] row = layerBitMap[center.y + y];
					row[(center.x+x)>>3] |= (1 << ((center.x+x)&7));
					row[(center.x-x)>>3] |= (1 << ((center.x-x)&7));

					row = layerBitMap[center.y - y];
					row[(center.x+x)>>3] |= (1 << ((center.x+x)&7));
					row[(center.x-x)>>3] |= (1 << ((center.x-x)&7));

					row = layerBitMap[center.y + x];
					row[(center.x+y)>>3] |= (1 << ((center.x+y)&7));
					row[(center.x-y)>>3] |= (1 << ((center.x-y)&7));

					row = layerBitMap[center.y - x];
					row[(center.x+y)>>3] |= (1 << ((center.x+y)&7));
					row[(center.x-y)>>3] |= (1 << ((center.x-y)&7));
				}

				if (d < 0) d += 4*x + 6; else
				{
					d += 4 * (x-y) + 10;
					y--;
				}
				x++;
			}
		} else
		{
			// clip version
			while (x <= y)
			{
				int thisy = center.y + y;
				if (thisy >= 0 && thisy < sz.height)
				{
					int thisx = center.x + x;
					if (thisx >= 0 && thisx < sz.width)
						drawPoint(thisx, thisy, layerBitMap, col);
					thisx = center.x - x;
					if (thisx >= 0 && thisx < sz.width)
						drawPoint(thisx, thisy, layerBitMap, col);
				}

				thisy = center.y - y;
				if (thisy >= 0 && thisy < sz.height)
				{
					int thisx = center.x + x;
					if (thisx >= 0 && thisx < sz.width)
						drawPoint(thisx, thisy, layerBitMap, col);
					thisx = center.x - x;
					if (thisx >= 0 && thisx < sz.width)
						drawPoint(thisx, thisy, layerBitMap, col);
				}

				thisy = center.y + x;
				if (thisy >= 0 && thisy < sz.height)
				{
					int thisx = center.x + y;
					if (thisx >= 0 && thisx < sz.width)
						drawPoint(thisx, thisy, layerBitMap, col);
					thisx = center.x - y;
					if (thisx >= 0 && thisx < sz.width)
						drawPoint(thisx, thisy, layerBitMap, col);
				}

				thisy = center.y - x;
				if (thisy >= 0 && thisy < sz.height)
				{
					int thisx = center.x + y;
					if (thisx >= 0 && thisx < sz.width)
						drawPoint(thisx, thisy, layerBitMap, col);
					thisx = center.x - y;
					if (thisx >= 0 && thisx < sz.width)
						drawPoint(thisx, thisy, layerBitMap, col);
				}

				if (d < 0) d += 4*x + 6; else
				{
					d += 4 * (x-y) + 10;
					y--;
				}
				x++;
			}
		}
	}

	/**
	 * Method to draw a thick circle on the off-screen buffer
	 */
	void drawThickCircle(Point center, Point edge, byte [][] layerBitMap, EGraphics desc, boolean dimmed)
	{
		// get parameters
		int radius = (int)center.distance(edge);
		int col = 0;
		if (desc != null) col = getTheColor(desc, dimmed);

		int x = 0;   int y = radius;
		int d = 3 - 2 * radius;
		while (x <= y)
		{
			int thisy = center.y + y;
			if (thisy >= 0 && thisy < sz.height)
			{
				int thisx = center.x + x;
				if (thisx >= 0 && thisx < sz.width)
					drawThickPoint(thisx, thisy, layerBitMap, col);
				thisx = center.x - x;
				if (thisx >= 0 && thisx < sz.width)
					drawThickPoint(thisx, thisy, layerBitMap, col);
			}

			thisy = center.y - y;
			if (thisy >= 0 && thisy < sz.height)
			{
				int thisx = center.x + x;
				if (thisx >= 0 && thisx < sz.width)
					drawThickPoint(thisx, thisy, layerBitMap, col);
				thisx = center.x - x;
				if (thisx >= 0 && thisx < sz.width)
					drawThickPoint(thisx, thisy, layerBitMap, col);
			}

			thisy = center.y + x;
			if (thisy >= 0 && thisy < sz.height)
			{
				int thisx = center.x + y;
				if (thisx >= 0 && thisx < sz.width)
					drawThickPoint(thisx, thisy, layerBitMap, col);
				thisx = center.x - y;
				if (thisx >= 0 && thisx < sz.width)
					drawThickPoint(thisx, thisy, layerBitMap, col);
			}

			thisy = center.y - x;
			if (thisy >= 0 && thisy < sz.height)
			{
				int thisx = center.x + y;
				if (thisx >= 0 && thisx < sz.width)
					drawThickPoint(thisx, thisy, layerBitMap, col);
				thisx = center.x - y;
				if (thisx >= 0 && thisx < sz.width)
					drawThickPoint(thisx, thisy, layerBitMap, col);
			}

			if (d < 0) d += 4*x + 6; else
			{
				d += 4 * (x-y) + 10;
				y--;
			}
			x++;
		}
	}

	// ************************************* DISC DRAWING *************************************

	/**
	 * Method to draw a scan line of the filled-in circle of radius "radius"
	 */
	private void drawDiscRow(int thisy, int startx, int endx, byte [][] layerBitMap, int col, int [] pattern)
	{
		if (thisy < 0 || thisy >= sz.height) return;
		if (startx < 0) startx = 0;
		if (endx >= sz.width) endx = sz.width - 1;
		if (pattern != null)
		{
			int pat = pattern[thisy & 15];
			if (pat != 0)
			{
				if (layerBitMap == null)
				{
					int baseIndex = thisy * sz.width;
					for(int x=startx; x<=endx; x++)
					{
						if ((pat & (1 << (15-(x&15)))) != 0)
							opaqueData[baseIndex + x] = col;
					}
				} else
				{
					byte [] row = layerBitMap[thisy];
					for(int x=startx; x<=endx; x++)
					{
						if ((pat & (1 << (15-(x&15)))) != 0)
							row[x>>3] |= (1 << (x&7));
					}
				}
			}
		} else
		{
			if (layerBitMap == null)
			{
				int baseIndex = thisy * sz.width;
				for(int x=startx; x<=endx; x++)
				{
					int index = baseIndex + x;
					int alpha = (opaqueData[index] >> 24) & 0xFF;
					if (alpha == 0xFF) opaqueData[index] = col;
				}
			} else
			{
				byte [] row = layerBitMap[thisy];
				for(int x=startx; x<=endx; x++)
				{
					row[x>>3] |= (1 << (x&7));
				}
			}
		}
	}

	/**
	 * Method to draw a filled-in circle of radius "radius" on the off-screen buffer
	 */
	void drawDisc(Point center, Point edge, byte [][] layerBitMap, EGraphics desc, boolean dimmed)
	{
		// get parameters
		int radius = (int)center.distance(edge);
		int col = 0;
		int [] pattern = null;
		if (desc != null)
		{
			col = getTheColor(desc, dimmed);
			if (nowPrinting != 0 ? desc.isPatternedOnPrinter() : desc.isPatternedOnDisplay())
			{
				pattern = desc.getPattern();
				if (desc.getOutlined() != EGraphics.Outline.NOPAT)
				{
					drawCircle(center, edge, layerBitMap, desc, dimmed);			
				}
			}
		}

		// set redraw area
		int left = center.x - radius;
		int right = center.x + radius + 1;
		int top = center.y - radius;
		int bottom = center.y + radius + 1;

		if (radius == 1)
		{
			// just fill the area for discs this small
			if (left < 0) left = 0;
			if (right >= sz.width) right = sz.width - 1;
			for(int y=top; y<bottom; y++)
			{
				if (y < 0 || y >= sz.height) continue;
				for(int x=left; x<right; x++)
					drawPoint(x, y, layerBitMap, col);
			}
			return;
		}

		int x = 0;   int y = radius;
		int d = 3 - 2 * radius;
		while (x <= y)
		{
			drawDiscRow(center.y+y, center.x-x, center.x+x, layerBitMap, col, pattern);
			drawDiscRow(center.y-y, center.x-x, center.x+x, layerBitMap, col, pattern);
			drawDiscRow(center.y+x, center.x-y, center.x+y, layerBitMap, col, pattern);
			drawDiscRow(center.y-x, center.x-y, center.x+y, layerBitMap, col, pattern);

			if (d < 0) d += 4*x + 6; else
			{
				d += 4 * (x-y) + 10;
				y--;
			}
			x++;
		}
	}

	// ************************************* ARC DRAWING *************************************

	private boolean [] arcOctTable = new boolean[9];
	private Point      arcCenter;
	private int        arcRadius;
	private int        arcCol;
	private byte [][]  arcLayerBitMap;
	private boolean    arcThick;

	private int arcFindOctant(int x, int y)
	{
		if (x > 0)
		{
			if (y >= 0)
			{
				if (y >= x) return 7;
				return 8;
			}
			if (x >= -y) return 1;
			return 2;
		}
		if (y > 0)
		{
			if (y > -x) return 6;
			return 5;
		}
		if (y > x) return 4;
		return 3;
	}

	private Point arcXformOctant(int x, int y, int oct)
	{
		switch (oct)
		{
			case 1 : return new Point(-y,  x);
			case 2 : return new Point( x, -y);
			case 3 : return new Point(-x, -y);
			case 4 : return new Point(-y, -x);
			case 5 : return new Point( y, -x);
			case 6 : return new Point(-x,  y);
			case 7 : return new Point( x,  y);
			case 8 : return new Point( y,  x);
		}
		return null;
	}

	private void arcDoPixel(int x, int y)
	{
		if (x < 0 || x >= sz.width || y < 0 || y >= sz.height) return;
		if (arcThick)
		{
			drawThickPoint(x, y, arcLayerBitMap, arcCol);
		} else
		{
			drawPoint(x, y, arcLayerBitMap, arcCol);
		}
	}

	private void arcOutXform(int x, int y)
	{
		if (arcOctTable[1]) arcDoPixel( y + arcCenter.x, -x + arcCenter.y);
		if (arcOctTable[2]) arcDoPixel( x + arcCenter.x, -y + arcCenter.y);
		if (arcOctTable[3]) arcDoPixel(-x + arcCenter.x, -y + arcCenter.y);
		if (arcOctTable[4]) arcDoPixel(-y + arcCenter.x, -x + arcCenter.y);
		if (arcOctTable[5]) arcDoPixel(-y + arcCenter.x,  x + arcCenter.y);
		if (arcOctTable[6]) arcDoPixel(-x + arcCenter.x,  y + arcCenter.y);
		if (arcOctTable[7]) arcDoPixel( x + arcCenter.x,  y + arcCenter.y);
		if (arcOctTable[8]) arcDoPixel( y + arcCenter.x,  x + arcCenter.y);
	}

	private void arcBresCW(Point pt, Point pt1)
	{
		int d = 3 - 2 * pt.y + 4 * pt.x;
		while (pt.x < pt1.x && pt.y > pt1.y)
		{
			arcOutXform(pt.x, pt.y);
			if (d < 0) d += 4 * pt.x + 6; else
			{
				d += 4 * (pt.x-pt.y) + 10;
				pt.y--;
			}
			pt.x++;
		}

		// get to the end
		for ( ; pt.x < pt1.x; pt.x++) arcOutXform(pt.x, pt.y);
		for ( ; pt.y > pt1.y; pt.y--) arcOutXform(pt.x, pt.y);
		arcOutXform(pt1.x, pt1.y);
	}

	private void arcBresMidCW(Point pt)
	{
		int d = 3 - 2 * pt.y + 4 * pt.x;
		while (pt.x < pt.y)
		{
			arcOutXform(pt.x, pt.y);
			if (d < 0) d += 4 * pt.x + 6; else
			{
				d += 4 * (pt.x-pt.y) + 10;
				pt.y--;
			}
			pt.x++;
	   }
	   if (pt.x == pt.y) arcOutXform(pt.x, pt.y);
	}

	private void arcBresMidCCW(Point pt)
	{
		int d = 3 + 2 * pt.y - 4 * pt.x;
		while (pt.x > 0)
		{
			arcOutXform(pt.x, pt.y);
			if (d > 0) d += 6-4 * pt.x; else
			{
				d += 4 * (pt.y-pt.x) + 10;
				pt.y++;
			}
			pt.x--;
	   }
	   arcOutXform(0, arcRadius);
	}

	private void arcBresCCW(Point pt, Point pt1)
	{
		int d = 3 + 2 * pt.y + 4 * pt.x;
		while(pt.x > pt1.x && pt.y < pt1.y)
		{
			// not always correct
			arcOutXform(pt.x, pt.y);
			if (d > 0) d += 6 - 4 * pt.x; else
			{
				d += 4 * (pt.y-pt.x) + 10;
				pt.y++;
			}
			pt.x--;
		}

		// get to the end
		for ( ; pt.x > pt1.x; pt.x--) arcOutXform(pt.x, pt.y);
		for ( ; pt.y < pt1.y; pt.y++) arcOutXform(pt.x, pt.y);
		arcOutXform(pt1.x, pt1.y);
	}

	/**
	 * draws an arc centered at (centerx, centery), clockwise,
	 * passing by (x1,y1) and (x2,y2)
	 */
	void drawCircleArc(Point center, Point p1, Point p2, boolean thick, byte [][] layerBitMap, EGraphics desc, boolean dimmed)
	{
		// ignore tiny arcs
		if (p1.x == p2.x && p1.y == p2.y) return;

		// get parameters
		arcLayerBitMap = layerBitMap;
		arcCol = 0;
		if (desc != null) arcCol = getTheColor(desc, dimmed);

		arcCenter = center;
		int pa_x = p2.x - arcCenter.x;
		int pa_y = p2.y - arcCenter.y;
		int pb_x = p1.x - arcCenter.x;
		int pb_y = p1.y - arcCenter.y;
		arcRadius = (int)arcCenter.distance(p2);
		int alternate = (int)arcCenter.distance(p1);
		int start_oct = arcFindOctant(pa_x, pa_y);
		int end_oct   = arcFindOctant(pb_x, pb_y);
		arcThick = thick;

		// move the point
		if (arcRadius != alternate)
		{
			int diff = arcRadius-alternate;
			switch (end_oct)
			{
				case 6:
				case 7: /*  y >  x */ pb_y += diff;  break;
				case 8: /*  x >  y */
				case 1: /*  x > -y */ pb_x += diff;  break;
				case 2: /* -y >  x */
				case 3: /* -y > -x */ pb_y -= diff;  break;
				case 4: /* -y < -x */
				case 5: /*  y < -x */ pb_x -= diff;  break;
			}
		}

		for(int i=1; i<9; i++) arcOctTable[i] = false;

		if (start_oct == end_oct)
		{
			arcOctTable[start_oct] = true;
			Point pa = arcXformOctant(pa_x, pa_y, start_oct);
			Point pb = arcXformOctant(pb_x, pb_y, start_oct);

			if ((start_oct&1) != 0) arcBresCW(pa, pb);
			else                    arcBresCCW(pa, pb);
			arcOctTable[start_oct] = false;
		} else
		{
			arcOctTable[start_oct] = true;
			Point pt = arcXformOctant(pa_x, pa_y, start_oct);
			if ((start_oct&1) != 0) arcBresMidCW(pt);
			else			    	arcBresMidCCW(pt);
			arcOctTable[start_oct] = false;

			arcOctTable[end_oct] = true;
			pt = arcXformOctant(pb_x, pb_y, end_oct);
			if ((end_oct&1) != 0) arcBresMidCCW(pt);
			else			      arcBresMidCW(pt);
			arcOctTable[end_oct] = false;

			if (MODP(start_oct+1) != end_oct)
			{
				if (MODP(start_oct+1) == MODM(end_oct-1))
				{
					arcOctTable[MODP(start_oct+1)] = true;
				} else
				{
					for(int i = MODP(start_oct+1); i != end_oct; i = MODP(i+1))
						arcOctTable[i] = true;
				}
				arcBresMidCW(new Point(0, arcRadius));
			}
		}
	}

	private int MODM(int x) { return (x<1) ? x+8 : x; }
	private int MODP(int x) { return (x>8) ? x-8 : x; }

	// ************************************* RENDERING SUPPORT *************************************

	void drawPoint(int x, int y, byte [][] layerBitMap, int col)
	{
		if (layerBitMap == null)
		{
			opaqueData[y * sz.width + x] = col;
		} else
		{
			layerBitMap[y][x>>3] |= (1 << (x&7));
		}
	}

	private void drawThickPoint(int x, int y, byte [][] layerBitMap, int col)
	{
		if (layerBitMap == null)
		{
	        int baseIndex = y * sz.width + x;
			opaqueData[baseIndex] = col;
			if (x > 0)
				opaqueData[baseIndex - 1] = col;
			if (x < sz.width-1)
				opaqueData[baseIndex + 1] = col;
			if (y > 0)
				opaqueData[baseIndex - sz.width] = col;
			if (y < sz.height-1)
				opaqueData[baseIndex + sz.width] = col;
		} else
		{
			layerBitMap[y][x>>3] |= (1 << (x&7));
			if (x > 0)
				layerBitMap[y][(x-1)>>3] |= (1 << ((x-1)&7));
			if (x < sz.width-1)
				layerBitMap[y][(x+1)>>3] |= (1 << ((x+1)&7));
			if (y > 0)
				layerBitMap[y-1][x>>3] |= (1 << (x&7));
			if (y < sz.height-1)
				layerBitMap[y+1][x>>3] |= (1 << (x&7));
		}
	}

	/**
	 * Method to convert a database coordinate to screen coordinates.
	 * @param dbX the X coordinate (in database units).
	 * @param dbY the Y coordinate (in database units).
	 * @param result the Point in which to store the screen coordinates.
	 */
	private void databaseToScreen(double dbX, double dbY, Point result) {
        double scrX = originX + dbX*scale;
        double scrY = originY - dbY*scale;
		result.x = (int)(scrX >= 0 ? scrX + 0.5 : scrX - 0.5);
		result.y = (int)(scrY >= 0 ? scrY + 0.5 : scrY - 0.5);
    }
    
	/**
	 * Method to convert a database rectangle to screen coordinates.
	 * @param db the rectangle (in database units).
	 * @return the rectangle on the screen.
	 */
	private Rectangle databaseToScreen(Rectangle2D db)
	{
        Point llPt = tempPt1;
        Point urPt = tempPt2;
		databaseToScreen(db.getMinX(), db.getMinY(), llPt);
		databaseToScreen(db.getMaxX(), db.getMaxY(), urPt);
		int screenLX = llPt.x;
		int screenHX = urPt.x;
		int screenLY = llPt.y;
		int screenHY = urPt.y;
		if (screenHX < screenLX) { int swap = screenHX;   screenHX = screenLX; screenLX = swap; }
		if (screenHY < screenLY) { int swap = screenHY;   screenHY = screenLY; screenLY = swap; }
		return new Rectangle(screenLX, screenLY, screenHX-screenLX+1, screenHY-screenLY+1);
	}
}
