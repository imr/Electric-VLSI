/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ELIB.java
 * Input/output tool: ELIB Library input
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

import com.sun.electric.database.geometry.*;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.ELIBConstants;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.ncc.basic.TransitiveRelation;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;


/**
 * This class reads files in binary (.elib) format.
 */
public class ELIB extends LibraryFiles
{
	// ------------------------- private data ----------------------------
	/** the magic number in the library file */								private int magic;

	// characteristics of the file
	/** true if bytes are swapped on disk */								private boolean bytesSwapped;
	/** the size of a "big" integer on disk (4 or more bytes) */			private int sizeOfBig;
	/** the size of a "small" integer on disk (2 or more bytes) */			private int sizeOfSmall;
	/** the size of a character on disk (1 or 2 bytes) */					private int sizeOfChar;

	// statistics about the file
	/** the number of integers on disk that got clipped during input */		private int clippedIntegers;

	// the tool information
	/** the number of tools in the file */									private int toolCount;
	/** the number of tools in older files */								private int toolBCount;
	/** list of all tools in the library */									private Tool [] toolList;
	/** list of all tool-related errors in the library */					private String [] toolError;
	/** true if tools are out of order in the library */					private boolean toolBitsMessed;

	// the technology information
	/** the number of technologies in the file */							private int techCount;
	/** list of all Technologies in the library */							private Technology [] techList;
	/** list of all technology-related errors in the library */				private String [] techError;
	/** scale factors for each technology in the library */					private double [] techScale;
	/** the number of ArcProtos in the file */								private int arcProtoCount;
	/** list of all ArcProtos in the library */								private PrimitiveArc [] arcProtoList;
	/** list of all ArcProto-related errors in the library */				private String [] arcProtoError;
	/** the number of primitive NodeProtos in the file */					private int primNodeProtoCount;
	/** list of all Primitive NodeProtos in the library */					private PrimitiveNode [] primNodeProtoList;
	/** list of the primitive-NodeProto-related errors in the library */	private boolean [] primNodeProtoError;
	/** list of the original primitive NodeProtos in the library */			private String [] primNodeProtoOrig;
	/** list of all NodeProto technologies in the library */				private int [] primNodeProtoTech;
	/** the number of primitive PortProtos in the file */					private int primPortProtoCount;
	/** list of all Primitive PortProtos in the library */					private PrimitivePort [] primPortProtoList;
	/** list of all Primitive-PortProto-related errors in the library */	private String [] primPortProtoError;
	/** the most popular layout technology */								private Technology layoutTech;

	// the cell information
	/** the number of Cells in the file */									private int cellCount;
	/** the index of the current Cell */									private int curCell;
	/** list of all former cells in the library */							private FakeCell [] fakeCellList;
	/** list of number of NodeInsts in each Cell of the library */			private int [] nodeCounts;
	/** index of first NodeInst in each cell of the library */				private int [] firstNodeIndex;
	/** list of number of ArcInsts in each Cell of the library */			private int [] arcCounts;
	/** index of first ArcInst in each cell of the library */				private int [] firstArcIndex;
	/** list of all Exports in each Cell of the library */					private int [] portCounts;
	/** index of first Export in each cell of the library */				private int [] firstPortIndex;
	/** X center each cell of the library */								private int [] cellXOff;
	/** Y center each cell of the library */								private int [] cellYOff;
	/** true if this x-lib cell ref is satisfied */							private boolean [] xLibRefSatisfied;
	/** mapping view indices to views */									private HashMap viewMapping;

	// the NodeInsts in the library
	/** the number of NodeInsts in the library */							private int nodeCount;
	/** All data for NodeInsts in each Cell. */								private LibraryFiles.NodeInstList nodeInstList;

	// the ArcInsts in the library
	/** the number of ArcInsts in the library */							private int arcCount;
	/** list of all ArcInsts in the library */								private ArcInst [] arcList;
	/** list of the prototype of the ArcInsts in the library */				private ArcProto [] arcTypeList;
	/** list of the Names of the ArcInsts in the library */					private Name [] arcNameList;
	/** list of the width of the ArcInsts in the library */					private int [] arcWidthList;
	/** list of the head X of the ArcInsts in the library */				private int [] arcHeadXPosList;
	/** list of the head Y of the ArcInsts in the library */				private int [] arcHeadYPosList;
	/** list of the head node of the ArcInsts in the library */				private int [] arcHeadNodeList;
	/** list of the head port of the ArcInsts in the library */				private Object [] arcHeadPortList;
	/** list of the tail X of the ArcInsts in the library */				private int [] arcTailXPosList;
	/** list of the tail Y of the ArcInsts in the library */				private int [] arcTailYPosList;
	/** list of the tail node of the ArcInsts in the library */				private int [] arcTailNodeList;
	/** list of the tail port of the ArcInsts in the library */				private Object [] arcTailPortList;
	/** list of the user flags on the ArcInsts in the library */			private int [] arcUserBits;

	// the Exports in the library
	/** the number of Exports in the library */								private int portProtoCount;
	/** counter for Exports in the library */								private int portProtoIndex;
	/** list of all Exports in the library */								private Object [] portProtoList;
	/** list of NodeInsts that are origins of Exports in the library */		private int [] portProtoSubNodeList;
	/** list of PortProtos that are origins of Exports in the library */	private Object [] portProtoSubPortList;
	/** list of Export names in the library */								private String [] portProtoNameList;
	/** list of Export userbits in the library */							private int [] portProtoUserbits;

	// the geometric information (only used for old format files)
	/** the number of Geometrics in the file */								private int geomCount;
	/** list of all Geometric types in the library */						private boolean [] geomType;
	/** list of all Geometric up-pointers in the library */					private int [] geomMoreUp;

	// the variable information
	/** number of variable names in the library */							private int nameCount;
    /** variable Keys possibly in the library */                            private VarKeys varKeys;
	/** true to convert all text descriptor values */						private boolean convertTextDescriptors;
	/** true to require text descriptor values */							private boolean alwaysTextDescriptors;

    /** Holder for all variable keys read in. Keys are only created if used */
    private static class VarKeys {
        private String [] keys;
        private VarKeys(int i) {
            keys = new String[i];
        }
        /** Add a Variable Key */
        protected void addKey(int i, String name) {
            keys[i] = name;
        }
        /** Get a Variable Key */
        protected Variable.Key getKey(int i) {
            String name = keys[i];
            return ElectricObject.newKey(name);
        }
    }

	ELIB() {}

	// ----------------------- public methods -------------------------------

