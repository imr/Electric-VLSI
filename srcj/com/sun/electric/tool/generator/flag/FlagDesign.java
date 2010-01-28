package com.sun.electric.tool.generator.flag;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.tool.generator.flag.router.Router;
import com.sun.electric.tool.generator.flag.router.SogRouterAdapter;
import com.sun.electric.tool.generator.flag.router.ToConnect;
import com.sun.electric.tool.generator.flag.scan.Scan;
import com.sun.electric.tool.generator.layout.AbutRouter;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.TechType;
import com.sun.electric.tool.generator.layout.LayoutLib.Corner;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.routing.SeaOfGates;
import com.sun.electric.tool.user.ExportChanges;


/** Super class for the physical design objects of all Cells */
public class FlagDesign {
    public static final double DEF_SIZE = LayoutLib.DEF_SIZE;
	private final FlagConfig config;
	private final Scan scan;
	private final Router router;
	private final SogRouterAdapter sogRouterAdapter;

	public TechType tech() {return config.techTypeEnum.getTechType();}
    
	
//	public static Rectangle2D findColBounds(Collection<NodeInst> stages) {
//		double minX, minY, maxX, maxY;
//		minX = minY = Double.MAX_VALUE;
//		maxX = maxY = Double.MIN_VALUE;
//		for (NodeInst ni : stages) {
//			Rectangle2D bounds = ni.findEssentialBounds();
//			error(bounds==null, 
//					        "Layout Cell is missing essential bounds: "+
//					        ni.getProto().describe(false));
//			minX = Math.min(minX, bounds.getMinX());
//			maxX = Math.max(maxX, bounds.getMaxX());
//			minY = Math.min(minY, bounds.getMinY());
//			maxY = Math.max(maxY, bounds.getMaxY());
//		}
//		return new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
//	}
//	private void sortPortInstsBotToTop(List<PortInst> pis) {
//		Collections.sort(pis, new Comparator<PortInst>() {
//			public int compare(PortInst p1, PortInst p2) {
//				double diff = p1.getCenter().getY() -
//				              p2.getCenter().getY();
//				return (int) Math.signum(diff);
//			}
//		});
//	}
//	private Rectangle2D getPortInstBounds(ToConnect toConn) {
//		double minX, minY, maxX, maxY;
//		minX = minY = Double.MAX_VALUE;
//		maxX = maxY = Double.MIN_VALUE;
//		for (PortInst pi : toConn.getPortInsts()) {
//			double x = pi.getCenter().getX();
//			double y = pi.getCenter().getY();
//			if (x<minX) minX=x;
//			if (x>maxX) maxX=x;
//			if (y<minY) minY=y;
//			if (y>maxY) maxY=y;
//		}
//		return new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
//	}
	
//	private void connect2PinM3(ToConnect toConn) {
//		prln("M3 "+toConn.toString());
//		PortInst pi1 = toConn.getPortInsts().get(0);
//		PortInst pi2 = toConn.getPortInsts().get(1);
//		error(!connectsToM3(pi1) || ! connectsToM3(pi2),
//				        "only 1 of 2 pins connects to m3");
//		double x1 = pi1.getCenter().getX();
//		double x2 = pi2.getCenter().getX();
//		error(x1!=x2, "m3 net not vertical");
//		
//		LayoutLib.newArcInst(tech().m3(), SIGNAL_WID, pi1, pi2);
//		
//	}
	
//	private void dumpChannels(LayerChannels m2chan, LayerChannels m3chan) {
//        prln("m2 channels");
//        prln(m2chan.toString());
//        
//        prln("m3 channels");
//        prln(m3chan.toString());
//	}
//	private List<ToConnect> reduceToTwoPinX(List<List<PortInst>> portLists) {
//		List<ToConnect> twoPins = new ArrayList<ToConnect>();
//		while (portLists.size()>1) {
//			ClosestClusters cc = findClosest(portLists);
//			ToConnect tc = new ToConnect(null);
//			tc.addPortInst(cc.pair.p1);
//			tc.addPortInst(cc.pair.p2);
//			twoPins.add(tc);
//			List<PortInst> pl1 = portLists.get(cc.ndx1);
//			List<PortInst> pl2 = portLists.get(cc.ndx2);
//			pl1.addAll(pl2);
//			portLists.remove(cc.ndx2);
//		}
//		return twoPins;
//	}
	

