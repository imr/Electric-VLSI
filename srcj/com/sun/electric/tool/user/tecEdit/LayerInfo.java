/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayerInfo.java
 * Technology Editor, layer information
 * Written by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.user.tecEdit;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EGraphics.Outline;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This class defines information about layers in the Technology Editor.
 */
public class LayerInfo extends Info
{
	String         name;
	String         javaName;
	EGraphics      desc;
	Layer.Function fun;
	int            funExtra;
	String         cif;
	String         gds;
	double         spiRes;
	double         spiCap;
	double         spiECap;
	double         height3d;
	double         thick3d;
	double         coverage;
	Layer          generated;

	static SpecialTextDescr [] layerTextTable =
	{
		new SpecialTextDescr(14,  18, LAYERFUNCTION),
		new SpecialTextDescr(14,  15, LAYERCOLOR),
		new SpecialTextDescr(14,  12, LAYERTRANSPARENCY),
		new SpecialTextDescr(14,   9, LAYERSTYLE),
		new SpecialTextDescr(14,   6, LAYERCIF),
		new SpecialTextDescr(14,   3, LAYERGDS),
		new SpecialTextDescr(14,   0, LAYERSPIRES),
		new SpecialTextDescr(14,  -3, LAYERSPICAP),
		new SpecialTextDescr(14,  -6, LAYERSPIECAP),
		new SpecialTextDescr(14,  -9, LAYER3DHEIGHT),
		new SpecialTextDescr(14, -12, LAYER3DTHICK),
		new SpecialTextDescr(14, -15, LAYERCOVERAGE)
	};

	LayerInfo()
	{
		name = "";
		desc = new EGraphics(false, true, null, 0, 0, 0, 0, 1, false, new int[16]);
		fun = Layer.Function.UNKNOWN;
		cif = "";
		gds = "";
	}

	/**
	 * Method to return an array of cells that comprise the layers in a technology library.
	 * @param lib the technology library.
	 * @return an array of cells for each layer (in the proper order).
	 */
	public static Cell [] getLayerCells(Library lib)
	{
		Library [] oneLib = new Library[1];
		oneLib[0] = lib;
		return findCellSequence(oneLib, "layer-", LAYERSEQUENCE_KEY);
	}

	/**
	 * Method to build the appropriate descriptive information for a layer into
	 * cell "np".  The color is "colorindex"; the stipple array is in "stip"; the
	 * layer style is in "style", the CIF layer is in "ciflayer"; the function is
	 * in "functionindex"; the Calma GDS-II layer is in "gds"; the SPICE resistance is in "spires",
	 * the SPICE capacitance is in "spicap", the SPICE edge capacitance is in "spiecap",
	 * the 3D height is in "height3d", and the 3D thickness is in "thick3d".
	 */
	void generate(Cell np)
	{
		NodeInst stippleNode = null, patClearNode = null, patInvertNode = null, patCopyNode = null, patPasteNode = null, patchNode = null;
		for(Iterator<NodeInst> it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			int opt = Manipulate.getOptionOnNode(ni);
			if (ni.getProto() == Artwork.tech.filledBoxNode)
			{
				if (opt != LAYERPATTERN) patchNode = ni;
			}
			switch (opt)
			{
				case LAYERPATTERN:   stippleNode = ni;    break;
				case LAYERPATCLEAR:  patClearNode = ni;   break;
				case LAYERPATINVERT: patInvertNode = ni;  break;
				case LAYERPATCOPY:   patCopyNode = ni;    break;
				case LAYERPATPASTE:  patPasteNode = ni;   break;
			}
		}

		// create the transparency information if it is not there
		if (patchNode == null)
		{
			// create the graphic color object
			NodeInst ni = NodeInst.makeInstance(Artwork.tech.filledBoxNode, new Point2D.Double(-7.5, 7.5), 5, 5, np);
			if (ni == null) return;
			Manipulate.setPatch(ni, desc);
		}

		// create the stipple pattern objects if none are there
		int [] stip = desc.getPattern();
		if (stippleNode == null)
		{
			for(int x=0; x<16; x++) for(int y=0; y<16; y++)
			{
				Point2D ctr = new Point2D.Double(x-19.5, 2.5-y);
				NodeInst ni = NodeInst.makeInstance(Artwork.tech.filledBoxNode, ctr, 1, 1, np);
				if (ni == null) return;
				if ((stip[y] & (1 << (15-x))) == 0)
				{
					Short [] spattern = new Short[16];
					for(int i=0; i<16; i++) spattern[i] = new Short((short)0);
					ni.newVar(Artwork.ART_PATTERN, spattern);
				}
				ni.newVar(OPTION_KEY, new Integer(LAYERPATTERN));
			}
			NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(-12, 3.5), 0, 0, np);
			if (ni == null) return;
            
            TextDescriptor td = TextDescriptor.getNodeTextDescriptor().withRelSize(0.5);
			ni.newVar(Artwork.ART_MESSAGE, "Stipple Pattern", td);
//			Variable var = ni.newDisplayVar(Artwork.ART_MESSAGE, "Stipple Pattern");
//			if (var != null)
//			{
////				var.setDisplay(true);
//				var.setRelSize(0.5);
//			}
		}

