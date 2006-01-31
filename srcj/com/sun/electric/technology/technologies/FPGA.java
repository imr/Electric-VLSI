/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FPGA.java
 * FPGA, a customizable technology.
 * Written by Steven M. Rubin
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
package com.sun.electric.technology.technologies;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.dialogs.PromptAt;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is the FPGA Technology.
 */
public class FPGA extends Technology
{
	/** the FPGA Technology object. */	public static final FPGA tech = new FPGA();

	private Layer wireLayer, componentLayer, pipLayer, repeaterLayer;
	private ArcProto wireArc;
	private PrimitiveNode wirePinNode, pipNode, repeaterNode;

	private FPGA()
	{
		super("fpga");
		setTechShortName("FPGA");
		setTechDesc("FPGA Building-Blocks");
		setFactoryScale(2000, true);   // in nanometers: really 2 microns
		setStaticTechnology();
		setNonStandard();
		setNoPrimitiveNodes();

		//**************************************** LAYERS ****************************************

		/** Wire layer */
		wireLayer = Layer.newInstance(this, "Wire",
			new EGraphics(false, false, null, 0, 255,0,0,1,true,
			new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));

		/** Component layer */
		componentLayer = Layer.newInstance(this, "Component",
			new EGraphics(false, false, null, 0, 0,0,0,1,true,
			new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));

		/** Pip layer */
		pipLayer = Layer.newInstance(this, "Pip",
			new EGraphics(false, false, null, 0, 0,255,0,1,true,
			new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));

		/** Repeater layer */
		repeaterLayer = Layer.newInstance(this, "Repeater",
			new EGraphics(false, false, null, 0, 0,0,255,1,true,
			new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));

		// The layer functions
		wireLayer.setFunction(Layer.Function.METAL1);		// wire
		componentLayer.setFunction(Layer.Function.ART);			// component
		pipLayer.setFunction(Layer.Function.ART);			// pip
		repeaterLayer.setFunction(Layer.Function.ART);			// repeater

		//**************************************** ARC ****************************************

		/** wire arc */
		wireArc = ArcProto.newInstance(this, "wire", 0.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(wireLayer, 0, Poly.Type.FILLED)
		});
		wireArc.setFunction(ArcProto.Function.METAL1);
		wireArc.setFactoryFixedAngle(true);
		wireArc.setFactorySlidable(false);
		wireArc.setFactoryAngleIncrement(45);

		//**************************************** NODES ****************************************

		/** wire pin */
		wirePinNode = PrimitiveNode.newInstance("Wire_Pin", this, 1, 1, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(wireLayer, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())})
			});
		wirePinNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, wirePinNode, new ArcProto[] {wireArc}, "wire", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		wirePinNode.setFunction(PrimitiveNode.Function.PIN);
		wirePinNode.setSquare();
		wirePinNode.setWipeOn1or2();

		/** pip */
		pipNode = PrimitiveNode.newInstance("Pip", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pipLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge())})
			});
		pipNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pipNode, new ArcProto[] {wireArc}, "pip", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		pipNode.setFunction(PrimitiveNode.Function.CONNECT);
		pipNode.setSquare();

		/** repeater */
		repeaterNode = PrimitiveNode.newInstance("Repeater", this, 10, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(repeaterLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge())})
			});
		repeaterNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, repeaterNode, new ArcProto[] {wireArc}, "a", 180,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeCenter(), EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
				PrimitivePort.newInstance(this, repeaterNode, new ArcProto[] {wireArc}, "b", 0,45, 1, PortCharacteristic.UNKNOWN,
					EdgeH.makeRightEdge(), EdgeV.makeCenter(), EdgeH.makeRightEdge(), EdgeV.makeCenter())
			});
		repeaterNode.setFunction(PrimitiveNode.Function.CONNECT);

        // Building information for palette
        nodeGroups = new Object[8][1];
        int count = -1;

        nodeGroups[++count][0] = wireArc;
        nodeGroups[++count][0] = "Cell";
        nodeGroups[++count][0] = "Misc.";
        nodeGroups[++count][0] = "Pure";
        nodeGroups[++count][0] = wirePinNode;
        nodeGroups[++count][0] = pipNode;
        nodeGroups[++count][0] = repeaterNode;
	}

	/******************** TREE STRUCTURE FOR ARCHITECTURE FILE ********************/

	/** max depth of FPGA nesting */	private static final int MAXDEPTH = 50;

	private static class LispTree
	{
		private String keyword;
		private int    lineNumber;
		private List<Object> values;

		LispTree()
		{
			values = new ArrayList<Object>();
		}

		void add(Object obj) { values.add(obj); }

		int size() { return values.size(); }

		boolean isLeaf(int i) { return !(values.get(i) instanceof LispTree); }

		boolean isBranch(int i) { return values.get(i) instanceof LispTree; }

		String getLeaf(int i) { return (String)values.get(i); }

		LispTree getBranch(int i) { return (LispTree)values.get(i); }
	};

	private static LispTree [] treeStack = new LispTree[MAXDEPTH];
	private static int         treeDepth;
	private static LispTree    treePosition;

	/******************** ADDITIONAL INFORMATION ABOUT PRIMITIVES ********************/

	/** level of display */						private static final int DISPLAYLEVEL       =  07;
	/**   display no internals */				private static final int NOPRIMDISPLAY      =   0;
	/**   display all internals */				private static final int FULLPRIMDISPLAY    =  01;
	/**   display only active internals */		private static final int ACTIVEPRIMDISPLAY  =  02;
	/** set to display text */					private static final int TEXTDISPLAY        = 010;

	/** set if segment or pip is active */		private static final int ACTIVEPART = 1;
	/** saved area for segment/pip activity */	private static final int ACTIVESAVE = 2;

	private static class FPGAPort
	{
		String             name;
		double             posX, posY;
		int                con;
		PortCharacteristic characteristic;
		PrimitivePort      pp;
	};

	private static class FPGANet
	{
		String     name;
		int        segActive;
		Point2D [] segFrom;
		Point2D [] segTo;
	};

	private static class FPGAPip
	{
		String  name;
		int     pipActive;
		int     con1, con2;
		double  posX, posY;
	};

	private static class FPGANode extends PrimitiveNode
	{
		FPGAPort [] portList;
		FPGANet  [] netList;
		FPGAPip  [] pipList;

		protected FPGANode(String protoName, Technology tech, double defWidth, double defHeight,
			SizeOffset offset, Technology.NodeLayer [] layers)
		{
			super(protoName, tech, defWidth, defHeight, offset, layers);
		}

		int numPorts()
		{
			if (portList == null) return 0;
			return portList.length;
		}

		int numNets()
		{
			if (netList == null) return 0;
			return netList.length;
		}

		int numPips()
		{
			if (pipList == null) return 0;
			return pipList.length;
		}
	};

	/** key of Variable holding active pips. */				private static final Variable.Key ACTIVEPIPS_KEY = Variable.newKey("FPGA_activepips");
	/** key of Variable holding active repeaters. */		private static final Variable.Key ACTIVEREPEATERS_KEY = Variable.newKey("FPGA_activerepeaters");
