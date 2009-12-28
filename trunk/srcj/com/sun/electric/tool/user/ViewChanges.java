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
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortOriginal;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
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
import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.tool.generator.layout.TechType;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * Class for view-related changes to the circuit.
 */
public class ViewChanges
{
	// constructor, never used
	ViewChanges() {}

	/****************************** CREATE AND VIEW A CELL ******************************/

	/**
	 * Class to create a cell and display it in a new window.
	 */
	public static class CreateAndViewCell extends Job
	{
		private String cellName;
		private Library lib;
		private Cell c;

		public CreateAndViewCell(String cellName, Library lib)
		{
			super("Create and View a Cell", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cellName = cellName;
			this.lib = lib;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			c = Cell.makeInstance(lib, cellName);
			fieldVariableChanged("c");
			return true;
		}

		public void terminateOK()
		{
            WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (User.isShowCellsInNewWindow()) wf = null;
			if (wf == null) wf = WindowFrame.createEditWindow(c);
            wf.setCellWindow(c, null);
		}
	}

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
		private Dimension2D alignment;
		private boolean fromRight;

		protected FixOldMultiPageSchematics(List<Cell> multiPageCells, Dimension2D alignment)
		{
			super("Repair old-style Multi-Page Schematics", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.multiPageCells = multiPageCells;
			this.alignment = alignment;
			this.fromRight = User.isIncrementRightmostIndex();
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
					true, fromRight, true, alignment, null, null);

				// also copy any variables on the cell
				for(Iterator<Variable> vIt = cell.getVariables(); vIt.hasNext(); )
				{
					Variable var = vIt.next();
					if (!var.isDisplay()) continue;
					destCell.addVar(var.withOff(var.getXOff(), var.getYOff() + dY));
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

		// database can't change icon/schematic views
		if (cell.isIcon() || cell.isSchematic()) {
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"Cannot change view of icon/schematic",
					"Change cell view failed", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (newView == View.ICON || newView == View.SCHEMATIC) {
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"Cannot change " + cell.getView() + " to icon/schematic " + newView,
					"Change cell view failed", JOptionPane.ERROR_MESSAGE);
			return;
		}

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
			}
			return true;
		}

		public void terminateOK()
		{
			User.fixStaleCellReferences(idMapper);
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
                WindowFrame wf = WindowFrame.getCurrentWindowFrame();
    			if (User.isShowCellsInNewWindow()) wf = null;
    			if (wf == null) wf = WindowFrame.createEditWindow(skeletonCell);
                wf.setCellWindow(skeletonCell, null);
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
		Map<Export,Export> newPortMap = new HashMap<Export,Export>();
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
				skeletonCell, newOrient, null);
			if (newNi == null)
			{
				System.out.println("Cannot create node in this cell");
				return true;
			}

			// export the port from the node
			PortInst newPi = newNi.findPortInstFromProto(bottomPp);
			Export npp = Export.newInstance(skeletonCell, newPi, pp.getName(), pp.getCharacteristic());
			if (npp == null)
			{
				System.out.println("Could not create port " + pp.getName());
				return true;
			}
			npp.copyTextDescriptorFrom(pp, Export.EXPORT_NAME);
			npp.copyVarsFrom(pp);
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
		Map<Export,Network> netMap = new HashMap<Export,Network>();
		for(Iterator<Export> it = curCell.getExports(); it.hasNext(); )
		{
			Export e = it.next();
			Network net = netlist.getNetwork(e, 0);
			netMap.put(e, net);
		}

