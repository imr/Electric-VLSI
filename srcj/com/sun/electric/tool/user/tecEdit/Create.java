/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Create.java
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
import com.sun.electric.database.hierarchy.Library;
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
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.tecEdit.Generate.LayerInfo;
import com.sun.electric.tool.user.tecEdit.Generate.LibFromTechJob;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.dialogs.PromptAt;
import com.sun.electric.tool.user.Highlighter;

import java.util.List;
import java.util.StringTokenizer;
import java.util.Iterator;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.Point;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * This class creates technology libraries from technologies.
 */
public class Create
{

//	/* these must correspond to the layer functions in "efunction.h" */
//	LIST us_teclayer_functions[] =
//	{
//		{x_("unknown"),           x_("LFUNKNOWN"),     LFUNKNOWN},
//		{x_("metal-1"),           x_("LFMETAL1"),      LFMETAL1},
//		{x_("metal-2"),           x_("LFMETAL2"),      LFMETAL2},
//		{x_("metal-3"),           x_("LFMETAL3"),      LFMETAL3},
//		{x_("metal-4"),           x_("LFMETAL4"),      LFMETAL4},
//		{x_("metal-5"),           x_("LFMETAL5"),      LFMETAL5},
//		{x_("metal-6"),           x_("LFMETAL6"),      LFMETAL6},
//		{x_("metal-7"),           x_("LFMETAL7"),      LFMETAL7},
//		{x_("metal-8"),           x_("LFMETAL8"),      LFMETAL8},
//		{x_("metal-9"),           x_("LFMETAL9"),      LFMETAL9},
//		{x_("metal-10"),          x_("LFMETAL10"),     LFMETAL10},
//		{x_("metal-11"),          x_("LFMETAL11"),     LFMETAL11},
//		{x_("metal-12"),          x_("LFMETAL12"),     LFMETAL12},
//		{x_("poly-1"),            x_("LFPOLY1"),       LFPOLY1},
//		{x_("poly-2"),            x_("LFPOLY2"),       LFPOLY2},
//		{x_("poly-3"),            x_("LFPOLY3"),       LFPOLY3},
//		{x_("gate"),              x_("LFGATE"),        LFGATE},
//		{x_("diffusion"),         x_("LFDIFF"),        LFDIFF},
//		{x_("implant"),           x_("LFIMPLANT"),     LFIMPLANT},
//		{x_("contact-1"),         x_("LFCONTACT1"),    LFCONTACT1},
//		{x_("contact-2"),         x_("LFCONTACT2"),    LFCONTACT2},
//		{x_("contact-3"),         x_("LFCONTACT3"),    LFCONTACT3},
//		{x_("contact-4"),         x_("LFCONTACT4"),    LFCONTACT4},
//		{x_("contact-5"),         x_("LFCONTACT5"),    LFCONTACT5},
//		{x_("contact-6"),         x_("LFCONTACT6"),    LFCONTACT6},
//		{x_("contact-7"),         x_("LFCONTACT7"),    LFCONTACT7},
//		{x_("contact-8"),         x_("LFCONTACT8"),    LFCONTACT8},
//		{x_("contact-9"),         x_("LFCONTACT9"),    LFCONTACT9},
//		{x_("contact-10"),        x_("LFCONTACT10"),   LFCONTACT10},
//		{x_("contact-11"),        x_("LFCONTACT11"),   LFCONTACT11},
//		{x_("contact-12"),        x_("LFCONTACT12"),   LFCONTACT12},
//		{x_("plug"),              x_("LFPLUG"),        LFPLUG},
//		{x_("overglass"),         x_("LFOVERGLASS"),   LFOVERGLASS},
//		{x_("resistor"),          x_("LFRESISTOR"),    LFRESISTOR},
//		{x_("capacitor"),         x_("LFCAP"),         LFCAP},
//		{x_("transistor"),        x_("LFTRANSISTOR"),  LFTRANSISTOR},
//		{x_("emitter"),           x_("LFEMITTER"),     LFEMITTER},
//		{x_("base"),              x_("LFBASE"),        LFBASE},
//		{x_("collector"),         x_("LFCOLLECTOR"),   LFCOLLECTOR},
//		{x_("substrate"),         x_("LFSUBSTRATE"),   LFSUBSTRATE},
//		{x_("well"),              x_("LFWELL"),        LFWELL},
//		{x_("guard"),             x_("LFGUARD"),       LFGUARD},
//		{x_("isolation"),         x_("LFISOLATION"),   LFISOLATION},
//		{x_("bus"),               x_("LFBUS"),         LFBUS},
//		{x_("art"),               x_("LFART"),         LFART},
//		{x_("control"),           x_("LFCONTROL"),     LFCONTROL},
//	
//		{x_("p-type"),            x_("LFPTYPE"),       LFPTYPE},
//		{x_("n-type"),            x_("LFNTYPE"),       LFNTYPE},
//		{x_("depletion"),         x_("LFDEPLETION"),   LFDEPLETION},
//		{x_("enhancement"),       x_("LFENHANCEMENT"), LFENHANCEMENT},
//		{x_("light"),             x_("LFLIGHT"),       LFLIGHT},
//		{x_("heavy"),             x_("LFHEAVY"),       LFHEAVY},
//		{x_("pseudo"),            x_("LFPSEUDO"),      LFPSEUDO},
//		{x_("nonelectrical"),     x_("LFNONELEC"),     LFNONELEC},
//		{x_("connects-metal"),    x_("LFCONMETAL"),    LFCONMETAL},
//		{x_("connects-poly"),     x_("LFCONPOLY"),     LFCONPOLY},
//		{x_("connects-diff"),     x_("LFCONDIFF"),     LFCONDIFF},
//		{x_("inside-transistor"), x_("LFINTRANS"),     LFINTRANS},
//		{NULL, NULL, 0}
//	};
//	
//	static GRAPHICS us_edtechigh = {LAYERH, HIGHLIT, SOLIDC, SOLIDC,
//		{0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,
//		0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF}, NOVARIABLE, 0};
//	
//	/**
//	 * the entry Method for all technology editing
//	 */
//	void us_tecedentry(INTBIG count, CHAR *par[])
//	{
//		REGISTER TECHNOLOGY *tech;
//		REGISTER LIBRARY *lib;
//		LIBRARY **dependentlibs;
//		NODEPROTO **sequence;
//		REGISTER CHAR *pp, **dependentlist;
//		CHAR *cellname, *newpar[2];
//		UINTSML stip[16];
//		REGISTER INTBIG i, l;
//		REGISTER INTBIG dependentlibcount;
//		REGISTER NODEPROTO *np;
//		REGISTER VARIABLE *var;
//		REGISTER void *infstr;
//	
//		if (count == 0)
//		{
//			ttyputusage(x_("technology edit OPTION"));
//			return;
//		}
//	
//		l = estrlen(pp = par[0]);
//		if (namesamen(pp, x_("library-to-tech-and-c"), l) == 0 && l >= 21)
//		{
//			if (count <= 1) pp = 0; else
//				pp = par[1];
//			us_tecfromlibinit(el_curlib, pp, 1);
//			return;
//		}
//		if (namesamen(pp, x_("library-to-tech-and-java"), l) == 0 && l >= 21)
//		{
//			if (count <= 1) pp = 0; else
//				pp = par[1];
//			us_tecfromlibinit(el_curlib, pp, -1);
//			return;
//		}
//		if (namesamen(pp, x_("library-to-tech"), l) == 0)
//		{
//			if (count <= 1) pp = 0; else
//				pp = par[1];
//			us_tecfromlibinit(el_curlib, pp, 0);
//			return;
//		}
//		if (namesamen(pp, x_("tech-to-library"), l) == 0)
//		{
//			if (count == 1) tech = el_curtech; else
//			{
//				tech = gettechnology(par[1]);
//				if (tech == NOTECHNOLOGY)
//				{
//					us_abortcommand(_("Technology '%s' unknown"), par[1]);
//					return;
//				}
//			}
//			if ((tech.userbits&NONSTANDARD) != 0)
//			{
//				us_abortcommand(_("Cannot convert technology '%s', it is nonstandard"), tech.techname);
//				return;
//			}
//	
//			// see if there is already such a library
//			for(lib = el_curlib; lib != NOLIBRARY; lib = lib.nextlibrary)
//				if (namesame(lib.libname, tech.techname) == 0) break;
//			if (lib != NOLIBRARY)
//				ttyputmsg(_("Already a library called %s, using that"), lib.libname); else
//					lib = us_tecedmakelibfromtech(tech);
//			if (lib != NOLIBRARY)
//			{
//				newpar[0] = x_("use");
//				newpar[1] = lib.libname;
//				us_library(2, newpar);
//				us_tecedloadlibmap(lib);
//			}
//			return;
//		}
//		if (namesamen(pp, x_("reorder-arcs"), l) == 0 && l >= 9)
//		{
//			us_reorderprimdlog(_("Arcs"), x_("arc-"), x_("EDTEC_arcsequence"));
//			return;
//		}
//		if (namesamen(pp, x_("reorder-nodes"), l) == 0 && l >= 9)
//		{
//			us_reorderprimdlog(_("Nodes"), x_("node-"), x_("EDTEC_nodesequence"));
//			return;
//		}
//		if (namesamen(pp, x_("reorder-layers"), l) == 0 && l >= 9)
//		{
//			us_reorderprimdlog(_("Layers"), x_("layer-"), x_("EDTEC_layersequence"));
//			return;
//		}
//		if (namesamen(pp, x_("inquire-layer"), l) == 0 && l >= 2)
//		{
//			us_teceditinquire();
//			return;
//		}
//		if (namesamen(pp, x_("place-layer"), l) == 0)
//		{
//			if (count < 2)
//			{
//				ttyputusage(x_("technology edit place-layer SHAPE"));
//				return;
//			}
//	
//			us_teceditcreat(count-1, &par[1]);
//			return;
//		}
//		if (namesamen(pp, x_("change"), l) == 0 && l >= 2)
//		{
//			// in outline edit, create a point
//			if ((el_curwindowpart.state&WINDOWOUTLINEEDMODE) != 0)
//			{
//				newpar[0] = x_("trace");
//				newpar[1] = x_("add-point");
//				us_node(2, newpar);
//				return;
//			}
//	
//			us_teceditmodobject(count-1, &par[1]);
//			return;
//		}
//		if (namesamen(pp, x_("edit-node"), l) == 0 && l >= 6)
//		{
//			if (count < 2)
//			{
//				ttyputusage(x_("technology edit edit-node NODENAME"));
//				return;
//			}
//			infstr = initinfstr();
//			addstringtoinfstr(infstr, x_("node-"));
//			addstringtoinfstr(infstr, par[1]);
//			(void)allocstring(&cellname, returninfstr(infstr), el_tempcluster);
//	
//			// first make sure all fields exist
//			np = getnodeproto(cellname);
//			if (np != NONODEPROTO)
//			{
//				us_tecedmakenode(np, NPUNKNOWN, FALSE, FALSE, FALSE, FALSE, 0);
//				(*el_curconstraint.solve)(np);
//			}
//	
//			np = us_tecedentercell(cellname);
//			efree(cellname);
//			if (np == NONODEPROTO) return;
//			us_tecedmakenode(np, NPUNKNOWN, FALSE, FALSE, FALSE, FALSE, 0);
//			(*el_curconstraint.solve)(np);
//			np.userbits |= TECEDITCELL;
//			(void)us_tecedentercell(describenodeproto(np));
//			return;
//		}
//		if (namesamen(pp, x_("edit-arc"), l) == 0 && l >= 6)
//		{
//			if (count < 2)
//			{
//				ttyputusage(x_("technology edit edit-arc ARCNAME"));
//				return;
//			}
//			infstr = initinfstr();
//			addstringtoinfstr(infstr, x_("arc-"));
//			addstringtoinfstr(infstr, par[1]);
//			(void)allocstring(&cellname, returninfstr(infstr), el_tempcluster);
//			np = us_tecedentercell(cellname);
//			efree(cellname);
//			if (np == NONODEPROTO) return;
//			us_tecedmakearc(np, APUNKNOWN, 1, 1, 0, 90);
//			(*el_curconstraint.solve)(np);
//			np.userbits |= TECEDITCELL;
//			(void)us_tecedentercell(describenodeproto(np));
//			return;
//		}
//		if (namesamen(pp, x_("edit-layer"), l) == 0 && l >= 6)
//		{
//			if (count < 2)
//			{
//				ttyputusage(x_("technology edit edit-layer LAYERNAME"));
//				return;
//			}
//			infstr = initinfstr();
//			addstringtoinfstr(infstr, x_("layer-"));
//			addstringtoinfstr(infstr, par[1]);
//			(void)allocstring(&cellname, returninfstr(infstr), el_tempcluster);
//	
//			// first make sure all fields exist
//			for(i=0; i<16; i++) stip[i] = 0;
//			np = getnodeproto(cellname);
//			if (np != NONODEPROTO)
//			{
//				us_tecedmakelayer(np, COLORT1, stip, SOLIDC, x_("XX"), LFUNKNOWN, x_("x"), x_(""),
//					x_(""), 0.0, 0.0, 0.0, 0, 0, 0);
//				(*el_curconstraint.solve)(np);
//			}
//	
//			np = us_tecedentercell(cellname);
//			efree(cellname);
//			if (np == NONODEPROTO) return;
//			us_tecedmakelayer(np, COLORT1, stip, SOLIDC, x_("XX"), LFUNKNOWN, x_("x"), x_(""),
//				x_(""), 0.0, 0.0, 0.0, 0, 0, 0);
//			(*el_curconstraint.solve)(np);
//			np.userbits |= TECEDITCELL;
//			(void)us_tecedentercell(describenodeproto(np));
//			return;
//		}
//		if (namesamen(pp, x_("edit-subsequent"), l) == 0 && l >= 6)
//		{
//			np = us_needcell();
//			if (np == NONODEPROTO) return;
//			if (namesamen(np.protoname, x_("node-"), 5) == 0)
//			{
//				dependentlibcount = us_teceditgetdependents(el_curlib, &dependentlibs);
//				i = us_teceditfindsequence(dependentlibs, dependentlibcount, x_("node-"),
//					x_("EDTEC_nodesequence"), &sequence);
//				if (i == 0) return;
//				for(l=0; l<i; l++) if (sequence[l] == np)
//				{
//					if (l == i-1) np = sequence[0]; else np = sequence[l+1];
//					(void)us_tecedentercell(describenodeproto(np));
//					break;
//				}
//				efree((CHAR *)sequence);
//				return;
//			}
//			if (namesamen(np.protoname, x_("arc-"), 4) == 0)
//			{
//				dependentlibcount = us_teceditgetdependents(el_curlib, &dependentlibs);
//				i = us_teceditfindsequence(dependentlibs, dependentlibcount, x_("arc-"),
//					x_("EDTEC_arcsequence"), &sequence);
//				if (i == 0) return;
//				for(l=0; l<i; l++) if (sequence[l] == np)
//				{
//					if (l == i-1) np = sequence[0]; else np = sequence[l+1];
//					(void)us_tecedentercell(describenodeproto(np));
//					break;
//				}
//				efree((CHAR *)sequence);
//				return;
//			}
//			if (namesamen(np.protoname, x_("layer-"), 6) == 0)
//			{
//				dependentlibcount = us_teceditgetdependents(el_curlib, &dependentlibs);
//				i = us_teceditfindsequence(dependentlibs, dependentlibcount, x_("layer-"),
//					x_("EDTEC_layersequence"), &sequence);
//				if (i == 0) return;
//				for(l=0; l<i; l++) if (sequence[l] == np)
//				{
//					if (l == i-1) np = sequence[0]; else np = sequence[l+1];
//					(void)us_tecedentercell(describenodeproto(np));
//					break;
//				}
//				efree((CHAR *)sequence);
//				return;
//			}
//			ttyputerr(_("Must be editing a layer, node, or arc to advance to the next"));
//			return;
//		}
//		if (namesamen(pp, x_("edit-colors"), l) == 0 && l >= 6)
//		{
//			us_teceditcolormap();
//			return;
//		}
//		if (namesamen(pp, x_("edit-design-rules"), l) == 0 && l >= 6)
//		{
//			us_teceditdrc();
//			return;
//		}
//		if (namesamen(pp, x_("edit-misc-information"), l) == 0 && l >= 6)
//		{
//			// first make sure all fields exist
//			np = getnodeproto(x_("factors"));
//			if (np != NONODEPROTO)
//			{
//				us_tecedmakeinfo(np, 2000, el_curlib.libname);
//				(*el_curconstraint.solve)(np);
//			}
//	
//			// now edit the cell
//			np = us_tecedentercell(x_("factors"));
//			if (np == NONODEPROTO) return;
//			us_tecedmakeinfo(np, 2000, el_curlib.libname);
//			(*el_curconstraint.solve)(np);
//			(void)us_tecedentercell(describenodeproto(np));
//			return;
//		}
//		if (namesamen(pp, x_("identify-layers"), l) == 0 && l >= 10)
//		{
//			us_teceditidentify(FALSE);
//			return;
//		}
//		if (namesamen(pp, x_("identify-ports"), l) == 0 && l >= 10)
//		{
//			us_teceditidentify(TRUE);
//			return;
//		}
//		if (namesamen(pp, x_("dependent-libraries"), l) == 0 && l >= 2)
//		{
//			if (count < 2)
//			{
//				// display dependent library names
//				var = getval((INTBIG)el_curlib, VLIBRARY, VSTRING|VISARRAY, x_("EDTEC_dependent_libraries"));
//				if (var == NOVARIABLE) ttyputmsg(_("There are no dependent libraries")); else
//				{
//					i = getlength(var);
//					ttyputmsg(_("%ld dependent %s:"), i, makeplural(x_("library"), i));
//					for(l=0; l<i; l++)
//					{
//						pp = ((CHAR **)var.addr)[l];
//						lib = getlibrary(pp);
//						ttyputmsg(x_("    %s%s"), pp, (lib == NOLIBRARY ? _(" (not read in)") : x_("")));
//					}
//				}
//				return;
//			}
//	
//			// clear list if just "-" is given
//			if (count == 2 && estrcmp(par[1], x_("-")) == 0)
//			{
//				var = getval((INTBIG)el_curlib, VLIBRARY, VSTRING|VISARRAY, x_("EDTEC_dependent_libraries"));
//				if (var != NOVARIABLE)
//					(void)delval((INTBIG)el_curlib, VLIBRARY, x_("EDTEC_dependent_libraries"));
//				return;
//			}
//	
//			// create a list
//			dependentlist = (CHAR **)emalloc((count-1) * (sizeof (CHAR *)), el_tempcluster);
//			if (dependentlist == 0) return;
//			for(i=1; i<count; i++) dependentlist[i-1] = par[i];
//			(void)setval((INTBIG)el_curlib, VLIBRARY, x_("EDTEC_dependent_libraries"), (INTBIG)dependentlist,
//				VSTRING|VISARRAY|((count-1)<<VLENGTHSH));
//			efree((CHAR *)dependentlist);
//			return;
//		}
//		if (namesamen(pp, x_("compact-current-cell"), l) == 0 && l >= 2)
//		{
//			if (el_curwindowpart == NOWINDOWPART) np = NONODEPROTO; else
//				np = el_curwindowpart.curnodeproto;
//			if (np != NONODEPROTO) us_tecedcompact(np); else
//				ttyputmsg(_("No current cell to compact"));
//			return;
//		}
//		ttyputbadusage(x_("technology edit"));
//	}
//	
//	/**
//	 * Method to compact the current technology-edit cell
//	 */
//	void us_tecedcompact(NODEPROTO *cell)
//	{
//		REGISTER EXAMPLE *nelist, *ne;
//		REGISTER SAMPLE *ns;
//		REGISTER BOOLEAN first;
//		REGISTER INTBIG i, numexamples, xoff, yoff, examplenum, leftheight, height,
//			lx, hx, ly, hy, topy, separation;
//		REGISTER NODEINST *ni;
//	
//		if (namesame(cell.protoname, x_("factors")) == 0)
//		{
//			// save highlighting
//			us_pushhighlight();
//			us_clearhighlightcount();
//	
//			// move the option text
//			us_tecedfindspecialtext(cell, us_tecedmisctexttable);
//			for(i=0; us_tecedmisctexttable[i].funct != 0; i++)
//			{
//				ni = us_tecedmisctexttable[i].ni;
//				if (ni == NONODEINST) continue;
//				xoff = us_tecedmisctexttable[i].x - (ni.lowx + ni.highx) / 2;
//				yoff = us_tecedmisctexttable[i].y - (ni.lowy + ni.highy) / 2;
//				if (xoff == 0 && yoff == 0) continue;
//				startobjectchange((INTBIG)ni, VNODEINST);
//				modifynodeinst(ni, xoff, yoff, xoff, yoff, 0, 0);
//				endobjectchange((INTBIG)ni, VNODEINST);
//			}
//			us_pophighlight(FALSE);
//			return;
//		}
//		if (namesamen(cell.protoname, x_("layer-"), 6) == 0)
//		{
//			ttyputmsg("Cannot compact technology-edit layer cells");
//			return;
//		}
//		if (namesamen(cell.protoname, x_("arc-"), 4) == 0)
//		{
//			// save highlighting
//			us_pushhighlight();
//			us_clearhighlightcount();
//	
//			// move the option text
//			us_tecedfindspecialtext(cell, us_tecedarctexttable);
//			for(i=0; us_tecedarctexttable[i].funct != 0; i++)
//			{
//				ni = us_tecedarctexttable[i].ni;
//				if (ni == NONODEINST) continue;
//				xoff = us_tecedarctexttable[i].x - (ni.lowx + ni.highx) / 2;
//				yoff = us_tecedarctexttable[i].y - (ni.lowy + ni.highy) / 2;
//				if (xoff == 0 && yoff == 0) continue;
//				startobjectchange((INTBIG)ni, VNODEINST);
//				modifynodeinst(ni, xoff, yoff, xoff, yoff, 0, 0);
//				endobjectchange((INTBIG)ni, VNODEINST);
//			}
//	
//			// compute bounds of arc contents
//			first = TRUE;
//			for(ni = cell.firstnodeinst; ni != NONODEINST; ni = ni.nextnodeinst)
//			{
//				// ignore the special text nodes
//				for(i=0; us_tecedarctexttable[i].funct != 0; i++)
//					if (us_tecedarctexttable[i].ni == ni) break;
//				if (us_tecedarctexttable[i].funct != 0) continue;
//	
//				if (first)
//				{
//					first = FALSE;
//					lx = ni.lowx;   hx = ni.highx;
//					ly = ni.lowy;   hy = ni.highy;
//				} else
//				{
//					if (ni.lowx < lx) lx = ni.lowx;
//					if (ni.highx > hx) hx = ni.highx;
//					if (ni.lowy < ly) ly = ni.lowy;
//					if (ni.highy > hy) hy = ni.highy;
//				}
//			}
//	
//			// now rearrange the geometry
//			if (!first)
//			{
//				xoff = -(lx + hx) / 2;
//				yoff = -hy;
//				if (xoff != 0 || yoff != 0)
//				{
//					for(ni = cell.firstnodeinst; ni != NONODEINST; ni = ni.nextnodeinst)
//					{
//						// ignore the special text nodes
//						for(i=0; us_tecedarctexttable[i].funct != 0; i++)
//							if (us_tecedarctexttable[i].ni == ni) break;
//						if (us_tecedarctexttable[i].funct != 0) continue;
//	
//						startobjectchange((INTBIG)ni, VNODEINST);
//						modifynodeinst(ni, xoff, yoff, xoff, yoff, 0, 0);
//						endobjectchange((INTBIG)ni, VNODEINST);
//					}
//				}
//			}
//			us_pophighlight(FALSE);
//			return;
//		}
//		if (namesamen(cell.protoname, x_("node-"), 5) == 0)
//		{
//			// save highlighting
//			us_pushhighlight();
//			us_clearhighlightcount();
//	
//			// move the option text
//			us_tecedfindspecialtext(cell, us_tecednodetexttable);
//			for(i=0; us_tecednodetexttable[i].funct != 0; i++)
//			{
//				ni = us_tecednodetexttable[i].ni;
//				if (ni == NONODEINST) continue;
//				xoff = us_tecednodetexttable[i].x - (ni.lowx + ni.highx) / 2;
//				yoff = us_tecednodetexttable[i].y - (ni.lowy + ni.highy) / 2;
//				if (xoff == 0 && yoff == 0) continue;
//				startobjectchange((INTBIG)ni, VNODEINST);
//				modifynodeinst(ni, xoff, yoff, xoff, yoff, 0, 0);
//				endobjectchange((INTBIG)ni, VNODEINST);
//			}
//	
//			// move the examples
//			nelist = us_tecedgetexamples(cell, TRUE);
//			if (nelist == NOEXAMPLE) return;
//			numexamples = 0;
//			for(ne = nelist; ne != NOEXAMPLE; ne = ne.nextexample) numexamples++;
//			examplenum = 0;
//			topy = 0;
//			separation = mini(nelist.hx - nelist.lx, nelist.hy - nelist.ly);
//			for(ne = nelist; ne != NOEXAMPLE; ne = ne.nextexample)
//			{
//				// handle left or right side
//				yoff = topy - ne.hy;
//				if ((examplenum&1) == 0)
//				{
//					// do left side
//					if (examplenum == numexamples-1)
//					{
//						// last one is centered
//						xoff = -(ne.lx + ne.hx) / 2;
//					} else
//					{
//						xoff = -ne.hx - separation/2;
//					}
//					leftheight = ne.hy - ne.ly;
//				} else
//				{
//					// do right side
//					xoff = -ne.lx + separation/2;
//					height = ne.hy - ne.ly;
//					if (leftheight > height) height = leftheight;
//					topy -= height + separation;
//				}
//				examplenum++;
//	
//				// adjust every node in the example
//				if (xoff == 0 && yoff == 0) continue;
//				for(ns = ne.firstsample; ns != NOSAMPLE; ns = ns.nextsample)
//				{
//					ni = ns.node;
//					startobjectchange((INTBIG)ni, VNODEINST);
//					modifynodeinst(ni, xoff, yoff, xoff, yoff, 0, 0);
//					endobjectchange((INTBIG)ni, VNODEINST);
//				}
//			}
//			us_tecedfreeexamples(nelist);
//			us_pophighlight(FALSE);
//			return;
//		}
//		ttyputmsg(_("Cannot compact technology-edit cell %s: unknown type"),
//			describenodeproto(cell));
//	}
//	
//	/*
//	 * Routine for editing the DRC tables.
//	 */
//	void us_teceditdrc(void)
//	{
//		REGISTER INTBIG i, changed, nodecount;
//		NODEPROTO **nodesequence;
//		LIBRARY *liblist[1];
//		REGISTER VARIABLE *var;
//		REGISTER DRCRULES *rules;
//	
//		// get the current list of layer and node names
//		us_tecedgetlayernamelist();
//		liblist[0] = el_curlib;
//		nodecount = us_teceditfindsequence(liblist, 1, x_("node-"), x_("EDTEC_nodesequence"), &nodesequence);
//	
//		// create a RULES structure
//		rules = dr_allocaterules(us_teceddrclayers, nodecount, x_("EDITED TECHNOLOGY"));
//		if (rules == NODRCRULES) return;
//		for(i=0; i<us_teceddrclayers; i++)
//			(void)allocstring(&rules.layernames[i], us_teceddrclayernames[i], el_tempcluster);
//		for(i=0; i<nodecount; i++)
//			(void)allocstring(&rules.nodenames[i], &nodesequence[i].protoname[5], el_tempcluster);
//		if (nodecount > 0) efree((CHAR *)nodesequence);
//	
//		// get the text-list of design rules and convert them into arrays
//		var = getval((INTBIG)el_curlib, VLIBRARY, VSTRING|VISARRAY, x_("EDTEC_DRC"));
//		us_teceditgetdrcarrays(var, rules);
//	
//		// edit the design-rule arrays
//		changed = dr_rulesdlog(NOTECHNOLOGY, rules);
//	
//		// if changes were made, convert the arrays back into a text-list
//		if (changed != 0)
//		{
//			us_tecedloaddrcmessage(rules, el_curlib);
//		}
//	
//		// free the arrays
//		dr_freerules(rules);
//	}
//	
//	/*
//	 * Routine to update tables to reflect that cell "oldname" is now called "newname".
//	 * If "newname" is not valid, any rule that refers to "oldname" is removed.
//	 * 
//	 */
//	void us_tecedrenamecell(CHAR *oldname, CHAR *newname)
//	{
//		REGISTER VARIABLE *var;
//		REGISTER INTBIG i, len;
//		REGISTER BOOLEAN valid;
//		INTBIG count;
//		REGISTER CHAR *origstr, *firstkeyword, *keyword;
//		CHAR *str, **strings;
//		REGISTER void *infstr, *sa;
//	
//		// if this is a layer, rename the layer sequence array
//		if (namesamen(oldname, x_("layer-"), 6) == 0 && namesamen(newname, x_("layer-"), 6) == 0)
//		{
//			us_tecedrenamesequence(x_("EDTEC_layersequence"), &oldname[6], &newname[6]);
//		}
//	
//		// if this is an arc, rename the arc sequence array
//		if (namesamen(oldname, x_("arc-"), 4) == 0 && namesamen(newname, x_("arc-"), 4) == 0)
//		{
//			us_tecedrenamesequence(x_("EDTEC_arcsequence"), &oldname[4], &newname[4]);
//		}
//	
//		// if this is a node, rename the node sequence array
//		if (namesamen(oldname, x_("node-"), 5) == 0 && namesamen(newname, x_("node-"), 5) == 0)
//		{
//			us_tecedrenamesequence(x_("EDTEC_nodesequence"), &oldname[5], &newname[5]);
//		}
//	
//		// see if there are design rules in the current library
//		var = getval((INTBIG)el_curlib, VLIBRARY, VSTRING|VISARRAY, x_("EDTEC_DRC"));
//		if (var == NOVARIABLE) return;
//	
//		// examine the rules and convert the name
//		len = getlength(var);
//		sa = newstringarray(us_tool.cluster);
//		for(i=0; i<len; i++)
//		{
//			// parse the DRC rule
//			str = ((CHAR **)var.addr)[i];
//			origstr = str;
//			firstkeyword = getkeyword(&str, x_(" "));
//			if (firstkeyword == NOSTRING) return;
//	
//			// pass wide wire limitation through
//			if (*firstkeyword == 'l')
//			{
//				addtostringarray(sa, origstr);
//				continue;
//			}
//	
//			// rename nodes in the minimum node size rule
//			if (*firstkeyword == 'n')
//			{
//				if (namesamen(oldname, x_("node-"), 5) == 0 &&
//					namesame(&oldname[5], &firstkeyword[1]) == 0)
//				{
//					// substitute the new name
//					if (namesamen(newname, x_("node-"), 5) == 0)
//					{
//						infstr = initinfstr();
//						addstringtoinfstr(infstr, x_("n"));
//						addstringtoinfstr(infstr, &newname[5]);
//						addstringtoinfstr(infstr, str);
//						addtostringarray(sa, returninfstr(infstr));
//					}
//					continue;
//				}
//				addtostringarray(sa, origstr);
//				continue;
//			}
//	
//			// rename layers in the minimum layer size rule
//			if (*firstkeyword == 's')
//			{
//				valid = TRUE;
//				infstr = initinfstr();
//				formatinfstr(infstr, x_("%s "), firstkeyword);
//				keyword = getkeyword(&str, x_(" "));
//				if (keyword == NOSTRING) return;
//				if (namesamen(oldname, x_("layer-"), 6) == 0 &&
//					namesame(&oldname[6], keyword) == 0)
//				{
//					if (namesamen(newname, x_("layer-"), 6) != 0) valid = FALSE; else
//						addstringtoinfstr(infstr, &newname[6]);
//				} else
//					addstringtoinfstr(infstr, keyword);
//				addstringtoinfstr(infstr, str);
//				str = returninfstr(infstr);
//				if (valid) addtostringarray(sa, str);
//				continue;
//			}
//	
//			// layer width rule: substitute layer names
//			infstr = initinfstr();
//			formatinfstr(infstr, x_("%s "), firstkeyword);
//			valid = TRUE;
//	
//			// get the first layer name and convert it
//			keyword = getkeyword(&str, x_(" "));
//			if (keyword == NOSTRING) return;
//			if (namesamen(oldname, x_("layer-"), 6) == 0 &&
//				namesame(&oldname[6], keyword) == 0)
//			{
//				// substitute the new name
//				if (namesamen(newname, x_("layer-"), 6) != 0) valid = FALSE; else
//					addstringtoinfstr(infstr, &newname[6]);
//			} else
//				addstringtoinfstr(infstr, keyword);
//			addtoinfstr(infstr, ' ');
//	
//			// get the second layer name and convert it
//			keyword = getkeyword(&str, x_(" "));
//			if (keyword == NOSTRING) return;
//			if (namesamen(oldname, x_("layer-"), 6) == 0 &&
//				namesame(&oldname[6], keyword) == 0)
//			{
//				// substitute the new name
//				if (namesamen(newname, x_("layer-"), 6) != 0) valid = FALSE; else
//					addstringtoinfstr(infstr, &newname[6]);
//			} else
//				addstringtoinfstr(infstr, keyword);
//	
//			addstringtoinfstr(infstr, str);
//			str = returninfstr(infstr);
//			if (valid) addtostringarray(sa, str);
//		}
//		strings = getstringarray(sa, &count);
//		setval((INTBIG)el_curlib, VLIBRARY, x_("EDTEC_DRC"), (INTBIG)strings,
//			VSTRING|VISARRAY|(count<<VLENGTHSH));
//		killstringarray(sa);
//	}
//	
//	/*
//	 * Routine to rename the layer/arc/node sequence arrays to account for a name change.
//	 * The sequence array is in variable "varname", and the item has changed from "oldname" to
//	 * "newname".
//	 */
//	void us_tecedrenamesequence(CHAR *varname, CHAR *oldname, CHAR *newname)
//	{
//		REGISTER VARIABLE *var;
//		CHAR **strings;
//		REGISTER INTBIG i, len;
//		INTBIG count;
//		void *sa;
//	
//		var = getval((INTBIG)el_curlib, VLIBRARY, VSTRING|VISARRAY, varname);
//		if (var == NOVARIABLE) return;
//	
//		strings = (CHAR **)var.addr;
//		len = getlength(var);
//		sa = newstringarray(us_tool.cluster);
//		for(i=0; i<len; i++)
//		{
//			if (namesame(strings[i], oldname) == 0)
//				addtostringarray(sa, newname); else
//					addtostringarray(sa, strings[i]);
//		}
//		strings = getstringarray(sa, &count);
//		setval((INTBIG)el_curlib, VLIBRARY, varname, (INTBIG)strings,
//			VSTRING|VISARRAY|(count<<VLENGTHSH));
//		killstringarray(sa);
//	}
//	
//	void us_tecedgetlayernamelist(void)
//	{
//		REGISTER INTBIG i, dependentlibcount;
//		REGISTER NODEPROTO *np;
//		NODEPROTO **sequence;
//		LIBRARY **dependentlibs;
//	
//		// free any former layer name information
//		if (us_teceddrclayernames != 0)
//		{
//			for(i=0; i<us_teceddrclayers; i++) efree(us_teceddrclayernames[i]);
//			efree((CHAR *)us_teceddrclayernames);
//			us_teceddrclayernames = 0;
//		}
//	
//		dependentlibcount = us_teceditgetdependents(el_curlib, &dependentlibs);
//		us_teceddrclayers = us_teceditfindsequence(dependentlibs, dependentlibcount, x_("layer-"),
//			x_("EDTEC_layersequence"), &sequence);
//	
//		// build and fill array of layers for DRC parsing
//		us_teceddrclayernames = (CHAR **)emalloc(us_teceddrclayers * (sizeof (CHAR *)), us_tool.cluster);
//		if (us_teceddrclayernames == 0) return;
//		for(i = 0; i<us_teceddrclayers; i++)
//		{
//			np = sequence[i];
//			(void)allocstring(&us_teceddrclayernames[i], &np.protoname[6], us_tool.cluster);
//		}
//	}
//	
//	/*
//	 * Routine to create arrays describing the design rules in the variable "var" (which is
//	 * from "EDTEC_DRC" on a library).  The arrays are stored in "rules".
//	 */
//	void us_teceditgetdrcarrays(VARIABLE *var, DRCRULES *rules)
//	{
//		REGISTER INTBIG i, l;
//		INTBIG amt;
//		BOOLEAN connected, wide, multi, edge;
//		INTBIG widrule, layer1, layer2, j;
//		REGISTER CHAR *str, *pt;
//		CHAR *rule;
//	
//		// get the design rules
//		if (var == NOVARIABLE) return;
//	
//		l = getlength(var);
//		for(i=0; i<l; i++)
//		{
//			// parse the DRC rule
//			str = ((CHAR **)var.addr)[i];
//			while (*str == ' ') str++;
//			if (*str == 0) continue;
//	
//			// special case for node minimum size rule
//			if (*str == 'n')
//			{
//				str++;
//				for(pt = str; *pt != 0; pt++) if (*pt == ' ') break;
//				if (*pt == 0)
//				{
//					ttyputmsg(_("Bad node size rule (line %ld): %s"), i+1, str);
//					continue;
//				}
//				*pt = 0;
//				for(j=0; j<rules.numnodes; j++)
//					if (namesame(str, rules.nodenames[j]) == 0) break;
//				*pt = ' ';
//				if (j >= rules.numnodes)
//				{
//					ttyputmsg(_("Unknown node (line %ld): %s"), i+1, str);
//					continue;
//				}
//				while (*pt == ' ') pt++;
//				rules.minnodesize[j*2] = atofr(pt);
//				while (*pt != 0 && *pt != ' ') pt++;
//				while (*pt == ' ') pt++;
//				rules.minnodesize[j*2+1] = atofr(pt);
//				while (*pt != 0 && *pt != ' ') pt++;
//				while (*pt == ' ') pt++;
//				if (*pt != 0) reallocstring(&rules.minnodesizeR[j], pt, el_tempcluster);
//				continue;
//			}
//	
//			// parse the layer rule
//			if (us_tecedgetdrc(str, &connected, &wide, &multi, &widrule, &edge,
//				&amt, &layer1, &layer2, &rule, rules.numlayers, rules.layernames))
//			{
//				ttyputmsg(_("DRC line %ld is: %s"), i+1, str);
//				continue;
//			}
//	
//			// set the layer spacing
//			if (widrule == 1)
//			{
//				rules.minwidth[layer1] = amt;
//				if (*rule != 0)
//					(void)reallocstring(&rules.minwidthR[layer1], rule, el_tempcluster);
//			} else if (widrule == 2)
//			{
//				rules.widelimit = amt;
//			} else
//			{
//				if (layer1 > layer2) { j = layer1;  layer1 = layer2;  layer2 = j; }
//				j = (layer1+1) * (layer1/2) + (layer1&1) * ((layer1+1)/2);
//				j = layer2 + rules.numlayers * layer1 - j;
//				if (edge)
//				{
//					rules.edgelist[j] = amt;
//					if (*rule != 0)
//						(void)reallocstring(&rules.edgelistR[j], rule, el_tempcluster);
//				} else if (wide)
//				{
//					if (connected)
//					{
//						rules.conlistW[j] = amt;
//						if (*rule != 0)
//							(void)reallocstring(&rules.conlistWR[j], rule, el_tempcluster);
//					} else
//					{
//						rules.unconlistW[j] = amt;
//						if (*rule != 0)
//							(void)reallocstring(&rules.unconlistWR[j], rule, el_tempcluster);
//					}
//				} else if (multi)
//				{
//					if (connected)
//					{
//						rules.conlistM[j] = amt;
//						if (*rule != 0)
//							(void)reallocstring(&rules.conlistMR[j], rule, el_tempcluster);
//					} else
//					{
//						rules.unconlistM[j] = amt;
//						if (*rule != 0)
//							(void)reallocstring(&rules.unconlistMR[j], rule, el_tempcluster);
//					}
//				} else
//				{
//					if (connected)
//					{
//						rules.conlist[j] = amt;
//						if (*rule != 0)
//							(void)reallocstring(&rules.conlistR[j], rule, el_tempcluster);
//					} else
//					{
//						rules.unconlist[j] = amt;
//						if (*rule != 0)
//							(void)reallocstring(&rules.unconlistR[j], rule, el_tempcluster);
//					}
//				}
//			}
//		}
//	}
//	
//	/*
//	 * routine to parse DRC line "str" and fill the factors "connected" (set nonzero
//	 * if rule is for connected layers), "amt" (rule distance), "layer1" and "layer2"
//	 * (the layers).  Presumes that there are "maxlayers" layer names in the
//	 * array "layernames".  Returns true on error.
//	 */
//	BOOLEAN us_tecedgetdrc(CHAR *str, BOOLEAN *connected, BOOLEAN *wide, BOOLEAN *multi, INTBIG *widrule,
//		BOOLEAN *edge, INTBIG *amt, INTBIG *layer1, INTBIG *layer2, CHAR **rule, INTBIG maxlayers,
//		CHAR **layernames)
//	{
//		REGISTER CHAR *pt;
//		REGISTER INTBIG save;
//	
//		*connected = *wide = *multi = *edge = FALSE;
//		for( ; *str != 0; str++)
//		{
//			if (tolower(*str) == 'c')
//			{
//				*connected = TRUE;
//				continue;
//			}
//			if (tolower(*str) == 'w')
//			{
//				*wide = TRUE;
//				continue;
//			}
//			if (tolower(*str) == 'm')
//			{
//				*multi = TRUE;
//				continue;
//			}
//			if (tolower(*str) == 'e')
//			{
//				*edge = TRUE;
//				continue;
//			}
//			break;
//		}
//		*widrule = 0;
//		if (tolower(*str) == 's')
//		{
//			*widrule = 1;
//			str++;
//		} else if (tolower(*str) == 'l')
//		{
//			*widrule = 2;
//			str++;
//		}
//	
//		// get the distance
//		pt = str;
//		while (*pt != 0 && *pt != ' ' && *pt != '\t') pt++;
//		while (*pt == ' ' || *pt == '\t') pt++;
//		*amt = atofr(str);
//	
//		// get the first layer
//		if (*widrule != 2)
//		{
//			str = pt;
//			if (*str == 0)
//			{
//				ttyputerr(_("Cannot find layer names on DRC line"));
//				return(TRUE);
//			}
//			while (*pt != 0 && *pt != ' ' && *pt != '\t') pt++;
//			if (*pt == 0)
//			{
//				ttyputerr(_("Cannot find layer name on DRC line"));
//				return(TRUE);
//			}
//			save = *pt;
//			*pt = 0;
//			for(*layer1 = 0; *layer1 < maxlayers; (*layer1)++)
//				if (namesame(str, layernames[*layer1]) == 0) break;
//			*pt++ = (CHAR)save;
//			if (*layer1 >= maxlayers)
//			{
//				ttyputerr(_("First DRC layer name unknown"));
//				return(TRUE);
//			}
//			while (*pt == ' ' || *pt == '\t') pt++;
//		}
//	
//		// get the second layer
//		if (*widrule == 0)
//		{
//			str = pt;
//			while (*pt != 0 && *pt != ' ' && *pt != '\t') pt++;
//			save = *pt;
//			*pt = 0;
//			for(*layer2 = 0; *layer2 < maxlayers; (*layer2)++)
//				if (namesame(str, layernames[*layer2]) == 0) break;
//			*pt = (CHAR)save;
//			if (*layer2 >= maxlayers)
//			{
//				ttyputerr(_("Second DRC layer name unknown"));
//				return(TRUE);
//			}
//		}
//	
//		while (*pt == ' ' || *pt == '\t') pt++;
//		*rule = pt;
//		return(FALSE);
//	}
//	
//	/*
//	 * Helper routine to examine the arrays describing the design rules and create
//	 * the variable "EDTEC_DRC" on library "lib".
//	 */
//	void us_tecedloaddrcmessage(DRCRULES *rules, LIBRARY *lib)
//	{
//		REGISTER INTBIG drccount, drcindex, i, k, j;
//		REGISTER CHAR **drclist;
//		REGISTER void *infstr;
//	
//		// determine the number of lines in the text-version of the design rules
//		drccount = 0;
//		for(i=0; i<rules.utsize; i++)
//		{
//			if (rules.conlist[i] >= 0) drccount++;
//			if (rules.unconlist[i] >= 0) drccount++;
//			if (rules.conlistW[i] >= 0) drccount++;
//			if (rules.unconlistW[i] >= 0) drccount++;
//			if (rules.conlistM[i] >= 0) drccount++;
//			if (rules.unconlistM[i] >= 0) drccount++;
//			if (rules.edgelist[i] >= 0) drccount++;
//		}
//		for(i=0; i<rules.numlayers; i++)
//		{
//			if (rules.minwidth[i] >= 0) drccount++;
//		}
//		for(i=0; i<rules.numnodes; i++)
//		{
//			if (rules.minnodesize[i*2] > 0 || rules.minnodesize[i*2+1] > 0) drccount++;
//		}
//	
//		// load the arrays
//		if (drccount != 0)
//		{
//			drccount++;
//			drclist = (CHAR **)emalloc((drccount * (sizeof (CHAR *))), el_tempcluster);
//			if (drclist == 0) return;
//			drcindex = 0;
//	
//			// write the width limit
//			infstr = initinfstr();
//			formatinfstr(infstr, x_("l%s"), frtoa(rules.widelimit));
//			(void)allocstring(&drclist[drcindex++], returninfstr(infstr), el_tempcluster);
//	
//			// write the minimum width for each layer
//			for(i=0; i<rules.numlayers; i++)
//			{
//				if (rules.minwidth[i] >= 0)
//				{
//					infstr = initinfstr();
//					formatinfstr(infstr, x_("s%s %s %s"), frtoa(rules.minwidth[i]),
//						rules.layernames[i], rules.minwidthR[i]);
//					(void)allocstring(&drclist[drcindex++], returninfstr(infstr), el_tempcluster);
//				}
//			}
//	
//			// write the minimum size for each node
//			for(i=0; i<rules.numnodes; i++)
//			{
//				if (rules.minnodesize[i*2] <= 0 && rules.minnodesize[i*2+1] <= 0) continue;
//				{
//					infstr = initinfstr();
//					formatinfstr(infstr, x_("n%s %s %s %s"), rules.nodenames[i],
//						frtoa(rules.minnodesize[i*2]), frtoa(rules.minnodesize[i*2+1]),
//						rules.minnodesizeR[i]);
//					(void)allocstring(&drclist[drcindex++], returninfstr(infstr), el_tempcluster);
//				}
//			}
//	
//			// now do the distance rules
//			k = 0;
//			for(i=0; i<rules.numlayers; i++) for(j=i; j<rules.numlayers; j++)
//			{
//				if (rules.conlist[k] >= 0)
//				{
//					infstr = initinfstr();
//					formatinfstr(infstr, x_("c%s %s %s %s"), frtoa(rules.conlist[k]),
//						rules.layernames[i], rules.layernames[j],
//							rules.conlistR[k]);
//					(void)allocstring(&drclist[drcindex++], returninfstr(infstr), el_tempcluster);
//				}
//				if (rules.unconlist[k] >= 0)
//				{
//					infstr = initinfstr();
//					formatinfstr(infstr, x_("%s %s %s %s"), frtoa(rules.unconlist[k]),
//						rules.layernames[i], rules.layernames[j],
//							rules.unconlistR[k]);
//					(void)allocstring(&drclist[drcindex++], returninfstr(infstr), el_tempcluster);
//				}
//				if (rules.conlistW[k] >= 0)
//				{
//					infstr = initinfstr();
//					formatinfstr(infstr, x_("cw%s %s %s %s"), frtoa(rules.conlistW[k]),
//						rules.layernames[i], rules.layernames[j],
//							rules.conlistWR[k]);
//					(void)allocstring(&drclist[drcindex++], returninfstr(infstr), el_tempcluster);
//				}
//				if (rules.unconlistW[k] >= 0)
//				{
//					infstr = initinfstr();
//					formatinfstr(infstr, x_("w%s %s %s %s"), frtoa(rules.unconlistW[k]),
//						rules.layernames[i], rules.layernames[j],
//							rules.unconlistWR[k]);
//					(void)allocstring(&drclist[drcindex++], returninfstr(infstr), el_tempcluster);
//				}
//				if (rules.conlistM[k] >= 0)
//				{
//					infstr = initinfstr();
//					formatinfstr(infstr, x_("cm%s %s %s %s"), frtoa(rules.conlistM[k]),
//						rules.layernames[i], rules.layernames[j],
//							rules.conlistMR[k]);
//					(void)allocstring(&drclist[drcindex++], returninfstr(infstr), el_tempcluster);
//				}
//				if (rules.unconlistM[k] >= 0)
//				{
//					infstr = initinfstr();
//					formatinfstr(infstr, x_("m%s %s %s %s"), frtoa(rules.unconlistM[k]),
//						rules.layernames[i], rules.layernames[j],
//							rules.unconlistMR[k]);
//					(void)allocstring(&drclist[drcindex++], returninfstr(infstr), el_tempcluster);
//				}
//				if (rules.edgelist[k] >= 0)
//				{
//					infstr = initinfstr();
//					formatinfstr(infstr, x_("e%s %s %s %s"), frtoa(rules.edgelist[k]),
//						rules.layernames[i], rules.layernames[j],
//							rules.edgelistR[k]);
//					(void)allocstring(&drclist[drcindex++], returninfstr(infstr), el_tempcluster);
//				}
//				k++;
//			}
//	
//			(void)setval((INTBIG)lib, VLIBRARY, x_("EDTEC_DRC"), (INTBIG)drclist,
//				VSTRING|VISARRAY|(drccount<<VLENGTHSH));
//			for(i=0; i<drccount; i++) efree(drclist[i]);
//			efree((CHAR *)drclist);
//		} else
//		{
//			// no rules: remove the variable
//			if (getval((INTBIG)lib, VLIBRARY, VSTRING|VISARRAY, x_("EDTEC_DRC")) != NOVARIABLE)
//				(void)delval((INTBIG)lib, VLIBRARY, x_("EDTEC_DRC"));
//		}
//	}
//	
//	/*
//	 * routine for manipulating color maps
//	 */
//	void us_teceditcolormap(void)
//	{
//		REGISTER INTBIG i, k, total, dependentlibcount, *printcolors;
//		INTBIG func, drcminwid, height3d, thick3d, printcol[5];
//		CHAR *layerlabel[5], *layerabbrev[5], *cif, *gds, *layerletters, *dxf, **layernames, line[50];
//		LIBRARY **dependentlibs;
//		GRAPHICS desc;
//		NODEPROTO **sequence;
//		REGISTER NODEINST *ni;
//		REGISTER NODEPROTO *np;
//		REGISTER VARIABLE *var;
//		float spires, spicap, spiecap;
//		REGISTER INTBIG *mapptr, *newmap;
//		REGISTER VARIABLE *varred, *vargreen, *varblue;
//	
//		if (us_needwindow()) return;
//	
//		// load the color map of the technology
//		us_tecedloadlibmap(el_curlib);
//	
//		dependentlibcount = us_teceditgetdependents(el_curlib, &dependentlibs);
//		total = us_teceditfindsequence(dependentlibs, dependentlibcount, x_("layer-"),
//			x_("EDTEC_layersequence"), &sequence);
//		printcolors = (INTBIG *)emalloc(total*5*SIZEOFINTBIG, us_tool.cluster);
//		if (printcolors == 0) return;
//		layernames = (CHAR **)emalloc(total * (sizeof (CHAR *)), us_tool.cluster);
//		if (layernames == 0) return;
//	
//		// now fill in real layer names if known
//		for(i=0; i<5; i++) layerlabel[i] = 0;
//		for(i=0; i<total; i++)
//		{
//			np = sequence[i];
//			cif = layerletters = gds = 0;
//			if (us_teceditgetlayerinfo(np, &desc, &cif, &func, &layerletters,
//				&dxf, &gds, &spires, &spicap, &spiecap, &drcminwid, &height3d,
//					&thick3d, printcol)) return;
//			for(k=0; k<5; k++) printcolors[i*5+k] = printcol[k];
//			layernames[i] = &np.protoname[6];
//			switch (desc.bits)
//			{
//				case LAYERT1: k = 0;   break;
//				case LAYERT2: k = 1;   break;
//				case LAYERT3: k = 2;   break;
//				case LAYERT4: k = 3;   break;
//				case LAYERT5: k = 4;   break;
//				default:      k = -1;  break;
//			}
//			if (k >= 0)
//			{
//				if (layerlabel[k] == 0)
//				{
//					layerlabel[k] = &np.protoname[6];
//					layerabbrev[k] = (CHAR *)emalloc(2 * SIZEOFCHAR, el_tempcluster);
//					layerabbrev[k][0] = *layerletters;
//					layerabbrev[k][1] = 0;
//				}
//			}
//			if (gds != 0) efree(gds);
//			if (cif != 0) efree(cif);
//			if (layerletters != 0) efree(layerletters);
//		}
//	
//		// set defaults
//		if (layerlabel[0] == 0)
//		{
//			layerlabel[0] = _("Ovrlap 1");
//			(void)allocstring(&layerabbrev[0], x_("1"), el_tempcluster);
//		}
//		if (layerlabel[1] == 0)
//		{
//			layerlabel[1] = _("Ovrlap 2");
//			(void)allocstring(&layerabbrev[1], x_("2"), el_tempcluster);
//		}
//		if (layerlabel[2] == 0)
//		{
//			layerlabel[2] = _("Ovrlap 3");
//			(void)allocstring(&layerabbrev[2], x_("3"), el_tempcluster);
//		}
//		if (layerlabel[3] == 0)
//		{
//			layerlabel[3] = _("Ovrlap 4");
//			(void)allocstring(&layerabbrev[3], x_("4"), el_tempcluster);
//		}
//		if (layerlabel[4] == 0)
//		{
//			layerlabel[4] = _("Ovrlap 5");
//			(void)allocstring(&layerabbrev[4], x_("5"), el_tempcluster);
//		}
//	
//		// run the color mixing palette
//		if (us_colormixdlog(layerlabel, total, layernames, printcolors))
//		{
//			// update all of the layer cells
//			for(i=0; i<total; i++)
//			{
//				np = sequence[i];
//				for(ni = np.firstnodeinst; ni != NONODEINST; ni = ni.nextnodeinst)
//				{
//					var = getvalkey((INTBIG)ni, VNODEINST, VINTEGER, us_edtec_option_key);
//					if (var == NOVARIABLE) continue;
//					if (var.addr != LAYERPRINTCOL) continue;
//					(void)esnprintf(line, 50, x_("%s%ld,%ld,%ld, %ld,%s"), TECEDNODETEXTPRINTCOL,
//						printcolors[i*5], printcolors[i*5+1], printcolors[i*5+2],
//							printcolors[i*5+3], (printcolors[i*5+4]==0 ? x_("off") : x_("on")));
//					startobjectchange((INTBIG)ni, VNODEINST);
//					(void)setvalkey((INTBIG)ni, VNODEINST, art_messagekey, (INTBIG)line,
//						VSTRING|VDISPLAY);
//					endobjectchange((INTBIG)ni, VNODEINST);
//				}
//			}
//		}
//		for(i=0; i<5; i++) efree(layerabbrev[i]);
//	
//		// save the map on the library
//		varred = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER|VISARRAY, us_colormap_red_key);
//		vargreen = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER|VISARRAY, us_colormap_green_key);
//		varblue = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER|VISARRAY, us_colormap_blue_key);
//		if (varred != NOVARIABLE && vargreen != NOVARIABLE && varblue != NOVARIABLE)
//		{
//			newmap = emalloc(256*3*SIZEOFINTBIG, el_tempcluster);
//			mapptr = newmap;
//			for(i=0; i<256; i++)
//			{
//				*mapptr++ = ((INTBIG *)varred.addr)[i];
//				*mapptr++ = ((INTBIG *)vargreen.addr)[i];
//				*mapptr++ = ((INTBIG *)varblue.addr)[i];
//			}
//			(void)setval((INTBIG)el_curlib, VLIBRARY, x_("EDTEC_colormap"), (INTBIG)newmap,
//				VINTEGER|VISARRAY|((256*3)<<VLENGTHSH));
//			efree((CHAR *)newmap);
//		}
//		efree((CHAR *)layernames);
//		efree((CHAR *)printcolors);
//	}
//	
//	/*
//	 * routine for creating a new layer with shape "pp"
//	 */
//	void us_teceditcreat(INTBIG count, CHAR *par[])
//	{
//		REGISTER INTBIG l;
//		REGISTER CHAR *name, *pp;
//		CHAR *subpar[3];
//		REGISTER NODEINST *ni;
//		REGISTER NODEPROTO *np, *savenp, *cell;
//		HIGHLIGHT high;
//		REGISTER VARIABLE *var;
//	
//		l = estrlen(pp = par[0]);
//		np = NONODEPROTO;
//		if (namesamen(pp, x_("port"), l) == 0 && l >= 1) np = gen_portprim;
//		if (namesamen(pp, x_("highlight"), l) == 0 && l >= 1) np = art_boxprim;
//		if (namesamen(pp, x_("rectangle-filled"), l) == 0 && l >= 11) np = art_filledboxprim;
//		if (namesamen(pp, x_("rectangle-outline"), l) == 0 && l >= 11) np = art_boxprim;
//		if (namesamen(pp, x_("rectangle-crossed"), l) == 0 && l >= 11) np = art_crossedboxprim;
//		if (namesamen(pp, x_("polygon-filled"), l) == 0 && l >= 9) np = art_filledpolygonprim;
//		if (namesamen(pp, x_("polygon-outline"), l) == 0 && l >= 9) np = art_closedpolygonprim;
//		if (namesamen(pp, x_("lines-solid"), l) == 0 && l >= 7) np = art_openedpolygonprim;
//		if (namesamen(pp, x_("lines-dotted"), l) == 0 && l >= 8) np = art_openeddottedpolygonprim;
//		if (namesamen(pp, x_("lines-dashed"), l) == 0 && l >= 8) np = art_openeddashedpolygonprim;
//		if (namesamen(pp, x_("lines-thicker"), l) == 0 && l >= 7) np = art_openedthickerpolygonprim;
//		if (namesamen(pp, x_("circle-outline"), l) == 0 && l >= 8) np = art_circleprim;
//		if (namesamen(pp, x_("circle-filled"), l) == 0 && l >= 8) np = art_filledcircleprim;
//		if (namesamen(pp, x_("circle-half"), l) == 0 && l >= 8) np = art_circleprim;
//		if (namesamen(pp, x_("circle-arc"), l) == 0 && l >= 8) np = art_circleprim;
//		if (namesamen(pp, x_("text"), l) == 0 && l >= 1) np = gen_invispinprim;
//	
//		if (np == NONODEPROTO)
//		{
//			ttyputerr(_("Unrecoginzed shape: '%s'"), pp);
//			return;
//		}
//	
//		// make sure the cell is right
//		cell = us_needcell();
//		if (cell == NONODEPROTO) return;
//		if (namesamen(cell.protoname, x_("node-"), 5) != 0 &&
//			namesamen(cell.protoname, x_("arc-"), 4) != 0)
//		{
//			us_abortcommand(_("Must be editing a node or arc to place geometry"));
//			if ((us_tool.toolstate&NODETAILS) == 0)
//				ttyputmsg(_("Use 'edit-node' or 'edit-arc' options"));
//			return;
//		}
//		if (np == gen_portprim &&
//			namesamen(cell.protoname, x_("node-"), 5) != 0)
//		{
//			us_abortcommand(_("Can only place ports in node descriptions"));
//			if ((us_tool.toolstate&NODETAILS) == 0)
//				ttyputmsg(_("Use the 'edit-node' options"));
//			return;
//		}
//	
//		// create the node
//		us_clearhighlightcount();
//		savenp = us_curnodeproto;
//		us_curnodeproto = np;
//		subpar[0] = x_("wait-for-down");
//		us_create(1, subpar);
//		us_curnodeproto = savenp;
//	
//		var = getvalkey((INTBIG)us_tool, VTOOL, VSTRING|VISARRAY, us_highlightedkey);
//		if (var == NOVARIABLE) return;
//		(void)us_makehighlight(((CHAR **)var.addr)[0], &high);
//		if (high.fromgeom == NOGEOM) return;
//		if (!high.fromgeom.entryisnode) return;
//		ni = high.fromgeom.entryaddr.ni;
//		(void)setvalkey((INTBIG)ni, VNODEINST, us_edtec_option_key, LAYERPATCH, VINTEGER);
//	
//		// postprocessing on the nodes
//		if (namesamen(pp, x_("port"), l) == 0 && l >= 1)
//		{
//			// a port layer
//			if (count == 1)
//			{
//				name = ttygetline(M_("Port name: "));
//				if (name == 0 || name[0] == 0)
//				{
//					us_abortedmsg();
//					return;
//				}
//			} else name = par[1];
//			var = setval((INTBIG)ni, VNODEINST, x_("EDTEC_portname"), (INTBIG)name, VSTRING|VDISPLAY);
//			if (var != NOVARIABLE) defaulttextdescript(var.textdescript, ni.geom);
//			if ((us_tool.toolstate&NODETAILS) == 0)
//				ttyputmsg(_("Use 'change' option to set arc connectivity and port angle"));
//		}
//		if (namesamen(pp, x_("highlight"), l) == 0 && l >= 1)
//		{
//			// a highlight layer
//			us_teceditsetpatch(ni, &us_edtechigh);
//			(void)setval((INTBIG)ni, VNODEINST, x_("EDTEC_layer"), (INTBIG)NONODEPROTO, VNODEPROTO);
//			ttyputmsg(_("Keep highlight a constant distance from the example edge"));
//		}
//		if (namesamen(pp, x_("circle-half"), l) == 0 && l >= 8)
//			setarcdegrees(ni, 0.0, 180.0*EPI/180.0);
//		if ((us_tool.toolstate&NODETAILS) == 0)
//		{
//			if (namesamen(pp, x_("rectangle-"), 10) == 0)
//				ttyputmsg(_("Use 'change' option to set a layer for this shape"));
//			if (namesamen(pp, x_("polygon-"), 8) == 0)
//			{
//				ttyputmsg(_("Use 'change' option to set a layer for this shape"));
//				ttyputmsg(_("Use 'outline edit' to describe polygonal shape"));
//			}
//			if (namesamen(pp, x_("lines-"), 6) == 0)
//				ttyputmsg(_("Use 'change' option to set a layer for this line"));
//			if (namesamen(pp, x_("circle-"), 7) == 0)
//				ttyputmsg(_("Use 'change' option to set a layer for this circle"));
//			if (namesamen(pp, x_("text"), l) == 0 && l >= 1)
//			{
//				ttyputmsg(_("Use 'change' option to set a layer for this text"));
//				ttyputmsg(_("Use 'var textedit ~.ART_message' command to set text"));
//				ttyputmsg(_("Then use 'var change ~.ART_message display' command"));
//			}
//		}
//		if (namesamen(pp, x_("circle-arc"), l) == 0 && l >= 8)
//		{
//			setarcdegrees(ni, 0.0, 45.0*EPI/180.0);
//			if ((us_tool.toolstate&NODETAILS) == 0)
//				ttyputmsg(_("Use 'setarc' command to set portion of circle"));
//		}
//		if ((ni.proto.userbits&HOLDSTRACE) != 0)
//		{
//			// give it real points if it holds an outline
//			subpar[0] = x_("trace");
//			subpar[1] = x_("init-points");
//			us_node(2, subpar);
//		}
//	}
//	
//	/*
//	 * routine to highlight information about all layers (or ports if "doports" is true)
//	 */
//	void us_teceditidentify(BOOLEAN doports)
//	{
//		REGISTER NODEPROTO *np;
//		REGISTER INTBIG total, qtotal, i, j, bestrot, indent;
//		REGISTER INTBIG xsep, ysep, *xpos, *ypos, dist, bestdist, *style;
//		INTBIG lx, hx, ly, hy;
//		REGISTER EXAMPLE *nelist;
//		REGISTER SAMPLE *ns, **whichsam;
//		static POLYGON *poly = NOPOLYGON;
//		extern GRAPHICS us_hbox;
//	
//		np = us_needcell();
//		if (np == NONODEPROTO) return;
//	
//		if (doports)
//		{
//			if (namesamen(np.protoname, x_("node-"), 5) != 0)
//			{
//				us_abortcommand(_("Must be editing a node to identify ports"));
//				if ((us_tool.toolstate&NODETAILS) == 0)
//					ttyputmsg(M_("Use the 'edit-node' option"));
//				return;
//			}
//		} else
//		{
//			if (namesamen(np.protoname, x_("node-"), 5) != 0 &&
//				namesamen(np.protoname, x_("arc-"), 4) != 0)
//			{
//				us_abortcommand(_("Must be editing a node or arc to identify layers"));
//				if ((us_tool.toolstate&NODETAILS) == 0)
//					ttyputmsg(M_("Use 'edit-node' or 'edit-arc' options"));
//				return;
//			}
//		}
//	
//		// get examples
//		if (namesamen(np.protoname, x_("node-"), 5) == 0)
//			nelist = us_tecedgetexamples(np, TRUE); else
//				nelist = us_tecedgetexamples(np, FALSE);
//		if (nelist == NOEXAMPLE) return;
//	
//		// count the number of appropriate samples in the main example
//		total = 0;
//		for(ns = nelist.firstsample; ns != NOSAMPLE; ns = ns.nextsample)
//		{
//			if (!doports)
//			{
//				if (ns.layer != gen_portprim) total++;
//			} else
//			{
//				if (ns.layer == gen_portprim) total++;
//			}
//		}
//		if (total == 0)
//		{
//			us_tecedfreeexamples(nelist);
//			us_abortcommand(_("There are no %s to identify"), (!doports ? _("layers") : _("ports")));
//			return;
//		}
//	
//		// make arrays for position and association
//		xpos = (INTBIG *)emalloc(total * SIZEOFINTBIG, el_tempcluster);
//		if (xpos == 0) return;
//		ypos = (INTBIG *)emalloc(total * SIZEOFINTBIG, el_tempcluster);
//		if (ypos == 0) return;
//		style = (INTBIG *)emalloc(total * SIZEOFINTBIG, el_tempcluster);
//		if (style == 0) return;
//		whichsam = (SAMPLE **)emalloc(total * (sizeof (SAMPLE *)), el_tempcluster);
//		if (whichsam == 0) return;
//	
//		// fill in label positions
//		qtotal = (total+3) / 4;
//		ysep = (el_curwindowpart.screenhy-el_curwindowpart.screenly) / qtotal;
//		xsep = (el_curwindowpart.screenhx-el_curwindowpart.screenlx) / qtotal;
//		indent = (el_curwindowpart.screenhy-el_curwindowpart.screenly) / 15;
//		for(i=0; i<qtotal; i++)
//		{
//			// label on the left side
//			xpos[i] = el_curwindowpart.screenlx + indent;
//			ypos[i] = el_curwindowpart.screenly + ysep * i + ysep/2;
//			style[i] = TEXTLEFT;
//			if (i+qtotal < total)
//			{
//				// label on the top side
//				xpos[i+qtotal] = el_curwindowpart.screenlx + xsep * i + xsep/2;
//				ypos[i+qtotal] = el_curwindowpart.screenhy - indent;
//				style[i+qtotal] = TEXTTOP;
//			}
//			if (i+qtotal*2 < total)
//			{
//				// label on the right side
//				xpos[i+qtotal*2] = el_curwindowpart.screenhx - indent;
//				ypos[i+qtotal*2] = el_curwindowpart.screenly + ysep * i + ysep/2;
//				style[i+qtotal*2] = TEXTRIGHT;
//			}
//			if (i+qtotal*3 < total)
//			{
//				// label on the bottom side
//				xpos[i+qtotal*3] = el_curwindowpart.screenlx + xsep * i + xsep/2;
//				ypos[i+qtotal*3] = el_curwindowpart.screenly + indent;
//				style[i+qtotal*3] = TEXTBOT;
//			}
//		}
//	
//		// fill in sample associations
//		i = 0;
//		for(ns = nelist.firstsample; ns != NOSAMPLE; ns = ns.nextsample)
//		{
//			if (!doports)
//			{
//				if (ns.layer != gen_portprim) whichsam[i++] = ns;
//			} else
//			{
//				if (ns.layer == gen_portprim) whichsam[i++] = ns;
//			}
//		}
//	
//		// rotate through all configurations, finding least distance
//		bestdist = MAXINTBIG;
//		for(i=0; i<total; i++)
//		{
//			// find distance from each label to its sample center
//			dist = 0;
//			for(j=0; j<total; j++)
//				dist += computedistance(xpos[j], ypos[j], whichsam[j].xpos, whichsam[j].ypos);
//			if (dist < bestdist)
//			{
//				bestdist = dist;
//				bestrot = i;
//			}
//	
//			// rotate the samples
//			ns = whichsam[0];
//			for(j=1; j<total; j++) whichsam[j-1] = whichsam[j];
//			whichsam[total-1] = ns;
//		}
//	
//		// rotate back to the best orientation
//		for(i=0; i<bestrot; i++)
//		{
//			ns = whichsam[0];
//			for(j=1; j<total; j++) whichsam[j-1] = whichsam[j];
//			whichsam[total-1] = ns;
//		}
//	
//		// get polygon
//		(void)needstaticpolygon(&poly, 2, us_tool.cluster);
//	
//		// draw the highlighting
//		us_clearhighlightcount();
//		for(i=0; i<total; i++)
//		{
//			ns = whichsam[i];
//			poly.xv[0] = xpos[i];
//			poly.yv[0] = ypos[i];
//			poly.count = 1;
//			if (ns.layer == NONODEPROTO)
//			{
//				poly.string = x_("HIGHLIGHT");
//			} else if (ns.layer == gen_cellcenterprim)
//			{
//				poly.string = x_("GRAB");
//			} else if (ns.layer == gen_portprim)
//			{
//				poly.string = us_tecedgetportname(ns.node);
//				if (poly.string == 0) poly.string = x_("?");
//			} else poly.string = &ns.layer.protoname[6];
//			poly.desc = &us_hbox;
//			poly.style = style[i];
//			TDCLEAR(poly.textdescript);
//			TDSETSIZE(poly.textdescript, TXTSETQLAMBDA(4));
//			poly.tech = el_curtech;
//			us_hbox.col = HIGHLIT;
//	
//			nodesizeoffset(ns.node, &lx, &ly, &hx, &hy);
//			switch (poly.style)
//			{
//				case TEXTLEFT:
//					poly.xv[1] = ns.node.lowx+lx;
//					poly.yv[1] = (ns.node.lowy + ns.node.highy) / 2;
//					poly.style = TEXTBOTLEFT;
//					break;
//				case TEXTRIGHT:
//					poly.xv[1] = ns.node.highx-hx;
//					poly.yv[1] = (ns.node.lowy + ns.node.highy) / 2;
//					poly.style = TEXTBOTRIGHT;
//					break;
//				case TEXTTOP:
//					poly.xv[1] = (ns.node.lowx + ns.node.highx) / 2;
//					poly.yv[1] = ns.node.highy-hy;
//					poly.style = TEXTTOPLEFT;
//					break;
//				case TEXTBOT:
//					poly.xv[1] = (ns.node.lowx + ns.node.highx) / 2;
//					poly.yv[1] = ns.node.lowy+ly;
//					poly.style = TEXTBOTLEFT;
//					break;
//			}
//			us_showpoly(poly, el_curwindowpart);
//	
//			// now draw the vector polygon
//			poly.count = 2;
//			poly.style = VECTORS;
//			us_showpoly(poly, el_curwindowpart);
//		}
//	
//		// free rotation arrays
//		efree((CHAR *)xpos);
//		efree((CHAR *)ypos);
//		efree((CHAR *)style);
//		efree((CHAR *)whichsam);
//	
//		// free all examples
//		us_tecedfreeexamples(nelist);
//	}
//	
//	/*
//	 * routine to print information about selected object
//	 */
//	void us_teceditinquire(void)
//	{
//		REGISTER NODEINST *ni;
//		REGISTER ARCINST *ai;
//		REGISTER NODEPROTO *np;
//		REGISTER CHAR *pt1, *pt2, *pt;
//		REGISTER VARIABLE *var;
//		REGISTER INTBIG opt;
//		REGISTER HIGHLIGHT *high;
//	
//		high = us_getonehighlight();
//		if ((high.status&HIGHTYPE) != HIGHTEXT && (high.status&HIGHTYPE) != HIGHFROM)
//		{
//			us_abortcommand(_("Must select a single object for inquiry"));
//			return;
//		}
//		if ((high.status&HIGHTYPE) == HIGHFROM && !high.fromgeom.entryisnode)
//		{
//			// describe currently highlighted arc
//			ai = high.fromgeom.entryaddr.ai;
//			if (ai.proto != gen_universalarc)
//			{
//				ttyputmsg(_("This is an unimportant %s arc"), describearcproto(ai.proto));
//				return;
//			}
//			if (ai.end[0].nodeinst.proto != gen_portprim ||
//				ai.end[1].nodeinst.proto != gen_portprim)
//			{
//				ttyputmsg(_("This arc makes an unimportant connection"));
//				return;
//			}
//			pt1 = us_tecedgetportname(ai.end[0].nodeinst);
//			pt2 = us_tecedgetportname(ai.end[1].nodeinst);
//			if (pt1 == 0 || pt2 == 0)
//				ttyputmsg(_("This arc connects two port objects")); else
//					ttyputmsg(_("This arc connects ports '%s' and '%s'"), pt1, pt2);
//			return;
//		}
//		ni = high.fromgeom.entryaddr.ni;
//		np = ni.parent;
//		opt = us_tecedgetoption(ni);
//		if (opt < 0)
//		{
//			ttyputmsg(_("This object has no relevance to technology editing"));
//			return;
//		}
//	
//		switch (opt)
//		{
//			case ARCFIXANG:
//				ttyputmsg(_("This object defines the fixed-angle factor of %s"), describenodeproto(np));
//				break;
//			case ARCFUNCTION:
//				ttyputmsg(_("This object defines the function of %s"), describenodeproto(np));
//				break;
//			case ARCINC:
//				ttyputmsg(_("This object defines the prefered angle increment of %s"), describenodeproto(np));
//				break;
//			case ARCNOEXTEND:
//				ttyputmsg(_("This object defines the arc extension of %s"), describenodeproto(np));
//				break;
//			case ARCWIPESPINS:
//				ttyputmsg(_("This object defines the arc coverage of %s"), describenodeproto(np));
//				break;
//			case CENTEROBJ:
//				ttyputmsg(_("This object identifies the grab point of %s"), describenodeproto(np));
//				break;
//			case LAYER3DHEIGHT:
//				ttyputmsg(_("This object defines the 3D height of %s"), describenodeproto(np));
//				break;
//			case LAYER3DTHICK:
//				ttyputmsg(_("This object defines the 3D thickness of %s"), describenodeproto(np));
//				break;
//			case LAYERPRINTCOL:
//				ttyputmsg(_("This object defines the print colors of %s"), describenodeproto(np));
//				break;
//			case LAYERCIF:
//				ttyputmsg(_("This object defines the CIF name of %s"), describenodeproto(np));
//				break;
//			case LAYERCOLOR:
//				ttyputmsg(_("This object defines the color of %s"), describenodeproto(np));
//				break;
//			case LAYERDXF:
//				ttyputmsg(_("This object defines the DXF name(s) of %s"), describenodeproto(np));
//				break;
//			case LAYERDRCMINWID:
//				ttyputmsg(_("This object defines the minimum DRC width of %s (OBSOLETE)"), describenodeproto(np));
//				break;
//			case LAYERFUNCTION:
//				ttyputmsg(_("This object defines the function of %s"), describenodeproto(np));
//				break;
//			case LAYERGDS:
//				ttyputmsg(_("This object defines the Calma GDS-II number of %s"), describenodeproto(np));
//				break;
//			case LAYERLETTERS:
//				ttyputmsg(_("This object defines the letters to use for %s"), describenodeproto(np));
//				break;
//			case LAYERPATCONT:
//				ttyputmsg(_("This object provides control of the stipple pattern in %s"), describenodeproto(np));
//				break;
//			case LAYERPATTERN:
//				ttyputmsg(_("This is one of the bitmap squares in %s"), describenodeproto(np));
//				break;
//			case LAYERSPICAP:
//				ttyputmsg(_("This object defines the SPICE capacitance of %s"), describenodeproto(np));
//				break;
//			case LAYERSPIECAP:
//				ttyputmsg(_("This object defines the SPICE edge capacitance of %s"), describenodeproto(np));
//				break;
//			case LAYERSPIRES:
//				ttyputmsg(_("This object defines the SPICE resistance of %s"), describenodeproto(np));
//				break;
//			case LAYERSTYLE:
//				ttyputmsg(_("This object defines the style of %s"), describenodeproto(np));
//				break;
//			case LAYERPATCH:
//			case HIGHLIGHTOBJ:
//				np = us_tecedgetlayer(ni);
//				if (np == 0)
//					ttyputerr(_("This is an object with no valid layer!")); else
//				{
//					if (np == NONODEPROTO) ttyputmsg(_("This is a highlight box")); else
//						ttyputmsg(_("This is a '%s' layer"), &np.protoname[6]);
//					var = getval((INTBIG)ni, VNODEINST, VSTRING, x_("EDTEC_minbox"));
//					if (var != NOVARIABLE)
//						ttyputmsg(_("   It is at minimum size"));
//				}
//				break;
//			case NODEFUNCTION:
//				ttyputmsg(_("This object defines the function of %s"), describenodeproto(np));
//				break;
//			case NODELOCKABLE:
//				ttyputmsg(_("This object tells if %s can be locked (used in array technologies)"),
//					describenodeproto(np));
//				break;
//			case NODEMULTICUT:
//				ttyputmsg(_("This object tells the separation between multiple contact cuts in %s"),
//					describenodeproto(np));
//				break;
//			case NODESERPENTINE:
//				ttyputmsg(_("This object tells if %s is a serpentine transistor"), describenodeproto(np));
//				break;
//			case NODESQUARE:
//				ttyputmsg(_("This object tells if %s is square"), describenodeproto(np));
//				break;
//			case NODEWIPES:
//				ttyputmsg(_("This object tells if %s disappears when conencted to one or two arcs"),
//					describenodeproto(np));
//				break;
//			case PORTOBJ:
//				pt = us_tecedgetportname(ni);
//				if (pt == 0) ttyputmsg(_("This is a port object")); else
//					ttyputmsg(_("This is port '%s'"), pt);
//				break;
//			case TECHDESCRIPT:
//				ttyputmsg(_("This object contains the technology description"));
//				break;
//			case TECHLAMBDA:
//				ttyputmsg(_("This object defines the value of lambda"));
//				break;
//			default:
//				ttyputerr(_("This object has unknown information"));
//				break;
//		}
//	}
//	
//	/*
//	 * Routine to return a brief description of node "ni" for use in the status area.
//	 */
//	CHAR *us_teceddescribenode(NODEINST *ni)
//	{
//		REGISTER INTBIG opt;
//		REGISTER NODEPROTO *np;
//		REGISTER void *infstr;
//		REGISTER CHAR *pt;
//	
//		opt = us_tecedgetoption(ni);
//		if (opt < 0) return(0);
//		switch (opt)
//		{
//			case ARCFIXANG:      return(_("Arc fixed-angle factor"));
//			case ARCFUNCTION:    return(_("Arc function"));
//			case ARCINC:         return(_("Arc angle increment"));
//			case ARCNOEXTEND:    return(_("Arc extension"));
//			case ARCWIPESPINS:   return(_("Arc coverage"));
//			case CENTEROBJ:      return(_("Grab point"));
//			case LAYER3DHEIGHT:  return(_("3D height"));
//			case LAYER3DTHICK:   return(_("3D thickness"));
//			case LAYERPRINTCOL:  return(_("Print colors"));
//			case LAYERCIF:       return(_("CIF names"));
//			case LAYERCOLOR:     return(_("Layer color"));
//			case LAYERDXF:       return(_("DXF name(s)"));
//			case LAYERFUNCTION:  return(_("Layer function"));
//			case LAYERGDS:       return(_("GDS-II number(s)"));
//			case LAYERLETTERS:   return(_("Layer letters"));
//			case LAYERPATCONT:   return(_("Pattern control"));
//			case LAYERPATTERN:   return(_("Stipple pattern element"));
//			case LAYERSPICAP:    return(_("Spice capacitance"));
//			case LAYERSPIECAP:   return(_("Spice edge capacitance"));
//			case LAYERSPIRES:    return(_("Spice resistance"));
//			case LAYERSTYLE:     return(_("Srawing style"));
//			case LAYERPATCH:
//			case HIGHLIGHTOBJ:
//				np = us_tecedgetlayer(ni);
//				if (np == 0) return(_("Unknown layer"));
//				if (np == NONODEPROTO) return(_("Highlight box"));
//				infstr = initinfstr();
//				formatinfstr(infstr, _("Layer %s"), &np.protoname[6]);
//				return(returninfstr(infstr));
//			case NODEFUNCTION:   return(_("Node function"));
//			case NODELOCKABLE:   return(_("Node lockability"));
//			case NODEMULTICUT:   return(_("Multicut separation"));
//			case NODESERPENTINE: return(_("Serpentine transistor"));
//			case NODESQUARE:     return(_("Square node"));
//			case NODEWIPES:      return(_("Disappearing pin"));
//			case PORTOBJ:
//				pt = us_tecedgetportname(ni);
//				if (pt == 0) return(_("Unnamed export"));
//				infstr = initinfstr();
//				formatinfstr(infstr, _("Export %s"), pt);
//				return(returninfstr(infstr));
//			case TECHDESCRIPT:   return(_("Technology description"));
//			case TECHLAMBDA:     return(_("Lambda value"));
//		}
//		return(0);
//	}
	
