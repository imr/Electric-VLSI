/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Cell.java
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

import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.technology.technologies.TecGeneric;
import com.sun.electric.database.text.CellName;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.text.DateFormat;

/**
 * A Cell is a non-primitive NodeProto.  It consists of an internal
 * set of nodes, arcs, connections, and networks.  Through its
 * NodeProto-ness, it projects a set of ports to the rest of the world.
 * A Cell is a member of a cell with a particular view, and has a
 * version number.  It knows about the most recent version of itself,
 * which may be itself.
 */
public class Cell extends NodeProto
{
	// ------------------------- private classes -----------------------------

	private class VersionGroup
	{
		List versions;
		public VersionGroup(Cell f)
		{
			versions = new ArrayList();
			add(f);
		}
		public void add(Cell f)
		{
			versions.add(f);
			f.versionGroup = this;
		}
		public void remove(Cell f)
		{
			versions.remove(f);
		}
		public int size()
		{
			return versions.size();
		}
		public Iterator iterator()
		{
			return versions.iterator();
		}
	}

	// -------------------------- private data ---------------------------------
	private static final Point2D.Double ORIGIN = new Point2D.Double(0, 0);
	private static int currentTime = 0;

	/** best guess technology */					private Technology tech;
	/** what group this is a cell of */				private CellGroup cellGroup;
	/** what history this is a cell of */			private VersionGroup versionGroup;
	/** what library this is a cell of */			private Library lib;
	/** what type of view this cell expresses */	private View view;
	/** when this cell was created */				private Date creationDate;
	/** when this cell was last modified */			private Date revisionDate;
	/** version of this cell */						private int version;
	/** cell-Center */								private NodeInst referencePointNode = null;
	/** cell-Center */								private Point2D.Double referencePointCoord;
	/** essential-bounds */							private List essenBounds = new ArrayList();
	/** NodeInsts that comprise this cell */		private List nodes;
	/** ArcInsts that comprise this cell */			private List arcs;
	/** time stamp for marking */					private int timeStamp;
	/** the bounds of the Cell */					private Rectangle2D.Double elecBounds;
	/** whether the bounds need to be recomputed */	private boolean boundsDirty;
	/** whether the bounds have anything in them */	private boolean boundsEmpty;
	
	

	// ------------------ protected and private methods -----------------------

	private Cell()
	{
		this.cellGroup = new CellGroup(this);
		this.versionGroup = new VersionGroup(this);
	}

	/**
	 * Low-level access routine to create a cell in library "lib".
	 */
	public static Cell lowLevelAllocate(Library lib)
	{
		Cell c = new Cell();
		c.nodes = new ArrayList();
		c.arcs = new ArrayList();
		c.timeStamp = -1; // initial time is in the past
		c.tech = null;
		c.lib = lib;
		c.referencePointCoord = new Point2D.Double(0, 0);
		c.creationDate = new Date();
		c.revisionDate = new Date();
		c.userBits = 0;
		c.elecBounds = new Rectangle2D.Double();
		c.boundsEmpty = true;
		c.boundsDirty = false;
		return c;
	}

	/**
	 * Low-level access routine to fill-in the cell name.
	 * Returns true on error.
	 */
	public boolean lowLevelPopulate(String name)
	{
		// see if this cell already exists
		Library lib = getLibrary();
		Cell existingCell = lib.findNodeProto(name);
		if (existingCell != null)
		{
			System.out.println("Cannot create cell " + name + " in library " + lib.getLibName() + " ...already exists");
			return true;
		}

		CellName n = CellName.parseName(name);
		if (n == null) return true;
		int version = n.getVersion();

		// make sure this version isn't in use
		if (version > 0)
		{
			for (Iterator it = lib.getCells(); it.hasNext();)
			{
				Cell c = (Cell) it.next();
				if (n.getName().equals(c.getProtoName()) && n.getView() == c.getView() &&
					version == c.getVersion())
				{
					System.out.println("Already a cell with this version");
					return true;
				}
			}
		} else
		{
			// find a new version
			version = 1;
			for (Iterator it = lib.getCells(); it.hasNext();)
			{
				Cell c = (Cell) it.next();
				if (n.getName().equals(c.getProtoName()) && n.getView() == c.getView() &&
					c.getVersion() >= version)
						version = c.getVersion() + 1;
			}
		}
		
		// fill-in the fields
		this.protoName = n.getName();
		this.view = n.getView();
		this.version = version;
		return false;
	}