	/** Convert each ToConnect with more than two pins to multiple two 
	 * pin TwoConnects */ 
//	private List<ToConnect> reduceToTwoPin(List<ToConnect> toConns) {
//		List<ToConnect> twoPins = new ArrayList<ToConnect>();
//		for (ToConnect tc : toConns) {
//			// Skip Exported net that touches no stage PortInsts 
//			if (tc.size()==0) continue;
//			
//			// Some PortInsts on a ToConnect may already be connected in 
//			// schematic by abut router
//			List<List<PortInst>> connPorts = groupConnectedPorts(tc);
//
//			// Generate a list of two pin ToConnects that connects  
//			// disconnected pin lists.
//			twoPins.addAll(reduceToTwoPinX(connPorts));
//		}
//		return twoPins;
//	}
	
	
	private void fattenVerticalM3(Map<Double, PortInst> bot,
			                      Map<Double, PortInst> top) {
		for (Double x : bot.keySet()) {
			PortInst piBot = bot.get(x);
			PortInst piTop = top.get(x);
			if (piTop==null) continue;
			LayoutLib.newArcInst(tech().m3(), config.m3PwrGndWid, 
		                         piBot, piTop);
		}
	}
	
	// Hack until I get a chance to fatten based upon need.
//	private void fattenPwrGnd(List<NodeInst> stages) {
//		Rectangle2D colBounds = findBounds(stages.get(0).getParent());
//		NodeInst topInst = stages.get(stages.size()-1);
//		Map<Double, PortInst> topPwr = new TreeMap<Double, PortInst>();
//		Map<Double, PortInst> topGnd = new TreeMap<Double, PortInst>();
//		getM3PwrGndExports(topPwr, topGnd, topInst, colBounds.getMaxY());
//		
//		NodeInst botInst = stages.get(0);
//		Map<Double, PortInst> botPwr = new TreeMap<Double, PortInst>();
//		Map<Double, PortInst> botGnd = new TreeMap<Double, PortInst>();
//		getM3PwrGndExports(botPwr, botGnd, botInst, colBounds.getMinY());
//
//		fattenVerticalM3(botGnd, topGnd);
//		fattenVerticalM3(botPwr, topPwr);
//	}
	
	
	private static class CloseToBound implements Comparator<PortInst> {
		private Rectangle2D bound;
		private double distToBound(PortInst pi) {
			double x = pi.getCenter().getX();
			double l = Math.abs(x - bound.getMinX());
			double r = Math.abs(x - bound.getMaxX());
			double y = pi.getCenter().getY();
			double b = Math.abs(y - bound.getMinY());
			double t = Math.abs(y - bound.getMaxY());
			return Math.min(Math.min(l, r), 
					        Math.min(t, b)); 
		}
		public CloseToBound(Rectangle2D bound) {this.bound=bound;}
		public int compare(PortInst pi1, PortInst pi2) {
			double d = distToBound(pi1) - distToBound(pi2);
			return (int) Math.signum(d);
		}
	}
	
	
	
	private void reExportIfPortNameMatches(List<String> expNms,
			                               List<PortInst> ports) {
		if (ports.isEmpty()) return;
		Cell parent = ports.get(0).getNodeInst().getParent();
		for (Iterator<String> sIt=expNms.iterator(); sIt.hasNext();) {
			String nm = sIt.next();
			if (parent.findExport(nm)!=null) {
				sIt.remove();
			} else {
				
			}
		}
		
	}
	/** If PortInst name matches Export name then export PortInst and
	 * remove PortInst and export name from respective lists. */
	private void exportPortInstsWithMatchingNames(List<String> expNames,
			                                      List<PortInst> ports) {
		for (Iterator<String> sIt=expNames.iterator(); sIt.hasNext();) {
			String expNm = sIt.next();
			for (Iterator<PortInst> pIt=ports.iterator(); pIt.hasNext();) {
				PortInst pi = pIt.next();
				String portNm = pi.getPortProto().getName();
				if (expNm.equals(portNm)) {
					Export.newInstance(pi.getNodeInst().getParent(), 
					                   pi, expNm);
					sIt.remove();
					pIt.remove();
					break;
				}
			}
		}
	}
	
