package com.sun.electric.plugins.j3d;

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

/**
 * Extending original rotation class to allow rotation not from original behavior
 * @author  Gilda Garreton
 * @version 0.1
 */
public class JMouseRotate extends MouseRotate
{
    public JMouseRotate(int flags) {super(flags);}

    void setRotation(double angleX, double angleY)
    {
        transformX.rotX(angleX);
        transformY.rotY(angleY);

        transformGroup.getTransform(currXform);

        Matrix4d mat = new Matrix4d();
        // Remember old matrix
        currXform.get(mat);

        // Translate to origin
        currXform.setTranslation(new Vector3d(0.0,0.0,0.0));
        if (invert) {
        currXform.mul(currXform, transformX);
        currXform.mul(currXform, transformY);
        } else {
        currXform.mul(transformX, currXform);
        currXform.mul(transformY, currXform);
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