	/**
	 * Low-level access routine to link a cell into its library.
	 * Returns true on error.
	 */
	public boolean lowLevelLink()
	{
		// add ourselves to the library
		Library lib = getLibrary();
		lib.addCell(this);

		// add to cell group
//		cellGroup.merge(nxtCellGrp);
		return false;
	}

	/**
	 * Create a new Cell in library "lib" named "name".
	 * The name should be something like "foo;2{sch}".
	 */
	public static Cell newInstance(Library lib, String name)
	{
		Cell theCell = lowLevelAllocate(lib);
		if (theCell.lowLevelPopulate(name)) return null;
		if (theCell.lowLevelLink()) return null;
		return theCell;
	}

	public void remove()
	{
		// remove ourselves from the cellGroup.
		// TODO: should this also remove the cell from the library?
		cellGroup.remove(this);
		versionGroup.remove(this);
		lib.removeCell(this); // remove ourselves from the library
//		removeAll(nodes); // kill nodes

		// arcs should have been killed by ditching the nodes
		if (arcs.size() != 0)
			System.out.println("Arcs should have been removed when the nodes were killed");

		super.remove();
	}

	public void addArc(ArcInst a)
	{
		if (arcs.contains(a))
		{
			System.out.println("Cell " + this +" already contains arc " + a);
			return;
		}
		arcs.add(a);

		// must recompute the bounds of the cell
		boundsDirty = true;
	}

	public void removeArc(ArcInst a)
	{
		if (!arcs.contains(a))
		{
			System.out.println("Cell " + this +" doesn't contain arc " + a);
			return;
		}
		arcs.remove(a);

		// must recompute the bounds of the cell
		boundsDirty = true;
	}

	/**
	 * Routine to add node instance "ni" to the list of nodes in this cell
	 */
	public void addNode(NodeInst ni)
	{
		// error check
		if (nodes.contains(ni))
		{
			System.out.println("Cell " + this +" already contains node inst " + ni);
			return;
		}

		// add the node
		nodes.add(ni);

		// must recompute the bounds of the cell
		boundsDirty = true;

		// make additional checks to keep circuit up-to-date
		NodeProto np = ni.getProto();
		if (np instanceof PrimitiveNode && np == TecGeneric.tech.cellCenter_node)
		{
			referencePointNode = ni;
			setReferencePoint(ni.getCenterX(), ni.getCenterY());
		}
		if (np instanceof PrimitiveNode
			&& np.getProtoName().equals("Essential-Bounds"))
		{
			essenBounds.add(ni);
		}
	}

	public void removeNode(NodeInst ni)
	{
		if (!nodes.contains(ni))
		{
			System.out.println("Cell " + this +" doesn't contain node inst " + ni);
			return;
		}
		nodes.remove(ni);

		// must recompute the bounds of the cell
		boundsDirty = true;

		if (ni == referencePointNode)
			referencePointNode = null;
		essenBounds.remove(ni);
	}