	private void exportTheRest(List<String> expNames, List<PortInst> ports) {
		for (int i=0; i<expNames.size(); i++) {
			String expNm = expNames.get(i);
			if (i>=ports.size()) {
				prln("Error: Schematic export: "+expNm+
					  " couldn't be added to layout");
				continue;
			}
			PortInst pi = ports.get(i);
			Export.newInstance(pi.getNodeInst().getParent(), 
	                           pi, expNm);
		}
	}
	
	/** Re-export all ports that the schematic exports. Don't do power
	 * and ground because they are handled by another method. 
	 * There are three criteria for selecting which PortInst to export. First, 
	 * prefer a PortInst with the same as the export. Second, prefer a PortInst
	 * without connections. Third, prefer a PortInst that is close to the cell
	 * boundary. */
	private void reExport(Cell layCell, List<ToConnect> toConns) {
		Rectangle2D colBounds = Utils.findBounds(layCell);
		CloseToBound closeToBound = new CloseToBound(colBounds);
		for (ToConnect tc : toConns) {
			if (tc.numPortInsts()>0 && tc.isExported() && !tc.isPowerOrGround()) {
				// separate out connected and not connected PortInsts
				// Because PortInst.hasConnections() is so slow, make sure
				// we do this test once. Keeping two different lists accomplishes
				// this.
				List<PortInst> unconnPorts = new ArrayList<PortInst>();
				List<PortInst> connPorts = new ArrayList<PortInst>();
				for (PortInst pi : tc.getPortInsts()) {
					if (pi.hasConnections()) connPorts.add(pi);
					else  unconnPorts.add(pi);
				}
				Collections.sort(connPorts, closeToBound);
				Collections.sort(unconnPorts, closeToBound);

				// allPorts sorted by following criterea in order of importance:
				// 1) no arcs connected
				// 2) distance to boundary
				List<PortInst> allPorts = new ArrayList<PortInst>();
				allPorts.addAll(unconnPorts);
				allPorts.addAll(connPorts);
				
				List<String> expNames = new ArrayList<String>();
				expNames.addAll(tc.getExportName());
				exportPortInstsWithMatchingNames(expNames, allPorts);
				
				exportTheRest(expNames, allPorts);
			}
		}
	}
	
	
	private static class CompareLayInstSchPos implements Comparator<NodeInst> {
		Map<NodeInst, SchematicPosition> layInstToSchPos;
		public int compare(NodeInst ni1, NodeInst ni2) {
			SchematicPosition sp1, sp2;
			sp1 = layInstToSchPos.get(ni1);
			sp2 = layInstToSchPos.get(ni2);
			return sp1.compareTo(sp2);
		}
		public CompareLayInstSchPos(Map<NodeInst, SchematicPosition> layInstToSchPos) {
			this.layInstToSchPos = layInstToSchPos;
		}
	}
	// sort layout instances according to X-coordinate in schematic
	private List<NodeInst> getSortedLayInsts(SchematicVisitor visitor) {
        List<NodeInst> layInsts = 
        	new ArrayList<NodeInst>(visitor.getLayInsts());
        Map<NodeInst, SchematicPosition> layInstSchPos = 
        	visitor.getLayInstSchematicPositions();
        
        CompareLayInstSchPos compareLayInstSchPos = 
        	new CompareLayInstSchPos(layInstSchPos);
        
        Collections.sort(layInsts, compareLayInstSchPos);
        return layInsts;
	}

	private Map<Cell,Integer> getCellCounts(List<NodeInst> layInsts) {
		Map<Cell,Integer> cellToCount = new HashMap<Cell,Integer>();
		for (NodeInst ni : layInsts) {
			Cell c = (Cell) ni.getProto();
			Integer cnt = cellToCount.get(c);
			if (cnt==null) cnt = 0;
			cellToCount.put(c, cnt+1);
		}
		return cellToCount; 
	}
	
