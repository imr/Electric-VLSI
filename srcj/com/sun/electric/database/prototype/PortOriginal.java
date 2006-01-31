/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PortOriginal.java
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
package com.sun.electric.database.prototype;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.PrimitivePort;

import java.awt.geom.AffineTransform;

/**
 * The PortOriginal helper class descends the hierarchy from an Export to the bottommost PrimitivePort
 * on a primitive NodeInst.
 * <P>
 * Many parts of the system need to know what is "under" an Export.
 * For example, assume that cell BOT has a contact node that is exported.
 * Further assume that cell MID has an instance of cell BOT, and that the
 * port on BOT is further exported.
 * Finally, assume that cell TOP has an instance of cell MID.
 * To find out information about the port on MID, it is necessary to descend
 * the hierarchy into MID and into BOT.
 * <P>
 * This class takes a PortInst or a NodeInst/PortProto pair (such as the port on MID in cell TOP).
 * It then provides these pieces of information:
 * <UL>
 * <LI>The bottommost PortInst (in this case, the port on the contact node in cell BOT).</LI>
 * <LI>The bottommost NodeInst (in this case, the contact node in cell BOT).</LI>
 * <LI>The bottommost PortProto (in this case, the port on the contact node in cell BOT).</LI>
 * <LI>The transformation to the top.  In this case, a transformation that:
 *     (1) accounts for rotation of the contact node;
 *     (2) accounts for translation between cell BOT and cell MID;
 *     (3) accounts for rotation on the instance of cell BOT in cell MID;
 *     (4) accounts for translation between cell MID and cell TOP; and
 *     (5) accounts for rotation of the instance of cell MID in cell TOP.
 * Thus, it transforms from coordinates on the contact node to the coordinate space of cell TOP.</LI>
 * <LI>The apparent angle of the lowest node when viewed from the top.
 * In this case, it is a combination of the contact, BOT cell instance, and MID cell instance rotations.</LI>
 */
public class PortOriginal
{
	private AffineTransform subrot;
	private Orientation orient;
	private PortInst bottomPort;
	private NodeInst bottomNi;
	private PortProto bottomPp;

	/**
	 * Constructor takes a PortInst and traverses it down to the bottom of the hierarchy.
	 * @param startPort the initial PortInst.
	 */
	public PortOriginal(PortInst startPort)
	{
		bottomPort = startPort;
		bottomNi = bottomPort.getNodeInst();
		bottomPp = bottomPort.getPortProto();
		subrot = bottomNi.rotateOut();
		traverse();
	}

	/**
	 * Constructor takes a PortInst and traverses it down to the bottom of the hierarchy.
	 * Also takes a transformation matrix to include in the final computation.
	 * @param startPort the initial PortInst.
	 * @param pre the transformation matrix to add to the final transformation.
	 */
	public PortOriginal(PortInst startPort, AffineTransform pre)
	{
		bottomPort = startPort;
		bottomNi = bottomPort.getNodeInst();
		bottomPp = bottomPort.getPortProto();
		subrot = bottomNi.rotateOut(pre);
		traverse();
	}

	/**
	 * Constructor takes a NodeInst/PortProto combination and traverses it down to the bottom of the hierarchy.
	 * @param ni the initial NodeInst.
	 * @param pp the initial PortProto.
	 */
	public PortOriginal(NodeInst ni, PortProto pp)
	{
		bottomPort = null;
		bottomNi = ni;
		bottomPp = pp;
		subrot = bottomNi.rotateOut();
		traverse();
	}

	private void traverse()
	{
        orient = bottomNi.getOrient();
		while (bottomNi.isCellInstance())
		{
			subrot = bottomNi.translateOut(subrot);
			bottomPort = ((Export)bottomPp).getOriginalPort();
			bottomNi = bottomPort.getNodeInst();
			bottomPp = bottomPort.getPortProto();
			subrot = bottomNi.rotateOut(subrot);
            orient = orient.concatenate(bottomNi.getOrient());
		}
	}

	/**
	 * Method to return the bottommost NodeInst (a primitive)
	 * from the initial port information given to the constructor.
	 * @return the NodeInst at the bottom of the hierarchy (a primitive).
	 */
	public NodeInst getBottomNodeInst() { return bottomNi; }

	/**
	 * Method to return the bottommost PortProto (a PrimitivePort)
	 * from the initial port information given to the constructor.
	 * @return the PortProto at the bottom of the hierarchy (a PrimitivePort).
	 */
	public PrimitivePort getBottomPortProto() { return (PrimitivePort)bottomPp; }

	/**
	 * Method to return the bottommost PortInst (on a primitive NodeInst)
	 * from the initial port information given to the constructor.
	 * @return the PortInst at the bottom of the hierarchy (on a primitive NodeInst).
	 */
	public PortInst getBottomPort()
	{
		if (bottomPort == null) bottomPort = bottomNi.findPortInstFromProto(bottomPp);			
		return bottomPort;
	}

	/**
	 * Method to return the transformation matrix from the bottommost NodeInst
	 * to the Cell containing the topmost PortInst.
	 * The transformation includes any rotation on the node with the topmost PortInst.
	 * @return the the transformation matrix from the bottommost NodeInst
	 * to the Cell containing the topmost PortInst.
	 */
	public AffineTransform getTransformToTop() { return subrot; }

	/**
	 * Method to return the apparent orientation of the lowest node when viewed from the top.
	 * The angle can be used to replicate a node at the top level that is in the same
	 * orientation of the node at the bottom.
	 * @return the apparent orientation of the lowest node when viewed from the top.
	 */
	public Orientation getOrientToTop() { return orient; }
}
