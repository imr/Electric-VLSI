/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DRCSchematics.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.drc;

import com.sun.electric.database.hierarchy.Cell;

public class DRCSchematics
{
	public static void dr_checkschematiccellrecursively(Cell cell)
	{
//		cell->temp1 = 1;
//		for(ni = cell->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			if (ni->proto->primindex != 0) continue;
//			cnp = contentsview(ni->proto);
//			if (cnp == NONODEPROTO) cnp = ni->proto;
//			if (cnp->temp1 != 0) continue;
//
//			// ignore documentation icon
//			if (isiconof(ni->proto, cell)) continue;
//
//			dr_checkschematiccellrecursively(cnp);
//		}
//
//		// now check this cell
//		ttyputmsg(_("Checking schematic cell %s"), describenodeproto(cell));
//		dr_checkschematiccell(cell, FALSE);
	}

//	void dr_checkschematiccell(NODEPROTO *cell, BOOLEAN justthis)
//	{
//		REGISTER NODEINST *ni;
//		REGISTER ARCINST *ai;
//		REGISTER INTBIG errorcount, initialerrorcount, thiserrors;
//		REGISTER CHAR *indent;
//
//		if (justthis) initerrorlogging(_("Schematic DRC"));
//		initialerrorcount = numerrors();
//		for(ni = cell->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//			dr_schdocheck(ni->geom);
//		for(ai = cell->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst)
//			dr_schdocheck(ai->geom);
//		errorcount = numerrors();
//		thiserrors = errorcount - initialerrorcount;
//		if (justthis) indent = x_(""); else
//			indent = x_("   ");
//		if (thiserrors == 0) ttyputmsg(_("%sNo errors found"), indent); else
//			ttyputmsg(_("%s%ld errors found"), indent, thiserrors);
//		if (justthis) termerrorlogging(TRUE);
//	}