	private HashMap buildConnPortsTable(ArrayList connPortsLists)
	{
		HashMap connPorts = new HashMap();
		if (connPortsLists == null)
			return connPorts;

		// iterate over all lists
		for (int i = 0; i < connPortsLists.size(); i++)
		{
			ArrayList connPortsList = (ArrayList) connPortsLists.get(i);

			// all these PortProtos are shorted together
			JNetwork dummyNet = new JNetwork(null);
			NodeProto parent = null;
			for (int j = 0; j < connPortsList.size(); j++)
			{
				PortProto pp = (PortProto) connPortsList.get(j);

				// make sure all connected ports have the same parent
				if (j == 0)
					parent = pp.getParent();
				if (pp.getParent() != parent)
				{
					System.out.println("PortProtos in the same connected" + " list must belong to same NodeProto");
					return null;
				}

				// make sure it's not already present
				if (connPorts.containsKey(pp))
				{
					System.out.println("PortProto occurs more than once in the connected Ports lists");
					return null;
				}

				connPorts.put(pp, dummyNet);
			}
		}
		return connPorts;
	}

	private void redoDescendents(HashMap equivPorts)
	{
		for (int i = 0; i < nodes.size(); i++)
		{
			NodeInst ni = (NodeInst) nodes.get(i);
			NodeProto np = ni.getProto();

			if (np instanceof Cell)
			{
				if (ni.isIconOfParent())
					continue;

				// If Cell is an Icon View then redo the Schematic
				// View. Otherwise redo the Cell.
				Cell equivCell = (Cell) np.getEquivalent();

				// if an icon has no corresponding schematic then equivCell==null
				if (equivCell != null)
					equivCell.redoNetworks(equivPorts);
			}
		}
	}

	private void placeEachPortInstOnItsOwnNet()
	{
		for (int i = 0; i < nodes.size(); i++)
		{
			NodeInst ni = (NodeInst) nodes.get(i);
			for (Iterator it = ni.getPortInsts(); it.hasNext();)
			{
				PortInst pi = (PortInst) it.next();
				JNetwork net = new JNetwork(this);
				net.addPortInst(pi);
			}
		}
	}

	private void mergeNetsConnectedByArcs()
	{
		for (int i = 0; i < arcs.size(); i++)
		{
			ArcInst ai = (ArcInst) arcs.get(i);
			JNetwork n0 = ai.getConnection(false).getPortInst().getNetwork();
			JNetwork n1 = ai.getConnection(true).getPortInst().getNetwork();

			JNetwork merged = JNetwork.merge(n0, n1);
			merged.addName(ai.getName());
		}
	}

	private void addExportNamesToNets()
	{
		for (Iterator it = getPorts(); it.hasNext();)
		{
			Export e = (Export) it.next();
			String expNm = e.getProtoName();
			if (expNm == null)
			{
				System.out.println("Cell.addExportNamesToNet: Export with no name!");
				return;
			}
			e.getNetwork().addName(expNm);
		}
	}

	private void mergeNetsConnectedByNodeProtoSubnets()
	{
		for (int i = 0; i < nodes.size(); i++)
		{
			NodeInst ni = (NodeInst) nodes.get(i);

			if (ni.isIconOfParent())
				continue;

			HashMap netToPort = new HashMap(); // subNet -> PortInst
			for (Iterator it = ni.getPortInsts(); it.hasNext();)
			{
				PortInst piNew = (PortInst) it.next();
				JNetwork subNet =
					piNew.getPortProto().getEquivalent().getNetwork();

				if (subNet == null && ni.getProto() instanceof Cell)
				{
					System.out.println("Cell.mergeNets... : no subNet on Cell: "
						+ ni.getProto().getProtoName()
						+ " port: "
						+ piNew.getPortProto());
					return;
				}

				if (subNet != null)
				{
					PortInst piOld = (PortInst) netToPort.get(subNet);
					if (piOld != null)
					{
						JNetwork.merge(piOld.getNetwork(), piNew.getNetwork());
					} else
					{
						netToPort.put(subNet, piNew);
					}
				}
			}
		}
	}

