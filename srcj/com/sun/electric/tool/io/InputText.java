/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: InputText.java
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
package com.sun.electric.tool.io;

import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.InputLibrary;
import com.sun.electric.tool.user.dialogs.Progress;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class reads files in readable-dump (.txt) format.
 */
public class InputText extends InputLibrary
{
	// ------------------------- private data ----------------------------
	static class ArcInstList
	{
		private ArcInst []   arcList;
		private ArcProto []  arcProto;
		private Name []      arcInstName;
		private int []       arcWidth;
		private NodeInst []  arcHeadNode;
		private String []    arcHeadPort;
		private int []       arcHeadX;
		private int []       arcHeadY;
		private NodeInst []  arcTailNode;
		private String []    arcTailPort;
		private int []       arcTailX;
		private int []       arcTailY;
	};
	static class ExportList
	{
		private Export []   exportList;
		private String []   exportName;
		private NodeInst [] exportSubNode;
		private String []   exportSubPort;
	};

	/** The current position in the file. */						private int filePosition;
	/** The version of Electric that made the file. */				private int emajor, eminor, edetail;
	/** The state of reading the file. */							private int textLevel;
	/** A counter for reading multiple Tools, Technologies, etc. */	private int bitCount;
	/** The current Cell in the Library. */							private int mainCell;
	/** The current ArcInst end being processed. */					private int curArcEnd;
	/** The current object type being processed. */					private int varPos;
	/** The current Cell being processed. */						private Cell curNodeProto;
	/** The index of the current Cell being processed. */			private int curCellNumber;
	/** The index of the current NodeInst being processed. */		private int curNodeInstIndex;
	/** The index of the current ArcInst being processed. */		private int curArcInstIndex;
	/** The index of the current Export being processed. */			private int curExportIndex;
//	/** Lambda values for cells being read. */						private double [] cellLambda;
	/** Offset values for cells being read. */						private double [] nodeProtoOffX, nodeProtoOffY;
	/** All data for NodeInsts in each Cell. */						private InputLibrary.NodeInstList[] nodeInstList;
	/** All data for ArcInsts in each Cell. */						private ArcInstList[] arcInstList;
	/** All data for Exports in each Cell. */						private ExportList [] exportList;
	/** True to convert old MOSIS CMOS technologies. */				private boolean convertMosisCMOSTechnologies;
	/** The input stream that counts lines. */						private LineNumberReader lineReader;
	/** The maximum characters in a keyword. */						private int keywordArrayLen;
	/** An array of keyword data. */								private char [] keywordArray = null;
	/** The current keyword from the file. */						private String keyWord;
	/** All Tools found in the file. */								private Tool [] toolList;
	/** The current Tool being processed. */						private Tool curTool;
	/** All Technologies found in the file. */						private Technology [] techList;
	/** The current Technology being processed. */					private Technology curTech;
	/** The values of Lambda for each Technology. */				private int [] lambdaValues;
	/** The name of the current Cell being processed. */			private CellName cellName;

	// state of input (value of "textLevel")
	/** Currently reading Library information. */		private static final int INLIB =         1;
	/** Currently reading Cell information. */			private static final int INCELL =        2;
	/** Currently reading Export information. */		private static final int INPORTPROTO =   3;
	/** Currently reading NodeInst information. */		private static final int INNODEINST =    4;
	/** Currently reading PortInst information. */		private static final int INPOR =         5;
	/** Currently reading ArcInst information. */		private static final int INARCINST =     6;
	/** Currently reading ArcInst end information. */	private static final int INARCEND =      7;

	// state of variable reading (value of "varPos")
	/** Currently reading a Tool. */					private static final int INVTOOL =        1;
	/** Currently reading a Technology. */				private static final int INVTECHNOLOGY =  2;
	/** Currently reading a Library. */					private static final int INVLIBRARY =     3;
	/** Currently reading a Cell. */					private static final int INVNODEPROTO =   4;
	/** Currently reading a NodeInst. */				private static final int INVNODEINST =    5;
	/** Currently reading a Export. */					private static final int INVPORTPROTO =   6;
	/** Currently reading a ArcInst. */					private static final int INVARCINST =     7;

	InputText()
	{
	}

	// ----------------------- public methods -------------------------------

	/**
	 * Method to read a Library in readable-dump (.txt) format.
	 * @return true on error.
	 */
	protected boolean readLib()
	{
		try
		{
			return readTheLibrary();
		} catch (IOException e)
		{
			System.out.println("End of file reached while reading " + filePath);
			return true;
		}
	}