	// Print report in the same order as sortedLayInsts
	private void printInstanceReport(List<NodeInst> sortedLayInsts) {
		prln("Here are the Cell's I've instantiated: ");
		Map<Cell,Integer> cellToCount = getCellCounts(sortedLayInsts);
		for (NodeInst ni : sortedLayInsts) {
			Cell c = (Cell) ni.getProto();
			Integer cnt = cellToCount.get(c);
			if (cnt!=null) {
				prln("    "+cnt+"   "+c.describe(false));
				// Only print report the first time we encounter the Cell
				cellToCount.remove(c);
			}
		}
		
	}
	
	//	private boolean isPlain(NodeInst ni) {
//		return ni.getProto().getName().contains("aPlainStage");
//	}
//	
//	// infinityA and infinityB need to be stretched to the height of infinityC
//	private void stretchInfinityAB(List<NodeInst> layInsts) {
//		Cell parent = layInsts.get(0).getParent();
//		Cell dummyStage = STAGE_LIB.findNodeProto("aDummyStage{lay}");
//		if (!layInsts.get(0).getParent().getName().contains("infinityA") &&
//			!layInsts.get(0).getParent().getName().contains("infinityB")) 
//			return;
//		
//		NodeInst prev = null;
//		int stretchCnt = 0;
//		for (ListIterator<NodeInst> niIt=layInsts.listIterator(); niIt.hasNext();) {
//			NodeInst ni =  niIt.next();
//			if (stretchCnt>=NUM_PLACES_TO_STRETCH) return;
//			if (prev!=null && isPlain(prev) && isPlain(ni)) {
//				NodeInst dumInst = LayoutLib.newNodeInst(dummyStage, 0, 0, 
//						                                 DEF_SIZE, DEF_SIZE, 
//						                                 0, parent);
//				niIt.add(dumInst);
//				stretchCnt++;
//			}
//			prev = ni;
//		}
//	}
	// infinityC needs its 2nd and 3rd stages overlapped. Same with the 
	// two stages next to the last stage.
	private void overlapInfinityC(List<NodeInst> layInsts) {
		NodeInst dataFan = layInsts.get(1);
		if (!dataFan.getParent().getName().contains("infinityC"))
			return;

		double h = dataFan.findEssentialBounds().getHeight();
		for (int i=2; i<layInsts.size(); i++) {
			NodeInst ni = layInsts.get(i);
			ni.move(0, -h);
		}
		if (true) {
			for (int i=layInsts.size()-2; i<layInsts.size(); i++) {
				NodeInst ni = layInsts.get(i);
				ni.move(0, -h);
			}
		}
	}
	// infinityC needs stages stacked top to bottom. Furthermore,
	// all stages except for the first three and last three need to be
	// mirrored top <=> bottom 
	private void flipInfinityC(List<NodeInst> layInsts) {
		NodeInst dataFan = layInsts.get(1);
		if (!dataFan.getParent().getName().contains("infinityC"))
			return;
		Collections.reverse(layInsts);
		int end =  3;
		for (int i=3; i<layInsts.size()-end; i++) {
			NodeInst ni = layInsts.get(i);
			ni.modifyInstance(0, 0, 0, 0, Orientation.Y);
		}
	}
	
	// infinityC needs scan stitched bottom to top
	private void reverseScanListInfinityC(List<NodeInst> scanList) {
		NodeInst first = scanList.get(1);
		if (!first.getParent().getName().contains("infinityC"))
			return;
		Collections.reverse(scanList);
	}
	
	private NodeInst findInst(String type, List<NodeInst> insts) {
		for (NodeInst ni : insts) {
			String t = ni.getProto().getName();
			if (t.equals(type)) return ni;
		}
		return null;
	}
	
