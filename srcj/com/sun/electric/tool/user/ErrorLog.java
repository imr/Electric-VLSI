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
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.ExplorerTree;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.Collections;
import java.awt.geom.Point2D;
import javax.swing.tree.DefaultMutableTreeNode;

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
	private static int currentErrorNumber;
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
		currentErrorNumber = -1;
		errorSystem = system;
		errorLimit = User.getErrorLimit();
	}

	/**
	 * Factory method to create an error message and log.
	 * with the given text "message" applying to cell "cell".
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
		currentErrorNumber = allErrors.size()-1;

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
//		Rectangle2D bounds = poly.getBox();
//		if (bounds != null)
//		{
//			(void)addlinetoerror(errorlist, bounds.getMinX(), bounds.getMinY(), bounds.getMinX(), bounds.getMaxY());
//			(void)addlinetoerror(errorlist, bounds.getMinX(), bounds.getMaxY(), bounds.getMaxX(), bounds.getMaxY());
//			(void)addlinetoerror(errorlist, bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxX(), bounds.getMinY()) ;
//			(void)addlinetoerror(errorlist, bounds.getMaxX(), bounds.getMinY(), bounds.getMinX(), bounds.getMinY());;
//		} else
		{
			Point2D [] points = poly.getPoints();
			for(int i=0; i<points.length; i++)
			{
				int prev = i-1;
				if (i == 0) prev = points.length-1;
				addLine(points[prev].getX(), points[prev].getY(),
					points[i].getX(), points[i].getY());
			}
		}
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
		rebuildExplorerTree();
		ExplorerTree.explorerTreeChanged();
		EditWindow.redrawAll();
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
	public static String reportNextError()
	{
		return reportNextError(true, null);
	}

	/**
	 * Method to advance to the next error and report it.
	 */
	public static String reportNextError(boolean showhigh, Geometric [] gPair)
	{
		if (currentErrorNumber < allErrors.size()-1)
		{
			currentErrorNumber++;
		} else
		{
			if (allErrors.size() <= 0) return "no errors";
			currentErrorNumber = 0;
		}

		ErrorLog el = (ErrorLog)allErrors.get(currentErrorNumber);
		return el.db_reportcurrenterror(showhigh, gPair);
	}

	/**
	 * Method to back up to the previous error and report it.
	 */
	public static String reportPrevError()
	{
		if (currentErrorNumber > 0)
		{
			currentErrorNumber--;
		} else
		{
			if (allErrors.size() <= 0) return "no errors";
			currentErrorNumber = allErrors.size() - 1;
		}

		ErrorLog el = (ErrorLog)allErrors.get(currentErrorNumber);
		return el.db_reportcurrenterror(true, null);
	}

	/**
	 * Method to tell the number of logged errors.
	 * @return the number of "ErrorLog" objects logged.
	 */
	public static int getNumErrors() { return allErrors.size(); }

	/**
	 * Method to list all logged errors.
	 * @return an Iterator over all of the "ErrorLog" objects.
	 */
	public static Iterator getErrors() { return allErrors.iterator(); }

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

	/** error tree */            private static DefaultMutableTreeNode explorerTree = new DefaultMutableTreeNode("ERRORS");

	public static DefaultMutableTreeNode getExplorerTree()
	{
		rebuildExplorerTree();
		return explorerTree;
	}
	
	private static void rebuildExplorerTree()
	{
		explorerTree.removeAllChildren();
		for (Iterator it = allErrors.iterator(); it.hasNext();)
		{
			ErrorLog el = (ErrorLog)it.next();
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(el);
			explorerTree.add(node);
		}
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
	public String reportError()
	{
		return db_reportcurrenterror(true, null);
	}

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
	private String db_reportcurrenterror(boolean showhigh, Geometric [] gPair)
	{
		if (allErrors.size() == 0)
		{
			return "No " + errorSystem + " errors";
		}

		// if two highlights are requested, find them
		if (gPair != null)
		{
			Geometric geom1 = null, geom2 = null;
			for(Iterator it = highlights.iterator(); it.hasNext(); )
			{
				ErrorHighlight eh = (ErrorHighlight)it.next(); 
				if (eh.type == ERRORTYPEGEOM)
				{
					if (geom1 == null) geom1 = eh.geom; else
						if (geom2 == null) geom2 = eh.geom;
				}
			}

			// return geometry if requested
			if (geom1 != null) gPair[0] = geom1;
			if (geom2 != null) gPair[1] = geom2;
		}

		// show the error
		if (showhigh)
		{
			Highlight.clear();

			// validate the cell (it may have been deleted)
			if (cell != null)
			{
				if (!cell.isLinked())
				{
					String msg = errorSystem + " error " + index + " of " + allErrors.size() + " (but cell is deleted): " + message;
					return msg;
				}
			}

			// first figure out which cells need to be displayed
			Set cells = new TreeSet();
			for(Iterator it = highlights.iterator(); it.hasNext(); )
			{
				ErrorHighlight eh = (ErrorHighlight)it.next();
				Cell showCell = null;
//				hierpathcount = 0;
				if (eh.showgeom && eh.type == ERRORTYPEGEOM && showCell == null)
				{
					showCell = eh.geom.getParent();
					if (showCell != null && !showCell.isLinked())
						showCell = null;
				}
				switch (eh.type)
				{
					case ERRORTYPEGEOM:
						if (!eh.showgeom) break;
						if (showCell != null)
						{
							// validate the geometry
							boolean found = false;
							if (!eh.geom.isLinked())
							{
								// geometry pointer is not valid
								eh.showgeom = false;
								showCell = null;
							}
						} else
						{
							showCell = eh.geom.getParent();
//							if (eh.pathlen > 0)
//							{
//								hierpathcount = eh.pathlen;
//								hierpath = eh.path;
//							}
						}
						break;
					case ERRORTYPEEXPORT:
						if (!eh.showgeom) showCell = null; else
							if (showCell != null)
						{
							showCell = (Cell)eh.pp.getParent();

							// validate the export
//							for(pp = cell->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//								if (pp == eh.pp) break;
//							if (pp == NOPORTPROTO)
//							{
//								eh.showgeom = FALSE;
//								cell = null;
//							}
						}
						break;
					case ERRORTYPELINE:
					case ERRORTYPEPOINT:
						break;
				}
				if (showCell == null) continue;
				cells.add(showCell.describe());
			}

			// be sure that all requested cells are shown
			int newwindows = 0;
			for(Iterator it = cells.iterator(); it.hasNext(); )
			{	
				// see if the cell is already being displayed
				String cellName = (String)it.next();
				Cell showCell = (Cell)NodeProto.findNodeProto(cellName);
//				for(w = el_topwindowpart; w != NOWINDOWPART; w = w->nextwindowpart)
//					if (w->curnodeproto == showCell) break;
//				if (w != NOWINDOWPART)
//				{
//					// already displayed: mark this cell done
//					bringwindowtofront(w->frame);
//					celllist[i] = NONODEPROTO;
//					continue;
//				}

				// keep a count of the number of new windows needed
				newwindows++;
			}
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

			// first show the geometry associated with this error
			for(Iterator it = highlights.iterator(); it.hasNext(); )
			{
				ErrorHighlight eh = (ErrorHighlight)it.next(); 
				switch (eh.type)
				{
					case ERRORTYPEGEOM:
						if (!eh.showgeom) break;
						Highlight.addElectricObject(eh.geom, cell);
						break;
					case ERRORTYPEEXPORT:
//						Highlight.addElectricObject(eh.pp, cell);
//						if (havegeoms == 0) infstr = initinfstr(); else
//							addtoinfstr(infstr, '\n');
//						havegeoms++;
//						formatinfstr(infstr, x_("CELL=%s TEXT=0%lo;0%lo;-"),
//							describenodeproto(eh->pp->parent), (INTBIG)eh->pp->subnodeinst->geom,
//								(INTBIG)eh->pp);
						break;
					case ERRORTYPELINE:
						Highlight.addLine(new Point2D.Double(eh.x1, eh.y1), new Point2D.Double(eh.x2, eh.y2), cell);
						break;
					case ERRORTYPEPOINT:
						double consize = 5;
						Highlight.addLine(new Point2D.Double(eh.x1-consize, eh.y1-consize), new Point2D.Double(eh.x2+consize, eh.y2+consize), cell);
						Highlight.addLine(new Point2D.Double(eh.x1-consize, eh.y1+consize), new Point2D.Double(eh.x2+consize, eh.y2-consize), cell);
						break;
				}

//				// set the hierarchical path
//				if (eh.type == ERRORTYPEGEOM && eh.showgeom && eh.pathlen > 0)
//				{
//					cell = geomparent(eh.geom);
//					for(w=el_topwindowpart; w != NOWINDOWPART; w = w->nextwindowpart)
//						if (w->curnodeproto == cell) break;
//					if (w != NOWINDOWPART)
//					{
//						for(int j=eh.pathlen-1; j>=0; j--)
//						{
//							sethierarchicalparent(cell, eh.path[j], w, 0, 0);
//							cell = eh.path[j].getParent();
//						}
//					}
//				}
			}

			Highlight.finished();
		}

		// return the error message
		String msg = errorSystem + " error " + index + " of " + allErrors.size() + ": " + message;
		return msg;
	}
}