	/**
	 * Method to read the .elib file.
	 * Returns true on error.
	 */
	private boolean readTheLibrary()
		throws IOException
	{
		InputStreamReader is = new InputStreamReader(fileInputStream);
		lineReader = new LineNumberReader(is);

		lib.erase();

		textLevel = INLIB;
		filePosition = 0;
		for(;;)
		{
			// get keyword from file
			if (getKeyword()) break;
			String thisKey = keyWord;

			// get argument to keyword
			if (getKeyword()) break;

			// determine which keyword table to use
			switch (textLevel)
			{
				case INLIB:
					if (thisKey.equals("****library:")) {     io_newlib();   break;   }
					if (thisKey.equals("bits:")) {            io_libbit();   break;   }
					if (thisKey.equals("lambda:")) {          io_lambda();   break;   }
					if (thisKey.equals("version:")) {         io_versn();    break;   }
					if (thisKey.equals("aids:")) {            io_libkno();   break;   }
					if (thisKey.equals("aidname:")) {         io_libain();   break;   }
					if (thisKey.equals("aidbits:")) {         io_libaib();   break;   }
					if (thisKey.equals("userbits:")) {        io_libusb();   break;   }
					if (thisKey.equals("techcount:")) {       io_libte();    break;   }
					if (thisKey.equals("techname:")) {        io_libten();   break;   }
					if (thisKey.equals("cellcount:")) {       io_libcc();    break;   }
					if (thisKey.equals("maincell:")) {        io_libms();    break;   }
					if (thisKey.equals("view:")) {            io_libvie();   break;   }
					if (thisKey.equals("***cell:")) {         io_newcel();   break;   }
					if (thisKey.equals("variables:")) {       io_getvar();   break;   }
					 break;
				case INCELL:
					if (thisKey.equals("bits:")) {            io_celbit();   break;   }
					if (thisKey.equals("userbits:")) {        io_celusb();   break;   }
					if (thisKey.equals("name:")) {            io_celnam();   break;   }
					if (thisKey.equals("version:")) {         io_celver();   break;   }
					if (thisKey.equals("creationdate:")) {    io_celcre();   break;   }
					if (thisKey.equals("revisiondate:")) {    io_celrev();   break;   }
					if (thisKey.equals("externallibrary:")) { io_celext();   break;   }
					if (thisKey.equals("nodes:")) {           io_celnoc();   break;   }
					if (thisKey.equals("arcs:")) {            io_celarc();   break;   }
					if (thisKey.equals("porttypes:")) {       io_celptc();   break;   }
					if (thisKey.equals("technology:")) {      io_tech();     break;   }
					if (thisKey.equals("**node:")) {          io_newno();    break;   }
					if (thisKey.equals("**arc:")) {           io_newar();    break;   }
					if (thisKey.equals("***cell:")) {         io_newcel();   break;   }
					if (thisKey.equals("variables:")) {       io_getvar();   break;   }
					break;
				case INPORTPROTO:
					if (thisKey.equals("bits:")) {            io_ptbit();    break;   }
					if (thisKey.equals("userbits:")) {        io_ptusb();    break;   }
					if (thisKey.equals("subnode:")) {         io_ptsno();    break;   }
					if (thisKey.equals("subport:")) {         io_ptspt();    break;   }
					if (thisKey.equals("name:")) {            io_ptnam();    break;   }
					if (thisKey.equals("descript:")) {        io_ptdes();    break;   }
					if (thisKey.equals("aseen:")) {           io_ptkse();    break;   }
					if (thisKey.equals("**porttype:")) {      io_newpt();    break;   }
					if (thisKey.equals("**arc:")) {           io_newar();    break;   }
					if (thisKey.equals("**node:")) {          io_newno();    break;   }
					if (thisKey.equals("***cell:")) {         io_newcel();   break;   }
					if (thisKey.equals("variables:")) {       io_getvar();   break;   }
					break;
				case INNODEINST:
					if (thisKey.equals("bits:")) {            io_nodbit();   break;   }
					if (thisKey.equals("userbits:")) {        io_nodusb();   break;   }
					if (thisKey.equals("type:")) {            io_nodtyp();   break;   }
					if (thisKey.equals("lowx:")) {            io_nodlx();    break;   }
					if (thisKey.equals("highx:")) {           io_nodhx();    break;   }
					if (thisKey.equals("lowy:")) {            io_nodly();    break;   }
					if (thisKey.equals("highy:")) {           io_nodhy();    break;   }
					if (thisKey.equals("rotation:")) {        io_nodrot();   break;   }
					if (thisKey.equals("transpose:")) {       io_nodtra();   break;   }
					if (thisKey.equals("aseen:")) {           io_nodkse();   break;   }
					if (thisKey.equals("name:")) {            io_nodnam();   break;   }
					if (thisKey.equals("descript:")) {        io_noddes();   break;   }
					if (thisKey.equals("*port:")) {           io_newpor();   break;   }
					if (thisKey.equals("**node:")) {          io_newno();    break;   }
					if (thisKey.equals("**porttype:")) {      io_newpt();    break;   }
					if (thisKey.equals("**arc:")) {           io_newar();    break;   }
					if (thisKey.equals("variables:")) {       io_getvar();   break;   }
					if (thisKey.equals("***cell:")) {         io_newcel();   break;   }
					if (thisKey.equals("ports:")) {           io_nodpoc();   break;   }
					break;
				case INPOR:
					if (thisKey.equals("*port:")) {           io_newpor();   break;   }
					if (thisKey.equals("**node:")) {          io_newno();    break;   }
					if (thisKey.equals("**porttype:")) {      io_newpt();    break;   }
					if (thisKey.equals("**arc:")) {           io_newar();    break;   }
					if (thisKey.equals("variables:")) {       io_getvar();   break;   }
					if (thisKey.equals("***cell:")) {         io_newcel();   break;   }
					break;
				case INARCINST:
					if (thisKey.equals("bits:")) {            io_arcbit();   break;   }
					if (thisKey.equals("userbits:")) {        io_arcusb();   break;   }
					if (thisKey.equals("type:")) {            io_arctyp();   break;   }
					if (thisKey.equals("width:")) {           io_arcwid();   break;   }
					if (thisKey.equals("aseen:")) {           io_arckse();   break;   }
					if (thisKey.equals("name:")) {            io_arcnam();   break;   }
					if (thisKey.equals("*end:")) {            io_newend();   break;   }
					if (thisKey.equals("**arc:")) {           io_newar();    break;   }
					if (thisKey.equals("**node:")) {          io_newno();    break;   }
					if (thisKey.equals("variables:")) {       io_getvar();   break;   }
					if (thisKey.equals("***cell:")) {         io_newcel();   break;   }
					break;
				case INARCEND:
					if (thisKey.equals("node:")) {            io_endnod();   break;   }
					if (thisKey.equals("nodeport:")) {        io_endpt();    break;   }
					if (thisKey.equals("xpos:")) {            io_endxp();    break;   }
					if (thisKey.equals("ypos:")) {            io_endyp();    break;   }
					if (thisKey.equals("*end:")) {            io_newend();   break;   }
					if (thisKey.equals("**arc:")) {           io_newar();    break;   }
					if (thisKey.equals("**node:")) {          io_newno();    break;   }
					if (thisKey.equals("variables:")) {       io_getvar();   break;   }
					if (thisKey.equals("***cell:")) {         io_newcel();   break;   }
					break;
			}
		}

		// see if cellgroup information was included
//		for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//			if (np->temp1 == -1) break;
//		if (np != NONODEPROTO)
//		{
//			// missing cellgroup information, construct it from names
//			if (emajor > 7 ||
//				(emajor == 7 && eminor > 0) ||
//				(emajor == 7 && eminor == 0 && edetail > 11))
//			{
//				ttyputmsg(M_("Unusual!  Version %s library has no cellgroup information"), version);
//			}
//			io_buildcellgrouppointersfromnames(lib);
//		} else if (lib->firstnodeproto != NONODEPROTO)
//		{
//			// convert numbers to cellgroup pointers
//			if (emajor < 7 ||
//				(emajor == 7 && eminor == 0 && edetail <= 11))
//			{
//				ttyputmsg(M_("Unusual!  Version %s library has cellgroup information"), version);
//			}
//			for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//				np->nextcellgrp = NONODEPROTO;
//			for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//			{
//				if (np->nextcellgrp != NONODEPROTO) continue;
//				prevmatch = np;
//				for(onp = np->nextnodeproto; onp != NONODEPROTO; onp = onp->nextnodeproto)
//				{
//					if (onp->temp1 != prevmatch->temp1) continue;
//					prevmatch->nextcellgrp = onp;
//					prevmatch = onp;
//				}
//				prevmatch->nextcellgrp = np;
//			}
//		}

		// warn if the MOSIS CMOS technologies were converted
//		if (convertMosisCMOSTechnologies)
//		{
//			for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//			{
//				for(ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//					if (ni->proto->primindex != 0 && ni->proto->tech == mocmos_tech) break;
//				if (ni != NONODEINST) break;
//				for(ai = np->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst)
//					if (ai->proto->tech == mocmos_tech) break;
//				if (ai != NOARCINST) break;
//			}
//			if (np != NONODEPROTO)
//				DiaMessageInDialog(
//					_("Warning: library %s has older 'mocmossub' technology, converted to new 'mocmos'"),
//						lib->libname);
//		}

		if (mainCell >= 0)
			lib.setCurCell((Cell)nodeProtoList[mainCell]);

		lib.clearChangedMajor();
		lib.clearChangedMinor();
		return false;
	}

	//************************************* recursive

	/**
	 * Method to recursively create the contents of each cell in the library.
	 */
	protected void realizeCellsRecursively(Cell cell, FlagSet markCellForNodes)
	{
		// recursively scan the nodes to the bottom and only proceed when everything below is built
		int cellIndex = cell.getTempInt();
		InputLibrary.NodeInstList nil = nodeInstList[cellIndex];
		int numNodes = 0;
		NodeProto [] nodePrototypes = null;
		if (nil != null)
		{
			nodePrototypes = nil.protoType;
			numNodes = nodePrototypes.length;
		}
		scanNodesForRecursion(cell, markCellForNodes, nodePrototypes, 0, numNodes);

		// report progress
		if (InputLibrary.VERBOSE)
			System.out.println("Text: Doing contents of cell " + cell.describe() + " in library " + lib.getLibName());
		cellsConstructed++;
		progress.setProgress(cellsConstructed * 100 / totalCells);

		// now fill in the nodes
		double lambda = cellLambda[cellIndex];
		Point2D offset = realizeNode(cell, nil, lambda);
		nodeProtoOffX[cellIndex] = offset.getX();
		nodeProtoOffY[cellIndex] = offset.getY();

		// do the exports now
		realizeExports(cell, cellIndex);

		// do the arcs now
		realizeArcs(cell, cellIndex);
	}

	protected boolean spreadLambda(Cell cell, int cellIndex)
	{
		boolean changed = false;
		InputLibrary.NodeInstList nil = nodeInstList[cellIndex];
		int numNodes = 0;
		if (nil != null) numNodes = nil.protoType.length;
		double thisLambda = cellLambda[cellIndex];
		for(int i=0; i<numNodes; i++)
		{
			NodeProto np = nil.protoType[i];
			if (np instanceof PrimitiveNode) continue;
			Cell subCell = (Cell)np;

			InputLibrary reader = this;
			if (subCell.getLibrary() != lib)
			{
				reader = getReaderForLib(subCell.getLibrary());
			}
			int subCellIndex = subCell.getTempInt();
			double subLambda = reader.cellLambda[subCellIndex];
			if (subLambda != thisLambda)
			{
				reader.cellLambda[subCellIndex] = thisLambda;
				changed = true;
			}
		}
		return changed;
	}

