/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: View3DWindow.java
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

import com.sun.j3d.utils.behaviors.interpolators.KBKeyFrame;
import com.sun.j3d.utils.behaviors.interpolators.TCBKeyFrame;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.picking.PickTool;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.electric.tool.user.User;

import javax.vecmath.*;
import javax.media.j3d.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Utility class for 3D module
 * @author  Gilda Garreton
 * @version 0.1
 */
public final class J3DUtils
{
    /** standard colors to be used by materials **/         public static final Color3f black = new Color3f(0.0f, 0.0f, 0.0f);
    /** standard colors to be used by materials **/         public static final Color3f white = new Color3f(1.0f, 1.0f, 1.0f);
    /** Ambiental light **/                 private static Color3fObservable ambientalColor;
    /** Directional light **/               private static Color3fObservable directionalColor;
    /** Directional vector **/              private static Vector3fObservable light1; // = new Vector3f(-1.0f, -1.0f, -1.0f);

    public static final BoundingSphere infiniteBounds = new BoundingSphere(new Point3d(), Double.MAX_VALUE);

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
     * Observer class for directional class
     */
    private static class DirectionalLightObserver extends DirectionalLight implements Observer
    {
        public DirectionalLightObserver(Color3f color3f, Vector3f vector3f)
        {
            super(color3f, vector3f);
        }
        public void update(Observable o, Object arg)
        {
            if (arg != null && arg instanceof Vector3f)
            {
                // change the direction
                setDirection((Vector3f)arg);
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

    /********************************************************************************************************
     *
     *******************************************************************************************************/

    public static boolean isValidDirection(String dir)
    {
        return !(dir.equals("") || dir.equals("0 0 0 "));
    }

    public static Vector3f transformIntoVector(String dir)
    {
        float[] values = new float[3];
        StringTokenizer parse = new StringTokenizer(dir, " ", false);
        int count = 0;

        while (parse.hasMoreTokens() && count < 3)
        {
            values[count++] = Float.parseFloat(parse.nextToken());
        }
        return (new Vector3f(values));
    }

    /**
     * Method to set direction in directional light
     * @param initValue
     */
    public static void setDirection(Object initValue)
    {
        Vector3f dir = transformIntoVector(User.get3DLightDirOne());
        if (light1 == null)
            light1 = new Vector3fObservable(dir);
        else if (initValue == null)
            light1.setValue(dir); // it will notify observers
    }

    /**
     * Method to set ambiental color
     * @param initValue null if value has to be redone from user data
     */
    public static void setAmbientalColor(Object initValue)
    {
        Color3f userColor = new Color3f(new Color(User.get3DColorAmbientLight()));
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
        Color3f userColor = new Color3f(new Color(User.get3DColorDirectionalLight()));
        if (directionalColor == null)
            directionalColor = new Color3fObservable(userColor);
        else if (initValue == null)
            directionalColor.setValue(userColor);
    }

    public static void createLights(BranchGroup scene, TransformGroup objTrans)
    {
        // Checking if light colors are available
        setDirectionalColor(scene);
        setAmbientalColor(scene);
        setDirection(scene);

        AmbientLightObserver ambientalLight =  new AmbientLightObserver(ambientalColor.getValue());
        ambientalLight.setInfluencingBounds(infiniteBounds);
        ambientalLight.setCapability(AmbientLight.ALLOW_COLOR_READ);
        ambientalLight.setCapability(AmbientLight.ALLOW_COLOR_WRITE);
        // adding observer
        ambientalColor.addObserver(ambientalLight);

        DirectionalLightObserver directionalLight = new DirectionalLightObserver(directionalColor.getValue(), light1.getValue());
        directionalLight.setInfluencingBounds(infiniteBounds);
        // Allow to turn off light while the scene graph is live
        directionalLight.setCapability(Light.ALLOW_STATE_WRITE);
        directionalLight.setCapability(DirectionalLight.ALLOW_DIRECTION_READ);
        directionalLight.setCapability(DirectionalLight.ALLOW_DIRECTION_WRITE);
        directionalLight.setCapability(DirectionalLight.ALLOW_COLOR_READ);
        directionalLight.setCapability(DirectionalLight.ALLOW_COLOR_WRITE);
        // adding observers
        light1.addObserver(directionalLight);
        directionalColor.addObserver(directionalLight);

        // Add lights to the env.
        scene.addChild(ambientalLight);
        objTrans.addChild(directionalLight);
    }

    public static void setViewPoint(SimpleUniverse u, Canvas3D canvas, BranchGroup scene, Rectangle2D cellBnd)
    {
		BoundingSphere sceneBnd = (BoundingSphere)scene.getBounds();
		double radius = sceneBnd.getRadius();
		View view = u.getViewer().getView();

		// Too expensive at this point
        if (canvas.getSceneAntialiasingAvailable() && User.is3DAntialiasing())
		    view.setSceneAntialiasingEnable(true);

		// Setting the projection policy
		view.setProjectionPolicy(User.is3DPerspective()? View.PERSPECTIVE_PROJECTION : View.PARALLEL_PROJECTION);
		if (!User.is3DPerspective()) view.setCompatibilityModeEnable(true);

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
		if (User.is3DPerspective())
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
        Vector3f pos = new Vector3f (knot.xValue, knot.yValue, knot.zValue);
        Point3f point = new Point3f (pos);            // position
        Quat4f quat = createQuaternionFromEuler(knot.rotX, knot.rotY, knot.rotZ);
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
	public static Quat4f createQuaternionFromAxisAndAngle( Vector3d axis, double angle )
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
		Quat4f qx = createQuaternionFromAxisAndAngle( new Vector3d( 1,0,0 ), angleX );
		Quat4f qy = createQuaternionFromAxisAndAngle( new Vector3d( 0,1,0 ), angleY );
		Quat4f qz = createQuaternionFromAxisAndAngle( new Vector3d( 0,0,1 ), angleZ );

		// qx = qx * qy
		qx.mul( qy );

		// qx = qx * qz
		qx.mul( qz );

		return qx;
	}

    public static double covertToDegrees(double radiant)
    {
        return ((180*radiant)/Math.PI);
    }

    public static double convertToRadiant(double degrees)
    {
        return ((Math.PI*degrees)/180);
    }

    /**
     * Utility class to modify live/compiled scene graph
     */
    private static class JGeometryUpdater implements GeometryUpdater
    {

        private double[] pts; // new set of points for this geometry, 3 values per point.

        public JGeometryUpdater(double[] pts)
        {
            this.pts = pts;
        }

        public void updateData(Geometry geometry)
        {
            if (!(geometry instanceof GeometryArray)) return;

            GeometryArray ga = (GeometryArray)geometry;
            ga.setCoordRefDouble(pts);

        }
    }

    /**
     * Method to reset z values of shapes created with addPolyhedron
     * @param shape
     * @param z1
     * @param z2
     */
    public static void updateZValues(Shape3D shape, double z1, double z2)
    {
        GeometryArray ga = (GeometryArray)shape.getGeometry();
        Point3d[] pts = new Point3d[8];
        double[] values = new double[3*8];
        //double[] newValues = ga.getCoordRefDouble();

        // They must be 8-points polyhedra
        for (int i = 0; i < 4; i++)
        {
//            newValues[i*3+2] = z1;
            pts[i] = new Point3d();
            ga.getCoordinate(i, pts[i]);
            pts[i].z = z1;
            ga.setCoordinate(i, pts[i]);
//            values[i*3] = pts[i].x;
//            values[i*3+1] = pts[i].y;
//            values[i*3+2] = z1;
        }

        for (int i = 4; i < 8; i++)
        {
            //newValues[i*3+2] = z2;
            pts[i] = new Point3d();
            ga.getCoordinate(i, pts[i]);
            pts[i].z = z2;
            ga.setCoordinate(i, pts[i]);
//            values[i*3] = pts[i].x;
//            values[i*3+1] = pts[i].y;
//            values[i*3+2] = z2;
        }
        //ga.updateData(new JGeometryUpdater(newValues));
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
        GeometryArray c = gi.getGeometryArray();
//
//        Point3f[] pts1 = gi.getCoordinates();
//
//        //gi.getTexCoordSetMapLength();
//        //c.get
//
//        GeometryArray c = new QuadArray(8,
//          GeometryArray.COORDINATES | GeometryArray.NORMALS | GeometryArray.BY_REFERENCE);
//
//        double[] values = new double[3*8];
//        for (int i = 0; i < 8; i++)
//        {
//            values[i*3] = pts1[i].x;
//            values[i*3+1] = pts1[i].y;
//            values[i*3+2] = pts1[i].z;
//        }
//        float[] valuesf = new float[8*3];
//        old.getCoordinates(0, valuesf);
//        c.setCoordRefFloat(valuesf);
//        float[] normals = new float[8*3];
//        old.getNormals(0, normals);
//        c.setNormalRefFloat(normals);

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
		java.util.List topList = new ArrayList();
		java.util.List bottomList = new ArrayList();
        java.util.List shapes = new ArrayList();

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
    public static void correctNormals(java.util.List topList, java.util.List bottomList)
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

    public static class ThreeDDemoKnot
    {
        float xValue;
        float yValue;
        float zValue;
        float scale;
        float heading; // Sets the camera's heading. This automatically modifies the target's position.
        float pitch; // Sets the camera's pitch in degrees. This automatically modifies the target's position.
        float bank; // Sets the camera's bank in degrees. The angle is relative to the horizon.
        double rotZ;
        double rotY;
        double rotX;

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
    }

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
