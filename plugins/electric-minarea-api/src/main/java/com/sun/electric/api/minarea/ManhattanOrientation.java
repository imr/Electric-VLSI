/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ManhattanOrientation.java
 *
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.api.minarea;

import java.awt.geom.AffineTransform;

import com.sun.electric.api.minarea.geometry.Point;

/**
 * Enumeration to specify Manhattan orientation.
 * The constants are standard EDIF orientations.
 */
public enum ManhattanOrientation {

    /**
     *     MY R0
     *  R90     MXR90
     * MYR90    R270
     *   R180 MX
     * 
     *   XXXXX XXXXX
     *       X X
     * X    XX XX    X
     * X     X X     X
     * X     X X     X
     * X X         X X
     * XXXXX     XXXXX
     * 
     * XXXXX     XXXXX
     * X X         X X
     * X     X X     X
     * X     X X     X
     * X    XX XX    X
     *       X X
     *   XXXXX XXXXX
     */
    R0 {

        public void transformPoints(long[] coords, int offset, int count) {
        }

        public void transformRects(int[] coords, int offset, int count) {
        }

        public Point transformPoint(Point p) {
            return p;
        }
    },
    R90 {

        public void transformPoints(long[] coords, int offset, int count) {
            for (int i = 0; i < count; i++) {
                long x = coords[offset + i * 2 + 0];
                long y = coords[offset + i * 2 + 1];
                coords[offset + i * 2 + 0] = -y;
                coords[offset + i * 2 + 1] = x;
            }
        }

        public void transformRects(int[] coords, int offset, int count) {
            for (int i = 0; i < count; i++) {
                int lx = coords[offset + i * 4 + 0];
                int ly = coords[offset + i * 4 + 1];
                int hx = coords[offset + i * 4 + 2];
                int hy = coords[offset + i * 4 + 3];
                coords[offset + i * 4 + 0] = -hy;
                coords[offset + i * 4 + 1] = lx;
                coords[offset + i * 4 + 2] = -ly;
                coords[offset + i * 4 + 3] = hx;
            }
        }

        public Point transformPoint(Point p) {
            return new Point(-p.getY(), p.getX());
        }
    },
    R180 {

        public void transformPoints(long[] coords, int offset, int count) {
            for (int i = 0; i < count; i++) {
                long x = coords[offset + i * 2 + 0];
                long y = coords[offset + i * 2 + 1];
                coords[offset + i * 2 + 0] = -x;
                coords[offset + i * 2 + 1] = -y;
            }
        }

        public void transformRects(int[] coords, int offset, int count) {
            for (int i = 0; i < count; i++) {
                int lx = coords[offset + i * 4 + 0];
                int ly = coords[offset + i * 4 + 1];
                int hx = coords[offset + i * 4 + 2];
                int hy = coords[offset + i * 4 + 3];
                coords[offset + i * 4 + 0] = -hx;
                coords[offset + i * 4 + 1] = -hy;
                coords[offset + i * 4 + 2] = -lx;
                coords[offset + i * 4 + 3] = -ly;
            }
        }

        public Point transformPoint(Point p) {
            return new Point(-p.getX(), -p.getY());
        }
    },
    R270 {

        public void transformPoints(long[] coords, int offset, int count) {
            for (int i = 0; i < count; i++) {
                long x = coords[offset + i * 2 + 0];
                long y = coords[offset + i * 2 + 1];
                coords[offset + i * 2 + 0] = y;
                coords[offset + i * 2 + 1] = -x;
            }
        }

        public void transformRects(int[] coords, int offset, int count) {
            for (int i = 0; i < count; i++) {
                int lx = coords[offset + i * 4 + 0];
                int ly = coords[offset + i * 4 + 1];
                int hx = coords[offset + i * 4 + 2];
                int hy = coords[offset + i * 4 + 3];
                coords[offset + i * 4 + 0] = ly;
                coords[offset + i * 4 + 1] = -hx;
                coords[offset + i * 4 + 2] = hy;
                coords[offset + i * 4 + 3] = -lx;
            }
        }

        public Point transformPoint(Point p) {
            return new Point(p.getY(), -p.getX());
        }
    },
    MY {

        public void transformPoints(long[] coords, int offset, int count) {
            for (int i = 0; i < count; i++) {
                long x = coords[offset + i * 2 + 0];
                coords[offset + i * 2 + 0] = -x;
            }
        }

        public void transformRects(int[] coords, int offset, int count) {
            for (int i = 0; i < count; i++) {
                int lx = coords[offset + i * 4 + 0];
                int hx = coords[offset + i * 4 + 2];
                coords[offset + i * 4 + 0] = -hx;
                coords[offset + i * 4 + 2] = -lx;
            }
        }

        public Point transformPoint(Point p) {
            return new Point(-p.getX(), p.getY());
        }
    },
    MYR90 {

        public void transformPoints(long[] coords, int offset, int count) {
            for (int i = 0; i < count; i++) {
                long x = coords[offset + i * 2 + 0];
                long y = coords[offset + i * 2 + 1];
                coords[offset + i * 2 + 0] = -y;
                coords[offset + i * 2 + 1] = -x;
            }
        }

        public void transformRects(int[] coords, int offset, int count) {
            for (int i = 0; i < count; i++) {
                int lx = coords[offset + i * 4 + 0];
                int ly = coords[offset + i * 4 + 1];
                int hx = coords[offset + i * 4 + 2];
                int hy = coords[offset + i * 4 + 3];
                coords[offset + i * 4 + 0] = -hy;
                coords[offset + i * 4 + 1] = -hx;
                coords[offset + i * 4 + 2] = -ly;
                coords[offset + i * 4 + 3] = -lx;
            }
        }

        public Point transformPoint(Point p) {
            return new Point(-p.getY(), -p.getX());
        }
    },
    MX {

        public void transformPoints(long[] coords, int offset, int count) {
            for (int i = 0; i < count; i++) {
                long y = coords[offset + i * 2 + 1];
                coords[offset + i * 2 + 1] = -y;
            }
        }

        public void transformRects(int[] coords, int offset, int count) {
            for (int i = 0; i < count; i++) {
                int ly = coords[offset + i * 4 + 1];
                int hy = coords[offset + i * 4 + 3];
                coords[offset + i * 4 + 1] = -hy;
                coords[offset + i * 4 + 3] = -ly;
            }
        }

        public Point transformPoint(Point p) {
            return new Point(p.getX(), -p.getY());
        }
    },
    MXR90 {

        public void transformPoints(long[] coords, int offset, int count) {
            for (int i = 0; i < count; i++) {
                long x = coords[offset + i * 2 + 0];
                long y = coords[offset + i * 2 + 1];
                coords[offset + i * 2 + 0] = y;
                coords[offset + i * 2 + 1] = x;
            }
        }

        public void transformRects(int[] coords, int offset, int count) {
            for (int i = 0; i < count; i++) {
                int lx = coords[offset + i * 4 + 0];
                int ly = coords[offset + i * 4 + 1];
                int hx = coords[offset + i * 4 + 2];
                int hy = coords[offset + i * 4 + 3];
                coords[offset + i * 4 + 0] = ly;
                coords[offset + i * 4 + 1] = lx;
                coords[offset + i * 4 + 2] = hy;
                coords[offset + i * 4 + 3] = hx;
            }
        }

        public Point transformPoint(Point p) {
            return new Point(p.getY(), p.getX());
        }
    };