	protected double computeLambda(Cell cell, int cellIndex)
	{
		InputLibrary.NodeInstList nil = nodeInstList[cellIndex];
		int numNodes = 0;
		NodeProto [] nodePrototypes = null;
		if (nil != null)
		{
			nodePrototypes = nil.protoType;
			numNodes = nodePrototypes.length;
		}

		ArcInstList ail = arcInstList[cellIndex];
		int numArcs = 0;
		ArcProto [] arcPrototypes = null;
		if (ail != null)
		{
			arcPrototypes = ail.arcProto;
			numArcs = arcPrototypes.length;
		}
		Technology cellTech = Technology.whatTechnology(cell, nodePrototypes, 0, numNodes,
			arcPrototypes, 0, numArcs);
		double lambda = 1;
		if (cellTech != null)
		{
			for(int i=0; i<techList.length; i++)
			{
				if (techList[i] != cellTech) continue;
				lambda = lambdaValues[i];
				break;
			}
		}
		return lambda;
	}

	private Point2D realizeNode(Cell cell, InputLibrary.NodeInstList nil, double lambda)
	{
		// find the "cell center" node and place it first
		double xoff = 0, yoff = 0;
		int numNodes = 0;
		if (nil != null) numNodes = nil.protoType.length;
		for(int j=0; j<numNodes; j++)
		{
			// convert to new style
			NodeProto np = nil.protoType[j];
			if (np == Generic.tech.cellCenterNode)
			{
				NodeInst ni = nil.theNode[j];
				Name name = nil.name[j];
				int lowX = nil.lowX[j];
				int lowY = nil.lowY[j];
				int highX = nil.highX[j];
				int highY = nil.highY[j];
				xoff = (lowX + highX) / 2;
				yoff = (lowY + highY) / 2;
				Point2D center = new Point2D.Double(xoff / lambda, yoff / lambda);
				double width = (highX - lowX) / lambda;
				double height = (highY - lowY) / lambda;
				ni.lowLevelPopulate(np, center, width, height, nil.rotation[j], cell);
				if (name != null) ni.setNameKey(name);
				ni.lowLevelLink();
			}
		}
		Point2D offset = new Point2D.Double(xoff, yoff);

		// create the rest of the nodes
		for(int j=0; j<numNodes; j++)
		{
			NodeProto np = nil.protoType[j];
			if (np == null) continue;
			if (np == Generic.tech.cellCenterNode) continue;
			NodeInst ni = nil.theNode[j];
			Name name = nil.name[j];
			int lowX = nil.lowX[j];
			int lowY = nil.lowY[j];
			int highX = nil.highX[j];
			int highY = nil.highY[j];
			int cX = (lowX + highX) / 2;
			int cY = (lowY + highY) / 2;
			Point2D center = new Point2D.Double((double)(cX-xoff) / lambda, (double)(cY-yoff) / lambda);
			double width = (highX - lowX) / lambda;
			double height = (highY - lowY) / lambda;
			int rotation = nil.rotation[j];

			if (emajor > 7 || (emajor == 7 && eminor >= 1))
			{
				// new version: allow mirror bits
				if ((nil.transpose[j]&1) != 0)
				{
					height = -height;
					rotation = (rotation + 900) % 3600;
				}
				if ((nil.transpose[j]&2) != 0)
				{
					// mirror in X
					width = -width;
				}
				if ((nil.transpose[j]&4) != 0)
				{
					// mirror in Y
					height = -height;
				}
			} else
			{
				// old version: just use transpose information
				if (nil.transpose[j] != 0)
				{
					height = -height;
					rotation = (rotation + 900) % 3600;
				}
			}
			if (np instanceof Cell)
			{
				Cell subCell = (Cell)np;
				Rectangle2D bounds = subCell.getBounds();
				Point2D shift = new Point2D.Double(-bounds.getCenterX(), -bounds.getCenterY());
				AffineTransform trans = NodeInst.pureRotate(rotation, width, height);
				trans.transform(shift, shift);
				center.setLocation(center.getX() + shift.getX(), center.getY() + shift.getY());
			}
			ni.lowLevelPopulate(np, center, width, height, rotation, cell);
			if (name != null) ni.setNameKey(name);
			ni.lowLevelLink();

			// convert outline information, if present
			scaleOutlineInformation(ni, np, lambda);
		}
		return offset;
	}

	private void realizeExports(Cell cell, int cellIndex)
	{
		ExportList el = exportList[cellIndex];
		int numExports = 0;
		if (el != null) numExports = el.exportList.length;
		for(int j=0; j<numExports; j++)
		{
			Export pp = el.exportList[j];
			if (pp.lowLevelName(cell, el.exportName[j])) return;
//			if (!el.exportSubNode[j].isLinked())
//			{
//System.out.println("Text export UNRESOLVED");
//				continue;
//			}
			PortInst pi = el.exportSubNode[j].findPortInst(el.exportSubPort[j]);
			if (pp.lowLevelPopulate(pi)) return;
			if (pp.lowLevelLink(null)) return;
		}
	}

	private void realizeArcs(Cell cell, int cellIndex)
	{
		ArcInstList ail = arcInstList[cellIndex];
		int numArcs = 0;
		if (ail != null) numArcs = ail.arcProto.length;
		double lambda = cellLambda[cellIndex];
		double xoff = nodeProtoOffX[cellIndex];
		double yoff = nodeProtoOffY[cellIndex];

		// create the arcs
		for(int j=0; j<numArcs; j++)
		{
			ArcInst ai = ail.arcList[j];
			ArcProto ap = ail.arcProto[j];
			Name name = ail.arcInstName[j];
			double width = ail.arcWidth[j] / lambda;
			if (!ail.arcHeadNode[j].isLinked() || !ail.arcTailNode[j].isLinked()) continue;
			PortInst headPortInst = ail.arcHeadNode[j].findPortInst(ail.arcHeadPort[j]);
			PortInst tailPortInst = ail.arcTailNode[j].findPortInst(ail.arcTailPort[j]);
			if (ap == null || headPortInst == null || tailPortInst == null) continue;
			double headX = (ail.arcHeadX[j]-xoff) / lambda;
			double headY = (ail.arcHeadY[j]-yoff) / lambda;
			double tailX = (ail.arcTailX[j]-xoff) / lambda;
			double tailY = (ail.arcTailY[j]-yoff) / lambda;
			Point2D headPt = new Point2D.Double(headX, headY);
			Point2D tailPt = new Point2D.Double(tailX, tailY);

//			// make checks
//			Poly poly = headPortInst.getPoly();
//			if (!poly.isInside(headPt))
//				System.out.println("Cell " + cell.describe() + ", arc " + ap.describe() + " head at (" +
//					ail.arcHeadX[j] + "," + ail.arcHeadY[j] + ") not in port");
//			poly = tailPortInst.getPoly();
//			if (!poly.isInside(tailPt))
//				System.out.println("Cell " + cell.describe() + ", arc " + ap.describe() + " tail at (" +
//					ail.arcTailX[j] + "," + ail.arcTailY[j] + ") not in port");

			int defAngle = ai.lowLevelGetArcAngle() * 10;
			ai.lowLevelPopulate(ap, width, tailPortInst, tailPt, headPortInst, headPt, defAngle);
			if (name != null) ai.setNameKey(name);
			ai.lowLevelLink();
		}
	}

	protected boolean readerHasExport(Cell c, String portName)
	{
		for(int cellIndex=0; cellIndex<nodeProtoCount; cellIndex++)
		{
			Cell cell = nodeProtoList[cellIndex];
			if (cell != c) continue;
			ExportList el = exportList[cellIndex];
			int numExports = 0;
			if (el != null) numExports = el.exportList.length;
			for(int j=0; j<numExports; j++)
			{
				String exportName = el.exportName[j];
				if (exportName.equalsIgnoreCase(portName)) return true;
			}
			break;
//			Export pp = el.exportList[j];
//			if (pp.lowLevelName(cell, el.exportName[j])) return;
//			if (!el.exportSubNode[j].isLinked()) continue;
//			PortInst pi = el.exportSubNode[j].findPortInst(el.exportSubPort[j]);
//			if (pp.lowLevelPopulate(pi)) return;
//			if (pp.lowLevelLink(null)) return;
		}
		System.out.println("readHasExport could not find port!!!!");
		return false;
	}