	private void mergeNetsConnectedByUserEquivPorts(HashMap equivPorts)
	{
		for (int i = 0; i < nodes.size(); i++)
		{
			NodeInst ni = (NodeInst) nodes.get(i);

			if (ni.isIconOfParent())
				continue;

			HashMap listToPort = new HashMap(); // equivList -> PortInst
			for (Iterator it = ni.getPortInsts(); it.hasNext();)
			{
				PortInst piNew = (PortInst) it.next();
				Object equivList =
					equivPorts.get(piNew.getPortProto().getEquivalent());
				if (equivList != null)
				{
					PortInst piOld = (PortInst) listToPort.get(equivList);
					if (piOld != null)
					{
						JNetwork.merge(piOld.getNetwork(), piNew.getNetwork());
					} else
					{
						listToPort.put(equivList, piNew);
					}
				}
			}
		}
	}

	private HashSet getNetsFromPortInsts()
	{
		HashSet nets = new HashSet();
		for (Iterator nit = getNodes(); nit.hasNext();)
		{
			NodeInst ni = (NodeInst) nit.next();
			for (Iterator pit = ni.getPortInsts(); pit.hasNext();)
			{
				PortInst pi = (PortInst) pit.next();
				nets.add(pi.getNetwork());
			}
		}
		return nets;
	}

	// Find all nets (including this net!) connected by name.  Each net
	// will occur exactly once in set;
	private HashSet findSameNameNets(JNetwork net, HashMap nmTab)
	{
		HashSet conNets = new HashSet();
		conNets.add(net);
		for (Iterator it = net.getNames(); it.hasNext();)
		{
			String nm = (String) it.next();
			JNetwork oldNet = (JNetwork) nmTab.get(nm);
			if (oldNet != null)
				conNets.add(oldNet);
		}
		return conNets;
	}

	// Merge all JNetworks with the same name into one big net.
	// Warning: this doesn't handle busses correctly because we don't
	// properly forward JNetworks pointed to by other JNetworks.
	private void mergeSameNameNets()
	{
		HashSet nets = getNetsFromPortInsts();
		HashMap nmTab = new HashMap();

		for (Iterator netIt = nets.iterator(); netIt.hasNext();)
		{
			JNetwork net = (JNetwork) netIt.next();

			JNetwork merged = JNetwork.merge(findSameNameNets(net, nmTab));

			// Net has gained names from merged nets. Update name table with
			// all names at once.  Name table invariant: if one of a net's
			// names point to the net then all of the net's names points to
			// the net.
			for (Iterator nmIt = merged.getNames(); nmIt.hasNext();)
			{
				nmTab.put((String) nmIt.next(), merged);
			}
		}
	}

	private void buildNetworkList()
	{
		removeAllNetworks();
		for (Iterator it = getNetsFromPortInsts().iterator(); it.hasNext();)
		{
			addNetwork((JNetwork) it.next());
		}
	}

	private void redoNetworks(HashMap equivPorts)
	{
		if (timeStamp == currentTime)
			return;
		timeStamp = currentTime;

		redoDescendents(equivPorts);
		placeEachPortInstOnItsOwnNet();
		mergeNetsConnectedByArcs();
		mergeNetsConnectedByNodeProtoSubnets();
		mergeNetsConnectedByUserEquivPorts(equivPorts);
		addExportNamesToNets();
		mergeSameNameNets();
		buildNetworkList();
	}

	/** Get the Electric bounds.  This excludes invisible widths. Base
	 * units */
	public Rectangle2D getBounds()
	{
		if (boundsDirty)
		{
			// recompute bounds
			double cellLowX, cellHighX, cellLowY, cellHighY;
			boundsEmpty = true;
			cellLowX = cellHighX = cellLowY = cellHighY = 0;

			for(Iterator it = nodes.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst) it.next();
				double xOffset = ni.getXSize()/2;
				double xCenter = ni.getCenterX();
				double yOffset = ni.getYSize()/2;
				double yCenter = ni.getCenterY();
				double lowx = xCenter - xOffset;
				double highx = xCenter + xOffset;
				double lowy = yCenter - yOffset;
				double highy = yCenter + yOffset;
				if (boundsEmpty)
				{
					boundsEmpty = false;
					cellLowX = lowx;   cellHighX = highx;
					cellLowY = lowy;   cellHighY = highy;
				} else
				{
					if (lowx < cellLowX) cellLowX = lowx;
					if (highx > cellHighX) cellHighX = highx;
					if (lowy < cellLowY) cellLowY = lowy;
					if (highy > cellHighY) cellHighY = highy;
				}
			}
			elecBounds.x = cellLowX;
			elecBounds.width = cellHighX - cellLowX;
			elecBounds.y = cellLowY;
			elecBounds.height = cellHighY - cellLowY;
			boundsDirty = false;
		}

