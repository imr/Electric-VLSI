package com.sun.electric.tool.drc;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Consumer;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.MultiTaskJob;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;

import java.util.Collection;
import java.util.Map;

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

    public MTDRCTool(String jobName, Cell c, Consumer<MTDRCResult> consumer)
    {
        super(jobName, DRC.getDRCTool(), Job.Type.CHANGE, consumer);
        this.topCell= c;
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
