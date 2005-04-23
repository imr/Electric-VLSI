package com.sun.electric.plugins.j3d.utils;

import javax.vecmath.Matrix4d;
import javax.media.j3d.Transform3D;
import java.io.Serializable;
import java.util.List;

/**
 * This class is @serial
 */
public class J3DSerialization implements Serializable
{
    static final long serialVersionUID = 8621126756329190786L;

    public List list;
    public Matrix4d matrix;
    public Boolean useView;

    public J3DSerialization(Boolean useView, List l, Transform3D t)
    {
        this.useView = useView;
        list = l;
        matrix = new Matrix4d(); // 16 elements are in transformation matrix
        t.get(matrix);
    }
}
