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

import java.awt.geom.AffineTransform;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.database.CellUsage;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.generator.layout.LayoutLib;

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
    /** Stores the information necessary to generate an instance name for a Part
	  * It is sometimes important not to store the instance name as a String. 
	  * When I stored instance names as strings in NCC profiles indicated that 
	  * almost 50% of the storage space was used in these strings and 70% of the
	  * execution time was spent generating these Strings!!! */
	public static abstract class NameProxy implements Serializable {
		private VarContext context;
		private String sep;

		private String makePath(VarContext context, String sep) {
			String path = context.getInstPath(sep);
			if (!path.equals(""))  path+=sep;
			return path; 
		}
		protected NameProxy(VarContext context, String sep) {
			this.context = context;
			this.sep = sep;
		}
		abstract public String leafName();
		abstract public Cell leafCell();
		public VarContext getContext() {return context;}
		public String toString() {
			return makePath(context, sep) + leafName();		
		}
        public String toString(int numRemoveParents) {
            VarContext localContext = context.removeParentContext(numRemoveParents);
            String ret = makePath(localContext, sep) + leafName();
            return ret;
        }
	}
	public static class NetNameProxy extends NameProxy {
	    static final long serialVersionUID = 0;

		private Network net;
		public String leafName() {
			Iterator<String> it = net.getNames();
			if (it.hasNext()) {
				return it.next();
			} else {
				return "netIndex"+net.getNetIndex();
			}
		}
		public Iterator<String> leafNames() {return net.getNames();}
		public NetNameProxy(VarContext context, String sep, Network net) {
			super(context, sep);
			this.net = net;
		}
		public Cell leafCell() {return net.getParent();}
        public Network getNet() { return net; }
	}
	public static class NodableNameProxy extends NameProxy {
	    static final long serialVersionUID = 0;

		private Nodable nodable;
		public String leafName() {return nodable.getName();}
		public NodableNameProxy(VarContext context, String sep, Nodable node) {
			super(context, sep);
			this.nodable = node;
		}
		public Cell leafCell() {return nodable.getParent();}
		public Nodable getNodable() {return nodable;}
	}
    /**
     * Class describes which nets in a Cell are (recursively) shortened by
     * resistors. The new number of nodes is "externalIds + localIds".
     * "externalIds" are connected to exports or globals, "localIds" are not.
     * "ne
     */
    private static class CellShorts {
        /** The netlist of the Cell */
//        private Netlist netlist;
        /** Number of new nets connected to exports and globals. */
        int totalIds;
        /** Map from old net indices to new nets.
         * Extrenal nets are negative numbers form -1 to -externalIds.
         * Local nets are natural numbers from 0 to localIds-1.
         */
        int[] net2id;
        /** Temporary array for inherited nets
         */
        int[] externalIds;
        
        private CellShorts(Netlist netlist) {
//            this.netlist = netlist;
            int numNets = netlist.getNumNetworks();
            net2id = new int[numNets];
            for (int i = 0; i < numNets; i++) net2id[i] = i;
        }
        private void connect(int netIndex1, int netIndex2) {
            Netlist.connectMap(net2id, netIndex1, netIndex2);
        }
        private void fixup(int numExternalsInMap) {
            int numExternals = 0;
            for (int i = 0; i < net2id.length; i++) {
                int id = net2id[i];
                assert id <= i;
                if (id < i) {
                    net2id[i] = net2id[id];
                    continue;
                }
                net2id[i] = totalIds++;
                if (i < numExternalsInMap)
                    numExternals++;
            }
            externalIds = new int[numExternals];
        }
    }
	// --------------------- private data ------------------------------
	private Visitor visitor;
    private boolean shortResistors;
    private boolean shortPolyResistors;
    private boolean shortSpiceAmmeters;
	private boolean caching;
	private int cellCnt = 0; // For statistics
	private int instCnt = 0; // For statistics

	private List<NetDescription> netIdToNetDesc = new ArrayList<NetDescription>();
    private HashMap<Cell,CellShorts>cellShortsMap = new HashMap<Cell,CellShorts>();

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	// Prevent anyone from instantiating HierarchyEnumerator.
	private HierarchyEnumerator() {	}

    private CellShorts getShortened(Cell cell, Netlist netlist) {
        CellShorts shorts = cellShortsMap.get(cell);
        if (shorts != null) return shorts;

        shorts = new CellShorts(netlist);
        
       	Library spiceParts = Library.findLibrary("spiceparts");
       	Cell ammeterIcon = spiceParts==null ? null :
        	                            spiceParts.findNodeProto("Ammeter{ic}");

        for (Iterator<Nodable> it = netlist.getNodables(); it.hasNext();) {
			Nodable ni = it.next();
			NodeProto np = ni.getProto();
			if (ni.isCellInstance() && !((Cell)np).isIcon()) {
                Cell subCell = (Cell)np;
                CellShorts subShorts = getShortened(subCell, netlist.getNetlist(ni));
                Netlist subNetlist = netlist.getNetlist(ni);
                int[] externalIds = subShorts.externalIds;
                Arrays.fill(externalIds, -1);
                
        		Global.Set gs = subNetlist.getGlobals();
                for (int i = 0, numGlobals = gs.size(); i < numGlobals; i++) {
                    Global g = gs.get(i);
                    Network net = netlist.getNetwork(ni, g);
                    Network subNet = subNetlist.getNetwork(g);
                    int extID = subShorts.net2id[subNet.getNetIndex()];
                    if (externalIds[extID] >= 0)
                        shorts.connect(net.getNetIndex(), externalIds[extID]);
                    else
                        externalIds[extID] = net.getNetIndex();
                }
                
        		for (int i=0, numPorts = subCell.getNumPorts(); i < numPorts; i++) {
                	Export export = subCell.getPort(i);
                    for (int j=0, busWidth = subNetlist.getBusWidth(export); j < busWidth; j++) {
            			Network net = netlist.getNetwork(ni, export, j);
                        Network subNet = subNetlist.getNetwork(export, j);
                        int extID = subShorts.net2id[subNet.getNetIndex()];
                        if (externalIds[extID] >= 0)
                            shorts.connect(net.getNetIndex(), externalIds[extID]);
                        else
                            externalIds[extID] = net.getNetIndex();
                    }
                }
            } else if (!ni.isCellInstance()) {
                PrimitiveNode.Function fun = ((NodeInst)ni).getFunction();
                if (fun == PrimitiveNode.Function.RESIST && shortResistors ||
                        fun == PrimitiveNode.Function.PRESIST && shortPolyResistors) {
                    Network net0 = netlist.getNetwork(ni, np.getPort(0), 0);
                    Network net1 = netlist.getNetwork(ni, np.getPort(1), 0);
                    shorts.connect(net0.getNetIndex(), net1.getNetIndex());
                }
			} else if (np==ammeterIcon && shortSpiceAmmeters) {
                Network net0 = netlist.getNetwork(ni, np.getPort(0), 0);
                Network net1 = netlist.getNetwork(ni, np.getPort(1), 0);
                shorts.connect(net0.getNetIndex(), net1.getNetIndex());
			}
		}
        shorts.fixup(netlist.getNumExternalNetworks());
        cellShortsMap.put(cell, shorts);
		return shorts;
	}

    private int nextNetID() { return netIdToNetDesc.size(); }

	private int[] numberNets(Cell cell, Netlist netlist, 
							 int[][] portNdxToNetIDs, CellInfo info) {
		int numNets = netlist.getNumNetworks();
        CellShorts shorts = cellShortsMap.get(cell);
		int[] netNdxToNetID = new int[numNets];
        int baseId = nextNetID();
        Arrays.fill(shorts.externalIds, -1);
		if (portNdxToNetIDs != null) {
			assert portNdxToNetIDs.length == cell.getNumPorts() + 1;
			Global.Set globals = netlist.getGlobals();
			assert portNdxToNetIDs[0].length == globals.size();
			for (int i = 0; i < globals.size(); i++) {
				Global global = globals.get(i);
				int netIndex = netlist.getNetwork(global).getNetIndex();
                shorts.externalIds[shorts.net2id[netIndex]] = portNdxToNetIDs[0][i];
			}
			for (int i = 0, numPorts = cell.getNumPorts(); i < numPorts; i++) {
				Export export = cell.getPort(i);
				int[] ids = portNdxToNetIDs[i + 1];
				assert ids.length == export.getNameKey().busWidth();
				for (int j=0; j<ids.length; j++) {
					int netIndex = netlist.getNetwork(export, j).getNetIndex();
                    shorts.externalIds[shorts.net2id[netIndex]] = ids[j];
				}
			}
            for (int i = 0; i < shorts.externalIds.length; i++)
                assert shorts.externalIds[i] >= 0;
            baseId -= shorts.externalIds.length;
        }
		for (int i = 0; i < numNets; i++) {
			Network net = netlist.getNetwork(i);
            int localId = shorts.net2id[i];
            assert baseId + localId <= nextNetID();
            if (baseId + localId == nextNetID()) {
                if (portNdxToNetIDs == null && localId < shorts.externalIds.length)
                    shorts.externalIds[localId] = localId;
                assert nextNetID() == baseId + localId;
                netIdToNetDesc.add(new NetDescription(net, info));
            } else if (localId >= shorts.externalIds.length || portNdxToNetIDs == null) {
                NetDescription nd = netIdToNetDesc.get(baseId + localId);
                int cmp = !net.isUsernamed() ? 1 : nd.net.isUsernamed() ? 0 : -1; 
                if (cmp == 0 && net.isExported() != nd.net.isExported())
                    cmp = net.isExported() ? -1 : 1;
                if (cmp == 0)
                    cmp = TextUtils.STRING_NUMBER_ORDER.compare(net.getName(), nd.net.getName());
                if (cmp < 0)
                    nd.net = net;
            }
            int id = localId < shorts.externalIds.length ? shorts.externalIds[localId] : baseId + localId;
            netNdxToNetID[i] = id;
		}
		return netNdxToNetID;
	}
	
	private static int[] getGlobalNetIDs(Nodable no, Netlist netlist, int[] netNdxToNetID) {
		Global.Set gs = netlist.getNetlist(no).getGlobals();
		int[] netIDs = new int[gs.size()];
		for (int i = 0; i < gs.size(); i++) {
			int netIndex = netlist.getNetwork(no, gs.get(i)).getNetIndex();
			int netID = netNdxToNetID[netIndex];
			error(netID<0, "no netID for net");
			netIDs[i] = netID;
		}
		return netIDs;
	}

	private static int[] getPortNetIDs(Nodable no, PortProto pp,
									   Netlist netlist, int[] netNdxToNetID) {
		int busWidth = pp.getNameKey().busWidth();
		int[] netIDs = new int[busWidth];
		for (int j=0; j<busWidth; j++) {
			int netIndex = netlist.getNetwork(no, pp, j).getNetIndex();
			int netID = netNdxToNetID[netIndex];
			error(netID<0, "no netID for net");
			netIDs[j] = netID;
		}
		return netIDs;
	}

	private static int[][] buildPortMap(Netlist netlist, Nodable ni, 
							     int[] netNdxToNetID) {
		Cell cell = (Cell)ni.getProto();
		int numPorts = cell.getNumPorts();
		int[][] portNdxToNetIDs = new int[numPorts + 1][];
		portNdxToNetIDs[0] = getGlobalNetIDs(ni, netlist, netNdxToNetID);
		for (int i=0; i<numPorts; i++) {
			PortProto pp = cell.getPort(i);
			portNdxToNetIDs[i + 1] = getPortNetIDs(ni, pp, netlist, netNdxToNetID);
		}
		return portNdxToNetIDs;
	}

	/** portNdxToNetIDs translates an Export's index to an array of NetIDs */ 
	private void enumerateCell(Nodable parentInst, Cell cell,
	                           VarContext context, Netlist netlist, 
	                           int[][] portNdxToNetIDs,
		                       AffineTransform xformToRoot, CellInfo parent) {
		CellInfo info = visitor.newCellInfo();

		int firstNetID = nextNetID();
		int[] netNdxToNetID = numberNets(cell, netlist, portNdxToNetIDs, info);
		int lastNetIDPlusOne = nextNetID();
		cellCnt++;
		info.init(parentInst, cell,	cellShortsMap.get(cell), context, netlist, netNdxToNetID,
				  portNdxToNetIDs, xformToRoot, netIdToNetDesc, parent);

		boolean enumInsts = visitor.enterCell(info);
		if (!enumInsts) return;

		for (Iterator<Nodable> it = netlist.getNodables(); it.hasNext();) {
			Nodable ni = it.next();

			instCnt++;
			boolean descend = visitor.visitNodeInst(ni, info);
			NodeProto np = ni.getProto();
			if (descend && ni.isCellInstance() && !((Cell)np).isIcon()) {
				int[][] portNmToNetIDs2 = buildPortMap(netlist, ni, netNdxToNetID);
				AffineTransform xformToRoot2 = xformToRoot;
				if (ni instanceof NodeInst) {
					// add transformation from lower level
					xformToRoot2 = new AffineTransform(xformToRoot);
					xformToRoot2.concatenate(((NodeInst)ni).rotateOut());
					xformToRoot2.concatenate(((NodeInst)ni).translateOut());
				}
				enumerateCell(ni, (Cell)np, 
							  caching ? context.pushCaching(ni): context.push(ni), 
							  netlist.getNetlist(ni), 
							  portNmToNetIDs2, xformToRoot2, info);
			}
		}

		visitor.exitCell(info);
		
		// release storage associated with VarContext variable cache
		context.deleteVariableCache();

		// remove entries in netIdToNetDesc that we'll never use again
		for (int i = firstNetID; i < lastNetIDPlusOne; i++) {
			netIdToNetDesc.set(i, null);
		}
	}

	//  Set up everything for the root cell and then initiate the
	//  hierarchical traversal.
	private void doIt(Cell root, VarContext context, Netlist netlist, 
	                  Visitor visitor, boolean shortResistors, 
					  boolean shortPolyResistors, 
					  boolean shortSpiceAmmeters, boolean cache) {
		this.visitor = visitor;
        this.shortResistors = shortResistors;
        this.shortPolyResistors = shortPolyResistors;
        this.shortSpiceAmmeters = shortSpiceAmmeters;
		this.caching = cache;
		if (context == null) context = VarContext.globalContext;
		int[][] exportNdxToNetIDs = null;
        getShortened(root, netlist);
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

	/** The NetDescription object provides a Network and the level of
	 * hierarchy in which the Network occurs. The visitor can use
	 * NetDescription to formulate, for example, the name of
	 * the net */
	public static class NetDescription {
		private Network net;
		private CellInfo info;
		NetDescription(Network net, CellInfo info) {
			this.net = net;
			this.info = info;
		}

		public Network getNet() {return net;}
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
        private CellShorts shorts;
		private VarContext context;
		private Netlist netlist;
		private int[] netNdxToNetID;
		private int[][] exportNdxToNetIDs;
		private AffineTransform xformToRoot;
		private List<NetDescription> netIdToNetDesc;
		private CellInfo parentInfo;

		// package private
		void init(Nodable parentInst, Cell cell, CellShorts shorts, VarContext context,
			      Netlist netlist,
		          int[] netToNetID, int[][] exportNdxToNetIDs, 
				  AffineTransform xformToRoot, List<NetDescription> netIdToNetDesc,	
				  CellInfo parentInfo) {
			this.parentInst = parentInst;
			this.cell = cell;
            this.shorts = shorts;
			this.context = context;
			this.netlist = netlist;
			this.netNdxToNetID = netToNetID;
			this.exportNdxToNetIDs = exportNdxToNetIDs;
			this.xformToRoot = xformToRoot;
			this.netIdToNetDesc = netIdToNetDesc;
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
//
//		private double angleFromXY(double x, double y) {
//			double ans = Math.atan2(y, x) * 180/Math.PI;
//			//System.out.println("(x, y): ("+x+", "+y+")  angle: "+ans);
//			return ans;
//		}
//
//		private double angle0To360(double a) {
//			while (a >= 360) a -= 360;
//			while (a < 0)	 a += 360;
//			return a;
//		}
//
//		private String makePath(VarContext context, String sep) {
//			String path = context.getInstPath(sep);
//			if (!path.equals(""))  path+=sep;
//			return path; 
//		}

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
					netIDs[i] = shorts.net2id[netlist.getNetwork(e, i).getNetIndex()];
				}
				return netIDs;
			} else {
				return exportNdxToNetIDs[e.getPortIndex() + 1];
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
		public final int getNetID(Network net) {
			return getNetID(net.getNetIndex());
		}
		/** Map a net index from the current cell to a net ID. 
		 * number. During the course of the traversal, all nets that
		 * map to the same net number are connected. Nets that map to
		 * different net numbers are disconnected.
		 * @param netIndex net index within current cell
		 * @return net ID that is unique over entire net list.
		 */
		private int getNetID(int netIndex) {
			return netNdxToNetID[netIndex];
		}
		
//		public final boolean isGlobalNet(int netID) {
//			error(netID<0, "negative netIDs are illegal");
//			return netID <= largestGlobalNetID;
//		}
		
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
         * @return a unique String identifier for the network */
        public final String getUniqueNetName(Network net, String sep) {
        	NameProxy proxy = getUniqueNetNameProxy(net, sep);
        	return proxy.toString();
        }
        
        /** Same as getUniqueNetName except it returns a NameProxy instead of a
         * String name */
		public final NetNameProxy getUniqueNetNameProxy(Network net, String sep) {
			return getUniqueNetNameProxy(getNetID(net), sep);
		}
        
		/** Get a unique, flat net name for the network.  The network 
		 * name will contain the hierarchical context as returned by
		 * VarContext.getInstPath() if it is not a top-level network.
		 * @param sep the hierarchy separator to use if needed.
		 * @return a unique String identifier for the network */
		public final String getUniqueNetName(int netID, String sep) {
  			NameProxy proxy = getUniqueNetNameProxy(netID, sep);
  			return proxy.toString();
        }
		/** Same as getUniqueNetName except it returns a NameProxy instead of a
		 * String name */
		public final NetNameProxy getUniqueNetNameProxy(int netID, String sep) {
			NetDescription ns = netIdToNetDesc.get(netID);
			VarContext netContext = ns.getCellInfo().getContext();
			String leafName;

//			Iterator it = ns.getNet().getNames();
//			if (it.hasNext()) {
//				leafName = (String) it.next();
//			} else {
//				leafName = "netID"+netID;
//			}
			return new NetNameProxy(netContext, sep, ns.getNet());
		}
        
        /** Get a unique, flat instance name for the Nodable.
         * @param no 
         * @param sep the hierarchy separator to use if needed
         * @return a unique String identifer for the Nodable */
        public final String getUniqueNodableName(Nodable no, String sep) {
        	return getUniqueNodableNameProxy(no, sep).toString();
        }

		/** Same as getUniqueNodableName except that it returns a NameProxy 
		 *  instead of a String name. */
		public final NodableNameProxy getUniqueNodableNameProxy(Nodable no, String sep) {
			return new NodableNameProxy(getContext(), sep, no);
		}
        
		/** Get the Network that is closest to the root in the design
		 * hierarchy that corresponds to netID. */
		public final NetDescription netIdToNetDescription(int netID) {
			return netIdToNetDesc.get(netID);
		}

        /**
         * Get the Network in the parent that connects to the specified
         * Network in this cell.  Returns null if no network in parent connects
         * to this network (i.e. network is not connected to export), or null
         * if no parent.
         * @param network the network in this cell
         * @return the network in the parent that connects to the
         * specified network, or null if no such network.
         */
        public Network getNetworkInParent(Network network) {
            if (parentInfo == null) return null;
			if (network == null) return null;
			if (network.getNetlist() != netlist) return null;
            // find export on network
            boolean found = false;
            Export export = null;
            int i = 0;
            for (Iterator<Export> it = cell.getExports(); it.hasNext(); ) {
                export = it.next();
                for (i=0; i<export.getNameKey().busWidth(); i++) {
                    Network net = netlist.getNetwork(export, i);
                    if (net == network) { found = true; break; }
                }
                if (found) break;
            }
            if (found) {
                // find corresponding port on icon
                //System.out.println("In "+cell.describe()+" JNet "+network.describe()+" is exported as "+export.getName()+"; index "+i);
                Nodable no = context.getNodable();
                PortProto pp = no.getProto().findPortProto(export.getNameKey());
                //System.out.println("Found corresponding port proto "+pp.getName()+" on cell "+no.getProto().describe());
                // find corresponding network in parent
                Network parentNet = parentInfo.getNetlist().getNetwork(no, pp, i);
                return parentNet;
            }
            // check if global network
            Global.Set globals = netlist.getGlobals();
            for (i=0; i<globals.size(); i++) {
                Global global = globals.get(i);
                if (netlist.getNetwork(global) == network) {
                    // it is a global, return the global network in the parent
                    return parentInfo.getNetlist().getNetwork(global);
                }
            }
            return null;
        }

        /**
         * Method to get the transformation from the current location to the root.
         * If this is at the top cell, the transformation is identity.
         * @return the transformation from the current location to the root.
         */
        public AffineTransform getTransformToRoot() { return xformToRoot; }

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
//	public static void enumerateCell(Cell root, VarContext context, 
//	                                 Netlist netlist, Visitor visitor) {
//		if (netlist == null) netlist = NetworkTool.getUserNetlist(root);
//		(new HierarchyEnumerator()).doIt(root, context, netlist, visitor, false, false, false, false);
//	}
	public static void enumerateCell(Cell root, VarContext context, Visitor visitor) {
        enumerateCell(root, context, visitor, false);
	}
	public static void enumerateCell(Cell root, VarContext context, Visitor visitor, boolean shorten) {
        enumerateCell(root, context, visitor, shorten, shorten, shorten, false);
	}
	/** Experimental. Optionally caches results of variable evaluation. */
	public static void enumerateCell(Cell root, VarContext context,  Visitor visitor,
									 boolean shortResistors, boolean shortPolyResistors, 
									 boolean shortSpiceAmmeters, boolean caching) {
		Netlist netlist = NetworkTool.getNetlist(root, shortResistors & shortPolyResistors);
		(new HierarchyEnumerator()).doIt(root, context, netlist, visitor, 
				                         shortResistors, shortPolyResistors, 
										 shortSpiceAmmeters, caching);
	}
    /**
     * Method to count number of unique cells in hierarchy.  Useful
     * for progress tracking of hierarchical netlisters and writers.
     */
    public static int getNumUniqueChildCells(Cell cell) {
        HashMap<Cell,Cell> uniqueChildCells = new HashMap<Cell,Cell>();
        hierCellsRecurse(cell, uniqueChildCells);
        return uniqueChildCells.size();
    }
        
    /** Recursive method used to traverse down hierarchy */
    private static void hierCellsRecurse(Cell cell, HashMap<Cell,Cell> uniqueCells) {
        for (Iterator<CellUsage> uit = cell.getUsagesIn(); uit.hasNext();) {
            CellUsage u = uit.next();
            Cell subCell = u.getProto();
            if (subCell.isIcon()) continue;
            uniqueCells.put(subCell, subCell);
            hierCellsRecurse(subCell, uniqueCells);
        }
    }

    /**
     * Get the Network in the childNodable that corresponds to the Network in the childNodable's
     * parent cell.
     * @param parentNet the network in the parent
     * @param childNodable the child nodable.
     * @return the network in the child that connects to the network in the parent, or
     * null if no such network.
     */
    public static Network getNetworkInChild(Network parentNet, Nodable childNodable) {
        if (childNodable == null || parentNet == null) return null;
        if (!childNodable.isCellInstance()) return null;
        Cell childCell = (Cell)childNodable.getProto();
		Netlist parentNetlist = parentNet.getNetlist();
		Netlist childNetlist = parentNetlist.getNetlist(childNodable);
        PortProto pp = null;
        int i = 0;
        boolean found = false;
        NodeInst ni = childNodable.getNodeInst();
        // find port and index on nodable that is connected to parentNet
        for (Iterator<PortInst> it = ni.getPortInsts(); it.hasNext(); ) {
            PortInst pi = it.next();
            pp = pi.getPortProto();
            for (i=0; i<pp.getNameKey().busWidth(); i++) {
                Network net = parentNetlist.getNetwork(childNodable, pp, i);
//                Network net = childNodable.getParent().getUserNetlist().getNetwork(childNodable, pp, i);
                if (net == parentNet) { found = true; break; }
            }
            if (found) break;
        }
        if (!found) return null;
        // find corresponding export in child
        if (childCell.contentsView() != null)
            childCell = childCell.contentsView();
        Export export = childCell.findExport(pp.getNameKey());
        Network childNet = childNetlist.getNetwork(export, i);
//        Network childNet = childCell.getUserNetlist().getNetwork(export, i);
        return childNet;
    }

    /**
     * Method to search if child network is connected to visitor network (visitorNet).
     * Used in Quick.java and Connection.java.
     */
    public static boolean searchNetworkInParent(Network net, CellInfo info,
                                                Network visitorNet)
    {
        if (visitorNet == net) return true;
        CellInfo cinfo = info;
        while (net != null && cinfo.getParentInst() != null) {
            net = cinfo.getNetworkInParent(net);
            if (visitorNet == net) return true;
            cinfo = cinfo.getParentInfo();
        }
        return false;
    }

    public static boolean searchInExportNetwork(Network net, CellInfo info,
                                                Network visitorNet)
    {
        boolean found = false;
        for (Iterator<Export> it = net.getExports(); !found && it.hasNext();)
        {
            Export exp = it.next();
            Network tmpNet = info.getNetlist().getNetwork(exp, 0);
            found = searchNetworkInParent(tmpNet, info, visitorNet);
        }
        return found;
    }
}
