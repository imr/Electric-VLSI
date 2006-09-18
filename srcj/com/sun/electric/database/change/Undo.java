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
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
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
        private int oldSnapshotId, newSnapshotId;
		private Tool tool;
		private String activity;
		private int startingHighlights = -1;				// highlights before changes made
		private int preUndoHighlights = -1;				// highlights before undo of changes done

		private ChangeBatch(Snapshot oldSnapshot, Tool tool, String activity, int startingHighlights, Snapshot newSnapshot) {
            oldSnapshotId = oldSnapshot.snapshotId;
            this.tool = tool;
            this.activity = activity;
            this.startingHighlights = startingHighlights;
            newSnapshotId = newSnapshot.snapshotId;
        }
		
        public int getStartingHighlights() { return startingHighlights; }
        public int getPreUndoHighlights() { return preUndoHighlights; }
        
		/**
		 * Method to return the Tool associated with this ChangeBatch.
		 * @return the Tool associated with this ChangeBatch.
		 */
		public Tool getTool() { return tool; }

		private void describe(String title)
		{
            String activityFromTool = activity;
            if (tool != null) activityFromTool += " from " + tool.getName() + " tool";
			String message = "*** Batch '" + title + "', " + oldSnapshotId + "->" + newSnapshotId + " (" + activityFromTool + ")";
			System.out.println(message);
		}
	}

	private static int maximumBatches = User.getMaxUndoHistory();
	private static final List<ChangeBatch> doneList = new ArrayList<ChangeBatch>();
	private static final List<ChangeBatch> undoneList = new ArrayList<ChangeBatch>();

    /**
	 * Method to terminate the current batch of changes.
	 */
	public static int endChanges(Snapshot oldSnapshot, Tool tool, String activity, Snapshot newSnapshot)
	{
        int highlights = Job.getExtendedUserInterface().saveHighlights();
        int oldId = oldSnapshot.snapshotId;
        int newId = newSnapshot.snapshotId;
        while (!doneList.isEmpty() && doneList.get(doneList.size() - 1).newSnapshotId != oldId)
            doneList.remove(doneList.size() - 1);
        while (!undoneList.isEmpty() && undoneList.get(0).oldSnapshotId != oldId)
            undoneList.remove(0);
        int doneIndex = indexOf(doneList, newId);
        int undoneIndex = indexOf(undoneList, newId);
        if (doneIndex >= 0) {
            // undo
            while (doneList.size() > doneIndex) {
                ChangeBatch batch = doneList.remove(doneList.size() - 1);
                System.out.println("Undoing: " + batch.activity);
                batch.preUndoHighlights = highlights;
                highlights = batch.startingHighlights;
                undoneList.add(0, batch);
            }
            Job.getExtendedUserInterface().restoreHighlights(highlights);
        } else if (undoneIndex >= 0) {
            // redo
            for (int i = 0; i < undoneIndex; i++) {
                ChangeBatch batch = undoneList.remove(0);
                System.out.println("Redoing: " + batch.activity);
                batch.startingHighlights = highlights;
                highlights = batch.preUndoHighlights;
                doneList.add(batch);
            }
            Job.getExtendedUserInterface().restoreHighlights(highlights);
        } else {
            // change task
            undoneList.clear();
            doneList.add(new ChangeBatch(oldSnapshot, tool, activity, highlights, newSnapshot));
            highlights = -1;
        }
        updateUndoRedo();
        return highlights;
	}
    
    private static void invalidate(int snapshotId) {
        int doneIndex = indexOf(doneList, snapshotId);
        int undoneIndex = indexOf(undoneList, snapshotId);
        if (doneIndex >= 0) {
            // undo
            if (doneIndex == 0) {
                doneList.clear();
            } else {
                while (doneList.size() >= doneIndex)
                    doneList.remove(doneList.size() - 1);
            }
        }
        if (undoneIndex >= 0) {
            // redo
            if (undoneIndex == undoneList.size()) {
                undoneList.clear();
            } else {
                for (int i = 0; i < undoneIndex; i++) {
                    if (!undoneList.isEmpty())
                        undoneList.remove(0);
                }
            }
        }
        updateUndoRedo();
    }
    
    private static int indexOf(List<ChangeBatch> list, int snapshotId) {
        for (int i = 0; i < list.size(); i++) {
            ChangeBatch batch = list.get(i);
            if (batch.oldSnapshotId == snapshotId) return i;
            if (batch.newSnapshotId == snapshotId) return i + 1;
        }
        return -1;
    }
 
    private static void updateUndoRedo() {
        ServerJobManager.setUndoRedoStatus(!doneList.isEmpty(), !undoneList.isEmpty());
    }

    /**
     * Method to undo a change.
     */
    public static void undo() {
        if (!doneList.isEmpty())
            new UndoJob("Undo", doneList.get(doneList.size() - 1).oldSnapshotId);
        else
            System.out.println("Undo failed");
    }
    
    /**
     * Method to redo a change.
     */
    public static void redo() {
        if (!undoneList.isEmpty())
            new UndoJob("Redo", undoneList.get(0).newSnapshotId);
        else
            System.out.println("Redo failed");
    }
            
    public static class UndoJob extends Job {
        int snapshotId;
        
        public UndoJob(String jobName, int snapshotId) {
            super(jobName, User.getUserTool(), Job.Type.UNDO, null, null, Job.Priority.USER);
            this.snapshotId = snapshotId;
            startJob();
        }
        
        public int getSnapshotId() { return snapshotId; }
        
        public boolean doIt() throws JobException {
            // must not be called.
            throw new IllegalStateException();
        }
        
        public void terminateFail(Throwable e) {
            invalidate(snapshotId);
            super.terminateFail(e);
        }
    }
    
//	/**
//	 * Method to undo the last batch of changes.
//	 * @return the batch number that was undone.
//	 * Returns null if nothing was undone.
//	 */
//	public static int undoABatch(int savedHighlights) throws JobException
//	{
//		// get the most recent batch of changes
//		int listSize = doneList.size();
//		if (listSize == 0) return null;
//		ChangeBatch batch = (ChangeBatch)doneList.get(listSize-1);
//		doneList.remove(listSize-1);
//		undoneList.add(batch);
//        updateUndoRedo();
//
//		// save pre undo highlights
//		batch.preUndoHighlights = savedHighlights;
//
//		// look through the changes in this batch
//        Job.undo(batch.oldSnapshotId);
//
//		// Put message in Message Window
//		System.out.println("Undoing: " + batch.activity);
////        batch.describe("Undo");
//		return batch;
//	}
//
//	/**
//	 * Method to redo the last batch of changes.
//	 * @return true if a batch was redone.
//	 */
//	public static ChangeBatch redoABatch() throws JobException
//	{
//		// get the most recent batch of changes
//		int listSize = undoneList.size();
//		if (listSize == 0) return null;
//		ChangeBatch batch = undoneList.get(listSize-1);
//		undoneList.remove(listSize-1);
//		doneList.add(batch);
//        updateUndoRedo();
//
//        Job.undo(batch.newSnapshotId);
//        
//        // Put message in Message Window
//		System.out.println("Redoing: " + batch.activity);
////        batch.describe("Redo");
//		return batch;
//	}
//
//	/**
//	 * Method to prevent undo by deleting all change batches.
//	 */
//	public static void noUndoAllowed()
//	{
// 		// kill them all
// 		doneList.clear();
// 		undoneList.clear();
//        updateUndoRedo();
//	}

	/**
	 * Method to prevent redo by deleting all undone change batches.
	 */
	public static void noRedoAllowed()
	{
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
