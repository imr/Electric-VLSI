/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DEF.java
 * Input/output tool: DEF (Design Exchange Format) reader
 * Written by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.input.LEFDEF.GetLayerInformation;
import com.sun.electric.tool.io.input.LEFDEF.ViaDef;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Iterator;

/**
 * This class reads files in DEF files.
 * <BR>
 * Note that this reader was built by examining DEF files and reverse-engineering them.
 * It does not claim to be compliant with the DEF specification, but it also does not
 * claim to define a new specification.  It is merely incomplete.
 */
public class DEF extends LEFDEF
{
	private double  scaleUnits;
	private ViaDef  firstViaDef;

	/**
	 * Method to import a library from disk.
	 * @param lib the library to fill
	 * @return true on error.
	 */
	protected boolean importALibrary(Library lib)
	{
		initKeywordParsing();
		scaleUnits = 1000;
		firstViaDef = null;

		// read the file
		try
		{
			boolean ret = readFile(lib);
		} catch (IOException e)
		{
			System.out.println("ERROR reading DEF libraries");
		}
		return false;
	}

	private boolean ignoreToSemicolon(String command)
		throws IOException
	{
		// ignore up to the next semicolon
		for(;;)
		{
			String key = mustGetKeyword(command);
			if (key == null) return true;
			if (key.equals(";")) break;
		}
		return false;
	}

	private String mustGetKeyword(String where)
		throws IOException
	{
		String key = getAKeyword();
		if (key == null) reportError("EOF parsing " + where);
		return key;
	}

	private double convertDEFString(String key)
	{
		double v = TextUtils.atof(key) / scaleUnits / 2;
		return TextUtils.convertFromDistance(v, Technology.getCurrent(), TextUtils.UnitScale.MICRO);
	}

	private void reportError(String command)
	{
		System.out.println("File " + filePath + ", line " + lineReader.getLineNumber() + ": " + command);
	}

	/**
	 * Method to read the DEF file.
	 */
	private boolean readFile(Library lib)
		throws IOException
	{
		Cell cell = null;
		for(;;)
		{
			// get the next keyword
			String key = getAKeyword();
			if (key == null) break;
			if (key.equalsIgnoreCase("VERSION") || key.equalsIgnoreCase("NAMESCASESENSITIVE") ||
				key.equalsIgnoreCase("DIVIDERCHAR") || key.equalsIgnoreCase("BUSBITCHARS") ||
				key.equalsIgnoreCase("DIEAREA") || key.equalsIgnoreCase("ROW") ||
				key.equalsIgnoreCase("TRACKS") || key.equalsIgnoreCase("GCELLGRID") ||
				key.equalsIgnoreCase("HISTORY") || key.equalsIgnoreCase("TECHNOLOGY"))
			{
				if (ignoreToSemicolon(key)) return true;
				continue;
			}
			if (key.equalsIgnoreCase("DEFAULTCAP") || key.equalsIgnoreCase("REGIONS"))
			{
				if (ignoreBlock(key)) return true;
				continue;
			}
			if (key.equalsIgnoreCase("DESIGN"))
			{
				String cellName = mustGetKeyword("DESIGN");
				if (cellName == null) return true;
				cell = Cell.makeInstance(lib, cellName);
				if (cell == null)
				{
					reportError("Cannot create cell '" + cellName + "'");
					return true;
				}
				if (ignoreToSemicolon("DESIGN")) return true;
				continue;
			}

			if (key.equalsIgnoreCase("UNITS"))
			{
				if (readUnits()) return true;
				continue;
			}

			if (key.equalsIgnoreCase("PROPERTYDEFINITIONS"))
			{
				if (readPropertyDefinitions()) return true;
				continue;
			}

			if (key.equalsIgnoreCase("VIAS"))
			{
				if (readVias(cell)) return true;
				continue;
			}

			if (key.equalsIgnoreCase("COMPONENTS"))
			{
				if (readComponents(cell)) return true;
				continue;
			}

			if (key.equalsIgnoreCase("PINS"))
			{
				if (readPins(cell)) return true;
				continue;
			}

			if (key.equalsIgnoreCase("SPECIALNETS"))
			{
				if (readNets(cell, true)) return true;
				continue;
			}

			if (key.equalsIgnoreCase("NETS"))
			{
				if (readNets(cell, false)) return true;
				continue;
			}

			if (key.equalsIgnoreCase("END"))
			{
				key = getAKeyword();
				break;
			}
		}
		return false;
	}

