/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: J3DAppearance.java
 * Written by Gilda Garreton, Sun Microsystems.
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

package com.sun.electric.plugins.j3d.utils;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.text.Pref;
import com.sun.electric.technology.Layer;

import javax.media.j3d.*;
import javax.vecmath.Color3f;
import java.awt.*;
import java.util.HashMap;
import java.util.Observer;
import java.util.Observable;

/**
 * Support class for 3D viewing.
 */
public class J3DAppearance extends Appearance
        implements Observer
{
    private static final HashMap<Layer,Pref> graphics3DTransModePrefs = new HashMap<Layer,Pref>(); // NONE is the default
    private static final HashMap<Layer,Pref> graphics3DTransFactorPrefs = new HashMap<Layer,Pref>(); // 0 is the default
    private static final int JAPP_DEFAULT_MODE = TransparencyAttributes.NONE;
    private static final float JAPP_DEFAULT_FACTOR = 0.2f;

    private EGraphics graphics; // reference to layer for fast access to appearance

	/** cell has a unique appearance **/    public static J3DAppearance cellApp;
    /** highligh appearance **/             public static J3DAppearance highligtApp;
    /** Appearance for axes */              public static J3DAppearance[] axisApps = new J3DAppearance[3];

    public J3DAppearance(J3DAppearance app)
    {
        super();
        if (app == null) throw new Error("Input appearance is null");
        this.graphics = app.graphics;
        if (graphics != null) graphics.addObserver(this);
        TransparencyAttributes oldTa = app.getTransparencyAttributes();
        setOtherAppearanceValues(oldTa.getTransparencyMode(), oldTa.getTransparency(), graphics.getColor());
    }

    private J3DAppearance(EGraphics graphics, int mode, float factor, Color color)
    {
        super();
        this.graphics = graphics;
        if (graphics != null) graphics.addObserver(this);
        setOtherAppearanceValues(mode, factor, color); //graphics.getColor());
    }
    public void setGraphics(EGraphics graphics)
    {
        this.graphics = graphics;
        if (graphics != null) graphics.addObserver(this);
    }
    public EGraphics getGraphics() { return graphics;}

    private void setOtherAppearanceValues(int mode, float factor, Color color)
    {
        // Transparency values
        TransparencyAttributes ta = new TransparencyAttributes(mode, factor);
        ta.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
        ta.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
        ta.setCapability(TransparencyAttributes.ALLOW_MODE_READ);
        ta.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
        setTransparencyAttributes(ta);

        setCapability(ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
        setCapability(ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
        // For highlight
        setCapability(ALLOW_MATERIAL_READ);
        setCapability(ALLOW_MATERIAL_WRITE);
        // For visibility
        setCapability(ALLOW_RENDERING_ATTRIBUTES_READ);
        setCapability(ALLOW_RENDERING_ATTRIBUTES_WRITE);

        // Adding Rendering attributes to access visibility flag if layer is available
        if (graphics != null)
        {
            RenderingAttributes ra = new RenderingAttributes();
            ra.setCapability(RenderingAttributes.ALLOW_VISIBLE_READ);
            ra.setCapability(RenderingAttributes.ALLOW_VISIBLE_WRITE);
            ra.setCapability(RenderingAttributes.ALLOW_DEPTH_ENABLE_READ);
            ra.setCapability(RenderingAttributes.ALLOW_DEPTH_ENABLE_WRITE);
            ra.setVisible(graphics.getLayer().isVisible());
            setRenderingAttributes(ra);
            if (mode != TransparencyAttributes.NONE)
                ra.setDepthBufferEnable(true);
        }

        // Set up the polygon attributes
        //PolygonAttributes pa = new PolygonAttributes();
        //pa.setCullFace(PolygonAttributes.CULL_NONE);
        //pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
        //setPolygonAttributes(pa);

        //TextureAttributes texAttr = new TextureAttributes();
        //texAttr.setTextureMode(TextureAttributes.MODULATE);
        //texAttr.setTextureColorTable(pattern);
        //setTextureAttributes(texAttr);

        //LineAttributes lineAttr = new LineAttributes();
        //lineAttr.setLineAntialiasingEnable(true);
        //setLineAttributes(lineAttr);

        // Adding to internal material
//				Material mat = new Material(objColor, black, objColor, white, 70.0f);
        if (color != null)
        {
            Color3f objColor = new Color3f(color);
            // Emissive is black and specular is plastic!
            //Color3f specular = new Color3f(color.brighter());
            Material mat = new Material(objColor, J3DUtils.black, objColor, J3DUtils.plastic/*J3DUtils.white*/, 17);
            mat.setLightingEnable(true);
            mat.setCapability(Material.ALLOW_COMPONENT_READ);
            mat.setCapability(Material.ALLOW_COMPONENT_WRITE);
            mat.setCapability(Material.AMBIENT_AND_DIFFUSE);
            setMaterial(mat);
        }
    }

    /**
     * Set visibility of appearance assigned to cell bounding box
     * @param value
     */
    public static void setCellVisibility(boolean value)
    {
        cellApp.getRenderingAttributes().setVisible(value);
    }

    /**
     * Method to get 3D appearance stored in EGraphics class. It will create
     * object if doesn't exist.
     * @param graphics
     * @return
     */
    public static J3DAppearance getAppearance(EGraphics graphics)
    {
        // Setting appearance
        J3DAppearance ap = (J3DAppearance)graphics.get3DAppearance();

        if (ap == null)
        {
            int mode = graphics.getLayer().getIntegerPref("3DTransparencyMode",
                    graphics3DTransModePrefs, JAPP_DEFAULT_MODE).getInt();
            float factor = (float)graphics.getLayer().getDoublePref("3DTransparencyFactor", graphics3DTransFactorPrefs, JAPP_DEFAULT_FACTOR).getDouble();
            ap = new J3DAppearance(graphics, mode, factor, graphics.getColor());

            graphics.set3DAppearance(ap);
        }
        return (ap);
    }

    public void setTransparencyAndRenderingAttributes(TransparencyAttributes transparencyAttributes, boolean rendering)
    {
        super.setTransparencyAttributes(transparencyAttributes);
        super.getRenderingAttributes().setDepthBufferEnable(rendering);
        int mode = transparencyAttributes.getTransparencyMode();
        graphics.getLayer().getIntegerPref("3DTransparencyMode",
                    graphics3DTransModePrefs, mode).setInt(mode);
        double factor = transparencyAttributes.getTransparency();
        graphics.getLayer().getDoublePref("3DTransparencyFactor", graphics3DTransFactorPrefs, factor).setDouble(factor);
    }

    /********************************************************************************************************
     *                  Model-View paradigm to control refresh from 2D
     ********************************************************************************************************/
    /**
     * Observer method to update 3D appearance if 2D
     * @param o
     * @param arg
     */
    public void update(Observable o, Object arg)
    {
        if (arg instanceof Boolean)
            set3DVisibility((Boolean)arg);
    }

    /**
	 * Method to set visibility in Appearance objects from external tools
	 * @param visible true if visibility is on
	 */
	private void set3DVisibility(Boolean visible)
	{
        if (getRenderingAttributes() == null || visible == null)
            System.out.println("Error in J3DAppearance.set3DVisibility");
		else
            getRenderingAttributes().setVisible(visible.booleanValue());
	}

    /**
     * Method to set color in 3D. Since it must set 3 different colors, it
     * is a function called by setOtherAppearanceValues.
     * @param material material to change if available
     * @param color
     */
    public void set3DColor(Object material, Color color)
    {
        Material mat = (material == null) ? getMaterial() : (Material)material;

        Color3f objColor = new Color3f(color);
        mat.setDiffuseColor(objColor);
        //mat.setSpecularColor(objColor);
        mat.setAmbientColor(objColor);
        if (getColoringAttributes() != null)
            getColoringAttributes().setColor(objColor);
    }

    /**
     * Method to access appearance of axes in 3D
     * @param initValue false if appearance has to be changed according to user value
     */
    public static void setAxisAppearanceValues(Object initValue)
    {
        double[] colors = GenMath.transformVectorIntoValues(J3DUtils.get3DColorAxes());

        for (int i = 0; i < axisApps.length; i++)
        {
            Color userColor = new Color((int)colors[i]);

            if (axisApps[i] == null)
            {
                axisApps[i] = new J3DAppearance(null, TransparencyAttributes.NONE, 0.5f, userColor);

                // Turn off face culling so we can see the back side of the labels
                // (since we're not using font extrusion)
                PolygonAttributes polygonAttributes = new PolygonAttributes();
                polygonAttributes.setCullFace(PolygonAttributes.CULL_NONE);

                // Make the axis lines 2 pixels wide
                LineAttributes lineAttributes = new LineAttributes();
                lineAttributes.setLineWidth(3.0f);

                ColoringAttributes colorAttrib = new ColoringAttributes();
                colorAttrib.setColor(new Color3f(userColor));
                colorAttrib.setCapability(ColoringAttributes.ALLOW_COLOR_READ);
                colorAttrib.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
                axisApps[i].setColoringAttributes(colorAttrib);
                axisApps[i].setPolygonAttributes(polygonAttributes);
                axisApps[i].setLineAttributes(lineAttributes);
                axisApps[i].setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
                axisApps[i].setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);

                RenderingAttributes ra = new RenderingAttributes();
                ra.setCapability(RenderingAttributes.ALLOW_VISIBLE_READ);
                ra.setCapability(RenderingAttributes.ALLOW_VISIBLE_WRITE);
                ra.setVisible(J3DUtils.is3DAxesOn());
                axisApps[i].setRenderingAttributes(ra);
            }
            else if (initValue == null) // redoing color only when it was changed in GUI
                axisApps[i].set3DColor(null, userColor);
        }
    }

    /**
     * Method to access appearance of highlighted nodes in 3D
     * @param initValue false if appearance has to be changed according to user value
     */
    public static void setHighlightedAppearanceValues(Object initValue)
    {
        Color userColor = new Color(J3DUtils.get3DColorHighlighted());

        if (highligtApp == null)
            highligtApp = new J3DAppearance(null, TransparencyAttributes.BLENDED, 0.5f, userColor);
        else if (initValue == null) // redoing color only when it was changed in GUI
            highligtApp.set3DColor(null, userColor);
    }

    /**
     * Method to access appearance for cells in 3D
     * @param initValue no null if appearance has to be changed according to user value. Using
     * this mechanism to avoid the creation of new Boolean() just for the checking
     */
    public static void setCellAppearanceValues(Object initValue)
    {
        Color3f userColor = new Color3f(new Color(J3DUtils.get3DColorInstanceCell()));

        if (cellApp == null)
        {
            cellApp = new J3DAppearance(null, TransparencyAttributes.SCREEN_DOOR, 0, null);

            RenderingAttributes ra = new RenderingAttributes();
            ra.setCapability(RenderingAttributes.ALLOW_VISIBLE_READ);
            ra.setCapability(RenderingAttributes.ALLOW_VISIBLE_WRITE);
            ra.setVisible(J3DUtils.is3DCellBndOn());
            cellApp.setRenderingAttributes(ra);

			// Set up the polygon attributes
            PolygonAttributes pa = new PolygonAttributes();
            pa.setCullFace(PolygonAttributes.CULL_NONE);
            pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
            cellApp.setPolygonAttributes(pa);

//            TextureAttributes texAttr = new TextureAttributes();
//            texAttr.setTextureMode(TextureAttributes.MODULATE);
//            //texAttr.setTextureColorTable(pattern);
//            cellApp.setTextureAttributes(texAttr);

            LineAttributes lineAttr = new LineAttributes();
            lineAttr.setLineAntialiasingEnable(true);
            cellApp.setLineAttributes(lineAttr);

            //** Data for cells
            ColoringAttributes ca = new ColoringAttributes();
            ca.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
            ca.setCapability(ColoringAttributes.ALLOW_COLOR_READ);
            ca.setColor(userColor);
            cellApp.setColoringAttributes(ca);
            cellApp.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
            cellApp.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
        }
        else if (initValue == null) // redoing color only when it was changed in GUI
        {
            ColoringAttributes ca = cellApp.getColoringAttributes();
            Color3f curColor = new Color3f();
            ca.getColor(curColor);
            if (!userColor.equals(curColor))
                ca.setColor(userColor);
        }
    }
}
