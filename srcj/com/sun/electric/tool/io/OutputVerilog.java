/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OutputVerilog.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.io.OutputTopology;
import com.sun.electric.tool.io.Output;

/**
 * This is the Simulation Interface tool.
 */
public class OutputVerilog extends OutputTopology
{

	public static boolean writeVerilogFile(Cell cell, String filePath)
	{
		boolean error = false;
System.out.println("can't write Verilog yet");
		OutputVerilog out = new OutputVerilog();
		if (out.openTextOutputStream(filePath)) error = true;
//		CIFVisitor visitor = out.makeCIFVisitor(getMaxHierDepth(cell));
//		if (out.writeCell(cell, visitor)) error = true;
		if (out.closeTextOutputStream()) error = true;
		if (!error) System.out.println(filePath + " written");
		return error;
	}

	/**
	 * Creates a new instance of Verilog
	 */
	OutputVerilog()
	{
	}

	protected void start()
	{
	}

	protected void done()
	{
	}

	/**
	 * Method to write cellGeom
	 */
	protected void writeCellGeom(CellGeom cellGeom)
	{
	}
	
	//#define IMPLICITINVERTERNODENAME x_("Imp")			/* name of inverters generated from negated wires */
//#define IMPLICITINVERTERSIGNAME  x_("ImpInv")		/* name of signals generated from negated wires */
//#define MAXDECLARATIONWIDTH      80					/* maximum size of output line */
//
//       INTBIG      sim_verilog_statekey;			/* key for "SIM_verilog_state" */
//static INTBIG      sim_verstate;
//static FILE       *sim_verfile;
//static INTBIG      sim_verilogcodekey = 0;
//static INTBIG      sim_verilogdeclarationkey = 0;
//static INTBIG      sim_verilogwiretypekey = 0;
//static INTBIG      sim_verilogtemplatekey = 0;		/* key for "ATTR_verilog_template" */
//static CHAR        sim_verdeclarationline[MAXDECLARATIONWIDTH];
//static INTBIG      sim_verdeclarationprefix;
//static CHAR      **sim_verilognamelist;
//static INTBIG      sim_verilognamelisttotal = 0;
//static INTBIG     *sim_verilognamelistlow;
//static INTBIG     *sim_verilognamelisthigh;
//static INTBIG     *sim_verilognamelisttempval;
//static INTBIG      sim_verilogglobalnetcount;		/* number of global nets */
//static INTBIG      sim_verilogglobalnettotal = 0;	/* size of global net array */
//static CHAR      **sim_verilogglobalnets;			/* global net names */
//static UINTBIG    *sim_verilogglobalchars;			/* global net characteristics */
//
//typedef struct
//{
//	NETWORK   *net;
//	PORTPROTO *pp;
//} WIRELIST;
//
//static WIRELIST   *sim_verilogwirelist;
//static INTBIG      sim_verilogwirelisttotal = 0;
//
///* verilog signal plotting */
//#define VERSIGNALSHOWN     1
//#define DEFIRSIMTIMERANGE  10.0E-9f			/* initial size of simulation window: 10ns */
//
//#define NOVERSIGNAL ((VERSIGNAL *)-1)
//
//typedef struct Iversignal
//{
//	CHAR               *symbol;
//	CHAR               *signalname;
//	CHAR               *signalcontext;
//	INTBIG              signal;			/* waveform window trace pointer */
//	INTBIG              flags;
//	INTBIG              level;
//	INTBIG              width;			/* bus width */
//	struct Iversignal **signals;		/* signals in the bus */
//	INTBIG              count;			/* number of stimuli on this signal */
//	INTBIG              total;			/* size of allocated array of stimuli on this signal */
//	double             *stimtime;		/* array of time values for stimuli on this signal */
//	INTSML             *stimstate;		/* state of signal for each stimuli */
//	struct Iversignal  *realversignal;	/* repeat signals point to the main entry */
//	struct Iversignal  *nextversignal;
//} VERSIGNAL;
//
//static VERSIGNAL  *sim_verfirstsignal = NOVERSIGNAL;
//static INTBIG      sim_verlineno;
//static CHAR        sim_verline[300];
//static CHAR        sim_vercurscope[1000];
//static FILE       *sim_verfd;
//static INTBIG      sim_vercurlevel;
//static INTBIG      sim_verfilesize;
//static void       *sim_verprogressdialog;		/* for showing input progress */
//static INTBIG      sim_numcharsused;			/* number of characters used in signal symbols */
//static INTBIG      sim_charused[256];			/* which characters are used in signal symbols */
//static double      sim_timescale;				/* scale of Verilog time to real time (in secs) */
//static INTBIG      sim_verignoresigname;		/* number of levels of verilog name to ignore */
//static VERSIGNAL  *sim_verwindow_iter;
//static VERSIGNAL  *sim_verwindow_lastiter;


	/*
	 * routine to write a ".v" file from the cell "np"
	 */
//	void sim_writevernetlist(NODEPROTO *np)
//	{
//		CHAR name[100], numberstring[100], *truename, *respar[2];
//		REGISTER NODEPROTO *lnp, *onp, *tnp;
//		REGISTER LIBRARY *lib, *olib;
//		REGISTER INTBIG i;
//		REGISTER VARIABLE *var;
//
//		// make sure network tool is on
//		if ((net_tool->toolstate&TOOLON) == 0)
//		{
//			ttyputerr(_("Network tool must be running...turning it on"));
//			toolturnon(net_tool);
//			ttyputerr(_("...now reissue the simulation command"));
//			return;
//		}
//		if (sim_verilogcodekey == 0)
//			sim_verilogcodekey = makekey(x_("VERILOG_code"));
//		if (sim_verilogdeclarationkey == 0)
//			sim_verilogdeclarationkey = makekey(x_("VERILOG_declaration"));
//		if (sim_verilogwiretypekey == 0)
//			sim_verilogwiretypekey = makekey(x_("SIM_verilog_wire_type"));
//		if (sim_verilogtemplatekey == 0)
//			sim_verilogtemplatekey = makekey(x_("ATTR_verilog_template"));
//
//		var = getvalkey((INTBIG)sim_tool, VTOOL, VINTEGER, sim_verilog_statekey);
//		if (var == NOVARIABLE) sim_verstate = 0; else
//			sim_verstate = var->addr;
//
//		// first write the "ver" file
//		(void)estrcpy(name, np->protoname);
//		(void)estrcat(name, x_(".v"));
//		sim_verfile = xcreate(name, sim_filetypeverilog, _("VERILOG File"), &truename);
//		if (sim_verfile == NULL)
//		{
//			if (truename != 0) ttyputerr(_("Cannot write %s"), truename);
//			return;
//		}
//
//		// see if there are any resistors in this circuit
//		if (hasresistors(np))
//		{
//			// has resistors: make sure they are being ignored
//			if (asktech(sch_tech, x_("ignoring-resistor-topology")) == 0)
//			{
//				// must redo network topology to ignore resistors
//				respar[0] = x_("resistors");
//				respar[1] = x_("ignore");
//				(void)telltool(net_tool, 2, respar);
//			}
//		}
//
//		// write header information
//		xprintf(sim_verfile, x_("/* Verilog for cell %s from Library %s */\n"),
//			describenodeproto(np), np->lib->libname);
//		us_emitcopyright(sim_verfile, x_("/* "), x_(" */"));
//		if ((us_useroptions&NODATEORVERSION) == 0)
//		{
//			if (np->creationdate)
//				xprintf(sim_verfile, x_("/* Created on %s */\n"),
//					timetostring((time_t)np->creationdate));
//			if (np->revisiondate)
//				xprintf(sim_verfile, x_("/* Last revised on %s */\n"),
//					timetostring((time_t)np->revisiondate));
//			(void)esnprintf(numberstring, 100, x_("%s"), timetostring(getcurrenttime()));
//			xprintf(sim_verfile, x_("/* Written on %s by Electric VLSI Design System, version %s */\n"),
//				numberstring, el_version);
//		} else
//		{
//			xprintf(sim_verfile, x_("/* Written by Electric VLSI Design System */\n"));
//		}
//
//		/*
//		 * determine whether any cells have name clashes in other libraries
//		 */
//		for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//			for(tnp = lib->firstnodeproto; tnp != NONODEPROTO; tnp = tnp->nextnodeproto)
//				tnp->temp2 = 0;
//		for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//		{
//			if ((lib->userbits&HIDDENLIBRARY) != 0) continue;
//			for(tnp = lib->firstnodeproto; tnp != NONODEPROTO; tnp = tnp->nextnodeproto)
//			{
//				for(olib = lib->nextlibrary; olib != NOLIBRARY; olib = olib->nextlibrary)
//				{
//					if ((olib->userbits&HIDDENLIBRARY) != 0) continue;
//					for(onp = olib->firstnodeproto; onp != NONODEPROTO; onp = onp->nextnodeproto)
//						if (namesame(tnp->protoname, onp->protoname) == 0) break;
//					if (onp != NONODEPROTO) {
//						tnp->temp2 = onp->temp2 = 1;
//					}
//				}
//			}
//		}
//
//		// gather all global signal names
//		for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//			for(onp = lib->firstnodeproto; onp != NONODEPROTO; onp = onp->nextnodeproto)
//				onp->temp1 = 0;
//		sim_verilogglobalnetcount = 0;
//		sim_vergatherglobals(np);
//		if (sim_verilogglobalnetcount > 0)
//		{
//			xprintf(sim_verfile, x_("\nmodule glbl();\n"));
//			for(i=0; i<sim_verilogglobalnetcount; i++)
//			{
//				if (sim_verilogglobalchars[i] == PWRPORT)
//				{
//					xprintf(sim_verfile, x_("    supply1 %s;\n"),
//						sim_verilogglobalnets[i]);
//				} else if (sim_verilogglobalchars[i] == GNDPORT)
//				{
//					xprintf(sim_verfile, x_("    supply0 %s;\n"),
//						sim_verilogglobalnets[i]);
//				} else if ((sim_verstate&VERILOGUSETRIREG) != 0)
//				{
//					xprintf(sim_verfile, x_("    trireg %s;\n"),
//						sim_verilogglobalnets[i]);
//				} else
//				{
//					xprintf(sim_verfile, x_("    wire %s;\n"),
//						sim_verilogglobalnets[i]);
//				}
//			}
//			xprintf(sim_verfile, x_("endmodule\n"));
//		}
//
//		// reset flags for cells that have been written
//		for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//			for(lnp = lib->firstnodeproto; lnp != NONODEPROTO; lnp = lnp->nextnodeproto)
//				lnp->temp1 = 0;
//		begintraversehierarchy();
//		initparameterizedcells();
//		if (sim_verwritecell(np, 0))
//			ttyputmsg(_("Back-annotation information has been added (library must be saved)"));
//		endtraversehierarchy();
//
//		// clean up
//		xclose(sim_verfile);
//		ttyputmsg(_("%s written"), truename);
//	}