		return new Rectangle2D.Double(
			elecBounds.x,
			elecBounds.y,
			elecBounds.width,
			elecBounds.height);
	}

	public double getDefWidth() { return elecBounds.width; }
	public double getDefHeight() { return elecBounds.height; }

	public double getLowXOffset() { return 0; }
	public double getHighXOffset() { return 0; }
	public double getLowYOffset() { return 0; }
	public double getHighYOffset() { return 0; }

	/** If there are two or more essential bounds return the bounding
	 * box that surrounds all of them; otherwise return null; */
	Rectangle2D.Double findEssentialBounds()
	{
		if (essenBounds.size() < 2)
			return null;
		double minX = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE;
		double maxY = Double.MIN_VALUE;

		for (int i = 0; i < essenBounds.size(); i++)
		{
			NodeInst ni = (NodeInst) essenBounds.get(i);
			minX = Math.min(minX, ni.getCenterX());
			maxX = Math.max(maxX, ni.getCenterX());
			minY = Math.min(minY, ni.getCenterY());
			maxY = Math.max(maxY, ni.getCenterY());
		}

		return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
	}

	private void copyReferencePoint(Cell f)
	{
		Point2D rp = getReferencePoint();
		f.setReferencePoint(rp.getX(), rp.getY());
	}

	private HashMap copyNodes(Cell f)
	{
		HashMap oldToNew = new HashMap();
		for (int i = 0; i < nodes.size(); i++)
		{
			NodeInst oldInst = (NodeInst) nodes.get(i);
			NodeProto oldProto = oldInst.getProto();
			if (oldProto instanceof PrimitiveNode
				&& oldProto.getProtoName().equals("Cell-Center"))
			{
				// Cell-Center already handled by copyReferencePoint
			} else
			{
				NodeInst newInst =
					NodeInst.newInstance(oldProto,new Point2D.Double(oldInst.getCenterX(), oldInst.getCenterY()),
						oldInst.getXSize(), oldInst.getYSize(), oldInst.getAngle(), f);
				String nm = oldInst.getName();
				if (nm != null)
					newInst.setName(nm);
				if (oldToNew.containsKey(oldInst))
				{
					System.out.println("oldInst already in oldToNew?!");
					return null;
				}
				oldToNew.put(oldInst, newInst);
			}
		}
		return oldToNew;
	}

	private PortInst getNewPortInst(PortInst oldPort, HashMap oldToNew)
	{
		NodeInst newInst = (NodeInst) oldToNew.get(oldPort.getNodeInst());
		if (newInst == null)
		{
			System.out.println( "no new instance for old instance in oldToNew?!");
			return null;
		}
		String portNm = oldPort.getPortProto().getProtoName();
		if (portNm == null)
		{
			System.out.println("PortProto with no name?");
			return null;
		}
		return newInst.findPortInst(portNm);
	}

	private void copyArcs(Cell f, HashMap oldToNew)
	{
		for (int i = 0; i < arcs.size(); i++)
		{
			ArcInst ai = (ArcInst) arcs.get(i);
			Connection c0 = ai.getConnection(false);
			Connection c1 = ai.getConnection(true);
			Point2D p0 = c0.getLocation();
			Point2D p1 = c1.getLocation();
			ArcInst.newInstance(ai.getProto(), ai.getWidth(),
				getNewPortInst(c0.getPortInst(), oldToNew), p0.getX(), p0.getY(),
				getNewPortInst(c1.getPortInst(), oldToNew), p1.getX(), p1.getY());
		}
	}
	private void copyExports(Cell f, HashMap oldToNew)
	{
		for (Iterator it = getPorts(); it.hasNext();)
		{
			Export e = (Export) it.next();
			PortInst newPort = getNewPortInst(e.getOriginalPort(), oldToNew);
			if (newPort == null)
			{
				System.out.println("can't find new PortInst to export");
				return;
			}
			Export.newInstance(f, e.getOriginalNode(), newPort, e.getProtoName());
		}
	}

	private void copyContents(Cell f)
	{
		// Note: Electric has already created f and called f.init()
		copyReferencePoint(f);
		HashMap oldToNew = copyNodes(f);
		copyArcs(f, oldToNew);
		copyExports(f, oldToNew);
	}

	public void getInfo()
	{
		System.out.println("--------- CELL: ---------");
		System.out.println("  name= " + protoName);
		System.out.println("  tech= " + tech);
		System.out.println("  view= " + view);
		System.out.println("  version= " + version);
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
		System.out.println("  creationDate= " + df.format(creationDate));
		System.out.println("  revisionDate= " + df.format(revisionDate));
		System.out.println("  newestVersion= " + getNewestVersion());
//		System.out.println("  userBits= " + Integer.toHexString(userBits));
		Rectangle2D rect = getBounds();
		System.out.println("  location: (" + rect.getX() + "," + rect.getY() + "), at: " + rect.getWidth() + "x" + rect.getHeight());
		System.out.println("  nodes (" + nodes.size() + "):");
		for (int i = 0; i < nodes.size(); i++)
		{
			if (i > 20)
			{
				System.out.println("     ...");
				break;
			}
			System.out.println("     " + nodes.get(i));
		}
		System.out.println("  arcs (" + arcs.size() + "):");
		for (int i = 0; i < arcs.size(); i++)
		{
			if (i > 20)
			{
				System.out.println("     ...");
				break;
			}
			System.out.println("     " + arcs.get(i));
		}
		super.getInfo();
	}

	// --------------------------- public types -----------------------------

	/** A CellGroup contains related cells. This includes different
	 * Views of a cell (e.g. the schematic, layout, and icon Views),
	 * alternative icons, all the parts of a multi-part icon */
	public class CellGroup
	{
		// private data
		ArrayList cells;
		// private and protected methods
		CellGroup(Cell f)
		{
			cells = new ArrayList();
			add(f);
		}
		void add(Cell f)
		{
			cells.add(f);
			f.cellGroup = this;
		}
		void remove(Cell f)
		{
			cells.remove(f);
		}

		// merge f's cell group into me
		void merge(Cell f)
		{
			CellGroup fg = f.cellGroup;
			if (fg == this)
				return; // we are the same group

			for (Iterator it = fg.getCells(); it.hasNext();)
				add((Cell) it.next());
		}
		// public methods
		/** Return a list of all the Cells that are in this Cell's CellGroup */
		public Iterator getCells()
		{
			return cells.iterator();
		}
	}

	// -------------------------- public constants -------------------------
	/** This constant can be passed to <code>rebuildNetworks()</code> in
	 * order to treat resistors as short circuits */
	public static final ArrayList SHORT_RESISTORS = new ArrayList();

	// ------------------------- public methods -----------------------------
	public CellGroup getCellGroup() { return cellGroup; }
	public Library getLibrary() { return lib; }
	public View getView() { return view; }

	public Date getCreationDate() { return creationDate; }
	public void lowLevelSetCreationDate(Date creationDate) { this.creationDate = creationDate; }
	public Date getRevisionDate() { return revisionDate; }
	public void lowLevelSetRevisionDate(Date revisionDate) { this.revisionDate = revisionDate; }

