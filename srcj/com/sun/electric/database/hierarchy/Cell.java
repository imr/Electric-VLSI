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
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.text.CellName;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.technologies.Generic;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.text.DateFormat;

/**
 * A Cell is a non-primitive NodeProto.
 * Besides the information that it inherits from NodeProto, the Cell holds a
 * set of nodes, arcs, and networks.
 * The exported ports on NodeInsts inside of this cell become the Exports
 * of this Cell.
 * A Cell also has a specific view and version number.
 * <P>
 * Cells belong to VersionGroup objects, which list all of the versions of
 * the cell.  Only the most recent version of any cell is referenced in
 * lists of cells.
 * A Cell knows about the most recent version of itself, which may be itself.
 * <P>
 * Cells also belong to CellGroup objects, which gather related cells together.
 * <P>
 * <CENTER><IMG SRC="doc-files/Cell-2.gif"></CENTER>
 * <P>
 * A Cell can have different views and versions, each of which is a cell.
 * The library shown here has two cells (“gate” and “twogate”), each of which has many
 * views (layout, schematics, icon, vhdl) and versions:
 * <P>
 * <CENTER><IMG SRC="doc-files/Cell-1.gif"></CENTER>
 */
public class Cell extends NodeProto
{
	// ------------------------- private classes -----------------------------

	/**
	 * A CellGroup contains a list of cells that are related.
	 * This includes different Views of a cell (e.g. the schematic, layout, and icon Views),
	 * alternative icons, all the parts of a multi-part icon.
	 * Only the most recent version of a cell is in the CellGroup.  You must
	 * explore the Cell's VersionGroup to find old versions.
	 */
	public static class CellGroup
	{
		// private data
		private ArrayList cells;

		/**
		 * Constructs a <CODE>CellGroup</CODE>.
		 */
		public CellGroup()
		{
			cells = new ArrayList();
		}

		/**
		 * Routine to add a Cell to this CellGroup.
		 * @param cell the cell to add to this CellGroup.
		 */
		void add(Cell cell)
		{
			cells.add(cell);
			cell.cellGroup = this;
		}

		/**
		 * Routine to remove a Cell from this CellGroup.
		 * @param cell the cell to remove from this CellGroup.
		 */
		void remove(Cell f) { cells.remove(f); }

		/**
		 * Routine to return an Iterator over all the Cells that are in this CellGroup.
		 * @return an Iterator over all the Cells that are in this CellGroup.
		 */
		public Iterator getCells() { return cells.iterator(); }

		public String toString() { return "CELLGROUP"; }
	}

	private static class VersionGroup
	{
		// private data
		private List versions;

		/**
		 * Constructs a <CODE>VersionGroup</CODE> that contains a Cell.
		 * @param cell the cell to initially add to this VersionGroup.
		 */
		public VersionGroup(Cell cell)
		{
			versions = new ArrayList();
			add(cell);
		}

		/**
		 * Routine to add a Cell to this VersionGroup.
		 * @param cell the cell to add to this VersionGroup.
		 */
		public void add(Cell cell)
		{
			versions.add(cell);
			cell.versionGroup = this;
		}

		/**
		 * Routine to remove a Cell from this VersionGroup.
		 * @param cell the cell to remove from this VersionGroup.
		 */
		public void remove(Cell cell) { versions.remove(cell); }

		/**
		 * Routine to return the number of Cells in this VersionGroup.
		 * @return the number of Cells in this VersionGroup.
		 */
		public int size() { return versions.size(); }

		/**
		 * Routine to return an Iterator over all the Cells that are in this VersionGroup.
		 * @return an Iterator over all the Cells that are in this VersionGroup.
		 */
		public Iterator iterator() { return versions.iterator(); }
	}

	// -------------------------- private data ---------------------------------

	/** The technology of this Cell. */								private Technology tech;
	/** The CellGroup this Cell belongs to. */						private CellGroup cellGroup;
	/** The VersionGroup this Cell belongs to. */					private VersionGroup versionGroup;
	/** The library this Cell belongs to. */						private Library lib;
	/** This Cell's View. */										private View view;
	/** The date this Cell was created. */							private Date creationDate;
	/** The date this Cell was last modified. */					private Date revisionDate;
	/** The version of this Cell. */								private int version;
	/** The NodeInst in this cell that defines the cell center. */	private NodeInst referencePointNode;
	/** The Cell's essential-bounds. */								private List essenBounds = new ArrayList();
	/** A list of NodeInsts in this Cell. */						private List nodes;
	/** A list of ArcInsts in this Cell. */							private List arcs;
	/** The current timestamp value. */								private static int currentTime = 0;
	/** The timestamp of last network renumbering of this Cell */	private int networksTime;
	/** The bounds of the Cell. */									private Rectangle2D.Double elecBounds;
	/** Whether the bounds need to be recomputed. */				private boolean boundsDirty;
	/** Whether the bounds have anything in them. */				private boolean boundsEmpty;
	/** The geometric data structure. */							private Geometric.RTNode rTree;

