/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HierarchyEnumerator.java
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
package com.sun.electric.database.hierarchy;

import com.sun.electric.database.network.Global;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.generator.layout.LayoutLib; 

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
//import java.util.Map;

/** The HierarchyEnumerator can help programs that need to "flatten"
 * the design hierarchy. Examples of such programs include the logical
 * effort engine and routers.
 *
 * <p>The HierarchyEnumerator performs a recursive descent of
 * the "completely expanded" design hierarchy. The HierarchyEnumerator
 * brings the Visitor along with it during the excursion. 
 * The HierarchyEnumerator doesn't build a flattened data structure,
 * that's the prerogative of the Visitor. The HierarchyEnumerator simply
 * invokes Visitor methods for each Cell instance and NodeInst.
 * 
 * <p>The following example illustrates the notion of "completely
 * expanded". Suppose the root Cell instantiates Cell A twice, and
 * Cell A instantiates Cell B twice. Then the HierarchyEnumerator
 * visits two instances of Cell A and four instances of Cell B. 
 */
public final class HierarchyEnumerator {
	// --------------------- private data ------------------------------
	private Visitor visitor;
	//private int nextNetID = 0; // first unassigned net number
	private int cellCnt = 0; // For statistics
	private int instCnt = 0; // For statistics

	private Global.Set rootGlobals;
	private int[] globalToNetID;
	private List netIdToNetDesc = new ArrayList();
	// All netIDs between 0 and largestGlobalNetID are global nets
	private int largestGlobalNetID = -1;

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	// Prevent anyone from instantiating HierarchyEnumerator.
	private HierarchyEnumerator() {	};

	private int nextNetID() { return netIdToNetDesc.size(); }

	private int[] numberNets(Cell cell, Netlist netlist, 
							 int[][] portNdxToNetIDs, CellInfo info) {
		int numNets = netlist.getNumNetworks();
		int[] netNdxToNetID = new int[numNets];
		Arrays.fill(netNdxToNetID, -1);
		Global.Set globals = netlist.getGlobals();
		for (int i = 0; i < globals.size(); i++) {
			Global global = globals.get(i);
			int netIndex = netlist.getNetIndex(global);
			int globalIndex = rootGlobals.indexOf(global);
			netNdxToNetID[netIndex] = globalToNetID[globalIndex];
		}
		for (int i = 0; i < portNdxToNetIDs.length; i++) {
			Export export = (Export) cell.getPort(i);
			int[] ids = portNdxToNetIDs[i];
			for (int j=0; j<ids.length; j++) {
				int netIndex = netlist.getNetIndex(export, j);
				netNdxToNetID[netIndex] = ids[j];
			}
		}
		for (int i = 0; i < numNets; i++) {
			JNetwork net = netlist.getNetwork(i);
			if (netNdxToNetID[i] >= 0) continue;

			// No netID from export. Allocate a new netID.
			int netID = nextNetID();
			netIdToNetDesc.add(new NetDescription(net, info));
			netNdxToNetID[i] = netID;
		}
		return netNdxToNetID;
	}
	
	private static int[] getPortNetIDs(Nodable no, PortProto pp,
									   Netlist netlist, int[] netNdxToNetID) {
		int busWidth = pp.getNameKey().busWidth();
		int[] netIDs = new int[busWidth];
		for (int j=0; j<busWidth; j++) {
			int netIndex = netlist.getNetIndex(no, pp, j);
			int netID = netNdxToNetID[netIndex];
			error(netID<0, "no netID for net");
			netIDs[j] = netID;
		}
		return netIDs;
	}

	private int[][] buildPortMap(Netlist netlist, Nodable ni, 
							     int[] netNdxToNetID) {
		Cell cell = (Cell)ni.getProto();
		int numPorts = cell.getNumPorts();
		int[][] portNdxToNetIDs = new int[numPorts][];
		for (int i=0; i<numPorts; i++) {
			PortProto pp = cell.getPort(i);
			portNdxToNetIDs[i] = getPortNetIDs(ni, pp, netlist, netNdxToNetID);
		}
		return portNdxToNetIDs;
	}

