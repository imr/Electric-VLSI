/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PrimitivePort.java
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
package com.sun.electric.technology;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.technology.technologies.Generic;

import java.awt.Color;

/**
 * A PrimitivePort lives in a PrimitiveNode in a Tecnology.
 * It contains a list of ArcProto types that it
 * accepts connections from.
 */
public class PrimitivePort extends PortProto
{
	// ---------------------------- private data --------------------------
	private ArcProto portArcs[]; // Immutable list of possible connection types.
	private EdgeH left;
	private EdgeV bottom;
	private EdgeH right;
	private EdgeV top;
	private Technology tech;
	private PortProto.Characteristic characteristic;

	// ---------------------- protected and private methods ----------------

	/**
	 * The constructor is only called from the factory method "newInstance".
	 */
	private PrimitivePort(Technology tech, PrimitiveNode parent, ArcProto [] portArcs, String protoName,
		int portAngle, int portRange, int portTopology, EdgeH left, EdgeV bottom, EdgeH right, EdgeV top)
	{
		// initialize the parent object
		super();
		this.parent = parent;
		setProtoName(protoName);
		setAngle(portAngle);
		setAngleRange(portRange);
		setTopology(portTopology);

		// initialize this object
		this.tech = tech;
		this.portArcs = portArcs;
		this.left = left;
		this.bottom = bottom;
		this.right = right;
		this.top = top;
        tech.addPortProto(this);
	}

	/**
	 * Method to create a new PrimitivePort from the parameters.
	 * @param tech the Technology in which this PrimitivePort is being created.
	 * @param parent the PrimitiveNode on which this PrimitivePort resides.
	 * @param portArcs an array of ArcProtos which can connect to this PrimitivePort.
	 * @param protoName the name of this PrimitivePort.
	 * It may not have unprintable characters, spaces, or tabs in it.
	 * @param portAngle the primary angle that the PrimitivePort faces on the PrimitiveNode (in degrees).
	 * This angle is measured counter-clockwise from a right-pointing direction.
	 * @param portRange the range about the angle of allowable connections (in degrees).
	 * Arcs must connect to this port within this many degrees of the port connection angle.
	 * When this value is 180, then all angles are permissible, since arcs
	 * can connect at up to 180 degrees in either direction from the port angle.
	 * @param portTopology is a small integer that is unique among PrimitivePorts on the PrimitiveNode.
	 * When two PrimitivePorts have the same topology number, it indicates that these ports are connected.
	 * @param characteristic describes the nature of this PrimitivePort (input, output, power, ground, etc.)
	 * @param left is an EdgeH that describes the left side of the port in a scalable way.
	 * @param bottom is an EdgeV that describes the bottom side of the port in a scalable way.
	 * @param right is an EdgeH that describes the right side of the port in a scalable way.
	 * @param top is an EdgeV that describes the top side of the port in a scalable way.
	 * @return the newly created PrimitivePort.
	 */
	public static PrimitivePort newInstance(Technology tech, PrimitiveNode parent, ArcProto [] portArcs, String protoName,
		int portAngle, int portRange, int portTopology, PortProto.Characteristic characteristic,
		EdgeH left, EdgeV bottom, EdgeH right, EdgeV top)
	{
		if (parent == null)
			System.out.println("PrimitivePort " + protoName + " has no parent");
		if (tech != Generic.tech && Generic.tech != null)
		{
			ArcProto [] realPortArcs = new ArcProto[portArcs.length + 3];
			for(int i=0; i<portArcs.length; i++)
				realPortArcs[i] = portArcs[i];
			realPortArcs[portArcs.length] = Generic.tech.universal_arc;
			realPortArcs[portArcs.length+1] = Generic.tech.invisible_arc;
			realPortArcs[portArcs.length+2] = Generic.tech.unrouted_arc;
			portArcs = realPortArcs;
		}
		PrimitivePort pp = new PrimitivePort(tech, parent, portArcs, protoName,
			portAngle, portRange, portTopology, left, bottom, right, top);
		pp.setCharacteristic(characteristic);
		return pp;
	}