	/**
	 * Method for modifying the selected object.  If two are selected, connect them.
	 */
	public static void us_teceditmodobject(EditWindow wnd, NodeInst ni, int opt)
	{
//		REGISTER NODEINST *ni;
//		GEOM *fromgeom, *togeom, **list;
//		PORTPROTO *fromport, *toport;
//		INTBIG opt, textcount, i, found;
//		REGISTER HIGHLIGHT *high;
//		HIGHLIGHT newhigh;
//		CHAR *newpar[2], **textinfo;
	
		// special case for port modification: reset highlighting by hand
//		if (opt == PORTOBJ)
//		{
//			// pick up old highlight values and then remove highlighting
//			newhigh = *high;
//			us_clearhighlightcount();
//	
//			// modify the port
//			us_tecedmodport(ni, count, par);
//	
//			// set new highlighting variable
//			newhigh.fromvar = getval((INTBIG)ni, VNODEINST, VSTRING, x_("EDTEC_portname"));
//			if (newhigh.fromvar == NOVARIABLE)
//				newhigh.fromvar = getvalkey((INTBIG)ni, VNODEINST, VSTRING, el_node_name_key);
//			us_addhighlight(&newhigh);
//			return;
//		}

		// handle other cases
		switch (opt)
		{
			case Generate.LAYERFUNCTION:     us_tecedlayerfunction(wnd, ni);  break;
			case Generate.LAYERCOLOR:        us_tecedlayercolor(wnd, ni);     break;
			case Generate.LAYERTRANSPARENCY: us_tecedlayertransparency(wnd, ni);  break;
			case Generate.LAYERSTYLE:        us_tecedlayerstyle(wnd, ni);     break;
			case Generate.LAYERCIF:          us_tecedlayercif(wnd, ni);       break;
			case Generate.LAYERGDS:          us_tecedlayergds(wnd, ni);       break;
			case Generate.LAYERDXF:          us_tecedlayerdxf(wnd, ni);       break;
			case Generate.LAYERSPIRES:       us_tecedlayerspires(wnd, ni);    break;
			case Generate.LAYERSPICAP:       us_tecedlayerspicap(wnd, ni);    break;
			case Generate.LAYERSPIECAP:      us_tecedlayerspiecap(wnd, ni);   break;
			case Generate.LAYER3DHEIGHT:     us_tecedlayer3dheight(wnd, ni);  break;
			case Generate.LAYER3DTHICK:      us_tecedlayer3dthick(wnd, ni);   break;
			case Generate.LAYERPATTERN:      us_tecedlayerpattern(wnd, ni);   break;
			case Generate.LAYERPATCONT:      us_tecedlayerpatterncontrol(wnd, ni);   break;
//			case Generate.LAYERPATCH:        us_tecedlayertype(wnd, ni);      break;
			case Generate.ARCFIXANG:         us_tecedarcfixang(wnd, ni);      break;
			case Generate.ARCFUNCTION:       us_tecedarcfunction(wnd, ni);    break;
			case Generate.ARCINC:            us_tecedarcinc(wnd, ni);         break;
			case Generate.ARCNOEXTEND:       us_tecedarcnoextend(wnd, ni);    break;
			case Generate.ARCWIPESPINS:      us_tecedarcwipes(wnd, ni);       break;
			case Generate.NODEFUNCTION:      us_tecednodefunction(wnd, ni);   break;
			case Generate.NODELOCKABLE:      us_tecednodelockable(wnd, ni);   break;
			case Generate.NODEMULTICUT:      us_tecednodemulticut(wnd, ni);   break;
			case Generate.NODESERPENTINE:    us_tecednodeserpentine(wnd, ni); break;
			case Generate.NODESQUARE:        us_tecednodesquare(wnd, ni);     break;
			case Generate.NODEWIPES:         us_tecednodewipes(wnd, ni);      break;
			case Generate.TECHDESCRIPT:      us_tecedinfodescript(wnd, ni);   break;
			case Generate.TECHLAMBDA:        us_tecedinfolambda(wnd, ni);     break;
			default:             System.out.println("Cannot modify this object");   break;
		}
	}
	
