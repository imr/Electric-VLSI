/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OutputBinary.java
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

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.ui.UITopLevel;
import com.sun.electric.tool.io.InputBinary;

import java.io.IOException;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.awt.geom.Point2D;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.nio.ByteOrder;
import java.awt.geom.Rectangle2D;


public class OutputBinary extends Output
{

	/** all of the names used in variables */			private static HashMap varNames;

	OutputBinary()
	{
	}

	// ----------------------- public methods -------------------------------

	public boolean WriteLib(Library lib)
	{
		try
		{
			return writeTheLibrary(lib);
		} catch (IOException e)
		{
			System.out.println("End of file reached while writing " + filePath);
			return true;
		}
	}

	/**
	 * Routine to write the .elib file.
	 * Returns true on error.
	 */
	private boolean writeTheLibrary(Library lib)
		throws IOException
	{
		writeBigInteger(InputBinary.MAGIC12);
		writeBigInteger(2);		// size of Short
		writeBigInteger(4);		// size of Int
		writeBigInteger(1);		// size of Char

		// write the number of tools
		int toolCount = Tool.getNumTools();
		writeBigInteger(toolCount);
		int techCount = Technology.getNumTechnologies();
		writeBigInteger(techCount);

		// initialize the number of objects in the database
		int nodeIndex = 0;
		int portprotoIndex = 0;
		int nodeprotoIndex = 0;
		int arcIndex = 0;
		int nodepprotoIndex = 0;
		int portpprotoIndex = 0;
		int arcprotoIndex = 0;
		int cellIndex = 0;

		// count and number the cells, nodes, arcs, and ports in this library
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			cell.setTemp1(nodeprotoIndex++);
			for(Iterator pit = cell.getPorts(); pit.hasNext(); )
			{
				Export pp = (Export)pit.next();
				pp.setTemp1(portprotoIndex++);
			}
			for(Iterator ait = cell.getArcs(); ait.hasNext(); )
			{
				ArcInst ai = (ArcInst)ait.next();
				ai.setTemp1(arcIndex++);
			}
			for(Iterator nit = cell.getNodes(); nit.hasNext(); )
			{
				NodeInst ni = (NodeInst)nit.next();
				ni.setTemp1(nodeIndex++);
			}
		}
		int cellsHere = nodeprotoIndex;

