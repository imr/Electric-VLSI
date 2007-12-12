package com.sun.electric.tool.drc;

import com.sun.electric.technology.*;
import com.sun.electric.tool.Consumer;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.geometry.*;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;

import java.util.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * User: gg151869
 * Date: Dec 11, 2007
 */

public class MTDRCAreaTool extends MTDRCTool
{
    public MTDRCAreaTool(Cell c, Consumer<MTDRCResult> consumer)
    {
        super("Design-Rule Area Check " + c, c, consumer);
    }

    @Override
    boolean checkArea() {return true;}

    @Override
    public MTDRCResult runTaskInternal(Layer theLayer)
    {
        DRCTemplate minAreaRule = DRC.getMinValue(theLayer, DRCTemplate.DRCRuleType.MINAREA);
        DRCTemplate enclosedAreaRule = DRC.getMinValue(theLayer, DRCTemplate.DRCRuleType.MINENCLOSEDAREA);
        DRCTemplate spaceRule = DRC.getSpacingRule(theLayer, null, theLayer, null, true, -1, -1.0, -1.0); // UCONSPA, CONSPA or SPACING
        if (minAreaRule == null && enclosedAreaRule == null && spaceRule == null)
            return null;

        ErrorLogger errorLogger = DRC.getDRCErrorLogger(true, false, ", Layer " + theLayer.getName());
        String msg = "Cell " + topCell.getName() + " , layer " + theLayer.getName();
        System.out.println("DRC for " + msg + " in thread " + Thread.currentThread().getName());
        HierarchyEnumerator.Visitor quickArea = new LayerAreaEnumerator(theLayer, minAreaRule, enclosedAreaRule,
            spaceRule, errorLogger, GeometryHandler.GHMode.ALGO_SWEEP,
                cellLayersCon);
        HierarchyEnumerator.enumerateCell(topCell, VarContext.globalContext, quickArea);

        long endTime = System.currentTimeMillis();
        int errorCount = errorLogger.getNumErrors();
        int warnCount = errorLogger.getNumWarnings();
        System.out.println(errorCount + " errors and " + warnCount + " warnings found in " + msg
                + " (took " + TextUtils.getElapsedTime(endTime - startTime) + " in thread " + Thread.currentThread().getName() + ")");
        long accuEndTime = System.currentTimeMillis() - globalStartTime;
        System.out.println("Accumulative time " + TextUtils.getElapsedTime(accuEndTime));
        return new MTDRCResult(errorCount, warnCount);
    }

    /**************************************************************************************************************
	 *  LayerAreaEnumerator class
	 **************************************************************************************************************/
    /**
     * Class that uses local GeometryHandler to calculate the area per cell
     */
    private class LayerAreaEnumerator extends HierarchyEnumerator.Visitor
    {
        private Layer theLayer;
        private Map<Cell,GeometryHandlerLayerBucket> cellsMap;
        private Layer.Function.Set thisLayerFunction;
        private GeometryHandler.GHMode mode;
        private Collection<PrimitiveNode> nodesList; // NodeProto that contains this layer
        private Collection<ArcProto> arcsList; // ArcProto that contains this layer
        private CellLayersContainer cellLayersCon;
        private DRCTemplate minAreaRule, enclosedAreaRule, spacingRule;
        private ErrorLogger errorLogger;

        LayerAreaEnumerator(Layer layer, DRCTemplate minAreaR, DRCTemplate enclosedAreaR, DRCTemplate spaceR,
                            ErrorLogger ErrorLog,
                            GeometryHandler.GHMode m, CellLayersContainer cellLayersC)
        {
            // This is required so the poly arcs will be properly merged with the transistor polys
            // even though non-electrical layers are retrieved
            this.minAreaRule = minAreaR;
            this.enclosedAreaRule = enclosedAreaR;
            this.spacingRule = spaceR;
            this.errorLogger = ErrorLog;
            this.theLayer = layer;
            this.thisLayerFunction = DRC.getMultiLayersSet(theLayer);
            this.mode = m;
            cellsMap = new HashMap<Cell,GeometryHandlerLayerBucket>();
            nodesList = new ArrayList<PrimitiveNode>();
            this.cellLayersCon = cellLayersC;
            for (PrimitiveNode node : topCell.getTechnology().getNodesCollection())
            {
                for (Technology.NodeLayer nLayer : node.getLayers())
                {
                    if (thisLayerFunction.contains(nLayer.getLayer().getFunction(), theLayer.getFunctionExtras()))
//                    if (nLayer.getLayer() == theLayer)
                    {
                        nodesList.add(node);
                        break; // found
                    }
                }
            }
            arcsList = new ArrayList<ArcProto>();
            for (ArcProto ap : topCell.getTechnology().getArcsCollection())
            {
                for (int i = 0; i < ap.getNumArcLayers(); i++)
                {
                    if (ap.getLayer(i) == theLayer)
                    {
                        arcsList.add(ap);
                        break; // found
                    }
                }
            }
        }

