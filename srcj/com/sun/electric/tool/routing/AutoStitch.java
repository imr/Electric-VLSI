/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AutoStitch.java
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
package com.sun.electric.tool.routing;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.routing.Routing;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * This is the Auto Stitching tool.
 */
public class AutoStitch
{

//	#define MENUDISPX 150			/* x displacement of confirm menu */
//	#define MENUDISPY   0			/* y displacement of confirm menu */
//
//	static ARCPROTO *ro_preferedarc;	/* the prefered arc */
//
//	// working memory for "ro_checkstitching()"
//	static INTBIG     ro_nodesinareatotal = 0;
//	static NODEINST **ro_nodesinarea;

	public static void autoStitch(boolean highlighted)
	{
		List nodesToStitch = new ArrayList();
		if (highlighted)
		{
			List highs = Highlight.getHighlighted(true, false);
			for(Iterator it = highs.iterator(); it.hasNext(); )
				nodesToStitch.add(it.next());
		} else
		{
			Cell cell = Library.needCurCell();
			if (cell == null) return;
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (ni.isIconOfParent()) continue;
				nodesToStitch.add(ni);
			}
		}
		if (nodesToStitch.size() > 0)
		{
			AutoStitchJob job = new AutoStitchJob(nodesToStitch);
		}
	}

	/**
	 * Class to change the node/arc type in a new thread.
	 */
	protected static class AutoStitchJob extends Job
	{
		List nodesToStitch;

		protected AutoStitchJob(List nodesToStitch)
		{
			super("Auto-Stitch", Routing.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.nodesToStitch = nodesToStitch;
			this.startJob();
		}

		public void doIt()
		{
			System.out.println("No auto-stitching yet");

//			// set temp1 flag on all cells that will be checked
//			for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//				for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//					np->temp1 = 0;
//
//			// next pre-compute bounds on all nodes in cells to be changed
//			count = 0;
//			for(r = ro_firstrcheck; r != NORCHECK; r = r->nextcheck)
//				if (r->entity->parent->temp1 == 0)
//			{
//				r->entity->parent->temp1++;
//				for(ni = r->entity->parent->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//				{
//					ni->temp1 = 0;
//
//					// count the ports on this node
//					for(total=0, pp = ni->proto->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//						total++;
//
//					// get memory for bounding box of each port
//					bbarray = emalloc((total * 4 * SIZEOFINTBIG), el_tempcluster);
//					if (bbarray == 0)
//					{
//						ttyputnomemory();
//						ni->temp2 = -1;
//					} else
//					{
//						ni->temp2 = (INTBIG)bbarray;
//						i = 0;
//						for(pp = ni->proto->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//						{
//							makerot(ni, trans);
//							rni = ni;   rpp = pp;
//							while (rni->proto->primindex == 0)
//							{
//								maketrans(rni, localtran);
//								transmult(localtran, trans, temp);
//								rni = rpp->subnodeinst;
//								rpp = rpp->subportproto;
//								makerot(rni, localtran);
//								transmult(localtran, temp, trans);
//							}
//							xform(rni->lowx, rni->lowy, &x1, &y1, trans);
//							xform(rni->highx, rni->highy, &x2, &y2, trans);
//							bbarray[i++] = mini(x1, x2);  bbarray[i++] = maxi(x1, x2);
//							bbarray[i++] = mini(y1, y2);  bbarray[i++] = maxi(y1, y2);
//						}
//					}
//				}
//			}
//
//			// next set ordinals on nodes to be checked
//			order = 1;
//			for(r = ro_firstrcheck; r != NORCHECK; r = r->nextcheck)
//				r->entity->temp1 = order++;
//
//			// find out the prefered routing arc
//			ro_preferedarc = NOARCPROTO;
//			var = getvalkey((INTBIG)ro_tool, VTOOL, VARCPROTO, ro_preferedkey);
//			if (var != NOVARIABLE) ro_preferedarc = (ARCPROTO *)var->addr; else
//			{
//				// see if there is a default user arc
//				if (us_curarcproto != NOARCPROTO) ro_preferedarc = us_curarcproto;
//			}
//
//			// finally, initialize the information about which layer is smallest on each arc
//			for(tech = el_technologies; tech != NOTECHNOLOGY; tech = tech->nexttechnology)
//				for(ap = tech->firstarcproto; ap != NOARCPROTO; ap = ap->nextarcproto)
//					ap->temp1 = -1;
//
//			// now run through the nodeinsts to be checked for stitching
//			for(r = ro_firstrcheck; r != NORCHECK; r = nextr)
//			{
//				nextr = r->nextcheck;
//				if (!stopping(STOPREASONROUTING))
//				{
//					ni = r->entity;
//					if ((ni->parent->userbits&NPLOCKED) != 0) continue;
//					count += ro_checkstitching(ni);
//				}
//				ro_freercheck(r);
//			}
//			ro_firstrcheck = NORCHECK;
//
//			// free any memory associated with this operation
//			for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//				for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//					if (np->temp1 != 0)
//			{
//				for(ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//					if (ni->temp2 != -1 && ni->temp2 != 0)
//						efree((CHAR *)ni->temp2);
//			}
//
//			// if selection was done, restore the highlighting
//			if ((ro_state&SELDONE) != 0)
//				(void)asktool(us_tool, x_("up-stack"));
//
//			// report results
//			if (count != 0)
//				ttyputmsg(_("AUTO ROUTING: added %ld %s"), count, makeplural(_("wire"), count));
		}

//		/*
//		 * routine to check nodeinst "ni" for possible stitching to neighboring
//		 * nodeinsts
//		 */
//		INTBIG ro_checkstitching(NODEINST *ni)
//		{
//			REGISTER INTBIG search, lx, hx, ly, hy, bestdist, dist, ox, oy, stitched, count,
//				tot, i, j, k, bbp, useportpoly, nodesinareacount, newtotal;
//			INTBIG x, y;
//			XARRAY trans, localtran, temp;
//			REGISTER GEOM *geom;
//			REGISTER NODEINST *oni, *rni, **newlist;
//			REGISTER PORTPROTO *pp, *rpp, *bestpp;
//			REGISTER PORTARCINST *pi;
//			REGISTER ARCPROTO *ap;
//			REGISTER POLYGON *polyptr;
//			static POLYGON *poly = NOPOLYGON;
//
//			// get polygon
//			(void)needstaticpolygon(&poly, 4, ro_tool->cluster);
//
//			// gather a list of other nodes that touch or overlap this one
//			lx = ni->geom->lowx;   hx = ni->geom->highx;
//			ly = ni->geom->lowy;   hy = ni->geom->highy;
//			search = initsearch(lx-1, hx+1, ly-1, hy+1, ni->parent);
//			nodesinareacount = 0;
//			for(;;)
//			{
//				if ((geom = nextobject(search)) == NOGEOM) break;
//				if (!geom->entryisnode) continue;
//				if (nodesinareacount >= ro_nodesinareatotal)
//				{
//					newtotal = ro_nodesinareatotal * 2;
//					if (nodesinareacount >= newtotal) newtotal = nodesinareacount + 50;
//					newlist = (NODEINST **)emalloc(newtotal * (sizeof (NODEINST *)), ro_tool->cluster);
//					if (newlist == 0) return(0);
//					for(k=0; k<nodesinareacount; k++) newlist[k] = ro_nodesinarea[k];
//					if (ro_nodesinareatotal > 0) efree((CHAR *)ro_nodesinarea);
//					ro_nodesinareatotal = newtotal;
//					ro_nodesinarea = newlist;
//				}
//				ro_nodesinarea[nodesinareacount++] = geom->entryaddr.ni;
//			}
//
//			count = 0;
//			for(k=0; k<nodesinareacount; k++)
//			{
//				// find another node in this area
//				oni = ro_nodesinarea[k];
//				if (stopping(STOPREASONROUTING)) { termsearch(search);   return(count); }
//
//				// don't check newly created nodes
//		/*		if (oni->temp2 == 0) continue; */
//
//				// if both nodes are being checked, examine them only once
//				if (oni->temp1 != 0 && oni->temp1 <= ni->temp1) continue;
//
//				// now look at every layer in this node
//				if (ni->proto->primindex == 0)
//				{
//					// complex node instance: look at all ports
//					if (ni->temp2 == 0 || ni->temp2 == -1) bbp = -1; else
//						bbp = 0;
//					for(pp = ni->proto->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//					{
//						// first do a bounding box check
//						if (bbp >= 0)
//						{
//							lx = ((INTBIG *)ni->temp2)[bbp++];
//							hx = ((INTBIG *)ni->temp2)[bbp++];
//							ly = ((INTBIG *)ni->temp2)[bbp++];
//							hy = ((INTBIG *)ni->temp2)[bbp++];
//							if (lx > oni->geom->highx || hx < oni->geom->lowx ||
//								ly > oni->geom->highy || hy < oni->geom->lowy) continue;
//						}
//
//						// stop now if already an arc on this port to other node
//						for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//							if (pi->proto == pp && (pi->conarcinst->end[0].nodeinst == oni ||
//									pi->conarcinst->end[1].nodeinst == oni)) break;
//						if (pi != NOPORTARCINST) continue;
//
//						// find the primitive node at the bottom of this port
//						makerot(ni, trans);
//						rni = ni;   rpp = pp;
//						while (rni->proto->primindex == 0)
//						{
//							maketrans(rni, localtran);
//							transmult(localtran, trans, temp);
//							rni = rpp->subnodeinst;
//							rpp = rpp->subportproto;
//							makerot(rni, localtran);
//							transmult(localtran, temp, trans);
//						}
//
//						// determine the smallest layer for all possible arcs
//						for(i=0; pp->connects[i] != NOARCPROTO; i++)
//						{
//							ap = pp->connects[i];
//							ro_findsmallestlayer(ap);
//						}
//
//						// look at all polygons on this nodeinst
//						useportpoly = 0;
//						tot = nodeEpolys(rni, 0, NOWINDOWPART);
//						if (tot == 0 || rni->proto == gen_simprobeprim)
//						{
//							useportpoly = 1;
//							tot = 1;
//						}
//						for(j=0; j<tot; j++)
//						{
//							if (useportpoly != 0)
//							{
//								shapeportpoly(ni, pp, poly, FALSE);
//							} else
//							{
//								shapeEnodepoly(rni, j, poly);
//
//								// only want electrically connected polygons
//								if (poly->portproto == NOPORTPROTO) continue;
//
//								// only want polygons on correct part of this nodeinst
//								if (poly->portproto->network != rpp->network) continue;
//
//								// transformed polygon
//								xformpoly(poly, trans);
//
//								// if the polygon layer is pseudo, substitute real layer
//								poly->layer = nonpseudolayer(poly->layer, poly->tech);
//							}
//
//							// first see if the prefered arc is possible
//							ap = ro_preferedarc;
//							stitched = 0;
//							for(i=0; pp->connects[i] != NOARCPROTO; i++)
//							{
//								if (pp->connects[i] != ap) continue;
//
//								// this polygon must be the smallest arc layer
//								if (useportpoly == 0)
//								{
//									if (!samelayer(ap->tech, ap->temp1, poly->layer)) continue;
//								}
//
//								// pass it on to the next test
//								stitched = ro_testpoly(ni, pp, ap, poly, oni);
//								count += stitched;
//								break;
//							}
//
//							// now look for any arc
//							if (stitched == 0)
//							{
//								for(i=0; pp->connects[i] != NOARCPROTO; i++)
//								{
//									ap = pp->connects[i];
//									if (ap == ro_preferedarc) continue;
//
//									// arc must be in the same technology
//									if (ap->tech != rni->proto->tech) continue;
//
//									// this polygon must be the smallest arc layer
//									if (useportpoly == 0)
//									{
//										if (!samelayer(ap->tech, ap->temp1, poly->layer)) continue;
//									}
//
//									// pass it on to the next test
//									stitched = ro_testpoly(ni, pp, ap, poly, oni);
//									count += stitched;
//									if (stitched != 0) break;
//								}
//							}
//							if (stitched != 0) break;
//						}
//					}
//				} else
//				{
//					// primitive node: check its layers
//					makerot(ni, trans);
//
//					// save information about the other node
//					ox = (oni->lowx + oni->highx) / 2;
//					oy = (oni->lowy + oni->highy) / 2;
//
//					// look at all polygons on this nodeinst
//					useportpoly = 0;
//					tot = allnodeEpolys(ni, ro_autostitchplist, NOWINDOWPART, TRUE);
//					if (tot == 0 || ni->proto == gen_simprobeprim)
//					{
//						useportpoly = 1;
//						tot = 1;
//					}
//					for(j=0; j<tot; j++)
//					{
//						if (useportpoly != 0)
//						{
//							// search all ports for the closest
//							bestpp = NOPORTPROTO;
//							bestdist = 0;
//							for(rpp = ni->proto->firstportproto; rpp != NOPORTPROTO; rpp = rpp->nextportproto)
//							{
//								// compute best distance to the other node
//								portposition(ni, rpp, &x, &y);
//								dist = abs(x-ox) + abs(y-oy);
//								if (bestpp == NOPORTPROTO) bestdist = dist;
//								if (dist > bestdist) continue;
//								bestpp = rpp;   bestdist = dist;
//							}
//							if (bestpp == NOPORTPROTO) continue;
//							rpp = bestpp;
//							shapeportpoly(ni, rpp, poly, FALSE);
//							polyptr = poly;
//						} else
//						{
//							polyptr = ro_autostitchplist->polygons[j];
//
//							// only want electrically connected polygons
//							if (polyptr->portproto == NOPORTPROTO) continue;
//
//							// if the polygon layer is pseudo, substitute real layer
//							polyptr->layer = nonpseudolayer(polyptr->layer, polyptr->tech);
//
//							// get the correct port connected to this polygon
//							rpp = NOPORTPROTO;
//
//							// search all ports for the closest connected to this layer
//							bestpp = NOPORTPROTO;
//							bestdist = 0;
//							for(rpp = ni->proto->firstportproto; rpp != NOPORTPROTO; rpp = rpp->nextportproto)
//								if (rpp->network == polyptr->portproto->network)
//							{
//								// compute best distance to the other node
//								portposition(ni, rpp, &x, &y);
//								dist = abs(x-ox) + abs(y-oy);
//								if (bestpp == NOPORTPROTO) bestdist = dist;
//								if (dist > bestdist) continue;
//								bestpp = rpp;   bestdist = dist;
//							}
//							if (bestpp == NOPORTPROTO) continue;
//							rpp = bestpp;
//
//							// transformed the polygon
//							xformpoly(polyptr, trans);
//						}
//
//						// stop now if already an arc on this port to other node
//						for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//							if (pi->proto->network == rpp->network &&
//									(pi->conarcinst->end[0].nodeinst == oni ||
//										pi->conarcinst->end[1].nodeinst == oni)) break;
//						if (pi != NOPORTARCINST) continue;
//
//						// first see if the prefered arc is possible
//						ap = ro_preferedarc;
//						stitched = 0;
//						for(i=0; rpp->connects[i] != NOARCPROTO; i++)
//						{
//							if (rpp->connects[i] != ap) continue;
//
//							// arc must be in the same technology
//							if (ap->tech != ni->proto->tech) break;
//
//							// this polygon must be the smallest arc layer
//							ro_findsmallestlayer(ap);
//							if (useportpoly == 0)
//							{
//								if (!samelayer(ap->tech, ap->temp1, polyptr->layer)) continue;
//							}
//
//							// pass it on to the next test
//							stitched = ro_testpoly(ni, rpp, ap, polyptr, oni);
//							count += stitched;
//							break;
//						}
//
//						// now look for any arc
//						if (stitched == 0)
//						{
//							for(i=0; rpp->connects[i] != NOARCPROTO; i++)
//							{
//								ap = rpp->connects[i];
//								if (ap == ro_preferedarc) continue;
//
//								// arc must be in the same technology
//								if (ap->tech != ni->proto->tech) continue;
//
//								// this polygon must be the smallest arc layer
//								ro_findsmallestlayer(ap);
//								if (useportpoly == 0)
//								{
//									if (!samelayer(ap->tech, ap->temp1, polyptr->layer)) continue;
//								}
//
//								// pass it on to the next test
//								stitched = ro_testpoly(ni, rpp, ap, polyptr, oni);
//								count += stitched;
//								if (stitched != 0) break;
//							}
//						}
//						if (stitched != 0) break;
//					}
//				}
//			}
//			return(count);
//		}

		/*
		 * routine to find exported polygons in node "oni" that abut with the polygon
		 * in "poly" on the same layer.  When they do, these should be connected to
		 * nodeinst "ni", port "pp" with an arc of type "ap".  Returns the number of
		 * connections made (0 if none).
		 */
//		INTBIG ro_testpoly(NODEINST *ni, PORTPROTO *pp, ARCPROTO *ap, POLYGON *poly, NODEINST *oni)
//		{
//			REGISTER NODEINST *rni;
//			REGISTER PORTPROTO *rpp, *mpp, *bestpp;
//			XARRAY localtran, temp, trans;
//			INTBIG x, y, plx, phx, ply, phy, ox, oy;
//			static POLYGON *opoly = NOPOLYGON;
//			REGISTER INTBIG tot, j, bbp, dist, bestdist, lx, hx, ly, hy;
//			REGISTER NETWORK *net, *onet;
//
//			// get polygon
//			(void)needstaticpolygon(&opoly, 4, ro_tool->cluster);
//
//			// get network associated with the node/port
//			net = getnetonport(ni, pp);
//
//			// now look at every layer in this node
//			if (oni->proto->primindex == 0)
//			{
//				// complex cell: look at all exports
//				if (oni->temp2 == 0 || oni->temp2 == -1) bbp = -1; else
//				{
//					getbbox(poly, &plx, &phx, &ply, &phy);
//					bbp = 0;
//				}
//				for(mpp = oni->proto->firstportproto; mpp != NOPORTPROTO; mpp = mpp->nextportproto)
//				{
//					// first do a bounding box check
//					if (bbp >= 0)
//					{
//						lx = ((INTBIG *)oni->temp2)[bbp++];
//						hx = ((INTBIG *)oni->temp2)[bbp++];
//						ly = ((INTBIG *)oni->temp2)[bbp++];
//						hy = ((INTBIG *)oni->temp2)[bbp++];
//						if (lx > phx || hx < plx || ly > phy || hy < ply) continue;
//					}
//
//					// port must be able to connect to the arc
//					if (!ro_canconnect(ap, mpp)) continue;
//
//					// do not stitch where there is already an electrical connection
//					onet = getnetonport(oni, mpp);
//					if (net != NONETWORK && onet == net) continue;
//
//					// find the primitive node at the bottom of this port
//					makerot(oni, trans);
//					rni = oni;   rpp = mpp;
//					while (rni->proto->primindex == 0)
//					{
//						maketrans(rni, localtran);
//						transmult(localtran, trans, temp);
//						rni = rpp->subnodeinst;
//						rpp = rpp->subportproto;
//						makerot(rni, localtran);
//						transmult(localtran, temp, trans);
//					}
//
//					// see how much geometry is on this node
//					tot = nodeEpolys(rni, 0, NOWINDOWPART);
//					if (tot == 0)
//					{
//						// not a geometric primitive: look for ports that touch
//						shapeportpoly(oni, mpp, opoly, FALSE);
//						if (ro_comparepoly(oni, mpp, opoly, ni, pp, poly, ap) != 0)
//							return(1);
//					} else
//					{
//						// a geometric primitive: look for ports on layers that touch
//						for(j=0; j<tot; j++)
//						{
//							shapeEnodepoly(rni, j, opoly);
//
//							// only want electrically connected polygons
//							if (opoly->portproto == NOPORTPROTO) continue;
//
//							// only want polygons connected to correct part of nodeinst
//							if (opoly->portproto->network != rpp->network) continue;
//
//							// if the polygon layer is pseudo, substitute real layer
//							if (ni->proto != gen_simprobeprim)
//							{
//								opoly->layer = nonpseudolayer(opoly->layer, opoly->tech);
//								if (!samelayer(ap->tech, ap->temp1, opoly->layer)) continue;
//							}
//
//							// transform the polygon and pass it on to the next test
//							xformpoly(opoly, trans);
//							if (ro_comparepoly(oni, mpp, opoly, ni, pp, poly, ap) != 0)
//								return(1);
//						}
//					}
//				}
//			} else
//			{
//				// primitive node: check its layers
//				makerot(oni, trans);
//
//				// determine target point
//				getcenter(poly, &ox, &oy);
//
//				// look at all polygons on this nodeinst
//				tot = nodeEpolys(oni, 0, NOWINDOWPART);
//				if (tot == 0)
//				{
//					// not a geometric primitive: look for ports that touch
//					bestpp = NOPORTPROTO;
//					bestdist = 0;
//					for(rpp = oni->proto->firstportproto; rpp != NOPORTPROTO; rpp = rpp->nextportproto)
//					{
//						// compute best distance to the other node
//						portposition(oni, rpp, &x, &y);
//						dist = abs(x-ox) + abs(y-oy);
//						if (bestpp == NOPORTPROTO) bestdist = dist;
//						if (dist > bestdist) continue;
//						bestpp = rpp;   bestdist = dist;
//					}
//					if (bestpp != NOPORTPROTO)
//					{
//						rpp = bestpp;
//
//						// port must be able to connect to the arc
//						if (ro_canconnect(ap, rpp))
//						{
//							// transformed the polygon and pass it on to the next test
//							shapeportpoly(oni, rpp, opoly, FALSE);
//							if (ro_comparepoly(oni, rpp, opoly, ni, pp, poly, ap) != 0)
//								return(1);
//						}
//					}
//				} else
//				{
//					// a geometric primitive: look for ports on layers that touch
//					for(j=0; j<tot; j++)
//					{
//						shapeEnodepoly(oni, j, opoly);
//
//						// only want electrically connected polygons
//						if (opoly->portproto == NOPORTPROTO) continue;
//
//						// if the polygon layer is pseudo, substitute real layer
//						opoly->layer = nonpseudolayer(opoly->layer, opoly->tech);
//
//						// this must be the smallest layer on the arc
//						if (!samelayer(ap->tech, ap->temp1, opoly->layer)) continue;
//
//						// do not stitch where there is already an electrical connection
//						onet = getnetonport(oni, opoly->portproto);
//						if (net != NONETWORK && onet == net) continue;
//
//						// search all ports for the closest connected to this layer
//						bestpp = NOPORTPROTO;
//						bestdist = 0;
//						for(rpp = oni->proto->firstportproto; rpp != NOPORTPROTO; rpp = rpp->nextportproto)
//							if (rpp->network == opoly->portproto->network)
//						{
//							// compute best distance to the other node
//							portposition(oni, rpp, &x, &y);
//							dist = abs(x-ox) + abs(y-oy);
//							if (bestpp == NOPORTPROTO) bestdist = dist;
//							if (dist > bestdist) continue;
//							bestpp = rpp;   bestdist = dist;
//						}
//						if (bestpp == NOPORTPROTO) continue;
//						rpp = bestpp;
//
//						// port must be able to connect to the arc
//						if (!ro_canconnect(ap, rpp)) continue;
//
//						// transformed the polygon and pass it on to the next test
//						xformpoly(opoly, trans);
//						if (ro_comparepoly(oni, rpp, opoly, ni, pp, poly, ap) != 0)
//							return(1);
//					}
//				}
//			}
//			return(0);
//		}

		/*
		 * routine to compare polygon "opoly" from nodeinst "oni", port "opp" and
		 * polygon "poly" from nodeinst "ni", port "pp".  If these polygons touch
		 * or overlap then the two nodes should be connected with an arc of type
		 * "ap".  If a connection is made, the routine returns nonzero, otherwise
		 * it returns zero.
		 */
//		INTBIG ro_comparepoly(NODEINST *oni, PORTPROTO *opp, POLYGON *opoly, NODEINST *ni,
//			PORTPROTO *pp, POLYGON *poly, ARCPROTO *ap)
//		{
//			INTBIG x, y, ox, oy, lx, hx, ly, hy, olx, oly, ohx, ohy, ret, tx, ty, dist, tdist;
//			ARCINST *alt1, *alt2;
//			REGISTER PORTPROTO *tpp;
//			NODEINST *con1, *con2;
//			REGISTER ARCINST *newai;
//
//			// find the bounding boxes of the polygons
//			getbbox(poly, &lx, &hx, &ly, &hy);
//			getbbox(opoly, &olx, &ohx, &oly, &ohy);
//
//			// quit now if bounding boxes don't intersect
//			if (lx > ohx || olx > hx || ly > ohy || oly > hy) return(0);
//
//			// be sure the closest ports are being used
//			portposition(ni, pp, &x, &y);
//			portposition(oni, opp, &ox, &oy);
//			dist = computedistance(x, y, ox, oy);
//			for(tpp = oni->proto->firstportproto; tpp != NOPORTPROTO; tpp = tpp->nextportproto)
//			{
//				if (tpp == opp) continue;
//				if (tpp->network != opp->network) continue;
//				portposition(oni, tpp, &tx, &ty);
//				tdist = computedistance(x, y, tx, ty);
//				if (tdist >= dist) continue;
//				dist = tdist;
//				opp = tpp;
//				ox = tx;   oy = ty;
//			}
//			for(tpp = ni->proto->firstportproto; tpp != NOPORTPROTO; tpp = tpp->nextportproto)
//			{
//				if (tpp == pp) continue;
//				if (tpp->network != pp->network) continue;
//				portposition(ni, tpp, &tx, &ty);
//				tdist = computedistance(ox, oy, tx, ty);
//				if (tdist >= dist) continue;
//				dist = tdist;
//				pp = tpp;
//				x = tx;   y = ty;
//			}
//
//			// find some dummy position to help run the arc
//			x = (ox+x) / 2;   y = (oy+y) / 2;
//
//			// run the wire
//			newai = aconnect(ni->geom, pp, oni->geom, opp, ap, x, y, &alt1, &alt2, &con1, &con2, 900, TRUE, TRUE);
//			ret = ro_didaccept(newai, alt1, alt2, con1, con2);
//			return(ret);
//		}

		/*
		 * routine to examine up to three arcs that were created and see if they
		 * are acceptable to the user.  Returns zero if not, nonzero if so.
		 */
//		INTBIG ro_didaccept(ARCINST *ai, ARCINST *alt1, ARCINST *alt2, NODEINST *con1, NODEINST *con2)
//		{
//			REGISTER VARIABLE *var;
//			POPUPMENU *pm;
//			REGISTER POPUPMENUITEM *miret;
//			BOOLEAN butstate;
//			WINDOWPART *w;
//			INTBIG x, y, retval;
//
//			// if main arc wasn't created, no update
//			if (ai == NOARCINST) return(0);
//
//			// see if user selection is to be done
//			retval = 1;
//			if ((ro_state&(SELECT|SELSKIP)) == SELECT)
//			{
//				// save highlighting on the first selection
//				if ((ro_state&SELDONE) == 0)
//				{
//					(void)asktool(us_tool, x_("down-stack"));
//					(void)setvalkey((INTBIG)ro_tool, VTOOL, ro_statekey, ro_state | SELDONE,
//						VINTEGER|VDONTSAVE);
//				}
//
//				// erase all highlighting
//				(void)asktool(us_tool, x_("clear"));
//
//				// force the stitch to be drawn
//				endobjectchange((INTBIG)ai, VARCINST);
//				if (alt1 != NOARCINST) endobjectchange((INTBIG)alt1, VARCINST);
//				if (alt2 != NOARCINST) endobjectchange((INTBIG)alt2, VARCINST);
//				if (con1 != NONODEINST) endobjectchange((INTBIG)con1, VNODEINST);
//				if (con2 != NONODEINST) endobjectchange((INTBIG)con2, VNODEINST);
//
//				// highlight the stitch
//				(void)asktool(us_tool, x_("show-object"), (INTBIG)ai->geom);
//				if (alt1 != NOARCINST) (void)asktool(us_tool, x_("show-object"), (INTBIG)alt1->geom);
//				if (alt2 != NOARCINST) (void)asktool(us_tool, x_("show-object"), (INTBIG)alt2->geom);
//				if (con1 != NONODEINST) (void)asktool(us_tool, x_("show-object"), (INTBIG)con1->geom);
//				if (con2 != NONODEINST) (void)asktool(us_tool, x_("show-object"), (INTBIG)con2->geom);
//				(void)asktool(us_tool, x_("flush-changes"));
//
//				// prepare confirm menu
//				pm = (POPUPMENU *)emalloc(sizeof(POPUPMENU), ro_tool->cluster);
//				if (pm == 0) return(0);
//				pm->name = x_("x");
//				pm->header = _("Confirm stitch");
//				pm->total = 3;
//				pm->list = (POPUPMENUITEM *)emalloc(pm->total * sizeof(POPUPMENUITEM), ro_tool->cluster);
//				if (pm->list == 0) return(0);
//				pm->list[0].attribute = _("&Accept");
//				pm->list[0].value = 0;
//				pm->list[0].response = NOUSERCOM;
//				pm->list[1].attribute = _("&Reject");
//				pm->list[1].value = 0;
//				pm->list[1].response = NOUSERCOM;
//				pm->list[2].attribute = _("Continue &silently");
//				pm->list[2].value = 0;
//				pm->list[2].response = NOUSERCOM;
//
//				// show confirm menu near first arc
//				butstate = TRUE;
//				if (el_curwindowpart != NOWINDOWPART)
//				{
//					w = el_curwindowpart;
//					x = (ai->end[0].xpos + ai->end[1].xpos) / 2;
//					y = (ai->end[0].ypos + ai->end[1].ypos) / 2;
//					x = applyxscale(w, x-w->screenlx) + w->uselx + MENUDISPX;
//					y = applyyscale(w, y-w->screenly) + w->usely + MENUDISPY;
//				} else x = y = -1;
//				miret = us_popupmenu(&pm, &butstate, TRUE, x, y, 4);
//				(void)asktool(us_tool, x_("clear"));
//				if (miret != NOPOPUPMENUITEM)
//				{
//					switch (miret - pm->list)
//					{
//						case 0: // accept
//							break;	
//						case 1: // reject
//							startobjectchange((INTBIG)ai, VARCINST);
//							if (killarcinst(ai)) ttyputerr(_("Problem retracting arc"));
//							if (alt1 != NOARCINST)
//							{
//								startobjectchange((INTBIG)alt1, VARCINST);
//								if (killarcinst(alt1))
//									ttyputerr(_("Problem retracting arc"));
//							}
//							if (alt2 != NOARCINST)
//							{
//								startobjectchange((INTBIG)alt2, VARCINST);
//								if (killarcinst(alt2))
//									ttyputerr(_("Problem retracting arc"));
//							}
//							if (con1 != NONODEINST)
//							{
//								startobjectchange((INTBIG)con1, VNODEINST);
//								if (killnodeinst(con1))
//									ttyputerr(_("Problem retracting node"));
//							}
//							if (con2 != NONODEINST)
//							{
//								startobjectchange((INTBIG)con2, VNODEINST);
//								if (killnodeinst(con2))
//									ttyputerr(_("Problem retracting node"));
//							}
//							ttyputmsg(_("Stitch not made"));
//							retval = 0;
//							break;
//						case 2: // continue silently
//							var = getvalkey((INTBIG)ro_tool, VTOOL, VINTEGER, ro_statekey);
//							if (var != NOVARIABLE)
//								(void)setvalkey((INTBIG)ro_tool, VTOOL, ro_statekey,
//									ro_state | SELSKIP, VINTEGER|VDONTSAVE);
//							break;
//					}
//				}
//				efree((CHAR *)pm->list);
//				efree((CHAR *)pm);
//			}
//			return(retval);
//		}

		/*
		 * routine to determine whether arcproto "ap" can connect to portproto
		 * "pp".  Returns true if it can connect.
		 */
//		BOOLEAN ro_canconnect(ARCPROTO *ap, PORTPROTO *pp)
//		{
//			REGISTER INTBIG i;
//
//			for(i=0; pp->connects[i] != NOARCPROTO; i++)
//				if (pp->connects[i] == ap) return(TRUE);
//			return(FALSE);
//		}

		/*
		 * routine to find the smallest layer on arc proto "ap" and cache that information
		 * in the "temp1" field of the arc proto.
		 */
//		void ro_findsmallestlayer(ARCPROTO *ap)
//		{
//			REGISTER ARCINST *ai;
//			ARCINST arc;
//			REGISTER float area, bestarea;
//			REGISTER INTBIG i, j, bestfound;
//			static POLYGON *poly = NOPOLYGON;
//
//			// quit if the value has already been computed
//			if (ap->temp1 >= 0) return;
//
//			// get polygon
//			(void)needstaticpolygon(&poly, 4, ro_tool->cluster);
//
//			// get a dummy arc to analyze
//			ai = &arc;   initdummyarc(ai);
//			ai->proto = ap;
//			ai->width = defaultarcwidth(ap);
//			ai->end[0].xpos = -5000;   ai->end[0].ypos = 0;
//			ai->end[1].xpos = 5000;    ai->end[1].ypos = 0;
//			ai->length = 10000;
//
//			// find the smallest layer
//			bestfound = 0;
//			bestarea = 0;
//			j = arcpolys(ai, NOWINDOWPART);
//			for(i=0; i<j; i++)
//			{
//				shapearcpoly(ai, i, poly);
//				area = (float)fabs(areapoly(poly));
//
//				// LINTED "bestarea" used in proper order
//				if (bestfound != 0 && area >= bestarea) continue;
//				bestarea = area;
//				bestfound++;
//				ap->temp1 = poly->layer;
//			}
//		}

	}

}
