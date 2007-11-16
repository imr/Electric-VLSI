package com.sun.electric.tool.drc;

import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.Job;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.geometry.*;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.technology.*;

import java.util.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: Nov 15, 2007
 * Time: 2:18:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class MultiDRCToolJob extends Job {

    private Layer theLayer;
    private Cell topCell;
    private DRCTemplate minAreaRule, enclosedAreaRule, spacingRule;
    // Must be static so it could be called in constructor
    private static String getJobName(Cell c, Layer l) { return "Design-Rule Check " + c + ", layer " + l.getName(); }

    MultiDRCToolJob(Cell c, Layer l, DRCTemplate minAreaRule, DRCTemplate enclosedAreaRule, DRCTemplate spaceRule)
    {
        super(getJobName(c, l), DRC.getDRCTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
        this.topCell = c;
        this.theLayer = l;
        this.minAreaRule = minAreaRule;
        this.enclosedAreaRule = enclosedAreaRule;
        this.spacingRule = spaceRule;
        startJob();
    }

    public boolean doIt()
    {
        long startTime = System.currentTimeMillis();
        ErrorLogger errorLog = DRC.getDRCErrorLogger(true, false);

    System.out.println("DRC for Cell " + topCell.getName() + " , layer " + theLayer.getName());
    HierarchyEnumerator.Visitor quickArea = new LayerAreaEnumerator(GeometryHandler.GHMode.ALGO_SWEEP);
    HierarchyEnumerator.enumerateCell(topCell, VarContext.globalContext, quickArea);

        long endTime = System.currentTimeMillis();
        int errorCount = errorLog.getNumErrors();
        int warnCount = errorLog.getNumWarnings();
        System.out.println(errorCount + " errors and " + warnCount + " warnings found (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");

        return true;
    }

    public static void checkDesignRules(Cell cell)
    {
        // Check if there are DRC rules for particular tech
        Technology tech = cell.getTechnology();
		DRCRules rules = DRC.getRules(tech);
        // Nothing to check for this particular technology
		if (rules == null || rules.getNumberOfRules() == 0)
                return;

        // Multi thread the DRC algorithms per layer
        for (Iterator<Layer> it = cell.getTechnology().getLayers(); it.hasNext();)
        {
            Layer layer = it.next();
            DRCTemplate minAreaRule = DRC.getMinValue(layer, DRCTemplate.DRCRuleType.MINAREA);
            DRCTemplate enclosedAreaRule = DRC.getMinValue(layer, DRCTemplate.DRCRuleType.MINENCLOSEDAREA);
            DRCTemplate spaceRule = DRC.getSpacingRule(layer, null, layer, null, true, -1, -1.0, -1.0); // UCONSPA, CONSPA or SPACING
            if (minAreaRule == null && enclosedAreaRule == null && spaceRule == null)
                continue; // nothing to check
            new MultiDRCToolJob(cell, layer, minAreaRule, enclosedAreaRule, spaceRule);
        }
    }

    /**************************************************************************************************************
	 *  QuickAreaEnumeratorLocal class
	 **************************************************************************************************************/
    /**
     * Class that uses local GeometryHandler to calculate the area per cell
     */
    private class LayerAreaEnumerator extends HierarchyEnumerator.Visitor
    {
        private Map<Cell, GeometryHandlerLayerBucket> cellsMap;
        private Layer.Function.Set thisLayerFunction;
//        private Cell topCell;
//        private Layer theLayer;
        private GeometryHandler.GHMode mode;

        LayerAreaEnumerator(GeometryHandler.GHMode mode)
        {
            // gate poly must be merged with the rest of the poly in transistors
            this.thisLayerFunction = (theLayer.getFunction().isPoly()) ?
                    new Layer.Function.Set(theLayer.getFunction(), Layer.Function.GATE) :
                    new Layer.Function.Set(theLayer.getFunction());
            this.mode = mode;
            cellsMap = new HashMap<Cell, GeometryHandlerLayerBucket>();
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
                    System.out.println("Merging Cell " + cell.getName());
                    merged = true;
                    for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext();)
                    {
                        NodeInst ni = it.next();
                        if (!ni.isCellInstance()) continue; // only cell instances
                        AffineTransform trans = ni.transformOut();
                        Cell protoCell = (Cell)ni.getProto();
                        GeometryHandlerLayerBucket bucket = cellsMap.get(protoCell);
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
                {
                    continue;
                }
                Technology tech = ai.getProto().getTechnology();
                Poly[] arcInstPolyList = tech.getShapeOfArc(ai, thisLayerFunction);
                int tot = arcInstPolyList.length;
                assert(tot == 1);
                for(int i=0; i<tot; i++)
                {
                    Poly poly = arcInstPolyList[i];
//                    Layer layer = poly.getLayer();
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
            AffineTransform root = info.getTransformToRoot();
//			if (root.getType() != AffineTransform.TYPE_IDENTITY)
//				trans.preConcatenate(root);

            PrimitiveNode pNp = (PrimitiveNode)np;
			Technology tech = pNp.getTechnology();

            // Don't get electric layers in case of transistors otherwise it is hard to detect ports
            Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, false, true, thisLayerFunction);
			int tot = nodeInstPolyList.length;
			for(int i=0; i<tot; i++)
			{
				Poly poly = nodeInstPolyList[i];
//				Layer layer = poly.getLayer();

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

            boolean minAreaDone = true;
            boolean enclosedAreaDone = true;
            GenMath.MutableInteger errorFound = new GenMath.MutableInteger(0);

            for (PolyBase.PolyBaseTree obj : trees)
            {
                traversePolyTree(layer, obj, 0, minAreaRule, enclosedAreaRule, spacingRule, cell, errorFound);
            }
            return errorFound.intValue();
        }

            private void traversePolyTree(Layer layer, PolyBase.PolyBaseTree obj, int level, DRCTemplate minAreaRule,
                                          DRCTemplate encloseAreaRule, DRCTemplate spacingRule, Cell cell, GenMath.MutableInteger count)
    {
        List<PolyBase.PolyBaseTree> sons = obj.getSons();
        for (PolyBase.PolyBaseTree son : sons)
        {
            traversePolyTree(layer, son, level+1, minAreaRule, encloseAreaRule, spacingRule, cell, count);
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
            if (encloseAreaRule != null)
            {
                minVal = encloseAreaRule.getValue(0);
                ruleName = encloseAreaRule.ruleName;
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
            System.out.println("Min or Enclusion error");
//            reportError(errorType, null, cell, minVal, area, ruleName,
//                            poly, null, layer, null, null, null);
        }
        if (checkNotch)
        {
            // Notches are calculated using the bounding box of the polygon -> this is an approximation
            Rectangle2D bnd = poly.getBounds2D();
            if (bnd.getWidth() < spacingRule.getValue(0))
            {
                count.increment();
                System.out.println("Hole error X");
//                reportError(DRCErrorType.NOTCHERROR, "(X axis)", cell, spacingRule.getValue(0), bnd.getWidth(),
//                        spacingRule.ruleName, poly, null, layer, null, null, layer);
            }
            if (bnd.getHeight() < spacingRule.getValue(1))
            {
                count.increment();
                System.out.println("Hole error Y");
//                reportError(DRCErrorType.NOTCHERROR, "(Y axis)", cell, spacingRule.getValue(1), bnd.getHeight(),
//                        spacingRule.ruleName, poly, null, layer, null, null, layer);
            }
        }
    }
    }
}