		// create the patch control object
		if (patClearNode == null)
		{
			NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(-12, -14), 0, 0, np);
			if (ni == null) return;
			Variable var = ni.newDisplayVar(Artwork.ART_MESSAGE, "Clear Pattern");
			ni.newVar(OPTION_KEY, new Integer(LAYERPATCLEAR));
		}
		if (patInvertNode == null)
		{
			NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(-12, -16), 0, 0, np);
			if (ni == null) return;
			Variable var = ni.newDisplayVar(Artwork.ART_MESSAGE, "Invert Pattern");
			ni.newVar(OPTION_KEY, new Integer(LAYERPATINVERT));
		}
		if (patCopyNode == null)
		{
			NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(-12, -18), 0, 0, np);
			if (ni == null) return;
			Variable var = ni.newDisplayVar(Artwork.ART_MESSAGE, "Copy Pattern");
			ni.newVar(OPTION_KEY, new Integer(LAYERPATCOPY));
		}
		if (patPasteNode == null)
		{
			NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(-12, -20), 0, 0, np);
			if (ni == null) return;
			Variable var = ni.newDisplayVar(Artwork.ART_MESSAGE, "Paste Pattern");
			ni.newVar(OPTION_KEY, new Integer(LAYERPATPASTE));
		}

		// load up the structure with the current values
		loadTableEntry(layerTextTable, LAYERFUNCTION, fun);
		loadTableEntry(layerTextTable, LAYERCOLOR, desc);
		loadTableEntry(layerTextTable, LAYERTRANSPARENCY, desc);
		loadTableEntry(layerTextTable, LAYERSTYLE, desc);
		loadTableEntry(layerTextTable, LAYERCIF, cif);
		loadTableEntry(layerTextTable, LAYERGDS, gds);
		loadTableEntry(layerTextTable, LAYERSPIRES, new Double(spiRes));
		loadTableEntry(layerTextTable, LAYERSPICAP, new Double(spiCap));
		loadTableEntry(layerTextTable, LAYERSPIECAP, new Double(spiECap));
		loadTableEntry(layerTextTable, LAYER3DHEIGHT, new Double(height3d));
		loadTableEntry(layerTextTable, LAYER3DTHICK, new Double(thick3d));
		loadTableEntry(layerTextTable, LAYERCOVERAGE, new Double(coverage));

		for(int i=0; i<layerTextTable.length; i++)
		{
			switch (layerTextTable[i].funct)
			{
				case LAYERFUNCTION:
					layerTextTable[i].value = fun;
					layerTextTable[i].extra = funExtra;
					break;
			}
		}

		// now create those text objects
		createSpecialText(np, layerTextTable);
	}

	/**
	 * Method to parse the layer cell in "np" and return a LayerInfo object that describes it.
	 */
	static LayerInfo parseCell(Cell np)
	{
		// create and initialize the GRAPHICS structure
		LayerInfo li = new LayerInfo();
		li.name = np.getName().substring(6);

		// look at all nodes in the layer description cell
		int patternCount = 0;
		Rectangle2D patternBounds = null;
		for(Iterator<NodeInst> it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			Variable var = ni.getVar(OPTION_KEY);
			if (var == null) continue;
			String str = getValueOnNode(ni);

			switch (((Integer)var.getObject()).intValue())
			{
				case LAYERFUNCTION:
					li.fun = Layer.Function.UNKNOWN;
					li.funExtra = 0;
					int commaPos = str.indexOf(',');
					String extras = "";
					if (commaPos >= 0)
					{
						extras = str.substring(commaPos+1);
						str = str.substring(0, commaPos);
					}
					List<Layer.Function> allFuncs = Layer.Function.getFunctions();
					for(Layer.Function fun : allFuncs)
					{
						if (fun.getName().equalsIgnoreCase(str))
						{
							li.fun = fun;
							break;
						}
					}
					int [] allExtraBits = Layer.Function.getFunctionExtras();
					while (extras.length() > 0)
					{
						int cp = extras.indexOf(',');
						String thisExtra = extras;
						if (cp >= 0)
						{
							thisExtra = extras.substring(0, cp);
							extras = extras.substring(cp+1);
						} else extras = "";
						for(int j=0; j<allExtraBits.length; j++)
						{
							if (Layer.Function.getExtraName(allExtraBits[j]).equalsIgnoreCase(thisExtra))
							{
								li.funExtra |= allExtraBits[j];
								break;
							}
						}
					}
					break;
				case LAYERCOLOR:
					StringTokenizer st = new StringTokenizer(str, ",");
					if (st.countTokens() != 5)
					{
						System.out.println("Color information must have 5 fields, separated by commas");
						break;
					}
					int r = TextUtils.atoi(st.nextToken());
					int g = TextUtils.atoi(st.nextToken());
					int b = TextUtils.atoi(st.nextToken());
					double o = TextUtils.atof(st.nextToken());
					boolean f = st.nextToken().equalsIgnoreCase("on");
					li.desc.setColor(new Color(r, g, b));
					li.desc.setOpacity(o);
					break;
				case LAYERTRANSPARENCY:
					if (str.equalsIgnoreCase("none")) li.desc.setTransparentLayer(0); else
					{
						int layerNum = TextUtils.atoi(str.substring(6));
						li.desc.setTransparentLayer(layerNum);
					}
					break;
				case LAYERPATTERN:
					if (patternCount == 0)
					{
						patternBounds = ni.getBounds();
					} else
					{
						Rectangle2D.union(patternBounds, ni.getBounds(), patternBounds);
					}
					patternCount++;
					break;
				case LAYERSTYLE:
					if (str.equalsIgnoreCase("solid"))
					{
						li.desc.setPatternedOnDisplay(false);
						li.desc.setPatternedOnPrinter(false);
					} else if (str.equalsIgnoreCase("patterned"))
					{
						li.desc.setPatternedOnDisplay(true);
						li.desc.setPatternedOnPrinter(true);
						li.desc.setOutlined(EGraphics.Outline.NOPAT);
					} else if (str.equalsIgnoreCase("patterned/outlined"))
					{
						li.desc.setPatternedOnDisplay(true);
						li.desc.setPatternedOnPrinter(true);
						li.desc.setOutlined(EGraphics.Outline.PAT_S);
					} else if (TextUtils.canonicString(str).startsWith("patterned/outline="))
					{
						li.desc.setPatternedOnDisplay(true);
						li.desc.setPatternedOnPrinter(true);
						EGraphics.Outline out = EGraphics.Outline.findOutline(str.substring(18));
						li.desc.setOutlined(out);
					}
					break;
				case LAYERCIF:
					li.cif = str;
					break;
				case LAYERGDS:
					if (str.equals("-1")) str = "";
					li.gds = str;
					break;
				case LAYERSPIRES:
					li.spiRes = TextUtils.atof(str);
					break;
				case LAYERSPICAP:
					li.spiCap = TextUtils.atof(str);
					break;
				case LAYERSPIECAP:
					li.spiECap = TextUtils.atof(str);
					break;
				case LAYER3DHEIGHT:
					li.height3d = TextUtils.atof(str);
					break;
				case LAYER3DTHICK:
					li.thick3d = TextUtils.atof(str);
					break;
				case LAYERCOVERAGE:
					li.coverage = TextUtils.atof(str);
					break;
			}
		}

		if (patternCount != 16*16 && patternCount != 16*8)
		{
			System.out.println("Incorrect number of pattern boxes in " + np +
				" (has " + patternCount + ", not " + (16*16) + ")");
			return null;
		}

		// construct the pattern
		int [] newPat = new int[16];
		for(Iterator<NodeInst> it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (ni.getProto() != Artwork.tech.filledBoxNode) continue;
			Variable var = ni.getVar(OPTION_KEY);
			if (var == null) continue;
			if (((Integer)var.getObject()).intValue() != LAYERPATTERN) continue;
			var = ni.getVar(Artwork.ART_PATTERN);
			if (var != null)
			{
				Short [] pat = (Short [])var.getObject();
				boolean nonZero = false;
				for(int i=0; i<pat.length; i++) if (pat[i].shortValue() != 0) { nonZero = true;   break; }
				if (!nonZero) continue;
			}
			Rectangle2D niBounds = ni.getBounds();
			int x = (int)((niBounds.getMinX() - patternBounds.getMinX()) / (patternBounds.getWidth() / 16));
			int y = (int)((patternBounds.getMaxY() - niBounds.getMaxY()) / (patternBounds.getHeight() / 16));
			newPat[y] |= (1 << (15-x));
		}
		if (patternCount == 16*8)
		{
			// older, half-height pattern: extend it
			for(int y=0; y<8; y++)
				newPat[y+8] = newPat[y];
		}
		li.desc.setPattern(newPat);
		return li;
	}

	static String makeLayerFunctionName(Layer.Function fun, int extraBits)
	{
		String str = fun.toString();
		int [] allExtraBits = Layer.Function.getFunctionExtras();
		for(int j=0; j<allExtraBits.length; j++)
		{
			if ((allExtraBits[j] & extraBits) != 0)
				str += "," + Layer.Function.getExtraName(allExtraBits[j]);
		}
		return str;
	}
}