        private class GeometryHandlerLayerBucket {
            GeometryHandler local;
            boolean merged = false;
            GeometryHandlerLayerBucket()
            {
                local = GeometryHandler.createGeometryHandler(mode, 1);
            }
            void mergeGeometry(Cell cell)
            {
                if (!merged)
                {
                    merged = true;
                    for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext();)
                    {
                        NodeInst ni = it.next();
                        if (!ni.isCellInstance()) continue; // only cell instances
                        AffineTransform trans = ni.transformOut();
                        Cell protoCell = (Cell)ni.getProto();
                        GeometryHandlerLayerBucket bucket = cellsMap.get(protoCell);
                        if (bucket != null) // it is null when the layer was not found in the subcell
                        local.addAll(bucket.local, trans);
//                        if (protoCell.getId().numUsagesOf() <= 1) // used only here
//                        {
//                            System.out.println("Here");
//                            cellsMap.put(protoCell, null);
//                        }
                    }
                    local.postProcess(true);
                }
                else
                {
                    assert(false); // It should not happen
                }
            }
        }

        public boolean enterCell(HierarchyEnumerator.CellInfo info)
        {
//            if (job != null && job.checkAbort()) return false;
            Cell cell = info.getCell();
            Set<String> set = cellLayersCon.getLayersSet(cell);
//            assert(set != null);

            // The cell doesn't contain the layer
            if (set != null && !set.contains(theLayer.getName()))
            {
//                System.out.println("Cell " + cell.getName() + " doesn't have layer " + theLayer.getName());
                return false;
            }

            GeometryHandlerLayerBucket bucket = cellsMap.get(cell);
            if (bucket == null)
            {
                bucket = new GeometryHandlerLayerBucket();
                cellsMap.put(cell, bucket);
            }
            else
            {
                assert(bucket.merged);
                return false; // done with this cell
            }

            for(Iterator<ArcInst> it = info.getCell().getArcs(); it.hasNext(); )
            {
                ArcInst ai = it.next();
                Network aNet = info.getNetlist().getNetwork(ai, 0);

                // aNet is null if ArcProto is Artwork
                if (aNet == null)
                    continue;
                ArcProto ap = ai.getProto();
                boolean notFound = !arcsList.contains(ap);

                if (notFound)
                    continue; // primitive doesn't contain this layer

                Technology tech = ap.getTechnology();
                Poly[] arcInstPolyList = tech.getShapeOfArc(ai, thisLayerFunction);
                int tot = arcInstPolyList.length;

                for(int i=0; i<tot; i++)
                {
                    Poly poly = arcInstPolyList[i];
                    addElementLocal(poly, theLayer, bucket);
                }
            }

            return true;
        }

        private void addElementLocal(Poly poly, Layer layer, GeometryHandlerLayerBucket bucket)
        {
            bucket.local.add(layer, poly);
        }

        public void exitCell(HierarchyEnumerator.CellInfo info)
        {
            Cell cell = info.getCell();
            boolean isTopCell = cell == topCell;
            GeometryHandlerLayerBucket bucket = cellsMap.get(cell);

            bucket.mergeGeometry(cell);

            if (isTopCell)
            {
                for (Layer layer : bucket.local.getKeySet())
                {
                    checkMinAreaLayerWithTree(bucket.local, topCell, layer);
                }
            }
        }

        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
        {

//            if (job != null && job.checkAbort()) return false;
            // Facet or special elements
			NodeInst ni = no.getNodeInst();
            if (NodeInst.isSpecialNode(ni)) return (false);
			NodeProto np = ni.getProto();
            GeometryHandlerLayerBucket bucket = cellsMap.get(info.getCell());

            // Cells
            if (ni.isCellInstance()) return (true);

			AffineTransform trans = ni.rotateOut();
            PrimitiveNode pNp = (PrimitiveNode)np;
            boolean notFound = !nodesList.contains(pNp);
            if (notFound) return false; // pNp doesn't have the layer

            Technology tech = pNp.getTechnology();

            // Don't get electric layers in case of transistors otherwise it is hard to detect ports
            Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, false, true, thisLayerFunction);
			int tot = nodeInstPolyList.length;

