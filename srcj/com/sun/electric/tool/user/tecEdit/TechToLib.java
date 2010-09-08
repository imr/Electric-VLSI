/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TechToLib.java
 * Technology Editor, conversion of technologies to libraries
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.tecEdit;

import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.DRCRules;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveNodeGroup;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.GraphicsPreferences;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.util.math.DBMath;
import com.sun.electric.util.math.GenMath;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

/**
 * This class generates technology libraries from technologys.
 */
public class TechToLib
{
	/**
	 * Method to convert the current technology into a library.
	 */
	public static void makeLibFromTech()
	{
		List<Technology> techs = new ArrayList<Technology>();
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			if (tech.isNonStandard()) continue;
			techs.add(tech);
		}
		String [] techChoices = new String[techs.size()];
		for(int i=0; i<techs.size(); i++)
			techChoices[i] = techs.get(i).getTechName();
		String chosen = (String)JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "Technology to Edit",
			"Choose a technology to edit", JOptionPane.QUESTION_MESSAGE, null, techChoices, Technology.getCurrent().getTechName());
		if (chosen == null) return;
		Technology tech = Technology.findTechnology(chosen);
		Library already = Library.findLibrary(tech.getTechName());
		if (already != null)
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"There is already a library called '" + tech.getTechName() + "'.  Delete it first.",
				"Cannot Convert Technology", JOptionPane.ERROR_MESSAGE);
			System.out.println();
			return;
		}
		new LibFromTechJob(tech, false);
	}

	/**
	 * Class to create a technology-library from a technology (in a Job).
	 */
	private static class LibFromTechJob extends Job
	{
		private Technology tech;
        private String libraryName;
        private boolean doItNow;
        private Library lib;
        private GraphicsPreferences gp = UserInterfaceMain.getGraphicsPreferences();

		private LibFromTechJob(Technology tech, boolean doItNow)
		{
			super("Make Technology Library from Technology", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.tech = tech;
            this.doItNow = doItNow;
            if (!doItNow)
                startJob();
		}

        public String getLibraryName() { return libraryName; }

		public boolean doIt()
		{
			lib = makeLibFromTech(tech, gp);
			if (lib == null) return false;
            fieldVariableChanged("lib");

			// switch to the library and show a cell
            if (!doItNow)
            fieldVariableChanged("libraryName");
            libraryName = lib.getName();
			return true;
		}

        @Override
        public void terminateOK() {
            User.setCurrentLibrary(lib);
        }
	}

	/**
	 * Method to convert technology "tech" into a library and return that library.
	 * Returns NOLIBRARY on error
	 */
	public static Library makeLibFromTech(Technology tech, GraphicsPreferences gp)
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

		// compute the number of layers (ignoring pseudo-layers)
		int layerTotal = 0;
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
			if (!layer.isPseudoLayer()) layerTotal++;
		}

		// build the general information cell
		GeneralInfo gi = new GeneralInfo();
        gi.shortName = tech.getTechShortName();
        if (gi.shortName == null)
            gi.shortName = tech.getTechName();
        gi.nonElectrical = tech.isNonElectrical();
		gi.scale = tech.getScale();
        gi.scaleRelevant = tech.isScaleRelevant();
        gi.resolution = tech.getFactoryResolution();
        gi.defaultFoundry = tech.getPrefFoundry();
        gi.defaultNumMetals = tech.getNumMetals();
		gi.description = tech.getTechDesc();
		gi.minRes = tech.getMinResistanceSetting().getDoubleFactoryValue();
		gi.minCap = tech.getMinCapacitanceSetting().getDoubleFactoryValue();
        gi.maxSeriesResistance = tech.getMaxSeriesResistance();
		gi.gateShrinkage = tech.getGateLengthSubtraction();
		gi.includeGateInResistance = tech.isGateIncluded();
		gi.includeGround = tech.isGroundNetIncluded();
        gi.gateCapacitance = tech.getGateCapacitanceSetting().getDoubleFactoryValue();
        gi.wireRatio = tech.getWireRatioSetting().getDoubleFactoryValue();
        gi.diffAlpha = tech.getDiffAlphaSetting().getDoubleFactoryValue();
		Color [] wholeMap = gp.getColorMap(tech);
		int numLayers = gp.getNumTransparentLayers(tech);
		gi.transparentColors = new Color[numLayers];
		for(int i=0; i<numLayers; i++)
			gi.transparentColors[i] = wholeMap[1<<i];
        gi.spiceLevel1Header = tech.getSpiceHeaderLevel1();
        gi.spiceLevel2Header = tech.getSpiceHeaderLevel2();
        gi.spiceLevel3Header = tech.getSpiceHeaderLevel3();
        DRCRules drcRules = tech.getFactoryDesignRules();
        if (drcRules != null) {
            int rulesSize = layerTotal*(layerTotal + 1)/2;
            gi.conDist = new double[rulesSize];
            gi.unConDist = new double[rulesSize];
            Arrays.fill(gi.conDist, -1);
            Arrays.fill(gi.unConDist, -1);
            int ruleIndex = 0;
            for (int i1 = 0; i1 < layerTotal; i1++) {
                for (int i2 = i1; i2 < layerTotal; i2++) {
                    for (DRCTemplate t: drcRules.getSpacingRules(drcRules.getRuleIndex(i1, i2), DRCTemplate.DRCRuleType.SPACING, false)) {
                        if (t.ruleType == DRCTemplate.DRCRuleType.CONSPA)
                            gi.conDist[ruleIndex] = t.getValue(0);
                        else if (t.ruleType == DRCTemplate.DRCRuleType.UCONSPA)
                            gi.unConDist[ruleIndex] = t.getValue(0);
                    }
                    ruleIndex++;
                }
            }
        }
		gi.generate(fNp);

		// create the layer node names
		Map<Layer,Cell> layerCells = new HashMap<Layer,Cell>();

		// create the layer nodes
		System.out.println("Creating the layers...");
		ArrayList<String> layerSequence = new ArrayList<String>();
        LayerInfo [] lList = new LayerInfo[layerTotal];
        Map<Layer,String> gdsLayers = tech.getGDSLayers();

        int layIndex = 0;
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
			if (layer.isPseudoLayer()) continue;
			EGraphics desc = gp.getGraphics(layer);
			String fName = "layer-" + layer.getName() + "{lay}";

			// make sure the layer doesn't exist
			if (lib.findNodeProto(fName) != null)
			{
				System.out.println("Warning: already a cell '" + fName + "'.  Creating a new version");
			}

			Cell lNp = Cell.newInstance(lib, fName);
			if (lNp == null) return null;
			lNp.setTechnology(Artwork.tech());
			lNp.setInTechnologyLibrary();
			layerCells.put(layer, lNp);

			LayerInfo li = new LayerInfo();
            lList[layIndex++] = li;
            li.name = layer.getName();
			li.fun = layer.getFunction();
			li.funExtra = layer.getFunctionExtras();
            li.pseudo = layer.isPseudoLayer();
			li.desc = desc;
            if (li.pseudo) {
                String masterName = layer.getNonPseudoLayer().getName();
                for(int j=0; j<layIndex; j++) {
                    if (lList[j].name.equals(masterName)) { lList[j].myPseudo = li;   break; }
                }
                continue;
            }

			// compute foreign file formats
			li.cif = (String)layer.getCIFLayerSetting().getFactoryValue();
            li.dxf = (String)layer.getDXFLayerSetting().getFactoryValue();
            li.skill = (String)layer.getSkillLayerSetting().getFactoryValue();
            String gdsLayer = gdsLayers.get(layer);
            if (gdsLayer != null)
                li.gds = gdsLayer;

			// compute the SPICE information
			li.spiRes = layer.getResistanceSetting().getDoubleFactoryValue();
			li.spiCap = layer.getCapacitanceSetting().getDoubleFactoryValue();
			li.spiECap = layer.getEdgeCapacitanceSetting().getDoubleFactoryValue();

			// compute the 3D information
			li.height3d = layer.getDistance();
			li.thick3d = layer.getThickness();

			// build the layer cell
			li.generate(lNp);
			layerSequence.add(lNp.getName().substring(6));
		}
		if (layIndex != layerTotal)
		{
			System.out.println("INTERNAL ERROR: ");
		}

		// save the layer sequence
        String[] layerSequenceArray = layerSequence.toArray(new String[layerSequence.size()]);
		lib.newVar(Info.LAYERSEQUENCE_KEY, layerSequenceArray);

		// create the arc cells
		System.out.println("Creating the arcs...");
        int arcTotal = 0;
		for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
			if (!it.next().isNotUsed()) arcTotal++;

		ArcInfo[] aList = new ArcInfo[arcTotal];
		String [] arcSequence = new String[arcTotal];
        int arcCount = 0;
		Map<ArcProto,Cell> arcCells = new HashMap<ArcProto,Cell>();
		for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			if (ap.isNotUsed()) continue;
			ArcInfo aIn = makeArcInfo(ap, lList);
            aList[arcCount] = aIn;
			arcSequence[arcCount] = ap.getName();
			arcCount++;

			String fName = "arc-" + ap.getName() + "{lay}";

			// make sure the arc doesn't exist
			if (lib.findNodeProto(fName) != null)
			{
				System.out.println("Warning: already a cell '" + fName + "'.  Creating a new version");
			}

			Cell aNp = Cell.makeInstance(lib, fName);
			if (aNp == null) return null;
			aNp.setTechnology(Artwork.tech());
			aNp.setInTechnologyLibrary();

			arcCells.put(ap, aNp);
			aIn.generate(aNp);

			// now create the arc layers
			double wid = ap.getDefaultLambdaBaseWidth();
			double widX4 = wid * 4;
			if (widX4 <= 0) widX4 = 10;
			Poly [] polys = ap.getShapeOfDummyArc(widX4);
			double xOff = wid*2 + DBMath.gridToLambda(ap.getMaxLayerGridExtend());
			for(int i=0; i<polys.length; i++)
			{
				Poly poly = polys[i];
				Layer arcLayer = poly.getLayer().getNonPseudoLayer();
				if (arcLayer == null) continue;
				EGraphics arcDesc = gp.getGraphics(arcLayer);

				// scale the arc geometry appropriately
				Point2D [] points = poly.getPoints();
				for(int k=0; k<points.length; k++)
					poly.setPoint(k, points[k].getX() - xOff - 20, points[k].getY() - 5);

				// create the node to describe this layer
				List<NodeInst> placedNodes = placeGeometry(poly, aNp);
				if (placedNodes == null) continue;

				// get graphics for this layer
				for(NodeInst ni : placedNodes)
				{
					Manipulate.setPatch(ni, arcDesc);
					Cell layerCell = layerCells.get(arcLayer);
					if (layerCell != null) ni.newVar(Info.LAYER_KEY, layerCell.getId());
					ni.newVar(Info.OPTION_KEY, new Integer(Info.LAYERPATCH));
				}
			}
			NodeInst ni = NodeInst.makeInstance(Artwork.tech().boxNode, new Point2D.Double(-20 - xOff, -5), wid*5, wid, aNp);
			if (ni == null) return null;
			ni.newVar(Artwork.ART_COLOR, new Integer(EGraphics.WHITE));
			ni.newVar(Info.OPTION_KEY, new Integer(Info.HIGHLIGHTOBJ));

			// compact it accordingly
			ArcInfo.compactCell(aNp);
		}

		// save the arc sequence
		lib.newVar(Info.ARCSEQUENCE_KEY, arcSequence);

		// create the node cells
		System.out.println("Creating the nodes...");
		List<String> nodeSequence = new ArrayList<String>();
		List<NodeInfo> nList = new ArrayList<NodeInfo>();
        Cell dummyCell = Cell.newInstance(lib, "dummyCell{lay}");

        List<PrimitiveNode> nodesToWrite = new ArrayList<PrimitiveNode>();
		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode pnp = it.next();
			if (pnp.isNotUsed()) continue;

			// only consider the first node in a group
			if (pnp.getPrimitiveNodeGroup() == null) nodesToWrite.add(pnp); else
            {
            	if (pnp.getPrimitiveNodeGroup().getNodes().get(0) != pnp) continue;

            	NodeInst ni = NodeInst.makeDummyInstance(pnp);
				Poly [] polys = tech.getShapeOfNode(ni);
				Set<Layer> inPolys = new HashSet<Layer>();
				for(int i=0; i<polys.length; i++) inPolys.add(polys[i].getLayer());

				boolean differentLayers = false;
				for(PrimitiveNode alt : pnp.getPrimitiveNodeGroup().getNodes())
				{
					if (alt == pnp) continue;
	                NodeInst altNi = NodeInst.makeDummyInstance(alt);
					Poly [] altPolys = tech.getShapeOfNode(altNi);
					Set<Layer> inAltPolys = new HashSet<Layer>();
					for(int i=0; i<altPolys.length; i++) inAltPolys.add(altPolys[i].getLayer());

					for(Layer l : inPolys)
					{
						if (inAltPolys.contains(l)) { inAltPolys.remove(l);  continue; }
						differentLayers = true;
						break;
					}
					if (inAltPolys.size() > 0) differentLayers = true;
					if (differentLayers) break;
				}
				if (differentLayers)
				{
					for(PrimitiveNode alt : pnp.getPrimitiveNodeGroup().getNodes())
						nodesToWrite.add(alt);	
				} else
				{
					nodesToWrite.add(pnp);
				}
            	if (pnp.getPrimitiveNodeGroup().getNodes().get(0) != pnp) continue;            	
            }
		}

		for(PrimitiveNode pnp : nodesToWrite)
		{
            NodeInfo nIn = makeNodeInfo(pnp, lList, aList);
            nList.add(nIn);
            nodeSequence.add(pnp.getName());

			// create the node layers
			boolean first = true;
			double xS = pnp.getDefWidth() * 2;
			double yS = pnp.getDefHeight() * 2;
			if (xS < 3) xS = 3;
			if (yS < 3) yS = 3;
			double nodeXPos = -xS*2;
			Point2D [] pos = new Point2D[4];
			pos[0] = new Point2D.Double(nodeXPos - xS, -5 + yS);
			pos[1] = new Point2D.Double(nodeXPos + xS, -5 + yS);
			pos[2] = new Point2D.Double(nodeXPos - xS, -5 - yS);
			pos[3] = new Point2D.Double(nodeXPos + xS, -5 - yS);
			SizeOffset so = pnp.getProtoSizeOffset();
			xS = pnp.getDefWidth() - so.getLowXOffset() - so.getHighXOffset();
			yS = pnp.getDefHeight() - so.getLowYOffset() - so.getHighYOffset();
			double [] xsc = new double[4];
			double [] ysc = new double[4];
			xsc[0] = xS*1;   ysc[0] = yS*1;
			xsc[1] = xS*2;   ysc[1] = yS*1;
			xsc[2] = xS*1;   ysc[2] = yS*2;
			xsc[3] = xS*2;   ysc[3] = yS*2;

			// for multicut contacts, make large size be just right for 2 cuts
			if (pnp.isMulticut())
			{
                EPoint min2size = pnp.getMulticut2Size();
                double min2X = min2size.getLambdaX();
                double min2Y = min2size.getLambdaY();
                xsc[1] = min2X;
                xsc[3] = min2X;
                ysc[2] = min2Y;
                ysc[3] = min2Y;
			}
			Cell nNp = null;
			Rectangle2D mainBounds = null;
			for(int e=0; e<4; e++)
			{
				// do not create node if main example had no polygons
				if (e != 0 && first) continue;

				// square nodes have only two examples
                if (pnp.isSquare() && (e == 1 || e == 2)) continue;
                double newXSize = xsc[e] + so.getLowXOffset() + so.getHighXOffset();
                double newYSize = ysc[e] + so.getLowYOffset() + so.getHighYOffset();
                NodeInst oNi = NodeInst.makeInstance(pnp, EPoint.snap(pos[e]), newXSize, newYSize, dummyCell);
				Poly [] polys = tech.getShapeOfNode(oNi);
				int j = polys.length;
				for(int i=0; i<j; i++)
				{
					Poly poly = polys[i];
					Layer nodeLayer = poly.getLayer().getNonPseudoLayer();
					if (nodeLayer == null) continue;
					EGraphics desc = gp.getGraphics(nodeLayer);

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
						String fName = "node-" + pnp.getName() + "{lay}";

						// make sure the node doesn't exist
						if (lib.findNodeProto(fName) != null)
						{
							System.out.println("Warning: already a cell '" + fName + "'.  Creating a new version");
						}

						// use "newInstance" instead of "makeInstance" so that cell center is not placed
						nNp = Cell.newInstance(lib, fName);
						if (nNp == null) return null;

						nNp.setTechnology(Artwork.tech());
						nNp.setInTechnologyLibrary();
						nIn.generate(nNp);
					}

					// create the node to describe this layer
					List<NodeInst> placedNodes = placeGeometry(poly, nNp);
					if (placedNodes == null)
					{
                        System.out.println("Error placing geometry " + poly.getStyle() + " on " + nNp);
                        continue;
                    }

					// get graphics for this layer
					for(NodeInst ni : placedNodes)
					{
						Manipulate.setPatch(ni, desc);
						Cell layerCell = layerCells.get(nodeLayer);
						if (layerCell != null) ni.newVar(Info.LAYER_KEY, layerCell.getId());
						ni.newVar(Info.OPTION_KEY, new Integer(Info.LAYERPATCH));

//	 					// set minimum polygon factor on smallest example
//	 					if (e != 0) continue;
//	 					if (i < nodeLayers.length)
//	 					{
//	 						if (nodeLayers[i].getRepresentation() == Technology.NodeLayer.MINBOX)
//	 						{
//	 							ni.newDisplayVar(Info.MINSIZEBOX_KEY, "MIN");
//	 						}
//	 					}
					}
				}
				if (first) continue;

				// create the highlight node
				xS = pnp.getDefWidth() - so.getLowXOffset() - so.getHighXOffset();
				yS = pnp.getDefHeight() - so.getLowYOffset() - so.getHighYOffset();
				Point2D loc = new Point2D.Double(pos[e].getX() + (so.getLowXOffset() - so.getHighXOffset())/2,
					pos[e].getY() + (so.getLowYOffset() - so.getHighYOffset())/2);
				NodeInst ni = NodeInst.makeInstance(Artwork.tech().boxNode, loc, xsc[e], ysc[e], nNp);
				if (ni == null) return null;
				ni.newVar(Artwork.ART_COLOR, new Integer(EGraphics.makeIndex(Color.WHITE)));
				ni.newVar(Info.OPTION_KEY, new Integer(Info.HIGHLIGHTOBJ));

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
				if (addPortsToPrimitive(pnp, oNi, tech, arcCells, aList, nNp)) return null;
//				Map<PrimitivePort,NodeInst> portNodes = new HashMap<PrimitivePort,NodeInst>();
//				for(Iterator<PortProto> pIt = pnp.getPorts(); pIt.hasNext(); )
//				{
//					PrimitivePort pp = (PrimitivePort)pIt.next();
//					Poly poly = tech.getShapeOfPort(oNi, pp);
//					SizeOffset pSo = Generic.tech().portNode.getProtoSizeOffset();
//					double width = poly.getBounds2D().getWidth() + pSo.getLowXOffset() + pSo.getHighXOffset();
//					double height = poly.getBounds2D().getHeight() + pSo.getLowYOffset() + pSo.getHighYOffset();
//					NodeInst pNi = NodeInst.makeInstance(Generic.tech().portNode, new Point2D.Double(poly.getCenterX(), poly.getCenterY()),
//						width, height, nNp);
//					if (pNi == null) return null;
//					portNodes.put(pp, pNi);
//					pNi.newVar(Info.OPTION_KEY, new Integer(Info.LAYERPATCH));
//					pNi.newDisplayVar(Info.PORTNAME_KEY, pp.getName());
//
//					// on the first sample, also show angle and connection
//					if (e != 0) continue;
//					if (pp.getAngle() != 0 || pp.getAngleRange() != 180)
//					{
//						pNi.newVar(Info.PORTANGLE_KEY, new Integer(pp.getAngle()));
//						pNi.newVar(Info.PORTRANGE_KEY, new Integer(pp.getAngleRange()));
//					}
//
//					// add in the "local" port connections (from this tech)
//					ArcProto [] connects = pp.getConnections();
//					List<Cell> validConns = new ArrayList<Cell>();
//					for(int i=0; i<connects.length; i++)
//					{
//						if (connects[i].getTechnology() != tech) continue;
//						Cell cell = arcCells.get(connects[i]);
//						if (cell != null) validConns.add(cell);
////                        for (int k = 0; k < aList.length; k++) {
////                            if (aList[k].name.equals(connects[i].getName())) {
////                                break;
////                            }
////                        }
//					}
//					int meaning = 0;
//					if (validConns.size() > 0)
//					{
//						CellId [] aplist = new CellId[validConns.size()];
//						for(int i=0; i<validConns.size(); i++)
//						{
//                            Cell cell = validConns.get(i);
//							aplist[i] = cell.getId();
//							String arcName = cell.getName().substring(4);
//							for (int k = 0; k < aList.length; k++)
//							{
//								if (aList[k].name.equals(arcName))
//								{
//									if (aList[k].func.isDiffusion()) meaning = 2; else
//										if (aList[k].func.isPoly()) meaning = 1;
//									break;
//								}
//							}
//                        }
//						pNi.newVar(Info.CONNECTION_KEY, aplist);
//					}
//
//					// add in gate/gated factor for transistors
//					if (pnp.getFunction().isTransistor())
//					{
//						pNi.newVar(Info.PORTMEANING_KEY, new Integer(meaning));
//					}
//
//					// connect the connected ports
//					for(Iterator<PortProto> oPIt = pnp.getPorts(); oPIt.hasNext(); )
//					{
//						PrimitivePort opp = (PrimitivePort)oPIt.next();
//						if (opp == pp) break;
//						if (opp.getTopology() != pp.getTopology()) continue;
//						NodeInst nni = portNodes.get(opp);
//						if (nni == null) continue;
//						PortInst head = nni.getOnlyPortInst();
//						PortInst tail = pNi.getOnlyPortInst();
//						ArcInst.newInstanceBase(Generic.tech().universal_arc, 0, head, tail);
//						break;
//					}
//				}
                oNi.kill();
			}

			// generate the parameterized variations if there are any
	        PrimitiveNodeGroup primitiveNodeGroup = pnp.getPrimitiveNodeGroup();
	        if (primitiveNodeGroup != null)
	        {
	        	int yOffset = 0;
	        	for(int k=1; k<primitiveNodeGroup.getNodes().size(); k++)
				{
					PrimitiveNode altPNp = pnp.getPrimitiveNodeGroup().getNodes().get(k);
					if (nodesToWrite.contains(altPNp)) continue;

					xS = altPNp.getDefWidth() * 2;
					yS = altPNp.getDefHeight() * 2;
					yOffset++;
					Point2D nPos = new Point2D.Double(nodeXPos + xS*5, -5 - yS*(yOffset*2-3));

					xS = altPNp.getDefWidth();
					yS = altPNp.getDefHeight();
	                NodeInst oNi = NodeInst.makeInstance(altPNp, EPoint.snap(nPos), xS, yS, dummyCell);
					Poly [] polys = tech.getShapeOfNode(oNi);
					int j = polys.length;
					NodeInst centerNI = null;
					for(int i=0; i<j; i++)
					{
						Poly poly = polys[i];
						Layer nodeLayer = poly.getLayer().getNonPseudoLayer();
						if (nodeLayer == null) continue;
						EGraphics desc = gp.getGraphics(nodeLayer);

						// create the node to describe this layer
						List<NodeInst> placedNodes = placeGeometry(poly, nNp);
						if (placedNodes == null)
						{
	                        System.out.println("Error placing geometry " + poly.getStyle() + " on " + nNp);
	                        continue;
	                    }
						for(NodeInst ni : placedNodes)
						{
							if (nodeLayer.getFunction().isContact()) centerNI = ni;
							if (centerNI == null && ni.getAnchorCenterX() == nPos.getX() && ni.getAnchorCenterY() == nPos.getY())
								centerNI = ni;

							// get graphics for this layer
							Manipulate.setPatch(ni, desc);
							Cell layerCell = layerCells.get(nodeLayer);
							if (layerCell != null) ni.newVar(Info.LAYER_KEY, layerCell.getId());
							ni.newVar(Info.OPTION_KEY, new Integer(Info.LAYERPATCH));
						}
					}
					if (centerNI != null)
					{
						centerNI.setName(altPNp.getName());
						TextDescriptor td = centerNI.getTextDescriptor(NodeInst.NODE_NAME).withOff(0,
							-altPNp.getFactoryDefaultLambdaBaseHeight()*1.5);
						centerNI.setTextDescriptor(NodeInst.NODE_NAME, td);
					}

					// create the highlight node
					Point2D loc = new Point2D.Double(nPos.getX() + (so.getLowXOffset() - so.getHighXOffset())/2,
						nPos.getY() + (so.getLowYOffset() - so.getHighYOffset())/2);
					xS = altPNp.getDefWidth() - so.getLowXOffset() - so.getHighXOffset();
					yS = altPNp.getDefHeight() - so.getLowYOffset() - so.getHighYOffset();
					NodeInst ni = NodeInst.makeInstance(Artwork.tech().boxNode, loc, xS, yS, nNp);
					if (ni == null) return null;
					ni.newVar(Artwork.ART_COLOR, new Integer(EGraphics.makeIndex(Color.WHITE)));
					ni.newVar(Info.OPTION_KEY, new Integer(Info.HIGHLIGHTOBJ));

					// also draw ports
					if (addPortsToPrimitive(pnp, oNi, tech, arcCells, aList, nNp)) return null;
	                oNi.kill();
				}
			}

			// compact it accordingly
			NodeInfo.compactCell(nNp);
		}
        dummyCell.kill();

		// save the node sequence
        String [] nodeSequenceArray = new String[nodeSequence.size()];
        for(int i=0; i<nodeSequence.size(); i++)
        	nodeSequenceArray[i] = nodeSequence.get(i);
		lib.newVar(Info.NODESEQUENCE_KEY, nodeSequenceArray);

