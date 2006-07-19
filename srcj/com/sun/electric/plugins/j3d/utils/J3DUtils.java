/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: J3DUtils.java
 * Written by Gilda Garreton, Sun Microsystems.
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
package com.sun.electric.plugins.j3d.utils;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.text.Pref;
import com.sun.electric.plugins.j3d.View3DWindow;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.dialogs.ColorPatternPanel;
import com.sun.electric.tool.user.dialogs.options.LayersTab;
import com.sun.j3d.utils.behaviors.interpolators.KBKeyFrame;
import com.sun.j3d.utils.behaviors.interpolators.TCBKeyFrame;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.picking.PickTool;
import com.sun.j3d.utils.universe.SimpleUniverse;

import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.StringTokenizer;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GeometryUpdater;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Light;
import javax.media.j3d.Node;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

/**
 * Utility class for 3D module
 * @author  Gilda Garreton
 * @version 0.1
 */
public final class J3DUtils
{
    /** standard colors to be used by materials **/         public static final Color3f black = new Color3f(0.0f, 0.0f, 0.0f);
//    /** standard colors to be used by materials **/         public static final Color3f white = new Color3f(1.0f, 1.0f, 1.0f);
    /** standard colors to be used by materials **/         public static final Color3f plastic = new Color3f(0.89f, 0.89f, 0.89f);
    /** Ambiental light color **/                 private static Color3fObservable ambientalColor;
    /** Directional light color **/               private static Color3fObservable directionalColor;
    /** Background color **/                      private static Color3fObservable backgroundColor;
    /** Directional vectors **/              private static Vector3fObservable[] lights = new Vector3fObservable[2]; // = new Vector3f(-1.0f, -1.0f, -1.0f);

    public static final BoundingSphere infiniteBounds = new BoundingSphere(new Point3d(), Double.MAX_VALUE);
    private static Pref cache3DOrigZoom = Pref.makeDoublePref("3DOrigZoom3D", User.getUserTool().prefs, 1);
    private static Pref cache3DRot = Pref.makeStringPref("3DRotation", User.getUserTool().prefs, "(0 0 0)");
    private static Pref cache3DFactor = Pref.makeDoublePref("3DScaleZ", User.getUserTool().prefs, 1.0);
    private static Pref cache3DAntialiasing = Pref.makeBooleanPref("3DAntialiasing", User.getUserTool().prefs, false);
	private static Pref cache3DPerspective = Pref.makeBooleanPref("3DPerspective", User.getUserTool().prefs, true);
    private static Pref cache3DCellBnd = Pref.makeBooleanPref("3DCellBnd", User.getUserTool().prefs, true);
    private static Pref cache3DAxes = Pref.makeBooleanPref("3DAxes", User.getUserTool().prefs, false);
    private static Pref cache3DMaxNumber = Pref.makeIntPref("3DMaxNumNodes", User.getUserTool().prefs, 1000);
    private static Pref cache3DAlpha = Pref.makeIntPref("3DAlpha", User.getUserTool().prefs, 20000);
    private static Pref cache3DColorInstanceCell = Pref.makeIntPref("3DColorInstanceCell", User.getUserTool().prefs, Color.GRAY.getRGB());
    private static Pref cache3DColorHighlighted = Pref.makeIntPref("3DColorHighlighted", User.getUserTool().prefs, Color.GRAY.getRGB());
    private static Pref cache3DColorAmbient = Pref.makeIntPref("3DColorAmbient", User.getUserTool().prefs, Color.GRAY.getRGB());
    private static Pref cache3DColorAxes = Pref.makeStringPref("3DColorAxes", User.getUserTool().prefs,
            "("+Color.RED.getRGB()+" "+ Color.BLUE.getRGB() + " "+ Color.GREEN.getRGB() + ")");
    private static Pref cache3DColorDirectionalLight = Pref.makeIntPref("3DColorDirectionalLight", User.getUserTool().prefs, Color.GRAY.getRGB());
    private static Pref cache3DLightDirs = Pref.makeStringPref("3DLightDirs", User.getUserTool().prefs, "(-1 1 -1)(1 -1 -1)");

    public static J3DAlpha jAlpha = null;
    // For reading data files (capacitance)
    private static final int VALUES_PER_LINE = 11;
    private static double[] lastValidValues = new double[VALUES_PER_LINE];

	/**
	 * Method to get the color of directional light on the 3D display.
	 * The default is "gray".
	 * @return the color of directional light on the 3D display.
	 */
	public static int get3DColorDirectionalLight() { return cache3DColorDirectionalLight.getInt(); }
	/**
	 * Method to set the color of directional light on the 3D display.
	 * @param c the color of directional light on the 3D display.
	 */
	public static void set3DColorDirectionalLight(int c)
    {
        cache3DColorDirectionalLight.setInt(c);
        setDirectionalColor(null);
    }

    /**
	 * Method to return the first light direction.
	 * The default is the X axis.
	 * @return the company name to use in schematic frames.
	 */
	public static String get3DLightDirs() { return cache3DLightDirs.getString(); }
	/**
	 * Method to set the first light direction.
     * It is stored as string
	 * @param c the company name to use in schematic frames.
	 */
	public static void set3DLightDirs(String c)
    {
        cache3DLightDirs.setString(c);
        setDirections(null);
    }

