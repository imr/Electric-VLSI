/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VectorCache.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.database.geometry.bool;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.PolyBase;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class UnloadPolys {

    private static class Polys {
        Polys	next;
        int x, y;
    }

    Polys polyPool;

    private static final int IN = 1;
    private static final int OUT = 0;
    private static final int B = OUT;
    private static final int W = IN;

    private static class Arc {
        Arc next;
        int[] y = new int[2];
        Arc[] b = new Arc[2], t = new Arc[2];
        List<PolyBase.PolyBaseTree> sons;
        Polys pol;

        private void addSon(PolyBase.PolyBaseTree son) {
            if (sons == null)
                sons = new ArrayList<PolyBase.PolyBaseTree>();
            sons.add(son);
        }

        private void addSons(List<PolyBase.PolyBaseTree> newSons) {
            if (newSons == null || newSons.isEmpty())
                return;
            if (sons == null)
                sons = new ArrayList<PolyBase.PolyBaseTree>();
            sons.addAll(newSons);
        }
    }

    Arc arcPool;

    private int[] inpA = new int[1];
    private int inpC;

    private int x;

    private Arc mainArc;
    private Arc top;
    private List<Arc> stack = new ArrayList<Arc>();
    private int v, nv;

    private boolean rotated;

    public List<PolyBase.PolyBaseTree> loop(DataInputStream inpS, boolean rotated) throws IOException {
        this.rotated = rotated;
    	mainArc = newArc();
    	mainArc.y[IN] = Integer.MIN_VALUE;
        mainArc.y[OUT] = Integer.MAX_VALUE;
        mainArc.t[IN] = mainArc;
        mainArc.b[OUT] = mainArc;
        
        while (getLine(inpS)) {
//            printInp();
            scanLine();
        }
        assert mainArc.t[IN] == mainArc && mainArc.b[OUT] == mainArc;
        List<PolyBase.PolyBaseTree> result = mainArc.sons;
        if (result == null)
            result = Collections.emptyList();
        dispArc(mainArc);
        return result;
    }

    private boolean getLine(DataInputStream inpS) throws IOException {
        resetInp();
        boolean eof = inpS.readBoolean();
        if  (!eof) {
            return false;
        }

        x = inpS.readInt();
        int count = inpS.readInt();
        while (count > inpA.length) {
            int[] newInpA = new int[inpA.length*2];
            System.arraycopy(inpA, 0, newInpA, 0, inpA.length);
            inpA = newInpA;
        }
        while (inpC < count) {
            inpA[inpC++] = inpS.readInt();
        }
        return true;
    }

/*    +->----!   OUT	*/
/*    | main !		*/
/*    |      !   W	*/
/*    |  #-<-!.. IN	*/
/*    |  |***! .	*/
/*    |  |***! .	*/
/*    |  |***! . B	*/
/*    |  +->-!.# OUT	*/
/*    |      ! poly	*/
/*    +-<----!   IN	*/

    private void scanLine() {
		stack.clear();
		top = mainArc;
		v = W; nv = B;
        /*   top->y[v] < top->y[!v]   */

		Arc al = top;

        int y = 0;
        int d = 0;
        for (int inpPos = 0; inpPos < inpC; ) {
            if (Math.abs(d) == 2) {
                d >>= 1;
            } else {
                int inpVal = inpA[inpPos++];
                y = inpVal >> 1;
                d = (inpVal & 1) != 0 ? 1 : -1;
                while (inpPos < inpC && (inpA[inpPos] >> 1) == y) {
                    if ((inpA[inpPos++] & 1) != 0) {
                        d++;
                    } else {
                        d--;
                    }
                }
            }
            assert d == 1 || d == -1;

            assert v + nv == 1;
		    while (y >= top.y[nv]) {
                al = top;
                POP();
		    }
            assert top.y[v] <= y && y < top.y[nv];
		    for (;;) {
                while (al.t[v] != top && y >= al.t[v].y[v]) {
                    al = al.t[v];
                }
                if (al.t[v] == top || y < al.t[v].y[nv]) {
                    break;
                }
                al = al.t[v];
                PUSH( al );
		    }
            assert top.y[v] <= y && y < top.y[nv];
		    Arc ar = al.t[v];
		    if (y > al.y[v]) {
                Polys pl = newPolys();
                pl.y = y;
                pl.next = pl;
                Arc at = newArc();
                at.y[nv] = at.y[v] = y;
                at.pol = pl;
                GLUE(al, at, v);
                GLUE(at, at, nv);
                GLUE(at, ar, v);
                al = at;
		    }
		    assert y == al.y[v];
            assert ar == al.t[v];
            assert al == ar.b[nv];
            {
                int inpVal = inpA[inpPos++];
                y = inpVal >> 1;
                d = (inpVal & 1) != 0 ? 1 : -1;
                while (inpPos < inpC && (inpA[inpPos] >> 1) == y) {
                    if ((inpA[inpPos++] & 1) != 0) {
                        d++;
                    } else {
                        d--;
                    }
                }
                assert d == 1 || d == -1 || d == 2 || d == -2;
            }
		    assert y <= ar.y[nv];
		    if ( y < ar.y[nv] /*|| Math.abs(d) == 2*/) {
                al.y[v] = y;
                Polys pl = newPolys();
                pl.y = y;
                pl.next = pl;
                al.pol = CAT( al.pol, v, pl);
		    } else {
                Arc aln = al.b[v];
                Arc arn = ar.t[nv];

                GLUE(aln, arn, nv);
                if (al == ar) {
                    assert al == top;
                    top.pol.x = x;
                    PolyBase.PolyBaseTree t = outTree(top.pol);
                    if (top.sons != null) {
                        for (PolyBase.PolyBaseTree s: top.sons)
                            t.addSonLowLevel(s);
                    }
//                    out(top.pol,v);
                    dispArc(top);
                    POP();
                    top.addSon(t);
                } else if (al == top) {
                    top.pol = CAT(top.pol, v, ar.pol);
                    REPLACE(ar,top,v);
                    arn = aln.t[nv];
                    List<PolyBase.PolyBaseTree> sons = ar.sons;
                    dispArc(ar);
                    POP();
                    top.addSons(sons);
                } else if (ar == top) {
                    top.pol = CAT(top.pol, nv, al.pol);
                    REPLACE(al, top, nv);
                    aln = arn.b[v];
                    List<PolyBase.PolyBaseTree> sons = al.sons;
                    dispArc(al);
                    POP();
                    top.addSons(sons);
                } else {
                    al.pol = CAT(al.pol, v, ar.pol);
                    REPLACE(ar, al, v);
                    arn = aln.t[nv];
                    List<PolyBase.PolyBaseTree> sons = ar.sons;
                    dispArc(ar);
                    al.addSons(sons);
                    PUSH(al);
                }
                al = aln;
                ar = arn;
		    }
        }
    }

    private void out(Polys pl, int v) {
        Polys pg = pl;
        int n = 0;

        int lx = Integer.MAX_VALUE, ly = Integer.MAX_VALUE, hx = Integer.MIN_VALUE, hy = Integer.MIN_VALUE;
    	do {
            lx = Math.min(lx, pg.x);
            hx = Math.max(hx, pg.x);
            ly = Math.min(ly, pg.y);
            hy = Math.max(hy, pg.y);
            pg = pg.next;
            n++;
        } while (pg != pl);
        if (v == B && n == 2 && ((lx + hx)&1) == 0) {
            System.out.println("WIRE " + prCoord(false, (lx + hx)/2) + " " + prCoord(true, ly) +
                    " w=" + DBMath.gridToLambda(hx - lx) + " " + prCoord(true, hy));
//            System.out.println("BOX " + prPoint(lx, ly) + " " + prPoint(hx, hy));
            return;
        }

        if (v == B) {
            Polys top = pl;
            while (top.y != hy) {
                top = top.next;
            }
            List<Polys> left = new ArrayList<Polys>();
            Polys bottom = top;
            while (bottom.next.y < bottom.y) {
                left.add(bottom);
                bottom = bottom.next;
            }
            left.add(bottom);
            List<Polys> right = new ArrayList<Polys>();
            Polys p = bottom;
            while (p.next.y > p.y) {
                right.add(p);
                p = p.next;
            }
            right.add(p);
            if (p == top) {
                int mx = (lx + hx)/2;
                System.out.print("WIRE " + prCoord(false, mx) + " " + prCoord(true, ly));
                int leftI = left.size() - 1;
                int rightI = 0;
                for (;;) {
                    int leftW = mx - left.get(leftI - 1).x;
                    int rightW = right.get(rightI).x - mx;
                    System.out.print(" w=");
                    if (leftW == rightW) {
                        System.out.print(DBMath.gridToLambda(leftW + rightW));
                    } else {
                        System.out.print(DBMath.gridToLambda(leftW) + "+" + DBMath.gridToLambda(rightW));
                    }
                    int nextY = Math.min(left.get(leftI - 1).y, right.get(rightI + 1).y);
                    System.out.print(" " + prCoord(true, nextY));
                    if (nextY == hy) {
                        assert leftI == 1 && rightI == right.size() - 2;
                        break;
                    }
                    if (nextY == left.get(leftI - 1).y) {
                        leftI--;
                    }
                    if (nextY == right.get(rightI + 1).y) {
                        rightI++;
                    }
                }
                System.out.println();
//                System.out.print("POLY");
//                do {
//                    System.out.print(" " + prPoint(pg.x, pg.y));
//                    System.out.print(" " + prPoint(pg.x, pg.next.y));
//                    pg = pg.next;
//                } while (pg != pl);
//                System.out.println();
                return;
            }
        }
        if (v == B) {
            System.out.print("POLY");
        } else {
            System.out.print("NEGATIVE POLY");
        }
    	do {
            System.out.print(" " + prPoint(pg.x, pg.y));
            System.out.print(" " + prPoint(pg.x, pg.next.y));
            pg = pg.next;
        } while (pg != pl);
        System.out.println();
    }

    PolyBase.PolyBaseTree outTree(Polys pl) {
        Polys pg = pl;
        int n = 0;
    	do {
            pg = pg.next;
            n++;
        } while (pg != pl);
        EPoint[] pts = new EPoint[n*2];
        if (rotated) {
            int k = 0;
            do {
                pts[k++] = EPoint.fromGrid(pg.y, pg.x);
                pts[k++] = EPoint.fromGrid(pg.next.y, pg.x);
                pg = pg.next;
            } while (pg != pl);
        } else {
            int k = 0;
            do {
                pts[k++] = EPoint.fromGrid(pg.x, pg.y);
                pts[k++] = EPoint.fromGrid(pg.x, pg.next.y);
                pg = pg.next;
            } while (pg != pl);
        }
        PolyBase p = new PolyBase(pts);
        return new PolyBase.PolyBaseTree(p);
    }

    private String prPoint(int x, int y) {
        return rotated ? prCoord(y) + "," + prCoord(x) : prCoord(x) + "," + prCoord(y);
    }

    private String prCoord(boolean isY, int v) {
        return (isY^rotated ? "y=" : "x=") + prCoord(v);
    }

    private String prCoord(int v) {
        double period = 144*400;
        int n = (int)Math.floor(v/period);
        return n+"#"+DBMath.gridToLambda(v - n*period);
    }

    private void GLUE(Arc al, Arc ar, int v)	{
        al.t[v] = ar;
        ar.b[1 - v] = al;
    }
    
    private void REPLACE(Arc so, Arc sn, int v) {
        int nv = 1 - v;
        sn.y[v] = so.y[v];
        so.b[v].t[nv] = sn;
        so.t[v].b[nv] = sn;
        sn.b[v] = so.b[v];
        sn.t[v] = so.t[v];
    }

    private void PUSH(Arc a) {
        stack.add(top);
        top = a;
        nv = v;
        v = 1 - v;
    }

    private void POP() {
        nv = v;
        v = 1 - v;
        top = stack.remove(stack.size() - 1);
    }

    private Polys CAT(Polys pl, int v, Polys pg) {
        (v == IN ? pg : pl ).x = x;
        Polys pt = pl.next;
        pl.next = pg.next;
        pg.next = pt;
        /*pl=*/ return v == IN ? pl : pg;
    }

    private void printInp() throws IOException {
        System.out.print("x=" + DBMath.gridToLambda(x));
        for (int i = 0; i < inpC; i++) {
            int inpVal = inpA[i];
            int y = inpVal >> 1;
            boolean df = (inpVal & 1) != 0;
            System.out.print(" " + DBMath.gridToLambda(y) + (df?"^":"_"));
        }
        System.out.println();
    }

    private void resetInp() {
        inpC = 0;
    }

    private void putPointInp(int y, int val) {
        if (inpC >= inpA.length) {
            int[] newInpA = new int[inpA.length*2];
            System.arraycopy(inpA, 0, newInpA, 0, inpA.length);
            inpA = newInpA;
        }
//        System.out.println("putPointInp " + y + " " + val);
        inpA[inpC++] = (y << 1) | (val > 0 ? 1 : 0);

    }

    private Polys newPolys() {
        Polys result;
        if (polyPool == null) {
            result = new Polys();
        } else {
            result = polyPool;
            polyPool = polyPool.next;
        }
//        result.next = next;
        return result;
    }

    private void dispPolys(Polys p) {
        p.next = polyPool;
        polyPool = p;
    }

    private Arc newArc() {
        Arc result;
        if (arcPool == null) {
            result = new Arc();
        } else {
            result = arcPool;
            arcPool = arcPool.next;
        }
//        result.next = next;
        return result;
    }

    private void dispArc(Arc p) {
        p.next = arcPool;
        arcPool = p;
        p.sons = null;
    }
}