	/*
	 * recursively called routine to print the Verilog description of cell "np".
	 */
//	BOOLEAN sim_verwritecell(NODEPROTO *np, CHAR *paramname)
//	{
//		REGISTER INTBIG i, j, k, l, nodetype, implicitports, total, localwires, nindex, wt,
//			impinv, nodewidth, isnegated, namecount, sigcount, wholenegated, dropbias,
//			low, high, addednames;
//		REGISTER BOOLEAN backannotate, first, dumpcell;
//		REGISTER NODEINST *ni;
//		REGISTER ARCINST *ai;
//		REGISTER PORTPROTO *pp, *lastpp, *opp, *cpp;
//		REGISTER NODEPROTO *onp, *cnp, *nip, *nipc;
//		REGISTER PORTARCINST *pi;
//		REGISTER PORTEXPINST *pe;
//		REGISTER NETWORK *net, *onet, *gnet, **outernetlist;
//		NETWORK *pwrnet, *gndnet, *pwrnetdummy, *gndnetdummy;
//		WIRELIST *wirelist;
//		INTBIG *lowindex, *highindex, *tempval, netcount, unconnectednet;
//		REGISTER VARIABLE *var, *vartemplate;
//		CHAR **namelist, *porttype, *thisline, *signame, impsigname[100], *startpt, line[200],
//			invsigname[100], *op, *pt, *opt, **ptr, *wiretype, save, osave, *pname, *modulename,
//			**strings, **nodenames, *nodename, *gn, *dir, *cellname;
//		REGISTER void *infstr;
//
//		// stop if requested
//		if (el_pleasestop != 0)
//		{
//			(void)stopping(STOPREASONDECK);
//			return(FALSE);
//		}
//		backannotate = FALSE;
//
//		// use attached file if specified
//		var = getval((INTBIG)np, VNODEPROTO, VSTRING, x_("SIM_verilog_behave_file"));
//		if (var != NOVARIABLE)
//		{
//			xprintf(sim_verfile, x_("`include \"%s\"\n"), (CHAR *)var->addr);
//
//			// mark this cell as written
//			np->temp1++;
//			return(backannotate);
//		}
//
//		// use library behavior if it is available
//		onp = anyview(np, el_verilogview);
//		if (onp != NONODEPROTO)
//		{
//			var = getvalkey((INTBIG)onp, VNODEPROTO, VSTRING|VISARRAY, el_cell_message_key);
//			if (var != NOVARIABLE)
//			{
//				l = getlength(var);
//				for(i=0; i<l; i++)
//				{
//					thisline = ((CHAR **)var->addr)[i];
//					xprintf(sim_verfile, x_("%s\n"), thisline);
//				}
//			}
//
//			// mark this cell as written
//			np->temp1++;
//			return(backannotate);
//		}
//
//		// make sure that all nodes and networks have names on them
//		addednames = 0;
//		if (asktool(net_tool, x_("name-nodes"), (INTBIG)np) != 0) addednames++;
//		if (asktool(net_tool, x_("name-nets"), (INTBIG)np) != 0) addednames++;
//		if (addednames != 0)
//		{
//			backannotate = TRUE;
//			net_endbatch();
//		}
//
//		// write netlist for this cell, first recurse on sub-cells
//		for(ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			if (ni->proto->primindex != 0) continue;
//
//			// ignore recursive references (showing icon in contents)
//			if (isiconof(ni->proto, np)) continue;
//
//			// see if this cell has a template
//			var = getvalkey((INTBIG)ni->proto, VNODEPROTO, VSTRING, sim_verilogtemplatekey);
//			if (var != NOVARIABLE) continue;
//
//			// get actual subcell (including contents/body distinction)
//			// NOTE: this gets the schematic before the layout
//			onp = contentsview(ni->proto);
//			if (onp == NONODEPROTO) onp = ni->proto; else
//			{
//				// see if this contents cell has a template
//				var = getvalkey((INTBIG)onp, VNODEPROTO, VSTRING, sim_verilogtemplatekey);
//				if (var != NOVARIABLE) continue;
//			}
//			dumpcell = TRUE;
//			if (onp->temp1 != 0) dumpcell = FALSE;
//			cellname = sim_vercellname(ni->proto);
//			pname = parameterizedname(ni, cellname);
//			if (pname != 0)
//			{
//				// do not force multiple writes if there is a behavioral file
//				var = getval((INTBIG)onp, VNODEPROTO, VSTRING, x_("SIM_verilog_behave_file"));
//				if (var == NOVARIABLE)
//				{
//					if (inparameterizedcells(onp, pname) == 0)
//					{
//						dumpcell = TRUE;
//						addtoparameterizedcells(onp, pname, 0);
//					}
//				}
//			}
//
//			// write the subcell
//			if (dumpcell)
//			{
//				downhierarchy(ni, onp, 0);
//				if (sim_verwritecell(onp, pname)) backannotate = TRUE;
//				uphierarchy();
//			}
//		}
//
//		// mark this cell as written
//		np->temp1++;
//
//		// prepare arcs to store implicit inverters
//		impinv = 1;
//		for(ai = np->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst)
//			ai->temp1 = 0;
//		for(ai = np->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst)
//		{
//			if ((ai->userbits&ISNEGATED) == 0) continue;
//			if ((ai->userbits&REVERSEEND) == 0)
//			{
//				ni = ai->end[0].nodeinst;
//				pi = ai->end[0].portarcinst;
//			} else
//			{
//				ni = ai->end[1].nodeinst;
//				pi = ai->end[1].portarcinst;
//			}
//			if (ni->proto == sch_bufprim || ni->proto == sch_andprim ||
//				ni->proto == sch_orprim || ni->proto == sch_xorprim)
//			{
//				if ((sim_verstate&VERILOGUSEASSIGN) != 0) continue;
//				if (estrcmp(pi->proto->protoname, x_("y")) == 0) continue;
//			}
//
//			// must create implicit inverter here
//			ai->temp1 = impinv;
//			if (ai->proto != sch_busarc) impinv++; else
//			{
//				net = ai->network;
//				if (net == NONETWORK) impinv++; else
//					impinv += net->buswidth;
//			}
//		}
//
//		// gather networks in the cell
//		namecount = sim_vergetnetworks(np, &namelist, &lowindex, &highindex,
//			&tempval, &wirelist, &netcount, &pwrnet, &gndnet, 0);
//
//		// write the module header
//		xprintf(sim_verfile, x_("\n"));
//		infstr = initinfstr();
//		if (paramname == 0) modulename = sim_vercellname(np); else
//			modulename = sim_verconvertname(paramname);
//		formatinfstr(infstr, x_("module %s("), modulename);
//		first = TRUE;
//		for(i=0; i<namecount; i++)
//		{
//			if (tempval[i] <= 1 || tempval[i] >= 6) continue;
//			if (!first) addstringtoinfstr(infstr, x_(", "));
//			addstringtoinfstr(infstr, namelist[i]);
//			first = FALSE;
//		}
//		addstringtoinfstr(infstr, x_(");"));
//		sim_verwritelongline(returninfstr(infstr));
//
//		// look for "wire/trireg" overrides
//		for(net = np->firstnetwork; net != NONETWORK; net = net->nextnetwork)
//			net->temp2 = 0;
//		for(ai = np->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst)
//		{
//			var = getvalkey((INTBIG)ai, VARCINST, VSTRING, sim_verilogwiretypekey);
//			if (var == NOVARIABLE) continue;
//			if (namesame((CHAR *)var->addr, x_("wire")) == 0) ai->network->temp2 = 1; else
//				if (namesame((CHAR *)var->addr, x_("trireg")) == 0) ai->network->temp2 = 2;
//		}
//
//		// write description of formal parameters to module
//		first = TRUE;
//		j = 0;
//		for(i=0; i<namecount; i++)
//		{
//			if (tempval[i] > 1 && tempval[i] < 6)
//			{
//				switch (tempval[i])
//				{
//					case 2: case 3: porttype = x_("input");   break;
//					case 4: case 5: porttype = x_("output");  break;
//				}
//				xprintf(sim_verfile, x_("  %s"), porttype);
//				if (lowindex[i] > highindex[i])
//				{
//					xprintf(sim_verfile, x_(" %s;"), namelist[i]);
//					if (wirelist[j].net->temp2 != 0)
//					{
//						if (wirelist[j].net->temp2 == 1) xprintf(sim_verfile, x_("  wire")); else
//							xprintf(sim_verfile, x_("  trireg"));
//						xprintf(sim_verfile, x_(" %s;"), namelist[i]);
//					}
//				} else
//				{
//					if ((tempval[i]&1) != 0)
//					{
//						low = highindex[i];   high = lowindex[i];
//					} else
//					{
//						low = lowindex[i];   high = highindex[i];
//					}
//					xprintf(sim_verfile, x_(" [%ld:%ld] %s;"), low, high, namelist[i]);
//					if (wirelist[j].net->temp2 != 0)
//					{
//						if (wirelist[j].net->temp2 == 1) xprintf(sim_verfile, x_("  wire")); else
//							xprintf(sim_verfile, x_("  trireg"));
//						xprintf(sim_verfile, x_(" [%ld:%ld] %s;"), low, high, namelist[i]);
//					}
//				}
//				xprintf(sim_verfile, x_("\n"));
//				first = FALSE;
//			}
//
//			// advance pointer to network information
//			if (lowindex[i] <= highindex[i]) j += highindex[i] - lowindex[i];
//			j++;
//		}
//		if (!first) xprintf(sim_verfile, x_("\n"));
//
//		// describe power and ground nets
//		if (pwrnet != NONETWORK) xprintf(sim_verfile, x_("  supply1 vdd;\n"));
//		if (gndnet != NONETWORK) xprintf(sim_verfile, x_("  supply0 gnd;\n"));
//
//		// determine whether to use "wire" or "trireg" for networks
//		if ((sim_verstate&VERILOGUSETRIREG) != 0) wiretype = x_("trireg"); else
//			wiretype = x_("wire");
//
//		// write "wire/trireg" declarations for internal single-wide signals
//		localwires = 0;
//		for(wt=0; wt<2; wt++)
//		{
//			first = TRUE;
//			j = 0;
//			for(i=0; i<namecount; i++)
//			{
//				if (tempval[i] <= 1 && lowindex[i] > highindex[i])
//				{
//					if (wirelist[j].net->temp2 == 0) estrcpy(impsigname, wiretype); else
//					{
//						if (wirelist[j].net->temp2 == 1) estrcpy(impsigname, x_("wire")); else
//							estrcpy(impsigname, x_("trireg"));
//					}
//					if ((wt == 0) ^ (namesame(wiretype, impsigname) == 0))
//					{
//						if (first)
//						{
//							esnprintf(invsigname, 100, x_("  %s"), impsigname);
//							sim_verinitdeclaration(invsigname);
//						}
//						sim_veradddeclaration(namelist[i]);
//						localwires++;
//						first = FALSE;
//					}
//				}
//
//				// advance pointer to network information
//				if (lowindex[i] <= highindex[i]) j += highindex[i] - lowindex[i];
//				j++;
//			}
//			if (!first) sim_vertermdeclaration();
//		}
//
//		// write "wire/trireg" declarations for internal busses
//		for(i=0; i<namecount; i++)
//		{
//			if (tempval[i] > 1) continue;
//			if (lowindex[i] > highindex[i]) continue;
//
//			if ((tempval[i]&1) != 0)
//			{
//				xprintf(sim_verfile, x_("  %s [%ld:%ld] %s;\n"), wiretype,
//					highindex[i], lowindex[i], namelist[i]);
//			} else
//			{
//				xprintf(sim_verfile, x_("  %s [%ld:%ld] %s;\n"), wiretype,
//					lowindex[i], highindex[i], namelist[i]);
//			}
//			localwires++;
//		}
//		if (localwires != 0) xprintf(sim_verfile, x_("\n"));
//
//		// add "wire" declarations for implicit inverters
//		if (impinv > 1)
//		{
//			esnprintf(invsigname, 100, x_("  %s"), wiretype);
//			sim_verinitdeclaration(invsigname);
//			for(i=1; i<impinv; i++)
//			{
//				esnprintf(impsigname, 100, x_("%s%ld"), IMPLICITINVERTERSIGNAME, i);
//				sim_veradddeclaration(impsigname);
//			}
//			sim_vertermdeclaration();
//		}
//
//		// add in any user-specified declarations and code
//		first = sim_verincludetypedcode(np, sim_verilogdeclarationkey, x_("declarations"));
//		first |= sim_verincludetypedcode(np, sim_verilogcodekey, x_("code"));
//		if (!first)
//			xprintf(sim_verfile, x_("  /* automatically generated Verilog */\n"));
//
//		// load Verilog names onto the networks
//		for(net = np->firstnetwork; net != NONETWORK; net = net->nextnetwork)
//			net->temp2 = 0;
//		j = 0;
//		for(i=0; i<namecount; i++)
//		{
//			if (lowindex[i] > highindex[i])
//			{
//				net = wirelist[j++].net;
//				ptr = (CHAR **)(&net->temp2);
//				if (net->globalnet > 1 && net->globalnet < np->globalnetcount)
//				{
//					gn = sim_verconvertname(np->globalnetnames[net->globalnet]);
//					*ptr = (CHAR *)emalloc((estrlen(gn)+7) * SIZEOFCHAR, sim_tool->cluster);
//					estrcpy(*ptr, x_(" glbl."));
//					estrcat(*ptr, gn);
//				} else
//				{
//					*ptr = (CHAR *)emalloc((estrlen(namelist[i])+2) * SIZEOFCHAR, sim_tool->cluster);
//					estrcpy(*ptr, x_(" "));
//					estrcat(*ptr, namelist[i]);
//				}
//			} else
//			{
//				if ((tempval[i]&1) != 0) dir = x_("D"); else dir = x_("U");
//				for(k=lowindex[i]; k<=highindex[i]; k++)
//				{
//					net = wirelist[j++].net;
//					infstr = initinfstr();
//					formatinfstr(infstr, x_("%s%s[%ld]"), dir, namelist[i], k);
//					ptr = (CHAR **)(&net->temp2);
//					(void)allocstring(ptr, returninfstr(infstr), sim_tool->cluster);
//				}
//			}
//		}
//
//		// look at every node in this cell
//		unconnectednet = 1;
//		for(ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			// not interested in passive nodes (ports electrically connected)
//			if (ni->proto->primindex != 0)
//			{
//				j = 0;
//				lastpp = ni->proto->firstportproto;
//				if (lastpp == NOPORTPROTO) continue;
//				for(pp = lastpp->nextportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//					if (pp->network != lastpp->network) j++;
//				if (j == 0) continue;
//			}
//
//			nodewidth = ni->arraysize;
//			if (nodewidth < 1) nodewidth = 1;
//
//			nodetype = nodefunction(ni);
//
//			// special case: verilog should ignore R L C etc.
//			if (nodetype == NPRESIST || nodetype == NPCAPAC || nodetype == NPECAPAC ||
//				nodetype == NPINDUCT) continue;
//
//			// use "assign" statement if possible
//			if ((sim_verstate&VERILOGUSEASSIGN) != 0)
//			{
//				if (nodetype == NPGATEAND || nodetype == NPGATEOR ||
//					nodetype == NPGATEXOR || nodetype == NPBUFFER)
//				{
//					// assign possible: determine operator
//					switch (nodetype)
//					{
//						case NPGATEAND:  op = x_(" & ");   break;
//						case NPGATEOR:   op = x_(" | ");   break;
//						case NPGATEXOR:  op = x_(" ^ ");   break;
//						case NPBUFFER:   op = x_("");      break;
//					}
//					for(nindex=0; nindex<nodewidth; nindex++)
//					{
//						// write a line describing this signal
//						infstr = initinfstr();
//						wholenegated = 0;
//						first = TRUE;
//						for(i=0; i<2; i++)
//						{
//							for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//							{
//								if (i == 0)
//								{
//									if (estrcmp(pi->proto->protoname, x_("y")) != 0) continue;
//								} else
//								{
//									if (estrcmp(pi->proto->protoname, x_("a")) != 0) continue;
//								}
//
//								// determine the network name at this port
//								net = pi->conarcinst->network;
//								if (nodewidth > 1)
//								{
//									(void)net_evalbusname(
//										(pi->conarcinst->proto->userbits&AFUNCTION)>>AFUNCTIONSH,
//											networkname(net, 0), &strings, pi->conarcinst, np, 1);
//									estrcpy(impsigname, strings[nindex]);
//									signame = impsigname;
//								} else
//								{
//									if (net != NONETWORK && net->namecount > 0)
//										signame = networkname(net, 0); else
//											signame = describenetwork(net);
//									if (net == pwrnet) signame = x_("vdd"); else
//										if (net == gndnet) signame = x_("gnd");
//								}
//
//								// see if this end is negated
//								isnegated = 0;
//								ai = pi->conarcinst;
//								if ((ai->userbits&ISNEGATED) != 0)
//								{
//									if ((ai->end[0].nodeinst == ni && (ai->userbits&REVERSEEND) == 0) ||
//										(ai->end[1].nodeinst == ni && (ai->userbits&REVERSEEND) != 0))
//									{
//										isnegated = 1;
//									}
//								}
//
//								// write the port name
//								if (i == 0)
//								{
//									// got the output port: do the left-side of the "assign"
//									addstringtoinfstr(infstr, x_("assign "));
//									addstringtoinfstr(infstr, signame);
//									addstringtoinfstr(infstr, x_(" = "));
//									if (isnegated != 0)
//									{
//										addstringtoinfstr(infstr, x_("~("));
//										wholenegated = 1;
//									}
//									break;
//								} else
//								{
//									if (!first)
//										addstringtoinfstr(infstr, op);
//									first = FALSE;
//									if (isnegated != 0) addstringtoinfstr(infstr, x_("~"));
//									addstringtoinfstr(infstr, signame);
//								}
//							}
//						}
//						if (wholenegated != 0)
//							addstringtoinfstr(infstr, x_(")"));
//						addstringtoinfstr(infstr, x_(";"));
//						sim_verwritelongline(returninfstr(infstr));
//					}
//					continue;
//				}
//			}
//
//			// get the name of the node
//			implicitports = 0;
//			dropbias = 0;
//			if (ni->proto->primindex == 0)
//			{
//				// ignore recursive references (showing icon in contents)
//				if (isiconof(ni->proto, np)) continue;
//				cellname = sim_vercellname(ni->proto);
//				pname = parameterizedname(ni, cellname);
//				if (pname == 0) pname = cellname; else
//					pname = sim_verconvertname(pname);
//				(void)allocstring(&nodename, pname, sim_tool->cluster);
//			} else
//			{
//				pt = ni->proto->protoname;
//	#if 1		// convert 4-port transistors to 3-port
//				switch (nodetype)
//				{
//					case NPTRA4NMOS: nodetype = NPTRANMOS;  dropbias = 1;   break;
//					case NPTRA4PMOS: nodetype = NPTRAPMOS;  dropbias = 1;   break;
//				}
//	#endif
//				switch (nodetype)
//				{
//					case NPTRANMOS:
//						implicitports = 2;
//						pt = x_("tranif1");
//						var = getvalkey((INTBIG)ni, VNODEINST, -1, sim_weaknodekey);
//						if (var != NOVARIABLE) pt = x_("rtranif1");
//						break;
//					case NPTRAPMOS:
//						implicitports = 2;
//						pt = x_("tranif0");
//						var = getvalkey((INTBIG)ni, VNODEINST, -1, sim_weaknodekey);
//						if (var != NOVARIABLE) pt = x_("rtranif0");
//						break;
//					case NPGATEAND:
//						implicitports = 1;
//						pt = x_("and");
//						for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//							if (estrcmp(pi->proto->protoname, x_("y")) == 0) break;
//						if (pi != NOPORTARCINST && (pi->conarcinst->userbits&ISNEGATED) != 0)
//							pt = x_("nand");
//						break;
//					case NPGATEOR:
//						implicitports = 1;
//						pt = x_("or");
//						for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//							if (estrcmp(pi->proto->protoname, x_("y")) == 0) break;
//						if (pi != NOPORTARCINST && (pi->conarcinst->userbits&ISNEGATED) != 0)
//							pt = x_("nor");
//						break;
//					case NPGATEXOR:
//						implicitports = 1;
//						pt = x_("xor");
//						for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//							if (estrcmp(pi->proto->protoname, x_("y")) == 0) break;
//						if (pi != NOPORTARCINST && (pi->conarcinst->userbits&ISNEGATED) != 0)
//							pt = x_("xnor");
//						break;
//					case NPBUFFER:
//						implicitports = 1;
//						pt = x_("buf");
//						for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//							if (estrcmp(pi->proto->protoname, x_("y")) == 0) break;
//						if (pi != NOPORTARCINST && (pi->conarcinst->userbits&ISNEGATED) != 0)
//							pt = x_("not");
//						break;
//				}
//				(void)allocstring(&nodename, pt, sim_tool->cluster);
//			}
//
//			if (nodewidth > 1)
//			{
//				var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, el_node_name_key);
//				if (var == NOVARIABLE) nodewidth = 1; else
//				{
//					sigcount = net_evalbusname(APBUS, (CHAR *)var->addr, &nodenames,
//						NOARCINST, NONODEPROTO, 0);
//					if (sigcount != nodewidth) nodewidth = 1;
//				}
//			}
//
//			// write the node (may be arrayed)
//			for(nindex=0; nindex<nodewidth; nindex++)
//			{
//				// look for a Verilog template on the prototype
//				vartemplate = getvalkey((INTBIG)ni->proto, VNODEPROTO, VSTRING, sim_verilogtemplatekey);
//				if (vartemplate == NOVARIABLE)
//				{
//					cnp = contentsview(ni->proto);
//					if (cnp != NONODEPROTO)
//						vartemplate = getvalkey((INTBIG)cnp, VNODEPROTO, VSTRING, sim_verilogtemplatekey);
//				}
//
//				if (vartemplate == NOVARIABLE)
//				{
//					// write the type of the node
//					infstr = initinfstr();
//					addstringtoinfstr(infstr, x_("  "));
//					addstringtoinfstr(infstr, nodename);
//
//					// write the name of the node
//					if (nodewidth > 1)
//					{
//						addstringtoinfstr(infstr, x_(" "));
//						addstringtoinfstr(infstr, sim_vernamenoindices(nodenames[nindex]));
//					} else
//					{
//						var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, el_node_name_key);
//						if (var != NOVARIABLE)
//						{
//							addstringtoinfstr(infstr, x_(" "));
//							addstringtoinfstr(infstr, sim_vernamenoindices((CHAR *)var->addr));
//						}
//					}
//					addstringtoinfstr(infstr, x_("("));
//				}
//
//				// write the rest of the ports
//				first = TRUE;
//				switch (implicitports)
//				{
//					case 0:		// explicit ports
//						cnp = contentsview(ni->proto);
//						if (cnp == NONODEPROTO) cnp = ni->proto;
//						for(net = cnp->firstnetwork; net != NONETWORK; net = net->nextnetwork)
//							net->temp2 = (INTBIG)NONETWORK;
//						cpp = NOPORTPROTO;
//						for(pp = ni->proto->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//						{
//							cpp = equivalentport(ni->proto, pp, cnp);
//							if (cpp == NOPORTPROTO) continue;
//							net = sim_vergetnetonport(ni, pp);
//							if (net != NONETWORK && net->buswidth > 1)
//							{
//								sigcount = net->buswidth;
//								if (nodewidth > 1 && cpp->network->buswidth * nodewidth == net->buswidth)
//								{
//									// map wide bus to a particular instantiation of an arrayed node
//									if (cpp->network->buswidth == 1)
//									{
//										onet = (NETWORK *)cpp->network->temp2;
//										if (onet == NONETWORK)
//											cpp->network->temp2 = (INTBIG)net->networklist[nindex];
//									} else
//									{
//										for(i=0; i<cpp->network->buswidth; i++)
//										{
//											onet = (NETWORK *)cpp->network->networklist[i]->temp2;
//											if (onet == NONETWORK)
//												cpp->network->networklist[i]->temp2 =
//													(INTBIG)net->networklist[i + nindex*cpp->network->buswidth];
//										}
//									}
//								} else
//								{
//									if (cpp->network->buswidth != net->buswidth)
//									{
//										ttyputerr(_("***ERROR: port %s on node %s in cell %s is %d wide, but is connected/exported with width %d"),
//											pp->protoname, describenodeinst(ni), describenodeproto(np),
//												cpp->network->buswidth, net->buswidth);
//										sigcount = mini(sigcount, cpp->network->buswidth);
//										if (sigcount == 1) sigcount = 0;
//									}
//									onet = (NETWORK *)cpp->network->temp2;
//									if (onet == NONETWORK) cpp->network->temp2 = (INTBIG)net;
//									for(i=0; i<sigcount; i++)
//									{
//										onet = (NETWORK *)cpp->network->networklist[i]->temp2;
//										if (onet == NONETWORK)
//											cpp->network->networklist[i]->temp2 = (INTBIG)net->networklist[i];
//									}
//								}
//							} else
//							{
//								onet = (NETWORK *)cpp->network->temp2;
//								if (onet == NONETWORK) cpp->network->temp2 = (INTBIG)net;
//							}
//						}
//
//						// special case for Verilog templates
//						if (vartemplate != NOVARIABLE)
//						{
//							infstr = initinfstr();
//							addstringtoinfstr(infstr, x_("  "));
//							for(pt = (CHAR *)vartemplate->addr; *pt != 0; pt++)
//							{
//								if (pt[0] != '$' || pt[1] != '(')
//								{
//									addtoinfstr(infstr, *pt);
//									continue;
//								}
//								startpt = pt + 2;
//								for(pt = startpt; *pt != 0; pt++)
//									if (*pt == ')') break;
//								save = *pt;
//								*pt = 0;
//								pp = getportproto(ni->proto, startpt);
//								if (pp != NOPORTPROTO)
//								{
//									// port name found: use its verilog node
//									net = sim_vergetnetonport(ni, pp);
//									if (net == NONETWORK)
//									{
//										formatinfstr(infstr, x_("UNCONNECTED%ld"), unconnectednet++);
//									} else
//									{
//										if (net->buswidth > 1)
//										{
//											sigcount = net->buswidth;
//											if (nodewidth > 1 && pp->network->buswidth * nodewidth == net->buswidth)
//											{
//												// map wide bus to a particular instantiation of an arrayed node
//												if (pp->network->buswidth == 1)
//												{
//													onet = net->networklist[nindex];
//													if (onet == pwrnet) addstringtoinfstr(infstr, x_("vdd")); else
//														if (onet == gndnet) addstringtoinfstr(infstr, x_("gnd")); else
//															addstringtoinfstr(infstr, &((CHAR *)onet->temp2)[1]);
//												} else
//												{
//													outernetlist = (NETWORK **)emalloc(pp->network->buswidth * (sizeof (NETWORK *)),
//														sim_tool->cluster);
//													for(j=0; j<pp->network->buswidth; j++)
//														outernetlist[j] = net->networklist[i + nindex*pp->network->buswidth];
//													for(opt = pp->protoname; *opt != 0; opt++)
//														if (*opt == '[') break;
//													osave = *opt;
//													*opt = 0;
//													sim_verwritebus(outernetlist, 0, net->buswidth-1, 0,
//														&unconnectednet, 0, pwrnet, gndnet, infstr);
//													*opt = osave;
//													efree((CHAR *)outernetlist);
//												}
//											} else
//											{
//												if (pp->network->buswidth != net->buswidth)
//												{
//													ttyputerr(_("***ERROR: port %s on node %s in cell %s is %d wide, but is connected/exported with width %d"),
//														pp->protoname, describenodeinst(ni), describenodeproto(np),
//															cpp->network->buswidth, net->buswidth);
//													sigcount = mini(sigcount, cpp->network->buswidth);
//													if (sigcount == 1) sigcount = 0;
//												}
//												outernetlist = (NETWORK **)emalloc(net->buswidth * (sizeof (NETWORK *)),
//													sim_tool->cluster);
//												for(j=0; j<net->buswidth; j++)
//													outernetlist[j] = net->networklist[j];
//												for(opt = pp->protoname; *opt != 0; opt++)
//													if (*opt == '[') break;
//												osave = *opt;
//												*opt = 0;
//												sim_verwritebus(outernetlist, 0, net->buswidth-1, 0,
//													&unconnectednet, 0, pwrnet, gndnet, infstr);
//												*opt = osave;
//												efree((CHAR *)outernetlist);
//											}
//										} else
//										{
//											if (net == pwrnet) addstringtoinfstr(infstr, x_("vdd")); else
//												if (net == gndnet) addstringtoinfstr(infstr, x_("gnd")); else
//													addstringtoinfstr(infstr, &((CHAR *)net->temp2)[1]);
//										}
//									}
//								} else if (namesame(startpt, x_("node_name")) == 0)
//								{
//									if (nodewidth > 1) opt = nodenames[nindex]; else
//									{
//										var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, el_node_name_key);
//										if (var == NOVARIABLE) opt = x_(""); else
//											opt = (CHAR *)var->addr;
//									}
//									addstringtoinfstr(infstr, sim_vernamenoindices(opt));
//								} else
//								{
//									// no port name found, look for variable name
//									esnprintf(line, 200, x_("ATTR_%s"), startpt);
//									var = getval((INTBIG)ni, VNODEINST, -1, line);
//									if (var == NOVARIABLE)
//										var = getval((INTBIG)ni, VNODEINST, -1, startpt);
//									if (var == NOVARIABLE)
//									{
//										// value not found: see if this is a parameter and use default
//										nip = ni->proto;
//										nipc = contentsview(nip);
//										if (nipc != NONODEPROTO) nip = nipc;
//										var = getval((INTBIG)nip, VNODEPROTO, -1, line);
//									}
//									if (var == NOVARIABLE)
//										addstringtoinfstr(infstr, x_("??")); else
//									{
//										addstringtoinfstr(infstr, describesimplevariable(var));
//									}
//								}
//								*pt = save;
//								if (save == 0) break;
//							}
//							break;
//						}
//
//						// generate the line the normal way
//						namecount = sim_vergetnetworks(cnp, &namelist, &lowindex, &highindex,
//							&tempval, &wirelist, &netcount, &pwrnetdummy, &gndnetdummy, 1);
//						l = 0;
//						for(i=0; i<namecount; i++)
//						{
//							// ignore networks that aren't exported
//							if (tempval[i] <= 1 || tempval[i] >= 6)
//							{
//								l++;
//								if (lowindex[i] <= highindex[i])
//									l += highindex[i] - lowindex[i];
//								continue;
//							}
//							if (first) first = FALSE; else
//								addstringtoinfstr(infstr, x_(", "));
//							if (lowindex[i] > highindex[i])
//							{
//								// single signal
//								addstringtoinfstr(infstr, x_("."));
//								addstringtoinfstr(infstr, namelist[i]);
//								addstringtoinfstr(infstr, x_("("));
//								net = (NETWORK *)wirelist[l++].net->temp2;
//								if (net == NONETWORK || net->temp2 == 0)
//								{
//									formatinfstr(infstr, x_("UNCONNECTED%ld"), unconnectednet++);
//								} else
//								{
//									if (net == pwrnet) addstringtoinfstr(infstr, x_("vdd")); else
//										if (net == gndnet) addstringtoinfstr(infstr, x_("gnd")); else
//											addstringtoinfstr(infstr, &((CHAR *)net->temp2)[1]);
//								}
//								addstringtoinfstr(infstr, x_(")"));
//							} else
//							{
//								total = highindex[i]-lowindex[i]+1;
//								outernetlist = (NETWORK **)emalloc(total * (sizeof (NETWORK *)),
//									sim_tool->cluster);
//								for(j=lowindex[i]; j<=highindex[i]; j++)
//									outernetlist[j-lowindex[i]] = (NETWORK *)wirelist[j-lowindex[i]+l].net->temp2;
//
//								sim_verwritebus(outernetlist, lowindex[i], highindex[i], tempval[i],
//									&unconnectednet, namelist[i], pwrnet, gndnet, infstr);
//								l += highindex[i] - lowindex[i] + 1;
//								efree((CHAR *)outernetlist);
//							}
//						}
//						addstringtoinfstr(infstr, x_(");"));
//						break;
//
//					case 1:		// and/or gate: write ports in the proper order
//						for(i=0; i<2; i++)
//						{
//							for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//							{
//								if (i == 0)
//								{
//									if (estrcmp(pi->proto->protoname, x_("y")) != 0) continue;
//								} else
//								{
//									if (estrcmp(pi->proto->protoname, x_("a")) != 0) continue;
//								}
//								if (first) first = FALSE; else
//									addstringtoinfstr(infstr, x_(", "));
//								net = pi->conarcinst->network;
//								if (nodewidth > 1)
//								{
//									if (net->buswidth == nodewidth) net = net->networklist[nindex];
//								} else
//								{
//									if (net->buswidth > 1)
//									{
//										ttyputerr(_("***ERROR: cell %s, node %s is not arrayed but is connected to a bus"),
//											describenodeproto(np), describenodeinst(ni));
//										net = net->networklist[0];
//									}
//								}
//								signame = &((CHAR *)net->temp2)[1];
//								if (net == pwrnet) signame = x_("vdd"); else
//									if (net == gndnet) signame = x_("gnd");
//								if (i != 0 && pi->conarcinst->temp1 != 0)
//								{
//									// this input is negated: write the implicit inverter
//									esnprintf(invsigname, 100, x_("%s%ld"), IMPLICITINVERTERSIGNAME,
//										pi->conarcinst->temp1+nindex);
//									xprintf(sim_verfile, x_("  inv %s%ld (%s, %s);\n"),
//										IMPLICITINVERTERNODENAME, pi->conarcinst->temp1+nindex,
//											invsigname, signame);
//									signame = invsigname;
//								}
//								addstringtoinfstr(infstr, signame);
//							}
//						}
//						addstringtoinfstr(infstr, x_(");"));
//						break;
//
//					case 2:		// transistors: write ports in the proper order
//						// schem: g/s/d  mos: g/s/g/d
//						gnet = ni->proto->firstportproto->network;
//						for(i=0; i<2; i++)
//						{
//							for(pp = ni->proto->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//							{
//								for(opp = ni->proto->firstportproto; opp != pp; opp = opp->nextportproto)
//									if (opp->network == pp->network) break;
//								if (opp != pp) continue;
//								if (dropbias != 0 && namesame(pp->protoname, x_("b")) == 0) continue;
//								if (i == 0)
//								{
//									if (pp->network == gnet) continue;
//								} else
//								{
//									if (pp->network != gnet) continue;
//								}
//								net = NONETWORK;
//								for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//									if (pi->proto->network == pp->network) break;
//								if (pi != NOPORTARCINST) net = pi->conarcinst->network; else
//								{
//									for(pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst)
//										if (pe->proto == pp) break;
//									if (pe != NOPORTEXPINST) net = pe->exportproto->network;
//								}
//								if (first) first = FALSE; else
//									addstringtoinfstr(infstr, x_(", "));
//								if (net == NONETWORK)
//								{
//									ttyputmsg(_("***Warning: cell %s, node %s is not fully connected"),
//										describenodeproto(np), describenodeinst(ni));
//									esnprintf(impsigname, 100, x_("UNCONNECTED%ld"), unconnectednet++);
//									signame = impsigname;
//								} else
//								{
//									if (nodewidth > 1)
//									{
//										(void)net_evalbusname(APBUS, networkname(net, 0), &strings, pi->conarcinst, np, 1);
//										estrcpy(impsigname, strings[nindex]);
//										signame = impsigname;
//									} else
//									{
//										signame = &((CHAR *)net->temp2)[1];
//										if (net == pwrnet) signame = x_("vdd"); else
//											if (net == gndnet) signame = x_("gnd");
//									}
//									if (i != 0 && pi != NOPORTARCINST && pi->conarcinst->temp1 != 0)
//									{
//										// this input is negated: write the implicit inverter
//										esnprintf(invsigname, 100, x_("%s%ld"), IMPLICITINVERTERSIGNAME,
//											pi->conarcinst->temp1+nindex);
//										xprintf(sim_verfile, x_("  inv %s%ld (%s, %s);\n"),
//											IMPLICITINVERTERNODENAME, pi->conarcinst->temp1+nindex,
//												invsigname, signame);
//										signame = invsigname;
//									}
//								}
//								addstringtoinfstr(infstr, signame);
//							}
//						}
//						addstringtoinfstr(infstr, x_(");"));
//						break;
//	#if 0
//					case 3:		// cell that has an external definition
//						for(pp = ni->proto->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//						{
//							for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//								if (pi->proto->network == pp->network) break;
//							if (first) first = FALSE; else
//								addstringtoinfstr(infstr, x_(", "));
//							if (pi == NOPORTARCINST)
//							{
//								ttyputmsg(_("Warning: cell %s, node %s is not fully connected"),
//									describenodeproto(np), describenodeinst(ni));
//								esnprintf(impsigname, 100, x_("UNCONNECTED%ld"), unconnectednet++);
//								signame = impsigname;
//							} else
//							{
//								net = pi->conarcinst->network;
//								if (nodewidth > 1)
//								{
//									(void)net_evalbusname(
//										(pi->conarcinst->proto->userbits&AFUNCTION)>>AFUNCTIONSH,
//											networkname(net, 0), &strings, pi->conarcinst, np, 1);
//									estrcpy(impsigname, strings[nindex]);
//									signame = impsigname;
//								} else
//								{
//									signame = &((CHAR *)net->temp2)[1];
//									if (net == pwrnet) signame = x_("vdd"); else
//										if (net == gndnet) signame = x_("gnd");
//								}
//								if (pi->conarcinst->temp1 != 0)
//								{
//									// this input is negated: write the implicit inverter
//									esnprintf(invsigname, 100, x_("%s%ld"), IMPLICITINVERTERSIGNAME,
//										pi->conarcinst->temp1+nindex);
//									xprintf(sim_verfile, x_("  inv %s%ld (%s, %s);\n"),
//										IMPLICITINVERTERNODENAME, pi->conarcinst->temp1+nindex,
//											invsigname, signame);
//									signame = invsigname;
//								}
//							}
//							formatinfstr(infstr, x_(".%s(%s)"), sim_verconvertname(pp->protoname), signame);
//						}
//						addstringtoinfstr(infstr, x_(");"));
//						break;
//	#endif
//				}
//				sim_verwritelongline(returninfstr(infstr));
//			}
//			efree((CHAR *)nodename);
//		}
//		if (paramname == 0) modulename = sim_vercellname(np); else
//			modulename = sim_verconvertname(paramname);
//		xprintf(sim_verfile, x_("endmodule   /* %s */\n"), modulename);
//
//		// free net names
//		for(net = np->firstnetwork; net != NONETWORK; net = net->nextnetwork)
//			if (net->temp2 != 0) efree((CHAR *)net->temp2);
//
//		return(backannotate);
//	}

