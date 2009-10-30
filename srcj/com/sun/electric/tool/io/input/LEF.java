/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LEF.java
 * Input/output tool: LEF (Library Exchange Format) reader
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.RTBounds;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URL;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class reads files in LEF files.
 * <BR>
 * Note that this reader was built by examining LEF files and reverse-engineering them.
 * It does not claim to be compliant with the LEF specification, but it also does not
 * claim to define a new specification.  It is merely incomplete.
 */
public class LEF extends LEFDEF
{
	/*************** LEF PATHS ***************/

	private static class LEFPath
	{
		private Point2D []  pt;
		private NodeInst [] ni;
		private double      width;
		private ArcProto    arc;
		private LEFPath     nextLEFPath;

		private LEFPath()
		{
			pt = new Point2D[2];
			ni = new NodeInst[2];
		}
	}
	private LEFPreferences localPrefs;
	private Technology curTech;

	public static class LEFPreferences extends InputPreferences
    {
		public LEFPreferences(boolean factory) { super(factory); }

        @Override
        public Library doInput(URL fileURL, Library lib, Technology tech, Map<Library,Cell> currentCells, Map<CellId,BitSet> nodesToExpand, Job job)
        {
        	LEF in = new LEF(this);
			if (in.openTextInput(fileURL)) return null;
			lib = in.importALibrary(lib, tech, currentCells);
			in.closeInput();
			return lib;
        }
    }

	/**
	 * Creates a new instance of LEF.
	 */
	LEF(LEFPreferences ap) { localPrefs = ap; }

	/**
	 * Method to import a library from disk.
	 * @param lib the library to fill
     * @param currentCells this map will be filled with currentCells in Libraries found in library file
	 * @return the created library (null on error).
	 */
    @Override
	protected Library importALibrary(Library lib, Technology tech, Map<Library,Cell> currentCells)
	{
		// remove any vias in the globals
    	curTech = tech;
		firstViaDefFromLEF = null;
		widthsFromLEF = new HashMap<ArcProto,Double>();
		knownLayers = new HashMap<String,GetLayerInformation>();
		initKeywordParsing();

		try
		{
            if (readFile(lib)) return null; // error during reading
        } catch (IOException e)
		{
			System.out.println("ERROR reading LEF libraries");
		}
		return lib;
	}

	/**
	 * Helper method for keyword processing which removes comments.
	 * @param line a line of text just read.
	 * @return the line after comments have been removed.
	 */
	protected String preprocessLine(String line)
	{
		int sharpPos = line.indexOf('#');
		if (sharpPos >= 0) return line.substring(0, sharpPos);
		return line;
	}

	/**
	 * Method to read the LEF file.
	 * @return true on error.
	 */
	private boolean readFile(Library lib)
		throws IOException
	{
		for(;;)
		{
			// get the next keyword
			String key = getAKeyword();
			if (key == null) break;
			if (key.equalsIgnoreCase("LAYER"))
			{
				if (readLayer(lib)) return true;
			}
			if (key.equalsIgnoreCase("MACRO"))
			{
				if (readMacro(lib)) return true;
			}
			if (key.equalsIgnoreCase("VIA"))
			{
				if (readVia(lib)) return true;
			}
			if (key.equalsIgnoreCase("VIARULE") || key.equalsIgnoreCase("SITE") ||
				key.equalsIgnoreCase("ARRAY"))
			{
				String name = getAKeyword();
				ignoreToEnd(name);
				continue;
			}
			if (key.equalsIgnoreCase("SPACING"))
			{
				ignoreToEnd(key);
				continue;
			}
			if (key.equalsIgnoreCase("MINFEATURE"))
			{
				ignoreToSemicolon(key);
				continue;
			}
		}
		return false;
	}