	/***************************** OBJECT MODIFICATION *****************************/

	private static void us_tecedlayer3dheight(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		String newHei = PromptAt.showPromptAt(wnd, ni, "Change 3D Height",
			"New 3D height (depth) for this layer:", initialMsg);
		if (newHei != null) us_tecedsetnode(ni, "3D Height: " + newHei);
	}
	
	private static void us_tecedlayer3dthick(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		String newThk = PromptAt.showPromptAt(wnd, ni, "Change 3D Thickness",
			"New 3D thickness for this layer:", initialMsg);
		if (newThk != null) us_tecedsetnode(ni, "3D Thickness: " + newThk);
	}
	
	private static void us_tecedlayercolor(EditWindow wnd, NodeInst ni)
	{
		String initialString = getValueOnNode(ni);
		StringTokenizer st = new StringTokenizer(initialString, ",");
		if (st.countTokens() != 5)
		{
			System.out.println("Color information must have 5 fields, separated by commas");
			return;
		}
		PromptAt.Field [] fields = new PromptAt.Field[5];
		fields[0] = new PromptAt.Field("Red (0-255):", TextUtils.atoi(st.nextToken()));
		fields[1] = new PromptAt.Field("Green (0-255):", TextUtils.atoi(st.nextToken()));
		fields[2] = new PromptAt.Field("Blue (0-255):", TextUtils.atoi(st.nextToken()));
		fields[3] = new PromptAt.Field("Opacity (0-1):", TextUtils.atof(st.nextToken()));
		fields[4] = new PromptAt.Field("Foreground:", new String [] {"on", "off"}, st.nextToken());
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Color", fields);
		if (choice == null) return;
		int r = ((Integer)fields[0].getFinal()).intValue();
		int g = ((Integer)fields[1].getFinal()).intValue();
		int b = ((Integer)fields[2].getFinal()).intValue();
		double o = ((Double)fields[3].getFinal()).doubleValue();
		String oo = (String)fields[4].getFinal();
		us_tecedsetnode(ni, "Color: " + r + "," + g + "," + b + ", " + o + "," + oo);

		// redraw the demo layer in this cell
		us_tecedredolayergraphics(ni.getParent());
	}
	