	private void allocateGlobalNetIDs(CellInfo rootInfo, Netlist rootNetlist) {
		error(globalToNetID!=null, "already initialized?");
		globalToNetID = new int[rootGlobals.size()];
		for (int i=0; i<rootGlobals.size(); i++) {	
			Global global = rootGlobals.get(i);
			error(rootGlobals.indexOf(global)!=i, "bad index?");
			int netIndex = rootNetlist.getNetIndex(global);
			globalToNetID[i] = netIndex;
			if (netIndex == nextNetID()) {
				JNetwork net = rootNetlist.getNetwork(netIndex);
				netIdToNetDesc.add(new NetDescription(net, rootInfo));
				System.out.println(global + " added at " + netIndex);
			} else {
				error(netIndex>nextNetID(),	
					  "HierarchyEnumerator: unexpected order of global signal "+
					  global);
			}
		}
		largestGlobalNetID = nextNetID() - 1;
	}

	/** portNdxToNetIDs translates an Export's index to an array of NetIDs */ 
	private void enumerateCell(Nodable parentInst,	Cell cell,
	                           VarContext context, Netlist netlist, 
	                           int[][] portNdxToNetIDs,
		                       AffineTransform xformToRoot, CellInfo parent) {
		CellInfo info = visitor.newCellInfo();

		// if this is root then allocate netIDs for globals
		if (parent==null) allocateGlobalNetIDs(info, netlist);

		int firstNetID = nextNetID();
		int[] netNdxToNetID = numberNets(cell, netlist, portNdxToNetIDs, info);
		int lastNetIDPlusOne = nextNetID();
		cellCnt++;
		info.init(parentInst, cell,	context, netlist, netNdxToNetID, 
				  portNdxToNetIDs, xformToRoot, netIdToNetDesc, 
				  largestGlobalNetID, parent);

		boolean enumInsts = visitor.enterCell(info);
		if (!enumInsts) return;

		for (Iterator it = netlist.getNodables(); it.hasNext();) {
			Nodable ni = (Nodable)it.next();

			instCnt++;
			boolean descend = visitor.visitNodeInst(ni, info);
			NodeProto np = ni.getProto();
			if (descend && np instanceof Cell && !np.isIcon()) {
				int[][] portNmToNetIDs2 = buildPortMap(netlist, ni, netNdxToNetID);
				AffineTransform xformToRoot2 = xformToRoot;
				if (ni instanceof NodeInst) {
					// add transformation from lower level
					xformToRoot2 = new AffineTransform(xformToRoot);
					xformToRoot2.concatenate(((NodeInst)ni).rotateOut());
					xformToRoot2.concatenate(((NodeInst)ni).translateOut());
				}
				enumerateCell(ni, (Cell)np, context.push(ni), 
							  netlist.getNetlist(ni), 
							  portNmToNetIDs2, xformToRoot2, info);
			}
		}

		visitor.exitCell(info);

		// remove entries in netIdToNetDesc that we'll never use again
		for (int i = firstNetID; i < lastNetIDPlusOne; i++) {
			netIdToNetDesc.set(i, null);
		}
	}

	//  Set up everything for the root cell and then initiate the
	//  hierarchical traversal.
	private void doIt(Cell root, VarContext context, Netlist netlist, 
	                  Visitor visitor) {
		this.visitor = visitor;
		if (context == null) context = VarContext.globalContext;
		int[][] exportNdxToNetIDs = new int[0][];
		rootGlobals = netlist.getGlobals();
		enumerateCell(null,	root, context, netlist, exportNdxToNetIDs,
		              new AffineTransform(), null);

//		System.out.println("A total of: " + nextNetID() + " nets were numbered");
//		System.out.println("A total of: " + cellCnt + " Cells were visited");
//		System.out.println("A total of: " + instCnt + " NodeInsts were visited");
	}

