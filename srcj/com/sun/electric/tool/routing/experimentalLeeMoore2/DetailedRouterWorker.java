/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DetailedRouterWorker.java
 * Written by: Alexander Herzog, Martin Fietz (Team 4)
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.routing.experimentalLeeMoore2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sun.electric.tool.routing.RoutingFrame.RoutingLayer;
import com.sun.electric.tool.routing.experimentalLeeMoore2.DetailedRouter.DetailedRoutingSolution;
import com.sun.electric.tool.routing.experimentalLeeMoore2.GlobalRouterV3.RegionToRoute;
import com.sun.electric.tool.routing.experimentalLeeMoore2.RoutingFrameLeeMoore.Coordinate;
import com.sun.electric.tool.routing.experimentalLeeMoore2.RoutingFrameLeeMoore.ManhattenAlignment;

public final class DetailedRouterWorker implements Runnable {

	/** field */
	private MazeField mf;
	private final double offsetX;
	private final double offsetY;
	private final int offsetZ;
	private final int lengthX;
	private final int lengthY;
	private final int lengthZ;

	private static boolean enableOutput = false;

	final static int HORIZONTAL = 1;
	final static int VERTICAL = 0;

	private RegionToRoute region;
	private final DetailedRoutingSolution allSolutions;
	private final DetailedRoutingSolution curSolutions;

	private final double tileSize;
	private double wireSpacing;

	private boolean isDone;

	private static ManhattenAlignment aStart;
	private static ManhattenAlignment aFinish;

	public DetailedRouterWorker(RegionToRoute region,
			RoutingLayer[] metalLayers, double tileSize) {
		this.tileSize = tileSize;
		wireSpacing = Double.MIN_VALUE;
		for (RoutingLayer lay : metalLayers) {
			if (null == lay) {
				continue;
			}
			wireSpacing = Math.max(lay.getMinSpacing(lay), wireSpacing);
		}
		offsetX = region.bounds.getMinX();
		offsetY = region.bounds.getMinY();
		offsetZ = 1;
		lengthX = (int) Math.round(region.bounds.getWidth() / tileSize) + 1;
		lengthY = (int) Math.round(region.bounds.getHeight() / tileSize) + 1;
		lengthZ = metalLayers.length - 1;
		allSolutions = new DetailedRoutingSolution();
		curSolutions = new DetailedRoutingSolution();
	}

	public void setRegion(RegionToRoute region) {
		this.region = region;
	}

	public void run() {
		isDone = false;
		curSolutions.clear();
		List<SegPart> unrouted = region.segments_to_route;
		if (unrouted.size() > 0) {
			mf = new MazeField(lengthX, lengthY, lengthZ);
			mf.resetAll();

			for (List<Coordinate> solution : allSolutions.values()) {
				//mf.addBlockage(toMazePoint(solution));
				mf.addBlockage(getPointsToBlock(toMazePoint(solution)));
			}

			// reserve start- and end-point tiles
			for (SegPart segs : unrouted) {
				MazePoint start = toMazePoint(segs.segment_part.get(0));
				MazePoint finish = toMazePoint(segs.segment_part.get(1));
				if (mf.canBeReserved(start) && mf.canBeReserved(finish)) {
					mf.reserve(start);
					mf.reserve(finish);
				}
			}

			for (SegPart sp : unrouted) {
				mf.reset();
				MazePoint start = toMazePoint(sp.segment_part.get(0));
				aStart = sp.segment_part.get(0).alignment;
				MazePoint finish = toMazePoint(sp.segment_part.get(1));
				aFinish = sp.segment_part.get(1).alignment;
				if (mf.getValue(start) == MazeField.POINT_RESERVED
						&& mf.getValue(finish) == MazeField.POINT_RESERVED
						&& mf.setStart(start) && mf.setFinish(finish)) {
					if (mf.wavefront()) {
						debug("1");
						List<MazePoint> ws = mf.backtracking();
						//mf.addBlockage(ws);
						mf.addBlockage(getPointsToBlock(ws));
						curSolutions.put(sp, toCoordinate((ws)));
					} else {
						debug("0");
					}
				} else {
					debug("?");
				}
			}
		}
		mf = null; // let the garbage collector do its magic
		isDone = true;
	}
	
