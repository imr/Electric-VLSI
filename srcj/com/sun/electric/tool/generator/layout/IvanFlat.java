/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IvanFlat.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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
package com.sun.electric.tool.generator.layout;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;

class IvanFlater extends HierarchyEnumerator.Visitor {
	private static final boolean debug = false;
	private Writer of;
	private int depth = 0;
	private int nameNumber = 0; // Generate names for NodeInsts
								// of Cells without names.
	private void spaces() {
		for (int i = 0; i < depth; i++)
			System.out.print(" ");
	}

	private static void error(boolean pred, String msg) {
		if (pred) {
			throw new RuntimeException(msg);
		}

	}

	IvanFlater(String fileNm) {
		try {
			of = new BufferedWriter(new FileWriter(fileNm));
		} catch (Exception e) {
			error(true, "can't open output file: " + e.toString());
		}
	}

	public void close() {
		try {
			of.close();
		} catch (Exception e) {
			error(true, "close failed: " + e.toString());
		}
	}
	
	void writeExports(HierarchyEnumerator.CellInfo info) {
		Cell rootCell = info.getCell();
		for (Iterator<PortProto> it=rootCell.getPorts(); it.hasNext();) {
			Export e = (Export) it.next();
			Network net = info.getNetlist().getNetwork(e, 0);
			int netId = info.getNetID(net);
			String netNm = generateNetName(netId, info);
			writeln("EXPORT "+e.getName()+" "+netNm);
		}
	}

	public boolean enterCell(HierarchyEnumerator.CellInfo info) {
		if (debug) {
			spaces();
			System.out.println("Enter cell: " + info.getCell().getName());
			depth++;
		}
		if (info.isRootCell()) writeExports(info);
		return true;
	}

	public void exitCell(HierarchyEnumerator.CellInfo info) {
		if (debug) {
			depth--;
			spaces();
			System.out.println("Exit cell: " + info.getCell().getName());
		}
	}

	private String generateNetName(int id,
		                           HierarchyEnumerator.CellInfo info) {
//		HierarchyEnumerator.NetDescription netDesc =
//			info.netIdToNetDescription(id);
//		String pathNm = netDesc.getCellInfo().getContext().getInstPath("/");
//		if (!pathNm.endsWith("/"))
//			pathNm += "/";
//		String netNm;
//		Iterator it = netDesc.getNet().getNames();
//		if (it.hasNext()) {
//			netNm = pathNm + (String) it.next();
//		} else {
//			netNm = pathNm + id;
//		}
//		return netNm;
		return null;
	}

	private String getNetNm(NodeInst ni, String portNm,	
							HierarchyEnumerator.CellInfo info) {
		PortInst port = ni.findPortInst(portNm);
		error(port == null, "can't find port: " + portNm);
		Network net = info.getNetlist().getNetwork(port);
		error(net == null, "missing Network for port: " + portNm);
		int id = info.getNetID(net);
		error(id < 0, "missing net ID for port: " + portNm);
		return generateNetName(id, info);
	}
	
	private void dumpVariables(NodeInst ni) {
		for (Iterator<Variable> it=ni.getVariables(); it.hasNext();) {
			Variable v = (Variable)it.next();
			System.out.println("    "+v.getKey()+" = "+v.getObject());
		}
	}

	private String getMosWidthLength(NodeInst ni) {
		SizeOffset so = ni.getSizeOffset();
		double w = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
		double l = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();
		return w + " " + l;
	}

	private void writeln(String msg) {
		try {
			of.write(msg);
			of.write("\n");
		} catch (Exception e) {
			error(true, "writeln failed: " + of.toString());
		}
	}

	public boolean visitNodeInst(Nodable no,
								 HierarchyEnumerator.CellInfo info) {
		//System.out.println("Visit inst of: "+ni.getProto().getName());
		NodeProto np = no.getProto();
		String msg = null;
		if (np instanceof PrimitiveNode) {
		    NodeInst ni = (NodeInst)no;
			String protNm = np.getName();
			if (protNm.equals("N-Transistor")) {
				msg =
					"NMOS "
						+ getMosWidthLength(ni) + " "
						+ getNetNm(ni, "n-trans-diff-top", info) + " "
						+ getNetNm(ni, "n-trans-poly-right", info) + " "
						+ getNetNm(ni, "n-trans-diff-bottom", info);
			} else if (protNm.equals("P-Transistor")) {
				msg =
					"PMOS "
						+ getMosWidthLength(ni) + " "
						+ getNetNm(ni, "p-trans-diff-top", info) + " "
						+ getNetNm(ni, "p-trans-poly-right", info) + " "
						+ getNetNm(ni, "p-trans-diff-bottom", info);
			} else if (protNm.equals("Transistor")) {
				/* help debug enumerator on simple schematic */
				msg =
					"MOS "
						+ getMosWidthLength(ni) + " "
						+ getNetNm(ni, "d", info) + " "
						+ getNetNm(ni, "g", info) + " "
						+ getNetNm(ni, "s", info);
			}
		}
		if (msg != null) writeln(msg);
		return true;
	}
}

