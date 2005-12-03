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

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.CellUsage;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableCell;
import com.sun.electric.database.ImmutableElectricObject;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.NodeProtoId;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.ElectricObject;
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
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.ui.TextWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

/**
 * A Cell is a non-primitive NodeProto.
 * Besides the information that it inherits from NodeProto, the Cell holds a
 * set of nodes, arcs, and networks.
 * The exported ports on NodeInsts inside of this cell become the Exports
 * of this Cell.
 * A Cell also has a specific view and version number.
 * <P>
 * It is possible to get all of the versions of the cell.
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
public class Cell extends ElectricObject implements NodeProto, Comparable<Cell>
{
	// ------------------------- private classes -----------------------------

	/**
	 * A CellGroup contains a list of cells that are related.
	 * This includes different Views of a cell (e.g. the schematic, layout, and icon Views),
	 * alternative icons, all the parts of a multi-part icon.
	 */
	public static class CellGroup
	{
		// private data
		private TreeSet<Cell> cells = new TreeSet<Cell>();
		private Cell mainSchematic;
		private String groupName = null;

		// ------------------------- public methods -----------------------------

		/**
		 * Constructs a CellGroup.
		 */
		private CellGroup()
		{
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
			cell.cellGroup = this;
			if (mainSchematic != null)
				mainSchematic = mainSchematic.getNewestVersion();
			groupName = null;
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
				if (f == mainSchematic)
					mainSchematic = null;
			}
			groupName = null;
		}

		/**
		 * Method to return an Iterator over all the Cells that are in this CellGroup.
		 * @return an Iterator over all the Cells that are in this CellGroup.
		 */
		public Iterator<Cell> getCells() { return cells.iterator(); }

		/**
		 * Method to return the number of Cells that are in this CellGroup.
		 * @return the number of Cells that are in this CellGroup.
		 */
		public int getNumCells() { return cells.size(); }

		/**
		 * Method to return a List of all cells in this Group, sorted by View.
		 * @return a List of all cells in this Group, sorted by View.
		 */
		public List<Cell> getCellsSortedByView()
		{
			synchronized(cells)
			{
				List<Cell> sortedList = new ArrayList<Cell>(cells);
				Collections.sort(sortedList, new TextUtils.CellsByView());
				return sortedList;
			}
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
			for (Iterator<Cell> it = getCells(); it.hasNext();)
			{
				Cell c = (Cell) it.next();
				if (c.isSchematic())
				{
                    // it is the latest version
					mainSchematic = c;
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
			for(Iterator<Cell> it = getCells(); it.hasNext(); )
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
			if (onlyCell != null) return groupName = onlyCell.describe(false);

			// name the group according to all of the different base names
			Set<String> groupNames = new TreeSet<String>();
			int widestName = 0;
			for(Iterator<Cell> it = getCells(); it.hasNext(); )
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
				for(Iterator<String> it = groupNames.iterator(); it.hasNext(); )
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
			for(Iterator<String> it = groupNames.iterator(); it.hasNext(); )
			{
				String oneName = (String)it.next();
				if (groupName == null) groupName = oneName; else
					groupName += "," + oneName;
			}
			return groupName;
		}

		/**
		 * Method to check invariants in this CellGroup.
		 * @exception AssertionError if invariants are not valid
		 */
		void check()
		{
			Library lib = null;
			for (Iterator<Cell> it = cells.iterator(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				if (lib == null) lib = cell.lib;
				assert lib.contains(cell);
				assert cell.cellGroup == this;
			}
			assert lib != null;
			if (mainSchematic != null)
			{
				assert containsCell(mainSchematic);
				assert mainSchematic.getNewestVersion() == mainSchematic;
			}
		}
	}

	private class MaxSuffix { int v = 0; }

	// -------------------------- private data ---------------------------------

	/** Variable key for characteristic spacing for a cell. */		public static final Variable.Key CHARACTERISTIC_SPACING = Variable.newKey("FACET_characteristic_spacing");
	/** Variable key for text cell contents. */						public static final Variable.Key CELL_TEXT_KEY = Variable.newKey("FACET_message");
	/** Variable key for number of multipage pages. */				public static final Variable.Key MULTIPAGE_COUNT_KEY = Variable.newKey("CELL_page_count");
	/** Variable key for font of text in textual cells. */			public static final Variable.Key TEXT_CELL_FONT_NAME = Variable.newKey("CELL_text_font");
	/** Variable key for size of text in textual cells. */			public static final Variable.Key TEXT_CELL_FONT_SIZE = Variable.newKey("CELL_text_size");

    private static final int[] NULL_INT_ARRAY = {};
	private static final Export[] NULL_EXPORT_ARRAY = {};

	/** set if instances should be expanded */						private static final int WANTNEXPAND   =           02;
//	/** set if cell is modified */						            private static final int MODIFIED      =     01000000;
	/** set if everything in cell is locked */						private static final int NPLOCKED      =     04000000;
	/** set if instances in cell are locked */						private static final int NPILOCKED     =    010000000;
	/** set if cell is part of a "cell library" */					private static final int INCELLLIBRARY =    020000000;
	/** set if cell is from a technology-library */					private static final int TECEDITCELL   =    040000000;
	/** set if cell is a multi-page schematic */					private static final int MULTIPAGE     = 017600000000;

	/** Length of base name for autonaming. */						private static final int ABBREVLEN = 8;
	/** zero rectangle */											private static final Rectangle2D CENTERRECT = new Rectangle2D.Double(0, 0, 0, 0);

    /** Bounds are correct */                                       private static final byte BOUNDS_CORRECT = 0;
    /** Bounds are correct if all subcells have correct bounds. */  private static final byte BOUNDS_CORRECT_SUB = 1;
    /** Bounds need to be recomputed. */                            private static final byte BOUNDS_RECOMPUTE = 2;
    
	/** static list of all linked cells indexed by CellId. */		private static final ArrayList<Cell> linkedCells = new ArrayList<Cell>();

    /** Persistent data of this Cell. */                            private ImmutableCell d;
	/** The CellName of the Cell. */								private CellName cellName;
	/** The CellGroup this Cell belongs to. */						private CellGroup cellGroup;
	/** The library this Cell belongs to. */						private Library lib;
	/** The date this Cell was created. */							private Date creationDate = new Date();
	/** The date this Cell was last modified. */					private Date revisionDate = new Date();
	/** Internal flag bits. */										private int userBits = 0;
	/** The basename for autonaming of instances of this Cell */	private Name basename;
    /** An array of Exports on the Cell by chronological index. */  private Export[] chronExports = new Export[2];
	/** A sorted array of Exports on the Cell. */					private Export[] exports = NULL_EXPORT_ARRAY;
	/** The Cell's essential-bounds. */								private final ArrayList<NodeInst> essenBounds = new ArrayList<NodeInst>();
    /** Chronological list of NodeInsts in this Cell. */            private final ArrayList<NodeInst> chronNodes = new ArrayList<NodeInst>();
	/** A list of NodeInsts in this Cell. */						private final ArrayList<NodeInst> nodes = new ArrayList<NodeInst>();
    /** Counts of NodeInsts for each CellUsage. */                  private int[] cellUsages = NULL_INT_ARRAY;
	/** A map from canonic String to Integer maximal numeric suffix */private final HashMap<String,MaxSuffix> maxSuffix = new HashMap<String,MaxSuffix>();
    /** A maximal suffix of temporary arc name. */                  private int maxArcSuffix = -1;
    /** Chronological list of ArcInst in this Cell. */              private final ArrayList<ArcInst> chronArcs = new ArrayList<ArcInst>();
    /** A list of ArcInsts in this Cell. */							private final ArrayList<ArcInst> arcs = new ArrayList<ArcInst>();
	/** A map from temporary canonicString to NodeInst. */			private final HashMap<String,NodeInst> tempNodeNames = new HashMap<String,NodeInst>();
	/** The bounds of the Cell. */									private final Rectangle2D cellBounds = new Rectangle2D.Double();
	/** Whether the bounds need to be recomputed.
     * BOUNDS_CORRECT - bounds are correct.
     * BOUNDS_CORRECT_SUB - bounds are correct prvided that bounds of subcells are correct.
     * BOUNDS_RECOMPUTE - bounds need to be recomputed. */          private byte boundsDirty;
	/** The geometric data structure. */							private RTNode rTree = RTNode.makeTopLevel();
	/** This Cell's Technology. */									private Technology tech;
	/** The temporary integer value. */								private int tempInt;
    /** Set if Cell is modified (major or minor). */                private int modified;
    /** Set if expanded status of subcell instances is modified. */ private boolean expandStatusModified;
    /** Set if contents (nodes, arc, exports) were modified in this change job. */ private boolean contentsModified;


	// ------------------ protected and private methods -----------------------

	/**
	 * This constructor should not be called.
	 * Use the factory "newInstance" to create a Cell.
	 */
	private Cell(ImmutableCell d, Library lib)
	{
        this.d = d;
        this.lib = lib;
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
		if (!isLinked())
		{
			System.out.println("Cell already killed");
			return;
		}
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
		HashMap<NodeInst,NodeProto> nodePrototypes = new HashMap<NodeInst,NodeProto>();
//		for(Iterator<NodeInst> it = fromCell.getNodes(); it.hasNext(); )
//		{
//			NodeInst ni = (NodeInst)it.next();
//			nodePrototypes.put(ni, ni.getProto());
//		}

		// if doing a cross-library copy and can use existing ones from new library, do it
		if (destLib != null)
		{
			// scan all subcells to see if they are found in the new library
			for(Iterator<NodeInst> it = fromCell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (ni.getProto() instanceof PrimitiveNode) continue;
				Cell niProto = (Cell)ni.getProto();

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
				for(Iterator<Cell> cIt = toLib.getCells(); cIt.hasNext(); )
				{
					lnt = (Cell)cIt.next();
					if (lnt.getName().equalsIgnoreCase(niProto.getName()) &&
						lnt.getView() == niProto.getView()) break;
					lnt = null;
				}
				if (lnt == null) continue;

				// make sure all used ports can be found on the uncopied cell
				boolean validPorts = true;
				for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
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
						System.out.println("Cannot use subcell " + lnt.noLibDescribe() + " in " + destLib +
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
		return copyNodeProtoUsingMapping(fromCell, toLib, toName, nodePrototypes);
	}

	/**
	 * Method to copy a Cell to any Library, using a preset mapping of node prototypes.
	 * @param fromCell the Cell to copy.
	 * @param toLib the Library to copy it to.
	 * If the destination library is the same as the original Cell's library, a new version is made.
	 * @param toName the name of the Cell in the destination Library.
	 * @param nodePrototypes a HashMap from NodeInsts in the source Cell to proper NodeProtos to use in the new Cell.
	 * @return the new Cell in the destination Library.
	 */
	public static Cell copyNodeProtoUsingMapping(Cell fromCell, Library toLib, String toName,
		HashMap<NodeInst,NodeProto> nodePrototypes)
	{
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
		HashMap<NodeInst,NodeInst> newNodes = new HashMap<NodeInst,NodeInst>();
		for(Iterator<NodeInst> it = fromCell.getNodes(); it.hasNext(); )
		{
			// create the new nodeinst
			NodeInst ni = (NodeInst)it.next();
			NodeProto lnt = (NodeProto)nodePrototypes.get(ni);
			if (lnt == null) lnt = ni.getProto();
			double scaleX = ni.getXSize();   //if (ni.isXMirrored()) scaleX = -scaleX;
			double scaleY = ni.getYSize();   //if (ni.isYMirrored()) scaleY = -scaleY;
			NodeInst toNi = NodeInst.newInstance(lnt, new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY()),
				scaleX, scaleY, newCell, ni.getOrient(), ni.getName(), 0);
			if (toNi == null) return null;

			// save the new nodeinst address in the old nodeinst
			newNodes.put(ni, toNi);

			// copy miscellaneous information
			toNi.copyTextDescriptorFrom(ni, NodeInst.NODE_PROTO);
			toNi.copyTextDescriptorFrom(ni, NodeInst.NODE_NAME);
			toNi.copyStateBits(ni);
		}

		// now copy the variables on the nodes
		for(Iterator<NodeInst> it = fromCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeInst toNi = (NodeInst)newNodes.get(ni);
			toNi.copyVarsFrom(ni);

            // if this is an icon, and this nodeinst is the box with the name of the cell on it,
            // then change the name from the old to the new
            if (newCell.isIcon()) {
                Variable var = toNi.getVar(Schematics.SCHEM_FUNCTION, String.class);
                if (var != null) {
                    String name = (String)var.getObject();
                    if (name.equals(fromCell.getName())) {
                        toNi.updateVar(var.getKey(), newCell.getName());
                    }
                }
            }
		}

		// copy arcs
		for(Iterator<ArcInst> it = fromCell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();

			// find the nodeinst and portinst connections for this arcinst
			PortInst [] opi = new PortInst[2];
			for(int i=0; i<2; i++)
			{
				opi[i] = null;
				NodeInst ono = (NodeInst)newNodes.get(ai.getPortInst(i).getNodeInst());
				PortProto pp = ai.getPortInst(i).getPortProto();
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
					System.out.println("Error: no port for " + ai.getProto() +
						" arc on " + ono.getProto());
			}
			if (opi[0] == null || opi[1] == null) return null;

			// create the arcinst
			ArcInst toAi = ArcInst.newInstance(ai.getProto(), ai.getWidth(), opi[ArcInst.HEADEND], opi[ArcInst.TAILEND],
				ai.getHeadLocation(), ai.getTailLocation(), ai.getName(), ai.getAngle());
			if (toAi == null) return null;

			// copy arcinst information
			toAi.copyPropertiesFrom(ai);
		}

		// copy the Exports
		for(Iterator<Export> it = fromCell.getExports(); it.hasNext(); )
		{
			Export pp = (Export)it.next();

			// match sub-portproto in old nodeinst to sub-portproto in new one
			NodeInst ni = (NodeInst)newNodes.get(pp.getOriginalPort().getNodeInst());
			PortInst pi = ni.findPortInst(pp.getOriginalPort().getPortProto().getName());
			if (pi == null)
			{
				System.out.println("Error: no port on " + pp.getOriginalPort().getNodeInst().getProto());
				return null;
			}

			// create the nodeinst portinst
			Export ppt = Export.newInstance(newCell, pi, pp.getName());
			if (ppt == null) return null;

			// copy portproto variables
			ppt.copyVarsFrom(pp);

			// copy miscellaneous information
			ppt.copyStateBits(pp);
//			ppt.lowLevelSetUserbits(pp.lowLevelGetUserbits());
			ppt.copyTextDescriptorFrom(pp, Export.EXPORT_NAME);
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
		rename(CellName.parseName(newName + ";" + getVersion() + "{" + getView().getAbbreviation() + "}"));
	}

	/**
	 * Method to rename this Cell.
	 * @param cellName the new name of this cell.
	 */
	private void rename(CellName cellName)
	{
		checkChanging();
		assert isLinked();
		if (cellName == null) return;
		if (cellName.equals(this.cellName)) return;

		// do the rename
		CellName oldCellName = this.cellName;
		lowLevelRename(cellName);

		// handle change control, constraint, and broadcast
		Undo.renameObject(this, oldCellName);
	}

	/****************************** LOW-LEVEL IMPLEMENTATION ******************************/

	/**
	 * Low-level access method to create a cell in library "lib".
	 * Unless you know what you are doing, do not use this method.
	 * @param lib library in which to place this cell.
	 * @return the newly created cell.
	 */
	public static Cell lowLevelAllocate(Library lib)
	{
		Job.checkChanging();
		Cell c = new Cell(ImmutableCell.newInstance(new CellId()), lib);
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
		assert !isLinked();
		CellName n = CellName.parseName(name);
		if (n == null) return true;

		// check name for legal characters
		String cellName = n.getName();
		String original = null;
		for(int i=0; i<cellName.length(); i++)
		{
			char chr = cellName.charAt(i);
			if (Character.isWhitespace(chr) || chr == ':' || chr == ';' || chr == '{' || chr == '}')
			{
				if (original == null) original = cellName;
				cellName = cellName.substring(0, i) + '_' + cellName.substring(i+1);
			}
		}
		if (original != null)
		{
			System.out.println("Cell name changed from '" + original + "' to '" + cellName + "'");
			n = CellName.newName(cellName, n.getView(), n.getVersion());
		}
		
		setCellName(n);
		return false;
	}

	/**
	 * Low-level access method to link this Cell into its library.
	 * @return true on error.
	 */
	public boolean lowLevelLink()
	{
		lib.checkChanging();
		assert !isLinked();
		if (cellName == null)
		{
			System.out.println(this+" has bad name");
			return true;
		}

		// add ourselves to the library
		lowLevelLinkCellName();

		// success
        CellId cellId = getD().cellId;
        while (linkedCells.size() <= cellId.cellIndex) linkedCells.add(null);
        linkedCells.set(cellId.cellIndex, this);
		checkInvariants();
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

		// remove from the library and from cell group
		lib.removeCell(this);
		cellGroup.remove(this);

        linkedCells.set(d.cellId.cellIndex, null);
	}

	/**
	 * Low-level access method to rename a Cell.
	 * Unless you know what you are doing, do not use this method...use "rename()" instead.
	 * @param newCellName the new cell name of this cell.
	 */
	public void lowLevelRename(CellName newCellName)
	{
		assert isLinked();
		if (newCellName.equals(cellName)) return;

		// remove temporarily from the library and from cell group
		lib.removeCell(this);
		cellGroup.remove(this);

		setCellName(newCellName);

		lowLevelLinkCellName();
		checkInvariants();
	}

	private void lowLevelLinkCellName()
	{
		// ensure unique cell name
		String protoName = getName();
		View view = getView();
		int version = getVersion();
		int greatestVersion = 0;
		boolean conflict = version <= 0;
		for (Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell c = (Cell)it.next();
			if (c.getName().equalsIgnoreCase(protoName) && c.getView() == view)
			{
				if (c.getVersion() == getVersion()) conflict = true;
				if (c.getVersion() > greatestVersion)
					greatestVersion = c.getVersion();
			}
		}
		if (conflict)
		{
			if (getVersion() > 0)
				System.out.println("Already have cell " + getCellName() + " with version " + getVersion() + ", generating a new version");
			CellName cn = CellName.newName(getName(), getView(), greatestVersion + 1);
			setCellName(cn);
		}

		// determine the cell group
		for (Iterator<Cell> it = getViewsTail(); it.hasNext(); )
		{
			Cell c = (Cell)it.next();
			if (c.getName().equals(getName()))
				cellGroup = c.cellGroup;
		}
		// still none: make a new one
		if (cellGroup == null) cellGroup = new CellGroup();

		// add ourselves to the library and to cell group
		lib.addCell(this);
		cellGroup.add(this);

	}

	/**
	 * Method to change CellName of this Cell.
	 * @param cellName new cell name.
	 */
	private void setCellName(CellName cellName)
	{
		this.cellName = cellName;

		// prepare basename for autonaming
		String protoName = cellName.getName();
		basename = Name.findName(protoName.substring(0,Math.min(ABBREVLEN,protoName.length()))+"@0").getBasename();
		if (basename == null)
			basename = PrimitiveNode.Function.UNKNOWN.getBasename();
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
	public void lowLevelSetUserbits(int userBits) { checkChanging(); this.userBits = userBits; Undo.otherChange(this); }

	/*
	 * Low-level method to backup this Cell to CellBackup.
     * @return CellBackup which is the backup of this Cell.
	 */
    public CellBackup backup(CellBackup oldBackup) {
        ImmutableNodeInst[] oldN = ImmutableNodeInst.NULL_ARRAY;
        ImmutableArcInst[] oldA = ImmutableArcInst.NULL_ARRAY;
        ImmutableExport[] oldE = ImmutableExport.NULL_ARRAY;
        if (oldBackup != null) {
            oldN = oldBackup.nodes;
            oldA = oldBackup.arcs;
            oldE = oldBackup.exports;
        }
        ImmutableNodeInst[] n = backupNodes(oldN);
        ImmutableArcInst[] a = backupArcs(oldA);
        ImmutableExport[] e = backupExports(oldE);
        if (oldBackup != null && d == oldBackup.d &&
                cellName == oldBackup.cellName &&
                cellGroup == oldBackup.cellGroup &&
                lib.getId() == oldBackup.libId &&
                creationDate.getTime() == oldBackup.creationDate &&
                revisionDate.getTime() == oldBackup.revisionDate &&
                tech == oldBackup.tech &&
                userBits == oldBackup.userBits &&
                n == oldBackup.nodes &&
                a == oldBackup.arcs &&
                e == oldBackup.exports)
            return oldBackup;
        return new CellBackup(d, cellName, cellGroup, lib.getId(), creationDate.getTime(), revisionDate.getTime(),
                tech, userBits, n, a, e, cellUsages);
    }

    private ImmutableNodeInst[] backupNodes(ImmutableNodeInst[] oldNodes) {
        int numNodes = Math.min(oldNodes.length, nodes.size());
        int matchedNodes = 0;
        while (matchedNodes < numNodes && oldNodes[matchedNodes] == nodes.get(matchedNodes).getD())
            matchedNodes++;
        if (matchedNodes == oldNodes.length && matchedNodes == nodes.size()) return oldNodes;
        ImmutableNodeInst[] newNodes = new ImmutableNodeInst[nodes.size()];
        System.arraycopy(oldNodes, 0, newNodes, 0, matchedNodes);
        for (int i = matchedNodes; i < nodes.size(); i++)
            newNodes[i] = nodes.get(i).getD();
        return newNodes;
    }
    
    private ImmutableArcInst[] backupArcs(ImmutableArcInst[] oldArcs) {
        int numArcs = Math.min(oldArcs.length, arcs.size());
        int matchedArcs = 0;
        while (matchedArcs < numArcs && oldArcs[matchedArcs] == arcs.get(matchedArcs).getD())
            matchedArcs++;
        if (matchedArcs == oldArcs.length && matchedArcs == arcs.size()) return oldArcs;
        ImmutableArcInst[] newArcs = new ImmutableArcInst[arcs.size()];
        System.arraycopy(oldArcs, 0, newArcs, 0, matchedArcs);
        for (int i = matchedArcs; i < arcs.size(); i++)
            newArcs[i] = arcs.get(i).getD();
        return newArcs;
    }
    
    private ImmutableExport[] backupExports(ImmutableExport[] oldExports) {
        int numExports = Math.min(oldExports.length, exports.length);
        int matchedExports = 0;
        while (matchedExports < numExports && oldExports[matchedExports] == exports[matchedExports].getD())
            matchedExports++;
        if (matchedExports == oldExports.length && matchedExports == exports.length) return oldExports;
        ImmutableExport[] newExports = new ImmutableExport[exports.length];
        System.arraycopy(oldExports, 0, newExports, 0, matchedExports);
        for (int i = matchedExports; i < exports.length; i++)
            newExports[i] = exports[i].getD();
        return newExports;
    }
    
	/*
	 * Low-level method to check consistency of backup of this Cell.
     * @param backup backup of this Cell.
     * @return true if backup is consistence with this Cell.
	 */
    public boolean checkBackup(CellBackup backup) {
        return backup(backup) == backup;
    }
    
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
	public void setDirty() { setDirty(BOUNDS_RECOMPUTE); }
    
	/**
	 * Method to indicate that the bounds of this Cell are incorrect because
	 * a node or arc has been created, deleted, or modified.
	 */
	private void setDirty(byte boundsLevel)
	{
        if (boundsDirty == BOUNDS_CORRECT) {
            boundsDirty = boundsLevel;
            for (Iterator<CellUsage> it = getUsagesOf(); it.hasNext(); ) {
                CellUsage u = (CellUsage)it.next();
                u.getParent().setDirty(BOUNDS_CORRECT_SUB);
            }
        } else if (boundsDirty < boundsLevel)
            boundsDirty = boundsLevel;
	}

	/**
	 * Method to return an interator over all Geometric objects in a given area of this Cell.
	 * @param bounds the specified area to search.
	 * @return an iterator over all of the Geometric objects in that area.
	 */
	public Iterator<Geometric> searchIterator(Rectangle2D bounds) { return new RTNode.Search(bounds, this); }

	/**
	 * Method to return the bounds of this Cell.
	 * @return a Rectangle2D with the bounds of this cell's contents
	 */
	public Rectangle2D getBounds()
	{
        // Don't recalculate in GUI thread.
        if (boundsDirty == BOUNDS_CORRECT ||
            Thread.currentThread() != Job.databaseChangesThread && !Job.NOTHREADING)
            return cellBounds;
        
        // Current bounds are correct if subcell bounds are the same
        if (boundsDirty == BOUNDS_CORRECT_SUB) {
            boundsDirty = BOUNDS_CORRECT;
            for (Iterator<CellUsage> it = getUsagesIn(); it.hasNext(); ) {
                CellUsage u = (CellUsage)it.next();
                u.getProto().getBounds();
            }
            // boundsDirty could be changes by subcell's getBounds.
            if (boundsDirty == BOUNDS_CORRECT)
                return cellBounds;
        }

        // recompute bounds
        double cellLowX, cellHighX, cellLowY, cellHighY;
        boolean boundsEmpty = true;
        cellLowX = cellHighX = cellLowY = cellHighY = 0;
        
        for(int i = 0; i < nodes.size(); i++ ) {
            NodeInst ni = (NodeInst) nodes.get(i);
            NodeProto np = ni.getProto();
            
            // special case: do not include "cell center" primitives from Generic
            if (np == Generic.tech.cellCenterNode) continue;
            
            // special case for invisible pins: do not include if inheritable or interior-only
            if (np == Generic.tech.invisiblePinNode) {
                boolean found = false;
                for(Iterator<Variable> it = ni.getVariables(); it.hasNext(); ) {
                    Variable var = (Variable)it.next();
                    if (var.isDisplay()) {
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
            if (boundsEmpty) {
                boundsEmpty = false;
                cellLowX = lowx;   cellHighX = highx;
                cellLowY = lowy;   cellHighY = highy;
            } else {
                if (lowx < cellLowX) cellLowX = lowx;
                if (highx > cellHighX) cellHighX = highx;
                if (lowy < cellLowY) cellLowY = lowy;
                if (highy > cellHighY) cellHighY = highy;
            }
        }
        for(int i = 0; i < arcs.size(); i++ ) {
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
        cellLowX = DBMath.round(cellLowX);
        cellLowY = DBMath.round(cellLowY);
        double width = DBMath.round(cellHighX - cellLowX);
        double height = DBMath.round(cellHighY - cellLowY);
        if (cellLowX != cellBounds.getMinX() || cellLowY != cellBounds.getMinY() ||
                width != cellBounds.getWidth() || height != cellBounds.getHeight()) {
//            System.out.print("Bounds " + this + " changed from " + cellBounds);
            cellBounds.setRect(cellLowX, cellLowY, width, height);
//            System.out.println(" to " + cellBounds);
            for (Iterator<NodeInst> it = getInstancesOf(); it.hasNext(); ) {
                NodeInst ni = (NodeInst)it.next();
                // Fake modify to recalcualte bounds and RTTree
				ni.lowLevelModify(ni.getD());
            }
        }
        boundsDirty = BOUNDS_CORRECT;
		return cellBounds;
	}

	/**
	 * Method to R-Tree of this Cell.
	 * The R-Tree organizes all of the Geometric objects spatially for quick search.
	 * @return R-Tree of this Cell.
	 */
	RTNode getRTree() { return rTree; }

	/**
	 * Method to set the R-Tree of this Cell.
	 * @param rTree the head of the new R-Tree for this Cell.
	 */
	void setRTree(RTNode rTree) { checkChanging(); this.rTree = rTree; }

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
	 * @param cX coordinate X of new center.
     * @param cY coordinate Y of new center.
	 */
	public void adjustReferencePoint(double cX, double cY)
	{
		checkChanging();

		// if there is no change, stop now
		if (cX == 0 && cY == 0) return;

		// move reference point by (dx,dy)
//		referencePointNode.modifyInstance(-cX, -cY, 0, 0, 0);

		// must adjust all nodes by (dx,dy)
		for(Iterator<NodeInst> it = getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() == Generic.tech.cellCenterNode) continue;

			// move NodeInst "ni" by (dx,dy)
			ni.move(-cX, -cY);
		}
		for(Iterator<ArcInst> it = getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();

			// move NodeInst "ni" by (dx,dy)
			ai.modify(0, -cX, -cY, -cX, -cY);
		}

		// adjust all instances of this cell
		for(Iterator<NodeInst> it = getInstancesOf(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			Undo.redrawObject(ni);
            AffineTransform trans = ni.getOrient().pureRotate();
//			AffineTransform trans = NodeInst.pureRotate(ni.getAngle(), ni.isMirroredAboutXAxis(), ni.isMirroredAboutYAxis());
			Point2D in = new Point2D.Double(cX, cY);
			trans.transform(in, in);
			ni.move(in.getX(), in.getY());
		}

		// adjust all windows showing this cell
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow_)) continue;
			Cell cell = content.getCell();
			if (cell != this) continue;
			EditWindow_ wnd = (EditWindow_)content;
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
		for(Iterator<NodeInst> it = getNodes(); it.hasNext(); )
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
		public static final double MULTIPAGESEPARATION = 1000;

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
		private List<Point2D> lineFromEnd;
		private List<Point2D> lineToEnd;
		private List<Point2D> textPoint;
		private List<Double> textSize;
		private List<Point2D> textBox;
		private List<String> textMessage;
		private int pageNo;

		/**
		 * Constructor for cell frame descriptions.
		 * @param cell the Cell that is having a frame drawn.
		 */
		public FrameDescription(Cell cell, int pageNo)
		{
			this.cell = cell;
			this.pageNo = pageNo;
			lineFromEnd = new ArrayList<Point2D>();
			lineToEnd = new ArrayList<Point2D>();
			textPoint = new ArrayList<Point2D>();
			textSize = new ArrayList<Double>();
			textBox = new ArrayList<Point2D>();
			textMessage = new ArrayList<String>();
			loadFrame();
		}

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
			double offY = 0;
			if (cell.isMultiPage())
			{
				offY = pageNo * MULTIPAGESEPARATION;
			}
			for(int i=0; i<lineFromEnd.size(); i++)
			{
				Point2D from = (Point2D)lineFromEnd.get(i);
				Point2D to = (Point2D)lineToEnd.get(i);
				if (offY != 0)
				{
					from = new Point2D.Double(from.getX(), from.getY() + offY);
					to = new Point2D.Double(to.getX(), to.getY() + offY);
				}
				showFrameLine(from, to);
			}
			for(int i=0; i<textPoint.size(); i++)
			{
				Point2D at = (Point2D)textPoint.get(i);
				if (offY != 0)
					at = new Point2D.Double(at.getX(), at.getY() + offY);
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

				point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox, -schYSize/2 + frameWid + yLogoBox*8/15);
				point1 = new Point2D.Double(schXSize/2 - frameWid,            -schYSize/2 + frameWid + yLogoBox*8/15);
				addLine(point0, point1);

				point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox, -schYSize/2 + frameWid + yLogoBox*10/15);
				point1 = new Point2D.Double(schXSize/2 - frameWid,            -schYSize/2 + frameWid + yLogoBox*10/15);
				addLine(point0, point1);

				point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox, -schYSize/2 + frameWid + yLogoBox*12/15);
				point1 = new Point2D.Double(schXSize/2 - frameWid,            -schYSize/2 + frameWid + yLogoBox*12/15);
				addLine(point0, point1);

				point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox/2, -schYSize/2 + frameWid + yLogoBox*13.5/15);
				addText(point0, yLogoBox*2/15, xLogoBox, yLogoBox*3/15, "Cell: " + cell.describe(false) + (cell.isMultiPage() ? " Page " + (pageNo+1) : ""));

				String projectName = User.getFrameProjectName();
				Variable pVar = cell.getLibrary().getVar(User.FRAME_PROJECT_NAME, String.class);
				if (pVar != null) projectName = (String)pVar.getObject();
				if (projectName.length() > 0)
				{
					point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox/2, -schYSize/2 + frameWid + yLogoBox*11/15);
					addText(point0, yLogoBox*2/15, xLogoBox, yLogoBox*2/15, "Project: " + projectName);
				}

				String designerName = User.getFrameDesignerName();
				Variable dVar = cell.getLibrary().getVar(User.FRAME_DESIGNER_NAME, String.class);
				if (dVar != null) designerName = (String)dVar.getObject();
				dVar = cell.getVar(User.FRAME_DESIGNER_NAME, String.class);
				if (dVar != null) designerName = (String)dVar.getObject();
				if (designerName.length() > 0)
				{
					point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox/2, -schYSize/2 + frameWid + yLogoBox*9/15);
					addText(point0, yLogoBox*2/15, xLogoBox, yLogoBox*2/15, "Designer: " + designerName);
				}

				Variable lVar = cell.getVar(User.FRAME_LAST_CHANGED_BY, String.class);
				if (lVar != null)
				{
					String lastChangeByName = (String)lVar.getObject();
					point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox/2, -schYSize/2 + frameWid + yLogoBox*7/15);
					addText(point0, yLogoBox*2/15, xLogoBox, yLogoBox*2/15, "Last Changed By: " + lastChangeByName);
				}

				String companyName = User.getFrameCompanyName();
				Variable cVar = cell.getLibrary().getVar(User.FRAME_COMPANY_NAME, String.class);
				if (cVar != null) companyName = (String)cVar.getObject();
				if (companyName.length() > 0)
				{
					point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox/2, -schYSize/2 + frameWid + yLogoBox*5/15);
					addText(point0, yLogoBox*2/15, xLogoBox, yLogoBox*2/15, "Company: " + companyName);
				}

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
	public synchronized Iterator<NodeInst> getNodes()
	{
        ArrayList<NodeInst> nodesCopy = new ArrayList<NodeInst>(nodes);
		return nodesCopy.iterator();
	}

	/**
	 * Method to return an Iterator over all NodeInst objects in this Cell.
	 * @return an Iterator over all NodeInst objects in this Cell.
	 */
	public synchronized Iterator<Nodable> getNodables()
	{
        ArrayList<Nodable> nodesCopy = new ArrayList<Nodable>(nodes);
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
	 * Method to return the PortInst by nodeId and PortProtoId.
	 * @param nodeId specified NodeId.
     * @param portProtoId
	 * @return the PortInst at specified position..
	 */
    public PortInst getPortInst(int nodeId, PortProtoId portProtoId) {
        NodeInst ni = chronNodes.get(nodeId);
        assert ni.getD().protoId == portProtoId.getParentId();
        NodeProto np = ni.getProto();
        PortProto pp = np.getPort(portProtoId);
        return ni.getPortInst(pp.getPortIndex());
    }
    
    /**
     * Method to return an Iterator over all CellUsage objects in this Cell.
     * @return an Iterator over all CellUsage objects in this Cell.
     */
    public synchronized Iterator<CellUsage> getUsagesIn() {
        return new Iterator<CellUsage>() {
            private int i = 0;
            CellUsage nextU = findNext();
            
            public boolean hasNext() { return nextU != null; }
            
/*5*/       public CellUsage next() {
//4*/       public Object next() {
                if (nextU == null) throw new NoSuchElementException();
                CellUsage u = nextU;
                nextU = findNext();
                return u;
            }
            
            public void remove() { throw new UnsupportedOperationException(); };
            
            private CellUsage findNext() {
                while (i < cellUsages.length) {
                    if (cellUsages[i] != 0)
                        return d.cellId.getUsageIn(i++);
                    i++;
                }
                return null;
            }
        };
    }

    /**
     * Method to return the number of NodeUsage objects in this Cell.
     * @return the number of NodeUsage objects in this Cell.
     */
    public int getNumUsagesIn() {
        int numUsages = 0;
        for (int i = 0; i < cellUsages.length; i++) {
            if (cellUsages[i] != 0)
                numUsages++;
        }
        return numUsages;
    }

	/**
	 * Method to find a named NodeInst on this Cell.
	 * @param name the name of the NodeInst.
	 * @return the NodeInst.  Returns null if none with that name are found.
	 */
	public NodeInst findNode(String name)
	{
		int nodeIndex = searchNode(name, 0);
		if (nodeIndex >= 0) return (NodeInst)nodes.get(nodeIndex);
		nodeIndex = - nodeIndex - 1;
		if (nodeIndex < nodes.size())
		{
			NodeInst ni = (NodeInst)nodes.get(nodeIndex);
			if (ni.getName().equals(name)) return ni;
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
	 * @return true on failure
	 */
	public boolean addNode(NodeInst ni)
	{
		checkChanging();

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
						return true;
					} else {
						System.out.println("WARNING: "+ libDescribe() + " instantiates " +
							instProto.libDescribe() + " which causes a circular library dependence: ");
						System.out.println(libDep.toString());
					}
				}
			}
		}

		addNodeName(ni);
        int nodeId = ni.getD().nodeId;
        while (chronNodes.size() <= nodeId) chronNodes.add(null);
        assert chronNodes.get(nodeId) == null;
        chronNodes.set(nodeId, ni);
        setContentsModified();
        
        // count usage
        if (protoType instanceof Cell) {
            CellUsage u = d.cellId.getUsageIn(((Cell)protoType).d.cellId);
            if (cellUsages.length <= u.indexInParent) {
                int[] newCellUsages = new int[u.indexInParent + 1];
                System.arraycopy(cellUsages, 0, newCellUsages, 0, cellUsages.length);
                cellUsages = newCellUsages;
            }
            cellUsages[u.indexInParent]++;
        }
		return false;
	}

	/**
	 * Method to add a new NodeInst to the name index of this cell.
	 * @param ni the NodeInst to be included tp the name index in the cell.
	 */
	public void addNodeName(NodeInst ni)
	{
		int nodeIndex = searchNode(ni.getName(), ni.getD().nodeId);
		assert nodeIndex < 0;
		nodeIndex = - nodeIndex - 1;
		nodes.add(nodeIndex, ni);
		for (; nodeIndex < nodes.size(); nodeIndex++)
		{
			NodeInst n = (NodeInst)nodes.get(nodeIndex);
			n.setNodeIndex(nodeIndex);
		}
        
        // add temporary name
		Name name = ni.getNameKey();
		if (!name.isTempname()) return;
		tempNodeNames.put(name.canonicString(), ni);

		Name basename = name.getBasename();
		if (basename != null && basename != name)
		{
			String basenameString = basename.canonicString(); 
			MaxSuffix ms = maxSuffix.get(basenameString);
			if (ms == null)
			{
				ms = new MaxSuffix();
				maxSuffix.put(basenameString, ms);
			}
			int numSuffix = name.getNumSuffix();
			if (numSuffix > ms.v)
			{
				ms.v = numSuffix;
			}
		}
	}

	/**
	 * Method check if NodeInst with specified temporary name key exists in a cell.
	 * @param name specified temorary name key.
	 */
	public boolean hasTempNodeName(Name name)
	{
		return tempNodeNames.containsKey(name.canonicString());
	}

	/**
	 * Method to return unique autoname for NodeInst in this cell.
	 * @param basename base name of autoname
	 * @return autoname
	 */
	public Name getNodeAutoname(Name basename)
	{
        String basenameString = basename.canonicString();
		MaxSuffix ms = maxSuffix.get(basenameString);
		if (ms == null)
		{
			ms = new MaxSuffix();
			maxSuffix.put(basenameString, ms);
			return basename.findSuffixed(0);
		} else 
		{
			ms.v++;
			return basename.findSuffixed(ms.v);
		}
	}

	/**
	 * Method to remove an NodeInst from the cell.
	 * @param ni the NodeInst to be removed from the cell.
	 */
	public void removeNode(NodeInst ni)
	{
		checkChanging();
		assert ni.isLinked();

        // remove usage count
        if (ni.getProto() instanceof Cell) {
            CellUsage u = d.cellId.getUsageIn(((Cell)ni.getProto()).d.cellId);
            cellUsages[u.indexInParent]--;
            if (cellUsages[u.indexInParent] <= 0) {
                assert cellUsages[u.indexInParent] == 0;
                // remove library dependency, if possible
                getLibrary().removeReferencedLib(((Cell)ni.getProto()).getLibrary());
             }
        }

		removeNodeName(ni);
        int nodeId = ni.getD().nodeId;
        assert chronNodes.get(nodeId) == ni;
        chronNodes.set(nodeId, null);
        setContentsModified();
	}

	/**
	 * Method to remove an NodeInst from the name index of this cell.
	 * @param ni the NodeInst to be removed from the cell.
	 */
	public void removeNodeName(NodeInst ni)
	{
		int nodeIndex = ni.getNodeIndex();
		NodeInst removedNi = (NodeInst) nodes.remove(nodeIndex);
		assert removedNi == ni;
		for (int i = nodeIndex; i < nodes.size(); i++)
		{
			NodeInst n = (NodeInst)nodes.get(i);
			n.setNodeIndex(i);
		}
		ni.setNodeIndex(-1);
        
        // remove temporary name
		if (!ni.isUsernamed())
            tempNodeNames.remove(ni.getNameKey().canonicString());
	}

    /**
     * Searches the nodes for the specified (name,nodeId) pair using the binary
     * search algorithm.
     * @param name the name to be searched.
	 * @param nodeId the nodeId index to be searched.
     * @return index of the search name, if it is contained in the nodes;
     *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *	       <i>insertion point</i> is defined as the point at which the
     *	       NodeInst would be inserted into the list: the index of the first
     *	       element greater than the name, or <tt>nodes.size()</tt>, if all
     *	       elements in the list are less than the specified name.  Note
     *	       that this guarantees that the return value will be &gt;= 0 if
     *	       and only if the NodeInst is found.
     */
	private int searchNode(String name, int nodeId)
	{
        int low = 0;
        int high = nodes.size()-1;
        int pick = high; // initially try the last postition
		while (low <= high) {
			NodeInst ni = (NodeInst)nodes.get(pick);
			int cmp = TextUtils.STRING_NUMBER_ORDER.compare(ni.getName(), name);
			if (cmp == 0) cmp = ni.getD().nodeId - nodeId;

			if (cmp < 0)
				low = pick + 1;
			else if (cmp > 0)
				high = pick - 1;
			else
				return pick; // NodeInst found
			pick = (low + high) >> 1; // try in a middle
		}
		return -(low + 1);  // NodeInst not found.
    }

	/**
	 * Method to link a NodeInst object into the R-tree of this Cell.
	 * @param ni a NodeInst object.
	 */
	public void linkNode(NodeInst ni)
	{
		setDirty();
		RTNode.linkGeom(this, ni);

		// make additional checks to keep circuit up-to-date
		NodeProto np = ni.getProto();
		if (np == Generic.tech.essentialBoundsNode)
			essenBounds.add(ni);
	}

	/**
	 * Method to unlink a NodeInst from the R-tree of this Cell.
	 * @param ni a NodeInst object.
	 */
	public void unLinkNode(NodeInst ni)
	{
		setDirty();
		RTNode.unLinkGeom(this, ni);
		essenBounds.remove(ni);
	}

	/****************************** ARCS ******************************/

	/**
	 * Method to return an Iterator over all ArcInst objects in this Cell.
	 * @return an Iterator over all ArcInst objects in this Cell.
	 */
	public synchronized Iterator<ArcInst> getArcs()
	{
        ArrayList<ArcInst> arcsCopy = new ArrayList<ArcInst>(arcs);
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
		int arcIndex = searchArc(name, 0);
		if (arcIndex >= 0) return (ArcInst)arcs.get(arcIndex);
		arcIndex = - arcIndex - 1;
		if (arcIndex < arcs.size())
		{
			ArcInst ai = (ArcInst)arcs.get(arcIndex);
			if (ai.getName().equals(name)) return ai;
		}
		return null;
	}

	/**
	 * Method to add a new ArcInst to the cell.
	 * @param ai the ArcInst to be included in the cell.
	 */
	public void addArc(ArcInst ai)
	{
		int arcIndex = searchArc(ai.getName(), ai.getD().arcId);
		assert arcIndex < 0;
		arcIndex = - arcIndex - 1;
		arcs.add(arcIndex, ai);
		for (; arcIndex < arcs.size(); arcIndex++)
		{
			ArcInst a = (ArcInst)arcs.get(arcIndex);
			a.setArcIndex(arcIndex);
		}
        int arcId = ai.getD().arcId;
        while (chronArcs.size() <= arcId) chronArcs.add(null);
        assert chronArcs.get(arcId) == null;
        chronArcs.set(arcId, ai);
        setContentsModified();
        
        // update maximal arc name suffux temporary name
		if (ai.isUsernamed()) return;
		Name name = ai.getNameKey();
        assert name.getBasename() == ImmutableArcInst.BASENAME;
        maxArcSuffix = Math.max(maxArcSuffix, name.getNumSuffix());
	}

	/**
	 * Method to return unique autoname for ArcInst in this cell.
	 * @return a unique autoname for ArcInst in this cell.
	 */
	public Name getArcAutoname()
	{
        if (maxArcSuffix < Integer.MAX_VALUE)
            return ImmutableArcInst.BASENAME.findSuffixed(++maxArcSuffix);
        for (int i = 0;; i++) {
            Name name = ImmutableArcInst.BASENAME.findSuffixed(i);
            if (!hasTempArcName(name)) return name;
        }
	}

	/**
	 * Method check if ArcInst with specified temporary name key exists in a cell.
	 * @param name specified temorary name key.
	 */
	public boolean hasTempArcName(Name name)
	{
		return name.isTempname() && findArc(name.toString()) != null;
	}

	/**
	 * Method to remove an ArcInst from the cell.
	 * @param ai the ArcInst to be removed from the cell.
	 */
	public void removeArc(ArcInst ai)
	{
		checkChanging();
		assert ai.isLinked();
		int arcIndex = ai.getArcIndex();
		ArcInst removedAi = (ArcInst) arcs.remove(arcIndex);
		assert removedAi == ai;
		for (int i = arcIndex; i < arcs.size(); i++)
		{
			ArcInst a = (ArcInst)arcs.get(i);
			a.setArcIndex(i);
		}
		ai.setArcIndex(-1);
        int arcId = ai.getD().arcId;
        assert chronArcs.get(arcId) == ai;
        chronArcs.set(arcId, null);
        setContentsModified();
	}

    /**
     * Searches the arcs for the specified (name,arcId) using the binary
     * search algorithm.
     * @param name the name to be searched.
	 * @param arcId the arcId index to be searched.
     * @return index of the search name, if it is contained in the arcs;
     *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *	       <i>insertion point</i> is defined as the point at which the
     *	       ArcInst would be inserted into the list: the index of the first
     *	       element greater than the name, or <tt>arcs.size()</tt>, if all
     *	       elements in the list are less than the specified name.  Note
     *	       that this guarantees that the return value will be &gt;= 0 if
     *	       and only if the ArcInst is found.
     */
	private int searchArc(String name, int arcId)
	{
		int low = 0;
		int high = arcs.size()-1;

		while (low <= high) {
			int mid = (low + high) >> 1;
			ArcInst ai = (ArcInst)arcs.get(mid);
			int cmp = TextUtils.STRING_NUMBER_ORDER.compare(ai.getName(), name);
			if (cmp == 0) cmp = ai.getD().arcId - arcId;

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // ArcInst found
		}
		return -(low + 1);  // ArcInst not found.
    }

	/**
	 * Method to link an ArcInst object into the R-tree of this Cell.
	 * @param ai an ArcInst object.
	 */
	public void linkArc(ArcInst ai)
	{
		setDirty();
		RTNode.linkGeom(this, ai);
	}

	/**
	 * Method to unlink an ArcInst object from the R-tree of this Cell.
	 * @param ai an ArcInst object.
	 */
	public void unLinkArc(ArcInst ai)
	{
		setDirty();
		RTNode.unLinkGeom(this, ai);
	}

	/****************************** EXPORTS ******************************/

	/**
	 * Add a PortProto to this NodeProto.
	 * Adds Exports for Cells, PrimitivePorts for PrimitiveNodes.
	 * @param export the PortProto to add to this NodeProto.
	 * @param oldPortInsts a collection of PortInsts to Undo or null.
	 */
	 void addExport(Export export)
	{
		checkChanging();
		int portIndex = - searchExport(export.getName()) - 1;
		assert portIndex >= 0;
		export.setPortIndex(portIndex);

        // Add to chronExprots 
        int chronIndex = export.getId().getChronIndex();
        if (chronExports.length <= chronIndex) {
            Export[] newChronExports = new Export[Math.max(chronIndex + 1, chronExports.length*2)];
            System.arraycopy(chronExports, 0, newChronExports, 0, chronExports.length);
            chronExports = newChronExports;
        }
        chronExports[chronIndex] = export;
        
		Export[] newExports = new Export[exports.length + 1];
		System.arraycopy(exports, 0, newExports, 0, portIndex);
		newExports[portIndex] = export;
		for (int i = portIndex; i < exports.length; i++)
		{
			Export e = exports[i];
			e.setPortIndex(i + 1);
			newExports[i + 1] = e;
		}
		exports = newExports;
        setContentsModified();

		// create a PortInst for every instance of this Cell
        if (d.cellId.numUsagesOf() == 0) return;
        int[] pattern = new int[exports.length];
        for (int i = 0; i < portIndex; i++) pattern[i] = i;
        pattern[portIndex] = -1;
        for (int i = portIndex + 1; i < exports.length; i++) pattern[i] = i - 1;
        updatePortInsts(pattern);
//        for(Iterator<NodeInst> it = getInstancesOf(); it.hasNext(); ) {
//            NodeInst ni = (NodeInst)it.next();
//            ni.addPortInst(export);
//            assert ni.getNumPortInsts() == exports.length;
//        }
	}

	/**
	 * Removes an Export from this Cell.
	 * @param export the Export to remove from this Cell.
	 */
	void removeExport(Export export)
	{
		checkChanging();
		int portIndex = export.getPortIndex();

		Export[] newExports = exports.length > 1 ? new Export[exports.length - 1] : NULL_EXPORT_ARRAY;
		System.arraycopy(exports, 0, newExports, 0, portIndex);
		for (int i = portIndex; i < newExports.length; i++)
		{
			Export e = exports[i + 1];
			e.setPortIndex(i);
			newExports[i] = e;
		}
		exports = newExports;
        chronExports[export.getId().getChronIndex()] = null;
        setContentsModified();
		export.setPortIndex(-1);

		// remove the PortInst from every instance of this Cell
        if (d.cellId.numUsagesOf() == 0) return;
        int[] pattern = new int[exports.length];
        for (int i = 0; i < portIndex; i++) pattern[i] = i;
        for (int i = portIndex; i < exports.length; i++) pattern[i] = i + 1;
        updatePortInsts(pattern);
//		for(Iterator<NodeInst> it = getInstancesOf(); it.hasNext(); )
//		{
//			NodeInst ni = (NodeInst)it.next();
//			ni.removePortInst(export);
//		}
	}

	/**
	 * Move renamed Export in sorted exports array.
	 * @param oldPortIndex old position of the Export in exports array.
	 */
	void moveExport(int oldPortIndex, String newName)
	{
		Export export = exports[oldPortIndex];
		int newPortIndex = - searchExport(newName) - 1;
		if (newPortIndex < 0) return;
		if (newPortIndex > oldPortIndex)
			newPortIndex--;
		if (newPortIndex == oldPortIndex) return;
		
		if (newPortIndex > oldPortIndex)
		{
			for (int i = oldPortIndex; i < newPortIndex; i++)
			{
				Export e = exports[i + 1];
				e.setPortIndex(i);
				exports[i] = e;
			}
		} else
		{
			for (int i = oldPortIndex; i > newPortIndex; i--)
			{
				Export e = exports[i - 1];
				e.setPortIndex(i);
				exports[i] = e;
			}
		}
		export.setPortIndex(newPortIndex);
		exports[newPortIndex] = export;
		for (int i = 0; i < exports.length; i++)
			System.out.print(" " + exports[i].getPortIndex() + ":" + exports[i].getName());
		System.out.println();

        // move PortInst for every instance of this Cell.
        if (d.cellId.numUsagesOf() == 0) return;
        int[] pattern = new int[exports.length];
        for (int i = 0; i < pattern.length; i++) pattern[i] = i;
        pattern[newPortIndex] = oldPortIndex;
        if (newPortIndex > oldPortIndex)
            for (int i = oldPortIndex; i < newPortIndex; i++) pattern[i] = i + 1;
        else
            for (int i = oldPortIndex; i > newPortIndex; i--) pattern[i] = i - 1;
        updatePortInsts(pattern);
        
        // move connections for every instance of this Cell.
		for(Iterator<NodeInst> it = getInstancesOf(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			ni.moveConnections(export);
		}
	}

	/**
	 * Update PortInsts of all instances of this Cell accoding to pattern.
     * Pattern contains an element for each Export.
     * If Export was just created, the element contains -1.
     * For old Exports the element contains old index of the Export.
	 * @param pattern array with elements describing new PortInsts.
	 */
    public void updatePortInsts(int[] pattern) {
		for(Iterator<NodeInst> it = getInstancesOf(); it.hasNext(); ) {
			NodeInst ni = (NodeInst)it.next();
			ni.updatePortInsts(pattern);
		}
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
		int portIndex = searchExport(name.toString());
		if (portIndex >= 0) return exports[portIndex];
		String nameString = name.canonicString();
		for (int i = 0; i < exports.length; i++)
		{
			Export e = exports[i];
			if (e.getNameKey().canonicString() == nameString)
				return e;
		}
		return null;
	}

	/**
	 * Method to determine if a given PortProto is considered as export
	 * @param port the PortProto in question.
	 * @return true if the PortProto is an export.
	 */
// 	public boolean findPortProto(PortProto port)
// 	{
// 		for (int i = 0; i < exports.length; i++)
// 		{
// 			PortProto pp = (PortProto) exports[i];
// 			if (pp == port) return true;
// 		}
// 		return false;
// 	}

	/**
	 * Method to return an iterator over all PortProtos of this NodeProto.
	 * @return an iterator over all PortProtos of this NodeProto.
	 */
	public Iterator<PortProto> getPorts() { return ArrayIterator.iterator((PortProto[])exports); }

	/**
	 * Method to return an iterator over all Exports of this NodeProto.
	 * @return an iterator over all Exports of this NodeProto.
	 */
	public Iterator<Export> getExports() { return ArrayIterator.iterator(exports); }

	/**
	 * Method to return the number of PortProtos on this NodeProto.
	 * @return the number of PortProtos on this NodeProto.
	 */
	public int getNumPorts() { return exports.length; }

	/**
	 * Method to return the PortProto at specified position.
	 * @param portIndex specified position of PortProto.
	 * @return the PortProto at specified position..
	 */
	public PortProto getPort(int portIndex) { return exports[portIndex]; }

	/**
	 * Method to return the PortProto by thread-independent PortProtoId.
	 * @param portProtoId thread-independent PortProtoId.
	 * @return the PortProto.
	 */
	public PortProto getPort(PortProtoId portProtoId) {
        if (portProtoId.getParentId() != d.cellId) throw new IllegalArgumentException();
        return chronExports[portProtoId.getChronIndex()];
    }

	/**
	 * Method to return the Export at specified chronological index.
	 * @param chronIndex specified chronological index of Export.
	 * @return the Export at specified chronological index or null.
	 */
	public Export getExportChron(int chronIndex) {
        try {
            return chronExports[chronIndex];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
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

    /**
     * Searches the exports for the specified name using the binary
     * search algorithm.
     * @param name the name to be searched.
     * @return index of the search name, if it is contained in the exports;
     *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *	       <i>insertion point</i> is defined as the point at which the
     *	       Export would be inserted into the list: the index of the first
     *	       element greater than the name, or <tt>exports.length()</tt>, if all
     *	       elements in the list are less than the specified name.  Note
     *	       that this guarantees that the return value will be &gt;= 0 if
     *	       and only if the Export is found.
     */
	private int searchExport(String name)
	{
		int low = 0;
		int high = exports.length-1;

		while (low <= high) {
			int mid = (low + high) >> 1;
			Export e = exports[mid];
			int cmp = TextUtils.STRING_NUMBER_ORDER.compare(e.getName(), name);

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // Export found
		}
		return -(low + 1);  // Export not found.
    }

	/****************************** TEXT ******************************/

	/**
	 * Method to return the pure name of this Cell, without
	 * any view or version information.
	 * @return the pure name of this Cell.
	 */
	public String getName() { return cellName.getName(); }

	/**
	 * Method to return the CellName object describing this Cell.
	 * @return the CellName object describing this Cell.
	 */
	public CellName getCellName()
	{
		return cellName;
	}

	/**
	 * Method to describe this cell.
	 * The description has the form: cell;version{view}
	 * If the cell is not from the current library, prepend the library name.
     * @param withQuotes to wrap description between quotes
	 * @return a String that describes this cell.
	 */
	public String describe(boolean withQuotes)
	{
		String name = "";
		if (lib != Library.getCurrent())
			name += lib.getName() + ":";
		name += noLibDescribe();
		return (withQuotes) ? "'"+name+"'" : name;
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
		String name = getName();
		if (getNewestVersion() != this)
			name += ";" + getVersion();
		name += "{" +  getView().getAbbreviation() + "}";
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
     * Method to return the Variable on this Cell with the given key
     * that is a parameter. Returns null if not found.
     * @param key the key of the variable
     * @return the Variable with that key, that is parameter. Returns null if none found.
     */
    public Variable getParameter(Variable.Key key) {
        Variable var = getVar(key);
        return var != null && var.getTextDescriptor().isParam() ? var : null;
    }

    /**
     * Method to return an Iterator over all Variables marked as parameters on this Cell.
     * @return an Iterator over all Variables on this Cell.
     */
    public Iterator<Variable> getParameters() {
        TreeMap<Variable.Key,Variable> keysToVars = new TreeMap<Variable.Key,Variable>();
        // get all parameters on this object
        for (Iterator<Variable> it = getVariables(); it.hasNext(); ) {
            Variable v = (Variable)it.next();
            if (!v.getTextDescriptor().isParam()) continue;
            keysToVars.put(v.getKey(), v);
        }
        return keysToVars.values().iterator();
    }

	/**
	 * Method to return true if the Variable on this ElectricObject with given key is a parameter.
	 * Parameters are those Variables that have values on instances which are
	 * passed down the hierarchy into the contents.
	 * Parameters can only exist on NodeInst objects.
     * @param varKey key to test
	 * @return true if the Variable with given key is a parameter.
	 */
    public boolean isParam(Variable.Key varKey) {
        Variable var = getVar(varKey);
        return var != null && var.getTextDescriptor().isParam();
    }
    
	/**
	 * Method to return a list of Polys that describes all text on this Cell.
	 * @param hardToSelect is true if considering hard-to-select text.
	 * @param wnd the window in which the text will be drawn.
	 * @return an array of Polys that describes the text.
	 */
	public Poly [] getAllText(boolean hardToSelect, EditWindow0 wnd)
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
	 * @param wnd the EditWindow0 in which this Cell is being displayed.
	 * @return the bounds of the relative (scalable) text.
	 */
	public Rectangle2D getRelativeTextBounds(EditWindow0 wnd)
	{
		Rectangle2D bounds = null;
		for(Iterator<NodeInst> it = this.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			bounds = accumulateTextBoundsOnObject(ni, bounds, wnd);
			for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
			{
				PortInst pi = (PortInst)pIt.next();
				bounds = accumulateTextBoundsOnObject(pi, bounds, wnd);
			}
		}
		for(Iterator<ArcInst> it = this.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			bounds = accumulateTextBoundsOnObject(ai, bounds, wnd);
		}
		for(Iterator<Export> it = this.getExports(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			bounds = accumulateTextBoundsOnObject(pp, bounds, wnd);
		}
		bounds = accumulateTextBoundsOnObject(this, bounds, wnd);
		return bounds;
	}

	private Rectangle2D accumulateTextBoundsOnObject(ElectricObject eObj, Rectangle2D bounds, EditWindow0 wnd)
	{
		Rectangle2D objBounds = eObj.getTextBounds(wnd);
		if (objBounds == null) return bounds;
		if (bounds == null) return objBounds;
		Rectangle2D.union(bounds, objBounds, bounds);
		return bounds;
	}

	/**
	 * Method to return the basename for autonaming instances of this Cell.
	 * @return the basename for autonaming instances of this Cell.
	 */
	public Name getBasename() { return basename; }

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
			for(Iterator<PortProto> it = getPorts(); it.hasNext(); )
			{
				PortProto pp = (PortProto)it.next();
				if (TextUtils.startsWithIgnoreCase(pp.getName(), prefix))
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
			for(Iterator<NodeInst> it = getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (TextUtils.startsWithIgnoreCase(ni.getName(), prefix))
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
			for(Iterator<ArcInst> it = getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				if (TextUtils.startsWithIgnoreCase(ai.getName(), prefix))
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
//		name = name.canonic();
		if (cls == PortProto.class)
		{
			PortProto pp = findExport(name);
			if (pp == null || exclude == pp) return true;
			return false;
		}
		if (cls == NodeInst.class)
		{
            String nameString = name.canonicString();
			if (name.isTempname())
			{
				NodeInst ni = tempNodeNames.get(nameString);
				return ni == null || exclude == ni;
			}
			for(Iterator<NodeInst> it = getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (exclude == ni) continue;
				Name nodeName = ni.getNameKey();
				if (nameString == nodeName.canonicString()) return false;
			}
			return true;
		}
		if (cls == ArcInst.class)
		{
            String nameString = name.canonicString();
			if (name.isTempname())
			{
                ArcInst ai = findArc(nameString);
				return ai == null || exclude == ai;
			}
			for(Iterator<ArcInst> it = getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				if (exclude == ai) continue;
				Name arcName = ai.getNameKey();
				if (nameString == arcName.canonicString()) return false;
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
			name.equals("SIM_window_signal_order") ||
			name.equals("SIM_window_signalorder")) return true;
		return super.isDeprecatedVariable(key);
	}

	/**
	 * Returns a printable version of this Cell.
	 * @return a printable version of this Cell.
	 */
	public String toString()
	{
		return "cell " + describe(true);
	}

	/****************************** HIERARCHY ******************************/

    /**
     * Returns persistent data of this Cell.
     * @return persistent data of this Cell.
     */
    public ImmutableCell getD() { return d; }
    
    /**
     * Modifies persistend data of this Cell.
     * @param newD new persistent data.
     * @return true if persistent data was modified.
     */
    private boolean setD(ImmutableCell newD) {
        checkChanging();
        ImmutableCell oldD = d;
        if (newD == oldD) return false;
        d = newD;
        Undo.modifyVariables(this, oldD);
        return true;
    }

    /**
     * Returns persistent data of this ElectricObject.
     * @return persistent data of this ElectricObject.
     */
    public ImmutableElectricObject getImmutable() { return d; }
    
    public void lowLevelModifyVariables(ImmutableCell d) { this.d = d; }
        
    /**
     * Method to add a Variable on this Cell.
     * It may add repaired copy of this Variable in some cases.
     * @param var Variable to add.
     */
    public void addVar(Variable var) {
        setD(d.withVariable(var));
    }

	/**
	 * Method to delete a Variable from this Cell.
	 * @param key the key of the Variable to delete.
	 */
	public void delVar(Variable.Key key)
	{
        setD(d.withoutVariable(key));
	}
    
    /**
     * Method to return NodeProtoId of this NodeProto.
     * NodeProtoId identifies NodeProto independently of threads.
     * @return NodeProtoId of this NodeProto.
     */
    public NodeProtoId getId() { return d.cellId; }
    
    /**
     * Returns a Cell by CellId.
     * Returns null if the Cell is not linked to the database.
     * @param cellId CellId to find.
     * @return Cell or null.
     */
    public static Cell inCurrentThread(CellId cellId) {
        try {
            return (Cell)linkedCells.get(cellId.cellIndex);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }
    
	/**
	 * Method to return an iterator over all usages of this NodeProto.
	 * @return an iterator over all usages of this NodeProto.
	 */
	public Iterator<CellUsage> getUsagesOf()
	{
        return new Iterator<CellUsage>() {
            int i;
            CellUsage nextU = findNext();
            
            public boolean hasNext() { return nextU != null; }
            
/*5*/       public CellUsage next() {
//4*/       public Object next() {
                if (nextU == null) throw new NoSuchElementException();
                CellUsage u = nextU;
                nextU = findNext();
                return u;
            }
            
            public void remove() { throw new UnsupportedOperationException(); };
            
            private CellUsage findNext() {
                while (i < d.cellId.numUsagesOf()) {
                    CellUsage u = d.cellId.getUsageOf(i++);
                    Cell parent = u.getParent();
                    if (parent == null) continue;
                    if (u.indexInParent >= parent.cellUsages.length) continue;
                    if (parent.cellUsages[u.indexInParent] > 0) return u;
                }
                return null;
            }
        };
	}

	/**
	 * Method to return an iterator over all instances of this NodeProto.
	 * @return an iterator over all instances of this NodeProto.
	 */
	public Iterator<NodeInst> getInstancesOf()
	{
		return new NodeInstsIterator();
	}

	private class NodeInstsIterator implements Iterator<NodeInst>
	{
		private Iterator<CellUsage> uit;
        private Cell cell;
		private int i, n;
        private NodeInst ni;

		NodeInstsIterator()
		{
			uit = getUsagesOf();
            findNext();
		}

		public boolean hasNext() { return ni != null; }

/*5*/	public NodeInst next()
//4*/	public Object next()
		{
            NodeInst ni = this.ni;
            if (ni == null) throw new NoSuchElementException();
            findNext();
            return ni;
		}

		public void remove() { throw new UnsupportedOperationException("NodeInstsIterator.remove()"); };

        private void findNext() {
            for (;;) {
                while (i < n) {
                    ni = cell.getNode(i++);
                    if (ni.getProto() == Cell.this)
                        return;
                }
                if (!uit.hasNext()) {
                    ni = null;
                    return;
                }
                CellUsage u = (CellUsage)uit.next();
                cell = u.getParent();
                if (cell == null) continue;
                i = 0;
                n = cell.getNumNodes();
            }
        }
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
        if (toInstantiate == parent)
            return true;

        // special case: allow instance of icon inside of the contents for illustration
        if (toInstantiate.isIconOf(parent)) {
            if (toInstantiate.isIcon() && !parent.isIcon())
                return false;
        }

        // if the parent is a child of the cell to instantiate, that would be a
        // recursive operation
        if (parent.isAChildOf(toInstantiate))
            return true;

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
		return getIsAChildOf(parent, new HashMap<Cell,Cell>());
	}

	private boolean getIsAChildOf(Cell parent, Map<Cell,Cell> checkedParents)
	{
        // if parent is an icon view, also check contents view
        if (parent.isIcon()) {
            Cell c = parent.contentsView();
            if (c != null && c != parent) {
                if (getIsAChildOf(c, checkedParents))
                    return true;
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

        for (Iterator<NodeInst> it = parent.getNodes(); it.hasNext(); ) {
            NodeInst ni = (NodeInst)it.next();
            NodeProto np = ni.getProto();
            if (np instanceof Cell) {
                Cell c = (Cell)np;
                // ignore instances of icon view inside content view
                if (c.isIconOf(parent)) continue;
                if (c == contentView) return true;
                if (c == iconView) return true;
                // recurse
                if (getIsAChildOf(c, checkedParents))
                    return true;
            }
        }
        return false;
    }

//    private boolean getIsAParentOf(Cell child)
//    {
//        if (this == child) return true;
//
//		// look through every instance of the child cell
//		Cell lastParent = null;
//		for(Iterator<NodeInst> it = child.getInstancesOf(); it.hasNext(); )
//		{
//			NodeInst ni = (NodeInst)it.next();
//
//			// if two instances in a row have same parent, skip this one
//			if (ni.getParent() == lastParent) continue;
//			lastParent = ni.getParent();
//
//			// recurse to see if the grandparent belongs to the child
//			if (getIsAParentOf(ni.getParent())) return true;
//		}
//
//		// if this has an icon, look at it's instances
//		Cell np = child.iconView();
//		if (np != null)
//		{
//			lastParent = null;
//			for(Iterator<NodeInst> it = np.getInstancesOf(); it.hasNext(); )
//			{
//				NodeInst ni = (NodeInst)it.next();
//
//				// if two instances in a row have same parent, skip this one
//				if (ni.getParent() == lastParent) continue;
//				lastParent = ni.getParent();
//
//				// special case: allow an icon to be inside of the contents for illustration
//				NodeProto niProto = ni.getProto();
//				if (niProto instanceof Cell)
//				{
//					if (((Cell)niProto).isIconOf(child))
//					{
//						if (!child.isIcon()) continue;
//					}
//				}
//
//				// recurse to see if the grandparent belongs to the child
//				if (getIsAParentOf(ni.getParent())) return true;
//			}
//		}
//		return false;
//	}

	/**
	 * Method to determine whether this Cell is in use anywhere.
	 * If it is, an error dialog is displayed.
	 * @param action a description of the intended action (i.e. "delete").
	 * @param quiet true not to warn the user of the cell being used.
	 * @return true if this Cell is in use anywhere.
	 */
	public boolean isInUse(String action, boolean quiet)
	{
		String parents = null;
		for(Iterator<CellUsage> it = getUsagesOf(); it.hasNext(); )
		{
			CellUsage u = (CellUsage)it.next();
			Cell parent = u.getParent();
			if (parents == null) parents = parent.describe(true); else
				parents += ", " + parent.describe(true);
		}
		if (parents != null)
		{
			if (!quiet)
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), "Cannot " + action + " " + this +
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
	public int getVersion() { return cellName.getVersion(); }

	/**
	 * Method to return the number of different versions of this Cell.
	 * @return the number of different versions of this Cell.
	 */
	public int getNumVersions()
	{
		int count = 0;
		String protoName = getName();
		View view = getView();
		synchronized (lib.cells) {
			for (Iterator<Cell> it = getVersionsTail(); it.hasNext(); )
			{
				Cell c = (Cell)it.next();
				if (!c.getName().equals(protoName) || c.getView() != view) break;
				count++;
			}
		}
		return count;
	}

	/**
	 * Method to return an Iterator over the different versions of this Cell.
	 * @return an Iterator over the different versions of this Cell.
	 */
	public Iterator<Cell> getVersions()
	{
		ArrayList<Cell> versions = new ArrayList<Cell>();
		String protoName = getName();
		View view = getView();
		synchronized (lib.cells) {
			for (Iterator<Cell> it = getVersionsTail(); it.hasNext(); )
			{
				Cell c = (Cell)it.next();
				if (!c.getName().equals(protoName) || c.getView() != view) break;
				versions.add(c);
			}
		}
		return versions.iterator();
	}

	/**
	 * Method to return the most recent version of this Cell.
	 * @return he most recent version of this Cell.
	 */
	public Cell getNewestVersion()
	{
		synchronized (lib.cells) {
			Iterator<Cell> it = getVersionsTail();
			if (it.hasNext())
			{
				Cell c = (Cell)it.next();
				if (c.getName().equals(getName()) && c.getView() == getView()) return c;
			}
		}
		return null;
	}

	/*
	 * Return tail submap of library cells which starts from
	 * cells with same protoName and view as this Cell.
	 * @return tail submap with versions of this Cell.
	 */
	private Iterator<Cell> getVersionsTail()
	{
		CellName cn = CellName.parseName(getName() + "{" + getView().getAbbreviation() + "}");
		return lib.getCellsTail(cn);
	}

	/*
	 * Return tail submap of library cells which starts from
	 * cells with same protoName as this Cell.
	 * @return tail submap with views of this Cell.
	 */
	private Iterator<Cell> getViewsTail()
	{
		CellName cn = CellName.parseName(getName());
		return lib.getCellsTail(cn);
	}

	/****************************** GROUPS ******************************/

	/**
	 * Method to get the CellGroup that this Cell is part of.
	 * @return the CellGroup that this Cell is part of.
	 */
	public CellGroup getCellGroup() { return cellGroup; }

	/**
	 * Method to move this Cell together with all its versions and views
	 * to the group of another Cell.
	 * @param otherCell the other cell whose group this Cell should join.
	 */
	public void joinGroup(Cell otherCell)
	{
		setCellGroup(otherCell.getCellGroup());
	}

	/**
	 * Method to Cell together with all its versions and views into its own CellGroup.
	 * If there is no already Cells withs other names in its CellGroup, nothing is done.
	 */
	public void putInOwnCellGroup()
	{
		setCellGroup(null);
	}

	/**
	 * Method to put this Cell together with all its versions and views into the given CellGroup.
	 * @param cellGroup the CellGroup that this cell belongs to or null to put int own cell group
	 */
	public void setCellGroup(CellGroup cellGroup)
	{
		if (!isLinked()) return;
		CellGroup oldCellGroup = this.cellGroup;
		if (cellGroup == null) cellGroup = new CellGroup();
		lowLevelSetCellGroup(cellGroup);
		Undo.modifyCellGroup(this, oldCellGroup);
	}

    /**
     * Low-level method to set a Cell's cell group. Do not use this
     * unless you know what you are doing. This method bypasses Undo.
     * @param cellGroup the new cell group
     */
    public void lowLevelSetCellGroup(CellGroup cellGroup)
	{
        checkChanging();
		if (cellGroup == this.cellGroup) return;
		String protoName = getName();
		for (Iterator<Cell> it = getViewsTail(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			if (!cell.getName().equals(protoName)) break;
			cell.cellGroup.remove(cell);
			cellGroup.add(cell);
		}
    }

	/****************************** VIEWS ******************************/

	/**
	 * Method to get this Cell's View.
	 * Views include "layout", "schematics", "icon", "netlist", etc.
	 * @return to get this Cell's View.
	 */
	public View getView() { return cellName.getView(); }

	/**
	 * Method to change the view of this Cell.
	 * @param newView the new View.
	 */
	public void setView(View newView)
	{
		rename(CellName.newName(getName(), newView, getVersion()));
	}

	/**
	 * Method to determine whether this Cell is an icon Cell.
	 * @return true if this Cell is an icon  Cell.
	 */
	public boolean isIcon() { return getView() == View.ICON; }

	/**
	 * Method to determine whether this Cell is an icon of another Cell.
	 * @param cell the other cell which this may be an icon of.
	 * @return true if this Cell is an icon of that other Cell.
	 */
	public boolean isIconOf(Cell cell)
	{
		return getView() == View.ICON && cellGroup == cell.cellGroup && cell.isSchematic();
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
		return false;
	}

	/**
	 * Method to return the number of pages in this multi-page Cell.
	 * @return the number of different pages.
	 */
	public int getNumMultiPages()
	{
		if (!isMultiPage()) return 1;
		Rectangle2D bounds = getBounds();
		int numPages = (int)(bounds.getHeight() / FrameDescription.MULTIPAGESEPARATION) + 1;
		Variable var = getVar(MULTIPAGE_COUNT_KEY, Integer.class);
		if (var != null)
		{
			Integer storedCount = (Integer)var.getObject();
			if (storedCount.intValue() > numPages) numPages = storedCount.intValue();
		}
		return numPages;
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
		for(Iterator<Cell> it = getCellGroup().getCells(); it.hasNext(); )
		{
			Cell cellInGroup = (Cell)it.next();
			if (cellInGroup.isSchematic()) return cellInGroup;
		}

		// now check to see if there is any layout link
		for(Iterator<Cell> it = getCellGroup().getCells(); it.hasNext(); )
		{
			Cell cellInGroup = (Cell)it.next();
			if (cellInGroup.getView() == View.LAYOUT) return cellInGroup;
		}

		// finally check to see if there is any "unknown" link
		for(Iterator<Cell> it = getCellGroup().getCells(); it.hasNext(); )
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
		for(Iterator<Cell> it = getCellGroup().getCells(); it.hasNext(); )
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
		for(Iterator<Cell> it = getCellGroup().getCells(); it.hasNext(); )
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
		HashSet<Cell> cellsChecked = new HashSet<Cell>();
		checkCellDate(getRevisionDate(), cellsChecked);
	}

	/**
	 * Recursive method to check sub-cell revision times.
	 * @param rev_time the revision date of the top-level cell.
	 * Nothing below it can be newer.
	 */
	private void checkCellDate(Date rev_time, HashSet<Cell> cellsChecked)
	{
		for(Iterator<NodeInst> it = getNodes(); it.hasNext(); )
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
		System.out.println("WARNING: sub-cell " + this +
			" has been edited since the last revision to the current cell");
	}

	/****************************** MISCELLANEOUS ******************************/

	/**
	 * Method to set this Cell so that instances of it are "expanded" by when created.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 */
	public void setWantExpanded() { checkChanging(); userBits |= WANTNEXPAND;  Undo.otherChange(this); }

	/**
	 * Method to set this Cell so that instances of it are "not expanded" by when created.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 */
	public void clearWantExpanded() { checkChanging(); userBits &= ~WANTNEXPAND; Undo.otherChange(this); }

	/**
	 * Method to tell if instances of it are "expanded" by when created.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * @return true if instances of it are "expanded" by when created.
	 */
	public boolean isWantExpanded() { return (userBits & WANTNEXPAND) != 0 || isIcon(); }

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
	public void setAllLocked() { checkChanging(); userBits |= NPLOCKED; Undo.otherChange(this); }

	/**
	 * Method to set this Cell so that everything inside of it is not locked.
	 * Locked instances cannot be moved or deleted.
	 */
	public void clearAllLocked() { checkChanging(); userBits &= ~NPLOCKED; Undo.otherChange(this); }

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
	public void setInstancesLocked() { checkChanging(); userBits |= NPILOCKED; Undo.otherChange(this); }

	/**
	 * Method to set this Cell so that all instances inside of it are not locked.
	 * Locked instances cannot be moved or deleted.
	 */
	public void clearInstancesLocked() { checkChanging(); userBits &= ~NPILOCKED; Undo.otherChange(this); }

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
	public void setInCellLibrary() { checkChanging(); userBits |= INCELLLIBRARY; Undo.otherChange(this); }

	/**
	 * Method to set this Cell so that it is not part of a cell library.
	 * Cell libraries are simply libraries that contain standard cells but no hierarchy
	 * (as opposed to libraries that define a complete circuit).
	 * Certain commands exclude facets from cell libraries, so that the actual circuit hierarchy can be more clearly seen.
	 */
	public void clearInCellLibrary() { checkChanging(); userBits &= ~INCELLLIBRARY; Undo.otherChange(this); }

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
	public void setInTechnologyLibrary() { checkChanging(); userBits |= TECEDITCELL; Undo.otherChange(this); }

	/**
	 * Method to set this Cell so that it is not part of a technology library.
	 * Technology libraries are those libraries that contain Cells with
	 * graphical descriptions of the nodes, arcs, and layers of a technology.
	 */
	public void clearInTechnologyLibrary() { checkChanging(); userBits &= ~TECEDITCELL; Undo.otherChange(this); }

	/**
	 * Method to tell if this Cell is part of a Technology Library.
	 * Technology libraries are those libraries that contain Cells with
	 * graphical descriptions of the nodes, arcs, and layers of a technology.
	 * @return true if this Cell is part of a Technology Library.
	 */
	public boolean isInTechnologyLibrary() { return (userBits & TECEDITCELL) != 0; }

    /**
	 * Method to set if cell has been modified since last save to disk. No need to call checkChanging().
     * -1 means no changes, 0 minor changes and 1 major changes
	 */
	public void setModified(boolean majorChange)
    {
        if (majorChange) modified = 1;
        else
        {
            // only set it if modified != 1
            if (modified != 1) modified = 0;
        }
    }

	/**
	 * Method to clear this Cell modified bit since last save to disk. No need to call checkChanging().
     * This is done when the library contained this cell is saved to disk.
	 */
	public void clearModified() { modified = -1; }

	/**
	 * Method to tell if this Cell has been modified since last save to disk.
	 * @return true if cell has been modified.
	 */
	public boolean isModified(boolean majorChange)
    {
        if (majorChange) return modified == 1;
        // only minor change
        return modified == 0;
    }

    /**
	 * Method to set if cell has been modified in the batch job.
	 */
	public void setContentsModified() {
        contentsModified = true;
    }

    /**
     * Method to load isExpanded status of subcell instances from Preferences.
     */    
    public void loadExpandStatus() {
        String cellName = noLibDescribe().replace('/', ':');
        String cellKey = "E" + cellName;
        boolean useWantExpanded = false, mostExpanded = false;
        if (lib.prefs.get(cellKey, null) == null)
            useWantExpanded = true;
        else
            mostExpanded = lib.prefs.getBoolean(cellKey, false);
        Preferences cellPrefs = null;
        try {
            if (lib.prefs.nodeExists(cellName))
                cellPrefs = lib.prefs.node(cellName);
        } catch (BackingStoreException e) {
            ActivityLogger.logException(e);
        }
        for (Iterator<NodeInst> it = getNodes(); it.hasNext(); ) {
            NodeInst ni = (NodeInst)it.next();
            if (!(ni.getProto() instanceof Cell)) continue;
            boolean expanded = useWantExpanded ? ((Cell)ni.getProto()).isWantExpanded() : mostExpanded;
            if (cellPrefs != null) {
                String nodeName = "E" + ni.getName();
                expanded = cellPrefs.getBoolean(nodeName, expanded);
            }
            ni.setExpanded(expanded);
//            if (ni.isIconOfParent() || ((Cell)ni.getProto()).isIcon()) ni.setExpanded(true); // forcing it
//            else
//            {
//                boolean expanded = useWantExpanded ? ((Cell)ni.getProto()).isWantExpanded() : mostExpanded;
//                if (cellPrefs != null) {
//                    String nodeName = ni.getDuplicate() == 0 ? "E" + ni.getName() : "E\"" + ni.getName() + "\"" + ni.getDuplicate();
//                    expanded = cellPrefs.getBoolean(nodeName, expanded);
//                }
//                if (expanded) ni.setExpanded(); else ni.clearExpanded();
//            }
        }
        expandStatusModified = false;
    }
    
    /**
     * Method to save isExpanded status of subcell instances to Preferences.
     */
    void saveExpandStatus() throws BackingStoreException {
        if (!expandStatusModified) return;
        if (Job.getDebug()) System.err.println("Save expanded status of " + this);
        int num = 0, expanded = 0, diff = 0;
        for (Iterator<NodeInst> it = getNodes(); it.hasNext(); ) {
            NodeInst ni = (NodeInst)it.next();
            if (!(ni.getProto() instanceof Cell)) continue;
            num++;
            if (ni.isExpanded()) expanded++;
            if (ni.isExpanded() != ((Cell)ni.getProto()).isWantExpanded())
                diff++;
        }
        String cellName = noLibDescribe().replace('/', ':');
        String cellKey = "E" + cellName;
        boolean useWantExpanded = false, mostExpanded = false;
        if (diff <= expanded && diff <= num - expanded) {
            useWantExpanded = true;
            lib.prefs.remove(cellKey);
        } else {
            if (num - expanded < expanded) {
                diff = num - expanded;
                mostExpanded = true;
            } else {
                diff = expanded;
            }
            lib.prefs.putBoolean(cellKey, mostExpanded);
        }
        if (diff == 0) {
            if (lib.prefs.nodeExists(cellName)) {
                lib.prefs.node(cellName).removeNode();
            }
        } else {
            Preferences cellPrefs = lib.prefs.node(cellName);
            cellPrefs.clear();
            cellPrefs.put("CELL", cellName);
            for (Iterator<NodeInst> it = getNodes(); it.hasNext(); ) {
                NodeInst ni = (NodeInst)it.next();
                if (!(ni.getProto() instanceof Cell)) continue;
                boolean defaultExpanded = useWantExpanded ? ((Cell)ni.getProto()).isWantExpanded() : mostExpanded;
                if (ni.isExpanded() != defaultExpanded) {
                    String nodeName = "E" + ni.getName();
                    cellPrefs.putBoolean(nodeName, ni.isExpanded());
                }
            }
            cellPrefs.flush();
        }
        expandStatusModified = false;
    }
    
    /**
     * Method to tell that expanded status of subcell instances was modified.
     */
    public void expandStatusChanged() {
        expandStatusModified = true;
    }
    
	/**
	 * Method to set the multi-page capability of this Cell.
	 * Multipage cells (usually schematics) must have cell frames to isolate the different
	 * areas of the cell that are different pages.
	 * @param multi true to make this cell multi-page.
	 */
	public void setMultiPage(boolean multi)
	{
		checkChanging();
		if (multi) userBits |= MULTIPAGE; else
			userBits &= ~MULTIPAGE;
	}

	/**
	 * Method to tell if this Cell is a multi-page drawing.
	 * Multipage cells (usually schematics) must have cell frames to isolate the different
	 * areas of the cell that are different pages.
	 * @return true if this Cell is a multi-page drawing.
	 */
	public boolean isMultiPage() { return (userBits & MULTIPAGE) != 0; }
	
    /**
     * Returns true if this Cell is linked into database.
     * @return true if this Cell is linked into database.
     */
	public boolean isLinked()
	{
        return inCurrentThread(d.cellId) == this;
	}

	/**
	 * Method to check and repair data structure errors in this Cell.
	 */
	public int checkAndRepair(boolean repair, ErrorLogger errorLogger)
	{
		int errorCount = 0;
        List<Geometric> list = new ArrayList<Geometric>();

		for(Iterator<ArcInst> it = getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
            errorCount += ai.checkAndRepair(repair, list, errorLogger);
		}
		for(Iterator<NodeInst> it = getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			errorCount += ni.checkAndRepair(repair, list, errorLogger);
		}
        if (repair && list.size() > 0)
            CircuitChanges.eraseObjectsInList(this, list);
		return errorCount;
	}

	/**
	 * Method to check invariants in this Cell.
	 * @exception AssertionError if invariants are not valid
	 */
	protected void check()
	{
        super.check();
        CellId cellId = getD().cellId;
		assert linkedCells.get(cellId.cellIndex) == this;
		assert cellName != null;
		assert getVersion() > 0;

		for (int i = 0; i < exports.length; i++)
		{
			Export e = exports[i];
			assert e.getParent() == this;
			assert e.getPortIndex() == i : e;
			if (i > 0)
				assert(TextUtils.STRING_NUMBER_ORDER.compare(exports[i - 1].getName(), e.getName()) < 0) : i;
			e.check();
            assert e == chronExports[e.getExportId().chronIndex]; 
		}
        for (int i = 0; i < chronExports.length; i++) {
            Export e = chronExports[i];
            if (e == null) continue;
            assert e.isLinked();
        }

		// make sure that every connection is on an arc and a node
//		HashSet connections = new HashSet();
		ArcInst prevAi = null;
		for(int i = 0; i < arcs.size(); i++)
		{
			ArcInst ai = (ArcInst)arcs.get(i);
			assert ai.getParent() == this;
            assert chronArcs.get(ai.getD().arcId) == ai;
			assert ai.getArcIndex() == i;
			if (prevAi != null)
			{
				int cmp = TextUtils.STRING_NUMBER_ORDER.compare(prevAi.getName(), ai.getName());
				assert cmp <= 0;
				if (cmp == 0)
					assert prevAi.getD().arcId < ai.getD().arcId;
			}
			ai.check();
			prevAi = ai;
		}
        int countArcs = 0;
        for (int i = 0; i < chronArcs.size(); i++)
            if (chronArcs.get(i) != null) countArcs++;
        assert countArcs == arcs.size();

        // now make sure that all nodes reference them
		NodeInst prevNi = null;
        int[] usages = new int[cellId.numUsagesIn()];
		for(int i = 0; i < nodes.size(); i++)
		{
			NodeInst ni = (NodeInst)nodes.get(i);
			assert ni.getParent() == this;
            assert chronNodes.get(ni.getD().nodeId) == ni;
			assert ni.getNodeIndex() == i;
			if (prevNi != null)
			{
				int cmp = TextUtils.STRING_NUMBER_ORDER.compare(prevNi.getName(), ni.getName());
				assert cmp <= 0;
				if (cmp == 0)
					assert prevNi.getD().nodeId < ni.getD().nodeId;
			}
            if (ni.getProto() instanceof Cell) {
                CellUsage u = cellId.getUsageIn(((Cell)ni.getProto()).d.cellId);
                usages[u.indexInParent]++;
            }
			ni.check();
//			for(Iterator<Connection> pIt = ni.getConnections(); pIt.hasNext(); )
//			{
//				Connection con = (Connection)pIt.next();
//                ArcInst ai = con.getArc();
//                assert ai.getParent() == this && ai.isLinked();
//			}
			prevNi = ni;
		}
        int countNodes = 0;
        for (int i = 0; i < chronNodes.size(); i++)
            if (chronNodes.get(i) != null) countNodes++;
        assert countNodes == nodes.size();

		// check node usages
        for (int i = 0; i < cellUsages.length; i++)
            assert cellUsages[i] == usages[i];
        for (int i = cellUsages.length; i < usages.length; i++)
            assert usages[i] == 0;

		// check group pointers
		assert cellGroup != null;
		assert cellGroup.containsCell(this);
	}

	private static boolean invariantsFailed = false;

	/**
	 * Method to check invariants in this Cell.
	 * @return true if invariants are valid
	 */
	public boolean checkInvariants()
	{
		try
		{
			check();
			return true;
		} catch (Throwable e)
		{
			if (!invariantsFailed)
			{
				System.out.println("Exception checking invariants of " + this);
				e.printStackTrace();
				ActivityLogger.logException(e);
				invariantsFailed = true;
			}
		}
		return false;
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
			for(Iterator<NodeInst> it = getNodes(); it.hasNext(); )
				if ((ElectricObject)it.next() == eObj) return true;
		} else if (eObj instanceof ArcInst)
		{
			for(Iterator<ArcInst> it = getArcs(); it.hasNext(); )
				if ((ElectricObject)it.next() == eObj) return true;
		} else if (eObj instanceof PortInst)
		{
			NodeInst ni = ((PortInst)eObj).getNodeInst();
			for(Iterator<NodeInst> it = getNodes(); it.hasNext(); )
				if ((ElectricObject)it.next() == ni) return true;
		}
		return false;
	}

	/**
	 * Method to get the 0-based index of this Cell.
	 * @return the index of this Cell.
	 */
	public final int getCellIndex() { return d.cellId.cellIndex; }

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
        checkChanging();
        this.tech = tech;
        Undo.otherChange(this);
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
			return nodes.contains(thing);
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
        Set<Object> noCheckAgain = new HashSet<Object>();
        for (Iterator<NodeInst> it = getNodes(); it.hasNext(); )
        {
            boolean found = false;
            NodeInst node = (NodeInst)it .next();

            for (Iterator<NodeInst> i = toCompare.getNodes(); i.hasNext();)
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
        for (Iterator<ArcInst> it = getArcs(); it.hasNext(); )
        {
            boolean found = false;
            ArcInst arc = (ArcInst)it.next();

            for (Iterator<ArcInst> i = toCompare.getArcs(); i.hasNext();)
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
        for (Iterator<Export> it = getExports(); it.hasNext(); )
        {
            boolean found = false;
            Export port = (Export)it.next();

            for (Iterator<Export> i = toCompare.getExports(); i.hasNext();)
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
        for (Iterator<Variable> it = getVariables(); it.hasNext(); )
        {
            Variable var = (Variable)it.next();
            boolean found = false;

            for (Iterator<Variable> i = toCompare.getVariables(); i.hasNext();)
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
     * Compares Cells by their Libraries and CellNames.
     * @param that the other Cell.
     * @return a comparison between the Cells.
     */
	public int compareTo(Cell that)
	{
		if (this.lib != that.lib)
		{
			int cmp = this.lib.compareTo(that.lib);
			if (cmp != 0) return cmp;
		}
		return this.getCellName().compareTo(that.getCellName());
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
        for (int i = 0; i < arcs.size(); i++)
        {
            ArcInst ai = (ArcInst)arcs.get(i);
            ArcProto ap = ai.getProto();
            ap.getZValues(array);
        }
	}

	/**
	 * Method to fill a set with any nodes in this Cell that refer to an external library.
	 * @param elib the external library being considered.
	 * @param set the set being filled.
	 * @return true if anything was added to the set.
	 */
	public boolean findReferenceInCell(Library elib, Set<Cell> set)
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