	/**
	 * Method to get the color of the axes on the 3D display.
	 * The default is "gray".
	 * @return the color of the axes on the 3D display.
	 */
	public static String get3DColorAxes() { return cache3DColorAxes.getString(); }
	/**
	 * Method to set the color of the axes on the 3D display.
	 * @param c the color of the axes on the 3D display.
	 */
	public static void set3DColorAxes(String c)
    {
        cache3DColorAxes.setString(c);
        J3DAppearance.setAxisAppearanceValues(null);
    }

    /**
     * Method to get maximum number of nodes to consider a scene graph bi
     * The default is "1000".
     * @return maximim number of nodes.
     */
    public static int get3DMaxNumNodes() { return cache3DMaxNumber.getInt(); }
    /**
     * Method to set maximum number of nodes to display in 3D view.
     * @param num maximim number of nodes.
     */
    public static void set3DMaxNumNodes(int num) { cache3DMaxNumber.setInt(num); }

    /**
     * Method to tell whether to draw 3D axes or not.
     * The default is "true".
     * @return true to draw 3D axes.
     */
    public static boolean is3DAxesOn() { return cache3DAxes.getBoolean(); }
    /**
     * Method to set whether to draw 3D axes or not.
     * @param on true to draw 3D axes.
     */
    public static void set3DAxesOn(boolean on) { cache3DAxes.setBoolean(on); }

	/**
	 * Method to tell whether to draw bounding box for the cells.
	 * The default is "true".
	 * @return true to draw bounding box for the cells.
	 */
	public static boolean is3DCellBndOn() { return cache3DCellBnd.getBoolean(); }
	/**
	 * Method to set whether to draw bounding box for the cells.
	 * @param on true to draw bounding box for the cells.
	 */
	public static void set3DCellBndOn(boolean on) { cache3DCellBnd.setBoolean(on); }

	/**
	 * Method to tell whether to draw 3D views with perspective.
	 * The default is "true".
	 * @return true to draw 3D views with perspective.
	 */
	public static boolean is3DPerspective() { return cache3DPerspective.getBoolean(); }
	/**
	 * Method to set whether to draw 3D views with perspective.
	 * @param on true to draw 3D views with perspective.
	 */
	public static void set3DPerspective(boolean on) { cache3DPerspective.setBoolean(on); }

	/**
	 * Method to tell whether to use antialiasing in 3D view.
	 * The default is "false" due to performance.
	 * @return true to draw 3D views with perspective.
	 */
	public static boolean is3DAntialiasing() { return cache3DAntialiasing.getBoolean(); }
	/**
	 * Method to set whether to draw 3D views with perspective.
	 * @param on true to draw 3D views with perspective.
	 */
	public static void set3DAntialiasing(boolean on) { cache3DAntialiasing.setBoolean(on); }

    /**
	 * Method to get original zoom factor for the view
	 * The default is 1
	 * @return original zoom factor
	 */
	public static double get3DOrigZoom() { return cache3DOrigZoom.getDouble(); }

    /**
	 * Method to set default zoom factor
	 * @param value zoom factor
	 */
	public static void set3DOrigZoom(double value) { cache3DOrigZoom.setDouble(value); }

    /**
	 * Method to get default rotation for the view along X, Y and Z
	 * The default is (0 0 0) and values are in radiant
	 * @return rotation along X, y and Z axes.
	 */
	public static String get3DRotation() { return cache3DRot.getString(); }

    /**
	 * Method to set default rotation angles along X, Y and Z. Values are in radiant
	 * @param value angles on X, Y and Z
	 */
	public static void set3DRotation(String value) { cache3DRot.setString(value); }

    /**
	 * Method to get current scale factor for Z values.
	 * The default is 1.0
	 * @return scale factor along Z.
	 */
	public static double get3DFactor() { return cache3DFactor.getDouble(); }

    /**
	 * Method to set 3D scale factor
	 * @param value 3D scale factor to set.
	 */
	public static void set3DFactor(double value) { cache3DFactor.setDouble(value); }

    /**
	 * Method to get current alpha speed for 3D demos
	 * The default is 1000
	 * @return alpha speed.
	 */
	public static int get3DAlpha() { return cache3DAlpha.getInt(); }

    /**
	 * Method to set 3D alpha speed
	 * @param value 3D alpha to set.
	 */
	public static void set3DAlpha(int value)
    {
        cache3DAlpha.setInt(value);
        setAlpha(value);
    }

