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

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.TransistorSize;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

/**
 * Class for view-related changes to the circuit.
 */
public class ViewChanges
{
	// constructor, never used
	ViewChanges() {}

	/****************************** CHANGE A CELL'S VIEW ******************************/

	public static void changeCellView(Cell cell, View newView)
	{
		// stop if already this way
		if (cell.getView() == newView) return;

		// warn if there is already a cell with that view
		for(Iterator it = cell.getLibrary().getCells(); it.hasNext(); )
		{
			Cell other = (Cell)it.next();
			if (other.getView() != newView) continue;
			if (!other.getName().equalsIgnoreCase(cell.getName())) continue;

			// there is another cell with this name and view: warn that it will become old
			int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
				"There is already a cell with that view.  Is it okay to make it an older version, and make this the newest version?");
			if (response != JOptionPane.YES_OPTION) return;
			break;
		}
		ChangeCellView job = new ChangeCellView(cell, newView);
	}

	/**
	 * Class to change a cell's view in a new thread.
	 */
	private static class ChangeCellView extends Job
	{
		Cell cell;
		View newView;

		protected ChangeCellView(Cell cell, View newView)
		{
			super("Change View of Cell" + cell.describe() + " to " + newView.getFullName(),
				User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.newView = newView;
			startJob();
		}

		public boolean doIt()
		{
			cell.setView(newView);
			for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)it.next();
				if (wf.getContent().getCell() == cell)
				{
					wf.getContent().setCell(cell, VarContext.globalContext);
				}
			}
			EditWindow.repaintAll();
			return true;
		}
	}

	/****************************** MAKE A MULTI-PAGE SCHEMATIC FOR A CELL ******************************/

	public static void makeMultiPageSchematicViewCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
		String newSchematicPage = JOptionPane.showInputDialog("Page Number", "");
		if (newSchematicPage == null) return;
		int pageNo = TextUtils.atoi(newSchematicPage);
		if (pageNo <= 0)
		{
			System.out.println("Multi-page schematics are numbered starting at page 1");
			return;
		}
		MakeMultiPageView job = new MakeMultiPageView(curCell, pageNo);
	}

	private static class MakeMultiPageView extends Job
	{
		private Cell cell;
		private int pageNo;

		protected MakeMultiPageView(Cell cell, int pageNo)
		{
			super("Make Multipage Schematic View", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.pageNo = pageNo;
			startJob();
		}

		public boolean doIt()
		{
			View v = View.findMultiPageSchematicView(pageNo);
			if (v == null)
			{
				v = View.newMultiPageSchematicInstance(pageNo);
			}
			Cell otherView = cell.otherView(v);
			if (otherView == null)
			{
				otherView = Cell.makeInstance(cell.getLibrary(), cell.getName() + "{p" + pageNo + "}");
			}
			WindowFrame.createEditWindow(otherView);
			return true;
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

		MakeSkeletonView job = new MakeSkeletonView(curCell);
	}

	private static class MakeSkeletonView extends Job
	{
		private Cell curCell;

		protected MakeSkeletonView(Cell curCell)
		{
			super("Make Skeleton View", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.curCell = curCell;
			startJob();
		}

		public boolean doIt()
		{
			// create the new icon cell
			String skeletonCellName = curCell.getName() + "{lay.sk}";
			Cell skeletonCell = Cell.makeInstance(curCell.getLibrary(), skeletonCellName);
			if (skeletonCell == null)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Cannot create Skeleton cell " + skeletonCellName,
						"Skeleton creation failed", JOptionPane.ERROR_MESSAGE);
				return false;
			}

			HashMap newPortMap = new HashMap();
			ArcProto univ = Generic.tech.universal_arc;

			// place all exports in the new cell
			for(Iterator it = curCell.getPorts(); it.hasNext(); )
			{
				Export pp = (Export)it.next();

				// traverse to the bottom of the hierarchy for this Export
				PortProto.FindPrimitive fp = new PortProto.FindPrimitive(pp.getOriginalPort());
				PortInst bottomPort = fp.getBottomPort();
				NodeInst bottomNi = bottomPort.getNodeInst();
				PortProto bottomPp = bottomPort.getPortProto();
				AffineTransform subRot = fp.getTransformToTop();
				int newAng = fp.getAngleToTop();

				// create this node
				Point2D center = new Point2D.Double(bottomNi.getAnchorCenterX(), bottomNi.getAnchorCenterY());
				subRot.transform(center, center);
				NodeInst newNi = NodeInst.makeInstance(bottomNi.getProto(), center, bottomNi.getXSize(), bottomNi.getYSize(),
				        skeletonCell, newAng, null, 0);
				if (newNi == null)
				{
					System.out.println("Cannot create node in this cell");
					return false;
				}

				// export the port from the node
				PortInst newPi = newNi.findPortInstFromProto(bottomPp);
				Export npp = Export.newInstance(skeletonCell, newPi, pp.getName());
				if (npp == null)
				{
					System.out.println("Could not create port " + pp.getName());
					return false;
				}
				npp.setTextDescriptor(pp.getTextDescriptor());
				npp.copyVarsFrom(pp);
				npp.setCharacteristic(pp.getCharacteristic());
				newPortMap.put(pp, npp);
			}

			// connect electrically-equivalent ports
			Netlist netlist = curCell.getUserNetlist();
			int numPorts = curCell.getNumPorts();
			for(int i=0; i<numPorts; i++)
			{
				Export pp = (Export)curCell.getPort(i);
				for(int j=i+1; j<numPorts; j++)
				{
					Export oPp = (Export)curCell.getPort(j);
					if (!netlist.sameNetwork(pp.getOriginalPort().getNodeInst(), pp.getOriginalPort().getPortProto(),
						oPp.getOriginalPort().getNodeInst(), oPp.getOriginalPort().getPortProto())) continue;

					Export newPp = (Export)newPortMap.get(pp);
					Export newOPp = (Export)newPortMap.get(oPp);
					if (newPp == null || newOPp == null) continue;
					ArcInst newAI = ArcInst.makeInstance(univ, univ.getDefaultWidth(), newPp.getOriginalPort(), newOPp.getOriginalPort());
					if (newAI == null)
					{
						System.out.println("Could not create connecting arc");
						return false;
					}
					newAI.setFixedAngle(false);
				}
			}

			// copy the cell center and essential-bounds nodes if they exist
			for(Iterator it = curCell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				NodeProto np = ni.getProto();
				if (np != Generic.tech.cellCenterNode && np != Generic.tech.essentialBoundsNode) continue;
				NodeInst newNi = NodeInst.makeInstance(np, ni.getAnchorCenter(),
					ni.getXSizeWithMirror(), ni.getYSizeWithMirror(), skeletonCell, ni.getAngle(), null, 0);
				if (newNi == null)
				{
					System.out.println("Cannot create node in this cell");
					return false;
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
				return false;
			}
			boundNi.setHardSelect();

			System.out.println("Cell " + skeletonCell.describe() + " created with a skeletal representation of " +
				curCell.describe());
			WindowFrame.createEditWindow(skeletonCell);

			return true;
		}
	}

	/****************************** MAKE AN ICON FOR A CELL ******************************/

	public static void makeIconViewCommand()
	{
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
        MakeIconView job = new MakeIconView(curCell);
	}

	private static class MakeIconView extends Job
	{
		private static boolean reverseIconExportOrder;
        private Cell curCell;

		protected MakeIconView(Cell cell)
		{
			super("Make Icon View", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.curCell = cell;
            startJob();
		}

		public boolean doIt()
		{
			Library lib = curCell.getLibrary();

			if (!curCell.isSchematicView())
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"The current cell must be a schematic in order to generate an icon",
						"Icon creation failed", JOptionPane.ERROR_MESSAGE);
				return false;
			}

			// see if the icon already exists and issue a warning if so
			Cell iconCell = curCell.iconView();
			if (iconCell != null)
			{
				int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
					"Warning: Icon " + iconCell.describe() + " already exists.  Create a new version?");
				if (response != JOptionPane.YES_OPTION) return false;
			}

			// get icon style controls
			double leadLength = User.getIconGenLeadLength();
			double leadSpacing = User.getIconGenLeadSpacing();
			reverseIconExportOrder = User.isIconGenReverseExportOrder();

			// make a sorted list of exports
			List exportList = new ArrayList();
			for(Iterator it = curCell.getPorts(); it.hasNext(); )
				exportList.add(it.next());
			Collections.sort(exportList, new ExportSorted());

			// create the new icon cell
			String iconCellName = curCell.getName() + "{ic}";
			iconCell = Cell.makeInstance(lib, iconCellName);
			if (iconCell == null)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Cannot create Icon cell " + iconCellName,
						"Icon creation failed", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			iconCell.setWantExpanded();

			// determine number of inputs and outputs
			int leftSide = 0, rightSide = 0, bottomSide = 0, topSide = 0;
			for(Iterator it = exportList.iterator(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				if (pp.isBodyOnly()) continue;
				int index = iconPosition(pp);
				switch (index)
			{
					case 0: pp.setTempInt(leftSide++);    break;
					case 1: pp.setTempInt(rightSide++);   break;
					case 2: pp.setTempInt(topSide++);     break;
					case 3: pp.setTempInt(bottomSide++);  break;
				}
			}

			// determine the size of the "black box" core
			double ySize = Math.max(Math.max(leftSide, rightSide), 5) * leadSpacing;
			double xSize = Math.max(Math.max(topSide, bottomSide), 3) * leadSpacing;

			// create the "black box"
			NodeInst bbNi = null;
			if (User.isIconGenDrawBody())
			{
                bbNi = NodeInst.newInstance(Artwork.tech.boxNode, new Point2D.Double(0,0), xSize, ySize, iconCell);
				if (bbNi == null) return false;
				bbNi.newVar(Artwork.ART_COLOR, new Integer(EGraphics.RED));

				// put the original cell name on it
				Variable var = bbNi.newVar(Schematics.SCHEM_FUNCTION, curCell.getName());
				if (var != null)
				{
					var.setDisplay(true);
				}
			}

			// place pins around the Black Box
			int total = 0;
			for(Iterator it = exportList.iterator(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				if (pp.isBodyOnly()) continue;

				// determine location of the port
				int index = iconPosition(pp);
				double spacing = leadSpacing;
				double xPos = 0, yPos = 0;
				double xBBPos = 0, yBBPos = 0;
				switch (index)
				{
					case 0:		// left side
						xBBPos = -xSize/2;
						xPos = xBBPos - leadLength;
						if (leftSide*2 < rightSide) spacing = leadSpacing * 2;
						yBBPos = yPos = ySize/2 - ((ySize - (leftSide-1)*spacing) / 2 + pp.getTempInt() * spacing);
						break;
					case 1:		// right side
						xBBPos = xSize/2;
						xPos = xBBPos + leadLength;
						if (rightSide*2 < leftSide) spacing = leadSpacing * 2;
						yBBPos = yPos = ySize/2 - ((ySize - (rightSide-1)*spacing) / 2 + pp.getTempInt() * spacing);
						break;
					case 2:		// top
						if (topSide*2 < bottomSide) spacing = leadSpacing * 2;
						xBBPos = xPos = xSize/2 - ((xSize - (topSide-1)*spacing) / 2 + pp.getTempInt() * spacing);
						yBBPos = ySize/2;
						yPos = yBBPos + leadLength;
						break;
					case 3:		// bottom
						if (bottomSide*2 < topSide) spacing = leadSpacing * 2;
						xBBPos = xPos = xSize/2 - ((xSize - (bottomSide-1)*spacing) / 2 + pp.getTempInt() * spacing);
						yBBPos = -ySize/2;
						yPos = yBBPos - leadLength;
						break;
				}

				if (makeIconExport(pp, index, xPos, yPos, xBBPos, yBBPos, iconCell))
					total++;
			}

			// if no body, leads, or cell center is drawn, and there is only 1 export, add more
			if (!User.isIconGenDrawBody() &&
				!User.isIconGenDrawLeads() &&
				User.isPlaceCellCenter() &&
				total <= 1)
			{
				NodeInst.newInstance(Generic.tech.invisiblePinNode, new Point2D.Double(0,0), xSize, ySize, iconCell);
			}

			// place an icon in the schematic
			int exampleLocation = User.getIconGenInstanceLocation();
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
			EditWindow.gridAlign(iconPos);
			double px = iconCell.getBounds().getWidth();
			double py = iconCell.getBounds().getHeight();
			NodeInst ni = NodeInst.makeInstance(iconCell, iconPos, px, py, curCell);
			if (ni != null)
			{
                EditWindow wnd = EditWindow.getCurrent();
                if (wnd != null) {
                    if (wnd.getCell() == curCell) {
                        Highlighter highlighter = wnd.getHighlighter();
                        highlighter.clear();
                        highlighter.addElectricObject(ni, curCell);
                        highlighter.finished();
                    }
                }
			}
			return true;
		}

		static class ExportSorted implements Comparator
		{
			public int compare(Object o1, Object o2)
			{
				Export e1 = (Export)o1;
				Export e2 = (Export)o2;
				String s1 = e1.getName();
				String s2 = e2.getName();
				if (reverseIconExportOrder)
					return s2.compareToIgnoreCase(s1);
				return s1.compareToIgnoreCase(s2);
			}
		}
	}

	/**
	 * Helper method to create an export in icon "np".  The export is from original port "pp",
	 * is on side "index" (0: left, 1: right, 2: top, 3: bottom), is at (xPos,yPos), and
	 * connects to the central box at (xBBPos,yBBPos).  Returns TRUE if the export is created.
	 * It uses icon style "style".
	 */
	public static boolean makeIconExport(Export pp, int index,
		double xPos, double yPos, double xBBPos, double yBBPos, Cell np)
	{
		// presume "universal" exports (Generic technology)
		NodeProto pinType = Generic.tech.universalPinNode;
		double pinSizeX = 0, pinSizeY = 0;
		if (User.getIconGenExportTech() != 0)
		{
			// instead, use "schematic" exports (Schematic Bus Pins)
			pinType = Schematics.tech.busPinNode;
			pinSizeX = pinType.getDefWidth();
			pinSizeY = pinType.getDefHeight();
		}

		// determine the type of wires used for leads
		PrimitiveArc wireType = Schematics.tech.wire_arc;
		if (pp.getBasePort().connectsTo(Schematics.tech.bus_arc))
		{
			wireType = Schematics.tech.bus_arc;
			pinType = Schematics.tech.busPinNode;
			pinSizeX = pinType.getDefWidth();
			pinSizeY = pinType.getDefHeight();
		}

		// if the export is on the body (no leads) then move it in
		if (!User.isIconGenDrawLeads())
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
			TextDescriptor td = port.getTextDescriptor();
			switch (User.getIconGenExportStyle())
			{
				case 0:		// Centered
					td.setPos(TextDescriptor.Position.CENT);
					break;
				case 1:		// Inward
					switch (index)
					{
						case 0: td.setPos(TextDescriptor.Position.RIGHT);  break;	// left
						case 1: td.setPos(TextDescriptor.Position.LEFT);   break;	// right
						case 2: td.setPos(TextDescriptor.Position.DOWN);   break;	// top
						case 3: td.setPos(TextDescriptor.Position.UP);     break;	// bottom
					}
					break;
				case 2:		// Outward
					switch (index)
					{
						case 0: td.setPos(TextDescriptor.Position.LEFT);   break;	// left
						case 1: td.setPos(TextDescriptor.Position.RIGHT);  break;	// right
						case 2: td.setPos(TextDescriptor.Position.UP);     break;	// top
						case 3: td.setPos(TextDescriptor.Position.DOWN);   break;	// bottom
					}
					break;
			}
			double xOffset = 0, yOffset = 0;
			int loc = User.getIconGenExportLocation();
			if (!User.isIconGenDrawLeads()) loc = 0;
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
			td.setOff(xOffset, yOffset);
			if (pp.isAlwaysDrawn()) port.setAlwaysDrawn(); else
				port.clearAlwaysDrawn();
			port.setCharacteristic(pp.getCharacteristic());
			port.copyVarsFrom(pp);
		}

		// add lead if requested
		if (User.isIconGenDrawLeads())
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
                    ai.setExtended(false);
			}
		}
		return true;
	}

	/**
	 * Method to determine the side of the icon that port "pp" belongs on.
	 */
	private static int iconPosition(Export pp)
	{
		PortProto.Characteristic character = pp.getCharacteristic();

		// special detection for power and ground ports
		if (pp.isPower()) character = PortProto.Characteristic.PWR;
		if (pp.isGround()) character = PortProto.Characteristic.GND;

		// see which side this type of port sits on
		if (character == PortProto.Characteristic.IN)
			return User.getIconGenInputSide();
		if (character == PortProto.Characteristic.OUT)
			return User.getIconGenOutputSide();
		if (character == PortProto.Characteristic.BIDIR)
			return User.getIconGenBidirSide();
		if (character == PortProto.Characteristic.PWR)
			return User.getIconGenPowerSide();
		if (character == PortProto.Characteristic.GND)
			return User.getIconGenGroundSide();
		if (character == PortProto.Characteristic.CLK || character == PortProto.Characteristic.C1 ||
			character == PortProto.Characteristic.C2 || character == PortProto.Characteristic.C3 ||
			character == PortProto.Characteristic.C4 || character == PortProto.Characteristic.C5 ||
			character == PortProto.Characteristic.C6)
				return User.getIconGenClockSide();
		return User.getIconGenInputSide();
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
		private static boolean reverseIconExportOrder;
        private Cell oldCell;

		protected MakeSchematicView(Cell cell)
		{
			super("Make Schematic View", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.oldCell = cell;
            startJob();
		}

		public boolean doIt()
		{
	        // create cell in new technology
			Cell newCell = makeSchematicCell(oldCell.getName(), View.SCHEMATIC, oldCell);
			if (newCell == null) return false;

			// create the parts in this cell
			HashMap newNodes = new HashMap();
			buildSchematicNodes(oldCell, newCell, Schematics.tech, newNodes);
			buildSchematicArcs(oldCell, newCell, newNodes);

			// now make adjustments for manhattan-ness
			makeArcsManhattan(newCell);

			// set "fixed-angle" if reasonable
			for(Iterator it = newCell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				Point2D headPt = ai.getHead().getLocation();
				Point2D tailPt = ai.getTail().getLocation();
				if (headPt.getX() == tailPt.getX() && headPt.getY() == tailPt.getY()) continue;
				if ((GenMath.figureAngle(headPt, tailPt)%450) == 0) ai.setFixedAngle(true);
			}

			System.out.println("Cell " + newCell.describe() + " created with a schematic representation of " +
				oldCell.describe());
			WindowFrame.createEditWindow(newCell);
			return true;
		}
	}

	/**
	 * Method to create a new cell called "newcellname" that is to be the
	 * equivalent to an old cell in "cell".  The view type of the new cell is
	 * in "newcellview" and the view type of the old cell is in "cellview"
	 */
	private static Cell makeSchematicCell(String newCellName, View newCellView, Cell cell)
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
	
	private static void buildSchematicNodes(Cell cell, Cell newCell, Technology newtech, HashMap newNodes)
	{
		// for each node, create a new node in the newcell, of the correct logical type.
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst mosNI = (NodeInst)it.next();
			NodeProto.Function type = getNodeType(mosNI);
			NodeInst schemNI = null;
			if (type == NodeProto.Function.UNKNOWN) continue;
			if (type == NodeProto.Function.PIN)
			{
				// compute new x, y coordinates
				NodeProto prim = Schematics.tech.wirePinNode;
				schemNI = makeSchematicNode(prim, mosNI, prim.getDefWidth(), prim.getDefHeight(), 0, 0, newCell);
			} else if (type == null)
			{
				// a cell
				Cell proto = (Cell)mosNI.getProto();
				Cell equivCell = proto.getEquivalent();
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
						Variable lenVar = schemNI.newVar("ATTR_length", new Double(ts.getDoubleLength()));
						if (lenVar != null)
						{
							lenVar.setDisplay(true);
							TextDescriptor lenTD = lenVar.getTextDescriptor();
							lenTD.setRelSize(0.5);
							lenTD.setOff(-0.5, -1);
						}
						Variable widVar = schemNI.newVar("ATTR_width", new Double(ts.getDoubleWidth()));
						if (widVar != null)
						{
							widVar.setDisplay(true);
							TextDescriptor widTD = widVar.getTextDescriptor();
							widTD.setRelSize(1);
							widTD.setOff(0.5, -1);
						}
					} else
					{
						// set area
						schemNI.newVar("ATTR_area", new Double(ts.getDoubleLength()));
					}
				}
			}
	
			// store the new node in the old node
			newNodes.put(mosNI, schemNI);
	
			// reexport ports
			if (schemNI != null)
			{
				for(Iterator eIt = mosNI.getExports(); eIt.hasNext(); )
				{
					Export mosPP = (Export)eIt.next();
					PortInst schemPI = convertPort(mosNI, mosPP.getOriginalPort().getPortProto(), schemNI);
					if (schemPI == null) continue;

					Export schemPP = Export.newInstance(newCell, schemPI, mosPP.getName());
					if (schemPP != null)
					{
						schemPP.setCharacteristic(mosPP.getCharacteristic());
						schemPP.setTextDescriptor(mosPP.getTextDescriptor());
						schemPP.copyVarsFrom(mosPP);
					}
				}
			}
		}
	}

	private static NodeInst makeSchematicNode(NodeProto prim, NodeInst orig, double wid, double hei, int angle, int techSpecific, Cell newCell)
	{
		Point2D newLoc = new Point2D.Double(orig.getAnchorCenterX(), orig.getAnchorCenterY());
		NodeInst newNI = NodeInst.makeInstance(prim, newLoc, wid, hei, newCell, angle, null, techSpecific);
		return newNI;
	}

	/**
	 * for each arc in cell, find the ends in the new technology, and
	 * make a new arc to connect them in the new cell.
	 */
	private static void buildSchematicArcs(Cell cell, Cell newcell, HashMap newNodes)
	{
		for(Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst mosAI = (ArcInst)it.next();
			NodeInst mosHeadNI = mosAI.getHead().getPortInst().getNodeInst();
			NodeInst mosTailNI = mosAI.getTail().getPortInst().getNodeInst();
			NodeInst schemHeadNI = (NodeInst)newNodes.get(mosHeadNI);
			NodeInst schemTailNI = (NodeInst)newNodes.get(mosTailNI);
			if (schemHeadNI == null || schemTailNI == null) continue;
			PortInst schemHeadPI = convertPort(mosHeadNI, mosAI.getHead().getPortInst().getPortProto(), schemHeadNI);
			PortInst schemTailPI = convertPort(mosTailNI, mosAI.getTail().getPortInst().getPortProto(), schemTailNI);
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
		NodeProto.Function fun = getNodeType(schemNI);
		if (fun == NodeProto.Function.PIN)
		{
			return schemNI.getOnlyPortInst();
		}
		if (fun == null)
		{
			// a cell
			PortProto schemPP = schemNI.getProto().findPortProto(mosPP.getName());
			return schemNI.findPortInstFromProto(schemPP);
		}

		// a transistor
		int portNum = 1;
		for(Iterator it = mosNI.getProto().getPorts(); it.hasNext(); )
		{
			PortProto pp = (PortProto)it.next();
			if (pp == mosPP) break;
			portNum++;
		}
		if (portNum == 4) portNum = 3; else
			if (portNum == 3) portNum = 1;
		for(Iterator it = schemNI.getProto().getPorts(); it.hasNext(); )
		{
			PortProto schemPP = (PortProto)it.next();
			portNum--;
			if (portNum > 0) continue;
			return schemNI.findPortInstFromProto(schemPP);
		}
		return null;
	}

	private static final int MAXADJUST = 5;

	private static void makeArcsManhattan(Cell newCell)
	{
		// adjust this cell
		double [] x = new double[MAXADJUST];
		double [] y = new double[MAXADJUST];
		for(Iterator it = newCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() instanceof Cell) continue;
			NodeProto.Function fun = ni.getFunction();
			if (fun != NodeProto.Function.PIN) continue;

			// see if this pin can be adjusted so that all wires are manhattan
			int count = 0;
			for(Iterator aIt = ni.getConnections(); aIt.hasNext(); )
			{
				Connection con = (Connection)aIt.next();
				ArcInst ai = con.getArc();
				Connection other = ai.getHead();
				if (ai.getHead() == con) other = ai.getTail();
				if (con.getPortInst().getNodeInst() == other.getPortInst().getNodeInst()) continue;
				x[count] = other.getLocation().getX();
				y[count] = other.getLocation().getY();
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
				ni.modifyInstance(bestX-xp, bestY-yp, 0, 0, 0);
		}
	}

	/**
	 * Method to figure out if a NodeInst is a MOS component
	 * (a wire or transistor).  If it's a transistor, return its function; 
	 * if it's a passive connector, return NodeProto.Function.PIN;
	 * if it's a cell, return null; else return NodeProto.Function.UNKNOWN.
	 */
	private static NodeProto.Function getNodeType(NodeInst ni)
	{
		if (ni.getProto() instanceof Cell) return null;
		NodeProto.Function fun = ni.getFunction();
		if (fun.isTransistor()) return fun;
		if (fun == NodeProto.Function.PIN || fun == NodeProto.Function.CONTACT ||
			fun == NodeProto.Function.NODE || fun == NodeProto.Function.CONNECT ||
			fun == NodeProto.Function.SUBSTRATE || fun == NodeProto.Function.WELL)
				return NodeProto.Function.PIN;
		return NodeProto.Function.UNKNOWN;
	}

	/****************************** CONVERT TO ALTERNATE LAYOUT ******************************/

//	/*
//	 * routine to recursively descend from cell "oldcell" and find subcells that
//	 * have to be converted.  When all subcells have been converted, convert this
//	 * one into a new one called "newcellname".  The technology for the old cell
//	 * is "oldtech" and the technology to use for the new cell is "newtech".  The
//	 * old view type is "oldview" and the new view type is "nview".
//	 */
//	NODEPROTO *us_tran_makelayoutcells(NODEPROTO *oldcell, CHAR *newcellname,
//		TECHNOLOGY *oldtech, TECHNOLOGY *newtech, VIEW *nview)
//	{
//		REGISTER NODEPROTO *newcell, *rnp;
//		REGISTER INTBIG bits;
//		REGISTER NODEINST *ni;
//		REGISTER ARCINST *ai;
//		CHAR *str;
//
//		// first convert the sub-cells */
//		for(ni = oldcell->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			/* ignore primitives */
//			if (ni->proto->primindex != 0) continue;
//
//			/* ignore recursive references (showing icon in contents) */
//			if (isiconof(ni->proto, oldcell)) continue;
//
//			/* ignore cells with associations */
//			FOR_CELLGROUP(rnp, ni->proto)
//				if (rnp->cellview == nview) break;
//			if (rnp != NONODEPROTO) continue;
//
//			/* make up a name for this cell */
//			(void)allocstring(&str, ni->proto->protoname, el_tempcluster);
//
//			(void)us_tran_makelayoutcells(ni->proto, str, oldtech, newtech, nview);
//			efree(str);
//		}
//
//		/* create the cell and fill it with parts */
//		newcell = makeSchematicCell(newcellname, nview, oldcell);
//		if (newcell == NONODEPROTO) return(NONODEPROTO);
//		if (us_tran_makelayoutparts(oldcell, newcell, oldtech, newtech, nview))
//		{
//			/* adjust for maximum Manhattan-ness */
//			makeArcsManhattan(newcell);
//
//			/* reset shrinkage values and constraints to defaults (is this needed? !!!) */
//			for(ai = newcell->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst)
//			{
//				bits = us_makearcuserbits(ai->proto);
//				if ((bits&FIXED) != 0) ai->userbits |= FIXED;
//				if ((bits&FIXANG) != 0) ai->userbits |= FIXANG;
//				(void)setshrinkvalue(ai, FALSE);
//			}
//		}
//
//		return(newcell);
//	}
//
//	/*
//	 * routine to create a new cell in "newcell" from the contents of an old cell
//	 * in "oldcell".  The technology for the old cell is "oldtech" and the
//	 * technology to use for the new cell is "newtech".
//	 */
//	BOOLEAN us_tran_makelayoutparts(NODEPROTO *oldcell, NODEPROTO *newcell,
//		TECHNOLOGY *oldtech, TECHNOLOGY *newtech, VIEW *nview)
//	{
//		REGISTER NODEPROTO *newnp;
//		REGISTER NODEINST *ni, *end1, *end2;
//		ARCPROTO *ap, *newap;
//		ARCINST *ai;
//		REGISTER PORTPROTO *mospp1, *mospp2, *schempp1, *schempp2;
//		INTBIG x1, y1, x2, y2, lx1, hx1, ly1, hy1, lx2, hx2, ly2, hy2, tx1, ty1, tx2, ty2;
//		REGISTER INTBIG newwid, newbits, oldlambda, newlambda, defwid, curwid;
//		REGISTER INTBIG badarcs, i, j;
//		REGISTER BOOLEAN univarcs;
//		static POLYGON *poly1 = NOPOLYGON, *poly2 = NOPOLYGON;
//
//		/* get a polygon */
//		(void)needstaticpolygon(&poly1, 4, us_tool->cluster);
//		(void)needstaticpolygon(&poly2, 4, us_tool->cluster);
//
//		/* get lambda values */
//		oldlambda = el_curlib->lambda[oldtech->techindex];
//		newlambda = el_curlib->lambda[newtech->techindex];
//
//		/* first convert the nodes */
//		for(ni = oldcell->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//			ni->temp1 = 0;
//		for(ni = oldcell->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			/* handle sub-cells */
//			if (ni->proto->primindex == 0)
//			{
//				FOR_CELLGROUP(newnp, ni->proto)
//					if (newnp->cellview == nview) break;
//				if (newnp == NONODEPROTO)
//				{
//					ttyputerr(_("No equivalent cell for %s"), describenodeproto(ni->proto));
//					continue;
//				}
//				us_tranplacenode(ni, newnp, newcell, oldtech, newtech);
//				continue;
//			}
//
//			/* handle primitives */
//			if (ni->proto == gen_cellcenterprim) continue;
//			newnp = us_figurenewnproto(ni, newtech);
//			us_tranplacenode(ni, newnp, newcell, oldtech, newtech);
//		}
//
//		/*
//		 * for each arc in cell, find the ends in the new technology, and
//		 * make a new arc to connect them in the new cell
//		 */
//		badarcs = 0;
//		univarcs = FALSE;
//		for(ai = oldcell->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst)
//		{
//			/* get the nodes and ports on the two ends of the arc */
//			end1 = (NODEINST *)ai->end[0].nodeinst->temp1;
//			end2 = (NODEINST *)ai->end[1].nodeinst->temp1;
//			if (end1 == 0 || end2 == 0) continue;
//			mospp1 = ai->end[0].portarcinst->proto;
//			mospp2 = ai->end[1].portarcinst->proto;
//			schempp1 = us_convport(ai->end[0].nodeinst, end1, mospp1);
//			schempp2 = us_convport(ai->end[1].nodeinst, end2, mospp2);
//
//			/* set bits in arc prototypes that can make the connection */
//			for(ap = newtech->firstarcproto; ap != NOARCPROTO; ap = ap->nextarcproto)
//				ap->userbits &= ~CANCONNECT;
//			for(i=0; schempp1->connects[i] != NOARCPROTO; i++)
//			{
//				for(j=0; schempp2->connects[j] != NOARCPROTO; j++)
//				{
//					if (schempp1->connects[i] != schempp2->connects[j]) continue;
//					schempp1->connects[i]->userbits |= CANCONNECT;
//					break;
//				}
//			}
//
//			/* compute arc type and see if it is acceptable */
//			newap = us_figurenewaproto(ai->proto, newtech);
//			if (newap->tech == newtech && (newap->userbits&CANCONNECT) == 0)
//			{
//				/* not acceptable: see if there are any valid ones */
//				for(newap = newtech->firstarcproto; newap != NOARCPROTO; newap = newap->nextarcproto)
//					if ((newap->userbits&CANCONNECT) != 0) break;
//
//				/* none are valid: use universal */
//				if (newap == NOARCPROTO) newap = gen_universalarc;
//			}
//
//			/* determine new arc width */
//			newbits = ai->userbits;
//			if (newap == gen_universalarc)
//			{
//				newwid = 0;
//				univarcs = TRUE;
//				newbits &= ~(FIXED | FIXANG);
//			} else
//			{
//				defwid = ai->proto->nominalwidth - arcprotowidthoffset(ai->proto);
//				curwid = ai->width - arcwidthoffset(ai);
//				newwid = muldiv(newap->nominalwidth - arcprotowidthoffset(newap), curwid, defwid) +
//					arcprotowidthoffset(newap);
//				if (newwid <= 0) newwid = defaultarcwidth(newap);
//			}
//
//			/* find the endpoints of the arc */
//			x1 = muldiv(ai->end[0].xpos, newlambda, oldlambda);
//			y1 = muldiv(ai->end[0].ypos, newlambda, oldlambda);
//			shapeportpoly(end1, schempp1, poly1, FALSE);
//			x2 = muldiv(ai->end[1].xpos, newlambda, oldlambda);
//			y2 = muldiv(ai->end[1].ypos, newlambda, oldlambda);
//			shapeportpoly(end2, schempp2, poly2, FALSE);
//
//			/* see if the new arc can connect without end adjustment */
//			if (!isinside(x1, y1, poly1) || !isinside(x2, y2, poly2))
//			{
//				/* arc cannot be run exactly ... presume port centers */
//				portposition(end1, schempp1, &x1, &y1);
//				portposition(end2, schempp2, &x2, &y2);
//				if ((newbits & FIXANG) != 0)
//				{
//					/* old arc was fixed-angle so look for a similar-angle path */
//					reduceportpoly(poly1, end1, schempp1, newwid-arcprotowidthoffset(newap), -1);
//					getbbox(poly1, &lx1, &hx1, &ly1, &hy1);
//					reduceportpoly(poly2, end2, schempp2, newwid-arcprotowidthoffset(newap), -1);
//					getbbox(poly2, &lx2, &hx2, &ly2, &hy2);
//					if (!arcconnects(((ai->userbits&AANGLE) >> AANGLESH) * 10, lx1, hx1, ly1, hy1,
//						lx2, hx2, ly2, hy2, &tx1, &ty1, &tx2, &ty2)) badarcs++; else
//					{
//						x1 = tx1;   y1 = ty1;
//						x2 = tx2;   y2 = ty2;
//					}
//				}
//			}
//			/* create the new arc */
//			if (newarcinst(newap, newwid, newbits, end1, schempp1, x1, y1,
//				end2, schempp2, x2, y2, newcell) == NOARCINST)
//			{
//				ttyputmsg(_("Cell %s: can't run arc from node %s port %s at (%s,%s)"),
//					describenodeproto(newcell), describenodeinst(end1),
//						schempp1->protoname, latoa(x1, 0), latoa(y1, 0));
//				ttyputmsg(_("   to node %s port %s at (%s,%s)"), describenodeinst(end2),
//					schempp2->protoname, latoa(x2, 0), latoa(y2, 0));
//			}
//		}
//
//		/* print warning if arcs were made nonmanhattan */
//		if (badarcs != 0)
//			ttyputmsg(_("WARNING: %ld %s made not-fixed-angle in cell %s"), badarcs,
//				makeplural(_("arc"), badarcs), describenodeproto(newcell));
//		return(univarcs);
//	}
//
//	void us_tranplacenode(NODEINST *ni, NODEPROTO *newnp, NODEPROTO *newcell,
//		TECHNOLOGY *oldtech, TECHNOLOGY *newtech)
//	{
//		INTBIG lx, ly, hx, hy, nlx, nly, nhx, nhy, bx, by, length, width;
//		REGISTER INTBIG i, len, newsx, newsy, x1, y1, newlx, newhx, newly, newhy, *newtrace,
//			oldlambda, newlambda, thissizex, thissizey, defsizex, defsizey;
//		XARRAY trans;
//		REGISTER INTSML trn;
//		REGISTER PORTEXPINST *pexp;
//		REGISTER NODEINST *newni;
//		REGISTER PORTPROTO *pp, *pp2;
//		REGISTER VARIABLE *var;
//
//		oldlambda = el_curlib->lambda[oldtech->techindex];
//		newlambda = el_curlib->lambda[newtech->techindex];
//
//		/* scale edge offsets if this is a primitive */
//		trn = ni->transpose;
//		if (ni->proto->primindex != 0)
//		{
//			/* get offsets for new node type */
//			nodeprotosizeoffset(newnp, &nlx, &nly, &nhx, &nhy, NONODEPROTO);
//
//			/* special case for schematic transistors: get size from description */
//			if (((ni->proto->userbits&NFUNCTION) >> NFUNCTIONSH) == NPTRANS)
//			{
//				transistorsize(ni, &length, &width);
//				if (length < 0) length = newnp->highy - newnp->lowy - nly - nhy;
//				if (width < 0) width = newnp->highx - newnp->lowx - nlx - nhx;
//				lx = (ni->lowx + ni->highx - width) / 2;
//				hx = (ni->lowx + ni->highx + width) / 2;
//				ly = (ni->lowy + ni->highy - length) / 2;
//				hy = (ni->lowy + ni->highy + length) / 2;
//				trn = 1 - trn;
//
//				/* compute scaled size for new node */
//				newsx = muldiv(hx - lx, newlambda, oldlambda);
//				newsy = muldiv(hy - ly, newlambda, oldlambda);
//			} else
//			{
//				/* determine this node's percentage of the default node's size */
//				nodeprotosizeoffset(ni->proto, &lx, &ly, &hx, &hy, NONODEPROTO);
//				defsizex = (ni->proto->highx - hx) - (ni->proto->lowx + lx);
//				defsizey = (ni->proto->highy - hy) - (ni->proto->lowy + ly);
//
//				nodesizeoffset(ni, &lx, &ly, &hx, &hy);
//				thissizex = (ni->highx - hx) - (ni->lowx + lx);
//				thissizey = (ni->highy - hy) - (ni->lowy + ly);
//
//				/* compute size of new node that is the same percentage of its default */
//				newsx = muldiv((newnp->highx - nhx) - (newnp->lowx + nlx), thissizex, defsizex);
//				newsy = muldiv((newnp->highy - nhy) - (newnp->lowy + nly), thissizey, defsizey);
//
//				/* determine location of new node */
//				lx = ni->lowx + lx;   hx = ni->highx - hx;
//				ly = ni->lowy + ly;   hy = ni->highy - hy;
//			}
//
//			/* compute center of old node */
//			x1 = muldiv((hx + lx) / 2, newlambda, oldlambda);
//			y1 = muldiv((hy + ly) / 2, newlambda, oldlambda);
//
//			/* compute bounds of the new node */
//			newlx = x1 - newsx/2 - nlx;   newhx = newlx + newsx + nlx + nhx;
//			newly = y1 - newsy/2 - nly;   newhy = newly + newsy + nly + nhy;
//		} else
//		{
//			x1 = (newnp->highx+newnp->lowx)/2 - (ni->proto->highx+ni->proto->lowx)/2;
//			y1 = (newnp->highy+newnp->lowy)/2 - (ni->proto->highy+ni->proto->lowy)/2;
//			makeangle(ni->rotation, ni->transpose, trans);
//			xform(x1, y1, &bx, &by, trans);
//			newlx = ni->lowx + bx;   newhx = ni->highx + bx;
//			newly = ni->lowy + by;   newhy = ni->highy + by;
//			newlx += ((newhx-newlx) - (newnp->highx-newnp->lowx)) / 2;
//			newhx = newlx + newnp->highx - newnp->lowx;
//			newly += ((newhy-newly) - (newnp->highy-newnp->lowy)) / 2;
//			newhy = newly + newnp->highy - newnp->lowy;
//		}
//
//		/* create the node */
//		newni = newnodeinst(newnp, newlx, newhx, newly, newhy, trn, ni->rotation, newcell);
//		if (newni == NONODEINST) return;
//		newni->userbits |= (ni->userbits & (NEXPAND | WIPED | NSHORT));
//		ni->temp1 = (INTBIG)newni;
//		(void)copyvars((INTBIG)ni, VNODEINST, (INTBIG)newni, VNODEINST, FALSE);
//
//		/* copy "trace" information if there is any */
//		var = gettrace(ni);
//		if (var != NOVARIABLE)
//		{
//			len = getlength(var);
//			newtrace = emalloc((len * SIZEOFINTBIG), el_tempcluster);
//			if (newtrace == 0) return;
//			for(i=0; i<len; i++)
//				newtrace[i] = muldiv(((INTBIG *)var->addr)[i], newlambda, oldlambda);
//			(void)setvalkey((INTBIG)newni, VNODEINST, el_trace_key, (INTBIG)newtrace,
//				VINTEGER|VISARRAY|(len<<VLENGTHSH));
//			efree((CHAR *)newtrace);
//		}
//		endobjectchange((INTBIG)newni, VNODEINST);
//
//		/* re-export any ports on the node */
//		for(pexp = ni->firstportexpinst; pexp != NOPORTEXPINST; pexp = pexp->nextportexpinst)
//		{
//			pp = us_convport(ni, newni, pexp->proto);
//			pp2 = newportproto(newcell, newni, pp, pexp->exportproto->protoname);
//			if (pp2 == NOPORTPROTO) return;
//			pp2->userbits = (pp2->userbits & ~STATEBITS) | (pexp->exportproto->userbits & STATEBITS);
//			TDCOPY(pp2->textdescript, pexp->exportproto->textdescript);
//			if (copyvars((INTBIG)pexp->exportproto, VPORTPROTO, (INTBIG)pp2, VPORTPROTO, FALSE))
//				return;
//		}
//	}
//
//	/*
//	 * routine to determine the port to use on node "newni" assuming that it should
//	 * be the same as port "oldpp" on equivalent node "ni"
//	 */
//	PORTPROTO *us_convport(NODEINST *ni, NODEINST *newni, PORTPROTO *oldpp)
//	{
//		REGISTER PORTPROTO *pp, *npp;
//		REGISTER INTBIG oldfun, newfun;
//
//		if (newni->proto->primindex == 0)
//		{
//			/* cells can associate by comparing names */
//			pp = getportproto(newni->proto, oldpp->protoname);
//			if (pp != NOPORTPROTO) return(pp);
//		}
//
//		/* if functions are different, handle some special cases */
//		oldfun = (ni->proto->userbits&NFUNCTION) >> NFUNCTIONSH;
//		newfun = (newni->proto->userbits&NFUNCTION) >> NFUNCTIONSH;
//		if (oldfun != newfun)
//		{
//			if (oldfun == NPTRANS && isfet(newni->geom))
//			{
//				/* converting from stick-figure to layout */
//				pp = ni->proto->firstportproto;   npp = newni->proto->firstportproto;
//				if (pp == oldpp) return(npp);
//				pp = pp->nextportproto;           npp = npp->nextportproto;
//				if (pp == oldpp) return(npp);
//				pp = pp->nextportproto;           npp = npp->nextportproto->nextportproto;
//				if (pp == oldpp) return(npp);
//			}
//		}
//
//		/* associate by position in port list */
//		for(pp = ni->proto->firstportproto, npp = newni->proto->firstportproto;
//			pp != NOPORTPROTO && npp != NOPORTPROTO;
//				pp = pp->nextportproto, npp = npp->nextportproto)
//					if (pp == oldpp) return(npp);
//
//		/* special case again: one-port capacitors are OK */
//		if (oldfun == NPCAPAC && newfun == NPCAPAC) return(newni->proto->firstportproto);
//
//		/* association has failed: assume the first port */
//		ttyputmsg(_("No port association between %s, port %s and %s"),
//			describenodeproto(ni->proto), oldpp->protoname,
//				describenodeproto(newni->proto));
//		return(newni->proto->firstportproto);
//	}
//
//	/*
//	 * routine to determine the equivalent prototype in technology "newtech" for
//	 * node prototype "oldnp".
//	 */
//	ARCPROTO *us_figurenewaproto(ARCPROTO *oldap, TECHNOLOGY *newtech)
//	{
//		REGISTER INTBIG type;
//		REGISTER ARCPROTO *ap;
//
//		/* schematic wires become universal arcs */
//		if (oldap == sch_wirearc) return(gen_universalarc);
//
//		/* determine the proper association of this node */
//		type = (oldap->userbits & AFUNCTION) >> AFUNCTIONSH;
//		for(ap = newtech->firstarcproto; ap != NOARCPROTO; ap = ap->nextarcproto)
//			if ((INTBIG)((ap->userbits&AFUNCTION) >> AFUNCTIONSH) == type) break;
//		if (ap == NOARCPROTO)
//		{
//			ttyputmsg(_("No equivalent arc for %s"), describearcproto(oldap));
//			return(oldap);
//		}
//		return(ap);
//	}
//
//	/*
//	 * routine to determine the equivalent prototype in technology "newtech" for
//	 * node prototype "oldnp".
//	 */
//	NODEPROTO *us_figurenewnproto(NODEINST *oldni, TECHNOLOGY *newtech)
//	{
//		REGISTER INTBIG type, i, j, k;
//		REGISTER ARCPROTO *ap, *oap;
//		REGISTER INTBIG important, funct;
//		REGISTER NODEPROTO *np, *rnp, *oldnp;
//		REGISTER NODEINST *ni;
//		NODEINST node;
//		static POLYGON *poly = NOPOLYGON;
//
//		/* get a polygon */
//		(void)needstaticpolygon(&poly, 4, us_tool->cluster);
//
//		/* easy translation if complex or already in the proper technology */
//		oldnp = oldni->proto;
//		if (oldnp->primindex == 0 || oldnp->tech == newtech) return(oldnp);
//
//		/* if this is a layer node, check the layer functions */
//		type = nodefunction(oldni);
//		if (type == NPNODE)
//		{
//			/* get the polygon describing the first box of the old node */
//			(void)nodepolys(oldni, 0, NOWINDOWPART);
//			shapenodepoly(oldni, 0, poly);
//			important = LFTYPE | LFPSEUDO | LFNONELEC;
//			funct = layerfunction(oldnp->tech, poly->layer) & important;
//
//			/* now search for that function in the other technology */
//			for(np = newtech->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//			{
//				if (((np->userbits&NFUNCTION) >> NFUNCTIONSH) != NPNODE) continue;
//				ni = &node;   initdummynode(ni);
//				ni->proto = np;
//				(void)nodepolys(ni, 0, NOWINDOWPART);
//				shapenodepoly(ni, 0, poly);
//				if ((layerfunction(newtech, poly->layer)&important) == funct)
//					return(np);
//			}
//		}
//
//		/* see if one node in the new technology has the same function */
//		for(i = 0, np = newtech->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//			if ((INTBIG)((np->userbits&NFUNCTION) >> NFUNCTIONSH) == type)
//		{
//			rnp = np;   i++;
//		}
//		if (i == 1) return(rnp);
//
//		/* if there are too many matches, determine which is proper from arcs */
//		if (i > 1)
//		{
//			for(np = newtech->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//			{
//				if ((INTBIG)((np->userbits&NFUNCTION) >> NFUNCTIONSH) != type) continue;
//
//				/* see if this node has equivalent arcs */
//				for(j=0; oldnp->firstportproto->connects[j] != NOARCPROTO; j++)
//				{
//					oap = oldnp->firstportproto->connects[j];
//					if (oap->tech == gen_tech) continue;
//
//					for(k=0; np->firstportproto->connects[k] != NOARCPROTO; k++)
//					{
//						ap = np->firstportproto->connects[k];
//						if (ap->tech == gen_tech) continue;
//						if ((ap->userbits&AFUNCTION) == (oap->userbits&AFUNCTION)) break;
//					}
//					if (np->firstportproto->connects[k] == NOARCPROTO) break;
//				}
//				if (oldnp->firstportproto->connects[j] == NOARCPROTO) break;
//			}
//			if (np != NONODEPROTO)
//			{
//				rnp = np;
//				i = 1;
//			}
//		}
//
//		/* give up if it still cannot be determined */
//		if (i != 1)
//		{
//			if (oldnp->temp1 == 0)
//				ttyputmsg(_("Node %s (function %s) has no equivalent in the %s technology"),
//					describenodeproto(oldnp), nodefunctionname(type, oldni),
//						newtech->techname);
//			oldnp->temp1 = 1;
//			return(oldnp);
//		}
//		return(rnp);
//	}
}