	/*
	 * Routine to check schematic object "geom".
	 */
//	void dr_schdocheck(GEOM *geom)
//	{
//		REGISTER NODEPROTO *cell, *np;
//		REGISTER NODEINST *ni;
//		REGISTER ARCINST *ai;
//		ARCINST **thearcs;
//		REGISTER PORTARCINST *pi;
//		REGISTER PORTPROTO *pp;
//		REGISTER VARIABLE *var, *fvar;
//		REGISTER BOOLEAN checkdangle;
//		REGISTER INTBIG i, j, fun, signals, portwidth, nodesize;
//		INTBIG x, y;
//		void *err, *infstr;
//		UINTBIG descript[TEXTDESCRIPTSIZE];
//
//		cell = geomparent(geom);
//		if (geom->entryisnode)
//		{
//			ni = geom->entryaddr.ni;
//
//			// check for bus pins that don't connect to any bus arcs
//			if (ni->proto == sch_buspinprim)
//			{
//				// proceed only if it has no exports on it
//				if (ni->firstportexpinst == NOPORTEXPINST)
//				{
//					// must not connect to any bus arcs
//					for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//						if (pi->conarcinst->proto == sch_busarc) break;
//					if (pi == NOPORTARCINST)
//					{
//						err = logerror(_("Bus pin does not connect to any bus arcs"), cell, 0);
//						addgeomtoerror(err, geom, TRUE, 0, 0);
//						return;
//					}
//				}
//
//				// flag bus pin if more than 1 wire is connected
//				i = 0;
//				for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//					if (pi->conarcinst->proto == sch_wirearc) i++;
//				if (i > 1)
//				{
//					err = logerror(_("Wire arcs cannot connect through a bus pin"), cell, 0);
//					addgeomtoerror(err, geom, TRUE, 0, 0);
//					for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//						if (pi->conarcinst->proto == sch_wirearc)
//							addgeomtoerror(err, pi->conarcinst->geom, TRUE, 0, 0);
//					return;
//				}
//			}
//
//			// check all pins
//			if ((ni->proto->userbits&NFUNCTION) >> NFUNCTIONSH == NPPIN)
//			{
//				// may be stranded if there are no exports or arcs
//				if (ni->firstportexpinst == NOPORTEXPINST && ni->firstportarcinst == NOPORTARCINST)
//				{
//					// see if the pin has displayable variables on it
//					for(i=0; i<ni->numvar; i++)
//					{
//						var = &ni->firstvar[i];
//						if ((var->type&VDISPLAY) != 0) break;
//					}
//					if (i >= ni->numvar)
//					{
//						err = logerror(_("Stranded pin (not connected or exported)"), cell, 0);
//						addgeomtoerror(err, geom, TRUE, 0, 0);
//						return;
//					}
//				}
//
//				if (isinlinepin(ni, &thearcs))
//				{
//					err = logerror(_("Unnecessary pin (between 2 arcs)"), cell, 0);
//					addgeomtoerror(err, geom, TRUE, 0, 0);
//					return;
//				}
//
//				if (invisiblepinwithoffsettext(ni, &x, &y, FALSE))
//				{
//					err = logerror(_("Invisible pin has text in different location"), cell, 0);
//					addgeomtoerror(err, geom, TRUE, 0, 0);
//					addlinetoerror(err, (ni->lowx+ni->highx)/2, (ni->lowy+ni->highy)/2, x, y);
//					return;
//				}
//			}
//
//			// check parameters
//			if (ni->proto->primindex == 0)
//			{
//				np = contentsview(ni->proto);
//				if (np == NONODEPROTO) np = ni->proto;
//
//				// ensure that this node matches the parameter list
//				for(i=0; i<ni->numvar; i++)
//				{
//					var = &ni->firstvar[i];
//					if (TDGETISPARAM(var->textdescript) == 0) continue;
//					fvar = NOVARIABLE;
//					for(j=0; j<np->numvar; j++)
//					{
//						fvar = &np->firstvar[j];
//						if (TDGETISPARAM(fvar->textdescript) == 0) continue;
//						if (namesame(makename(var->key), makename(fvar->key)) == 0) break;
//					}
//					if (j >= np->numvar)
//					{
//						// this node's parameter is no longer on the cell: delete from instance
//						infstr = initinfstr();
//						formatinfstr(infstr, _("Parameter '%s' on node %s is invalid and has been deleted"),
//							truevariablename(var), describenodeinst(ni));
//						err = logerror(returninfstr(infstr), cell, 0);
//						addgeomtoerror(err, geom, TRUE, 0, 0);
//						startobjectchange((INTBIG)ni, VNODEINST);
//						(void)delvalkey((INTBIG)ni, VNODEINST, var->key);
//						endobjectchange((INTBIG)ni, VNODEINST);
//						i--;
//					} else
//					{
//						// this node's parameter is still on the cell: make sure units are OK
//						if (TDGETUNITS(var->textdescript) != TDGETUNITS(fvar->textdescript))
//						{
//							infstr = initinfstr();
//							formatinfstr(infstr, _("Parameter '%s' on node %s had incorrect units (now fixed)"),
//								truevariablename(var), describenodeinst(ni));
//							err = logerror(returninfstr(infstr), cell, 0);
//							addgeomtoerror(err, geom, TRUE, 0, 0);
//							startobjectchange((INTBIG)ni, VNODEINST);
//							TDCOPY(descript, var->textdescript);
//							TDSETUNITS(descript, TDGETUNITS(fvar->textdescript));
//							modifydescript((INTBIG)ni, VNODEINST, var, descript);
//							endobjectchange((INTBIG)ni, VNODEINST);
//						}
//
//						// make sure visibility is OK
//						if (TDGETINTERIOR(fvar->textdescript) != 0)
//						{
//							if ((var->type&VDISPLAY) != 0)
//							{
//								infstr = initinfstr();
//								formatinfstr(infstr, _("Parameter '%s' on node %s should not be visible (now fixed)"),
//									truevariablename(var), describenodeinst(ni));
//								err = logerror(returninfstr(infstr), cell, 0);
//								addgeomtoerror(err, geom, TRUE, 0, 0);
//								startobjectchange((INTBIG)ni, VNODEINST);
//								var->type &= ~VDISPLAY;
//								endobjectchange((INTBIG)ni, VNODEINST);
//							}
//						} else
//						{
//							if ((var->type&VDISPLAY) == 0)
//							{
//								infstr = initinfstr();
//								formatinfstr(infstr, _("Parameter '%s' on node %s should be visible (now fixed)"),
//									truevariablename(var), describenodeinst(ni));
//								err = logerror(returninfstr(infstr), cell, 0);
//								addgeomtoerror(err, geom, TRUE, 0, 0);
//								startobjectchange((INTBIG)ni, VNODEINST);
//								var->type |= VDISPLAY;
//								endobjectchange((INTBIG)ni, VNODEINST);
//							}
//						}
//					}
//				}
//			}
//		} else
//		{
//			ai = geom->entryaddr.ai;
//
//			// check for being floating if it does not have a visible name on it
//			var = getvalkey((INTBIG)ai, VARCINST, -1, el_arc_name_key);
//			if (var == NOVARIABLE || (var->type&VDISPLAY) == 0) checkdangle = TRUE; else
//				checkdangle = FALSE;
//			if (checkdangle)
//			{
//				// do not check for dangle when busses are on named networks
//				if (ai->proto == sch_busarc)
//				{
//					if (ai->network->namecount > 0 && ai->network->tempname == 0) checkdangle = FALSE;
//				}
//			}
//			if (checkdangle)
//			{
//				// check to see if this arc is floating
//				for(i=0; i<2; i++)
//				{
//					ni = ai->end[i].nodeinst;
//
//					// OK if not a pin
//					fun = nodefunction(ni);
//					if (fun != NPPIN) continue;
//
//					// OK if it has exports on it
//					if (ni->firstportexpinst != NOPORTEXPINST) continue;
//
//					// OK if it connects to more than 1 arc
//					if (ni->firstportarcinst == NOPORTARCINST) continue;
//					if (ni->firstportarcinst->nextportarcinst != NOPORTARCINST) continue;
//
//					// the arc dangles
//					err = logerror(_("Arc dangles"), cell, 0);
//					addgeomtoerror(err, geom, TRUE, 0, 0);
//					return;
//				}
//			}
//
//
//			// check to see if its width is sensible
//			signals = ai->network->buswidth;
//			if (signals < 1) signals = 1;
//			for(i=0; i<2; i++)
//			{
//				ni = ai->end[i].nodeinst;
//				if (ni->proto->primindex != 0) continue;
//				np = contentsview(ni->proto);
//				if (np == NONODEPROTO) np = ni->proto;
//				pp = equivalentport(ni->proto, ai->end[i].portarcinst->proto, np);
//				if (pp == NOPORTPROTO)
//				{
//					infstr = initinfstr();
//					formatinfstr(infstr, _("Arc %s connects to port %s of node %s, but there is no equivalent port in cell %s"),
//						describearcinst(ai), ai->end[i].portarcinst->proto->protoname, describenodeinst(ni), describenodeproto(np));
//					err = logerror(returninfstr(infstr), cell, 0);
//					addgeomtoerror(err, geom, TRUE, 0, 0);
//					addgeomtoerror(err, ni->geom, TRUE, 0, 0);
//					continue;
//				}
//				portwidth = pp->network->buswidth;
//				if (portwidth < 1) portwidth = 1;
//				nodesize = ni->arraysize;
//				if (nodesize <= 0) nodesize = 1;
//				if (signals != portwidth && signals != portwidth*nodesize)
//				{
//					infstr = initinfstr();
//					formatinfstr(infstr, _("Arc %s (%ld wide) connects to port %s of node %s (%ld wide)"),
//						describearcinst(ai), signals, pp->protoname, describenodeinst(ni), portwidth);
//					err = logerror(returninfstr(infstr), cell, 0);
//					addgeomtoerror(err, geom, TRUE, 0, 0);
//					addgeomtoerror(err, ni->geom, TRUE, 0, 0);
//				}
//			}
//		}
//
//		// check for overlap
//		dr_schcheckobjectvicinity(geom, geom, el_matid);
//	}