    /**
     * Method to generate knots for interpolator from a file
     * @param view3D
     * @return list with knot points. Null if operation was cancelled
     */
    public static List<J3DUtils.ThreeDDemoKnot> readDemoDataFromFile(View3DWindow view3D)
    {
        String fileName = OpenFile.chooseInputFile(FileType.TEXT, null);

        if (fileName == null) return null; // Cancel

        String[] possibleValues = { "Accept All", "OK", "Skip", "Cancel" };

        List<J3DUtils.ThreeDDemoKnot> knotList = null;
        try {
            LineNumberReader lineReader = new LineNumberReader(new FileReader(fileName));
            int response = -1;
            for(;;)
            {
                // get keyword from file
                String line = lineReader.readLine();
                if (line == null) break;
                // responce 0 -> Accept All
                if (response != 0)
                {
                    response = Job.getUserInterface().askForChoice("Applying following data " + line, "Action",
                  		possibleValues, possibleValues[0]);
                    if (response == 2) continue; // skip
                    else if (response == 3) break; // cancel option
                }
                String[] stringValues = parseValues(line, 0);
                double[] values = convertValues(stringValues);
                if (knotList == null) knotList = new ArrayList<J3DUtils.ThreeDDemoKnot>();
                knotList.add(view3D.moveAndRotate(values));
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return knotList;
    }

    public static double[] convertValues(String[] stringValues)
    {
        double[] values = new double[stringValues.length];
        for (int i = 0; i < stringValues.length; i++)
        {
            try
            {
                values[i] = Double.parseDouble(stringValues[i]);
            }
            catch (Exception e) // invalid number in line
            {
                values[i] = lastValidValues[i];
            }
            lastValidValues[i] = values[i];
            if (2 < i && i < 6 )
                values[i] = convertToRadiant(values[i]);   // original value is in degrees
        }
        return values;
    }

    /**
     * To parse capacitance data from line
     * Format: posX posY posZ rotX rotY rotZ rotPosX rotPosY rotPosZ capacitance radius error
     * @param line
     * @param lineNumner
     */
    public static String[] parseValues(String line, int lineNumner)
    {
        int count = 0;
        String[] strings = new String[VALUES_PER_LINE]; // 12 is the max value including errors
        StringTokenizer parse = new StringTokenizer(line, " ", false);

        while (parse.hasMoreTokens() && count < VALUES_PER_LINE)
        {
            strings[count++] = parse.nextToken();
        }
        if (count < 9 || count > 13)
        {
            System.out.println("Error reading capacitance file in line " + lineNumner);
        }
        return strings;
    }

    /**
	 * Method to get the color of the cell instance on the 3D display.
	 * The default is "gray".
	 * @return the color of the cell instance on the 3D display.
	 */
	public static int get3DColorInstanceCell() { return cache3DColorInstanceCell.getInt(); }

    /**
	 * Method to set the color of the cell instance on the 3D display.
	 * @param c the color of the cell instance on the 3D display.
	 */
	public static void set3DColorInstanceCell(int c)
    {
        cache3DColorInstanceCell.setInt(c);
        J3DAppearance.setCellAppearanceValues(null);
    }

    /**
	 * Method to get the color of the highlighted instance on the 3D display.
	 * The default is "gray".
	 * @return the color of the highlighted instance on the 3D display.
	 */
	public static int get3DColorHighlighted() { return cache3DColorHighlighted.getInt(); }

    /**
	 * Method to set the color of the highlighted instance on the 3D display.
	 * @param c the color of the highlighted instance on the 3D display.
	 */
	public static void set3DColorHighlighted(int c)
    {
        cache3DColorHighlighted.setInt(c);
        J3DAppearance.setHighlightedAppearanceValues(null);
    }

    /**
	 * Method to get the ambiental color on the 3D display.
	 * The default is "gray".
	 * @return the ambiental color on the 3D display.
	 */
	public static int get3DColorAmbientLight() { return cache3DColorAmbient.getInt(); }

    /**
	 * Method to set the ambiental color on the 3D display.
	 * @param c the ambiental color on the 3D display.
	 */
	public static void set3DColorAmbientLight(int c)
    {
        cache3DColorAmbient.setInt(c);
        setAmbientalColor(null);
    }

    /********************************************************************************************************
     *   Observer-Observable pattern for Vector3f
     *******************************************************************************************************/
    /**
     * Class using view-model paradigm to update Vector3d-type of variables like directions
     */
    private static class Vector3fObservable extends Observable
    {
        private Vector3f vector;

        public Vector3fObservable(Vector3f vec)
        {
            this.vector = vec;
        }
        public void setValue(Vector3f vec)
        {
            this.vector = vec;
            setChanged();
            notifyObservers(vector);
            clearChanged();
        }
        public Vector3f getValue() {return vector;}
    }

    /**
     * Observer class for directional light
     */
    private static class DirectionalLightObserver extends DirectionalLight implements Observer
    {
        public DirectionalLightObserver(Color3f color3f, Vector3f vector3f)
        {
            super(color3f, vector3f);
        }
        public void update(Observable o, Object arg)
        {
            if (arg != null)
            {
                if (arg instanceof Vector3f) // Change direction
                {
                    // change the direction
                    setDirection((Vector3f)arg);
                    return;
                }

                if (arg instanceof Color3f) // Change direction
                {
                    // change the direction
                    setColor((Color3f)arg);
                    return;
                }
            }
        }
    }

    /********************************************************************************************************
     *   Observer-Observable pattern for Color3f
     *******************************************************************************************************/
    private static class Color3fObservable extends Observable
    {
        private Color3f color;

        public Color3fObservable(Color3f color)
        {
            this.color = color;
        }
        public void setValue(Color3f color)
        {
            this.color = color;
            setChanged();
            notifyObservers(color);
            clearChanged();
        }
        public Color3f getValue() {return color;}
    }

    private static class AmbientLightObserver extends AmbientLight implements Observer
    {
        public AmbientLightObserver(Color3f color3f)
        {
            super(color3f);
        }
        public void update(Observable o, Object arg)
        {
            if (arg != null && arg instanceof Color3f)
            {
                // change the color
                setColor((Color3f)arg);
            }
        }
    }

    private static class BackgroundObserver extends Background implements Observer
    {
        public BackgroundObserver(Color3f color3f)
        {
            super(color3f);
            setCapability(Background.ALLOW_COLOR_WRITE);
            setCapability(Background.ALLOW_COLOR_READ);
        }
        public void update(Observable o, Object arg)
        {
            if (arg != null && arg instanceof Color3f)
            {
                // change the color
                setColor((Color3f)arg);
            }
        }
    }

    public static Vector3f[] transformIntoVectors(String dir)
    {
        float[][] values = new float[2][3];
        StringTokenizer parse = new StringTokenizer(dir, "()", false);
        int pair = 0;

        while (parse.hasMoreTokens() && pair < 2)
        {
            String vector = parse.nextToken();
            StringTokenizer parseDir = new StringTokenizer(vector, " )", false);
            int count = 0;
            while (parseDir.hasMoreTokens() && count < 3)
            {
                String value = parseDir.nextToken();
                values[pair][count++] = Float.parseFloat(value);
            }
            pair++;
        }
        Vector3f[] vectors = new Vector3f[2];
        for (int i = 0; i < 2; i++)
        {
            if (!(values[i][0] == 0 && values[i][1] == 0 && values[i][2] == 0))
                vectors[i]= new Vector3f(values[i]);
        }
        return (vectors);
    }

    /**
     * Method to set direction in directional light
     * @param initValue
     */
    public static void setDirections(Object initValue)
    {
        Vector3f[] dirs = transformIntoVectors(get3DLightDirs());
        for (int i = 0; i < dirs.length; i++)
        {
            if (lights[i] == null)
                lights[i] = new Vector3fObservable(dirs[i]);
            else if (initValue == null)
                lights[i].setValue(dirs[i]); // it will notify observers
        }
    }

    /**
     * Method to set ambiental color
     * @param initValue null if value has to be redone from user data
     */
    public static void setAmbientalColor(Object initValue)
    {
        Color3f userColor = new Color3f(new Color(get3DColorAmbientLight()));
        if (ambientalColor == null)
            ambientalColor = new Color3fObservable(userColor);
        else if (initValue == null)
            ambientalColor.setValue(userColor);
    }

    /**
     * Method to set directional color
     * @param initValue null if value has to be redone from user data
     */
    public static void setDirectionalColor(Object initValue)
    {
        Color3f userColor = new Color3f(new Color(get3DColorDirectionalLight()));
        if (directionalColor == null)
            directionalColor = new Color3fObservable(userColor);
        else if (initValue == null)
            directionalColor.setValue(userColor);
    }

    /**
     * Method to set background color
     * @param initValue null if value has to be redone from user data
     */
    public static void setBackgroundColor(Object initValue)
    {
        Color3f userColor = new Color3f(new Color(User.getColorBackground()));
        if (backgroundColor == null)
            backgroundColor = new Color3fObservable(userColor);
        else if (initValue == null)
            backgroundColor.setValue(userColor);
    }

    /** Create the background node based on given background color
     * @param scene
     */
    public static void createBackground(BranchGroup scene)
    {
        setBackgroundColor(scene);
        BackgroundObserver bg = new BackgroundObserver(backgroundColor.getValue());
        backgroundColor.addObserver(bg);
		bg.setApplicationBounds(infiniteBounds);
		scene.addChild(bg);
    }

    /**
     * Create alpha according to given interval time in miliseconds
     * @param speed
     */
    public static void setAlpha(int speed)
    {
        if (jAlpha == null)
            jAlpha = new J3DAlpha(speed, true, 0.5f);
        else
            jAlpha.setAlphaSpeed(speed);
    }

    /**
     * Create the lights (directional and ambiental) for the given scene graph
     * based on User's data
     * @param scene
     */
    public static void createLights(BranchGroup scene)
    {
        // Checking if light colors are available
        setDirectionalColor(scene);
        setAmbientalColor(scene);
        setDirections(scene);

        AmbientLightObserver ambientalLight =  new AmbientLightObserver(ambientalColor.getValue());
        ambientalLight.setInfluencingBounds(infiniteBounds);
        ambientalLight.setCapability(AmbientLight.ALLOW_COLOR_READ);
        ambientalLight.setCapability(AmbientLight.ALLOW_COLOR_WRITE);
        // adding observer
        ambientalColor.addObserver(ambientalLight);
        // Add ambiental light to the env.
        scene.addChild(ambientalLight);

        for (int i = 0; i < lights.length; i++)
        {
            if (lights[i] == null || lights[i].getValue() == null ||
                    (lights[i].getValue().x == 0 &&
                    lights[i].getValue().y == 0 &&
                    lights[i].getValue().z == 0)) continue; // invalid light
            DirectionalLightObserver directionalLight = new DirectionalLightObserver(directionalColor.getValue(),
                    lights[i].getValue());
            directionalLight.setInfluencingBounds(infiniteBounds);
            // Allow to turn off light while the scene graph is live
            directionalLight.setCapability(Light.ALLOW_STATE_WRITE);
            directionalLight.setCapability(DirectionalLight.ALLOW_DIRECTION_READ);
            directionalLight.setCapability(DirectionalLight.ALLOW_DIRECTION_WRITE);
            directionalLight.setCapability(DirectionalLight.ALLOW_COLOR_READ);
            directionalLight.setCapability(DirectionalLight.ALLOW_COLOR_WRITE);
            // adding observers
            lights[i].addObserver(directionalLight);
            directionalColor.addObserver(directionalLight);

            // Add lights to the env.
            scene.addChild(directionalLight);
        }
    }

    public static void setViewPoint(SimpleUniverse u, Canvas3D canvas, BranchGroup scene, Rectangle2D cellBnd)
    {
		BoundingSphere sceneBnd = (BoundingSphere)scene.getBounds();
		double radius = sceneBnd.getRadius();
		View view = u.getViewer().getView();

		// Too expensive at this point
        if (canvas.getSceneAntialiasingAvailable() && is3DAntialiasing())
		    view.setSceneAntialiasingEnable(true);

		// Setting the projection policy
		view.setProjectionPolicy(is3DPerspective()? View.PERSPECTIVE_PROJECTION : View.PARALLEL_PROJECTION);
		if (!is3DPerspective()) view.setCompatibilityModeEnable(true);

         // Setting transparency sorting
        //view.setTransparencySortingPolicy(View.TRANSPARENCY_SORT_GEOMETRY);
        //view.setDepthBufferFreezeTransparent(false); // set to true only for transparent layers

		Point3d c1 = new Point3d();
		sceneBnd.getCenter(c1);
		Vector3d vCenter = new Vector3d(c1);
		double vDist = 1.4 * radius / Math.tan(view.getFieldOfView()/2.0);
        Point3d c2 = new Point3d();

        sceneBnd.getCenter(c2);
		c2.z += vDist;

		//if (User.is3DPerspective())
		vCenter.z += vDist;
		Transform3D vTrans = new Transform3D();

		vTrans.set(vCenter);

		view.setBackClipDistance((vDist+radius)*2.0);
		view.setFrontClipDistance((vDist+radius)/200.0);
		view.setBackClipPolicy(View.VIRTUAL_EYE);
		view.setFrontClipPolicy(View.VIRTUAL_EYE);
		if (is3DPerspective())
		{
			u.getViewingPlatform().getViewPlatformTransform().setTransform(vTrans);
		}
		else
		{
            Transform3D proj = new Transform3D();
            proj.ortho(cellBnd.getMinX(), cellBnd.getMinX(), cellBnd.getMinY(), cellBnd.getMaxY(), (vDist+radius)/200.0, (vDist+radius)*2.0);
			view.setVpcToEc(proj);
			//viewingPlatform.getViewPlatformTransform().setTransform(lookAt);
		}

    }


//    public static double covertToDegrees(double radiant)
//    {
//        return ((180*radiant)/Math.PI);
//    }

    public static double convertToRadiant(double degrees)
    {
        return ((Math.PI*degrees)/180);
    }

    /**
     * Utility class to modify live/compiled scene graph
     */
    private static class JGeometryUpdater implements GeometryUpdater
    {
        float z1, z2, origZ1, origZ2;

        public JGeometryUpdater(float origZ1, float origZ2, float z1, float z2)
        {
            this.z1 = z1;
            this.z2 = z2;
            this.origZ1 = origZ1;
            this.origZ2 = origZ2;
        }

        public void updateData(Geometry geometry)
        {
            if (!(geometry instanceof GeometryArray)) return;

            GeometryArray ga = (GeometryArray)geometry;
            float[] vals = ga.getCoordRefFloat();

            for (int i = 0; i < vals.length/3; i++)
            {
                if (DBMath.areEquals(vals[i*3+2], origZ1))
                    vals[i*3+2] = z1;
                else if (DBMath.areEquals(vals[i*3+2], origZ2))
                    vals[i*3+2] = z2;
            }

            ga.setCoordRefFloat(vals);
        }
    }

    /**
     * Method to reset z values of shapes created with addPolyhedron
     * @param shape
     * @param origZ1
     * @param origZ2
     * @param z1
     * @param z2
     * @return true if values were valid
     */
    public static boolean updateZValues(Shape3D shape, float origZ1, float origZ2, float z1, float z2)
    {
        if (DBMath.areEquals(z1, z2)) return false; // nothing to do. Eg. 0 as value
        GeometryArray ga = (GeometryArray)shape.getGeometry();
        ga.updateData(new JGeometryUpdater(origZ1, origZ2, z1, z2));
        return true;
    }

    /**
	 * Method to add a polyhedron to the transformation group
	 * @param objTrans
	 */
	public static Shape3D addPolyhedron(Rectangle2D bounds, double distance, double thickness,
	                          Appearance ap, TransformGroup objTrans)
	{
        GeometryInfo gi = new GeometryInfo(GeometryInfo.QUAD_ARRAY);
        double height = thickness + distance;
        Point3d[] pts = new Point3d[8];
        pts[0] = new Point3d(bounds.getMinX(), bounds.getMinY(), distance);
        pts[1] = new Point3d(bounds.getMinX(), bounds.getMaxY(), distance);
        pts[2] = new Point3d(bounds.getMaxX(), bounds.getMaxY(), distance);
        pts[3] = new Point3d(bounds.getMaxX(), bounds.getMinY(), distance);
        pts[4] = new Point3d(bounds.getMinX(), bounds.getMinY(), height);
        pts[5] = new Point3d(bounds.getMinX(), bounds.getMaxY(), height);
        pts[6] = new Point3d(bounds.getMaxX(), bounds.getMaxY(), height);
        pts[7] = new Point3d(bounds.getMaxX(), bounds.getMinY(), height);
        int[] indices = {0, 1, 2, 3, /* bottom z */
                         0, 4, 5, 1, /* back y */
                         0, 3, 7, 4, /* back x */
                         1, 5, 6, 2, /* front x */
                         2, 6, 7, 3, /* front y */
                         4, 7, 6, 5}; /* top z */
        gi.setCoordinates(pts);
        gi.setCoordinateIndices(indices);
        NormalGenerator ng = new NormalGenerator();
        ng.generateNormals(gi);
        GeometryArray c = gi.getGeometryArray(true, false, false);

        c.setCapability(GeometryArray.ALLOW_INTERSECT);
        //c.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
        c.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
        c.setCapability(GeometryArray.BY_REFERENCE);
        c.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
        c.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);

        Shape3D box = new Shape3D(c, ap);
        box.setCapability(Shape3D.ENABLE_PICK_REPORTING);
        box.setCapability(Node.ALLOW_LOCAL_TO_VWORLD_READ);
        box.setCapability(Shape3D.ALLOW_PICKABLE_READ);
		box.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
		box.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		box.setCapability(Shape3D.ALLOW_BOUNDS_READ);
        box.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
        PickTool.setCapabilities(box, PickTool.INTERSECT_FULL);
        objTrans.addChild(box);

		return(box);
	}

    /**
     * Simple method to generate polyhedra
     * @param pts
     * @param listLen
     * @param ap
     * @return
     */
    public static Shape3D addShape3D(Point3d[] pts, int listLen, Appearance ap,
                                     TransformGroup objTrans)
    {

        int numFaces = listLen + 2; // contour + top + bottom
        int[] indices = new int[listLen*6];
        int[] stripCounts = new int[numFaces];
        int[] contourCount = new int[numFaces];
        Arrays.fill(contourCount, 1);
        Arrays.fill(stripCounts, 4);
        stripCounts[0] = listLen; // top
        stripCounts[numFaces-1] = listLen; // bottom

        int count = 0;
        // Top face
        for (int i = 0; i < listLen; i++)
            indices[count++] = i;
        // Contour
        for (int i = 0; i < listLen; i++)
        {
            indices[count++] = i;
            indices[count++] = i + listLen;
            indices[count++] = (i+1)%listLen + listLen;
            indices[count++] = (i+1)%listLen;
        }
        // Bottom face
        for (int i = 0; i < listLen; i++)
            indices[count++] = (listLen-i)%listLen + listLen;

        GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
        gi.setCoordinates(pts);
        gi.setCoordinateIndices(indices);
        gi.setStripCounts(stripCounts);
        gi.setContourCounts(contourCount);
        NormalGenerator ng = new NormalGenerator();
        ng.setCreaseAngle ((float) Math.toRadians(30));
        ng.generateNormals(gi);
        GeometryArray c = gi.getGeometryArray();
        c.setCapability(GeometryArray.ALLOW_INTERSECT);

        Shape3D box = new Shape3D(c, ap);
        box.setCapability(Shape3D.ENABLE_PICK_REPORTING);
        box.setCapability(Node.ALLOW_LOCAL_TO_VWORLD_READ);
        box.setCapability(Shape3D.ALLOW_PICKABLE_READ);
        box.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        box.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
        box.setCapability(Shape3D.ALLOW_BOUNDS_READ);
        PickTool.setCapabilities(box, PickTool.INTERSECT_FULL);
        objTrans.addChild(box);
        return (box);
    }

    /**
     */
	public static Shape3D addPolyhedron(PathIterator pIt, double distance, double thickness,
	                          Appearance ap, TransformGroup objTrans)
	{
        double height = thickness + distance;
        double [] coords = new double[6];
		java.util.List<Point3d> topList = new ArrayList<Point3d>();
		java.util.List<Point3d> bottomList = new ArrayList<Point3d>();
        java.util.List<Shape3D> shapes = new ArrayList<Shape3D>();

		while (!pIt.isDone())
		{
			int type = pIt.currentSegment(coords);
			if (type == PathIterator.SEG_CLOSE)
			{
				int listLen = topList.size();
				Point3d [] pts = new Point3d[listLen*2];
                correctNormals(topList, bottomList);
				System.arraycopy(topList.toArray(), 0, pts, 0, listLen);
				System.arraycopy(bottomList.toArray(), 0, pts, listLen, listLen);
				int numFaces = listLen + 2; // contour + top + bottom
				int[] indices = new int[listLen*6];
				int[] stripCounts = new int[numFaces];
                int[] contourCount = new int[numFaces];
				Arrays.fill(contourCount, 1);
				Arrays.fill(stripCounts, 4);
				stripCounts[0] = listLen; // top
				stripCounts[numFaces-1] = listLen; // bottom

				int count = 0;
				// Top face
				for (int i = 0; i < listLen; i++)
					indices[count++] = i;
				// Contour
				for (int i = 0; i < listLen; i++)
				{
					indices[count++] = i;
					indices[count++] = i + listLen;
					indices[count++] = (i+1)%listLen + listLen;
					indices[count++] = (i+1)%listLen;
				}
				// Bottom face
				for (int i = 0; i < listLen; i++)
					indices[count++] = (listLen-i)%listLen + listLen;

				GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
				gi.setCoordinates(pts);
				gi.setCoordinateIndices(indices);
				gi.setStripCounts(stripCounts);
				gi.setContourCounts(contourCount);
				NormalGenerator ng = new NormalGenerator();
				ng.setCreaseAngle ((float) Math.toRadians(30));
				ng.generateNormals(gi);
				GeometryArray c = gi.getGeometryArray();
				c.setCapability(GeometryArray.ALLOW_INTERSECT);

				Shape3D box = new Shape3D(c, ap);
				box.setCapability(Shape3D.ENABLE_PICK_REPORTING);
				box.setCapability(Node.ALLOW_LOCAL_TO_VWORLD_READ);
				box.setCapability(Shape3D.ALLOW_PICKABLE_READ);
				box.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
				box.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
				box.setCapability(Shape3D.ALLOW_BOUNDS_READ);
				PickTool.setCapabilities(box, PickTool.INTERSECT_FULL);
				objTrans.addChild(box);
				shapes.add(box);

				topList.clear();
				bottomList.clear();
			} else if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO)
			{
				Point3d pt = new Point3d(coords[0], coords[1], distance);
				topList.add(pt);
				pt = new Point3d(coords[0], coords[1], height);
				bottomList.add(pt);
			}
			pIt.next();
		}

		if (shapes.size()>1) System.out.println("Error: case not handled");
		return((Shape3D)shapes.get(0));
	}