	// ------------------------ public types ---------------------------
	/** Perform useful work while the HierarchyEnumerator enumerates
	 * the design. Whereas the HierarchyEnumerator is responsible for
	 * enumerating every Cell and NodeInst in the flattened design,
	 * the Visitor object is responsible for performing useful work
	 * during the enumeration.
	 *
	 * <p>The HierarchyEnumerator performs a recursive descent of the
	 * design hierarchy starting with the root Cell. When the
	 * HierarchyEnumerator enters a Cell instance it calls the Visitor's
	 * enterCell() method to let the Visitor know that it's just started
	 * working on a new Cell instance. Then the HierarchyEnumerator calls
	 * the Visitor's visitNodeInst() method for each NodeInst in that
	 * Cell. Finally, after all the NodeInsts have been visited, the
	 * HierarchyEnumerator calls the Visitor's exitCell() method to
	 * inform the Visitor that the HierarchyEnumerator is done with that
	 * Cell.
	 *
	 * <p>The Visitor's visitNodeInst() method controls whether the
	 * HierarchyEnumerator descends recursively into the Cell
	 * instantiated by that NodeInst. If the visitNodeInst() method
	 * returns true, then the HierarchyEnumerator enumerates the contents
	 * of that NodeInst's child Cell before it continues enumerating the
	 * NodeInsts of the current Cell.
	 */
	public static abstract class Visitor {
		/** A hook to allow the user to add additional information to
		 * a CellInfo. The newCellInfo method is a "Factory"
		 * method. If the user wishes to record additional application
		 * specific information for each Cell, the user should extend
		 * the CellInfo class and then override newCellInfo to return
		 * an instance of that derived class. */
		public CellInfo newCellInfo() {return new CellInfo();}

		/** The HierarchyEnumerator is about to begin enumerating the
		 * contents of a new Cell instance. That instance has just
		 * become the new "current" Cell instance.
		 * @param info information about the Cell instance being
		 * enumerated
		 * @return a boolean indicating if the HierarchyEnumerator
		 * should enumerate the contents of the current Cell. True
		 * means enumerate the current cell */
		public abstract boolean enterCell(CellInfo info);

		/** The HierarchyEnumerator has finished enumerating the
		 * contents of the current Cell instance. It is about to leave
		 * it, never to return.  The CellInfo associated with the
		 * current Cell instance is about to be abandoned.
		 * @param info information about the Cell instance being
		 * enumerated */
		public abstract void exitCell(CellInfo info);

		/** The HierarchyEnumerator is visiting Nodable ni.
		 * @param ni the Nodable that HierarchyEnumerator is visiting.
		 * @return a boolean indicating whether or not the
		 * HierarchyEnumerator should expand the Cell instantiated by
		 * ni. True means expand. If ni instantiates a PrimitiveNode
		 * then the return value is ignored by the
		 * HierarchyEnumerator. */
		public abstract boolean visitNodeInst(Nodable ni, CellInfo info);
        
        /** Using visitNodeInst implements a Top-Down traversal. If one
         * wishes to implement Bottom-Up traversal, use this method instead,
         * or in conjunction with visitNodeInst. */
        //public abstract void visitNodeInstBottomUp(Nodable ni, CellInfo info);
	}

	/** The NetDescription object provides a JNetwork and the level of
	 * hierarchy in which the JNetwork occurs. The visitor can use
	 * NetDescription to formulate, for example, the name of
	 * the net */
	public static class NetDescription {
		private JNetwork net;
		private CellInfo info;
		NetDescription(JNetwork net, CellInfo info) {
			this.net = net;
			this.info = info;
		}

		public JNetwork getNet() {return net;}
		public CellInfo getCellInfo() {return info;}
	}