	/*
	 * Routine to check whether object "geom" has a DRC violation with a neighboring object.
	 */
//	void dr_schcheckobjectvicinity(GEOM *topgeom, GEOM *geom, XARRAY trans)
//	{
//		REGISTER INTBIG i, total;
//		REGISTER NODEINST *ni, *subni;
//		REGISTER ARCINST *ai, *subai;
//		XARRAY xformr, xformt, subrot, localtrans;
//		static POLYGON *poly = NOPOLYGON;
//
//		needstaticpolygon(&poly, 4, dr_tool->cluster);
//		if (geom->entryisnode)
//		{
//			ni = geom->entryaddr.ni;
//			makerot(ni, xformr);
//			transmult(xformr, trans, localtrans);
//			if (ni->proto->primindex == 0)
//			{
//				if ((ni->userbits&NEXPAND) != 0)
//				{
//					// expand the instance
//					maketrans(ni, xformt);
//					transmult(xformt, localtrans, subrot);
//					for(subni = ni->proto->firstnodeinst; subni != NONODEINST; subni = subni->nextnodeinst)
//						dr_schcheckobjectvicinity(topgeom, subni->geom, subrot); 
//					for(subai = ni->proto->firstarcinst; subai != NOARCINST; subai = subai->nextarcinst)
//						dr_schcheckobjectvicinity(topgeom, subai->geom, subrot); 
//				}
//			} else
//			{
//				// primitive
//				total = nodepolys(ni, 0, NOWINDOWPART);
//				for(i=0; i<total; i++)
//				{
//					shapenodepoly(ni, i, poly);
//					xformpoly(poly, localtrans);
//					(void)dr_schcheckpolygonvicinity(topgeom, poly);
//				}
//			}
//		} else
//		{
//			ai = geom->entryaddr.ai;
//			total = arcpolys(ai, NOWINDOWPART);
//			for(i=0; i<total; i++)
//			{
//				shapearcpoly(ai, i, poly);
//				xformpoly(poly, trans);
//				(void)dr_schcheckpolygonvicinity(topgeom, poly);
//			}
//		}
//	}