	private List<MazePoint> getPointsToBlock( List<MazePoint> lmp ) {
		List<MazePoint> result = new ArrayList<MazePoint>();
		for( MazePoint mp : lmp ) {
			result.add(mp);
		}
		
		if( lmp.size() > 0 && mf.contains(lmp.get(0)) ) {
			result.add(lmp.get(0));
		}
		if( lmp.size() > 0 && mf.contains(lmp.get(lmp.size() - 1)) ) {
			result.add(lmp.get(lmp.size() - 1));
		}
		return result;
	}

	private static void debug(String s) {
		if (enableOutput) {
			System.out.print(s);
		}
	}

	public void enableOutput() {
		enableOutput = true;
	}

	public DetailedRoutingSolution getSolution() {
		return this.curSolutions;
	}

	public void removeSolutions(List<Integer> ids) {
		for (Iterator<Map.Entry<SegPart, List<Coordinate>>> it = curSolutions
				.entrySet().iterator(); it.hasNext();) {
			Map.Entry<SegPart, List<Coordinate>> s = it.next();
			if (ids.contains(s.getKey().id)) {
				it.remove();
			} else {
				allSolutions.put(s.getKey(), s.getValue());
			}
		}
	}

	public boolean isDone() {
		return isDone;
	}

	private List<MazePoint> toMazePoint(final List<Coordinate> lc) {
		List<MazePoint> lm = new ArrayList<MazePoint>(lc.size());
		for (Coordinate c : lc)
			lm.add(toMazePoint(c));
		return lm;
	}

	private MazePoint toMazePoint(Coordinate c) {
		int x = (int) Math.floor((c.x - offsetX + 0.5 * tileSize) / tileSize);
		int y = (int) Math.floor((c.y - offsetY + 0.5 * tileSize) / tileSize);
		int z = c.layer - offsetZ;
		return new MazePoint(x, y, z);
	}

	private List<Coordinate> toCoordinate(final List<MazePoint> lm) {
		List<Coordinate> result = new ArrayList<Coordinate>(lm.size());
		for (MazePoint mp : lm) {
			double x = mp.x * tileSize + offsetX;
			double y = mp.y * tileSize + offsetY;
			int z = mp.z + offsetZ;
			result.add(new Coordinate(x, y, z));
		}
		return result;
	}

	public final static class MazePoint {
		public final int x, y, z;

		public MazePoint(int i, int j, int z) {
			this.x = i;
			this.y = j;
			this.z = z;
		}
	}

	public final static class MazeField {
		/** POINT VALUES */
		private final static int POINT_UNDEFINED = 0;
		private final static int POINT_START = -1;
		private final static int POINT_FINISH = -2;
		private final static int POINT_BLOCK = -4;
		private final static int POINT_BORDER = -8;
		private final static int POINT_RESERVED = -16;

		private final int[][][] field;

		private MazePoint start;
		private MazePoint finish;

		public MazeField(int sizeX, int sizeY, int sizeZ) {
			field = new int[sizeX][sizeY][sizeZ];
		}

		public MazeField reset() {
			for (int x = 0; x < field.length; x++) {
				for (int y = 0; y < field[x].length; y++) {
					for (int z = 0; z < field[x][y].length; z++) {
						if (getValue(x, y, z) != POINT_BLOCK
								&& getValue(x, y, z) != POINT_BORDER
								&& getValue(x, y, z) != POINT_RESERVED) {
							setValue(x, y, z, POINT_UNDEFINED);
						}
					}
				}
			}
			if (start != null && getValue(start) != POINT_BLOCK) {
				setValue(start, POINT_BORDER);
			}
			if (finish != null && getValue(finish) != POINT_BLOCK) {
				setValue(finish, POINT_BORDER);
			}
			return this;
		}

		public MazeField resetAll() {
			for (int x = 0; x < field.length; x++) {
				for (int y = 0; y < field[x].length; y++) {
					for (int z = 0; z < field[x][y].length; z++) {
						if (x == 0 || x == field.length - 1 || y == 0
								|| y == field[x].length - 1) {
							setValue(x, y, z, POINT_BORDER);
						} else {
							setValue(x, y, z, POINT_UNDEFINED);
						}
					}
				}
			}
			return this;
		}

		public boolean setStart(MazePoint mp) {
			if (contains(mp)
					&& (getValue(mp) == POINT_BORDER
							|| getValue(mp) == POINT_UNDEFINED || getValue(mp) == POINT_RESERVED)) {
				start = mp;
				setValue(mp, POINT_START);
				return true;
			} else {
				return false;
			}
		}

