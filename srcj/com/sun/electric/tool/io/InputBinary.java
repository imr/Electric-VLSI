/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: InputBinary.java
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
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.BinaryConstants;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.dialogs.Progress;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Date;


/**
 * This class reads files in binary (.elib) format.
 */
public class InputBinary extends Input
{
	// ------------------------- private data ----------------------------
	/** the magic number in the library file */								private int magic;
	/** the Electric version in the library file */							private Version version;

	// characteristics of the file
	/** true if bytes are swapped on disk */								private boolean bytesSwapped;
	/** the size of a "big" integer on disk (4 or more bytes) */			private int sizeOfBig;
	/** the size of a "small" integer on disk (2 or more bytes) */			private int sizeOfSmall;
	/** the size of a character on disk (1 or 2 bytes) */					private int sizeOfChar;

	// statistics about the file
	/** the number of integers on disk that got clipped during input */		private int clippedIntegers;
	/** the number of bytes of data read so far */							private int byteCount;

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
	/** true if old MOSIS CMOS technologies appear in the library */		private boolean convertMosisCmosTechnologies;
	/** the most popular layout technology */								private Technology layoutTech;

	// the cell information
	/** the number of NodeProtos in the file */								private int nodeProtoCount;
	/** the number of Cells in the file */									private int cellCount;
	/** the index of the current Cell */									private int curCell;
	/** list of all Cells in the library */									private Cell [] nodeProtoList;
	/** list of all former cells in the library */							private FakeCell [] fakeCellList;
	/** list of number of NodeInsts in each Cell of the library */			private int [] nodeCounts;
	/** index of first NodeInst in each cell of the library */				private int [] firstNodeIndex;
	/** list of number of ArcInsts in each Cell of the library */			private int [] arcCounts;
	/** index of first ArcInst in each cell of the library */				private int [] firstArcIndex;
	/** list of all Exports in each Cell of the library */					private int [] portCounts;
	/** index of first Export in each cell of the library */				private int [] firstPortIndex;

	// the NodeInsts in the library
	/** the number of NodeInsts in the library */							private int nodeCount;
	/** list of all NodeInsts in the library */								private NodeInst [] nodeList;
	/** list of Prototypes of NodeInsts in the library */					private NodeProto [] nodeTypeList;
	/** list of Names of NodeInsts in the library */						private Name [] nodeNameList;
	/** list of Low X values for NodeInsts in the library */				private int [] nodeLowXList;
	/** list of High X values for NodeInsts in the library */				private int [] nodeHighXList;
	/** list of Low Y values for NodeInsts in the library */				private int [] nodeLowYList;
	/** list of High Y values for NodeInsts in the library */				private int [] nodeHighYList;
	/** list of rotation values for NodeInsts in the library */				private short [] nodeRotationList;
	/** list of transpose values for NodeInsts in the library */			private int [] nodeTransposeList;

	// the ArcInsts in the library
	/** the number of ArcInsts in the library */							private int arcCount;
	/** list of all ArcInsts in the library */								private ArcInst [] arcList;
	/** list of the prototype of the ArcInsts in the library */				private ArcProto [] arcTypeList;
	/** list of the Names of the ArcInsts in the library */					private Name [] arcNameList;
	/** list of the width of the ArcInsts in the library */					private int [] arcWidthList;
	/** list of the head X of the ArcInsts in the library */				private int [] arcHeadXPosList;
	/** list of the head Y of the ArcInsts in the library */				private int [] arcHeadYPosList;
	/** list of the head node of the ArcInsts in the library */				private NodeInst [] arcHeadNodeList;
	/** list of the head port of the ArcInsts in the library */				private PortProto [] arcHeadPortList;
	/** list of the tail X of the ArcInsts in the library */				private int [] arcTailXPosList;
	/** list of the tail Y of the ArcInsts in the library */				private int [] arcTailYPosList;
	/** list of the tail node of the ArcInsts in the library */				private NodeInst [] arcTailNodeList;
	/** list of the tail port of the ArcInsts in the library */				private PortProto [] arcTailPortList;

	// the Exports in the library
	/** the number of Exports in the library */								private int portProtoCount;
	/** counter for Exports in the library */								private int portProtoIndex;
	/** list of all Exports in the library */								private Export [] portProtoList;
	/** list of NodeInsts that are origins of Exports in the library */		private NodeInst [] portProtoSubNodeList;
	/** list of PortProtos that are origins of Exports in the library */	private Object [] portProtoSubPortList;
	/** list of Export names in the library */								private String [] portProtoNameList;
	/** list of Export userbits in the library */							private int [] portProtoUserbits;

	// the geometric information (only used for old format files)
	/** the number of Geometrics in the file */								private int geomCount;
	/** list of all Geometric types in the library */						private boolean [] geomType;
	/** list of all Geometric up-pointers in the library */					private int [] geomMoreUp;

	// the variable information
	/** number of variable names in the library */							private int nameCount;
	/** list of variable keys in the library */								private Variable.Key [] realKey;
	/** true to convert all text descriptor values */						private boolean convertTextDescriptors;
	/** true to require text descriptor values */							private boolean alwaysTextDescriptors;

	/** cell flag for recursive setup */									private FlagSet recursiveSetupFlag;

	// ".elib" file version numbers
	/** current magic number: version 12 */		public static final int MAGIC12 = -1595;
	/** older magic number: version 11 */		private static final int MAGIC11 = -1593;
	/** older magic number: version 10 */		private static final int MAGIC10 = -1591;
	/** older magic number: version 9 */		private static final int MAGIC9 =  -1589;
	/** older magic number: version 8 */		private static final int MAGIC8 =  -1587;
	/** older magic number: version 7 */		private static final int MAGIC7 =  -1585;
	/** older magic number: version 6 */		private static final int MAGIC6 =  -1583;
	/** older magic number: version 5 */		private static final int MAGIC5 =  -1581;
	/** older magic number: version 4 */		private static final int MAGIC4 =  -1579;
	/** older magic number: version 3 */		private static final int MAGIC3 =  -1577;
	/** older magic number: version 2 */		private static final int MAGIC2 =  -1575;
	/** oldest magic number: version 1 */		private static final int MAGIC1 =  -1573;

	InputBinary() {}

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
		if (readHeader()) return true;

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
		if (magic <= MAGIC9 && magic >= MAGIC11)
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
		if (magic <= MAGIC8) versionString = readString(); else
			versionString = "3.35";
		version = Version.parseVersion(versionString);