	/*
	 * Routine to add a bus of signals named "name" to the infinite string "infstr".  If "name" is zero,
	 * do not include the ".NAME()" wrapper.  The signals are in "outernetlist" and range in index from
	 * "lowindex" to "highindex".  They are described by a bus with characteristic "tempval"
	 * (low bit is on if the bus descends).  Any unconnected networks can be numbered starting at
	 * "*unconnectednet".  The power and grounds nets are "pwrnet" and "gndnet".
	 */
//	void sim_verwritebus(NETWORK **outernetlist, INTBIG lowindex, INTBIG highindex, INTBIG tempval,
//		INTBIG *unconnectednet, CHAR *name, NETWORK *pwrnet, NETWORK *gndnet, void *infstr)
//	{
//		REGISTER INTBIG breakbus, numexported, numinternal, j, k, start, end, order, li;
//		REGISTER NETWORK *net, *onet, *lastnet;
//		REGISTER CHAR *thisnetname, *lastnetname, *pt, *lastpt;
//
//		// array signal: see if it gets split out
//		breakbus = 0;
//
//		// bus cannot have pwr/gnd, must be connected
//		numexported = numinternal = 0;
//		for(j=lowindex; j<=highindex; j++)
//		{
//			net = outernetlist[j-lowindex];
//			if (net == NONETWORK || net->temp1 == 6) break;
//			if (net->temp1 > 1) numexported++; else
//				numinternal++;
//		}
//		if (j <= highindex) breakbus = 1;
//
//		// must be all exported or all internal, not a mix
//		if (numexported > 0 && numinternal > 0) breakbus = 1;
//
//		if (breakbus == 0)
//		{
//			// see if all of the nets on this bus are distinct
//			for(j=lowindex+1; j<=highindex; j++)
//			{
//				net = outernetlist[j-lowindex];
//				for(k=lowindex; k<j; k++)
//				{
//					onet = outernetlist[k-lowindex];
//					if (net == onet) break;
//				}
//				if (k < j) break;
//			}
//			if (j <= highindex) breakbus = 1; else
//			{
//				// bus entries must have the same root name and go in order
//				lastnet = NONETWORK;
//				for(j=lowindex; j<=highindex; j++)
//				{
//					net = outernetlist[j-lowindex];
//					thisnetname = (CHAR *)net->temp2;
//					if (*thisnetname == 'D')
//					{
//						if ((tempval&1) == 0) break;
//					} else if (*thisnetname == 'U')
//					{
//						if ((tempval&1) != 0) break;
//					}
//					thisnetname++;
//
//					for(pt = thisnetname; *pt != 0; pt++)
//						if (*pt == '[') break;
//					if (*pt == 0) break;
//					if (j > lowindex)
//					{
//						lastnetname = &((CHAR *)lastnet->temp2)[1];
//						for(li = 0; lastnetname[li] != 0; li++)
//						{
//							if (thisnetname[li] != lastnetname[li]) break;
//							if (lastnetname[li] == '[') break;
//						}
//						if (lastnetname[li] != '[' || thisnetname[li] != '[') break;
//						if (myatoi(pt+1) != myatoi(&lastnetname[li+1])+1) break;
//					}
//					lastnet = net;
//				}
//				if (j <= highindex) breakbus = 1;
//			}
//		}
//
//		if (name != 0) formatinfstr(infstr, x_(".%s("), name);
//		if (breakbus != 0)
//		{
//			addstringtoinfstr(infstr, x_("{"));
//			if ((tempval&1) != 0)
//			{
//				start = highindex;
//				end = lowindex;
//				order = -1;
//			} else
//			{
//				start = lowindex;
//				end = highindex;
//				order = 1;
//			}
//			for(j=start; ; j += order)
//			{
//				if (j != start)
//					addstringtoinfstr(infstr, x_(", "));
//				net = outernetlist[j-lowindex];
//				if (net == NONETWORK)
//				{
//					formatinfstr(infstr, x_("UNCONNECTED%ld"), *unconnectednet);
//					(*unconnectednet)++;
//				} else
//				{
//					if (net == pwrnet) addstringtoinfstr(infstr, x_("vdd")); else
//						if (net == gndnet) addstringtoinfstr(infstr, x_("gnd")); else
//							addstringtoinfstr(infstr, &((CHAR *)net->temp2)[1]);
//				}
//				if (j == end) break;
//			}
//			addstringtoinfstr(infstr, x_("}"));
//		} else
//		{
//			lastnet = outernetlist[0];
//			for(lastpt = &((CHAR *)lastnet->temp2)[1]; *lastpt != 0; lastpt++)
//				if (*lastpt == '[') break;
//			net = outernetlist[highindex-lowindex];
//			for(pt = &((CHAR *)net->temp2)[1]; *pt != 0; pt++)
//			{
//				if (*pt == '[') break;
//				addtoinfstr(infstr, *pt);
//			}
//			if ((tempval&1) != 0)
//			{
//				formatinfstr(infstr, x_("[%ld:%ld]"), myatoi(pt+1), myatoi(lastpt+1));
//			} else
//			{
//				formatinfstr(infstr, x_("[%ld:%ld]"), myatoi(lastpt+1), myatoi(pt+1));
//			}
//		}
//		if (name != 0) addstringtoinfstr(infstr, x_(")"));
//	}

