/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ERCAntenna.java
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
package com.sun.electric.tool.erc;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.geometry.PolyMerge;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.TransistorSize;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * This is the Antenna checker of the Electrical Rule Checker tool.
 * <P>
 * Antenna rules are required by some IC manufacturers to ensure that the transistors of the
 * chip are not destroyed during fabrication.  This is because, during fabrication, the wafer is
 * bombarded with ions while making the polysilicon and metal layers.
 * These ions must find a path to through the wafer (to the substrate and active layers at the bottom).
 * If there is a large area of poly or metal, and if it connects ONLY to gates of transistors
 * (not to source or drain or any other active material) then these ions will travel through
 * the transistors.  If the ratio of the poly or metal "sidewall area" to the transistor gate area
 * is too large, the transistors will be destroyed.  The "sidewall area" is the area of the sides
 * of the poly or metal wires, so it is the perimeter times the thickness.
 *<P>
 * Things to do:
 *   Have errors show the gates;
 *   Not all active connections excuse the area trouble...they should be part of the ratio formula
 */
public class ERCAntenna
{
	private static class AntennaObject
	{
		/** the object */							Geometric   geom;
		/** the depth of hierarchy at this node */	int         depth;
		/** end of arc to walk along */				int         otherend;
		/** the hierarchical stack at this node */	NodeInst [] hierstack;

		AntennaObject(Geometric geom) { this.geom = geom; }

		/**
		 * Method to load antenna object "ao" with the hierarchical stack in "stack" that is
		 * "depth" deep.
		 */
		private void loadAntennaObject(NodeInst [] stack, int depth)
		{
			hierstack = new NodeInst[depth];
			for(int i=0; i<depth; i++) hierstack[i] = stack[i];
		}
	};
	
	/** default maximum ratio of poly to gate area */		public static final double DEFPOLYRATIO  = 200;
	/** default maximum ratio of metal to gate area */		public static final double DEFMETALRATIO = 400;
	/** default poly thickness for side-area */				public static final double DEFPOLYTHICKNESS  = 2;
	/** default metal thickness for side-area */			public static final double DEFMETALTHICKNESS = 5.7;

	/** nothing found on the path */						private static final int ERCANTPATHNULL   = 0;
	/** found a gate on the path */							private static final int ERCANTPATHGATE   = 1;
	/** found active on the path */							private static final int ERCANTPATHACTIVE = 2;
	/** search was aborted */								private static final int ERCABORTED       = 3;

	/** head of linked list of antenna objects to spread */	private List<AntennaObject>     firstSpreadAntennaObj;
	/** current technology being considered */				private Technology              curTech;	
	/** top-level cell being checked */						private Cell                    topCell;
	/** accumulated gate area */							private double                  totalGateArea;
	/** the worst ratio found */							private double                  worstRatio;
	/** A list of AntennaObjects to process. */				private List<AntennaObject>     pathList;
	/** Map from ArcProtos to Layers. */					private HashMap<ArcProto,Layer> arcProtoToLayer;
	/** Map from Layers to ArcProtos. */					private HashMap<Layer,ArcProto> layerToArcProto;
	/** Map for marking ArcInsts and NodeInsts. */			private HashSet<Geometric>      fsGeom;
	/** Map for marking Cells. */							private HashSet<Cell>           fsCell;
    /** for storing errors */                               private ErrorLogger             errorLogger;

	/************************ CONTROL ***********************/

	private ERCAntenna() {}

	/**
	 * The main entrypoint for Antenna checking.
	 * Creating an ERCAntenna object checks the current cell.
	 */
	public static void doAntennaCheck()
	{
		UserInterface ui = Job.getUserInterface();
		Cell cell = ui.needCurrentCell();
		if (cell == null) return;
		new AntennaCheckJob(cell);
	}

	/**
	 * Class to do antenna checking in a new thread.
	 */
	private static class AntennaCheckJob extends Job
	{
		private Cell cell;

