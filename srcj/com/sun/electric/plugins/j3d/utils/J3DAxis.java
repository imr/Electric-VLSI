/*
 * $RCSfile: J3DAxis.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 *
 * $Revision: 1.2 $
 * $Date: 2005/04/22 01:11:14 $
 * $State: Exp $
 */
package com.sun.electric.plugins.j3d.utils;

import com.sun.j3d.utils.geometry.Cone;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Cylinder;

import java.awt.*;
import javax.media.j3d.*;
import javax.vecmath.*;

/**
 * Original class provided by Java3D team.
 * Modified by Gilda Garreton to incorporate Java Electric attributes
 */
public class J3DAxis extends Group {
//	private static final float[] axisCoords = {
//	    0.0f, 0.0f, 0.0f,  0.10f,  0.00f,  0.00f, // X-axis
//	    0.1f, 0.0f, 0.0f,  0.09f,  0.01f,  0.00f, // X-axis arrow
//	    0.1f, 0.0f, 0.0f,  0.09f, -0.01f,  0.00f, // X-axis arrow
//	    0.1f, 0.0f, 0.0f,  0.09f,  0.00f,  0.01f, // X-axis arrow
//	    0.1f, 0.0f, 0.0f,  0.09f,  0.00f, -0.01f, // X-axis arrow
//	};
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
