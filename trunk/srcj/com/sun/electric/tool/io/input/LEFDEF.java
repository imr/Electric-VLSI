/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LEFDEF.java
 * Input/output tool: LEF and DEF helpers
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

import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class defines supporting structures and methods for reading of LEF and DEF files.
 */
public class LEFDEF extends Input
{
	// special controls to ignore LEF/DEF data (set false to eliminate data)
	protected static final boolean PLACELEFGEOMETRY = true;
	protected static final boolean PLACEDEFNETS = true;

	protected static ViaDef firstViaDefFromLEF = null;
	protected static HashMap<ArcProto,Double> widthsFromLEF = new HashMap<ArcProto,Double>();
	protected static Map<String,GetLayerInformation> knownLayers;
	protected static final double OVERALLSCALE = 1;

	protected static Variable.Key prXkey = Variable.newKey("ATTR_LEFwidth");
	protected static Variable.Key prYkey = Variable.newKey("ATTR_LEFheight");

	/**
	 * Class to define Via information for LEF and DEF reading.
	 */
	protected static class ViaDef
	{
		protected String    viaName;
		protected NodeProto via;
		protected ArcProto  lay1, lay2;
		protected double    sX, sY;
		protected ViaDef    nextViaDef;
	};

	/**
	 * Method to find a layer from its name.
	 * Uses a map of known layers, and analyzes the name if none is found.
	 * @param name the layer name.
	 * @return the layer information object.
	 */
	protected GetLayerInformation getLayerInformation(String name)
	{
		if (knownLayers != null)
		{
			GetLayerInformation li = knownLayers.get(name);
			if (li != null) return li;
		}
		GetLayerInformation li = new GetLayerInformation(name);
		return li;
	}

	/**
	 * Class to define layer information for LEF and DEF reading.
	 */
	protected static class GetLayerInformation
	{
		NodeProto pin;
		NodeProto pure;
		ArcProto arc;
		ArcProto.Function arcFun;
		Layer.Function layerFun;
		ArcProto viaArc1, viaArc2;

		private NodeProto getPureLayerNode()
		{
			// find the pure layer node with this function
			for(Iterator<Layer> it = Technology.getCurrent().getLayers(); it.hasNext(); )
			{
				Layer lay = it.next();
				if (lay.getFunction() == layerFun)
				{
					return lay.getPureLayerNode();
				}
			}
			return null;
		}

		GetLayerInformation(String name)
		{
			initialize(name, null);
		}

		GetLayerInformation(String name, String type)
		{
			initialize(name, type);
		}

		private void initialize(String name, String type)
		{
			// initialize
			pin = null;
			pure = null;
			arc = null;
			arcFun = ArcProto.Function.UNKNOWN;
			layerFun = Layer.Function.UNKNOWN;
			viaArc1 = viaArc2 = null;

			// first handle known layer names
			if (name.startsWith("POLY"))
			{
				setupPolyLayer(name.substring(4));
				return;
			}
			if (name.equals("PDIFF"))
			{
				arcFun = ArcProto.Function.DIFFP;
				layerFun = Layer.Function.DIFFP;
				pure = getPureLayerNode();
				return;
			}
			if (name.equals("NDIFF"))
			{
				arcFun = ArcProto.Function.DIFFN;
				layerFun = Layer.Function.DIFFN;
				pure = getPureLayerNode();
				return;
			}
			if (name.equals("DIFF"))
			{
				arcFun = ArcProto.Function.DIFF;
				layerFun = Layer.Function.DIFF;
				pure = getPureLayerNode();
				return;
			}
			if (name.equals("CONT"))
			{
				layerFun = Layer.Function.CONTACT1;
				pure = getPureLayerNode();
				return;
			}

			// handle via layers
			int j = 0;
			name = name.toUpperCase();
			if (name.startsWith("VIA")) j = 3; else
				if (name.startsWith("V")) j = 1;
			if (j != 0)
			{
				setupViaLayer(name.substring(j));
				return;
			}

			// handle metal layers
			j = 0;
			if (name.startsWith("METAL")) j = 5; else
				if (name.startsWith("MET")) j = 3; else
					if (name.startsWith("M")) j = 1;
			if (j != 0)
			{
				setupMetalLayer(name.substring(j));
				return;
			}

			// if type is given, use it
			if (type != null)
			{
				if (type.equalsIgnoreCase("masterslice"))
				{
					// masterslice layers are typically polysilicon
					j = 0;
					name = name.toUpperCase();
					if (name.startsWith("POLY")) j = 4; else
						if (name.startsWith("P")) j = 1;
					setupPolyLayer(name.substring(j));
					return;
				}
				if (type.equalsIgnoreCase("cut"))
				{
					j = 0;
					name = name.toUpperCase();
					if (name.startsWith("VIA")) j = 3; else
						if (name.startsWith("V")) j = 1;
					setupViaLayer(name.substring(j));
					return;
				}
				if (type.equalsIgnoreCase("routing"))
				{
					j = 0;
					if (name.startsWith("METAL")) j = 5; else
						if (name.startsWith("MET")) j = 3; else
							if (name.startsWith("M")) j = 1;
					name = name.substring(j);
					while (name.length() > 0 && !Character.isDigit(name.charAt(0)))
						name = name.substring(1);
					setupMetalLayer(name);
					return;
				}
			}
		}