    /**
     * Method to correct points sequence to obtain valid normals
     * @param topList
     * @param bottomList
     */
    public static void correctNormals(List<Point3d> topList, List<Point3d> bottomList)
    {
        // Determining normal direction
        Point3d p0 = (Point3d)topList.get(0);
        Point3d p1 = new Point3d((Point3d)topList.get(1));
        p1.sub(p0);
        Point3d pn = new Point3d((Point3d)topList.get(topList.size()-1));
        pn.sub(p0);
        Vector3d aux = new Vector3d();
        aux.cross(new Vector3d(p1), new Vector3d(pn));
        // the other layer
        Point3d b0 = new Point3d((Point3d)bottomList.get(0));
        b0.sub(p0);
        // Now the dot product
        double dot = aux.dot(new Vector3d(b0));
        if (dot > 0)  // Invert sequence of points otherwise the normals will be wrong
        {
            Collections.reverse(topList);
            Collections.reverse(bottomList);
        }
    }

    /*******************************************************************************************************
     *                      DEMO SECTION
     *******************************************************************************************************/
    /**
     * This class is @serial
     */
    public static class ThreeDDemoKnot implements Serializable
    {
        private static final long serialVersionUID = -7059885190094183408L;

        float xValue;
        float yValue;
        float zValue;
        Vector3f translation;