	private boolean ignoreBlock(String command)
		throws IOException
	{
		for(;;)
		{
			// get the next keyword
			String key = mustGetKeyword(command);
			if (key == null) return true;

			if (key.equalsIgnoreCase("END"))
			{
				getAKeyword();
				break;
			}
		}
		return false;
	}

	private Point2D readCoordinate()
		throws IOException
	{
		// get "("
		String key = mustGetKeyword("coordinate");
		if (key == null) return null;
		if (!key.equals("("))
		{
			reportError("Expected '(' in coordinate");
			return null;
		}

		// get X
		key = mustGetKeyword("coordinate");
		if (key == null) return null;
		double x = convertDEFString(key);

		// get Y
		key = mustGetKeyword("coordinate");
		if (key == null) return null;
		double y = convertDEFString(key);

		// get ")"
		key = mustGetKeyword("coordinate");
		if (key == null) return null;
		if (!key.equals(")"))
		{
			reportError("Expected ')' in coordinate");
			return null;
		}
		return new Point2D.Double(x, y);
	}

	private Cell getNodeProto(String name, Library curlib)
	{
		// first see if this cell is in the current library
		Cell cell = curlib.findNodeProto(name);
		if (cell != null) return cell;

		// now look in other libraries
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			if (lib.isHidden()) continue;
			if (lib == curlib) continue;
			cell = lib.findNodeProto(name);
			if (cell != null)
			{
				// must copy the cell
//				Cell newCell = copyrecursively(cell, cell->protoname, curlib, cell->cellview,
//					FALSE, FALSE, "", FALSE, FALSE, TRUE, new HashSet());
//				return newCell;
				return cell;
			}
		}
		return null;
	}

	private class GetOrientation
	{
//		private int angle;
//		private boolean mX, mY;
        private Orientation orient;

		private GetOrientation()
			throws IOException
		{
			String key = mustGetKeyword("orientation");
			if (key == null) return;
            int angle;
			boolean transpose = false;
			if (key.equalsIgnoreCase("N")) { angle = 0; } else
			if (key.equalsIgnoreCase("S")) { angle = 1800; } else
			if (key.equalsIgnoreCase("E")) { angle = 2700; } else
			if (key.equalsIgnoreCase("W")) { angle = 900; } else
			if (key.equalsIgnoreCase("FN")) { angle = 900;  transpose = true; } else
			if (key.equalsIgnoreCase("FS")) { angle = 2700; transpose = true; } else
			if (key.equalsIgnoreCase("FE")) { angle = 1800; transpose = true; } else
			if (key.equalsIgnoreCase("FW")) { angle = 0;    transpose = true; } else
			{
				reportError("Unknown orientation (" + key + ")");
				return;
			}
    		orient = Orientation.fromC(angle, transpose);
//			angle = or.getAngle();
//			mX = or.isXMirrored();
//			mY = or.isYMirrored();
		}
	}

	/**
	 * Method to look for a connection to arcs of type "ap" in cell "cell"
	 * at (x, y).  The connection can not be on "not" (if it is not null).
	 * If found, return the PortInst.
	 */
	private PortInst findConnection(double x, double y, ArcProto ap, Cell cell, NodeInst noti)
	{
		Rectangle2D bound = new Rectangle2D.Double(x, y, 0, 0);
		Point2D pt = new Point2D.Double(x, y);
		for(Iterator<Geometric> sea = cell.searchIterator(bound); sea.hasNext(); )
		{
			Geometric geom = (Geometric)sea.next();
			if (!(geom instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)geom;
			if (ni == noti) continue;
			for(Iterator<PortInst> it = ni.getPortInsts(); it.hasNext(); )
			{
				PortInst pi = (PortInst)it.next();
				if (!pi.getPortProto().connectsTo(ap)) continue;
				Poly poly = pi.getPoly();
				if (poly.isInside(pt)) return pi;
			}
		}
		return null;
	}

	/**
	 * Method to look for a connection to arcs of type "ap" in cell "cell"
	 * at (x, y).  If nothing is found, create a pin.  In any case, return
	 * the PortInst.  Returns null on error.
	 */
	private PortInst getPin(double x, double y, ArcProto ap, Cell cell)
	{
		// if there is an existing connection, return it
		PortInst pi = findConnection(x, y, ap, cell, null);
		if (pi != null) return pi;

		// nothing found at this location: create a pin
		NodeProto pin = ap.findPinProto();
		double sX = pin.getDefWidth();
		double sY = pin.getDefHeight();
		NodeInst ni = NodeInst.makeInstance(pin, new Point2D.Double(x, y), sX, sY, cell);
		if (ni == null)
		{
			reportError("Unable to create net pin");
			return null;
		}
		return ni.getOnlyPortInst();
	}

	/*************** PINS ***************/

	private boolean readPins(Cell cell)
		throws IOException
	{
		if (ignoreToSemicolon("PINS")) return true;
		for(;;)
		{
			// get the next keyword
			String key = mustGetKeyword("PINs");
			if (key == null) return true;
			if (key.equals("-"))
			{
				if (readPin(cell)) return true;
				continue;
			}

			if (key.equalsIgnoreCase("END"))
			{
				key = getAKeyword();
				break;
			}

			// ignore the keyword
			if (ignoreToSemicolon(key)) return true;
		}
		return false;
	}

	private boolean readPin(Cell cell)
		throws IOException
	{
		// get the pin name
		String key = mustGetKeyword("PIN");
		if (key == null) return true;
		String pinName = key;
		PortCharacteristic portCharacteristic = null;
		NodeProto np = null;
		Point2D ll = null, ur = null, xy = null;
		boolean haveCoord = false;
		GetOrientation orient = null;

		for(;;)
		{
			// get the next keyword
			key = mustGetKeyword("PIN");
			if (key == null) return true;
			if (key.equals("+"))
			{
				key = mustGetKeyword("PIN");
				if (key == null) return true;
				if (key.equalsIgnoreCase("NET"))
				{
					key = mustGetKeyword("net name");
					if (key == null) return true;
					continue;
				}
				if (key.equalsIgnoreCase("DIRECTION"))
				{
					key = mustGetKeyword("DIRECTION");
					if (key == null) return true;
					if (key.equalsIgnoreCase("INPUT")) portCharacteristic = PortCharacteristic.IN; else
					if (key.equalsIgnoreCase("OUTPUT")) portCharacteristic = PortCharacteristic.OUT; else
					if (key.equalsIgnoreCase("INOUT")) portCharacteristic = PortCharacteristic.BIDIR; else
					if (key.equalsIgnoreCase("FEEDTHRU")) portCharacteristic = PortCharacteristic.BIDIR; else
					{
						reportError("Unknown direction (" + key + ")");
						return true;
					}
					continue;
				}
				if (key.equalsIgnoreCase("USE"))
				{
					key = mustGetKeyword("USE");
					if (key == null) return true;
					if (key.equalsIgnoreCase("SIGNAL")) ; else
					if (key.equalsIgnoreCase("POWER")) portCharacteristic = PortCharacteristic.PWR; else
					if (key.equalsIgnoreCase("GROUND")) portCharacteristic = PortCharacteristic.GND; else
					if (key.equalsIgnoreCase("CLOCK")) portCharacteristic = PortCharacteristic.CLK; else
					if (key.equalsIgnoreCase("TIEOFF")) ; else
					if (key.equalsIgnoreCase("ANALOG")) ; else
					{
						reportError("Unknown usage (" + key + ")");
						return true;
					}
					continue;
				}
				if (key.equalsIgnoreCase("LAYER"))
				{
					key = mustGetKeyword("LAYER");
					if (key == null) return true;
					GetLayerInformation li = new GetLayerInformation(key);
					if (li.pin == null)
					{
						reportError("Unknown layer (" + key + ")");
						return true;
					}
					np = li.pin;
					ll = readCoordinate();
					if (ll == null) return true;
					ur = readCoordinate();
					if (ur == null) return true;
					continue;
				}
				if (key.equalsIgnoreCase("PLACED"))
				{
					// get pin location and orientation
					xy = readCoordinate();
					if (xy == null) return true;
					orient = new GetOrientation();
					haveCoord = true;
					continue;
				}
				continue;
			}

			if (key.equals(";"))
				break;
		}

		// all factors read, now place the pin
		if (np != null && haveCoord)
		{
			// determine the pin size
			AffineTransform trans = orient.orient.pureRotate();
//			AffineTransform trans = NodeInst.pureRotate(orient.angle, orient.mX, orient.mY);
			trans.transform(ll, ll);
			trans.transform(ur, ur);
			double sX = Math.abs(ll.getX() - ur.getX());
			double sY = Math.abs(ll.getY() - ur.getY());
			double cX = (ll.getX() + ur.getX()) / 2 + xy.getX();
			double cY = (ll.getY() + ur.getY()) / 2 + xy.getY();

			// make the pin
			NodeInst ni = NodeInst.makeInstance(np, new Point2D.Double(cX, cY), sX, sY, cell);
			if (ni == null)
			{
				reportError("Unable to create pin");
				return true;
			}
			PortInst pi = ni.findPortInstFromProto(np.getPort(0));
			Export e = Export.newInstance(cell, pi, pinName);
			if (e == null)
			{
				reportError("Unable to create pin name");
				return true;
			}
			e.setCharacteristic(portCharacteristic);
		}
		return false;
	}

	/*************** COMPONENTS ***************/

	private boolean readComponents(Cell cell)
		throws IOException
	{
		if (ignoreToSemicolon("COMPONENTS")) return true;
		for(;;)
		{
			// get the next keyword
			String key = mustGetKeyword("COMPONENTs");
			if (key == null) return true;
			if (key.equals("-"))
			{
				if (readComponent(cell)) return true;
				continue;
			}

			if (key.equalsIgnoreCase("END"))
			{
				key = getAKeyword();
				break;
			}

			// ignore the keyword
			if (ignoreToSemicolon(key)) return true;
		}
		return false;
	}

	private boolean readComponent(Cell cell)
		throws IOException
	{
		// get the component name and model name
		String key = mustGetKeyword("COMPONENT");
		if (key == null) return true;
		String compName = key;
		key = mustGetKeyword("COMPONENT");
		if (key == null) return true;
		String modelName = key;

		// find the named cell
		Cell np = getNodeProto(modelName, cell.getLibrary());
		if (np == null)
		{
			reportError("Unknown cell (" + modelName + ")");
			return true;
		}

		for(;;)
		{
			// get the next keyword
			key = mustGetKeyword("COMPONENT");
			if (key == null) return true;
			if (key.equals("+"))
			{
				key = mustGetKeyword("COMPONENT");
				if (key == null) return true;
				if (key.equalsIgnoreCase("PLACED") || key.equalsIgnoreCase("FIXED"))
				{
					// handle placement
					Point2D pt = readCoordinate();
					if (pt == null) return true;
					GetOrientation or = new GetOrientation();

					// place the node
					double sX = np.getDefWidth();
					double sY = np.getDefHeight();
//					if (or.mX) sX = -sX;
//					if (or.mY) sY = -sY;
					NodeInst ni = NodeInst.makeInstance(np, pt, sX, sY, cell, or.orient, compName, 0);
//					NodeInst ni = NodeInst.makeInstance(np, pt, sX, sY, cell, or.angle, compName, 0);
					if (ni == null)
					{
						reportError("Unable to create node");
						return true;
					}
					continue;
				}
				continue;
			}

			if (key.equals(";")) break;
		}
		return false;
	}

	/*************** NETS ***************/

	private boolean readNets(Cell cell, boolean special)
		throws IOException
	{
		for(;;)
		{
			// get the next keyword
			String key = mustGetKeyword("NETs");
			if (key == null) return true;
			if (key.equals("-"))
			{
				if (readNet(cell, special)) return true;
				continue;
			}
			if (key.equalsIgnoreCase("END"))
			{
				key = getAKeyword();
				break;
			}

			// ignore the keyword
			if (ignoreToSemicolon(key)) return true;
		}
		return false;
	}

	private boolean readNet(Cell cell, boolean special)
		throws IOException
	{
		// get the net name
		String key = mustGetKeyword("NET");
		if (key == null) return true;

		// get the next keyword
		key = mustGetKeyword("NET");
		if (key == null) return true;

		// scan the "net" statement
		boolean wantPinPairs = true;
		double lastX = 0, lastY = 0;
		double curX = 0, curY = 0;
		double specialWidth = 0;
		boolean pathStart = true;
		PortInst lastLogPi = null;
		PortInst lastPi = null;
		GetLayerInformation li = null;
		for(;;)
		{
			// examine the next keyword
			if (key.equals(";")) break;

			if (key.equals("+"))
			{
				wantPinPairs = false;
				key = mustGetKeyword("NET");
				if (key == null) return true;

				if (key.equalsIgnoreCase("USE"))
				{
					// ignore "USE" keyword
					key = mustGetKeyword("NET");
					if (key == null) return true;
				} else if (key.equalsIgnoreCase("ROUTED"))
				{
					// handle "ROUTED" keyword
					key = mustGetKeyword("NET");
					if (key == null) return true;
					li = new GetLayerInformation(key);
					if (li.pin == null)
					{
						reportError("Unknown layer (" + key + ")");
						return true;
					}
					pathStart = true;
					if (special)
					{
						// specialnets have width here
						key = mustGetKeyword("NET");
						if (key == null) return true;
						specialWidth = convertDEFString(key);
					}
				} else if (key.equalsIgnoreCase("FIXED"))
				{
					// handle "FIXED" keyword
					key = mustGetKeyword("NET");
					if (key == null) return true;
					li = new GetLayerInformation(key);
					if (li.pin == null)
					{
						reportError("Unknown layer (" + key + ")");
						return true;
					}
					pathStart = true;
				} else if (key.equalsIgnoreCase("SHAPE"))
				{
					// handle "SHAPE" keyword
					key = mustGetKeyword("NET");
					if (key == null) return true;
				} else
				{
					reportError("Cannot handle '" + key + "' nets");
					return true;
				}

				// get next keyword
				key = mustGetKeyword("NET");
				if (key == null) return true;
				continue;
			}

			// if still parsing initial pin pairs, do so
			if (wantPinPairs)
			{
				// it must be the "(" of a pin pair
				if (!key.equals("("))
				{
					reportError("Expected '(' of pin pair");
					return true;
				}

				// get the pin names
				key = mustGetKeyword("NET");
				if (key == null) return true;
				PortInst pi = null;
				if (key.equalsIgnoreCase("PIN"))
				{
					// find the export
					key = mustGetKeyword("NET");
					if (key == null) return true;
					Export pp = (Export)cell.findPortProto(key);
					if (pp == null)
					{
						reportError("Warning: unknown pin '" + key + "'");
						if (ignoreToSemicolon("NETS")) return true;
						return false;
					}
					pi = pp.getOriginalPort();
				} else
				{
					NodeInst found = null;
					for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
					{
						NodeInst ni = (NodeInst)it.next();
						if (ni.getName().equalsIgnoreCase(key)) { found = ni;   break; }
					}
					if (found == null)
					{
						reportError("Unknown component '" + key + "'");
						return true;
					}

					// get the port name
					key = mustGetKeyword("NET");
					if (key == null) return true;
					PortProto pp = found.getProto().findPortProto(key);
					if (pp == null)
					{
						reportError("Unknown port '" + key + "' on component " + found);
						return true;
					}
					pi = found.findPortInstFromProto(pp);
				}

				// get the close parentheses
				key = mustGetKeyword("NET");
				if (key == null) return true;
				if (!key.equals(")"))
				{
					reportError("Expected ')' of pin pair");
					return true;
				}

				if (lastLogPi != null && IOTool.isDEFLogicalPlacement())
				{
					ArcProto ap = Generic.tech.unrouted_arc;
					ArcInst ai = ArcInst.makeInstance(ap, ap.getDefaultWidth(), pi, lastLogPi);
					if (ai == null)
					{
						reportError("Could not create unrouted arc");
						return true;
					}
				}
				lastLogPi = pi;

				// get the next keyword and continue parsing
				key = mustGetKeyword("NET");
				if (key == null) return true;
				continue;
			}

			// handle "new" start of coordinate trace
			if (key.equalsIgnoreCase("NEW"))
			{
				key = mustGetKeyword("NET");
				if (key == null) return true;
				li = new GetLayerInformation(key);
				if (li.pin == null)
				{
					reportError("Unknown layer (" + key + ")");
					return true;
				}
				pathStart = true;
				key = mustGetKeyword("NET");
				if (key == null) return true;
				if (special)
				{
					// specialnets have width here
					specialWidth = convertDEFString(key);

					// get the next keyword
					key = mustGetKeyword("NET");
					if (key == null) return true;
				}
				continue;
			}

			boolean foundCoord = false;
			if (key.equals("("))
			{
				// get the X coordinate
				foundCoord = true;
				key = mustGetKeyword("NET");
				if (key == null) return true;
				if (key.equals("*")) curX = lastX; else
				{
					curX = convertDEFString(key);
				}

				// get the Y coordinate
				key = mustGetKeyword("NET");
				if (key == null) return true;
				if (key.equals("*")) curY = lastY; else
				{
					curY = convertDEFString(key);
				}

				// get the close parentheses
				key = mustGetKeyword("NET");
				if (key == null) return true;
				if (!key.equals(")"))
				{
					reportError("Expected ')' of coordinate pair");
					return true;
				}
			}

			// get the next keyword
			key = mustGetKeyword("NET");
			if (key == null) return true;

			// see if it is a via name
			ViaDef vd = null;
			for(vd = firstViaDef; vd != null; vd = vd.nextViaDef)
				if (key.equalsIgnoreCase(vd.viaName)) break;
			if (vd == null)
			{
				// see if the via name is from the LEF file
				for(vd = firstViaDefFromLEF; vd != null; vd = vd.nextViaDef)
					if (key.equalsIgnoreCase(vd.viaName)) break;
			}

			// stop now if not placing physical nets
			if (!IOTool.isDEFPhysicalPlacement())
			{
				// ignore the next keyword if a via name is coming
				if (vd != null)
				{
					key = mustGetKeyword("NET");
					if (key == null) return true;
				}
				continue;
			}

			// if a via is mentioned next, use it
			PortInst pi = null;
			boolean placedVia = false;
			if (vd != null)
			{
				// place the via at this location
				double sX = vd.sX;
				double sY = vd.sY;
				if (vd.via == null)
				{
					reportError("Cannot to create via");
					return true;
				}

				// see if there is a connection point here when starting a path
				if (pathStart)
				{
					lastPi = findConnection(curX, curY, li.arc, cell, null);
				}

				// create the via
				SizeOffset so = vd.via.getProtoSizeOffset();
				sX += so.getLowXOffset() + so.getHighXOffset();
				sY += so.getLowYOffset() + so.getHighYOffset();
				NodeInst ni = NodeInst.makeInstance(vd.via, new Point2D.Double(curX, curY), sX, sY, cell);
				if (ni == null)
				{
					reportError("Unable to create via layer");
					return true;
				}
				pi = ni.getOnlyPortInst();

				// if the path starts with a via, wire it
				if (pathStart && lastPi != null && foundCoord)
				{
					double width = li.arc.getDefaultWidth();
					if (special) width = specialWidth; else
					{
						// get the width from the LEF file
						Double wid = widthsFromLEF.get(li.arc);
						if (wid != null) width = wid.doubleValue();
					}
					ArcInst ai = ArcInst.makeInstance(li.arc, width, lastPi, pi);
					if (ai == null)
					{
						reportError("Unable to create net starting point");
						return true;
					}
				}

				// remember that a via was placed
				placedVia = true;

				// get the next keyword
				key = mustGetKeyword("NET");
				if (key == null) return true;
			} else
			{
				// no via mentioned: just make a pin
				pi = getPin(curX, curY, li.arc, cell);
				if (pi == null) return true;
			}
			if (!foundCoord) continue;

			// run the wire
			if (!pathStart)
			{
				// make sure that this arc can connect to the current pin
				if (!pi.getPortProto().connectsTo(li.arc))
				{
					NodeProto np = li.arc.findPinProto();
					double sX = np.getDefWidth();
					double sY = np.getDefHeight();
					NodeInst ni = NodeInst.makeInstance(np, new Point2D.Double(curX, curY), sX, sY, cell);
					if (ni == null)
					{
						reportError("Unable to create net pin");
						return true;
					}
					pi = ni.getOnlyPortInst();
				}

				// run the wire
				double width = li.arc.getDefaultWidth();
				if (special) width = specialWidth; else
				{
					// get the width from the LEF file
					Double wid = widthsFromLEF.get(li.arc);
					if (wid != null) width = wid.doubleValue();
				}
				ArcInst ai = ArcInst.makeInstance(li.arc, width, lastPi, pi);
				if (ai == null)
				{
					reportError("Unable to create net path");
					return true;
				}
			}
			lastX = curX;   lastY = curY;
			pathStart = false;
			lastPi = pi;

			// switch layers to the other one supported by the via
			if (placedVia)
			{
				if (li.arc == vd.lay1)
				{
					li.arc = vd.lay2;
				} else if (li.arc == vd.lay2)
				{
					li.arc = vd.lay1;
				}
				li.pin = li.arc.findPinProto();
			}

			// if the path ends here, connect it
			if (key.equalsIgnoreCase("NEW") || key.equals(";"))
			{
				// see if there is a connection point here when starting a path
				PortInst nextPi = findConnection(curX, curY, li.arc, cell, pi.getNodeInst());

				// if the path starts with a via, wire it
				if (nextPi != null)
				{
					double width = li.arc.getDefaultWidth();
					if (special) width = specialWidth; else
					{
						// get the width from the LEF file
						Double wid = widthsFromLEF.get(li.arc);
						if (wid != null) width = wid.doubleValue();
					}
					ArcInst ai = ArcInst.makeInstance(li.arc, width, pi, nextPi);
					if (ai == null)
					{
						reportError("Unable to create net ending point");
						return true;
					}
				}
			}
		}
		return false;
	}

	/*************** VIAS ***************/

	private boolean readVias(Cell cell)
		throws IOException
	{
		if (ignoreToSemicolon("VIAS")) return true;
		for(;;)
		{
			// get the next keyword
			String key = mustGetKeyword("VIAs");
			if (key == null) return true;
			if (key.equals("-"))
			{
				if (readVia()) return true;
				continue;
			}

			if (key.equalsIgnoreCase("END"))
			{
				key = getAKeyword();
				break;
			}

			// ignore the keyword
			if (ignoreToSemicolon(key)) return true;
		}
		return false;
	}

	private boolean readVia()
		throws IOException
	{
		// get the via name
		String key = mustGetKeyword("VIA");
		if (key == null) return true;

		// create a new via definition
		ViaDef vd = new ViaDef();
		vd.viaName = key;
		vd.sX = vd.sY = 0;
		vd.via = null;
		vd.lay1 = vd.lay2 = null;
		vd.nextViaDef = firstViaDef;
		firstViaDef = vd;

		for(;;)
		{
			// get the next keyword
			key = mustGetKeyword("VIA");
			if (key == null) return true;
			if (key.equals("+"))
			{
				key = mustGetKeyword("VIA");
				if (key == null) return true;
				if (key.equalsIgnoreCase("RECT"))
				{
					// handle definition of a via rectangle
					key = mustGetKeyword("VIA");
					if (key == null) return true;
					GetLayerInformation li = new GetLayerInformation(key);
					if (li.pure == null)
					{
						reportError("Layer " + key + " not in current technology");
					}
					if (key.startsWith("VIA"))
					{
						if (li.pin == null) li.pin = Generic.tech.universalPinNode;
						vd.via = li.pin;
					}
					if (key.startsWith("METAL"))
					{
						if (li.arc == null) li.arc = Generic.tech.universal_arc;
						if (vd.lay1 == null) vd.lay1 = li.arc; else
							vd.lay2 = li.arc;
					}
					Point2D ll = readCoordinate();
					if (ll == null) return true;
					Point2D ur = readCoordinate();
					if (ur == null) return true;

					// accumulate largest contact size
					if (ur.getX()-ll.getX() > vd.sX) vd.sX = ur.getX() - ll.getX();
					if (ur.getY()-ll.getY() > vd.sY) vd.sY = ur.getY() - ll.getY();
					continue;
				}
				continue;
			}

			if (key.equals(";")) break;
		}
		if (vd.via != null)
		{
			if (vd.sX == 0) vd.sX = vd.via.getDefWidth();
			if (vd.sY == 0) vd.sY = vd.via.getDefHeight();
		}
		return false;
	}

	/*************** PROPERTY DEFINITIONS ***************/

	private boolean readPropertyDefinitions()
		throws IOException
	{
		for(;;)
		{
			// get the next keyword
			String key = mustGetKeyword("PROPERTYDEFINITION");
			if (key == null) return true;
			if (key.equalsIgnoreCase("END"))
			{
				key = getAKeyword();
				break;
			}

			// ignore the keyword
			if (ignoreToSemicolon(key)) return true;
		}
		return false;
	}

	/*************** UNITS ***************/

	private boolean readUnits()
		throws IOException
	{
		// get the "DISTANCE" keyword
		String key = mustGetKeyword("UNITS");
		if (key == null) return true;
		if (!key.equalsIgnoreCase("DISTANCE"))
		{
			reportError("Expected 'DISTANCE' after 'UNITS'");
			return true;
		}

		// get the "MICRONS" keyword
		key = mustGetKeyword("UNITS");
		if (key == null) return true;
		if (!key.equalsIgnoreCase("MICRONS"))
		{
			reportError("Expected 'MICRONS' after 'UNITS'");
			return true;
		}

		// get the amount
		key = mustGetKeyword("UNITS");
		if (key == null) return true;
		scaleUnits = TextUtils.atof(key);

		// ignore the keyword
		if (ignoreToSemicolon("UNITS")) return true;
		return false;
	}

}
