/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Generate.java
 * User tool: Technology Editor, creation
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
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.erc.ERC;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

/**
 * This class generates technologies from technology libraries.
 */
public class Generate
{

	static class GeneralInfo
	{
		String description;
		double scale;
		double minres, mincap;
		double gateShrinkage;
		boolean includeGateInResistance;
		boolean includeGround;
		Color [] transparentColors;

		/**
		 * Method to build the appropriate descriptive information for a layer into
		 * cell "np".  The color is "colorindex"; the stipple array is in "stip"; the
		 * layer style is in "style", the CIF layer is in "ciflayer"; the function is
		 * in "functionindex"; the Calma GDS-II layer is in "gds"; the SPICE resistance is in "spires",
		 * the SPICE capacitance is in "spicap", the SPICE edge capacitance is in "spiecap",
		 * the 3D height is in "height3d", and the 3D thickness is in "thick3d".
		 */
		void us_tecedmakeinfo(Cell np)
		{
			// load up the structure with the current values
			loadTableEntry(us_tecedmisctexttable, TECHLAMBDA, new Double(scale));
			loadTableEntry(us_tecedmisctexttable, TECHDESCRIPT, description);
			loadTableEntry(us_tecedmisctexttable, TECHSPICEMINRES, new Double(minres));
			loadTableEntry(us_tecedmisctexttable, TECHSPICEMINCAP, new Double(mincap));
			loadTableEntry(us_tecedmisctexttable, TECHGATESHRINK, new Double(gateShrinkage));
			loadTableEntry(us_tecedmisctexttable, TECHGATEINCLUDED, new Boolean(includeGateInResistance));
			loadTableEntry(us_tecedmisctexttable, TECHGROUNDINCLUDED, new Boolean(includeGround));
			loadTableEntry(us_tecedmisctexttable, TECHTRANSPCOLORS, transparentColors);

			// now create those text objects
			us_tecedcreatespecialtext(np, us_tecedmisctexttable);
		}

		/**
		 * Method to parse the miscellaneous-info cell in "np" and return a GeneralInfo object that describes it.
		 */
		public static GeneralInfo us_teceditgettechinfo(Cell np)
		{
			// create and initialize the GRAPHICS structure
			GeneralInfo gi = new GeneralInfo();
		
			for(Iterator it = np.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				int opt = Manipulate.us_tecedgetoption(ni);
				String str = Manipulate.getValueOnNode(ni);
				switch (opt)
				{
					case TECHLAMBDA:
						gi.scale = TextUtils.atof(str);
						break;
					case TECHDESCRIPT:
						gi.description = str;
						break;
					case TECHSPICEMINRES:
						gi.minres = TextUtils.atof(str);
						break;
					case TECHSPICEMINCAP:
						gi.mincap = TextUtils.atof(str);
						break;
					case TECHGATESHRINK:
						gi.gateShrinkage = TextUtils.atof(str);
						break;
					case TECHGATEINCLUDED:
						gi.includeGateInResistance = str.equalsIgnoreCase("yes");
						break;
					case TECHGROUNDINCLUDED:
						gi.includeGround = str.equalsIgnoreCase("yes");
						break;
					case TECHTRANSPCOLORS:
						Variable var = ni.getVar(TRANSLAYER_KEY);
						if (var != null)
						{
							Color [] colors = getTransparentColors((String)var.getObject());
							if (colors != null) gi.transparentColors = colors;
						}
						break;						
					case CENTEROBJ:
						break;
					default:
						Parse.us_tecedpointout(ni, np);
						System.out.println("Unknown object in miscellaneous-information cell (node " + ni.describe() + ")");
						break;
				}
			}
			return gi;
		}
	}

	static class LayerInfo
	{
		String name;
		String javaName;
		EGraphics desc;
		Layer.Function fun;
		int funExtra;
		String cif;
		String gds;
		double spires;
		double spicap;
		double spiecap;
		double height3d;
		double thick3d;
		double coverage;
		Layer generated;

		LayerInfo()
		{
		}

		static LayerInfo makeInstance()
		{
			LayerInfo li = new LayerInfo();
			li.name = "";
			li.desc = new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, 0, 0, 0, 0, 1, false, new int[16]);
			li.fun = Layer.Function.UNKNOWN;
			li.cif = "";
			li.gds = "";
			return li;
		}

		/**
		 * Method to build the appropriate descriptive information for a layer into
		 * cell "np".  The color is "colorindex"; the stipple array is in "stip"; the
		 * layer style is in "style", the CIF layer is in "ciflayer"; the function is
		 * in "functionindex"; the Calma GDS-II layer is in "gds"; the SPICE resistance is in "spires",
		 * the SPICE capacitance is in "spicap", the SPICE edge capacitance is in "spiecap",
		 * the 3D height is in "height3d", and the 3D thickness is in "thick3d".
		 */
		void us_tecedmakelayer(Cell np)
		{
			NodeInst laystipple = null, laypatclear = null, laypatinvert = null, laypatcopy = null, laypatpaste = null, patchNode = null;
			for(Iterator it = np.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				int opt = Manipulate.us_tecedgetoption(ni);
				if (ni.getProto() == Artwork.tech.filledBoxNode)
				{
					if (opt != Generate.LAYERPATTERN) patchNode = ni;
				}
				switch (opt)
				{
					case LAYERPATTERN:   laystipple = ni;    break;
					case LAYERPATCLEAR:  laypatclear = ni;   break;
					case LAYERPATINVERT: laypatinvert = ni;  break;
					case LAYERPATCOPY:   laypatcopy = ni;    break;
					case LAYERPATPASTE:  laypatpaste = ni;   break;
				}
			}
		
			// create the transparency information if it is not there
			if (patchNode == null)
			{
				// create the graphic color object
				NodeInst nicolor = NodeInst.makeInstance(Artwork.tech.filledBoxNode, new Point2D.Double(-15000/SCALEALL,15000/SCALEALL),
					10000/SCALEALL, 10000/SCALEALL, np);
				if (nicolor == null) return;
				Manipulate.us_teceditsetpatch(nicolor, desc);
			}
		
			// create the stipple pattern objects if none are there
			int [] stip = desc.getPattern();
			if (laystipple == null)
			{
				for(int x=0; x<16; x++) for(int y=0; y<16; y++)
				{
					Point2D ctr = new Point2D.Double((x*2000-39000)/SCALEALL, (5000-y*2000)/SCALEALL);
					NodeInst ni = NodeInst.makeInstance(Artwork.tech.filledBoxNode, ctr, 2000/SCALEALL, 2000/SCALEALL, np);
					if (ni == null) return;
					if ((stip[y] & (1 << (15-x))) == 0)
					{
						Short [] spattern = new Short[16];
						for(int i=0; i<16; i++) spattern[i] = new Short((short)0);
						ni.newVar(Artwork.ART_PATTERN, spattern);
					}
					ni.newVar(OPTION_KEY, new Integer(LAYERPATTERN));
				}
				NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(-24000/SCALEALL,7000/SCALEALL), 0, 0, np);
				if (ni == null) return;
				Variable var = ni.newVar(Artwork.ART_MESSAGE, "Stipple Pattern");
				if (var != null)
				{
					var.setDisplay(true);
					var.setRelSize(0.5);
				}
			}
		
