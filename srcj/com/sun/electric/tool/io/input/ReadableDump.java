/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ReadableDump.java
 * Input/output tool: "Readable Dump" Library input
 * Written by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.ELIBConstants;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class reads files in readable-dump (.txt) format.
 */
public class ReadableDump extends LibraryFiles
{
	// ------------------------- private data ----------------------------
	private static class ArcInstList
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
	private static class ExportList
	{
		private Export []   exportList;
		private String []   exportName;
		private NodeInst [] exportSubNode;
		private String []   exportSubPort;
	};

	/** The current position in the file. */						private int filePosition;
	/** The state of reading the file. */							private int textLevel;
	/** A counter for reading multiple Tools, Technologies, etc. */	private int bitCount;
	/** The current Cell in the Library. */							private int mainCell;
	/** The current ArcInst end being processed. */					private int curArcEnd;
	/** The current object type being processed. */					private int varPos;
	/** The current Cell being processed. */						private Cell curCell;
	/** The index of the current Cell being processed. */			private int curCellNumber;
	/** The index of the current NodeInst being processed. */		private int curNodeInstIndex;
	/** The index of the current ArcInst being processed. */		private int curArcInstIndex;
	/** The index of the current Export being processed. */			private int curExportIndex;
	/** Offset values for cells being read. */						private double [] nodeProtoOffX, nodeProtoOffY;
	/** All data for NodeInsts in each Cell. */						private LibraryFiles.NodeInstList[] nodeInstList;
	/** All data for ArcInsts in each Cell. */						private ArcInstList[] arcInstList;
	/** All data for Exports in each Cell. */						private ExportList [] exportList;
	/** The maximum characters in a keyword. */						private int keywordArrayLen;
	/** An array of keyword data. */								private char [] keywordArray = null;
	/** The current keyword from the file. */						private String keyWord;
	/** All Tools found in the file. */								private Tool [] toolList;
	/** The current Tool being processed. */						private Tool curTool;
	/** All Technologies found in the file. */						private Technology [] techList;
	/** The current Technology being processed. */					private Technology curTech;
	/** The values of Lambda for each Technology. */				private int [] lambdaValues;
	/** The group number of the current Cell being processed. */	private int curCellGroup;
	/** The CellName of the current Cell being processed. */		private CellName curCellName;
	/** The creation date of the current Cell being processed. */	private int curCellCreationDate;
	/** The revision date of the current Cell being processed. */	private int curCellRevisionDate;
	/** The userbits of the current Cell being processed. */		private int curCellUserbits;
	/** The low X bounds of the current Cell being processed. */	private int curCellLowX;
	/** The high X bounds of the current Cell being processed. */	private int curCellHighX;
	/** The low Y bounds of the current Cell being processed. */	private int curCellLowY;
	/** The high Y bounds of the current Cell being processed. */	private int curCellHighY;
	/** cells being processed from this or external libraries. */	private Cell [] allCellsArray;

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

