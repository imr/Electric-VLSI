/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PNG.java
 * Written by Gilda Garreton, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.output;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.Iterator;
import java.awt.image.BufferedImage;

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
	public static void writeImage(BufferedImage img, String filePath)
	{
		// just do this file
		//writeCellToFile(cell, context, filePath);
		File tmp = new File(filePath);

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
            System.out.println("PNG output '" + filePath + "' cannot be generated");
        }
	}

	/**
	 * Returns true if the specified format name can be written
	 * @param formatName
	 */
    public static boolean canWriteFormat(String formatName) {
        Iterator iter = ImageIO.getImageWritersByFormatName(formatName);
        return iter.hasNext();
    }
}
