package com.sun.electric.database.hierarchy;

import java.util.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.variable.VarContext;

/** The purpose of the HierarchyEnumerator is to provide an shared
 * infrastructure for programs that wish to "flatten" the design
 * hierarchy. Examples of such programs include the logical effort
 * engine and routers.
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
 *
 * <p>Warning: this code is experimental. */
public final class HierarchyEnumerator {
	// --------------------- private data ------------------------------
	private Visitor visitor;
	private int nextNetID = 0; // first unassigned net number
	private int cellCnt = 0; // For statistics
	private int instCnt = 0; // For statistics

	private Map netIdToNetDesc = new HashMap();

	private static void error(boolean pred, String msg) {
		if (pred) System.out.println(msg);
	}

	// Prevent anyone from instantiating HierarchyEnumerator.
	private HierarchyEnumerator() {	};

	// See if this net inherits a net number from an export. If not then
	// return null.
	private Integer netIDFromExports(JNetwork net, Map portNmToNetID) {
		// A network may get a net number from an export
		for (Iterator expIt = net.getExports(); expIt.hasNext();) {
			Export e = (Export) expIt.next();
			Integer[] ids = (Integer[]) portNmToNetID.get(e.getProtoName());
			if (ids != null) {
				// this only works in the absence of busses
				return ids[0];
			}
		}
		return null;
	}

	private Map numberNets(Cell cell, Map portNmToNetIDs, CellInfo info) {
		Map netToNetID = new HashMap();
		for (Iterator netIt = cell.getNetworks(); netIt.hasNext();) {
			JNetwork net = (JNetwork) netIt.next();

			Integer netID = netIDFromExports(net, portNmToNetIDs);

			// No netID from export. Allocate a new netID.
			if (netID == null) {
				netID = new Integer(nextNetID++);
				netIdToNetDesc.put(netID, new NetDescription(net, info));
			}

			netToNetID.put(net, netID);
		}
		return netToNetID;
	}

	private Integer[] getNetIDs(PortInst pi, Map netToNetID) {
		JNetwork net = pi.getNetwork();
		error(net == null,
			  "Network=null! Did you call Cell.rebuildNetworks()?");
		Integer netID = (Integer) netToNetID.get(net);
		error(netID == null, "no netID for net");

		// only works in absence of busses
		return new Integer[] { netID };
	}

	private Map buildPortMap(NodeInst ni, Map netToNetID) {
		Map portNmToNetIDs = new HashMap();
		for (Iterator it = ni.getPortInsts(); it.hasNext();) {
			PortInst pi = (PortInst) it.next();
			Integer[] netIDs = getNetIDs(pi, netToNetID);
			portNmToNetIDs.put(pi.getPortProto().getProtoName(), netIDs);
		}
		return portNmToNetIDs;
	}

	// portNmToNetIDs is a map from an Export name to an array of
	// NetIDs.
	private void enumerateCell(NodeInst parentInst,	Cell cell,
	                           VarContext context, Map portNmToNetIDs,
		                       AffineTransform xformToRoot,
		                       CellInfo parent) {
		CellInfo info = visitor.newCellInfo();
		int firstNetID = nextNetID;
		Map netToNetID = numberNets(cell, portNmToNetIDs, info);
		int lastNetIDPlusOne = nextNetID;
		cellCnt++;
		info.init(parentInst, cell,	context, netToNetID, portNmToNetIDs,
			      xformToRoot, netIdToNetDesc, parent);

		boolean enumInsts = visitor.enterCell(info);
		if (!enumInsts) return;

		for (Iterator it = cell.getNodes(); it.hasNext();) {
			NodeInst ni = (NodeInst) it.next();
                
			instCnt++;
			boolean descend = visitor.visitNodeInst(ni, info);
            NodeProto np = ni.getProto();
            Cell eq = ni.getProtoEquivalent();
            if (cell == eq) descend = false;  // do not descend into own icon
            if (descend && np instanceof Cell) {
				if (eq == null) {
					System.out.println("Warning: missing schematic: " 
					                   + np.getProtoName());
				} else {
					Map portNmToNetIDs2 = buildPortMap(ni, netToNetID);
					AffineTransform xformToRoot2 =
						new AffineTransform(xformToRoot);
					xformToRoot2.concatenate(ni.rkTransformOut());
					enumerateCell(ni, eq, context.push(ni), portNmToNetIDs2,
						          xformToRoot2, info);
				}
			}
		}
		visitor.exitCell(info);

		// remove entries in netIdToNetDesc that we'll never use again
		for (int i = firstNetID; i < lastNetIDPlusOne; i++) {
			netIdToNetDesc.remove(new Integer(i));
		}
	}