		// prepare to locate references to cells in other libraries
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library olib = (Library)it.next();
			for(Iterator cit = olib.getCells(); cit.hasNext(); )
			{
				Cell cell = (Cell)cit.next();
				cell.setTemp2(0);
			}
		}

		// scan for all cross-library references
		varNames = new HashMap();
		findXLibVariables(lib);
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			findXLibVariables(tool);
		}
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			findXLibVariables(tech);
			for(Iterator ait = tech.getArcs(); ait.hasNext(); )
			{
				ArcProto ap = (ArcProto)ait.next();
				findXLibVariables(ap);
			}
			for(Iterator nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)nit.next();
				findXLibVariables(np);
				for(Iterator eit = np.getPorts(); eit.hasNext(); )
				{
					PrimitivePort pp = (PrimitivePort)eit.next();
					findXLibVariables(pp);
				}
			}
		}
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View view = (View)it.next();
			findXLibVariables(view);
		}
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			findXLibVariables(cell);
			for(Iterator eit = cell.getPorts(); eit.hasNext(); )
			{
				Export pp = (Export)eit.next();
				findXLibVariables(pp);
			}
			for(Iterator nit = cell.getNodes(); nit.hasNext(); )
			{
				NodeInst ni = (NodeInst)nit.next();
				findXLibVariables(ni);
				for(Iterator cit = ni.getConnections(); cit.hasNext(); )
				{
					Connection con = (Connection)cit.next();
					findXLibVariables(con);
				}
				for(Iterator eit = ni.getExports(); eit.hasNext(); )
				{
					Export pp = (Export)eit.next();
					findXLibVariables(pp);
				}
				if (ni.getProto() instanceof Cell)
					ni.getProto().setTemp2(1);
			}
			for(Iterator ait = cell.getArcs(); ait.hasNext(); )
			{
				ArcInst ai = (ArcInst)ait.next();
				findXLibVariables(ai);
			}
		}

		// count and number the cells in other libraries
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library olib = (Library)it.next();
			if (olib == lib) continue;
			for(Iterator cit = olib.getCells(); cit.hasNext(); )
			{
				Cell cell = (Cell)cit.next();
				if (cell.getTemp2() == 0) continue;
				cell.setTemp1(nodeprotoIndex++);
				for(Iterator eit = cell.getPorts(); eit.hasNext(); )
				{
					Export pp = (Export)eit.next();
					pp.setTemp1(portprotoIndex++);
				}
			}
		}

		// count and number the primitive node and port prototypes
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)nit.next();
				np.setTemp1(-2 - nodepprotoIndex++);
				for(Iterator eit = np.getPorts(); eit.hasNext(); )
				{
					Export pp = (Export)eit.next();
					pp.setTemp1(-2 - portpprotoIndex++);
				}
			}
			for(Iterator ait = tech.getArcs(); ait.hasNext(); )
			{
				ArcProto ap = (ArcProto)ait.next();
				ap.setTemp1(-2 - arcprotoIndex++);
			}
		}

		// write number of objects
		writeBigInteger(nodepprotoIndex);
		writeBigInteger(portpprotoIndex);
		writeBigInteger(arcprotoIndex);
		writeBigInteger(nodeprotoIndex);
		writeBigInteger(nodeIndex);
		writeBigInteger(portprotoIndex);
		writeBigInteger(arcIndex);
		writeBigInteger(0);

		// write the current cell
		int curNodeProto = -1;
		if (lib.getCurCell() != null)
			curNodeProto = lib.getCurCell().getTemp1();
		writeBigInteger(curNodeProto);

		// write the version number
		writeString(Version.CURRENT);

		// number the views and write nonstandard ones
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View view = (View)it.next();
			view.setTemp1(0);
		}
		View.UNKNOWN.setTemp1(-1);
		View.LAYOUT.setTemp1(-2);
		View.SCHEMATIC.setTemp1(-3);
		View.ICON.setTemp1(-4);
		View.SIMSNAP.setTemp1(-5);
		View.SKELETON.setTemp1(-6);
		View.VHDL.setTemp1(-7);
		View.NETLIST.setTemp1(-8);
		View.DOC.setTemp1(-9);
		View.NETLISTNETLISP.setTemp1(-10);
		View.NETLISTALS.setTemp1(-11);
		View.NETLISTQUISC.setTemp1(-12);
		View.NETLISTRSIM.setTemp1(-13);
		View.NETLISTSILOS.setTemp1(-14);
		View.VERILOG.setTemp1(-15);
		View.COMP.setTemp1(-16);
		int i = 1;
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View view = (View)it.next();
			if (view.getTemp1() == 0) view.setTemp1(i++);
		}
		i--;
		writeBigInteger(i);
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View view = (View)it.next();
			if (view.getTemp1() < 0) continue;
			writeString(view.getFullName());
			writeString(view.getShortName());
		}

		// write total number of arcinsts, nodeinsts, and ports in each cell
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			writeBigInteger(cell.getNumArcs());
			writeBigInteger(cell.getNumNodes());
			writeBigInteger(cell.getNumPorts());
		}

		// write dummy numbers of arcinsts and nodeinst; count ports for external cells
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library olib = (Library)it.next();
			if (olib == lib) continue;
			for(Iterator cit = olib.getCells(); cit.hasNext(); )
			{
				Cell cell = (Cell)cit.next();
				if (cell.getTemp2() == 0) continue;
				writeBigInteger(-1);
				writeBigInteger(-1);
				writeBigInteger(cell.getNumPorts());
			}
		}

		// write the names of technologies and primitive prototypes
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();

			// write the technology name
			writeString(tech.getTechName());

			// count and write the number of primitive node prototypes
			writeBigInteger(tech.getNumNodes());

			for(Iterator nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)nit.next();

				// write the primitive node prototype name
				writeString(np.getProtoName());
				writeBigInteger(np.getNumPorts());
				for(Iterator pit = np.getPorts(); pit.hasNext(); )
				{
					PrimitivePort pp = (PrimitivePort)pit.next();
					writeString(pp.getProtoName());
				}
			}

			// count and write the number of arc prototypes
			writeBigInteger(tech.getNumArcs());

			// write the primitive arc prototype names
			for(Iterator ait = tech.getArcs(); ait.hasNext(); )
			{
				PrimitiveArc ap = (PrimitiveArc)ait.next();
				writeString(ap.getProtoName());
			}
		}

		// write the names of the tools
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			writeString(tool.getName());
		}

		// write the userbits for the library
		writeBigInteger(lib.lowLevelGetUserBits());

		// write the tool lambda values
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			writeBigInteger((int)tech.getScale());
		}

		// write the global namespace
		writeNameSpace();

		// write the library variables
		writeVariables(lib);

		// write the tool variables
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			writeVariables(tool);
		}

		// write the variables on technologies
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			writeVariables(tech);
		}

		// write the arcproto variables
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator ait = tech.getArcs(); ait.hasNext(); )
			{
				PrimitiveArc ap = (PrimitiveArc)ait.next();
				writeVariables(ap);
			}
		}

		// write the variables on primitive node prototypes
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)nit.next();
				writeVariables(np);
			}
		}

		// write the variables on primitive port prototypes
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)nit.next();
				for(Iterator pit = np.getPorts(); pit.hasNext(); )
				{
					PrimitivePort pp = (PrimitivePort)pit.next();
					writeVariables(pp);
				}
			}
		}

		// write the view variables
		writeBigInteger(View.getNumViews());
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View view = (View)it.next();
			writeBigInteger(view.getTemp1());
			writeVariables(view);
		}

		// write all of the cells in this library
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			writeNodeProto(cell, true);
		}

		// write all of the cells in external libraries
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library olib = (Library)it.next();
			if (olib == lib) continue;
			for(Iterator cit = olib.getCells(); cit.hasNext(); )
			{
				Cell cell = (Cell)cit.next();
				if (cell.getTemp2() == 0) continue;
				writeNodeProto(cell, false);
			}
		}

		// write all of the arcs and nodes in this library
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			for(Iterator ait = cell.getArcs(); ait.hasNext(); )
			{
				ArcInst ai = (ArcInst)ait.next();
				writeArcInst(ai);
			}
			for(Iterator nit = cell.getArcs(); nit.hasNext(); )
			{
				NodeInst ni = (NodeInst)nit.next();
				writeNodeInst(ni);
			}
		}

		// restore any damage to the database
