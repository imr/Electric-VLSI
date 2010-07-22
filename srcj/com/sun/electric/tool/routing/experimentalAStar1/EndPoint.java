/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EndPoint.java
 * Written by: Christian Julg, Jonas Thedering (Team 1)
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
package com.sun.electric.tool.routing.experimentalAStar1;

/** 
 * Helper class for EndPointMarker which wraps a start/finish end point
 * 
 * @author Christian Jülg
 * @author Jonas Thedering
 */
public class EndPoint {
	Path path;
	Net net;
	
	int type;
	public static final int START = -1;
	public static final int FINISH = -2;
	
	/**
	 * @param path
	 * @param net
	 * @param pathIndexInNet
	 * @param x
	 * @param y
	 */
	public EndPoint(Path path, Net net, int type) {
		super();
		this.path = path;
		this.net = net;
		this.type = type;
	}
	
	int[] possibleLayers;
	int preferredLayer;
	
	int fullyMarkedCount;
	int[] fullyMarkedLayers;
	
	int markingIndex = 0;

	/**
	 * set the associated path's start/finish layers
	 */
	public void setZ() {
		if (type == START) {
			path.startZ = new int[fullyMarkedCount];
			System.arraycopy(fullyMarkedLayers, 0, path.startZ, 0, fullyMarkedCount);
		} else if (type == FINISH) {
			path.finishZ = new int[fullyMarkedCount];
			System.arraycopy(fullyMarkedLayers, 0, path.finishZ, 0, fullyMarkedCount);
		} else {
			assert false : "Endpoint has illegal type "+type;
		}
		
		path.updateBoundingBox();
	}
	
	public int getX() {
		if (type == START) {
			return path.startX;
		} else if (type == FINISH) {
			return path.finishX;
		} else {
			assert false : "Endpoint has illegal type "+type;
			return -1;
		}
	}
	
	public int getY() {
		if (type == START) {
			return path.startY;
		} else if (type == FINISH) {
			return path.finishY;
		} else {
			assert false : "Endpoint has illegal type "+type;
			return -1;
		}
	}
	
	public boolean isRight() {
		if (type == START) {
			return path.startRight;
		} else if (type == FINISH) {
			return path.finishRight;
		} else {
			assert false : "Endpoint has illegal type "+type;
			return false;
		}
	}
	
	public boolean isAbove() {
		if (type == START) {
			return path.startAbove;
		} else if (type == FINISH) {
			return path.finishAbove;
		} else {
			assert false : "Endpoint has illegal type "+type;
			return false;
		}
	}
	
	/** Sorts the given layer to the front of possible layers */
	public void preferLayer(int layer) {
		int temp = possibleLayers[0];
		possibleLayers[0] = layer;
		
		for(int i=1; i < possibleLayers.length; ++i) {
			if(possibleLayers[i] == layer) {
				possibleLayers[i] = temp;
				break;
			}
		}
	}
}
