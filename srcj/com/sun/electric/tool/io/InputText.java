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

import java.io.IOException;
import java.io.File;
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

//#define REPORTRATE  100		/* number of elements between graphical update

//#define LONGJMPNOMEM         1		/* error: No Memory */
//#define LONGJMPMISCOLON      2		/* error: Missing colon */
//#define LONGJMPUKNKEY        3		/* error: Unknown keyword */
//#define LONGJMPEOF           4		/* error: EOF too soon */
//#define LONGJMPEOFVAR        5		/* error: EOF in variables */
//#define LONGJMPMISCOLVAR     6		/* error: Missing ':' in variable */
//#define LONGJMPMISOSBVAR     7		/* error: Missing '[' in variable */
//#define LONGJMPMISOSBVARARR  8		/* error: Missing '[' in variable array */
//#define LONGJMPSHORTARR      9		/* error: Short array specification */
//#define LONGJMPUKNNOPROTO   10		/* error: Unknown node prototype */
//#define LONGJMPMISVIEWABR   11		/* error: Missing view abbreviation */
//#define LONGJMPBADVIEWABR   12		/* error: View abbreviation bad */
//#define LONGJMPINCVIEWABR   13		/* error: View abbreviation inconsistent */
//#define LONGJMPSECCELLNAME  14		/* error: Second cell name */
//#define LONGJMPUKNTECH      15		/* error: Unknown technology */
//#define LONGJMPINVARCPROTO  16		/* error: Invalid arc prototype */
//#define LONGJMPUKNPORPROTO  17		/* error: Unknown port prototype name */


public class InputText extends Input
{
	// ------------------------- private data ----------------------------
	private int          cellCount;
	private int          textLevel;
	private int          bitCount;
	private int          mainCell;
	private int          curArcEnd;
	private int          nodeInstCount;
	private int          arcInstCount;
	private int          varAddr;
	private int          portProtoCount;
	private int          lineNumber;
	private int          curPortCount;
	private int          varPos;
	private int          fileLength;
	private int          filePosition;
	private int          curCellNumber;
	private String       version;
	private int          emajor, eminor, edetail;
	private NodeInst     curNodeInst;
	private ArcInst      curArcInst;
	private PortProto    curPortProto;
	private NodeProto    curNodeProto;
	private Technology   curTech;
	private PortProto [] portProtoList;		// list of portprotos for readin
	private NodeProto [] nodeProtoList;		// list of cells for readin
	private ArcInst []   arcList;			// list of arcs for readin
	private NodeInst []  nodeList;			// list of nodes for readin
//	private FILE        *textfilein;
	private int          nodeInstError;
	private int          portArcInstError;
	private int          portExpInstError;
	private int          portProtoError;
	private int          arcInstError;
	private int          geomError;
	private int          RTNodeError;
	private int          libraryError;
	private int          convertMosisCMOSTechnologies;

	// state of input (value of "textLevel")
	private static final int INLIB =       1;
	private static final int INCELL =      2;
	private static final int INPORTPROTO = 3;
	private static final int INNODEINST =  4;
	private static final int INPOR =       5;
	private static final int INARCINST =   6;
	private static final int INARCEND =    7;

	// state of variable reading (value of "varPos")
	private static final int INVTOOL =        1;
	private static final int INVTECHNOLOGY =  2;
	private static final int INVLIBRARY =     3;
	private static final int INVNODEPROTO =   4;
	private static final int INVNODEINST =    5;
	private static final int INVPORTARCINST = 6;
	private static final int INVPORTEXPINST = 7;
	private static final int INVPORTPROTO =   8;
	private static final int INVARCINST =     9;

/* input string buffer */
//static INTBIG io_maxinputline = 0;
//static CHAR *io_keyword;
//
//struct keyroutine
//{
//	CHAR *matchstring;
//	void (*routine)(void);
//};

	InputText()
	{
	}

	// ----------------------- public methods -------------------------------