		public boolean reserve(MazePoint mp) {
			if (contains(mp)
					&& (getValue(mp) == POINT_BORDER || getValue(mp) == POINT_UNDEFINED)) {
				setValue(mp, POINT_RESERVED );
				return true;
			} else {
				return false;
			}
		}

		public boolean canBeReserved(MazePoint mp) {
			if (contains(mp)) {
				return (getValue(mp) == POINT_BORDER || getValue(mp) == POINT_UNDEFINED);
			} else {
				return false;
			}
		}

		public boolean setFinish(MazePoint mp) {
			if (contains(mp)
					&& (getValue(mp) == POINT_BORDER
							|| getValue(mp) == POINT_UNDEFINED || getValue(mp) == POINT_RESERVED)) {
				finish = mp;
				setValue(mp, POINT_FINISH);
				return true;
			} else {
				return false;
			}
		}

		public void addBlockage(List<MazePoint> blockList) {
			for (MazePoint block : blockList)
				addBlockage(block);
		}

		public void addBlockage(MazePoint mp) {
			addBlockage(mp.x, mp.y, mp.z);
		}

		public void addBlockage(int x, int y, int z) {
			setValue(x, y, z, POINT_BLOCK);
		}

		public void setValue(MazePoint mp, int i) {
			setValue(mp.x, mp.y, mp.z, i);
		}

		public void setValue(int x, int y, int z, int value) {
			if (contains(x, y, z)) {
				field[x][y][z] = value;
			}
		}

		public int getValue(final MazePoint mp) {
			return getValue(mp.x, mp.y, mp.z);
		}

		public int getDistance(MazePoint m, MazePoint n) {
			return Math.abs(m.x - n.x) + Math.abs(m.y - n.y)
					+ Math.abs(m.z - n.z);
		}

		public int getValue(int x, int y, int z) {
			if (contains(x, y, z)) {
				return field[x][y][z];
			} else {
				return POINT_BLOCK;
			}
		}

		public List<MazePoint> getNeighbours(MazePoint mp) {
			return getNeighbours(mp.x, mp.y, mp.z);
		}

		public List<MazePoint> getNeighbours(int x, int y, int z) {
			ArrayList<MazePoint> neighbours = new ArrayList<MazePoint>();
			/*int[] dx, dy;
			if ((z + 1) % 2 == 0) // even layer
			{
				dx = new int[] { 0, 0, 0, 0 };
				dy = new int[] { -1, 1, 0, 0 };
			} else // odd layer
			{
				dx = new int[] { -1, 1, 0, 0 };
				dy = new int[] { 0, 0, 0, 0 };
			}
			int[] dz = { 0, 0, -1, 1 }; */
			int dx[] = { -1, 1, 0, 0, 0, 0 };
			int dy[] = { 0, 0, -1, 1, 0, 0 };
			int dz[] = { 0, 0, 0, 0, -1, 1 };
			for (int i = 0; i < dx.length; i++) {
				MazePoint n = new MazePoint(x + dx[i], y + dy[i], z + dz[i]);
				if (contains(n)) {
					neighbours.add(n);
				}
			}
			return neighbours;
		}

		private boolean contains(MazePoint mp) {
			return contains(mp.x, mp.y, mp.z);
		}

		private boolean contains(int x, int y, int z) {
			if (0 <= x && x < field.length && 0 <= y && y < field[x].length
					&& 0 <= z && z < field[x][y].length)
				return true;
			else
				return false;
		}

		public boolean wavefront() {
			/** add first element to queue */
			MazeQueue queue = new MazeQueue();
			queue.add(getDistance(start, finish), start);
			/**
			 * go on while there are elements in queue or return when finish
			 * reached
			 */
			while (queue.size() > 0) {
				/** get next element to work with */
				MazePoint curP = queue.removeNext();
				for (MazePoint nextP : getNeighbours(curP)) {
					int nextValue = getValue(curP) + 1 /*+ Math.abs(nextP.x 
							- field.length / 2) + Math.abs(nextP.y - field[0].length / 2)*/;
					if (getValue(curP) == POINT_START) {
						nextValue = 1;
					}
					switch (getValue(nextP)) {
					case POINT_START:
					case POINT_BLOCK:
					case POINT_BORDER:
					case POINT_RESERVED:
						break;
					case POINT_FINISH:
						return true;
					case POINT_UNDEFINED:
						setValue(nextP, nextValue);
						queue.add(getDistance(nextP, finish), nextP);
						break;
					default: // we have already visited this point
						if (nextValue < getValue(nextP)) {
							setValue(nextP, nextValue);
							queue.add(getDistance(nextP, finish), nextP);
						}
						break;
					}
				}
			}
			// debugPrintField();
			// wavefront could not reach any finish point
			return false;
		}

