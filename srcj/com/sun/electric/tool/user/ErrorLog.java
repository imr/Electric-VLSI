/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ErrorLog.java
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

package com.sun.electric.tool.user;

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

public class ErrorLog
{
	/*
	 * These are the methods to log errors:
	 *   initLogging(String s)                  initialize for subsystem "s"
	 *   e = logError(String msg, Cell c, k)    log message "msg" in cell "c", sort key "k"
	 *   addGeom(Geometric g, boolean s, int l, NodeInst [] p)
	 *                                          add geom "g" to error (show if "s" nonzero)
	 *   addExport(Export p, boolean s)         add export "pp" to error
	 *   addLine(x1, y1, x2, y2)                add line to error
	 *   addPoly(POLYGON *p)                    add polygon to error
	 *   addPoint(x, y)                         add point to error
	 *
	 * To report errors, call:
	 *   termLogging(explain)                   complete error accumulation
	 *   sortErrors()                           sort errors by key
	 *   n = numErrors()                        returns number of errors
	 *   s = reportnexterror(show, &g1, &g2)    report next error
	 *   s = reportpreverror()                  report previous error
	 *
	 * To obtain errors internally, call:
	 *   e = getnexterror(e, msg)               gets error after "e" (0 is first error)
	 *   msg = describe()                       return description of error "e"
	 *   reportError(e)                         reports error "e"
	 *   t = getNumGeoms()                      get number of geoms on error
	 *   eg = getErrorGeom(i)                   get geom "i" on error "e"
	 *   showerrorgeom(eg)                      highlight error geom "eg"
	 */

	private static final int ERRORTYPEGEOM      = 1;
	private static final int ERRORTYPEEXPORT    = 2;
	private static final int ERRORTYPELINE      = 3;
	private static final int ERRORTYPEPOINT     = 4;

	static class ErrorHighlight
	{
		int         type;
		Geometric   geom;
		PortProto   pp;
		boolean     showgeom;
		double      x1, y1;
		double      x2, y2;
		int         pathlen;
		NodeInst [] path;

		/**
		 * Method to describe an object on an error.
		 */
		public String describe()
		{
			String msg;
			if (geom instanceof NodeInst) msg = "Node " + geom.describe(); else
				msg = "Arc " + geom.describe();
			for(int i=0; i<pathlen; i++)
			{
				NodeInst ni = path[i];
				msg += " in " + ni.getParent().getLibrary().getLibName() +
					":" + ni.getParent().noLibDescribe() + ":" + ni.describe();
			}
			return msg;
		}
	};


	private String message;
	private Cell   cell;
	private int    sortKey;
	private int    index;
	private List   highlights;

	private static int trueNumErrors;
	private static int errorLimit;
	private static List allErrors = new ArrayList();
	private static int errorPosition;
	private static boolean limitExceeded;
	private static String errorSystem;

	/**
	 * Method to free all previously stored errors and initialize the system.
	 * The errors are described by "system" and up to two cells "cell1" and
	 * "cell2" (may be NONODEPROTO).
	 */
	public static void initLogging(String system)
	{
		allErrors.clear();
		trueNumErrors = 0;
		limitExceeded = false;
		errorPosition = 0;
		errorSystem = system;
		errorLimit = User.getErrorLimit();
	}

	/**
	 * Method to create an error message with the text "message" applying to cell "cell".
	 * Returns a pointer to the message (0 on error) which can be used to add highlights.
	 */
	public static ErrorLog logError(String message, Cell cell, int sortKey)
	{
		trueNumErrors++;

		// if too many errors, don't save it
		if (errorLimit > 0 && numErrors() >= errorLimit)
		{
			if (!limitExceeded)
			{
				System.out.println("WARNING: more than " + errorLimit + " errors found, ignoring the rest");
				limitExceeded = true;
			}
			return null;
		}

		// create a new ErrorLog object
		ErrorLog el = new ErrorLog();

		// store information about the error
		el.message = message;
		el.cell = cell;
		el.sortKey = sortKey;
		el.highlights = new ArrayList();

		// add the ErrorLog into the global list
		allErrors.add(el);

		return el;
	}

	/**
	 * Method to add "geom" to the error in "errorlist".  Also adds a
	 * hierarchical traversal path "path" (which is "pathlen" long).
	 */
	public void addGeom(Geometric geom, boolean showit, int pathlen, NodeInst [] path)
	{
		ErrorHighlight eh = new ErrorHighlight();
		eh.type = ERRORTYPEGEOM;
		eh.geom = geom;
		eh.showgeom = showit;
		eh.pathlen = pathlen;
		if (pathlen > 0) eh.path = path;
		highlights.add(eh);
	}

