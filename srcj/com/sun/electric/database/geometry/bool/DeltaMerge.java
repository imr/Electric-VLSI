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

import java.awt.Point;
import java.awt.Rectangle;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 */
public class DeltaMerge {

    private static class Segment {
        private Segment next;
        private int y; // begin of the segment
        private int val; // height at points below y
    }

    private static class NegativePoint extends Point {
        private NegativePoint(int x, int y) {
            super(x, y);
        }
    }

    private static final Segment segLast = new Segment();
    static {
        segLast.y = Integer.MAX_VALUE;
    }

    private Segment segPool;
    private Segment chain;
    private int[] inpA = new int[1];
    private int inpC;
    private int[] outA = new int[1];
    private int outC;

    private List<Point> points = new ArrayList<Point>();

    private int curPoint;
    private int x;

    public DeltaMerge() {
        chain = new Segment();
        chain.next = segLast;
        chain.y = Integer.MIN_VALUE;
    }

    public void loop(Collection<Rectangle> rects, DataOutputStream out) throws IOException {
        for (Rectangle rect: rects) {
            put(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
        }
        Collections.sort(points, new Comparator<Point> () {
            public int compare(Point p1, Point p2) {
                if (p1.x < p2.x) {
                    return -1;
                } else if (p1.x > p2.x) {
                    return 1;
                } else if (p1.y < p2.y) {
                    return -1;
                } else if (p1.y > p2.y) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        curPoint = 0;
        while (getLine()) {
            scanLine();
            printOut(out);
            printSegments();
        }
        out.writeBoolean(false);
    }

    private void put(int lx, int ly, int hx, int hy) {
        put(lx, ly, true);
        put(lx, hy, false);
        put(hx, ly, false);
        put(hx, hy, true);
    }

    private void put(int x, int y, boolean positive) {
        points.add(positive ? new Point(x, y) : new NegativePoint(x, y));
    }

    private void printOut(DataOutputStream out) throws IOException {
        if (outC == 0) {
            return;
        }
        out.writeBoolean(true);
//        System.out.print("x=" + DBMath.gridToLambda(x));
        out.writeInt(x);
        out.writeInt(outC);
        for (int i = 0; i < outC; i++) {
            int outVal = outA[i];
            int y = outVal >> 1;
            boolean df = (outVal & 1) != 0;
//            System.out.print(" " + DBMath.gridToLambda(y) + (df?"^":"_"));
            out.writeInt(outVal);
        }
//        System.out.println();
    }

    private boolean getLine() {
        resetInp();
        if  (curPoint >= points.size()) {
            return false;
        }
        Point p = points.get(curPoint++);
        x = p.x;
        for (;;) {
            putPointInp(p.y, p instanceof NegativePoint ? -1 : 1);
            if (curPoint >= points.size()) {
                break;
            }
            p = points.get(curPoint);
            if (p.x != x) {
                break;
            }
            curPoint++;
        }
        return true;
    }

    private void printSegments() {
        for (Segment cp = chain; cp.next != segLast; cp = cp.next) {
//            System.out.println("y="+cp.y+" val="+cp.val);
            assert cp.y < cp.next.y;
            assert cp.val >= 0;
        }
    }

    private void checkSegments() {
        assert chain.y == Integer.MIN_VALUE;
        assert chain.val == 0;
        for (Segment cp = chain; cp.next != segLast; cp = cp.next) {
            assert cp.y < cp.next.y;
            assert cp.val >= 0;
        }
        assert segLast.y == Integer.MAX_VALUE;
        assert segLast.val == 0;
    }

    private void scanLine() {
        assert inpC > 0;
        int inpStep = 0;
        int outStep = 0;
        Segment cp = chain;
        resetOut();
        for (int inpPos = 0; inpPos < inpC; ) {
            int inpVal = inpA[inpPos++];
            int y = inpVal >> 1;
            assert cp.y < y;
            int df = (inpVal & 1) != 0 ? 1 : -1;
            while (inpPos < inpC && (inpA[inpPos] >> 1) == y) {
                if ((inpA[inpPos++] & 1) != 0) {
                    df++;
                } else {
                    df--;
                }
            }
            if (df == 0) {
                continue;
            }

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
                        putPointOut(p.y, newOutStep - outStep);
                        outStep = newOutStep;
                    }
                    cp = p;
                }
            }
            assert cp.y < y && y <= cp.next.y;
            Segment p;
			if (cp.next.y > y) {
                p = newSegment(cp.next);
				p.y = y;
                p.val = cp.val - inpStep;
                cp.next = p;
			} else {
                p = cp.next;
            }
			inpStep += df;
            assert p.y == y;
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
            assert cp.y <= y;
		}
		assert inpStep == 0;
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

    private void resetOut() {
        outC = 0;
    }

    private void putPointOut(int y, int val) {
        if (outC >= outA.length) {
            int[] newOutA = new int[outA.length*2];
            System.arraycopy(outA, 0, newOutA, 0, outA.length);
            outA = newOutA;
        }
        if (val == +1) {
            outA[outC++] = (y << 1) | 1;
        } else if (val == -1) {
            outA[outC++] = (y << 1);
        } else if (val == +2) {
            outA[outC++] = (y << 1) | 1;
            putPointOut(y, +1);
        } else if (val == -2) {
            outA[outC++] = (y << 1);
            putPointOut(y, -1);
        } else {
            throw new AssertionError();
        }
    }

    private Segment newSegment(Segment next) {
        Segment result;
        if (segPool == null) {
            result = new Segment();
        } else {
            result = segPool;
            segPool = segPool.next;
        }
        result.next = next;
        return result;
    }

    private void dispSegment(Segment p) {
        p.next = segPool;
        segPool = p;
    }
}
