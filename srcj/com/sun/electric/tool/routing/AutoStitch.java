/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AutoStitch.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.routing;

import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.WiringListener;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * This is the Auto Stitching tool.
 */
public class AutoStitch
{
	/** the prefered arc */		static ArcProto preferredArc;
    /** router used to wire */  static InteractiveRouter router = new SimpleWirer();

	/**
	 * Method to do auto-stitching.
	 * @param highlighted true to stitch only the highlighted objects.
	 * False to stitch the entire current cell.
	 */
	public static void autoStitch(boolean highlighted, boolean forced)
	{
		List nodesToStitch = new ArrayList();
		if (highlighted)
		{
			List highs = Highlight.getHighlighted(true, false);
			for(Iterator it = highs.iterator(); it.hasNext(); )
				nodesToStitch.add(it.next());
		} else
		{
			Cell cell = WindowFrame.needCurCell();
			if (cell == null) return;
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (ni.isIconOfParent()) continue;
				nodesToStitch.add(ni);
			}
		}
		if (nodesToStitch.size() > 0)
		{
			AutoStitchJob job = new AutoStitchJob(nodesToStitch, forced);
		}
	}

	/**
	 * Class to change the node/arc type in a new thread.
	 */
	private static class AutoStitchJob extends Job
	{
		List nodesToStitch;
		FlagSet nodeMark;
		boolean forced;

		protected AutoStitchJob(List nodesToStitch, boolean forced)
		{
			super("Auto-Stitch", Routing.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.nodesToStitch = nodesToStitch;
			this.forced = forced;
			startJob();
		}

		public boolean doIt()
		{
			FlagSet cellMark = NodeProto.getFlagSet(1);
			nodeMark = NodeInst.getFlagSet(1);

			// clear flag for finding cells that will be checked
			cellMark.clearOnAllCells();

			// next pre-compute bounds on all nodes in cells to be changed
			int count = 0;
			for(Iterator it = nodesToStitch.iterator(); it.hasNext(); )
			{
				NodeInst nodeToStitch = (NodeInst)it.next();
				Cell parent = nodeToStitch.getParent();
				if (parent.isBit(cellMark)) continue;
				parent.setBit(cellMark);

				for(Iterator nIt = parent.getNodes(); nIt.hasNext(); )
				{
					NodeInst ni = (NodeInst)nIt.next();
					ni.clearBit(nodeMark);

					// count the ports on this node
					int total = ni.getProto().getNumPorts();

					// get memory for bounding box of each port
					double [] bbArray = new double[total*4];
					ni.setTempObj(bbArray);
					int i = 0;
					for(Iterator pIt = ni.getProto().getPorts(); pIt.hasNext(); )
					{
						PortProto pp = (PortProto)pIt.next();
						AffineTransform trans = ni.rotateOut();
						NodeInst rNi = ni;
						PortProto rPp = pp;
						while (rNi.getProto() instanceof Cell)
						{
							AffineTransform temp = rNi.translateOut();
							temp.preConcatenate(trans);
							PortInst subPi = ((Export)rPp).getOriginalPort();
							rPp = subPi.getPortProto();
							rNi = subPi.getNodeInst();
							trans = rNi.rotateOut();
							trans.preConcatenate(temp);
						}
						Rectangle2D bounds = new Rectangle2D.Double(rNi.getAnchorCenterX() - rNi.getXSize()/2, 
							rNi.getAnchorCenterY() - rNi.getYSize()/2, rNi.getXSize(), rNi.getYSize());
						EMath.transformRect(bounds, trans);
						bbArray[i++] = bounds.getMinX();
						bbArray[i++] = bounds.getMaxX();
						bbArray[i++] = bounds.getMinY();
						bbArray[i++] = bounds.getMaxY();
					}
				}
			}

			// next mark nodes to be checked
			for(Iterator it = nodesToStitch.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni.setBit(nodeMark);
			}

			// find out the prefered routing arc
			preferredArc = null;
			String preferredName = Routing.getPreferredRoutingArc();
			if (preferredName.length() > 0) preferredArc = ArcProto.findArcProto(preferredName);
			if (preferredArc == null)
			{
				// see if there is a default user arc
				ArcProto curAp = User.tool.getCurrentArcProto();
				if (curAp != null) preferredArc = curAp;
			}

			// finally, initialize the information about which layer is smallest on each arc
			for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
			{
				Technology tech = (Technology)it.next();
				for(Iterator aIt = tech.getArcs(); aIt.hasNext(); )
				{
					PrimitiveArc ap = (PrimitiveArc)aIt.next();
					ap.setTempObj(null);
				}
			}

			// now run through the nodeinsts to be checked for stitching
			for(Iterator it = nodesToStitch.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (ni.getParent().isAllLocked()) continue;
				count += checkStitching(ni);
			}

			// report results
			if (count != 0)
			{
				System.out.println("AUTO ROUTING: added " + count + " wires");
			} else
			{
				if (forced)
					System.out.println("No arcs added");
			}

			// clean up
			for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
			{
				Technology tech = (Technology)it.next();
				for(Iterator aIt = tech.getArcs(); aIt.hasNext(); )
				{
					PrimitiveArc ap = (PrimitiveArc)aIt.next();
					ap.setTempObj(null);
				}
			}
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					if (!cell.isBit(cellMark)) continue;
					for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
					{
						NodeInst ni = (NodeInst)nIt.next();
						ni.setTempObj(null);
					}
				}
			}
			cellMark.freeFlagSet();
			nodeMark.freeFlagSet();

			return true;
		}

		/*
		 * Method to check NodeInst "ni" for possible stitching to neighboring NodeInsts.
		 */
		private int checkStitching(NodeInst ni)
		{
			Cell cell = ni.getParent();
			Netlist netlist = cell.getUserNetlist();
			double [] boundArray = (double [])ni.getTempObj();

			// gather a list of other nodes that touch or overlap this one
			List nodesInArea = new ArrayList();
			for(Iterator it = cell.searchIterator(ni.getBounds()); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				if (geom instanceof NodeInst) nodesInArea.add(geom);
			}

			int count = 0;
			for(Iterator it = nodesInArea.iterator(); it.hasNext(); )
			{
				// find another node in this area
				NodeInst oNi = (NodeInst)it.next();

				// if both nodes are being checked, examine them only once
				if (oNi.isBit(nodeMark) && oNi.getNodeIndex() <= ni.getNodeIndex()) continue;

				// now look at every layer in this node
				Rectangle2D oBounds = oNi.getBounds();
				if (ni.getProto() instanceof Cell)
				{
					// complex node instance: look at all ports
					int bbp = 0;
					for(Iterator pIt = ni.getProto().getPorts(); pIt.hasNext(); )
					{
						PortProto pp = (PortProto)pIt.next();

						// first do a bounding box check
						if (boundArray != null)
						{
							double lX = boundArray[bbp++];
							double hX = boundArray[bbp++];
							double lY = boundArray[bbp++];
							double hY = boundArray[bbp++];
							if (lX > oBounds.getMaxX() || hX < oBounds.getMinX() ||
								lY > oBounds.getMaxY() || hY < oBounds.getMinY()) continue;
						}

						// stop now if already an arc on this port to other node
						boolean found = false;
						for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
						{
							Connection con = (Connection)cIt.next();
							PortInst pi = con.getPortInst();
							if (pi.getPortProto() != pp) continue;
							if (con.getArc().getHead().getPortInst().getNodeInst() == oNi ||
								con.getArc().getTail().getPortInst().getNodeInst() == oNi) { found = true;   break; }
						}
						if (found) continue;

						// find the primitive node at the bottom of this port
						AffineTransform trans = ni.rotateOut();
						NodeInst rNi = ni;
						PortProto rPp = pp;
						while (rNi.getProto() instanceof Cell)
						{
							AffineTransform temp = rNi.translateOut();
							temp.preConcatenate(trans);
							Export e = (Export)rPp;
							rNi = e.getOriginalPort().getNodeInst();
							rPp = e.getOriginalPort().getPortProto();

							trans = rNi.rotateOut();
							trans.preConcatenate(temp);
						}

						// determine the smallest layer for all possible arcs
						ArcProto [] connections = pp.getBasePort().getConnections();
						for(int i=0; i<connections.length; i++)
						{
							findSmallestLayer(connections[i]);
						}

						// look at all polygons on this nodeinst
						boolean usePortPoly = false;
						Technology tech = rNi.getProto().getTechnology();
						Poly [] nodePolys = tech.getShapeOfNode(rNi, null, true, true);
						int tot = nodePolys.length;
						if (tot == 0 || rNi.getProto() == Generic.tech.simProbeNode)
						{
							usePortPoly = true;
							tot = 1;
						}
						Netlist subNetlist = rNi.getParent().getUserNetlist();
						for(int j=0; j<tot; j++)
						{
							Layer layer = null;
							Poly poly = null;
							if (usePortPoly)
							{
								poly = ni.getShapeOfPort(pp);
								layer = poly.getLayer();
							} else
							{
								poly = nodePolys[j];

								// only want electrically connected polygons
								if (poly.getPort() == null) continue;

								// only want polygons on correct part of this nodeinst
								if (!subNetlist.portsConnected(rNi, rPp, poly.getPort())) continue;

								// transformed polygon
								poly.transform(trans);

								// if the polygon layer is pseudo, substitute real layer
								layer = poly.getLayer();
								if (layer != null) layer = layer.getNonPseudoLayer();
							}

							// see which arc can make the connection
							boolean connected = false;
							for(int pass=0; pass<2; pass++)
							{
								for(int i=0; i<connections.length; i++)
								{
									ArcProto ap = connections[i];
									if (pass == 0)
									{
										if (ap != preferredArc) continue;
									} else
									{
										if (ap == preferredArc) continue;

										// arc must be in the same technology
										if (ap.getTechnology() != rNi.getProto().getTechnology()) continue;
									}

									// this polygon must be the smallest arc layer
									if (!usePortPoly)
									{
										if (!tech.sameLayer((Layer)ap.getTempObj(), layer)) continue;
									}

									// pass it on to the next test
									connected = testPoly(ni, pp, ap, poly, oNi, netlist);
									if (connected) { count++;   break; }
								}
								if (connected) break;
							}
							if (connected) break;
						}
					}
				} else
				{
					// primitive node: check its layers
					AffineTransform trans = ni.rotateOut();

					// save information about the other node
					double oX = oNi.getAnchorCenterX();
					double oY = oNi.getAnchorCenterY();

					// look at all polygons on this nodeinst
					boolean usePortPoly = false;
					Technology tech = ni.getProto().getTechnology();
					Poly [] polys = tech.getShapeOfNode(ni, null, true, true);
					int tot = polys.length;
					if (tot == 0 || ni.getProto() == Generic.tech.simProbeNode)
					{
						usePortPoly = true;
						tot = 1;
					}
					for(int j=0; j<tot; j++)
					{
						PortProto rPp = null;
						Poly polyPtr = null;
						if (usePortPoly)
						{
							// search all ports for the closest
							PortProto bestPp = null;
							double bestDist = 0;
							for(Iterator pIt = ni.getProto().getPorts(); pIt.hasNext(); )
							{
								PortProto tPp = (PortProto)pIt.next();

								// compute best distance to the other node
								Poly portPoly = ni.getShapeOfPort(tPp);
								double x = portPoly.getCenterX();
								double y = portPoly.getCenterY();
								double dist = Math.abs(x-oX) + Math.abs(y-oY);
								if (bestPp == null) bestDist = dist;
								if (dist > bestDist) continue;
								bestPp = rPp;   bestDist = dist;
							}
							if (bestPp == null) continue;
							rPp = bestPp;
							polyPtr = ni.getShapeOfPort(rPp);
						} else
						{
							polyPtr = polys[j];

							// only want electrically connected polygons
							if (polyPtr.getPort() == null) continue;

							// search all ports for the closest connected to this layer
							PortProto bestPp = null;
							double bestDist = 0;
							for(Iterator pIt = ni.getProto().getPorts(); pIt.hasNext(); )
							{
								PortProto tPp = (PortProto)pIt.next();
								if (!netlist.portsConnected(ni, tPp, polyPtr.getPort())) continue;

								// compute best distance to the other node
								Poly portPoly = ni.getShapeOfPort(tPp);
								double x = portPoly.getCenterX();
								double y = portPoly.getCenterY();
								double dist = Math.abs(x-oX) + Math.abs(y-oY);
								if (bestPp == null) bestDist = dist;
								if (dist > bestDist) continue;
								bestPp = tPp;   bestDist = dist;
							}
							if (bestPp == null) continue;
							rPp = bestPp;

							// transformed the polygon
							polyPtr.transform(trans);
						}

						// if the polygon layer is pseudo, substitute real layer
						Layer layer = polyPtr.getLayer();
						if (layer != null) layer = layer.getNonPseudoLayer();

						// stop now if already an arc on this port to other node
						boolean found = false;
						for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
						{
							Connection con = (Connection)cIt.next();
							PortInst pi = con.getPortInst();
							if (!netlist.portsConnected(ni, rPp, pi.getPortProto())) continue;
							if (con.getArc().getHead().getPortInst().getNodeInst() == oNi ||
								con.getArc().getTail().getPortInst().getNodeInst() == oNi) { found = true;   break; }
						}
						if (found) continue;

						// see if an arc is possible
						boolean connected = false;
						ArcProto [] connections = rPp.getBasePort().getConnections();
						for(int pass=0; pass<2; pass++)
						{
							for(int i=0; i<connections.length; i++)
							{
								ArcProto ap = connections[i];
								if (pass == 0)
								{
									if (ap != preferredArc) continue;
								} else
								{
									if (ap == preferredArc) continue;
								}

								// arc must be in the same technology
								if (ap.getTechnology() != ni.getProto().getTechnology()) break;

								// this polygon must be the smallest arc layer
								findSmallestLayer(ap);
								if (!usePortPoly)
								{
									Layer oLayer = (Layer)ap.getTempObj();
									if (!ap.getTechnology().sameLayer(oLayer, layer)) continue;
								}

								// pass it on to the next test
								connected = testPoly(ni, rPp, ap, polyPtr, oNi, netlist);
								if (connected) { count++;   break; }
							}
							if (connected) break;
						}
						if (connected) break;
					}
				}
			}
			return count;
		}

		/*
		 * Method to find exported polygons in node "oNi" that abut with the polygon
		 * in "poly" on the same layer.  When they do, these should be connected to
		 * nodeinst "ni", port "pp" with an arc of type "ap".  Returns the number of
		 * connections made (0 if none).
		 */
		private static boolean testPoly(NodeInst ni, PortProto pp, ArcProto ap, Poly poly, NodeInst oNi, Netlist netlist)
		{
			// get network associated with the node/port
			PortInst pi = ni.findPortInstFromProto(pp);
			JNetwork net = netlist.getNetwork(pi);

			// now look at every layer in this node
			if (oNi.getProto() instanceof Cell)
			{
				// complex cell: look at all exports
				double [] boundArray = (double [])oNi.getTempObj();
				int bbp = 0;
				Rectangle2D bounds = poly.getBounds2D();
				for(Iterator it = oNi.getProto().getPorts(); it.hasNext(); )
				{
					PortProto mPp = (PortProto)it.next();

					// first do a bounding box check
					if (boundArray != null)
					{
						double lX = boundArray[bbp++];
						double hX = boundArray[bbp++];
						double lY = boundArray[bbp++];
						double hY = boundArray[bbp++];
						if (lX > bounds.getMaxX() || hX < bounds.getMinX() ||
							lY > bounds.getMaxY() || hY < bounds.getMinY()) continue;
					}

					// port must be able to connect to the arc
					if (!mPp.getBasePort().connectsTo(ap)) continue;

					// do not stitch where there is already an electrical connection
					JNetwork oNet = netlist.getNetwork(oNi.findPortInstFromProto(mPp));
					if (net != null && oNet == net) continue;

					// find the primitive node at the bottom of this port
					AffineTransform trans = oNi.rotateOut();
					NodeInst rNi = oNi;
					PortProto rPp = mPp;
					while (rNi.getProto() instanceof Cell)
					{
						AffineTransform temp = rNi.translateOut();
						temp.preConcatenate(trans);
						Export e = (Export)rPp;
						rNi = e.getOriginalPort().getNodeInst();
						rPp = e.getOriginalPort().getPortProto();

						trans = rNi.rotateOut();
						trans.preConcatenate(temp);
					}

					// see how much geometry is on this node
					Technology tech = rNi.getProto().getTechnology();
					Poly [] polys = tech.getShapeOfNode(rNi, null, true, true);
					int tot = polys.length;
					if (tot == 0)
					{
						// not a geometric primitive: look for ports that touch
						Poly oPoly = oNi.getShapeOfPort(mPp);
						if (comparePoly(oNi, mPp, oPoly, ni, pp, poly, ap, netlist))
							return true;
					} else
					{
						// a geometric primitive: look for ports on layers that touch
						Netlist subNetlist = rNi.getParent().getUserNetlist();
						for(int j=0; j<tot; j++)
						{
							Poly oPoly = polys[j];

							// only want electrically connected polygons
							if (oPoly.getPort() == null) continue;

							// only want polygons connected to correct part of nodeinst
							if (!subNetlist.portsConnected(rNi, rPp, oPoly.getPort())) continue;

							// if the polygon layer is pseudo, substitute real layer
							if (ni.getProto() != Generic.tech.simProbeNode)
							{
								Layer oLayer = oPoly.getLayer();
								if (oLayer != null) oLayer = oLayer.getNonPseudoLayer();
								if (!tech.sameLayer(oLayer, (Layer)ap.getTempObj())) continue;
							}

							// transform the polygon and pass it on to the next test
							oPoly.transform(trans);
							if (comparePoly(oNi, mPp, oPoly, ni, pp, poly, ap, netlist))
								return true;
						}
					}
				}
			} else
			{
				// primitive node: check its layers
				AffineTransform trans = oNi.rotateOut();

				// determine target point
				double ox = poly.getCenterX();
				double oy = poly.getCenterY();

				// look at all polygons on this nodeinst
				Technology tech = oNi.getProto().getTechnology();
				Poly [] polys = tech.getShapeOfNode(oNi, null, true, true);
				int tot = polys.length;
				if (tot == 0)
				{
					// not a geometric primitive: look for ports that touch
					PortProto bestPp = null;
					double bestDist = 0;
					for(Iterator pIt = oNi.getProto().getPorts(); pIt.hasNext(); )
					{
						PortProto rPp = (PortProto)pIt.next();
						// compute best distance to the other node
						
						Poly portPoly = oNi.getShapeOfPort(rPp);
						double dist = Math.abs(portPoly.getCenterX()-ox) + Math.abs(portPoly.getCenterY()-oy);
						if (bestPp == null) bestDist = dist;
						if (dist > bestDist) continue;
						bestPp = rPp;   bestDist = dist;
					}
					if (bestPp != null)
					{
						PortProto rPp = bestPp;

						// port must be able to connect to the arc
						if (rPp.getBasePort().connectsTo(ap))
						{
							// transformed the polygon and pass it on to the next test
							Poly oPoly = oNi.getShapeOfPort(rPp);
							if (comparePoly(oNi, rPp, oPoly, ni, pp, poly, ap, netlist))
								return true;
						}
					}
				} else
				{
					// a geometric primitive: look for ports on layers that touch
					for(int j=0; j<tot; j++)
					{
						Poly oPoly = polys[j];

						// only want electrically connected polygons
						if (oPoly.getPort() == null) continue;

						// if the polygon layer is pseudo, substitute real layer
						Layer oLayer = oPoly.getLayer();
						if (oLayer != null) oLayer = oLayer.getNonPseudoLayer();

						// this must be the smallest layer on the arc
						if (!tech.sameLayer((Layer)ap.getTempObj(), oLayer)) continue;

						// do not stitch where there is already an electrical connection
						PortInst oPi = oNi.findPortInstFromProto(oPoly.getPort());
						JNetwork oNet = netlist.getNetwork(oPi);
						if (net != null && oNet == net) continue;

						// search all ports for the closest connected to this layer
						PortProto bestPp = null;
						double bestDist = 0;
						for(Iterator pIt = oNi.getProto().getPorts(); pIt.hasNext(); )
						{
							PortProto rPp = (PortProto)pIt.next();
							if (!netlist.portsConnected(oNi, rPp, oPoly.getPort())) continue;

							// compute best distance to the other node
							Poly portPoly = oNi.getShapeOfPort(rPp);
							double dist = Math.abs(ox-portPoly.getCenterX()) + Math.abs(oy-portPoly.getCenterY());
							if (bestPp == null) bestDist = dist;
							if (dist > bestDist) continue;
							bestPp = rPp;   bestDist = dist;
						}
						if (bestPp == null) continue;
						PortProto rPp = bestPp;

						// port must be able to connect to the arc
						if (!rPp.getBasePort().connectsTo(ap)) continue;

						// transformed the polygon and pass it on to the next test
						oPoly.transform(trans);
						if (comparePoly(oNi, rPp, oPoly, ni, pp, poly, ap, netlist))
							return true;
					}
				}
			}
			return false;
		}

		/*
		 * Method to compare polygon "oPoly" from nodeinst "oNi", port "opp" and
		 * polygon "poly" from nodeinst "ni", port "pp".  If these polygons touch
		 * or overlap then the two nodes should be connected with an arc of type
		 * "ap".  If a connection is made, the method returns true, otherwise
		 * it returns false.
		 */
		private static boolean comparePoly(NodeInst oNi, PortProto opp, Poly oPoly, NodeInst ni,
			PortProto pp, Poly poly, ArcProto ap, Netlist netlist)
		{
			// find the bounding boxes of the polygons
			Rectangle2D polyBounds = poly.getBounds2D();
			Rectangle2D oPolyBounds = oPoly.getBounds2D();

			// quit now if bounding boxes don't intersect
			if (polyBounds.getMinX() > oPolyBounds.getMaxX() || oPolyBounds.getMinX() > polyBounds.getMaxX() ||
				polyBounds.getMinY() > oPolyBounds.getMaxY() || oPolyBounds.getMinY() > polyBounds.getMaxY()) return false;

			// be sure the closest ports are being used
			Poly portPoly = ni.getShapeOfPort(pp);
			Point2D portCenter = new Point2D.Double(portPoly.getCenterX(), portPoly.getCenterY());
			portPoly = oNi.getShapeOfPort(opp);
			Point2D oPortCenter = new Point2D.Double(portPoly.getCenterX(), portPoly.getCenterY());

			double dist = portCenter.distance(oPortCenter);
			for(Iterator it = oNi.getProto().getPorts(); it.hasNext(); )
			{
				PortProto tPp = (PortProto)it.next();
				if (tPp == opp) continue;
				if (!netlist.portsConnected(oNi, tPp, opp)) continue;
				portPoly = oNi.getShapeOfPort(tPp);
				Point2D tPortCenter = new Point2D.Double(portPoly.getCenterX(), portPoly.getCenterY());
				double tDist = portCenter.distance(tPortCenter);
				if (tDist >= dist) continue;
				dist = tDist;
				opp = tPp;
				oPortCenter.setLocation(tPortCenter);
			}
			for(Iterator it = ni.getProto().getPorts(); it.hasNext(); )
			{
				PortProto tPp = (PortProto)it.next();
				if (tPp == pp) continue;
				if (!netlist.portsConnected(ni, tPp, pp)) continue;
				portPoly = ni.getShapeOfPort(tPp);
				Point2D tPortCenter = new Point2D.Double(portPoly.getCenterX(), portPoly.getCenterY());
				double tDist = oPortCenter.distance(tPortCenter);
				if (tDist >= dist) continue;
				dist = tDist;
				pp = tPp;
				portCenter.setLocation(tPortCenter);
			}

			// find some dummy position to help run the arc
			double x = (oPortCenter.getX() + portCenter.getX()) / 2;
			double y = (oPortCenter.getY() + portCenter.getY()) / 2;

			// run the wire
			List added = WiringListener.makeConnection(ni, pp, oNi, opp, new Point2D.Double(x,y), true, true);
            PortInst pi = ni.findPortInstFromProto(pp);
            PortInst opi = oNi.findPortInstFromProto(opp);
            Route route = router.planRoute(ni.getParent(), pi, opi, new Point2D.Double(x,y));
            if (route.size() == 0) return false;
            Router.createRouteNoJob(route, ni.getParent(), false);
			return true;
		}

		/**
		 * Method to find the smallest layer on arc proto "ap" and cache that information
		 * in the "temp1" field of the arc proto.
		 */
		public static void findSmallestLayer(ArcProto ap)
		{
			// quit if the value has already been computed
			if (ap.getTempObj() != null) return;

			// get a dummy arc to analyze
			ArcInst ai = ArcInst.makeDummyInstance((PrimitiveArc)ap, 100);

			// find the smallest layer
			boolean bestFound = false;
			double bestArea = 0;
			Technology tech = ap.getTechnology();
			Poly [] polys = tech.getShapeOfArc(ai);
			int tot = polys.length;
			for(int i=0; i<tot; i++)
			{
				Poly poly = polys[i];
				double area = Math.abs(poly.getArea());

				if (bestFound && area >= bestArea) continue;
				bestArea = area;
				bestFound = true;
				ap.setTempObj(poly.getLayer());
			}
		}

	}

}
