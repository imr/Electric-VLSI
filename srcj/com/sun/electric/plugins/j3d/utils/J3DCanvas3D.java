package com.sun.electric.plugins.j3d.utils;

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

    public J3DCanvas3D(GraphicsConfiguration gc)
    {
	    super(gc);
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
