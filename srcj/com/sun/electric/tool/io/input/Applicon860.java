/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Applicon860.java
 * Input tool: Applicon\860 input
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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
 *
 * This code only reads Apple/860 format, and it does not handle many of the
 * features.  It is built from Applicon document A-20482-002, November 1983.
 * The features that are not included are:
 * > Only polygons are read, not paths, areas, or CWHA rectangles
 * > Cell instances cannot scale or stretch and must be manhattan
 * > Groups are ignored
 * > Auxiliary records are ignored
 * > Multilevel polygons are not handled (must be on only one level)
 * > Polygon records must use default layer and not have their own color
 * > Text is handled poorly and should be commented out (see below)
 * The code can accomodate tape or disk format files with optional byte swapping.
 */
package com.sun.electric.tool.io.input;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.TextUtils.UnitScale;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Technology.NodeLayer;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.Job;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URL;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Applicon860 extends Input
{
	private static final int POLYSPLIT    = 999;	/* maximum polygon record size (was 62) */
	private static final int BYTESSWAPPED = 1;		/* bit set if bytes are swapped */
	private static final int TAPEFORMAT   = 2;		/* bit set if file is tape-blocked */
	private static final int INITIALIZED  = 4;		/* bit set if file status is known */

	private Map<Integer,PrimitiveNode> appleNodeMap = new HashMap<Integer,PrimitiveNode>();
	private int appleState;

	private int [] polylist;
	private int polylistcount = 0;
	private int [] px, py;
	private Point2D [] ptrace;
	private Applicon860Preferences localPrefs;

	public static class Applicon860Preferences extends InputPreferences
    {
		public Applicon860Preferences(boolean factory) { super(factory); }

        @Override
        public Library doInput(URL fileURL, Library lib, Technology tech, Map<Library,Cell> currentCells, Map<CellId,BitSet> nodesToExpand, Job job)
        {
        	Applicon860 in = new Applicon860(this);
			if (in.openBinaryInput(fileURL)) return null;
			lib = in.importALibrary(lib, tech, currentCells);
			in.closeInput();
			return lib;
        }
    }

	/**
	 * Creates a new instance of Applicon860.
	 */
	Applicon860(Applicon860Preferences ap) { localPrefs = ap; }

	/**
	 * Method to import a library from disk.
	 * @param lib the library to fill
     * @param currentCells this map will be filled with currentCells in Libraries found in library file
	 * @return the created library (null on error).
	 */
    @Override
	protected Library importALibrary(Library lib, Technology tech, Map<Library,Cell> currentCells)
	{
		try
		{
			if (!readAppleLibrary(lib, tech)) return null; // error reading the file
		} catch (IOException e) {}
		return lib;
	}

	/**
	 * Method to read the Applicon file.
	 * @param lib the Library to fill.
	 */
    /**
     * Method to read the Applicon file.
     * @param lib the Library to fill.
     * @return True if no error was found
     * @throws IOException
     */
    private boolean readAppleLibrary(Library lib, Technology tech)
		throws IOException
	{
		String str;
		int chain, length, type, i, eitype, offset, lvlgrp, layer, polyptr, nfl, nfh, nfe, tcount,
			st, levels, a1, a2, a3, a4, lx, hx, ly, hy;
		int [] cidata = new int[32];
		String name, units, cellname;
		int x1, y1, xoff = 0, yoff = 0;
		Cell curfacet, subnp = null;
		PrimitiveNode layernp = null;
		NodeInst ni = null;
		double scale = 0;

		// establish linkage between Apple numbers and primitives
		setupAppleLayers(tech);

		curfacet = null;
		Set<Integer> notFound = new HashSet<Integer>();
		appleState = 0;
		for(;;)
		{
			chain = appleGetChain();
			if (chain != 3 && chain != 1)
			{
				System.out.println("CHAIN=" + chain + " at byte " + byteCount);
				return false;
			}
			length = appleGetWord();
			type = appleGetWord();
			switch (type)
			{
				case 2:		// top global parameter record
				case 254:		// cell global parameter record
					if (length != 26)
					{
						if (type == 2)
							System.out.println("Top global parameter length=" + length); else
								System.out.println("Cell global parameter length=" + length);
						return false;
					}
					name = "";
					for(int k=0; k<30; k++) name += dataInputStream.readByte();
					st = name.indexOf(']') + 1;
					i = name.indexOf('.', st);
					if (i >= 0) name = name.substring(0, i);

					units = "";
					for(int k=0; k<4; k++) units += dataInputStream.readByte();
					nfl = appleGetWord();
					nfh = appleGetWord();
					nfe = appleGetWord();
					scale = appleFloat(nfl, nfh, nfe) / 2147483648.0;
					if (units.equals("MIL "))
					{
						// 25.4 microns to the mil
						scale = TextUtils.convertFromDistance(scale*25.4, tech, UnitScale.MICRO);
					} else if (units.equals("INCH"))
					{
						// 25400 microns to the inch
						TextUtils.convertFromDistance(scale*25400.0, tech, UnitScale.MICRO);
					} else
					{
						// presume microns
						scale = TextUtils.convertFromDistance(scale, tech, UnitScale.MICRO);
					}
					xoff = appleGetWord() & 0xFFFF;
					xoff |= (appleGetWord() & 0xFFFF) << 16;
					yoff = appleGetWord() & 0xFFFF;
					yoff |= (appleGetWord() & 0xFFFF) << 16;
					curfacet = Cell.makeInstance(lib, name.substring(st));
					if (curfacet == null)
					{
						System.out.println("Cannot create facet '" + name.substring(st) + "'");
						return false;
					}
					break;

				case 3:		// element instance record
					if (length != 9)
					{
						System.out.println("Element instance length=" + length);
						return false;
					}
					lvlgrp = appleGetWord();
					levels = appleGetWord() & 0xFFFF;
					levels |= (appleGetWord() & 0xFFFF) << 16;
					for(i=0; i<32; i++) if (levels == (1 << i)) break;
					if (i >= 32)
					{
						System.out.println("Unknown layer code " + levels);
						return false;
					}
					layer = lvlgrp * 32 + i + 1;
					appleGetWord();
					eitype = appleGetWord();
					appleGetWord();		// numeir
					appleGetWord();		// textflg
					switch (eitype)
					{
						case 0: break;	// polygon
						case 1: break;	// area
						case 2: break;	// path
						case 3: break;	// rectangle
					}

					Integer layerInt = new Integer(layer);
					layernp = appleNodeMap.get(layerInt);
					if (layernp == null && !notFound.contains(layerInt))
					{
						System.out.println("Warning: Apple layer " + layer + " not found");
						notFound.add(layerInt);
					}
					break;

				case 101:		// polygon record
					appleGetWord();		// color
					appleGetWord();		// draw
					offset = 4;
					if (length-offset > polylistcount)
					{
						polylistcount = 0;
						polylist = new int[length-offset];
						px = new int[((length-offset)+4)/5];
						py = new int[((length-offset)+4)/5];
						ptrace = new Point2D[((length-offset)+4)/5];
						polylistcount = length - offset;
					}
					polyptr = 0;
					while (length > POLYSPLIT)
					{
						for(i=0; i<POLYSPLIT-offset; i++)
							polylist[polyptr++] = appleGetWord();
						chain = appleGetChain();
						if (chain != 2 && chain != 0)
						{
							System.out.println("END CHAIN=" + chain + " at byte " + byteCount);
							return false;
						}
						length -= POLYSPLIT;
						offset = 0;
					}
					for(i=0; i<length-offset; i++)
						polylist[polyptr++] = appleGetWord();
					if (polyptr > polylistcount)
						System.out.println("Just overflowed " + polylistcount + " long array with " + polyptr + " points");
					if ((chain&2) == 0) appleGetWord();

					// process data into a set of points
					polyptr /= 5;
					for(i=0; i<polyptr; i++)
					{
						px[i] = polylist[i*5] & 0xFFFF;
						px[i] |= (polylist[i*5+1] & 0xFFFF) << 16;
						py[i] = polylist[i*5+2] & 0xFFFF;
						py[i] |= (polylist[i*5+3] & 0xFFFF) << 16;
						px[i] = appleScale(px[i], xoff, scale);
						py[i] = appleScale(py[i], yoff, scale);
						if ((polylist[i*5+4]&2) != 0 && i != 0)
							System.out.println("Warning: point " + i + " of polygon has s=" + polylist[i*5+4]);
						if ((polylist[i*5+4]&1) != 0 && i != polyptr-1)
							System.out.println("Warning: point " + i + " of polygon has s=" + polylist[i*5+4]);
					}
					if (px[polyptr-1] == px[0] && py[polyptr-1] == py[0]) polyptr--;
					lx = hx = px[0];
					ly = hy = py[0];
					for(i=1; i<polyptr; i++)
					{
						if (px[i] < lx) lx = px[i];
						if (px[i] > hx) hx = px[i];
						if (py[i] < ly) ly = py[i];
						if (py[i] > hy) hy = py[i];
					}
					for(i=0; i<polyptr; i++)
						ptrace[i] = new Point2D.Double(px[i] - (lx+hx) / 2, py[i] - (ly+hy) / 2);

					// now create the polygon
					if (layernp != null && curfacet != null)
					{
						Point2D center = new Point2D.Double((lx+hx)/2, (ly+hy)/2);
						ni = NodeInst.newInstance(layernp, center, hx-lx, hy-ly, curfacet);
						if (ni == null) return false;
						ni.newVar(NodeInst.TRACE, ptrace);
					}
					break;

				case 107:		// flag record
					if (length != 3)
					{
						System.out.println("Flag length=" + length);
						return false;
					}
					appleGetWord();	// flag
					break;

				case 117:		// text record
					appleGetWord();	// vnumb
					appleGetWord();	// color
					appleGetWord();	// lvlgrp
					appleGetWord();	// levels1
					appleGetWord();	// levels2
					appleGetWord();	// txtdx low
					appleGetWord();	// txtdx high
					appleGetWord();	// txtdy low
					appleGetWord();	// txtdy high
					appleGetWord();	// txtsiz low
					appleGetWord();	// txtsiz high
					appleGetWord();	// tfield
					appleGetWord();	// torien
					tcount = appleGetWord();
					if ((tcount&1) != 0) tcount++;

					// allocate and read the string
					str = "";
					for(int k=0; k<tcount; k++) str += dataInputStream.readByte();

					// remove line-feeds, count carriage-returns
					i = 0;
					StringBuffer sb = new StringBuffer();
					for(int k=0; k<str.length(); k++)
					{
						char ch = str.charAt(k);
						if (ch == '\r')
						{
							sb.append('\n');
							i++;
						} else if (ch != '\n') sb.append(ch);
					}
					str = sb.toString();

					if (ni != null)
					{
						if (i <= 1)
						{
							// one carriage return: simple message
							int crPos = str.indexOf('\n');
							if (crPos >= 0) str = str.substring(0, crPos);
							TextDescriptor td = TextDescriptor.getNodeTextDescriptor().withDisplay(true);
							ni.newVar(Artwork.ART_MESSAGE, str, td);
						} else
						{
							// multi-line message
							String [] message = str.split("\n");
							TextDescriptor td = TextDescriptor.getNodeTextDescriptor().withDisplay(true);
							ni.newVar(Artwork.ART_MESSAGE, message, td);
						}
					}
					break;

				case 4:		// cell name record
					if (length != 17)
					{
						System.out.println("Cell name length=" + length);
						return false;
					}
					cellname = "";
					for(int k=0; k<30; k++) cellname += dataInputStream.readByte();
					st = cellname.indexOf(']') + 1;
					i = cellname.indexOf('.', st);
					if (i >= 0) cellname = cellname.substring(0, i);
					subnp = lib.findNodeProto(cellname);
					if (subnp == null)
					{
						System.out.println("Instance of cell '" + cellname.substring(st) + "' not found");
						return false;
					}
					break;

				case 5:		// cell instance header record
					if (length != 6)
					{
						System.out.println("Cell instance header length=" + length);
						return false;
					}
					appleGetWord();
					appleGetWord();
					appleGetWord();	// numcir
					appleGetWord();	// textflg
					break;

				case 105:		// cell instance record
					length -= 2;
					for(i=0; i<length; i++) cidata[i] = appleGetWord();

					// get first point
					x1 = cidata[0] & 0xFFFF;
					x1 |= (cidata[1] & 0xFFFF) << 16;
					y1 = cidata[2] & 0xFFFF;
					y1 |= (cidata[3] & 0xFFFF) << 16;
					x1 = appleScale(x1, xoff, scale);
					y1 = appleScale(y1, yoff, scale);
					i = 5;

					// ignore stretch point
					if ((cidata[length-1]&2) != 0) i += 5;

					// get transformation matrix
					int rot = 0, trans = 0;
					if ((cidata[length-1]&4) != 0)
					{
						a1 = cidata[i] & 0xFFFF;
						a1 |= (cidata[i+1] & 0xFFFF) << 16;
						if (a1 == -2147483647) a1 = -1; else
							if (a1 == 2147483647) a1 = 1; else a1 = 0;
						a2 = cidata[i+2] & 0xFFFF;
						a2 |= (cidata[i+3] & 0xFFFF) << 16;
						if (a2 == -2147483647) a2 = -1; else
							if (a2 == 2147483647) a2 = 1; else a2 = 0;
						a3 = cidata[i+4] & 0xFFFF;
						a3 |= (cidata[i+5] & 0xFFFF) << 16;
						if (a3 == -2147483647) a3 = -1; else
							if (a3 == 2147483647) a3 = 1; else a3 = 0;
						a4 = cidata[i+6] & 0xFFFF;
						a4 |= (cidata[i+7] & 0xFFFF) << 16;
						if (a4 == -2147483647) a4 = -1; else
							if (a4 == 2147483647) a4 = 1; else a4 = 0;

						// convert to rotation/transpose
						if (a1 == 0 && a2 == -1 && a3 == 1 && a4 == 0) rot = 900;
						if (a1 == -1 && a2 == 0 && a3 == 0 && a4 == -1) rot = 1800;
						if (a1 == 0 && a2 == 1 && a3 == -1 && a4 == 0) rot = 2700;
						if (a1 == 0 && a2 == -1 && a3 == -1 && a4 == 0)
						{
							trans = 1;
						}
						if (a1 == -1 && a2 == 0 && a3 == 0 && a4 == 1)
						{
							rot = 900;
							trans = 1;
						}
						if (a1 == 0 && a2 == 1 && a3 == 1 && a4 == 0)
						{
							rot = 1800;
							trans = 1;
						}
						if (a1 == 1 && a2 == 0 && a3 == 0 && a4 == -1)
						{
							rot = 2700;
							trans = 1;
						}
						i += 8;
					}
					if (subnp != null && curfacet != null)
					{
						// determine center of instance
						Orientation orient = Orientation.fromC(rot, trans != 0);
						AffineTransform mat = orient.pureRotate();
						Rectangle2D subBounds = subnp.getBounds();
						Point2D dest = new Point2D.Double(0, 0);
						mat.transform(new Point2D.Double(subBounds.getCenterX(), subBounds.getCenterY()), dest);
						double vx = dest.getX() + x1;
						double vy = dest.getY() + y1;
						Point2D center = new Point2D.Double(subBounds.getWidth(), subBounds.getHeight());
						ni = NodeInst.newInstance(subnp, center, vx*2, vy*2, curfacet, orient, null);
						if (ni == null) return false;
					}
					break;

				case 255:		// end definition record
					if (length != 2)
					{
						System.out.println("End definition length=" + length);
						return false;
					}
					if (curfacet == null)
					{
						System.out.println("End definition has no associated begin");
						return false;
					}

					curfacet = null;
					break;

				case 0:			// format record
					for(i=0; i<length-2; i++) appleGetWord();
					break;
				case 1:			// title record
					for(i=0; i<length-2; i++) appleGetWord();
					break;
				case 100:		// group record
					for(i=0; i<length-2; i++) appleGetWord();
					break;
				case 104:		// auxiliary record
					for(i=0; i<length-2; i++) appleGetWord();
					break;
				default:
					System.out.println("Unknown record type: " + type);
                return true;
			}
		}
//        return true;
    }

	/**
	 * Method to get the chain value from disk.
	 * Automatically senses byte swapping and tape/disk format.
	 */
	private int appleGetChain()
		throws IOException
	{
		int chain;

		if ((appleState&INITIALIZED) == 0)
		{
			// on first call, evaluate nature of file
			appleState |= INITIALIZED;
			chain = appleGetWord();
			if (chain == 3 || chain == 1) return(chain);

			// tape header may be present
			if (Character.isDigit(chain&0377) && Character.isDigit((chain>>8)&0377))
			{
				appleGetWord();
				appleState |= TAPEFORMAT;
				chain = appleGetWord();
			}

			// bytes may be swapped
			if (chain == 0x300 || chain == 0x100)
			{
				appleState |= BYTESSWAPPED;
				chain >>= 8;
			}
			return chain;
		}

		// normal chain request
		if ((appleState&TAPEFORMAT) != 0)
		{
			// get 4-digit code, skip to end of block
			for(int i=0; i<4; i++)
			{
				int val = dataInputStream.readByte();
				updateProgressDialog(2);
				if (Character.isDigit(val)) continue;
				i--;
			}
		}
		chain = appleGetWord();
		return chain;
	}

	private int appleGetWord()
		throws IOException
	{
		byte low = dataInputStream.readByte();
		byte high = dataInputStream.readByte();
		updateProgressDialog(2);
		if ((appleState&BYTESSWAPPED) == 0) return (low&0377) | ((high&0377)<<8);
		return (high&0377) | ((low&0377)<<8);
	}

	/**
	 * Method to convert the Apple floating point representation in "lo", "hi"
	 * and "exp" into a true floating point number
	 */
	private double appleFloat(int lo, int hi, int exp)
	{
		int i;
		double fl;

		i = (lo & 0xFFFF) | ((hi & 0xFFFF) << 16);
		exp = 31 - exp;
		fl = i & 0x7FFFFFFF;
		fl /= (1 << exp);
		if ((i & 0x80000000) != 0) fl = -fl;
		return(fl);
	}

	static Poly poly = null;

	private void setupAppleLayers(Technology tech)
	{
		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode pn = it.next();
			if (pn.getFunction() == PrimitiveNode.Function.NODE)
			{
				NodeLayer [] nls = pn.getNodeLayers();
				for(int i=0; i<nls.length; i++)
				{
					NodeLayer nl = nls[i];
					Layer lay = nl.getLayer();
					int appleLayer = lay.getIndex();		// TODO: should get the user-specified apple/860 layer
					appleNodeMap.put(new Integer(appleLayer), pn);
				}
			}
		}
	}

	/**
	 * Method to convert from Apple units, through offset "offset" and scale
	 * "scale" and return the proper value
	 */
	private int appleScale(int value, int offset, double scale)
	{
		double temp;

		temp = value - offset;
		temp = temp * scale;
		value = (int)temp;

		// round to the nearest quarter micron
		if ((value % 25) != 0)
		{
			if (value > 0) value = (value+12) / 25 * 25; else
				value = (value-12) / 25 * 25;
		}
		return(value);
	}
}