        float scale;
        float heading; // Sets the camera's heading. This automatically modifies the target's position.
        float pitch; // Sets the camera's pitch in degrees. This automatically modifies the target's position.
        float bank; // Sets the camera's bank in degrees. The angle is relative to the horizon.

        double rotZ;
        double rotY;
        double rotX;
        Quat4f rotation;
//        public Shape3D shape;

        public ThreeDDemoKnot(double xValue, double yValue, double zValue, double scale,
                              double heading, double pitch, double bank,
                              double rotX, double rotY, double rotZ)
        {
            this.xValue = (float)xValue;
            this.yValue = (float)yValue;
            this.zValue = (float)zValue;
            this.scale = (float)scale;
            this.heading = (float)heading;
            this.pitch = (float)pitch;
            this.bank = (float)bank;
            this.rotZ = rotZ;
            this.rotX = rotX;
            this.rotY = rotY;
        }

        public ThreeDDemoKnot(double scale, Vector3f trans, Quat4f rot, Shape3D shape)
        {
            this.scale = (float)scale;
            this.translation = trans;
            this.rotation = rot;
//            this.shape = shape;
        }
    }

    /**
     * Method to generate each individual frame key for the interporlation
     * based on Poly information
     * @param ratio
     * @param knot
     * @return
     */
    public static KBKeyFrame getNextKBKeyFrame(float ratio, ThreeDDemoKnot knot)
    {
        // Prepare spline keyframe data
        Vector3f pos = new Vector3f (knot.xValue+100, knot.yValue+100, knot.zValue);
        Point3f point   = new Point3f (pos);            // position
        Point3f scale   = new Point3f(knot.scale, knot.scale, knot.scale); // uniform scale3D
        KBKeyFrame key = new KBKeyFrame(ratio, 0, point, knot.heading, knot.pitch, knot.bank, scale, 0.0f, 0.0f, 1.0f);
        return key;
    }

