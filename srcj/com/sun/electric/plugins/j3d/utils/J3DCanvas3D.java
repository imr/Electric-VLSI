/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: J3DCanvas3D.java
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

import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.sun.electric.tool.user.Resources;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;
import javax.media.j3d.*;
import javax.vecmath.*;
import javax.imageio.ImageIO;

/** Inspired in example found in www.j3d.org
 * @author  Gilda Garreton
 * @version 0.1
*/

public class J3DCanvas3D extends Canvas3D  {

    public String filePath = null;
    public boolean writePNG_;
    public BufferedImage img;
    private int count;
    public boolean movieMode;

    public J3DCanvas3D(GraphicsConfiguration gc)
    {
	    super(gc);
    }

    public void renderField( int fieldDesc )
	{
        try {
            super.renderField( fieldDesc );
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    List<String> inputFiles = new ArrayList<String>();
    public void saveMovie(String filename)
    {
        Class movieClass = Resources.getJMFJ3DClass("JMFImageToMovie");
        if (movieClass == null)
        {
            System.out.println("Java3D plugin not available");
            return;
        }
        try {
            Dimension dim = getSize();
            Method createJMFMethod = movieClass.getDeclaredMethod("createMovie", new Class[] {String.class,
                                                                                              Dimension.class, List.class});
            createJMFMethod.invoke(movieClass, new Object[] {filename, dim, inputFiles});
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void resetMoveFrames()
    {
        inputFiles.clear();
    }

    public void postSwap()
    {
        if(writePNG_)
        {
            Dimension dim = getSize();
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

            if (movieMode)
            {
                try
                {
                    String capture = "Capture" + count + ".jpg";
                    inputFiles.add(capture);
                    FileOutputStream out = new FileOutputStream(capture);

                    JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
                     JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(img);
                     param.setQuality(0.75f,false); // 75% quality for the JPEG
                     encoder.setJPEGEncodeParam(param);
                     encoder.encode(img);
                     out.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                count++;
            }
            else if (filePath != null)  // for png export
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
