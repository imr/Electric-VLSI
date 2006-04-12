/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DebugMenuDima.java
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
package com.sun.electric.tool.user.menus;

import com.sun.electric.Main;
import com.sun.electric.database.AnalyzeHeap;
import com.sun.electric.database.CellUsage;
import com.sun.electric.database.DumpHeap;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.WeakReferences;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.simulation.interval.Diode;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.menus.EMenu;
import com.sun.electric.tool.user.menus.EMenuItem;
import com.sun.electric.tool.user.ui.LayerDrawing;

import java.awt.geom.Point2D;
import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Dima's TEST MENU
 */
public class DebugMenuDima
{
    static EMenu makeMenu() {
        return new EMenu("_Dima",
	        new EMenuItem("Show vector cache") { public void run() {
                LayerDrawing.showVectorCache(); }},
	        new EMenuItem("Show graphics") { public void run() {
                LayerDrawing.showGraphics(); }},
	        new EMenuItem("Test weak references") { public void run() {
                testWeakReferences(); }},
	        new EMenuItem("Show memory usage") { public void run() {
                System.out.println("Memory usage " + Main.getMemoryUsage() + " bytes"); }},
	        new EMenuItem("Plot diode") { public void run() {
                Diode.plotDiode(User.getWorkingDirectory() + File.separator + "diode.raw"); }},
	        new EMenuItem("Var stat") { public void run() {
                varStatistics(); }},
	        new EMenuItem("Dump heap") { public void run() {
                DumpHeap.dump("heapdump.dat"); }},
	        new EMenuItem("Read dump") { public void run() {
                AnalyzeHeap.analyze("heapdump.dat"); }});
    }


	// ---------------------- Dima's Stuff MENU -----------------

