/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: Parse.java
* User tool: Technology Editor, creation
* Written by Steven M. Rubin, Sun Microsystems.
*
* Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.tecEdit;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.util.Iterator;

/**
* This class creates technology libraries from technologies.
*/
public class Parse
{
//	/* -*- tab-width: 4 -*-
//	 *
//	 * Electric(tm) VLSI Design System
//	 *
//	 * File: usredtecp.c
//	 * User interface technology editor: conversion from library to technology
//	 * Written by: Steven M. Rubin, Static Free Software
//	 *
//	 * Copyright (c) 2000 Static Free Software.
//	 *
//	 * Electric(tm) is free software; you can redistribute it and/or modify
//	 * it under the terms of the GNU General Public License as published by
//	 * the Free Software Foundation; either version 2 of the License, or
//	 * (at your option) any later version.
//	 *
//	 * Electric(tm) is distributed in the hope that it will be useful,
//	 * but WITHOUT ANY WARRANTY; without even the implied warranty of
//	 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	 * GNU General Public License for more details.
//	 *
//	 * You should have received a copy of the GNU General Public License
//	 * along with Electric(tm); see the file COPYING.  If not, write to
//	 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
//	 * Boston, Mass 02111-1307, USA.
//	 *
//	 * Static Free Software
//	 * 4119 Alpine Road
//	 * Portola Valley, California 94028
//	 * info@staticfreesoft.com
//	 */
//	
//	#include "global.h"
//	#include "egraphics.h"
//	#include "efunction.h"
//	#include "tech.h"
//	#include "tecgen.h"
//	#include "tecart.h"
//	#include "usr.h"
//	#include "drc.h"
//	#include "usredtec.h"
//	
//	/* the globals that define a technology */
//	static INTBIG           us_tecflags;
//	static INTBIG           us_teclayer_count;
//	static CHAR           **us_teclayer_iname = 0;
//	static CHAR           **us_teclayer_names = 0;
//	static CHAR           **us_teccif_layers = 0;
//	static CHAR           **us_tecdxf_layers = 0;
//	static CHAR           **us_tecgds_layers = 0;
//	static INTBIG          *us_teclayer_function = 0;
//	static CHAR           **us_teclayer_letters = 0;
//	static DRCRULES        *us_tecdrc_rules = 0;
//	static float           *us_tecspice_res = 0;
//	static float           *us_tecspice_cap = 0;
//	static float           *us_tecspice_ecap = 0;
//	static INTBIG          *us_tec3d_height = 0;
//	static INTBIG          *us_tec3d_thickness = 0;
//	static INTBIG          *us_tecprint_colors = 0;
//	static INTBIG           us_tecarc_count;
//	static INTBIG          *us_tecarc_widoff = 0;
//	static INTBIG           us_tecnode_count;
//	static INTBIG          *us_tecnode_widoff = 0;
//	static INTBIG          *us_tecnode_grab = 0;
//	static INTBIG           us_tecnode_grabcount;
//	static TECH_COLORMAP    us_teccolmap[32];
//	
//	/* these must correspond to the layer functions in "efunction.h" */
//	LIST us_tecarc_functions[] =
//	{
//		{x_("unknown"),             x_("APUNKNOWN"),  APUNKNOWN},
//		{x_("metal-1"),             x_("APMETAL1"),   APMETAL1},
//		{x_("metal-2"),             x_("APMETAL2"),   APMETAL2},
//		{x_("metal-3"),             x_("APMETAL3"),   APMETAL3},
//		{x_("metal-4"),             x_("APMETAL4"),   APMETAL4},
//		{x_("metal-5"),             x_("APMETAL5"),   APMETAL5},
//		{x_("metal-6"),             x_("APMETAL6"),   APMETAL6},
//		{x_("metal-7"),             x_("APMETAL7"),   APMETAL7},
//		{x_("metal-8"),             x_("APMETAL8"),   APMETAL8},
//		{x_("metal-9"),             x_("APMETAL9"),   APMETAL9},
//		{x_("metal-10"),            x_("APMETAL10"),  APMETAL10},
//		{x_("metal-11"),            x_("APMETAL11"),  APMETAL11},
//		{x_("metal-12"),            x_("APMETAL12"),  APMETAL12},
//		{x_("polysilicon-1"),       x_("APPOLY1"),    APPOLY1},
//		{x_("polysilicon-2"),       x_("APPOLY2"),    APPOLY2},
//		{x_("polysilicon-3"),       x_("APPOLY3"),    APPOLY3},
//		{x_("diffusion"),           x_("APDIFF"),     APDIFF},
//		{x_("p-Diffusion"),         x_("APDIFFP"),    APDIFFP},
//		{x_("n-Diffusion"),         x_("APDIFFN"),    APDIFFN},
//		{x_("substrate-Diffusion"), x_("APDIFFS"),    APDIFFS},
//		{x_("well-Diffusion"),      x_("APDIFFW"),    APDIFFW},
//		{x_("bus"),                 x_("APBUS"),      APBUS},
//		{x_("unrouted"),            x_("APUNROUTED"), APUNROUTED},
//		{x_("nonelectrical"),       x_("APNONELEC"),  APNONELEC},
//		{NULL, NULL, 0}
//	};
//	
//	static PCON  *us_tecedfirstpcon = NOPCON;	/* list of port connections */
//	static RULE  *us_tecedfirstrule = NORULE;	/* list of rules */
//	
//	/* working memory for "us_tecedmakeprim()" */
//	static INTBIG *us_tecedmakepx, *us_tecedmakepy, *us_tecedmakefactor,
//		*us_tecedmakeleftdist, *us_tecedmakerightdist, *us_tecedmakebotdist,
//		*us_tecedmaketopdist, *us_tecedmakecentxdist, *us_tecedmakecentydist, *us_tecedmakeratiox,
//		*us_tecedmakeratioy, *us_tecedmakecx, *us_tecedmakecy;
//	static INTBIG us_tecedmakearrlen = 0;
//	
//	/* working memory for "us_teceditgetdependents()" */
//	static LIBRARY **us_teceddepliblist;
//	static INTBIG    us_teceddepliblistsize = 0;
//	
//	/*
//	 * Routine to free all memory associated with this module.
//	 */
//	void us_freeedtecpmemory(void)
//	{
//		if (us_tecedmakearrlen != 0)
//		{
//			efree((CHAR *)us_tecedmakepx);
//			efree((CHAR *)us_tecedmakepy);
//			efree((CHAR *)us_tecedmakecx);
//			efree((CHAR *)us_tecedmakecy);
//			efree((CHAR *)us_tecedmakefactor);
//			efree((CHAR *)us_tecedmakeleftdist);
//			efree((CHAR *)us_tecedmakerightdist);
//			efree((CHAR *)us_tecedmakebotdist);
//			efree((CHAR *)us_tecedmaketopdist);
//			efree((CHAR *)us_tecedmakecentxdist);
//			efree((CHAR *)us_tecedmakecentydist);
//			efree((CHAR *)us_tecedmakeratiox);
//			efree((CHAR *)us_tecedmakeratioy);
//		}
//		if (us_teceddepliblistsize != 0) efree((CHAR *)us_teceddepliblist);
//		us_tecedfreetechmemory();
//	}
//	
//	/*
//	 * the routine invoked for the "technology edit library-to-tech" command.  Dumps
//	 * C code if "dumpc" is nonzero
//	 */
//	void us_tecfromlibinit(LIBRARY *lib, CHAR *techname, INTBIG dumpformat)
//	{
//		REGISTER FILE *f;
//		REGISTER TECHNOLOGY *tech;
//		REGISTER CLUSTER *clus;
//		REGISTER VARIABLE *var, *ovar;
//		REGISTER CHAR **varnames;
//		CHAR *truename, *newtechname;
//		LIBRARY **dependentlibs;
//		REGISTER INTBIG dependentlibcount, i, j, modified;
//		REGISTER void *infstr;
//		static TECH_VARIABLES us_tecvariables[2] = {{NULL, NULL, 0.0, 0},
//	                                                {NULL, NULL, 0.0, 0}};
//	
//		// make sure network tool is on
//		if ((net_tool.toolstate&TOOLON) == 0)
//		{
//			ttyputerr(_("Network tool must be running...turning it on"));
//			toolturnon(net_tool);
//			ttyputerr(_("...now reissue the technology editing command"));
//			return;
//		}
//	
//		// loop until the name is valid
//		if (techname == 0) techname = lib.libname;
//		if (allocstring(&newtechname, techname, el_tempcluster)) return;
//		modified = 0;
//		for(;;)
//		{
//			// search by hand because "gettechnology" handles partial matches
//			for(tech = el_technologies; tech != NOTECHNOLOGY; tech = tech.nexttechnology)
//				if (namesame(newtechname, tech.techname) == 0) break;
//			if (tech == NOTECHNOLOGY) break;
//			infstr = initinfstr();
//			addstringtoinfstr(infstr, newtechname);
//			addtoinfstr(infstr, 'X');
//			(void)reallocstring(&newtechname, returninfstr(infstr), el_tempcluster);
//			modified= 1;
//		}
//	
//		// create the technology
//		infstr = initinfstr();
//		addstringtoinfstr(infstr, x_("tech:"));
//		addstringtoinfstr(infstr, newtechname);
//		clus = alloccluster(returninfstr(infstr));
//		if (clus == NOCLUSTER) return;
//		tech = alloctechnology(clus);
//		if (tech == NOTECHNOLOGY) return;
//	
//		// set the technology name
//		if (allocstring(&tech.techname, newtechname, clus)) return;
//		efree((CHAR *)newtechname);
//		if (modified != 0)
//			ttyputmsg(_("Warning: already a technology called %s.  Naming this %s"),
//				techname, tech.techname);
//	
//		// set technology description
//		if (allocstring(&tech.techdescript, tech.techname, clus)) return;
//	
//		// free any previous memory for the technology
//		us_tecedfreetechmemory();
//	
//		// get list of dependent libraries
//		dependentlibcount = us_teceditgetdependents(lib, &dependentlibs);
//	
//		// initialize the state of this technology
//		us_tecflags = 0;
//		if (us_tecedmakefactors(dependentlibs, dependentlibcount, tech)) return;
//	
//		// build layer structures
//		if (us_tecedmakelayers(dependentlibs, dependentlibcount, tech)) return;
//	
//		// build arc structures
//		if (us_tecedmakearcs(dependentlibs, dependentlibcount, tech)) return;
//	
//		// build node structures
//		if (us_tecedmakenodes(dependentlibs, dependentlibcount, tech)) return;
//	
//		// copy any miscellaneous variables (should use dependent libraries facility)
//		Variable var = lib.getVar(Generate.VARIABLELIST_KEY);
//		if (var != NOVARIABLE)
//		{
//			j = getlength(var);
//			varnames = (CHAR **)var.addr;
//			for(i=0; i<j; i++)
//			{
//				ovar = getval((INTBIG)lib, VLIBRARY, -1, varnames[i]);
//				if (ovar == NOVARIABLE) continue;
//				(void)setval((INTBIG)tech, VTECHNOLOGY, varnames[i], ovar.addr, ovar.type);
//			}
//		}
//	
//		// check technology for consistency
//		us_tecedcheck(tech);
//	
//		if (dumpformat > 0)
//		{
//			// print the technology as C code
//			infstr = initinfstr();
//			addstringtoinfstr(infstr, x_("tec"));
//			addstringtoinfstr(infstr, techname);
//			addstringtoinfstr(infstr, x_(".c"));
//			f = xcreate(returninfstr(infstr), el_filetypetext, _("Technology Code File"), &truename);
//			if (f == NULL)
//			{
//				if (truename != 0) ttyputerr(_("Cannot write %s"), truename);
//				return;
//			}
//			ttyputverbose(M_("Writing: %s"), truename);
//	
//			// write the layers, arcs, and nodes
//			us_teceditdumplayers(f, tech, techname);
//			us_teceditdumparcs(f, tech, techname);
//			us_teceditdumpnodes(f, tech, techname);
//			us_teceditdumpvars(f, tech, techname);
//	
//			// clean up
//			xclose(f);
//		}
//	
//		if (dumpformat < 0)
//		{
//			// print the technology as Java code
//			infstr = initinfstr();
//			addstringtoinfstr(infstr, techname);
//			addstringtoinfstr(infstr, x_(".java"));
//			f = xcreate(returninfstr(infstr), el_filetypetext, _("Technology Code File"), &truename);
//			if (f == NULL)
//			{
//				if (truename != 0) ttyputerr(_("Cannot write %s"), truename);
//				return;
//			}
//			ttyputverbose(M_("Writing: %s"), truename);
//	
//			// write the layers, arcs, and nodes
//			us_teceditdumpjavalayers(f, tech, techname);
//			us_teceditdumpjavaarcs(f, tech, techname);
//			us_teceditdumpjavanodes(f, tech, techname);
//			xprintf(f, x_("}\n"));
//	
//			// clean up
//			xclose(f);
//		}
//	
//		// finish initializing the technology
//		if ((us_tecflags&HASGRAB) == 0) us_tecvariables[0].name = 0; else
//		{
//			us_tecvariables[0].name = x_("prototype_center");
//			us_tecvariables[0].value = (CHAR *)us_tecnode_grab;
//			us_tecvariables[0].type = us_tecnode_grabcount/3;
//		}
//		tech.variables = us_tecvariables;
//		if (tech_doinitprocess(tech)) return;
//		if (tech_doaddportsandvars(tech)) return;
//	
//		// install the technology fully
//		addtechnology(tech);
//	
//		// let the user interface process it
//		us_figuretechopaque(tech);
//	
//		// switch to this technology
//		ttyputmsg(_("Technology %s built.  Switching to it."), tech.techname);
//		us_setnodeproto(NONODEPROTO);
//		us_setarcproto(NOARCPROTO, TRUE);
//	
//		// disable option tracking while colormap is updated
//		(void)setvalkey((INTBIG)us_tool, VTOOL, us_ignoreoptionchangeskey, 1,
//			VINTEGER|VDONTSAVE);
//		us_getcolormap(tech, COLORSEXISTING, TRUE);
//		var = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER, us_ignoreoptionchangeskey);
//		if (var != NOVARIABLE)
//			(void)delvalkey((INTBIG)us_tool, VTOOL, us_ignoreoptionchangeskey);
//	
//		(void)setvalkey((INTBIG)us_tool, VTOOL, us_current_technology_key, (INTBIG)tech,
//			VTECHNOLOGY|VDONTSAVE);
//	
//		// fix up the menu entries
//		us_setmenunodearcs();
//		if ((us_state&NONPERSISTENTCURNODE) == 0) us_setnodeproto(tech.firstnodeproto);
//		us_setarcproto(tech.firstarcproto, TRUE);
//	}
//	
//	void us_tecedcheck(TECHNOLOGY *tech)
//	{
//		REGISTER INTBIG i, j, k, l;
//		REGISTER TECH_POLYGON *plist;
//		REGISTER TECH_NODES *nlist;
//	
//		// make sure there is a pure-layer node for every nonpseudo layer
//		for(i=0; i<tech.layercount; i++)
//		{
//			if ((us_teclayer_function[i]&LFPSEUDO) != 0) continue;
//			for(j=0; j<tech.nodeprotocount; j++)
//			{
//				nlist = tech.nodeprotos[j];
//				if (((nlist.initialbits&NFUNCTION)>>NFUNCTIONSH) != NPNODE) continue;
//				plist = &nlist.layerlist[0];
//				if (plist.layernum == i) break;
//			}
//			if (j < tech.nodeprotocount) continue;
//			ttyputmsg(_("Warning: Layer %s has no associated pure-layer node"),
//				us_teclayer_names[i]);
//		}
//	
//		// make sure there is a pin for every arc and that it uses pseudo-layers
//		for(i=0; i<tech.arcprotocount; i++)
//		{
//			for(j=0; j<tech.nodeprotocount; j++)
//			{
//				nlist = tech.nodeprotos[j];
//				if (((nlist.initialbits&NFUNCTION)>>NFUNCTIONSH) != NPPIN) continue;
//				for(k=0; k<nlist.portcount; k++)
//				{
//					for(l=1; nlist.portlist[k].portarcs[l] >= 0; l++)
//						if (nlist.portlist[k].portarcs[l] == i) break;
//					if (nlist.portlist[k].portarcs[l] >= 0) break;
//				}
//				if (k < nlist.portcount) break;
//			}
//			if (j < tech.nodeprotocount)
//			{
//				// pin found: make sure it uses pseudo-layers
//				nlist = tech.nodeprotos[j];
//				for(k=0; k<nlist.layercount; k++)
//				{
//					plist = &nlist.layerlist[k];
//					if ((us_teclayer_function[plist.layernum]&LFPSEUDO) == 0) break;
//				}
//				if (k < nlist.layercount)
//					ttyputmsg(_("Warning: Pin %s is not composed of pseudo-layers"),
//						tech.nodeprotos[j].nodename);
//				continue;
//			}
//			ttyputmsg(_("Warning: Arc %s has no associated pin node"), tech.arcprotos[i].arcname);
//		}
//	}
//	
//	void us_tecedfreetechmemory(void)
//	{
//		REGISTER INTBIG i;
//		REGISTER PCON *pc;
//		REGISTER RULE *r;
//	
//		// free DRC layer name information
//		if (us_teceddrclayernames != 0)
//		{
//			for(i=0; i<us_teceddrclayers; i++) efree(us_teceddrclayernames[i]);
//			efree((CHAR *)us_teceddrclayernames);
//			us_teceddrclayernames = 0;
//		}
//	
//		if (us_teclayer_iname != 0)
//		{
//			for(i=0; i<us_teclayer_count; i++)
//				if (us_teclayer_iname[i] != 0) efree((CHAR *)us_teclayer_iname[i]);
//			efree((CHAR *)us_teclayer_iname);
//			us_teclayer_iname = 0;
//		}
//		if (us_teclayer_names != 0)
//		{
//			for(i=0; i<us_teclayer_count; i++)
//				if (us_teclayer_names[i] != 0) efree((CHAR *)us_teclayer_names[i]);
//			efree((CHAR *)us_teclayer_names);
//			us_teclayer_names = 0;
//		}
//		if (us_teccif_layers != 0)
//		{
//			for(i=0; i<us_teclayer_count; i++)
//				if (us_teccif_layers[i] != 0) efree((CHAR *)us_teccif_layers[i]);
//			efree((CHAR *)us_teccif_layers);
//			us_teccif_layers = 0;
//		}
//		if (us_tecdxf_layers != 0)
//		{
//			for(i=0; i<us_teclayer_count; i++)
//				if (us_tecdxf_layers[i] != 0) efree((CHAR *)us_tecdxf_layers[i]);
//			efree((CHAR *)us_tecdxf_layers);
//			us_tecdxf_layers = 0;
//		}
//		if (us_tecgds_layers != 0)
//		{
//			for(i=0; i<us_teclayer_count; i++)
//				if (us_tecgds_layers[i] != 0) efree((CHAR *)us_tecgds_layers[i]);
//			efree((CHAR *)us_tecgds_layers);
//			us_tecgds_layers = 0;
//		}
//		if (us_teclayer_function != 0)
//		{
//			efree((CHAR *)us_teclayer_function);
//			us_teclayer_function = 0;
//		}
//		if (us_teclayer_letters != 0)
//		{
//			for(i=0; i<us_teclayer_count; i++)
//				if (us_teclayer_letters[i] != 0) efree((CHAR *)us_teclayer_letters[i]);
//			efree((CHAR *)us_teclayer_letters);
//			us_teclayer_letters = 0;
//		}
//		if (us_tecdrc_rules != 0)
//		{
//			dr_freerules(us_tecdrc_rules);
//			us_tecdrc_rules = 0;
//		}
//		if (us_tecspice_res != 0)
//		{
//			efree((CHAR *)us_tecspice_res);
//			us_tecspice_res = 0;
//		}
//		if (us_tecspice_cap != 0)
//		{
//			efree((CHAR *)us_tecspice_cap);
//			us_tecspice_cap = 0;
//		}
//		if (us_tecspice_ecap != 0)
//		{
//			efree((CHAR *)us_tecspice_ecap);
//			us_tecspice_ecap = 0;
//		}
//		if (us_tec3d_height != 0)
//		{
//			efree((CHAR *)us_tec3d_height);
//			us_tec3d_height = 0;
//		}
//		if (us_tec3d_thickness != 0)
//		{
//			efree((CHAR *)us_tec3d_thickness);
//			us_tec3d_thickness = 0;
//		}
//		if (us_tecprint_colors != 0)
//		{
//			efree((CHAR *)us_tecprint_colors);
//			us_tecprint_colors = 0;
//		}
//	
//		if (us_tecarc_widoff != 0)
//		{
//			efree((CHAR *)us_tecarc_widoff);
//			us_tecarc_widoff = 0;
//		}
//	
//		if (us_tecnode_widoff != 0)
//		{
//			efree((CHAR *)us_tecnode_widoff);
//			us_tecnode_widoff = 0;
//		}
//		if (us_tecnode_grab != 0)
//		{
//			efree((CHAR *)us_tecnode_grab);
//			us_tecnode_grab = 0;
//		}
//	
//		while (us_tecedfirstpcon != NOPCON)
//		{
//			pc = us_tecedfirstpcon;
//			us_tecedfirstpcon = us_tecedfirstpcon.nextpcon;
//			efree((CHAR *)pc.connects);
//			efree((CHAR *)pc.assoc);
//			efree((CHAR *)pc);
//		}
//	
//		while (us_tecedfirstrule != NORULE)
//		{
//			r = us_tecedfirstrule;
//			us_tecedfirstrule = us_tecedfirstrule.nextrule;
//			efree((CHAR *)r.value);
//			efree((CHAR *)r);
//		}
//	}
//	
//	/*
//	 * routine to scan the "dependentlibcount" libraries in "dependentlibs",
//	 * and get global factors for technology "tech".  Returns true on error.
//	 */
//	BOOLEAN us_tecedmakefactors(LIBRARY **dependentlibs, INTBIG dependentlibcount, TECHNOLOGY *tech)
//	{
//		REGISTER NODEPROTO *np;
//		REGISTER NODEINST *ni;
//		REGISTER VARIABLE *var;
//		REGISTER INTBIG opt;
//		REGISTER CHAR *str;
//		REGISTER INTBIG i;
//	
//		np = NONODEPROTO;
//		for(i=dependentlibcount-1; i>=0; i--)
//		{
//			for(np = dependentlibs[i].firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//				if (namesame(np.protoname, x_("factors")) == 0) break;
//			if (np != NONODEPROTO) break;
//		}
//		if (np == NONODEPROTO)
//		{
//			tech.deflambda = 2000;
//			return(FALSE);
//		}
//	
//		for(ni = np.firstnodeinst; ni != NONODEINST; ni = ni.nextnodeinst)
//		{
//			opt = us_tecedgetoption(ni);
//			var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, art_messagekey);
//			if (var == NOVARIABLE) continue;
//			str = (CHAR *)var.addr;
//			switch (opt)
//			{
//				case TECHLAMBDA:	// lambda
//					tech.deflambda = myatoi(&str[8]);
//					break;
//				case TECHDESCRIPT:	// description
//					(void)reallocstring(&tech.techdescript, &str[13], tech.cluster);
//					break;
//				default:
//					us_tecedpointout(ni, np);
//					ttyputerr(_("Unknown object in miscellaneous-information cell"));
//					return(TRUE);
//			}
//		}
//		return(FALSE);
//	}
//	
//	/*
//	 * routine to scan the "dependentlibcount" libraries in "dependentlibs",
//	 * and build the layer structures for it in technology "tech".  Returns true on error.
//	 */
//	BOOLEAN us_tecedmakelayers(LIBRARY **dependentlibs, INTBIG dependentlibcount, TECHNOLOGY *tech)
//	{
//		REGISTER NODEPROTO *np;
//		NODEPROTO **sequence, **nodesequence;
//		REGISTER INTBIG i, j, l, total, nodecount, *drcptr, drcsize;
//		REGISTER CHAR *ab;
//		REGISTER VARIABLE *var;
//		REGISTER void *infstr;
//	
//		// first find the number of layers
//		tech.layercount = us_teceditfindsequence(dependentlibs, "layer-", Generate.LAYERSEQUENCE_KEY);
//		if (tech.layercount <= 0)
//		{
//			ttyputerr(_("No layers found"));
//			if ((us_tool.toolstate&NODETAILS) == 0)
//				ttyputerr(_("Create them with the 'edit-layer' option"));
//			return(TRUE);
//		}
//	
//		// allocate the arrays for the layers
//		us_teclayer_count = tech.layercount;
//		drcsize = us_teclayer_count*us_teclayer_count/2 + (us_teclayer_count+1)/2;
//	
//		us_teclayer_iname = (CHAR **)emalloc((us_teclayer_count * (sizeof (CHAR *))), us_tool.cluster);
//		if (us_teclayer_iname == 0) return(TRUE);
//		for(i=0; i<us_teclayer_count; i++) us_teclayer_iname[i] = 0;
//	
//		tech.layers = (GRAPHICS **)emalloc(((us_teclayer_count+1) * (sizeof (GRAPHICS *))), tech.cluster);
//		if (tech.layers == 0) return(TRUE);
//		for(i=0; i<us_teclayer_count; i++) tech.layers[i] = (GRAPHICS *)emalloc(sizeof (GRAPHICS), tech.cluster);
//		tech.layers[us_teclayer_count] = NOGRAPHICS;
//	
//		us_teclayer_names = (CHAR **)emalloc((us_teclayer_count * (sizeof (CHAR *))), us_tool.cluster);
//		if (us_teclayer_names == 0) return(TRUE);
//		for(i=0; i<us_teclayer_count; i++) us_teclayer_names[i] = 0;
//	
//		us_teccif_layers = (CHAR **)emalloc((us_teclayer_count * (sizeof (CHAR *))), us_tool.cluster);
//		if (us_teccif_layers == 0) return(TRUE);
//		for(i=0; i<us_teclayer_count; i++) us_teccif_layers[i] = 0;
//	
//		us_tecdxf_layers = (CHAR **)emalloc((us_teclayer_count * (sizeof (CHAR *))), us_tool.cluster);
//		if (us_tecdxf_layers == 0) return(TRUE);
//		for(i=0; i<us_teclayer_count; i++) us_tecdxf_layers[i] = 0;
//	
//		us_tecgds_layers = (CHAR **)emalloc((us_teclayer_count * (sizeof (CHAR *))), us_tool.cluster);
//		if (us_tecgds_layers == 0) return(TRUE);
//		for(i=0; i<us_teclayer_count; i++) us_tecgds_layers[i] = 0;
//	
//		us_teclayer_function = emalloc((us_teclayer_count * SIZEOFINTBIG), us_tool.cluster);
//		if (us_teclayer_function == 0) return(TRUE);
//	
//		us_teclayer_letters = (CHAR **)emalloc((us_teclayer_count * (sizeof (CHAR *))), us_tool.cluster);
//		if (us_teclayer_letters == 0) return(TRUE);
//		for(i=0; i<us_teclayer_count; i++) us_teclayer_letters[i] = 0;
//	
//		us_tecspice_res = (float *)emalloc((us_teclayer_count * (sizeof (float))), us_tool.cluster);
//		if (us_tecspice_res == 0) return(TRUE);
//		for(i=0; i<us_teclayer_count; i++) us_tecspice_res[i] = 0.0;
//	
//		us_tecspice_cap = (float *)emalloc((us_teclayer_count * (sizeof (float))), us_tool.cluster);
//		if (us_tecspice_cap == 0) return(TRUE);
//		for(i=0; i<us_teclayer_count; i++) us_tecspice_cap[i] = 0.0;
//	
//		us_tecspice_ecap = (float *)emalloc((us_teclayer_count * (sizeof (float))), us_tool.cluster);
//		if (us_tecspice_ecap == 0) return(TRUE);
//		for(i=0; i<us_teclayer_count; i++) us_tecspice_ecap[i] = 0.0;
//	
//		us_tec3d_height = (INTBIG *)emalloc((us_teclayer_count * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tec3d_height == 0) return(TRUE);
//		for(i=0; i<us_teclayer_count; i++) us_tec3d_height[i] = 0;
//	
//		us_tec3d_thickness = (INTBIG *)emalloc((us_teclayer_count * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tec3d_thickness == 0) return(TRUE);
//		for(i=0; i<us_teclayer_count; i++) us_tec3d_thickness[i] = 0;
//	
//		us_tecprint_colors = (INTBIG *)emalloc((us_teclayer_count * 5 * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tecprint_colors == 0) return(TRUE);
//		for(i=0; i<us_teclayer_count*5; i++) us_tecprint_colors[i] = 0;
//	
//		// get the design rules
//		us_tecedgetlayernamelist();
//		if (us_tecdrc_rules != 0)
//		{
//			dr_freerules(us_tecdrc_rules);
//			us_tecdrc_rules = 0;
//		}
//		nodecount = us_teceditfindsequence(dependentlibs, "node-", Generate.NODERSEQUENCE_KEY);
//		us_tecdrc_rules = dr_allocaterules(us_teceddrclayers, nodecount, x_("EDITED TECHNOLOGY"));
//		if (us_tecdrc_rules == NODRCRULES) return(TRUE);
//		for(i=0; i<us_teceddrclayers; i++)
//			(void)allocstring(&us_tecdrc_rules.layernames[i], us_teceddrclayernames[i], el_tempcluster);
//		for(i=0; i<nodecount; i++)
//			(void)allocstring(&us_tecdrc_rules.nodenames[i], &nodesequence[i].protoname[5], el_tempcluster);
//		if (nodecount > 0) efree((CHAR *)nodesequence);
//		var = NOVARIABLE;
//		for(i=dependentlibcount-1; i>=0; i--)
//		{
//			var = getval((INTBIG)dependentlibs[i], VLIBRARY, VSTRING|VISARRAY, x_("EDTEC_DRC"));
//			if (var != NOVARIABLE) break;
//		}
//		us_teceditgetdrcarrays(var, us_tecdrc_rules);
//	
//		// now scan each layer and fill in the data
//		for(total=0; total<us_teclayer_count; total++)
//		{
//			// set the layer name
//			np = sequence[total];
//			(void)allocstring(&us_teclayer_names[total], &np.protoname[6], us_tool.cluster);
//	
//			if (us_teceditgetlayerinfo(np, tech.layers[total], &us_teccif_layers[total],
//				&us_teclayer_function[total], &us_teclayer_letters[total], &us_tecdxf_layers[total],
//					&us_tecgds_layers[total], &us_tecspice_res[total], &us_tecspice_cap[total],
//						&us_tecspice_ecap[total], &us_tecdrc_rules.minwidth[total],
//							&us_tec3d_height[total], &us_tec3d_thickness[total],
//								&us_tecprint_colors[total*5])) return(TRUE);
//			if (us_teccif_layers[total] != 0 && namesame(us_teccif_layers[total], x_("xx")) != 0)
//				us_tecflags |= HASCIF;
//			if (us_tecdxf_layers[total] != 0) us_tecflags |= HASDXF;
//			if (us_tecgds_layers[total] != 0) us_tecflags |= HASGDS;
//			if (us_tecspice_res[total] != 0.0) us_tecflags |= HASSPIRES;
//			if (us_tecspice_cap[total] != 0.0) us_tecflags |= HASSPICAP;
//			if (us_tecspice_ecap[total] != 0.0) us_tecflags |= HASSPIECAP;
//			if (us_tec3d_height[total] != 0 || us_tec3d_thickness[total] != 0) us_tecflags |= HAS3DINFO;
//			if (us_tecprint_colors[total*5] != 0 || us_tecprint_colors[total*5+1] != 0 ||
//				us_tecprint_colors[total*5+2] != 0 || us_tecprint_colors[total*5+3] != 0 ||
//				us_tecprint_colors[total*5+4] != 0) us_tecflags |= HASPRINTCOL;
//			tech.layers[total].firstvar = NOVARIABLE;
//			tech.layers[total].numvar = 0;
//		}
//	
//		for(i=0; i<total; i++)
//		{
//			(void)allocstring(&us_teclayer_iname[i], makeabbrev(us_teclayer_names[i], TRUE),
//				us_tool.cluster);
//	
//			// loop until the name is unique
//			for(;;)
//			{
//				// see if a previously assigned abbreviation is the same
//				for(j=0; j<i; j++)
//					if (namesame(us_teclayer_iname[i], us_teclayer_iname[j]) == 0)
//						break;
//				if (j >= i) break;
//	
//				// name conflicts: change it
//				l = estrlen(ab = us_teclayer_iname[i]);
//				if (ab[l-1] >= '0' && ab[l-1] <= '8') ab[l-1]++; else
//				{
//					infstr = initinfstr();
//					addstringtoinfstr(infstr, ab);
//					addtoinfstr(infstr, '0');
//					(void)reallocstring(&us_teclayer_iname[i], returninfstr(infstr), us_tool.cluster);
//				}
//			}
//		}
//	
//		// get the color map
//		var = NOVARIABLE;
//		for(i=dependentlibcount-1; i>=0; i--)
//		{
//			var = dependentlibs[i].getVar(Generate.COLORMAP_KEY);
//			if (var != null) break;
//		}
//		if (var != NOVARIABLE)
//		{
//			us_tecflags |= HASCOLORMAP;
//			drcptr = (INTBIG *)var.addr;
//			for(i=0; i<32; i++)
//			{
//				us_teccolmap[i].red = (INTSML)drcptr[(i<<2)*3];
//				us_teccolmap[i].green = (INTSML)drcptr[(i<<2)*3+1];
//				us_teccolmap[i].blue = (INTSML)drcptr[(i<<2)*3+2];
//			}
//		}
//	
//		// see which design rules exist
//		for(i=0; i<us_teceddrclayers; i++)
//		{
//			if (us_tecdrc_rules.minwidth[i] >= 0) us_tecflags |= HASDRCMINWID;
//			if (*us_tecdrc_rules.minwidthR[i] != 0) us_tecflags |= HASDRCMINWIDR;
//		}
//		for(i=0; i<drcsize; i++)
//		{
//			if (us_tecdrc_rules.conlist[i] >= 0) us_tecflags |= HASCONDRC;
//			if (*us_tecdrc_rules.conlistR[i] != 0) us_tecflags |= HASCONDRCR;
//			if (us_tecdrc_rules.unconlist[i] >= 0) us_tecflags |= HASUNCONDRC;
//			if (*us_tecdrc_rules.unconlistR[i] != 0) us_tecflags |= HASUNCONDRCR;
//			if (us_tecdrc_rules.conlistW[i] >= 0) us_tecflags |= HASCONDRCW;
//			if (*us_tecdrc_rules.conlistWR[i] != 0) us_tecflags |= HASCONDRCWR;
//			if (us_tecdrc_rules.unconlistW[i] >= 0) us_tecflags |= HASUNCONDRCW;
//			if (*us_tecdrc_rules.unconlistWR[i] != 0) us_tecflags |= HASUNCONDRCWR;
//			if (us_tecdrc_rules.conlistM[i] >= 0) us_tecflags |= HASCONDRCM;
//			if (*us_tecdrc_rules.conlistMR[i] != 0) us_tecflags |= HASCONDRCMR;
//			if (us_tecdrc_rules.unconlistM[i] >= 0) us_tecflags |= HASUNCONDRCM;
//			if (*us_tecdrc_rules.unconlistMR[i] != 0) us_tecflags |= HASUNCONDRCMR;
//			if (us_tecdrc_rules.edgelist[i] >= 0) us_tecflags |= HASEDGEDRC;
//			if (*us_tecdrc_rules.edgelistR[i] != 0) us_tecflags |= HASEDGEDRCR;
//		}
//		for(i=0; i<us_tecdrc_rules.numnodes; i++)
//		{
//			if (us_tecdrc_rules.minnodesize[i*2] > 0 ||
//				us_tecdrc_rules.minnodesize[i*2+1] > 0) us_tecflags |= HASMINNODE;
//			if (*us_tecdrc_rules.minnodesizeR[i] != 0) us_tecflags |= HASMINNODER;
//		}
//	
//		// store this information on the technology object
//		(void)setval((INTBIG)tech, VTECHNOLOGY, x_("TECH_layer_names"), (INTBIG)us_teclayer_names,
//			VSTRING|VDONTSAVE|VISARRAY|(tech.layercount<<VLENGTHSH));
//		(void)setval((INTBIG)tech, VTECHNOLOGY, x_("TECH_layer_function"), (INTBIG)us_teclayer_function,
//			VINTEGER|VDONTSAVE|VISARRAY|(tech.layercount<<VLENGTHSH));
//		(void)setvalkey((INTBIG)tech, VTECHNOLOGY, us_layer_letters_key, (INTBIG)us_teclayer_letters,
//			VSTRING|VDONTSAVE|VISARRAY|(tech.layercount<<VLENGTHSH));
//		if ((us_tecflags&HASCOLORMAP) != 0)
//			(void)setval((INTBIG)tech, VTECHNOLOGY, x_("USER_color_map"), (INTBIG)us_teccolmap,
//				VCHAR|VDONTSAVE|VISARRAY|((sizeof us_teccolmap)<<VLENGTHSH));
//		if ((us_tecflags&HASCIF) != 0)
//			(void)setval((INTBIG)tech, VTECHNOLOGY, x_("IO_cif_layer_names"), (INTBIG)us_teccif_layers,
//				VSTRING|VDONTSAVE|VISARRAY|(tech.layercount<<VLENGTHSH));
//		if ((us_tecflags&HASDXF) != 0)
//			(void)setval((INTBIG)tech, VTECHNOLOGY, x_("IO_dxf_layer_names"), (INTBIG)us_tecdxf_layers,
//				VSTRING|VDONTSAVE|VISARRAY|(tech.layercount<<VLENGTHSH));
//		if ((us_tecflags&HASGDS) != 0)
//			(void)setval((INTBIG)tech, VTECHNOLOGY, x_("IO_gds_layer_numbers"), (INTBIG)us_tecgds_layers,
//				VSTRING|VDONTSAVE|VISARRAY|(tech.layercount<<VLENGTHSH));
//	
//		if ((us_tecflags&(HASCONDRCW|HASUNCONDRCW)) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_wide_limitkey, us_tecdrc_rules.widelimit,
//				VFRACT|VDONTSAVE);
//		if ((us_tecflags&HASDRCMINWID) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_min_widthkey, (INTBIG)us_tecdrc_rules.minwidth,
//				VFRACT|VDONTSAVE|VISARRAY|(tech.layercount<<VLENGTHSH));
//		if ((us_tecflags&HASDRCMINWIDR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_min_width_rulekey, (INTBIG)us_tecdrc_rules.minwidthR,
//				VSTRING|VDONTSAVE|VISARRAY|(tech.layercount<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRC) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distanceskey,
//				(INTBIG)us_tecdrc_rules.conlist, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRCR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distances_rulekey,
//				(INTBIG)us_tecdrc_rules.conlistR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRC) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distanceskey,
//				(INTBIG)us_tecdrc_rules.unconlist, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRCR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distances_rulekey,
//				(INTBIG)us_tecdrc_rules.unconlistR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRCW) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distancesWkey,
//				(INTBIG)us_tecdrc_rules.conlistW, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRCWR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distancesW_rulekey,
//				(INTBIG)us_tecdrc_rules.conlistWR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRCW) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distancesWkey,
//				(INTBIG)us_tecdrc_rules.unconlistW, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRCWR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distancesW_rulekey,
//				(INTBIG)us_tecdrc_rules.unconlistWR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRCM) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distancesMkey,
//				(INTBIG)us_tecdrc_rules.conlistM, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRCMR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distancesM_rulekey,
//				(INTBIG)us_tecdrc_rules.conlistMR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRCM) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distancesMkey,
//				(INTBIG)us_tecdrc_rules.unconlistM, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRCMR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distancesM_rulekey,
//				(INTBIG)us_tecdrc_rules.unconlistMR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASEDGEDRC) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_edge_distanceskey,
//				(INTBIG)us_tecdrc_rules.edgelist, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASEDGEDRCR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_edge_distances_rulekey,
//				(INTBIG)us_tecdrc_rules.edgelistR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASMINNODE) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_min_node_sizekey,
//				(INTBIG)us_tecdrc_rules.minnodesize, VFRACT|VDONTSAVE|VISARRAY|((us_tecdrc_rules.numnodes*2)<<VLENGTHSH));
//		if ((us_tecflags&HASMINNODER) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_min_node_size_rulekey,
//				(INTBIG)us_tecdrc_rules.minnodesizeR, VSTRING|VDONTSAVE|VISARRAY|(us_tecdrc_rules.numnodes<<VLENGTHSH));
//		if ((us_tecflags&HASSPIRES) != 0)
//			(void)setval((INTBIG)tech, VTECHNOLOGY, x_("SIM_spice_resistance"), (INTBIG)us_tecspice_res,
//				VFLOAT|VDONTSAVE|VISARRAY|(tech.layercount<<VLENGTHSH));
//		if ((us_tecflags&HASSPICAP) != 0)
//			(void)setval((INTBIG)tech, VTECHNOLOGY, x_("SIM_spice_capacitance"), (INTBIG)us_tecspice_cap,
//				VFLOAT|VDONTSAVE|VISARRAY|(tech.layercount<<VLENGTHSH));
//		if ((us_tecflags&HASSPIECAP) != 0)
//			(void)setval((INTBIG)tech, VTECHNOLOGY, x_("SIM_spice_edge_capacitance"), (INTBIG)us_tecspice_ecap,
//				VFLOAT|VDONTSAVE|VISARRAY|(tech.layercount<<VLENGTHSH));
//		if ((us_tecflags&HAS3DINFO) != 0)
//		{
//			(void)setval((INTBIG)tech, VTECHNOLOGY, x_("TECH_layer_3dheight"), (INTBIG)us_tec3d_height,
//				VINTEGER|VDONTSAVE|VISARRAY|(tech.layercount<<VLENGTHSH));
//			(void)setval((INTBIG)tech, VTECHNOLOGY, x_("TECH_layer_3dthickness"), (INTBIG)us_tec3d_thickness,
//				VINTEGER|VDONTSAVE|VISARRAY|(tech.layercount<<VLENGTHSH));
//		}
//		if ((us_tecflags&HASPRINTCOL) != 0)
//			(void)setval((INTBIG)tech, VTECHNOLOGY, x_("USER_print_colors"), (INTBIG)us_tecprint_colors,
//				VINTEGER|VDONTSAVE|VISARRAY|((tech.layercount*5)<<VLENGTHSH));
//	
//	
//		efree((CHAR *)sequence);
//		return(FALSE);
//	}
//	
//	/*
//	 * routine to scan the "dependentlibcount" libraries in "dependentlibs",
//	 * and build the arc structures for it in technology "tech".  Returns true on error.
//	 */
//	BOOLEAN us_tecedmakearcs(LIBRARY **dependentlibs, INTBIG dependentlibcount,
//		TECHNOLOGY *tech)
//	{
//		REGISTER NODEPROTO *np;
//		NODEPROTO **sequence;
//		REGISTER INTBIG arcindex, count, j, k, layerindex, typ;
//		REGISTER INTBIG maxwid, hwid, wid, lambda;
//		REGISTER CHAR *str;
//		REGISTER NODEINST *ni;
//		REGISTER EXAMPLE *nelist;
//		REGISTER SAMPLE *ns;
//		REGISTER VARIABLE *var;
//	
//		// count the number of arcs in the technology
//		us_tecarc_count = us_teceditfindsequence(dependentlibs, "arc-", Generate.ARCSEQUENCE_KEY);
//		if (us_tecarc_count <= 0)
//		{
//			ttyputerr(_("No arcs found"));
//			if ((us_tool.toolstate&NODETAILS) == 0)
//				ttyputerr(_("Create them with the 'edit-arc' option"));
//			return(TRUE);
//		}
//		tech.arcprotocount = us_tecarc_count;
//	
//		// allocate the arcs
//		tech.arcprotos = (TECH_ARCS **)emalloc(((us_tecarc_count+1) * (sizeof (TECH_ARCS *))),
//			tech.cluster);
//		if (tech.arcprotos == 0) return(TRUE);
//		tech.arcprotos[us_tecarc_count] = ((TECH_ARCS *)-1);
//		us_tecarc_widoff = emalloc((us_tecarc_count * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tecarc_widoff == 0) return(TRUE);
//	
//		// create the arc structures
//		lambda = el_curlib.lambda[art_tech.techindex];
//		for(arcindex=0; arcindex<us_tecarc_count; arcindex++)
//		{
//			// build a list of examples found in this arc
//			np = sequence[arcindex];
//			nelist = us_tecedgetexamples(np, FALSE);
//			if (nelist == NOEXAMPLE) return(TRUE);
//			if (nelist.nextexample != NOEXAMPLE)
//			{
//				us_tecedpointout(NONODEINST, np);
//				ttyputerr(_("Can only be one example of %s but more were found"),
//					describenodeproto(np));
//				us_tecedfreeexamples(nelist);
//				return(TRUE);
//			}
//	
//			// get width and polygon count information
//			count = 0;
//			maxwid = hwid = -1;
//			for(ns = nelist.firstsample; ns != NOSAMPLE; ns = ns.nextsample)
//			{
//				wid = mini(ns.node.highx - ns.node.lowx, ns.node.highy - ns.node.lowy);
//				if (wid > maxwid) maxwid = wid;
//				if (ns.layer == NONODEPROTO) hwid = wid; else count++;
//			}
//	
//			// error if there is no highlight box
//			if (hwid < 0)
//			{
//				us_tecedpointout(NONODEINST, np);
//				ttyputerr(_("No highlight layer found in %s"), describenodeproto(np));
//				if ((us_tool.toolstate&NODETAILS) == 0)
//					ttyputmsg(_("Use 'place-layer' option to create HIGHLIGHT"));
//				us_tecedfreeexamples(nelist);
//				return(TRUE);
//			}
//	
//			// create and fill the basic structure entries for this arc
//			tech.arcprotos[arcindex] = (TECH_ARCS *)emalloc(sizeof (TECH_ARCS), tech.cluster);
//			if (tech.arcprotos[arcindex] == 0) return(TRUE);
//			(void)allocstring(&tech.arcprotos[arcindex].arcname, &np.protoname[4],
//				tech.cluster);
//			tech.arcprotos[arcindex].arcwidth = maxwid * WHOLE / lambda;
//			tech.arcprotos[arcindex].arcindex = arcindex;
//			tech.arcprotos[arcindex].creation = NOARCPROTO;
//			tech.arcprotos[arcindex].laycount = count;
//			us_tecarc_widoff[arcindex] = (maxwid - hwid) * WHOLE / lambda;
//			if (us_tecarc_widoff[arcindex] != 0) us_tecflags |= HASARCWID;
//	
//			// look for descriptive nodes in the cell
//			tech.arcprotos[arcindex].initialbits = 0;
//			for(ni = np.firstnodeinst; ni != NONODEINST; ni = ni.nextnodeinst)
//			{
//				typ = us_tecedgetoption(ni);
//				var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, art_messagekey);
//				if (var == NOVARIABLE) continue;
//	
//				// the "Function:" node
//				if (typ == ARCFUNCTION)
//				{
//					str = us_teceditgetparameter(var);
//					for(j=0; us_tecarc_functions[j].name != 0; j++)
//						if (namesame(str, us_tecarc_functions[j].name) == 0)
//					{
//						tech.arcprotos[arcindex].initialbits |=
//							(us_tecarc_functions[j].value << AFUNCTIONSH);
//						break;
//					}
//				}
//	
//				// the "Fixed-angle:" node
//				if (typ == ARCFIXANG)
//				{
//					str = (CHAR *)var.addr;
//					if (str[9] == ':') str = &str[11]; else str = &str[13];
//					if (namesame(str, x_("yes")) == 0)
//						tech.arcprotos[arcindex].initialbits |= WANTFIXANG;
//				}
//	
//				// the "Wipes pins:" node
//				if (typ == ARCWIPESPINS)
//				{
//					str = us_teceditgetparameter(var);
//					if (namesame(str, x_("yes")) == 0)
//						tech.arcprotos[arcindex].initialbits |= CANWIPE;
//				}
//	
//				// the "Extend arcs:" node
//				if (typ == ARCNOEXTEND)
//				{
//					str = us_teceditgetparameter(var);
//					if (namesame(str, x_("no")) == 0)
//						tech.arcprotos[arcindex].initialbits |= WANTNOEXTEND;
//				}
//	
//				// the "Angle increment:" node
//				if (typ == ARCINC)
//				{
//					str = us_teceditgetparameter(var);
//					j = myatoi(str) % 360;
//					if (j < 0) j += 360;
//					tech.arcprotos[arcindex].initialbits &= ~AANGLEINC;
//					tech.arcprotos[arcindex].initialbits |= (j << AANGLEINCSH);
//				}
//			}
//	
//			// allocate the individual arc layer structures
//			tech.arcprotos[arcindex].list = (TECH_ARCLAY *)emalloc((count * (sizeof (TECH_ARCLAY))),
//				tech.cluster);
//			if (tech.arcprotos[arcindex].list == 0) return(TRUE);
//	
//			// fill the individual arc layer structures
//			layerindex = 0;
//			for(k=0; k<2; k++)
//				for(ns = nelist.firstsample; ns != NOSAMPLE; ns = ns.nextsample)
//			{
//				if (ns.layer == NONODEPROTO) continue;
//	
//				// get the layer index
//				for(j=0; j<tech.layercount; j++)
//					if (namesame(&ns.layer.protoname[6], us_teclayer_names[j]) == 0) break;
//				if (j >= tech.layercount)
//				{
//					ttyputerr(_("Cannot find layer %s, used in %s"), describenodeproto(ns.layer),
//						describenodeproto(np));
//					us_tecedfreeexamples(nelist);
//					return(TRUE);
//				}
//	
//				// only add transparent layers when k=0
//				if (k == 0)
//				{
//					if (tech.layers[j].bits == LAYERO) continue;
//				} else
//				{
//					if (tech.layers[j].bits != LAYERO) continue;
//				}
//	
//				tech.arcprotos[arcindex].list[layerindex].lay = j;
//	
//				// determine the style of this arc layer
//				if (ns.node.proto == art_filledboxprim)
//					tech.arcprotos[arcindex].list[layerindex].style = FILLED; else
//						tech.arcprotos[arcindex].list[layerindex].style = CLOSED;
//	
//				// determine the width offset of this arc layer
//				wid = mini(ns.node.highx-ns.node.lowx, ns.node.highy-ns.node.lowy);
//				tech.arcprotos[arcindex].list[layerindex].off = (maxwid-wid) * WHOLE /
//					lambda;
//	
//				layerindex++;
//			}
//			us_tecedfreeexamples(nelist);
//		}
//	
//		// store width offset on the technology
//		if ((us_tecflags&HASARCWID) != 0)
//			(void)setval((INTBIG)tech, VTECHNOLOGY, x_("TECH_arc_width_offset"), (INTBIG)us_tecarc_widoff,
//				VFRACT|VDONTSAVE|VISARRAY|(us_tecarc_count<<VLENGTHSH));
//		efree((CHAR *)sequence);
//		return(FALSE);
//	}
//	
//	/*
//	 * routine to scan the "dependentlibcount" libraries in "dependentlibs",
//	 * and build the node structures for it in technology "tech".  Returns true on error.
//	 */
//	BOOLEAN us_tecedmakenodes(LIBRARY **dependentlibs, INTBIG dependentlibcount,
//		TECHNOLOGY *tech)
//	{
//		REGISTER NODEPROTO *np;
//		NODEPROTO **sequence;
//		REGISTER NODEINST *ni;
//		REGISTER VARIABLE *var;
//		REGISTER CHAR *str, *portname;
//		REGISTER INTBIG *list, save1, nfunction, x1pos, x2pos, y1pos, y2pos, net, lambda;
//		REGISTER INTBIG i, j, k, l, m, pass, nodeindex, sty, difindex, polindex,
//			serpdifind, opt, nsindex, err, portchecked;
//		INTBIG pol1port, pol2port, dif1port, dif2port;
//		INTBIG serprule[8];
//		REGISTER EXAMPLE *nelist;
//		REGISTER SAMPLE *ns, *ons, *diflayer, *pollayer;
//		REGISTER PCON *pc;
//		REGISTER RULE *r;
//		REGISTER TECH_NODES *tlist;
//	
//		// no rectangle rules
//		us_tecedfirstrule = NORULE;
//	
//		us_tecnode_count = us_teceditfindsequence(dependentlibs, "node-", Generate.NODESEQUENCE_KEY);
//		if (us_tecnode_count <= 0)
//		{
//			ttyputerr(_("No nodes found"));
//			if ((us_tool.toolstate&NODETAILS) == 0)
//				ttyputerr(_("Create them with the 'edit-node' option"));
//			return(TRUE);
//		}
//		tech.nodeprotocount = us_tecnode_count;
//	
//		// allocate the nodes
//		tech.nodeprotos = (TECH_NODES **)emalloc((us_tecnode_count+1) *
//			(sizeof (TECH_NODES *)), tech.cluster);
//		if (tech.nodeprotos == 0) return(TRUE);
//		tech.nodeprotos[us_tecnode_count] = ((TECH_NODES *)-1);
//		us_tecnode_widoff = emalloc((4*us_tecnode_count * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tecnode_widoff == 0) return(TRUE);
//		us_tecnode_grab = emalloc((3*us_tecnode_count * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tecnode_grab == 0) return(TRUE);
//		us_tecnode_grabcount = 0;
//	
//		// get the nodes
//		lambda = el_curlib.lambda[art_tech.techindex];
//		nodeindex = 0;
//		for(pass=0; pass<3; pass++)
//			for(m=0; m<us_tecnode_count; m++)
//		{
//			// make sure this is the right type of node for this pass of the nodes
//			np = sequence[m];
//			nfunction = NPUNKNOWN;
//			for(ni = np.firstnodeinst; ni != NONODEINST; ni = ni.nextnodeinst)
//			{
//				// get the node function
//				if (us_tecedgetoption(ni) != NODEFUNCTION) continue;
//				var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, art_messagekey);
//				if (var == NOVARIABLE) continue;
//				str = us_teceditgetparameter(var);
//				for(j=0; j<MAXNODEFUNCTION; j++)
//					if (namesame(str, nodefunctionname(j, NONODEINST)) == 0) break;
//				if (j < MAXNODEFUNCTION)
//				{
//					nfunction = j;
//					break;
//				}
//			}
//	
//			// only want pins on pass 0, pure-layer nodes on pass 2
//			if (pass == 0 && nfunction != NPPIN) continue;
//			if (pass == 1 && (nfunction == NPPIN || nfunction == NPNODE)) continue;
//			if (pass == 2 && nfunction != NPNODE) continue;
//	
//			// build a list of examples found in this node
//			nelist = us_tecedgetexamples(np, TRUE);
//			if (nelist == NOEXAMPLE) return(TRUE);
//	
//			// associate the samples in each example
//			if (us_tecedassociateexamples(nelist, np))
//			{
//				us_tecedfreeexamples(nelist);
//				return(TRUE);
//			}
//	
//			// allocate and fill the TECH_NODES structure
//			tlist = (TECH_NODES *)emalloc(sizeof (TECH_NODES), tech.cluster);
//			if (tlist == 0) return(TRUE);
//			tech.nodeprotos[nodeindex] = tlist;
//			(void)allocstring(&tlist.nodename, &np.protoname[5], tech.cluster);
//			tlist.nodeindex = (INTSML)(nodeindex + 1);
//			tlist.creation = NONODEPROTO;
//			tlist.xsize = (nelist.hx-nelist.lx)*WHOLE/lambda;
//			tlist.ysize = (nelist.hy-nelist.ly)*WHOLE/lambda;
//			tlist.layerlist = 0;
//			tlist.layercount = 0;
//			tlist.special = 0;
//			tlist.f1 = 0;
//			tlist.f2 = 0;
//			tlist.f3 = 0;
//			tlist.f4 = 0;
//			tlist.f5 = 0;
//			tlist.f6 = 0;
//			tlist.gra = 0;
//			tlist.ele = 0;
//	
//			// determine user bits
//			tlist.initialbits = nfunction<<NFUNCTIONSH;
//			for(ni = np.firstnodeinst; ni != NONODEINST; ni = ni.nextnodeinst)
//			{
//				opt = us_tecedgetoption(ni);
//	
//				// pick up square node information
//				if (opt == NODESQUARE)
//				{
//					var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, art_messagekey);
//					if (var == NOVARIABLE) continue;
//					str = us_teceditgetparameter(var);
//					if (namesame(str, x_("yes")) == 0) tlist.initialbits |= NSQUARE;
//					continue;
//				}
//	
//				// pick up invisible on 1 or 2 arc information
//				if (opt == NODEWIPES)
//				{
//					var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, art_messagekey);
//					if (var == NOVARIABLE) continue;
//					str = us_teceditgetparameter(var);
//					if (namesame(str, x_("yes")) == 0)
//						tlist.initialbits = WIPEON1OR2 | (tlist.initialbits & ~(ARCSWIPE|ARCSHRINK));
//					continue;
//				}
//	
//				// pick up lockable information
//				if (opt == NODELOCKABLE)
//				{
//					var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, art_messagekey);
//					if (var == NOVARIABLE) continue;
//					str = us_teceditgetparameter(var);
//					if (namesame(str, x_("yes")) == 0) tlist.initialbits |= LOCKEDPRIM;
//					continue;
//				}
//	
//				// pick up multicut information
//				if (opt == NODEMULTICUT)
//				{
//					var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, art_messagekey);
//					if (var == NOVARIABLE) continue;
//					str = us_teceditgetparameter(var);
//					tlist.f4 = (INTSML)atofr(str);
//					continue;
//				}
//	
//				// pick up serpentine transistor information
//				if (opt == NODESERPENTINE)
//				{
//					var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, art_messagekey);
//					if (var == NOVARIABLE) continue;
//					str = us_teceditgetparameter(var);
//					if (namesame(str, x_("yes")) == 0)
//					{
//						if (tlist.special != 0)
//						{
//							us_tecedpointout(ni, np);
//							ttyputerr(_("Serpentine %s must have Transistor function"),
//								describenodeproto(np));
//							us_tecedfreeexamples(nelist);
//							return(TRUE);
//						}
//						tlist.special = SERPTRANS;
//						tlist.initialbits |= (NODESHRINK | HOLDSTRACE);
//					}
//					continue;
//				}
//			}
//	
//			// derive primitives from the examples
//			if (us_tecedmakeprim(nelist, np, tech, tlist, lambda))
//			{
//				us_tecedfreeexamples(nelist);
//				efree((CHAR *)tlist.nodename);
//				efree((CHAR *)tlist);
//				return(TRUE);
//			}
//	
//			// analyze special node function circumstances
//			switch (nfunction)
//			{
//				case NPNODE:
//					if (tlist.special != 0)
//					{
//						us_tecedpointout(NONODEINST, np);
//						ttyputerr(_("Pure layer %s can not be serpentine"), describenodeproto(np));
//						us_tecedfreeexamples(nelist);
//						return(TRUE);
//					}
//					tlist.special = POLYGONAL;
//					tlist.initialbits |= HOLDSTRACE;
//					break;
//				case NPPIN:
//					if ((tlist.initialbits&WIPEON1OR2) == 0)
//					{
//						tlist.initialbits |= ARCSWIPE;
//						tlist.initialbits |= ARCSHRINK;
//					}
//					break;
//			}
//	
//			// count the number of ports on this node
//			tlist.portcount = 0;
//			for(ns = nelist.firstsample; ns != NOSAMPLE; ns = ns.nextsample)
//				if (ns.layer == gen_portprim) tlist.portcount++;
//			if (tlist.portcount == 0)
//			{
//				us_tecedpointout(NONODEINST, np);
//				ttyputerr(_("No ports found in %s"), describenodeproto(np));
//				if ((us_tool.toolstate&NODETAILS) == 0)
//					ttyputmsg(_("Use 'place-layer port' option to create one"));
//				us_tecedfreeexamples(nelist);
//				return(TRUE);
//			}
//	
//			// allocate space for the ports
//			tlist.portlist = (TECH_PORTS *)emalloc((tlist.portcount * (sizeof (TECH_PORTS))),
//				tech.cluster);
//			if (tlist.portlist == 0) return(TRUE);
//	
//			// fill the port structures
//			pol1port = pol2port = dif1port = dif2port = -1;
//			i = 0;
//			for(ns = nelist.firstsample; ns != NOSAMPLE; ns = ns.nextsample)
//			{
//				if (ns.layer != gen_portprim) continue;
//	
//				// port connections
//				var = ns.node.getVar(Generate.CONNECTION_KEY);
//				if (var == NOVARIABLE) pc = us_tecedaddportlist(0, (INTBIG *)0); else
//				{
//					// convert "arc-CELL" pointers to indices
//					l = getlength(var);
//					list = emalloc((l * SIZEOFINTBIG), el_tempcluster);
//					if (list == 0) return(TRUE);
//					portchecked = 0;
//					for(j=0; j<l; j++)
//					{
//						// find arc that connects
//						for(k=0; k<tech.arcprotocount; k++)
//							if (namesame(tech.arcprotos[k].arcname,
//								&((NODEPROTO **)var.addr)[j].protoname[4]) == 0) break;
//						if (k >= tech.arcprotocount)
//						{
//							us_tecedpointout(ns.node, ns.node.parent);
//							ttyputerr(_("Invalid connection list on port in %s"),
//								describenodeproto(np));
//							if ((us_tool.toolstate&NODETAILS) == 0)
//								ttyputmsg(_("Use 'change' option to remove arc %s"),
//									&((NODEPROTO **)var.addr)[j].protoname[4]);
//							us_tecedfreeexamples(nelist);
//							return(TRUE);
//						}
//	
//						// fill in the connection information
//						list[j] = k;
//	
//						// find port characteristics for possible transistors
//						if (portchecked != 0) continue;
//						switch ((tech.arcprotos[k].initialbits&AFUNCTION)>>AFUNCTIONSH)
//						{
//							case APPOLY1:   case APPOLY2:
//								if (pol1port < 0)
//								{
//									pol1port = i;
//									portchecked++;
//								} else if (pol2port < 0)
//								{
//									pol2port = i;
//									portchecked++;
//								}
//								break;
//							case APDIFF:    case APDIFFP:   case APDIFFN:
//							case APDIFFS:   case APDIFFW:
//								if (dif1port < 0)
//								{
//									dif1port = i;
//									portchecked++;
//								} else if (dif2port < 0)
//								{
//									dif2port = i;
//									portchecked++;
//								}
//								break;
//						}
//					}
//	
//					// store complete list in memory
//					pc = us_tecedaddportlist(l, list);
//					efree((CHAR *)list);
//				}
//	
//				// link connection list to the port
//				if (pc == NOPCON) return(TRUE);
//				tlist.portlist[i].portarcs = pc.connects;
//	
//				// port name
//				portname = us_tecedgetportname(ns.node);
//				if (portname == 0)
//				{
//					us_tecedpointout(ns.node, np);
//					ttyputerr(_("Cell %s: port does not have a name"), describenodeproto(np));
//					us_tecedfreeexamples(nelist);
//					return(TRUE);
//				}
//				for(str = portname; *str != 0; str++)
//					if (*str <= ' ' || *str >= 0177)
//				{
//					us_tecedpointout(ns.node, np);
//					ttyputerr(_("Invalid port name '%s' in %s"), portname,
//						describenodeproto(np));
//					us_tecedfreeexamples(nelist);
//					return(TRUE);
//				}
//				(void)allocstring(&tlist.portlist[i].protoname, portname, tech.cluster);
//	
//				// port angle and range
//				tlist.portlist[i].initialbits = 0;
//				var = ns.node.getVar(Generate.PORTANGLE_KEY);
//				if (var != NOVARIABLE)
//					tlist.portlist[i].initialbits |= var.addr << PORTANGLESH;
//				var = ns.node.getVar(Generate.PORTRANGE_KEY);
//				if (var != NOVARIABLE)
//					tlist.portlist[i].initialbits |= var.addr << PORTARANGESH; else
//						tlist.portlist[i].initialbits |= 180 << PORTARANGESH;
//	
//				// port connectivity
//				net = i;
//				if (ns.node.firstportarcinst != NOPORTARCINST)
//				{
//					j = 0;
//					for(ons = nelist.firstsample; ons != ns; ons = ons.nextsample)
//					{
//						if (ons.layer != gen_portprim) continue;
//						if (ons.node.firstportarcinst != NOPORTARCINST)
//						{
//							if (ns.node.firstportarcinst.conarcinst.network ==
//								ons.node.firstportarcinst.conarcinst.network)
//							{
//								net = j;
//								break;
//							}
//						}
//						j++;
//					}
//				}
//				tlist.portlist[i].initialbits |= (net << PORTNETSH);
//	
//				// port area rule
//				r = ns.rule;
//				if (r == NORULE) continue;
//				tlist.portlist[i].lowxmul = (INTSML)r.value[0];
//				tlist.portlist[i].lowxsum = (INTSML)r.value[1];
//				tlist.portlist[i].lowymul = (INTSML)r.value[2];
//				tlist.portlist[i].lowysum = (INTSML)r.value[3];
//				tlist.portlist[i].highxmul = (INTSML)r.value[4];
//				tlist.portlist[i].highxsum = (INTSML)r.value[5];
//				tlist.portlist[i].highymul = (INTSML)r.value[6];
//				tlist.portlist[i].highysum = (INTSML)r.value[7];
//				i++;
//			}
//	
//			// on FET transistors, make sure ports 0 and 2 are poly
//			if (nfunction == NPTRANMOS || nfunction == NPTRADMOS || nfunction == NPTRAPMOS ||
//				nfunction == NPTRADMES || nfunction == NPTRAEMES)
//			{
//				if (pol1port < 0 || pol2port < 0 || dif1port < 0 || dif2port < 0)
//				{
//					us_tecedpointout(NONODEINST, np);
//					ttyputerr(_("Need 2 gate and 2 active ports on field-effect transistor %s"),
//						describenodeproto(np));
//					us_tecedfreeexamples(nelist);
//					return(TRUE);
//				}
//				if (pol1port != 0)
//				{
//					if (pol2port == 0) us_tecedswapports(&pol1port, &pol2port, tlist); else
//					if (dif1port == 0) us_tecedswapports(&pol1port, &dif1port, tlist); else
//					if (dif2port == 0) us_tecedswapports(&pol1port, &dif2port, tlist);
//				}
//				if (pol2port != 2)
//				{
//					if (dif1port == 2) us_tecedswapports(&pol2port, &dif1port, tlist); else
//					if (dif2port == 2) us_tecedswapports(&pol2port, &dif2port, tlist);
//				}
//				if (dif1port != 1) us_tecedswapports(&dif1port, &dif2port, tlist);
//	
//				// also make sure that dif1port is positive and dif2port is negative
//				x1pos = (tlist.portlist[dif1port].lowxmul*tlist.xsize +
//					tlist.portlist[dif1port].lowxsum +
//						tlist.portlist[dif1port].highxmul*tlist.xsize +
//							tlist.portlist[dif1port].highxsum) / 2;
//				x2pos = (tlist.portlist[dif2port].lowxmul*tlist.xsize +
//					tlist.portlist[dif2port].lowxsum +
//						tlist.portlist[dif2port].highxmul*tlist.xsize +
//							tlist.portlist[dif2port].highxsum) / 2;
//				y1pos = (tlist.portlist[dif1port].lowymul*tlist.ysize +
//					tlist.portlist[dif1port].lowysum +
//						tlist.portlist[dif1port].highymul*tlist.ysize +
//							tlist.portlist[dif1port].highysum) / 2;
//				y2pos = (tlist.portlist[dif2port].lowymul*tlist.ysize +
//					tlist.portlist[dif2port].lowysum +
//						tlist.portlist[dif2port].highymul*tlist.ysize +
//							tlist.portlist[dif2port].highysum) / 2;
//				if (abs(x1pos-x2pos) > abs(y1pos-y2pos))
//				{
//					if (x1pos < x2pos)
//					{
//						us_tecedswapports(&dif1port, &dif2port, tlist);
//						j = dif1port;   dif1port = dif2port;   dif2port = j;
//					}
//				} else
//				{
//					if (y1pos < y2pos)
//					{
//						us_tecedswapports(&dif1port, &dif2port, tlist);
//						j = dif1port;   dif1port = dif2port;   dif2port = j;
//					}
//				}
//	
//				// also make sure that pol1port is negative and pol2port is positive
//				x1pos = (tlist.portlist[pol1port].lowxmul*tlist.xsize +
//					tlist.portlist[pol1port].lowxsum +
//						tlist.portlist[pol1port].highxmul*tlist.xsize +
//							tlist.portlist[pol1port].highxsum) / 2;
//				x2pos = (tlist.portlist[pol2port].lowxmul*tlist.xsize +
//					tlist.portlist[pol2port].lowxsum +
//						tlist.portlist[pol2port].highxmul*tlist.xsize +
//							tlist.portlist[pol2port].highxsum) / 2;
//				y1pos = (tlist.portlist[pol1port].lowymul*tlist.ysize +
//					tlist.portlist[pol1port].lowysum +
//						tlist.portlist[pol1port].highymul*tlist.ysize +
//							tlist.portlist[pol1port].highysum) / 2;
//				y2pos = (tlist.portlist[pol2port].lowymul*tlist.ysize +
//					tlist.portlist[pol2port].lowysum +
//						tlist.portlist[pol2port].highymul*tlist.ysize +
//							tlist.portlist[pol2port].highysum) / 2;
//				if (abs(x1pos-x2pos) > abs(y1pos-y2pos))
//				{
//					if (x1pos > x2pos)
//					{
//						us_tecedswapports(&pol1port, &pol2port, tlist);
//						j = pol1port;   pol1port = pol2port;   pol2port = j;
//					}
//				} else
//				{
//					if (y1pos > y2pos)
//					{
//						us_tecedswapports(&pol1port, &pol2port, tlist);
//						j = pol1port;   pol1port = pol2port;   pol2port = j;
//					}
//				}
//			}
//	
//			// count the number of layers on the node
//			tlist.layercount = 0;
//			for(ns = nelist.firstsample; ns != NOSAMPLE; ns = ns.nextsample)
//			{
//				if (ns.rule != NORULE && ns.layer != gen_portprim &&
//					ns.layer != gen_cellcenterprim && ns.layer != NONODEPROTO)
//						tlist.layercount++;
//			}
//	
//			// allocate space for the layers
//			if (tlist.special != SERPTRANS)
//			{
//				tlist.layerlist = (TECH_POLYGON *)emalloc((tlist.layercount *
//					(sizeof (TECH_POLYGON))), tech.cluster);
//				if (tlist.layerlist == 0) return(TRUE);
//			} else
//			{
//				tlist.gra = (TECH_SERPENT *)emalloc(((sizeof (TECH_SERPENT)) * tlist.layercount),
//					tech.cluster);
//				if (tlist.gra == 0) return(TRUE);
//				tlist.ele = (TECH_SERPENT *)emalloc(((sizeof (TECH_SERPENT)) * (tlist.layercount+1)),
//					tech.cluster);
//				if (tlist.ele == 0) return(TRUE);
//			}
//	
//			// fill the layer structures (3 times: transparent, opaque, multicut)
//			i = 0;
//			pollayer = diflayer = NOSAMPLE;
//			for(k=0; k<3; k++)
//				for(nsindex=0, ns = nelist.firstsample; ns != NOSAMPLE; nsindex++, ns = ns.nextsample)
//			{
//				r = ns.rule;
//				if (r == NORULE || ns.layer == gen_portprim ||
//					ns.layer == gen_cellcenterprim || ns.layer == NONODEPROTO) continue;
//	
//				// add cut layers last (only when k=2)
//				if (k == 2)
//				{
//					if (!r.multicut) continue;
//					if (tlist.special != 0)
//					{
//						us_tecedpointout(ns.node, ns.node.parent);
//						ttyputerr(_("%s is too complex (multiple cuts AND serpentine)"),
//							describenodeproto(np));
//						us_tecedfreeexamples(nelist);
//						return(TRUE);
//					}
//					tlist.special = MULTICUT;
//					tlist.f1 = (INTSML)(r.multixs*WHOLE/lambda);
//					tlist.f2 = (INTSML)(r.multiys*WHOLE/lambda);
//					tlist.f3 = (INTSML)(r.multiindent*WHOLE/lambda);
//					tlist.f4 = (INTSML)(r.multisep*WHOLE/lambda);
//				} else
//				{
//					if (r.multicut) continue;
//				}
//	
//				// layer number
//				for(j=0; j<tech.layercount; j++)
//					if (namesame(&ns.layer.protoname[6], us_teclayer_names[j]) == 0) break;
//				if (j >= tech.layercount)
//				{
//					ttyputerr(_("Cannot find layer %s in %s"), describenodeproto(ns.layer),
//						describenodeproto(np));
//					return(TRUE);
//				}
//	
//				// only add transparent layers when k=0
//				if (k == 0)
//				{
//					if (tech.layers[j].bits == LAYERO) continue;
//				} else if (k == 1)
//				{
//					if (tech.layers[j].bits != LAYERO) continue;
//				}
//	
//				// layer style
//				sty = -1;
//				if (ns.node.proto == art_filledboxprim)             sty = FILLEDRECT; else
//				if (ns.node.proto == art_boxprim)                   sty = CLOSEDRECT; else
//				if (ns.node.proto == art_crossedboxprim)            sty = CROSSED; else
//				if (ns.node.proto == art_filledpolygonprim)         sty = FILLED; else
//				if (ns.node.proto == art_closedpolygonprim)         sty = CLOSED; else
//				if (ns.node.proto == art_openedpolygonprim)         sty = OPENED; else
//				if (ns.node.proto == art_openeddottedpolygonprim)   sty = OPENEDT1; else
//				if (ns.node.proto == art_openeddashedpolygonprim)   sty = OPENEDT2; else
//				if (ns.node.proto == art_openedthickerpolygonprim)  sty = OPENEDT3; else
//				if (ns.node.proto == art_filledcircleprim)          sty = DISC; else
//				if (ns.node.proto == art_circleprim)
//				{
//					var = getvalkey((INTBIG)ns.node, VNODEINST, VINTEGER, art_degreeskey);
//					if (var != NOVARIABLE) sty = CIRCLEARC; else sty = CIRCLE;
//				} else if (ns.node.proto == art_thickcircleprim)
//				{
//					var = getvalkey((INTBIG)ns.node, VNODEINST, VINTEGER, art_degreeskey);
//					if (var != NOVARIABLE) sty = THICKCIRCLEARC; else sty = THICKCIRCLE;
//				} else if (ns.node.proto == gen_invispinprim)
//				{
//					var = getvalkey((INTBIG)ns.node, VNODEINST, VSTRING|VISARRAY, art_messagekey);
//					if (var != NOVARIABLE)
//					{
//						switch (TDGETPOS(var.textdescript))
//						{
//							case VTPOSBOXED:     sty = TEXTBOX;       break;
//							case VTPOSCENT:      sty = TEXTCENT;      break;
//							case VTPOSUP:        sty = TEXTBOT;       break;
//							case VTPOSDOWN:      sty = TEXTTOP;       break;
//							case VTPOSLEFT:      sty = TEXTRIGHT;     break;
//							case VTPOSRIGHT:     sty = TEXTLEFT;      break;
//							case VTPOSUPLEFT:    sty = TEXTBOTRIGHT;  break;
//							case VTPOSUPRIGHT:   sty = TEXTBOTLEFT;   break;
//							case VTPOSDOWNLEFT:  sty = TEXTTOPRIGHT;  break;
//							case VTPOSDOWNRIGHT: sty = TEXTTOPLEFT;   break;
//						}
//					}
//				}
//				if (sty == -1)
//					ttyputmsg(_("Cannot determine style to use for %s node in %s"),
//						describenodeproto(ns.node.proto), describenodeproto(np));
//	
//				// load the layer structure(s)
//				if (tlist.special == SERPTRANS)
//				{
//					// determine port numbers for serpentine transistors
//					if (layerismetal(us_teclayer_function[j]))
//					{
//						tlist.gra[i].basics.portnum = 0;
//					} else if (layerispoly(us_teclayer_function[j]))
//					{
//						pollayer = ns;
//						if (pol1port >= 0)
//							tlist.gra[i].basics.portnum = (INTSML)pol1port; else
//								tlist.gra[i].basics.portnum = 0;
//						polindex = i;
//					} else if ((us_teclayer_function[j]&LFTYPE) == LFDIFF)
//					{
//						diflayer = ns;
//						difindex = i;
//						tlist.gra[i].basics.portnum = 0;
//					} else
//					{
//						tlist.gra[i].basics.portnum = -1;
//					}
//	
//					tlist.gra[i].basics.layernum = (INTSML)j;
//					tlist.gra[i].basics.count = (INTSML)(r.count/4);
//					if (sty == CROSSED || sty == FILLEDRECT || sty == FILLED || sty == CLOSEDRECT ||
//						sty == CLOSED)
//					{
//						if (tlist.gra[i].basics.count == 4)
//						{
//							ttyputmsg(_("Ignoring Minimum-Size setting on layer %s in serpentine transistor %s"),
//								&ns.layer.protoname[6], &np.protoname[5]);
//							tlist.gra[i].basics.count = 2;
//						}
//					}
//					tlist.gra[i].basics.style = (INTSML)sty;
//					if (tlist.gra[i].basics.count == 2 && (sty == CROSSED ||
//						sty == FILLEDRECT || sty == FILLED || sty == CLOSEDRECT || sty == CLOSED))
//					{
//						tlist.gra[i].basics.representation = BOX;
//						tlist.gra[i].basics.count = 4;
//					} else tlist.gra[i].basics.representation = POINTS;
//					tlist.gra[i].basics.points = r.value;
//					tlist.gra[i].lwidth = (INTSML)nsindex;
//					tlist.gra[i].rwidth = 0;
//					tlist.gra[i].extendt = 0;
//					tlist.gra[i].extendb = 0;
//				} else
//				{
//					tlist.layerlist[i].portnum = (INTSML)us_tecedfindport(tlist, nelist,
//						ns.node.lowx, ns.node.highx, ns.node.lowy, ns.node.highy,
//							lambdaofnode(ns.node));
//					tlist.layerlist[i].layernum = (INTSML)j;
//					tlist.layerlist[i].count = (INTSML)(r.count/4);
//					tlist.layerlist[i].style = (INTSML)sty;
//					tlist.layerlist[i].representation = POINTS;
//					if (sty == CROSSED || sty == FILLEDRECT || sty == FILLED || sty == CLOSEDRECT ||
//						sty == CLOSED)
//					{
//						if (r.count == 8)
//						{
//							tlist.layerlist[i].representation = BOX;
//							tlist.layerlist[i].count = 4;
//						} else if (r.count == 16)
//						{
//							tlist.layerlist[i].representation = MINBOX;
//							tlist.layerlist[i].count = 4;
//						}
//					}
//					tlist.layerlist[i].points = r.value;
//				}
//	
//				// mark this rectangle rule "used"
//				r.used = TRUE;
//				i++;
//			}
//	
//			// finish up serpentine transistors
//			if (tlist.special == SERPTRANS)
//			{
//				if (diflayer == NOSAMPLE || pollayer == NOSAMPLE || dif1port < 0)
//				{
//					us_tecedpointout(NONODEINST, np);
//					ttyputerr(_("No diffusion and polysilicon layers in transistor %s"),
//						describenodeproto(np));
//					us_tecedfreeexamples(nelist);
//					return(TRUE);
//				}
//	
//				// compute port extension factors
//				tlist.f1 = tlist.layercount+1;
//				if (tlist.portlist[dif1port].lowxsum >
//					tlist.portlist[dif1port].lowysum)
//				{
//					// vertical diffusion layer: determine polysilicon width
//					tlist.f4 = (INTSML)((muldiv(tlist.ysize, tlist.gra[polindex].basics.points[6], WHOLE) +
//						tlist.gra[polindex].basics.points[7]) -
//							(muldiv(tlist.ysize, tlist.gra[polindex].basics.points[2], WHOLE) +
//								tlist.gra[polindex].basics.points[3]));
//	
//					// determine diffusion port rule
//					tlist.f2 = (INTSML)((muldiv(tlist.xsize, tlist.portlist[dif1port].lowxmul, WHOLE) +
//						tlist.portlist[dif1port].lowxsum) -
//							(muldiv(tlist.xsize, tlist.gra[difindex].basics.points[0], WHOLE) +
//								tlist.gra[difindex].basics.points[1]));
//					tlist.f3 = (INTSML)((muldiv(tlist.ysize, tlist.portlist[dif1port].lowymul, WHOLE) +
//						tlist.portlist[dif1port].lowysum) -
//							(muldiv(tlist.ysize, tlist.gra[polindex].basics.points[6], WHOLE) +
//								tlist.gra[polindex].basics.points[7]));
//	
//					// determine polysilicon port rule
//					tlist.f5 = (INTSML)((muldiv(tlist.ysize, tlist.portlist[pol1port].lowymul, WHOLE) +
//						tlist.portlist[pol1port].lowysum) -
//							(muldiv(tlist.ysize, tlist.gra[polindex].basics.points[2], WHOLE) +
//								tlist.gra[polindex].basics.points[3]));
//					tlist.f6 = (INTSML)((muldiv(tlist.xsize, tlist.gra[difindex].basics.points[0], WHOLE) +
//						tlist.gra[difindex].basics.points[1]) -
//							(muldiv(tlist.xsize, tlist.portlist[pol1port].highxmul, WHOLE) +
//								tlist.portlist[pol1port].highxsum));
//				} else
//				{
//					// horizontal diffusion layer: determine polysilicon width
//					tlist.f4 = (INTSML)((muldiv(tlist.xsize, tlist.gra[polindex].basics.points[4], WHOLE) +
//						tlist.gra[polindex].basics.points[5]) -
//							(muldiv(tlist.xsize, tlist.gra[polindex].basics.points[0], WHOLE) +
//								tlist.gra[polindex].basics.points[1]));
//	
//					// determine diffusion port rule
//					tlist.f2 = (INTSML)((muldiv(tlist.ysize, tlist.portlist[dif1port].lowymul, WHOLE) +
//						tlist.portlist[dif1port].lowysum) -
//							(muldiv(tlist.ysize, tlist.gra[difindex].basics.points[2], WHOLE) +
//								tlist.gra[difindex].basics.points[3]));
//					tlist.f3 = (INTSML)((muldiv(tlist.xsize, tlist.gra[polindex].basics.points[0], WHOLE) +
//						tlist.gra[polindex].basics.points[1]) -
//							(muldiv(tlist.xsize, tlist.portlist[dif1port].highxmul, WHOLE) +
//								tlist.portlist[dif1port].highxsum));
//	
//					// determine polysilicon port rule
//					tlist.f5 = (INTSML)((muldiv(tlist.xsize, tlist.portlist[pol1port].lowxmul, WHOLE) +
//						tlist.portlist[pol1port].lowxsum) -
//							(muldiv(tlist.xsize, tlist.gra[polindex].basics.points[0], WHOLE) +
//								tlist.gra[polindex].basics.points[1]));
//					tlist.f6 = (INTSML)((muldiv(tlist.ysize, tlist.gra[difindex].basics.points[2], WHOLE) +
//						tlist.gra[difindex].basics.points[3]) -
//							(muldiv(tlist.ysize, tlist.portlist[pol1port].highymul, WHOLE) +
//								tlist.portlist[pol1port].highysum));
//				}
//	
//				// find width and extension from comparison to poly layer
//				for(i=0; i<tlist.layercount; i++)
//				{
//					for(nsindex=0, ns = nelist.firstsample; ns != NOSAMPLE;
//						nsindex++, ns = ns.nextsample)
//							if (tlist.gra[i].lwidth == nsindex) break;
//					if (ns == NOSAMPLE)
//					{
//						us_tecedpointout(NONODEINST, np);
//						ttyputerr(_("Internal error in serpentine %s"), describenodeproto(np));
//						us_tecedfreeexamples(nelist);
//						continue;
//					}
//	
//					if (pollayer.node.highx-pollayer.node.lowx >
//						pollayer.node.highy-pollayer.node.lowy)
//					{
//						// horizontal layer
//						tlist.gra[i].lwidth = (INTSML)((ns.node.highy - (ns.parent.ly + ns.parent.hy)/2) *
//							WHOLE/lambda);
//						tlist.gra[i].rwidth = (INTSML)(((ns.parent.ly + ns.parent.hy)/2 - ns.node.lowy) *
//							WHOLE/lambda);
//						tlist.gra[i].extendt = (INTSML)((diflayer.node.lowx - ns.node.lowx) * WHOLE /
//							lambda);
//					} else
//					{
//						// vertical layer
//						tlist.gra[i].lwidth = (INTSML)((ns.node.highx - (ns.parent.lx + ns.parent.hx)/2) *
//							WHOLE/lambda);
//						tlist.gra[i].rwidth = (INTSML)(((ns.parent.lx + ns.parent.hx)/2 - ns.node.lowx) *
//							WHOLE/lambda);
//						tlist.gra[i].extendt = (INTSML)((diflayer.node.lowy - ns.node.lowy) * WHOLE /
//							lambda);
//					}
//					tlist.gra[i].extendb = tlist.gra[i].extendt;
//				}
//	
//				// copy basic graphics to electrical version, doubling diffusion
//				i = 0;
//				for(j=0; j<tlist.layercount; j++)
//				{
//					if (j != difindex) k = 1; else
//					{
//						k = 2;
//	
//						// copy rectangle rule and prepare for electrical layers
//						r = diflayer.rule;
//						if (r.count != 8)
//						{
//							us_tecedpointout(NONODEINST, np);
//							ttyputerr(_("Nonrectangular diffusion in Serpentine %s"),
//								describenodeproto(np));
//							us_tecedfreeexamples(nelist);
//							return(TRUE);
//						}
//						for(l=0; l<r.count; l++) serprule[l] = r.value[l];
//						if (serprule[0] != -H0 || serprule[2] != -H0 ||
//							serprule[4] != H0 || serprule[6] != H0)
//						{
//							us_tecedpointout(NONODEINST, np);
//							ttyputerr(_("Unusual diffusion in Serpentine %s"), describenodeproto(np));
//							us_tecedfreeexamples(nelist);
//							return(TRUE);
//						}
//						if (tlist.xsize - serprule[1] + serprule[5] <
//							tlist.ysize - serprule[3] + serprule[7]) serpdifind = 2; else
//								serpdifind = 0;
//					}
//					for(l=0; l<k; l++)
//					{
//						tlist.ele[i].basics.layernum = tlist.gra[j].basics.layernum;
//						tlist.ele[i].basics.count = tlist.gra[j].basics.count;
//						tlist.ele[i].basics.style = tlist.gra[j].basics.style;
//						tlist.ele[i].basics.representation = tlist.gra[j].basics.representation;
//						tlist.ele[i].basics.points = tlist.gra[j].basics.points;
//						tlist.ele[i].lwidth = tlist.gra[j].lwidth;
//						tlist.ele[i].rwidth = tlist.gra[j].rwidth;
//						tlist.ele[i].extendt = tlist.gra[j].extendt;
//						tlist.ele[i].extendb = tlist.gra[j].extendb;
//						if (k == 1) tlist.ele[i].basics.portnum = tlist.gra[j].basics.portnum; else
//							switch (l)
//						{
//							case 0:
//								tlist.ele[i].basics.portnum = (INTSML)dif1port;
//								tlist.ele[i].rwidth = -tlist.gra[polindex].lwidth;
//								save1 = serprule[serpdifind+1];
//	
//								// in transistor, diffusion stops in center
//								serprule[serpdifind] = 0;
//								serprule[serpdifind+1] = 0;
//								r = us_tecedaddrule(serprule, 8, FALSE, (CHAR *)0);
//								if (r == NORULE) return(TRUE);
//								r.used = TRUE;
//								tlist.ele[i].basics.points = r.value;
//								serprule[serpdifind] = -H0;
//								serprule[serpdifind+1] = save1;
//								break;
//							case 1:
//								tlist.ele[i].basics.portnum = (INTSML)dif2port;
//								tlist.ele[i].lwidth = -tlist.gra[polindex].rwidth;
//								save1 = serprule[serpdifind+5];
//	
//								// in transistor, diffusion stops in center
//								serprule[serpdifind+4] = 0;
//								serprule[serpdifind+5] = 0;
//								r = us_tecedaddrule(serprule, 8, FALSE, (CHAR *)0);
//								if (r == NORULE) return(TRUE);
//								r.used = TRUE;
//								tlist.ele[i].basics.points = r.value;
//								serprule[serpdifind+4] = H0;
//								serprule[serpdifind+5] = save1;
//								break;
//						}
//						i++;
//					}
//				}
//			}
//	
//			// extract width offset information
//			us_tecnode_widoff[nodeindex*4] = 0;
//			us_tecnode_widoff[nodeindex*4+1] = 0;
//			us_tecnode_widoff[nodeindex*4+2] = 0;
//			us_tecnode_widoff[nodeindex*4+3] = 0;
//			for(ns = nelist.firstsample; ns != NOSAMPLE; ns = ns.nextsample)
//				if (ns.layer == NONODEPROTO) break;
//			if (ns != NOSAMPLE)
//			{
//				r = ns.rule;
//				if (r != NORULE)
//				{
//					err = 0;
//					switch (r.value[0])		// left edge offset
//					{
//						case -H0:
//							us_tecnode_widoff[nodeindex*4] = r.value[1];
//							break;
//						case H0:
//							us_tecnode_widoff[nodeindex*4] = tlist.xsize + r.value[1];
//							break;
//						default:
//							err++;
//							break;
//					}
//					switch (r.value[2])		// bottom edge offset
//					{
//						case -H0:
//							us_tecnode_widoff[nodeindex*4+2] = r.value[3];
//							break;
//						case H0:
//							us_tecnode_widoff[nodeindex*4+2] = tlist.ysize + r.value[3];
//							break;
//						default:
//							err++;
//							break;
//					}
//					switch (r.value[4])		// right edge offset
//					{
//						case H0:
//							us_tecnode_widoff[nodeindex*4+1] = -r.value[5];
//							break;
//						case -H0:
//							us_tecnode_widoff[nodeindex*4+1] = tlist.xsize - r.value[5];
//							break;
//						default:
//							err++;
//							break;
//					}
//					switch (r.value[6])		// top edge offset
//					{
//						case H0:
//							us_tecnode_widoff[nodeindex*4+3] = -r.value[7];
//							break;
//						case -H0:
//							us_tecnode_widoff[nodeindex*4+3] = tlist.ysize - r.value[7];
//							break;
//						default:
//							err++;
//							break;
//					}
//					if (err != 0)
//					{
//						us_tecedpointout(ns.node, ns.node.parent);
//						ttyputmsg(_("Highlighting cannot scale from center in %s"), describenodeproto(np));
//						us_tecedfreeexamples(nelist);
//						return(TRUE);
//					}
//				} else
//				{
//					us_tecedpointout(ns.node, ns.node.parent);
//					ttyputerr(_("No rule found for highlight in %s"), describenodeproto(np));
//					us_tecedfreeexamples(nelist);
//					return(TRUE);
//				}
//			} else
//			{
//				us_tecedpointout(NONODEINST, np);
//				ttyputerr(_("No highlight found in %s"), describenodeproto(np));
//				if ((us_tool.toolstate&NODETAILS) == 0)
//					ttyputmsg(_("Use 'place-layer' option to create HIGHLIGHT"));
//				us_tecedfreeexamples(nelist);
//				return(TRUE);
//			}
//	
//			// get grab point information
//			for(ns = nelist.firstsample; ns != NOSAMPLE; ns = ns.nextsample)
//				if (ns.layer == gen_cellcenterprim) break;
//			if (ns != NOSAMPLE)
//			{
//				us_tecnode_grab[us_tecnode_grabcount++] = nodeindex+1;
//				us_tecnode_grab[us_tecnode_grabcount++] = (ns.node.geom.lowx +
//					ns.node.geom.highx - nelist.lx - nelist.hx)/2 *
//					el_curlib.lambda[tech.techindex] / lambda;
//				us_tecnode_grab[us_tecnode_grabcount++] = (ns.node.geom.lowy +
//					ns.node.geom.highy - nelist.ly - nelist.hy)/2 *
//					el_curlib.lambda[tech.techindex] / lambda;
//				us_tecflags |= HASGRAB;
//			}
//	
//			// free all examples
//			us_tecedfreeexamples(nelist);
//	
//			// advance the fill pointer
//			nodeindex++;
//		}
//	
//		// store width offset on the technology
//		(void)setval((INTBIG)tech, VTECHNOLOGY, x_("TECH_node_width_offset"), (INTBIG)us_tecnode_widoff,
//			VFRACT|VDONTSAVE|VISARRAY|((us_tecnode_count*4)<<VLENGTHSH));
//		efree((CHAR *)sequence);
//		return(FALSE);
//	}
//	
//	/*
//	 * routine to find the closest port to the layer describe by "lx<=X<=hx" and
//	 * "ly<+Y<=hy" in the list "nelist".  The ports are listed in "tlist".  The algorithm
//	 * is to find a port that overlaps this layer.  If there is only one, or if all of
//	 * them electrically connect, use that.  If there are no such ports, or multiple
//	 * unconnected ports, presume that the layer is not related to any port.
//	 */
//	INTBIG us_tecedfindport(TECH_NODES *tlist, EXAMPLE *nelist, INTBIG lx, INTBIG hx, INTBIG ly,
//		INTBIG hy, INTBIG lambda)
//	{
//		REGISTER INTBIG bestport, l, oldnet, newnet;
//		INTBIG portlx, porthx, portly, porthy;
//		REGISTER INTBIG swap;
//	
//		bestport = -1;
//		for(l=0; l<tlist.portcount; l++)
//		{
//			subrange(nelist.lx, nelist.hx, tlist.portlist[l].lowxmul,
//				tlist.portlist[l].lowxsum, tlist.portlist[l].highxmul,
//					tlist.portlist[l].highxsum, &portlx, &porthx, lambda);
//			if (portlx > porthx)
//			{
//				swap = portlx;   portlx = porthx;   porthx = swap;
//			}
//			subrange(nelist.ly, nelist.hy, tlist.portlist[l].lowymul,
//				tlist.portlist[l].lowysum, tlist.portlist[l].highymul,
//					tlist.portlist[l].highysum, &portly, &porthy, lambda);
//			if (portlx > porthx)
//			{
//				swap = portly;   portly = porthy;   porthy = swap;
//			}
//	
//			// ignore the port if there is no intersection
//			if (lx > porthx || hx < portlx || ly > porthy || hy < portly) continue;
//	
//			// if there is no previous overlapping port, use this
//			if (bestport == -1)
//			{
//				bestport = l;
//				continue;
//			}
//	
//			// if these two ports connect, all is well
//			newnet = (tlist.portlist[l].initialbits & PORTNET) >> PORTNETSH;
//			oldnet = (tlist.portlist[bestport].initialbits & PORTNET) >> PORTNETSH;
//			if (newnet == oldnet) continue;
//	
//			// two unconnected ports intersect layer: make it free
//			return(-1);
//		}
//		return(bestport);
//	}
//	
//	/*
//	 * Routine to free the examples created by "us_tecedgetexamples()".
//	 */
//	void us_tecedfreeexamples(EXAMPLE *nelist)
//	{
//		REGISTER EXAMPLE *ne;
//		REGISTER SAMPLE *ns;
//	
//		while (nelist != NOEXAMPLE)
//		{
//			ne = nelist;
//			nelist = nelist.nextexample;
//			while (ne.firstsample != NOSAMPLE)
//			{
//				ns = ne.firstsample;
//				ne.firstsample = ne.firstsample.nextsample;
//				efree((CHAR *)ns);
//			}
//			efree((CHAR *)ne);
//		}
//	}
//	
//	/*
//	 * routine to parse the node examples in cell "np" and return a list of
//	 * EXAMPLEs (one per example).  "isnode" is true if this is a node
//	 * being examined.  Returns NOEXAMPLE on error.
//	 */
//	EXAMPLE *us_tecedgetexamples(NODEPROTO *np, BOOLEAN isnode)
//	{
//		REGISTER SAMPLE *ns;
//		REGISTER EXAMPLE *ne, *nelist, *bestne;
//		REGISTER NODEINST *ni, *otherni;
//		REGISTER INTBIG sea, sizex, sizey, newsize, locx, locy, lambda, hcount, funct;
//		REGISTER BOOLEAN foundone, gotbbox;
//		INTBIG lx, hx, ly, hy, sflx, sfhx, sfly, sfhy;
//		REGISTER GEOM *geom;
//		XARRAY trans;
//		static POLYGON *poly = NOPOLYGON;
//	
//		for(ni = np.firstnodeinst; ni != NONODEINST; ni = ni.nextnodeinst)
//		{
//			ni.temp1 = (INTBIG)NOEXAMPLE;
//	
//			// ignore special nodes with function information
//			funct = us_tecedgetoption(ni);
//			if (funct != LAYERPATCH && funct != PORTOBJ && funct != HIGHLIGHTOBJ) ni.temp1 = 0;
//		}
//	
//		nelist = NOEXAMPLE;
//		for(ni = np.firstnodeinst; ni != NONODEINST; ni = ni.nextnodeinst)
//		{
//			if (ni.temp1 != (INTBIG)NOEXAMPLE) continue;
//	
//			// get a new cluster of nodes
//			ne = (EXAMPLE *)emalloc((sizeof (EXAMPLE)), us_tool.cluster);
//			if (ne == 0) return(NOEXAMPLE);
//			ne.firstsample = NOSAMPLE;
//			gotbbox = FALSE;
//			(void)needstaticpolygon(&poly, 4, us_tool.cluster);
//			nodesizeoffset(ni, &lx, &ly, &hx, &hy);
//			maketruerectpoly(ni.lowx+lx, ni.highx-hx, ni.lowy+ly, ni.highy-hy, poly);
//			makerot(ni, trans);
//			xformpoly(poly, trans);
//			getbbox(poly, &sflx, &sfhx, &sfly, &sfhy);
//			ne.nextexample = nelist;
//			nelist = ne;
//	
//			// now find all others that touch this area
//			foundone = TRUE;
//			hcount = 0;
//			while (foundone)
//			{
//				foundone = FALSE;
//	
//				// begin to search the area so far
//				sea = initsearch(sflx, sfhx, sfly, sfhy, np);
//				if (sea == -1) return(NOEXAMPLE);
//				for(;;)
//				{
//					// get next node in the area
//					geom = nextobject(sea);
//					if (geom == NOGEOM) break;
//					if (!geom.entryisnode) continue;
//					otherni = geom.entryaddr.ni;
//					(void)needstaticpolygon(&poly, 4, us_tool.cluster);
//					nodesizeoffset(otherni, &lx, &ly, &hx, &hy);
//					maketruerectpoly(otherni.lowx+lx, otherni.highx-hx, otherni.lowy+ly, otherni.highy-hy, poly);
//					makerot(otherni, trans);
//					xformpoly(poly, trans);
//					getbbox(poly, &lx, &hx, &ly, &hy);
//					if (hx < sflx || lx > sfhx || hy < sfly || ly > sfhy) continue;
//	
//					// make sure the node is valid
//					if (otherni.temp1 != (INTBIG)NOEXAMPLE)
//					{
//						if (otherni.temp1 == 0) continue;
//						if (otherni.temp1 == (INTBIG)ne) continue;
//						us_tecedpointout(otherni, np);
//						ttyputerr(_("Examples are too close in %s"), describenodeproto(np));
//						termsearch(sea);
//						return(NOEXAMPLE);
//					}
//					otherni.temp1 = (INTBIG)ne;
//	
//					// add it to the cluster
//					ns = (SAMPLE *)emalloc((sizeof (SAMPLE)), us_tool.cluster);
//					if (ns == 0) return(NOEXAMPLE);
//					ns.node = otherni;
//					ns.rule = NORULE;
//					ns.parent = ne;
//					ns.nextsample = ne.firstsample;
//					ne.firstsample = ns;
//					ns.assoc = NOSAMPLE;
//					ns.xpos = (lx + hx) / 2;
//					ns.ypos = (ly + hy) / 2;
//					if (otherni.proto == gen_portprim)
//					{
//						if (!isnode)
//						{
//							us_tecedpointout(otherni, np);
//							ttyputerr(_("%s cannot have ports.  Delete this"), describenodeproto(np));
//							termsearch(sea);
//							return(NOEXAMPLE);
//						}
//						ns.layer = gen_portprim;
//					} else if (otherni.proto == gen_cellcenterprim)
//					{
//						if (!isnode)
//						{
//							us_tecedpointout(otherni, np);
//							ttyputerr(_("%s cannot have a grab point.  Delete this"), describenodeproto(np));
//							termsearch(sea);
//							return(NOEXAMPLE);
//						}
//						ns.layer = gen_cellcenterprim;
//					} else
//					{
//						ns.layer = us_tecedgetlayer(otherni);
//						if (ns.layer == 0)
//						{
//							us_tecedpointout(otherni, np);
//							ttyputerr(_("No layer information on this sample in %s"),
//								describenodeproto(np));
//							if ((us_tool.toolstate&NODETAILS) == 0)
//								ttyputmsg(_("Use 'change' option or delete it"));
//							termsearch(sea);
//							return(NOEXAMPLE);
//						}
//						if (ns.layer == NONODEPROTO) hcount++;
//					}
//	
//					// accumulate state if this is not a "grab point" mark
//					if (otherni.proto != gen_cellcenterprim)
//					{
//						if (!gotbbox)
//						{
//							ne.lx = lx;   ne.hx = hx;
//							ne.ly = ly;   ne.hy = hy;
//							gotbbox = TRUE;
//						} else
//						{
//							if (lx < ne.lx) ne.lx = lx;
//							if (hx > ne.hx) ne.hx = hx;
//							if (ly < ne.ly) ne.ly = ly;
//							if (hy > ne.hy) ne.hy = hy;
//						}
//						sflx = ne.lx;   sfhx = ne.hx;
//						sfly = ne.ly;   sfhy = ne.hy;
//					}
//					foundone = TRUE;
//				}
//			}
//			if (hcount == 0)
//			{
//				us_tecedpointout(NONODEINST, np);
//				ttyputerr(_("No highlight layer in %s example"), describenodeproto(np));
//				if ((us_tool.toolstate&NODETAILS) == 0)
//					ttyputmsg(_("Use 'place-layer' option to create HIGHLIGHT"));
//				return(NOEXAMPLE);
//			}
//			if (hcount != 1)
//			{
//				us_tecedpointout(NONODEINST, np);
//				ttyputerr(_("Too many highlight layers in %s example.  Delete some"), describenodeproto(np));
//				return(NOEXAMPLE);
//			}
//		}
//		if (nelist == NOEXAMPLE)
//		{
//			us_tecedpointout(NONODEINST, np);
//			ttyputerr(_("No examples found in %s"), describenodeproto(np));
//			if ((us_tool.toolstate&NODETAILS) == 0)
//				ttyputmsg(_("Use 'place-layer' option to produce some geometry"));
//			return(nelist);
//		}
//	
//		/*
//		 * now search the list for the smallest, most upper-right example
//		 * (the "main" example)
//		 */
//		lambda = el_curlib.lambda[art_tech.techindex];
//		sizex = (nelist.hx - nelist.lx) / lambda;
//		sizey = (nelist.hy - nelist.ly) / lambda;
//		locx = (nelist.lx + nelist.hx) / 2;
//		locy = (nelist.ly + nelist.hy) / 2;
//		bestne = nelist;
//		for(ne = nelist; ne != NOEXAMPLE; ne = ne.nextexample)
//		{
//			newsize = (ne.hx-ne.lx) / lambda;
//			newsize *= (ne.hy-ne.ly) / lambda;
//			if (newsize > sizex*sizey) continue;
//			if (newsize == sizex*sizey && (ne.lx+ne.hx)/2 >= locx && (ne.ly+ne.hy)/2 <= locy)
//				continue;
//			sizex = (ne.hx - ne.lx) / lambda;
//			sizey = (ne.hy - ne.ly) / lambda;
//			locx = (ne.lx + ne.hx) / 2;
//			locy = (ne.ly + ne.hy) / 2;
//			bestne = ne;
//		}
//	
//		// place the main example at the top of the list
//		if (bestne != nelist)
//		{
//			for(ne = nelist; ne != NOEXAMPLE; ne = ne.nextexample)
//				if (ne.nextexample == bestne)
//			{
//				ne.nextexample = bestne.nextexample;
//				break;
//			}
//			bestne.nextexample = nelist;
//			nelist = bestne;
//		}
//	
//		// done
//		return(nelist);
//	}
//	
//	/*
//	 * Routine to associate the samples of example "nelist" in cell "np"
//	 * Returns true if there is an error
//	 */
//	BOOLEAN us_tecedassociateexamples(EXAMPLE *nelist, NODEPROTO *np)
//	{
//		REGISTER EXAMPLE *ne;
//		REGISTER SAMPLE *ns, *nslist, *nsfound, **listsort, **thissort;
//		REGISTER INTBIG total, i;
//		REGISTER CHAR *name, *othername;
//	
//		// if there is only one example, no association
//		if (nelist.nextexample == NOEXAMPLE) return(FALSE);
//	
//		// associate each example "ne" with the original in "nelist"
//		for(ne = nelist.nextexample; ne != NOEXAMPLE; ne = ne.nextexample)
//		{
//			// clear associations for every sample "ns" in the example "ne"
//			for(ns = ne.firstsample; ns != NOSAMPLE; ns = ns.nextsample)
//				ns.assoc = NOSAMPLE;
//	
//			// associate every sample "ns" in the example "ne"
//			for(ns = ne.firstsample; ns != NOSAMPLE; ns = ns.nextsample)
//			{
//				if (ns.assoc != NOSAMPLE) continue;
//	
//				// cannot have center in other examples
//				if (ns.layer == gen_cellcenterprim)
//				{
//					us_tecedpointout(ns.node, ns.node.parent);
//					ttyputerr(_("Grab point should only be in main example of %s"),
//						describenodeproto(np));
//					return(TRUE);
//				}
//	
//				// count number of similar layers in original example "nelist"
//				for(total = 0, nslist = nelist.firstsample; nslist != NOSAMPLE;
//					nslist = nslist.nextsample)
//				{
//					if (nslist.layer != ns.layer) continue;
//					total++;
//					nsfound = nslist;
//				}
//	
//				// no similar layer found in the original: error
//				if (total == 0)
//				{
//					us_tecedpointout(ns.node, ns.node.parent);
//					ttyputerr(_("Layer %s not found in main example of %s"),
//						us_tecedsamplename(ns.layer), describenodeproto(np));
//					return(TRUE);
//				}
//	
//				// just one in the original: simple association
//				if (total == 1)
//				{
//					ns.assoc = nsfound;
//					continue;
//				}
//	
//				// if it is a port, associate by port name
//				if (ns.layer == gen_portprim)
//				{
//					name = us_tecedgetportname(ns.node);
//					if (name == 0)
//					{
//						us_tecedpointout(ns.node, ns.node.parent);
//						ttyputerr(_("Cell %s: port does not have a name"), describenodeproto(np));
//						return(TRUE);
//					}
//	
//					// search the original for that port
//					for(nslist = nelist.firstsample; nslist != NOSAMPLE; nslist = nslist.nextsample)
//						if (nslist.layer == gen_portprim)
//					{
//						othername = us_tecedgetportname(nslist.node);
//						if (othername == 0)
//						{
//							us_tecedpointout(nslist.node, nslist.node.parent);
//							ttyputerr(_("Cell %s: port does not have a name"), describenodeproto(np));
//							return(TRUE);
//						}
//						if (namesame(name, othername) != 0) continue;
//						ns.assoc = nslist;
//						break;
//					}
//					if (nslist == NOSAMPLE)
//					{
//						us_tecedpointout(NONODEINST, np);
//						ttyputerr(_("Could not find port %s in all examples of %s"),
//							name, describenodeproto(np));
//						return(TRUE);
//					}
//					continue;
//				}
//	
//				// count the number of this layer in example "ne"
//				for(i = 0, nslist = ne.firstsample; nslist != NOSAMPLE;
//					nslist = nslist.nextsample)
//						if (nslist.layer == ns.layer) i++;
//	
//				// if number of similar layers differs: error
//				if (total != i)
//				{
//					us_tecedpointout(ns.node, ns.node.parent);
//					ttyputerr(_("Layer %s found %ld times in main example, %ld in other"),
//						us_tecedsamplename(ns.layer), total, i);
//					ttyputmsg(_("Make the counts consistent"));
//					return(TRUE);
//				}
//	
//				// make a list of samples on this layer in original
//				listsort = (SAMPLE **)emalloc((total * (sizeof (SAMPLE *))), el_tempcluster);
//				if (listsort == 0) return(TRUE);
//				for(i = 0, nslist = nelist.firstsample; nslist != NOSAMPLE;
//					nslist = nslist.nextsample)
//						if (nslist.layer == ns.layer) listsort[i++] = nslist;
//	
//				// make a list of samples on this layer in example "ne"
//				thissort = (SAMPLE **)emalloc((total * (sizeof (SAMPLE *))), el_tempcluster);
//				if (thissort == 0) return(TRUE);
//				for(i = 0, nslist = ne.firstsample; nslist != NOSAMPLE; nslist = nslist.nextsample)
//					if (nslist.layer == ns.layer) thissort[i++] = nslist;
//	
//				// sort each list in X/Y/shape
//				esort(listsort, total, sizeof (SAMPLE *), us_samplecoordascending);
//				esort(thissort, total, sizeof (SAMPLE *), us_samplecoordascending);
//	
//				// see if the lists have duplication
//				for(i=1; i<total; i++)
//					if ((thissort[i].xpos == thissort[i-1].xpos &&
//						thissort[i].ypos == thissort[i-1].ypos &&
//							thissort[i].node.proto == thissort[i-1].node.proto) ||
//						(listsort[i].xpos == listsort[i-1].xpos &&
//							listsort[i].ypos == listsort[i-1].ypos &&
//								listsort[i].node.proto == listsort[i-1].node.proto)) break;
//				if (i >= total)
//				{
//					// association can be made in X
//					for(i=0; i<total; i++) thissort[i].assoc = listsort[i];
//					efree((CHAR *)thissort);
//					efree((CHAR *)listsort);
//					continue;
//				}
//	
//				// don't know how to associate this sample
//				us_tecedpointout(thissort[i].node, thissort[i].node.parent);
//				ttyputerr(_("Sample %s is unassociated in %s"),
//					us_tecedsamplename(thissort[i].layer), describenodeproto(np));
//				efree((CHAR *)thissort);
//				efree((CHAR *)listsort);
//				return(TRUE);
//			}
//	
//			// final check: make sure every sample in original example associates
//			for(nslist = nelist.firstsample; nslist != NOSAMPLE;
//				nslist = nslist.nextsample) nslist.assoc = NOSAMPLE;
//			for(ns = ne.firstsample; ns != NOSAMPLE; ns = ns.nextsample)
//				ns.assoc.assoc = ns;
//			for(nslist = nelist.firstsample; nslist != NOSAMPLE;
//				nslist = nslist.nextsample) if (nslist.assoc == NOSAMPLE)
//			{
//				if (nslist.layer == gen_cellcenterprim) continue;
//				us_tecedpointout(nslist.node, nslist.node.parent);
//				ttyputerr(_("Layer %s found in main example, but not others in %s"),
//					us_tecedsamplename(nslist.layer), describenodeproto(np));
//				return(TRUE);
//			}
//		}
//		return(FALSE);
//	}
//	
//	/*
//	 * Helper routine to "xx()" for sorting samples by coordinate value.
//	 */
//	int us_samplecoordascending(const void *e1, const void *e2)
//	{
//		SAMPLE *s1, *s2;
//	
//		s1 = *((SAMPLE **)e1);
//		s2 = *((SAMPLE **)e2);
//		if (s1.xpos != s2.xpos) return(s1.xpos - s2.xpos);
//		if (s1.ypos != s2.ypos) return(s1.ypos - s2.ypos);
//		return(s1.node.proto - s2.node.proto);
//	}
//	
//	/* flags about the edge positions in the examples */
//	#define TOEDGELEFT       01		/* constant to left edge */
//	#define TOEDGERIGHT      02		/* constant to right edge */
//	#define TOEDGETOP        04		/* constant to top edge */
//	#define TOEDGEBOT       010		/* constant to bottom edge */
//	#define FROMCENTX       020		/* constant in X to center */
//	#define FROMCENTY       040		/* constant in Y to center */
//	#define RATIOCENTX     0100		/* fixed ratio from X center to edge */
//	#define RATIOCENTY     0200		/* fixed ratio from Y center to edge */
//	
//	BOOLEAN us_tecedmakeprim(EXAMPLE *nelist, NODEPROTO *np, TECHNOLOGY *tech, TECH_NODES *tlist, INTBIG lambda)
//	{
//		REGISTER SAMPLE *ns, *nso, *hs;
//		REGISTER EXAMPLE *ne;
//		REGISTER INTBIG total, count, newcount, i, truecount, multixs, multiys, multiindent, multisep;
//		REGISTER INTBIG *newrule, r, dist;
//		XARRAY trans;
//		REGISTER CHAR *str;
//		REGISTER VARIABLE *var, *var2, *var3;
//		REGISTER NODEINST *ni;
//	
//		// look at every sample "ns" in the main example "nelist"
//		for(ns = nelist.firstsample; ns != NOSAMPLE; ns = ns.nextsample)
//		{
//			// ignore grab point specification
//			if (ns.layer == gen_cellcenterprim) continue;
//	
//			// if there is only one example: make sample scale with edge
//			if (nelist.nextexample == NOEXAMPLE)
//			{
//				// if a multicut separation was given and this is a cut, add the rule
//				if (tlist.f4 > 0 && ns.layer != NONODEPROTO && ns.layer != gen_portprim)
//				{
//					for(i=0; i<us_teclayer_count; i++)
//						if (namesame(us_teclayer_names[i], &ns.layer.protoname[6]) == 0) break;
//					if (i < us_teclayer_count)
//					{
//						if (layeriscontact(us_teclayer_function[i]))
//						{
//							hs = us_tecedneedhighlightlayer(nelist, np);
//							if (hs == 0) return(TRUE);
//							multixs = ns.node.highx - ns.node.lowx;
//							multiys = ns.node.highy - ns.node.lowy;
//							multiindent = ns.node.lowx - hs.node.lowx;
//							multisep = muldiv(tlist.f4, lambda, WHOLE);
//							ns.rule = us_tecedaddmulticutrule(multixs, multiys, multiindent, multisep);
//							if (ns.rule == 0) return(TRUE);
//							continue;
//						}
//					}
//				}
//	
//				// see if there is polygonal information
//				if (ns.node.proto == art_filledpolygonprim ||
//					ns.node.proto == art_closedpolygonprim ||
//					ns.node.proto == art_openedpolygonprim ||
//					ns.node.proto == art_openeddottedpolygonprim ||
//					ns.node.proto == art_openeddashedpolygonprim ||
//					ns.node.proto == art_openedthickerpolygonprim)
//				{
//					var = gettrace(ns.node);
//				} else var = NOVARIABLE;
//				if (var != NOVARIABLE)
//				{
//					// make sure the arrays hold "count" points
//					count = getlength(var) / 2;
//					us_tecedforcearrays(count);
//	
//					// fill the array
//					makerot(ns.node, trans);
//					for(i=0; i<count; i++)
//					{
//						xform((ns.node.geom.lowx + ns.node.geom.highx)/2 +
//							((INTBIG *)var.addr)[i*2],
//								(ns.node.geom.lowy + ns.node.geom.highy)/2 +
//									((INTBIG *)var.addr)[i*2+1], &us_tecedmakepx[i], &us_tecedmakepy[i], trans);
//						us_tecedmakefactor[i] = FROMCENTX|FROMCENTY;
//					}
//				} else
//				{
//					// see if it is an arc of a circle
//					count = 2;
//					if (ns.node.proto == art_circleprim || ns.node.proto == art_thickcircleprim)
//					{
//						var = getvalkey((INTBIG)ns.node, VNODEINST, VINTEGER, art_degreeskey);
//						if (var != NOVARIABLE) count = 3;
//					} else var = NOVARIABLE;
//	
//					// make sure the arrays hold enough points
//					us_tecedforcearrays(count);
//	
//					// set sample description
//					if (var != NOVARIABLE)
//					{
//						// handle circular arc sample
//						us_tecedmakepx[0] = (ns.node.geom.lowx + ns.node.geom.highx) / 2;
//						us_tecedmakepy[0] = (ns.node.geom.lowy + ns.node.geom.highy) / 2;
//						makerot(ns.node, trans);
//						dist = ns.node.geom.highx - us_tecedmakepx[0];
//						xform(us_tecedmakepx[0] + mult(dist, cosine(var.addr)),
//							us_tecedmakepy[0] + mult(dist, sine(var.addr)), &us_tecedmakepx[1], &us_tecedmakepy[1], trans);
//						xform(ns.node.geom.highx,
//							(ns.node.geom.lowy + ns.node.geom.highy) / 2, &us_tecedmakepx[2], &us_tecedmakepy[2], trans);
//						us_tecedmakefactor[0] = FROMCENTX|FROMCENTY;
//						us_tecedmakefactor[1] = RATIOCENTX|RATIOCENTY;
//						us_tecedmakefactor[2] = RATIOCENTX|RATIOCENTY;
//					} else if (ns.node.proto == art_circleprim || ns.node.proto == art_thickcircleprim ||
//						ns.node.proto == art_filledcircleprim)
//					{
//						// handle circular sample
//						us_tecedmakepx[0] = (ns.node.geom.lowx + ns.node.geom.highx) / 2;
//						us_tecedmakepy[0] = (ns.node.geom.lowy + ns.node.geom.highy) / 2;
//						us_tecedmakepx[1] = ns.node.geom.highx;
//						us_tecedmakepy[1] = (ns.node.geom.lowy + ns.node.geom.highy) / 2;
//						us_tecedmakefactor[0] = FROMCENTX|FROMCENTY;
//						us_tecedmakefactor[1] = TOEDGERIGHT|FROMCENTY;
//					} else
//					{
//						// rectangular sample: get the bounding box in (px, py)
//						us_tecedgetbbox(ns.node, &us_tecedmakepx[0], &us_tecedmakepx[1], &us_tecedmakepy[0], &us_tecedmakepy[1]);
//	
//						// preset stretch factors to go to the edges of the box
//						us_tecedmakefactor[0] = TOEDGELEFT|TOEDGEBOT;
//						us_tecedmakefactor[1] = TOEDGERIGHT|TOEDGETOP;
//					}
//				}
//	
//				// add the rule to the collection
//				newrule = us_tecedstretchpoints(us_tecedmakepx,us_tecedmakepy, count, us_tecedmakefactor, ns, np,
//					nelist);
//				if (newrule == 0) return(TRUE);
//				var = getvalkey((INTBIG)ns.node, VNODEINST, VSTRING|VISARRAY, art_messagekey);
//				if (var == NOVARIABLE) str = (CHAR *)0; else
//					str = ((CHAR **)var.addr)[0];
//				ns.rule = us_tecedaddrule(newrule, count*4, FALSE, str);
//				if (ns.rule == NORULE) return(TRUE);
//				efree((CHAR *)newrule);
//				continue;
//			}
//	
//			// look at other examples and find samples associated with this
//			nelist.studysample = ns;
//			for(ne = nelist.nextexample; ne != NOEXAMPLE; ne = ne.nextexample)
//			{
//				// count number of samples associated with the main sample
//				total = 0;
//				for(nso = ne.firstsample; nso != NOSAMPLE; nso = nso.nextsample)
//					if (nso.assoc == ns)
//				{
//					ne.studysample = nso;
//					total++;
//				}
//				if (total == 0)
//				{
//					us_tecedpointout(ns.node, ns.node.parent);
//					ttyputerr(_("Still unassociated sample in %s (shouldn't happen)"),
//						describenodeproto(np));
//					return(TRUE);
//				}
//	
//				// if there are multiple associations, it must be a contact cut
//				if (total > 1)
//				{
//					// make sure the layer is real geometry, not highlight or a port
//					if (ns.layer == NONODEPROTO || ns.layer == gen_portprim)
//					{
//						us_tecedpointout(ns.node, ns.node.parent);
//						ttyputerr(_("Only contact layers may be iterated in examples of %s"),
//							describenodeproto(np));
//						return(TRUE);
//					}
//	
//					// make sure the contact cut layer is opaque
//					for(i=0; i<tech.layercount; i++)
//						if (namesame(&ns.layer.protoname[6], us_teclayer_names[i]) == 0)
//					{
//						if (tech.layers[i].bits != LAYERO)
//						{
//							us_tecedpointout(ns.node, ns.node.parent);
//							ttyputerr(_("Multiple contact layers must not be transparent in %s"),
//								describenodeproto(np));
//							return(TRUE);
//						}
//						break;
//					}
//	
//					// add the rule
//					if (us_tecedmulticut(ns, nelist, np)) return(TRUE);
//					break;
//				}
//			}
//			if (ne != NOEXAMPLE) continue;
//	
//			// associations done for this sample, now analyze them
//			if (ns.node.proto == art_filledpolygonprim ||
//				ns.node.proto == art_closedpolygonprim ||
//				ns.node.proto == art_openedpolygonprim ||
//				ns.node.proto == art_openeddottedpolygonprim ||
//				ns.node.proto == art_openeddashedpolygonprim ||
//				ns.node.proto == art_openedthickerpolygonprim)
//			{
//				var = gettrace(ns.node);
//			} else var = NOVARIABLE;
//			if (var != NOVARIABLE)
//			{
//				truecount = count = getlength(var) / 2;
//	
//				// make sure the arrays hold "count" points
//				us_tecedforcearrays(count);
//				makerot(ns.node, trans);
//				for(i=0; i<count; i++)
//					xform((ns.node.geom.lowx + ns.node.geom.highx)/2 + ((INTBIG *)var.addr)[i*2],
//						(ns.node.geom.lowy + ns.node.geom.highy)/2 +
//							((INTBIG *)var.addr)[i*2+1], &us_tecedmakepx[i], &us_tecedmakepy[i], trans);
//			} else
//			{
//				// make sure the arrays hold enough points
//				count = 2;
//				if (ns.node.proto == art_circleprim || ns.node.proto == art_thickcircleprim)
//				{
//					var3 = getvalkey((INTBIG)ns.node, VNODEINST, VINTEGER, art_degreeskey);
//					if (var3 != NOVARIABLE) count = 3;
//				} else var3 = NOVARIABLE;
//				truecount = count;
//				if (var3 == NOVARIABLE)
//				{
//					Variable var2 = ns.node.getVar(Generate.MINSIZEBOX_KEY);
//					if (var2 != null) count *= 2;
//				}
//				us_tecedforcearrays(count);
//	
//				// set sample description
//				if (var3 != NOVARIABLE)
//				{
//					// handle circular arc sample
//					us_tecedmakepx[0] = (ns.node.geom.lowx + ns.node.geom.highx) / 2;
//					us_tecedmakepy[0] = (ns.node.geom.lowy + ns.node.geom.highy) / 2;
//					makerot(ns.node, trans);
//					dist = ns.node.geom.highx - us_tecedmakepx[0];
//					xform(us_tecedmakepx[0] + mult(dist, cosine(var3.addr)),
//						us_tecedmakepy[0] + mult(dist, sine(var3.addr)), &us_tecedmakepx[1], &us_tecedmakepy[1], trans);
//					xform(ns.node.geom.highx,
//						(ns.node.geom.lowy + ns.node.geom.highy) / 2, &us_tecedmakepx[2], &us_tecedmakepy[2], trans);
//				} else if (ns.node.proto == art_circleprim || ns.node.proto == art_thickcircleprim ||
//					ns.node.proto == art_filledcircleprim)
//				{
//					// handle circular sample
//					us_tecedmakepx[0] = (ns.node.geom.lowx + ns.node.geom.highx) / 2;
//					us_tecedmakepy[0] = (ns.node.geom.lowy + ns.node.geom.highy) / 2;
//					us_tecedmakepx[1] = ns.node.geom.highx;
//					us_tecedmakepy[1] = (ns.node.geom.lowy + ns.node.geom.highy) / 2;
//				} else
//				{
//					// rectangular sample: get the bounding box in (us_tecedmakepx, us_tecedmakepy)
//					us_tecedgetbbox(ns.node, &us_tecedmakepx[0], &us_tecedmakepx[1], &us_tecedmakepy[0], &us_tecedmakepy[1]);
//				}
//				if (var2 != NOVARIABLE)
//				{
//					us_tecedmakepx[2] = us_tecedmakepx[0];   us_tecedmakepy[2] = us_tecedmakepy[0];
//					us_tecedmakepx[3] = us_tecedmakepx[1];   us_tecedmakepy[3] = us_tecedmakepy[1];
//				}
//			}
//	
//			for(i=0; i<count; i++)
//			{
//				us_tecedmakeleftdist[i] = us_tecedmakepx[i] - nelist.lx;
//				us_tecedmakerightdist[i] = nelist.hx - us_tecedmakepx[i];
//				us_tecedmakebotdist[i] = us_tecedmakepy[i] - nelist.ly;
//				us_tecedmaketopdist[i] = nelist.hy - us_tecedmakepy[i];
//				us_tecedmakecentxdist[i] = us_tecedmakepx[i] - (nelist.lx+nelist.hx)/2;
//				us_tecedmakecentydist[i] = us_tecedmakepy[i] - (nelist.ly+nelist.hy)/2;
//				if (nelist.hx == nelist.lx) us_tecedmakeratiox[i] = 0; else
//					us_tecedmakeratiox[i] = (us_tecedmakepx[i] - (nelist.lx+nelist.hx)/2) * WHOLE / (nelist.hx-nelist.lx);
//				if (nelist.hy == nelist.ly) us_tecedmakeratioy[i] = 0; else
//					us_tecedmakeratioy[i] = (us_tecedmakepy[i] - (nelist.ly+nelist.hy)/2) * WHOLE / (nelist.hy-nelist.ly);
//				if (i < truecount)
//					us_tecedmakefactor[i] = TOEDGELEFT | TOEDGERIGHT | TOEDGETOP | TOEDGEBOT | FROMCENTX |
//						FROMCENTY | RATIOCENTX | RATIOCENTY; else
//							us_tecedmakefactor[i] = FROMCENTX | FROMCENTY;
//			}
//			for(ne = nelist.nextexample; ne != NOEXAMPLE; ne = ne.nextexample)
//			{
//				ni = ne.studysample.node;
//				if (ni.proto == art_filledpolygonprim ||
//					ni.proto == art_closedpolygonprim ||
//					ni.proto == art_openedpolygonprim ||
//					ni.proto == art_openeddottedpolygonprim ||
//					ni.proto == art_openeddashedpolygonprim ||
//					ni.proto == art_openedthickerpolygonprim)
//				{
//					var = gettrace(ni);
//				} else var = NOVARIABLE;
//				if (var != NOVARIABLE)
//				{
//					newcount = getlength(var) / 2;
//					makerot(ni, trans);
//					for(i=0; i<mini(truecount, newcount); i++)
//						xform((ni.geom.lowx + ni.geom.highx)/2 + ((INTBIG *)var.addr)[i*2],
//							(ni.geom.lowy + ni.geom.highy)/2 +
//								((INTBIG *)var.addr)[i*2+1], &us_tecedmakecx[i], &us_tecedmakecy[i], trans);
//				} else
//				{
//					newcount = 2;
//					if (ni.proto == art_circleprim || ni.proto == art_thickcircleprim)
//					{
//						var3 = getvalkey((INTBIG)ni, VNODEINST, VINTEGER, art_degreeskey);
//						if (var3 != NOVARIABLE) newcount = 3;
//					} else var3 = NOVARIABLE;
//					if (var3 != NOVARIABLE)
//					{
//						us_tecedmakecx[0] = (ni.geom.lowx + ni.geom.highx) / 2;
//						us_tecedmakecy[0] = (ni.geom.lowy + ni.geom.highy) / 2;
//						makerot(ni, trans);
//						dist = ni.geom.highx - us_tecedmakecx[0];
//						xform(us_tecedmakecx[0] + mult(dist, cosine(var3.addr)),
//							us_tecedmakecy[0] + mult(dist, sine(var3.addr)), &us_tecedmakecx[1], &us_tecedmakecy[1], trans);
//						xform(ni.geom.highx, (ni.geom.lowy + ni.geom.highy) / 2,
//							&us_tecedmakecx[2], &us_tecedmakecy[2], trans);
//					} else if (ni.proto == art_circleprim || ni.proto == art_thickcircleprim ||
//						ni.proto == art_filledcircleprim)
//					{
//						us_tecedmakecx[0] = (ni.geom.lowx + ni.geom.highx) / 2;
//						us_tecedmakecy[0] = (ni.geom.lowy + ni.geom.highy) / 2;
//						us_tecedmakecx[1] = ni.geom.highx;
//						us_tecedmakecy[1] = (ni.geom.lowy + ni.geom.highy) / 2;
//					} else
//					{
//						us_tecedgetbbox(ni, &us_tecedmakecx[0], &us_tecedmakecx[1], &us_tecedmakecy[0], &us_tecedmakecy[1]);
//					}
//				}
//				if (newcount != truecount)
//				{
//					us_tecedpointout(ni, ni.parent);
//					ttyputerr(_("Main example of %s has %ld points but this has %ld in %s"),
//						us_tecedsamplename(ne.studysample.layer),
//							truecount, newcount, describenodeproto(np));
//					return(TRUE);
//				}
//	
//				for(i=0; i<truecount; i++)
//				{
//					// see if edges are fixed distance from example edge
//					if (us_tecedmakeleftdist[i] != us_tecedmakecx[i] - ne.lx) us_tecedmakefactor[i] &= ~TOEDGELEFT;
//					if (us_tecedmakerightdist[i] != ne.hx - us_tecedmakecx[i]) us_tecedmakefactor[i] &= ~TOEDGERIGHT;
//					if (us_tecedmakebotdist[i] != us_tecedmakecy[i] - ne.ly) us_tecedmakefactor[i] &= ~TOEDGEBOT;
//					if (us_tecedmaketopdist[i] != ne.hy - us_tecedmakecy[i]) us_tecedmakefactor[i] &= ~TOEDGETOP;
//	
//					// see if edges are fixed distance from example center
//					if (us_tecedmakecentxdist[i] != us_tecedmakecx[i] - (ne.lx+ne.hx)/2) us_tecedmakefactor[i] &= ~FROMCENTX;
//					if (us_tecedmakecentydist[i] != us_tecedmakecy[i] - (ne.ly+ne.hy)/2) us_tecedmakefactor[i] &= ~FROMCENTY;
//	
//					// see if edges are fixed ratio from example center
//					if (ne.hx == ne.lx) r = 0; else
//						r = (us_tecedmakecx[i] - (ne.lx+ne.hx)/2) * WHOLE / (ne.hx-ne.lx);
//					if (r != us_tecedmakeratiox[i]) us_tecedmakefactor[i] &= ~RATIOCENTX;
//					if (ne.hy == ne.ly) r = 0; else
//						r = (us_tecedmakecy[i] - (ne.ly+ne.hy)/2) * WHOLE / (ne.hy-ne.ly);
//					if (r != us_tecedmakeratioy[i]) us_tecedmakefactor[i] &= ~RATIOCENTY;
//				}
//	
//				// make sure port information is on the primary example
//				if (ns.layer != gen_portprim) continue;
//	
//				// check port angle
//				var = ns.node.getVar(Generate.PORTANGLE_KEY);
//				var2 = ni.getVar(Generate.PORTANGLE_KEY);
//				if (var == null && var2 != null)
//				{
//					us_tecedpointout(NONODEINST, np);
//					ttyputerr(_("Warning: moving port angle to main example of %s"),
//						describenodeproto(np));
//					ns.node.newVar(Generate.PORTANGLE_KEY, new Integer(var2.addr));
//				}
//	
//				// check port range
//				var = ns.node.getVar(Generate.PORTRANGE_KEY);
//				var2 = ni.getVar(Generate.PORTRANGE_KEY);
//				if (var == null && var2 != null)
//				{
//					us_tecedpointout(NONODEINST, np);
//					ttyputerr(_("Warning: moving port range to main example of %s"), describenodeproto(np));
//					ns.node.newVar(Generate.PORTRANGE_KEY, new Integer(var2.addr));
//				}
//	
//				// check connectivity
//				var = ns.node.getVar(Generate.CONNECTION_KEY);
//				var2 = ni.getVar(Generate.CONNECTION_KEY);
//				if (var == null && var2 != null)
//				{
//					us_tecedpointout(NONODEINST, np);
//					ttyputerr(_("Warning: moving port connections to main example of %s"),
//						describenodeproto(np));
//					ns.node.newVar(Generate.CONNECTION_KEY, var2.addr);
//				}
//			}
//	
//			// error check for the highlight layer
//			if (ns.layer == NONODEPROTO)
//				for(i=0; i<truecount; i++)
//					if ((us_tecedmakefactor[i]&(TOEDGELEFT|TOEDGERIGHT)) == 0 ||
//						(us_tecedmakefactor[i]&(TOEDGETOP|TOEDGEBOT)) == 0)
//			{
//				us_tecedpointout(ns.node, ns.node.parent);
//				ttyputerr(_("Highlight must be constant distance from edge in %s"), describenodeproto(np));
//				return(TRUE);
//			}
//	
//			// finally, make a rule for this sample
//			newrule = us_tecedstretchpoints(us_tecedmakepx, us_tecedmakepy, count, us_tecedmakefactor, ns, np, nelist);
//			if (newrule == 0) return(TRUE);
//	
//			// add the rule to the global list
//			var = getvalkey((INTBIG)ns.node, VNODEINST, VSTRING|VISARRAY, art_messagekey);
//			if (var == NOVARIABLE) str = (CHAR *)0; else
//				str = ((CHAR **)var.addr)[0];
//			ns.rule = us_tecedaddrule(newrule, count*4, FALSE, str);
//			if (ns.rule == NORULE) return(TRUE);
//			efree((CHAR *)newrule);
//		}
//		return(FALSE);
//	}
//	
//	/*
//	 * routine to ensure that the 13 global arrays are all at least "want" long.
//	 * Their current size is "us_tecedmakearrlen".
//	 */
//	void us_tecedforcearrays(INTBIG want)
//	{
//		if (us_tecedmakearrlen >= want) return;
//		if (us_tecedmakearrlen != 0)
//		{
//			efree((CHAR *)us_tecedmakepx);
//			efree((CHAR *)us_tecedmakepy);
//			efree((CHAR *)us_tecedmakecx);
//			efree((CHAR *)us_tecedmakecy);
//			efree((CHAR *)us_tecedmakefactor);
//			efree((CHAR *)us_tecedmakeleftdist);
//			efree((CHAR *)us_tecedmakerightdist);
//			efree((CHAR *)us_tecedmakebotdist);
//			efree((CHAR *)us_tecedmaketopdist);
//			efree((CHAR *)us_tecedmakecentxdist);
//			efree((CHAR *)us_tecedmakecentydist);
//			efree((CHAR *)us_tecedmakeratiox);
//			efree((CHAR *)us_tecedmakeratioy);
//		}
//		us_tecedmakearrlen = want;
//		us_tecedmakepx = emalloc((want * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tecedmakepx == 0) return;
//		us_tecedmakepy = emalloc((want * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tecedmakepy == 0) return;
//		us_tecedmakecx = emalloc((want * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tecedmakecx == 0) return;
//		us_tecedmakecy = emalloc((want * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tecedmakecy == 0) return;
//		us_tecedmakefactor = emalloc((want * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tecedmakefactor == 0) return;
//		us_tecedmakeleftdist = emalloc((want * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tecedmakeleftdist == 0) return;
//		us_tecedmakerightdist = emalloc((want * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tecedmakerightdist == 0) return;
//		us_tecedmakebotdist = emalloc((want * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tecedmakebotdist == 0) return;
//		us_tecedmaketopdist = emalloc((want * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tecedmaketopdist == 0) return;
//		us_tecedmakecentxdist = emalloc((want * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tecedmakecentxdist == 0) return;
//		us_tecedmakecentydist = emalloc((want * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tecedmakecentydist == 0) return;
//		us_tecedmakeratiox = emalloc((want * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tecedmakeratiox == 0) return;
//		us_tecedmakeratioy = emalloc((want * SIZEOFINTBIG), us_tool.cluster);
//		if (us_tecedmakeratioy == 0) return;
//	}
//	
//	/*
//	 * routine to adjust the "count"-long array of points in "px" and "py" according
//	 * to the stretch factor bits in "factor" and return an array that describes
//	 * these points.  Returns zero on error.
//	 */
//	INTBIG *us_tecedstretchpoints(INTBIG *px, INTBIG *py, INTBIG count, INTBIG *factor,
//		SAMPLE *ns, NODEPROTO *np, EXAMPLE *nelist)
//	{
//		REGISTER INTBIG *newrule, lambda;
//		REGISTER INTBIG i;
//	
//		newrule = emalloc((count*4*SIZEOFINTBIG), el_tempcluster);
//		if (newrule == 0) return(0);
//	
//		lambda = el_curlib.lambda[art_tech.techindex];
//		for(i=0; i<count; i++)
//		{
//			// determine the X algorithm
//			if ((factor[i]&TOEDGELEFT) != 0)
//			{
//				// left edge rule
//				newrule[i*4] = -H0;
//				newrule[i*4+1] = (px[i]-nelist.lx) * WHOLE/lambda;
//			} else if ((factor[i]&TOEDGERIGHT) != 0)
//			{
//				// right edge rule
//				newrule[i*4] = H0;
//				newrule[i*4+1] = (px[i]-nelist.hx) * WHOLE/lambda;
//			} else if ((factor[i]&FROMCENTX) != 0)
//			{
//				// center rule
//				newrule[i*4] = 0;
//				newrule[i*4+1] = (px[i]-(nelist.lx+nelist.hx)/2) * WHOLE/lambda;
//			} else if ((factor[i]&RATIOCENTX) != 0)
//			{
//				// constant stretch rule
//				if (nelist.hx == nelist.lx) newrule[i*4] = 0; else
//					newrule[i*4] = (px[i] - (nelist.lx+nelist.hx)/2) * WHOLE / (nelist.hx-nelist.lx);
//				newrule[i*4+1] = 0;
//			} else
//			{
//				us_tecedpointout(ns.node, ns.node.parent);
//				ttyputerr(_("Cannot determine X stretching rule for layer %s in %s"),
//					us_tecedsamplename(ns.layer), describenodeproto(np));
//				return(0);
//			}
//	
//			// determine the Y algorithm
//			if ((factor[i]&TOEDGEBOT) != 0)
//			{
//				// bottom edge rule
//				newrule[i*4+2] = -H0;
//				newrule[i*4+3] = (py[i]-nelist.ly) * WHOLE/lambda;
//			} else if ((factor[i]&TOEDGETOP) != 0)
//			{
//				// top edge rule
//				newrule[i*4+2] = H0;
//				newrule[i*4+3] = (py[i]-nelist.hy) * WHOLE/lambda;
//			} else if ((factor[i]&FROMCENTY) != 0)
//			{
//				// center rule
//				newrule[i*4+2] = 0;
//				newrule[i*4+3] = (py[i]-(nelist.ly+nelist.hy)/2) * WHOLE/lambda;
//			} else if ((factor[i]&RATIOCENTY) != 0)
//			{
//				// constant stretch rule
//				if (nelist.hy == nelist.ly) newrule[i*4+2] = 0; else
//					newrule[i*4+2] = (py[i] - (nelist.ly+nelist.hy)/2) * WHOLE /
//						(nelist.hy-nelist.ly);
//				newrule[i*4+3] = 0;
//			} else
//			{
//				us_tecedpointout(ns.node, ns.node.parent);
//				ttyputerr(_("Cannot determine Y stretching rule for layer %s in %s"),
//					us_tecedsamplename(ns.layer), describenodeproto(np));
//				return(0);
//			}
//		}
//		return(newrule);
//	}
//	
//	SAMPLE *us_tecedneedhighlightlayer(EXAMPLE *nelist, NODEPROTO *np)
//	{
//		REGISTER SAMPLE *hs;
//	
//		// find the highlight layer
//		for(hs = nelist.firstsample; hs != NOSAMPLE; hs = hs.nextsample)
//			if (hs.layer == NONODEPROTO) return(hs);
//	
//		us_tecedpointout(NONODEINST, np);
//		ttyputerr(_("No highlight layer on contact %s"), describenodeproto(np));
//		if ((us_tool.toolstate&NODETAILS) == 0)
//			ttyputmsg(_("Use 'place-layer' option to create HIGHLIGHT"));
//		return(0);
//	}
//	
//	/*
//	 * routine to build a rule for multiple contact-cut sample "ns" from the
//	 * overall example list in "nelist".  Returns true on error.
//	 */
//	BOOLEAN us_tecedmulticut(SAMPLE *ns, EXAMPLE *nelist, NODEPROTO *np)
//	{
//		REGISTER INTBIG total, i, multixs, multiys, multiindent, multisep;
//		REGISTER INTBIG xsep, ysep, sepx, sepy;
//		REGISTER SAMPLE **nslist, *nso, *hs;
//		REGISTER EXAMPLE *ne;
//	
//		// find the highlight layer
//		hs = us_tecedneedhighlightlayer(nelist, np);
//		if (hs == 0) return(TRUE);
//	
//		// determine size of each cut
//		multixs = ns.node.highx - ns.node.lowx;
//		multiys = ns.node.highy - ns.node.lowy;
//	
//		// determine indentation of cuts
//		multiindent = ns.node.lowx - hs.node.lowx;
//		if (hs.node.highx - ns.node.highx != multiindent ||
//			ns.node.lowy - hs.node.lowy != multiindent ||
//			hs.node.highy - ns.node.highy != multiindent)
//		{
//			us_tecedpointout(ns.node, ns.node.parent);
//			ttyputerr(_("Multiple contact cuts must be indented uniformly in %s"),
//				describenodeproto(np));
//			return(TRUE);
//		}
//	
//		// look at every example after the first
//		xsep = ysep = -1;
//		for(ne = nelist.nextexample; ne != NOEXAMPLE; ne = ne.nextexample)
//		{
//			// count number of samples equivalent to the main sample
//			total = 0;
//			for(nso = ne.firstsample; nso != NOSAMPLE; nso = nso.nextsample)
//				if (nso.assoc == ns)
//			{
//				// make sure size is proper
//				if (multixs != nso.node.highx - nso.node.lowx ||
//					multiys != nso.node.highy - nso.node.lowy)
//				{
//					us_tecedpointout(nso.node, nso.node.parent);
//					ttyputerr(_("Multiple contact cuts must not differ in size in %s"),
//						describenodeproto(np));
//					return(TRUE);
//				}
//				total++;
//			}
//	
//			// allocate space for these samples
//			nslist = (SAMPLE **)emalloc((total * (sizeof (SAMPLE *))), el_tempcluster);
//			if (nslist == 0) return(TRUE);
//	
//			// fill the list of samples
//			i = 0;
//			for(nso = ne.firstsample; nso != NOSAMPLE; nso = nso.nextsample)
//				if (nso.assoc == ns) nslist[i++] = nso;
//	
//			// analyze the samples for separation
//			for(i=1; i<total; i++)
//			{
//				// find separation
//				sepx = abs((nslist[i-1].node.highx + nslist[i-1].node.lowx) / 2 -
//					(nslist[i].node.highx + nslist[i].node.lowx) / 2);
//				sepy = abs((nslist[i-1].node.highy + nslist[i-1].node.lowy) / 2 -
//					(nslist[i].node.highy + nslist[i].node.lowy) / 2);
//	
//				// check for validity
//				if (sepx < multixs && sepy < multiys)
//				{
//					us_tecedpointout(nslist[i].node, nslist[i].node.parent);
//					ttyputerr(_("Multiple contact cuts must not overlap in %s"),
//						describenodeproto(np));
//					efree((CHAR *)nslist);
//					return(TRUE);
//				}
//	
//				// accumulate minimum separation
//				if (sepx >= multixs)
//				{
//					if (xsep < 0) xsep = sepx; else
//					{
//						if (xsep > sepx) xsep = sepx;
//					}
//				}
//				if (sepy >= multiys)
//				{
//					if (ysep < 0) ysep = sepy; else
//					{
//						if (ysep > sepy) ysep = sepy;
//					}
//				}
//			}
//	
//			// finally ensure that all separations are multiples of "multisep"
//			for(i=1; i<total; i++)
//			{
//				// find X separation
//				sepx = abs((nslist[i-1].node.highx + nslist[i-1].node.lowx) / 2 -
//					(nslist[i].node.highx + nslist[i].node.lowx) / 2);
//				sepy = abs((nslist[i-1].node.highy + nslist[i-1].node.lowy) / 2 -
//					(nslist[i].node.highy + nslist[i].node.lowy) / 2);
//				if (sepx / xsep * xsep != sepx)
//				{
//					us_tecedpointout(nslist[i].node, nslist[i].node.parent);
//					ttyputerr(_("Multiple contact cut X spacing must be uniform in %s"),
//						describenodeproto(np));
//					efree((CHAR *)nslist);
//					return(TRUE);
//				}
//	
//				// find Y separation
//				if (sepy / ysep * ysep != sepy)
//				{
//					us_tecedpointout(nslist[i].node, nslist[i].node.parent);
//					ttyputerr(_("Multiple contact cut Y spacing must be uniform in %s"),
//						describenodeproto(np));
//					efree((CHAR *)nslist);
//					return(TRUE);
//				}
//			}
//			efree((CHAR *)nslist);
//		}
//		multisep = xsep - multixs;
//		if (multisep != ysep - multiys)
//		{
//			us_tecedpointout(NONODEINST, np);
//			ttyputerr(_("Multiple contact cut X and Y spacing must be the same in %s"),
//				describenodeproto(np));
//			return(TRUE);
//		}
//		ns.rule = us_tecedaddmulticutrule(multixs, multiys, multiindent, multisep);
//		if (ns.rule == 0) return(TRUE);
//		return(FALSE);
//	}
//	
//	RULE *us_tecedaddmulticutrule(INTBIG multixs, INTBIG multiys, INTBIG multiindent, INTBIG multisep)
//	{
//		REGISTER RULE *rule;
//		INTBIG rulearr[8];
//	
//		rulearr[0] = -H0;   rulearr[1] = K1;
//		rulearr[2] = -H0;   rulearr[3] = K1;
//		rulearr[4] = -H0;   rulearr[5] = K3;
//		rulearr[6] = -H0;   rulearr[7] = K3;
//		rule = us_tecedaddrule(rulearr, 8, TRUE, (CHAR *)0);
//		if (rule == NORULE) return(0);
//		rule.multixs = multixs;
//		rule.multiys = multiys;
//		rule.multiindent = multiindent;
//		rule.multisep = multisep;
//		return(rule);
//	}
//	
//	/*
//	 * routine to add the "len"-long list of port connections in "conlist" to
//	 * the list of port connections, and return the port connection entry
//	 * for this one.  Returns NOPCON on error
//	 */
//	PCON *us_tecedaddportlist(INTBIG len, INTBIG *conlist)
//	{
//		REGISTER PCON *pc;
//		REGISTER INTBIG i, j;
//	
//		// find a port connection that is the same
//		for(pc = us_tecedfirstpcon; pc != NOPCON; pc = pc.nextpcon)
//		{
//			if (pc.total != len) continue;
//			for(j=0; j<pc.total; j++) pc.assoc[j] = 0;
//			for(i=0; i<len; i++)
//			{
//				for(j=0; j<pc.total; j++) if (pc.connects[j+1] == conlist[i])
//				{
//					pc.assoc[j]++;
//					break;
//				}
//			}
//			for(j=0; j<pc.total; j++) if (pc.assoc[j] == 0) break;
//			if (j >= pc.total) return(pc);
//		}
//	
//		// not found: add to list
//		pc = (PCON *)emalloc((sizeof (PCON)), us_tool.cluster);
//		if (pc == 0) return(NOPCON);
//		pc.total = len;
//		pc.connects = emalloc(((len+5)*SIZEOFINTBIG), us_tool.cluster);
//		if (pc.connects == 0) return(NOPCON);
//		pc.assoc = emalloc((len*SIZEOFINTBIG), us_tool.cluster);
//		if (pc.assoc == 0) return(NOPCON);
//		for(j=0; j<len; j++) pc.connects[j+1] = conlist[j];
//		pc.connects[0] = -1;
//		pc.connects[len+1] = AUNIV;
//		pc.connects[len+2] = AINVIS;
//		pc.connects[len+3] = AUNROUTED;
//		pc.connects[len+4] = -1;
//		pc.nextpcon = us_tecedfirstpcon;
//		us_tecedfirstpcon = pc;
//		return(pc);
//	}
//	
//	RULE *us_tecedaddrule(INTBIG list[8], INTBIG count, BOOLEAN multcut, CHAR *istext)
//	{
//		REGISTER RULE *r;
//		REGISTER INTBIG i, textinc;
//	
//		for(r = us_tecedfirstrule; r != NORULE; r = r.nextrule)
//		{
//			if (multcut != r.multicut) continue;
//			if (istext != 0 && r.istext != 0)
//			{
//				if (namesame(istext, (CHAR *)r.value[count]) != 0) continue;
//			} else if (istext != 0 || r.istext != 0) continue;
//			if (count != r.count) continue;
//			for(i=0; i<count; i++) if (r.value[i] != list[i]) break;
//			if (i >= count) return(r);
//		}
//	
//		r = (RULE *)emalloc((sizeof (RULE)), us_tool.cluster);
//		if (r == 0) return(NORULE);
//		if (istext != 0) textinc = 1; else textinc = 0;
//		r.value = emalloc(((count+textinc) * SIZEOFINTBIG), us_tool.cluster);
//		if (r.value == 0) return(NORULE);
//		r.count = count;
//		r.nextrule = us_tecedfirstrule;
//		r.used = FALSE;
//		r.multicut = multcut;
//		us_tecedfirstrule = r;
//		for(i=0; i<count; i++) r.value[i] = list[i];
//		r.istext = 0;
//		if (istext != 0)
//		{
//			(void)allocstring((CHAR **)(&r.value[count]), istext, us_tool.cluster);
//			r.istext = 1;
//		}
//		return(r);
//	}
	
//	/****************************** WRITE TECHNOLOGY AS "C" CODE ******************************/
//	
//	/*
//	 * routine to dump the layer information in technology "tech" to the stream in
//	 * "f".
//	 */
//	void us_teceditdumplayers(FILE *f, TECHNOLOGY *tech, CHAR *techname)
//	{
//		CHAR *sym, *colorname, *colorsymbol, date[30];
//		REGISTER INTBIG i, j, k, l;
//		REGISTER CHAR *l1, *l2, *l3, *l4, *l5;
//		REGISTER void *infstr;
//	
//		// write information for "tectable.c"
//		xprintf(f, x_("#if 0\n"));
//		xprintf(f, _("/* the next 4 lines belong at the top of 'tectable.c': */\n"));
//		xprintf(f, x_("extern GRAPHICS *%s_layers[];\n"), us_tecedmakesymbol(techname));
//		xprintf(f, x_("extern TECH_ARCS *%s_arcprotos[];\n"), us_tecedmakesymbol(techname));
//		xprintf(f, x_("extern TECH_NODES *%s_nodeprotos[];\n"), us_tecedmakesymbol(techname));
//		xprintf(f, x_("extern TECH_VARIABLES %s_variables[];\n"), us_tecedmakesymbol(techname));
//		xprintf(f, x_("\n/* the next 8 lines belong in the 'el_technologylist' array of 'tectable.c': */\n"));
//		xprintf(f, x_("\t{x_(\"%s\"), 0, %ld, NONODEPROTO,NOARCPROTO,NOVARIABLE,0,NOCOMCOMP,NOCLUSTER,\t/* info */\n"),
//			us_tecedmakesymbol(techname), tech.deflambda);
//		xprintf(f, x_("\tN_(\"%s\"),\t/* description */\n"), tech.techdescript);
//		xprintf(f, x_("\t0, %s_layers,"), us_tecedmakesymbol(techname));
//		xprintf(f, x_(" 0, %s_arcprotos,"), us_tecedmakesymbol(techname));
//		xprintf(f, x_(" 0, %s_nodeprotos,"), us_tecedmakesymbol(techname));
//		xprintf(f, x_(" %s_variables,\t/* tables */\n"), us_tecedmakesymbol(techname));
//		xprintf(f, x_("\t0, 0, 0, 0,\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t/* control routines */\n"));
//		xprintf(f, x_("\t0, 0, 0, 0, 0, 0, 0,\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t/* node routines */\n"));
//		xprintf(f, x_("\t0,\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t/* port routine */\n"));
//		xprintf(f, x_("\t0, 0, 0, 0,\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t/* arc routines */\n"));
//		xprintf(f, x_("\tNOTECHNOLOGY, NONEGATEDARCS|STATICTECHNOLOGY, 0, 0},\t\t\t\t\t\t\t/* miscellaneous */\n"));
//		xprintf(f, x_("#endif\n"));
//	
//		// write legal banner
//		xprintf(f, x_("/*\n"));
//		xprintf(f, x_(" * Electric(tm) VLSI Design System\n"));
//		xprintf(f, x_(" *\n"));
//		xprintf(f, x_(" * File: %s.c\n"), techname);
//		xprintf(f, x_(" * %s technology description\n"), techname);
//		xprintf(f, x_(" * Generated automatically from a library\n"));
//		xprintf(f, x_(" *\n"));
//		estrcpy(date, timetostring(getcurrenttime()));
//		date[24] = 0;
//		xprintf(f, x_(" * Copyright (c) %s Static Free Software.\n"), &date[20]);
//		xprintf(f, x_(" *\n"));
//		xprintf(f, x_(" * Electric(tm) is free software; you can redistribute it and/or modify\n"));
//		xprintf(f, x_(" * it under the terms of the GNU General Public License as published by\n"));
//		xprintf(f, x_(" * the Free Software Foundation; either version 2 of the License, or\n"));
//		xprintf(f, x_(" * (at your option) any later version.\n"));
//		xprintf(f, x_(" *\n"));
//		xprintf(f, x_(" * Electric(tm) is distributed in the hope that it will be useful,\n"));
//		xprintf(f, x_(" * but WITHOUT ANY WARRANTY; without even the implied warranty of\n"));
//		xprintf(f, x_(" * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"));
//		xprintf(f, x_(" * GNU General Public License for more details.\n"));
//		xprintf(f, x_(" *\n"));
//		xprintf(f, x_(" * You should have received a copy of the GNU General Public License\n"));
//		xprintf(f, x_(" * along with Electric(tm); see the file COPYING.  If not, write to\n"));
//		xprintf(f, x_(" * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,\n"));
//		xprintf(f, x_(" * Boston, Mass 02111-1307, USA.\n"));
//		xprintf(f, x_(" *\n"));
//		xprintf(f, x_(" * Static Free Software\n"));
//		xprintf(f, x_(" * 4119 Alpine Road\n"));
//		xprintf(f, x_(" * Portola Valley, California 94028\n"));
//		xprintf(f, x_(" * info@staticfreesoft.com\n"));
//		xprintf(f, x_(" */\n"));
//	
//		// write header
//		xprintf(f, x_("#include \"global.h\"\n"));
//		xprintf(f, x_("#include \"egraphics.h\"\n"));
//		xprintf(f, x_("#include \"tech.h\"\n"));
//		xprintf(f, x_("#include \"efunction.h\"\n"));
//	
//		// write the layer declarations
//		xprintf(f, x_("\n/******************** LAYERS ********************/\n"));
//		k = 8;
//		for(i=0; i<tech.layercount; i++) k = maxi(k, estrlen(us_teclayer_iname[i]));
//		k++;
//		xprintf(f, x_("\n#define MAXLAYERS"));
//		for(j=8; j<k; j++) xprintf(f, x_(" "));
//		xprintf(f, x_("%ld\n"), tech.layercount);
//		for(i=0; i<tech.layercount; i++)
//		{
//			xprintf(f, x_("#define L%s"), us_teclayer_iname[i]);
//			for(j=estrlen(us_teclayer_iname[i]); j<k; j++) xprintf(f, x_(" "));
//			xprintf(f, x_("%ld\t\t\t\t/* %s */\n"), i, us_teclayer_names[i]);
//		}
//		xprintf(f, x_("\n"));
//	
//		// write the layer descriptions
//		for(i=0; i<tech.layercount; i++)
//		{
//			xprintf(f, x_("static GRAPHICS %s_%s_lay = {"), us_tecedmakesymbol(techname),
//				us_teclayer_iname[i]);
//			switch (tech.layers[i].bits)
//			{
//				case LAYERT1: xprintf(f, x_("LAYERT1, "));   break;
//				case LAYERT2: xprintf(f, x_("LAYERT2, "));   break;
//				case LAYERT3: xprintf(f, x_("LAYERT3, "));   break;
//				case LAYERT4: xprintf(f, x_("LAYERT4, "));   break;
//				case LAYERT5: xprintf(f, x_("LAYERT5, "));   break;
//				case LAYERO:  xprintf(f, x_("LAYERO, "));    break;
//			}
//			if (ecolorname(tech.layers[i].col, &colorname, &colorsymbol)) colorsymbol = x_("unknown");
//			xprintf(f, x_("%s, "), colorsymbol);
//			if ((tech.layers[i].colstyle&NATURE) == SOLIDC) xprintf(f, x_("SOLIDC")); else
//			{
//				xprintf(f, x_("PATTERNED"));
//				if ((tech.layers[i].colstyle&OUTLINEPAT) != 0) xprintf(f, x_("|OUTLINEPAT"));
//			}
//			xprintf(f, x_(", "));
//			if ((tech.layers[i].bwstyle&NATURE) == SOLIDC) xprintf(f, x_("SOLIDC")); else
//			{
//				xprintf(f, x_("PATTERNED"));
//				if ((tech.layers[i].bwstyle&OUTLINEPAT) != 0) xprintf(f, x_("|OUTLINEPAT"));
//			}
//			xprintf(f, x_(","));
//	
//			xprintf(f, x_("\n"));
//			for(j=0; j<16; j++) if (tech.layers[i].raster[j] != 0) break;
//			if (j >= 16)
//				xprintf(f, x_("\t{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, NOVARIABLE, 0};\n")); else
//			{
//				for(j=0; j<16; j++)
//				{
//					xprintf(f, x_("\t"));
//					if (j == 0) xprintf(f, x_("{"));
//					xprintf(f, x_("0x%04x"), tech.layers[i].raster[j]&0xFFFF);
//					if (j == 15) xprintf(f, x_("}"));
//					xprintf(f, x_(","));
//					if (j > 0 && j < 15) xprintf(f, x_(" "));
//					xprintf(f, x_("  /* "));
//					for(k=0; k<16; k++)
//						if ((tech.layers[i].raster[j] & (1 << (15-k))) != 0)
//							xprintf(f, x_("X")); else xprintf(f, x_(" "));
//					xprintf(f, x_(" */\n"));
//				}
//				xprintf(f, x_("\tNOVARIABLE, 0};\n"));
//			}
//		}
//	
//		// write the aggregation of all layers
//		sym = us_tecedmakesymbol(techname);
//		xprintf(f, x_("\nGRAPHICS *%s_layers[MAXLAYERS+1] = {\n"), sym);
//		for(i=0; i<tech.layercount; i++)
//		{
//			xprintf(f, x_("\t&%s_%s_lay,"), sym, us_teclayer_iname[i]);
//			xprintf(f, x_("\t\t/* %s */\n"), us_teclayer_names[i]);
//		}
//		xprintf(f, x_("\tNOGRAPHICS\n};\n"));
//	
//		// write the layer names
//		sym = us_tecedmakesymbol(techname);
//		l = estrlen(sym) + 40;
//		xprintf(f, x_("static char *%s_layer_names[MAXLAYERS] = {"), sym);
//		for(i=0; i<tech.layercount; i++)
//		{
//			if (i != 0) { xprintf(f, x_(", ")); l += 2; }
//			if (us_teclayer_names[i] == 0) sym = x_(""); else sym = us_teclayer_names[i];
//			if (l + estrlen(sym) + 2 > 80)
//			{
//				xprintf(f, x_("\n\t"));
//				l = 4;
//			}
//			xprintf(f, x_("x_(\"%s\")"), sym);
//			l += estrlen(sym) + 2;
//		}
//		xprintf(f, x_("};\n"));
//	
//		// write the CIF layer names
//		if ((us_tecflags&HASCIF) != 0)
//		{
//			xprintf(f, x_("static char *%s_cif_layers[MAXLAYERS] = {\n"), us_tecedmakesymbol(techname));
//			for(i=0; i<tech.layercount; i++)
//			{
//				if (us_teccif_layers[i] == 0) xprintf(f, x_("\tx_(\"\")")); else
//					xprintf(f, x_("\tx_(\"%s\")"), us_teccif_layers[i]);
//				if (i != tech.layercount-1) xprintf(f, x_(","));
//				xprintf(f, x_("\t\t/* %s */\n"), us_teclayer_names[i]);
//			}
//			xprintf(f, x_("};\n"));
//		}
//	
//		// write the DXF layer numbers
//		if ((us_tecflags&HASDXF) != 0)
//		{
//			xprintf(f, x_("static char *%s_dxf_layers[MAXLAYERS] = {\n"), us_tecedmakesymbol(techname));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\tx_(\"%s\")"), us_tecdxf_layers[i]);
//				if (i != tech.layercount-1) xprintf(f, x_(","));
//				xprintf(f, x_("\t\t/* %s */\n"), us_teclayer_names[i]);
//			}
//			xprintf(f, x_("};\n"));
//		}
//	
//		// write the Calma GDS-II layer number
//		if ((us_tecflags&HASGDS) != 0)
//		{
//			xprintf(f, x_("static CHAR *%s_gds_layers[MAXLAYERS] = {\n"), us_tecedmakesymbol(techname));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\tx_(\"%s\")"), us_tecgds_layers[i]);
//				if (i != tech.layercount-1) xprintf(f, x_(","));
//				xprintf(f, x_("\t\t/* %s */\n"), us_teclayer_names[i]);
//			}
//			xprintf(f, x_("};\n"));
//		}
//	
//		// write the layer functions
//		xprintf(f, x_("static INTBIG %s_layer_function[MAXLAYERS] = {\n"), us_tecedmakesymbol(techname));
//		for(i=0; i<tech.layercount; i++)
//		{
//			infstr = initinfstr();
//			addstringtoinfstr(infstr, us_teclayer_functions[us_teclayer_function[i]&LFTYPE].constant);
//			for(j=0; us_teclayer_functions[j].name != 0; j++)
//			{
//				if (us_teclayer_functions[j].value <= LFTYPE) continue;
//				if ((us_teclayer_function[i]&us_teclayer_functions[j].value) != 0)
//				{
//					addtoinfstr(infstr, '|');
//					addstringtoinfstr(infstr, us_teclayer_functions[j].constant);
//				}
//			}
//			if (tech.layers[i].bits == LAYERT1) addstringtoinfstr(infstr, x_("|LFTRANS1")); else
//			if (tech.layers[i].bits == LAYERT2) addstringtoinfstr(infstr, x_("|LFTRANS2")); else
//			if (tech.layers[i].bits == LAYERT3) addstringtoinfstr(infstr, x_("|LFTRANS3")); else
//			if (tech.layers[i].bits == LAYERT4) addstringtoinfstr(infstr, x_("|LFTRANS4")); else
//			if (tech.layers[i].bits == LAYERT5) addstringtoinfstr(infstr, x_("|LFTRANS5"));
//			xprintf(f, x_("\t%s"), returninfstr(infstr));
//			if (i != tech.layercount-1) xprintf(f, x_(","));
//			xprintf(f, x_("\t\t/* %s */\n"), us_teclayer_names[i]);
//		}
//		xprintf(f, x_("};\n"));
//	
//		// write the layer letters
//		xprintf(f, x_("static char *%s_layer_letters[MAXLAYERS] = {\n"), us_tecedmakesymbol(techname));
//		for(i=0; i<tech.layercount; i++)
//		{
//			if (us_teclayer_letters[i] == 0) xprintf(f, x_("\tx_(\"\")")); else
//				xprintf(f, x_("\tx_(\"%s\")"), us_teclayer_letters[i]);
//			if (i != tech.layercount-1) xprintf(f, x_(","));
//			xprintf(f, x_("\t\t/* %s */\n"), us_teclayer_names[i]);
//		}
//		xprintf(f, x_("};\n"));
//	
//		// write the SPICE information
//		if ((us_tecflags&HASSPIRES) != 0)
//		{
//			xprintf(f, x_("static float %s_sim_spice_resistance[MAXLAYERS] = {\n"),
//				us_tecedmakesymbol(techname));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\t%s"), us_tecedmakefloatstring(us_tecspice_res[i]));
//				if (i != tech.layercount-1) xprintf(f, x_(","));
//				xprintf(f, x_("\t\t/* %s */\n"), us_teclayer_names[i]);
//			}
//			xprintf(f, x_("};\n"));
//		}
//		if ((us_tecflags&HASSPICAP) != 0)
//		{
//			xprintf(f, x_("static float %s_sim_spice_capacitance[MAXLAYERS] = {\n"),
//				us_tecedmakesymbol(techname));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\t%s"), us_tecedmakefloatstring(us_tecspice_cap[i]));
//				if (i != tech.layercount-1) xprintf(f, x_(","));
//				xprintf(f, x_("\t\t/* %s */\n"), us_teclayer_names[i]);
//			}
//			xprintf(f, x_("};\n"));
//		}
//		if ((us_tecflags&HASSPIECAP) != 0)
//		{
//			xprintf(f, x_("static float %s_sim_spice_edge_cap[MAXLAYERS] = {\n"),
//				us_tecedmakesymbol(techname));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\t%s"), us_tecedmakefloatstring(us_tecspice_ecap[i]));
//				if (i != tech.layercount-1) xprintf(f, x_(","));
//				xprintf(f, x_("\t\t/* %s */\n"), us_teclayer_names[i]);
//			}
//			xprintf(f, x_("};\n"));
//		}
//	
//		// write the 3D information
//		if ((us_tecflags&HAS3DINFO) != 0)
//		{
//			xprintf(f, x_("static INTBIG %s_3dheight_layers[MAXLAYERS] = {\n"),
//				us_tecedmakesymbol(techname));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\t%ld"), us_tec3d_height[i]);
//				if (i != tech.layercount-1) xprintf(f, x_(","));
//				xprintf(f, x_("\t\t/* %s */\n"), us_teclayer_names[i]);
//			}
//			xprintf(f, x_("};\n"));
//			xprintf(f, x_("static INTBIG %s_3dthick_layers[MAXLAYERS] = {\n"),
//				us_tecedmakesymbol(techname));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\t%ld"), us_tec3d_thickness[i]);
//				if (i != tech.layercount-1) xprintf(f, x_(","));
//				xprintf(f, x_("\t\t/* %s */\n"), us_teclayer_names[i]);
//			}
//			xprintf(f, x_("};\n"));
//		}
//		if ((us_tecflags&HASPRINTCOL) != 0)
//		{
//			xprintf(f, x_("static INTBIG %s_printcolors_layers[MAXLAYERS*5] = {\n"),
//				us_tecedmakesymbol(techname));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\t%ld,%ld,%ld, %ld,%ld"), us_tecprint_colors[i*5],
//					us_tecprint_colors[i*5+1], us_tecprint_colors[i*5+2],
//						us_tecprint_colors[i*5+3], us_tecprint_colors[i*5+4]);
//				if (i != tech.layercount-1) xprintf(f, x_(","));
//				xprintf(f, x_("\t\t/* %s */\n"), us_teclayer_names[i]);
//			}
//			xprintf(f, x_("};\n"));
//		}
//	
//		// write the color map
//		if ((us_tecflags&HASCOLORMAP) != 0)
//		{
//			// determine the five transparent layers
//			l1 = l2 = l3 = l4 = l5 = 0;
//			for(i=0; i<tech.layercount; i++)
//			{
//				if (tech.layers[i].bits == LAYERT1 && l1 == 0)
//					l1 = us_teclayer_names[i]; else
//				if (tech.layers[i].bits == LAYERT2 && l2 == 0)
//					l2 = us_teclayer_names[i]; else
//				if (tech.layers[i].bits == LAYERT3 && l3 == 0)
//					l3 = us_teclayer_names[i]; else
//				if (tech.layers[i].bits == LAYERT4 && l4 == 0)
//					l4 = us_teclayer_names[i]; else
//				if (tech.layers[i].bits == LAYERT5 && l5 == 0)
//					l5 = us_teclayer_names[i];
//			}
//			if (l1 == 0) l1 = x_("layer 1");
//			if (l2 == 0) l2 = x_("layer 2");
//			if (l3 == 0) l3 = x_("layer 3");
//			if (l4 == 0) l4 = x_("layer 4");
//			if (l5 == 0) l5 = x_("layer 5");
//			xprintf(f, x_("\nstatic TECH_COLORMAP %s_colmap[32] =\n{\n"), us_tecedmakesymbol(techname));
//			for(i=0; i<32; i++)
//			{
//				xprintf(f, x_("\t{%3d,%3d,%3d}, /* %2d: "), us_teccolmap[i].red,
//					us_teccolmap[i].green, us_teccolmap[i].blue, i);
//				if ((i&1) != 0) xprintf(f, x_("%s"), l1); else
//					for(j=0; j<(INTBIG)estrlen(l1); j++) xprintf(f, x_(" "));
//				xprintf(f, x_("+"));
//				if ((i&2) != 0) xprintf(f, x_("%s"), l2); else
//					for(j=0; j<(INTBIG)estrlen(l2); j++) xprintf(f, x_(" "));
//				xprintf(f, x_("+"));
//				if ((i&4) != 0) xprintf(f, x_("%s"), l3); else
//					for(j=0; j<(INTBIG)estrlen(l3); j++) xprintf(f, x_(" "));
//				xprintf(f, x_("+"));
//				if ((i&8) != 0) xprintf(f, x_("%s"), l4); else
//					for(j=0; j<(INTBIG)estrlen(l4); j++) xprintf(f, x_(" "));
//				xprintf(f, x_("+"));
//				if ((i&16) != 0) xprintf(f, x_("%s"), l5); else
//					for(j=0; j<(INTBIG)estrlen(l5); j++) xprintf(f, x_(" "));
//				xprintf(f, x_(" */\n"));
//			}
//			xprintf(f, x_("};\n"));
//		}
//	
//		// write design rules
//		if ((us_tecflags&(HASDRCMINWID|HASCONDRC|HASUNCONDRC|HASCONDRCW|HASUNCONDRCW|HASCONDRCM|HASUNCONDRCM|HASEDGEDRC)) != 0)
//		{
//			xprintf(f, x_("\n/******************** DESIGN RULES ********************/\n"));
//	
//			// write the DRC minimum width information
//			if ((us_tecflags&HASDRCMINWID) != 0)
//			{
//				xprintf(f, x_("static INTBIG %s_minimum_width[MAXLAYERS] = {"),
//					us_tecedmakesymbol(techname));
//				for(i=0; i<tech.layercount; i++)
//				{
//					if (i != 0) xprintf(f, x_(", "));
//					xprintf(f, x_("%s"), us_tecedmakefract(us_tecdrc_rules.minwidth[i]));
//				}
//				xprintf(f, x_("};\n"));
//				if ((us_tecflags&HASDRCMINWIDR) != 0)
//				{
//					xprintf(f, x_("static char *%s_minimum_width_rule[MAXLAYERS] = {"),
//						us_tecedmakesymbol(techname));
//					for(i=0; i<tech.layercount; i++)
//					{
//						if (i != 0) xprintf(f, x_(", "));
//						xprintf(f, x_("x_(\"%s\")"), us_tecdrc_rules.minwidthR[i]);
//					}
//					xprintf(f, x_("};\n"));
//				}
//			}
//	
//			if ((us_tecflags&HASCONDRC) != 0)
//			{
//				xprintf(f, x_("\nstatic INTBIG %s_connectedtable[] = {\n"),
//					us_tecedmakesymbol(techname));
//				us_teceditdumpdrctab(f, us_tecdrc_rules.conlist, tech, FALSE);
//				if ((us_tecflags&HASCONDRCR) != 0)
//				{
//					xprintf(f, x_("\nstatic char *%s_connectedtable_rule[] = {\n"),
//						us_tecedmakesymbol(techname));
//					us_teceditdumpdrctab(f, us_tecdrc_rules.conlistR, tech, TRUE);
//				}
//			}
//			if ((us_tecflags&HASUNCONDRC) != 0)
//			{
//				xprintf(f, x_("\nstatic INTBIG %s_unconnectedtable[] = {\n"),
//					us_tecedmakesymbol(techname));
//				us_teceditdumpdrctab(f, us_tecdrc_rules.unconlist, tech, FALSE);
//				if ((us_tecflags&HASUNCONDRCR) != 0)
//				{
//					xprintf(f, x_("\nstatic char *%s_unconnectedtable_rule[] = {\n"),
//						us_tecedmakesymbol(techname));
//					us_teceditdumpdrctab(f, us_tecdrc_rules.unconlistR, tech, TRUE);
//				}
//			}
//			if ((us_tecflags&HASCONDRCW) != 0)
//			{
//				xprintf(f, x_("\nstatic INTBIG %s_connectedtable_wide[] = {\n"),
//					us_tecedmakesymbol(techname));
//				us_teceditdumpdrctab(f, us_tecdrc_rules.conlistW, tech, FALSE);
//				if ((us_tecflags&HASCONDRCWR) != 0)
//				{
//					xprintf(f, x_("\nstatic char *%s_connectedtable_wide_rule[] = {\n"),
//						us_tecedmakesymbol(techname));
//					us_teceditdumpdrctab(f, us_tecdrc_rules.conlistWR, tech, TRUE);
//				}
//			}
//			if ((us_tecflags&HASUNCONDRCW) != 0)
//			{
//				xprintf(f, x_("\nstatic INTBIG %s_unconnectedtable_wide[] = {\n"),
//					us_tecedmakesymbol(techname));
//				us_teceditdumpdrctab(f, us_tecdrc_rules.unconlistW, tech, FALSE);
//				if ((us_tecflags&HASUNCONDRCWR) != 0)
//				{
//					xprintf(f, x_("\nstatic char *%s_unconnectedtable_wide_rule[] = {\n"),
//						us_tecedmakesymbol(techname));
//					us_teceditdumpdrctab(f, us_tecdrc_rules.unconlistWR, tech, TRUE);
//				}
//			}
//			if ((us_tecflags&HASCONDRCM) != 0)
//			{
//				xprintf(f, x_("\nstatic INTBIG %s_connectedtable_multi[] = {\n"),
//					us_tecedmakesymbol(techname));
//				us_teceditdumpdrctab(f, us_tecdrc_rules.conlistM, tech, FALSE);
//				if ((us_tecflags&HASCONDRCMR) != 0)
//				{
//					xprintf(f, x_("\nstatic char *%s_connectedtable_multi_rule[] = {\n"),
//						us_tecedmakesymbol(techname));
//					us_teceditdumpdrctab(f, us_tecdrc_rules.conlistMR, tech, TRUE);
//				}
//			}
//			if ((us_tecflags&HASUNCONDRCM) != 0)
//			{
//				xprintf(f, x_("\nstatic INTBIG %s_unconnectedtable_multi[] = {\n"),
//					us_tecedmakesymbol(techname));
//				us_teceditdumpdrctab(f, us_tecdrc_rules.unconlistM, tech, FALSE);
//				if ((us_tecflags&HASUNCONDRCMR) != 0)
//				{
//					xprintf(f, x_("\nstatic char *%s_unconnectedtable_multi_rule[] = {\n"),
//						us_tecedmakesymbol(techname));
//					us_teceditdumpdrctab(f, us_tecdrc_rules.unconlistMR, tech, TRUE);
//				}
//			}
//			if ((us_tecflags&HASEDGEDRC) != 0)
//			{
//				xprintf(f, x_("\nstatic INTBIG %s_edgetable[] = {\n"),
//					us_tecedmakesymbol(techname));
//				us_teceditdumpdrctab(f, us_tecdrc_rules.edgelist, tech, FALSE);
//				if ((us_tecflags&HASEDGEDRCR) != 0)
//				{
//					xprintf(f, x_("\nstatic char *%s_edgetable_rule[] = {\n"),
//						us_tecedmakesymbol(techname));
//					us_teceditdumpdrctab(f, us_tecdrc_rules.edgelistR, tech, TRUE);
//				}
//			}
//		}
//	}
//	
//	void us_teceditdumpdrctab(FILE *f, void *distances, TECHNOLOGY *tech, BOOLEAN isstring)
//	{
//		REGISTER INTBIG i, j;
//		REGISTER INTBIG amt, mod, *amtlist;
//		CHAR shortname[7], *msg, **distlist;
//	
//		for(i=0; i<6; i++)
//		{
//			xprintf(f, x_("/*            "));
//			for(j=0; j<tech.layercount; j++)
//			{
//				if ((INTBIG)estrlen(us_teclayer_iname[j]) <= i) xprintf(f, x_(" ")); else
//					xprintf(f, x_("%c"), us_teclayer_iname[j][i]);
//				xprintf(f, x_("  "));
//			}
//			xprintf(f, x_(" */\n"));
//		}
//		if (isstring) distlist = (CHAR **)distances; else
//			amtlist = (INTBIG *)distances;
//		for(j=0; j<tech.layercount; j++)
//		{
//			(void)estrncpy(shortname, us_teclayer_iname[j], 6);
//			shortname[6] = 0;
//			xprintf(f, x_("/* %-6s */ "), shortname);
//			for(i=0; i<j; i++) xprintf(f, x_("   "));
//			for(i=j; i<tech.layercount; i++)
//			{
//				if (isstring)
//				{
//					msg = *distlist++;
//					xprintf(f, x_("x_(\"%s\")"), msg);
//				} else
//				{
//					amt = *amtlist++;
//					if (amt < 0) xprintf(f, x_("XX")); else
//					{
//						mod = amt % WHOLE;
//						if (mod == 0) xprintf(f, x_("K%ld"), amt/WHOLE); else
//						if (mod == WHOLE/2) xprintf(f, x_("H%ld"), amt/WHOLE); else
//						if (mod == WHOLE/4) xprintf(f, x_("Q%ld"), amt/WHOLE); else
//						if (mod == WHOLE/4*3) xprintf(f, x_("T%ld"), amt/WHOLE); else
//							xprintf(f, x_("%ld"), amt);
//					}
//				}
//				if (j != tech.layercount-1 || i != tech.layercount-1)
//					xprintf(f, x_(","));
//			}
//			xprintf(f, x_("\n"));
//		}
//		xprintf(f, x_("};\n"));
//	}
//	
//	/*
//	 * routine to dump the arc information in technology "tech" to the stream in
//	 * "f".
//	 */
//	void us_teceditdumparcs(FILE *f, TECHNOLOGY *tech, CHAR *techname)
//	{
//		REGISTER INTBIG i, j, k;
//	
//		// print the header
//		xprintf(f, x_("\n/******************** ARCS ********************/\n"));
//	
//		// compute the width of the widest arc name
//		k = 12;
//		for(i=0; i<tech.arcprotocount; i++)
//			k = maxi(k, estrlen(tech.arcprotos[i].arcname));
//		k++;
//	
//		// write the number of arcs
//		xprintf(f, x_("\n#define ARCPROTOCOUNT"));
//		for(j=12; j<k; j++) xprintf(f, x_(" "));
//		xprintf(f, x_("%ld\n"), tech.arcprotocount);
//	
//		// write defines for each arc
//		for(i=0; i<tech.arcprotocount; i++)
//		{
//			xprintf(f, x_("#define A%s"), us_tecedmakeupper(tech.arcprotos[i].arcname));
//			for(j=estrlen(tech.arcprotos[i].arcname); j<k; j++)
//				xprintf(f, x_(" "));
//			xprintf(f, x_("%ld\t\t\t\t/* %s */\n"), i, tech.arcprotos[i].arcname);
//		}
//	
//		// now write the arcs
//		for(i=0; i<tech.arcprotocount; i++)
//		{
//			xprintf(f, x_("\nstatic TECH_ARCLAY %s_al_%ld[] = {"),
//				us_tecedmakesymbol(techname), i);
//			for(k=0; k<tech.arcprotos[i].laycount; k++)
//			{
//				if (k != 0) xprintf(f, x_(", "));
//				xprintf(f, x_("{"));
//				xprintf(f, x_("L%s,"), us_teclayer_iname[tech.arcprotos[i].list[k].lay]);
//				if (tech.arcprotos[i].list[k].off == 0) xprintf(f, x_("0,")); else
//					xprintf(f, x_("%s,"), us_tecedmakefract(tech.arcprotos[i].list[k].off));
//				if (tech.arcprotos[i].list[k].style == FILLED) xprintf(f, x_("FILLED}")); else
//					xprintf(f, x_("CLOSED}"));
//			}
//			xprintf(f, x_("};\n"));
//			xprintf(f, x_("static TECH_ARCS %s_a_%ld = {\n"), us_tecedmakesymbol(techname), i);
//			xprintf(f, x_("\tx_(\"%s\"), "), tech.arcprotos[i].arcname);
//			xprintf(f, x_("%s, "), us_tecedmakefract(tech.arcprotos[i].arcwidth));
//			xprintf(f, x_("A%s,NOARCPROTO,\n"), us_tecedmakeupper(tech.arcprotos[i].arcname));
//			xprintf(f, x_("\t%d, %s_al_%ld,\n"), tech.arcprotos[i].laycount,
//				us_tecedmakesymbol(techname), i);
//			for(j=0; us_tecarc_functions[j].name != 0; j++)
//				if (us_tecarc_functions[j].value ==
//					(INTBIG)((tech.arcprotos[i].initialbits&AFUNCTION)>>AFUNCTIONSH))
//			{
//				xprintf(f, x_("\t(%s<<AFUNCTIONSH)"), us_tecarc_functions[j].constant);
//				break;
//			}
//			if (us_tecarc_functions[j].name == 0)                    xprintf(f, x_("\t(APUNKNOWN<<AFUNCTIONSH)"));
//			if ((tech.arcprotos[i].initialbits&WANTFIXANG) != 0)   xprintf(f, x_("|WANTFIXANG"));
//			if ((tech.arcprotos[i].initialbits&CANWIPE) != 0)      xprintf(f, x_("|CANWIPE"));
//			if ((tech.arcprotos[i].initialbits&WANTNOEXTEND) != 0) xprintf(f, x_("|WANTNOEXTEND"));
//			xprintf(f, x_("|(%ld<<AANGLEINCSH)"), (tech.arcprotos[i].initialbits&AANGLEINC)>>AANGLEINCSH);
//			xprintf(f, x_("};\n"));
//		}
//	
//		// print the summary
//		xprintf(f, x_("\nTECH_ARCS *%s_arcprotos[ARCPROTOCOUNT+1] = {\n\t"),
//			us_tecedmakesymbol(techname));
//		for(i=0; i<tech.arcprotocount; i++)
//			xprintf(f, x_("&%s_a_%ld, "), us_tecedmakesymbol(techname), i);
//		xprintf(f, x_("((TECH_ARCS *)-1)};\n"));
//	
//		// print the variable with the width offsets
//		if ((us_tecflags&HASARCWID) != 0)
//		{
//			xprintf(f, x_("\nstatic INTBIG %s_arc_widoff[ARCPROTOCOUNT] = {"),
//				us_tecedmakesymbol(techname));
//			for(i=0; i<tech.arcprotocount; i++)
//			{
//				if (i != 0) xprintf(f, x_(", "));
//				if (us_tecarc_widoff[i] == 0) xprintf(f, x_("0")); else
//					xprintf(f, x_("%s"), us_tecedmakefract(us_tecarc_widoff[i]));
//			}
//			xprintf(f, x_("};\n"));
//		}
//	}
//	
//	/*
//	 * routine to dump the node information in technology "tech" to the stream in
//	 * "f".
//	 */
//	void us_teceditdumpnodes(FILE *f, TECHNOLOGY *tech, CHAR *techname)
//	{
//		REGISTER RULE *r;
//		REGISTER INTBIG i, j, k, l, tot;
//		CHAR *ab, *sym;
//		BOOLEAN yaxis;
//		REGISTER PCON *pc;
//		REGISTER TECH_POLYGON *plist;
//		REGISTER TECH_SERPENT *slist;
//		REGISTER TECH_NODES *nlist;
//		REGISTER void *infstr;
//	
//		// make abbreviations for each node
//		for(i=0; i<tech.nodeprotocount; i++)
//		{
//			(void)allocstring(&ab, makeabbrev(tech.nodeprotos[i].nodename, FALSE), el_tempcluster);
//			tech.nodeprotos[i].creation = (NODEPROTO *)ab;
//	
//			// loop until the name is unique
//			for(;;)
//			{
//				// see if a previously assigned abbreviation is the same
//				for(j=0; j<i; j++)
//					if (namesame(ab, (CHAR *)tech.nodeprotos[j].creation) == 0) break;
//				if (j == i) break;
//	
//				// name conflicts: change it
//				l = estrlen(ab);
//				if (ab[l-1] >= '0' && ab[l-1] <= '8') ab[l-1]++; else
//				{
//					infstr = initinfstr();
//					addstringtoinfstr(infstr, ab);
//					addtoinfstr(infstr, '0');
//					(void)reallocstring(&ab, returninfstr(infstr), el_tempcluster);
//					tech.nodeprotos[i].creation = (NODEPROTO *)ab;
//				}
//			}
//		}
//	
//		// write the port lists
//		xprintf(f, x_("\n/******************** PORT CONNECTIONS ********************/\n\n"));
//		i = 1;
//		for(pc = us_tecedfirstpcon; pc != NOPCON; pc = pc.nextpcon)
//		{
//			pc.pcindex = i++;
//			xprintf(f, x_("static INTBIG %s_pc_%ld[] = {-1, "),
//				us_tecedmakesymbol(techname), pc.pcindex);
//			for(j=0; j<pc.total; j++)
//			{
//				k = pc.connects[j+1];
//				xprintf(f, x_("A%s, "), us_tecedmakeupper(tech.arcprotos[k].arcname));
//			}
//			xprintf(f, x_("ALLGEN, -1};\n"));
//		}
//	
//		xprintf(f, x_("\n/******************** RECTANGLE DESCRIPTIONS ********************/"));
//		xprintf(f, x_("\n\n"));
//	
//		// print box information
//		i = 1;
//		for(r = us_tecedfirstrule; r != NORULE; r = r.nextrule)
//		{
//			if (!r.used) continue;
//			r.rindex = i++;
//			xprintf(f, x_("static INTBIG %s_box%ld[%ld] = {"),
//				us_tecedmakesymbol(techname), r.rindex, r.count);
//			for(j=0; j<r.count; j += 2)
//			{
//				if (j != 0) xprintf(f, x_(", "));
//				if ((j%4) == 0) yaxis = FALSE; else yaxis = TRUE;
//				xprintf(f, x_("%s"), us_tecededgelabel(r.value[j], r.value[j+1], yaxis));
//			}
//			if (r.istext != 0)
//				xprintf(f, x_(", x_(\"%s\")"), (CHAR *)r.value[r.count]);
//			xprintf(f, x_("};\n"));
//		}
//	
//		xprintf(f, x_("\n/******************** NODES ********************/\n"));
//	
//		// compute widest node name
//		k = 13;
//		for(i=0; i<tech.nodeprotocount; i++)
//			k = maxi(k, estrlen((CHAR *)tech.nodeprotos[i].creation));
//		k++;
//	
//		// write the total define
//		xprintf(f, x_("\n#define NODEPROTOCOUNT"));
//		for(j=13; j<k; j++) xprintf(f, x_(" "));
//		xprintf(f, x_("%ld\n"), tech.nodeprotocount);
//	
//		// write the other defines
//		for(i=0; i<tech.nodeprotocount; i++)
//		{
//			ab = (CHAR *)tech.nodeprotos[i].creation;
//			xprintf(f, x_("#define N%s"), us_tecedmakeupper(ab));
//			for(j=estrlen(ab); j<k; j++) xprintf(f, x_(" "));
//			xprintf(f, x_("%ld\t\t\t\t/* %s */\n"), i+1, tech.nodeprotos[i].nodename);
//		}
//	
//		// print node information
//		for(i=0; i<tech.nodeprotocount; i++)
//		{
//			// header comment
//			nlist = tech.nodeprotos[i];
//			ab = (CHAR *)nlist.creation;
//			xprintf(f, x_("\n/* %s */\n"), nlist.nodename);
//	
//			// print ports
//			xprintf(f, x_("static TECH_PORTS %s_%s_p[] = {\n"), us_tecedmakesymbol(techname), ab);
//			for(j=0; j<nlist.portcount; j++)
//			{
//				if (j != 0) xprintf(f, x_(",\n"));
//	
//				// the name of the connection structure
//				for(pc = us_tecedfirstpcon; pc != NOPCON; pc = pc.nextpcon)
//					if (pc.connects == nlist.portlist[j].portarcs) break;
//				if (pc != NOPCON)
//					xprintf(f, x_("\t{%s_pc_%ld, "), us_tecedmakesymbol(techname), pc.pcindex);
//	
//				// the port name
//				xprintf(f, x_("x_(\"%s\"), NOPORTPROTO, "), nlist.portlist[j].protoname);
//	
//				// the port userbits
//				xprintf(f, x_("(%ld<<PORTARANGESH)"),
//					(nlist.portlist[j].initialbits&PORTARANGE)>>PORTARANGESH);
//				if ((nlist.portlist[j].initialbits&PORTANGLE) != 0)
//					xprintf(f, x_("|(%ld<<PORTANGLESH)"),
//						(nlist.portlist[j].initialbits&PORTANGLE)>>PORTANGLESH);
//				if ((nlist.portlist[j].initialbits&PORTNET) != 0)
//					xprintf(f, x_("|(%ld<<PORTNETSH)"), (nlist.portlist[j].initialbits&PORTNET)>>PORTNETSH);
//				xprintf(f, x_(",\n"));
//	
//				// the port area
//				xprintf(f, x_("\t\t%s, %s, %s, %s}"),
//					us_tecededgelabel(nlist.portlist[j].lowxmul, nlist.portlist[j].lowxsum, FALSE),
//					us_tecededgelabel(nlist.portlist[j].lowymul, nlist.portlist[j].lowysum, TRUE),
//					us_tecededgelabel(nlist.portlist[j].highxmul, nlist.portlist[j].highxsum, FALSE),
//					us_tecededgelabel(nlist.portlist[j].highymul, nlist.portlist[j].highysum, TRUE));
//			}
//			xprintf(f, x_("};\n"));
//	
//			// print layers
//			for(k=0; k<2; k++)
//			{
//				if (nlist.special == SERPTRANS)
//				{
//					if (k == 0)
//					{
//						xprintf(f, x_("static TECH_SERPENT %s_%s_l[] = {\n"),
//							us_tecedmakesymbol(techname), ab);
//						tot = nlist.layercount;
//					} else
//					{
//						xprintf(f, x_("static TECH_SERPENT %s_%sE_l[] = {\n"),
//							us_tecedmakesymbol(techname), ab);
//						tot = nlist.layercount + 1;
//					}
//				} else
//				{
//					if (k != 0) continue;
//					xprintf(f, x_("static TECH_POLYGON %s_%s_l[] = {\n"),
//						us_tecedmakesymbol(techname), ab);
//					tot = nlist.layercount;
//				}
//				for(j=0; j<tot; j++)
//				{
//					if (j != 0) xprintf(f, x_(",\n"));
//					xprintf(f, x_("\t"));
//					if (nlist.special == SERPTRANS)
//					{
//						xprintf(f, x_("{"));
//						if (k == 0) plist = &nlist.gra[j].basics;
//							else plist = &nlist.ele[j].basics;
//					} else plist = &nlist.layerlist[j];
//					xprintf(f, x_("{L%s,"), us_teclayer_iname[plist.layernum]);
//					xprintf(f, x_(" %d,"), plist.portnum);
//					xprintf(f, x_(" %d,"), plist.count);
//					switch (plist.style)
//					{
//						case FILLEDRECT:     xprintf(f, x_(" FILLEDRECT,"));     break;
//						case CLOSEDRECT:     xprintf(f, x_(" CLOSEDRECT,"));     break;
//						case CROSSED:        xprintf(f, x_(" CROSSED,"));        break;
//						case FILLED:         xprintf(f, x_(" FILLED,"));         break;
//						case CLOSED:         xprintf(f, x_(" CLOSED,"));         break;
//						case OPENED:         xprintf(f, x_(" OPENED,"));         break;
//						case OPENEDT1:       xprintf(f, x_(" OPENEDT1,"));       break;
//						case OPENEDT2:       xprintf(f, x_(" OPENEDT2,"));       break;
//						case OPENEDT3:       xprintf(f, x_(" OPENEDT3,"));       break;
//						case VECTORS:        xprintf(f, x_(" VECTORS,"));        break;
//						case CIRCLE:         xprintf(f, x_(" CIRCLE,"));         break;
//						case THICKCIRCLE:    xprintf(f, x_(" THICKCIRCLE,"));    break;
//						case DISC:           xprintf(f, x_(" DISC,"));           break;
//						case CIRCLEARC:      xprintf(f, x_(" CIRCLEARC,"));      break;
//						case THICKCIRCLEARC: xprintf(f, x_(" THICKCIRCLEARC,")); break;
//						case TEXTCENT:       xprintf(f, x_(" TEXTCENT,"));       break;
//						case TEXTTOP:        xprintf(f, x_(" TEXTTOP,"));        break;
//						case TEXTBOT:        xprintf(f, x_(" TEXTBOT,"));        break;
//						case TEXTLEFT:       xprintf(f, x_(" TEXTLEFT,"));       break;
//						case TEXTRIGHT:      xprintf(f, x_(" TEXTRIGHT,"));      break;
//						case TEXTTOPLEFT:    xprintf(f, x_(" TEXTTOPLEFT,"));    break;
//						case TEXTBOTLEFT:    xprintf(f, x_(" TEXTBOTLEFT,"));    break;
//						case TEXTTOPRIGHT:   xprintf(f, x_(" TEXTTOPRIGHT,"));   break;
//						case TEXTBOTRIGHT:   xprintf(f, x_(" TEXTBOTRIGHT,"));   break;
//						case TEXTBOX:        xprintf(f, x_(" TEXTBOX,"));        break;
//						default:             xprintf(f, x_(" ????,"));           break;
//					}
//					switch (plist.representation)
//					{
//						case BOX:    xprintf(f, x_(" BOX,"));     break;
//						case MINBOX: xprintf(f, x_(" MINBOX,"));  break;
//						case POINTS: xprintf(f, x_(" POINTS,"));  break;
//						default:     xprintf(f, x_(" ????,"));    break;
//					}
//					for(r = us_tecedfirstrule; r != NORULE; r = r.nextrule)
//						if (r.value == plist.points) break;
//					if (r != NORULE)
//						xprintf(f, x_(" %s_box%ld"), us_tecedmakesymbol(techname), r.rindex); else
//							xprintf(f, x_(" %s_box??"), us_tecedmakesymbol(techname));
//					xprintf(f, x_("}"));
//					if (nlist.special == SERPTRANS)
//					{
//						if (k == 0) slist = &nlist.gra[j]; else
//							slist = &nlist.ele[j];
//						xprintf(f, x_(", %s"), us_tecedmakefract(slist.lwidth));
//						xprintf(f, x_(", %s"), us_tecedmakefract(slist.rwidth));
//						xprintf(f, x_(", %s"), us_tecedmakefract(slist.extendt));
//						xprintf(f, x_(", %s}"), us_tecedmakefract(slist.extendb));
//					}
//				}
//				xprintf(f, x_("};\n"));
//			}
//	
//			// print the node information
//			xprintf(f, x_("static TECH_NODES %s_%s = {\n"), us_tecedmakesymbol(techname), ab);
//			xprintf(f, x_("\tx_(\"%s\"), N%s, NONODEPROTO,\n"), nlist.nodename, us_tecedmakeupper(ab));
//			xprintf(f, x_("\t%s,"), us_tecedmakefract(nlist.xsize));
//			xprintf(f, x_(" %s,\n"), us_tecedmakefract(nlist.ysize));
//			xprintf(f, x_("\t%d, %s_%s_p,\n"), nlist.portcount, us_tecedmakesymbol(techname), ab);
//			if (nlist.special == SERPTRANS)
//				xprintf(f, x_("\t%d, (TECH_POLYGON *)0,\n"), nlist.layercount); else
//					xprintf(f, x_("\t%d, %s_%s_l,\n"), nlist.layercount,
//						us_tecedmakesymbol(techname), ab);
//			j = (nlist.initialbits&NFUNCTION)>>NFUNCTIONSH;
//			if (j < 0 || j >= MAXNODEFUNCTION) j = 0;
//			xprintf(f, x_("\t(%s<<NFUNCTIONSH)"), nodefunctionconstantname(j));
//			if ((nlist.initialbits&WIPEON1OR2) != 0) xprintf(f, x_("|WIPEON1OR2"));
//			if ((nlist.initialbits&HOLDSTRACE) != 0) xprintf(f, x_("|HOLDSTRACE"));
//			if ((nlist.initialbits&NSQUARE) != 0)    xprintf(f, x_("|NSQUARE"));
//			if ((nlist.initialbits&ARCSWIPE) != 0)   xprintf(f, x_("|ARCSWIPE"));
//			if ((nlist.initialbits&ARCSHRINK) != 0)  xprintf(f, x_("|ARCSHRINK"));
//			if ((nlist.initialbits&NODESHRINK) != 0) xprintf(f, x_("|NODESHRINK"));
//			if ((nlist.initialbits&LOCKEDPRIM) != 0) xprintf(f, x_("|LOCKEDPRIM"));
//			xprintf(f, x_(",\n"));
//			switch (nlist.special)
//			{
//				case 0:
//					xprintf(f, x_("\t0,0,0,0,0,0,0,0,0"));
//					break;
//				case SERPTRANS:
//					xprintf(f, x_("\tSERPTRANS,%d,"), nlist.f1);
//					xprintf(f, x_("%s,"), us_tecedmakefract(nlist.f2));
//					xprintf(f, x_("%s,"), us_tecedmakefract(nlist.f3));
//					xprintf(f, x_("%s,"), us_tecedmakefract(nlist.f4));
//					xprintf(f, x_("%s,"), us_tecedmakefract(nlist.f5));
//					xprintf(f, x_("%s,"), us_tecedmakefract(nlist.f6));
//					xprintf(f, x_("%s_%s_l,"), us_tecedmakesymbol(techname), ab);
//					xprintf(f, x_("%s_%sE_l"), us_tecedmakesymbol(techname), ab);
//					break;
//				case MULTICUT:
//					xprintf(f, x_("\tMULTICUT,%s,"), us_tecedmakefract(nlist.f1));
//					xprintf(f, x_("%s,"), us_tecedmakefract(nlist.f2));
//					xprintf(f, x_("%s,"), us_tecedmakefract(nlist.f3));
//					xprintf(f, x_("%s,0,0,0,0"), us_tecedmakefract(nlist.f4));
//					break;
//				case POLYGONAL:
//					xprintf(f, x_("\tPOLYGONAL,0,0,0,0,0,0,0,0"));
//					break;
//			}
//			xprintf(f, x_("};\n"));
//		}
//	
//		// print summary of nodes
//		xprintf(f, x_("\nTECH_NODES *%s_nodeprotos[NODEPROTOCOUNT+1] = {\n\t"),
//			us_tecedmakesymbol(techname));
//		l = 4;
//		for(i=0; i<tech.nodeprotocount; i++)
//		{
//			sym = us_tecedmakesymbol(techname);
//			if (l + estrlen(sym) + estrlen((CHAR *)tech.nodeprotos[i].creation) + 4 > 80)
//			{
//				xprintf(f, x_("\n\t"));
//				l = 4;
//			}
//			xprintf(f, x_("&%s_%s, "), sym, (CHAR *)tech.nodeprotos[i].creation);
//			l += estrlen(sym) + estrlen((CHAR *)tech.nodeprotos[i].creation) + 4;
//		}
//		xprintf(f, x_("((TECH_NODES *)-1)};\n"));
//	
//		// print highlight offset information
//		xprintf(f, x_("\nstatic INTBIG %s_node_widoff[NODEPROTOCOUNT*4] = {\n\t"),
//			us_tecedmakesymbol(techname));
//		l = 4;
//		for(i=0; i<tech.nodeprotocount; i++)
//		{
//			if (i != 0) { xprintf(f, x_(", ")); l += 2; }
//			infstr = initinfstr();
//			if (us_tecnode_widoff[i*4] == 0) addtoinfstr(infstr, '0'); else
//				addstringtoinfstr(infstr, us_tecedmakefract(us_tecnode_widoff[i*4]));
//			addtoinfstr(infstr, ',');
//			if (us_tecnode_widoff[i*4+1] == 0) addtoinfstr(infstr, '0'); else
//				addstringtoinfstr(infstr, us_tecedmakefract(us_tecnode_widoff[i*4+1]));
//			addtoinfstr(infstr, ',');
//			if (us_tecnode_widoff[i*4+2] == 0) addtoinfstr(infstr, '0'); else
//				addstringtoinfstr(infstr, us_tecedmakefract(us_tecnode_widoff[i*4+2]));
//			addtoinfstr(infstr, ',');
//			if (us_tecnode_widoff[i*4+3] == 0) addtoinfstr(infstr, '0'); else
//				addstringtoinfstr(infstr, us_tecedmakefract(us_tecnode_widoff[i*4+3]));
//			sym = returninfstr(infstr);
//			l += estrlen(sym);
//			if (l > 80)
//			{
//				xprintf(f, x_("\n\t"));
//				l = 4;
//			}
//			xprintf(f, x_("%s"), sym);
//		}
//		xprintf(f, x_("};\n"));
//	
//		// print grab point informaton if it exists
//		if ((us_tecflags&HASGRAB) != 0 && us_tecnode_grabcount > 0)
//		{
//			xprintf(f, x_("\nstatic INTBIG %s_centergrab[] = {\n"), us_tecedmakesymbol(techname));
//			for(i=0; i<us_tecnode_grabcount; i += 3)
//			{
//				ab = (CHAR *)tech.nodeprotos[us_tecnode_grab[i]-1].creation;
//				xprintf(f, x_("\tN%s, %ld, %ld"), us_tecedmakeupper(ab), us_tecnode_grab[i+1],
//					us_tecnode_grab[i+2]);
//				if (i != us_tecnode_grabcount-3) xprintf(f, x_(",\n"));
//			}
//			xprintf(f, x_("\n};\n"));
//		}
//	
//		// print minimum node size informaton if it exists
//		if ((us_tecflags&HASMINNODE) != 0)
//		{
//			xprintf(f, x_("\nstatic INTBIG %s_node_minsize[NODEPROTOCOUNT*2] = {\n"), us_tecedmakesymbol(techname));
//			for(i=0; i<tech.nodeprotocount; i++)
//			{
//				if (us_tecdrc_rules.minnodesize[i*2] < 0) ab = x_("XX"); else
//					ab = us_tecedmakefract(us_tecdrc_rules.minnodesize[i*2]);
//				xprintf(f, x_("\t%s, "), ab);
//				if (us_tecdrc_rules.minnodesize[i*2+1] < 0) ab = x_("XX"); else
//					ab = us_tecedmakefract(us_tecdrc_rules.minnodesize[i*2+1]);
//				xprintf(f, x_("%s"), ab);
//				if (i == tech.nodeprotocount-1) ab = x_(""); else ab = x_(",");
//				xprintf(f, x_("%s\t\t/* %s */\n"), ab, tech.nodeprotos[i].nodename);
//			}
//			xprintf(f, x_("};\n"));
//		}
//		if ((us_tecflags&HASMINNODER) != 0)
//		{
//			xprintf(f, x_("\nstatic char *%s_node_minsize_rule[NODEPROTOCOUNT] = {\n"), us_tecedmakesymbol(techname));
//			for(i=0; i<tech.nodeprotocount; i++)
//			{
//				if (i == tech.nodeprotocount-1) ab = x_(""); else ab = x_(",");
//				xprintf(f, x_("\tx_(\"%s\")%s\t\t/* %s */\n"), us_tecdrc_rules.minnodesizeR[i], ab,
//					tech.nodeprotos[i].nodename);
//			}
//			xprintf(f, x_("};\n"));
//		}
//	
//		// clean up
//		for(i=0; i<tech.nodeprotocount; i++)
//		{
//			efree((CHAR *)tech.nodeprotos[i].creation);
//			tech.nodeprotos[i].creation = NONODEPROTO;
//		}
//	}
//	
//	/*
//	 * routine to dump the variable information in technology "tech" to the stream in
//	 * "f".
//	 */
//	void us_teceditdumpvars(FILE *f, TECHNOLOGY *tech, CHAR *techname)
//	{
//		REGISTER INTBIG i, j, k;
//		REGISTER CHAR *pt;
//		REGISTER VARIABLE *var;
//	
//		xprintf(f, x_("\n/******************** VARIABLE AGGREGATION ********************/\n"));
//	
//		// write any miscellaneous string array variables
//		for(i=0; us_knownvars[i].varname != 0; i++)
//		{
//			var = getval((INTBIG)tech, VTECHNOLOGY, -1, us_knownvars[i].varname);
//			if (var == NOVARIABLE) continue;
//			if ((var.type&(VTYPE|VISARRAY)) == (VSTRING|VISARRAY))
//			{
//				xprintf(f, x_("\nchar *%s_%s[] = {\n"), us_tecedmakesymbol(techname),
//					us_knownvars[i].varname);
//				j = getlength(var);
//				for(k=0; k<j; k++)
//				{
//					xprintf(f, x_("\tx_(\""));
//					for(pt = ((CHAR **)var.addr)[k]; *pt != 0; pt++)
//					{
//						if (*pt == '"') xprintf(f, x_("\\"));
//						xprintf(f, x_("%c"), *pt);
//					}
//					xprintf(f, x_("\"),\n"));
//				}
//				xprintf(f, x_("\tNOSTRING};\n"));
//			}
//		}
//	
//		xprintf(f, x_("\nTECH_VARIABLES %s_variables[] =\n{\n"), us_tecedmakesymbol(techname));
//	
//		xprintf(f, x_("\t{x_(\"TECH_layer_names\"), (CHAR *)%s_layer_names, 0.0,\n"),
//			us_tecedmakesymbol(techname));
//		xprintf(f, x_("\t\tVSTRING|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},\n"));
//	
//		xprintf(f, x_("\t{x_(\"TECH_layer_function\"), (CHAR *)%s_layer_function, 0.0,\n"),
//			us_tecedmakesymbol(techname));
//		xprintf(f, x_("\t\tVINTEGER|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},\n"));
//	
//		xprintf(f, x_("\t{x_(\"TECH_node_width_offset\"), (CHAR *)%s_node_widoff, 0.0,\n"),
//			us_tecedmakesymbol(techname));
//		xprintf(f, x_("\t\tVFRACT|VDONTSAVE|VISARRAY|((NODEPROTOCOUNT*4)<<VLENGTHSH)},\n"));
//		if ((us_tecflags&HASGRAB) != 0 && us_tecnode_grabcount > 0)
//			xprintf(f, x_("\t{x_(\"prototype_center\"), (CHAR *)%s_centergrab, 0.0, %ld},\n"),
//				us_tecedmakesymbol(techname), us_tecnode_grabcount/3);
//	
//		if ((us_tecflags&HASMINNODE) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"DRC_min_node_size\"), (CHAR *)%s_node_minsize, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVFRACT|VDONTSAVE|VISARRAY|((NODEPROTOCOUNT*2)<<VLENGTHSH)},\n"));
//		}
//		if ((us_tecflags&HASMINNODER) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"DRC_min_node_size_rule\"), (CHAR *)%s_node_minsize_rule, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVSTRING|VDONTSAVE|VISARRAY|(NODEPROTOCOUNT<<VLENGTHSH)},\n"));
//		}
//	
//		if ((us_tecflags&HASARCWID) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"TECH_arc_width_offset\"), (CHAR *)%s_arc_widoff, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVFRACT|VDONTSAVE|VISARRAY|(ARCPROTOCOUNT<<VLENGTHSH)},\n"));
//		}
//	
//		if ((us_tecflags&HAS3DINFO) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"TECH_layer_3dthickness\"), (CHAR *)%s_3dthick_layers, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVINTEGER|VDONTSAVE|VISARRAY|(ARCPROTOCOUNT<<VLENGTHSH)},\n"));
//			xprintf(f, x_("\t{x_(\"TECH_layer_3dheight\"), (CHAR *)%s_3dheight_layers, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVINTEGER|VDONTSAVE|VISARRAY|(ARCPROTOCOUNT<<VLENGTHSH)},\n"));
//		}
//		if ((us_tecflags&HASPRINTCOL) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"USER_print_colors\"), (CHAR *)%s_printcolors_layers, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVINTEGER|VDONTSAVE|VISARRAY|((MAXLAYERS*5)<<VLENGTHSH)},\n"));
//		}
//	
//		xprintf(f, x_("\t{x_(\"USER_layer_letters\"), (CHAR *)%s_layer_letters, 0.0,\n"),
//			us_tecedmakesymbol(techname));
//		xprintf(f, x_("\t\tVSTRING|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},\n"));
//	
//		if ((us_tecflags&HASCOLORMAP) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"USER_color_map\"), (CHAR *)%s_colmap, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVCHAR|VDONTSAVE|VISARRAY|((sizeof %s_colmap)<<VLENGTHSH)},\n"),
//				us_tecedmakesymbol(techname));
//		}
//	
//		if ((us_tecflags&HASCIF) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"IO_cif_layer_names\"), (CHAR *)%s_cif_layers, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVSTRING|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},\n"));
//		}
//	
//		if ((us_tecflags&HASDXF) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"IO_dxf_layer_names\"), (CHAR *)%s_dxf_layers, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVSTRING|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},\n"));
//		}
//	
//		if ((us_tecflags&HASGDS) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"IO_gds_layer_numbers\"), (CHAR *)%s_gds_layers, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVSTRING|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},\n"));
//		}
//	
//		if ((us_tecflags&HASDRCMINWID) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"DRC_min_width\"), (CHAR *)%s_minimum_width, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVFRACT|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},\n"));
//			if ((us_tecflags&HASDRCMINWIDR) != 0)
//			{
//				xprintf(f, x_("\t{x_(\"DRC_min_width_rule\"), (CHAR *)%s_minimum_width_rule, 0.0,\n"),
//					us_tecedmakesymbol(techname));
//				xprintf(f, x_("\t\tVSTRING|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},\n"));
//			}
//		}
//		if ((us_tecflags&(HASCONDRCW|HASUNCONDRCW)) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"DRC_wide_limit\"), (CHAR *)%ld, 0.0,\n"),
//				us_tecdrc_rules.widelimit);
//			xprintf(f, x_("\t\tVFRACT|VDONTSAVE},\n"));
//		}
//	
//		if ((us_tecflags&HASCONDRC) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"DRC_min_connected_distances\"), (CHAR *)%s_connectedtable, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVFRACT|VDONTSAVE|VISARRAY|\n"));
//			xprintf(f, x_("\t\t\t(((sizeof %s_connectedtable)/SIZEOFINTBIG)<<VLENGTHSH)},\n"),
//				us_tecedmakesymbol(techname));
//			if ((us_tecflags&HASCONDRCR) != 0)
//			{
//				xprintf(f, x_("\t{x_(\"DRC_min_connected_distances_rule\"), (CHAR *)%s_connectedtable_rule, 0.0,\n"),
//					us_tecedmakesymbol(techname));
//				xprintf(f, x_("\t\tVSTRING|VDONTSAVE|VISARRAY|\n"));
//				xprintf(f, x_("\t\t\t(((sizeof %s_connectedtable_rule)/SIZEOFINTBIG)<<VLENGTHSH)},\n"),
//					us_tecedmakesymbol(techname));
//			}
//		}
//		if ((us_tecflags&HASUNCONDRC) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"DRC_min_unconnected_distances\"), (CHAR *)%s_unconnectedtable, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVFRACT|VDONTSAVE|VISARRAY|\n"));
//			xprintf(f, x_("\t\t   (((sizeof %s_unconnectedtable)/SIZEOFINTBIG)<<VLENGTHSH)},\n"),
//				us_tecedmakesymbol(techname));
//			if ((us_tecflags&HASUNCONDRCR) != 0)
//			{
//				xprintf(f, x_("\t{x_(\"DRC_min_unconnected_distances_rule\"), (CHAR *)%s_unconnectedtable_rule, 0.0,\n"),
//					us_tecedmakesymbol(techname));
//				xprintf(f, x_("\t\tVSTRING|VDONTSAVE|VISARRAY|\n"));
//				xprintf(f, x_("\t\t\t(((sizeof %s_unconnectedtable_rule)/SIZEOFINTBIG)<<VLENGTHSH)},\n"),
//					us_tecedmakesymbol(techname));
//			}
//		}
//	
//		if ((us_tecflags&HASCONDRCW) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"DRC_min_connected_distances_wide\"), (CHAR *)%s_connectedtable_wide, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVFRACT|VDONTSAVE|VISARRAY|\n"));
//			xprintf(f, x_("\t\t\t(((sizeof %s_connectedtable_wide)/SIZEOFINTBIG)<<VLENGTHSH)},\n"),
//				us_tecedmakesymbol(techname));
//			if ((us_tecflags&HASCONDRCWR) != 0)
//			{
//				xprintf(f, x_("\t{x_(\"DRC_min_connected_distances_wide_rule\"), (CHAR *)%s_connectedtable_wide_rule, 0.0,\n"),
//					us_tecedmakesymbol(techname));
//				xprintf(f, x_("\t\tVSTRING|VDONTSAVE|VISARRAY|\n"));
//				xprintf(f, x_("\t\t\t(((sizeof %s_connectedtable_wide_rule)/SIZEOFINTBIG)<<VLENGTHSH)},\n"),
//					us_tecedmakesymbol(techname));
//			}
//		}
//		if ((us_tecflags&HASUNCONDRCW) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"DRC_min_unconnected_distances_wide\"), (CHAR *)%s_unconnectedtable_wide, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVFRACT|VDONTSAVE|VISARRAY|\n"));
//			xprintf(f, x_("\t\t   (((sizeof %s_unconnectedtable_wide)/SIZEOFINTBIG)<<VLENGTHSH)},\n"),
//				us_tecedmakesymbol(techname));
//			if ((us_tecflags&HASUNCONDRCWR) != 0)
//			{
//				xprintf(f, x_("\t{x_(\"DRC_min_unconnected_distances_wide_rule\"), (CHAR *)%s_unconnectedtable_wide_rule, 0.0,\n"),
//					us_tecedmakesymbol(techname));
//				xprintf(f, x_("\t\tVSTRING|VDONTSAVE|VISARRAY|\n"));
//				xprintf(f, x_("\t\t\t(((sizeof %s_unconnectedtable_wide)/SIZEOFINTBIG)<<VLENGTHSH)},\n"),
//					us_tecedmakesymbol(techname));
//			}
//		}
//	
//		if ((us_tecflags&HASCONDRCM) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"DRC_min_connected_distances_multi\"), (CHAR *)%s_connectedtable_multi, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVFRACT|VDONTSAVE|VISARRAY|\n"));
//			xprintf(f, x_("\t\t\t(((sizeof %s_connectedtable_multi)/SIZEOFINTBIG)<<VLENGTHSH)},\n"),
//				us_tecedmakesymbol(techname));
//			if ((us_tecflags&HASCONDRCMR) != 0)
//			{
//				xprintf(f, x_("\t{x_(\"DRC_min_connected_distances_multi_rule\"), (CHAR *)%s_connectedtable_multi_rule, 0.0,\n"),
//					us_tecedmakesymbol(techname));
//				xprintf(f, x_("\t\tVSTRING|VDONTSAVE|VISARRAY|\n"));
//				xprintf(f, x_("\t\t\t(((sizeof %s_connectedtable_multi_rule)/SIZEOFINTBIG)<<VLENGTHSH)},\n"),
//					us_tecedmakesymbol(techname));
//			}
//		}
//		if ((us_tecflags&HASUNCONDRCM) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"DRC_min_unconnected_distances_multi\"), (CHAR *)%s_unconnectedtable_multi, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVFRACT|VDONTSAVE|VISARRAY|\n"));
//			xprintf(f, x_("\t\t   (((sizeof %s_unconnectedtable_multi)/SIZEOFINTBIG)<<VLENGTHSH)},\n"),
//				us_tecedmakesymbol(techname));
//			if ((us_tecflags&HASUNCONDRCMR) != 0)
//			{
//				xprintf(f, x_("\t{x_(\"DRC_min_unconnected_distances_multi_rule\"), (CHAR *)%s_unconnectedtable_multi_rule, 0.0,\n"),
//					us_tecedmakesymbol(techname));
//				xprintf(f, x_("\t\tVSTRING|VDONTSAVE|VISARRAY|\n"));
//				xprintf(f, x_("\t\t\t(((sizeof %s_unconnectedtable_multi)/SIZEOFINTBIG)<<VLENGTHSH)},\n"),
//					us_tecedmakesymbol(techname));
//			}
//		}
//		if ((us_tecflags&HASEDGEDRC) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"DRC_min_edge_distances\"), (CHAR *)%s_edgetable, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVFRACT|VDONTSAVE|VISARRAY|\n"));
//			xprintf(f, x_("\t\t   (((sizeof %s_edgetable)/SIZEOFINTBIG)<<VLENGTHSH)},\n"),
//				us_tecedmakesymbol(techname));
//			if ((us_tecflags&HASEDGEDRCR) != 0)
//			{
//				xprintf(f, x_("\t{x_(\"DRC_min_edge_distances_rule\"), (CHAR *)%s_edgetable_rule, 0.0,\n"),
//					us_tecedmakesymbol(techname));
//				xprintf(f, x_("\t\tVSTRING|VDONTSAVE|VISARRAY|\n"));
//				xprintf(f, x_("\t\t\t(((sizeof %s_edgetable)/SIZEOFINTBIG)<<VLENGTHSH)},\n"),
//					us_tecedmakesymbol(techname));
//			}
//		}
//	
//		if ((us_tecflags&HASSPIRES) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"SIM_spice_resistance\"), (CHAR *)%s_sim_spice_resistance, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVFLOAT|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},\n"));
//		}
//		if ((us_tecflags&HASSPICAP) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"SIM_spice_capacitance\"), (CHAR *)%s_sim_spice_capacitance, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVFLOAT|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},\n"));
//		}
//		if ((us_tecflags&HASSPIECAP) != 0)
//		{
//			xprintf(f, x_("\t{x_(\"SIM_spice_edge_capacitance\"), (CHAR *)%s_sim_spice_edge_cap, 0.0,\n"),
//				us_tecedmakesymbol(techname));
//			xprintf(f, x_("\t\tVFLOAT|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},\n"));
//		}
//	
//		// throw in pointers to any miscellaneous variables
//		for(i=0; us_knownvars[i].varname != 0; i++)
//		{
//			var = getval((INTBIG)tech, VTECHNOLOGY, -1, us_knownvars[i].varname);
//			if (var == NOVARIABLE) continue;
//			xprintf(f, x_("\t{x_(\"%s\"), "), us_knownvars[i].varname);
//			switch (var.type&(VTYPE|VISARRAY))
//			{
//				case VINTEGER:
//					xprintf(f, x_("(CHAR *)%ld, 0.0, VINTEGER|VDONTSAVE"), var.addr);
//					break;
//				case VFLOAT:
//					xprintf(f, x_("(CHAR *)0, %g, VFLOAT|VDONTSAVE"), castfloat(var.addr));
//					break;
//				case VSTRING:
//					xprintf(f, x_("x_(\"%s\"), 0.0, VSTRING|VDONTSAVE"), (CHAR *)var.addr);
//					break;
//				case VSTRING|VISARRAY:
//					xprintf(f, x_("(CHAR *)%s_%s, 0.0,\n\t\tVSTRING|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)"),
//						us_tecedmakesymbol(techname), us_knownvars[i].varname);
//					break;
//			}
//			xprintf(f, x_("},\n"));
//		}
//	
//		xprintf(f, x_("\t{NULL, NULL, 0.0, 0}\n};\n"));
//	}
//	
//	/*
//	 * routine to convert the multiplication and addition factors in "mul" and
//	 * "add" into proper constant names.  The "yaxis" is false for X and 1 for Y
//	 */
//	CHAR *us_tecededgelabel(INTBIG mul, INTBIG add, BOOLEAN yaxis)
//	{
//		CHAR line[20];
//		REGISTER INTBIG amt;
//		REGISTER void *infstr;
//	
//		infstr = initinfstr();
//	
//		// handle constant distance from center (handles halves up to 5.5)
//		if (mul == 0 && (add%H0) == 0 && abs(add) < K6)
//		{
//			addstringtoinfstr(infstr, x_("CENTER"));
//			if (add == 0) return(returninfstr(infstr));
//			if (!yaxis)
//			{
//				if (add < 0) addtoinfstr(infstr, 'L'); else addtoinfstr(infstr, 'R');
//			} else
//			{
//				if (add < 0) addtoinfstr(infstr, 'D'); else addtoinfstr(infstr, 'U');
//			}
//			amt = abs(add);
//			switch (amt%WHOLE)
//			{
//				case 0:  (void)esnprintf(line, 20, x_("%ld"),  amt/WHOLE);   break;
//				case H0: (void)esnprintf(line, 20, x_("%ldH"), amt/WHOLE);   break;
//			}
//			addstringtoinfstr(infstr, line);
//			return(returninfstr(infstr));
//		}
//	
//		// handle constant distance from edge (handles quarters up to 10, halves to 20)
//		if ((mul == H0 || mul == -H0) &&
//			(((add%Q0) == 0 && abs(add) < K10) || ((add%H0) == 0 && abs(add) < K20)))
//		{
//			if (!yaxis)
//			{
//				if (mul < 0) addstringtoinfstr(infstr, x_("LEFT")); else
//					addstringtoinfstr(infstr, x_("RIGHT"));
//			} else
//			{
//				if (mul < 0) addstringtoinfstr(infstr, x_("BOT")); else
//					addstringtoinfstr(infstr, x_("TOP"));
//			}
//			if (add == 0) addstringtoinfstr(infstr, x_("EDGE")); else
//			{
//				amt = abs(add);
//				switch (amt%WHOLE)
//				{
//					case 0:  (void)esnprintf(line, 20, x_("IN%ld"),  amt/WHOLE);   break;
//					case Q0: (void)esnprintf(line, 20, x_("IN%ldQ"), amt/WHOLE);   break;
//					case H0: (void)esnprintf(line, 20, x_("IN%ldH"), amt/WHOLE);   break;
//					case T0: (void)esnprintf(line, 20, x_("IN%ldT"), amt/WHOLE);   break;
//				}
//				addstringtoinfstr(infstr, line);
//			}
//			return(returninfstr(infstr));
//		}
//	
//		// generate two-value description
//		addstringtoinfstr(infstr, us_tecedmakefract(mul));
//		addtoinfstr(infstr, ',');
//		addstringtoinfstr(infstr, us_tecedmakefract(add));
//		return(returninfstr(infstr));
//	}
//	
//	/****************************** WRITE TECHNOLOGY AS "JAVA" CODE ******************************/
//	
//	/*
//	 * routine to dump the layer information in technology "tech" to the stream in
//	 * "f".
//	 */
//	void us_teceditdumpjavalayers(FILE *f, TECHNOLOGY *tech, CHAR *techname)
//	{
//		CHAR date[30], *transparent, *l1, *l2, *l3, *l4, *l5;
//		REGISTER INTBIG i, j, k, red, green, blue;
//		REGISTER void *infstr;
//		float r, c;
//		REGISTER BOOLEAN extrafunction;
//		REGISTER VARIABLE *varr, *varc, *rvar, *gvar, *bvar;
//	
//		// write legal banner
//		xprintf(f, x_("// BE SURE TO INCLUDE THIS TECHNOLOGY IN Technology.initAllTechnologies()\n\n"));
//		xprintf(f, x_("/* -*- tab-width: 4 -*-\n"));
//		xprintf(f, x_(" *\n"));
//		xprintf(f, x_(" * Electric(tm) VLSI Design System\n"));
//		xprintf(f, x_(" *\n"));
//		xprintf(f, x_(" * File: %s.java\n"), techname);
//		xprintf(f, x_(" * %s technology description\n"), techname);
//		xprintf(f, x_(" * Generated automatically from a library\n"));
//		xprintf(f, x_(" *\n"));
//		estrcpy(date, timetostring(getcurrenttime()));
//		date[24] = 0;
//		xprintf(f, x_(" * Copyright (c) %s Sun Microsystems and Static Free Software\n"), &date[20]);
//		xprintf(f, x_(" *\n"));
//		xprintf(f, x_(" * Electric(tm) is free software; you can redistribute it and/or modify\n"));
//		xprintf(f, x_(" * it under the terms of the GNU General Public License as published by\n"));
//		xprintf(f, x_(" * the Free Software Foundation; either version 2 of the License, or\n"));
//		xprintf(f, x_(" * (at your option) any later version.\n"));
//		xprintf(f, x_(" *\n"));
//		xprintf(f, x_(" * Electric(tm) is distributed in the hope that it will be useful,\n"));
//		xprintf(f, x_(" * but WITHOUT ANY WARRANTY; without even the implied warranty of\n"));
//		xprintf(f, x_(" * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"));
//		xprintf(f, x_(" * GNU General Public License for more details.\n"));
//		xprintf(f, x_(" *\n"));
//		xprintf(f, x_(" * You should have received a copy of the GNU General Public License\n"));
//		xprintf(f, x_(" * along with Electric(tm); see the file COPYING.  If not, write to\n"));
//		xprintf(f, x_(" * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,\n"));
//		xprintf(f, x_(" * Boston, Mass 02111-1307, USA.\n"));
//		xprintf(f, x_(" */\n"));
//		xprintf(f, x_("package com.sun.electric.technology.technologies;\n"));
//		xprintf(f, x_("\n"));
//	
//		// write header
//		xprintf(f, x_("import com.sun.electric.database.geometry.EGraphics;\n"));
//		xprintf(f, x_("import com.sun.electric.database.geometry.Poly;\n"));
//		xprintf(f, x_("import com.sun.electric.database.prototype.ArcProto;\n"));
//		xprintf(f, x_("import com.sun.electric.database.prototype.PortProto;\n"));
//		xprintf(f, x_("import com.sun.electric.database.prototype.NodeProto;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.DRCRules;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.EdgeH;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.EdgeV;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.Layer;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.PrimitiveArc;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.PrimitiveNode;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.PrimitivePort;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.SizeOffset;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.Technology;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.technologies.utils.MOSRules;\n"));
//		xprintf(f, x_("\n"));
//		xprintf(f, x_("import java.awt.Color;\n"));
//		xprintf(f, x_("\n"));
//	
//		xprintf(f, x_("/**\n"));
//		xprintf(f, x_(" * This is the %s Technology.\n"), tech.techdescript);
//		xprintf(f, x_(" */\n"));
//		xprintf(f, x_("public class %s extends Technology\n"), techname);
//		xprintf(f, x_("{\n"), techname);
//		xprintf(f, x_("\t/** the %s Technology object. */	public static final %s tech = new %s();\n"),
//			tech.techdescript, techname, techname);
//		if ((us_tecflags&(HASCONDRC|HASUNCONDRC)) != 0)
//		{
//			xprintf(f, x_("\tprivate static final double XX = -1;\n"));
//			xprintf(f, x_("\tprivate double [] conDist, unConDist;\n"));
//		}
//		xprintf(f, x_("\n"));
//	
//		xprintf(f, x_("\t// -------------------- private and protected methods ------------------------\n"));
//		xprintf(f, x_("\tprivate %s()\n"), techname);
//		xprintf(f, x_("\t{\n"));
//		xprintf(f, x_("\t\tsuper(\"%s\");\n"), techname);
//		xprintf(f, x_("\t\tsetTechDesc(\"%s\");\n"), tech.techdescript);
//		xprintf(f, x_("\t\tsetFactoryScale(%ld, true);   // in nanometers: really %g microns\n"),
//			tech.deflambda / 2, (float)tech.deflambda / 2000.0);
//		xprintf(f, x_("\t\tsetNoNegatedArcs();\n"));
//		xprintf(f, x_("\t\tsetStaticTechnology();\n"));
//	
//		// write the color map
//		if ((us_tecflags&HASCOLORMAP) != 0)
//		{
//			// determine the five transparent layers
//			l1 = x_("layer 1");
//			l2 = x_("layer 2");
//			l3 = x_("layer 3");
//			l4 = x_("layer 4");
//			l5 = x_("layer 5");
//			for(i=0; i<tech.layercount; i++)
//			{
//				if (tech.layers[i].bits == LAYERT1 && l1 == 0)
//					l1 = us_teclayer_names[i]; else
//				if (tech.layers[i].bits == LAYERT2 && l2 == 0)
//					l2 = us_teclayer_names[i]; else
//				if (tech.layers[i].bits == LAYERT3 && l3 == 0)
//					l3 = us_teclayer_names[i]; else
//				if (tech.layers[i].bits == LAYERT4 && l4 == 0)
//					l4 = us_teclayer_names[i]; else
//				if (tech.layers[i].bits == LAYERT5 && l5 == 0)
//					l5 = us_teclayer_names[i];
//			}
//			xprintf(f, x_("\t\tsetFactoryTransparentLayers(new Color []\n"));
//			xprintf(f, x_("\t\t{\n"));
//			xprintf(f, x_("\t\t\tnew Color(%3d,%3d,%3d), // %s\n"),
//				us_teccolmap[1].red, us_teccolmap[1].green, us_teccolmap[1].blue, l1);
//			xprintf(f, x_("\t\t\tnew Color(%3d,%3d,%3d), // %s\n"),
//				us_teccolmap[2].red, us_teccolmap[2].green, us_teccolmap[2].blue, l2);
//			xprintf(f, x_("\t\t\tnew Color(%3d,%3d,%3d), // %s\n"),
//				us_teccolmap[4].red, us_teccolmap[4].green, us_teccolmap[4].blue, l3);
//			xprintf(f, x_("\t\t\tnew Color(%3d,%3d,%3d), // %s\n"),
//				us_teccolmap[8].red, us_teccolmap[8].green, us_teccolmap[8].blue, l4);
//			xprintf(f, x_("\t\t\tnew Color(%3d,%3d,%3d), // %s\n"),
//				us_teccolmap[16].red, us_teccolmap[16].green, us_teccolmap[16].blue, l5);
//			xprintf(f, x_("\t\t});\n"));
//		}
//		xprintf(f, x_("\n"));
//	
//		// write the layer declarations
//		xprintf(f, x_("\t\t//**************************************** LAYERS ****************************************\n\n"));
//		for(i=0; i<tech.layercount; i++)
//		{
//			xprintf(f, x_("\t\t/** %s layer */\n"), us_teclayer_iname[i]);
//			xprintf(f, x_("\t\tLayer %s_lay = Layer.newInstance(this, \"%s\",\n"), us_teclayer_iname[i],
//				us_teclayer_names[i]);
//			xprintf(f, x_("\t\t\tnew EGraphics("));
//			if ((tech.layers[i].colstyle&NATURE) == SOLIDC) xprintf(f, x_("EGraphics.SOLID")); else
//			{
//				if ((tech.layers[i].colstyle&OUTLINEPAT) == 0)
//					xprintf(f, x_("EGraphics.PATTERNED")); else
//						xprintf(f, x_("EGraphics.OUTLINEPAT"));
//			}
//			xprintf(f, x_(", "));
//			if ((tech.layers[i].bwstyle&NATURE) == SOLIDC) xprintf(f, x_("EGraphics.SOLID")); else
//			{
//				if ((tech.layers[i].bwstyle&OUTLINEPAT) == 0)
//					xprintf(f, x_("EGraphics.PATTERNED")); else
//						xprintf(f, x_("EGraphics.OUTLINEPAT"));
//			}
//			transparent = "0";
//			switch (tech.layers[i].bits)
//			{
//				case LAYERT1: transparent = "EGraphics.TRANSPARENT_1";   break;
//				case LAYERT2: transparent = "EGraphics.TRANSPARENT_2";   break;
//				case LAYERT3: transparent = "EGraphics.TRANSPARENT_3";   break;
//				case LAYERT4: transparent = "EGraphics.TRANSPARENT_4";   break;
//				case LAYERT5: transparent = "EGraphics.TRANSPARENT_5";   break;
//			}
//			if (tech.layers[i].bits == LAYERO)
//			{
//				rvar = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER|VISARRAY, us_colormap_red_key);
//				gvar = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER|VISARRAY, us_colormap_green_key);
//				bvar = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER|VISARRAY, us_colormap_blue_key);
//				if (rvar != NOVARIABLE && gvar != NOVARIABLE && bvar != NOVARIABLE)
//				{
//					red = ((INTBIG *)rvar.addr)[tech.layers[i].col];
//					green = ((INTBIG *)gvar.addr)[tech.layers[i].col];
//					blue = ((INTBIG *)bvar.addr)[tech.layers[i].col];
//				}
//			} else
//			{
//				red = us_teccolmap[tech.layers[i].col].red;
//				green = us_teccolmap[tech.layers[i].col].green;
//				blue = us_teccolmap[tech.layers[i].col].blue;
//			}
//			if (red < 0 || red > 255) red = 0;
//			if (green < 0 || green > 255) green = 0;
//			if (blue < 0 || blue > 255) blue = 0;
//			xprintf(f, x_(", %s, %ld,%ld,%ld, 0.8,true,\n"), transparent, red, green, blue);
//	
//			for(j=0; j<16; j++) if (tech.layers[i].raster[j] != 0) break;
//			if (j >= 16)
//				xprintf(f, x_("\t\t\tnew int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));\n\n")); else
//			{
//				for(j=0; j<16; j++)
//				{
//					xprintf(f, x_("\t\t\t"));
//					if (j == 0) xprintf(f, x_("new int[] { ")); else
//						xprintf(f, x_("\t\t\t"));
//					xprintf(f, x_("0x%04x"), tech.layers[i].raster[j]&0xFFFF);
//					if (j == 15) xprintf(f, x_("}));")); else
//						xprintf(f, x_(",   "));
//	
//					xprintf(f, x_("// "));
//					for(k=0; k<16; k++)
//						if ((tech.layers[i].raster[j] & (1 << (15-k))) != 0)
//							xprintf(f, x_("X")); else xprintf(f, x_(" "));
//					xprintf(f, x_("\n"));
//				}
//				xprintf(f, x_("\n"));
//			}
//		}
//	
//		// write the layer functions
//		xprintf(f, x_("\t\t// The layer functions\n"));
//		for(i=0; i<tech.layercount; i++)
//		{
//			k = us_teclayer_function[i];
//			infstr = initinfstr();
//			formatinfstr(infstr, x_("%s_lay.setFunction(Layer.Function."), us_teclayer_iname[i]);
//			if ((k&(LFTYPE|LFPTYPE)) == (LFDIFF|LFPTYPE))
//			{
//				addstringtoinfstr(infstr, "DIFFP");
//				k &= ~LFPTYPE;
//			} else if ((k&(LFTYPE|LFNTYPE)) == (LFDIFF|LFNTYPE))
//			{
//				addstringtoinfstr(infstr, "DIFFN");
//				k &= ~LFNTYPE;
//			} else if ((k&(LFTYPE|LFPTYPE)) == (LFWELL|LFPTYPE))
//			{
//				addstringtoinfstr(infstr, "WELLP");
//				k &= ~LFPTYPE;
//			} else if ((k&(LFTYPE|LFNTYPE)) == (LFWELL|LFNTYPE))
//			{
//				addstringtoinfstr(infstr, "WELLN");
//				k &= ~LFNTYPE;
//			} else if ((k&(LFTYPE|LFPTYPE)) == (LFIMPLANT|LFPTYPE))
//			{
//				addstringtoinfstr(infstr, "IMPLANTP");
//				k &= ~LFPTYPE;
//			} else if ((k&(LFTYPE|LFNTYPE)) == (LFIMPLANT|LFNTYPE))
//			{
//				addstringtoinfstr(infstr, "IMPLANTN");
//				k &= ~LFNTYPE;
//			} else if ((k&(LFTYPE|LFINTRANS)) == (LFPOLY1|LFINTRANS))
//			{
//				addstringtoinfstr(infstr, "GATE");
//				k &= ~LFINTRANS;
//			} else
//			{
//				addstringtoinfstr(infstr, &us_teclayer_functions[k&LFTYPE].constant[2]);
//			}
//			extrafunction = FALSE;
//			for(j=0; us_teclayer_functions[j].name != 0; j++)
//			{
//				if (us_teclayer_functions[j].value <= LFTYPE) continue;
//				if ((k&us_teclayer_functions[j].value) != 0)
//				{
//					if (extrafunction) addstringtoinfstr(infstr, "|"); else
//						addstringtoinfstr(infstr, ", ");
//					addstringtoinfstr(infstr, "Layer.Function.");
//					addstringtoinfstr(infstr, &us_teclayer_functions[j].constant[2]);
//					extrafunction = TRUE;
//				}
//			}
//			addstringtoinfstr(infstr, ");");
//			xprintf(f, x_("\t\t%s"), returninfstr(infstr));
//			xprintf(f, x_("\t\t// %s\n"), us_teclayer_names[i]);
//		}
//	
//		// write the CIF layer names
//		if ((us_tecflags&HASCIF) != 0)
//		{
//			xprintf(f, x_("\n\t\t// The CIF names\n"));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\t\t%s_lay.setFactoryCIFLayer(\""), us_teclayer_iname[i]);
//				if (us_teccif_layers[i] != 0) xprintf(f, x_("%s"), us_teccif_layers[i]);
//				xprintf(f, x_("\");\t\t// %s\n"), us_teclayer_names[i]);
//			}
//		}
//	
//		// write the DXF layer numbers
//		if ((us_tecflags&HASDXF) != 0)
//		{
//			xprintf(f, x_("\n\t\t// The DXF names\n"));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\t\t%s_lay.setFactoryDXFLayer(\""), us_teclayer_iname[i]);
//				xprintf(f, x_("%s"), us_tecdxf_layers[i]);
//				xprintf(f, x_("\");\t\t// %s\n"), us_teclayer_names[i]);
//			}
//		}
//	
//		// write the Calma GDS-II layer number
//		if ((us_tecflags&HASGDS) != 0)
//		{
//			xprintf(f, x_("\n\t\t// The GDS names\n"));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\t\t%s_lay.setFactoryGDSLayer(\""), us_teclayer_iname[i]);
//				xprintf(f, x_("%s"), us_tecgds_layers[i]);
//				xprintf(f, x_("\");\t\t// %s\n"), us_teclayer_names[i]);
//			}
//		}
//	
//		// write the 3D information
//		if ((us_tecflags&HAS3DINFO) != 0)
//		{
//			xprintf(f, x_("\n\t\t// The layer height\n"));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\t\t%s_lay.setFactory3DInfo("), us_teclayer_iname[i]);
//				xprintf(f, x_("%ld, %ld"), us_tec3d_thickness[i], us_tec3d_height[i]);
//				xprintf(f, x_(");\t\t// %s\n"), us_teclayer_names[i]);
//			}
//		}
//	
//		// write the SPICE information
//		if ((us_tecflags&(HASSPIRES|HASSPICAP|HASSPIECAP)) != 0)
//		{
//			xprintf(f, x_("\n\t\t// The SPICE information\n"));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\t\t%s_lay.setFactoryParasitics("), us_teclayer_iname[i]);
//				if ((us_tecflags&HASSPIRES) == 0) xprintf(f, x_("0, ")); else
//					xprintf(f, x_("%s, "), us_tecedmakefloatstring(us_tecspice_res[i]));
//				if ((us_tecflags&HASSPICAP) == 0) xprintf(f, x_("0, ")); else
//					xprintf(f, x_("%s, "), us_tecedmakefloatstring(us_tecspice_cap[i]));
//				if ((us_tecflags&HASSPIECAP) == 0) xprintf(f, x_("0")); else
//					xprintf(f, x_("%s"), us_tecedmakefloatstring(us_tecspice_ecap[i]));
//				xprintf(f, x_(");\t\t// %s\n"), us_teclayer_names[i]);
//			}
//		}
//		varr = getval((INTBIG)tech, VTECHNOLOGY, -1, x_("SIM_spice_min_resistance"));
//		varc = getval((INTBIG)tech, VTECHNOLOGY, -1, x_("SIM_spice_min_capacitance"));
//		if (varr != NOVARIABLE || varc != NOVARIABLE)
//		{
//			if (varr != NOVARIABLE) r = castfloat(varr.addr); else r = 0.0;
//			if (varc != NOVARIABLE) c = castfloat(varr.addr); else c = 0.0;
//	        xprintf(f, x_("\t\tsetFactoryParasitics(%g, %g);\n"), r, c);
//		}
//	
//		// write design rules
//		if ((us_tecflags&(HASCONDRC|HASUNCONDRC)) != 0)
//		{
//			xprintf(f, x_("\n\t\t//******************** DESIGN RULES ********************\n"));
//	
//			if ((us_tecflags&HASCONDRC) != 0)
//			{
//				xprintf(f, x_("\n\t\tconDist = new double[] {\n"));
//				us_teceditdumpjavadrctab(f, us_tecdrc_rules.conlist, tech, FALSE);
//			}
//			if ((us_tecflags&HASUNCONDRC) != 0)
//			{
//				xprintf(f, x_("\n\t\tunConDist = new double[] {\n"));
//				us_teceditdumpjavadrctab(f, us_tecdrc_rules.unconlist, tech, FALSE);
//			}
//		}
//	}
//	
//	void us_teceditdumpjavadrctab(FILE *f, void *distances, TECHNOLOGY *tech, BOOLEAN isstring)
//	{
//		REGISTER INTBIG i, j;
//		REGISTER INTBIG amt, *amtlist;
//		CHAR shortname[7], *msg, **distlist;
//	
//		for(i=0; i<6; i++)
//		{
//			xprintf(f, x_("\t\t\t//            "));
//			for(j=0; j<tech.layercount; j++)
//			{
//				if ((INTBIG)estrlen(us_teclayer_iname[j]) <= i) xprintf(f, x_(" ")); else
//					xprintf(f, x_("%c"), us_teclayer_iname[j][i]);
//				xprintf(f, x_("  "));
//			}
//			xprintf(f, x_("\n"));
//		}
//		if (isstring) distlist = (CHAR **)distances; else
//			amtlist = (INTBIG *)distances;
//		for(j=0; j<tech.layercount; j++)
//		{
//			(void)estrncpy(shortname, us_teclayer_iname[j], 6);
//			shortname[6] = 0;
//			xprintf(f, x_("\t\t\t/* %-6s */ "), shortname);
//			for(i=0; i<j; i++) xprintf(f, x_("   "));
//			for(i=j; i<tech.layercount; i++)
//			{
//				if (isstring)
//				{
//					msg = *distlist++;
//					xprintf(f, x_("x_(\"%s\")"), msg);
//				} else
//				{
//					amt = *amtlist++;
//					if (amt < 0) xprintf(f, x_("XX")); else
//					{
//						xprintf(f, x_("%g"), (float)amt/WHOLE);
//					}
//				}
//				if (j != tech.layercount-1 || i != tech.layercount-1)
//					xprintf(f, x_(","));
//			}
//			xprintf(f, x_("\n"));
//		}
//		xprintf(f, x_("\t\t};\n"));
//	}
//	
//	/*
//	 * routine to dump the arc information in technology "tech" to the stream in
//	 * "f".
//	 */
//	void us_teceditdumpjavaarcs(FILE *f, TECHNOLOGY *tech, CHAR *techname)
//	{
//		REGISTER INTBIG i, j, k;
//	
//		// print the header
//		xprintf(f, x_("\n\t\t//******************** ARCS ********************\n"));
//	
//		// now write the arcs
//		for(i=0; i<tech.arcprotocount; i++)
//		{
//			xprintf(f, x_("\n\t\t/** %s arc */\n"), tech.arcprotos[i].arcname);
//			xprintf(f, x_("\t\tPrimitiveArc %s_arc = PrimitiveArc.newInstance(this, \"%s\", %g, new Technology.ArcLayer []\n"),
//				us_teceditconverttojava(tech.arcprotos[i].arcname), tech.arcprotos[i].arcname, (float)tech.arcprotos[i].arcwidth/WHOLE);
//			xprintf(f, x_("\t\t{\n"));
//			for(k=0; k<tech.arcprotos[i].laycount; k++)
//			{
//				xprintf(f, x_("\t\t\tnew Technology.ArcLayer(%s_lay, "),
//					us_teclayer_iname[tech.arcprotos[i].list[k].lay]);
//				if (tech.arcprotos[i].list[k].off == 0) xprintf(f, x_("0,")); else
//					xprintf(f, x_("%g,"), (float)tech.arcprotos[i].list[k].off/WHOLE);
//				if (tech.arcprotos[i].list[k].style == FILLED) xprintf(f, x_(" Poly.Type.FILLED)")); else
//					xprintf(f, x_(" Poly.Type.CLOSED)"));
//				if (k+1 < tech.arcprotos[i].laycount) xprintf(f, x_(","));
//				xprintf(f, x_("\n"));
//			}
//			xprintf(f, x_("\t\t});\n"));
//			for(j=0; us_tecarc_functions[j].name != 0; j++)
//				if (us_tecarc_functions[j].value ==
//					(INTBIG)((tech.arcprotos[i].initialbits&AFUNCTION)>>AFUNCTIONSH))
//			{
//				xprintf(f, x_("\t\t%s_arc.setFunction(PrimitiveArc.Function.%s);\n"),
//					us_teceditconverttojava(tech.arcprotos[i].arcname), &us_tecarc_functions[j].constant[2]);
//				break;
//			}
//			if (us_tecarc_functions[j].name == 0)
//				xprintf(f, x_("\t\t%s_arc.setFunction(PrimitiveArc.Function.UNKNOWN);\n"), us_teceditconverttojava(tech.arcprotos[i].arcname));
//			if ((tech.arcprotos[i].initialbits&CANWIPE) != 0)
//				xprintf(f, x_("\t\t%s_arc.setWipable();\n"), us_teceditconverttojava(tech.arcprotos[i].arcname));
//			if ((us_tecflags&HASARCWID) != 0 && us_tecarc_widoff[i] != 0)
//			{
//				xprintf(f, x_("\t\t%s_arc.setWidthOffset(%ld);\n"), us_teceditconverttojava(tech.arcprotos[i].arcname),
//					(float)us_tecarc_widoff[i]/WHOLE);
//			}
//	
//			if ((tech.arcprotos[i].initialbits&WANTFIXANG) != 0)
//				xprintf(f, x_("\t\t%s_arc.setFactoryFixedAngle(true);\n"), us_teceditconverttojava(tech.arcprotos[i].arcname));
//			if ((tech.arcprotos[i].initialbits&WANTNOEXTEND) != 0)
//				xprintf(f, x_("\t\t%s_arc.setFactoryExtended(false);\n"), us_teceditconverttojava(tech.arcprotos[i].arcname));
//			xprintf(f, x_("\t\t%s_arc.setFactoryAngleIncrement(%ld);\n"), us_teceditconverttojava(tech.arcprotos[i].arcname),
//				(tech.arcprotos[i].initialbits&AANGLEINC)>>AANGLEINCSH);
//	
//		}
//	}
//	
//	/*
//	 * routine to dump the node information in technology "tech" to the stream in
//	 * "f".
//	 */
//	void us_teceditdumpjavanodes(FILE *f, TECHNOLOGY *tech, CHAR *techname)
//	{
//		REGISTER RULE *r;
//		REGISTER INTBIG i, j, k, l, tot;
//		CHAR *ab;
//		REGISTER PCON *pc;
//		REGISTER BOOLEAN yaxis;
//		REGISTER TECH_POLYGON *plist;
//		REGISTER TECH_NODES *nlist;
//		REGISTER void *infstr;
//	
//		// make abbreviations for each node
//		for(i=0; i<tech.nodeprotocount; i++)
//		{
//			(void)allocstring(&ab, makeabbrev(tech.nodeprotos[i].nodename, FALSE), el_tempcluster);
//			tech.nodeprotos[i].creation = (NODEPROTO *)ab;
//	
//			// loop until the name is unique
//			for(;;)
//			{
//				// see if a previously assigned abbreviation is the same
//				for(j=0; j<i; j++)
//					if (namesame(ab, (CHAR *)tech.nodeprotos[j].creation) == 0) break;
//				if (j == i) break;
//	
//				// name conflicts: change it
//				l = estrlen(ab);
//				if (ab[l-1] >= '0' && ab[l-1] <= '8') ab[l-1]++; else
//				{
//					infstr = initinfstr();
//					addstringtoinfstr(infstr, ab);
//					addtoinfstr(infstr, '0');
//					(void)reallocstring(&ab, returninfstr(infstr), el_tempcluster);
//					tech.nodeprotos[i].creation = (NODEPROTO *)ab;
//				}
//			}
//		}
//	
//		xprintf(f, x_("\n\t\t//******************** RECTANGLE DESCRIPTIONS ********************"));
//		xprintf(f, x_("\n\n"));
//	
//		// print box information
//		i = 1;
//		for(r = us_tecedfirstrule; r != NORULE; r = r.nextrule)
//		{
//			if (!r.used) continue;
//			r.rindex = i++;
//			xprintf(f, x_("\t\tTechnology.TechPoint [] box_%ld = new Technology.TechPoint[] {\n"),
//				r.rindex);
//			for(j=0; j<r.count; j += 2)
//			{
//				if ((j%4) == 0)
//				{
//					yaxis = FALSE;
//					xprintf(f, x_("\t\t\tnew Technology.TechPoint("));
//				} else
//				{
//					yaxis = TRUE;
//				}
//				xprintf(f, x_("%s"), us_tecededgelabeljava(r.value[j], r.value[j+1], yaxis));
//				if ((j%4) == 0) xprintf(f, x_(", ")); else
//				{
//					xprintf(f, x_(")"));
//					if (j+1 < r.count) xprintf(f, x_(","));
//					xprintf(f, x_("\n"));
//				}
//			}
//			xprintf(f, x_("\t\t};\n"));
//		}
//	
//		xprintf(f, x_("\n\t\t//******************** NODES ********************\n"));
//	
//		// print node information
//		for(i=0; i<tech.nodeprotocount; i++)
//		{
//			// header comment
//			nlist = tech.nodeprotos[i];
//			ab = (CHAR *)nlist.creation;
//			xprintf(f, x_("\n\t\t/** %s */\n"), nlist.nodename);
//	
//			xprintf(f, x_("\t\tPrimitiveNode %s_node = PrimitiveNode.newInstance(\"%s\", this, %g, %g, "),
//				ab, nlist.nodename, (float)nlist.xsize/WHOLE, (float)nlist.ysize/WHOLE);
//			if (us_tecnode_widoff[i*4] != 0 || us_tecnode_widoff[i*4+1] != 0 ||
//				us_tecnode_widoff[i*4+2] != 0 || us_tecnode_widoff[i*4+3] != 0)
//			{
//				xprintf(f, x_("new SizeOffset(%g, %g, %g, %g),\n"),
//					(float)us_tecnode_widoff[i*4] / WHOLE, (float)us_tecnode_widoff[i*4+1] / WHOLE,
//					(float)us_tecnode_widoff[i*4+2] / WHOLE, (float)us_tecnode_widoff[i*4+3] / WHOLE);
//			} else
//			{
//				xprintf(f, x_("null,\n"));
//			}
//	
//			// print layers
//			xprintf(f, x_("\t\t\tnew Technology.NodeLayer []\n"));
//			xprintf(f, x_("\t\t\t{\n"));
//			tot = nlist.layercount;
//			for(j=0; j<tot; j++)
//			{
//				if (nlist.special == SERPTRANS) plist = &nlist.gra[j].basics; else
//					plist = &nlist.layerlist[j];
//				xprintf(f, x_("\t\t\t\tnew Technology.NodeLayer(%s_lay, %ld, Poly.Type."),
//					us_teclayer_iname[plist.layernum], plist.portnum);
//				switch (plist.style)
//				{
//					case FILLEDRECT:     xprintf(f, x_("FILLED,"));         break;
//					case CLOSEDRECT:     xprintf(f, x_("CLOSED,"));         break;
//					case CROSSED:        xprintf(f, x_("CROSSED,"));        break;
//					case FILLED:         xprintf(f, x_("FILLED,"));         break;
//					case CLOSED:         xprintf(f, x_("CLOSED,"));         break;
//					case OPENED:         xprintf(f, x_("OPENED,"));         break;
//					case OPENEDT1:       xprintf(f, x_("OPENEDT1,"));       break;
//					case OPENEDT2:       xprintf(f, x_("OPENEDT2,"));       break;
//					case OPENEDT3:       xprintf(f, x_("OPENEDT3,"));       break;
//					case VECTORS:        xprintf(f, x_("VECTORS,"));        break;
//					case CIRCLE:         xprintf(f, x_("CIRCLE,"));         break;
//					case THICKCIRCLE:    xprintf(f, x_("THICKCIRCLE,"));    break;
//					case DISC:           xprintf(f, x_("DISC,"));           break;
//					case CIRCLEARC:      xprintf(f, x_("CIRCLEARC,"));      break;
//					case THICKCIRCLEARC: xprintf(f, x_("THICKCIRCLEARC,")); break;
//					case TEXTCENT:       xprintf(f, x_("TEXTCENT,"));       break;
//					case TEXTTOP:        xprintf(f, x_("TEXTTOP,"));        break;
//					case TEXTBOT:        xprintf(f, x_("TEXTBOT,"));        break;
//					case TEXTLEFT:       xprintf(f, x_("TEXTLEFT,"));       break;
//					case TEXTRIGHT:      xprintf(f, x_("TEXTRIGHT,"));      break;
//					case TEXTTOPLEFT:    xprintf(f, x_("TEXTTOPLEFT,"));    break;
//					case TEXTBOTLEFT:    xprintf(f, x_("TEXTBOTLEFT,"));    break;
//					case TEXTTOPRIGHT:   xprintf(f, x_("TEXTTOPRIGHT,"));   break;
//					case TEXTBOTRIGHT:   xprintf(f, x_("TEXTBOTRIGHT,"));   break;
//					case TEXTBOX:        xprintf(f, x_("TEXTBOX,"));        break;
//					default:             xprintf(f, x_("????,"));           break;
//				}
//				switch (plist.representation)
//				{
//					case BOX:    xprintf(f, x_(" Technology.NodeLayer.BOX,"));     break;
//					case MINBOX: xprintf(f, x_(" Technology.NodeLayer.MINBOX,"));  break;
//					case POINTS: xprintf(f, x_(" Technology.NodeLayer.POINTS,"));  break;
//					default:     xprintf(f, x_(" Technology.NodeLayer.????,"));    break;
//				}
//				for(r = us_tecedfirstrule; r != NORULE; r = r.nextrule)
//					if (r.value == plist.points) break;
//				if (r != NORULE)
//				{
//					xprintf(f, x_(" box_%ld"), r.rindex);
//				} else
//					xprintf(f, x_(" box??"));
//				if (nlist.special == SERPTRANS)
//				{
//					xprintf(f, x_(", %g, %g, %g, %g"),
//						nlist.gra[j].lwidth / (float)WHOLE, nlist.gra[j].rwidth / (float)WHOLE,
//						nlist.gra[j].extendb / (float)WHOLE, nlist.gra[j].extendt / (float)WHOLE);
//				}
//				xprintf(f, x_(")"));
//				if (j+1 < tot) xprintf(f, x_(","));
//				xprintf(f, x_("\n"));
//			}
//			xprintf(f, x_("\t\t\t});\n"));
//	
//			// print ports
//			xprintf(f, x_("\t\t%s_node.addPrimitivePorts(new PrimitivePort[]\n"), ab);
//			xprintf(f, x_("\t\t\t{\n"));
//			for(j=0; j<nlist.portcount; j++)
//			{
//				xprintf(f, x_("	\t\t\tPrimitivePort.newInstance(this, %s_node, new ArcProto [] {"), ab);
//				for(pc = us_tecedfirstpcon; pc != NOPCON; pc = pc.nextpcon)
//					if (pc.connects == nlist.portlist[j].portarcs) break;
//				if (pc != NOPCON)
//				{
//					for(l=0; l<pc.total; l++)
//					{
//						k = pc.connects[l+1];
//						xprintf(f, x_("%s_arc"), us_teceditconverttojava(tech.arcprotos[k].arcname));
//						if (l+1 < pc.total) xprintf(f, x_(", "));
//					}
//				}
//				xprintf(f, x_("}, \"%s\", %ld,%ld, %ld, PortCharacteristic.UNKNOWN,\n"),
//					nlist.portlist[j].protoname,
//					(nlist.portlist[j].initialbits&PORTANGLE)>>PORTANGLESH,
//					(nlist.portlist[j].initialbits&PORTARANGE)>>PORTARANGESH,
//					(nlist.portlist[j].initialbits&PORTNET)>>PORTNETSH);
//				xprintf(f, x_("\t\t\t\t\t%s, %s, %s, %s)"),
//					us_tecededgelabeljava(nlist.portlist[j].lowxmul, nlist.portlist[j].lowxsum, FALSE),
//					us_tecededgelabeljava(nlist.portlist[j].lowymul, nlist.portlist[j].lowysum, TRUE),
//					us_tecededgelabeljava(nlist.portlist[j].highxmul, nlist.portlist[j].highxsum, FALSE),
//					us_tecededgelabeljava(nlist.portlist[j].highymul, nlist.portlist[j].highysum, TRUE));
//	
//				if (j+1 < nlist.portcount) xprintf(f, x_(","));
//				xprintf(f, x_("\n"));
//			}
//			xprintf(f, x_("\t\t\t});\n"));
//	
//			// print the node information
//			j = (nlist.initialbits&NFUNCTION)>>NFUNCTIONSH;
//			if (j < 0 || j >= MAXNODEFUNCTION) j = 0;
//			xprintf(f, x_("\t\t%s_node.setFunction(PrimitiveNode.Function.%s);\n"), ab, &nodefunctionconstantname(j)[2]);
//	
//			if ((nlist.initialbits&WIPEON1OR2) != 0)
//				xprintf(f, x_("\t\t%s_node.setWipeOn1or2();\n"), ab);
//			if ((nlist.initialbits&HOLDSTRACE) != 0)
//				xprintf(f, x_("\t\t%s_node.setHoldsOutline();\n"), ab);
//			if ((nlist.initialbits&NSQUARE) != 0)
//				xprintf(f, x_("\t\t%s_node.setSquare();\n"), ab);
//			if ((nlist.initialbits&ARCSWIPE) != 0)
//				xprintf(f, x_("\t\t%s_node.setArcsWipe();\n"), ab);
//			if ((nlist.initialbits&ARCSHRINK) != 0)
//				xprintf(f, x_("\t\t%s_node.setArcsShrink();\n"), ab);
//			if ((nlist.initialbits&NODESHRINK) != 0)
//				xprintf(f, x_("\t\t%s_node.setCanShrink();\n"), ab);
//			if ((nlist.initialbits&LOCKEDPRIM) != 0)
//				xprintf(f, x_("\t\t%s_node.setLockedPrim();\n"), ab);
//			if (nlist.special != 0)
//			{
//				switch (nlist.special)
//				{
//					case SERPTRANS:
//						xprintf(f, x_("\t\t%s_node.setSpecialType(PrimitiveNode.SERPTRANS);\n"), ab);
//						xprintf(f, x_("\t\t%s_node.setSpecialValues(new double [] {%g, %g, %g, %g, %g, %g});\n"),
//							ab, (float)nlist.f1/WHOLE, (float)nlist.f2/WHOLE, (float)nlist.f3/WHOLE,
//								(float)nlist.f4/WHOLE, (float)nlist.f5/WHOLE, (float)nlist.f6/WHOLE);
//						break;
//					case POLYGONAL:
//						xprintf(f, x_("\t\t%s_node.setSpecialType(PrimitiveNode.POLYGONAL);\n"), ab);
//						break;
//					case MULTICUT:
//						xprintf(f, x_("\t\t%s_node.setSpecialType(PrimitiveNode.MULTICUT);\n"), ab);
//						xprintf(f, x_("\t\t%s_node.setSpecialValues(new double [] {%g, %g, %g, %g, %g, %g});\n"),
//							ab, (float)nlist.f1/WHOLE, (float)nlist.f2/WHOLE,
//								(float)nlist.f3/WHOLE, (float)nlist.f3/WHOLE,
//								(float)nlist.f4/WHOLE, (float)nlist.f4/WHOLE);
//						break;
//				}
//			}
//		}
//	
//		// write the pure-layer associations
//		xprintf(f, x_("\n\t\t// The pure layer nodes\n"));
//		for(i=0; i<tech.layercount; i++)
//		{
//			if ((us_teclayer_function[i]&LFPSEUDO) != 0) continue;
//	
//			// find the pure layer node
//			for(j=0; j<tech.nodeprotocount; j++)
//			{
//				nlist = tech.nodeprotos[j];
//				if (((nlist.initialbits&NFUNCTION)>>NFUNCTIONSH) != NPNODE) continue;
//				plist = &nlist.layerlist[0];
//				if (plist.layernum == i) break;
//			}
//			if (j >= tech.nodeprotocount) continue;
//			ab = (CHAR *)tech.nodeprotos[j].creation;
//			xprintf(f, x_("\t\t%s_lay.setPureLayerNode("), us_teclayer_iname[i]);
//			xprintf(f, x_("%s_node"), ab);
//			xprintf(f, x_(");\t\t// %s\n"), us_teclayer_names[i]);
//		}
//	
//		xprintf(f, x_("\t};\n"));
//	
//	#if 0
//		// print grab point informaton if it exists
//		if ((us_tecflags&HASGRAB) != 0 && us_tecnode_grabcount > 0)
//		{
//			xprintf(f, x_("\nstatic INTBIG %s_centergrab[] = {\n"), us_tecedmakesymbol(techname));
//			for(i=0; i<us_tecnode_grabcount; i += 3)
//			{
//				ab = (CHAR *)tech.nodeprotos[us_tecnode_grab[i]-1].creation;
//				xprintf(f, x_("\tN%s, %ld, %ld"), us_tecedmakeupper(ab), us_tecnode_grab[i+1],
//					us_tecnode_grab[i+2]);
//				if (i != us_tecnode_grabcount-3) xprintf(f, x_(",\n"));
//			}
//			xprintf(f, x_("\n};\n"));
//		}
//	
//		// print minimum node size informaton if it exists
//		if ((us_tecflags&HASMINNODE) != 0)
//		{
//			xprintf(f, x_("\nstatic INTBIG %s_node_minsize[NODEPROTOCOUNT*2] = {\n"), us_tecedmakesymbol(techname));
//			for(i=0; i<tech.nodeprotocount; i++)
//			{
//				if (us_tecdrc_rules.minnodesize[i*2] < 0) ab = x_("XX"); else
//					ab = us_tecedmakefract(us_tecdrc_rules.minnodesize[i*2]);
//				xprintf(f, x_("\t%s, "), ab);
//				if (us_tecdrc_rules.minnodesize[i*2+1] < 0) ab = x_("XX"); else
//					ab = us_tecedmakefract(us_tecdrc_rules.minnodesize[i*2+1]);
//				xprintf(f, x_("%s"), ab);
//				if (i == tech.nodeprotocount-1) ab = x_(""); else ab = x_(",");
//				xprintf(f, x_("%s\t\t/* %s */\n"), ab, tech.nodeprotos[i].nodename);
//			}
//			xprintf(f, x_("};\n"));
//		}
//		if ((us_tecflags&HASMINNODER) != 0)
//		{
//			xprintf(f, x_("\nstatic char *%s_node_minsize_rule[NODEPROTOCOUNT] = {\n"), us_tecedmakesymbol(techname));
//			for(i=0; i<tech.nodeprotocount; i++)
//			{
//				if (i == tech.nodeprotocount-1) ab = x_(""); else ab = x_(",");
//				xprintf(f, x_("\tx_(\"%s\")%s\t\t/* %s */\n"), us_tecdrc_rules.minnodesizeR[i], ab,
//					tech.nodeprotos[i].nodename);
//			}
//			xprintf(f, x_("};\n"));
//		}
//	#endif
//	
//		// write method to reset rules
//		if ((us_tecflags&(HASCONDRC|HASUNCONDRC)) != 0)
//		{
//			CHAR *conword, *unconword;
//			if ((us_tecflags&HASCONDRC) != 0) conword = "conDist"; else conword = "null";
//			if ((us_tecflags&HASUNCONDRC) != 0) unconword = "unConDist"; else unconword = "null";
//			xprintf(f, x_("\tpublic DRCRules getFactoryDesignRules()\n"));
//			xprintf(f, x_("\t{\n"));
//			xprintf(f, x_("\t\treturn MOSRules.makeSimpleRules(this, %s, %s);\n"), conword, unconword);
//			xprintf(f, x_("\t}\n"));
//		}
//	
//		// clean up
//		for(i=0; i<tech.nodeprotocount; i++)
//		{
//			efree((CHAR *)tech.nodeprotos[i].creation);
//			tech.nodeprotos[i].creation = NONODEPROTO;
//		}
//	}
//	
//	/*
//	 * Routine to remove illegal Java charcters from "string".
//	 */
//	CHAR *us_teceditconverttojava(CHAR *string)
//	{
//		REGISTER void *infstr;
//		REGISTER CHAR *pt;
//	
//		infstr = initinfstr();
//		for(pt = string; *pt != 0; pt++)
//		{
//			if (*pt == '-') addtoinfstr(infstr, '_'); else
//				addtoinfstr(infstr, *pt);
//		}
//		return(returninfstr(infstr));
//	}
//	
//	/*
//	 * routine to convert the multiplication and addition factors in "mul" and
//	 * "add" into proper constant names.  The "yaxis" is false for X and 1 for Y
//	 */
//	CHAR *us_tecededgelabeljava(INTBIG mul, INTBIG add, BOOLEAN yaxis)
//	{
//		REGISTER INTBIG amt;
//		REGISTER void *infstr;
//	
//		infstr = initinfstr();
//	
//		// handle constant distance from center
//		if (mul == 0)
//		{
//			if (yaxis) addstringtoinfstr(infstr, "EdgeV."); else
//				addstringtoinfstr(infstr, "EdgeH.");
//			if (add == 0)
//			{
//				addstringtoinfstr(infstr, x_("makeCenter()"));
//			} else
//			{
//				formatinfstr(infstr, x_("fromCenter(%g)"), (float)add/WHOLE);
//			}
//			return(returninfstr(infstr));
//		}
//	
//		// handle constant distance from edge
//		if ((mul == H0 || mul == -H0))
//		{
//			if (yaxis) addstringtoinfstr(infstr, "EdgeV."); else
//				addstringtoinfstr(infstr, "EdgeH.");
//			amt = abs(add);
//			if (!yaxis)
//			{
//				if (mul < 0)
//				{
//					if (add == 0) addstringtoinfstr(infstr, x_("makeLeftEdge()")); else
//						formatinfstr(infstr, x_("fromLeft(%g)"), (float)amt/WHOLE);
//				} else
//				{
//					if (add == 0) addstringtoinfstr(infstr, x_("makeRightEdge()")); else
//						formatinfstr(infstr, x_("fromRight(%g)"), (float)amt/WHOLE);
//				}
//			} else
//			{
//				if (mul < 0)
//				{
//					if (add == 0) addstringtoinfstr(infstr, x_("makeBottomEdge()")); else
//						formatinfstr(infstr, x_("fromBottom(%g)"), (float)amt/WHOLE);
//				} else
//				{
//					if (add == 0) addstringtoinfstr(infstr, x_("makeTopEdge()")); else
//						formatinfstr(infstr, x_("fromTop(%g)"), (float)amt/WHOLE);
//				}
//			}
//			return(returninfstr(infstr));
//		}
//	
//		// generate two-value description
//		if (!yaxis)
//			formatinfstr(infstr, x_("new EdgeH(%g, %g)"), (float)mul/WHOLE, (float)add/WHOLE); else
//			formatinfstr(infstr, x_("new EdgeV(%g, %g)"), (float)mul/WHOLE, (float)add/WHOLE);
//		return(returninfstr(infstr));
//	}
//	
//	/****************************** SUPPORT FOR SOURCE-CODE GENERATION ******************************/
//	
//	/*
//	 * Routine to return a string representation of the floating point value "v".
//	 * The letter "f" is added to the end if appropriate.
//	 */
//	CHAR *us_tecedmakefloatstring(float v)
//	{
//		static CHAR retstr[50];
//		REGISTER CHAR *pt;
//	
//		esnprintf(retstr, 50, x_("%g"), v);
//		if (estrcmp(retstr, x_("0")) == 0) return(retstr);
//		for(pt = retstr; *pt != 0; pt++)
//			if (*pt == '.') break;
//		if (*pt == 0) estrcat(retstr, x_(".0"));
//		estrcat(retstr, x_("f"));
//		return(retstr);
//	}
//	
//	/*
//	 * routine to convert the fractional value "amt" to a technology constant.
//	 * The presumption is that quarter values exist from K0 to K10, that
//	 * half values exist up to K20, that whole values exist up to K30, and
//	 * that other values are not necessarily defined in "tech.h".
//	 */
//	CHAR *us_tecedmakefract(INTBIG amt)
//	{
//		static CHAR line[21];
//		REGISTER INTBIG whole;
//		REGISTER CHAR *pt;
//	
//		pt = line;
//		if (amt < 0)
//		{
//			*pt++ = '-';
//			amt = -amt;
//		}
//		whole = amt/WHOLE;
//		switch (amt%WHOLE)
//		{
//			case 0:
//				if (whole <= 30) (void)esnprintf(pt, 20, x_("K%ld"), whole); else
//					(void)esnprintf(pt, 20, x_("%ld"), amt);
//				break;
//			case Q0:
//				if (whole <= 10) (void)esnprintf(pt, 20, x_("Q%ld"), whole); else
//					(void)esnprintf(pt, 20, x_("%ld"), amt);
//				break;
//			case H0:
//				if (whole <= 20) (void)esnprintf(pt, 20, x_("H%ld"), whole); else
//					(void)esnprintf(pt, 20, x_("%ld"), amt);
//				break;
//			case T0:
//				if (whole <= 10) (void)esnprintf(pt, 20, x_("T%ld"), whole); else
//					(void)esnprintf(pt, 20, x_("%ld"), amt);
//				break;
//			default:
//				(void)esnprintf(pt, 20, x_("%ld"), amt);
//				break;
//		}
//		return(line);
//	}
//	
//	/*
//	 * routine to convert all characters in string "str" to upper case and to
//	 * change any nonalphanumeric characters to a "_"
//	 */
//	CHAR *us_tecedmakeupper(CHAR *str)
//	{
//		REGISTER CHAR ch;
//		REGISTER void *infstr;
//	
//		infstr = initinfstr();
//		while (*str != 0)
//		{
//			ch = *str++;
//			if (islower(ch)) ch = toupper(ch);
//			if (!isalnum(ch)) ch = '_';
//			addtoinfstr(infstr, ch);
//		}
//		return(returninfstr(infstr));
//	}
//	
//	/*
//	 * routine to change any nonalphanumeric characters in string "str" to a "_"
//	 */
//	CHAR *us_tecedmakesymbol(CHAR *str)
//	{
//		REGISTER CHAR ch;
//		REGISTER void *infstr;
//	
//		infstr = initinfstr();
//		while (*str != 0)
//		{
//			ch = *str++;
//			if (!isalnum(ch)) ch = '_';
//			addtoinfstr(infstr, ch);
//		}
//		return(returninfstr(infstr));
//	}
//	
//	/*
//	 * Routine to find the parameter value in a string that has been stored as a message
//	 * on a node.  These parameters always have the form "name: value".  This returns a pointer
//	 * to the "value" part.
//	 */
//	CHAR *us_teceditgetparameter(VARIABLE *var)
//	{
//		REGISTER CHAR *str, *orig;
//	
//		orig = str = (CHAR *)var.addr;
//		while (*str != 0 && *str != ':') str++;
//		if (*str == 0) return(orig);
//		*str++;
//		while (*str == ' ') str++;
//		return(str);
//	}
}