	private void doInfinity(Cell autoLay, Cell schCell) {
        Library autoLib = autoLay.getLibrary();
        List<Library> primLibs = new ArrayList<Library>();
        primLibs.add(autoLib);
        SchematicVisitor visitor = new SchematicVisitor(autoLay);
        HierarchyEnumerator.enumerateCell(schCell, VarContext.globalContext, visitor);

        List<NodeInst> layInsts = visitor.getLayInsts();
        
		NodeInst niA = findInst("infinityA", layInsts);
		NodeInst niB = findInst("infinityB", layInsts);
		NodeInst niC = findInst("infinityC", layInsts);
		
		double colWid = niA.findEssentialBounds().getWidth();
		double numFillCellsAcross = Math.ceil(colWid/config.fillCellWidth);
		double columnPitch = config.fillCellWidth * numFillCellsAcross;
		double spaceBetweenCols = config.fillCellWidth + columnPitch - colWid;
		
		LayoutLib.alignCorners(niC, Corner.TL, niA, Corner.TR, -spaceBetweenCols, 0);
		LayoutLib.alignCorners(niC, Corner.BR, niB, Corner.BL, spaceBetweenCols, 0);
		List<ArcProto> horizLayers = new ArrayList<ArcProto>();
		horizLayers.add(tech().m2());
		horizLayers.add(tech().m4());
		AbutRouter.abutRouteLeftRight(niA, niC, 0, horizLayers);
		AbutRouter.abutRouteLeftRight(niC, niB, 0, horizLayers);

//        Rectangle2D bounds = findColBounds(layInsts);
//        addEssentialBounds(layInsts, bounds);
		addEssentialBounds(autoLay);
        ExportNamer vddNm = new ExportNamer("vdd");
        ExportNamer gndNm = new ExportNamer("gnd");
        //exportPwrGnd(autoLay, vddNm, gndNm);
        exportPwrGnd(layInsts,  vddNm, gndNm);

        List<ToConnect> toConns = visitor.getLayoutToConnects();
        reExport(autoLay, toConns);
	}
	private void doGuts(Cell autoLay, Cell schCell) {
        Library autoLib = autoLay.getLibrary();
        List<Library> primLibs = new ArrayList<Library>();
        primLibs.add(autoLib);
        SchematicVisitor visitor = new SchematicVisitor(autoLay);
        HierarchyEnumerator.enumerateCell(schCell, VarContext.globalContext, visitor);

        List<NodeInst> layInsts = visitor.getLayInsts();
        
		NodeInst niI = findInst("infinity", layInsts);
		NodeInst niC = findInst("crosser", layInsts);
		NodeInst niR = findInst("ring", layInsts);
		
		double colWid = niR.findEssentialBounds().getWidth();
		double numFillCellsAcross = Math.ceil(colWid/config.fillCellWidth);
		double columnPitch = config.fillCellWidth * numFillCellsAcross;
		double spaceBetweenCols = columnPitch - colWid;

		LayoutLib.alignCorners(niI, Corner.BR, niC, Corner.BL, spaceBetweenCols, (670-2));
		LayoutLib.alignCorners(niC, Corner.TL, niR, Corner.BL, 0, 0);
		
		List<ArcProto> horizLayers = new ArrayList<ArcProto>();
		horizLayers.add(tech().m2());
		horizLayers.add(tech().m4());

		AbutRouter.abutRouteLeftRight(niI, niC, 0, horizLayers);
		AbutRouter.abutRouteLeftRight(niI, niR, 0, horizLayers);
	}
	
	private boolean portOnLayer(PortInst pi, List<ArcProto> layers) {
		for (ArcProto ap : layers) {
			if (pi.getPortProto().connectsTo(ap)) return true;
		}
		return false;
	}
	private void exportPwrGnd(List<NodeInst> stages, 
			                  ExportNamer vdd, ExportNamer gnd) {
		Rectangle2D colBounds = Utils.findBounds(stages.get(0).getParent());
		List<ArcProto> vertLayers = new ArrayList<ArcProto>();
		vertLayers.add(tech().m3());
		List<ArcProto> horiLayers = new ArrayList<ArcProto>();
		horiLayers.add(tech().m2());
		for (NodeInst ni : stages) {
			 for (Iterator piIt=ni.getPortInsts(); piIt.hasNext();) {
				 PortInst pi = (PortInst) piIt.next();
				 if (Utils.isPwrGnd(pi)) {
					 if ((Utils.onTopOrBottom(pi, colBounds, 0) && 
					      portOnLayer(pi, vertLayers)) ||
						 (Utils.onLeftOrRight(pi, colBounds, 0) && 
						  portOnLayer(pi, horiLayers))) {
						 Cell parent = pi.getNodeInst().getParent();
						 String exptNm;
						 if (Utils.isPwr(pi)) {
							 exptNm = vdd.nextName();
						 } else {
							 exptNm = gnd.nextName();
						 }
						 Export.newInstance(parent, pi, exptNm);
					 }
				 }
			 }
		}
	}
	