//		// create the design rule information
//		rules = dr_allocaterules(layerTotal, nodeTotal, tech.techname);
//		if (rules == NODRCRULES) return(NOLIBRARY);
//		for(i=0; i<layerTotal; i++)
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

        gi.menuPalette = tech.getFactoryMenuPalette();

//        String techName = tech.getTechName();
//        LibToTech.writeXml(techName + ".xml", techName, gi, lList, nList, aList);

		// clean up
		System.out.println("Done.");
		return(lib);
	}

	private static boolean addPortsToPrimitive(PrimitiveNode pnp, NodeInst oNi, Technology tech, Map<ArcProto,Cell> arcCells,
		ArcInfo[] aList, Cell nNp)
	{
		Map<PrimitivePort,NodeInst> portNodes = new HashMap<PrimitivePort,NodeInst>();
		for(Iterator<PortProto> pIt = pnp.getPorts(); pIt.hasNext(); )
		{
			PrimitivePort pp = (PrimitivePort)pIt.next();
			Poly poly = tech.getShapeOfPort(oNi, pp);
			SizeOffset pSo = Generic.tech().portNode.getProtoSizeOffset();
			double width = poly.getBounds2D().getWidth() + pSo.getLowXOffset() + pSo.getHighXOffset();
			double height = poly.getBounds2D().getHeight() + pSo.getLowYOffset() + pSo.getHighYOffset();
			NodeInst pNi = NodeInst.makeInstance(Generic.tech().portNode, new Point2D.Double(poly.getCenterX(), poly.getCenterY()),
				width, height, nNp);
			if (pNi == null) return true;
			portNodes.put(pp, pNi);
			pNi.newVar(Info.OPTION_KEY, new Integer(Info.LAYERPATCH));
			pNi.newDisplayVar(Info.PORTNAME_KEY, pp.getName());

			// on the first sample, also show angle and connection
			if (pp.getAngle() != 0 || pp.getAngleRange() != 180)
			{
				pNi.newVar(Info.PORTANGLE_KEY, new Integer(pp.getAngle()));
				pNi.newVar(Info.PORTRANGE_KEY, new Integer(pp.getAngleRange()));
			}

			// add in the "local" port connections (from this tech)
			ArcProto [] connects = pp.getConnections();
			List<Cell> validConns = new ArrayList<Cell>();
			for(int i=0; i<connects.length; i++)
			{
				if (connects[i].getTechnology() != tech) continue;
				Cell cell = arcCells.get(connects[i]);
				if (cell != null) validConns.add(cell);
			}
			int meaning = 0;
			if (validConns.size() > 0)
			{
				CellId [] aplist = new CellId[validConns.size()];
				for(int i=0; i<validConns.size(); i++)
				{
	                Cell cell = validConns.get(i);
					aplist[i] = cell.getId();
					String arcName = cell.getName().substring(4);
					for (int l = 0; l < aList.length; l++)
					{
						if (aList[l].name.equals(arcName))
						{
							if (aList[l].func.isDiffusion()) meaning = 2; else
								if (aList[l].func.isPoly()) meaning = 1;
							break;
						}
					}
	            }
				pNi.newVar(Info.CONNECTION_KEY, aplist);
			}

			// add in gate/gated factor for transistors
			if (pnp.getFunction().isTransistor())
			{
				pNi.newVar(Info.PORTMEANING_KEY, new Integer(meaning));
			}

			// connect the connected ports
			for(Iterator<PortProto> oPIt = pnp.getPorts(); oPIt.hasNext(); )
			{
				PrimitivePort opp = (PrimitivePort)oPIt.next();
				if (opp == pp) break;
				if (opp.getTopology() != pp.getTopology()) continue;
				NodeInst nni = portNodes.get(opp);
				if (nni == null) continue;
				PortInst head = nni.getOnlyPortInst();
				PortInst tail = pNi.getOnlyPortInst();
				ArcInst.newInstanceBase(Generic.tech().universal_arc, 0, head, tail);
				break;
			}
		}
		return false;
	}

	private static ArcInfo makeArcInfo(ArcProto ap, LayerInfo[] lList)
    {
        ArcInfo aIn = new ArcInfo();
        ImmutableArcInst defA = ap.getFactoryDefaultInst();
        aIn.name = ap.getName();
        aIn.func = ap.getFunction();
        aIn.widthOffset = ap.getLambdaElibWidthOffset();
        aIn.fixAng = defA.isFixedAngle();
        aIn.wipes = ap.isWipable();
        aIn.noExtend = !defA.isTailExtended();
        aIn.curvable = ap.isCurvable();
        aIn.special = ap.isSpecialArc();
        aIn.notUsed = ap.isNotUsed();
        aIn.skipSizeInPalette = ap.isSkipSizeInPalette();
        aIn.slidable = defA.isSlidable();
        aIn.angInc = ap.getFactoryAngleIncrement();
        aIn.antennaRatio = ap.getFactoryAntennaRatio();
        aIn.arcDetails = new ArcInfo.LayerDetails[ap.getNumArcLayers()];
        for(int i=0; i<aIn.arcDetails.length; i++) {
            ArcInfo.LayerDetails ald = new ArcInfo.LayerDetails();
            aIn.arcDetails[i] = ald;
            String layerName = ap.getLayer(i).getName();
            for(int j=0; j<lList.length; j++) {
                if (lList[j].name.equals(layerName)) { ald.layer = lList[j];   break; }
            }
            ald.style = ap.getLayerStyle(i);
            ald.width = ap.getLayerGridExtend(i);
        }
        return aIn;
    }

    private static NodeInfo makeNodeInfo(PrimitiveNode pnp, LayerInfo[] lList, ArcInfo[] aList)
    {
        Technology tech = pnp.getTechnology();
        NodeInfo nIn = new NodeInfo();
        nIn.name = pnp.getName();
        nIn.func = pnp.getFunction();
        nIn.serp = false;
        if (nIn.func.isFET() && pnp.isHoldsOutline()) nIn.serp = true;
        nIn.arcsShrink = pnp.isArcsShrink();
        assert pnp.isArcsWipe() == nIn.arcsShrink;
        nIn.square = pnp.isSquare();
        assert pnp.isHoldsOutline() == (pnp.getSpecialType() == PrimitiveNode.POLYGONAL || pnp.getSpecialType() == PrimitiveNode.SERPTRANS);
        nIn.canBeZeroSize = pnp.isCanBeZeroSize();
        nIn.wipes = pnp.isWipeOn1or2();
        nIn.lockable = pnp.isLockedPrim();
        nIn.edgeSelect = pnp.isEdgeSelect();
        nIn.skipSizeInPalette = pnp.isSkipSizeInPalette();
        nIn.notUsed = pnp.isNotUsed();
        nIn.lowVt = pnp.isNodeBitOn(PrimitiveNode.LOWVTBIT);
        nIn.highVt = pnp.isNodeBitOn(PrimitiveNode.HIGHVTBIT);
        nIn.nativeBit = pnp.isNodeBitOn(PrimitiveNode.NATIVEBIT);
        nIn.od18 = pnp.isNodeBitOn(PrimitiveNode.OD18BIT);
        nIn.od25 = pnp.isNodeBitOn(PrimitiveNode.OD25BIT);
        nIn.od33 = pnp.isNodeBitOn(PrimitiveNode.OD33BIT);
        nIn.xSize = pnp.getDefWidth();
        nIn.ySize = pnp.getDefHeight();
        nIn.so = pnp.getProtoSizeOffset();
        if (nIn != null && nIn.so.getLowXOffset() == 0 && nIn.so.getHighXOffset() == 0 && nIn.so.getLowYOffset() == 0 && nIn.so.getHighYOffset() == 0)
            nIn.so = null;
        nIn.nodeSizeRule = pnp.getMinSizeRule();
        nIn.autoGrowth = pnp.getAutoGrowth();
        nIn.specialType = pnp.getSpecialType();
        nIn.specialValues = pnp.getSpecialValues();
        nIn.spiceTemplate = pnp.getSpiceTemplate();

        List<Technology.NodeLayer> nodeLayers = new ArrayList<Technology.NodeLayer>();
        for(Technology.NodeLayer nld : pnp.getNodeLayers())
        {
//        	BitSet bs = nld.getParamBitset();
//        	if (bs != null && !bs.get(paramValue)) continue;
        	nodeLayers.add(nld);
        }
        List<Technology.NodeLayer> electricalNodeLayers = nodeLayers;
        if (pnp.getElectricalLayers() != null)
        {
            electricalNodeLayers = new ArrayList<Technology.NodeLayer>();
            for(Technology.NodeLayer nld : pnp.getElectricalLayers())
            {
//            	BitSet bs = nld.getParamBitset();
//            	if (bs != null && !bs.get(paramValue)) continue;
            	electricalNodeLayers.add(nld);
            }
        }
        List<NodeInfo.LayerDetails> layerDetails = new ArrayList<NodeInfo.LayerDetails>();
        EPoint correction = EPoint.fromGrid(pnp.getFullRectangle().getGridWidth(), pnp.getFullRectangle().getGridHeight());
        int m = 0;
        for (Technology.NodeLayer nld: electricalNodeLayers)
        {
            int j = nodeLayers.indexOf(nld);
            if (j < 0)
            {
                layerDetails.add(makeNodeLayerDetails(nld, lList, correction, false, true));
                continue;
            }
            while (m < j)
                layerDetails.add(makeNodeLayerDetails(nodeLayers.get(m++), lList, correction, true, false));
            layerDetails.add(makeNodeLayerDetails(nodeLayers.get(m++), lList, correction, true, true));
        }
        while (m < nodeLayers.size())
            layerDetails.add(makeNodeLayerDetails(nodeLayers.get(m++), lList, correction, true, false));
        nIn.nodeLayers = layerDetails.toArray(new NodeInfo.LayerDetails[layerDetails.size()]);

        nIn.nodePortDetails = new NodeInfo.PortDetails[pnp.getNumPorts()];
        for (int i = 0; i < nIn.nodePortDetails.length; i++) {
            PrimitivePort pp = pnp.getPort(i);
            NodeInfo.PortDetails pd = new NodeInfo.PortDetails();
            nIn.nodePortDetails[i] = pd;
            pd.name = pp.getName();
            pd.netIndex = pp.getTopology();
            pd.angle = pp.getAngle();
            pd.range = pp.getAngleRange();
            EdgeH left = new EdgeH(pp.getLeft().getMultiplier(), pp.getLeft().getAdder() - pp.getLeft().getMultiplier() * nIn.xSize);
            EdgeH right = new EdgeH(pp.getRight().getMultiplier(), pp.getRight().getAdder() - pp.getRight().getMultiplier() * nIn.xSize);
            EdgeV bottom = new EdgeV(pp.getBottom().getMultiplier(), pp.getBottom().getAdder() - pp.getBottom().getMultiplier() * nIn.ySize);
            EdgeV top = new EdgeV(pp.getTop().getMultiplier(), pp.getTop().getAdder() - pp.getTop().getMultiplier() * nIn.ySize);
            pd.values = new Technology.TechPoint[] {
                new Technology.TechPoint(left, bottom),
                new Technology.TechPoint(right, top)};
            pd.characterisitic = pp.getCharacteristic();
            pd.isolated = pp.isIsolated();
            pd.negatable = pp.isNegatable();

            ArcProto [] connects = pp.getConnections();
            List<ArcInfo> validArcInfoConns = new ArrayList<ArcInfo>();
            for(int j=0; j<connects.length; j++) {
                ArcProto ap = connects[j];
                if (ap.getTechnology() != tech) continue;
                for (int k = 0; k < aList.length; k++) {
                    if (aList[k].name.equals(ap.getName())) {
                        validArcInfoConns.add(aList[k]);
                        break;
                    }
                }
            }
            pd.connections = validArcInfoConns.toArray(new ArcInfo[validArcInfoConns.size()]);
        }
        if (nIn.func == PrimitiveNode.Function.NODE) {
            assert nIn.nodeLayers.length == 1;
            LayerInfo l = nIn.nodeLayers[0].layer;
            if (l.pureLayerNode != null)
            	System.out.println("Warning: technology has two pure-layer nodes for layer " + l.name + ": " +
            		l.pureLayerNode.name + " and " + nIn.name);
//            assert l.pureLayerNode == null;
            l.pureLayerNode = nIn;
        }
        return nIn;
    }

    private static NodeInfo.LayerDetails makeNodeLayerDetails(Technology.NodeLayer nl, LayerInfo[] lList, EPoint correction, boolean inLayers, boolean inElectricalLayers) {
        NodeInfo.LayerDetails nld = new NodeInfo.LayerDetails();
        nld.inLayers = inLayers;
        nld.inElectricalLayers = inElectricalLayers;
        nld.style = nl.getStyle();
        nld.portIndex = nl.getPortNum();
        nld.representation = nl.getRepresentation();
        nld.values = nl.getPoints().clone();
        for (int k = 0; k < nld.values.length; k++) {
            Technology.TechPoint p = nld.values[k];
            EdgeH x = p.getX();
            x = x.withAdder(x.getAdder() - x.getMultiplier() * correction.getLambdaX());
            EdgeV y = p.getY();
            y = y.withAdder(y.getAdder() - y.getMultiplier() * correction.getLambdaY());
            nld.values[k] = p.withX(x).withY(y);
        }
        for(int k=0; k<lList.length; k++) {
            if (nl.getLayer().getNonPseudoLayer().getName().equals(lList[k].name)) { nld.layer = lList[k];   break; }
        }
        nld.multiCut = nld.representation == Technology.NodeLayer.MULTICUTBOX;
        nld.multiXS = nl.getMulticutSizeX();
        nld.multiYS = nl.getMulticutSizeY();
        nld.multiSep = nl.getMulticutSep1D();
        nld.multiSep2D = nl.getMulticutSep2D();
        return nld;
    }

	private static List<NodeInst> placeGeometry(Poly poly, Cell cell)
	{
		List<NodeInst> placedNodes = new ArrayList<NodeInst>();
		Rectangle2D box = poly.getBox();
		Rectangle2D bounds = poly.getBounds2D();
		Poly.Type style = poly.getStyle();
		if (style == Poly.Type.FILLED)
		{
			if (box != null)
			{
				NodeInst ni = NodeInst.makeInstance(Artwork.tech().filledBoxNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
					box.getWidth(), box.getHeight(), cell);
				if (ni == null) return null;
				placedNodes.add(ni);
			} else
			{
				NodeInst ni = NodeInst.makeInstance(Artwork.tech().filledPolygonNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
					bounds.getWidth(), bounds.getHeight(), cell);
				if (ni == null) return null;
				ni.setTrace(poly.getPoints());
				placedNodes.add(ni);
			}
			return placedNodes;
		}
		if (style == Poly.Type.CLOSED)
		{
			if (box != null)
			{
				NodeInst ni = NodeInst.makeInstance(Artwork.tech().boxNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
					box.getWidth(), box.getHeight(), cell);
				if (ni == null) return null;
				placedNodes.add(ni);
			} else
			{
				NodeInst ni = NodeInst.makeInstance(Artwork.tech().closedPolygonNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
					bounds.getWidth(), bounds.getHeight(), cell);
				if (ni == null) return null;
				ni.setTrace(poly.getPoints());
				placedNodes.add(ni);
			}
			return placedNodes;
		}
		if (style == Poly.Type.CROSSED)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech().crossedBoxNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			placedNodes.add(ni);
			return placedNodes;
		}
		if (style == Poly.Type.OPENED)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech().openedPolygonNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.setTrace(poly.getPoints());
			placedNodes.add(ni);
			return placedNodes;
		}
		if (style == Poly.Type.OPENEDT1)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech().openedDottedPolygonNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.setTrace(poly.getPoints());
			placedNodes.add(ni);
			return placedNodes;
		}
		if (style == Poly.Type.OPENEDT2)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech().openedDashedPolygonNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.setTrace(poly.getPoints());
			placedNodes.add(ni);
			return placedNodes;
		}
		if (style == Poly.Type.OPENEDT3)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech().openedThickerPolygonNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.setTrace(poly.getPoints());
			placedNodes.add(ni);
			return placedNodes;
		}
		if (style == Poly.Type.VECTORS)
		{
			Point2D [] points = poly.getPoints();
			for(int i=0; i<points.length; i += 2)
			{
				double lX = Math.min(points[i].getX(), points[i+1].getX());
				double hX = Math.max(points[i].getX(), points[i+1].getX());
				double lY = Math.min(points[i].getY(), points[i+1].getY());
				double hY = Math.max(points[i].getY(), points[i+1].getY());
				NodeInst ni = NodeInst.makeInstance(Artwork.tech().openedPolygonNode, new Point2D.Double((lX+hX)/2, (lY+hY)/2),
					hX-lX, hY-lY, cell);
				if (ni == null) return null;
				Point2D [] line = new Point2D[]{ points[i], points[i+1]};
				ni.setTrace(line);
				placedNodes.add(ni);
			}
			return placedNodes;
		}
		if (style == Poly.Type.CIRCLE)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech().circleNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			placedNodes.add(ni);
			return placedNodes;
		}
		if (style == Poly.Type.THICKCIRCLE)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech().thickCircleNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			placedNodes.add(ni);
			return placedNodes;
		}
		if (style == Poly.Type.DISC)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech().filledCircleNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			placedNodes.add(ni);
			return placedNodes;
		}
		if (style == Poly.Type.CIRCLEARC)
		{
			Point2D [] points = poly.getPoints();
			Point2D center = points[0];
			double radius = points[1].distance(center);
			double startAngle = GenMath.figureAngle(center, points[2]) / 10;
			double endAngle = GenMath.figureAngle(center, points[1]) / 10;
			double amt;
			if (startAngle > endAngle) amt = endAngle - startAngle + 360; else
				amt = endAngle - startAngle;
			NodeInst ni = NodeInst.makeInstance(Artwork.tech().circleNode, center, radius*2, radius*2, cell);
			if (ni == null) return null;
			ni.setArcDegrees(startAngle/180.0*Math.PI, amt/180.0*Math.PI);
			placedNodes.add(ni);
			return placedNodes;
		}
		if (style == Poly.Type.THICKCIRCLEARC)
		{
			Point2D [] points = poly.getPoints();
			Point2D center = points[0];
			double radius = points[1].distance(center);
			double startAngle = GenMath.figureAngle(center, points[2]) / 10;
			double endAngle = GenMath.figureAngle(center, points[1]) / 10;
			double amt;
			if (startAngle > endAngle) amt = endAngle - startAngle + 360; else
				amt = endAngle - startAngle;
			NodeInst ni = NodeInst.makeInstance(Artwork.tech().thickCircleNode, center, radius*2, radius*2, cell);
			if (ni == null) return null;
			ni.setArcDegrees(startAngle/180.0*Math.PI, amt/180.0*Math.PI);
			placedNodes.add(ni);
			return placedNodes;
		}
		if (style == Poly.Type.TEXTCENT)
		{
			NodeInst ni = NodeInst.makeInstance(Generic.tech().invisiblePinNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.newVar(Artwork.ART_MESSAGE, poly.getString(), TextDescriptor.getNodeTextDescriptor().withPos(TextDescriptor.Position.CENT));
			placedNodes.add(ni);
			return placedNodes;
		}
		if (style == Poly.Type.TEXTBOTLEFT)
		{
			NodeInst ni = NodeInst.makeInstance(Generic.tech().invisiblePinNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.newVar(Artwork.ART_MESSAGE, poly.getString(), TextDescriptor.getNodeTextDescriptor().withPos(TextDescriptor.Position.UPRIGHT));
			placedNodes.add(ni);
			return placedNodes;
		}
		if (style == Poly.Type.TEXTBOTRIGHT)
		{
			NodeInst ni = NodeInst.makeInstance(Generic.tech().invisiblePinNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.newVar(Artwork.ART_MESSAGE, poly.getString(), TextDescriptor.getNodeTextDescriptor().withPos(TextDescriptor.Position.UPLEFT));
			placedNodes.add(ni);
			return placedNodes;
		}
		if (style == Poly.Type.TEXTBOX)
		{
			NodeInst ni = NodeInst.makeInstance(Generic.tech().invisiblePinNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.newVar(Artwork.ART_MESSAGE, poly.getString(), TextDescriptor.getNodeTextDescriptor().withPos(TextDescriptor.Position.BOXED));
			placedNodes.add(ni);
			return placedNodes;
		}
		return null;
	}
}