	// ------------------ protected and private methods -----------------------

	/**
	 * This constructor should not be called.
	 * Use the factory "newInstance" to create a Cell.
	 */
	private Cell()
	{
		this.versionGroup = new VersionGroup(this);
	}

	/**
	 * Low-level access routine to create a cell in library "lib".
	 * Unless you know what you are doing, do not use this method.
	 * @param lib library in which to place this cell.
	 * @return the newly created cell.
	 */
	public static Cell lowLevelAllocate(Library lib)
	{
		Cell c = new Cell();
		c.nodes = new ArrayList();
		c.arcs = new ArrayList();
		c.cellGroup = null;
		c.tech = null;
		c.lib = lib;
		c.creationDate = new Date();
		c.revisionDate = new Date();
		c.userBits = 0;
		c.networksTime = 0;
		c.elecBounds = new Rectangle2D.Double();
		c.boundsEmpty = true;
		c.boundsDirty = false;
		c.referencePointNode = null;
		c.rTree = Geometric.RTNode.makeTopLevel();
		return c;
	}

	/**
	 * Low-level access routine to fill-in the cell name.
	 * Unless you know what you are doing, do not use this method.
	 * @param name the name of this cell.
	 * Cell names may not contain unprintable characters, spaces, tabs, a colon (:), semicolon (;) or curly braces ({}).
	 * @return true on error.
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
				if (n.getName().equalsIgnoreCase(c.getProtoName()) && n.getView() == c.getView() &&
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
				if (n.getName().equalsIgnoreCase(c.getProtoName()) && n.getView() == c.getView() &&
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
	 * @return true on error.
	 */
	public boolean lowLevelLink()
	{
		// determine the cell group
		if (cellGroup == null)
		{
			// look for similar-named cell and use its group
			for (Iterator it = lib.getCells(); it.hasNext();)
			{
				Cell c = (Cell) it.next();
				if (c.getCellGroup() == null) continue;
				if (getProtoName().equalsIgnoreCase(c.getProtoName()))
				{
					cellGroup = c.getCellGroup();
					break;
				}
			}
			
			// still none: make a new one
			if (cellGroup == null) cellGroup = new CellGroup();
		}

		// add to cell group
		cellGroup.add(this);

		// add ourselves to the library
		Library lib = getLibrary();
		lib.addCell(this);

		// success
		return false;
	}

	/**
	 * Factory method to create a new Cell.
	 * @param lib the Library in which to place this cell.
	 * @param name the name of this cell.
	 * Cell names may not contain unprintable characters, spaces, tabs, a colon (:), semicolon (;) or curly braces ({}).
	 * However, the name can be fully qualified with version and view information.
	 * For example, "foo;2{sch}".
	 * @return the newly created cell (null on error).
	 */
	public static Cell newInstance(Library lib, String name)
	{
		Cell theCell = lowLevelAllocate(lib);
		if (theCell.lowLevelPopulate(name)) return null;
		if (theCell.lowLevelLink()) return null;
		return theCell;
	}

	/**
	 * Routine to remove this node from all lists.
	 */
	public void remove()
	{
		// remove ourselves from the cellGroup.
		cellGroup.remove(this);
		versionGroup.remove(this);
		lib.removeCell(this); // remove ourselves from the library
//		removeAll(nodes); // kill nodes

		// arcs should have been killed by ditching the nodes
		if (arcs.size() != 0)
			System.out.println("Arcs should have been removed when the nodes were killed");

		super.remove();
	}

	/**
	 * Routine to add a new ArcInst to the cell.
	 * @param ai the ArcInst to be included in the cell.
	 */
	public void addArc(ArcInst ai)
	{
		if (arcs.contains(ai))
		{
			System.out.println("Cell " + this +" already contains arc " + ai);
			return;
		}
		arcs.add(ai);

		// must recompute the bounds of the cell
		boundsDirty = true;
		setNetworksDirty();
	}

