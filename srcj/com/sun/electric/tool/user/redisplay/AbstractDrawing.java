/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AbstractDrawing.java
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.user.GraphicsPreferences;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.LayerVisibility;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Base class for redisplay algorithms
 */
public abstract class AbstractDrawing {

    public final EditWindow wnd;
    public WindowFrame.DisplayAttributes da;

    protected AbstractDrawing(EditWindow wnd) {
        this.wnd = wnd;
    }

    public static AbstractDrawing createDrawing(EditWindow wnd, AbstractDrawing drawing, Cell cell) {
        boolean isLayerDrawing = User.getDisplayAlgorithm() == 2 && cell != null && cell.getTechnology().isLayout();
        if (isLayerDrawing) {
            drawing = new LayerDrawing.Drawing(wnd);
        } else {
            drawing = new PixelDrawing.Drawing(wnd);
        }
        return drawing;
    }

    public abstract boolean paintComponent(Graphics2D g, LayerVisibility lv, Dimension sz);

    public abstract void render(Dimension sz, WindowFrame.DisplayAttributes da, GraphicsPreferences gp, DrawingPreferences dp, boolean fullInstantiate, Rectangle2D bounds);

    public void abortRendering() {
    }

    public void opacityChanged() {
    }

    /**
     * Notifies about visibility change
     * Retruns true if full repaint is necessary
     */
    public boolean visibilityChanged() {
        return true;
    }

    public boolean hasOpacity() {
        return false;
    }

    public void testJogl() {
    }

    /**
     * Method to clear the cache of expanded subcells.
     * This is used by layer visibility which, when changed, causes everything to be redrawn.
     */
    public static void clearSubCellCache(boolean layerAlso) {
        PixelDrawing.clearSubCellCache();
        if (layerAlso) {
            LayerDrawing.clearSubCellCache();
        }
    }

    public static void forceRedraw(Cell cell) {
        PixelDrawing.forceRedraw(cell);
        LayerDrawing.forceRedraw(cell);
    }

    /**
     * Method to draw polygon "poly", transformed through "trans".
     */
    public static void drawShapes(Graphics2D g, GraphicsPreferences gp, int imgX, int imgY, double scale, VectorCache.VectorBase[] shapes,
            PixelDrawing offscreen, Rectangle entryRect) {
        if (User.getDisplayAlgorithm() < 2 || User.isLegacyComposite()) {
            PixelDrawing.gp = gp;
            offscreen.initDrawing(scale);
            VectorDrawing vd = new VectorDrawing(false);
            vd.render(offscreen, scale, EPoint.ORIGIN, shapes);
            Image img = offscreen.composite(null);
            g.drawImage(img, imgX, imgY, null);
        } else {
            LayerDrawing.drawTechPalette(g, gp, imgX, imgY, entryRect, scale, shapes);
        }
    }

    public static class LayerColor {

        public final Layer layer;
        // nextRgb = inverseAlpha*prevRgb + premultipliedRgb
        public final float premultipliedRed;
        public final float premultipliedGreen;
        public final float premultipliedBlue;
        public final float inverseAlpha;

        public LayerColor(Layer layer, float premultipliedRed, float premultipliedGreen, float premultipliedBlue, float inverseAlpha) {
            this.layer = layer;
            this.premultipliedRed = premultipliedRed;
            this.premultipliedGreen = premultipliedGreen;
            this.premultipliedBlue = premultipliedBlue;
            this.inverseAlpha = inverseAlpha;
        }

        public LayerColor(Color color) {
            layer = null;
            float[] compArray = color.getRGBColorComponents(null);
            premultipliedRed = compArray[0];
            premultipliedGreen = compArray[1];
            premultipliedBlue = compArray[2];
            inverseAlpha = 0;
        }
    }

    public static class DrawingPreferences {

        boolean gridAxesShown = User.isGridAxesShown();
        double gridXBoldFrequency = User.getDefGridXBoldFrequency();
        double gridYBoldFrequency = User.getDefGridYBoldFrequency();
        double globalTextScale = User.getGlobalTextScale();
    }
}