	/*
	 * Routine to add text from all nodes in cell "np"
	 * (which have "verilogkey" text on them)
	 * to that text to the output file.  Returns true if anything
	 * was found.
	 */
//	BOOLEAN sim_verincludetypedcode(NODEPROTO *np, INTBIG verilogkey, CHAR *descript)
//	{
//		BOOLEAN first;
//		REGISTER INTBIG len, i;
//		REGISTER NODEINST *ni;
//		REGISTER VARIABLE *var;
//
//		// write out any directly-typed Verilog code
//		first = TRUE;
//		for(ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			if (ni->proto != gen_invispinprim) continue;
//			var = getvalkey((INTBIG)ni, VNODEINST, -1, verilogkey);
//			if (var == NOVARIABLE) continue;
//			if ((var->type&VTYPE) != VSTRING) continue;
//			if ((var->type&VDISPLAY) == 0) continue;
//			if (first)
//			{
//				first = FALSE;
//				xprintf(sim_verfile, x_("  /* user-specified Verilog %s */\n"),
//					descript);
//			}
//			if ((var->type&VISARRAY) == 0)
//			{
//				xprintf(sim_verfile, x_("  %s\n"), (CHAR *)var->addr);
//			} else
//			{
//				len = getlength(var);
//				for(i=0; i<len; i++)
//					xprintf(sim_verfile, x_("  %s\n"), ((CHAR **)var->addr)[i]);
//			}
//		}
//		if (!first) xprintf(sim_verfile, x_("\n"));
//		return(first);
//	}

	/*
	 * Routine to write a long line to the Verilog file, breaking it where sensible.
	 */
//	void sim_verwritelongline(CHAR *s)
//	{
//		CHAR *pt, save;
//		INTBIG i;
//		CHAR *lastspace;
//
//		lastspace = NULL;
//		i = 0;
//		for (pt = s; *pt; pt++)
//		{
//			if (*pt == ' ' || *pt == ',') lastspace = pt;
//			++i;
//			if (i >= MAXDECLARATIONWIDTH)
//			{
//				if (lastspace != NULL)
//				{
//					if (*lastspace != ' ') lastspace++;
//					save = *lastspace;   *lastspace = 0;
//					xputs(s, sim_verfile);
//					*lastspace = save;
//					xputs(x_("\n      "), sim_verfile);
//					s = lastspace;
//					if (*s == ' ') s++;
//					i = 6 + pt-s+1;
//					lastspace = NULL;
//				} else
//				{
//					xputs(x_("\n      "), sim_verfile);
//					i = 6 + 1;
//				}
//			}
//		}
//		xputs(s, sim_verfile);
//		xputs(x_("\n"), sim_verfile);
//	}

	/*
	 * Routine to initialize the collection of signal names in a declaration.
	 * The declaration starts with the string "header".
	 */
//	void sim_verinitdeclaration(CHAR *header)
//	{
//		estrcpy(sim_verdeclarationline, header);
//		sim_verdeclarationprefix = estrlen(sim_verdeclarationline);
//	}

	/*
	 * Routine to add "signame" to the collection of signal names in a declaration.
	 */
//	void sim_veradddeclaration(CHAR *signame)
//	{
//		if (estrlen(sim_verdeclarationline) + estrlen(signame) + 3 > MAXDECLARATIONWIDTH)
//		{
//			xprintf(sim_verfile, x_("%s;\n"), sim_verdeclarationline);
//			sim_verdeclarationline[sim_verdeclarationprefix] = 0;
//		}
//		if ((INTBIG)estrlen(sim_verdeclarationline) != sim_verdeclarationprefix)
//			estrcat(sim_verdeclarationline, x_(","));
//		estrcat(sim_verdeclarationline, x_(" "));
//		estrcat(sim_verdeclarationline, signame);
//	}

	/*
	 * Routine to terminate the collection of signal names in a declaration
	 * and write the declaration to the Verilog file.
	 */
//	void sim_vertermdeclaration(void)
//	{
//		xprintf(sim_verfile, x_("%s;\n"), sim_verdeclarationline);
//	}

	/*
	 * Routine to return the name of cell "c", given that it may be ambiguously used in multiple
	 * libraries.
	 */
//	CHAR *sim_vercellname(NODEPROTO *np)
//	{
//		REGISTER void *infstr;
//
//		/* Some other function is messing with temp2 during verilog netlisting
//		   so this function is broken.  For now, every instance will be prepended
//		   with it's library name
//		if (np->temp2 == 0)
//			return(sim_verconvertname(np->protoname));
//			*/
//
//		infstr = initinfstr();
//		formatinfstr(infstr, x_("%s__%s"), np->lib->libname, np->protoname);
//		return(returninfstr(infstr));
//	}

	/*
	 * routine to adjust name "p" and return the string.
	 * Verilog does permit a digit in the first location; prepend a "_" if found.
	 * Verilog only permits the "_" and "$" characters: all others are converted to "_".
	 * Verilog does not permit nonnumeric indices, so "P[A]" is converted to "P_A_"
	 * Verilog does not permit multidimensional arrays, so "P[1][2]" is converted to "P_1_[2]"
	 *   and "P[1][T]" is converted to "P_1_T_"
	 */
//	CHAR *sim_verconvertname(CHAR *p)
//	{
//		REGISTER CHAR *t, *end;
//		REGISTER void *infstr;
//
//		// simple names are trivially accepted as is
//		for(t = p; *t != 0; t++) if (!isalnum(*t)) break;
//		if (*t == 0 && !isdigit(*p)) return(p);
//
//		infstr = initinfstr();
//		end = sim_verstartofindex(p);
//		for(t = p; t < end; t++)
//		{
//			if (*t == '[' || *t == ']')
//			{
//				addtoinfstr(infstr, '_');
//				if (*t == ']' && t[1] == '[') t++;
//			} else
//			{
//				if (isalnum(*t) || *t == '_' || *t == '$')
//					addtoinfstr(infstr, *t); else
//						addtoinfstr(infstr, '_');
//			}
//		}
//		addstringtoinfstr(infstr, end);
//		if (*end != 0) addstringtoinfstr(infstr, x_("_"));
//		return(returninfstr(infstr));
//	}

	/*
	 * routine to adjust name "p" and return the string.
	 * This code removes all index indicators and other special characters, turning
	 * them into "_".
	 */
//	CHAR *sim_vernamenoindices(CHAR *p)
//	{
//		REGISTER CHAR *t;
//		REGISTER void *infstr;
//
//		infstr = initinfstr();
//		if (isdigit(*p)) addtoinfstr(infstr, '_');
//		for(t = p; *t != 0 ; t++)
//		{
//			if (isalnum(*t) || *t == '_' || *t == '$')
//				addtoinfstr(infstr, *t); else
//					addtoinfstr(infstr, '_');
//		}
//		return(returninfstr(infstr));
//	}

	/*
	 * Routine to return the character position in network name "name" that is the start of indexing.
	 * If there is no indexing ("clock"), this will point to the end of the string.
	 * If there is simple indexing ("dog[12]"), this will point to the "[".
	 * If the index is nonnumeric ("dog[cat]"), this will point to the end of the string.
	 * If there are multiple indices, ("dog[12][45]") this will point to the last "[" (unless it is nonnumeric).
	 */
//	CHAR *sim_verstartofindex(CHAR *name)
//	{
//		REGISTER INTBIG len, i;
//
//		len = estrlen(name);
//		if (name[len-1] != ']') return(name+len);
//		for(i = len-2; i > 0; i--)
//		{
//			if (name[i] == '[') break;
//			if (name[i] == ':' || name[i] == ',') continue;
//			if (!isdigit(name[i])) break;
//		}
//		if (name[i] != '[') return(name+len);
//		return(name+i);
//	}

