/* -*- tab-width: 4 -*-
 */
package com.sun.electric.tool.generator.layout;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;

class Flattener extends HierarchyEnumerator.Visitor {
	private static final boolean debug = false;
	private Cell flatCell;
	private int depth = 0;

	private void spaces() {
		for (int i = 0; i < depth; i++)
			System.out.print(" ");
	}

	private void createArcs(FlatInfo info) {
		for (Iterator it = info.getCell().getArcs(); it.hasNext();) {
			ArcInst ai = (ArcInst) it.next();
			Connection c0 = ai.getConnection(false);
			Connection c1 = ai.getConnection(true);

			// find flat PortInst and location
			Point2D p0 = info.getPositionInRoot(c0.getLocation());
			Point2D p1 = info.getPositionInRoot(c1.getLocation());
			PortInst pi0 = info.getFlatPort(c0.getPortInst());
			PortInst pi1 = info.getFlatPort(c1.getPortInst());
			//System.out.println("Old ArcInst: "+ai+" width: "+ai.getWidth());
			ArcInst ai2 = 
				ArcInst.newInstance(ai.getProto(), ai.getWidth(),
					                pi0, p0,
									pi1, p1, null);
			if (ai.isExtended()) ai2.setExtended();
		}
	}

	// Add PortInst to PortInst mappings for the NodeInst that
	// instantiated this Cell.
	private void addPortMappingsToParentCell(FlatInfo info) {
		NodeInst parentInst = (NodeInst)info.getParentInst(); // In layout all Nodables are NodeInsts

		if (parentInst == null) return; // root Cell has no parent

		for (Iterator it = info.getCell().getPorts(); it.hasNext();) {
			Export e = (Export) it.next();
			PortInst hierPort = e.getOriginalPort();
			PortInst flatPort = info.getFlatPort(hierPort);
			PortInst parentHierPort = parentInst.findPortInst(e.getProtoName());
			FlatInfo parentInfo = (FlatInfo) info.getParentInfo();
			parentInfo.mapHierPortToFlatPort(parentHierPort, flatPort);
		}
	}

	// Add the root's Exports to the Flattened Cell
	private void addRootExportsToFlatCell(FlatInfo info) {
		Cell root = info.getCell();
		for (Iterator it = root.getPorts(); it.hasNext();) {
			Export e = (Export) it.next();
			PortInst p = info.getFlatPort(e.getOriginalPort());
			Export eRoot = Export.newInstance(flatCell, p, e.getProtoName());
			//System.out.println("Export characteristic: "+e.getCharacteristic());
			eRoot.setCharacteristic(e.getCharacteristic());
		}
	}

	Flattener(Cell outCell) {flatCell=outCell;}

	public HierarchyEnumerator.CellInfo newCellInfo() {
		return new FlatInfo();
	}

	public boolean enterCell(HierarchyEnumerator.CellInfo inf) {
		FlatInfo info = (FlatInfo) inf;
		if (debug) {
			spaces();
			System.out.println("Enter cell: " + info.getCell().getProtoName());
			depth++;
		}
		return true;
	}

	public void exitCell(HierarchyEnumerator.CellInfo inf) {
		FlatInfo info = (FlatInfo) inf;

		// All the NodeInsts have been created for this cell.  Now create
		// the ArcInsts.
		createArcs(info);

		// Add PortInst to PortInst mappings for the NodeInst that
		// instantiated this Cell.
		addPortMappingsToParentCell(info);

		if (info.isRootCell()) addRootExportsToFlatCell(info);
		if (debug) {
			depth--;
			spaces();
			System.out.println("Exit cell: " + info.getCell().getProtoName());
		}
	}

	/**
	 * @TODO GVG Not finished yet
	 * @param no
	 * @param inf
	 * @return
	 */
	public boolean visitNodeInst(Nodable no,
	                             HierarchyEnumerator.CellInfo inf) {
		FlatInfo info = (FlatInfo) inf;
		//System.out.println("Visit inst of: "+ni.getProto().getName());
		NodeProto np = no.getProto();
		if (np instanceof PrimitiveNode) {
			NodeInst ni = (NodeInst)no;
			// don't copy Facet-Centers
			if (!np.getProtoName().equals("Facet-Center")) {
				AffineTransform at = info.getPositionInRoot(ni);
				//System.out.println("NodeInst: "+ni+" xform: "+at);
				NodeInst ni2 =
					NodeInst.newInstance(np, new Point2D.Double(0, 0),
						                 1, 1, 0, flatCell, null);
//				ni2.setPositionFromTransform(at);
				for (Iterator it = ni.getPortInsts(); it.hasNext();) {
					PortInst pi = (PortInst) it.next();
					PortInst pi2 = ni2.findPortInst(pi.getPortProto().getProtoName());
					info.mapHierPortToFlatPort(pi, pi2);
				}
			}
		}
		return true;
	}
}