//	/** key of Variable holding cache of pips on node. */	private static final Variable.Key NODEPIPCACHE_KEY = Variable.newKey("FPGA_nodepipcache");
//	/** key of Variable holding cache of active arcs. */	private static final Variable.Key ARCACTIVECACHE_KEY = Variable.newKey("FPGA_arcactivecache");
	/** name of current repeater for activity examining */	private static String         repeaterName;
	/** nonzero if current repeater is found to be active */private static boolean        repeaterActive;
	/** what is being displayed */							private static int            internalDisplay = ACTIVEPRIMDISPLAY | TEXTDISPLAY;
	/** whether the technology has been read */				private static boolean        defined = false;

	private static Technology.NodeLayer[] NULLNODELAYER = new Technology.NodeLayer[0];
	private static Poly[] NULLPOLYS = new Poly[0];

	/**
	 * Method to return a list of Polys that describe a given NodeInst.
	 * This method overrides the general one in the Technology object
	 * because of the unusual primitives in this Technology.
	 * @param ni the NodeInst to describe.
	 * @param wnd the window in which this node will be drawn.
	 * @param context the VarContext to this node in the hierarchy.
	 * @param electrical true to get the "electrical" layers.
	 * This makes no sense for Schematics primitives.
	 * @param reasonable true to get only a minimal set of contact cuts in large contacts.
	 * This makes no sense for Schematics primitives.
	 * @param primLayers an array of NodeLayer objects to convert to Poly objects.
	 * @param layerOverride the layer to use for all generated polygons (if not null).
	 * @return an array of Poly objects.
	 */
	public Poly [] getShapeOfNode(NodeInst ni, EditWindow0 wnd, VarContext context, boolean electrical, boolean reasonable, Technology.NodeLayer [] primLayers, Layer layerOverride)
	{
		if (ni.isCellInstance()) return null;

		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		if (np == tech.wirePinNode)
		{
			if (ni.pinUseCount()) primLayers = NULLNODELAYER;
		} else if (np == tech.repeaterNode)
		{
			if ((internalDisplay&DISPLAYLEVEL) == ACTIVEPRIMDISPLAY)
			{
				if (!repeaterActive(ni)) primLayers = NULLNODELAYER;
			}
		} else if (np instanceof FPGANode)
		{
			// dynamic primitive
			FPGANode fn = (FPGANode)np;

			// hard reset of all segment and pip activity
			int numPips = 0, numSegs = 0;
			for(int i=0; i<fn.numNets(); i++) fn.netList[i].segActive = 0;
			for(int i=0; i<fn.numPips(); i++) fn.pipList[i].pipActive = 0;

			switch (internalDisplay & DISPLAYLEVEL)
			{
				case NOPRIMDISPLAY:
					break;
				case ACTIVEPRIMDISPLAY:
					// count number of active nets and pips

					// determine the active segments and pips
					reEvaluatePips(ni, fn, context);

					// save the activity bits
					for(int i=0; i<fn.numNets(); i++)
						if ((fn.netList[i].segActive&ACTIVEPART) != 0)
							fn.netList[i].segActive |= ACTIVESAVE;
					for(int i=0; i<fn.numPips(); i++)
						if ((fn.pipList[i].pipActive&ACTIVEPART) != 0)
							fn.pipList[i].pipActive |= ACTIVESAVE;

					// propagate inactive segments to others that may be active
					if (context != null && context.getNodable() != null)
					{
//						NodeInst oNi = (NodeInst)context.getNodable();
						VarContext higher = context.pop();
						for(int i=0; i<fn.numNets(); i++)
						{
							if ((fn.netList[i].segActive&ACTIVESAVE) != 0) continue;
							boolean found = false;
							for(int j=0; j<fn.numPorts(); j++)
							{
								if (fn.portList[j].con != i) continue;
								for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
								{
									Connection con = (Connection)it.next();
									if (con.getPortInst().getPortProto() != fn.portList[j].pp) continue;
									ArcInst ai = con.getArc();
                                    int otherEnd = 1 - con.getEndIndex();
//									int otherEnd = 0;
//									if (ai.getConnection(0) == con) otherEnd = 1;
									if (arcEndActive(ai, otherEnd, higher)) { found = true;   break; }
								}
								if (found) break;
							}
							if (found) fn.netList[i].segActive |= ACTIVESAVE;
						}
					}

					// add up the active segments
					for(int i=0; i<fn.numPips(); i++)
						if ((fn.pipList[i].pipActive&ACTIVESAVE) != 0) numPips++;
					for(int i=0; i<fn.numNets(); i++)
						if ((fn.netList[i].segActive&ACTIVESAVE) != 0)
							numSegs += fn.netList[i].segFrom.length;
					break;
				case FULLPRIMDISPLAY:
					for(int i=0; i<fn.numNets(); i++)
					{
						fn.netList[i].segActive |= ACTIVESAVE;
						numSegs += fn.netList[i].segFrom.length;
					}
					break;
			}
			int total = 1 + numPips + numSegs;
			if ((internalDisplay&TEXTDISPLAY) != 0)
			{
				total++;
				if (wnd != null) total += ni.numDisplayableVariables(true);
			}

			// construct the polygon array
			Poly [] polys = new Poly[total];

			// add the basic box layer
 			double xCenter = ni.getTrueCenterX();
 			double yCenter = ni.getTrueCenterY();
			double xSize = ni.getXSize();
			double ySize = ni.getYSize();
			Point2D [] pointList = Poly.makePoints(xCenter - xSize/2, xCenter + xSize/2, yCenter - ySize/2, yCenter + ySize/2);
			polys[0] = new Poly(pointList);
			polys[0].setStyle(fn.getLayers()[0].getStyle());
			polys[0].setLayer(tech.componentLayer);
			int fillPos = 1;

			// add in the pips
			for(int i=0; i<fn.numPips(); i++)
			{
				if ((fn.pipList[i].pipActive&ACTIVESAVE) == 0) continue;
				double x = xCenter + fn.pipList[i].posX;
				double y = yCenter + fn.pipList[i].posY;
				polys[fillPos] = new Poly(Poly.makePoints(x-1, x+1, y-1, y+1));
				polys[fillPos].setStyle(Poly.Type.FILLED);
				polys[fillPos].setLayer(tech.pipLayer);
				fillPos++;
			}

			// add in the network segments
			for(int i=0; i<fn.numNets(); i++)
			{
				if ((fn.netList[i].segActive&ACTIVESAVE) == 0) continue;
				for(int j=0; j<fn.netList[i].segFrom.length; j++)
				{
					double fX = xCenter + fn.netList[i].segFrom[j].getX();
					double fY = yCenter + fn.netList[i].segFrom[j].getY();
					double tX = xCenter + fn.netList[i].segTo[j].getX();
					double tY = yCenter + fn.netList[i].segTo[j].getY();
					Point2D [] line = new Point2D[2];
					line[0] = new Point2D.Double(fX, fY);
					line[1] = new Point2D.Double(tX, tY);
					polys[fillPos] = new Poly(line);
					polys[fillPos].setStyle(Poly.Type.OPENED);
					polys[fillPos].setLayer(tech.wireLayer);
					fillPos++;
				}
			}

			// add the primitive name if requested
			if ((internalDisplay&TEXTDISPLAY) != 0)
			{
				polys[fillPos] = new Poly(pointList);
				polys[fillPos].setStyle(Poly.Type.TEXTBOX);
				polys[fillPos].setLayer(tech.componentLayer);
				polys[fillPos].setString(fn.getName());
				TextDescriptor td = TextDescriptor.EMPTY.withRelSize(3);
				polys[fillPos].setTextDescriptor(td);
				fillPos++;

				// add in displayable variables
				if (wnd != null)
				{
					Rectangle2D rect = ni.getUntransformedBounds();
					ni.addDisplayableVariables(rect, polys, fillPos, wnd, true);
				}
			}
			return polys;
		}

		return super.getShapeOfNode(ni, wnd, context, electrical, reasonable, primLayers, layerOverride);
	}

	/**
	 * Method to return a list of Polys that describe a given ArcInst.
	 * This method overrides the general one in the Technology object
	 * because of the unusual primitives in this Technology.
	 * @param ai the ArcInst to describe.
	 * @param wnd the window in which this arc will be drawn.
	 * @param onlyTheseLayers to filter the only required layers
	 * @return an array of Poly objects.
	 */
	public Poly [] getShapeOfArc(ArcInst ai, EditWindow0 wnd, Layer layerOverride, List<Layer.Function> onlyTheseLayers)
	{
		boolean active = true;
		if ((internalDisplay&DISPLAYLEVEL) == NOPRIMDISPLAY ||
			(internalDisplay&DISPLAYLEVEL) == ACTIVEPRIMDISPLAY)
		{
			VarContext context = VarContext.globalContext;
			if (wnd != null) context = wnd.getVarContext();
			if (!arcActive(ai, context)) active = false;
		}
		if (!active) return NULLPOLYS;

		int numDisplayable = 0;
		if (wnd != null) numDisplayable = ai.numDisplayableVariables(true);
		Poly [] polys = new Poly[numDisplayable + 1];
		int polyNum = 0;

		// draw the arc
		polys[polyNum] = ai.makePoly(ai.getWidth(), Poly.Type.FILLED);
		if (polys[polyNum] == null) return null;
		polys[polyNum].setLayer(tech.wireLayer);
		polyNum++;

		// add in the displayable variables
		if (numDisplayable > 0)
		{
			Rectangle2D rect = ai.getBounds();
			ai.addDisplayableVariables(rect, polys, polyNum, wnd, true);
		}
		return polys;
	}

	/******************** TECHNOLOGY INTERFACE SUPPORT ********************/

	private boolean arcEndActive(ArcInst ai, int j, VarContext curContext)
	{
		// examine end
		PortInst pi = ai.getPortInst(j);
		NodeInst ni = pi.getNodeInst();
		PortProto pp = pi.getPortProto();
		NodeProto np = ni.getProto();
		if (ni.isCellInstance())
		{
			// follow down into cell
			VarContext down = curContext.push(ni);
			NodeInst subni = ((Export)pp).getOriginalPort().getNodeInst();
			for(Iterator<Connection> it = subni.getConnections(); it.hasNext(); )
			{
				Connection nextCon = (Connection)it.next();
				ArcInst oAi = nextCon.getArc();
				int newEnd = 0;
				if (oAi.getPortInst(0).getNodeInst() == subni) newEnd = 1;
				if (arcEndActive(oAi, newEnd, down)) return true;
			}
			return false;
		} else
		{
			// primitive: see if it is one of ours
			if (np instanceof FPGANode)
			{
				FPGANode fn = (FPGANode)np;
//				VarContext down = curContext.push(ni);
				reEvaluatePips(ni, fn, curContext);
				for(int i = 0; i < fn.numPorts(); i++)
				{
					if (fn.portList[i].pp != pp) continue;
					int index = fn.portList[i].con;
					if (index >= 0 && fn.netList != null)
					{
						if ((fn.netList[index].segActive&ACTIVEPART) != 0) return true;
					}
					break;
				}
			}
		}

		// propagate
		Cell parent = ai.getParent();
		if (parent != null)
		{
			Netlist nl = parent.acquireUserNetlist();
			Network net = nl.getNetwork(ni, pp, 0);
			if (net != null)
			{
				for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
				{
					Connection nextCon = (Connection)it.next();
					ArcInst oAi = nextCon.getArc();
					if (oAi == ai) continue;
					Network oNet = nl.getNetwork(oAi, 0);
					if (oNet != net) continue;
                    int newEnd = 1 - nextCon.getEndIndex();
//					int newEnd = 0;
//					if (oAi.getConnection(0) == nextCon) newEnd = 1;
					if (arcEndActive(oAi, newEnd, curContext)) return true;
				}

				VarContext higher = curContext.pop();
				if (higher != null && higher.getNodable() != null)
				{
					NodeInst oNi = (NodeInst)higher.getNodable();
					for (Iterator<Export> it = ni.getExports(); it.hasNext(); )
					{
						Export opp = (Export)it.next();
						Network oNet = nl.getNetwork(opp, 0);
						if (oNet != net) continue;

						for(Iterator<Connection> uIt = oNi.getConnections(); uIt.hasNext(); )
						{
							Connection nextCon = (Connection)uIt.next();
							ArcInst oAi = nextCon.getArc();
							if (nextCon.getPortInst().getPortProto() != opp) continue;
                            int newEnd = 1 - nextCon.getEndIndex();
//							int newEnd = 0;
//							if (oAi.getConnection(0) == nextCon) newEnd = 1;
							if (arcEndActive(oAi, newEnd, higher)) return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean arcActive(ArcInst ai, VarContext curContext)
	{
		// see if there is a cache on the arc
//		gettraversalpath(ai.parent, NOWINDOWPART, &nilist, &indexlist, &depth, 0);
//		var = getvalkey((INTBIG)ai, VARCINST, VCHAR|VISARRAY, ARCACTIVECACHE_KEY);
//		if (var != NOVARIABLE)
//		{
//			ptr = (UCHAR1 *)var.addr;
//			cachedepth = ((INTBIG *)ptr)[0];   ptr += SIZEOFINTBIG;
//			if (cachedepth == depth)
//			{
//				for(i=0; i<cachedepth; i++)
//				{
//					oni = ((NODEINST **)ptr)[0];   ptr += (sizeof (NODEINST *));
//					if (oni != nilist[i]) break;
//				}
//				if (i >= cachedepth)
//				{
//					// cache applies to this arc: get active factor
//					if (((INTSML *)ptr)[0] == 0) return false;
//					return true;
//				}
//			}
//		}

		// compute arc activity
		boolean value = false;
		if (arcEndActive(ai, 0, curContext)) value = true; else
			if (arcEndActive(ai, 1, curContext)) value = true;

		// store the cache
//		size = depth * (sizeof (NODEINST *)) + SIZEOFINTBIG + SIZEOFINTSML;
//		if (size > arcBufSize)
//		{
//			if (arcBufSize > 0) efree((CHAR *)arcBuf);
//			arcBufSize = 0;
//			arcBuf = (UCHAR1 *)emalloc(size, tech.cluster);
//			if (arcBuf == 0) return(value);
//			arcBufSize = size;
//		}
//		ptr = arcBuf;
//		((INTBIG *)ptr)[0] = depth;   ptr += SIZEOFINTBIG;
//		for(i=0; i<depth; i++)
//		{
//			((NODEINST **)ptr)[0] = nilist[i];   ptr += (sizeof (NODEINST *));
//		}
//		((INTSML *)ptr)[0] = value ? 1 : 0;
//		nextchangequiet();
//		setvalkey((INTBIG)ai, VARCINST, ARCACTIVECACHE_KEY, (INTBIG)arcBuf,
//			VCHAR|VISARRAY|(size<<VLENGTHSH)|VDONTSAVE);
		return value;
	}

	/**
	 * Method to reevaluate primitive node "ni" (which is associated with internal
	 * structure "fn").  Finds programming of pips and sets pip and net activity.
	 */
	private void reEvaluatePips(NodeInst ni, FPGANode fn, VarContext context)
	{
		// primitives with no pips or nets need no evaluation
		if (fn.numNets() == 0 && fn.numPips() == 0) return;

		// see if there is a cache on the node
//		gettraversalpath(ni.parent, NOWINDOWPART, &nilist, &indexlist, &depth, 0);
//		var = getvalkey((INTBIG)ni, VNODEINST, VCHAR|VISARRAY, NODEPIPCACHE_KEY);
//		if (var != NOVARIABLE)
//		{
//			ptr = (UCHAR1 *)var.addr;
//			cachedepth = ((INTBIG *)ptr)[0];   ptr += SIZEOFINTBIG;
//			if (cachedepth == depth)
//			{
//				for(i=0; i<cachedepth; i++)
//				{
//					oni = ((NODEINST **)ptr)[0];   ptr += (sizeof (NODEINST *));
//					if (oni != nilist[i]) break;
//				}
//				if (i >= cachedepth)
//				{
//					// cache applies to this node: get values
//					for(i=0; i<fn.numNets(); i++)
//					{
//						value = ((INTSML *)ptr)[0];   ptr += SIZEOFINTSML;
//						if (value != 0) fn.netList[i].segActive |= ACTIVEPART; else
//							fn.netList[i].segActive &= ~ACTIVEPART;
//					}
//					for(i=0; i<fn.numPips(); i++)
//					{
//						value = ((INTSML *)ptr)[0];   ptr += SIZEOFINTSML;
//						if (value != 0) fn.pipList[i].pipActive |= ACTIVEPART; else
//							fn.pipList[i].pipActive &= ~ACTIVEPART;
//					}
//					return;
//				}
//			}
//		}

		// reevaluate: presume all nets and pips are inactive
		for(int i=0; i<fn.numNets(); i++) fn.netList[i].segActive &= ~ACTIVEPART;
		for(int i=0; i<fn.numPips(); i++) fn.pipList[i].pipActive &= ~ACTIVEPART;

		// look for pip programming
		findVariableObjects(fn, ni, ACTIVEPIPS_KEY, true, context);

		// set nets active where they touch active pips
		for(int i=0; i<fn.numPips(); i++)
		{
			FPGAPip fPip = fn.pipList[i];
			if ((fPip.pipActive&ACTIVEPART) == 0) continue;
			if (fPip.con1 > 0) fn.netList[fPip.con1].segActive |= ACTIVEPART;
			if (fPip.con2 > 0) fn.netList[fPip.con2].segActive |= ACTIVEPART;
		}

//		// store the cache
//		size = depth * (sizeof (NODEINST *)) + SIZEOFINTBIG + fn.numNets() * SIZEOFINTSML +
//			fn.numPips() * SIZEOFINTSML;
//		if (size > pipBufSize)
//		{
//			if (pipBufSize > 0) efree((CHAR *)pipBuf);
//			pipBufSize = 0;
//			pipBuf = (UCHAR1 *)emalloc(size, tech.cluster);
//			if (pipBuf == 0) return;
//			pipBufSize = size;
//		}
//		ptr = pipBuf;
//		((INTBIG *)ptr)[0] = depth;   ptr += SIZEOFINTBIG;
//		for(i=0; i<depth; i++)
//		{
//			((NODEINST **)ptr)[0] = nilist[i];   ptr += (sizeof (NODEINST *));
//		}
//		for(i=0; i<fn.numNets(); i++)
//		{
//			if ((fn.netList[i].segActive&ACTIVEPART) != 0) ((INTSML *)ptr)[0] = 1; else
//				((INTSML *)ptr)[0] = 0;
//			ptr += SIZEOFINTSML;
//		}
//		for(i=0; i<fn.numPips(); i++)
//		{
//			if ((fn.pipList[i].pipActive&ACTIVEPART) != 0) ((INTSML *)ptr)[0] = 1; else
//				((INTSML *)ptr)[0] = 0;
//			ptr += SIZEOFINTSML;
//		}
//		nextchangequiet();
//		setvalkey((INTBIG)ni, VNODEINST, NODEPIPCACHE_KEY, (INTBIG)pipBuf,
//			VCHAR|VISARRAY|(size<<VLENGTHSH)|VDONTSAVE);
	}

	/**
	 * Method to examine primitive node "ni" and return true if the repeater is active.
	 */
	private boolean repeaterActive(NodeInst ni)
	{
		repeaterName = ni.getName();
		repeaterActive = false;
		findVariableObjects(null, ni, ACTIVEREPEATERS_KEY, false, null);
		return repeaterActive;
	}

	Nodable [] path = new Nodable[100];

	private void findVariableObjects(FPGANode fn, NodeInst ni, Variable.Key varKey, boolean setPips, VarContext context)
	{
		// search hierarchical path
		int depth = 0;
		path[depth++] = ni;
		while (context != null)
		{
			Nodable niClimb = context.getNodable();
            if (niClimb == null) break;
			path[depth++] = niClimb;
			context = context.pop();
		}

		// look for programming variables on the nodes
		for(int c=0; c<depth; c++)
		{
			Nodable niClimb = path[c];
			Variable var = niClimb.getVar(varKey);
			if (var == null) continue;

			// found pip settings: evaluate them
			String pt = (String)var.getObject();
			String [] pipNames = pt.split(" ");
			for(int i=0; i<pipNames.length; i++)
			{
				String start = pipNames[i];
				if (start.length() == 0) continue;

				// find pip name in "start"
				String [] pipParts = start.split("\\.");
				if (pipParts.length == 0 || pipParts.length > depth) continue;
				boolean pathGood = true;
//				VarContext climb = context;
				for(int j=0; j<pipParts.length-1; j++)
				{
					if (!pipParts[j].equalsIgnoreCase(path[depth-2-j].getName()))
					{
						pathGood = false;
						break;
					}
				}
				if (pathGood)
				{
					String lastPart = pipParts[pipParts.length-1];
					if (setPips)
					{
						for(int k=0; k<fn.numPips(); k++)
							if (fn.pipList[k].name.equalsIgnoreCase(lastPart))
						{
							fn.pipList[k].pipActive |= ACTIVEPART;
						}
					} else
					{
						if (repeaterName.equalsIgnoreCase(lastPart)) repeaterActive = true;
					}
				}
			}
			break;
		}
	}

//	/* working memory for "arcActive()" */
//	static INTBIG         arcBufSize = 0;
//	static UCHAR1        *arcBuf;
//
//	/* working memory for "reEvaluatePips()" */
//	static INTBIG         pipBufSize = 0;
//	static UCHAR1        *pipBuf;

//	void setMode(INTBIG count, CHAR *par[])
//	{
//		l = estrlen(pp = par[0]);
//		if (namesamen(pp, x_("wipe-cache"), l) == 0)
//		{
//			clearCache(null);
//			return;
//		}
//		if (namesamen(pp, x_("clear-node-cache"), l) == 0)
//		{
//			ni = (NODEINST *)us_getobject(VNODEINST, FALSE);
//			if (ni == null) return;
//			clearCache(ni);
//			return;
//		}
//	}

//	/*
//	 * Routine to clear the cache of arc activity in the current cell.  If "ni" is NONODEINST,
//	 * clear all node caches as well, otherwise only clear the node cache on "ni".
//	 */
//	void clearCache(NodeInst ni)
//	{
//		REGISTER VARIABLE *var;
//		REGISTER ARCINST *ai;
//		REGISTER NODEPROTO *np;
//
//		np = getcurcell();
//		if (np == null)
//		{
//			ttyputerr(_("Must edit a cell to clear its cache"));
//			return;
//		}
//		for(ai = np.firstarcinst; ai != null; ai = ai.nextarcinst)
//		{
//			var = getvalkey((INTBIG)ai, VARCINST, VCHAR|VISARRAY, ARCACTIVECACHE_KEY);
//			if (var != NOVARIABLE)
//				(void)delvalkey((INTBIG)ai, VARCINST, ARCACTIVECACHE_KEY);
//		}
//		if (ni != null)
//		{
//			var = getvalkey((INTBIG)ni, VNODEINST, VCHAR|VISARRAY, NODEPIPCACHE_KEY);
//			if (var != NOVARIABLE)
//				(void)delvalkey((INTBIG)ni, VNODEINST, NODEPIPCACHE_KEY);
//		}
//	}

	/******************** TECHNOLOGY CONTROL ********************/

	/**
	 * Method to read an architecture file and customize the FPGA technology.
	 * Prompts for a file and reads it.
	 * @param placeAndWire true to build the primitives and structures; false to simply build the primitives.
	 */
	public static void readArchitectureFile(boolean placeAndWire)
	{
		if (defined)
		{
			System.out.println("This technology already has primitives defined");
			return;
		}

		// get architecture file
		String fileName = OpenFile.chooseInputFile(FileType.FPGA, null);
		if (fileName == null) return;

		// turn the tree into primitives
		new BuildTechnology(fileName, placeAndWire);
	}

	/**
	 * Method to set the wire display level.
	 * @param level 0 to show no wires; 1 to show active wires; 2 to show all wires.
	 */
	public static void setWireDisplay(int level)
	{
		switch (level)
		{
			case 0:		// no wires
				internalDisplay = (internalDisplay & ~DISPLAYLEVEL) | NOPRIMDISPLAY;
				break;
			case 1:		// active wires
				internalDisplay = (internalDisplay & ~DISPLAYLEVEL) | ACTIVEPRIMDISPLAY;
				break;
			case 2:		// all wires
				internalDisplay = (internalDisplay & ~DISPLAYLEVEL) | FULLPRIMDISPLAY;
				break;
		}
		UserInterface ui = Job.getUserInterface();
		ui.repaintAllEditWindows();
	}

	/**
	 * Method to set the text display level.
	 * @param show true to see text, false to hide text.
	 */
	public static void setTextDisplay(boolean show)
	{
		if (show) internalDisplay |= TEXTDISPLAY; else
			internalDisplay &= ~TEXTDISPLAY;
		UserInterface ui = Job.getUserInterface();
		ui.repaintAllEditWindows();
	}

	/**
	 * Method to program the currently selected PIPs.
	 */
	public static void programPips()
	{
		UserInterface ui = Job.getUserInterface();
		EditWindow_ wnd = ui.getCurrentEditWindow_();
		if (wnd == null) return;
		ElectricObject eObj = wnd.getOneElectricObject(NodeInst.class);
		if (eObj == null) return;
		NodeInst ni = (NodeInst)eObj;
		String pips = "";
		Variable var = ni.getVar(ACTIVEPIPS_KEY);
		if (var != null) pips = (String)var.getObject();

		String newPips = PromptAt.showPromptAt(wnd, ni, "Edit Pips",
			"Pips on this node:", pips);
		if (newPips == null) return;
		new SetPips(ni, newPips);
	}

	/**
	 * This class sets pip programming on a node.
	 */
	private static class SetPips extends Job
	{
		private NodeInst ni;
		private String newPips;

		private SetPips(NodeInst ni, String newPips)
		{
			super("Program Pips", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.newPips = newPips;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			ni.newVar(ACTIVEPIPS_KEY, newPips);
			return true;
		}

        public void terminateOK()
        {
			UserInterface ui = Job.getUserInterface();
			ui.repaintAllEditWindows();
        }
	}

	/**
	 * This class implement the command to build an FPGA technology.
	 */
	private static class BuildTechnology extends Job
	{
		private String fileName;
		private boolean placeAndWire;
		private Cell topCell;

		private BuildTechnology(String fileName, boolean placeAndWire)
		{
			super("Build FPGA Technology", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.fileName = fileName;
			this.placeAndWire = placeAndWire;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// read the file
			LispTree lt = readFile(fileName);
			if (lt == null)
				throw new JobException("Error reading file");

			int total = makePrimitives(lt);
			System.out.println("Created " + total + " primitives");

			// setup the generic technology to handle all connections
			Generic.tech.makeUnivList();

			// place and wire the primitives
			if (placeAndWire)
			{
				topCell = placePrimitives(lt);
				fieldVariableChanged("topCell");
			}
			return true;
		}

        public void terminateOK()
        {
			if (topCell != null)
			{
				// display top cell
				UserInterface ui = Job.getUserInterface();
				ui.displayCell(topCell);
			}
        }
	}

	/**
	 * Method to read the FPGA file in "f" and create a LISPTREE structure which is returned.
	 * Returns zero on error.
	 */
	private static LispTree readFile(String fileName)
	{
		// make the tree top
		LispTree treeTop = new LispTree();
		treeTop.keyword = "TOP";

		// initialize current position and stack
		treePosition = treeTop;
		treeDepth = 0;

		URL url = TextUtils.makeURLToFile(fileName);
		try
		{
			URLConnection urlCon = url.openConnection();
			InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
			LineNumberReader lnr = new LineNumberReader(is);

			// read the file
			for(;;)
			{
				// get the next line of text
				String line = lnr.readLine();
				if (line == null) break;

				// stop now if it is a comment
				line = line.trim();
				if (line.length() == 0) continue;
				if (line.charAt(0) == '#') continue;

				// keep parsing it
				int pt = 0;
				for(;;)
				{
					// skip spaces
					while (pt < line.length() && Character.isWhitespace(line.charAt(pt))) pt++;
					if (pt >= line.length()) break;

					// check for special characters
					char chr = line.charAt(pt);
					if (chr == ')')
					{
						if (pushKeyword(line.substring(pt, pt+1), lnr)) return null;
						pt++;
						continue;
					}

					// gather a keyword
					int ptEnd = pt;
					for(;;)
					{
						if (ptEnd >= line.length()) break;
						char chEnd = line.charAt(ptEnd);
						if (chEnd == ')' || Character.isWhitespace(chEnd)) break;
						if (chEnd == '"')
						{
							ptEnd++;
							for(;;)
							{
								if (ptEnd >= line.length() || line.charAt(ptEnd) == '"') break;
								ptEnd++;
							}
							if (ptEnd < line.length()) ptEnd++;
							break;
						}
						ptEnd++;
					}
					if (pushKeyword(line.substring(pt, ptEnd), lnr)) return null;
					pt = ptEnd;
				}
			}
			lnr.close();
			System.out.println(fileName + " read");
		} catch (IOException e)
		{
			System.out.println("Error reading " + fileName);
			return null;
		}

		if (treeDepth != 0)
		{
			System.out.println("Not enough close parenthesis in file");
			return null;
		}
		return treeTop;
	}

	/**
	 * Method to add the next keyword "keyword" to the lisp tree in the globals.
	 * Returns true on error.
	 */
	private static boolean pushKeyword(String keyword, LineNumberReader lnr)
	{
		if (keyword.startsWith("("))
		{
			if (treeDepth >= MAXDEPTH)
			{
				System.out.println("Nesting too deep (more than " + MAXDEPTH + ")");
				return true;
			}

			// create a new tree branch
			LispTree newTree = new LispTree();
			newTree.lineNumber = lnr.getLineNumber();

			// add branch to previous branch
			treePosition.add(newTree);

			// add keyword
			int pt = 1;
			while (pt < keyword.length() && Character.isWhitespace(keyword.charAt(pt))) pt++;
			newTree.keyword = keyword.substring(pt);

			// push tree onto stack
			treeStack[treeDepth] = treePosition;
			treeDepth++;
			treePosition = newTree;
			return false;
		}

		if (keyword.equals(")"))
		{
			// pop tree stack
			if (treeDepth <= 0)
			{
				System.out.println("Too many close parenthesis");
				return true;
			}
			treeDepth--;
			treePosition = treeStack[treeDepth];
			return false;
		}

		// just add the atomic keyword
		if (keyword.startsWith("\"") && keyword.endsWith("\""))
			keyword = keyword.substring(1, keyword.length()-1);
		treePosition.add(keyword);
		return false;
	}

	/******************** ARCHITECTURE PARSING: PRIMITIVES ********************/

	/**
	 * Method to parse the entire tree and create primitives.
	 * Returns the number of primitives made.
	 */
	private static int makePrimitives(LispTree lt)
	{
		// look through top level for the "primdef"s
		int total = 0;
		for(int i=0; i<lt.size(); i++)
		{
			if (lt.isLeaf(i)) continue;
			LispTree subLT = lt.getBranch(i);
			if (!subLT.keyword.equalsIgnoreCase("primdef")) continue;

			// create the primitive
			if (makePrimitive(subLT)) return(0);
			total++;
		}
		return total;
	}

	/**
	 * Method to create a primitive from a subtree "lt".
	 * Tree has "(primdef...)" structure.
	 */
	private static boolean makePrimitive(LispTree lt)
	{
		// find all of the pieces of this primitive
		LispTree ltAttribute = null, ltNets = null, ltPorts = null, ltComponents = null;
		String primName = null;
		String primSizeX = null;
		String primSizeY = null;
		for(int i=0; i<lt.size(); i++)
		{
			if (lt.isLeaf(i)) continue;
			LispTree scanLT = lt.getBranch(i);
			if (scanLT.keyword.equalsIgnoreCase("attributes"))
			{
				if (ltAttribute != null)
				{
					System.out.println("Multiple 'attributes' sections for a primitive (line " + scanLT.lineNumber + ")");
					return true;
				}
				for(int j=0; j<scanLT.size(); j++)
				{
					if (scanLT.isLeaf(j)) continue;
					LispTree subLT = scanLT.getBranch(j);
					if (subLT.keyword.equalsIgnoreCase("name"))
					{
						if (subLT.size() != 1 || subLT.isBranch(0))
						{
							System.out.println("Primitive 'name' attribute should take a single atomic parameter (line " + subLT.lineNumber + ")");
							return true;
						}
						primName = subLT.getLeaf(0);
						continue;
					}
					if (subLT.keyword.equalsIgnoreCase("size"))
					{
						if (subLT.size() != 2 || subLT.isBranch(0) || subLT.isBranch(1))
						{
							System.out.println("Primitive 'size' attribute should take two atomic parameters (line " + subLT.lineNumber + ")");
							return true;
						}
						primSizeX = subLT.getLeaf(0);
						primSizeY = subLT.getLeaf(1);
						continue;
					}
				}
				ltAttribute = scanLT;
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("nets"))
			{
				if (ltNets != null)
				{
					System.out.println("Multiple 'nets' sections for a primitive (line " + scanLT.lineNumber + ")");
					return true;
				}
				ltNets = scanLT;
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("ports"))
			{
				if (ltPorts != null)
				{
					System.out.println("Multiple 'ports' sections for a primitive (line " + scanLT.lineNumber + ")");
					return true;
				}
				ltPorts = scanLT;
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("components"))
			{
				if (ltComponents != null)
				{
					System.out.println("Multiple 'components' sections for a primitive (line " + scanLT.lineNumber + ")");
					return true;
				}
				ltComponents = scanLT;
				continue;
			}
		}

		// make sure a name and size were given
		if (primName == null)
		{
			System.out.println("Missing 'name' attribute in primitive definition (line " + lt.lineNumber + ")");
			return true;
		}
		if (primSizeX == null || primSizeY == null)
		{
			System.out.println("Missing 'size' attribute in primitive definition (line " + lt.lineNumber + ")");
			return true;
		}

		// make the primitive
		double sizeX = TextUtils.atof(primSizeX);
		double sizeY = TextUtils.atof(primSizeY);
		FPGANode primNP = new FPGANode(primName, tech, sizeX, sizeY, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(tech.componentLayer, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, new Technology.TechPoint[] {
					new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
				})
			});
		primNP.setLockedPrim();
		defined = true;

		// get ports
		if (ltPorts != null)
		{
			// count ports
			int portCount = 0;
			for(int j=0; j<ltPorts.size(); j++)
			{
				if (ltPorts.isLeaf(j)) continue;
				LispTree scanLT = ltPorts.getBranch(j);
				if (scanLT.keyword.equalsIgnoreCase("port")) portCount++;
			}

			// create the ports
			primNP.portList = new FPGAPort[portCount];
			int portNumber = 0;
			for(int j=0; j<ltPorts.size(); j++)
			{
				if (ltPorts.isLeaf(j)) continue;
				LispTree scanLT = ltPorts.getBranch(j);
				if (scanLT.keyword.equalsIgnoreCase("port"))
				{
					FPGAPort fp = new FPGAPort();
					primNP.portList[portNumber] = fp;
					if (makePrimPort(primNP, scanLT, fp, portNumber)) return true;
					for(int k=0; k<portNumber; k++)
					{
						if (primNP.portList[k].name.equalsIgnoreCase(fp.name))
						{
							System.out.println("Duplicate port name: " + fp.name + " (line " + scanLT.lineNumber + ")");
							return true;
						}
					}
					portNumber++;
				}
			}
		}

		// get nets
		if (ltNets != null)
		{
			// count the nets
			int netCount = 0;
			for(int j=0; j<ltNets.size(); j++)
			{
				if (ltNets.isLeaf(j)) continue;
				LispTree scanLT = ltNets.getBranch(j);
				if (scanLT.keyword.equalsIgnoreCase("net")) netCount++;
			}

			// create the nets
			primNP.netList = new FPGANet[netCount];
			int index = 0;
			for(int j=0; j<ltNets.size(); j++)
			{
				if (ltNets.isLeaf(j)) continue;
				LispTree scanLT = ltNets.getBranch(j);
				if (scanLT.keyword.equalsIgnoreCase("net"))
				{
					primNP.netList[index] = new FPGANet();
					if (makePrimNet(primNP, scanLT, primNP, primNP.netList[index])) return true;
					index++;
				}
			}
		}

		// associate nets and ports
		for(int k=0; k<primNP.numPorts(); k++)
		{
			FPGAPort fp = primNP.portList[k];
			for(int i=0; i<primNP.numNets(); i++)
			{
				boolean found = false;
				for(int j=0; j<primNP.netList[i].segFrom.length; j++)
				{
					if ((primNP.netList[i].segFrom[j].getX() == fp.posX && primNP.netList[i].segFrom[j].getY() == fp.posY) ||
						(primNP.netList[i].segTo[j].getX() == fp.posX && primNP.netList[i].segTo[j].getY() == fp.posY))
					{
						fp.con = i;
						found = true;
						break;
					}
				}
				if (found) break;
			}
		}

		// create the ports on the primitive
		PrimitivePort [] ports = new PrimitivePort[primNP.numPorts()];
		for(int i=0; i<primNP.numPorts(); i++)
		{
			FPGAPort fp = primNP.portList[i];
			fp.pp = PrimitivePort.newInstance(tech, primNP, new ArcProto [] {tech.wireArc}, fp.name, 0,180, fp.con,
				fp.characteristic,EdgeH.fromCenter(fp.posX), EdgeV.fromCenter(fp.posY), EdgeH.fromCenter(fp.posX),
				EdgeV.fromCenter(fp.posY));
			ports[i] = fp.pp;
		}
		primNP.addPrimitivePorts(ports);

		// get pips
		if (ltComponents != null)
		{
			// count the pips
			int pipCount = 0;
			for(int j=0; j<ltComponents.size(); j++)
			{
				if (ltComponents.isLeaf(j)) continue;
				LispTree scanLT = ltComponents.getBranch(j);
				if (scanLT.keyword.equalsIgnoreCase("pip")) pipCount++;
			}

			// create the pips
			primNP.pipList = new FPGAPip[pipCount];
			int i = 0;
			for(int j=0; j<ltComponents.size(); j++)
			{
				if (ltComponents.isLeaf(j)) continue;
				LispTree scanLT = ltComponents.getBranch(j);
				if (scanLT.keyword.equalsIgnoreCase("pip"))
				{
					primNP.pipList[i] = new FPGAPip();
					if (makePrimPip(primNP, scanLT, primNP, primNP.pipList[i])) return true;
					i++;
				}
			}
		}
		return false;
	}

	/**
	 * Method to add a port to primitive "np" from the tree in "lt" and
	 * store information about it in the local structure "fp".
	 * Tree has "(port...)" structure.  Returns true on error.
	 */
	private static boolean makePrimPort(PrimitiveNode np, LispTree lt, FPGAPort fp, int net)
	{
		// look for keywords
		LispTree ltName = null, ltPosition = null, ltDirection = null;
		for(int j=0; j<lt.size(); j++)
		{
			if (lt.isLeaf(j)) continue;
			LispTree scanLT = lt.getBranch(j);
			if (scanLT.keyword.equalsIgnoreCase("name"))
			{
				ltName = scanLT;
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("position"))
			{
				ltPosition = scanLT;
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("direction"))
			{
				ltDirection = scanLT;
				continue;
			}
		}

		// validate
		if (ltName == null)
		{
			System.out.println("Port has no name (line " + lt.lineNumber + ")");
			return true;
		}
		if (ltName.size() != 1 || ltName.isBranch(0))
		{
			System.out.println("Port name must be a single atom (line " + ltName.lineNumber + ")");
			return true;
		}
		fp.name = ltName.getLeaf(0);

		if (ltPosition == null)
		{
			System.out.println("Port has no position (line " + lt.lineNumber + ")");
			return true;
		}
		if (ltPosition.size() != 2 || ltPosition.isBranch(0) || ltPosition.isBranch(1))
		{
			System.out.println("Port position must be two atoms (line " + ltPosition.lineNumber + ")");
			return true;
		}
		fp.posX = TextUtils.atof(ltPosition.getLeaf(0)) - np.getDefWidth()/2;
		fp.posY = TextUtils.atof(ltPosition.getLeaf(1)) - np.getDefHeight()/2;

		// determine directionality
		fp.characteristic = PortCharacteristic.UNKNOWN;
		if (ltDirection != null)
		{
			if (ltDirection.size() != 1 || ltDirection.isBranch(0))
			{
				System.out.println("Port direction must be a single atom (line " + ltDirection.lineNumber + ")");
				return true;
			}
			String dir = ltDirection.getLeaf(0);
			if (dir.equalsIgnoreCase("input")) fp.characteristic = PortCharacteristic.IN; else
				if (dir.equalsIgnoreCase("output")) fp.characteristic = PortCharacteristic.OUT; else
					if (dir.equalsIgnoreCase("bidir")) fp.characteristic = PortCharacteristic.BIDIR; else
			{
				System.out.println("Unknown port direction (line " + ltDirection.lineNumber + ")");
				return true;
			}
		}
		fp.con = net;
		return false;
	}

	/**
	 * Method to add a net to primitive "np" from the tree in "lt" and store information
	 * about it in the local object "fNet".
	 * Tree has "(net...)" structure.  Returns true on error.
	 */
	private static boolean makePrimNet(PrimitiveNode np, LispTree lt, FPGANode fn, FPGANet fNet)
	{
		// scan for information in the tree
		fNet.name = null;
		int segCount = 0;
		Point2D [] seg = new Point2D[2];
		for(int j=0; j<lt.size(); j++)
		{
			if (lt.isLeaf(j)) continue;
			LispTree scanLT = lt.getBranch(j);
			if (scanLT.keyword.equalsIgnoreCase("name") && scanLT.size() == 1 && scanLT.isLeaf(0))
			{
				if (fNet.name != null)
				{
					System.out.println("Multiple names for network (line " + lt.lineNumber + ")");
					return true;
				}
				fNet.name = scanLT.getLeaf(0);
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("segment"))
			{
				int pos = 0;
				for(int i=0; i<2; i++)
				{
					// get end of net segment
					if (scanLT.size() < pos+1)
					{
						System.out.println("Incomplete block net segment (line " + scanLT.lineNumber + ")");
						return true;
					}
					if (scanLT.isBranch(pos))
					{
						System.out.println("Must have atoms in block net segment (line " + scanLT.lineNumber + ")");
						return true;
					}
					if (scanLT.getLeaf(pos).equalsIgnoreCase("coord"))
					{
						if (scanLT.size() < pos+3)
						{
							System.out.println("Incomplete block net segment (line " + scanLT.lineNumber + ")");
							return true;
						}
						if (scanLT.isBranch(pos+1) || scanLT.isBranch(pos+2))
						{
							System.out.println("Must have atoms in block net segment (line " + scanLT.lineNumber + ")");
							return true;
						}
						double x = TextUtils.atof(scanLT.getLeaf(pos+1)) - np.getDefWidth()/2;
						double y = TextUtils.atof(scanLT.getLeaf(pos+2)) - np.getDefHeight()/2;
						seg[i] = new Point2D.Double(x, y);
						pos += 3;
					} else if (scanLT.getLeaf(pos).equalsIgnoreCase("port"))
					{
						if (scanLT.size() < pos+2)
						{
							System.out.println("Incomplete block net segment (line " + scanLT.lineNumber + ")");
							return true;
						}
						if (scanLT.isBranch(pos+1))
						{
							System.out.println("Must have atoms in block net segment (line " + scanLT.lineNumber + ")");
							return true;
						}

						// find port
						int found = -1;
						for(int k=0; k<fn.numPorts(); k++)
						{
							if (fn.portList[k].name.equalsIgnoreCase(scanLT.getLeaf(pos+1)))
							{
								found = k;
								break;
							}
						}
						if (found < 0)
						{
							System.out.println("Unknown port on primitive net segment (line " + scanLT.lineNumber + ")");
							return true;
						}
						double x = fn.portList[found].posX;
						double y = fn.portList[found].posY;
						seg[i] = new Point2D.Double(x, y);
						pos += 2;
					} else
					{
						System.out.println("Unknown keyword '" + scanLT.getLeaf(pos) +
							"' in block net segment (line " + scanLT.lineNumber + ")");
						return true;
					}
				}

				Point2D [] newFrom = new Point2D[segCount+1];
				Point2D [] newTo = new Point2D[segCount+1];
				for(int i=0; i<segCount; i++)
				{
					newFrom[i] = fNet.segFrom[i];
					newTo[i] = fNet.segTo[i];
				}
				newFrom[segCount] = seg[0];
				newTo[segCount] = seg[1];
				fNet.segFrom = newFrom;
				fNet.segTo = newTo;
				segCount++;
				continue;
			}
		}
		return false;
	}

	/**
	 * Method to add a pip to primitive "np" from the tree in "lt" and save
	 * information about it in the local object "fpip".
	 * Tree has "(pip...)" structure.  Returns true on error.
	 */
	private static boolean makePrimPip(PrimitiveNode np, LispTree lt, FPGANode fn, FPGAPip fPip)
	{
		// scan for information in this FPGAPIP object
		fPip.name = null;
		fPip.con1 = fPip.con2 = -1;
		for(int j=0; j<lt.size(); j++)
		{
			if (lt.isLeaf(j)) continue;
			LispTree scanLT = lt.getBranch(j);
			if (scanLT.keyword.equalsIgnoreCase("name") && scanLT.size() == 1 && scanLT.isLeaf(0))
			{
				if (fPip.name != null)
				{
					System.out.println("Multiple names for pip (line " + lt.lineNumber + ")");
					return true;
				}
				fPip.name = scanLT.getLeaf(0);
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("position") && scanLT.size() == 2 &&
				scanLT.isLeaf(0) && scanLT.isLeaf(1))
			{
				fPip.posX = TextUtils.atof(scanLT.getLeaf(0)) - np.getDefWidth()/2;
				fPip.posY = TextUtils.atof(scanLT.getLeaf(1)) - np.getDefHeight()/2;
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("connectivity") && scanLT.size() == 2 &&
				scanLT.isLeaf(0) && scanLT.isLeaf(1))
			{
				for(int i=0; i<fn.numNets(); i++)
				{
					if (fn.netList[i].name.equalsIgnoreCase(scanLT.getLeaf(0))) fPip.con1 = i;
					if (fn.netList[i].name.equalsIgnoreCase(scanLT.getLeaf(1))) fPip.con2 = i;
				}
				continue;
			}
		}
		return false;
	}

	/******************** ARCHITECTURE PARSING: LAYOUT ********************/

	/**
	 * Method to scan the entire tree for block definitions and create them.
	 */
	private static Cell placePrimitives(LispTree lt)
	{
		// look through top level for the "blockdef"s
		Cell topLevel = null;
		for(int i=0; i<lt.size(); i++)
		{
			if (lt.isLeaf(i)) continue;
			LispTree subLT = lt.getBranch(i);
			if (!subLT.keyword.equalsIgnoreCase("blockdef") &&
				!subLT.keyword.equalsIgnoreCase("architecture")) continue;

			// create the primitive
			Cell np = makeCell(subLT);
			if (np == null) return null;
			if (subLT.keyword.equalsIgnoreCase("architecture")) topLevel = np;
		}
		return topLevel;
	}

	/**
	 * Method to create a cell from a subtree "lt".
	 * Tree has "(blockdef...)" or "(architecture...)" structure.
	 * Returns nonzero on error.
	 */
	private static Cell makeCell(LispTree lt)
	{
		// find all of the pieces of this block
		LispTree ltAttribute = null, ltNets = null, ltPorts = null, ltComponents = null;
		for(int i=0; i<lt.size(); i++)
		{
			if (lt.isLeaf(i)) continue;
			LispTree scanLT = lt.getBranch(i);
			if (scanLT.keyword.equalsIgnoreCase("attributes"))
			{
				if (ltAttribute != null)
				{
					System.out.println("Multiple 'attributes' sections for a block (line " + lt.lineNumber + ")");
					return null;
				}
				ltAttribute = scanLT;
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("nets"))
			{
				if (ltNets != null)
				{
					System.out.println("Multiple 'nets' sections for a block (line " + lt.lineNumber + ")");
					return null;
				}
				ltNets = scanLT;
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("ports"))
			{
				if (ltPorts != null)
				{
					System.out.println("Multiple 'ports' sections for a block (line " + lt.lineNumber + ")");
					return null;
				}
				ltPorts = scanLT;
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("components"))
			{
				if (ltComponents != null)
				{
					System.out.println("Multiple 'components' sections for a block (line " + lt.lineNumber + ")");
					return null;
				}
				ltComponents = scanLT;
				continue;
			}
		}

		// scan the attributes section
		if (ltAttribute == null)
		{
			System.out.println("Missing 'attributes' sections on a block (line " + lt.lineNumber + ")");
			return null;
		}
		String blockName = null;
		boolean gotSize = false;
		double sizeX = 0, sizeY = 0;
		for(int j=0; j<ltAttribute.size(); j++)
		{
			if (ltAttribute.isLeaf(j)) continue;
			LispTree scanLT = ltAttribute.getBranch(j);
			if (scanLT.keyword.equalsIgnoreCase("name"))
			{
				if (scanLT.size() != 1 || scanLT.isBranch(0))
				{
					System.out.println("Block 'name' attribute should take a single atomic parameter (line " + scanLT.lineNumber + ")");
					return null;
				}
				blockName = scanLT.getLeaf(0);
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("size") && scanLT.size() == 2 &&
				scanLT.isLeaf(0) && scanLT.isLeaf(1))
			{
				gotSize = true;
				sizeX = TextUtils.atof(scanLT.getLeaf(0));
				sizeY = TextUtils.atof(scanLT.getLeaf(1));
				continue;
			}
		}

		// validate
		if (blockName == null)
		{
			System.out.println("Missing 'name' attribute in block definition (line " + ltAttribute.lineNumber + ")");
			return null;
		}

		// make the cell
		Cell cell = Cell.newInstance(Library.getCurrent(), blockName);
		if (cell == null) return null;
		System.out.println("Creating cell '" + blockName + "'");

		// force size by placing pins in the corners
		if (gotSize)
		{
			NodeInst.makeInstance(tech.wirePinNode, new Point2D.Double(0.5, 0.5), 1, 1, cell);
			NodeInst.makeInstance(tech.wirePinNode, new Point2D.Double(sizeX-0.5, 0.5), 1, 1, cell);
			NodeInst.makeInstance(tech.wirePinNode, new Point2D.Double(0.5, sizeY-0.5), 1, 1, cell);
			NodeInst.makeInstance(tech.wirePinNode, new Point2D.Double(sizeX-0.5, sizeY-0.5), 1, 1, cell);
		}

		// add any unrecognized attributes
		for(int j=0; j<ltAttribute.size(); j++)
		{
			if (ltAttribute.isLeaf(j)) continue;
			LispTree scanLT = ltAttribute.getBranch(j);
			if (scanLT.keyword.equalsIgnoreCase("name")) continue;
			if (scanLT.keyword.equalsIgnoreCase("size")) continue;

			if (scanLT.size() != 1 || scanLT.isBranch(0))
			{
				System.out.println("Attribute '" + scanLT.keyword + "' attribute should take a single atomic parameter (line " +
					scanLT.lineNumber + ")");
				return null;
			}
			cell.newVar(scanLT.keyword, scanLT.getLeaf(0));
		}

		// place block components
		if (ltComponents != null)
		{
			for(int j=0; j<ltComponents.size(); j++)
			{
				if (ltComponents.isLeaf(j)) continue;
				LispTree scanLT = ltComponents.getBranch(j);
				if (scanLT.keyword.equalsIgnoreCase("repeater"))
				{
					if (makeBlockRepeater(cell, scanLT)) return null;
					continue;
				}
				if (scanLT.keyword.equalsIgnoreCase("instance"))
				{
					if (makeBlockInstance(cell, scanLT)) return null;
					continue;
				}
			}
		}

		// place block ports
		if (ltPorts != null)
		{
			for(int j=0; j<ltPorts.size(); j++)
			{
				if (ltPorts.isLeaf(j)) continue;
				LispTree scanLT = ltPorts.getBranch(j);
				if (scanLT.keyword.equalsIgnoreCase("port"))
				{
					if (makeBlockPort(cell, scanLT)) return null;
				}
			}
		}

		// place block nets
		if (ltNets != null)
		{
			// read the block nets
			for(int j=0; j<ltNets.size(); j++)
			{
				if (ltNets.isLeaf(j)) continue;
				LispTree scanLT = ltNets.getBranch(j);
				if (scanLT.keyword.equalsIgnoreCase("net"))
				{
					if (makeBlockNet(cell, scanLT)) return null;
				}
			}
		}
		return cell;
	}

	/**
	 * Method to place an instance in cell "cell" from the LISPTREE in "lt".
	 * Tree has "(instance...)" structure.  Returns true on error.
	 */
	private static boolean makeBlockInstance(Cell cell, LispTree lt)
	{
		// scan for information in this block instance object
		LispTree ltType = null, ltName = null, ltPosition = null, ltRotation = null, ltAttribute = null;
		for(int i=0; i<lt.size(); i++)
		{
			if (lt.isLeaf(i)) continue;
			LispTree scanLT = lt.getBranch(i);
			if (scanLT.keyword.equalsIgnoreCase("type"))
			{
				if (ltType != null)
				{
					System.out.println("Multiple 'type' sections for a block (line " + lt.lineNumber + ")");
					return true;
				}
				ltType = scanLT;
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("name"))
			{
				if (ltName != null)
				{
					System.out.println("Multiple 'name' sections for a block (line " + lt.lineNumber + ")");
					return true;
				}
				ltName = scanLT;
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("position"))
			{
				if (ltPosition != null)
				{
					System.out.println("Multiple 'position' sections for a block (line " + lt.lineNumber + ")");
					return true;
				}
				ltPosition = scanLT;
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("rotation"))
			{
				if (ltRotation != null)
				{
					System.out.println("Multiple 'rotation' sections for a block (line " + lt.lineNumber + ")");
					return true;
				}
				ltRotation = scanLT;
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("attributes"))
			{
				if (ltAttribute != null)
				{
					System.out.println("Multiple 'attributes' sections for a block (line " + lt.lineNumber + ")");
					return true;
				}
				ltAttribute = scanLT;
				continue;
			}
		}

		// validate
		if (ltType == null)
		{
			System.out.println("No 'type' specified for block instance (line " + lt.lineNumber + ")");
			return true;
		}
		if (ltType.size() != 1 || ltType.isBranch(0))
		{
			System.out.println("Need one atom in 'type' of block instance (line " + ltType.lineNumber + ")");
			return true;
		}
		NodeProto np = tech.findNodeProto(ltType.getLeaf(0));
		if (np == null) np = cell.getLibrary().findNodeProto(ltType.getLeaf(0));
		if (np == null)
		{
			System.out.println("Cannot find block type '" + ltType.getLeaf(0) + "' (line " + ltType.lineNumber + ")");
			return true;
		}
		if (ltPosition == null)
		{
			System.out.println("No 'position' specified for block instance (line " + lt.lineNumber + ")");
			return true;
		}
		if (ltPosition.size() != 2 || ltPosition.isBranch(0) || ltPosition.isBranch(1))
		{
			System.out.println("Need two atoms in 'position' of block instance (line " + ltPosition.lineNumber + ")");
			return true;
		}
		int rotation = 0;
		if (ltRotation != null)
		{
			if (ltRotation.size() != 1 || ltRotation.isBranch(0))
			{
				System.out.println("Need one atom in 'rotation' of block instance (line " + ltRotation.lineNumber + ")");
				return true;
			}
			rotation = TextUtils.atoi(ltRotation.getLeaf(0)) * 10;
		}

		// name the instance if one is given
		String nodeName = null;
		if (ltName != null)
		{
			if (ltName.size() != 1 || ltName.isBranch(0))
			{
				System.out.println("Need one atom in 'name' of block instance (line " + ltName.lineNumber + ")");
				return true;
			}
			nodeName = ltName.getLeaf(0);
		}

		// place the instance
		double posX = TextUtils.atof(ltPosition.getLeaf(0));
		double posY = TextUtils.atof(ltPosition.getLeaf(1));
		double wid = np.getDefWidth();
		double hei = np.getDefHeight();
		if (np instanceof PrimitiveNode)
		{
			posX += wid/2;
			posY += hei/2;
		}
		Point2D ctr = new Point2D.Double(posX, posY);
        Orientation orient = Orientation.fromAngle(rotation);
		NodeInst ni = NodeInst.makeInstance(np, ctr, wid, hei, cell, orient, nodeName, 0);
//		NodeInst ni = NodeInst.makeInstance(np, ctr, wid, hei, cell, rotation, nodeName, 0);
		if (ni == null) return true;

		// add any attributes
		if (ltAttribute != null)
		{
			for(int i=0; i<ltAttribute.size(); i++)
			{
				if (ltAttribute.isLeaf(i)) continue;
				LispTree scanLT = ltAttribute.getBranch(i);
				if (scanLT.size() != 1 || scanLT.isBranch(0))
				{
					System.out.println("Attribute '" + scanLT.keyword+ "' attribute should take a single atomic parameter (line " + lt.lineNumber + ")");
					return true;
				}
				ni.newVar(scanLT.keyword, scanLT.getLeaf(0));
			}
		}
		return false;
	}

	/**
	 * Method to add a port to block "cell" from the tree in "lt".
	 * Tree has "(port...)" structure.  Returns true on error.
	 */
	private static boolean makeBlockPort(Cell cell, LispTree lt)
	{
		LispTree ltName = null, ltPosition = null;
		for(int j=0; j<lt.size(); j++)
		{
			if (lt.isLeaf(j)) continue;
			LispTree scanLT = lt.getBranch(j);
			if (scanLT.keyword.equalsIgnoreCase("name"))
			{
				ltName = scanLT;
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("position"))
			{
				ltPosition = scanLT;
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("direction"))
			{
				continue;
			}
		}

		// make the port
		if (ltName == null)
		{
			System.out.println("Port has no name (line " + lt.lineNumber + ")");
			return true;
		}
		if (ltName.size() != 1 || ltName.isBranch(0))
		{
			System.out.println("Port name must be a single atom (line " + ltName.lineNumber + ")");
		}
		if (ltPosition == null)
		{
			System.out.println("Port has no position (line " + lt.lineNumber + ")");
			return true;
		}
		if (ltPosition.size() != 2 || ltPosition.isBranch(0) || ltPosition.isBranch(1))
		{
			System.out.println("Port position must be two atoms (line " + ltPosition.lineNumber + ")");
		}

		// create the structure
		double posX = TextUtils.atof(ltPosition.getLeaf(0));
		double posY = TextUtils.atof(ltPosition.getLeaf(1));
		NodeInst ni = NodeInst.makeInstance(tech.wirePinNode, new Point2D.Double(posX, posY), 0, 0, cell);
		if (ni == null)
		{
			System.out.println("Error creating pin for port '" + ltName.getLeaf(0) + "' (line " + lt.lineNumber + ")");
			return true;
		}
		PortInst pi = ni.getOnlyPortInst();
		Export e = Export.newInstance(cell, pi, ltName.getLeaf(0));
		if (e == null)
		{
			System.out.println("Error creating port '" + ltName.getLeaf(0) + "' (line " + lt.lineNumber + ")");
			return true;
		}
		return false;
	}

	/**
	 * Method to place a repeater in cell "cell" from the LISPTREE in "lt".
	 * Tree has "(repeater...)" structure.  Returns true on error.
	 */
	private static boolean makeBlockRepeater(Cell cell, LispTree lt)
	{
		LispTree ltName = null, ltPortA = null, ltPortB = null;
		for(int j=0; j<lt.size(); j++)
		{
			if (lt.isLeaf(j)) continue;
			LispTree scanLT = lt.getBranch(j);
			if (scanLT.keyword.equalsIgnoreCase("name"))
			{
				ltName = scanLT;
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("porta"))
			{
				ltPortA = scanLT;
				continue;
			}
			if (scanLT.keyword.equalsIgnoreCase("portb"))
			{
				ltPortB = scanLT;
				continue;
			}
		}

		// make the repeater
		if (ltPortA == null)
		{
			System.out.println("Repeater has no 'porta' (line " + lt.lineNumber + ")");
			return true;
		}
		if (ltPortA.size() != 2 || ltPortA.isBranch(0) || ltPortA.isBranch(1))
		{
			System.out.println("Repeater 'porta' position must be two atoms (line " + ltPortA.lineNumber + ")");
		}
		if (ltPortB == null)
		{
			System.out.println("Repeater has no 'portb' (line " + lt.lineNumber + ")");
			return true;
		}
		if (ltPortB.size() != 2 || ltPortB.isBranch(0) || ltPortB.isBranch(1))
		{
			System.out.println("Repeater 'portb' position must be two atoms (line " + ltPortB.lineNumber + ")");
		}

		// name the repeater if one is given
		String name = null;
		if (ltName != null)
		{
			if (ltName.size() != 1 || ltName.isBranch(0))
			{
				System.out.println("Need one atom in 'name' of block repeater (line " + ltName.lineNumber + ")");
				return true;
			}
			name = ltName.getLeaf(0);
		}

		// create the repeater
		double portAX = TextUtils.atof(ltPortA.getLeaf(0));
		double portAY = TextUtils.atof(ltPortA.getLeaf(1));
		double portBX = TextUtils.atof(ltPortB.getLeaf(0));
		double portBY = TextUtils.atof(ltPortB.getLeaf(1));
		int angle = GenMath.figureAngle(new Point2D.Double(portAX, portAY), new Point2D.Double(portBX, portBY));
		Point2D ctr = new Point2D.Double((portAX + portBX) / 2, (portAY + portBY) / 2);
        Orientation orient = Orientation.fromAngle(angle);
		NodeInst ni = NodeInst.makeInstance(tech.repeaterNode, ctr, 10,3, cell, orient, name, 0);
//		NodeInst ni = NodeInst.makeInstance(tech.repeaterNode, ctr, 10,3, cell, angle, name, 0);
		if (ni == null)
		{
			System.out.println("Error creating repeater (line " + lt.lineNumber + ")");
			return true;
		}
		return false;
	}

	/**
	 * Method to extract block net information from the LISPTREE in "lt".
	 * Tree has "(net...)" structure.  Returns true on error.
	 */
	private static boolean makeBlockNet(Cell cell, LispTree lt)
	{
		// find the net name
		for(int j=0; j<lt.size(); j++)
		{
			if (lt.isLeaf(j)) continue;
			LispTree scanLT = lt.getBranch(j);
			if (scanLT.keyword.equalsIgnoreCase("name"))
			{
				if (scanLT.size() != 1 || scanLT.isBranch(0))
				{
					System.out.println("Net name must be a single atom (line " + scanLT.lineNumber + ")");
					return true;
				}
				continue;
			}
		}

		// scan for segment objects
		for(int j=0; j<lt.size(); j++)
		{
			if (lt.isLeaf(j)) continue;
			LispTree scanLT = lt.getBranch(j);
			if (scanLT.keyword.equalsIgnoreCase("segment"))
			{
				int pos = 0;
				NodeInst [] nis = new NodeInst[2];
				PortProto [] pps = new PortProto[2];
				for(int i=0; i<2; i++)
				{
					// get end of arc
					if (scanLT.size() < pos+1)
					{
						System.out.println("Incomplete block net segment (line " + scanLT.lineNumber + ")");
						return true;
					}
					if (scanLT.isBranch(pos))
					{
						System.out.println("Must have atoms in block net segment (line " + scanLT.lineNumber + ")");
						return true;
					}
					if (scanLT.getLeaf(pos).equalsIgnoreCase("component"))
					{
						if (scanLT.size() < pos+3)
						{
							System.out.println("Incomplete block net segment (line " + scanLT.lineNumber + ")");
							return true;
						}
						if (scanLT.isBranch(pos+1) || scanLT.isBranch(pos+2))
						{
							System.out.println("Must have atoms in block net segment (line " + scanLT.lineNumber + ")");
							return true;
						}

						// find component and port
						NodeInst niFound = null;
						String name = scanLT.getLeaf(pos+1);
						for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
						{
							NodeInst ni = (NodeInst)it.next();
							if (ni.getName().equalsIgnoreCase(name))
							{
								niFound = ni;
								break;
							}
						}
						if (niFound == null)
						{
							System.out.println("Cannot find component '" + scanLT.getLeaf(pos+1) +
								"' in block net segment (line " + scanLT.lineNumber + ")");
							return true;
						}
						nis[i] = niFound;
						pps[i] = niFound.getProto().findPortProto(scanLT.getLeaf(pos+2));
						if (pps[i] == null)
						{
							System.out.println("Cannot find port '" + scanLT.getLeaf(pos+2) +
								"' on component '" + scanLT.getLeaf(pos+1) +
								"' in block net segment (line " + scanLT.lineNumber + ")");
							return true;
						}
						pos += 3;
					} else if (scanLT.getLeaf(pos).equalsIgnoreCase("coord"))
					{
						if (scanLT.size() < pos+3)
						{
							System.out.println("Incomplete block net segment (line " + scanLT.lineNumber + ")");
							return true;
						}
						if (scanLT.isBranch(pos+1) || scanLT.isBranch(pos+2))
						{
							System.out.println("Must have atoms in block net segment (line " + scanLT.lineNumber + ")");
							return true;
						}
						double x = TextUtils.atof(scanLT.getLeaf(pos+1));
						double y = TextUtils.atof(scanLT.getLeaf(pos+2));
						Rectangle2D search = new Rectangle2D.Double(x, y, 0, 0);

						// find pin at this point
						NodeInst niFound = null;
						for(Iterator<Geometric> it = cell.searchIterator(search); it.hasNext(); )
						{
							Geometric geom = (Geometric)it.next();
							if (!(geom instanceof NodeInst)) continue;
							NodeInst ni = (NodeInst)geom;
							if (ni.getProto() != tech.wirePinNode) continue;
							if (ni.getTrueCenterX() == x && ni.getTrueCenterY() == y)
							{
								niFound = ni;
								break;
							}
						}
						if (niFound == null)
						{
							niFound = NodeInst.makeInstance(tech.wirePinNode, new Point2D.Double(x, y), 0, 0, cell);
							if (niFound == null)
							{
								System.out.println("Cannot create pin for block net segment (line " + scanLT.lineNumber + ")");
								return true;
							}
						}
						nis[i] = niFound;
						pps[i] = niFound.getProto().getPort(0);
						pos += 3;
					} else if (scanLT.getLeaf(pos).equalsIgnoreCase("port"))
					{
						if (scanLT.size() < pos+2)
						{
							System.out.println("Incomplete block net segment (line " + scanLT.lineNumber + ")");
							return true;
						}
						if (scanLT.isBranch(pos+1))
						{
							System.out.println("Must have atoms in block net segment (line " + scanLT.lineNumber + ")");
							return true;
						}

						// find port
						Export pp = cell.findExport(scanLT.getLeaf(pos+1));
						if (pp == null)
						{
							System.out.println("Cannot find port '" + scanLT.getLeaf(pos+1) +
								"' in block net segment (line " + scanLT.lineNumber + ")");
							return true;
						}
						pps[i] = pp.getOriginalPort().getPortProto();
						nis[i] = pp.getOriginalPort().getNodeInst();
						pos += 2;
					} else
					{
						System.out.println("Unknown keyword '" + scanLT.getLeaf(pos) +
							"' in block net segment (line " + scanLT.lineNumber + ")");
						return true;
					}
				}

				// now create the arc
				PortInst pi0 = nis[0].findPortInstFromProto(pps[0]);
				PortInst pi1 = nis[1].findPortInstFromProto(pps[1]);
				ArcInst ai = ArcInst.makeInstance(tech.wireArc, 0, pi0, pi1);
				if (ai == null)
				{
					System.out.println("Cannot run segment (line " + scanLT.lineNumber + ")");
					return true;
				}
			}
		}
		return false;
	}

}