//	/** Create an export for this Cell.
//	 * @param name the name of the new Export
//	 * @param role the Export's type 
//	 * @param port the PortInst that will be exported */
//	public Export newExport(String name, PortInst port)
//	{
//		/* RKao: Why do we care that export name has both '[' and '_' ?
//		   if ((name.indexOf('[')>=0) && (name.indexOf('_')>=0)) {
//		   System.out.println("Oops:  tried to create an export called "+
//		   name);
//		   return null;
//		   }
//		*/
////		Export e = Electric.newPortProto(this.getAddr(), port.getNodeInst().getAddr(), port.getPortProto().getAddr(), name);
////		e.setRole(role);
////		return e;
//		return null;
//	}

	/** Get Export with specified name. @return null if not found */
	public Export findExport(String nm)
	{
		return (Export) findPortProto(nm);
	}

	/** Return an iterator over the NodeInsts of this Cell */
	public Iterator getNodes()
	{
		return nodes.iterator();
	}

	/** Return the number of NodeInsts in this Cell */
	public int getNumNodes()
	{
		return nodes.size();
	}

	/** Return a NodeInst with specified name */
	public NodeInst findNode(String nm)
	{
		for (int i = 0; i < nodes.size(); i++)
		{
			NodeInst ni = (NodeInst) nodes.get(i);
			String nodeNm = ni.getName();
			if (nodeNm != null && nodeNm.equals(nm))
				return ni;
		}
		return null;
	}

	/** Return an iterator over the ArcInsts of this Cell */
	public Iterator getArcs()
	{
		return arcs.iterator();
	}

	/** Return the number of ArcInsts in this Cell */
	public int getNumArcs()
	{
		return arcs.size();
	}

	/** Return the version number of this Cell */
	public int getVersion() { return version; }

	/** Get an ordered array of the versions of this Cell.  NOTE: the
	 * array is sorted, but the version number may have little to do
	 * with the Cell's index in the array. */
	public Iterator getVersions()
	{
		return versionGroup.iterator();
	}

	/** Get the most recent version of this Cell. */
	public Cell getNewestVersion()
	{
		return (Cell) getVersions().next();
	}

	/**
	 * Return a name of the form: cell;version{view}
	 * If the cell is not from the current library, prepend the library name.
	 */
	public String describe()
	{
		String name = "";
		if (lib != Library.getCurrent())
			name += lib.getLibName() + ":";
		name += noLibDescribe();
		return name;
	}

	/**
	 * Return a name of the form: cell;version{view}
	 */
	public String noLibDescribe()
	{
		String name = protoName;
		if (getNewestVersion() != this)
			name += ";" + version;
		if (view != null)
			name += "{" +  view.getShortName() + "}";
		return name;
	}

	/** Recompute the network structure for this Cell.
	 *
	 * @param connectedPorts this argument allows the user to tell the
	 * network builder to treat certain PortProtos of a NodeProto as a
	 * short circuit. For example, it is sometimes useful to build the
	 * net list as if the PortProtos of a resistor where shorted
	 * together.
	 *
	 * <p> <code>connectedPorts</code> must be either null or an
	 * ArrayList of ArrayLists of PortProtos.  All of the PortProtos in
	 * an ArrayList are treated as if they are connected.  All of the
	 * PortProtos in a single ArrayList must belong to the same
	 * NodeProto. */
	public void rebuildNetworks(ArrayList connectedPorts)
	{
		if (connectedPorts == null)
		{
			connectedPorts = new ArrayList();
		}
		HashMap connPorts = buildConnPortsTable(connectedPorts);
		currentTime++;
		redoNetworks(connPorts);
	}

	/** If this is an Icon View Cell then return the newest version of
	 * the corresponding Schematic View Cell.  If an Icon View Cell
	 * has no corresponding Schematic View Cell then return null. */
	public NodeProto getEquivalent()
	{
		if (!view.getFullName().equals("icon"))
		{
			return this;
		}

		View sch = View.getView("schematic");
		for (Iterator it = cellGroup.getCells(); it.hasNext();)
		{
			Cell f = (Cell) it.next();
			if (f.getView() == sch)
				return f.getNewestVersion();
		}
		return null;
	}

	/** If this Cell contains an instance of the Cell-Center then
	 * return its absolute coordinates; otherwise return (0, 0).
	 */
	public Point2D.Double getReferencePoint()
	{
		return referencePointCoord;
	}

	/** If there is no instance of Cell-Center then create one.
	 * Position the Cell-Center at the absolute coordinates: (x,
	 * y). From now on, all positions in this Cell will be interpreted
	 * relative to the position of the Cell-Center. */
	public void setReferencePoint(double x, double y)
	{
//		if (referencePointNode == null)
//		{
//			Technology generic = Technology.findTechnology("generic");
//			error(generic == null, "can't find generic technlogy?");
//			PrimitiveNode cellCenter = generic.findNodeProto("Cell-Center");
//			error(cellCenter == null,
//				"can't find PrimitiveNode: Cell-Center");
//			referencePointNode = NodeInst.newInstance(cellCenter, new Point2D.Double(1, 1), 0, 0, 0, this);
//		}
//		referencePointNode.alterShape(0, 0, x - r.getX(), y - r.getY(), 0);
//		referencePointNode.setHardSelect();

		referencePointCoord.setLocation(x, y);
	}
	/** sanity check method used by Geometric.checkobj */
	public boolean containsInstance(Geometric thing)
	{
		if (thing instanceof ArcInst)
		{
			return arcs.contains(thing);
		} else if (thing instanceof NodeInst)
		{
			return nodes.contains(thing);
		} else
		{
			return false;
		}
	}
	
	public String toString()
	{
		return "Cell " + describe();
	}

	/** Create a copy of this Cell. Warning: this routine doesn't yet
	 * properly copy all variables on all objects.
	 * @param copyLib library into which the copy is placed. null means
	 * place the copy into the library that contains this Cell.
	 * @param copyNm name of the copy
	 * @return the copy */