	/**
	 * Routine to remove an ArcInst from the cell.
	 * @param ai the ArcInst to be removed from the cell.
	 */
	public void removeArc(ArcInst ai)
	{
		if (!arcs.contains(ai))
		{
			System.out.println("Cell " + this +" doesn't contain arc " + ai);
			return;
		}
		arcs.remove(ai);

		// must recompute the bounds of the cell
		boundsDirty = true;
		setNetworksDirty();
	}

	/**
	 * Routine adjust this cell when the reference point moves.
	 * This requires renumbering all coordinate values in the Cell.
	 * @param dx the X distance that the reference point has moved.
	 * @param dy the Y distance that the reference point has moved.
	 */
	public void setReferencePoint(double dx, double dy)
	{
		// if there is no change, stop now
		if (dx == 0 && dy == 0) return;

		// move reference point by (dx,dy)
		referencePointNode.modifyInstance(-dx, -dy, 0, 0, 0);

		// must adjust all nodes by (dx,dy)
		for(Iterator it = getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni == referencePointNode) continue;

			// move NodeInst "ni" by (dx,dy)
			ni.modifyInstance(-dx, -dy, 0, 0, 0);
		}
	}

	/**
	 * Routine to add a new NodeInst to the cell.
	 * @param ni the NodeInst to be included in the cell.
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
		setNetworksDirty();

		// make additional checks to keep circuit up-to-date
		NodeProto np = ni.getProto();
		if (np instanceof PrimitiveNode && np == Generic.tech.cellCenter_node)
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

	/**
	 * Routine to remove an NodeInst from the cell.
	 * @param ni the NodeInst to be removed from the cell.
	 */
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
		setNetworksDirty();

		if (ni == referencePointNode)
			referencePointNode = null;
		essenBounds.remove(ni);
	}

	/**
	 * Routine to indicate that the bounds of this Cell are incorrect because
	 * a node or arc has been created, deleted, or modified.
	 */
	public void setDirty()
	{
		boundsDirty = true;
	}

	/**
	 * Routine to return the bounds of this Cell.
	 * @return a Rectangle2D.Double with the bounds of this cell's contents
	 */
	public Rectangle2D.Double getBounds()
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
				if (ni.getProto() == Generic.tech.cellCenter_node) continue;
				Rectangle2D bounds = ni.getBounds();
				double lowx = bounds.getMinX();
				double highx = bounds.getMaxX();
				double lowy = bounds.getMinY();
				double highy = bounds.getMaxY();
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
			for(Iterator it = arcs.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst) it.next();
				Rectangle2D bounds = ai.getBounds();
				double lowx = bounds.getMinX();
				double highx = bounds.getMaxX();
				double lowy = bounds.getMinY();
				double highy = bounds.getMaxY();
				if (lowx < cellLowX) cellLowX = lowx;
				if (highx > cellHighX) cellHighX = highx;
				if (lowy < cellLowY) cellLowY = lowy;
				if (highy > cellHighY) cellHighY = highy;
			}
			elecBounds.x = cellLowX;
			elecBounds.width = cellHighX - cellLowX;
			elecBounds.y = cellLowY;
			elecBounds.height = cellHighY - cellLowY;
			boundsDirty = false;
		}

		return elecBounds;
	}

	/**
	 * Routine to get the width of this Cell.
	 * @return the width of this Cell.
	 */
	public double getDefWidth() { return getBounds().width; }

	/**
	 * Routine to the height of this Cell.
	 * @return the height of this Cell.
	 */
	public double getDefHeight() { return getBounds().height; }

	/**
	 * Routine to size offset of this Cell.
	 * @return the size offset of this Cell.  It is always zero for cells.
	 */
	public SizeOffset getSizeOffset() { return new SizeOffset(0, 0, 0, 0); }

	/**
	 * Routine to R-Tree of this Cell.
	 * The R-Tree organizes all of the Geometric objects spatially for quick search.
	 * @return R-Tree of this Cell.
	 */
	public Geometric.RTNode getRTree() { return rTree; }

	/**
	 * Routine to set the R-Tree of this Cell.
	 * @param rTree the head of the new R-Tree for this Cell.
	 */
	public void setRTree(Geometric.RTNode rTree) { this.rTree = rTree; }

	/**
	 * Routine to compute the "essential bounds" of this Cell.
	 * It looks for NodeInst objects in the cell that are of the type
	 * "generic:Essential-Bounds" and builds a rectangle from their locations.
	 * @return the bounding area of the essential bounds.
	 * Returns null if an essential bounds cannot be determined.
	 */
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

