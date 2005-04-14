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
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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

		/**
		 * Method to parse the miscellaneous-info cell in "np" and return a GeneralInfo object that describes it.
		 */
		public static GeneralInfo us_teceditgetlayerinfo(Cell np)
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
					case Generate.TECHLAMBDA:
						gi.scale = TextUtils.atof(str);
						break;
					case Generate.TECHDESCRIPT:
						gi.description = str;
						break;
					case Generate.CENTEROBJ:
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
		EGraphics desc;
		Layer.Function fun;
		int funExtra;
		String cif;
		String dxf;
		String gds;
		double spires;
		double spicap;
		double spiecap;
		double height3d;
		double thick3d;
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
			li.dxf = "";
			li.gds = "";
			return li;
		}

		/**
		 * Method to build the appropriate descriptive information for a layer into
		 * cell "np".  The color is "colorindex"; the stipple array is in "stip"; the
		 * layer style is in "style", the CIF layer is in "ciflayer"; the function is
		 * in "functionindex"; the DXF layer name(s)
		 * are "dxf"; the Calma GDS-II layer is in "gds"; the SPICE resistance is in "spires",
		 * the SPICE capacitance is in "spicap", the SPICE edge capacitance is in "spiecap",
		 * the 3D height is in "height3d", and the 3D thickness is in "thick3d".
		 */
		void us_tecedmakelayer(Cell np)
		{
			NodeInst laystipple = null, laypatclear = null, laypatinvert = null, laypatcopy = null, laypatpaste = null;
			for(Iterator it = np.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				Variable var = ni.getVar(OPTION_KEY);
				if (var == null) continue;
				switch (((Integer)var.getObject()).intValue())
				{
					case LAYERPATTERN:   laystipple = ni;    break;
					case LAYERPATCLEAR:  laypatclear = ni;   break;
					case LAYERPATINVERT: laypatinvert = ni;  break;
					case LAYERPATCOPY:   laypatcopy = ni;    break;
					case LAYERPATPASTE:  laypatpaste = ni;   break;
				}
			}
		
			// create the transparency information if it is not there
			Variable patchVar = np.getVar(Generate.COLORNODE_KEY);
			if (patchVar == null)
			{
				// create the graphic color object
				NodeInst nicolor = NodeInst.makeInstance(Artwork.tech.filledBoxNode, new Point2D.Double(-15000/SCALEALL,15000/SCALEALL),
					10000/SCALEALL, 10000/SCALEALL, np);
				if (nicolor == null) return;
				Manipulate.us_teceditsetpatch(nicolor, desc);
				np.newVar(COLORNODE_KEY, nicolor);
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
			for(int i=0; i<us_tecedlayertexttable.length; i++)
			{
				switch (us_tecedlayertexttable[i].funct)
				{
					case LAYERFUNCTION:
						us_tecedlayertexttable[i].value = fun;
						us_tecedlayertexttable[i].extra = funExtra;
						break;
					case LAYERCOLOR:
						us_tecedlayertexttable[i].value = desc;
						break;
					case LAYERTRANSPARENCY:
						us_tecedlayertexttable[i].value = desc;
						break;
					case LAYERSTYLE:
						us_tecedlayertexttable[i].value = desc;
						break;
					case LAYERCIF:
						us_tecedlayertexttable[i].value = cif;
						break;
					case LAYERGDS:
						us_tecedlayertexttable[i].value = gds;
						break;
					case LAYERDXF:
						us_tecedlayertexttable[i].value = dxf;
						break;
					case LAYERSPIRES:
						us_tecedlayertexttable[i].value = new Double(spires);
						break;
					case LAYERSPICAP:
						us_tecedlayertexttable[i].value = new Double(spicap);
						break;
					case LAYERSPIECAP:
						us_tecedlayertexttable[i].value = new Double(spiecap);
						break;
					case LAYER3DHEIGHT:
						us_tecedlayertexttable[i].value = new Double(height3d);
						break;
					case LAYER3DTHICK:
						us_tecedlayertexttable[i].value = new Double(thick3d);
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
					case LAYERDXF:
						li.dxf = str;
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
		ArcProto.Function func;
		boolean fixang;
		boolean wipes;
		boolean noextend;
		int anginc;
		ArcProto generated;
		ArcDetails [] arcDetails;
		double widthOffset;
		double maxWidth;

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
			for(int i=0; i<us_tecedarctexttable.length; i++)
			{
				switch (us_tecedarctexttable[i].funct)
				{
					case ARCFUNCTION:
						us_tecedarctexttable[i].value = func;
						break;
					case ARCFIXANG:
						us_tecedarctexttable[i].value = new Boolean(fixang);
						break;
					case ARCWIPESPINS:
						us_tecedarctexttable[i].value = new Boolean(wipes);
						break;
					case ARCNOEXTEND:
						us_tecedarctexttable[i].value = new Boolean(noextend);
						break;
					case ARCINC:
						us_tecedarctexttable[i].value = new Integer(anginc);
						break;
				}
			}
		
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
		Parse.Rule rule;
	}

	static class NodeLayerDetails
	{
		LayerInfo layer;
		Poly.Type style;
		Parse.Rule rule;
		int portIndex;
	}

	static class NodeInfo
	{
		String name;
		PrimitiveNode.Function func;
		boolean serp;
		boolean square;
		boolean wipes;
		boolean lockable;
		double multicutsep;
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
			for(int i=0; i < us_tecednodetexttable.length; i++)
			{
				switch (us_tecednodetexttable[i].funct)
				{
					case NODEFUNCTION:
						us_tecednodetexttable[i].value = func;
						break;
					case NODESERPENTINE:
						us_tecednodetexttable[i].value = new Boolean(serp);
						break;
					case NODESQUARE:
						us_tecednodetexttable[i].value = new Boolean(square);
						break;
					case NODEWIPES:
						us_tecednodetexttable[i].value = new Boolean(wipes);
						break;
					case NODELOCKABLE:
						us_tecednodetexttable[i].value = new Boolean(lockable);
						break;
					case NODEMULTICUT:
						us_tecednodetexttable[i].value = new Double(multicutsep);
						break;
				}
			}
		
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
					case NODEMULTICUT:
						nin.multicutsep = TextUtils.atof(str);
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

	/* the meaning of OPTION_KEY on nodes */
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
	static final int LAYERPATPASTE     = 37;					/* paste the pattern (layer cell) */

	/** key of Variable holding option information. */	public static final Variable.Key OPTION_KEY = ElectricObject.newKey("EDTEC_option");
	/** key of Variable holding layer information. */	public static final Variable.Key LAYER_KEY = ElectricObject.newKey("EDTEC_layer");
	/** key of Variable holding arc ordering. */		public static final Variable.Key ARCSEQUENCE_KEY = ElectricObject.newKey("EDTEC_arcsequence");
	/** key of Variable holding node ordering. */		public static final Variable.Key NODESEQUENCE_KEY = ElectricObject.newKey("EDTEC_nodesequence");
	/** key of Variable holding layer ordering. */		public static final Variable.Key LAYERSEQUENCE_KEY = ElectricObject.newKey("EDTEC_layersequence");
	/** key of Variable holding extra variables. */		public static final Variable.Key VARIABLELIST_KEY = ElectricObject.newKey("EDTEC_variable_list");
	/** key of Variable marking geometry as min-size. */public static final Variable.Key MINSIZEBOX_KEY = ElectricObject.newKey("EDTEC_minbox");
	/** key of Variable holding port name. */			public static final Variable.Key PORTNAME_KEY = ElectricObject.newKey("EDTEC_portname");
	/** key of Variable holding port angle. */			public static final Variable.Key PORTANGLE_KEY = ElectricObject.newKey("EDTEC_portangle");
	/** key of Variable holding port range. */			public static final Variable.Key PORTRANGE_KEY = ElectricObject.newKey("EDTEC_portrange");
	/** key of Variable holding arc connection list. */	public static final Variable.Key CONNECTION_KEY = ElectricObject.newKey("EDTEC_connects");
	/** key of Variable with color node in layer cell. */public static final Variable.Key COLORNODE_KEY = ElectricObject.newKey("EDTEC_colornode");
	/** key of Variable with color map table. */		public static final Variable.Key COLORMAP_KEY = ElectricObject.newKey("EDTEC_colormap");
	/** key of Variable with color map table. */		public static final Variable.Key DEPENDENTLIB_KEY = ElectricObject.newKey("EDTEC_dependent_libraries");

	/* additional technology variables */
	static class TechVar
	{
		String    varname;
		TechVar   nexttechvar;
		boolean   changed;
		int       ival;
		float     fval;
		String    sval;
		int       vartype;
		String    description;

		TechVar(String name, String desc)
		{
			varname = name;
			description = desc;
		}
	};
	
//	#define MAXNAMELEN 25		/* max chars in a new name */
//	
//	INTBIG us_teceddrclayers = 0;
//	CHAR **us_teceddrclayernames = 0;
	
	/* the known technology variables */
	static TechVar [] us_knownvars =
	{
		new TechVar("DRC_ecad_deck",             /*VSTRING|VISARRAY,*/ "Dracula design-rule deck"),
		new TechVar("IO_cif_polypoints",         /*VINTEGER,*/         "Maximum points in a CIF polygon"),
		new TechVar("IO_cif_resolution",         /*VINTEGER,*/         "Minimum resolution of CIF coordinates"),
		new TechVar("IO_gds_polypoints",         /*VINTEGER,*/         "Maximum points in a GDS-II polygon"),
		new TechVar("SIM_spice_min_resistance",  /*VFLOAT,*/           "Minimum resistance of SPICE elements"),
		new TechVar("SIM_spice_min_capacitance", /*VFLOAT,*/           "Minimum capacitance of SPICE elements"),
		new TechVar("SIM_spice_mask_scale",      /*VFLOAT,*/           "Scaling factor for SPICE decks"),
		new TechVar("SIM_spice_header_level1",   /*VSTRING|VISARRAY,*/ "Level 1 header for SPICE decks"),
		new TechVar("SIM_spice_header_level2",   /*VSTRING|VISARRAY,*/ "Level 2 header for SPICE decks"),
		new TechVar("SIM_spice_header_level3",   /*VSTRING|VISARRAY,*/ "Level 3 header for SPICE decks"),
		new TechVar("SIM_spice_model_file",      /*VSTRING,*/          "Disk file with SPICE header cards"),
		new TechVar("SIM_spice_trailer_file",    /*VSTRING,*/          "Disk file with SPICE trailer cards")
	};
//
//
//	/* the meaning of "us_tecflags" */
//	#define HASDRCMINWID         01				/* has DRC minimum width information */
//	#define HASDRCMINWIDR        02				/* has DRC minimum width information */
//	#define HASCOLORMAP          04				/* has color map */
//	#define HASARCWID           010				/* has arc width offset factors */
//	#define HASCIF              020				/* has CIF layers */
//	#define HASDXF              040				/* has DXF layers */
//	#define HASGDS             0100				/* has Calma GDS-II layers */
//	#define HASGRAB            0200				/* has grab point information */
//	#define HASSPIRES          0400				/* has SPICE resistance information */
//	#define HASSPICAP         01000				/* has SPICE capacitance information */
//	#define HASSPIECAP        02000				/* has SPICE edge capacitance information */
//	#define HAS3DINFO         04000				/* has 3D height/thickness information */
//	#define HASCONDRC        010000				/* has connected design rules */
//	#define HASCONDRCR       020000				/* has connected design rules reasons */
//	#define HASUNCONDRC      040000				/* has unconnected design rules */
//	#define HASUNCONDRCR    0100000				/* has unconnected design rules reasons */
//	#define HASCONDRCW      0200000				/* has connected wide design rules */
//	#define HASCONDRCWR     0400000				/* has connected wide design rules reasons */
//	#define HASUNCONDRCW   01000000				/* has unconnected wide design rules */
//	#define HASUNCONDRCWR  02000000				/* has unconnected wide design rules reasons */
//	#define HASCONDRCM     04000000				/* has connected multicut design rules */
//	#define HASCONDRCMR   010000000				/* has connected multicut design rules reasons */
//	#define HASUNCONDRCM  020000000				/* has unconnected multicut design rules */
//	#define HASUNCONDRCMR 040000000				/* has unconnected multicut design rules reasons */
//	#define HASEDGEDRC   0100000000				/* has edge design rules */
//	#define HASEDGEDRCR  0200000000				/* has edge design rules reasons */
//	#define HASMINNODE   0400000000				/* has minimum node size */
//	#define HASMINNODER 01000000000				/* has minimum node size reasons */
//	#define HASPRINTCOL 02000000000				/* has print colors */

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

//	/* the globals that define a technology */
//	extern INTBIG           us_teceddrclayers;
//	extern CHAR           **us_teceddrclayernames;

	static SpecialTextDescr [] us_tecedmisctexttable =
	{
		new SpecialTextDescr(0/SCALEALL, 6000/SCALEALL, TECHLAMBDA),
		new SpecialTextDescr(0/SCALEALL,    0/SCALEALL, TECHDESCRIPT)
	};
	
	static SpecialTextDescr [] us_tecedlayertexttable =
	{
		new SpecialTextDescr(28000/SCALEALL,  36000/SCALEALL, LAYERFUNCTION),
		new SpecialTextDescr(28000/SCALEALL,  30000/SCALEALL, LAYERCOLOR),
		new SpecialTextDescr(28000/SCALEALL,  24000/SCALEALL, LAYERTRANSPARENCY),
		new SpecialTextDescr(28000/SCALEALL,  18000/SCALEALL, LAYERSTYLE),
		new SpecialTextDescr(28000/SCALEALL,  12000/SCALEALL, LAYERCIF),
		new SpecialTextDescr(28000/SCALEALL,   6000/SCALEALL, LAYERGDS),
		new SpecialTextDescr(28000/SCALEALL,      0/SCALEALL, LAYERDXF),
		new SpecialTextDescr(28000/SCALEALL,  -6000/SCALEALL, LAYERSPIRES),
		new SpecialTextDescr(28000/SCALEALL, -12000/SCALEALL, LAYERSPICAP),
		new SpecialTextDescr(28000/SCALEALL, -18000/SCALEALL, LAYERSPIECAP),
		new SpecialTextDescr(28000/SCALEALL, -24000/SCALEALL, LAYER3DHEIGHT),
		new SpecialTextDescr(28000/SCALEALL, -30000/SCALEALL, LAYER3DTHICK)
	};
	
	static SpecialTextDescr [] us_tecedarctexttable =
	{
		new SpecialTextDescr(0/SCALEALL, 30000/SCALEALL, ARCFUNCTION),
		new SpecialTextDescr(0/SCALEALL, 24000/SCALEALL, ARCFIXANG),
		new SpecialTextDescr(0/SCALEALL, 18000/SCALEALL, ARCWIPESPINS),
		new SpecialTextDescr(0/SCALEALL, 12000/SCALEALL, ARCNOEXTEND),
		new SpecialTextDescr(0/SCALEALL,  6000/SCALEALL, ARCINC)
	};
	
	static SpecialTextDescr [] us_tecednodetexttable =
	{
		new SpecialTextDescr(0/SCALEALL, 36000/SCALEALL, NODEFUNCTION),
		new SpecialTextDescr(0/SCALEALL, 30000/SCALEALL, NODESERPENTINE),
		new SpecialTextDescr(0/SCALEALL, 24000/SCALEALL, NODESQUARE),
		new SpecialTextDescr(0/SCALEALL, 18000/SCALEALL, NODEWIPES),
		new SpecialTextDescr(0/SCALEALL, 12000/SCALEALL, NODELOCKABLE),
		new SpecialTextDescr(0/SCALEALL,  6000/SCALEALL, NODEMULTICUT)
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
	
		// create the information node
		Cell fNp = Cell.makeInstance(lib, "factors");
		if (fNp == null) return null;
		fNp.setInTechnologyLibrary();
	
		// create the miscellaneous info cell (called "factors")
		us_tecedmakeinfo(fNp, tech.getTechDesc());
	
		// copy any miscellaneous variables and make a list of their names
		int varCount = 0;
//		for(int i=0; i<us_knownvars.length; i++)
//		{
//			us_knownvars[i].ival = 0;
//			Variable var = tech.getVar(us_knownvars[i].varname);
//			if (var == null) continue;
//			us_knownvars[i].ival = 1;
//			varCount++;
//			lib.newVar(us_knownvars[i].varname, var.getObject());
//		}
		if (varCount > 0)
		{
			String [] varnames = new String[varCount];
			varCount = 0;
			for(int i=0; i<us_knownvars.length; i++)
				if (us_knownvars[i].ival != 0) varnames[varCount++] = us_knownvars[i].varname;
			lib.newVar(VARIABLELIST_KEY, varnames);
		}
	
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

			Cell lNp = Cell.makeInstance(lib, fname);
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
			li.dxf = layer.getDXFLayer();
	
			// compute the SPICE information
			li.spires = layer.getResistance();
			li.spicap = layer.getCapacitance();
			li.spiecap = layer.getEdgeCapacitance();
	
			// compute the 3D information
			li.height3d = layer.getDepth();
			li.thick3d = layer.getThickness();
	
			// build the layer cell
			li.us_tecedmakelayer(lNp);
			layerSequence[i] = lNp.getName();
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
			arcCells.put(ap, aNp);
//			var = getvalkey((INTBIG)ap, VARCPROTO, VINTEGER, us_arcstylekey);
//			if (var != NOVARIABLE) bits = var.addr; else
//				bits = ap.userbits;
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
//				if (arcDesc.bits == LAYERN) continue;
	
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
//			us_tecedcompact(aNp);
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
//					if (desc.bits == LAYERN) continue;

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
						nIn.multicutsep = 0;
						if (pnp.getSpecialType() == PrimitiveNode.MULTICUT)
						{
							double [] values = pnp.getSpecialValues();
							nIn.multicutsep = values[4];
						}
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
					int tcon = 0;
					for(int i=0; i<connects.length; i++)
					{
						if (connects[i].getTechnology() == tech) tcon++;
					}
					if (tcon != 0)
					{
						Cell [] aplist = new Cell[tcon];
						int k = 0;
						for(int i=0; i<connects.length; i++)
						{
							if (connects[i].getTechnology() == tech) aplist[k++] = (Cell)arcCells.get(connects[i]);
						}
						pNi.newVar(CONNECTION_KEY, aplist);
					}
	
					// connect the connected ports
//					for(opp = pnp.firstportproto; opp != pp; opp = opp.nextportproto)
//					{
//						if (opp.network != pp.network) continue;
//						nni = (NODEINST *)opp.temp1;
//						if (nni == null) continue;
//						if (newarcinst(gen_universalarc, 0, 0, pNi, pNi.proto.firstportproto,
//							(pNi.highx+pNi.lowx)/2, (pNi.highy+pNi.lowy)/2, nni,
//								nni.proto.firstportproto, (nni.highx+nni.lowx)/2,
//									(nni.highy+nni.lowy)/2, np) == NOARCINST) return(NOLIBRARY);
//						break;
//					}
				}
			}
//			minnodesize[nodetotal*2] = mainBounds.getWidth();
//			minnodesize[nodetotal*2+1] = mainBounds.getHeight();
			nodetotal++;
	
			// compact it accordingly
//			us_tecedcompact(np);
		}
	
		// save the node sequence
		lib.newVar(NODESEQUENCE_KEY, nodeSequence);
	
		// create the color map information
//		System.out.println("Adding color map and design rules...");
//		Variable var2 = getval((INTBIG)tech, VTECHNOLOGY, VCHAR|VISARRAY, x_("USER_color_map"));
//		Variable varred = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER|VISARRAY, us_colormap_red_key);
//		Variable vargreen = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER|VISARRAY, us_colormap_green_key);
//		Variable varblue = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER|VISARRAY, us_colormap_blue_key);
//		if (varred != NOVARIABLE && vargreen != NOVARIABLE && varblue != NOVARIABLE &&
//			var2 != NOVARIABLE)
//		{
//			newmap = emalloc((256*3*SIZEOFINTBIG), el_tempcluster);
//			if (newmap == 0) return(NOLIBRARY);
//			mapptr = newmap;
//			colmap = (TECH_COLORMAP *)var2.addr;
//			for(i=0; i<256; i++)
//			{
//				*mapptr++ = ((INTBIG *)varred.addr)[i];
//				*mapptr++ = ((INTBIG *)vargreen.addr)[i];
//				*mapptr++ = ((INTBIG *)varblue.addr)[i];
//			}
//			for(i=0; i<32; i++)
//			{
//				newmap[(i<<2)*3]   = colmap[i].red;
//				newmap[(i<<2)*3+1] = colmap[i].green;
//				newmap[(i<<2)*3+2] = colmap[i].blue;
//			}
//			lib.newVar(COLORMAP_KEY, newmap);
//			efree((CHAR *)newmap);
//		}
	
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
	
	/**
	 * Method to build the appropriate descriptive information for the information
	 * cell "np".
	 */
	private static void us_tecedmakeinfo(Cell np, String description)
	{
		// load up the structure with the current values
		for(int i=0; i < us_tecedmisctexttable.length; i++)
		{
			switch (us_tecedmisctexttable[i].funct)
			{
				case TECHLAMBDA:
					us_tecedmisctexttable[i].value = new Double(100);
					break;
				case TECHDESCRIPT:
					us_tecedmisctexttable[i].value = description;
					break;
			}
		}
	
		// now create those text objects
		us_tecedcreatespecialtext(np, us_tecedmisctexttable);
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

	/**
	 * Method to create special text geometry described by "table" in cell "np".
	 */
	static void us_tecedcreatespecialtext(Cell np, SpecialTextDescr [] table)
	{
		us_tecedfindspecialtext(np, table);
		for(int i=0; i < table.length; i++)
		{
			NodeInst ni = table[i].ni;
			if (ni == null)
			{
				ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(table[i].x, table[i].y), 0, 0, np);
				if (ni == null) return;
				String str = null;
				switch (table[i].funct)
				{
					case TECHLAMBDA:
						str = "Scale: " + ((Double)table[i].value).doubleValue();
						break;
					case TECHDESCRIPT:
						str = "Description: " + (String)table[i].value;
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
					case LAYERDXF:
						str = "DXF Layer(s): " + (String)table[i].value;
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
					case NODEMULTICUT:
						str = "Multicut separation: " + ((Double)table[i].value).doubleValue();
						break;
				}
				Variable var = ni.newVar(Artwork.ART_MESSAGE, str);
				if (var != null)
					var.setDisplay(true);
				var = ni.newVar(OPTION_KEY, new Integer(table[i].funct));
			}
		}
	}
	
	/**
	 * Method to locate the nodes with the special node-cell text.  In cell "np", finds
	 * the relevant text nodes in "table" and loads them into the structure.
	 */
	static void us_tecedfindspecialtext(Cell np, SpecialTextDescr [] table)
	{
		// clear the node assignments
		for(int i=0; i < table.length; i++)
			table[i].ni = null;
	
		// determine the number of special texts here
		for(Iterator it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			Variable var = ni.getVar(OPTION_KEY);
			if (var == null) continue;
			int opt = ((Integer)var.getObject()).intValue();
			for(int i=0; i < table.length; i++)
			{
				if (opt != table[i].funct) continue;
				table[i].ni = ni;
				break;
			}
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
}