        private AntennaCheckJob(Cell cell)
		{
			super("ERC Antenna Check", ERC.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			ERCAntenna handler = new ERCAntenna();
			handler.topCell = cell;
			handler.doCheck(this);
			return true;
		}
	}

	/**
	 * Method to do the Antenna check.
	 */
	private void doCheck(Job job)
	{
		curTech = topCell.getTechnology();

		// maps for marking nodes and arcs, and also for marking cells
		fsGeom = new HashSet<Geometric>();
		fsCell = new HashSet<Cell>();

		// create mappings between ArcProtos and Layers
		arcProtoToLayer = new HashMap<ArcProto,Layer>();
		layerToArcProto = new HashMap<Layer,ArcProto>();
		for(Iterator<ArcProto> it = curTech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			ArcProto.Function aFun = ap.getFunction();
			if (!aFun.isMetal() && aFun != ArcProto.Function.POLY1) continue;
			for(Iterator<Layer> lIt = curTech.getLayers(); lIt.hasNext(); )
			{
				Layer lay = lIt.next();
				Layer.Function lFun = lay.getFunction();
				if ((aFun.isMetal() && lFun.isMetal() && aFun.getLevel() == lFun.getLevel()) ||
					(aFun.isPoly() && lFun.isPoly() && aFun.getLevel() == lFun.getLevel()))
				{
					arcProtoToLayer.put(ap, lay);
					layerToArcProto.put(lay, ap);
					break;
				}
			}
		}

		// initialize error logging
		long startTime = System.currentTimeMillis();
		errorLogger = ErrorLogger.newInstance("ERC Antella Rules Check");

		// now check each layer of the cell
		int lasterrorcount = 0;
		worstRatio = 0;
		for(Layer lay : layerToArcProto.keySet())
		{
			System.out.println("Checking Antenna rules for " + lay.getName() + "...");

			// clear timestamps on all cells
			fsCell.clear();

			// do the check for this level
			if (checkThisCell(topCell, lay, job)) break;
			int i = errorLogger.getNumErrors();
			if (i != lasterrorcount)
			{
				System.out.println("  Found " + (i - lasterrorcount) + " errors");
				lasterrorcount = i;
			}
		}

		long endTime = System.currentTimeMillis();
		int errorCount = errorLogger.getNumErrors();
		if (errorCount == 0)
		{
			System.out.println("No antenna errors found (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
		} else
		{
			System.out.println("FOUND " + errorCount + " ANTENNA ERRORS (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
		}
		errorLogger.termLogging(true);
		fsGeom = null;
		fsCell = null;
	}

	/**
	 * Method to check the contents of a cell.
	 * @param cell the Cell to check.
	 * @param lay the Layer to check in the Cell.
	 * @return true if aborted
	 */
	private boolean checkThisCell(Cell cell, Layer lay, Job job)
	{
		// examine every node and follow all relevant arcs
		fsGeom.clear();

		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			if (job.checkAbort()) return true;

			NodeInst ni = it.next();
			if (fsGeom.contains(ni)) continue;
			fsGeom.add(ni);

			// check every connection on the node
			for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
			{
				PortInst pi = pIt.next();

				// ignore if an arc on this port is already seen
				boolean seen = false;
				for(Iterator<Connection> cIt = pi.getConnections(); cIt.hasNext(); )
				{
					Connection con = cIt.next();
					ArcInst ai = con.getArc();
					if (fsGeom.contains(ai)) { seen = true;   break; }
				}
//				for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
//				{
//					Connection con = cIt.next();
//					ArcInst ai = con.getArc();
//					if (con.getPortInst() == pi && fsGeom.contains(ai)) { seen = true;   break; }
//				}
				if (seen) continue;

				totalGateArea = 0.0;
				pathList = new ArrayList<AntennaObject>();

				int found = followNode(ni, pi.getPortProto(), lay, DBMath.MATID, job);
				if (found == ERCABORTED) return true;
				if (found == ERCANTPATHGATE)
				{
					// gather the geometry here
					PolyMerge vmerge = null;
					for(AntennaObject ao : pathList)
					{
						if (ao.geom instanceof NodeInst)
						{
							NodeInst oni = (NodeInst)ao.geom;
							AffineTransform trans = oni.rotateOut();
							for(int i = ao.depth-1; i >= 0; i--)
							{
								AffineTransform tTrans = ao.hierstack[i].translateOut();
								trans.concatenate(tTrans);
								AffineTransform rTrans = ao.hierstack[i].rotateOut();
								trans.concatenate(rTrans);
							}

							Technology tech = oni.getProto().getTechnology();
							if (tech != curTech) continue;
							Poly [] polyList = tech.getShapeOfNode(oni);
							for(int i=0; i<polyList.length; i++)
							{
								Poly poly = polyList[i];
								if (poly.getLayer() != lay) continue;
								if (vmerge == null)
									vmerge = new PolyMerge();
								poly.transform(trans);
								vmerge.addPolygon(poly.getLayer(), poly);
							}
						} else
						{
							ArcInst ai = (ArcInst)ao.geom;
							AffineTransform trans = new AffineTransform();
							for(int i = ao.depth-1; i >= 0; i--)
							{
								AffineTransform tTrans = ao.hierstack[i].translateOut();
								trans.concatenate(tTrans);
								AffineTransform rTrans = ao.hierstack[i].rotateOut();
								trans.concatenate(rTrans);
							}
	
							Technology tech = ai.getProto().getTechnology();
							if (tech != curTech) continue;
							Poly [] polyList = tech.getShapeOfArc(ai);
							for(int i=0; i<polyList.length; i++)
							{
								Poly poly = polyList[i];
								if (poly.getLayer() != lay) continue;
								if (vmerge == null)
									vmerge = new PolyMerge();
								poly.transform(trans);
								vmerge.addPolygon(poly.getLayer(), poly);
							}
						}
					}
					if (vmerge != null)
					{
						// get the area of the antenna
						double totalRegionPerimeterArea = 0.0;
                        for (Layer oLay : vmerge.getKeySet())
						{
							double thickness = oLay.getThickness();
							if (thickness == 0)
							{
								if (oLay.getFunction().isMetal()) thickness = DEFMETALTHICKNESS; else
									if (oLay.getFunction().isPoly()) thickness = DEFPOLYTHICKNESS;
							}
							List<PolyBase> merges = vmerge.getMergedPoints(oLay, true);
							for(PolyBase merged : merges)
							{
								totalRegionPerimeterArea += merged.getPerimeter() * thickness;
							}
						}

						// see if it is an antenna violation
						double ratio = totalRegionPerimeterArea / totalGateArea;
						double neededratio = getAntennaRatio(lay);
						if (ratio > worstRatio) worstRatio = ratio;
						if (ratio >= neededratio)
						{
							// error
							String errMsg = "layer " + lay.getName() + " has perimeter-area " + totalRegionPerimeterArea +
								"; gates have area " + totalGateArea + ", ratio is " + ratio + " but limit is " + neededratio;
							List<PolyBase> polyList = new ArrayList<PolyBase>();
                            for (Layer oLay : vmerge.getKeySet())
							{
								List<PolyBase> merges = vmerge.getMergedPoints(oLay, true);
								for(PolyBase merged : merges)
								{
									polyList.add(merged);
								}
							}
							errorLogger.logError(errMsg, null, null, null, null, polyList, cell, 0);
						}
					}
				}
			}
		}
	
		// now look at subcells
		fsCell.add(cell);
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (!ni.isCellInstance()) continue;
			Cell subCell = (Cell)ni.getProto();
			if (fsCell.contains(subCell)) continue;
	
			if (checkThisCell(subCell, lay, job)) return true;
		}
		return false;
	}
	