	/*
	 * Routine to scan networks in cell "cell".  The "temp1" field is filled in with
	 *    0: internal network (ascending order when in a bus)
	 *    1: internal network (descending order when in a bus)
	 *    2: exported input network (ascending order when in a bus)
	 *    3: exported input network (descending order when in a bus)
	 *    4: exported output network (ascending order when in a bus)
	 *    5: exported output network (descending order when in a bus)
	 *    6: power or ground network
	 * All networks are sorted by name within "temp1" and the common array entries are
	 * reduced to a list of names (in "namelist") and their low/high range of indices
	 * (in "lowindex" and "highindex", with high < low if no indices apply).  The value
	 * of "temp1" is returned in the array "tempval".  The list of "netcount" networks
	 * found (uncombined by index) is returned in "wirelist".  The power and ground nets
	 * are stored in "pwrnet" and "gndnet".  The total number of names is returned.
	 */
//	INTBIG sim_vergetnetworks(NODEPROTO *cell, CHAR ***namelist,
//		INTBIG **lowindex, INTBIG **highindex, INTBIG **tempval,
//		WIRELIST **wirelist, INTBIG *netcount, NETWORK **pwrnet, NETWORK **gndnet, INTBIG quiet)
//	{
//		REGISTER NETWORK *net, *endnet, *subnet;
//		REGISTER PORTPROTO *pp, *widestpp;
//		REGISTER BOOLEAN updir, downdir, randomdir, multipwr, multignd, found;
//		REGISTER NODEINST *ni;
//		REGISTER ARCINST *ai;
//		REGISTER PORTARCINST *pi;
//		REGISTER INTBIG wirecount, i, j, k, namelistcount, index, newtotal, dirbit,
//			*newtemp, *newlow, *newhigh, comp, fun, last, widestfound;
//		REGISTER UINTBIG characteristics;
//		REGISTER CHAR **newnamelist, save, *pt, *ept, *name, *endname;
//		REGISTER void *infstr;
//
//		// initialize to describe all nets
//		namelistcount = 0;
//		wirecount = 0;
//		for(net = cell->firstnetwork; net != NONETWORK; net = net->nextnetwork)
//		{
//			net->temp1 = 0;
//			if (net->buswidth > 1) continue;
//			wirecount++;
//		}
//
//		// determine default direction of busses
//		if ((net_options&NETDEFBUSBASEDESC) != 0) dirbit = 1; else
//			dirbit = 0;
//
//		// mark exported networks
//		for(pp = cell->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//		{
//			switch (pp->userbits&STATEBITS)
//			{
//				case OUTPORT:   j = 4+dirbit;  break;
//				default:        j = 2+dirbit;  break;
//			}
//			if (pp->network->buswidth > 1)
//			{
//				// bus export: mark individual network entries
//				for(i=0; i<pp->network->buswidth; i++)
//				{
//					net = pp->network->networklist[i];
//					net->temp1 = j;
//				}
//			} else
//			{
//				// single wire export: mark the network
//				net = pp->network;
//				net->temp1 = j;
//			}
//		}
//
//		// postprocess to ensure the directionality is correct
//		for(net = cell->firstnetwork; net != NONETWORK; net = net->nextnetwork)
//		{
//			if (net->temp1 == 0) continue;
//			if (net->namecount <= 0) continue;
//			name = networkname(net, 0);
//			for(pp = cell->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//			{
//				if (pp->network->buswidth > 1) continue;
//				if (namesame(pp->protoname, name) != 0) continue;
//				switch (pp->userbits&STATEBITS)
//				{
//					case OUTPORT:   j = 4+dirbit;  break;
//					default:        j = 2+dirbit;  break;
//				}
//				net->temp1 = j;
//			}
//		}
//
//		// make sure all busses go in the same direction
//		for(net = cell->firstnetwork; net != NONETWORK; net = net->nextnetwork)
//		{
//			if (net->buswidth <= 1) continue;
//			j = 0;
//			for(i=0; i<net->buswidth; i++)
//			{
//				subnet = net->networklist[i];
//				if (subnet->temp1 == 0) continue;
//				if (j == 0) j = subnet->temp1;
//				if (subnet->temp1 != j) break;
//			}
//			if (i >= net->buswidth) continue;
//
//			// mixed directionality: make it all nonoutput
//			for(i=0; i<net->buswidth; i++)
//			{
//				subnet = net->networklist[i];
//				subnet->temp1 = 2+dirbit;
//			}
//		}
//
//		// scan all networks for those that go in descending order
//		for(net = cell->firstnetwork; net != NONETWORK; net = net->nextnetwork)
//		{
//			if (net->buswidth <= 1) continue;
//			updir = downdir = randomdir = FALSE;
//			last = 0;
//			for(i=0; i<net->buswidth; i++)
//			{
//				subnet = net->networklist[i];
//				if (subnet->namecount == 0) break;
//				for(pt = networkname(subnet, 0); *pt != 0; pt++)
//					if (*pt == '[') break;
//				if (*pt == 0) break;
//				if (isdigit(pt[1]) == 0) break;
//				index = myatoi(pt+1);
//				if (i != 0)
//				{
//					if (index == last-1) downdir = TRUE; else
//						if (index == last+1) updir = TRUE; else
//							randomdir = TRUE;
//				}
//				last = index;
//			}
//			if (randomdir) continue;
//			if (updir && downdir) continue;
//			if (!updir && !downdir) continue;
//			if (downdir) dirbit = 1; else
//				dirbit = 0;
//			for(i=0; i<net->buswidth; i++)
//			{
//				subnet = net->networklist[i];
//				subnet->temp1 = (subnet->temp1 & ~1) | dirbit;
//			}
//		}
//
//		// find power and ground
//		*pwrnet = *gndnet = NONETWORK;
//		multipwr = multignd = FALSE;
//		for(pp = cell->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//		{
//			if (portispower(pp))
//			{
//				if (*pwrnet != NONETWORK && *pwrnet != pp->network && !multipwr)
//				{
//					if (quiet == 0)
//						ttyputmsg(_("Warning: multiple power networks in cell %s"),
//							describenodeproto(cell));
//					multipwr = TRUE;
//				}
//				*pwrnet = pp->network;
//			}
//			if (portisground(pp))
//			{
//				if (*gndnet != NONETWORK && *gndnet != pp->network && !multignd)
//				{
//					if (quiet == 0)
//						ttyputmsg(_("Warning: multiple ground networks in cell %s"),
//							describenodeproto(cell));
//					multignd = TRUE;
//				}
//				*gndnet = pp->network;
//			}
//		}
//		for(net = cell->firstnetwork; net != NONETWORK; net = net->nextnetwork)
//		{
//			if (net->globalnet >= 0 && net->globalnet < cell->globalnetcount)
//			{
//				characteristics = cell->globalnetchar[net->globalnet];
//				if (characteristics == PWRPORT)
//				{
//					if (*pwrnet != NONETWORK && *pwrnet != net && !multipwr)
//					{
//						if (quiet == 0)
//							ttyputmsg(_("Warning: multiple power networks in cell %s"),
//								describenodeproto(cell));
//						multipwr = TRUE;
//					}
//					*pwrnet = net;
//				} else if (characteristics == GNDPORT)
//				{
//					if (*gndnet != NONETWORK && *gndnet != net && !multignd)
//					{
//						if (quiet == 0)
//							ttyputmsg(_("Warning: multiple ground networks in cell %s"),
//								describenodeproto(cell));
//						multignd = TRUE;
//					}
//					*gndnet = net;
//				}
//			}
//		}
//		for(ni = cell->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			fun = nodefunction(ni);
//			if (fun == NPCONPOWER || fun == NPCONGROUND)
//			{
//				for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//				{
//					ai = pi->conarcinst;
//					if (fun == NPCONPOWER)
//					{
//						if (*pwrnet != NONETWORK && *pwrnet != ai->network && !multipwr)
//						{
//							if (quiet == 0)
//								ttyputmsg(_("Warning: multiple power networks in cell %s"),
//									describenodeproto(cell));
//							multipwr = TRUE;
//						}
//						*pwrnet = ai->network;
//					} else
//					{
//						if (*gndnet != NONETWORK && *gndnet != ai->network && !multignd)
//						{
//							if (quiet == 0)
//								ttyputmsg(_("Warning: multiple ground networks in cell %s"),
//									describenodeproto(cell));
//							multignd = TRUE;
//						}
//						*gndnet = ai->network;
//					}
//				}
//			}
//		}
//		if (*pwrnet != NONETWORK) (*pwrnet)->temp1 = 6;
//		if (*gndnet != NONETWORK) (*gndnet)->temp1 = 6;
//
//		// make sure there is room in the array of networks
//		if (wirecount > sim_verilogwirelisttotal)
//		{
//			if (sim_verilogwirelisttotal > 0)
//				efree((CHAR *)sim_verilogwirelist);
//			sim_verilogwirelisttotal = 0;
//			sim_verilogwirelist = (WIRELIST *)emalloc(wirecount * (sizeof (WIRELIST)),
//				sim_tool->cluster);
//			if (sim_verilogwirelist == 0) return(0);
//			sim_verilogwirelisttotal = wirecount;
//		}
//
//		// load the array
//		if (wirecount > 0)
//		{
//			i = 0;
//			for(net = cell->firstnetwork; net != NONETWORK; net = net->nextnetwork)
//			{
//				if (net->buswidth > 1) continue;
//				sim_verilogwirelist[i].net = net;
//				sim_verilogwirelist[i].pp = NOPORTPROTO;
//
//				// find the widest export that touches this network
//				widestfound = -1;
//				widestpp = NOPORTPROTO;
//				for(pp = cell->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//				{
//					found = FALSE;
//					if (pp->network == net) found = TRUE; else
//					{
//						if (pp->network->buswidth > 1)
//						{
//							for(j=0; j<pp->network->buswidth; j++)
//								if (pp->network->networklist[j] == net) break;
//							if (j < pp->network->buswidth) found = TRUE;
//						}
//					}
//					if (found)
//					{
//						if (pp->network->buswidth > widestfound)
//						{
//							widestfound = pp->network->buswidth;
//							widestpp = pp;
//						}
//					}
//				}
//				if (widestpp != NOPORTPROTO) sim_verilogwirelist[i].pp = widestpp;
//				i++;
//			}
//
//			// sort the networks by name
//			esort(sim_verilogwirelist, wirecount, sizeof (WIRELIST), sim_versortwirelistsbyname);
//
//			// organize by name and index order
//			for(i=0; i<wirecount; i++)
//			{
//				net = sim_verilogwirelist[i].net;
//
//				// make sure there is room in the list
//				if (namelistcount >= sim_verilognamelisttotal)
//				{
//					newtotal = sim_verilognamelisttotal * 2;
//					if (newtotal <= namelistcount) newtotal = namelistcount + 5;
//					newnamelist = (CHAR **)emalloc(newtotal * (sizeof (CHAR *)),
//						sim_tool->cluster);
//					if (newnamelist == 0) return(0);
//					newlow = (INTBIG *)emalloc(newtotal * SIZEOFINTBIG, sim_tool->cluster);
//					if (newlow == 0) return(0);
//					newhigh = (INTBIG *)emalloc(newtotal * SIZEOFINTBIG, sim_tool->cluster);
//					if (newhigh == 0) return(0);
//					newtemp = (INTBIG *)emalloc(newtotal * SIZEOFINTBIG, sim_tool->cluster);
//					if (newtemp == 0) return(0);
//					for(j=0; j<newtotal; j++) newnamelist[j] = 0;
//					for(j=0; j<namelistcount; j++)
//					{
//						newnamelist[j] = sim_verilognamelist[j];
//						newlow[j] = sim_verilognamelistlow[j];
//						newhigh[j] = sim_verilognamelisthigh[j];
//						newtemp[j] = sim_verilognamelisttempval[j];
//					}
//					if (sim_verilognamelisttotal > 0)
//					{
//						efree((CHAR *)sim_verilognamelist);
//						efree((CHAR *)sim_verilognamelistlow);
//						efree((CHAR *)sim_verilognamelisthigh);
//						efree((CHAR *)sim_verilognamelisttempval);
//					}
//					sim_verilognamelist = newnamelist;
//					sim_verilognamelistlow = newlow;
//					sim_verilognamelisthigh = newhigh;
//					sim_verilognamelisttempval = newtemp;
//					sim_verilognamelisttotal = newtotal;
//				}
//
//				// add this name to the list
//				if (net->globalnet > 1 && net->globalnet < net->parent->globalnetcount)
//				{
//					name = net->parent->globalnetnames[net->globalnet];
//				} else
//				{
//					if (net->namecount == 0) name = describenetwork(net); else
//						name = networkname(net, 0);
//
//					// if exported, be sure to get the export name
//					if (net->temp1 > 1 && net->temp1 < 6)
//					{
//						for(pp = cell->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//							if (pp->network == net) break;
//						if (pp != NOPORTPROTO) name = pp->protoname;
//					}
//				}
//
//				pt = sim_verstartofindex(name);
//				save = *pt;
//				*pt = 0;
//				if (sim_verilognamelist[namelistcount] != 0)
//					efree(sim_verilognamelist[namelistcount]);
//				(void)allocstring(&sim_verilognamelist[namelistcount],
//					sim_verconvertname(name), sim_tool->cluster);
//				sim_verilognamelisttempval[namelistcount] = net->temp1;
//				if (save == 0)
//				{
//					// single wire: set range to show that
//					sim_verilognamelistlow[namelistcount] = 1;
//					sim_verilognamelisthigh[namelistcount] = 0;
//				} else
//				{
//					sim_verilognamelistlow[namelistcount] =
//						sim_verilognamelisthigh[namelistcount] = myatoi(pt+1);
//					for(j=i+1; j<wirecount; j++)
//					{
//						endnet = sim_verilogwirelist[j].net;
//						if (endnet == NONETWORK) break;
//						if (endnet->temp1 != net->temp1) break;
//						if (sim_verilogwirelist[j].pp != sim_verilogwirelist[i].pp) break;
//						if (endnet->globalnet != net->globalnet) break;
//						if (endnet->namecount == 0) break;
//						endname = networkname(endnet, 0);
//						ept = sim_verstartofindex(endname);
//						if (*ept != '[') break;
//						*ept = 0;
//						comp = namesame(name, endname);
//						*ept = '[';
//						if (comp != 0) break;
//						index = myatoi(ept+1);
//
//						// make sure export indices go in order
//						if (index != sim_verilognamelisthigh[namelistcount]+1)
//							break;
//						if (index > sim_verilognamelisthigh[namelistcount])
//							sim_verilognamelisthigh[namelistcount] = index;
//						i = j;
//					}
//				}
//				*pt = save;
//				namelistcount++;
//			}
//		}
//
//		// make sure all names are unique
//		for(i=1; i<namelistcount; i++)
//		{
//			// single signal: give it that name
//			if (sim_verilognamelistlow[i] == sim_verilognamelisthigh[i])
//			{
//				infstr = initinfstr();
//				formatinfstr(infstr, x_("%s_%ld_"), sim_verilognamelist[i], sim_verilognamelistlow[i]);
//				(void)reallocstring(&sim_verilognamelist[i], returninfstr(infstr), sim_tool->cluster);
//			}
//
//			// see if it clashes
//			for(j=0; j<i; j++)
//				if (namesame(sim_verilognamelist[i], sim_verilognamelist[j]) == 0) break;
//			if (j < i)
//			{
//				// name the same: rename
//				pt = 0;
//				for(k=1; k<1000; k++)
//				{
//					infstr = initinfstr();
//					formatinfstr(infstr, x_("%s_%ld"), sim_verilognamelist[i], k);
//					pt = returninfstr(infstr);
//					for(j=0; j<namelistcount; j++)
//						if (namesame(pt, sim_verilognamelist[j]) == 0) break;
//					if (j >= namelistcount) break;
//				}
//				(void)reallocstring(&sim_verilognamelist[i], pt, sim_tool->cluster);
//			}
//		}
//
//		*namelist = sim_verilognamelist;
//		*lowindex = sim_verilognamelistlow;
//		*highindex = sim_verilognamelisthigh;
//		*tempval = sim_verilognamelisttempval;
//		*wirelist = sim_verilogwirelist;
//		*netcount = wirecount;
//		return(namelistcount);
//	}

	/*
	 * Helper routine for "esort" that makes networks with names go in ascending name order.
	 */
//	int sim_versortwirelistsbyname(const void *e1, const void *e2)
//	{
//		REGISTER WIRELIST *w1, *w2;
//		REGISTER NETWORK *net1, *net2;
//		REGISTER CHAR *pt1, *pt2;
//		CHAR empty[1];
//
//		w1 = (WIRELIST *)e1;
//		w2 = (WIRELIST *)e2;
//		net1 = w1->net;
//		net2 = w2->net;
//		if (net1->temp1 != net2->temp1) return(net1->temp1 - net2->temp1);
//		empty[0] = 0;
//		if (net1->globalnet > 1 && net1->globalnet < net1->parent->globalnetcount)
//		{
//			pt1 = net1->parent->globalnetnames[net1->globalnet];
//		} else
//		{
//			if (net1->namecount == 0) pt1 = empty; else pt1 = networkname(net1, 0);
//		}
//		if (net2->globalnet > 1 && net2->globalnet < net2->parent->globalnetcount)
//		{
//			pt2 = net2->parent->globalnetnames[net2->globalnet];
//		} else
//		{
//			if (net2->namecount == 0) pt2 = empty; else pt2 = networkname(net2, 0);
//		}
//		return(namesamenumeric(pt1, pt2));
//	}

	/*
	 * Routine to return the network connected to node "ni", port "pp".
	 */
//	NETWORK *sim_vergetnetonport(NODEINST *ni, PORTPROTO *pp)
//	{
//		REGISTER PORTARCINST *pi;
//		REGISTER PORTEXPINST *pe;
//
//		for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//			if (pi->proto == pp) return(pi->conarcinst->network);
//		for(pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst)
//			if (pe->proto == pp) return(pe->exportproto->network);
//		return(NONETWORK);
//	}

	/*
	 * Routine to recursively examine cells and gather global network names.
	 */
//	void sim_vergatherglobals(NODEPROTO *np)
//	{
//		REGISTER INTBIG i, newtotal, globalnet;
//		REGISTER UINTBIG *newchars;
//		NETWORK *net;
//		REGISTER CHAR *gname, **newlist;
//		REGISTER NODEPROTO *onp, *cnp;
//		REGISTER NODEINST *ni;
//		REGISTER PORTARCINST *pi;
//
//		if (np->temp1 != 0) return;
//		np->temp1 = 1;
//
//		// mark all exported nets
//		for(ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			if (ni->proto != sch_globalprim) continue;
//			pi = ni->firstportarcinst;
//			if (pi == NOPORTARCINST) continue;
//			net = pi->conarcinst->network;
//			if (net == NONETWORK) continue;	
//			globalnet = net->globalnet;
//			if (globalnet < 2) continue;
//
//			// global net found: see if it is already in the list
//			gname = sim_verconvertname(np->globalnetnames[globalnet]);
//			for(i=0; i<sim_verilogglobalnetcount; i++)
//				if (namesame(gname, sim_verilogglobalnets[i]) == 0) break;
//			if (i < sim_verilogglobalnetcount) continue;
//
//			// add the global net name
//			if (sim_verilogglobalnetcount >= sim_verilogglobalnettotal)
//			{
//				newtotal = sim_verilogglobalnettotal * 2;
//				if (sim_verilogglobalnetcount >= newtotal)
//					newtotal = sim_verilogglobalnetcount + 5;
//				newlist = (CHAR **)emalloc(newtotal * (sizeof (CHAR *)), sim_tool->cluster);
//				if (newlist == 0) return;
//				newchars = (UINTBIG *)emalloc(newtotal * SIZEOFINTBIG, sim_tool->cluster);
//				if (newchars == 0) return;
//				for(i=0; i<sim_verilogglobalnettotal; i++)
//				{
//					newlist[i] = sim_verilogglobalnets[i];
//					newchars[i] = sim_verilogglobalchars[i];
//				}
//				for(i=sim_verilogglobalnettotal; i<newtotal; i++)
//					newlist[i] = 0;
//				if (sim_verilogglobalnettotal > 0)
//				{
//					efree((CHAR *)sim_verilogglobalnets);
//					efree((CHAR *)sim_verilogglobalchars);
//				}
//				sim_verilogglobalnets = newlist;
//				sim_verilogglobalchars = newchars;
//				sim_verilogglobalnettotal = newtotal;
//			}
//			if (sim_verilogglobalnets[sim_verilogglobalnetcount] != 0)
//				efree((CHAR *)sim_verilogglobalnets[sim_verilogglobalnetcount]);
//			(void)allocstring(&sim_verilogglobalnets[sim_verilogglobalnetcount], gname,
//				sim_tool->cluster);
//
//			// should figure out the characteristics of this global!!!
//			sim_verilogglobalchars[sim_verilogglobalnetcount] =
//				((ni->userbits&NTECHBITS) >> NTECHBITSSH) << STATEBITSSH;
//			sim_verilogglobalnetcount++;
//		}
//
//		for(ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			onp = ni->proto;
//			if (onp->primindex != 0) continue;
//
//			// ignore recursive references (showing icon in contents)
//			if (isiconof(onp, np)) continue;
//
//			if (onp->cellview == el_iconview)
//			{
//				cnp = contentsview(onp);
//				if (cnp != NONODEPROTO) onp = cnp;
//			}
//
//			sim_vergatherglobals(onp);
//		}
//	}

