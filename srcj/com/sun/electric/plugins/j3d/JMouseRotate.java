package com.sun.electric.plugins.j3d;

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;
import javax.media.j3d.Transform3D;

/**
 * Extending original rotation class to allow rotation not from original behavior
 * @author  Gilda Garreton
 * @version 0.1
 */
public class JMouseRotate extends MouseRotate
{
    private Transform3D transformZ = new Transform3D();
    public JMouseRotate(int flags) {super(flags);}

    public void setRotation(double angleX, double angleY, double angleZ)
    {
        // Rotations are in the opposite direction
        transformX.rotX(-1*angleX);
        transformY.rotY(-1*angleY);
        transformZ.rotZ(-1*angleZ);

        transformGroup.getTransform(currXform);

        Matrix4d mat = new Matrix4d();
        // Remember old matrix
        currXform.get(mat);

        // Translate to origin
        currXform.setTranslation(new Vector3d(0.0,0.0,0.0));
        if (invert) {
            currXform.mul(currXform, transformX);
            currXform.mul(currXform, transformY);
            currXform.mul(currXform, transformZ);
        } else {
            currXform.mul(transformX, currXform);
            currXform.mul(transformY, currXform);
            currXform.mul(transformZ, currXform);
        }

        // Set old translation back
        Vector3d translation = new
        Vector3d(mat.m03, mat.m13, mat.m23);
        currXform.setTranslation(translation);

        // Update xform
        transformGroup.setTransform(currXform);

        transformChanged( currXform );
    }
}