	ReadableDump()
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
					if (thisKey.equals("****library:")) {     keywordNewLib();   break;   }
					if (thisKey.equals("bits:")) {            keywordLibBit();   break;   }
					if (thisKey.equals("lambda:")) {          keywordLambda();   break;   }
					if (thisKey.equals("version:")) {         keywordVersn();    break;   }
					if (thisKey.equals("aids:")) {            keywordLibKno();   break;   }
					if (thisKey.equals("aidname:")) {         keywordLibAiN();   break;   }
					if (thisKey.equals("aidbits:")) {         keywordLibAiB();   break;   }
					if (thisKey.equals("userbits:")) {        keywordLibUsb();   break;   }
					if (thisKey.equals("techcount:")) {       keywordLibTe();    break;   }
					if (thisKey.equals("techname:")) {        keywordLibTeN();   break;   }
					if (thisKey.equals("cellcount:")) {       keywordLibCC();    break;   }
					if (thisKey.equals("maincell:")) {        keywordLibMS();    break;   }
					if (thisKey.equals("view:")) {            keywordLibVie();   break;   }
					if (thisKey.equals("***cell:")) {         keywordNewCel();   break;   }
					if (thisKey.equals("variables:")) {       keywordGetVar();   break;   }
					 break;
				case INCELL:
					if (thisKey.equals("bits:")) {            keywordCelBit();   break;   }
					if (thisKey.equals("userbits:")) {        keywordCelUsb();   break;   }
					if (thisKey.equals("name:")) {            keywordCelNam();   break;   }
					if (thisKey.equals("version:")) {         keywordCelVer();   break;   }
					if (thisKey.equals("creationdate:")) {    keywordCelCre();   break;   }
					if (thisKey.equals("revisiondate:")) {    keywordCelRev();   break;   }
					if (thisKey.equals("externallibrary:")) { keywordCelExt();   break;   }
					if (thisKey.equals("lowx:")) {            keywordCelLX();    break;   }
					if (thisKey.equals("highx:")) {           keywordCelHX();    break;   }
					if (thisKey.equals("lowy:")) {            keywordCelLY();    break;   }
					if (thisKey.equals("highy:")) {           keywordCelHY();    break;   }
					if (thisKey.equals("nodes:")) {           keywordCelNoC();   break;   }
					if (thisKey.equals("arcs:")) {            keywordCelArC();   break;   }
					if (thisKey.equals("porttypes:")) {       keywordCelPtC();   break;   }
					if (thisKey.equals("technology:")) {      keywordTech();     break;   }
					if (thisKey.equals("**node:")) {          keywordNewNo();    break;   }
					if (thisKey.equals("**arc:")) {           keywordNewAr();    break;   }
					if (thisKey.equals("***cell:")) {         keywordNewCel();   break;   }
					if (thisKey.equals("variables:")) {       keywordGetVar();   break;   }
					break;
				case INPORTPROTO:
					if (thisKey.equals("bits:")) {            keywordPtBit();    break;   }
					if (thisKey.equals("userbits:")) {        keywordPtUsb();    break;   }
					if (thisKey.equals("subnode:")) {         keywordPtSNo();    break;   }
					if (thisKey.equals("subport:")) {         keywordPtSPt();    break;   }
					if (thisKey.equals("name:")) {            keywordPtNam();    break;   }
					if (thisKey.equals("descript:")) {        keywordPtDes();    break;   }
					if (thisKey.equals("aseen:")) {           keywordPtKse();    break;   }
					if (thisKey.equals("**porttype:")) {      keywordNewPt();    break;   }
					if (thisKey.equals("**arc:")) {           keywordNewAr();    break;   }
					if (thisKey.equals("**node:")) {          keywordNewNo();    break;   }
					if (thisKey.equals("***cell:")) {         keywordNewCel();   break;   }
					if (thisKey.equals("variables:")) {       keywordGetVar();   break;   }
					break;
				case INNODEINST:
					if (thisKey.equals("bits:")) {            keywordNodBit();   break;   }
					if (thisKey.equals("userbits:")) {        keywordNodUsb();   break;   }
					if (thisKey.equals("type:")) {            keywordNodTyp();   break;   }
					if (thisKey.equals("lowx:")) {            keywordNodLX();    break;   }
					if (thisKey.equals("highx:")) {           keywordNodHX();    break;   }
					if (thisKey.equals("lowy:")) {            keywordNodLY();    break;   }
					if (thisKey.equals("highy:")) {           keywordNodHY();    break;   }
					if (thisKey.equals("rotation:")) {        keywordNodRot();   break;   }
					if (thisKey.equals("transpose:")) {       keywordNodTra();   break;   }
					if (thisKey.equals("aseen:")) {           keywordNodKse();   break;   }
					if (thisKey.equals("name:")) {            keywordNodNam();   break;   }
					if (thisKey.equals("descript:")) {        keywordNodDes();   break;   }
					if (thisKey.equals("*port:")) {           keywordNewPor();   break;   }
					if (thisKey.equals("**node:")) {          keywordNewNo();    break;   }
					if (thisKey.equals("**porttype:")) {      keywordNewPt();    break;   }
					if (thisKey.equals("**arc:")) {           keywordNewAr();    break;   }
					if (thisKey.equals("variables:")) {       keywordGetVar();   break;   }
					if (thisKey.equals("***cell:")) {         keywordNewCel();   break;   }
					if (thisKey.equals("ports:")) {           keywordNodPoC();   break;   }
					break;
				case INPOR:
					if (thisKey.equals("*port:")) {           keywordNewPor();   break;   }
					if (thisKey.equals("**node:")) {          keywordNewNo();    break;   }
					if (thisKey.equals("**porttype:")) {      keywordNewPt();    break;   }
					if (thisKey.equals("**arc:")) {           keywordNewAr();    break;   }
					if (thisKey.equals("variables:")) {       keywordGetVar();   break;   }
					if (thisKey.equals("***cell:")) {         keywordNewCel();   break;   }
					break;
				case INARCINST:
					if (thisKey.equals("bits:")) {            keywordArcBit();   break;   }
					if (thisKey.equals("userbits:")) {        keywordArcUsb();   break;   }
					if (thisKey.equals("type:")) {            keywordArcTyp();   break;   }
					if (thisKey.equals("width:")) {           keywordArcWid();   break;   }
					if (thisKey.equals("aseen:")) {           keywordArcKse();   break;   }
					if (thisKey.equals("name:")) {            keywordArcNam();   break;   }
					if (thisKey.equals("*end:")) {            keywordNewEnd();   break;   }
					if (thisKey.equals("**arc:")) {           keywordNewAr();    break;   }
					if (thisKey.equals("**node:")) {          keywordNewNo();    break;   }
					if (thisKey.equals("variables:")) {       keywordGetVar();   break;   }
					if (thisKey.equals("***cell:")) {         keywordNewCel();   break;   }
					break;
				case INARCEND:
					if (thisKey.equals("node:")) {            keywordEndNod();   break;   }
					if (thisKey.equals("nodeport:")) {        keywordEndPt();    break;   }
					if (thisKey.equals("xpos:")) {            keywordEndXP();    break;   }
					if (thisKey.equals("ypos:")) {            keywordEndYP();    break;   }
					if (thisKey.equals("*end:")) {            keywordNewEnd();   break;   }
					if (thisKey.equals("**arc:")) {           keywordNewAr();    break;   }
					if (thisKey.equals("**node:")) {          keywordNewNo();    break;   }
					if (thisKey.equals("variables:")) {       keywordGetVar();   break;   }
					if (thisKey.equals("***cell:")) {         keywordNewCel();   break;   }
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
//			buildcellgrouppointersfromnames(lib);
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

