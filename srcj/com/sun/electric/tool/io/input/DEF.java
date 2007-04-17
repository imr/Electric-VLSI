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

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.io.IOTool;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class reads files in DEF files.
 * <BR>
 * Note that this reader was built by examining DEF files and reverse-engineering them.
 * It does not claim to be compliant with the DEF specification, but it also does not
 * claim to define a new specification.  It is merely incomplete.
 *
 * R. Reese (RBR) - modified Spring 2007 to be able to import a DEF file to a currently
 * opened View. The intended use is for the Views to either be layout or schematic.
 * If the view is layout, then all geometry is input and unrouted net connections
 * are used to maintain connectivity between logical nets and physical geometries.
 * At some point in the future, these unrouted nets need to be cleaned up, but for
 * now, the use of unrouted nets allows the layout to pass DRC and to be simulated.
 * Can also import to a schematic view - this creates a hodgepodge of icons in the
 * schematic view but net connections are correct so NCC can be used to check
 * layout vs schematic. This is useful in a hierarchical design where part of the
 * design is imported DEF (say, a standard cell layout), and the rest of the design
 * is manual layout. Having a schematic view for the imported DEF allows NCC to
 * complain less when checking the design.
 */
public class DEF extends LEFDEF
{
	private double  scaleUnits;
	private ViaDef  firstViaDef;
	private Hashtable<String,PortInst> specialNetsHT = null;
	private Hashtable<String,PortInst> normalNetsHT = null;
	private Hashtable<Double,List<NodeInst>> PortHT = null;
	private boolean schImport = false;

	private Pattern pat_starleftbracket = Pattern.compile(".*\\\\"+ "\\[");
	private Pattern pat_leftbracket = Pattern.compile("\\\\"+ "\\[");
	private Pattern pat_starrightbracket = Pattern.compile(".*\\\\"+ "\\]");
	private Pattern pat_rightbracket = Pattern.compile("\\\\"+ "\\]");

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
			readFile(lib);
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
		double v = TextUtils.atof(key) / scaleUnits;
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

				/* RBR - first, see if Cell name is equal to current cells
				 * it exists then read into cell
				 */
				cell = lib.getCurCell();
				if (Input.isNewLibraryCreated()== false)
				{
					// reading into current cell, current library
					if (cell == null)
					{
						reportError("A cell must be currently opened for this operation, aborting.");
						return true;
					}
					if (!cell.getCellName().getName().equals(cellName))
					{
						reportError("Cell name in DEF file '" + cellName + "' does not equal current cell name '" + cell.getCellName().getName() + "', aborting.");
						return true;
					}
					View cellView = cell.getCellName().getView();
					if (cellView.getAbbreviation().equals("sch"))
					{
						schImport = true; // special flag when importing into schematic view
					}
				}
				else if (cell == null || !cell.getCellName().getName().equals(cellName))
				{
					// does not equal current cell, so lets
					cell = Cell.makeInstance(lib, cellName);
				}

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

