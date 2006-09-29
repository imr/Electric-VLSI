/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ViewChanges.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user;

import com.sun.electric.database.IdMapper;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortOriginal;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.DisplayedText;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.TransistorSize;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.generator.layout.GateLayoutGenerator;
import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

/**
 * Class for view-related changes to the circuit.
 */
public class ViewChanges
{
	// constructor, never used
	ViewChanges() {}

	/****************************** CONVERT OLD-STYLE MULTI-PAGE SCHEMATICS ******************************/

	public static void convertMultiPageViews()
	{
		List<Cell> multiPageCells = new ArrayList<Cell>();
		for(Iterator<Library> lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = lIt.next();
			if (lib.isHidden()) continue;
			for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = cIt.next();
				if (cell.getView().getFullName().startsWith("schematic-page-")) multiPageCells.add(cell);
			}
		}
		if (multiPageCells.size() == 0)
		{
			System.out.println("No old-style multi-page schematics to convert");
			return;
		}
		Collections.sort(multiPageCells/*, new TextUtils.CellsByName()*/);

		new FixOldMultiPageSchematics(multiPageCells, User.getAlignmentToGrid());
	}

	/**
	 * Class to update old-style multi-page schematics in a new thread.
	 */
	private static class FixOldMultiPageSchematics extends Job
	{
		private List<Cell> multiPageCells;
		private double alignment;

		protected FixOldMultiPageSchematics(List<Cell> multiPageCells, double alignment)
		{
			super("Repair old-style Multi-Page Schematics", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.multiPageCells = multiPageCells;
			this.alignment = alignment;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			for(Cell cell : multiPageCells)
			{
				int pageNo = TextUtils.atoi(cell.getView().getFullName().substring(15));
				String destCellName = cell.getName() + "{sch}";
				Cell destCell = cell.getLibrary().findNodeProto(destCellName);
				if (pageNo == 1 || destCell == null)
				{
					destCell = Cell.makeInstance(cell.getLibrary(), destCellName);
					if (destCell == null)
					{
						System.out.println("Unable to create cell " + cell.getLibrary().getName() + ":" + destCellName);
						return false;
					}
					destCell.setMultiPage(true);
					destCell.newVar(User.FRAME_SIZE, "d");
				}

				// copy this page into the multipage cell
				double dY = (pageNo - 1) * 1000;
				List<Geometric> geomList = new ArrayList<Geometric>();
				List<DisplayedText> textList = new ArrayList<DisplayedText>();
				for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
					geomList.add(nIt.next());
				for(Iterator<ArcInst> aIt = cell.getArcs(); aIt.hasNext(); )
					geomList.add(aIt.next());
				Clipboard.copyListToCell(destCell, geomList, textList, null, null, new Point2D.Double(0, dY),
					true, true, alignment, null, null);

				// also copy any variables on the cell
				for(Iterator<Variable> vIt = cell.getVariables(); vIt.hasNext(); )
				{
					Variable var = vIt.next();
					if (!var.isDisplay()) continue;
                    destCell.addVar(var.withOff(var.getXOff(), var.getYOff() + dY));
//					Variable cellVar = destCell.newVar(var.getKey(), var.getObject());
//					if (cellVar != null)
//					{
//						cellVar.setTextDescriptor(var.getTextDescriptor());
//						cellVar.setOff(cellVar.getXOff(), cellVar.getYOff() + dY);
//					}
				}

				// delete the original
				cell.kill();
			}
			return true;
		}
	}

	/****************************** CHANGE A CELL'S VIEW ******************************/

	public static void changeCellView(Cell cell, View newView)
	{
		// stop if already this way
		if (cell.getView() == newView) return;

		// warn if there is already a cell with that view
		for(Iterator<Cell> it = cell.getLibrary().getCells(); it.hasNext(); )
		{
			Cell other = it.next();
			if (other.getView() != newView) continue;
			if (!other.getName().equalsIgnoreCase(cell.getName())) continue;

			// there is another cell with this name and view: warn that it will become old
			int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
				"There is already a cell with that view.  Is it okay to make it an older version, and make this the newest version?");
			if (response != JOptionPane.YES_OPTION) return;
			break;
		}
		new ChangeCellView(cell, newView);
	}

	/**
	 * Class to change a cell's view in a new thread.
	 */
	private static class ChangeCellView extends Job
	{
		private Cell cell;
		private View newView;
        private IdMapper idMapper;

		protected ChangeCellView(Cell cell, View newView)
		{
			super("Change View of " + cell + " to " + newView.getFullName(),
				User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.newView = newView;
			startJob();
		}

		public boolean doIt() throws JobException
		{
            EDatabase database = cell.getDatabase();
			idMapper = cell.setView(newView);
            fieldVariableChanged("idMapper");
            if (idMapper != null) {
                cell = idMapper.get(cell.getId()).inDatabase(database);
    			cell.setTechnology(null);
            }
			return true;
		}

        public void terminateOK()
        {
            User.fixStaleCellReferences(idMapper);
//			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
//			{
//				WindowFrame wf = it.next();
//				if (wf.getContent().getCell() == cell)
//				{
//					wf.getContent().setCell(cell, VarContext.globalContext, null);
//				}
//			}
			EditWindow.repaintAll();
        }
	}

	/****************************** MAKE A SKELETON FOR A CELL ******************************/

	public static void makeSkeletonViewCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;

		// cannot skeletonize text-only views
		if (curCell.getView().isTextView())
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"Cannot skeletonize textual views: only layout",
					"Skeleton creation failed", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// warn if skeletonizing nonlayout views
		if (curCell.getView() != View.UNKNOWN && curCell.getView() != View.LAYOUT)
			System.out.println("Warning: skeletonization only makes sense for layout cells, not " +
				curCell.getView().getFullName());

		new MakeSkeletonView(curCell);
	}

	private static class MakeSkeletonView extends Job
	{
		private Cell curCell;
		private Cell skeletonCell;

		protected MakeSkeletonView(Cell curCell)
		{
			super("Make Skeleton View", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.curCell = curCell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// create the new icon cell
			String skeletonCellName = curCell.getName() + "{lay.sk}";
			skeletonCell = Cell.makeInstance(curCell.getLibrary(), skeletonCellName);
			if (skeletonCell == null)
			{
				throw new JobException("Cannot create Skeleton cell " + skeletonCellName);
			}

			boolean error = skeletonizeCell(curCell, skeletonCell);
			if (error) skeletonCell = null;
			fieldVariableChanged("skeletonCell");

			if (error) return false;
			return true;
		}

        public void terminateOK()
        {
        	if (skeletonCell != null)
        	{
				System.out.println("Cell " + skeletonCell.describe(true) + " created with a skeletal representation of " +
					curCell);
				WindowFrame.createEditWindow(skeletonCell);
        	}
        }
	}

	/**
	 * Method to copy the skeletonized version of one Cell into another.
	 * @param curCell the original Cell to be skeletonized.
	 * @param skeletonCell the destination Cell that gets the skeletonized representation.
	 * @return true on error.
	 */
	public static boolean skeletonizeCell(Cell curCell, Cell skeletonCell)
	{
		// place all exports in the new cell
		HashMap<Export,Export> newPortMap = new HashMap<Export,Export>();
		for(Iterator<PortProto> it = curCell.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();

			// traverse to the bottom of the hierarchy for this Export
			PortOriginal fp = new PortOriginal(pp.getOriginalPort());
			PortInst bottomPort = fp.getBottomPort();
			NodeInst bottomNi = bottomPort.getNodeInst();
			PortProto bottomPp = bottomPort.getPortProto();
			AffineTransform subRot = fp.getTransformToTop();
			Orientation newOrient = fp.getOrientToTop();

			// create this node
			Point2D center = new Point2D.Double(bottomNi.getAnchorCenterX(), bottomNi.getAnchorCenterY());
			subRot.transform(center, center);
			NodeInst newNi = NodeInst.makeInstance(bottomNi.getProto(), center, bottomNi.getXSize(), bottomNi.getYSize(),
				skeletonCell, newOrient, null, 0);
			if (newNi == null)
			{
				System.out.println("Cannot create node in this cell");
				return true;
			}

			// export the port from the node
			PortInst newPi = newNi.findPortInstFromProto(bottomPp);
			Export npp = Export.newInstance(skeletonCell, newPi, pp.getName());
			if (npp == null)
			{
				System.out.println("Could not create port " + pp.getName());
				return true;
			}
			npp.copyTextDescriptorFrom(pp, Export.EXPORT_NAME);
			npp.copyVarsFrom(pp);
			npp.setCharacteristic(pp.getCharacteristic());
			newPortMap.put(pp, npp);
		}

		// connect electrically-equivalent ports
		Netlist netlist = curCell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted skeletonization (network information unavailable).  Please try again");
			return true;
		}

		// map exports in the original cell to networks
		HashMap<Export,Network> netMap = new HashMap<Export,Network>();
		for(Iterator<Export> it = curCell.getExports(); it.hasNext(); )
		{
			Export e = it.next();
			Network net = netlist.getNetwork(e, 0);
			netMap.put(e, net);
		}

		int numPorts = curCell.getNumPorts();
		for(int i=0; i<numPorts; i++)
		{
			Export pp = (Export)curCell.getPort(i);
			Network net = (Network)netMap.get(pp);
			for(int j=i+1; j<numPorts; j++)
			{
				Export oPp = (Export)curCell.getPort(j);
				Network oNet = (Network)netMap.get(oPp);
				if (net != oNet) continue;

				Export newPp = (Export)newPortMap.get(pp);
				Export newOPp = (Export)newPortMap.get(oPp);
				if (newPp == null || newOPp == null) continue;
				ArcProto univ = Generic.tech.universal_arc;
				ArcInst newAI = ArcInst.makeInstance(univ, univ.getDefaultWidth(), newPp.getOriginalPort(), newOPp.getOriginalPort());
				if (newAI == null)
				{
					System.out.println("Could not create connecting arc");
					return true;
				}
				newAI.setFixedAngle(false);
				break;
			}
		}

		// copy the essential-bounds nodes if they exist
		for(Iterator<NodeInst> it = curCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			NodeProto np = ni.getProto();
			if (np != Generic.tech.essentialBoundsNode) continue;
			NodeInst newNi = NodeInst.makeInstance(np, ni.getAnchorCenter(),
				ni.getXSize(), ni.getYSize(), skeletonCell, ni.getOrient(), null, 0);
//			NodeInst newNi = NodeInst.makeInstance(np, ni.getAnchorCenter(),
//				ni.getXSizeWithMirror(), ni.getYSizeWithMirror(), skeletonCell, ni.getAngle(), null, 0);
			if (newNi == null)
			{
				System.out.println("Cannot create node in this cell");
				return true;
			}
			newNi.setHardSelect();
			if (np == Generic.tech.cellCenterNode) newNi.setVisInside();
		}

		// place an outline around the skeleton
		Rectangle2D bounds = curCell.getBounds();
		NodeInst boundNi = NodeInst.makeInstance(Generic.tech.invisiblePinNode,
			new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()), bounds.getWidth(), bounds.getHeight(), skeletonCell);
		if (boundNi == null)
		{
			System.out.println("Cannot create boundary node");
			return true;
		}
		boundNi.setHardSelect();
		return false;
	}

	/****************************** MAKE AN ICON FOR A CELL ******************************/

	public static void makeIconViewCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
		if (!curCell.isSchematic())
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"The current cell must be a schematic in order to generate an icon",
					"Icon creation failed", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// see if the icon already exists and issue a warning if so
		Cell iconCell = curCell.iconView();
		if (iconCell != null)
		{
			int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
				"Warning: Icon " + iconCell.describe(true) + " already exists.  Create a new version?");
			if (response != JOptionPane.YES_OPTION) return;
		}
		double leadLength = User.getIconGenLeadLength();
		double leadSpacing = User.getIconGenLeadSpacing();
		boolean reverseIconExportOrder = User.isIconGenReverseExportOrder();
		boolean drawBody = User.isIconGenDrawBody();
		boolean drawLeads = User.isIconGenDrawLeads();
		boolean placeCellCenter = User.isPlaceCellCenter();
		int exportTech = User.getIconGenExportTech();
		int exportStyle = User.getIconGenExportStyle();
		int exportLocation = User.getIconGenExportLocation();
		int inputSide = User.getIconGenInputSide();
		int outputSide = User.getIconGenOutputSide();
		int bidirSide = User.getIconGenBidirSide();
		int pwrSide = User.getIconGenPowerSide();
		int gndSide = User.getIconGenGroundSide();
		int clkSide = User.getIconGenClockSide();
		new MakeIconView(curCell, User.getAlignmentToGrid(), User.getIconGenInstanceLocation(), leadLength, leadSpacing,
			reverseIconExportOrder, drawBody, drawLeads, placeCellCenter, exportTech, exportStyle, exportLocation,
			inputSide, outputSide, bidirSide, pwrSide, gndSide, clkSide);
	}

	private static class MakeIconView extends Job
	{
		private Cell curCell;
		private double alignment;
		private int exampleLocation;
		private double leadLength, leadSpacing;
		private boolean reverseIconExportOrder, drawBody, drawLeads, placeCellCenter;
		private int exportTech, exportStyle, exportLocation;
		private int inputSide, outputSide, bidirSide, pwrSide, gndSide, clkSide;
		private NodeInst iconNode;

		// get icon style controls
		private MakeIconView(Cell cell, double alignment, int exampleLocation,
			double leadLength, double leadSpacing, boolean reverseIconExportOrder, boolean drawBody, boolean drawLeads, boolean placeCellCenter,
			int exportTech, int exportStyle, int exportLocation,
			int inputSide, int outputSide, int bidirSide, int pwrSide, int gndSide, int clkSide)
		{
			super("Make Icon View", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.curCell = cell;
			this.alignment = alignment;
			this.exampleLocation = exampleLocation;
			this.leadLength = leadLength;
			this.leadSpacing = leadSpacing;
			this.reverseIconExportOrder = reverseIconExportOrder;
			this.drawBody = drawBody;
			this.drawLeads = drawLeads;
			this.placeCellCenter = placeCellCenter;
			this.exportTech = exportTech;
			this.exportStyle = exportStyle;
			this.exportLocation = exportLocation;
			this.inputSide = inputSide;
			this.outputSide = outputSide;
			this.bidirSide = bidirSide;
			this.pwrSide = pwrSide;
			this.gndSide = gndSide;
			this.clkSide = clkSide;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			Library lib = curCell.getLibrary();

			Cell iconCell = makeIconForCell(curCell, leadLength, leadSpacing, reverseIconExportOrder,
				drawBody, drawLeads, placeCellCenter, exportTech, exportStyle, exportLocation,
				inputSide, outputSide, bidirSide, pwrSide, gndSide, clkSide);
			if (iconCell == null) return false;

			// place an icon in the schematic
			Point2D iconPos = new Point2D.Double(0,0);
			Rectangle2D cellBounds = curCell.getBounds();
			Rectangle2D iconBounds = iconCell.getBounds();
			double halfWidth = iconBounds.getWidth() / 2;
			double halfHeight = iconBounds.getHeight() / 2;
			switch (exampleLocation)
			{
				case 0:		// upper-right
					iconPos.setLocation(cellBounds.getMaxX()+halfWidth, cellBounds.getMaxY()+halfHeight);
					break;
				case 1:		// upper-left
					iconPos.setLocation(cellBounds.getMinX()-halfWidth, cellBounds.getMaxY()+halfHeight);
					break;
				case 2:		// lower-right
					iconPos.setLocation(cellBounds.getMaxX()+halfWidth, cellBounds.getMinY()-halfHeight);
					break;
				case 3:		// lower-left
					iconPos.setLocation(cellBounds.getMinX()-halfWidth, cellBounds.getMinY()-halfHeight);
					break;
			}
			DBMath.gridAlign(iconPos, alignment);
			double px = iconCell.getBounds().getWidth();
			double py = iconCell.getBounds().getHeight();
			iconNode = NodeInst.makeInstance(iconCell, iconPos, px, py, curCell);
			fieldVariableChanged("iconNode");
//            ni.addObserver(curCell); // adding observer to notify icons if there are changes in master cell
//            curCell.addObserver(ni);
			return true;
		}

        public void terminateOK()
        {
			if (iconNode != null)
			{
				EditWindow wnd = EditWindow.getCurrent();
				if (wnd != null)
				{
					if (wnd.getCell() == curCell)
					{
						Highlighter highlighter = wnd.getHighlighter();
						highlighter.clear();
						highlighter.addElectricObject(iconNode, curCell);
						highlighter.finished();
					}
				}
			}
        }
	}

	/**
	 * Method to create an icon for a cell.
	 * @param curCell the cell to turn into an icon.
	 * @return the icon cell (null on error).
	 */
	public static Cell makeIconForCell(Cell curCell, double leadLength, double leadSpacing,
		boolean reverseIconExportOrder, boolean drawBody, boolean drawLeads, boolean placeCellCenter,
		int exportTech, int exportStyle, int exportLocation,
		int inputSide, int outputSide, int bidirSide, int pwrSide, int gndSide, int clkSide)
			throws JobException
	{
		// make a sorted list of exports
		List<Export> exportList = new ArrayList<Export>();
		for(Iterator<PortProto> it = curCell.getPorts(); it.hasNext(); )
			exportList.add((Export)it.next());
		if (reverseIconExportOrder)
			Collections.reverse(exportList);

		// create the new icon cell
		String iconCellName = curCell.getName() + "{ic}";
		Cell iconCell = Cell.makeInstance(curCell.getLibrary(), iconCellName);
		if (iconCell == null)
		{
			throw new JobException("Cannot create Icon cell " + iconCellName);
		}
		iconCell.setWantExpanded();

		// determine number of inputs and outputs
		int leftSide = 0, rightSide = 0, bottomSide = 0, topSide = 0;
		HashMap<Export,Integer> portIndex = new HashMap<Export,Integer>();
		for(Export pp : exportList)
		{
			if (pp.isBodyOnly()) continue;
			int index = iconPosition(pp, inputSide, outputSide, bidirSide, pwrSide, gndSide, clkSide);
			switch (index)
			{
				case 0: portIndex.put(pp, new Integer(leftSide++));    break;
				case 1: portIndex.put(pp, new Integer(rightSide++));   break;
				case 2: portIndex.put(pp, new Integer(topSide++));     break;
				case 3: portIndex.put(pp, new Integer(bottomSide++));  break;
			}
		}

		// determine the size of the "black box" core
		double ySize = Math.max(Math.max(leftSide, rightSide), 5) * leadSpacing;
		double xSize = Math.max(Math.max(topSide, bottomSide), 3) * leadSpacing;

		// create the "black box"
		NodeInst bbNi = null;
		if (drawBody)
		{
			bbNi = NodeInst.newInstance(Artwork.tech.openedThickerPolygonNode, new Point2D.Double(0,0), xSize, ySize, iconCell);
			if (bbNi == null) return null;
			EPoint [] boxOutline = new EPoint[5];
			boxOutline[0] = new EPoint(-xSize/2, -ySize/2);
			boxOutline[1] = new EPoint(-xSize/2,  ySize/2);
			boxOutline[2] = new EPoint( xSize/2,  ySize/2);
			boxOutline[3] = new EPoint( xSize/2, -ySize/2);
			boxOutline[4] = new EPoint(-xSize/2, -ySize/2);
			bbNi.newVar(NodeInst.TRACE, boxOutline);

			// put the original cell name on it
			Variable var = bbNi.newDisplayVar(Schematics.SCHEM_FUNCTION, curCell.getName());
		}

		// place pins around the Black Box
		int total = 0;
		for(Export pp : exportList)
		{
			if (pp.isBodyOnly()) continue;
			Integer portPosition = (Integer)portIndex.get(pp);

			// determine location of the port
			int index = iconPosition(pp, inputSide, outputSide, bidirSide, pwrSide, gndSide, clkSide);
			double spacing = leadSpacing;
			double xPos = 0, yPos = 0;
			double xBBPos = 0, yBBPos = 0;
			switch (index)
			{
				case 0:		// left side
					xBBPos = -xSize/2;
					xPos = xBBPos - leadLength;
					if (leftSide*2 < rightSide) spacing = leadSpacing * 2;
					yBBPos = yPos = ySize/2 - ((ySize - (leftSide-1)*spacing) / 2 + portPosition.intValue() * spacing);
					break;
				case 1:		// right side
					xBBPos = xSize/2;
					xPos = xBBPos + leadLength;
					if (rightSide*2 < leftSide) spacing = leadSpacing * 2;
					yBBPos = yPos = ySize/2 - ((ySize - (rightSide-1)*spacing) / 2 + portPosition.intValue() * spacing);
					break;
				case 2:		// top
					if (topSide*2 < bottomSide) spacing = leadSpacing * 2;
					xBBPos = xPos = xSize/2 - ((xSize - (topSide-1)*spacing) / 2 + portPosition.intValue() * spacing);
					yBBPos = ySize/2;
					yPos = yBBPos + leadLength;
					break;
				case 3:		// bottom
					if (bottomSide*2 < topSide) spacing = leadSpacing * 2;
					xBBPos = xPos = xSize/2 - ((xSize - (bottomSide-1)*spacing) / 2 + portPosition.intValue() * spacing);
					yBBPos = -ySize/2;
					yPos = yBBPos - leadLength;
					break;
			}

			if (makeIconExport(pp, index, xPos, yPos, xBBPos, yBBPos, iconCell,
				exportTech, drawLeads, exportStyle, exportLocation))
					total++;
		}

		// if no body, leads, or cell center is drawn, and there is only 1 export, add more
		if (!drawBody && !drawLeads && placeCellCenter && total <= 1)
		{
			NodeInst.newInstance(Generic.tech.invisiblePinNode, new Point2D.Double(0,0), xSize, ySize, iconCell);
		}

		return iconCell;
	}

	/**
	 * Helper method to create an export in icon "np".  The export is from original port "pp",
	 * is on side "index" (0: left, 1: right, 2: top, 3: bottom), is at (xPos,yPos), and
	 * connects to the central box at (xBBPos,yBBPos).  Returns TRUE if the export is created.
	 * It uses icon style "style".
	 */
	public static boolean makeIconExport(Export pp, int index,
		double xPos, double yPos, double xBBPos, double yBBPos, Cell np,
		int exportTech, boolean drawLeads, int exportStyle, int exportLocation)
	{
		// presume "universal" exports (Generic technology)
		NodeProto pinType = Generic.tech.universalPinNode;
		double pinSizeX = 0, pinSizeY = 0;
		if (exportTech != 0)
		{
			// instead, use "schematic" exports (Schematic Bus Pins)
			pinType = Schematics.tech.busPinNode;
			pinSizeX = pinType.getDefWidth();
			pinSizeY = pinType.getDefHeight();
		}

		// determine the type of wires used for leads
		ArcProto wireType = Schematics.tech.wire_arc;
		if (pp.getBasePort().connectsTo(Schematics.tech.bus_arc) && pp.getNameKey().isBus())
		{
			wireType = Schematics.tech.bus_arc;
			pinType = Schematics.tech.busPinNode;
			pinSizeX = pinType.getDefWidth();
			pinSizeY = pinType.getDefHeight();
		}

		// if the export is on the body (no leads) then move it in
		if (!drawLeads)
		{
			xPos = xBBPos;   yPos = yBBPos;
		}

		// make the pin with the port
		NodeInst pinNi = NodeInst.newInstance(pinType, new Point2D.Double(xPos, yPos), pinSizeX, pinSizeY, np);
		if (pinNi == null) return false;

		// export the port that should be on this pin
		PortInst pi = pinNi.getOnlyPortInst();
		Export port = Export.newInstance(np, pi, pp.getName());
		if (port != null)
		{
			TextDescriptor td = port.getTextDescriptor(Export.EXPORT_NAME);
			switch (exportStyle)
			{
				case 0:		// Centered
					td = td.withPos(TextDescriptor.Position.CENT);
					break;
				case 1:		// Inward
					switch (index)
					{
						case 0: td = td.withPos(TextDescriptor.Position.RIGHT);  break;	// left
						case 1: td = td.withPos(TextDescriptor.Position.LEFT);   break;	// right
						case 2: td = td.withPos(TextDescriptor.Position.DOWN);   break;	// top
						case 3: td = td.withPos(TextDescriptor.Position.UP);     break;	// bottom
					}
					break;
				case 2:		// Outward
					switch (index)
					{
						case 0: td = td.withPos(TextDescriptor.Position.LEFT);   break;	// left
						case 1: td = td.withPos(TextDescriptor.Position.RIGHT);  break;	// right
						case 2: td = td.withPos(TextDescriptor.Position.UP);     break;	// top
						case 3: td= td.withPos(TextDescriptor.Position.DOWN);   break;	// bottom
					}
					break;
			}
			port.setTextDescriptor(Export.EXPORT_NAME, td);
			double xOffset = 0, yOffset = 0;
			int loc = exportLocation;
			if (!drawLeads) loc = 0;
			switch (loc)
			{
				case 0:		// port on body
					xOffset = xBBPos - xPos;   yOffset = yBBPos - yPos;
					break;
				case 1:		// port on lead end
					break;
				case 2:		// port on lead middle
					xOffset = (xPos+xBBPos) / 2 - xPos;
					yOffset = (yPos+yBBPos) / 2 - yPos;
					break;
			}
			port.setOff(Export.EXPORT_NAME, xOffset, yOffset);
			port.setAlwaysDrawn(pp.isAlwaysDrawn());
			port.setCharacteristic(pp.getCharacteristic());
			port.copyVarsFrom(pp);
		}

		// add lead if requested
		if (drawLeads)
		{
			pinType = wireType.findPinProto();
			if (pinType == Schematics.tech.busPinNode)
				pinType = Generic.tech.invisiblePinNode;
			double wid = pinType.getDefWidth();
			double hei = pinType.getDefHeight();
			NodeInst ni = NodeInst.newInstance(pinType, new Point2D.Double(xBBPos, yBBPos), wid, hei, np);
			if (ni != null)
			{
				PortInst head = ni.getOnlyPortInst();
				PortInst tail = pinNi.getOnlyPortInst();
				ArcInst ai = ArcInst.makeInstance(wireType, wireType.getDefaultWidth(),
					head, tail, new Point2D.Double(xBBPos, yBBPos),
						new Point2D.Double(xPos, yPos), null);
				if (ai != null && wireType == Schematics.tech.bus_arc)
				{
					ai.setHeadExtended(false);
					ai.setTailExtended(false);
				}
			}
		}
		return true;
	}

	/**
	 * Method to determine the side of the icon that port "pp" belongs on.
	 */
	private static int iconPosition(Export pp, int inputSide, int outputSide, int bidirSide, int pwrSide, int gndSide, int clkSide)
	{
		PortCharacteristic character = pp.getCharacteristic();

		// special detection for power and ground ports
		if (pp.isPower()) character = PortCharacteristic.PWR;
		if (pp.isGround()) character = PortCharacteristic.GND;

		// see which side this type of port sits on
		if (character == PortCharacteristic.IN) return inputSide;
		if (character == PortCharacteristic.OUT) return outputSide;
		if (character == PortCharacteristic.BIDIR) return bidirSide;
		if (character == PortCharacteristic.PWR) return pwrSide;
		if (character == PortCharacteristic.GND) return gndSide;
		if (character == PortCharacteristic.CLK || character == PortCharacteristic.C1 ||
			character == PortCharacteristic.C2 || character == PortCharacteristic.C3 ||
			character == PortCharacteristic.C4 || character == PortCharacteristic.C5 ||
			character == PortCharacteristic.C6) return clkSide;
		return inputSide;
	}

	/****************************** CONVERT TO SCHEMATICS ******************************/

	/**
	 * Method to converts the current Cell into a schematic.
	 */
	public static void makeSchematicView()
	{
		Cell oldCell = WindowFrame.needCurCell();
		if (oldCell == null) return;
		MakeSchematicView job = new MakeSchematicView(oldCell);
	}

	private static class MakeSchematicView extends Job
	{
		private Cell oldCell;
		private Cell newCell;

		protected MakeSchematicView(Cell cell)
		{
			super("Make Schematic View", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.oldCell = cell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			newCell = convertSchematicCell(oldCell);
			if (newCell == null) return false;
			fieldVariableChanged("newCell");
			return true;
		}

        public void terminateOK()
        {
        	if (newCell != null)
        	{
    			System.out.println("Cell " + newCell.describe(true) + " created with a schematic representation of " + oldCell);
				WindowFrame.createEditWindow(newCell);
        	}
        }
	}

	private static Cell convertSchematicCell(Cell oldCell)
	{
		// create cell in new technology
		Cell newCell = makeNewCell(oldCell.getName(), View.SCHEMATIC, oldCell);
		if (newCell == null) return null;

		// create the parts in this cell
		HashMap<NodeInst,NodeInst> newNodes = new HashMap<NodeInst,NodeInst>();
		buildSchematicNodes(oldCell, newCell, newNodes);
		buildSchematicArcs(oldCell, newCell, newNodes);

		// now make adjustments for manhattan-ness
//		makeArcsManhattan(newCell);

		// set "fixed-angle" if reasonable
		for(Iterator<ArcInst> it = newCell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
			Point2D headPt = ai.getHeadLocation();
			Point2D tailPt = ai.getTailLocation();
			if (headPt.getX() == tailPt.getX() && headPt.getY() == tailPt.getY()) continue;
			if ((GenMath.figureAngle(headPt, tailPt)%450) == 0) ai.setFixedAngle(true);
		}
		return newCell;
	}

	/**
	 * Method to create a new cell called "newcellname" that is to be the
	 * equivalent to an old cell in "cell".  The view type of the new cell is
	 * in "newcellview" and the view type of the old cell is in "cellview"
	 */
	private static Cell makeNewCell(String newCellName, View newCellView, Cell cell)
	{
		// create the new cell
		String cellName = newCellName;
		if (newCellView.getAbbreviation().length() > 0)
		{
			cellName = newCellName + "{" + newCellView.getAbbreviation() + "}";
		}
		Cell newCell = Cell.makeInstance(cell.getLibrary(), cellName);
		if (newCell == null)
			System.out.println("Could not create cell: " + cellName); else
				System.out.println("Creating new cell: " + cellName);
		return newCell;
	}

	private static void buildSchematicNodes(Cell cell, Cell newCell, HashMap<NodeInst,NodeInst> newNodes)
	{
		// for each node, create a new node in the newcell, of the correct logical type.
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst mosNI = it.next();
			PrimitiveNode.Function type = getNodeType(mosNI);
			NodeInst schemNI = null;
			if (type == PrimitiveNode.Function.UNKNOWN) continue;
			if (type == PrimitiveNode.Function.PIN)
			{
				// compute new x, y coordinates
				NodeProto prim = Schematics.tech.wirePinNode;
				schemNI = makeSchematicNode(prim, mosNI, prim.getDefWidth(), prim.getDefHeight(), 0, 0, newCell);
			} else if (type == null)
			{
				// a cell
				Cell proto = (Cell)mosNI.getProto();
				Cell equivCell = proto.otherView(View.SCHEMATIC);
				if (equivCell == null)
					equivCell = convertSchematicCell(proto);

				schemNI = makeSchematicNode(equivCell, mosNI, equivCell.getDefWidth(), equivCell.getDefHeight(), mosNI.getAngle(), 0, newCell);
			} else
			{
				int rotate = mosNI.getAngle();
				rotate = (rotate + 2700) % 3600;
				NodeProto prim = Schematics.tech.transistorNode;
				int bits = Schematics.getPrimitiveFunctionBits(mosNI.getFunction());
				schemNI = makeSchematicNode(prim, mosNI, prim.getDefWidth(), prim.getDefHeight(), rotate, bits, newCell);

				// add in the size
				TransistorSize ts = mosNI.getTransistorSize(VarContext.globalContext);
				if (ts != null)
				{
					if (mosNI.isFET())
					{
						// set length/width
                        TextDescriptor td = TextDescriptor.getNodeTextDescriptor().withRelSize(0.5).withOff(-0.5, -1);
						schemNI.newVar(Schematics.ATTR_LENGTH, new Double(ts.getDoubleLength()), td);
//						Variable lenVar = schemNI.newDisplayVar(Schematics.ATTR_LENGTH, new Double(ts.getDoubleLength()));
//						if (lenVar != null)
//						{
//							lenVar.setRelSize(0.5);
//							lenVar.setOff(-0.5, -1);
//						}
                        td = TextDescriptor.getNodeTextDescriptor().withRelSize(1).withOff(0.5, -1);
//                        mtd.setRelSize(1);
//                        mtd.setOff(0.5, -1);
                        schemNI.newVar(Schematics.ATTR_WIDTH, new Double(ts.getDoubleWidth()), td);
//						Variable widVar = schemNI.newDisplayVar(Schematics.ATTR_WIDTH, new Double(ts.getDoubleWidth()));
//						if (widVar != null)
//						{
//							widVar.setRelSize(1);
//							widVar.setOff(0.5, -1);
//						}
					} else
					{
						// set area
						schemNI.newVar(Schematics.ATTR_AREA, new Double(ts.getDoubleLength()));
					}
				}
			}

			// store the new node in the old node
			newNodes.put(mosNI, schemNI);

			// reexport ports
			if (schemNI != null)
			{
				for(Iterator<Export> eIt = mosNI.getExports(); eIt.hasNext(); )
				{
					Export mosPP = eIt.next();
					PortInst schemPI = convertPort(mosNI, mosPP.getOriginalPort().getPortProto(), schemNI);
					if (schemPI == null) continue;

					Export schemPP = Export.newInstance(newCell, schemPI, mosPP.getName());
					if (schemPP != null)
					{
						schemPP.setCharacteristic(mosPP.getCharacteristic());
						schemPP.copyTextDescriptorFrom(mosPP, Export.EXPORT_NAME);
						schemPP.copyVarsFrom(mosPP);
					}
				}
			}
		}
	}

	private static NodeInst makeSchematicNode(NodeProto prim, NodeInst orig, double wid, double hei, int angle, int techSpecific, Cell newCell)
	{
		Point2D newLoc = new Point2D.Double(orig.getAnchorCenterX(), orig.getAnchorCenterY());
        Orientation orient = Orientation.fromAngle(angle);
		NodeInst newNI = NodeInst.makeInstance(prim, newLoc, wid, hei, newCell, orient, null, techSpecific);
//		NodeInst newNI = NodeInst.makeInstance(prim, newLoc, wid, hei, newCell, angle, null, techSpecific);
		return newNI;
	}

	/**
	 * for each arc in cell, find the ends in the new technology, and
	 * make a new arc to connect them in the new cell.
	 */
	private static void buildSchematicArcs(Cell cell, Cell newcell, HashMap<NodeInst,NodeInst> newNodes)
	{
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst mosAI = it.next();
			NodeInst mosHeadNI = mosAI.getHeadPortInst().getNodeInst();
			NodeInst mosTailNI = mosAI.getTailPortInst().getNodeInst();
			NodeInst schemHeadNI = (NodeInst)newNodes.get(mosHeadNI);
			NodeInst schemTailNI = (NodeInst)newNodes.get(mosTailNI);
			if (schemHeadNI == null || schemTailNI == null) continue;
			PortInst schemHeadPI = convertPort(mosHeadNI, mosAI.getHeadPortInst().getPortProto(), schemHeadNI);
			PortInst schemTailPI = convertPort(mosTailNI, mosAI.getTailPortInst().getPortProto(), schemTailNI);
			if (schemHeadPI == null || schemTailPI == null) continue;

			// create the new arc
			ArcInst schemAI = ArcInst.makeInstance(Schematics.tech.wire_arc, 0, schemHeadPI, schemTailPI, null, null, mosAI.getName());
			if (schemAI == null) continue;
			schemAI.setFixedAngle(false);
			schemAI.setRigid(false);
		}
	}

	/**
	 * Method to find the logical portproto corresponding to the mos portproto of ni
	 */
	private static PortInst convertPort(NodeInst mosNI, PortProto mosPP, NodeInst schemNI)
	{
		PrimitiveNode.Function fun = getNodeType(schemNI);
		if (fun == PrimitiveNode.Function.PIN)
		{
			return schemNI.getOnlyPortInst();
		}
		if (fun == null)
		{
			// a cell
			PortProto schemPP = schemNI.getProto().findPortProto(mosPP.getName());
			if (schemPP == null) return null;
			return schemNI.findPortInstFromProto(schemPP);
		}

		// a transistor
		int portNum = 1;
		for(Iterator<PortProto> it = mosNI.getProto().getPorts(); it.hasNext(); )
		{
			PortProto pp = it.next();
			if (pp == mosPP) break;
			portNum++;
		}
		if (portNum == 4) portNum = 3; else
			if (portNum == 3) portNum = 1;
		for(Iterator<PortProto> it = schemNI.getProto().getPorts(); it.hasNext(); )
		{
			PortProto schemPP = it.next();
			portNum--;
			if (portNum > 0) continue;
			return schemNI.findPortInstFromProto(schemPP);
		}
		return null;
	}

	private static final int MAXADJUST = 5;

	private static void makeArcsManhattan(Cell newCell)
	{
		// copy the list of nodes in the cell
		List<NodeInst> nodesInCell = new ArrayList<NodeInst>();
		for(Iterator<NodeInst> it = newCell.getNodes(); it.hasNext(); )
			nodesInCell.add(it.next());

		// examine all nodes and adjust them
		double [] x = new double[MAXADJUST];
		double [] y = new double[MAXADJUST];
		for(NodeInst ni : nodesInCell)
		{
			if (ni.isCellInstance()) continue;
			PrimitiveNode.Function fun = ni.getFunction();
			if (fun != PrimitiveNode.Function.PIN) continue;

			// see if this pin can be adjusted so that all wires are manhattan
			int count = 0;
			for(Iterator<Connection> aIt = ni.getConnections(); aIt.hasNext(); )
			{
				Connection con = aIt.next();
				ArcInst ai = con.getArc();
                int otherEnd = 1 - con.getEndIndex();
//				Connection other = ai.getHead();
//				if (ai.getHead() == con) other = ai.getTail();
				if (con.getPortInst().getNodeInst() == ai.getPortInst(otherEnd).getNodeInst()) continue;
				x[count] = ai.getLocation(otherEnd).getX();
				y[count] = ai.getLocation(otherEnd).getY();
				count++;
				if (count >= MAXADJUST) break;
			}
			if (count == 0) continue;

			// now adjust for all these points
			double xp = ni.getAnchorCenterX();
			double yp = ni.getAnchorCenterY();
			double bestDist = Double.MAX_VALUE;
			double bestX = 0, bestY = 0;
			for(int i=0; i<count; i++) for(int j=0; j<count; j++)
			{
				double dist = Math.abs(xp - x[i]) + Math.abs(yp - y[j]);
				if (dist > bestDist) continue;
				bestDist = dist;
				bestX = x[i];   bestY = y[j];
			}

			// if there was a better place, move the node
			if (bestDist != Double.MAX_VALUE)
				ni.move(bestX-xp, bestY-yp);
		}
	}

	/**
	 * Method to figure out if a NodeInst is a MOS component
	 * (a wire or transistor).  If it's a transistor, return its function;
	 * if it's a passive connector, return PrimitiveNode.Function.PIN;
	 * if it's a cell, return null; else return PrimitiveNode.Function.UNKNOWN.
	 */
	private static PrimitiveNode.Function getNodeType(NodeInst ni)
	{
		if (ni.isCellInstance()) return null;
		PrimitiveNode.Function fun = ni.getFunction();
		if (fun.isTransistor()) return fun;
		if (fun == PrimitiveNode.Function.PIN || fun == PrimitiveNode.Function.CONTACT ||
			fun == PrimitiveNode.Function.NODE || fun == PrimitiveNode.Function.CONNECT ||
			fun == PrimitiveNode.Function.SUBSTRATE || fun == PrimitiveNode.Function.WELL)
				return PrimitiveNode.Function.PIN;
		return PrimitiveNode.Function.UNKNOWN;
	}

	/****************************** CONVERT TO ALTERNATE LAYOUT ******************************/

	/**
	 * Method to converts the current Cell into a layout in a given technology.
	 */
	public static void makeLayoutView()
	{
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Cell oldCell = wnd.getCell();
		if (oldCell == null) return;
        VarContext context = wnd.getVarContext();
        if (context == null) context = VarContext.globalContext;

        // find out which technology they want to convert to
		Technology oldTech = oldCell.getTechnology();
		int numTechs = 0;
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			if (tech.isScaleRelevant()) numTechs++;
		}
		String [] techNames = new String[numTechs];
		int i=0;
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			if (tech.isScaleRelevant()) techNames[i++] = tech.getTechName();
		}
		String selectedValue = (String)JOptionPane.showInputDialog(null,
			"New technology to create", "Technology conversion",
			JOptionPane.INFORMATION_MESSAGE, null, techNames, User.getSchematicTechnology().getTechName());
		if (selectedValue == null) return;
		Technology newTech = Technology.findTechnology(selectedValue);
		if (newTech == null) return;
		if (newTech == oldTech)
		{
			System.out.println("Cell " + oldCell.describe(true) + " is already in the " + newTech.getTechName() + " technology");
			return;
		}

		new MakeLayoutView(oldCell, newTech, context);
	}

	private static class MakeLayoutView extends Job
	{
		private Cell oldCell;
		private Technology newTech;
		private Cell newCell;
        private VarContext context;

        protected MakeLayoutView(Cell oldCell, Technology newTech, VarContext context)
		{
			super("Make Alternate Layout", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.oldCell = oldCell;
			this.newTech = newTech;
            this.context = context;
            startJob();
		}

		public boolean doIt() throws JobException
		{
			// convert the cell and all subcells
			Technology oldTech = oldCell.getTechnology();
            MakeLayoutVisitor visitor = new MakeLayoutVisitor(newTech, oldCell.getLibrary());
            HierarchyEnumerator.enumerateCell(oldCell, context, visitor, true);
            newCell = visitor.convertedCells.get(oldCell);
            fieldVariableChanged("newCell");
			return true;
		}

        public void terminateOK()
        {
        	if (newCell != null)
        	{
    			System.out.println("Cell " + newCell.describe(true) + " created with a " + newTech.getTechName() +
    				" layout equivalent of " + oldCell);
				WindowFrame.createEditWindow(newCell);
        	}
        }

        private static class Info extends HierarchyEnumerator.CellInfo {
            private Map<Nodable,Cell> generatedCells;
            public Info() {
                generatedCells = new HashMap<Nodable,Cell>();
            }
        }
        private static class MakeLayoutVisitor extends HierarchyEnumerator.Visitor {
            private Technology newTech;
            private Library defaultLib;
            private Map<Cell,Cell> convertedCells;

            private MakeLayoutVisitor(Technology newTech, Library defaultLib) {
                this.newTech = newTech;
                this.defaultLib = defaultLib;
                convertedCells = new HashMap<Cell,Cell>();
            }

            public HierarchyEnumerator.CellInfo newCellInfo() { return new Info(); }

            private static class Conn {
                private Nodable no;
                private PortProto pp;
                private int index;
                private Name portName;
                private Conn(Nodable no, PortProto pp, int index, Name portName) {
                    this.no = no; this.pp = pp; this.index = index; this.portName = portName;
                }
            }

            public boolean enterCell(HierarchyEnumerator.CellInfo info)
            {
                Cell oldCell = info.getCell();
                VarContext context = info.getContext();
                Info myInfo = (Info)info;
                if (convertedCells.containsKey(oldCell)) return false;

                Tech.Type type = null;
                if      (newTech == Technology.findTechnology("MoCMOS")) type = Tech.Type.MOCMOS;
                else if (newTech == Technology.findTechnology("TSMC180")) type = Tech.Type.TSMC180;
                else if (newTech == Technology.findTechnology("TSMC90")) type = Tech.Type.TSMC90;

                if (type != null) {
                    GateLayoutGenerator gen = new GateLayoutGenerator(type);
                    gen.generateLayout(oldCell.getLibrary(), oldCell, context, type, true);
                    myInfo.generatedCells = gen.getGeneratedCells();
                }
                return true;
            }

            public boolean visitNodeInst(Nodable ni, HierarchyEnumerator.CellInfo info) {
                Cell layCell = getLayoutCell(ni, (Info)info);
                if (layCell == null) return true;
                return false;
            }

            private Cell getLayoutCell(Nodable no, Info myInfo) {
                if (!no.isCellInstance()) return null;
                Cell layCell = null;
                Cell subCell = (Cell)no.getProto();
                layCell = myInfo.generatedCells.get(no);
                if (layCell == null)
                    layCell = convertedCells.get(subCell);
                if (layCell == null) {
                    // see if it already exists
                    String searchCellName = subCell.getName() + "{lay}";
                    layCell = defaultLib.findNodeProto(searchCellName);
                }
                if (layCell != null) {
                    convertedCells.put(subCell, layCell);
                }
                return layCell;
            }

            public void exitCell(HierarchyEnumerator.CellInfo info) {
                Cell oldCell = info.getCell();
                String newCellName = oldCell.getName();
                boolean isSch = oldCell.getView() == View.SCHEMATIC;

                // create the cell
                Cell newCell = makeNewCell(newCellName, View.LAYOUT, oldCell);
                if (newCell == null) return;

                HashMap<Network,List<Conn>> connections = new HashMap<Network,List<Conn>>();
                HashMap<Nodable,NodeInst> convertedNodes = new HashMap<Nodable,NodeInst>();
                //HashMap<NodeInst,Nodable> convertedNodesReverse = new HashMap<NodeInst,Nodable>();

                // create node placement tree and network connections list
                PlacerGrid placer = new PlacerGrid();
                for (Iterator<Nodable> it = info.getNetlist().getNodables(); it.hasNext(); ) {
                    Nodable no = it.next();
                    NodeProto np = null;

                    if (no.isCellInstance()) {
                        np = getLayoutCell(no, (Info)info);
                        if (np == null) {
                            System.out.println("Warning: Unable to find layout version of cell "+no.getProto().describe(false));
                        }
                    } else {
                        np = figureNewNodeProto(no.getNodeInst(), newTech);
                    }
                    if (np != null) {
                        if (!isSch) {
                            // layout Conversion
                            NodeInst newNi = placeLayoutNode(no, np, newCell, info.getContext());
                            convertedNodes.put(no, newNi);
                        } else {

                            if (np.getFunction() == PrimitiveNode.Function.PIN) continue;
                            if (np.getFunction() == PrimitiveNode.Function.CONNECT) continue;

                            placer.insert(new Leaf(no, info.getContext(), np, newCell));

                            // record connections on ports
                            for (Iterator<PortProto> itp = no.getProto().getPorts(); itp.hasNext(); ) {
                                PortProto pp = itp.next();
                                Name ppname = pp.getNameKey();
                                for (int i=0; i<ppname.busWidth(); i++) {
                                    Name subname = ppname.subname(i);
                                    Conn conn = new Conn(no, pp, i, subname);
                                    Network net = info.getNetlist().getNetwork(no, pp, i);
                                    List<Conn> list = connections.get(net);
                                    if (list == null) {
                                        list = new ArrayList<Conn>();
                                        connections.put(net, list);
                                    }
                                    // if port on same node already connects to this network, skip it
                                    boolean add = true;
                                    for (Conn aconn : list) {
                                        if (aconn.no == conn.no) { add = false; break; }
                                    }
                                    if (add) list.add(conn);
                                }
                            }
                        }
                    }
                }

                if (isSch) {
                    // place all new nodes
                    placer.place(this, convertedNodes);

                    // create rats nest of connections
                    ArcProto ratArc = Generic.tech.unrouted_arc;
                    for (Iterator<Network> it = info.getNetlist().getNetworks(); it.hasNext(); ) {
                        Network network = it.next();
                        List<Conn> list = connections.get(network);
                        if (list == null) continue;
                        if (list.size() == 0) continue;
                        Conn conn = list.get(0);
                        PortInst pi = getLayoutPortInst(conn, convertedNodes);
                        if (pi == null) {
                            System.out.println("Cannot find port "+conn.portName+" on "+conn.no);
                            continue;
                        }
                        for (int i=1; i<list.size(); i++) {
                            Conn nextConn = list.get(i);
                            PortInst nextPi = getLayoutPortInst(nextConn, convertedNodes);
                            if (nextPi == null) {
                                System.out.println("Cannot find port "+nextConn.portName+" on "+nextConn.no);
                                continue;
                            }

                            ArcInst newAi = ArcInst.makeInstance(ratArc, 1, pi, nextPi, pi.getCenter(), nextPi.getCenter(), null);
                            if (newAi == null)
                            {
                                System.out.println("Cell " + newCell.describe(true) + ": can't run " + ratArc + " from " +
                                    pi.getNodeInst() + " " + pi + " at (" + pi.getCenter().getX() + "," + pi.getCenter().getY() + ") to " +
                                    nextPi.getNodeInst() + " " + nextPi + " at (" + nextPi.getCenter().getX() + "," + nextPi.getCenter().getY() + ")");
                                continue;
                            }
                            newAi.setFixedAngle(false);
                            newAi.setRigid(false);
                        }
                    }
                    convertedCells.put(oldCell, newCell);
                } else {
                    makeLayoutParts(oldCell, newCell, oldCell.getTechnology(), newTech, View.LAYOUT, convertedCells, convertedNodes);
                }
            }

            private PortInst getLayoutPortInst(Conn schConn, HashMap<Nodable,NodeInst> convertedNodes) {
                NodeInst layNi = convertedNodes.get(schConn.no);
                if (layNi == null) return null;
                PortInst pi = layNi.findPortInst(schConn.portName.toString());
                if (pi != null) return pi;

                // if each has only 1 port, they match
                int numNewPorts = layNi.getProto().getNumPorts();
                if (numNewPorts == 0) return null;
                if (numNewPorts == 1)
                {
                    return layNi.getPortInst(0);
                }

                if (schConn.no.getNodeInst().getFunction().isTransistor()) {
                    if (schConn.portName.toString().equals("g")) return layNi.getPortInst(0);
                    if (schConn.portName.toString().equals("s")) return layNi.getPortInst(1);
                    if (schConn.portName.toString().equals("d")) return layNi.getPortInst(3);
                }

                // associate by position in port list
                for (int i=0; i<schConn.no.getNodeInst().getNumPortInsts() && i<layNi.getNumPortInsts(); i++) {
                    PortInst api = schConn.no.getNodeInst().getPortInst(i);
                    if (api.getPortProto() == schConn.pp) {
                        //System.out.println("Associated port "+i+": "+schConn.no.getProto().getName()+"."+schConn.pp.getName()+" to "+
                        //                    layNi.getProto().getName()+"."+layNi.getProto().getPort(i).getName());
                        return layNi.getPortInst(i);
                    }
                }

                // special case again: one-port capacitors are OK
                PrimitiveNode.Function oldFun = schConn.no.getNodeInst().getFunction();
                PrimitiveNode.Function newFun = layNi.getFunction();
                if (oldFun == PrimitiveNode.Function.CAPAC && newFun == PrimitiveNode.Function.ECAPAC) return layNi.getPortInst(0);

                // association has failed: assume the first port
                System.out.println("No port association between " + schConn.no.getName() + ", "
                    + schConn.pp.getName() + " and " + layNi.getProto());
                return layNi.getPortInst(0);
            }

            /**
             * Method to create a new cell in "newcell" from the contents of an old cell
             * in "oldcell".  The technology for the old cell is "oldtech" and the
             * technology to use for the new cell is "newTech".
             */
            private void makeLayoutParts(Cell oldCell, Cell newCell,
                Technology oldTech, Technology newTech, View nView, Map<Cell,Cell> convertedCells,
                Map<Nodable,NodeInst> convertedNodes)
            {
                // first convert the nodes
                for(Iterator<NodeInst> it = oldCell.getNodes(); it.hasNext(); )
                {
                    NodeInst ni = it.next();
                    // handle sub-cells
                    if (ni.isCellInstance())
                    {
                        Cell newCellType = (Cell)convertedCells.get((Cell)ni.getProto());
                        if (newCellType == null)
                        {
                            System.out.println("No equivalent cell for " + ni.getProto());
                            continue;
                        }
                        placeLayoutNode(ni, newCellType, newCell, VarContext.globalContext);
                        continue;
                    }

                    // handle primitives
                    if (ni.getProto() == Generic.tech.cellCenterNode) continue;
                    NodeProto newNp = figureNewNodeProto(ni, newTech);
                    if (newNp != null)
                        placeLayoutNode(ni, newNp, newCell, VarContext.globalContext);
                }

                /*
                 * for each arc in cell, find the ends in the new technology, and
                 * make a new arc to connect them in the new cell
                 */
                int badArcs = 0;
                for(Iterator<ArcInst> it = oldCell.getArcs(); it.hasNext(); )
                {
                    ArcInst ai = it.next();
                    // get the nodes and ports on the two ends of the arc
                    NodeInst oldHeadNi = ai.getHeadPortInst().getNodeInst();
                    NodeInst oldTailNi = ai.getTailPortInst().getNodeInst();

                    NodeInst newHeadNi = (NodeInst)convertedNodes.get(oldHeadNi);
                    NodeInst newTailNi = (NodeInst)convertedNodes.get(oldTailNi);
                    if (newHeadNi == null || newTailNi == null) continue;
                    PortProto oldHeadPp = ai.getHeadPortInst().getPortProto();
                    PortProto oldTailPp = ai.getTailPortInst().getPortProto();
                    PortProto newHeadPp = convertPortProto(oldHeadNi, newHeadNi, oldHeadPp);
                    PortProto newTailPp = convertPortProto(oldTailNi, newTailNi, oldTailPp);
                    if (newHeadPp == null || newTailPp == null) continue;

                    // compute arc type and see if it is acceptable
                    ArcProto newAp = figureNewArcProto(ai.getProto(), newTech, newHeadPp, newTailPp);

                    // determine new arc width
                    boolean fixAng = ai.isFixedAngle();
                    double newWid = 0;
                    if (newAp == Generic.tech.universal_arc) fixAng = false; else
                    {
                        double defwid = ai.getProto().getDefaultWidth() - ai.getProto().getWidthOffset();
                        double curwid = ai.getWidth() - ai.getProto().getWidthOffset();
                        newWid = (newAp.getDefaultWidth() - newAp.getWidthOffset()) * curwid / defwid + newAp.getWidthOffset();
                        if (!(newWid > 0)) newWid = newAp.getDefaultWidth();
                    }

                    // find the endpoints of the arc
                    Point2D pHead = ai.getHeadLocation();
                    Point2D pTail = ai.getTailLocation();
                    PortInst newHeadPi = newHeadNi.findPortInstFromProto(newHeadPp);
                    PortInst newTailPi = newTailNi.findPortInstFromProto(newTailPp);
                    Poly newHeadPoly = newHeadPi.getPoly();
                    Poly newTailPoly = newTailPi.getPoly();

                    // see if the new arc can connect without end adjustment
                    if (!newHeadPoly.contains(pHead) || !newTailPoly.contains(pTail))
                    {
                        // arc cannot be run exactly ... presume port centers
                        if (!newHeadPoly.contains(pHead)) pHead = new EPoint(newHeadPoly.getCenterX(), newHeadPoly.getCenterY());
                        if (fixAng)
                        {
                            // old arc was fixed-angle so look for a similar-angle path
                            Rectangle2D headBounds = newHeadPoly.getBounds2D();
                            Rectangle2D tailBounds = newTailPoly.getBounds2D();
                            Point2D [] newPoints = GenMath.arcconnects(ai.getAngle(), headBounds, tailBounds);
                            if (newPoints != null)
                            {
                                pHead = new EPoint(newPoints[0].getX(), newPoints[0].getY());
                                pTail = new EPoint(newPoints[1].getX(), newPoints[1].getY());
                            }
                        }
                    }

                    // create the new arc
                    ArcInst newAi = ArcInst.makeInstance(newAp, newWid, newHeadPi, newTailPi, pHead, pTail, ai.getName());
                    if (newAi == null)
                    {
                        System.out.println("Cell " + newCell.describe(true) + ": can't run " + newAp + " from " +
                            newHeadNi + " " + newHeadPp + " at (" + pHead.getX() + "," + pHead.getY() + ") to " +
                            newTailNi + " " + newTailPp + " at (" + pTail.getX() + "," + pTail.getY() + ")");
                        continue;
                    }
                    newAi.copyPropertiesFrom(ai);
                    if (newAp == Generic.tech.universal_arc)
                    {
                        ai.setFixedAngle(false);
                        ai.setRigid(false);
                    }
                }
            }

            /**
             * Method to determine the equivalent prototype in technology "newtech" for
             * node prototype "oldnp".
             */
            private NodeProto figureNewNodeProto(NodeInst oldni, Technology newTech)
            {
                // easy translation if complex or already in the proper technology
                NodeProto oldNp = oldni.getProto();
                if (oldni.isCellInstance() || oldNp.getTechnology() == newTech) return oldNp;

                // if this is a layer node, check the layer functions
                PrimitiveNode.Function type = oldni.getFunction();

                if (oldni.getParent().getView() == View.SCHEMATIC && type.isTransistor()) {
                    for (Iterator<PrimitiveNode> it = newTech.getNodes(); it.hasNext(); ) {
                        PrimitiveNode node = it.next();
                        if (type == node.getFunction())
                            return node;
                    }
                }

                if (type == PrimitiveNode.Function.NODE)
                {
                    // get the polygon describing the first box of the old node
                    PrimitiveNode np = (PrimitiveNode)oldNp;
                    Technology.NodeLayer [] nodeLayers = np.getLayers();
                    Layer layer = nodeLayers[0].getLayer();
                    Layer.Function fun = layer.getFunction();

                    // now search for that function in the other technology
                    for(Iterator<PrimitiveNode> it = newTech.getNodes(); it.hasNext(); )
                    {
                        PrimitiveNode oNp = it.next();
                        if (oNp.getFunction() != PrimitiveNode.Function.NODE) continue;
                        Technology.NodeLayer [] oNodeLayers = oNp.getLayers();
                        Layer oLayer = oNodeLayers[0].getLayer();
                        Layer.Function oFun = oLayer.getFunction();
                        if (fun == oFun) return oNp;
                    }
                }

                // see if one node in the new technology has the same function
                int i = 0;
                PrimitiveNode rNp = null;
                for(Iterator<PrimitiveNode> it = newTech.getNodes(); it.hasNext(); )
                {
                    PrimitiveNode np = it.next();
                    if (np.getFunction() == type)
                    {
                        rNp = np;   i++;
                    }
                }
                if (i == 1) return rNp;

                // if there are too many matches, determine which is proper from arcs
                if (i > 1)
                {
                    // see if this node has equivalent arcs
                    PrimitiveNode pOldNp = (PrimitiveNode)oldNp;
                    PrimitivePort pOldPp = (PrimitivePort)pOldNp.getPort(0);
                    ArcProto [] oldConnections = pOldPp.getConnections();

                    for(Iterator<PrimitiveNode> it = newTech.getNodes(); it.hasNext(); )
                    {
                        PrimitiveNode pNewNp = it.next();
                        if (pNewNp.getFunction() != type) continue;
                        PrimitivePort pNewPp = (PrimitivePort)pNewNp.getPort(0);
                        ArcProto [] newConnections = pNewPp.getConnections();

                        boolean oldMatches = true;
                        for(int j=0; j<oldConnections.length; j++)
                        {
                            ArcProto oap = oldConnections[j];
                            if (oap.getTechnology() == Generic.tech) continue;

                            boolean foundNew = false;
                            for(int k=0; k<newConnections.length; k++)
                            {
                                ArcProto ap = newConnections[k];
                                if (ap.getTechnology() == Generic.tech) continue;
                                if (ap.getFunction() == oap.getFunction()) { foundNew = true;   break; }
                            }
                            if (!foundNew) { oldMatches = false;   break; }
                        }
                        if (oldMatches) { rNp = pNewNp;   i = 1;   break; }
                    }
                }

                // give up if it still cannot be determined
                return rNp;
            }

            private NodeInst placeLayoutNode(Nodable no, NodeProto newNp, Cell newCell, VarContext context)
            {
                NodeInst ni = no.getNodeInst();
                // scale edge offsets if this is a primitive
                double newXSize = 0, newYSize = 0;
                if (newNp instanceof PrimitiveNode)
                {
                    // get offsets for new node type
                    PrimitiveNode pNewNp = (PrimitiveNode)newNp;
                    PrimitiveNode pOldNp = (PrimitiveNode)ni.getProto();
                    newXSize = pNewNp.getDefWidth() + ni.getXSize() - pOldNp.getDefWidth();
                    newYSize = pNewNp.getDefHeight() + ni.getYSize() - pOldNp.getDefHeight();
                    if (no.getParent().getView() == View.SCHEMATIC && pNewNp.getFunction().isTransistor()) {
                        Variable width = no.getVar(Variable.newKey("ATTR_width"));
                        SizeOffset offset = newNp.getProtoSizeOffset();
                        if (width != null) {
                            newXSize = VarContext.objectToDouble(context.evalVar(width, no), newXSize);
                            newXSize = newXSize + offset.getHighXOffset() + offset.getLowXOffset();
                        }
                        Variable length = no.getVar(Variable.newKey("ATTR_length"));
                        if (length != null) {
                            newYSize = VarContext.objectToDouble(context.evalVar(length, no), newYSize);
                            newYSize = newYSize + offset.getHighYOffset() + offset.getLowYOffset();
                        }
                    }
                } else
                {
                    Cell np = (Cell)newNp;
                    Rectangle2D bounds = np.getBounds();
                    newXSize = bounds.getWidth();
                    newYSize = bounds.getHeight();
                }
    //			if (ni.isXMirrored()) newXSize = -newXSize;
    //			if (ni.isYMirrored()) newYSize = -newYSize;

                // create the node
                NodeInst newNi = NodeInst.makeInstance(newNp, ni.getAnchorCenter(), newXSize, newYSize, newCell, ni.getOrient(), no.getName(), ni.getTechSpecific());
    //			NodeInst newNi = NodeInst.makeInstance(newNp, ni.getAnchorCenter(), newXSize, newYSize, newCell, ni.getAngle(), ni.getName(), ni.getTechSpecific());
                if (newNi == null)
                {
                    System.out.println("Could not create " + newNp + " in " + newCell);
                    return null;
                }
                newNi.copyStateBits(ni);
                if (no.getParent().getView() != View.SCHEMATIC)
                    newNi.copyVarsFrom(ni);

                // re-export any ports on the node
                for(Iterator<Export> it = ni.getExports(); it.hasNext(); )
                {
                    Export e = it.next();
                    PortProto pp = convertPortProto(ni, newNi, e.getOriginalPort().getPortProto());
                    PortInst pi = newNi.findPortInstFromProto(pp);
                    Export pp2 = Export.newInstance(newCell, pi, e.getName());
                    if (pp2 == null) return newNi;
                    pp2.setCharacteristic(e.getCharacteristic());
                    pp2.copyTextDescriptorFrom(e, Export.EXPORT_NAME);
                    pp2.copyVarsFrom(e);
                }
                return newNi;
            }

            private static class PlacerGrid {
                private Leaf node = null;
                private PlacerGrid aboveNode = null;
                private PlacerGrid belowNode = null;
                private PlacerGrid leftNode = null;
                private PlacerGrid rightNode = null;
                private PlacerGrid() {}

                protected PlacerGrid insert(Leaf leafnode) {
                    if (node == null) {
                        node = leafnode;
                        return this;
                    }
                    Location loc = getRelativeLocation(leafnode.getCenter(false), node.getCenter(false));
                    switch (loc) {
                        case ABOVE: {
                            if (aboveNode == null)
                                aboveNode = leafnode;
                            else
                                aboveNode = aboveNode.insert(leafnode);
                            break;
                        }
                        case BELOW: {
                            if (belowNode == null)
                                belowNode = leafnode;
                            else
                                belowNode = belowNode.insert(leafnode);
                            break;
                        }
                        case LEFT:  {
                            if (leftNode == null)
                                leftNode = leafnode;
                            else
                                leftNode  = leftNode.insert(leafnode);
                            break;
                        }
                        case RIGHT: {
                            if (rightNode == null)
                                rightNode = leafnode;
                            else
                                rightNode = rightNode.insert(leafnode);
                            break;
                        }
                    }
                    return this;
                }

                public enum Location { RIGHT, LEFT, ABOVE, BELOW };

                private static Location getRelativeLocation(Point2D p, Point2D referenceP) {
                    double xDist = p.getX() - referenceP.getX();
                    double yDist = p.getY() - referenceP.getY();
                    if (Math.abs(xDist) >= Math.abs(yDist)) {
                        if (xDist < 0) return Location.LEFT;
                        return Location.RIGHT;
                    } else {
                        if (yDist < 0) return Location.BELOW;
                        return Location.ABOVE;
                    }
                }
                protected Point2D getCenter(boolean lay) {
                    Rectangle2D bounds;
                    if (lay) bounds = getLayBounds();
                    else bounds = getSchBounds();
                    if (bounds == null) return null;
                    return new EPoint(bounds.getCenterX(), bounds.getCenterY());
                }
                protected Rectangle2D getLayBounds() {
                    if (node == null) return null;
                    Rectangle2D bounds = node.getLayBounds();
                    if (aboveNode != null) bounds = bounds.createUnion(aboveNode.getLayBounds());
                    if (belowNode != null) bounds = bounds.createUnion(belowNode.getLayBounds());
                    if (leftNode != null) bounds = bounds.createUnion(leftNode.getLayBounds());
                    if (rightNode != null) bounds = bounds.createUnion(rightNode.getLayBounds());
                    return bounds;
                }
                protected Rectangle2D getSchBounds() {
                    if (node == null) return null;
                    Rectangle2D bounds = node.getSchBounds();
                    if (aboveNode != null) bounds = bounds.createUnion(aboveNode.getSchBounds());
                    if (belowNode != null) bounds = bounds.createUnion(belowNode.getSchBounds());
                    if (leftNode != null) bounds = bounds.createUnion(leftNode.getSchBounds());
                    if (rightNode != null) bounds = bounds.createUnion(rightNode.getSchBounds());
                    return bounds;
                }
                protected void place(MakeLayoutVisitor visitor, HashMap<Nodable,NodeInst> convertedNodes) {
                    if (node == null) return;
                    node.place(visitor, convertedNodes);
                    if (aboveNode != null) placeRelative(aboveNode, node, Location.ABOVE, visitor, convertedNodes);
                    if (belowNode != null) placeRelative(belowNode, node, Location.BELOW, visitor, convertedNodes);
                    if (leftNode != null)  placeRelative(leftNode,  node, Location.LEFT,  visitor, convertedNodes);
                    if (rightNode != null) placeRelative(rightNode, node, Location.RIGHT, visitor, convertedNodes);
                }
                private void placeRelative(PlacerGrid placeNode, PlacerGrid refNode,
                                           Location schRelLoc,
                                           MakeLayoutVisitor visitor,
                                           HashMap<Nodable,NodeInst> convertedNodes) {
                    placeNode.place(visitor, convertedNodes);
                    Location layRelLoc = getRelativeLocation(placeNode.getCenter(true), refNode.getCenter(true));
                    if (schRelLoc != layRelLoc || placeNode.getLayBounds().intersects(refNode.getLayBounds())) {
                        // move placed node to abut to reference node
                        abut(placeNode, refNode, schRelLoc);
                    }
                }
                private void abut(PlacerGrid abutNode, PlacerGrid refNode, Location relativeLocation) {
                    double dx = 0;
                    double dy = 0;
                    Rectangle2D referenceBounds = refNode.getLayBounds();
                    Rectangle2D niBounds = abutNode.getLayBounds();
                    if (relativeLocation == Location.RIGHT) {
                        dx = referenceBounds.getMaxX() - niBounds.getMinX();
                    } else if (relativeLocation == Location.LEFT) {
                        dx = referenceBounds.getMinX() - niBounds.getMaxX();
                    } else if (relativeLocation == Location.ABOVE) {
                        dy = referenceBounds.getMaxY() - niBounds.getMinY();
                    } else if (relativeLocation == Location.BELOW) {
                        dy = referenceBounds.getMinY() - niBounds.getMaxY();
                    }
                    if (dx != 0 || dy != 0) {
                        abutNode.move(dx, dy);
                    }
                }
                protected void move(double dx, double dy) {
                    node.move(dx, dy);
                    if (aboveNode != null) aboveNode.move(dx, dy);
                    if (belowNode != null) belowNode.move(dx, dy);
                    if (leftNode != null) leftNode.move(dx, dy);
                    if (rightNode != null) rightNode.move(dx, dy);
                }
                protected void print(int indent) {

                }
                public void prindent(int ident) {
                    for (int i=0; i<ident; i++) System.out.print(" ");
                }
            }

            private static class Leaf extends PlacerGrid {
                private Nodable schNo;
                private VarContext context;
                private NodeProto layNp;
                private NodeInst layNi;
                private Cell newCell;
                private Leaf(Nodable schNo, VarContext context, NodeProto layNp, Cell newCell) {
                    this.schNo = schNo;
                    this.layNp = layNp;
                    this.newCell = newCell;
                    this.context = context;
                }
                protected Point2D getCenter(boolean lay) {
                    if (lay) return layNi.getAnchorCenter();
                    return schNo.getNodeInst().getAnchorCenter();
                }
                protected PlacerGrid insert(Leaf node) {
                    PlacerGrid gridnode = new PlacerGrid();
                    gridnode.insert(this);
                    gridnode.insert(node);
                    return gridnode;
                }
                protected Rectangle2D getLayBounds() {
                    assert(layNi != null);
                    return layNi.getBounds();
                }
                protected Rectangle2D getSchBounds() {
                    return schNo.getNodeInst().getBounds();
                }
                protected void place(MakeLayoutVisitor visitor, HashMap<Nodable,NodeInst> convertedNodes) {
                    layNi = visitor.placeLayoutNode(schNo, layNp, newCell, context);
                    convertedNodes.put(schNo, layNi);
                }
                protected void move(double dx, double dy) {
                    layNi.modifyInstance(dx, dy, 0, 0, Orientation.IDENT);
                    System.out.println("Moved "+layNi.getName()+" ("+dx+","+dy+")");
                }
                protected void print(int indent) {
                    prindent(indent);
                    System.out.println("node "+schNo.getName()+" at "+getCenter(false));
                }
            }

            /**
             * Method to determine the equivalent prototype in technology "newTech" for
             * node prototype "oldnp".
             */
            private ArcProto figureNewArcProto(ArcProto oldAp, Technology newTech, PortProto headPp, PortProto tailPp)
            {
                // schematic wires become universal arcs
                if (oldAp != Schematics.tech.wire_arc)
                {
                    // determine the proper association of this node
                    ArcProto.Function type = oldAp.getFunction();
                    for(Iterator<ArcProto> it = newTech.getArcs(); it.hasNext(); )
                    {
                        ArcProto newAp = it.next();
                        if (newAp.getFunction() == type) return newAp;
                    }
                }

                // cannot figure it out from the function: find anything that can connect
                HashSet<ArcProto> possibleArcs = new HashSet<ArcProto>();
                ArcProto [] headArcs = headPp.getBasePort().getConnections();
                ArcProto [] tailArcs = tailPp.getBasePort().getConnections();
                for(int i=0; i < headArcs.length; i++)
                {
                    if (headArcs[i].getTechnology() == Generic.tech) continue;
                    for(int j=0; j < tailArcs.length; j++)
                    {
                        if (tailArcs[j].getTechnology() == Generic.tech) continue;
                        if (headArcs[i] != tailArcs[j]) continue;
                        possibleArcs.add(headArcs[i]);
                        break;
                    }
                }
                for(Iterator<ArcProto> it = newTech.getArcs(); it.hasNext(); )
                {
                    ArcProto ap = it.next();
                    if (possibleArcs.contains(ap)) return ap;
                }
                System.out.println("No equivalent arc for " + oldAp);
                return Generic.tech.universal_arc;
            }

            /**
             * Method to determine the port to use on node "newni" assuming that it should
             * be the same as port "oldPp" on equivalent node "ni"
             */
            private PortProto convertPortProto(NodeInst ni, NodeInst newNi, PortProto oldPp)
            {
                if (newNi.isCellInstance())
                {
                    // cells can associate by comparing names
                    PortProto pp = newNi.getProto().findPortProto(oldPp.getName());
                    if (pp != null) return pp;
                    System.out.println("Cannot find export " + oldPp.getName() + " in " + newNi.getProto());
                    return newNi.getProto().getPort(0);
                }

                // if each has only 1 port, they match
                int numNewPorts = newNi.getProto().getNumPorts();
                if (numNewPorts == 0) return null;
                if (numNewPorts == 1)
                {
                    return newNi.getProto().getPort(0);
                }

                // associate by position in port list
                Iterator<PortProto> oldPortIt = ni.getProto().getPorts();
                Iterator<PortProto> newPortIt = newNi.getProto().getPorts();
                while (oldPortIt.hasNext() && newPortIt.hasNext())
                {
                    PortProto pp = oldPortIt.next();
                    PortProto newPp = newPortIt.next();
                    if (pp == oldPp) return newPp;
                }

                // special case again: one-port capacitors are OK
                PrimitiveNode.Function oldFun = ni.getFunction();
                PrimitiveNode.Function newFun = newNi.getFunction();
                if (oldFun == PrimitiveNode.Function.CAPAC && newFun == PrimitiveNode.Function.ECAPAC) return newNi.getProto().getPort(0);

                // association has failed: assume the first port
                System.out.println("No port association between " + ni.getProto() + ", "
                    + oldPp + " and " + newNi.getProto());
                return newNi.getProto().getPort(0);
            }

        }
    }

}
