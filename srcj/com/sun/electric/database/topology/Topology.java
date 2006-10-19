/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Topology.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.topology;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A class to manage nodes and arcs of a Cell.
 */
public class Topology {
    /** Owner cell of this Topology. */                             final Cell cell;
    
    /** A maximal suffix of temporary arc name. */                  private int maxArcSuffix = -1;
    /** Chronological list of ArcInst in this Cell. */              private final ArrayList<ArcInst> chronArcs = new ArrayList<ArcInst>();
    /** A list of ArcInsts in this Cell. */							private final ArrayList<ArcInst> arcs = new ArrayList<ArcInst>();
    
	/** The geometric data structure. */							private RTNode rTree = RTNode.makeTopLevel();
    /** True of RTree matches node/arc sizes */                     private boolean rTreeFresh;
    
    /** Creates a new instance of Topology */
    public Topology(Cell cell, boolean loadBackup) {
        this.cell = cell;
        if (loadBackup)
            updateArcs(cell.backup());
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
		return arcs.get(arcIndex);
	}

	/**
	 * Method to return the ArcInst by its chronological index.
	 * @param arcId chronological index of ArcInst.
	 * @return the ArcInst with specified chronological index.
	 */
	public ArcInst getArcById(int arcId)
	{
		return arcId < chronArcs.size() ? chronArcs.get(arcId) : null;
	}

	/**
	 * Method to find a named ArcInst on this Cell.
	 * @param name the name of the ArcInst.
	 * @return the ArcInst.  Returns null if none with that name are found.
	 */
	public ArcInst findArc(String name)
	{
		int arcIndex = searchArc(name, 0);
		if (arcIndex >= 0) return arcs.get(arcIndex);
		arcIndex = - arcIndex - 1;
		if (arcIndex < arcs.size())
		{
			ArcInst ai = arcs.get(arcIndex);
			if (ai.getName().equals(name)) return ai;
		}
		return null;
	}

	/**
	 * Method to add a new ArcInst to the cell.
	 * @param ai the ArcInst to be included in the cell.
	 */
	void addArc(ArcInst ai)
	{
        cell.setTopologyModified();
		int arcIndex = searchArc(ai.getName(), ai.getD().arcId);
		assert arcIndex < 0;
		arcIndex = - arcIndex - 1;
		arcs.add(arcIndex, ai);
		for (; arcIndex < arcs.size(); arcIndex++)
		{
			ArcInst a = arcs.get(arcIndex);
			a.setArcIndex(arcIndex);
		}
        int arcId = ai.getD().arcId;
        while (chronArcs.size() <= arcId) chronArcs.add(null);
        assert chronArcs.get(arcId) == null;
        chronArcs.set(arcId, ai);
        
        // update maximal arc name suffux temporary name
		if (ai.isUsernamed()) return;
		Name name = ai.getNameKey();
        assert name.getBasename() == ImmutableArcInst.BASENAME;
        maxArcSuffix = Math.max(maxArcSuffix, name.getNumSuffix());
        cell.setDirty();
	}

	/**
	 * Method to return unique autoname for ArcInst in this cell.
	 * @return a unique autoname for ArcInst in this cell.
	 */
	Name getArcAutoname()
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
	boolean hasTempArcName(Name name)
	{
		return name.isTempname() && findArc(name.toString()) != null;
	}

	/**
	 * Method to remove an ArcInst from the cell.
	 * @param ai the ArcInst to be removed from the cell.
	 */
	void removeArc(ArcInst ai)
	{
		cell.checkChanging();
        cell.setTopologyModified();
		assert ai.isLinked();
		int arcIndex = ai.getArcIndex();
		ArcInst removedAi = (ArcInst) arcs.remove(arcIndex);
		assert removedAi == ai;
		for (int i = arcIndex; i < arcs.size(); i++)
		{
			ArcInst a = arcs.get(i);
			a.setArcIndex(i);
		}
		ai.setArcIndex(-1);
        int arcId = ai.getD().arcId;
        assert chronArcs.get(arcId) == ai;
        chronArcs.set(arcId, null);
        cell.setDirty();
	}

    public ImmutableArcInst[] backupArcs(ImmutableArrayList<ImmutableArcInst> oldArcs) {
        ImmutableArcInst[] newArcs = new ImmutableArcInst[arcs.size()];
        boolean changed = arcs.size() != oldArcs.size();
        for (int i = 0; i < arcs.size(); i++) {
            ArcInst ai = arcs.get(i);
            ImmutableArcInst d = ai.getD();
            changed = changed || oldArcs.get(i) != d;
            newArcs[i] = d;
        }
        return changed ? newArcs : null;
    }
    
