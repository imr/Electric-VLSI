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

import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;

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

	private List netIdToNetDesc = new ArrayList();

	private static void error(boolean pred, String msg) {
		if (pred) System.out.println(msg);
	}

	// Prevent anyone from instantiating HierarchyEnumerator.
	private HierarchyEnumerator() {	};

	private int nextNetID() { return netIdToNetDesc.size(); }

	private int[] numberNets(Cell cell, Netlist netlist, int[][] portNmToNetIDs, CellInfo info) {
		int numNets = netlist.getNumNetworks();
		int[] netToNetID = new int[numNets];
		Arrays.fill(netToNetID, -1);
		for (int i = 0; i < portNmToNetIDs.length; i++) {
			Export export = (Export) cell.getPort(i);
			int[] ids = portNmToNetIDs[i];
			for (int j = 0; j < ids.length; j++) {
				int netIndex = netlist.getNetIndex(export, j);
				netToNetID[netIndex] = ids[j];
			}
		}
		for (int i = 0; i < numNets; i++) {
			JNetwork net = netlist.getNetwork(i);
			if (netToNetID[i] >= 0) continue;

			// No netID from export. Allocate a new netID.
			int netID = nextNetID();
			netIdToNetDesc.add(new NetDescription(net, info));
			netToNetID[i] = netID;
		}
		return netToNetID;
	}

	private int[][] buildPortMap(Netlist netlist, Nodable ni, int[] netToNetID) {
		Cell cell = (Cell)ni.getProto();
		int numPorts = cell.getNumPorts();
		int[][] portNmToNetIDs = new int[numPorts][];
		for (int i = 0; i < numPorts; i++) {
			PortProto pp = cell.getPort(i);
			int busWidth = pp.getProtoNameKey().busWidth();
			int[] netIDs = new int[busWidth];
			for (int j = 0; j < busWidth; j++) {
				int netIndex = netlist.getNetIndex(ni, pp, j);
				int netID = netToNetID[netIndex];
				error(netID < 0, "no netID for net");
				netIDs[j] = netID;
			}
			portNmToNetIDs[i] = netIDs;
		}
		return portNmToNetIDs;
	}

	// portNmToNetIDs is a map from an Export name to an array of
	// NetIDs.
	private void enumerateCell(Nodable parentInst,	Cell cell,
	                           VarContext context, Netlist netlist, int[][] portNmToNetIDs,
		                       AffineTransform xformToRoot,
		                       CellInfo parent) {
		CellInfo info = visitor.newCellInfo();
		int firstNetID = nextNetID();
		int[] netToNetID = numberNets(cell, netlist, portNmToNetIDs, info);
		int lastNetIDPlusOne = nextNetID();
		cellCnt++;
		info.init(parentInst, cell,	context, netlist, netToNetID, portNmToNetIDs,
			      xformToRoot, netIdToNetDesc, parent);

		boolean enumInsts = visitor.enterCell(info);
		if (!enumInsts) return;

		for (Iterator it = netlist.getNodables(); it.hasNext();) {
			Nodable ni = (Nodable)it.next();

			instCnt++;
			boolean descend = visitor.visitNodeInst(ni, info);
			NodeProto np = ni.getProto();
			if (descend && np instanceof Cell && !np.isIcon()) {
				int[][] portNmToNetIDs2 = buildPortMap(netlist, ni, netToNetID);
				AffineTransform xformToRoot2 = xformToRoot;
				if (ni instanceof NodeInst) {
					xformToRoot2 = new AffineTransform(xformToRoot);
					xformToRoot2.concatenate(((NodeInst)ni).rkTransformOut());
				}
				enumerateCell(ni, (Cell)np, context.push(ni), netlist.getNetlist(ni), portNmToNetIDs2,
					xformToRoot2, info);
			}
		}

// 		for (Iterator it = cell.getNodes(); it.hasNext();) {
// 			NodeInst ni = (NodeInst) it.next();
                
// 			instCnt++;
// 			boolean descend = visitor.visitNodeInst(ni, info);
//             NodeProto np = ni.getProto();
//             Cell eq = ni.getProtoEquivalent();
//             if (cell == eq) descend = false;  // do not descend into own icon
//             if (descend && np instanceof Cell) {
// 				if (eq == null) {
// 					System.out.println("Warning: missing schematic: " 
// 					                   + np.getProtoName());
// 				} else {
// 					Map portNmToNetIDs2 = buildPortMap(ni, netToNetID);
// 					AffineTransform xformToRoot2 =
// 						new AffineTransform(xformToRoot);
// 					xformToRoot2.concatenate(ni.rkTransformOut());
// 					enumerateCell(ni, eq, context.push(ni), portNmToNetIDs2,
// 						          xformToRoot2, info);
// 				}
// 			}
// 		}
		visitor.exitCell(info);

		// remove entries in netIdToNetDesc that we'll never use again
		for (int i = firstNetID; i < lastNetIDPlusOne; i++) {
			netIdToNetDesc.set(i, null);
		}
	}

	//  Set up everything for the root cell and then initiate the
	//  hierarchical traversal.
	private void doIt(Cell root, VarContext context, Netlist netlist, Visitor visitor) {
		this.visitor = visitor;
		if (context == null) context = VarContext.globalContext;
		int numPorts = root.getNumPorts();
		int[][] portNmToNetIDs = new int[0][];
		enumerateCell(null,	root, context, netlist, portNmToNetIDs,
		              new AffineTransform(), null);

		System.out.println("A total of: " + nextNetID() + " nets were numbered");
		System.out.println("A total of: " + cellCnt + " Cells were visited");
		System.out.println(
			"A total of: " + instCnt + " NodeInsts were visited");
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
		private Cell cell;
		private VarContext context;
		private Netlist netlist;
		private int[] netToNetID;
		private int[][] exportNmToNetIDs;
		private AffineTransform xformToRoot;
		private Nodable parentInst;
		private CellInfo parentInfo;
		private List netIdToNetDesc;

		// package private
		void init(Nodable parentInst, Cell cell, VarContext context,
			      Netlist netlist,
		          int[] netToNetID, int[][] exportNmToNetIDs, 
				  AffineTransform xformToRoot, List netIdToNetDesc,	
				  CellInfo parentInfo) {
			this.parentInst = parentInst;
			this.cell = cell;
			this.context = context;
			this.netlist = netlist;
			this.netToNetID = netToNetID;
			this.exportNmToNetIDs = exportNmToNetIDs;
			this.xformToRoot = xformToRoot;
			this.parentInfo = parentInfo;
			this.netIdToNetDesc = netIdToNetDesc;
		}

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

		/** Get netIDs for the Export named: exportNm.
		 * @return an array of net numbers. */
		public final int[] getExportNetIDs(String exportNm) {
			PortProto pp = cell.findPortProto(exportNm);
			if (pp == null) return null;
			return exportNmToNetIDs[pp.getPortIndex()];
		}

		/** Map any net inside the current cell to a net
		 * number. During the course of the traversal, all nets that
		 * map to the same net number are connected. Nets that map to
		 * different net numbers are disconnected.
		 *
		 * <p>If you want to generate a unique name for the net use
		 * netIdToNetDescription().
		 *
		 * <p>I'm not sure getNetID is the right thing to do. It might
		 * be better to return a NetDescription here and embed the
		 * netID in the NetDescription.  Also, I haven't provided a
		 * mechanism for mapping a JNetwork in the current Cell to a
		 * JNetwork in the current Cell's parent. */
		public final int getNetID(JNetwork net) {
			return netToNetID[net.getNetIndex()];
		}

        /** Get a unique, flat net name for the network.  The network 
         * name will contain the hierarchical context as returned by
         * VarContext.getInstPath() if it is not a top-level network.
         * @param sep the context separator to use if needed.
         * @return a unique String identifier for the network
         */
        public final String getUniqueNetName(JNetwork net, String sep) {
            int netID = netToNetID[net.getNetIndex()];
            NetDescription ns = (NetDescription) netIdToNetDesc.get(netID);
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
			x.concatenate(ni.getPositionAsTransform());
			return x;
		}

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
	public static void enumerateCell(
		Cell root,
		VarContext context,
		Netlist netlist,
		Visitor visitor) {
		if (netlist == null) netlist = Network.getUserNetlist(root);
		(new HierarchyEnumerator()).doIt(root, context, netlist, visitor);
	}

    /**
     * Method to count number of unique cells in hierarchy.  Useful
     * for progress tracking of hierarchical netlisters and writers.
     */
    public static int getNumUniqueChildCells(Cell cell)
    {
        HashMap uniqueChildCells = new HashMap();
        hierCellsRecurse(cell, uniqueChildCells);
        return uniqueChildCells.size();
    }
        
    /** Recursive method used to traverse down hierarchy */
    private static void hierCellsRecurse(Cell cell, HashMap uniqueCells)
    {
        for (Iterator uit = cell.getUsagesIn(); uit.hasNext();)
        {
            NodeUsage nu = (NodeUsage) uit.next();
            if (nu.isIcon()) continue;
            NodeProto np = nu.getProto();
            if (!(np instanceof Cell)) continue;
            uniqueCells.put((Cell)np, (Cell)np);
            hierCellsRecurse((Cell)np, uniqueCells);
        }
    }
}
