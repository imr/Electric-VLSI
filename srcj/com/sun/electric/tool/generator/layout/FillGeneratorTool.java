package com.sun.electric.tool.generator.layout;

import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.geometry.*;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.extract.LayerCoverageTool;
import com.sun.electric.tool.routing.InteractiveRouter;
import com.sun.electric.tool.routing.SimpleWirer;
import com.sun.electric.tool.routing.Route;
import com.sun.electric.tool.routing.Router;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.technology.*;
import com.sun.electric.technology.technologies.Generic;

import java.util.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: May 8, 2006
 * Time: 10:04:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class FillGeneratorTool extends Tool {
    /** the fill generator tool. */								protected static FillGeneratorTool tool = new FillGeneratorTool();

    private FillGeneratorTool()
	{
		super("Fill Generator");
	}

    /****************************** PREFERENCES ******************************/

    private static Pref cacheFillCell = Pref.makeBooleanPref("FillCell", tool.prefs, true);
    /**
     * Method to tell whether FillGenerator will fill a given cell instead of create fill templates.
     * The default is "true".
     * @return true if FillGenerator should fill a given cell instead of create fill templates.
     */
    public static boolean isFillCell() { return cacheFillCell.getBoolean(); }
    /**
     * Method to set whether FillGenerator will fill a given cell instead of create fill templates.
     * @param on true if FillGenerator should fill a given cell instead of create fill templates.
     */
    public static void setFillCell(boolean on) { cacheFillCell.setBoolean(on); }

    public enum FillCellMode
    {
        NONE(-1),
        FLAT(0),
        BINARY(1),
        ADAPTIVE(2);
        private final int mode;
        FillCellMode(int m) { mode = m; }
        static FillCellMode find(int mode)
        {
            for (FillCellMode m : FillCellMode.values())
            {
                if (m.mode == mode) return m;
            }
            return NONE;
        }
    };
    private static Pref cacheFillCellMode = Pref.makeIntPref("FillCellMode", tool.prefs, FillCellMode.FLAT.mode);
    /**
     * Method to retrieve mode when a given cell must filled.
     * The default is FLAT.
     * @return value representing the algorithm to use for filling a given cell.
     */
    public static FillCellMode getFillCellMode() { return FillCellMode.find(cacheFillCellMode.getInt()); }
    /**
     * Method to set mode when a given cell must filled.
     * @param mode value representing the algorithm to use for filling a given cell.
     */
    public static void setFillCellMode(FillCellMode mode) { cacheFillCellMode.setInt(mode.mode); }

    private static Pref cacheFillCellCreateMaster = Pref.makeBooleanPref("FillCellCreateMaster", tool.prefs, true);
    /**
     * Method to tell whether FillGenerator will generate a master cell or use a given one.
     * The default is "true".
     * @return true if FillGenerator should generate a master cell instead of use a given one.
     */
    public static boolean isFillCellCreateMasterOn() { return cacheFillCellCreateMaster.getBoolean(); }
    /**
     * Method to set whether FillGenerator will generate a master cell or use a given one.
     * @param on true if FillGenerator should generate a master cell instead of use a given one.
     */
    public static void setFillCellCreateMasterOn(boolean on) { cacheFillCellCreateMaster.setBoolean(on); }

    /****************************** JOB ******************************/

    public static class FillGenJob extends Job
    {
        private FillGenerator.FillGeneratorConfig fillGenConfig;
        private Cell topCell;
        private ErrorLogger log;
        private boolean doItNow;

		public FillGenJob(Cell cell, FillGenerator.FillGeneratorConfig gen, boolean doItNow)
		{
			super("Fill generator job", null, Type.CHANGE, null, null, Priority.USER);
            this.fillGenConfig = gen;
            this.topCell = cell; // Only if 1 cell is generated.
            this.doItNow = doItNow;
                                                  assert(fillGenConfig.evenLayersHorizontal);
            if (doItNow) // call from regressions
            {
                try
                {
                    if (doIt())
                        terminateOK();

                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            else
			    startJob(); // queue the job
		}

        public ErrorLogger getLogger() { return log; }

        public Library getAutoFilLibrary()
        {
            return Library.findLibrary(fillGenConfig.fillLibName);
        }

        /**
         * Method to obtain the PrimitiveNode layer holding this export. It travels along hierarchy
         * until reaches the PrimitiveNode leaf containing the export. Only metal layers are selected.
         * @param ex
         * @return Non pseudo layer for the given export
         */
        public static Layer getMetalLayerFromExport(PortProto ex)
        {
            PortProto po = ex;

            if (ex instanceof Export)
            {
                PortInst pi = ((Export)ex).getOriginalPort();
                po = pi.getPortProto();
            }
            if (po instanceof Export)
                return getMetalLayerFromExport((Export)po);
            if (po instanceof PrimitivePort)
            {
                PrimitivePort pp = (PrimitivePort)po;
                PrimitiveNode node = pp.getParent();
                // Search for at least m2
                for (Iterator<Layer> it = node.getLayerIterator(); it.hasNext();)
                {
                    Layer layer = it.next();
                    Layer.Function func = layer.getFunction();
                    // Exclude metal1
                    if (func.isMetal() && func != Layer.Function.METAL1)
                        return layer.getNonPseudoLayer();
                }
            }
            return null;
        }

        private List<FillGenerator.PortConfig> searchPortList()
        {
           // Searching common power/gnd connections and skip the ones are in the same network
            // Don't change List by Set otherwise the sequence given by Set is not deterministic and hard to debug
            List<PortInst> portList = new ArrayList<PortInst>();
            Netlist topCellNetlist = topCell.acquireUserNetlist();
            List<Export> exportList = new ArrayList<Export>();

            for (Iterator<NodeInst> it = topCell.getNodes(); it.hasNext(); )
            {
                NodeInst ni = it.next();

                if (!ni.isCellInstance())
                {
//                    for (Iterator<PortInst> itP = ni.getPortInsts(); itP.hasNext(); )
//                    {
//                        PortInst p = itP.next();
//
//                        if (!p.getPortProto().isGround() && !p.getPortProto().isPower())
//                            continue;
//                        // Simple case
//                        portList.add(p);
//                    }
                    for (Iterator<Export> itE = ni.getExports(); itE.hasNext(); )
                    {
                        Export e = itE.next();
                        if (!e.isGround() && !e.isPower())
                            continue;
                        portList.add(e.getOriginalPort());
                        exportList.add(e);
                    }
                }
                else
                {
                    Cell cell = (Cell)ni.getProto();
                    Netlist netlist = cell.acquireUserNetlist();
                    List<PortInst> list = new ArrayList<PortInst>();
                    List<Network> nets = new ArrayList<Network>();
                    List<Export> eList = new ArrayList<Export>();

                    for (Iterator<PortInst> itP = ni.getPortInsts(); itP.hasNext(); )
                    {
                        PortInst p = itP.next();

                        if (!p.getPortProto().isGround() && !p.getPortProto().isPower())
                            continue;
                        // If subcell has two exports on the same network, it assumes they are connected inside
                        // and therefore only one of them is checked
                        assert(p.getPortProto() instanceof Export);
                        Export ex = (Export)p.getPortProto();
                        Network net = netlist.getNetwork(ex.getOriginalPort());
                        Network topNet = topCellNetlist.getNetwork(p);
                        Cell fillCell = null;

                        // search for possible existing fill already defined
                        for (Iterator<Nodable> itN = topNet.getNetlist().getNodables(); itN.hasNext(); )
                        {
                            Nodable no = itN.next();
                            if (ni == no) continue; // skip itself
                            if (!no.isCellInstance()) continue; // skip any flat PrimitiveNode?
                            Cell c = (Cell)no.getProto();
                            if (c == p.getNodeInst().getProto()) // skip port parent
                                continue;
                            if (c.getName().indexOf("fill") == -1) continue; // not a fill cell
                            fillCell = c;
                            break;
                        }
                        // if fillCell is not null -> cover by a fill cell
                        if (fillCell == null && !nets.contains(net))
                        {
                            list.add(p);
                            nets.add(net);
                            eList.add(ex);
                        }
                        else
                            System.out.println("Skipping export " + p + " in " + ni);
                    }
                    portList.addAll(list);
                    exportList.addAll(eList);
                }
            }

            // searching for exclusion regions. If port is inside these regions, then it will be removed.
            // Search them in a chunk of ports. It should be faster
//            ObjectQTree tree = new ObjectQTree(topCell.getBounds());
//            List<Rectangle2D> searchBoxes = new ArrayList<Rectangle2D>(); // list of AFG boxes to use.
//
//            for (Iterator<NodeInst> it = topCell.getNodes(); it.hasNext(); )
//            {
//                NodeInst ni = it.next();
//                NodeProto np = ni.getProto();
//                if (np == Generic.tech.afgNode)
//                    searchBoxes.add(ni.getBounds());
//            }
//            if (searchBoxes.size() > 0)
//            {
//                for (PortInst p : portList)
//                {
//                    tree.add(p, p.getBounds());
//                }
//                for (Rectangle2D rect : searchBoxes)
//                {
//                    Set set = tree.find(rect);
//                    portList.removeAll(set);
//                }
//            }
            List<FillGenerator.PortConfig> plList = new ArrayList<FillGenerator.PortConfig>();

            assert(portList.size() == exportList.size());

            for (int i = 0; i < exportList.size(); i++)
            {
                PortInst p = portList.get(i);

                Layer l = FillGenJob.getMetalLayerFromExport(p.getPortProto());
                // Checking that pin is on metal port
                if (l != null)
                    plList.add(new FillGenerator.PortConfig(p, exportList.get(i), l));
            }
//            for (PortInst p : portList)
//            {
//                plList.add(new PortConfig(p, getMetalLayerFromExport(p.getPortProto())));
//            }
            return plList;
        }

        private Cell searchPossibleMaster()
        {
            for (Iterator<Library> it = Library.getLibraries(); it.hasNext();)
            {
                Library lib = it.next();
                for (Iterator<Cell> itC = lib.getCells(); itC.hasNext();)
                {
                    Cell c = itC.next();
                    if (c.getVar("FILL_MASTER") != null)
                        return c;
                }
            }

            return null;
        }

        public boolean doIt()
        {
            FillGenerator fillGen = new FillGenerator(fillGenConfig);

            // logger must be created in server otherwise it won't return the elements.
            log = ErrorLogger.newInstance("Fill");
            if (!doItNow)
                fieldVariableChanged("log");

            if (topCell == null)
                doTemplateFill(fillGen);
            else
                doFillOnCell(fillGen);
            return true;
        }

        public void doTemplateFill(FillGenerator fillGen)
        {
            fillGen.standardMakeFillCell(fillGenConfig.firstLayer, fillGenConfig.lastLayer, fillGenConfig.perim,
                    fillGenConfig.cellTiles, false);
            fillGen.makeGallery();
        }

		public void doFillOnCell(FillGenerator fillGen)
		{
            // Searching for possible master
            Cell master = (fillGen.config.useMaster) ? searchPossibleMaster() : null;

            // Creating fills only for layers found in exports
//            firstMetal = Integer.MAX_VALUE;
//            lastMetal = Integer.MIN_VALUE;
            List<FillGenerator.PortConfig> portList = searchPortList();

//            for (PortConfig p : portList)
//            {
//                int index = p.l.getIndex() + 1;
//                if (index < firstMetal) firstMetal = index;
////                if (lastMetal < index) lastMetal = index;
//            }
//            if (firstMetal <= 2) firstMetal = 3;
//            if (lastMetal < firstMetal) lastMetal = firstMetal;

            // otherwise pins at edges increase cell sizes and FillRouter.connectCoincident(portInsts)
            // does work
//            G.DEF_SIZE = 0;
            List<Rectangle2D> topBoxList = new ArrayList<Rectangle2D>();
            topBoxList.add(topCell.getBounds()); // topBox might change if predefined pitch is included in master cell

            Area exclusionArea = new Area();
            for (Iterator<NodeInst> it = topCell.getNodes(); it.hasNext(); )
            {
                NodeInst ni = it.next();
                NodeProto np = ni.getProto();
                // Creates fill only arounds the top cells
                if (np == Generic.tech.afgNode || fillGenConfig.onlyAround && ni.isCellInstance())
                    exclusionArea.add(new Area(ni.getBounds()));

            }

            Cell fillCell = (fillGenConfig.hierarchy) ?
                    fillGen.treeMakeFillCell(fillGenConfig.firstLayer, fillGenConfig.lastLayer, fillGenConfig.perim,
                            topCell, master, topBoxList, exclusionArea, fillGenConfig.drcSpacingRule, fillGenConfig.binary) :
                    fillGen.standardMakeFillCell(fillGenConfig.firstLayer, fillGenConfig.lastLayer, fillGenConfig.perim,
                            fillGenConfig.cellTiles, true);

            if (topCell == null || portList == null || portList.size() == 0) return;

            Cell connectionCell = Cell.newInstance(topCell.getLibrary(), topCell.getName()+"fill{lay}");

            Rectangle2D fillBnd = fillCell.getBounds();
            double essentialX = fillBnd.getWidth()/2;
            double essentialY = fillBnd.getHeight()/2;
            LayoutLib.newNodeInst(Tech.essentialBounds,
                      -essentialX, -essentialY,
                      G.DEF_SIZE, G.DEF_SIZE, 180, connectionCell);
		    LayoutLib.newNodeInst(Tech.essentialBounds,
                      essentialX, essentialY,
                      G.DEF_SIZE, G.DEF_SIZE, 0, connectionCell);

            // Adding the connection cell into topCell
            assert(topBoxList.size() == 1);
            Rectangle2D bnd = topBoxList.get(0);
            NodeInst conNi = LayoutLib.newNodeInst(connectionCell, bnd.getCenterX(), bnd.getCenterY(),
                    fillBnd.getWidth(), fillBnd.getHeight(), 0, topCell);

            // Adding the fill cell into connectionCell
            Rectangle2D conBnd = connectionCell.getBounds();
            NodeInst fillNi = LayoutLib.newNodeInst(fillCell, conBnd.getCenterX() - fillBnd.getWidth()/2 - fillBnd.getX(),
                    conBnd.getCenterY() - fillBnd.getHeight()/2 - fillBnd.getY(),
                    fillBnd.getWidth(), fillBnd.getHeight(), 0, connectionCell);

            AffineTransform conTransOut = conNi.transformOut();
            AffineTransform fillTransOutToCon = fillNi.transformOut(); // Don't want to calculate transformation to top
            AffineTransform fillTransIn = fillNi.transformIn(conNi.transformIn());

            InteractiveRouter router  = new SimpleWirer();
            boolean rotated = (master != null && master.getVar("ROTATED_MASTER") != null);
            FillGenJobContainer container = new FillGenJobContainer(router, fillCell, fillNi, connectionCell, conNi,
                    fillGenConfig.drcSpacingRule, rotated);

            // Checking if any arc in FillCell collides with rest of the cells
            if (!fillGenConfig.hierarchy)
            {
                AffineTransform fillTransOut = fillNi.transformOut(conTransOut);
                removeOverlappingBars(container, fillTransOut);
            }

            // Export all fillCell exports in connectCell before extra exports are added into fillCell
            for (Iterator<Export> it = container.fillCell.getExports(); it.hasNext();)
            {
                Export export = it.next();
                PortInst p = container.fillNi.findPortInstFromProto(export);
                Export e = Export.newInstance(container.connectionCell, p, p.getPortProto().getName());
		        e.setCharacteristic(p.getPortProto().getCharacteristic());
            }

            // First attempt if ports are below a power/ground bars
            for (FillGenerator.PortConfig p : portList)
            {
                Rectangle2D rect = null;
                // Transformation of the cell instance containing this port
                AffineTransform trans = null; // null if the port is on the top cell

                if (p.p.getPortProto() instanceof Export)
                {
                    Export ex = (Export)p.p.getPortProto();
                    assert(ex == p.e);
                    Cell exportCell = (Cell)p.p.getNodeInst().getProto();
                    // Supposed to work only with metal layers
                    rect = LayerCoverageTool.getGeometryOnNetwork(exportCell, ex.getOriginalPort(),
                            p.l.getNonPseudoLayer());
                    trans = p.p.getNodeInst().transformOut();
                }
                else // port on pins
                    rect = (Rectangle2D)p.p.getNodeInst().getBounds().clone(); // just to be cloned due to changes inside function

                // Looking to detect any possible contact based on overlap between this geometry and fill
                Rectangle2D backupRect = (Rectangle2D)rect.clone();
                NodeInst added = addAllPossibleContacts(container, p, rect, trans, fillTransIn, fillTransOutToCon,
                        conTransOut, exclusionArea);
                List<PolyBase> polyList = new ArrayList<PolyBase>();
                List<Geometric> gList = new ArrayList<Geometric>();
                polyList.add(p.pPoly);
                gList.add(p.p.getNodeInst());
                if (added != null)
                {
                    log.logWarning(p.p.describe(false) + " connected", gList, null, null, null, polyList, topCell, 0);
                    continue;
                }
                else
                {
//                    log.logError(p.p.describe(false) + " not connected", gList, null, null, null, polyList, topCell, 0);
                }

                // Trying the closest arc
                rect = backupRect;
                rect = p.pPoly.getBounds2D();
                double searchWidth = fillGen.master.getBounds().getWidth();
                rect = new Rectangle2D.Double(rect.getX()-searchWidth/2, rect.getY(), rect.getWidth()+searchWidth/2,
                        backupRect.getHeight());
                added = addAllPossibleContacts(container, p, rect,
                        null, //trans,
                        fillTransIn, fillTransOutToCon,
                        conTransOut, null);

                if (added != null)
                {
                    log.logWarning(p.p.describe(false) + " connected by extension", gList, null, null, null, polyList, topCell, 0);
                    continue;
                }
                else
                {
                    log.logError(p.p.describe(false) + " not connected", gList, null, null, null, polyList, topCell, 0);
                }
            }

            // Checking if ports not falling over power/gnd bars can be connected using existing contacts
            // along same X axis
//            PortInst[] ports = new PortInst[portNotReadList.size()];
//            portNotReadList.toArray(ports);
//            portNotReadList.clear();
//            Rectangle2D[] rects = new Rectangle2D[ports.length];
//            bndNotReadList.toArray(rects);
//            bndNotReadList.clear();
//
//            for (int i = 0; i < ports.length; i++)
//            {
//                PortInst p = ports[i];
//                Rectangle2D portBnd = rects[i];
//                NodeInst minNi = connectToExistingContacts(p, portBnd, fillContactList, fillPortInstList);
//
//                if (minNi != null)
//                {
//                    int index = fillContactList.indexOf(minNi);
//                    PortInst fillNiPort = fillPortInstList.get(index);
//                    // Connecting the export in the top cell
//                    Route exportRoute = router.planRoute(topCell, p, fillNiPort,
//                            new Point2D.Double(p.getBounds().getCenterX(), p.getBounds().getCenterY()), null, false);
//                    Router.createRouteNoJob(exportRoute, topCell, true, false, null);
//                }
//                else
//                {
//                    portNotReadList.add(p);
//                    bndNotReadList.add(rects[i]);
//                }
//            }
//
//            // If nothing works, try to insert contacts in location with same Y
//            // Cleaning fillContacts so it doesn't try again with the same sets
//            fillPortInstList.clear();
//            fillContactList.clear();
//            for (int i = 0; i < portNotReadList.size(); i++)
//            {
//                PortInst p = portNotReadList.get(i);
//                Rectangle2D r = bndNotReadList.get(i);
//                double newWid = r.getWidth()+globalWidth;
//                Rectangle2D rect = new Rectangle2D.Double(r.getX()-newWid, r.getY(),
//                        2*newWid, r.getHeight()); // copy the rectangle to add extra width
//
//                // Check possible new contacts added
//                NodeInst minNi = connectToExistingContacts(p, rect, fillContactList, fillPortInstList);
//                if (minNi != null)
//                {
//                    int index = fillContactList.indexOf(minNi);
//                    PortInst fillNiPort = fillPortInstList.get(index);
//                    // Connecting the export in the top cell
//                    Route exportRoute = router.planRoute(topCell, p, fillNiPort,
//                            new Point2D.Double(p.getBounds().getCenterX(), p.getBounds().getCenterY()), null, false);
//                    Router.createRouteNoJob(exportRoute, topCell, true, false, null);
//                }
//                else
//                {
//                    // Searching arcs again
//                    Geometric geom = routeToClosestArc(container, p, rect, 10, fillTransOut);
//                    if (geom == null)
//                    {
//                        ErrorLogger.MessageLog l = log.logError(p.describe(false) + " not connected", topCell, 0);
//                        l.addPoly(p.getPoly(), true, topCell);
//                        if (p.getPortProto() instanceof Export)
//                            l.addExport((Export)p.getPortProto(), true, topCell, null);
//                        l.addGeom(p.getNodeInst(), true, fillCell, null);
//                    }
//                }
//            }
        }

        public void terminateOK()
        {
            log.termLogging(false);
            long endTime = System.currentTimeMillis();
            int errorCount = log.getNumErrors();
            int warnCount = log.getNumWarnings();
            System.out.println(errorCount + " errors and " + warnCount + " warnings found (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
        }

        /**
         * Method to detect which fill nodes are overlapping in the top cell.
         * @param cell
         * @param fillTransUp matrix
         */
        private boolean detectOverlappingBars(Cell cell, AffineTransform fillTransUp, HashSet<Geometric> nodesToRemove,
                                              FillGenJobContainer container)
        {
            List<Layer.Function> tmp = new ArrayList<Layer.Function>();

            // Check if any metalXY must be removed
            for (Iterator<NodeInst> itNode = cell.getNodes(); itNode.hasNext(); )
            {
                NodeInst ni = itNode.next();

                if (NodeInst.isSpecialNode(ni)) continue;

                tmp.clear();
                NodeProto np = ni.getProto();
                if (ni.isCellInstance())
                {
                    Cell subCell = (Cell)ni.getProto();
                    AffineTransform subTransUp = ni.transformOut(fillTransUp);
                    // No need of checking the rest of the elements if first one is detected.
                    if (detectOverlappingBars(subCell, subTransUp, nodesToRemove, container))
                    {
                        if (cell == container.fillCell)
                            nodesToRemove.add(ni);
                        else
                            return true;
                    }
                    continue;
                }
                PrimitiveNode pn = (PrimitiveNode)np;
                if (pn.getFunction() == PrimitiveNode.Function.PIN) continue; // pins have pseudo layers

                for (Technology.NodeLayer tlayer : pn.getLayers())
                {
                    tmp.add(tlayer.getLayer().getFunction());
                }
                Rectangle2D rect = FillGenerator.getSearchRectangle(ni.getBounds(), fillTransUp, container.drcSpacing);
                if (FillGenerator.searchCollision(topCell, rect, tmp, null,
                        new NodeInst[] {container.fillNi, container.connectionNi}, null))
                {
                    // Direct on last top fill cell
                    if (cell == container.fillCell)
                        nodesToRemove.add(ni);
                    else
                        return true; // time to delete parent NodeInst
                }
            }

            // Checking if any arc in FillCell collides with rest of the cells
            for (Iterator<ArcInst> itArc = cell.getArcs(); itArc.hasNext(); )
            {
                ArcInst ai = itArc.next();
                tmp.clear();
                tmp.add(ai.getProto().getLayers()[0].getLayer().getNonPseudoLayer().getFunction());
                // Searching box must reflect DRC constrains
                Rectangle2D rect = FillGenerator.getSearchRectangle(ai.getBounds(), fillTransUp, container.drcSpacing);
                if (FillGenerator.searchCollision(topCell, rect, tmp, null,
                        new NodeInst[] {container.fillNi, container.connectionNi}, null))
                {
                    if (cell == container.fillCell)
                    {
                        nodesToRemove.add(ai);
                        // Remove exports and pins as well
                        nodesToRemove.add(ai.getTail().getPortInst().getNodeInst());
                        nodesToRemove.add(ai.getHead().getPortInst().getNodeInst());
                    }
                    else
                        return true; // time to delete parent NodeInst.
                }
            }
            return false;
        }

        private void removeOverlappingBars(FillGenJobContainer container, AffineTransform fillTransOut)
        {
            // Check if any metalXY must be removed
            HashSet<Geometric> nodesToRemove = new HashSet<Geometric>();

            // This function should replace NodeInsts for temporary cells that don't have elements overlapping
            // the standard fill cells.
            // DRC conditions to detect overlap otherwise too many elements/cells might be discarded.
            detectOverlappingBars(container.fillCell, fillTransOut, nodesToRemove, container);

            for (Geometric geo : nodesToRemove)
            {
                System.out.println("Removing " + geo);
                if (geo instanceof NodeInst)
                    ((NodeInst)geo).kill();
                else
                    ((ArcInst)geo).kill();
            }
        }

//        private NodeInst connectToExistingContacts(PortInst p, Rectangle2D portBnd,
//                                               List<NodeInst> fillContactList, List<PortInst> fillPortInstList)
//        {
//            double minDist = Double.POSITIVE_INFINITY;
//            NodeInst minNi = null;
//
//            for (int j = 0; j < fillContactList.size(); j++)
//            {
//                NodeInst ni = fillContactList.get(j);
//                PortInst fillNiPort = fillPortInstList.get(j);
//                // Checking only the X distance between a placed contact and the port
//                Rectangle2D contBox = ni.getBounds();
//
//                // check if contact is connected to the same grid
//                if (fillNiPort.getPortProto().getCharacteristic() != p.getPortProto().getCharacteristic())
//                    continue; // no match in network type
//
//                // If they are not aligned on Y, discard
//                if (!DBMath.areEquals(contBox.getCenterY(), portBnd.getCenterY())) continue;
//                double pdx = Math.abs(Math.max(contBox.getMinX()-portBnd.getMaxX(), portBnd.getMinX()-contBox.getMaxX()));
//                if (pdx < minDist)
//                {
//                    minNi = ni;
//                    minDist = pdx;
//                }
//            }
//            return minNi;
//        }

        private class FillGenJobContainer
        {
            InteractiveRouter router;
            Cell fillCell, connectionCell;
            NodeInst fillNi, connectionNi;
            List<PortInst> fillPortInstList;
            List<NodeInst> fillContactList;
            double drcSpacing;
            boolean rotated; // tmp fix

            FillGenJobContainer(InteractiveRouter r, Cell fC, NodeInst fNi, Cell cC, NodeInst cNi, double drcSpacing,
                                boolean rotated)
            {
                this.router = r;
                this.fillCell = fC;
                this.fillNi = fNi;
                this.connectionCell = cC;
                this.connectionNi = cNi;
                this.fillPortInstList = new ArrayList<PortInst>();
                this.fillContactList = new ArrayList<NodeInst>();
                this.drcSpacing = drcSpacing;
                this.rotated = rotated;
            }
        }

        /**
         * THIS METHOD ASSUMES contactAreaOrig is horizontal!
         */
        /**
         * Method to search for all overlaps with metal bars in the fill
         * @param searchCell
         * @param rotated true if original fill cell is rotated. This should be a temporary fix.
         * @param handler structure containing elements that overlap with the given contactAreaOrig
         * @param closestHandler structure containing elements that are close to the given contactAreaOrig
         * @param p
         * @param contactAreaOrig
         * @param downTrans
         * @param upTrans
         * @return
         */
        private boolean searchOverlapHierarchically(Cell searchCell, boolean rotated,
                                                    GeometryHandler handler, GeometryHandler closestHandler,
                                                    FillGenerator.PortConfig p, Rectangle2D contactAreaOrig,
                                                    AffineTransform downTrans, AffineTransform upTrans)
        {
            Rectangle2D contactArea = (Rectangle2D)contactAreaOrig.clone();
            DBMath.transformRect(contactArea, downTrans);
            Netlist fillNetlist = searchCell.acquireUserNetlist();
            double contactAreaHeight = contactArea.getHeight();
            double contactAreaWidth = contactArea.getWidth();
            // Give high priority to lower arcs
            HashMap<Layer, List<ArcInst>> protoMap = new HashMap<Layer, List<ArcInst>>();
            boolean noIntermediateCells = false;

            for (Iterator<Geometric> it = searchCell.searchIterator(contactArea); it.hasNext(); )
            {
                // Check if there is a contact on that place already!
                Geometric geom = it.next();

                if (geom instanceof NodeInst)
                {
                    NodeInst ni = (NodeInst)geom;
                    if (!ni.isCellInstance()) continue;

                    AffineTransform fillIn = ni.transformIn();
                    AffineTransform fillUp = ni.transformOut(upTrans);
                    // In case of being a cell
                    if (searchOverlapHierarchically((Cell)ni.getProto(), rotated,
                            handler, closestHandler, p, contactArea, fillIn, fillUp))
                        noIntermediateCells = true;
                    continue;
                }

                ArcInst ai = (ArcInst)geom;
                ArcProto ap = ai.getProto();
                Network arcNet = fillNetlist.getNetwork(ai, 0);

                // No export with the same characteristic found in this netlist
                if (arcNet.findExportWithSameCharacteristic(p.e) == null)
                    continue; // no match in network type

                if (ap == Tech.m2 || ap == Tech.m1 || !ap.getFunction().isMetal())
                    continue;
                
//                if (ap != Tech.m3)
//                {
//                    System.out.println("picking  metal");
//                    continue; // Only metal 3 arcs
//                }

                // Adding now
                Layer layer = ap.getLayerIterator().next();
                List<ArcInst> list = protoMap.get(layer);
                if (list == null)
                {
                    list = new ArrayList<ArcInst>();
                    protoMap.put(layer, list);
                }
                list.add(ai);
            }

            if (noIntermediateCells)
                return true; // done already down in the hierarchy

            // Assign priority to lower metal bars. eg m3 instead of m4
            Set<Layer> results = protoMap.keySet();
            List<Layer> listOfLayers = new ArrayList<Layer>(results.size());
            listOfLayers.addAll(results);
            Collections.sort(listOfLayers, Layer.layerSortByName);
            double closestDist = Double.POSITIVE_INFINITY;
            Rectangle2D closestRect = null;

            // Give priority to port layer (p.l)
            int index = listOfLayers.indexOf(p.l);
            if (index > -1)
            {
                Layer first = listOfLayers.get(0);
                listOfLayers.set(0, p.l);
                listOfLayers.set(index, first);
            }

            // Now select possible pins
            for (Layer layer : listOfLayers)
            {
                ArcProto ap = findArcProtoFromLayer(layer);
                boolean horizontalBar = (rotated) ? (ap == Tech.m3 || ap == Tech.m5) : (ap == Tech.m4 || ap == Tech.m6);
                PrimitiveNode defaultContact = null;

                if (horizontalBar)
                {
//                    continue;
                    if ((!rotated && ap == Tech.m4) || (rotated && ap == Tech.m3))
                        defaultContact = Tech.m3m4;
                    else if ((!rotated && ap == Tech.m6) || (rotated && ap == Tech.m5))
                        defaultContact = Tech.m4m5;
                    else
                        assert(false);
                }

                boolean found = false;

                Layer theLayer = null;
                for (ArcInst ai : protoMap.get(layer))
                {
                    Rectangle2D geomBnd = ai.getBounds();
                    theLayer = layer;

                    // Add only the piece that overlap. If more than 1 arc covers the same area -> only 1 contact
                    // will be added.
                    Rectangle2D newElem = null;
                    double usefulBar, newElemMin, newElemMax, areaMin, areaMax, geoMin, geoMax;

                    if (horizontalBar)
                    {
                        if (layer != p.l) continue; // only when they match in layer so the same contacts can be used
                        usefulBar = geomBnd.getHeight();
                        // search for the contacts m3m4 at least
                        Network net = fillNetlist.getNetwork(ai, 0);
                        List<NodeInst> nodes = new ArrayList<NodeInst>();

                        // get contact nodes in the same network
                        for (Iterator<NodeInst> it = searchCell.getNodes(); it.hasNext(); )
                        {
                            NodeInst ni = it.next();
                            Rectangle2D r = ni.getBounds();
                            // only contacts
                            if (ni.getProto().getFunction() != PrimitiveNode.Function.CONTACT) continue;
                            // Only those that overlap 100% with the contact otherwise it would add zig-zag extra metals
                            if (!r.intersects(geomBnd)) continue;
                            for (Iterator<PortInst> pit = ni.getPortInsts(); pit.hasNext(); ) {
                                PortInst pi = pit.next();
                                // Only those
                                if (fillNetlist.getNetwork(pi) == net)
                                {
                                    nodes.add(ni);
                                    break; // stop the loop here
                                }
                            }
                        }
                        // No contact on that bar
                        if (nodes.size() == 0)
                            newElem = new Rectangle2D.Double(contactArea.getX(), geomBnd.getY(), defaultContact.getDefWidth(), usefulBar);
                        else
                        {
                            // better if find a vertical bar by closest distance
//                            continue;
                            // search for closest distance or I could add all!!
                            // Taking the first element for now
                            NodeInst ni = nodes.get(0);
                            // Check lower layer of the contact so it won't add unnecessary contacts
                            PrimitiveNode np = (PrimitiveNode)ni.getProto();
//                            layerTmpList.clear();
//                            for (Iterator<Layer> it = np.getLayerIterator(); it.hasNext(); )
//                            {
//                                Layer l = it.next();
//                                if (l.getFunction().isMetal())
//                                    layerTmpList.add(l);
//                            }
//                            Collections.sort(layerTmpList, Layer.layerSortByName);
//                            theLayer = layerTmpList.get(0);
                            Rectangle2D r = ni.getBounds();
                            double contactW = ni.getXSizeWithoutOffset();
                            double contactH = ni.getYSizeWithoutOffset();
                            r = new Rectangle2D.Double(r.getCenterX()-contactW/2, contactArea.getY(),
                                    contactW, contactAreaHeight);
                            geomBnd = r;
                            newElem = geomBnd;
                        }
                        newElemMin = newElem.getMinY();
                        newElemMax = newElem.getMaxY();
                        areaMin = contactArea.getMinY();
                        areaMax = contactArea.getMaxY();
                        geoMin = geomBnd.getMinY();
                        geoMax = geomBnd.getMaxY();
                    }
                    else
                    {
                        if (rotated)
                        {
                            usefulBar = geomBnd.getHeight();
                            newElem = new Rectangle2D.Double(contactArea.getX(), geomBnd.getY(), contactAreaWidth, usefulBar);
                            newElemMin = newElem.getMinY();
                            newElemMax = newElem.getMaxY();
                            areaMin = contactArea.getMinY();
                            areaMax = contactArea.getMaxY();
                            geoMin = geomBnd.getMinY();
                            geoMax = geomBnd.getMaxY();
                        }
                        else
                        {
                            usefulBar = geomBnd.getWidth();
                            newElem = new Rectangle2D.Double(geomBnd.getX(), contactArea.getY(), usefulBar, contactAreaHeight);
                            newElemMin = newElem.getMinX();
                            newElemMax = newElem.getMaxX();
                            areaMin = contactArea.getMinX();
                            areaMax = contactArea.getMaxX();
                            geoMin = geomBnd.getMinX();
                            geoMax = geomBnd.getMaxX();
                        }
                    }

                    // Don't consider no overlapping areas
                    if (newElemMax < areaMin || areaMax < newElemMin)
                        continue;
                    boolean containMin = newElemMin <= areaMin && areaMin <= newElemMax;
                    boolean containMax = newElemMin <= areaMax && areaMax <= newElemMax;

                    // Either end is not contained otherwise the contact is fully contained by the arc
                    if (!containMin || !containMax)
                    {
                        // Getting the intersection along X/Y axis. Along YX it should cover completely
                        assert(geoMin == newElemMin);
                        assert(geoMax == newElemMax);

                        double min = Math.max(geoMin, areaMin);
                        double max = Math.min(geoMax, areaMax);
                        double diff = max-min;
                        double overlap = (diff)/usefulBar;
                        // Checking if new element is completely inside the contactArea otherwise routeToClosestArc could add
                        // the missing contact
                        if (overlap < fillGenConfig.minOverlap)
                        {
                            System.out.println("Not enough overlap (" + overlap + ") in " + ai + " to cover " + p.p);
                            double val = Math.abs(diff);
                            if (closestDist > val) // only in this case the elements are close enough but not touching
                            {
                                closestDist = val;
                                closestRect = newElem;
                            }
                            continue;
                        }
                    }
                    // Transforming geometry up to fillCell coordinates
                    DBMath.transformRect(newElem, upTrans);
                    // Adding element
                    handler.add(theLayer, newElem);
                    found = true;
                }
                if (found)
                    return true; // only one set for now if something overlapping was found

                if (horizontalBar || closestRect == null) continue;
                // trying with closest vertical arcs
                // Transforming geometry up to fillCell coordinates
                DBMath.transformRect(closestRect, upTrans);
                // Adding element
                closestHandler.add(theLayer, closestRect);
//                return true;
            }
            return false;
        }

        /**
         * Method to find corresponding metal pin associated to the given layer
         * @param layer
         * @return
         */
        private static PrimitiveNode findPrimitiveNodeFromLayer(Layer layer)
        {
            for (PrimitiveNode pin : VddGndStraps.PINS)
            {
                if (pin != null && layer == pin.getLayerIterator().next().getNonPseudoLayer())
                {
                    return pin; // found
                }
            }
            return null;
        }

        /**
         * Method to find corresponding metal arc associated to the given layer
         * @param layer
         * @return
         */
        private static ArcProto findArcProtoFromLayer(Layer layer)
        {
            for (ArcProto arc : VddGndStraps.METALS)
            {
                if (arc != null && layer == arc.getLayerIterator().next().getNonPseudoLayer())
                {
                    return arc; // found
                }
            }
            return null;
        }

        /**
         * Method to add all possible contacts in connection cell based on the overlap of a given metal2 area
         * and fill cell.
         * THIS ONLY WORK if first fill bar is vertical
         * @param container
         * @param p
         * @param contactArea
         * @param nodeTransOut null if the port is on the top cell
         * @param fillTransOutToCon
         * @return
         */
        private NodeInst addAllPossibleContacts(FillGenJobContainer container, FillGenerator.PortConfig p, Rectangle2D contactArea,
                                                AffineTransform nodeTransOut, AffineTransform fillTransIn, AffineTransform fillTransOutToCon,
                                                AffineTransform conTransOut, Area exclusionArea)
        {
            // Until this point, contactArea is at the fillCell level
            // Contact area will contain the remaining are to check
            double contactAreaHeight = contactArea.getHeight();

            NodeInst added = null;
            // Transforming rectangle with gnd/power metal into the connection cell
            if (nodeTransOut != null)
                DBMath.transformRect(contactArea, nodeTransOut);
            if (exclusionArea != null && exclusionArea.intersects(contactArea))
                return null; // can't connect here.

            GeometryHandler overlapHandler = GeometryHandler.createGeometryHandler(GeometryHandler.GHMode.ALGO_SWEEP, 1);
            GeometryHandler closestHandler = GeometryHandler.createGeometryHandler(GeometryHandler.GHMode.ALGO_SWEEP, 1);
            GeometryHandler handler;

            searchOverlapHierarchically(container.fillCell, container.rotated, overlapHandler, closestHandler, p,
                    contactArea, fillTransIn, GenMath.MATID);
            handler = overlapHandler;
            handler.postProcess(false);
            closestHandler.postProcess(false);

            Set<Layer> overlapResults = handler.getKeySet();
            Set<Layer> closestResults = closestHandler.getKeySet();

            List<Layer> listOfLayers = new ArrayList<Layer>(overlapResults.size());
            listOfLayers.addAll(overlapResults);
            listOfLayers.addAll(closestResults);
            Collections.sort(listOfLayers, Layer.layerSortByName);

            int size = listOfLayers.size();

//            assert(size <= 1); // Must contain only m3

            if (size == 0) return null;

            Rectangle2D portInConFill = new Rectangle2D.Double();
            portInConFill.setRect(p.pPoly.getBounds2D());
            DBMath.transformRect(portInConFill, fillTransIn);

            // Creating the corresponding export in connectionNi (projection pin)
            // This should be done only once!
            PrimitiveNode thePin = findPrimitiveNodeFromLayer(p.l);
            assert(thePin != null);
            assert(thePin != Tech.m1pin); // should start from m2
            NodeInst pinNode = null;
            PortInst pin = null;

            // Loop along all possible connections (different layers)
            // from both GeometricHandler structures.
            for (Layer layer : listOfLayers)
            {
                if (!layer.getFunction().isMetal()) continue;  // in case of active arcs!
                Collection set = handler.getObjects(layer, false, true);

                if (set == null || set.size() == 0) // information from closestHandling
                    set = closestHandler.getObjects(layer, false, true);

                assert (set != null && set.size() > 0);

                // Get connecting metal contact (PrimitiveNode) starting from techPin up to the power/vdd bar found
                List<Layer.Function> fillLayers = new ArrayList<Layer.Function>();
                PrimitiveNode topPin = findPrimitiveNodeFromLayer(layer);
                PrimitiveNode topContact = null;
                int start = -1;
                int end = -1;
                for (int i = 0; i < VddGndStraps.PINS.length; i++)
                {
                    if (start == -1 && VddGndStraps.PINS[i] == thePin)
                        start = i;
                    if (end == -1 && VddGndStraps.PINS[i] == topPin)
                        end = i;
                }
                if (start > end)
                {
                    int tmp = start;
                    start = end;
                    end = tmp;
                }
                for (int i = start; i <= end; i++)
                {
                    fillLayers.add(VddGndStraps.PINS[i].getLayerIterator().next().getFunction());
                    if (i < end)
                        topContact = VddGndStraps.fillContacts[i];
                }

//                assert(topContact != null);
                boolean horizontalBar = (container.rotated) ? (topPin == Tech.m3pin || topPin == Tech.m5pin) : (topPin == Tech.m4pin || topPin == Tech.m6pin);

                for (Iterator it = set.iterator(); it.hasNext(); )
                {
                    // ALGO_SWEEP retrieves only PolyBase
                    PolyBase poly = (PolyBase)it.next();
                    Rectangle2D newElemFill = poly.getBounds2D();
                    double newElemFillWidth = newElemFill.getWidth();
                    double newElemFillHeight = newElemFill.getHeight();

                    // Location of new element in fillCell
                    Rectangle2D newElemConnect = (Rectangle2D)newElemFill.clone();
                    DBMath.transformRect(newElemConnect, fillTransOutToCon);

                    // Location of new contact from top cell
                    Rectangle2D newElemTop = (Rectangle2D)newElemConnect.clone();
                    DBMath.transformRect(newElemTop, conTransOut);

                    // Get connecting metal contact (PrimitiveNode) starting from techPin up to the power/vdd bar found
                    // Search if there is a collision with existing nodes/arcs
                    if (FillGenerator.searchCollision(topCell, newElemTop, fillLayers, p,
                            new NodeInst[] {container.fillNi, container.connectionNi}, null))
                        continue;

                    // The first time but only after at least one element can be placed
                    if (pinNode == null)
                    {
                        pinNode = LayoutLib.newNodeInst(thePin, portInConFill.getCenterX(), portInConFill.getCenterY(),
                                thePin.getDefWidth(), contactAreaHeight, 0, container.connectionCell);
                        pin = pinNode.getOnlyPortInst();
                    }

                    if (topContact != null)
                    {
                        // adding contact
                        // center if the overlapping was found by just overlapping of the vertical metal bar
//                        boolean center = horizontalBar || newElemConnect.getCenterY() != contactArea.getCenterY();
                        added = horizontalBar ?
                                LayoutLib.newNodeInst(topContact, newElemConnect.getCenterX(), newElemConnect.getCenterY(),
                                newElemFillWidth, newElemFillHeight, 0, container.connectionCell) :
                                LayoutLib.newNodeInst(topContact, newElemConnect.getCenterX(), newElemConnect.getCenterY(),
                                newElemFillWidth, contactAreaHeight, 0, container.connectionCell);
                    }
                    else // on the same layer as thePin
                       added = pinNode;

                    container.fillContactList.add(added);

                    // route new pin instance in connectioNi with new contact
                    Route pinExportRoute = container.router.planRoute(container.connectionCell, pin, added.getOnlyPortInst(),
                            new Point2D.Double(portInConFill.getCenterX(), portInConFill.getCenterY()), null, false);
                    Router.createRouteNoJob(pinExportRoute, container.connectionCell, true, false);

                    // It was removed by the vertical router
                    if (!pin.isLinked())
                    {
                        pinNode = pinExportRoute.getStart().getNodeInst();
                        pin = pinExportRoute.getStart().getPortInst();
                    }

                    // Adding the connection to the fill via the exports.
                    // Looking for closest export in fillCell.
                    PortInst fillNiPort = null;
                    double minDistance = Double.POSITIVE_INFINITY;

                    for (Iterator<Export> e = container.fillNi.getExports(); e.hasNext();)
                    {
                        Export exp = e.next();
                        PortInst port = exp.getOriginalPort();

                        // The port characteristics must be identical
                        if (port.getPortProto().getCharacteristic() != p.e.getCharacteristic())
                            continue;

                        Rectangle2D geo = port.getPoly().getBounds2D();
                        assert(fillGenConfig.evenLayersHorizontal);
                        double deltaX = geo.getCenterX() - newElemConnect.getCenterX();
                        double deltaY = geo.getCenterY() - newElemConnect.getCenterY();

                        boolean condition = (horizontalBar) ?
                                DBMath.isInBetween(geo.getCenterY(), newElemConnect.getMinY(), newElemConnect.getMaxY()) :
                                DBMath.isInBetween(geo.getCenterX(), newElemConnect.getMinX(), newElemConnect.getMaxX());
                        boolean cond = (horizontalBar) ?
                                DBMath.areEquals(deltaY, 0) :
                                DBMath.areEquals(deltaX, 0);
                        if (cond != condition)
                            System.out.println("Here");
                        if (!condition)
                            continue; // only align with this so it could guarantee correct arc (M3)
                        double dist = Math.sqrt(deltaX*deltaX + deltaY*deltaY);
                        if (DBMath.isGreaterThan(minDistance, dist))
                        {
                            minDistance = dist;
                            fillNiPort = port;
                        }
                    }
                    if (fillNiPort != null)
                    {
//                        Rectangle2D r = fillNiPort.getBounds();
                        EPoint center = fillNiPort.getCenter();
                        Route exportRoute = container.router.planRoute(container.connectionCell, added.getOnlyPortInst(),
                                fillNiPort,
                                center,
//                                new Point2D.Double(r.getCenterX(), r.getCenterY()),
                                null, false);
                        Router.createRouteNoJob(exportRoute, container.connectionCell, true, false);
                    }
                }

                // Done at the end so extra connections would not produce collisions.
                // Routing the new contact to topCell in connectNi instead of top cell
                // Export connect projected pin in ConnectionCell
                if (pinNode != null) // at least done for one
                {
                    Export pinExport = Export.newInstance(container.connectionCell, pin, "proj-"+p.e.getName());
                    assert(pinExport != null);
                    pinExport.setCharacteristic(p.e.getCharacteristic());
                    // Connect projected pin in ConnectionCell with real port
                    PortInst pinPort = container.connectionNi.findPortInstFromProto(pinExport);
                    Route conTopExportRoute = container.router.planRoute(topCell, p.p, pinPort,
                            new Point2D.Double(p.pPoly.getBounds2D().getCenterX(), p.pPoly.getBounds2D().getCenterY()), null, false);
                    Router.createRouteNoJob(conTopExportRoute, topCell, true, false);

                    return added;
                }
            }
            return null;
        }
    }
}