//	private HashMap copyNodes(Cell f)
//	{
//		HashMap oldToNew = new HashMap();
//		for (int i = 0; i < nodes.size(); i++)
//		{
//			NodeInst oldInst = (NodeInst) nodes.get(i);
//			NodeProto oldProto = oldInst.getProto();
//			if (oldProto instanceof PrimitiveNode
//				&& oldProto.getProtoName().equals("Cell-Center"))
//			{
//				// Cell-Center already handled by copyReferencePoint
//			} else
//			{
//				NodeInst newInst =
//					NodeInst.newInstance(oldProto,new Point2D.Double(oldInst.getCenterX(), oldInst.getCenterY()),
//						oldInst.getXSize(), oldInst.getYSize(), oldInst.getAngle(), f);
//				String nm = oldInst.getName();
//				if (nm != null)
//					newInst.setName(nm);
//				if (oldToNew.containsKey(oldInst))
//				{
//					System.out.println("oldInst already in oldToNew?!");
//					return null;
//				}
//				oldToNew.put(oldInst, newInst);
//			}
//		}
//		return oldToNew;
//	}

//	private PortInst getNewPortInst(PortInst oldPort, HashMap oldToNew)
//	{
//		NodeInst newInst = (NodeInst) oldToNew.get(oldPort.getNodeInst());
//		if (newInst == null)
//		{
//			System.out.println( "no new instance for old instance in oldToNew?!");
//			return null;
//		}
//		String portNm = oldPort.getPortProto().getProtoName();
//		if (portNm == null)
//		{
//			System.out.println("PortProto with no name?");
//			return null;
//		}
//		return newInst.findPortInst(portNm);
//	}

//	private void copyArcs(Cell f, HashMap oldToNew)
//	{
//		for (int i = 0; i < arcs.size(); i++)
//		{
//			ArcInst ai = (ArcInst) arcs.get(i);
//			Connection c0 = ai.getConnection(false);
//			Connection c1 = ai.getConnection(true);
//			Point2D p0 = c0.getLocation();
//			Point2D p1 = c1.getLocation();
//			ArcInst.newInstance(ai.getProto(), ai.getWidth(),
//				getNewPortInst(c0.getPortInst(), oldToNew), p0.getX(), p0.getY(),
//				getNewPortInst(c1.getPortInst(), oldToNew), p1.getX(), p1.getY());
//		}
//	}

//	private void copyExports(Cell f, HashMap oldToNew)
//	{
//		for (Iterator it = getPorts(); it.hasNext();)
//		{
//			Export e = (Export) it.next();
//			PortInst newPort = getNewPortInst(e.getOriginalPort(), oldToNew);
//			if (newPort == null)
//			{
//				System.out.println("can't find new PortInst to export");
//				return;
//			}
//			Export.newInstance(f, e.getOriginalNode(), newPort, e.getProtoName());
//		}
//	}