class FlatInfo extends HierarchyEnumerator.CellInfo {
	private Map hierPortToFlatPort = new HashMap();

	public void mapHierPortToFlatPort(PortInst hier, PortInst flat) {
		hierPortToFlatPort.put(hier, flat);
	}

	public PortInst getFlatPort(PortInst hierPort) {
		return (PortInst) hierPortToFlatPort.get(hierPort);
	}
}

public class LayFlat extends Job {
	private static void error(boolean pred, String msg) {
		if(pred) {
			System.out.println(msg);
			throw new RuntimeException();
		}
	}

	private boolean osIsWindows() {
		Properties props = System.getProperties();
		String osName = ((String) props.get("os.name")).toLowerCase();
		return osName.indexOf("windows") != -1;
	}

	private static String userName() {
		Properties props = System.getProperties();
		String name = (String) props.get("user.name");
		return name;
	}

	private Cell openCell(String libDir, String libNm, String cellNm) {
		Library lib = LayoutLib.openLibForRead(libNm, libDir + libNm + ".elib");
		Cell cell = lib.findNodeProto(cellNm);
		error(cell==null, "can't find cell: " + cellNm);
		return cell;
	}
	
	private String getHomeDir() {
		String homeDir = null;
		if (userName().equals("rkao")) {
			boolean nfsWedged = false;
			if (osIsWindows()) {
				// windows
				if (nfsWedged) {
					homeDir = "c:/a1/kao/Sun/";
				} else {
					homeDir = "x:/";
				}
			} else {
				// unix
				homeDir = "/home/rkao/";
			}
		} else {
			error(true, "unrecognized user");
		}
		return homeDir;
	}

	private Cell getTestCell() {
		String homeDir = getHomeDir();
		Cell cell = null;
		String libDir = homeDir + "work/async/qFour.0/";
		//cell = openCell(libDir, "txPads29", "txPadAmp{lay}");
		//cell = openCell(libDir, "txPads29", "txPadAmpMux5Left{lay}");
		//cell = openCell(libDir, "txPads29", "txPadAll{lay}");
		//cell = openCell(libDir, "txPads29", "txPadLeft{lay}");
		//cell = openCell(libDir, "zMeas", "txArray9{lay}");
		//cell = openCell(libDir, "zMeas.elib", "zMeasTx1{lay}");

		libDir = homeDir + "work/async/kaoLayout/"; 
		cell = openCell(libDir,	"flatTest",	"bot{lay}");

		libDir = homeDir + "work/async/ivanTest/qFourP1/electric-final/";
		//cell = openCell(libDir, "rxPads", "equilibrate{lay}");		
		//cell = openCell(libDir, "rxPads", "rxPadArray2{lay}");
		//cell = openCell(libDir, "rxPads", "rxGroup{lay}");
		//cell = openCell(libDir, "qFourP1", "expArings{lay}");
		//cell = openCell(libDir, "qFourP1", "expTail{lay}");
 
		return cell;
	}

	public boolean doIt() {
		System.out.println("Begin flat");
		Cell cell = getTestCell();
		Library scratch = LayoutLib.openLibForWrite(
			"scratch", 
		    getHomeDir()+"work/async/scratch.elib");
		Cell lowCell = Cell.newInstance(scratch, "lowCell{lay}");
		NodeInst.newInstance(Tech.m1m2, new Point2D.Double(1,2),
							 5, 9, 0, lowCell, null);
		Cell hiCell = Cell.newInstance(scratch, "hiCell{lay}");
		Rectangle2D b = lowCell.getBounds();
		NodeInst lowInst = 
		NodeInst.newInstance(lowCell, new Point2D.Double(0,0),
							 b.getWidth(), b.getHeight(), 0, hiCell, null);
		System.out.println(lowInst.getAnchorCenter());
		
//		WindowFrame window1 = WindowFrame.createEditWindow(flatCell);
//		Flattener flattener = new Flattener(flatCell);
//		long startTime = System.currentTimeMillis();
//		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext,
//										  null, flattener);
//		long endTime = System.currentTimeMillis();
//		double deltaTime = (endTime - startTime) / 1000.0;
//		System.out.println("Flattening took " + deltaTime + " seconds");

//		LayoutLib.writeLibrary(scratch);			                            
		System.out.println("Done");
		return true;
	}
	public LayFlat() {
		super("Run Layout Flattener", User.tool, Job.Type.CHANGE, 
			  null, null, Job.Priority.ANALYSIS);
		startJob();

	}
}
