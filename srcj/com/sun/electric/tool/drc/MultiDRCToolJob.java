package com.sun.electric.tool.drc;

import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.*;

import java.util.*;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: Nov 15, 2007
 * Time: 2:18:14 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class MultiDRCToolJob extends Job {
    protected Cell topCell;
    protected Layer theLayer;
    protected String theLayerName; // to avoid serialization of the class Layer
    protected ErrorLogger errorLogger;
    protected long globalStartTime;

    MultiDRCToolJob(Cell c, String l, String jobName)
    {
        super(jobName, DRC.getDRCTool(), Type.REMOTE_EXAMINE, null, null, Priority.USER);
        this.theLayerName = l;
        this.topCell = c;
    }

    public void terminateOK()
    {
        // REMOTE_EXAMINE jobs not working properly in GUI mode
        if (!Job.BATCHMODE)
            MultiDRCCollectData.jobsList.add(errorLogger);
    }

    static Layer.Function.Set getMultiLayersSet(Layer layer)
    {
        Layer.Function.Set thisLayerFunction = (layer.getFunction().isPoly()) ?
        new Layer.Function.Set(layer.getFunction(), Layer.Function.GATE) :
        new Layer.Function.Set(layer.getFunction(), layer.getFunctionExtras());
        return thisLayerFunction;
    }

    /***********************************
     * Multi-Threaded Jobs
     ***********************************/
    public static MultiDRCCollectData checkDesignRules(Cell cell, boolean checkArea)
    {
        // Check if there are DRC rules for particular tech
        Technology tech = cell.getTechnology();
		DRCRules rules = DRC.getRules(tech);
        // Nothing to check for this particular technology
		if (rules == null || rules.getNumberOfRules() == 0)
                return null;

        CellLayersContainer cellLayersCon = new CellLayersContainer();
//        CheckLayerEnumerator layerCheck = new CheckLayerEnumerator(cell.getTechnology().getNumLayers());
//        HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, layerCheck);
        CheckCellLayerEnumerator layerCellCheck = new CheckCellLayerEnumerator(cellLayersCon);
        HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, layerCellCheck);
        GenMath.MutableBoolean polyDone = new GenMath.MutableBoolean(false);
        long globalStartTime = System.currentTimeMillis();
        // Multi thread the DRC algorithms per layer
        Collection<String> layers = cellLayersCon.getLayersSet(cell);

        // Collecing
        for (String layerS : layers)
//        for (Iterator<Layer> it = cell.getTechnology().getLayers(); it.hasNext();)
        {
            Layer layer = tech.findLayer(layerS);
//            Layer layer = it.next();
//            if (layerCheck.layersMap.get(layer) == null)
//            {
//                System.out.println("SkippingLayer " + layer.getName());
//                continue;
//            }
            if (checkArea)
                MultiDRCAreaToolJob.startMultiMinAreaChecking(cell, layer, cellLayersCon, globalStartTime, polyDone);
            else
                MultiLayoutDRCToolJob.startMultiLayoutChecking(cell, layer, null, null, null);
        }
        return new MultiDRCCollectData(globalStartTime);
    }


    /**
     * ***********************************************************************************************************
     * MultiDRCCollectData class
     * ************************************************************************************************************
     */
    public static class MultiDRCCollectData extends Job {
        static List<ErrorLogger> jobsList = new ArrayList<ErrorLogger>();
        long globalStartTime;

        MultiDRCCollectData(long s) {
            super("Design-Rule Check Collect Data", DRC.getDRCTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.globalStartTime = s;
            jobsList.clear();
            startJob();
        }

        /**
         * Method does do nothing. Just collect information at the end.
         *
         * @return
         */
        public boolean doIt() {
            // Print the results in this function in case of running in batch mode
            // and in terminateOK in case of using the GUI
            if (!Job.BATCHMODE) return true; // do nothing

            int totalErrors = 0, totalWarnings = 0;
            for (ErrorLogger data : jobsList) {
                totalErrors += data.getNumErrors();
                totalWarnings += data.getNumWarnings();
            }
            System.out.println("Total Number of Threads: " + Job.getNumThreads());
            System.out.println("Total Number of Errors: " + totalErrors);
            System.out.println("Total Number of Warnings: " + totalWarnings);
            System.out.println(totalErrors + " errors and " + totalWarnings + " warnings found (took " +
                    TextUtils.getElapsedTime(System.currentTimeMillis() - globalStartTime) + ")");
            return true;
        }

        public void terminateOK() {
            // This code will only be executed in GUI mode
            int totalErrors = 0, totalWarnings = 0;

            for (ErrorLogger data : jobsList) {
                totalErrors += data.getNumErrors();
                totalWarnings += data.getNumWarnings();
            }
            System.out.println("Total Number of Errors: " + totalErrors);
            System.out.println("Total Number of Warnings: " + totalWarnings);
            System.out.println(totalErrors + " errors and " + totalWarnings + " warnings found (took " +
                    TextUtils.getElapsedTime(System.currentTimeMillis() - globalStartTime) + ")");
        }
    }
}

/**************************************************************************************************************
	 *  CellLayersContainer class
 **************************************************************************************************************/
class CellLayersContainer implements Serializable {
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
 *  CheckLayerEnumerator class
 **************************************************************************************************************/
/** Class to collect which layers are available in the design
 *
 */
//    private static class CheckLayerEnumerator extends HierarchyEnumerator.Visitor
//    {
//        private int numLayersDone = 0, totalNumLayers;
//        private Map<Layer,Layer> layersMap;
//        private Map<Cell,Cell> cellsMap;
//        private Map<PrimitiveNode,PrimitiveNode> primitivesMap;
//
//        CheckLayerEnumerator(int totalNumL)
//        {
//            this.totalNumLayers = totalNumL;
//            layersMap = new HashMap<Layer,Layer>(totalNumLayers);
//            cellsMap = new HashMap<Cell,Cell>();
//            primitivesMap = new HashMap<PrimitiveNode,PrimitiveNode>();
//        }
//
//        /**
//         * When the cell should be visited. Either it is the first time or the number of layers hasn't reached
//         * the maximum
//         * @param cell
//         * @return
//         */
//        private boolean skipCell(Cell cell) {return cellsMap.get(cell) != null || doneWithLayers();}
//
//        private boolean doneWithLayers() {return numLayersDone == totalNumLayers;}
//
//        public boolean enterCell(HierarchyEnumerator.CellInfo info)
//        {
//            Cell cell = info.getCell();
//            if (skipCell(cell)) return false; // skip
//            cellsMap.put(cell, cell);
//            return true;
//        }
//
//        public void exitCell(HierarchyEnumerator.CellInfo info)
//        {;
//        }
//
//        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
//        {
//            if (doneWithLayers()) return false; // done;
//            NodeInst ni = no.getNodeInst();
//            if (NodeInst.isSpecialNode(ni)) return (false);
//            NodeProto np = ni.getProto();
//
//            // Cells
//            if (ni.isCellInstance()) return (true);
//
//            PrimitiveNode pNp = (PrimitiveNode)np;
//            if (primitivesMap.get(pNp) != null) return false; // done node.
//            primitivesMap.put(pNp, pNp);
//
//            for (Technology.NodeLayer nLayer : pNp.getLayers())
//            {
//                Layer layer = nLayer.getLayer();
//                if (layersMap.get(layer) == null) // not in yet
//                {
//                    layersMap.put(layer, layer);
//                    numLayersDone++;
//                }
//            }
//            return false;
//        }
//    }

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

