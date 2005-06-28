/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JMouseTranslate.java
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

import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;

import javax.vecmath.Vector3d;
import java.awt.*;

/**
 * Extending original translate class to allow panning
 * @author  Gilda Garreton
 * @version 0.1
 */
public class JMouseTranslate extends MouseTranslate
{
    Vector3d extraTrans = new Vector3d();

    public JMouseTranslate(Component c, int flags) {super(c, flags);}

    void setView(double x, double y)
    {
        transformGroup.getTransform(currXform);
        extraTrans.x = x;
        extraTrans.y = -y;
        transformX.set(extraTrans);

        if (invert) {
            currXform.mul(currXform, transformX);
        } else {
            currXform.mul(transformX, currXform);
        }

        transformGroup.setTransform(currXform);
        transformChanged( currXform );
    }

    void panning(int dx, int dy)
    {
        setView(dx*getXFactor(), -dy*getYFactor());
    }
}