	private boolean readVia(Library lib)
		throws IOException
	{
		// get the via name
		String viaName = getAKeyword();
		if (viaName == null) return true;

		// create a new via definition
		ViaDef vd = new ViaDef();
		vd.viaName = viaName;
		vd.sX = vd.sY = 0;
		vd.via = null;
		vd.lay1 = vd.lay2 = null;
		vd.nextViaDef = firstViaDefFromLEF;
		firstViaDefFromLEF = vd;

		boolean ignoreDefault = true;
		for(;;)
		{
			// get the next keyword
			String key = getAKeyword();
			if (key == null) return true;
			if (ignoreDefault)
			{
				ignoreDefault = false;
				if (key.equalsIgnoreCase("DEFAULT")) continue;
			}
			if (key.equalsIgnoreCase("END"))
			{
				key = getAKeyword();
				break;
			}
			if (key.equalsIgnoreCase("RESISTANCE"))
			{
				if (ignoreToSemicolon(key)) return true;
				continue;
			}
			if (key.equalsIgnoreCase("LAYER"))
			{
				key = getAKeyword();
				if (key == null) return true;
				GetLayerInformation li = getLayerInformation(key);
				if (li.arc != null)
				{
					if (vd.lay1 == null) vd.lay1 = li.arc; else
						vd.lay2 = li.arc;
				}
				if (ignoreToSemicolon("LAYER")) return true;
				continue;
			}
			if (key.equalsIgnoreCase("RECT"))
			{
				// handle definition of a via rectangle
				key = getAKeyword();
				if (key == null) return true;
				double lX = convertLEFString(key);

				key = getAKeyword();
				if (key == null) return true;
				double lY = convertLEFString(key);

				key = getAKeyword();
				if (key == null) return true;
				double hX = convertLEFString(key);

				key = getAKeyword();
				if (key == null) return true;
				double hY = convertLEFString(key);

				// accumulate largest layer size
				if (hX-lX > vd.sX) vd.sX = hX - lX;
				if (hY-lY > vd.sY) vd.sY = hY - lY;

				if (ignoreToSemicolon("RECT")) return true;
				continue;
			}
		}
		if (vd.lay1 != null && vd.lay2 != null)
		{
			for(Iterator<PrimitiveNode> it = curTech.getNodes(); it.hasNext(); )
			{
				PrimitiveNode np = it.next();
				if (!np.getFunction().isContact()) continue;
				PortProto pp = np.getPort(0);
				if (pp.connectsTo(vd.lay1) && pp.connectsTo(vd.lay2))
				{
					vd.via = np;
					break;
				}
			}
		}
		return false;
	}

