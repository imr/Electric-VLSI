package com.sun.electric.plugins.j3d;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.text.Pref;
import com.sun.electric.technology.Layer;

import javax.media.j3d.Appearance;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Material;
import javax.vecmath.Color3f;
import java.awt.*;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Feb 18, 2005
 * Time: 1:59:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class JAppearance extends Appearance
{
    private static final HashMap graphics3DTransModePrefs = new HashMap(); // NONE is the default
    private static final HashMap graphics3DTransFactorPrefs = new HashMap(); // 0 is the default

    private EGraphics graphics; // reference to layer for fast access to appearance
    protected int transparencyMode;
    protected float transparencyFactor;

    public JAppearance(EGraphics graphics, int mode, float factor)
    {
        super();
        this.graphics = graphics;
        this.transparencyMode = mode;
        this.transparencyFactor = factor;
    }
    public void seGraphics(EGraphics graphics) {this.graphics = graphics;}
    public EGraphics getGraphics() { return graphics;}

    public static JAppearance getAppearance(Layer layer)
    {
        // Setting appearance
        EGraphics graphics = layer.getGraphics();
        JAppearance ap = (JAppearance)graphics.get3DAppearance();

        if (ap == null)
        {
            int mode = layer.getIntegerPref("3DTransparencyMode",
                    graphics3DTransModePrefs, TransparencyAttributes.NONE).getInt();
            float factor = (float)layer.getDoublePref("3DTransparencyFactor", graphics3DTransFactorPrefs, 0).getDouble();
            ap = new JAppearance(graphics, mode, factor);
            Color color = layer.getGraphics().getColor();
            Color3f objColor = new Color3f(color);
            /*
            ColoringAttributes ca = new ColoringAttributes(objColor, ColoringAttributes.SHADE_GOURAUD);
            ca.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
            ap.setColoringAttributes(ca);
            */

            TransparencyAttributes ta = new TransparencyAttributes();
            ta.setTransparencyMode(mode);
            ta.setTransparency(factor);
            ap.setTransparencyAttributes(ta);

            // Adding Rendering attributes to access visibility flag
            RenderingAttributes ra = new RenderingAttributes();
            ra.setCapability(RenderingAttributes.ALLOW_VISIBLE_READ);
            ra.setCapability(RenderingAttributes.ALLOW_VISIBLE_WRITE);
            ra.setVisible(layer.isVisible());
            ap.setRenderingAttributes(ra);

            // Set up the polygon attributes
            //PolygonAttributes pa = new PolygonAttributes();
            //pa.setCullFace(PolygonAttributes.CULL_NONE);
            //pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
            //ap.setPolygonAttributes(pa);

            //TextureAttributes texAttr = new TextureAttributes();
            //texAttr.setTextureMode(TextureAttributes.MODULATE);
            //texAttr.setTextureColorTable(pattern);
            //ap.setTextureAttributes(texAttr);

            //LineAttributes lineAttr = new LineAttributes();
            //lineAttr.setLineAntialiasingEnable(true);
            //ap.setLineAttributes(lineAttr);

            // Adding to internal material
//				Material mat = new Material(objColor, black, objColor, white, 70.0f);
            Material mat = new Material();
            mat.setDiffuseColor(objColor);
            mat.setSpecularColor(objColor);
            mat.setAmbientColor(objColor);
            mat.setLightingEnable(true);
            mat.setCapability(Material.ALLOW_COMPONENT_READ);
            mat.setCapability(Material.ALLOW_COMPONENT_WRITE);
            ap.setMaterial(mat);

            // For changing color
            //ap.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
            //ap.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
            ap.setCapability(ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
            ap.setCapability(ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
            // For highlight
            ap.setCapability(ALLOW_MATERIAL_READ);
            ap.setCapability(ALLOW_MATERIAL_WRITE);
            // For visibility
            ap.setCapability(ALLOW_RENDERING_ATTRIBUTES_READ);
            ap.setCapability(ALLOW_RENDERING_ATTRIBUTES_WRITE);

            graphics.set3DAppearance(ap);
        }
        return (ap);
    }

    /**
	 * Method to set color in Appearance objects from external tools
	 * @param obj Appearance object
	 * @param color new color to setup
	 */
	public static void set3DColor(Object obj, Color color)
	{
		JAppearance app = (JAppearance)obj;
		Color3f color3D = new Color3f(color);
		Material mat = app.getMaterial();
		mat.setAmbientColor(color3D);
		mat.setDiffuseColor(color3D);
	}

    /**
	 * Method to set visibility in Appearance objects from external tools
	 * @param obj Appearance object
	 * @param visible true if visibility is on
	 */
	public static void set3DVisibility(Object obj, Boolean visible)
	{
		JAppearance app = (JAppearance)obj;
		app.getRenderingAttributes().setVisible(visible.booleanValue());
	}
}