	/** The CellInfo object is used to pass information to the Visitor
	 * during the enumeration. The CellInfo object contains many methods
	 * that describe the Cell currently being enumerated and the state
	 * of the enumeration.
	 *
	 * <p>The HierarchyEnumerator creates a new CellInfo for a Cell
	 * instance just before it begins enumerating the contents of that
	 * Cell instance.  The HierarchyEnumerator abandons the CellInfo once
	 * it is done enumerating the contents of that Cell instance. Once
	 * the CellInfo is abandoned the garbage collector may reclaim the
	 * CellInfo's storage.
	 *
	 * <p>Each CellInfo has a reference to the CellInfo of the parent of
	 * the current Cell instance. Thus the Visitor is able to get
	 * information about all the ancestors of the current Cell
	 * instance.
	 *
	 * <p>In most cases, the user will need to store additional
	 * information in the CellInfo. In those cases the user should
	 * extend the CellInfo class and override the Visitor.newCellInfo()
	 * method to return an instance of the derived class.
	 */
	public static class CellInfo {
		private Nodable parentInst;
		private Cell cell;
		private VarContext context;
		private Netlist netlist;
		private int[] netNdxToNetID;
		private int[][] exportNdxToNetIDs;
		private AffineTransform xformToRoot;
		private List netIdToNetDesc;
		private int largestGlobalNetID;
		private CellInfo parentInfo;

		// package private
		void init(Nodable parentInst, Cell cell, VarContext context,
			      Netlist netlist,
		          int[] netToNetID, int[][] exportNdxToNetIDs, 
				  AffineTransform xformToRoot, List netIdToNetDesc,	
				  int largestGlobalNetID, CellInfo parentInfo) {
			this.parentInst = parentInst;
			this.cell = cell;
			this.context = context;
			this.netlist = netlist;
			this.netNdxToNetID = netToNetID;
			this.exportNdxToNetIDs = exportNdxToNetIDs;
			this.xformToRoot = xformToRoot;
			this.netIdToNetDesc = netIdToNetDesc;
			this.largestGlobalNetID = largestGlobalNetID;
			this.parentInfo = parentInfo;
		}
		/**
		 * <p>Return an AffineTransform that encodes the size, rotation, and 
		 * center of this NodeInst.
		 *  
		 * <p>The returned AffineTransform has the property that when
		 * it is applied to a unit square centered at the origin the
		 * result is the bounding box of the NodeInst.
		 * This transform is useful because it can be used to 
		 * map the position of a NodeInst through levels of the design 
		 * hierarchy.
		 *  
		 * <p>Note that the user can set the position of a NodeInst 
		 * using NodeInst.setPositionFromTransform(). For example, the 
		 * following operations make no change to a NodeInst's position:
		 * 
		 * <code>
		 * ni.setPositionFromTransform(ni.getPositionFromTransform());
		 * </code>
		 */
//		public AffineTransform getPositionAsTransform(NodeInst ni) {
//			AffineTransform at = new AffineTransform();
//			at.setToTranslation(getAnchorCenterX(), getAnchorCenterY());
//			boolean transpose = sX<0 ^ sY<0;
//			if (transpose){
//				at.scale(1, -1);
//				at.rotate(Math.PI/2);
//			}
//			at.rotate(angle*Math.PI/1800);
//			at.scale(sX, sY);
//			return at;
//		}

		private double angleFromXY(double x, double y) {
			double ans = Math.atan2(y, x) * 180/Math.PI;
			//System.out.println("(x, y): ("+x+", "+y+")  angle: "+ans);
			return ans;
		}

		private double angle0To360(double a) {
			while (a >= 360) a -= 360;
			while (a < 0)	 a += 360;
			return a;
		}

