/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Manipulate.java
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
import com.sun.electric.database.geometry.Geometric;
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
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.tecEdit.Generate.ArcInfo;
import com.sun.electric.tool.user.tecEdit.Generate.NodeInfo;
import com.sun.electric.tool.user.tecEdit.Generate.LayerInfo;
import com.sun.electric.tool.user.tecEdit.Generate.LibFromTechJob;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.PromptAt;
import com.sun.electric.tool.user.dialogs.PromptAt.Field;
import com.sun.electric.tool.user.Highlighter;

import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.HashSet;

import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Point;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * This class manipulates technology libraries from technologies.
 */
public class Manipulate
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
//		if (namesamen(pp, x_("dependent-libraries"), l) == 0 && l >= 2)
//		{
//			if (count < 2)
//			{
//				// display dependent library names
//				var = el_curlib.getVar(Generate.DEPENDENTLIB_KEY);
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
//				var = el_curlib.getVar(Generate.DEPENDENTLIB_KEY);
//				if (var != NOVARIABLE)
//					delval((INTBIG)el_curlib, VLIBRARY, Generate.DEPENDENTLIB_KEY);
//				return;
//			}
//	
//			// create a list
//			dependentlist = (CHAR **)emalloc((count-1) * (sizeof (CHAR *)), el_tempcluster);
//			if (dependentlist == 0) return;
//			for(i=1; i<count; i++) dependentlist[i-1] = par[i];
//			el_curlib.newVar(Generate.DEPENDENTLIB_KEY, dependentlist);
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
//		nodecount = us_teceditfindsequence(liblist, "node-", Generate.NODESEQUENCE_KEY);
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
	/**
	 * Method to update tables to reflect that cell "oldname" is now called "newname".
	 * If "newname" is not valid, any rule that refers to "oldname" is removed.
	 */
	public static void renamedCell(String oldname, String newname)
	{
//		REGISTER VARIABLE *var;
//		REGISTER INTBIG i, len;
//		REGISTER BOOLEAN valid;
//		INTBIG count;
//		REGISTER CHAR *origstr, *firstkeyword, *keyword;
//		CHAR *str, **strings;
//		REGISTER void *infstr, *sa;
	
		// if this is a layer, rename the layer sequence array
		if (oldname.startsWith("layer-") && newname.startsWith("layer-"))
		{
			us_tecedrenamesequence(Generate.LAYERSEQUENCE_KEY, oldname.substring(6), newname.substring(6));
		}
	
		// if this is an arc, rename the arc sequence array
		if (oldname.startsWith("arc-") && newname.startsWith("arc-"))
		{
			us_tecedrenamesequence(Generate.ARCSEQUENCE_KEY, oldname.substring(4), newname.substring(4));
		}
	
		// if this is a node, rename the node sequence array
		if (oldname.startsWith("node-") && newname.startsWith("node-"))
		{
			us_tecedrenamesequence(Generate.NODESEQUENCE_KEY, oldname.substring(5), newname.substring(5));
		}
	
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
	}

	/**
	 * Method called when a cell has been deleted.
	 */
	public static void deletedCell(Cell np)
	{
//		REGISTER VARIABLE *var;
//		REGISTER NODEPROTO *onp;
//		REGISTER NODEINST *ni;
//		REGISTER BOOLEAN warned, isnode;
//		REGISTER CHAR *layername;
//		REGISTER void *infstr;
//	
		if (np.getName().startsWith("layer-"))
		{
//			// may have deleted layer cell in technology library
//			layername = &np.protoname[6];
//			warned = FALSE;
//			for(onp = np.lib.firstnodeproto; onp != NONODEPROTO; onp = onp.nextnodeproto)
//			{
//				if (namesamen(onp.protoname, x_("node-"), 5) == 0) isnode = TRUE; else
//					if (namesamen(onp.protoname, x_("arc-"), 4) == 0) isnode = FALSE; else
//						continue;
//				for(ni = onp.firstnodeinst; ni != NONODEINST; ni = ni.nextnodeinst)
//				{
//					var = ni.getVar(Generate.LAYER_KEY);
//					if (var == null) continue;
//					if ((NODEPROTO *)var.addr == np) break;
//				}
//				if (ni != NONODEINST)
//				{
//					if (warned) addtoinfstr(infstr, ','); else
//					{
//						infstr = initinfstr();
//						formatinfstr(infstr, _("Warning: layer %s is used in"), layername);
//						warned = TRUE;
//					}
//					if (isnode) formatinfstr(infstr, _(" node %s"), &onp.protoname[5]); else
//						formatinfstr(infstr, _(" arc %s"), &onp.protoname[4]);
//				}
//			}
//			if (warned)
//				ttyputmsg(x_("%s"), returninfstr(infstr));
//		
//			// see if this layer is mentioned in the design rules
//			renamedCell(np.protoname, "");
		} else if (np.getName().startsWith("node-"))
		{
//			// see if this node is mentioned in the design rules
//			renamedCell(np.protoname, "");
		}
	}

	/**
	 * Method to rename the layer/arc/node sequence arrays to account for a name change.
	 * The sequence array is in variable "varname", and the item has changed from "oldname" to
	 * "newname".
	 */
	static void us_tecedrenamesequence(Variable.Key varname, String oldname, String newname)
	{
		Library lib = Library.getCurrent();
		Variable var = lib.getVar(varname);
		if (var == null) return;
	
		String [] strings = (String [])var.getObject();
		for(int i=0; i<strings.length; i++)
			if (strings[i].equals(oldname)) strings[i] = newname;
		lib.newVar(varname, strings);
	}
	
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
//		total = us_teceditfindsequence(dependentlibs, "layer-", Generate.LAYERSEQUENCE_KEY);
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
//					var = ni.getVar(Generate.OPTION_KEY);
//					if (var == null) continue;
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
//			el_curlib.newVar(Generate.COLORMAP_KEY, newmap);
//			efree((CHAR *)newmap);
//		}
//		efree((CHAR *)layernames);
//		efree((CHAR *)printcolors);
//	}

	/**
	 * Method to determine whether it is legal to place an instance in a technology-edit cell.
	 * @param np the type of node to create.
	 * @param cell the cell in which to place it.
	 * @return true if the creation is invalid (and prints an error message).
	 */
	public static boolean invalidCreation(NodeProto np, Cell cell)
	{
		// make sure the cell is right
		if (!cell.getName().startsWith("node-") && !cell.getName().startsWith("arc-"))
		{
			System.out.println("Must be editing a node or arc to place geometry");
			return true;
		}
		if (np == Generic.tech.portNode && !cell.getName().startsWith("node-"))
		{
			System.out.println("Can only place ports in node descriptions");
			return true;
		}
		return false;
	}

	/**
	 * Make a new technology-edit cell of a given type.
	 * @param type 1=layer, 2=arc, 3=node, 4=factors
	 */
	public static void makeCell(int type)
	{
		Library lib = Library.getCurrent();
		String cellName = null;
		switch (type)
		{
			case 1:		// layer
				String layerName = JOptionPane.showInputDialog("Name of new layer:", "");
				if (layerName == null) return;
				cellName = "layer-" + layerName;
				break;
			case 2:		// arc
				String arcName = JOptionPane.showInputDialog("Name of new arc:", "");
				if (arcName == null) return;
				cellName = "arc-" + arcName;
				break;
			case 3:		// node
				String nodeName = JOptionPane.showInputDialog("Name of new node:", "");
				if (nodeName == null) return;
				cellName = "node-" + nodeName;
				break;
			case 4:		// factors
				cellName = "factors";
				break;
		}

		// see if the cell exists
		Cell cell = lib.findNodeProto(cellName);
		if (cell != null)
		{
			// cell exists: put it in the current window
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (wf != null) wf.setCellWindow(cell);
			return;
		}

		// create the cell
		MakeOneCellJob job = new MakeOneCellJob(lib, cellName, type);
	}

	/**
	 * Class to create a single cell in a technology-library.
	 */
	public static class MakeOneCellJob extends Job
	{
		private Library lib;
		private String name;
		private int type;
	
		public MakeOneCellJob(Library lib, String name, int type)
		{
			super("Make Cell in Technology-Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lib = lib;
			this.name = name;
			this.type = type;
			startJob();
		}

		public boolean doIt()
		{
			Cell cell = Cell.makeInstance(lib, name);
			if (cell == null) return false;
			cell.setInTechnologyLibrary();
			cell.setTechnology(Artwork.tech);

			// specialty initialization
			switch (type)
			{
				case 1:
					LayerInfo li = LayerInfo.makeInstance();
					li.us_tecedmakelayer(cell);
					break;
				case 2:
					ArcInfo aIn = ArcInfo.makeInstance();
					aIn.us_tecedmakearc(cell);
					break;
				case 3:
					NodeInfo nIn = NodeInfo.makeInstance();
					nIn.us_tecedmakenode(cell);
					break;
			}

			// show it
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (wf != null) wf.setCellWindow(cell);
			return true;
		}
	}

	/**
	 * Method to complete the creation of a new node in a technology edit cell.
	 * @param newNi the node that was just created.
	 */
	public static void completeNodeCreation(NodeInst newNi, Object toDraw)
	{
		newNi.newVar(Generate.OPTION_KEY, new Integer(Generate.LAYERPATCH));
		
		// postprocessing on the nodes
		if (newNi.getProto() == Generic.tech.portNode)
		{
			// a port layer
			String portName = JOptionPane.showInputDialog("Port name:", "");
			if (portName == null) return;
			Variable var = newNi.newVar(Generate.PORTNAME_KEY, portName);
			if (var != null) var.setDisplay(true);
			return;
		}
		if (newNi.getProto() == Artwork.tech.boxNode)
		{
			// could be a highlight layer
			if (toDraw instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)toDraw;
				if (ni.getVar(Generate.LAYER_KEY) != null)
				{
					newNi.newVar(Generate.LAYER_KEY, null);
					return;
				}
			}
		}

		// a real layer: default to the first one
		String [] layerNames = us_tecedgetlayernamelist();
		if (layerNames != null && layerNames.length > 0)
		{
			Cell cell = Library.getCurrent().findNodeProto(layerNames[0]);
			if (cell != null)
			{
				newNi.newVar(Generate.LAYER_KEY, cell);
				Generate.LayerInfo li = Generate.LayerInfo.us_teceditgetlayerinfo(cell);
				if (li != null)
					us_teceditsetpatch(newNi, li.desc);
			}
		}
	}

	/**
	 * Method to highlight information about all layers (or ports if "doports" is true)
	 */
	public static void us_teceditidentify(boolean doports)
	{
		EditWindow wnd = EditWindow.getCurrent();
		Cell np = WindowFrame.needCurCell();
		if (wnd == null || np == null) return;
	
		if (doports)
		{
			if (!np.getName().startsWith("node-"))
			{
				System.out.println("Must be editing a node to identify ports");
				return;
			}
		} else
		{
			if (!np.getName().startsWith("node-") && !np.getName().startsWith("arc-"))
			{
				System.out.println("Must be editing a node or arc to identify layers");
				return;
			}
		}
	
		// get examples
		Parse.Example nelist = null;
		if (np.getName().startsWith("node-"))
			nelist = Parse.us_tecedgetexamples(np, true); else
				nelist = Parse.us_tecedgetexamples(np, false);
		if (nelist == null) return;
	
		// count the number of appropriate samples in the main example
		int total = 0;
		for(Parse.Sample ns = nelist.firstsample; ns != null; ns = ns.nextsample)
		{
			if (!doports)
			{
				if (ns.layer != Generic.tech.portNode) total++;
			} else
			{
				if (ns.layer == Generic.tech.portNode) total++;
			}
		}
		if (total == 0)
		{
			System.out.println("There are no " + (doports ? "ports" : "layers") + " to identify");
			return;
		}

		// make arrays for position and association
		double [] xpos = new double[total];
		double [] ypos = new double[total];
		Poly.Type [] style = new Poly.Type[total];
		Parse.Sample [] whichsam = new Parse.Sample[total];
	
		// fill in label positions
		int qtotal = (total+3) / 4;
		Rectangle2D screen = wnd.getBoundsInWindow();
		double ysep = screen.getHeight() / qtotal;
		double xsep = screen.getWidth() / qtotal;
		double indent = screen.getHeight() / 15;
		for(int i=0; i<qtotal; i++)
		{
			// label on the left side
			xpos[i] = screen.getMinX() + indent;
			ypos[i] = screen.getMinY() + ysep * i + ysep/2;
			style[i] = Poly.Type.TEXTLEFT;
			if (i+qtotal < total)
			{
				// label on the top side
				xpos[i+qtotal] = screen.getMinX() + xsep * i + xsep/2;
				ypos[i+qtotal] = screen.getMaxY() - indent;
				style[i+qtotal] = Poly.Type.TEXTTOP;
			}
			if (i+qtotal*2 < total)
			{
				// label on the right side
				xpos[i+qtotal*2] = screen.getMaxX() - indent;
				ypos[i+qtotal*2] = screen.getMinY() + ysep * i + ysep/2;
				style[i+qtotal*2] = Poly.Type.TEXTRIGHT;
			}
			if (i+qtotal*3 < total)
			{
				// label on the bottom side
				xpos[i+qtotal*3] = screen.getMinX() + xsep * i + xsep/2;
				ypos[i+qtotal*3] = screen.getMinY() + indent;
				style[i+qtotal*3] = Poly.Type.TEXTBOT;
			}
		}
	
		// fill in sample associations
		int k = 0;
		for(Parse.Sample ns = nelist.firstsample; ns != null; ns = ns.nextsample)
		{
			if (!doports)
			{
				if (ns.layer != Generic.tech.portNode) whichsam[k++] = ns;
			} else
			{
				if (ns.layer == Generic.tech.portNode) whichsam[k++] = ns;
			}
		}
	
		// rotate through all configurations, finding least distance
		double bestdist = Double.MAX_VALUE;
		int bestrot = 0;
		for(int i=0; i<total; i++)
		{
			// find distance from each label to its sample center
			double dist = 0;
			for(int j=0; j<total; j++)
				dist += new Point2D.Double(xpos[j], ypos[j]).distance(new Point2D.Double(whichsam[j].xpos, whichsam[j].ypos));
			if (dist < bestdist)
			{
				bestdist = dist;
				bestrot = i;
			}
	
			// rotate the samples
			Parse.Sample ns = whichsam[0];
			for(int j=1; j<total; j++) whichsam[j-1] = whichsam[j];
			whichsam[total-1] = ns;
		}
	
		// rotate back to the best orientation
		for(int i=0; i<bestrot; i++)
		{
			Parse.Sample ns = whichsam[0];
			for(int j=1; j<total; j++) whichsam[j-1] = whichsam[j];
			whichsam[total-1] = ns;
		}
	
		// draw the highlighting
        Highlighter highlighter = wnd.getHighlighter();
		highlighter.clear();
		for(int i=0; i<total; i++)
		{
			Parse.Sample ns = whichsam[i];
			String msg = null;
			if (ns.layer == null)
			{
				msg = "HIGHLIGHT";
			} else if (ns.layer == Generic.tech.cellCenterNode)
			{
				msg = "GRAB";
			} else if (ns.layer == Generic.tech.portNode)
			{
				msg = us_tecedgetportname(ns.node);
				if (msg == null) msg = "?";
			} else msg = ns.layer.getName().substring(6);
//			poly.style = style[i];
			Point2D curPt = new Point2D.Double(xpos[i], ypos[i]);
			highlighter.addMessage(np, msg, curPt);

			SizeOffset so = ns.node.getSizeOffset();
			Rectangle2D nodeBounds = ns.node.getBounds();
			Point2D other = null;
			if (style[i] == Poly.Type.TEXTLEFT)
			{
				other = new Point2D.Double(nodeBounds.getMinX()+so.getLowXOffset(), nodeBounds.getCenterY());
//				poly.style = Poly.Type.TEXTBOTLEFT;
			} else if (style[i] == Poly.Type.TEXTRIGHT)
			{
				other = new Point2D.Double(nodeBounds.getMaxX()-so.getHighXOffset(), nodeBounds.getCenterY());
//				poly.style = Poly.Type.TEXTBOTRIGHT;
			} else if (style[i] == Poly.Type.TEXTTOP)
			{
				other = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getMaxY()-so.getHighYOffset());
//				poly.style = Poly.Type.TEXTTOPLEFT;
			} else if (style[i] == Poly.Type.TEXTBOT)
			{
				other = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getMinY()+so.getLowYOffset());
//				poly.style = Poly.Type.TEXTBOTLEFT;
			}
			highlighter.addLine(curPt, other, np);
		}
		highlighter.finished();
	}
	
	/**
	 * Method to return information about a given object.
	 */
	public static String us_teceditinquire(Geometric geom)
	{
		if (geom instanceof ArcInst)
		{
			// describe currently highlighted arc
			ArcInst ai = (ArcInst)geom;
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
			return "An arc";
		}
		NodeInst ni = (NodeInst)geom;
		Cell cell = ni.getParent();
		int opt = us_tecedgetoption(ni);
		if (opt < 0) return "No relevance";
	
		switch (opt)
		{
			case Generate.ARCFIXANG:
				return "Whether " + cell.describe() + " is fixed-angle";
			case Generate.ARCFUNCTION:
				return "The function of " + cell.describe();
			case Generate.ARCINC:
				return "The prefered angle increment of " + cell.describe();
			case Generate.ARCNOEXTEND:
				return "The arc extension of " + cell.describe();
			case Generate.ARCWIPESPINS:
				return "Thie arc coverage of " + cell.describe();
			case Generate.CENTEROBJ:
				return "The grab point of " + cell.describe();
			case Generate.LAYER3DHEIGHT:
				return "The 3D height of " + cell.describe();
			case Generate.LAYER3DTHICK:
				return "The 3D thickness of " + cell.describe();
			case Generate.LAYERTRANSPARENCY:
				return "The transparency layer of " + cell.describe();
			case Generate.LAYERCIF:
				return "The CIF name of " + cell.describe();
			case Generate.LAYERCOLOR:
				return "The color of " + cell.describe();
			case Generate.LAYERDXF:
				return "The DXF name(s) of " + cell.describe();
			case Generate.LAYERFUNCTION:
				return "The function of " + cell.describe();
			case Generate.LAYERGDS:
				return "The Calma GDS-II number of " + cell.describe();
			case Generate.LAYERPATCONT:
				return "A stipple-pattern controller";
			case Generate.LAYERPATTERN:
				return "One of the bitmap squares in " + cell.describe();
			case Generate.LAYERSPICAP:
				return "The SPICE capacitance of " + cell.describe();
			case Generate.LAYERSPIECAP:
				return "The SPICE edge capacitance of " + cell.describe();
			case Generate.LAYERSPIRES:
				return "The SPICE resistance of " + cell.describe();
			case Generate.LAYERSTYLE:
				return "The style of " + cell.describe();
			case Generate.LAYERPATCH:
			case Generate.HIGHLIGHTOBJ:
				Cell np = us_tecedgetlayer(ni);
				if (np == null) return "Highlight box";
				String msg = "Layer '" + np.getName().substring(6) + "'";
				Variable var = ni.getVar(Generate.MINSIZEBOX_KEY);
				if (var != null) msg += " (at minimum size)";
				return msg;
			case Generate.NODEFUNCTION:
				return "The function of " + cell.describe();
			case Generate.NODELOCKABLE:
				return "Whether " + cell.describe() + " can be locked (used in array technologies)";
			case Generate.NODEMULTICUT:
				return "The separation between multiple contact cuts in " + cell.describe();
			case Generate.NODESERPENTINE:
				return "Whether " + cell.describe() + " is a serpentine transistor";
			case Generate.NODESQUARE:
				return "Whether " + cell.describe() + " is square";
			case Generate.NODEWIPES:
				return "Whether " + cell.describe() + " disappears when conencted to one or two arcs";
			case Generate.PORTOBJ:
				String pt = us_tecedgetportname(ni);
				if (pt == null) return "Unnamed port";
				return "Port '" + pt + "'";
			case Generate.TECHDESCRIPT:
				return "The technology description";
			case Generate.TECHLAMBDA:
				return "The technology scale";
		}
		return "Unknown information";
	}
	
	/**
	 * Method to obtain the layer associated with node "ni".  Returns 0 if the layer is not
	 * there or invalid.  Returns NONODEPROTO if this is the highlight layer.
	 */
	static Cell us_tecedgetlayer(NodeInst ni)
	{
		Variable var = ni.getVar(Generate.LAYER_KEY);
		if (var == null) return null;
		Cell np = (Cell)var.getObject();
		if (np != null)
		{
			// validate the reference
			for(Iterator it = ni.getParent().getLibrary().getCells(); it.hasNext(); )
			{
				Cell oNp = (Cell)it.next();
				if (oNp == np) return np;
			}
		}
		return null;
	}
	
	/**
	 * Method to return the name of the technology-edit port on node "ni".  Typically,
	 * this is stored on the Generate.PORTNAME_KEY variable, but it may also be the node's name.
	 */
	static String us_tecedgetportname(NodeInst ni)
	{	
		Variable var = ni.getVar(Generate.PORTNAME_KEY);
		if (var != null) return (String)var.getObject();
		var = ni.getVar(NodeInst.NODE_NAME);
		if (var != null) return (String)var.getObject();
		return null;
	}
	
	/**
	 * Method for modifying the selected object.  If two are selected, connect them.
	 */
	public static void us_teceditmodobject(EditWindow wnd, NodeInst ni, int opt)
	{
		// handle other cases
		switch (opt)
		{
			case Generate.PORTOBJ:           us_tecedmodport(wnd, ni);                break;
			case Generate.LAYERFUNCTION:     us_tecedlayerfunction(wnd, ni);          break;
			case Generate.LAYERCOLOR:        us_tecedlayercolor(wnd, ni);             break;
			case Generate.LAYERTRANSPARENCY: us_tecedlayertransparency(wnd, ni);      break;
			case Generate.LAYERSTYLE:        us_tecedlayerstyle(wnd, ni);             break;
			case Generate.LAYERCIF:          us_tecedlayercif(wnd, ni);               break;
			case Generate.LAYERGDS:          us_tecedlayergds(wnd, ni);               break;
			case Generate.LAYERDXF:          us_tecedlayerdxf(wnd, ni);               break;
			case Generate.LAYERSPIRES:       us_tecedlayerspires(wnd, ni);            break;
			case Generate.LAYERSPICAP:       us_tecedlayerspicap(wnd, ni);            break;
			case Generate.LAYERSPIECAP:      us_tecedlayerspiecap(wnd, ni);           break;
			case Generate.LAYER3DHEIGHT:     us_tecedlayer3dheight(wnd, ni);          break;
			case Generate.LAYER3DTHICK:      us_tecedlayer3dthick(wnd, ni);           break;
			case Generate.LAYERPATTERN:      us_tecedlayerpattern(wnd, ni);           break;
			case Generate.LAYERPATCONT:      us_tecedlayerpatterncontrol(wnd, ni, 0); break;
			case Generate.LAYERPATCLEAR:     us_tecedlayerpatterncontrol(wnd, ni, 1); break;
			case Generate.LAYERPATINVERT:    us_tecedlayerpatterncontrol(wnd, ni, 2); break;
			case Generate.LAYERPATCOPY:      us_tecedlayerpatterncontrol(wnd, ni, 3); break;
			case Generate.LAYERPATPASTE:     us_tecedlayerpatterncontrol(wnd, ni, 4); break;
			case Generate.LAYERPATCH:        us_tecedlayertype(wnd, ni);              break;
			case Generate.ARCFIXANG:         us_tecedarcfixang(wnd, ni);              break;
			case Generate.ARCFUNCTION:       us_tecedarcfunction(wnd, ni);            break;
			case Generate.ARCINC:            us_tecedarcinc(wnd, ni);                 break;
			case Generate.ARCNOEXTEND:       us_tecedarcnoextend(wnd, ni);            break;
			case Generate.ARCWIPESPINS:      us_tecedarcwipes(wnd, ni);               break;
			case Generate.NODEFUNCTION:      us_tecednodefunction(wnd, ni);           break;
			case Generate.NODELOCKABLE:      us_tecednodelockable(wnd, ni);           break;
			case Generate.NODEMULTICUT:      us_tecednodemulticut(wnd, ni);           break;
			case Generate.NODESERPENTINE:    us_tecednodeserpentine(wnd, ni);         break;
			case Generate.NODESQUARE:        us_tecednodesquare(wnd, ni);             break;
			case Generate.NODEWIPES:         us_tecednodewipes(wnd, ni);              break;
			case Generate.TECHDESCRIPT:      us_tecedinfodescript(wnd, ni);           break;
			case Generate.TECHLAMBDA:        us_tecedinfolambda(wnd, ni);             break;
			default:
				System.out.println("Cannot modify this object");
				break;
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

	static int [] copiedPattern = null;

	private static void us_tecedlayerpatterncontrol(EditWindow wnd, NodeInst ni, int forced)
	{
		if (forced == 0)
		{
			String [] operationNames = new String[4];
			operationNames[0] = "Clear Pattern";
			operationNames[1] = "Invert Pattern";
			operationNames[2] = "Copy Pattern";
			operationNames[3] = "Paste Pattern";
			String choice = PromptAt.showPromptAt(wnd, ni, "Pattern Operations", null, "", operationNames);
			if (choice == null) return;
			if (choice.equals("Clear Pattern")) forced = 1; else
			if (choice.equals("Invert Pattern")) forced = 2; else
			if (choice.equals("Copy Pattern")) forced = 3; else
			if (choice.equals("Paste Pattern")) forced = 4;
		}
		switch (forced)
		{
			case 1:		// clear pattern
				for(Iterator it = ni.getParent().getNodes(); it.hasNext(); )
				{
					NodeInst pni = (NodeInst)it.next();
					int opt = us_tecedgetoption(pni);
					if (opt != Generate.LAYERPATTERN) continue;
					int color = us_tecedlayergetpattern(pni);
					if (color != 0)
						us_tecedlayersetpattern(pni, 0);
				}
		
				// redraw the demo layer in this cell
				us_tecedredolayergraphics(ni.getParent());
				break;
			case 2:		// invert pattern
				for(Iterator it = ni.getParent().getNodes(); it.hasNext(); )
				{
					NodeInst pni = (NodeInst)it.next();
					int opt = us_tecedgetoption(pni);
					if (opt != Generate.LAYERPATTERN) continue;
					int color = us_tecedlayergetpattern(pni);
					us_tecedlayersetpattern(pni, ~color);
				}
		
				// redraw the demo layer in this cell
				us_tecedredolayergraphics(ni.getParent());
				break;
			case 3:		// copy pattern
				Generate.LayerInfo li = Generate.LayerInfo.us_teceditgetlayerinfo(ni.getParent());
				if (li == null) return;
				copiedPattern = li.desc.getPattern();
				break;
			case 4:		// paste pattern
				if (copiedPattern == null) return;
				us_teceditsetlayerpattern(ni.getParent(), copiedPattern);
		
				// redraw the demo layer in this cell
				us_tecedredolayergraphics(ni.getParent());
				break;
		}
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

	/**
	 * Method to get a list of layers in the current library (in the proper order).
	 * @return an array of strings with the names of the layers.
	 */
	static String [] us_tecedgetlayernamelist()
	{
		Library [] dependentlibs = us_teceditgetdependents(Library.getCurrent());
		Cell [] layerCells = us_teceditfindsequence(dependentlibs, "layer-", Generate.LAYERSEQUENCE_KEY);
	
		// build and fill array of layers for DRC parsing
		String [] layerNames = new String[layerCells.length];
		for(int i=0; i<layerCells.length; i++)
			layerNames[i] = layerCells[i].getName().substring(6);
		return layerNames;
	}

	/**
	 * Method to get a list of arcs in the current library (in the proper order).
	 * @return an array of strings with the names of the arcs.
	 */
	static String [] us_tecedgetarcnamelist()
	{
		Library [] dependentlibs = us_teceditgetdependents(Library.getCurrent());
		Cell [] arcCells = us_teceditfindsequence(dependentlibs, "arc-", Generate.ARCSEQUENCE_KEY);
	
		// build and fill array of layers for DRC parsing
		String [] arcNames = new String[arcCells.length];
		for(int i=0; i<arcCells.length; i++)
			arcNames[i] = arcCells[i].getName().substring(4);
		return arcNames;
	}

	/**
	 * Method to get a list of arcs in the current library (in the proper order).
	 * @return an array of strings with the names of the arcs.
	 */
	static String [] us_tecedgetnodenamelist()
	{
		Library [] dependentlibs = us_teceditgetdependents(Library.getCurrent());
		Cell [] nodeCells = us_teceditfindsequence(dependentlibs, "node-", Generate.NODESEQUENCE_KEY);
	
		// build and fill array of nodes
		String [] nodeNames = new String[nodeCells.length];
		for(int i=0; i<nodeCells.length; i++)
			nodeNames[i] = nodeCells[i].getName().substring(5);
		return nodeNames;
	}

	/**
	 * Method to get the list of libraries that are used in the construction
	 * of library "lib".  Returns an array of libraries, terminated with "lib".
	 */
	public static Library [] us_teceditgetdependents(Library lib)
	{
		// get list of dependent libraries
		List dependentLibs = new ArrayList();
		Variable var = lib.getVar(Generate.DEPENDENTLIB_KEY);
		if (var != null)
		{
			String [] libNames = (String [])var.getObject();
			for(int i=0; i<libNames.length; i++)
			{
				String pt = libNames[i];
				Library dLib = Library.findLibrary(pt);
				if (dLib == null)
				{
					System.out.println("Cannot find dependent technology library " + pt + ", ignoring");
					continue;
				}
				if (dLib == lib)
				{
					System.out.println("Library " + lib.getName() + " cannot depend on itself, ignoring dependency");
					continue;
				}
				dependentLibs.add(dLib);
			}
		}
		dependentLibs.add(lib);
		Library [] theLibs = new Library[dependentLibs.size()];
		for(int i=0; i<dependentLibs.size(); i++)
			theLibs[i] = (Library)dependentLibs.get(i);
		return theLibs;
	}

	/**
	 * general-purpose method to scan the libraries in "dependentlibs",
	 * looking for cells that begin with the string "match".  It then uses the
	 * variable "seqname" on the last library to determine an ordering of the cells.
	 * Then, it returns the cells in an array.
	 */
	public static Cell [] us_teceditfindsequence(Library [] dependentlibs, String match, Variable.Key seqKey)
	{
		// look backwards through libraries for the appropriate cells
		int total = 0;
		List npList = new ArrayList();
		for(int i=dependentlibs.length-1; i>=0; i--)
		{
			Library olderlib = dependentlibs[i];
			for(Iterator it = olderlib.getCells(); it.hasNext(); )
			{
				Cell np = (Cell)it.next();
				if (!np.getName().startsWith(match)) continue;

				// see if this cell is used in a later library
				boolean foundInLater = false;
				for(int j=i+1; j<dependentlibs.length; j++)
				{
					Library laterLib = dependentlibs[j];
					for(Iterator oIt = laterLib.getCells(); oIt.hasNext(); )
					{
						Cell lNp = (Cell)oIt.next();
						if (!lNp.getName().equals(np.getName())) continue;
						foundInLater = true;

						// got older and later version of same cell: check dates
						if (lNp.getRevisionDate().before(np.getRevisionDate()))
							System.out.println("Warning: library " + olderlib.getName() + " has newer " + np.getName() +
								" than library " + laterLib.getName());
						break;
					}
					if (foundInLater) break;
				}
	
				// if no later library has this, add to total
				if (!foundInLater) npList.add(np);
			}
		}
	
		// if there is no sequence, simply return the list
		Variable var = dependentlibs[dependentlibs.length-1].getVar(seqKey);
		if (var == null) return (Cell [])npList.toArray();

		// build a new list with the sequence
		List sequence = new ArrayList();
		String [] sequenceNames = (String [])var.getObject();
		for(int i=0; i<sequenceNames.length; i++)
		{
			Cell foundCell = null;
			for(int l = 0; l < npList.size(); l++)
			{
				Cell np = (Cell)npList.get(l);
				if (np.getName().substring(match.length()).equals(sequenceNames[i])) { foundCell = np;   break; }
			}
			if (foundCell != null)
			{
				sequence.add(foundCell);
				npList.remove(foundCell);
			}
		}
		for(Iterator it = npList.iterator(); it.hasNext(); )
			sequence.add(it.next());
		Cell [] theCells = new Cell[sequence.size()];
		for(int i=0; i<sequence.size(); i++)
			theCells[i] = (Cell)sequence.get(i);
		return theCells;
	}

	/**
	 * Method to modify the layer information in node "ni".
	 */
	static void us_tecedlayertype(EditWindow wnd, NodeInst ni)
	{
		Library [] dependentlibs = us_teceditgetdependents(Library.getCurrent());
		Cell [] layerCells = us_teceditfindsequence(dependentlibs, "layer-", Generate.LAYERSEQUENCE_KEY);
		if (layerCells == null) return;

		String [] options = new String[layerCells.length + 2];
		for(int i=0; i<layerCells.length; i++)
			options[i] = layerCells[i].getName().substring(6);
		options[layerCells.length] = "SET-MINIMUM-SIZE";
		options[layerCells.length+1] = "CLEAR-MINIMUM-SIZE";
		String initial = options[0];
		Variable curLay = ni.getVar(Generate.LAYER_KEY);
		if (curLay != null) initial = ((Cell)curLay.getObject()).getName().substring(6);
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Layer", "New layer for this geometry:", initial, options);
		if (choice == null) return;

		// save the results
		ModifyLayerJob job = new ModifyLayerJob(ni, choice, layerCells);
	}

    /**
     * Class to modify a port object in a node of the technology editor.
     */
    public static class ModifyLayerJob extends Job
	{
		private NodeInst ni;
		private String choice;
		private Cell [] layerCells;

		public ModifyLayerJob(NodeInst ni, String choice, Cell [] layerCells)
		{
			super("Change Layer Information", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.choice = choice;
			this.layerCells = layerCells;
			startJob();
		}

		public boolean doIt()
		{
			if (choice.equals("SET-MINIMUM-SIZE"))
			{
				if (!ni.getParent().getName().startsWith("node-"))
				{
					System.out.println("Can only set minimum size in node descriptions");
					return true;
				}
				Variable var = ni.newVar(Generate.MINSIZEBOX_KEY, "MIN");
				if (var != null) var.setDisplay(true);
				return true;
			}
		
			if (choice.equals("CLEAR-MINIMUM-SIZE"))
			{
				if (ni.getVar(Generate.MINSIZEBOX_KEY) == null)
				{
					System.out.println("Minimum size is not set on this layer");
					return true;
				}
				ni.delVar(Generate.MINSIZEBOX_KEY);
				return true;
			}
		
			// find the actual cell with that layer specification
			for(int i=0; i<layerCells.length; i++)
			{
				if (choice.equals(layerCells[i].getName().substring(6)))
				{
					// found the name, set the patch
					Generate.LayerInfo li = Generate.LayerInfo.us_teceditgetlayerinfo(layerCells[i]);
					if (li == null) return true;
					us_teceditsetpatch(ni, li.desc);
					ni.newVar(Generate.LAYER_KEY, layerCells[i]);
				}
			}
			System.out.println("Cannot find layer primitive " + choice);
			return true;
		}
	}
	
	/**
	 * Method to modify port characteristics
	 */
	static void us_tecedmodport(EditWindow wnd, NodeInst ni)
	{
		// count the number of arcs in this technology
		List allArcs = new ArrayList();
		for(Iterator it = ni.getParent().getLibrary().getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			if (cell.getName().startsWith("arc-")) allArcs.add(cell);
		}

		// make a set of those arcs which can connect to this port
		HashSet connectSet = new HashSet();
		Variable var = ni.getVar(Generate.CONNECTION_KEY);
		if (var != null)
		{
			Cell [] connects = (Cell [])var.getObject();
			for(int i=0; i<connects.length; i++)
				connectSet.add(connects[i]);
		}

		// build an array of arc connections
		PromptAt.Field [] fields = new PromptAt.Field[allArcs.size()+2];
		for(int i=0; i<allArcs.size(); i++)
		{
			Cell cell = (Cell)allArcs.get(i);
			boolean doesConnect = connectSet.contains(cell);
			fields[i] = new PromptAt.Field(cell.getName().substring(4),
				new String [] {"Allowed", "Disallowed"}, (doesConnect ? "Allowed" : "Disallowed"));
		}
		Variable angVar = ni.getVar(Generate.PORTANGLE_KEY);
		int ang = 0;
		if (angVar != null) ang = ((Integer)angVar.getObject()).intValue();
		Variable rangeVar = ni.getVar(Generate.PORTRANGE_KEY);
		int range = 180;
		if (rangeVar != null) range = ((Integer)rangeVar.getObject()).intValue();
		fields[allArcs.size()] = new PromptAt.Field("Angle:", ang);
		fields[allArcs.size()+1] = new PromptAt.Field("Angle Range:", range);
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Port", fields);
		if (choice == null) return;

		// save the results
		ModifyPortJob job = new ModifyPortJob(ni, allArcs, fields);
	}

    /**
     * Class to modify a port object in a node of the technology editor.
     */
    public static class ModifyPortJob extends Job
	{
		private NodeInst ni;
		private List allArcs;
		private PromptAt.Field [] fields;

		public ModifyPortJob(NodeInst ni, List allArcs, PromptAt.Field [] fields)
		{
			super("Change Port Information", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.allArcs = allArcs;
			this.fields = fields;
			startJob();
		}

		public boolean doIt()
		{
			int numConnects = 0;
			for(int i=0; i<allArcs.size(); i++)
			{
				String answer = (String)fields[i].getFinal();
				if (answer.equals("Allowed")) numConnects++;
			}
			Cell [] newConnects = new Cell[numConnects];
			int k = 0;
			for(int i=0; i<allArcs.size(); i++)
			{
				String answer = (String)fields[i].getFinal();
				if (answer.equals("Allowed")) newConnects[k++] = (Cell)allArcs.get(i);
			}
			ni.newVar(Generate.CONNECTION_KEY, newConnects);

			Integer newAngle = (Integer)fields[allArcs.size()].getFinal();
			ni.newVar(Generate.PORTANGLE_KEY, newAngle);
			Integer newRange = (Integer)fields[allArcs.size()+1].getFinal();
			ni.newVar(Generate.PORTRANGE_KEY, newRange);
			return true;
		}
	}

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

	public static String getValueOnNode(NodeInst ni)
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
			Variable var = cell.getVar(Generate.COLORNODE_KEY);
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
					Variable varLay = cNi.getVar(Generate.LAYER_KEY);
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
//		var = getval((INTBIG)lib, VLIBRARY, VINTEGER|VISARRAY, Generate.COLORMAP_KEY);
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
	
	/**
	 * Method to set the layer-pattern squares of cell "np" to the bits in "desc".
	 */
	private static void us_teceditsetlayerpattern(Cell np, int [] pattern)
	{
		// look at all nodes in the layer description cell
		int patterncount = 0;
		Rectangle2D patternBounds = null;
		for(Iterator it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() == Artwork.tech.boxNode || ni.getProto() == Artwork.tech.filledBoxNode)
			{
				Variable var = ni.getVar(Generate.OPTION_KEY);
				if (var == null) continue;
				if (((Integer)var.getObject()).intValue() != Generate.LAYERPATTERN) continue;
				Rectangle2D bounds = ni.getBounds();
				if (patterncount == 0)
				{
					patternBounds = bounds;
				} else
				{
					Rectangle2D.union(patternBounds, bounds, patternBounds);
				}
				patterncount++;
			}
		}
	
		if (patterncount != 16*16 && patterncount != 16*8)
		{
			System.out.println("Incorrect number of pattern boxes in " + np.describe() +
				" (has " + patterncount + ", not " + (16*16) + ")");
			return;
		}
	
		// set the pattern
		for(Iterator it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() != Artwork.tech.boxNode && ni.getProto() != Artwork.tech.filledBoxNode) continue;
			Variable var = ni.getVar(Generate.OPTION_KEY);
			if (var == null) continue;
			if (((Integer)var.getObject()).intValue() != Generate.LAYERPATTERN) continue;

			Rectangle2D niBounds = ni.getBounds();
			int x = (int)((niBounds.getMinX() - patternBounds.getMinX()) / (patternBounds.getWidth() / 16));
			int y = (int)((patternBounds.getMaxY() - niBounds.getMaxY()) / (patternBounds.getHeight() / 16));
			int wantColor = 0;
			if ((pattern[y] & (1 << (15-x))) != 0) wantColor = 0xFFFF;
	
			int color = us_tecedlayergetpattern(ni);
			if (color != wantColor)
				us_tecedlayersetpattern(ni, wantColor);
		}
	}
	
	/**
	 * Method to return the option index of node "ni"
	 */
	public static int us_tecedgetoption(NodeInst ni)
	{
		// port objects are readily identifiable
		if (ni.getProto() == Generic.tech.portNode) return Generate.PORTOBJ;
	
		// center objects are also readily identifiable
		if (ni.getProto() == Generic.tech.cellCenterNode) return Generate.CENTEROBJ;
	
		Variable var = ni.getVar(Generate.OPTION_KEY);
		if (var == null) return -1;
		int option = ((Integer)var.getObject()).intValue();
		if (option == Generate.LAYERPATCH)
		{
			// may be a highlight object
			Variable var2 = ni.getVar(Generate.LAYER_KEY);
			if (var2 != null)
			{
				if (var2.getObject() == null) return Generate.HIGHLIGHTOBJ;
			}
		}
		return option;
	}

	/******************** SUPPORT FOR "usredtecp.c" ROUTINES ********************/
	
	public static void us_reorderprimdlog(int type)
	{
		RearrangeOrder dialog = new RearrangeOrder();
		dialog.lib = Library.getCurrent();
		dialog.type = type;
		dialog.initComponents();
		dialog.setVisible(true);
	}

	/**
	 * This class displays a dialog for rearranging layers, arcs, or nodes in a technology library.
	 */
	public static class RearrangeOrder extends EDialog
	{
		private JList list;
		private DefaultListModel model;
		private Library lib;
		private int type;

		/** Creates new form Rearrange technology components */
		public RearrangeOrder()
		{
			super(null, true);
		}
		
		private void ok() { exit(true); }
		
		protected void escapePressed() { exit(false); }
		 
		// Call this method when the user clicks the OK button
		private void exit(boolean goodButton)
		{
			if (goodButton)
			{
				String [] newList = new String[model.size()];
				for(int i=0; i<model.size(); i++)
					newList[i] = (String)model.getElementAt(i);
				new UpdateOrderingJob(lib, newList, type);
			}
			dispose();
		}

		private static class UpdateOrderingJob extends Job
		{
			private Library lib;
			private String [] newList;
			private int type;
		
			public UpdateOrderingJob(Library lib, String [] newList, int type)
			{
				super("Update Ordering", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
				this.lib = lib;
				this.newList = newList;
				this.type = type;
				startJob();
			}

			public boolean doIt()
			{
				switch (type)
				{
					case 1: lib.newVar(Generate.LAYERSEQUENCE_KEY, newList);   break;
					case 2: lib.newVar(Generate.ARCSEQUENCE_KEY, newList);     break;
					case 3: lib.newVar(Generate.NODESEQUENCE_KEY, newList);    break;
				}
				return true;
			}
		}

		/**
		 * Call when an up/down button is pressed.
		 * @param direction -2: far down   -1: down   1: up   2: far up
		 */
		private void moveSelected(int direction)
		{
			int index = list.getSelectedIndex();
			if (index < 0) return;
			int newIndex = index;
			switch (direction)
			{
				case -2: newIndex -= 10;   break;
				case -1: newIndex -= 1;    break;
				case  1: newIndex += 1;    break;
				case  2: newIndex += 10;   break;
			}
			if (newIndex < 0) newIndex = 0;
			if (newIndex >= model.size()) newIndex = model.size()-1;
			Object was = model.getElementAt(index);
			model.remove(index);
			model.add(newIndex, was);
			list.setSelectedIndex(newIndex);
			list.ensureIndexIsVisible(newIndex);
		}


		private void initComponents()
		{
			getContentPane().setLayout(new GridBagLayout());

			switch (type)
			{
				case 1: setTitle("Rearrange Layer Order");   break;
				case 2: setTitle("Rearrange Arc Order");     break;
				case 3: setTitle("Rearrange Node Order");    break;
			}
			setName("");
			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt) { exit(false); }
			});

			JScrollPane center = new JScrollPane();
			center.setMinimumSize(new java.awt.Dimension(100, 50));
			center.setPreferredSize(new java.awt.Dimension(300, 200));
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;      gbc.gridy = 1;
			gbc.weightx = 1;    gbc.weighty = 1;
			gbc.gridwidth = 2;  gbc.gridheight = 4;
			gbc.anchor = java.awt.GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
	        getContentPane().add(center, gbc);

			model = new DefaultListModel();
			list = new JList(model);
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			center.setViewportView(list);

			model.clear();
			String [] listNames = null;
			switch (type)
			{
				case 1: listNames = us_tecedgetlayernamelist();   break;
				case 2: listNames = us_tecedgetarcnamelist();     break;
				case 3: listNames = us_tecedgetnodenamelist();    break;
			}
			for(int i=0; i<listNames.length; i++)
				model.addElement(listNames[i]);

			JButton farUp = new JButton("Far Up");
			gbc = new GridBagConstraints();
			gbc.gridx = 2;   gbc.gridy = 1;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(farUp, gbc);
			farUp.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { moveSelected(-2); }
			});

			JButton up = new JButton("Up");
			gbc = new GridBagConstraints();
			gbc.gridx = 2;   gbc.gridy = 2;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(up, gbc);
			up.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { moveSelected(-1); }
			});

			JButton down = new JButton("Down");
			gbc = new GridBagConstraints();
			gbc.gridx = 2;   gbc.gridy = 3;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(down, gbc);
			down.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { moveSelected(1); }
			});

			JButton farDown = new JButton("Far Down");
			gbc = new GridBagConstraints();
			gbc.gridx = 2;   gbc.gridy = 4;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(farDown, gbc);
			farDown.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { moveSelected(2); }
			});

			// OK and Cancel
			JButton cancel = new JButton("Cancel");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 5;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(cancel, gbc);
			cancel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { exit(false); }
			});

			JButton ok = new JButton("OK");
			getRootPane().setDefaultButton(ok);
			gbc = new java.awt.GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 5;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(ok, gbc);
			ok.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent evt) { exit(true); }
			});

			pack();
		}
	}

	/**
	 * Method to print detailled information about a given technology.
	 * @param tech the technology to describe.
	 */
	public static void us_printtechnology(Technology tech)
	{
		// ***************************** dump layers ******************************

		// allocate space for all layer fields
		int layerCount = tech.getNumLayers();
		String [] layerNames = new String[layerCount+1];
		String [] layerColors = new String[layerCount+1];
		String [] layerStyles = new String[layerCount+1];
		String [] layerCifs = new String[layerCount+1];
		String [] layerGdss = new String[layerCount+1];
		String [] layerFuncs = new String[layerCount+1];

		// load the header
		layerNames[0] = "Layer";
		layerColors[0] = "Color";
		layerStyles[0] = "Style";
		layerCifs[0] = "CIF";
		layerGdss[0] = "GDS";
		layerFuncs[0] = "Function";

		// compute each layer
		for(int i=0; i<layerCount; i++)
		{
			Layer layer = tech.getLayer(i);
			layerNames[i+1] = layer.getName();

			EGraphics gra = layer.getGraphics();
			if (gra.getTransparentLayer() > 0) layerColors[i+1] = "Transparent " + gra.getTransparentLayer(); else
			{
				Color col = gra.getColor();
				layerColors[i+1] = "(" + col.getRed() + "," + col.getGreen() + "," + col.getBlue() + ")";
			}

			layerStyles[i+1] = "?";
			if (gra.isPatternedOnDisplay())
			{
				if (gra.isOutlinedOnDisplay()) layerStyles[i+1] = "pat/outl"; else
					layerStyles[i+1] = "pat";
			} else
			{
				layerStyles[i+1] = "solid";
			}

			layerCifs[i+1] = layer.getCIFLayer();
			layerGdss[i+1] = layer.getGDSLayer();
			layerFuncs[i+1] = layer.getFunction().toString();
		}

		// write the layer information
		String [][] fields = new String[6][];
		fields[0] = layerNames;   fields[1] = layerColors;   fields[2] = layerStyles;
		fields[3] = layerCifs;    fields[4] = layerGdss;     fields[5] = layerFuncs;
		us_dumpfields(fields, layerCount+1, "LAYERS");

		// ****************************** dump arcs ******************************

		// allocate space for all arc fields
		int tot = 1;
		for(Iterator it = tech.getArcs(); it.hasNext(); )
		{
			PrimitiveArc ap = (PrimitiveArc)it.next();
			ArcInst ai = ArcInst.makeDummyInstance(ap, 4000);
			Poly [] polys = tech.getShapeOfArc(ai);
			tot += polys.length;
		}
		String [] arcNames = new String[tot];
		String [] arcLayers = new String[tot];
		String [] arcLayerSizes = new String[tot];
		String [] arcExtensions = new String[tot];
		String [] arcAngles = new String[tot];
		String [] arcWipes = new String[tot];
		String [] arcFuncs = new String[tot];

		// load the header
		arcNames[0] = "Arc";
		arcLayers[0] = "Layer";
		arcLayerSizes[0] = "Size";
		arcExtensions[0] = "Extend";
		arcAngles[0] = "Angle";
		arcWipes[0] = "Wipes";
		arcFuncs[0] = "Function";

		tot = 1;
		for(Iterator it = tech.getArcs(); it.hasNext(); )
		{
			PrimitiveArc ap = (PrimitiveArc)it.next();
			arcNames[tot] = ap.getName();
			arcExtensions[tot] = (ap.isExtended() ? "yes" : "no");
			arcAngles[tot] = "" + ap.getAngleIncrement();
			arcWipes[tot] = (ap.isWipable() ? "yes" : "no");
			arcFuncs[tot] = ap.getFunction().toString();

			ArcInst ai = ArcInst.makeDummyInstance(ap, 4000);
			ai.setExtended(ArcInst.HEADEND, false);
			ai.setExtended(ArcInst.TAILEND, false);
			Poly [] polys = tech.getShapeOfArc(ai);
			for(int k=0; k<polys.length; k++)
			{
				Poly poly = polys[k];
				arcLayers[tot] = poly.getLayer().getName();
				double area = poly.getArea() / ai.getLength();
				arcLayerSizes[tot] = TextUtils.formatDouble(area);
				if (k > 0)
				{
					arcNames[tot] = "";
					arcExtensions[tot] = "";
					arcAngles[tot] = "";
					arcWipes[tot] = "";
					arcFuncs[tot] = "";
				}
				tot++;
			}
		}

		// write the arc information
		fields = new String[7][];
		fields[0] = arcNames;        fields[1] = arcLayers;   fields[2] = arcLayerSizes;
		fields[3] = arcExtensions;   fields[4] = arcAngles;   fields[5] = arcWipes;
		fields[6] = arcFuncs;
		us_dumpfields(fields, tot, "ARCS");

		// ****************************** dump nodes ******************************

		// allocate space for all node fields
		int total = 1;
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			NodeInst ni = NodeInst.makeDummyInstance(np);
			Poly [] polys = tech.getShapeOfNode(ni);
			int l = 0;
			for(Iterator pIt = np.getPorts(); pIt.hasNext(); )
			{
				PrimitivePort pp = (PrimitivePort)pIt.next();
				int m = 0;
				ArcProto [] apArray = pp.getConnections();
				for(int k=0; k<apArray.length; k++)
					if (apArray[k].getTechnology() == tech) m++;
				if (m == 0) m = 1;
				l += m;
			}
			total += Math.max(polys.length, l);
		}
		String [] nodeNames = new String[total];
		String [] nodeFuncs = new String[total];
		String[] nodeLayers = new String[total];
		String [] nodeLayerSizes = new String[total];
		String [] nodePorts = new String[total];
		String [] nodePortSizes = new String[total];
		String [] nodePortAngles = new String[total];
		String [] nodeConnections = new String[total];

		// load the header
		nodeNames[0] = "Node";
		nodeFuncs[0] = "Function";
		nodeLayers[0] = "Layers";
		nodeLayerSizes[0] = "Size";
		nodePorts[0] = "Ports";
		nodePortSizes[0] = "Size";
		nodePortAngles[0] = "Angle";
		nodeConnections[0] = "Connections";

		tot = 1;
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			int base = tot;
			nodeNames[tot] = np.getName();
			nodeFuncs[tot] = np.getFunction().getName();

			NodeInst ni = NodeInst.makeDummyInstance(np);
			Poly [] polys = tech.getShapeOfNode(ni);
			for(int k=0; k<polys.length; k++)
			{
				Poly poly = polys[k];
				if (tot >= total)
				{
					System.out.println("ARRAY OVERFLOW: LIMIT IS " + total);
					break;
				}
				nodeLayers[tot] = poly.getLayer().getName();
				Rectangle2D polyBounds = poly.getBounds2D();
				nodeLayerSizes[tot] = polyBounds.getWidth() + " x " + polyBounds.getHeight();
				if (k > 0)
				{
					nodeNames[tot] = "";
					nodeFuncs[tot] = "";
				}
				tot++;
			}
			for(Iterator pIt = np.getPorts(); pIt.hasNext(); )
			{
				PrimitivePort pp = (PrimitivePort)pIt.next();
				nodePorts[base] = pp.getName();
				Poly portPoly = ni.getShapeOfPort(pp);
				Rectangle2D portRect = portPoly.getBounds2D();
				nodePortSizes[base] = portRect.getWidth() + " x " + portRect.getHeight();
				if (pp.getAngleRange() == 180) nodePortAngles[base] = ""; else
					nodePortAngles[base] = "" + pp.getAngleRange();
				int m = 0;
				ArcProto [] conList = pp.getConnections();
				for(int k=0; k<conList.length; k++)
				{
					if (conList[k].getTechnology() != tech) continue;
					nodeConnections[base] = conList[k].getName();
					if (m != 0)
					{
						nodePorts[base] = "";
						nodePortSizes[base] = "";
						nodePortAngles[base] = "";
					}
					m++;
					base++;
				}
				if (m == 0) nodeConnections[base++] = "<NONE>";
			}
			for( ; base < tot; base++)
			{
				nodePorts[base] = "";
				nodePortSizes[base] = "";
				nodePortAngles[base] = "";
				nodeConnections[base] = "";
			}
			for( ; tot < base; tot++)
			{
				nodeNames[tot] = "";
				nodeFuncs[tot] = "";
				nodeLayers[tot] = "";
				nodeLayerSizes[tot] = "";
			}
		}

		// write the node information */
		fields = new String[8][];
		fields[0] = nodeNames;        fields[1] = nodeFuncs;    fields[2] = nodeLayers;
		fields[3] = nodeLayerSizes;   fields[4] = nodePorts;    fields[5] = nodePortSizes;
		fields[6] = nodePortAngles;   fields[7] = nodeConnections;
		us_dumpfields(fields, tot, "NODES");
	}

	static void us_dumpfields(String [][] fields, int length, String title)
	{
		int totwid = 0;
		int [] widths = new int[fields.length];
		for(int i=0; i<fields.length; i++)
		{
			widths[i] = 8;
			for(int j=0; j<length; j++)
			{
				if (fields[i][j] == null) continue;
				int len = fields[i][j].length();
				if (len > widths[i]) widths[i] = len;
			}
			widths[i]++;
			totwid += widths[i];
		}

		int stars = (totwid - title.length() - 2) / 2;
		for(int i=0; i<stars; i++) System.out.print("*");
		System.out.print(" " + title + " ");
		for(int i=0; i<stars; i++) System.out.print("*");
		System.out.println();

		for(int j=0; j<length; j++)
		{
			for(int i=0; i<fields.length; i++)
			{
				int len = 0;
				if (fields[i][j] != null)
				{
					System.out.print(fields[i][j]);
					len = fields[i][j].length();
				}
				if (i == fields.length-1) continue;
				for(int k=len; k<widths[i]; k++) System.out.print(" ");
			}
			System.out.println();
		}
		System.out.println();
	}
}
