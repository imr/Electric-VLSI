/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Undo.java
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
package com.sun.electric.database.change;

import com.sun.electric.database.CellUsage;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableCell;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableLibrary;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.constraint.Layout;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.ServerJobManager;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * This interface defines changes that are made to the database.
 */
public class Undo
{

	/**
	 * Type is a typesafe enum class that describes the nature of a change.
	 */
    public enum Type
    {
		/** Describes a newly-created NodeInst. */							NODEINSTNEW,
		/** Describes a deleted NodeInst. */								NODEINSTKILL,
		/** Describes a changed NodeInst. */								NODEINSTMOD,
		/** Describes a newly-created ArcInst. */							ARCINSTNEW,
		/** Describes a deleted ArcInst. */									ARCINSTKILL,
		/** Describes a changed ArcInst. */									ARCINSTMOD,
		/** Describes a newly-created Export. */							EXPORTNEW,
		/** Describes a deleted Export. */									EXPORTKILL,
		/** Describes a changed Export. */									EXPORTMOD,
		/** Describes a newly-created Cell. */								CELLNEW,
		/** Describes a deleted Cell. */									CELLKILL,
        /** Describes a changed Cell. */                                    CELLMOD,
		/** Describes the renaming of an arbitrary object. */				OBJECTRENAME,
		/** Describes a new library change */								LIBRARYNEW,
		/** Describes a delete library change */							LIBRARYKILL,
		/** Describes a changed Library. */                                 LIBRARYMOD,
		/** Describes a Cell-group change */								CELLGROUPMOD;
    }

	/**
	 * The Change class describes a single change to the Electric database.
	 * These objects are used to undo and redo changes.
	 */
	public static class Change
	{
		private ElectricObject obj;
		private Type type;
		private Object o1;

		Change(ElectricObject obj, Type type)
		{
			this.obj = obj;
			this.type = type;
		}

		/**
		 * Method to get the ElectricObject associated with this Change.
		 * @return the ElectricObject associated with this Change.
		 */
		public ElectricObject getObject() { return obj; }
		/**
		 * Method to get the type of this Change.
		 * @return the type of this Change.
		 */
		public Type getType() { return type; }
		/**
		 * Method to get the first Object associated with this Change.
		 * @return the first Object associated with this Change.
		 */
		public Object getO1() { return o1; }

