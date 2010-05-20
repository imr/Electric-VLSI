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
package com.sun.electric.database.hierarchy;

import static com.sun.electric.database.hierarchy.HierarchyEnumerator.buildPortMap;

import java.awt.geom.AffineTransform;
import java.util.Iterator;

import com.sun.electric.database.hierarchy.HierarchyEnumerator.Visitor;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.util.concurrent.PJob;
import com.sun.electric.util.concurrent.PoolExistsException;
import com.sun.electric.util.concurrent.Task;
import com.sun.electric.util.concurrent.ThreadPool;

/**
 * The HierarchyEnumerator can help programs that need to "flatten" the design
 * hierarchy. Examples of such programs include the logical effort engine and
 * routers.
 * 
 * <p>
 * The HierarchyEnumerator performs a recursive descent of the
 * "completely expanded" design hierarchy. The HierarchyEnumerator brings the
 * Visitor along with it during the excursion. The HierarchyEnumerator doesn't
 * build a flattened data structure, that's the prerogative of the Visitor. The
 * HierarchyEnumerator simply invokes Visitor methods for each Cell instance and
 * NodeInst.
 * 
 * <p>
 * The following example illustrates the notion of "completely expanded".
 * Suppose the root Cell instantiates Cell A twice, and Cell A instantiates Cell
 * B twice. Then the HierarchyEnumerator visits two instances of Cell A and four
 * instances of Cell B.
 */
public final class HierarchyEnumeratorParallel extends HierarchyEnumerator {

	protected HierarchyEnumeratorParallel() {
	}

	private class EnumerateCellTask extends Task {

		private Nodable parentInst;
		private Cell cell;
		private VarContext context;
		private Netlist netlist;
		private int[][] portNdxToNetIDs;
		private AffineTransform xformToRoot;
		private CellInfo parent;

		public EnumerateCellTask(PJob job, Nodable parentInst, Cell cell, VarContext context,
				Netlist netlist, int[][] portNdxToNetIDs, AffineTransform xformToRoot, CellInfo parent) {
			super(job);
			this.parentInst = parentInst;
			this.cell = cell;
			this.context = context;
			this.netlist = netlist;
			this.portNdxToNetIDs = portNdxToNetIDs;
			this.xformToRoot = xformToRoot;
			this.parent = parent;
		}

		@Override
		public void execute() {
			CellInfo info = visitor.newCellInfo();

			int firstNetID = curNetId;
			int[] netNdxToNetID = numberNets(cell, netlist, portNdxToNetIDs, info);
			int lastNetIDPlusOne = curNetId;
			cellCnt++;
			info.init(parentInst, cell, context, netlist, netNdxToNetID, portNdxToNetIDs, xformToRoot,
					netIdToNetDesc, parent);

			boolean enumInsts = visitor.enterCell(info);
			if (!enumInsts) {
				return;
			}

			for (Iterator<Nodable> it = netlist.getNodables(); it.hasNext();) {
				Nodable ni = it.next();

				instCnt++;
				boolean descend = visitor.visitNodeInst(ni, info);
				NodeProto np = ni.getProto();
				if (descend && ni.isCellInstance() && !((Cell) np).isIcon()) {
					int[][] portNmToNetIDs2 = buildPortMap(netlist, ni, netNdxToNetID);
					AffineTransform xformToRoot2 = xformToRoot;
					if (ni instanceof NodeInst) {
						// add transformation from lower level
						xformToRoot2 = new AffineTransform(xformToRoot);
						xformToRoot2.concatenate(((NodeInst) ni).rotateOut());
						xformToRoot2.concatenate(((NodeInst) ni).translateOut());
					}
					// enumerateCell(ni, (Cell) np, caching ?
					// context.pushCaching(ni) : context.push(ni),
					// netlist.getNetlist(ni), portNmToNetIDs2, xformToRoot2,
					// info);
					job.add(new EnumerateCellTask(job, ni, (Cell) np, caching ? context.pushCaching(ni)
							: context.push(ni), netlist.getNetlist(ni), portNmToNetIDs2, xformToRoot2, info));
				}
			}

			visitor.exitCell(info);

			// release storage associated with VarContext variable cache
			context.deleteVariableCache();

			// remove entries in netIdToNetDesc that we'll never use again
			for (int i = firstNetID; i < lastNetIDPlusOne; i++) {
				netIdToNetDesc.remove(i);
			}

		}

	}

	/** portNdxToNetIDs translates an Export's index to an array of NetIDs */
	private void enumerateCell(Nodable parentInst, Cell cell, VarContext context, Netlist netlist,
			int[][] portNdxToNetIDs, AffineTransform xformToRoot, CellInfo parent) {

		try {
			ThreadPool.initialize();
		} catch (PoolExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		PJob enumerateJob = new PJob();
		enumerateJob.add(new EnumerateCellTask(enumerateJob, parentInst, cell, context, netlist,
				portNdxToNetIDs, xformToRoot, parent));

		enumerateJob.execute();
		
		return;

	}

	protected void doIt(Cell root, VarContext context, Netlist netlist, Visitor visitor, boolean cache) {
		this.visitor = visitor;
		this.caching = cache;
		if (context == null) {
			context = VarContext.globalContext;
		}
		int[][] exportNdxToNetIDs = null;
		enumerateCell(null, root, context, netlist, exportNdxToNetIDs, new AffineTransform(), null);

		// System.out.println("A total of: " + curNetId +
		// " nets were numbered");
		// System.out.println("A total of: " + cellCnt + " Cells were visited");
		// System.out.println("A total of: " + instCnt +
		// " NodeInsts were visited");
	}

	public static void enumerateCell(Cell root, VarContext context, Visitor visitor) {
		enumerateCell(root, context, visitor, Netlist.ShortResistors.NO);
	}

	public static void enumerateCell(Cell root, VarContext context, Visitor visitor,
			Netlist.ShortResistors shortResistors) {
		enumerateCell(root.getNetlist(shortResistors), context, visitor);
	}

	public static void enumerateCell(Netlist rootNetlist, VarContext context, Visitor visitor) {
		enumerateCell(rootNetlist, context, visitor, false);
	}

	/** Experimental. Optionally caches results of variable evaluation. */
	public static void enumerateCell(Netlist rootNetlist, VarContext context, Visitor visitor, boolean caching) {
		Netlist.ShortResistors shortResistors = rootNetlist.getShortResistors();
		(new HierarchyEnumeratorParallel()).doIt(rootNetlist.getCell(), context, rootNetlist, visitor,
				caching);
	}
}