//		io_cleanup();


		if (!lib.isHidden())
		{
			System.out.println(filePath + " written (" + cellsHere + " cells)");
		}
		lib.clearChangedMinor();
		lib.clearChangedMajor();
		lib.setFromDisk();

		// library written successfully
		return false;
	}

	void writeNodeProto(Cell cell, boolean thislib)
		throws IOException
	{
//		INTBIG i;
//		REGISTER PORTPROTO *pp;
//		REGISTER LIBRARY *instlib;
//		static INTBIG nullptr = -1;

//		// write cell information
//		writeString(cell.getProtoName());
//		writeBigInteger(&cell->nextcellgrp->temp1);
//		if (cell->nextcont != NONODEPROTO) writeBigInteger(&cell->nextcont->temp1); else
//			writeBigInteger(&nullptr);
//		writeBigInteger(&cell->cellview->temp1);
//		i = cell->version;   writeBigInteger(&i);
//		writeBigInteger(&cell->creationdate);
//		writeBigInteger(&cell->revisiondate);
//
//		// write the nodeproto bounding box
//		writeBigInteger(&cell->lowx);0
//		writeBigInteger(&cell->highx);
//		writeBigInteger(&cell->lowy);
//		writeBigInteger(&cell->highy);
//
//		if (!thislib)
//		{
//			instlib = cell->lib;
//			writeString(instlib->libfile);
//		}
//
//		// write the number of portprotos on this nodeproto
//		i = 0;
//		for(pp = cell->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto) i++;
//		writeBigInteger(&i);
//
//		// write the portprotos on this nodeproto
//		for(pp = cell->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//		{
//			if (thislib)
//			{
//				// write the connecting subnodeinst for this portproto
//				if (pp->subnodeinst != NONODEINST) i = pp->subnodeinst->temp1; else
//				{
//					ttyputmsg(_("ERROR: Library %s, cell %s, export %s has no node"),
//						cell->lib->libname, describenodeproto(cell), pp->protoname);
//					i = -1;
//				}	
//				writeBigInteger(&i);
//
//				// write the portproto index in the subnodeinst
//				if (pp->subnodeinst != NONODEINST && pp->subportproto != NOPORTPROTO)
//				{
//					i = pp->subportproto->temp1;
//				} else
//				{
//					if (pp->subportproto != NOPORTPROTO)
//						ttyputmsg(_("ERROR: Library %s, cell %s, export %s has no subport"),
//							cell->lib->libname, describenodeproto(cell), pp->protoname);
//					i = -1;
//				}
//				writeBigInteger(&i);
//			}
//
//			// write the portproto name
//			writeString(pp->protoname);
//
//			if (thislib)
//			{
//				// write the text descriptor
//				writeBigInteger(&pp->textdescript[0]);
//				writeBigInteger(&pp->textdescript[1]);
//
//				// write the portproto tool information
//				writeBigInteger(&pp->userbits);
//
//				// write variable information
//				writeVariables(pp);
//			}
//		}
//
//		if (thislib)
//		{
//			// write tool information
//			writeBigInteger(&cell->adirty);
//			writeBigInteger(&cell->userbits);
//
//			// write variable information
//			writeVariables(cell);
//		}
	}

	void writeNodeInst(NodeInst ni)
		throws IOException
	{
//		INTBIG i;
//		REGISTER ARCINST *ai;
//		REGISTER PORTARCINST *pi;
//		REGISTER PORTEXPINST *pe;
//
//		// write the nodeproto pointer
//		writeBigInteger(&ni->proto->temp1);
//
//		// write descriptive information
//		writeBigInteger(&ni->lowx);
//		writeBigInteger(&ni->lowy);
//		writeBigInteger(&ni->highx);
//		writeBigInteger(&ni->highy);
//		i = ni->transpose;   writeBigInteger(&i);
//		i = ni->rotation;    writeBigInteger(&i);
//
//		writeBigInteger(&ni->textdescript[0]);
//		writeBigInteger(&ni->textdescript[1]);
//
//		// count the arc ports
//		i = 0;
//		for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst) i++;
//		writeBigInteger(&i);
//
//		// write the arc ports
//		for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//		{
//			// write the arcinst index (and the particular end on that arc)
//			ai = pi->conarcinst;
//			if (ai->end[0].portarcinst == pi) i = 0; else i = 1;
//			i = (ai->temp1 << 1) + (ai->end[0].portarcinst == pi ? 0 : 1);
//			writeBigInteger(&i);
//
//			// write the portinst prototype
//			writeBigInteger(&pi->proto->temp1);
//
//			// write the variable information
//			writeVariables(pi);
//		}
//
//		// count the exports
//		i = 0;
//		for(pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst) i++;
//		writeBigInteger(&i);
//
//		// write the exports
//		for(pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst)
//		{
//			writeBigInteger(&pe->exportproto->temp1);
//
//			// write the portinst prototype
//			writeBigInteger(&pe->proto->temp1);
//
//			// write the variable information
//			writeVariables(pe);
//		}
//
//		// write the tool information
//		writeBigInteger(&ni->userbits);
//
//		// write variable information
//		writeVariables(ni);
	}

	void writeArcInst(ArcInst ai)
		throws IOException
	{
		// write the arcproto pointer
		writeBigInteger(ai.getProto().getTemp1());

		// write basic arcinst information
		writeBigInteger((int)ai.getWidth());

		// write the arcinst head information
		Point2D location = ai.getHead().getLocation();
		writeBigInteger((int)location.getX());
		writeBigInteger((int)location.getY());
		writeBigInteger(ai.getHead().getPortInst().getNodeInst().getTemp1());

		// write the arcinst tail information
		location = ai.getTail().getLocation();
		writeBigInteger((int)location.getX());
		writeBigInteger((int)location.getY());
		writeBigInteger(ai.getTail().getPortInst().getNodeInst().getTemp1());

		// write the arcinst's tool information
		writeBigInteger(ai.lowLevelGetUserbits());

		// write variable information
		writeVariables(ai);
	}

	// --------------------------------- VARIABLES ---------------------------------

	private static final int VUNKNOWN =                  0;		/** undefined variable */
	private static final int VINTEGER =                 01;		/** 32-bit integer variable */
	private static final int VADDRESS =                 02;		/** unsigned address */
	private static final int VCHAR =                    03;		/** character variable */
	private static final int VSTRING =                  04;		/** string variable */
	private static final int VFLOAT =                   05;		/** floating point variable */
	private static final int VDOUBLE =                  06;		/** double-precision floating point */
	private static final int VNODEINST =                07;		/** nodeinst pointer */
	private static final int VNODEPROTO =              010;		/** nodeproto pointer */
	private static final int VPORTARCINST =            011;		/** portarcinst pointer */
	private static final int VPORTEXPINST =            012;		/** portexpinst pointer */
	private static final int VPORTPROTO =              013;		/** portproto pointer */
	private static final int VARCINST =                014;		/** arcinst pointer */
	private static final int VARCPROTO =               015;		/** arcproto pointer */
	private static final int VGEOM =                   016;		/** geometry pointer */
	private static final int VLIBRARY =                017;		/** library pointer */
	private static final int VTECHNOLOGY =             020;		/** technology pointer */
	private static final int VTOOL =                   021;		/** tool pointer */
	private static final int VRTNODE =                 022;		/** R-tree pointer */
	private static final int VFRACT =                  023;		/** fractional integer (scaled by WHOLE) */
	private static final int VNETWORK =                024;		/** network pointer */
	private static final int VVIEW =                   026;		/** view pointer */
	private static final int VWINDOWPART =             027;		/** window partition pointer */
	private static final int VGRAPHICS =               030;		/** graphics object pointer */
	private static final int VSHORT =                  031;		/** 16-bit integer */
	private static final int VCONSTRAINT =             032;		/** constraint solver */
	private static final int VGENERAL =                033;		/** general address/type pairs (used only in fixed-length arrays) */
	private static final int VWINDOWFRAME =            034;		/** window frame pointer */
	private static final int VPOLYGON =                035;		/** polygon pointer */
	private static final int VBOOLEAN =                036;		/** boolean variable */
	private static final int VTYPE =                   037;		/** all above type fields */
	private static final int VCODE1 =                  040;		/** variable is interpreted code (with VCODE2) */
	private static final int VDISPLAY =               0100;		/** display variable (uses textdescript field) */
	private static final int VISARRAY =               0200;		/** set if variable is array of above objects */
	private static final int VLENGTH =         03777777000;		/** array length (0: array is -1 terminated) */
	private static final int VCODE2 =          04000000000;		/** variable is interpreted code (with VCODE1) */

	/**
	 * routine to write the global namespace.  returns true upon error
	 */
	boolean writeNameSpace()
		throws IOException
	{
		writeBigInteger(ElectricObject.getNumVariableNames());
		
		// THIS IS WRONG: MUST SORT BY INDEX AND WRITE IN THAT ORDER!!!
		for(Iterator it = ElectricObject.getVariableNames(); it.hasNext(); )
		{
			Variable.Name vn = (Variable.Name)it.next();
			writeString(vn.getName());
		}
		return false;
	}

	/**
	 * routine to write a set of object variables.  returns negative upon error and
	 * otherwise returns the number of variables write
	 */
	int writeVariables(ElectricObject obj)
		throws IOException
	{
		int count = 0;
		for(Iterator it = obj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.isDontSave()) count++;
		}
		writeBigInteger(count);
		for(Iterator it = obj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.isDontSave()) continue;
			Variable.Name vn = var.getName();
			writeSmallInteger((short)vn.getIndex());
			writeBigInteger(var.lowLevelGetFlags());
			TextDescriptor td = var.getTextDescriptor();
			writeBigInteger(td.lowLevelGet0());
			writeBigInteger(td.lowLevelGet1());

			Object varObj = var.getObject();
			if (varObj instanceof Object[])
			{
				int len = ((Object[])varObj).length;
				writeBigInteger(len);
				for(int i=0; i<len; i++)
				{
					Object oneObj = ((Object[])varObj)[i];
					putOutVar(oneObj);
				}
			} else
			{
				putOutVar(obj);
			}
		}
		return(count);
	}

	/**
	 * Helper routine to write a variable at address "addr" of type "ty".
	 * Returns zero if OK, negative on memory error, positive if there were
	 * correctable problems in the write.
	 */
	void putOutVar(Object obj)
		throws IOException
	{
		if (obj instanceof Integer)
		{
			writeBigInteger(((Integer)obj).intValue());
			return;
		}
		if (obj instanceof Short)
		{
			writeSmallInteger(((Short)obj).shortValue());
			return;
		}
		if (obj instanceof Byte)
		{
			writeByte(((Byte)obj).byteValue());
			return;
		}
		if (obj instanceof String)
		{
			writeString((String)obj);
			return;
		}
		if (obj instanceof Float)
		{
			writeFloat(((Float)obj).floatValue());
			return;
		}
		if (obj instanceof Double)
		{
			writeDouble(((Double)obj).doubleValue());
			return;
		}
		if (obj instanceof Technology)
		{
			Technology tech = (Technology)obj;
			if (tech == null) writeBigInteger(-1); else
				writeBigInteger(tech.getIndex());
			return;
		}
		if (obj instanceof Library)
		{
			Library lib = (Library)obj;
			if (lib == null) writeString("noname"); else
				writeString(lib.getLibName());
			return;
		}
		if (obj instanceof Tool)
		{
			Tool tool = (Tool)obj;
			if (tool == null) writeBigInteger(-1); else
				writeBigInteger(tool.getIndex());
			return;
		}
		if (obj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)obj;
			if (ni == null) writeBigInteger(-1); else
				writeBigInteger(ni.getTemp1());
			return;
		}
		if (obj instanceof ArcInst)
		{
			ArcInst ai = (ArcInst)obj;
			if (ai == null) writeBigInteger(-1); else
				writeBigInteger(ai.getTemp1());
			return;
		}
		if (obj instanceof NodeProto)
		{
			NodeProto np = (NodeProto)obj;
			if (np == null) writeBigInteger(-1); else
				writeBigInteger(np.getTemp1());
			return;
		}
		if (obj instanceof ArcProto)
		{
			ArcProto ap = (ArcProto)obj;
			if (ap == null) writeBigInteger(-1); else
				writeBigInteger(ap.getTemp1());
			return;
		}
		if (obj instanceof PortProto)
		{
			PortProto pp = (PortProto)obj;
			if (pp == null) writeBigInteger(-1); else
				writeBigInteger(pp.getTemp1());
			return;
		}
	}

	// --------------------------------- OBJECT CONVERSION ---------------------------------

	/*
	 * Routine to scan the variables on an object (which are in "firstvar" and "numvar")
	 * for NODEPROTO references.  Any found are marked (by setting "temp2" to 1).
	 * This is used to gather cross-library references.
	 */
	void findXLibVariables(ElectricObject obj)
	{
//		REGISTER INTBIG i;
//		REGISTER INTBIG len, type, j;
//		REGISTER VARIABLE *var;
//		REGISTER NODEPROTO *np, **nparray;
//
//		for(i=0; i<numvar; i++)
//		{
//			var = &firstvar[i];
//			type = var->type;
//			if ((type&VDONTSAVE) != 0) continue;
//			if ((type&VTYPE) != VNODEPROTO) continue;
//			if ((type&VISARRAY) != 0)
//			{
//				len = (type&VLENGTH) >> VLENGTHSH;
//				nparray = (NODEPROTO **)var->addr;
//				if (len == 0) for(len=0; nparray[len] != NONODEPROTO; len++) ;
//				for(j=0; j<len; j++)
//				{
//					np = nparray[j];
//					if (np != NONODEPROTO && np->primindex == 0)
//					{
//						np->temp2 = 1;
//					}
//				}
//			} else
//			{
//				np = (NODEPROTO *)var->addr;
//				if (np != NONODEPROTO && np->primindex == 0)
//				{
//					np->temp2 = 1;
//				}
//			}
//		}
	}

	/**
	 * routine to convert the Java Date object to an Electric-format date (seconds since the epoch).
	 */
	int toElectricDate(Date secondsSinceEpoch)
	{
//		GregorianCalendar creation = new GregorianCalendar();
//		creation.setTimeInMillis(0);
//		creation.setLenient(true);
//		creation.add(Calendar.SECOND, secondsSinceEpoch);
//		return creation.getTime();
		return 0;
	}

	// --------------------------------- LOW-LEVEL INPUT ---------------------------------

	/**
	 * routine to write a single byte from the input stream and return it.
	 */
	void writeByte(byte b)
		throws IOException
	{
		dataOutputStream.write(b);
	}

	static ByteBuffer bb = ByteBuffer.allocateDirect(8);
	static byte [] rawData = new byte[8];

	/**
	 * routine to write an integer (4 bytes) from the input stream and return it.
	 */
	void writeBigInteger(int i)
		throws IOException
	{
		dataOutputStream.writeInt(i);
	}

	/**
	 * routine to write a float (4 bytes) from the input stream and return it.
	 */
	void writeFloat(float f)
		throws IOException
	{
		dataOutputStream.writeFloat(f);
	}

	/**
	 * routine to write a double (8 bytes) from the input stream and return it.
	 */
	void writeDouble(double d)
		throws IOException
	{
		dataOutputStream.writeDouble(d);
	}

	/**
	 * routine to write an short (2 bytes) from the input stream and return it.
	 */
	void writeSmallInteger(short s)
		throws IOException
	{
		dataOutputStream.writeShort(s);
	}

	/**
	 * routine to write a string from the input stream and return it.
	 */
	void writeString(String s)
		throws IOException
	{
		// disk and memory match: write the data
		int len = s.length();
		writeBigInteger(len);
		dataOutputStream.write(s.getBytes(), 0, len);
	}
}