	// Re-export all PortInsts of all NodeInsts that have the power or ground
	// characteristic that aren't connected to arcs.
	private void exportPwrGnd(Cell c, ExportNamer vdd, ExportNamer gnd) {
		for (Iterator<NodeInst> niIt=c.getNodes(); niIt.hasNext();) {
			NodeInst ni = niIt.next();
			if (!(ni.getProto() instanceof Cell)) continue;
			for (Iterator<PortInst> piIt=ni.getPortInsts(); piIt.hasNext();) {
				PortInst pi = piIt.next();
				if (!Utils.isPwrGnd(pi)) continue;
				if (pi.hasConnections()) continue;
				Export e;
				if (Utils.isPwr(pi)) {
					e = Export.newInstance(c, pi, vdd.nextName());
					e.setCharacteristic(PortCharacteristic.PWR);
				} else {
					e = Export.newInstance(c, pi, gnd.nextName());
					e.setCharacteristic(PortCharacteristic.GND);
				}
			}
		}
	}
	
	
//	private void doInfinityABC(Cell autoLay, Cell schCell) {
//        List<NodeInst> layInsts = new ArrayList<NodeInst>();
//        List<ToConnect> toConns = new ArrayList<ToConnect>();
//        LayoutNetlist layNets = createLayInstsFromSch(autoLay, schCell);
//
//        flipInfinityC(layInsts);
//        stackLayInsts(layInsts);
//        overlapInfinityC(layInsts);
//        
////        Rectangle2D colBounds = findColBounds(layInsts);
////        addEssentialBounds(layInsts, colBounds);
//        addEssentialBounds(autoLay);
//        router.connectPwrGnd(layInsts);
//
//        List<NodeInst> scanList = new ArrayList<NodeInst>(layInsts);
//        reverseScanListInfinityC(scanList);
//        scan.stitchScanChains(scanList, router);
//        
//		// debug
//		//for (ToConnect cl : toConns)  prln("  N-Pin "+cl.toString());
//        
//        //List<ToConnect> twoPins = reduceToTwoPin(toConns);
////        List<ToConnect> twoOrThreePins = reduceToTwoOrThreePin(toConns);
////        
////        LayerChannels m2chan = new LayerChannels();
////        LayerChannels m3chan = new LayerChannels();
////        
////        findChannels(m2chan, m3chan, layInsts);
////        
////        route(twoOrThreePins, m2chan, m3chan);
//        
////        List<NodeInst> covers = coverWithContacts(layInsts);
////        //exportVddGndCover(vddCnt, gndCnt, covers);
////        List<ArcProto> m3Layer = new ArrayList<ArcProto>();
////        m3Layer.add(tech().m3());
////		AbutRouter.abutRouteBotTop(covers.get(0), layInsts.get(0), 0, m3Layer);
//		
//        ExportNamer vddNmr = new ExportNamer("vdd");
//        ExportNamer gndNmr = new ExportNamer("gnd");
//		//exportPwrGnd(autoLay, vddNmr, gndNmr);
//        exportPwrGnd(layInsts,  vddNmr, gndNmr);
//		//fattenPwrGnd(layInsts);
//        
//        // Debug
//        //dumpChannels(m2chan, m3chan);
//
//        System.out.println("done.");
//	}
	
	/** If any PortInst is a scan port then ToConnect is a scan ToConnect */
	boolean isScanToConnect(ToConnect tc) {
		for (PortInst pi : tc.getPortInsts()) {
			if (scan.isScan(pi)) return true;
		}
		return false;
	}
	