	private static void us_tecedlayertransparency(EditWindow wnd, NodeInst ni)
	{
		String initialTransLayer = getValueOnNode(ni);
		String [] transNames = new String[11];
		transNames[0] = "none";
		transNames[1] = "layer 1";
		transNames[2] = "layer 2";
		transNames[3] = "layer 3";
		transNames[4] = "layer 4";
		transNames[5] = "layer 5";
		transNames[6] = "layer 6";
		transNames[7] = "layer 7";
		transNames[8] = "layer 8";
		transNames[9] = "layer 9";
		transNames[10] = "layer 10";
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Transparent Layer",
			"New transparent layer number for this layer:", initialTransLayer, transNames);
		if (choice == null) return;
		us_tecedsetnode(ni, "Transparency: " + choice);

		// redraw the demo layer in this cell
		us_tecedredolayergraphics(ni.getParent());
	}

	private static void us_tecedlayerstyle(EditWindow wnd, NodeInst ni)
	{
		String initialStyleName = getValueOnNode(ni);
		String [] styleNames = new String[3];
		styleNames[0] = "Solid";
		styleNames[1] = "Patterned";
		styleNames[2] = "Patterned/Outlined";
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Layer Drawing Style",
			"New drawing style for this layer:", initialStyleName, styleNames);
		if (choice == null) return;
		us_tecedsetnode(ni, "Style: " + choice);

		// redraw the demo layer in this cell
		us_tecedredolayergraphics(ni.getParent());
	}
	
	private static void us_tecedlayercif(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		String newCIF = PromptAt.showPromptAt(wnd, ni, "Change CIF layer name", "New CIF symbol for this layer:", initialMsg);
		if (newCIF != null) us_tecedsetnode(ni, "CIF Layer: " + newCIF);
	}
	
	private static void us_tecedlayerdxf(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		String newDXF = PromptAt.showPromptAt(wnd, ni, "Change DXF layer name", "New DXF symbol for this layer:", initialMsg);
		if (newDXF != null) us_tecedsetnode(ni, "DXF Layer(s): " + newDXF);
	}
	
	private static void us_tecedlayergds(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		String newGDS = PromptAt.showPromptAt(wnd, ni, "Change GDS layer name", "New GDS symbol for this layer:", initialMsg);
		if (newGDS != null) us_tecedsetnode(ni, "GDS-II Layer: " + newGDS);
	}
	
	private static void us_tecedlayerspires(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		String newRes = PromptAt.showPromptAt(wnd, ni, "Change SPICE Layer Resistance",
			"New SPICE resistance for this layer:", initialMsg);
		if (newRes != null) us_tecedsetnode(ni, "SPICE Resistance: " + newRes);
	}
	
	private static void us_tecedlayerspicap(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		String newCap = PromptAt.showPromptAt(wnd, ni, "Change SPICE Layer Capacitance",
			"New SPICE capacitance for this layer:", initialMsg);
		if (newCap != null) us_tecedsetnode(ni, "SPICE Capacitance: " + newCap);
	}
	
	private static void us_tecedlayerspiecap(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		String newCap = PromptAt.showPromptAt(wnd, ni, "Change SPICE Layer Edge Capacitance",
			"New SPICE edge capacitance for this layer:", initialMsg);
		if (newCap != null) us_tecedsetnode(ni, "SPICE Edge Capacitance: " + newCap);
	}

	private static void us_tecedlayerfunction(EditWindow wnd, NodeInst ni)
	{
		String initialFuncName = getValueOnNode(ni);
		int commaPos = initialFuncName.indexOf(',');
		if (commaPos >= 0) initialFuncName = initialFuncName.substring(0, commaPos);

		// make a list of all layer functions and extras
		List funs = Layer.Function.getFunctions();
		int [] extraBits = Layer.Function.getFunctionExtras();
		String [] functionNames = new String[funs.size() + extraBits.length];
		int j = 0;
		for(Iterator it = funs.iterator(); it.hasNext(); )
		{
			Layer.Function fun = (Layer.Function)it.next();
			functionNames[j++] = fun.toString();
		}
		for(int i=0; i<extraBits.length; i++)
			functionNames[j++] = Layer.Function.getExtraName(extraBits[i]);

		// prompt for a new layer function
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Layer Function", "New function for this layer:", initialFuncName, functionNames);
		if (choice == null) return;

		// see if the choice is an extra
		int thisExtraBit = -1;
		for(int i=0; i<extraBits.length; i++)
		{
			if (choice.equals(Layer.Function.getExtraName(extraBits[i]))) { thisExtraBit = extraBits[i];   break; }
		}

		LayerInfo li = Generate.LayerInfo.us_teceditgetlayerinfo(ni.getParent());
		if (li == null) return;
		if (thisExtraBit > 0)
		{
			// adding (or removing) an extra bit
			if ((li.funExtra & thisExtraBit) != 0) li.funExtra &= ~thisExtraBit; else
				li.funExtra |= thisExtraBit;
		} else
		{
			li.funExtra = 0;
			for(Iterator it = funs.iterator(); it.hasNext(); )
			{
				Layer.Function fun = (Layer.Function)it.next();
				if (fun.toString().equalsIgnoreCase(choice))
				{
					li.fun = fun;
					break;
				}
			}
		}
		us_tecedsetnode(ni, "Function: " + Generate.makeLayerFunctionName(li.fun, li.funExtra));
	}