		/**
		 * Temporary for testing the HierarchyEnumerator.
		 * 
		 * <p>Set the size, angle, and center of this NodeInst based upon an
		 * affine transformation. 
		 * 
		 * <p>The AffineTransform must map a unit square centered at the 
		 * origin to the desired bounding box for the NodeInst. 
		 *
		 * <p>Note that this operation cannot succeed for all affine
		 * transformations.  The reason is that Electric's transformations
		 * always preserve right angles whereas, in general, affine
		 * transformations do not.  If the given affine transformation does
		 * not preserve right angles this method will print a warning
		 * displaying the angle that results when a right angle is
		 * transformed.
		 * 
		 * <p>Warning: this code is experimental
		 * @param xForm the affine transformation. xForm must yield the 
		 * bounding box of the NodeInst when applied to a unit square 
		 * centered at the origin. 
		 */
//		public void setPositionFromTransform(AffineTransform xForm) {
//			double sizeX, sizeY, newAngle, centX, centY;
//			boolean debug = false;
//
//			if (debug) System.out.println(xForm);
//
//			Point2D a = new Point2D.Double(0, 1); // along Y axis
//			Point2D b = new Point2D.Double(0, 0); // origin
//			Point2D c = new Point2D.Double(1, 0); // along X axis
//
//			Point2D aP = new Point2D.Double();
//			Point2D bP = new Point2D.Double();
//			Point2D cP = new Point2D.Double();
//
//			xForm.transform(a, aP);
//			xForm.transform(b, bP);
//			xForm.transform(c, cP);
//
//			if (debug) {
//				System.out.println("aP: " + aP);
//				System.out.println("bP: " + bP);
//				System.out.println("cP: " + cP);
//			}
//
//			sizeX = bP.distance(cP);
//			sizeY = bP.distance(aP);
//			centX = bP.getX();
//			centY = bP.getY();
//		
//			double angleA = angleFromXY(aP.getX() - bP.getX(), aP.getY() - bP.getY());
//			double angleC = angleFromXY(cP.getX() - bP.getX(), cP.getY() - bP.getY());
//			double angleAC = angle0To360(angleA - angleC);
//
//			if (debug) {
//				System.out.println("angleC: " + angleC);
//				System.out.println("angleA: " + angleA);
//				System.out.println("angleAC: " + angleAC);
//			}
//			// round to 1/10 degrees
//			angleAC = Math.rint(angleAC * 10) / 10;
//			if (angleAC == 90) {
//				newAngle = angle0To360(angleC);
//			} else if (angleAC == 270) {
//				// By using geometric constructions on paper I determined that I 
//				// need to rotate by (270 degrees - angleC) and then transpose. 
//				newAngle = angle0To360(270 - angleC);
//				sizeX = -sizeX; // Negative size means transpose (not mirror)
//			} else {
//				System.out.println("error in NodeInst.setPositionFromTransform: "+
//								   "angle not 90 or 270: " + angleAC);
//				newAngle = angleC;
//			}
//
//			if (debug) System.out.println(
//							"setPositionFromTransform: new position {\n"
//								+ "    sizeX: " + sizeX + "\n"
//								+ "    sizeY: " + sizeY + "\n"
//								+ "    angle: "  + newAngle + "\n"
//								+ "    dx: " + centX + "\n"
//								+ "    dy: " + centY + "\n"
//								+ "}\n");
//
//			modifyInstance(centX-getAnchorCenterX(), centY-getAnchorCenterY(), sizeX-sX,
//						   sizeY-sY, (int)Math.round(newAngle*10)-angle);
//		}


		/** The Cell currently being visited. */
		public final Cell getCell() {return cell;}

		/** The Cell that is the root of the traversal */
		public final boolean isRootCell() {return parentInfo == null;}

		/** The VarContext to use for evaluating all variables in the
		 * current Cell. */
		public final VarContext getContext() {return context;}

		/** The Netlist of the current Cell. */
		public final Netlist getNetlist() {return netlist;}

		/** Get the CellInfo for the current Cell's parent.  If the
		 * current Cell is the root then return null. */
		public final CellInfo getParentInfo() {return parentInfo;}