    public static TCBKeyFrame getNextTCBKeyFrame(float ratio, ThreeDDemoKnot knot)
    {
        // Prepare spline keyframe data
        Vector3f pos = knot.translation;
        // Initial translatio is not given as vector
        if (pos == null)
            pos = new Vector3f (knot.xValue, knot.yValue, knot.zValue);
        Point3f point = new Point3f (pos);            // position

        Quat4f quat = knot.rotation;
        // Initial rotation not given as Quat4f
        if (quat == null)
            quat = createQuaternionFromEuler(knot.rotX, knot.rotY, knot.rotZ);
        Point3f scale = new Point3f(knot.scale, knot.scale, knot.scale); // uniform scale3D
        TCBKeyFrame key = new TCBKeyFrame(ratio, 0, point, quat, scale, 0.0f, 0.0f, 1.0f);
        return key;
    }

    /**
     * Convert an angular rotation about an axis to a Quaternion.
     * From Selman's book
     * @param axis
     * @param angle
     * @return
     */
	private static Quat4f createQuaternionFromAxisAndAngle( Vector3d axis, double angle )
	{
		double sin_a = Math.sin( angle / 2 );
		double cos_a = Math.cos( angle / 2 );

		// use a vector so we can call normalize
		Vector4f q = new Vector4f( );

		q.x = (float) (axis.x * sin_a);
		q.y = (float) (axis.y * sin_a);
		q.z = (float) (axis.z * sin_a);
		q.w = (float) cos_a;

		// It is necessary to normalise the quaternion
		// in case any values are very close to zero.
		q.normalize( );

		// convert to a Quat4f and return
		return new Quat4f( q );
	}