	/*
	 * Routine to check whether polygon "poly" from object "geom" has a DRC violation
	 * with a neighboring object.  Returns TRUE if an error was found.
	 */
//	BOOLEAN dr_schcheckpolygonvicinity(GEOM *geom, POLYGON *poly)
//	{
//		REGISTER NODEINST *ni, *oni;
//		REGISTER ARCINST *ai, *oai;
//		REGISTER NODEPROTO *cell;
//		REGISTER GEOM *ogeom;
//		REGISTER PORTARCINST *pi;
//		REGISTER NETWORK *net;
//		REGISTER BOOLEAN connected;
//		REGISTER INTBIG i, sea;
//
//		// don't check text
//		if (poly->style == TEXTCENT || poly->style == TEXTTOP ||
//			poly->style == TEXTBOT || poly->style == TEXTLEFT ||
//			poly->style == TEXTRIGHT || poly->style == TEXTTOPLEFT ||
//			poly->style == TEXTBOTLEFT || poly->style == TEXTTOPRIGHT ||
//			poly->style == TEXTBOTRIGHT || poly->style == TEXTBOX ||
//			poly->style == GRIDDOTS) return(FALSE);
//
//		cell = geomparent(geom);
//		if (geom->entryisnode) ni = geom->entryaddr.ni; else ai = geom->entryaddr.ai;
//		sea = initsearch(geom->lowx, geom->highx, geom->lowy, geom->highy, cell);
//		for(;;)
//		{
//			ogeom = nextobject(sea);
//			if (ogeom == NOGEOM) break;
//
//			// canonicalize so that errors are found only once
//			if ((INTBIG)geom <= (INTBIG)ogeom) continue;
//
//			// what type of object was found in area
//			if (ogeom->entryisnode)
//			{
//				// found node nearby
//				oni = ogeom->entryaddr.ni;
//				if (geom->entryisnode)
//				{
//					// this is node, nearby is node: see if two nodes touch
//					for(net = cell->firstnetwork; net != NONETWORK; net = net->nextnetwork)
//						net->temp1 = 0;
//					for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//						pi->conarcinst->network->temp1 |= 1;
//					for(pi = oni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//						pi->conarcinst->network->temp1 |= 2;
//					for(net = cell->firstnetwork; net != NONETWORK; net = net->nextnetwork)
//						if (net->temp1 = 3) break;
//					if (net != NONETWORK) continue;
//				} else
//				{			
//					// this is arc, nearby is node: see if electrically connected
//					for(pi = oni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//						if (pi->conarcinst->network == ai->network) break;
//					if (pi != NOPORTARCINST) continue;
//				}
//
//				// no connection: check for touching another
//				if (dr_schcheckpoly(geom, poly, ogeom, ogeom, el_matid, FALSE))
//				{
//					termsearch(sea);
//					return(TRUE);
//				}
//			} else
//			{
//				// found arc nearby
//				oai = ogeom->entryaddr.ai;
//				if (geom->entryisnode)
//				{
//					// this is node, nearby is arc: see if electrically connected
//					for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//						if (pi->conarcinst->network == oai->network) break;
//					if (pi != NOPORTARCINST) continue;
//
//					if (dr_schcheckpoly(geom, poly, ogeom, ogeom, el_matid, FALSE))
//					{
//						termsearch(sea);
//						return(TRUE);
//					}
//				} else
//				{
//					// this is arc, nearby is arc: check for colinearity
//					if (dr_schcheckcolinear(ai, oai))
//					{
//						termsearch(sea);
//						return(TRUE);
//					}
//
//					// if not connected, check to see if they touch
//					connected = FALSE;
//					if (ai->network == oai->network) connected = TRUE; else
//					{
//						if (ai->network->buswidth > 1 && oai->network->buswidth <= 1)
//						{
//							for(i=0; i<ai->network->buswidth; i++)
//								if (ai->network->networklist[i] == oai->network) break;
//							if (i < ai->network->buswidth) connected = TRUE;
//						} else if (oai->network->buswidth > 1 && ai->network->buswidth <= 1)
//						{
//							for(i=0; i<oai->network->buswidth; i++)
//								if (oai->network->networklist[i] == ai->network) break;
//							if (i < oai->network->buswidth) connected = TRUE;
//						}
//					}
//					if (!connected)
//					{
//						if (dr_schcheckpoly(geom, poly, ogeom, ogeom, el_matid, TRUE))
//						{
//							termsearch(sea);
//							return(TRUE);
//						}
//					}
//				}
//			}
//		}
//		return(TRUE);
//	}