    @Deprecated
    public abstract void transformPoints(long[] coords, int offset, int count);

    public abstract Point transformPoint(Point p);

    public abstract void transformRects(int[] coords, int offset, int count);

    public ManhattanOrientation concatenate(ManhattanOrientation other) {
        return concatenate[ordinal() * 8 + other.ordinal()];
    }

    public AffineTransform affineTransform() {
        return transforms[ordinal()];
    }
    public static final AffineTransform[] transforms = {
        /*R0*/new AffineTransform(1, 0, 0, 1, 0, 0),
        /*R90*/ new AffineTransform(0, 1, -1, 0, 0, 0),
        /*R180*/ new AffineTransform(-1, 0, 0, -1, 0, 0),
        /*R270*/ new AffineTransform(0, -1, 1, 0, 0, 0),
        /*MX*/ new AffineTransform(-1, 0, 0, 1, 0, 0),
        /*MXR90*/ new AffineTransform(0, -1, -1, 0, 0, 0),
        /*MY*/ new AffineTransform(1, 0, 0, -1, 0, 0),
        /*MYR90*/ new AffineTransform(0, 1, 1, 0, 0, 0)
    };
    private static final ManhattanOrientation[] concatenate = {
        /*R0*/R0, R90, R180, R270, MY, MYR90, MX, MXR90,
        /*R90*/ R90, R180, R270, R0, MYR90, MX, MXR90, MY,
        /*R180*/ R180, R270, R0, R90, MX, MXR90, MY, MYR90,
        /*R270*/ R270, R0, R90, R180, MXR90, MY, MYR90, MX,
        /*MY*/ MY, MXR90, MX, MYR90, R0, R270, R180, R90,
        /*MYR90*/ MYR90, MY, MXR90, MX, R90, R0, R270, R180,
        /*MX*/ MX, MYR90, MY, MXR90, R180, R90, R0, R270,
        /*MXR90*/ MXR90, MX, MYR90, MY, R270, R180, R90, R0
    };
}
