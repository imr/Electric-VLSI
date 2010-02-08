/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayerDrawing.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.redisplay;

import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.user.GraphicsPreferences;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.LayerVisibility;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
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
import java.awt.image.DataBufferInt;
import java.awt.image.VolatileImage;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

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
class LayerDrawing {

    /** Text smaller than this will not be drawn. */
    public static final int MINIMUMTEXTSIZE = 5;
    /** Number of singleton cells to cache when redisplaying. */
    public static final int SINGLETONSTOADD = 5;
    /** Text size is limited by this. */
    public static final int MAXIMUMTEXTSIZE = 200;

    private static class PolySeg {

        private int fx, fy, tx, ty, direction, increment;
        private PolySeg nextedge;
        private PolySeg nextactive;
    }
    // statistics stuff
    private static final boolean TAKE_STATS = false;
    private static int tinyCells, tinyPrims, totalCells, renderedCells, totalPrims, tinyArcs, linedArcs, totalArcs;
    private static int offscreensCreated, offscreenPixelsCreated, offscreensUsed, offscreenPixelsUsed, cellsRendered;
    private static Set<ExpandedCellKey> offscreensUsedSet = new HashSet<ExpandedCellKey>();
    private static int boxArrayCount, boxCount, boxDisplayCount, lineCount, polygonCount, crossCount, circleCount, discCount, arcCount;
    private static final boolean DEBUG = false;

    private static class ExpandedCellKey {

        private CellId cellId;
        private Orientation orient;

        private ExpandedCellKey(CellId cellId, Orientation orient) {
            this.cellId = cellId;
            this.orient = orient;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ExpandedCellKey) {
                ExpandedCellKey that = (ExpandedCellKey) obj;
                return this.cellId == that.cellId && this.orient.equals(that.orient);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return cellId.hashCode() ^ orient.hashCode();
        }
    }

    /**
     * This class holds information about expanded cell instances.
     * For efficiency, Electric remembers the bits in an expanded cell instance
     * and uses them when another expanded instance appears elsewhere.
     * Of course, the orientation of the instance matters, so each combination of
     * cell and orientation forms a "cell cache".  The Cell Cache is stored in the
     * "wnd" field (which has its own PixelDrawing object).
     */
    private static class ExpandedCellInfo {

        private boolean singleton;
        private int instanceCount;
        private boolean tooLarge;
        private LayerDrawing offscreen;

        ExpandedCellInfo() {
            singleton = true;
            offscreen = null;
        }
    }
    /** the size of the EditWindow */
    private final Dimension sz;
    /** the scale of the EditWindow */
    private double scale;
    /** the global text scale of the EditWindow */
    private double globalTextScale;
    /** the default font of the EditWindow */
    private String defaultFont;
    /** the VarContext of the EditWindow */
    private VarContext varContext;
    /** the X origin of the cell in display coordinates. */
    private double originX;
    /** the Y origin of the cell in display coordinates. */
    private double originY;
    /** the scale of the EditWindow */
    private double scale_;
    /** the window scale and pan factor */
    private float factorX, factorY;
    /** 0: color display, 1: color printing, 2: B&W printing */
    private static final int nowPrinting = 0;
    /** true if the last display was a full-instantiate */
    private boolean lastFullInstantiate = false;
    /** A set of subcells being in-place edited. */
    private BitSet inPlaceSubcellPath;
    /** The current cell being in-place edited. */
    private Cell inPlaceCurrent;
    /** true if text can be drawn (not too zoomed-out) */
    private boolean canDrawText;
    /** Threshold for relative text can be drawn */
    private double canDrawRelativeText = Double.MAX_VALUE;
    /** maximum size before an object is too small */
    private static double maxObjectSize;
    /** temporary objects (saves reallocation) */
    private final Point tempPt1 = new Point(), tempPt2 = new Point();
    /** temporary objects (saves reallocation) */
    private final Point tempPt3 = new Point(), tempPt4 = new Point();
    // the full-depth image
    /** size of the opaque layer of the window */
    private final int total;
    /** list of render text. */
    private final ArrayList<RenderTextInfo> renderTextList = new ArrayList<RenderTextInfo>();
    /** list of greek text. */
    private final ArrayList<GreekTextInfo> greekTextList = new ArrayList<GreekTextInfo>();
    /** list of cross text. */
    private final ArrayList<CrossTextInfo> crossTextList = new ArrayList<CrossTextInfo>();
    // the transparent bitmaps
    /** the number of ints per row in offscreen maps */
    private final int numIntsPerRow;
    /** the map from layers to layer bitmaps */
    private Map<Layer, TransparentRaster> layerRasters = new HashMap<Layer, TransparentRaster>();
    /** Raster with unexpanded instances */
    private TransparentRaster instanceRaster;
    /** Raster with grid */
    private TransparentRaster gridRaster;
    /** temporary raster for patterned layers */
    private PatternedTransparentRaster currentPatternedTransparentRaster = new PatternedTransparentRaster();
    /** the top-level window being rendered */
    private boolean renderedWindow;
    /** whether to occasionally update the display. */
    private boolean periodicRefresh;
    /** keeps track of when to update the display. */
    private int objectCount;
    /** keeps track of when to update the display. */
    private long lastRefreshTime;
    /** the EditWindow being drawn */
    private EditWindow wnd;
    /** the size of the top-level EditWindow */
    private static Dimension topSz;
    /** draw layers patterned (depends on scale). */
    private boolean patternedDisplay;
    /** Alpha blending with overcolor (depends on scale). */
    private static boolean alphaBlendingOvercolor;
    /** list of cell expansions. */
    private static Map<ExpandedCellKey, ExpandedCellInfo> expandedCells = null;
    /** Set of changed cells. */
    private static final Set<CellId> changedCells = new HashSet<CellId>();
    /** scale of cell expansions. */
    private static double expandedScale = 0;
    /** number of extra cells to render this time */
    private static int numberToReconcile;
    // Color things only at top offscreen
    private GraphicsPreferences gp;
    private AbstractDrawing.DrawingPreferences dp;
    private Color textColor;
    /** cache of port colors */
    private HashMap<PrimitivePort, Color> portColorsCache;
    private int clipLX, clipHX, clipLY, clipHY;
    private final EditWindow0 dummyWnd = new EditWindow0()
    {
        public VarContext getVarContext() { return varContext; }

        public double getScale() { return scale; }

        public double getGlobalTextScale() { return globalTextScale; }

        public String getDefaultFont() { return defaultFont; }
    };

    private static class DrawingData {

        private final LayerDrawing offscreen;
        private final int width;
        private final int height;
        private final int numIntsPerRow;
        private final boolean patternedDisplay;
        /** the map from layers to layer bitmaps */
        private final Map<Layer, TransparentRaster> layerRasters;
        private final TransparentRaster instanceRaster;
        private final TransparentRaster gridRaster;
        private final GreekTextInfo[] greekText;
        private final RenderTextInfo[] renderText;
        private final CrossTextInfo[] crossText;

        DrawingData(LayerDrawing offscreen) {
            this.offscreen = offscreen;
            width = offscreen.sz.width;
            height = offscreen.sz.height;
            numIntsPerRow = offscreen.numIntsPerRow;
            patternedDisplay = offscreen.patternedDisplay;
            layerRasters = new HashMap<Layer, TransparentRaster>(offscreen.layerRasters);
            instanceRaster = offscreen.instanceRaster;
            gridRaster = offscreen.gridRaster;
            greekText = offscreen.greekTextList.toArray(new GreekTextInfo[offscreen.greekTextList.size()]);
            crossText = offscreen.crossTextList.toArray(new CrossTextInfo[offscreen.crossTextList.size()]);
            renderText = offscreen.renderTextList.toArray(new RenderTextInfo[offscreen.renderTextList.size()]);
        }
    }

    static class Drawing extends AbstractDrawing {

        private static final int SMALL_IMG_HEIGHT = 2;
        /** the offscreen opaque image of the window */
        private VolatileImage vImg;
        private BufferedImage smallImg;
        private int[] smallOpaqueData;
        /** alpha blender of layer maps */
        private final AlphaBlender alphaBlender = new AlphaBlender();
        // The following fields are produced by "render" method in Job thread.
        private volatile boolean needComposite;
        private volatile DrawingData drawingData;
        /** whether any layers are highlighted/dimmed */
        boolean highlightingLayers;
        private final double patternedScaleLimit = User.getPatternedScaleLimit();
        private final double alphaBlendingOvercolorLimit = User.getAlphaBlendingOvercolorLimit();

        Drawing(EditWindow wnd) {
            super(wnd);
        }