		/** Get the NodeInst that instantiates the Current
		 * instance. If the current Cell is the root then return
		 * null. */
		public final Nodable getParentInst() {return parentInst;}

		/** Get the CellInfo for the root Cell */
		public final CellInfo getRootInfo() {
			CellInfo i = this;
			while (i.getParentInfo()!=null)  i = i.getParentInfo();
			return i;
		}

		/** Get netIDs for the Export: e.
		 * @return an array of net numbers. */
		public final int[] getExportNetIDs(Export e) {
			if (isRootCell()) {
				// exportNdxToNetIDs is invalid for the root Cell because
				// no mapping from net index to netID is performed for Exports
				// of the root Cell.
				int width = netlist.getBusWidth(e);
				int[] netIDs = new int[width];
				for (int i=0; i<width; i++) {
					netIDs[i] = netlist.getNetIndex(e, i);
				}
				return netIDs;
			} else {
				return exportNdxToNetIDs[e.getPortIndex()];
			}
		}

		/** Map any net inside the current cell to a net
		 * number. During the course of the traversal, all nets that
		 * map to the same net number are connected. Nets that map to
		 * different net numbers are disconnected.
		 *
		 * <p>If you want to generate a unique name for the net use
		 * getUniqueNetName().
		 */
		public final int getNetID(JNetwork net) {
			return getNetID(net.getNetIndex());
		}
		/** Map a net index from the current cell to a net ID. 
		 * number. During the course of the traversal, all nets that
		 * map to the same net number are connected. Nets that map to
		 * different net numbers are disconnected.
		 * @param netIndex net index within current cell
		 * @return net ID that is unique over entire net list.
		 */
		public final int getNetID(int netIndex) {
			return netNdxToNetID[netIndex];
		}
		
		public final boolean isGlobalNet(int netID) {
			error(netID<0, "negative netIDs are illegal");
			return netID <= largestGlobalNetID;
		}
		
		/** 
		 * Get the set of netIDs that are connected to the specified port of
		 * the specified Nodable. 
		 */
		public final int[] getPortNetIDs(Nodable no, PortProto pp) {
			return HierarchyEnumerator.getPortNetIDs(no, pp, netlist, 
													 netNdxToNetID);
		}

        /** Get a unique, flat net name for the network.  The network 
         * name will contain the hierarchical context as returned by
         * VarContext.getInstPath() if it is not a top-level network.
         * @param sep the context separator to use if needed.
         * @return a unique String identifier for the network
         */
        public final String getUniqueNetName(JNetwork net, String sep) {
        	return getUniqueNetName(net.getNetIndex(), sep);
        }

		/** Get a unique, flat net name for the network.  The network 
		 * name will contain the hierarchical context as returned by
		 * VarContext.getInstPath() if it is not a top-level network.
		 * @param sep the context separator to use if needed.
		 * @return a unique String identifier for the network
		 */
		public final String getUniqueNetName(int netID, String sep) {
            NetDescription ns = (NetDescription) netIdToNetDesc.get(netID);
            if (ns == null ) {
                System.out.println("ns is null");
            }
            if (ns.getCellInfo() == null ) {
                System.out.println("cell info is null");
            }
            VarContext netContext = ns.getCellInfo().getContext();

            StringBuffer buf = new StringBuffer();
            buf.append(ns.getCellInfo().getContext().getInstPath(sep));  // append hier path if any
            if (!buf.toString().equals("")) buf.append(sep);
        	Iterator it = ns.getNet().getNames();
            if (it.hasNext()) {
    			buf.append((String) it.next());
    		} else {
        		buf.append("netID"+netID);
            }
            return buf.toString();
        }
        
		/** Get the JNetwork that is closest to the root in the design
		 * hierarchy that corresponds to netID. */
		public final NetDescription netIdToNetDescription(int netID) {
			return (NetDescription) netIdToNetDesc.get(netID);
		}