	/*
	 * Method to write a description of this PrimitivePort.
	 * Displays the description in the Messages Window.
	 */
	public void getInfo()
	{
		System.out.println(" Connection types: " + portArcs.length);
		for (int i = 0; i < portArcs.length; i++)
		{
			System.out.println("   * " + portArcs[i]);
		}
		super.getInfo();
	}

	// ------------------------ public methods ------------------------

	/**
	 * Method to set the list of allowable connections on this PrimitivePort.
	 * @param portArcs an array of ArcProtos which can connect to this PrimitivePort.
	 */
	public void setConnections(ArcProto [] portArcs) { this.portArcs = portArcs; }

	/**
	 * Method to return the list of allowable connections on this PrimitivePort.
	 * @return an array of ArcProtos which can connect to this PrimitivePort.
	 */
	public ArcProto [] getConnections() { return portArcs; }

	/**
	 * Method to return the base-level port that this PortProto is created from.
	 * Since it is a PrimitivePort, it simply returns itself.
	 * @return the base-level port that this PortProto is created from (this).
	 */
	public PrimitivePort getBasePort() { return this; }

	/**
	 * Method to return the left edge of the PrimitivePort as a value that scales with the actual NodeInst.
	 * @return an EdgeH object that describes the left edge of the PrimitivePort.
	 */
	public EdgeH getLeft() { return left; }

	/**
	 * Method to return the right edge of the PrimitivePort as a value that scales with the actual NodeInst.
	 * @return an EdgeH object that describes the right edge of the PrimitivePort.
	 */
	public EdgeH getRight() { return right; }

	/**
	 * Method to return the top edge of the PrimitivePort as a value that scales with the actual NodeInst.
	 * @return an EdgeV object that describes the top edge of the PrimitivePort.
	 */
	public EdgeV getTop() { return top; }

	/**
	 * Method to return the bottom edge of the PrimitivePort as a value that scales with the actual NodeInst.
	 * @return an EdgeV object that describes the bottom edge of the PrimitivePort.
	 */
	public EdgeV getBottom() { return bottom; }

	/**
	 * Method to return true if this PrimitivePort can connect to an arc of a given type.
	 * @param arc the ArcProto to test for connectivity.
	 * @return true if this PrimitivePort can connect to the arc, false if it can't
	 */
	public boolean connectsTo(ArcProto arc)
	{
		for (int i = 0; i < portArcs.length; i++)
		{
			if (portArcs[i] == arc)
				return true;
		}
		return false;
	}

	/**
	 * Method to compute the color of this PrimitivePort.
	 * Combines all arcs that can connect.
	 * @return the color to use for this PrimitivePort.
	 */
	public Color getPortColor()
	{
		Technology tech = getParent().getTechnology();
		int numColors = 0;
		int r=0, g=0, b=0;
		for (int i = 0; i < portArcs.length; i++)
		{
			PrimitiveArc ap = (PrimitiveArc)portArcs[i];

			// ignore the generic arcs
			if (ap.getTechnology() != tech) continue;

			// get the arc's color
			Technology.ArcLayer [] layers = ap.getLayers();
			Layer layer = layers[0].getLayer();
			EGraphics graphics = layer.getGraphics();
			Color layerCol = graphics.getColor();
			r += layerCol.getRed();
			g += layerCol.getGreen();
			b += layerCol.getBlue();
			numColors++;
		}
		if (numColors == 0) return null;
		return new Color((int)(r/numColors), (int)(g/numColors), (int)(b/numColors));
	}

	/**
	 * Returns a printable version of this PrimitivePort.
	 * @return a printable version of this PrimitivePort.
	 */
	public String toString()
	{
		return "PrimitivePort " + getProtoName();
	}
}