	private boolean getKeyword()
		throws IOException
	{
		// skip leading blanks
		int c = 0;
		for(;;)
		{
			c = lineReader.read();
			if (c == -1) return true;
			filePosition++;
			if (c != ' ') break;
		}

		// collect the word
		int cindex = 0;
		boolean inQuote = false;
		if (c == '"') inQuote = true;
		if (keywordArray == null)
		{
			keywordArrayLen = 500;
			keywordArray = new char[keywordArrayLen];
		}
		keywordArray[cindex++] = (char)c;
		for(;;)
		{
			c = lineReader.read();
			if (c == -1) return true;
			filePosition++;
			if (c == '\n' || (c == ' ' && !inQuote)) break;
			if (c == '"' && (cindex == 0 || keywordArray[cindex-1] != '^'))
				inQuote = !inQuote;
			if (cindex >= keywordArrayLen)
			{
				int newKeywordArrayLen = keywordArrayLen * 2;
				char [] newKeywordArray = new char[newKeywordArrayLen];
				for(int i=0; i<keywordArrayLen; i++)
					newKeywordArray[i] = keywordArray[i];
				keywordArray = newKeywordArray;
				keywordArrayLen = newKeywordArrayLen;
			}
			keywordArray[cindex++] = (char)c;
		}
		keyWord = new String(keywordArray, 0, cindex);
		if (progress != null && fileLength > 0)
		{
			progress.setProgress((int)(filePosition * 100 / fileLength));
		}
		return false;
	}

	// --------------------------------- LIBRARY PARSING METHODS ---------------------------------

	/**
	 * a new library is introduced (keyword "****library")
	 * This should be the first keyword in the file
	 */
	private void io_newlib()
	{
		// set defaults
		mainCell = -1;
		varPos = INVTOOL;
		curTech = null;
		textLevel = INLIB;
	}

	/**
	 * get the file's Electric version number (keyword "version")
	 */
	private void io_versn()
	{
		String version = keyWord;

		// for versions before 6.03q, convert MOSIS CMOS technology names
		Version versionStruct = Version.parseVersion(version);
		emajor = versionStruct.getMajor();
		eminor = versionStruct.getMinor();
		edetail = versionStruct.getDetail();
		convertMosisCMOSTechnologies = false;
//		if (emajor < 6 ||
//			(emajor == 6 && eminor < 3) ||
//			(emajor == 6 && eminor == 3 && edetail < 17))
//		{
//			if ((asktech(mocmossub_tech, x_("get-state"))&MOCMOSSUBNOCONV) == 0)
//				convertMosisCMOSTechnologies = true;
//		}
//		if (convertMosisCMOSTechnologies)
//			ttyputmsg(x_("   Converting MOSIS CMOS technologies (mocmossub => mocmos)"));
	}

	/**
	 * get the number of tools (keyword "aids")
	 */
	private void io_libkno()
	{
		bitCount = 0;
		toolList = new Tool[Integer.parseInt(keyWord)];
	}

	/**
	 * get the name of the tool (keyword "aidname")
	 */
	private void io_libain()
	{
		curTool = Tool.findTool(keyWord);
		toolList[bitCount++] = curTool;
	}

	/**
	 * get the number of toolbits (keyword "aidbits")
	 */
	private void io_libaib()
	{
		bitCount = 0;
	}

	/**
	 * get tool information for the library (keyword "bits")
	 */
	private void io_libbit()
	{
		if (bitCount == 0)
			lib.lowLevelSetUserBits(TextUtils.atoi(keyWord));
		bitCount++;
	}

	/**
	 * get the number of toolbits (keyword "userbits")
	 */
	private void io_libusb()
	{
		lib.lowLevelSetUserBits(TextUtils.atoi(keyWord));

		// this library came as readable dump, so don't automatically save it to disk
		lib.clearFromDisk();
	}

	/**
	 * get the number of technologies (keyword "techcount")
	 */
	private void io_libte()
	{
		varPos = INVTECHNOLOGY;
		bitCount = 0;
		int numTechs = Integer.parseInt(keyWord);
		techList = new Technology[numTechs];
		lambdaValues = new int[numTechs];
	}

	/**
	 * get the name of the technology (keyword "techname")
	 */
	private void io_libten()
	{
		curTech = Technology.findTechnology(keyWord);
		techList[bitCount++] = curTech;
	}

	/**
	 * get lambda values for each technology in library (keyword "lambda")
	 */
	private void io_lambda()
	{
		int lam = Integer.parseInt(keyWord);

		// for version 4.0 and earlier, scale lambda by 20
		if (emajor <= 4) lam *= 20;
		lambdaValues[bitCount-1] = lam;
	}

	/**
	 * get the number of cells in this library (keyword "cellcount")
	 */
	private void io_libcc()
	{
		varPos = INVLIBRARY;

		nodeProtoCount = Integer.parseInt(keyWord);
		if (nodeProtoCount == 0) return;

		// allocate a list of node prototypes for this library
		nodeProtoList = new Cell[nodeProtoCount];
		cellLambda = new double[nodeProtoCount];
		nodeProtoOffX = new double[nodeProtoCount];
		nodeProtoOffY = new double[nodeProtoCount];
		nodeInstList = new InputLibrary.NodeInstList[nodeProtoCount];
		arcInstList = new ArcInstList[nodeProtoCount];
		exportList = new ExportList[nodeProtoCount];

		for(int i=0; i<nodeProtoCount; i++)
		{
			nodeProtoList[i] = Cell.lowLevelAllocate(lib);
			if (nodeProtoList[i] == null) break;
		}
	}

	/**
	 * get the main cell of this library (keyword "maincell")
	 */
	private void io_libms()
	{
		mainCell = Integer.parseInt(keyWord);
	}

	/**
	 * get a view (keyword "view")
	 */
	private void io_libvie()
	{
		int openCurly = keyWord.indexOf('{');
		if (openCurly < 0)
		{
			System.out.println("Error on line "+lineReader.getLineNumber()+": missing '{' in view name: " + keyWord);
			return;
		}
		String fullName = keyWord.substring(0, openCurly);
		String abbrev = keyWord.substring(openCurly+1);
		int closeCurly = abbrev.indexOf('}');
		if (closeCurly < 0)
		{
			System.out.println("Error on line "+lineReader.getLineNumber()+": missing '}' in view name: " + keyWord);
			return;
		}
		abbrev = abbrev.substring(0, closeCurly);

		View v = View.findView(fullName);
		if (v == null)
		{
			v = View.newInstance(fullName, abbrev);
		} else
		{
			if (!v.getAbbreviation().equals(abbrev))
			{
				System.out.println("Error on line "+lineReader.getLineNumber()+": view "+fullName+" has abbreviation '"+abbrev+
					"' which does not match the existing abbreviation '"+v.getAbbreviation()+"'");
				return;
			}
		}
	}

	// --------------------------------- CELL PARSING METHODS ---------------------------------

	/**
	 * initialize for a new cell (keyword "***cell")
	 */
	private void io_newcel()
	{
		curCellNumber = TextUtils.atoi(keyWord);
		curNodeProto = nodeProtoList[curCellNumber];

		int tempVal = -1;
		int slashPos = keyWord.indexOf('/');
		if (slashPos >= 0) tempVal = TextUtils.atoi(keyWord.substring(slashPos+1));
		curNodeProto.setTempInt(tempVal);
		textLevel = INCELL;
		varPos = INVNODEPROTO;
	}

	/**
	 * get the name of the current cell (keyword "name")
	 */
	private void io_celnam()
	{
		cellName = CellName.parseName(keyWord);
	}

	/**
	 * get the version of the current cell (keyword "version")
	 */
	void io_celver()
	{
		curNodeProto.lowLevelPopulate(cellName.getName()+";"+keyWord+"{"+cellName.getView().getAbbreviation()+"}");
		curNodeProto.lowLevelLink();
	}

	/**
	 * get the creation date of the current cell (keyword "creationdate")
	 */
	private void io_celcre()
	{
		curNodeProto.lowLevelSetCreationDate(BinaryConstants.secondsToDate(TextUtils.atoi(keyWord)));
	}

	/**
	 * get the revision date of the current cell (keyword "revisiondate")
	 */
	private void io_celrev()
	{
		curNodeProto.lowLevelSetRevisionDate(BinaryConstants.secondsToDate(TextUtils.atoi(keyWord)));
	}