        /**
         * This method is called from AWT thread.
         */
        @Override
        public boolean paintComponent(Graphics2D g, LayerVisibility lv, Dimension sz) {
            assert SwingUtilities.isEventDispatchThread();
            GraphicsPreferences gp = UserInterfaceMain.getGraphicsPreferences();
            assert sz.equals(wnd.getSize());
            DrawingData drawingData = this.drawingData;
            if (drawingData == null || !drawingData.offscreen.getSize().equals(sz)) {
                return false;
            }

            if (vImg == null || vImg.getWidth() != sz.width || vImg.getHeight() != sz.height) {
                if (vImg != null) {
                    vImg.flush();
                }
                vImg = wnd.createVolatileImage(sz.width, sz.height);
            }

//            smallImg = (BufferedImage)wnd.createImage(sz.width, 1);
            if (smallImg == null || smallImg.getWidth() != sz.width) {
                smallImg = new BufferedImage(sz.width, SMALL_IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
                DataBufferInt smallDbi = (DataBufferInt) smallImg.getRaster().getDataBuffer();
                smallOpaqueData = smallDbi.getData();
            }

            // show the image
            // copying from the image (here, gScreen is the Graphics
            // object for the onscreen window)
            do {
                int returnCode = vImg.validate(wnd.getGraphicsConfiguration());
                if (returnCode == VolatileImage.IMAGE_RESTORED) {
                    // Contents need to be restored
                    renderOffscreen(gp, lv, drawingData);	    // restore contents
                } else if (returnCode == VolatileImage.IMAGE_INCOMPATIBLE) {
                    // old vImg doesn't work with new GraphicsConfig; re-create it
                    vImg.flush();
                    vImg = wnd.createVolatileImage(sz.width, sz.height);
                    renderOffscreen(gp, lv, drawingData);
                } else if (needComposite) {
                    renderOffscreen(gp, lv, drawingData);
                }
                g.drawImage(vImg, 0, 0, wnd);
            } while (vImg.contentsLost());

            return true;
        }

        /**
         * This method is called from AWT thread.
         */
        private void renderOffscreen(GraphicsPreferences gp, LayerVisibility lv, DrawingData dd) {
            needComposite = false;
            do {
                if (vImg.validate(wnd.getGraphicsConfiguration()) == VolatileImage.IMAGE_INCOMPATIBLE) {
                    // old vImg doesn't work with new GraphicsConfig; re-create it
                    vImg = wnd.createVolatileImage(dd.width, dd.height);
                }
                long startTime = System.currentTimeMillis();
                Graphics2D g = vImg.createGraphics();

                // see if any layers are being highlighted/dimmed
                highlightingLayers = false;
                for (Iterator<Layer> it = Technology.getCurrent().getLayers(); it.hasNext();) {
                    Layer layer = it.next();
                    if (lv.isHighlighted(layer)) {
                        highlightingLayers = true;
                    }
                }

                if (User.isLegacyComposite()) {
                    legacyLayerComposite(g, gp, lv, dd);
                } else {
                    layerComposite(g, gp, lv, dd);
                }
//                if (alphaBlendingComposite) {
//                    boolean TRY_OVERBLEND = false;
//                    if (TRY_OVERBLEND) {
//                        layerCompositeSlow(g);
//                    } else {
//                        layerComposite(g);
//                    }
//                } else {
//                    layerCompositeCompatable(g);
//                }
                long compositeTime = System.currentTimeMillis();
                for (GreekTextInfo greekInfo : dd.greekText) {
                    greekInfo.draw(g, lv);
                }
                for (CrossTextInfo crossInfo : dd.crossText) {
                    crossInfo.draw(g, lv);
                }
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                for (RenderTextInfo textInfo : dd.renderText) {
                    textInfo.draw(g, lv);
                }
                g.dispose();
                if (TAKE_STATS) {
                    long endTime = System.currentTimeMillis();
                    System.out.println((alphaBlendingOvercolor ? "alphaBlendingOvercolor took " : "alphaBlending took ")
                            + (compositeTime - startTime) + " msec, textRendering " + dd.renderText.length + "+" + dd.greekText.length + "+" + dd.crossText.length + " took " + (endTime - compositeTime) + " msec");
                }
            } while (vImg.contentsLost());
        }

        @Override
        public void opacityChanged() {
            assert SwingUtilities.isEventDispatchThread();
            needComposite = true;
        }

        /**
         * Retruns true if full repaint is necessary
         * @return
         */
        public boolean visibilityChanged() {
            assert SwingUtilities.isEventDispatchThread();
            needComposite = true;
            return true;
        }

        @Override
        public boolean hasOpacity() {
            return !User.isLegacyComposite();
        }

        private void layerComposite(Graphics2D g, GraphicsPreferences gp, LayerVisibility lv, DrawingData dd) {
            Map<Layer, int[]> layerBits = new HashMap<Layer, int[]>();
            for (Map.Entry<Layer, TransparentRaster> e : dd.layerRasters.entrySet()) {
                layerBits.put(e.getKey(), e.getValue().layerBitMap);
            }
            List<AbstractDrawing.LayerColor> blendingOrder = getBlendingOrder(layerBits.keySet(), gp, lv, dd.patternedDisplay, alphaBlendingOvercolor);
            if (TAKE_STATS) {
                System.out.print("BlendingOrder:");
                for (AbstractDrawing.LayerColor lc : blendingOrder) {
                    int alpha = (int) ((1 - lc.inverseAlpha) * 100 + 0.5);
                    System.out.print(" " + lc.layer.getName() + ":" + alpha);
                }
                System.out.println();
            }

            ArrayList<AbstractDrawing.LayerColor> colors = new ArrayList<AbstractDrawing.LayerColor>();
            ArrayList<int[]> bits = new ArrayList<int[]>();
            for (AbstractDrawing.LayerColor layerColor : blendingOrder) {
                int[] b = layerBits.get(layerColor.layer);
                if (b == null) {
                    continue;
                }
                colors.add(layerColor);
                bits.add(b);
            }
            if (dd.instanceRaster != null) {
                colors.add(new AbstractDrawing.LayerColor(gp.getColor(User.ColorPrefType.INSTANCE)));
                bits.add(dd.instanceRaster.layerBitMap);
            }
            if (dd.gridRaster != null) {
                colors.add(new AbstractDrawing.LayerColor(gp.getColor(User.ColorPrefType.GRID)));
                bits.add(dd.gridRaster.layerBitMap);
            }
            alphaBlender.init(gp.getColor(User.ColorPrefType.BACKGROUND), colors, bits);

            int width = dd.width;
            int height = dd.height;
            int numIntsPerRow = dd.numIntsPerRow;
            int baseByteIndex = 0;
            int y = 0;
            while (y < height) {
                int h = Math.min(SMALL_IMG_HEIGHT, height - y);
                int baseIndex = 0;
                for (int k = 0; k < h; k++) {
                    alphaBlender.composeLine(baseByteIndex, 0, width - 1, smallOpaqueData, baseIndex);
                    baseByteIndex += numIntsPerRow;
                    baseIndex += width;
                }
                g.drawImage(smallImg, 0, y, null);
                y += h;
            }
        }

        /**
         * Method to complete rendering by combining the transparent and opaque imagery.
         * This is called after all rendering is done.
         * @return the offscreen Image with the final display.
         */
        private void legacyLayerComposite(Graphics2D g, GraphicsPreferences gp, LayerVisibility lv, DrawingData dd) {
            getBlendingOrder(dd.layerRasters.keySet(), gp, lv, false, false);

            Technology curTech = Technology.getCurrent();
            if (curTech == null) {
                for (Layer layer : dd.layerRasters.keySet()) {
                    int transparentDepth = gp.getGraphics(layer).getTransparentLayer();
                    if (transparentDepth != 0 && layer.getTechnology() != null) {
                        curTech = layer.getTechnology();
                    }
                }
            }
            if (curTech == null) {
                curTech = Generic.tech();
            }

            // get the technology's color map
            Color[] colorMap = gp.getColorMap(curTech);

            // adjust the colors if any of the transparent layers are dimmed
            boolean dimmedTransparentLayers = false;
            for (Iterator<Layer> it = curTech.getLayers(); it.hasNext();) {
                Layer layer = it.next();
                if (!highlightingLayers || lv.isHighlighted(layer)) {
                    continue;
                }
                if (gp.getGraphics(layer).getTransparentLayer() == 0) {
                    continue;
                }
                dimmedTransparentLayers = true;
                break;
            }
            if (dimmedTransparentLayers) {
                Color[] newColorMap = new Color[colorMap.length];
                int numTransparents = curTech.getNumTransparentLayers();
                boolean[] dimLayer = new boolean[numTransparents];
                for (int i = 0; i < numTransparents; i++) {
                    dimLayer[i] = true;
                }
                for (Iterator<Layer> it = curTech.getLayers(); it.hasNext();) {
                    Layer layer = it.next();
                    if (!lv.isHighlighted(layer)) {
                        continue;
                    }
                    int tIndex = gp.getGraphics(layer).getTransparentLayer();
                    if (tIndex == 0) {
                        continue;
                    }
                    dimLayer[tIndex - 1] = false;
                }

                for (int i = 0; i < colorMap.length; i++) {
                    newColorMap[i] = colorMap[i];
                    if (i == 0) {
                        continue;
                    }
                    boolean dimThisEntry = true;
                    for (int j = 0; j < numTransparents; j++) {
                        if ((i & (1 << j)) != 0) {
                            if (!dimLayer[j]) {
                                dimThisEntry = false;
                                break;
                            }
                        }
                    }
                    if (dimThisEntry) {
                        newColorMap[i] = new Color(dimColor(colorMap[i].getRGB()));
                    } else {
                        newColorMap[i] = new Color(brightenColor(colorMap[i].getRGB()));
                    }
                }
                colorMap = newColorMap;
            }

            int numTransparent = 0, numOpaque = 0;
            for (Layer layer : dd.layerRasters.keySet()) {
                if (!lv.isVisible(layer)) {
                    continue;
                }
                if (gp.getGraphics(layer).getTransparentLayer() == 0) {
                    numOpaque++;
                } else {
                    numTransparent++;
                }
            }
            TransparentRaster[] transparentRasters = new TransparentRaster[numTransparent];
            int[] transparentMasks = new int[numTransparent];
            TransparentRaster[] opaqueRasters = new TransparentRaster[numOpaque];
            int[] opaqueCols = new int[numOpaque];

            numTransparent = numOpaque = 0;
            for (Map.Entry<Layer, TransparentRaster> e : dd.layerRasters.entrySet()) {
                Layer layer = e.getKey();
                if (!lv.isVisible(layer)) {
                    continue;
                }
                TransparentRaster raster = e.getValue();
                int transparentNum = gp.getGraphics(layer).getTransparentLayer();
                if (transparentNum != 0) {
                    transparentMasks[numTransparent] = (1 << (transparentNum - 1)) & (colorMap.length - 1);
                    transparentRasters[numTransparent++] = raster;
                } else {
                    opaqueCols[numOpaque] = getTheColor(gp.getGraphics(layer), !lv.isHighlighted(layer));
                    opaqueRasters[numOpaque++] = raster;
                }
            }

            // determine range
            int numIntsPerRow = dd.numIntsPerRow;
            int backgroundColor = gp.getColor(User.ColorPrefType.BACKGROUND).getRGB() & GraphicsPreferences.RGB_MASK;
//            int lx = 0;
            int hx = dd.width - 1;
            int ly = 0;
            int hy = dd.height - 1;

            for (int y = ly; y <= hy; y++) {
                int baseByteIndex = y * numIntsPerRow;
//                int baseIndex = y * dd.width;
                for (int x = 0; x <= hx; x++) {
                    int entry = baseByteIndex + (x >> 5);
                    int maskBit = 1 << (x & 31);
                    int opaqueIndex = -1;
                    for (int i = 0; i < opaqueRasters.length; i++) {
                        if ((opaqueRasters[i].layerBitMap[entry] & maskBit) != 0) {
                            opaqueIndex = i;
                        }
                    }
                    int pixelValue;
                    if (opaqueIndex >= 0) {
                        pixelValue = opaqueCols[opaqueIndex];
                    } else {
                        int bits = 0;
                        for (int i = 0; i < transparentRasters.length; i++) {
                            if ((transparentRasters[i].layerBitMap[entry] & maskBit) != 0) {
                                bits |= transparentMasks[i];
                            }
                        }
                        pixelValue = bits != 0 ? colorMap[bits].getRGB() & 0xFFFFFF : backgroundColor;
                    }
                    smallOpaqueData[x] = pixelValue;
                }
                g.drawImage(smallImg, 0, y, null);
            }
        }

        int getTheColor(EGraphics desc, boolean dimmed) {
            if (nowPrinting == 2) {
                return 0;
            }
            int col = desc.getRGB();
            if (highlightingLayers) {
                if (dimmed) {
                    col = dimColor(col);
                } else {
                    col = brightenColor(col);
                }
            }
            return col;
        }
        private double[] hsvTempArray = new double[3];

        /**
         * Method to dim a color by reducing its saturation.
         * @param col the color as a 24-bit integer.
         * @return the dimmed color, a 24-bit integer.
         */
        private int dimColor(int col) {
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
        private int brightenColor(int col) {
            int r = col & 0xFF;
            int g = (col >> 8) & 0xFF;
            int b = (col >> 16) & 0xFF;
            fromRGBtoHSV(r, g, b, hsvTempArray);
            hsvTempArray[1] *= 1.5;
            if (hsvTempArray[1] > 1) {
                hsvTempArray[1] = 1;
            }
            col = fromHSVtoRGB(hsvTempArray[0], hsvTempArray[1], hsvTempArray[2]);
            return col;
        }

        /**
         * Method to convert a red/green/blue color to a hue/saturation/intensity color.
         * Why not use Color.RGBtoHSB?  It doesn't work as well.
         */
        private void fromRGBtoHSV(int ir, int ig, int ib, double[] hsi) {
            double r = ir / 255.0f;
            double g = ig / 255.0f;
            double b = ib / 255.0f;

            // "i" is maximum of "r", "g", and "b"
            hsi[2] = Math.max(Math.max(r, g), b);

            // "x" is minimum of "r", "g", and "b"
            double x = Math.min(Math.min(r, g), b);

            // "saturation" is (i-x)/i
            if (hsi[2] == 0.0) {
                hsi[1] = 0.0;
            } else {
                hsi[1] = (hsi[2] - x) / hsi[2];
            }

            // hue is quadrant-based
            hsi[0] = 0.0;
            if (hsi[1] != 0.0) {
                double rdot = (hsi[2] - r) / (hsi[2] - x);
                double gdot = (hsi[2] - g) / (hsi[2] - x);
                double bdot = (hsi[2] - b) / (hsi[2] - x);
                if (b == x && r == hsi[2]) {
                    hsi[0] = (1.0 - gdot) / 6.0;
                } else if (b == x && g == hsi[2]) {
                    hsi[0] = (1.0 + rdot) / 6.0;
                } else if (r == x && g == hsi[2]) {
                    hsi[0] = (3.0 - bdot) / 6.0;
                } else if (r == x && b == hsi[2]) {
                    hsi[0] = (3.0 + gdot) / 6.0;
                } else if (g == x && b == hsi[2]) {
                    hsi[0] = (5.0 - rdot) / 6.0;
                } else if (g == x && r == hsi[2]) {
                    hsi[0] = (5.0 + bdot) / 6.0;
                } else {
                    System.out.println("Cannot convert (" + ir + "," + ig + "," + ib + "), for x=" + x + " i=" + hsi[2] + " s=" + hsi[1]);
                }
            }
        }

        /**
         * Method to convert a hue/saturation/intensity color to a red/green/blue color.
         * Why not use Color.HSBtoRGB?  It doesn't work as well.
         */
        private int fromHSVtoRGB(double h, double s, double v) {
            h = h * 6.0;
            int i = (int) h;
            double f = h - i;
            double m = v * (1.0 - s);
            double n = v * (1.0 - s * f);
            double k = v * (1.0 - s * (1.0 - f));
            int r = 0, g = 0, b = 0;
            switch (i) {
                case 0:
                    r = (int) (v * 255.0);
                    g = (int) (k * 255.0);
                    b = (int) (m * 255.0);
                    break;
                case 1:
                    r = (int) (n * 255.0);
                    g = (int) (v * 255.0);
                    b = (int) (m * 255.0);
                    break;
                case 2:
                    r = (int) (m * 255.0);
                    g = (int) (v * 255.0);
                    b = (int) (k * 255.0);
                    break;
                case 3:
                    r = (int) (m * 255.0);
                    g = (int) (n * 255.0);
                    b = (int) (v * 255.0);
                    break;
                case 4:
                    r = (int) (k * 255.0);
                    g = (int) (m * 255.0);
                    b = (int) (v * 255.0);
                    break;
                case 5:
                    r = (int) (v * 255.0);
                    g = (int) (m * 255.0);
                    b = (int) (n * 255.0);
                    break;
            }
            if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
                System.out.println("(" + h + "," + s + "," + v + ") -> (" + r + "," + g + "," + b + ") (i=" + i + ")");
                if (r < 0) {
                    r = 0;
                }
                if (r > 255) {
                    r = 255;
                }
                if (g < 0) {
                    g = 0;
                }
                if (g > 255) {
                    g = 255;
                }
                if (b < 0) {
                    b = 0;
                }
                if (b > 255) {
                    b = 255;
                }
            }
            return (b << 16) | (g << 8) | r;
        }

        /**
         * Returns alpha blending order for this EditWindow.
         * Alpha blending order specifies pixel color by such a way:
         * Color col = backgroudColor;
         * for (LayerColor layerColor: blendingOrder) {
         *    if (This pixel covers a piece of layer layerColor.layer) {
         *       alpha = layerColor.color.getAlpha();
         *       col =  layerColor.color.getRGB()*alpha + col*(1 - alpha)
         *    }
         * }
         * return col;
         * @param layersAvailable layers available in this EditWindow
         * @return alpha blending order.
         */
        private List<AbstractDrawing.LayerColor> getBlendingOrder(Set<Layer> layersAvailable, GraphicsPreferences gp, LayerVisibility lv, boolean patternedDisplay, boolean alphaBlendingOvercolor) {
            List<AbstractDrawing.LayerColor> layerColors = new ArrayList<AbstractDrawing.LayerColor>();
            List<Layer> sortedLayers = new ArrayList<Layer>(layersAvailable);
            Collections.sort(sortedLayers, Technology.LAYERS_BY_HEIGHT_LIFT_CONTACTS);
            float[] backgroundComps = gp.getColor(User.ColorPrefType.BACKGROUND).getRGBColorComponents(null);
            float bRed = backgroundComps[0];
            float bGreen = backgroundComps[1];
            float bBlue = backgroundComps[2];
            for (Layer layer : sortedLayers) {
                if (!lv.isVisible(layer)) {
                    continue;
                }
                if (layer == Generic.tech().glyphLay && !patternedDisplay) {
                    continue;
                }
                Color color = gp.getGraphics(layer).getColor();
                float[] compArray = color.getRGBComponents(null);
                float red = compArray[0];
                float green = compArray[1];
                float blue = compArray[2];
                float opacity = lv.getOpacity(layer);
                if (opacity <= 0) {
                    continue;
                }
                float inverseAlpha = 1 - opacity;
                if (alphaBlendingOvercolor) {
                    red -= bRed * inverseAlpha;
                    green -= bGreen * inverseAlpha;
                    blue -= bBlue * inverseAlpha;
                } else {
                    red *= opacity;
                    green *= opacity;
                    blue *= opacity;
                }
                layerColors.add(new AbstractDrawing.LayerColor(layer, red, green, blue, inverseAlpha));
            }
//            final LayerTab layerTab = getWindowFrame().getLayersTab();
//            final boolean showOpacity = !User.isLegacyComposite();
//            if (layerTab != null)
//                SwingUtilities.invokeLater(new Runnable() { public void run() { layerTab.setDisplayAlgorithm(showOpacity); }});
            return layerColors;
        }

        /**
         * This method is called from Job thread.
         */
        @Override
        public void render(Dimension sz, WindowFrame.DisplayAttributes da, GraphicsPreferences gp,
                DrawingPreferences dp, boolean fullInstantiate, Rectangle2D bounds) {
            LayerDrawing offscreen = null;
            if (drawingData != null && drawingData.offscreen.getSize().equals(sz)) {
                offscreen = drawingData.offscreen;
            }
            if (offscreen == null) {
                offscreen = new LayerDrawing(sz);
            }
            this.da = da;
            offscreen.gp = gp;
            offscreen.dp = dp;
            offscreen.textColor = gp.getColor(User.ColorPrefType.TEXT);
            offscreen.portColorsCache = new HashMap<PrimitivePort, Color>();
//            updateScaleAndOffset();

            offscreen.drawImage(this, fullInstantiate, bounds, patternedScaleLimit, alphaBlendingOvercolorLimit);
            needComposite = true;
            drawingData = new DrawingData(offscreen);
        }
        private static boolean joglChecked = false;
        private static Class<?> layerDrawerClass;
        private static Method joglShowLayerMethod;

        /**
         * Method to tell whether JOGL redisplay is available.
         * JOGL is Java extension.
         * This method dynamically figures out whether the JOGL module is present by using reflection.
         * @return true if the JOGL redisplay is available.
         */
        public static boolean hasJogl() {
            if (!joglChecked) {
                joglChecked = true;

                // find the LayerDrawer class
                try {
                    layerDrawerClass = Class.forName("com.sun.electric.plugins.jogl.LayerDrawer");
                    joglShowLayerMethod = layerDrawerClass.getMethod("showLayer", new Class[]{Dimension.class, (new int[0]).getClass(), Double.TYPE, Double.TYPE, Double.TYPE});
                } catch (Exception e) {
                }
            }
            return joglShowLayerMethod != null;
        }

        @Override
        public void testJogl() {
            if (hasJogl()) {
                try {
                    int numBoxes = 1000000;
                    int[] boxes = new int[numBoxes * 4];
                    for (int i = 0; i < numBoxes; i++) {
                        int x = (i * 5) % 501 - 100;
                        int y = (i * 7) % 500 - 200;
                        boxes[i * 4 + 0] = x;
                        boxes[i * 4 + 1] = y;
                        boxes[i * 4 + 2] = x + 10;
                        boxes[i * 4 + 3] = y + 10;
                    }
//                    joglShowLayerMethod.invoke(layerDrawerClass, new Object[] {offscreen___.sz, boxes, 1.0, 0.0, 0.0});
                } catch (Exception e) {
                    System.out.println("Unable to run the LayerDrawer input module (" + e.getClass() + ")");
                    e.printStackTrace(System.out);
                }
                return;
            }
//            testJogl_();
        }
//        private void testJogl_() {
//            JFrame frame = new JFrame("Jogl");
//            GLCapabilities capabilities = new GLCapabilities();
//            capabilities.setDoubleBuffered(false);
//            capabilities.setHardwareAccelerated(false);
//            System.out.println("Capabilities: " + capabilities);
//            GLCanvas canvas = new GLCanvas(capabilities);
//
//            canvas.addGLEventListener(new JoglEventListener());
//            frame.add(canvas);
//            frame.setSize(offscreen.getSize());
//
//            frame.setVisible(true);
//        }
//
//        private static void showInt(GL gl, String s, int i) {
//            IntBuffer intBuffer = IntBuffer.allocate(100);
//            gl.glGetIntegerv(GL.GL_MULTISAMPLE, intBuffer);
//            System.out.println(s + ": " + intBuffer.get(0));
//        }
//
//        private class JoglEventListener implements GLEventListener {
//
//            public void init(GLAutoDrawable drawable) {
//                GL gl = drawable.getGL();
//                gl = new DebugGL(gl);
//                drawable.setGL(gl);
//
////                gl.glDisable(GL.GL_MULTISAMPLE);
//
//                System.out.println("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
//                System.out.println("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
//                System.out.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
//                showInt(gl, "GL_MULTISAMPLE", GL.GL_MULTISAMPLE);
//                showInt(gl, "GL_SAMPLE_BUFFERS", GL.GL_SAMPLE_BUFFERS);
//                showInt(gl, "GL_SAMPLES", GL.GL_SAMPLES);
//
////                gl.glPixelStorei(GL.GL_UNPACK_SWAP_BYTES, 1);
////                gl.glPixelStorei(gl.GL_UNPACK_LSB_FIRST, 1);
////                gl.glPixelStorei(gl.GL_UNPACK_ALIGNMENT, 1);
//
////                gl.glPixelTransferi(gl.GL_MAP_COLOR, 1);
//                  gl.glEnable(gl.GL_BLEND);
//                  gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
//            }
//
//            public void display(GLAutoDrawable drawable) {
//                List<EditWindow.LayerColor> blendingOrder = offscreen.wnd.getBlendingOrder(offscreen.layerRasters.keySet(), false);
//
//                GL gl = drawable.getGL();
//
//                BufferedImage bImg = vImg.getSnapshot();
//                DataBufferInt dbi = (DataBufferInt)bImg.getRaster().getDataBuffer();
//                int[] opaqueData = dbi.getData();
//                int w = bImg.getWidth(), h = bImg.getHeight();
//                IntBuffer intBuffer = IntBuffer.allocate(w*h);
//                ByteBuffer redBuffer = ByteBuffer.allocate(w*h);
//                ByteBuffer greenBuffer = ByteBuffer.allocate(w*h);
//                ByteBuffer blueBuffer = ByteBuffer.allocate(w*h);
//                assert intBuffer.order() == ByteOrder.nativeOrder();
//                for (int y = 0; y < h; y++) {
//                    for (int x = 0; x < w; x++) {
//                        int v = opaqueData[(h - y - 1)*w + x];
//                        intBuffer.put(v | 0xFF000000);
//                        redBuffer.put((byte)(v >> 16));
//                        greenBuffer.put((byte)(v >> 8));
//                        blueBuffer.put((byte)v);
//                    }
//                }
//                intBuffer.rewind();
//                redBuffer.rewind();
//                greenBuffer.rewind();
//                blueBuffer.rewind();
////                IntBuffer intBuffer = IntBuffer.wrap(offscreen.opaqueData, 0, w*h);
//
//                long startTime = System.currentTimeMillis();
////                float[] bg = (new Color(User.getColor(User.ColorPrefType.BACKGROUND))).getRGBComponents(null);
////                gl.glClearColor(bg[0], bg[1], bg[2], 1.0f);
//                gl.glClearColor(1f, 1f, 1f, 1f);
//                gl.glClear(GL.GL_COLOR_BUFFER_BIT);
//
//                gl.glMatrixMode(GL.GL_PROJECTION);
//                gl.glLoadIdentity();
////                gl.glFrustum(-1.0f, 1.0f, -h, h, 5.0f, 60.0f);
//
//                gl.glMatrixMode(GL.GL_MODELVIEW);
//                gl.glLoadIdentity();
//
////                ByteBuffer buf = ByteBuffer.allocate((w + 7)/8*h);
////                for (int i = 0; i < 100; i++)
////                    buf.put(i*3, (byte)0x55);
////                gl.glPixelMapfv(gl.GL_PIXEL_MAP_I_TO_R, 2, new float[256], 0);
////                float[] green = new float[256];
////                Arrays.fill(green, 1.0f);
////                green[0] = 0;
////                gl.glPixelMapfv(gl.GL_PIXEL_MAP_I_TO_G, 2, green, 0);
////                gl.glPixelMapfv(gl.GL_PIXEL_MAP_I_TO_B, 2, new float[256], 0);
////                gl.glPixelMapfv(gl.GL_PIXEL_MAP_I_TO_A, 2, new float[256], 0);
////                gl.glDrawPixels(10, 10, gl.GL_COLOR_INDEX, gl.GL_BITMAP, buf);
////                gl.glBitmap(10, 10, 0f, 0f, 0f, 0f, buf);
//
////                byte[] bytes1 = new byte[(w+7)/8*h*2];
////                Arrays.fill(bytes1, (byte)-2);
////                bytes1[0] = 0;
////                bytes1[299] = 0;
//                if (true) {
////                    gl.glPixelMapfv(gl.GL_PIXEL_MAP_I_TO_R, 256, new float[256], 0);
////                    float[] green = new float[256];
////                    for (int i = 0; i < green.length; i++)
////                        green[i] = i/255f;
////                    gl.glPixelMapfv(gl.GL_PIXEL_MAP_I_TO_G, 256, green, 0);
////                    gl.glPixelMapfv(gl.GL_PIXEL_MAP_I_TO_B, 256, new float[256], 0);
////                    float[] alpha = new float[256];
////                    Arrays.fill(alpha, 1f);
////                    gl.glPixelMapfv(gl.GL_PIXEL_MAP_I_TO_A, 256, alpha, 0);
//
//                      for (int i = 0; i < 1000; i++)
//                        gl.glDrawPixels(w, h, GL.GL_BGRA, GL.GL_UNSIGNED_INT_8_8_8_8_REV, intBuffer);
////                    gl.glDrawPixels(w, h, GL.GL_GREEN, GL.GL_UNSIGNED_BYTE, greenBuffer);
////                    gl.glDrawPixels(w, h, GL.GL_BLUE, GL.GL_UNSIGNED_BYTE, blueBuffer);
////                    gl.glDrawPixels(w, h, GL.GL_RED, GL.GL_UNSIGNED_BYTE, redBuffer);
////                    ByteBuffer byteBuffer = ByteBuffer.allocate(w*h);
////                    gl.glReadPixels(0, 0, w, h, GL.GL_BLUE, GL.GL_UNSIGNED_BYTE, byteBuffer);
////                    for (int y = 0; y < h; y++) {
////                        for (int x = 0; x < w; x++) {
////                            int v = opaqueData[(h - y - 1)*w + x];
////                            byte b = byteBuffer.get();
////                            if (((v >> 16) & 0xFF) != (b & 0xFF))
////                                System.out.println("Mismatch at x=" + x + " y=" + y + " v=" + Integer.toHexString(v) + " b=" + Integer.toHexString(b));
////                        }
////                    }
//                } else {
//                    for (EditWindow.LayerColor layerColor: blendingOrder) {
//                        float[] c = layerColor.color.getComponents(null);
////                    gl.glColor4f(c[0], c[1], c[2], c[3]);
//                        gl.glPixelMapfv(gl.GL_PIXEL_MAP_I_TO_R, 2, new float[] {0f, c[0]}, 0);
//                        gl.glPixelMapfv(gl.GL_PIXEL_MAP_I_TO_G, 2, new float[] {0f, c[1]}, 0);
//                        gl.glPixelMapfv(gl.GL_PIXEL_MAP_I_TO_B, 2, new float[] {0f, c[2]}, 0);
//                        gl.glPixelMapfv(gl.GL_PIXEL_MAP_I_TO_A, 2, new float[] {0f, c[3]}, 0);
////                    byte[] b = offscreen.layerRasters.get(layerColor.layer).layerBitMap;
////                    buf.rewind();
////                    buf.put(b);
////                    buf.rewind();
////                    ByteBuffer buf = offscreen.layerBuffers.get(layerColor.layer);
////                    gl.glDrawPixels(w, h, gl.GL_COLOR_INDEX, gl.GL_BITMAP, buf);
////                    gl.glBitmap(w, h, 0f, 0f, 0f, 0f, buf);
//                    }
//                }
//                long endTime = System.currentTimeMillis();
//                System.out.println("jogl display took " + (endTime - startTime) + " msec");
//
//            }
//
//            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
//            public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}
//        }
    }