		/**
		 * Method to broadcast a change to all tools that are on.
		 * @param undoRedo true if this is an undo/redo batch.
		 */
		private void broadcast(boolean undoRedo)
		{
			// start the batch if this is the first change
			broadcasting = type;
			if (type == Type.NODEINSTNEW || type == Type.ARCINSTNEW || type == Type.EXPORTNEW ||
				type == Type.CELLNEW/* || type == Type.LIBRARYNEW*/)
			{
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.newObject(obj);
				}
			} else if (type == Type.NODEINSTKILL || type == Type.ARCINSTKILL || type == Type.EXPORTKILL ||
				type == Type.CELLKILL/* || type == Type.LIBRARYKILL*/)
			{
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.killObject(obj);
				}
			} else if (type == Type.OBJECTRENAME) {
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.renameObject(obj, o1);
				}
			} else if (type == Type.NODEINSTMOD)
			{
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyNodeInst((NodeInst)obj, (ImmutableNodeInst)o1);
				}
			} else if (type == Type.ARCINSTMOD)
			{
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyArcInst((ArcInst)obj, (ImmutableArcInst)o1);
				}
			} else if (type == Type.EXPORTMOD)
			{
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyExport((Export)obj, (ImmutableExport)o1);
				}
			} else if (type == Type.CELLMOD)
			{
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyCell((Cell)obj, (ImmutableCell)o1);
				}
			} else if (type == Type.LIBRARYMOD)
			{
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyLibrary((Library)obj, (ImmutableLibrary)o1);
				}
			} else if (type == Type.CELLGROUPMOD)
			{
				for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyCellGroup((Cell)obj, (Cell.CellGroup)o1);
				}
            }
			broadcasting = null;
		}

		/**
		 * Method to undo the effects of this change.
		 */
		private void reverse(boolean backwards)
		{
			// determine what needs to be marked as changed
//			setDirty(type, obj, o1);

			if (type == Type.NODEINSTNEW)
			{
				NodeInst ni = (NodeInst)obj;
				ni.lowLevelUnlink();
				type = Type.NODEINSTKILL;
				return;
			}
			if (type == Type.NODEINSTKILL)
			{
				NodeInst ni = (NodeInst)obj;
				ni.lowLevelLink();
				type = Type.NODEINSTNEW;
				return;
			}
			if (type == Type.NODEINSTMOD)
			{
				// get information about the node as it is now
				NodeInst ni = (NodeInst)obj;
                ImmutableNodeInst oldD = ni.getD();

				// change the node information
				ni.lowLevelModify((ImmutableNodeInst)o1);

				// update the change to its reversed state
				o1 = oldD;
				return;
			}
			if (type == Type.ARCINSTNEW)
			{
				ArcInst ai = (ArcInst)obj;
				ai.lowLevelUnlink();
				type = Type.ARCINSTKILL;
				return;
			}
			if (type == Type.ARCINSTKILL)
			{
				ArcInst ai = (ArcInst)obj;
				ai.lowLevelLink();
				type = Type.ARCINSTNEW;
				if (Job.getDebug()) ai.getParent().checkInvariants();
				return;
			}
			if (type == Type.ARCINSTMOD)
			{
				// get information about the arc as it is now
				ArcInst ai = (ArcInst)obj;
                ImmutableArcInst oldD = ai.getD();

				// change the arc information
				ai.lowLevelModify((ImmutableArcInst)o1);

				// update the change to its reversed state
                o1 = oldD;
				return;
			}
			if (type == Type.EXPORTNEW)
			{
				Export pp = (Export)obj;
				pp.lowLevelUnlink();
				type = Type.EXPORTKILL;
				return;
			}
			if (type == Type.EXPORTKILL)
			{
				Export pp = (Export)obj;
				pp.lowLevelLink();
				type = Type.EXPORTNEW;
				if (Job.getDebug()) ((Cell)pp.getParent()).checkInvariants();
				return;
			}
			if (type == Type.EXPORTMOD)
			{
				Export pp = (Export)obj;
                ImmutableExport oldD = pp.getD();
				pp.lowLevelModify((ImmutableExport)o1);
				o1 = oldD;
				return;
			}
			if (type == Type.CELLNEW)
			{
				Cell cell = (Cell)obj;
				cell.lowLevelUnlink();
				type = Type.CELLKILL;
				return;
			}
			if (type == Type.CELLKILL)
			{
				Cell cell = (Cell)obj;
				cell.lowLevelLink_();
				type = Type.CELLNEW;
				return;
			}
			if (type == Type.CELLMOD)
			{
				Cell cell = (Cell)obj;
                ImmutableCell oldD = cell.getD();
                cell.lowLevelModify((ImmutableCell)o1);
                o1 = oldD;
				return;
			}
			if (type == Type.LIBRARYMOD)
			{
                Library lib = (Library)obj;
				ImmutableLibrary oldD = lib.getD();
                lib.lowLevelModify(oldD);
                o1 = oldD;
				return;
			}
			if (type == Type.CELLGROUPMOD)
			{
				Cell cell = (Cell)obj;
				Cell.CellGroup oldGroup = (Cell.CellGroup)o1;
				o1 = cell.getCellGroup();
				cell.lowLevelSetCellGroup(oldGroup);
				return;
			}
			if (type == Type.OBJECTRENAME)
			{
                Library lib = (Library)obj;
                String oldName = (String)o1;
                o1 = lib.getName();
                lib.lowLevelRename(oldName);
				return;
			}
		}

		private static boolean isMajorVariable(ElectricObject obj, Variable.Key key)
		{
// 			if ((obj instanceof Cell) && key == el_cell_message_key) return true;
// 			if ((obj instanceof NodeInst) && key == sim_weaknodekey) return true;
			return false;
		}

		/**
		 * Returns a printable version of this Change.
		 * @return a printable version of this Change.
		 */
		public String toString() { return describe(); }

		/**
		 * Method to describe this change as a string.
		 */
		private String describe()
		{
			if (type == Type.NODEINSTNEW)
			{
				NodeInst ni = (NodeInst)obj;
				return ni + " created in " + ni.getParent();
			}
			if (type == Type.NODEINSTKILL)
			{
				NodeInst ni = (NodeInst)obj;
				return ni + " deleted from " + ni.getParent();
			}
			if (type == Type.NODEINSTMOD)
			{
				NodeInst ni = (NodeInst)obj;
                ImmutableNodeInst d = (ImmutableNodeInst)getO1();
				return ni + " modified in " + ni.getParent() +
					"[was " + d.width + "x" + d.height + " at (" + d.anchor.getX() + "," + d.anchor.getY() + ") rotated " + d.orient + ", is " +
					ni.getD().width + "x" + ni.getD().height + " at (" + ni.getAnchorCenterX() + "," +
					ni.getAnchorCenterY() + ") rotated " + ni.getOrient() + "]";
			}
			if (type == Type.ARCINSTNEW)
			{
				ArcInst ai = (ArcInst)obj;
				return ai + " created in " + ai.getParent();
			}
			if (type == Type.ARCINSTKILL)
			{
				ArcInst ai = (ArcInst)obj;
				return ai + " deleted from " + ai.getParent();
			}
			if (type == Type.ARCINSTMOD)
			{
				ArcInst ai = (ArcInst)obj;
                ImmutableArcInst d = (ImmutableArcInst)getO1();
				return ai + " modified in " + ai.getParent() +
					"[was " + d.width + " wide from (" + d.headLocation.getX() + "," + d.headLocation.getY() + ") to (" + d.tailLocation.getX() + "," + d.tailLocation.getY() + ")]" +
                        ", is " + ai.getWidth() + " wide from (" + ai.getHeadLocation().getX() + "," + ai.getHeadLocation().getY() + ") to (" + ai.getTailLocation().getX() + "," + ai.getTailLocation().getY() + ")";
			}
			if (type == Type.EXPORTNEW)
			{
				Export pp = (Export)obj;
				return "Export " + pp.getName() + " created in " + pp.getParent();
			}
			if (type == Type.EXPORTKILL)
			{
				Export pp = (Export)obj;
				return "Export " + pp.getName() + " deleted from " + pp.getParent();
			}
			if (type == Type.EXPORTMOD)
			{
				Export pp = (Export)obj;
				ImmutableExport d = (ImmutableExport)o1;
                PortInst pi = ((Cell)pp.getParent()).getPortInst(d.originalNodeId, d.originalPortId);
				return "Export " + pp.getName() + " moved in " + pp.getParent() +
					"[was on " + pi.getNodeInst() + " port " + pi.getPortProto().getName() + "]";
			}
			if (type == Type.CELLNEW)
			{
				Cell cell = (Cell)obj;
				return cell + " created";
			}
			if (type == Type.CELLKILL)
			{
				Cell cell = (Cell)obj;
				return cell + " deleted";
			}
			if (type == Type.CELLMOD)
			{
				Cell cell = (Cell)obj;
				return cell + " modified";
			}
			if (type == Type.CELLGROUPMOD)
			{
				Cell cell = (Cell)obj;
//				Cell.CellGroup group = (Cell.CellGroup)o1;
				return cell + " moved to group";
			}
			if (type == Type.LIBRARYMOD)
			{
				return "Changed library "+obj;
			}
			if (type == Type.OBJECTRENAME)
			{
				return "Renamed object " + obj + " (was " + o1 + ")";
			}
			if (type == Type.LIBRARYNEW)
			{
				return "Created "+obj;
			}
			if (type == Type.LIBRARYKILL)
			{
				return "Deleted "+obj;
			}
			return "UNKNOWN CHANGE, type=" + type;
		}
	}

	/**
	 * Class to describe a batch of changes to the Electric database.
	 */
	public static class ChangeBatch
	{
        private Snapshot oldSnapshot, newSnapshot;
        
		private ArrayList<Change> changes;
		private int batchNumber;
//		private boolean done;
		private Tool tool;
		private String activity;
//		private Cell upCell;
		private int startingHighlights = -1;				// highlights before changes made
		private int preUndoHighlights = -1;				// highlights before undo of changes done

		private ChangeBatch(Snapshot oldSnapshot) { this.oldSnapshot = oldSnapshot; }
		
		private void add(Change change) { changes.add(change); }

		/**
		 * Method to return an iterator over all changes in this ChangeBatch.
		 * @return an iterator over all changes in this ChangeBatch.
		 */
		public Iterator<Change> getChanges() { return changes.iterator(); }

		/**
		 * Method to return the number of this ChangeBatch.
		 * Each batch has a unique number, and their order increases with time.
		 * @return the unique number of this ChangeBatch.
		 */
		public int getBatchNumber() { return batchNumber; }

        public int getStartingHighlights() { return startingHighlights; }
        public int getPreUndoHighlights() { return preUndoHighlights; }
        
		/**
		 * Method to return the Tool associated with this ChangeBatch.
		 * @return the Tool associated with this ChangeBatch.
		 */
		public Tool getTool() { return tool; }

		/**
		 * Method to return the number of changes in this ChangeBatch.
		 * @return the number of changes in this ChangeBatch.
		 */
		public int getNumChanges() { return changes.size(); }

        public void reverse(boolean backwards) {
            Library.checkFresh(newSnapshot);
            if (changes.isEmpty()) {
                assert newSnapshot == oldSnapshot;
            } else {
                broadcastStart(true);
                
                Snapshot tmpSnapshot = newSnapshot;
                newSnapshot = oldSnapshot;
                oldSnapshot = tmpSnapshot;
              
                Library.undo(newSnapshot);
//                Collections.reverse(changes);
//                for(Change ch: changes) {
//                    // reverse the change
//                    ch.reverse(backwards);
//                    // now broadcast this change
//                    ch.broadcast(true);
//                }
            }
            
    		// broadcast the end-batch
            refreshCellBounds();
            for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); ) {
                Listener listener = it.next();
                listener.endBatch(oldSnapshot, newSnapshot, true);
            }
            Library.checkFresh(newSnapshot);
        }
        
		private void describe(String title)
		{
			// display the change batches
			int nodeInst = 0, arcInst = 0, export = 0, cell = 0, library = 0;
			int batchSize = changes.size();
			for(int j = 0; j<batchSize; j++)
			{
				Change ch = (Change)changes.get(j);
				if (ch.getType() == Type.NODEINSTNEW || ch.getType() == Type.NODEINSTKILL || ch.getType() == Type.NODEINSTMOD)
				{
					nodeInst++;
				} else if (ch.getType() == Type.ARCINSTNEW || ch.getType() == Type.ARCINSTKILL || ch.getType() == Type.ARCINSTMOD)
				{
					arcInst++;
				} else if (ch.getType() == Type.EXPORTNEW || ch.getType() == Type.EXPORTKILL || ch.getType() == Type.EXPORTMOD)
				{
					export++;
				} else if (ch.getType() == Type.CELLNEW || ch.getType() == Type.CELLKILL || ch.getType() == Type.CELLMOD || ch.getType() == Type.CELLGROUPMOD)
				{
					cell++;
				} else if (ch.getType() == Type.LIBRARYMOD)
				{
					library++;
                }
			}

			String message = "*** Batch '" + title + "', " + batchNumber + " (" + activity + " from " + tool.getName() + " tool) has " + batchSize + " changes and affects";
			if (nodeInst != 0) message += " " + nodeInst + " nodes";
			if (arcInst != 0) message += " " + arcInst + " arcs";
			if (export != 0) message += " " + export + " exports";
			if (cell != 0) message += " " + cell + " cells";
			if (library != 0) message += " " + library + " libraries";
			System.out.println(message + ":");
			for(int j = 0; j<batchSize; j++)
			{
				Change ch = (Change)changes.get(j);
				System.out.println("   " + ch.describe());
			}
		}
	}

	private static Type broadcasting = null;
	private static boolean doChangesQuietly = false;
	private static ChangeBatch currentBatch = null;
	private static int maximumBatches = User.getMaxUndoHistory();
	private static int overallBatchNumber = 0;
	private static List<ChangeBatch> doneList = new ArrayList<ChangeBatch>();
	private static List<ChangeBatch> undoneList = new ArrayList<ChangeBatch>();