	/**
	 * Method to add "pp" to the error in "errorlist".
	 */
	public void addExport(Export pp, boolean showit)
	{
		ErrorHighlight eh = new ErrorHighlight();
		eh.type = ERRORTYPEEXPORT;
		eh.pp = pp;
		eh.showgeom = showit;
		eh.pathlen = 0;
		highlights.add(eh);
	}

	/**
	 * Method to add line (x1,y1)=>(x2,y2) to the error in "errorlist".
	 */
	public void addLine(double x1, double y1, double x2, double y2)
	{
		ErrorHighlight eh = new ErrorHighlight();
		eh.type = ERRORTYPELINE;
		eh.x1 = x1;
		eh.y1 = y1;
		eh.x2 = x2;
		eh.y2 = y2;
		eh.pathlen = 0;
		highlights.add(eh);
	}

	/**
	 * Method to add polygon "poly" to the error in "errorlist".
	 */
	public void addPoly(Poly poly)
	{
//		if (isbox(poly, &lx, &hx, &ly, &hy))
//		{
//			(void)addlinetoerror(errorlist, lx, ly, lx, hy);
//			(void)addlinetoerror(errorlist, lx, hy, hx, hy);
//			(void)addlinetoerror(errorlist, hx, hy, hx, ly) ;
//			(void)addlinetoerror(errorlist, hx, ly, lx, ly);;
//		} else
//		{
//			for(i=0; i<poly->count; i++)
//			{
//				if (i == 0) prev = poly->count-1; else prev = i-1;
//				(void)addlinetoerror(errorlist, poly->xv[prev], poly->yv[prev],
//					poly->xv[i], poly->yv[i]);
//			}
//		}
	}

	/**
	 * Method to add point (x,y) to the error in "errorlist".
	 */
	public void addPoint(double x, double y)
	{
		ErrorHighlight eh = new ErrorHighlight();
		eh.type = ERRORTYPEPOINT;
		eh.x1 = x;
		eh.y1 = y;
		eh.pathlen = 0;
		highlights.add(eh);
	}

	/**
	 * Method called when all errors are logged.  Initializes pointers for replay of errors.
	 */
	public static void termLogging(boolean explain)
	{
		// enumerate the errors
		int errs = 0;
		for(Iterator it = allErrors.iterator(); it.hasNext(); )
		{
			ErrorLog el = (ErrorLog)it.next();
			el.index = ++errs;
		}

//		if (db_errorchangedroutine != 0) (*db_errorchangedroutine)();

		if (errs > 0 && explain)
		{
			System.out.println("To review errors, type:");
			System.out.println(" >  Show the next error");
			System.out.println(" <  Show the previous error");
		}
	}

	/**
	 * Method to sort the errors by their "key" (a value provided to "logerror()").
	 * Obviously, this should be called after all errors have been reported.
	 */
	public static void sortErrors()
	{
		Collections.sort(allErrors, new ErrorLogOrder());
	}

	static class ErrorLogOrder implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			ErrorLog el1 = (ErrorLog)o1;
			ErrorLog el2 = (ErrorLog)o2;
			return el1.sortKey - el2.sortKey;
		}
	}

	/**
	 * Method to return the number of logged errors.
	 */
	public static int numErrors()
	{
		return trueNumErrors;
	}

	/**
	 * Method to advance to the next error and report it.
	 */
//	CHAR *reportnexterror(INTBIG showhigh, GEOM **g1, GEOM **g2)
//	{
//		if (db_nexterrorlist != NOERRORLIST)
//		{
//			db_curerrorlist = db_nexterrorlist;
//			db_nexterrorlist = db_curerrorlist->nexterrorlist;
//			db_preverrorlist = db_curerrorlist->preverrorlist;
//		} else
//		{
//			// at end: go to start of list
//			db_curerrorlist = db_firsterrorlist;
//			if (db_curerrorlist == NOERRORLIST) db_nexterrorlist = NOERRORLIST; else
//				db_nexterrorlist = db_curerrorlist->nexterrorlist;
//			db_preverrorlist = NOERRORLIST;
//		}
//		return(db_reportcurrenterror(showhigh, g1, g2));
//	}

	/**
	 * Method to back up to the previous error and report it.
	 */
