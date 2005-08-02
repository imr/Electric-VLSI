/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: J3DAxis.java
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

import com.sun.j3d.utils.geometry.Cone;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Cylinder;

import java.awt.*;
import javax.media.j3d.*;
import javax.vecmath.*;

/**
 * Utility class to create 3D axes
 * @author  Gilda Garreton
 * @version 0.1
 */
public class J3DAxis extends Group
{
    /** Font for 3D axis labels */ private static Font3D font3D;
    public static final Vector3d axisX = new Vector3d(1,0,0);
    public static final Vector3d axisY = new Vector3d(0,1,0);
    public static final Vector3d axisZ = new Vector3d(0,0,1);

    /**
     * Method to create axis composed of a cylinder + a cone
     * @param factor
     */
    private void createAxis(double factor, Vector3d dir, Appearance app, String text)
    {
        Transform3D t = new Transform3D();

        float length = 0.1f;
        float diameter = (float)length*.08f;
        Primitive axis = new Cylinder(diameter, (float)length, app);
        Transform3D cylinderTrans = new Transform3D();
        Transform3D coneTrans = new Transform3D();

        if (dir == axisX)
            t.rotZ(-Math.PI/2);
        else if (dir == axisZ)
            t.rotX(Math.PI/2);
        t.setScale(factor);  // Axes are scaled according to sphere radius of the scene graph
        TransformGroup axisTG = new TransformGroup(t); // Identity transform

        Vector3d cylinderLocation = new Vector3d(0, length/2, 0);
        cylinderTrans.setTranslation(cylinderLocation);
        TransformGroup cylinderG = new TransformGroup(cylinderTrans);
        cylinderG.addChild(axis);
        axisTG.addChild(cylinderG);
        Primitive arrow = new Cone(1.5f * diameter, (float)length/3, app);
        Vector3d coneLocation = new Vector3d(0, length, 0);
        coneTrans.setTranslation(coneLocation);
        TransformGroup coneG = new TransformGroup(coneTrans);
        coneG.addChild(arrow);
        axisTG.addChild(coneG);
        
        // Adding the text
	    Text3D axisText = new Text3D(font3D, text);
	    Shape3D axisLabel = new Shape3D(axisText, app);
	    Transform3D textScale = new Transform3D();
	    textScale.set(0.015);
	    textScale.setTranslation(new Vector3d(0, 0.11, 0.0));
	    TransformGroup axisLabelG = new TransformGroup(textScale);
	    axisLabelG.addChild(axisLabel);
	    axisTG.addChild(axisLabelG);

        // Adding finally to the group
        addChild(axisTG);
    }

	public J3DAxis(double factor, Appearance xApp, Appearance yApp, Appearance zApp,
                   String defaultFont)
    {
        if (font3D == null)
            font3D = new Font3D(new Font(defaultFont, Font.PLAIN, 2),
                     new FontExtrusion());

        createAxis(factor, axisX, xApp, "+X");
        createAxis(factor, axisY, yApp, "+Y");
        createAxis(factor, axisZ, zApp, "+Z");

	}
}