            for(int i=0; i<tot; i++)
			{
				Poly poly = nodeInstPolyList[i];

                poly.roundPoints(); // Trying to avoid mismatches while joining areas.
                poly.transform(trans);
                addElementLocal(poly, theLayer, bucket);
            }

            return true;
        }

        private int checkMinAreaLayerWithTree(GeometryHandler merge, Cell cell, Layer layer)
        {
            // Layer doesn't have min areae
            if (minAreaRule == null && enclosedAreaRule == null && spacingRule == null) return 0;

            Collection<PolyBase.PolyBaseTree> trees = merge.getTreeObjects(layer);
            GenMath.MutableInteger errorFound = new GenMath.MutableInteger(0);

            if (trees.isEmpty())
                System.out.println("Nothing for layer " + layer.getName() + " found.");

            for (PolyBase.PolyBaseTree obj : trees)
            {
                traversePolyTree(layer, obj, 0, cell, errorFound);
            }
            return errorFound.intValue();
        }

        private void traversePolyTree(Layer layer, PolyBase.PolyBaseTree obj, int level,
                                      Cell cell, GenMath.MutableInteger count)
        {
            List<PolyBase.PolyBaseTree> sons = obj.getSons();
            for (PolyBase.PolyBaseTree son : sons)
            {
                traversePolyTree(layer, son, level+1, cell, count);
            }
            boolean minAreaCheck = level%2 == 0;
            boolean checkMin = false, checkNotch = false;
            DRC.DRCErrorType errorType = DRC.DRCErrorType.MINAREAERROR;
            double minVal = 0;
            String ruleName = "";

            if (minAreaCheck)
            {
                if (minAreaRule == null) return; // no rule
                minVal = minAreaRule.getValue(0);
                ruleName = minAreaRule.ruleName;
                checkMin = true;
            }
            else
            {
                // odd level checks enclose area and holes (spacing rule)
                errorType = DRC.DRCErrorType.ENCLOSEDAREAERROR;
                if (enclosedAreaRule != null)
                {
                    minVal = enclosedAreaRule.getValue(0);
                    ruleName = enclosedAreaRule.ruleName;
                    checkMin = true;
                }
                checkNotch = (spacingRule != null);
            }
            PolyBase poly = obj.getPoly();

            if (checkMin)
            {
                double area = poly.getArea();
                // isGreaterThan doesn't consider equals condition therefore negate condition is used
                if (!DBMath.isGreaterThan(minVal, area)) return; // larger than the min value
                count.increment();
                DRC.createDRCErrorLogger(errorLogger, null, DRC.DRCCheckMode.ERROR_CHECK_DEFAULT, true,
                        errorType, null, cell, minVal, area, ruleName,
                        poly, null, layer, null, null, null);
            }
            if (checkNotch)
            {
                // Notches are calculated using the bounding box of the polygon -> this is an approximation
                Rectangle2D bnd = poly.getBounds2D();
                if (bnd.getWidth() < spacingRule.getValue(0))
                {
                    count.increment();
                    DRC.createDRCErrorLogger(errorLogger, null, DRC.DRCCheckMode.ERROR_CHECK_DEFAULT, true,
                            DRC.DRCErrorType.NOTCHERROR, "(X axis)", cell, spacingRule.getValue(0), bnd.getWidth(),
                            spacingRule.ruleName, poly, null, layer, null, null, layer);
                }
                if (bnd.getHeight() < spacingRule.getValue(1))
                {
                    count.increment();
                    DRC.createDRCErrorLogger(errorLogger, null, DRC.DRCCheckMode.ERROR_CHECK_DEFAULT, true,
                            DRC.DRCErrorType.NOTCHERROR, "(Y axis)", cell, spacingRule.getValue(1), bnd.getHeight(),
                            spacingRule.ruleName, poly, null, layer, null, null, layer);
                }
            }
        }
    }
}
