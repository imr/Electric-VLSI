/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Interval.java
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

package com.sun.electric.tool.simulation.interval;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Class for writing Spice ".raw" files.
 * Raw data consists of points in vector space.
 * Components of vector have name and type.
 */
public class RawFile {
	private int numVars;
	private int numPoints;
	private double[][] data;
	private String[] varName;
	private String[] varType;

	/**
	 * Constructs RawFile object.
	 * @param numPoints number of points.
	 * @param numVars dimension of vectors..
	 */
	public RawFile(int numPoints, int numVars) {
		this.numVars = numVars;
		this.numPoints = numPoints;
		varName = new String[numVars];
		varType = new String[numVars];
		data = new double[numPoints][];
		for (int i = 0; i < numPoints; i++)
			data[i] = new double[numVars];
    }

	/**
	 * Defines name and type of component of vector.
	 * @param iVar index of component of vector.
	 * @param varName name of component of vector..
	 * @param varType type of component of vector..
	 */
	public void setVar(int iVar, String varName, String varType) {
		this.varName[iVar] = varName;
		this.varType[iVar] = varType;
	}

	/**
	 * Sets value of component of point.
	 * @param iPoint index of point.
	 * @param iVar index of component of vector.
	 * @param v value of component of point.
	 */
	public void set(int iPoint, int iVar, double v) {
		data[iPoint][iVar] = v;
	}

	/**
	 * Writes RawFile data to file in ascii format.
	 * Ascii format is portable across platforms with different byte order.
	 * @param fileName name of file.
	 */
	public void write(String fileName) {
		write(fileName, false, null);
	}

	/**
	 * Writes RawFile data to file either in binary format.
	 * Binary format is more compact and faster than ascii, but
	 * it is not portable across platforms with different byte order.
	 * @param fileName name of file.
	 * @param byteOrder byte order, if null then native bute order of the underlying platform.
	 */
	public void writeBinary(String fileName, ByteOrder byteOrder) {
		write(fileName, true, byteOrder);
	}

	/**
	 * Writes RawFile data to file either in binary format or in ascii format.
	 * Ascii format is portable across platforms with different byte order.
	 * Binary format is faster.
	 * @param fileName name of file.
	 * @param binaryFormat if true write in binary format else write in ascii format.
	 * @param byteOrder byte order, if null then native bute order of the underlying platform.
	 */
	private void write(String fileName, boolean binaryFormat, ByteOrder byteOrder) {
		try {
			DecimalFormat fmt = new DecimalFormat("0.000000000000000000E00");
			fmt.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));

			FileOutputStream outStream = new FileOutputStream(fileName);
			PrintWriter FILE = new PrintWriter(outStream);

			FILE.println("Title: " + fileName);
			FILE.println("Plotname: Waveform");
			FILE.println("Flags: real");
			FILE.println("No. Variables: " + numVars);
			FILE.println("No. Points: " + numPoints);
			FILE.println("Variables:");
			for (int iVar = 0; iVar < numVars; iVar++) {
				FILE.println("\t" + iVar + "\t" + varName[iVar] + "\t" + varType[iVar]);
			}
			if (binaryFormat) {
				FILE.println("Binary:");
				FILE.flush();

				byte[] bytes = new byte[numVars*8];
				ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
				byteBuffer.order(byteOrder != null ? byteOrder : ByteOrder.nativeOrder());

				for (int iPoint = 0; iPoint < numPoints; iPoint++) {
					byteBuffer.clear();
					for (int iVar = 0; iVar < numVars; iVar++)
						byteBuffer.putDouble(data[iPoint][iVar]);
					outStream.write(bytes);
				}
			} else {
				FILE.println("Values:");

				for (int iPoint = 0; iPoint < numPoints; iPoint++) {
					FILE.println(iPoint + "\t" + fmt.format(data[iPoint][0]));
					
					for (int iVar = 1; iVar < numVars; iVar++) {
						FILE.println("\t" + fmt.format(data[iPoint][iVar]));
					}
					FILE.println();
				}
			}
			FILE.close();
		}
		catch (FileNotFoundException e) {
			System.out.println("Failure writing file " + fileName);
		}
		catch (IOException e) {
			System.out.println("Failure writing file " + fileName);
		}
	}
}