		/** Find position of NodeInst: ni in the root Cell. The
		 * getPositionInRoot method is useful for flattening
		 * layout. */
		public final AffineTransform getPositionInRoot(NodeInst ni) {
			AffineTransform x = new AffineTransform(xformToRoot);
//			x.concatenate(ni.getPositionAsTransform());
			return x;
		}

		/**
		 * Method to get the transformation from the current location to the root.
		 * If this is at the top cell, the transformation is identity.
		 * @return the transformation from the current location to the root.
		 */
		public AffineTransform getTransformToRoot() { return xformToRoot; }

		/** Find the position of a Point2D: p in the root Cell. The
		 * getPositionInRoot method is useful for flattening
		 * layout. */
		public final Point2D getPositionInRoot(Point2D p) {
			Point2D ans = new Point2D.Double();
			xformToRoot.transform(p, ans);
			return ans;
		}

		/** Find the rectangular bounds of rectangle: r in the root
		 * Cell.  If the current Cell instance is rotated by a
		 * multiple of 90 degrees in the root Cell then the result is
		 * also the position of rectangle: r mapped to the root Cell.
		 * getBoundsInRoot is useful for flattening layout. */
		public final Rectangle2D getBoundsInRoot(Rectangle2D r) {
			double[] coords = new double[8];
			// Clockwise
			coords[0] = r.getX();
			coords[1] = r.getY();

			coords[2] = r.getX();
			coords[3] = r.getY() + r.getHeight();

			coords[4] = r.getX() + r.getWidth();
			coords[5] = r.getY() + r.getHeight();

			coords[6] = r.getX() + r.getWidth();
			coords[7] = r.getY();

			xformToRoot.transform(coords, 0, coords, 0, 8);
			double minX, minY, maxX, maxY;
			minX = minY = Double.MAX_VALUE;
			maxX = maxY = Double.MIN_VALUE;
			for (int i = 0; i < 8; i += 2) {
				minX = Math.min(minX, coords[i]);
				maxX = Math.max(maxX, coords[i]);
				minY = Math.min(minY, coords[i + 1]);
				maxY = Math.max(maxY, coords[i + 1]);
			}
            return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
		}
	}

	// ----------------------- public methods --------------------------

	/** Begin enumeration of the contents of the Cell root.  You MUST
	 * call rebuildNetworks() on the root Cell before calling
	 * beginEnumeration().
	 * @param root the starting point of the enumeration.
	 * @param context the VarContext for evaluating parameters in Cell
	 * root. If context is null then VarContext.globalContext is used.
	 * @param visitor the object responsible for doing something useful
	 * during the enumertion of the design hierarchy. */
	public static void enumerateCell(Cell root, VarContext context, 
	                                 Netlist netlist, Visitor visitor) {
		if (netlist == null) netlist = Network.getUserNetlist(root);
		(new HierarchyEnumerator()).doIt(root, context, netlist, visitor);
	}

    /**
     * Method to count number of unique cells in hierarchy.  Useful
     * for progress tracking of hierarchical netlisters and writers.
     */
    public static int getNumUniqueChildCells(Cell cell) {
        HashMap uniqueChildCells = new HashMap();
        hierCellsRecurse(cell, uniqueChildCells);
        return uniqueChildCells.size();
    }
        
    /** Recursive method used to traverse down hierarchy */
    private static void hierCellsRecurse(Cell cell, HashMap uniqueCells) {
        for (Iterator uit = cell.getUsagesIn(); uit.hasNext();) {
            NodeUsage nu = (NodeUsage) uit.next();
            if (nu.isIcon()) continue;
            NodeProto np = nu.getProto();
            if (!(np instanceof Cell)) continue;
            uniqueCells.put((Cell)np, (Cell)np);
            hierCellsRecurse((Cell)np, uniqueCells);
        }
    }
}