	/**
	 * Method to follow a node around the cell.
	 * @param ni the NodeInst to follow.
	 * @param pp the PortProto on the NodeInst.
	 * @param lay the layer to consider.
	 * @param trans a transformation to the top-level.
	 * @return ERCANTPATHNULL if it found no gate or active on the path.
	 * Returns ERCANTPATHGATE if it found gates on the path.
	 * Returns ERCANTPATHACTIVE if it found active on the path.
	 */
	private int followNode(NodeInst ni, PortProto pp, Layer lay, AffineTransform trans, Job job)
	{
		// presume that nothing was found
		int ret = ERCANTPATHNULL;
		firstSpreadAntennaObj = new ArrayList<AntennaObject>();
		NodeInst [] antstack = new NodeInst[200];
		int depth = 0;
	
		// keep walking along the nodes and arcs
		for(;;)
		{
			if (job.checkAbort()) return ERCABORTED;

			// if this is a subcell, recurse on it
			fsGeom.add(ni);
			NodeInst thisni = ni;
			while (thisni.isCellInstance())
			{
				antstack[depth] = thisni;
				depth++;
				thisni = ((Export)pp).getOriginalPort().getNodeInst();
				pp = ((Export)pp).getOriginalPort().getPortProto();
			}
	
			// see if we hit a transistor
			boolean seen = false;
			if (thisni.isFET())
			{
				// stop tracing
				if (thisni.getTransistorDrainPort().getPortProto() == pp ||
					thisni.getTransistorSourcePort().getPortProto() == pp)
				{
					// touching the diffusion side of the transistor
					return ERCANTPATHACTIVE;
				} else
				{
					// touching the gate side of the transistor
					TransistorSize dim = thisni.getTransistorSize(VarContext.globalContext);
					totalGateArea += dim.getDoubleLength() * dim.getDoubleWidth();
					ret = ERCANTPATHGATE;
				}
			} else
			{
				// normal primitive: propagate
				if (hasDiffusion(thisni)) return ERCANTPATHACTIVE;
				AntennaObject ao = new AntennaObject(ni);
	
				if (haveAntennaObject(ao))
				{
					// already in the list: free this object
					seen = true;
				} else
				{
					// not in the list: add it
					ao.loadAntennaObject(antstack, depth);
					addAntennaObject(ao);
				}
			}
	
			// look at all arcs on the node
			if (!seen)
			{
				int found = findArcs(thisni, pp, lay, depth, antstack);
				if (found == ERCANTPATHACTIVE) return found;
				if (depth > 0)
				{
					found = findExports(thisni, pp, lay, depth, antstack);
					if (found == ERCANTPATHACTIVE) return found;
				}
			}
	
			// look for an unspread antenna object and keep walking
			if (firstSpreadAntennaObj.size() == 0) break;
			AntennaObject ao = firstSpreadAntennaObj.get(0);
			firstSpreadAntennaObj.remove(0);
	
			ArcInst ai = (ArcInst)ao.geom;
			ni = ai.getPortInst(ao.otherend).getNodeInst();
			pp = ai.getPortInst(ao.otherend).getPortProto();
			depth = ao.hierstack.length;
			for(int i=0; i<depth; i++)
				antstack[i] = ao.hierstack[i];
		}
		return ret;
	}
	
