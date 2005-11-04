/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: J3DSerialization.java
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

import javax.vecmath.Matrix4d;
import javax.media.j3d.Transform3D;
import java.io.Serializable;
import java.util.List;

/**
 * This class is @serial
 * @author  Gilda Garreton
 * @version 0.1
 */
public class J3DSerialization implements Serializable
{
    static final long serialVersionUID = 8621126756329190786L;

    public List<J3DUtils.ThreeDDemoKnot> list;
    public Matrix4d matrix;
    public Boolean useView;

    public J3DSerialization(Boolean useView, List<J3DUtils.ThreeDDemoKnot> l, Transform3D t)
    {
        this.useView = useView;
        list = l;
        matrix = new Matrix4d(); // 16 elements are in transformation matrix
        t.get(matrix);
    }
}