	private List<ToConnect> selectScanToConnects(List<ToConnect> toConns) {
		List<ToConnect> scans = new ArrayList<ToConnect>();
		for (ToConnect tc : toConns) if (isScanToConnect(tc)) scans.add(tc);
		
		// debug
		for (ToConnect tc : scans) {
			prln("selectScanToCOnnects: "+tc.toString());
		}
		
		return scans;
	}
	private List<ToConnect> selectSignalToConnects(List<ToConnect> toConns) {
		List<ToConnect> signals = new ArrayList<ToConnect>();
		for (ToConnect tc : toConns) {
			if (isScanToConnect(tc)) continue;
			if (Utils.isPwrGnd(tc)) continue;
			signals.add(tc);
		}
		return signals;
	}
	
	
	// --------------------- methods for physical designers -------------------
	protected FlagDesign(FlagConfig cfg, FlagConstructorData data) {
		this.config = cfg;
		scan = new Scan(config.chains, cfg);
		router = new Router(config, scan);
		sogRouterAdapter = new SogRouterAdapter(data.getJob());
	}
	
	protected static void prln(String s) {Utils.prln(s);}
	protected static void pr(String s) {Utils.pr(s);}
	protected static void error(boolean cond, String msg) {Utils.error(cond, msg);}
	
	protected void addEssentialBounds(Cell c) {
		Rectangle2D bounds = Utils.findBounds(c);
		LayoutLib.newNodeInst(tech().essentialBounds(), 
							  bounds.getMinX(),
		                      bounds.getMinY(),
		                      DEF_SIZE, DEF_SIZE, 180, c);
		LayoutLib.newNodeInst(tech().essentialBounds(), 
		                      bounds.getMaxX(),
		                      bounds.getMaxY(),
		                      DEF_SIZE, DEF_SIZE, 0, c);
	}
	/** Traverse schematic hierarchy. For each instance of primitive Cell
	 * in the schematic, instantiate the layout of that primitive Cell in
	 * the layout. Return a list of the layout
	 * instances sorted by y coordinate of corresponding schematic instances. */
	protected LayoutNetlist createLayoutInstancesFromSchematic(FlagConstructorData data) {
		Cell layCell = data.getLayoutCell();
		Cell schCell = data.getSchematicCell();
        SchematicVisitor visitor = new SchematicVisitor(layCell);
        HierarchyEnumerator.enumerateCell(schCell, VarContext.globalContext, visitor);
        List<NodeInst> sortedLayInsts = getSortedLayInsts(visitor);
        printInstanceReport(sortedLayInsts);
        return new LayoutNetlist(layCell, sortedLayInsts,
        		                 visitor.getLayoutToConnects());
	}
	protected void stitchScanChains(LayoutNetlist layNets) {
		scan.stitchScanChains(layNets.getLayoutInstancesSortedBySchematicPosition(), router);
	}
	protected void stitchScanChainsSog(LayoutNetlist layNets, SeaOfGates.SeaOfGatesOptions prefs) {
		List<ToConnect> scans = selectScanToConnects(layNets.getToConnects());
		sogRouterAdapter.route(scans, prefs);
	}
	protected void routeSignalsSog(List<ToConnect> toConns, SeaOfGates.SeaOfGatesOptions prefs) {
		List<ToConnect> signals = selectSignalToConnects(toConns);
		sogRouterAdapter.route(signals, prefs);
	}
	protected void routeSignals(LayoutNetlist layNets) {
		List<ToConnect> signals = selectSignalToConnects(layNets.getToConnects());
		router.routeSignals(signals, layNets);
	}
	protected void reexportPowerGround(Cell c) {
		List<Geometric> allNodes = new ArrayList<Geometric>();
		for (Iterator<NodeInst> it = c.getNodes(); it.hasNext(); ) {
			allNodes.add(it.next());
		}
		int num = ExportChanges.reExportNodes(c, allNodes, false, true, true, true, true, config.iconParameters);
	}
	protected void reexportSignals(LayoutNetlist layNets) {
		Cell layCell = layNets.getLayoutCell();
		List<ToConnect> toConns = layNets.getToConnects();
		reExport(layCell, toConns);
	}
	protected void addNccVddGndExportsConnectedByParent(Cell c) {
		NccCellAnnotations.addNccAnnotation(c, "exportsConnectedByParent vdd /vdd_[0-9]+/");
		NccCellAnnotations.addNccAnnotation(c, "exportsConnectedByParent gnd /gnd_[0-9]+/");
	}
	

}