			// create the patch control object
			if (laypatclear == null)
			{
				NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode,
					new Point2D.Double((16000-40000)/SCALEALL, (4000-16*2000)/SCALEALL), 0, 0, np);
				if (ni == null) return;
				Variable var = ni.newVar(Artwork.ART_MESSAGE, "Clear Pattern");
				if (var != null) var.setDisplay(true);
				ni.newVar(OPTION_KEY, new Integer(LAYERPATCLEAR));
			}
			if (laypatinvert == null)
			{
				NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode,
					new Point2D.Double((16000-40000)/SCALEALL, (4000-18*2000)/SCALEALL), 0, 0, np);
				if (ni == null) return;
				Variable var = ni.newVar(Artwork.ART_MESSAGE, "Invert Pattern");
				if (var != null) var.setDisplay(true);
				ni.newVar(OPTION_KEY, new Integer(LAYERPATINVERT));
			}
			if (laypatcopy == null)
			{
				NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode,
					new Point2D.Double((16000-40000)/SCALEALL, (4000-20*2000)/SCALEALL), 0, 0, np);
				if (ni == null) return;
				Variable var = ni.newVar(Artwork.ART_MESSAGE, "Copy Pattern");
				if (var != null) var.setDisplay(true);
				ni.newVar(OPTION_KEY, new Integer(LAYERPATCOPY));
			}
			if (laypatpaste == null)
			{
				NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode,
					new Point2D.Double((16000-40000)/SCALEALL, (4000-22*2000)/SCALEALL), 0, 0, np);
				if (ni == null) return;
				Variable var = ni.newVar(Artwork.ART_MESSAGE, "Paste Pattern");
				if (var != null) var.setDisplay(true);
				ni.newVar(OPTION_KEY, new Integer(LAYERPATPASTE));
			}

			// load up the structure with the current values
			loadTableEntry(us_tecedlayertexttable, LAYERFUNCTION, fun);
			loadTableEntry(us_tecedlayertexttable, LAYERCOLOR, desc);
			loadTableEntry(us_tecedlayertexttable, LAYERTRANSPARENCY, desc);
			loadTableEntry(us_tecedlayertexttable, LAYERSTYLE, desc);
			loadTableEntry(us_tecedlayertexttable, LAYERCIF, cif);
			loadTableEntry(us_tecedlayertexttable, LAYERGDS, gds);
			loadTableEntry(us_tecedlayertexttable, LAYERSPIRES, new Double(spires));
			loadTableEntry(us_tecedlayertexttable, LAYERSPICAP, new Double(spicap));
			loadTableEntry(us_tecedlayertexttable, LAYERSPIECAP, new Double(spiecap));
			loadTableEntry(us_tecedlayertexttable, LAYER3DHEIGHT, new Double(height3d));
			loadTableEntry(us_tecedlayertexttable, LAYER3DTHICK, new Double(thick3d));
			loadTableEntry(us_tecedlayertexttable, LAYERCOVERAGE, new Double(coverage));
			
			for(int i=0; i<us_tecedlayertexttable.length; i++)
			{
				switch (us_tecedlayertexttable[i].funct)
				{
					case LAYERFUNCTION:
						us_tecedlayertexttable[i].value = fun;
						us_tecedlayertexttable[i].extra = funExtra;
						break;
				}
			}

			// now create those text objects
			us_tecedcreatespecialtext(np, us_tecedlayertexttable);
		}

		/**
		 * Method to parse the layer cell in "np" and return a LayerInfo object that describes it.
		 */
		static LayerInfo us_teceditgetlayerinfo(Cell np)
		{
			// create and initialize the GRAPHICS structure
			LayerInfo li = LayerInfo.makeInstance();
			li.name = np.getName().substring(6);
		
			// look at all nodes in the layer description cell
			int patterncount = 0;
			Rectangle2D patternBounds = null;
			for(Iterator it = np.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				Variable varkey = ni.getVar(OPTION_KEY);
				if (varkey == null) continue;
				String str = Manipulate.getValueOnNode(ni);
		
				switch (((Integer)varkey.getObject()).intValue())
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
						List allFuncs = Layer.Function.getFunctions();
						for(Iterator fIt = allFuncs.iterator(); fIt.hasNext(); )
						{
							Layer.Function fun = (Layer.Function)fIt.next();
							if (fun.toString().equalsIgnoreCase(str))
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
						if (patterncount == 0)
						{
							patternBounds = ni.getBounds();
						} else
						{
							Rectangle2D.union(patternBounds, ni.getBounds(), patternBounds);
						}
						patterncount++;
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
							li.desc.setOutlinedOnDisplay(false);
							li.desc.setOutlinedOnPrinter(false);
						} else if (str.equalsIgnoreCase("patterned/outlined"))
						{
							li.desc.setPatternedOnDisplay(true);
							li.desc.setPatternedOnPrinter(true);
							li.desc.setOutlinedOnDisplay(true);
							li.desc.setOutlinedOnPrinter(true);
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
						li.spires = TextUtils.atof(str);
						break;
					case LAYERSPICAP:
						li.spicap = TextUtils.atof(str);
						break;
					case LAYERSPIECAP:
						li.spiecap = TextUtils.atof(str);
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

			if (patterncount != 16*16 && patterncount != 16*8)
			{
				System.out.println("Incorrect number of pattern boxes in " + np.describe() +
					" (has " + patterncount + ", not " + (16*16) + ")");
				return null;
			}
		
			// construct the pattern
			int [] newPat = new int[16];
			for(Iterator it = np.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (ni.getProto() != Artwork.tech.filledBoxNode) continue;
				Variable var = ni.getVar(OPTION_KEY);
				if (var == null) continue;
				if (((Integer)var.getObject()).intValue() != LAYERPATTERN) continue;
				var = ni.getVar(Artwork.ART_PATTERN);
				if (var != null)
				{
					Short [] pat = (Short [])var.getObject();
					boolean nonzero = false;
					for(int i=0; i<pat.length; i++) if (pat[i].shortValue() != 0) { nonzero = true;   break; }
					if (!nonzero) continue;
				}
				Rectangle2D niBounds = ni.getBounds();
				int x = (int)((niBounds.getMinX() - patternBounds.getMinX()) / (patternBounds.getWidth() / 16));
				int y = (int)((patternBounds.getMaxY() - niBounds.getMaxY()) / (patternBounds.getHeight() / 16));
				newPat[y] |= (1 << (15-x));
			}
			if (patterncount == 16*8)
			{
				// older, half-height pattern: extend it
				for(int y=0; y<8; y++)
					newPat[y+8] = newPat[y];
			}
			li.desc.setPattern(newPat);
			return li;
		}
	}

	static class ArcDetails
	{
		LayerInfo layer;
		double width;
		Poly.Type style;
	}

	static class ArcInfo
	{
		String name;
		String javaName;
		ArcProto.Function func;
		boolean fixang;
		boolean wipes;
		boolean noextend;
		int anginc;
		ArcProto generated;
		ArcDetails [] arcDetails;
		double widthOffset;
		double maxWidth;
		double antennaRatio;

		ArcInfo()
		{
		}

		static ArcInfo makeInstance()
		{
			ArcInfo aIn = new ArcInfo();
			aIn.func = ArcProto.Function.UNKNOWN;
			return aIn;
		}

		/**
		 * Method to build the appropriate descriptive information for an arc into
		 * cell "np".  The function is in "func"; the arc is fixed-angle if "fixang"
		 * is nonzero; the arc wipes pins if "wipes" is nonzero; and the arc does
		 * not extend its ends if "noextend" is nonzero.  The angle increment is
		 * in "anginc".
		 */
		void us_tecedmakearc(Cell np)
		{
			// load up the structure with the current values
			loadTableEntry(us_tecedarctexttable, ARCFUNCTION, func);
			loadTableEntry(us_tecedarctexttable, ARCFIXANG, new Boolean(fixang));
			loadTableEntry(us_tecedarctexttable, ARCWIPESPINS, new Boolean(wipes));
			loadTableEntry(us_tecedarctexttable, ARCNOEXTEND, new Boolean(noextend));
			loadTableEntry(us_tecedarctexttable, ARCINC, new Integer(anginc));
			loadTableEntry(us_tecedarctexttable, ARCANTENNARATIO, new Double(antennaRatio));
		
			// now create those text objects
			us_tecedcreatespecialtext(np, us_tecedarctexttable);
		}

		/**
		 * Method to parse the arc cell in "np" and return an ArcInfo object that describes it.
		 */
		static ArcInfo us_teceditgetarcinfo(Cell np)
		{
			// create and initialize the GRAPHICS structure
			ArcInfo ain = ArcInfo.makeInstance();
			ain.name = np.getName().substring(4);

			// look at all nodes in the arc description cell
			for(Iterator it = np.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				Variable varkey = ni.getVar(OPTION_KEY);
				if (varkey == null) continue;
				String str = Manipulate.getValueOnNode(ni);
		
				switch (((Integer)varkey.getObject()).intValue())
				{
					case ARCFUNCTION:
						ain.func = ArcProto.Function.UNKNOWN;
						List allFuncs = ArcProto.Function.getFunctions();
						for(Iterator fIt = allFuncs.iterator(); fIt.hasNext(); )
						{
							ArcProto.Function fun = (ArcProto.Function)fIt.next();
							if (fun.toString().equalsIgnoreCase(str))
							{
								ain.func = fun;
								break;
							}
						}
						break;
					case ARCINC:
						ain.anginc = TextUtils.atoi(str);
						break;
					case ARCFIXANG:
						ain.fixang = str.equalsIgnoreCase("yes");
						break;
					case ARCWIPESPINS:
						ain.wipes = str.equalsIgnoreCase("yes");
						break;
					case ARCNOEXTEND:
						ain.noextend = str.equalsIgnoreCase("no");
						break;
					case ARCANTENNARATIO:
						ain.antennaRatio = TextUtils.atof(str);
						break;
				}
			}
			return ain;
		}
	}

	static class NodePortDetails
	{
		String name;
		Generate.ArcInfo [] connections;
		int angle;
		int range;
		int netIndex;
		Technology.TechPoint[] values;
	}

	static class NodeLayerDetails
	{
		LayerInfo layer;
		Poly.Type style;
		int representation;
		Technology.TechPoint[] values;
		Parse.Sample ns;
		int portIndex;
		boolean multiCut;				/* true if a multi-cut layer */
		double        multixs, multiys;			/* size of multicut */
		double        multiindent, multisep;	/* indent and separation of multicuts */
		double lWidth, rWidth, extendT, extendB;		/* serpentine transistor information */
	}

	static class NodeInfo
	{
		String name;
		String abbrev;
		PrimitiveNode generated;
		PrimitiveNode.Function func;
		boolean serp;
		boolean square;
		boolean wipes;
		boolean lockable;
		NodeLayerDetails [] nodeLayers;
		NodePortDetails [] nodePortDetails;
		PrimitivePort[] primPorts;
		SizeOffset so;
		int specialType;
		double [] specialValues;
		double xSize, ySize;

		NodeInfo()
		{
		}

		static NodeInfo makeInstance()
		{
			NodeInfo nIn = new NodeInfo();
			nIn.func = PrimitiveNode.Function.UNKNOWN;
			return nIn;
		}
		
		/**
		 * Method to build the appropriate descriptive information for a node into
		 * cell "np".  The function is in "func", the serpentine transistor factor
		 * is in "serp", the node is square if "square" is true, the node
		 * is invisible on 1 or 2 arcs if "wipes" is true, and the node is lockable
		 * if "lockable" is true.
		 */
		void us_tecedmakenode(Cell np)
		{
			// load up the structure with the current values
			loadTableEntry(us_tecednodetexttable, NODEFUNCTION, func);
			loadTableEntry(us_tecednodetexttable, NODESERPENTINE, new Boolean(serp));
			loadTableEntry(us_tecednodetexttable, NODESQUARE, new Boolean(square));
			loadTableEntry(us_tecednodetexttable, NODEWIPES, new Boolean(wipes));
			loadTableEntry(us_tecednodetexttable, NODELOCKABLE, new Boolean(lockable));
		
			// now create those text objects
			us_tecedcreatespecialtext(np, us_tecednodetexttable);
		}

		/**
		 * Method to parse the node cell in "np" and return an NodeInfo object that describes it.
		 */
		static NodeInfo us_teceditgetnodeinfo(Cell np)
		{
			NodeInfo nin = NodeInfo.makeInstance();

			// look at all nodes in the arc description cell
			for(Iterator it = np.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				Variable varkey = ni.getVar(OPTION_KEY);
				if (varkey == null) continue;
				String str = Manipulate.getValueOnNode(ni);
		
				switch (((Integer)varkey.getObject()).intValue())
				{
					case NODEFUNCTION:
						nin.func = PrimitiveNode.Function.UNKNOWN;
						List allFuncs = PrimitiveNode.Function.getFunctions();
						for(Iterator fIt = allFuncs.iterator(); fIt.hasNext(); )
						{
							PrimitiveNode.Function fun = (PrimitiveNode.Function)fIt.next();
							if (fun.toString().equalsIgnoreCase(str))
							{
								nin.func = fun;
								break;
							}
						}
						break;
					case NODESQUARE:
						nin.square = str.equalsIgnoreCase("yes");
						break;
					case NODEWIPES:
						nin.wipes = str.equalsIgnoreCase("yes");
						break;
					case NODELOCKABLE:
						nin.lockable = str.equalsIgnoreCase("yes");
						break;
					case NODESERPENTINE:
						nin.serp = str.equalsIgnoreCase("yes");
						break;
				}
			}
			return nin;
		}
	}

	static final double SCALEALL = 2000;

	/*
	 * the meaning of OPTION_KEY on nodes
	 * Note that these values are stored in the technology libraries and therefore cannot be changed.
	 * Gaps in the table are where older values became obsolete.
	 * Do not reuse lower numbers when creating a new attribute: add at the end
	 * (as Ivan Sutherland likes to say, numbers are cheap).
	 */
	static final int LAYERTRANSPARENCY =  1;					/* transparency layer (layer cell) */
	static final int LAYERSTYLE        =  2;					/* style (layer cell) */
	static final int LAYERCIF          =  3;					/* CIF name (layer cell) */
	static final int LAYERFUNCTION     =  4;					/* function (layer cell) */
	static final int LAYERLETTERS      =  5;					/* letters (layer cell) */
	static final int LAYERPATTERN      =  6;					/* pattern (layer cell) */
	static final int LAYERPATCONT      =  7;					/* pattern control (layer cell) */
	static final int LAYERPATCH        =  8;					/* patch of layer (node/arc cell) */
	static final int ARCFUNCTION       =  9;					/* function (arc cell) */
	static final int NODEFUNCTION      = 10;					/* function (node cell) */
	static final int ARCFIXANG         = 11;					/* fixed-angle (arc cell) */
	static final int ARCWIPESPINS      = 12;					/* wipes pins (arc cell) */
	static final int ARCNOEXTEND       = 13;					/* end extension (arc cell) */
	static final int TECHLAMBDA        = 14;					/* lambda (info cell) */
	static final int TECHDESCRIPT      = 15;					/* description (info cell) */
	static final int NODESERPENTINE    = 16;					/* serpentine MOS trans (node cell) */
	static final int LAYERDRCMINWID    = 17;					/* DRC minimum width (layer cell, OBSOLETE) */
	static final int PORTOBJ           = 18;					/* port object (node cell) */
	static final int HIGHLIGHTOBJ      = 19;					/* highlight object (node/arc cell) */
	static final int LAYERGDS          = 20;					/* Calma GDS-II layer (layer cell) */
	static final int NODESQUARE        = 21;					/* square node (node cell) */
	static final int NODEWIPES         = 22;					/* pin node can disappear (node cell) */
	static final int ARCINC            = 23;					/* increment for arc angles (arc cell) */
	static final int NODEMULTICUT      = 24;					/* separation of multiple contact cuts (node cell) */
	static final int NODELOCKABLE      = 25;					/* lockable primitive (node cell) */
	static final int CENTEROBJ         = 26;					/* grab point object (node cell) */
	static final int LAYERSPIRES       = 27;					/* SPICE resistance (layer cell) */
	static final int LAYERSPICAP       = 28;					/* SPICE capacitance (layer cell) */
	static final int LAYERSPIECAP      = 29;					/* SPICE edge capacitance (layer cell) */
	static final int LAYERDXF          = 30;					/* DXF layer (layer cell) */
	static final int LAYER3DHEIGHT     = 31;					/* 3D height (layer cell) */
	static final int LAYER3DTHICK      = 32;					/* 3D thickness (layer cell) */
	static final int LAYERCOLOR        = 33;					/* color (layer cell) */
	static final int LAYERPATCLEAR     = 34;					/* clear the pattern (layer cell) */
	static final int LAYERPATINVERT    = 35;					/* invert the pattern (layer cell) */
	static final int LAYERPATCOPY      = 36;					/* copy the pattern (layer cell) */
	static final int LAYERPATPASTE     = 37;					/* copy the pattern (layer cell) */
	static final int TECHSPICEMINRES   = 38;					/* Minimum resistance of SPICE elements (info cell) */
	static final int TECHSPICEMINCAP   = 39;					/* Minimum capacitance of SPICE elements (info cell) */
	static final int ARCANTENNARATIO   = 40;					/* Maximum antenna ratio (arc cell) */
	static final int LAYERCOVERAGE     = 41;					/* Desired coverage percentage (layer cell) */
	static final int TECHGATESHRINK    = 42;					/* gate shrinkage, in um (info cell) */
	static final int TECHGATEINCLUDED  = 43;					/* true if gate is included in resistance (info cell) */
	static final int TECHGROUNDINCLUDED= 44;					/* true to include the ground network (info cell) */
	static final int TECHTRANSPCOLORS  = 45;					/* the transparent colors (info cell) */

	/** key of Variable holding option information. */	public static final Variable.Key OPTION_KEY = ElectricObject.newKey("EDTEC_option");
	/** key of Variable holding layer information. */	public static final Variable.Key LAYER_KEY = ElectricObject.newKey("EDTEC_layer");
	/** key of Variable holding arc ordering. */		public static final Variable.Key ARCSEQUENCE_KEY = ElectricObject.newKey("EDTEC_arcsequence");
	/** key of Variable holding node ordering. */		public static final Variable.Key NODESEQUENCE_KEY = ElectricObject.newKey("EDTEC_nodesequence");
	/** key of Variable holding layer ordering. */		public static final Variable.Key LAYERSEQUENCE_KEY = ElectricObject.newKey("EDTEC_layersequence");
	/** key of Variable marking geometry as min-size. */public static final Variable.Key MINSIZEBOX_KEY = ElectricObject.newKey("EDTEC_minbox");
	/** key of Variable holding port name. */			public static final Variable.Key PORTNAME_KEY = ElectricObject.newKey("EDTEC_portname");
	/** key of Variable holding port angle. */			public static final Variable.Key PORTANGLE_KEY = ElectricObject.newKey("EDTEC_portangle");
	/** key of Variable holding port range. */			public static final Variable.Key PORTRANGE_KEY = ElectricObject.newKey("EDTEC_portrange");
	/** key of Variable holding arc connection list. */	public static final Variable.Key CONNECTION_KEY = ElectricObject.newKey("EDTEC_connects");
	/** key of Variable with color map table. */		public static final Variable.Key COLORMAP_KEY = ElectricObject.newKey("EDTEC_colormap");
	/** key of Variable with color map table. */		public static final Variable.Key DEPENDENTLIB_KEY = ElectricObject.newKey("EDTEC_dependent_libraries");
	/** key of Variable with transparent color list. */	public static final Variable.Key TRANSLAYER_KEY = ElectricObject.newKey("EDTEC_transparent_layers");
	
//	INTBIG us_teceddrclayers = 0;
//	CHAR **us_teceddrclayernames = 0;
//	extern INTBIG           us_teceddrclayers;
//	extern CHAR           **us_teceddrclayernames;

	/* for describing special text in a cell */
	static class SpecialTextDescr
	{
		NodeInst ni;
		Object   value;
		int      extra;
		double   x, y;
		int      funct;

		SpecialTextDescr(double x, double y, int funct)
		{
			ni = null;
			value = null;
			this.x = x;
			this.y = y;
			this.funct = funct;
		}
	};

	static SpecialTextDescr [] us_tecedmisctexttable =
	{
		new SpecialTextDescr(0/SCALEALL,  6000/SCALEALL, TECHLAMBDA),
		new SpecialTextDescr(0/SCALEALL,     0/SCALEALL, TECHDESCRIPT),
		new SpecialTextDescr(0/SCALEALL, -6000/SCALEALL, TECHSPICEMINRES),
		new SpecialTextDescr(0/SCALEALL,-12000/SCALEALL, TECHSPICEMINCAP),
		new SpecialTextDescr(0/SCALEALL,-18000/SCALEALL, TECHGATESHRINK),
		new SpecialTextDescr(0/SCALEALL,-24000/SCALEALL, TECHGATEINCLUDED),
		new SpecialTextDescr(0/SCALEALL,-30000/SCALEALL, TECHGROUNDINCLUDED),
		new SpecialTextDescr(0/SCALEALL,-36000/SCALEALL, TECHTRANSPCOLORS),
	};
	
	static SpecialTextDescr [] us_tecedlayertexttable =
	{
		new SpecialTextDescr(28000/SCALEALL,  36000/SCALEALL, LAYERFUNCTION),
		new SpecialTextDescr(28000/SCALEALL,  30000/SCALEALL, LAYERCOLOR),
		new SpecialTextDescr(28000/SCALEALL,  24000/SCALEALL, LAYERTRANSPARENCY),
		new SpecialTextDescr(28000/SCALEALL,  18000/SCALEALL, LAYERSTYLE),
		new SpecialTextDescr(28000/SCALEALL,  12000/SCALEALL, LAYERCIF),
		new SpecialTextDescr(28000/SCALEALL,   6000/SCALEALL, LAYERGDS),
		new SpecialTextDescr(28000/SCALEALL,      0/SCALEALL, LAYERSPIRES),
		new SpecialTextDescr(28000/SCALEALL,  -6000/SCALEALL, LAYERSPICAP),
		new SpecialTextDescr(28000/SCALEALL, -12000/SCALEALL, LAYERSPIECAP),
		new SpecialTextDescr(28000/SCALEALL, -18000/SCALEALL, LAYER3DHEIGHT),
		new SpecialTextDescr(28000/SCALEALL, -24000/SCALEALL, LAYER3DTHICK),
		new SpecialTextDescr(28000/SCALEALL, -30000/SCALEALL, LAYERCOVERAGE)
	};

	static SpecialTextDescr [] us_tecedarctexttable =
	{
		new SpecialTextDescr(0/SCALEALL, 36000/SCALEALL, ARCFUNCTION),
		new SpecialTextDescr(0/SCALEALL, 30000/SCALEALL, ARCFIXANG),
		new SpecialTextDescr(0/SCALEALL, 24000/SCALEALL, ARCWIPESPINS),
		new SpecialTextDescr(0/SCALEALL, 18000/SCALEALL, ARCNOEXTEND),
		new SpecialTextDescr(0/SCALEALL, 12000/SCALEALL, ARCINC),
		new SpecialTextDescr(0/SCALEALL,  6000/SCALEALL, ARCANTENNARATIO)
	};

	static SpecialTextDescr [] us_tecednodetexttable =
	{
		new SpecialTextDescr(0/SCALEALL, 30000/SCALEALL, NODEFUNCTION),
		new SpecialTextDescr(0/SCALEALL, 24000/SCALEALL, NODESERPENTINE),
		new SpecialTextDescr(0/SCALEALL, 18000/SCALEALL, NODESQUARE),
		new SpecialTextDescr(0/SCALEALL, 12000/SCALEALL, NODEWIPES),
		new SpecialTextDescr(0/SCALEALL,  6000/SCALEALL, NODELOCKABLE)
	};

	public static void makeLibFromTech()
	{
		List techs = new ArrayList();
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			if (tech.isNonStandard()) continue;
			techs.add(tech);
		}
		String [] techChoices = new String[techs.size()];
		for(int i=0; i<techs.size(); i++)
			techChoices[i] = ((Technology)techs.get(i)).getTechName();
        String chosen = (String)JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "Technology to Edit",
            "Choose a technology to edit", JOptionPane.QUESTION_MESSAGE, null, techChoices, Technology.getCurrent().getTechName());
        if (chosen == null) return;
        Technology tech = Technology.findTechnology(chosen);
        LibFromTechJob job = new LibFromTechJob(tech);
	}

    /**
     * Class to create a technology-library from a technology.
     */
    public static class LibFromTechJob extends Job
	{
		private Technology tech;

		public LibFromTechJob(Technology tech)
		{
			super("Make Technology Library from Technology", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.tech = tech;
			startJob();
		}

		public boolean doIt()
		{
	        Library lib = us_tecedmakelibfromtech(tech);
	        if (lib == null) return false;

	        // switch to the library and show a cell
	        lib.setCurrent();
			return true;
		}
	}

	/**
	 * Method to convert technology "tech" into a library and return that library.
	 * Returns NOLIBRARY on error
	 */
	static Library us_tecedmakelibfromtech(Technology tech)
	{
		Library lib = Library.newInstance(tech.getTechName(), null);
		if (lib == null)
		{
			System.out.println("Cannot create library " + tech.getTechName());
			return null;
		}
		System.out.println("Created library " + tech.getTechName() + "...");
	
		// create the miscellaneous info cell (called "factors")
		Cell fNp = Cell.newInstance(lib, "factors");
		if (fNp == null) return null;
		fNp.setInTechnologyLibrary();

		// build the layer cell
		GeneralInfo gi = new GeneralInfo();
		gi.scale = tech.getScale();
		gi.description = tech.getTechDesc();
		gi.minres = tech.getMinResistance();
		gi.mincap = tech.getMinCapacitance();
		gi.gateShrinkage = tech.getGateLengthSubtraction();
		gi.includeGateInResistance = tech.isGateIncluded();
		gi.includeGround = tech.isGroundNetIncluded();
		Color [] wholeMap = tech.getColorMap();
		int numLayers = tech.getNumTransparentLayers();
		gi.transparentColors = new Color[numLayers];
		for(int i=0; i<numLayers; i++)
			gi.transparentColors[i] = wholeMap[1<<i];
		gi.us_tecedmakeinfo(fNp);
	
		// create the layer node names
		int layertotal = tech.getNumLayers();
		HashMap layerCells = new HashMap();
	
		// create the layer nodes
		System.out.println("Creating the layers...");
		String [] layerSequence = new String[layertotal];
		for(int i=0; i<layertotal; i++)
		{
			Layer layer = tech.getLayer(i);
			EGraphics desc = layer.getGraphics();
			String fname = "layer-" + layer.getName();
	
			// make sure the layer doesn't exist
			if (lib.findNodeProto(fname) != null)
			{
				System.out.println("Warning: multiple layers named '" + fname + "'");
				break;
			}

			Cell lNp = Cell.newInstance(lib, fname);
			if (lNp == null) return null;
			lNp.setTechnology(Artwork.tech);
			lNp.setInTechnologyLibrary();
			layerCells.put(layer, lNp);

			LayerInfo li = new LayerInfo();
			li.fun = layer.getFunction();
			li.funExtra = layer.getFunctionExtras();
			li.desc = desc;

			// compute foreign file formats
			li.cif = layer.getCIFLayer();
			li.gds = layer.getGDSLayer();
	
			// compute the SPICE information
			li.spires = layer.getResistance();
			li.spicap = layer.getCapacitance();
			li.spiecap = layer.getEdgeCapacitance();
	
			// compute the 3D information
			li.height3d = layer.getDepth();
			li.thick3d = layer.getThickness();
	
			// build the layer cell
			li.us_tecedmakelayer(lNp);
			layerSequence[i] = lNp.getName().substring(6);
		}
	
		// save the layer sequence
		lib.newVar(LAYERSEQUENCE_KEY, layerSequence);
	
		// create the arc cells
		System.out.println("Creating the arcs...");
		int arctotal = 0;
		HashMap arcCells = new HashMap();
		for(Iterator it = tech.getArcs(); it.hasNext(); )
		{
			PrimitiveArc ap = (PrimitiveArc)it.next();
			if (ap.isNotUsed()) continue;
			String fname = "arc-" + ap.getName();
	
			// make sure the arc doesn't exist
			if (lib.findNodeProto(fname) != null)
			{
				System.out.println("Warning: multiple arcs named '" + fname + "'");
				break;
			}
	
			Cell aNp = Cell.makeInstance(lib, fname);
			if (aNp == null) return null;
			aNp.setTechnology(Artwork.tech);
			aNp.setInTechnologyLibrary();

			ArcInfo aIn = new ArcInfo();
			aIn.func = ap.getFunction();
			aIn.fixang = ap.isFixedAngle();
			aIn.wipes = ap.isWipable();
			aIn.noextend = ap.isExtended();
			aIn.anginc = ap.getAngleIncrement();
			aIn.antennaRatio = ERC.getERCTool().getAntennaRatio(ap);
			arcCells.put(ap, aNp);
			aIn.us_tecedmakearc(aNp);
	
			// now create the arc layers
			double wid = ap.getDefaultWidth() - ap.getWidthOffset();
            ArcInst ai = ArcInst.makeDummyInstance(ap, wid*4);
            Poly [] polys = tech.getShapeOfArc(ai);
			double xoff = wid*2 + wid/2 + ap.getWidthOffset()/2;
            for(int i=0; i<polys.length; i++)
			{
            	Poly poly = polys[i];
            	Layer arcLayer = poly.getLayer();
            	if (arcLayer == null) continue;
            	EGraphics arcDesc = arcLayer.getGraphics();
	
				// scale the arc geometry appropriately
            	Point2D [] points = poly.getPoints();
				for(int k=0; k<points.length; k++)
					points[k] = new Point2D.Double(points[k].getX() - xoff - 40000/SCALEALL, points[k].getY() - 10000/SCALEALL);
	
				// create the node to describe this layer
				NodeInst ni = us_tecedplacegeom(poly, aNp);
				if (ni == null) continue;
	
				// get graphics for this layer
				Manipulate.us_teceditsetpatch(ni, arcDesc);
				Cell layerCell = (Cell)layerCells.get(arcLayer);
				if (layerCell != null) ni.newVar(LAYER_KEY, layerCell);
				ni.newVar(OPTION_KEY, new Integer(LAYERPATCH));
			}
            double i = ai.getProto().getWidthOffset() / 2;
			NodeInst ni = NodeInst.makeInstance(Artwork.tech.boxNode, new Point2D.Double(-40000/SCALEALL - wid*2.5 - i, -10000/SCALEALL), wid*5, wid, aNp);
			if (ni == null) return null;
			ni.newVar(Artwork.ART_COLOR, new Integer(EGraphics.WHITE));
			ni.newVar(LAYER_KEY, null);
			ni.newVar(OPTION_KEY, new Integer(LAYERPATCH));
			arctotal++;

			// compact it accordingly
			us_tecedcompactArc(aNp);
		}
	
		// save the arc sequence
		String [] arcSequence = new String[arctotal];
		int arcIndex = 0;
		for(Iterator it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = (ArcProto)it.next();
			if (ap.isNotUsed()) continue;
			arcSequence[arcIndex++] = ap.getName();
		}
		lib.newVar(ARCSEQUENCE_KEY, arcSequence);
	
		// create the node cells
		System.out.println("Creating the nodes...");
		int nodetotal = 0;
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode pnp = (PrimitiveNode)it.next();
			if (!pnp.isNotUsed()) nodetotal++;
		}
		double [] minnodesize = new double[nodetotal * 2];
		String [] nodeSequence = new String[nodetotal];
		int nodeIndex = 0;
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode pnp = (PrimitiveNode)it.next();
			if (pnp.isNotUsed()) continue;
			nodeSequence[nodeIndex++] = pnp.getName();
			boolean first = true;
	
			// create the node layers
            NodeInst oNi = NodeInst.makeDummyInstance(pnp);
			double xs = pnp.getDefWidth() * 2;
			double ys = pnp.getDefHeight() * 2;
			if (xs < 3) xs = 3;
			if (ys < 3) ys = 3;
			double nodexpos = -xs*2;
			Point2D [] pos = new Point2D[4];
			pos[0] = new Point2D.Double(nodexpos - xs, -10000/SCALEALL + ys);
			pos[1] = new Point2D.Double(nodexpos + xs, -10000/SCALEALL + ys);
			pos[2] = new Point2D.Double(nodexpos - xs, -10000/SCALEALL - ys);
			pos[3] = new Point2D.Double(nodexpos + xs, -10000/SCALEALL - ys);

			SizeOffset so = pnp.getProtoSizeOffset();
			xs = pnp.getDefWidth() - so.getLowXOffset() - so.getHighXOffset();
			ys = pnp.getDefHeight() - so.getLowYOffset() - so.getHighYOffset();
			double [] xsc = new double[4];
			double [] ysc = new double[4];
			xsc[0] = xs*1;   ysc[0] = ys*1;
			xsc[1] = xs*2;   ysc[1] = ys*1;
			xsc[2] = xs*1;   ysc[2] = ys*2;
			xsc[3] = xs*2;   ysc[3] = ys*2;
	
			// for multicut contacts, make large size be just right for 2 cuts
			if (pnp.getSpecialType() == PrimitiveNode.MULTICUT)
			{
				double [] values = pnp.getSpecialValues();
				double min2x = values[0]*2 + values[2]*2 + values[4];
				double min2y = values[1]*2 + values[3]*2 + values[4];
				xsc[1] = min2x;
				xsc[3] = min2x;
				ysc[2] = min2y;
				ysc[3] = min2y;
			}
			Cell nNp = null;
			Rectangle2D mainBounds = null;
			for(int e=0; e<4; e++)
			{
				// do not create node if main example had no polygons
				if (e != 0 && first) continue;
	
				// square nodes have only two examples
				if (pnp.isSquare() && (e == 1 || e == 2)) continue;
				double dX = pos[e].getX() - oNi.getAnchorCenterX();
				double dY = pos[e].getY() - oNi.getAnchorCenterY();
				double dXSize = xsc[e] + so.getLowXOffset() + so.getHighXOffset() - oNi.getXSize();
				double dYSize = ysc[e] + so.getLowYOffset() + so.getHighYOffset() - oNi.getYSize();
				oNi.lowLevelModify(dX, dY, dXSize, dYSize, 0);
				Poly [] polys = tech.getShapeOfNode(oNi);
				int j = polys.length;
				for(int i=0; i<j; i++)
				{
					Poly poly = polys[i];
					Layer nodeLayer = poly.getLayer();
					if (nodeLayer == null) continue;
					EGraphics desc = nodeLayer.getGraphics();

					// accumulate total size of main example
					if (e == 0)
					{
						Rectangle2D polyBounds = poly.getBounds2D();
						if (i == 0)
						{
							mainBounds = polyBounds;
						} else
						{
							Rectangle2D.union(mainBounds, polyBounds, mainBounds);
						}
					}
	
					// create the node cell on the first valid layer
					if (first)
					{
						first = false;
						String fName = "node-" + pnp.getName();
	
						// make sure the node doesn't exist
						if (lib.findNodeProto(fName) != null)
						{
							System.out.println("Warning: multiple nodes named '" + fName + "'");
							break;
						}

						nNp = Cell.makeInstance(lib, fName);
						if (nNp == null) return null;

						nNp.setTechnology(Artwork.tech);
						nNp.setInTechnologyLibrary();
						NodeInfo nIn = new NodeInfo();
						nIn.func = pnp.getFunction();
						nIn.serp = false;
						if ((nIn.func == PrimitiveNode.Function.TRANMOS || nIn.func == PrimitiveNode.Function.TRAPMOS ||
							nIn.func == PrimitiveNode.Function.TRADMOS) && pnp.isHoldsOutline()) nIn.serp = true;
						nIn.square = pnp.isSquare();
						nIn.wipes = pnp.isWipeOn1or2();
						nIn.lockable = pnp.isLockedPrim();
						nIn.us_tecedmakenode(nNp);
					}

					// create the node to describe this layer
					NodeInst ni = us_tecedplacegeom(poly, nNp);
					if (ni == null) return null;

					// get graphics for this layer
					Manipulate.us_teceditsetpatch(ni, desc);
					Cell layerCell = (Cell)layerCells.get(nodeLayer);
					if (layerCell != null) ni.newVar(LAYER_KEY, layerCell);
					ni.newVar(OPTION_KEY, new Integer(LAYERPATCH));
	
					// set minimum polygon factor on smallest example
					if (e != 0) continue;
//					if (i >= tech.nodeprotos[pnp.primindex-1].layercount) continue;
//					ll = tech.nodeprotos[pnp.primindex-1].layerlist;
//					if (ll == 0) continue;
//					if (ll[i].representation != MINBOX) continue;
//					Variable var = ni.newVar(MINSIZEBOX_KEY, "MIN");
//					if (var != null) var.setDisplay(true);
				}
				if (first) continue;
	
				// create the highlight node
				NodeInst ni = NodeInst.makeInstance(Artwork.tech.boxNode, pos[e], xsc[e], ysc[e], nNp);
				if (ni == null) return null;
				ni.newVar(Artwork.ART_COLOR, new Integer(EGraphics.makeIndex(Color.WHITE)));
				ni.newVar(LAYER_KEY, null);
				ni.newVar(OPTION_KEY, new Integer(LAYERPATCH));
	
				// create a grab node (only in main example)
//				if (e == 0)
//				{
//					var = getvalkey((INTBIG)pnp, VNODEPROTO, VINTEGER|VISARRAY, el_prototype_center_key);
//					if (var != NOVARIABLE)
//					{
//						lx = hx = xpos[0] + ((INTBIG *)var.addr)[0];
//						ly = hy = ypos[0] + ((INTBIG *)var.addr)[1];
//						lx = muldiv(lx, lambda, oldlam);
//						hx = muldiv(hx, lambda, oldlam);
//						ly = muldiv(ly, lambda, oldlam);
//						hy = muldiv(hy, lambda, oldlam);
//						nodeprotosizeoffset(gen_cellcenterprim, &lxo, &lyo, &hxo, &hyo, np);
//						ni = newnodeinst(gen_cellcenterprim, lx-lxo, hx+hxo, ly-lyo, hy+hyo, 0, 0, np);
//						if (ni == null) return(NOLIBRARY);
//					}
//				}
	
				// also draw ports
				HashMap portNodes = new HashMap();
				for(Iterator pIt = pnp.getPorts(); pIt.hasNext(); )
				{
					PrimitivePort pp = (PrimitivePort)pIt.next();
					Poly poly = tech.getShapeOfPort(oNi, pp);
					SizeOffset pSo = Generic.tech.portNode.getProtoSizeOffset();
					double width = poly.getBounds2D().getWidth() + pSo.getLowXOffset() + pSo.getHighXOffset();
					double height = poly.getBounds2D().getHeight() + pSo.getLowYOffset() + pSo.getHighYOffset();
					NodeInst pNi = NodeInst.makeInstance(Generic.tech.portNode, new Point2D.Double(poly.getCenterX(), poly.getCenterY()),
						width, height, nNp);
					if (pNi == null) return null;
					portNodes.put(pp, pNi);
					pNi.newVar(OPTION_KEY, new Integer(LAYERPATCH));
					Variable var = pNi.newVar(PORTNAME_KEY, pp.getName());
					if (var != null)
						var.setDisplay(true);
	
					// on the first sample, also show angle and connection
					if (e != 0) continue;
					if (pp.getAngle() != 0 || pp.getAngleRange() != 180)
					{
						pNi.newVar(PORTANGLE_KEY, new Integer(pp.getAngle()));
						pNi.newVar(PORTRANGE_KEY, new Integer(pp.getAngleRange()));
					}

					// add in the "local" port connections (from this tech)
					ArcProto [] connects = pp.getConnections();
					List validConns = new ArrayList();
					for(int i=0; i<connects.length; i++)
					{
						if (connects[i].getTechnology() != tech) continue;
						Cell cell = (Cell)arcCells.get(connects[i]);
						if (cell != null) validConns.add(cell);						
					}
					if (validConns.size() > 0)
					{
						Cell [] aplist = new Cell[validConns.size()];
						for(int i=0; i<validConns.size(); i++)
							aplist[i] = (Cell)validConns.get(i);
						pNi.newVar(CONNECTION_KEY, aplist);
					}
	
					// connect the connected ports
					for(Iterator oPIt = pnp.getPorts(); oPIt.hasNext(); )
					{
						PrimitivePort opp = (PrimitivePort)oPIt.next();
						if (opp == pp) break;
						if (opp.getTopology() != pp.getTopology()) continue;
						NodeInst nni = (NodeInst)portNodes.get(opp);
						if (nni == null) continue;
						PortInst head = nni.getOnlyPortInst();
						PortInst tail = pNi.getOnlyPortInst();
						ArcInst.newInstance(Generic.tech.universal_arc, 0, head, tail);
						break;
					}
				}
			}
//			minnodesize[nodetotal*2] = mainBounds.getWidth();
//			minnodesize[nodetotal*2+1] = mainBounds.getHeight();
			nodetotal++;
	
			// compact it accordingly
			us_tecedcompactNode(nNp);
		}
	
		// save the node sequence
		lib.newVar(NODESEQUENCE_KEY, nodeSequence);
	
