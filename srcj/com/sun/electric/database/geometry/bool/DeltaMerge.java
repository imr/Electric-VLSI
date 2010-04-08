/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Collect.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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

import java.awt.Rectangle;
import java.util.Collection;

/**
 *
 */
public class DeltaMerge {

    private static class Segment {
        private Segment next;
        private int y;
        private int val; // height at points below y
    }

    private static final Segment segLast = new Segment();
    static {
        segLast.y = Integer.MAX_VALUE;
    }

    private Segment segPool;
    private Segment chain;
    private int[] inpA;
    private int inpC;
    private int[] outA = new int[2];
    private int outC;

    private int x;

    DeltaMerge() {

    }
    
    private void put(Collection<Rectangle> rects) {
        
    }

    private void printOut() {
        System.out.println("x=" + x);
        for (int i = 0; i < outC; i++) {
            int outVal = outA[i];
            int y = outVal >> 1;
            boolean df = (outVal & 1) != 0;
            System.out.print(" " + y + (df?"^":"_"));
        }
        System.out.println();
    }

    private void scanLine() {
        assert inpC > 0;
        int inpStep = 0;
        int outStep = 0;
        Segment cp = chain;
        resetOut();
        for (int inpPos = 0; inpPos < inpC; inpPos++) {
            int inpVal = inpA[inpPos];
            int y = inpVal >> 1;
            int df = (inpVal & 1) != 0 ? 1 : -1;

            if (inpStep == 0) {
                while (cp.next.y < y) {
                    cp = cp.next;
                }
            } else {
                while (cp.next.y < y) {
                    Segment p = cp.next;
                    int oldO = p.val == 0 ? 0 : 1;
                    p.val += inpStep;
                    int newO = p.val == 0 ? 0 : 1;
                    int newOutStep = newO - oldO;
                    if (newOutStep != outStep) {
                        putPointOut(y, newOutStep - outStep);
                        outStep = newOutStep;
                    }
                    cp = p;
                }
            }
			if (cp.next.y > y) {
                Segment p = newSegment(cp.next);
				p.y = y;
                p.val = cp.val - inpStep;
                cp.next = p;
			}
			inpStep += df;
            Segment p = cp.next;
            int oldO = p.val == 0 ? 0 : 1;
            p.val += inpStep;
            int newO = p.val == 0 ? 0 : 1;
            int newOutStep = newO - oldO;
            if (newOutStep != outStep) {
                putPointOut(y, newOutStep - outStep);
                outStep = newOutStep;
			}
			if (cp.val == p.val) {
				cp.next = p.next;
                dispSegment(p);
			} else {
                cp = p;
            }
		}
		assert inpStep == 0;
    }

    private void resetOut() {
        outC = 0;
    }

    private void putPointOut(int y, int val) {
        if (outC >= outA.length) {
            int[] newOutA = new int[outA.length*2];
            System.arraycopy(outA, 0, newOutA, 0, outA.length);
            outA = newOutA;
        }
        outA[outC++] = (y << 1) | (val > 0 ? 1 : 0);
    }

    private Segment newSegment(Segment next) {
        if (segPool == null) {
            return new Segment();
        }
        Segment result = segPool;
        segPool = segPool.next;
        result.next = next;
        return result;
    }

    private void dispSegment(Segment p) {
        p.next = segPool;
        segPool = p;
    }
}
