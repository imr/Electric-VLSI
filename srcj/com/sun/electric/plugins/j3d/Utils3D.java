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
package com.sun.electric.plugins.j3d;

import com.sun.j3d.utils.behaviors.interpolators.KBKeyFrame;
import com.sun.j3d.utils.behaviors.interpolators.TCBKeyFrame;

import javax.vecmath.*;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.ImageComponent;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Utility class for 3D module
 * @author  Gilda Garreton
 * @version 0.1
 */
public final class Utils3D
{
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
        Vector3f pos = new Vector3f (knot.xValue+100, knot.yValue+100, knot.zValue);
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
        OffScreenCanvas3D(GraphicsConfiguration graphicsConfiguration,
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