    /**
     * Convert three rotations about the Euler axes to a Quaternion.
     * From Selman's book
     * @param angleX
     * @param angleY
     * @param angleZ
     * @return
     */
	public static Quat4f createQuaternionFromEuler( double angleX, double angleY, double angleZ )
	{
		// simply call createQuaternionFromAxisAndAngle
		// for each axis and multiply the results
		Quat4f qx = createQuaternionFromAxisAndAngle(J3DAxis.axisX, angleX);
		Quat4f qy = createQuaternionFromAxisAndAngle(J3DAxis.axisY, angleY);
		Quat4f qz = createQuaternionFromAxisAndAngle(J3DAxis.axisZ, angleZ);

		// qx = qx * qy
		qx.mul( qy );

		// qx = qx * qz
		qx.mul( qz );

		return qx;
	}

    /*******************************************************************************************************
     *                      Color Tab SECTION
     *******************************************************************************************************/


    public static void get3DColorsInTab(HashMap<String,ColorPatternPanel.Info> transAndSpecialMap)
    {
        // 3D Stuff
		transAndSpecialMap.put("Special: 3D CELL INSTANCES", new ColorPatternPanel.Info(get3DColorInstanceCell()));
        transAndSpecialMap.put("Special: 3D HIGHLIGHTED INSTANCES", new ColorPatternPanel.Info(get3DColorHighlighted()));
        transAndSpecialMap.put("Special: 3D AMBIENT LIGHT", new ColorPatternPanel.Info(get3DColorAmbientLight()));
        transAndSpecialMap.put("Special: 3D DIRECTIONAL LIGHT", new ColorPatternPanel.Info(get3DColorDirectionalLight()));


        double[] colors = GenMath.transformVectorIntoValues(get3DColorAxes());
        String[] axisNames = {" X", " Y", " Z"};
		String name = "Special: 3D AXIS";
        for (int i = 0; i < colors.length; i++)
        {
            String color3DName = name+axisNames[i];
            transAndSpecialMap.put(color3DName, new ColorPatternPanel.Info((int)colors[i]));
        }
    }