	/*************************************** READING VCD DUMPS INTO WAVEFORM ***************************************/

//	void sim_verfreesimdata(void)
//	{
//		REGISTER VERSIGNAL *vs;
//
//		while (sim_verfirstsignal != NOVERSIGNAL)
//		{
//			vs = sim_verfirstsignal;
//			sim_verfirstsignal = vs->nextversignal;
//			if (vs->total > 0)
//			{
//				efree((CHAR *)vs->stimtime);
//				efree((CHAR *)vs->stimstate);
//			}
//			if (vs->width > 1) efree((CHAR *)vs->signals);
//			efree((CHAR *)vs);
//		}
//	}

//	void sim_verparsefile(CHAR *file, NODEPROTO *cell)
//	{
//		REGISTER INTBIG i, len, numsignals, curposition, hashtablesize, hashcode, units, width,
//			namelen, symbollen, contextlen, structlen, structsize;
//		REGISTER INTSML state=0;
//		REGISTER double curtime;
//		REGISTER NODEINST *ni;
//		REGISTER BOOLEAN foundlevel, foundend;
//		REGISTER VARIABLE *var;
//		REGISTER CHAR *keyword, *symname, *nodename, *symbol, *name, *context;
//		REGISTER VERSIGNAL *vs, *vslast, **verhash, **vslist, *subvs;
//		NODEPROTO *np;
//		REGISTER STRINGOBJ *sosymbol, *soname, *socontext;
//		REGISTER void *infstr;
//		double min, max;
//		REGISTER UCHAR1 *onestruct;
//		CHAR *filename, *pt, *pars[10];
//		extern COMCOMP us_showdp;
//
//		sim_verfd = xopen(file, el_filetypetext, 0, &filename);
//		if (sim_verfd == 0) return;
//
//		// show the progress dialog
//		sim_verfilesize = filesize(sim_verfd);
//		if (sim_verfilesize > 0)
//		{
//			sim_verprogressdialog = DiaInitProgress(x_(""), _("Reading dump file..."));
//			if (sim_verprogressdialog == 0)
//			{
//				xclose(sim_verfd);
//				return;
//			}
//		}
//		DiaSetProgress(sim_verprogressdialog, 0, sim_verfilesize);
//
//		sim_timescale = 1.0;
//		sim_vercurscope[0] = 0;
//		sim_verlineno = 0;
//		sim_vercurlevel = 0;
//		numsignals = 0;
//		verhash = 0;
//		hashtablesize = 1;
//		vslast = NOVERSIGNAL;
//		sim_verfreesimdata();
//		sosymbol = newstringobj(sim_tool->cluster);
//		soname = newstringobj(sim_tool->cluster);
//		socontext = newstringobj(sim_tool->cluster);
//		for(;;)
//		{
//			if (xfgets(sim_verline, 300, sim_verfd)) break;
//			sim_verlineno++;
//			if ((sim_verlineno%100) == 0)
//			{
//				if (stopping(STOPREASONSIMULATE)) break;
//				curposition = xtell(sim_verfd);
//				DiaSetProgress(sim_verprogressdialog, curposition, sim_verfilesize);
//			}
//
//			// accumulate the scope name
//			pt = sim_verline;
//			keyword = getkeyword(&pt, x_(" "));
//			if (keyword == NOSTRING) continue;
//
//			// ignore "$date", "$version" or "$timescale"
//			if (namesame(keyword, x_("$date")) == 0 ||
//				namesame(keyword, x_("$version")) == 0)
//			{
//				sim_verparsetoend(&pt);
//				continue;
//			}
//			if (namesame(keyword, x_("$timescale")) == 0)
//			{
//				if (xfgets(sim_verline, 300, sim_verfd)) break;
//				sim_verlineno++;
//				units = -1;
//				pt = sim_verline;
//				keyword = getkeyword(&pt, x_(" "));
//				for(pt = keyword; *pt != 0; pt++)
//					if (!isdigit(*pt)) break;
//				if (*pt == 0)
//				{
//					ttyputerr(_("No time units on line %ld"), pt, sim_verlineno);
//				} else
//				{
//					if (namesame(pt, "ps") == 0) units = INTTIMEUNITPSEC; else
//					if (namesame(pt, "s") == 0) units = INTTIMEUNITSEC; else
//						ttyputerr(_("Unknown time units: '%s' on line %ld"), pt, sim_verlineno);
//				}
//				if (units >= 0)
//				{
//					*pt = 0;
//					sim_timescale = figureunits(keyword, VTUNITSTIME, units);
//				}
//				sim_verparsetoend(&pt);
//				continue;
//			}
//			if (namesame(keyword, x_("$scope")) == 0)
//			{
//				keyword = getkeyword(&pt, x_(" "));
//				if (keyword != NOSTRING)
//				{
//					if (namesame(keyword, x_("module")) == 0 || namesame(keyword, x_("task")) == 0 ||
//						namesame(keyword, x_("function")) == 0)
//					{
//						keyword = getkeyword(&pt, x_(" "));
//						if (keyword != NOSTRING)
//						{
//							if (sim_vercurscope[0] != 0) strcat(sim_vercurscope, x_("."));
//							strcat(sim_vercurscope, keyword);
//							sim_vercurlevel++;
//						}
//					}
//					sim_verparsetoend(&pt);
//				}
//				continue;
//			}
//
//			if (namesame(keyword, x_("$upscope")) == 0)
//			{
//				if (sim_vercurlevel <= 0 || sim_vercurscope[0] == 0)
//				{
//					ttyputerr(_("Unbalanced $upscope on line %ld"), sim_verlineno);
//					continue;
//				} else
//				{
//					len = strlen(sim_vercurscope);
//					for(i=len-1; i>0; i--) if (sim_vercurscope[i] == '.') break;
//					if (sim_vercurscope[i] == '.') sim_vercurscope[i] = 0;
//					sim_vercurlevel--;
//				}
//				sim_verparsetoend(&pt);
//				continue;
//			}
//
//			if (namesame(keyword, x_("$var")) == 0)
//			{
//				keyword = getkeyword(&pt, x_(" "));
//				if (keyword != NOSTRING)
//				{
//					if (namesame(keyword, x_("wire")) == 0 ||
//						namesame(keyword, x_("supply0")) == 0 ||
//						namesame(keyword, x_("supply1")) == 0 ||
//						namesame(keyword, x_("reg")) == 0 ||
//						namesame(keyword, x_("parameter")) == 0 ||
//						namesame(keyword, x_("trireg")) == 0)
//					{
//						// get the bus width
//						keyword = getkeyword(&pt, x_(" "));
//						if (keyword == NOSTRING) continue;
//						width = myatoi(keyword);
//
//						// get the symbol name for this signal
//						keyword = getkeyword(&pt, x_(" "));
//						if (keyword == NOSTRING) continue;
//						clearstringobj(sosymbol);
//						addstringtostringobj(keyword, sosymbol);
//
//						// get the signal name
//						keyword = getkeyword(&pt, x_(" "));
//						if (keyword == NOSTRING) continue;
//						clearstringobj(soname);
//						addstringtostringobj(keyword, soname);
//
//						// see if there is an index
//						keyword = getkeyword(&pt, x_(" "));
//						if (keyword == NOSTRING) continue;
//						foundend = FALSE;
//						if (namesame(keyword, x_("$end")) == 0) foundend = TRUE; else
//							addstringtostringobj(keyword, soname);
//
//						// set the context
//						clearstringobj(socontext);
//						addstringtostringobj(sim_vercurscope, socontext);
//
//						// allocate one big object with structure and names
//						structlen = sizeof (VERSIGNAL);
//						symbol = getstringobj(sosymbol);   symbollen = (estrlen(symbol)+1) * SIZEOFCHAR;
//						name = getstringobj(soname);       namelen = (estrlen(name)+1) * SIZEOFCHAR;
//						context = getstringobj(socontext); contextlen = (estrlen(context)+1) * SIZEOFCHAR;
//						structsize = structlen + symbollen + namelen + contextlen;
//						onestruct = (UCHAR1 *)emalloc(structsize, sim_tool->cluster);
//						if (onestruct == 0) continue;
//						vs = (VERSIGNAL *)onestruct;
//						vs->symbol = (CHAR *)&onestruct[structlen];
//						vs->signalname = (CHAR *)&onestruct[structlen+symbollen];
//						vs->signalcontext = (CHAR *)&onestruct[structlen+symbollen+namelen];
//						strcpy(vs->symbol, symbol);
//						strcpy(vs->signalname, name);
//						strcpy(vs->signalcontext, context);
//						vs->nextversignal = NOVERSIGNAL;
//						if (vslast == NOVERSIGNAL) sim_verfirstsignal = vs; else
//							vslast->nextversignal = vs;
//						vslast = vs;
//						vs->level = sim_vercurlevel;
//						vs->flags = 0;
//						vs->signal = 0;
//						vs->total = 0;
//						vs->count = 0;
//						vs->width = width;
//						vs->realversignal = NOVERSIGNAL;
//						numsignals++;
//
//						if (width > 1)
//						{
//							// create fake signals for the individual entries
//							vslist = (VERSIGNAL **)emalloc(width * (sizeof (VERSIGNAL *)), sim_tool->cluster);
//							if (vslist == 0) continue;
//							vs->signals = vslist;
//							for(i=0; i<width; i++)
//							{
//								structlen = sizeof (VERSIGNAL);
//								infstr = initinfstr();
//								formatinfstr(infstr, x_("%s[%ld]"), vs->signalname, i);
//								name = returninfstr(infstr);       namelen = (estrlen(name)+1) * SIZEOFCHAR;
//								context = sim_vercurscope;         contextlen = (estrlen(context)+1) * SIZEOFCHAR;
//								structsize = structlen + namelen + contextlen;
//								onestruct = (UCHAR1 *)emalloc(structsize, sim_tool->cluster);
//								if (onestruct == 0) break;
//								subvs = (VERSIGNAL *)onestruct;
//								subvs->symbol = 0;
//								subvs->signalname = (CHAR *)&onestruct[structlen];
//								subvs->signalcontext = (CHAR *)&onestruct[structlen+namelen];
//								strcpy(subvs->signalname, name);
//								strcpy(subvs->signalcontext, context);
//								subvs->nextversignal = NOVERSIGNAL;
//								if (vslast == NOVERSIGNAL) sim_verfirstsignal = subvs; else
//									vslast->nextversignal = subvs;
//								vslast = subvs;
//								subvs->level = sim_vercurlevel;
//								subvs->flags = 0;
//								subvs->signal = 0;
//								subvs->total = 0;
//								subvs->count = 0;
//								subvs->width = 1;
//								subvs->realversignal = NOVERSIGNAL;
//								vslist[i] = subvs;
//								numsignals++;
//							}
//						}
//						if (foundend) continue;
//					} else
//					{
//						ttyputerr(_("Invalid $var on line %ld: %s"), sim_verlineno, sim_verline);
//						continue;
//					}
//				}
//				sim_verparsetoend(&pt);
//				continue;
//			}
//
//			if (namesame(keyword, x_("$enddefinitions")) == 0)
//			{
//				sim_verparsetoend(&pt);
//				infstr = initinfstr();
//				formatinfstr(infstr, _("Found %ld signal names"), numsignals);
//				DiaSetTextProgress(sim_verprogressdialog, _("Building signal table..."));
//				DiaSetCaptionProgress(sim_verprogressdialog, returninfstr(infstr));
//
//				// build a table for finding signal names
//				for(i=0; i<256; i++) sim_charused[i] = -1;
//				sim_numcharsused = 0;
//				for(vs = sim_verfirstsignal; vs != NOVERSIGNAL; vs = vs->nextversignal)
//				{
//					if (vs->symbol == 0) continue;
//					for(pt = vs->symbol; *pt != 0; pt++)
//					{
//						i = *pt & 0xFF;
//						if (sim_charused[i] < 0)
//							sim_charused[i] = ++sim_numcharsused;
//					}
//				}
//
//				hashtablesize = pickprime(numsignals*2);
//				verhash = (VERSIGNAL **)emalloc(hashtablesize * (sizeof (VERSIGNAL *)), sim_tool->cluster);
//				if (verhash == 0) return;
//				for(i=0; i<hashtablesize; i++) verhash[i] = NOVERSIGNAL;
//
//				// insert the signals
//				for(vs = sim_verfirstsignal; vs != NOVERSIGNAL; vs = vs->nextversignal)
//				{
//					if (vs->symbol == 0) continue;
//					hashcode = sim_vergetsignalhash(vs->symbol) % hashtablesize;
//					for(i=0; i<hashtablesize; i++)
//					{
//						if (verhash[hashcode] == NOVERSIGNAL)
//						{
//							verhash[hashcode] = vs;
//							break;
//						}
//						if (strcmp(vs->symbol, verhash[hashcode]->symbol) == 0)
//						{
//							// same symbol name: merge the signals
//							vs->realversignal = verhash[hashcode];
//							break;
//						}
//						hashcode++;
//						if (hashcode >= hashtablesize) hashcode = 0;
//					}
//				}
//				DiaSetTextProgress(sim_verprogressdialog, _("Reading stimulus..."));
//				continue;
//			}
//			if (namesame(keyword, x_("$dumpvars")) == 0)
//			{
//				curtime = 0.0;
//				for(;;)
//				{
//					if (xfgets(sim_verline, 300, sim_verfd)) break;
//					sim_verlineno++;
//					if ((sim_verlineno%1000) == 0)
//					{
//						if (stopping(STOPREASONSIMULATE)) break;
//						curposition = xtell(sim_verfd);
//						DiaSetProgress(sim_verprogressdialog, curposition, sim_verfilesize);
//					}
//					if (sim_verline[0] == '0' || sim_verline[0] == '1' ||
//						sim_verline[0] == 'x' || sim_verline[0] == 'z')
//					{
//						symname = &sim_verline[1];
//						hashcode = sim_vergetsignalhash(symname) % hashtablesize;
//						vs = NOVERSIGNAL;
//						for(i=0; i<hashtablesize; i++)
//						{
//							vs = verhash[hashcode];
//							if (vs == NOVERSIGNAL) break;
//							if (strcmp(symname, vs->symbol) == 0) break;
//							hashcode++;
//							if (hashcode >= hashtablesize) hashcode = 0;
//						}
//						if (vs == NOVERSIGNAL)
//						{
//							ttyputmsg(_("Unknown symbol '%s' on line %ld"), symname, sim_verlineno);
//							continue;
//						}
//
//						// insert the stimuli
//						switch (sim_verline[0])
//						{
//							case '0': state = (LOGIC_LOW << 8) | GATE_STRENGTH;   break;
//							case '1': state = (LOGIC_HIGH << 8) | GATE_STRENGTH;  break;
//							case 'x': state = (LOGIC_X << 8) | GATE_STRENGTH;     break;
//							case 'z': state = (LOGIC_Z << 8) | GATE_STRENGTH;     break;
//						}
//						sim_versetvalue(vs, curtime, state);
//						continue;
//					}
//					if (sim_verline[0] == '$')
//					{
//						if (namesame(&sim_verline[0], x_("$end")) == 0) continue;
//						ttyputmsg(_("Unknown directive on line %ld: %s"), sim_verlineno, sim_verline);
//						continue;
//					}
//					if (sim_verline[0] == '#')
//					{
//						curtime = myatoi(&sim_verline[1]) * sim_timescale;
//						continue;
//					}
//					if (sim_verline[0] == 'b')
//					{
//						for(pt = &sim_verline[1]; *pt != 0; pt++)
//							if (*pt == ' ') break;
//						if (*pt == 0)
//						{
//							ttyputmsg(_("Bus has missing signal name on line %ld: %s"), sim_verlineno, sim_verline);
//							continue;
//						}
//						symname = &pt[1];
//						hashcode = sim_vergetsignalhash(symname) % hashtablesize;
//						vs = NOVERSIGNAL;
//						for(i=0; i<hashtablesize; i++)
//						{
//							vs = verhash[hashcode];
//							if (vs == NOVERSIGNAL) break;
//							if (strcmp(symname, vs->symbol) == 0) break;
//							hashcode++;
//							if (hashcode >= hashtablesize) hashcode = 0;
//						}
//						if (vs == NOVERSIGNAL)
//						{
//							ttyputmsg(_("Unknown symbol '%s' on line %ld"), symname, sim_verlineno);
//							continue;
//						}
//						for(i=0; i<vs->width; i++)
//						{
//							switch (sim_verline[i+1])
//							{
//								case '0': state = (LOGIC_LOW << 8) | GATE_STRENGTH;   break;
//								case '1': state = (LOGIC_HIGH << 8) | GATE_STRENGTH;  break;
//								case 'x': state = (LOGIC_X << 8) | GATE_STRENGTH;     break;
//								case 'z': state = (LOGIC_Z << 8) | GATE_STRENGTH;     break;
//							}
//							sim_versetvalue(vs->signals[i], curtime, state);
//						}
//						continue;
//					}
//					ttyputmsg(_("Unknown stimulus on line %ld: %s"), sim_verlineno, sim_verline);
//				}
//			}
//		}
//		DiaDoneProgress(sim_verprogressdialog);
//		xclose(sim_verfd);
//		if (verhash != 0) efree((CHAR *)verhash);
//		killstringobj(sosymbol);
//		killstringobj(soname);
//		killstringobj(socontext);
//
//		// pick an associated cell
//		if (cell == NONODEPROTO)
//		{
//			i = ttygetparam(_("Please select a cell to associate with this dump file: "),
//				&us_showdp, 3, pars);
//			if (i > 0) cell = getnodeproto(pars[0]);
//			if (cell == NONODEPROTO) return;
//		}
//
//		// remember signal list stored on this cell
//		sim_window_grabcachedsignalsoncell(cell);
//
//		// display the waveform window
//		i = sim_window_isactive(&np);
//		if ((i&SIMWINDOWWAVEFORM) == 0)
//		{
//			if (i != 0 && np != cell)
//			{
//				// stop simulation of cell "np"
//				sim_window_stopsimulation();
//			}
//			if (sim_window_create(0, cell,
//				((sim_window_state&SHOWWAVEFORM) != 0 ? sim_vercharhandlerwave : 0),
//					us_charhandler, IRSIM)) return;
//			sim_window_state = (sim_window_state & ~SIMENGINECUR) | SIMENGINECURVERILOG;
//			sim_window_settimerange(0, 0.0, DEFIRSIMTIMERANGE);
//			sim_window_setmaincursor(DEFIRSIMTIMERANGE/5.0*2.0);
//			sim_window_setextensioncursor(DEFIRSIMTIMERANGE/5.0*3.0);
//		}
//
//		// find a subcell and set the initial simulation level from it
//		sim_vercurlevel = -1;
//		for(ni = cell->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			if (ni->proto->primindex != 0) continue;
//			if (isiconof(ni->proto, cell)) continue;
//			var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, el_node_name_key);
//			if (var != NOVARIABLE)
//			{
//				nodename = (CHAR *)var->addr;
//				for(sim_vercurlevel=2; sim_vercurlevel<4; sim_vercurlevel++)
//				{
//					foundlevel = FALSE;
//					for(vs = sim_verfirstsignal; vs != NOVERSIGNAL; vs = vs->nextversignal)
//					{
//						if (vs->level != sim_vercurlevel) continue;
//						foundlevel = TRUE;
//						estrcpy(sim_vercurscope, vs->signalcontext);
//						len = estrlen(sim_vercurscope);
//						for(i=len-1; i>0; i--) if (sim_vercurscope[i] == '.') break;
//						if (i < 0) continue;
//						if (namesame(&sim_vercurscope[i+1], nodename) == 0) break;
//					}
//					if (!foundlevel)
//					{
//						sim_vercurlevel = -1;
//						break;
//					}
//					if (vs != NOVERSIGNAL)
//					{
//						sim_vercurlevel--;
//						sim_vercurscope[i] = 0;
//						break;
//					}
//				}
//				if (sim_vercurlevel >= 0 && sim_vercurlevel < 4) break;
//			}
//		}
//		if (sim_vercurlevel < 0)
//		{
//			// no subcell found: just start at the top level
//			sim_vercurlevel = 1;
//			for(vs = sim_verfirstsignal; vs != NOVERSIGNAL; vs = vs->nextversignal)
//			{
//				if (vs->level != sim_vercurlevel) continue;
//				estrcpy(sim_vercurscope, vs->signalcontext);
//				break;
//			}
//		}
//		sim_verignoresigname = sim_vercurlevel - 1;
//
//		// show current level
//		sim_vershowcurrentlevel(cell);
//		sim_window_gettimeextents(&min, &max);
//		sim_window_settimerange(0, min, max);
//		sim_window_redraw();
//	}

//	void sim_versetvalue(VERSIGNAL *vs, double curtime, INTSML state)
//	{
//		REGISTER INTBIG newtotal, i;
//		REGISTER INTSML *newstate;
//		REGISTER double *newtime;
//
//		if (vs->count >= vs->total)
//		{
//			newtotal = vs->total * 2;
//			if (vs->count >= newtotal) newtotal = vs->count + 50;
//			newtime = (double *)emalloc(newtotal * (sizeof (double)), sim_tool->cluster);
//			newstate = (INTSML *)emalloc(newtotal * SIZEOFINTSML, sim_tool->cluster);
//			if (newtime == 0 || newstate == 0) return;
//			for(i=0; i<vs->count; i++)
//			{
//				newtime[i] = vs->stimtime[i];
//				newstate[i] = vs->stimstate[i];
//			}
//			if (vs->total > 0)
//			{
//				efree((CHAR *)vs->stimtime);
//				efree((CHAR *)vs->stimstate);
//			}
//			vs->total = newtotal;
//			vs->stimtime = newtime;
//			vs->stimstate = newstate;
//		}
//		vs->stimtime[vs->count] = curtime;
//		vs->stimstate[vs->count] = state;
//		vs->count++;
//	}
//
//	INTBIG sim_vergetsignalhash(CHAR *name)
//	{
//		REGISTER INTBIG value, index, i, len;
//
//		value = 0;
//		len = estrlen(name);
//		for(i=len-1; i>=0; i--)
//		{
//			index = sim_charused[name[i] & 0xFF];
//			value = (value * sim_numcharsused) + index;
//		}
//		return(value);
//	}

//	void sim_vershowcurrentlevel(NODEPROTO *cell)
//	{
//		REGISTER INTBIG tot, i, j, buscount, plainnet, numnets, namednet, shownsignals,
//			busprefixlen, oldsigcount;
//		REGISTER CHAR *pt, *opt, *start, save, *name, *busprefix;
//		CHAR **oldsignames;
//		REGISTER void *infstr;
//		REGISTER VERSIGNAL *vs, **netlist, *realvs, *subvs;
//		INTBIG *bussignals;
//		Q_UNUSED( cell );
//
//		// show the waveform window
//		sim_window_cleartracehighlight();
//		sim_window_killalltraces(FALSE);
//		infstr = initinfstr();
//		formatinfstr(infstr, _("level=%s"), sim_vercurscope);
//		pt = returninfstr(infstr);
//		for(j=0; j<sim_verignoresigname; j++)
//		{
//			while (*pt != 0 && *pt != '.') pt++;
//			if (*pt == '.') pt++;
//		}
//		sim_window_titleinfo(pt);
//		for(vs = sim_verfirstsignal; vs != NOVERSIGNAL; vs = vs->nextversignal)
//		{
//			vs->flags &= ~VERSIGNALSHOWN;
//			vs->signal = 0;
//		}
//		// first show the signals saved from last time
//		oldsigcount = sim_window_getcachedsignals(&oldsignames);
//		shownsignals = 0;
//		for(j=0; j<oldsigcount; j++)
//		{
//			// see if the name is a bus
//			for(pt = oldsignames[j]; *pt != 0; pt++) if (*pt == '\t') break;
//			if (*pt == '\t')
//			{
//				// a bus
//				pt++;
//				sim_initbussignals();
//				for(;;)
//				{
//					for(start = pt; *pt != 0; pt++) if (*pt == '\t') break;
//					save = *pt;
//					*pt = 0;
//					opt = start;
//					if (*opt == '-') opt++;
//					for( ; *opt != 0; opt++)
//						if (!isdigit(*opt) || *opt == ':') break;
//					if (*opt == ':') start = opt+1;
//					for(vs = sim_verfirstsignal; vs != NOVERSIGNAL; vs = vs->nextversignal)
//					{
//						name = vs->signalname;
//						if (namesame(name, start) != 0) continue;
//						realvs = vs;
//						if (vs->realversignal != NOVERSIGNAL) realvs = vs->realversignal;
//						if ((realvs->flags&VERSIGNALSHOWN) != 0) continue;
//
//						realvs->signal = sim_window_newtrace(-1, name, (INTBIG)realvs);
//						sim_window_loaddigtrace(realvs->signal, realvs->count, realvs->stimtime, realvs->stimstate);
//						realvs->flags |= VERSIGNALSHOWN;
//						sim_addbussignal(realvs->signal);
//						shownsignals++;
//						break;
//					}
//					*pt++ = save;
//					if (save == 0) break;
//				}
//
//				// create the bus
//				infstr = initinfstr();
//				for(pt = oldsignames[j]; *pt != 0; pt++)
//				{
//					if (*pt == '\t') break;
//					addtoinfstr(infstr, *pt);
//				}
//				pt = returninfstr(infstr);
//				buscount = sim_getbussignals(&bussignals);
//				(void)sim_window_makebus(buscount, bussignals, pt);
//			} else
//			{
//				// a single signal
//				pt = oldsignames[j];
//				if (*pt == '-') pt++;
//				for( ; *pt != 0; pt++)
//					if (!isdigit(*pt) || *pt == ':') break;
//				if (*pt == ':') pt++; else pt = oldsignames[j];
//				for(vs = sim_verfirstsignal; vs != NOVERSIGNAL; vs = vs->nextversignal)
//				{
//					name = vs->signalname;
//					if (namesame(name, pt) != 0) continue;
//					realvs = vs;
//					if (vs->realversignal != NOVERSIGNAL) realvs = vs->realversignal;
//					if ((realvs->flags&VERSIGNALSHOWN) != 0) continue;
//
//					realvs->signal = sim_window_newtrace(-1, name, (INTBIG)realvs);
//					sim_window_loaddigtrace(realvs->signal, realvs->count, realvs->stimtime, realvs->stimstate);
//					realvs->flags |= VERSIGNALSHOWN;
//					shownsignals++;
//					break;
//				}
//			}
//		}
//
//		// show default signals if none are cached
//		if (oldsigcount == 0)
//		{
//			// see how many signals of each type are left
//			plainnet = namednet = 0;
//			for(vs = sim_verfirstsignal; vs != NOVERSIGNAL; vs = vs->nextversignal)
//			{
//				if (vs->symbol == 0) continue;
//				if (vs->level != sim_vercurlevel) continue;
//				if (estrcmp(sim_vercurscope, vs->signalcontext) != 0) continue;
//				realvs = vs;
//				if (vs->realversignal != NOVERSIGNAL) realvs = vs->realversignal;
//				if ((realvs->flags&VERSIGNALSHOWN) != 0) continue;
//				if (namesamen(vs->signalname, x_("net"), 3) == 0) plainnet++; else
//					namednet++;
//			}
//			numnets = namednet;
//			if (numnets == 0 && shownsignals == 0)
//				numnets = plainnet;
//			if (numnets > 0)
//			{
//				netlist = (VERSIGNAL **)emalloc(numnets * (sizeof (VERSIGNAL *)), sim_tool->cluster);
//				if (netlist == 0) return;
//				tot = 0;
//				for(vs = sim_verfirstsignal; vs != NOVERSIGNAL; vs = vs->nextversignal)
//				{
//					if (vs->symbol == 0) continue;
//					if (vs->level != sim_vercurlevel) continue;
//					if (estrcmp(sim_vercurscope, vs->signalcontext) != 0) continue;
//					realvs = vs;
//					if (vs->realversignal != NOVERSIGNAL) realvs = vs->realversignal;
//					if ((realvs->flags&VERSIGNALSHOWN) != 0) continue;
//					if (namednet > 0)
//					{
//						if (namesamen(vs->signalname, x_("net"), 3) == 0) continue;
//					}
//					netlist[tot++] = vs;
//				}
//				esort(netlist, tot, sizeof (VERSIGNAL *), sim_versortsignalnames);
//				busprefixlen = 0;
//				busprefix = 0;
//				sim_initbussignals();
//				for(j=0; j<tot; j++)
//				{
//					vs = netlist[j];
//					realvs = vs;
//					if (vs->realversignal != NOVERSIGNAL) realvs = vs->realversignal;
//
//					// if in a bus, see if it is done
//					if (busprefixlen > 0 && (namesamen(vs->signalname, busprefix, busprefixlen) != 0 ||
//						vs->signalname[busprefixlen] != '[' || realvs->width > 1))
//					{
//						buscount = sim_getbussignals(&bussignals);
//						save = busprefix[busprefixlen];
//						busprefix[busprefixlen] = 0;
//						(void)sim_window_makebus(buscount, bussignals, busprefix);
//						busprefix[busprefixlen] = save;
//						busprefixlen = 0;
//						sim_initbussignals();
//					}
//					if (realvs->width > 1)
//					{
//						// show a bus
//						sim_initbussignals();
//						for(i=0; i<realvs->width; i++)
//						{
//							subvs = realvs->signals[i];
//							subvs->signal = sim_window_newtrace(-1, subvs->signalname, (INTBIG)subvs);
//							sim_window_loaddigtrace(subvs->signal, subvs->count, subvs->stimtime, subvs->stimstate);
//							subvs->flags |= VERSIGNALSHOWN;
//							sim_addbussignal(subvs->signal);
//						}
//						buscount = sim_getbussignals(&bussignals);
//						(void)sim_window_makebus(buscount, bussignals, vs->signalname);
//						sim_initbussignals();
//					} else
//					{
//						realvs->signal = sim_window_newtrace(-1, vs->signalname, (INTBIG)realvs);
//						sim_window_loaddigtrace(realvs->signal, realvs->count, realvs->stimtime, realvs->stimstate);
//						realvs->flags |= VERSIGNALSHOWN;
//						if (busprefixlen == 0)
//						{
//							for(i=0; vs->signalname[i] != 0; i++)
//								if (vs->signalname[i] == '[') break;
//							if (vs->signalname[i] == '[')
//							{
//								busprefix = vs->signalname;
//								busprefixlen = i;
//								sim_addbussignal(realvs->signal);
//							}
//						} else sim_addbussignal(realvs->signal);
//					}
//				}
//				if (busprefixlen > 0)
//				{
//					buscount = sim_getbussignals(&bussignals);
//					save = busprefix[busprefixlen];
//					busprefix[busprefixlen] = 0;
//					(void)sim_window_makebus(buscount, bussignals, busprefix);
//					busprefix[busprefixlen] = save;
//				}
//				efree((CHAR *)netlist);
//			}
//		}
//
//		sim_window_redraw();
//	}

//	int sim_versortsignalnames(const void *e1, const void *e2)
//	{
//		VERSIGNAL *vs1, *vs2;
//
//		vs1 = *((VERSIGNAL **)e1);
//		vs2 = *((VERSIGNAL **)e2);
//		return(namesamenumeric(vs1->signalname, vs2->signalname));
//	}

//	void sim_verparsetoend(CHAR **pt)
//	{
//		REGISTER CHAR *keyword;
//
//		for(;;)
//		{
//			keyword = getkeyword(pt, x_(" "));
//			if (keyword == NOSTRING) return;
//			if (*keyword == 0)
//			{
//				if (stopping(STOPREASONSIMULATE)) break;
//				if (xfgets(sim_verline, 300, sim_verfd)) break;
//				sim_verlineno++;
//				*pt = sim_verline;
//				continue;
//			}
//			if (namesame(keyword, x_("$end")) == 0) return;
//		}
//	}

