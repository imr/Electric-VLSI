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
 * $Revision: 1.1 $
 * $Date: 2005/04/14 18:07:25 $
 * $State: Exp $
 */
package com.sun.electric.plugins.j3d.utils;

import com.sun.electric.tool.user.User;

import java.awt.*;
import javax.media.j3d.*;
import javax.vecmath.*;

/**
 * Original class provided by Java3D team.
 * Modified by Gilda Garreton to incorporate Java Electric attributes
 */
public class J3DAxis extends Group {
	private static final float[] axisCoords = {
	    0.0f, 0.0f, 0.0f,  0.10f,  0.00f,  0.00f, // X-axis
	    0.1f, 0.0f, 0.0f,  0.09f,  0.01f,  0.00f, // X-axis arrow
	    0.1f, 0.0f, 0.0f,  0.09f, -0.01f,  0.00f, // X-axis arrow
	    0.1f, 0.0f, 0.0f,  0.09f,  0.00f,  0.01f, // X-axis arrow
	    0.1f, 0.0f, 0.0f,  0.09f,  0.00f, -0.01f, // X-axis arrow
	};
    /** Font for 3D axis labels */ private static Font3D font3D;

	public J3DAxis(double factor)
    {
	    Transform3D t = new Transform3D();

        if (font3D == null)
            font3D = new Font3D(new Font(User.getDefaultFont(), Font.PLAIN, 2),
                     new FontExtrusion());
	    //
	    // Build X-axis (red)
	    //

	    t = new Transform3D();
        t.setScale(factor);  // Axes are scaled according to sphere radius of the scene graph
	    TransformGroup xAxisTG = new TransformGroup(t); // Identity transform

	    // X-axis lines
	    LineArray xAxisLineArr = new LineArray(10, GeometryArray.COORDINATES);
	    xAxisLineArr.setCoordinates(0, axisCoords);
	    Shape3D xAxisLines = new Shape3D(xAxisLineArr, J3DAppearance.axisApps[0]);
	    xAxisTG.addChild(xAxisLines);

	    // X-axis label
	    Text3D xAxisText = new Text3D(font3D, "+X");
	    Shape3D xAxisLabel = new Shape3D(xAxisText, J3DAppearance.axisApps[0]);
	    Transform3D xTextScale = new Transform3D();
	    xTextScale.set(0.015);
	    xTextScale.setTranslation(new Vector3d(0.11, 0.0, 0.0));
	    TransformGroup xAxisLabelTG = new TransformGroup(xTextScale);
	    xAxisLabelTG.addChild(xAxisLabel);
	    xAxisTG.addChild(xAxisLabelTG);

	    this.addChild(xAxisTG);

	    //
	    // Build Y-axis (green)
	    //
	    t = new Transform3D();
	    t.rotZ(Math.PI/2.0); // rotate about Z-axis to create Y-axis
        t.setScale(factor);
	    TransformGroup yAxisTG = new TransformGroup(t);

	    // Y-axis lines
	    LineArray yAxisLineArr = new LineArray(10, GeometryArray.COORDINATES);
	    yAxisLineArr.setCoordinates(0, axisCoords);
	    Shape3D yAxisLines = new Shape3D(yAxisLineArr, J3DAppearance.axisApps[1]);
	    yAxisTG.addChild(yAxisLines);

	    // Y-axis label
	    Text3D yAxisText = new Text3D(font3D, "+Y");
	    Shape3D yAxisLabel = new Shape3D(yAxisText, J3DAppearance.axisApps[1]);
	    Transform3D yTextScale = new Transform3D();
	    yTextScale.set(0.015);
	    yTextScale.setTranslation(new Vector3d(0.11, 0.0, 0.0));
	    TransformGroup yAxisLabelTG = new TransformGroup(yTextScale);
	    yAxisLabelTG.addChild(yAxisLabel);
	    yAxisTG.addChild(yAxisLabelTG);

	    this.addChild(yAxisTG);

	    //
	    // Build Z-axis (blue)
	    //

	    t = new Transform3D();
	    t.rotY(-Math.PI/2.0); // rotate about Y-axis to create Z-axis
        t.setScale(factor);
	    TransformGroup zAxisTG = new TransformGroup(t);

	    // Z-axis lines
	    LineArray zAxisLineArr = new LineArray(10, GeometryArray.COORDINATES);
	    zAxisLineArr.setCoordinates(0, axisCoords);
	    Shape3D zAxisLines = new Shape3D(zAxisLineArr, J3DAppearance.axisApps[2]);
	    zAxisTG.addChild(zAxisLines);

	    // Z-axis label
	    Text3D zAxisText = new Text3D(font3D, "+Z");
	    Shape3D zAxisLabel = new Shape3D(zAxisText, J3DAppearance.axisApps[2]);
	    Transform3D zTextScale = new Transform3D();
	    zTextScale.set(0.015);
	    zTextScale.setTranslation(new Vector3d(0.11, 0.0, 0.0));
	    TransformGroup zAxisLabelTG = new TransformGroup(zTextScale);
	    zAxisLabelTG.addChild(zAxisLabel);
	    zAxisTG.addChild(zAxisLabelTG);

	    this.addChild(zAxisTG);

	}
}