	//  Set up everything for the root cell and then initiate the
	//  hierarchical traversal.
	private void doIt(Cell root, VarContext context, Visitor visitor) {
		this.visitor = visitor;
		if (context == null) context = VarContext.globalContext;
		enumerateCell(null,	root, context, new HashMap(),
		              new AffineTransform(), null);

		System.out.println("A total of: " + nextNetID + " nets were numbered");
		System.out.println("A total of: " + cellCnt + " Cells were visited");
		System.out.println(
			"A total of: " + instCnt + " NodeInsts were visited");
	}

	// ------------------------ public types ---------------------------
	/** Whereas the HierarchyEnumerator is responsible for enumerating
	 * every Cell and NodeInst in the flattened design, the Visitor
	 * object is responsible for performing useful work during the
	 * enumeration.  
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
	 *
	 * <p>Warning: This code is experimental.*/
	public static abstract class Visitor {
		/** This is a hook to allow the user to add additional information
		 * to a CellInfo. This is a "Factory" method. If the user wishes
		 * to record additional application specific information for each
		 * Cell, she should extend the CellInfo class and then override
		 * this method to return an instance of that derived class. */
		public CellInfo newCellInfo() {return new CellInfo();}

		/** The HierarchyEnumerator is about to begin enumerating the
		 * contents of a new Cell instance.
		 * @param info information about the Cell instance being
		 * enumerated
		 * @return The Visitor should true if she wishes to enumerate the
		 * contents of this Cell instance */
		public abstract boolean enterCell(CellInfo info);

		/** The HierarchyEnumerator has finished enumerating the contents
		 * of this Cell instance. It is about to leave this Cell instance,
		 * never to return.  The CellInfo associated with this Cell
		 * instance is about to be abandoned.
		 * @param info information about the Cell instance being
		 * enumerated */
		public abstract void exitCell(CellInfo info);

		/** The HierarchyEnumerator is visiting NodeInst ni.
		 * @param ni The NodeInst that HierarchyEnumerator is visiting.
		 * @return The visitor should return true if this is an instance
		 * of a cell and the visitor wishes to descend into this
		 * instance. If this instance is of a PrimitiveNode then the
		 * return value is ignored by the HierarchyEnumerator. */
		public abstract boolean visitNodeInst(NodeInst ni, CellInfo info);
	}

	/** The NetDescription object provides a JNetwork and the level of
	 * hierarchy in which the JNetwork occurs. The visitor can use this
	 * object to formulate, for example, the name of the net */
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
	 *
	 * <p>Warning: Did I mention that this code is experimental? */
	public static class CellInfo {
		private Cell cell;
		private VarContext context;
		private Map netToNetID;
		private Map exportNmToNetIDs;
		private AffineTransform xformToRoot;
		private NodeInst parentInst;
		private CellInfo parentInfo;
		private Map netIdToNetDesc;

		// package private
		void init(NodeInst parentInst, Cell cell, VarContext context, 
		          Map netToNetID, Map exportNmToNetIDs, 
				  AffineTransform xformToRoot, Map netIdToNetDesc,	
				  CellInfo parentInfo) {
			this.parentInst = parentInst;
			this.cell = cell;
			this.context = context;
			this.netToNetID = netToNetID;
			this.exportNmToNetIDs = exportNmToNetIDs;
			this.xformToRoot = xformToRoot;
			this.parentInfo = parentInfo;
			this.netIdToNetDesc = netIdToNetDesc;
		}

		/** The Cell currently being visited. */
		public final Cell getCell() {return cell;}

		/** This Cell is the root of the traversal */
		public final boolean isRootCell() {return parentInfo == null;}

