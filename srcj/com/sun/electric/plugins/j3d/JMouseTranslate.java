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
