/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExportChanges.java
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
package com.sun.electric.tool.user;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.DisplayedText;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.menus.MenuCommands;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

/**
 * This class has all of the pulldown menu commands in Electric.
 */
public final class ExportChanges
{
	/****************************** EXPORT LISTING ******************************/

	private static class ExportList
	{
		Export pp;
		int equiv;
		int busList;
	};

	public static void describeExports(boolean summarize)
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted your query (network information unavailable).  Please try again");
			return;
		}

		// compute the associated cell to check
		Cell wnp = cell.contentsView();
		if (wnp == null) wnp = cell.iconView();
		if (wnp == cell) wnp = null;

		// count the number of exports
		if (cell.getNumPorts() == 0)
		{
			System.out.println("There are no exports on " + cell);
			return;
		}

		// make a list of exports
		List<ExportList> exports = new ArrayList<ExportList>();
		for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
		{
			ExportList el = new ExportList();
			el.pp = (Export)it.next();
			el.equiv = -1;
			el.busList = -1;
			exports.add(el);
		}

		// sort exports by name within type
		Collections.sort(exports, new ExportSortedByNameAndType());

		// if summarizing, make associations that combine exports
		int num_found = exports.size();
		if (summarize)
		{
			// make associations among electrically equivalent exports
			for(int j=0; j<num_found; j++)
			{
				int eqJ = exports.get(j).equiv;
				int blJ = exports.get(j).busList;
				if (eqJ != -1 || blJ != -1) continue;
				Export ppJ = exports.get(j).pp;
				for(int k=j+1; k<num_found; k++)
				{
					int eqK = exports.get(k).equiv;
					int blK = exports.get(k).busList;
					if (eqK != -1 || blK != -1) continue;
					Export ppK = exports.get(k).pp;
					if (ppJ.getCharacteristic() != ppK.getCharacteristic()) break;
					if (!netlist.sameNetwork(ppJ.getOriginalPort().getNodeInst(), ppJ.getOriginalPort().getPortProto(),
						ppK.getOriginalPort().getNodeInst(), ppK.getOriginalPort().getPortProto())) continue;
					exports.get(k).equiv = j;
					exports.get(j).equiv = -2;
				}
			}

			// make associations among bussed exports
			for(int j=0; j<num_found; j++)
			{
				int eqJ = exports.get(j).equiv;
				int blJ = exports.get(j).busList;
				if (eqJ != -1 || blJ != -1) continue;
				Export ppJ = exports.get(j).pp;
				String ptJ = ppJ.getName();
				int sqPosJ = ptJ.indexOf('[');
				if (sqPosJ < 0) continue;
				for(int k=j+1; k<num_found; k++)
				{
					int eqK = exports.get(k).equiv;
					int blK = exports.get(k).busList;
					if (eqK != -1 || blK != -1) continue;
					Export ppK = exports.get(k).pp;
					if (ppJ.getCharacteristic() != ppK.getCharacteristic()) break;

					String ptK = ppK.getName();
					int sqPosK = ptK.indexOf('[');
					if (sqPosJ != sqPosK) continue;
					if (ptJ.substring(0, sqPosJ).equalsIgnoreCase(ptK.substring(0, sqPosK)))
					{
						exports.get(k).busList = j;
						exports.get(j).busList = -2;
					}
				}
			}
		}

		// describe each export
		System.out.println("----- Exports on " + cell + " -----");
		HashSet<ArcProto> arcsSeen = new HashSet<ArcProto>();
		for(int j=0; j<num_found; j++)
		{
			ExportList el = exports.get(j);
			Export pp = el.pp;
			if (el.equiv >= 0 || el.busList >= 0) continue;

			// reset flags for arcs that can connect
			for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
			{
				Technology tech = it.next();
				for(Iterator<ArcProto> aIt = tech.getArcs(); aIt.hasNext(); )
				{
					ArcProto ap = aIt.next();
					arcsSeen.remove(ap);
				}
			}

			String infstr = "";
			String activity = pp.getCharacteristic().getFullName();
			int m = j+1;
			for( ; m<num_found; m++)
			{
				if (exports.get(m).equiv == j) break;
			}
			double lx = 0, hx = 0, ly = 0, hy = 0;
			if (m < num_found)
			{
				// many exports that are electrically equivalent
				infstr += activity + " exports ";
				for(int k=j; k<num_found; k++)
				{
					if (j != k && exports.get(k).equiv != j) continue;
					if (j != k) infstr += ", ";
					Export opp = exports.get(k).pp;
					infstr += "'" + opp.getName() + "'";
					Poly poly = opp.getOriginalPort().getPoly();
					double x = poly.getCenterX();
					double y = poly.getCenterY();
					if (j == k)
					{
						lx = hx = x;   ly = hy = y;
					} else
					{
						if (x < lx) lx = x;
						if (x > hx) hx = x;
						if (y < ly) ly = y;
						if (y > hy) hy = y;
					}
					ArcProto [] arcList = opp.getBasePort().getConnections();
					for(int a=0; a<arcList.length; a++)
						arcsSeen.add(arcList[a]);
				}
				infstr += " at (" + lx + "<=X<=" + hx + ", " + ly + "<=Y<=" + hy + "), electrically connected to";
				infstr = addPossibleArcConnections(infstr, arcsSeen);
			} else
			{
				m = j + 1;
				for( ; m<num_found; m++)
				{
					if (exports.get(m).busList == j) break;
				}
				if (m < num_found)
				{
					// many exports from the same bus
					int tot = 0;
					for(int k=j; k<num_found; k++)
					{
						if (j != k && exports.get(k).busList != j) continue;
						tot++;
						Export opp = exports.get(k).pp;
						Poly poly = opp.getOriginalPort().getPoly();
						double x = poly.getCenterX();
						double y = poly.getCenterY();
						if (j == k)
						{
							lx = hx = x;   ly = hy = y;
						} else
						{
							if (x < lx) lx = x;
							if (x > hx) hx = x;
							if (y < ly) ly = y;
							if (y > hy) hy = y;
						}
						ArcProto [] arcList = opp.getBasePort().getConnections();
						for(int a=0; a<arcList.length; a++)
							arcsSeen.add(arcList[a]);
					}

					List<Export> sortedBusList = new ArrayList<Export>();
					sortedBusList.add(exports.get(j).pp);
					for(int k=j+1; k<num_found; k++)
					{
						ExportList elK = exports.get(k);
						if (elK.busList == j) sortedBusList.add(elK.pp);
					}

					// sort the bus by indices
					Collections.sort(sortedBusList, new ExportSortedByBusIndex());

					boolean first = true;
					for(Export ppS : sortedBusList)
					{
						String pt1 = ppS.getName();
						int openPos = pt1.indexOf('[');
						if (first)
						{
							infstr += activity + " ports '" + pt1.substring(0, openPos) + "[";
							first = false;
						} else
						{
							infstr += ",";
						}
						int closePos = pt1.lastIndexOf(']');
						infstr += pt1.substring(openPos+1, closePos);
					}
					infstr += "]' at (" + lx + "<=X<=" + hx + ", " + ly + "<=Y<=" + hy + "), same bus, connects to";
					infstr = addPossibleArcConnections(infstr, arcsSeen);
				} else
				{
					// isolated export
					Poly poly = pp.getOriginalPort().getPoly();
					double x = poly.getCenterX();
					double y = poly.getCenterY();
					infstr += activity + " export '" + pp.getName() + "' at (" + x + ", " + y + ") connects to";
					ArcProto [] arcList = pp.getBasePort().getConnections();
					for(int a=0; a<arcList.length; a++)
						arcsSeen.add(arcList[a]);
					infstr = addPossibleArcConnections(infstr, arcsSeen);

					// check for the export in the associated cell
					if (wnp != null)
					{
						if (pp.getEquivalentPort(wnp) == null)
							infstr += " *** no equivalent in " + wnp;
					}
				}
			}

			TextUtils.printLongString(infstr);
		}
		if (wnp != null)
		{
			for(Iterator<PortProto> it = wnp.getPorts(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				if (pp.getEquivalentPort(cell) == null)
					System.out.println("*** Export " + pp.getName() + ", found in " + wnp + ", is missing here");
			}
		}
	}

	/**
	 * Helper method to add all marked arc prototypes to the infinite string.
	 * Marking is done by having the "temp1" field be nonzero.
	 */
	private static String addPossibleArcConnections(String infstr, HashSet arcsSeen)
	{
		int i = 0;
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			for(Iterator<ArcProto> aIt = tech.getArcs(); aIt.hasNext(); )
			{
				ArcProto ap = aIt.next();
				if (!arcsSeen.contains(ap)) i++;
			}
		}
		if (i == 0) infstr += " EVERYTHING"; else
		{
			i = 0;
			for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
			{
				Technology tech = it.next();
				if (tech == Generic.tech) continue;
				for(Iterator<ArcProto> aIt = tech.getArcs(); aIt.hasNext(); )
				{
					ArcProto ap = aIt.next();
					if (!arcsSeen.contains(ap)) continue;
					if (i != 0) infstr += ",";
					i++;
					infstr += " " + ap.getName();
				}
			}
		}
		return infstr;
	}

	private static class ExportSortedByNameAndType implements Comparator<ExportList>
	{
		public int compare(ExportList el1, ExportList el2)
		{
			Export e1 = el1.pp;
			Export e2 = el2.pp;
			PortCharacteristic ch1 = e1.getCharacteristic();
			PortCharacteristic ch2 = e2.getCharacteristic();
			if (ch1 != ch2) return ch1.getOrder() - ch2.getOrder();
			String s1 = e1.getName();
			String s2 = e2.getName();
			return TextUtils.STRING_NUMBER_ORDER.compare(s1, s2);
		}
	}

	private static class ExportSortedByBusIndex implements Comparator<Export>
	{
		public int compare(Export e1, Export e2)
		{
			String s1 = e1.getName();
			String s2 = e2.getName();
			return TextUtils.STRING_NUMBER_ORDER.compare(s1, s2);
		}
	}

    private static class PortInstsSortedByBusIndex implements Comparator<PortInst>
    {
        public int compare(PortInst p1, PortInst p2)
        {
            String s1 = p1.getPortProto().getName();
            String s2 = p2.getPortProto().getName();
            return TextUtils.STRING_NUMBER_ORDER.compare(s1, s2);
        }
    }

	/****************************** EXPORT CREATION ******************************/

	/**
	 * Method to re-export all unwired/unexported ports on cell instances in the current Cell.
	 */
	public static void reExportAll()
	{
		// make sure there is a current cell
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;

		List<Geometric> allNodes = new ArrayList<Geometric>();
		for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
			allNodes.add(it.next());
		}

		new ReExportNodes(cell, allNodes, false, false, true);
	}

	/**
	 * Method to re-export everything that is selected.
	 * @param includeWiredPorts true to re-export ports that are wired.
	 */
	public static void reExportSelected(boolean includeWiredPorts)
	{
		// make sure there is a current cell
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;

		List<Geometric> nodeInsts = MenuCommands.getSelectedObjects(true, false);
		if (nodeInsts.size() == 0) {
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"Please select one or objects to re-export",
					"Re-export failed", JOptionPane.ERROR_MESSAGE);
			return;
		}

		new ReExportNodes(cell, nodeInsts, includeWiredPorts, false, true);
	}

	/**
	 * Method to reexport the selected port on other nodes in the cell.
	 */
	public static void reExportSelectedPort()
	{
		// make sure there is a current cell
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
		Highlighter highlighter = wnd.getHighlighter();
		if (highlighter == null) return;
		Highlight2 high = highlighter.getOneHighlight();
		if (high == null || !high.isHighlightEOBJ() || !(high.getElectricObject() instanceof PortInst))
		{
            System.out.println("Must first select a single node and its port");
            return;
        }
		PortInst pi = (PortInst)high.getElectricObject();
		PortProto pp = pi.getPortProto();
		NodeInst ni = pi.getNodeInst();

		// make a list of ports to reexport
		List<PortInst> queuedExports = new ArrayList<PortInst>();
		Cell cell = ni.getParent();
		for(Iterator<NodeInst>it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst oNi = it.next();
			if (oNi.getProto() != ni.getProto()) continue;
			boolean unexported = true;
			for(Iterator<Export> eIt = oNi.getExports(); eIt.hasNext(); )
			{
				Export e = eIt.next();
				if (e.getOriginalPort().getPortProto() == pp)
				{
					unexported = false;
					break;
				}
			}
			if (unexported)
			{
				PortInst oPi = oNi.findPortInstFromProto(pp);
				queuedExports.add(oPi);
			}
		}

		// create job
        new ReExportPorts(cell, queuedExports, true, false, false, null);
	}

	/**
	 * Method to re-export all unwired/unexported ports on cell instances in the current Cell.
	 * Only works for power and ground ports.
	 */
	public static void reExportPowerAndGround()
	{
		// make sure there is a current cell
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;

        List<Geometric> allNodes = new ArrayList<Geometric>();
        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
            allNodes.add(it.next());
        }

        new ReExportNodes(cell, allNodes, false, true, true);
	}

	/**
	 * Helper class for re-exporting ports on nodes.
	 */
    private static class ReExportNodes extends Job
    {
        private Cell cell;
        private List<Geometric> nodeInsts;
        private boolean includeWiredPorts;
        private boolean onlyPowerGround;
        private boolean ignorePrimitives;

        /**
         * @see ExportChanges#reExportNodes(java.util.List, boolean, boolean, boolean)
         */
        public ReExportNodes(Cell cell, List<Geometric> nodeInsts, boolean includeWiredPorts, boolean onlyPowerGround,
                             boolean ignorePrimitives)
        {
            super("Re-export nodes", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cell = cell;
            this.nodeInsts = nodeInsts;
            this.includeWiredPorts = includeWiredPorts;
            this.onlyPowerGround = onlyPowerGround;
            this.ignorePrimitives = ignorePrimitives;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            // disallow port action if lock is on
            if (CircuitChangeJobs.cantEdit(cell, null, true) != 0) return false;

            int num = reExportNodes(cell, nodeInsts, includeWiredPorts, onlyPowerGround, ignorePrimitives);
            System.out.println(num+" ports exported.");
            return true;
        }
    }

    /**
     * Re-exports ports on each NodeInst in the list, in the order the nodeinsts appear
     * in the list. Sorts the exports on each node before exporting them to make sure they
     * get the correct bus indices at the next level up.
     * @param cell the cell in which exporting is happening.
     * @param nodeInsts a list of NodeInsts whose ports will be exported
     * @param includeWiredPorts true to include ports that have wire connections
     * @param onlyPowerGround true to only export power and ground type ports
     * @param ignorePrimitives true to ignore primitive nodes
     * @return the number of exports created
     */
    public static int reExportNodes(Cell cell, List<Geometric> nodeInsts, boolean includeWiredPorts, boolean onlyPowerGround,
                                     boolean ignorePrimitives) {
        int total = 0;

        for (Geometric geom : nodeInsts)
        {
            NodeInst ni = (NodeInst)geom;

            // only look for cells, not primitives
            if (ignorePrimitives)
                if (!ni.isCellInstance()) continue;

            // ignore recursive references (showing icon in contents)
            if (ni.isIconOfParent()) continue;

            List<PortInst> portInstsToExport = new ArrayList<PortInst>();
            for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); ) {
                PortInst pi = pIt.next();

				// ignore if already exported
				boolean found = false;
				for(Iterator<Export> eIt = ni.getExports(); eIt.hasNext(); )
				{
					Export pp = eIt.next();
					if (pp.getOriginalPort() == pi) { found = true;   break; }
				}
				if (found) continue;

                // add pi to list of ports to export
                portInstsToExport.add(pi);
            }
            total += reExportPorts(cell, portInstsToExport, true, includeWiredPorts, onlyPowerGround, null);
        }
        return total;
    }

    /**
     * Helper class for re-exporting a port on a node.
     */
    public static class ReExportPorts extends Job
    {
        private Cell cell;
        private List<PortInst> portInsts;
        private boolean sort;
        private boolean includeWiredPorts;
        private boolean onlyPowerGround;
        private HashMap<PortInst,Export> originalExports;

        /**
         * Constructor.
         */
        public ReExportPorts(Cell cell, List<PortInst> portInsts, boolean sort, boolean includeWiredPorts,
                             boolean onlyPowerGround, HashMap<PortInst,Export> originalExports)
        {
            super("Re-export ports", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cell = cell;
            this.portInsts = portInsts;
            this.includeWiredPorts = includeWiredPorts;
            this.onlyPowerGround = onlyPowerGround;
            this.sort = sort;
            this.originalExports = originalExports;
            startJob();
        }

        public boolean doIt() throws JobException
        {
    		// disallow port action if lock is on
    		if (CircuitChangeJobs.cantEdit(cell, null, true) != 0) return false;

    		int num = reExportPorts(cell, portInsts, sort, includeWiredPorts, onlyPowerGround, originalExports);
            System.out.println(num+" ports exported.");
            return true;
        }
    }

	/****************************** EXPORT CREATION IN A HIGHLIGHTED AREA ******************************/

	/**
	 * Method to re-export all unwired/unexported ports on cell instances in the current Cell.
	 * Only works in the currently highlighted area.
	 * @param deep true to reexport hierarchically to the bottom.
	 * @param includeWiredPorts true to reexport ports that are wired.
	 */
	public static void reExportHighlighted(boolean deep, boolean includeWiredPorts)
	{
		// make sure there is a current cell
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;

		EditWindow wnd = EditWindow.getCurrent();
		Rectangle2D bounds = wnd.getHighlighter().getHighlightedArea(null);
		if (bounds == null)
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"Must select area before re-exporting the highlighted area",
					"Re-export failed", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// do the job of reexporting in a boundary
		ERectangle eBounds = new ERectangle(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
		new ReExportHighlighted(cell, eBounds, deep, includeWiredPorts);
	}

	/**
	 * Class to Re-export the highlighted area in a Job.
	 */
    private static class ReExportHighlighted extends Job
    {
        private Cell cell;
        private ERectangle bounds;
        private boolean deep;
        private boolean includeWiredPorts;

        public ReExportHighlighted(Cell cell, ERectangle bounds, boolean deep, boolean includeWiredPorts)
        {
            super("Re-export highlighted", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cell = cell;
            this.bounds = bounds;
            this.deep = deep;
            this.includeWiredPorts = includeWiredPorts;
            startJob();
        }

        public boolean doIt() throws JobException
        {
    		// disallow port action if lock is on
    		if (CircuitChangeJobs.cantEdit(cell, null, true) != 0) return false;

    		reExportInBounds(cell, bounds, deep, includeWiredPorts, true);
            return true;
        }
    }

    /**
     * Helper method to recursively re-export everything in a highlighted area.
     * @param cell the Cell in which to re-export.
     * @param bounds the area of the Cell to re-export.
     * @param deep true to recurse down to subcells and re-export.
     * @param includeWiredPorts true to re-export when the port is wired.
     * @param topLevel true if this is the top-level call.
     */
	private static void reExportInBounds(Cell cell, Rectangle2D bounds, boolean deep, boolean includeWiredPorts, boolean topLevel)
	{
		// find all ports in highlighted area
		List<PortInst> queuedExports = new ArrayList<PortInst>();
		for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (!ni.isCellInstance()) continue;

			// see if the cell intersects the bounds
			Rectangle2D cellBounds = ni.getBounds();
			if (!bounds.intersects(cellBounds)) continue;

			// if doing a deep reexport, recurse into the cell
			if (deep)
			{
				AffineTransform goIn = ni.translateIn(ni.rotateIn());
				Rectangle2D boundsInside = new Rectangle2D.Double(bounds.getMinX(), bounds.getMinY(),
					bounds.getWidth(), bounds.getHeight());
				GenMath.transformRect(boundsInside, goIn);
				reExportInBounds((Cell)ni.getProto(), boundsInside, deep, includeWiredPorts, false);
			}
			
			for (Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
			{
				PortInst pi = pIt.next();

				// make sure the port is inside the selected area
				Poly portPoly = pi.getPoly();
				if (!bounds.contains(portPoly.getCenterX(), portPoly.getCenterY())) continue;
				queuedExports.add(pi);
			}
		}

		// remove already-exported ports
		for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			PortInst pi = pp.getOriginalPort();
			queuedExports.remove(pi);
		}

		// no ports to export
		if (queuedExports.size() == 0)
		{
			if (topLevel) System.out.println("No ports in area to export");
			return;
		}

		// create job
		int num = reExportPorts(cell, queuedExports, true, includeWiredPorts, false, null);
        System.out.println(num+" ports exported.");
	}

    /**
     * Re-exports the PortInsts in the list. If sort is true, it first sorts the list by name and
     * bus index. Otherwise, they are exported in the order they are found in the list.
     * Note that ports are filtered first, then sorted.
     * @param cell the cell in which exporting is happening.
     * @param portInsts the list of PortInsts to export
     * @param sort true to re-sort the portInsts list
     * @param includeWiredPorts true to export ports that are already wired
     * @param onlyPowerGround true to only export ports that are power and ground
     * @param originalExports a map from the entries in portInsts to original Exports.
     * This is used when re-exporting ports on a copy that were exported on the original.
     * Ignored if null.
     * @return the number of ports exported
     */
    public static int reExportPorts(Cell cell, List<PortInst> portInsts, boolean sort, boolean includeWiredPorts, boolean onlyPowerGround,
                                    HashMap<PortInst,Export> originalExports) {
        EDatabase.serverDatabase().checkChanging();

        List<PortInst> portInstsFiltered = new ArrayList<PortInst>();
        int total = 0;

        // filter the ports - remove unwanted
        for (PortInst pi : portInsts) {
            if (!includeWiredPorts) {
                // remove ports that already have connections
                if (pi.hasConnections()) continue;
//                if (pi.getConnections().hasNext()) continue;
            }
            if (onlyPowerGround) {
                // remove ports that are not power or ground
                PortProto pp = pi.getPortProto();
                if (!pp.isPower() && !pp.isGround()) continue;
            }
            // remove exported ports
            NodeInst ni = pi.getNodeInst();
            for (Iterator<Export> exit = ni.getExports(); exit.hasNext(); ) {
                Export e = exit.next();
                if (e.getOriginalPort() == pi) continue;
            }

            portInstsFiltered.add(pi);
        }

        // sort the ports by name and bus index
        if (sort)
            Collections.sort(portInstsFiltered, new PortInstsSortedByBusIndex());

        // export the ports
        Set<String> already = new HashSet<String>();
        for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
        {
        	Export e = (Export)it.next();
        	already.add(e.getNameKey().canonicString());
        }
        HashMap<String,GenMath.MutableInteger> nextPlainIndex = new HashMap<String,GenMath.MutableInteger>();
        for (PortInst pi : portInstsFiltered)
        {
            // disallow port action if lock is on
			int errorCode = CircuitChangeJobs.cantEdit(cell, pi.getNodeInst(), true);
			if (errorCode < 0) return total;
			if (errorCode > 0) continue;

			// presume the name of the new Export
            String protoName = pi.getPortProto().getName();

            // or use export name if there is a reference export
            Export refExport = null;
            if (originalExports != null)
            {
				refExport = originalExports.get(pi);
	            if (refExport != null) protoName = refExport.getName();
            }

            // get unique name here so Export.newInstance doesn't print message
            protoName = ElectricObject.uniqueObjectName(protoName, cell, PortProto.class, already, nextPlainIndex, false);

            // create export
            Export newPp = Export.newInstance(cell, pi, protoName);
            if (newPp != null)
            {
                // copy text descriptor, var, and characteristic
				if (pi.getPortProto() instanceof Export)
				{
					newPp.copyTextDescriptorFrom((Export)pi.getPortProto(), Export.EXPORT_NAME);
					newPp.copyVarsFrom(((Export)pi.getPortProto()));
				}
                newPp.setCharacteristic(pi.getPortProto().getCharacteristic());
                // find original export if any, and copy text descriptor, vars, and characteristic
                if (refExport != null) {
                    newPp.copyTextDescriptorFrom(refExport, Export.EXPORT_NAME);
                    newPp.copyVarsFrom(refExport);
                    newPp.setCharacteristic(refExport.getCharacteristic());
                }
                total++;
                already.add(newPp.getNameKey().canonicString());
            }
        }

        return total;
    }

    /**
     * This returns the port inst on newNi that corresponds to the portinst that has been exported
     * as 'referenceExport' on some other nodeinst of the same node prototype.
     * This method is useful when re-exporting ports on copied nodes because
     * the original port was exported.
     * @param newNi the new node inst on which the port inst will be found
     * @param referenceExport the export on the old node inst
     * @return the port inst on newNi which corresponds to the exported portinst on the oldNi
     * referred to through 'referenceExport'.
     */
    public static PortInst getNewPortFromReferenceExport(NodeInst newNi, Export referenceExport) {
        PortInst origPi = referenceExport.getOriginalPort();
        PortInst newPi = newNi.findPortInstFromProto(origPi.getPortProto());
        return newPi;
    }

	/****************************** EXPORT DELETION ******************************/
    
	/**
	 * Method to delete the currently selected exports.
	 */
	public static void deleteExport()
	{
		// make sure there is a current cell
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;

		List<Export> exportsToDelete = new ArrayList<Export>();
        EditWindow wnd = EditWindow.getCurrent();
		List<DisplayedText> dts = wnd.getHighlighter().getHighlightedText(true);
		for(DisplayedText dt : dts)
		{
			if (dt.getElectricObject() instanceof Export)
			{
				Export pp = (Export)dt.getElectricObject();
				exportsToDelete.add(pp);
			}
		}
		if (exportsToDelete.size() == 0)
		{
			System.out.println("There are no selected exports to delete");
			return;
		}
		new DeleteExports(cell, exportsToDelete);
	}

	/**
	 * Method to delete all exports on the highlighted objects.
	 */
	public static void deleteExportsOnSelected()
	{
		// make sure there is a current cell
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;

		List<Export> exportsToDelete = new ArrayList<Export>();
        EditWindow wnd = EditWindow.getCurrent();
		List<Geometric> highs = wnd.getHighlighter().getHighlightedEObjs(true, false);
		for(Geometric geom : highs)
		{
			NodeInst ni = (NodeInst)geom;
			for(Iterator<Export> eIt = ni.getExports(); eIt.hasNext(); )
			{
				exportsToDelete.add(eIt.next());
			}
		}
		if (exportsToDelete.size() == 0)
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"There are no exports on the highlighted objects",
					"Re-export failed", JOptionPane.ERROR_MESSAGE);
			return;
		}
		new DeleteExports(cell, exportsToDelete);
	}

	/**
	 * Method to delete all exports in the highlighted area.
	 */
	public static void deleteExportsInArea()
	{
		// make sure there is a current cell
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;

		List<Export> exportsToDelete = new ArrayList<Export>();
        EditWindow wnd = EditWindow.getCurrent();
		Rectangle2D bounds = wnd.getHighlighter().getHighlightedArea(null);
		if (bounds == null)
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"Must select something before deleting the highlighted exports",
					"Export delete failed", JOptionPane.ERROR_MESSAGE);
			return;
		}
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			for(Iterator<Export> eIt = ni.getExports(); eIt.hasNext(); )
			{
				Export e = eIt.next();
				PortInst pi = e.getOriginalPort();
				Poly poly = pi.getPoly();
				if (bounds.contains(poly.getCenterX(), poly.getCenterY()))
					exportsToDelete.add(e);
			}
		}
		if (exportsToDelete.size() == 0)
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"There are no exports in the highlighted area",
					"Re-export failed", JOptionPane.ERROR_MESSAGE);
			return;
		}
		new DeleteExports(cell, exportsToDelete);
	}

	public static class DeleteExports extends Job
	{
		private Cell cell;
		private List<Export> exportsToDelete;

		public DeleteExports(Cell cell, List<Export> exportsToDelete)
		{
			super("Delete exports", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cell = cell;
			this.exportsToDelete = exportsToDelete;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// disallow port action if lock is on
			if (CircuitChangeJobs.cantEdit(cell, null, true) != 0) return false;

			int total = 0;
			for(Export e : exportsToDelete)
			{
				int errorCode = CircuitChangeJobs.cantEdit(cell, e.getOriginalPort().getNodeInst(), true);
				if (errorCode < 0) break;
				if (errorCode > 0) continue;
				e.kill();
				total++;
			}
			if (total == 0) System.out.println("No exports deleted"); else
				System.out.println(total + " exports deleted");

			return true;
		}
	}

	/****************************** EXPORT MOVING ******************************/

	/**
	 * Method to move the currently selected export from one node to another.
	 */
	public static void moveExport()
	{
		Export source = null;
		PortInst dest = null;
        EditWindow wnd = EditWindow.getCurrent();
		for(Highlight2 h : wnd.getHighlighter().getHighlights())
		{
			boolean used = false;
			if (h.isHighlightEOBJ())
			{
				if (h.getElectricObject() instanceof PortInst)
				{
					if (dest != null)
					{
						System.out.println("Must select only one node-port as a destination of the move");
						return;
					}
					dest = (PortInst)h.getElectricObject();
					used = true;
				}
			} else if (h.isHighlightText())
			{
				if (h.getVarKey() == Export.EXPORT_NAME && h.getElectricObject() instanceof Export)
				{
					source = (Export)h.getElectricObject();
					used = true;
				}
			}
			if (!used)
			{
				System.out.println("Moving exports: select one export to move, and one node-port as its destination");
				return;
			
			}
		}
		if (source == null || dest == null)
		{
			System.out.println("First select one export to move, and one node-port as its destination");
			return;
		}
		new MoveExport(source, dest);
	}

	private static class MoveExport extends Job
	{
		private Export source;
		private PortInst dest;

		protected MoveExport(Export source, PortInst dest)
		{
			super("Move export", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.source = source;
			this.dest = dest;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			source.move(dest);
			return true;
		}
	}

	/****************************** EXPORT RENAMING ******************************/

	/**
	 * Method to rename the currently selected export.
	 */
	public static void renameExport()
	{
        EditWindow wnd = EditWindow.getCurrent();
		Highlight2 h = wnd.getHighlighter().getOneHighlight();
		if (h == null || h.getVarKey() != Export.EXPORT_NAME || !(h.getElectricObject() instanceof Export))
		{
			System.out.println("Must select an export name before renaming it");
			return;
		}
		Export pp = (Export)h.getElectricObject();
		String response = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "Rename export", pp.getName());
		if (response == null) return;
		new RenameExport(pp, response);
	}

	/**
	 * Class to rename a cell in a new thread.
	 */
	private static class RenameExport extends Job
	{
		private Export pp;
		private String newName;

		protected RenameExport(Export pp, String newName)
		{
			super("Rename Export" + pp.getName(), User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.pp = pp;
			this.newName = newName;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			pp.rename(newName);
			return true;
		}
	}

	/****************************** EXPORT HIGHLIGHTING ******************************/

	private static class ShownPorts
	{
		Point2D   loc;
		PortProto pp;
		int       angle;
	};

	/**
	 * Method to show all exports in the current cell.
	 */
	public static void showExports()
	{
		showPortsAndExports(null);
	}

	/**
	 * Method to show all ports on the selected nodes in the current cell.
	 */
	public static void showPorts()
	{
        EditWindow wnd = EditWindow.getCurrent();
		List<Geometric> nodes = wnd.getHighlighter().getHighlightedEObjs(true, false);
		if (nodes == null || nodes.size() == 0)
		{
			System.out.println("No nodes are highlighted");
			return;
		}
		showPortsAndExports(nodes);
	}

	private static void showPortsAndExports(List<Geometric> nodes)
	{
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
		Cell cell = wnd.getCell();
		if (cell == null)
		{
			System.out.println("No cell in this window");
			return;
		}

		// determine the maximum number of ports to show
		int total = cell.getNumPorts();
		if (nodes != null)
		{
			total = 0;
			for(Geometric geom : nodes)
			{
				NodeInst ni = (NodeInst)geom;
				total += ni.getNumPortInsts();
			}
		}

		// associate ports with display locations (and compute the true number of ports to show)
		Rectangle2D displayable = wnd.displayableBounds();
		ShownPorts [] portList = new ShownPorts[total];
		total = 0;
		int ignored = 0;
		if (nodes == null)
		{
			// handle exports on the cell
			for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				Poly poly = pp.getOriginalPort().getPoly();

				Point2D ptOut = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
				if (ptOut.getX() < displayable.getMinX() || ptOut.getX() > displayable.getMaxX() ||
					ptOut.getY() < displayable.getMinY() || ptOut.getY() > displayable.getMaxY())
				{
					ignored++;
					continue;
				}

				portList[total] = new ShownPorts();
				portList[total].loc = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
				portList[total].pp = pp;
				total++;
			}
		} else
		{
			// handle ports on the selected nodes
			for(Geometric geom : nodes)
			{
				NodeInst ni = (NodeInst)geom;
				for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
				{
					PortInst pi = pIt.next();
					Poly poly = pi.getPoly();

					Point2D ptOut = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
					if (ptOut.getX() < displayable.getMinX() || ptOut.getX() > displayable.getMaxX() ||
						ptOut.getY() < displayable.getMinY() || ptOut.getY() > displayable.getMaxY())
					{
						ignored++;
						continue;
					}

					portList[total] = new ShownPorts();
					portList[total].loc = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
					portList[total].pp = pi.getPortProto();
					total++;
				}
			}
		}

		// determine the height of text in screen space
        int fontSize = EditWindow.getDefaultFontSize();
        Point screenOrigin = wnd.databaseToScreen(0, 0);
		Point2D thPoint = wnd.screenToDatabase(screenOrigin.x, screenOrigin.y + fontSize);
		double textHeight = Math.abs(thPoint.getY());

		// determine the location of the port labels
		Point2D [] labelLocs = new Point2D.Double[total];
		double digitIndentX = displayable.getWidth() / 20;
		double digitIndentY = displayable.getHeight() / 20;
		int numPerSide = (total + 3) / 4;
		int leftSideCount, topSideCount, rightSideCount, botSideCount;
		leftSideCount = topSideCount = rightSideCount = botSideCount = numPerSide;
		if (leftSideCount + topSideCount + rightSideCount + botSideCount > total)
			botSideCount--;
		if (leftSideCount + topSideCount + rightSideCount + botSideCount > total)
			topSideCount--;
		if (leftSideCount + topSideCount + rightSideCount + botSideCount > total)
			rightSideCount--;
		int fill = 0;
		for(int i=0; i<leftSideCount; i++)
		{
			labelLocs[fill++] = new Point2D.Double(displayable.getMinX() + digitIndentX,
				displayable.getHeight() / (leftSideCount+1) * (i+1) + displayable.getMinY());
		}
		for(int i=0; i<topSideCount; i++)
		{
			double shift = (i % 3) * textHeight - textHeight;
			labelLocs[fill++] = new Point2D.Double(displayable.getWidth() / (topSideCount+1) * (i+1) + displayable.getMinX(),
				displayable.getMaxY() - digitIndentY - shift);
		}
		for(int i=0; i<rightSideCount; i++)
		{
			labelLocs[fill++] = new Point2D.Double(displayable.getMaxX() - digitIndentX,
				displayable.getMaxY() - displayable.getHeight() / (rightSideCount+1) * (i+1));
		}
		for(int i=0; i<botSideCount; i++)
		{
			double shift = (i % 3) * textHeight - textHeight;
			labelLocs[fill++] = new Point2D.Double(displayable.getMaxX() - displayable.getWidth() / (botSideCount+1) * (i+1),
				displayable.getMinY() + digitIndentY - shift);
		}

		// build a sorted list of ports around the center
		double x = 0, y = 0;
		for(int i=0; i<total; i++)
		{
			x += portList[i].loc.getX();
			y += portList[i].loc.getY();
		}
		Point2D center = new Point2D.Double(x / total, y / total);
		for(int i=0; i<total; i++)
		{
			if (center.getX() == portList[i].loc.getX() && center.getY() == portList[i].loc.getY())
				portList[i].angle = 0; else
					portList[i].angle = -DBMath.figureAngle(center, portList[i].loc);
		}

		List<ShownPorts> portLabels = new ArrayList<ShownPorts>();
		for(int i=0; i<total; i++)
			portLabels.add(portList[i]);
		Collections.sort(portLabels, new SortPortAngle());
		total = 0;
		for(ShownPorts sp : portLabels)
			portList[total++] = sp;

		// figure out the best rotation offset
		double bestDist = 0;
		int bestOff = 0;
		for(int i=0; i<total; i++)
		{
			double dist = 0;
			for(int j=0; j<total; j++)
				dist += labelLocs[j].distance(portList[(j+i)%total].loc);
			if (dist < bestDist || i == 0)
			{
				bestOff = i;
				bestDist = dist;
			}
		}

		// show the ports
        Highlighter highlighter = wnd.getHighlighter();
		Font font = wnd.getFont(null);
		FontRenderContext frc = new FontRenderContext(null, true, true);
		LineMetrics lm = font.getLineMetrics("hy", frc);
		double baselineVer = wnd.getTextUnitSize(lm.getDescent());
		double baselineHor = wnd.getTextUnitSize(2);

		for(int i=0; i<total; i++)
		{
			int index = (bestOff + i) % total;
			Point2D loc = labelLocs[i];
			String msg = portList[index].pp.getName();

			// get the connecting-line coordinates in screen space
	        Point locationLabel = wnd.databaseToScreen(loc.getX(), loc.getY());
	        Point locationPort = wnd.databaseToScreen(portList[index].loc.getX(), portList[index].loc.getY());

	        // determine the opposite corner of the text
			GlyphVector v = font.createGlyphVector(frc, msg);
			Rectangle2D glyphBounds = v.getLogicalBounds();
	        int otherX = locationLabel.x + (int)glyphBounds.getWidth();
	        int otherY = locationLabel.y - (int)glyphBounds.getHeight();
			Point2D locOther = wnd.screenToDatabase(otherX, otherY);

	        // if the text is off-screen, adjust it
	        if (otherX > wnd.getSize().width)
	        {
	        	int offDist = otherX - wnd.getSize().width;
	        	locationLabel.x -= offDist;
	        	otherX -= offDist;
	        	loc = wnd.screenToDatabase(locationLabel.x, locationLabel.y);
	        }

	        // change the attachment point on the label to be closest to the port
	        if (Math.abs(locationPort.x-locationLabel.x) > Math.abs(locationPort.x-otherX))
	        	locationLabel.x = otherX;
	        if (Math.abs(locationPort.y-locationLabel.y) > Math.abs(locationPort.y-otherY))
	        	locationLabel.y = otherY;

	        // convert this shift back to database units for the highlight
			Point2D locLineEnd = wnd.screenToDatabase(locationLabel.x, locationLabel.y);

			// draw the port name
			highlighter.addMessage(cell, msg, new Point2D.Double(loc.getX()+baselineHor, loc.getY()+baselineVer));

			// draw a box around the text
			Point2D odd1 = new Point2D.Double(loc.getX(), locOther.getY());
			Point2D odd2 = new Point2D.Double(locOther.getX(), loc.getY());
			highlighter.addLine(loc, odd1, cell);
			highlighter.addLine(odd1, locOther, cell);
			highlighter.addLine(locOther, odd2, cell);
			highlighter.addLine(odd2, loc, cell);

			// draw a line from the text to the port
			highlighter.addLine(locLineEnd, portList[index].loc, cell);
		}
		highlighter.finished();
		if (total == 0)
			System.out.println("No exported ports to show");
		if (ignored > 0)
			System.out.println("Could not display " + ignored + " ports (outside of the window)");
	}

	private static class SortPortAngle implements Comparator<ShownPorts>
	{
		public int compare(ShownPorts s1, ShownPorts s2)
		{
			return s1.angle - s2.angle;
		}
	}

	/****************************** EXPORT MATCHING BETWEEN LIBRARIES ******************************/

	/**
	 * Method to synchronize the exports in two libraries.
	 * The user is prompted for another library (other than the current one)
	 * and all exports in that library are copied to the current one.
	 */
	public static void synchronizeLibrary()
	{
		List<Library> libs = Library.getVisibleLibraries();
		Library curLib = Library.getCurrent();
		int otherLibraries = libs.size() - 1;
		if (otherLibraries < 1)
		{
			System.out.println("There must be an other library (not the current one) from which to copy exports.");
			return;
		}
		String [] libNames = new String[otherLibraries];
		int i=0;
		for (Library oLib : libs)
		{
			if (oLib == curLib) continue;
			libNames[i++] = oLib.getName();
		}
        String chosen = (String)JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(),
        	"Choose another library from which to copy exports", "Choose a Library",
			JOptionPane.QUESTION_MESSAGE, null, libNames, libNames[0]);
        if (chosen == null) return;
        Library oLib = Library.findLibrary(chosen);
        if (oLib == null) return;

        // now run the synchronization
        new SynchronizeExports(oLib);
	}

	/**
	 * Class to synchronize exports in a separate Job.
	 */
	private static class SynchronizeExports extends Job
	{
		private Library oLib;

		private SynchronizeExports(Library oLib)
		{
			super("Synchronize exports", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.oLib = oLib;
			startJob();
		}

		public boolean doIt() throws JobException
		{
	        // merge the two libraries
	    	int newPorts = 0;
	    	boolean noCells = false;
			Library curLib = Library.getCurrent();
	    	for(Iterator<Cell> cIt = curLib.getCells(); cIt.hasNext(); )
	    	{
	    		Cell np = cIt.next();

	    		// find this cell in the other library
	    		for(Iterator<Cell> oCIt = oLib.getCells(); oCIt.hasNext(); )
	    		{
	    			Cell oNp = oCIt.next();
	    			if (!np.getName().equals(oNp.getName())) continue;

	    			// synchronize the ports
	    			for(Iterator<PortProto> pIt = oNp.getPorts(); pIt.hasNext(); )
	    			{
	    				Export oPp = (Export)pIt.next();

	    				// see if that other cell's port is in this one
	    				Export pp = (Export)np.findPortProto(oPp.getName());
	    				if (pp != null) continue;

	    				// must add port "oPp" to cell "np"
	    				NodeInst oNi = oPp.getOriginalPort().getNodeInst();
	    				if (oNi.isCellInstance())
	    				{
	    					if (!noCells)
	    						System.out.println("Cannot yet make exports that come from other cell instances (i.e. export " +
	    							oPp.getName() + " in " + oNp + ")");
	    					noCells = true;
	    					continue;
	    				}

	    				// presume that the cells have the same coordinate system
	    				NodeInst ni = NodeInst.makeInstance(oNi.getProto(), oNi.getAnchorCenter(), oNi.getXSize(), oNi.getYSize(),
	    					np, oNi.getOrient(), oNi.getName(), oNi.getTechSpecific());
//	    				NodeInst ni = NodeInst.makeInstance(oNi.getProto(), oNi.getAnchorCenter(), oNi.getXSizeWithMirror(), oNi.getYSizeWithMirror(),
//	    					np, oNi.getAngle(), oNi.getName(), oNi.getTechSpecific());
	    				if (ni == null) continue;
	    				PortInst pi = ni.findPortInstFromProto(oPp.getOriginalPort().getPortProto());
	    				pp = Export.newInstance(np, pi, oPp.getName());
	    				if (pp == null) continue;
	    				pp.setCharacteristic(oPp.getCharacteristic());
	    				pp.copyTextDescriptorFrom(oPp, Export.EXPORT_NAME);
	    				pp.copyVarsFrom(oPp);
	    				newPorts++;
	    			}
	    		}
	    	}
	    	System.out.println("Created " + newPorts + " new exports in current " + curLib);
			return true;
		}
	}

	/****************************** REPLACING CELL INSTANCES FROM ANOTHER LIBRARY ******************************/

	/**
	 * Method to replace all cell instances in the current cell with like-named
	 * ones from another library.
	 */
	public static void replaceFromOtherLibrary()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;

		List<Library> libs = Library.getVisibleLibraries();
		Library curLib = Library.getCurrent();
		int otherLibraries = libs.size() - 1;
		if (otherLibraries < 1)
		{
			System.out.println("There must be an other library (not the current one) from which to replace cells.");
			return;
		}
		String [] libNames = new String[otherLibraries];
		int i=0;
		for (Library oLib : libs)
		{
			if (oLib == curLib) continue;
			libNames[i++] = oLib.getName();
		}
        String chosen = (String)JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(),
        	"Choose another library from which to replace cell instances", "Choose a Library",
			JOptionPane.QUESTION_MESSAGE, null, libNames, libNames[0]);
        if (chosen == null) return;
        Library oLib = Library.findLibrary(chosen);
        if (oLib == null) return;

        // now run the replacement
        new ReplaceFromOtherLibrary(curCell, oLib);
	}

	/**
	 * Class to replace all cell instances in the current cell with like-named
	 * ones from another library.
	 */
	private static class ReplaceFromOtherLibrary extends Job
	{
		private Cell cell;
		private Library oLib;

		private ReplaceFromOtherLibrary(Cell cell, Library oLib)
		{
			super("Replace Cell Instances From Another Library", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.oLib = oLib;
			startJob();
		}

		public boolean doIt() throws JobException
		{
	        HashMap<NodeInst,Cell> cellsToReplace = new HashMap<NodeInst,Cell>();
	        for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
	        {
	        	NodeInst ni = it.next();
	        	if (!ni.isCellInstance()) continue;
        		if (ni.getXSize() != 0 || ni.getYSize() != 0) continue;
	        	Cell oldType = (Cell)ni.getProto();
	        	if (oldType.getLibrary() == oLib) continue;
	        	
        		String nameToFind = oldType.getName();
        		if (oldType.getView() != View.UNKNOWN) nameToFind += "{" + oldType.getView().getAbbreviation() + "}";
        		Cell newType = oLib.findNodeProto(nameToFind);
        		if (newType != null)
        		{
        			cellsToReplace.put(ni, newType);
	        	}
	        }
	        System.out.println("Changing " + cellsToReplace.size() + " cell instances...");
	        int replacements = 0;
	        for(Iterator <NodeInst> it = cellsToReplace.keySet().iterator(); it.hasNext(); )
	        {
	        	NodeInst ni = it.next();
	        	Cell newType = cellsToReplace.get(ni);
    			ni.replace(newType, true, true);
    			replacements++;
    		}
	        System.out.println("Changed " + replacements + " cell instances");
			return true;
		}
	}

}