	/**
	 * Method to read a Library in binary (.elib) format.
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
		// initialize
		clippedIntegers = 0;
		byteCount = 0;
		layoutTech = null;

		// read the magic number and determine whether bytes are swapped
		if (readHeader())
		{
			System.out.println("Error reading header");
			return true;
		}

		// get count of objects in the file
		toolCount = readBigInteger();
		techCount = readBigInteger();
		primNodeProtoCount = readBigInteger();
		primPortProtoCount = readBigInteger();
		arcProtoCount = readBigInteger();
		nodeProtoCount = readBigInteger();
		nodeCount = readBigInteger();
		portProtoCount = readBigInteger();
		arcCount = readBigInteger();
		geomCount = readBigInteger();
		if (magic <= ELIBConstants.MAGIC9 && magic >= ELIBConstants.MAGIC11)
		{
			// versions 9 through 11 stored a "cell count"
			cellCount = readBigInteger();
		} else
		{
			cellCount = nodeProtoCount;
		}
		curCell = readBigInteger();

		// get the Electric version (version 8 and later)
		String versionString;
		if (magic <= ELIBConstants.MAGIC8) versionString = readString(); else
			versionString = "3.35";
		version = Version.parseVersion(versionString);
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
// 		}

		// for versions before 6.04c, convert text descriptor values
		convertTextDescriptors = version.compareTo(Version.parseVersion("6.04c")) < 0;
// 		convertTextDescriptors = false;
// 		if (emajor < 6 ||
// 			(emajor == 6 && eminor < 4) ||
// 			(emajor == 6 && eminor == 4 && edetail < 3))
// 		{
// 			convertTextDescriptors = true;
// 		}

		// for versions 6.05x and later, always have text descriptor values
		alwaysTextDescriptors = version.compareTo(Version.parseVersion("6.05x")) >= 0;
// 		alwaysTextDescriptors = true;
// 		if (emajor < 6 ||
// 			(emajor == 6 && eminor < 5) ||
// 			(emajor == 6 && eminor == 5 && edetail < 24))
// 		{
// 			alwaysTextDescriptors = false;
// 		}

		// for Electric version 4 or earlier, scale lambda by 20
		scaleLambdaBy20 = version.compareTo(Version.parseVersion("5")) < 0;
// 		if (emajor <= 4) lambda *= 20;

		// mirror bits
		rotationMirrorBits = version.compareTo(Version.parseVersion("7.01")) >= 0;
//		if (emajor > 7 || (emajor == 7 && eminor >= 1))

		// get the newly created views (version 9 and later)
		viewMapping = new HashMap();
		viewMapping.put(new Integer(-1), View.UNKNOWN);
		viewMapping.put(new Integer(-2), View.LAYOUT);
		viewMapping.put(new Integer(-3), View.SCHEMATIC);
		viewMapping.put(new Integer(-4), View.ICON);
		viewMapping.put(new Integer(-5), View.DOCWAVE);
		viewMapping.put(new Integer(-6), View.LAYOUTSKEL);
		viewMapping.put(new Integer(-7), View.VHDL);
		viewMapping.put(new Integer(-8), View.NETLIST);
		viewMapping.put(new Integer(-9), View.DOC);
		viewMapping.put(new Integer(-10), View.NETLISTNETLISP);
		viewMapping.put(new Integer(-11), View.NETLISTALS);
		viewMapping.put(new Integer(-12), View.NETLISTQUISC);
		viewMapping.put(new Integer(-13), View.NETLISTRSIM);
		viewMapping.put(new Integer(-14), View.NETLISTSILOS);
		viewMapping.put(new Integer(-15), View.VERILOG);
		viewMapping.put(new Integer(-16), View.LAYOUTCOMP);
		if (magic <= ELIBConstants.MAGIC9)
		{
			int numExtraViews = readBigInteger();
			for(int i=0; i<numExtraViews; i++)
			{
				String viewName = readString();
				String viewShortName = readString();
				View view = View.findView(viewName);
				if (view == null)
				{
					// special conversion from old view names
					view = findOldViewName(viewName);
					if (view == null)
					{
						view = View.newInstance(viewName, viewShortName);
						if (view == null) return true;
					}
				}
				viewMapping.put(new Integer(i+1), view);
			}
		}

		// get the number of toolbits to ignore
		if (magic <= ELIBConstants.MAGIC3 && magic >= ELIBConstants.MAGIC6)
		{
			// versions 3, 4, 5, and 6 find this in the file
			toolBCount = readBigInteger();
		} else
		{
			// versions 1 and 2 compute this (versions 7 and later ignore it)
			toolBCount = toolCount;
		}

		// erase the current database
		lib.erase();

		// allocate pointers for the Technologies
		techList = new Technology[techCount];
		techError = new String[techCount];
		techScale = new double[Technology.getNumTechnologies()];
		arcProtoList = new PrimitiveArc[arcProtoCount];
		arcProtoError = new String[arcProtoCount];
		primNodeProtoList = new PrimitiveNode[primNodeProtoCount];
		primNodeProtoError = new boolean[primNodeProtoCount];
		primNodeProtoOrig = new String[primNodeProtoCount];
		primNodeProtoTech = new int[primNodeProtoCount];
		primPortProtoList = new PrimitivePort[primPortProtoCount];
		primPortProtoError = new String[primPortProtoCount];

		// allocate pointers for the Tools
		toolList = new Tool[toolCount];
		toolError = new String[toolCount];

		// allocate pointers for the Cells
		nodeProtoList = new Cell[nodeProtoCount];
		nodeCounts = new int[nodeProtoCount];
		firstNodeIndex = new int[nodeProtoCount+1];
		arcCounts = new int[nodeProtoCount];
		firstArcIndex = new int[nodeProtoCount+1];
		portCounts = new int[nodeProtoCount];
		firstPortIndex = new int[nodeProtoCount];
		cellLambda = new double[nodeProtoCount];
		cellXOff = new int[nodeProtoCount];
		cellYOff = new int[nodeProtoCount];
		xLibRefSatisfied = new boolean[nodeProtoCount];

		// allocate pointers for the NodeInsts
		nodeInstList = new LibraryFiles.NodeInstList();
		nodeInstList.theNode = new NodeInst[nodeCount];
		nodeInstList.protoType = new NodeProto[nodeCount];
		nodeInstList.name = new Name[nodeCount];
		nodeInstList.lowX = new int[nodeCount];
		nodeInstList.highX = new int[nodeCount];
		nodeInstList.lowY = new int[nodeCount];
		nodeInstList.highY = new int[nodeCount];
		nodeInstList.anchorX = new int[nodeCount];
		nodeInstList.anchorY = new int[nodeCount];
		nodeInstList.rotation = new short[nodeCount];
		nodeInstList.transpose = new int[nodeCount];
		nodeInstList.userBits = new int[nodeCount];

		// allocate pointers for the ArcInsts
		arcList = new ArcInst[arcCount];
		arcTypeList = new ArcProto[arcCount];
		arcNameList = new Name[arcCount];
		arcWidthList = new int[arcCount];
		arcHeadXPosList = new int[arcCount];
		arcHeadYPosList = new int[arcCount];
		arcHeadNodeList = new int[arcCount];
		arcHeadPortList = new Object[arcCount];
		arcTailXPosList = new int[arcCount];
		arcTailYPosList = new int[arcCount];
		arcTailNodeList = new int[arcCount];
		arcTailPortList = new Object[arcCount];
		arcUserBits = new int[arcCount];
		for(int i = 0; i < arcCount; i++)
		{
			arcHeadNodeList[i] = -1;
			arcHeadPortList[i] = null;
			arcTailNodeList[i] = -1;
			arcTailPortList[i] = null;
			arcNameList[i] = null;
			arcUserBits[i] = 0;
		}

		// allocate pointers for the Exports
		portProtoList = new Object[portProtoCount];
		portProtoSubNodeList = new int[portProtoCount];
		portProtoSubPortList = new Object[portProtoCount];
		portProtoNameList = new String[portProtoCount];
		portProtoUserbits = new int[portProtoCount];

		// versions 9 to 11 allocate fake-cell pointers
		if (magic <= ELIBConstants.MAGIC9 && magic >= ELIBConstants.MAGIC11)
		{
			fakeCellList = new FakeCell[cellCount];
			for(int i=0; i<cellCount; i++)
				fakeCellList[i] = new FakeCell();
		}

		// versions 4 and earlier allocate geometric pointers
		if (magic > ELIBConstants.MAGIC5)
		{
			geomType = new boolean [geomCount];
			geomMoreUp = new int [geomCount];
		}

		// get number of arcinsts and nodeinsts in each cell
		if (magic != ELIBConstants.MAGIC1)
		{
			// versions 2 and later find this in the file
			int nodeInstPos = 0, arcInstPos = 0, portProtoPos = 0;
			for(int i=0; i<nodeProtoCount; i++)
			{
				arcCounts[i] = readBigInteger();
				nodeCounts[i] = readBigInteger();
				portCounts[i] = readBigInteger();

				// the arc and node counts are negative for external cell references
				if (arcCounts[i] > 0 || nodeCounts[i] > 0)
				{
					arcInstPos += arcCounts[i];
					nodeInstPos += nodeCounts[i];
				}
				portProtoPos += portCounts[i];
			}

			// verify that the number of node instances is equal to the total in the file
			if (nodeInstPos != nodeCount)
			{
				System.out.println("Error: cells have " + nodeInstPos + " nodes but library has " + nodeCount);
				return true;
			}
			if (arcInstPos != arcCount)
			{
				System.out.println("Error: cells have " + arcInstPos + " arcs but library has " + arcCount);
				return true;
			}
			if (portProtoPos != portProtoCount)
			{
				System.out.println("Error: cells have " + portProtoPos + " ports but library has " + portProtoCount);
				return true;
			}
		} else
		{
			// version 1 computes this information
			arcCounts[0] = arcCount;
			nodeCounts[0] = nodeCount;
			portCounts[0] = portProtoCount;
			for(int i=1; i<nodeProtoCount; i++)
				arcCounts[i] = nodeCounts[i] = portCounts[i] = 0;
		}

		// allocate all cells in the library
		for(int i=0; i<nodeProtoCount; i++)
		{
			if (arcCounts[i] < 0 && nodeCounts[i] < 0)
			{
				// this cell is from an external library
				nodeProtoList[i] = null;
				xLibRefSatisfied[i] = false;
			} else
			{
				nodeProtoList[i] = Cell.lowLevelAllocate(lib);
				if (nodeProtoList[i] == null) return true;
				xLibRefSatisfied[i] = true;
			}
		}

		// allocate the nodes, arcs, and exports in each cell
		int nodeinstpos = 0, arcinstpos = 0, portprotopos = 0;
		for(int cellIndex=0; cellIndex<nodeProtoCount; cellIndex++)
		{
			Cell cell = nodeProtoList[cellIndex];
			if (cell == null)
			{
				// for external references, clear the port proto list
				for(int i=0; i<portCounts[cellIndex]; i++)
					portProtoList[portprotopos+i] = null;
				portprotopos += portCounts[cellIndex];
				continue;
			}

			// allocate node instances in this cell
			for(int i=0; i<nodeCounts[cellIndex]; i++)
			{
				int thisone = i + nodeinstpos;
				nodeInstList.theNode[thisone] = NodeInst.lowLevelAllocate();
				if (nodeInstList.theNode[thisone] == null) return true;
			}
			nodeinstpos += nodeCounts[cellIndex];

			// allocate port prototypes in this cell
			for(int i=0; i<portCounts[cellIndex]; i++)
			{
				int thisone = i + portprotopos;
				portProtoList[thisone] = Export.lowLevelAllocate();
				if (portProtoList[thisone] == null) return true;
			}
			portprotopos += portCounts[cellIndex];

			// allocate arc instances in this cell
			for(int i=0; i<arcCounts[cellIndex]; i++)
			{
				int thisone = i + arcinstpos;
				arcList[thisone] = ArcInst.lowLevelAllocate();
				if (arcList[thisone] == null) return true;
			}
			arcinstpos += arcCounts[cellIndex];
		}

		// setup pointers for technologies and primitives
		primNodeProtoCount = 0;
		primPortProtoCount = 0;
		arcProtoCount = 0;
		for(int techIndex=0; techIndex<techCount; techIndex++)
		{
			// get the technology
			String name = readString();
			Technology tech = findTechnologyName(name);
			boolean imosconv = false;
			if (name.equals("imos"))
			{
				tech = MoCMOS.tech;
				imosconv = true;
			}
			if (tech == null)
			{
				// cannot figure it out: just pick the first technology
				Iterator it = Technology.getTechnologies();
				tech = (Technology) it.next();
				techError[techIndex] = name;
			} else techError[techIndex] = null;
			techList[techIndex] = tech;

			// get the number of primitive node prototypes
			int numPrimNodes = readBigInteger();
			for(int j=0; j<numPrimNodes; j++)
			{
				primNodeProtoOrig[primNodeProtoCount] = null;
				primNodeProtoError[primNodeProtoCount] = false;
				name = readString();
				if (imosconv) name = name.substring(6);
				PrimitiveNode pnp = tech.findNodeProto(name);
				if (pnp == null)
				{
					// automatic conversion of "Active-Node" in to "P-Active-Node" (MOSIS CMOS)
					if (name.equals("Active-Node"))
						pnp = tech.findNodeProto("P-Active-Node");
				}
				if (pnp == null)
				{
					boolean advise = true;

					// look for substring name match at start of name
					for(Iterator it = tech.getNodes(); it.hasNext();)
					{
						PrimitiveNode opnp = (PrimitiveNode) it.next();
						String primName = opnp.getName();
						if (primName.startsWith(name) || name.startsWith(primName))
						{
							pnp = opnp;
							break;
						}
					}

					// look for substring match at end of name
					if (pnp == null)
					{
						for(Iterator it = tech.getNodes(); it.hasNext();)
						{
							PrimitiveNode opnp = (PrimitiveNode) it.next();
							String primName = opnp.getName();
							if (primName.endsWith(name) || name.endsWith(primName))
							{
								pnp = opnp;
								break;
							}
						}
					}

					// special cases: convert special primitives that are known to the technologies
					if (pnp == null)
					{
						pnp = tech.convertOldNodeName(name);
						if (pnp != null) advise = false;
					}

					// give up and use first primitive in this technology
					if (pnp == null)
					{
						Iterator it = tech.getNodes();
						pnp = (PrimitiveNode) it.next();
					}

					// construct the error message
					if (advise)
					{
						String errorMessage;
						if (techError[techIndex] != null) errorMessage = techError[techIndex]; else
							errorMessage = tech.getTechName();
						errorMessage += ":" + name;
						primNodeProtoOrig[primNodeProtoCount] = errorMessage;
						primNodeProtoError[primNodeProtoCount] = true;
					}
				}
				primNodeProtoTech[primNodeProtoCount] = techIndex;
				primNodeProtoList[primNodeProtoCount] = pnp;

				// get the number of primitive port prototypes
				int numPrimPorts = readBigInteger();
				for(int i=0; i<numPrimPorts; i++)
				{
					primPortProtoError[primPortProtoCount] = null;
					name = readString();
					PrimitivePort pp = (PrimitivePort)pnp.findPortProto(name);

					// convert special port names
					if (pp == null)
						pp = tech.convertOldPortName(name, pnp);

					if (pp == null)
					{
						Iterator it = pnp.getPorts();
						if (it.hasNext())
						{
							pp = (PrimitivePort)it.next();
							if (!primNodeProtoError[primNodeProtoCount])
							{
								String errorMessage = name + " on ";
								if (primNodeProtoOrig[primNodeProtoCount] != null)
									errorMessage += primNodeProtoOrig[primNodeProtoCount]; else
								{
									if (techError[techIndex] != null)
										errorMessage += techError[techIndex]; else
											errorMessage += tech.getTechName();
									errorMessage += ":" + pnp.getName();
								}
								primPortProtoError[primPortProtoCount] = errorMessage;
							}
						}
					}
					primPortProtoList[primPortProtoCount++] = pp;
				}
				primNodeProtoCount++;
			}

			// get the number of arc prototypes
			int numArcProtos = readBigInteger();
			for(int j=0; j<numArcProtos; j++)
			{
				arcProtoError[arcProtoCount] = null;
				name = readString();
				if (imosconv) name = name.substring(6);
				PrimitiveArc ap = tech.findArcProto(name);
				if (ap == null)
				{
					ap = tech.convertOldArcName(name);
				}
				if (ap == null)
				{
					Iterator it = tech.getArcs();
					ap = (PrimitiveArc) it.next();
					String errorMessage;
					if (techError[techIndex] != null)
						errorMessage = techError[techIndex]; else
							errorMessage = tech.getTechName();
					errorMessage += ":" + name;
					arcProtoError[arcProtoCount] = errorMessage;
				}
				arcProtoList[arcProtoCount++] = ap;
			}
		}

		// setup pointers for tools
		toolBitsMessed = false;
		for(int i=0; i<toolCount; i++)
		{
			String name = readString();
			toolError[i] = null;
			Tool t = Tool.findTool(name);
			int toolIndex = -1;
			if (t == null)
			{
				toolError[i] = name;
			} else
			{
				toolIndex = t.getIndex();
			}
			if (i != toolIndex) toolBitsMessed = true;
			toolList[i] = t;
		}
		if (magic <= ELIBConstants.MAGIC3 && magic >= ELIBConstants.MAGIC6)
		{
			// versions 3, 4, 5, and 6 must ignore toolbits associations
			for(int i=0; i<toolBCount; i++) readString();
		}

		// get the library userbits
		int userBits = 0;
		if (magic <= ELIBConstants.MAGIC7)
		{
			// version 7 and later simply read the relevant data
			userBits = readBigInteger();
		} else
		{
			// version 6 and earlier must sift through the information
			if (toolBCount >= 1) userBits = readBigInteger();
			for(int i=1; i<toolBCount; i++) readBigInteger();
		}
		lib.lowLevelSetUserBits(userBits);
		lib.clearChangedMajor();
		lib.clearChangedMinor();
		lib.setFromDisk();
		lib.setVersion(version);

		// get the lambda values in the library
		for(int i=0; i<techCount; i++)
		{
			int lambda = readBigInteger();
			if (techError[i] != null) continue;
			Technology tech = techList[i];

			// for Electric version 4 or earlier, scale lambda by 20
			if (scaleLambdaBy20) lambda *= 20;
// 			if (emajor <= 4) lambda *= 20;

			int index = tech.getIndex();
			techScale[index] = lambda;
			if (topLevelLibrary)
			{
				String varName = tech.getScaleVariableName();
				Pref.Meaning meaning = Pref.getMeaningVariable(tech, varName);
				if (meaning != null)
				{
					Pref.changedMeaningVariable(meaning, new Double(lambda/2));
// 					Variable var = tech.newVar(varName, new Double(lambda/2));
// 					Pref.changedMeaningVariable(meaning);
				}
			}
		}
		for (Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			if (techScale[tech.getIndex()] > 0.0) continue;
			techScale[tech.getIndex()] = tech.getScale();
		}

		// read the global namespace
		if (readNameSpace())
		{
			System.out.println("Error reading namespace");
			return true;
		}

		// read the library variables
		if (readVariables(lib, -1) < 0) return true;

		// grab font associations from the library
		Input.getFontAssociationVariable(lib);
		Input.fixVariableFont(lib);

		// read the tool variables
		for(int i=0; i<toolCount; i++)
		{
			Tool tool = toolList[i];
			if (tool == null) ignoreVariables(); else
			{
				if (readMeaningPrefs(tool) < 0) return true;
// 				if (readVariables(tool, -1) < 0) return true;
// 				Input.fixVariableFont(tool);
			}
		}

		// read the technology variables
		for(int i=0; i<techCount; i++)
		{
			Technology tech = techList[i];
			if (tech == null) ignoreVariables(); else
			{
				int j = readMeaningPrefs(tech);
// 				int j = readVariables(tech, -1);
				if (j < 0) return true;
				if (j > 0) getTechList(i);
// 				Input.fixVariableFont(tech);
			}
		}

		// read the arcproto variables
		for(int i=0; i<arcProtoCount; i++)
		{
			ignoreVariables();
// 			PrimitiveArc ap = arcProtoList[i];
// 			int j = readVariables(ap, -1);
// 			if (j < 0) return true;
// 			if (j > 0) getArcProtoList(i);
// 			Input.fixVariableFont(ap);
		}

		// read the primitive nodeproto variables
		for(int i=0; i<primNodeProtoCount; i++)
		{
			ignoreVariables();
// 			PrimitiveNode np = primNodeProtoList[i];
// 			int j = readVariables(np, -1);
// 			if (j < 0) return true;
// 			if (j > 0) getPrimNodeProtoList(i);
// 			Input.fixVariableFont(np);
		}

		// read the primitive portproto variables
		for(int i=0; i<primPortProtoCount; i++)
		{
			ignoreVariables();
// 			PortProto pp = primPortProtoList[i];
// 			int j = readVariables(pp, -1);
// 			if (j < 0) return true;
// 			if (j > 0) getPrimPortProtoList(i);
		}

		// read the view variables (version 9 and later)
		if (magic <= ELIBConstants.MAGIC9)
		{
			int count = readBigInteger();
			for(int i=0; i<count; i++)
			{
				int j = readBigInteger();
				View v = getView(j);
				if (v != null)
				{
					ignoreVariables();
// 					if (readVariables(v, -1) < 0) return true;
// 					Input.fixVariableFont(v);
				} else
				{
					System.out.println("View index " + j + " not found");
					ignoreVariables();
				}
			}
		}

		// setup fake cell structures (version 9 to 11)
		if (magic <= ELIBConstants.MAGIC9 && magic >= ELIBConstants.MAGIC11)
		{
			for(int i=0; i<cellCount; i++)
			{
				String thecellname = readString();
				ignoreVariables();

				fakeCellList[i].cellName = convertCellName(thecellname);
			}
		}

		// read the cells
		portProtoIndex = 0;
		HashMap nextInCellGroup = new HashMap();
		for(int i=0; i<nodeProtoCount; i++)
		{
			Cell cell = nodeProtoList[i];
			if (cell == null) continue;
			if (readNodeProto(cell, i, nextInCellGroup))
			{
				System.out.println("Error reading cell");
				return true;
			}
		}

		// collect the cells by common protoName and by "nextInCellGroup" relation
		TransitiveRelation transitive = new TransitiveRelation();
		HashMap/*<String,String>*/ protoNames = new HashMap();
		for(int cellIndex=0; cellIndex<nodeProtoCount; cellIndex++)
		{
			Cell cell = nodeProtoList[cellIndex];
			if (cell == null || cell.getLibrary() != lib) continue;
			String protoName = (String)protoNames.get(cell.getName());
			if (protoName == null)
			{
				protoName = cell.getName();
				protoNames.put(protoName, protoName);
			}
			transitive.theseAreRelated(cell, protoName);
			Cell otherCell = (Cell)nextInCellGroup.get(cell);
			if (otherCell != null && cell.getLibrary() == lib)
				transitive.theseAreRelated(cell, otherCell);
		}
		// create the cell groups
		HashMap/*<Cell,CellGroup>*/ cellGroups = new HashMap();
		for (Iterator git = transitive.getSetsOfRelatives(); git.hasNext();)
		{
			Set group = (Set)git.next();
			Cell.CellGroup cg = new Cell.CellGroup();
			for (Iterator it = group.iterator(); it.hasNext();)
			{
				Object o = it.next();
				if (o instanceof Cell)
					cellGroups.put(o, cg);
			}
		}
		// put cells into the cell groups
		for(int cellIndex=0; cellIndex<nodeProtoCount; cellIndex++)
		{
			Cell cell = nodeProtoList[cellIndex];
			if (cell == null || cell.getLibrary() != lib) continue;
			cell.setCellGroup((Cell.CellGroup)cellGroups.get(cell));
		}

// 		// initialize for processing cell groups
// 		FlagSet cellFlag = Cell.getFlagSet(1);
// 		for(int cellIndex=0; cellIndex<nodeProtoCount; cellIndex++)
// 		{
// 			Cell cell = nodeProtoList[cellIndex];
// 			if (cell == null) continue;
// 			cell.clearBit(cellFlag);
// 		}

// 		// process the cell groups
// 		for(int cellIndex=0; cellIndex<nodeProtoCount; cellIndex++)
// 		{
// 			if (magic <= ELIBConstants.MAGIC9 && magic >= ELIBConstants.MAGIC11) continue;
// 			Cell cell = nodeProtoList[cellIndex];
// 			if (cell == null) continue;
// 			if (cell.isBit(cellFlag)) continue;

// 			// follow cell group links, give them a cell group
// 			Cell.CellGroup cg = new Cell.CellGroup();
// 			for(;;)
// 			{
// 				cell.setBit(cellFlag);
// 				cell.setCellGroup(cg);

// 				// move to the next in the group
// 				Object other = nextInCellGroup.get(cell);
// 				if (other == null) break;
// 				if (!(other instanceof Cell)) break;
// 				Cell otherCell = (Cell)other;
// 				if (otherCell == null) break;
// 				if (otherCell.isBit(cellFlag)) break;
// 				cell = otherCell;
// 			}
// 		}
// 		cellFlag.freeFlagSet();