		// for versions before 6.03q, convert MOSIS CMOS technology names
		convertMosisCmosTechnologies = false;
		if (version.getMajor() < 6 ||
			(version.getMajor() == 6 && version.getMinor() < 3) ||
			(version.getMajor() == 6 && version.getMinor() == 3 && version.getDetail() < 17))
		{
//			if ((asktech(mocmossub_tech, x_("get-state"))&MOCMOSSUBNOCONV) == 0)
				convertMosisCmosTechnologies = true;
		}

		// for versions before 6.04c, convert text descriptor values
		convertTextDescriptors = false;
		if (version.getMajor() < 6 ||
			(version.getMajor() == 6 && version.getMinor() < 4) ||
			(version.getMajor() == 6 && version.getMinor() == 4 && version.getDetail() < 3))
		{
			convertTextDescriptors = true;
		}

		// for versions 6.05x and later, always have text descriptor values
		alwaysTextDescriptors = true;
		if (version.getMajor() < 6 ||
			(version.getMajor() == 6 && version.getMinor() < 5) ||
			(version.getMajor() == 6 && version.getMinor() == 5 && version.getDetail() < 24))
		{
			alwaysTextDescriptors = false;
		}

		// get the newly created views (version 9 and later)
		for (Iterator it = View.getViews(); it.hasNext();)
		{
			View view = (View) it.next();
			view.setTempInt(0);
		}
		View.UNKNOWN.setTempInt(-1);
		View.LAYOUT.setTempInt(-2);
		View.SCHEMATIC.setTempInt(-3);
		View.ICON.setTempInt(-4);
		View.DOCWAVE.setTempInt(-5);
		View.LAYOUTSKEL.setTempInt(-6);
		View.VHDL.setTempInt(-7);
		View.NETLIST.setTempInt(-8);
		View.DOC.setTempInt(-9);
		View.NETLISTNETLISP.setTempInt(-10);
		View.NETLISTALS.setTempInt(-11);
		View.NETLISTQUISC.setTempInt(-12);
		View.NETLISTRSIM.setTempInt(-13);
		View.NETLISTSILOS.setTempInt(-14);
		View.VERILOG.setTempInt(-15);
		View.LAYOUTCOMP.setTempInt(-16);
		if (magic <= MAGIC9)
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
					if (version.getMajor() < 7 ||
						(version.getMajor() == 7 && version.getMinor() < 1))
					{
						if (viewName.equals("compensated")) view = View.LAYOUTCOMP; else
						if (viewName.equals("skeleton")) view = View.LAYOUTSKEL; else
						if (viewName.equals("simulation-snapshot")) view = View.DOCWAVE; else
						if (viewName.equals("netlist-netlisp-format")) view = View.NETLISTNETLISP; else
						if (viewName.equals("netlist-rsim-format")) view = View.NETLISTRSIM; else
						if (viewName.equals("netlist-silos-format")) view = View.NETLISTSILOS; else
						if (viewName.equals("netlist-quisc-format")) view = View.NETLISTQUISC; else
						if (viewName.equals("netlist-als-format")) view = View.NETLISTALS;
					}
					if (view == null)
					{
						view = View.newInstance(viewName, viewShortName);
						if (view == null) return true;
					}
				}
				view.setTempInt(i + 1);
			}
		}

		// get the number of toolbits to ignore
		if (magic <= MAGIC3 && magic >= MAGIC6)
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
		techScale = new double[techCount];
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

		// allocate pointers for the NodeInsts
		nodeList = new NodeInst[nodeCount];
		nodeTypeList = new NodeProto[nodeCount];
		nodeNameList = new Name[nodeCount];
		nodeLowXList = new int[nodeCount];
		nodeHighXList = new int[nodeCount];
		nodeLowYList = new int[nodeCount];
		nodeHighYList = new int[nodeCount];
		nodeRotationList = new short[nodeCount];
		nodeTransposeList = new int[nodeCount];


		// allocate pointers for the ArcInsts
		arcList = new ArcInst[arcCount];
		arcTypeList = new ArcProto[arcCount];
		arcNameList = new Name[arcCount];
		arcWidthList = new int[arcCount];
		arcHeadXPosList = new int[arcCount];
		arcHeadYPosList = new int[arcCount];
		arcHeadNodeList = new NodeInst[arcCount];
		arcHeadPortList = new PortProto[arcCount];
		arcTailXPosList = new int[arcCount];
		arcTailYPosList = new int[arcCount];
		arcTailNodeList = new NodeInst[arcCount];
		arcTailPortList = new PortProto[arcCount];
		for(int i = 0; i < arcCount; i++)
		{
			arcHeadNodeList[i] = null;
			arcHeadPortList[i] = null;
			arcTailNodeList[i] = null;
			arcTailPortList[i] = null;
			arcNameList[i] = null;
		}

		// allocate pointers for the Exports
		portProtoList = new Export[portProtoCount];
		portProtoSubNodeList = new NodeInst[portProtoCount];
		portProtoSubPortList = new Object[portProtoCount];
		portProtoNameList = new String[portProtoCount];
		portProtoUserbits = new int[portProtoCount];

		// versions 9 to 11 allocate fake-cell pointers
		if (magic <= MAGIC9 && magic >= MAGIC11)
		{
			fakeCellList = new FakeCell[cellCount];
			for(int i=0; i<cellCount; i++)
				fakeCellList[i] = new FakeCell();
		}

		// versions 4 and earlier allocate geometric pointers
		if (magic > MAGIC5)
		{
			geomType = new boolean [geomCount];
			geomMoreUp = new int [geomCount];
		}

		// get number of arcinsts and nodeinsts in each cell
		if (magic != MAGIC1)
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
			} else
			{
				nodeProtoList[i] = Cell.lowLevelAllocate(lib);
				if (nodeProtoList[i] == null) return true;
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
				nodeList[thisone] = NodeInst.lowLevelAllocate();
				if (nodeList[thisone] == null) return true;
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
			Technology tech = null;
			if (convertMosisCmosTechnologies)
			{
				if (name.equals("mocmossub")) tech = Technology.findTechnology("mocmos"); else
					if (name.equals("mocmos")) tech = Technology.findTechnology("mocmosold");
			}
			if (tech == null) tech = Technology.findTechnology(name);
			boolean imosconv = false;
			if (tech == null && name.equals("imos"))
			{
				tech = Technology.findTechnology("mocmos");
				if (tech != null) imosconv = true;
			}
			if (tech == null && name.equals("logic"))
				tech = Technology.findTechnology("schematic");
			if (tech == null && (name.equals("epic8c") || name.equals("epic7c")))
				tech = Technology.findTechnology("epic7s");
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
						String primName = opnp.getProtoName();
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
							String primName = opnp.getProtoName();
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
						pp = (PrimitivePort) it.next();
						if (!primNodeProtoError[primNodeProtoCount])
						{
							String errorMessage = name + " on ";
							if (primNodeProtoOrig[primNodeProtoCount] != null)
								errorMessage += primNodeProtoOrig[primNodeProtoCount]; else
							{
								if (techError[techIndex] != null)
									errorMessage += techError[techIndex]; else
										errorMessage += tech.getTechName();
								errorMessage += ":" + pnp.getProtoName();
							}
							primPortProtoError[primPortProtoCount] = errorMessage;
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
		if (magic <= MAGIC3 && magic >= MAGIC6)
		{
			// versions 3, 4, 5, and 6 must ignore toolbits associations
			for(int i=0; i<toolBCount; i++) readString();
		}

		// get the library userbits
		int userBits = 0;
		if (magic <= MAGIC7)
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

		// get the lambda values in the library
		for(int i=0; i<techCount; i++)
		{
			int lambda = readBigInteger();
			if (techError[i] != null) continue;
			Technology tech = techList[i];

			// for Electric version 4 or earlier, scale lambda by 20
			if (version.getMajor() <= 4) lambda *= 20;

			int index = tech.getIndex();
			techScale[index] = lambda;
		}

		// read the global namespace
		if (readNameSpace()) return true;

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
				if (readVariables(tool, -1) < 0) return true;
				Input.fixVariableFont(tool);
			}
		}

		// read the technology variables
		for(int i=0; i<techCount; i++)
		{
			Technology tech = techList[i];
			if (tech == null) ignoreVariables(); else
			{
				int j = readVariables(tech, -1);
				if (j < 0) return true;
				if (j > 0) getTechList(i);
				Input.fixVariableFont(tech);
			}
		}

		// read the arcproto variables
		for(int i=0; i<arcProtoCount; i++)
		{
			PrimitiveArc ap = arcProtoList[i];
			int j = readVariables(ap, -1);
			if (j < 0) return true;
			if (j > 0) getArcProtoList(i);
			Input.fixVariableFont(ap);
		}

		// read the primitive nodeproto variables
		for(int i=0; i<primNodeProtoCount; i++)
		{
			NodeProto np = primNodeProtoList[i];
			int j = readVariables(np, -1);
			if (j < 0) return true;
			if (j > 0) getPrimNodeProtoList(i);
			Input.fixVariableFont(np);
		}

		// read the primitive portproto variables
		for(int i=0; i<primPortProtoCount; i++)
		{
			PortProto pp = primPortProtoList[i];
			int j = readVariables(pp, -1);
			if (j < 0) return true;
			if (j > 0) getPrimPortProtoList(i);
		}

		// read the view variables (version 9 and later)
		if (magic <= MAGIC9)
		{
			int count = readBigInteger();
			for(int i=0; i<count; i++)
			{
				int j = readBigInteger();
				View v = getView(j);
				if (v != null)
				{
					if (readVariables(v, -1) < 0) return true;
					Input.fixVariableFont(v);
				} else
				{
					System.out.println("View index " + j + " not found");
					ignoreVariables();
				}
			}
		}

		// setup fake cell structures (version 9 to 11)
		if (magic <= MAGIC9 && magic >= MAGIC11)
		{
			for(int i=0; i<cellCount; i++)
			{
				String thecellname = readString();
				ignoreVariables();

				fakeCellList[i].cellName = thecellname;
			}
		}

		// read the cells
		portProtoIndex = 0;
		for(int i=0; i<nodeProtoCount; i++)
		{
			Cell cell = nodeProtoList[i];
			if (cell == null) continue;
			if (readNodeProto(cell, i)) return true;
		}

		// initialize for processing cell groups
		FlagSet cellFlag = NodeProto.getFlagSet(1);
		for(int cellIndex=0; cellIndex<nodeProtoCount; cellIndex++)
		{
			Cell cell = nodeProtoList[cellIndex];
			if (cell == null) continue;
			cell.clearBit(cellFlag);
		}

		// process the cell groups
		for(int cellIndex=0; cellIndex<nodeProtoCount; cellIndex++)
		{
			if (magic <= MAGIC9 && magic >= MAGIC11) continue;
			Cell cell = nodeProtoList[cellIndex];
			if (cell == null) continue;
			if (cell.isBit(cellFlag)) continue;

			// follow cell group links, give them a cell group
			Cell.CellGroup cg = new Cell.CellGroup();
			for(;;)
			{
				cell.setBit(cellFlag);
				cell.setCellGroup(cg);

				// move to the next in the group
				Object other = cell.getTempObj();
				if (other == null) break;
				if (!(other instanceof Cell)) break;
				Cell otherCell = (Cell)other;
				if (otherCell == null) break;
				if (otherCell.isBit(cellFlag)) break;
				cell = otherCell;
			}
		}
		cellFlag.freeFlagSet();

		// cleanup cellgroup processing, link the cells
		for(int cellIndex=0; cellIndex<nodeProtoCount; cellIndex++)
		{
			Cell cell = nodeProtoList[cellIndex];
			if (cell == null) continue;
			cell.setTempObj(null);
			cell.lowLevelLink();
		}

		// now read external cells
		for(int i=0; i<nodeProtoCount; i++)
		{
			Cell cell = nodeProtoList[i];
			if (cell != null) continue;
			if (readExternalNodeProto(lib, i)) return true;
		}

		// now that external cells are resolved, fix all variables that may have used them
		fixExternalVariables(lib);
		for(int i=0; i<toolCount; i++)
		{
			Tool tool = toolList[i];
			fixExternalVariables(tool);
		}
		for(int i=0; i<techCount; i++)
		{
			Technology tech = techList[i];
			fixExternalVariables(tech);
		}
		for(int i=0; i<arcProtoCount; i++)
		{
			PrimitiveArc ap = arcProtoList[i];
			fixExternalVariables(ap);
		}
		for(int i=0; i<primNodeProtoCount; i++)
		{
			PrimitiveNode np = primNodeProtoList[i];
			fixExternalVariables(np);
		}
		for(int i=0; i<primPortProtoCount; i++)
		{
			PrimitivePort pp = primPortProtoList[i];
			fixExternalVariables(pp);
		}
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View view = (View) it.next();
			fixExternalVariables(view);
		}
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
			if (magic > MAGIC5)
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
					if (readArcInst(arcIndex)) return true;
					arcIndex++;
				} else
				{
					if (readNodeInst(nodeIndex, cellIndex)) return true;
					nodeIndex++;
				}
			} else
			{
				// version 5 and later find the arcs and nodes in linear order
				for(int j=0; j<arcCounts[cellIndex]; j++)
				{
					if (readArcInst(arcIndex)) return true;
					arcIndex++;
				}
				for(int j=0; j<nodeCounts[cellIndex]; j++)
				{
					if (readNodeInst(nodeIndex, cellIndex)) return true;
					nodeIndex++;
				}
			}
		}
		firstNodeIndex[nodeProtoCount] = nodeIndex;
		firstArcIndex[nodeProtoCount] = arcIndex;

		// clear flag bits for scanning the library hierarchically
		recursiveSetupFlag = NodeProto.getFlagSet(1);
		for(int cellIndex=0; cellIndex<nodeProtoCount; cellIndex++)
		{
			Cell cell = nodeProtoList[cellIndex];
			cell.clearBit(recursiveSetupFlag);
			cell.setTempInt(cellIndex);
		}

		// scan library hierarchically and complete the setup
		for(int cellIndex=0; cellIndex<nodeProtoCount; cellIndex++)
		{
			Cell cell = nodeProtoList[cellIndex];
			if (cell.isBit(recursiveSetupFlag)) continue;
			completeCellSetupRecursively(cell, cellIndex);
		}
		recursiveSetupFlag.freeFlagSet();
		if (curCell >= 0)
		{
			NodeProto currentCell = convertNodeProto(curCell);
			lib.setCurCell((Cell)currentCell);
		}

		// library read successfully
		return false;
	}

	/**
	 * Method to recursively create the contents of each cell in the library.
	 */
	private void completeCellSetupRecursively(Cell cell, int cellIndex)
	{
		// scan the nodes in this cell and recurse
		int startNode = firstNodeIndex[cellIndex];
		int endNode = firstNodeIndex[cellIndex+1];
		for(int i=startNode; i<endNode; i++)
		{
			// convert to new style
			NodeInst ni = nodeList[i];
			NodeProto np = nodeTypeList[i];
			if (np instanceof PrimitiveNode) continue;
			Cell otherCell = (Cell)np;

			// ignore cross-reference instances
			if (otherCell.getLibrary() != cell.getLibrary()) continue;

			// subcell: make sure that cell is setup
			if (otherCell.isBit(recursiveSetupFlag)) continue;

			// setup the subcell recursively
			completeCellSetupRecursively(otherCell,  otherCell.getTempInt());
		}
		cell.setBit(recursiveSetupFlag);

		// determine the lambda value for this cell
		double lambda = 1.0;
		int startArc = firstArcIndex[cellIndex];
		int endArc = firstArcIndex[cellIndex+1];
		Technology cellTech = Technology.whatTechnology(cell, nodeTypeList, startNode, endNode, arcTypeList, startArc, endArc);
		if (cellTech != null) lambda = techScale[cellTech.getIndex()];

		// finish initializing the NodeInsts in the cell: start with the cell-center
		int xoff = 0, yoff = 0;
		for(int i=startNode; i<endNode; i++)
		{
			if (nodeTypeList[i] == Generic.tech.cellCenterNode)
			{
				buildNodeInst(i, xoff, yoff, lambda, cell);
				xoff = (nodeLowXList[i] + nodeHighXList[i]) / 2;
				yoff = (nodeLowYList[i] + nodeHighYList[i]) / 2;
				break;
			}
		}

		// finish creating the rest of the NodeInsts
		for(int i=startNode; i<endNode; i++)
		{
			if (nodeTypeList[i] != Generic.tech.cellCenterNode)
				buildNodeInst(i, xoff, yoff, lambda, cell);
		}

		// finish initializing the Exports in the cell
		int startPort = firstPortIndex[cellIndex];
		int endPort = startPort + portCounts[cellIndex];
		for(int i=startPort; i<endPort; i++)
		{
			Export pp = portProtoList[i];
			NodeInst subNodeInst = portProtoSubNodeList[i];
			if (portProtoSubPortList[i] instanceof Integer)
			{
				// this was an external reference that couldn't be resolved yet.  Do it now
				int index = ((Integer)portProtoSubPortList[i]).intValue();
				portProtoSubPortList[i] = convertPortProto(index);
			}
			PortProto subPortProto = (PortProto)portProtoSubPortList[i];

			// null entries happen when there are external cell references
			if (subNodeInst == null || subPortProto == null) continue;

			// convert portproto to portinst
			String exportName = portProtoNameList[i];
			PortInst pi = subNodeInst.findPortInst(subPortProto.getProtoName());
			if (pp.lowLevelPopulate(pi)) return;
			if (pp.lowLevelLink()) return;
			pp.lowLevelSetUserbits(portProtoUserbits[i]);
		}

		// finish initializing the ArcInsts in the cell
		boolean arcInfoError = false;
		for(int i=startArc; i<endArc; i++)
		{
			ArcInst ai = arcList[i];
			ArcProto ap = arcTypeList[i];
			Name name = arcNameList[i];
			double width = arcWidthList[i] / lambda;
			double headX = (arcHeadXPosList[i] - xoff) / lambda;
			double headY = (arcHeadYPosList[i] - yoff) / lambda;
			double tailX = (arcTailXPosList[i] - xoff) / lambda;
			double tailY = (arcTailYPosList[i] - yoff) / lambda;
			NodeInst headNode = arcHeadNodeList[i];
			PortProto headPort = arcHeadPortList[i];
			NodeInst tailNode = arcTailNodeList[i];
			PortProto tailPort = arcTailPortList[i];
			if (headNode == null || headPort == null || tailNode == null || tailPort == null)
			{
				if (!arcInfoError)
				{
					System.out.println("Missing arc information in cell " + cell.noLibDescribe() +
						" ...  Database may be corrupt");
					if (headNode == null) System.out.println("   Head node not found");
					if (headPort == null) System.out.println("   Head port not found");
					if (tailNode == null) System.out.println("   Tail node not found");
					if (tailPort == null) System.out.println("   Tail port not found");
					arcInfoError = true;
				}
				continue;
			}
			PortInst headPortInst = headNode.findPortInst(headPort.getProtoName());
			PortInst tailPortInst = tailNode.findPortInst(tailPort.getProtoName());
			if (headPortInst == null || tailPortInst == null)
			{
				System.out.println("Cannot create arc of type " + ap.getProtoName() + " in cell " + cell.getProtoName() +
					" because ends are unknown");
				continue;
			}
			ai.lowLevelPopulate(ap, width, tailPortInst, new Point2D.Double(tailX, tailY), headPortInst, new Point2D.Double(headX, headY));
			if (name != null) ai.setNameKey(name);
			ai.lowLevelLink();
		}

		// convert "ATTRP_" variables on NodeInsts to be on PortInsts
		for(int i=startNode; i<endNode; i++)
		{
			NodeInst ni = nodeList[i];
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
//System.out.println("Setting variable '"+varName+"' on port "+thePortName);
								Variable var = pi.newVar(varName, origVar.getObject());
								if (var != null)
								{
									if (origVar.isDisplay()) var.setDisplay();
									if (origVar.isJava()) var.setJava();
									var.setDescriptor(origVar.getTextDescriptor());									
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
	 * Method to build a NodeInst.
	 */
	private void buildNodeInst(int i, int xoff, int yoff, double lambda, Cell cell)
	{
		NodeInst ni = nodeList[i];
		NodeProto np = nodeTypeList[i];
		Name name = nodeNameList[i];
		double lowX = nodeLowXList[i]-xoff;
		double lowY = nodeLowYList[i]-yoff;
		double highX = nodeHighXList[i]-xoff;
		double highY = nodeHighYList[i]-yoff;
		Point2D center = new Point2D.Double(((lowX + highX) / 2) / lambda, ((lowY + highY) / 2) / lambda);
		double width = (highX - lowX) / lambda;
		double height = (highY - lowY) / lambda;

		int rotation = nodeRotationList[i];
		if (version.getMajor() > 7 || (version.getMajor() == 7 && version.getMinor() >= 1))
		{
			// new version: allow mirror bits
			if ((nodeTransposeList[i]&1) != 0)
			{
				height = -height;
				rotation = (rotation + 900) % 3600;
			}
			if ((nodeTransposeList[i]&2) != 0)
			{
				// mirror in X
				width = -width;
			}
			if ((nodeTransposeList[i]&4) != 0)
			{
				// mirror in Y
				height = -height;
			}
		} else
		{
			// old version: just use transpose information
			if (nodeTransposeList[i] != 0)
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
		if (magic != MAGIC1 && magic != MAGIC2 && magic != MAGIC3 && magic != MAGIC4 &&
			magic != MAGIC5 && magic != MAGIC6 && magic != MAGIC7 && magic != MAGIC8 &&
			magic != MAGIC9 && magic != MAGIC10 && magic != MAGIC11 && magic != MAGIC12)
		{
			magic = ((byte1&0xFF) << 24) | ((byte2&0xFF) << 16) | ((byte3&0xFF) << 8) | (byte4&0xFF);
			if (magic != MAGIC1 && magic != MAGIC2 && magic != MAGIC3 && magic != MAGIC4 &&
				magic != MAGIC5 && magic != MAGIC6 && magic != MAGIC7 && magic != MAGIC8 &&
				magic != MAGIC9 && magic != MAGIC10 && magic != MAGIC11 && magic != MAGIC12)
			{
				System.out.println("Bad file format: does not start with proper magic number");
				return true;
			}
			bytesSwapped = true;
		}
		
		// determine the size of "big" and "small" integers as well as characters on disk
		if (magic <= MAGIC10)
		{
			sizeOfSmall = readByte();
			sizeOfBig = readByte();
		} else
		{
			sizeOfSmall = 2;
			sizeOfBig = 4;
		}
		if (magic <= MAGIC11)
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
	private boolean readNodeProto(Cell cell, int cellIndex)
		throws IOException
	{
		// read the cell name
		String theProtoName;
		if (magic <= MAGIC9)
		{
			// read the cell information (version 9 and later)
			if (magic >= MAGIC11)
			{
				// only versions 9 to 11
				int k = readBigInteger();
				theProtoName = fakeCellList[k].cellName;
			} else
			{
				// version 12 or later
				theProtoName = readString();
				int k = readBigInteger();
				cell.setTempObj(nodeProtoList[k]);		// the "next in cell group" circular pointer
				k = readBigInteger();
//				cell->nextcont = nodeProtoList[k];		// the "next in cell continuation" circular pointer
			}
			View v = getView(readBigInteger());
			if (v == null) v = View.UNKNOWN;
			int version = readBigInteger();
			theProtoName += ";" + version + "{" + v.getAbbreviation() + "}";
			int creationDate = readBigInteger();
			int revisionDate = readBigInteger();
			cell.lowLevelSetCreationDate(BinaryConstants.secondsToDate(creationDate));
			cell.lowLevelSetRevisionDate(BinaryConstants.secondsToDate(revisionDate));
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
		if (magic >= MAGIC5)
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
			Export pp = portProtoList[portProtoIndex];

			// read the sub-NodeInst for this Export
			portProtoSubNodeList[portProtoIndex] = null;
			int whichNode = readBigInteger();
			if (whichNode >= 0 && whichNode < nodeCount)
				portProtoSubNodeList[portProtoIndex] = nodeList[whichNode];

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

			if (portProtoSubNodeList[portProtoIndex] == null)
			{
				System.out.println("Error: Export '" + exportName + "' of cell " + theProtoName +
					" cannot be read properly");
			}

			// read the portproto text descriptor
			int descript0 = 0, descript1 = 0;
			if (magic <= MAGIC9)
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
			TextDescriptor descript = new TextDescriptor(null, descript0, descript1);
			Input.fixTextDescriptorFont(descript);
			pp.setTextDescriptor(descript);

			// ignore the "seen" bits (versions 8 and older)
			if (magic > MAGIC9) readBigInteger();

			// read the portproto's "user bits"
			portProtoUserbits[portProtoIndex] = 0;
			if (magic <= MAGIC7)
			{
				// version 7 and later simply read the relevant data
				portProtoUserbits[portProtoIndex] = readBigInteger();

				// versions 7 and 8 ignore net number
				if (magic >= MAGIC8) readBigInteger();
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
		if (magic > MAGIC5)
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
		if (magic <= MAGIC7)
		{
			// version 7 and later simply read the relevant data
			userBits = readBigInteger();

			// versions 7 and 8 ignore net number
			if (magic >= MAGIC8) readBigInteger();
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
		if (magic >= MAGIC11)
		{
			// only versions 9 to 11
			int k = readBigInteger();
			theProtoName = fakeCellList[k].cellName;
		} else
		{
			// version 12 or later
			theProtoName = readString();
			int k = readBigInteger();
//			cell->nextcellgrp = nodeProtoList[k];
			k = readBigInteger();
//			cell->nextcont = nodeProtoList[k];
		}
		View v = getView(readBigInteger());
		if (v == null) v = View.UNKNOWN;
		int version = readBigInteger();
		String fullCellName = theProtoName + ";" + version + "{" + v.getAbbreviation() + "}";
		Date creationDate = BinaryConstants.secondsToDate(readBigInteger());
		Date revisionDate = BinaryConstants.secondsToDate(readBigInteger());

		// read the nodeproto bounding box
		int lowX = readBigInteger();
		int highX = readBigInteger();
		int lowY = readBigInteger();
		int highY = readBigInteger();

		// get the path to the library file
		File libFile = new File(readString());

		// see if this library is already read in
		String libFileName = libFile.getName();
		String libFilePath = libFile.getParent();
		
		// special case if the library path came from a different computer system and still has separators
		int backSlashPos = libFileName.lastIndexOf('\\');
		int colonPos = libFileName.lastIndexOf(':');
		int slashPos = libFileName.lastIndexOf('/');
		int charPos = Math.max(backSlashPos, Math.max(colonPos, slashPos));
		if (charPos >= 0)
		{
			libFileName = libFileName.substring(charPos+1);
			libFilePath = "";
		}
		ImportType importType = ImportType.BINARY;
		String libName = libFileName;
		if (libName.endsWith(".elib"))
		{
			libName = libName.substring(0, libName.length()-5);
		} else if (libName.endsWith(".txt"))
		{
			libName = libName.substring(0, libName.length()-4);
			importType = ImportType.TEXT;
		}
		Library elib = Library.findLibrary(libName);
		if (elib == null)
		{
			// library does not exist: see if file is in the same directory as the main file
			String externalFile = mainLibDirectory + libFileName;
			File testFile = new File(externalFile);
			if (!testFile.exists())
			{
                // try secondary library file locations
                for (Iterator libIt = InputLibDirs.getLibDirs(); libIt.hasNext(); )
                {
                    externalFile = (String)libIt.next() + File.separator + libFileName;
                    testFile = new File(externalFile);
                    if (testFile.exists()) break;
                }
                if (!testFile.exists())
                {
                    // try the exact path specified in the reference
                    externalFile = libFile.getPath();
                    testFile = libFile;
                    if (!testFile.exists())
                    {
                        // try the Electric library area
                        externalFile = LibFile.getLibFile(libFileName);
                    }
                }
			}
			if (externalFile == null)
			{
				System.out.println("CANNOT FIND referenced library " + libFile.getPath());
				String description = "Reference library '" + libFileName + "'";
				String pt = OpenFile.chooseInputFile(OpenFile.ELIB, description);
				if (pt != null) externalFile = pt;
			}
			if (externalFile != null)
			{
				System.out.println("Reading referenced library " + externalFile);
				elib = Library.newInstance(libName, externalFile);
			} else
			{
				System.out.println("CANNOT FIND referenced library " + libFile.getPath());
				elib = null;
			}
			if (elib == null) return true;

			// read the external library
			String oldNote = progress.getNote();
			if (progress != null)
			{
				progress.setProgress(0);
				progress.setNote("Reading referenced library " + libName + "...");
			}

			elib = readALibrary(externalFile, elib, importType);
//			if (failed) elib->userbits |= UNWANTEDLIB; else
//			{
//				// queue this library for announcement through change control
//				io_queuereadlibraryannouncement(elib);
//			}
			progress.setProgress((int)(byteCount * 100 / fileLength));
			progress.setNote(oldNote);
		}

		// read the portproto names on this nodeproto
		int portCount = readBigInteger();
		String [] localPortNames = new String[portCount];
		for(int j=0; j<portCount; j++)
			localPortNames[j] = readString();

		// find this cell in the external library
		Cell c = elib.findNodeProto(fullCellName);
		if (c == null)
		{
			// cell not found in library: issue warning
			System.out.println("Cannot find cell " + fullCellName + " in library " + elib.getLibName());
		}

		// if cell found, check that size is unchanged
		if (c != null)
		{
			Rectangle2D bounds = c.getBounds();
			double lambda = 1;
			Technology cellTech = Technology.whatTechnology(c);
			if (cellTech != null) lambda = techScale[cellTech.getIndex()];
			double cellWidth = (highX - lowX) / lambda;
			double cellHeight = (highY - lowY) / lambda;
			if (!EMath.doublesEqual(bounds.getWidth(), cellWidth) || !EMath.doublesEqual(bounds.getHeight(), cellHeight))
			{
				// bounds differ, but lambda scaling is inaccurate: see if aspect ratio changed
				if (!EMath.doublesEqual(bounds.getWidth() * cellHeight, bounds.getHeight() * cellWidth))
				{
					System.out.println("Error: cell " + c.noLibDescribe() + " in library " + elib.getLibName() +
						" has changed size since its use in library " + lib.getLibName());
					System.out.println("  The cell is " + bounds.getWidth() + "x" +  bounds.getHeight() +
						" but the instances in library " + lib.getLibName() + " are " + cellWidth + "x" + cellHeight);
					c = null;
				}
			}
		}

		// if cell found, check that ports match
		if (c != null)
		{
			for(int j=0; j<portCount; j++)
			{
				PortProto pp = c.findExport(localPortNames[j]);
				if (pp == null)
				{
					System.out.println("Error: cell " + c.noLibDescribe() + " in library " + elib.getLibName() +
						" is missing export " + localPortNames[j]);
// 					for (Iterator it = c.getPorts(); it.hasNext(); )
// 					{
// 						PortProto pp = (PortProto)it.next();
// 						System.out.println("\t"+pp.getProtoName());
// 					}
					c = null;
					break;
				}
			}
		}

		// if cell found, warn if minor modification was made
		if (c != null)
		{
			if (revisionDate.compareTo(c.getRevisionDate()) != 0)
			{
				System.out.println("Error: cell " + c.noLibDescribe() + " in library " + elib.getLibName() +
					" has changed since its use in library " + lib.getLibName());
			}
		}

		// make new cell if needed
		boolean newCell = false;
		NodeInst fakeNodeInst = null;
		if (c == null)
		{
			// create a cell that meets these specs
			String dummyCellName = null;
			for(int index=0; ; index++)
			{
				dummyCellName = theProtoName + "FROM" + elib.getLibName();
				if (index > 0) dummyCellName += "." + index;
				dummyCellName += "{" + v.getAbbreviation() + "}";
				if (lib.findNodeProto(dummyCellName) == null) break;
			}
			c = Cell.newInstance(lib, dummyCellName);
			if (c == null) return true;
			c.lowLevelSetCreationDate(creationDate);
			c.lowLevelSetRevisionDate(revisionDate);

			// create an artwork "Crossed box" to define the cell size
			Technology tech = MoCMOS.tech;
			if (v == View.ICON) tech = Artwork.tech; else
				if (v == View.SCHEMATIC) tech = Schematics.tech;
			double lambda = techScale[tech.getIndex()];
			int cX = (lowX + highX) / 2;
			int cY = (lowY + highY) / 2;
			double width = (highX - lowX) / lambda;
			double height = (highY - lowY) / lambda;
			Point2D center = new Point2D.Double(cX / lambda, cY / lambda);
			NodeInst.newInstance(Generic.tech.drcNode, center, width, height, 0, c, null);
			fakeNodeInst = NodeInst.newInstance(Generic.tech.universalPinNode, center, width, height, 0, c, null);

			newCell = true;
			System.out.println("...Creating dummy version of cell in library " + lib.getLibName());
			c.newVar(IO_TRUE_LIBRARY, elib.getLibName());
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
			Export pp = c.findExport(protoName);
			if (pp == null)
			{
				if (newCell)
				{
					PortInst pi = fakeNodeInst.findPortInst("univ");
					pp = Export.newInstance(c, pi, protoName);
				} else
				{
					System.out.println("Cannot find port " + protoName + " on cell " + c.describe() + " in library " + elib.getLibName());
				}
			}
			portProtoList[portProtoIndex] = pp;
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
		NodeInst ni = nodeList[nodeIndex];
		int protoIndex = readBigInteger();
		NodeProto np = convertNodeProto(protoIndex);
		if (np == null) return true;

		// read the descriptive information
		nodeTypeList[nodeIndex] = np;
		nodeLowXList[nodeIndex] = readBigInteger();
		nodeLowYList[nodeIndex] = readBigInteger();
		nodeHighXList[nodeIndex] = readBigInteger();
		nodeHighYList[nodeIndex] = readBigInteger();
		nodeTransposeList[nodeIndex] = readBigInteger();
		nodeRotationList[nodeIndex] = (short)readBigInteger();
		nodeNameList[nodeIndex] = null;

		// versions 9 and later get text descriptor for cell name
		int descript0 = 0, descript1 = 0;
		if (magic <= MAGIC9)
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
		TextDescriptor descript = new TextDescriptor(null, descript0, descript1);
		Input.fixTextDescriptorFont(descript);
		ni.setProtoTextDescriptor(descript);

		// read the nodeinst name (versions 1, 2, or 3 only)
		if (magic >= MAGIC3)
		{
			String instName = readString();
			if (instName.length() > 0)
				ni.setName(instName);
		}

		// ignore the geometry index (versions 4 or older)
		if (magic > MAGIC5) readBigInteger();

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
			PortProto pp = convertPortProto(portIndex);
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
		if (magic > MAGIC9) readBigInteger();

		// read the tool information
		int userBits = 0;
		if (magic <= MAGIC7)
		{
			// version 7 and later simply read the relevant data
			userBits = readBigInteger();
		} else
		{
			// version 6 and earlier must sift through the information
			if (toolBCount >= 1) userBits = readBigInteger();
			for(int i=1; i<toolBCount; i++) readBigInteger();
		}
		ni.lowLevelSetUserbits(userBits);

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
		if (magic >= MAGIC5) readBigInteger();

		// read the arc width
		arcWidthList[arcIndex] = readBigInteger();

		// ignore the signals value (versions 6, 7, or 8)
		if (magic <= MAGIC6 && magic >= MAGIC8)
			readBigInteger();

		// read the arcinst name (versions 3 or older)
		if (magic >= MAGIC3)
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
			arcHeadNodeList[arcIndex] = nodeList[nodeIndex];

		// read the tail information
		arcTailXPosList[arcIndex] = readBigInteger();
		arcTailYPosList[arcIndex] = readBigInteger();
		nodeIndex = readBigInteger();
		if (nodeIndex >= 0 && nodeIndex < nodeCount)
			arcTailNodeList[arcIndex] = nodeList[nodeIndex];

		// ignore the geometry index (versions 4 or older)
		if (magic > MAGIC5) readBigInteger();

		// ignore the "seen" bits (versions 8 and older)
		if (magic > MAGIC9) readBigInteger();

		// read the arcinst's tool information
		int userBits = 0;
		if (magic <= MAGIC7)
		{
			// version 7 and later simply read the relevant data
			userBits = readBigInteger();

			// versions 7 and 8 ignore net number
			if (magic >= MAGIC8) readBigInteger();
		} else
		{
			// version 6 and earlier must sift through the information
			if (toolBCount >= 1) userBits = readBigInteger();
			for(int i=1; i<toolBCount; i++) readBigInteger();
		}
		ai.lowLevelSetUserbits(userBits);

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
		realKey = new Variable.Key[nameCount];
		for(int i=0; i<nameCount; i++)
			realKey[i] = ElectricObject.newKey(readString());
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
			if (magic <= MAGIC9)
			{
				if (alwaysTextDescriptors)
				{
					descript0 = readBigInteger();
					descript1 = readBigInteger();
					definedDescript = true;
				} else
				{
					if ((newtype&BinaryConstants.VDISPLAY) != 0)
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
			if ((newtype&BinaryConstants.VISARRAY) != 0)
			{
				int len = readBigInteger();
				int cou = len;
				if ((newtype&BinaryConstants.VLENGTH) == 0) cou++;
				Object [] newAddrArray = null;
				switch (newtype&BinaryConstants.VTYPE)
				{
					case BinaryConstants.VADDRESS:
					case BinaryConstants.VINTEGER:    newAddrArray = new Integer[cou];     break;
					case BinaryConstants.VFRACT:      
					case BinaryConstants.VFLOAT:      newAddrArray = new Float[cou];       break;
					case BinaryConstants.VDOUBLE:     newAddrArray = new Double[cou];      break;
					case BinaryConstants.VSHORT:      newAddrArray = new Short[cou];       break;
					case BinaryConstants.VBOOLEAN:
					case BinaryConstants.VCHAR:       newAddrArray = new Byte[cou];        break;
					case BinaryConstants.VSTRING:     newAddrArray = new String[cou];      break;
					case BinaryConstants.VNODEINST:   newAddrArray = new NodeInst[cou];    break;
					case BinaryConstants.VNODEPROTO:  newAddrArray = new NodeProto[cou];   break;
					case BinaryConstants.VARCPROTO:   newAddrArray = new ArcProto[cou];    break;
					case BinaryConstants.VPORTPROTO:  newAddrArray = new PortProto[cou];   break;
					case BinaryConstants.VARCINST:    newAddrArray = new ArcInst[cou];     break;
					case BinaryConstants.VTECHNOLOGY: newAddrArray = new Technology[cou];  break;
					case BinaryConstants.VLIBRARY:    newAddrArray = new Library[cou];     break;
					case BinaryConstants.VTOOL:       newAddrArray = new Tool[cou];        break;
				}
				newAddr = newAddrArray;
				if ((newtype&BinaryConstants.VTYPE) == BinaryConstants.VGENERAL)
				{
					for(int j=0; j<len; j += 2)
					{
						int type = readBigInteger();
						int addr = readBigInteger();
						newAddrArray[j] = null;
					}
				} else
				{
					for(int j=0; j<len; j++)
					{
						Object ret = getInVar(newtype);
						if (ret == null) return(-1);
						newAddrArray[j] = ret;
					}
					
				}
			} else
			{
				newAddr = getInVar(newtype);
				if (newAddr == null) return(-1);
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
				if ((obj instanceof NodeInst && realKey[key] == NodeInst.NODE_NAME) ||
					(obj instanceof ArcInst && realKey[key] == ArcInst.ARC_NAME))
				{
					Geometric geom = (Geometric)obj;
					TextDescriptor nameDescript = new TextDescriptor(null, descript0, descript1);
					Input.fixTextDescriptorFont(nameDescript);
					geom.setNameTextDescriptor(nameDescript);
					Name name = makeGeomName(geom, newAddr, newtype);
					if (obj instanceof NodeInst)
						nodeNameList[index] = name;
					else
						arcNameList[index] = name;
					continue;
				}
			}

			// see if the variable is deprecated
			boolean invalid = obj.isDeprecatedVariable(realKey[key]);
			if (!invalid)
			{
				Variable.Key varKey = realKey[key];
				Variable var = null;
				if (var == null)
				{
					var = obj.newVar(varKey, newAddr);
				}
				if (var == null) return(-1);
				var.setDescriptor(new TextDescriptor(null, descript0, descript1));
				var.lowLevelSetFlags(newtype);

				// handle updating of technology caches
//				if (type == VTECHNOLOGY)
//					changedtechnologyvariable(keyval);
			}
		}
		return(count);
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

		if ((ty&(BinaryConstants.VCODE1|BinaryConstants.VCODE2)) != 0) ty = BinaryConstants.VSTRING;
		switch (ty&BinaryConstants.VTYPE)
		{
			case BinaryConstants.VADDRESS:
			case BinaryConstants.VINTEGER:
				return new Integer(readBigInteger());
			case BinaryConstants.VFRACT:
				return new Float(readBigInteger() / 120.0f);
			case BinaryConstants.VFLOAT:
				return new Float(readFloat());
			case BinaryConstants.VDOUBLE:
				return new Double(readDouble());
			case BinaryConstants.VSHORT:
				return new Short(readSmallInteger());
			case BinaryConstants.VBOOLEAN:
			case BinaryConstants.VCHAR:
				return new Byte(readByte());
			case BinaryConstants.VSTRING:
				return readString();
			case BinaryConstants.VNODEINST:
				i = readBigInteger();
				if (i < 0 || i >= nodeCount) return null;
				return nodeList[i];
			case BinaryConstants.VNODEPROTO:
				i = readBigInteger();
				return convertNodeProto(i);
			case BinaryConstants.VARCPROTO:
				i = readBigInteger();
				return convertArcProto(i);
			case BinaryConstants.VPORTPROTO:
				i = readBigInteger();
				return convertPortProto(i);
			case BinaryConstants.VARCINST:
				i = readBigInteger();
				if (i < 0 || i >= arcCount) return null;
				return arcList[i];
			case BinaryConstants.VGEOM:
				readBigInteger();
				return null;
			case BinaryConstants.VTECHNOLOGY:
				i = readBigInteger();
				if (i == -1) return null;
				return getTechList(i);
			case BinaryConstants.VPORTARCINST:
				readBigInteger();
				return null;
			case BinaryConstants.VPORTEXPINST:
				readBigInteger();
				return null;
			case BinaryConstants.VLIBRARY:
				String libName = readString();
				return Library.findLibrary(libName);
			case BinaryConstants.VTOOL:
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
			case BinaryConstants.VRTNODE:
				readBigInteger();
				return null;
		}
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
			return getPrimNodeProtoList(0);
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
		return portProtoList[i];
	}

	private NodeProto getPrimNodeProtoList(int i)
	{
		getTechList(primNodeProtoTech[i]);
		if (primNodeProtoError[i])
		{
			System.out.println("Cannot find primitive '" + primNodeProtoOrig[i] + "', using " +
				primNodeProtoList[i].getProtoName());
			primNodeProtoError[i] = false;
		}
		return(primNodeProtoList[i]);
	}

	private PrimitiveArc getArcProtoList(int i)
	{
		if (arcProtoError[i] != null)
		{
			System.out.println("Cannot find arc '" + arcProtoError[i] + "', using " + arcProtoList[i].getProtoName());
			arcProtoError[i] = null;
		}
		return(arcProtoList[i]);
	}

	private PortProto getPrimPortProtoList(int i)
	{
		if (primPortProtoError[i] != null)
		{
			System.out.println("WARNING: port " + primPortProtoError[i] + " not found, using " +
				primPortProtoList[i].getProtoName());
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
		View v = null;
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			v = (View) it.next();
			if (v.getTempInt() == i) return v;
		}
		return null;
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
		readMoreBytes(1);
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
			readMoreBytes(4);
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
			readMoreBytes(4);
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
			readMoreBytes(8);
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
			readMoreBytes(2);
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
			byte [] stringBytes = new byte[len];
			int ret = dataInputStream.read(stringBytes, 0, len);
			if (ret != len) throw new IOException();
			readMoreBytes(len);
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
		readMoreBytes(diskSize);
	}
	
	private void readMoreBytes(int diskSize)
	{
		byteCount += diskSize;
		if (progress != null && fileLength > 0)
		{
			progress.setProgress((int)(byteCount * 100 / fileLength));
		}
	}
}