	/**
	 * get the external library file (keyword "externallibrary")
	 */
	private void io_celext()
	{
//		INTBIG len, filetype, filelen;
//		REGISTER LIBRARY *elib;
//		REGISTER CHAR *libname, *pt;
//		CHAR *filename, *libfile, *oldline2, *libfilename, *libfilepath;
//		FILE *io;
//		REGISTER BOOLEAN failed;
//		CHAR *cellname;
//		REGISTER NODEPROTO *np, *onp;
//		REGISTER NODEINST *ni;
//		TXTINPUTDATA savetxtindata;
//		REGISTER void *infstr;
//
//		// get the path to the library file
//		libfile = keyWord;
//		if (libfile[0] == '"')
//		{
//			libfile++;
//			len = estrlen(libfile) - 1;
//			if (libfile[len] == '"') libfile[len] = 0;
//		}
//
//		// see if this library is already read in
//		infstr = initinfstr();
//		addstringtoinfstr(infstr, skippath(libfile));
//		libname = returninfstr(infstr);
//		len = estrlen(libname);
//		filelen = estrlen(libfile);
//		if (len < filelen)
//		{
//			libfilename = &libfile[filelen-len-1];
//			*libfilename++ = 0;
//			libfilepath = libfile;
//		} else
//		{
//			libfilename = libfile;
//			libfilepath = x_("");
//		}
//
//		filetype = io_filetypetlib;
//		if (len > 5 && namesame(&libname[len-5], x_(".elib")) == 0)
//		{
//			libname[len-5] = 0;
//			filetype = io_filetypeblib;
//		} else
//		{
//			if (len > 4 && namesame(&libname[len-4], x_(".txt")) == 0) libname[len-4] = 0;
//		}
//		elib = getlibrary(libname);
//		if (elib == NOLIBRARY)
//		{
//			// library does not exist: see if file is there
//			io = xopen(libfilename, filetype, truepath(libfilepath), &filename);
//			if (io == 0)
//			{
//				// try the library area
//				io = xopen(libfilename, filetype, el_libdir, &filename);
//			}
//			if (io != 0)
//			{
//				xclose(io);
//				ttyputmsg(_("Reading referenced library %s"), libname);
//			} else
//			{
//				infstr = initinfstr();
//				formatinfstr(infstr, _("Reference library '%s'"), libname);
//				pt = fileselect(returninfstr(infstr), filetype, x_(""));
//				if (pt != 0)
//				{
//					estrcpy(libfile, pt);
//					filename = libfile;
//				}
//			}
//			elib = newlibrary(libname, filename);
//			if (elib == NOLIBRARY) return;
//
//			// read the external library
//			savetxtindata = io_txtindata;
//			if (io_verbose < 0 && fileLength > 0 && io_inputprogressdialog != 0)
//			{
//				(void)allocstring(&oldline2, DiaGetTextProgress(io_inputprogressdialog), el_tempcluster);
//			}
//
//			len = estrlen(libfilename);
//			io_libinputrecursivedepth++;
//			io_libinputreadmany++;
//			if (len > 4 && namesame(&libfilename[len-4], x_(".txt")) == 0)
//			{
//				// ends in ".txt", presume text file
//				failed = io_doreadtextlibrary(elib, FALSE);
//			} else
//			{
//				// all other endings: presume binary file
//				failed = io_doreadbinlibrary(elib, FALSE);
//			}
//			io_libinputrecursivedepth--;
//			if (failed) elib->userbits |= UNWANTEDLIB; else
//			{
//				// queue this library for announcement through change control
//				io_queuereadlibraryannouncement(elib);
//			}
//			io_txtindata = savetxtindata;
//			if (io_verbose < 0 && fileLength > 0 && io_inputprogressdialog != 0)
//			{
//				DiaSetProgress(io_inputprogressdialog, filePosition, fileLength);
//				infstr = initinfstr();
//				formatinfstr(infstr, _("Reading library %s"), lib->libname);
//				DiaSetCaptionProgress(io_inputprogressdialog, returninfstr(infstr));
//				DiaSetTextProgress(io_inputprogressdialog, oldline2);
//				efree(oldline2);
//			}
//		}
//
//		// find this cell in the external library
//		cellname = curNodeProto->protoname;
//		for(np = elib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//		{
//			if (namesame(np->protoname, cellname) != 0) continue;
//			if (np->cellview != curNodeProto->cellview) continue;
//			if (np->version != curNodeProto->version) continue;
//			break;
//		}
//		if (np == NONODEPROTO)
//		{
//			// cell not found in library: issue warning
//			infstr = initinfstr();
//			addstringtoinfstr(infstr, cellname);
//			if (curNodeProto->cellview != el_unknownview)
//			{
//				addtoinfstr(infstr, '{');
//				addstringtoinfstr(infstr, curNodeProto->cellview->sviewname);
//				addtoinfstr(infstr, '}');
//			}
//			ttyputerr(_("Cannot find cell %s in library %s..creating dummy version"),
//				returninfstr(infstr), elib->libname);
//		} else
//		{
//			// cell found: make sure it is valid
//			if (np->revisiondate != curNodeProto->revisiondate ||
//				np->lowx != curNodeProto->lowx ||
//				np->highx != curNodeProto->highx ||
//				np->lowy != curNodeProto->lowy ||
//				np->highy != curNodeProto->highy)
//			{
//				ttyputerr(_("Warning: cell %s in library %s has been modified"),
//					describenodeproto(np), elib->libname);
//				np = NONODEPROTO;
//			}
//		}
//		if (np != NONODEPROTO)
//		{
//			// get rid of existing cell/cell and plug in the external reference
//			onp = nodeProtoList[curCellNumber];
//			db_retractnodeproto(onp);
//			freenodeproto(onp);
//			nodeProtoList[curCellNumber] = np;
//		} else
//		{
//			// rename the cell
//			np = curNodeProto;
//			infstr = initinfstr();
//			formatinfstr(infstr, x_("%sFROM%s"), cellname, elib->libname);
//			db_retractnodeproto(np);
//
//			cellname = returninfstr(infstr);
//			(void)allocstring(&np->protoname, cellname, lib->cluster);
//			db_insertnodeproto(np);
//
//			// create an artwork "Crossed box" to define the cell size
//			ni = allocnodeinst(lib->cluster);
//			ni->proto = art_crossedboxprim;
//			ni->parent = np;
//			ni->nextnodeinst = np->firstnodeinst;
//			np->firstnodeinst = ni;
//			ni->lowx = np->lowx;   ni->highx = np->highx;
//			ni->lowy = np->lowy;   ni->highy = np->highy;
//			ni->geom = allocgeom(lib->cluster);
//			ni->geom->entryisnode = TRUE;   ni->geom->entryaddr.ni = ni;
//			linkgeom(ni->geom, np);
//		}
	}

	/**
	 * get the default technology for objects in this cell (keyword "technology")
	 */
	private void io_tech()
	{
		Technology tech = findThisTechnology(keyWord);
		curNodeProto.setTechnology(tech);
	}

	/**
	 * get tool information for current cell (keyword "bits")
	 */
	private void io_celbit()
	{
		if (bitCount == 0) curNodeProto.lowLevelSetUserbits(TextUtils.atoi(keyWord));
		bitCount++;
	}

	/**
	 * get tool information for current cell (keyword "userbits")
	 */
	private void io_celusb()
	{
		curNodeProto.lowLevelSetUserbits(TextUtils.atoi(keyWord));
	}

	/**
	 * get the number of node instances in the current cell (keyword "nodes")
	 */
	private void io_celnoc()
	{
		int nodeInstCount = Integer.parseInt(keyWord);
		if (nodeInstCount == 0) return;
		InputLibrary.NodeInstList nil = new InputLibrary.NodeInstList();
		nodeInstList[curCellNumber] = nil;
		nil.theNode = new NodeInst[nodeInstCount];
		nil.protoType = new NodeProto[nodeInstCount];
		nil.name = new Name[nodeInstCount];
		nil.lowX = new int[nodeInstCount];
		nil.highX = new int[nodeInstCount];
		nil.lowY = new int[nodeInstCount];
		nil.highY = new int[nodeInstCount];
		nil.rotation = new short[nodeInstCount];
		nil.transpose = new int[nodeInstCount];
		for(int i=0; i<nodeInstCount; i++)
		{
			nil.theNode[i] = NodeInst.lowLevelAllocate();
		}
	}

