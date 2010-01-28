/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellChangeJobs.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user;

import com.sun.electric.database.IdMapper;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for Jobs that make changes to the cells.
 */
public class CellChangeJobs
{
	// constructor, never used
	private CellChangeJobs() {}

	/****************************** DELETE A CELL ******************************/

	/**
	 * Class to delete a cell in a new thread.
	 */
	public static class DeleteCell extends Job
	{
		Cell cell;

		public DeleteCell(Cell cell)
		{
			super("Delete " + cell, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// check cell usage once more
			if (cell.isInUse("delete", false, true)) return false;
			cell.kill();
			return true;
		}
	}

	/**
	 * This class implement the command to delete a list of cells.
	 */
	public static class DeleteManyCells extends Job
	{
		private List<Cell> cellsToDelete;

		public DeleteManyCells(List<Cell> cellsToDelete)
		{
			super("Delete Multiple Cells", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cellsToDelete = cellsToDelete;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// iteratively delete, allowing cells in use to be deferred
			boolean didDelete = true;
			while (didDelete)
			{
				didDelete = false;
				for (int i=0; i<cellsToDelete.size(); i++)
				{
					Cell cell = cellsToDelete.get(i);

					// if the cell is in use, defer
					if (cell.isInUse(null, true, true)) continue;

					// cell not in use: remove it from the list and delete it
					cellsToDelete.remove(i);
					i--;
					System.out.println("Deleting " + cell);
					cell.kill();
					didDelete = true;
				}
			}

			// warn about remaining cells that were in use
			for(Cell cell : cellsToDelete)
				cell.isInUse("delete", false, true);
			return true;
		}

		public void terminateOK()
		{
			System.out.println("Deleted " + cellsToDelete.size() + " cells");
			EditWindow.repaintAll();
		}
	}

	/****************************** RENAME CELLS ******************************/

	/**
	 * Class to rename a cell in a new thread.
	 */
	public static class RenameCell extends Job
	{
		private Cell cell;
		private String newName;
		private String newGroupCell;
		private IdMapper idMapper;

		public RenameCell(Cell cell, String newName, String newGroupCell)
		{
			super("Rename " + cell, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.newName = newName;
			this.newGroupCell = newGroupCell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			idMapper = cell.rename(newName, newGroupCell);
			fieldVariableChanged("idMapper");
			return true;
		}

		public void terminateOK()
		{
			User.fixStaleCellReferences(idMapper);
		}
	}

	/**
	 * Class to rename a cell in a new thread.
	 */
	public static class DeleteCellGroup extends Job
	{
		List<Cell> cells;

		public DeleteCellGroup(Cell.CellGroup group)
		{
			super("Delete Cell Group", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			cells = new ArrayList<Cell>();

			for(Iterator<Cell> it = group.getCells(); it.hasNext(); )
			{
				cells.add(it.next());
			}
			startJob();
		}

		public boolean doIt() throws JobException
		{
			for(Cell cell : cells)
			{
				// Doesn't check cells in the same group
				// check cell usage once more
				if (cell.isInUse("delete", false, false))
					return false;
			}
			// Now real delete
			for(Cell cell : cells)
			{
				cell.kill();
			}
			return true;
		}
	}

	/**
	 * Class to rename a cell in a new thread.
	 */
	public static class RenameCellGroup extends Job
	{
		Cell cellInGroup;
		String newName;

		public RenameCellGroup(Cell cellInGroup, String newName)
		{
			super("Rename Cell Group", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cellInGroup = cellInGroup;
			this.newName = newName;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// see if all cells in the group have the same name
			boolean allSameName = true;
			String lastName = null;
			for(Iterator<Cell> it = cellInGroup.getCellGroup().getCells(); it.hasNext(); )
			{
				String cellName = it.next().getName();
				if (lastName != null && !lastName.equals(cellName))
				{
					allSameName = false;
					break;
				}
				lastName = cellName;
			}

			List<Cell> cells = new ArrayList<Cell>();
			for(Iterator<Cell> it = cellInGroup.getCellGroup().getCells(); it.hasNext(); )
				cells.add(it.next());
			String newGroupCell = null;
			for(Cell cell : cells)
			{
				if (allSameName)
				{
					cell.rename(newName, newName);
				} else
				{
					if (newGroupCell == null)
					{
						System.out.println("Renaming is not possible because cells in group don't have same root name.");
						System.out.println("'" + newName + "' was added as prefix.");
						newGroupCell = newName + cell.getName();
					}
					cell.rename(newName+cell.getName(), newGroupCell);
				}
			}
			return true;
		}
	}

	/****************************** SHOW CELLS GRAPHICALLY ******************************/

	/**
	 * This class implement the command to make a graph of the cells.
	 */
	public static class GraphCells extends Job
	{
		private static final double TEXTHEIGHT = 2;

		private Cell top;
		private Cell graphCell;

		private static class GraphNode
		{
			String    name;
			int       depth;
			int       clock;
			double    x, y;
			double    yoff;
			NodeInst  pin;
			NodeInst  topPin;
			NodeInst  botPin;
			GraphNode main;
		}

		public GraphCells(Cell top)
		{
			super("Graph Cells", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.top = top;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// create the graph cell
			graphCell = Cell.newInstance(Library.getCurrent(), "CellStructure");
			fieldVariableChanged("graphCell");
			if (graphCell == null) return false;
			if (graphCell.getNumVersions() > 1)
				System.out.println("Creating new version of cell: " + graphCell.getName()); else
					System.out.println("Creating cell: " + graphCell.getName());

			// create GraphNodes for every cell and initialize the depth to -1
			Map<Cell,GraphNode> graphNodes = new HashMap<Cell,GraphNode>();
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				if (lib.isHidden()) continue;
				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = cIt.next();
					GraphNode cgn = new GraphNode();
					cgn.name = cell.describe(false);
					cgn.depth = -1;
					graphNodes.put(cell, cgn);
				}
			}

			// find all top-level cells
			int maxDepth = 0;
			if (top != null)
			{
				GraphNode cgn = graphNodes.get(top);
				cgn.depth = 0;
			} else
			{
				for(Iterator<Cell> cIt = Library.getCurrent().getCells(); cIt.hasNext(); )
				{
					Cell cell = cIt.next();
					if (cell.getNumUsagesIn() == 0)
					{
						GraphNode cgn = graphNodes.get(cell);
						cgn.depth = 0;
					}
				}
			}

			double xScale = 2.0 / 3.0;
			double yScale = 20;
			double yOffset = TEXTHEIGHT * 1.25;
			double maxWidth = 0;