	/*
	 * The character handler for the waveform window of ALS simulation
	 */
//	BOOLEAN sim_vercharhandlerwave(WINDOWPART *w, INTSML chr, INTBIG special)
//	{
//		NODEPROTO *np;
//		REGISTER INTBIG *highsigs, highsig, i, j, thispos, *bussigs,
//			trl, nexttr, prevtr, pos, buscount;
//		REGISTER VERSIGNAL *vs;
//
//		ttynewcommand();
//
//		// special characters are not handled here
//		if (special != 0)
//			return(us_charhandler(w, chr, special));
//
//		// can always preserve snapshot
//		if (chr == 'p')
//		{
//			sim_window_savegraph();
//			return(FALSE);
//		}
//
//		// can always do help
//		if (chr == '?')
//		{
//			sim_verhelpwindow();
//			return(FALSE);
//		}
//
//		// if not simulating, don't handle any simulation commands
//		if (sim_window_isactive(&np) == 0)
//			return(us_charhandler(w, chr, special));
//		switch (chr)
//		{
//			// convert busses
//			case 'b':
//				if (sim_window_buscommand()) return(FALSE);
//				highsigs = sim_window_gethighlighttraces();
//				return(FALSE);
//
//			// add trace
//			case 'a':
//				sim_veraddsignals();
//				return(FALSE);
//
//			case 'i':
//			case 'r':
//			case DELETEKEY:
//				break;
//			default:
//				return(FALSE);
//		}
//
//		// the following commands demand a current trace...get it
//		highsigs = sim_window_gethighlighttraces();
//		if (highsigs[0] == 0)
//		{
//			ttyputerr(_("Select a signal name first"));
//			return(FALSE);
//		}
//		if (chr == 'i')
//		{
//			for(j=0; highsigs[j] != 0; j++)
//			{
//				highsig = highsigs[j];
//				vs = (VERSIGNAL *)sim_window_gettracedata(highsig);
//				ttyputmsg("Signal %s has %d stimuli on it:", vs->signalname, vs->count);
//				for(i=0; i<vs->count; i++)
//				{
//					ttyputmsg("  %d at time %s (%g)", vs->stimstate[i],
//						sim_windowconvertengineeringnotation(vs->stimtime[i]), vs->stimtime[i]);
//				}
//			}
//			return(FALSE);
//		}
//		if (chr == 'r' || chr == DELETEKEY)		// remove trace(s)
//		{
//			sim_window_cleartracehighlight();
//
//			// delete them
//			nexttr = prevtr = 0;
//			for(j=0; highsigs[j] != 0; j++)
//			{
//				highsig = highsigs[j];
//				thispos = sim_window_gettraceframe(highsig);
//				bussigs = sim_window_getbustraces(highsig);
//				for(buscount=0; bussigs[buscount] != 0; buscount++) ;
//				sim_window_inittraceloop();
//				nexttr = prevtr = 0;
//				for(;;)
//				{
//					trl = sim_window_nexttraceloop();
//					if (trl == 0) break;
//					pos = sim_window_gettraceframe(trl);
//					if (pos > thispos)
//					{
//						if (pos-1 == thispos) nexttr = trl;
//						pos = pos - 1 + buscount;
//						sim_window_settraceframe(trl, pos);
//					} else if (pos == thispos-1) prevtr = trl;
//				}
//				if (buscount > 0)
//				{
//					for(i=0; i<buscount; i++)
//						sim_window_settraceframe(bussigs[i], thispos+i);
//				}
//
//				// remove from the simulator's list
//				for(vs = sim_verfirstsignal; vs != NOVERSIGNAL; vs = vs->nextversignal)
//				{
//					if (vs->signal == highsig)
//					{
//						vs->signal = 0;
//						break;
//					}
//				}
//
//				// kill trace
//				sim_window_killtrace(highsig);
//			}
//
//			// redraw
//			if (nexttr != 0)
//			{
//				sim_window_addhighlighttrace(nexttr);
//			} else if (prevtr != 0)
//			{
//				sim_window_addhighlighttrace(prevtr);
//			}
//			sim_window_redraw();
//			return(FALSE);
//		}
//
//		return(FALSE);
//	}