	/**
	 * Method to tell whether a NodeInst has diffusion on it.
	 * @param ni the NodeInst in question.
	 * @return true if the NodeInst has diffusion on it.
	 */
	private boolean hasDiffusion(NodeInst ni)
	{
		// stop if this is a pin
		if (ni.getFunction() == PrimitiveNode.Function.PIN) return false;
	
		// analyze to see if there is diffusion here
		Technology tech = ni.getProto().getTechnology();
		Poly [] polyList = tech.getShapeOfNode(ni);
		for(int i=0; i<polyList.length; i++)
		{
			Poly poly = polyList[i];
			Layer.Function fun = poly.getLayer().getFunction();
			if (fun.isDiff()) return true;
		}
		return false;
	}

	private int findArcs(NodeInst ni, PortProto pp, Layer lay, int depth, NodeInst [] antstack)
	{
        PortInst pi = ni.findPortInstFromProto(pp);
		for(Iterator<Connection> it = pi.getConnections(); it.hasNext(); )
		{
			Connection con = it.next();
//		for(Iterator it = ni.getConnections(); it.hasNext(); )
//		{
//			Connection con = it.next();
//			PortInst pi = con.getPortInst();
//			if (pi.getPortProto() != pp) continue;
			ArcInst ai = con.getArc();

			// see if it is the desired layer
			if (ai.getProto().getFunction().isDiffusion()) return ERCANTPATHACTIVE;
			Layer aLayer = arcProtoToLayer.get(ai.getProto());
			if (aLayer == null) continue;
			if (ai.getProto().getFunction().isMetal() != aLayer.getFunction().isMetal()) continue;
			if (ai.getProto().getFunction().isPoly() != aLayer.getFunction().isPoly()) continue;
			if (ai.getProto().getFunction().getLevel() > aLayer.getFunction().getLevel()) continue;

			// make an antenna object for this arc
			fsGeom.add(ai);
			AntennaObject ao = new AntennaObject(ai);

			if (haveAntennaObject(ao)) continue;
			ao.loadAntennaObject(antstack, depth);

			int other = 0;
			if (ai.getPortInst(0) == pi) other = 1;
			ao.otherend = other;
			addAntennaObject(ao);

			// add to the list of "unspread" antenna objects
			firstSpreadAntennaObj.add(ao);
		}
		return ERCANTPATHNULL;
	}