    public static Boolean set3DColorsInTab(LayersTab tab)
    {
        int [] colors3D = new int[3];
		boolean colorChanged = false;
        int c = -1;

        if ((c = tab.specialMapColor("Special: 3D CELL INSTANCES", get3DColorInstanceCell())) >= 0)
        { set3DColorInstanceCell(c);   colorChanged = true; }
        if ((c = tab.specialMapColor("Special: 3D HIGHLIGHTED INSTANCES", get3DColorHighlighted())) >= 0)
        { set3DColorHighlighted(c);   colorChanged = true; }

        if ((c = tab.specialMapColor("Special: 3D AMBIENT LIGHT", get3DColorAmbientLight())) >= 0)
        { set3DColorAmbientLight(c);   colorChanged = true; }
        if ((c = tab.specialMapColor("Special: 3D DIRECTIONAL LIGHT", get3DColorDirectionalLight())) >= 0)
        { set3DColorDirectionalLight(c);   colorChanged = true; }
        if ((c = tab.specialMapColor("Special: 3D AXIS X", get3DColorHighlighted())) >= 0)
        { colors3D[0] = c;   colorChanged = true; }
        if ((c = tab.specialMapColor("Special: 3D AXIS Y", get3DColorHighlighted())) >= 0)
        { colors3D[1] = c;   colorChanged = true; }
        if ((c = tab.specialMapColor("Special: 3D AXIS Z", get3DColorHighlighted())) >= 0)
        { colors3D[2] = c;   colorChanged = true; }

        // For 3D colors as they are stored together
        String newColors = "("+colors3D[0]+" "+
                colors3D[1]+" "+
                colors3D[2]+")";

        if (!newColors.equals(get3DColorAxes()))
        {
            set3DColorAxes(newColors);
            colorChanged = true;
        }
        return (new Boolean(colorChanged));
    }

    /*******************************************************************************************************
     *                      IMAGE SECTION
     *******************************************************************************************************/
    /**
     * Class to create offscreen from canvas 3D
     */
    public static class OffScreenCanvas3D extends Canvas3D {
        public OffScreenCanvas3D(GraphicsConfiguration graphicsConfiguration,
                  boolean offScreen) {
            super(graphicsConfiguration, offScreen);
        }

        BufferedImage doRender(int width, int height)
        {
			BufferedImage bImage =
				new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

			ImageComponent2D buffer =
				new ImageComponent2D(ImageComponent.FORMAT_RGBA, bImage);

			setOffScreenBuffer(buffer);
			renderOffScreenBuffer();
			waitForOffScreenRendering();
			bImage = getOffScreenBuffer().getImage();

			return bImage;
        }

        public void postSwap() {
        // No-op since we always wait for off-screen rendering to complete
        }
    }


}