	private boolean readMacro(Library lib)
		throws IOException
	{
		String cellName = getAKeyword();
		if (cellName == null)
		{
			System.out.println("EOF parsing MACRO header");
			return true;
		}
		cellName = cellName + "{lay.sk}";
		Cell cell = Cell.makeInstance(lib, cellName);
		if (cell == null)
		{
			System.out.println("Cannot create cell '" + cellName + "'");
			return true;
		}

		for(;;)
		{
			// get the next keyword
			String key = getAKeyword();
			if (key == null)
			{
				System.out.println("EOF parsing MACRO");
				return true;
			}

			if (key.equalsIgnoreCase("END"))
			{
				key = getAKeyword();
				break;
			}

			if (key.equalsIgnoreCase("SOURCE") || key.equalsIgnoreCase("FOREIGN") ||
				key.equalsIgnoreCase("SYMMETRY") || key.equalsIgnoreCase("SITE") ||
				key.equalsIgnoreCase("CLASS") || key.equalsIgnoreCase("LEQ") ||
				key.equalsIgnoreCase("POWER"))
			{
				if (ignoreToSemicolon(key)) return true;
				continue;
			}

			if (key.equalsIgnoreCase("ORIGIN"))
			{
				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading ORIGIN X");
					return true;
				}
				double oX = convertLEFString(key);

				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading ORIGIN Y");
					return true;
				}
				double oY = convertLEFString(key);
				if (ignoreToSemicolon("ORIGIN")) return true;

				// create or move the cell-center node
				NodeInst ccNi = null;
				for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = it.next();
					if (ni.getProto() == Generic.tech().cellCenterNode) { ccNi = ni;   break; }
				}
				if (ccNi == null)
				{
					double sX = Generic.tech().cellCenterNode.getDefWidth();
					double sY = Generic.tech().cellCenterNode.getDefHeight();
					ccNi = NodeInst.makeInstance(Generic.tech().cellCenterNode, new Point2D.Double(oX, oY), sX, sY, cell);
					if (ccNi == null)
					{
						System.out.println("Line " + lineReader.getLineNumber() + ": Cannot create cell center node");
						return true;
					}
					ccNi.setHardSelect();
					ccNi.setVisInside();
				} else
				{
					double dX = oX - ccNi.getTrueCenterX();
					double dY = oY - ccNi.getTrueCenterY();
					ccNi.move(dX, dY);
				}
				continue;
			}

			if (key.equalsIgnoreCase("SIZE"))
			{
				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading SIZE X");
					return true;
				}
				double wid = convertLEFString(key);		// get width

				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading SIZE 'BY'");
					return true;
				}
				if (!key.equalsIgnoreCase("BY"))
				{
					System.out.println("Line " + lineReader.getLineNumber() + ": Expected 'by' in SIZE");
					return true;
				}

				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading SIZE Y");
					return true;
				}
				double hei = convertLEFString(key);		// get height
				cell.newVar(prXkey, new Double(wid));
				cell.newVar(prYkey, new Double(hei));
				if (ignoreToSemicolon("SIZE")) return true;

				if (!PLACELEFGEOMETRY)
				{
					Point2D ctr = new Point2D.Double(wid/2, hei/2);
					NodeInst.makeInstance(Generic.tech().invisiblePinNode, ctr, wid, hei, cell);
				}
				continue;
			}

			if (key.equalsIgnoreCase("PIN"))
			{
				if (readPin(cell)) return true;
				continue;
			}

			if (key.equalsIgnoreCase("OBS"))
			{
				if (readObs(cell)) return true;
				continue;
			}

			System.out.println("Line " + lineReader.getLineNumber() + ": Unknown MACRO keyword (" + key + ")");
			return true;
		}
		return false;
	}

	private boolean readObs(Cell cell)
		throws IOException
	{
		NodeProto np = null;
		for(;;)
		{
			String key = getAKeyword();
			if (key == null)
			{
				System.out.println("EOF parsing OBS");
				return true;
			}

			if (key.equalsIgnoreCase("END")) break;

			if (key.equalsIgnoreCase("LAYER"))
			{
				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading LAYER clause");
					return true;
				}
				GetLayerInformation li = getLayerInformation(key);
				np = li.pure;
				if (li.layerFun == Layer.Function.UNKNOWN || np == null)
				{
					System.out.println("Line " + lineReader.getLineNumber() + ": Unknown layer name (" + key + ")");
					return true;
				}
				if (ignoreToSemicolon("LAYER")) return true;
				continue;
			}

			if (key.equalsIgnoreCase("RECT"))
			{
				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading RECT low X");
					return true;
				}
				double lX = convertLEFString(key);

				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading RECT low Y");
					return true;
				}
				double lY = convertLEFString(key);

				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading RECT high X");
					return true;
				}
				double hX = convertLEFString(key);

				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading RECT high Y");
					return true;
				}
				double hY = convertLEFString(key);

				if (ignoreToSemicolon("RECT")) return true;

				// make the obstruction
				if (PLACELEFGEOMETRY)
				{
					if (np == null)
					{
						System.out.println("Line " + lineReader.getLineNumber() + ": No layers for RECT");
						return true;
					}
					Point2D ctr = new Point2D.Double((lX+hX)/2, (lY+hY)/2);
					double sX = Math.abs(hX - lX);
					double sY = Math.abs(hY - lY);
					NodeInst ni = NodeInst.makeInstance(np, ctr, sX, sY, cell);
					if (ni == null)
					{
						System.out.println("Line " + lineReader.getLineNumber() + ": Cannot create node for RECT");
						return true;
					}
				}
				continue;
			}
		}
		return false;
	}

	private boolean readPin(Cell cell)
		throws IOException
	{
		// get the pin name
		String key = getAKeyword();
		if (key == null)
		{
			System.out.println("EOF parsing PIN name");
			return true;
		}
		String pinName = key.replace('<', '[').replace('>', ']');

		PortCharacteristic useCharacteristics = PortCharacteristic.UNKNOWN;
		PortCharacteristic portCharacteristics = PortCharacteristic.UNKNOWN;
		for(;;)
		{
			key = getAKeyword();
			if (key == null)
			{
				System.out.println("EOF parsing PIN");
				return true;
			}

			if (key.equalsIgnoreCase("END"))
			{
				key = getAKeyword();
				break;
			}

			if (key.equalsIgnoreCase("SHAPE") || key.equalsIgnoreCase("CAPACITANCE") ||
				key.equalsIgnoreCase("ANTENNASIZE"))
			{
				if (ignoreToSemicolon(key)) return true;
				continue;
			}

			if (key.equalsIgnoreCase("USE"))
			{
				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading USE clause");
					return true;
				}
				if (key.equalsIgnoreCase("POWER")) useCharacteristics = PortCharacteristic.PWR; else
				if (key.equalsIgnoreCase("GROUND")) useCharacteristics = PortCharacteristic.GND; else
				if (key.equalsIgnoreCase("CLOCK")) useCharacteristics = PortCharacteristic.CLK; else
				if (!key.equalsIgnoreCase("SIGNAL") && !key.equalsIgnoreCase("DATA"))
				{
					System.out.println("Line " + lineReader.getLineNumber() + ": Unknown USE keyword (" + key + ")");
				}
				if (ignoreToSemicolon("USE")) return true;
				continue;
			}

			if (key.equalsIgnoreCase("DIRECTION"))
			{
				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading DIRECTION clause");
					return true;
				}
				if (key.equalsIgnoreCase("INPUT")) portCharacteristics = PortCharacteristic.IN; else
				if (key.equalsIgnoreCase("OUTPUT")) portCharacteristics = PortCharacteristic.OUT; else
				if (key.equalsIgnoreCase("INOUT")) portCharacteristics = PortCharacteristic.BIDIR; else
				{
					System.out.println("Line " + lineReader.getLineNumber() + ": Unknown DIRECTION keyword (" + key + ")");
				}
				if (ignoreToSemicolon("DIRECTION")) return true;
				continue;
			}

			if (key.equalsIgnoreCase("PORT"))
			{
				if (useCharacteristics != PortCharacteristic.UNKNOWN) portCharacteristics = useCharacteristics;
				if (readPort(cell, pinName, portCharacteristics)) return true;
				continue;
			}

			System.out.println("Line " + lineReader.getLineNumber() + ": Unknown PIN keyword (" + key + ")");
			return true;
		}
		return false;
	}

	private boolean readPort(Cell cell, String portname, PortCharacteristic portCharacteristics)
		throws IOException
	{
		ArcProto ap = null;
		NodeProto pureNp = null;
		LEFPath lefPaths = null;
		boolean first = true;
		double intWidth = 0;
		double lastIntX = 0, lastIntY = 0;
		Point2D singlePathPoint = null;
		for(;;)
		{
			String key = getAKeyword();
			if (key == null)
			{
				System.out.println("EOF parsing PORT");
				return true;
			}

			if (key.equalsIgnoreCase("END"))
			{
				break;
			}
			if (key.equalsIgnoreCase("CLASS"))
			{
				if (ignoreToSemicolon("LAYER")) return true;
				continue;
			}
			if (key.equalsIgnoreCase("LAYER"))
			{
				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading LAYER clause");
					return true;
				}
				GetLayerInformation li = getLayerInformation(key);
				ap = li.arc;
				pureNp = li.pure;
				if (ignoreToSemicolon("LAYER")) return true;
				continue;
			}

			if (key.equalsIgnoreCase("WIDTH"))
			{
				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading WIDTH clause");
					return true;
				}
				intWidth = convertLEFString(key);
				if (ignoreToSemicolon("WIDTH")) return true;
				continue;
			}

			if (key.equalsIgnoreCase("RECT"))
			{
				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading RECT low X");
					return true;
				}
				double lX = convertLEFString(key);

				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading RECT low Y");
					return true;
				}
				double lY = convertLEFString(key);

				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading RECT high X");
					return true;
				}
				double hX = convertLEFString(key);

				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading RECT high Y");
					return true;
				}
				double hY = convertLEFString(key);

				if (ignoreToSemicolon("RECT")) return true;

				// make the pin
				if (PLACELEFGEOMETRY)
				{
					if (pureNp == null)
					{
						System.out.println("Line " + lineReader.getLineNumber() + ": No layers for RECT");
						return true;
					}
					Point2D ctr = new Point2D.Double((lX+hX)/2, (lY+hY)/2);
					double sX = Math.abs(hX - lX);
					double sY = Math.abs(hY - lY);
					NodeInst ni = NodeInst.makeInstance(pureNp, ctr, sX, sY, cell);
					if (ni == null)
					{
						System.out.println("Line " + lineReader.getLineNumber() + ": Cannot create pin for RECT");
						return true;
					}

					if (first)
					{
						// create the port on the first pin
						first = false;
						Export pp = newPort(cell, ni, pureNp.getPort(0), portname);
						if (pp != null) pp.setCharacteristic(portCharacteristics);
					}
				}
				continue;
			}

			if (key.equalsIgnoreCase("PATH"))
			{
				if (ap == null)
				{
					System.out.println("Line " + lineReader.getLineNumber() + ": No layers for PATH");
					return true;
				}
				for(int i=0; ; i++)
				{
					key = getAKeyword();
					if (key == null)
					{
						System.out.println("EOF reading PATH clause");
						return true;
					}
					if (key.equals(";")) break;
					double intx = convertLEFString(key);

					key = getAKeyword();
					if (key == null)
					{
						System.out.println("EOF reading PATH clause");
						return true;
					}
					double inty = convertLEFString(key);

					// plot this point
					if (i == 0) singlePathPoint = new Point2D.Double(intx, inty); else
					{
						// queue path
						LEFPath lp = new LEFPath();
						lp.pt[0] = new Point2D.Double(lastIntX, lastIntY);
						lp.pt[1] = new Point2D.Double(intx, inty);
						lp.ni[0] = null;        lp.ni[1] = null;
						lp.width = intWidth;
						lp.arc = ap;
						lp.nextLEFPath = lefPaths;
						lefPaths = lp;
					}
					lastIntX = intx;   lastIntY = inty;
				}
				continue;
			}

			if (key.equalsIgnoreCase("VIA"))
			{
				// get the coordinates
				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading VIA clause");
					return true;
				}
				double intX = convertLEFString(key);

				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading VIA clause");
					return true;
				}
				double intY = convertLEFString(key);

				// find the proper via
				key = getAKeyword();
				GetLayerInformation li = getLayerInformation(key);
				if (li.pin == null)
				{
					System.out.println("Line " + lineReader.getLineNumber() + ": No Via in current technology for '" + key + "'");
					return true;
				}
				if (ignoreToSemicolon("VIA")) return true;

				// create the via
				if (PLACELEFGEOMETRY)
				{
					double sX = li.pin.getDefWidth();
					double sY = li.pin.getDefHeight();
					NodeInst ni = NodeInst.makeInstance(li.pin, new Point2D.Double(intX, intY), sX, sY, cell);
					if (ni == null)
					{
						System.out.println("Line " + lineReader.getLineNumber() + ": Cannot create VIA for PATH");
						return true;
					}
				}
				continue;
			}

			System.out.println("Line " + lineReader.getLineNumber() + ": Unknown PORT keyword (" + key + ")");
			return true;
		}

		if (!PLACELEFGEOMETRY) return false;

		// look for paths that end at vias
		for(LEFPath lp = lefPaths; lp != null; lp = lp.nextLEFPath)
		{
			for(int i=0; i<2; i++)
			{
				if (lp.ni[i] != null) continue;
				Rectangle2D bounds = new Rectangle2D.Double(lp.pt[i].getX(), lp.pt[i].getY(), 0, 0);
				for(Iterator<RTBounds> sea = cell.searchIterator(bounds); sea.hasNext(); )
				{
					RTBounds geom = sea.next();
					if (!(geom instanceof NodeInst)) continue;
					NodeInst ni = (NodeInst)geom;
					if (!DBMath.areEquals(ni.getTrueCenter(), lp.pt[i])) continue;
					lp.ni[i] = ni;
					break;
				}
				if (lp.ni[i] == null) continue;

				// use this via at other paths which meet here
				for(LEFPath oLp = lefPaths; oLp != null; oLp = oLp.nextLEFPath)
				{
					for(int j=0; j<2; j++)
					{
						if (oLp.ni[j] != null) continue;
						if (!DBMath.areEquals(oLp.pt[j], lp.pt[i])) continue;
						oLp.ni[j] = lp.ni[i];
					}
				}
			}
		}

		// create pins at all other path ends
		for(LEFPath lp = lefPaths; lp != null; lp = lp.nextLEFPath)
		{
			for(int i=0; i<2; i++)
			{
				if (lp.ni[i] != null) continue;
				NodeProto pin = lp.arc.findPinProto();
				if (pin == null) continue;
				double sX = pin.getDefWidth();
				double sY = pin.getDefHeight();
				lp.ni[i] = NodeInst.makeInstance(pin, lp.pt[i], sX, sY, cell);
				if (lp.ni[i] == null)
				{
					System.out.println("Line " + lineReader.getLineNumber() + ": Cannot create pin for PATH");
					return true;
				}

				if (first)
				{
					// create the port on the first pin
					first = false;
					Export pp = newPort(cell, lp.ni[i], pin.getPort(0), portname);
					if (pp != null) pp.setCharacteristic(portCharacteristics);
				}

				// use this pin at other paths which meet here
				for(LEFPath oLp = lefPaths; oLp != null; oLp = oLp.nextLEFPath)
				{
					for(int j=0; j<2; j++)
					{
						if (oLp.ni[j] != null) continue;
						if (!DBMath.areEquals(oLp.pt[j], lp.pt[i])) continue;
						oLp.ni[j] = lp.ni[i];
					}
				}
			}
		}

		// now instantiate the paths
		for(LEFPath lp = lefPaths; lp != null; lp = lp.nextLEFPath)
		{
			PortInst head = lp.ni[0].getPortInst(0);
			PortInst tail = lp.ni[1].getPortInst(0);
			Point2D headPt = lp.pt[0];
			Point2D tailPt = lp.pt[1];
			ArcInst ai = ArcInst.makeInstanceBase(lp.arc, lp.width, head, tail, headPt, tailPt, null);
			if (ai == null)
			{
				System.out.println("Line " + lineReader.getLineNumber() + ": Cannot create arc for PATH");
				return true;
			}
		}

		if (lefPaths == null && singlePathPoint != null && ap != null && first)
		{
			// path was a single point: plot it
			NodeProto pin = ap.findPinProto();
			if (pin != null)
			{
				double sX = pin.getDefWidth();
				double sY = pin.getDefHeight();
				NodeInst ni = NodeInst.makeInstance(pin, singlePathPoint, sX, sY, cell);
				if (ni == null)
				{
					System.out.println("Line " + lineReader.getLineNumber() + ": Cannot create pin for PATH");
					return true;
				}

				// create the port on the pin
				Export pp = newPort(cell, ni, pin.getPort(0), portname);
				if (pp != null) pp.setCharacteristic(portCharacteristics);
			}
		}
		return false;
	}

	/**
	 * Method to create a port called "thename" on port "pp" of node "ni" in cell "cell".
	 * The name is modified if it already exists.
	 */
	private Export newPort(Cell cell, NodeInst ni, PortProto pp, String thename)
	{
		String portName = thename;
		String newName = null;
		for(int i=0; ; i++)
		{
			Export e = (Export)cell.findPortProto(portName);
			if (e == null)
			{
				PortInst pi = ni.findPortInstFromProto(pp);
				return Export.newInstance(cell, pi, portName);
			}

			// make space for modified name
			newName = thename + "-" + i;
			portName = newName;
		}
	}

	private boolean readLayer(Library lib)
		throws IOException
	{
		String layerName = getAKeyword();
		if (layerName == null)
		{
			System.out.println("EOF parsing LAYER header");
			return true;
		}

		String layerType = null;
		double defWidth = -1;
		for(;;)
		{
			// get the next keyword
			String key = getAKeyword();
			if (key == null)
			{
				System.out.println("EOF parsing LAYER");
				return true;
			}

			if (key.equalsIgnoreCase("END"))
			{
				key = getAKeyword();
				break;
			}

			if (key.equalsIgnoreCase("WIDTH"))
			{
				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF reading WIDTH");
					return true;
				}
				defWidth = convertLEFString(key);
				if (ignoreToSemicolon("WIDTH")) return true;
				continue;
			}

			if (key.equalsIgnoreCase("TYPE"))
			{
				layerType = getAKeyword();
				if (ignoreToSemicolon("TYPE")) return true;
				continue;
			}

			if (key.equalsIgnoreCase("SPACING") || key.equalsIgnoreCase("PITCH") ||
				key.equalsIgnoreCase("DIRECTION") || key.equalsIgnoreCase("CAPACITANCE") ||
				key.equalsIgnoreCase("RESISTANCE"))
			{
				if (ignoreToSemicolon(key)) return true;
				continue;
			}
		}

		GetLayerInformation li = new GetLayerInformation(layerName, layerType);
		knownLayers.put(layerName, li);
		ArcProto ap = li.arc;
		if (ap != null && defWidth > 0)
			widthsFromLEF.put(ap, new Double(defWidth));
		return false;
	}

	private boolean ignoreToSemicolon(String command)
		throws IOException
	{
		// ignore up to the next semicolon
		for(;;)
		{
			String key = getAKeyword();
			if (key == null)
			{
				System.out.println("EOF parsing " + command);
				return true;
			}
			if (key.equals(";")) break;
		}
		return false;
	}

	private boolean ignoreToEnd(String endName)
		throws IOException
	{
		// ignore up to "END endName"
		boolean findEnd = true;
		for(;;)
		{
			String key = getAKeyword();
			if (key == null)
			{
				System.out.println("EOF parsing " + endName);
				return true;
			}
			if (findEnd && key.equalsIgnoreCase("END"))
			{
				key = getAKeyword();
				if (key == null)
				{
					System.out.println("EOF parsing " + endName);
					return true;
				}
				if (key.equals(endName)) break;
				continue;
			}
			if (key.equals(";")) findEnd = true; else findEnd = false;
		}
		return false;
	}

	private double convertLEFString(String key)
	{
		double v = TextUtils.atof(key) * OVERALLSCALE;
		return TextUtils.convertFromDistance(v, curTech, TextUtils.UnitScale.MICRO);
	}
}