//	private void copyContents(Cell f)
//	{
//		// Note: Electric has already created f and called f.init()
//		HashMap oldToNew = copyNodes(f);
//		copyArcs(f, oldToNew);
//		copyExports(f, oldToNew);
//	}

	/*
	 * Routine to write a description of this Cell.
	 * Displays the description in the Messages Window.
	 */
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

	// ------------------------- public methods -----------------------------

	/**
	 * Routine to get the CellGroup that this Cell is part of.
	 * @return he CellGroup that this Cell is part of.
	 */
	public CellGroup getCellGroup() { return cellGroup; }

	/**
	 * Routine to put this Cell into the given CellGroup.
	 * @param cellGroup the CellGroup that this cell belongs to.
	 */
	public void setCellGroup(CellGroup cellGroup) { this.cellGroup = cellGroup; }

	/**
	 * Routine to get the library to which this Cell belongs.
	 * @return to get the library to which this Cell belongs.
	 */
	public Library getLibrary() { return lib; }

	/**
	 * Routine to get this Cell's View.
	 * Views include "layout", "schematics", "icon", "netlist", etc.
	 * @return to get this Cell's View.
	 */
	public View getView() { return view; }

	/**
	 * Routine to return the Technology of this Cell.
	 * It can be quite complex to determine which Technology a Cell belongs to.
	 * The system examines all of the nodes and arcs in it, and also considers
	 * the Cell's view.
	 * @return return the Technology of this Cell.
	 */
	public Technology getTechnology()
	{
		if (tech == null) tech = Technology.whatTechnology(this, null, 0, 0, null, 0, 0);
		return tech;
	}

	/**
	 * Routine to get the creation date of this Cell.
	 * @return the creation date of this Cell.
	 */
	public Date getCreationDate() { return creationDate; }

	/**
	 * Routine to set this Cell's creation date.
	 * This is a low-level routine and should not be called unless you know what you are doing.
	 * @param creationDate the date of this Cell's creation.
	 */
	public void lowLevelSetCreationDate(Date creationDate) { this.creationDate = creationDate; }

	/**
	 * Routine to return the revision date of this Cell.
	 * @return the revision date of this Cell.
	 */
	public Date getRevisionDate() { return revisionDate; }

	/**
	 * Routine to set this Cell's last revision date.
	 * This is a low-level routine and should not be called unless you know what you are doing.
	 * @param revisionDate the date of this Cell's last revision.
	 */
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

	/**
	 * Routine to find a named Export on this Cell.
	 * @param name the name of the export.
	 * @return the export.  Returns null if that name was not found.
	 */
	public Export findExport(String name)
	{
		return (Export) findPortProto(name);
	}

	/**
	 * Routine to return an Iterator over all NodeInst objects in this Cell.
	 * @return an Iterator over all NodeInst objects in this Cell.
	 */
	public Iterator getNodes()
	{
		return nodes.iterator();
	}

	/**
	 * Routine to return the number of NodeInst objects in this Cell.
	 * @return the number of NodeInst objects in this Cell.
	 */
	public int getNumNodes()
	{
		return nodes.size();
	}

	/**
	 * Routine to find a named NodeInst on this Cell.
	 * @param name the name of the NodeInst.
	 * @return the NodeInst.  Returns null if none with that name are found.
	 */
	public NodeInst findNode(String name)
	{
		for (int i = 0; i < nodes.size(); i++)
		{
			NodeInst ni = (NodeInst) nodes.get(i);
			String nodeNm = ni.getName();
			if (nodeNm != null && nodeNm.equals(name))
				return ni;
		}
		return null;
	}

	/**
	 * Routine to return an Iterator over all ArcInst objects in this Cell.
	 * @return an Iterator over all ArcInst objects in this Cell.
	 */
	public Iterator getArcs()
	{
		return arcs.iterator();
	}

	/**
	 * Routine to return the number of ArcInst objects in this Cell.
	 * @return the number of ArcInst objects in this Cell.
	 */
	public int getNumArcs()
	{
		return arcs.size();
	}

	/**
	 * Routine to return the version number of this Cell.
	 * @return the version number of this Cell.
	 */
	public int getVersion() { return version; }

	/**
	 * Routine to return an Iterator over the different versions of this Cell.
	 * @return an Iterator over the different versions of this Cell.
	 */
	public Iterator getVersions()
	{
		return versionGroup.iterator();
	}

	/**
	 * Routine to return the most recent version of this Cell.
	 * @return he most recent version of this Cell.
	 */
	public Cell getNewestVersion()
	{
		return (Cell) getVersions().next();
	}

	/**
	 * Routine to describe this cell.
	 * The description has the form: cell;version{view}
	 * If the cell is not from the current library, prepend the library name.
	 * @return a String that describes this cell.
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
	 * Routine to describe this cell.
	 * The description has the form: cell;version{view}
	 * Unlike "describe()", this routine never prepends the library name.
	 * @return a String that describes this cell.
	 */
	public String noLibDescribe()
	{
		String name = protoName;
		if (getNewestVersion() != this)
			name += ";" + version;
		if (view != null)
			name += "{" +  view.getAbbreviation() + "}";
		return name;
	}

	/**
	 * Finds the Schematic Cell associated with this Icon Cell.
	 * If this Cell is an Icon View then find the schematic Cell in its
	 * CellGroup.
	 * @return the Schematic Cell.  Returns null if there is no equivalent.
	 * If there are multiple versions of the Schematic View then
	 * return the latest version.
	 */
	public Cell getEquivalent()
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

	/** Sanity check method used by Geometric.checkobj. */
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
	
	/**
	 * Returns a printable version of this Cell.
	 * @return a printable version of this Cell.
	 */
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

	/** Recompute the network structure for all Cells.
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
	public static void rebuildAllNetworks(ArrayList connectedPorts)
	{
		if (connectedPorts == null)
		{
			connectedPorts = new ArrayList();
		}
		HashMap connPorts = buildConnPortsTable(connectedPorts);
		currentTime++;
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			for(Iterator cit = lib.getCells(); cit.hasNext(); )
			{
				Cell cell = (Cell)cit.next();
				if (cell.getView() != View.LAYOUT) continue;
				cell.redoNetworks(connPorts);;
			}
		}
	}

	private static HashMap buildConnPortsTable(ArrayList connPortsLists)
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

	/**
	 * Update map of equivalent ports userEquivPort.
	 * @param userEquivMap HashMap (PortProto -> JNetwork) of user-specified equivalent ports
	 * @param currentTime.time stamp of current network reevaluation
	 * This routine will always set equivPortsCheckTime to currentTime.
     * equivPortsUpdateTime will either change to currentTime if map will change,
	 * or will be kept untouched if not.
     * For the Cell this routine will refresh networks of contents cell too.
	 */
	public void updateEquivPorts(HashMap userEquivPorts, int currentTime)
	{
		// If Cell is an Icon View then redo the Schematic
		// View. Otherwise redo the Cell.
		Cell equivCell = getEquivalent();
		if (equivCell == null) equivCell = this;
		equivCell.redoNetworks(userEquivPorts);

		super.updateEquivPorts(userEquivPorts, currentTime);
	}

	/**
	 * Redo subcells of this cell. Ifmap of equivalent ports of some subcell has
     * been updated since last network renumbering, then retunr true.
	 */
	private boolean redoDescendents(HashMap userEquivPorts)
	{
		boolean redoThis = false;
		for (int i = 0; i < nodes.size(); i++)
		{
			NodeInst ni = (NodeInst) nodes.get(i);
			if (ni.isIconOfParent()) continue;

			NodeProto np = ni.getProto();
			if (np.getEquivPortsCheckTime() != currentTime)
				np.updateEquivPorts(userEquivPorts, currentTime);
			if (networksTime < np.getEquivPortsUpdateTime())
				redoThis = true;
		}
		return redoThis;
	}

	private void placeEachPortInstOnItsOwnNet()
	{
		for (int i = 0; i < nodes.size(); i++)
		{
			NodeInst ni = (NodeInst) nodes.get(i);
			if (ni.isIconOfParent()) continue;

			NodeProto np = ni.getProto();
			np.numeratePorts();
			int[] eq = np.getEquivPorts();
			JNetwork[] nets = new JNetwork[eq.length];
			for (Iterator it = ni.getPortInsts(); it.hasNext();)
			{
				PortInst pi = (PortInst) it.next();
				int k = eq[pi.getPortProto().getTempInt()];
				if (nets[k] == null) nets[k] = new JNetwork(this);
				nets[k].addPortInst(pi);
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

	/*
	private PortProto getEquivPortProto(PortProto pp) {
		NodeProto np = pp.getParent();
		if (np instanceof Cell) {
			String nm = pp.getProtoName();
			return ((Cell)np).getEquivalent().findPortProto(nm);
		} else {
			return pp;
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
					getEquivPortProto(piNew.getPortProto()).getNetwork();

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
					equivPorts.get(getEquivPortProto(piNew.getPortProto()));
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
	*/

	protected void connectEquivPorts(int[] newEquivPorts)
	{
		HashMap netToPort = new HashMap(); // subNet -> Integer
		int i = 0;
		for (Iterator it = getPorts(); it.hasNext(); i++)
		{
			PortProto pp = (PortProto) it.next();
			JNetwork subNet = pp.getEquivalent().getNetwork();
			Integer iOld = (Integer) netToPort.get(subNet);
			if (iOld != null)
			{
				connectMap(newEquivPorts, iOld.intValue(), i);
			} else
			{
				netToPort.put(subNet, new Integer(i));
			}
		}
	}

	private HashSet getNetsFromPortInsts()
	{
		HashSet nets = new HashSet();
		for (Iterator nit = getNodes(); nit.hasNext();)
		{
			NodeInst ni = (NodeInst) nit.next();
			if (ni.isIconOfParent()) continue;

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
		if (!redoDescendents(equivPorts)) return;

		placeEachPortInstOnItsOwnNet();
		mergeNetsConnectedByArcs();
		//mergeNetsConnectedByNodeProtoSubnets();
		//mergeNetsConnectedByUserEquivPorts(equivPorts);
		addExportNamesToNets();
		mergeSameNameNets();
		buildNetworkList();
		networksTime = currentTime;
	}

	public final void setNetworksDirty()
	{
		networksTime = 0;
	}

}
