/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Quick.java
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
package com.sun.electric.tool.drc;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.tool.Consumer;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.MultiTaskJob;
import com.sun.electric.technology.*;

import java.util.*;

/**
 * User: gg151869
 * Date: Dec 12, 2007
 */
public abstract class MTDRCTool extends MultiTaskJob<Layer, MTDRCTool.MTDRCResult, MTDRCTool.MTDRCResult>
//public abstract class MTDRCTool extends MultiTaskJob<Layer, MTDRCTool.MTDRCResult, MTDRCTool.MTDRCResult>
{
    protected DRC.DRCPreferences dp;
    protected Cell topCell;
    protected long globalStartTime;
    protected CellLayersContainer cellLayersCon = new CellLayersContainer();
    protected final boolean printLog = Job.getDebug();
    protected DRCRules rules;

    protected MTDRCTool(String jobName, DRC.DRCPreferences dp, Cell c, Consumer<MTDRCResult> consumer)
    {
        super(jobName, DRC.getDRCTool(), consumer);
        this.dp = dp;
        this.topCell= c;
        // Rules set must be local to avoid concurrency issues with other tasks
        this.rules = topCell.getTechnology().getFactoryDesignRules();
    }

    @Override
    public void prepareTasks()
    {
        Technology tech = topCell.getTechnology();
        cellLayersCon = new CellLayersContainer();
        CheckCellLayerEnumerator layerCellCheck = new CheckCellLayerEnumerator(cellLayersCon);
        HierarchyEnumerator.enumerateCell(topCell, VarContext.globalContext, layerCellCheck);
        Collection<String> layers = cellLayersCon.getLayersSet(topCell);
        globalStartTime = System.currentTimeMillis();
        for (String layerS : layers)
        {
            Layer layer = tech.findLayer(layerS);
            startTask(layer.getName(), layer);
        }
        if (!checkArea())
            startTask("Node Min Size.", null);
    }

    @Override
    public MTDRCResult mergeTaskResults(Map<Layer,MTDRCResult> taskResults)
    {
        int numTE = 0, numTW = 0;
        Set<Cell> goodSpacingSet = new HashSet<Cell>();
        Set<Cell> goodAreaSet = new HashSet<Cell>();
        Set<Cell> cleanSpacingSet = new HashSet<Cell>();
        Set<Cell> cleanAreaSet = new HashSet<Cell>();
        boolean runFine = true;

        for (Map.Entry<Layer, MTDRCResult> e : taskResults.entrySet())
        {
            MTDRCResult p = e.getValue();
            numTE += p.numErrors;
            numTW += p.numWarns;
            if (!p.runfine)
                runFine = false;

            // Collect all cells that must be clear
            cleanSpacingSet.addAll(p.cleanSpacingDRCDate);
            cleanAreaSet.addAll(p.cleanAreaDRCDate);
        }
        // Now that all the cells to be clean are collected, then good cells can be stored.
        for (Map.Entry<Layer, MTDRCResult> e : taskResults.entrySet())
        {
            MTDRCResult p = e.getValue();
            for (Cell c : p.goodSpacingDRCDate)
            {
                if (!cleanSpacingSet.contains(c))
                    goodSpacingSet.add(c);
            }
            for (Cell c : p.goodAreaDRCDate)
            {
                if (!cleanAreaSet.contains(c))
                    goodAreaSet.add(c);
            }
        }
        System.out.println("Finished " + ((runFine)?"without":"with") + " problems.");

        System.out.println("Total DRC Errors: " + numTE);
        System.out.println("Total DRC Warnings: " + numTW);
        long accuEndTime = System.currentTimeMillis() - globalStartTime;
        System.out.println("Total Time: " + TextUtils.getElapsedTime(accuEndTime));

        if (runFine)
        {
            int activeSpacingBits = DRC.getActiveBits(topCell.getTechnology(), dp);
             DRC.addDRCUpdate(activeSpacingBits, goodSpacingSet, cleanSpacingSet,
                goodAreaSet, cleanAreaSet, null, dp);
        }

        return new MTDRCResult(numTE, numTW, runFine, null, null, null, null, null);
    }

    @Override
    public MTDRCResult runTask(Layer taskKey)
    {
        if (skipLayer(taskKey))
            return null;
        return runTaskInternal(taskKey);
    }

    abstract MTDRCResult runTaskInternal(Layer taskKey);

    abstract boolean checkArea();

    static boolean skipLayerInvalidForMinArea(Layer theLayer)
    {
        // via*, polyCut, activeCut if theLayer is not null
        // theLayer is null if it is checking node min sizes
        return theLayer == null || theLayer.getFunction().isContact(); // via*, polyCut, activeCut
    }

    boolean skipLayer(Layer theLayer)
    {
        if (theLayer == null) return false;

        if (theLayer.getFunction().isDiff() && theLayer.getName().toLowerCase().equals("p-active-well"))
            return true; // dirty way to skip the MoCMOS p-active well
        else if (theLayer.getFunction().isGatePoly())
            return true; // check transistor-poly together with polysilicon-1
        else if (checkArea() && skipLayerInvalidForMinArea(theLayer))  // via*, polyCut, activeCut
            return true;
        return false;
    }

    public static class MTDRCResult
    {
        private int numErrors, numWarns;
        private boolean runfine;
        private HashSet<Cell> goodSpacingDRCDate, goodAreaDRCDate;
        private HashSet<Cell> cleanSpacingDRCDate, cleanAreaDRCDate;

        MTDRCResult(int numE, int numW, boolean notAborted,
                    HashSet<Cell> goodSpacingDRCD, HashSet<Cell> cleanSpacingDRCD,
                    HashSet<Cell> goodAreaDRCD, HashSet<Cell> cleanAreaDRCD,
                    HashMap<Geometric, List<Variable>> newVars)
        {
            this.numErrors = numE;
            this.numWarns = numW;
            this.runfine = notAborted;
            this.goodSpacingDRCDate = goodSpacingDRCD;
            this.cleanSpacingDRCDate = cleanSpacingDRCD;
            this.goodAreaDRCDate = goodAreaDRCD;
            this.cleanAreaDRCDate = cleanAreaDRCD;
            assert(newVars == null); // not implemented for Schematics DRC
        }

        public int getNumErrors()
        {
            return numErrors;
        }

        public int getNumWarnings()
        {
            return numWarns;
        }
    }
}