	// Simulation Signal Selection
//	static DIALOGITEM sim_sigselectdialogitems[] =
//	{
//	 /*  1 */ {0, {344,276,368,356}, BUTTON, N_("OK")},
//	 /*  2 */ {0, {344,184,368,264}, BUTTON, N_("Cancel")},
//	 /*  3 */ {0, {4,4,336,356}, SCROLLMULTI, x_("")},
//	 /*  4 */ {0, {348,4,364,176}, CHECK, N_("Show lower levels")}
//	};
//	static DIALOG sim_sigselectdialog = {{75,75,452,441}, N_("Select Signal for Simulation"), 0, 4, sim_sigselectdialogitems, 0, 0};

	/* special items for the "Simulation Signal Selection" dialog: */
//	#define DSSS_LIST            3		/* list of signals (scroll) */
//	#define DSSS_SHOWLOWER       4		/* show lower levels (check) */
//
//	BOOLEAN sim_signalshowlower = FALSE;

	/*
	 * Routine to prompt the user for signal names and add them to the waveform window.
	 */
//	void sim_veraddsignals(void)
//	{
//		REGISTER void *infstr, *dia;
//		CHAR *pt;
//		REGISTER INTBIG i, traces, itemHit, *list;
//		REGISTER VERSIGNAL *vs;
//
//		dia = DiaInitDialog(&sim_sigselectdialog);
//		if (dia == 0) return;
//		DiaInitTextDialog(dia, DSSS_LIST, sim_vertopofsignals, sim_vernextsignals, DiaNullDlogDone, 0,
//			SCSELMOUSE|SCSELKEY|SCHORIZBAR);
//		if (sim_signalshowlower) DiaSetControl(dia, DSSS_SHOWLOWER, 1);
//		infstr = initinfstr();
//		formatinfstr(infstr, "Signal in %s", sim_vercurscope);
//		ttyputmsg("%s", returninfstr(infstr));
//		for(;;)
//		{
//			itemHit = DiaNextHit(dia);
//			if (itemHit == OK || itemHit == CANCEL) break;
//			if (itemHit == DSSS_SHOWLOWER)
//			{
//				sim_signalshowlower = !sim_signalshowlower;
//				if (sim_signalshowlower) DiaSetControl(dia, DSSS_SHOWLOWER, 1); else
//					DiaSetControl(dia, DSSS_SHOWLOWER, 0);
//				DiaLoadTextDialog(dia, DSSS_LIST, sim_vertopofsignals, sim_vernextsignals, DiaNullDlogDone, 0);
//				continue;
//			}
//		}
//
//		if (itemHit == OK)
//		{
//			list = DiaGetCurLines(dia, DSSS_LIST);
//			for(i=0; list[i] >= 0; i++)
//			{
//				pt = DiaGetScrollLine(dia, DSSS_LIST, list[i]);
//
//				// find the signal
//				for(vs = sim_verfirstsignal; vs != NOVERSIGNAL; vs = vs->nextversignal)
//				{
//					if (vs->signal != 0) continue;
//					if (namesame(vs->signalcontext, sim_vercurscope) != 0) continue;
//					if (namesame(vs->signalname, pt) == 0) break;
//				}
//				if (vs == NOVERSIGNAL)
//				{
//					for(vs = sim_verfirstsignal; vs != NOVERSIGNAL; vs = vs->nextversignal)
//					{
//						if (vs->signal != 0) continue;
//						infstr = initinfstr();
//						formatinfstr(infstr, x_("%s.%s"), vs->signalcontext, vs->signalname);
//						if (namesame(pt, returninfstr(infstr)) == 0) break;
//					}
//				}
//				if (vs == NOVERSIGNAL) continue;
//
//				// ready to add: remove highlighting
//				sim_window_cleartracehighlight();
//
//				// count the number of traces
//				sim_window_inittraceloop();
//				for(traces=0; ; traces++) if (sim_window_nexttraceloop() == 0) break;
//
//				// create a new trace in the last slot
//				vs->signal = sim_window_newtrace(-1, vs->signalname, (INTBIG)vs);
//				sim_window_addhighlighttrace(vs->signal);
//				sim_window_setnumframes(sim_window_getnumframes()+1);
//				sim_window_loaddigtrace(vs->signal, vs->count, vs->stimtime, vs->stimstate);
//			}
//		}
//		sim_window_redraw();
//		DiaDoneDialog(dia);
//	}

//	BOOLEAN sim_vertopofsignals(CHAR **c)
//	{
//		Q_UNUSED( c );
//		sim_verwindow_iter = sim_verfirstsignal;
//		return(TRUE);
//	}

//	CHAR *sim_vernextsignals(void)
//	{
//		VERSIGNAL *vs;
//		REGISTER void *infstr;
//
//		for(;;)
//		{
//			vs = sim_verwindow_iter;
//			if (vs == NOVERSIGNAL) break;
//			sim_verwindow_iter = vs->nextversignal;
//			if (vs->signal != 0) continue;
//			if (vs->count <= 0) continue;
//			if (sim_signalshowlower)
//			{
//				if (vs->level < sim_vercurlevel) continue;
//				if (vs->level == sim_vercurlevel)
//				{
//					if (estrcmp(vs->signalcontext, sim_vercurscope) != 0) continue;
//					return(vs->signalname);
//				}
//				infstr = initinfstr();
//				formatinfstr(infstr, x_("%s.%s"), vs->signalcontext, vs->signalname);
//				return(returninfstr(infstr));
//			}
//			if (vs->level != sim_vercurlevel) continue;
//			if (estrcmp(vs->signalcontext, sim_vercurscope) != 0) continue;
//			return(vs->signalname);
//		}
//		return(0);
//	}

//	BOOLEAN sim_vertopofinstances(CHAR **c)
//	{
//		Q_UNUSED( c );
//		sim_verwindow_iter = sim_verfirstsignal;
//		sim_verwindow_lastiter = NOVERSIGNAL;
//		return(TRUE);
//	}

//	CHAR *sim_vernextinstance(void)
//	{
//		VERSIGNAL *vs;
//		REGISTER INTBIG len, curlen, i;
//
//		curlen = estrlen(sim_vercurscope);
//		for(;;)
//		{
//			vs = sim_verwindow_iter;
//			if (vs == NOVERSIGNAL) break;
//			sim_verwindow_iter = vs->nextversignal;
//			if (vs->level != sim_vercurlevel+1) continue;
//			if (estrncmp(vs->signalcontext, sim_vercurscope, curlen) != 0) continue;
//			if (vs->signalcontext[curlen] != '.') continue;
//			if (sim_verwindow_lastiter != NOVERSIGNAL &&
//				strcmp(vs->signalcontext, sim_verwindow_lastiter->signalcontext) == 0) continue;
//			sim_verwindow_lastiter = vs;
//			len = estrlen(vs->signalcontext);
//			for(i=len-1; i>0; i--)
//				if (vs->signalcontext[i] == '.') break;
//			if (i > 0) i++;
//			return(&vs->signalcontext[i]);
//		}
//		return(0);
//	}

//	void sim_verhelpwindow(void)
//	{
//		NODEPROTO *np;
//		INTBIG active;
//
//		active = sim_window_isactive(&np);
//		if (active == 0)
//		{
//			ttyputmsg(_("There is no current simulation"));
//			return;
//		}
//
//		ttyputmsg(_("These keys may be typed in the Verilog Waveform window:"));
//		ttyputinstruction(x_(" a"), 5, _("Add signal to simulation window"));
//		ttyputinstruction(x_(" r"), 5, _("Remove signal from the window"));
//		ttyputinstruction(x_(" b"), 5, _("Combine selected signals into a bus"));
//		ttyputinstruction(x_(" p"), 5, _("Preserve snapshot of simulation window in database"));
//	}

	/*
	 * Routine to move down the hierarchy into instance "level" and show
	 * signals at that level.
	 */
//	void sim_verlevel_set(CHAR *level, NODEPROTO *cell)
//	{
//		REGISTER VERSIGNAL *vs;
//		REGISTER INTBIG len;
//		REGISTER BOOLEAN foundlowerlevel;
//		REGISTER CHAR *lowerlevel;
//
//		// see if this name is in the data
//		if (level != 0)
//		{
//			foundlowerlevel = FALSE;
//			lowerlevel = 0;
//			len = estrlen(sim_vercurscope);
//			for(vs = sim_verfirstsignal; vs != NOVERSIGNAL; vs = vs->nextversignal)
//			{
//				if (vs->level != sim_vercurlevel+1) continue;
//				if (namesamen(vs->signalcontext, sim_vercurscope, len) != 0) continue;
//				if (vs->signalcontext[len] != '.') continue;
//
//				// check the lower level
//				if (namesame(&vs->signalcontext[len+1], level) == 0) break;
//				if (foundlowerlevel)
//				{
//					if (lowerlevel != 0)
//					{
//						if (namesame(lowerlevel, &vs->signalcontext[len+1]) != 0)
//							lowerlevel = 0;
//					}
//				} else
//				{
//					lowerlevel = &vs->signalcontext[len+1];
//					foundlowerlevel = TRUE;
//				}
//			}
//
//			if (vs == NOVERSIGNAL)
//			{
//				// did not find the lower level
//				if (lowerlevel != 0)
//				{
//					estrcat(sim_vercurscope, x_("."));
//					estrcat(sim_vercurscope, lowerlevel);
//					sim_vercurlevel++;
//				}
//			}
//
//			estrcat(sim_vercurscope, x_("."));
//			estrcat(sim_vercurscope, level);
//			sim_vercurlevel++;
//		}
//
//		sim_window_grabcachedsignalsoncell(cell);
//		sim_vershowcurrentlevel(cell);
//	}

	/*
	 * Routine to move up one level of hierarchy in the display of signal names.
	 */
//	void sim_verlevel_up(NODEPROTO *cell)
//	{
//		REGISTER INTBIG i;
//
//		if (sim_vercurlevel <= 0)
//		{
//			ttyputerr(_("At the top level of the hierarchy"));
//			return;
//		}
//		sim_vercurlevel--;
//		for(i=estrlen(sim_vercurscope)-1; i>0; i--)
//			if (sim_vercurscope[i] == '.') break;
//		sim_vercurscope[i] = 0;
//		sim_window_grabcachedsignalsoncell(cell);
//		sim_vershowcurrentlevel(cell);
//	}

//	CHAR *sim_verlevel_cur(void)
//	{
//		return(sim_vercurscope);
//	}

	/*
	 * Routine to add the signal called "signame" to the waveform window.
	 */
//	void sim_veraddhighlightednet(CHAR *signame)
//	{
//		REGISTER VERSIGNAL *vs;
//		REGISTER INTBIG traces;
//		REGISTER void *infstr;
//
//		for(vs = sim_verfirstsignal; vs != NOVERSIGNAL; vs = vs->nextversignal)
//		{
//			if (vs->signal != 0) continue;
//			infstr = initinfstr();
//			formatinfstr(infstr, x_("%s.%s"), vs->signalcontext, vs->signalname);
//			if (namesame(signame, returninfstr(infstr)) == 0) break;
//		}
//		if (vs == NOVERSIGNAL) return;
//
//		// ready to add: remove highlighting
//		sim_window_cleartracehighlight();
//
//		// count the number of traces
//		sim_window_inittraceloop();
//		for(traces=0; ; traces++) if (sim_window_nexttraceloop() == 0) break;
//
//		// create a new trace in the last slot
//		vs->signal = sim_window_newtrace(-1, vs->signalname, (INTBIG)vs);
//		sim_window_addhighlighttrace(vs->signal);
//		sim_window_setnumframes(sim_window_getnumframes()+1);
//		sim_window_loaddigtrace(vs->signal, vs->count, vs->stimtime, vs->stimstate);
//	}

	/*
	 * Routine that feeds the current signals into the explorer window.
	 */
//	void sim_verreportsignals(WINDOWPART *simwin, void *(*addbranch)(CHAR*, void*),
//		void *(*findbranch)(CHAR*, void*), void *(*addleaf)(CHAR*, void*), CHAR *(*nodename)(void*))
//	{
//		REGISTER INTBIG j, k, len;
//		REGISTER CHAR *pt, save;
//		REGISTER void *curlevel, *nextlevel;
//		REGISTER VERSIGNAL *vs;
//		CHAR signame[500];
//
//		for(vs = sim_verfirstsignal; vs != NOVERSIGNAL; vs = vs->nextversignal)
//		{
//			if (vs->level < 1) continue;
//			esnprintf(signame, 500, x_("%s.%s"), vs->signalcontext, vs->signalname);
//			len = strlen(signame);
//			for(j=len-2; j>0; j--)
//				if (signame[j] == '.') break;
//			if (j <= 0)
//			{
//				// no "." qualifiers
//				(*addleaf)(signame, 0);
//			} else
//			{
//				// is down the hierarchy
//				signame[j] = 0;
//
//				// now find the location for this branch
//				curlevel = 0;
//				pt = signame;
//				for(;;)
//				{
//					for(k=0; pt[k] != 0; k++) if (pt[k] == '.') break;
//					save = pt[k];
//					pt[k] = 0;
//					nextlevel = (*findbranch)(pt, curlevel);
//					if (nextlevel == 0)
//						nextlevel = (*addbranch)(pt, curlevel);
//					curlevel = nextlevel;
//					if (save == 0) break;
//					pt[k] = save;
//					pt = &pt[k+1];
//				}
//				(*addleaf)(&signame[j+1], curlevel);
//				signame[j] = '.';
//			}
//		}
//	}

}