		// cleanup cellgroup processing, link the cells
		for(int cellIndex=0; cellIndex<nodeProtoCount; cellIndex++)
		{
			Cell cell = nodeProtoList[cellIndex];
			if (cell == null) continue;
			cell.lowLevelLink();
		}

		// now read external cells
		for(int i=0; i<nodeProtoCount; i++)
		{
			Cell cell = nodeProtoList[i];
			if (cell != null) continue;
			if (readExternalNodeProto(lib, i))
			{
				System.out.println("Error reading external cell");
				return true;
			}
		}

		// now that external cells are resolved, fix all variables that may have used them
		fixExternalVariables(lib);
// 		for(int i=0; i<toolCount; i++)
// 		{
// 			Tool tool = toolList[i];
// 			fixExternalVariables(tool);
// 		}
// 		for(int i=0; i<techCount; i++)
// 		{
// 			Technology tech = techList[i];
// 			fixExternalVariables(tech);
// 		}
// 		for(int i=0; i<arcProtoCount; i++)
// 		{
// 			PrimitiveArc ap = arcProtoList[i];
// 			fixExternalVariables(ap);
// 		}
// 		for(int i=0; i<primNodeProtoCount; i++)
// 		{
// 			PrimitiveNode np = primNodeProtoList[i];
// 			fixExternalVariables(np);
// 		}
// 		for(int i=0; i<primPortProtoCount; i++)
// 		{
// 			PrimitivePort pp = primPortProtoList[i];
// 			fixExternalVariables(pp);
// 		}
// 		for(Iterator it = View.getViews(); it.hasNext(); )
// 		{
// 			View view = (View) it.next();
// 			fixExternalVariables(view);
// 		}
		for(int i=0; i<nodeProtoCount; i++)
		{
			Cell cell = nodeProtoList[i];
			fixExternalVariables(cell);
		}

		// read the cell contents: arcs and nodes
		int nodeIndex = 0, arcIndex = 0, geomIndex = 0;
		for(int cellIndex=0; cellIndex<nodeProtoCount; cellIndex++)
		{
			Cell cell = nodeProtoList[cellIndex];
			firstNodeIndex[cellIndex] = nodeIndex;
			firstArcIndex[cellIndex] = arcIndex;
			if (magic > ELIBConstants.MAGIC5)
			{
				// versions 4 and earlier must read some geometric information
				int j = geomIndex;
				readGeom(geomType, geomMoreUp, j);   j++;
				readGeom(geomType, geomMoreUp, j);   j++;
				int top = j;   readGeom(geomType, geomMoreUp, j);   j++;
				int bot = j;   readGeom(geomType, geomMoreUp, j);   j++;
				for(;;)
				{
					readGeom(geomType, geomMoreUp, j);   j++;
					if (geomMoreUp[j-1] == top) break;
				}
				geomIndex = j;
				for(int look = bot; look != top; look = geomMoreUp[look])
					if (!geomType[look])
				{
					if (readArcInst(arcIndex))
					{
						System.out.println("Error reading arc");
						return true;
					}
					arcIndex++;
				} else
				{
					if (readNodeInst(nodeIndex, cellIndex))
					{
						System.out.println("Error reading node");
						return true;
					}
					nodeIndex++;
				}
			} else
			{
				// version 5 and later find the arcs and nodes in linear order
				for(int j=0; j<arcCounts[cellIndex]; j++)
				{
					if (readArcInst(arcIndex))
					{
						System.out.println("Error reading arc");
						return true;
					}
					arcIndex++;
				}
				for(int j=0; j<nodeCounts[cellIndex]; j++)
				{
					if (readNodeInst(nodeIndex, cellIndex))
					{
						System.out.println("Error reading node index " + nodeIndex + " in cell " + cell.describe() + " of library " + lib.getName());
						return true;
					}
					nodeIndex++;
				}
			}
		}
		firstNodeIndex[nodeProtoCount] = nodeIndex;
		firstArcIndex[nodeProtoCount] = arcIndex;

		if (curCell >= 0 && curCell < nodeProtoCount)
		{
			NodeProto currentCell = convertNodeProto(curCell);
			lib.setCurCell((Cell)currentCell);
		}

        // warn if any dummy cells were read in
        for (Iterator it = lib.getCells(); it.hasNext(); ) {
            Cell c = (Cell)it.next();
            if (c.getVar(IO_DUMMY_OBJECT) != null) {
                System.out.println("WARNING: Library "+lib.getName()+" contains DUMMY cell "+c.noLibDescribe());
            }
        }