	/**
	 * get the number of arc instances in the current cell (keyword "arcs")
	 */
	private void io_celarc()
	{
		int arcInstCount = Integer.parseInt(keyWord);
		if (arcInstCount == 0) return;
		ArcInstList ail = new ArcInstList();
		arcInstList[curCellNumber] = ail;
		ail.arcList = new ArcInst[arcInstCount];
		ail.arcProto = new ArcProto[arcInstCount];
		ail.arcInstName = new Name[arcInstCount];
		ail.arcWidth = new int[arcInstCount];
		ail.arcHeadNode = new NodeInst[arcInstCount];
		ail.arcHeadPort = new String[arcInstCount];
		ail.arcHeadX = new int[arcInstCount];
		ail.arcHeadY = new int[arcInstCount];
		ail.arcTailNode = new NodeInst[arcInstCount];
		ail.arcTailPort = new String[arcInstCount];
		ail.arcTailX = new int[arcInstCount];
		ail.arcTailY = new int[arcInstCount];
		for(int i=0; i<arcInstCount; i++)
		{
			ail.arcList[i] = ArcInst.lowLevelAllocate();
		}
	}

	/**
	 * get the number of port prototypes in the current cell (keyword "porttypes")
	 */
	private void io_celptc()
	{
		int exportCount = Integer.parseInt(keyWord);
		if (exportCount == 0) return;

		ExportList el = new ExportList();
		exportList[curCellNumber] = el;
		el.exportList = new Export[exportCount];
		el.exportName = new String[exportCount];
		el.exportSubNode = new NodeInst[exportCount];
		el.exportSubPort = new String[exportCount];
		for(int i=0; i<exportCount; i++)
		{
			el.exportList[i] = Export.lowLevelAllocate();
		}
	}

	// --------------------------------- NODE INSTANCE PARSING METHODS ---------------------------------

	/**
	 * initialize for a new node instance (keyword "**node")
	 */
	private void io_newno()
	{
		curNodeInstIndex = Integer.parseInt(keyWord);
		textLevel = INNODEINST;
		varPos = INVNODEINST;
	}

	/**
	 * get the type of the current nodeinst (keyword "type")
	 */
	private void io_nodtyp()
	{
		NodeProto curNodeInstProto = null;
		int openSquare = keyWord.indexOf('[');
		if (openSquare >= 0)
		{
			curNodeInstProto = nodeProtoList[TextUtils.atoi(keyWord, openSquare+1)];
		} else
		{
			curNodeInstProto = NodeProto.findNodeProto(keyWord);
			if (curNodeInstProto == null)
			{
				// get the technology
				Technology tech = null;
				int colonPos = keyWord.indexOf(':');
				if (colonPos >= 0)
				{
					tech = Technology.findTechnology(keyWord.substring(0, colonPos));
				}
				if (tech != null)
				{
					// convert "Active-Node" to "P-Active-Node" (MOSIS CMOS)
					if (keyWord.equals("Active-Node"))
					{
						curNodeInstProto = tech.findNodeProto("P-Active-Node");
						if (curNodeInstProto == null)
						{
							// convert "message" and "cell-center" nodes
							curNodeInstProto = tech.convertOldNodeName(keyWord);
						}
					}
				}
			}
		}
		if (curNodeInstProto == null)
			System.out.println("Error on line "+lineReader.getLineNumber()+": unknown node type: "+keyWord);
		nodeInstList[curCellNumber].protoType[curNodeInstIndex] = curNodeInstProto;
	}

	/**
	 * get the bounding box information for the current node instance
	 */
	private void io_nodlx()
	{
		nodeInstList[curCellNumber].lowX[curNodeInstIndex] = TextUtils.atoi(keyWord);
	}

	private void io_nodhx()
	{
		nodeInstList[curCellNumber].highX[curNodeInstIndex] = TextUtils.atoi(keyWord);
	}

	private void io_nodly()
	{
		nodeInstList[curCellNumber].lowY[curNodeInstIndex] = TextUtils.atoi(keyWord);
	}

	private void io_nodhy()
	{
		nodeInstList[curCellNumber].highY[curNodeInstIndex] = TextUtils.atoi(keyWord);
	}

	/**
	 * get the instance name of the current node instance (keyword "name")
	 */
	private void io_nodnam()
	{
		nodeInstList[curCellNumber].theNode[curNodeInstIndex].setName(keyWord);
	}

	/**
	 * get the text descriptor of the current node instance (keyword "descript")
	 */
	private void io_noddes()
	{
		int td0 = TextUtils.atoi(keyWord);
		int td1 = 0;
		int slashPos = keyWord.indexOf('/');
		if (slashPos >= 0)
			td1 = TextUtils.atoi(keyWord.substring(slashPos+1));
		TextDescriptor td = new TextDescriptor(null, td0, td1);
		Input.fixTextDescriptorFont(td);
		nodeInstList[curCellNumber].theNode[curNodeInstIndex].setProtoTextDescriptor(td);
	}

	/**
	 * get the rotation for the current nodeinst (keyword "rotation");
	 */
	private void io_nodrot()
	{
		nodeInstList[curCellNumber].rotation[curNodeInstIndex] = (short)Integer.parseInt(keyWord);
	}

	/**
	 * get the transposition for the current nodeinst (keyword "transpose")
	 */
	private void io_nodtra()
	{
		nodeInstList[curCellNumber].transpose[curNodeInstIndex] = Integer.parseInt(keyWord);
	}

	/**
	 * get the tool seen bits for the current nodeinst (keyword "aseen")
	 */
	private void io_nodkse()
	{
		bitCount = 0;
	}

	/**
	 * get the port count for the current nodeinst (keyword "ports")
	 */
	private void io_nodpoc() {}

	/**
	 * get tool information for current nodeinst (keyword "bits")
	 */
	private void io_nodbit()
	{
		if (bitCount == 0) nodeInstList[curCellNumber].theNode[curNodeInstIndex].lowLevelSetUserbits(TextUtils.atoi(keyWord));
		bitCount++;
	}

	/**
	 * get tool information for current nodeinst (keyword "userbits")
	 */
	private void io_nodusb()
	{
		nodeInstList[curCellNumber].theNode[curNodeInstIndex].lowLevelSetUserbits(TextUtils.atoi(keyWord));
	}

	/**
	 * initialize for a new portinst on the current nodeinst (keyword "*port")
	 */
	private void io_newpor()
	{
		textLevel = INPOR;
	}

	// --------------------------------- ARC INSTANCE PARSING METHODS ---------------------------------

	/**
	 * initialize for a new arc instance (keyword "**arc")
	 */
	private void io_newar()
	{
		curArcInstIndex = Integer.parseInt(keyWord);
		textLevel = INARCINST;
		varPos = INVARCINST;
	}

	/**
	 * get the type of the current arc instance (keyword "type")
	 */
	private void io_arctyp()
	{
		ArcProto curArcInstProto = null;
		curArcInstProto = ArcProto.findArcProto(keyWord);
		if (curArcInstProto == null)
		{
			// get the technology
			Technology tech = null;
			int colonPos = keyWord.indexOf(':');
			if (colonPos >= 0)
			{
				tech = Technology.findTechnology(keyWord.substring(0, colonPos));
			}
			if (tech != null)
			{
				// convert old arcs
				curArcInstProto = tech.convertOldArcName(keyWord);
			}
		}
		if (curArcInstProto == null)
			System.out.println("Error on line "+lineReader.getLineNumber()+": unknown arc type: "+keyWord);
		arcInstList[curCellNumber].arcProto[curArcInstIndex] = curArcInstProto;
	}

	/**
	 * get the instance name of the current arc instance (keyword "name")
	 */
	private void io_arcnam()
	{
		arcInstList[curCellNumber].arcList[curArcInstIndex].setName(keyWord);
	}

	/**
	 * get the width of the current arc instance (keyword "width")
	 */
	private void io_arcwid()
	{
		arcInstList[curCellNumber].arcWidth[curArcInstIndex] = TextUtils.atoi(keyWord);
	}

	/**
	 * initialize for an end of the current arcinst (keyword "*end")
	 */
	private void io_newend()
	{
		curArcEnd = Integer.parseInt(keyWord);
		textLevel = INARCEND;
	}

	/**
	 * get the node at the current end of the current arcinst (keyword "node")
	 */
	private void io_endnod()
	{
		int endIndex = TextUtils.atoi(keyWord);
		if (curArcEnd == 0)
		{
			arcInstList[curCellNumber].arcHeadNode[curArcInstIndex] = nodeInstList[curCellNumber].theNode[endIndex];
		} else
		{
			arcInstList[curCellNumber].arcTailNode[curArcInstIndex] = nodeInstList[curCellNumber].theNode[endIndex];
		}
	}