    private static void testWeakReferences() {
        ArrayList<SoftReference<Object>> softs = new ArrayList<SoftReference<Object>>();
        WeakReferences<Object> pool = new WeakReferences<Object>();
        for (int i = 0; i < 100; i++) {
            Object o = new int[100000000];
            softs.add(new SoftReference<Object>(o));
            pool.add(o);
        }
        countReferences("Before", pool);
        System.gc();
        System.runFinalization();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}
        countReferences("After", pool);
    }
    
    private static void countReferences(String msg, WeakReferences<Object> pool) {
        int count = 0;
        for (Iterator<Object> it = pool.iterator(); it.hasNext(); ) {
            it.next();
            count++;
        }
        System.out.println(msg + " " + count);
    }
    
	private static int[] objs;
	private static int[] vobjs;
	private static int[] vobjs1;
	private static int[] vcnt;
	private static int numPoints;
	private static HashSet<Point2D> points;
    private static HashSet<TextDescriptor> descriptors;

	private static void varStatistics()
	{
		int subCells = 0;
		int cellUsages = 0;
		int namedArcs = 0;
		int namedNodes = 0;
		int sameLocations = 0;

		objs = new int[96];
		vobjs = new int[96];
		vobjs1 = new int[96];
		vcnt = new int[96];
		points = new HashSet<Point2D>();
        descriptors = new HashSet<TextDescriptor>();
		numPoints = 0;
		
		TreeSet<String> nodeNames = new TreeSet<String>();
		TreeSet<String> arcNames = new TreeSet<String>();

		for (Iterator<Library> lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = lIt.next();
			countVars('H', lib);

			for (Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = cIt.next();
				countVars('C', cell);
				TreeSet<String> cellNodes = new TreeSet<String>();
				TreeSet<String> cellArcs = new TreeSet<String>();

				for (Iterator<CellUsage> uIt = cell.getUsagesIn(); uIt.hasNext(); )
				{
					CellUsage nu = uIt.next();
					cellUsages++;
				}

				for (Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
				{
					NodeInst ni = nIt.next();
					countVars('N', ni);
					if (ni.isCellInstance()) subCells++;
					if (ni.isUsernamed()) namedNodes++;
// 					if (cellNodes.contains(ni.getName()))
// 						System.out.println(cell + " has duplicate node " + ni.getName());
					cellNodes.add(ni.getName());
                    if (ni.isUsernamed()) countDescriptor(ni.getTextDescriptor(NodeInst.NODE_NAME), true, null);
                    if (ni.isCellInstance()) countDescriptor(ni.getTextDescriptor(NodeInst.NODE_PROTO), true, null);
					countPoint(ni.getAnchorCenter());
					
					for (Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
					{
						PortInst pi = pIt.next();
						countVars('P', pi);
					}
				}

				for (Iterator<ArcInst> aIt = cell.getArcs(); aIt.hasNext(); )
				{
					ArcInst ai = aIt.next();
					countVars('A', ai);
					if (ai.isUsernamed()) namedArcs++;
// 					if (cellArcs.contains(ai.getName()))
// 						System.out.println(cell + " has duplicate arc " + ai.getName());
					cellArcs.add(ai.getName());
                    if (ai.isUsernamed()) countDescriptor(ai.getTextDescriptor(ArcInst.ARC_NAME), true, null);
					for (int i = 0; i < 2; i++) {
						Point2D p = ai.getLocation(i);
						if (ai.getPortInst(i).getNodeInst().getAnchorCenter().equals(p))
							sameLocations++;
						countPoint(p);
					}
				}

				for (Iterator<PortProto> eIt = cell.getPorts(); eIt.hasNext(); )
				{
					Export e = (Export)eIt.next();
                    countDescriptor(e.getTextDescriptor(Export.EXPORT_NAME), true, null);
					countVars('E', e);
				}
				nodeNames.addAll(cellNodes);
				arcNames.addAll(cellArcs);
			}
		}

		int o = 0, v = 0, v1 = 0, c = 0;
		for (int i = 0; i < objs.length; i++)
		{
			if (objs[i] == 0) continue;
			System.out.println(((char)i) + " " + objs[i] + " " + vobjs[i] + " " + vobjs1[i] + " " + vcnt[i]);
			o += objs[i];
			v += vobjs[i];
			v1 += vobjs1[i];
			c += vcnt[i];
		}
		System.out.println(o + " " + v + " " + v1 + " " + c);
		if (cellUsages != 0)
			System.out.println(subCells + " subcells " + cellUsages + " cellUsages " +
				((double)subCells)/cellUsages /*+ " " + Math.sqrt(((double)cellSqr)/cellUsages)*/);
		int prims = objs['N'] - subCells;
        System.out.println(prims + " prims");
		System.out.println(namedNodes + " named nodes " + nodeNames.size());
		System.out.println(namedArcs + " named arcs " + arcNames.size());
		System.out.println(sameLocations + " same locations");
		System.out.println(numPoints + " points " + points.size());
		HashSet<Double> doubles = new HashSet<Double>();
		for (Point2D point : points)
		{
			doubles.add(new Double(point.getX()));
			doubles.add(new Double(point.getY()));
		}
		int whole = 0;
		int quarter = 0;
		for (Double dO : doubles)
		{
			double d = dO.doubleValue();
			double rd = Math.rint(d);
			if (d == Math.rint(d))
				whole++;
			else if (d*4 == Math.rint(d*4))
				quarter++;
		}
		System.out.println(doubles.size() + " doubles " + whole + " whole " + quarter + " quarter");
        System.out.println(descriptors.size() + " descriptors. cacheSize=" + TextDescriptor.cacheSize());
/*
loco
A 192665 1657 1657 1657
C 2106 1872 1018 3509
E 37765 189 130 283
H 43 42 37 47
N 113337 4713 2328 22715
P 392542 0 0 0
738458 8473 5170 28211
16916 subcells 3093 cellUsages 5.469123827998707 24.734189873996737
96421 prims 13727 primUsages 7.024185910978364 40.263985564608774
468 named nodes 12604
1496 named arcs 10925
121518 same locations
499519 points 136298
14542 doubles 7299 whole 6728 quarter
1256 descriptors

qFour
A 336551 2504 2504 2504
C 3370 3161 2155 4898
E 112598 309 248 407
H 49 47 43 51
N 188496 8490 4847 32189
P 704883 0 0 0
1345947 14511 9797 40049
25997 subcells 7383 cellUsages 3.5211973452526073 17.251291844283067
162499 prims 20344 primUsages 7.987563900904443 69.86202450228595
910 named nodes 19655
5527 named arcs 10363
233128 same locations
862879 points 230599
18702 doubles 9531 whole 8486 quarter
1515 descriptors

Treasure
A 316961 13134 13102 13166
C 2629 1748 629 12597
E 145528 201 139 302
H 55 13 11 15
N 190346 7007 3034 19337
P 864993 2295 2295 2295
1520512 24398 19210 47712
27069 subcells 4511 cellUsages 6.000665041010862
163277 prims
1001 named nodes 44432
3469 named arcs 54631
173054 same locations
826508 points 291176
20889 doubles 8618 whole 8503 quarter
1461 descriptors. cacheSize=1782
*/
	}

	private static void countVars(char type, ElectricObject eObj)
	{
		int c = (int)type;
		objs[c]++;
		int numVars = eObj.getNumVariables();
		if (numVars == 0) return;
		vobjs[c]++;
		if (numVars == 1) vobjs1[c]++;
		vcnt[c] += numVars;
		for (Iterator<Variable> it = eObj.getVariables(); it.hasNext(); )
		{
			Variable var = it.next();
            countDescriptor(var.getTextDescriptor(), var.isDisplay(), var.getCode());
			Object value = var.getObject();
			if (value instanceof Point2D)
			{
				countPoint((Point2D)value);
			} else if (value instanceof Point2D[])
			{
				Point2D[] points = (Point2D[])value;
				for (int i = 0; i < points.length; i++)
					countPoint(points[i]);
			}
		}
	}

	private static void countPoint(Point2D point)
	{
		double x = DBMath.round(point.getX());
		if (x == 0) x = 0;
		double y = DBMath.round(point.getY());
		if (x == 0) x = 0;
		point = new Point2D.Double(x, y);
		numPoints++;
		points.add(point);
	}

    private static void countDescriptor(TextDescriptor td, boolean display, TextDescriptor.Code code)
    {
        if (code == null) code = TextDescriptor.Code.NONE;
        descriptors.add(td);
    }
}
