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

import com.sun.electric.technology.*;
import com.sun.electric.tool.Consumer;
import com.sun.electric.tool.Job;
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
import java.awt.geom.Area;

/**
 * User: gg151869
 * Date: Dec 11, 2007
 */

public class MTDRCAreaTool extends MTDRCTool
{
//    /** Miscellanous data for DRC */                            private DRC.ReportInfo reportInfo;

    public MTDRCAreaTool(DRC.DRCPreferences dp, Cell c, Consumer<MTDRCResult> consumer)
    {
        super("Design-Rule Area Check " + c, dp, c, consumer);
    }

    @Override
    boolean checkArea() {return true;}

    static int checkMinArea(Layer theLayer, Cell topC, DRC.ReportInfo rI, DRCRules areaRules,
                            CellLayersContainer cellLayersC, Job job, String msg)
    {
        DRCTemplate minAreaRule = areaRules.getMinValue(theLayer, DRCTemplate.DRCRuleType.MINAREA);
        DRCTemplate enclosedAreaRule = areaRules.getMinValue(theLayer, DRCTemplate.DRCRuleType.MINENCLOSEDAREA);
        DRCTemplate spaceRule = areaRules.getSpacingRule(theLayer, null, theLayer, null, true, -1, -1.0, -1.0); // UCONSPA, CONSPA or SPACING
        if (minAreaRule == null && enclosedAreaRule == null && spaceRule == null)
            return 0;

        if (msg != null)
        {
            System.out.println("Min Area DRC for " + msg + " in thread " + Thread.currentThread().getName());
        }
        HierarchyEnumerator.Visitor quickArea = new LayerAreaEnumerator(theLayer, minAreaRule, enclosedAreaRule,
            spaceRule, topC, rI, GeometryHandler.GHMode.ALGO_SWEEP,
                cellLayersC, job);
        HierarchyEnumerator.enumerateCell(topC, VarContext.globalContext, quickArea);
        return rI.errorLogger.getNumErrors() + rI.errorLogger.getNumWarnings();
    }

    @Override
    public MTDRCResult runTaskInternal(Layer theLayer)
    {
        ErrorLogger errorLogger = DRC.getDRCErrorLogger(true, ", Layer " + theLayer.getName());
        String msg = "Cell " + topCell.getName() + " , layer " + theLayer.getName();
        DRC.ReportInfo reportInfo = new DRC.ReportInfo(errorLogger, topCell.getTechnology(), dp, false);

        Date lastAreaGoodDate = DRC.getLastDRCDateBasedOnBits(topCell, false, -1, !reportInfo.inMemory);

        if (DRC.isCellDRCDateGood(topCell, lastAreaGoodDate))
            System.out.println("The cell seems to be MinArea OK. Should I run the code?");

        int totalNumErrors = checkMinArea(theLayer, topCell, reportInfo, rules, cellLayersCon, this, msg);
        HashSet<Cell> goodAreaDRCDate = new HashSet<Cell>();
        HashSet<Cell> cleanAreaDRCDate = new HashSet<Cell>();

        long endTime = System.currentTimeMillis();
        int errorCount = errorLogger.getNumErrors();
        int warnCount = errorLogger.getNumWarnings();
        System.out.println(errorCount + " errors and " + warnCount + " warnings found in " + msg
                + " (took " + TextUtils.getElapsedTime(endTime - startTime) + " in thread " + Thread.currentThread().getName() + ")");
        long accuEndTime = System.currentTimeMillis() - globalStartTime;
        System.out.println("Accumulative time " + TextUtils.getElapsedTime(accuEndTime));
        if (totalNumErrors == 0)
        {
            goodAreaDRCDate.add(topCell);
        }
        else
        {
            cleanAreaDRCDate.add(topCell);
            errorLogger.termLogging(true);
        }
        return new MTDRCResult(errorCount, warnCount, !checkAbort(), new HashSet<Cell>(), new HashSet<Cell>(),
            goodAreaDRCDate, cleanAreaDRCDate, null);
    }

    /**************************************************************************************************************
	 *  LayerAreaEnumerator class
	 **************************************************************************************************************/

    public static class GeometryHandlerLayerBucket
    {
        GeometryHandler local;
        boolean merged = false;