	/**
	 * get the porttype at the current end of current arcinst (keyword "nodeport")
	 */
	private void io_endpt()
	{
		if (curArcEnd == 0)
		{
			arcInstList[curCellNumber].arcHeadPort[curArcInstIndex] = keyWord;
		} else
		{
			arcInstList[curCellNumber].arcTailPort[curArcInstIndex] = keyWord;
		}
	}

	/**
	 * get the coordinates of the current end of the current arcinst
	 */
	private void io_endxp()
	{
		int x = TextUtils.atoi(keyWord);
		if (curArcEnd == 0)
		{
			arcInstList[curCellNumber].arcHeadX[curArcInstIndex] = x;
		} else
		{
			arcInstList[curCellNumber].arcTailX[curArcInstIndex] = x;
		}
	}

	private void io_endyp()
	{
		int y = TextUtils.atoi(keyWord);
		if (curArcEnd == 0)
		{
			arcInstList[curCellNumber].arcHeadY[curArcInstIndex] = y;
		} else
		{
			arcInstList[curCellNumber].arcTailY[curArcInstIndex] = y;
		}
	}

	/**
	 * get the tool information for the current arcinst (keyword "aseen")
	 */
	private void io_arckse()
	{
		bitCount = 0;
	}

	/**
	 * get tool information for current arcinst (keyword "bits")
	 */
	private void io_arcbit()
	{
		if (bitCount == 0) arcInstList[curCellNumber].arcList[curArcInstIndex].lowLevelSetUserbits(TextUtils.atoi(keyWord));
		bitCount++;
	}

	/**
	 * get tool information for current arcinst (keyword "userbits")
	 */
	private void io_arcusb()
	{
		arcInstList[curCellNumber].arcList[curArcInstIndex].lowLevelSetUserbits(TextUtils.atoi(keyWord));
	}

	// --------------------------------- PORT PROTOTYPE PARSING METHODS ---------------------------------

	/**
	 * initialize for a new port prototype (keyword "**porttype")
	 */
	private void io_newpt()
	{
		curExportIndex = Integer.parseInt(keyWord);
		textLevel = INPORTPROTO;
		varPos = INVPORTPROTO;
	}

	/**
	 * get the name for the current port prototype (keyword "name")
	 */
	private void io_ptnam()
	{
		exportList[curCellNumber].exportName[curExportIndex] = keyWord;
	}

	/**
	 * get the text descriptor for the current port prototype (keyword "descript")
	 */
	private void io_ptdes()
	{
		int td0 = TextUtils.atoi(keyWord);
		int td1 = 0;
		int slashPos = keyWord.indexOf('/');
		if (slashPos >= 0)
			td1 = TextUtils.atoi(keyWord, slashPos+1);
		TextDescriptor td = new TextDescriptor(null, td0, td1);
		Input.fixTextDescriptorFont(td);
		exportList[curCellNumber].exportList[curExportIndex].setTextDescriptor(td);
	}

	/**
	 * get the sub-nodeinst for the current port prototype (keyword "subnode")
	 */
	private void io_ptsno()
	{
		int index = Integer.parseInt(keyWord);
		exportList[curCellNumber].exportSubNode[curExportIndex] = nodeInstList[curCellNumber].theNode[index];
	}

	/**
	 * get the sub-portproto for the current port prototype (keyword "subport")
	 */
	private void io_ptspt()
	{
		exportList[curCellNumber].exportSubPort[curExportIndex] = keyWord;
	}

	/**
	 * get the tool seen for the current port prototype (keyword "aseen")
	 */
	private void io_ptkse()
	{
		bitCount = 0;
	}

	/**
	 * get the tool data for the current port prototype (keyword "bits")
	 */
	private void io_ptbit()
	{
		if (bitCount == 0) exportList[curCellNumber].exportList[curExportIndex].lowLevelSetUserbits(TextUtils.atoi(keyWord));
		bitCount++;
	}

	/**
	 * get the tool data for the current port prototype (keyword "userbits")
	 */
	private void io_ptusb()
	{
		exportList[curCellNumber].exportList[curExportIndex].lowLevelSetUserbits(TextUtils.atoi(keyWord));
	}

	// --------------------------------- VARIABLE PARSING METHODS ---------------------------------

	/**
	 * get variables on current object (keyword "variables")
	 */
	private void io_getvar()
		throws IOException
	{
		ElectricObject naddr = null;
		switch (varPos)
		{
			case INVTOOL:				// keyword applies to tools
				naddr = curTool;
				break;
			case INVTECHNOLOGY:			// keyword applies to technologies
				naddr = curTech;
				break;
			case INVLIBRARY:			// keyword applies to library
				naddr = lib;
				break;
			case INVNODEPROTO:			// keyword applies to nodeproto
				naddr = curNodeProto;
				break;
			case INVNODEINST:			// keyword applies to nodeinst
				naddr = nodeInstList[curCellNumber].theNode[curNodeInstIndex];
				break;
			case INVPORTPROTO:			// keyword applies to portproto
				naddr = exportList[curCellNumber].exportList[curExportIndex];
				break;
			case INVARCINST:			// keyword applies to arcinst
				naddr = arcInstList[curCellNumber].arcList[curArcInstIndex];
				break;
		}

		// find out how many variables to read
		int count = Integer.parseInt(keyWord);
		for(int i=0; i<count; i++)
		{
			// read the first keyword with the name, type, and descriptor
			if (getKeyword())
			{
				System.out.println("EOF too soon");
				return;
			}

			// get the variable name
			String varName = "";
			int len = keyWord.length();
			if (keyWord.charAt(len-1) != ':')
			{
				System.out.println("Error on line "+lineReader.getLineNumber()+": missing colon in variable specification: "+keyWord);
				return;
			}
			for(int j=0; j<len; j++)
			{
				char cat = keyWord.charAt(j);
				if (cat == '^' && j < len-1)
				{
					j++;
					varName += keyWord.charAt(j);
					continue;
				}
				if (cat == '(' || cat == '[' || cat == ':') break;
				varName += cat;
			}
			Variable.Key varKey = ElectricObject.newKey(varName);

			// see if the variable is valid
			boolean invalid = false;
			if (naddr == null) invalid = true; else
				invalid = naddr.isDeprecatedVariable(varKey);

			// get type
			int openSquarePos = keyWord.indexOf('[');
			if (openSquarePos < 0)
			{
				System.out.println("Error on line "+lineReader.getLineNumber()+": missing type information in variable: " + keyWord);
				return;
			}
			int type = TextUtils.atoi(keyWord, openSquarePos+1);

			// get the descriptor
			int commaPos = keyWord.indexOf(',');
			int td0 = 0;
			int td1 = 0;
			if (commaPos >= 0)
			{
				td0 = TextUtils.atoi(keyWord, commaPos+1);
				td1 = 0;
				int slashPos = keyWord.indexOf('/');
				if (slashPos >= 0)
					td1 = TextUtils.atoi(keyWord, slashPos+1);
			}
			TextDescriptor td = new TextDescriptor(null, td0, td1);

			// get value
			if (getKeyword())
			{
				System.out.println("EOF too soon");
				return;
			}
			Object value = null;
			if ((type&BinaryConstants.VISARRAY) == 0)
			{
				value = io_decode(keyWord, type);
			} else
			{
				if (keyWord.charAt(0) != '[')
				{
					System.out.println("Error on line "+lineReader.getLineNumber()+": missing '[' in list of variable values: " + keyWord);
					return;
				}
				ArrayList al = new ArrayList();
				int pos = 1;
				len = keyWord.length();
				for(;;)
				{
					// string arrays must be handled specially
					int start = pos;
					if ((type&BinaryConstants.VTYPE) == BinaryConstants.VSTRING)
					{
						while (keyWord.charAt(pos) != '"' && pos < len-1) pos++;
						start = pos;
						if (pos < len)
						{
							pos++;
							for(;;)
							{
								if (keyWord.charAt(pos) == '^' && pos < len-1)
								{
									pos += 2;
									continue;
								}
								if (keyWord.charAt(pos) == '"' || pos == len-1) break;
								pos++;
							}
							if (pos < len) pos++;
						}
					} else
					{
						while (keyWord.charAt(pos) != ',' && keyWord.charAt(pos) != ']' && pos < len) pos++;
					}
					if (pos >= len)
					{
						System.out.println("Error on line "+lineReader.getLineNumber()+": array too short in variable values: " + keyWord);
						return;
					}
					String entry = keyWord.substring(start, pos);
					al.add(io_decode(entry, type));
					if (keyWord.charAt(pos) == ']') break;
					if (keyWord.charAt(pos) != ',')
					{
						System.out.println("Error on line "+lineReader.getLineNumber()+": missing comma between array entries: " + keyWord);
						return;
					}
					pos++;
				}
				int arrayLen = al.size();
				switch (type&BinaryConstants.VTYPE)
				{
					case BinaryConstants.VADDRESS:
					case BinaryConstants.VINTEGER:    value = new Integer[arrayLen];     break;
					case BinaryConstants.VFRACT:      
					case BinaryConstants.VFLOAT:      value = new Float[arrayLen];       break;
					case BinaryConstants.VDOUBLE:     value = new Double[arrayLen];      break;
					case BinaryConstants.VSHORT:      value = new Short[arrayLen];       break;
					case BinaryConstants.VBOOLEAN:
					case BinaryConstants.VCHAR:       value = new Byte[arrayLen];        break;
					case BinaryConstants.VSTRING:     value = new String[arrayLen];      break;
					case BinaryConstants.VNODEINST:   value = new NodeInst[arrayLen];    break;
					case BinaryConstants.VNODEPROTO:  value = new NodeProto[arrayLen];   break;
					case BinaryConstants.VARCPROTO:   value = new ArcProto[arrayLen];    break;
					case BinaryConstants.VPORTPROTO:  value = new PortProto[arrayLen];   break;
					case BinaryConstants.VARCINST:    value = new ArcInst[arrayLen];     break;
					case BinaryConstants.VTECHNOLOGY: value = new Technology[arrayLen];  break;
					case BinaryConstants.VLIBRARY:    value = new Library[arrayLen];     break;
					case BinaryConstants.VTOOL:       value = new Tool[arrayLen];        break;
				}
				if (value != null)
				{
					for(int j=0; j<arrayLen; j++)
					{
						((Object [])value)[j] = al.get(j);
					}
				}
			}
			// Geometric names are saved as variables.
			if (value instanceof String)
			{
				if ((naddr instanceof NodeInst && varKey == NodeInst.NODE_NAME) ||
					(naddr instanceof ArcInst && varKey == ArcInst.ARC_NAME))
				{
					Geometric geom = (Geometric)naddr;
					Input.fixTextDescriptorFont(td);
					geom.setNameTextDescriptor(td);
					Name name = makeGeomName(geom, value, type);
					if (naddr instanceof NodeInst)
						nodeInstList[curCellNumber].name[curNodeInstIndex] = name;
					else
						arcInstList[curCellNumber].arcInstName[curArcInstIndex] = name;
					continue;
				}
			}
			if (!invalid)
			{
				Variable var = naddr.newVar(varKey, value);
				if (var == null)
				{
					System.out.println("Error on line "+lineReader.getLineNumber()+": cannot store array variable: " + keyWord);
					return;
				}
				var.setDescriptor(td);
				var.lowLevelSetFlags(type);

				// handle updating of technology caches
//				if (naddr instanceof Technology)
//					changedtechnologyvariable(key);
			}
		}
		if (varPos == INVLIBRARY)
		{
			// cache the font associations
			Input.getFontAssociationVariable(lib);
		}
		Input.fixVariableFont(naddr);
	}

