/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CircuitChanges.java
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

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.constraint.Layout;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Date;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Class for user-level changes to the circuit.
 */
public class CircuitChanges
{
	// constructor, never used
	CircuitChanges() {}

	/****************************** NODE TRANSFORMATION ******************************/

	private static double lastRotationAmount = 90;

	public static void rotateObjects(int amount)
	{
		Cell cell = Library.needCurCell();
		if (cell == null) return;

		// disallow rotating if lock is on
		if (cantEdit(cell, null, true)) return;

		// if zero rotation, prompt for amount
		if (amount == 0)
		{
			String val = JOptionPane.showInputDialog("Amount to rotate", new Double(lastRotationAmount));
			if (val == null) return;
			double fAmount = TextUtils.atof(val);
			if (fAmount == 0)
			{
				System.out.println("Null rotation amount");
				return;
			}
			lastRotationAmount = fAmount;
			amount = (int)(fAmount * 10);
		}

		RotateSelected job = new RotateSelected(cell, amount);
	}

	protected static class RotateSelected extends Job
	{
		Cell cell;
		int amount;

		protected RotateSelected(Cell cell, int amount)
		{
			super("Rotate selected objects", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.amount = amount;
			this.startJob();
		}

		public void doIt()
		{
			// figure out which nodes get rotated
			FlagSet markObj = Geometric.getFlagSet(1);
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni.clearBit(markObj);
			}
			int nicount = 0;
			NodeInst theni = null;
			Rectangle2D selectedBounds = new Rectangle2D.Double();
			List highs = Highlight.getHighlighted(true, true);
			for(Iterator it = highs.iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				if (!(geom instanceof NodeInst)) continue;
				NodeInst ni = (NodeInst)geom;
				if (cantEdit(cell, ni, true))
				{
					markObj.freeFlagSet();
					return;
				}
				ni.setBit(markObj);
				if (nicount == 0)
				{
					selectedBounds.setRect(ni.getBounds());
				} else
				{
					Rectangle2D.union(selectedBounds, ni.getBounds(), selectedBounds);
				}
				theni = ni;
				nicount++;
			}

			// must be at least 1 node
			if (nicount <= 0)
			{
				System.out.println("Must select at least 1 node for rotation");
				markObj.freeFlagSet();
				return;
			}

			// if multiple nodes, find the center one
			if (nicount > 1)
			{
				Point2D center = new Point2D.Double(selectedBounds.getCenterX(), selectedBounds.getCenterY());
				theni = null;
				double bestdist = Integer.MAX_VALUE;
				for(Iterator it = cell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = (NodeInst)it.next();
					if (!ni.isBit(markObj)) continue;
					double dist = center.distance(ni.getTrueCenter());

					// LINTED "bestdist" used in proper order
					if (theni == null || dist < bestdist)
					{
						theni = ni;
						bestdist = dist;
					}
				}
			}

//			if (nicount > 1)
//			{
//				us_abortcommand(_("Must highlight one node for rotation about the grab-point"));
//				markObj.freeFlagSet();
//				return;
//			}
//			ni = theni;
//
//			// disallow rotating if lock is on
//			if (us_cantedit(ni->parent, ni, TRUE))
//			{
//				markObj.freeFlagSet();
//				return;
//			}
//
//			// find the grab point
//			corneroffset(ni, ni->proto, ni->rotation, ni->transpose, &gx, &gy, FALSE);
//			gx += ni->lowx;   gy += ni->lowy;
//
//			// build transformation for this operation
//			transid(transtz);   transtz[2][0] = -gx;   transtz[2][1] = -gy;
//			makeangle(amt, 0, rot);
//			transid(transfz);   transfz[2][0] = gx;    transfz[2][1] = gy;
//			transmult(transtz, rot, t1);
//			transmult(t1, transfz, t2);
//			cx = (ni->lowx+ni->highx)/2;   cy = (ni->lowy+ni->highy)/2;
//			xform(cx, cy, &gx, &gy, t2);
//			gx -= cx;   gy -= cy;
//
//			// save highlighting
//			us_pushhighlight();
//			us_clearhighlightcount();
//
//			// do the rotation
//			startobjectchange((INTBIG)ni, VNODEINST);
//
//			// rotate and translate
//			modifynodeinst(ni, gx, gy, gx, gy, amt, 0);
//
//			// end change
//			endobjectchange((INTBIG)ni, VNODEINST);
//
//			// restore highlighting
//			us_pophighlight(TRUE);

			// see which nodes already connect to the main rotation node (theni)
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni.clearBit(markObj);
			}
			theni.setBit(markObj);
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ai.clearBit(markObj);
			}
			for(Iterator it = highs.iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				if (!(geom instanceof ArcInst)) continue;
				ArcInst ai = (ArcInst)geom;
				ai.setBit(markObj);
			}
			us_spreadrotateconnection(theni, markObj);

			// now make sure that it is all connected
			List nilist = new ArrayList();
			List ailist = new ArrayList();
			for(Iterator it = highs.iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				if (!(geom instanceof NodeInst)) continue;
				NodeInst ni = (NodeInst)geom;
				if (ni == theni) continue;
				if (ni.isBit(markObj)) continue;

				if (theni.getNumPortInsts() == 0)
				{
					// no port on the cell: create one
					Cell subCell = (Cell)theni.getProto();
					NodeInst subni = NodeInst.makeInstance(Generic.tech.universalPinNode, new Point2D.Double(0,0), 0, 0, 0, subCell, null);
					if (subni == null) break;
					Export thepp = Export.newInstance(subCell, subni.getOnlyPortInst(), "temp");
					if (thepp == null) break;

					// add to the list of temporary nodes
					nilist.add(subni);
				}
				PortInst thepi = theni.getPortInst(0);
				if (ni.getNumPortInsts() != 0)
				{
					ArcInst ai = ArcInst.makeInstance(Generic.tech.invisible_arc, 0, ni.getPortInst(0), thepi, null);
					if (ai == null) break;
					ai.setRigid();
					ailist.add(ai);
				}
			}

			// make all selected arcs temporarily rigid
//			us_modarcbits(6, FALSE, x_(""), list);

			// see if there is a snap point
//			if (!us_getonesnappoint(&rotcx, &rotcy))
//			{
//				// no snap point, use center of node
//				rotcx = (theni->lowx + theni->highx) / 2;
//				rotcy = (theni->lowy + theni->highy) / 2;
//			}

			// build transformation for this operation
//			transid(transtz);   transtz[2][0] = -rotcx;   transtz[2][1] = -rotcy;
//			makeangle(amt, 0, rot);
//			transid(transfz);   transfz[2][0] = rotcx;    transfz[2][1] = rotcy;
//			transmult(transtz, rot, t1);
//			transmult(t1, transfz, t2);
//			cx = (theni->lowx+theni->highx)/2;   cy = (theni->lowy+theni->highy)/2;
//			xform(cx, cy, &gx, &gy, t2);
//			gx -= cx;   gy -= cy;

			// do the rotation
			theni.modifyInstance(0, 0, 0, 0, amount);

			// delete intermediate arcs used to constrain
			for(Iterator it = ailist.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ai.kill();
			}