        GeometryHandlerLayerBucket(GeometryHandler.GHMode mode)
        {
            local = GeometryHandler.createGeometryHandler(mode, 1);
        }

        void addElementLocal(Poly poly, Layer layer)
        {
            local.add(layer, poly);
        }

        void mergeGeometry(Cell cell, Map<Cell, GeometryHandlerLayerBucket> cellsMap)
        {
            if (!merged)
            {
                merged = true;
                for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext();)
                {
                    NodeInst ni = it.next();
                    if (!ni.isCellInstance()) continue; // only cell instances
                    AffineTransform trans = ni.transformOut();
                    Cell protoCell = (Cell) ni.getProto();
                    GeometryHandlerLayerBucket bucket = cellsMap.get(protoCell);
                    if (bucket != null) // it is null when the layer was not found in the subcell
                        local.addAll(bucket.local, trans);
                }
                local.postProcess(true);
            } else
            {
                assert (false); // It should not happen
            }
        }
    }

    /**************************************************************************************************************
	 *  LayerAreaEnumerator class
	 **************************************************************************************************************/
    /**
     * Class that uses local GeometryHandler to calculate the area per cell
     */
    private static class LayerAreaEnumerator extends HierarchyEnumerator.Visitor
    {
        private Layer theLayer;
        private Job job;
        private Map<Cell,GeometryHandlerLayerBucket> cellsMap;
        private Layer.Function.Set thisLayerFunction;
        private GeometryHandler.GHMode mode;
        private Collection<PrimitiveNode> nodesList; // NodeProto that contains this layer
        private Collection<ArcProto> arcsList; // ArcProto that contains this layer
        private CellLayersContainer cellLayersCon;
        private DRCTemplate minAreaRule, enclosedAreaRule, spacingRule, minAreaEnclosedRule;
//        private ErrorLogger errorLogger;
        private Cell topCell;
        private DRC.ReportInfo reportInfo;

        LayerAreaEnumerator(Layer layer, DRCTemplate minAreaR, DRCTemplate enclosedAreaR, DRCTemplate spaceR,
                            Cell topC, DRC.ReportInfo rI,
                            GeometryHandler.GHMode m, CellLayersContainer cellLayersC, Job j)
        {
            // This is required so the poly arcs will be properly merged with the transistor polys
            // even though non-electrical layers are retrieved
            this.minAreaRule = minAreaR;
            this.enclosedAreaRule = enclosedAreaR;
            this.spacingRule = spaceR;
            this.topCell = topC;
            this.reportInfo = rI;
            this.theLayer = layer;
            this.thisLayerFunction = DRC.getMultiLayersSet(theLayer);
            this.mode = m;
            cellsMap = new HashMap<Cell,GeometryHandlerLayerBucket>();
            nodesList = new ArrayList<PrimitiveNode>();
            this.cellLayersCon = cellLayersC;
            this.job = j;

            // Store the min value betwen min area and min enclosure to speed up the process
            minAreaEnclosedRule = minAreaRule;
            if (enclosedAreaRule != null &&
                (minAreaRule == null || enclosedAreaRule.getValue(0) < minAreaRule.getValue(0)))
                minAreaEnclosedRule = enclosedAreaRule;

            // determine list of PrimitiveNodes that have this particular layer
            for (PrimitiveNode node : topCell.getTechnology().getNodesCollection())
            {
                for (Technology.NodeLayer nLayer : node.getNodeLayers())
                {
                    if (thisLayerFunction.contains(nLayer.getLayer().getFunction(), theLayer.getFunctionExtras()))
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

        public boolean enterCell(HierarchyEnumerator.CellInfo info)
        {
            // Checking if the job was aborted by the user
            if (job.checkAbort()) return false;

            Cell cell = info.getCell();
            Set<String> set = cellLayersCon.getLayersSet(cell);

            // The cell doesn't contain the layer
            if (set != null && !set.contains(theLayer.getName()))
            {
                return false;
            }

            GeometryHandlerLayerBucket bucket = cellsMap.get(cell);
            if (bucket == null)
            {
                bucket = new GeometryHandlerLayerBucket(mode);
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
                    bucket.addElementLocal(poly, theLayer);
                }
            }

            return true;
        }

        public void exitCell(HierarchyEnumerator.CellInfo info)
        {
            Cell cell = info.getCell();
            boolean isTopCell = cell == topCell;
            GeometryHandlerLayerBucket bucket = cellsMap.get(cell);

            bucket.mergeGeometry(cell, cellsMap);

            if (isTopCell)
            {
                for (Layer layer : bucket.local.getKeySet())
                {
//                    int oldV = checkMinAreaLayerWithTree(bucket.local, topCell, layer);
                    checkMinAreaLayerWithLoops(bucket.local, topCell, layer);
                }
            }
        }

        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
        {
            // Checking if the job was aborted
            if (job.checkAbort()) return false;

            // Facet or special elements
			NodeInst ni = no.getNodeInst();
            if (NodeInst.isSpecialNode(ni)) return (false);

            // Cells
            if (ni.isCellInstance()) return (true);

			NodeProto np = ni.getProto();
            if (!np.getTechnology().isLayout()) return (false); // only layout nodes

            GeometryHandlerLayerBucket bucket = cellsMap.get(info.getCell());
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
                bucket.addElementLocal(poly, theLayer);
            }

            return true;
        }

        private int checkMinAreaLayerWithLoops(GeometryHandler merge, Cell cell, Layer layer)
        {
            // Layer doesn't have min area
            if (minAreaRule == null && enclosedAreaRule == null && spacingRule == null) return 0;
            PolySweepMerge m = (PolySweepMerge)merge; // easier to implement for now.
            List<Area> areas = m.getAreas(layer);
            GenMath.MutableInteger errorFound = new GenMath.MutableInteger(0);

            // It could be multi-threaded per area
            for (Area area : areas)
            {
                List<PolyBase> list = PolyBase.getLoopsFromArea(area, layer);
                boolean minPass = true;

                for (PolyBase p : list)
                {
                    double a = p.getArea();
                    minPass = (minAreaEnclosedRule == null) ? true : a >= minAreaEnclosedRule.getValue(0);

                    if (!minPass) break; // go for full checking
                    if (spacingRule != null)
                    {
                        Rectangle2D bnd = p.getBounds2D();
                        minPass = bnd.getWidth() >= spacingRule.getValue(0);
                        if (minPass)
                           minPass = bnd.getHeight() >= spacingRule.getValue(1);
                    }
                    if (!minPass) break;  // go for full checking
                }

                // Must run the real checking
                if (!minPass)
                {
                    List<PolyBase.PolyBaseTree> roots = PolyBase.getTreesFromLoops(list);

                    for (PolyBase.PolyBaseTree obj : roots)
                    {
                        traversePolyTree(layer, obj, 0, cell, errorFound);
                    }
                }
            }

            return errorFound.intValue();
        }

//        private int checkMinAreaLayerWithTree(GeometryHandler merge, Cell cell, Layer layer)
//        {
//            // Layer doesn't have min area
//            if (minAreaRule == null && enclosedAreaRule == null && spacingRule == null) return 0;
//
//            Collection<PolyBase.PolyBaseTree> trees = merge.getTreeObjects(layer);
//            GenMath.MutableInteger errorFound = new GenMath.MutableInteger(0);
//
//            for (PolyBase.PolyBaseTree obj : trees)
//            {
//                traversePolyTree(layer, obj, 0, cell, errorFound);
//            }
//            return errorFound.intValue();
//        }

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
                DRC.createDRCErrorLogger(reportInfo,
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
                    DRC.createDRCErrorLogger(reportInfo,
                            DRC.DRCErrorType.NOTCHERROR, "(X axis)", cell, spacingRule.getValue(0), bnd.getWidth(),
                            spacingRule.ruleName, poly, null, layer, null, null, layer);
                }
                if (bnd.getHeight() < spacingRule.getValue(1))
                {
                    count.increment();
                    DRC.createDRCErrorLogger(reportInfo,
                            DRC.DRCErrorType.NOTCHERROR, "(Y axis)", cell, spacingRule.getValue(1), bnd.getHeight(),
                            spacingRule.ruleName, poly, null, layer, null, null, layer);
                }
            }
        }
    }
}