		if (allCellsArray == null) return true; // error
		if (mainCell >= 0)
			lib.setCurCell(allCellsArray[mainCell]);

		lib.clearChangedMajor();
		lib.clearChangedMinor();
		return false;
	}

	//************************************* recursive

	/**
	 * Method to recursively create the contents of each cell in the library.
	 */
	protected void realizeCellsRecursively(Cell cell, FlagSet markCellForNodes, String scaledCellName, double scaleX, double scaleY)
	{
		// do not realize cross-library references
		if (cell.getLibrary() != lib) return;

		// cannot do scaling yet
		if (scaledCellName != null) return;

		// recursively scan the nodes to the bottom and only proceed when everything below is built
		int cellIndex = cell.getTempInt();
		if (nodeInstList == null)
			return; // error
		LibraryFiles.NodeInstList nil = nodeInstList[cellIndex];
		int numNodes = 0;
		NodeProto [] nodePrototypes = null;
		if (nil != null)
		{
			nodePrototypes = nil.protoType;
			numNodes = nodePrototypes.length;
		}
		scanNodesForRecursion(cell, markCellForNodes, nodePrototypes, 0, numNodes);

		// report progress
		if (LibraryFiles.VERBOSE)
			System.out.println("Text: Doing contents of cell " + cell.describe() + " in library " + lib.getName());
		cellsConstructed++;
		progress.setProgress(cellsConstructed * 100 / totalCells);

		// now fill in the nodes
		double lambda = cellLambda[cellIndex];
		Point2D offset = realizeNode(cell, nil, lambda, lambda);
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
		LibraryFiles.NodeInstList nil = nodeInstList[cellIndex];
		int numNodes = 0;
		if (nil != null) numNodes = nil.protoType.length;
		double thisLambda = cellLambda[cellIndex];
		for(int i=0; i<numNodes; i++)
		{
			NodeProto np = nil.protoType[i];
			if (np == null) continue;
			if (np instanceof PrimitiveNode) continue;
			Cell subCell = (Cell)np;

			LibraryFiles reader = this;
			if (subCell.getLibrary() != lib)
			{
				reader = getReaderForLib(subCell.getLibrary());
				if (reader == null) continue;
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
		LibraryFiles.NodeInstList nil = nodeInstList[cellIndex];
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
		cell.setTechnology(cellTech);
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

	private Point2D realizeNode(Cell cell, LibraryFiles.NodeInstList nil, double lambdaX, double lambdaY)
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
				Point2D center = new Point2D.Double(xoff / lambdaX, yoff / lambdaY);
				double width = (highX - lowX) / lambdaX;
				double height = (highY - lowY) / lambdaY;
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
			Point2D center = new Point2D.Double((double)(cX-xoff) / lambdaX, (double)(cY-yoff) / lambdaY);
			double width = (highX - lowX) / lambdaX;
			double height = (highY - lowY) / lambdaY;
			int rotation = nil.rotation[j];

			if (rotationMirrorBits)
//			if (emajor > 7 || (emajor == 7 && eminor >= 1))
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
				AffineTransform trans = NodeInst.pureRotate(rotation, width < 0, height < 0);
				trans.transform(shift, shift);
				center.setLocation(center.getX() + shift.getX(), center.getY() + shift.getY());
			}
			ni.lowLevelPopulate(np, center, width, height, rotation, cell);
			if (name != null) ni.setNameKey(name);
			ni.lowLevelLink();

			// convert outline information, if present
			scaleOutlineInformation(ni, np, lambdaX, lambdaY);
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
			PortInst pi = el.exportSubNode[j].findPortInst(el.exportSubPort[j]);
			int userBits = pp.lowLevelGetUserbits();
			if (pp.lowLevelPopulate(pi)) return;
			pp.lowLevelSetUserbits(userBits);
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
			ai.lowLevelPopulate(ap, width, headPortInst, headPt, tailPortInst, tailPt, defAngle);
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
	private void keywordNewLib()
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
	private void keywordVersn()
	{
		version = Version.parseVersion(keyWord);
// 		emajor = version.getMajor();
// 		eminor = version.getMinor();
// 		edetail = version.getDetail();

		// for versions before 6.03q, convert MOSIS CMOS technology names
		convertMosisCmosTechnologies = version.compareTo(Version.parseVersion("6.03q")) < 0;
// 		convertMosisCmosTechnologies = false;
// 		if (emajor < 6 ||
// 			(emajor == 6 && eminor < 3) ||
// 			(emajor == 6 && eminor == 3 && edetail < 17))
// 		{
// 			convertMosisCmosTechnologies = true;
// 			System.out.println("   Converting MOSIS CMOS technologies (mocmossub => mocmos)");
// 		}

		// for Electric version 4 or earlier, scale lambda by 20
		scaleLambdaBy20 = version.compareTo(Version.parseVersion("5")) < 0;
// 		if (emajor <= 4) lambda *= 20;

		// mirror bits
		rotationMirrorBits = version.compareTo(Version.parseVersion("7.01")) >= 0;
//		if (emajor > 7 || (emajor == 7 && eminor >= 1))
	}

	/**
	 * get the number of tools (keyword "aids")
	 */
	private void keywordLibKno()
	{
		bitCount = 0;
		toolList = new Tool[Integer.parseInt(keyWord)];
	}

	/**
	 * get the name of the tool (keyword "aidname")
	 */
	private void keywordLibAiN()
	{
		curTool = Tool.findTool(keyWord);
		toolList[bitCount++] = curTool;
	}

	/**
	 * get the number of toolbits (keyword "aidbits")
	 */
	private void keywordLibAiB()
	{
		bitCount = 0;
	}

	/**
	 * get tool information for the library (keyword "bits")
	 */
	private void keywordLibBit()
	{
		if (bitCount == 0)
			lib.lowLevelSetUserBits(TextUtils.atoi(keyWord));
		bitCount++;
	}

	/**
	 * get the number of toolbits (keyword "userbits")
	 */
	private void keywordLibUsb()
	{
		lib.lowLevelSetUserBits(TextUtils.atoi(keyWord));

		// this library came as readable dump, so don't automatically save it to disk
		lib.clearFromDisk();
	}

	/**
	 * get the number of technologies (keyword "techcount")
	 */
	private void keywordLibTe()
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
	private void keywordLibTeN()
	{
		curTech = Technology.findTechnology(keyWord);
		techList[bitCount++] = curTech;
	}

	/**
	 * get lambda values for each technology in library (keyword "lambda")
	 */
	private void keywordLambda()
	{
		int lam = Integer.parseInt(keyWord);

		// for version 4.0 and earlier, scale lambda by 20
		if (scaleLambdaBy20) lam *= 20;
// 		if (emajor <= 4) lam *= 20;
		lambdaValues[bitCount-1] = lam;
	}

	/**
	 * get the number of cells in this library (keyword "cellcount")
	 */
	private void keywordLibCC()
	{
		varPos = INVLIBRARY;

		nodeProtoCount = Integer.parseInt(keyWord);
		if (nodeProtoCount == 0) return;

		// allocate a list of node prototypes for this library
		nodeProtoList = new Cell[nodeProtoCount];
		allCellsArray = new Cell[nodeProtoCount];
		cellLambda = new double[nodeProtoCount];
		nodeProtoOffX = new double[nodeProtoCount];
		nodeProtoOffY = new double[nodeProtoCount];
		nodeInstList = new LibraryFiles.NodeInstList[nodeProtoCount];
		arcInstList = new ArcInstList[nodeProtoCount];
		exportList = new ExportList[nodeProtoCount];

		for(int i=0; i<nodeProtoCount; i++)
		{
			allCellsArray[i] = nodeProtoList[i] = Cell.lowLevelAllocate(lib);
			if (nodeProtoList[i] == null) break;
		}
	}

	/**
	 * get the main cell of this library (keyword "maincell")
	 */
	private void keywordLibMS()
	{
		mainCell = Integer.parseInt(keyWord);
	}

	/**
	 * get a view (keyword "view")
	 */
	private void keywordLibVie()
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
			v = findOldViewName(fullName);
			if (v != null) abbrev = v.getAbbreviation();
		}
		if (v == null)
		{
			v = View.newInstance(fullName, abbrev);
		} else
		{
			if (!v.getAbbreviation().equals(abbrev))
			{
				System.out.println("Error on line " + lineReader.getLineNumber() + ": view " + fullName + " has abbreviation '" + abbrev +
					"' which does not match the existing abbreviation '" + v.getAbbreviation() + "'");
				return;
			}
		}
	}

	// --------------------------------- CELL PARSING METHODS ---------------------------------
	
	/**
	 * initialize for a new cell (keyword "***cell")
	 */
	private void keywordNewCel()
	{
		curCellNumber = TextUtils.atoi(keyWord);
		curCell = nodeProtoList[curCellNumber];

		curCellGroup = -1;
		int slashPos = keyWord.indexOf('/');
		if (slashPos >= 0) curCellGroup = TextUtils.atoi(keyWord.substring(slashPos+1));
		textLevel = INCELL;
		varPos = INVNODEPROTO;
	}

	/**
	 * get the name of the current cell (keyword "name")
	 */
	private void keywordCelNam()
	{
		curCellName = CellName.parseName(keyWord);
	}

	/**
	 * get the version of the current cell (keyword "version")
	 */
	private void keywordCelVer()
	{
        curCellName = CellName.newName(curCellName.getName(), curCellName.getView(), TextUtils.atoi(keyWord));
		//curCellName.setVersion(TextUtils.atoi(keyWord));
	}

	/**
	 * get the creation date of the current cell (keyword "creationdate")
	 */
	private void keywordCelCre()
	{
		curCellCreationDate = TextUtils.atoi(keyWord);
	}

	/**
	 * get the revision date of the current cell (keyword "revisiondate")
	 */
	private void keywordCelRev()
	{
		curCellRevisionDate = TextUtils.atoi(keyWord);
	}

	/**
	 * get the low X of the current cell (keyword "lowx")
	 */
	private void keywordCelLX()
	{
		curCellLowX = TextUtils.atoi(keyWord);
	}

	/**
	 * get the high X of the current cell (keyword "highx")
	 */
	private void keywordCelHX()
	{
		curCellHighX = TextUtils.atoi(keyWord);
	}

	/**
	 * get the low Y of the current cell (keyword "lowy")
	 */
	private void keywordCelLY()
	{
		curCellLowY = TextUtils.atoi(keyWord);
	}

	/**
	 * get the high Y of the current cell (keyword "highy")
	 */
	private void keywordCelHY()
	{
		curCellHighY = TextUtils.atoi(keyWord);
	}

	/**
	 * get tool information for current cell (keyword "bits")
	 */
	private void keywordCelBit()
	{
		curCellUserbits = TextUtils.atoi(keyWord);
	}

	/**
	 * get tool information for current cell (keyword "userbits")
	 */
	private void keywordCelUsb()
	{
		curCellUserbits = TextUtils.atoi(keyWord);
	}

	/**
	 * get the external library file (keyword "externallibrary")
	 */
	private void keywordCelExt()
	{
		// get the path to the library file
		String withoutQuotes = keyWord;
		if (withoutQuotes.charAt(0) == '"')
		{
			withoutQuotes = withoutQuotes.substring(1);
			if (withoutQuotes.endsWith("\""))
				withoutQuotes = withoutQuotes.substring(0, withoutQuotes.length()-1);
		}

		// get the library associated with that name
		Library elib = readExternalLibraryFromFilename(withoutQuotes, OpenFile.Type.ELIB);

		// find the requested cell in the external library
		Cell cell = null;
		if (elib != null)
		{
			// find this cell in the external library
			cell = elib.findNodeProto(curCellName.toString());
			if (cell != null)
			{
				// cell found: make sure it is valid
				if (cell.getRevisionDate().compareTo(ELIBConstants.secondsToDate(curCellRevisionDate)) != 0)
				{
					System.out.println("Warning: cell " + cell.describe() + " in library " + elib.getName() +
						" has been modified since its use in library " + lib.getName());
				}
			}
		}

		// see if a cell was found
		if (cell != null)
		{
			// cell found in external library: remember the external reference
			curCell = cell;
			allCellsArray[curCellNumber] = cell;
			nodeProtoList[curCellNumber] = null;
		} else
		{
			// cell not found in external library: figure out the library name
			String elibName = null;
			if (elib != null) elibName = elib.getName(); else
			{
				File libFile = new File(withoutQuotes);
				elibName = libFile.getName();
				int lastDotPos = elibName.lastIndexOf('.');
				if (lastDotPos > 0) elibName = elibName.substring(0, lastDotPos);
			}

			// cell not found in library: issue warning
			System.out.println("Cannot find cell " + curCellName.toString() +
				" in library " + elibName + "...creating dummy version");

			// rename the cell
			//curCellName.setName(curCellName.getName() + "FROM" + elibName);
			if (curCellName.getVersion() != 0)
				curCellName = CellName.parseName(curCellName.getName() + "FROM" + elibName +
					";" + curCellName.getVersion() + "{" + curCellName.getView().getAbbreviation() + "}");
			else
				curCellName = CellName.parseName(curCellName.getName() + "FROM" + elibName +
					"{" + curCellName.getView().getAbbreviation() + "}");
			finishCellInitialization();

			// schedule the cell to have two nodes (cell center and big "X")
			LibraryFiles.NodeInstList nil = new LibraryFiles.NodeInstList();
			nodeInstList[curCellNumber] = nil;
			nil.theNode = new NodeInst[2];
			nil.protoType = new NodeProto[2];
			nil.name = new Name[2];
			nil.lowX = new int[2];
			nil.highX = new int[2];
			nil.lowY = new int[2];
			nil.highY = new int[2];
			nil.rotation = new short[2];
			nil.transpose = new int[2];

			// create a cell-center node
			nil.theNode[0] = NodeInst.lowLevelAllocate();
			nil.protoType[0] = Generic.tech.cellCenterNode;
			nil.name[0] = null;
			nil.lowX[0] = 0;
			nil.highX[0] = 0;
			nil.lowY[0] = 0;
			nil.highY[0] = 0;
			nil.rotation[0] = 0;
			nil.transpose[0] = 0;

			// create an artwork "Crossed box" to define the cell size
			nil.theNode[1] = NodeInst.lowLevelAllocate();
			nil.protoType[1] = Artwork.tech.crossedBoxNode;
			nil.name[1] = null;
			nil.lowX[1] = curCellLowX;
			nil.highX[1] = curCellHighX;
			nil.lowY[1] = curCellLowY;
			nil.highY[1] = curCellHighY;
			nil.rotation[1] = 0;
			nil.transpose[1] = 0;
		}
	}

	private void finishCellInitialization()
	{
		curCell.setTempInt(curCellGroup);
		curCell.lowLevelPopulate(curCellName.toString());
		curCell.lowLevelLink();
		curCell.lowLevelSetCreationDate(ELIBConstants.secondsToDate(curCellCreationDate));
		curCell.lowLevelSetRevisionDate(ELIBConstants.secondsToDate(curCellRevisionDate));
		curCell.lowLevelSetUserbits(curCellUserbits);
	}

	/**
	 * get the default technology for objects in this cell (keyword "technology")
	 */
	private void keywordTech()
	{
		Technology tech = findTechnologyName(keyWord);
		curCell.setTechnology(tech);
	}

	/**
	 * get the number of node instances in the current cell (keyword "nodes")
	 */
	private void keywordCelNoC()
	{
		// this keyword indicates that the cell is NOT external, so establish it now
		finishCellInitialization();

		// handle the NodeInst count in the cell
		int nodeInstCount = Integer.parseInt(keyWord);
		if (nodeInstCount == 0) return;
		LibraryFiles.NodeInstList nil = new LibraryFiles.NodeInstList();
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
	private void keywordCelArC()
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
	private void keywordCelPtC()
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
	private void keywordNewNo()
	{
		curNodeInstIndex = Integer.parseInt(keyWord);
		textLevel = INNODEINST;
		varPos = INVNODEINST;
	}

	/**
	 * get the type of the current nodeinst (keyword "type")
	 */
	private void keywordNodTyp()
	{
		NodeProto curNodeInstProto = null;
		int openSquare = keyWord.indexOf('[');
		if (openSquare >= 0)
		{
			curNodeInstProto = allCellsArray[TextUtils.atoi(keyWord, openSquare+1)];
		} else
		{
			curNodeInstProto = Cell.findNodeProto(keyWord);
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
	private void keywordNodLX()
	{
		nodeInstList[curCellNumber].lowX[curNodeInstIndex] = TextUtils.atoi(keyWord);
	}

	private void keywordNodHX()
	{
		nodeInstList[curCellNumber].highX[curNodeInstIndex] = TextUtils.atoi(keyWord);
	}

	private void keywordNodLY()
	{
		nodeInstList[curCellNumber].lowY[curNodeInstIndex] = TextUtils.atoi(keyWord);
	}

	private void keywordNodHY()
	{
		nodeInstList[curCellNumber].highY[curNodeInstIndex] = TextUtils.atoi(keyWord);
	}

	/**
	 * get the instance name of the current node instance (keyword "name")
	 */
	private void keywordNodNam()
	{
		nodeInstList[curCellNumber].theNode[curNodeInstIndex].setName(keyWord);
	}

	/**
	 * get the text descriptor of the current node instance (keyword "descript")
	 */
	private void keywordNodDes()
	{
		int td0 = TextUtils.atoi(keyWord);
		int td1 = 0;
		int slashPos = keyWord.indexOf('/');
		if (slashPos >= 0)
			td1 = TextUtils.atoi(keyWord.substring(slashPos+1));
		TextDescriptor td = new TextDescriptor(null, td0, td1, 0);
		Input.fixTextDescriptorFont(td);
		nodeInstList[curCellNumber].theNode[curNodeInstIndex].setProtoTextDescriptor(td);
	}

	/**
	 * get the rotation for the current nodeinst (keyword "rotation");
	 */
	private void keywordNodRot()
	{
		nodeInstList[curCellNumber].rotation[curNodeInstIndex] = (short)Integer.parseInt(keyWord);
	}

	/**
	 * get the transposition for the current nodeinst (keyword "transpose")
	 */
	private void keywordNodTra()
	{
		nodeInstList[curCellNumber].transpose[curNodeInstIndex] = Integer.parseInt(keyWord);
	}

	/**
	 * get the tool seen bits for the current nodeinst (keyword "aseen")
	 */
	private void keywordNodKse()
	{
		bitCount = 0;
	}

	/**
	 * get the port count for the current nodeinst (keyword "ports")
	 */
	private void keywordNodPoC() {}

	/**
	 * get tool information for current nodeinst (keyword "bits")
	 */
	private void keywordNodBit()
	{
		if (bitCount == 0) nodeInstList[curCellNumber].theNode[curNodeInstIndex].lowLevelSetUserbits(TextUtils.atoi(keyWord));
		bitCount++;
	}

	/**
	 * get tool information for current nodeinst (keyword "userbits")
	 */
	private void keywordNodUsb()
	{
		nodeInstList[curCellNumber].theNode[curNodeInstIndex].lowLevelSetUserbits(TextUtils.atoi(keyWord));
	}

	/**
	 * initialize for a new portinst on the current nodeinst (keyword "*port")
	 */
	private void keywordNewPor()
	{
		textLevel = INPOR;
	}

	// --------------------------------- ARC INSTANCE PARSING METHODS ---------------------------------

	/**
	 * initialize for a new arc instance (keyword "**arc")
	 */
	private void keywordNewAr()
	{
		curArcInstIndex = Integer.parseInt(keyWord);
		textLevel = INARCINST;
		varPos = INVARCINST;
	}

	/**
	 * get the type of the current arc instance (keyword "type")
	 */
	private void keywordArcTyp()
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
	private void keywordArcNam()
	{
		arcInstList[curCellNumber].arcList[curArcInstIndex].setName(keyWord);
	}

	/**
	 * get the width of the current arc instance (keyword "width")
	 */
	private void keywordArcWid()
	{
		arcInstList[curCellNumber].arcWidth[curArcInstIndex] = TextUtils.atoi(keyWord);
	}

	/**
	 * initialize for an end of the current arcinst (keyword "*end")
	 */
	private void keywordNewEnd()
	{
		curArcEnd = Integer.parseInt(keyWord);
		textLevel = INARCEND;
	}

	/**
	 * get the node at the current end of the current arcinst (keyword "node")
	 */
	private void keywordEndNod()
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
	private void keywordEndPt()
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
	private void keywordEndXP()
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

	private void keywordEndYP()
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
	private void keywordArcKse()
	{
		bitCount = 0;
	}

	/**
	 * get tool information for current arcinst (keyword "bits")
	 */
	private void keywordArcBit()
	{
		if (bitCount == 0) arcInstList[curCellNumber].arcList[curArcInstIndex].lowLevelSetUserbits(TextUtils.atoi(keyWord));
		bitCount++;
	}

	/**
	 * get tool information for current arcinst (keyword "userbits")
	 */
	private void keywordArcUsb()
	{
		arcInstList[curCellNumber].arcList[curArcInstIndex].lowLevelSetUserbits(TextUtils.atoi(keyWord));
	}

	// --------------------------------- PORT PROTOTYPE PARSING METHODS ---------------------------------

	/**
	 * initialize for a new port prototype (keyword "**porttype")
	 */
	private void keywordNewPt()
	{
		curExportIndex = Integer.parseInt(keyWord);
		textLevel = INPORTPROTO;
		varPos = INVPORTPROTO;
	}

	/**
	 * get the name for the current port prototype (keyword "name")
	 */
	private void keywordPtNam()
	{
		exportList[curCellNumber].exportName[curExportIndex] = keyWord;
	}

	/**
	 * get the text descriptor for the current port prototype (keyword "descript")
	 */
	private void keywordPtDes()
	{
		int td0 = TextUtils.atoi(keyWord);
		int td1 = 0;
		int slashPos = keyWord.indexOf('/');
		if (slashPos >= 0)
			td1 = TextUtils.atoi(keyWord, slashPos+1);
		TextDescriptor td = new TextDescriptor(null, td0, td1, 0);
		Input.fixTextDescriptorFont(td);
		exportList[curCellNumber].exportList[curExportIndex].setTextDescriptor(td);
	}

	/**
	 * get the sub-nodeinst for the current port prototype (keyword "subnode")
	 */
	private void keywordPtSNo()
	{
		int index = Integer.parseInt(keyWord);
		exportList[curCellNumber].exportSubNode[curExportIndex] = nodeInstList[curCellNumber].theNode[index];
	}

	/**
	 * get the sub-portproto for the current port prototype (keyword "subport")
	 */
	private void keywordPtSPt()
	{
		exportList[curCellNumber].exportSubPort[curExportIndex] = keyWord;
	}

	/**
	 * get the tool seen for the current port prototype (keyword "aseen")
	 */
	private void keywordPtKse()
	{
		bitCount = 0;
	}

	/**
	 * get the tool data for the current port prototype (keyword "bits")
	 */
	private void keywordPtBit()
	{
		if (bitCount == 0) exportList[curCellNumber].exportList[curExportIndex].lowLevelSetUserbits(TextUtils.atoi(keyWord));
		bitCount++;
	}

	/**
	 * get the tool data for the current port prototype (keyword "userbits")
	 */
	private void keywordPtUsb()
	{
		exportList[curCellNumber].exportList[curExportIndex].lowLevelSetUserbits(TextUtils.atoi(keyWord));
	}

	// --------------------------------- VARIABLE PARSING METHODS ---------------------------------

	/**
	 * get variables on current object (keyword "variables")
	 */
	private void keywordGetVar()
		throws IOException
	{
		ElectricObject naddr = null;
		Object obj = null;
		switch (varPos)
		{
			case INVTOOL:				// keyword applies to tools
				obj = curTool;
				break;
			case INVTECHNOLOGY:			// keyword applies to technologies
				obj = curTech;
				break;
			case INVLIBRARY:			// keyword applies to library
				naddr = lib;
				break;
			case INVNODEPROTO:			// keyword applies to nodeproto
				naddr = curCell;
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
			int openSquarePos = keyWord.lastIndexOf('['); // lastIndex, because LE variables may contain '['
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
			TextDescriptor td = new TextDescriptor(null, td0, td1, 0);

			// get value
			if (getKeyword())
			{
				System.out.println("EOF too soon");
				return;
			}
			Object value = null;
			if ((type&ELIBConstants.VISARRAY) == 0)
			{
				value = variableDecode(keyWord, type);
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
					if ((type&ELIBConstants.VTYPE) == ELIBConstants.VSTRING)
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
					al.add(variableDecode(entry, type));
					if (keyWord.charAt(pos) == ']') break;
					if (keyWord.charAt(pos) != ',')
					{
						System.out.println("Error on line "+lineReader.getLineNumber()+": missing comma between array entries: " + keyWord);
						return;
					}
					pos++;
				}
				int arrayLen = al.size();
				switch (type&ELIBConstants.VTYPE)
				{
					case ELIBConstants.VADDRESS:
					case ELIBConstants.VINTEGER:    value = new Integer[arrayLen];     break;
					case ELIBConstants.VFRACT:      
					case ELIBConstants.VFLOAT:      value = new Float[arrayLen];       break;
					case ELIBConstants.VDOUBLE:     value = new Double[arrayLen];      break;
					case ELIBConstants.VSHORT:      value = new Short[arrayLen];       break;
					case ELIBConstants.VBOOLEAN:
					case ELIBConstants.VCHAR:       value = new Byte[arrayLen];        break;
					case ELIBConstants.VSTRING:     value = new String[arrayLen];      break;
					case ELIBConstants.VNODEINST:   value = new NodeInst[arrayLen];    break;
					case ELIBConstants.VNODEPROTO:  value = new NodeProto[arrayLen];   break;
					case ELIBConstants.VARCPROTO:   value = new ArcProto[arrayLen];    break;
					case ELIBConstants.VPORTPROTO:  value = new PortProto[arrayLen];   break;
					case ELIBConstants.VARCINST:    value = new ArcInst[arrayLen];     break;
					case ELIBConstants.VTECHNOLOGY: value = new Technology[arrayLen];  break;
					case ELIBConstants.VLIBRARY:    value = new Library[arrayLen];     break;
					case ELIBConstants.VTOOL:       value = new Tool[arrayLen];        break;
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
				var.setTextDescriptor(td);
				var.lowLevelSetFlags(type);

				// handle updating of technology caches
//				if (naddr instanceof Technology)
//					changedtechnologyvariable(key);
			} else if (obj != null && topLevelLibrary) {
				if (value instanceof Integer ||
					value instanceof Double ||
					value instanceof Float ||
					value instanceof String)
				{
					// change "meaning option"
					Pref.Meaning meaning = Pref.getMeaningVariable(obj, varName);
					if (meaning != null)
						Pref.changedMeaningVariable(meaning, value);
					else if (obj instanceof Technology)
						((Technology)obj).convertOldVariable(varName, value);
				}
			}
		}
		if (varPos == INVLIBRARY)
		{
			// cache the font associations
			Input.getFontAssociationVariable(lib);
		}
		Input.fixVariableFont(naddr);
	}

	private Object variableDecode(String name, int type)
	{
		int thistype = type;
		if ((thistype&(ELIBConstants.VCODE1|ELIBConstants.VCODE2)) != 0) thistype = ELIBConstants.VSTRING;

		switch (thistype&ELIBConstants.VTYPE)
		{
			case ELIBConstants.VINTEGER:
			case ELIBConstants.VSHORT:
			case ELIBConstants.VBOOLEAN:
			case ELIBConstants.VADDRESS:
				return new Integer(TextUtils.atoi(name));
			case ELIBConstants.VFRACT:
				return new Float(TextUtils.atoi(name) / 120.0f);
			case ELIBConstants.VCHAR:
				return new Character(name.charAt(0));
			case ELIBConstants.VSTRING:
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
			case ELIBConstants.VFLOAT:
				return new Float(Float.parseFloat(name));
			case ELIBConstants.VDOUBLE:
				return new Double(Double.parseDouble(name));
			case ELIBConstants.VNODEINST:
				int niIndex = TextUtils.atoi(name);
				NodeInst ni = nodeInstList[curCellNumber].theNode[niIndex];
				return ni;
			case ELIBConstants.VNODEPROTO:
				int colonPos = name.indexOf(':');
				if (colonPos < 0)
				{
					// just an integer specification
					int cindex = Integer.parseInt(name);
					return allCellsArray[cindex];
				} else
				{
					// parse primitive nodeproto name
					NodeProto np = Cell.findNodeProto(name);
					if (np == null)
					{
						System.out.println("Error on line "+lineReader.getLineNumber()+": cannot find node " + name);
						return null;
					}
					return np;
				}
			case ELIBConstants.VPORTPROTO:
				int ppIndex = TextUtils.atoi(name);
				PortProto pp = exportList[curCellNumber].exportList[ppIndex];
				return pp;
			case ELIBConstants.VARCINST:
				int aiIndex = TextUtils.atoi(name);
				ArcInst ai = arcInstList[curCellNumber].arcList[aiIndex];
				return ai;
			case ELIBConstants.VARCPROTO:
				return ArcProto.findArcProto(name);
			case ELIBConstants.VTECHNOLOGY:
				return Technology.findTechnology(name);
			case ELIBConstants.VTOOL:
				return Tool.findTool(name);
		}
		return null;
	}
}
