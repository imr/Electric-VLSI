package com.sun.electric.plugins.j3d.utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.media.j3d.*;
import javax.vecmath.*;
//import com.sun.image.codec.jpeg.*;

/** Class CapturingCanvas3D, using the instructions from the Java3D 
    FAQ pages on how to capture a still image in jpeg format.

    A capture button would call a method that looks like


    public static void captureImage(CapturingCanvas3D MyCanvas3D) {
	MyCanvas3D.writeJPEG_ = true;
	MyCanvas3D.repaint();
    }


    Peter Z. Kunszt
    Johns Hopkins University
    Dept of Physics and Astronomy
    Baltimore MD
*/

public class J3DCanvas3D extends Canvas3D  {

    public boolean writeJPEG_;
    public BufferedImage img;

    public J3DCanvas3D(GraphicsConfiguration gc) {
	super(gc);
    }

    public void postSwap() {
	if(writeJPEG_) {
	    System.out.println("Writing JPEG");
	    GraphicsContext3D  ctx = getGraphicsContext3D();
	    // The raster components need all be set!
	    Raster ras = new Raster(
                   new Point3f(-1.0f,-1.0f,-1.0f),
		   Raster.RASTER_COLOR,
		   0,0,
		   512,512,
		   new ImageComponent2D(
                             ImageComponent.FORMAT_RGB,
			     new BufferedImage(512,512,
					       BufferedImage.TYPE_INT_RGB)),
		   null);

	    ctx.readRaster(ras);

	    // Now strip out the image info
	    img = ras.getImage().getImage();


		writeJPEG_ = false;

	    // write that to disk....
//	    try {
//		FileOutputStream out = new FileOutputStream("Capture"+postSwapCount_+".jpg");
//		JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
//		JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(img);
//		param.setQuality(0.9f,false); // 90% qualith JPEG
//		encoder.setJPEGEncodeParam(param);
//		encoder.encode(img);
//		writeJPEG_ = false;
//		out.close();
//	    } catch ( IOException e ) {
//		System.out.println("I/O exception!");
//	    }
	}
    }
}
