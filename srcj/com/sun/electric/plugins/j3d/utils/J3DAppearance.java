package com.sun.electric.plugins.j3d.utils;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.user.dialogs.ColorPatternPanel;
import com.sun.electric.tool.user.User;
import com.sun.electric.plugins.j3d.utils.J3DUtils;
import com.sun.electric.plugins.j3d.View3DWindow;

import javax.media.j3d.*;
import javax.vecmath.Color3f;
import java.awt.*;
import java.util.HashMap;
import java.util.Observer;
import java.util.Observable;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Feb 18, 2005
 * Time: 1:59:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class J3DAppearance extends Appearance
        implements Observer
{
    private static final HashMap graphics3DTransModePrefs = new HashMap(); // NONE is the default
    private static final HashMap graphics3DTransFactorPrefs = new HashMap(); // 0 is the default
    private static final int JAPP_DEFAULT_MODE = TransparencyAttributes.NONE;
    private static final float JAPP_DEFAULT_FACTOR = 0.05f;

    private EGraphics graphics; // reference to layer for fast access to appearance

	/** cell has a unique appearance **/    public static J3DAppearance cellApp;
    /** highligh appearance **/             public static J3DAppearance highligtAp;

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

        // For changing color
        //setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
        //setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
        setCapability(ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
        setCapability(ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
        // For highlight
        setCapability(ALLOW_MATERIAL_READ);
        setCapability(ALLOW_MATERIAL_WRITE);
        // For visibility
        setCapability(ALLOW_RENDERING_ATTRIBUTES_READ);
        setCapability(ALLOW_RENDERING_ATTRIBUTES_WRITE);

        // Nothing else to set
//        if (graphics == null) return;
//
//        // Adding color
//        Color color = graphics.getColor();
        //Color3f objColor = new Color3f(color);
        /*
        ColoringAttributes ca = new ColoringAttributes(objColor, ColoringAttributes.SHADE_GOURAUD);
        ca.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
        setColoringAttributes(ca);
        */

        // Adding Rendering attributes to access visibility flag if layer is available
        if (graphics != null)
        {
            RenderingAttributes ra = new RenderingAttributes();
            ra.setCapability(RenderingAttributes.ALLOW_VISIBLE_READ);
            ra.setCapability(RenderingAttributes.ALLOW_VISIBLE_WRITE);
            ra.setVisible(graphics.getLayer().isVisible());
            setRenderingAttributes(ra);
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
            Material mat = new Material();
            mat.setLightingEnable(true);
            mat.setCapability(Material.ALLOW_COMPONENT_READ);
            mat.setCapability(Material.ALLOW_COMPONENT_WRITE);
            setMaterial(mat);
            set3DColor(mat, color);
        }
    }

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
        mat.setSpecularColor(objColor);
        mat.setAmbientColor(objColor);
    }

    /**
     * Method to access appearance of highlighted nodes in 3D
     * @param initValue false if appearance has to be changed according to user value
     */
    public static void setHighlightedAppearanceValues(Object initValue)
    {
        Color userColor = new Color(User.get3DColorHighlighted());

        if (highligtAp == null)
            highligtAp = new J3DAppearance(null, TransparencyAttributes.BLENDED, 0.5f, userColor);
        else if (initValue == null) // redoing color only when it was changed in GUI
            highligtAp.set3DColor(null, userColor);
    }

    /**
     * Method to access appearance for cells in 3D
     * @param initValue no null if appearance has to be changed according to user value. Using
     * this mechanism to avoid the creation of new Boolean() just for the checking
     */
    public static void setCellAppearanceValues(Object initValue)
    {
        Color3f userColor = new Color3f(new Color(User.get3DColorInstanceCell()));

        if (cellApp == null)
        {
            cellApp = new J3DAppearance(null, TransparencyAttributes.SCREEN_DOOR, 0, null);

			// Set up the polygon attributes
            PolygonAttributes pa = new PolygonAttributes();
            pa.setCullFace(PolygonAttributes.CULL_NONE);
            pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
            cellApp.setPolygonAttributes(pa);

            TextureAttributes texAttr = new TextureAttributes();
            texAttr.setTextureMode(TextureAttributes.MODULATE);
            //texAttr.setTextureColorTable(pattern);
            cellApp.setTextureAttributes(texAttr);

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