//	CHAR *reportpreverror(void)
//	{
//		REGISTER ERRORLIST *el;
//
//		if (db_preverrorlist != NOERRORLIST)
//		{
//			db_curerrorlist = db_preverrorlist;
//			db_nexterrorlist = db_curerrorlist->nexterrorlist;
//			db_preverrorlist = db_curerrorlist->preverrorlist;
//		} else
//		{
//			// at start: go to end of list
//			db_preverrorlist = db_curerrorlist = db_nexterrorlist = NOERRORLIST;
//			for(el = db_firsterrorlist; el != NOERRORLIST; el = el->nexterrorlist)
//			{
//				db_preverrorlist = db_curerrorlist;
//				db_curerrorlist = el;
//			}
//		}
//		return(db_reportcurrenterror(1, 0, 0));
//	}

	/**
	 * Method to return a list of errors.  On the first call, set "e" to zero.
	 * Returns the next in the list (zero when done).
	 */
//	void *getnexterror(void *elv)
//	{
//		REGISTER ERRORLIST *el;
//
//		if (elv == 0) el = db_firsterrorlist; else
//		{
//			el = (ERRORLIST *)elv;
//			if (el == NOERRORLIST) return(0);
//			el = el->nexterrorlist;
//		}
//		if (el == NOERRORLIST) return(0);
//		return((void *)el);
//	}

	/**
	 * Method to return the number of objects associated with error "e".  Only
	 * returns "geom" objects
	 */
	public int getNumGeoms()
	{
		int total = 0;
		for(Iterator it = highlights.iterator(); it.hasNext(); )
		{
			ErrorHighlight eh = (ErrorHighlight)it.next();
			if (eh.type == ERRORTYPEGEOM) total++;
		}
		return total;
	}

	/**
	 * Method to return a specific object in a list of objects on this ErrorLog.
	 * Returns null at the end of the list.
	 */
	public ErrorHighlight getErrorGeom(int index)
	{
		int total = 0;
		for(Iterator it = highlights.iterator(); it.hasNext(); )
		{
			ErrorHighlight eh = (ErrorHighlight)it.next();
			if (eh.type != ERRORTYPEGEOM) continue;
			if (total == index) return eh;
			total++;
		}
		return null;
	}

	/**
	 * Method to highlight an object on an error.
	 */
	public void show()
	{
//		if (ehv == 0) return;
//		eh = (ERRORHIGHLIGHT *)ehv;
//		geom = eh->geom;
//		np = geomparent(geom);
//		for(w = el_topwindowpart; w != NOWINDOWPART; w = w->nextwindowpart)
//			if (w->curnodeproto == np) break;
//		if (w == NOWINDOWPART)
//		{
//			el_curwindowpart = us_wantnewwindow(0);
//			us_fullview(np, &lx, &hx, &ly, &hy);
//			us_switchtocell(np, lx, hx, ly, hy, NONODEINST, NOPORTPROTO, FALSE, FALSE, FALSE);
//		}
//		(void)asktool(us_tool, x_("show-object"), (INTBIG)geom);
//
//		for(i=eh->pathlen-1; i>=0; i--)
//		{
//			sethierarchicalparent(np, eh->path[i], NOWINDOWPART, 0, 0);
//			np = eh->path[i]->parent;
//		}
	}

	/**
	 * Method to describe error "elv".
	 */
	public String describeError() { return message; }

	/**
	 * Method to highlight and report error "elv".
	 */
//	void reporterror(void *elv)
//	{
//		REGISTER ERRORLIST *el;
//
//		if (elv == 0) return;
//		el = (ERRORLIST *)elv;
//		db_curerrorlist = el;
//		db_nexterrorlist = db_curerrorlist->nexterrorlist;
//		db_preverrorlist = db_curerrorlist->preverrorlist;
//		(void)db_reportcurrenterror(1, 0, 0);
//	}

	/**
	 * Method to request that "routine" be called whenever any changes are made to the list of
	 * errors.
	 */
//	void adviseofchanges(void (*routine)(void))
//	{
//		db_errorchangedroutine = routine;
//	}

	/******************** ERROR REPORTING SUPPORT METHODS ********************/

	/**
	 * Method to return the error message associated with the current error.
	 * Highlights associated graphics if "showhigh" is nonzero.  Fills "g1" and "g2"
	 * with associated geometry modules (if nonzero).
	 */
//	#define MAXCELLS 20