		int numPorts = curCell.getNumPorts();
		for(int i=0; i<numPorts; i++)
		{
			Export pp = curCell.getPort(i);
			Network net = netMap.get(pp);
			for(int j=i+1; j<numPorts; j++)
			{
				Export oPp = curCell.getPort(j);
				Network oNet = netMap.get(oPp);
				if (net != oNet) continue;

				Export newPp = newPortMap.get(pp);
				Export newOPp = newPortMap.get(oPp);
				if (newPp == null || newOPp == null) continue;
				ArcProto univ = Generic.tech().universal_arc;
				ArcInst newAI = ArcInst.makeInstance(univ, newPp.getOriginalPort(), newOPp.getOriginalPort());
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
			if (np != Generic.tech().essentialBoundsNode) continue;
			NodeInst newNi = NodeInst.makeInstance(np, ni.getAnchorCenter(),
				ni.getXSize(), ni.getYSize(), skeletonCell, ni.getOrient(), null);
			if (newNi == null)
			{
				System.out.println("Cannot create node in this cell");
				return true;
			}
			newNi.setHardSelect();
			if (np == Generic.tech().cellCenterNode) newNi.setVisInside();
		}

		// place an outline around the skeleton
		Rectangle2D bounds = curCell.getBounds();
		NodeInst boundNi = NodeInst.makeInstance(Generic.tech().invisiblePinNode,
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
		makeIconViewNoGUI(curCell, false, false);
	}

	public static void makeIconViewNoGUI(Cell curCell, boolean doItNow, boolean fixedValues)
	{
		if (!fixedValues)
		{
			IconParameters ip = new IconParameters();
			ip.initFromUserDefaults();
			new MakeIconView(curCell, User.getAlignmentToGrid(), User.getIconGenInstanceLocation(), ip, doItNow);
		}
		else
		{
			// in case of debugging mode, better to draw the body and leads
			IconParameters ip = new IconParameters();
			new MakeIconView(curCell, new Dimension2D.Double(0.05, 0.05), 0, ip, doItNow);
		}
	}

	private static class MakeIconView extends Job
	{
		private Cell curCell;
		private Dimension2D alignment;
		private int exampleLocation;
		private IconParameters ip;
		private NodeInst iconNode;
		private boolean doItNow;

		// get icon style controls
		private MakeIconView(Cell cell, Dimension2D alignment, int exampleLocation, IconParameters ip, boolean doItNow)
		{
			super("Make Icon View", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.curCell = cell;
			this.alignment = alignment;
			this.exampleLocation = exampleLocation;
			this.ip = ip;
			this.doItNow = doItNow;
			if (doItNow)
			{
			   try {doIt();} catch (Exception e) {e.printStackTrace();}
			}
			else
				startJob();
		}

		public boolean doIt() throws JobException
		{
			Cell iconCell = ip.makeIconForCell(curCell);
			if (iconCell == null) return false;

			// Check user preference to see if an instance should be made in the original schematic
			if (exampleLocation != 4)
			{
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
			}
			if (!doItNow)
				fieldVariableChanged("iconNode");
			return true;
		}

		public void terminateOK()
		{
			if (iconNode != null && !doItNow)
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
	 * Class to define parameters for automatic icon generation
	 */
	public static class IconParameters implements Serializable
	{
		/** length of leads from body to export */							double leadLength;
		/** spacing between leads (or exports) */							double leadSpacing;
		/** true to place exports by location in original cell */			boolean placeByCellLocation;
		/** true to place exports exactly by location in original cell */   boolean useExactLocation;
		/** true to reverse placement of exports */							boolean reverseIconExportOrder;
		/** true to draw an icon body (a rectangle) */						boolean drawBody;
		/** size (in units) of text on body */								double bodyTextSize;
		/** true to draw leads between the body and exports */				boolean drawLeads;
		/** true to place a cell-center in the icon */						boolean placeCellCenter;
		/** technology: 0=generic, 1=schematic */							int exportTech;
		/** text style: 0=centered, 1=inward, 2=outward */					int exportStyle;
		/** text location: 0=on body, 1=end of lead, 2=middle of lead */	int exportLocation;
		/** true to make exports "always drawn" */							boolean alwaysDrawn;
		/** side for input ports (when placeByCellLocation false) */		int inputSide;
		/** side for output ports (when placeByCellLocation false) */		int outputSide;
		/** side for bidir ports (when placeByCellLocation false) */		int bidirSide;
		/** side for power ports (when placeByCellLocation false) */		int pwrSide;
		/** side for ground ports (when placeByCellLocation false) */		int gndSide;
		/** side for clock ports (when placeByCellLocation false) */		int clkSide;
		/** rotation of input text (when placeByCellLocation false) */		int inputRot;
		/** rotation of output text (when placeByCellLocation false) */		int outputRot;
		/** rotation of bidir text (when placeByCellLocation false) */		int bidirRot;
		/** rotation of power text (when placeByCellLocation false) */		int pwrRot;
		/** rotation of ground text (when placeByCellLocation false) */		int gndRot;
		/** rotation of clock text (when placeByCellLocation false) */		int clkRot;
		/** rotation of top text (when placeByCellLocation true) */			int topRot;
		/** rotation of bottom text (when placeByCellLocation true) */		int bottomRot;
		/** rotation of left text (when placeByCellLocation true) */		int leftRot;
		/** rotation of right text (when placeByCellLocation true) */		int rightRot;

		public IconParameters()
		{
			boolean drawBodyAndLeads = Job.getDebug();
			leadLength = 2.0;
			leadSpacing = 2.0;
			placeByCellLocation = false;
			useExactLocation = false;
			reverseIconExportOrder = false;
			drawBody = drawBodyAndLeads;
			bodyTextSize = 1.0;
			drawLeads = drawBodyAndLeads;
			placeCellCenter = true;
			exportTech = 0;
			exportStyle = 1;
			exportLocation = 1;
			alwaysDrawn = false;
			inputSide = 0;
			outputSide = 1;
			bidirSide = 2;
			pwrSide = 3;
			gndSide = 3;
			clkSide = 0;
			inputRot = 0;
			outputRot = 0;
			bidirRot = 0;
			pwrRot = 0;
			gndRot = 0;
			clkRot = 0;
			topRot = 0;
			bottomRot = 0;
			leftRot = 0;
			rightRot = 0;
		}

		public void initFromUserDefaults()
		{
			leadLength = User.getIconGenLeadLength();
			leadSpacing = User.getIconGenLeadSpacing();
			placeByCellLocation = User.getIconGenExportPlacement() == 1;
			useExactLocation = User.getIconGenExportPlacementExact();
			reverseIconExportOrder = User.isIconGenReverseExportOrder();
			drawBody = User.isIconGenDrawBody();
			bodyTextSize = User.getIconGenBodyTextSize();
			drawLeads = User.isIconGenDrawLeads();
			placeCellCenter = User.isPlaceCellCenter();
			exportTech = User.getIconGenExportTech();
			exportStyle = User.getIconGenExportStyle();
			exportLocation = User.getIconGenExportLocation();
			alwaysDrawn = User.isIconsAlwaysDrawn();
			inputSide = User.getIconGenInputSide();
			outputSide = User.getIconGenOutputSide();
			bidirSide = User.getIconGenBidirSide();
			pwrSide = User.getIconGenPowerSide();
			gndSide = User.getIconGenGroundSide();
			clkSide = User.getIconGenClockSide();
			inputRot = User.getIconGenInputRot();
			outputRot = User.getIconGenOutputRot();
			bidirRot = User.getIconGenBidirRot();
			pwrRot = User.getIconGenPowerRot();
			gndRot = User.getIconGenGroundRot();
			clkRot = User.getIconGenClockRot();
			topRot = User.getIconGenTopRot();
			bottomRot = User.getIconGenBottomRot();
			leftRot = User.getIconGenLeftRot();
			rightRot = User.getIconGenRightRot();
		}

		/**
		 * Method to create an icon for a cell.
		 * @param curCell the cell to turn into an icon.
		 * @return the icon cell (null on error).
		 */
		public Cell makeIconForCell(Cell curCell)
			throws JobException
		{
			// create the new icon cell
			String iconCellName = curCell.getName() + "{ic}";
			Cell iconCell = Cell.makeInstance(curCell.getLibrary(), iconCellName);
			if (iconCell == null)
				throw new JobException("Cannot create Icon cell " + iconCellName);
			iconCell.setWantExpanded();

			// determine number of ports on each side
			int leftSide = 0, rightSide = 0, bottomSide = 0, topSide = 0;
			Map<Export,Integer> portIndex = new HashMap<Export,Integer>();
			Map<Export,Integer> portSide = new HashMap<Export,Integer>();
			Map<Export,Integer> portRotation = new HashMap<Export,Integer>();

			// make a sorted list of exports
			List<Export> exportList = new ArrayList<Export>();
			for(Iterator<PortProto> it = curCell.getPorts(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				if (pp.isBodyOnly()) continue;
				exportList.add(pp);
			}
			if (placeByCellLocation)
			{
				// place exports according to their location in the cell
				Collections.sort(exportList, new ExportsByAngle());

				// figure out how many exports go on each side
				int numExports = exportList.size();
				leftSide = rightSide = topSide = bottomSide = numExports / 4;
				if (leftSide + rightSide + topSide + bottomSide < numExports) leftSide++;
				if (leftSide + rightSide + topSide + bottomSide < numExports) rightSide++;
				if (leftSide + rightSide + topSide + bottomSide < numExports) topSide++;

				// cache the location of each export
				Map<Export,Point2D> portCenters = new HashMap<Export,Point2D>();
				for(int i=0; i<numExports; i++)
				{
					Export pp = exportList.get(i);
					portCenters.put(pp, pp.getOriginalPort().getCenter());
				}

				// make an array of points in the middle of each side
				ERectangle bounds = curCell.getBounds();
				Point2D leftPoint = new Point2D.Double(bounds.getCenterX() - bounds.getWidth(), bounds.getCenterY());
				Point2D rightPoint = new Point2D.Double(bounds.getCenterX() + bounds.getWidth(), bounds.getCenterY());
				Point2D topPoint = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY() + bounds.getWidth());
				Point2D bottomPoint = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY() - bounds.getWidth());
				Point2D[] sidePoints = new Point2D[numExports];
				int fill = 0;
				for(int i=0; i<leftSide; i++) sidePoints[fill++] = leftPoint;
				for(int i=0; i<topSide; i++) sidePoints[fill++] = topPoint;
				for(int i=0; i<rightSide; i++) sidePoints[fill++] = rightPoint;
				for(int i=0; i<bottomSide; i++) sidePoints[fill++] = bottomPoint;

				// rotate the points and find the rotation with the least distance to the side points
				double [] totDist = new double[numExports];
				for(int i=0; i<numExports; i++)
				{
					totDist[i] = 0;
					for(int j=0; j<numExports; j++)
					{
						Point2D ppCtr = portCenters.get(exportList.get((j+i)%numExports));
						double dist = ppCtr.distance(sidePoints[j]);
						totDist[i] += dist;
					}
				}
				double bestDist = Double.MAX_VALUE;
				int bestIndex = -1;
				for(int i=0; i<numExports; i++)
				{
					if (totDist[i] < bestDist)
					{
						bestDist = totDist[i];
						bestIndex = i;
					}
				}

				// assign ports along each side
				for(int i=0; i<leftSide; i++)
				{
					Export pp = exportList.get((i+bestIndex)%numExports);
					portSide.put(pp, new Integer(0));
					portIndex.put(pp, new Integer(leftSide-i-1));
					portRotation.put(pp, new Integer(leftRot));
				}
				for(int i=0; i<topSide; i++)
				{
					Export pp = exportList.get((i+leftSide+bestIndex)%numExports);
					portSide.put(pp, new Integer(2));
					portIndex.put(pp, new Integer(topSide-i-1));
					portRotation.put(pp, new Integer(topRot));
				}
				for(int i=0; i<rightSide; i++)
				{
					Export pp = exportList.get((i+leftSide+topSide+bestIndex)%numExports);
					portSide.put(pp, new Integer(1));
					portIndex.put(pp, new Integer(i));
					portRotation.put(pp, new Integer(rightRot));
				}
				for(int i=0; i<bottomSide; i++)
				{
					Export pp = exportList.get((i+leftSide+topSide+rightSide+bestIndex)%numExports);
					portSide.put(pp, new Integer(3));
					portIndex.put(pp, new Integer(i));
					portRotation.put(pp, new Integer(bottomRot));
				}
			} else
			{
				// place exports according to their characteristics
				if (reverseIconExportOrder)
					Collections.reverse(exportList);
				for(Export pp : exportList)
				{
					int index = iconPosition(pp);
					portSide.put(pp, new Integer(index));
					switch (index)
					{
						case 0: portIndex.put(pp, new Integer(leftSide++));    break;
						case 1: portIndex.put(pp, new Integer(rightSide++));   break;
						case 2: portIndex.put(pp, new Integer(topSide++));     break;
						case 3: portIndex.put(pp, new Integer(bottomSide++));  break;
					}
					int rotation = iconTextRotation(pp, inputRot, outputRot, bidirRot, pwrRot, gndRot, clkRot);
					portRotation.put(pp, new Integer(rotation));
				}
			}

			// determine the size of the "black box" core
			double xSize, ySize;
			if (placeByCellLocation && useExactLocation)
			{
				xSize = curCell.getDefWidth();
				ySize = curCell.getDefHeight();
			} else
			{
				ySize = Math.max(Math.max(leftSide, rightSide), 5) * leadSpacing;
				xSize = Math.max(Math.max(topSide, bottomSide), 3) * leadSpacing;
			}

			// create the "black box"
			NodeInst bbNi = null;
			if (drawBody)
			{
				bbNi = NodeInst.newInstance(Artwork.tech().openedThickerPolygonNode, new Point2D.Double(0,0), xSize, ySize, iconCell);
				if (bbNi == null) return null;
				EPoint [] boxOutline = new EPoint[5];
				if (placeByCellLocation && useExactLocation)
				{
					boxOutline[0] = new EPoint(curCell.getBounds().getMinX(), curCell.getBounds().getMinY());
					boxOutline[1] = new EPoint(curCell.getBounds().getMinX(), curCell.getBounds().getMaxY());
					boxOutline[2] = new EPoint(curCell.getBounds().getMaxX(), curCell.getBounds().getMaxY());
					boxOutline[3] = new EPoint(curCell.getBounds().getMaxX(), curCell.getBounds().getMinY());
					boxOutline[4] = new EPoint(curCell.getBounds().getMinX(), curCell.getBounds().getMinY());
				} else
				{
					boxOutline[0] = new EPoint(-xSize/2, -ySize/2);
					boxOutline[1] = new EPoint(-xSize/2,  ySize/2);
					boxOutline[2] = new EPoint( xSize/2,  ySize/2);
					boxOutline[3] = new EPoint( xSize/2, -ySize/2);
					boxOutline[4] = new EPoint(-xSize/2, -ySize/2);
				}
				bbNi.setTrace(boxOutline);

				// put the original cell name on it
				TextDescriptor td = TextDescriptor.getAnnotationTextDescriptor().withRelSize(bodyTextSize);
				bbNi.newVar(Schematics.SCHEM_FUNCTION, curCell.getName(), td);
			}

			// place pins around the Black Box
			int total = 0;
			for(Export pp : exportList)
			{
				// determine location and side of the port
				int portPosition = portIndex.get(pp).intValue();
				int index = portSide.get(pp).intValue();
				double spacing = leadSpacing;
				double xPos = 0, yPos = 0;
				double xBBPos = 0, yBBPos = 0;
				if (placeByCellLocation && useExactLocation)
				{
					xBBPos = xPos = pp.getOriginalPort().getCenter().getX();
					yBBPos = yPos = pp.getOriginalPort().getCenter().getY();
				} else
				{
					switch (index)
					{
						case 0:		// left side
							xBBPos = -xSize/2;
							xPos = xBBPos - leadLength;
							if (leftSide*2 < rightSide) spacing = leadSpacing * 2;
							yBBPos = yPos = ySize/2 - ((ySize - (leftSide-1)*spacing) / 2 + portPosition * spacing);
							break;
						case 1:		// right side
							xBBPos = xSize/2;
							xPos = xBBPos + leadLength;
							if (rightSide*2 < leftSide) spacing = leadSpacing * 2;
							yBBPos = yPos = ySize/2 - ((ySize - (rightSide-1)*spacing) / 2 + portPosition * spacing);
							break;
						case 2:		// top
							if (topSide*2 < bottomSide) spacing = leadSpacing * 2;
							xBBPos = xPos = xSize/2 - ((xSize - (topSide-1)*spacing) / 2 + portPosition * spacing);
							yBBPos = ySize/2;
							yPos = yBBPos + leadLength;
							break;
						case 3:		// bottom
							if (bottomSide*2 < topSide) spacing = leadSpacing * 2;
							xBBPos = xPos = xSize/2 - ((xSize - (bottomSide-1)*spacing) / 2 + portPosition * spacing);
							yBBPos = -ySize/2;
							yPos = yBBPos - leadLength;
							break;
					}
				}

				int rotation = portRotation.get(pp).intValue();
				if (makeIconExport(pp, index, xPos, yPos, xBBPos, yBBPos, iconCell,
					exportTech, drawLeads, exportStyle, exportLocation, rotation, alwaysDrawn))
						total++;
			}

			// if no body, leads, or cell center is drawn, and there is only 1 export, add more
			if (!drawBody && !drawLeads && placeCellCenter && total <= 1)
			{
				NodeInst.newInstance(Generic.tech().invisiblePinNode, new Point2D.Double(0,0), xSize, ySize, iconCell);
			}

			return iconCell;
		}

		/**
		 * Method to determine the side of the icon that port "pp" belongs on.
		 */
		private int iconPosition(Export pp)
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
			if (character.isClock()) return clkSide;
			return inputSide;
		}
	}

	/**
	 * Comparator class for sorting Exports by their angle about the cell center.
	 */
	private static class ExportsByAngle implements Comparator<Export>
	{
		/**
		 * Method to sort Exports by their angle about the cell center.
		 */
		public int compare(Export p1, Export p2)
		{
			Cell cell = p1.getParent();
			ERectangle bounds = cell.getBounds();
			Point2D cellCtr = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
			Point2D p1Ctr = p1.getOriginalPort().getCenter();
			Point2D p2Ctr = p2.getOriginalPort().getCenter();
			double angle1 = DBMath.figureAngleRadians(cellCtr, p1Ctr);
			double angle2 = DBMath.figureAngleRadians(cellCtr, p2Ctr);
			if (angle1 < angle2) return 1;
			if (angle1 > angle2) return -1;
			return 0;
		}
	}

	/**
	 * Helper method to create an export in an icon.
	 * @param pp the Export to build.
	 * @param index the side (0: left, 1: right, 2: top, 3: bottom).
	 * @param xPos the export location
	 * @param yPos the export location
	 * @param xBBPos the central box location
	 * @param yBBPos the central box location.
	 * @param np the cell in which to create the export.
	 * @param exportTech the technology to use (generic or schematic)
	 * @param drawLeads true to draw leads on the icon
	 * @param exportStyle the icon style
	 * @param exportLocation
	 * @param textRotation
	 * @param alwaysDrawn true to make export text be "always drawn"
	 * @return true if the export was created.
	 */
	public static boolean makeIconExport(Export pp, int index,
		double xPos, double yPos, double xBBPos, double yBBPos, Cell np,
		int exportTech, boolean drawLeads, int exportStyle, int exportLocation, int textRotation, boolean alwaysDrawn)
	{
		// presume "universal" exports (Generic technology)
		NodeProto pinType = Generic.tech().universalPinNode;
		double pinSizeX = 0, pinSizeY = 0;
		if (exportTech != 0)
		{
			// instead, use "schematic" exports (Schematic Bus Pins)
			pinType = Schematics.tech().busPinNode;
			pinSizeX = pinType.getDefWidth();
			pinSizeY = pinType.getDefHeight();
		}

		// determine the type of wires used for leads
		ArcProto wireType = Schematics.tech().wire_arc;
		if (pp.getBasePort().connectsTo(Schematics.tech().bus_arc) && pp.getNameKey().isBus())
		{
			wireType = Schematics.tech().bus_arc;
			pinType = Schematics.tech().busPinNode;
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
		Export port = Export.newInstance(np, pi, pp.getName(), pp.getCharacteristic());
		if (port != null)
		{
			TextDescriptor td = port.getTextDescriptor(Export.EXPORT_NAME);
			if (textRotation != 0) td = td.withRotation(TextDescriptor.Rotation.getRotationAt(textRotation));
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
			port.setAlwaysDrawn(alwaysDrawn);
			port.copyVarsFrom(pp);
		}

		// add lead if requested
		if (drawLeads)
		{
			pinType = wireType.findPinProto();
			if (pinType == Schematics.tech().busPinNode)
				pinType = Generic.tech().invisiblePinNode;
			double wid = pinType.getDefWidth();
			double hei = pinType.getDefHeight();
			NodeInst ni = NodeInst.newInstance(pinType, new Point2D.Double(xBBPos, yBBPos), wid, hei, np);
			if (ni != null)
			{
				PortInst head = ni.getOnlyPortInst();
				PortInst tail = pinNi.getOnlyPortInst();
				ArcInst ai = ArcInst.makeInstance(wireType,
					head, tail, new Point2D.Double(xBBPos, yBBPos),
						new Point2D.Double(xPos, yPos), null);
				if (ai != null && wireType == Schematics.tech().bus_arc)
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
	public static int iconTextRotation(Export pp, int inputRot, int outputRot, int bidirRot, int pwrRot, int gndRot, int clkRot)
	{
		PortCharacteristic character = pp.getCharacteristic();

		// special detection for power and ground ports
		if (pp.isPower()) character = PortCharacteristic.PWR;
		if (pp.isGround()) character = PortCharacteristic.GND;

		// see which side this type of port sits on
		if (character == PortCharacteristic.IN) return inputRot;
		if (character == PortCharacteristic.OUT) return outputRot;
		if (character == PortCharacteristic.BIDIR) return bidirRot;
		if (character == PortCharacteristic.PWR) return pwrRot;
		if (character == PortCharacteristic.GND) return gndRot;
		if (character.isClock()) return clkRot;
		return inputRot;
	}

	/****************************** CONVERT TO SCHEMATICS ******************************/

	/**
	 * Method to converts the current Cell into a schematic.
	 */
	public static void makeSchematicView()
	{
		Cell oldCell = WindowFrame.needCurCell();
		if (oldCell == null) return;
		new MakeSchematicView(oldCell);
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
                WindowFrame wf = WindowFrame.getCurrentWindowFrame();
    			if (User.isShowCellsInNewWindow()) wf = null;
    			if (wf == null) wf = WindowFrame.createEditWindow(newCell);
                wf.setCellWindow(newCell, null);
			}
		}
	}

	private static Cell convertSchematicCell(Cell oldCell)
	{
		// create cell in new technology
		Cell newCell = makeNewCell(oldCell.getName(), View.SCHEMATIC, oldCell, null);
		if (newCell == null) return null;

		// create the parts in this cell
		Map<NodeInst,NodeInst> newNodes = new HashMap<NodeInst,NodeInst>();
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
			if ((GenMath.figureAngle(tailPt, headPt)%450) == 0) ai.setFixedAngle(true);
		}
		return newCell;
	}

	/**
	 * Method to create a new cell called "newcellname" that is to be the
	 * equivalent to an old cell in "cell".  The view type of the new cell is
	 * in "newcellview" and the view type of the old cell is in "cellview"
	 */
	private static Cell makeNewCell(String newCellName, View newCellView, Cell cell, Library lib)
	{
		// create the new cell
		String cellName = newCellName;
		if (newCellView.getAbbreviation().length() > 0)
		{
			cellName = newCellName + newCellView.getAbbreviationExtension();
		}
		if (lib == null) lib = cell.getLibrary();
		Cell newCell = Cell.makeInstance(lib, cellName);
		if (newCell == null)
			System.out.println("Could not create cell: " + cellName); else
				System.out.println("Creating new cell: " + cellName + " from cell " + cell.describe(false));
		return newCell;
	}

	private static void buildSchematicNodes(Cell cell, Cell newCell, Map<NodeInst,NodeInst> newNodes)
	{
		// for each node, create a new node in the newcell, of the correct logical type.
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst mosNI = it.next();
			PrimitiveNode.Function type = getNodeType(mosNI);
			NodeInst schemNI = null;
			if (type == PrimitiveNode.Function.UNKNOWN) continue;
			if (type.isPin())
			{
				// compute new x, y coordinates
				NodeProto prim = Schematics.tech().wirePinNode;
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
				NodeProto prim = Schematics.tech().transistorNode;
				int bits = Schematics.getPrimitiveFunctionBits(mosNI.getFunction());
				schemNI = makeSchematicNode(prim, mosNI, prim.getDefWidth(), prim.getDefHeight(), rotate, bits, newCell);

				// add in the size
				TransistorSize ts = mosNI.getTransistorSize(VarContext.globalContext);
				if (ts != null)
				{
					if (mosNI.getFunction().isFET())
					{
						// set length/width
						TextDescriptor td = TextDescriptor.getNodeTextDescriptor().withRelSize(0.5).withOff(-0.5, -1);
						schemNI.newVar(Schematics.ATTR_LENGTH, new Double(ts.getDoubleLength()), td);
						td = TextDescriptor.getNodeTextDescriptor().withRelSize(1).withOff(0.5, -1);
						schemNI.newVar(Schematics.ATTR_WIDTH, new Double(ts.getDoubleWidth()), td);
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

					Export schemPP = Export.newInstance(newCell, schemPI, mosPP.getName(), mosPP.getCharacteristic());
					if (schemPP != null)
					{
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
		return newNI;
	}

	/**
	 * for each arc in cell, find the ends in the new technology, and
	 * make a new arc to connect them in the new cell.
	 */
	private static void buildSchematicArcs(Cell cell, Cell newcell, Map<NodeInst,NodeInst> newNodes)
	{
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst mosAI = it.next();
			NodeInst mosHeadNI = mosAI.getHeadPortInst().getNodeInst();
			NodeInst mosTailNI = mosAI.getTailPortInst().getNodeInst();
			NodeInst schemHeadNI = newNodes.get(mosHeadNI);
			NodeInst schemTailNI = newNodes.get(mosTailNI);
			if (schemHeadNI == null || schemTailNI == null) continue;
			PortInst schemHeadPI = convertPort(mosHeadNI, mosAI.getHeadPortInst().getPortProto(), schemHeadNI);
			PortInst schemTailPI = convertPort(mosTailNI, mosAI.getTailPortInst().getPortProto(), schemTailNI);
			if (schemHeadPI == null || schemTailPI == null) continue;

			// create the new arc
			ArcInst schemAI = ArcInst.makeInstanceBase(Schematics.tech().wire_arc, 0, schemHeadPI, schemTailPI, null, null, mosAI.getName());
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
		if (fun.isPin())
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

//	private static final int MAXADJUST = 5;
//
//	private static void makeArcsManhattan(Cell newCell)
//	{
//		// copy the list of nodes in the cell
//		List<NodeInst> nodesInCell = new ArrayList<NodeInst>();
//		for(Iterator<NodeInst> it = newCell.getNodes(); it.hasNext(); )
//			nodesInCell.add(it.next());
//
//		// examine all nodes and adjust them
//		double [] x = new double[MAXADJUST];
//		double [] y = new double[MAXADJUST];
//		for(NodeInst ni : nodesInCell)
//		{
//			if (ni.isCellInstance()) continue;
//			PrimitiveNode.Function fun = ni.getFunction();
//			if (fun != PrimitiveNode.Function.PIN) continue;
//
//			// see if this pin can be adjusted so that all wires are manhattan
//			int count = 0;
//			for(Iterator<Connection> aIt = ni.getConnections(); aIt.hasNext(); )
//			{
//				Connection con = aIt.next();
//				ArcInst ai = con.getArc();
//				int otherEnd = 1 - con.getEndIndex();
//				if (con.getPortInst().getNodeInst() == ai.getPortInst(otherEnd).getNodeInst()) continue;
//				x[count] = ai.getLocation(otherEnd).getX();
//				y[count] = ai.getLocation(otherEnd).getY();
//				count++;
//				if (count >= MAXADJUST) break;
//			}
//			if (count == 0) continue;
//
//			// now adjust for all these points
//			double xp = ni.getAnchorCenterX();
//			double yp = ni.getAnchorCenterY();
//			double bestDist = Double.MAX_VALUE;
//			double bestX = 0, bestY = 0;
//			for(int i=0; i<count; i++) for(int j=0; j<count; j++)
//			{
//				double dist = Math.abs(xp - x[i]) + Math.abs(yp - y[j]);
//				if (dist > bestDist) continue;
//				bestDist = dist;
//				bestX = x[i];   bestY = y[j];
//			}
//
//			// if there was a better place, move the node
//			if (bestDist != Double.MAX_VALUE)
//				ni.move(bestX-xp, bestY-yp);
//		}
//	}

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
		if (fun.isPin() || fun.isContact() ||
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
		new MakeNewViewDialog(oldCell, wnd);
	}

	/**
	 * Class to handle the "Make New View" dialog.
	 */
	public static class MakeNewViewDialog extends EDialog
	{
		private Cell oldCell;
		private VarContext context;
		private Technology oldTech;
		private JComboBox newTechnology, standardCellLibrary;
		private JCheckBox putInSeparateLibrary;
		private JTextField separateLibraryName;
		private JLabel jLabel2;

		/** Creates new form */
		private MakeNewViewDialog(Cell oldCell, EditWindow wnd)
		{
			super(TopLevel.getCurrentJFrame(), false);
			this.oldCell = oldCell;
			initComponents();

			context = wnd.getVarContext();
			if (context == null) context = VarContext.globalContext;

			sepLibChanged();
			finishInitialization();
			pack();
			setVisible(true);
		}

		protected void escapePressed() { closeDialog(); }

	    private void initComponents()
	    {
	        getContentPane().setLayout(new GridBagLayout());
	        setTitle("Make New View");
	        setName("");
	        addWindowListener(new WindowAdapter() {
	            public void windowClosing(WindowEvent evt) { closeDialog(); }
	        });
	        GridBagConstraints gbc;

	        JLabel jLabel1 = new JLabel("Technology of new cell:");
	        gbc = new GridBagConstraints();
	        gbc.gridx = 0;   gbc.gridy = 0;
	        gbc.anchor = GridBagConstraints.WEST;
	        gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(jLabel1, gbc);

	        newTechnology = new JComboBox();
	        gbc = new GridBagConstraints();
	        gbc.gridx = 1;   gbc.gridy = 0;
	        gbc.fill = GridBagConstraints.HORIZONTAL;
	        gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(newTechnology, gbc);

	        // show the list of technologies to convert to
			oldTech = oldCell.getTechnology();
			for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
			{
				Technology tech = it.next();
				if (tech == oldTech) continue;
				if (tech.isLayout())
					newTechnology.addItem(tech.getTechName());
			}


	        putInSeparateLibrary = new JCheckBox("Place new circuitry in a separate library");
	        putInSeparateLibrary.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent evt) { sepLibChanged(); }
	        });
	        gbc = new GridBagConstraints();
	        gbc.gridx = 0;   gbc.gridy = 1;
	        gbc.gridwidth = 2;
	        gbc.anchor = GridBagConstraints.WEST;
	        gbc.insets = new Insets(4, 4, 1, 4);
	        getContentPane().add(putInSeparateLibrary, gbc);

	        jLabel2 = new JLabel("Library for new circuitry:");
	        gbc = new GridBagConstraints();
	        gbc.gridx = 0;   gbc.gridy = 2;
	        gbc.anchor = GridBagConstraints.WEST;
	        gbc.insets = new Insets(1, 20, 4, 4);
	        getContentPane().add(jLabel2, gbc);

	        separateLibraryName = new JTextField();
			EDialog.makeTextFieldSelectAllOnTab(separateLibraryName);
	        gbc = new GridBagConstraints();
	        gbc.gridx = 1;   gbc.gridy = 2;
	        gbc.fill = GridBagConstraints.HORIZONTAL;
	        gbc.insets = new Insets(1, 4, 4, 4);
	        getContentPane().add(separateLibraryName, gbc);


	        if (oldTech == Schematics.tech())
	        {
		        JLabel jLabel3 = new JLabel("Standard Cell library:");
		        gbc = new GridBagConstraints();
		        gbc.gridx = 0;   gbc.gridy = 3;
		        gbc.anchor = GridBagConstraints.WEST;
		        gbc.insets = new Insets(4, 4, 4, 4);
		        getContentPane().add(jLabel3, gbc);

		        standardCellLibrary = new JComboBox();
		        gbc = new GridBagConstraints();
		        gbc.gridx = 1;   gbc.gridy = 3;
		        gbc.fill = GridBagConstraints.HORIZONTAL;
		        gbc.insets = new Insets(4, 4, 4, 4);
		        getContentPane().add(standardCellLibrary, gbc);

		        // show a list of libraries that can hold standard cells
				standardCellLibrary.addItem("");
				for(Library lib : Library.getVisibleLibraries())
				{
					standardCellLibrary.addItem(lib.getName());
				}
	        }


	        JButton cancel = new JButton("Cancel");
	        cancel.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent evt) { closeDialog(); }
	        });
	        gbc = new GridBagConstraints();
	        gbc.gridx = 0;   gbc.gridy = 4;
	        gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(cancel, gbc);

		    JButton ok = new JButton("OK");
	        ok.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent evt) { ok(); }
	        });
			getRootPane().setDefaultButton(ok);
	        gbc = new GridBagConstraints();
	        gbc.gridx = 1;   gbc.gridy = 4;
	        gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(ok, gbc);

	        pack();
	    }