		// library read successfully
		if (LibraryFiles.VERBOSE)
			System.out.println("Binary: finished reading data for library " + lib.getName());
		return false;
	}

	// *************************** THE CELL CLEANUP INTERFACE ***************************

	/**
	 * Method to recursively create the contents of each cell in the library.
	 */
	protected void realizeCellsRecursively(Cell cell, FlagSet recursiveSetupFlag, String scaledCellName, double scaleX, double scaleY)
	{
		// do not realize cross-library references
		if (cell.getLibrary() != lib) return;

        // skip if dummy cell, already created
        boolean dummyCell = (cell.getVar(IO_DUMMY_OBJECT) != null) ? true : false;
        if (dummyCell) return;

		// get information about this cell
		int cellIndex = cell.getTempInt();
		if (cellIndex+1 >= firstNodeIndex.length) return;
		int startNode = firstNodeIndex[cellIndex];
		int endNode = firstNodeIndex[cellIndex+1];

		// recursively ensure that external library references are satisfied
		for(int i=startNode; i<endNode; i++)
		{
			NodeProto np = nodeInstList.protoType[i];
			if (np instanceof PrimitiveNode) continue;
			Cell subCell = (Cell)np;
			if (subCell.getLibrary() == lib) continue;

			// subcell: make sure that cell is setup
			for(int cI = 0; cI<nodeProtoCount; cI++)
			{
				if (nodeProtoList[cI] != subCell) continue;
				if (xLibRefSatisfied[cI]) break;

				// make sure that cell is properly built
				if (!subCell.isBit(recursiveSetupFlag))
				{
					LibraryFiles reader = getReaderForLib(subCell.getLibrary());
					if (reader != null)
						reader.realizeCellsRecursively(subCell, recursiveSetupFlag, null, 0, 0);
				}

				int startPort = firstPortIndex[cI];
				int endPort = startPort + portCounts[cI];
				for(int j=startPort; j<endPort; j++)
				{
					Object obj = portProtoList[j];
					Export pp = null;
					Cell otherCell = null;
					if (obj instanceof Cell)
					{
						otherCell = (Cell)obj;
						pp = otherCell.findExport(portProtoNameList[j]);
						if (pp != null)
						{
							portProtoList[j] = pp;
						}
					}
				}
				xLibRefSatisfied[cI] = true;
				break;
			}
		}

		// recursively scan the nodes to the bottom and only proceed when everything below is built
		scanNodesForRecursion(cell, recursiveSetupFlag, nodeInstList.protoType, startNode, endNode);

		// report progress
		if (LibraryFiles.VERBOSE)
		{
			if (scaledCellName == null)
			{
				System.out.println("Binary: Doing contents of cell " + cell.describe() + " in library " + lib.getName());
			} else
			{
				System.out.println("Binary: Scaling (by " + scaleX + "x" + scaleY + ") contents of cell " + cell.describe() + " in library " + lib.getName());
			}
		}
		cellsConstructed++;
		progress.setProgress(cellsConstructed * 100 / totalCells);

		double lambdaX = cellLambda[cellIndex];
		assert lambdaX > 0;
		double lambdaY = lambdaX;
		NodeInst [] oldNodes = null;

		// if scaling, actually construct the cell
		if (scaledCellName != null)
		{
			Cell oldCell = cell;
			cell = Cell.lowLevelAllocate(cell.getLibrary());
			cell.lowLevelPopulate(scaledCellName);
			cell.lowLevelLink();
			cell.setTempInt(cellIndex);
			cell.setBit(recursiveSetupFlag);
			cell.joinGroup(oldCell);
			if (scaleX != scaleY) skewedCells.add(cell); else
				scaledCells.add(cell);

			lambdaX /= scaleX;
			lambdaY /= scaleY;
			oldNodes = new NodeInst[endNode - startNode];
			int j = 0;
			for(int i=startNode; i<endNode; i++)
			{
				oldNodes[j] = nodeInstList.theNode[i];
				nodeInstList.theNode[i] = NodeInst.lowLevelAllocate();
				j++;
			}
		} else scaleX = scaleY = 1;

		// finish initializing the NodeInsts in the cell: start with the cell-center
		int xoff = 0, yoff = 0;
		for(int i=startNode; i<endNode; i++)
		{
			if (nodeInstList.protoType[i] == Generic.tech.cellCenterNode)
			{
				realizeNode(i, xoff, yoff, lambdaX, lambdaY, cell, recursiveSetupFlag);
				xoff = (nodeInstList.lowX[i] + nodeInstList.highX[i]) / 2;
				yoff = (nodeInstList.lowY[i] + nodeInstList.highY[i]) / 2;
				break;
			}
		}
		cellXOff[cellIndex] = xoff;
		cellYOff[cellIndex] = yoff;

		// finish creating the rest of the NodeInsts
		for(int i=startNode; i<endNode; i++)
		{
			if (nodeInstList.protoType[i] != Generic.tech.cellCenterNode)
			{
				realizeNode(i, xoff, yoff, lambdaX, lambdaY, cell, recursiveSetupFlag);
			}
		}

		// do the exports now
		realizeExports(cell, cellIndex, scaledCellName);

		// do the arcs now
		realizeArcs(cell, cellIndex, scaledCellName, scaleX, scaleY);

		// restore the node pointers if this was a scaled cell construction
		if (scaledCellName != null)
		{
			int j = 0;
			for(int i=startNode; i<endNode; i++)
				nodeInstList.theNode[i] = oldNodes[j++];
		}
	}

	protected boolean spreadLambda(Cell cell, int cellIndex)
	{
		boolean changed = false;
		int startNode = firstNodeIndex[cellIndex];
		int endNode = firstNodeIndex[cellIndex+1];
		double thisLambda = cellLambda[cellIndex];
		for(int i=startNode; i<endNode; i++)
		{
			NodeProto np = nodeInstList.protoType[i];
			if (np instanceof PrimitiveNode) continue;
			Cell subCell = (Cell)np;

            // ignore dummy cells, they are created in the default lambda
            if (subCell.getVar(IO_DUMMY_OBJECT) != null) continue;

			LibraryFiles reader = this;
			if (subCell.getLibrary() != lib)
			{
				reader = getReaderForLib(subCell.getLibrary());
				if (reader == null) continue;
			}
			int subCellIndex = subCell.getTempInt();
//			if (subCellIndex < 0 || subCellIndex >= reader.cellLambda.length)
//			{
//				System.out.println("Index is "+subCellIndex+" but limit is "+reader.cellLambda.length);
//				continue;
//			}
			double subLambda = reader.cellLambda[subCellIndex];
			if (subLambda < thisLambda)
			{
				reader.cellLambda[subCellIndex] = thisLambda;
				changed = true;
			}
		}
		return changed;
	}

	protected void computeTech(Cell cell, Set uncomputedCells)
	{
		uncomputedCells.remove(cell);
		int cellIndex = 0;
		for(; cellIndex<nodeProtoCount && nodeProtoList[cellIndex] != cell; cellIndex++);
		if (cellIndex >= nodeProtoCount) return;
		
		int startNode = firstNodeIndex[cellIndex];
		int endNode = firstNodeIndex[cellIndex+1];

		// recursively ensure that subcells's technologies are computed
		for(int i=startNode; i<endNode; i++)
		{
			NodeProto np = nodeInstList.protoType[i];
			if (!uncomputedCells.contains(np)) continue;
			Cell subCell = (Cell)np;
			LibraryFiles reader = getReaderForLib(subCell.getLibrary());
			if (reader != null)
				reader.computeTech(subCell, uncomputedCells);
		}

		int startArc = firstArcIndex[cellIndex];
		int endArc = firstArcIndex[cellIndex+1];
		Technology cellTech = Technology.whatTechnology(cell, nodeInstList.protoType, startNode, endNode, arcTypeList, startArc, endArc);
		cell.setTechnology(cellTech);
	}

	protected double computeLambda(Cell cell, int cellIndex)
	{
		double lambda = 1.0;
// 		int startNode = firstNodeIndex[cellIndex];
// 		int endNode = firstNodeIndex[cellIndex+1];
// 		int startArc = firstArcIndex[cellIndex];
// 		int endArc = firstArcIndex[cellIndex+1];
// 		Technology cellTech = Technology.whatTechnology(cell, nodeInstList.protoType, startNode, endNode, arcTypeList, startArc, endArc);
// 		cell.setTechnology(cellTech);
		Technology cellTech = cell.getTechnology();
		if (cellTech != null) lambda = techScale[cellTech.getIndex()];
		return lambda;
	}

	protected boolean canScale() { return true; }

	private void realizeExports(Cell cell, int cellIndex, String scaledCellName)
	{
		// finish initializing the Exports in the cell
		int startPort = firstPortIndex[cellIndex];
		int endPort = startPort + portCounts[cellIndex];
		for(int i=startPort; i<endPort; i++)
		{
//			if (portProtoList[i] instanceof Cell)
//			{
//				Cell otherCell = (Cell)portProtoList[i];
//				Export pp = otherCell.findExport(portProtoNameList[i]);
//				if (pp != null) portProtoList[i] = pp;
//			}
			if (!(portProtoList[i] instanceof Export))
            {
                // could be missing because this is a dummy cell
                if (cell.getVar(IO_DUMMY_OBJECT) != null)
                    continue;               // don't issue error message
                // not on a dummy cell, issue error message
                System.out.println("ERROR: Cell "+cell.describe() + ": export " + portProtoNameList[i] + " is unresolved");
                continue;
			}
			Export pp = (Export)portProtoList[i];
			if (scaledCellName != null)
			{
				String oldName = pp.getName();
				pp = Export.lowLevelAllocate();
				pp.lowLevelName(cell, oldName);
			}
			int nodeIndex = portProtoSubNodeList[i];
			if (nodeIndex < 0)
			{
				System.out.println("ERROR: Cell " + cell.describe() + ": cannot find the node on which export " + pp.getName() + " resides");
				continue;
			}
			NodeInst subNodeInst = nodeInstList.theNode[nodeIndex];
			if (portProtoSubPortList[i] instanceof Integer)
			{
				// this was an external reference that couldn't be resolved yet.  Do it now
				int index = ((Integer)portProtoSubPortList[i]).intValue();
				portProtoSubPortList[i] = convertPortProto(index);
			}
			PortProto subPortProto = (PortProto)portProtoSubPortList[i];

			// null entries happen when there are external cell references
			if (subNodeInst == null || subPortProto == null) {
                System.out.println("ERROR: Cell "+cell.describe() + ": export " + portProtoNameList[i] + " could not be created");
                continue;
            }
			if (subNodeInst.getProto() == null)
			{
                System.out.println("ERROR: Cell "+cell.describe() + ": export " + portProtoNameList[i] + " could not be created...proto bad!");
				continue;
			}

			// convert portproto to portinst
			String exportName = portProtoNameList[i];
			PortInst pi = subNodeInst.findPortInst(subPortProto.getName());
			if (pp.lowLevelPopulate(pi)) return;
			if (pp.lowLevelLink(null)) return;
			pp.lowLevelSetUserbits(portProtoUserbits[i]);
		}

		// convert "ATTRP_" variables on NodeInsts to be on PortInsts
		int startNode = firstNodeIndex[cellIndex];
		int endNode = firstNodeIndex[cellIndex+1];
		for(int i=startNode; i<endNode; i++)
		{
			NodeInst ni = nodeInstList.theNode[i];
			boolean found = true;
			while (found)
			{
				found = false;
				for(Iterator it = ni.getVariables(); it.hasNext(); )
				{
					Variable origVar = (Variable)it.next();
					Variable.Key origVarKey = origVar.getKey();
					String origVarName = origVarKey.getName();
					if (origVarName.startsWith("ATTRP_"))
					{
						// the form is "ATTRP_portName_variableName" with "\" escapes
						StringBuffer portName = new StringBuffer();
						String varName = null;
						int len = origVarName.length();
						for(int j=6; j<len; j++)
						{
							char ch = origVarName.charAt(j);
							if (ch == '\\')
							{
								j++;
								portName.append(origVarName.charAt(j));
								continue;
							}
							if (ch == '_')
							{
								varName = origVarName.substring(j+1);
								break;
							}
							portName.append(ch);
						}
						if (varName != null)
						{
							String thePortName = portName.toString();
							PortInst pi = ni.findPortInst(thePortName);
							if (pi != null)
							{
								Variable var = pi.newVar(varName, origVar.getObject());
								if (var != null)
								{
									if (origVar.isDisplay()) var.setDisplay(true);
									var.setCode(origVar.getCode());
									var.setTextDescriptor(origVar.getTextDescriptor());									
								}
								ni.delVar(origVarKey);
								found = true;
								break;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Method to create the ArcInsts in a given cell and it's index in the global lists.
	 */
	private void realizeArcs(Cell cell, int cellIndex, String scaledCellName, double scaleX, double scaleY)
	{
		double lambdaX = cellLambda[cellIndex] / scaleX;
		double lambdaY = cellLambda[cellIndex] / scaleY;
		int xoff = cellXOff[cellIndex];
		int yoff = cellYOff[cellIndex];
		boolean arcInfoError = false;
		int startArc = firstArcIndex[cellIndex];
		int endArc = firstArcIndex[cellIndex+1];
		for(int i=startArc; i<endArc; i++)
		{
			ArcInst ai = arcList[i];
			if (scaledCellName != null)
				ai = ArcInst.lowLevelAllocate();

			ArcProto ap = arcTypeList[i];
			Name name = arcNameList[i];
			double width = arcWidthList[i] / lambdaX;
			double headX = (arcHeadXPosList[i] - xoff) / lambdaX;
			double headY = (arcHeadYPosList[i] - yoff) / lambdaY;
			double tailX = (arcTailXPosList[i] - xoff) / lambdaX;
			double tailY = (arcTailYPosList[i] - yoff) / lambdaY;
			if (arcHeadNodeList[i] < 0)
			{
				System.out.println("ERROR: head of arc " + ap.describe() + " not known");
				continue;
			}
			NodeInst headNode = nodeInstList.theNode[arcHeadNodeList[i]];
			Object headPort = arcHeadPortList[i];
			int headPortIntValue = -1;
            String headname = "Port name not found";
			if (headPort instanceof Integer)
			{
				// this was an external reference that couldn't be resolved yet.  Do it now
				headPortIntValue = ((Integer)headPort).intValue();
				headPort = convertPortProto(headPortIntValue);
			}
            if (headPort != null) {
                headname = ((PortProto)headPort).getName();
            } else {
                if (headPortIntValue >= 0 && headPortIntValue < portProtoNameList.length)
                    headname = portProtoNameList[headPortIntValue];
            }

			if (arcTailNodeList[i] < 0)
			{
				System.out.println("ERROR: tail of arc " + ap.describe() + " not known");
				continue;
			}
			NodeInst tailNode = nodeInstList.theNode[arcTailNodeList[i]];
			Object tailPort = arcTailPortList[i];
			int tailPortIntValue = -1;
            String tailname = "Port name not found";
			if (tailPort instanceof Integer)
			{
				// this was an external reference that couldn't be resolved yet.  Do it now
				tailPortIntValue = ((Integer)tailPort).intValue();
				tailPort = convertPortProto(tailPortIntValue);
                if (tailPortIntValue > 0 && tailPortIntValue < portProtoNameList.length)
                    tailname = portProtoNameList[tailPortIntValue];
			}
            if (tailPort != null) {
                tailname = ((PortProto)tailPort).getName();
            } else {
                if (tailPortIntValue >= 0 && tailPortIntValue < portProtoNameList.length)
                    tailname = portProtoNameList[tailPortIntValue];
            }
            /*			if (headNode == null || headPort == null || tailNode == null || tailPort == null)
			{
				if (!arcInfoError)
				{
					System.out.println("ERROR: Missing arc information in cell " + cell.noLibDescribe() +
						" in library " + lib.getName() + " ...");
					if (headNode == null) System.out.println("   Head node not found");
					if (headPort == null) System.out.println("   Head port "+headname+" not found (was "+headPortIntValue+", node="+headNode+")");
					if (tailNode == null) System.out.println("   Tail node not found");
					if (tailPort == null) System.out.println("   Tail port "+tailname+" not found (was "+tailPortIntValue+", node="+tailNode+")");
					arcInfoError = true;
				}
				continue;
			}*/
            //PortInst headPortInst = headNode.findPortInst(((PortProto)headPort).getName());
            //PortInst tailPortInst = tailNode.findPortInst(((PortProto)tailPort).getName());
            PortInst headPortInst = getArcEnd(ai, ap, headNode, headname, headX, headY, cell);
            PortInst tailPortInst = getArcEnd(ai, ap, tailNode, tailname, tailX, tailY, cell);
			if (headPortInst == null || tailPortInst == null)
			{
				System.out.println("Cannot create arc of type " + ap.getName() + " in cell " + cell.getName() +
					" because ends are unknown");
				continue;
			}
			ai.lowLevelSetUserbits(arcUserBits[i]);
			int defAngle = ai.lowLevelGetArcAngle() * 10;
			ai.lowLevelPopulate(ap, width, tailPortInst, new Point2D.Double(tailX, tailY), headPortInst, new Point2D.Double(headX, headY), defAngle);
			if (name != null) ai.setNameKey(name);
			if ((arcUserBits[i]&ELIBConstants.ISNEGATED) != 0)
			{
				Connection con = ai.getTail();
				if (ai.isReverseEnds()) con = ai.getHead();
				con.setNegated(true);
			}
			if ((arcUserBits[i]&ELIBConstants.ISHEADNEGATED) != 0)
			{
				Connection con = ai.getHead();
				if (ai.isReverseEnds()) con = ai.getTail();
				con.setNegated(true);
			}
			ai.lowLevelLink();
		}
	}

	/**
	 * Method to build a NodeInst.
	 */
	private void realizeNode(int i, int xoff, int yoff, double lambdaX, double lambdaY, Cell cell, FlagSet recursiveSetupFlag)
	{
		NodeInst ni = nodeInstList.theNode[i];
		NodeProto np = nodeInstList.protoType[i];
		Name name = nodeInstList.name[i];
		double lowX = nodeInstList.lowX[i]-xoff;
		double lowY = nodeInstList.lowY[i]-yoff;
		double highX = nodeInstList.highX[i]-xoff;
		double highY = nodeInstList.highY[i]-yoff;
		Point2D center = new Point2D.Double(((lowX + highX) / 2) / lambdaX, ((lowY + highY) / 2) / lambdaY);
		double width = (highX - lowX) / lambdaX;
		double height = (highY - lowY) / lambdaY;
        double anchorX = (nodeInstList.anchorX[i]-xoff) / lambdaX;
        double anchorY = (nodeInstList.anchorY[i]-yoff) / lambdaY;
		if (np instanceof Cell)
		{
			Cell subCell = (Cell)np;
			LibraryFiles reader = this;
			if (subCell.getLibrary() != lib)
			{
				reader = getReaderForLib(subCell.getLibrary());
			}
			Rectangle2D bounds = subCell.getBounds();
            //if (false)
			if ((bounds.getWidth() != width || bounds.getHeight() != height)
				&& reader != null && reader.canScale())
			{
				if (Math.abs(bounds.getWidth() - width) > 0.5 ||
					Math.abs(bounds.getHeight() - height) > 0.5)
				{
					// see if uniform scaling can be done
					double scaleX = width / bounds.getWidth();
					double scaleY = height / bounds.getHeight();
					String scaledCellName = subCell.getName() + "-SCALED-BY-" + scaleX +
						"{" + subCell.getView().getAbbreviation() + "}";
					if (!GenMath.doublesClose(scaleX, scaleY))
					{
                        // don't scale, most likely the size changed, and this is not a lambda problem
                        //scaledCellName = null;
						// nonuniform scaling: library inconsistency detected
						scaledCellName = subCell.getName() + "-SCALED-BY-" + scaleX + "-AND-" + scaleY +
							"{" + subCell.getView().getAbbreviation() + "}";
					} else {
					Cell scaledCell = subCell.getLibrary().findNodeProto(scaledCellName);
					if (scaledCell == null)
					{
						// create a scaled version of the cell
						if (reader != null)
							reader.realizeCellsRecursively(subCell, recursiveSetupFlag, scaledCellName, scaleX, scaleY);
						scaledCell = subCell.getLibrary().findNodeProto(scaledCellName);
						if (scaledCell == null)
						{
							System.out.println("Error scaling cell " + subCell.describe() + " by " + scaleX);
						}
					}
					if (scaledCell != null)
					{
						bounds = scaledCell.getBounds();
						np = scaledCell;
					}

					if (Math.abs(bounds.getWidth() - width) > 0.5 ||
						Math.abs(bounds.getHeight() - height) > 0.5)
					{
						// see if there is already a dummy cell that is the right size
						Cell dummyCell = null;
//						String theProtoName = np.getName();
//						String startString = theProtoName + "FROM" + lib.getName();
//						View v = subCell.getView();
//						for(Iterator it = lib.getCells(); it.hasNext(); )
//						{
//							Cell oC = (Cell)it.next();
//							if (!oC.getName().startsWith(startString)) continue;
//							if (oC.getView() != v) continue;
//							Rectangle2D oCBounds = oC.getBounds();
//							if (Math.abs(oCBounds.getWidth() - width) <= 0.5 ||
//								Math.abs(oCBounds.getHeight() - height) <= 0.5)
//							{
//								dummyCell = oC;
//								break;
//							}
//						}
//						if (dummyCell == null)
//						{
//							String dummyCellName = null;
//							for(int index=0; ; index++)
//							{
//								dummyCellName = new String(startString);
//								if (index > 0) dummyCellName += "." + index;
//								dummyCellName += "{" + v.getAbbreviation() + "}";
//								if (lib.findNodeProto(dummyCellName) == null) break;
//							}
//							dummyCell = Cell.newInstance(lib, dummyCellName);
//							if (dummyCell != null)
//							{
//								// create an artwork "Crossed box" to define the cell size
//								Technology tech = MoCMOS.tech;
//								if (v == View.ICON) tech = Artwork.tech; else
//									if (v == View.SCHEMATIC) tech = Schematics.tech;
//								Point2D ctr = new Point2D.Double(0, 0);
//								NodeInst.newInstance(Generic.tech.drcNode, ctr, width, height, 0, dummyCell, null);
//								NodeInst.newInstance(Generic.tech.universalPinNode, ctr, width, height, 0, dummyCell, null);
//		
//								System.out.println("...Creating dummy version of cell in library " + lib.getName());
////								oC.newVar(IO_TRUE_LIBRARY, elib.getName());
//							}
//						}

						if (dummyCell == null)
						{
							System.out.println("Cell " + cell.describe() + ": adjusting size of instance of " + subCell.describe() +
								" (cell is " + bounds.getWidth() + "x" + bounds.getHeight() +
								" but instance is " + width + "x" + height + ")");
						} else
						{
							np = dummyCell;
							bounds = dummyCell.getBounds();
						}
					}
                    }
				}
				width = bounds.getWidth();
				height = bounds.getHeight();
			}
		}

		int rotation = nodeInstList.rotation[i];
		if (rotationMirrorBits)
// 		if (emajor > 7 || (emajor == 7 && eminor >= 1))
		{
			// new version: allow mirror bits
			if ((nodeInstList.transpose[i]&1) != 0)
			{
				height = -height;
				rotation = (rotation + 900) % 3600;
			}
			if ((nodeInstList.transpose[i]&2) != 0)
			{
				// mirror in X
				width = -width;
			}
			if ((nodeInstList.transpose[i]&4) != 0)
			{
				// mirror in Y
				height = -height;
			}
		} else
		{
			// old version: just use transpose information
			if (nodeInstList.transpose[i] != 0)
			{
				height = -height;
				rotation = (rotation + 900) % 3600;
			}
		}

		// figure out the grab center if this is a cell instance
		if (np instanceof Cell)
		{
			Cell subCell = (Cell)np;
			Rectangle2D bounds = subCell.getBounds();
			Point2D shift = new Point2D.Double(-bounds.getCenterX(), -bounds.getCenterY());
			AffineTransform trans = NodeInst.pureRotate(rotation, width < 0, height < 0);
			trans.transform(shift, shift);
            if (magic <= ELIBConstants.MAGIC13)
            {
                // version 13 and later stores and uses anchor location
                center.setLocation(anchorX, anchorY);
            } else
            {
                center.setLocation(center.getX() + shift.getX(), center.getY() + shift.getY());
            }
		}
		// convert outline information, if present
		scaleOutlineInformation(ni, np, lambdaX, lambdaY);
		ni.lowLevelSetUserbits(nodeInstList.userBits[i]);
		ni.lowLevelPopulate(np, center, width, height, rotation, cell);
		if (name != null) ni.setNameKey(name);
		ni.lowLevelLink();

        // if this was a dummy cell, log instance as an error so the user can find easily
        if (np instanceof Cell && ((Cell)np).getVar(IO_DUMMY_OBJECT) != null) {
            ErrorLogger.MessageLog error = Input.errorLogger.logError("Instance of dummy cell "+np.getName(), cell, 1);
            error.addGeom(ni, true, cell, null);
        }
	}

	protected boolean readerHasExport(Cell c, String portName)
	{
		for(int cellIndex=0; cellIndex<nodeProtoCount; cellIndex++)
		{
			Cell cell = nodeProtoList[cellIndex];
			if (cell != c) continue;
			int startPort = firstPortIndex[cellIndex];
			int endPort = startPort + portCounts[cellIndex];
			for(int i=startPort; i<endPort; i++)
			{
				String exportName = portProtoNameList[i];
				if (exportName.equalsIgnoreCase(portName)) return true;
			}
			break;
		}
		return false;
	}

    // node is node we expect to have port 'portname' at location x,y.
    protected PortInst getArcEnd(ArcInst ai, ArcProto ap, NodeInst node, String portname, double x, double y, Cell cell)
    {
        PortInst pi = null;
        String whatHappenedToPort = "not found";
        String nodeName = "missing node";

        if (node != null) {
            pi = node.findPortInst(portname);
            nodeName = node.getName();

            if (pi != null) {
                // check to make sure location is correct
                Poly portLocation = pi.getPoly();
                String extra = "";

	            // Forcing rounding here instead of PolyBase.calcBounds()
//	            portLocation.roundPoints();
                if (portLocation.contains(x, y) || portLocation.polyDistance(x, y) < TINYDISTANCE) {
                    return pi;
                }
                // give extra info to user if didn't contain port
                Rectangle2D box = portLocation.getBox();

                if (box != null) {
                    extra = "...expected ("+x+","+y+"), found ("+box.getCenterX()+","+box.getCenterY()+")";
                } else
				{
                    extra = "...expected ("+x+","+y+"), polyDistance=" + portLocation.polyDistance(x, y);
				}
                whatHappenedToPort = "has moved"+extra;
                pi = null;
            } else {
                // name not found, see if any ports exist at location that we can connect to
                for (Iterator it = node.getPortInsts(); it.hasNext(); ) {
                    pi = (PortInst)it.next();
                    Poly portLocation = pi.getPoly();
                    if (portLocation.contains(x, y)) {
                        if (pi.getPortProto().connectsTo(ap)) {
                            // connect to this port
                            String msg = "Cell "+cell.describe()+": Port '"+portname+"' on '"+nodeName+"' not found, connecting to port '"+
                                    pi.getPortProto().getName()+"' at the same location";
                            System.out.println("ERROR: "+msg);
                            ErrorLogger.MessageLog error = Input.errorLogger.logError(msg, cell, 0);
                            error.addGeom(ai, true, cell, null);
                            return pi;
                        }
                    }
                    pi = null;
                }
                whatHappenedToPort = "is missing";
            }

            // if this was a dummy cell, create the export in cell
            Cell c = null;
            if (node.getProto() != null && node.getProto() instanceof Cell)
                if (((Cell)node.getProto()).getVar(IO_DUMMY_OBJECT) != null) c = (Cell)node.getProto();
            if (c != null) {
                double anchorX = node.getAnchorCenterX();
                double anchorY = node.getAnchorCenterY();
                Point2D expected = new Point2D.Double(x, y);
                AffineTransform trans = node.rotateIn();
                expected = trans.transform(expected, expected);
                Point2D center = new Point2D.Double(expected.getX() - anchorX, expected.getY() - anchorY);
                PrimitiveNode pn = Generic.tech.universalPinNode;
                NodeInst ni = NodeInst.newInstance(pn, center, 0, 0, c, 0, "", 0);
                Export ex = Export.newInstance(c, ni.getOnlyPortInst(), portname, false);
                if (ex != null) {
                    return node.findPortInst(portname);
                }
            }
        }


        // create pin as new end point of arc
        String msg = "Cell "+cell.describe()+": Port '"+portname+"' on '"+nodeName+"' "+whatHappenedToPort+": leaving arc disconnected";
        System.out.println("ERROR: "+msg);
        ErrorLogger.MessageLog error = Input.errorLogger.logError(msg, cell, 0);
        error.addGeom(ai, true, cell, null);

        PrimitiveNode pn = ((PrimitiveArc)ap).findOverridablePinProto();
        node = NodeInst.newInstance(pn, new Point2D.Double(x, y), pn.getDefWidth(), pn.getDefHeight(), cell);
        error.addGeom(node, true, cell, null);
        return node.getOnlyPortInst();
    }

	// --------------------------------- HIGH-LEVEL OBJECTS ---------------------------------

	/**
	 * Method to read the header information of an "elib" file.
	 * The header consists of the "magic" number and the size of various pieces of data.
	 * Returns true on error.
	 */
	private boolean readHeader()
		throws IOException
	{
		bytesSwapped = false;
		byte byte1 = readByte();
		byte byte2 = readByte();
		byte byte3 = readByte();
		byte byte4 = readByte();
		magic = ((byte4&0xFF) << 24) | ((byte3&0xFF) << 16) | ((byte2&0xFF) << 8) | (byte1&0xFF);
		if (magic != ELIBConstants.MAGIC1 && magic != ELIBConstants.MAGIC2 &&
			magic != ELIBConstants.MAGIC3 && magic != ELIBConstants.MAGIC4 &&
			magic != ELIBConstants.MAGIC5 && magic != ELIBConstants.MAGIC6 &&
			magic != ELIBConstants.MAGIC7 && magic != ELIBConstants.MAGIC8 &&
			magic != ELIBConstants.MAGIC9 && magic != ELIBConstants.MAGIC10 &&
			magic != ELIBConstants.MAGIC11 && magic != ELIBConstants.MAGIC12 &&
			magic != ELIBConstants.MAGIC13)
		{
			magic = ((byte1&0xFF) << 24) | ((byte2&0xFF) << 16) | ((byte3&0xFF) << 8) | (byte4&0xFF);
			if (magic != ELIBConstants.MAGIC1 && magic != ELIBConstants.MAGIC2 &&
				magic != ELIBConstants.MAGIC3 && magic != ELIBConstants.MAGIC4 &&
				magic != ELIBConstants.MAGIC5 && magic != ELIBConstants.MAGIC6 &&
				magic != ELIBConstants.MAGIC7 && magic != ELIBConstants.MAGIC8 &&
				magic != ELIBConstants.MAGIC9 && magic != ELIBConstants.MAGIC10 &&
				magic != ELIBConstants.MAGIC11 && magic != ELIBConstants.MAGIC12 &&
				magic != ELIBConstants.MAGIC13)
			{
				System.out.println("Bad file format: does not start with proper magic number");
				return true;
			}
			bytesSwapped = true;
		}

		// determine the size of "big" and "small" integers as well as characters on disk
		if (magic <= ELIBConstants.MAGIC10)
		{
			sizeOfSmall = readByte();
			sizeOfBig = readByte();
		} else
		{
			sizeOfSmall = 2;
			sizeOfBig = 4;
		}
		if (magic <= ELIBConstants.MAGIC11)
		{
			sizeOfChar = readByte();
		} else
		{
			sizeOfChar = 1;
		}
		return false;
	}

	/**
	 * Method to read a cell.  returns true upon error
	 */
	private boolean readNodeProto(Cell cell, int cellIndex, HashMap nextInCellGroup)
		throws IOException
	{
		// read the cell name
		String theProtoName;
		if (magic <= ELIBConstants.MAGIC9)
		{
			// read the cell information (version 9 and later)
			if (magic >= ELIBConstants.MAGIC11)
			{
				// only versions 9 to 11
				int k = readBigInteger();
				theProtoName = fakeCellList[k].cellName;
			} else
			{
				// version 12 or later
				theProtoName = convertCellName(readString());
				int k = readBigInteger();

                // fix for new cell version library corruption bug
                if (k == -1) {
                    // find self in list
                    for (int i=0; i<nodeProtoList.length; i++) {
                        if (cell == nodeProtoList[i]) { k = i; break; }
                    }
                }

                nextInCellGroup.put(cell, nodeProtoList[k]);		// the "next in cell group" circular pointer
				k = readBigInteger();
//				cell->nextcont = nodeProtoList[k];		// the "next in cell continuation" circular pointer
			}
			View v = getView(readBigInteger());
			if (v == null) v = View.UNKNOWN;
			int version = readBigInteger();
			theProtoName += ";" + version + "{" + v.getAbbreviation() + "}";
			int creationDate = readBigInteger();
			int revisionDate = readBigInteger();
			cell.lowLevelSetCreationDate(ELIBConstants.secondsToDate(creationDate));
			cell.lowLevelSetRevisionDate(ELIBConstants.secondsToDate(revisionDate));
		} else
		{
			// versions 8 and earlier read a cell name
			theProtoName = readString();
		}
		cell.lowLevelPopulate(theProtoName);

		// ignore the cell bounding box
		int lowX = readBigInteger();
		int highX = readBigInteger();
		int lowY = readBigInteger();
		int highY = readBigInteger();

		// ignore the linked list pointers (versions 5 or older)
		if (magic >= ELIBConstants.MAGIC5)
		{
			int prevIndex = readBigInteger();
			int nextIndex = readBigInteger();
		}

		// read the exports on this nodeproto
		firstPortIndex[cellIndex] = portProtoIndex;
		int portCount = readBigInteger();
		if (portCount != portCounts[cellIndex])
			System.out.println("Error! Cell header lists " + portCounts[cellIndex] + " exports, but body lists " + portCount);
		for(int j=0; j<portCount; j++)
		{
			// set pointers to portproto
			Export pp = (Export)portProtoList[portProtoIndex];

			// read the sub-NodeInst for this Export
			portProtoSubNodeList[portProtoIndex] = -1;
			int whichNode = readBigInteger();
			if (whichNode >= 0 && whichNode < nodeCount)
				portProtoSubNodeList[portProtoIndex] = whichNode;

			// read the sub-PortProto on the sub-NodeInst
			portProtoSubPortList[portProtoIndex] = null;
			int whichPort = readBigInteger();
			if (whichPort < 0 || portProtoList[whichPort] != null)
				portProtoSubPortList[portProtoIndex] = convertPortProto(whichPort);
			if (portProtoSubPortList[portProtoIndex] == null)
			{
				portProtoSubPortList[portProtoIndex] = new Integer(whichPort);
			}

			// read the Export name
			String exportName = readString();
			portProtoNameList[portProtoIndex] = exportName;
			if (pp.lowLevelName(cell, exportName)) return true;

			if (portProtoSubNodeList[portProtoIndex] == -1)
			{
				System.out.println("Error: Export '" + exportName + "' of cell " + theProtoName +
					" cannot be read properly");
			}

			// read the portproto text descriptor
			int descript0 = 0, descript1 = 0;
			if (magic <= ELIBConstants.MAGIC9)
			{
				if (convertTextDescriptors)
				{
					// conversion is done later
					descript0 = readBigInteger();
					descript1 = 0;
				} else
				{
					descript0 = readBigInteger();
					descript1 = readBigInteger();
				}
			}
			TextDescriptor descript = new TextDescriptor(null, descript0, descript1, 0);
			Input.fixTextDescriptorFont(descript);
			pp.setTextDescriptor(descript);

			// ignore the "seen" bits (versions 8 and older)
			if (magic > ELIBConstants.MAGIC9) readBigInteger();

			// read the portproto's "user bits"
			portProtoUserbits[portProtoIndex] = 0;
			if (magic <= ELIBConstants.MAGIC7)
			{
				// version 7 and later simply read the relevant data
				portProtoUserbits[portProtoIndex] = readBigInteger();

				// versions 7 and 8 ignore net number
				if (magic >= ELIBConstants.MAGIC8) readBigInteger();
			} else
			{
				// version 6 and earlier must sift through the information
				if (toolBCount >= 1) portProtoUserbits[portProtoIndex] = readBigInteger();
				for(int i=1; i<toolBCount; i++) readBigInteger();
			}

			// read the export variables
			if (readVariables(pp, -1) < 0) return true;
			Input.fixVariableFont(pp);

			portProtoIndex++;
		}

		// ignore the cell's geometry information
		if (magic > ELIBConstants.MAGIC5)
		{
			// versions 4 and older have geometry module pointers (ignore it)
			readBigInteger();
			readBigInteger();
			readBigInteger();
			readBigInteger();
			readBigInteger();
		}

		// read tool information
		int dirty = readBigInteger();
		
		// read the "user bits"
		int userBits = 0;
		if (magic <= ELIBConstants.MAGIC7)
		{
			// version 7 and later simply read the relevant data
			userBits = readBigInteger();

			// versions 7 and 8 ignore net number
			if (magic >= ELIBConstants.MAGIC8) readBigInteger();
		} else
		{
			// version 6 and earlier must sift through the information
			if (toolBCount >= 1) userBits = readBigInteger();
			for(int i=1; i<toolBCount; i++) readBigInteger();
		}
		cell.lowLevelSetUserbits(userBits);

		// read variable information
		if (readVariables(cell, -1) < 0) return true;
		Input.fixVariableFont(cell);

		// cell read successfully
		return false;
	}

	/** Method to read node prototype for external references */
	private boolean readExternalNodeProto(Library lib, int cellIndex)
		throws IOException
	{
		// read the cell information (version 9 and later)
		String theProtoName;
		if (magic >= ELIBConstants.MAGIC11)
		{
			// only versions 9 to 11
			int k = readBigInteger();
			theProtoName = fakeCellList[k].cellName;
		} else
		{
			// version 12 or later
			theProtoName = convertCellName(readString());
			int k = readBigInteger();
//			cell->nextcellgrp = nodeProtoList[k];
			k = readBigInteger();
//			cell->nextcont = nodeProtoList[k];
		}
		View v = getView(readBigInteger());
		if (v == null) v = View.UNKNOWN;
		int version = readBigInteger();
		String fullCellName = theProtoName + ";" + version + "{" + v.getAbbreviation() + "}";
        if (version <= 1) fullCellName = theProtoName + "{" + v.getAbbreviation() + "}";
		Date creationDate = ELIBConstants.secondsToDate(readBigInteger());
		Date revisionDate = ELIBConstants.secondsToDate(readBigInteger());

		// read the nodeproto bounding box
		int lowX = readBigInteger();
		int highX = readBigInteger();
		int lowY = readBigInteger();
		int highY = readBigInteger();

		// get the external library
		Library elib = readExternalLibraryFromFilename(readString(), FileType.ELIB);

 		// read the portproto names on this nodeproto
		int portCount = readBigInteger();
		String [] localPortNames = new String[portCount];
		for(int j=0; j<portCount; j++)
			localPortNames[j] = readString();

		// find this cell in the external library
		Cell c = elib.findNodeProto(fullCellName);

        // if can't find, look for dummy version
        //String dummyName = "DUMMY" + fullCellName;
        String dummyName = fullCellName;
        if (c == null) {
            c = elib.findNodeProto(dummyName);
        }

		if (c == null)
		{
			// cell not found in library: issue warning
			System.out.println("ERROR: Cannot find cell " + fullCellName + " in library " + elib.getName());
		}

		// if cell found, check that size is unchanged (size is unknown at this point)
//		if (c != null)
//		{
//			Rectangle2D bounds = c.getBounds();
//			double lambda = 1;
//			Technology cellTech = Technology.whatTechnology(c);
//			if (cellTech != null) lambda = techScale[cellTech.getIndex()];
//			double cellWidth = (highX - lowX) / lambda;
//			double cellHeight = (highY - lowY) / lambda;
//			if (!DBMath.doublesEqual(bounds.getWidth(), cellWidth) || !DBMath.doublesEqual(bounds.getHeight(), cellHeight))
//			{
//				// bounds differ, but lambda scaling is inaccurate: see if aspect ratio changed
//				if (!DBMath.doublesEqual(bounds.getWidth() * cellHeight, bounds.getHeight() * cellWidth))
//				{
//					System.out.println("Error: cell " + c.noLibDescribe() + " in library " + elib.getName() +
//						" has changed size since its use in library " + lib.getName());
//					System.out.println("  The cell is " + bounds.getWidth() + "x" +  bounds.getHeight() +
//						" but the instances in library " + lib.getName() + " are " + cellWidth + "x" + cellHeight);
//					c = null;
//				}
//			}
//		}

		// if cell found, check that ports match
/*
		if (c != null)
		{
			for(int j=0; j<portCount; j++)
			{
				PortProto pp = c.findExport(localPortNames[j]);
				if (pp == null)
				{
					LibraryFiles reader = getReaderForLib(elib);
					if (reader != null)
					{
						if (reader.readerHasExport(c, localPortNames[j])) continue;
					}

					System.out.println("Error: cell " + c.noLibDescribe() + " in library " + elib.getName() +
						" is missing export '" + localPortNames[j] + "'");
// 					for (Iterator it = c.getPorts(); it.hasNext(); )
// 					{
// 						PortProto ppo = (PortProto)it.next();
// 						System.out.println("\t"+ppo.getName());
// 					}
					c = null;
					break;
				}
			}
		}
*/

		// if cell found, warn if minor modification was made
		if (c != null)
		{
			if (revisionDate.compareTo(c.getRevisionDate()) != 0)
			{
				System.out.println("Warning: cell " + c.noLibDescribe() + " in library " + elib.getName() +
					" has changed since its use in library " + lib.getName());
			}
		}

		// make new cell if needed
		boolean newCell = false;
		NodeInst fakeNodeInst = null;
		if (c == null)
		{
			// create a cell that meets these specs
/*
			String dummyCellName = null;
			for(int index=0; ; index++)
			{
				dummyCellName = theProtoName + "FROM" + elib.getName();
				if (index > 0) dummyCellName += "." + index;
				dummyCellName += "{" + v.getAbbreviation() + "}";
				if (lib.findNodeProto(dummyCellName) == null) break;
			}
			c = Cell.newInstance(lib, dummyCellName);
*/
            c = Cell.newInstance(elib, dummyName);
            if (c == null) return true;
            c.lowLevelSetCreationDate(creationDate);
            c.lowLevelSetRevisionDate(revisionDate);

            // create an artwork "Crossed box" to define the cell size
            Technology tech = MoCMOS.tech;
            if (c.isIcon()) tech = Artwork.tech; else
                if (c.isSchematic()) tech = Schematics.tech;
            double lambda = techScale[tech.getIndex()];
            int cX = (lowX + highX) / 2;
            int cY = (lowY + highY) / 2;
            double width = (highX - lowX) / lambda;
            double height = (highY - lowY) / lambda;
            Point2D center = new Point2D.Double(cX / lambda, cY / lambda);
            NodeInst.newInstance(Generic.tech.drcNode, center, width, height, c);
            //PrimitiveNode cellCenter = Generic.tech.cellCenterNode;
            //NodeInst.newInstance(cellCenter, new Point2D.Double(0,0), cellCenter.getDefWidth(),
            //        cellCenter.getDefHeight(), 0, c, null);
            //fakeNodeInst = NodeInst.newInstance(Generic.tech.universalPinNode, center, width, height, 0, c, null);

            // note that exports get created in getArcEnd. If it tries to connect to a missing export
            // on a dummy cell, it creates the export site.

            newCell = true;
            System.out.println("...Creating dummy cell '"+dummyName+"' in library " + elib.getName() +
                    ". Instances will be logged as Errors.");
            c.newVar(IO_TRUE_LIBRARY, elib.getName());
            c.newVar(IO_DUMMY_OBJECT, fullCellName);
		}
		nodeProtoList[cellIndex] = c;

		// read the portprotos on this Cell
		firstPortIndex[cellIndex] = portProtoIndex;
		if (portCount != portCounts[cellIndex])
			System.out.println("Error! Cell header lists " + portCounts[cellIndex] + " exports, but body lists " + portCount);
		for(int j=0; j<portCount; j++)
		{
			// read the portproto name
			String protoName = localPortNames[j];
			portProtoList[portProtoIndex] = c;
			portProtoNameList[portProtoIndex] = protoName;
			portProtoIndex++;
		}
		return false;
	}

	/**
	 * Method to read a node instance.  returns true upon error
	 */
	private boolean readNodeInst(int nodeIndex, int cellIndex)
		throws IOException
	{
		// read the nodeproto index
		Cell parent = nodeProtoList[cellIndex];
		NodeInst ni = nodeInstList.theNode[nodeIndex];
		int protoIndex = readBigInteger();
		NodeProto np = convertNodeProto(protoIndex);
		if (np == null) return true;

		// read the descriptive information
		nodeInstList.protoType[nodeIndex] = np;
		nodeInstList.lowX[nodeIndex] = readBigInteger();
		nodeInstList.lowY[nodeIndex] = readBigInteger();
		nodeInstList.highX[nodeIndex] = readBigInteger();
		nodeInstList.highY[nodeIndex] = readBigInteger();
		if (magic <= ELIBConstants.MAGIC13)
		{
			// read anchor point for cell references
			if (np instanceof Cell)
			{
				nodeInstList.anchorX[nodeIndex] = readBigInteger();
				nodeInstList.anchorY[nodeIndex] = readBigInteger();
			}
		}

		nodeInstList.transpose[nodeIndex] = readBigInteger();
		nodeInstList.rotation[nodeIndex] = (short)readBigInteger();
		nodeInstList.name[nodeIndex] = null;

		// versions 9 and later get text descriptor for cell name
		int descript0 = 0, descript1 = 0;
		if (magic <= ELIBConstants.MAGIC9)
		{
			if (convertTextDescriptors)
			{
				// conversion is done later
				descript0 = readBigInteger();
			} else
			{
				descript0 = readBigInteger();
				descript1 = readBigInteger();
			}
		}
		TextDescriptor descript = new TextDescriptor(null, descript0, descript1, 0);
		Input.fixTextDescriptorFont(descript);
		ni.setProtoTextDescriptor(descript);

		// read the nodeinst name (versions 1, 2, or 3 only)
		if (magic >= ELIBConstants.MAGIC3)
		{
			String instName = readString();
			if (instName.length() > 0)
				ni.setName(instName);
		}

		// ignore the geometry index (versions 4 or older)
		if (magic > ELIBConstants.MAGIC5) readBigInteger();

		// read the arc ports
		int numPorts = readBigInteger();
		for(int j=0; j<numPorts; j++)
		{
			// read the arcinst information (and the particular end on the arc)
			int k = readBigInteger();
			int arcIndex = k >> 1;
			if (k < 0 || arcIndex >= arcCount) return true;

			// read the port information
			int portIndex = readBigInteger();
			Object pp = convertPortProto(portIndex);
			if (pp == null)
				pp = new Integer(portIndex);
			if ((k&1) == 0) arcHeadPortList[arcIndex] = pp; else
				arcTailPortList[arcIndex] = pp;

			// ignore variables on port instance
			ignoreVariables();
		}

		// ignore the exports
		int numExports = readBigInteger();
		for(int j=0; j<numExports; j++)
		{
			readBigInteger();
			readBigInteger();
			ignoreVariables();
		}

		// ignore the "seen" bits (versions 8 and older)
		if (magic > ELIBConstants.MAGIC9) readBigInteger();

		// read the tool information
		int userBits = 0;
		if (magic <= ELIBConstants.MAGIC7)
		{
			// version 7 and later simply read the relevant data
			userBits = readBigInteger();
		} else
		{
			// version 6 and earlier must sift through the information
			if (toolBCount >= 1) userBits = readBigInteger();
			for(int i=1; i<toolBCount; i++) readBigInteger();
		}
		nodeInstList.userBits[nodeIndex] = userBits;

		// read variable information
		if (readVariables(ni, nodeIndex) < 0) return true;
		Input.fixVariableFont(ni);

		// node read successfully
		return false;
	}

	/**
	 * Method to read an arc instance.  returns true upon error
	 */
	private boolean readArcInst(int arcIndex)
		throws IOException
	{
		ArcInst ai = arcList[arcIndex];

		// read the arcproto pointer
		int protoIndex = readBigInteger();
		PrimitiveArc ap = convertArcProto(protoIndex);
		arcTypeList[arcIndex] = ap;

		// read the arc length (versions 5 or older)
		if (magic >= ELIBConstants.MAGIC5) readBigInteger();

		// read the arc width
		arcWidthList[arcIndex] = readBigInteger();

		// ignore the signals value (versions 6, 7, or 8)
		if (magic <= ELIBConstants.MAGIC6 && magic >= ELIBConstants.MAGIC8)
			readBigInteger();

		// read the arcinst name (versions 3 or older)
		if (magic >= ELIBConstants.MAGIC3)
		{
			String instName = readString();
			if (instName.length() > 0)
				ai.setName(instName);
		}

		// read the head information
		arcHeadXPosList[arcIndex] = readBigInteger();
		arcHeadYPosList[arcIndex] = readBigInteger();
		int nodeIndex = readBigInteger();
		if (nodeIndex >= 0 && nodeIndex < nodeCount)
			arcHeadNodeList[arcIndex] = nodeIndex;

		// read the tail information
		arcTailXPosList[arcIndex] = readBigInteger();
		arcTailYPosList[arcIndex] = readBigInteger();
		nodeIndex = readBigInteger();
		if (nodeIndex >= 0 && nodeIndex < nodeCount)
			arcTailNodeList[arcIndex] = nodeIndex;

		// ignore the geometry index (versions 4 or older)
		if (magic > ELIBConstants.MAGIC5) readBigInteger();

		// ignore the "seen" bits (versions 8 and older)
		if (magic > ELIBConstants.MAGIC9) readBigInteger();

		// read the arcinst's tool information
		int userBits = 0;
		if (magic <= ELIBConstants.MAGIC7)
		{
			// version 7 and later simply read the relevant data
			userBits = readBigInteger();

			// versions 7 and 8 ignore net number
			if (magic >= ELIBConstants.MAGIC8) readBigInteger();
		} else
		{
			// version 6 and earlier must sift through the information
			if (toolBCount >= 1) userBits = readBigInteger();
			for(int i=1; i<toolBCount; i++) readBigInteger();
		}
		arcUserBits[arcIndex] = userBits;

		// read variable information
		if (readVariables(ai, arcIndex) < 0) return true;
		Input.fixVariableFont(ai);

		// arc read successfully
		return false;
	}

	/**
	 * Method to read (and mostly ignore) a geometry module
	 */
	private void readGeom(boolean [] isNode, int [] moreup, int index)
		throws IOException
	{
		int type = readBigInteger();		// read entrytype
		if (type != 0) isNode[index] = true; else
			isNode[index] = false;
		if (isNode[index]) readBigInteger();// skip entryaddr
		readBigInteger();					// skip lowx
		readBigInteger();					// skip highx
		readBigInteger();					// skip lowy
		readBigInteger();					// skip highy
		readBigInteger();					// skip moreleft
		readBigInteger();					// skip ll
		readBigInteger();					// skip moreright
		readBigInteger();					// skip lr
		moreup[index] = readBigInteger();	// read moreup
		readBigInteger();					// skip lu
		readBigInteger();					// skip moredown
		readBigInteger();					// skip ld
		ignoreVariables();					// skip variables
	}

	// --------------------------------- VARIABLES ---------------------------------

	/**
	 * Method to read the global namespace.  returns true upon error
	 */
	private boolean readNameSpace()
		throws IOException
	{
		nameCount = readBigInteger();
		if (nameCount == 0) return false;

		// read in the namespace
        varKeys = new VarKeys(nameCount);
		for(int i=0; i<nameCount; i++) {
            varKeys.addKey(i, readString());
        }
		return false;
	}

	/**
	 * Method to read a set of variables onto a given object.
	 * @param obj the object onto which the variables will be stored.
	 * @param index the index (in global arrays) of the arc or node being read (-1 if not a NodeInst or ArcInst).
	 * These special indices must be used because node and arc names are checked for uniqueness within the
	 * cell, but the cell has not been constructed yet.  So the names must be stored in a separate place
	 * and applied later.
	 * @return the number of variables read (negative on error).
	 */
	private int readVariables(ElectricObject obj, int index)
		throws IOException
	{
		int count = readBigInteger();
		for(int i=0; i<count; i++)
		{
			short key = readSmallInteger();
			int newtype = readBigInteger();

			// version 9 and later reads text description on displayable variables
			boolean definedDescript = false;
			TextDescriptor td;
			int descript0 = 0;
			int descript1 = 0;
			if (magic <= ELIBConstants.MAGIC9)
			{
				if (alwaysTextDescriptors)
				{
					descript0 = readBigInteger();
					descript1 = readBigInteger();
					definedDescript = true;
				} else
				{
					if ((newtype&ELIBConstants.VDISPLAY) != 0)
					{
						if (convertTextDescriptors)
						{
							// conversion is done later
							descript0 = readBigInteger();
						} else
						{
							descript0 = readBigInteger();
							descript1 = readBigInteger();
						}
						definedDescript = true;
					}
				}
			}
			if (!definedDescript)
			{
//				defaulttextdescript(newDescript, NOGEOM);
			}
			Object newAddr;
			if ((newtype&ELIBConstants.VISARRAY) != 0)
			{
				int len = readBigInteger();
				int cou = len;
				if ((newtype&ELIBConstants.VLENGTH) == 0) cou++;
				Object [] newAddrArray = null;
				switch (newtype&ELIBConstants.VTYPE)
				{
					case ELIBConstants.VADDRESS:
					case ELIBConstants.VINTEGER:    newAddrArray = new Integer[cou];     break;
					case ELIBConstants.VFRACT:      
					case ELIBConstants.VFLOAT:      newAddrArray = new Float[cou];       break;
					case ELIBConstants.VDOUBLE:     newAddrArray = new Double[cou];      break;
					case ELIBConstants.VSHORT:      newAddrArray = new Short[cou];       break;
					case ELIBConstants.VBOOLEAN:
					case ELIBConstants.VCHAR:       newAddrArray = new Byte[cou];        break;
					case ELIBConstants.VSTRING:     newAddrArray = new String[cou];      break;
					case ELIBConstants.VNODEINST:   newAddrArray = new NodeInst[cou];    break;
					case ELIBConstants.VNODEPROTO:  newAddrArray = new NodeProto[cou];   break;
					case ELIBConstants.VARCPROTO:   newAddrArray = new ArcProto[cou];    break;
					case ELIBConstants.VPORTPROTO:  newAddrArray = new PortProto[cou];   break;
					case ELIBConstants.VARCINST:    newAddrArray = new ArcInst[cou];     break;
					case ELIBConstants.VTECHNOLOGY: newAddrArray = new Technology[cou];  break;
					case ELIBConstants.VLIBRARY:    newAddrArray = new Library[cou];     break;
					case ELIBConstants.VTOOL:       newAddrArray = new Tool[cou];        break;
				}
				if (newAddrArray == null)
				{
					System.out.println("Cannot figure out the type for code "+(newtype&ELIBConstants.VTYPE));
				}
				newAddr = newAddrArray;
				if ((newtype&ELIBConstants.VTYPE) == ELIBConstants.VGENERAL)
				{
					for(int j=0; j<len; j += 2)
					{
						int type = readBigInteger();
						int addr = readBigInteger();
						if (newAddrArray != null) newAddrArray[j] = null;
					}
				} else
				{
					for(int j=0; j<len; j++)
					{
						Object ret = getInVar(newtype);
						if (ret == null)
						{
							System.out.println("Error reading array variable type");
							return -1;
						}
						if (newAddrArray != null) newAddrArray[j] = ret;
					}
					
				}
			} else
			{
				newAddr = getInVar(newtype);
				if (newAddr == null)
				{
					System.out.println("Error reading variable type " + newtype);
					return -1;
				}
			}

			// copy this variable into database
			if (key < 0 || key >= nameCount)
			{
				System.out.println("Bad variable index (" + key + ", limit is " + nameCount + ") on " + obj + " object");
				return -1;
			}

			// Geometric names are saved as variables.
			if (newAddr instanceof String)
			{
				if ((obj instanceof NodeInst && varKeys.getKey(key) == NodeInst.NODE_NAME) ||
					(obj instanceof ArcInst && varKeys.getKey(key) == ArcInst.ARC_NAME))
				{
					Geometric geom = (Geometric)obj;
					TextDescriptor nameDescript = new TextDescriptor(null, descript0, descript1, 0);
					Input.fixTextDescriptorFont(nameDescript);
					geom.setNameTextDescriptor(nameDescript);
					Name name = makeGeomName(geom, newAddr, newtype);
					if (obj instanceof NodeInst)
						nodeInstList.name[index] = name;
					else
						arcNameList[index] = name;
					continue;
				}
			}

			// see if the variable is deprecated
			Variable.Key varKey = varKeys.getKey(key);
			if (obj.isDeprecatedVariable(varKey)) continue;

			// see if the variable is a "meaning option"
// 			Pref.Meaning meaning = Pref.getMeaningVariable(obj, varKey.getName());
// 			if (meaning != null)
// 			{
// 				// ignore these when not from top library
// 				if (!topLevelLibrary) continue;

// 				// accumulate this for later
// 				Pref.changedMeaningVariable(meaning);
// 			}

			// set the variable
			Variable var = obj.newVar(varKey, newAddr);
			if (var == null)
			{
				System.out.println("Error reading variable");
				return -1;
			}
			var.setTextDescriptor(new TextDescriptor(null, descript0, descript1, 0));
			var.lowLevelSetFlags(newtype);

			// handle updating of technology caches
//			if (type == VTECHNOLOGY)
//				changedtechnologyvariable(keyval);
		}
		return count;
	}

	/**
	 * Method to ignore one set of object variables on readin
	 */
	private void ignoreVariables()
		throws IOException
	{
		NodeInst ni = NodeInst.lowLevelAllocate();
		readVariables(ni, -1);
	}

	/**
	 * Method to read a set of meaning preferences onto a given object.
	 * @param obj the object onto which the meaning preferences will be stored.
	 */
	private int readMeaningPrefs(Object obj)
		throws IOException
	{
		int count = readBigInteger();
		for(int i=0; i<count; i++)
		{
			short key = readSmallInteger();
			int newtype = readBigInteger();

			// version 9 and later reads text description on displayable variables
			if (magic <= ELIBConstants.MAGIC9)
			{
				if (alwaysTextDescriptors)
				{
					readBigInteger();
					readBigInteger();
				} else
				{
					if ((newtype&ELIBConstants.VDISPLAY) != 0)
					{
						if (convertTextDescriptors)
						{
							readBigInteger();
						} else
						{
							readBigInteger();
							readBigInteger();
						}
					}
				}
			}
			if ((newtype&ELIBConstants.VISARRAY) != 0)
			{
				int len = readBigInteger();
				if ((newtype&ELIBConstants.VTYPE) == ELIBConstants.VGENERAL)
				{
					for(int j=0; j<len; j += 2)
					{
						readBigInteger();
						readBigInteger();
					}
				} else
				{
					for(int j=0; j<len; j++)
					{
						getInVar(newtype);
					}
					
				}
				continue;
			}
			Object value = getInVar(newtype);
			if (value == null)
			{
				System.out.println("Error reading variable type " + newtype);
				return -1;
			}
			// copy this variable into database
			if (key < 0 || key >= nameCount)
			{
				System.out.println("Bad variable index (" + key + ", limit is " + nameCount + ") on " + obj + " object");
				return -1;
			}

			if (!(value instanceof Integer ||
				  value instanceof Float ||
				  value instanceof Double ||
				  value instanceof String))
				continue;
			if (!topLevelLibrary) continue;

			// change "meaning option"
			String varName = varKeys.getKey(key).getName();
			Pref.Meaning meaning = Pref.getMeaningVariable(obj, varName);
			if (meaning != null)
				Pref.changedMeaningVariable(meaning, value);
			else if (obj instanceof Technology)
				((Technology)obj).convertOldVariable(varName, value);
		}
		return count;
	}

	/**
	 * Method to fix variables that make reference to external cells.
	 */
	private void fixExternalVariables(ElectricObject obj)
	{
	}

	/**
	 * Helper method to read a variable at address "addr" of type "ty".
	 * Returns zero if OK, negative on memory error, positive if there were
	 * correctable problems in the read.
	 */
	private Object getInVar(int ty)
		throws IOException
	{
		int i;

		if ((ty&(ELIBConstants.VCODE1|ELIBConstants.VCODE2)) != 0) ty = ELIBConstants.VSTRING;
		switch (ty&ELIBConstants.VTYPE)
		{
			case ELIBConstants.VADDRESS:
			case ELIBConstants.VINTEGER:
				return new Integer(readBigInteger());
			case ELIBConstants.VFRACT:
				return new Float(readBigInteger() / 120.0f);
			case ELIBConstants.VFLOAT:
				return new Float(readFloat());
			case ELIBConstants.VDOUBLE:
				return new Double(readDouble());
			case ELIBConstants.VSHORT:
				return new Short(readSmallInteger());
			case ELIBConstants.VBOOLEAN:
			case ELIBConstants.VCHAR:
				return new Byte(readByte());
			case ELIBConstants.VSTRING:
				return readString();
			case ELIBConstants.VNODEINST:
				i = readBigInteger();
				if (i < 0 || i >= nodeCount)
				{
					System.out.println("Variable of type NodeInst has index " + i + " when range is 0 to " + nodeCount);
					return null;
				}
				return nodeInstList.theNode[i];
			case ELIBConstants.VNODEPROTO:
				i = readBigInteger();
				return convertNodeProto(i);
			case ELIBConstants.VARCPROTO:
				i = readBigInteger();
				return convertArcProto(i);
			case ELIBConstants.VPORTPROTO:
				i = readBigInteger();
				return convertPortProto(i);
			case ELIBConstants.VARCINST:
				i = readBigInteger();
				if (i < 0 || i >= arcCount)
				{
					System.out.println("Variable of type ArcInst has index " + i + " when range is 0 to " + arcCount);
					return null;
				}
				return arcList[i];
			case ELIBConstants.VGEOM:
				readBigInteger();
				System.out.println("Cannot read variable of type Geometric");
				return null;
			case ELIBConstants.VTECHNOLOGY:
				i = readBigInteger();
				if (i == -1)
				{
					System.out.println("Variable of type Technology has negative index");
					return null;
				}
				return getTechList(i);
			case ELIBConstants.VPORTARCINST:
				readBigInteger();
				System.out.println("Cannot read variable of type PortArcInst");
				return null;
			case ELIBConstants.VPORTEXPINST:
				readBigInteger();
				System.out.println("Cannot read variable of type PortExpInst");
				return null;
			case ELIBConstants.VLIBRARY:
				String libName = readString();
				return Library.findLibrary(libName);
			case ELIBConstants.VTOOL:
				i = readBigInteger();
				if (i < 0 || i >= toolCount) return null;
				Tool tool = toolList[i];
				if (tool == null)
				{
					i = 0;
					if (toolError[i] != null)
					{
						System.out.println("WARNING: no tool called '" + toolError[i] + "', using 'user'");
						toolError[i] = null;
					}
				}
				return tool;
			case ELIBConstants.VRTNODE:
				readBigInteger();
				System.out.println("Cannot read variable of type RTNode");
				return null;
		}
		System.out.println("Cannot read variable of type " + (ty&ELIBConstants.VTYPE));
		return null;
	}

	// --------------------------------- OBJECT CONVERSION ---------------------------------

	/**
	 * Method to convert the nodeproto index "i" to a true nodeproto pointer.
	 */
	private NodeProto convertNodeProto(int i)
	{
		int error = 0;
		if (i < 0)
		{
			// negative values are primitives
			int nindex = -i - 2;
			if (nindex >= primNodeProtoCount)
			{
				System.out.println("Error: want primitive node index " + nindex + " when limit is " + primNodeProtoCount);
				nindex = 0;
				error = 1;
			}
			return getPrimNodeProtoList(nindex);
		}

		// see if the cell value is valid
		if (i >= nodeProtoCount)
		{
			System.out.println("Error: want cell index " + i + " when limit is " + nodeProtoCount);
			return null;
		}
		return nodeProtoList[i];
	}

	/**
	 * Method to convert the arcproto index "i" to a true arcproto pointer.
	 */
	private PrimitiveArc convertArcProto(int i)
	{
		int aindex = -i - 2;
		if (aindex >= arcProtoCount || aindex < 0)
		{
			System.out.println("Want primitive arc index " + aindex + " when range is 0 to " + arcProtoCount);
			aindex = 0;
		}
		return getArcProtoList(aindex);
	}

	/**
	 * Method to convert the PortProto index "i" to a true PortProto pointer.
	 */
	private PortProto convertPortProto(int i)
	{
		if (i < 0)
		{
			int pindex = -i - 2;
			if (pindex >= primPortProtoCount)
			{
				System.out.println("Error: want primitive port index " + pindex + " when limit is " + primPortProtoCount);
				pindex = 0;
			}
			return getPrimPortProtoList(pindex);
		}

		if (i >= portProtoCount)
		{
			System.out.println("Error: want port index " + i + " when limit is " + portProtoCount);
			i = 0;
		}
		if (portProtoList[i] instanceof Cell) return null;
		return (Export)portProtoList[i];
	}

	private NodeProto getPrimNodeProtoList(int i)
	{
		getTechList(primNodeProtoTech[i]);
		if (primNodeProtoError[i])
		{
			System.out.println("Cannot find primitive '" + primNodeProtoOrig[i] + "', using " +
				primNodeProtoList[i].getName());
			primNodeProtoError[i] = false;
		}
		return(primNodeProtoList[i]);
	}

	private PrimitiveArc getArcProtoList(int i)
	{
		if (arcProtoError[i] != null)
		{
			System.out.println("Cannot find arc '" + arcProtoError[i] + "', using " + arcProtoList[i].getName());
			arcProtoError[i] = null;
		}
		return(arcProtoList[i]);
	}

	private PortProto getPrimPortProtoList(int i)
	{
		if (primPortProtoError[i] != null)
		{
			System.out.println("WARNING: port " + primPortProtoError[i] + " not found, using " +
				primPortProtoList[i].getName());
			primPortProtoError[i] = null;
		}
		return(primPortProtoList[i]);
	}

	/**
	 * Method to return the Technology associated with index "i".
	 */
	private Technology getTechList(int i)
	{
		if (techError[i] != null)
		{
			System.out.println("WARNING: technology '" + techError[i] + "' does not exist, using '" + techList[i].getTechName() + "'");
			techError[i] = null;
		}
		return(techList[i]);
	}
	
	/**
	 * Method to return the View associated with index "i".
	 */
	private View getView(int i)
	{
		View v = (View)viewMapping.get(new Integer(i));
		return v;
	}

	// --------------------------------- LOW-LEVEL INPUT ---------------------------------

	/**
	 * Method to read a single byte from the input stream and return it.
	 */
	private byte readByte()
		throws IOException
	{
		int value = dataInputStream.read();
		if (value == -1) throw new IOException();
		updateProgressDialog(1);
		return (byte)value;
	}

	static private ByteBuffer bb = ByteBuffer.allocateDirect(8);
	static private byte [] rawData = new byte[8];

	/**
	 * Method to read an integer (4 bytes) from the input stream and return it.
	 */
	private int readBigInteger()
		throws IOException
	{
		if (sizeOfBig == 4)
		{
			updateProgressDialog(4);
			int data = dataInputStream.readInt();
			if (!bytesSwapped)
				data = ((data >> 24) & 0xFF) | ((data >> 8) & 0xFF00) | ((data & 0xFF00) << 8) | ((data & 0xFF) << 24);
			return data;
		}
		readBytes(rawData, sizeOfBig, 4, true);
		if (bytesSwapped)
		{
			bb.put(0, rawData[0]);
			bb.put(1, rawData[1]);
			bb.put(2, rawData[2]);
			bb.put(3, rawData[3]);
		} else
		{
			bb.put(0, rawData[3]);
			bb.put(1, rawData[2]);
			bb.put(2, rawData[1]);
			bb.put(3, rawData[0]);
		}
		return bb.getInt(0);
	}

	/**
	 * Method to read a float (4 bytes) from the input stream and return it.
	 */
	private float readFloat()
		throws IOException
	{
		if (bytesSwapped)
		{
			updateProgressDialog(4);
			return dataInputStream.readFloat();
		} else
		{
			readBytes(rawData, sizeOfBig, 4, true);
			bb.put(0, rawData[3]);
			bb.put(1, rawData[2]);
			bb.put(2, rawData[1]);
			bb.put(3, rawData[0]);
			return bb.getFloat(0);
		}
	}

	/**
	 * Method to read a double (8 bytes) from the input stream and return it.
	 */
	private double readDouble()
		throws IOException
	{
		if (bytesSwapped)
		{
			updateProgressDialog(8);
			return dataInputStream.readDouble();
		} else
		{
			readBytes(rawData, sizeOfBig, 8, true);
			bb.put(0, rawData[7]);
			bb.put(1, rawData[2]);
			bb.put(2, rawData[3]);
			bb.put(3, rawData[4]);
			bb.put(4, rawData[3]);
			bb.put(5, rawData[2]);
			bb.put(6, rawData[1]);
			bb.put(7, rawData[0]);
			return bb.getDouble(0);
		}
	}

	/**
	 * Method to read an short (2 bytes) from the input stream and return it.
	 */
	private short readSmallInteger()
		throws IOException
	{
		if (sizeOfSmall == 2)
		{
			updateProgressDialog(2);
			int data = dataInputStream.readShort();
			if (!bytesSwapped)
				data = ((data >> 8) & 0xFF) | ((data & 0xFF) << 8);
			return (short)data;
		}
		readBytes(rawData, sizeOfSmall, 2, true);
		if (bytesSwapped)
		{
			bb.put(0, rawData[0]);
			bb.put(1, rawData[1]);
		} else
		{
			bb.put(0, rawData[1]);
			bb.put(1, rawData[0]);
		}
		return bb.getShort(0);
	}

	/**
	 * Method to read a string from the input stream and return it.
	 */
	private String readString()
		throws IOException
	{
		if (sizeOfChar != 1)
		{
			// disk and memory don't match: read into temporary string
			System.out.println("Cannot handle library files with unicode strings");
//			tempstr = io_gettempstring();
//			if (allocstring(&name, tempstr, cluster)) return(0);
			return null;
		} else
		{
			// disk and memory match: read the data
			int len = readBigInteger();
			if (len <= 0) return "";
			byte [] stringBytes = new byte[len];
            if ((len > 150) || (len < 0)) {
                System.out.flush();
            }
            int ret = dataInputStream.read(stringBytes, 0, len);
            if (ret != len) throw new IOException();
			updateProgressDialog(len);
			String theString = new String(stringBytes);
			return theString;
		}
	}

	/**
	 * Method to read a number of bytes from the input stream and return it.
	 */
	private void readBytes(byte [] data, int diskSize, int memorySize, boolean signExtend)
		throws IOException
	{
		// check for direct transfer
		if (diskSize == memorySize)
		{
			// just peel it off the disk
			int ret = dataInputStream.read(data, 0, diskSize);
			if (ret != diskSize) throw new IOException();
		} else
		{
			// not a simple read, use a buffer
			int ret = dataInputStream.read(rawData, 0, diskSize);
			if (ret != diskSize) throw new IOException();
			if (diskSize > memorySize)
			{
				// trouble! disk has more bits than memory.  check for clipping
				for(int i=0; i<memorySize; i++) data[i] = rawData[i];
				for(int i=memorySize; i<diskSize; i++)
					if (rawData[i] != 0 && rawData[i] != 0xFF)
						clippedIntegers++;
			} else
			{
				// disk has smaller integer
				if (!signExtend || (rawData[diskSize-1] & 0x80) == 0)
				{
					for(int i=diskSize; i<memorySize; i++) rawData[i] = 0;
				} else
				{
					for(int i=diskSize; i<memorySize; i++) rawData[i] = (byte)0xFF;
				}
				for(int i=0; i<memorySize; i++) data[i] = rawData[i];
			}
		}
		updateProgressDialog(diskSize);
	}
}
