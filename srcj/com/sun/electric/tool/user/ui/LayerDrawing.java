/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayerDrawing_.java
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
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.tool.user.User;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.technologies.Artwork;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
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
class LayerDrawing
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
    private static int boxes, crosses, solidLines, patLines, thickLines, polygons, texts, circles, thickCircles, discs, circleArcs, points, thickPoints;
    private static final boolean DEBUG = false;
    private static boolean patternedDisplay = true;

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
		private LayerDrawing offscreen;

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
    /** the scale of the EditWindow */                      private double scale_;
	/** the window scale and pan factor */					private float factorX, factorY;
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
    /** alpha blender of layer maps */                      private final AlphaBlender alphaBlender = new AlphaBlender();

	// the transparent bitmaps
	/** the offscreen maps for transparent layers */		private byte [][] layerBitMaps;
	/** the number of transparent layers */					int numLayerBitMaps;
	/** the number of bytes per row in offscreen maps */	private final int numBytesPerRow;
	/** the number of offscreen transparent maps made */	private int numLayerBitMapsCreated;
	/** the technology of the window */						private Technology curTech;
    /** ERasters for layers of current technology. */       private ERaster[] curTechLayers;

	/** the map from layers to layer bitmaps */             private HashMap<Layer,TransparentRaster> layerRasters = new HashMap<Layer,TransparentRaster>();
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
	/** zero rectangle */									private static final Rectangle2D CENTERRECT = new Rectangle2D.Double(0, 0, 0, 0);
    private static Color textColor;
	private static final EGraphics textGraphics = new EGraphics(false, false, null, 0, 0,0,0, 1.0,true,
		new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
	private static final EGraphics instanceGraphics = new EGraphics(false, false, null, 0, 0,0,0, 1.0,true,
		new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
    private static final Layer instanceLayer = Layer.newInstance("Instance", instanceGraphics);
	private static final EGraphics portGraphics = new EGraphics(false, false, null, 0, 255,0,0, 1.0,true,
		new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});

    private int clipLX, clipHX, clipLY, clipHY;
    private final int width;
    
    private final EditWindow0 dummyWnd = new EditWindow0() {
        public VarContext getVarContext() { return varContext; }
        
        public double getScale() { return scale; }
    };

    static class Drawing extends EditWindow.Drawing {
        private LayerDrawing offscreen;
        
        Drawing(EditWindow wnd) {
            super(wnd);
        }
        
        void setScreenSize(Dimension sz) {
            offscreen = new LayerDrawing(sz);
        }
        
        boolean paintComponent(Graphics g, Dimension sz) {
            if (offscreen == null || !wnd.getSize().equals(sz)) {
                Dimension newSize = wnd.getSize();
                wnd.setScreenSize(newSize);
                wnd.repaintContents(null, false);
                g.setColor(new Color(User.getColorBackground()));
                g.fillRect(0, 0, newSize.width, newSize.height);
                return false;
            }
            
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
    }
    
    // ************************************* TOP LEVEL *************************************

	/**
	 * Constructor creates an offscreen PixelDrawing object.
	 * @param sz the size of an offscreen PixelDrawinf object.
	 */
	public LayerDrawing(Dimension sz)
	{
		this.sz = new Dimension(sz);
        width = sz.width;
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
    
    public LayerDrawing(double scale, int lX, int hX, int lY, int hY) {
        this.scale = scale;
        scale_ = (float)(scale/DBMath.GRID);
        
        this.originX = -lX;
        this.originY = hY;
        factorX = (float)(-originX/scale_);
        factorY = (float)(originY/scale_);
        this.sz = new Dimension(hX - lX + 1, hY - lY + 1);
        width = sz.width;
        clipLX = 0;
        clipHX = sz.width - 1;
        clipLY = 0;
        clipHY = sz.height - 1;
        
		// allocate pointer to the opaque image
        img = null;
		total = sz.height * sz.width;
        opaqueData = null;
		numBytesPerRow = (sz.width + 7) / 8;
        
		// initialize the data
		clearImage(null);
    }
    
    void initOrigin(double scale, Point2D offset) {
        this.scale = scale;
        scale_ = (float)(scale/DBMath.GRID);
        this.originX = sz.width/2 - offset.getX()*scale;
        this.originY = sz.height/2 + offset.getY()*scale;
		factorX = (float)(offset.getX()*DBMath.GRID - sz.width/2/scale_);
		factorY = (float)(offset.getY()*DBMath.GRID + sz.height/2/scale_);
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
		long startTime = 0, clearTime = 0, countTime = 0, drawTime = 0;
		long initialUsed = 0;
		if (TAKE_STATS)
		{
//			Runtime.getRuntime().gc();
			startTime = System.currentTimeMillis();
			initialUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			tinyCells = tinyPrims = totalCells = renderedCells = totalPrims = tinyArcs = linedArcs = totalArcs = 0;
			offscreensCreated = offscreenPixelsCreated = offscreensUsed = offscreenPixelsUsed = cellsRendered = 0;
            boxes = crosses = solidLines = patLines = thickLines = polygons = texts = circles = thickCircles = discs = circleArcs = points = thickPoints = 0;
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
		instanceGraphics.setColor(new Color(User.getColorInstanceOutline()));
		
		// initialize the cache of expanded cell displays
		if (expandedScale != wnd.getScale())
		{
			clearSubCellCache();
			expandedScale = wnd.getScale();
		}
        varContext = wnd.getVarContext();
        initOrigin(expandedScale, wnd.getOffset());
        patternedDisplay = expandedScale > User.getPatternedScaleLimit() || Technology.getCurrent() != MoCMOS.tech;
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
        // reset cached cell counts
        numberToReconcile = SINGLETONSTOADD;
        for(ExpandedCellInfo count : expandedCells.values())
            count.instanceCount = 0;
        if (TAKE_STATS) clearTime = System.currentTimeMillis();
        
        // determine which cells should be cached (must have at least 2 instances)
        countCell(cell, drawLimitBounds, fullInstantiate, Orientation.IDENT, DBMath.MATID);
        if (TAKE_STATS) countTime = System.currentTimeMillis();
        
        // now render it all
        drawCell(cell, drawLimitBounds, fullInstantiate, Orientation.IDENT, 0, 0, true, wnd.getVarContext());
        if (TAKE_STATS) drawTime = System.currentTimeMillis();

		// merge transparent image into opaque one
		synchronized(img)
		{
			// if a grid is requested, overlay it
			if (cell != null && wnd.isGrid()) drawGrid(wnd);

			// combine transparent and opaque colors into a final image
			composite(renderBounds);
		};

		if (TAKE_STATS)
		{
			long endTime = System.currentTimeMillis();
			long curUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			long memConsumed = curUsed - initialUsed;
			System.out.println("Took "+TextUtils.getElapsedTime(endTime-startTime) +
                "(" + (clearTime-startTime) + "+" + (countTime - clearTime) + "+" + (drawTime-countTime) + "+" + (endTime - drawTime) + ")"+
				", rendered "+cellsRendered+" cells, used "+offscreensUsed+" ("+offscreenPixelsUsed+" pixels) cached cells, created "+
				offscreensCreated+" ("+offscreenPixelsCreated+" pixels) new cell caches (my size is "+total+" pixels), memory used="+memConsumed);
			System.out.println("   Cells ("+totalCells+") "+tinyCells+" are tiny;"+
				" Primitives ("+totalPrims+") "+tinyPrims+" are tiny;"+
				" Arcs ("+totalArcs+") "+tinyArcs+" are tiny, "+linedArcs+" are lines");
//            System.out.print("    " + (boxes+crosses+solidLines+patLines+thickLines+polygons+texts+circles+thickCircles+discs+circleArcs+points+thickPoints)+" rendered: ");
//            if (boxes != 0) System.out.print(boxes+" boxes ");
//            if (crosses != 0) System.out.print(crosses+" crosses ");
//            if (solidLines != 0) System.out.print(solidLines+" solidLines ");
//            if (patLines != 0) System.out.print(patLines+" patLines ");
//            if (thickLines != 0) System.out.print(thickLines+" thickLines ");
//            if (polygons != 0) System.out.print(polygons+" polygons ");
//            if (texts != 0) System.out.print(texts+" texts ");
//            if (circles != 0) System.out.print(circles+" circles ");
//            if (thickCircles != 0) System.out.print(thickCircles+" thickCircles ");
//            if (discs != 0) System.out.print(discs+" discs ");
//            if (circleArcs != 0) System.out.print(circleArcs+" circleArcs ");
//            if (points != 0) System.out.print(points+" points ");
//            if (thickPoints != 0) System.out.print(thickPoints+" thickPoints ");
//            System.out.println();
		}
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

		// erase the patterned opaque layer bitmaps
		for(Map.Entry<Layer,TransparentRaster> e: layerRasters.entrySet())
		{
			TransparentRaster raster = e.getValue();
			byte [] layerBitMap = raster.layerBitMap;
            for (int i = 0; i < layerBitMap.length; i++)
                layerBitMap[i] = 0;
		}

		// erase the transparent bitmaps
		for(int i=0; i<numLayerBitMaps; i++)
		{
			byte [] layerBitMap = layerBitMaps[i];
			if (layerBitMap == null) continue;
            for (int j = 0; j < layerBitMap.length; j++)
                layerBitMap[j] = 0;
		}

		// erase opaque image
        if (opaqueData == null) return;
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
        if (!patternedDisplay) {
            layerComposite();
            return img;
        }
        
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
                int baseByteIndex = y*numBytesPerRow;
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
						int entry = baseByteIndex + (x>>3);
						int maskBit = 1 << (x & 7);
						for(int i=0; i<numLayerBitMaps; i++)
						{
                            byte[] layerBitMap = layerBitMaps[i]; 
							if (layerBitMap == null) continue;
							int byt = layerBitMap[entry];
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
    
    private void layerComposite() {
        HashMap<Layer,byte[]> layerBytes = new HashMap<Layer,byte[]>();
        for (Map.Entry<Layer,TransparentRaster> e: layerRasters.entrySet())
            layerBytes.put(e.getKey(), e.getValue().layerBitMap);
        alphaBlender.init(backgroundValue, wnd.getBlendingOrder(layerBytes.keySet(), false), layerBytes);
    
        long startTime = System.currentTimeMillis();
        int numFullBytes = sz.width / 8;
        int numTailBits = sz.width % 8;
        byte tailBitsMask = (byte)((1 << numTailBits) - 1);
        int baseIndex = 0, baseByteIndex = 0;
        for (int y = 0; y < sz.height; y++) {
            alphaBlender.composeBytes(baseByteIndex, numFullBytes, opaqueData, baseIndex);
            if (numTailBits != 0)
                alphaBlender.composeBits(baseByteIndex + numFullBytes, tailBitsMask, opaqueData, baseIndex + numFullBytes*8);
            baseByteIndex += numBytesPerRow;
            baseIndex += width;
        }
        assert baseByteIndex == numBytesPerRow * sz.height; 
        assert baseIndex == sz.width * sz.height;
        long endTime = System.currentTimeMillis();
        System.out.println("layerComposite took " + (endTime - startTime) + " msec");
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
                    
                    // draw box  in opaque area
                    for(int yg=boxLY; yg<=boxHY; yg++) {
                        int baseIndex = yg * sz.width;
                        for(int xg=boxLX; xg<=boxHX; xg++)
                            opaqueData[baseIndex + xg] = col;
                        baseIndex += sz.width;
                    }
                    
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
			if (xy.x >= 0 && xy.x < sz.width) {
                int baseIndex = xy.x;
                for (int y = 0; y < sz.height; y++) {
                    opaqueData[baseIndex] = col;
                    baseIndex += sz.width;
                }
            }
			if (xy.y >= 0 && xy.y < sz.height) {
                int baseIndex = xy.y * sz.width;
                for (int x = 0; x < sz.width; x++)
                    opaqueData[baseIndex + x] = col;
            }
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
		layerBitMaps = new byte[numLayerBitMaps][];
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
	private void drawCell(Cell cell, Rectangle2D drawLimitBounds, boolean fullInstantiate, Orientation orient, int oX, int oY, boolean topLevel, VarContext context)
	{
		renderedCells++;

        VectorCache.VectorCell vc = VectorCache.theCache.drawCell(cell, orient, context, scale);
        
		// draw all subcells
		for(VectorCache.VectorSubCell vsc : vc.subCells) {
			totalCells++;

			// get instance location
            int soX = vsc.offsetX + oX;
            int soY = vsc.offsetY + oY;
            VectorCache.VectorCell subVC = VectorCache.theCache.findVectorCell(vsc.subCell.getId(), vc.orient.concatenate(vsc.pureRotate));
            gridToScreen(subVC.lX + soX, subVC.hY + soY, tempPt1);
            gridToScreen(subVC.hX + soX, subVC.lY + soY, tempPt2);
            int lX = tempPt1.x;
            int lY = tempPt1.y;
            int hX = tempPt2.x;
            int hY = tempPt2.y;

			// see if the subcell is clipped
			if (hX < clipLX || lX > clipHX) continue;
			if (hY < clipLY || lY > clipHY) continue;

			boolean expanded = vsc.ni.isExpanded() || fullInstantiate;

			// if not expanded, but viewing this cell in-place, expand it
			if (!expanded)
			{
				if (inPlaceNodePath != null)
				{
					for(int pathIndex=0; pathIndex<inPlaceNodePath.size(); pathIndex++)
					{
						NodeInst niOnPath = inPlaceNodePath.get(pathIndex);
						if (niOnPath.getProto() == vsc.subCell)
						{
							expanded = true;
							break;
						}
					}
				}
			}

			// two ways to draw a cell instance
			Cell subCell = (Cell)vsc.ni.getProto();
			if (expanded)
			{
				// show the contents of the cell
                Orientation subOrient = orient.concatenate(vsc.ni.getOrient());
                int soX_ = vsc.offsetX + oX;
                int soY_ = vsc.offsetY + oY;
				if (!expandedCellCached(subCell, subOrient, soX_, soY_, context, fullInstantiate))
				{
					// just draw it directly
					cellsRendered++;
					drawCell(subCell, drawLimitBounds, fullInstantiate, subOrient, soX_, soY_, false, context.push(vsc.ni));
				}
			} else
			{
				// draw the black box of the instance
                int[] op = subVC.outlinePoints;
                int p1x = op[0] + soX;
                int p1y = op[1] + soY;
                int p2x = op[2] + soX;
                int p2y = op[3] + soY;
                int p3x = op[4] + soX;
                int p3y = op[5] + soY;
                int p4x = op[6] + soX;
                int p4y = op[7] + soY;
                gridToScreen(p1x, p1y, tempPt1);   gridToScreen(p2x, p2y, tempPt2);
                OpaqueRaster.current.init(opaqueData, sz.width, instanceGraphics.getRGB() & 0xffffff);
                ERaster instanceRaster = getRaster(instanceLayer, instanceGraphics, false);
				drawLine(tempPt1, tempPt2, 0, instanceRaster);
				gridToScreen(p2x, p2y, tempPt1);   gridToScreen(p3x, p3y, tempPt2);
				drawLine(tempPt1, tempPt2, 0, instanceRaster);
				gridToScreen(p3x, p3y, tempPt1);   gridToScreen(p4x, p4y, tempPt2);
				drawLine(tempPt1, tempPt2, 0, instanceRaster);
				gridToScreen(p1x, p1y, tempPt1);   gridToScreen(p4x, p4y, tempPt2);
				drawLine(tempPt1, tempPt2, 0, instanceRaster);

				// draw the instance name
				if (canDrawText && User.isTextVisibilityOnInstance())
				{
					tempRect.setBounds(lX, lY, hX-lX, hY-lY);
					TextDescriptor descript = vsc.ni.getTextDescriptor(NodeInst.NODE_PROTO);
					NodeProto np = vsc.ni.getProto();
					drawText(tempRect, Poly.Type.TEXTBOX, descript, np.describe(false), textGraphics, false);
				}
			}
			if (canDrawText) drawPortList(vsc, subVC, soX, soY, vsc.ni.isExpanded());
        }

        // draw primitives
        drawList(oX, oY, vc.filledShapes);
        drawList(oX, oY, vc.shapes);
            
		// show cell variables if at the top level
        if (canDrawText && topLevel)
            drawList(oX, oY, vc.topOnlyShapes);
	}
    
	/**
	 * @return true if the cell is properly handled and need no further processing.
	 * False to render the contents recursively.
	 */
	private boolean expandedCellCached(Cell subCell, Orientation orient, int oX, int oY, VarContext context, boolean fullInstantiate)
	{
		// if there is no global for remembering cached cells, do not cache
		if (expandedCells == null) return false;

		// do not cache icons: they can be redrawn each time
		if (subCell.isIcon()) return false;

        ExpandedCellKey expansionKey = new ExpandedCellKey(subCell, orient);
		ExpandedCellInfo expandedCellCount = expandedCells.get(expansionKey);
		if (expandedCellCount != null && expandedCellCount.offscreen == null)
		{
            if (expandedCellCount.tooLarge) return false;
			// if this combination is not used multiple times, do not cache it
			if (expandedCellCount.singleton && expandedCellCount.instanceCount < 2)
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
            int lX = (int)Math.ceil(cellBounds.getMinX()*scale - 0.5);
            int hX = (int)Math.floor(cellBounds.getMaxX()*scale + 0.5);
            int lY = (int)Math.ceil(cellBounds.getMinY()*scale - 0.5);
            int hY = (int)Math.floor(cellBounds.getMaxY()*scale + 0.5);
            assert lX <= hX && lY <= hY;
            
			// if this is the first use, create the offscreen buffer
			if (expandedCellCount == null)
			{
				expandedCellCount = new ExpandedCellInfo();
				expandedCells.put(expansionKey, expandedCellCount);
			}

			// do not cache if the cell is too large (creates immense offscreen buffers)
			if (hX - lX >= topSz.width/2 && hY - lY >= topSz.height/2) {
                expandedCellCount.tooLarge = true;
				return false;
            }

            expandedCellCount.offscreen = new LayerDrawing(scale, lX, hX, lY, hY);
			expandedCellCount.offscreen.drawCell(subCell, null, fullInstantiate, orient, 0, 0, false, context);
			offscreensCreated++;
            offscreenPixelsCreated += expandedCellCount.offscreen.total;
            if (DEBUG) {
                System.out.print(subCell + " " + orient + " rendered in " + expandedCellCount.offscreen.total + " pixels" +
                        " t=" + System.currentTimeMillis());
                for (Layer layer: expandedCellCount.offscreen.layerRasters.keySet())
                    System.out.print(" " + layer.getName());
                System.out.println();
            }
		}

		// copy out of the offscreen buffer into the main buffer
        gridToScreen(oX, oY, tempPt1);
		copyBits(expandedCellCount.offscreen, tempPt1.x, tempPt1.y);
		offscreensUsed++;
        offscreenPixelsUsed += expandedCellCount.offscreen.total;
		return true;
	}

	// ************************************* CELL CACHING *************************************

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
	private void copyBits(LayerDrawing srcOffscreen, int centerX, int centerY)
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

		// copy the patterned opaque layers
        for(Map.Entry<Layer,TransparentRaster> e : srcOffscreen.layerRasters.entrySet()) {
            Layer layer = e.getKey();
            ERaster raster = getRaster(layer, layer.getGraphics(), false);
            if (raster == null) continue;
            TransparentRaster polSrc = e.getValue();
            raster.copyBits(polSrc, minSrcX, maxSrcX, minSrcY, maxSrcY, cornerX, cornerY);
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
        
        /**
         * Method to copy bits from rectangle of sorce TransparentRaster to thus ERaster.
         * @param polSrc source TransparentRaster.
         * @param minSrcX left bound of source rectangle (inclusive).
         * @param maxSrcX right bound of source rectangle (inclusive).
         * @param minSrcY top bound of source rectangle (inclusive).
         * @param maxSrcY bottom bound of source rectangle (inclusive).
         * @param dx the X translation factor from src space to dst space of the copy.
         * @param dy the Y translation factor from src space to dst space of the copy.
         */
        public void copyBits(TransparentRaster src, int minSrcX, int maxSrcX, int minSrcY, int maxSrcY, int dx, int dy);
    }
    
    ERaster getRaster(Layer layer, EGraphics graphics, boolean forceVisible) {
        if (!patternedDisplay || !renderedWindow) {
            layer = layer.getNonPseudoLayer();
            if (layer.getTechnology() == null)
                layer = Artwork.tech.defaultLayer;
            TransparentRaster raster = layerRasters.get(layer);
            if (raster == null) {
                raster = new TransparentRaster(sz.height, numBytesPerRow);
                layerRasters.put(layer, raster);
            }
            return raster;
        }
        
        boolean dimmed = false;
        if (layer != null) {
            if (!forceVisible && !layer.isVisible()) return null;
            graphics = layer.getGraphics();
            dimmed = layer.isDimmed();
        }
        
		byte [] layerBitMap = null;
		int layerNum = graphics.getTransparentLayer() - 1;
		if (layerNum < numLayerBitMaps) layerBitMap = getLayerBitMap(layerNum);

        ERaster raster;
		int [] pattern = null;
        if (nowPrinting != 0 ? graphics.isPatternedOnPrinter() : graphics.isPatternedOnDisplay())
            pattern = graphics.getPattern();
        if (layerBitMap != null) {
            if (pattern == null) {
                TransparentRaster.current.init(layerBitMap, numBytesPerRow);
                return TransparentRaster.current;
            }
			EGraphics.Outline o = graphics.getOutlined();
            if (o == EGraphics.Outline.NOPAT)
                o = null;
            PatternedTransparentRaster.current.init(layerBitMap, numBytesPerRow, pattern, o);
            return PatternedTransparentRaster.current;
        } else {
    		int col = getTheColor(graphics, dimmed);
            if (pattern == null) {
                OpaqueRaster.current.init(opaqueData, sz.width, col);
                return OpaqueRaster.current;
            }
            EGraphics.Outline o = graphics.getOutlined();
            if (o == EGraphics.Outline.NOPAT)
                o = null;
            PatternedOpaqueRaster.current.init(opaqueData, sz.width, col, pattern, o);
            return PatternedOpaqueRaster.current;
        }
    }
    
    /**
     * ERaster for solid opaque layers.
     */
    private static class OpaqueRaster implements ERaster {
        private static OpaqueRaster current = new OpaqueRaster();
        
        int[] opaqueData;
        int width;
        int col;
        
        void init(int[] opaqueData, int width, int col) {
            this.opaqueData = opaqueData;
            this.width = width;
            this.col = col;
        }
        
        public void fillBox(int lX, int hX, int lY, int hY) {
            int baseIndex = lY*width;
            for (int y = lY; y <= hY; y++) {
                for(int x = lX; x <= hX; x++)
                    opaqueData[baseIndex + x] = col;
                baseIndex += width;
            }
        }
        
        public void fillHorLine(int y, int lX, int hX) {
            int baseIndex = y*width + lX;
            for (int x = lX; x <= hX; x++)
                opaqueData[baseIndex++] = col;
        }
        
        public void fillVerLine(int x, int lY, int hY) {
            int baseIndex = lY*width + x;
            for (int y = lY; y <= hY; y++) {
                opaqueData[baseIndex] = col;
                baseIndex += width;
            }
        }
        
        public void fillPoint(int x, int y) {
            opaqueData[y * width + x] = col;
        }
        
        public void drawHorLine(int y, int lX, int hX) {
            int baseIndex = y*width + lX;
            for (int x = lX; x <= hX; x++)
                opaqueData[baseIndex++] = col;
        }
        
        public void drawVerLine(int x, int lY, int hY) {
            int baseIndex = lY*width + x;
            for (int y = lY; y <= hY; y++) {
                opaqueData[baseIndex] = col;
                baseIndex += width;
            }
        }
        
        public void drawPoint(int x, int y) {
            opaqueData[y * width + x] = col;
        }

        public EGraphics.Outline getOutline() {
            return null;
        }
        
        public void copyBits(TransparentRaster src, int minSrcX, int maxSrcX, int minSrcY, int maxSrcY, int dx, int dy) {
            byte[] srcLayerBitMap = src.layerBitMap;
            for (int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
                int destY = srcY + dy;
                int destBase = destY * width;
                int srcBaseIndex = srcY*src.bytesPerRow;
                for (int srcX = minSrcX; srcX <= maxSrcX; srcX++) {
                    int destX = srcX + dx;
                    if ((srcLayerBitMap[srcBaseIndex + (srcX>>3)] & (1<<(srcX&7))) != 0)
                        opaqueData[destBase + destX] = col;
                }
            }
        }
    }

    /**
     * ERaster for solid opaque layers with alpha protection against overriding.
     */
    private static class OpaqueAlphaRaster extends OpaqueRaster {
        private static OpaqueAlphaRaster current = new OpaqueAlphaRaster();
        
        public void fillBox(int lX, int hX, int lY, int hY) {
            int baseIndex = lY*width;
            for (int y = lY; y <= hY; y++) {
                for(int x = lX; x <= hX; x++) {
                    int index = baseIndex + x;
                    int alpha = (opaqueData[index] >> 24) & 0xFF;
                    if (alpha == 0xFF) opaqueData[index] = col;
                }
                baseIndex += width;
            }
        }
        
        public void fillHorLine(int y, int lX, int hX) {
            int baseIndex = y*width + lX;
            for (int x = lX; x <= hX; x++) {
                int alpha = (opaqueData[baseIndex] >> 24) & 0xFF;
                if (alpha == 0xFF) opaqueData[baseIndex] = col;
                baseIndex++;
            }
        }
        
        public void fillVerLine(int x, int lY, int hY) {
            int baseIndex = lY*width + x;
            for (int y = lY; y <= hY; y++) {
                int alpha = (opaqueData[baseIndex] >> 24) & 0xFF;
                if (alpha == 0xFF) opaqueData[baseIndex] = col;
                baseIndex += width;
            }
        }
        
        public void fillPoint(int x, int y) {
            int baseIndex = y*width + x;
            int alpha = (opaqueData[baseIndex] >> 24) & 0xFF;
            if (alpha == 0xFF) opaqueData[baseIndex] = col;
        }
    }

    /**
     * ERaster for solid transparent layers.
     */
    private static class TransparentRaster implements ERaster {
        private static TransparentRaster current = new TransparentRaster();
        
        byte[] layerBitMap;
        int bytesPerRow;
        
        private TransparentRaster() {}
        
        TransparentRaster(int height, int numBytesPerRow) {
            this.bytesPerRow = numBytesPerRow;
            layerBitMap = new byte[height*numBytesPerRow];
        }
            
    private void init(byte[] layerBitMap, int bytesPerRow) {
            this.layerBitMap = layerBitMap;
            this.bytesPerRow = bytesPerRow;
        }
        
        public void fillBox(int lX, int hX, int lY, int hY) {
            int baseIndex = lY*bytesPerRow;
            for (int y = lY; y <= hY; y++) {
                for (int x = lX; x <= hX; x++)
                    layerBitMap[baseIndex + (x>>3)] |= (1 << (x&7));
                baseIndex += bytesPerRow;
            }
        }
        
        public void fillHorLine(int y, int lX, int hX) {
            int baseIndex = y*bytesPerRow;
            for (int x = lX; x <= hX; x++)
                layerBitMap[baseIndex + (x>>3)] |= (1 << (x&7));
        }
        
        public void fillVerLine(int x, int lY, int hY) {
            int baseIndex = lY*bytesPerRow + (x>>3);
            byte mask = (byte)(1 << (x&7));
            for (int y = lY; y <= hY; y++) {
                layerBitMap[baseIndex] |= mask;
                baseIndex += bytesPerRow;
            }
        }
        
        public void fillPoint(int x, int y) {
            layerBitMap[y*bytesPerRow + (x>>3)] |= (1 << (x&7));
        }
        
        public void drawHorLine(int y, int lX, int hX) {
            int baseIndex = y*bytesPerRow;
            for (int x = lX; x <= hX; x++)
                layerBitMap[baseIndex + (x>>3)] |= (1 << (x&7));
        }
        
        public void drawVerLine(int x, int lY, int hY) {
            int baseIndex = lY*bytesPerRow + (x>>3);
            byte mask = (byte)(1 << (x&7));
            for (int y = lY; y <= hY; y++) {
                layerBitMap[baseIndex] |= mask;
                baseIndex += bytesPerRow;
            }
        }
        
        public void drawPoint(int x, int y) {
            layerBitMap[y*bytesPerRow + (x>>3)] |= (1 << (x&7));
        }
        
        public EGraphics.Outline getOutline() {
            return null;
        }
        
        public void copyBits(TransparentRaster src, int minSrcX, int maxSrcX, int minSrcY, int maxSrcY, int dx, int dy) {
            byte [] srcLayerBitMap = src.layerBitMap;
            int minDestX = minSrcX + dx;
            int maxDestX = maxSrcX + dx;
            int minDestY = minSrcY + dy;
            int maxDestY = maxSrcY + dy;
            int leftShift = dx&7;
            int rightShift = 8 - leftShift;
            int srcBaseIndex = minSrcY*src.bytesPerRow + minSrcX/8;
            int destBaseIndex = minDestY*bytesPerRow + minDestX/8;
            if (maxDestX/8 == minDestX/8) {
                // Single destination byte.
                int destMask = (1 << (maxDestX%8 + 1)) - (1 << minDestX%8);
                if (minSrcX/8 != maxSrcX/8) {
                    // A pair of source bytes
                    for (int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
                        int s0 = srcLayerBitMap[srcBaseIndex]&0xFF;
                        int s1 = srcLayerBitMap[srcBaseIndex + 1]&0xFF;
                        int v = ((s0 >> rightShift) | (s1 << leftShift)) & destMask;
                        if (v != 0)
                            layerBitMap[destBaseIndex] |= v;
                        srcBaseIndex += src.bytesPerRow;
                        destBaseIndex += bytesPerRow;
                    }
                } else if (minDestX%8 >= minSrcX%8) {
                    // source byte shifted left
                    for (int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
                        int s = srcLayerBitMap[srcBaseIndex]&0xFF;
                        int v = (s << leftShift) & destMask;
                        if (v != 0)
                            layerBitMap[destBaseIndex] |= v;
                        srcBaseIndex += src.bytesPerRow;
                        destBaseIndex += bytesPerRow;
                    }
                } else {
                    // source byte shifted right
                    for (int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
                        int s = srcLayerBitMap[srcBaseIndex]&0xFF;
                        int v = (s >> rightShift) & destMask;
                        if (v != 0)
                            layerBitMap[destBaseIndex] |= v;
                        srcBaseIndex += src.bytesPerRow;
                        destBaseIndex += bytesPerRow;
                    }
                }
            } else {
                int minDestMask = (1 << 8) - (1 << minDestX%8);
                int maxDestMask = (1 << (maxDestX%8 + 1)) - 1;
                boolean minSrcPair = 7 - rightShift >= minDestX%8;
                boolean maxSrcPair = leftShift <= maxDestX%8;
                int numDestBytes = maxDestX/8 - minDestX/8;
                int srcIncr = src.bytesPerRow - maxSrcX/8 + minSrcX/8 - 1;
                for (int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
                    assert srcBaseIndex == srcY*src.bytesPerRow + minSrcX/8;
                    assert destBaseIndex == (srcY + dy)*bytesPerRow + (minSrcX + dx)/8;
                    int s = minSrcPair ? srcLayerBitMap[srcBaseIndex++]&0xFF : 0;
                    int b0 = srcLayerBitMap[srcBaseIndex++]&0xFF;
                    int v0 = ((s >> rightShift) | (b0 << leftShift)) & minDestMask;
                    if (v0 != 0)
                        layerBitMap[destBaseIndex] |= v0;
                    destBaseIndex++;
                    s = b0;
                    for (int i = 1; i < numDestBytes; i++) {
                        int b = srcLayerBitMap[srcBaseIndex++]&0xFF;
                        int v = (s >> rightShift) | (b << leftShift);
                        if (v != 0)
                            layerBitMap[destBaseIndex] |= v;
                        destBaseIndex++;
                        s = b;
                    }
                    int bf = maxSrcPair ? srcLayerBitMap[srcBaseIndex++]&0xFF : 0;
                    int vf = ((s >> rightShift) | (bf << leftShift)) & maxDestMask;
                    if (vf != 0)
                        layerBitMap[destBaseIndex] |= vf;
                    srcBaseIndex += srcIncr;
                    destBaseIndex += (bytesPerRow - numDestBytes);
                }
            }
        }
    }

    /**
     * ERaster for patterned opaque layers.
     */
    private static class PatternedOpaqueRaster extends OpaqueRaster {
        private static PatternedOpaqueRaster current = new PatternedOpaqueRaster();
        
        int[] pattern;
        EGraphics.Outline outline;
        
        private void init(int[] opaqueData, int width, int col, int[] pattern, EGraphics.Outline outline) {
            super.init(opaqueData, width, col);
            this.pattern = pattern;
            this.outline = outline;
        }
        
        public void fillBox(int lX, int hX, int lY, int hY) {
            for (int y = lY; y <= hY; y++) {
                // setup pattern for this row
                int pat = pattern[y&15];
                if (pat == 0) continue;
                
                int baseIndex = y * width;
                for (int x = lX; x <= hX; x++) {
                    if ((pat & (0x8000 >> (x&15))) != 0)
                        opaqueData[baseIndex + x] = col;
                }
            }
        }
        
        public void fillHorLine(int y, int lX, int hX) {
            int pat = pattern[y & 15];
            if (pat == 0) return;
            int baseIndex = y * width;
            for (int x = lX; x <= hX; x++) {
                if ((pat & (1 << (15-(x&15)))) != 0) {
                    int index = baseIndex + x;
                    opaqueData[index] = col;
                }
            }
        }
        
        public void fillVerLine(int x, int lY, int hY) {
            int patMask = 0x8000 >> (x&15);
            int baseIndex = lY*width + x;
            for (int y = lY; y <= hY; y++) {
                if ((pattern[y&15] & patMask) != 0)
                    opaqueData[baseIndex] = col;
                baseIndex += width;
            }
        }
        
        public void fillPoint(int x, int y) {
            int patMask = 0x8000 >> (x&15);
            if ((pattern[y&15] &  patMask) != 0)
                opaqueData[y*width + x] = col;
        }
        
        public EGraphics.Outline getOutline() {
            return outline;
        }
        
        public void copyBits(TransparentRaster src, int minSrcX, int maxSrcX, int minSrcY, int maxSrcY, int dx, int dy) {
            byte[] srcLayerBitMap = src.layerBitMap;
            for (int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
                int destY = srcY + dy;
                int destBase = destY * width;
                int pat = pattern[destY&15];
                if (pat == 0) continue;
                int srcBaseIndex = srcY*src.bytesPerRow;
                for (int srcX = minSrcX; srcX <= maxSrcX; srcX++) {
                    int destX = srcX + dx;
                    if ((srcLayerBitMap[srcBaseIndex + (srcX>>3)] & (1<<(srcX&7))) != 0) {
                        if ((pat & (0x8000 >> (destX&15))) != 0)
                            opaqueData[destBase + destX] = col;
                    }
                }
            }
        }
    }
    
    /**
     * ERaster for patterned transparent layers.
     */
    private static class PatternedTransparentRaster extends TransparentRaster {
        private static PatternedTransparentRaster current = new PatternedTransparentRaster();
        
        int[] pattern;
        EGraphics.Outline outline;
        
        private void init(byte[] layerBitMap, int bytesPerRow, int[] pattern, EGraphics.Outline outline) {
            super.init(layerBitMap, bytesPerRow);
            this.pattern = pattern;
            this.outline = outline;
        }
        
        public void fillBox(int lX, int hX, int lY, int hY) {
            for (int y = lY; y <= hY; y++) {
                // setup pattern for this row
                int pat = pattern[y&15];
                if (pat == 0) continue;
                
                int baseIndex = y*bytesPerRow;
                for(int x=lX; x<=hX; x++) {
                    if ((pat & (0x8000 >> (x&15))) != 0)
                        layerBitMap[baseIndex + (x>>3)] |= (1 << (x&7));
                }
            }
        }
        
        public void fillHorLine(int y, int lX, int hX) {
            int pat = pattern[y & 15];
            if (pat == 0) return;
            int baseIndex = y*bytesPerRow;
            for (int x = lX; x <= hX; x++) {
                if ((pat & (1 << (15-(x&15)))) != 0)
                    layerBitMap[baseIndex + (x>>3)] |= (1 << (x&7));
            }
        }
        
        public void fillVerLine(int x, int lY, int hY) {
            int patMask = 0x8000 >> (x&15);
            int baseIndex = lY*bytesPerRow + (x>>3);
            byte mask = (byte)(1 << (x&7));
            for (int y = lY; y <= hY; y++) {
                if ((pattern[y&15] & patMask) != 0)
                    layerBitMap[baseIndex] |= mask;
                baseIndex += bytesPerRow;
            }
        }
        
        public void fillPoint(int x, int y) {
            int patMask = 0x8000 >> (x&15);
            if ((pattern[y&15] & patMask) != 0)
                layerBitMap[y*bytesPerRow + (x>>3)] |= (1 << (x&7));
        }
        
        public EGraphics.Outline getOutline() {
            return outline;
        }
        
        public void copyBits(TransparentRaster src, int minSrcX, int maxSrcX, int minSrcY, int maxSrcY, int dx, int dy) {
            byte[] srcLayerBitMap = src.layerBitMap;
            for(int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
                int destY = srcY + dy;
                int pat = pattern[destY & 15];
                if (pat == 0) return;
                int srcBaseIndex = srcY*src.bytesPerRow;
                int destBaseIndex = destY*bytesPerRow;
                for (int srcX = minSrcX; srcX <= maxSrcX; srcX++) {
                    int destX = srcX + dx;
                    if ((srcLayerBitMap[srcBaseIndex + (srcX>>3)] & (1<<(srcX&7))) != 0)
                        if ((pat & (1 << (15-(destX&15)))) != 0)
                            layerBitMap[destBaseIndex + (destX>>3)] |= (1 << (destX&7));
                }
            }
        }
    }
    
	// ************************************* RENDERING POLY SHAPES *************************************

    private static int boxCount, tinyBoxCount, lineBoxCount, lineCount, polygonCount, crossCount, circleCount, discCount, arcCount, textCount;
    private static Rectangle tempRect = new Rectangle();
    private void gridToScreen(int dbX, int dbY, Point result) {
        double scrX = (dbX - factorX) * scale_;
        double scrY = (factorY - dbY) * scale_;
        result.x = (int)(scrX >= 0 ? scrX + 0.5 : scrX - 0.5);
        result.y = (int)(scrY >= 0 ? scrY + 0.5 : scrY - 0.5);
    }
    
	/**
	 * Method to draw a list of cached shapes.
	 * @param oX the X offset to draw the shapes (in database grid coordinates).
	 * @param oY the Y offset to draw the shapes (in database grid coordinates).
	 * @param shapes the List of shapes (VectorBase objects).
	 * @param level: 0=top-level cell in window; 1=low level cell; -1=greeked cell.
	 */
	private void drawList(int oX, int oY, List<VectorCache.VectorBase> shapes)
//		throws AbortRenderingException
	{
		// render all shapes
		for(VectorCache.VectorBase vb : shapes)
		{
//			if (stopRendering) throw new AbortRenderingException();
			// handle refreshing
			periodicRefresh();
            
            if (vb.isFilled()) {
                ERaster raster = getRaster(vb.layer, vb.graphics, false);
                if (raster == null) continue;
                
                // handle each shape
                if (vb instanceof VectorCache.VectorManhattan) {
                    boxCount++;
                    VectorCache.VectorManhattan vm = (VectorCache.VectorManhattan)vb;
                    for (int i = 0; i < vm.coords.length; i += 4) {
                        int c1X = vm.coords[i];
                        int c1Y = vm.coords[i+1];
                        int c2X = vm.coords[i+2];
                        int c2Y = vm.coords[i+3];
                        
                        // determine coordinates of rectangle on the screen
                        gridToScreen(c1X+oX, c2Y+oY, tempPt1);
                        gridToScreen(c2X+oX, c1Y+oY, tempPt2);
                        int lX = tempPt1.x;
                        int lY = tempPt1.y;
                        int hX = tempPt2.x;
                        int hY = tempPt2.y;
                        
                        drawBox(lX, hX, lY, hY, raster);
                    }
                } else if (vb instanceof VectorCache.VectorPolygon) {
                    polygonCount++;
                    VectorCache.VectorPolygon vp = (VectorCache.VectorPolygon)vb;
                    Point [] intPoints = new Point[vp.points.length];
                    for(int i=0; i<vp.points.length; i++) {
                        intPoints[i] = new Point();
                        gridToScreen(vp.points[i].x+oX, vp.points[i].y+oY, intPoints[i]);
                    }
                    Point [] clippedPoints = GenMath.clipPoly(intPoints, clipLX, clipHX, clipLY, clipHY);
                    drawPolygon(clippedPoints, raster);
                } else if (vb instanceof VectorCache.VectorCircle) {
                    discCount++;
                    VectorCache.VectorCircle vci = (VectorCache.VectorCircle)vb;
                    gridToScreen(vci.cX+oX, vci.cY+oY, tempPt1);
                    gridToScreen(vci.eX+oX, vci.eY+oY, tempPt2);
                    assert vci.nature == 2;
                    drawDisc(tempPt1, tempPt2, raster);
                }
            } else if (vb instanceof VectorCache.VectorText) {
                if (!canDrawText) continue;
                VectorCache.VectorText vt = (VectorCache.VectorText)vb;
                switch (vt.textType) {
                    case VectorCache.VectorText.TEXTTYPEARC:
                        if (!User.isTextVisibilityOnArc()) continue;
                        break;
                    case VectorCache.VectorText.TEXTTYPENODE:
                        if (!User.isTextVisibilityOnNode()) continue;
                        break;
                    case VectorCache.VectorText.TEXTTYPECELL:
                        if (!User.isTextVisibilityOnCell()) continue;
                        break;
                    case VectorCache.VectorText.TEXTTYPEEXPORT:
                        if (!User.isTextVisibilityOnExport()) continue;
                        break;
                    case VectorCache.VectorText.TEXTTYPEANNOTATION:
                        if (!User.isTextVisibilityOnAnnotation()) continue;
                        break;
                    case VectorCache.VectorText.TEXTTYPEINSTANCE:
                        if (!User.isTextVisibilityOnInstance()) continue;
                        break;
                    case VectorCache.VectorText.TEXTTYPEPORT:
                        assert false;
                        if (!User.isTextVisibilityOnPort()) continue;
                        break;
                }
//				if (vt.height < maxTextSize) continue;
                
                String drawString = vt.str;
                int lX = vt.bounds.x;
                int lY = vt.bounds.y;
                int hX = lX + vt.bounds.width;
                int hY = lY + vt.bounds.height;
                gridToScreen(lX + oX, hY + oY, tempPt1);
                gridToScreen(hX + oX, lY + oY, tempPt2);
                lX = tempPt1.x;
                lY = tempPt1.y;
                hX = tempPt2.x;
                hY = tempPt2.y;
                
                EGraphics graphics = vt.graphics;
                boolean dimmed = vt.layer != null && vt.layer.isDimmed();
                if (vt.textType == VectorCache.VectorText.TEXTTYPEEXPORT && vt.e != null) {
                    int exportDisplayLevel = User.getExportDisplayLevel();
                    if (exportDisplayLevel == 2) {
                        // draw export as a cross
                        int cX = (lX + hX) / 2;
                        int cY = (lY + hY) / 2;
                        drawCross(cX, cY, 3, textGraphics, false);
                        crossCount++;
                        continue;
                    }
                    
                    // draw export as text
                    if (exportDisplayLevel == 1) drawString = vt.e.getShortName(); else
                        drawString = vt.e.getName();
                    graphics = textGraphics;
                }
                
                textCount++;
                tempRect.setBounds(lX, lY, hX-lX, hY-lY);
                drawText(tempRect, vt.style, vt.descript, drawString, graphics, dimmed);
                
            } else {
                ERaster raster = getRaster(vb.layer, vb.graphics, false);
                if (raster == null) continue;
                
                if (vb instanceof VectorCache.VectorLine) {
                    lineCount++;
                    VectorCache.VectorLine vl = (VectorCache.VectorLine)vb;
                    
                    // determine coordinates of line on the screen
                    gridToScreen(vl.fX+oX, vl.fY+oY, tempPt1);
                    gridToScreen(vl.tX+oX, vl.tY+oY, tempPt2);
                    
                    // clip and draw the line
                    drawLine(tempPt1, tempPt2, vl.texture, raster);
                } else if (vb instanceof VectorCache.VectorCross) {
                    crossCount++;
                    VectorCache.VectorCross vcr = (VectorCache.VectorCross)vb;
                    gridToScreen(vcr.x+oX, vcr.y+oY, tempPt1);
                    int size = vcr.small ? 3 : 5;
                    drawCross(tempPt1.x, tempPt1.y, size, raster);
                } else if (vb instanceof VectorCache.VectorCircle) {
                    circleCount++;
                    VectorCache.VectorCircle vci = (VectorCache.VectorCircle)vb;
                    gridToScreen(vci.cX+oX, vci.cY+oY, tempPt1);
                    gridToScreen(vci.eX+oX, vci.eY+oY, tempPt2);
                    switch (vci.nature) {
                        case 0: drawCircle(tempPt1, tempPt2, raster);        break;
                        case 1: drawThickCircle(tempPt1, tempPt2, raster);   break;
                    }
                } else if (vb instanceof VectorCache.VectorCircleArc) {
                    arcCount++;
                    VectorCache.VectorCircleArc vca = (VectorCache.VectorCircleArc)vb;
                    gridToScreen(vca.cX+oX, vca.cY+oY, tempPt1);
                    gridToScreen(vca.eX1+oX, vca.eY1+oY, tempPt2);
                    gridToScreen(vca.eX2+oX, vca.eY2+oY, tempPt3);
                    drawCircleArc(tempPt1, tempPt2, tempPt3, vca.thick, raster);
                }
            }
		}
	}

	/**
	 * Method to draw a list of cached port shapes.
	 * @param oX the X offset to draw the shapes (in database grid coordinates).
	 * @param oY the Y offset to draw the shapes (in database grid coordinates).
     * @parem true to draw a list on expanded instance
	 */
	private void drawPortList(VectorCache.VectorSubCell vsc, VectorCache.VectorCell subVC_, int oX, int oY, boolean expanded)
//		throws AbortRenderingException
	{
        if (!User.isTextVisibilityOnPort()) return;
		// render all shapes
		for(VectorCache.VectorText vt : subVC_.getPortShapes())
		{
//			if (stopRendering) throw new AbortRenderingException();

			// get visual characteristics of shape
            assert vt.textType == VectorCache.VectorText.TEXTTYPEPORT;
            if (vsc.shownPorts.get(vt.e.getId().getChronIndex())) continue;
//			if (vt.height < maxTextSize) continue;

            String drawString = vt.str;
            int lX = vt.bounds.x;
            int lY = vt.bounds.y;
            int hX = lX + vt.bounds.width;
            int hY = lY + vt.bounds.height;
            gridToScreen(lX + oX, hY + oY, tempPt1);
            gridToScreen(hX + oX, lY + oY, tempPt2);
            lX = tempPt1.x;
            lY = tempPt1.y;
            hX = tempPt2.x;
            hY = tempPt2.y;

			int portDisplayLevel = User.getPortDisplayLevel();
			Color portColor = vt.e.getBasePort().getPortColor();
			if (expanded) portColor = textColor;
			if (portColor != null) portGraphics.setColor(portColor);
			int cX = (lX + hX) / 2;
			int cY = (lY + hY) / 2;
			if (portDisplayLevel == 2)
			{
				// draw port as a cross
                drawCross(cX, cY, 3, portGraphics, false);
				crossCount++;
				continue;
			}

			// draw port as text
			if (portDisplayLevel == 1) drawString = vt.e.getShortName(); else
				drawString = vt.e.getName();
			lX = hX = cX;
			lY = hY = cY;
            
			textCount++;
			tempRect.setBounds(lX, lY, hX-lX, hY-lY);
			drawText(tempRect, vt.style, vt.descript, drawString, portGraphics, false);
		}
	}

	byte [] getLayerBitMap(int layerNum)
	{
		if (layerNum < 0) return null;

		byte [] layerBitMap = layerBitMaps[layerNum];
		if (layerBitMap != null) return layerBitMap;
		// allocate this bitplane dynamically
        return newLayerBitMap(layerNum);
	}

    private byte [] newLayerBitMap(int layerNum) {
        byte [] layerBitMap= new byte[sz.height*numBytesPerRow];
        layerBitMaps[layerNum] = layerBitMap;
        numLayerBitMapsCreated++;
        return layerBitMap;
        
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
    public void drawBox(int lX, int hX, int lY, int hY, ERaster raster) {
        if (lX < clipLX) lX = clipLX;
        if (hX > clipHX) hX = clipHX;
        if (lY < clipLY) lY = clipLY;
        if (hY > clipHY) hY = clipHY;
        if (lX > hX || lY > hY) return;
        EGraphics.Outline o = raster.getOutline();
        if (lY == hY) {
            if (lX == hX) {
                if (o == null)
                    raster.fillPoint(lX, lY);
                else
                    raster.drawPoint(lX, lY);
            } else {
                if (o == null)
                    raster.fillHorLine(lY, lX, hX);
                else
                    raster.drawHorLine(lY, lX, hX);
            }
            return;
        }
        if (lX == hX) {
            if (o == null)
                raster.fillVerLine(lX, lY, hY);
            else
                raster.drawVerLine(lX, lY, hY);
            return;
        }
        raster.fillBox(lX, hX, lY, hY);
        if (o == null) return;
        if (o.isSolidPattern()) {
            raster.drawVerLine(lX, lY, hY);
            raster.drawHorLine(hY, lX, hX);
            raster.drawVerLine(hX, lY, hY);
            raster.drawHorLine(lY, lX, hX);
            if (o.getThickness() != 1) {
                for(int i=1; i<o.getThickness(); i++) {
                    if (lX + i <= clipHX)
                        raster.drawVerLine(lX+i, lY, hY);
                    if (hY - i >= clipLX)
                        raster.drawHorLine(hY-i, lX, hX);
                    if (hX - i >= clipLY)
                        raster.drawVerLine(hX-i, lY, hY);
                    if (lY + i <= clipHY)
                        raster.drawHorLine(lY+i, lX, hX);
                }
            }
        } else {
            int pattern = o.getPattern();
            int len = o.getLen();
            drawVerOutline(lX, lY, hY, pattern, len, raster);
            drawHorOutline(hY, lX, hX, pattern, len, raster);
            drawVerOutline(hX, lY, hY, pattern, len, raster);
            drawHorOutline(lY, lX, hX, pattern, len, raster);
            if (o.getThickness() != 1) {
                for(int i=1; i<o.getThickness(); i++) {
                    if (lX + i <= clipHX)
                        drawVerOutline(lX+i, lY, hY, pattern, len, raster);
                    if (hY - i >= clipLX)
                        drawHorOutline(hY-i, lX, hX, pattern, len, raster);
                    if (hX - i >= clipLY)
                        drawVerOutline(hX-i, lY, hY, pattern, len, raster);
                    if (lY + i <= clipHY)
                        drawHorOutline(lY+i, lX, hX, pattern, len, raster);
                }
            }
        }
    }
    
    private static void drawHorOutline(int y, int lX, int hX, int pattern, int len, ERaster raster) {
        int i = 0;
        for (int x = lX; x <= hX; x++) {
            if ((pattern & (1 << i)) != 0)
                raster.drawPoint(x, y);
            i++;
            if (i == len) i = 0;
        }
    }
    
    private static void drawVerOutline(int x, int lY, int hY, int pattern, int len, ERaster raster) {
        int i = 0;
        for (int y = lY; y <= hY; y++) {
            if ((pattern & (1 << i)) != 0)
                raster.drawPoint(x, y);
            i++;
            if (i == len) i = 0;
        }
    }
    
    /**
     * Method to draw a box on the off-screen buffer.
     */
    private void drawBox(int lX, int hX, int lY, int hY, byte[] layerBitMap, byte layerBitMask) {
        boxes++;
        int dx = hX - lX;
        int dy = hY - lY;
        int baseIndex = lY * width + lX;
        if (dx >= dy) {
            int baseIncr = width - (dx + 1);
            for (int i = dy; i >= 0; i--) {
                for (int j = dx; j >= 0; j--) {
                    layerBitMap[baseIndex] |= layerBitMask;
                    baseIndex += 1;
                }
                baseIndex += baseIncr;
            }
        } else {
            int baseIncr = 1 - (dy + 1) * width;
            for (int i = dx; i >= 0; i--) {
                for (int j = dy; j >= 0; j--) {
                    layerBitMap[baseIndex] |= layerBitMask;
                    baseIndex += width;
                }
                baseIndex += baseIncr;
            }
        }
    }

	// ************************************* LINE DRAWING *************************************

	/**
	 * Method to draw a line on the off-screen buffer.
	 */
	void drawLine(Point pt1, Point pt2, int texture, ERaster raster)
	{
		// first clip the line
		if (GenMath.clipLine(pt1, pt2, 0, sz.width-1, 0, sz.height-1)) return;

		// now draw with the proper line type
		switch (texture)
		{
			case 0: drawSolidLine(pt1.x, pt1.y, pt2.x, pt2.y, raster);          break;
			case 1: drawPatLine(pt1.x, pt1.y, pt2.x, pt2.y, 0x88, 8, raster);   break;
			case 2: drawPatLine(pt1.x, pt1.y, pt2.x, pt2.y, 0xE7, 8, raster);   break;
			case 3: drawThickLine(pt1.x, pt1.y, pt2.x, pt2.y, raster);          break;
		}
	}

    private void drawCross(int cX, int cY, int size, EGraphics graphics, boolean dimmed) {
        crosses++;
        int col = getTheColor(graphics, dimmed);
        if (clipLY <= cY && cY <= clipHY) {
            int baseIndex = cY * width;
            for (int x = Math.max(clipLX, cX - size), xend = Math.min(clipLY, cX + size); x <= xend; x++)
                opaqueData[baseIndex + x] = col;
        }
        if (clipLX <= cX && cX <= clipHX) {
            int baseIndex = cX;
            for (int y = Math.max(clipLY, cY - size), yend = Math.min(clipHY, cY + size); y <= yend; y++)
                opaqueData[y * width + baseIndex] = col;
        }
    }

    private void drawCross(int cX, int cY, int size, ERaster raster) {
        crosses++;
        if (clipLY <= cY && cY <= clipHY) {
            int lX = Math.max(clipLX, cX - size);
            int hX = Math.min(clipHY, cX + size);
            if (lX <= hX)
                raster.drawHorLine(cY, lX, hX);
        }
        if (clipLX <= cX && cX <= clipHX) {
            int lY = Math.max(clipLY, cY - size);
            int hY = Math.min(clipHY, cY + size);
            if (lY <= hY)
                raster.drawVerLine(cX, lY, hY);
        }
    }

	private void drawSolidLine(int x1, int y1, int x2, int y2, ERaster raster)
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
            raster.drawPoint(x, y);

			// draw line that increments along X
			while (x < xend)
			{
				x++;
				if (d < 0) d += incr1; else
				{
					y += yincr;   d += incr2;
				}
                raster.drawPoint(x, y);
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
            raster.drawPoint(x, y);

			// draw line that increments along X
			while (y < yend)
			{
				y++;
				if (d < 0) d += incr1; else
				{
					x += xincr;   d += incr2;
				}
                raster.drawPoint(x, y);
			}
		}
	}

    private void drawSolidLine(int x1, int y1, int x2, int y2, byte[] layerBitMap, byte layerBitMask) {
        solidLines++;
        // initialize the Bresenham algorithm
        int dx = Math.abs(x2-x1);
        int dy = Math.abs(y2-y1);
        if (dx >= dy) {
            // initialize for lines that increment along X
            int incr1 = 2 * dy;
            int incr2 = 2 * (dy - dx);
            int d = incr2;
            int x, y, yend;
            if (x1 <= x2) {
                x = x1;   y = y1;   yend = y2;
            } else {
                x = x2;   y = y2;   yend = y1;
            }
            int baseIndex = y * width + x;
            if (dy == 0) {
                // draw horizontal line
                for (int i = dx; i >= 0; i--)
                    layerBitMap[baseIndex++] |= layerBitMask;
            } else {
                // draw line that increments along X
                int baseIncr = yend >= y ? 1 + width : 1 - width;
                for (int i = dx; i >= 0; i--) {
                    layerBitMap[baseIndex] |= layerBitMask;
                    if (d < 0) {
                        d += incr1;
                        baseIndex += 1;
                    } else {
                        d += incr2;
                        baseIndex += baseIncr;
                    }
                }
            }
        } else {
            // initialize for lines that increment along Y
            int incr1 = 2 * dx;
            int incr2 = 2 * (dx - dy);
            int d = incr2;
            int x, y, xend;
            if (y1 <= y2) {
                x = x1;   y = y1;   xend = x2;
            } else {
                x = x2;   y = y2;   xend = x1;
            }
            int baseIndex = y * width + x;
            if (dx == 0) {
                // draw vertical line
                for (int i = dy; i >= 0; i--) {
                    layerBitMap[baseIndex] |= layerBitMask;
                    baseIndex += width;
                }
            } else {
                int baseIncr = xend >= x ? width + 1 : width - 1;
                for (int i = dy; i >= 0; i--) {
                    layerBitMap[baseIndex] |= layerBitMask;
                    if (d < 0) {
                        d += incr1;
                        baseIndex += width;
                    } else {
                        d += incr2;
                        baseIndex += baseIncr;
                    }
                }
            }
        }
    }

	private void drawOutline(int x1, int y1, int x2, int y2, int pattern, int len, ERaster raster)
	{
        tempPt3.x = x1;
        tempPt3.y = y1;
        tempPt4.x = x2;
        tempPt4.y = y2;
        
		// first clip the line
		if (GenMath.clipLine(tempPt3, tempPt4, 0, sz.width-1, 0, sz.height-1)) return;

        drawPatLine(tempPt3.x, tempPt3.y, tempPt4.x, tempPt4.y, pattern, len, raster);
	}

	private void drawPatLine(int x1, int y1, int x2, int y2, int pattern, int len, ERaster raster)
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
            raster.drawPoint(x, y);

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
                raster.drawPoint(x, y);
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
            raster.drawPoint(x, y);

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
                raster.drawPoint(x, y);
			}
		}
	}

    private void drawPatLine(int x1, int y1, int x2, int y2, byte[] layerBitMap, byte layerBitMask, int pattern, int len) {
        patLines++;
        // initialize the Bresenham algorithm
        int dx = Math.abs(x2-x1);
        int dy = Math.abs(y2-y1);
        if (dx >= dy) {
            // initialize for lines that increment along X
            int incr1 = 2 * dy;
            int incr2 = 2 * (dy - dx);
            int d = incr2;
            int x, y, yend;
            if (x1 <= x2) {
                x = x1;   y = y1;   yend = y2;
            } else {
                x = x2;   y = y2;   yend = y1;
            }
            int baseIndex = y * width + x;
            if (dy == 0) {
                // draw horizontal line
                for (int i = 0; i <= dx; i++) {
                    if ((pattern & (1 << (i&7))) != 0)
                        layerBitMap[baseIndex++] |= layerBitMask;
                }
            } else {
                // draw line that increments along X
                int baseIncr = yend >= y ? 1 + width : 1 - width;
                for (int i = 0; i <= dx; i++) {
                    if ((pattern & (1 << (i&7))) != 0)
                        layerBitMap[baseIndex] |= layerBitMask;
                    if (d < 0) {
                        d += incr1;
                        baseIndex += 1;
                    } else {
                        d += incr2;
                        baseIndex += baseIncr;
                    }
                }
            }
        } else {
            // initialize for lines that increment along Y
            int incr1 = 2 * dx;
            int incr2 = 2 * (dx - dy);
            int d = incr2;
            int x, y, xend;
            if (y1 <= y2) {
                x = x1;   y = y1;   xend = x2;
            } else {
                x = x2;   y = y2;   xend = x1;
            }
            int baseIndex = y * width + x;
            if (dx == 0) {
                // draw vertical line
                for (int i = 0; i <= dy; i++) {
                    if ((pattern & (1 << (i&7))) != 0)
                        layerBitMap[baseIndex] |= layerBitMask;
                    baseIndex += width;
                }
            } else {
                int baseIncr = xend >= x ? width + 1 : width - 1;
                for (int i = 0; i <= dy; i++) {
                    if ((pattern & (1 << (i&7))) != 0)
                        layerBitMap[baseIndex] |= layerBitMask;
                    if (d < 0) {
                        d += incr1;
                        baseIndex += width;
                    } else {
                        d += incr2;
                        baseIndex += baseIncr;
                    }
                }
            }
        }
    }

	private void drawThickLine(int x1, int y1, int x2, int y2, ERaster raster)
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
			drawThickPoint(x, y, raster);

			// draw line that increments along X
			while (x < xend)
			{
				x++;
				if (d < 0) d += incr1; else
				{
					y += yincr;
					d += incr2;
				}
				drawThickPoint(x, y, raster);
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
			drawThickPoint(x, y, raster);

			// draw line that increments along X
			while (y < yend)
			{
				y++;
				if (d < 0) d += incr1; else
				{
					x += xincr;
					d += incr2;
				}
				drawThickPoint(x, y, raster);
			}
		}
	}

    private void drawThickLine(int x1, int y1, int x2, int y2, byte[] layerBitMap, byte layerBitMask) {
        thickLines++;
        // initialize the Bresenham algorithm
        int dx = Math.abs(x2-x1);
        int dy = Math.abs(y2-y1);
        if (dx >= dy) {
            // initialize for lines that increment along X
            int incr1 = 2 * dy;
            int incr2 = 2 * (dy - dx);
            int d = incr2;
            int x, y, xend, yend;
            if (x1 <= x2) {
                x = x1;   y = y1;   xend = x2;  yend = y2;
            } else {
                x = x2;   y = y2;   xend = x1;  yend = y1;
            }
            if (dy == 0) {
                // draw horizontal line
                drawBox(x, xend, Math.max(clipLY, y - 1), Math.min(clipHY, y + 1), layerBitMap, layerBitMask);
                if (x > clipLX)
                    drawPoint(x - 1, y, layerBitMap, layerBitMask);
                if (xend < clipHX)
                    drawPoint(xend + 1, y, layerBitMap, layerBitMask);
            } else {
                // draw line that increments along X
                int yIncr = yend >= y ? 1 : -1;
                for (int i = 0; i <= dx; i++) {
                    drawThickPoint(x + i, y, layerBitMap, layerBitMask);
                    if (d < 0) {
                        d += incr1;
                    } else {
                        d += incr2;
                        y += yIncr;
                    }
                }
            }
        } else {
            // initialize for lines that increment along Y
            int incr1 = 2 * dx;
            int incr2 = 2 * (dx - dy);
            int d = incr2;
            int x, y, xend, yend;
            if (y1 <= y2) {
                x = x1;   y = y1;   xend = x2;  yend = y2;
            } else {
                x = x2;   y = y2;   xend = x1;  yend = x1;
            }
            if (dx == 0) {
                // draw vertical line
                drawBox(Math.max(clipLX, x - 1), Math.min(clipHX, x + 1), y, yend, layerBitMap, layerBitMask);
                if (y > clipLY)
                    drawPoint(x, y - 1, layerBitMap, layerBitMask);
                if (yend < clipHY)
                    drawPoint(x, yend + 1, layerBitMap, layerBitMask);
            } else {
                int xIncr = xend >= x ? 1 : - 1;
                for (int i = 0; i <= dy; i++) {
                    drawThickPoint(x, y + i, layerBitMap, layerBitMask);
                    if (d < 0) {
                        d += incr1;
                    } else {
                        d += incr2;
                        x += xIncr;
                    }
                }
            }
        }
    }

	// ************************************* POLYGON DRAWING *************************************

	/**
	 * Method to draw a polygon on the off-screen buffer.
	 */
	void drawPolygon(Point [] points, ERaster raster)
	{
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

                    raster.fillHorLine(ycur, j, k);
                    
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
        EGraphics.Outline o = raster.getOutline();
        if (o == null) return;
        for(int i=0; i<points.length; i++) {
            int last = i-1;
            if (last < 0) last = points.length - 1;
            int fX = points[last].x;   int fY = points[last].y;
            int tX = points[i].x;      int tY = points[i].y;
            drawOutline(fX, fY, tX, tY, o.getPattern(), o.getLen(), raster);
            if (o.getThickness() != 1) {
                int ang = GenMath.figureAngle(new Point2D.Double(fX, fY), new Point2D.Double(tX, tY));
                double sin = DBMath.sin(ang+900);
                double cos = DBMath.cos(ang+900);
                for(int t=1; t<o.getThickness(); t++) {
                    int dX = (int)(cos*t + 0.5);
                    int dY = (int)(sin*t + 0.5);
                    drawOutline(fX+dX, fY+dY, tX+dX, tY+dY, o.getPattern(), o.getLen(), raster);
                }
            }
		}
	}

	// ************************************* TEXT DRAWING *************************************

	/**
	 * Method to draw a text on the off-screen buffer
	 */
	public void drawText(Rectangle rect, Poly.Type style, TextDescriptor descript, String s, EGraphics desc, boolean dimmed)
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

            // greeked box in opaque area
            for(int y=lY; y<=hY; y++) {
                int baseIndex = y * sz.width + lX;
                for(int x=lX; x<=hX; x++) {
                    int index = baseIndex++;
                    int alpha = (opaqueData[index] >> 24) & 0xFF;
                    if (alpha == 0xFF) opaqueData[index] = col;
                }
            }
            return;
		}

		// check if text is on-screen
        if (renderInfo.bounds.getMinX() >= sz.width || renderInfo.bounds.getMaxX() < 0 ||
            renderInfo.bounds.getMinY() >= sz.height || renderInfo.bounds.getMaxY() < 0)
                return;

		// render the text
		Raster ras = renderText(renderInfo);
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
					int baseIndex = trueY * sz.width;
					int samp = (y>>shiftUp) * textImageWidth;
					for(int x=sx; x<ex; x++)
					{
						int trueX = atX + x;
						int alpha = samples[samp + (x>>shiftUp)] & 0xFF;
						if (alpha == 0) continue;
                        // drawing opaque
                        int fullIndex = baseIndex + trueX;
                        int pixelValue = opaqueData[fullIndex];
                        int oldAlpha = (pixelValue >> 24) & 0xFF;
                        int color = col;
                        if (oldAlpha == 0) {
                            // blend with opaque
                            if (alpha != 0xFF) color = alphaBlend(color, pixelValue, alpha);
                        } else if (oldAlpha == 0xFF) {
                            // blend with background
                            if (alpha < 255) color = (color & 0xFFFFFF) | (alpha << 24);
                        }
                        opaqueData[fullIndex] = color;
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
					int baseIndex = trueY * sz.width;
					for(int x=sx; x<ex; x++)
					{
						int trueX = atX + x;
						int alpha = samples[(x>>shiftUp) * textImageWidth + (y>>shiftUp)] & 0xFF;
                        if (alpha == 0) continue;
                        // drawing opaque
                        int fullIndex = baseIndex + trueX;
                        int pixelValue = opaqueData[fullIndex];
                        int oldAlpha = (pixelValue >> 24) & 0xFF;
                        int color = col;
                        if (oldAlpha == 0) {
                            // blend with opaque
                            if (alpha != 0xFF) color = alphaBlend(color, pixelValue, alpha);
                        } else if (oldAlpha == 0xFF) {
                            // blend with background
                            if (alpha < 255) color = (color & 0xFFFFFF) | (alpha << 24);
                        }
                        opaqueData[fullIndex] = color;
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
					int baseIndex = trueY * sz.width;
					for(int x=sx; x<ex; x++)
					{
						int trueX = atX + x;
						int index = ((rasHeight-y-1)>>shiftUp) * textImageWidth + ((rasWidth-x-1)>>shiftUp);
						int alpha = samples[index] & 0xFF;
						if (alpha == 0) continue;
                        // drawing opaque
                        int fullIndex = baseIndex + trueX;
                        int pixelValue = opaqueData[fullIndex];
                        int oldAlpha = (pixelValue >> 24) & 0xFF;
                        int color = col;
                        if (oldAlpha == 0) {
                            // blend with opaque
                            if (alpha != 0xFF) color = alphaBlend(color, pixelValue, alpha);
                        } else if (oldAlpha == 0xFF) {
                            // blend with background
                            if (alpha < 255) color = (color & 0xFFFFFF) | (alpha << 24);
                        }
                        opaqueData[fullIndex] = color;
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
					int baseIndex = trueY * sz.width;
					for(int x=sx; x<ex; x++)
					{
						int trueX = atX - x;
						int alpha = samples[(x>>shiftUp) * textImageWidth + (y>>shiftUp)] & 0xFF;
						if (alpha == 0) continue;
                        // drawing opaque
                        int fullIndex = baseIndex + trueX;
                        int pixelValue = opaqueData[fullIndex];
                        int oldAlpha = (pixelValue >> 24) & 0xFF;
                        int color = col;
                        if (oldAlpha == 0) {
                            // blend with opaque
                            if (alpha != 0xFF) color = alphaBlend(color, pixelValue, alpha);
                        } else if (oldAlpha == 0xFF) {
                            // blend with background
                            if (alpha < 255) color = (color & 0xFFFFFF) | (alpha << 24);
                        }
                        opaqueData[fullIndex] = color;
					}
				}
				break;
		}
	}

    /**
	 * Method to draw a text on the off-screen buffer
	 */
	private void drawText(Rectangle rect, Poly.Type style, TextDescriptor descript, String s, byte[] layerBitMap, byte layerBitMask) {
        texts++;
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
	void drawCircle(Point center, Point edge, ERaster raster)
	{
		// get parameters
		int radius = (int)center.distance(edge);

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
                raster.drawPoint(center.x + x, center.y + y);
                raster.drawPoint(center.x - x, center.y + y);
                raster.drawPoint(center.x + x, center.y - y);
                raster.drawPoint(center.x - x, center.y - y);
                raster.drawPoint(center.x + y, center.y + x);
                raster.drawPoint(center.x - y, center.y + x);
                raster.drawPoint(center.x + y, center.y - x);
                raster.drawPoint(center.x - y, center.y - x);

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
						raster.drawPoint(thisx, thisy);
					thisx = center.x - x;
					if (thisx >= 0 && thisx < sz.width)
						raster.drawPoint(thisx, thisy);
				}

				thisy = center.y - y;
				if (thisy >= 0 && thisy < sz.height)
				{
					int thisx = center.x + x;
					if (thisx >= 0 && thisx < sz.width)
						raster.drawPoint(thisx, thisy);
					thisx = center.x - x;
					if (thisx >= 0 && thisx < sz.width)
						raster.drawPoint(thisx, thisy);
				}

				thisy = center.y + x;
				if (thisy >= 0 && thisy < sz.height)
				{
					int thisx = center.x + y;
					if (thisx >= 0 && thisx < sz.width)
						raster.drawPoint(thisx, thisy);
					thisx = center.x - y;
					if (thisx >= 0 && thisx < sz.width)
						raster.drawPoint(thisx, thisy);
				}

				thisy = center.y - x;
				if (thisy >= 0 && thisy < sz.height)
				{
					int thisx = center.x + y;
					if (thisx >= 0 && thisx < sz.width)
						raster.drawPoint(thisx, thisy);
					thisx = center.x - y;
					if (thisx >= 0 && thisx < sz.width)
						raster.drawPoint(thisx, thisy);
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
	 * Method to draw a circle on the off-screen buffer
	 */
	private void drawCircle(Point center, Point edge, byte[] layerBitMap, byte layerBitMask) {
        circles++;
    }
    
	/**
	 * Method to draw a thick circle on the off-screen buffer
	 */
	void drawThickCircle(Point center, Point edge, ERaster raster)
	{
		// get parameters
		int radius = (int)center.distance(edge);

		int x = 0;   int y = radius;
		int d = 3 - 2 * radius;
		while (x <= y)
		{
			int thisy = center.y + y;
			if (thisy >= 0 && thisy < sz.height)
			{
				int thisx = center.x + x;
				if (thisx >= 0 && thisx < sz.width)
					drawThickPoint(thisx, thisy, raster);
				thisx = center.x - x;
				if (thisx >= 0 && thisx < sz.width)
					drawThickPoint(thisx, thisy, raster);
			}

			thisy = center.y - y;
			if (thisy >= 0 && thisy < sz.height)
			{
				int thisx = center.x + x;
				if (thisx >= 0 && thisx < sz.width)
					drawThickPoint(thisx, thisy, raster);
				thisx = center.x - x;
				if (thisx >= 0 && thisx < sz.width)
					drawThickPoint(thisx, thisy, raster);
			}

			thisy = center.y + x;
			if (thisy >= 0 && thisy < sz.height)
			{
				int thisx = center.x + y;
				if (thisx >= 0 && thisx < sz.width)
					drawThickPoint(thisx, thisy, raster);
				thisx = center.x - y;
				if (thisx >= 0 && thisx < sz.width)
					drawThickPoint(thisx, thisy, raster);
			}

			thisy = center.y - x;
			if (thisy >= 0 && thisy < sz.height)
			{
				int thisx = center.x + y;
				if (thisx >= 0 && thisx < sz.width)
					drawThickPoint(thisx, thisy, raster);
				thisx = center.x - y;
				if (thisx >= 0 && thisx < sz.width)
					drawThickPoint(thisx, thisy, raster);
			}

			if (d < 0) d += 4*x + 6; else
			{
				d += 4 * (x-y) + 10;
				y--;
			}
			x++;
		}
	}

	/**
	 * Method to draw a thick circle on the off-screen buffer
	 */
	private void drawThickCircle(Point center, Point edge, byte[] layerBitMap, byte layerBitMask) {
        thickCircles++;
    }
    
	// ************************************* DISC DRAWING *************************************

	/**
	 * Method to draw a scan line of the filled-in circle of radius "radius"
	 */
	private void drawDiscRow(int thisy, int startx, int endx, ERaster raster)
	{
		if (thisy < 0 || thisy >= sz.height) return;
		if (startx < 0) startx = 0;
		if (endx >= sz.width) endx = sz.width - 1;
        raster.drawHorLine(thisy, startx, endx);
	}

	/**
	 * Method to draw a filled-in circle of radius "radius" on the off-screen buffer
	 */
	void drawDisc(Point center, Point edge, ERaster raster)
	{
		// get parameters
		int radius = (int)center.distance(edge);
        EGraphics.Outline o = raster.getOutline();
        if (o != null)
            drawCircle(center, edge, raster);

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
					raster.drawPoint(x, y);
			}
			return;
		}

		int x = 0;   int y = radius;
		int d = 3 - 2 * radius;
		while (x <= y)
		{
			drawDiscRow(center.y+y, center.x-x, center.x+x, raster);
			drawDiscRow(center.y-y, center.x-x, center.x+x, raster);
			drawDiscRow(center.y+x, center.x-y, center.x+y, raster);
			drawDiscRow(center.y-x, center.x-y, center.x+y, raster);

			if (d < 0) d += 4*x + 6; else
			{
				d += 4 * (x-y) + 10;
				y--;
			}
			x++;
		}
	}

	/**
	 * Method to draw a filled-in circle of radius "radius" on the off-screen buffer
	 */
	private void drawDisc(Point center, Point edge, byte[] layerBitMap, byte layerBitMask) {
        discs++;
    }
    
	// ************************************* ARC DRAWING *************************************

	private boolean [] arcOctTable = new boolean[9];
	private Point      arcCenter;
	private int        arcRadius;
	private int        arcCol;
	private ERaster    arcRaster;
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
		if (x < clipLX || x > clipHX || y < clipLY || y > clipHY) return;
		if (arcThick)
		{
			drawThickPoint(x, y, arcRaster);
		} else
		{
			arcRaster.drawPoint(x, y);
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
	void drawCircleArc(Point center, Point p1, Point p2, boolean thick, ERaster raster)
	{
		// ignore tiny arcs
		if (p1.x == p2.x && p1.y == p2.y) return;

		// get parameters
		arcRaster = raster;

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

	/**
	 * draws an arc centered at (centerx, centery), clockwise,
	 * passing by (x1,y1) and (x2,y2)
	 */
	private void drawCircleArc(Point center, Point p1, Point p2, boolean thick, byte[] layerBitMap, byte layerBitMask) {
        circleArcs++;
    }
    
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

    private void drawPoint(int x, int y, byte[] layerBitMap, byte layerBitMask) {
        points++;
        layerBitMap[y * width + x] |= layerBitMask;
    }
    
    private void drawThickPoint(int x, int y, ERaster raster) {
        raster.drawPoint(x, y);
        if (x > clipLX)
            raster.drawPoint(x - 1, y);
        if (x < clipHX)
            raster.drawPoint(x + 1, y);
        if (y > clipLY)
            raster.drawPoint(x, y - 1);
        if (y < sz.height-1)
            raster.drawPoint(x, y + 1);
    }

    private void drawThickPoint(int x, int y, byte[] layerBitMap, byte layerBitMask) {
        thickPoints++;
        int baseIndex = y * sz.width + x;
        layerBitMap[baseIndex] |= layerBitMask;
        if (x > clipLX)
            layerBitMap[baseIndex - 1] |= layerBitMask;
        if (x < clipHX)
            layerBitMap[baseIndex + 1] |= layerBitMask;
        if (y > clipLY)
            layerBitMap[baseIndex - width] |= layerBitMask;
        if (y < sz.height-1)
            layerBitMap[baseIndex + width] |= layerBitMask;
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