	/*
	 * Check polygon "poly" from object "geom" against
	 * geom "ogeom" transformed by "otrans" (and really on top-level object "otopgeom").
	 * Returns TRUE if an error was found.
	 */
//	BOOLEAN dr_schcheckpoly(GEOM *geom, POLYGON *poly, GEOM *otopgeom, GEOM *ogeom, XARRAY otrans,
//		BOOLEAN cancross)
//	{
//		REGISTER INTBIG i, total;
//		REGISTER NODEINST *ni, *subni;
//		REGISTER ARCINST *ai, *subai;
//		XARRAY xformr, xformt, thistrans, subrot;
//		static POLYGON *opoly = NOPOLYGON;
//
//		needstaticpolygon(&opoly, 4, dr_tool->cluster);
//		if (ogeom->entryisnode)
//		{
//			ni = ogeom->entryaddr.ni;
//			makerot(ni, xformr);
//			transmult(xformr, otrans, thistrans);
//			if (ni->proto->primindex == 0)
//			{
//				maketrans(ni, xformt);
//				transmult(xformt, thistrans, subrot);
//				for(subni = ni->proto->firstnodeinst; subni != NONODEINST; subni = subni->nextnodeinst)
//				{
//					if (dr_schcheckpoly(geom, poly, otopgeom, subni->geom, subrot, cancross))
//						return(TRUE);
//				}
//				for(subai = ni->proto->firstarcinst; subai != NOARCINST; subai = subai->nextarcinst)
//				{
//					if (dr_schcheckpoly(geom, poly, otopgeom, subai->geom, subrot, cancross))
//						return(TRUE);
//				}
//			} else
//			{
//				total = nodepolys(ni, 0, NOWINDOWPART);
//				for(i=0; i<total; i++)
//				{
//					shapenodepoly(ni, i, opoly);
//					xformpoly(opoly, thistrans);
//					if (dr_checkpolyagainstpoly(geom, poly, otopgeom, opoly, cancross))
//						return(TRUE);
//				}
//			}
//		} else
//		{
//			ai = ogeom->entryaddr.ai;
//			total = arcpolys(ai, NOWINDOWPART);
//			for(i=0; i<total; i++)
//			{
//				shapearcpoly(ai, i, opoly);
//				xformpoly(opoly, otrans);
//				if (dr_checkpolyagainstpoly(geom, poly, otopgeom, opoly, cancross))
//					return(TRUE);
//			}
//		}
//		return(FALSE);
//	}

	/*
	 * Check polygon "poly" from object "geom" against
	 * polygon "opoly" from object "ogeom".
	 * If "cancross" is TRUE, they can cross each other (but an endpoint cannot touch).
	 * Returns TRUE if an error was found.
	 */
//	BOOLEAN dr_checkpolyagainstpoly(GEOM *geom, POLYGON *poly, GEOM *ogeom, POLYGON *opoly, BOOLEAN cancross)
//	{
//		REGISTER void *err;
//		REGISTER INTBIG i;
//
//		if (cancross)
//		{
//			for(i=0; i<poly->count; i++)
//			{
//				if (polydistance(opoly, poly->xv[i], poly->yv[i]) <= 0) break;
//			}
//			if (i >= poly->count)
//			{
//				// none in "poly" touched one in "opoly", try other way
//				for(i=0; i<opoly->count; i++)
//				{
//					if (polydistance(poly, opoly->xv[i], opoly->yv[i]) <= 0) break;
//				}
//				if (i >= opoly->count) return(FALSE);
//			}
//		} else
//		{
//			if (!polyintersect(poly, opoly)) return(FALSE);
//		}
//
//		// report the error
//		err = logerror(_("Objects touch"), geomparent(geom), 0);
//		addgeomtoerror(err, geom, TRUE, 0, 0);
//		addgeomtoerror(err, ogeom, TRUE, 0, 0);
//		return(TRUE);
//	}