			// delete intermediate nodes used to constrain
			for(Iterator it = nilist.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
//				(void)killportproto(nilist[i]->parent, nilist[i]->firstportexpinst->exportproto);
				ni.kill();
			}
			markObj.freeFlagSet();
		}
	}

	/*
	 * Helper method for rotation to mark selected nodes that need not be
	 * connected with an invisible arc.
	 */
	private static void us_spreadrotateconnection(NodeInst theni, FlagSet markObj)
	{
		for(Iterator it = theni.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			ArcInst ai = con.getArc();
			if (!ai.isBit(markObj)) continue;
			Connection other = ai.getTail();
			if (other == con) other = ai.getHead();
			NodeInst ni = other.getPortInst().getNodeInst();
			if (ni.isBit(markObj)) continue;
			ni.setBit(markObj);
			us_spreadrotateconnection(ni, markObj);
		}
	}

	public static void mirrorObjects()
	{
		System.out.println("Cannot mirror yet");
	}

	public static void sizeObjects()
	{
		System.out.println("Cannot size yet");
	}

	/****************************** DELETE SELECTED OBJECTS ******************************/

	/**
	 * Method to delete all selected objects.
	 */
	public static void deleteSelected()
	{
        DeleteSelected job = new DeleteSelected();
	}

	protected static class DeleteSelected extends Job
	{
		protected DeleteSelected()
		{
			super("Delete selected objects", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.startJob();
		}

		public void doIt()
		{
			if (Highlight.getNumHighlights() == 0) return;
			List highlightedText = Highlight.getHighlightedText();
			List deleteList = new ArrayList();
			Cell cell = null;
			Geometric oneGeom = null;
			for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.EOBJ) continue;
				ElectricObject eobj = h.getElectricObject();
				if (eobj instanceof PortInst)
					eobj = ((PortInst)eobj).getNodeInst();
				if (!(eobj instanceof Geometric)) continue;
				if (cell == null) cell = h.getCell(); else
				{
					if (cell != h.getCell())
					{
						JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
							"All objects to be deleted must be in the same cell",
								"Delete failed", JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				oneGeom = (Geometric)eobj;
				deleteList.add(eobj);
			}

			// clear the highlighting
			Highlight.clear();
			Highlight.finished();

			// if just one node is selected, see if it can be deleted with "pass through"
			if (deleteList.size() == 1 && oneGeom instanceof NodeInst)
			{
				Reconnect re = Reconnect.erasePassThru((NodeInst)oneGeom, false);
				if (re != null)
				{
					ArcInst ai = re.reconnectArcs();
					Highlight.addElectricObject(ai, cell);
					Highlight.finished();
					return;
				}
			}

			// delete the text
			for(Iterator it = highlightedText.iterator(); it.hasNext(); )
			{
				Highlight high = (Highlight)it.next();

				// disallow erasing if lock is on
				Cell np = high.getCell();
				if (np != null)
				{
					if (cantEdit(np, null, true)) continue;
				}

//				// do not deal with text on an object if the object is already in the list
//				if (high.fromgeom != NOGEOM)
//				{
//					for(j=0; list[j] != NOGEOM; j++)
//						if (list[j] == high.fromgeom) break;
//					if (list[j] != NOGEOM) continue;
//				}

				// deleting variable on object
				Variable var = high.getVar();
				ElectricObject eobj = high.getElectricObject();
				if (var != null)
				{
					eobj.delVar(var.getKey());
				} else
				{
					if (high.getName() != null)
					{
						if (eobj instanceof Geometric)
						{
							Geometric geom = (Geometric)eobj;
							if (geom instanceof NodeInst)
							{
								NodeInst ni = (NodeInst)geom;
								ni.setName(null);
								ni.modifyInstance(0, 0, 0, 0, 0);
							} else
							{
								ArcInst ai = (ArcInst)geom;
								ai.setName(null);
								ai.modify(0, 0, 0, 0, 0);
							}
						}
					} else
					{
						if (eobj instanceof Export)
						{
							Export pp = (Export)eobj;
							pp.kill();
						}
					}
				}
			}
			if (cell != null)
				eraseObjectsInList(cell, deleteList);
		}
	}

	/****************************** DELETE OBJECTS IN A LIST ******************************/

	/**
	 * Method to delete all of the Geometrics in a list.
	 * @param cell the cell with the objects to be deleted.
	 * @param list a List of Geometric objects to be deleted.
	 */
	public static void eraseObjectsInList(Cell cell, List list)
	{
		FlagSet deleteFlag = Geometric.getFlagSet(2);

		// mark all nodes touching arcs that are killed
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			ni.setFlagValue(deleteFlag, 0);
		}
		for(Iterator it=list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)geom;
				ai.getHead().getPortInst().getNodeInst().setFlagValue(deleteFlag, 1);
				ai.getTail().getPortInst().getNodeInst().setFlagValue(deleteFlag, 1);
			}
		}

		// also mark all nodes on arcs that will be erased
		for(Iterator it=list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				if (ni.getFlagValue(deleteFlag) != 0)
					ni.setFlagValue(deleteFlag, 2);
			}
		}

		// also mark all nodes on the other end of arcs connected to erased nodes
		for(Iterator it=list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				for(Iterator sit = ni.getConnections(); sit.hasNext(); )
				{
					Connection con = (Connection)sit.next();
					ArcInst ai = con.getArc();
					Connection otherEnd = ai.getHead();
					if (ai.getHead() == con) otherEnd = ai.getTail();
					if (otherEnd.getPortInst().getNodeInst().getFlagValue(deleteFlag) == 0)
						otherEnd.getPortInst().getNodeInst().setFlagValue(deleteFlag, 1);
				}
			}
		}

		// now kill all of the arcs
		for(Iterator it=list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)geom;
				ai.kill();
			}
		}

		// next kill all of the nodes
		for(Iterator it=list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				eraseNodeInst(ni);
			}
		}

		// kill all pin nodes that touched an arc and no longer do
		List nodesToDelete = new ArrayList();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getFlagValue(deleteFlag) == 0) continue;
			if (ni.getProto() instanceof PrimitiveNode)
			{
				if (ni.getProto().getFunction() != NodeProto.Function.PIN) continue;
				if (ni.getNumConnections() != 0 || ni.getNumExports() != 0) continue;
				nodesToDelete.add(ni);
			}
		}
		for(Iterator it = nodesToDelete.iterator(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			eraseNodeInst(ni);
		}

		// kill all unexported pin or bus nodes left in the middle of arcs
		List nodesToPassThru = new ArrayList();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getFlagValue(deleteFlag) == 0) continue;
			if (ni.getProto() instanceof PrimitiveNode)
			{
				if (ni.getProto().getFunction() != NodeProto.Function.PIN) continue;
				if (ni.getNumExports() != 0) continue;
				Reconnect re = Reconnect.erasePassThru(ni, false);
				if (re != null) nodesToPassThru.add(re);
			}
		}
		for(Iterator it = nodesToPassThru.iterator(); it.hasNext(); )
		{
			Reconnect re = (Reconnect)it.next();
			re.reconnectArcs();
		}

		deleteFlag.freeFlagSet();
	}

	/*
	 * Method to erase node "ni" and all associated arcs, exports, etc.
	 */
	private static void eraseNodeInst(NodeInst ni)
	{
		// erase all connecting ArcInsts on this NodeInst
		int numConnectedArcs = ni.getNumConnections();
		if (numConnectedArcs > 0)
		{
			ArcInst [] arcsToDelete = new ArcInst[numConnectedArcs];
			int i = 0;
			for(Iterator it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				arcsToDelete[i++] = con.getArc();
			}
			for(int j=0; j<numConnectedArcs; j++)
			{
				ArcInst ai = arcsToDelete[j];

				// delete the ArcInst
				ai.kill();
			}
		}

		// if this NodeInst has Exports, delete them
		undoExport(ni, null);

		// now erase the NodeInst
		ni.kill();
	}

	/*
	 * Method to recursively delete ports at nodeinst "ni" and all arcs connected
	 * to them anywhere.  If "spt" is not null, delete only that portproto
	 * on this nodeinst (and its hierarchically related ports).  Otherwise delete
	 * all portprotos on this nodeinst.
	 */
	private static void undoExport(NodeInst ni, Export spt)
	{
		int numExports = ni.getNumExports();
		if (numExports == 0) return;
		Export exportsToDelete [] = new Export[numExports];
		int i = 0;		
		for(Iterator it = ni.getExports(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			exportsToDelete[i++] = pp;
		}
		for(int j=0; j<numExports; j++)
		{
			Export pp = exportsToDelete[j];
			if (spt != null && spt != pp) continue;
			pp.kill();
		}
	}

	/****************************** DELETE A CELL ******************************/

	public static void deleteCell(Cell cell, boolean confirm)
	{
		// see if this cell is in use anywhere
		if (cell.isInUse("delete")) return;

		// make sure the user really wants to delete the cell
		if (confirm)
		{
			int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
				"Are you sure you want to delete cell " + cell.describe() + "?");
			if (response != JOptionPane.YES_OPTION) return;
		}

		// delete the cell
		DeleteCell job = new DeleteCell(cell);
	}

	/**
	 * Class to delete a cell in a new thread.
	 */
	protected static class DeleteCell extends Job
	{
		Cell cell;

		protected DeleteCell(Cell cell)
		{
			super("Delete Cell" + cell.describe(), User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.startJob();
		}

		public void doIt()
		{
			// check cell usage once more
			if (cell.isInUse("delete")) return;
			doKillCell(cell);
		}
	}

	/*
	 * Method to delete cell "cell".  Validity checks are assumed to be made (i.e. the
	 * cell is not used and is not locked).
	 */
	private static void doKillCell(Cell cell)
	{
		// delete random references to this cell
		Library lib = cell.getLibrary();
		if (cell == lib.getCurCell()) lib.setCurCell(null);

		// close windows that reference this cell
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			EditWindow wnd = wf.getEditWindow();
			if (wnd.getCell() == cell)
				wnd.setCell(null, null);
		}

//		prevversion = cell->prevversion;
//		toolturnoff(net_tool, FALSE);
		cell.kill();
//		toolturnon(net_tool);

//		// see if this was the latest version of a cell
//		if (prevversion != NONODEPROTO)
//		{
//			// newest version was deleted: rename next older version
//			for(ni = prevversion->firstinst; ni != NONODEINST; ni = ni->nextinst)
//			{
//				if ((ni->userbits&NEXPAND) != 0) continue;
//				startobjectchange((INTBIG)ni, VNODEINST);
//				endobjectchange((INTBIG)ni, VNODEINST);
//			}
//		}
//
//		// update status display if necessary
//		if (us_curnodeproto != NONODEPROTO && us_curnodeproto->primindex == 0)
//		{
//			if (cell == us_curnodeproto)
//			{
//				if ((us_state&NONPERSISTENTCURNODE) != 0) us_setnodeproto(NONODEPROTO); else
//					us_setnodeproto(el_curtech->firstnodeproto);
//			}
//		}
	}

	/****************************** DELETE UNUSED OLD VERSIONS OF CELLS ******************************/

	public static void deleteUnusedOldVersions()
	{
		DeleteUnusedOldCells job = new DeleteUnusedOldCells();
	}

	/**
	 * This class implement the command to delete unused old versions of cells.
	 */
	protected static class DeleteUnusedOldCells extends Job
	{
		protected DeleteUnusedOldCells()
		{
			super("Delete Unused Old Cells", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.startJob();
		}

		public void doIt()
		{
			boolean found = true;
			int totalDeleted = 0;
			while (found)
			{
				found = false;
				for(Iterator it = Library.getCurrent().getCells(); it.hasNext(); )
				{
					Cell cell = (Cell)it.next();
					if (cell.getNewestVersion() == cell) continue;
					if (cell.getInstancesOf().hasNext()) continue;
					System.out.println("Deleting cell " + cell.describe());
					doKillCell(cell);
					found = true;
					totalDeleted++;
					break;
				}
			}
			if (totalDeleted == 0) System.out.println("No unused old cell versions to delete"); else
			{
				System.out.println("Deleted " + totalDeleted + " cells");
				EditWindow.repaintAll();
			}
		}
	}

	/****************************** CLEAN-UP ******************************/

	public static void cleanupPinsCommand(boolean everywhere)
	{
		Highlight.clear();
		if (everywhere)
		{
			boolean cleaned = false;
			for(Iterator it = Library.getCurrent().getCells(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				if (us_cleanupcell(cell, false)) cleaned = true;
			}
			if (!cleaned) System.out.println("Nothing to clean");
		} else
		{
			// just cleanup the current cell
			Cell cell = Library.needCurCell();
			if (cell == null) return;
			us_cleanupcell(cell, true);
		}
		Highlight.finished();
	}

	/**
	 * Method to clean-up cell "np" as follows:
	 *   remove stranded pins
	 *   collapse redundant (inline) arcs
	 *   highlight zero-size nodes
	 *   removes duplicate arcs
	 *   highlight oversize pins that allow arcs to connect without touching
	 *   move unattached and invisible pins with text in a different location
	 *   resize oversized pins that don't have oversized arcs on them
	 * Returns true if changes are made.
	 */
	private static boolean us_cleanupcell(Cell cell, boolean justThis)
	{
		// look for unused pins that can be deleted
		List pinsToRemove = new ArrayList();
		List pinsToPassThrough = new ArrayList();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getFunction() != NodeProto.Function.PIN) continue;

			// if the pin is an export, save it
			if (ni.getNumExports() > 0) continue;

			// if the pin is not connected or displayed, delete it
			if (ni.getNumConnections() == 0)
			{
				// see if the pin has displayable variables on it
				boolean hasDisplayable = false;
				for(Iterator vIt = ni.getVariables(); vIt.hasNext(); )
				{
					Variable var = (Variable)vIt.next();
					if (var.isDisplay()) { hasDisplayable = true;   break; }
				}
				if (hasDisplayable) continue;

				// disallow erasing if lock is on
				if (cantEdit(cell, ni, true)) continue;

				// no displayable variables: delete it
				pinsToRemove.add(ni);
				continue;
			}

			// if the pin is connected to two arcs along the same slope, delete it
			if (ni.isInlinePin())
			{
				Reconnect re = Reconnect.erasePassThru(ni, false);
				if (re != null)
					pinsToPassThrough.add(re);
			}
		}

		// look for oversized pins that can be reduced in size
		HashMap pinsToScale = new HashMap();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getFunction() != NodeProto.Function.PIN) continue;

			// if the pin is standard size, leave it alone
			double overSizeX = ni.getXSize() - ni.getProto().getDefWidth();
			if (overSizeX < 0) overSizeX = 0;
			double overSizeY = ni.getYSize() - ni.getProto().getDefHeight();
			if (overSizeY < 0) overSizeY = 0;
			if (overSizeX == 0 && overSizeY == 0) continue;

			// all arcs must connect in the pin center
			boolean arcsInCenter = true;
			for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
			{
				Connection con = (Connection)cIt.next();
				ArcInst ai = con.getArc();
				if (ai.getHead().getPortInst().getNodeInst() == ni)
				{
					if (ai.getHead().getLocation().getX() != ni.getGrabCenterX()) { arcsInCenter = false;   break; }
					if (ai.getHead().getLocation().getY() != ni.getGrabCenterY()) { arcsInCenter = false;   break; }
				}
				if (ai.getTail().getPortInst().getNodeInst() == ni)
				{
					if (ai.getTail().getLocation().getX() != ni.getGrabCenterX()) { arcsInCenter = false;   break; }
					if (ai.getTail().getLocation().getY() != ni.getGrabCenterY()) { arcsInCenter = false;   break; }
				}
			}
			if (!arcsInCenter) continue;

			// look for arcs that are oversized
			double overSizeArc = 0;
			for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
			{
				Connection con = (Connection)cIt.next();
				ArcInst ai = con.getArc();
				double overSize = ai.getWidth() - ai.getProto().getDefaultWidth();
				if (overSize < 0) overSize = 0;
				if (overSize > overSizeArc) overSizeArc = overSize;
			}

			// if an arc covers the pin, leave the pin
			if (overSizeArc >= overSizeX && overSizeArc >= overSizeY) continue;

			double dSX = 0, dSY = 0;
			if (overSizeArc < overSizeX) dSX = overSizeX - overSizeArc;
			if (overSizeArc < overSizeY) dSY = overSizeY - overSizeArc;
			pinsToScale.put(ni, new Point2D.Double(-dSX, -dSY));
		}

		// look for pins that are invisible and have text in different location
		List textToMove = new ArrayList();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			Point2D pt = ni.invisiblePinWithOffsetText(false);
			if (pt != null)
				textToMove.add(ni);
		}

		// highlight oversize pins that allow arcs to connect without touching
		int overSizePins = 0;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getFunction() != NodeProto.Function.PIN) continue;

			// make sure all arcs touch each other
			boolean nodeIsBad = false;
			for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
			{
				Connection con = (Connection)cIt.next();
				ArcInst ai = con.getArc();
				double i = ai.getWidth() - ai.getProto().getWidthOffset();
				Poly poly = ai.curvedArcOutline(ai, Poly.Type.CLOSED, i);
				if (poly == null)
					poly = ai.makePoly(ai.getLength(), i, Poly.Type.FILLED);
				for(Iterator oCIt = ni.getConnections(); oCIt.hasNext(); )
				{
					Connection oCon = (Connection)oCIt.next();
					ArcInst oAi = oCon.getArc();
					if (ai.getArcIndex() <= oAi.getArcIndex()) continue;
					double oI = oAi.getWidth() - oAi.getProto().getWidthOffset();
					Poly oPoly = oAi.curvedArcOutline(oAi, Poly.Type.CLOSED, oI);
					if (oPoly == null)
						oPoly = oAi.makePoly(oAi.getLength(), oI, Poly.Type.FILLED);
					double dist = poly.separation(oPoly);
					if (dist <= 0) continue;
					nodeIsBad = true;
					break;
				}
				if (nodeIsBad) break;
			}
			if (nodeIsBad)
			{
				if (justThis)
				{
					Highlight.addElectricObject(ni, cell);
				}
				overSizePins++;
			}
		}

		// look for duplicate arcs
		HashMap arcsToKill = new HashMap();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
			{
				Connection con = (Connection)cIt.next();
				ArcInst ai = con.getArc();
				int otherEnd = 0;
				if (ai.getConnection(0) == con) otherEnd = 1;
				boolean foundAnother = false;
				for(Iterator oCIt = ni.getConnections(); oCIt.hasNext(); )
				{
					Connection oCon = (Connection)oCIt.next();
					ArcInst oAi = oCon.getArc();
					if (ai.getArcIndex() <= oAi.getArcIndex()) continue;
					if (con.getPortInst().getPortProto() != oCon.getPortInst().getPortProto()) continue;
					if (ai.getProto() != oAi.getProto()) continue;
					int oOtherEnd = 0;
					if (oAi.getConnection(0) == oCon) oOtherEnd = 1;
					if (ai.getConnection(otherEnd).getPortInst().getNodeInst() !=
						oAi.getConnection(oOtherEnd).getPortInst().getNodeInst()) continue;
					if (ai.getConnection(otherEnd).getPortInst().getPortProto() !=
						oAi.getConnection(oOtherEnd).getPortInst().getPortProto()) continue;

					// this arc is a duplicate
					arcsToKill.put(oAi, oAi);
					foundAnother = true;
					break;
				}
				if (foundAnother) break;
			}
		}

		// now highlight negative or zero-size nodes
		int zeroSize = 0, negSize = 0;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() == Generic.tech.cellCenterNode ||
				ni.getProto() == Generic.tech.invisiblePinNode ||
				ni.getProto() == Generic.tech.essentialBoundsNode) continue;
			SizeOffset so = ni.getProto().getSizeOffset();
			double sX = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
			double sY = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();
			if (sX > 0 && sY > 0) continue;
			if (justThis)
			{
				Highlight.addElectricObject(ni, cell);
			}
			if (sX < 0 || sY < 0) negSize++; else
				zeroSize++;
		}

		if (pinsToRemove.size() == 0 &&
			pinsToPassThrough.size() == 0 &&
			pinsToScale.size() == 0 &&
			zeroSize == 0 &&
			negSize == 0 &&
			textToMove.size() == 0 &&
			overSizePins == 0 &&
			arcsToKill.size() == 0)
		{
			if (justThis) System.out.println("Nothing to clean");
			return false;
		}

		CleanupChanges job = new CleanupChanges(cell, justThis, pinsToRemove, pinsToPassThrough, pinsToScale, textToMove, arcsToKill,
			zeroSize, negSize, overSizePins);
		return true;
	}
	
	/**
	 * This class implements the changes needed to cleanup pins in a Cell.
	 */
	private static class CleanupChanges extends Job
	{
		private Cell cell;
		private boolean justThis;
		private List pinsToRemove;
		private List pinsToPassThrough;
		private HashMap pinsToScale;
		private List textToMove;
		private HashMap arcsToKill;
		private int zeroSize, negSize, overSizePins;

		private CleanupChanges(Cell cell, boolean justThis, List pinsToRemove, List pinsToPassThrough, HashMap pinsToScale, List textToMove, HashMap arcsToKill,
			int zeroSize, int negSize, int overSizePins)
		{
			super("Cleanup cell " + cell.describe(), User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.justThis = justThis;
			this.pinsToRemove = pinsToRemove;
			this.pinsToPassThrough = pinsToPassThrough;
			this.pinsToScale = pinsToScale;
			this.textToMove = textToMove;
			this.arcsToKill = arcsToKill;
			this.zeroSize = zeroSize;
			this.negSize = negSize;
			this.overSizePins = overSizePins;
			this.startJob();
		}

		public void doIt()
		{
			// do the queued operations
			for(Iterator it = pinsToRemove.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni.kill();
			}
			for(Iterator it = pinsToPassThrough.iterator(); it.hasNext(); )
			{
				Reconnect re = (Reconnect)it.next();
				re.reconnectArcs();
			}
			for(Iterator it = pinsToScale.keySet().iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				Point2D scale = (Point2D)pinsToScale.get(ni);
				ni.modifyInstance(0, 0, scale.getX(), scale.getY(), 0);
			}
			for(Iterator it = textToMove.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni.invisiblePinWithOffsetText(true);
			}
			for(Iterator it = arcsToKill.keySet().iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ai.kill();
			}

			// report what was cleaned
			String infstr = "";
			if (!justThis) infstr += "Cell " + cell.describe() + ":";
			boolean spoke = false;
			if (pinsToRemove.size() != 0)
			{
				infstr += "Removed " + pinsToRemove.size() + " pins";
				spoke = true;
			}
			if (arcsToKill.size() != 0)
			{
				if (spoke) infstr += "; ";
				infstr += "Removed " + arcsToKill.size() + " duplicate arcs";
				spoke = true;
			}
			if (pinsToScale.size() != 0)
			{
				if (spoke) infstr += "; ";
				infstr += "Shrunk " + pinsToScale.size() + " pins";
				spoke = true;
			}
			if (zeroSize != 0)
			{
				if (spoke) infstr += "; ";
				if (justThis)
				{
					infstr += "Highlighted " + zeroSize + " zero-size pins";
				} else
				{
					infstr += "Found " + zeroSize + " zero-size pins";
				}
				spoke = true;
			}
			if (negSize != 0)
			{
				if (spoke) infstr += "; ";
				if (justThis)
				{
					infstr += "Highlighted " + negSize + " negative-size pins";
				} else
				{
					infstr += "Found " + negSize + " negative-size pins";
				}
				spoke = true;
			}
			if (overSizePins != 0)
			{
				if (spoke) infstr += "; ";
				if (justThis)
				{
					infstr += "Highlighted " + overSizePins + " oversize pins with arcs that don't touch";
				} else
				{
					infstr += "Found " + overSizePins + " oversize pins with arcs that don't touch";
				}
				spoke = true;
			}
			if (textToMove.size() != 0)
			{
				if (spoke) infstr += "; ";
				infstr += "Moved text on " + textToMove.size() + " pins with offset text";
			}
			System.out.println(infstr);
		}
	}

	/**
	 * Method to analyze the current cell and show all nonmanhattan geometry.
	 */
	public static void showNonmanhattanCommand()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;

		// see which cells (in any library) have nonmanhattan stuff
		FlagSet cellMark = NodeProto.getFlagSet(1);
		for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = (Library)lIt.next();
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				cell.clearBit(cellMark);
			}
		}
		for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = (Library)lIt.next();
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				for(Iterator aIt = cell.getArcs(); aIt.hasNext(); )
				{
					ArcInst ai = (ArcInst)aIt.next();
					ArcProto ap = ai.getProto();
					if (ap.getTechnology() == Generic.tech || ap.getTechnology() == Artwork.tech ||
						ap.getTechnology() == Schematics.tech) continue;
					Variable var = ai.getVar(ArcInst.ARC_RADIUS);
					if (var != null) cell.setBit(cellMark);
					if (ai.getHead().getLocation().getX() != ai.getTail().getLocation().getX() &&
						ai.getHead().getLocation().getY() != ai.getTail().getLocation().getY())
							cell.setBit(cellMark);
				}
				for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
				{
					NodeInst ni = (NodeInst)nIt.next();
					if ((ni.getAngle() % 900) != 0) cell.setBit(cellMark);
				}
			}
		}

		// show the nonmanhattan things in the current cell
		int i = 0;
		for(Iterator aIt = curCell.getArcs(); aIt.hasNext(); )
		{
			ArcInst ai = (ArcInst)aIt.next();
			ArcProto ap = ai.getProto();
			if (ap.getTechnology() == Generic.tech || ap.getTechnology() == Artwork.tech ||
				ap.getTechnology() == Schematics.tech) continue;
			boolean nonMan = false;
			Variable var = ai.getVar(ArcInst.ARC_RADIUS);
			if (var != null) nonMan = true;
			if (ai.getHead().getLocation().getX() != ai.getTail().getLocation().getX() &&
				ai.getHead().getLocation().getY() != ai.getTail().getLocation().getY())
					nonMan = true;
			if (nonMan)
			{
				if (i == 0) Highlight.clear();
				Highlight.addElectricObject(ai, curCell);
				i++;
			}
		}
		for(Iterator nIt = curCell.getNodes(); nIt.hasNext(); )
		{
			NodeInst ni = (NodeInst)nIt.next();
			if ((ni.getAngle() % 900) == 0) continue;
			if (i == 0) Highlight.clear();
			Highlight.addElectricObject(ni, curCell);
			i++;
		}
		if (i == 0) System.out.println("No nonmanhattan objects in this cell"); else
		{
			Highlight.finished();
			System.out.println(i + " objects are not manhattan in this cell");
		}

		// tell about other non-manhatten-ness elsewhere
		for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = (Library)lIt.next();
			if (lib.isHidden()) continue;
			int numBad = 0;
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				if (cell.isBit(cellMark) && cell != curCell) numBad++;
			}
			if (numBad == 0) continue;
			if (lib == Library.getCurrent())
			{
				int cellsFound = 0;
				String infstr = "";
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					if (cell == curCell || !cell.isBit(cellMark)) continue;
					if (cellsFound > 0) infstr += " ";;
					infstr += cell.describe();
					cellsFound++;
				}
				if (cellsFound == 1)
				{
					System.out.println("Found nonmanhattan geometry in cell " + infstr);
				} else
				{
					System.out.println("Found nonmanhattan geometry in these cells: " + infstr);
				}
			} else
			{
				System.out.println("Found nonmanhattan geometry in library " + lib.getLibName());
			}
		}
		cellMark.freeFlagSet();
	}

	/**
	 * Method to shorten all selected arcs.
	 * Since arcs may connect anywhere inside of the ports on nodes, a port with nonzero area will allow an arc
	 * to move freely.
	 * This command shortens selected arcs so that their endpoints arrive at the part of the node that allows the shortest arc.
	 */
	public static void shortenArcsCommand()
	{
		ShortenArcs job = new ShortenArcs();
	}
	
	/**
	 * This class implements the changes needed to shorten selected arcs.
	 */
	private static class ShortenArcs extends Job
	{
		private ShortenArcs()
		{
			super("Shorten selected arcs", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.startJob();
		}

		public void doIt()
		{
			List selected = Highlight.getHighlighted(false, true);
			int l = 0;
			double [] dX = new double[2];
			double [] dY = new double[2];
			for(Iterator it = selected.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				for(int j=0; j<2; j++)
				{
					Poly portPoly = ai.getConnection(j).getPortInst().getPoly();
					double wid = ai.getWidth() - ai.getProto().getWidthOffset();
					portPoly.reducePortPoly(ai.getConnection(j).getPortInst(), wid, ai.getAngle());
					Point2D closest = portPoly.closestPoint(ai.getConnection(1-j).getLocation());
					dX[j] = closest.getX() - ai.getConnection(j).getLocation().getX();
					dY[j] = closest.getY() - ai.getConnection(j).getLocation().getY();
				}
				if (dX[0] != 0 || dY[0] != 0 || dX[1] != 0 || dY[1] != 0)
				{
					ai.modify(0, dX[0], dY[0], dX[1], dY[1]);
					l++;
				}
			}
			System.out.println("Shortened " + l + " arcs");
		}
	}

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
			if (!other.getProtoName().equalsIgnoreCase(cell.getProtoName())) continue;

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
	protected static class ChangeCellView extends Job
	{
		Cell cell;
		View newView;

		protected ChangeCellView(Cell cell, View newView)
		{
			super("Change View of Cell" + cell.describe() + " to " + newView.getFullName(),
				User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.newView = newView;
			this.startJob();
		}

		public void doIt()
		{
			cell.setView(newView);
			EditWindow.repaintAll();
		}
	}

	/****************************** MAKE A NEW VERSION OF A CELL ******************************/

	public static void newVersionOfCell(Cell cell)
	{
		NewCellVersion job = new NewCellVersion(cell);
	}

	/**
	 * This class implement the command to make a new version of a cell.
	 */
	protected static class NewCellVersion extends Job
	{
		Cell cell;

		protected NewCellVersion(Cell cell)
		{
			super("Create new Version of cell " + cell.describe(), User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.startJob();
		}

		public void doIt()
		{
			Cell newVersion = cell.makeNewVersion();
			if (newVersion == null) return;

			// change the display of old versions to the new one
			for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)it.next();
				EditWindow wnd = wf.getEditWindow();
				if (wnd.getCell() == cell)
					wnd.setCell(newVersion, null);
			}
			EditWindow.repaintAll();
		}
	}

	/****************************** MAKE AN ICON FOR A CELL ******************************/

	public static void makeIconViewCommand()
	{
        MakeIconView job = new MakeIconView();
	}

	protected static class MakeIconView extends Job
	{
		private static boolean reverseIconExportOrder;

		protected MakeIconView()
		{
			super("Make Icon View", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.startJob();
		}

		public void doIt()
		{
			Cell curCell = Library.needCurCell();
			if (curCell == null) return;
			Library lib = curCell.getLibrary();

			if (!curCell.isSchematicView())
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
					"Warning: Icon " + iconCell.describe() + " already exists.  Create a new version?");
				if (response != JOptionPane.YES_OPTION) return;
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
			String iconCellName = curCell.getProtoName() + "{ic}";
			iconCell = Cell.makeInstance(lib, iconCellName);
			if (iconCell == null)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Cannot create Icon cell " + iconCellName,
						"Icon creation failed", JOptionPane.ERROR_MESSAGE);
				return;
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
				bbNi = NodeInst.newInstance(Artwork.tech.boxNode, new Point2D.Double(0,0), xSize, ySize, 0, iconCell, null);
				if (bbNi == null) return;
				bbNi.newVar(Artwork.ART_COLOR, new Integer(EGraphics.RED));

				// put the original cell name on it
				Variable var = bbNi.newVar("SCHEM_function", curCell.getProtoName());
				if (var != null)
				{
					var.setDisplay();
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
				NodeInst.newInstance(Generic.tech.invisiblePinNode, new Point2D.Double(0,0), xSize, ySize, 0, iconCell, null);
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
			NodeInst ni = NodeInst.makeInstance(iconCell, iconPos, px, py, 0, curCell, null);
			if (ni != null)
			{
//				ni.setExpanded();

				// now inherit parameters that now do exist
				inheritAttributes(ni);

				Highlight.clear();
				Highlight.addElectricObject(ni, curCell);
				Highlight.finished();
//				if (lx > el_curwindowpart->screenhx || hx < el_curwindowpart->screenlx ||
//					ly > el_curwindowpart->screenhy || hy < el_curwindowpart->screenly)
//				{
//					newpar[0] = x_("center-highlight");
//					us_window(1, newpar);
//				}
			}
		}

		static class ExportSorted implements Comparator
		{
			public int compare(Object o1, Object o2)
			{
				Export e1 = (Export)o1;
				Export e2 = (Export)o2;
				String s1 = e1.getProtoName();
				String s2 = e2.getProtoName();
				if (reverseIconExportOrder)
					return s2.compareToIgnoreCase(s1);
				return s1.compareToIgnoreCase(s2);
			}
		}
	}

	/*
	 * Helper method to create an export in icon "np".  The export is from original port "pp",
	 * is on side "index" (0: left, 1: right, 2: top, 3: bottom), is at (xPos,yPos), and
	 * connects to the central box at (xBBPos,yBBPos).  Returns TRUE if the export is created.
	 * It uses icon style "style".
	 */
	private static boolean makeIconExport(Export pp, int index,
		double xPos, double yPos, double xBBPos, double yBBPos, Cell np)
	{
		// presume "universal" exports (Generic Invisible Pins)
		NodeProto pinType = Generic.tech.invisiblePinNode;
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
			wireType = Schematics.tech.bus_arc;

		// if the export is on the body (no leads) then move it in
		if (!User.isIconGenDrawLeads())
		{
			xPos = xBBPos;   yPos = yBBPos;
		}

		// make the pin with the port
		NodeInst pinNi = NodeInst.newInstance(pinType, new Point2D.Double(xPos, yPos), pinSizeX, pinSizeY, 0, np, null);
		if (pinNi == null) return false;

		// export the port that should be on this pin
		PortInst pi = pinNi.getOnlyPortInst();
		Export port = Export.newInstance(np, pi, pp.getProtoName());
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
			port.copyVars(pp);
		}

		// add lead if requested
		if (User.isIconGenDrawLeads())
		{
			pinType = wireType.findPinProto();
			double wid = pinType.getDefWidth();
			double hei = pinType.getDefHeight();
			NodeInst ni = NodeInst.newInstance(pinType, new Point2D.Double(xBBPos, yBBPos), wid, hei, 0, np, null);
			if (ni != null)
			{
				PortInst head = ni.getOnlyPortInst();
				PortInst tail = pinNi.getOnlyPortInst();
				ArcInst ai = ArcInst.makeInstance(wireType, wireType.getDefaultWidth(),
					head, new Point2D.Double(xBBPos, yBBPos),
					tail, new Point2D.Double(xPos, yPos), null);
			}
		}
		return true;
	}

	/*
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

	/****************************** MAKE A COPY OF A CELL ******************************/

	public static void duplicateCell(Cell cell, String newName)
	{
		DuplicateCell job = new DuplicateCell(cell, newName);
	}

	/**
	 * This class implement the command to duplicate a cell.
	 */
	protected static class DuplicateCell extends Job
	{
		Cell cell;
		String newName;

		protected DuplicateCell(Cell cell, String newName)
		{
			super("Duplicate cell " + cell.describe(), User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.newName = newName;
			this.startJob();
		}

		public void doIt()
		{
			Cell dupCell = Cell.copynodeproto(cell, cell.getLibrary(), newName + "{" + cell.getView().getAbbreviation() + "}", false);
			if (dupCell == null) return;

			// change the display of old cell to the new one
			for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)it.next();
				EditWindow wnd = wf.getEditWindow();
				if (wnd.getCell() == cell)
					wnd.setCell(dupCell, null);
			}
			EditWindow.repaintAll();
		}
	}

	/****************************** MOVE SELECTED OBJECTS ******************************/

	/**
	 * Method to move the arcs in the GEOM module list "list" (terminated by
	 * NOGEOM) and the "total" nodes in the list "nodelist" by (dX, dY).
	 */
	public static void manyMove(double dX, double dY, EditWindow wnd)
	{
        ManyMove job = new ManyMove(dX, dY, wnd);
	}

	protected static class ManyMove extends Job
	{
		double dX, dY;
		EditWindow wnd;

		protected ManyMove(double dX, double dY, EditWindow wnd)
		{
			super("Move", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dX = dX;   this.dY = dY;
			this.wnd = wnd;
			this.startJob();
		}

		public void doIt()
		{
			// get information about what is highlighted
			int total = Highlight.getNumHighlights();
			if (total <= 0) return;
			List highlightedText = Highlight.getHighlightedText();
			Iterator oit = Highlight.getHighlights();
			Highlight firstH = (Highlight)oit.next();
			ElectricObject firstEObj = firstH.getElectricObject();
			Cell cell = firstH.getCell();

			// special case if moving only one node
			if (total == 1 && firstH.getType() == Highlight.Type.EOBJ &&
				firstEObj instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)firstEObj;
				ni.modifyInstance(dX, dY, 0, 0, 0);
				return;
			}

			// special case if moving diagonal fixed-angle arcs connected to single manhattan arcs
			boolean found = false;
			for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.EOBJ) continue;
				ElectricObject eobj = h.getElectricObject();
				if (eobj instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)eobj;
					if (ai.getHead().getLocation().getX() != ai.getTail().getLocation().getX() &&
						ai.getHead().getLocation().getY() != ai.getTail().getLocation().getY())
					{
						if (ai.isFixedAngle() && !ai.isRigid())
						{
							int j;
							for(j=0; j<2; j++)
							{
								NodeInst ni = ai.getConnection(j).getPortInst().getNodeInst();
								ArcInst oai = null;
								for(Iterator pIt = ni.getConnections(); pIt.hasNext(); )
								{
									Connection con = (Connection)pIt.next();
									if (con.getArc() == ai) continue;
									if (oai == null) oai = con.getArc(); else
									{
										oai = null;
										break;
									}
								}
								if (oai == null) break;
								if (oai.getHead().getLocation().getX() != oai.getTail().getLocation().getX() &&
									oai.getHead().getLocation().getY() != oai.getTail().getLocation().getY()) break;
							}
							if (j >= 2) { found = true;   break; }
						}
					}
				}
			}
			if (found)
			{
				// meets the test: make the special move to slide other orthogonal arcs
				for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
				{
					Highlight h = (Highlight)it.next();
					if (h.getType() != Highlight.Type.EOBJ) continue;
					ElectricObject eobj = h.getElectricObject();
					if (!(eobj instanceof ArcInst)) continue;
					ArcInst ai = (ArcInst)eobj;

					double [] deltaXs = new double[2];
					double [] deltaYs = new double[2];
					double [] deltaNulls = new double[2];
					int [] deltaRots = new int[2];
					NodeInst [] niList = new NodeInst[2];
					deltaNulls[0] = deltaNulls[1] = 0;
					deltaXs[0] = deltaYs[0] = deltaXs[1] = deltaYs[1] = 0;
					int arcangle = ai.getAngle();
					int j;
					for(j=0; j<2; j++)
					{
						NodeInst ni = ai.getConnection(j).getPortInst().getNodeInst();
						niList[j] = ni;
						ArcInst oai = null;
						for(Iterator pIt = ni.getConnections(); pIt.hasNext(); )
						{
							Connection con = (Connection)pIt.next();
							if (con.getArc() != ai) { oai = con.getArc();   break; }
						}
						if (oai == null) break;
						if (EMath.doublesEqual(oai.getHead().getLocation().getX(), oai.getTail().getLocation().getX()))
						{
							Point2D iPt = EMath.intersect(oai.getHead().getLocation(), 900,
								new Point2D.Double(ai.getHead().getLocation().getX()+dX, ai.getHead().getLocation().getY()+dY), arcangle);
							deltaXs[j] = iPt.getX() - ai.getConnection(j).getLocation().getX();
							deltaYs[j] = iPt.getY() - ai.getConnection(j).getLocation().getY();
						} else if (EMath.doublesEqual(oai.getHead().getLocation().getY(), oai.getTail().getLocation().getY()))
						{
							Point2D iPt = EMath.intersect(oai.getHead().getLocation(), 0,
								new Point2D.Double(ai.getHead().getLocation().getX()+dX, ai.getHead().getLocation().getY()+dY), arcangle);
							deltaXs[j] = iPt.getX() - ai.getConnection(j).getLocation().getX();
							deltaYs[j] = iPt.getY() - ai.getConnection(j).getLocation().getY();
						}
					}
					if (j < 2) continue;
					deltaRots[0] = deltaRots[1] = 0;
					NodeInst.modifyInstances(niList, deltaXs, deltaYs, deltaNulls, deltaNulls, deltaRots);
				}
				return;
			}

			// special case if moving only arcs and they slide
			boolean onlySlidable = true, foundArc = false;
			for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.EOBJ) continue;
				ElectricObject eobj = h.getElectricObject();
				if (eobj instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)eobj;
					foundArc = true;
					// see if the arc moves in its ports
					if (ai.isSlidable())
					{
						Connection head = ai.getHead();
						Connection tail = ai.getTail();
						Point2D newHead = new Point2D.Double(head.getLocation().getX()+dX, head.getLocation().getY()+dY);
						Point2D newTail = new Point2D.Double(tail.getLocation().getX()+dX, tail.getLocation().getY()+dY);
						if (ai.stillInPort(head, newHead, true) && ai.stillInPort(tail, newTail, true)) continue;
					}
				}
				onlySlidable = false;
			}
			if (foundArc && onlySlidable)
			{
				for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
				{
					Highlight h = (Highlight)it.next();
					if (h.getType() != Highlight.Type.EOBJ) continue;
					ElectricObject eobj = h.getElectricObject();
					if (eobj instanceof ArcInst)
					{
						ArcInst ai = (ArcInst)eobj;
						ai.modify(0, dX, dY, dX, dY);
					}
				}
				return;
			}

			// make flag to track the nodes that move
			FlagSet flag = Geometric.getFlagSet(1);

			// remember the location of every node and arc
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni.clearBit(flag);
				ni.setTempObj(new Point2D.Double(ni.getGrabCenterX(), ni.getGrabCenterY()));
			}
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ai.setTempObj(new Point2D.Double(ai.getTrueCenterX(), ai.getTrueCenterY()));
			}

			int numNodes = 0;
			for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.EOBJ) continue;
				ElectricObject eobj = h.getElectricObject();
				if (eobj instanceof PortInst) eobj = ((PortInst)eobj).getNodeInst();
				if (eobj instanceof NodeInst)
				{
					NodeInst ni = (NodeInst)eobj;
					if (!ni.isBit(flag)) numNodes++;
					ni.setBit(flag);
				} else if (eobj instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)eobj;
					NodeInst ni1 = ai.getHead().getPortInst().getNodeInst();
					NodeInst ni2 = ai.getTail().getPortInst().getNodeInst();
					if (!ni1.isBit(flag)) numNodes++;
					if (!ni2.isBit(flag)) numNodes++;
					ni1.setBit(flag);
					ni2.setBit(flag);
					Layout.setTempRigid(ai, true);
				}
			}

			// look at all nodes and move them appropriately
			if (numNodes > 0)
			{
				NodeInst [] nis = new NodeInst[numNodes];
				double [] dXs = new double[numNodes];
				double [] dYs = new double[numNodes];
				double [] dSize = new double[numNodes];
				int [] dRot = new int[numNodes];
				boolean [] dTrn = new boolean[numNodes];
				numNodes = 0;
				for(Iterator it = cell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = (NodeInst)it.next();
					if (!ni.isBit(flag)) continue;
					nis[numNodes] = ni;
					dXs[numNodes] = dX;
					dYs[numNodes] = dY;
					dSize[numNodes] = 0;
					dRot[numNodes] = 0;
					dTrn[numNodes] = false;
					numNodes++;
				}
				NodeInst.modifyInstances(nis, dXs, dYs, dSize, dSize, dRot);
			}
			flag.freeFlagSet();

			// look at all arcs and move them appropriately
			for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.EOBJ) continue;
				ElectricObject eobj = h.getElectricObject();
				if (!(eobj instanceof ArcInst)) continue;
				ArcInst ai = (ArcInst)eobj;
				Point2D pt = (Point2D)ai.getTempObj();
				if (pt.getX() != ai.getTrueCenterX() ||
					pt.getY() != ai.getTrueCenterY()) continue;

				// see if the arc moves in its ports
				boolean headInPort = false, tailInPort = false;
				if (!ai.isRigid() && ai.isSlidable())
				{
					headInPort = ai.stillInPort(ai.getHead(),
						new Point2D.Double(ai.getHead().getLocation().getX()+dX, ai.getHead().getLocation().getY()+dY), true);
					tailInPort = ai.stillInPort(ai.getTail(),
						new Point2D.Double(ai.getTail().getLocation().getX()+dX, ai.getTail().getLocation().getY()+dY), true);
				}

				// if both ends slide in their port, move the arc
				if (headInPort && tailInPort)
				{
					ai.modify(0, dX, dY, dX, dY);
					continue;
				}

				// if neither end can slide in its port, move the nodes
				if (!headInPort && !tailInPort)
				{
					for(int k=0; k<2; k++)
					{
						NodeInst ni;
						if (k == 0) ni = ai.getHead().getPortInst().getNodeInst(); else
							ni = ai.getTail().getPortInst().getNodeInst();
						Point2D nPt = (Point2D)ni.getTempObj();
						if (ni.getGrabCenterX() != nPt.getX() || ni.getGrabCenterY() != nPt.getY()) continue;

						// fix all arcs that aren't sliding
						for(Iterator oIt = Highlight.getHighlights(); oIt.hasNext(); )
						{
							Highlight oH = (Highlight)oIt.next();
							if (oH.getType() != Highlight.Type.EOBJ) continue;
							ElectricObject oEObj = oH.getElectricObject();
							if (oEObj instanceof ArcInst)
							{
								ArcInst oai = (ArcInst)oEObj;
								Point2D aPt = (Point2D)oai.getTempObj();
								if (aPt.getX() != oai.getTrueCenterX() ||
									aPt.getY() != oai.getTrueCenterY()) continue;
								if (oai.stillInPort(oai.getHead(),
										new Point2D.Double(ai.getHead().getLocation().getX()+dX, ai.getHead().getLocation().getY()+dY), true) ||
									oai.stillInPort(oai.getTail(),
										new Point2D.Double(ai.getTail().getLocation().getX()+dX, ai.getTail().getLocation().getY()+dY), true))
											continue;
								Layout.setTempRigid(oai, true);
							}
						}
						ni.modifyInstance(dX - (ni.getGrabCenterX() - nPt.getX()),
							dY - (ni.getGrabCenterY() - nPt.getY()), 0, 0, 0);
					}
					continue;
				}