//		// create the design rule information
//		rules = dr_allocaterules(layertotal, nodetotal, tech.techname);
//		if (rules == NODRCRULES) return(NOLIBRARY);
//		for(i=0; i<layertotal; i++)
//			(void)allocstring(&rules.layernames[i], layername(tech, i), el_tempcluster);
//		i = 0;
//		for(np = tech.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//			if (np.temp1 != 0)
//				(void)allocstring(&rules.nodenames[i++],  &((NODEPROTO *)np.temp1).protoname[5],
//					el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_min_widthkey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.numlayers; i++) rules.minwidth[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_min_width_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.numlayers; i++)
//				(void)reallocstring(&rules.minwidthR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_connected_distanceskey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.conlist[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_connected_distances_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.conlistR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_unconnected_distanceskey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.unconlist[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_unconnected_distances_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.unconlistR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_connected_distancesWkey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.conlistW[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_connected_distancesW_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.conlistWR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_unconnected_distancesWkey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.unconlistW[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_unconnected_distancesW_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.unconlistWR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_connected_distancesMkey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.conlistM[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_connected_distancesM_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.conlistMR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_unconnected_distancesMkey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.unconlistM[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_unconnected_distancesM_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.unconlistMR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_edge_distanceskey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.edgelist[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_edge_distances_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.edgelistR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT, dr_wide_limitkey);
//		if (var != NOVARIABLE) rules.widelimit = var.addr;
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_min_node_sizekey);
//		if (var != NOVARIABLE)
//		{
//			i = j = 0;
//			for(np = tech.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//			{
//				if (np.temp1 != 0)
//				{
//					rules.minnodesize[i*2] = ((INTBIG *)var.addr)[j*2];
//					rules.minnodesize[i*2+1] = ((INTBIG *)var.addr)[j*2+1];
//	
//					// if rule is valid, make sure it is no larger than actual size
//					if (rules.minnodesize[i*2] > 0 && rules.minnodesize[i*2+1] > 0)
//					{
//						if (rules.minnodesize[i*2] > minnodesize[i*2])
//							rules.minnodesize[i*2] = minnodesize[i*2];
//						if (rules.minnodesize[i*2+1] > minnodesize[i*2+1])
//							rules.minnodesize[i*2+1] = minnodesize[i*2+1];
//					}
//					i++;
//				}
//				j++;
//			}
//		}
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_min_node_size_rulekey);
//		if (var != NOVARIABLE)
//		{
//			i = j = 0;
//			for(np = tech.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//			{
//				if (np.temp1 != 0)
//				{
//					reallocstring(&rules.minnodesizeR[i], ((CHAR **)var.addr)[j], el_tempcluster);
//					i++;
//				}
//				j++;
//			}
//		}
//	
//		us_tecedloaddrcmessage(rules, lib);
//		dr_freerules(rules);
	
		// clean up
		System.out.println("Done.");
		return(lib);
	}
	
	/*************************** CELL CREATION HELPERS ***************************/
	
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

	private static void foundNodeForFunction(NodeInst ni, int func, SpecialTextDescr [] table)
	{
		for(int i=0; i<table.length; i++)
		{
			if (table[i].funct == func)
			{
				table[i].ni = ni;
				return;
			}
		}
	}

	private static void loadTableEntry(SpecialTextDescr [] table, int func, Object value)
	{
		for(int i=0; i<table.length; i++)
		{
			if (func == table[i].funct)
			{
				table[i].value = value;
				return;
			}
		}
	}

	/**
	 * Method to create special text geometry described by "table" in cell "np".
	 */
	static void us_tecedcreatespecialtext(Cell np, SpecialTextDescr [] table)
	{
		// don't create any nodes already there
		for(int i=0; i < table.length; i++) table[i].ni = null;
		for(Iterator it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			Variable var = ni.getVar(OPTION_KEY);
			if (var == null) continue;
			foundNodeForFunction(ni, ((Integer)var.getObject()).intValue(), table);
		}

		for(int i=0; i < table.length; i++)
		{
			if (table[i].ni != null) continue;
			table[i].ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(table[i].x, table[i].y), 0, 0, np);
			if (table[i].ni == null) return;
			String str = null;
			switch (table[i].funct)
			{
				case TECHLAMBDA:
					str = "Scale: " + ((Double)table[i].value).doubleValue();
					break;
				case TECHDESCRIPT:
					str = "Description: " + (String)table[i].value;
					break;
				case TECHSPICEMINRES:
					str = "Minimum Resistance: " + ((Double)table[i].value).doubleValue();
					break;
				case TECHSPICEMINCAP:
					str = "Minimum Capacitance: " + ((Double)table[i].value).doubleValue();
					break;
				case TECHGATESHRINK:
					str = "Gate Shrinkage: " + ((Double)table[i].value).doubleValue();
					break;
				case TECHGATEINCLUDED:
					str = "Gates Included in Resistance: " + (((Boolean)table[i].value).booleanValue() ? "Yes" : "No");
					break;
				case TECHGROUNDINCLUDED:
					str = "Parasitics Includes Ground: " + (((Boolean)table[i].value).booleanValue() ? "Yes" : "No");
					break;
				case TECHTRANSPCOLORS:
					table[i].ni.newVar(TRANSLAYER_KEY, makeTransparentColorsLine((Color [])table[i].value));
					str = "Transparent Colors";
					break;

				case LAYERFUNCTION:
					str = "Function: " + makeLayerFunctionName((Layer.Function)table[i].value, table[i].extra);
					break;
				case LAYERCOLOR:
					EGraphics desc = (EGraphics)table[i].value;
					str = "Color: " + desc.getColor().getRed() + "," + desc.getColor().getGreen() + "," +
						desc.getColor().getBlue() + ", " + desc.getOpacity() + "," + (desc.getForeground() ? "on" : "off");
					break;
				case LAYERTRANSPARENCY:
					desc = (EGraphics)table[i].value;
					str = "Transparency: " + (desc.getTransparentLayer() == 0 ? "none" : "layer " + desc.getTransparentLayer());
					break;
				case LAYERSTYLE:
					desc = (EGraphics)table[i].value;
					str = "Style: ";
					if (desc.isPatternedOnDisplay())
					{
						if (desc.isOutlinedOnDisplay())
						{
							str += "patterned/outlined";
						} else
						{
							str += "patterned";
						}
					} else
					{
						str += "solid";
					}
					break;
				case LAYERCIF:
					str = "CIF Layer: " + (String)table[i].value;
					break;
				case LAYERGDS:
					str = "GDS-II Layer: " + (String)table[i].value;
					break;
				case LAYERSPIRES:
					str = "SPICE Resistance: " + ((Double)table[i].value).doubleValue();
					break;
				case LAYERSPICAP:
					str = "SPICE Capacitance: " + ((Double)table[i].value).doubleValue();
					break;
				case LAYERSPIECAP:
					str = "SPICE Edge Capacitance: " + ((Double)table[i].value).doubleValue();
					break;
				case LAYER3DHEIGHT:
					str = "3D Height: " + ((Double)table[i].value).doubleValue();
					break;
				case LAYER3DTHICK:
					str = "3D Thickness: " + ((Double)table[i].value).doubleValue();
					break;
				case LAYERCOVERAGE:
					str = "Coverage percent: " + ((Double)table[i].value).doubleValue();
					break;

				case ARCFUNCTION:
					str = "Function: " + ((ArcProto.Function)table[i].value).toString();
					break;
				case ARCFIXANG:
					str = "Fixed-angle: " + (((Boolean)table[i].value).booleanValue() ? "Yes" : "No");
					break;
				case ARCWIPESPINS:
					str = "Wipes pins: "  + (((Boolean)table[i].value).booleanValue() ? "Yes" : "No");
					break;
				case ARCNOEXTEND:
					str = "Extend arcs: " + (((Boolean)table[i].value).booleanValue() ? "Yes" : "No");
					break;
				case ARCINC:
					str = "Angle increment: " + ((Integer)table[i].value).intValue();
					break;
				case ARCANTENNARATIO:
					str = "Antenna Ratio: " + ((Double)table[i].value).doubleValue();
					break;

				case NODEFUNCTION:
					str = "Function: " + ((PrimitiveNode.Function)table[i].value).toString();
					break;
				case NODESERPENTINE:
					str = "Serpentine transistor: " + (((Boolean)table[i].value).booleanValue() ? "Yes" : "No");
					break;
				case NODESQUARE:
					str = "Square node: " + (((Boolean)table[i].value).booleanValue() ? "Yes" : "No");
					break;
				case NODEWIPES:
					str = "Invisible with 1 or 2 arcs: " + (((Boolean)table[i].value).booleanValue() ? "Yes" : "No");
					break;
				case NODELOCKABLE:
					str = "Lockable: " + (((Boolean)table[i].value).booleanValue() ? "Yes" : "No");
					break;
			}
			Variable var = table[i].ni.newVar(Artwork.ART_MESSAGE, str);
			if (var != null)
				var.setDisplay(true);
			var = table[i].ni.newVar(OPTION_KEY, new Integer(table[i].funct));
		}
	}
	
	static NodeInst us_tecedplacegeom(Poly poly, Cell np)
	{
		Rectangle2D box = poly.getBox();
		Poly.Type style = poly.getStyle();
		if (style == Poly.Type.FILLED)
		{
			if (box != null)
			{
				return NodeInst.makeInstance(Artwork.tech.filledBoxNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
					box.getWidth(), box.getHeight(), np);
			} else
			{
				box = poly.getBounds2D();
				NodeInst nni = NodeInst.makeInstance(Artwork.tech.filledPolygonNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
					box.getWidth(), box.getHeight(), np);
				if (nni == null) return null;
				nni.setTrace(poly.getPoints());
				return nni;
			}
		}
		if (style == Poly.Type.CLOSED)
		{
			if (box != null)
			{
				return NodeInst.makeInstance(Artwork.tech.boxNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
					box.getWidth(), box.getHeight(), np);
			} else
			{
				box = poly.getBounds2D();
				NodeInst nni = NodeInst.makeInstance(Artwork.tech.closedPolygonNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
					box.getWidth(), box.getHeight(), np);
				if (nni == null) return null;
				nni.setTrace(poly.getPoints());
				return nni;
			}
		}
		if (style == Poly.Type.CROSSED)
		{
			if (box == null) box = poly.getBounds2D();
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.crossedBoxNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			return nni;
		}
		if (style == Poly.Type.OPENED)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.openedPolygonNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			nni.setTrace(poly.getPoints());
			return nni;
		}
		if (style == Poly.Type.OPENEDT1)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.openedDottedPolygonNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			nni.setTrace(poly.getPoints());
			return nni;
		}
		if (style == Poly.Type.OPENEDT2)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.openedDashedPolygonNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			nni.setTrace(poly.getPoints());
			return nni;
		}
		if (style == Poly.Type.OPENEDT3)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.openedThickerPolygonNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			nni.setTrace(poly.getPoints());
			return nni;
		}
		if (style == Poly.Type.CIRCLE)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			return nni;
		}
		if (style == Poly.Type.THICKCIRCLE)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.thickCircleNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			return nni;
		}
		if (style == Poly.Type.DISC)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.filledCircleNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			return nni;
		}
		if (style == Poly.Type.CIRCLEARC)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			nni.setArcDegrees(0.0, 45.0*Math.PI/180.0);
			return nni;
		}
		if (style == Poly.Type.THICKCIRCLEARC)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.thickCircleNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			nni.setArcDegrees(0.0, 45.0*Math.PI/180.0);
			return nni;
		}
		if (style == Poly.Type.TEXTCENT)
		{
			NodeInst nni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			Variable var = nni.newVar(Artwork.ART_MESSAGE, poly.getString());
			if (var != null)
			{
				var.setDisplay(true);
				var.setPos(TextDescriptor.Position.CENT);
			}
			return nni;
		}
		if (style == Poly.Type.TEXTBOTLEFT)
		{
			NodeInst nni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			Variable var = nni.newVar(Artwork.ART_MESSAGE, poly.getString());
			if (var != null)
			{
				var.setDisplay(true);
				var.setPos(TextDescriptor.Position.UPRIGHT);
			}
			return nni;
		}
		if (style == Poly.Type.TEXTBOTRIGHT)
		{
			NodeInst nni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			Variable var = nni.newVar(Artwork.ART_MESSAGE, poly.getString());
			if (var != null)
			{
				var.setDisplay(true);
				var.setPos(TextDescriptor.Position.UPLEFT);
			}
			return nni;
		}
		if (style == Poly.Type.TEXTBOX)
		{
			NodeInst nni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			Variable var = nni.newVar(Artwork.ART_MESSAGE, poly.getString());
			if (var != null)
			{
				var.setDisplay(true);
				var.setPos(TextDescriptor.Position.BOXED);
			}
			return nni;
		}
		return(null);
	}
	
	static Color [] getTransparentColors(String str)
	{
		String [] colorNames = str.split("/");
		Color [] colors = new Color[colorNames.length];
		for(int i=0; i<colorNames.length; i++)
		{
			String colorName = colorNames[i].trim();
			String [] rgb = colorName.split(",");
			if (rgb.length != 3) return null;
			int r = TextUtils.atoi(rgb[0]);
			int g = TextUtils.atoi(rgb[1]);
			int b = TextUtils.atoi(rgb[2]);
			colors[i] = new Color(r, g, b);
		}
		return colors;
	}

	static String makeTransparentColorsLine(Color [] trans)
	{
		String str = "The Transparent Colors: ";
		for(int j=0; j<trans.length; j++)
		{
			if (j != 0) str += " /";
			str += " " + trans[j].getRed() + "," + trans[j].getGreen() + "," + trans[j].getBlue();
		}
		return str;
	}
	
	/**
	 * Method to compact an Arc technology-edit cell
	 */
	private static void us_tecedcompactArc(Cell cell)
	{
		// compute bounds of arc contents
		Rectangle2D nonSpecBounds = null;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() == Generic.tech.cellCenterNode) continue;

			// ignore the special text nodes
			boolean special = false;
			for(int i=0; i<us_tecedarctexttable.length; i++)
				if (us_tecedarctexttable[i].ni == ni) special = true;
			if (special) continue;

			// compute overall bounds
			Rectangle2D bounds = ni.getBounds();
			if (nonSpecBounds == null) nonSpecBounds = bounds; else
				Rectangle2D.union(nonSpecBounds, bounds, nonSpecBounds);
		}

		// now rearrange the geometry
		if (nonSpecBounds != null)
		{
			double xoff = -nonSpecBounds.getCenterX();
			double yoff = -nonSpecBounds.getMaxY();
			if (xoff != 0 || yoff != 0)
			{
				for(Iterator it = cell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = (NodeInst)it.next();
					if (ni.getProto() == Generic.tech.cellCenterNode) continue;

					// ignore the special text nodes
					boolean special = false;
					for(int i=0; i<us_tecedarctexttable.length; i++)
						if (us_tecedarctexttable[i].ni == ni) special = true;
					if (special) continue;

					// center the geometry
					ni.modifyInstance(xoff, yoff, 0, 0, 0);
				}
			}
		}
	}
	
	/**
	 * Method to compact a Node technology-edit cell
	 */
	private static void us_tecedcompactNode(Cell cell)
	{
		// move the examples
		Parse.Example nelist = Parse.us_tecedgetexamples(cell, true);
		if (nelist == null) return;
		int numexamples = 0;
		Parse.Example smallest = nelist;
		Parse.Example biggest = nelist;
		for(Parse.Example ne = nelist; ne != null; ne = ne.nextexample)
		{
			numexamples++;
			if (ne.hx-ne.lx > biggest.hx-biggest.lx) biggest = ne;
		}
		if (numexamples == 1)
		{
			moveExample(nelist, -(nelist.lx + nelist.hx) / 2, -nelist.hy);
			return;
		}
		if (numexamples != 4) return;

		Parse.Example stretchX = null;
		Parse.Example stretchY = null;
		for(Parse.Example ne = nelist; ne != null; ne = ne.nextexample)
		{
			if (ne == biggest || ne == smallest) continue;
			if (stretchX == null) stretchX = ne; else
				if (stretchY == null) stretchY = ne;
		}
		if (stretchX.hx-stretchX.lx < stretchY.hx-stretchY.lx)
		{
			Parse.Example swap = stretchX;
			stretchX = stretchY;
			stretchY = swap;
		}

		double separation = Math.min(smallest.hx - smallest.lx, smallest.hy - smallest.ly);
		double totalWid = (stretchX.hx-stretchX.lx) + (smallest.hx-smallest.lx) + separation;
		double totalHei = (stretchY.hy-stretchY.ly) + (smallest.hy-smallest.ly) + separation;

		// center the smallest (main) example
		double cX = -totalWid / 2 - smallest.lx;
		double cY = -smallest.hy - 1;
		moveExample(smallest, cX, cY);

		// center the stretch-x (upper-right) example
		cX = totalWid/2 - stretchX.hx;
		cY = -stretchX.hy - 1;
		moveExample(stretchX, cX, cY);

		// center the stretch-y (lower-left) example
		cX = -totalWid/2 - stretchY.lx;
		cY = -totalHei - stretchY.ly - 1;
		moveExample(stretchY, cX, cY);

		// center the biggest (lower-right) example
		cX = totalWid/2 - biggest.hx;
		cY = -totalHei - biggest.ly - 1;
		moveExample(biggest, cX, cY);
	}

	private static void moveExample(Parse.Example ne, double dX, double dY)
	{
		for(Parse.Sample ns = ne.firstsample; ns != null; ns = ns.nextsample)
		{
			ns.node.modifyInstance(dX, dY, 0, 0, 0);
		}
	}
}