	protected boolean ReadLib()
	{
		// mark all libraries as "not being read", but "wanted"
//		for(olib = el_curlib; olib != NOLIBRARY; olib = olib->nextlibrary)
//		{
//			olib->temp1 = 0;
//			olib->userbits &= ~UNWANTEDLIB;
//		}
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
	 * Routine to read the .elib file.
	 * Returns true on error.
	 */
	private boolean readTheLibrary()
		throws IOException
	{
		// update the library file name if the true location is different
//		if (estrcmp(filename, lib->libfile) != 0)
//			(void)reallocstring(&lib->libfile, filename, lib->cluster);

//		reportcount = filePosition = 0;
//		if (io_verbose < 0)
//		{
//			fileLength = filesize(textfilein);
//			if (fileLength > 0)
//			{
//				if (newprogress)
//				{
//					io_inputprogressdialog = DiaInitProgress(x_(""), _("Reading library"));
//					if (io_inputprogressdialog == 0)
//					{
//						xclose(textfilein);
//						return(TRUE);
//					}
//				}
//				infstr = initinfstr();
//				formatinfstr(infstr, _("Reading library %s"), lib->libname);
//				DiaSetCaptionProgress(io_inputprogressdialog, returninfstr(infstr));
//				DiaSetTextProgress(io_inputprogressdialog, _("Initializing..."));
//				DiaSetProgress(io_inputprogressdialog, 0, fileLength);
//			}
//		} else fileLength = 0;

		// the error recovery point
//		errcode = setjmp(io_filerror);
//		if (errcode != 0)
//		{
//			switch (errcode)
//			{
//				case LONGJMPNOMEM:        pp = _("No Memory");                        break;
//				case LONGJMPMISCOLON:     pp = _("Missing colon");                    break;
//				case LONGJMPUKNKEY:       pp = _("Unknown keyword");                  break;
//				case LONGJMPEOF:          pp = _("EOF too soon");                     break;
//				case LONGJMPEOFVAR:       pp = _("EOF in variables");                 break;
//				case LONGJMPMISCOLVAR:    pp = _("Missing ':' in variable");          break;
//				case LONGJMPMISOSBVAR:    pp = _("Missing '[' in variable");          break;
//				case LONGJMPMISOSBVARARR: pp = _("Missing '[' in variable array");    break;
//				case LONGJMPSHORTARR:     pp = _("Short array specification");        break;
//				case LONGJMPUKNNOPROTO:   pp = _("Unknown node prototype");           break;
//				case LONGJMPMISVIEWABR:   pp = _("Missing view abbreviation");        break;
//				case LONGJMPBADVIEWABR:   pp = _("View abbreviation bad");            break;
//				case LONGJMPINCVIEWABR:   pp = _("View abbreviation inconsistent");   break;
//				case LONGJMPSECCELLNAME:  pp = _("Second cell name");                 break;
//				case LONGJMPUKNTECH:      pp = _("Unknown technology");               break;
//				case LONGJMPINVARCPROTO:  pp = _("Invalid arc prototype");            break;
//				case LONGJMPUKNPORPROTO:  pp = _("Unknown port prototype name");      break;
//				default:                  pp = _("Unknown");                          break;
//			}
//			ttyputerr(_("Error: %s on line %ld, keyword '%s'"), pp,
//				lineNumber, io_keyword);
//			xclose(textfilein);
//			if (io_verbose < 0 && fileLength > 0 && io_inputprogressdialog != 0)
//			{
//				DiaDoneProgress(io_inputprogressdialog);
//				io_inputprogressdialog = 0;
//			}
//			return(TRUE);
//		}

		// force the library name to be the proper file name without the "txt"
//		infstr = initinfstr();
//		for(i=estrlen(lib->libfile)-1; i>=0; i--) if (lib->libfile[i] == DIRSEP) break;
//		pp = &lib->libfile[i+1];
//		j = estrlen(pp);
//		if (j > 4 && namesame(&pp[j-4], x_(".txt")) == 0) j -= 4;
//		for(i=0; i<j; i++) addtoinfstr(infstr, pp[i]);
//		if (reallocstring(&lib->libname, returninfstr(infstr), lib->cluster))
//			longjmp(io_filerror, LONGJMPNOMEM);

//		lib.erase();

		// clear error counters
		nodeInstError = portArcInstError = 0;
		portExpInstError = portProtoError = 0;
		arcInstError = geomError = 0;
		libraryError = RTNodeError = 0;

		textLevel = INLIB;
		lineNumber = 0;
		for(;;)
		{
			// get keyword from file
			if (io_getkeyword()) break;
//			pp = io_keyword;   while (*pp != 0) pp++;
//			if (pp[-1] != ':') longjmp(io_filerror, LONGJMPMISCOLON);
//			pp[-1] = 0;

//			if (reportcount++ > REPORTRATE)
//			{
//				reportcount = 0;
//				if (io_verbose < 0 && fileLength > 0 && io_inputprogressdialog != 0)
//					DiaSetProgress(io_inputprogressdialog, filePosition, fileLength);
//			}
String io_keyword = null;
			// determine which keyword table to use
			switch (textLevel)
			{
				case INLIB:
					if (io_keyword == "****library") {     io_newlib();   break;   }
					if (io_keyword == "bits") {            io_libbit();   break;   }
					if (io_keyword == "lambda") {          io_lambda();   break;   }
					if (io_keyword == "version") {         io_versn();    break;   }
					if (io_keyword == "aids") {            io_libkno();   break;   }
					if (io_keyword == "aidname") {         io_libain();   break;   }
					if (io_keyword == "aidbits") {         io_libaib();   break;   }
					if (io_keyword == "userbits") {        io_libusb();   break;   }
					if (io_keyword == "techcount") {       io_libte();    break;   }
					if (io_keyword == "techname") {        io_libten();   break;   }
					if (io_keyword == "cellcount") {       io_libcc();    break;   }
					if (io_keyword == "maincell") {        io_libms();    break;   }
					if (io_keyword == "view") {            io_libvie();   break;   }
					if (io_keyword == "***cell") {         io_newcel();   break;   }
					if (io_keyword == "variables") {       io_getvar();   break;   }
					 break;
				case INCELL:
					if (io_keyword == "bits") {            io_celbit();   break;   }
					if (io_keyword == "userbits") {        io_celusb();   break;   }
					if (io_keyword == "netnum") {          io_celnet();   break;   }
					if (io_keyword == "name") {            io_celnam();   break;   }
					if (io_keyword == "version") {         io_celver();   break;   }
					if (io_keyword == "creationdate") {    io_celcre();   break;   }
					if (io_keyword == "revisiondate") {    io_celrev();   break;   }
					if (io_keyword == "lowx") {            io_cellx();    break;   }
					if (io_keyword == "highx") {           io_celhx();    break;   }
					if (io_keyword == "lowy") {            io_celly();    break;   }
					if (io_keyword == "highy") {           io_celhy();    break;   }
					if (io_keyword == "externallibrary") { io_celext();   break;   }
					if (io_keyword == "aadirty") {         io_celaad();   break;   }
					if (io_keyword == "nodes") {           io_celnoc();   break;   }
					if (io_keyword == "arcs") {            io_celarc();   break;   }
					if (io_keyword == "porttypes") {       io_celptc();   break;   }
					if (io_keyword == "celldone") {        io_celdon();   break;   }
					if (io_keyword == "technology") {      io_tech();     break;   }
					if (io_keyword == "**node") {          io_newno();    break;   }
					if (io_keyword == "**arc") {           io_newar();    break;   }
					if (io_keyword == "***cell") {         io_newcel();   break;   }
					if (io_keyword == "variables") {       io_getvar();   break;   }
					break;
				case INPORTPROTO:
					if (io_keyword == "bits") {            io_ptbit();    break;   }
					if (io_keyword == "userbits") {        io_ptusb();    break;   }
					if (io_keyword == "netnum") {          io_ptnet();    break;   }
					if (io_keyword == "subnode") {         io_ptsno();    break;   }
					if (io_keyword == "subport") {         io_ptspt();    break;   }
					if (io_keyword == "name") {            io_ptnam();    break;   }
					if (io_keyword == "descript") {        io_ptdes();    break;   }
					if (io_keyword == "aseen") {           io_ptkse();    break;   }
					if (io_keyword == "**porttype") {      io_newpt();    break;   }
					if (io_keyword == "**arc") {           io_newar();    break;   }
					if (io_keyword == "**node") {          io_newno();    break;   }
					if (io_keyword == "celldone") {        io_celdon();   break;   }
					if (io_keyword == "***cell") {         io_newcel();   break;   }
					if (io_keyword == "variables") {       io_getvar();   break;   }
					if (io_keyword == "arcbits") {         io_null();     break;   }	// used no more: ignored for compatibility
					break;
				case INNODEINST:
					if (io_keyword == "bits") {            io_nodbit();   break;   }
					if (io_keyword == "userbits") {        io_nodusb();   break;   }
					if (io_keyword == "type") {            io_nodtyp();   break;   }
					if (io_keyword == "lowx") {            io_nodlx();    break;   }
					if (io_keyword == "highx") {           io_nodhx();    break;   }
					if (io_keyword == "lowy") {            io_nodly();    break;   }
					if (io_keyword == "highy") {           io_nodhy();    break;   }
					if (io_keyword == "rotation") {        io_nodrot();   break;   }
					if (io_keyword == "transpose") {       io_nodtra();   break;   }
					if (io_keyword == "aseen") {           io_nodkse();   break;   }
					if (io_keyword == "name") {            io_nodnam();   break;   }
					if (io_keyword == "descript") {        io_noddes();   break;   }
					if (io_keyword == "*port") {           io_newpor();   break;   }
					if (io_keyword == "**node") {          io_newno();    break;   }
					if (io_keyword == "**porttype") {      io_newpt();    break;   }
					if (io_keyword == "**arc") {           io_newar();    break;   }
					if (io_keyword == "celldone") {        io_celdon();   break;   }
					if (io_keyword == "variables") {       io_getvar();   break;   }
					if (io_keyword == "***cell") {         io_newcel();   break;   }
					if (io_keyword == "ports") {           io_nodpoc();   break;   }
					if (io_keyword == "exportcount") {     io_null();    break;   }	// used no more: ignored for compatibility
					break;
				case INPOR:
					if (io_keyword == "arc") {             io_porarc();   break;   }
					if (io_keyword == "exported") {        io_porexp();   break;   }
					if (io_keyword == "*port") {           io_newpor();   break;   }
					if (io_keyword == "**node") {          io_newno();    break;   }
					if (io_keyword == "**porttype") {      io_newpt();    break;   }
					if (io_keyword == "**arc") {           io_newar();    break;   }
					if (io_keyword == "variables") {       io_getvar();   break;   }
					if (io_keyword == "celldone") {        io_celdon();   break;   }
					if (io_keyword == "***cell") {         io_newcel();   break;   }
					break;
				case INARCINST:
					if (io_keyword == "bits") {            io_arcbit();   break;   }
					if (io_keyword == "userbits") {        io_arcusb();   break;   }
					if (io_keyword == "netnum") {          io_arcnet();   break;   }
					if (io_keyword == "type") {            io_arctyp();   break;   }
					if (io_keyword == "width") {           io_arcwid();   break;   }
					if (io_keyword == "length") {          io_arclen();   break;   }
					if (io_keyword == "signals") {         io_arcsig();   break;   }
					if (io_keyword == "aseen") {           io_arckse();   break;   }
					if (io_keyword == "name") {            io_arcnam();   break;   }
					if (io_keyword == "*end") {            io_newend();   break;   }
					if (io_keyword == "**arc") {           io_newar();    break;   }
					if (io_keyword == "**node") {          io_newno();    break;   }
					if (io_keyword == "variables") {       io_getvar();   break;   }
					if (io_keyword == "celldone") {        io_celdon();   break;   }
					if (io_keyword == "***cell") {         io_newcel();   break;   }
					break;
				case INARCEND:
					if (io_keyword == "node") {            io_endnod();   break;   }
					if (io_keyword == "nodeport") {        io_endpt();    break;   }
					if (io_keyword == "xpos") {            io_endxp();    break;   }
					if (io_keyword == "ypos") {            io_endyp();    break;   }
					if (io_keyword == "*end") {            io_newend();   break;   }
					if (io_keyword == "**arc") {           io_newar();    break;   }
					if (io_keyword == "**node") {          io_newno();    break;   }
					if (io_keyword == "variables") {       io_getvar();   break;   }
					if (io_keyword == "celldone") {        io_celdon();   break;   }
					if (io_keyword == "***cell") {         io_newcel();   break;   }
					break;
			}
		}
//		if (io_verbose < 0 && fileLength > 0 && io_inputprogressdialog != 0)
//		{
//			DiaSetProgress(io_inputprogressdialog, 999, 1000);
//			DiaSetTextProgress(io_inputprogressdialog, _("Cleaning up..."));
//		}

		// set the former version number on the library
//		nextchangequiet();
//		(void)setval((INTBIG)lib, VLIBRARY, x_("LIB_former_version"),
//			(INTBIG)version, VSTRING|VDONTSAVE);

		// fill in any lambda values that are not specified
//		oldunit = ((lib->userbits & LIBUNITS) >> LIBUNITSSH) << INTERNALUNITSSH;
//		if (oldunit == (el_units&INTERNALUNITS)) num = den = 1; else
//			db_getinternalunitscale(&num, &den, el_units, oldunit);
//		for(tech = el_technologies; tech != NOTECHNOLOGY; tech = tech->nexttechnology)
//		{
//			lambda = lib->lambda[tech->techindex];
//			if (lambda != 0) lambda = muldiv(lambda, den, num); else
//			{
//				lambda = el_curlib->lambda[tech->techindex];
//				if (lambda == 0) lambda = tech->deflambda;
//			}			
//			lib->lambda[tech->techindex] = lambda;
//		}

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

		// if converting MOSIS CMOS technologies, store lambda in the right place
//		if (convertMosisCMOSTechnologies != 0)
//			lib->lambda[mocmos_tech->techindex] =
//				lib->lambda[mocmossub_tech->techindex];

		// if this is to be the current library, adjust technologies
//		if (lib == el_curlib)
//			for(tech = el_technologies; tech != NOTECHNOLOGY; tech = tech->nexttechnology)
//				changetechnologylambda(tech, lib->lambda[tech->techindex]);

		// warn if the MOSIS CMOS technologies were converted
//		if (convertMosisCMOSTechnologies != 0)
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

		// print any variable-related error messages
//		if (nodeInstError != 0)
//			ttyputmsg(_("Warning: %ld invalid NODEINST pointers"), nodeInstError);
//		if (arcInstError != 0)
//			ttyputmsg(_("Warning: %ld invalid ARCINST pointers"), arcInstError);
//		if (portProtoError != 0)
//			ttyputmsg(_("Warning: %ld invalid PORTPROTO pointers"), portProtoError);
//		if (portArcInstError != 0)
//			ttyputmsg(_("Warning: %ld PORTARCINST pointers not restored"), portArcInstError);
//		if (portExpInstError != 0)
//			ttyputmsg(_("Warning: %ld PORTEXPINST pointers not restored"), portExpInstError);
//		if (geomError != 0)
//			ttyputmsg(_("Warning: %ld GEOM pointers not restored"), geomError);
//		if (RTNodeError != 0)
//			ttyputmsg(_("Warning: %ld RTNODE pointers not restored"), RTNodeError);
//		if (libraryError != 0)
//			ttyputmsg(_("Warning: %ld LIBRARY pointers not restored"), libraryError);

//		if (mainCell != -1 && mainCell < cellCount)
//			lib->curNodeProto = nodeProtoList[mainCell]; else
//				lib->curNodeProto = NONODEPROTO;
//		if (cellCount != 0) efree((CHAR *)nodeProtoList);
//		if (io_verbose < 0 && fileLength > 0 && newprogress && io_inputprogressdialog != 0)
//		{
//			DiaDoneProgress(io_inputprogressdialog);
//			io_inputprogressdialog = 0;
//		}

		// clean up the library
//		io_fixnewlib(lib, 0);
//		lib->userbits &= ~(LIBCHANGEDMAJOR | LIBCHANGEDMINOR);
//		efree(version);
		return false;
	}

	//******************* GENERAL PARSING ROUTINES ********************/

	boolean io_getkeyword()
	{
//		REGISTER INTBIG c, cindex, inquote;
//
//		// skip leading blanks
//		for(;;)
//		{
//			c = xgetc(textfilein);
//			filePosition++;
//			if (c == '\n')
//			{
//				lineNumber++;
//				if (io_verbose == 0 && lineNumber%1000 == 0)
//					ttyputmsg(_("%ld lines read"), lineNumber);
//				continue;
//			}
//			if (c != ' ') break;
//		}
//
//		// if the file ended, quit now
//		if (c == EOF) return(TRUE);
//
//		// collect the word
//		cindex = 0;
//		if (c == '"') inquote = 1; else inquote = 0;
//		io_keyword[cindex++] = (CHAR)c;
//		for(;;)
//		{
//			c = xgetc(textfilein);
//			if (c == EOF) return(TRUE);
//			filePosition++;
//			if (c == '\n' || (c == ' ' && inquote == 0)) break;
//			if (cindex >= io_maxinputline)
//			{
//				// try to allocate more space
//				if (io_grabbuffers(io_maxinputline*2)) break;
//			}
//			if (c == '"' && (cindex == 0 || io_keyword[cindex-1] != '^'))
//				inquote = 1 - inquote;
//			io_keyword[cindex++] = (CHAR)c;
//		}
//		if (c == '\n')
//		{
//			lineNumber++;
//			if (io_verbose == 0 && lineNumber%1000 == 0)
//				ttyputmsg(_("%ld lines read"), lineNumber);
//		}
//		io_keyword[cindex] = 0;
		return false;
	}

	/**
	 * helper routine to parse a port prototype name "line" that should be
	 * in node prototype "np".  The routine returns NOPORTPROTO if it cannot
	 * figure out what port this name refers to.
	 */
	PortProto io_getport(String line, NodeProto np)
	{
//		REGISTER PORTPROTO *pp;
//		REGISTER INTBIG i;
//
//		pp = getportproto(np, line);
//		if (pp != NOPORTPROTO) return(pp);
//
//		// convert special port names
//		pp = io_convertoldportname(line, np);
//		if (pp != NOPORTPROTO) return(pp);
//
//		// try to parse version 1 port names
//		if (version[0] == '1')
//		{
//			// see if database uses shortened name
//			for(pp = np->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//			{
//				i = estrlen(pp->protoname);
//				if (namesamen(line, pp->protoname, i) == 0) return(pp);
//			}
//
//			// see if the port name ends in a digit, fake with that
//			i = estrlen(line);
//			if (line[i-2] == '-' && line[i-1] >= '0' && line[i-1] <= '9')
//			{
//				i = (eatoi(&line[i-1])-1) / 3;
//				for(pp = np->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//					if (i-- == 0) return(pp);
//			}
//		}

		// sorry, cannot figure out what port prototype this is
		return null;
	}

	/**
	 * null routine for ignoring keywords
	 */
	void io_null() {}

	//******************* LIBRARY PARSING ROUTINES ********************/

	/**
	 * a new library is introduced (keyword "****library")
	 * This should be the first keyword in the file
	 */
	void io_newlib()
	{
//		REGISTER TECHNOLOGY *tech;
//
//		// set defaults
//		mainCell = -1;
//		(void)allocstring(&version, x_("1.00"), el_tempcluster);
//		varPos = INVTOOL;
//		curTech = NOTECHNOLOGY;
//		for(tech = el_technologies; tech != NOTECHNOLOGY; tech = tech->nexttechnology)
//			lib->lambda[tech->techindex] = 0;
//		textLevel = INLIB;
	}

	/**
	 * get the file's Electric version number (keyword "version")
	 */
	void io_versn()
	{
//		(void)reallocstring(&version, io_keyword, el_tempcluster);
//
//		// for versions before 6.03q, convert MOSIS CMOS technology names
//		parseelectricversion(version, &emajor, &eminor,
//			&edetail);
//		convertMosisCMOSTechnologies = 0;
//		if (emajor < 6 ||
//			(emajor == 6 && eminor < 3) ||
//			(emajor == 6 && eminor == 3 && edetail < 17))
//		{
//			if ((asktech(mocmossub_tech, x_("get-state"))&MOCMOSSUBNOCONV) == 0)
//				convertMosisCMOSTechnologies = 1;
//		}
//	#ifdef REPORTCONVERSION
//		ttyputmsg(x_("Library is version %s (%ld.%ld.%ld)"), version, emajor,
//			eminor, edetail);
//		if (convertMosisCMOSTechnologies != 0)
//			ttyputmsg(x_("   Converting MOSIS CMOS technologies (mocmossub => mocmos)"));
//	#endif
	}

	/**
	 * get the number of tools (keyword "aids")
	 */
	void io_libkno()
	{
		bitCount = 0;
	}

	/**
	 * get the name of the tool (keyword "aidname")
	 */
	void io_libain()
	{
//		REGISTER TOOL *tool;
//
//		tool = gettool(io_keyword);
//		if (tool == NOTOOL) bitCount = -1; else
//			bitCount = tool->toolindex + 1;
	}

	/**
	 * get the number of toolbits (keyword "aidbits")
	 */
	void io_libaib()
	{
		bitCount = 0;
	}

	/**
	 * get tool information for the library (keyword "bits")
	 */
	void io_libbit()
	{
//		if (bitCount == 0)
//			lib->userbits = eatoi(io_keyword);
//		bitCount++;
	}

	/**
	 * get the number of toolbits (keyword "userbits")
	 */
	void io_libusb()
	{
//		lib->userbits = eatoi(io_keyword);
//
//		// this library came as readable dump, so don't automatically save it to disk
//		lib->userbits &= ~READFROMDISK;
	}

	/**
	 * get the number of technologies (keyword "techcount")
	 */
	void io_libte()
	{
		varPos = INVTECHNOLOGY;
		bitCount = 0;
	}

	/**
	 * get the name of the technology (keyword "techname")
	 */
	void io_libten()
	{
//		REGISTER TECHNOLOGY *tech;
//
//		tech = gettechnology(io_keyword);
//		if (tech == NOTECHNOLOGY) bitCount = -1; else
//			bitCount = tech->techindex;
	}

	/**
	 * get lambda values for each technology in library (keyword "lambda")
	 */
	void io_lambda()
	{
//		REGISTER INTBIG lam;
//
//		if (bitCount >= el_maxtech ||
//			bitCount < 0) return;
//		lam = eatoi(io_keyword);
//
//		// for version 4.0 and earlier, scale lambda by 20
//		if (eatoi(version) <= 4) lam *= 20;
//		lib->lambda[bitCount++] = lam;
	}

	/**
	 * get variables on current object (keyword "variables")
	 */
	void io_getvar()
	{
//		REGISTER INTBIG i, j, count, type, len, naddr, ntype, thisnaddr, *store, key, ret;
//		REGISTER float *fstore;
//		REGISTER INTSML *sstore;
//		UINTBIG descript[TEXTDESCRIPTSIZE];
//		REGISTER CHAR *pt, *start, *varname;
//		REGISTER VARIABLE *var;
//		REGISTER TECHNOLOGY *t;
//		REGISTER void *infstr;
//
//		naddr = -1;
//		switch (varPos)
//		{
//			case INVTOOL:				// keyword applies to tools
//				if (bitCount < 0) break;
//				naddr = (INTBIG)&el_tools[bitCount-1];
//				ntype = VTOOL;
//				break;
//			case INVTECHNOLOGY:			// keyword applies to technologies
//				if (bitCount < 0) break;
//				for(t = el_technologies; t != NOTECHNOLOGY; t = t->nexttechnology)
//					if (t->techindex == bitCount-1) break;
//				naddr = (INTBIG)t;
//				ntype = VTECHNOLOGY;
//				break;
//			case INVLIBRARY:			// keyword applies to library
//				naddr = (INTBIG)lib;
//				ntype = VLIBRARY;
//				break;
//			case INVNODEPROTO:			// keyword applies to nodeproto
//				naddr = (INTBIG)curNodeProto;    ntype = VNODEPROTO;
//				break;
//			case INVNODEINST:			// keyword applies to nodeinst
//				naddr = (INTBIG)curNodeInst;    ntype = VNODEINST;
//				break;
//			case INVPORTARCINST:		// keyword applies to portarcinsts
//				naddr = (INTBIG)varAddr;    ntype = VPORTARCINST;
//				break;
//			case INVPORTEXPINST:		// keyword applies to portexpinsts
//				naddr = (INTBIG)varAddr;    ntype = VPORTEXPINST;
//				break;
//			case INVPORTPROTO:			// keyword applies to portproto
//				naddr = (INTBIG)curPortProto;    ntype = VPORTPROTO;
//				break;
//			case INVARCINST:			// keyword applies to arcinst
//				naddr = (INTBIG)curArcInst;    ntype = VARCINST;
//				break;
//		}
//
//		// find out how many variables to read
//		count = eatoi(io_keyword);
//		for(i=0; i<count; i++)
//		{
//			// read the first keyword with the name, type, and descriptor
//			if (io_getkeyword()) longjmp(io_filerror, LONGJMPEOFVAR);
//			if (io_keyword[estrlen(io_keyword)-1] != ':')
//				longjmp(io_filerror, LONGJMPMISCOLVAR);
//
//			// get the variable name
//			infstr = initinfstr();
//			for(pt = io_keyword; *pt != 0; pt++)
//			{
//				if (*pt == '^' && pt[1] != 0)
//				{
//					pt++;
//					addtoinfstr(infstr, *pt);
//				}
//				if (*pt == '(' || *pt == '[' || *pt == ':') break;
//				addtoinfstr(infstr, *pt);
//			}
//			varname = returninfstr(infstr);
//			key = makekey(varname);
//
//			// see if the variable is valid
//			thisnaddr = naddr;
//			if (isdeprecatedvariable(naddr, ntype, varname)) thisnaddr = -1;
//
//			// get optional length
//			if (*pt == '(')
//			{
//				len = myatoi(&pt[1]);
//				while (*pt != '[' && *pt != ':' && *pt != 0) pt++;
//			} else len = -1;
//
//			// get type
//			if (*pt != '[') longjmp(io_filerror, LONGJMPMISOSBVAR);
//			type = myatoi(&pt[1]);
//
//			// get the descriptor
//			while (*pt != ',' && *pt != ':' && *pt != 0) pt++;
//			TDCLEAR(descript);
//			if (*pt == ',')
//			{
//				descript[0] = myatoi(&pt[1]);
//				for(pt++; *pt != 0 && *pt != '/' && *pt != ']'; pt++) ;
//				if (*pt == '/')
//					descript[1] = myatoi(pt+1);
//			}
//
//			// get value
//			if (io_getkeyword()) longjmp(io_filerror, LONGJMPEOFVAR);
//			if ((type&VISARRAY) != 0)
//			{
//				if (len < 0)
//				{
//					len = (type&VLENGTH) >> VLENGTHSH;
//					store = emalloc((len * SIZEOFINTBIG), el_tempcluster);
//					if (store == 0) longjmp(io_filerror, LONGJMPNOMEM);
//					fstore = (float *)store;
//					sstore = (INTSML *)store;
//				} else
//				{
//					store = emalloc(((len+1) * SIZEOFINTBIG), el_tempcluster);
//					if (store == 0) longjmp(io_filerror, LONGJMPNOMEM);
//					fstore = (float *)store;
//					sstore = (INTSML *)store;
//					store[len] = -1;
//					if ((type&VTYPE) == VFLOAT) fstore[len] = castfloat(-1);
//					if ((type&VTYPE) == VSHORT) sstore[len] = -1;
//				}
//				pt = io_keyword;
//				if (*pt++ != '[')
//					longjmp(io_filerror, LONGJMPMISOSBVARARR);
//				for(j=0; j<len; j++)
//				{
//					// string arrays must be handled specially
//					if ((type&VTYPE) == VSTRING)
//					{
//						while (*pt != '"' && *pt != 0) pt++;
//						if (*pt != 0)
//						{
//							start = pt++;
//							for(;;)
//							{
//								if (pt[0] == '^' && pt[1] != 0)
//								{
//									pt += 2;
//									continue;
//								}
//								if (pt[0] == '"' || pt[0] == 0) break;
//								pt++;
//							}
//							if (*pt != 0) pt++;
//						}
//					} else
//					{
//						start = pt;
//						while (*pt != ',' && *pt != ']' && *pt != 0) pt++;
//					}
//					if (*pt == 0)
//						longjmp(io_filerror, LONGJMPSHORTARR);
//					*pt++ = 0;
//					if ((type&VTYPE) == VGENERAL)
//					{
//						if ((j&1) == 0)
//						{
//							store[j+1] = myatoi(start);
//						} else
//						{
//							store[j-1] = io_decode(start, store[j]);
//						}
//					} else
//					{
//						if ((type&VTYPE) == VFLOAT)
//						{
//							fstore[j] = castfloat(io_decode(start, type));
//						} else if ((type&VTYPE) == VSHORT)
//						{
//							sstore[j] = (INTSML)io_decode(start, type);
//						} else
//						{
//							store[j] = io_decode(start, type);
//						}
//					}
//				}
//				if (thisnaddr != -1)
//				{
//					nextchangequiet();
//					var = setvalkey(thisnaddr, ntype, key, (INTBIG)store, type);
//					if (var == NOVARIABLE) longjmp(io_filerror, LONGJMPNOMEM);
//					TDCOPY(var->textdescript, descript);
//
//					// handle updating of technology caches
//					if (ntype == VTECHNOLOGY)
//						changedtechnologyvariable(key);
//				}
//				efree((CHAR *)store);
//			} else
//			{
//				ret = io_decode(io_keyword, type);
//				if (thisnaddr != -1)
//				{
//					nextchangequiet();
//					var = setvalkey(thisnaddr, ntype, key, ret, type);
//					if (var == NOVARIABLE) longjmp(io_filerror, LONGJMPNOMEM);
//					TDCOPY(var->textdescript, descript);
//
//					// handle updating of technology caches
//					if (ntype == VTECHNOLOGY)
//						changedtechnologyvariable(key);
//				}
//			}
//		}
	}

	int io_decode(String name, int type)
	{
//		REGISTER INTBIG thistype, cindex;
//		REGISTER CHAR *out, *retur;
//		REGISTER NODEPROTO *np;
//
//		thistype = type;
//		if ((thistype&(VCODE1|VCODE2)) != 0) thistype = VSTRING;
//
//		switch (thistype&VTYPE)
//		{
//			case VINTEGER:
//			case VSHORT:
//			case VBOOLEAN:
//			case VFRACT:
//			case VADDRESS:
//				return(myatoi(name));
//			case VCHAR:
//				return((INTBIG)name);
//			case VSTRING:
//				if (*name == '"') name++;
//				retur = name;
//				for(out = name; *name != 0; name++)
//				{
//					if (*name == '^' && name[1] != 0)
//					{
//						name++;
//						*out++ = *name;
//						continue;
//					}
//					if (*name == '"') break;
//					*out++ = *name;
//				}
//				*out = 0;
//	#if 0 // Dump language code
//				if ((type&(VCODE1|VCODE2)) != 0)
//					printf("Code <%s>\n", retur);
//	#endif
//				return((INTBIG)retur);
//			case VFLOAT:
//			case VDOUBLE:
//				return(castint((float)eatof(name)));
//			case VNODEINST:
//				cindex = myatoi(name);
//				if (cindex >= 0 && cindex < nodeInstCount)
//					return((INTBIG)nodeList[cindex]);
//				nodeInstError++;
//				return(-1);
//			case VNODEPROTO:
//				cindex = myatoi(name);
//				if (cindex == -1) return(-1);
//
//				// see if there is a ":" in the type
//				for(out = name; *out != 0; out++) if (*out == ':') break;
//				if (*out == 0)
//				{
//					cindex = eatoi(name);
//					if (cindex == -1) return(-1);
//					return((INTBIG)nodeProtoList[cindex]);
//				}
//
//				// parse primitive nodeproto name
//				np = getnodeproto(name);
//				if (np == NONODEPROTO)
//					longjmp(io_filerror, LONGJMPUKNNOPROTO);
//				if (np->primindex == 0)
//					longjmp(io_filerror, LONGJMPUKNNOPROTO);
//				return((INTBIG)np);
//			case VPORTARCINST:
//				portArcInstError++;   break;
//			case VPORTEXPINST:
//				portExpInstError++;   break;
//			case VPORTPROTO:
//				cindex = myatoi(name);
//				if (cindex >= 0 && cindex < portProtoCount)
//					return((INTBIG)portProtoList[cindex]);
//				portProtoError++;
//				return(-1);
//			case VARCINST:
//				cindex = myatoi(name);
//				if (cindex >= 0 && cindex < arcInstCount)
//					return((INTBIG)arcList[cindex]);
//				arcInstError++;
//				return(-1);
//			case VARCPROTO:
//				return((INTBIG)getarcproto(name));
//			case VGEOM:
//				geomError++;   break;
//			case VLIBRARY:
//				libraryError++;   break;
//			case VTECHNOLOGY:
//				return((INTBIG)io_gettechnology(name));
//			case VTOOL:
//				return((INTBIG)gettool(name));
//			case VRTNODE:
//				RTNodeError++;   break;
//		}
		return(-1);
	}

	/**
	 * get the number of cells in this library (keyword "cellcount")
	 */
	void io_libcc()
	{
//		REGISTER INTBIG i;
//
//		varPos = INVLIBRARY;
//
//		cellCount = eatoi(io_keyword);
//		if (cellCount == 0)
//		{
//			lib->firstnodeproto = NONODEPROTO;
//			lib->tailnodeproto = NONODEPROTO;
//			lib->numnodeprotos = 0;
//			return;
//		}
//
//		// allocate a list of node prototypes for this library
//		nodeProtoList = (NODEPROTO **)emalloc(((sizeof (NODEPROTO *)) * cellCount),
//			el_tempcluster);
//		if (nodeProtoList == 0) longjmp(io_filerror, LONGJMPNOMEM);
//		for(i=0; i<cellCount; i++)
//		{
//			nodeProtoList[i] = allocnodeproto(lib->cluster);
//			if (nodeProtoList[i] == NONODEPROTO)
//				longjmp(io_filerror, LONGJMPNOMEM);
//			nodeProtoList[i]->cellview = el_unknownview;
//			nodeProtoList[i]->newestversion = nodeProtoList[i];
//			nodeProtoList[i]->primindex = 0;
//			nodeProtoList[i]->firstportproto = NOPORTPROTO;
//			nodeProtoList[i]->firstnodeinst = NONODEINST;
//			nodeProtoList[i]->firstarcinst = NOARCINST;
//		}
	}

	/**
	 * get the main cell of this library (keyword "maincell")
	 */
	void io_libms()
	{
//		mainCell = eatoi(io_keyword);
	}

	/**
	 * get a view (keyword "view")
	 */
	void io_libvie()
	{
//		REGISTER CHAR *pt;
//		REGISTER INTBIG len;
//		REGISTER VIEW *v;
//
//		pt = io_keyword;
//		while (*pt != 0 && *pt != '{') pt++;
//		if (*pt != '{') longjmp(io_filerror, LONGJMPMISVIEWABR);
//		*pt++ = 0;
//		len = estrlen(pt);
//		if (pt[len-1] != '}') longjmp(io_filerror, LONGJMPBADVIEWABR);
//		pt[len-1] = 0;
//		v = getview(io_keyword);
//		if (v == NOVIEW)
//		{
//			v = allocview();
//			if (v == NOVIEW) longjmp(io_filerror, LONGJMPNOMEM);
//			(void)allocstring(&v->viewname, io_keyword, db_cluster);
//			(void)allocstring(&v->sviewname, pt, db_cluster);
//			if (namesamen(io_keyword, x_("Schematic-Page-"), 15) == 0)
//				v->viewstate |= MULTIPAGEVIEW;
//			v->nextview = el_views;
//			el_views = v;
//		} else
//		{
//			if (namesame(v->sviewname, pt) != 0)
//				longjmp(io_filerror, LONGJMPINCVIEWABR);
//		}
//		pt[-1] = '{';   pt[len-1] = '}';
	}

	//******************** CELL PARSING ROUTINES ********************/

	/**
	 * initialize for a new cell (keyword "***cell")
	 */
	void io_newcel()
	{
//		REGISTER CHAR *pt;
//
//		curCellNumber = eatoi(io_keyword);
//		for(pt = io_keyword; *pt != 0; pt++) if (*pt == '/') break;
//		curNodeProto = nodeProtoList[curCellNumber];
//		if (*pt == '/') curNodeProto->temp1 = eatoi(pt+1); else
//			curNodeProto->temp1 = -1;
//		textLevel = INCELL;
//		varPos = INVNODEPROTO;
	}

	/**
	 * get the name of the current cell (keyword "name")
	 */
	void io_celnam()
	{
//		REGISTER CHAR *pt;
//		REGISTER VIEW *v;
//		CHAR *cellname;
//		REGISTER void *infstr;
//
//		if (io_verbose != 0)
//		{
//			if (io_verbose < 0 && fileLength > 0 && io_inputprogressdialog != 0)
//			{
//				infstr = initinfstr();
//				formatinfstr(infstr, _("Reading %s"), io_keyword);
//				DiaSetTextProgress(io_inputprogressdialog, returninfstr(infstr));
//			} else ttyputmsg(_("Reading %s"), io_keyword);
//		}
//		curNodeProto->cellview = el_unknownview;
//		pt = io_keyword;
//		while (*pt != 0 && *pt != '{') pt++;
//		if (*pt == '{')
//		{
//			*pt = 0;
//			if (allocstring(&cellname, io_keyword,
//				lib->cluster))
//					longjmp(io_filerror, LONGJMPNOMEM);
//			pt++;
//			pt[estrlen(pt)-1] = 0;
//			for(v = el_views; v != NOVIEW; v = v->nextview)
//				if (*v->sviewname != 0 && namesame(pt, v->sviewname) == 0) break;
//			if (v != NOVIEW) curNodeProto->cellview = v;
//		} else
//		{
//			if (allocstring(&cellname, io_keyword,
//				lib->cluster))
//					longjmp(io_filerror, LONGJMPNOMEM);
//		}
//		curNodeProto->lib = lib;
//
//		// copy cell information to the nodeproto
//		curNodeProto->lib = lib;
//		allocstring(&curNodeProto->protoname, cellname, lib->cluster);
//
//		db_insertnodeproto(curNodeProto);
	}

	/**
	 * get the version of the current cell (keyword "version")
	 */
	void io_celver()
	{
//		curNodeProto->version = eatoi(io_keyword);
	}

	/**
	 * get the creation date of the current cell (keyword "creationdate")
	 */
	void io_celcre()
	{
//		curNodeProto->creationdate = eatoi(io_keyword);
	}

	/**
	 * get the revision date of the current cell (keyword "revisiondate")
	 */
	void io_celrev()
	{
//		curNodeProto->revisiondate = eatoi(io_keyword);
	}

	/**
	 * get the external library file (keyword "externallibrary")
	 */
	void io_celext()
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
//		libfile = io_keyword;
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
	 * get the boundary of the current cell
	 */
	void io_cellx()
	{
//		curNodeProto->lowx = eatoi(io_keyword);
	}

	void io_celhx()
	{
//		curNodeProto->highx = eatoi(io_keyword);
	}

	void io_celly()
	{
//		curNodeProto->lowy = eatoi(io_keyword);
	}

	void io_celhy()
	{
//		curNodeProto->highy = eatoi(io_keyword);
	}

	/**
	 * get the default technology for objects in this cell (keyword "technology")
	 */
	void io_tech()
	{
//		REGISTER TECHNOLOGY *tech;
//
//		tech = io_gettechnology(io_keyword);
//		if (tech == NOTECHNOLOGY) longjmp(io_filerror, LONGJMPUKNTECH);
//		curTech = tech;
	}

	/**
	 * get the tool dirty word for the current cell (keyword "aadirty")
	 */
	void io_celaad()
	{
//		curNodeProto->adirty = eatoi(io_keyword);
//		bitCount = 0;
	}

	/**
	 * get tool information for current cell (keyword "bits")
	 */
	void io_celbit()
	{
//		if (bitCount == 0) curNodeProto->userbits = eatoi(io_keyword);
//		bitCount++;
	}

	/**
	 * get tool information for current cell (keyword "userbits")
	 */
	void io_celusb()
	{
//		curNodeProto->userbits = eatoi(io_keyword);
	}

	/**
	 * get tool information for current cell (keyword "netnum")
	 */
	void io_celnet() {}

	/**
	 * get the number of node instances in the current cell (keyword "nodes")
	 */
	void io_celnoc()
	{
//		REGISTER INTBIG i;
//		REGISTER NODEINST *ni;
//		REGISTER GEOM **geomlist;
//
//		nodeInstCount = eatoi(io_keyword);
//		if (nodeInstCount == 0) return;
//		nodeList = (NODEINST **)emalloc(((sizeof (NODEINST *)) * nodeInstCount),
//			el_tempcluster);
//		if (nodeList == 0) longjmp(io_filerror, LONGJMPNOMEM);
//		geomlist = (GEOM **)emalloc(((sizeof (GEOM *)) * nodeInstCount), el_tempcluster);
//		if (geomlist == 0) longjmp(io_filerror, LONGJMPNOMEM);
//		for(i=0; i<nodeInstCount; i++)
//		{
//			ni = allocnodeinst(lib->cluster);
//			geomlist[i] = allocgeom(lib->cluster);
//			if (ni == NONODEINST || geomlist[i] == NOGEOM)
//				longjmp(io_filerror, LONGJMPNOMEM);
//			nodeList[i] = ni;
//			ni->parent = curNodeProto;
//			ni->firstportarcinst = NOPORTARCINST;
//			ni->firstportexpinst = NOPORTEXPINST;
//			ni->geom = geomlist[i];
//
//			// compute linked list of nodes in this cell
//			if (curNodeProto->firstnodeinst != NONODEINST)
//				curNodeProto->firstnodeinst->prevnodeinst = ni;
//			ni->nextnodeinst = curNodeProto->firstnodeinst;
//			ni->prevnodeinst = NONODEINST;
//			curNodeProto->firstnodeinst = ni;
//		}
//		efree((CHAR *)geomlist);
	}

	/**
	 * get the number of arc instances in the current cell (keyword "arcs")
	 */
	void io_celarc()
	{
//		REGISTER INTBIG i;
//		REGISTER ARCINST *ai;
//		REGISTER GEOM **geomlist;
//
//		arcInstCount = eatoi(io_keyword);
//		if (arcInstCount == 0) return;
//		arcList = (ARCINST **)emalloc(((sizeof (ARCINST *)) * arcInstCount),
//			el_tempcluster);
//		if (arcList == 0) longjmp(io_filerror, LONGJMPNOMEM);
//		geomlist = (GEOM **)emalloc(((sizeof (GEOM *)) * arcInstCount), el_tempcluster);
//		if (geomlist == 0) longjmp(io_filerror, LONGJMPNOMEM);
//		for(i=0; i<arcInstCount; i++)
//		{
//			ai = allocarcinst(lib->cluster);
//			geomlist[i] = allocgeom(lib->cluster);
//			if (ai == NOARCINST || geomlist[i] == NOGEOM)
//				longjmp(io_filerror, LONGJMPNOMEM);
//			arcList[i] = ai;
//			ai->parent = curNodeProto;
//			ai->geom = geomlist[i];
//
//			// compute linked list of arcs in this cell
//			if (curNodeProto->firstarcinst != NOARCINST)
//				curNodeProto->firstarcinst->prevarcinst = ai;
//			ai->nextarcinst = curNodeProto->firstarcinst;
//			ai->prevarcinst = NOARCINST;
//			curNodeProto->firstarcinst = ai;
//		}
//		efree((CHAR *)geomlist);
	}

	/**
	 * get the number of port prototypes in the current cell (keyword "porttypes")
	 */
	void io_celptc()
	{
//		REGISTER INTBIG i;
//
//		portProtoCount = eatoi(io_keyword);
//		curNodeProto->numportprotos = portProtoCount;
//		if (portProtoCount == 0) return;
//		portProtoList = (PORTPROTO **)emalloc(((sizeof (PORTPROTO *)) *
//			portProtoCount), el_tempcluster);
//		if (portProtoList == 0) longjmp(io_filerror, LONGJMPNOMEM);
//		for(i=0; i<portProtoCount; i++)
//		{
//			portProtoList[i] = allocportproto(lib->cluster);
//			if (portProtoList[i] == NOPORTPROTO)
//				longjmp(io_filerror, LONGJMPNOMEM);
//			portProtoList[i]->parent = curNodeProto;
//		}
//
//		// link the portprotos
//		curNodeProto->firstportproto = portProtoList[0];
//		for(i=1; i<portProtoCount; i++)
//			portProtoList[i-1]->nextportproto = portProtoList[i];
//		portProtoList[portProtoCount-1]->nextportproto = NOPORTPROTO;
	}

	/**
	 * close the current cell (keyword "celldone")
	 */
	void io_celdon()
	{
//		REGISTER INTBIG i;
//		REGISTER NODEINST *ni;
//		REGISTER ARCINST *ai;
//		REGISTER PORTARCINST *pi;
//		REGISTER GEOM *geom;
//
//		// verify cell name
//		if (namesame(io_keyword, curNodeProto->protoname) != 0)
//			ttyputmsg(_("Warning: cell '%s' wants to be '%s'"),
//				curNodeProto->protoname, io_keyword);
//
//		// silly hack: convert arcinst->end->portarcinst pointers
//		for(i=0; i<nodeInstCount; i++)
//		{
//			ni = nodeList[i];
//			for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//			{
//				ai = pi->conarcinst;
//				if (ai->end[0].nodeinst != ni)
//				{
//					ai->end[1].portarcinst = pi;
//					continue;
//				}
//				if (ai->end[1].nodeinst != ni)
//				{
//					ai->end[0].portarcinst = pi;
//					continue;
//				}
//				if ((PORTPROTO *)ai->end[0].portarcinst == pi->proto) ai->end[0].portarcinst = pi;
//				if ((PORTPROTO *)ai->end[1].portarcinst == pi->proto) ai->end[1].portarcinst = pi;
//			}
//		}
//
//		// create geometry structure in the cell
//		if (geomstructure(curNodeProto))
//			longjmp(io_filerror, LONGJMPNOMEM);
//
//		// fill geometry module for each nodeinst
//		for(i=0; i<nodeInstCount; i++)
//		{
//			ni = nodeList[i];
//			geom = ni->geom;
//			geom->entryisnode = TRUE;  geom->entryaddr.ni = ni;
//			linkgeom(geom, curNodeProto);
//		}
//
//		// fill geometry modules for each arcinst
//		for(i=0; i<arcInstCount; i++)
//		{
//			ai = arcList[i];
//			(void)setshrinkvalue(ai, FALSE);
//			geom = ai->geom;
//			geom->entryisnode = FALSE;  geom->entryaddr.ai = ai;
//			linkgeom(geom, curNodeProto);
//		}
//
//		// free the lists of objects in this cell
//		if (nodeInstCount != 0) efree((CHAR *)nodeList);
//		if (portProtoCount != 0) efree((CHAR *)portProtoList);
//		if (arcInstCount != 0) efree((CHAR *)arcList);
	}

	//******************** NODE INSTANCE PARSING ROUTINES ********************/

	/**
	 * initialize for a new node instance (keyword "**node")
	 */
	void io_newno()
	{
//		curNodeInst = nodeList[eatoi(io_keyword)];
//		textLevel = INNODEINST;
//		varPos = INVNODEINST;
	}

	/**
	 * get the type of the current nodeinst (keyword "type")
	 */
	void io_nodtyp()
	{
//		REGISTER NODEPROTO *np;
//		REGISTER TECHNOLOGY *tech;
//		REGISTER CHAR *pt, *line;
//		CHAR orig[50];
//		static INTBIG orignodenamekey = 0;
//
//		line = io_keyword;
//		if (*line == '[')
//		{
//			curNodeInst->proto = nodeProtoList[eatoi(&line[1])];
//		} else
//		{
//			for(pt = line; *pt != 0; pt++) if (*pt == ':') break;
//			if (*pt == ':')
//			{
//				*pt = 0;
//				tech = io_gettechnology(line);
//				if (tech == NOTECHNOLOGY) longjmp(io_filerror, LONGJMPUKNTECH);
//				*pt++ = ':';
//				line = pt;
//			} else tech = curTech;
//			if (curTech == NOTECHNOLOGY) curTech = tech;
//			for(np = tech->firstnodeproto; np != NONODEPROTO;
//				np = np->nextnodeproto) if (namesame(np->protoname, line) == 0)
//			{
//				curNodeInst->proto = np;
//				return;
//			}
//
//			// convert "Active-Node" to "P-Active-Node" (MOSIS CMOS)
//			if (estrcmp(line, x_("Active-Node")) == 0)
//			{
//				for(np = tech->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//					if (estrcmp(np->protoname, x_("P-Active-Node")) == 0)
//				{
//					curNodeInst->proto = np;
//					return;
//				}
//			}
//
//			// convert "message" and "cell-center" nodes
//			curNodeInst->proto = io_convertoldprimitives(tech, line);
//			if (curNodeInst->proto == gen_invispinprim)
//			{
//				(void)estrcpy(orig, x_("artwork:"));
//				(void)estrcat(orig, line);
//				if (orignodenamekey == 0) orignodenamekey = makekey(x_("NODE_original_name"));
//				nextchangequiet();
//				(void)setvalkey((INTBIG)curNodeInst, VNODEINST,
//					orignodenamekey, (INTBIG)orig, VSTRING|VDONTSAVE);
//			}
//			if (curNodeInst->proto != NONODEPROTO) return;
//
//			longjmp(io_filerror, LONGJMPUKNNOPROTO);
//		}
	}

	/**
	 * get the bounding box information for the current node instance
	 */
	void io_nodlx()
	{
//		curNodeInst->lowx = eatoi(io_keyword);
	}

	void io_nodhx()
	{
//		curNodeInst->highx = eatoi(io_keyword);
	}

	void io_nodly()
	{
//		curNodeInst->lowy = eatoi(io_keyword);
	}

	void io_nodhy()
	{
//		curNodeInst->highy = eatoi(io_keyword);
	}

	/**
	 * get the instance name of the current node instance (keyword "name")
	 */
	void io_nodnam()
	{
//		nextchangequiet();
//		(void)setvalkey((INTBIG)curNodeInst, VNODEINST, el_node_name_key,
//			(INTBIG)io_keyword, VSTRING|VDISPLAY);
	}

	/**
	 * get the text descriptor of the current node instance (keyword "descript")
	 */
	void io_noddes()
	{
//		REGISTER CHAR *pt;
//
//		curNodeInst->textdescript[0] = eatoi(io_keyword);
//		for(pt = io_keyword; *pt != 0 && *pt != '/'; pt++) ;
//		if (*pt == 0)
//		{
//			curNodeInst->textdescript[1] = 0;
//		} else
//		{
//			curNodeInst->textdescript[1] = eatoi(pt+1);
//		}
	}

	/**
	 * get the rotation for the current nodeinst (keyword "rotation");
	 */
	void io_nodrot()
	{
//		curNodeInst->rotation = eatoi(io_keyword);
	}

	/**
	 * get the transposition for the current nodeinst (keyword "transpose")
	 */
	void io_nodtra()
	{
//		curNodeInst->transpose = eatoi(io_keyword);
	}

	/**
	 * get the tool seen bits for the current nodeinst (keyword "aseen")
	 */
	void io_nodkse()
	{
		bitCount = 0;
	}

	/**
	 * get the port count for the current nodeinst (keyword "ports")
	 */
	void io_nodpoc()
	{
//		curPortCount = eatoi(io_keyword);
	}

	/**
	 * get tool information for current nodeinst (keyword "bits")
	 */
	void io_nodbit()
	{
//		if (bitCount == 0) curNodeInst->userbits = eatoi(io_keyword);
//		bitCount++;
	}

	/**
	 * get tool information for current nodeinst (keyword "userbits")
	 */
	void io_nodusb()
	{
//		curNodeInst->userbits = eatoi(io_keyword);
	}

	/**
	 * initialize for a new portinst on the current nodeinst (keyword "*port")
	 */
	void io_newpor()
	{
//		REGISTER INTBIG i, cindex;
//		REGISTER PORTPROTO *pp;
//
//		if (version[0] == '1')
//		{
//			// version 1 files used an index here
//			cindex = eatoi(io_keyword);
//
//			// dividing is a heuristic used to decypher version 1 files
//			for(pp = curNodeInst->proto->firstportproto, i=0; pp != NOPORTPROTO;
//				pp = pp->nextportproto) i++;
//			if (i < curPortCount)
//			{
//				for(;;)
//				{
//					cindex /= 3;
//					if (cindex < i) break;
//				}
//			}
//
//			for(curPortProto = curNodeInst->proto->firstportproto, i=0;
//				curPortProto != NOPORTPROTO;
//					curPortProto = curPortProto->nextportproto, i++)
//						if (i == cindex) break;
//		} else
//		{
//			// version 2 and later use port prototype names
//			curPortProto = io_getport(io_keyword, curNodeInst->proto);
//		}
//		textLevel = INPOR;
	}

	/**
	 * get an arc connection for the current nodeinst (keyword "arc")
	 */
	void io_porarc()
	{
//		REGISTER PORTARCINST *pi;
//
//		pi = allocportarcinst(lib->cluster);
//		if (pi == NOPORTARCINST) longjmp(io_filerror, LONGJMPNOMEM);
//		pi->proto = curPortProto;
//		db_addportarcinst(curNodeInst, pi);
//		pi->conarcinst = arcList[eatoi(io_keyword)];
//		varPos = INVPORTARCINST;
//		varAddr = (INTBIG)pi;
	}

	/**
	 * get an export site for the current nodeinst (keyword "exported")
	 */
	void io_porexp()
	{
//		REGISTER PORTEXPINST *pe;
//
//		pe = allocportexpinst(lib->cluster);
//		if (pe == NOPORTEXPINST) longjmp(io_filerror, LONGJMPNOMEM);
//		pe->proto = curPortProto;
//		db_addportexpinst(curNodeInst, pe);
//		pe->exportproto = portProtoList[eatoi(io_keyword)];
//		varPos = INVPORTEXPINST;
//		varAddr = (INTBIG)pe;
	}

	//******************** ARC INSTANCE PARSING ROUTINES ********************/

	/**
	 * initialize for a new arc instance (keyword "**arc")
	 */
	void io_newar()
	{
//		curArcInst = arcList[eatoi(io_keyword)];
//		textLevel = INARCINST;
//		varPos = INVARCINST;
	}

	/**
	 * get the type of the current arc instance (keyword "type")
	 */
	void io_arctyp()
	{
//		REGISTER ARCPROTO *ap;
//		REGISTER CHAR *pt, *line;
//		REGISTER TECHNOLOGY *tech;
//
//		line = io_keyword;
//		for(pt = line; *pt != 0; pt++) if (*pt == ':') break;
//		if (*pt == ':')
//		{
//			*pt = 0;
//			tech = io_gettechnology(line);
//			if (tech == NOTECHNOLOGY) longjmp(io_filerror, LONGJMPUKNTECH);
//			*pt++ = ':';
//			line = pt;
//		} else tech = curTech;
//		if (curTech == NOTECHNOLOGY) curTech = tech;
//		for(ap = tech->firstarcproto; ap != NOARCPROTO; ap = ap->nextarcproto)
//			if (namesame(line, ap->protoname) == 0)
//		{
//			curArcInst->proto = ap;
//			return;
//		}
//
//		// convert old Artwork names
//		if (tech == art_tech)
//		{
//			if (estrcmp(line, x_("Dash-1")) == 0)
//			{
//				curArcInst->proto = art_dottedarc;
//				return;
//			}
//			if (estrcmp(line, x_("Dash-2")) == 0)
//			{
//				curArcInst->proto = art_dashedarc;
//				return;
//			}
//			if (estrcmp(line, x_("Dash-3")) == 0)
//			{
//				curArcInst->proto = art_thickerarc;
//				return;
//			}
//		}
//
//		// special hack: try the generic technology if name is not found
//		for(ap = gen_tech->firstarcproto; ap != NOARCPROTO; ap = ap->nextarcproto)
//			if (namesame(line, ap->protoname) == 0)
//		{
//			curArcInst->proto = ap;
//			return;
//		}
//
//		longjmp(io_filerror, LONGJMPINVARCPROTO);
	}

	/**
	 * get the instance name of the current arc instance (keyword "name")
	 */
	void io_arcnam()
	{
//		nextchangequiet();
//		(void)setvalkey((INTBIG)curArcInst, VARCINST, el_arc_name_key,
//			(INTBIG)io_keyword, VSTRING|VDISPLAY);
	}

	/**
	 * get the width of the current arc instance (keyword "width")
	 */
	void io_arcwid()
	{
//		curArcInst->width = eatoi(io_keyword);
	}

	/**
	 * get the length of the current arc instance (keyword "length")
	 */
	void io_arclen()
	{
//		curArcInst->length = eatoi(io_keyword);
	}

	/**
	 * get the signals information of the current arc instance (keyword "signals")
	 */
	void io_arcsig() {}

	/**
	 * initialize for an end of the current arcinst (keyword "*end")
	 */
	void io_newend()
	{
//		curArcEnd = eatoi(io_keyword);
//		textLevel = INARCEND;
	}

	/**
	 * get the node at the current end of the current arcinst (keyword "node")
	 */
	void io_endnod()
	{
//		curArcInst->end[curArcEnd].nodeinst = nodeList[eatoi(io_keyword)];
	}

	/**
	 * get the porttype at the current end of current arcinst (keyword "nodeport")
	 */
	void io_endpt()
	{
//		REGISTER PORTPROTO *pp;
//
//		pp = io_getport(io_keyword,curArcInst->end[curArcEnd].nodeinst->proto);
//		if (pp == NOPORTPROTO) longjmp(io_filerror, LONGJMPUKNPORPROTO);
//		curArcInst->end[curArcEnd].portarcinst = (PORTARCINST *)pp;
	}

	/**
	 * get the coordinates of the current end of the current arcinst
	 */
	void io_endxp()
	{
//		curArcInst->end[curArcEnd].xpos = eatoi(io_keyword);
	}

	void io_endyp()
	{
//		curArcInst->end[curArcEnd].ypos = eatoi(io_keyword);
	}

	/**
	 * get the tool information for the current arcinst (keyword "aseen")
	 */
	void io_arckse()
	{
		bitCount = 0;
	}

	/**
	 * get tool information for current arcinst (keyword "bits")
	 */
	void io_arcbit()
	{
//		if (bitCount == 0) curArcInst->userbits = eatoi(io_keyword);
//		bitCount++;
	}

	/**
	 * get tool information for current arcinst (keyword "userbits")
	 */
	void io_arcusb()
	{
//		curArcInst->userbits = eatoi(io_keyword);
	}

	/**
	 * get tool information for current arcinst (keyword "netnum")
	 */
	void io_arcnet() {}

	//******************** PORT PROTOTYPE PARSING ROUTINES ********************/

	/**
	 * initialize for a new port prototype (keyword "**porttype")
	 */
	void io_newpt()
	{
//		curPortProto = portProtoList[eatoi(io_keyword)];
//		textLevel = INPORTPROTO;
//		varPos = INVPORTPROTO;
	}

	/**
	 * get the name for the current port prototype (keyword "name")
	 */
	void io_ptnam()
	{
//		if (allocstring(&curPortProto->protoname, io_keyword, lib->cluster))
//			longjmp(io_filerror, LONGJMPNOMEM);
	}

	/**
	 * get the text descriptor for the current port prototype (keyword "descript")
	 */
	void io_ptdes()
	{
//		REGISTER CHAR *pt;
//
//		curPortProto->textdescript[0] = eatoi(io_keyword);
//		for(pt = io_keyword; *pt != 0 && *pt != '/'; pt++) ;
//		if (*pt == 0)
//		{
//			curPortProto->textdescript[1] = 0;
//		} else
//		{
//			curPortProto->textdescript[1] = eatoi(pt+1);
//		}
	}

	/**
	 * get the sub-nodeinst for the current port prototype (keyword "subnode")
	 */
	void io_ptsno()
	{
//		curPortProto->subnodeinst = nodeList[eatoi(io_keyword)];
	}

	/**
	 * get the sub-portproto for the current port prototype (keyword "subport")
	 */
	void io_ptspt()
	{
//		REGISTER PORTPROTO *pp;
//
//		pp = io_getport(io_keyword, curPortProto->subnodeinst->proto);
//		if (pp == NOPORTPROTO) longjmp(io_filerror, LONGJMPUKNPORPROTO);
//		curPortProto->subportproto = pp;
	}

	/**
	 * get the tool seen for the current port prototype (keyword "aseen")
	 */
	void io_ptkse()
	{
		bitCount = 0;
	}

	/**
	 * get the tool data for the current port prototype (keyword "bits")
	 */
	void io_ptbit()
	{
//		if (bitCount == 0) curPortProto->userbits = eatoi(io_keyword);
//		bitCount++;
	}

	/**
	 * get the tool data for the current port prototype (keyword "userbits")
	 */
	void io_ptusb()
	{
//		curPortProto->userbits = eatoi(io_keyword);
	}

	/**
	 * get the tool data for the current port prototype (keyword "netnum")
	 */
	void io_ptnet() {}

	/**
	 * routine to convert the technology name in "line" to a technology.
	 * also handles conversion of the old technology name "logic"
	 */
	Technology io_gettechnology(String line)
	{
//		REGISTER TECHNOLOGY *tech;
//
//		tech = NOTECHNOLOGY;
//		if (convertMosisCMOSTechnologies != 0)
//		{
//			if (namesame(line, x_("mocmossub")) == 0) tech = gettechnology(x_("mocmos")); else
//				if (namesame(line, x_("mocmos")) == 0) tech = gettechnology(x_("mocmosold"));
//		}
//		if (tech == NOTECHNOLOGY) tech = gettechnology(line);
//		if (tech != NOTECHNOLOGY) return(tech);
//		if (namesame(line, x_("logic")) == 0) tech = sch_tech;
//		return(tech);
return null;
	}

	/**
	 * Routine to free all memory associated with this module.
	 */
	void io_freetextinmemory()
	{
//		if (io_maxinputline != 0)
//			efree(io_keyword);
	}
}