//	private static HashMap<Cell,Boolean> changedCells = new HashMap<Cell,Boolean>();

	/**
	 * Method to start a new batch of changes.
	 * @param tool the tool that is producing the activity.
	 * @param activity a String describing the activity.
	 * @param cell root of up-tree or null for whole database lock
	 */
	public static void startChanges(Snapshot oldSnapshot, Tool tool, String activity, int startingHighlights)
	{
		// close any open batch of changes
		clearChanges();

		// kill off any undone batches
		noRedoAllowed();

		// erase the list of changed cells
//		changedCells.clear();

		// allocate a new change batch
		currentBatch = new ChangeBatch(oldSnapshot);
		currentBatch.changes = new ArrayList<Change>();
		currentBatch.batchNumber = ++overallBatchNumber;
//		currentBatch.done = true;
		currentBatch.tool = tool;
		currentBatch.activity = activity;
//		currentBatch.upCell = cell;
		currentBatch.startingHighlights = startingHighlights;

		// put at head of list
		doneList.add(currentBatch);

		// kill last batch if list is full
		if (doneList.size() > maximumBatches)
		{
			doneList.remove(0);
		}
        updateUndoRedo();

		// start the batch of changes
		Constraints.getCurrent().startBatch(oldSnapshot);

		for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
		{
			Listener listener = (Listener)it.next();
			listener.startBatch(tool, false);
		}
	}

    private static void clearChanges() {
        endChanges();
        if (currentBatch == null) return;
        System.out.println("Ignored a batch " + currentBatch.activity + " of " + currentBatch.getNumChanges() + " changes");
        currentBatch = null;
    }
    
    /**
	 * Method to terminate the current batch of changes.
	 */
	public static void endChanges()
	{
		// if no changes were recorded, stop
		if (currentBatch == null) return;

		// if no changes were recorded, stop
		if (currentBatch.changes.size() == 0)
		{
			// remove from doneList
			doneList.remove(currentBatch);
            updateUndoRedo();
		}

		// changes made: apply final constraints to this batch of changes
        String userName = System.getProperty("user.name"); 
        currentBatch.newSnapshot = Constraints.getCurrent().endBatch(userName);
        for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); ) {
            Listener listener = it.next();
            listener.endBatch(currentBatch.oldSnapshot, currentBatch.newSnapshot, false);
        }
        Library.checkFresh(currentBatch.newSnapshot);
		currentBatch = null;
	}

    private static synchronized void updateUndoRedo() {
        ServerJobManager.setUndoRedoStatus(!doneList.isEmpty(), !undoneList.isEmpty());
    }

    private static void broadcastStart(boolean undoRedo) {
        // broadcast a start-batch on the first change
        for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); ) {
            Listener listener = (Listener)it.next();
            listener.startBatch(listener, undoRedo);
        }
    }
    
	/**
	 * Method to record and broadcast a change.
	 * <P>
	 * These types of changes exist:
	 * <UL>
	 * <LI>NODEINSTNEW takes nothing.
	 * <LI>NODEINSTKILL takes nothing.
	 * <LI>NODEINSTMOD takes o1=oldD.
	 * <LI>ARCINSTNEW takes nothing.
	 * <LI>ARCINSTKILL takes nothing.
	 * <LI>ARCINSTMOD takes o1=oldD.
	 * <LI>EXPORTNEW takes nothing.
	 * <LI>EXPORTKILL takes nothing.
	 * <LI>EXPORTMOD takes o1=oldD.
	 * <LI>CELLNEW takes nothing.
	 * <LI>CELLKILL takes nothing.
	 * <LI>CELLMOD takes o1=oldD.
	 * <LI>OBJECTRENAME takes o1=oldName.
	 * <LI>CELLGROUPMOD takes o1=oldCellGroup
	 * <LI>LIBRARYMOD takes o1=oldD.
	 * </UL>
	 * @param obj the object to which the change applies.
	 * @param change the change being recorded.
	 * @return the change object (null on error).
	 */
	private static Change newChange(ElectricObject obj, Type change, Object o1)
	{
        Job.checkChanging();
		if (currentBatch == null)
		{
			System.out.println("Received " + change + " change when no batch started");
			return null;
		}
		if (broadcasting != null)
		{
			System.out.println("Received " + change + " change during broadcast of " + broadcasting);
			return null;
		}

		// determine what needs to be marked as changed
//        Change.setDirty(change, obj, o1);

		// see if this is the first change
//		boolean firstChange = false;
//		if (currentBatch.getNumChanges() == 0) firstChange = true;

		// get change module
		Change ch = new Change(obj, change);
		ch.o1 = o1;

		// insert new change module into linked list
		currentBatch.add(ch);

		// broadcast the change
		//ch.broadcast(firstChange, false);
		return ch;
	}

	/**
	 * Method to store a change to a NodeInst in the change-control system.
	 * @param ni the NodeInst that changed.
	 * @param oD the old contents of the NodeInst.
	 */
	public static void modifyNodeInst(NodeInst ni, ImmutableNodeInst oD)
	{
		if (!recordChange()) return;
		Change ch = newChange(ni, Type.NODEINSTMOD, oD);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
        Constraints.getCurrent().modifyNodeInst(ni, oD);
	}

	/**
	 * Method to store a change to an ArcInst in the change-control system.
	 * @param ai the ArcInst that changed.
     * @param oD the old contents of the ArcInst.
	 */
	public static void modifyArcInst(ArcInst ai, ImmutableArcInst oD)
	{
		if (!recordChange()) return;
		Change ch = newChange(ai, Type.ARCINSTMOD, oD);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
        Constraints.getCurrent().modifyArcInst(ai, oD);
	}

	/**
	 * Method to store a change to an Export in the change-control system.
	 * Export changes involve moving them from one PortInst to another in the Cell.
	 * @param pp the Export that was moved.
	 * @param oD the old contents of the Export.
	 */
	public static void modifyExport(Export pp, ImmutableExport oD)
	{
		if (!recordChange()) return;
		Change ch = newChange(pp, Type.EXPORTMOD, oD);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
		Constraints.getCurrent().modifyExport(pp, oD);
	}

	/**
	 * Method to store a change to an Cell in the change-control system.
	 * @param pp the Cell that was moved.
	 * @param oD the old contents of the Cell.
	 */
	public static void modifyCell(Cell cell, ImmutableCell oD)
	{
		if (!recordChange()) return;
		Change ch = newChange(cell, Type.CELLMOD, oD);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
		Constraints.getCurrent().modifyCell(cell, oD);
	}

	/**
	 * Method to store a change to a Cell's CellGroup in the change-control system.
	 * @param cell the Cell whose CellGroup is changing
	 * @param oldGroup the old CellGroup
	 */
	public static void modifyCellGroup(Cell cell, Cell.CellGroup oldGroup)
	{
		if (!recordChange()) return;
		Change ch = newChange(cell, Type.CELLGROUPMOD, oldGroup);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
	}

    /*
	 * Method to store the change to a Linrary
	 * @param lib the Library that was changed.
	 * @param oldD the old contents of the Library.
	 */
	public static void modifyLibrary(Library lib, ImmutableLibrary oldD)
	{
		if (!recordChange()) return;
		Type type = Type.LIBRARYMOD;
		Change ch = newChange(lib, type, oldD);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
		Constraints.getCurrent().modifyLibrary(lib, oldD);
	}

	/**
	 * Method to store the creation of a new ElectricObject in the change-control system.
	 * @param obj the ElectricObject that was created.
	 */
	public static void newObject(ElectricObject obj)
	{
//		// always broadcast library changes
//		if (!recordChange() && !(obj instanceof Library)) return;
		if (!recordChange()) return;
		Cell cell = obj.whichCell();
		Type type = null;
		if (obj instanceof Cell) type = Type.CELLNEW;
		else if (obj instanceof NodeInst) type = Type.NODEINSTNEW;
		else if (obj instanceof ArcInst) type = Type.ARCINSTNEW;
		else if (obj instanceof Export) type = Type.EXPORTNEW;
		else if (obj instanceof Library) type = Type.LIBRARYNEW;
		Change ch = newChange(obj, type, null);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
		Constraints.getCurrent().newObject(obj);
	}

	/**
	 * Method to store the deletion of an ElectricObject in the change-control system.
	 * @param obj the ElectricObject that was deleted.
	 * Note: because a reference to the object is kept in the change-control system,
	 * the object is not really deallocated until the change-control system no longer
	 * remembers the change, and it can no longer be undone.
	 */
	public static void killObject(ElectricObject obj)
	{
//		// always broadcast library changes
//		if (!recordChange() && !(obj instanceof Library)) return;
		if (!recordChange()) return;
		Type type = null;
		if (obj instanceof Cell) type = Type.CELLKILL;
		else if (obj instanceof NodeInst) type = Type.NODEINSTKILL;
		else if (obj instanceof ArcInst) type = Type.ARCINSTKILL;
        else if (obj instanceof Export) type = Type.EXPORTKILL;
		else if (obj instanceof Library) type = Type.LIBRARYKILL;
		Change ch = newChange(obj, type, null);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
		Constraints.getCurrent().killObject(obj);
	}

	/**
	 * Method to store the renaming of an ElectricObject in the change-control system.
	 * @param obj the ElectricObject that was renamed.
	 * @param oldName the former name of the ElectricObject.
	 */
	public static void renameObject(ElectricObject obj, Object oldName)
	{
		if (!recordChange()) return;
		Type type = Type.OBJECTRENAME;
		Change ch = newChange(obj, type, oldName);
		if (ch == null) return;

        if (currentBatch.getNumChanges() <= 1)
            broadcastStart(false);
		ch.broadcast(false);
		Constraints.getCurrent().renameObject(obj, oldName);
	}

    /**
	 * Method to return the current change batch.
	 * @return the current change batch (null if no changes are being done).
	 */
	public static ChangeBatch getCurrentBatch() { return currentBatch; }

	/**
	 * Method to tell whether changes are being made quietly.
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 * @return true if changes are being made quietly.
	 */
	public static boolean isChangeQuiet() { return doChangesQuietly; }

	/**
	 * Method to set the subsequent changes to be "quiet".
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 * @return the previous value of the "quiet" state.
	 */
	public static boolean changesQuiet(boolean quiet) {
		Library.checkInvariants();
        Layout.changesQuiet(quiet);
		NetworkTool.changesQuiet(quiet);
		boolean formerQuiet = doChangesQuietly;
        doChangesQuietly = quiet;
		return formerQuiet;
    }

	/**
	 * Method to tell whether changes are currently "quiet".
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 * By calling this method, the "next change quiet" state is reset.
	 * @return true if changes are "quiet".
	 */
	private static boolean recordChange()
	{
		if (currentBatch == null) return false;
		return !doChangesQuietly;
	}

	/**
	 * Method to undo the last batch of changes.
	 * @return the batch number that was undone.
	 * Returns null if nothing was undone.
	 */
	public static ChangeBatch undoABatch(int savedHighlights)
	{
		// close out the current batch
		clearChanges();

		// get the most recent batch of changes
		int listSize = doneList.size();
		if (listSize == 0) return null;
		ChangeBatch batch = (ChangeBatch)doneList.get(listSize-1);
		doneList.remove(listSize-1);
		undoneList.add(batch);
        updateUndoRedo();

		// save pre undo highlights
		batch.preUndoHighlights = savedHighlights;

		// look through the changes in this batch
        batch.reverse(true);

		// Put message in Message Window
		System.out.println("Undoing: " + batch.activity);
//        batch.describe("Undo");
		return batch;
	}

	/**
	 * Method to redo the last batch of changes.
	 * @return true if a batch was redone.
	 */
	public static ChangeBatch redoABatch()
	{
		// close out the current batch
		clearChanges();

		// get the most recent batch of changes
		if (undoneList == null) return null;
		int listSize = undoneList.size();
		if (listSize == 0) return null;
		ChangeBatch batch = (ChangeBatch)undoneList.get(listSize-1);
		undoneList.remove(listSize-1);
		doneList.add(batch);
        updateUndoRedo();

        batch.reverse(false);
        
        // Put message in Message Window
		System.out.println("Redoing: " + batch.activity);
//        batch.describe("Redo");
		return batch;
	}

    /** 
     * Refresh Cell bounds. This method is necessary for Undo/Redu batches,
     * because constraint system is disabled.
     */
    private static void refreshCellBounds() {
        for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
            Library lib = (Library)it.next();
            for (Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); ) {
                Cell cell = (Cell)cIt.next();
                cell.getBounds();
            }
        }
    }
    
    /**
	 * Returns root of up-tree of undo or redo batch.
	 * @param redo true if redo batch, false if undo batch
	 * @return root of up-tree of a batch.
	 */
	public static Cell upCell(boolean redo)
	{
        return null;
//		ChangeBatch batch = null;
//		if (redo)
//		{
//			int listSize = undoneList.size();
//			if (listSize > 0)
//				batch = (ChangeBatch)undoneList.get(listSize-1);
//		} else
//		{
//			int listSize = doneList.size();
//			if (listSize != 0)
//				batch = (ChangeBatch)doneList.get(listSize-1);
//		}
//		return batch != null ? batch.upCell : null;
	}

	/**
	 * Method to prevent undo by deleting all change batches.
	 */
	public static void noUndoAllowed()
	{
 		// properly terminate the current batch
		clearChanges();

 		// kill them all
 		doneList.clear();
 		undoneList.clear();
        updateUndoRedo();
	}

	/**
	 * Method to prevent redo by deleting all undone change batches.
	 */
	public static void noRedoAllowed()
	{
		// properly terminate the current batch
		clearChanges();

		undoneList.clear();
        updateUndoRedo();
	}

	/**
	 * Method to set the size of the history list and return the former size.
	 * @param newSize the new size of the history list (number of batches of changes).
	 * If not positive, the list size is not changed.
	 * @return the former size of the history list.
	 */
	public static int setHistoryListSize(int newSize)
	{
		if (newSize <= 0) return maximumBatches;

		int oldSize = maximumBatches;
		maximumBatches = newSize;
		if (doneList.size() > maximumBatches)
		{
			doneList.remove(0);
		}
        updateUndoRedo();
		return oldSize;
	}

	/**
	 * Method to display all changes.
	 */
	public static void showHistoryList()
	{
		System.out.println("----------  Done batches (" + doneList.size() + ") ----------:");
		for(int i=0; i<doneList.size(); i++)
		{
			ChangeBatch batch = (ChangeBatch)doneList.get(i);
			batch.describe("Done");
		}
		System.out.println("----------  Undone batches (" + undoneList.size() + ") ----------:");
		for(int i=0; i<undoneList.size(); i++)
		{
			ChangeBatch batch = (ChangeBatch)undoneList.get(i);
			batch.describe("Undone");
		}
	}
}
