/* -*- tab-width: 4 -*-
 */
package com.sun.electric.tool.generator.layout;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
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
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.io.Input;
import com.sun.electric.tool.io.Output;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

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
				ni2.setPositionFromTransform(at);
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

public class LayFlat implements ActionListener {
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
		return (String) props.get("user.name");
	}
	private Library openLibForRead(String libNm, String libFileNm) {
		Library lib = Library.findLibrary(libNm);
		if (lib==null) {
			Input.readLibrary(libFileNm, Input.ImportType.BINARY);
			lib = Library.findLibrary(libNm);
		}
		error(lib==null, "can't open Library for reading: "+libFileNm);
		return lib;
	}

	private Library openLibForAppend(String libNm, String libFileNm) {
		Library lib = Library.findLibrary(libNm);
		if (lib==null) {
			lib = Library.newInstance(libNm, libFileNm);
		}
		error(lib==null, "can't open Library for reading: "+libFileNm);
		return lib;
	}

	// Electric doesn't open the spiceparts.txt file properly yet. Read 
	// this in explicitly. 
	private void openSpiceParts(String libDir) {
		String spNm = "spiceparts";
		Library lib = openLibForRead(spNm, libDir + spNm + ".elib");
	}
	private Cell openCell(String libDir, String libNm, String cellNm) {
		openSpiceParts(libDir);
		Library lib = openLibForRead(libNm, libDir + libNm + ".elib");
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

	public void actionPerformed(ActionEvent e) {
		Thread doItThread = new Thread() {
			public void run() {
				doIt(new String[] {});
			}
		};
		doItThread.start();
	}

	void doIt(String[] args) {
		System.out.println("Begin flat");
		Cell cell = getTestCell();
		cell.rebuildNetworks(null, false);
		Library scratch = 
			openLibForAppend("scratch", 
		                     getHomeDir()+"work/async/scratch.elib");
		Cell flatCell = Cell.newInstance(scratch, "flatCell{lay}");
		WindowFrame window1 = WindowFrame.createEditWindow(flatCell);
		Flattener flattener = new Flattener(flatCell);
		long startTime = System.currentTimeMillis();
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext,
										  flattener);
		long endTime = System.currentTimeMillis();
		double deltaTime = (endTime - startTime) / 1000.0;
		System.out.println("Flattening took " + deltaTime + " seconds");

		Output.writeLibrary(scratch, Output.ExportType.BINARY);			                            
		System.out.println("Done");
	}

	public static void main(String[] args) {
		(new LayFlat()).doIt(args);
	}
}