//	public Cell copy(Library copyLib, String copyNm)
//	{
//		if (copyLib == null)
//			copyLib = lib;
//		error(copyNm == null, "Cell.makeCopy: copyNm is null");
//		Cell f = copyLib.newCell(copyNm);
//		error(f == null, "unable to create copy Cell named: " + copyNm);
//		copyContents(f);
//		return f;
//	}

	/** Create an export for a particular layer.
	 *
	 * <p> At the coordinates <code>(x, y)</code> create an instance of
	 * a pin for the layer <code>ap</code>. Export that layer-pin's
	 * PortInst.
	 *
	 * <p> Attach an arc to the layer-pin.  This is done because
	 * Electric uses the widest arc on a PortInst as a hint for the
	 * width to use for all future arcs. Because Electric doesn't use
	 * the size of layer-pins as width hints, the layer-pin is created
	 * in it's default size.
	 *
	 * <p> This method seems very specialized, but it's nearly the only
	 * one I use when generating layout.
	 * @param name the name of the new Export
	 * @param role the Export's type 
	 * @param ap the ArcProto indicating what layer I want to create an
	 * export on.
	 * @param hintW width of the arc hint
	 * @param x the x coordinate of the layer pins.
	 * @param y the y coordinate of the layer pin. */
//	public Export newExport(String name, ArcProto ap, double w, double x, double y)
//	{
//		NodeProto np = ap.findPinProto();
//		error(np == null, "Cell.newExport: This layer has no layer-pin");
//
//		NodeInst ni = NodeInst.newInstance(np, new Point2D.Double(1, 1), x, y, 0, this);
//		ArcInst.newInstance(ap, w, ni.getPort(), ni.getPort());
//
//		return newExport(name, ni.getPort());
//	}

}
