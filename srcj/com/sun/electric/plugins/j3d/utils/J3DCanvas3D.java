package com.sun.electric.plugins.j3d.utils;

import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Cylinder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.media.j3d.*;
import javax.vecmath.*;
import javax.imageio.ImageIO;
//import com.sun.image.codec.jpeg.*;

/** Class CapturingCanvas3D, using the instructions from the Java3D 
    FAQ pages on how to capture a still image in jpeg format.

    A capture button would call a method that looks like


    public static void captureImage(CapturingCanvas3D MyCanvas3D) {
	MyCanvas3D.writePNG_ = true;
	MyCanvas3D.repaint();
    }


    Peter Z. Kunszt
    Johns Hopkins University
    Dept of Physics and Astronomy
    Baltimore MD
*/

public class J3DCanvas3D extends Canvas3D  {

    public String filePath = null;
    public boolean writePNG_;
    public BufferedImage img;
    private Shape3D axis;

    public J3DCanvas3D(GraphicsConfiguration gc)
    {
	    super(gc);
    }

    public void renderField( int fieldDesc )
	{
        try {
            super.renderField( fieldDesc );
//            GraphicsContext3D g = getGraphicsContext3D( );
//                Dimension dim = getScreen3D().getSize();
//            if (axis == null)
//            {
//                Cylinder cylinder = new Cylinder(1, (float)10);
//                axis = cylinder.getShape(0);
//            }
//            Screen3D sOn = getScreen3D();
//            Dimension dim1 = sOn.getSize();
//            Transform3D cylinderTrans = new Transform3D();
//        Vector3d cylinderLocation = new Vector3d(dim1.getWidth()/2, dim1.getHeight()/3, 0); // Cylinder and cone are along Y
//            cylinderTrans.lookAt(new Point3d(10, 10, 10), new Point3d(0, 0, 0), new Vector3d(0, 0, -11));
//        //cylinderTrans.setTranslation(cylinderLocation);
//            g.setModelTransform(cylinderTrans);
//            g.draw(axis);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void postSwap()
    {
        if(writePNG_)
        {
            Dimension dim = getScreen3D().getSize();

            GraphicsContext3D  ctx = getGraphicsContext3D();
            // The raster components need all be set!

            Raster ras = new Raster(
                       new Point3f(-1.0f, -1.0f, -1.0f),
               Raster.RASTER_COLOR,
               0,0,
               dim.width, dim.height,
               new ImageComponent2D(
                                 ImageComponent.FORMAT_RGB,
                     new BufferedImage(dim.width, dim.height,
                               BufferedImage.TYPE_INT_RGB)),
               null);

            ctx.readRaster(ras);

            // Now strip out the image info
            img = ras.getImage().getImage();
            writePNG_ = false;

            if (filePath != null)
            {
                File tmp = new File(filePath);
                try
                {
                    ImageIO.write(img, "PNG", tmp);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                filePath = null;
            }
        }

    }
}
