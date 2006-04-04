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

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.technologies.MoCMOS;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.ImageCapabilities;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.util.ArrayList;

/**
 *
 */
public class LayerDrawing {

    public Layer layer;
    public LayerCell topCell;
	/** temporary objects (saves allocation) */				private Point tempPt1 = new Point(), tempPt2 = new Point();
    private double scale;
    private double factorX, factorY;
    private PixelDrawing offscreen;
    private EGraphics graphics;
	private byte [][] layerBitMap = null;
    
	public class LayerCell {
        final ArrayList<Rectangle2D.Float> rects = new ArrayList<Rectangle2D.Float>();
		final ArrayList<LayerSubCell> subCells = new ArrayList<LayerSubCell>();

		LayerCell() {
		}
        
        public void addSubCell(LayerCell proto, float offX, float offY) {
            subCells.add(new LayerSubCell(proto, offX, offY));
        }
        
        public void renderPixelDrawing(float oX, float oY) {
            for (LayerSubCell sc: subCells)
                sc.proto.renderPixelDrawing(oX + sc.offX, oY + sc.offY);
            for (Rectangle2D.Float r: rects) {
				databaseToScreen(r.getMinX() + oX, r.getMinY() + oY, tempPt1);
				databaseToScreen(r.getMaxX() + oX, r.getMaxY() + oY, tempPt2);
                int lX = tempPt1.x;
                int lY = tempPt2.y;
                int hX = tempPt2.x;
                int hY = tempPt1.y;
				offscreen.drawBox(lX, hX, lY, hY, layerBitMap, graphics, false);
            }
        }
	}
    
    public class LayerSubCell {
        final LayerCell proto;
        final float offX, offY;
        
        LayerSubCell(LayerCell proto, float offX, float offY) {
            this.proto = proto;
            this.offX = offX;
            this.offY = offY;
        }
    }
    
    /** Creates a new instance of LayerDrawing */
    public LayerDrawing(Layer layer) {
        this.layer = layer;
        graphics = layer.getGraphics();
        System.out.println("Layer" + layer.getIndex() + " " + layer +
                " transpar=" + graphics.getTransparentLayer() + " pattern=" + graphics.isPatternedOnDisplay() + " outline=" + graphics.getOutlined());
    }
    
    public LayerCell newCell() { return new LayerCell(); }
    
    public void draw(EditWindow wnd) {
        GraphicsConfiguration gc = wnd.getGraphicsConfiguration();
        System.out.println("Graphics configuration " + gc);
        offscreen = wnd.getOffscreen();
        offscreen.clearImage(true, null);
        
		scale = (float)wnd.getScale();
        Dimension sz = offscreen.getSize();
		int szHalfWidth = sz.width / 2;
		int szHalfHeight = sz.height / 2;
		Point2D offset = wnd.getOffset();
		float offX = (float)offset.getX();
		float offY = (float)offset.getY();
		factorX = szHalfWidth - offX*scale;
		factorY = szHalfHeight + offY*scale;
        if (graphics != null) {
            int layerNum = graphics.getTransparentLayer() - 1;
            if (layerNum < offscreen.numLayerBitMaps) layerBitMap = offscreen.getLayerBitMap(layerNum);
        }

        long startTime = System.currentTimeMillis();
        topCell.renderPixelDrawing(0, 0);
        long renderTime = System.currentTimeMillis();
		offscreen.composite(null);
        long compositeTime = System.currentTimeMillis();
        wnd.repaint();
        System.out.println("RenderTime=" + (renderTime-startTime) + " CompositeTime=" + (compositeTime - renderTime));
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

    public static void showVectorCache() {
        VectorDrawing.showStatistics(MoCMOS.tech.getLayer(0));
    }
    
    public static void showGraphics() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (GraphicsDevice gd: gs) {
            System.out.println("device " + gd + " has " + gd.getAvailableAcceleratedMemory() + " bytes of accelerated memory");
            System.out.println("Default graphics configuration is " + gd.getDefaultConfiguration());
            GraphicsConfiguration[] gcs = gd.getConfigurations();
            for (GraphicsConfiguration gc: gcs) {
                ImageCapabilities ic = gc.getImageCapabilities();
                ColorModel cm = gc.getColorModel(Transparency.TRANSLUCENT);
                System.out.println("\t" + gc + " accelerated=" + ic.isAccelerated() + " trueVolatile=" + ic.isTrueVolatile() + " " + cm);
//                JFrame f = new JFrame(gd.getDefaultConfiguration());
//                Canvas c = new Canvas(gc[i]);
//                Rectangle gcBounds = gc[i].getBounds();
//                int xoffs = gcBounds.x;
//                int yoffs = gcBounds.y;
//                f.getContentPane().add(c);
//                f.setLocation((i*50)+xoffs, (i*60)+yoffs);
//                f.setVisible(true);
            }
        }
    }
    
}
