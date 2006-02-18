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

import com.sun.electric.database.Snapshot;
import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.constraint.Layout;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.ServerJobManager;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.User;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This interface defines changes that are made to the database.
 */
public class Undo
{
	/**
	 * Class to describe a batch of changes to the Electric database.
	 */
	public static class ChangeBatch
	{
        private Snapshot oldSnapshot, newSnapshot;
        
		private int batchNumber;
		private Tool tool;
		private String activity;
		private int startingHighlights = -1;				// highlights before changes made
		private int preUndoHighlights = -1;				// highlights before undo of changes done

		private ChangeBatch(Snapshot oldSnapshot, Tool tool, String activity, int startingHighlights) {
            this.oldSnapshot = oldSnapshot;
            this.tool = tool;
            this.activity = activity;
            this.startingHighlights = startingHighlights;
    		batchNumber = ++overallBatchNumber;
        }
		
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

        public void reverse(boolean backwards) {
            EDatabase edb = EDatabase.serverDatabase();
            edb.checkFresh(newSnapshot);
            if (newSnapshot != oldSnapshot) {
                NetworkTool.getNetworkTool().startBatch(null, true);
//                // broadcast a start-batch on the first change
//                for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); ) {
//                    Listener listener = it.next();
//                    listener.startBatch(listener, true);
//                }
//                
                Snapshot tmpSnapshot = newSnapshot;
                newSnapshot = oldSnapshot;
                oldSnapshot = tmpSnapshot;
              
                edb.undo(newSnapshot);
            }
            
    		// broadcast the end-batch
            refreshCellBounds();
            Job.currentUI.showSnapshot(newSnapshot, batchNumber, true);
            NetworkTool.getNetworkTool().endBatch(oldSnapshot, newSnapshot, true);
            edb.checkFresh(newSnapshot);
        }
        
		private void describe(String title)
		{
			String message = "*** Batch '" + title + "', " + batchNumber + " (" + activity + " from " + tool.getName() + " tool)";
			System.out.println(message);
		}
	}

	private static boolean doChangesQuietly = false;
	private static ChangeBatch currentBatch = null;
	private static int maximumBatches = User.getMaxUndoHistory();
	private static int overallBatchNumber = 0;
	private static List<ChangeBatch> doneList = new ArrayList<ChangeBatch>();
	private static List<ChangeBatch> undoneList = new ArrayList<ChangeBatch>();

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

		// allocate a new change batch
		currentBatch = new ChangeBatch(oldSnapshot, tool, activity, startingHighlights);

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
        NetworkTool.getNetworkTool().startBatch(tool, false);

//		for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
//		{
//			Listener listener = (Listener)it.next();
//			listener.startBatch(tool, false);
//		}
	}

    private static void clearChanges() {
        endChanges();
        if (currentBatch == null) return;
        System.out.println("Ignored a batch " + currentBatch.activity);
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
		if (EDatabase.serverDatabase().backup() == currentBatch.oldSnapshot)
		{
			// remove from doneList
			doneList.remove(currentBatch);
            updateUndoRedo();
		}

		// changes made: apply final constraints to this batch of changes
        String userName = System.getProperty("user.name"); 
        currentBatch.newSnapshot = Constraints.getCurrent().endBatch(userName);
        Job.currentUI.showSnapshot(currentBatch.newSnapshot, currentBatch.batchNumber, false);
        NetworkTool.getNetworkTool().endBatch(currentBatch.oldSnapshot, currentBatch.newSnapshot, false);
        EDatabase.serverDatabase().checkFresh(currentBatch.newSnapshot);
		currentBatch = null;
	}

    private static synchronized void updateUndoRedo() {
        ServerJobManager.setUndoRedoStatus(!doneList.isEmpty(), !undoneList.isEmpty());
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
		EDatabase.serverDatabase().checkInvariants();
        Layout.changesQuiet(quiet);
//		NetworkTool.changesQuiet(quiet);
		boolean formerQuiet = doChangesQuietly;
        doChangesQuietly = quiet;
		return formerQuiet;
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
		ChangeBatch batch = undoneList.get(listSize-1);
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
            Library lib = it.next();
            for (Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); ) {
                Cell cell = cIt.next();
                cell.getBounds();
            }
        }
    }
    
    /**
	 * Returns root of up-tree of undo or redo batch.
	 * @param redo true if redo batch, false if undo batch
	 * @return root of up-tree of a batch.
	 */
	public static Cell upCell(boolean redo) { return null; }

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