	/*
	 * Routine to check whether arc "ai" is colinear with another.
	 * Returns TRUE if an error was found.
	 */
//	BOOLEAN dr_schcheckcolinear(ARCINST *ai, ARCINST *oai)
//	{
//		REGISTER INTBIG lowx, highx, lowy, highy, olow, ohigh, ang, oang, fx, fy, tx, ty,
//			ofx, ofy, otx, oty, dist, gdist, frx, fry, tox, toy, ca, sa;
//		REGISTER void *err;
//
//		// get information about the other line
//		fx = ai->end[0].xpos;   fy = ai->end[0].ypos;
//		tx = ai->end[1].xpos;   ty = ai->end[1].ypos;
//		ofx = oai->end[0].xpos;   ofy = oai->end[0].ypos;
//		otx = oai->end[1].xpos;   oty = oai->end[1].ypos;
//		if (ofx == otx && ofy == oty) return(FALSE);
//
//		// see if they are colinear
//		lowx = mini(fx, tx);
//		highx = maxi(fx, tx);
//		lowy = mini(fy, ty);
//		highy = maxi(fy, ty);
//		if (fx == tx)
//		{
//			// vertical line
//			olow = mini(ofy, oty);
//			ohigh = maxi(ofy, oty);
//			if (ofx != fx || otx != fx) return(FALSE);
//			if (lowy >= ohigh || highy <= olow) return(FALSE);
//			ang = 900;
//		} else if (fy == ty)
//		{
//			// horizontal line
//			olow = mini(ofx, otx);
//			ohigh = maxi(ofx, otx);
//			if (ofy != fy || oty != fy) return(FALSE);
//			if (lowx >= ohigh || highx <= olow) return(FALSE);
//			ang = 0;
//		} else
//		{
//			// general case
//			ang = figureangle(fx, fy, tx, ty);
//			oang = figureangle(ofx, ofy, otx, oty);
//			if (ang != oang && mini(ang, oang) + 1800 != maxi(ang, oang)) return(FALSE);
//			if (muldiv(ofx-fx, ty-fy, tx-fx) != ofy-fy) return(FALSE);
//			if (muldiv(otx-fx, ty-fy, tx-fx) != oty-fy) return(FALSE);
//			olow = mini(ofy, oty);
//			ohigh = maxi(ofy, oty);
//			if (lowy >= ohigh || highy <= olow) return(FALSE);
//			olow = mini(ofx, otx);
//			ohigh = maxi(ofx, otx);
//			if (lowx >= ohigh || highx <= olow) return(FALSE);
//		}
//		err = logerror(_("Arcs overlap"), ai->parent, 0);
//		addgeomtoerror(err, ai->geom, TRUE, 0, 0);
//		addgeomtoerror(err, oai->geom, TRUE, 0, 0);
//
//		// add information that shows the arcs
//		ang = (ang + 900) % 3600;
//		dist = ai->parent->lib->lambda[ai->parent->tech->techindex] * 2;
//		gdist = dist / 2;
//		ca = cosine(ang);   sa = sine(ang);
//		frx = fx + mult(dist, ca);
//		fry = fy + mult(dist, sa);
//		tox = tx + mult(dist, ca);
//		toy = ty + mult(dist, sa);
//		fx = fx + mult(gdist, ca);
//		fy = fy + mult(gdist, sa);
//		tx = tx + mult(gdist, ca);
//		ty = ty + mult(gdist, sa);
//		addlinetoerror(err, frx, fry, tox, toy);
//		addlinetoerror(err, frx, fry, fx, fy);
//		addlinetoerror(err, tx, ty, tox, toy);
//
//		frx = ofx - mult(dist, ca);
//		fry = ofy - mult(dist, sa);
//		tox = otx - mult(dist, ca);
//		toy = oty - mult(dist, sa);
//		ofx = ofx - mult(gdist, ca);
//		ofy = ofy - mult(gdist, sa);
//		otx = otx - mult(gdist, ca);
//		oty = oty - mult(gdist, sa);
//		addlinetoerror(err, frx, fry, tox, toy);
//		addlinetoerror(err, frx, fry, ofx, ofy);
//		addlinetoerror(err, otx, oty, tox, toy);
//		return(TRUE);
//	}
}