		/** The VarContext to use for evaluating all variables in this
		 * Cell. */
		public final VarContext getContext() {return context;}

		/** Get the CellInfo for this Cell's parent.  If this Cell is the
		 * root then return null. */
		public final CellInfo getParentInfo() {return parentInfo;}

		/** Get the NodeInst in the parent Cell that instantiates this
		 * Cell instance. If this Cell is the root then return null. */
		public final NodeInst getParentInst() {return parentInst;}

		/** Get the CellInfo for the root Cell */
		public final CellInfo getRootInfo() {
			CellInfo i = this;
			while (i.getParentInfo()!=null)  i = i.getParentInfo();
			return i;
		}

		/** Get netIDs for the Export named: exportNm.
		 * @return an array of net numbers. */
		public final Integer[] getExportNetIDs(String exportNm) {
			return (Integer[]) exportNmToNetIDs.get(exportNm);
		}

		/** Map any net inside the cell to a net number. During the course
		 * of the traversal, all nets that map to the same net number are
		 * connected. Nets that map to different net numbers are
		 * disconnected.
		 *
		 * <p>If you want to generate a unique name for the net use
		 * netIdToNetDescription().
		 *
		 * <p>I'm not sure this is the right thing to do. It might be
		 * better to return a NetDescription here and embed the netID in
		 * the NetDescription.  Also, I haven't provided a mechanism for
		 * mapping a JNetwork in this Cell to a JNetwork in this Cell's
		 * parent. */
		public final Integer getNetID(JNetwork net) {
			return (Integer) netToNetID.get(net);
		}

        /** Get a unique, flat net name for the network.  The network 
         * name will contain the hierarchical context as returned by
         * VarContext.getInstPath() if it is not a top-level network.
         * @param sep the context separator to use if needed.
         * @return a unique String identifier for the network
         */
        public final String getUniqueNetName(JNetwork net, String sep) {
            System.out.println("Jnetwork is "+net);
            Integer netID = (Integer)netToNetID.get(net);
            System.out.println("netID is "+netID);
            NetDescription ns = (NetDescription)netIdToNetDesc.get(netID);
            if (ns == null) System.out.println("ns is null");
            if (ns.getCellInfo() == null) System.out.println("getCellInfo returned null");
            VarContext netContext = ns.getCellInfo().getContext();
            
            
            StringBuffer buf = new StringBuffer();
            buf.append(ns.getCellInfo().getContext().getInstPath(sep));  // append hier path if any
            if (!buf.toString().equals("")) buf.append(sep);
        	Iterator it = ns.getNet().getNames();
            if (it.hasNext()) {
    			buf.append((String) it.next());
    		} else {
        		buf.append("net"+netID.intValue());
            }
            return buf.toString();
        }
        
		/** Get the JNetwork that is closest to the root in the design
		 * hierarchy that corresponds to this netID. */
		public final NetDescription netIdToNetDescription(Integer netID) {
			return (NetDescription) netIdToNetDesc.get(netID);
		}

		/** Find position of NodeInst: ni in the root Cell. This is useful
		 * for flattening layout. */
		public final AffineTransform getPositionInRoot(NodeInst ni) {
			AffineTransform x = new AffineTransform(xformToRoot);
			x.concatenate(ni.getPositionAsTransform());
			return x;
		}

		/** Find position of Point2D: p in the root Cell. This is useful
		 * for flattening layout. */
		public final Point2D getPositionInRoot(Point2D p) {
			Point2D ans = new Point2D.Double();
			xformToRoot.transform(p, ans);
			return ans;
		}

		/** Find the rectangular bounds of rectangle: r in the root Cell.
		 * If this Cell instance is rotated by a multiple of 90 degrees in
		 * the root Cell then the result is also the position of
		 * rectangle: r mapped to the root Cell.  This is useful for
		 * flattening layout. */
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
	 * root. If this parameter is null then VarContext.globalContext is
	 * used.
	 * @param visitor the object responsible for doing something useful
	 * during the enumertion of the design hierarchy. */
	public static void enumerateCell(
		Cell root,
		VarContext context,
		Visitor visitor) {
		(new HierarchyEnumerator()).doIt(root, context, visitor);
	}
}