	private Object io_decode(String name, int type)
	{
		int thistype = type;
		if ((thistype&(BinaryConstants.VCODE1|BinaryConstants.VCODE2)) != 0) thistype = BinaryConstants.VSTRING;

		switch (thistype&BinaryConstants.VTYPE)
		{
			case BinaryConstants.VINTEGER:
			case BinaryConstants.VSHORT:
			case BinaryConstants.VBOOLEAN:
			case BinaryConstants.VADDRESS:
				return new Integer(TextUtils.atoi(name));
			case BinaryConstants.VFRACT:
				return new Float(TextUtils.atoi(name) / 120.0f);
			case BinaryConstants.VCHAR:
				return new Character(name.charAt(0));
			case BinaryConstants.VSTRING:
				char [] letters = new char[name.length()];
				int outpos = 0;
				int inpos = 0;
				if (name.charAt(inpos) == '"') inpos++;
				for( ; inpos < name.length(); inpos++)
				{
					if (name.charAt(inpos) == '^' && inpos < name.length()-1)
					{
						inpos++;
						letters[outpos++] = name.charAt(inpos);
						continue;
					}
					if (name.charAt(inpos) == '"') break;
					letters[outpos++] = name.charAt(inpos);
				}
				return new String(letters, 0, outpos);
			case BinaryConstants.VFLOAT:
				return new Float(Float.parseFloat(name));
			case BinaryConstants.VDOUBLE:
				return new Double(Double.parseDouble(name));
			case BinaryConstants.VNODEINST:
				int niIndex = TextUtils.atoi(name);
				NodeInst ni = nodeInstList[curCellNumber].theNode[niIndex];
				return ni;
			case BinaryConstants.VNODEPROTO:
				int colonPos = name.indexOf(':');
				if (colonPos < 0)
				{
					// just an integer specification
					int cindex = Integer.parseInt(name);
					return nodeProtoList[cindex];
				} else
				{
					// parse primitive nodeproto name
					NodeProto np = NodeProto.findNodeProto(name);
					if (np == null)
					{
						System.out.println("Error on line "+lineReader.getLineNumber()+": cannot find node " + name);
						return null;
					}
					return np;
				}
			case BinaryConstants.VPORTPROTO:
				int ppIndex = TextUtils.atoi(name);
				PortProto pp = exportList[curCellNumber].exportList[ppIndex];
				return pp;
			case BinaryConstants.VARCINST:
				int aiIndex = TextUtils.atoi(name);
				ArcInst ai = arcInstList[curCellNumber].arcList[aiIndex];
				return ai;
			case BinaryConstants.VARCPROTO:
				return ArcProto.findArcProto(name);
			case BinaryConstants.VTECHNOLOGY:
				return Technology.findTechnology(name);
			case BinaryConstants.VTOOL:
				return Tool.findTool(name);
		}
		return null;
	}

	// --------------------------------- CONVERSTION METHODS ---------------------------------

	/**
	 * Method to convert the technology name in "line" to a technology.
	 * also handles conversion of the old technology name "logic"
	 */
	private Technology findThisTechnology(String line)
	{
		Technology tech = null;
		if (convertMosisCMOSTechnologies)
		{
			if (line.equals("mocmossub")) tech = Technology.findTechnology("mocmos"); else
				if (line.equals("mocmos")) tech = Technology.findTechnology("mocmosold");
		}
		if (tech == null) tech = Technology.findTechnology(line);
		if (tech != null) return tech;
		if (line.equals("logic")) tech = Technology.findTechnology("schematics");
		return tech;
	}

	/**
	 * helper method to parse a port prototype name "line" that should be
	 * in node prototype "np".  The method returns NOPORTPROTO if it cannot
	 * figure out what port this name refers to.
	 */
//	private PortProto io_getport(String line, NodeProto np)
//	{
//		PortProto pp = np.findPortProto(line);
//		if (pp != null) return pp;
//
//		// convert special port names
//		if (np instanceof PrimitiveNode)
//		{
//			PrimitiveNode pnp = (PrimitiveNode)np;
//			pp = pnp.getTechnology().convertOldPortName(line, pnp);
//			if (pp != null) return pp;
//		}
//
//		// try to parse version 1 port names
//		if (emajor == 1)
//		{
//			// see if database uses shortened name
//			for(Iterator it = np.getPorts(); it.hasNext(); )
//			{
//				pp = (PortProto)it.next();
//				String name = pp.getProtoName();
//				if (line.startsWith(name)) return pp;
//			}
//
//			// see if the port name ends in a digit, fake with that
//			int len = line.length();
//			if (len > 2 && line.charAt(len-2) == '-')
//			{
//				char chr = line.charAt(len-1);
//				if (Character.isDigit(chr))
//				{
//					int i = (chr-'0'-1) / 3;
//					for(Iterator it = np.getPorts(); it.hasNext(); )
//					{
//						pp = (PortProto)it.next();
//						if (i-- == 0) return(pp);
//					}
//				}
//			}
//		}
//
//		// sorry, cannot figure out what port prototype this is
//		return null;
//	}

}
