package com.sun.electric.tool.io.output;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.Iterator;
import java.awt.image.RenderedImage;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Dec 6, 2004
 * Time: 9:42:30 AM
 * To change this template use File | Settings | File Templates.
 */
/**
 * Format to write PNG (Portable Network Graphics) output
 */
public class PNG extends Output
{
	/**
	 * Main entry point for PNG output.
	 * @param img image to export
	 * @param filePath the name of the file to create.
	 */
	public static void writeImage(RenderedImage img, String filePath)
	{
		// just do this file
		//writeCellToFile(cell, context, filePath);
		File tmp = new File(filePath);
        boolean b = canWriteFormat("PNG");

		if (!canWriteFormat("PNG"))
		{
			System.out.println("PNG format cannot be generated");
			return;
		}

        try {
            ImageIO.write(img, "PNG", tmp);
        }
        catch (Exception e)
        {
	        e.printStackTrace();
        }
	}

	/**
	 * Returns true if the specified format name can be written
	 * @param formatName
	 * @return
	 */
    public static boolean canWriteFormat(String formatName) {
        Iterator iter = ImageIO.getImageWritersByFormatName(formatName);
        return iter.hasNext();
    }
}
