/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JMouseZoom.java
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

package com.sun.electric.plugins.j3d;

import com.sun.j3d.utils.behaviors.mouse.MouseZoom;

import javax.vecmath.Matrix4d;
import java.awt.*;

/**
 * Extending original zoom class to allow zoom not from original behavior
 * @author  Gilda Garreton
 * @version 0.1
 */
public class JMouseZoom extends MouseZoom
{
    public JMouseZoom(Component c, int flags) {super(c, flags);}

    public void setZoom(double factor)
    {
        // Remember old matrix
        transformGroup.getTransform(currXform);
        Matrix4d mat = new Matrix4d();
        currXform.get(mat);
        double dy = currXform.getScale() * factor;
        currXform.setScale(dy);
        transformGroup.setTransform(currXform);
        transformChanged( currXform );
    }

    void zoomInOut(boolean out)
    {
//			// Remember old matrix
//			transformGroup.getTransform(currXform);
//
//			Matrix4d mat = new Matrix4d();
//			currXform.get(mat);
        double z_factor = Math.abs(getFactor());
        double factor = (out) ? (0.5/z_factor) : (2*z_factor);
        double factor1 = (out) ? (1/z_factor) : (z_factor);
        setZoom(factor1);
//
//			double dy = currXform.getScale() * factor1;
//			currXform.setScale(dy);
//
//			// Translate to origin
////			currXform.setTranslation(new Vector3d(0.0,0.0,0.0));
////
////			extraTrans.z = dy; //dy*getFactor();
////
////			//transformX.set(extraTrans);
////			transformX.setScale(dy);
////
////			if (invert) {
////				currXform.mul(currXform, transformX);
////			} else {
////				currXform.mul(transformX, currXform);
////			}
////
////			// Set old extraTrans back
////			Vector3d extraTrans = new Vector3d(mat.m03, mat.m13, mat.m23);
////			currXform.setTranslation(extraTrans);
//
//			transformGroup.setTransform(currXform);
//
//			transformChanged( currXform );

    }
}
