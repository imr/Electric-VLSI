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
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.tool.Consumer;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.MultiTaskJob;
import com.sun.electric.technology.*;

import java.util.*;
import java.io.Serializable;

/**
 * User: gg151869
 * Date: Dec 12, 2007
 */
public abstract class MTDRCTool extends MultiTaskJob<Layer, MTDRCTool.MTDRCResult, MTDRCTool.MTDRCResult>
{
    protected Cell topCell;
    protected long globalStartTime;
    protected CellLayersContainer cellLayersCon = new CellLayersContainer();
    protected final boolean printLog = false;
    protected DRCRules rules;

    public MTDRCTool(String jobName, Cell c, Consumer<MTDRCResult> consumer)
    {
        super(jobName, DRC.getDRCTool(), Job.Type.CHANGE, consumer);
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
        for (Map.Entry<Layer, MTDRCResult> e : taskResults.entrySet())
        {
            MTDRCResult p = e.getValue();
            numTE += p.numErrors;
            numTW += p.numWarns;
        }
        System.out.println("Total DRC Errors: " + numTE);
        System.out.println("Total DRC Warnings: " + numTW);
        long accuEndTime = System.currentTimeMillis() - globalStartTime;             
        System.out.println("Total Time: " + TextUtils.getElapsedTime(accuEndTime));
        return new MTDRCResult(numTE, numTW);
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

    boolean skipLayer(Layer theLayer)
    {
        if (theLayer == null) return false;
        
        if (theLayer.getFunction().isDiff() && theLayer.getName().toLowerCase().equals("p-active-well"))
            return true; // dirty way to skip the MoCMOS p-active well
        else if (theLayer.getFunction().isGatePoly())
            return true; // check transistor-poly together with polysilicon-1
        else if (checkArea() && theLayer.getFunction().isContact())  // via*, polyCut, activeCut
            return true;
        return false;
    }

    public static class MTDRCResult
    {
        private int numErrors, numWarns;

        MTDRCResult(int numE, int numW)
        {
            this.numErrors = numE;
            this.numWarns = numW;
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


/**************************************************************************************************************
	 *  CellLayersContainer class
 **************************************************************************************************************/
class CellLayersContainer implements Serializable
{
    private Map<NodeProto, Set<String>> cellLayersMap;

    CellLayersContainer() {
        cellLayersMap = new HashMap<NodeProto, Set<String>>();
    }

    Set<String> getLayersSet(NodeProto cell) {
        return cellLayersMap.get(cell);
    }

    void addCellLayers(Cell cell, Set<String> set) {
        cellLayersMap.put(cell, set);
    }

    boolean addCellLayers(Cell cell, Layer layer) {
        Set<String> set = cellLayersMap.get(cell);

        // first time the cell is accessed
        if (set == null) {
            set = new HashSet<String>(1);
            cellLayersMap.put(cell, set);
        }
        return set.add(layer.getName());
    }
}

/**************************************************************************************************************
 *  CheckCellLayerEnumerator class
 **************************************************************************************************************/

/**
 * Class to collect which layers are available in the design
 */
class CheckCellLayerEnumerator extends HierarchyEnumerator.Visitor {
    private Map<Cell, Cell> cellsMap;
    private CellLayersContainer cellLayersCon;

    CheckCellLayerEnumerator(CellLayersContainer cellLayersC) {
        cellsMap = new HashMap<Cell, Cell>();
        cellLayersCon = cellLayersC;
    }

    /**
     * When the cell should be visited. Either it is the first time or the number of layers hasn't reached
     * the maximum
     *
     * @param cell
     * @return
     */
    private boolean skipCell(Cell cell) {
        return cellsMap.get(cell) != null;
    }

    public boolean enterCell(HierarchyEnumerator.CellInfo info) {
        Cell cell = info.getCell();
        if (skipCell(cell)) return false; // skip
        cellsMap.put(cell, cell);
        return true;
    }

    private Set<String> getLayersInCell(Cell cell) {
        Map<NodeProto, NodeProto> tempNodeMap = new HashMap<NodeProto, NodeProto>();
        Map<ArcProto, ArcProto> tempArcMap = new HashMap<ArcProto, ArcProto>();
        Set<String> set = new HashSet<String>();

        // Nodes
        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext();) {
            NodeInst ni = it.next();
            NodeProto np = ni.getProto();
            if (ni.isCellInstance()) {
                Set<String> s = cellLayersCon.getLayersSet(np);
                set.addAll(s);
                assert (s != null); // it must have layers? unless is empty
            } else {
                if (tempNodeMap.get(np) != null)
                    continue; // done with this PrimitiveNode
                tempNodeMap.put(np, np);

                if (NodeInst.isSpecialNode(ni)) // like pins
                    continue;

                PrimitiveNode pNp = (PrimitiveNode) np;
                for (Technology.NodeLayer nLayer : pNp.getLayers()) {
                    Layer layer = nLayer.getLayer();
                    set.add(layer.getName());
                }
            }
        }

        // Arcs
        for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext();) {
            ArcInst ai = it.next();
            ArcProto ap = ai.getProto();
            if (tempArcMap.get(ap) != null)
                continue; // done with this arc primitive
            tempArcMap.put(ap, ap);
            for (int i = 0; i < ap.getNumArcLayers(); i++) {
                Layer layer = ap.getLayer(i);
                set.add(layer.getName());
            }
        }
        return set;
    }

    public void exitCell(HierarchyEnumerator.CellInfo info) {
        Cell cell = info.getCell();
        Set<String> set = getLayersInCell(cell);
        assert (cellLayersCon.getLayersSet(cell) == null);
        cellLayersCon.addCellLayers(cell, set);
    }

    public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) {
        NodeInst ni = no.getNodeInst();

        // true only for Cells
        return ni.isCellInstance();
    }
}

/**************************************************************************************************************
 *  ValidationLayers class
 **************************************************************************************************************/
class ValidationLayers
{
    private boolean [] layersValid;

    /**
	 * Method to determine which layers in a Technology are valid.
	 */
	ValidationLayers(Technology tech)
	{
		// determine the layers that are being used
		int numLayers = tech.getNumLayers();
		layersValid = new boolean[numLayers];
		for(int i=0; i < numLayers; i++)
			layersValid[i] = false;
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			if (np.isNotUsed()) continue;
			Technology.NodeLayer [] layers = np.getLayers();
			for(int i=0; i<layers.length; i++)
			{
				Layer layer = layers[i].getLayer();
				layersValid[layer.getIndex()] = true;
			}
		}
		for(Iterator it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = (ArcProto)it.next();
			if (ap.isNotUsed()) continue;
			for (Iterator<Layer> lIt = ap.getLayerIterator(); lIt.hasNext(); )
			{
				Layer layer = lIt.next();
				layersValid[layer.getIndex()] = true;
			}
		}
	}
}