//	private static void us_tecedlayerletters(EditWindow wnd, NodeInst ni)
//	{
//		REGISTER NODEPROTO *np;
//		REGISTER CHAR *pt;
//		REGISTER INTBIG i;
//		CHAR *cif, *layerletters, *dxf, *gds;
//		GRAPHICS desc;
//		float spires, spicap, spiecap;
//		INTBIG func, drcminwid, height3d, thick3d, printcol[5];
//		REGISTER void *infstr;
//	
//		if (par[0][0] == 0)
//		{
//			us_abortcommand(_("Layer letter(s) required"));
//			return;
//		}
//	
//		// check layer letters for uniqueness
//		for(np = el_curlib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//		{
//			if (namesamen(np.protoname, x_("layer-"), 6) != 0) continue;
//			cif = layerletters = gds = 0;
//			if (us_teceditgetlayerinfo(np, &desc, &cif, &func, &layerletters,
//				&dxf, &gds, &spires, &spicap, &spiecap, &drcminwid, &height3d,
//					&thick3d, printcol)) return;
//			if (gds != 0) efree(gds);
//			if (cif != 0) efree(cif);
//			if (layerletters == 0) continue;
//	
//			// check these layer letters for uniqueness
//			for(pt = layerletters; *pt != 0; pt++)
//			{
//				for(i=0; par[0][i] != 0; i++) if (par[0][i] == *pt)
//				{
//					us_abortcommand(_("Cannot use letter '%c', it is in %s"), *pt, describenodeproto(np));
//					efree(layerletters);
//					return;
//				}
//			}
//			efree(layerletters);
//		}
//	
//		infstr = initinfstr();
//		addstringtoinfstr(infstr, TECEDNODETEXTLETTERS);
//		addstringtoinfstr(infstr, par[0]);
//		us_tecedsetnode(ni, returninfstr(infstr));
//	}
	
	private static void us_tecedlayerpatterncontrol(EditWindow wnd, NodeInst ni)
	{
//		REGISTER INTBIG len;
//		REGISTER NODEINST *pni;
//		REGISTER INTBIG opt;
//		UINTSML color;
//		static GRAPHICS desc;
//		static BOOLEAN didcopy = FALSE;
//		CHAR *cif, *layerletters, *gds, *dxf;
//		INTBIG func, drcminwid, height3d, thick3d, printcol[5];
//		float spires, spicap, spiecap;
	
//		len = estrlen(par[0]);
//		if (namesamen(par[0], x_("Clear Pattern"), len) == 0 && len > 2)
//		{
//			us_clearhighlightcount();
//			for(pni = ni.parent.firstnodeinst; pni != NONODEINST; pni = pni.nextnodeinst)
//			{
//				opt = us_tecedgetoption(pni);
//				if (opt != LAYERPATTERN) continue;
//				color = us_tecedlayergetpattern(pni);
//				if (color != 0)
//					us_tecedlayersetpattern(pni, 0);
//			}
//	
//			// redraw the demo layer in this cell
//			us_tecedredolayergraphics(ni.parent);
//			return;
//		}
//		if (namesamen(par[0], x_("Invert Pattern"), len) == 0 && len > 1)
//		{
//			us_clearhighlightcount();
//			for(pni = ni.parent.firstnodeinst; pni != NONODEINST; pni = pni.nextnodeinst)
//			{
//				opt = us_tecedgetoption(pni);
//				if (opt != LAYERPATTERN) continue;
//				color = us_tecedlayergetpattern(pni);
//				us_tecedlayersetpattern(pni, (INTSML)(~color));
//			}
//	
//			// redraw the demo layer in this cell
//			us_tecedredolayergraphics(ni.parent);
//			return;
//		}
//		if (namesamen(par[0], x_("Copy Pattern"), len) == 0 && len > 2)
//		{
//			us_clearhighlightcount();
//			if (us_teceditgetlayerinfo(ni.parent, &desc, &cif, &func, &layerletters, &dxf,
//				&gds, &spires, &spicap, &spiecap, &drcminwid, &height3d, &thick3d, printcol)) return;
//			didcopy = TRUE;
//			return;
//		}
//		if (namesamen(par[0], x_("Paste Pattern"), len) == 0 && len > 1)
//		{
//			us_clearhighlightcount();
//			us_teceditsetlayerpattern(ni.parent, &desc);
//	
//			// redraw the demo layer in this cell
//			us_tecedredolayergraphics(ni.parent);
//			return;
//		}
	}
	
	/**
	 * Method to return the color in layer-pattern node "ni" (off is 0, on is 0xFFFF).
	 */
	private static int us_tecedlayergetpattern(NodeInst ni)
	{
		if (ni.getProto() == Artwork.tech.boxNode) return 0;
		if (ni.getProto() != Artwork.tech.filledBoxNode) return 0;
		Variable var = ni.getVar(Artwork.ART_PATTERN);
		if (var == null) return 0xFFFF;
		return ((Short[])var.getObject())[0].intValue();
	}
	
	/**
	 * Method to set layer-pattern node "ni" to be color "color" (off is 0, on is 0xFFFF).
	 * Returns the address of the node (may be different than "ni" if it had to be replaced).
	 */
	private static void us_tecedlayersetpattern(NodeInst ni, int color)
	{
		SetLayerPatternJob job = new SetLayerPatternJob(ni, color);
	}

    /**
     * Class to create a technology-library from a technology.
     */
    public static class SetLayerPatternJob extends Job
	{
		private NodeInst ni;
		private int color;

		public SetLayerPatternJob(NodeInst ni, int color)
		{
			super("Change Pattern In Layer", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.color = color;
			startJob();
		}

		public boolean doIt()
		{
			if (ni.getProto() == Artwork.tech.boxNode)
			{
				if (color == 0) return true;
				NodeInst newni = ni.replace(Artwork.tech.filledBoxNode, false, false);
			} else if (ni.getProto() == Artwork.tech.filledBoxNode)
			{
				Short [] col = new Short[16];
				for(int i=0; i<16; i++) col[i] = new Short((short)color);
				ni.newVar(Artwork.ART_PATTERN, col);
			}
			return true;
		}
	}

	/**
	 * Method to toggle the color of layer-pattern node "ni" (called when the user does a
	 * "technology edit" click on the node).
	 */
	private static void us_tecedlayerpattern(EditWindow wnd, NodeInst ni)
	{
		int color = us_tecedlayergetpattern(ni);
		us_tecedlayersetpattern(ni, ~color);

		Highlighter h = wnd.getHighlighter();
		h.clear();
		h.addElectricObject(ni, ni.getParent());
	
		// redraw the demo layer in this cell
		us_tecedredolayergraphics(ni.getParent());
	}
	
//	/**
//	 * Method to modify the layer information in node "ni".
//	 */
//	void us_tecedlayertype(NODEINST *ni, INTBIG count, CHAR *par[])
//	{
//		REGISTER NODEPROTO *np;
//		CHAR *cif, *layerletters, *dxf, *gds;
//		REGISTER CHAR *name;
//		GRAPHICS desc;
//		float spires, spicap, spiecap;
//		INTBIG func, drcminwid, height3d, thick3d, printcol[5];
//		REGISTER void *infstr;
//	
//		if (par[0][0] == 0)
//		{
//			us_abortcommand(_("Requires a layer name"));
//			return;
//		}
//	
//		np = us_needcell();
//		if (np == NONODEPROTO) return;
//		if (namesame(par[0], x_("SET-MINIMUM-SIZE")) == 0)
//		{
//			if (namesamen(np.protoname, x_("node-"), 5) != 0)
//			{
//				us_abortcommand(_("Can only set minimum size in node descriptions"));
//				if ((us_tool.toolstate&NODETAILS) == 0) ttyputmsg(_("Use 'edit-node' option"));
//				return;
//			}
//			startobjectchange((INTBIG)ni, VNODEINST);
//			(void)setval((INTBIG)ni, VNODEINST, x_("EDTEC_minbox"), (INTBIG)x_("MIN"), VSTRING|VDISPLAY);
//			endobjectchange((INTBIG)ni, VNODEINST);
//			return;
//		}
//	
//		if (namesame(par[0], x_("CLEAR-MINIMUM-SIZE")) == 0)
//		{
//			if (getval((INTBIG)ni, VNODEINST, VSTRING, x_("EDTEC_minbox")) == NOVARIABLE)
//			{
//				ttyputmsg(_("Minimum size is not set on this layer"));
//				return;
//			}
//			startobjectchange((INTBIG)ni, VNODEINST);
//			(void)delval((INTBIG)ni, VNODEINST, x_("EDTEC_minbox"));
//			endobjectchange((INTBIG)ni, VNODEINST);
//			return;
//		}
//	
//		// find the actual cell with that layer specification
//		infstr = initinfstr();
//		addstringtoinfstr(infstr, x_("layer-"));
//		addstringtoinfstr(infstr, par[0]);
//		name = returninfstr(infstr);
//		np = getnodeproto(name);
//		if (np == NONODEPROTO)
//		{
//			ttyputerr(_("Cannot find layer primitive %s"), name);
//			return;
//		}
//	
//		// get the characteristics of that layer
//		cif = layerletters = gds = 0;
//		if (us_teceditgetlayerinfo(np, &desc, &cif, &func, &layerletters,
//			&dxf, &gds, &spires, &spicap, &spiecap, &drcminwid, &height3d,
//				&thick3d, printcol)) return;
//		if (gds != 0) efree(gds);
//		if (cif != 0) efree(cif);
//		if (layerletters != 0) efree(layerletters);
//	
//		startobjectchange((INTBIG)ni, VNODEINST);
//		us_teceditsetpatch(ni, &desc);
//		(void)setval((INTBIG)ni, VNODEINST, x_("EDTEC_layer"), (INTBIG)np, VNODEPROTO);
//		endobjectchange((INTBIG)ni, VNODEINST);
//	}
//	
//	/**
//	 * Method to modify port characteristics
//	 */
//	void us_tecedmodport(NODEINST *ni, INTBIG count, CHAR *par[])
//	{
//		REGISTER INTBIG total, i, len, j, yes;
//		BOOLEAN changed;
//		REGISTER BOOLEAN *yesno;
//		REGISTER NODEPROTO *np, **conlist;
//		REGISTER VARIABLE *var;
//	
//		// build an array of arc connections
//		for(total = 0, np = el_curlib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//			if (namesamen(np.protoname, x_("arc-"), 4) == 0) total++;
//		conlist = (NODEPROTO **)emalloc(total * (sizeof (NODEPROTO *)), el_tempcluster);
//		if (conlist == 0) return;
//		for(total = 0, np = el_curlib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//			if (namesamen(np.protoname, x_("arc-"), 4) == 0) conlist[total++] = np;
//		yesno = (BOOLEAN *)emalloc(total * (sizeof (BOOLEAN)), el_tempcluster);
//		if (yesno == 0) return;
//		for(i=0; i<total; i++) yesno[i] = FALSE;
//	
//		// put current list into the array
//		var = getval((INTBIG)ni, VNODEINST, VNODEPROTO|VISARRAY, x_("EDTEC_connects"));
//		if (var != NOVARIABLE)
//		{
//			len = getlength(var);
//			for(j=0; j<len; j++)
//			{
//				for(i=0; i<total; i++)
//					if (conlist[i] == ((NODEPROTO **)var.addr)[j]) break;
//				if (i < total) yesno[i] = TRUE;
//			}
//		}
//	
//		// parse the command parameters
//		changed = FALSE;
//		for(i=0; i<count-1; i += 2)
//		{
//			// search for an arc name
//			for(np = el_curlib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//				if (namesamen(np.protoname, x_("arc-"), 4) == 0 &&
//					namesame(&np.protoname[4], par[i]) == 0) break;
//			if (np != NONODEPROTO)
//			{
//				for(j=0; j<total; j++) if (conlist[j] == np)
//				{
//					if (*par[i+1] == 'y' || *par[i+1] == 'Y') yesno[j] = TRUE; else
//						yesno[j] = FALSE;
//					changed = TRUE;
//					break;
//				}
//				continue;
//			}
//	
//			if (namesame(par[i], x_("PORT-ANGLE")) == 0)
//			{
//				(void)setval((INTBIG)ni, VNODEINST, x_("EDTEC_portangle"), myatoi(par[i+1]), VINTEGER);
//				continue;
//			}
//			if (namesame(par[i], x_("PORT-ANGLE-RANGE")) == 0)
//			{
//				(void)setval((INTBIG)ni, VNODEINST, x_("EDTEC_portrange"), myatoi(par[i+1]), VINTEGER);
//				continue;
//			}
//		}
//	
//		// store list back if it was changed
//		if (changed)
//		{
//			yes = 0;
//			for(i=0; i<total; i++)
//			{
//				if (!yesno[i]) continue;
//				conlist[yes++] = conlist[i];
//			}
//			if (yes == 0 && var != NOVARIABLE)
//				(void)delval((INTBIG)ni, VNODEINST, x_("EDTEC_connects")); else
//			{
//				(void)setval((INTBIG)ni, VNODEINST, x_("EDTEC_connects"), (INTBIG)conlist,
//					VNODEPROTO|VISARRAY|(yes<<VLENGTHSH));
//			}
//		}
//		efree((CHAR *)conlist);
//		efree((CHAR *)yesno);
//	}
	
	private static void us_tecedarcfunction(EditWindow wnd, NodeInst ni)
	{
		String initialFuncName = getValueOnNode(ni);
		List funs = ArcProto.Function.getFunctions();
		String [] functionNames = new String[funs.size()];
		for(int i=0; i<funs.size(); i++)
		{
			ArcProto.Function fun = (ArcProto.Function)funs.get(i);
			functionNames[i] = fun.toString();
		}
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Arc Function", "New function for this arc:", initialFuncName, functionNames);
		if (choice == null) return;
		us_tecedsetnode(ni, "Function: " + choice);
	}

	private static String getValueOnNode(NodeInst ni)
	{
		String initial = "";
		Variable var = ni.getVar(Artwork.ART_MESSAGE, String.class);
		if (var != null)
		{
			initial = (String)var.getObject();
			int colonPos = initial.indexOf(':');
			if (colonPos > 0) initial = initial.substring(colonPos+2);
		}
		return initial;
	}

	private static void us_tecedarcfixang(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		boolean initialChoice = initialMsg.equalsIgnoreCase("yes");
		boolean finalChoice = PromptAt.showPromptAt(wnd, ni, "Choose angle", "Initial angle:", initialChoice);
		if (finalChoice != initialChoice)
		{
			us_tecedsetnode(ni, "Fixed-angle: " + (finalChoice ? "Yes" : "No"));
		}
	}
	
	private static void us_tecedarcwipes(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		boolean initialChoice = initialMsg.equalsIgnoreCase("yes");
		boolean finalChoice = PromptAt.showPromptAt(wnd, ni, "Set Whether this Arc Can Obscure a Pin Node",
			"Can this arc obscure a pin node (that is obscurable)?", initialChoice);
		if (finalChoice != initialChoice)
		{
			us_tecedsetnode(ni, "Wipes pins: " + (finalChoice ? "Yes" : "No"));
		}
	}
	
	private static void us_tecedarcnoextend(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		boolean initialChoice = initialMsg.equalsIgnoreCase("yes");
		boolean finalChoice = PromptAt.showPromptAt(wnd, ni, "Set Extension Default",
			"Are new instances of this arc drawn with ends extended?", initialChoice);
		if (finalChoice != initialChoice)
		{
			us_tecedsetnode(ni, "Extend arcs: " + (finalChoice ? "Yes" : "No"));
		}
	}
	
	private static void us_tecedarcinc(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		String newInc = PromptAt.showPromptAt(wnd, ni, "Change Angle Increment",
			"New angular granularity for placing this type of arc:", initialMsg);
		if (newInc != null) us_tecedsetnode(ni, "Angle increment: " + newInc);
	}
	
	private static void us_tecednodefunction(EditWindow wnd, NodeInst ni)
	{
		String initialFuncName = getValueOnNode(ni);
		List funs = PrimitiveNode.Function.getFunctions();
		String [] functionNames = new String[funs.size()];
		for(int i=0; i<funs.size(); i++)
		{
			PrimitiveNode.Function fun = (PrimitiveNode.Function)funs.get(i);
			functionNames[i] = fun.toString();
		}
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Node Function", "New function for this node:", initialFuncName, functionNames);
		if (choice == null) return;
		us_tecedsetnode(ni, "Function: " + choice);
	}
	
	private static void us_tecednodeserpentine(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		boolean initialChoice = initialMsg.equalsIgnoreCase("yes");
		boolean finalChoice = PromptAt.showPromptAt(wnd, ni, "Set Serpentine Transistor Capability",
			"Is this node a serpentine transistor?", initialChoice);
		if (finalChoice != initialChoice)
		{
			us_tecedsetnode(ni, "Serpentine transistor: " + (finalChoice ? "Yes" : "No"));
		}
	}
	
	private static void us_tecednodesquare(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		boolean initialChoice = initialMsg.equalsIgnoreCase("yes");
		boolean finalChoice = PromptAt.showPromptAt(wnd, ni, "Does Node Remain Square",
			"Must this node remain square?", initialChoice);
		if (finalChoice != initialChoice)
		{
			us_tecedsetnode(ni, "Square node: " + (finalChoice ? "Yes" : "No"));
		}
	}
	
	private static void us_tecednodewipes(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		boolean initialChoice = initialMsg.equalsIgnoreCase("yes");
		boolean finalChoice = PromptAt.showPromptAt(wnd, ni, "Set How Arcs Obscure This Node",
			"Is this node invisible when 1 or 2 arcs connect to it?", initialChoice);
		if (finalChoice != initialChoice)
		{
			us_tecedsetnode(ni, "Invisible with 1 or 2 arcs: " + (finalChoice ? "Yes" : "No"));
		}
	}
	
	private static void us_tecednodelockable(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		boolean initialChoice = initialMsg.equalsIgnoreCase("yes");
		boolean finalChoice = PromptAt.showPromptAt(wnd, ni, "Set Node Lockability",
			"Is this node able to be locked down (used for FPGA primitives):", initialChoice);
		if (finalChoice != initialChoice)
		{
			us_tecedsetnode(ni, "Lockable: " + (finalChoice ? "Yes" : "No"));
		}
	}
	
	private static void us_tecednodemulticut(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		String newSep = PromptAt.showPromptAt(wnd, ni, "Set Multicut Separation",
			"The distance between multiple cuts in this contact:", initialMsg);
		if (newSep != null) us_tecedsetnode(ni, "Multicut separation: " + newSep);
	}
	
	private static void us_tecedinfolambda(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		String newUnit = PromptAt.showPromptAt(wnd, ni, "Set Unit Size",
			"The scale of this technology (nanometers per grid unit):", initialMsg);
		if (newUnit != null) us_tecedsetnode(ni, "Lambda: " + newUnit);
	}
	
	private static void us_tecedinfodescript(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = getValueOnNode(ni);
		String newDesc = PromptAt.showPromptAt(wnd, ni, "Set Technology Description",
			"Full description of this technology:", initialMsg);
		if (newDesc != null) us_tecedsetnode(ni, "Description: " + newDesc);
	}
	
	/****************************** UTILITIES ******************************/
	
	private static void us_tecedsetnode(NodeInst ni, String chr)
	{
		SetTextJob job = new SetTextJob(ni, chr);
	}

    /**
     * Class to create a technology-library from a technology.
     */
    public static class SetTextJob extends Job
	{
		private NodeInst ni;
		private String chr;

		public SetTextJob(NodeInst ni, String chr)
		{
			super("Make Technology Library from Technology", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.chr = chr;
			startJob();
		}

		public boolean doIt()
		{
			Variable var = ni.newVar(Artwork.ART_MESSAGE, chr);
			if (var != null)
				var.setDisplay(true);
			return true;
		}
	}

//	/**
//	 * Method to call up the cell "cellname" (either create it or reedit it)
//	 * returns NONODEPROTO if there is an error or the cell exists
//	 */
//	NODEPROTO *us_tecedentercell(CHAR *cellname)
//	{
//		REGISTER NODEPROTO *np;
//		CHAR *newpar[2];
//	
//		np = getnodeproto(cellname);
//		if (np != NONODEPROTO && np.primindex == 0)
//		{
//			newpar[0] = x_("editcell");
//			newpar[1] = cellname;
//			telltool(us_tool, 2, newpar);
//			return(NONODEPROTO);
//		}
//	
//		// create the cell
//		np = newnodeproto(cellname, el_curlib);
//		if (np == NONODEPROTO) return(NONODEPROTO);
//	
//		// now edit the cell
//		newpar[0] = x_("editcell");
//		newpar[1] = cellname;
//		telltool(us_tool, 2, newpar);
//		return(np);
//	}

	/**
	 * Method to redraw the demo layer in "layer" cell "np"
	 */
	private static void us_tecedredolayergraphics(Cell np)
	{
		RedoLayerGraphicsJob job = new RedoLayerGraphicsJob(np);
	}

    /**
     * Class to create a technology-library from a technology.
     */
    public static class RedoLayerGraphicsJob extends Job
	{
		private Cell cell;

		public RedoLayerGraphicsJob(Cell cell)
		{
			super("Redo Layer Graphics", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt()
		{
			Variable var = cell.getVar("EDTEC_colornode");
			if (var == null) return false;
			NodeInst ni = (NodeInst)var.getObject();
		
			// get the current description of this layer
			Generate.LayerInfo li = Generate.LayerInfo.us_teceditgetlayerinfo(cell);
			if (li == null) return false;
		
			// modify the demo patch to reflect the color and pattern
			us_teceditsetpatch(ni, li.desc);
		
			// now do this to all layers in all cells!
			for(Iterator cIt = cell.getLibrary().getCells(); cIt.hasNext(); )
			{
				Cell onp = (Cell)cIt.next();
				if (!onp.getName().startsWith("arc-") && !onp.getName().startsWith("node-")) continue;
				for(Iterator nIt = onp.getNodes(); nIt.hasNext(); )
				{
					NodeInst cNi = (NodeInst)nIt.next();
					if (us_tecedgetoption(cNi) != Generate.LAYERPATCH) continue;
					Variable varLay = cNi.getVar("EDTEC_layer");
					if (varLay == null) continue;
					if ((Cell)varLay.getObject() != cell) continue;
					us_teceditsetpatch(cNi, li.desc);
				}
			}
			return true;
		}
	}

	static void us_teceditsetpatch(NodeInst ni, EGraphics desc)
	{
		if (desc.getTransparentLayer() > 0)
		{
			ni.newVar(Artwork.ART_COLOR, new Integer(EGraphics.makeIndex(desc.getTransparentLayer())));
		} else
		{
			ni.newVar(Artwork.ART_COLOR, new Integer(EGraphics.makeIndex(desc.getColor())));
		}
		if (desc.isPatternedOnDisplay())
		{
			int [] raster = desc.getPattern();
			if (desc.isOutlinedOnDisplay())
			{
				Short [] pattern = new Short[16];
				for(int i=0; i<16; i++) pattern[i] = new Short((short)raster[i]);
				ni.newVar(Artwork.ART_PATTERN, pattern);
			} else
			{
				Integer [] pattern = new Integer[16];
				for(int i=0; i<16; i++) pattern[i] = new Integer(raster[i]);
				ni.newVar(Artwork.ART_PATTERN, pattern);
			}
		} else
		{
			if (ni.getVar(Artwork.ART_PATTERN) != null)
				ni.delVar(Artwork.ART_PATTERN);
		}
	}
	
//	/**
//	 * Method to load the color map associated with library "lib"
//	 */
//	void us_tecedloadlibmap(LIBRARY *lib)
//	{
//		REGISTER VARIABLE *var;
//		REGISTER INTBIG i;
//		REGISTER INTBIG *mapptr;
//		INTBIG redmap[256], greenmap[256], bluemap[256];
//	
//		var = getval((INTBIG)lib, VLIBRARY, VINTEGER|VISARRAY, x_("EDTEC_colormap"));
//		if (var != NOVARIABLE)
//		{
//			mapptr = (INTBIG *)var.addr;
//			for(i=0; i<256; i++)
//			{
//				redmap[i] = *mapptr++;
//				greenmap[i] = *mapptr++;
//				bluemap[i] = *mapptr++;
//			}
//	
//			// disable option tracking
//			(void)setvalkey((INTBIG)us_tool, VTOOL, us_ignoreoptionchangeskey, 1,
//				VINTEGER|VDONTSAVE);
//	
//			startobjectchange((INTBIG)us_tool, VTOOL);
//			(void)setvalkey((INTBIG)us_tool, VTOOL, us_colormap_red_key, (INTBIG)redmap,
//				VINTEGER|VISARRAY|(256<<VLENGTHSH));
//			(void)setvalkey((INTBIG)us_tool, VTOOL, us_colormap_green_key, (INTBIG)greenmap,
//				VINTEGER|VISARRAY|(256<<VLENGTHSH));
//			(void)setvalkey((INTBIG)us_tool, VTOOL, us_colormap_blue_key, (INTBIG)bluemap,
//				VINTEGER|VISARRAY|(256<<VLENGTHSH));
//			endobjectchange((INTBIG)us_tool, VTOOL);
//	
//			// re-enable option tracking
//			var = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER, us_ignoreoptionchangeskey);
//			if (var != NOVARIABLE)
//				(void)delvalkey((INTBIG)us_tool, VTOOL, us_ignoreoptionchangeskey);
//		}
//	}
//	
//	void us_teceditgetprintcol(VARIABLE *var, INTBIG *r, INTBIG *g, INTBIG *b, INTBIG *o, INTBIG *f)
//	{
//		REGISTER CHAR *pt;
//	
//		// set default values
//		*r = *g = *b = *o = *f = 0;
//		if (var == NOVARIABLE) return;
//	
//		// skip the header
//		pt = (CHAR *)var.addr;
//		while (*pt != 0 && *pt != ':') pt++;
//		if (*pt == ':') pt++;
//	
//		// get red
//		while (*pt == ' ') pt++;
//		*r = myatoi(pt);
//		while (*pt != 0 && *pt != ',') pt++;
//		if (*pt == ',') pt++;
//	
//		// get green
//		while (*pt == ' ') pt++;
//		*g = myatoi(pt);
//		while (*pt != 0 && *pt != ',') pt++;
//		if (*pt == ',') pt++;
//	
//		// get blue
//		while (*pt == ' ') pt++;
//		*b = myatoi(pt);
//		while (*pt != 0 && *pt != ',') pt++;
//		if (*pt == ',') pt++;
//	
//		// get opacity
//		while (*pt == ' ') pt++;
//		*o = myatoi(pt);
//		while (*pt != 0 && *pt != ',') pt++;
//		if (*pt == ',') pt++;
//	
//		// get foreground
//		while (*pt == ' ') pt++;
//		if (namesamen(pt, x_("on"), 2) == 0) *f = 1; else
//			if (namesamen(pt, x_("off"), 3) == 0) *f = 0; else
//				*f = myatoi(pt);
//	}
//	
//	/**
//	 * Method to set the layer-pattern squares of cell "np" to the bits in "desc".
//	 */
//	void us_teceditsetlayerpattern(NODEPROTO *np, GRAPHICS *desc)
//	{
//		REGISTER NODEINST *ni;
//		REGISTER INTBIG patterncount;
//		REGISTER INTBIG lowx, highx, lowy, highy, x, y;
//		REGISTER INTSML wantcolor, color;
//		REGISTER VARIABLE *var;
//	
//		// look at all nodes in the layer description cell
//		patterncount = 0;
//		for(ni = np.firstnodeinst; ni != NONODEINST; ni = ni.nextnodeinst)
//		{
//			if (ni.proto == art_boxprim || ni.proto == art_filledboxprim)
//			{
//				var = getvalkey((INTBIG)ni, VNODEINST, VINTEGER, us_edtec_option_key);
//				if (var == NOVARIABLE) continue;
//				if (var.addr != LAYERPATTERN) continue;
//				if (patterncount == 0)
//				{
//					lowx = ni.lowx;   highx = ni.highx;
//					lowy = ni.lowy;   highy = ni.highy;
//				} else
//				{
//					if (ni.lowx < lowx) lowx = ni.lowx;
//					if (ni.highx > highx) highx = ni.highx;
//					if (ni.lowy < lowy) lowy = ni.lowy;
//					if (ni.highy > highy) highy = ni.highy;
//				}
//				patterncount++;
//			}
//		}
//	
//		if (patterncount != 16*16 && patterncount != 16*8)
//		{
//			ttyputerr(_("Incorrect number of pattern boxes in %s (has %ld, not %d)"),
//				describenodeproto(np), patterncount, 16*16);
//			return;
//		}
//	
//		// set the pattern
//		for(ni = np.firstnodeinst; ni != NONODEINST; ni = ni.nextnodeinst)
//		{
//			if (ni.proto != art_boxprim && ni.proto != art_filledboxprim) continue;
//			var = getvalkey((INTBIG)ni, VNODEINST, VINTEGER, us_edtec_option_key);
//			if (var == NOVARIABLE) continue;
//			if (var.addr != LAYERPATTERN) continue;
//	
//			x = (ni.lowx - lowx) / ((highx-lowx) / 16);
//			y = (highy - ni.highy) / ((highy-lowy) / 16);
//			if ((desc.raster[y] & (1 << (15-x))) == 0) wantcolor = 0;
//				else wantcolor = (INTSML)0xFFFF;
//	
//			color = us_tecedlayergetpattern(ni);
//			if (color != wantcolor)
//				us_tecedlayersetpattern(ni, wantcolor);
//		}
//	}
	
	/**
	 * Method to return the option index of node "ni"
	 */
	static int us_tecedgetoption(NodeInst ni)
	{
		// port objects are readily identifiable
		if (ni.getProto() == Generic.tech.portNode) return Generate.PORTOBJ;
	
		// center objects are also readily identifiable
		if (ni.getProto() == Generic.tech.cellCenterNode) return Generate.CENTEROBJ;
	
		Variable var = ni.getVar(Generate.EDTEC_OPTION);
		if (var == null) return -1;
		int option = ((Integer)var.getObject()).intValue();
		if (option == Generate.LAYERPATCH)
		{
			// may be a highlight object
			Variable var2 = ni.getVar("EDTEC_layer");
			if (var2 != null)
			{
				if (var2.getObject() == null) return Generate.HIGHLIGHTOBJ;
			}
		}
		return option;
	}
	
//	/**
//	 * Method called when cell "np" has been deleted (and it may be a layer cell because its name
//	 * started with "layer-").
//	 */
//	void us_teceddeletelayercell(NODEPROTO *np)
//	{
//		REGISTER VARIABLE *var;
//		REGISTER NODEPROTO *onp;
//		REGISTER NODEINST *ni;
//		REGISTER BOOLEAN warned, isnode;
//		REGISTER CHAR *layername;
//		static INTBIG edtec_layer_key = 0;
//		REGISTER void *infstr;
//	
//		// may have deleted layer cell in technology library
//		if (edtec_layer_key == 0) edtec_layer_key = makekey(x_("EDTEC_layer"));
//		layername = &np.protoname[6];
//		warned = FALSE;
//		for(onp = np.lib.firstnodeproto; onp != NONODEPROTO; onp = onp.nextnodeproto)
//		{
//			if (namesamen(onp.protoname, x_("node-"), 5) == 0) isnode = TRUE; else
//				if (namesamen(onp.protoname, x_("arc-"), 4) == 0) isnode = FALSE; else
//					continue;
//			for(ni = onp.firstnodeinst; ni != NONODEINST; ni = ni.nextnodeinst)
//			{
//				var = getvalkey((INTBIG)ni, VNODEINST, VNODEPROTO, edtec_layer_key);
//				if (var == NOVARIABLE) continue;
//				if ((NODEPROTO *)var.addr == np) break;
//			}
//			if (ni != NONODEINST)
//			{
//				if (warned) addtoinfstr(infstr, ','); else
//				{
//					infstr = initinfstr();
//					formatinfstr(infstr, _("Warning: layer %s is used in"), layername);
//					warned = TRUE;
//				}
//				if (isnode) formatinfstr(infstr, _(" node %s"), &onp.protoname[5]); else
//					formatinfstr(infstr, _(" arc %s"), &onp.protoname[4]);
//			}
//		}
//		if (warned)
//			ttyputmsg(x_("%s"), returninfstr(infstr));
//	
//		// see if this layer is mentioned in the design rules
//		us_tecedrenamecell(np.protoname, x_(""));
//	}
//	
//	/**
//	 * Method called when cell "np" has been deleted (and it may be a node cell because its name
//	 * started with "node-").
//	 */
//	void us_teceddeletenodecell(NODEPROTO *np)
//	{
//		// see if this node is mentioned in the design rules
//		us_tecedrenamecell(np.protoname, x_(""));
//	}
//	
//	/******************** SUPPORT FOR "usredtecp.c" ROUTINES ********************/
//	
//	/**
//	 * Method to return the actual bounding box of layer node "ni" in the
//	 * reference variables "lx", "hx", "ly", and "hy"
//	 */
//	void us_tecedgetbbox(NODEINST *ni, INTBIG *lx, INTBIG *hx, INTBIG *ly, INTBIG *hy)
//	{
//		REGISTER INTBIG twolambda;
//	
//		*lx = ni.geom.lowx;
//		*hx = ni.geom.highx;
//		*ly = ni.geom.lowy;
//		*hy = ni.geom.highy;
//		if (ni.proto != gen_portprim) return;
//		twolambda = lambdaofnode(ni) * 2;
//		*lx += twolambda;   *hx -= twolambda;
//		*ly += twolambda;   *hy -= twolambda;
//	}
//	
//	void us_tecedpointout(NODEINST *ni, NODEPROTO *np)
//	{
//		REGISTER WINDOWPART *w;
//		CHAR *newpar[2];
//	
//		for(w = el_topwindowpart; w != NOWINDOWPART; w = w.nextwindowpart)
//			if (w.curnodeproto == np) break;
//		if (w == NOWINDOWPART)
//		{
//			newpar[0] = describenodeproto(np);
//			us_editcell(1, newpar);
//		}
//		if (ni != NONODEINST)
//		{
//			us_clearhighlightcount();
//			(void)asktool(us_tool, x_("show-object"), (INTBIG)ni.geom);
//		}
//	}
//	
//	/**
//	 * Method to swap entries "p1" and "p2" of the port list in "tlist"
//	 */
//	void us_tecedswapports(INTBIG *p1, INTBIG *p2, TECH_NODES *tlist)
//	{
//		REGISTER INTBIG temp, *templ;
//		REGISTER CHAR *tempc;
//	
//		templ = tlist.portlist[*p1].portarcs;
//		tlist.portlist[*p1].portarcs = tlist.portlist[*p2].portarcs;
//		tlist.portlist[*p2].portarcs = templ;
//	
//		tempc = tlist.portlist[*p1].protoname;
//		tlist.portlist[*p1].protoname = tlist.portlist[*p2].protoname;
//		tlist.portlist[*p2].protoname = tempc;
//	
//		temp = tlist.portlist[*p1].initialbits;
//		tlist.portlist[*p1].initialbits = tlist.portlist[*p2].initialbits;
//		tlist.portlist[*p2].initialbits = temp;
//	
//		temp = tlist.portlist[*p1].lowxmul;
//		tlist.portlist[*p1].lowxmul = tlist.portlist[*p2].lowxmul;
//		tlist.portlist[*p2].lowxmul = (INTSML)temp;
//		temp = tlist.portlist[*p1].lowxsum;
//		tlist.portlist[*p1].lowxsum = tlist.portlist[*p2].lowxsum;
//		tlist.portlist[*p2].lowxsum = (INTSML)temp;
//	
//		temp = tlist.portlist[*p1].lowymul;
//		tlist.portlist[*p1].lowymul = tlist.portlist[*p2].lowymul;
//		tlist.portlist[*p2].lowymul = (INTSML)temp;
//		temp = tlist.portlist[*p1].lowysum;
//		tlist.portlist[*p1].lowysum = tlist.portlist[*p2].lowysum;
//		tlist.portlist[*p2].lowysum = (INTSML)temp;
//	
//		temp = tlist.portlist[*p1].highxmul;
//		tlist.portlist[*p1].highxmul = tlist.portlist[*p2].highxmul;
//		tlist.portlist[*p2].highxmul = (INTSML)temp;
//		temp = tlist.portlist[*p1].highxsum;
//		tlist.portlist[*p1].highxsum = tlist.portlist[*p2].highxsum;
//		tlist.portlist[*p2].highxsum = (INTSML)temp;
//	
//		temp = tlist.portlist[*p1].highymul;
//		tlist.portlist[*p1].highymul = tlist.portlist[*p2].highymul;
//		tlist.portlist[*p2].highymul = (INTSML)temp;
//		temp = tlist.portlist[*p1].highysum;
//		tlist.portlist[*p1].highysum = tlist.portlist[*p2].highysum;
//		tlist.portlist[*p2].highysum = (INTSML)temp;
//	
//		// finally, swap the actual identifiers
//		temp = *p1;   *p1 = *p2;   *p2 = temp;
//	}
//	
//	CHAR *us_tecedsamplename(NODEPROTO *layernp)
//	{
//		if (layernp == gen_portprim) return(x_("PORT"));
//		if (layernp == gen_cellcenterprim) return(x_("GRAB"));
//		if (layernp == NONODEPROTO) return(x_("HIGHLIGHT"));
//		return(&layernp.protoname[6]);
//	}
//	
//	/* Technology Edit Reorder */
//	static DIALOGITEM us_tecedredialogitems[] =
//	{
//	 /*  1 */ {0, {376,208,400,288}, BUTTON, N_("OK")},
//	 /*  2 */ {0, {344,208,368,288}, BUTTON, N_("Cancel")},
//	 /*  3 */ {0, {28,4,404,200}, SCROLL, x_("")},
//	 /*  4 */ {0, {4,4,20,284}, MESSAGE, x_("")},
//	 /*  5 */ {0, {168,208,192,268}, BUTTON, N_("Up")},
//	 /*  6 */ {0, {212,208,236,268}, BUTTON, N_("Down")},
//	 /*  7 */ {0, {136,208,160,280}, BUTTON, N_("Far Up")},
//	 /*  8 */ {0, {244,208,268,280}, BUTTON, N_("Far Down")}
//	};
//	static DIALOG us_tecedredialog = {{75,75,488,373}, N_("Reorder Technology Primitives"), 0, 8, us_tecedredialogitems, 0, 0};
//	
//	/* special items for the "Reorder Primitives" dialog: */
//	#define DTER_LIST           3		/* List of primitives (scroll) */
//	#define DTER_TITLE          4		/* Primitive title (message) */
//	#define DTER_UP             5		/* Move Up (button) */
//	#define DTER_DOWN           6		/* Move Down (button) */
//	#define DTER_FARUP          7		/* Move Far Up (button) */
//	#define DTER_FARDOWN        8		/* Move Far Down (button) */
//	
//	void us_reorderprimdlog(CHAR *type, CHAR *prefix, CHAR *varname)
//	{
//		REGISTER INTBIG itemHit, i, j, total, len, amt;
//		CHAR line[100], **seqname;
//		REGISTER BOOLEAN changed;
//		LIBRARY *thelib[1];
//		NODEPROTO **sequence, *np;
//		REGISTER void *dia;
//	
//		dia = DiaInitDialog(&us_tecedredialog);
//		if (dia == 0) return;
//		DiaInitTextDialog(dia, DTER_LIST, DiaNullDlogList, DiaNullDlogItem,
//			DiaNullDlogDone, -1, SCSELMOUSE);
//		esnprintf(line, 100, _("%s in technology %s"), type, el_curlib.libname);
//		DiaSetText(dia, DTER_TITLE, line);
//		thelib[0] = el_curlib;
//		total = us_teceditfindsequence(thelib, 1, prefix, varname, &sequence);
//		len = strlen(prefix);
//		for(i=0; i<total; i++)
//			DiaStuffLine(dia, DTER_LIST, &sequence[i].protoname[len]);
//		DiaSelectLine(dia, DTER_LIST, 0);
//	
//		changed = FALSE;
//		for(;;)
//		{
//			itemHit = DiaNextHit(dia);
//			if (itemHit == OK || itemHit == CANCEL) break;
//			if (itemHit == DTER_UP || itemHit == DTER_FARUP)
//			{
//				// shift up
//				if (itemHit == DTER_UP) amt = 1; else amt = 10;
//				for(j=0; j<amt; j++)
//				{
//					i = DiaGetCurLine(dia, DTER_LIST);
//					if (i <= 0) break;
//					np = sequence[i];
//					sequence[i] = sequence[i-1];
//					sequence[i-1] = np;
//					DiaSetScrollLine(dia, DTER_LIST, i, &sequence[i].protoname[len]);
//					DiaSetScrollLine(dia, DTER_LIST, i-1, &sequence[i-1].protoname[len]);
//				}
//				changed = TRUE;
//				continue;
//			}
//			if (itemHit == DTER_DOWN || itemHit == DTER_FARDOWN)
//			{
//				// shift down
//				if (itemHit == DTER_DOWN) amt = 1; else amt = 10;
//				for(j=0; j<amt; j++)
//				{
//					i = DiaGetCurLine(dia, DTER_LIST);
//					if (i >= total-1) continue;
//					np = sequence[i];
//					sequence[i] = sequence[i+1];
//					sequence[i+1] = np;
//					DiaSetScrollLine(dia, DTER_LIST, i, &sequence[i].protoname[len]);
//					DiaSetScrollLine(dia, DTER_LIST, i+1, &sequence[i+1].protoname[len]);
//				}
//				changed = TRUE;
//				continue;
//			}
//		}
//	
//		// preserve order
//		if (itemHit == OK && changed)
//		{
//			seqname = (CHAR **)emalloc(total * (sizeof (CHAR *)), el_tempcluster);
//			for(i=0; i<total; i++)
//				seqname[i] = &sequence[i].protoname[len];
//			setval((INTBIG)el_curlib, VLIBRARY, varname, (INTBIG)seqname,
//				VSTRING|VISARRAY|(total<<VLENGTHSH));
//			efree((CHAR *)seqname);
//		}
//		efree((CHAR *)sequence);
//		DiaDoneDialog(dia);
//	}
}