		/** second part of algorithm */
		public List<MazePoint> backtracking() {
			List<MazePoint> route = new ArrayList<MazePoint>();

			/** add finish to route */
			route.add(new MazePoint(finish.x, finish.y, finish.z));

			/** while start not reached */
			MazePoint curPoint = finish;
			int curValue = Integer.MAX_VALUE;
			while (curValue != POINT_START) {
				MazePoint nextPoint = curPoint;
				List<MazePoint> neighbours = getNeighbours(nextPoint);
				setValue( curPoint, POINT_BLOCK );
				for (MazePoint neighbour : neighbours) {
					int value = getValue(neighbour);
					switch (value) {
					case POINT_UNDEFINED:
					case POINT_BLOCK:
					case POINT_BORDER:
					case POINT_FINISH:
					case POINT_RESERVED:
						continue;
					default:
						if (value < curValue) {
							nextPoint = neighbour;
						}
						break;
					}
				}
				if (nextPoint == curPoint) {
					debug("oops, this should never ever happen!\n");
				} else {
					curPoint = nextPoint;
					curValue = getValue(nextPoint);
					route.add(nextPoint);
				}
			}
			Collections.reverse(route);
			return route;
		}

		/** for debugging */
		public void debugPrintField() {
			debug("\nstart: ");
			printMazePoint(start);
			if (aStart == ManhattenAlignment.ma_horizontal)
				debug(" h");
			if (aStart == ManhattenAlignment.ma_vertical)
				debug(" v");
			if (aStart == ManhattenAlignment.ma_undefined)
				debug(" u");
			debug("\n");
			debug("finish: ");
			printMazePoint(finish);
			if (aFinish == ManhattenAlignment.ma_horizontal)
				debug(" h");
			if (aFinish == ManhattenAlignment.ma_vertical)
				debug(" v");
			if (aFinish == ManhattenAlignment.ma_undefined)
				debug(" u");
			debug("\n");
			for (int z = 0; z < field[0][0].length; z++) {
				debug("z: " + z + " "
						+ ((z + 1) % 2 == 0 ? "vertical" : "horizontal") + "\n");
				for (int y = 0; y < field[0].length; y++) {
					for (int x = 0; x < field.length; x++) {
						switch (getValue(x, y, z)) {
						case POINT_START:
							debug("=S=");
							break;
						case POINT_FINISH:
							debug("=F=");
							break;
						case POINT_BORDER:
							debug(" B ");
							break;
						case POINT_BLOCK:
							debug(" L ");
							break;
						case POINT_RESERVED:
							debug(" R ");
							break;
						default:
							int value = getValue(x, y, z);
							debug((value < 10 ? " " : "") + value + " ");
						}
					}
					debug("\n");
				}
			}
		}

		public static void debugPrintMazePointList(List<MazePoint> points) {
			for (MazePoint mp : points)
				printMazePoint(mp);
			debug(".\n");
		}

		public static void printMazePoint(MazePoint mp) {
			debug("(" + mp.x + "," + mp.y + "," + mp.z + ") ");
		}

		private static void debug(String s) {
			if (enableOutput) {
				System.out.print(s);
			}
		}

		final static class MazeQueue extends TreeMap<Integer, List<MazePoint>> {

			private static final long serialVersionUID = -9114852150292907187L;

			public void add(int distance, MazePoint mp) {
				if (!this.containsKey(distance)) {
					this.put(distance, new ArrayList<MazePoint>());
				}
				this.get(distance).add(mp);
			}

			public MazePoint removeNext() {
				while (this.get(this.firstKey()).size() == 0) {
					this.remove(this.firstKey());
				}
				List<MazePoint> next = this.get(this.firstKey());
				MazePoint result = next.remove(0);
				if (next.size() == 0) {
					this.remove(this.firstKey());
				}
				return result;
			}
		}
	}
}