			// now place all cells at their proper depth
			boolean more = true;
			while (more)
			{
				more = false;
				for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = it.next();
					if (lib.isHidden()) continue;
					for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = cIt.next();
						GraphNode cgn = graphNodes.get(cell);
						if (cgn.depth == -1) continue;
						for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
						{
							NodeInst ni = nIt.next();
							if (!ni.isCellInstance()) continue;

							// ignore recursive references (showing icon in contents)
							if (ni.isIconOfParent()) continue;

							Cell sub = (Cell)ni.getProto();
							GraphNode subCgn = graphNodes.get(sub);
							if (subCgn.depth <= cgn.depth)
							{
								subCgn.depth = cgn.depth + 1;
								if (subCgn.depth > maxDepth) maxDepth = subCgn.depth;
								more = true;
							}
							Cell trueCell = sub.contentsView();
							if (trueCell == null) continue;
							GraphNode trueCgn = graphNodes.get(trueCell);
							if (trueCgn.depth <= cgn.depth)
							{
								trueCgn.depth = cgn.depth + 1;
								if (trueCgn.depth > maxDepth) maxDepth = trueCgn.depth;
								more = true;
							}
						}
					}
				}

				// add in any cells referenced from other libraries
				if (!more && top == null)
				{
					for(Iterator<Cell> cIt = Library.getCurrent().getCells(); cIt.hasNext(); )
					{
						Cell cell = cIt.next();
						GraphNode cgn = graphNodes.get(cell);
						if (cgn.depth >= 0) continue;
						cgn.depth = 0;
						more = true;
					}
				}
			}

			// now assign X coordinates to each graph node
			maxDepth++;
			double [] xval = new double[maxDepth];
			double [] yoff = new double[maxDepth];
			for(int i=0; i<maxDepth; i++) xval[i] = yoff[i] = 0;
			for(Cell cell : graphNodes.keySet())
			{
				GraphNode cgn = graphNodes.get(cell);

				// ignore icon cells from the graph (merge with contents)
				if (cgn.depth == -1) continue;

				cgn.x = xval[cgn.depth];
				xval[cgn.depth] += cgn.name.length();
				if (xval[cgn.depth] > maxWidth) maxWidth = xval[cgn.depth];
				cgn.y = cgn.depth;
				cgn.yoff = 0;
			}

			// now center each row
			for(Cell cell : graphNodes.keySet())
			{
				GraphNode cgn = graphNodes.get(cell);
				if (cgn.depth == -1) continue;
				if (xval[(int)cgn.y] < maxWidth)
				{
					double spread = maxWidth / xval[(int)cgn.y];
					cgn.x = cgn.x * spread;
				}
			}

			// generate accurate X/Y coordinates
			for(Cell cell : graphNodes.keySet())
			{
				GraphNode cgn = graphNodes.get(cell);
				if (cgn.depth == -1) continue;
				double x = cgn.x;   double y = cgn.y;
				x = x * xScale;
				y = -y * yScale + ((yoff[(int)cgn.y]++)%3) * yOffset;
				cgn.x = x;   cgn.y = y;
			}

			// make unattached cells sit with their contents view
			if (top == null)
			{
				for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = it.next();
					if (lib.isHidden()) continue;
					for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = cIt.next();
						GraphNode cgn = graphNodes.get(cell);
						if (cgn.depth != -1) continue;

						if (cell.getNumUsagesIn() != 0 && !cell.isIcon() &&
							cell.getView() != View.LAYOUTSKEL) continue;
						Cell trueCell = graphMainView(cell);
						if (trueCell == null) continue;
						GraphNode trueCgn = graphNodes.get(trueCell);
						if (trueCgn.depth == -1) continue;

						cgn.pin = cgn.topPin = cgn.botPin = null;
						cgn.main = trueCgn;
						cgn.yoff += yOffset*2;
						cgn.x = trueCgn.x;
						cgn.y = trueCgn.y + trueCgn.yoff;
					}
				}
			}

			// write the header message
			double xsc = maxWidth * xScale / 2;
			NodeInst titleNi = NodeInst.newInstance(Generic.tech().invisiblePinNode, new Point2D.Double(xsc, yScale), 0, 0, graphCell);
			if (titleNi == null) return false;
			String msg;
			if (top != null) msg = "Structure below " + top; else
				msg = "Structure of library " + Library.getCurrent().getName();
			TextDescriptor td = TextDescriptor.getNodeTextDescriptor().withRelSize(6);
			titleNi.newVar(Artwork.ART_MESSAGE, msg, td);

			// place the components
			for(Cell cell : graphNodes.keySet())
			{
				GraphNode cgn = graphNodes.get(cell);
				if (cgn.depth == -1) continue;

				double x = cgn.x;   double y = cgn.y;
				cgn.pin = NodeInst.newInstance(Generic.tech().invisiblePinNode, new Point2D.Double(x, y), 0, 0, graphCell);
				if (cgn.pin == null) return false;
				cgn.topPin = NodeInst.newInstance(Generic.tech().invisiblePinNode, new Point2D.Double(x, y+TEXTHEIGHT/2), 0, 0, graphCell);
				if (cgn.topPin == null) return false;
				cgn.botPin = NodeInst.newInstance(Generic.tech().invisiblePinNode, new Point2D.Double(x, y-TEXTHEIGHT/2), 0, 0, graphCell);
				if (cgn.botPin == null) return false;
				PortInst pinPi = cgn.pin.getOnlyPortInst();
				PortInst toppinPi = cgn.botPin.getOnlyPortInst();
				PortInst botPinPi = cgn.topPin.getOnlyPortInst();
				ArcInst link1 = ArcInst.makeInstanceBase(Generic.tech().invisible_arc, 0, toppinPi, pinPi);
				ArcInst link2 = ArcInst.makeInstanceBase(Generic.tech().invisible_arc, 0, pinPi, botPinPi);
				link1.setRigid(true);
				link2.setRigid(true);
				link1.setHardSelect(true);
				link2.setHardSelect(true);
				cgn.topPin.setHardSelect();
				cgn.botPin.setHardSelect();

				// write the cell name in the node
				TextDescriptor ctd = TextDescriptor.getNodeTextDescriptor().withRelSize(2);
				cgn.pin.newVar(Artwork.ART_MESSAGE, cgn.name, ctd);
			}

			// attach related components with rigid arcs
			for(Cell cell : graphNodes.keySet())
			{
				GraphNode cgn = graphNodes.get(cell);
				if (cgn.depth == -1) continue;
				if (cgn.main == null) continue;

				PortInst firstPi = cgn.pin.getOnlyPortInst();
				ArcInst ai = ArcInst.makeInstanceBase(Artwork.tech().solidArc, 0, firstPi, firstPi);
				if (ai == null) return false;
				ai.setRigid(true);
				ai.setHardSelect(true);

				// set an invisible color on the arc
				ai.newVar(Artwork.ART_COLOR, new Integer(0));
			}