public class IvanFlat extends Job {
	private static class CellDescription {
		final String libName;
		final String cellName;
		CellDescription(String libNm, String cellNm) {
			libName=libNm; cellName=cellNm;
		}
	}
	
	private static void error(boolean pred, String msg) {
		if (pred) {
			throw new RuntimeException(msg);
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
			URL libFileURL = TextUtils.makeURLToFile(libFileNm);
			LibraryFiles.readLibrary(libFileURL, null, FileType.ELIB, false);
			lib = Library.findLibrary(libNm);
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

	private String getKaoHomeDir() {
		boolean nfsWedged = false;
		if (osIsWindows()) {
			// windows
			if (nfsWedged) {
				return "c:/a1/kao/Sun/";
			} else {
				return "x:/";
			}
		} else {
			// unix
			return "/home/rkao/";
		}
	}

	private Cell getTestCell(String homeDir) {
		Cell cell = null;
		String libDir = null;
		//cell = openCell(homeDir + "work/async/brownTest/rkTest.elib",
		//	      "top{lay}");
		//cell = openCell(homeDir + "work/async/brownTest/scanChainFour.elib",
		//	      "scanCLhor{lay}");
		//cell = openCell(homeDir + "work/async/qFour.0/txPads29.elib",
		//	      "txPadAmp{lay}");
		//cell = openCell(homeDir + "work/async/qFour.0/txPads29.elib",
		//	      "txPadAmpMux5Left{lay}");
		//cell = openCell(homeDir + "work/async/qFour.0/txPads29.elib",
		//	    "txPadLeft{lay}");
		//cell = openCell(homeDir + "work/async/qFour.0/txPads29.elib",
		//	    "txPadAll{lay}");
		//cell = openCell(homeDir + "work/async/qFour.0/zMeas.elib",
		//	      "txArray9{lay}");
		//cell = openCell(homeDir + "work/async/qFour.0/zMeas.elib",
		//	      "zMeasTx1{lay}");

		return cell;
	}

	private String outFileName(String cellNm) {
		int openBrace = cellNm.indexOf("{");
		String fileNm = openBrace>=0 ? cellNm.substring(0, openBrace) 
		                             : cellNm;
		fileNm += ".flat";
		return fileNm;
	}
	
	private void flattenOneCell(String outFileDir, String libDir, 
	                            CellDescription cellDesc) {
		System.out.println("processing: "+cellDesc.libName+" : "
						   +cellDesc.cellName);
		Cell cell = openCell(libDir, cellDesc.libName, cellDesc.cellName);
//		Netlist netlist = cell.getNetlist(false);
		String cellNm = cell.getName();

		String outFileNm = outFileDir + outFileName(cellNm); 
		IvanFlater flattener = new IvanFlater(outFileNm);
		long startTime = System.currentTimeMillis();
		HierarchyEnumerator.enumerateCell(cell,	VarContext.globalContext, flattener);
//		HierarchyEnumerator.enumerateCell(cell,	VarContext.globalContext,
//										  netlist, flattener);
		long endTime = System.currentTimeMillis();
		double deltaTime = (endTime - startTime) / 1000.0;
		System.out.println("Flattening took " + deltaTime + " seconds");
		System.out.flush();

		flattener.close();
	}

	public boolean doIt() {
		System.out.println("Begin IvanFlat");
		String homeDir = getKaoHomeDir();
		
		String outFileDir = homeDir + "ivanTest/qFourP1/"; 

		String libDir = homeDir + "ivanTest/qFourP1/electric-final/";

		CellDescription[] cellDescrs = {
			new CellDescription("stages", "stagePairJac{lay}"),
			new CellDescription("rxPads", "equilibrate{lay}"),
			new CellDescription("rxPads", "rxPadArray2{lay}"),
			new CellDescription("rxPads", "rxGroup{lay}"),
			new CellDescription("qFourP1", "expArings{lay}"),
			new CellDescription("qFourP1", "expTail{lay}")
		};
		
//		String libDir = homeDir + "work/async/kaoLayout/";		
//		CellDescription[] cellDescrs = {
//			new CellDescription("hierEnumNameTest", "top{sch}")
//		};


		for (int i=0; i<cellDescrs.length; i++) {
			flattenOneCell(outFileDir, libDir, cellDescrs[i]);
		}

		System.out.println("Done");
		return true;
	}
	public IvanFlat() {
		super("Flatten Netlists for Ivan", User.getUserTool(), Job.Type.CHANGE, 
		      null, null, Job.Priority.ANALYSIS);
		startJob();		      
	}
}