//				// only one end is slidable: move other node and the arc
//				for(int k=0; k<2; k++)
//				{
//					if (e[k] != 0) continue;
//					ni = ai->end[k].nodeinst;
//					if (ni->lowx == ni->temp1 && ni->lowy == ni->temp2)
//					{
//						// node "ni" hasn't moved yet but must because arc motion forces it
//						for(j=0; list[j] != NOGEOM; j++)
//						{
//							if (list[j]->entryisnode) continue;
//							oai = list[j]->entryaddr.ai;
//							if (oai->temp1 != (oai->end[0].xpos + oai->end[1].xpos) / 2 ||
//								oai->temp2 != (oai->end[0].ypos + oai->end[1].ypos) / 2) continue;
//							if (oai->end[0].nodeinst == ni) otherend = 1; else otherend = 0;
//							if (db_stillinport(oai, otherend, ai->end[otherend].xpos+dX,
//								ai->end[otherend].ypos+dY)) continue;
//							(void)(*el_curconstraint->setobject)((INTBIG)oai,
//								VARCINST, CHANGETYPETEMPRIGID, 0);
//						}
//						startobjectchange((INTBIG)ni, VNODEINST);
//						modifynodeinst(ni, dX-(ni->lowx-ni->temp1), dY-(ni->lowy-ni->temp2),
//							dX-(ni->lowx-ni->temp1), dY-(ni->lowy-ni->temp2), 0, 0);
//						endobjectchange((INTBIG)ni, VNODEINST);
//
//						if (ai->temp1 != (ai->end[0].xpos + ai->end[1].xpos) / 2 ||
//							ai->temp2 != (ai->end[0].ypos + ai->end[1].ypos) / 2) continue;
//						startobjectchange((INTBIG)ai, VARCINST);
//						(void)modifyarcinst(ai, 0, dX, dY, dX, dY);
//						endobjectchange((INTBIG)ai, VARCINST);
//					}
//				}
			}

			// remove coordinate objects on nodes and arcs
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni.setTempObj(null);
			}
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ai.setTempObj(null);
			}

			// also move selected text
			moveSelectedText(highlightedText);
		}

		/*
		 * Method to move the "numtexts" text objects described (as highlight strings)
		 * in the array "textlist", by "odx" and "ody".  Geometry objects in "list" (NOGEOM-terminated)
		 * and the "total" nodes in "nodelist" have already been moved, so don't move any text that
		 * is on these objects.
		 */
		private void moveSelectedText(List highlightedText)
		{
			for(Iterator it = highlightedText.iterator(); it.hasNext(); )
			{
				Highlight high = (Highlight)it.next();

				// disallow moving if lock is on
				Cell np = high.getCell();
				if (np != null)
				{
					if (cantEdit(np, null, true)) continue;
				}

				// handle nodes that move with text
				ElectricObject eobj = high.getElectricObject();
				if (high.nodeMovesWithText())
				{
					NodeInst ni = null;
					if (eobj instanceof NodeInst) ni = (NodeInst)eobj;
					if (eobj instanceof Export) ni = ((Export)eobj).getOriginalPort().getNodeInst();
					if (ni != null)
					{
						ni.modifyInstance(dX, dY, 0, 0, 0);
						continue;
					}
				}

				// moving variable on object
				Variable var = high.getVar();
				if (var != null)
				{
					TextDescriptor td = var.getTextDescriptor();
					if (eobj instanceof NodeInst || eobj instanceof PortInst || eobj instanceof Export)
					{
						NodeInst ni = null;
						if (eobj instanceof NodeInst) ni = (NodeInst)eobj; else
							if (eobj instanceof PortInst) ni = ((PortInst)eobj).getNodeInst(); else
								if (eobj instanceof Export) ni = ((Export)eobj).getOriginalPort().getNodeInst();
						if (ni != null)
						{
							adjustTextDescriptor(td, ni);
						}
					} else
					{
						td.setOff(td.getXOff()+dX, td.getYOff()+dY);
					}
				} else
				{
					if (high.getName() != null)
					{
						TextDescriptor td = ((Geometric)eobj).getNameTextDescriptor();
						if (eobj instanceof NodeInst)
						{
							NodeInst ni = (NodeInst)eobj;
							adjustTextDescriptor(td, ni);
						} else
							td.setOff(td.getXOff()+dX, td.getYOff()+dY);
					} else
					{
						if (eobj instanceof Export)
						{
							Export pp = (Export)eobj;
							TextDescriptor td = pp.getTextDescriptor();
							adjustTextDescriptor(td, pp.getOriginalPort().getNodeInst());
						}
					}
				}
			}
		}

		private void adjustTextDescriptor(TextDescriptor td, NodeInst ni)
		{
			Point2D curLoc = new Point2D.Double(ni.getGrabCenterX()+td.getXOff(), ni.getGrabCenterY()+td.getYOff());
			AffineTransform rotateOut = ni.rotateOut();
			rotateOut.transform(curLoc, curLoc);
			curLoc.setLocation(curLoc.getX()+dX, curLoc.getY()+dY);
			AffineTransform rotateIn = ni.rotateIn();
			rotateIn.transform(curLoc, curLoc);
			td.setOff(curLoc.getX()-ni.getGrabCenterX(), curLoc.getY()-ni.getGrabCenterY());
		}
	}

	/**
	 * This class handles deleting pins that are between two arcs,
	 * and reconnecting the arcs without the pin.
	 */
	static class Reconnect
	{
		/** node in the center of the reconnect */			private NodeInst ni;
		/** number of arcs found on this reconnection */	private int arcsFound;
		/** coordinate at other end of arc */				private Point2D [] recon;
		/** coordinate where arc hits deleted node */		private Point2D [] orig;
		/** distance between ends */						private Point2D [] delta;
		/** port at other end of arc */						private PortInst [] reconPi;
		/** arcinst being reconnected */					private ArcInst [] reconAr;
		/** prototype of new arc */							private ArcProto ap;
		/** width of new arc */								private double wid;
		/** true to make new arc directional */				private boolean directional;
		/** true to make new arc negated */					private boolean negated;
		/** true to ignore the head of the new arc */		private boolean ignoreHead;
		/** true to ignore the tail of the new arc */		private boolean ignoreTail;
		/** true to reverse the head/tail on new arc */		private boolean reverseEnd;

		/**
		 * Method to find a possible Reconnect through a given NodeInst.
		 * @param ni the NodeInst to examine.
		 * @param allowDiffs true to allow differences in the two arcs.
		 * If this is false, then different width arcs, or arcs that are not lined up
		 * precisely, will not be considered for reconnection.
		 * @return a Reconnect object that describes the reconnection to be done.
		 * Returns null if no reconnection can be found.
		 */
		public static Reconnect erasePassThru(NodeInst ni, boolean allowdiffs)
		{
			// disallow erasing if lock is on
			Cell cell = ni.getParent();
			if (cantEdit(cell, ni, true)) return null;
			Netlist netlist = cell.getUserNetlist();

			// look for pairs arcs that will get reconnected
			List reconList = new ArrayList();
			for(Iterator it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				PortInst pi = con.getPortInst();
				ArcInst ai = con.getArc();

				// ignore arcs that connect from the node to itself
				if (ai.getHead().getPortInst().getNodeInst() == ai.getTail().getPortInst().getNodeInst())
					continue;

				// find a "reconnect" object with this network
				Reconnect reFound = null;
				for(Iterator rIt = reconList.iterator(); rIt.hasNext(); )
				{
					Reconnect re = (Reconnect)rIt.next();
					if (netlist.sameNetwork(ai, re.reconAr[0])) { reFound = re;   break; }
				}
				if (reFound == null)
				{
					reFound = new Reconnect();
					reFound.ni = ni;
					reFound.recon = new Point2D[2];
					reFound.orig = new Point2D[2];
					reFound.delta = new Point2D[2];
					reFound.reconPi = new PortInst[2];
					reFound.reconAr = new ArcInst[2];
					reFound.arcsFound = 0;
					reconList.add(reFound);
				}
				int j = reFound.arcsFound;
				reFound.arcsFound++;
				if (reFound.arcsFound > 2) continue;
				reFound.reconAr[j] = ai;
				for(int i=0; i<2; i++)
				{
					if (ai.getConnection(i).getPortInst().getNodeInst() == ni) continue;
					reFound.reconPi[j] = ai.getConnection(i).getPortInst();
					reFound.recon[j] = ai.getConnection(i).getLocation();
					reFound.orig[j] = ai.getConnection(1-i).getLocation();
					reFound.delta[j] = new Point2D.Double(reFound.recon[j].getX() - reFound.orig[j].getX(),
						reFound.recon[j].getY() - reFound.orig[j].getY());
				}
			}

			// examine all of the reconnection situations
			for(Iterator rIt = reconList.iterator(); rIt.hasNext(); )
			{
				Reconnect re = (Reconnect)rIt.next();
				if (re.arcsFound != 2) continue;

				// verify that the two arcs to merge have the same type
				if (re.reconAr[0].getProto() != re.reconAr[1].getProto()) { re.arcsFound = -1; continue; }
				re.ap = re.reconAr[0].getProto();

				if (!allowdiffs)
				{
					// verify that the two arcs to merge have the same width
					if (re.reconAr[0].getWidth() != re.reconAr[1].getWidth()) { re.arcsFound = -2; continue; }

					// verify that the two arcs have the same slope
					if ((re.delta[1].getX()*re.delta[0].getY()) != (re.delta[0].getX()*re.delta[1].getY())) { re.arcsFound = -3; continue; }
					if (re.orig[0].getX() != re.orig[1].getX() || re.orig[0].getY() != re.orig[1].getY())
					{
						// did not connect at the same location: be sure that angle is consistent
						if (re.delta[0].getX() != 0 || re.delta[0].getY() != 0)
						{
							if (((re.orig[0].getX()-re.orig[1].getX())*re.delta[0].getY()) !=
								(re.delta[0].getX()*(re.orig[0].getY()-re.orig[1].getY()))) { re.arcsFound = -3; continue; }
						} else if (re.delta[1].getX() != 0 || re.delta[1].getY() != 0)
						{
							if (((re.orig[0].getX()-re.orig[1].getX())*re.delta[1].getY()) !=
								(re.delta[1].getX()*(re.orig[0].getY()-re.orig[1].getY()))) { re.arcsFound = -3; continue; }
						} else { re.arcsFound = -3; continue; }
					}
				}

				// remember facts about the new arcinst
				re.wid = re.reconAr[0].getWidth();

				re.directional = re.reconAr[0].isDirectional() || re.reconAr[1].isDirectional();
				re.negated = re.reconAr[0].isNegated() || re.reconAr[1].isNegated();
				if (re.reconAr[0].isNegated() && re.reconAr[1].isNegated()) re.negated = false;
				re.ignoreHead = re.reconAr[0].isSkipHead() || re.reconAr[1].isSkipHead();
				re.ignoreTail = re.reconAr[0].isSkipTail() || re.reconAr[1].isSkipTail();
				re.reverseEnd = re.reconAr[0].isReverseEnds() || re.reconAr[1].isReverseEnds();

				// special code to handle directionality
				if (re.directional || re.negated || re.ignoreHead || re.ignoreTail || re.reverseEnd)
				{
					// reverse ends if the arcs point the wrong way
					for(int i=0; i<2; i++)
					{
						if (re.reconAr[i].getConnection(i).getPortInst().getNodeInst() == ni)
						{
							if (re.reconAr[i].isReverseEnds()) re.reverseEnd = false; else
								re.reverseEnd = true;									
						}
					}
				}
			}

			// see if any reconnection will be done
			Reconnect reFound = null;
			for(Iterator rIt = reconList.iterator(); rIt.hasNext(); )
			{
				Reconnect re = (Reconnect)rIt.next();
				if (re.arcsFound == 2) { reFound = re;   break; }
			}
			return reFound;
		}

		/**
		 * Method to implement the reconnection in this Reconnect.
		 * @return the newly created ArcInst that reconnects.
		 */
		public ArcInst reconnectArcs()
		{
			// kill the intermediate pin
			eraseNodeInst(ni);

			// reconnect the arcs
			ArcInst newai = ArcInst.makeInstance(ap, wid,
				reconPi[0], recon[0], reconPi[1], recon[1], null);
			if (newai == null) return null;
			if (directional) newai.setDirectional();
			if (negated) newai.setNegated();
			if (ignoreHead) newai.setSkipHead();
			if (ignoreTail) newai.setSkipTail();
			if (reverseEnd) newai.setReverseEnds();

			reconAr[0].copyVars(newai);
			reconAr[1].copyVars(newai);

			return newai;
		}
	};

	/****************************** COPY CELLS ******************************/

	/**
	 * recursive helper method for "us_copycell" which copies cell "fromnp"
	 * to a new cell called "toname" in library "tolib" with the new view type
	 * "toview".  All needed subcells are copied (unless "nosubcells" is true).
	 * All shared view cells referenced by variables are copied too
	 * (unless "norelatedviews" is true).  If "useexisting" is TRUE, any subcells
	 * that already exist in the destination library will be used there instead of
	 * creating a cross-library reference.
	 * If "move" is nonzero, delete the original after copying, and update all
	 * references to point to the new cell.  If "subdescript" is empty, the operation
	 * is a top-level request.  Otherwise, this is for a subcell, so only create a
	 * new cell if one with the same name and date doesn't already exists.
	 */
	public static Cell copyRecursively(Cell fromnp, String toname, Library tolib,
		View toview, boolean verbose, boolean move, String subdescript, boolean norelatedviews,
		boolean nosubcells, boolean useexisting)
	{
		Date fromnp_creationdate = fromnp.getCreationDate();
		Date fromnp_revisiondate = fromnp.getRevisionDate();

		// see if the cell is already there
		for(Iterator it = tolib.getCells(); it.hasNext(); )
		{
			Cell newfromnp = (Cell)it.next();
			if (!newfromnp.getProtoName().equalsIgnoreCase(toname)) continue;
			if (newfromnp.getView() != toview) continue;
			if (!newfromnp.getCreationDate().equals(fromnp.getCreationDate())) continue;
			if (!newfromnp.getRevisionDate().equals(fromnp.getRevisionDate())) continue;
			if (subdescript != null) return newfromnp;
			break;
		}

		// copy subcells
		if (!nosubcells)
		{
			boolean found = true;
			while (found)
			{
				found = false;
				for(Iterator it = fromnp.getNodes(); it.hasNext(); )
				{
					NodeInst ni = (NodeInst)it.next();
					NodeProto np = ni.getProto();
					if (!(np instanceof Cell)) continue;
					Cell cell = (Cell)np;

					// allow cross-library references to stay
					if (cell.getLibrary() != fromnp.getLibrary()) continue;
					if (cell.getLibrary() == tolib) continue;

					// see if the cell is already there
					if (us_indestlib(cell, tolib)) continue;

					// copy subcell if not already there
					Cell onp = copyRecursively(cell, cell.getProtoName(), tolib, cell.getView(),
						verbose, move, "subcell ", norelatedviews, nosubcells, useexisting);
					if (onp == null)
					{
						if (move) System.out.println("Move of subcell " + cell.describe() + " failed"); else
							System.out.println("Copy of subcell " + cell.describe() + " failed");
						return null;
					}
					found = true;
					break;
				}
			}
		}

		// also copy equivalent views
		if (!norelatedviews)
		{
			// first copy the icons
			boolean found = true;
			Cell fromnpwalk = fromnp;
			while (found)
			{
				found = false;
				for(Iterator it = fromnpwalk.getCellGroup().getCells(); it.hasNext(); )
				{
					Cell np = (Cell)it.next();
					if (np.getView() != View.ICON) continue;

					// see if the cell is already there
					if (us_indestlib(np, tolib)) continue;

					// copy equivalent view if not already there
//					if (move) fromnpwalk = np->nextcellgrp; // if np is moved (i.e. deleted), circular linked list is broken
					Cell onp = copyRecursively(np, np.getProtoName(), tolib, np.getView(),
						verbose, move, "alternate view ", true, nosubcells, useexisting);
					if (onp == null)
					{
						if (move) System.out.println("Move of alternate view " + np.describe() + " failed"); else
							System.out.println("Copy of alternate view " + np.describe() + " failed");
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
				for(Iterator it = fromnpwalk.getCellGroup().getCells(); it.hasNext(); )
				{
					Cell np = (Cell)it.next();
					if (np.getView() == View.ICON) continue;

					// see if the cell is already there
					if (us_indestlib(np, tolib)) continue;

					// copy equivalent view if not already there
//					if (move) fromnpwalk = np->nextcellgrp; // if np is moved (i.e. deleted), circular linked list is broken
					Cell onp = copyRecursively(np, np.getProtoName(), tolib, np.getView(),
						verbose, move, "alternate view ", true, nosubcells, useexisting);
					if (onp == null)
					{
						if (move) System.out.println("Move of alternate view " + np.describe() + " failed"); else
							System.out.println("Copy of alternate view " + np.describe() + " failed");
						return null;
					}
					found = true;
					break;
				}
			}
		}

		// see if the cell is NOW there
		Cell beforenewfromnp = null;
		Cell newfromnp = null;
		for(Iterator it = tolib.getCells(); it.hasNext(); )
		{
			Cell thisCell = (Cell)it.next();
			if (!thisCell.getProtoName().equalsIgnoreCase(toname)) continue;
			if (thisCell.getView() != toview) continue;
			if (thisCell.getCreationDate() != fromnp_creationdate) continue;
			if (!move && thisCell.getRevisionDate() != fromnp_revisiondate) continue; // moving icon of schematic changes schematic's revision date
			if (subdescript != null) return thisCell;
			newfromnp = thisCell;
			break;
		}
		if (beforenewfromnp == newfromnp || newfromnp == null)
		{
			// copy the cell
			String newname = toname;
			if (toview.getAbbreviation().length() > 0)
			{
				newname = toname + "{" + toview.getAbbreviation() + "}";
			}
			newfromnp = Cell.copynodeproto(fromnp, tolib, newname, useexisting);
			if (newfromnp == null)
			{
				if (move)
				{
					System.out.println("Move of " + subdescript + fromnp.describe() + " failed");
				} else
				{
					System.out.println("Copy of " + subdescript + fromnp.describe() + " failed");
				}
				return null;
			}

			// ensure that the copied cell is the right size
//			(*el_curconstraint->solve)(newfromnp);

			// if moving, adjust pointers and kill original cell
			if (move)
			{
				// ensure that the copied cell is the right size
//				(*el_curconstraint->solve)(newfromnp);

				// clear highlighting if the current node is being replaced
//				list = us_gethighlighted(WANTNODEINST, 0, 0);
//				for(i=0; list[i] != NOGEOM; i++)
//				{
//					if (!list[i]->entryisnode) continue;
//					ni = list[i]->entryaddr.ni;
//					if (ni->proto == fromnp) break;
//				}
//				if (list[i] != NOGEOM) us_clearhighlightcount();

				// now replace old instances with the moved one
				for(Iterator it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = (Library)it.next();
					for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell np = (Cell)cIt.next();
						boolean found = true;
						while (found)
						{
							found = false;
							for(Iterator nIt = np.getNodes(); nIt.hasNext(); )
							{
								NodeInst ni = (NodeInst)nIt.next();
								if (ni.getProto() == fromnp)
								{
									NodeInst replacedNi = ni.replace(newfromnp, false, false);
									if (replacedNi == null)
										System.out.println("Error moving node " + ni.describe() + " in cell " + np.describe());
									found = true;
									break;
								}
							}
						}
					}
				}
//				toolturnoff(net_tool, FALSE);
				doKillCell(fromnp);
//				toolturnon(net_tool);
				fromnp = null;
			}

			if (verbose)
			{
				if (Library.getCurrent() != tolib)
				{
					String msg = "";
					if (move) msg += "Moved "; else
						 msg += "Copied ";
					msg += subdescript + Library.getCurrent().getLibName() + ":" + newfromnp.noLibDescribe() +
						" to library " + tolib.getLibName();
					System.out.println(msg);
				} else
				{
					System.out.println("Copied " + subdescript + newfromnp.describe());
				}
			}
		}
		return newfromnp;
	}

	/*
	 * Method to return true if a cell like "np" exists in library "lib".
	 */
	private static boolean us_indestlib(Cell np, Library lib)
	{
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell onp = (Cell)it.next();
			if (!onp.getProtoName().equalsIgnoreCase(np.getProtoName())) continue;
			if (onp.getView() != np.getView()) continue;
			if (onp.getCreationDate() != np.getCreationDate()) continue;
			if (onp.getRevisionDate() != np.getRevisionDate()) continue;
			return true;
		}
		return false;
	}

	/****************************** CHANGE CELL EXPANSION ******************************/

	private static FlagSet expandFlagBit;

	/**
	 * Class to read a library in a new thread.
	 */
	protected static class ExpandUnExpand extends Job
	{
		List list;
		boolean unExpand;
		int amount;
		
		protected ExpandUnExpand(List list, boolean unExpand, int amount)
		{
			super("Change Cell Expansion", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.list = list;
			this.unExpand = unExpand;
			this.amount = amount;
			this.startJob();
		}

		public void doIt()
		{
			expandFlagBit = Geometric.getFlagSet(1);
			if (unExpand)
			{
				for(Iterator it = list.iterator(); it.hasNext(); )
				{
					NodeInst ni = (NodeInst)it.next();
					NodeProto np = ni.getProto();
					if (!(np instanceof Cell)) continue;
					{
						if (ni.isExpanded())
							setUnExpand(ni, amount);
					}
				}
			}
			for(Iterator it = list.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (unExpand) doUnExpand(ni); else
					doExpand(ni, amount, 0);
				Undo.redrawObject(ni);
			}
			expandFlagBit.freeFlagSet();
			EditWindow.repaintAllContents();
		}
	}

	/**
	 * Method to recursively expand the cell "ni" by "amount" levels.
	 * "sofar" is the number of levels that this has already been expanded.
	 */
	private static void doExpand(NodeInst ni, int amount, int sofar)
	{
		if (!ni.isExpanded())
		{
			// expanded the cell
			ni.setExpanded();

			// if depth limit reached, quit
			if (++sofar >= amount) return;
		}

		// explore insides of this one
		NodeProto np = ni.getProto();
		if (!(np instanceof Cell)) return;
		Cell cell = (Cell)np;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst subNi = (NodeInst)it.next();
			NodeProto subNp = subNi.getProto();
			if (!(subNp instanceof Cell)) continue;
			Cell subCell = (Cell)subNp;

			// ignore recursive references (showing icon in contents)
			if (subNi.isIconOfParent()) continue;
			doExpand(subNi, amount, sofar);
		}
	}

	private static void doUnExpand(NodeInst ni)
	{
		if (!ni.isExpanded()) return;

		NodeProto np = ni.getProto();
		if (!(np instanceof Cell)) return;
		Cell cell = (Cell)np;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst subNi = (NodeInst)it.next();
			NodeProto subNp = subNi.getProto();
			if (!(subNp instanceof Cell)) continue;

			// ignore recursive references (showing icon in contents)
			if (subNi.isIconOfParent()) continue;
			if (subNi.isExpanded()) doUnExpand(subNi);
		}

		// expanded the cell
		if (ni.isBit(expandFlagBit))
		{
			ni.clearExpanded();
		}
	}

	private static int setUnExpand(NodeInst ni, int amount)
	{
		ni.clearBit(expandFlagBit);
		if (!ni.isExpanded()) return(0);
		NodeProto np = ni.getProto();
		int depth = 0;
		if (np instanceof Cell)
		{
			Cell cell = (Cell)np;
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst subNi = (NodeInst)it.next();
				NodeProto subNp = subNi.getProto();
				if (!(subNp instanceof Cell)) continue;

				// ignore recursive references (showing icon in contents)
				if (subNi.isIconOfParent()) continue;
				if (subNi.isExpanded())
					depth = Math.max(depth, setUnExpand(subNi, amount));
			}
			if (depth < amount) ni.setBit(expandFlagBit);
		}
		return depth+1;
	}

	/****************************** NODE AND ARC REPLACEMENT ******************************/

	static class PossibleVariables
	{
		Variable.Key varKey;
		PrimitiveNode pn;

		private PossibleVariables(String varName, PrimitiveNode pn)
		{
			this.varKey = ElectricObject.newKey(varName);
			this.pn = pn;
		}
		public static final PossibleVariables [] list = new PossibleVariables []
		{
			new PossibleVariables("ATTR_length",       Schematics.tech.transistorNode),
			new PossibleVariables("ATTR_length",       Schematics.tech.transistor4Node),
			new PossibleVariables("ATTR_width",        Schematics.tech.transistorNode),
			new PossibleVariables("ATTR_width",        Schematics.tech.transistor4Node),
			new PossibleVariables("ATTR_area",         Schematics.tech.transistorNode),
			new PossibleVariables("ATTR_area",         Schematics.tech.transistor4Node),
			new PossibleVariables("SIM_spice_model",   Schematics.tech.sourceNode),
			new PossibleVariables("SIM_spice_model",   Schematics.tech.transistorNode),
			new PossibleVariables("SIM_spice_model",   Schematics.tech.transistor4Node),
			new PossibleVariables("SCHEM_meter_type",  Schematics.tech.meterNode),
			new PossibleVariables("SCHEM_diode",       Schematics.tech.diodeNode),
			new PossibleVariables("SCHEM_capacitance", Schematics.tech.capacitorNode),
			new PossibleVariables("SCHEM_resistance",  Schematics.tech.resistorNode),
			new PossibleVariables("SCHEM_inductance",  Schematics.tech.inductorNode),
			new PossibleVariables("SCHEM_function",    Schematics.tech.bboxNode)
		};
	}

	/**
	 * Method to replace node "oldNi" with a new one of type "newNp"
	 * and return the new node.  Also removes any node-specific variables.
	 */
	public static NodeInst replaceNodeInst(NodeInst oldNi, NodeProto newNp, boolean ignorePortNames,
		boolean allowMissingPorts)
	{
		// replace the node
		NodeInst newNi = oldNi.replace(newNp, ignorePortNames, allowMissingPorts);
		if (newNi != null)
		{
			if (newNp instanceof PrimitiveNode)
			{
				// remove variables that make no sense
				for(int i=0; i<PossibleVariables.list.length; i++)
				{
					if (newNi.getProto() == PossibleVariables.list[i].pn) continue;
					Variable var = newNi.getVar(PossibleVariables.list[i].varKey);
					if (var != null)
						newNi.delVar(PossibleVariables.list[i].varKey);
				}
			} else
			{
				// remove parameters that don't exist on the new object
				Cell newCell = (Cell)newNp;
				List varList = new ArrayList();
				for(Iterator it = newNi.getVariables(); it.hasNext(); )
					varList.add(it.next());
				for(Iterator it = varList.iterator(); it.hasNext(); )
				{
					Variable var = (Variable)it.next();
					if (!var.getTextDescriptor().isParam()) continue;

					// see if this parameter exists on the new prototype
					Cell cNp = newCell.contentsView();
					if (cNp == null) cNp = newCell;
					for(Iterator cIt = cNp.getVariables(); it.hasNext(); )
					{
						Variable cVar = (Variable)cIt.next();
						if (var.getKey() != cVar.getKey()) continue;
						if (cVar.getTextDescriptor().isParam())
						{
							newNi.delVar(var.getKey());
							break;
						}
					}
				}
			}

			// now inherit parameters that now do exist
			inheritAttributes(newNi);

			// remove node name if it is not visible
			//Variable var = newNi.getVar(NodeInst.NODE_NAME, String.class);
			//if (var != null && !var.isDisplay())
			//	newNi.delVar(NodeInst.NODE_NAME);
		}
		return newNi;
	}

	/****************************** INHERIT ATTRIBUTES ******************************/

	/**
	 * Method to inherit all prototype attributes down to instance "ni".
	 */
	public static void inheritAttributes(NodeInst ni)
	{
		// ignore primitives
		NodeProto np = ni.getProto();
		if (np instanceof PrimitiveNode) return;
		Cell cell = (Cell)np;

		// first inherit directly from this node's prototype
		for(Iterator it = cell.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.getTextDescriptor().isInherit()) continue;
			inheritCellAttribute(var, ni, cell, null);
		}

		// inherit directly from each port's prototype
		for(Iterator it = cell.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			inheritExportAttributes(pp, ni, cell);
		}

		// if this node is an icon, also inherit from the contents prototype
		Cell cNp = cell.contentsView();
		if (cNp != null)
		{
			// look for an example of the icon in the contents
			NodeInst icon = null;
			for(Iterator it = cNp.getNodes(); it.hasNext(); )
			{
				icon = (NodeInst)it.next();
				if (icon.getProto() == cell) break;
				icon = null;
			}

			for(Iterator it = cNp.getVariables(); it.hasNext(); )
			{
				Variable var = (Variable)it.next();
				if (!var.getTextDescriptor().isInherit()) continue;
				inheritCellAttribute(var, ni, cNp, icon);
			}
			for(Iterator it = cNp.getPorts(); it.hasNext(); )
			{
				Export cpp = (Export)it.next();
				inheritExportAttributes(cpp, ni, cNp);
			}
		}

		// now delete parameters that are not in the prototype
		if (cNp == null) cNp = cell;
		boolean found = true;
		while (found)
		{
			found = false;
			for(Iterator it = ni.getVariables(); it.hasNext(); )
			{
				Variable var = (Variable)it.next();
				if (!var.getTextDescriptor().isParam()) continue;
				Variable oVar = null;
				for(Iterator oIt = cNp.getVariables(); oIt.hasNext(); )
				{
					oVar = (Variable)oIt.next();
					if (!oVar.getTextDescriptor().isParam()) continue;
					if (oVar.getKey() == var.getKey()) break;
					oVar = null;
				}
				if (oVar != null)
				{
					ni.delVar(var.getKey());
					found = true;
					break;
				}
			}
		}
	}

	/**
	 * Method to add all inheritable export variables from export "pp" on cell "np"
	 * to instance "ni".
	 */
	private static void inheritExportAttributes(PortProto pp, NodeInst ni, Cell np)
	{
		for(Iterator it = pp.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.getTextDescriptor().isInherit()) continue;

			Variable.Key attrKey = var.getKey();

			// see if the attribute is already there
			PortInst pi = ni.findPortInstFromProto(pp);
			Variable newVar = pi.getVar(attrKey);
			if (newVar != null) continue;

			// set the attribute
			newVar = pi.newVar(attrKey, inheritAddress(pp, var));
			if (newVar != null)
			{
				double lambda = 1;
				TextDescriptor descript = new TextDescriptor(null);
				var.setDescriptor(descript);
				double dX = descript.getXOff();
				double dY = descript.getYOff();

//				saverot = pp->subnodeinst->rotation;
//				savetrn = pp->subnodeinst->transpose;
//				pp->subnodeinst->rotation = pp->subnodeinst->transpose = 0;
//				portposition(pp->subnodeinst, pp->subportproto, &x, &y);
//				pp->subnodeinst->rotation = saverot;
//				pp->subnodeinst->transpose = savetrn;
//				x += dX;   y += dY;
//				makerot(pp->subnodeinst, trans);
//				xform(x, y, &x, &y, trans);
//				maketrans(ni, trans);
//				xform(x, y, &x, &y, trans);
//				makerot(ni, trans);
//				xform(x, y, &x, &y, trans);
//				x = x - (ni->lowx + ni->highx) / 2;
//				y = y - (ni->lowy + ni->highy) / 2;
//				switch (TDGETPOS(descript))
//				{
//					case VTPOSCENT:      style = TEXTCENT;      break;
//					case VTPOSBOXED:     style = TEXTBOX;       break;
//					case VTPOSUP:        style = TEXTBOT;       break;
//					case VTPOSDOWN:      style = TEXTTOP;       break;
//					case VTPOSLEFT:      style = TEXTRIGHT;     break;
//					case VTPOSRIGHT:     style = TEXTLEFT;      break;
//					case VTPOSUPLEFT:    style = TEXTBOTRIGHT;  break;
//					case VTPOSUPRIGHT:   style = TEXTBOTLEFT;   break;
//					case VTPOSDOWNLEFT:  style = TEXTTOPRIGHT;  break;
//					case VTPOSDOWNRIGHT: style = TEXTTOPLEFT;   break;
//				}
//				makerot(pp->subnodeinst, trans);
//				style = rotatelabel(style, TDGETROTATION(descript), trans);
//				switch (style)
//				{
//					case TEXTCENT:     TDSETPOS(descript, VTPOSCENT);      break;
//					case TEXTBOX:      TDSETPOS(descript, VTPOSBOXED);     break;
//					case TEXTBOT:      TDSETPOS(descript, VTPOSUP);        break;
//					case TEXTTOP:      TDSETPOS(descript, VTPOSDOWN);      break;
//					case TEXTRIGHT:    TDSETPOS(descript, VTPOSLEFT);      break;
//					case TEXTLEFT:     TDSETPOS(descript, VTPOSRIGHT);     break;
//					case TEXTBOTRIGHT: TDSETPOS(descript, VTPOSUPLEFT);    break;
//					case TEXTBOTLEFT:  TDSETPOS(descript, VTPOSUPRIGHT);   break;
//					case TEXTTOPRIGHT: TDSETPOS(descript, VTPOSDOWNLEFT);  break;
//					case TEXTTOPLEFT:  TDSETPOS(descript, VTPOSDOWNRIGHT); break;
//				}
//				x = x * 4 / lambda;
//				y = y * 4 / lambda;
//				TDSETOFF(descript, x, y);
//				TDSETINHERIT(descript, 0);
//				TDCOPY(newVar->textdescript, descript);
			}
		}
	}

	/*
	 * Method to add inheritable variable "var" from cell "np" to instance "ni".
	 * If "icon" is not NONODEINST, use the position of the variable from it.
	 */
	private static void inheritCellAttribute(Variable var, NodeInst ni, Cell np, NodeInst icon)
	{
		// see if the attribute is already there
		Variable.Key key = var.getKey();
		Variable newVar = ni.getVar(key.getName());
		if (newVar != null)
		{
			// make sure visibility is OK
			if (!var.getTextDescriptor().isInterior())
			{
				// parameter should be visible: make it so
				if (!newVar.isDisplay())
				{
					newVar.setDisplay();
				}
			} else
			{
				// parameter not normally visible: make it invisible if it has the default value
				if (newVar.isDisplay())
				{
					if (var.describe(-1, -1).equals(newVar.describe(-1, -1)))
					{
						newVar.clearDisplay();
					}
				}
			}
			return;
		}

		// determine offset of the attribute on the instance
		Variable posVar = var;
		if (icon != null)
		{
			for(Iterator it = icon.getVariables(); it.hasNext(); )
			{
				Variable ivar = (Variable)it.next();
				if (ivar.getKey() == var.getKey())
				{
					posVar = ivar;
					break;
				}
			}
		}

		double xc = posVar.getTextDescriptor().getXOff();
		if (posVar == var) xc -= np.getBounds().getCenterX();
		double yc = posVar.getTextDescriptor().getYOff();
		if (posVar == var) yc -= np.getBounds().getCenterY();

		// set the attribute
		newVar = ni.newVar(var.getKey(), inheritAddress(np, posVar));
		if (newVar != null)
		{
			if (var.isDisplay()) newVar.setDisplay(); else newVar.clearDisplay();
			if (var.getTextDescriptor().isInterior()) newVar.clearDisplay();
			TextDescriptor newDescript = TextDescriptor.newNodeArcDescriptor(null);
			newDescript.clearInherit();
			newDescript.setOff(xc, yc);
			if (var.getTextDescriptor().isParam())
			{
				newDescript.clearInterior();
				TextDescriptor.DispPos i = newDescript.getDispPart();
				if (i == TextDescriptor.DispPos.NAMEVALINH || i == TextDescriptor.DispPos.NAMEVALINHALL)
					newDescript.setDispPart(TextDescriptor.DispPos.NAMEVALUE);
			}
			newVar.setDescriptor(newDescript);
		}
	}

	/**
	 * Helper method to determine the proper value of an inherited Variable.
	 * Normally, it is simply "var.getObject()", but if it is a string with the "++" or "--"
	 * sequence in it, then it indicates an auto-increments/decrements of that numeric value.
	 * The returned object has the "++"/"--" removed, and the original variable is modified.
	 * @param addr the ElectricObject on which this Variable resides.
	 * @param var the Variable being examined.
	 * @return the Object in the Variable.
	 */
	private static Object inheritAddress(ElectricObject addr, Variable var)
	{
		// if it isn't a string, just return its address
		Object obj = var.getObject();
		if (obj instanceof Object[]) return obj;
		if (!var.isCode() && !(obj instanceof String)) return obj;

		String str = (String)obj;
		int plusPlusPos = str.indexOf("++");
		int minusMinusPos = str.indexOf("--");
		if (plusPlusPos < 0 && minusMinusPos < 0) return obj;

		// construct the proper inherited string and increment the variable
		int incrPoint = Math.max(plusPlusPos, minusMinusPos);
		String retVal = str.substring(0, incrPoint) + str.substring(incrPoint+2);

		// increment the variable
		int i;
		for(i = incrPoint-1; i>0; i--)
			if (!Character.isDigit(str.charAt(i))) break;
		i++;
		int curVal = TextUtils.atoi(str.substring(i));
		if (str.charAt(incrPoint) == '+') curVal++; else curVal--;
		String newIncrString = str.substring(0, i) + curVal + str.substring(incrPoint+2);
		addr.newVar(var.getKey(), newIncrString);

		return retVal;
	}

	/****************************** DETERMINE ABILITY TO MAKE CHANGES ******************************/

	/**
	 * Method to tell whether a NodeInst can be modified in a cell.
	 * @param cell the Cell in which the NodeInst resides.
	 * @param item the NodeInst (may be null).
	 * @param giveError true to print an error message if the modification is disallowed.
	 * @return true if the edit CANNOT be done.
	 */
	public static boolean cantEdit(Cell cell, NodeInst item, boolean giveError)
	{
		// if an instance is specified, check it
		if (item != null)
		{
			if (item.isLocked())
			{
				if (!giveError) return true;
				String [] options = {"Yes", "No", "Always"};
				int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
					"Changes to locked node " + item.describe() + " are disallowed.  Change anyway?",
					"Allow changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
					null, options, options[1]);
				if (ret == 1) return true;
				if (ret == 2) item.clearLocked();
			}
			if (item.getProto() instanceof PrimitiveNode)
			{
				// see if a primitive is locked
				if (item.getProto().isLockedPrim() &&
					User.isDisallowModificationLockedPrims())
				{
					if (!giveError) return true;
					String [] options = {"Yes", "No", "Always"};
					int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
						"Changes to locked primitives (such as " + item.describe() + ") are disallowed.  Change anyway?",
						"Allow changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
						null, options, options[1]);
					if (ret == 1) return true;
					if (ret == 2) User.setDisallowModificationLockedPrims(false);
				}
			} else
			{
				// see if this type of cell is locked
				if (cell.isInstancesLocked())
				{
					if (!giveError) return true;
					String [] options = {"Yes", "No", "Always"};
					int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
						"Instances in cell " + cell.describe() + " are locked.  You cannot move " + item.describe() +
						".  Change anyway?",
						"Allow changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
						null, options, options[1]);
					if (ret == 1) return true;
					if (ret == 2) cell.clearInstancesLocked();
				}
			}
		}

		// check for general changes to the cell
		if (cell.isAllLocked())
		{
			if (!giveError) return true;
			String [] options = {"Yes", "No", "Always"};
			int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
				"Changes to cell " + cell.describe() + " are locked.  Change anyway?",
				"Allow changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
				null, options, options[1]);
			if (ret == 1) return true;
			if (ret == 2) cell.clearAllLocked();
		}
		return false;
	}

}