	    private void sepLibChanged()
	    {
    		jLabel2.setEnabled(putInSeparateLibrary.isSelected());
    		separateLibraryName.setEditable(putInSeparateLibrary.isSelected());
	    }

	    private void ok()
		{
			String techName = (String)newTechnology.getSelectedItem();
			Technology newTech = Technology.findTechnology(techName);
			String newLibName = "";
			if (putInSeparateLibrary.isSelected()) newLibName = separateLibraryName.getText();
			Library stdCellLib = null;
			if (oldTech == Schematics.tech())
			{
				String stdCellLibName = (String)standardCellLibrary.getSelectedItem();
				if (stdCellLibName.length() > 0) stdCellLib = Library.findLibrary(stdCellLibName);
			}
			new MakeLayoutView(oldCell, oldTech, newTech, stdCellLib, context, newLibName);
			closeDialog();
		}
	}

	/**
	 * Class to generate the alternate view of a cell.
	 */
	public static class MakeLayoutView extends Job
	{
		private Cell oldCell;
		private Technology oldTech, newTech;
		private Library stdCellLib;
		private VarContext context;
		private String newLibName;
		private List<Cell> createdCells;

		public MakeLayoutView(Cell oldCell, Technology oldTech, Technology newTech, Library stdCellLib, VarContext context, String newLibName)
		{
			super("Make Alternate Layout", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.oldCell = oldCell;
			this.oldTech = oldTech;
			this.newTech = newTech;
			this.stdCellLib = stdCellLib;
			this.context = context;
			this.newLibName = newLibName;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// convert the cell and all subcells
			Library newLib = Library.getCurrent();
			if (newLibName.length() > 0)
			{
				newLib = Library.findLibrary(newLibName);
				if (newLib == null) newLib = Library.newInstance(newLibName, null);
			}
			createdCells = new ArrayList<Cell>();
			MakeLayoutVisitor visitor = new MakeLayoutVisitor(oldTech, newTech, oldCell.getLibrary(), stdCellLib, createdCells, newLib);
			HierarchyEnumerator.enumerateCell(oldCell, context, visitor, Netlist.ShortResistors.ALL);
			fieldVariableChanged("createdCells");
			return true;
		}

		public void terminateOK()
		{
			Cell showCell = null;
			for(Cell cell : createdCells)
			{
				showCell = cell;
//				System.out.println("Created cell " + cell.describe(true) + " in technology " + newTech.getTechName());
			}
			if (showCell != null)
			{
                WindowFrame wf = WindowFrame.getCurrentWindowFrame();
    			if (User.isShowCellsInNewWindow()) wf = null;
    			if (wf == null) wf = WindowFrame.createEditWindow(showCell);
                wf.setCellWindow(showCell, null);
			}
		}

		private static class Info extends HierarchyEnumerator.CellInfo
		{
			private Map<Nodable,Cell> generatedCells;
			public Info()
			{
				generatedCells = new HashMap<Nodable,Cell>();
			}
		}

		private static class MakeLayoutVisitor extends HierarchyEnumerator.Visitor
		{
			private Technology oldTech, newTech;
			private Library defaultLib, stdCellLib;
			private Map<Cell,Cell> convertedCells;
			private StdCellParams stdCell;
			private List<Cell> createdCells;
			private Library newLib;

			private MakeLayoutVisitor(Technology oldTech, Technology newTech, Library defaultLib, Library stdCellLib,
				List<Cell> createdCells, Library newLib)
			{
				this.oldTech = oldTech;
				this.newTech = newTech;
				this.defaultLib = defaultLib;
				this.stdCellLib = stdCellLib;
				this.createdCells = createdCells;
				this.newLib = newLib;
				convertedCells = new HashMap<Cell,Cell>();
				TechType.TechTypeEnum type = null;

				// TODO This code "steals" the intended functionality and diverts it into the gate generator
				if (oldTech == Schematics.tech())
				{
					stdCell = null;
					if      (newTech == Technology.getMocmosTechnology()) type = TechType.TechTypeEnum.MOCMOS;
					else if (newTech == Technology.findTechnology("TSMC180")) type = TechType.TechTypeEnum.TSMC180;
					else if (newTech == Technology.findTechnology("CMOS90")) type = TechType.TechTypeEnum.CMOS90;
					if (type != null)
					{
						Tech.setTechType(type.getTechType());
						if (type == TechType.TechTypeEnum.CMOS90)
						{
							stdCell = GateLayoutGenerator.sportParams(false);
						} else
						{
							stdCell = GateLayoutGenerator.dividerParams(type, false);
						}
					}
				}
			}

			public HierarchyEnumerator.CellInfo newCellInfo() { return new Info(); }

			private static class Conn
			{
				private Nodable no;
				private PortProto pp;
				private Name portName;
				private Conn(Nodable no, PortProto pp, Name portName)
				{
					this.no = no; this.pp = pp; this.portName = portName;
				}
			}

			public boolean enterCell(HierarchyEnumerator.CellInfo info)
			{
				Cell oldCell = info.getCell();
				if (convertedCells.containsKey(oldCell)) return false;
				if (stdCell != null)
				{
					Info myInfo = (Info)info;
					myInfo.generatedCells = GateLayoutGenerator.generateLayoutFromSchematics(
						oldCell.getLibrary(), oldCell, info.getContext(), stdCell, true);
				}
				return true;
			}

			public boolean visitNodeInst(Nodable ni, HierarchyEnumerator.CellInfo info)
			{
				Cell layCell = getLayoutCell(ni, (Info)info);
				if (layCell == null) return true;
				return false;
			}

			private Cell getLayoutCell(Nodable no, Info myInfo)
			{
				if (!no.isCellInstance()) return null;
				Cell layCell = null;
				Cell subCell = (Cell)no.getProto();
				layCell = myInfo.generatedCells.get(no);
				if (layCell == null)
					layCell = convertedCells.get(subCell);
				if (layCell == null)
				{
					if (oldTech == Schematics.tech())
					{
						// see if it already exists
						for (Iterator<Cell> it = subCell.getCellGroup().getCells(); it.hasNext(); )
						{
							Cell c = it.next();
							if (c.getView() == View.LAYOUT)
							{
								layCell = c;
								break;
							}
						}
						if (layCell == null && stdCellLib != null && myInfo.isRootCell())
						{
							layCell = findStandardCell(no);
							if (layCell != null)
							{
								NodeInst ni = no.getNodeInst();
								System.out.println("Using standard cell " + layCell.describe(false) + " for " +
									ni.getParent().describe(false) + ":" + ni.describe(false));
							}
						}
						if (layCell == null)
						{
							String searchCellName = subCell.getName() + "{lay}";
							layCell = defaultLib.findNodeProto(searchCellName);
						}
					}
				}
				if (layCell != null)
				{
					convertedCells.put(subCell, layCell);
				}
				return layCell;
			}

			public void exitCell(HierarchyEnumerator.CellInfo info)
			{
				Cell oldCell = info.getCell();
				String newCellName = oldCell.getName();

				// create the cell
				Cell newCell = makeNewCell(newCellName, View.LAYOUT, oldCell, newLib);
				if (newCell == null) return;
				createdCells.add(newCell);

				Map<Network,List<Conn>> connections = new HashMap<Network,List<Conn>>();
				Map<Nodable,NodeInst> convertedNodes = new HashMap<Nodable,NodeInst>();

				// create node placement tree and network connections list
				PlacerGrid placer = new PlacerGrid();
				for (Iterator<Nodable> it = info.getNetlist().getNodables(); it.hasNext(); ) {
					Nodable no = it.next();
					NodeProto np = null;

					if (no.isCellInstance())
					{
						np = getLayoutCell(no, (Info)info);
						if (np == null)
						{
							System.out.println("Warning: Unable to find layout version of cell "+no.getProto().describe(false));
						}
					} else
					{
						np = figureNewNodeProto(no.getNodeInst(), newTech);
					}
					if (np != null)
					{
						if (oldTech != Schematics.tech())
						{
							// layout Conversion
							NodeInst newNi = placeLayoutNode(no, np, newCell, info.getContext());
							convertedNodes.put(no, newNi);
						} else
						{
							if (np.getFunction().isPin()) continue;
							if (np.getFunction() == PrimitiveNode.Function.CONNECT) continue;
							if (no.getName().startsWith("fill") || no.getName().startsWith("tfill")) continue;

							placer.insert(new Leaf(no, info.getContext(), np, newCell));

							// record connections on ports
							for (Iterator<PortProto> itp = no.getProto().getPorts(); itp.hasNext(); )
							{
								PortProto pp = itp.next();
								Name ppname = pp.getNameKey();
								for (int i=0; i<ppname.busWidth(); i++)
								{
									Name subname = ppname.subname(i);
									Conn conn = new Conn(no, pp, subname);
									Network net = info.getNetlist().getNetwork(no, pp, i);
									List<Conn> list = connections.get(net);
									if (list == null)
									{
										list = new ArrayList<Conn>();
										connections.put(net, list);
									}
									// if port on same node already connects to this network, skip it
									boolean add = true;
									for (Conn aconn : list)
									{
										if (aconn.no == conn.no) { add = false; break; }
									}
									if (add) list.add(conn);
								}
							}
						}
					}
				}

				if (oldTech == Schematics.tech())
				{
					// place all new nodes
					placer.place(this, convertedNodes);

					// create rats nest of connections
					ArcProto ratArc = Generic.tech().unrouted_arc;
					for (Iterator<Network> it = info.getNetlist().getNetworks(); it.hasNext(); )
					{
						Network network = it.next();
						List<Conn> list = connections.get(network);
						if (list == null) continue;
						if (list.size() == 0) continue;
						Conn conn = list.get(0);
						PortInst pi = getLayoutPortInst(conn, convertedNodes);
						if (pi == null)
						{
							System.out.println("Cannot find port "+conn.portName+" on "+conn.no.getName()+" in cell "+newCell.describe(false));
							continue;
						}
						String exportName = null;
						Export e = null;
						Iterator<Export> eIt = network.getExports();
						if (eIt.hasNext())
						{
							e = eIt.next();
							exportName = network.getName();
						}
						for (int i=1; i<list.size(); i++)
						{
							Conn nextConn = list.get(i);
							PortInst nextPi = getLayoutPortInst(nextConn, convertedNodes);
							if (nextPi == null)
							{
								System.out.println("Cannot find port "+nextConn.portName+" on "+nextConn.no.getName()+" in cell "+newCell.describe(false));
								continue;
							}

							ArcInst newAi = ArcInst.makeInstanceBase(ratArc, ratArc.getDefaultLambdaBaseWidth(), pi, nextPi,
								pi.getCenter(), nextPi.getCenter(), null);
							if (newAi == null)
							{
								System.out.println("Cell " + newCell.describe(true) + ": can't run " + ratArc + " from " +
									pi.getNodeInst() + " " + pi + " at (" + pi.getCenter().getX() + "," + pi.getCenter().getY() + ") to " +
									nextPi.getNodeInst() + " " + nextPi + " at (" + nextPi.getCenter().getX() + "," + nextPi.getCenter().getY() + ")");
								continue;
							}
							newAi.setFixedAngle(false);
							newAi.setRigid(false);

							// create export if name matches
							if (exportName != null && nextPi.getPortProto().getName().equals(exportName))
							{
								Export pp2 = Export.newInstance(newCell, nextPi, exportName, e.getCharacteristic());
								pp2.copyTextDescriptorFrom(e, Export.EXPORT_NAME);
								pp2.copyVarsFrom(e);
								exportName = null;
							}
						}
						if (exportName != null)
						{
							Export pp2 = Export.newInstance(newCell, pi, exportName, e.getCharacteristic());
							pp2.copyTextDescriptorFrom(e, Export.EXPORT_NAME);
							pp2.copyVarsFrom(e);
							exportName = null;
						}
					}
					convertedCells.put(oldCell, newCell);
				} else
				{
					makeLayoutParts(oldCell, newCell, oldCell.getTechnology(), newTech, View.LAYOUT, convertedCells, convertedNodes);
					convertedCells.put(oldCell, newCell);
				}
			}

			private PortInst getLayoutPortInst(Conn schConn, Map<Nodable,NodeInst> convertedNodes)
			{
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

				if (schConn.no.getNodeInst().getFunction().isTransistor())
				{
					if (schConn.portName.toString().equals("g")) return layNi.getTransistorGatePort();
					if (schConn.portName.toString().equals("s")) return layNi.getTransistorSourcePort();
					if (schConn.portName.toString().equals("d")) return layNi.getTransistorDrainPort();
				}

				// associate by position in port list
				for (int i=0; i<schConn.no.getNodeInst().getNumPortInsts() && i<layNi.getNumPortInsts(); i++)
				{
					PortInst api = schConn.no.getNodeInst().getPortInst(i);
					if (api.getPortProto() == schConn.pp)
					{
						//System.out.println("Associated port "+i+": "+schConn.no.getProto().getName()+"."+schConn.pp.getName()+" to "+
						// layNi.getProto().getName()+"."+layNi.getProto().getPort(i).getName());
						return layNi.getPortInst(i);
					}
				}

				// special case again: one-port capacitors are OK
				PrimitiveNode.Function oldFun = schConn.no.getNodeInst().getFunction();
				PrimitiveNode.Function newFun = layNi.getFunction();
				if (oldFun.isCapacitor() && newFun.isCapacitor()) 
//				if (oldFun == PrimitiveNode.Function.CAPAC && newFun == PrimitiveNode.Function.ECAPAC) 
					return layNi.getPortInst(0);

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
//				// first convert the nodes
//				for(Iterator<NodeInst> it = oldCell.getNodes(); it.hasNext(); )
//				{
//					NodeInst ni = it.next();
//					// handle sub-cells
//					if (ni.isCellInstance())
//					{
//						Cell newCellType = (Cell)convertedCells.get((Cell)ni.getProto());
//						if (newCellType == null)
//						{
//							System.out.println("No equivalent cell for " + ni.getProto());
//							continue;
//						}
//						placeLayoutNode(ni, newCellType, newCell, VarContext.globalContext);
//						continue;
//					}
//
//					// handle primitives
//					if (ni.getProto() == Generic.tech.cellCenterNode) continue;
//					NodeProto newNp = figureNewNodeProto(ni, newTech);
//					if (newNp != null)
//						placeLayoutNode(ni, newNp, newCell, VarContext.globalContext);
//				}

				/*
				 * for each arc in cell, find the ends in the new technology, and
				 * make a new arc to connect them in the new cell
				 */
				for(Iterator<ArcInst> it = oldCell.getArcs(); it.hasNext(); )
				{
					ArcInst ai = it.next();
					// get the nodes and ports on the two ends of the arc
					NodeInst oldHeadNi = ai.getHeadPortInst().getNodeInst();
					NodeInst oldTailNi = ai.getTailPortInst().getNodeInst();

					NodeInst newHeadNi = convertedNodes.get(oldHeadNi);
					NodeInst newTailNi = convertedNodes.get(oldTailNi);
					if (newHeadNi == null || newTailNi == null) continue;
					PortProto oldHeadPp = ai.getHeadPortInst().getPortProto();
					PortProto oldTailPp = ai.getTailPortInst().getPortProto();
					PortProto newHeadPp = convertPortName(oldHeadNi, newHeadNi, oldHeadPp.getNameKey());
					PortProto newTailPp = convertPortName(oldTailNi, newTailNi, oldTailPp.getNameKey());
					if (newHeadPp == null || newTailPp == null) continue;

					// compute arc type and see if it is acceptable
					ArcProto newAp = figureNewArcProto(ai.getProto(), newTech, newHeadPp, newTailPp);

					// determine new arc width
					boolean fixAng = ai.isFixedAngle();
					double newWid = 0;
					if (newAp == Generic.tech().universal_arc) fixAng = false; else
					{
						double defwid = ai.getProto().getDefaultLambdaBaseWidth();
						double curwid = ai.getLambdaBaseWidth();
						newWid = newAp.getDefaultLambdaBaseWidth() * curwid / defwid;
						if (!(newWid > 0)) newWid = newAp.getDefaultLambdaBaseWidth();
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
					ArcInst newAi = ArcInst.makeInstanceBase(newAp, newWid, newHeadPi, newTailPi, pHead, pTail, ai.getName());
					if (newAi == null)
					{
						System.out.println("Cell " + newCell.describe(true) + ": can't run " + newAp + " from " +
							newHeadNi + " " + newHeadPp + " at (" + pHead.getX() + "," + pHead.getY() + ") to " +
							newTailNi + " " + newTailPp + " at (" + pTail.getX() + "," + pTail.getY() + ")");
						continue;
					}
					newAi.copyPropertiesFrom(ai);
					if (newAp == Generic.tech().universal_arc)
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

				if (oldni.getParent().isSchematic() && type.isTransistor())
				{
					for (Iterator<PrimitiveNode> it = newTech.getNodes(); it.hasNext(); )
					{
						PrimitiveNode node = it.next();
						if (type == node.getFunction()) return node;
					}
					PrimitiveNode.Function threePort = type.make3PortTransistor();
					for (Iterator<PrimitiveNode> it = newTech.getNodes(); it.hasNext(); )
					{
						PrimitiveNode node = it.next();
						if (threePort == node.getFunction()) return node;
					}
				}

				if (type == PrimitiveNode.Function.NODE)
				{
					// get the polygon describing the first box of the old node
					PrimitiveNode np = (PrimitiveNode)oldNp;
					Technology.NodeLayer [] nodeLayers = np.getNodeLayers();
					Layer layer = nodeLayers[0].getLayer();
					Layer.Function fun = layer.getFunction();

					// now search for that function in the other technology
					for(Iterator<PrimitiveNode> it = newTech.getNodes(); it.hasNext(); )
					{
						PrimitiveNode oNp = it.next();
						if (oNp.getFunction() != PrimitiveNode.Function.NODE) continue;
						Technology.NodeLayer [] oNodeLayers = oNp.getNodeLayers();
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
					PrimitivePort pOldPp = pOldNp.getPort(0);
					ArcProto [] oldConnections = pOldPp.getConnections();

					for(Iterator<PrimitiveNode> it = newTech.getNodes(); it.hasNext(); )
					{
						PrimitiveNode pNewNp = it.next();
						if (pNewNp.getFunction() != type) continue;
						PrimitivePort pNewPp = pNewNp.getPort(0);
						ArcProto [] newConnections = pNewPp.getConnections();

						boolean oldMatches = true;
						for(int j=0; j<oldConnections.length; j++)
						{
							ArcProto oap = oldConnections[j];
							if (oap.getTechnology() == Generic.tech()) continue;

							boolean foundNew = false;
							for(int k=0; k<newConnections.length; k++)
							{
								ArcProto ap = newConnections[k];
								if (ap.getTechnology() == Generic.tech()) continue;
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
				Orientation or = ni.getOrient();
				if (newNp instanceof PrimitiveNode)
				{
					// get offsets for new node type
					PrimitiveNode pNewNp = (PrimitiveNode)newNp;
					PrimitiveNode pOldNp = (PrimitiveNode)ni.getProto();
					newXSize = pNewNp.getDefWidth() + ni.getXSize() - pOldNp.getDefWidth();
					newYSize = pNewNp.getDefHeight() + ni.getYSize() - pOldNp.getDefHeight();
					if (no.getParent().isSchematic() && pNewNp.getFunction().isTransistor())
					{
						Variable width = no.getVar(Variable.newKey("ATTR_width"));
						SizeOffset offset = newNp.getProtoSizeOffset();
						if (width != null)
						{
							newXSize = VarContext.objectToDouble(context.evalVar(width, no), newXSize);
							newXSize = newXSize + offset.getHighXOffset() + offset.getLowXOffset();
						}
						Variable length = no.getVar(Variable.newKey("ATTR_length"));
						if (length != null)
						{
							newYSize = VarContext.objectToDouble(context.evalVar(length, no), newYSize);
							newYSize = newYSize + offset.getHighYOffset() + offset.getLowYOffset();
						}

						// see which way the existing transistor is rotated
						Point2D origSLoc = ni.getTransistorSourcePort().getCenter();
						Point2D origDLoc = ni.getTransistorDrainPort().getCenter();
						Point2D origCtr = new Point2D.Double((origSLoc.getX()+origDLoc.getX())/2, (origSLoc.getY()+origDLoc.getY())/2);
						int gOrigAng = DBMath.figureAngle(origCtr, ni.getTransistorGatePort().getCenter());
						int sOrigAng = DBMath.figureAngle(origCtr, ni.getTransistorSourcePort().getCenter());
						int dOrigAng = DBMath.figureAngle(origCtr, ni.getTransistorDrainPort().getCenter());
						int origDirection = (gOrigAng - dOrigAng + 3600) % 3600;

						// see if the transistor should be rotated
						for(;;)
						{
							NodeInst testNi = NodeInst.makeDummyInstance(newNp, ni.getAnchorCenter(), newXSize, newYSize, or);
							PortInst gPi = pNewNp.getTechnology().getTransistorGatePort(testNi);
							PortInst sPi = pNewNp.getTechnology().getTransistorSourcePort(testNi);
							PortInst dPi = pNewNp.getTechnology().getTransistorDrainPort(testNi);
							int gAng = DBMath.figureAngle(ni.getAnchorCenter(), gPi.getCenter());
							int sAng = DBMath.figureAngle(ni.getAnchorCenter(), sPi.getCenter());
							int dAng = DBMath.figureAngle(ni.getAnchorCenter(), dPi.getCenter());
							int direction = (gAng - dAng + 3600) % 3600;
							if (origDirection != direction)
							{
								// flip it
								or = Orientation.fromJava(or.getAngle(), !or.isXMirrored(), or.isYMirrored());
								continue;
							}

							int sDelta = (sAng - sOrigAng) % 3600;
							int dDelta = (dAng - dOrigAng) % 3600;
							int averageDelta = (sDelta + dDelta) / 2;
							if (averageDelta <= 450 && averageDelta >= -450) break;
							if (averageDelta <= 450) or = or.concatenate(Orientation.R); else
								or = or.concatenate(Orientation.RRR);
						}
					}
				} else
				{
					Cell np = (Cell)newNp;
					Rectangle2D bounds = np.getBounds();
					newXSize = bounds.getWidth();
					newYSize = bounds.getHeight();
				}

				// create the node
				NodeInst newNi = NodeInst.makeInstance(newNp, ni.getAnchorCenter(), newXSize, newYSize, newCell, or, no.getName(), ni.getTechSpecific());
				if (newNi == null)
				{
					System.out.println("Could not create " + newNp + " in " + newCell);
					return null;
				}
				newNi.copyStateBitsAndExpandedFlag(ni);
				if (!no.getParent().isSchematic())
					newNi.copyVarsFrom(ni);

				// re-export any ports on the node
				for(Iterator<Export> it = ni.getExports(); it.hasNext(); )
				{
					Export e = it.next();
					for (int i=0; i<e.getNameKey().busWidth(); i++)
					{
						Name portName = e.getOriginalPort().getPortProto().getNameKey().subname(i);
						Name exportName = e.getNameKey().subname(i);
						PortProto pp = convertPortName(ni, newNi, portName);
						if (pp == null) continue;
						PortInst pi = newNi.findPortInstFromProto(pp);
						Export pp2 = Export.newInstance(newCell, pi, exportName.toString(), e.getCharacteristic());
						if (pp2 == null) continue;
						pp2.copyTextDescriptorFrom(e, Export.EXPORT_NAME);
						pp2.copyVarsFrom(e);
					}
				}
				return newNi;
			}

			private static class PlacerGrid
			{
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
					if (Math.abs(xDist) >= Math.abs(yDist))
					{
						if (xDist < 0) return Location.LEFT;
						return Location.RIGHT;
					}
					if (yDist < 0) return Location.BELOW;
					return Location.ABOVE;
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
				protected void place(MakeLayoutVisitor visitor, Map<Nodable,NodeInst> convertedNodes) {
					if (node == null) return;
					node.place(visitor, convertedNodes);
					if (aboveNode != null) placeRelative(aboveNode, node, Location.ABOVE, visitor, convertedNodes);
					if (belowNode != null) placeRelative(belowNode, node, Location.BELOW, visitor, convertedNodes);
					if (leftNode != null)  placeRelative(leftNode,  node, Location.LEFT,  visitor, convertedNodes);
					if (rightNode != null) placeRelative(rightNode, node, Location.RIGHT, visitor, convertedNodes);
				}
				private void placeRelative(PlacerGrid placeNode, PlacerGrid refNode, Location schRelLoc,
					MakeLayoutVisitor visitor, Map<Nodable,NodeInst> convertedNodes) {
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
				protected void place(MakeLayoutVisitor visitor, Map<Nodable,NodeInst> convertedNodes) {
					layNi = visitor.placeLayoutNode(schNo, layNp, newCell, context);
					convertedNodes.put(schNo, layNi);
				}
				protected void move(double dx, double dy) {
					layNi.modifyInstance(dx, dy, 0, 0, Orientation.IDENT);
					//System.out.println("Moved "+layNi.getName()+" ("+dx+","+dy+")");
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
				// generic arcs stay the same
				if (oldAp.getTechnology() == Generic.tech()) return oldAp;

				// schematic wires become universal arcs
				if (oldAp != Schematics.tech().wire_arc)
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
				Set<ArcProto> possibleArcs = new HashSet<ArcProto>();
				ArcProto [] headArcs = headPp.getBasePort().getConnections();
				ArcProto [] tailArcs = tailPp.getBasePort().getConnections();
				for(int i=0; i < headArcs.length; i++)
				{
					if (headArcs[i].getTechnology() == Generic.tech()) continue;
					for(int j=0; j < tailArcs.length; j++)
					{
						if (tailArcs[j].getTechnology() == Generic.tech()) continue;
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
				return Generic.tech().universal_arc;
			}

			/**
			 * Method to determine the port to use on node "newni" assuming that it should
			 * be the same as port "oldPp" on equivalent node "ni"
			 */
/*			private PortProto convertPortProto(NodeInst ni, NodeInst newNi, PortProto oldPp)
			{
				if (newNi.isCellInstance())
				{
					// cells can associate by comparing names
					PortProto pp = newNi.getProto().findPortProto(oldPp.getName());
					if (pp != null) return pp;
					// System.out.println("Cannot find export " + oldPp.getName() + " in " + newNi.getProto());
					return null;
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
			}*/

			/**
			 * Method to determine the port to use on node "newNi" assuming that it should
			 * be the same as port "portName" on equivalent node "ni"
			 */
			private PortProto convertPortName(NodeInst ni, NodeInst newNi, Name portName) {

				if (newNi.isCellInstance()) {
					PortProto pp = newNi.getProto().findPortProto(portName);
					if (pp == null)
						System.out.println("Cannot find export " + portName + " in " + newNi.getProto());
					return pp;
				}

				// if each has only 1 port, they match
				int numNewPorts = newNi.getProto().getNumPorts();
				if (numNewPorts == 0) return null;
				if (numNewPorts == 1)
				{
					return newNi.getProto().getPort(0);
				}

				// associate by position in port list
				PortProto pp = ni.getProto().findPortProto(portName);
				if (pp != null) {
					int i = 0;
					for (i=0; i<ni.getProto().getNumPorts(); i++) {
						if (ni.getProto().getPort(i) == pp) break;
					}
					if (i < ni.getProto().getNumPorts()) {
						return newNi.getProto().getPort(i);
					}
				}

				// special case again: one-port capacitors are OK
				PrimitiveNode.Function oldFun = ni.getFunction();
				PrimitiveNode.Function newFun = newNi.getFunction();
				if (oldFun.isCapacitor() && newFun.isCapacitor())
//				if (oldFun == PrimitiveNode.Function.CAPAC && newFun == PrimitiveNode.Function.ECAPAC)
					return newNi.getProto().getPort(0);

				// association has failed: assume the first port
				System.out.println("No port association between " + ni.getProto() + ", "
					+ portName + " and " + newNi.getProto());
				return newNi.getProto().getPort(0);
			}

			/**
			 * Method to find the layout cell that corresponds to a desired schematic instance.
			 * @param no the Schematic Nodable in question.
			 * @return the proper layout cell to use.
			 */
			private Cell findStandardCell(Nodable no)
			{
				// first see if the desired cell has a single "working" icon instance in it
				NodeInst essentialIcon = no.getNodeInst();
				if (essentialIcon == null) return null;
				Map<String,Double> essentialVars = getVariables(essentialIcon);

				// now find a cell with just that essential icon
				Map<NodeInst,Map<String,Double>> possibleNodes = new HashMap<NodeInst,Map<String,Double>>();
				for(Iterator<Cell> it = stdCellLib.getCells(); it.hasNext(); )
				{
					Cell stdCell = it.next();
					NodeInst thisEI = getEssentialContent(stdCell);
					if (thisEI != null && thisEI.getProto() == essentialIcon.getProto())
					{
						// find equivalent layout cell
						for(Iterator<Cell> cIt = stdCell.getCellGroup().getCells(); cIt.hasNext(); )
						{
							Cell layCell = cIt.next();
							if (layCell.getView() == View.LAYOUT)
								possibleNodes.put(thisEI, getVariables(thisEI)); // return layCell;
						}
					}
				}
				if (possibleNodes.size() == 0) return null;
				double bestParamDist = Double.MAX_VALUE;
				NodeInst bestNi = null;
				for(NodeInst thisEI : possibleNodes.keySet())
				{
					Map<String,Double> thisVars = possibleNodes.get(thisEI);
					double paramDist = computeParameterDistance(essentialVars, thisVars);
					if (paramDist < bestParamDist)
					{
						bestParamDist = paramDist;
						bestNi = thisEI;
					}
				}

				// determine the layout cell with this node
				for(Iterator<Cell> cIt = bestNi.getParent().getCellGroup().getCells(); cIt.hasNext(); )
				{
					Cell layCell = cIt.next();
					if (layCell.getView() == View.LAYOUT) return layCell;
				}
				return null;
			}

			/**
			 * Method to find the "essential" node in a cell.
			 * There must be just one such node, ignoring pins, contacts, etc.
			 * @param cell the Cell to search.
			 * @return the essential NodeInst in the Cell (null if none).
			 */
			private NodeInst getEssentialContent(Cell cell)
			{
				NodeInst essentialContent = null;
				for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = it.next();
					if (ni.isIconOfParent()) continue;
					PrimitiveNode.Function fun = ni.getFunction();
					if (fun.isPin()) continue;
					if (fun == PrimitiveNode.Function.CONNECT) continue;
					if (fun == PrimitiveNode.Function.ART) continue;
					if (essentialContent != null) { essentialContent = null;   break; }
					essentialContent = ni;
				}
				return essentialContent;
			}

			/**
			 * Method to create a map of all variables and values on a given NodeInst.
			 * @param ni the NodeInst to examine.
			 * @return a Map from String variable names to Double variable values.
			 */
			private Map<String,Double> getVariables(NodeInst ni)
			{
				Map<String,Double> vars = new HashMap<String,Double>();
				for(Iterator<Variable> it = ni.getParametersAndVariables(); it.hasNext(); )
				{
					Variable var = it.next();
					if (!var.isDisplay()) continue;
					String value = null; // var.describe(-1, VarContext.globalContext, ni);
			        if (var.isCode())
					{
						// special case for code: it is a string, the type applies to the result
			            Object val = null;
			            try {
			                val = VarContext.globalContext.evalVarRecurse(var, ni);
				            value = val.toString();
			            } catch (VarContext.EvalException e) {}
					} else
					{
						value = var.getPureValue(-1);
					}
					if (TextUtils.isANumber(value))
					{
						Double v = new Double(TextUtils.atof(value));
						vars.put(var.getKey().getName(), v);
					}
				}
				return vars;
			}

			/**
			 * Method to determine the distance between two sets of parameters.
			 * @param pars1 a map of parameter name and value for the first set of parameters.
			 * @param pars2 a map of parameter name and value for the second set of parameters.
			 * @return the distance between the parameter values (smaller numbers are closer).
			 */
			private double computeParameterDistance(Map<String,Double> pars1, Map<String,Double> pars2)
			{
				double dist = 0;
				for(String par1 : pars1.keySet())
				{
					Double val1 = pars1.get(par1);
					Double val2 = pars2.get(par1);
					if (val2 == null) continue;
					dist += Math.abs(val1.doubleValue() - val2.doubleValue());
				}
				return dist;
			}
		}
	}
}