    static void drawTechPalette(Graphics2D g, GraphicsPreferences gp, int imgX, int imgY, Rectangle entrySize, double scale, VectorCache.VectorBase[] shapes) {

        BufferedImage smallImg = new BufferedImage(entrySize.width, Drawing.SMALL_IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        DataBufferInt smallDbi = (DataBufferInt) smallImg.getRaster().getDataBuffer();
        int[] smallOpaqueData = smallDbi.getData();

        LayerDrawing offscreen = new LayerDrawing(new Dimension(entrySize.width, entrySize.height));
        offscreen.gp = gp;
        // set colors to use
        offscreen.textColor = gp.getColor(User.ColorPrefType.TEXT);

        // initialize the cache of expanded cell displays
//        varContext = wnd.getVarContext();
        offscreen.initOrigin(scale, EPoint.ORIGIN);
        offscreen.renderedWindow = true;
        offscreen.patternedDisplay = true;
        offscreen.canDrawText = true;
        offscreen.canDrawRelativeText = 0;

        // remember the true window size (since recursive calls may cache individual cells that are smaller)
//		topSz = sz;

        // initialize rendering into the offscreen image
        offscreen.clipLX = 0;
        offscreen.clipHX = offscreen.sz.width - 1;
        offscreen.clipLY = 0;
        offscreen.clipHY = offscreen.sz.height - 1;
        offscreen.clearImage(null);

        // now render it all
        offscreen.renderTextList.clear();
        offscreen.greekTextList.clear();
        offscreen.crossTextList.clear();
        ArrayList<VectorCache.VectorBase> shapeList = new ArrayList<VectorCache.VectorBase>();
        for (VectorCache.VectorBase shape : shapes) {
            shapeList.add(shape);
        }
        offscreen.drawList(0, 0, shapeList);


        AlphaBlender alphaBlender = new AlphaBlender();
        Map<Layer, int[]> layerBits = new HashMap<Layer, int[]>();
        for (Map.Entry<Layer, TransparentRaster> e : offscreen.layerRasters.entrySet()) {
            layerBits.put(e.getKey(), e.getValue().layerBitMap);
        }
        List<AbstractDrawing.LayerColor> blendingOrder = getBlendingOrderForTechPalette(gp, layerBits.keySet());
        if (TAKE_STATS) {
            System.out.print("BlendingOrder:");
            for (AbstractDrawing.LayerColor lc : blendingOrder) {
                int alpha = (int) ((1 - lc.inverseAlpha) * 100 + 0.5);
                System.out.print(" " + lc.layer.getName() + ":" + alpha);
            }
            System.out.println();
        }

        ArrayList<AbstractDrawing.LayerColor> colors = new ArrayList<AbstractDrawing.LayerColor>();
        ArrayList<int[]> bits = new ArrayList<int[]>();
        for (AbstractDrawing.LayerColor layerColor : blendingOrder) {
            int[] b = layerBits.get(layerColor.layer);
            if (b == null) {
                continue;
            }
            colors.add(layerColor);
            bits.add(b);
        }
        alphaBlender.init(gp.getColor(User.ColorPrefType.BACKGROUND), colors, bits);

        int width = offscreen.sz.width;
        int height = offscreen.sz.height;
        int numIntsPerRow = offscreen.numIntsPerRow;
        int baseByteIndex = 0;
        int y = 0;
        while (y < height) {
            int h = Math.min(Drawing.SMALL_IMG_HEIGHT, height - y);
            int baseIndex = 0;
            for (int k = 0; k < h; k++) {
                alphaBlender.composeLine(baseByteIndex, 0, width - 1, smallOpaqueData, baseIndex);
                baseByteIndex += numIntsPerRow;
                baseIndex += width;
            }
            g.drawImage(smallImg, imgX, imgY + y, null);
            y += h;
        }

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        for (RenderTextInfo textInfo : offscreen.renderTextList) {
            textInfo.offX += imgX;
            textInfo.offY += imgY;
            textInfo.draw(g, null);
        }
//        needComposite = true;
//        layerRasters = new HashMap<Layer,TransparentRaster>(offscreen.layerRasters);
//        greekText = offscreen.greekTextList.toArray(new GreekTextInfo[offscreen.greekTextList.size()]);
//        crossText = offscreen.crossTextList.toArray(new CrossTextInfo[offscreen.crossTextList.size()]);
//        renderText = offscreen.renderTextList.toArray(new RenderTextInfo[offscreen.renderTextList.size()]);
    }

    private static List<AbstractDrawing.LayerColor> getBlendingOrderForTechPalette(GraphicsPreferences gp, Set<Layer> layersAvailable) {
        boolean alphaBlendingOvercolor = true;
        ArrayList<AbstractDrawing.LayerColor> layerColors = new ArrayList<AbstractDrawing.LayerColor>();
        ArrayList<Layer> sortedLayers = new ArrayList<Layer>(layersAvailable);
        Collections.sort(sortedLayers, Technology.LAYERS_BY_HEIGHT_LIFT_CONTACTS);
        float[] backgroundComps = gp.getColor(User.ColorPrefType.BACKGROUND).getRGBColorComponents(null);
        float bRed = backgroundComps[0];
        float bGreen = backgroundComps[1];
        float bBlue = backgroundComps[2];
        for (Layer layer : sortedLayers) {
            Color color = gp.getGraphics(layer).getColor();
            float[] compArray = color.getRGBComponents(null);
            float red = compArray[0];
            float green = compArray[1];
            float blue = compArray[2];
            float opacity = 0.7f;
            float inverseAlpha = 1 - opacity;
            if (alphaBlendingOvercolor) {
                red -= bRed * inverseAlpha;
                green -= bGreen * inverseAlpha;
                blue -= bBlue * inverseAlpha;
            } else {
                red *= opacity;
                green *= opacity;
                blue *= opacity;
            }
            layerColors.add(new AbstractDrawing.LayerColor(layer, red, green, blue, inverseAlpha));
        }
        return layerColors;
    }

    // ************************************* TOP LEVEL *************************************
    /**
     * Constructor creates an offscreen PixelDrawing object.
     * @param sz the size of an offscreen PixelDrawinf object.
     */
    public LayerDrawing(Dimension sz) {
        this.sz = new Dimension(sz);
//        width = sz.width;
        clipLX = 0;
        clipHX = sz.width - 1;
        clipLY = 0;
        clipHY = sz.height - 1;

        // allocate pointer to the opaque image
        total = sz.height * sz.width;
        numIntsPerRow = (sz.width + Integer.SIZE - 1) / Integer.SIZE;
        renderedWindow = true;
    }

    public LayerDrawing(double scale, int lX, int hX, int lY, int hY) {
        this.scale = scale;
        scale_ = (float) (scale / DBMath.GRID);

        this.originX = -lX;
        this.originY = hY;
        factorX = (float) (-originX / scale_);
        factorY = (float) (originY / scale_);
        this.sz = new Dimension(hX - lX + 1, hY - lY + 1);
//        width = sz.width;
        clipLX = 0;
        clipHX = sz.width - 1;
        clipLY = 0;
        clipHY = sz.height - 1;

        // allocate pointer to the opaque image
        total = sz.height * sz.width;
        numIntsPerRow = (sz.width + Integer.SIZE - 1) / Integer.SIZE;
    }

    void initOrigin(double scale, Point2D offset) {
        this.scale = scale;
        scale_ = (float) (scale / DBMath.GRID);
        this.originX = sz.width / 2 - offset.getX() * scale;
        this.originY = sz.height / 2 + offset.getY() * scale;
        factorX = (float) (offset.getX() * DBMath.GRID - sz.width / 2 / scale_);
        factorY = (float) (offset.getY() * DBMath.GRID + sz.height / 2 / scale_);
    }

    /**
     * Method to set the printing mode used for all drawing.
     * @param mode the printing mode:  0=color display (default), 1=color printing, 2=B&W printing.
     */
//    public void setPrintingMode(int mode) { nowPrinting = mode; }
    /**
     * Method for obtaining the size of the offscreen bitmap.
     * @return the size of the offscreen bitmap.
     */
    public Dimension getSize() {
        return sz;
    }

    /**
     * Method to clear the cache of expanded subcells.
     * This is used by layer visibility which, when changed, causes everything to be redrawn.
     */
    public static void clearSubCellCache() {
        expandedCells = new HashMap<ExpandedCellKey, ExpandedCellInfo>();
    }

    /**
     * This is the entry point for rendering.
     * It displays a cell in this offscreen window.
     * @param fullInstantiate true to display to the bottom of the hierarchy (for peeking).
     * @param drawLimitBounds the area in the cell to display (null to show all).
     * The rendered Image can then be obtained with "getImage()".
     */
    private void drawImage(Drawing drawing, boolean fullInstantiate, Rectangle2D drawLimitBounds,
            double patternedScaleLimit, double alphaBlendingOvercolorLimit) {
        long startTime = 0, clearTime = 0, countTime = 0;
        long initialUsed = 0;
        if (TAKE_STATS) {
//			Runtime.getRuntime().gc();
            startTime = System.currentTimeMillis();
            initialUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            tinyCells = tinyPrims = totalCells = renderedCells = totalPrims = tinyArcs = linedArcs = totalArcs = 0;
            offscreensCreated = offscreenPixelsCreated = offscreensUsed = offscreenPixelsUsed = cellsRendered = 0;
            offscreensUsedSet.clear();
            boxArrayCount = boxCount = boxDisplayCount = lineCount = polygonCount = crossCount = circleCount = discCount = arcCount = 0;
        }

        if (fullInstantiate != lastFullInstantiate) {
            clearSubCellCache();
            lastFullInstantiate = fullInstantiate;
        }

        EditWindow wnd = drawing.wnd;
        Cell cell = wnd.getInPlaceEditTopCell();
        List<NodeInst> inPlaceNodePath = wnd.getInPlaceEditNodePath();
        if (inPlaceNodePath.isEmpty()) {
            inPlaceSubcellPath = null;
        } else {
            inPlaceSubcellPath = new BitSet();
            for (NodeInst ni : inPlaceNodePath) {
                Cell subCell = (Cell) ni.getProto();
                inPlaceSubcellPath.set(subCell.getId().cellIndex);
            }
        }
        inPlaceCurrent = wnd.getCell();

        // initialize the cache of expanded cell displays
        if (expandedScale != drawing.da.scale) {
            clearSubCellCache();
            expandedScale = drawing.da.scale;
        }
        varContext = wnd.getVarContext();
        globalTextScale = wnd.getGlobalTextScale();
        defaultFont = wnd.getDefaultFont();
        initOrigin(expandedScale, new Point2D.Double(drawing.da.offX, drawing.da.offY));
        patternedDisplay = expandedScale > patternedScaleLimit;
        alphaBlendingOvercolor = expandedScale > alphaBlendingOvercolorLimit;
        canDrawText = expandedScale > 1;
        canDrawRelativeText = canDrawText ? 0 : MINIMUMTEXTSIZE;
        maxObjectSize = 2 / expandedScale;

        // remember the true window size (since recursive calls may cache individual cells that are smaller)
        topSz = sz;

        // initialize rendering into the offscreen image
        Rectangle renderBounds = null;
        if (drawLimitBounds != null) {
            renderBounds = databaseToScreen(drawLimitBounds);
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

        Set<CellId> changedCellsCopy;
        synchronized (changedCells) {
            changedCellsCopy = new HashSet<CellId>(changedCells);
            changedCells.clear();
        }
        forceRedraw(changedCellsCopy);
        VectorCache.theCache.forceRedraw();
        // reset cached cell counts
        numberToReconcile = SINGLETONSTOADD;
        for (ExpandedCellInfo count : expandedCells.values()) {
            count.instanceCount = 0;
        }
        if (TAKE_STATS) {
            clearTime = System.currentTimeMillis();
        }

        // determine which cells should be cached (must have at least 2 instances)
        countCell(cell, drawLimitBounds, fullInstantiate, Orientation.IDENT, DBMath.MATID);
        if (TAKE_STATS) {
            countTime = System.currentTimeMillis();
        }

        // now render it all
        renderTextList.clear();
        greekTextList.clear();
        crossTextList.clear();
        drawCell(cell, drawLimitBounds, fullInstantiate, Orientation.IDENT, 0, 0, true, wnd.getVarContext());
        // if a grid is requested, overlay it
        if (cell != null && wnd.isGrid()) {
            drawGrid(wnd, drawing.da);
        }

        if (TAKE_STATS) {
            long endTime = System.currentTimeMillis();
            long curUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long memConsumed = curUsed - initialUsed;
            System.out.println("Took " + TextUtils.getElapsedTime(endTime - startTime)
                    + "(" + (clearTime - startTime) + "+" + (countTime - clearTime) + "+" + (endTime - countTime) + ")"
                    + ", rendered " + cellsRendered + " cells, used " + offscreensUsed + " (" + offscreenPixelsUsed + " pixels) " + offscreensUsedSet.size() + "cached cells, created "
                    + offscreensCreated + " (" + offscreenPixelsCreated + " pixels) new cell caches (my size is " + total + " pixels), memory used=" + memConsumed);
            System.out.println("   Cells (" + totalCells + ") " + tinyCells + " are tiny;"
                    + " Primitives (" + totalPrims + ") " + tinyPrims + " are tiny;"
                    + " Arcs (" + totalArcs + ") " + tinyArcs + " are tiny, " + linedArcs + " are lines"
                    + " Texts " + renderTextList.size() + " Greeks " + greekTextList.size());
            if (true) {
                System.out.print("    " + (boxCount + polygonCount + discCount + lineCount + crossCount + circleCount + arcCount) + " rendered: ");
                if (boxArrayCount != 0) {
                    System.out.print(boxCount + "(" + boxArrayCount + "," + boxDisplayCount + ") boxes ");
                }
                if (polygonCount != 0) {
                    System.out.print(polygonCount + " polygons ");
                }
                if (discCount != 0) {
                    System.out.print(discCount + " discs ");
                }
                if (lineCount != 0) {
                    System.out.print(lineCount + " lines ");
                }
                if (crossCount != 0) {
                    System.out.print(crossCount + " crosses ");
                }
                if (circleCount != 0) {
                    System.out.print(circleCount + " circles ");
                }
                if (arcCount != 0) {
                    System.out.print(arcCount + " circleArcs ");
                }
                System.out.println();
            }
        }
    }

    // ************************************* INTERMEDIATE CONTROL LEVEL *************************************
    /**
     * Method to erase the offscreen data in this PixelDrawing.
     * This is called before any rendering is done.
     * @param bounds the area of the image to actually draw (null to draw all).
     */
    public void clearImage(Rectangle bounds) {
        // erase the patterned opaque layer bitmaps
        if (bounds == null) {
            for (TransparentRaster raster : layerRasters.values()) {
                raster.eraseAll();
            }
            if (instanceRaster != null) {
                instanceRaster.eraseAll();
            }
            if (gridRaster != null) {
                gridRaster.eraseAll();
            }
        } else {
            for (TransparentRaster raster : layerRasters.values()) {
                raster.eraseBox(clipLX, clipHX, clipLY, clipHY);
            }
            if (instanceRaster != null) {
                instanceRaster.eraseBox(clipLX, clipHX, clipLY, clipHY);
            }
            if (gridRaster != null) {
                gridRaster.eraseBox(clipLX, clipHX, clipLY, clipHY);
            }
        }
    }

    /**
     * Method to draw the grid into the offscreen buffer
     */
    private void drawGrid(EditWindow wnd, WindowFrame.DisplayAttributes da) {
        double spacingX = wnd.getGridXSpacing();
        double spacingY = wnd.getGridYSpacing();
        if (spacingX == 0 || spacingY == 0) {
            return;
        }
        double boldSpacingX = spacingX * dp.gridXBoldFrequency;
        double boldSpacingY = spacingY * dp.gridYBoldFrequency;
        double boldSpacingThreshX = spacingX / 4;
        double boldSpacingThreshY = spacingY / 4;

        // screen extent

        Rectangle2D displayable = displayableBounds(da.getIntoCellTransform());
        double lX = displayable.getMinX();
        double lY = displayable.getMaxY();
        double hX = displayable.getMaxX();
        double hY = displayable.getMinY();
        double scaleX = sz.width / (hX - lX);
        double scaleY = sz.height / (lY - hY);

        // initial grid location
        double x1 = DBMath.toNearest(lX, spacingX);
        double y1 = DBMath.toNearest(lY, spacingY);

        // adjust grid placement according to scale
        boolean allBoldDots = false;
        if (spacingX * scaleX < 5 || spacingY * scaleY < 5) {
            // normal grid is too fine: only show the "bold dots"
            x1 = DBMath.toNearest(x1, boldSpacingX);
            spacingX = boldSpacingX;
            y1 = DBMath.toNearest(y1, boldSpacingY);
            spacingY = boldSpacingY;

            // if even the bold dots are too close, don't draw a grid
            if (spacingX * scaleX < 10 || spacingY * scaleY < 10) {
                wnd.printGridWarning();
                return;
            }
        } else if (spacingX * scaleX > 75 && spacingY * scaleY > 75) {
            // if zoomed-out far enough, show all bold dots
            allBoldDots = true;
        }

        // draw the grid
        Point2D.Double tmpPt = new Point2D.Double();
        AffineTransform outofCellTransform = da.getOutofCellTransform();
        ERaster raster = getGridRaster();
        for (double i = y1; i > hY; i -= spacingY) {
            double boldValueY = i;
            if (i < 0) {
                boldValueY -= boldSpacingThreshY / 2;
            } else {
                boldValueY += boldSpacingThreshY / 2;
            }
            boolean everyTenY = Math.abs(boldValueY) % boldSpacingY < boldSpacingThreshY;
            for (double j = x1; j < hX; j += spacingX) {
                tmpPt.setLocation(j, i);
                outofCellTransform.transform(tmpPt, tmpPt);
                databaseToScreen(tmpPt.getX(), tmpPt.getY(), tempPt1);
                int x = tempPt1.x;
                int y = tempPt1.y;
                if (x < 0 || x >= sz.width) {
                    continue;
                }
                if (y < 0 || y >= sz.height) {
                    continue;
                }

                double boldValueX = j;
                if (j < 0) {
                    boldValueX -= boldSpacingThreshX / 2;
                } else {
                    boldValueX += boldSpacingThreshX / 2;
                }
                boolean everyTenX = Math.abs(boldValueX) % boldSpacingX < boldSpacingThreshX;
                if (allBoldDots && everyTenX && everyTenY) {
                    int boxLX = x - 2;
                    if (boxLX < 0) {
                        boxLX = 0;
                    }
                    int boxHX = x + 2;
                    if (boxHX >= sz.width) {
                        boxHX = sz.width - 1;
                    }
                    int boxLY = y - 2;
                    if (boxLY < 0) {
                        boxLY = 0;
                    }
                    int boxHY = y + 2;
                    if (boxHY >= sz.height) {
                        boxHY = sz.height - 1;
                    }

                    // draw box  in opaque area
                    raster.fillBox(boxLX, boxHX, boxLY, boxHY);
//                    for(int yg=boxLY; yg<=boxHY; yg++) {
//                        int baseIndex = yg * sz.width;
//                        for(int xg=boxLX; xg<=boxHX; xg++)
//                            opaqueData[baseIndex + xg] = col;
//                        baseIndex += sz.width;
//                    }

                    if (x > 1) {
                        raster.fillPoint(x - 2, y);
                    }
                    if (x < sz.width - 2) {
                        raster.fillPoint(x + 2, y);
                    }
                    if (y > 1) {
                        raster.fillPoint(x, y - 2);
                    }
                    if (y < sz.height - 2) {
                        raster.fillPoint(x, y + 2);
                    }
//					if (x > 1) opaqueData[y * sz.width + (x-2)] = col;
//					if (x < sz.width-2) opaqueData[y * sz.width + (x+2)] = col;
//					if (y > 1) opaqueData[(y-2) * sz.width + x] = col;
//					if (y < sz.height-2) opaqueData[(y+2) * sz.width + x] = col;
                    continue;
                }

                // special case every 10 grid points in each direction
                if (allBoldDots || (everyTenX && everyTenY)) {
                    raster.fillPoint(x, y);
                    if (x > 0) {
                        raster.fillPoint(x - 1, y);
                    }
                    if (x < sz.width - 1) {
                        raster.fillPoint(x + 1, y);
                    }
                    if (y > 0) {
                        raster.fillPoint(x, y - 1);
                    }
                    if (y < sz.height - 1) {
                        raster.fillPoint(x, y + 1);
                    }
//					opaqueData[y * sz.width + x] = col;
//					if (x > 0) opaqueData[y * sz.width + (x-1)] = col;
//					if (x < sz.width-1) opaqueData[y * sz.width + (x+1)] = col;
//					if (y > 0) opaqueData[(y-1) * sz.width + x] = col;
//					if (y < sz.height-1) opaqueData[(y+1) * sz.width + x] = col;
                    continue;
                }

                // just a single dot
                raster.fillPoint(x, y);
//				opaqueData[y * sz.width + x] = col;
            }
        }
        if (dp.gridAxesShown) {
            tmpPt.setLocation(0, 0);
            outofCellTransform.transform(tmpPt, tmpPt);
            databaseToScreen(tmpPt.getX(), tmpPt.getY(), tempPt1);
            int x = tempPt1.x;
            int y = tempPt1.y;
            if (x >= 0 && x < sz.width) {
                raster.fillVerLine(x, 0, sz.height - 1);
//                int baseIndex = xy.x;
//                for (int y = 0; y < sz.height; y++) {
//                    opaqueData[baseIndex] = col;
//                    baseIndex += sz.width;
//                }
            }
            if (y >= 0 && y < sz.height) {
                raster.fillHorLine(y, 0, sz.width - 1);
//                int baseIndex = xy.y * sz.width;
//                for (int x = 0; x < sz.width; x++)
//                    opaqueData[baseIndex + x] = col;
            }
        }
    }

    /**
     * Method to return a rectangle in database coordinates that covers the viewable extent of this window.
     * @return a rectangle that describes the viewable extent of this window (database coordinates).
     */
    private Rectangle2D displayableBounds(AffineTransform intoCellTransform) {
        Point2D low = new Point2D.Double();
        screenToDatabase(0, 0, low);
        intoCellTransform.transform(low, low);
        Point2D high = new Point2D.Double();
        screenToDatabase(sz.width - 1, sz.height - 1, high);
        intoCellTransform.transform(high, high);

        double lowX = Math.min(low.getX(), high.getX());
        double lowY = Math.min(low.getY(), high.getY());
        double sizeX = Math.abs(high.getX() - low.getX());
        double sizeY = Math.abs(high.getY() - low.getY());
        Rectangle2D bounds = new Rectangle2D.Double(lowX, lowY, sizeX, sizeY);
        return bounds;
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
    private void drawCell(Cell cell, Rectangle2D drawLimitBounds, boolean fullInstantiate, Orientation orient, int oX, int oY, boolean topLevel, VarContext context) {
        renderedCells++;

        VectorCache.VectorCell vc = VectorCache.theCache.drawCell(cell.getId(), orient, context, scale);

        // draw all subcells
        for (VectorCache.VectorSubCell vsc : vc.subCells) {
            totalCells++;

            // get instance location
            int soX = vsc.offsetX + oX;
            int soY = vsc.offsetY + oY;
            VectorCache.VectorCell subVC = VectorCache.theCache.findVectorCell(vsc.subCellId, vc.orient.concatenate(vsc.n.orient));
            gridToScreen(subVC.lX + soX, subVC.hY + soY, tempPt1);
            gridToScreen(subVC.hX + soX, subVC.lY + soY, tempPt2);
            int lX = tempPt1.x;
            int lY = tempPt1.y;
            int hX = tempPt2.x;
            int hY = tempPt2.y;

            // see if the subcell is clipped
            if (hX < clipLX || lX > clipHX) {
                continue;
            }
            if (hY < clipLY || lY > clipHY) {
                continue;
            }

            // see if drawing "down in place"
            boolean onPathDown = inPlaceSubcellPath != null && inPlaceSubcellPath.get(vsc.subCellId.cellIndex);

            // see if cell contents should be drawn
            boolean isExpanded = cell.isExpanded(vsc.n.nodeId);
            boolean expanded = isExpanded || fullInstantiate;

            // if not expanded, but viewing this cell in-place, expand it
            if (!expanded && onPathDown) {
                expanded = true;
            }

            // two ways to draw a cell instance
            CellId subCellId = vsc.subCellId;
            Cell subCell = VectorCache.theCache.database.getCell(subCellId);
            if (expanded) {
                // show the contents of the cell
                Orientation subOrient = orient.concatenate(vsc.n.orient);
                int soX_ = vsc.offsetX + oX;
                int soY_ = vsc.offsetY + oY;
                if (!expandedCellCached(subCell, subOrient, soX_, soY_, context, fullInstantiate)) {
                    // just draw it directly
                    cellsRendered++;
//                    NodeInst ni = subCell.getNodeById(vsc.n.nodeId);
                    drawCell(subCell, drawLimitBounds, fullInstantiate, subOrient, soX_, soY_, false, context.push(cell, vsc.n));
                }
            } else {
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
                gridToScreen(p1x, p1y, tempPt1);
                gridToScreen(p2x, p2y, tempPt2);
                ERaster instanceRaster = getInstanceRaster();
                drawLine(tempPt1, tempPt2, 0, instanceRaster);
                gridToScreen(p2x, p2y, tempPt1);
                gridToScreen(p3x, p3y, tempPt2);
                drawLine(tempPt1, tempPt2, 0, instanceRaster);
                gridToScreen(p3x, p3y, tempPt1);
                gridToScreen(p4x, p4y, tempPt2);
                drawLine(tempPt1, tempPt2, 0, instanceRaster);
                gridToScreen(p1x, p1y, tempPt1);
                gridToScreen(p4x, p4y, tempPt2);
                drawLine(tempPt1, tempPt2, 0, instanceRaster);

                // draw the instance name
                if (canDrawText && gp.isTextVisibilityOn(TextDescriptor.TextType.INSTANCE)) {
                    tempRect.setBounds(lX, lY, hX - lX, hY - lY);
                    TextDescriptor descript = vsc.n.protoDescriptor;
                    NodeProto np = VectorCache.theCache.database.getCell(vsc.subCellId);
                    drawText(tempRect, Poly.Type.TEXTBOX, descript, np.describe(false), textColor, null);
                }
            }
            if (canDrawText && (topLevel || onPathDown || inPlaceCurrent == cell)) {
                drawPortList(vsc, subVC, soX, soY, expanded, onPathDown);
            }
//                drawPortList(vsc, subVC, soX, soY, isExpanded);
        }

        // draw primitives
        drawList(oX, oY, vc.shapes);

        // show cell variables if at the top level
        if (topLevel) {
            drawList(oX, oY, vc.getTopOnlyShapes());
        }
    }

    /**
     * @return true if the cell is properly handled and need no further processing.
     * False to render the contents recursively.
     */
    private boolean expandedCellCached(Cell subCell, Orientation orient, int oX, int oY, VarContext context, boolean fullInstantiate) {
        // if there is no global for remembering cached cells, do not cache
        if (expandedCells == null) {
            return false;
        }

        // do not cache icons: they can be redrawn each time
        if (subCell.isIcon()) {
            return false;
        }

        ExpandedCellKey expansionKey = new ExpandedCellKey(subCell.getId(), orient);
        ExpandedCellInfo expandedCellCount = expandedCells.get(expansionKey);
        if (expandedCellCount != null && expandedCellCount.offscreen == null) {
            if (expandedCellCount.tooLarge) {
                return false;
            }
            // if this combination is not used multiple times, do not cache it
            if (expandedCellCount.singleton && expandedCellCount.instanceCount < 2) {
                if (numberToReconcile > 0) {
                    numberToReconcile--;
                    expandedCellCount.singleton = false;
                } else {
                    return false;
                }
            }
        }

        if (expandedCellCount == null || expandedCellCount.offscreen == null) {
            // compute the cell's location on the screen
            Rectangle2D cellBounds = new Rectangle2D.Double();
            cellBounds.setRect(subCell.getBounds());
            if (canDrawText) {
                Rectangle2D textBounds = subCell.getTextBounds(dummyWnd);
                if (textBounds != null) {
                    cellBounds.add(textBounds);
                }
            }
            AffineTransform rotTrans = orient.pureRotate();
            DBMath.transformRect(cellBounds, rotTrans);
            int lX = (int) Math.ceil(cellBounds.getMinX() * scale - 0.5);
            int hX = (int) Math.floor(cellBounds.getMaxX() * scale + 0.5);
            int lY = (int) Math.ceil(cellBounds.getMinY() * scale - 0.5);
            int hY = (int) Math.floor(cellBounds.getMaxY() * scale + 0.5);
            assert lX <= hX && lY <= hY;

            // if this is the first use, create the offscreen buffer
            if (expandedCellCount == null) {
                expandedCellCount = new ExpandedCellInfo();
                expandedCells.put(expansionKey, expandedCellCount);
            }

            // do not cache if the cell is too large (creates immense offscreen buffers)
            if (hX - lX >= topSz.width / 32 && hY - lY >= topSz.height / 32) {
                expandedCellCount.tooLarge = true;
                return false;
            }

            expandedCellCount.offscreen = new LayerDrawing(scale, lX, hX, lY, hY);
            expandedCellCount.offscreen.drawCell(subCell, null, fullInstantiate, orient, 0, 0, false, context);
            offscreensCreated++;
            offscreenPixelsCreated += expandedCellCount.offscreen.total;
            if (DEBUG) {
                System.out.print(subCell + " " + orient + " rendered in " + expandedCellCount.offscreen.total + " pixels"
                        + " t=" + System.currentTimeMillis());
                for (Layer layer : expandedCellCount.offscreen.layerRasters.keySet()) {
                    System.out.print(" " + layer.getName());
                }
                if (instanceRaster != null) {
                    System.out.print(" INSTANCE");
                }
                System.out.println();
                assert gridRaster == null;
            }
        }

        // copy out of the offscreen buffer into the main buffer
        gridToScreen(oX, oY, tempPt1);
        copyBits(expandedCellCount.offscreen, tempPt1.x, tempPt1.y);
        offscreensUsed++;
        if (TAKE_STATS) {
            offscreensUsedSet.add(expansionKey);
        }
        offscreenPixelsUsed += expandedCellCount.offscreen.total;
        return true;
    }

    // ************************************* CELL CACHING *************************************
    /**
     * Recursive method to count the number of times that a cell-transformation is used
     */
    private void countCell(Cell cell, Rectangle2D drawLimitBounds, boolean fullInstantiate, Orientation orient, AffineTransform prevTrans) {
        // look for subcells
        for (ImmutableNodeInst n: cell.backupUnsafe().cellRevision.nodes) {
            if (!(n.protoId instanceof CellId)) {
                continue;
            }

            // if limiting drawing, reject when out of area
            if (drawLimitBounds != null) {
                Cell subCell = cell.getDatabase().getCell((CellId)n.protoId);
                Rectangle2D bounds = new Rectangle2D.Double();
                n.orient.rectangleBounds(subCell.getBounds(), n.anchor, bounds);
                DBMath.transformRect(bounds, prevTrans);
                if (!DBMath.rectsIntersect(bounds, drawLimitBounds)) {
                    return;
                }
            }

            countNode(cell, n, fullInstantiate, orient, prevTrans);
        }
    }

    /**
     * Recursive method to count the number of times that a cell-transformation is used
     */
    private void countNode(Cell parent, ImmutableNodeInst n, boolean fullInstantiate, Orientation orient, AffineTransform trans) {
        CellId subCellId = (CellId)n.protoId;
        Cell subCell = parent.getDatabase().getCell(subCellId);
        Rectangle2D cellBounds = subCell.getBounds();
        // if the node is tiny, it will be approximated
        double objWidth = Math.max(cellBounds.getWidth(), cellBounds.getHeight());
//        double objWidth = Math.max(ni.getXSize(), ni.getYSize());
        if (objWidth < maxObjectSize) {
            return;
        }

        // transform into the subcell
        Orientation subOrient = orient.concatenate(n.orient);
        AffineTransform subTrans = n.orient.rotateAbout(n.anchor.getLambdaX(), n.anchor.getLambdaY(), 0, 0);
        subTrans.preConcatenate(trans);
//        AffineTransform subTrans = ni.transformOut(trans);

        // compute where this cell lands on the screen
        Poly poly = new Poly(cellBounds);
        poly.transform(subTrans);
//		if (wnd.isInPlaceEdit()) poly.transform(wnd.getInPlaceTransformIn());
        cellBounds = poly.getBounds2D();
        Rectangle screenBounds = databaseToScreen(cellBounds);
        if (screenBounds.width <= 0 || screenBounds.height <= 0) {
            return;
        }
        if (screenBounds.x > sz.width || screenBounds.x + screenBounds.width < 0) {
            return;
        }
        if (screenBounds.y > sz.height || screenBounds.y + screenBounds.height < 0) {
            return;
        }

        // only interested in expanded instances
        boolean expanded = parent.isExpanded(n.nodeId);
//        boolean expanded = ni.isExpanded();
        if (fullInstantiate) {
            expanded = true;
        }

        // if not expanded, but viewing this cell in-place, expand it
        expanded = expanded || inPlaceSubcellPath != null && inPlaceSubcellPath.get(subCell.getId().cellIndex);
        if (!expanded) {
            return;
        }

        if (screenBounds.width < sz.width / 2 || screenBounds.height <= sz.height / 2) {
            // construct the cell name that combines with the transformation
            ExpandedCellKey expansionKey = new ExpandedCellKey(subCell.getId(), subOrient);
            ExpandedCellInfo expansionCount = expandedCells.get(expansionKey);
            if (expansionCount == null) {
                expansionCount = new ExpandedCellInfo();
                expansionCount.instanceCount = 1;
                expandedCells.put(expansionKey, expansionCount);
            } else {
                expansionCount.instanceCount++;
                if (expansionCount.instanceCount > 1) {
                    return;
                }
            }
        }

        // now recurse

        countCell(subCell, null, fullInstantiate, subOrient, subTrans);
    }

    public static void forceRedraw(Cell cell) {
        synchronized (changedCells) {
            changedCells.add(cell.getId());
        }
    }

    private static void forceRedraw(Set<CellId> changedCells) {
        // if there is no global for remembering cached cells, do not cache
        if (expandedCells == null) {
            return;
        }

        List<ExpandedCellKey> keys = new ArrayList<ExpandedCellKey>();
        for (ExpandedCellKey eck : expandedCells.keySet()) {
            keys.add(eck);
        }
        for (ExpandedCellKey expansionKey : keys) {
            if (changedCells.contains(expansionKey.cellId)) {
                expandedCells.remove(expansionKey);
            }
        }
    }

    /**
     * Method to copy the offscreen bits for a cell into the offscreen bits for the entire screen.
     */
    private void copyBits(LayerDrawing srcOffscreen, int centerX, int centerY) {
        if (srcOffscreen == null) {
            return;
        }
        Dimension dim = srcOffscreen.sz;
        int cornerX = centerX - (int) srcOffscreen.originX;
        int cornerY = centerY - (int) srcOffscreen.originY;
        int minSrcX = Math.max(0, clipLX - cornerX);
        int maxSrcX = Math.min(dim.width - 1, clipHX - cornerX);
        int minSrcY = Math.max(0, clipLY - cornerY);
        int maxSrcY = Math.min(dim.height - 1, clipHY - cornerY);
        if (minSrcX > maxSrcX || minSrcY > maxSrcY) {
            return;
        }

        // copy the patterned opaque layers
        for (Map.Entry<Layer, TransparentRaster> e : srcOffscreen.layerRasters.entrySet()) {
            Layer layer = e.getKey();
            ERaster raster = getRaster(layer, null, false);
            if (raster == null) {
                continue;
            }
            TransparentRaster polSrc = e.getValue();
            raster.copyBits(polSrc, minSrcX, maxSrcX, minSrcY, maxSrcY, cornerX, cornerY);
        }
        if (srcOffscreen.instanceRaster != null) {
            ERaster raster = getInstanceRaster();
            raster.copyBits(srcOffscreen.instanceRaster, minSrcX, maxSrcX, minSrcY, maxSrcY, cornerX, cornerY);
        }
        assert srcOffscreen.gridRaster == null;
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
        if (layer == null) {
            layer = Artwork.tech().defaultLayer;
        }
        assert !layer.isPseudoLayer();
        TransparentRaster raster = layerRasters.get(layer);
        if (raster == null) {
            raster = new TransparentRaster(sz.height, numIntsPerRow);
            layerRasters.put(layer, raster);
        }
        if (patternedDisplay && renderedWindow) {
            int[] pattern = null;
            if (graphics == null && layer != null) {
                graphics = gp.getGraphics(layer);
            }
            if (nowPrinting != 0 ? graphics.isPatternedOnPrinter() : graphics.isPatternedOnDisplay()) {
                pattern = graphics.getReversedPattern();
            }
            if (pattern != null) {
                EGraphics.Outline o = graphics.getOutlined();
                if (o == EGraphics.Outline.NOPAT) {
                    o = null;
                }
                currentPatternedTransparentRaster.init(raster.layerBitMap, raster.intsPerRow, pattern, o);
                raster = currentPatternedTransparentRaster;
            }
        }
        return raster;
    }

    ERaster getInstanceRaster() {
        if (instanceRaster == null) {
            instanceRaster = new TransparentRaster(sz.height, numIntsPerRow);
        }
        return instanceRaster;
    }

    ERaster getGridRaster() {
        if (gridRaster == null) {
            gridRaster = new TransparentRaster(sz.height, numIntsPerRow);
        }
        return gridRaster;
    }

//    /**
//     * ERaster for solid opaque layers.
//     */
//    private static class OpaqueRaster implements ERaster {
//        private static OpaqueRaster current = new OpaqueRaster();
//
//        int[] opaqueData;
//        int width;
//        int col;
//
//        void init(int[] opaqueData, int width, int col) {
//            this.opaqueData = opaqueData;
//            this.width = width;
//            this.col = col;
//        }
//
//        public void fillBox(int lX, int hX, int lY, int hY) {
//            int baseIndex = lY*width;
//            for (int y = lY; y <= hY; y++) {
//                for(int x = lX; x <= hX; x++)
//                    opaqueData[baseIndex + x] = col;
//                baseIndex += width;
//            }
//        }
//
//        public void fillHorLine(int y, int lX, int hX) {
//            int baseIndex = y*width + lX;
//            for (int x = lX; x <= hX; x++)
//                opaqueData[baseIndex++] = col;
//        }
//
//        public void fillVerLine(int x, int lY, int hY) {
//            int baseIndex = lY*width + x;
//            for (int y = lY; y <= hY; y++) {
//                opaqueData[baseIndex] = col;
//                baseIndex += width;
//            }
//        }
//
//        public void fillPoint(int x, int y) {
//            opaqueData[y * width + x] = col;
//        }
//
//        public void drawHorLine(int y, int lX, int hX) {
//            int baseIndex = y*width + lX;
//            for (int x = lX; x <= hX; x++)
//                opaqueData[baseIndex++] = col;
//        }
//
//        public void drawVerLine(int x, int lY, int hY) {
//            int baseIndex = lY*width + x;
//            for (int y = lY; y <= hY; y++) {
//                opaqueData[baseIndex] = col;
//                baseIndex += width;
//            }
//        }
//
//        public void drawPoint(int x, int y) {
//            opaqueData[y * width + x] = col;
//        }
//
//        public EGraphics.Outline getOutline() {
//            return null;
//        }
//
//        public void copyBits(TransparentRaster src, int minSrcX, int maxSrcX, int minSrcY, int maxSrcY, int dx, int dy) {
//            int[] srcLayerBitMap = src.layerBitMap;
//            for (int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
//                int destY = srcY + dy;
//                int destBase = destY * width;
//                int srcBaseIndex = srcY*src.intsPerRow;
//                for (int srcX = minSrcX; srcX <= maxSrcX; srcX++) {
//                    int destX = srcX + dx;
//                    if ((srcLayerBitMap[srcBaseIndex + (srcX>>5)] & (1<<(srcX&31))) != 0)
//                        opaqueData[destBase + destX] = col;
//                }
//            }
//        }
//    }
    /**
     * ERaster for solid transparent layers.
     */
    private static class TransparentRaster implements ERaster {

        private static TransparentRaster current = new TransparentRaster();
        int[] layerBitMap;
        int intsPerRow;

        private TransparentRaster() {
        }

        TransparentRaster(int height, int numIntsPerRow) {
            this.intsPerRow = numIntsPerRow;
            layerBitMap = new int[height * numIntsPerRow];
        }

        void init(int[] layerBitMap, int intsPerRow) {
            this.layerBitMap = layerBitMap;
            this.intsPerRow = intsPerRow;
        }

        public void fillBox(int lX, int hX, int lY, int hY) {
            int baseIndex = lY * intsPerRow;
            int lIndex = baseIndex + (lX >> 5);
            int hIndex = baseIndex + (hX >> 5);
            if (lIndex == hIndex) {
                int mask = (2 << (hX & 31)) - (1 << (lX & 31));
                for (int y = lY; y < hY; y++) {
                    layerBitMap[lIndex] |= mask;
                    lIndex += intsPerRow;
                }
            } else {
                int lMask = -(1 << (lX & 31));
                int hMask = (2 << (hX & 31)) - 1;
                for (int y = lY; y <= hY; y++) {
                    layerBitMap[lIndex] |= lMask;
                    for (int index = lIndex + 1; index < hIndex; index++) {
                        layerBitMap[index] = -1;
                    }
                    layerBitMap[hIndex] |= hMask;
                    lIndex += intsPerRow;
                    hIndex += intsPerRow;
                }
            }
        }

        public void eraseBox(int lX, int hX, int lY, int hY) {
            int baseIndex = lY * intsPerRow;
            int lIndex = baseIndex + (lX >> 5);
            int hIndex = baseIndex + (hX >> 5);
            if (lIndex == hIndex) {
                int mask = (2 << (hX & 31)) - (1 << (lX & 31));
                mask = ~mask;
                for (int y = lY; y < hY; y++) {
                    layerBitMap[lIndex] &= mask;
                    lIndex += intsPerRow;
                }
            } else {
                int lMask = -(1 << (lX & 31));
                int hMask = (2 << (hX & 31)) - 1;
                lMask = ~lMask;
                hMask = ~hMask;
                for (int y = lY; y <= hY; y++) {
                    layerBitMap[lIndex] &= lMask;
                    for (int index = lIndex + 1; index < hIndex; index++) {
                        layerBitMap[index] = 0;
                    }
                    layerBitMap[hIndex] &= hMask;
                    lIndex += intsPerRow;
                    hIndex += intsPerRow;
                }
            }
        }

        public void eraseAll() {
            for (int i = 0; i < layerBitMap.length; i++) {
                layerBitMap[i] = 0;
            }
        }

        public void fillHorLine(int y, int lX, int hX) {
            int baseIndex = y * intsPerRow;
            int lIndex = baseIndex + (lX >> 5);
            int hIndex = baseIndex + (hX >> 5);
            if (lIndex == hIndex) {
                layerBitMap[lIndex] |= (2 << (hX & 31)) - (1 << (lX & 31));
            } else {
                layerBitMap[lIndex++] |= -(1 << (lX & 31));
                while (lIndex < hIndex) {
                    layerBitMap[lIndex++] |= -1;
                }
                layerBitMap[hIndex] |= (2 << (hX & 31)) - 1;
            }
        }

        public void fillVerLine(int x, int lY, int hY) {
            int baseIndex = lY * intsPerRow + (x >> 5);
            int mask = 1 << (x & 31);
            for (int y = lY; y <= hY; y++) {
                layerBitMap[baseIndex] |= mask;
                baseIndex += intsPerRow;
            }
        }

        public void fillPoint(int x, int y) {
            layerBitMap[y * intsPerRow + (x >> 5)] |= (1 << (x & 31));
        }

        public void drawHorLine(int y, int lX, int hX) {
            int baseIndex = y * intsPerRow;
            int lIndex = baseIndex + (lX >> 5);
            int hIndex = baseIndex + (hX >> 5);
            if (lIndex == hIndex) {
                layerBitMap[lIndex] |= (2 << (hX & 31)) - (1 << (lX & 31));
            } else {
                layerBitMap[lIndex++] |= -(1 << (lX & 31));
                while (lIndex < hIndex) {
                    layerBitMap[lIndex++] |= -1;
                }
                layerBitMap[hIndex] |= (2 << (hX & 31)) - 1;
            }
        }

        public void drawVerLine(int x, int lY, int hY) {
            int baseIndex = lY * intsPerRow + (x >> 5);
            int mask = 1 << (x & 31);
            for (int y = lY; y <= hY; y++) {
                layerBitMap[baseIndex] |= mask;
                baseIndex += intsPerRow;
            }
        }

        public void drawPoint(int x, int y) {
            layerBitMap[y * intsPerRow + (x >> 5)] |= (1 << (x & 31));
        }

        public EGraphics.Outline getOutline() {
            return null;
        }

        public void copyBits(TransparentRaster src, int minSrcX, int maxSrcX, int minSrcY, int maxSrcY, int dx, int dy) {
            int[] srcLayerBitMap = src.layerBitMap;
            int minDestX = minSrcX + dx;
            int maxDestX = maxSrcX + dx;
            int minDestY = minSrcY + dy;
//            int maxDestY = maxSrcY + dy;
            int leftShift = dx & 31;
            int rightShift = 32 - leftShift;
            int srcBaseIndex = minSrcY * src.intsPerRow + (minSrcX >> 5);
            int destBaseIndex = minDestY * intsPerRow + (minDestX >> 5);
            int numDestInts = (maxDestX >> 5) - (minDestX >> 5);
            if (numDestInts == 0) {
                // Single destination byte.
                int destMask = (2 << (maxDestX & 31)) - (1 << (minDestX & 31));
                if ((minSrcX >> 5) != (maxSrcX >> 5)) {
                    // A pair of source bytes
                    for (int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
                        int s0 = srcLayerBitMap[srcBaseIndex];
                        int s1 = srcLayerBitMap[srcBaseIndex + 1];
                        int v = ((s0 >>> rightShift) | (s1 << leftShift)) & destMask;
                        if (v != 0) {
                            layerBitMap[destBaseIndex] |= v;
                        }
                        srcBaseIndex += src.intsPerRow;
                        destBaseIndex += intsPerRow;
                    }
                } else if ((minDestX & 31) >= (minSrcX & 31)) {
                    // source byte shifted left
                    for (int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
                        int s = srcLayerBitMap[srcBaseIndex];
                        int v = (s << leftShift) & destMask;
                        if (v != 0) {
                            layerBitMap[destBaseIndex] |= v;
                        }
                        srcBaseIndex += src.intsPerRow;
                        destBaseIndex += intsPerRow;
                    }
                } else {
                    // source byte shifted right
                    for (int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
                        int s = srcLayerBitMap[srcBaseIndex];
                        int v = (s >>> rightShift) & destMask;
                        if (v != 0) {
                            layerBitMap[destBaseIndex] |= v;
                        }
                        srcBaseIndex += src.intsPerRow;
                        destBaseIndex += intsPerRow;
                    }
                }
            } else {
                int minDestMask = -(1 << (minDestX & 31));
                int maxDestMask = (2 << (maxDestX & 31)) - 1;
                int srcIncr = src.intsPerRow - (maxSrcX >> 5) + (minSrcX >> 5) - 1;
                if (leftShift == 0) {
                    for (int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
                        assert srcBaseIndex == srcY * src.intsPerRow + (minSrcX >> 5);
                        assert destBaseIndex == (srcY + dy) * intsPerRow + (minDestX >> 5);
                        int v0 = srcLayerBitMap[srcBaseIndex++] & minDestMask;
                        if (v0 != 0) {
                            layerBitMap[destBaseIndex] |= v0;
                        }
                        destBaseIndex++;
                        for (int i = 1; i < numDestInts; i++) {
                            int v = srcLayerBitMap[srcBaseIndex++];
                            if (v != 0) {
                                layerBitMap[destBaseIndex] |= v;
                            }
                            destBaseIndex++;
                        }
                        int vf = srcLayerBitMap[srcBaseIndex++] & maxDestMask;
                        if (vf != 0) {
                            layerBitMap[destBaseIndex] |= vf;
                        }
                        srcBaseIndex += srcIncr;
                        destBaseIndex += (intsPerRow - numDestInts);
                    }
                } else if (numDestInts == 2 && (minSrcX >> 5) == (maxSrcX >> 5)) {
                    for (int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
                        assert srcBaseIndex == srcY * src.intsPerRow + (minSrcX >> 5);
                        assert destBaseIndex == (srcY + dy) * intsPerRow + (minDestX >> 5);
                        int s = srcLayerBitMap[srcBaseIndex];
                        int b0 = srcLayerBitMap[srcBaseIndex++];
                        int v0 = (s << leftShift) & minDestMask;
                        if (v0 != 0) {
                            layerBitMap[destBaseIndex] |= v0;
                        }
                        int vf = (s >>> rightShift) & maxDestMask;
                        if (vf != 0) {
                            layerBitMap[destBaseIndex + 1] |= vf;
                        }
                        srcBaseIndex += src.intsPerRow;
                        destBaseIndex += intsPerRow;
                    }
                } else {
                    boolean minSrcPair = leftShift > (minDestX & 31);
                    boolean maxSrcPair = leftShift <= (maxDestX & 31);
                    for (int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
                        assert srcBaseIndex == srcY * src.intsPerRow + (minSrcX >> 5);
                        assert destBaseIndex == (srcY + dy) * intsPerRow + (minDestX >> 5);
                        int s = minSrcPair ? srcLayerBitMap[srcBaseIndex++] : 0;
                        int b0 = srcLayerBitMap[srcBaseIndex++];
                        int v0 = ((s >>> rightShift) | (b0 << leftShift)) & minDestMask;
                        if (v0 != 0) {
                            layerBitMap[destBaseIndex] |= v0;
                        }
                        destBaseIndex++;
                        s = b0;
                        for (int i = 1; i < numDestInts; i++) {
                            int b = srcLayerBitMap[srcBaseIndex++];
                            int v = (s >>> rightShift) | (b << leftShift);
                            if (v != 0) {
                                layerBitMap[destBaseIndex] |= v;
                            }
                            destBaseIndex++;
                            s = b;
                        }
                        int bf = maxSrcPair ? srcLayerBitMap[srcBaseIndex++] : 0;
                        int vf = ((s >>> rightShift) | (bf << leftShift)) & maxDestMask;
                        if (vf != 0) {
                            layerBitMap[destBaseIndex] |= vf;
                        }
                        srcBaseIndex += srcIncr;
                        destBaseIndex += (intsPerRow - numDestInts);
                    }
                }
            }
        }
    }

//    /**
//     * ERaster for patterned opaque layers.
//     */
//    private static class PatternedOpaqueRaster extends OpaqueRaster {
//        private static PatternedOpaqueRaster current = new PatternedOpaqueRaster();
//
//        int[] pattern;
//        EGraphics.Outline outline;
//
//        private void init(int[] opaqueData, int width, int col, int[] pattern, EGraphics.Outline outline) {
//            super.init(opaqueData, width, col);
//            this.pattern = pattern;
//            this.outline = outline;
//        }
//
//        public void fillBox(int lX, int hX, int lY, int hY) {
//            for (int y = lY; y <= hY; y++) {
//                // setup pattern for this row
//                int pat = pattern[y&15];
//                if (pat == 0) continue;
//
//                int baseIndex = y * width;
//                for (int x = lX; x <= hX; x++) {
//                    if ((pat & (0x8000 >> (x&15))) != 0)
//                        opaqueData[baseIndex + x] = col;
//                }
//            }
//        }
//
//        public void fillHorLine(int y, int lX, int hX) {
//            int pat = pattern[y & 15];
//            if (pat == 0) return;
//            int baseIndex = y * width;
//            for (int x = lX; x <= hX; x++) {
//                if ((pat & (1 << (15-(x&15)))) != 0) {
//                    int index = baseIndex + x;
//                    opaqueData[index] = col;
//                }
//            }
//        }
//
//        public void fillVerLine(int x, int lY, int hY) {
//            int patMask = 0x8000 >> (x&15);
//            int baseIndex = lY*width + x;
//            for (int y = lY; y <= hY; y++) {
//                if ((pattern[y&15] & patMask) != 0)
//                    opaqueData[baseIndex] = col;
//                baseIndex += width;
//            }
//        }
//
//        public void fillPoint(int x, int y) {
//            int patMask = 0x8000 >> (x&15);
//            if ((pattern[y&15] &  patMask) != 0)
//                opaqueData[y*width + x] = col;
//        }
//
//        public EGraphics.Outline getOutline() {
//            return outline;
//        }
//
//        public void copyBits(TransparentRaster src, int minSrcX, int maxSrcX, int minSrcY, int maxSrcY, int dx, int dy) {
//            int[] srcLayerBitMap = src.layerBitMap;
//            for (int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
//                int destY = srcY + dy;
//                int destBase = destY * width;
//                int pat = pattern[destY&15];
//                if (pat == 0) continue;
//                int srcBaseIndex = srcY*src.intsPerRow;
//                for (int srcX = minSrcX; srcX <= maxSrcX; srcX++) {
//                    int destX = srcX + dx;
//                    if ((srcLayerBitMap[srcBaseIndex + (srcX>>5)] & (1<<(srcX&31))) != 0) {
//                        if ((pat & (0x8000 >> (destX&15))) != 0)
//                            opaqueData[destBase + destX] = col;
//                    }
//                }
//            }
//        }
//    }
    /**
     * ERaster for patterned transparent layers.
     */
    private static class PatternedTransparentRaster extends TransparentRaster {

        int[] pattern;
        EGraphics.Outline outline;

        PatternedTransparentRaster() {
        }

        private void init(int[] layerBitMap, int intsPerRow, int[] pattern, EGraphics.Outline outline) {
            super.init(layerBitMap, intsPerRow);
            this.pattern = pattern;
            this.outline = outline;
        }

        public void fillBox(int lX, int hX, int lY, int hY) {
            int baseIndex = lY * intsPerRow;
            int lIndex = baseIndex + (lX >> 5);
            int hIndex = baseIndex + (hX >> 5);
            if (lIndex == hIndex) {
                int mask = (2 << (hX & 31)) - (1 << (lX & 31));
                for (int y = lY; y < hY; y++) {
                    int pat = mask & pattern[y & 15];
                    if (pat != 0) {
                        layerBitMap[lIndex] |= pat;
                    }
                    lIndex += intsPerRow;
                }
            } else {
                int lMask = -(1 << (lX & 31));
                int hMask = (2 << (hX & 31)) - 1;
                for (int y = lY; y <= hY; y++) {
                    int pat = pattern[y & 15];
                    if (pat != 0) {
                        layerBitMap[lIndex] |= lMask & pat;
                        for (int index = lIndex + 1; index < hIndex; index++) {
                            layerBitMap[index] |= pat;
                        }
                        layerBitMap[hIndex] |= hMask & pat;
                    }
                    lIndex += intsPerRow;
                    hIndex += intsPerRow;
                }
            }
        }

        public void fillHorLine(int y, int lX, int hX) {
            int pat = pattern[y & 15];
            if (pat == 0) {
                return;
            }
            int baseIndex = y * intsPerRow;
            int lIndex = baseIndex + (lX >> 5);
            int hIndex = baseIndex + (hX >> 5);
            if (lIndex == hIndex) {
                int mask = pat & ((2 << (hX & 31)) - (1 << (lX & 31)));
                if (mask != 0) {
                    layerBitMap[lIndex] |= mask;
                }
            } else {
                layerBitMap[lIndex++] |= pat & (-(1 << (lX & 31)));
                while (lIndex < hIndex) {
                    layerBitMap[lIndex++] |= pat;
                }
                layerBitMap[hIndex] |= pat & ((2 << (hX & 31)) - 1);
            }
        }

        public void fillVerLine(int x, int lY, int hY) {
            int baseIndex = lY * intsPerRow + (x >> 5);
            int mask = 1 << (x & 31);
            for (int y = lY; y <= hY; y++) {
                if ((pattern[y & 15] & mask) != 0) {
                    layerBitMap[baseIndex] |= mask;
                }
                baseIndex += intsPerRow;
            }
        }

        public void fillPoint(int x, int y) {
            int mask = (1 << (x & 31)) & pattern[y & 15];
            if (mask != 0) {
                layerBitMap[y * intsPerRow + (x >> 5)] |= mask;
            }
        }

        public EGraphics.Outline getOutline() {
            return outline;
        }

        public void copyBits(TransparentRaster src, int minSrcX, int maxSrcX, int minSrcY, int maxSrcY, int dx, int dy) {
            int[] srcLayerBitMap = src.layerBitMap;
            assert (minSrcY + dy) * intsPerRow + ((minSrcX + dx) >> 5) >= 0;
            assert (maxSrcY + dy) * intsPerRow + ((maxSrcX + dx) >> 5) < layerBitMap.length;
            for (int srcY = minSrcY; srcY <= maxSrcY; srcY++) {
                int destY = srcY + dy;
                int pat = pattern[destY & 15];
                if (pat == 0) {
                    continue;
                }
                int srcBaseIndex = srcY * src.intsPerRow;
                int destBaseIndex = destY * intsPerRow;
                for (int srcX = minSrcX; srcX <= maxSrcX; srcX++) {
                    int destX = srcX + dx;
                    if ((srcLayerBitMap[srcBaseIndex + (srcX >> 5)] & (1 << (srcX & 31))) != 0) {
                        int destMask = 1 << (destX & 31);
                        if ((pat & destMask) != 0) {
                            layerBitMap[destBaseIndex + (destX >> 5)] |= destMask;
                        }
                    }
                }
            }
        }
    }
    // ************************************* RENDERING POLY SHAPES *************************************
    private static Rectangle tempRect = new Rectangle();

    private void gridToScreen(int dbX, int dbY, Point result) {
        double scrX = (dbX - factorX) * scale_;
        double scrY = (factorY - dbY) * scale_;
        result.x = (int) (scrX >= 0 ? scrX + 0.5 : scrX - 0.5);
        result.y = (int) (scrY >= 0 ? scrY + 0.5 : scrY - 0.5);
    }

    /**
     * Method to draw a list of cached shapes.
     * @param oX the X offset to draw the shapes (in database grid coordinates).
     * @param oY the Y offset to draw the shapes (in database grid coordinates).
     * @param shapes the List of shapes (VectorBase objects).
     */
    private void drawList(int oX, int oY, List<VectorCache.VectorBase> shapes) //		throws AbortRenderingException
    {
        EditWindow0 textWnd = wnd != null ? wnd : dummyWnd;
        // render all shapes
        for (VectorCache.VectorBase vb : shapes) {
//			if (stopRendering) throw new AbortRenderingException();
            // handle refreshing
            periodicRefresh();

            if (vb instanceof VectorCache.VectorText) {
                VectorCache.VectorText vt = (VectorCache.VectorText) vb;
                TextDescriptor td = vt.descript;
                if (td != null && !td.isAbsoluteSize()) {
                    double size = td.getTrueSize(scale, textWnd);
                    if (size <= canDrawRelativeText) {
                        continue;
                    }
                } else {
                    if (!canDrawText) {
                        continue;
                    }
                }
                switch (vt.textType) {
                    case VectorCache.VectorText.TEXTTYPEARC:
                        if (!gp.isTextVisibilityOn(TextDescriptor.TextType.ARC)) {
                            continue;
                        }
                        break;
                    case VectorCache.VectorText.TEXTTYPENODE:
                        if (!gp.isTextVisibilityOn(TextDescriptor.TextType.NODE)) {
                            continue;
                        }
                        break;
                    case VectorCache.VectorText.TEXTTYPECELL:
                        if (!gp.isTextVisibilityOn(TextDescriptor.TextType.CELL)) {
                            continue;
                        }
                        break;
                    case VectorCache.VectorText.TEXTTYPEEXPORT:
                        if (!gp.isTextVisibilityOn(TextDescriptor.TextType.EXPORT)) {
                            continue;
                        }
                        break;
                    case VectorCache.VectorText.TEXTTYPEANNOTATION:
                        if (!gp.isTextVisibilityOn(TextDescriptor.TextType.ANNOTATION)) {
                            continue;
                        }
                        break;
                    case VectorCache.VectorText.TEXTTYPEINSTANCE:
                        if (!gp.isTextVisibilityOn(TextDescriptor.TextType.INSTANCE)) {
                            continue;
                        }
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

                Color color = textColor;
                if (vt.layer != null) {
                    color = gp.getGraphics(vt.layer).getColor();
                }
                PrimitiveNode baseNode = null;
                if (vt.textType == VectorCache.VectorText.TEXTTYPEEXPORT && vt.basePort != null) {
                    baseNode = vt.basePort.getParent();
                    int exportDisplayLevel = gp.exportDisplayLevel;
                    if (exportDisplayLevel == 2) {
                        // draw export as a cross
                        int cX = (lX + hX) / 2;
                        int cY = (lY + hY) / 2;
                        crossTextList.add(new CrossTextInfo(cX, cY, textColor, baseNode));
                        continue;
                    }

                    // draw export as text
                    if (exportDisplayLevel == 1) {
                        drawString = Export.getShortName(drawString);
                    }
                    color = textColor;
                }

                tempRect.setBounds(lX, lY, hX - lX, hY - lY);
                drawText(tempRect, vt.style, vt.descript, drawString, color, baseNode);
                continue;
            }

            ERaster raster = getRaster(vb.layer, vb.graphicsOverride, false);
            if (raster == null) {
                continue;
            }

            // handle each shape
            if (vb instanceof VectorCache.VectorManhattan) {
                boxCount++;
                VectorCache.VectorManhattan vm = (VectorCache.VectorManhattan) vb;
                boxArrayCount += vm.coords.length / 4;
                for (int i = 0; i < vm.coords.length; i += 4) {
                    int c1X = vm.coords[i];
                    int c1Y = vm.coords[i + 1];
                    int c2X = vm.coords[i + 2];
                    int c2Y = vm.coords[i + 3];

                    // determine coordinates of rectangle on the screen
                    gridToScreen(c1X + oX, c2Y + oY, tempPt1);
                    gridToScreen(c2X + oX, c1Y + oY, tempPt2);
                    int lX = tempPt1.x;
                    int lY = tempPt1.y;
                    int hX = tempPt2.x;
                    int hY = tempPt2.y;

                    drawBox(lX, hX, lY, hY, raster);
                }
            } else if (vb instanceof VectorCache.VectorPolygon) {
                polygonCount++;
                VectorCache.VectorPolygon vp = (VectorCache.VectorPolygon) vb;
                Point[] intPoints = new Point[vp.points.length];
                for (int i = 0; i < vp.points.length; i++) {
                    intPoints[i] = new Point();
                    gridToScreen(vp.points[i].x + oX, vp.points[i].y + oY, intPoints[i]);
                }
                Point[] clippedPoints = GenMath.clipPoly(intPoints, clipLX, clipHX, clipLY, clipHY);
                drawPolygon(clippedPoints, raster);
            } else if (vb instanceof VectorCache.VectorLine) {
                lineCount++;
                VectorCache.VectorLine vl = (VectorCache.VectorLine) vb;

                // determine coordinates of line on the screen
                gridToScreen(vl.fX + oX, vl.fY + oY, tempPt1);
                gridToScreen(vl.tX + oX, vl.tY + oY, tempPt2);

                // clip and draw the line
                drawLine(tempPt1, tempPt2, vl.texture, raster);
            } else if (vb instanceof VectorCache.VectorCross) {
                crossCount++;
                VectorCache.VectorCross vcr = (VectorCache.VectorCross) vb;
                gridToScreen(vcr.x + oX, vcr.y + oY, tempPt1);
                int size = vcr.small ? 3 : 5;
                drawCross(tempPt1.x, tempPt1.y, size, raster);
            } else if (vb instanceof VectorCache.VectorCircle) {
                VectorCache.VectorCircle vci = (VectorCache.VectorCircle) vb;
                gridToScreen(vci.cX + oX, vci.cY + oY, tempPt1);
                gridToScreen(vci.eX + oX, vci.eY + oY, tempPt2);
                switch (vci.nature) {
                    case 0:
                        circleCount++;
                        drawCircle(tempPt1, tempPt2, raster);
                        break;
                    case 1:
                        circleCount++;
                        drawThickCircle(tempPt1, tempPt2, raster);
                        break;
                    case 2:
                        discCount++;
                        drawDisc(tempPt1, tempPt2, raster);
                        break;
                }

            } else if (vb instanceof VectorCache.VectorCircleArc) {
                arcCount++;
                VectorCache.VectorCircleArc vca = (VectorCache.VectorCircleArc) vb;
                gridToScreen(vca.cX + oX, vca.cY + oY, tempPt1);
                gridToScreen(vca.eX1 + oX, vca.eY1 + oY, tempPt2);
                gridToScreen(vca.eX2 + oX, vca.eY2 + oY, tempPt3);
                drawCircleArc(tempPt1, tempPt2, tempPt3, vca.thick, raster);
            }
        }
    }

    /**
     * Method to draw a list of cached port shapes.
     * @param oX the X offset to draw the shapes (in database grid coordinates).
     * @param oY the Y offset to draw the shapes (in database grid coordinates).
     * @param expanded true to draw a list on expanded instance.
	 * @param onPathDown true if this level of hierarchy is the current one in "down-in-place" editing.
     */
    private void drawPortList(VectorCache.VectorSubCell vsc, VectorCache.VectorCell subVC_,
    	int oX, int oY, boolean expanded, boolean onPathDown)
    //		throws AbortRenderingException
    {
        if (!gp.isTextVisibilityOn(TextDescriptor.TextType.PORT)) {
            return;
        }
        // render all shapes
        List<VectorCache.VectorCellExport> portShapes = subVC_.vcg.getPortShapes();
        int[] portCenters = subVC_.getPortCenters();
        assert portShapes.size() * 2 == portCenters.length;
        for (int i = 0; i < portShapes.size(); i++) {
            VectorCache.VectorCellExport vce = portShapes.get(i);
//			if (stopRendering) throw new AbortRenderingException();

            // get visual characteristics of shape
            if (!onPathDown && vsc.shownPorts.get(vce.getChronIndex())) {
                continue;
            }
//			if (vt.height < maxTextSize) continue;

            int cX = portCenters[i * 2];
            int cY = portCenters[i * 2 + 1];
            gridToScreen(cX + oX, cY + oY, tempPt1);
            cX = tempPt1.x;
            cY = tempPt1.y;

            int portDisplayLevel = gp.portDisplayLevel;
            Color portColor;
            if (expanded) {
                portColor = textColor;
            } else {
                PrimitivePort basePort = vce.getBasePort();
                portColor = portColorsCache.get(basePort);
                if (portColor == null) {
                    portColor = basePort.getPortColor(gp);
                    if (portColor == null) {
                        portColor = textColor;
                    }
                    portColorsCache.put(basePort, portColor);
                }
            }
            if (portDisplayLevel == 2) {
                // draw port as a cross
                crossTextList.add(new CrossTextInfo(cX, cY, portColor, null));
                continue;
            }

            // draw port as text
            boolean shortName = portDisplayLevel == 1;
            String drawString = vce.getName(shortName);

            tempRect.setBounds(cX, cY, 0, 0);
            drawText(tempRect, vce.style, vce.descript, drawString, portColor, null);
        }
    }

    // ************************************* BOX DRAWING *************************************
    /**
     * Method to draw a box on the off-screen buffer.
     */
    public void drawBox(int lX, int hX, int lY, int hY, ERaster raster) {
        if (lX < clipLX) {
            lX = clipLX;
        }
        if (hX > clipHX) {
            hX = clipHX;
        }
        if (lY < clipLY) {
            lY = clipLY;
        }
        if (hY > clipHY) {
            hY = clipHY;
        }
        if (lX > hX || lY > hY) {
            return;
        }
        boxDisplayCount++;
        EGraphics.Outline o = raster.getOutline();
        if (lY == hY) {
            if (lX == hX) {
                if (o == null) {
                    raster.fillPoint(lX, lY);
                } else {
                    raster.drawPoint(lX, lY);
                }
            } else {
                if (o == null) {
                    raster.fillHorLine(lY, lX, hX);
                } else {
                    raster.drawHorLine(lY, lX, hX);
                }
            }
            return;
        }
        if (lX == hX) {
            if (o == null) {
                raster.fillVerLine(lX, lY, hY);
            } else {
                raster.drawVerLine(lX, lY, hY);
            }
            return;
        }
        raster.fillBox(lX, hX, lY, hY);
        if (o == null) {
            return;
        }
        if (o.isSolidPattern()) {
            raster.drawVerLine(lX, lY, hY);
            raster.drawHorLine(hY, lX, hX);
            raster.drawVerLine(hX, lY, hY);
            raster.drawHorLine(lY, lX, hX);
            if (o.getThickness() != 1) {
                for (int i = 1; i < o.getThickness(); i++) {
                    if (lX + i <= clipHX) {
                        raster.drawVerLine(lX + i, lY, hY);
                    }
                    if (hY - i >= clipLX) {
                        raster.drawHorLine(hY - i, lX, hX);
                    }
                    if (hX - i >= clipLY) {
                        raster.drawVerLine(hX - i, lY, hY);
                    }
                    if (lY + i <= clipHY) {
                        raster.drawHorLine(lY + i, lX, hX);
                    }
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
                for (int i = 1; i < o.getThickness(); i++) {
                    if (lX + i <= clipHX) {
                        drawVerOutline(lX + i, lY, hY, pattern, len, raster);
                    }
                    if (hY - i >= clipLX) {
                        drawHorOutline(hY - i, lX, hX, pattern, len, raster);
                    }
                    if (hX - i >= clipLY) {
                        drawVerOutline(hX - i, lY, hY, pattern, len, raster);
                    }
                    if (lY + i <= clipHY) {
                        drawHorOutline(lY + i, lX, hX, pattern, len, raster);
                    }
                }
            }
        }
    }

    private static void drawHorOutline(int y, int lX, int hX, int pattern, int len, ERaster raster) {
        int i = 0;
        for (int x = lX; x <= hX; x++) {
            if ((pattern & (1 << i)) != 0) {
                raster.drawPoint(x, y);
            }
            i++;
            if (i == len) {
                i = 0;
            }
        }
    }

    private static void drawVerOutline(int x, int lY, int hY, int pattern, int len, ERaster raster) {
        int i = 0;
        for (int y = lY; y <= hY; y++) {
            if ((pattern & (1 << i)) != 0) {
                raster.drawPoint(x, y);
            }
            i++;
            if (i == len) {
                i = 0;
            }
        }
    }

//    /**
//     * Method to draw a box on the off-screen buffer.
//     */
//    private void drawBox(int lX, int hX, int lY, int hY, byte[] layerBitMap, byte layerBitMask) {
//        boxes++;
//        int dx = hX - lX;
//        int dy = hY - lY;
//        int baseIndex = lY * width + lX;
//        if (dx >= dy) {
//            int baseIncr = width - (dx + 1);
//            for (int i = dy; i >= 0; i--) {
//                for (int j = dx; j >= 0; j--) {
//                    layerBitMap[baseIndex] |= layerBitMask;
//                    baseIndex += 1;
//                }
//                baseIndex += baseIncr;
//            }
//        } else {
//            int baseIncr = 1 - (dy + 1) * width;
//            for (int i = dx; i >= 0; i--) {
//                for (int j = dy; j >= 0; j--) {
//                    layerBitMap[baseIndex] |= layerBitMask;
//                    baseIndex += width;
//                }
//                baseIndex += baseIncr;
//            }
//        }
//    }
    // ************************************* LINE DRAWING *************************************
    /**
     * Method to draw a line on the off-screen buffer.
     */
    void drawLine(Point pt1, Point pt2, int texture, ERaster raster) {
        // first clip the line
        if (GenMath.clipLine(pt1, pt2, 0, sz.width - 1, 0, sz.height - 1)) {
            return;
        }

        // now draw with the proper line type
        switch (texture) {
            case 0:
                drawSolidLine(pt1.x, pt1.y, pt2.x, pt2.y, raster);
                break;
            case 1:
                drawPatLine(pt1.x, pt1.y, pt2.x, pt2.y, 0x88, 8, raster);
                break;
            case 2:
                drawPatLine(pt1.x, pt1.y, pt2.x, pt2.y, 0xE7, 8, raster);
                break;
            case 3:
                drawThickLine(pt1.x, pt1.y, pt2.x, pt2.y, raster);
                break;
        }
    }

    private void drawCross(int cX, int cY, int size, ERaster raster) {
        if (clipLY <= cY && cY <= clipHY) {
            int lX = Math.max(clipLX, cX - size);
            int hX = Math.min(clipHX, cX + size);
            if (lX <= hX) {
                raster.drawHorLine(cY, lX, hX);
            }
        }
        if (clipLX <= cX && cX <= clipHX) {
            int lY = Math.max(clipLY, cY - size);
            int hY = Math.min(clipHY, cY + size);
            if (lY <= hY) {
                raster.drawVerLine(cX, lY, hY);
            }
        }
    }

    private void drawSolidLine(int x1, int y1, int x2, int y2, ERaster raster) {
        // initialize the Bresenham algorithm
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        if (dx > dy) {
            // initialize for lines that increment along X
            int incr1 = 2 * dy;
            int incr2 = 2 * (dy - dx);
            int d = incr2;
            int x, y, xend, yend, yincr;
            if (x1 > x2) {
                x = x2;
                y = y2;
                xend = x1;
                yend = y1;
            } else {
                x = x1;
                y = y1;
                xend = x2;
                yend = y2;
            }
            if (yend < y) {
                yincr = -1;
            } else {
                yincr = 1;
            }
            raster.drawPoint(x, y);

            // draw line that increments along X
            while (x < xend) {
                x++;
                if (d < 0) {
                    d += incr1;
                } else {
                    y += yincr;
                    d += incr2;
                }
                raster.drawPoint(x, y);
            }
        } else {
            // initialize for lines that increment along Y
            int incr1 = 2 * dx;
            int incr2 = 2 * (dx - dy);
            int d = incr2;
            int x, y, xend, yend, xincr;
            if (y1 > y2) {
                x = x2;
                y = y2;
                xend = x1;
                yend = y1;
            } else {
                x = x1;
                y = y1;
                xend = x2;
                yend = y2;
            }
            if (xend < x) {
                xincr = -1;
            } else {
                xincr = 1;
            }
            raster.drawPoint(x, y);

            // draw line that increments along X
            while (y < yend) {
                y++;
                if (d < 0) {
                    d += incr1;
                } else {
                    x += xincr;
                    d += incr2;
                }
                raster.drawPoint(x, y);
            }
        }
    }

//    private void drawSolidLine(int x1, int y1, int x2, int y2, byte[] layerBitMap, byte layerBitMask) {
//        solidLines++;
//        // initialize the Bresenham algorithm
//        int dx = Math.abs(x2-x1);
//        int dy = Math.abs(y2-y1);
//        if (dx >= dy) {
//            // initialize for lines that increment along X
//            int incr1 = 2 * dy;
//            int incr2 = 2 * (dy - dx);
//            int d = incr2;
//            int x, y, yend;
//            if (x1 <= x2) {
//                x = x1;   y = y1;   yend = y2;
//            } else {
//                x = x2;   y = y2;   yend = y1;
//            }
//            int baseIndex = y * width + x;
//            if (dy == 0) {
//                // draw horizontal line
//                for (int i = dx; i >= 0; i--)
//                    layerBitMap[baseIndex++] |= layerBitMask;
//            } else {
//                // draw line that increments along X
//                int baseIncr = yend >= y ? 1 + width : 1 - width;
//                for (int i = dx; i >= 0; i--) {
//                    layerBitMap[baseIndex] |= layerBitMask;
//                    if (d < 0) {
//                        d += incr1;
//                        baseIndex += 1;
//                    } else {
//                        d += incr2;
//                        baseIndex += baseIncr;
//                    }
//                }
//            }
//        } else {
//            // initialize for lines that increment along Y
//            int incr1 = 2 * dx;
//            int incr2 = 2 * (dx - dy);
//            int d = incr2;
//            int x, y, xend;
//            if (y1 <= y2) {
//                x = x1;   y = y1;   xend = x2;
//            } else {
//                x = x2;   y = y2;   xend = x1;
//            }
//            int baseIndex = y * width + x;
//            if (dx == 0) {
//                // draw vertical line
//                for (int i = dy; i >= 0; i--) {
//                    layerBitMap[baseIndex] |= layerBitMask;
//                    baseIndex += width;
//                }
//            } else {
//                int baseIncr = xend >= x ? width + 1 : width - 1;
//                for (int i = dy; i >= 0; i--) {
//                    layerBitMap[baseIndex] |= layerBitMask;
//                    if (d < 0) {
//                        d += incr1;
//                        baseIndex += width;
//                    } else {
//                        d += incr2;
//                        baseIndex += baseIncr;
//                    }
//                }
//            }
//        }
//    }
    private void drawOutline(int x1, int y1, int x2, int y2, int pattern, int len, ERaster raster) {
        tempPt3.x = x1;
        tempPt3.y = y1;
        tempPt4.x = x2;
        tempPt4.y = y2;

        // first clip the line
        if (GenMath.clipLine(tempPt3, tempPt4, 0, sz.width - 1, 0, sz.height - 1)) {
            return;
        }

        drawPatLine(tempPt3.x, tempPt3.y, tempPt4.x, tempPt4.y, pattern, len, raster);
    }

    private void drawPatLine(int x1, int y1, int x2, int y2, int pattern, int len, ERaster raster) {
        // initialize counter for line style
        int i = 0;

        // initialize the Bresenham algorithm
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        if (dx > dy) {
            // initialize for lines that increment along X
            int incr1 = 2 * dy;
            int incr2 = 2 * (dy - dx);
            int d = incr2;
            int x, y, xend, yend, yincr;
            if (x1 > x2) {
                x = x2;
                y = y2;
                xend = x1;
                yend = y1;
            } else {
                x = x1;
                y = y1;
                xend = x2;
                yend = y2;
            }
            if (yend < y) {
                yincr = -1;
            } else {
                yincr = 1;
            }
            raster.drawPoint(x, y);

            // draw line that increments along X
            while (x < xend) {
                x++;
                if (d < 0) {
                    d += incr1;
                } else {
                    y += yincr;
                    d += incr2;
                }
                i++;
                if (i == len) {
                    i = 0;
                }
                if ((pattern & (1 << i)) == 0) {
                    continue;
                }
                raster.drawPoint(x, y);
            }
        } else {
            // initialize for lines that increment along Y
            int incr1 = 2 * dx;
            int incr2 = 2 * (dx - dy);
            int d = incr2;
            int x, y, xend, yend, xincr;
            if (y1 > y2) {
                x = x2;
                y = y2;
                xend = x1;
                yend = y1;
            } else {
                x = x1;
                y = y1;
                xend = x2;
                yend = y2;
            }
            if (xend < x) {
                xincr = -1;
            } else {
                xincr = 1;
            }
            raster.drawPoint(x, y);

            // draw line that increments along X
            while (y < yend) {
                y++;
                if (d < 0) {
                    d += incr1;
                } else {
                    x += xincr;
                    d += incr2;
                }
                i++;
                if (i == len) {
                    i = 0;
                }
                if ((pattern & (1 << i)) == 0) {
                    continue;
                }
                raster.drawPoint(x, y);
            }
        }
    }

//    private void drawPatLine(int x1, int y1, int x2, int y2, byte[] layerBitMap, byte layerBitMask, int pattern, int len) {
//        patLines++;
//        // initialize the Bresenham algorithm
//        int dx = Math.abs(x2-x1);
//        int dy = Math.abs(y2-y1);
//        if (dx >= dy) {
//            // initialize for lines that increment along X
//            int incr1 = 2 * dy;
//            int incr2 = 2 * (dy - dx);
//            int d = incr2;
//            int x, y, yend;
//            if (x1 <= x2) {
//                x = x1;   y = y1;   yend = y2;
//            } else {
//                x = x2;   y = y2;   yend = y1;
//            }
//            int baseIndex = y * width + x;
//            if (dy == 0) {
//                // draw horizontal line
//                for (int i = 0; i <= dx; i++) {
//                    if ((pattern & (1 << (i&7))) != 0)
//                        layerBitMap[baseIndex++] |= layerBitMask;
//                }
//            } else {
//                // draw line that increments along X
//                int baseIncr = yend >= y ? 1 + width : 1 - width;
//                for (int i = 0; i <= dx; i++) {
//                    if ((pattern & (1 << (i&7))) != 0)
//                        layerBitMap[baseIndex] |= layerBitMask;
//                    if (d < 0) {
//                        d += incr1;
//                        baseIndex += 1;
//                    } else {
//                        d += incr2;
//                        baseIndex += baseIncr;
//                    }
//                }
//            }
//        } else {
//            // initialize for lines that increment along Y
//            int incr1 = 2 * dx;
//            int incr2 = 2 * (dx - dy);
//            int d = incr2;
//            int x, y, xend;
//            if (y1 <= y2) {
//                x = x1;   y = y1;   xend = x2;
//            } else {
//                x = x2;   y = y2;   xend = x1;
//            }
//            int baseIndex = y * width + x;
//            if (dx == 0) {
//                // draw vertical line
//                for (int i = 0; i <= dy; i++) {
//                    if ((pattern & (1 << (i&7))) != 0)
//                        layerBitMap[baseIndex] |= layerBitMask;
//                    baseIndex += width;
//                }
//            } else {
//                int baseIncr = xend >= x ? width + 1 : width - 1;
//                for (int i = 0; i <= dy; i++) {
//                    if ((pattern & (1 << (i&7))) != 0)
//                        layerBitMap[baseIndex] |= layerBitMask;
//                    if (d < 0) {
//                        d += incr1;
//                        baseIndex += width;
//                    } else {
//                        d += incr2;
//                        baseIndex += baseIncr;
//                    }
//                }
//            }
//        }
//    }
    private void drawThickLine(int x1, int y1, int x2, int y2, ERaster raster) {
        // initialize the Bresenham algorithm
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        if (dx > dy) {
            // initialize for lines that increment along X
            int incr1 = 2 * dy;
            int incr2 = 2 * (dy - dx);
            int d = incr2;
            int x, y, xend, yend, yincr;
            if (x1 > x2) {
                x = x2;
                y = y2;
                xend = x1;
                yend = y1;
            } else {
                x = x1;
                y = y1;
                xend = x2;
                yend = y2;
            }
            if (yend < y) {
                yincr = -1;
            } else {
                yincr = 1;
            }
            drawThickPoint(x, y, raster);

            // draw line that increments along X
            while (x < xend) {
                x++;
                if (d < 0) {
                    d += incr1;
                } else {
                    y += yincr;
                    d += incr2;
                }
                drawThickPoint(x, y, raster);
            }
        } else {
            // initialize for lines that increment along Y
            int incr1 = 2 * dx;
            int incr2 = 2 * (dx - dy);
            int d = incr2;
            int x, y, xend, yend, xincr;
            if (y1 > y2) {
                x = x2;
                y = y2;
                xend = x1;
                yend = y1;
            } else {
                x = x1;
                y = y1;
                xend = x2;
                yend = y2;
            }
            if (xend < x) {
                xincr = -1;
            } else {
                xincr = 1;
            }
            drawThickPoint(x, y, raster);

            // draw line that increments along X
            while (y < yend) {
                y++;
                if (d < 0) {
                    d += incr1;
                } else {
                    x += xincr;
                    d += incr2;
                }
                drawThickPoint(x, y, raster);
            }
        }
    }

//    private void drawThickLine(int x1, int y1, int x2, int y2, byte[] layerBitMap, byte layerBitMask) {
//        thickLines++;
//        // initialize the Bresenham algorithm
//        int dx = Math.abs(x2-x1);
//        int dy = Math.abs(y2-y1);
//        if (dx >= dy) {
//            // initialize for lines that increment along X
//            int incr1 = 2 * dy;
//            int incr2 = 2 * (dy - dx);
//            int d = incr2;
//            int x, y, xend, yend;
//            if (x1 <= x2) {
//                x = x1;   y = y1;   xend = x2;  yend = y2;
//            } else {
//                x = x2;   y = y2;   xend = x1;  yend = y1;
//            }
//            if (dy == 0) {
//                // draw horizontal line
//                drawBox(x, xend, Math.max(clipLY, y - 1), Math.min(clipHY, y + 1), layerBitMap, layerBitMask);
//                if (x > clipLX)
//                    drawPoint(x - 1, y, layerBitMap, layerBitMask);
//                if (xend < clipHX)
//                    drawPoint(xend + 1, y, layerBitMap, layerBitMask);
//            } else {
//                // draw line that increments along X
//                int yIncr = yend >= y ? 1 : -1;
//                for (int i = 0; i <= dx; i++) {
//                    drawThickPoint(x + i, y, layerBitMap, layerBitMask);
//                    if (d < 0) {
//                        d += incr1;
//                    } else {
//                        d += incr2;
//                        y += yIncr;
//                    }
//                }
//            }
//        } else {
//            // initialize for lines that increment along Y
//            int incr1 = 2 * dx;
//            int incr2 = 2 * (dx - dy);
//            int d = incr2;
//            int x, y, xend, yend;
//            if (y1 <= y2) {
//                x = x1;   y = y1;   xend = x2;  yend = y2;
//            } else {
//                x = x2;   y = y2;   xend = x1;  yend = x1;
//            }
//            if (dx == 0) {
//                // draw vertical line
//                drawBox(Math.max(clipLX, x - 1), Math.min(clipHX, x + 1), y, yend, layerBitMap, layerBitMask);
//                if (y > clipLY)
//                    drawPoint(x, y - 1, layerBitMap, layerBitMask);
//                if (yend < clipHY)
//                    drawPoint(x, yend + 1, layerBitMap, layerBitMask);
//            } else {
//                int xIncr = xend >= x ? 1 : - 1;
//                for (int i = 0; i <= dy; i++) {
//                    drawThickPoint(x, y + i, layerBitMap, layerBitMask);
//                    if (d < 0) {
//                        d += incr1;
//                    } else {
//                        d += incr2;
//                        x += xIncr;
//                    }
//                }
//            }
//        }
//    }
    // ************************************* POLYGON DRAWING *************************************
    /**
     * Method to draw a polygon on the off-screen buffer.
     */
    void drawPolygon(Point[] points, ERaster raster) {
        // fill in internal structures
        PolySeg edgelist = null;
        PolySeg[] polySegs = new PolySeg[points.length];
        for (int i = 0; i < points.length; i++) {
            polySegs[i] = new PolySeg();
            if (i == 0) {
                polySegs[i].fx = points[points.length - 1].x;
                polySegs[i].fy = points[points.length - 1].y;
            } else {
                polySegs[i].fx = points[i - 1].x;
                polySegs[i].fy = points[i - 1].y;
            }
            polySegs[i].tx = points[i].x;
            polySegs[i].ty = points[i].y;
        }
        for (int i = 0; i < points.length; i++) {
            // compute the direction of this edge
            int j = polySegs[i].ty - polySegs[i].fy;
            if (j > 0) {
                polySegs[i].direction = 1;
            } else if (j < 0) {
                polySegs[i].direction = -1;
            } else {
                polySegs[i].direction = 0;
            }

            // compute the X increment of this edge
            if (j == 0) {
                polySegs[i].increment = 0;
            } else {
                polySegs[i].increment = polySegs[i].tx - polySegs[i].fx;
                if (polySegs[i].increment != 0) {
                    polySegs[i].increment =
                            (polySegs[i].increment * 65536 - j + 1) / j;
                }
            }
            polySegs[i].tx <<= 16;
            polySegs[i].fx <<= 16;

            // make sure "from" is above "to"
            if (polySegs[i].fy > polySegs[i].ty) {
                j = polySegs[i].tx;
                polySegs[i].tx = polySegs[i].fx;
                polySegs[i].fx = j;
                j = polySegs[i].ty;
                polySegs[i].ty = polySegs[i].fy;
                polySegs[i].fy = j;
            }

            // insert this edge into the edgelist, sorted by ascending "fy"
            if (edgelist == null) {
                edgelist = polySegs[i];
                polySegs[i].nextedge = null;
            } else {
                // insert by ascending "fy"
                if (edgelist.fy > polySegs[i].fy) {
                    polySegs[i].nextedge = edgelist;
                    edgelist = polySegs[i];
                } else {
                    for (PolySeg a = edgelist; a != null; a = a.nextedge) {
                        if (a.nextedge == null
                                || a.nextedge.fy > polySegs[i].fy) {
                            // insert after this
                            polySegs[i].nextedge = a.nextedge;
                            a.nextedge = polySegs[i];
                            break;
                        }
                    }
                }
            }
        }

        // scan polygon and render
        int ycur = 0;
        PolySeg active = null;
        while (active != null || edgelist != null) {
            if (active == null) {
                active = edgelist;
                active.nextactive = null;
                edgelist = edgelist.nextedge;
                ycur = active.fy;
            }

            // introduce edges from edge list into active list
            while (edgelist != null && edgelist.fy <= ycur) {
                // insert "edgelist" into active list, sorted by "fx" coordinate
                if (active.fx > edgelist.fx
                        || (active.fx == edgelist.fx && active.increment > edgelist.increment)) {
                    edgelist.nextactive = active;
                    active = edgelist;
                    edgelist = edgelist.nextedge;
                } else {
                    for (PolySeg a = active; a != null; a = a.nextactive) {
                        if (a.nextactive == null
                                || a.nextactive.fx > edgelist.fx
                                || (a.nextactive.fx == edgelist.fx
                                && a.nextactive.increment > edgelist.increment)) {
                            // insert after this
                            edgelist.nextactive = a.nextactive;
                            a.nextactive = edgelist;
                            edgelist = edgelist.nextedge;
                            break;
                        }
                    }
                }
            }

            // generate regions to be filled in on current scan line
            int wrap = 0;
            PolySeg left = active;
            for (PolySeg edge = active; edge != null; edge = edge.nextactive) {
                wrap = wrap + edge.direction;
                if (wrap == 0) {
                    int j = (left.fx + 32768) >> 16;
                    int k = (edge.fx + 32768) >> 16;

                    raster.fillHorLine(ycur, j, k);

                    left = edge.nextactive;
                }
            }
            ycur++;

            // update edges in active list
            PolySeg lastedge = null;
            for (PolySeg edge = active; edge != null; edge = edge.nextactive) {
                if (ycur >= edge.ty) {
                    if (lastedge == null) {
                        active = edge.nextactive;
                    } else {
                        lastedge.nextactive = edge.nextactive;
                    }
                } else {
                    edge.fx += edge.increment;
                    lastedge = edge;
                }
            }
        }

        // if outlined pattern, draw the outline
        EGraphics.Outline o = raster.getOutline();
        if (o == null) {
            return;
        }
        for (int i = 0; i < points.length; i++) {
            int last = i - 1;
            if (last < 0) {
                last = points.length - 1;
            }
            int fX = points[last].x;
            int fY = points[last].y;
            int tX = points[i].x;
            int tY = points[i].y;
            drawOutline(fX, fY, tX, tY, o.getPattern(), o.getLen(), raster);
            if (o.getThickness() != 1) {
                int ang = GenMath.figureAngle(new Point2D.Double(fX, fY), new Point2D.Double(tX, tY));
                double sin = DBMath.sin(ang + 900);
                double cos = DBMath.cos(ang + 900);
                for (int t = 1; t < o.getThickness(); t++) {
                    int dX = (int) (cos * t + 0.5);
                    int dY = (int) (sin * t + 0.5);
                    drawOutline(fX + dX, fY + dY, tX + dX, tY + dY, o.getPattern(), o.getLen(), raster);
                }
            }
        }
    }

    // ************************************* TEXT DRAWING *************************************
    /**
     * Method to draw a text on the off-screen buffer
     */
    public void drawText(Rectangle rect, Poly.Type style, TextDescriptor descript, String s, Color color, PrimitiveNode baseNode) {
        // quit if string is null
        if (s == null) {
            return;
        }
        int len = s.length();
        if (len == 0) {
            return;
        }

        // get parameters
//        if (color == null) color = new Color(0,0,0);
//        int col = color.getRGB() & 0xFFFFFF;

        // get text description
        int size = EditWindow.getDefaultFontSize();
        String fontName = gp.defaultFont;
        boolean italic = false;
        boolean bold = false;
        boolean underline = false;
        int rotation = 0;
        int greekScale = 0;
        if (descript != null) {
            rotation = descript.getRotation().getIndex();
            int colorIndex = descript.getColorIndex();
            if (colorIndex != 0) {
                Color full = EGraphics.getColorFromIndex(colorIndex);
                if (full != null) {
                    color = full;
                }
//				if (full != null) col = full.getRGB() & 0xFFFFFF;
            }
            double dSize = descript.getTrueSize(scale, wnd);
            size = Math.min((int) dSize, MAXIMUMTEXTSIZE);
            if (size < MINIMUMTEXTSIZE) {
                // text too small: scale it to get proper size
                greekScale = 2;
                for (;;) {
                    size = (int) (dSize * greekScale);
                    if (size >= MINIMUMTEXTSIZE) {
                        break;
                    }
                    greekScale *= 2;
                }
            }

            italic = descript.isItalic();
            bold = descript.isBold();
            underline = descript.isUnderline();
            int fontIndex = descript.getFace();
            if (fontIndex != 0) {
                TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(fontIndex);
                if (af != null) {
                    fontName = af.getName();
                }
            }
        }

        // get box information for limiting text size
        if (style == Poly.Type.TEXTBOX) {
            if (rect.x >= sz.width || rect.x + rect.width < 0 || rect.y >= sz.height || rect.y + rect.height < 0) {
                return;
            }
        }

        // create RenderInfo
        RenderTextInfo renderInfo = new RenderTextInfo(color, baseNode);
        if (!renderInfo.buildInfo(s, fontName, size, italic, bold, underline, rect, style, rotation)) {
            return;
        }

        // if text was made "greek", just draw a line
        if (greekScale != 0) {
            // text too small: make it "greek"
            int width = (int) renderInfo.bounds.getWidth() / greekScale;
            int sizeIndent = (size / greekScale + 1) / 4;
            Point pt = getTextCorner(width, size / greekScale, style, rect, rotation);

            // do clipping
            int lX = pt.x;
            int hX = lX + width;
            int lY = pt.y + sizeIndent;
            int hY = lY;
            if (lX < 0) {
                lX = 0;
            }
            if (hX >= sz.width) {
                hX = sz.width - 1;
            }
            if (lY < 0) {
                lY = 0;
            }
            if (hY >= sz.height) {
                hY = sz.height - 1;
            }

            // greeked box in opaque area
            if (lX > hX || lY > hY) {
                return;
            }

            greekTextList.add(new GreekTextInfo(lX, hX, lY, hY, color, baseNode));
//            for(int y=lY; y<=hY; y++) {
//                int baseIndex = y * sz.width + lX;
//                for(int x=lX; x<=hX; x++) {
//                    int index = baseIndex++;
//                    int alpha = (opaqueData[index] >> 24) & 0xFF;
//                    if (alpha == 0xFF) opaqueData[index] = col;
//                }
//            }
            return;
        }

        // check if text is on-screen
        if (renderInfo.bounds.getMinX() >= sz.width || renderInfo.bounds.getMaxX() < 0
                || renderInfo.bounds.getMinY() >= sz.height || renderInfo.bounds.getMaxY() < 0) {
            return;
        }

        // render the text
        renderTextList.add(renderInfo);
//        renderInfo.draw();
    }

    private static class TextInfo {

        private Color color;
        private PrimitiveNode baseNode;

        private TextInfo(Color color, PrimitiveNode baseNode) {
            if (color == null) {
                throw new NullPointerException();
            }
            this.color = color;
            this.baseNode = baseNode;
        }

        boolean isDrawn(Graphics2D g, LayerVisibility lv) {
            if (baseNode != null && !lv.isVisible(baseNode)) {
                return false;
            }
            g.setColor(color);
            return true;
        }
    }

    private static class RenderTextInfo extends TextInfo {

        private GlyphVector gv;
        private LineMetrics lm;
        private Rectangle2D rasBounds;              // the raster bounds of the unrotated text, in pixels (screen units)
        private Rectangle2D bounds;                 // the real bounds of the rotated, anchored text (in screen units)
        private boolean underline;
        private int rotation;
        private Rectangle rect;
        private int offX, offY;

        private RenderTextInfo(Color color, PrimitiveNode baseNode) {
            super(color, baseNode);
        }

        private boolean buildInfo(String msg, String fontName, int tSize, boolean italic, boolean bold, boolean underline,
                Rectangle probableBoxedBounds, Poly.Type style, int rotation) {
            Font font = getFont(msg, fontName, tSize, italic, bold, underline);
            this.underline = underline;
            this.rotation = rotation;
            rect = (Rectangle) probableBoxedBounds.clone();

            // convert the text to a GlyphVector
            FontRenderContext frc = new FontRenderContext(null, true, true);
            gv = font.createGlyphVector(frc, msg);
            lm = font.getLineMetrics(msg, frc);

            // figure bounding box of text
            Rectangle2D rasRect = gv.getLogicalBounds();
            int width = (int) rasRect.getWidth();
            int height = (int) (lm.getHeight() + 0.5);
            if (width <= 0 || height <= 0) {
                return false;
            }
            int fontStyle = font.getStyle();

            int boxedWidth = (int) probableBoxedBounds.getWidth();
            int boxedHeight = (int) probableBoxedBounds.getHeight();

            // if text is to be "boxed", make sure it fits
            if (boxedWidth > 1 && boxedHeight > 1) {
                if (width > boxedWidth || height > boxedHeight) {
                    double scale = Math.min((double) boxedWidth / width, (double) boxedHeight / height);
                    font = new Font(fontName, fontStyle, (int) (tSize * scale));
                    if (font != null) {
                        // convert the text to a GlyphVector
                        gv = font.createGlyphVector(frc, msg);
                        lm = font.getLineMetrics(msg, frc);
                        rasRect = gv.getLogicalBounds();
                        height = (int) (lm.getHeight() + 0.5);
                        if (height <= 0) {
                            return false;
                        }
                        width = (int) rasRect.getWidth();
                    }
                }
            }
            if (underline) {
                height++;
            }
            rasBounds = new Rectangle2D.Double(0, lm.getAscent() - lm.getLeading(), width, height);

            Point2D anchorPoint = getTextCorner(width, height, style, probableBoxedBounds, rotation);
            if (rotation == 1 || rotation == 3) {
                bounds = new Rectangle2D.Double(anchorPoint.getX(), anchorPoint.getY(), height, width);
            } else {
                bounds = new Rectangle2D.Double(anchorPoint.getX(), anchorPoint.getY(), width, height);
            }
            int textWidth = (int) rasBounds.getWidth();
            int textHeight = (int) rasBounds.getHeight();
            if (style == Poly.Type.TEXTCENT) {
                offX = -textWidth / 2;
                offY = -textHeight / 2;
            } else if (style == Poly.Type.TEXTTOP) {
                offX = -textWidth / 2;
            } else if (style == Poly.Type.TEXTBOT) {
                offX = -textWidth / 2;
                offY = -textHeight;
            } else if (style == Poly.Type.TEXTLEFT) {
                offY = -textHeight / 2;
            } else if (style == Poly.Type.TEXTRIGHT) {
                offX = -textWidth;
                offY = -textHeight / 2;
            } else if (style == Poly.Type.TEXTTOPLEFT) {
            } else if (style == Poly.Type.TEXTBOTLEFT) {
                offY = -textHeight;
            } else if (style == Poly.Type.TEXTTOPRIGHT) {
                offX = -textWidth;
            } else if (style == Poly.Type.TEXTBOTRIGHT) {
                offX = -textWidth;
                offY = -textHeight;
            }
            if (style == Poly.Type.TEXTBOX) {
                offX = -textWidth / 2;
                offY = -textHeight / 2;
            }
            return true;
        }

        private void draw(Graphics2D g, LayerVisibility lv) {
            if (!isDrawn(g, lv)) {
                return;
            }
            int width = (int) rasBounds.getWidth();
            int height = (int) rasBounds.getHeight();
            if (rotation == 0) {
                int atX = (int) rect.getCenterX() + offX;
                int atY = (int) rect.getCenterY() + offY;
                g.drawGlyphVector(gv, (float) (atX - rasBounds.getX()), atY + (lm.getAscent() - lm.getLeading()));
                if (underline) {
                    g.drawLine(atX, atY + height - 1, atX + width - 1, atY + height - 1);
                }
            } else {
                AffineTransform saveAT = g.getTransform();
                g.translate(rect.getCenterX(), rect.getCenterY());
                g.rotate(-rotation * Math.PI / 2);
                g.drawGlyphVector(gv, (float) (offX - rasBounds.getX()), offY + (lm.getAscent() - lm.getLeading()));
                if (underline) {
                    g.drawLine(offX, offY + height - 1, offX + width - 1, offY + height - 1);
                }
                g.setTransform(saveAT);
            }
        }
    }

    private class GreekTextInfo extends TextInfo {

        int lX, hX, lY, hY;

        private GreekTextInfo(int lX, int hX, int lY, int hY, Color color, PrimitiveNode baseNode) {
            super(color, baseNode);
            this.lX = lX;
            this.hX = hX;
            this.lY = lY;
            this.hY = hY;
        }

        private void draw(Graphics2D g, LayerVisibility lv) {
            if (!isDrawn(g, lv)) {
                return;
            }
            g.drawLine(lX, lY, hX, hY);
        }
    }

    private class CrossTextInfo extends TextInfo {

        int x, y;

        private CrossTextInfo(int x, int y, Color color, PrimitiveNode baseNode) {
            super(color, baseNode);
            this.x = x;
            this.y = y;
        }

        private void draw(Graphics2D g, LayerVisibility lv) {
            if (!isDrawn(g, lv)) {
                return;
            }
            g.drawLine(x - 3, y, x + 3, y);
            g.drawLine(x, y - 3, x, y + 3);
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
    private static Point getTextCorner(int rasterWidth, int rasterHeight, Poly.Type style, Rectangle rect, int rotation) {
        // adjust to place text in the center
        int textWidth = rasterWidth;
        int textHeight = rasterHeight;
        int offX = 0, offY = 0;
        if (style == Poly.Type.TEXTCENT) {
            offX = -textWidth / 2;
            offY = -textHeight / 2;
        } else if (style == Poly.Type.TEXTTOP) {
            offX = -textWidth / 2;
        } else if (style == Poly.Type.TEXTBOT) {
            offX = -textWidth / 2;
            offY = -textHeight;
        } else if (style == Poly.Type.TEXTLEFT) {
            offY = -textHeight / 2;
        } else if (style == Poly.Type.TEXTRIGHT) {
            offX = -textWidth;
            offY = -textHeight / 2;
        } else if (style == Poly.Type.TEXTTOPLEFT) {
        } else if (style == Poly.Type.TEXTBOTLEFT) {
            offY = -textHeight;
        } else if (style == Poly.Type.TEXTTOPRIGHT) {
            offX = -textWidth;
        } else if (style == Poly.Type.TEXTBOTRIGHT) {
            offX = -textWidth;
            offY = -textHeight;
        }
        if (style == Poly.Type.TEXTBOX) {
            offX = -textWidth / 2;
            offY = -textHeight / 2;
        }
        if (rotation != 0) {
            int saveOffX = offX;
            switch (rotation) {
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
        int cX = (int) rect.getCenterX() + offX;
        int cY = (int) rect.getCenterY() + offY;
        return new Point(cX, cY);
    }

    public static Font getFont(String msg, String font, int tSize, boolean italic, boolean bold, boolean underline) {
        // get the font
        int fontStyle = Font.PLAIN;
        if (italic) {
            fontStyle |= Font.ITALIC;
        }
        if (bold) {
            fontStyle |= Font.BOLD;
        }
        Font theFont = new Font(font, fontStyle, tSize);
        if (theFont == null) {
            System.out.println("Could not find font " + font + " to render text: " + msg);
            return null;
        }
        return theFont;
    }

    // ************************************* CIRCLE DRAWING *************************************
    /**
     * Method to draw a circle on the off-screen buffer
     */
    void drawCircle(Point center, Point edge, ERaster raster) {
        // get parameters
        int radius = (int) center.distance(edge);

        // set redraw area
        int left = center.x - radius;
        int right = center.x + radius + 1;
        int top = center.y - radius;
        int bottom = center.y + radius + 1;

        int x = 0;
        int y = radius;
        int d = 3 - 2 * radius;
        if (left >= 0 && right < sz.width && top >= 0 && bottom < sz.height) {
            // no clip version is faster
            while (x <= y) {
                raster.drawPoint(center.x + x, center.y + y);
                raster.drawPoint(center.x - x, center.y + y);
                raster.drawPoint(center.x + x, center.y - y);
                raster.drawPoint(center.x - x, center.y - y);
                raster.drawPoint(center.x + y, center.y + x);
                raster.drawPoint(center.x - y, center.y + x);
                raster.drawPoint(center.x + y, center.y - x);
                raster.drawPoint(center.x - y, center.y - x);

                if (d < 0) {
                    d += 4 * x + 6;
                } else {
                    d += 4 * (x - y) + 10;
                    y--;
                }
                x++;
            }
        } else {
            // clip version
            while (x <= y) {
                int thisy = center.y + y;
                if (thisy >= 0 && thisy < sz.height) {
                    int thisx = center.x + x;
                    if (thisx >= 0 && thisx < sz.width) {
                        raster.drawPoint(thisx, thisy);
                    }
                    thisx = center.x - x;
                    if (thisx >= 0 && thisx < sz.width) {
                        raster.drawPoint(thisx, thisy);
                    }
                }

                thisy = center.y - y;
                if (thisy >= 0 && thisy < sz.height) {
                    int thisx = center.x + x;
                    if (thisx >= 0 && thisx < sz.width) {
                        raster.drawPoint(thisx, thisy);
                    }
                    thisx = center.x - x;
                    if (thisx >= 0 && thisx < sz.width) {
                        raster.drawPoint(thisx, thisy);
                    }
                }

                thisy = center.y + x;
                if (thisy >= 0 && thisy < sz.height) {
                    int thisx = center.x + y;
                    if (thisx >= 0 && thisx < sz.width) {
                        raster.drawPoint(thisx, thisy);
                    }
                    thisx = center.x - y;
                    if (thisx >= 0 && thisx < sz.width) {
                        raster.drawPoint(thisx, thisy);
                    }
                }

                thisy = center.y - x;
                if (thisy >= 0 && thisy < sz.height) {
                    int thisx = center.x + y;
                    if (thisx >= 0 && thisx < sz.width) {
                        raster.drawPoint(thisx, thisy);
                    }
                    thisx = center.x - y;
                    if (thisx >= 0 && thisx < sz.width) {
                        raster.drawPoint(thisx, thisy);
                    }
                }

                if (d < 0) {
                    d += 4 * x + 6;
                } else {
                    d += 4 * (x - y) + 10;
                    y--;
                }
                x++;
            }
        }
    }

    /**
     * Method to draw a thick circle on the off-screen buffer
     */
    void drawThickCircle(Point center, Point edge, ERaster raster) {
        // get parameters
        int radius = (int) center.distance(edge);

        int x = 0;
        int y = radius;
        int d = 3 - 2 * radius;
        while (x <= y) {
            int thisy = center.y + y;
            if (thisy >= 0 && thisy < sz.height) {
                int thisx = center.x + x;
                if (thisx >= 0 && thisx < sz.width) {
                    drawThickPoint(thisx, thisy, raster);
                }
                thisx = center.x - x;
                if (thisx >= 0 && thisx < sz.width) {
                    drawThickPoint(thisx, thisy, raster);
                }
            }

            thisy = center.y - y;
            if (thisy >= 0 && thisy < sz.height) {
                int thisx = center.x + x;
                if (thisx >= 0 && thisx < sz.width) {
                    drawThickPoint(thisx, thisy, raster);
                }
                thisx = center.x - x;
                if (thisx >= 0 && thisx < sz.width) {
                    drawThickPoint(thisx, thisy, raster);
                }
            }

            thisy = center.y + x;
            if (thisy >= 0 && thisy < sz.height) {
                int thisx = center.x + y;
                if (thisx >= 0 && thisx < sz.width) {
                    drawThickPoint(thisx, thisy, raster);
                }
                thisx = center.x - y;
                if (thisx >= 0 && thisx < sz.width) {
                    drawThickPoint(thisx, thisy, raster);
                }
            }

            thisy = center.y - x;
            if (thisy >= 0 && thisy < sz.height) {
                int thisx = center.x + y;
                if (thisx >= 0 && thisx < sz.width) {
                    drawThickPoint(thisx, thisy, raster);
                }
                thisx = center.x - y;
                if (thisx >= 0 && thisx < sz.width) {
                    drawThickPoint(thisx, thisy, raster);
                }
            }

            if (d < 0) {
                d += 4 * x + 6;
            } else {
                d += 4 * (x - y) + 10;
                y--;
            }
            x++;
        }
    }

    // ************************************* DISC DRAWING *************************************
    /**
     * Method to draw a scan line of the filled-in circle of radius "radius"
     */
    private void drawDiscRow(int thisy, int startx, int endx, ERaster raster) {
        if (thisy < clipLY || thisy > clipHY) {
            return;
        }
        if (startx < clipLX) {
            startx = clipLX;
        }
        if (endx > clipHX) {
            endx = clipHX;
        }
        if (startx > endx) {
            return;
        }
        raster.fillHorLine(thisy, startx, endx);
    }

    /**
     * Method to draw a filled-in circle of radius "radius" on the off-screen buffer
     */
    void drawDisc(Point center, Point edge, ERaster raster) {
        // get parameters
        int radius = (int) center.distance(edge);
        EGraphics.Outline o = raster.getOutline();
        if (o != null) {
            drawCircle(center, edge, raster);
        }

        // set redraw area
        int left = center.x - radius;
        int right = center.x + radius + 1;
        int top = center.y - radius;
        int bottom = center.y + radius + 1;

        if (radius == 1) {
            // just fill the area for discs this small
            if (left < 0) {
                left = 0;
            }
            if (right >= sz.width) {
                right = sz.width - 1;
            }
            for (int y = top; y < bottom; y++) {
                if (y < 0 || y >= sz.height) {
                    continue;
                }
                for (int x = left; x < right; x++) {
                    raster.drawPoint(x, y);
                }
            }
            return;
        }

        int x = 0;
        int y = radius;
        int d = 3 - 2 * radius;
        while (x <= y) {
            drawDiscRow(center.y + y, center.x - x, center.x + x, raster);
            drawDiscRow(center.y - y, center.x - x, center.x + x, raster);
            drawDiscRow(center.y + x, center.x - y, center.x + y, raster);
            drawDiscRow(center.y - x, center.x - y, center.x + y, raster);

            if (d < 0) {
                d += 4 * x + 6;
            } else {
                d += 4 * (x - y) + 10;
                y--;
            }
            x++;
        }
    }
    // ************************************* ARC DRAWING *************************************
    private boolean[] arcOctTable = new boolean[9];
    private Point arcCenter;
    private int arcRadius;
    private ERaster arcRaster;
    private boolean arcThick;

    private int arcFindOctant(int x, int y) {
        if (x > 0) {
            if (y >= 0) {
                if (y >= x) {
                    return 7;
                }
                return 8;
            }
            if (x >= -y) {
                return 1;
            }
            return 2;
        }
        if (y > 0) {
            if (y > -x) {
                return 6;
            }
            return 5;
        }
        if (y > x) {
            return 4;
        }
        return 3;
    }

    private Point arcXformOctant(int x, int y, int oct) {
        switch (oct) {
            case 1:
                return new Point(-y, x);
            case 2:
                return new Point(x, -y);
            case 3:
                return new Point(-x, -y);
            case 4:
                return new Point(-y, -x);
            case 5:
                return new Point(y, -x);
            case 6:
                return new Point(-x, y);
            case 7:
                return new Point(x, y);
            case 8:
                return new Point(y, x);
        }
        return null;
    }

    private void arcDoPixel(int x, int y) {
        if (x < clipLX || x > clipHX || y < clipLY || y > clipHY) {
            return;
        }
        if (arcThick) {
            drawThickPoint(x, y, arcRaster);
        } else {
            arcRaster.drawPoint(x, y);
        }
    }

    private void arcOutXform(int x, int y) {
        if (arcOctTable[1]) {
            arcDoPixel(y + arcCenter.x, -x + arcCenter.y);
        }
        if (arcOctTable[2]) {
            arcDoPixel(x + arcCenter.x, -y + arcCenter.y);
        }
        if (arcOctTable[3]) {
            arcDoPixel(-x + arcCenter.x, -y + arcCenter.y);
        }
        if (arcOctTable[4]) {
            arcDoPixel(-y + arcCenter.x, -x + arcCenter.y);
        }
        if (arcOctTable[5]) {
            arcDoPixel(-y + arcCenter.x, x + arcCenter.y);
        }
        if (arcOctTable[6]) {
            arcDoPixel(-x + arcCenter.x, y + arcCenter.y);
        }
        if (arcOctTable[7]) {
            arcDoPixel(x + arcCenter.x, y + arcCenter.y);
        }
        if (arcOctTable[8]) {
            arcDoPixel(y + arcCenter.x, x + arcCenter.y);
        }
    }

    private void arcBresCW(Point pt, Point pt1) {
        int d = 3 - 2 * pt.y + 4 * pt.x;
        while (pt.x < pt1.x && pt.y > pt1.y) {
            arcOutXform(pt.x, pt.y);
            if (d < 0) {
                d += 4 * pt.x + 6;
            } else {
                d += 4 * (pt.x - pt.y) + 10;
                pt.y--;
            }
            pt.x++;
        }

        // get to the end
        for (; pt.x < pt1.x; pt.x++) {
            arcOutXform(pt.x, pt.y);
        }
        for (; pt.y > pt1.y; pt.y--) {
            arcOutXform(pt.x, pt.y);
        }
        arcOutXform(pt1.x, pt1.y);
    }

    private void arcBresMidCW(Point pt) {
        int d = 3 - 2 * pt.y + 4 * pt.x;
        while (pt.x < pt.y) {
            arcOutXform(pt.x, pt.y);
            if (d < 0) {
                d += 4 * pt.x + 6;
            } else {
                d += 4 * (pt.x - pt.y) + 10;
                pt.y--;
            }
            pt.x++;
        }
        if (pt.x == pt.y) {
            arcOutXform(pt.x, pt.y);
        }
    }

    private void arcBresMidCCW(Point pt) {
        int d = 3 + 2 * pt.y - 4 * pt.x;
        while (pt.x > 0) {
            arcOutXform(pt.x, pt.y);
            if (d > 0) {
                d += 6 - 4 * pt.x;
            } else {
                d += 4 * (pt.y - pt.x) + 10;
                pt.y++;
            }
            pt.x--;
        }
        arcOutXform(0, arcRadius);
    }

    private void arcBresCCW(Point pt, Point pt1) {
        int d = 3 + 2 * pt.y + 4 * pt.x;
        while (pt.x > pt1.x && pt.y < pt1.y) {
            // not always correct
            arcOutXform(pt.x, pt.y);
            if (d > 0) {
                d += 6 - 4 * pt.x;
            } else {
                d += 4 * (pt.y - pt.x) + 10;
                pt.y++;
            }
            pt.x--;
        }

        // get to the end
        for (; pt.x > pt1.x; pt.x--) {
            arcOutXform(pt.x, pt.y);
        }
        for (; pt.y < pt1.y; pt.y++) {
            arcOutXform(pt.x, pt.y);
        }
        arcOutXform(pt1.x, pt1.y);
    }

    /**
     * draws an arc centered at (centerx, centery), clockwise,
     * passing by (x1,y1) and (x2,y2)
     */
    void drawCircleArc(Point center, Point p1, Point p2, boolean thick, ERaster raster) {
        // ignore tiny arcs
        if (p1.x == p2.x && p1.y == p2.y) {
            return;
        }

        // get parameters
        arcRaster = raster;

        arcCenter = center;
        int pa_x = p2.x - arcCenter.x;
        int pa_y = p2.y - arcCenter.y;
        int pb_x = p1.x - arcCenter.x;
        int pb_y = p1.y - arcCenter.y;
        arcRadius = (int) arcCenter.distance(p2);
        int alternate = (int) arcCenter.distance(p1);
        int start_oct = arcFindOctant(pa_x, pa_y);
        int end_oct = arcFindOctant(pb_x, pb_y);
        arcThick = thick;

        // move the point
        if (arcRadius != alternate) {
            int diff = arcRadius - alternate;
            switch (end_oct) {
                case 6:
                case 7: /*  y >  x */ pb_y += diff;
                    break;
                case 8: /*  x >  y */
                case 1: /*  x > -y */ pb_x += diff;
                    break;
                case 2: /* -y >  x */
                case 3: /* -y > -x */ pb_y -= diff;
                    break;
                case 4: /* -y < -x */
                case 5: /*  y < -x */ pb_x -= diff;
                    break;
            }
        }

        for (int i = 1; i < 9; i++) {
            arcOctTable[i] = false;
        }

        if (start_oct == end_oct) {
            arcOctTable[start_oct] = true;
            Point pa = arcXformOctant(pa_x, pa_y, start_oct);
            Point pb = arcXformOctant(pb_x, pb_y, start_oct);

            if ((start_oct & 1) != 0) {
                arcBresCW(pa, pb);
            } else {
                arcBresCCW(pa, pb);
            }
            arcOctTable[start_oct] = false;
        } else {
            arcOctTable[start_oct] = true;
            Point pt = arcXformOctant(pa_x, pa_y, start_oct);
            if ((start_oct & 1) != 0) {
                arcBresMidCW(pt);
            } else {
                arcBresMidCCW(pt);
            }
            arcOctTable[start_oct] = false;

            arcOctTable[end_oct] = true;
            pt = arcXformOctant(pb_x, pb_y, end_oct);
            if ((end_oct & 1) != 0) {
                arcBresMidCCW(pt);
            } else {
                arcBresMidCW(pt);
            }
            arcOctTable[end_oct] = false;

            if (MODP(start_oct + 1) != end_oct) {
                if (MODP(start_oct + 1) == MODM(end_oct - 1)) {
                    arcOctTable[MODP(start_oct + 1)] = true;
                } else {
                    for (int i = MODP(start_oct + 1); i != end_oct; i = MODP(i + 1)) {
                        arcOctTable[i] = true;
                    }
                }
                arcBresMidCW(new Point(0, arcRadius));
            }
        }
    }

    private int MODM(int x) {
        return (x < 1) ? x + 8 : x;
    }

    private int MODP(int x) {
        return (x > 8) ? x - 8 : x;
    }

    // ************************************* RENDERING SUPPORT *************************************
//    private void drawPoint(int x, int y, byte[] layerBitMap, byte layerBitMask) {
//        layerBitMap[y * width + x] |= layerBitMask;
//    }

    private void drawThickPoint(int x, int y, ERaster raster) {
        raster.drawPoint(x, y);
        if (x > clipLX) {
            raster.drawPoint(x - 1, y);
        }
        if (x < clipHX) {
            raster.drawPoint(x + 1, y);
        }
        if (y > clipLY) {
            raster.drawPoint(x, y - 1);
        }
        if (y < sz.height - 1) {
            raster.drawPoint(x, y + 1);
        }
    }

//    private void drawThickPoint(int x, int y, byte[] layerBitMap, byte layerBitMask) {
//        int baseIndex = y * sz.width + x;
//        layerBitMap[baseIndex] |= layerBitMask;
//        if (x > clipLX) {
//            layerBitMap[baseIndex - 1] |= layerBitMask;
//        }
//        if (x < clipHX) {
//            layerBitMap[baseIndex + 1] |= layerBitMask;
//        }
//        if (y > clipLY) {
//            layerBitMap[baseIndex - width] |= layerBitMask;
//        }
//        if (y < sz.height - 1) {
//            layerBitMap[baseIndex + width] |= layerBitMask;
//        }
//    }

    /**
     * Method to convert a database coordinate to screen coordinates.
     * @param dbX the X coordinate (in database units).
     * @param dbY the Y coordinate (in database units).
     * @param result the Point in which to store the screen coordinates.
     */
    private void databaseToScreen(double dbX, double dbY, Point result) {
        double scrX = originX + dbX * scale;
        double scrY = originY - dbY * scale;
        result.x = (int) (scrX >= 0 ? scrX + 0.5 : scrX - 0.5);
        result.y = (int) (scrY >= 0 ? scrY + 0.5 : scrY - 0.5);
    }

    private void screenToDatabase(int x, int y, Point2D result) {
        result.setLocation((x - originX) / scale, (originY - y) / scale);
    }

    /**
     * Method to convert a database rectangle to screen coordinates.
     * @param db the rectangle (in database units).
     * @return the rectangle on the screen.
     */
    private Rectangle databaseToScreen(Rectangle2D db) {
        Point llPt = tempPt1;
        Point urPt = tempPt2;
        databaseToScreen(db.getMinX(), db.getMinY(), llPt);
        databaseToScreen(db.getMaxX(), db.getMaxY(), urPt);
        int screenLX = llPt.x;
        int screenHX = urPt.x;
        int screenLY = llPt.y;
        int screenHY = urPt.y;
        if (screenHX < screenLX) {
            int swap = screenHX;
            screenHX = screenLX;
            screenLX = swap;
        }
        if (screenHY < screenLY) {
            int swap = screenHY;
            screenHY = screenLY;
            screenLY = swap;
        }
        return new Rectangle(screenLX, screenLY, screenHX - screenLX + 1, screenHY - screenLY + 1);
    }
}