		private void setupMetalLayer(String name)
		{
			int layNum = TextUtils.atoi(name);
			arcFun = ArcProto.Function.getMetal(layNum);
			layerFun = Layer.Function.getMetal(layNum);
			if (arcFun == null || layerFun == null) return;

			// find the arc with this function
			for(Iterator<ArcProto> it = Technology.getCurrent().getArcs(); it.hasNext(); )
			{
				ArcProto ap = it.next();
				if (ap.getFunction() == arcFun)
				{
					arc = ap;
					pin = ap.findPinProto();
					break;
				}
			}

			// find the pure layer node with this function
			pure = getPureLayerNode();
		}

		private void setupPolyLayer(String name)
		{			
			int layNum = TextUtils.atoi(name);
			if (layNum == 0) layNum = 1;
			arcFun = ArcProto.Function.getPoly(layNum);
			layerFun = Layer.Function.getPoly(layNum);
			if (arcFun == null || layerFun == null) return;

			// find the arc with this function
			for(Iterator<ArcProto> it = Technology.getCurrent().getArcs(); it.hasNext(); )
			{
				ArcProto ap = it.next();
				if (ap.getFunction() == arcFun)
				{
					arc = ap;
					pin = ap.findPinProto();
					break;
				}
			}

			// find the pure layer node with this function
			pure = getPureLayerNode();
		}

		private void setupViaLayer(String name)
		{
			// find the two layer functions
			ArcProto.Function aFunc1 = ArcProto.Function.UNKNOWN;
			ArcProto.Function aFunc2 = ArcProto.Function.UNKNOWN;
			if (name.length() <= 0)
			{
				aFunc1 = ArcProto.Function.METAL1;
				aFunc2 = ArcProto.Function.METAL2;
			} else if (name.length() <= 1)
			{
				int level = name.charAt(0) - '0';
				if (level == 0) aFunc1 = ArcProto.Function.getPoly(1); else
					aFunc1 = ArcProto.Function.getMetal(level);
				aFunc2 = ArcProto.Function.getMetal(level + 1);
			} else
			{
				int level1 = name.charAt(0) - '0';
				if (level1 == 0) aFunc1 = ArcProto.Function.getPoly(1); else
					aFunc1 = ArcProto.Function.getMetal(level1);
				int level2 = name.charAt(1) - '0';
				aFunc2 = ArcProto.Function.getMetal(level2);
			}

			// find the arcprotos that embody these layers
			for(Iterator<ArcProto> it = Technology.getCurrent().getArcs(); it.hasNext(); )
			{
				ArcProto apTry = it.next();
				if (apTry.getFunction() == aFunc1) viaArc1 = apTry;
				if (apTry.getFunction() == aFunc2) viaArc2 = apTry;
			}
			if (viaArc1 == null || viaArc2 == null) return;

			// find the via that connects these two arcs
			for(Iterator<PrimitiveNode> it = Technology.getCurrent().getNodes(); it.hasNext(); )
			{
				PrimitiveNode np = it.next();
				// must have just one port
				if (np.getNumPorts() != 1) continue;

				// port must connect to both arcs
				PortProto pp = np.getPort(0);
				boolean ap1Found = pp.connectsTo(viaArc1);
				boolean ap2Found = pp.connectsTo(viaArc2);
				if (ap1Found && ap2Found) { pin = np;   break; }
			}

			// find the pure layer node that is the via contact
			if (pin != null)
			{
				// find the layer on this node that is of type "contact"
				PrimitiveNode pNp = (PrimitiveNode)pin;
				Technology.NodeLayer [] nl = pNp.getNodeLayers();
				Layer viaLayer = null;
				for(int i=0; i<nl.length; i++)
				{
					Technology.NodeLayer nLay = nl[i];
					Layer lay = nLay.getLayer();
					Layer.Function fun = lay.getFunction();
					if (fun.isContact()) { viaLayer = lay;   layerFun = fun;   break; }
				}
				if (viaLayer == null) return;
				pure = viaLayer.getPureLayerNode();
			}
		}
	}

}