    public void updateArcs(CellBackup newBackup) {
        arcs.clear();
        maxArcSuffix = -1;
        for (int i = 0; i < newBackup.arcs.size(); i++) {
            ImmutableArcInst d = newBackup.arcs.get(i);
            while (d.arcId >= chronArcs.size()) chronArcs.add(null);
            ArcInst ai = chronArcs.get(d.arcId);
            PortInst headPi = cell.getPortInst(d.headNodeId, d.headPortId);
            PortInst tailPi = cell.getPortInst(d.tailNodeId, d.tailPortId);
            if (ai != null && (/*!full ||*/ ai.getHeadPortInst() == headPi && ai.getTailPortInst() == tailPi)) {
                ai.setDInUndo(d);
            } else {
                ai = new ArcInst(this, d, headPi, tailPi);
                chronArcs.set(d.arcId, ai);
            }
            ai.setArcIndex(i);
            arcs.add(ai);
//            tailPi.getNodeInst().lowLevelAddConnection(ai.getTail());
//            headPi.getNodeInst().lowLevelAddConnection(ai.getHead());
            if (!ai.isUsernamed()) {
                Name name = ai.getNameKey();
                assert name.getBasename() == ImmutableArcInst.BASENAME;
                maxArcSuffix = Math.max(maxArcSuffix, name.getNumSuffix());
            }
        }

        int arcCount = 0;
        for (int i = 0; i < chronArcs.size(); i++) {
            ArcInst ai = chronArcs.get(i);
            if (ai == null) continue;
            int arcIndex = ai.getArcIndex();
            if (arcIndex >= arcs.size() || ai != arcs.get(arcIndex)) {
                ai.setArcIndex(-1);
                chronArcs.set(i, null);
                continue;
            }
            arcCount++;
        }
        assert arcCount == arcs.size();
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
			ArcInst ai = arcs.get(mid);
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
	 * Method to return an interator over all Geometric objects in a given area of this Cell that allows
     * to ignore elements touching the area.
	 * @param bounds the specified area to search.
     * @param includeEdges true if Geometric objects along edges are considered in.
	 * @return an iterator over all of the Geometric objects in that area.
	 */
    public Iterator<Geometric> searchIterator(Rectangle2D bounds, boolean includeEdges) {
        return new RTNode.Search(bounds, getRTree(), includeEdges);
    }

    public void unfreshRTree() {
        rTreeFresh = false;
    }
    
	/**
	 * Method to R-Tree of this Cell.
	 * The R-Tree organizes all of the Geometric objects spatially for quick search.
	 * @return R-Tree of this Cell.
	 */
    private RTNode getRTree() {
        if (rTreeFresh) return rTree;
        EDatabase database = cell.getDatabase();
        if (database.canComputeBounds()) {
            rebuildRTree();
            rTreeFresh = true;
        } else {
            Snapshot snapshotBefore = database.getFreshSnapshot();
            rebuildRTree();
            rTreeFresh = snapshotBefore != null && database.getFreshSnapshot() == snapshotBefore;
        }
        return rTree;
    }

    private void rebuildRTree() {
//        long startTime = System.currentTimeMillis();
        CellId cellId = cell.getId();
        RTNode root = RTNode.makeTopLevel();
        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            root = RTNode.linkGeom(cellId, root, ni);
        }
        for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); ) {
            ArcInst ai = it.next();
            root = RTNode.linkGeom(cellId, root, ai);
        }
        root.checkRTree(0, cellId);
        rTree = root;
//        long stopTime = System.currentTimeMillis();
//        if (Job.getDebug()) System.out.println("Rebuilding R-Tree in " + this + " took " + (stopTime - startTime) + " msec");
    }
    
    /**
     * Method to check invariants in this Cell.
     * @exception AssertionError if invariants are not valid
     */
    public void check() {
        // check arcs
        ArcInst prevAi = null;
        for(int arcIndex = 0; arcIndex < arcs.size(); arcIndex++) {
            ArcInst ai = arcs.get(arcIndex);
            ImmutableArcInst a = ai.getD();
            assert ai.getParent() == cell;
            assert ai.getArcIndex() == arcIndex;
            assert chronArcs.get(a.arcId) == ai;
            if (prevAi != null) {
                int cmp = TextUtils.STRING_NUMBER_ORDER.compare(prevAi.getName(), ai.getName());
                assert cmp <= 0;
                if (cmp == 0)
                    assert prevAi.getD().arcId < a.arcId;
            }
            assert ai.getHeadPortInst() == cell.getPortInst(a.headNodeId, a.headPortId);
            assert ai.getTailPortInst() == cell.getPortInst(a.tailNodeId, a.tailPortId);
            ai.check();
            prevAi = ai;
        }
        for (int arcId = 0; arcId < chronArcs.size(); arcId++) {
            ArcInst ai = chronArcs.get(arcId);
            if (ai == null) continue;
            assert ai.getD().arcId == arcId;
            assert ai == arcs.get(ai.getArcIndex());
        }
        
        if (rTreeFresh)
            rTree.checkRTree(0, cell.getId());
    }
}
