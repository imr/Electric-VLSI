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

import com.sun.electric.Main;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TextWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

import javax.swing.JOptionPane;

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
 * The library shown here has two cells (?gate? and ?twogate?), each of which has many
 * views (layout, schematics, icon, vhdl) and versions:
 * <P>
 * <CENTER><IMG SRC="doc-files/Cell-1.gif"></CENTER>
 */
public class Cell extends ElectricObject implements NodeProto, Comparable
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
		private Cell mainSchematic;
		private String groupName = null;

		// ------------------------- public methods -----------------------------

		/**
		 * Constructs a CellGroup.
		 */
		public CellGroup()
		{
			cells = new ArrayList();
		}

		/**
		 * Method to add a Cell to this CellGroup.
		 * @param cell the cell to add to this CellGroup.
		 */
		void add(Cell cell)
		{
			synchronized(cells)
			{
                if (!cells.contains(cell))
				    cells.add(cell);
			}
			groupName = null;
			cell.cellGroup = this;
		}

		/**
		 * Method to remove a Cell from this CellGroup.
		 * @param f the cell to remove from this CellGroup.
		 */
		void remove(Cell f)
		{
			synchronized (cells)
			{
				cells.remove(f);
				if (f == mainSchematic) mainSchematic = null;
			}
			groupName = null;
		}

		/**
		 * Method to return an Iterator over all the Cells that are in this CellGroup.
		 * @return an Iterator over all the Cells that are in this CellGroup.
		 */
		public Iterator getCells() { return cells.iterator(); }

		/**
		 * Method to return the number of Cells that are in this CellGroup.
		 * @return the number of Cells that are in this CellGroup.
		 */
		public int getNumCells() { return cells.size(); }

		/**
		 * Method to return a List of all cells in this Group, sorted by View.
		 * @return a List of all cells in this Group, sorted by View.
		 */
		public List getCellsSortedByView()
		{
			List sortedList = new ArrayList();
			synchronized(cells)
			{
				for(Iterator it = cells.iterator(); it.hasNext(); )
					sortedList.add(it.next());
			}
			Collections.sort(sortedList, new TextUtils.CellsByView());
			return sortedList;
		}

		/**
		 * Method to return main schematics Cell in ths CellGroup.
		 * The main schematic is the one that is shown when descending into an icon.
		 * Other schematic views may exist in the group, but they are "alternates".
		 * @return main schematics Cell  in this CellGroup.
		 */
		public Cell getMainSchematics()
		{
			if (mainSchematic != null) return mainSchematic;

			// not set: see if it is obvious
			for (Iterator it = getCells(); it.hasNext();)
			{
				Cell c = (Cell) it.next();
				if (c.isSchematic())
				{
                    // get latest version
					mainSchematic = c.getNewestVersion();
                    return mainSchematic;
                }
			}
			return null;
		}
		/**
		 * Method to set the main schematics Cell in ths CellGroup.
		 * The main schematic is the one that is shown when descending into an icon.
		 * Other schematic views may exist in the group, but they are "alternates".
		 * @param cell the new main schematics Cell in this CellGroup.
		 */
		public void setMainSchematics(Cell cell)
		{
			if (getMainSchematics() == cell) return;
			if (!(cell.isSchematic() && cell.getNewestVersion() == cell))
			{
				System.out.println("Cell " + cell + ": cannot be main schematics");
				return;
			}
			mainSchematic = cell;
			Undo.modifyCellGroup(cell, this); // Notify network tool
		}

        /**
         * Method to tell whether this CellGroup contains a specified Cell.
         * @param cell the Cell in question.
         * @return true if the Cell is in this CellGroup.
         */
        public boolean containsCell(Cell cell) { return cells.contains(cell); }
        
		/**
		 * Returns a printable version of this CellGroup.
		 * @return a printable version of this CellGroup.
		 */
		public String toString() { return "CellGroup " + getName(); }

        /**
         * Returns a string representing the name of the cell group
		 */
		public String getName()
		{
			// if the name is cached, return that
			if (groupName != null) return groupName;

			// first see if this is the only cell in the group (allowing for old versions)
			Cell onlyCell = null;
			for(Iterator it = getCells(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				if (onlyCell == null) onlyCell = cell.getNewestVersion(); else
				{
					if (cell.getNewestVersion() != onlyCell)
					{
						onlyCell = null;
						break;
					}
				}
			}
			if (onlyCell != null) return groupName = onlyCell.describe();

			// name the group according to all of the different base names
			Set groupNames = new TreeSet();
			int widestName = 0;
			for(Iterator it = getCells(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				String cellName = cell.getName();
				if (cellName.length() > widestName) widestName = cellName.length();
				groupNames.add(cellName);
			}

			// if there is only 1 base name, use it
			if (groupNames.size() == 1) return groupName = (String)groupNames.iterator().next();

			// look for common root to the names
			for(int i=widestName; i>widestName/2; i--)
			{
				String lastName = null;
				boolean allSame = true;
				for(Iterator it = groupNames.iterator(); it.hasNext(); )
				{
					String oneName = (String)it.next();
					if (lastName != null)
					{
						if (oneName.length() < i || lastName.length() < i || !lastName.substring(0, i).equals(oneName.substring(0, i))) { allSame = false;   break; }
					}
					lastName = oneName;
				}
				if (allSame)
					return groupName = lastName.substring(0, i) + "*";
			}

			// just list all of the different base names
			for(Iterator it = groupNames.iterator(); it.hasNext(); )
			{
				String oneName = (String)it.next();
				if (groupName == null) groupName = oneName; else
					groupName += "," + oneName;
			}
			return groupName;
		}
	}

	private static class VersionGroup
	{
		// private data
		private List versions;

		/**
		 * Constructs a VersionGroup that contains the history of a Cell.
		 */
		public VersionGroup()
		{
			versions = new ArrayList();
		}

		/**
		 * Method to add a Cell to this VersionGroup.
		 * @param cell the cell to add to this VersionGroup.
		 * @return the cell that used to be the newest (null if adding the Cell did not displace another newer one).
		 */
		public Cell add(Cell cell)
		{
			// remember the cell that used to be the newest in the group
			Cell formerNewestCell = null;
			if (versions.size() > 0) formerNewestCell = (Cell)versions.iterator().next();

			// add this cell to the group
			versions.add(cell);
			cell.setVersionGroup(this);

			// resort the group and find the newest
			Collections.sort(versions, new TextUtils.CellsByVersion());
			Cell newestCell = (Cell)versions.iterator().next();

			// if the former newest is still newest, report no displacement
			if (newestCell == formerNewestCell) formerNewestCell = null;

			return formerNewestCell;
		}

		/**
		 * Method to remove a Cell from this VersionGroup.
		 * @param cell the cell to remove from this VersionGroup.
		 */
		public void remove(Cell cell) { versions.remove(cell); }

		/**
		 * Method to return the number of Cells in this VersionGroup.
		 * @return the number of Cells in this VersionGroup.
		 */
		public int size() { return versions.size(); }

		/**
		 * Method to return an Iterator over all the Cells that are in this VersionGroup.
		 * @return an Iterator over all the Cells that are in this VersionGroup.
		 */
		public Iterator iterator() { return versions.iterator(); }
	}

	private class MaxSuffix { int v = 0; }

	// -------------------------- private data ---------------------------------

	/** Variable key for characteristic spacing for a cell. */		public static final Variable.Key CHARACTERISTIC_SPACING = ElectricObject.newKey("FACET_characteristic_spacing");
	/** Variable key for text cell contents. */						public static final Variable.Key CELL_TEXT_KEY = ElectricObject.newKey("FACET_message");

	/** set if instances should be expanded */						private static final int WANTNEXPAND =          02;
	/** set if everything in cell is locked */						private static final int NPLOCKED =       04000000;
	/** set if instances in cell are locked */						private static final int NPILOCKED =     010000000;
	/** set if cell is part of a "cell library" */					private static final int INCELLLIBRARY = 020000000;
	/** set if cell is from a technology-library */					private static final int TECEDITCELL =   040000000;

	/** Length of base name for autonaming. */						private static final int ABBREVLEN = 8;
	/** zero rectangle */											private static final Rectangle2D CENTERRECT = new Rectangle2D.Double(0, 0, 0, 0);

	/** counter for enumerating cells */							private static int cellNumber = 0;

	/** The object used to request flag bits. */					private static final FlagSet.Generator flagGenerator = new FlagSet.Generator("Cell");

	/** The name of the Cell. */									private String protoName;
	/** The CellGroup this Cell belongs to. */						private CellGroup cellGroup;
	/** The VersionGroup this Cell belongs to. */					private VersionGroup versionGroup;
	/** The library this Cell belongs to. */						private Library lib;
	/** This Cell's View. */										private View view;
	/** The date this Cell was created. */							private Date creationDate;
	/** The date this Cell was last modified. */					private Date revisionDate;
	/** The version of this Cell. */								private int version;
	/** Internal flag bits. */										private int userBits;
	/** The basename for autonaming of instances of this Cell */	private Name basename;
	/** A list of Exports on the Cell. */							private List exports;
	/** The Cell's essential-bounds. */								private List essenBounds = new ArrayList();
	/** A list of NodeInsts in this Cell. */						private List nodes;
	/** A map from NodeProto to NodeUsages in it */					private Map usagesIn;
	/** A list of NodeUsages of this Cell. */						/*package-only*/ List usagesOf;
	/** A map from Name to Integer maximal numeric suffix */        private Map maxSuffix;
	/** A list of ArcInsts in this Cell. */							private List arcs;
	/** A map from temporary Name keys to Geometric. */				private Map tempNames;
	/** The bounds of the Cell. */									private Rectangle2D cellBounds;
	/** Whether the bounds need to be recomputed. */				private boolean boundsDirty;
	/** Whether the bounds have anything in them. */				private boolean boundsEmpty;
	/** The geometric data structure. */							private Geometric.RTNode rTree;
	/** The Change object. */										private Undo.Change change;
// 	/** Lock count. lock=0 "no locked",
// 	 *  lock=-1 "locked for changes".
// 	 *  lock=n>0 "locked for examination n times"
// 	 */                                                             private int lock;
	/** 0-based index of this Cell. */								private int cellIndex;
	/** This Cell's Technology. */									private Technology tech;
	/** The temporary integer value. */								private int tempInt;
	/** The temporary flag bits. */									private int flagBits;


	// ------------------ protected and private methods -----------------------

	/**
	 * This constructor should not be called.
	 * Use the factory "newInstance" to create a Cell.
	 */
	private Cell()
	{
		this.cellIndex = cellNumber++;
		exports = new ArrayList();
		nodes = new ArrayList();
		usagesIn = new HashMap();
		usagesOf = new ArrayList();
		maxSuffix = new HashMap();
		arcs = new ArrayList();
		tempNames = new HashMap();
		cellGroup = null;
		tech = null;
		creationDate = new Date();
		revisionDate = new Date();
		userBits = 0;
		cellBounds = new Rectangle2D.Double();
		boundsEmpty = true;
		boundsDirty = false;
		rTree = Geometric.RTNode.makeTopLevel();
        setLinked(false);
	}

	/****************************** CREATE, DELETE ******************************/

	/**
	 * Factory method to create a new Cell.
	 * Also does auxiliary things to create the Cell, such as placing a cell-center if requested.
	 * @param lib the Library in which to place this cell.
	 * @param name the name of this cell.
	 * Cell names may not contain unprintable characters, spaces, tabs, a colon (:), semicolon (;) or curly braces ({}).
	 * However, the name can be fully qualified with version and view information.
	 * For example, "foo;2{sch}".
	 * @return the newly created cell (null on error).
	 */
	public static Cell makeInstance(Library lib, String name)
	{
		Cell cell = newInstance(lib, name);

		// add cell-center if requested
		if (User.isPlaceCellCenter())
		{
			NodeProto cellCenterProto = Generic.tech.cellCenterNode;
			NodeInst cellCenter = NodeInst.newInstance(cellCenterProto, new Point2D.Double(0, 0),
				cellCenterProto.getDefWidth(), cellCenterProto.getDefHeight(), cell);
            if (cellCenter != null)
            {
                cellCenter.setVisInside();
			    cellCenter.setHardSelect(); 
            }
		}
		return cell;
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
		Job.checkChanging();
		Cell cell = lowLevelAllocate(lib);
		if (cell.lowLevelPopulate(name)) return null;
		if (cell.lowLevelLink()) return null;

		// handle change control, constraint, and broadcast
		Undo.newObject(cell);
		return cell;
	}

	/**
	 * Method to remove this node from all lists.
	 */
	public void kill()
	{
		checkChanging();

		// remove ourselves from the cellGroup.
		lowLevelUnlink();

		// handle change control, constraint, and broadcast
		Undo.killObject(this);
	}

	/**
	 * Method to copy a Cell to any Library.
	 * @param fromCell the Cell to copy.
	 * @param toLib the Library to copy it to.
	 * If the destination library is the same as the original Cell's library, a new version is made.
	 * @param toName the name of the Cell in the destination Library.
	 * @param useExisting true to use existing Cell instances if they exist in the destination Library.
	 * @return the new Cell in the destination Library.
	 */
	public static Cell copyNodeProto(Cell fromCell, Library toLib, String toName, boolean useExisting)
	{
		// check for validity
		if (fromCell == null) return null;
		if (toLib == null) return null;

		// make sure name of new cell is valid
		for(int i=0; i<toName.length(); i++)
		{
			char ch = toName.charAt(i);
			if (ch <= ' ' || ch == ':' || ch >= 0177)
            {
                System.out.println("invalid name of new cell");
                return null;
            }
		}

		// determine whether this copy is to a different library
		Library destLib = toLib;
		if (toLib == fromCell.getLibrary()) destLib = null;

		// mark the proper prototype to use for each node
		HashMap nodePrototypes = new HashMap();
		for(Iterator it = fromCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			nodePrototypes.put(ni, ni.getProto());
		}

		// if doing a cross-library copy and can use existing ones from new library, do it
		if (destLib != null)
		{
			// scan all subcells to see if they are found in the new library
			for(Iterator it = fromCell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (ni.getProto() instanceof PrimitiveNode) continue;
				Cell niProto = (Cell)ni.getProto();

				// keep cross-library references
//				if (niProto.getLibrary() != fromCell.getLibrary()) continue;

				boolean maySubstitute = useExisting;
				if (!maySubstitute)
				{
					// force substitution for documentation icons
					if (niProto.isIcon())
					{
						if (niProto.isIconOf(fromCell)) maySubstitute = true;
					}
				}
				if (!maySubstitute) continue;

				// search for cell with same name and view in new library
				Cell lnt = null;
				for(Iterator cIt = toLib.getCells(); cIt.hasNext(); )
				{
					lnt = (Cell)cIt.next();
					if (lnt.getName().equalsIgnoreCase(niProto.getName()) &&
						lnt.getView() == niProto.getView()) break;
					lnt = null;
				}
				if (lnt == null) continue;

				// make sure all used ports can be found on the uncopied cell
				boolean validPorts = true;
				for(Iterator pIt = ni.getPortInsts(); pIt.hasNext(); )
				{
					PortInst pi = (PortInst)pIt.next();
					PortProto pp = pi.getPortProto();
					PortProto ppt = lnt.findPortProto(pp.getName());
					if (ppt != null)
					{
						// the connections must match, too
//						if (pp->connects != ppt->connects) ppt = null;
					}
					if (ppt == null)
					{
						System.out.println("Cannot use subcell " + lnt.noLibDescribe() + " in library " + destLib.getName() +
							": exports don't match");
						validPorts = false;
						break;
					}
				}
				if (!validPorts) continue;

				// match found: use the prototype from the destination library
				nodePrototypes.put(ni, lnt);
			}
		}

		// create the nodeproto
		String cellName = toName;
		if (toName.indexOf('{') < 0 && fromCell.getView() != View.UNKNOWN)
		{
			cellName = toName + "{" + fromCell.getView().getAbbreviation() + "}";
		}
		Cell newCell = Cell.newInstance(toLib, cellName);
		if (newCell == null) return(null);
		newCell.lowLevelSetUserbits(fromCell.lowLevelGetUserbits());

		// copy nodes
		HashMap newNodes = new HashMap();
		for(Iterator it = fromCell.getNodes(); it.hasNext(); )
		{
			// create the new nodeinst
			NodeInst ni = (NodeInst)it.next();
			NodeProto lnt = (NodeProto)nodePrototypes.get(ni);
			double scaleX = ni.getXSize();   if (ni.isXMirrored()) scaleX = -scaleX;
			double scaleY = ni.getYSize();   if (ni.isYMirrored()) scaleY = -scaleY;
			NodeInst toNi = NodeInst.newInstance(lnt, new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY()),
				scaleX, scaleY, newCell, ni.getAngle(), ni.getName(), 0);
			if (toNi == null) return null;

			// save the new nodeinst address in the old nodeinst
			newNodes.put(ni, toNi);

			// copy miscellaneous information
			toNi.setProtoTextDescriptor(ni.getProtoTextDescriptor());
			toNi.setNameTextDescriptor(ni.getNameTextDescriptor());
			toNi.lowLevelSetUserbits(ni.lowLevelGetUserbits());

		}

		// now copy the variables on the nodes
		for(Iterator it = fromCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeInst toNi = (NodeInst)newNodes.get(ni);
			toNi.copyVarsFrom(ni);

            // if this is an icon, and this nodeinst is the box with the name of the cell on it,
            // then change the name from the old to the new
            if (newCell.isIcon()) {
                Variable var = toNi.getVar(Schematics.SCHEM_FUNCTION);
                if (var != null) {
                    String name = (String)var.getObject();
                    if (name.equals(fromCell.getName())) {
                        toNi.updateVar(var.getKey(), newCell.getName());
                    }
                }
            }
		}

		// copy arcs
		for(Iterator it = fromCell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();

			// find the nodeinst and portinst connections for this arcinst
			PortInst [] opi = new PortInst[2];
			for(int i=0; i<2; i++)
			{
				opi[i] = null;
				Connection con = ai.getConnection(i);
				NodeInst ono = (NodeInst)newNodes.get(con.getPortInst().getNodeInst());
				PortProto pp = con.getPortInst().getPortProto();
				if (ono.getProto() instanceof PrimitiveNode)
				{
					// primitives associate ports directly
					opi[i] = ono.findPortInstFromProto(pp);
				} else
				{
					// cells associate ports by name
					PortProto ppt = ono.getProto().findPortProto(pp.getName());
					if (ppt != null) opi[i] = ono.findPortInstFromProto(ppt);
				}
				if (opi[i] == null)
					System.out.println("Error: no port for " + ai.getProto().describe() +
						" arc on " + ono.getProto().describe() + " node");
			}
			if (opi[0] == null || opi[1] == null) return null;

			// create the arcinst
			ArcInst toAi = ArcInst.newInstance(ai.getProto(), ai.getWidth(), opi[0], opi[1], ai.getHead().getLocation(),
			        ai.getTail().getLocation(), ai.getName(), ai.getAngle());
			if (toAi == null) return null;

			// copy arcinst information
			toAi.copyPropertiesFrom(ai);
		}

		// copy the Exports
		for(Iterator it = fromCell.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();

			// match sub-portproto in old nodeinst to sub-portproto in new one
			NodeInst ni = (NodeInst)newNodes.get(pp.getOriginalPort().getNodeInst());
			PortInst pi = ni.findPortInst(pp.getOriginalPort().getPortProto().getName());
			if (pi == null)
			{
				System.out.println("Error: no port on " + pp.getOriginalPort().getNodeInst().getProto().describe() + " cell");
				return null;
			}

			// create the nodeinst portinst
			Export ppt = Export.newInstance(newCell, pi, pp.getName());
			if (ppt == null) return null;

			// copy portproto variables
			ppt.copyVarsFrom(pp);

			// copy miscellaneous information
			ppt.lowLevelSetUserbits(pp.lowLevelGetUserbits());
			ppt.setTextDescriptor(pp.getTextDescriptor());
		}

		// copy cell variables
		newCell.copyVarsFrom(fromCell);

		// reset (copy) date information
		newCell.lowLevelSetCreationDate(fromCell.getCreationDate());
		newCell.lowLevelSetRevisionDate(fromCell.getRevisionDate());

		return newCell;
	}

	/**
	 * Method to rename this Cell.
	 * @param newName the new name of this cell.
	 */
	public void rename(String newName)
	{
		checkChanging();

		CellName n = CellName.parseName(newName + ";" + version + "{" + view.getAbbreviation() + "}");
		if (n == null) return;

        // check for same name already in library
        for (Iterator it = getLibrary().getCells(); it.hasNext(); )
        {
            Cell c = (Cell)it.next();
            if (newName.equalsIgnoreCase(c.getName()) && (getView() == c.getView()))
            {
                System.out.println("Already a Cell named " + noLibDescribe() + " in Library " + getLibrary().getName() +
                	"...making this a new version");
                break;
            }
        }

		// do the rename
		Name oldName = basename;
		int oldVersion = version;
		lowLevelRename(n.getName(), version);
//		lowLevelRename(newName, version);

		// handle change control, constraint, and broadcast
		Undo.renameObject(this, oldName, oldVersion);
	}

	/****************************** LOW-LEVEL IMPLEMENTATION ******************************/

	/**
	 * Low-level access method to rename a Cell.
	 * Unless you know what you are doing, do not use this method...use "rename()" instead.
	 * @param newName the new name of this cell.
	 * @param newVersion the new version number of this cell (if reassignment is necessary).
	 */
	public void lowLevelRename(String newName, int newVersion)
	{
		// if the current cell has other versions, separate it from them
		if (versionGroup.size() > 1)
		{
			versionGroup.remove(this);
			versionGroup = new VersionGroup();
			versionGroup.add(this);
		}

		// if the new name exists, make this a new version
		for(Iterator it = this.getLibrary().getCells(); it.hasNext(); )
		{
			Cell oCell = (Cell)it.next();
			if (oCell.getView() == this.getView() && oCell.getName().equalsIgnoreCase(newName))
			{
				int greatestVersion = 0;
				for(Iterator vIt = oCell.versionGroup.iterator(); vIt.hasNext(); )
				{
					Cell vCell = (Cell)vIt.next();
					if (vCell.getVersion() == newVersion) newVersion = -1;
					if (vCell.getVersion() > greatestVersion)
						greatestVersion = vCell.getVersion();
				}
				if (newVersion < 0) newVersion = greatestVersion + 1;
				this.version = newVersion;
				versionGroup.remove(this);
				oCell.versionGroup.add(this);
				this.versionGroup = oCell.versionGroup;
				break;
			}
		}
		setProtoName(newName);
	}

	/**
	 * Low-level access method to create a cell in library "lib".
	 * Unless you know what you are doing, do not use this method.
	 * @param lib library in which to place this cell.
	 * @return the newly created cell.
	 */
	public static Cell lowLevelAllocate(Library lib)
	{
		Job.checkChanging();
		Cell c = new Cell();
		c.lib = lib;
		return c;
	}

	/**
	 * Low-level access method to fill-in the cell name.
	 * Unless you know what you are doing, do not use this method.
	 * @param name the name of this cell.
	 * Cell names may not contain unprintable characters, spaces, tabs, a colon (:), semicolon (;) or curly braces ({}).
	 * @return true on error.
	 */
	public boolean lowLevelPopulate(String name)
	{
		checkChanging();

		// see if this cell already exists
		Library lib = getLibrary();
		Cell existingCell = lib.findNodeProto(name);
//		if (existingCell != null)
//		{
//			System.out.println("Cannot create cell " + name + " in library " + lib.getName() + " ...already exists");
//			return true;
//		}

		CellName n = CellName.parseName(name);
		if (n == null) return true;
        //if (existingCell != null) n.setVersion(n.getVersion()+1);
		int version = n.getVersion();

		// make sure this version isn't in use
		if ((version > 0))
		{
			for (Iterator it = lib.getCells(); it.hasNext();)
			{
				Cell c = (Cell) it.next();
				if (n.getName().equalsIgnoreCase(c.getName()) && n.getView() == c.getView() &&
					version == c.getVersion())
				{
					System.out.println("Already have cell " + c.getName() + " with version " + version + ", generating a new version");
					version = 1;
					for (Iterator vIt = lib.getCells(); vIt.hasNext();)
					{
						c = (Cell) vIt.next();
						if (n.getName().equalsIgnoreCase(c.getName()) && n.getView() == c.getView() &&
							c.getVersion() >= version)
								version = c.getVersion() + 1;
					}
				}
			}
		} else
		{
			// find a new version
			version = 1;
			for (Iterator it = lib.getCells(); it.hasNext();)
			{
				Cell c = (Cell) it.next();
				if (n.getName().equalsIgnoreCase(c.getName()) && n.getView() == c.getView() &&
					c.getVersion() >= version)
						version = c.getVersion() + 1;
			}
		}
		
		// fill-in the fields
		setProtoName(n.getName());
		this.view = n.getView();
		this.version = version;

		return false;
	}

	/**
	 * Method to change name of this Cell.
	 * @param name new name.
	 */
	private void setProtoName(String name)
	{
		this.protoName = name;

		// prepare basename for autonaming
		basename = Name.findName(protoName.substring(0,Math.min(ABBREVLEN,protoName.length()))+'@').getBasename();
		if (basename == null)
			basename = PrimitiveNode.Function.UNKNOWN.getBasename();
	}

	/**
	 * Low-level access method to link this Cell into its library.
	 * @return true on error.
	 */
	public boolean lowLevelLink()
	{
		checkChanging();
		if (isLinked())
		{
			System.out.println(this+" already linked");
			return true;
		}

		// see if this is a version of another
		versionGroup = null;
		for (Iterator it = lib.getCells(); it.hasNext();)
		{
			Cell c = (Cell) it.next();
			if (c.getView() != getView()) continue;
			if (getName().equalsIgnoreCase(c.getName()))
			{
				versionGroup = c.versionGroup;
				break;
			}
		}
		if (versionGroup == null)
			versionGroup = new VersionGroup();
		Cell displacedCell = versionGroup.add(this);
		if (displacedCell != null)
		{
			// remove this from the cellgroup since there is now a newer version
			//displacedCell.getCellGroup().remove(displacedCell);
		}

		if (getNewestVersion() != this) cellGroup = null; else
		{
			// determine the cell group
			if (cellGroup == null)
			{
				// look for similar-named cell and use its group
				for (Iterator it = lib.getCells(); it.hasNext();)
				{
					Cell c = (Cell) it.next();
					if (c.getCellGroup() == null) continue;
					if (getName().equalsIgnoreCase(c.getName()))
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
		}

		// add ourselves to the library
		Library lib = getLibrary();
		lib.addCell(this);

		// link NodeUsages
		for (Iterator it = getUsagesIn(); it.hasNext(); )
		{
			NodeUsage nu = (NodeUsage)it.next();
			NodeProto np = nu.getProto();
			if (np instanceof Cell)
				((Cell)np).usagesOf.add(nu);
		}

		// success
		setLinked(true);
		return false;
	}

	/**
	 * Low-level access method to unlink this Cell from its library.
	 */
	public void lowLevelUnlink()
	{
		checkChanging();
		if (!isLinked())
		{
			System.out.println(this+" already unlinked");
			return;
		}

		// see if this was the newest version
		Iterator vIt = getVersions();
		Cell newest = (Cell)vIt.next();
		Cell nextNewest = null;
		if (vIt.hasNext()) nextNewest = (Cell)vIt.next();

		versionGroup.remove(this);
		setVersionGroup(null);
		if (this == newest && nextNewest != null)
		{
			cellGroup.add(nextNewest);
		}

		if (cellGroup != null) cellGroup.remove(this);

		Library lib = getLibrary();
		lib.removeCell(this);

		// unlink NodeUsages
		for (Iterator it = getUsagesIn(); it.hasNext(); )
		{
			NodeUsage nu = (NodeUsage)it.next();
			NodeProto np = nu.getProto();
			if (np instanceof Cell)
				((Cell)np).usagesOf.remove(nu);
		}

		setLinked(false);
	}

	/**
	 * Low-level method to get the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the "user bits".
	 */
	public int lowLevelGetUserbits() { return userBits; }

	/**
	 * Low-level method to set the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @param userBits the new "user bits".
	 */
	public void lowLevelSetUserbits(int userBits) { checkChanging(); this.userBits = userBits; }

	/****************************** GRAPHICS ******************************/

	/**
	 * Method to get the width of this Cell.
	 * @return the width of this Cell.
	 */
	public double getDefWidth() { return getBounds().getWidth(); }

	/**
	 * Method to the height of this Cell.
	 * @return the height of this Cell.
	 */
	public double getDefHeight() { return getBounds().getHeight(); }

	/**
	 * Method to get the size offset of this Cell.
	 * @return the size offset of this Cell.  It is always zero for cells.
	 */
	public SizeOffset getProtoSizeOffset() { return SizeOffset.ZERO_OFFSET; }

	/**
	 * Method to get the characteristic spacing for this Cell.
	 * The characteristic spacing is used by the Array command to space these cells sensibly.
	 * @return a dimension that is the characteristic spacing for this cell.
	 * Returns null if there is no spacing defined.
	 */
	public Dimension2D getCharacteristicSpacing()
	{
		Variable var = getVar(CHARACTERISTIC_SPACING);
		if (var != null)
		{
			Object obj = var.getObject();
			if (obj instanceof Integer[])
			{
				Integer [] iSpac = (Integer [])obj;
				Dimension2D spacing = new Dimension2D.Double(iSpac[0].intValue(), iSpac[1].intValue());
				return spacing;
			} else if (obj instanceof Double[])
			{
				Double [] dSpac = (Double [])obj;
				Dimension2D spacing = new Dimension2D.Double(dSpac[0].doubleValue(), dSpac[1].doubleValue());
				return spacing;
			}
		}
		return null;
	}

	/**
	 * Method to set the characteristic spacing for this Cell.
	 * The characteristic spacing is used by the Array command to space these cells sensibly.
	 * @param x the characteristic width.
	 * @param y the characteristic height.
	 */
	public void setCharacteristicSpacing(double x, double y)
	{
		Double [] newVals = new Double[2];
		newVals[0] = new Double(x);
		newVals[1] = new Double(y);
		newVar(CHARACTERISTIC_SPACING, newVals);
	}

	/**
	 * Method to indicate that the bounds of this Cell are incorrect because
	 * a node or arc has been created, deleted, or modified.
	 */
	public void setDirty()
	{
		boundsDirty = true;
	}

	/**
	 * Method to return an interator over all Geometric objects in a given area of this Cell.
	 * @param bounds the specified area to search.
	 * @return an iterator over all of the Geometric objects in that area.
	 */
	public Iterator searchIterator(Rectangle2D bounds) { return new Geometric.Search(bounds, this); }

	private boolean boundLock = false;
	private Rectangle2D lastBounds = new Rectangle2D.Double();

	/**
	 * Method to request that the current bounds of this Cell be remembered.
	 * After this, you may call "getRememberedBounds()" to retrieve these bounds.
	 */
	public void rememberBounds()
	{
		if (boundsDirty)
		{
			getBounds();
		}
		boundLock = true;
	}

	/**
	 * Method to get the bounds of this Cell that were saved earlier by a call to "rememberBounds()".
	 * @return a Rectangle2D with the bounds at the time of the call to "rememberBounds()".
	 */
	public Rectangle2D getRememberedBounds()
	{
		Rectangle2D retBounds = lastBounds;
		if (boundLock) retBounds = cellBounds;
		boundLock = false;
		return retBounds;
	}

	/**
	 * Method to return the bounds of this Cell.
	 * @return a Rectangle2D with the bounds of this cell's contents
	 */
	public Rectangle2D getBounds()
	{
		if (boundsDirty)
		{
			if (boundLock)
			{
				boundLock = false;
				lastBounds.setRect(cellBounds);
			}

			// recompute bounds
			double cellLowX, cellHighX, cellLowY, cellHighY;
			boundsEmpty = true;
			cellLowX = cellHighX = cellLowY = cellHighY = 0;

			for(int i = 0; i < nodes.size(); i++ )
			{
				NodeInst ni = (NodeInst) nodes.get(i);
				NodeProto np = ni.getProto();

				// special case: do not include "cell center" primitives from Generic
				if (np == Generic.tech.cellCenterNode) continue;

				// special case for invisible pins: do not include if inheritable or interior-only
				if (np == Generic.tech.invisiblePinNode)
				{
					boolean found = false;
					for(Iterator it = ni.getVariables(); it.hasNext(); )
					{
						Variable var = (Variable)it.next();
						if (var.isDisplay())
						{
							TextDescriptor td = var.getTextDescriptor();
							if (td.isInterior() || td.isInherit()) { found = true;   break; }
						}
					}
					if (found) continue;
				}

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
			for(int i = 0; i < arcs.size(); i++ )
			{
				ArcInst ai = (ArcInst) arcs.get(i);
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
			cellBounds.setRect(DBMath.round(cellLowX), DBMath.round(cellLowY),
				DBMath.round(cellHighX - cellLowX), DBMath.round(cellHighY - cellLowY));
			boundsDirty = false;
		}
		return cellBounds;
	}

	/**
	 * Method to R-Tree of this Cell.
	 * The R-Tree organizes all of the Geometric objects spatially for quick search.
	 * @return R-Tree of this Cell.
	 */
	public Geometric.RTNode getRTree() { return rTree; }

	/**
	 * Method to set the R-Tree of this Cell.
	 * @param rTree the head of the new R-Tree for this Cell.
	 */
	public void setRTree(Geometric.RTNode rTree) { checkChanging(); this.rTree = rTree; }

	/**
	 * Method to compute the "essential bounds" of this Cell.
	 * It looks for NodeInst objects in the cell that are of the type
	 * "generic:Essential-Bounds" and builds a rectangle from their locations.
	 * @return the bounding area of the essential bounds.
	 * Returns null if an essential bounds cannot be determined.
	 */
	public Rectangle2D findEssentialBounds()
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
			minX = Math.min(minX, ni.getTrueCenterX());
			maxX = Math.max(maxX, ni.getTrueCenterX());
			minY = Math.min(minY, ni.getTrueCenterY());
			maxY = Math.max(maxY, ni.getTrueCenterY());
		}

		return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
	}

	/**
	 * Method adjust this cell when the reference point moves.
	 * This requires renumbering all coordinate values in the Cell.
	 * @param referencePointNode the Node that is the cell-center.
	 */
	public void adjustReferencePoint(NodeInst referencePointNode)
	{
		checkChanging();

		// if there is no change, stop now
		double cX = referencePointNode.getAnchorCenterX();
		double cY = referencePointNode.getAnchorCenterY();
		if (cX == 0 && cY == 0) return;

		// move reference point by (dx,dy)
		referencePointNode.modifyInstance(-cX, -cY, 0, 0, 0);

		// must adjust all nodes by (dx,dy)
		for(Iterator it = getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni == referencePointNode) continue;

			// move NodeInst "ni" by (dx,dy)
			ni.lowLevelModify(-cX, -cY, 0, 0, 0);
		}
		for(Iterator it = getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();

			// move NodeInst "ni" by (dx,dy)
			ai.lowLevelModify(0, -cX, -cY, -cX, -cY);
		}

		// adjust all instances of this cell
		for(Iterator it = getInstancesOf(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			Undo.redrawObject(ni);
			AffineTransform trans = NodeInst.pureRotate(ni.getAngle(), ni.isMirroredAboutXAxis(), ni.isMirroredAboutYAxis());
			Point2D in = new Point2D.Double(cX, cY);
			trans.transform(in, in);
			ni.modifyInstance(in.getX(), in.getY(), 0, 0, 0);
		}

		// adjust all windows showing this cell
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow)) continue;
			Cell cell = content.getCell();
			if (cell != this) continue;
			EditWindow wnd = (EditWindow)content;
			Point2D off = wnd.getOffset();
			off.setLocation(off.getX()-cX, off.getY()-cY);
			wnd.setOffset(off);
		}
	}

	/**
	 * Method to determine whether this Cell has a cell center in it.
	 * @return true if this Cell has a Cell-center node in it.
	 */
	public boolean alreadyCellCenter()
	{
		for(Iterator it = getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() == Generic.tech.cellCenterNode) return true;
		}
		return false;
	}

	/**
	 * Class for creating a description of a frame around a schematic cell.
	 */
	public static class FrameDescription
	{
		private static final double FRAMESCALE = 18.0;
		private static final double HASCHXSIZE = ( 8.5  * FRAMESCALE);
		private static final double HASCHYSIZE = ( 5.5  * FRAMESCALE);
		private static final double ASCHXSIZE  = (11.0  * FRAMESCALE);
		private static final double ASCHYSIZE  = ( 8.5  * FRAMESCALE);
		private static final double BSCHXSIZE  = (17.0  * FRAMESCALE);
		private static final double BSCHYSIZE  = (11.0  * FRAMESCALE);
		private static final double CSCHXSIZE  = (24.0  * FRAMESCALE);
		private static final double CSCHYSIZE  = (17.0  * FRAMESCALE);
		private static final double DSCHXSIZE  = (36.0  * FRAMESCALE);
		private static final double DSCHYSIZE  = (24.0  * FRAMESCALE);
		private static final double ESCHXSIZE  = (48.0  * FRAMESCALE);
		private static final double ESCHYSIZE  = (36.0  * FRAMESCALE);
		private static final double FRAMEWID   = ( 0.15 * FRAMESCALE);
		private static final double XLOGOBOX   = ( 2.0  * FRAMESCALE);
		private static final double YLOGOBOX   = ( 1.0  * FRAMESCALE);

		private Cell cell;
		private List lineFromEnd;
		private List lineToEnd;
		private List textPoint;
		private List textSize;
		private List textBox;
		private List textMessage;

		/**
		 * Constructor for cell frame descriptions.
		 * @param cell the Cell that is having a frame drawn.
		 */
		public FrameDescription(Cell cell)
		{
			this.cell = cell;
			lineFromEnd = new ArrayList();
			lineToEnd = new ArrayList();
			textPoint = new ArrayList();
			textSize = new ArrayList();
			textBox = new ArrayList();
			textMessage = new ArrayList();
			loadFrame();
		}

		/**
		 * Method to initialize the drawing of a frame.
		 * This method is overridden by subclasses that know how to do the function.
		 */
		public void renderInit() {}

		/**
		 * Method to draw a line in a frame.
		 * This method is overridden by subclasses that know how to do the function.
		 * @param from the starting point of the line (in database units).
		 * @param to the ending point of the line (in database units).
		 */
		public void showFrameLine(Point2D from, Point2D to) {}

		/**
		 * Method to draw text in a frame.
		 * This method is overridden by subclasses that know how to do the function.
		 * @param ctr the anchor point of the text.
		 * @param size the size of the text (in database units).
		 * @param maxWid the maximum width of the text (ignored if zero).
		 * @param maxHei the maximum height of the text (ignored if zero).
		 * @param string the text to be displayed.
		 */
		public void showFrameText(Point2D ctr, double size, double maxWid, double maxHei, String string) {}

		/**
		 * Method called to render the frame information.
		 * It makes calls to "renderInit()", "showFrameLine()", and "showFrameText()".
		 */
		public void renderFrame()
		{
			for(int i=0; i<lineFromEnd.size(); i++)
			{
				Point2D from = (Point2D)lineFromEnd.get(i);
				Point2D to = (Point2D)lineToEnd.get(i);
				showFrameLine(from, to);
			}
			for(int i=0; i<textPoint.size(); i++)
			{
				Point2D at = (Point2D)textPoint.get(i);
				double size = ((Double)textSize.get(i)).doubleValue();
				Point2D box = (Point2D)textBox.get(i);
				double width = box.getX();
				double height = box.getY();
				String msg = (String)textMessage.get(i);
				showFrameText(at, size, width, height, msg);
			}
		}

		/**
		 * Method to determine the size of the schematic frame in the current Cell.
		 * @param d a Dimension in which the size (database units) will be placed.
		 * @return 0: there should be a frame whose size is absolute;
		 * 1: there should be a frame but it combines with other stuff in the cell;
		 * 2: there is no frame.
		 */
		public static int getCellFrameInfo(Cell cell, Dimension d)
		{
			Variable var = cell.getVar(User.FRAME_SIZE, String.class);
			if (var == null) return 2;
			String frameInfo = (String)var.getObject();
			if (frameInfo.length() == 0) return 2;
			int retval = 0;
			char chr = frameInfo.charAt(0);
			double wid = 0, hei = 0;
			if (chr == 'x')
			{
				wid = XLOGOBOX + FRAMEWID;   hei = YLOGOBOX + FRAMEWID;
				retval = 1;
			} else
			{
				switch (chr)
				{
					case 'h': wid = HASCHXSIZE;  hei = HASCHYSIZE;  break;
					case 'a': wid = ASCHXSIZE;   hei = ASCHYSIZE;   break;
					case 'b': wid = BSCHXSIZE;   hei = BSCHYSIZE;   break;
					case 'c': wid = CSCHXSIZE;   hei = CSCHYSIZE;   break;
					case 'd': wid = DSCHXSIZE;   hei = DSCHYSIZE;   break;
					case 'e': wid = ESCHXSIZE;   hei = ESCHYSIZE;   break;
				}
			}
			if (frameInfo.indexOf("v") >= 0)
			{
				d.setSize(hei, wid);
			} else
			{
				d.setSize(wid, hei);
			}
			return retval;
		}

		private void loadFrame()
		{
			Dimension d = new Dimension();
			int frameFactor = getCellFrameInfo(cell, d);
			if (frameFactor == 2) return;

			Variable var = cell.getVar(User.FRAME_SIZE, String.class);
			if (var == null) return;
			String frameInfo = (String)var.getObject();
			double schXSize = d.getWidth();
			double schYSize = d.getHeight();

			boolean drawTitleBox = true;
			int xSections = 8;
			int ySections = 4;
			if (frameFactor == 1)
			{
				xSections = ySections = 0;
			} else
			{
				if (frameInfo.indexOf("v") >= 0)
				{
					xSections = 4;
					ySections = 8;
				}
				if (frameInfo.indexOf("n") >= 0) drawTitleBox = false;
			}

			double xLogoBox = XLOGOBOX;
			double yLogoBox = YLOGOBOX;
			double frameWid = FRAMEWID;

			// draw the frame
			if (xSections > 0)
			{
				double xSecSize = (schXSize - frameWid*2) / xSections;
				double ySecSize = (schYSize - frameWid*2) / ySections;

				// draw the outer frame
				Point2D point0 = new Point2D.Double(-schXSize/2, -schYSize/2);
				Point2D point1 = new Point2D.Double(-schXSize/2,  schYSize/2);
				Point2D point2 = new Point2D.Double( schXSize/2,  schYSize/2);
				Point2D point3 = new Point2D.Double( schXSize/2, -schYSize/2);
				addLine(point0, point1);
				addLine(point1, point2);
				addLine(point2, point3);
				addLine(point3, point0);

				// draw the inner frame
				point0 = new Point2D.Double(-schXSize/2 + frameWid, -schYSize/2 + frameWid);
				point1 = new Point2D.Double(-schXSize/2 + frameWid,  schYSize/2 - frameWid);
				point2 = new Point2D.Double( schXSize/2 - frameWid,  schYSize/2 - frameWid);
				point3 = new Point2D.Double( schXSize/2 - frameWid, -schYSize/2 + frameWid);
				addLine(point0, point1);
				addLine(point1, point2);
				addLine(point2, point3);
				addLine(point3, point0);

				// tick marks along the top and bottom sides
				for(int i=0; i<xSections; i++)
				{
					double x = i * xSecSize - (schXSize/2 - frameWid);
					if (i > 0)
					{
						point0 = new Point2D.Double(x, schYSize/2 - frameWid);
						point1 = new Point2D.Double(x, schYSize/2 - frameWid/2);
						addLine(point0, point1);
						point0 = new Point2D.Double(x, -schYSize/2 + frameWid);
						point1 = new Point2D.Double(x, -schYSize/2 + frameWid/2);
						addLine(point0, point1);
					}

					char chr = (char)('1' + xSections - i - 1);
					point0 = new Point2D.Double(x + xSecSize/2, schYSize/2 - frameWid/2);
					addText(point0, frameWid, 0, 0, String.valueOf(chr));

					point0 = new Point2D.Double(x + xSecSize/2, -schYSize/2 + frameWid/2);
					addText(point0, frameWid, 0, 0, String.valueOf(chr));
				}

				// tick marks along the left and right sides
				for(int i=0; i<ySections; i++)
				{
					double y = i * ySecSize - (schYSize/2 - frameWid);
					if (i > 0)
					{
						point0 = new Point2D.Double(schXSize/2 - frameWid, y);
						point1 = new Point2D.Double(schXSize/2 - frameWid/2, y);
						addLine(point0, point1);
						point0 = new Point2D.Double(-schXSize/2 + frameWid, y);
						point1 = new Point2D.Double(-schXSize/2 + frameWid/2, y);
						addLine(point0, point1);
					}
					char chr = (char)('A' + i);
					point0 = new Point2D.Double(schXSize/2 - frameWid/2, y + ySecSize/2);
					addText(point0, frameWid, 0, 0, String.valueOf(chr));

					point0 = new Point2D.Double(-schXSize/2 + frameWid/2, y + ySecSize/2);
					addText(point0, frameWid, 0, 0, String.valueOf(chr));
				}
			}
			if (drawTitleBox)
			{
				Point2D point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox, -schYSize/2 + frameWid + yLogoBox);
				Point2D point1 = new Point2D.Double(schXSize/2 - frameWid, -schYSize/2 + frameWid + yLogoBox);
				Point2D point2 = new Point2D.Double(schXSize/2 - frameWid, -schYSize/2 + frameWid);
				Point2D point3 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox, -schYSize/2 + frameWid);
				addLine(point0, point1);
				addLine(point1, point2);
				addLine(point2, point3);
				addLine(point3, point0);
		
				point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox, -schYSize/2 + frameWid + yLogoBox*2/15);
				point1 = new Point2D.Double(schXSize/2 - frameWid,            -schYSize/2 + frameWid + yLogoBox*2/15);
				addLine(point0, point1);

				point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox, -schYSize/2 + frameWid + yLogoBox*4/15);
				point1 = new Point2D.Double(schXSize/2 - frameWid,            -schYSize/2 + frameWid + yLogoBox*4/15);
				addLine(point0, point1);

				point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox, -schYSize/2 + frameWid + yLogoBox*6/15);
				point1 = new Point2D.Double(schXSize/2 - frameWid,            -schYSize/2 + frameWid + yLogoBox*6/15);
				addLine(point0, point1);

				point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox, -schYSize/2 + frameWid + yLogoBox*9/15);
				point1 = new Point2D.Double(schXSize/2 - frameWid,            -schYSize/2 + frameWid + yLogoBox*9/15);
				addLine(point0, point1);

				point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox, -schYSize/2 + frameWid + yLogoBox*12/15);
				point1 = new Point2D.Double(schXSize/2 - frameWid,            -schYSize/2 + frameWid + yLogoBox*12/15);
				addLine(point0, point1);

				point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox/2, -schYSize/2 + frameWid + yLogoBox*13.5/15);
				addText(point0, yLogoBox*2/15, xLogoBox, yLogoBox*3/15, "Name: " + cell.describe());

				String projectName = User.getFrameProjectName();
				Variable pVar = cell.getLibrary().getVar(User.FRAME_PROJECT_NAME, String.class);
				if (pVar != null) projectName = (String)pVar.getObject();
				point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox/2, -schYSize/2 + frameWid + yLogoBox*10.5/15);
				addText(point0, yLogoBox*2/15, xLogoBox, yLogoBox*3/15, projectName);

				String designerName = User.getFrameDesignerName();
				Variable dVar = cell.getLibrary().getVar(User.FRAME_DESIGNER_NAME, String.class);
				if (dVar != null) designerName = (String)dVar.getObject();
				dVar = cell.getVar(User.FRAME_DESIGNER_NAME, String.class);
				if (dVar != null) designerName = (String)dVar.getObject();
				point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox/2, -schYSize/2 + frameWid + yLogoBox*7.5/15);
				addText(point0, yLogoBox*2/15, xLogoBox, yLogoBox*3/15, designerName);

				String companyName = User.getFrameCompanyName();
				Variable cVar = cell.getLibrary().getVar(User.FRAME_COMPANY_NAME, String.class);
				if (cVar != null) companyName = (String)cVar.getObject();
				point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox/2, -schYSize/2 + frameWid + yLogoBox*5/15);
				addText(point0, yLogoBox*2/15, xLogoBox, yLogoBox*2/15, companyName);

				point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox/2, -schYSize/2 + frameWid + yLogoBox*3/15);
				addText(point0, yLogoBox*2/15, xLogoBox, yLogoBox*2/15, "Created: " + TextUtils.formatDate(cell.getCreationDate()));

				point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox/2, -schYSize/2 + frameWid + yLogoBox*1/15);
				addText(point0, yLogoBox*2/15, xLogoBox, yLogoBox*2/15, "Revised: " + TextUtils.formatDate(cell.getRevisionDate()));
			}
		}

		private void addLine(Point2D from, Point2D to)
		{
			lineFromEnd.add(from);
			lineToEnd.add(to);
		}

		private void addText(Point2D at, double size, double width, double height, String msg)
		{
			textPoint.add(at);
			textSize.add(new Double(size));
			textBox.add(new Point2D.Double(width, height));
			textMessage.add(msg);
		}
	}

	/****************************** NODES ******************************/

	/**
	 * Method to return an Iterator over all NodeInst objects in this Cell.
	 * @return an Iterator over all NodeInst objects in this Cell.
	 */
	public synchronized Iterator getNodes()
	{
        ArrayList nodesCopy = new ArrayList(nodes);
		return nodesCopy.iterator();
	}

	/**
	 * Method to return the number of NodeInst objects in this Cell.
	 * @return the number of NodeInst objects in this Cell.
	 */
	public int getNumNodes()
	{
		return nodes.size();
	}

	/**
	 * Method to return the NodeInst at specified position.
	 * @param nodeIndex specified position of NodeInst.
	 * @return the NodeInst at specified position..
	 */
	public final NodeInst getNode(int nodeIndex)
	{
		return (NodeInst)nodes.get(nodeIndex);
	}

	/**
	 * Method to return an Iterator over all NodeUsage objects in this Cell.
	 * @return an Iterator over all NodeUsage objects in this Cell.
	 */
	public synchronized Iterator getUsagesIn()
	{
        Map usagesCopy = new HashMap(usagesIn);
		return usagesCopy.values().iterator();
	}

	/**
	 * Method to return the number of NodeUsage objects in this Cell.
	 * @return the number of NodeUsage objects in this Cell.
	 */
	public int getNumUsagesIn()
	{
		return usagesIn.size();
	}

	/**
	 * Method to find a named NodeInst on this Cell.
	 * @param name the name of the NodeInst.
	 * @return the NodeInst.  Returns null if none with that name are found.
	 */
	public NodeInst findNode(String name)
	{
		int n = nodes.size();
		for (int i = 0; i < n; i++)
		{
			NodeInst ni = (NodeInst) nodes.get(i);
			String nodeNm = ni.getName();
			if (nodeNm != null && nodeNm.equals(name))
				return ni;
		}
		return null;
	}

	private static boolean allowCirDep = false;

	/**
	 * Method to allow temporarily circular library dependences
	 * (for example to read legacy libraries).
	 * It is called only from synchronyzed method Input.readLibrary.
	 * @param val true allows circular dependencies.
	 */
	public static void setAllowCircularLibraryDependences(boolean val)
	{
		allowCirDep = val;
	}

	/**
	 * Method to add a new NodeInst to the cell.
	 * @param ni the NodeInst to be included in the cell.
	 */
	public NodeUsage addNode(NodeInst ni)
	{
		checkChanging();
		NodeUsage nu = addUsage(ni.getProto());

		// error check
		if (nu.contains(ni))
		{
			System.out.println("Cell " + this +" already contains node inst " + ni);
			return null;
		}

        // check to see if this instantiation would create a circular library dependency
        NodeProto protoType = ni.getProto();
        if (protoType instanceof Cell) {
            Cell instProto = (Cell)protoType;
            if (instProto.getLibrary() != getLibrary()) {
                // a reference will be created, check it
                Library.LibraryDependency libDep = getLibrary().addReferencedLib(instProto.getLibrary());
                if (libDep != null) {
                    // addition would create circular dependency
                    if (!allowCirDep) {
                        System.out.println("ERROR: "+ libDescribe() + " cannot instantiate " +
                             instProto.libDescribe() + " because it would create a circular library dependence: ");
                        System.out.println(libDep.toString());
                        return null;
                    } else {
                        System.out.println("WARNING: "+ libDescribe() + " instantiates " +
                            instProto.libDescribe() + " which causes a circular library dependence: ");
                        System.out.println(libDep.toString());
                    }
                }
            }
        }

		// add the node
		ni.setNodeIndex(nodes.size());
		nodes.add(ni);
		addTempName(ni);
		nu.addInst(ni);

		// must recompute the bounds of the cell
		boundsDirty = true;

		// make additional checks to keep circuit up-to-date
		NodeProto np = ni.getProto();
		if (np instanceof PrimitiveNode && np == Generic.tech.cellCenterNode)
		{
			adjustReferencePoint(ni);
		}
		if (np instanceof PrimitiveNode
			&& np.getName().equals("Essential-Bounds"))
		{
			essenBounds.add(ni);
		}
		return nu;
	}

	/**
	 * Method to remove an NodeInst from the cell.
	 * @param ni the NodeInst to be removed from the cell.
	 */
	public void removeNode(NodeInst ni)
	{
		checkChanging();
		NodeUsage nu = ni.getNodeUsage();
		if (nu == null || !nu.contains(ni))
		{
			System.out.println("Cell " + this +" doesn't contain node inst " + ni);
			return;
		}
		nu.removeInst(ni);
		if (nu.isEmpty())
			removeUsage(nu);

		removeTempName(ni);
		int nodeIndex = ni.getNodeIndex();
		int lastNode = nodes.size() - 1;
		if (nodeIndex == lastNode)
		{
			nodes.remove(nodeIndex);
		} else
		{
			NodeInst lastNi = (NodeInst) nodes.remove(lastNode);
			nodes.set(nodeIndex, lastNi);
			lastNi.setNodeIndex(nodeIndex);
		}
		ni.setNodeIndex(-1);

        // remove library dependency, if possible
        if (ni.getProto() instanceof Cell) {
            getLibrary().removeReferencedLib(((Cell)ni.getProto()).getLibrary());
        }

		// must recompute the bounds of the cell
		boundsDirty = true;

		essenBounds.remove(ni);
	}

	/**
	 * Method to find or to to add a new NodeUsage to the cell.
	 * @param protoType is a NodeProto of node usage
	 */
	private NodeUsage addUsage(NodeProto protoType)
	{
		if (!isLinked()) System.out.println("addUsage of "+protoType+" to unlinked "+this);
		NodeUsage nu = (NodeUsage)usagesIn.get(protoType);
		if (nu == null)
		{
			nu = new NodeUsage(protoType, this);
			usagesIn.put(protoType, nu);
			if (protoType instanceof Cell)
				((Cell)protoType).usagesOf.add(nu);
		}
		return nu;
	}

	/**
	 * Method to remove a NodeUsage of the cell.
	 * @param nu is a NodeUsage to remove
	 */
	private void removeUsage(NodeUsage nu)
	{
		if (!isLinked()) System.out.println("removeUsage of "+nu.getProto()+" to unliked "+this);
		NodeProto protoType = nu.getProto();
		if (protoType instanceof Cell)
			((Cell)protoType).usagesOf.remove(nu);
		usagesIn.remove(protoType);
	}

	/****************************** ARCS ******************************/

	/**
	 * Method to return an Iterator over all ArcInst objects in this Cell.
	 * @return an Iterator over all ArcInst objects in this Cell.
	 */
	public synchronized Iterator getArcs()
	{
        ArrayList arcsCopy = new ArrayList(arcs);
		return arcsCopy.iterator();
	}

	/**
	 * Method to return the number of ArcInst objects in this Cell.
	 * @return the number of ArcInst objects in this Cell.
	 */
	public int getNumArcs()
	{
		return arcs.size();
	}

	/**
	 * Method to return the ArcInst at specified position.
	 * @param arcIndex specified position of ArcInst.
	 * @return the ArcInst at specified position..
	 */
	public final ArcInst getArc(int arcIndex)
	{
		return (ArcInst)arcs.get(arcIndex);
	}

	/**
	 * Method to find a named ArcInst on this Cell.
	 * @param name the name of the ArcInst.
	 * @return the ArcInst.  Returns null if none with that name are found.
	 */
	public ArcInst findArc(String name)
	{
		int a = arcs.size();
		for (int i = 0; i < a; i++)
		{
			ArcInst ai = (ArcInst)arcs.get(i);
			String arcNm = ai.getName();
			if (arcNm != null && arcNm.equals(name))
				return ai;
		}
		return null;
	}

	/**
	 * Method to add a new ArcInst to the cell.
	 * @param ai the ArcInst to be included in the cell.
	 */
	public void addArc(ArcInst ai)
	{
		checkChanging();
		if (arcs.contains(ai))
		{
			System.out.println("Cell " + this +" already contains arc " + ai);
			return;
		}
		ai.setArcIndex(arcs.size());
		arcs.add(ai);
		addTempName(ai);

		// must recompute the bounds of the cell
		boundsDirty = true;
	}

	/**
	 * Method to remove an ArcInst from the cell.
	 * @param ai the ArcInst to be removed from the cell.
	 */
	public void removeArc(ArcInst ai)
	{
		checkChanging();
		if (!arcs.contains(ai))
		{
			System.out.println("Cell " + this +" doesn't contain arc " + ai);
			return;
		}

		removeTempName(ai);
		int arcIndex = ai.getArcIndex();
		int lastArc = arcs.size() - 1;
		if (arcIndex == lastArc)
		{
			arcs.remove(arcIndex);
		} else
		{
			ArcInst lastAi = (ArcInst) arcs.remove(lastArc);
			arcs.set(arcIndex, lastAi);
			lastAi.setArcIndex(arcIndex);
		}
		ai.setArcIndex(-1);

		// must recompute the bounds of the cell
		boundsDirty = true;
	}

	/****************************** EXPORTS ******************************/

	/**
	 * Add a PortProto to this NodeProto.
	 * Adds Exports for Cells, PrimitivePorts for PrimitiveNodes.
	 * @param export the PortProto to add to this NodeProto.
	 * @param oldPortInsts a collection of PortInsts to Undo or null.
	 */
	void addExport(Export export, Collection oldPortInsts)
	{
		checkChanging();
		export.setPortIndex(exports.size());
		exports.add(export);

		// create a PortInst for every instance of this node
		if (oldPortInsts != null)
		{
			for(Iterator it = oldPortInsts.iterator(); it.hasNext(); )
			{
				PortInst pi = (PortInst)it.next();
				pi.getNodeInst().linkPortInst(pi);
			}
		} else
		{
			for(Iterator it = getInstancesOf(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni.addPortInst(export);
			}
		}
	}

	/**
	 * Removes an Export from this Cell.
	 * @param export the Export to remove from this Cell.
	 * @return collection of deleted PortInsts of the Export.
	 */
	Collection removeExport(Export export)
	{
		checkChanging();
		int portIndex = export.getPortIndex();
		exports.remove(portIndex);
		for (; portIndex < exports.size(); portIndex++)
		{
			((Export)exports.get(portIndex)).setPortIndex(portIndex);
		}

		Collection portInsts = new ArrayList();
		// remove the PortInst from every instance of this node
		for(Iterator it = getInstancesOf(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			portInsts.add(ni.removePortInst(export));
		}

		export.setPortIndex(-1);
		return portInsts;
	}

	/**
	 * Method to find the PortProto that has a particular name.
	 * @return the PortProto, or null if there is no PortProto with that name.
	 */
	public PortProto findPortProto(String name)
	{
        if (name == null) return null;
		return findPortProto(Name.findName(name));
	}

	/**
	 * Method to find the PortProto that has a particular Name.
	 * @return the PortProto, or null if there is no PortProto with that name.
	 */
	public PortProto findPortProto(Name name)
	{
        if (name == null) return null;
		name = name.lowerCase();
		for (int i = 0; i < exports.size(); i++)
		{
			PortProto pp = (PortProto) exports.get(i);
			if (pp.getNameKey().lowerCase() == name)
				return pp;
		}
		return null;
	}

	/**
	 * Method to determine if a given PortProto is considered as export
	 * @param port
	 * @return
	 */
	public boolean findPortProto(PortProto port)
	{
		for (int i = 0; i < exports.size(); i++)
		{
			PortProto pp = (PortProto) exports.get(i);
			if (pp == port) return true;
		}
		return false;
	}

	/**
	 * Method to return an iterator over all PortProtos of this NodeProto.
	 * @return an iterator over all PortProtos of this NodeProto.
	 */
	public Iterator getPorts()
	{
		return exports.iterator();
	}

	/**
	 * Method to return the number of PortProtos on this NodeProto.
	 * @return the number of PortProtos on this NodeProto.
	 */
	public int getNumPorts()
	{
		return exports.size();
	}

	/**
	 * Method to return the PortProto at specified position.
	 * @param portIndex specified position of PortProto.
	 * @return the PortProto at specified position..
	 */
	public final PortProto getPort(int portIndex)
	{
		return (PortProto)exports.get(portIndex);
	}

	/**
	 * Method to find a named Export on this Cell.
	 * @param name the name of the export.
	 * @return the export.  Returns null if that name was not found.
	 */
	public Export findExport(String name)
	{
		return (Export) findPortProto(name);
	}

	/**
	 * Method to find a named Export on this Cell.
	 * @param name the Name of the export.
	 * @return the export.  Returns null if that name was not found.
	 */
	public Export findExport(Name name)
	{
		return (Export) findPortProto(name);
	}

	/****************************** TEXT ******************************/

	/**
	 * Method to return the pure name of this Cell, without
	 * any view or version information.
	 * @return the pure name of this Cell.
	 */
	public String getName() { return protoName; }

	/**
	 * Method to return the CellName object describing this Cell.
	 * @return the CellName object describing this Cell.
	 */
	public CellName getCellName()
	{
		return CellName.parseName(protoName + ";" + version + "{" + view.getAbbreviation() + "}");
	}

	/**
	 * Method to describe this cell.
	 * The description has the form: cell;version{view}
	 * If the cell is not from the current library, prepend the library name.
	 * @return a String that describes this cell.
	 */
	public String describe()
	{
		String name = "";
		if (lib != Library.getCurrent())
			name += lib.getName() + ":";
		name += noLibDescribe();
		return name;
	}

    /**
     * Method to describe this cell.
     * The description has the form: Library:cell;version{view}
     * @return a String that describes this cell.
     */
    public String libDescribe()
    {
        return (lib.getName() + ":" + noLibDescribe());
    }

	/**
	 * Method to describe this cell.
	 * The description has the form: cell;version{view}
	 * Unlike "describe()", this method never prepends the library name.
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
	 * Method to find the NodeProto with the given name.
	 * This can be a PrimitiveNode (and can be prefixed by a Technology name),
	 * or it can be a Cell (and be prefixed by a Library name).
	 * @param line the name of the NodeProto.
	 * @return the specified NodeProto, or null if none can be found.
	 */
	public static NodeProto findNodeProto(String line)
	{
		Technology tech = Technology.getCurrent();
		Library lib = Library.getCurrent();
		boolean saidtech = false;
		boolean saidlib = false;
		int colon = line.indexOf(':');
		String withoutPrefix;
		if (colon == -1) withoutPrefix = line; else
		{
			String prefix = line.substring(0, colon);
			Technology t = Technology.findTechnology(prefix);
			if (t != null)
			{
				tech = t;
				saidtech = true;
			}
			Library l = Library.findLibrary(prefix);
			if (l != null)
			{
				lib = l;
				saidlib = true;
			}
			withoutPrefix = line.substring(colon+1);
		}

		/* try primitives in the technology */
		if (!saidlib)
		{
			PrimitiveNode np = tech.findNodeProto(withoutPrefix);
			if (np != null) return np;
		}
		
		if (!saidtech)
		{
			Cell np = lib.findNodeProto(withoutPrefix);
			if (np != null) return np;
		}
		return null;
	}

	/**
	 * Method to get the strings in this Cell.
	 * It is only valid for cells with "text" views (documentation, vhdl, netlist, etc.)
	 * @return the strings in this Cell.
	 * Returns null if there are no strings.
	 */
	public String [] getTextViewContents()
	{
		// first see if this cell is being actively edited in a TextWindow
		String [] strings = TextWindow.getEditedText(this);
		if (strings != null) return strings;

		// look on the cell for its text
		Variable var = getVar(Cell.CELL_TEXT_KEY);
		if (var == null) return null;
		Object obj = var.getObject();
		if (!(obj instanceof String[])) return null;
		return (String [])obj;
	}

	/**
	 * Method to get the strings in this Cell.
	 * It is only valid for cells with "text" views (documentation, vhdl, netlist, etc.)
	 * The call needs to be wrapped inside of a Job.
	 * Returns null if there are no strings.
	 */
	public void setTextViewContents(String [] strings)
	{
		Job.checkChanging();

		// see if this cell is being actively edited in a TextWindow
		TextWindow.updateText(this, strings);

		newVar(Cell.CELL_TEXT_KEY, strings);
	}

	/**
	 * Method to return a list of Polys that describes all text on this Cell.
	 * @param hardToSelect is true if considering hard-to-select text.
	 * @param wnd the window in which the text will be drawn.
	 * @return an array of Polys that describes the text.
	 */
	public Poly [] getAllText(boolean hardToSelect, EditWindow wnd)
	{
		int dispVars = numDisplayableVariables(false);
		if (dispVars == 0) return null;
		Poly [] polys = new Poly[dispVars];

		// add in the displayable variables
		addDisplayableVariables(CENTERRECT, polys, 0, wnd, false);
		return polys;
	}

	/**
	 * Method to return the bounds of all relative text in this Cell.
	 * This is used when displaying "full screen" because the text may grow to
	 * be larger than the actual cell contents.
	 * Only relative (scalable) text is considered, since it is not possible
	 * to change the size of absolute text.
	 * @param wnd the EditWindow in which this Cell is being displayed.
	 * @return the bounds of the relative (scalable) text.
	 */
	public Rectangle2D getRelativeTextBounds(EditWindow wnd)
	{
		Rectangle2D bounds = null;
		for(Iterator it = this.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			bounds = accumulateTextBoundsOnObject(ni, bounds, wnd);
			for(Iterator pIt = ni.getPortInsts(); pIt.hasNext(); )
			{
				PortInst pi = (PortInst)pIt.next();
				bounds = accumulateTextBoundsOnObject(pi, bounds, wnd);
			}
		}
		for(Iterator it = this.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			bounds = accumulateTextBoundsOnObject(ai, bounds, wnd);
		}
		for(Iterator it = this.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			bounds = accumulateTextBoundsOnObject(pp, bounds, wnd);
		}
		bounds = accumulateTextBoundsOnObject(this, bounds, wnd);
		return bounds;
	}

	private Rectangle2D accumulateTextBoundsOnObject(ElectricObject eObj, Rectangle2D bounds, EditWindow wnd)
	{
		for(Iterator vIt = eObj.getVariables(); vIt.hasNext(); )
		{
			Variable var = (Variable)vIt.next();
			if (!var.isDisplay()) continue;
			TextDescriptor td = var.getTextDescriptor();
			if (td.getSize().isAbsolute()) continue;
			Poly poly = eObj.computeTextPoly(wnd, var, null);
			if (poly == null) continue;
			Rectangle2D polyBound = poly.getBounds2D();
			if (bounds == null) bounds = polyBound; else
				Rectangle2D.union(bounds, polyBound, bounds);
		}

		if (eObj instanceof Geometric)
		{
			Geometric geom = (Geometric)eObj;
			Name name = geom.getNameKey();
			if (!name.isTempname())
			{
				Poly poly = eObj.computeTextPoly(wnd, null, name);
				if (poly != null)
				{
					Rectangle2D polyBound = poly.getBounds2D();
					if (bounds == null) bounds = polyBound; else
						Rectangle2D.union(bounds, polyBound, bounds);
				}
			}
		}
		if (eObj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)eObj;
			for(Iterator it = ni.getExports(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				Poly poly = pp.computeTextPoly(wnd, null, null);
				if (poly != null)
				{
					Rectangle2D polyBound = poly.getBounds2D();
					if (bounds == null) bounds = polyBound; else
						Rectangle2D.union(bounds, polyBound, bounds);
				}
			}
		}
		return bounds;
	}

	/**
	 * Method to return the basename for autonaming instances of this Cell.
	 * @return the basename for autonaming instances of this Cell.
	 */
	public Name getBasename() { return basename; }

	/**
	 * Method to return unique autoname in this cell.
	 * @param basename base name of autoname
	 * @return autoname
	 */
	public Name getAutoname(Name basename)
	{
		MaxSuffix ms = (MaxSuffix)maxSuffix.get(basename);
		if (ms == null)
		{
			ms = new MaxSuffix();
			maxSuffix.put(basename.lowerCase(), ms);
			return basename.findSuffixed(0);
		} else 
		{
			ms.v++;
			return basename.findSuffixed(ms.v);
		}
	}

	/**
	 * Method to add a new temporary name of Geometric.
	 * @param geom the Geometric to be added to the cell.
	 */
	public void addTempName(Geometric geom)
	{
		Name name = geom.getNameKey();
		if (!name.isTempname()) return;
		tempNames.put(name.lowerCase(), geom);

		Name basename = name.getBasename();
		if (basename != null && basename != name)
		{
			basename = basename.lowerCase(); 
			MaxSuffix ms = (MaxSuffix) maxSuffix.get(basename);
			if (ms == null)
			{
				ms = new MaxSuffix();
				maxSuffix.put(basename, ms);
			}
			int numSuffix = name.getNumSuffix();
			if (numSuffix > ms.v)
			{
				ms.v = numSuffix;
			}
		}
	}

	/**
	 * Method to remove temporary name of Geometric.
	 * @param geom the Geometric to be removed from the cell.
	 */
	public void removeTempName(Geometric geom)
	{
		Name name = geom.getNameKey();
		if (!name.isTempname()) return;
		tempNames.remove(name.lowerCase());
	}

	/**
	 * Method check if Geometric with specified temporary name key exists in a cell.
	 * @param name specified temorary name key.
	 */
	public boolean hasTempName(Name name)
	{
		return tempNames.get(name) != null;
	}

	/**
	 * Method to determine the index value which, when appended to a given string,
	 * will generate a unique name in this Cell.
	 * @param prefix the start of the string.
	 * @param cls the type of object being examined.
	 * @param startingIndex the starting value to append to the string.
	 * @return a value that, when appended to the prefix, forms a unique name in the cell.
	 */
	public int getUniqueNameIndex(String prefix, Class cls, int startingIndex)
	{
		int len = prefix.length();
		int uniqueIndex = startingIndex;
		if (cls == PortProto.class)
		{
			for(Iterator it = getPorts(); it.hasNext(); )
			{
				PortProto pp = (PortProto)it.next();
				if (pp.getName().startsWith(prefix))
				{
					String restOfName = pp.getName().substring(len);
					if (TextUtils.isANumber(restOfName))
					{
						int indexVal = TextUtils.atoi(restOfName);
						if (indexVal >= uniqueIndex) uniqueIndex = indexVal + 1;
					}
				}
			}
		} else if (cls == NodeInst.class)
		{
			for(Iterator it = getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (ni.getName().startsWith(prefix))
				{
					String restOfName = ni.getName().substring(len);
					if (TextUtils.isANumber(restOfName))
					{
						int indexVal = TextUtils.atoi(restOfName);
						if (indexVal >= uniqueIndex) uniqueIndex = indexVal + 1;
					}
				}
			}
		} else if (cls == ArcInst.class)
		{
			for(Iterator it = getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				if (ai.getName().startsWith(prefix))
				{
					String restOfName = ai.getName().substring(len);
					if (TextUtils.isANumber(restOfName))
					{
						int indexVal = TextUtils.atoi(restOfName);
						if (indexVal >= uniqueIndex) uniqueIndex = indexVal + 1;
					}
				}
			}
		}
		return uniqueIndex;
	}

	/**
	 * Method to determine whether a name is unique in this Cell.
	 * @param name the Name being tested to see if it is unique.
	 * @param cls the type of object being examined.
	 * The only classes that can be examined are PortProto, NodeInst, and ArcInst.
	 * @param exclude an object that should not be considered in this test (null to ignore the exclusion).
	 * @return true if the name is unique in the Cell.  False if it already exists.
	 */
	public boolean isUniqueName(String name, Class cls, ElectricObject exclude)
	{
		return isUniqueName(Name.findName(name), cls, exclude);
	}

	/**
	 * Method to determine whether a name is unique in this Cell.
	 * @param name the Name being tested to see if it is unique.
	 * @param cls the type of object being examined.
	 * The only classes that can be examined are PortProto, NodeInst, and ArcInst.
	 * @param exclude an object that should not be considered in this test (null to ignore the exclusion).
	 * @return true if the name is unique in the Cell.  False if it already exists.
	 */
	public boolean isUniqueName(Name name, Class cls, ElectricObject exclude)
	{
		name = name.lowerCase();
		if (cls == PortProto.class)
		{
			PortProto pp = findExport(name);
			if (pp == null || exclude == pp) return true;
			return false;
		}
		if (cls == NodeInst.class)
		{
			if (name.isTempname())
			{
				Geometric geom = (Geometric)tempNames.get(name);
				return geom == null || exclude == geom;
			}
			for(Iterator it = getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (exclude == ni) continue;
				Name nodeName = ni.getNameKey();
				if (name == nodeName.lowerCase()) return false;
			}
			return true;
		}
		if (cls == ArcInst.class)
		{
			if (name.isTempname())
			{
				Geometric geom = (Geometric)tempNames.get(name);
				return geom == null || exclude == geom;
			}
			for(Iterator it = getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				if (exclude == ai) continue;
				Name arcName = ai.getNameKey();
				if (name == arcName.lowerCase()) return false;
			}
			return true;
		}
		return true;
	}

	/**
	 * Method to determine whether a variable key on Cell is deprecated.
	 * Deprecated variable keys are those that were used in old versions of Electric,
	 * but are no longer valid.
	 * @param key the key of the variable.
	 * @return true if the variable key is deprecated.
	 */
	public boolean isDeprecatedVariable(Variable.Key key)
	{
		String name = key.getName();
		if (name.equals("NET_last_good_ncc") ||
			name.equals("NET_last_good_ncc_facet") ||
			name.equals("SIM_window_signal_order")) return true;
		return super.isDeprecatedVariable(key);
	}

	/**
	 * Returns a printable version of this Cell.
	 * @return a printable version of this Cell.
	 */
	public String toString()
	{
		return "Cell " + describe();
	}

	/****************************** HIERARCHY ******************************/

	/**
	 * Method to return an iterator over all usages of this NodeProto.
	 * @return an iterator over all usages of this NodeProto.
	 */
	public Iterator getUsagesOf()
	{
		return usagesOf.iterator();
	}

	/**
	 * Method to return an iterator over all instances of this NodeProto.
	 * @return an iterator over all instances of this NodeProto.
	 */
	public Iterator getInstancesOf()
	{
		return new NodeInstsIterator();
	}

	private class NodeInstsIterator implements Iterator
	{
		private Iterator uit;
		private NodeUsage nu;
		private int i, n;

		NodeInstsIterator()
		{
			uit = getUsagesOf();
			i = n = 0;
			while (i >= n && uit.hasNext())
			{
				nu = (NodeUsage)uit.next();
				n = nu.getNumInsts();
			}
		}

		public boolean hasNext() { return i < n; }

		public Object next()
		{
			if (i >= n) uit.next(); // throw NoSuchElementException
			NodeInst ni = nu.getInst(i);
			i++;
			while (i >= n && uit.hasNext())
			{
				nu = (NodeUsage)uit.next();
				n = nu.getNumInsts();
				i = 0;
			}
			return ni;
		}

		public void remove() { throw new UnsupportedOperationException("NodeInstsIterator.remove()"); };
	}

    /**
     * Determines whether an instantiation of cell <code>toInstantiate</code>
     * into <code>parent</code> would be a rescursive operation.
     * @param toInstantiate the cell to instantiate
     * @param parent the cell in which to create the instance
     * @return true if the operation would be recursive, false otherwise
     */
    public static boolean isInstantiationRecursive(Cell toInstantiate, Cell parent) {
        // if they are equal, this is recursive
        if (toInstantiate == parent) return true;

        // special case: allow instance of icon inside of the contents for illustration
        if (toInstantiate.isIconOf(parent)) {
            if (toInstantiate.isIcon() && !parent.isIcon())
                return false;
        }

        // if the parent is a child of the cell to instantiate, that would be a
        // recursive operation
        if (parent.isAChildOf(toInstantiate)) return true;

        return false;
    }

	/**
	 * Method to determine whether this Cell is a child of a given parent Cell.
	 * DO NOT use this method to determine whether an instantiation should be allowed
     * (i.e. it is not a recursive instantation).  Use <code>isInstantiationRecursive</code>
     * instead.  This method *only* does what is it says it does: it checks if this cell
     * is currently instantiated as a child of 'parent' cell.
	 * @param parent the parent cell being examined.
	 * @return true if, somewhere above the hierarchy of this Cell is the parent Cell.
	 */
	public boolean isAChildOf(Cell parent)
	{
		return getIsAChildOf(parent, new HashMap());
	}

	private boolean getIsAChildOf(Cell parent, Map checkedParents)
	{
        // if parent is an icon view, also check contents view
        if (parent.isIcon()) {
            Cell c = parent.contentsView();
            if (c != null && c != parent) {
                if (getIsAChildOf(c, checkedParents)) return true;
            }
        }

        // see if parent checked already
        if (checkedParents.get(parent) != null) return false;
        // mark this parent as being checked so we don't recurse into it again
        checkedParents.put(parent, parent);

        //System.out.println("Checking if this "+describe()+" is a child of "+parent.describe());

        // see if any instances of this have parent 'parent'
        // check both icon and content views
        // Note that contentView and iconView are the same for every recursion
        Cell contentView = contentsView();
        if (contentView == null) contentView = this;
        Cell iconView = iconView();

        for (Iterator it = parent.getNodes(); it.hasNext(); ) {
            NodeInst ni = (NodeInst)it.next();
            NodeProto np = ni.getProto();
            if (np instanceof Cell) {
                Cell c = (Cell)np;
                // ignore instances of icon view inside content view
                if (c.isIconOf(parent)) continue;
                if (c == contentView) return true;
                if (c == iconView) return true;
                // recurse
                if (getIsAChildOf(c, checkedParents)) return true;
            }
        }
        return false;
    }

    private boolean getIsAParentOf(Cell child)
    {
        if (this == child) return true;

		// look through every instance of the child cell
		Cell lastParent = null;
		for(Iterator it = child.getInstancesOf(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();

			// if two instances in a row have same parent, skip this one
			if (ni.getParent() == lastParent) continue;
			lastParent = ni.getParent();

			// recurse to see if the grandparent belongs to the child
			if (getIsAParentOf(ni.getParent())) return true;
		}

		// if this has an icon, look at it's instances
		Cell np = child.iconView();
		if (np != null)
		{
			lastParent = null;
			for(Iterator it = np.getInstancesOf(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();

				// if two instances in a row have same parent, skip this one
				if (ni.getParent() == lastParent) continue;
				lastParent = ni.getParent();

				// special case: allow an icon to be inside of the contents for illustration
				NodeProto niProto = ni.getProto();
				if (niProto instanceof Cell)
				{
					if (((Cell)niProto).isIconOf(child))
					{
						if (!child.isIcon()) continue;
					}
				}

				// recurse to see if the grandparent belongs to the child
				if (getIsAParentOf(ni.getParent())) return true;
			}
		}
		return false;
	}

	/**
	 * Method to determine whether this Cell is in use anywhere.
	 * If it is, an error dialog is displayed.
	 * @param action a description of the intended action (i.e. "delete").
	 * @return true if this Cell is in use anywhere.
	 */
	public boolean isInUse(String action)
	{
		String parents = null;
		for(Iterator it = getUsagesOf(); it.hasNext(); )
		{
			NodeUsage nu = (NodeUsage)it.next();
			Cell parent = nu.getParent();
			if (parents == null) parents = parent.describe(); else
				parents += ", " + parent.describe();
		}
		if (parents != null)
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), "Cannot " + action + " cell " + describe() +
				" because it is used in " + parents,
					action + " failed", JOptionPane.ERROR_MESSAGE);
			return true;
		}
		return false;
	}

	/****************************** VERSIONS ******************************/

	/**
	 * Method to create a new version of this Cell.
	 * @return a new Cell that is a new version of this Cell.
	 */
	public Cell makeNewVersion()
	{
		Cell newVersion = Cell.copyNodeProto(this, lib, noLibDescribe(), false);
		return newVersion;
	}

	/**
	 * Method to return the version number of this Cell.
	 * @return the version number of this Cell.
	 */
	public int getVersion() { return version; }

	/**
	 * Method to return the number of different versions of this Cell.
	 * @return the number of different versions of this Cell.
	 */
	public int getNumVersions()
	{
		if (versionGroup == null) return 1;
		return versionGroup.size();
	}

	/**
	 * Method to return an Iterator over the different versions of this Cell.
	 * @return an Iterator over the different versions of this Cell.
	 */
	public Iterator getVersions()
	{
		// don't know why, but keep getting null pointer exceptions on version group
		if (versionGroup == null) {
			VersionGroup vg = new VersionGroup();
			vg.add(this);
			return vg.iterator();
		}
		return versionGroup.iterator();
	}

	/**
	 * Method to return the most recent version of this Cell.
	 * @return he most recent version of this Cell.
	 */
	public Cell getNewestVersion()
	{
		return (Cell) getVersions().next();
	}

	/**
	 * Method to put this Cell into the given VersionGroup.
	 * @param versionGroup the VersionGroup that this cell belongs to.
	 */
	public void setVersionGroup(VersionGroup versionGroup) { this.versionGroup = versionGroup; }

	/****************************** GROUPS ******************************/

	/**
	 * Method to move this Cell to the group of another Cell.
	 * @param otherCell the other cell whose group this Cell should join.
	 */
	public void joinGroup(Cell otherCell)
	{
		setCellGroup(otherCell.getCellGroup());
	}

	/**
	 * Method to get the CellGroup that this Cell is part of.
	 * @return the CellGroup that this Cell is part of.
	 */
	public CellGroup getCellGroup() { return cellGroup; }

	/**
	 * Method to put this Cell into its own CellGroup.
	 * If it is already the only Cell in its CellGroup, nothing is done.
	 */
	public void putInOwnCellGroup()
	{
		if (cellGroup.getNumCells() == 1) return;

		CellGroup newGroup = new CellGroup();
		setCellGroup(newGroup);
	}

	/**
	 * Method to put this Cell into the given CellGroup.
	 * @param cellGroup the CellGroup that this cell belongs to.
	 */
	public void setCellGroup(CellGroup cellGroup)
	{
        CellGroup oldGroup = this.cellGroup;
        for(Iterator it = this.getVersions(); it.hasNext(); )
        {
        	Cell cell = (Cell)it.next();
        	cell.lowLevelSetCellGroup(cellGroup);
            Undo.modifyCellGroup(cell, oldGroup);
        }
	}

    /**
     * Low-level method to set a Cell's cell group. Do not use this
     * unless you know what you are doing. This method bypasses Undo.
     * @param cellGroup the new cell group
     */
    public void lowLevelSetCellGroup(CellGroup cellGroup) {

        checkChanging();

        if (cellGroup == null)
        {
            Exception e = new Exception("Cannot set CellGroup to NULL!");
            ActivityLogger.logException(e);
        }
        // stop if already that way
        if (this.cellGroup == cellGroup) return;

        if (this.cellGroup != null) this.cellGroup.remove(this);
        this.cellGroup = cellGroup;
        if (cellGroup != null) cellGroup.add(this);
    }

	/****************************** VIEWS ******************************/

	/**
	 * Method to get this Cell's View.
	 * Views include "layout", "schematics", "icon", "netlist", etc.
	 * @return to get this Cell's View.
	 */
	public View getView() { return view; }

	/**
	 * Method to change the view of this Cell.
	 * @param newView the new View.
	 */
	public void setView(View newView)
	{
		// stop now if already this view
		if (newView == view) return;

		// unlink this Cell
		lowLevelUnlink();

		// if there is already another with the same view, name, and version, make this a newer version
		int newVersion = version;
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell other = (Cell)it.next();
			if (other.view != newView) continue;
			if (!other.protoName.equalsIgnoreCase(protoName)) continue;
			if (other.version >= newVersion) newVersion = other.version + 1;
		}

		// set the new view and version
		view = newView;
		version = newVersion;

		// link the Cell back
		lowLevelLink();
	}

	/**
	 * Method to determine whether this Cell is an icon Cell.
	 * @return true if this Cell is an icon  Cell.
	 */
	public boolean isIcon() { return view == View.ICON; }

	/**
	 * Method to determine whether this Cell is an icon of another Cell.
	 * @param cell the other cell which this may be an icon of.
	 * @return true if this Cell is an icon of that other Cell.
	 */
	public boolean isIconOf(Cell cell)
	{
		return view == View.ICON && cellGroup == cell.cellGroup && cell.isSchematic();
	}

	/**
	 * Method to tell if this Cell is icon cell which is a part of multi-part icon.
	 * @return true if this Cell is part of multi-part icon.
	 */
	public boolean isMultiPartIcon() { return false; }

	/**
	 * Method to return true if this Cell is a schematic Cell.
	 * @return true if this Cell is a schematic Cell.
	 */
	public boolean isSchematic()
	{
		if (getView() == View.SCHEMATIC) return true;
		if (getView().isMultiPageView()) return true; // isn't it deprecated
		return false;
	}

	/**
	 * Method to find the contents Cell associated with this Cell.
	 * This only makes sense if the current Cell is an icon or skeleton Cell.
	 * @return the contents Cell associated with this Cell.
	 * Returns null if no such Cell can be found.
	 */
	public Cell contentsView()
	{
		// can only consider contents if this cell is an icon
		if (!isIcon() && getView() != View.LAYOUTSKEL)
			return null;

		// first check to see if there is a schematics link
		for(Iterator it = getCellGroup().getCells(); it.hasNext(); )
		{
			Cell cellInGroup = (Cell)it.next();
			if (cellInGroup.isSchematic()) return cellInGroup;
		}

		// now check to see if there is any layout link
		for(Iterator it = getCellGroup().getCells(); it.hasNext(); )
		{
			Cell cellInGroup = (Cell)it.next();
			if (cellInGroup.getView() == View.LAYOUT) return cellInGroup;
		}

		// finally check to see if there is any "unknown" link
		for(Iterator it = getCellGroup().getCells(); it.hasNext(); )
		{
			Cell cellInGroup = (Cell)it.next();
			if (cellInGroup.getView() == View.UNKNOWN) return cellInGroup;
		}

		// no contents found
		return null;
	}

	/**
	 * Method to find the icon Cell associated with this Cell.
	 * @return the icon Cell associated with this Cell.
	 * Returns null if no such Cell can be found.
	 */
	public Cell iconView()
	{
		// can only get icon view if this is a schematic
		if (!isSchematic()) return null;

		// now look for views
		for(Iterator it = getCellGroup().getCells(); it.hasNext(); )
		{
			Cell cellInGroup = (Cell)it.next();
			if (cellInGroup.isIcon()) return cellInGroup;
		}

		return null;
	}

	/**
	 * Method to find the Cell of a given View that is in the same group as this Cell.
	 * @param view the View of the other Cell.
	 * @return the Cell from this group with the specified View.
	 * Returns null if no such Cell can be found.
	 */
	public Cell otherView(View view)
	{
		// look for views
		for(Iterator it = getCellGroup().getCells(); it.hasNext(); )
		{
			Cell cellInGroup = (Cell)it.next();
			if (cellInGroup.getView() == view) {
                // get latest version
                return cellInGroup.getNewestVersion();
            }
		}

		return null;
	}

	/****************************** NETWORKS ******************************/

	/** Recompute the Netlist structure for this Cell.
     * <p>Because shorting resistors is a fairly common request, it is 
     * implemented in the method if @param shortResistors is set to true.
	 * @return the Netlist structure for this cell.
	 * @throws NetworkTool.NetlistNotReady if called from GUI thread and change Job hasn't prepared Netlist yet
     */
	public Netlist getNetlist(boolean shortResistors) { return NetworkTool.getNetlist(this, shortResistors); }

	/** Returns the Netlist structure for this Cell, using current network options.
	 * Waits for completion of change Job when called from GUI thread
	 * @return the Netlist structure for this cell.
     */
	public Netlist getUserNetlist() { return NetworkTool.getUserNetlist(this); }

	/** Returns the Netlist structure for this Cell, using current network options.
	 * Returns null if change Job hasn't prepared GUI Netlist
	 * @return the Netlist structure for this cell.
     */
	public Netlist acquireUserNetlist() { return NetworkTool.acquireUserNetlist(this); }

	/****************************** DATES ******************************/

	/**
	 * Method to get the creation date of this Cell.
	 * @return the creation date of this Cell.
	 */
	public Date getCreationDate() { return creationDate; }

	/**
	 * Method to set this Cell's creation date.
	 * This is a low-level method and should not be called unless you know what you are doing.
	 * @param creationDate the date of this Cell's creation.
	 */
	public void lowLevelSetCreationDate(Date creationDate) { checkChanging(); this.creationDate = creationDate; }

	/**
	 * Method to return the revision date of this Cell.
	 * @return the revision date of this Cell.
	 */
	public Date getRevisionDate() { return revisionDate; }

	/**
	 * Method to set this Cell's last revision date.
	 * This is a low-level method and should not be called unless you know what you are doing.
	 * @param revisionDate the date of this Cell's last revision.
	 */
	public void lowLevelSetRevisionDate(Date revisionDate) { checkChanging(); this.revisionDate = revisionDate; }

	/**
	 * Method to set this Cell's revision date to the current time.
	 */
	public void madeRevision()
	{
		checkChanging();
		revisionDate = new Date();
	}

	/**
	 * Method to check the current cell to be sure that no subcells have a more recent date.
	 * This is invoked when the "Check cell dates" feature is enabled in the New Nodes tab of
	 * the Edit Options dialog.
	 */
	public void checkCellDates()
	{
		HashSet cellsChecked = new HashSet();
		checkCellDate(getRevisionDate(), cellsChecked);
	}

	/**
	 * Recursive method to check sub-cell revision times.
	 * @param rev_time the revision date of the top-level cell.
	 * Nothing below it can be newer.
	 */
	private void checkCellDate(Date rev_time, HashSet cellsChecked)
	{
		for(Iterator it = getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			if (!(np instanceof Cell)) continue;
			Cell subCell = (Cell)np;

			// ignore recursive references (showing icon in contents)
			if (subCell.isIconOf(this)) continue;
			if (!cellsChecked.contains(subCell))
			{
				subCell.checkCellDate(rev_time, cellsChecked); // recurse
			}

			Cell contentsCell = subCell.contentsView();
			if (contentsCell != null)
			{
				if (!cellsChecked.contains(contentsCell))
				{
					contentsCell.checkCellDate(rev_time, cellsChecked); // recurse
				}
			}
		}

		// check this cell
		cellsChecked.add(this); // flag that we have seen this one
		if (!getRevisionDate().after(rev_time)) return;

		// possible error in hierarchy
		System.out.println("WARNING: sub-cell " + describe() +
			" has been edited since the last revision to the current cell");
	}

	/****************************** MISCELLANEOUS ******************************/

	/**
	 * Method to set this Cell so that instances of it are "expanded" by when created.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 */
	public void setWantExpanded() { checkChanging(); userBits |= WANTNEXPAND; }

	/**
	 * Method to set this Cell so that instances of it are "not expanded" by when created.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 */
	public void clearWantExpanded() { checkChanging(); userBits &= ~WANTNEXPAND; }

	/**
	 * Method to tell if instances of it are "not expanded" by when created.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * @return true if instances of it are "not expanded" by when created.
	 */
	public boolean isWantExpanded() { return (userBits & WANTNEXPAND) != 0; }

	/**
	 * Method to return the function of this Cell.
	 * The Function of CELL is alway UNKNOWN.
	 * @return the function of this Cell.
	 */
	public PrimitiveNode.Function getFunction() { return PrimitiveNode.Function.UNKNOWN; }

	/**
	 * Method to set this Cell so that everything inside of it is locked.
	 * Locked instances cannot be moved or deleted.
	 */
	public void setAllLocked() { checkChanging(); userBits |= NPLOCKED; }

	/**
	 * Method to set this Cell so that everything inside of it is not locked.
	 * Locked instances cannot be moved or deleted.
	 */
	public void clearAllLocked() { checkChanging(); userBits &= ~NPLOCKED; }

	/**
	 * Method to tell if the contents of this Cell are locked.
	 * Locked instances cannot be moved or deleted.
	 * @return true if the contents of this Cell are locked.
	 */
	public boolean isAllLocked() { return (userBits & NPLOCKED) != 0; }

	/**
	 * Method to set this Cell so that all instances inside of it are locked.
	 * Locked instances cannot be moved or deleted.
	 */
	public void setInstancesLocked() { checkChanging(); userBits |= NPILOCKED; }

	/**
	 * Method to set this Cell so that all instances inside of it are not locked.
	 * Locked instances cannot be moved or deleted.
	 */
	public void clearInstancesLocked() { checkChanging(); userBits &= ~NPILOCKED; }

	/**
	 * Method to tell if the sub-instances in this Cell are locked.
	 * Locked instances cannot be moved or deleted.
	 * @return true if the sub-instances in this Cell are locked.
	 */
	public boolean isInstancesLocked() { return (userBits & NPILOCKED) != 0; }

	/**
	 * Method to set this Cell so that it is part of a cell library.
	 * Cell libraries are simply libraries that contain standard cells but no hierarchy
	 * (as opposed to libraries that define a complete circuit).
	 * Certain commands exclude facets from cell libraries, so that the actual circuit hierarchy can be more clearly seen.
	 */
	public void setInCellLibrary() { checkChanging(); userBits |= INCELLLIBRARY; }

	/**
	 * Method to set this Cell so that it is not part of a cell library.
	 * Cell libraries are simply libraries that contain standard cells but no hierarchy
	 * (as opposed to libraries that define a complete circuit).
	 * Certain commands exclude facets from cell libraries, so that the actual circuit hierarchy can be more clearly seen.
	 */
	public void clearInCellLibrary() { checkChanging(); userBits &= ~INCELLLIBRARY; }

	/**
	 * Method to tell if this Cell is part of a cell library.
	 * Cell libraries are simply libraries that contain standard cells but no hierarchy
	 * (as opposed to libraries that define a complete circuit).
	 * Certain commands exclude facets from cell libraries, so that the actual circuit hierarchy can be more clearly seen.
	 * @return true if this Cell is part of a cell library.
	 */
	public boolean isInCellLibrary() { return (userBits & INCELLLIBRARY) != 0; }

	/**
	 * Method to set this Cell so that it is part of a technology library.
	 * Technology libraries are those libraries that contain Cells with
	 * graphical descriptions of the nodes, arcs, and layers of a technology.
	 */
	public void setInTechnologyLibrary() { checkChanging(); userBits |= TECEDITCELL; }

	/**
	 * Method to set this Cell so that it is not part of a technology library.
	 * Technology libraries are those libraries that contain Cells with
	 * graphical descriptions of the nodes, arcs, and layers of a technology.
	 */
	public void clearInTechnologyLibrary() { checkChanging(); userBits &= ~TECEDITCELL; }

	/**
	 * Method to tell if this Cell is part of a Technology Library.
	 * Technology libraries are those libraries that contain Cells with
	 * graphical descriptions of the nodes, arcs, and layers of a technology.
	 * @return true if this Cell is part of a Technology Library.
	 */
	public boolean isInTechnologyLibrary() { return (userBits & TECEDITCELL) != 0; }

    /**
     * Returns true if this Cell is completely linked into database.
	 * This means there is path to this Cell through lists:
	 * Library&#46;libraries->Library&#46;cells-> Cell
     */
	public boolean isActuallyLinked()
	{
		return lib != null && lib.isActuallyLinked() && lib.contains(this);
	}

	/**
	 * Method to check and repair data structure errors in this Cell.
	 */
	public int checkAndRepair(boolean repair, ErrorLogger errorLogger)
	{
		int errorCount = 0;

		for (int i = 0; i < exports.size(); i++)
		{
			Export pp = (Export)exports.get(i);
			if (pp.getPortIndex() != i)
			{
				String msg = this + ", " + pp + " has wrong index";
				System.out.println(msg);
				if (errorLogger != null)
				{
					ErrorLogger.MessageLog error = errorLogger.logError(msg, this, 1);
					error.addExport(pp, true, this, null);
				}
				errorCount++;
			}
		}

		// make sure that every connection is on an arc and a node
		HashMap connections = new HashMap();
		for(Iterator it = getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			errorCount += ai.checkAndRepair(repair, errorLogger);
			ArcInst otherAi = (ArcInst)connections.get(ai.getHead());
			if (otherAi != null)
			{
				String msg = "Cell " + describe() + ", Arc " + ai.describe() +
					": head connection already on other arc " + otherAi.describe();
				System.out.println(msg);
				if (errorLogger != null)
				{
					ErrorLogger.MessageLog error = errorLogger.logError(msg, this, 1);
					error.addGeom(ai, true, this, null);
					error.addGeom(otherAi, true, this, null);
				}
				errorCount++;
			} else
			{
				connections.put(ai.getHead(), ai);
			}

			otherAi = (ArcInst)connections.get(ai.getTail());
			if (otherAi != null)
			{
				String msg = "Cell " + describe() + ", Arc " + ai.describe() +
					": tail connection already on other arc " + otherAi.describe();
				System.out.println(msg);
				if (errorLogger != null)
				{
					ErrorLogger.MessageLog error = errorLogger.logError(msg, this, 1);
					error.addGeom(ai, true, this, null);
					error.addGeom(otherAi, true, this, null);
				}
				errorCount++;
			} else
			{
				connections.put(ai.getTail(), ai);
			}
		}

		// now make sure that all nodes reference them
		for(Iterator it = getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			errorCount += ni.checkAndRepair(repair, errorLogger);
			for(Iterator pIt = ni.getConnections(); pIt.hasNext(); )
			{
				Connection con = (Connection)pIt.next();
				ArcInst ai = (ArcInst)connections.get(con);
				if (ai == null)
				{
					String msg = "Cell " + describe() + ", Node " + ni.describe() +
						": has connection to unknown arc: " + con.getArc().describe() +
						" (node has " + ni.getNumConnections() + " connections)";
					System.out.println(msg);
					if (errorLogger != null)
					{
						ErrorLogger.MessageLog error = errorLogger.logError(msg, this, 1);
						error.addGeom(ni, true, this, null);
					}
					errorCount++;
				} else
				{
					connections.put(con, null);
				}
			}
		}

		// finally check to see if there are any left in the hash table
		for(Iterator it = connections.values().iterator(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			if (ai != null)
			{
				String msg = "Cell " + describe() + ", Arc " + ai.describe() +
					": connection is not on any node";
				System.out.println(msg);
				if (errorLogger != null)
				{
					ErrorLogger.MessageLog error = errorLogger.logError(msg, this, 1);
					error.addGeom(ai, true, this, null);
				}
				errorCount++;
			}
		}

		// check node usages
		for(Iterator it = getUsagesIn(); it.hasNext(); )
		{
			NodeUsage nu = (NodeUsage)it.next();
			errorCount += nu.checkAndRepair(errorLogger);
		}

		// check group pointers
		if (versionGroup == null)
		{
			String msg = "Cell " + describe() + ", Version group is null";
			System.out.println(msg);
			if (errorLogger != null)
				errorLogger.logError(msg, this, 1);
			errorCount++;
		}
		if (cellGroup == null)
		{
			String msg = "Cell " + describe() + ", Cell group is null";
			System.out.println(msg);
			if (errorLogger != null)
				errorLogger.logError(msg, this, 1);
			errorCount++;
		}
		return errorCount;
	}

	/**
	 * Method to tell whether an ElectricObject exists in this Cell.
	 * Used when saving and restoring highlighting to ensure that the object still
	 * exists.
	 * @param eObj the ElectricObject in question
	 * @return true if that ElectricObject is in this Cell.
	 */
	public boolean objInCell(ElectricObject eObj)
	{
		if (eObj instanceof NodeInst)
		{
			for(Iterator it = getNodes(); it.hasNext(); )
				if ((ElectricObject)it.next() == eObj) return true;
		} else if (eObj instanceof ArcInst)
		{
			for(Iterator it = getArcs(); it.hasNext(); )
				if ((ElectricObject)it.next() == eObj) return true;
		} else if (eObj instanceof PortInst)
		{
			NodeInst ni = ((PortInst)eObj).getNodeInst();
			for(Iterator it = getNodes(); it.hasNext(); )
				if ((ElectricObject)it.next() == ni) return true;
		}
		return false;
	}

	/**
	 * Method to set change lock of cells in up-tree of this cell.
	 */
	public void setChangeLock()
	{
// 		if (lock < 0) return;
// 		if (lock > 0)
// 		{
// 			System.out.println("An attemt to set change lock of cell "+describe()+" being examined");
// 			return;
// 		}
// 		lock = -1;
// 		for (Iterator it = getUsagesOf(); it.hasNext(); )
// 		{
// 			NodeUsage nu = (NodeUsage)it.next();
// 			nu.getParent().setChangeLock();
// 		}
// 		if (!isSchematic()) return;
// 		for (Iterator it = cellGroup.getCells(); it.hasNext(); )
// 		{
// 			Cell cell = (Cell)it.next();
// 			if (cell.isIcon()) cell.setChangeLock();
// 		}
	}

	/**
	 * Method to get the 0-based index of this Cell.
	 * @return the index of this Cell.
	 */
	public final int getCellIndex() { return cellIndex; }

	/**
	 * Method to get counter for enumerating cells.
	 * @return counter for enumerating cells. */
	public static int getCellNumber() { return cellNumber; }

	/**
	 * Method to set an arbitrary integer in a temporary location on this Cell.
	 * @param tempInt the integer to be set on this Cell.
	 */
	public void setTempInt(int tempInt) { checkChanging(); this.tempInt = tempInt; }

	/**
	 * Method to get the temporary integer on this Cell.
	 * @return the temporary integer on this Cell.
	 */
	public int getTempInt() { return tempInt; }

	/**
	 * Method to get access to flag bits on this Cell.
	 * Flag bits allow Cells to be marked and examined more conveniently.
	 * However, multiple competing activities may want to mark the nodes at
	 * the same time.  To solve this, each activity that wants to mark nodes
	 * must create a FlagSet that allocates bits in the node.  When done,
	 * the FlagSet must be released.
	 * @param numBits the number of flag bits desired.
	 * @return a FlagSet object that can be used to mark and test the Cell.
	 */
	public static FlagSet getFlagSet(int numBits) { return FlagSet.getFlagSet(flagGenerator, numBits); }

	/**
	 * Method to set the specified flag bits on this Cell.
	 * @param set the flag bits that are to be set on this Cell.
	 */
	public void setBit(FlagSet set) { /*checkChanging();*/ flagBits = flagBits | set.getMask(); }

	/**
	 * Method to set the specified flag bits on this Cell.
	 * @param set the flag bits that are to be cleared on this Cell.
	 */
	public void clearBit(FlagSet set) { /*checkChanging();*/ flagBits = flagBits & set.getUnmask(); }

	/**
	 * Method to test the specified flag bits on this Cell.
	 * @param set the flag bits that are to be tested on this Cell.
	 * @return true if the flag bits are set.
	 */
	public boolean isBit(FlagSet set) { return (flagBits & set.getMask()) != 0; }

	/**
	 * Method to clear change lock of this cell.
	 */
// 	public void clearChangeLock()
// 	{
// 		if (lock >= 0) return;
// 		lock = 0;
// 	}

	/**
	 * Routing to check whether changing of this cell allowed or not.
	 */
// 	public void checkChanging()
// 	{
//         if (Main.NOTHREADING) return;

// 		if (Job.getChangingThread() != Thread.currentThread())
// 		{
// 			if (Job.getChangingThread() == null)
// 				System.out.println(this+" is changing without Undo.startChanges() lock");
// 			else
// 				System.out.println(this+" is changing by another thread "+Job.getChangingThread());
// 			//throw new IllegalStateException("Cell.checkChanging()");
// 		}
// 		Cell rootCell = Job.getChangingCell();
// 		if (lock != -1 && rootCell != null)
// 		{
// 			System.out.println("Change to cell "+rootCell.describe()+" affects cell "+describe()+" which is not above it in the hierarchy");
// 			//throw new IllegalStateException("Cell.checkChanging()");
// 		}
// 	}

	/**
	 * Method to set a Change object on this Cell.
	 * This is used during constraint propagation to tell whether this object has already been changed and by how much.
	 * @param change the Change object to be set on this Cell.
	 */
	public void setChange(Undo.Change change) { checkChanging(); this.change = change; }

	/**
	 * Method to get the Change object on this Cell.
	 * This is used during constraint propagation to tell whether this object has already been changed and by how much.
	 * @return the Change object on this Cell.
	 */
	public Undo.Change getChange() { return change; }

	/**
	 * Method to determine the appropriate Cell associated with this ElectricObject.
	 * @return the appropriate Cell associated with this ElectricObject..
	 * Returns null if no Cell can be found.
	 */
	public Cell whichCell()	{ return this; }

	/**
	 * Method to get the library to which this Cell belongs.
	 * @return to get the library to which this Cell belongs.
	 */
	public Library getLibrary() { return lib; }

	/**
	 * Method to return the Technology of this Cell.
	 * It can be quite complex to determine which Technology a Cell belongs to.
	 * The system examines all of the nodes and arcs in it, and also considers
	 * the Cell's view.
	 * @return return the Technology of this Cell.
	 */
	public Technology getTechnology()
	{
		if (tech == null)
            tech = Technology.whatTechnology(this, null, 0, 0, null, 0, 0);
		return tech;
	}

	/**
	 * Method to set the Technology to which this NodeProto belongs
	 * It can only be called for Cells because PrimitiveNodes have fixed Technology membership.
	 * @param tech the new technology for this NodeProto (Cell).
	 */
	public void setTechnology(Technology tech)
	{
		if (this instanceof Cell)
			this.tech = tech;
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
		return isIcon() ? cellGroup.getMainSchematics() : this;
	}

	/** Sanity check method used by Geometric.checkobj. */
	public boolean containsInstance(Geometric thing)
	{
		if (thing instanceof ArcInst)
		{
			return arcs.contains(thing);
		} else if (thing instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)thing;
			NodeUsage nu = (NodeUsage)usagesIn.get(ni.getProto());
			return (nu != null) && nu.contains(ni);
		} else
		{
			return false;
		}
	}

	/**
	 * Use to compare cells in Cross Library Check
	 * @param obj Object to compare to
	 * @param buffer To store comparison messages in case of failure
	 * @return True if objects represent same NodeInst
	 */
	public boolean compare(Object obj, StringBuffer buffer)
	{
		if (this == obj) return (true);

		// Consider already obj==null
        if (obj == null || getClass() != obj.getClass())
            return (false);

		Cell toCompare = (Cell)obj;

        // Checking if they have same amount of children
        // Better not return here otherwise no valid information is reported
        /*
        if (getNumNodes() != toCompare.getNumNodes() ||
                getNumArcs() != toCompare.getNumArcs() ||
                getNumPorts() != toCompare.getNumPorts() ||
                getNumVariables() != toCompare.getNumVariables())
        {
	        String msg = "";
	        if (getNumNodes() != toCompare.getNumNodes()) msg += "nodes/";
	        if (getNumArcs() != toCompare.getNumArcs()) msg += "arcs/";
	        if (getNumPorts() != toCompare.getNumPorts()) msg += "ports/";
	        if (getNumVariables() != toCompare.getNumVariables()) msg += "variables/";
	        if (buffer != null)
	            buffer.append("Different numbers of " + msg + "\n");
            return (false);
        }
        */

        // Traversing nodes
        // @TODO GVG This should be removed if equals is implemented
        Set noCheckAgain = new HashSet();
        for (Iterator it = getNodes(); it.hasNext(); )
        {
            boolean found = false;
            NodeInst node = (NodeInst)it .next();

            for (Iterator i = toCompare.getNodes(); i.hasNext();)
            {
                NodeInst n = (NodeInst)i .next();

                if (noCheckAgain.contains(n)) continue;

                if (node.compare(n, buffer))
                {
                    found = true;
                    // if node is found, remove elem from iterator
                    // because it was found
                    //@TODO GVG Check iterator functionality
                    // Not sure if it could be done with iterators
                    noCheckAgain.add(n);
                    break;
                }
            }
            // No correspoding NodeInst found
            if (!found)
            {
	            if (buffer != null)
	                buffer.append("No corresponding node '" + node + "' found in '" + toCompare + "'\n");
	            return (false);
            }
        }
        // other node has more instances
        if (getNumNodes() != toCompare.getNumNodes())
        {
            if (buffer != null)
                buffer.append("Cell '" + toCompare.libDescribe() + "' has more nodes than '" + this + "'\n");
            return (false);
        }

        // Traversing Arcs
        for (Iterator it = getArcs(); it.hasNext(); )
        {
            boolean found = false;
            ArcInst arc = (ArcInst)it.next();

            for (Iterator i = toCompare.getArcs(); i.hasNext();)
            {
                ArcInst a = (ArcInst)i.next();

                if (noCheckAgain.contains(a)) continue;

                if (arc.compare(a, buffer))
                {
                    found = true;
                    noCheckAgain.add(a);
                    break;
                }
            }
            // No correspoding ArcInst found
            if (!found)
            {
	            if (buffer != null)
	                buffer.append("No corresponding arc '" + arc + "' found in other cell" + "\n");
	            return (false);
            }
        }
        // other node has more instances
        if (getNumArcs() != toCompare.getNumArcs())
        {
            if (buffer != null)
                buffer.append("Cell '" + toCompare.libDescribe() + "' has more arcs than '" + this + "'\n");
            return (false);
        }

        // Traversing ports. This includes Exports
        noCheckAgain.clear();
        for (Iterator it = getPorts(); it.hasNext(); )
        {
            boolean found = false;
            Export port = (Export)it.next();

            for (Iterator i = toCompare.getPorts(); i.hasNext();)
            {
                Export p = (Export)i.next();

                if (noCheckAgain.contains(p)) continue;

                if (port.compare(p, buffer))
                {
                    found = true;
                    noCheckAgain.add(p);
                    break;
                }
            }
            // No correspoding PortProto found
            if (!found)
            {
                if (buffer != null)
                    buffer.append("No corresponding port '" + port.getName() + "' found in other cell" + "\n");
                return (false);
            }
        }
        // other node has more instances
        if (getNumPorts() != toCompare.getNumPorts())
        {
            if (buffer != null)
                buffer.append("Cell '" + toCompare.libDescribe() + "' has more pors than '" + this + "'\n");
            return (false);
        }

        // Checking attributes
        noCheckAgain.clear();
        for (Iterator it = getVariables(); it.hasNext(); )
        {
            Variable var = (Variable)it.next();
            boolean found = false;

            for (Iterator i = toCompare.getVariables(); i.hasNext();)
            {
                Variable v = (Variable)i.next();

                if (noCheckAgain.contains(v)) continue;

                if (var.compare(v, buffer))
                {
                    found = true;
                    noCheckAgain.add(v);
                    break;
                }
            }
            // No correspoding Variable found
            if (!found)
            {
                if (buffer != null)
                    buffer.append("No corresponding variable '" + var + "' found in other cell" + "\n");
                return (false);
            }
        }
        // other node has more instances
        if (getNumVariables() != toCompare.getNumVariables())
        {
            if (buffer != null)
                buffer.append("Cell '" + toCompare + "' has more variables than '" + this + "'\n");
            return (false);
        }
        return (true);
	}

    /**
     * Compares revision dates of Cells.
     * @param obj
     * @return
     */
	public int compareTo(Object obj)
	{
		if (equals(obj)) return 0;
        if (!(obj instanceof Cell)) return (-1);

		Cell toCompare = (Cell)obj;
        Date toCompareDate = toCompare.getRevisionDate();
        return (toCompareDate.compareTo(getRevisionDate()));
	}

	/**
	 * Method to get MinZ and MaxZ of the cell calculated based on nodes.
	 * You must guarantee minZ = Double.MaxValue() and maxZ = Double.MinValue()
	 * for initial call.
	 * @param array array[0] is minZ and array[1] is max
	 */
	public void getZValues(double [] array)
	{
		for (int i = 0; i < nodes.size(); i++)
		{
			NodeInst ni = (NodeInst) nodes.get(i);
			NodeProto nProto = ni.getProto();
			if (nProto instanceof Cell)
			{
				Cell nCell = (Cell)nProto;
				nCell.getZValues(array);
			}
			else
			{
				PrimitiveNode np = (PrimitiveNode)nProto;
				np.getZValues(array);
			}
		}
	}

	public boolean findReferenceInCell(Library elib, Set set)
	{
		// Stop recursive search here

		if (lib == elib)
		{
			//set.add(this);
			return (true);
		}
		int initial = set.size();

		for (int i = 0; i < nodes.size(); i++)
		{
			NodeInst ni = (NodeInst) nodes.get(i);
			NodeProto nProto = ni.getProto();
			if (nProto instanceof Cell)
			{
				Cell nCell = (Cell)nProto;
				if (nCell.getLibrary() == elib)
					set.add(this);
				else
					nCell.findReferenceInCell(elib, set);
			}
		}
		return (set.size() != initial);
	}
}