			// build wires between the hierarchical levels
			int clock = 0;
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				if (lib.isHidden()) continue;
				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = cIt.next();
					if (cell == graphCell) continue;

					// always use the contents cell, not the icon
					Cell trueCell = cell.contentsView();
					if (trueCell == null) trueCell = cell;
					GraphNode trueCgn = graphNodes.get(trueCell);
					if (trueCgn.depth == -1) continue;

					clock++;
					for(Iterator<NodeInst> nIt = trueCell.getNodes(); nIt.hasNext(); )
					{
						NodeInst ni = nIt.next();
						if (!ni.isCellInstance()) continue;

						// ignore recursive references (showing icon in contents)
						if (ni.isIconOfParent()) continue;
						Cell sub = (Cell)ni.getProto();

						Cell truesubnp = sub.contentsView();
						if (truesubnp == null) truesubnp = sub;

						GraphNode trueSubCgn = graphNodes.get(truesubnp);
						if (trueSubCgn.clock == clock) continue;
						trueSubCgn.clock = clock;

						// draw a line from cell "trueCell" to cell "truesubnp"
						if (trueSubCgn.depth == -1) continue;
						PortInst toppinPi = trueCgn.botPin.getOnlyPortInst();
						PortInst niBotPi = trueSubCgn.topPin.getOnlyPortInst();
						ArcInst ai = ArcInst.makeInstance(Artwork.tech().solidArc, toppinPi, niBotPi);
						if (ai == null) return false;
						ai.setRigid(false);
						ai.setFixedAngle(false);
						ai.setSlidable(false);
						ai.setHardSelect(true);

						// set an appropriate color on the arc (red for jumps of more than 1 level of depth)
						int color = EGraphics.BLUE;
						if (trueCgn.y - trueSubCgn.y > yScale+yOffset+yOffset) color = EGraphics.RED;
						ai.newVar(Artwork.ART_COLOR, new Integer(color));
					}
				}
			}
			return true;
		}

		public void terminateOK()
		{
			// display the graph cell
			UserInterface ui = Job.getUserInterface();
			ui.displayCell(graphCell);
		}

		/**
		 * Method to find the main cell that "cell" is associated with in the graph.  This code is
		 * essentially the same as "contentscell()" except that any original type is allowed.
		 * @return null if the cell is not associated.
		 */
		private Cell graphMainView(Cell cell)
		{
			// first check to see if there is a schematics link
			Cell mainSchem = cell.getCellGroup().getMainSchematics();
			if (mainSchem != null) return mainSchem;

			// now check to see if there is any layout link
			for(Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); )
			{
				Cell cellInGroup = it.next();
				if (cellInGroup.getView() == View.LAYOUT) return cellInGroup;
			}

			// finally check to see if there is any "unknown" link
			for(Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); )
			{
				Cell cellInGroup = it.next();
				if (cellInGroup.getView() == View.UNKNOWN) return cellInGroup;
			}

			// no contents found
			return null;
		}
	}

	/****************************** SHOW LIBRARIES GRAPHICALLY ******************************/

	/**
	 * This class implement the command to make a graph of the libraries.
	 */
	public static class GraphLibraries extends Job
	{
		private Cell graphCell;

		private static class GraphNode
		{
			String   name;
			boolean  topLibrary;
			boolean  leafLibrary;
			double   x, y;
			NodeInst pin;
		}

		private static class GraphArc
		{
			GraphNode from;
			GraphNode to;
			boolean   doubleHeaded;
		}

		public GraphLibraries()
		{
			super("Graph Libraries", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// create the graph cell
			graphCell = Cell.newInstance(Library.getCurrent(), "LibraryStructure");
			fieldVariableChanged("graphCell");
			if (graphCell == null) return false;
			if (graphCell.getNumVersions() > 1)
				System.out.println("Creating new version of cell: " + graphCell.getName()); else
					System.out.println("Creating cell: " + graphCell.getName());

			// create GraphNodes for every library
			Map<Library,GraphNode> graphNodes = new HashMap<Library,GraphNode>();
			Map<Library,Set<Library>> libraryDependencies = new HashMap<Library,Set<Library>>();
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				if (lib.isHidden()) continue;
				GraphNode cgn = new GraphNode();
				cgn.name = lib.getName();
				cgn.leafLibrary = true;
				cgn.topLibrary = true;
				graphNodes.put(lib, cgn);
				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = cIt.next();
					for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
					{
						NodeInst ni = nIt.next();
						if (!ni.isCellInstance()) continue;

						// ignore recursive references (showing icon in contents)
						if (ni.isIconOfParent()) continue;

						Library subLib = ((Cell)ni.getProto()).getLibrary();
						if (subLib == lib) continue;
						Set<Library> subLibs = libraryDependencies.get(lib);
						if (subLibs == null) libraryDependencies.put(lib, subLibs = new HashSet<Library>());
						subLibs.add(subLib);
						cgn.leafLibrary = false;
					}				
				}
			}

			// compute top libraries
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				GraphNode cgn = graphNodes.get(lib);
				if (cgn == null) continue;
				Set<Library> subLibs = libraryDependencies.get(lib);
				if (subLibs == null) continue;
				for(Library subLib : subLibs)
				{
					GraphNode subCGN = graphNodes.get(subLib);
					if (subCGN != null) subCGN.topLibrary = false;
				}
			}

			double radius = 50;

			// count the number of each class of library
			int numCentral = 0, numTop = 0, numLeaf = 0;
			for(Library lib : graphNodes.keySet())
			{
				GraphNode cgn = graphNodes.get(lib);
				if (cgn.topLibrary) numTop++; else
					if (cgn.leafLibrary) numLeaf++; else
						numCentral++;
			}

			// arrange the top and leaf libraries outside, the others in a circle
			double curAngle = 0, curTop = 0, curLeaf = 0;
			for(Library lib : graphNodes.keySet())
			{
				GraphNode cgn = graphNodes.get(lib);
				if (cgn.topLibrary)
				{
					cgn.x = -radius + (radius*2/numTop*curTop) + (radius*2/(numTop+1));
					cgn.y = radius * 1.25;
					curTop++;
				} else if (cgn.leafLibrary)
				{
					cgn.x = -radius + (radius*2/numLeaf*curLeaf) + (radius*2/(numLeaf+1));
					cgn.y = -radius * 1.25;
					curLeaf++;
				} else
				{
					cgn.x = Math.cos(curAngle) * radius;
					cgn.y = Math.sin(curAngle) * radius;
					curAngle += Math.PI * 2 / numCentral;
				}
			}

			// write the header message
			NodeInst titleNi = NodeInst.newInstance(Generic.tech().invisiblePinNode, new Point2D.Double(0, radius*1.5), 0, 0, graphCell);
			if (titleNi == null) return false;
			TextDescriptor td = TextDescriptor.getNodeTextDescriptor().withRelSize(6);
			titleNi.newVar(Artwork.ART_MESSAGE, "Structure of library dependencies", td);

			// make a list of all arcs in the graph
			List<GraphArc> allArcs = new ArrayList<GraphArc>();
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				if (lib.isHidden()) continue;
				GraphNode trueCgn = graphNodes.get(lib);

				Set<Library> subLibs = libraryDependencies.get(lib);
				if (subLibs == null) continue;
				for(Library subLib : subLibs)
				{
					GraphNode trueSubCgn = graphNodes.get(subLib);
					boolean found = false;
					for(GraphArc ga : allArcs)
					{
						if (ga.from == trueSubCgn && ga.to == trueCgn)
						{
							found = true;
							ga.doubleHeaded = true;
							break;
						}
					}
					if (found) continue;
					GraphArc ga = new GraphArc();
					ga.from = trueCgn;
					ga.to = trueSubCgn;
					ga.doubleHeaded = false;
					allArcs.add(ga);
				}
			}

			// place the components
			for(Library lib : graphNodes.keySet())
			{
				GraphNode cgn = graphNodes.get(lib);

				double x = cgn.x;   double y = cgn.y;
				cgn.pin = NodeInst.newInstance(Generic.tech().invisiblePinNode, new Point2D.Double(x, y), 0, 0, graphCell);
				if (cgn.pin == null) return false;

				// write the cell name in the node
				TextDescriptor ctd = TextDescriptor.getNodeTextDescriptor().withRelSize(2);
				if (!cgn.leafLibrary && !cgn.topLibrary)
				{
					if (x > Math.abs(y))
					{
						// on the right
						ctd = ctd.withPos(TextDescriptor.Position.UPRIGHT);
					} else if (x < -Math.abs(y))
					{
						// on the left
						ctd = ctd.withPos(TextDescriptor.Position.UPLEFT);
					} else if (y > Math.abs(x))
					{
						// on the top
						ctd = ctd.withPos(TextDescriptor.Position.UPRIGHT).withRotation(TextDescriptor.Rotation.getRotation(90));
					} else
					{
						// on the bottom
						ctd = ctd.withPos(TextDescriptor.Position.UPRIGHT).withRotation(TextDescriptor.Rotation.getRotation(270));
					}
				}
				cgn.pin.setName(cgn.name);
				cgn.pin.setTextDescriptor(NodeInst.NODE_NAME, ctd);
			}

			// build wires between the hierarchical levels
			for(GraphArc ga : allArcs)
			{
				GraphNode trueCgn = ga.from;
				GraphNode trueSubCgn = ga.to;

				// draw a line from cell "trueCell" to cell "truesubnp"
				PortInst toppinPi = trueCgn.pin.getOnlyPortInst();
				PortInst niBotPi = trueSubCgn.pin.getOnlyPortInst();
				ArcInst ai = ArcInst.makeInstance(Artwork.tech().solidArc, toppinPi, niBotPi);
				if (ai == null) return false;
				ai.setRigid(false);
				ai.setFixedAngle(false);
				ai.setSlidable(false);

				// set an appropriate color on the arc
				int color = EGraphics.RED;
				if (ga.doubleHeaded) color = EGraphics.LRED;
				if (trueCgn.topLibrary) color = EGraphics.BLUE; else
					if (trueSubCgn.leafLibrary) color = EGraphics.GREEN;
				ai.newVar(Artwork.ART_COLOR, new Integer(color));
				String msg = trueCgn.name + "-USES-" + trueSubCgn.name;
				if (ga.doubleHeaded) msg = trueCgn.name + "-CO-DEPENDS-ON-" + trueSubCgn.name;
				ai.setName(msg);
				TextDescriptor atd = TextDescriptor.getArcTextDescriptor().withDisplay(false);
				ai.setTextDescriptor(ArcInst.ARC_NAME, atd);
			}
			return true;
		}

		public void terminateOK()
		{
			// display the graph cell
			UserInterface ui = Job.getUserInterface();
			ui.displayCell(graphCell);
		}
	}

	/****************************** EXTRACT CELL INSTANCES ******************************/

	/**
	 * This class implement the command to delete unused old versions of cells.
	 */
	public static class PackageCell extends Job
	{
		Cell curCell;
		Set<Geometric> whatToPackage;
		String newCellName;
        private Set<NodeInst> expandedNodes = new HashSet<NodeInst>();
        private IconParameters iconParameters = IconParameters.makeInstance(true);

        public PackageCell(Cell curCell, Set<Geometric> whatToPackage, String newCellName)
		{
			super("Package Cell", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.curCell = curCell;
			this.whatToPackage = whatToPackage;
			this.newCellName = newCellName;
            startJob();
		}

        @Override
		public boolean doIt() throws JobException
		{
			// create the new cell
			Cell cell = Cell.makeInstance(Library.getCurrent(), newCellName);
			if (cell == null) return false;

			// copy the nodes into the new cell
			Map<NodeInst,NodeInst> newNodes = new HashMap<NodeInst,NodeInst>();
			for(Geometric look : whatToPackage)
			{
				if (!(look instanceof NodeInst)) continue;
				NodeInst ni = (NodeInst)look;

				String name = null;
				Name oldName = ni.getNameKey();
				if (!oldName.isTempname()) name = oldName.toString();
				NodeInst newNi = NodeInst.makeInstance(ni.getProto(), new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY()),
					ni.getXSize(), ni.getYSize(), cell, ni.getOrient(), name);
				if (newNi == null) return false;
				newNodes.put(ni, newNi);
				newNi.copyStateBits(ni);
                if(ni.isExpanded())
                    expandedNodes.add(newNi);
				newNi.copyVarsFrom(ni);
				newNi.copyTextDescriptorFrom(ni, NodeInst.NODE_NAME);

				// make ports where this nodeinst has them
				for(Iterator<Export> it = ni.getExports(); it.hasNext(); )
				{
					Export pp = it.next();
					PortInst pi = newNi.findPortInstFromProto(pp.getOriginalPort().getPortProto());
					Export newPp = Export.newInstance(cell, pi, pp.getName(), pp.getCharacteristic(), iconParameters);
					if (newPp != null)
					{
						newPp.copyTextDescriptorFrom(pp, Export.EXPORT_NAME);
						newPp.copyVarsFrom(pp);
					}
				}
			}

			// copy the arcs into the new cell
			for(Geometric look : whatToPackage)
			{
				if (!(look instanceof ArcInst)) continue;
				ArcInst ai = (ArcInst)look;
				NodeInst niTail = newNodes.get(ai.getTailPortInst().getNodeInst());
				NodeInst niHead = newNodes.get(ai.getHeadPortInst().getNodeInst());
				if (niTail == null || niHead == null) continue;
				PortInst piTail = niTail.findPortInstFromProto(ai.getTailPortInst().getPortProto());
				PortInst piHead = niHead.findPortInstFromProto(ai.getHeadPortInst().getPortProto());

				String name = null;
				Name oldName = ai.getNameKey();
				if (!oldName.isTempname()) name = oldName.toString();
				ArcInst newAi = ArcInst.makeInstanceBase(ai.getProto(), ai.getLambdaBaseWidth(), piHead, piTail, ai.getHeadLocation(),
					ai.getTailLocation(), name);
				if (newAi == null) return false;
				newAi.copyPropertiesFrom(ai);
			}
			System.out.println("Cell " + cell.describe(true) + " created");
			return true;
		}

        @Override
        public void terminateOK() {
            for (NodeInst ni: expandedNodes)
                ni.setExpanded(true);
        }
	}

	/**
	 * This class implement the command to extract the contents of cell instances.
	 */
	public static class ExtractCellInstances extends Job
	{
		private Cell cell;
		private List<NodeInst> nodes;
		private boolean copyExports;
		private boolean fromRight;
		private int depth;
        private Set<NodeInst> expandedNodes = new HashSet<NodeInst>();
        private boolean startNow;

        public ExtractCellInstances(Cell cell, List<NodeInst> highlighted, int depth, boolean copyExports,
			boolean fromRight, boolean startNow)
		{
			super("Extract Cell Instances", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.nodes = highlighted;
			this.copyExports = copyExports;
			this.fromRight = fromRight;
			this.depth = depth;
            this.startNow = startNow;
            if (!startNow)
				startJob();
			else
			{
				try {doIt(); } catch (Exception e) {e.printStackTrace();}
			}
		}

        @Override
		public boolean doIt() throws JobException
		{
			doArbitraryExtraction(cell, expandedNodes, nodes, copyExports, depth, fromRight);
            if (!startNow)
                fieldVariableChanged("expandedNodes");
			return true;
		}

        @Override
        public void terminateOK() {
            for (NodeInst ni: expandedNodes)
                ni.setExpanded(true);
        }
	}

	private static void doArbitraryExtraction(Cell cell, Set<NodeInst> expandedNodes, List<NodeInst> nodes, boolean copyExports, int depth, boolean fromRight)
	{
		Job.getUserInterface().startProgressDialog("Extracting " + nodes.size() + " cells", null);
		Map<NodeInst,Map<PortInst,PortInst>> newNodes = new HashMap<NodeInst,Map<PortInst,PortInst>>();
		int done = 0;
		Set<NodeInst> nodesToKill = new HashSet<NodeInst>();
		List<Export> exportsToCopy = new ArrayList<Export>();
		for(NodeInst ni : nodes)
		{
			if (!ni.isCellInstance()) continue;
			Map<PortInst,PortInst> portMap = new HashMap<PortInst,PortInst>();
			extractOneLevel(cell, expandedNodes, ni, GenMath.MATID, portMap, 1, depth, fromRight);

			newNodes.put(ni, portMap);
			for (Iterator<Export> it = ni.getExports(); it.hasNext(); )
				exportsToCopy.add(it.next());
			done++;
			Job.getUserInterface().setProgressValue(done * 100 / nodes.size());
			nodesToKill.add(ni);
		}

		// replace arcs to the cell and exports on the cell
		Job.getUserInterface().setProgressNote("Replacing top-level arcs and exports");
		replaceExtractedArcs(cell, cell, newNodes, GenMath.MATID, fromRight);

		// replace the exports if needed
		if (copyExports)
		{
			for(Export pp : exportsToCopy)
			{
				PortInst oldPi = pp.getOriginalPort();
				Map<PortInst,PortInst> nodePortMap = newNodes.get(oldPi.getNodeInst());
				if (nodePortMap == null) continue;
				PortInst newPi = nodePortMap.get(oldPi);
				if (newPi == null)
				{
					pp.kill();
					continue;
				}
				pp.move(newPi);
			}
		}

		// delete original nodes
		cell.killNodes(nodesToKill);
		Job.getUserInterface().stopProgressDialog();
	}

	private static void extractOneLevel(Cell cell, Set<NodeInst> expandedNodes, NodeInst topno, AffineTransform prevTrans,
		Map<PortInst,PortInst> portMap, int curDepth, int totDepth, boolean fromRight)
	{
		Map<NodeInst,Map<PortInst,PortInst>> newNodes = new HashMap<NodeInst,Map<PortInst,PortInst>>();

		// see if there are already Essential Bounds nodes in the top cell
		boolean hasEssentialBounds = false;
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			NodeProto np = ni.getProto();
			if (np == Generic.tech().essentialBoundsNode) { hasEssentialBounds = true;   break; }
		}

		// make transformation matrix for this cell
		Cell subCell = (Cell)topno.getProto();
		AffineTransform localTrans = topno.translateOut(topno.rotateOut());
		localTrans.preConcatenate(prevTrans);

		for(Iterator<NodeInst> it = subCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			Map<PortInst,PortInst> subPortMap = new HashMap<PortInst,PortInst>();
			newNodes.put(ni, subPortMap);

			// do not extract "cell center" primitives
			NodeProto np = ni.getProto();
			if (np == Generic.tech().cellCenterNode) continue;

			// do not extract "essential bounds" primitives if they exist in the top-level cell
			if (np == Generic.tech().essentialBoundsNode && hasEssentialBounds) continue;

			boolean extractCell = false;
			if (ni.isCellInstance() && curDepth < totDepth) extractCell = true;
			if (extractCell)
			{
				extractOneLevel(cell, expandedNodes, ni, localTrans, subPortMap, curDepth+1, totDepth, fromRight);

				// add to the portmap
				for(Iterator<Export> eIt = ni.getExports(); eIt.hasNext(); )
				{
					Export e = eIt.next();
					PortInst fromPi = topno.findPortInstFromProto(e);
					PortInst toPi = subPortMap.get(e.getOriginalPort());
					portMap.put(fromPi, toPi);
				}
			} else
			{
				String name = null;
				if (ni.isUsernamed())
					name = ElectricObject.uniqueObjectName(ni.getName(), cell, NodeInst.class, false, fromRight);
				Orientation orient = topno.getOrient().concatenate(ni.getOrient());
				Point2D pt = new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY());
				AffineTransform instTrans = ni.rotateOut(localTrans);
				instTrans.transform(pt, pt);
				NodeInst newNi = NodeInst.makeInstance(np, pt, ni.getXSize(), ni.getYSize(), cell, orient, name);
				if (newNi == null) continue;
				newNi.copyTextDescriptorFrom(ni, NodeInst.NODE_NAME);
                newNi.copyStateBits(ni);
                if (ni.isExpanded())
                    expandedNodes.add(newNi);
				newNi.copyVarsFrom(ni);

				// add ports to the new node's portmap
				for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
				{
					PortInst oldPi = pIt.next();
					PortInst newPi = newNi.findPortInstFromProto(oldPi.getPortProto());
					subPortMap.put(oldPi, newPi);
				}

				// add exports to the parent portmap
				for(Iterator<Export> eIt = ni.getExports(); eIt.hasNext(); )
				{
					Export e = eIt.next();
					PortInst fromPi = topno.findPortInstFromProto(e);
					PortInst toPi = newNi.findPortInstFromProto(e.getOriginalPort().getPortProto());
					portMap.put(fromPi, toPi);
				}
			}
		}

		replaceExtractedArcs(cell, subCell, newNodes, localTrans, fromRight);
	}

	private static void replaceExtractedArcs(Cell destCell, Cell cell, Map<NodeInst,Map<PortInst,PortInst>> nodeMaps,
		AffineTransform trans, boolean fromRight)
	{
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
			PortInst oldHeadPi = ai.getHeadPortInst();
			NodeInst headNi = oldHeadPi.getNodeInst();
			Map<PortInst,PortInst> headMap = nodeMaps.get(headNi);
			PortInst oldTailPi = ai.getTailPortInst();
			NodeInst tailNi = oldTailPi.getNodeInst();
			Map<PortInst,PortInst> tailMap = nodeMaps.get(tailNi);
			if (headMap == null && tailMap == null) continue;

			PortInst newHeadPi = oldHeadPi;
			if (headMap != null)
			{
				newHeadPi = headMap.get(oldHeadPi);
				if (newHeadPi == null)
				{
					System.out.println("Warning: arc " + ai.describe(false) + " in cell " + cell.describe(false) +
						" is missing head connectivity information");
					continue;
				}
			}

			PortInst newTailPi = oldTailPi;
			if (tailMap != null)
			{
				newTailPi = tailMap.get(oldTailPi);
				if (newTailPi == null)
				{
					System.out.println("Warning: arc " + ai.describe(false) + " in cell " + cell.describe(false) +
						" is missing tail connectivity information");
					continue;
				}
			}

			if (newHeadPi == null || newTailPi == null)
			{
				System.out.println("Warning: cannot reconnect arc in cell " + cell.describe(false) +
					" from " + oldHeadPi + " to " + oldTailPi);
				continue;
			}
			Point2D headLoc = new Point2D.Double(ai.getHeadLocation().getX(), ai.getHeadLocation().getY());
			trans.transform(headLoc, headLoc);
			Point2D tailLoc = new Point2D.Double(ai.getTailLocation().getX(), ai.getTailLocation().getY());
			trans.transform(tailLoc, tailLoc);

			ArcProto ap = ai.getProto();
			String name = null;
			if (ai.isUsernamed())
				name = ElectricObject.uniqueObjectName(ai.getName(), cell, ArcInst.class, false, fromRight);

            ImmutableArcInst a = ai.getD();
            ArcInst newAi = ArcInst.newInstance(destCell, ap, name, a.nameDescriptor,
                newHeadPi, newTailPi, EPoint.snap(headLoc), EPoint.snap(tailLoc), a.getGridExtendOverMin(), a.getAngle(), a.flags);
			if (newAi == null)
			{
				System.out.println("Error: arc " + ai.describe(false) + " in cell " + cell.describe(false) +
					" was not extracted");
				continue;
			}
			newAi.copyPropertiesFrom(ai);
		}
	}

	/****************************** MAKE A NEW VERSION OF A CELL ******************************/

	/**
	 * This class implement the command to make a new version of a cell.
	 */
	public static class NewCellVersion extends Job
	{
		private Cell cell;
		private Cell newVersion;
        private Map<CellId,Cell> newCells = new HashMap<CellId,Cell>();

		public NewCellVersion(Cell cell)
		{
			super("Create new Version of " + cell, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			newVersion = cell.makeNewVersion();
			if (newVersion == null) return false;
			newCells.put(cell.getId(), newVersion);
			fieldVariableChanged("newVersion");
			return true;
		}

		public void terminateOK()
		{
			if (newVersion == null) return;

			// update cell expansion information
            copyExpandedStatus(newCells);

			// change the display of old versions to the new one
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				WindowContent content = wf.getContent();
				if (content == null) continue;
				if (content.getCell() == cell)
					wf.setCellWindow(newVersion, null);
			}

			EditWindow.repaintAll();
			System.out.println("Created new version: "+newVersion+", old version renamed to "+cell);
		}
	}

	/****************************** MAKE A COPY OF A CELL ******************************/

	/**
	 * This class implement the command to duplicate a cell.
	 */
	public static class DuplicateCell extends Job
	{
		private Cell cell;
        private Library destLib;
        private String newName;
		private boolean entireGroup;
		private Cell dupCell;
        private boolean startNow;
        private Map<CellId,Cell> newCells = new HashMap<CellId,Cell>();

        public DuplicateCell(Cell cell, String newName, Library lib, boolean entireGroup, boolean startN)
		{
			super("Duplicate " + cell, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.newName = newName;
            this.destLib = lib;
            this.entireGroup = entireGroup;
            this.startNow = startN;
            if (startNow)
            {
                // mainly due to regressions
                try {doIt(); } catch (Exception e) {e.printStackTrace();}
            }
            else
                startJob();
		}

		public boolean doIt() throws JobException
		{
			String newCellName = newName + cell.getView().getAbbreviationExtension();
			dupCell = Cell.copyNodeProto(cell, destLib, newCellName, false);
			if (dupCell == null) {
				System.out.println("Could not duplicate "+cell);
				return false;
			}
			newCells.put(cell.getId(), dupCell);
            if (!startNow) {
                fieldVariableChanged("newCells");
                fieldVariableChanged("dupCell");
            }

			System.out.println("Duplicated cell "+cell+".  New cell is "+dupCell+".");

			// examine all other cells in the group
			List<Cell> othersInGroup = new ArrayList<Cell>();
			for(Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); ) othersInGroup.add(it.next());
			for(Cell otherCell : othersInGroup)
			{
				if (otherCell == cell) continue;

				// When copy a schematic, we should copy the icon if entireGroup == false
				if (!entireGroup && !(cell.isSchematic() && otherCell.isIcon())) continue;
				Cell copyCell = Cell.copyNodeProto(otherCell, otherCell.getLibrary(),
					newName + otherCell.getView().getAbbreviationExtension(), false);
				if (copyCell == null)
				{
					System.out.println("Could not duplicate cell "+otherCell);
					break;
				}
				newCells.put(otherCell.getId(), copyCell);
				System.out.println("  Also duplicated cell "+otherCell+".  New cell is "+copyCell+".");
			}

			// if icon of cell is present, replace old icon with new icon in new schematics cell
			for(CellId oldCellId : newCells.keySet())
			{
				Cell newCell = newCells.get(oldCellId);
				if (!newCell.isSchematic()) continue;
				List<NodeInst> replaceThese = new ArrayList<NodeInst>();
				for (Iterator<NodeInst> it = newCell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = it.next();
					Cell replaceCell = newCells.get(ni.getProto().getId());
					if (replaceCell != null) replaceThese.add(ni);
				}
				for(NodeInst ni : replaceThese)
				{
					// replace old icon(s) in duplicated cell
					Cell replaceCell = newCells.get(ni.getProto().getId());
					ni.replace(replaceCell, true, true);
				}
			}
			return true;
		}

        @Override
		public void terminateOK()
		{
        	// update cell expansion information
            copyExpandedStatus(newCells);

			// change the display of old cell to the new one
			WindowFrame curWf = WindowFrame.getCurrentWindowFrame();
			if (curWf != null)
			{
				WindowContent content = curWf.getContent();
				if (content != null && content.getCell() == cell)
				{
					curWf.setCellWindow(dupCell, null);
					return;
				}
			}

			// current cell was not duplicated: see if any displayed cell is
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				WindowContent content = wf.getContent();
				if (content != null && content.getCell() == cell)
				{
					curWf.setCellWindow(dupCell, null);
					return;
				}
			}
		}
	}

    /**
     * Copy expanded status in client database after some cells were copied or moved in server database
     * @param newCells map from old to new cells.
     */
    public static void copyExpandedStatus(Map<CellId,Cell> newCells)
    {
        for (Map.Entry<CellId,Cell> e: newCells.entrySet())
        {
            CellId oldCellId = e.getKey();
            Cell newCell = e.getValue();
            Cell oldCell = newCell.getDatabase().getCell(oldCellId);
            if (oldCell == null) continue;

            for (Iterator<NodeInst> it = oldCell.getNodes(); it.hasNext(); )
            {
                NodeInst oldNi = it.next();
                if (!oldNi.isCellInstance()) continue;
                NodeInst newNi = newCell.findNode(oldNi.getName());
                if (newNi == null) continue;
                newNi.setExpanded(oldNi.isExpanded());
            }
        }
    }

	/****************************** COPY CELLS ******************************/

	/**
	 * Method to recursively copy cells between libraries.
	 * @param fromCells the original cells being copied.
	 * @param toLib the destination library to copy the cell.
	 * @param verbose true to display extra information.
	 * @param move true to move instead of copy.
	 * @param allRelatedViews true to copy all related views (schematic cell with layout, etc.)
	 * If false, only schematic/icon relations are copied.
	 * @param copySubCells true to recursively copy sub-cells.  If true, "useExisting" must be true.
	 * @param useExisting true to use any existing cells in the destination library
	 * instead of creating a cross-library reference.  False to copy everything needed.
	 * @return address of a copied cell (null on failure).
	 */
	public static IdMapper copyRecursively(List<Cell> fromCells, Library toLib, boolean verbose, boolean move,
		boolean allRelatedViews, boolean copySubCells, boolean useExisting, Map<CellId,Cell> newCells)
	{
		IdMapper idMapper = new IdMapper();
		Cell.setAllowCircularLibraryDependences(true);
		try {
			Map<String,Map<String,String>> existing = new HashMap<String,Map<String,String>>();
			for(Cell fromCell : fromCells)
			{
				Cell copiedCell = copyRecursively(fromCell, toLib, verbose, move, "", true,
					allRelatedViews, allRelatedViews, copySubCells, useExisting, existing, idMapper, newCells);
				if (copiedCell == null) break;
			}
		} finally {
			Cell.setAllowCircularLibraryDependences(false);
		}
		return idMapper;
	}

	/**
	 * Method to recursively copy cells between libraries.
	 * @param fromCell the original cell being copied.
	 * @param toLib the destination library to copy the cell.
	 * @param verbose true to display extra information.
	 * @param move true to move instead of copy.
	 * @param subDescript a String describing the nature of this copy (empty string initially).
	 * @param schematicRelatedView true to copy a schematic related view.  Typically this is true,
	 * meaning that if copying an icon, also copy the schematic.  If already copying the example icon,
	 * this is set to false so that we don't get into a loop.
	 * @param allRelatedViews true to copy all related views (schematic cell with layout, etc.)
	 * If false, only schematic/icon relations are copied.
	 * @param allRelatedViewsThisLevel true to copy related views for this
	 * level of invocation only (but further recursion will use "allRelatedViews").
	 * @param copySubCells true to recursively copy sub-cells.  If true, "useExisting" must be true.
	 * @param useExisting true to use any existing cells in the destination library
	 * instead of creating a cross-library reference.  False to copy everything needed.
	 * @param existing a map that disambiguates cell names when they clash in different original libraries.
	 * The main key is an old cell name, and the value for that key is a map of library names to new cell names.
	 * So, for example, if libraries "A" and "B" both have a cell called "X", then existing.get("X").get("A") is "X" but
	 * existing.get(X").get("B") is "X_1" which disambiguates the cell names in the destination library.
     * @param idMapper mapper which handles renamed cells
     * @param newCells mapper which handles both copied and renamed cells
	 */
	private static Cell copyRecursively(Cell fromCell, Library toLib, boolean verbose, boolean move, String subDescript,
		boolean schematicRelatedView, boolean allRelatedViews, boolean allRelatedViewsThisLevel, boolean copySubCells,
		boolean useExisting, Map<String,Map<String,String>> existing, IdMapper idMapper, Map<CellId,Cell> newCells)
	{
		// check for sensibility
		if (copySubCells && !useExisting)
			System.out.println("Cross-library copy warning: It makes no sense to copy subcells but not use them");

		// see if the cell is already there
		String toName = fromCell.getName();
		View toView = fromCell.getView();
		Cell copiedCell = inDestLib(fromCell, existing, toLib);
		if (copiedCell != null)
			return copiedCell;

		// copy subcells
		if (copySubCells || fromCell.isSchematic())
		{
			boolean found = true;
			while (found)
			{
				found = false;
				for(Iterator<NodeInst> it = fromCell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = it.next();
					if (!copySubCells && !ni.isIconOfParent()) continue;
					if (!ni.isCellInstance()) continue;
					Cell cell = (Cell)ni.getProto();

					// allow cross-library references to stay
					if (cell.getLibrary() == toLib) continue;

					// see if the cell is already there
					if (inDestLib(cell, existing, toLib) != null) continue;

					// do not copy subcell if it exists already (and was not copied by this operation)
					if (useExisting && !copySubCells)
					{
						if (toLib.findNodeProto(cell.noLibDescribe()) != null) continue;
					}

					// copy subcell if not already there
					boolean doCopySchematicView = true;
					if (ni.isIconOfParent()) doCopySchematicView = false;
					Cell oNp = copyRecursively(cell, toLib, verbose,
						move, "subcell ", doCopySchematicView, allRelatedViews, allRelatedViewsThisLevel,
						copySubCells, useExisting, existing, idMapper, newCells);
					if (oNp == null)
					{
						if (move) System.out.println("Move of sub" + cell + " failed"); else
							System.out.println("Copy of sub" + cell + " failed");
						return null;
					}
					found = true;
					break;
				}
			}
		}

		// see if copying related views
		if (!allRelatedViewsThisLevel)
		{
			// not copying related views: just copy schematic if this was icon
			if (toView == View.ICON && schematicRelatedView /*&& move*/ )
			{
				// now copy the schematics
				boolean found = true;
				while (found)
				{
					found = false;
					assert fromCell.isLinked();
					for(Iterator<Cell> it = fromCell.getCellGroup().getCells(); it.hasNext(); )
					{
						Cell np = it.next();
						if (!np.isSchematic()) continue;

						// see if the cell is already there
						if (inDestLib(np, existing, toLib) != null) continue;

						// copy equivalent view if not already there
						Cell oNp = copyRecursively(np, toLib, verbose,
							move, "schematic view ", true, allRelatedViews, false, copySubCells, useExisting, existing, idMapper, newCells);
						if (oNp == null)
						{
							if (move) System.out.println("Move of schematic view " + np + " failed"); else
								System.out.println("Copy of schematic view " + np + " failed");
							return null;
						}
						found = true;
						break;
					}
					if (!fromCell.isLinked())
						return inDestLib(fromCell, existing, toLib);
				}
			}
		} else
		{
			// first copy the icons
			boolean found = true;
			Cell fromCellWalk = fromCell;
			while (found)
			{
				found = false;
				for(Iterator<Cell> it = fromCellWalk.getCellGroup().getCells(); it.hasNext(); )
				{
					Cell np = it.next();
					if (!np.isIcon()) continue;

					// see if the cell is already there
					if (inDestLib(np, existing, toLib) != null) continue;

					// copy equivalent view if not already there
					Cell oNp = copyRecursively(np, toLib, verbose,
						move, "alternate view ", true, allRelatedViews, false, copySubCells, useExisting, existing, idMapper, newCells);
					if (oNp == null)
					{
						if (move) System.out.println("Move of alternate view " + np + " failed"); else
							System.out.println("Copy of alternate view " + np + " failed");
						return null;
					}
					found = true;
					break;
				}
			}

			// now copy the rest
			found = true;
			while (found)
			{
				found = false;
				for(Iterator<Cell> it = fromCellWalk.getCellGroup().getCells(); it.hasNext(); )
				{
					Cell np = it.next();
					if (np.isIcon()) continue;

					// see if the cell is already there
					if (inDestLib(np, existing, toLib) != null) continue;

					// copy equivalent view if not already there
					Cell oNp = copyRecursively(np, toLib, verbose,
						move, "alternate view ", true, allRelatedViews, false, copySubCells, useExisting, existing, idMapper, newCells);
					if (oNp == null)
					{
						if (move) System.out.println("Move of alternate view " + np + " failed"); else
							System.out.println("Copy of alternate view " + np + " failed");
						return null;
					}
					found = true;
					break;
				}
			}
		}

		// see if the cell is NOW there
		copiedCell = inDestLib(fromCell, existing, toLib);
		if (copiedCell != null) return copiedCell;

		// get the proper cell name to use in the destination library
		Map<String,String> libToNameMap = existing.get(fromCell.getName());
		if (libToNameMap == null)
		{
			libToNameMap = new HashMap<String,String>();
			existing.put(fromCell.getName(), libToNameMap);
		}
		String newName = libToNameMap.get(fromCell.getLibrary().getName());
		if (newName == null)
		{
			for(int i=0; i<1000; i++)
			{
				newName = toName;
				if (i > 0) newName += "_" + i;
				if (!libToNameMap.values().contains(newName)) break;
			}
			libToNameMap.put(fromCell.getLibrary().getName(), newName);
		}
		newName += ";" + fromCell.getVersion();
		if (toView.getAbbreviation().length() > 0)
			newName += toView.getAbbreviationExtension();
		Cell newFromCell = Cell.copyNodeProto(fromCell, toLib, newName, useExisting, existing);
		if (newFromCell == null)
		{
			System.out.println("Copy of " + subDescript + fromCell + " failed");
			return null;
		}

		// Message before the delete!!
		if (verbose)
		{
			if (fromCell.getLibrary() != toLib)
			{
				String msg = "";
				if (move) msg += "Moved "; else
					 msg += "Copied ";
				msg += subDescript + fromCell.libDescribe() + " to " + toLib;
				System.out.println(msg);
			} else
			{
				System.out.println("Copied " + subDescript + newFromCell);
			}
		}

		// if moving, adjust pointers and kill original cell
		if (move)
		{
			// now replace old instances with the moved one
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell np = cIt.next();
					boolean found = true;
					while (found)
					{
						found = false;
						for(Iterator<NodeInst> nIt = np.getNodes(); nIt.hasNext(); )
						{
							NodeInst ni = nIt.next();
							if (ni.getProto() == fromCell)
							{
								NodeInst replacedNi = ni.replace(newFromCell, false, false);
								if (replacedNi == null)
								{
									System.out.println("Error moving " + ni + " in " + np);
									found = false;
								}
								else
									found = true;
								break;
							}
						}
					}
				}
			}
			idMapper.moveCell(fromCell.backup(), newFromCell.getId());
			fromCell.kill();
		}
        if (newCells != null) {
            newCells.put(fromCell.getId(), newFromCell);
        }
		return newFromCell;
	}

	/**
	 * Method to tell whether a cell exists in the destination library.
	 * @param cell the Cell in question.
	 * @param existing a map that disambiguates cell names when they clash in different original libraries.
	 * The main key is an old cell name, and the value for that key is a map of library names to new cell names.
	 * So, for example, if libraries "A" and "B" both have a cell called "X", then existing.get("X").get("A") is "X" but
	 * existing.get(X").get("B") is "X_1" which disambiguates the cell names in the destination library.
	 * @param destLib the destination library being searched.
	 * @return a Cell from the destination library that matches the Cell being searched (null if none).
	 */
	private static Cell inDestLib(Cell cell, Map<String,Map<String,String>> existing, Library destLib)
	{
		Map<String,String> libToNameMap = existing.get(cell.getName());
		if (libToNameMap == null) return null;
		String newCellName = libToNameMap.get(cell.getLibrary().getName());
		if (newCellName == null) return null;
		Cell copiedCell = destLib.findNodeProto(newCellName + ";" + cell.getVersion() + cell.getView().getAbbreviationExtension());
		return copiedCell;
	}

}