//	String db_reportcurrenterror(int showhigh, Geometric **g1, Geometric **g2)
//	{
//		REGISTER ERRORLIST *el;
//		REGISTER NODEPROTO *cell;
//		REGISTER PORTPROTO *pp;
//		NODEPROTO *celllist[MAXCELLS];
//		REGISTER INTBIG i, j, consize, numcells, havegeoms, newwindows, count, hierpathcount;
//		REGISTER NODEINST **hierpath, *ni;
//		REGISTER ARCINST *ai;
//		INTBIG lx, hx, ly, hy;
//		REGISTER ERRORHIGHLIGHT *eh;
//		REGISTER GEOM *geom1, *geom2;
//		REGISTER WINDOWPART *w;
//		WINDOWPART *neww[4];
//		REGISTER void *infstr;
//
//		el = db_curerrorlist;
//		if (el == NOERRORLIST)
//		{
//			infstr = initinfstr();
//			formatinfstr(infstr, _("No %s errors"), errorSystem);
//			return(returninfstr(infstr));
//		}
//
//		// turn off highlighting
//		if (showhigh != 0)
//		{
//			(void)asktool(us_tool, x_("clear"));
//
//			// validate the cell (it may have been deleted)
//			cell = el->cell;
//			if (cell != NONODEPROTO)
//			{
//				if (!db_validatecell(cell))
//				{
//					infstr = initinfstr();
//					formatinfstr(infstr, _("%s error %ld of %ld (but cell is deleted): %s"), errorSystem,
//						el->index, db_numerrors, el->message);
//					return(returninfstr(infstr));
//				}
//			}
//
//			// first figure out which cells need to be displayed
//			numcells = 0;
//			for(i=0; i<el->numhighlights; i++)
//			{
//				eh = el->highlights[i];
//				hierpathcount = 0;
//				cell = el->cell;
//				if (eh->showgeom && eh->type == ERRORTYPEGEOM && cell == NONODEPROTO)
//				{
//					cell = geomparent(eh->geom);
//					if (cell != NONODEPROTO && !db_validatecell(cell))
//						cell = NONODEPROTO;
//				}
//				switch (eh->type)
//				{
//					case ERRORTYPEGEOM:
//						if (!eh->showgeom) cell = NONODEPROTO; else
//							if (cell != NONODEPROTO)
//						{
//							// validate the geometry
//							for(ai = cell->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst)
//								if (ai->geom == eh->geom) break;
//							if (ai == NOARCINST)
//							{
//								for(ni = cell->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//									if (ni->geom == eh->geom) break;
//								if (ni == NONODEINST)
//								{
//									// geometry pointer is not valid
//									eh->showgeom = FALSE;
//									cell = NONODEPROTO;
//								}
//							}
//						}
//						if (eh->showgeom)
//						{
//							cell = geomparent(eh->geom);
//							if (eh->pathlen > 0)
//							{
//								hierpathcount = eh->pathlen;
//								hierpath = eh->path;
//							}
//						}
//						break;
//					case ERRORTYPEEXPORT:
//						if (!eh->showgeom) cell = NONODEPROTO; else
//							if (cell != NONODEPROTO)
//						{
//							// validate the export
//							for(pp = cell->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//								if (pp == eh->pp) break;
//							if (pp == NOPORTPROTO)
//							{
//								eh->showgeom = FALSE;
//								cell = NONODEPROTO;
//							} else cell = eh->pp->parent;
//						}
//						break;
//					case ERRORTYPELINE:
//					case ERRORTYPEPOINT:
//						break;
//				}
//				if (cell == NONODEPROTO) continue;
//				for(j=0; j<numcells; j++)
//					if (celllist[j] == cell) break;
//				if (j < numcells) continue;
//				if (numcells >= MAXCELLS) break;
//				celllist[numcells] = cell;
//				numcells++;
//			}
//
//			// be sure that all requested cells are shown
//			newwindows = 0;
//			for(i=0; i<numcells; i++)
//			{
//				// see if the cell is already being displayed
//				for(w = el_topwindowpart; w != NOWINDOWPART; w = w->nextwindowpart)
//					if (w->curnodeproto == celllist[i]) break;
//				if (w != NOWINDOWPART)
//				{
//					// already displayed: mark this cell done
//					bringwindowtofront(w->frame);
//					celllist[i] = NONODEPROTO;
//					continue;
//				}
//
//				// keep a count of the number of new windows needed
//				newwindows++;
//			}
//			while (newwindows > 0)
//			{
//				neww[0] = us_wantnewwindow(0);
//				newwindows--;
//				if (newwindows > 0)
//				{
//					el_curwindowpart = neww[0];
//					neww[1] = us_splitcurrentwindow(2, FALSE, &neww[0], 50);
//					newwindows--;
//				}
//				if (newwindows > 0)
//				{
//					el_curwindowpart = neww[0];
//					neww[2] = us_splitcurrentwindow(1, FALSE, &neww[0], 50);
//					newwindows--;
//				}
//				if (newwindows > 0)
//				{
//					el_curwindowpart = neww[1];
//					neww[3] = us_splitcurrentwindow(1, FALSE, &neww[1], 50);
//					newwindows--;
//				}
//				count = 0;
//				for(i=0; i<numcells; i++)
//				{
//					if (celllist[i] == NONODEPROTO) continue;
//					el_curwindowpart = neww[count++];
//					us_fullview(celllist[i], &lx, &hx, &ly, &hy);
//					us_switchtocell(celllist[i], lx, hx, ly, hy, NONODEINST, NOPORTPROTO, FALSE, FALSE, FALSE);
//					celllist[i] = NONODEPROTO;
//					if (count >= 4) break;
//				}
//			}
//		}
//
//		// first show the geometry associated with this error
//		geom1 = geom2 = NOGEOM;
//		havegeoms = 0;
//		for(i=0; i<el->numhighlights; i++)
//		{
//			eh = el->highlights[i];
//			if (showhigh == 0 || !eh->showgeom) continue;
//			switch (eh->type)
//			{
//				case ERRORTYPEGEOM:
//					if (geom1 == NOGEOM) geom1 = eh->geom; else
//						if (geom2 == NOGEOM) geom2 = eh->geom;
//
//					// include this geometry module in list to show
//					if (havegeoms == 0) infstr = initinfstr(); else
//						addtoinfstr(infstr, '\n');
//					havegeoms++;
//					formatinfstr(infstr, x_("CELL=%s FROM=0%lo;-1;0"),
//						describenodeproto(geomparent(eh->geom)), (INTBIG)eh->geom);
//					break;
//				case ERRORTYPEEXPORT:
//					if (havegeoms == 0) infstr = initinfstr(); else
//						addtoinfstr(infstr, '\n');
//					havegeoms++;
//					formatinfstr(infstr, x_("CELL=%s TEXT=0%lo;0%lo;-"),
//						describenodeproto(eh->pp->parent), (INTBIG)eh->pp->subnodeinst->geom,
//							(INTBIG)eh->pp);
//					break;
//				case ERRORTYPELINE:
//				case ERRORTYPEPOINT:
//					break;
//			}
//
//			// set the hierarchical path
//			if (eh->type == ERRORTYPEGEOM && eh->showgeom && eh->pathlen > 0)
//			{
//				cell = geomparent(eh->geom);
//				for(w=el_topwindowpart; w != NOWINDOWPART; w = w->nextwindowpart)
//					if (w->curnodeproto == cell) break;
//				if (w != NOWINDOWPART)
//				{
//					for(j=eh->pathlen-1; j>=0; j--)
//					{
//						sethierarchicalparent(cell, eh->path[j], w, 0, 0);
//						cell = eh->path[j]->parent;
//					}
//				}
//			}
//		}
//
//		if (havegeoms != 0)
//			(void)asktool(us_tool, x_("show-multiple"), (INTBIG)returninfstr(infstr));
//
//		// now show the lines and points associated with this error
//		for(i=0; i<el->numhighlights; i++)
//		{
//			eh = el->highlights[i];
//			switch (eh->type)
//			{
//				case ERRORTYPELINE:
//					if (showhigh != 0)
//						(void)asktool(us_tool, x_("show-line"), eh->x1, eh->y1, eh->x2, eh->y2,
//							el->cell);
//					break;
//				case ERRORTYPEPOINT:
//					if (showhigh != 0)
//					{
//						consize = lambdaofcell(el->cell) * 5;
//						(void)asktool(us_tool, x_("show-line"), eh->x1-consize, eh->y1-consize,
//							eh->x1+consize, eh->y1+consize, el->cell);
//						(void)asktool(us_tool, x_("show-line"), eh->x1-consize, eh->y1+consize,
//							eh->x1+consize, eh->y1-consize, el->cell);
//					}
//					break;
//				case ERRORTYPEGEOM:
//				case ERRORTYPEEXPORT:
//					break;
//			}
//		}
//
//		// return geometry if requested
//		if (g1 != 0) *g1 = geom1;
//		if (g2 != 0) *g2 = geom2;
//
//		// return the error message
//		infstr = initinfstr();
//		formatinfstr(infstr, _("%s error %ld of %ld: %s"), errorSystem,
//			el->index, db_numerrors, el->message);
//		return(returninfstr(infstr));
//	}

//	BOOLEAN db_validatecell(NODEPROTO *cell)
//	{
//		REGISTER LIBRARY *lib;
//		REGISTER NODEPROTO *np;
//
//		for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//		{
//			for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//				if (np == cell) return(TRUE);
//		}
//		return(FALSE);
//	}
}