	/**
	 * Find nodeProto with same view as the parent cell
	 */
	private Cell getNodeProto(String name, Library curlib, Cell parent)
	{
		// first see if this cell is in the current library
		CellName cn;
		if (schImport)
		{
			cn = CellName.newName(name,View.ICON,0);
		} else
		{
			cn = CellName.newName(name,parent.getView(),0);
		}
		Cell cell = curlib.findNodeProto(cn.toString());
		if (cell != null) return cell;

		// now look in other libraries
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = it.next();
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

	private Cell getNodeProto(String name, Library curlib)
	{
		// first see if this cell is in the current library
		Cell cell = curlib.findNodeProto(name);
		if (cell != null) return cell;

		// now look in other libraries
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = it.next();
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

	//RBR - temp method until I figure out
	//why in Java 6.0 my use of GetOrientation
	//generates a compile error
	private Orientation FetchOrientation() throws IOException
	{
		String key = mustGetKeyword("orientation");
		if (key == null) return null;
		int angle;
		boolean transpose = false;
		if (key.equalsIgnoreCase("N"))  { angle = 0;    } else
		if (key.equalsIgnoreCase("S"))  { angle = 1800; } else
		if (key.equalsIgnoreCase("E"))  { angle = 2700; } else
		if (key.equalsIgnoreCase("W"))  { angle = 900;  } else
		if (key.equalsIgnoreCase("FN")) { angle = 900;   transpose = true; } else
		if (key.equalsIgnoreCase("FS")) { angle = 2700;  transpose = true; } else
		if (key.equalsIgnoreCase("FE")) { angle = 1800;  transpose = true; } else
		if (key.equalsIgnoreCase("FW")) { angle = 0;     transpose = true; } else
		{
			reportError("Unknown orientation (" + key + ")");
			return null;
		}
		return (Orientation.fromC(angle, transpose));
	}

	private class GetOrientation
	{
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
		}
	}

	/**
	 * Method to look for a connection to arcs of type "ap" in cell "cell"
	 * at (x, y).  The connection can not be on "not" (if it is not null).
	 * If found, return the PortInst.
	 */
	/*
	 * This function became too slow as the number of nets in cell increased.
	 * Replaced by function below. RBR Mar 2007
	 */
//	private PortInst findConnection(double x, double y, ArcProto ap, Cell cell, NodeInst noti)
//	{
//		Rectangle2D bound = new Rectangle2D.Double(x, y, 0, 0);
//		Point2D pt = new Point2D.Double(x, y);
//		for(Iterator<RTBounds> sea = cell.searchIterator(bound); sea.hasNext(); )
//		{
//			RTBounds geom = sea.next();
//			if (!(geom instanceof NodeInst)) continue;
//			NodeInst ni = (NodeInst)geom;
//			if (ni == noti) continue;
//			for(Iterator<PortInst> it = ni.getPortInsts(); it.hasNext(); )
//			{
//				PortInst pi = (PortInst)it.next();
//				if (!pi.getPortProto().connectsTo(ap)) continue;
//				Poly poly = pi.getPoly();
//				if (poly.isInside(pt)) return pi;
//			}
//		}
//		return null;
//	}
	private PortInst findConnection(double x, double y, ArcProto ap, Cell cell, NodeInst noti)
	{
		if (PortHT.containsKey(x+y)) {
			List<NodeInst> pl = PortHT.get(x+y);
			Point2D pt = new Point2D.Double(x, y);
			for (int i=0; i < pl.size(); i++)
			{
				NodeInst ni = pl.get(i);
				if (ni == noti) continue;
				for(Iterator<PortInst> it = ni.getPortInsts(); it.hasNext(); )
				{
					PortInst pi = it.next();
					if (!pi.getPortProto().connectsTo(ap)) continue;
					Poly poly = pi.getPoly();
					if (poly.isInside(pt)) return pi;
				}
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
		List<NodeInst> pl;
		if (PortHT.containsKey(x+y))
		{
			pl = PortHT.get(x+y);
		} else
		{
			pl = new ArrayList<NodeInst>();
			PortHT.put(x+y, pl);
		}
		pl.add(ni);

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

	private String translateDefName (String name)
	{
		Matcher m_starleftbracket = pat_starleftbracket.matcher(name);
		Matcher m_starrightbracket = pat_starrightbracket.matcher(name);

		if ( m_starleftbracket.matches() || m_starrightbracket.matches())
		{
			String tmpa, tmpb;
			Matcher m_leftbracket = pat_leftbracket.matcher(name);

			tmpa = m_leftbracket.replaceAll("[");
			Matcher m_rightbracket = pat_rightbracket.matcher(tmpa);
			tmpb = m_rightbracket.replaceAll("]");
			return(tmpb);
		} else
		{
			return name;
		}
	}

	private boolean readPin(Cell cell)
		throws IOException
	{
		// get the pin name
		String key = mustGetKeyword("PIN");
		if (key == null) return true;
		String pinName = translateDefName(key);
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
					if (!schImport)
					{
						GetLayerInformation li = new GetLayerInformation(key);
						if (li.pin == null)
						{
							reportError("Unknown layer (" + key + ")");
							return true;
						}
						np = li.pin;
					}
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
		if (schImport)
		{
			ArcProto apTry = null;
			for(Iterator<ArcProto> it = Technology.getCurrent().getArcs(); it.hasNext(); )
			{
				apTry = it.next();
				if (apTry.getName().equals("wire")) break;
			}
			if (apTry == null)
			{
				reportError("Unable to resolve pin component");
				return true;
			}
			for(Iterator<PrimitiveNode> it = Technology.getCurrent().getNodes(); it.hasNext(); )
			{
				PrimitiveNode loc_np = it.next();
				// must have just one port
				if (loc_np.getNumPorts() != 1) continue;

				// port must connect to both arcs
				PortProto pp = loc_np.getPort(0);
				if (pp.connectsTo(apTry)) { np = loc_np;   break; }
			}
		}

		// all factors read, now place the pin
		if (np != null && haveCoord)
		{
			// determine the pin size
			AffineTransform trans = orient.orient.pureRotate();
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

	private static Variable.Key prXkey = Variable.newKey("ATTR_prWidth");
	private static Variable.Key prYkey = Variable.newKey("ATTR_prHeight");

	/* cell is the parent cell */
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
		Cell np;
		if (cell.getView() != null)
		{
			np = getNodeProto(modelName, cell.getLibrary(), cell);
		} else
		{
			/* cell does not have a view yet, have no idea
			 * what view we need, so just get the first one
			 */
			np = getNodeProto(modelName, cell.getLibrary());
		}
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
					double nx = pt.getX();
					double ny = pt.getY();
					Orientation or = FetchOrientation();

					// place the node
					double sX = np.getDefWidth();
					double sY = np.getDefHeight();

					Variable prX = np.getVar(prXkey);
					double width=0;
					if (prX != null)
					{
						String tmps = prX.getPureValue(0);
						int tmp = Integer.parseInt(tmps);
						width = tmp;
					} else
					{
						width = sX;  //no PR boundary, use cell boundary
					}
					Variable prY = np.getVar(prYkey);
					double height=0;
					if (prY != null)
					{
						String tmps = prY.getPureValue(0);
						int tmp = Integer.parseInt(tmps);
						height = tmp;
					} else
					{
						height = sY; //no PR boundary, use cell boundary
					}

					/* DEF orientations require translations from Java orientations
					 * Need to add W, E, FW, FE support
					 */
					if (or.equals(Orientation.YRR))
					{
						// FN DEF orientation
						nx = nx + width;
					}
					if (or.equals(Orientation.Y))
					{
						// FS DEF orientation
						ny = ny + height;
					}
					if (or.equals(Orientation.RR))
					{
						// S DEF orientation
						ny = ny + height;
						nx = nx + width;
					}
					Point2D npt = new Point2D.Double(nx,ny);
					NodeInst ni = NodeInst.makeInstance(np, npt, sX, sY, cell, or, compName, 0);
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
		if (special) specialNetsHT = new Hashtable<String,PortInst>();
			else normalNetsHT = new Hashtable<String,PortInst>();
		PortHT = new Hashtable<Double,List<NodeInst>>();
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
		connectSpecialNormalNets();
		return false;
	}

	private PortInst connectGlobal(Cell cell, String portName)
	{
		PortInst pi = null;
		PortInst lastPi = null;
		NodeInst ni = null;
		PortProto pp = null;

		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			ni = it.next();
			pp = ni.getProto().findPortProto(portName);
			if (pp == null) continue;
			pi = ni.findPortInstFromProto(pp);
			if (lastPi != null)
			{
				//do connection
				ArcProto ap = Generic.tech.unrouted_arc;
				ArcInst ai = ArcInst.makeInstance(ap, ap.getDefaultLambdaFullWidth(), pi, lastPi);
				if (ai == null)
				{
					reportError("Could not create unrouted arc");
					return null;
				}
			}
			lastPi = pi;
		}
		return lastPi;
	}

	/*
	 * Look for special nets that need to be merged with normal nets
	 * Synopsys Astro router places patches of metal in special nets
	 * to cover normal nets as a method of filling notches
	 */
	private void connectSpecialNormalNets()
	{
		if (specialNetsHT == null) return;
		if (normalNetsHT == null) return;
		for (Enumeration enSpec = specialNetsHT.keys(); enSpec.hasMoreElements();)
		{
			String netName = (String)enSpec.nextElement();
			PortInst specPi = specialNetsHT.get(netName);
			PortInst normalPi = null;
			if (normalNetsHT.containsKey(netName))
			{
				normalPi = (normalNetsHT.get(netName));
				if (normalPi != null)
				{
					// create a logical net between these two points
					ArcProto ap = Generic.tech.unrouted_arc;
					ArcInst ai = ArcInst.makeInstance(ap, ap.getDefaultLambdaFullWidth(), specPi, normalPi);
					if (ai == null)
					{
						reportError("Could not create unrouted arc");
						return;
					}
				}
			}
		}
	}

	private ViaDef checkForVia (String key)
	{
		ViaDef vd = null;
		for(vd = firstViaDef; vd != null; vd = vd.nextViaDef)
			if (key.equalsIgnoreCase(vd.viaName)) break;
		if (vd == null)
		{
			// see if the via name is from the LEF file
			for(vd = firstViaDefFromLEF; vd != null; vd = vd.nextViaDef)
				if (key.equalsIgnoreCase(vd.viaName)) break;
		}
		return vd;
	}

	private boolean readNet(Cell cell, boolean special)
		throws IOException
	{
		if (schImport && special)
		{
			// when doing schematic import, ignore special nets
			ignoreToSemicolon("NET");
			return false;
		}

		// get the net name
		String key = mustGetKeyword("NET");
		if (key == null) return true;
		String netName = translateDefName(key);    // save this so net can be placed in hashtable

		// get the next keyword
		key = mustGetKeyword("NET");
		if (key == null) return true;

		// scan the "net" statement
		boolean adjustPinLocPi = false;
		boolean adjustPinLocLastPi = false;
		boolean wantPinPairs = true;
		boolean connectAllComponents = false;
		String wildcardPort = null;
		double lastX = 0, lastY = 0;
		double curX = 0, curY = 0;
		double specialWidth = 0;
		boolean pathStart = true;
		PortInst lastLogPi = null;
		PortInst lastPi = null;
		GetLayerInformation li = null;
		boolean foundCoord = false;
		boolean stackedViaFlag = false;
		for(;;)
		{
			// examine the next keyword
			if (key.equals(";"))
			{
				if (lastPi != null)
				{
					// remember at least one physical port instance for this net!
					if (special) specialNetsHT.put(netName,lastPi);
						else normalNetsHT.put(netName,lastPi);
				}
				if (lastLogPi != null && lastPi != null)
				{
					//connect logical network and physical network so that DRC passes
					ArcProto ap = Generic.tech.unrouted_arc;
					ArcInst ai = ArcInst.makeInstance(ap, ap.getDefaultLambdaFullWidth(), lastPi, lastLogPi);
					if (ai == null)
					{
						reportError("Could not create unrouted arc");
						return true;
					}
				}
				break;
			}

			if (key.equals("+"))
			{
				wantPinPairs = false;
				if (schImport)
				{
					// ignore the remainder
					ignoreToSemicolon("NET");
					break;
				}
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
					key = translateDefName(key);
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
					if (key.equals("*")) connectAllComponents = true;
						else connectAllComponents = false;
					for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
					{
						NodeInst ni = it.next();
						if (connectAllComponents || ni.getName().equalsIgnoreCase(key)) { found = ni;   break; }
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
					if (connectAllComponents) wildcardPort = key;
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
				if (IOTool.isDEFLogicalPlacement())
				{
					if (connectAllComponents)
					{
						//must connect all components in netlist
						pi = connectGlobal(cell, wildcardPort);
						if (pi == null) return true;
					} else
						if (lastLogPi != null)
						{
							ArcProto ap = Generic.tech.unrouted_arc;
							ArcInst ai = ArcInst.makeInstance(ap, ap.getDefaultLambdaFullWidth(), pi, lastLogPi);
							if (ai == null)
							{
								reportError("Could not create unrouted arc");
								return true;
							}
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
				/*
				 * Connect last created segment to logical network
				 */
				if (lastLogPi != null && lastPi != null){
					//connect logical network and physical network so that DRC passes
					ArcProto ap = Generic.tech.unrouted_arc;
					ArcInst ai = ArcInst.makeInstance(ap, ap.getDefaultLambdaFullWidth(), lastPi, lastLogPi);
					if (ai == null)
					{
						reportError("Could not create unrouted arc");
						return true;
					}
				}

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

			if (!stackedViaFlag) foundCoord = false;

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

			/*
			 * if stackedViaFlag is set, then we have already fetched
			 * this Via key word, so don't fetch next keyword
			 */
			if (!stackedViaFlag)
			{
				// get the next keyword
				key = mustGetKeyword("NET");
				if (key == null) return true;
			}

			// see if it is a via name
			ViaDef vd = checkForVia(key) ;

			// stop now if not placing physical nets
			if (!IOTool.isDEFPhysicalPlacement() || schImport)
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
					double width = li.arc.getDefaultLambdaFullWidth();
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
				/*
				 * check if next key is yet another via
				 */
				ViaDef vdStack = checkForVia(key) ;
				if (vdStack == null) stackedViaFlag = false;
				else stackedViaFlag = true;
			} else
			{
				// no via mentioned: just make a pin
				// this pin center will have to be adjusted if special! RBR
				pi = getPin(curX, curY, li.arc, cell);
				if (pi == null) return true;
				adjustPinLocPi = true;
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
				double width = li.arc.getDefaultLambdaFullWidth();
				if (special) width = specialWidth; else
				{
					// get the width from the LEF file
					Double wid = widthsFromLEF.get(li.arc);
					if (wid != null) width = wid.doubleValue();
				}
				if (adjustPinLocLastPi && special)
				{
					// starting pin; have to adjust the last pin location
					double dX = 0;
					double dY = 0;
					if (curX != lastX)
					{
						//horizontal route
						dX = width/2;  // default, adjust left
						if (curX < lastX)
						{
							dX = -dX; // route runs right to left, adjust right
						}
					}
					if (curY != lastY)
					{
						// vertical route
						dY = width/2; // default, adjust up
						if (curY < lastY)
						{
							dY = -dY; // route runs top to bottom, adjust down
						}
					}
					lastPi.getNodeInst().move(dX,dY);
					adjustPinLocLastPi = false;
				}

				/* note that this adjust is opposite of previous since
				 * this pin is on the end of the wire instead of the beginning
				 */
				if (adjustPinLocPi && special)
				{
					// ending pin; have to adjust the last pin location
					double dX = 0;
					double dY = 0;
					if (curX != lastX)
					{
						// horizontal route
						dX = -width/2;  // default, adjust right
						if (curX < lastX)
						{
							dX = -dX; // route runs right to left, adjust left
						}
					}
					if (curY != lastY)
					{
						// vertical route
						dY = -width/2; // default, adjust down
						if (curY < lastY)
						{
							dY = -dY; //route runs top to bottom, adjust up
						}
					}
					pi.getNodeInst().move(dX,dY);
					adjustPinLocPi = false;
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
			adjustPinLocLastPi=adjustPinLocPi;
			adjustPinLocPi = false;

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
					double width = li.arc.getDefaultLambdaFullWidth();
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
		if (schImport)
		{
			ignoreToSemicolon("VIA");
			return false;
		}

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