	private int findExports(NodeInst ni, PortProto pp, Layer lay, int depth, NodeInst [] antstack)
	{
		depth--;
		for(Iterator<Export> it = ni.getExports(); it.hasNext(); )
		{
			Export e = it.next();
			if (e != pp) continue;

			ni = antstack[depth];
			pp = e;
			int found = findArcs(ni, pp, lay, depth, antstack);
			if (found == ERCANTPATHACTIVE) return found;
			if (depth > 0)
			{
				found = findExports(ni, pp, lay, depth, antstack);
				if (found == ERCANTPATHACTIVE) return found;
			}
		}
		return ERCANTPATHNULL;
	}
	
	/**
	 * Method to tell whether an AntennaObject is in the active list.
	 * @param ao the AntennaObject.
	 * @return true if the AntennaObject is already in the list.
	 */
	private boolean haveAntennaObject(AntennaObject ao)
	{
		for(AntennaObject oAo : pathList)
		{	
			if (oAo.geom == ao.geom && oAo.depth == ao.depth)
			{
				boolean found = true;
				int len = 0;
				if (ao.hierstack != null) len = ao.hierstack.length;
				int oLen = 0;
				if (oAo.hierstack != null) oLen = oAo.hierstack.length;
				if (len != oLen) continue;
				for(int i=0; i<len; i++)
				{
					if (oAo.hierstack[i] != ao.hierstack[i]) { found = false;   break; }
				}
				if (found) return true;
			}
		}
		return false;
	}
	
	/**
	 * Method to add an AntennaObject to the list of antenna objects on this path.
	 * @param ao the AntennaObject to add.
	 */
	private void addAntennaObject(AntennaObject ao)
	{
		pathList.add(ao);
	}

	/**
	 * Method to return the maximum antenna ratio on a given Layer.
	 * @param layer the layer in question.
	 * @return the maximum antenna ratio for the Layer.
	 */
	private double getAntennaRatio(Layer layer)
	{
		// find the ArcProto that corresponds to this layer
		ArcProto ap = layerToArcProto.get(layer);
		if (ap == null) return 0;

		// return its ratio
		return ERC.tool.getAntennaRatio(ap);
        //return ap.getAntennaRatio();
	}

}
