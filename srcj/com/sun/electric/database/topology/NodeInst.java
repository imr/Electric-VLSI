/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NodeInst.java
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
package com.sun.electric.database.topology;

import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortOriginal;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.ImmutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.*;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A NodeInst is an instance of a NodeProto (a PrimitiveNode or a Cell).
 * A NodeInst points to its prototype and the Cell in which it has been
 * instantiated.  It also has a name, and contains a list of Connections
 * and Exports.
 * <P>
 * The rotation and transposition of a NodeInst can be confusing, so it is illustrated here.
 * Nodes are transposed when one of their scale factors is negative.
 * <P>
 * <CENTER><IMG SRC="doc-files/NodeInst-1.gif"></CENTER>
 */
public class NodeInst extends Geometric implements Nodable, Comparable
{
	/** special name for text descriptor of prototype name */	public static final String NODE_PROTO_TD = new String("NODE_proto");
	/** special name for text descriptor of instance name */	public static final String NODE_NAME_TD = new String("NODE_name");

	/** key of obsolete Variable holding instance name. */		public static final Variable.Key NODE_NAME = ElectricObject.newKey("NODE_name");
	/** key of Varible holding outline information. */			public static final Variable.Key TRACE = ElectricObject.newKey("trace");
	/** key of Varible holding serpentine transistor length. */	private static final Variable.Key TRANSISTOR_LENGTH_KEY = ElectricObject.newKey("transistor_width");

	private static final PortInst[] NULL_PORT_INST_ARRAY = new PortInst[0];
	private static final Export[] NULL_EXPORT_ARRAY = new Export[0];

	/**
	 * Method to detect if np is not relevant for some tool calculation and therefore
	 * could be skip. E.g. cellCenter, drcNodes, essential bounds and pins in DRC.
	 * Similar for layer generation
	 * @param ni the NodeInst in question.
	 * @return true if it is a special node (cell center, etc.)
	 */
	public static boolean isSpecialNode(NodeInst ni)
	{
		NodeProto np = ni.getProto();
		return (np == Generic.tech.cellCenterNode || np == Generic.tech.drcNode ||
		        np == Generic.tech.essentialBoundsNode || np.getFunction() == PrimitiveNode.Function.PIN ||
		        np.getFunction() == PrimitiveNode.Function.CONNECT);
	}

	/**
	 * the PortAssociation class is used when replacing nodes.
	 */
	private static class PortAssociation
	{
		/** the original PortInst being associated. */			PortInst portInst;
		/** the Poly that describes the original PortInst. */	Poly poly;
		/** the center point in the original PortInst. */		Point2D pos;
		/** the associated PortInst on the new NodeInst. */		PortInst assn;
	}

	// ---------------------- private data ----------------------------------
    /** persistent data of this NodeInst. */                private ImmutableNodeInst d;
	/** prototype of this NodeInst. */						private NodeProto protoType;
	/** node usage of this NodeInst. */						private NodeUsage nodeUsage;
	/** 0-based index of this NodeInst in Cell. */			private int nodeIndex = -1;
	/** Array of PortInsts on this NodeInst. */				private PortInst[] portInsts = NULL_PORT_INST_ARRAY;
	/** List of connections belonging to this NodeInst. */	private List connections = new ArrayList(2);
	/** Array of Exports belonging to this NodeInst. */		private Export[] exports = NULL_EXPORT_ARRAY;
    
	/** bounds after transformation. */						private Rectangle2D visBounds = new Rectangle2D.Double(0, 0, 0, 0);
	/** The timestamp for changes. */						private int changeClock;
	/** The Change object. */								private Undo.Change change;

    
    // --------------------- private and protected methods ---------------------

	/**
	 * The protected constructor of NodeInst. Use the factory "newInstance" instead.
	 * @param parent the Cell in which this NodeInst will reside.
	 * @param protoType the NodeProto of which this is an instance.
	 * @param name name of new NodeInst
	 * @param duplicate duplicate index of this NodeInst
     * @param nameDescriptor TextDescriptor of name of this NodeInst
	 * @param center the center location of this NodeInst.
	 * @param width the width of this NodeInst.
	 * If negative, flip the X coordinate (or flip ABOUT the Y axis).
	 * @param height the height of this NodeInst.
	 * If negative, flip the Y coordinate (or flip ABOUT the X axis).
	 * @param angle the angle of this NodeInst (in tenth-degrees).
	 * @param userBits flag bits of this NodeInst.
     * @param protoDescriptor TextDescriptor of prototype name of this NodeInst
	 */
    protected NodeInst(Cell parent, NodeProto protoType,
            Name name, int duplicate, ImmutableTextDescriptor nameDescriptor,
            Point2D center, double width, double height, int angle,
            int userBits, ImmutableTextDescriptor protoDescriptor)
	{
		// initialize this object
		this.parent = parent;
		this.protoType = protoType;
        
		// create all of the portInsts on this node inst
		portInsts = new PortInst[protoType.getNumPorts()];
		for (int i = 0; i < portInsts.length; i++)
		{
			PortProto pp = protoType.getPort(i);
			portInsts[i] = PortInst.newInstance(pp, this);
		}

        if (nameDescriptor == null) nameDescriptor = ImmutableTextDescriptor.getNodeTextDescriptor();
		EPoint anchor = EPoint.snap(center);
        width = DBMath.round(width);
        boolean flipX = width < 0 || width == 0 && 1/width < 0;
        height = DBMath.round(height);
        boolean flipY = height < 0 || height == 0 && 1/height < 0;
        Orientation orient = Orientation.fromJava(angle, flipX, flipY);
        if (protoDescriptor == null) protoDescriptor = ImmutableTextDescriptor.getInstanceTextDescriptor();
        
        d = ImmutableNodeInst.newInstance(0, name, duplicate, nameDescriptor, orient, anchor, Math.abs(width), Math.abs(height), userBits, protoDescriptor);
        
		// fill in the geometry
		redoGeometric();
	}

	/****************************** CREATE, DELETE, MODIFY ******************************/

	/**
	 * Short form method to create a NodeInst and do extra things necessary for it. Angle, name
	 * and techBits are set to defaults.
	 * @param protoType the NodeProto of which this is an instance.
	 * @param center the center location of this NodeInst.
	 * @param width the width of this NodeInst.
	 * If negative, flip the X coordinate (or flip ABOUT the Y axis).
	 * @param height the height of this NodeInst.
	 * If negative, flip the Y coordinate (or flip ABOUT the X axis).
	 * @param parent the Cell in which this NodeInst will reside.
     * @return the newly created NodeInst, or null on error.
	 */
	public static NodeInst makeInstance(NodeProto protoType, Point2D center, double width, double height, Cell parent)
	{
		return (makeInstance(protoType, center, width, height, parent, 0, null, 0));
	}

	/**
	 * Long form method to create a NodeInst and do extra things necessary for it.
	 * @param protoType the NodeProto of which this is an instance.
	 * @param center the center location of this NodeInst.
	 * @param width the width of this NodeInst.
	 * If negative, flip the X coordinate (or flip ABOUT the Y axis).
	 * @param height the height of this NodeInst.
	 * If negative, flip the Y coordinate (or flip ABOUT the X axis).
	 * @param parent the Cell in which this NodeInst will reside.
	 * @param angle the angle of this NodeInst (in tenth-degrees).
	 * @param name name of new NodeInst
	 * @param techBits bits associated to different technologies
     * @return the newly created NodeInst, or null on error.
	 */
	public static NodeInst makeInstance(NodeProto protoType, Point2D center, double width, double height,
	                                    Cell parent, int angle, String name, int techBits)
	{
		NodeInst ni = newInstance(protoType, center, width, height, parent, angle, name, techBits);
		if (ni != null)
		{
			// set default information from the prototype
			if (protoType instanceof Cell)
			{
				// for cells, use the default expansion on this instance
				if (((Cell)protoType).isWantExpanded()) ni.setExpanded();
			} else
			{
				// for primitives, set a default outline if appropriate
				protoType.getTechnology().setDefaultOutline(ni);
			}

			// create inheritable variables
			CircuitChanges.inheritAttributes(ni, false);
		}
		return ni;
	}

	/**
	 * Method to create a "dummy" NodeInst for use outside of the database.
	 * @param np the prototype of the NodeInst.
	 * @return the dummy NodeInst.
	 */
	public static NodeInst makeDummyInstance(NodeProto np)
	{
		return makeDummyInstance(np, EPoint.ORIGIN, np.getDefWidth(), np.getDefHeight(), 0);
	}

	/**
	 * Method to create a "dummy" NodeInst for use outside of the database.
	 * @param np the prototype of the NodeInst.
	 * @param center the center location of this NodeInst.
	 * @param width the width of this NodeInst.
	 * If negative, flip the X coordinate (or flip ABOUT the Y axis).
	 * @param height the height of this NodeInst.
	 * If negative, flip the Y coordinate (or flip ABOUT the X axis).
	 * @param angle the angle of this NodeInst (in tenth-degrees).
	 * @return the dummy NodeInst.
	 */
	public static NodeInst makeDummyInstance(NodeProto np, Point2D center, double width, double height, int angle)
	{
        return new NodeInst(null, np, Name.findName(""), 0, null, center, width, height, angle, 0, null);
	}

	/**
	 * Short form method to create a NodeInst. Angle, name
	 * and techBits are set to defaults.
	 * @param protoType the NodeProto of which this is an instance.
	 * @param center the center location of this NodeInst.
	 * @param width the width of this NodeInst.
	 * If negative, flip the X coordinate (or flip ABOUT the Y axis).
	 * @param height the height of this NodeInst.
	 * If negative, flip the Y coordinate (or flip ABOUT the X axis).
	 * @param parent the Cell in which this NodeInst will reside.
     * @return the newly created NodeInst, or null on error.
	 */
	public static NodeInst newInstance(NodeProto protoType, Point2D center, double width, double height, Cell parent)
	{
		return (newInstance(protoType, center, width, height, parent, 0, null, 0));
	}

	/**
	 * Long form method to create a NodeInst.
	 * @param protoType the NodeProto of which this is an instance.
	 * @param center the center location of this NodeInst.
	 * @param width the width of this NodeInst.
	 * If negative, flip the X coordinate (or flip ABOUT the Y axis).
	 * @param height the height of this NodeInst.
	 * If negative, flip the Y coordinate (or flip ABOUT the X axis).
	 * @param parent the Cell in which this NodeInst will reside.
	 * @param angle the angle of this NodeInst (in tenth-degrees).
	 * @param name name of new NodeInst
	 * @param techBits bits associated to different technologies
     * @return the newly created NodeInst, or null on error.
	 */
	public static NodeInst newInstance(NodeProto protoType, Point2D center, double width, double height,
	                                   Cell parent, int angle, String name, int techBits)
	{
        int userBits = (techBits << ImmutableNodeInst.NTECHBITSSH)&ImmutableNodeInst.NTECHBITS;
        return newInstance(parent, protoType, name, -1, null, center, width, height, angle, userBits, null);
	}

	/**
	 * Long form method to create a NodeInst.
	 * @param parent the Cell in which this NodeInst will reside.
	 * @param protoType the NodeProto of which this is an instance.
	 * @param name name of new NodeInst
	 * @param duplicate duplicate index of this NodeInst
     * @param nameDescriptor TextDescriptor of name of this NodeInst
	 * @param center the center location of this NodeInst.
	 * @param width the width of this NodeInst.
	 * If negative, flip the X coordinate (or flip ABOUT the Y axis).
	 * @param height the height of this NodeInst.
	 * If negative, flip the Y coordinate (or flip ABOUT the X axis).
	 * @param angle the angle of this NodeInst (in tenth-degrees).
	 * @param userBits bits associated to different technologies
     * @param protoDescriptor TextDescriptor of name of this NodeInst
     * @return the newly created NodeInst, or null on error.
	 */
    public static NodeInst newInstance(Cell parent, NodeProto protoType,
            String name, int duplicate, ImmutableTextDescriptor nameDescriptor,
            Point2D center, double width, double height, int angle,
            int userBits, ImmutableTextDescriptor protoDescriptor)
	{
        if (protoType == null) return null;
//        if (protoType instanceof PrimitiveNode && ((PrimitiveNode)protoType).isNotUsed())
//        {
////            System.out.println("Cannot create node instance of " + protoType + " in " + parent +
////					" because prototype is unused");
////            return null;
//        }
        if (parent == null) return null;
        
		if (protoType instanceof Cell)
		{
            if (Cell.isInstantiationRecursive((Cell)protoType, parent))
			{
				System.out.println("Cannot create instance of " + protoType + " in " + parent +
					" because it is recursive");
				return null;
			}
		}

		if (protoType == Generic.tech.cellCenterNode)
		{
			// only 1 cell-center is allowed: check for others
			for(Iterator it = parent.getNodes(); it.hasNext(); )
			{
				NodeInst oNi = (NodeInst)it.next();
				if (oNi.getProto() == Generic.tech.cellCenterNode)
				{
					System.out.println("Can only be one cell-center in " + parent + ": new one ignored");
					return null;
				}
			}
		}

        Name nameKey = name != null ? Name.findName(name) : null;
		if (nameKey == null || nameKey.isTempname() && (!parent.isUniqueName(nameKey, NodeInst.class, null)) || checkNameKey(nameKey, parent, true))
		{
            Name baseName;
            if (protoType instanceof Cell) {
                baseName = ((Cell)protoType).getBasename();
            } else {
                PrimitiveNode np = (PrimitiveNode)protoType;
                int techBits = (userBits&ImmutableNodeInst.NTECHBITS) >> ImmutableNodeInst.NTECHBITSSH;
                baseName = np.getTechnology().getPrimitiveFunction(np, techBits).getBasename();
            }
            nameKey = parent.getAutoname(baseName);
			duplicate = 0;
		}
        duplicate = parent.fixupNodeDuplicate(nameKey, duplicate);
		NodeInst ni = new NodeInst(parent, protoType, nameKey, duplicate, nameDescriptor, center, width, height, angle, userBits, protoDescriptor);
		if (ni.checkAndRepair(true, null, null) > 0) return null;
		if (ni.lowLevelLink()) return null;

		// handle change control, constraint, and broadcast
		Undo.newObject(ni);
		return ni;
	}

    
	/**
	 * Method to delete this NodeInst.
	 */
	public void kill()
	{
		if (!isLinked())
		{
			System.out.println("NodeInst already killed");
			return;
		}
		// kill the arcs attached to the connections.  This will also remove the connections themselves
		while (connections.size() > 0)
		{
			Connection con = (Connection)connections.get(connections.size() - 1);
			con.getArc().kill();
		}

		// remove any exports
		while (exports.length != 0)
		{
			Export pp = exports[exports.length - 1];
			pp.kill();
		}

		// remove the node
		lowLevelUnlink();

		// handle change control, constraint, and broadcast
		Undo.killObject(this);
	}

	/**
	 * Method to change this NodeInst.
	 * @param dX the amount to move the NodeInst in X.
	 * @param dY the amount to move the NodeInst in Y.
	 * @param dXSize the amount to scale the NodeInst in X.
	 * @param dYSize the amount to scale the NodeInst in Y.
	 * @param dRot the amount to alter the NodeInst rotation (in tenths of a degree).
	 */
	public void modifyInstance(double dX, double dY, double dXSize, double dYSize, int dRot)
	{
		// make sure the change values are sensible
		dRot = dRot % 3600;
		if (dRot < 0) dRot += 3600;

		// make the change
		if (Undo.recordChange())
		{
			Constraints.getCurrent().modifyNodeInst(this, dX, dY, dXSize, dYSize, dRot);
		} else
		{
			lowLevelModify(dX, dY, dXSize, dYSize, dRot);

			// change the coordinates of every arc end connected to this
			for(Iterator it = getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				if (con.getPortInst().getNodeInst() == this)
				{
					Point2D oldLocation = con.getLocation();
					switch (con.getEndIndex())
					{
						case ArcInst.HEADEND:
							con.getArc().modify(0, dX, dY, 0, 0);
							break;
						case ArcInst.TAILEND:
							con.getArc().modify(0, 0, 0, dX, dY);
							break;
					}
				}
			}
		}

		// if the cell-center changed, notify the cell and fix lots of stuff
		if (protoType instanceof PrimitiveNode && protoType == Generic.tech.cellCenterNode)
		{
			parent.adjustReferencePoint(this);
		}
	}

	/**
	 * Method to change many NodeInsts.
	 * @param nis the NodeInsts to change.
	 * @param dXs the amount to move the NodeInsts in X.
	 * @param dYs the amount to move the NodeInsts in Y.
	 * @param dXSizes the amount to scale the NodeInsts in X.
	 * @param dYSizes the amount to scale the NodeInsts in Y.
	 * @param dRots the amount to alter the NodeInst rotation (in tenths of a degree).
	 */
	public static void modifyInstances(NodeInst [] nis, double [] dXs, double [] dYs, double [] dXSizes, double [] dYSizes, int [] dRots)
	{
		// make the change
		if (Undo.recordChange())
		{
			Constraints.getCurrent().modifyNodeInsts(nis, dXs, dYs, dXSizes, dYSizes, dRots);
		} else
		{
			for(int i=0; i<nis.length; i++)
			{
				nis[i].lowLevelModify(dXs[i], dYs[i], dXSizes[i], dYSizes[i], dRots[i]);

				// change the coordinates of every arc end connected to this
				for(Iterator it = nis[i].getConnections(); it.hasNext(); )
				{
					Connection con = (Connection)it.next();
					if (con.getPortInst().getNodeInst() == nis[i])
					{
						Point2D oldLocation = con.getLocation();
						switch (con.getEndIndex())
						{
							case ArcInst.HEADEND:
								con.getArc().modify(0, dXs[i], dYs[i], 0, 0);
								break;
							case ArcInst.TAILEND:
								con.getArc().modify(0, 0, 0, dXs[i], dYs[i]);
								break;
						}
					}
				}
			}
		}

		// if the cell-center changed, notify the cell and fix lots of stuff
		for(int i=0; i<nis.length; i++)
		{
			if (nis[i].getProto() instanceof PrimitiveNode && nis[i].getProto() == Generic.tech.cellCenterNode)
			{
				nis[i].getParent().adjustReferencePoint(nis[i]);
			}
		}
	}

	/**
	 * Method to replace this NodeInst with one of another type.
	 * All arcs and exports on this NodeInst are moved to the new one.
	 * @param np the new type to put in place of this NodeInst.
	 * @param ignorePortNames true to not use port names when determining association between old and new prototype.
	 * @param allowMissingPorts true to allow replacement to have missing ports and, therefore, delete the arcs that used to be there.
	 * @return the new NodeInst that replaces this one.
	 * Returns null if there is an error doing the replacement.
	 */
	public NodeInst replace(NodeProto np, boolean ignorePortNames, boolean allowMissingPorts)
	{
		// check for recursion
		if (np instanceof Cell)
		{
            if (Cell.isInstantiationRecursive((Cell)np, getParent()))
            {
                System.out.println("Cannot replace because it would be recursive");
                return null;
            }
		}

		// get the location of the cell-center on the old NodeInst
		Point2D oldCenter = getAnchorCenter();

		// create the new NodeInst
		double newXS = np.getDefWidth();
		double newYS = np.getDefHeight();
		if ((np instanceof PrimitiveNode) && (getProto() instanceof PrimitiveNode))
		{
			// replacing one primitive with another: adjust sizes accordingly
			SizeOffset oldSO = getProto().getProtoSizeOffset();
			SizeOffset newSO = np.getProtoSizeOffset();
			newXS = getXSize() - oldSO.getLowXOffset() - oldSO.getHighXOffset() + newSO.getLowXOffset() + newSO.getHighXOffset();
			newYS = getYSize() - oldSO.getLowYOffset() - oldSO.getHighYOffset() + newSO.getLowYOffset() + newSO.getHighYOffset();
            // if less than min size, set it to min size
            if (newXS < np.getDefWidth()) newXS = np.getDefWidth();
            if (newYS < np.getDefHeight()) newYS = np.getDefHeight();
            // if old prim is min size, set new prim to min size
            if (getXSize() == getProto().getDefWidth()) newXS = np.getDefWidth();
            if (getYSize() == getProto().getDefHeight()) newYS = np.getDefHeight();
		}

		// see if nodeinst is mirrored
        if (getXSizeWithMirror() < 0) newXS *= -1;
        if (getYSizeWithMirror() < 0) newYS *= -1;
		NodeInst newNi = NodeInst.newInstance(np, oldCenter, newXS, newYS, getParent(), getAngle(), null, 0);
		if (newNi == null) return null;

		// draw new node expanded if appropriate
		if (np instanceof Cell)
		{
			if (getProto() instanceof Cell)
			{
				// replacing an instance: copy the expansion information
				if (isExpanded()) newNi.setExpanded(); else
					newNi.clearExpanded();
			} else
			{
				// replacing a primitive: use default expansion for the cell
				if (((Cell)np).isWantExpanded()) newNi.setExpanded(); else
					newNi.clearExpanded();
			}
		}

		// associate the ports between these nodes
		PortAssociation [] oldAssoc = portAssociate(this, newNi, ignorePortNames);

		// see if the old arcs can connect to ports
		double arcDx = 0, arcDy = 0;
		int arcCount = 0;
		for(Iterator it = getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();

			// make sure there is an association for this port
			int index = 0;
			for( ; index<oldAssoc.length; index++)
				if (oldAssoc[index].portInst == con.getPortInst()) break;
			if (index >= oldAssoc.length || oldAssoc[index].assn == null)
			{
				if (allowMissingPorts) continue;
				System.out.println("No port on new node has same name and location as old node port: " + con.getPortInst().getPortProto().getName());
				newNi.kill();
				return null;
			}

			// make sure the arc can connect to this type of port
			PortInst opi = oldAssoc[index].assn;
			ArcInst ai = con.getArc();
			if (!opi.getPortProto().connectsTo(ai.getProto()))
			{
				if (allowMissingPorts) continue;
				System.out.println(ai + " on old port " + con.getPortInst().getPortProto().getName() +
					" cannot connect to new port " + opi.getPortProto().getName());
				newNi.kill();
				return null;
			}

			// see if the arc fits in the new port
			Poly poly = opi.getPoly();
			if (!poly.isInside(con.getLocation()))
			{
				// arc doesn't fit: accumulate error distance
				double xp = poly.getCenterX();
				double yp = poly.getCenterY();
				arcDx += xp - con.getLocation().getX();
				arcDy += yp - con.getLocation().getY();
			}
			arcCount++;
		}

		// see if the old exports have the same connections
		for(Iterator it = getExports(); it.hasNext(); )
		{
			Export pp = (Export)it.next();

			// make sure there is an association for this port
			int index = 0;
			for( ; index<oldAssoc.length; index++)
				if (oldAssoc[index].portInst == pp.getOriginalPort()) break;
			if (index >= oldAssoc.length || oldAssoc[index].assn == null)
			{
				System.out.println("No port on new node has same name and location as old node port: " +
					pp.getOriginalPort().getPortProto().getName());
				newNi.kill();
				return null;
			}
			PortInst opi = oldAssoc[index].assn;

			// ensure that all arcs connected at exports still connect
			if (pp.doesntConnect(opi.getPortProto().getBasePort()))
			{
				newNi.kill();
				return null;
			}
		}

		// now replace all of the arcs
		List arcList = new ArrayList();
		for(Iterator it = getConnections(); it.hasNext(); )
			arcList.add(it.next());
		for(Iterator it = arcList.iterator(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			int index = 0;
			for( ; index<oldAssoc.length; index++)
				if (oldAssoc[index].portInst == con.getPortInst()) break;
			if (index >= oldAssoc.length || oldAssoc[index].assn == null)
			{
				if (allowMissingPorts) continue;
				System.out.println("No port on new node has same name and location as old node port: " + con.getPortInst().getPortProto().getName());
				newNi.kill();
				return null;
			}

			// make sure the arc can connect to this type of port
			PortInst opi = oldAssoc[index].assn;
			ArcInst ai = con.getArc();
            if (opi == null) {
				if (!allowMissingPorts)
				{
					System.out.println("Cannot re-connect " + ai);
				} else
				{
					ai.kill();
				}
				continue;
            }
			PortInst [] newPortInst = new PortInst[2];
			Point2D [] newPoint = new Point2D[2];
            int thisEnd = con.getEndIndex();
    		newPortInst[thisEnd] = opi;
    		Poly poly = opi.getPoly();
            newPoint[thisEnd] = poly.isInside(con.getLocation()) ? con.getLocation() : new EPoint(poly.getCenterX(), poly.getCenterY());
            int otherEnd = 1 - thisEnd;
			newPortInst[otherEnd] = ai.getPortInst(otherEnd);
            newPoint[otherEnd] = ai.getLocation(otherEnd);
//			for(int i=0; i<2; i++)
//			{
//				Connection oneCon = ai.getConnection(i);
//				if (oneCon == con)
//				{
//					newPortInst[i] = opi;
//					if (newPortInst[i] == null) break;
//					newPoint[i] = new Point2D.Double(con.getLocation().getX(), con.getLocation().getY());
//					Poly poly = opi.getPoly();
//					if (!poly.isInside(newPoint[i]))
//						newPoint[i].setLocation(poly.getCenterX(), poly.getCenterY());
//				} else
//				{
//					newPortInst[i] = oneCon.getPortInst();
//					newPoint[i] = oneCon.getLocation();
//				}
//			}
//			if (newPortInst[0] == null || newPortInst[1] == null)
//			{
//				if (!allowMissingPorts)
//				{
//					System.out.println("Cannot re-connect " + ai.describe() + " arc");
//				} else
//				{
//					ai.kill();
//				}
//				continue;
//			}

			// see if a bend must be made in the wire
			boolean zigzag = false;
			if (ai.isFixedAngle())
			{
				if (newPoint[0].getX() != newPoint[1].getX() || newPoint[0].getY() != newPoint[1].getY())
				{
					int ii = DBMath.figureAngle(newPoint[0], newPoint[1]);
					int ang = ai.getAngle();
					if ((ii%1800) != (ang%1800)) zigzag = true;
				}
			}

			// see if a bend can be a straight by some simple manipulations
			if (zigzag && !ai.isRigid() && (ai.getAngle() % 900) == 0)
			{
				// find the node at the other end
//				int otherEnd = 0;
//				if (ai.getConnection(0) == con) otherEnd = 1;
				NodeInst adjustThisNode = ai.getPortInst(otherEnd).getNodeInst();
				if (adjustThisNode.getNumExports() == 0)
				{
					// other end not exported, see if all arcs can be adjusted
					boolean adjustable = true;
					for(Iterator oIt = adjustThisNode.getConnections(); oIt.hasNext(); )
					{
						Connection otherCon = (Connection)oIt.next();
						ArcInst otherArc = otherCon.getArc();
						if (otherArc == ai) continue;
						if (otherArc.isRigid()) { adjustable = false;   break; }
						if (otherArc.getAngle() % 900 != 0) { adjustable = false;   break; }
						if (((ai.getAngle() / 900) & 1) == ((otherArc.getAngle() / 900) & 1)) { adjustable = false;   break; }
					}
					if (adjustable)
					{
						double dX = 0, dY = 0;
						if ((ai.getAngle() % 1800) == 0)
						{
							// horizontal arc: move the other node vertically
							dY = newPoint[1-otherEnd].getY() - newPoint[otherEnd].getY();
							newPoint[otherEnd] = new Point2D.Double(newPoint[otherEnd].getX(), newPoint[1-otherEnd].getY());
						} else
						{
							// vertical arc: move the other node horizontaly
							dX = newPoint[1-otherEnd].getX() - newPoint[otherEnd].getX();
							newPoint[otherEnd] = new Point2D.Double(newPoint[1-otherEnd].getX(), newPoint[otherEnd].getY());
						}

						// special case where the old arc must be deleted first so that the other node can move
						ai.kill();
						adjustThisNode.modifyInstance(dX, dY, 0, 0, 0);
						ArcInst newAi = ArcInst.newInstance(ai.getProto(), ai.getWidth(), newPortInst[ArcInst.HEADEND], newPortInst[ArcInst.TAILEND],
							newPoint[ArcInst.HEADEND], newPoint[ArcInst.TAILEND], ai.getName(), 0);
						if (newAi == null)
						{
							newNi.kill();
							return null;
						}
                        newAi.copyPropertiesFrom(ai);
//						newAi.lowLevelSetUserbits(ai.lowLevelGetUserbits());
//						newAi.setHeadNegated(ai.isHeadNegated());
//						newAi.setTailNegated(ai.isTailNegated());
//						newAi.copyVarsFrom(ai);
//						newAi.copyTextDescriptorFrom(ai, ArcInst.ARC_NAME_TD);
						continue;
					}
				}
			}

			ArcInst newAi;
			if (zigzag)
			{
				// make that two wires
				double cX = newPoint[0].getX();
				double cY = newPoint[1].getY();
				NodeProto pinNp = ai.getProto().findOverridablePinProto();
				double psx = pinNp.getDefWidth();
				double psy = pinNp.getDefHeight();
				NodeInst pinNi = NodeInst.newInstance(pinNp, new Point2D.Double(cX, cY), psx, psy, getParent());
				PortInst pinPi = pinNi.getOnlyPortInst();
				newAi = ArcInst.newInstance(ai.getProto(), ai.getWidth(), newPortInst[ArcInst.HEADEND], pinPi, newPoint[ArcInst.HEADEND],
				    new Point2D.Double(cX, cY), null, 0);
				if (newAi == null) return null;
                newAi.copyPropertiesFrom(ai);
//				newAi.copyStateBits(ai);
//				newAi.setHeadNegated(ai.isHeadNegated());
//                newAi.copyVarsFrom(ai);
//                newAi.copyTextDescriptorFrom(ai, ArcInst.ARC_NAME_TD);

				ArcInst newAi2 = ArcInst.newInstance(ai.getProto(), ai.getWidth(), pinPi, newPortInst[ArcInst.TAILEND], new Point2D.Double(cX, cY),
				        newPoint[ArcInst.TAILEND], null, 0);
				if (newAi2 == null) return null;
                newAi2.copyConstraintsFrom(ai);
//				newAi2.copyStateBits(ai);
//				newAi2.setTailNegated(ai.isTailNegated());
				if (newPortInst[ArcInst.TAILEND].getNodeInst() == this)
				{
					ArcInst aiSwap = newAi;   newAi = newAi2;   newAi2 = aiSwap;
				}
			} else
			{
				// replace the arc with another arc
				newAi = ArcInst.newInstance(ai.getProto(), ai.getWidth(), newPortInst[ArcInst.HEADEND], newPortInst[ArcInst.TAILEND],
                        newPoint[ArcInst.HEADEND], newPoint[ArcInst.TAILEND], null, 0);
				if (newAi == null)
				{
					newNi.kill();
					return null;
				}
				newAi.copyPropertiesFrom(ai);
//				newAi.setHeadNegated(ai.isHeadNegated());
//				newAi.setTailNegated(ai.isTailNegated());
			}
			ai.kill();
			newAi.setName(ai.getName());
		}

		// now replace all of the exports
		List exportList = new ArrayList();
		for(Iterator it = getExports(); it.hasNext(); )
		{
			exportList.add(it.next());
		}
		for(Iterator it = exportList.iterator(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			int index = 0;
			for( ; index<oldAssoc.length; index++)
				if (oldAssoc[index].portInst == pp.getOriginalPort()) break;
			if (index >= oldAssoc.length || oldAssoc[index].assn == null) continue;
			PortInst newPi = oldAssoc[index].assn;
			pp.move(newPi);
		}

		// copy all variables on the nodeinst
		newNi.copyVarsFrom(this);
		newNi.copyTextDescriptorFrom(this, NodeInst.NODE_NAME_TD);
		newNi.copyTextDescriptorFrom(this, NodeInst.NODE_PROTO_TD);
		newNi.lowLevelSetUserbits(lowLevelGetUserbits());

		// now delete the original nodeinst
		kill();
		newNi.setName(getName());
		return newNi;
	}

	/****************************** LOW-LEVEL IMPLEMENTATION ******************************/

	/**
	 * Low-level access method to link the NodeInst into its Cell.
	 * @return true on error.
	 */
	public boolean lowLevelLink()
	{
        if (!isDatabaseObject()) {
			System.out.println("NodeInst can't be linked because it is a dummy object");
			return true;
		}

		// add to linked lists
		nodeUsage = parent.addUsage(getProto());
        if (nodeUsage == null) return true;
		if (parent.addNode(this)) return true;
		parent.linkNode(this);
		return false;
	}

	/**
	 * Low-level method to unlink the NodeInst from its Cell.
	 */
	public void lowLevelUnlink()
	{
		// remove this node from the cell
		parent.removeNode(this);
		parent.unLinkNode(this);
		parent.checkInvariants();
	}

	/**
	 * Method to adjust this NodeInst by the specified deltas.
	 * This method does not go through change control, and so should not be used unless you know what you are doing.
	 * @param dX the change to the center X coordinate.
	 * @param dY the change to the center Y coordinate.
	 * @param dXSize the change to the X size.
	 * @param dYSize the change to the Y size.
	 * @param dRot the change to the rotation (in tenths of a degree).
	 */
	public void lowLevelModify(double dX, double dY, double dXSize, double dYSize, int dRot)
	{
		// remove from the R-Tree structure
		if (parent != null)
			parent.unLinkNode(this);

		// make the change
        if (dX != 0 || dY != 0)
            d = d.withAnchor(new EPoint(getAnchorCenterX() + dX, getAnchorCenterY() + dY));
        if (dXSize != 0 || dYSize != 0 || dRot != 0) {
            double width, height;
            boolean flipX, flipY;
    		if (dXSize != 0) {
                double newSX = DBMath.round(getXSizeWithMirror() + dXSize);
                flipX = newSX < 0;
                width = Math.abs(newSX);
            } else {
                flipX = isXMirrored();
                width = getXSize();
            }
    		if (dYSize != 0) {
                double newSY = DBMath.round(getYSizeWithMirror() + dYSize);
                flipY = newSY < 0;
                height = Math.abs(newSY);
            } else {
                flipY = isYMirrored();
                height = getYSize();
            }
            if (flipX != d.orient.isXMirrored() || flipY != d.orient.isYMirrored() || dRot != 0)
                d = d.withOrient(Orientation.fromJava(d.orient.getAngle() + dRot, flipX, flipY));
            d = d.withSize(width, height);
        }
            
		// fill in the Geometric fields
		redoGeometric();

		// link back into the R-Tree
		if (parent != null)
		{
			parent.linkNode(this);
			parent.setDirty();
		}
	}

	/**
	 * Method to tell whether this NodeInst is an icon of its parent.
	 * Electric does not allow recursive circuit hierarchies (instances of Cells inside of themselves).
	 * However, it does allow one exception: a schematic may contain its own icon for documentation purposes.
	 * This method determines whether this NodeInst is such an icon.
	 * @return true if this NodeInst is an icon of its parent.
	 */
	public boolean isIconOfParent()
	{
		return nodeUsage.isIconOfParent();
	}

	/**
	 * Method to set an index of this NodeInst in Cell nodes.
	 * This is a zero-based index of nodes on the Cell.
	 * @param nodeIndex an index of this NodeInst in Cell nodes.
	 */
	public void setNodeIndex(int nodeIndex) { this.nodeIndex = nodeIndex; }

	/**
	 * Method to get the index of this NodeInst.
	 * This is a zero-based index of nodes on the Cell.
	 * @return the index of this NodeInst.
	 */
	public final int getNodeIndex() { return nodeIndex; }

	/**
	 * Method tells if this NodeInst is linked to parent Cell.
	 * @return true if this NodeInst is linked to parent Cell.
	 */
	public boolean isLinked()
	{
		try
		{
			return parent != null && parent.isLinked() && parent.getNode(nodeIndex) == this;
		} catch (IndexOutOfBoundsException e)
		{
			return false;
		}
	}

    /**
     * This method can be overridden by extending objects.
     * For objects (such as instances) that have instance variables that are
     * inherited from some Object that has the default variables, this gets
     * the object that has the default variables. From that object the
     * default values of the variables can then be found.
     * @return the object that holds the default variables and values.
     */
    public ElectricObject getVarDefaultOwner() {
        if (getProto() instanceof Cell) {
            Cell proto = (Cell)getProto();
            if (proto.isIcon()) {
                // schematic has default vars
                Cell sch = proto.getCellGroup().getMainSchematics();
                if (sch != null) {
                    return sch;
                }
                return proto;
            } else
                return proto;
        }
        return this;
    }

	/****************************** GRAPHICS ******************************/

	/**
	 * Method to return the rotation angle of this NodeInst.
	 * @return the rotation angle of this NodeInst (in tenth-degrees).
	 */
	public int getAngle() { return d.orient.getAngle(); }

	/**
	 * Method to return the center point of this NodeInst object.
	 * @return the center point of this NodeInst object.
	 */
	public EPoint getAnchorCenter() { return d.anchor; }

	/**
	 * Method to return the center X coordinate of this NodeInst.
	 * @return the center X coordinate of this NodeInst.
	 */
	public double getAnchorCenterX() { return d.anchor.getX(); }

	/**
	 * Method to return the center Y coordinate of this NodeInst.
	 * @return the center Y coordinate of this NodeInst.
	 */
	public double getAnchorCenterY() { return d.anchor.getY(); }

	/**
	 * Method to return the X size of this NodeInst.
	 * @return the X size of this NodeInst.
	 */
	public double getXSize() { return d.width; }

	/**
	 * Method to return the Y size of this NodeInst.
	 * @return the Y size of this NodeInst.
	 */
	public double getYSize() { return d.height; }

	/**
	 * Method to return the X size of this NodeInst, including the mirroring factor.
	 * When mirrored about Y, the X size is negated.
	 * @return the X size of this NodeInst, including the mirroring factor.
	 */
	public double getXSizeWithMirror() { return d.orient.isXMirrored() ? -d.width : d.width; }

	/**
	 * Method to return the Y size of this NodeInst, including the mirroring factor.
	 * When mirrored about X, the Y size is negated.
	 * @return the Y size of this NodeInst, including the mirroring factor.
	 */
	public double getYSizeWithMirror() { return d.orient.isYMirrored() ? -d.height : d.height; }
	
	/**
	 * Method to return whether NodeInst is mirrored about a 
	 * horizontal line running through its center.
	 * @return true if mirrored.
	 */
	public boolean isMirroredAboutXAxis() { return isYMirrored(); }
	
	/** 
	 * Method to return whether NodeInst is mirrored about a
	 * vertical line running through its center.
	 * @return true if mirrored.
	 */
	public boolean isMirroredAboutYAxis() { return isXMirrored(); }

	/**
	 * Method to tell whether this NodeInst is mirrored in the X coordinate.
	 * Mirroring in the X axis implies that X coordinates are negated.
	 * Thus, it is equivalent to mirroring ABOUT the Y axis.
	 * @return true if this NodeInst is mirrored in the X coordinate.
	 */
	public boolean isXMirrored() { return d.orient.isXMirrored(); }

	/**
	 * Method to tell whether this NodeInst is mirrored in the Y coordinate.
	 * Mirroring in the Y axis implies that Y coordinates are negated.
	 * Thus, it is equivalent to mirroring ABOUT the X axis.
	 * @return true if this NodeInst is mirrored in the Y coordinate.
	 */
	public boolean isYMirrored() { return d.orient.isYMirrored(); }

	/**
	 * Class to convert between Java transformations and C transformations.
	 * The C code used an angle (in tenth-degrees) and a "transpose" factor
	 * which would flip the object along the major diagonal after rotation.
	 * The Java code uses the same angle (in tenth-degrees) but has two mirror
	 * options: Mirror X and Mirror Y.
	 */
	public static class OldStyleTransform
	{
		private int cAngle;
		private boolean cTranspose;
		private int jAngle;
		private boolean jMirrorX;
		private boolean jMirrorY;

		/**
		 * Constructor gives the old C style parameters.
		 * The Java conversion can be obtained from the "get" methods.
		 * @param cAngle the angle of rotation (in tenth-degrees)
		 * @param cTranspose if true, object is flipped over the major diagonal after rotation.
		 */
		public OldStyleTransform(int cAngle, boolean cTranspose)
		{
			// store C information
			this.cAngle = cAngle;
			this.cTranspose = cTranspose;

			// compute Java information
			jAngle = cAngle;
			jMirrorX = jMirrorY = false;
			if (cTranspose)
			{
				jMirrorY = true;
				jAngle = (jAngle + 900) % 3600;
			}
		}

		/**
		 * Constructor gives the new Java style parameters.
		 * The C conversion can be obtained from the "get" methods.
		 * @param ni the NodeInst to use.
		 */
		public OldStyleTransform(NodeInst ni)
		{
			buildFromJava(ni.getAngle(), ni.isXMirrored(), ni.isYMirrored());
		}

		/**
		 * Constructor gives the new Java style parameters.
		 * The C conversion can be obtained from the "get" methods.
		 * @param jAngle the angle of rotation (in tenth-degrees)
		 * @param jMirrorX if true, object is flipped over the vertical (mirror in X).
		 * @param jMirrorY if true, object is flipped over the horizontal (mirror in Y).
		 */
		public OldStyleTransform(int jAngle, boolean jMirrorX, boolean jMirrorY)
		{
			buildFromJava(jAngle, jMirrorX, jMirrorY);
		}

		private void buildFromJava(int jAngle, boolean jMirrorX, boolean jMirrorY)
		{

			// store Java information
			this.jAngle = jAngle;
			this.jMirrorX = jMirrorX;
			this.jMirrorY = jMirrorY;

			// compute C information
			cAngle = jAngle;
			cTranspose = false;
			if (jMirrorX)
			{
				if (jMirrorY)
				{
					cAngle = (cAngle + 1800) % 3600;
				} else
				{
					cAngle = (cAngle + 900) % 3600;
					cTranspose = true;
				}
			} else if (jMirrorY)
			{
				cAngle = (cAngle + 2700) % 3600;
				cTranspose = true;
			}
		}

		/**
		 * Method to return the old C style angle value.
		 * @return the old C style angle value, in tenth-degrees.
		 */
		public int getCAngle() { return cAngle; }
		/**
		 * Method to return the old C style transpose factor.
		 * @return the old C style transpose factor: true to flip over the major diagonal after rotation.
		 */
		public boolean isCTranspose() { return cTranspose; }

		/**
		 * Method to return the new Java style angle value.
		 * @return the new Java style angle value, in tenth-degrees.
		 */
		public int getJAngle() { return jAngle; }
		/**
		 * Method to return the new Java style Mirror X factor.
		 * @return true to flip over the vertical axis (mirror in X).
		 */
		public boolean isJMirrorX() { return jMirrorX; }
		/**
		 * Method to return the new Java style Mirror Y factor.
		 * @return true to flip over the horizontal axis (mirror in Y).
		 */
		public boolean isJMirrorY() { return jMirrorY; }
		
	}

	/**
	 * Method to return the bounds of this NodeInst.
	 * TODO: dangerous to give a pointer to our internal field; should make a copy of visBounds
	 * @return the bounds of this NodeInst.
	 */
	public Rectangle2D getBounds() { return visBounds; }

	/**
	 * Method to return the starting and ending angle of an arc described by this NodeInst.
	 * These values can be found in the "ART_degrees" variable on the NodeInst.
	 * @return a 2-long double array with the starting offset in the first entry (a value in radians)
	 * and the amount of curvature in the second entry (in radians).
	 * If the NodeInst does not have circular information, both values are set to zero.
	 */
	public double [] getArcDegrees()
	{
		double [] returnValues = new double[2];
		returnValues[0] = returnValues[1] = 0.0;

		if (!(protoType instanceof PrimitiveNode)) return returnValues;
		if (protoType != Artwork.tech.circleNode && protoType != Artwork.tech.thickCircleNode) return returnValues;

		Variable var = getVar(Artwork.ART_DEGREES);
		if (var != null)
		{
			Object addr = var.getObject();
			if (addr instanceof Integer)
			{
				Integer iAddr = (Integer)addr;
				returnValues[0] = 0.0;
				returnValues[1] = (double)iAddr.intValue() * Math.PI / 1800.0;
			} else if (addr instanceof Float[])
			{
				Float [] fAddr = (Float [])addr;
				returnValues[0] = fAddr[0].doubleValue();
				returnValues[1] = fAddr[1].doubleValue();
			}
		}
		return returnValues;
	}

	/**
	 * Method to set the starting and ending angle of an arc described by this NodeInst.
	 * These values are stored in the "ART_degrees" variable on the NodeInst.
	 * @param start the starting offset of the angle (typically 0)
	 * @param curvature the the amount of curvature
	 */
	public void setArcDegrees(double start, double curvature)
	{
		if (!(protoType instanceof PrimitiveNode)) return;
		if (protoType != Artwork.tech.circleNode && protoType != Artwork.tech.thickCircleNode) return;
		if (start == 0 && curvature == 0)
		{
			if (getVar(Artwork.ART_DEGREES) == null) return;
			delVar(Artwork.ART_DEGREES);
		} else
		{
			Float [] fAddr = new Float[2];
			fAddr[0] = new Float(start);
			fAddr[1] = new Float(curvature);
			newVar(Artwork.ART_DEGREES, fAddr);
		}
	}

	/**
	 * Method to get the SizeOffset associated with this NodeInst.
	 * By invoking a method in the node's technology,
	 * the determination of SizeOffset can be overriden by specific technologies.
	 * @return the SizeOffset object for the NodeInst.
	 */
	public SizeOffset getSizeOffset()
	{
		Technology tech = protoType.getTechnology();
		return tech.getNodeInstSizeOffset(this);
	}

	/**
	 * Method to recalculate the Geometric bounds for this NodeInst.
	 */
	private void redoGeometric()
	{
		// if zero size, set the bounds directly
		if (getXSize() == 0 && getYSize() == 0)
		{
			visBounds.setRect(getAnchorCenterX(), getAnchorCenterY(), 0, 0);
			return;
		}

		// handle cell bounds
		if (protoType instanceof Cell)
		{
			// offset by distance from cell-center to the true center
			Cell subCell = (Cell)protoType;
			Rectangle2D bounds = subCell.getBounds();
			Point2D shift = new Point2D.Double(-bounds.getCenterX(), -bounds.getCenterY());
			AffineTransform trans = pureRotate(d.orient.getAngle(), isXMirrored(), isYMirrored());
			trans.transform(shift, shift);
			double cX = getAnchorCenterX(), cY = getAnchorCenterY();
			cX -= shift.getX();
			cY -= shift.getY();
			Poly poly = new Poly(cX, cY, getXSize(), getYSize());
			trans = rotateAbout(d.orient.getAngle(), cX, cY, getXSizeWithMirror(), getYSizeWithMirror());
			poly.transform(trans);
			visBounds.setRect(poly.getBounds2D());
			return;
		}

		PrimitiveNode pn = (PrimitiveNode)protoType;

		// special case for arcs of circles
		if (pn == Artwork.tech.circleNode || pn == Artwork.tech.thickCircleNode)
		{
			// see if this circle is only a partial one
			double [] angles = getArcDegrees();
			if (angles[0] != 0.0 || angles[1] != 0.0)
			{
				Point2D [] pointList = Artwork.fillEllipse(getAnchorCenter(), getXSize(), getYSize(), angles[0], angles[1]);
				Poly poly = new Poly(pointList);
				poly.setStyle(Poly.Type.OPENED);
				poly.transform(rotateOut());
				visBounds.setRect(poly.getBounds2D());
				return;
			}
		}

		// special case for pins that become steiner points
		if (pn.isWipeOn1or2() && getNumExports() == 0)
		{
			if (pinUseCount())
			{
				visBounds.setRect(getAnchorCenterX(), getAnchorCenterY(), 0, 0);
				return;
			}
		}

		// special case for polygonally-defined nodes: compute precise geometry
		if (pn.isHoldsOutline() && getTrace() != null)
		{
			AffineTransform trans = rotateOut();
			Poly[] polys = pn.getTechnology().getShapeOfNode(this);
			Rectangle2D bounds = new Rectangle2D.Double();
			for (int i = 0; i < polys.length; i++)
			{
				Poly poly = polys[i];
				if (i == 0)
					bounds.setRect(poly.getBounds2D());
				else
					Rectangle2D.union(poly.getBounds2D(), bounds, bounds);
				poly.transform(trans);
				if (i == 0)
					visBounds.setRect(poly.getBounds2D());
				else
					Rectangle2D.union(poly.getBounds2D(), visBounds, visBounds);
			}

			// recompute actual bounds
			d = d.withSize(bounds.getWidth(), bounds.getHeight());
			return;
		}

		// normal bounds computation
		Poly poly = new Poly(getAnchorCenterX(), getAnchorCenterY(), getXSize(), getYSize());
		AffineTransform trans = rotateOut();
		poly.transform(trans);
		visBounds.setRect(poly.getBounds2D());
	}

	/**
	 * Method to return a list of Polys that describes all text on this NodeInst.
	 * @param hardToSelect is true if considering hard-to-select text.
	 * @param wnd the window in which the text will be drawn.
	 * @return an array of Polys that describes the text.
	 */
	public Poly [] getAllText(boolean hardToSelect, EditWindow wnd)
	{
		int cellInstanceNameText = 0;
		if (protoType instanceof Cell && !isExpanded() && hardToSelect) cellInstanceNameText = 1;
		if (!User.isTextVisibilityOnInstance()) cellInstanceNameText = 0;
		int dispVars = numDisplayableVariables(false);
		int numExports = 0;
		int numExportVariables = 0;
		if (User.isTextVisibilityOnExport())
		{
			numExports = getNumExports();
			for(Iterator it = getExports(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				numExportVariables += pp.numDisplayableVariables(false);
			}
		}
		if (protoType == Generic.tech.invisiblePinNode &&
			!User.isTextVisibilityOnAnnotation())
		{
			dispVars = numExports = numExportVariables = 0;
		}
		if (!User.isTextVisibilityOnNode())
		{
			cellInstanceNameText = dispVars = numExports = numExportVariables = 0;
		}
		int totalText = cellInstanceNameText + dispVars + numExports + numExportVariables;
		if (totalText == 0) return null;
		Poly [] polys = new Poly[totalText];
		int start = 0;

		// add in the cell name if appropriate
		if (cellInstanceNameText != 0)
		{
			double cX = getTrueCenterX();
			double cY = getTrueCenterY();
			TextDescriptor td = getTextDescriptor(NodeInst.NODE_PROTO_TD);
			double offX = td.getXOff();
			double offY = td.getYOff();
			TextDescriptor.Position pos = td.getPos();
			Poly.Type style = pos.getPolyType();
			Point2D [] pointList = new Point2D.Double[1];
			pointList[0] = new Point2D.Double(cX+offX, cY+offY);
			polys[start] = new Poly(pointList);
			polys[start].setStyle(style);
			polys[start].setString(getProto().describe(false));
			polys[start].setTextDescriptor(td);
			start++;
		}

		// add in the exports
		if (numExports > 0)
		{
			AffineTransform unTrans = rotateIn();
			for(Iterator it = getExports(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				polys[start] = pp.getNamePoly();
				polys[start].transform(unTrans);
				start++;

				// add in variables on the exports
				Poly poly = pp.getOriginalPort().getPoly();
				int numadded = pp.addDisplayableVariables(poly.getBounds2D(), polys, start, wnd, false);
				for(int i=0; i<numadded; i++)
					polys[start+i].setPort(pp);
				start += numadded;
			}
		}

		// add in the displayable variables
		if (dispVars > 0) addDisplayableVariables(getUntransformedBounds(), polys, start, wnd, false);
		return polys;
	}

	/**
	 * Method to return the bounds of this NodeInst before it is transformed.
	 * @return the bounds of this NodeInst before it is transformed.
	 */
	public Rectangle2D getUntransformedBounds()
	{
		if (protoType instanceof PrimitiveNode)
		{
			// primitive
			SizeOffset so = getSizeOffset();
			double wid = getXSize();
			double hei = getYSize();
			double lx = getAnchorCenterX() - wid/2;
			double hx = lx + wid;
			double ly = getAnchorCenterY() - hei/2;
			double hy = ly + hei;
			lx += so.getLowXOffset();
			hx -= so.getHighXOffset();
			ly += so.getLowYOffset();
			hy -= so.getHighYOffset();
			Rectangle2D ret = new Rectangle2D.Double(lx, ly, hx-lx, hy-ly);
			return ret;
		}

		// cell instance
		Rectangle2D bounds = ((Cell)protoType).getBounds();
		Rectangle2D ret = new Rectangle2D.Double(bounds.getMinX()+getAnchorCenterX(), bounds.getMinY()+getAnchorCenterY(), bounds.getWidth(), bounds.getHeight());
		return ret;
	}

	/**
	 * Method to return the number of displayable Variables on this NodeInst and all of its PortInsts.
	 * A displayable Variable is one that will be shown with its object.
	 * Displayable Variables can only sensibly exist on NodeInst, ArcInst, and PortInst objects.
	 * @return the number of displayable Variables on this NodeInst and all of its PortInsts.
	 */
	public int numDisplayableVariables(boolean multipleStrings)
	{
		int numVarsOnNode = super.numDisplayableVariables(multipleStrings);

		for(Iterator it = getPortInsts(); it.hasNext(); )
		{
			PortInst pi = (PortInst)it.next();
			numVarsOnNode += pi.numDisplayableVariables(multipleStrings);
		}
		return numVarsOnNode;
	}

	/**
	 * Method to add all displayable Variables on this NodeInst and its PortInsts to an array of Poly objects.
	 * @param rect a rectangle describing the bounds of the NodeInst on which the Variables will be displayed.
	 * @param polys an array of Poly objects that will be filled with the displayable Variables.
	 * @param start the starting index in the array of Poly objects to fill with displayable Variables.
	 * @param wnd window in which the Variables will be displayed.
	 * @param multipleStrings true to break multiline text into multiple Polys.
	 * @return the number of Polys that were added.
	 */
	public int addDisplayableVariables(Rectangle2D rect, Poly [] polys, int start, EditWindow wnd, boolean multipleStrings)
	{
		int numAddedVariables = super.addDisplayableVariables(rect, polys, start, wnd, multipleStrings);

		for(Iterator it = getPortInsts(); it.hasNext(); )
		{
			PortInst pi = (PortInst)it.next();
			int justAdded = pi.addDisplayableVariables(rect, polys, start+numAddedVariables, wnd, multipleStrings);
			for(int i=0; i<justAdded; i++)
				polys[start+numAddedVariables+i].setPort(pi.getPortProto());
			numAddedVariables += justAdded;
		}
		return numAddedVariables;
	}

	/**
	 * Method to return a transformation that moves up the hierarchy.
	 * Presuming that this NodeInst is a Cell instance, the
	 * transformation maps points in the Cell's coordinate space 
	 * into this NodeInst's parent Cell's coordinate space.
	 * @return a transformation that moves up the hierarchy.
	 */
	public AffineTransform transformOut()
	{
		// The transform first translates to the position of the
		// NodeInst's Anchor point in the parent Cell, and then rotates and
		// mirrors about the anchor point. 
		AffineTransform xform = rotateOut();
		xform.concatenate(translateOut());
		return xform;
	}

	/**
	 * Method to return a transformation that moves down the hierarchy.
	 * Presuming that this NodeInst is a Cell instance, the
	 * transformation maps points in the Cell's coordinate space
	 * into this NodeInst's parent Cell's coordinate space.
	 * @return a transformation that moves down the hierarchy.
	 */
	public AffineTransform transformIn()
	{
		// The transform first rotates in, and then translates to the position..
		AffineTransform xform = rotateIn();
		xform.preConcatenate(translateIn());
		return xform;
	}

	/**
	 * Method to return a transformation that translates down the hierarchy.
	 * Transform out of this node instance, translate outer coordinates to inner
	 * However, it does not account for the rotation of this NodeInst...it only
	 * translates from one space to another.
	 * @return a transformation that translates down the hierarchy.
	 */
	public AffineTransform translateIn()
	{
		// to transform out of this node instance, translate outer coordinates to inner
		//Cell lowerCell = (Cell)protoType;
		double dx = getAnchorCenterX();
		double dy = getAnchorCenterY();
		AffineTransform transform = new AffineTransform();
		transform.translate(-dx, -dy);
		return transform;
	}

	/**
	 * Method to return a transformation that translates down the
	 * hierarchy, combined with a previous transformation.
	 * However, it does not account for the rotation of
	 * this NodeInst...it only translates from one space to another.
	 * @param prevTransform the previous transformation to the NodeInst's Cell.
	 * @return a transformation that translates down the hierarchy,
	 * including the previous transformation.
	 */
	public AffineTransform translateIn(AffineTransform prevTransform)
	{
		AffineTransform transform = translateIn();
		AffineTransform returnTransform = new AffineTransform(prevTransform);
		returnTransform.concatenate(transform);
		return returnTransform;
	}

	/**
	 * Method to return a transformation that translates up the hierarchy.
	 * Transform out of this node instance, translate inner coordinates to outer.
	 * However, it does not account for the rotation of this NodeInst...it only
	 * translates from one space to another.
	 * @return a transformation that translates up the hierarchy.
	 */
	public AffineTransform translateOut()
	{
		// to transform out of this node instance, translate inner coordinates to outer
		//Cell lowerCell = (Cell)protoType;
		double dx = getAnchorCenterX();
		double dy = getAnchorCenterY();
		AffineTransform transform = new AffineTransform();
		transform.translate(dx, dy);
		return transform;
	}

	/**
	 * Method to return a transformation that translates up the
	 * hierarchy, combined with a previous transformation.  Presuming
	 * that this NodeInst is a Cell instance, the transformation goes
	 * from the space of that Cell to the space of this NodeInst's
	 * parent Cell.  However, it does not account for the rotation of
	 * this NodeInst...it only translates from one space to another.
	 * @param prevTransform the previous transformation to the NodeInst's Cell.
	 * @return a transformation that translates up the hierarchy,
	 * including the previous transformation.
	 */
	public AffineTransform translateOut(AffineTransform prevTransform)
	{
		AffineTransform transform = translateOut();
		AffineTransform returnTransform = new AffineTransform(prevTransform);
		returnTransform.concatenate(transform);
		return returnTransform;
	}

	private static AffineTransform rotateTranspose = new AffineTransform();
	private static AffineTransform mirrorXcoord = new AffineTransform(-1, 0, 0, 1, 0, 0);
	private static AffineTransform mirrorYcoord = new AffineTransform(1, 0, 0, -1, 0, 0);

	/**
	 * Method to return a transformation that rotates an object about a point.
	 * @param angle the amount to rotate (in tenth-degrees).
	 * @param cX the center X coordinate about which to rotate.
	 * @param cY the center Y coordinate about which to rotate.
	 * @param sX the scale in X (negative to flip the X coordinate, or flip ABOUT the Y axis).
	 * @param sY the scale in Y (negative to flip the Y coordinate, or flip ABOUT the X axis).
	 * @return a transformation that rotates about that point.
	 */
	public static AffineTransform rotateAbout(int angle, double cX, double cY, double sX, double sY)
	{
		AffineTransform transform = new AffineTransform();
		boolean xMirrored = sX < 0 || sX == 0 && 1/sX < 0;
		boolean yMirrored = sY < 0 || sY == 0 && 1/sY < 0;
		if (xMirrored || yMirrored)
		{
			// must do mirroring, so it is trickier
			rotateTranspose.setToRotation(angle * Math.PI / 1800.0);
			transform.setToTranslation(cX, cY);
			if (xMirrored) transform.concatenate(mirrorXcoord);
			if (yMirrored) transform.concatenate(mirrorYcoord);
			transform.concatenate(rotateTranspose);
			transform.translate(-cX, -cY);
		} else
		{
			transform.setToRotation(angle * Math.PI / 1800.0, cX, cY);
		}
		return transform;
	}

	/**
	 * Method to return a transformation that rotates an object.
	 * @param angle the amount to rotate (in tenth-degrees).
	 * @param mirrorX true to flip the X coordinate, or flip ABOUT the Y axis).
	 * @param mirrorY true to flip the Y coordinate, or flip ABOUT the X axis).
	 * @return a transformation that rotates by this amount.
	 */
	public static AffineTransform pureRotate(int angle, boolean mirrorX, boolean mirrorY)
	{
		AffineTransform transform = new AffineTransform();
		transform.setToRotation(angle * Math.PI / 1800.0);

		// add mirroring
		if (mirrorX) transform.preConcatenate(mirrorXcoord);
		if (mirrorY) transform.preConcatenate(mirrorYcoord);
		return transform;
	}

	/**
	 * Method to return a transformation that rotates an object using old C-style notation.
	 * @param angle the amount to rotate (in tenth-degrees).
	 * @param transpose true to transpose about the diagonal after rotation.
	 * @return a transformation that rotates by this amount.
	 */
	public static AffineTransform pureRotate(int angle, boolean transpose)
	{
		boolean mirrorY = false;
		if (transpose)
		{
			mirrorY = true;
			angle = (angle + 900) % 3600;
		}
		return pureRotate(angle, false, mirrorY);
	}

	/**
	 * Method to return a transformation that rotates the same as this NodeInst.
	 * It transforms points on this NodeInst to account for the NodeInst's rotation.
	 * The rotation happens about the origin.
	 * @return a transformation that rotates the same as this NodeInst.
	 * If this NodeInst is not rotated, the returned transformation is identity.
	 */
	public AffineTransform pureRotateOut()
	{
		return pureRotate(d.orient.getAngle(), isXMirrored(), isYMirrored());
	}

	/**
	 * Method to return a transformation that unrotates the same as this NodeInst.
	 * It transforms points on this NodeInst to account for the NodeInst's rotation.
	 * The rotation happens about the origin.
	 * @return a transformation that unrotates the same as this NodeInst.
	 * If this NodeInst is not rotated, the returned transformation is identity.
	 */
	public AffineTransform pureRotateIn()
	{
		int numFlips = 0;
		if (isXMirrored()) numFlips++;
		if (isYMirrored()) numFlips++;
		int rotAngle = d.orient.getAngle();
		if (numFlips != 1) rotAngle = -rotAngle;
		return pureRotate(rotAngle, isXMirrored(), isYMirrored());
	}

	/**
	 * Method to return a transformation that unrotates this NodeInst.
	 * It transforms points on this NodeInst that have been rotated with the node
	 * so that they appear in the correct location on the unrotated node.
	 * The rotation happens about the node's Anchor Point (the location of the cell-center inside of cell definitions).
	 * @return a transformation that unrotates this NodeInst.
	 * If this NodeInst is not rotated, the returned transformation is identity.
	 */
	public AffineTransform rotateIn()
	{
		int numFlips = 0;
		if (isXMirrored()) numFlips++;
		if (isYMirrored()) numFlips++;
		int rotAngle = d.orient.getAngle();
		if (numFlips != 1) rotAngle = -rotAngle;
		return rotateAbout(rotAngle, getAnchorCenterX(), getAnchorCenterY(), getXSizeWithMirror(), getYSizeWithMirror());
	}

	/**
	 * Method to return a transformation that unrotates this NodeInst,
	 * combined with a previous transformation.
	 * It transforms points on this NodeInst that have been rotated with the node
	 * so that they appear in the correct location on the unrotated node.
	 * The rotation happens about the node's Anchor Point (the location of the cell-center inside of cell definitions).
	 * @param prevTransform the previous transformation to be applied.
	 * @return a transformation that unrotates this NodeInst, combined
	 * with a previous transformation.  If this NodeInst is not
	 * rotated, the returned transformation is the original parameter.
	 */
	public AffineTransform rotateIn(AffineTransform prevTransform)
	{
		// if there is no transformation, stop now
		if (d.orient == Orientation.IDENT) return prevTransform;

		AffineTransform transform = rotateIn();
		AffineTransform returnTransform = new AffineTransform(prevTransform);
		returnTransform.concatenate(transform);
		return returnTransform;
	}

	/**
	 * Method to return a transformation that rotates this NodeInst.
	 * It transforms points on this NodeInst to account for the NodeInst's rotation.
	 * The rotation happens about the node's Anchor Point (the location of the cell-center inside of cell definitions).
	 * @return a transformation that rotates this NodeInst.
	 * If this NodeInst is not rotated, the returned transformation is identity.
	 */
	public AffineTransform rotateOut()
	{
		return rotateAbout(d.orient.getAngle(), getAnchorCenterX(), getAnchorCenterY(), getXSizeWithMirror(), getYSizeWithMirror());
	}

	/**
	 * Method to return a transformation that rotates this NodeInst.
	 * It transforms points on this NodeInst to account for the NodeInst's rotation.
	 * The rotation happens about the node's true geometric center.
	 * @return a transformation that rotates this NodeInst.
	 * If this NodeInst is not rotated, the returned transformation is identity.
	 */
	public AffineTransform rotateOutAboutTrueCenter()
	{
		return rotateAbout(d.orient.getAngle(), getTrueCenterX(), getTrueCenterY(), getXSizeWithMirror(), getYSizeWithMirror());
	}

	/**
	 * Method to return a transformation that rotates this NodeInst,
	 * combined with a previous transformation.  It transforms points
	 * on this NodeInst to account for the NodeInst's rotation.
	 * The rotation happens about the node's Anchor Point (the location of the cell-center inside of cell definitions).
	 * @param prevTransform the previous transformation to be applied.
	 * @return a transformation that rotates this NodeInst, combined
	 * with a previous transformation..  If this NodeInst is not
	 * rotated, the returned transformation is identity.
	 */
	public AffineTransform rotateOut(AffineTransform prevTransform)
	{
		// if there is no transformation, stop now
		if (d.orient == Orientation.IDENT) return prevTransform;

		AffineTransform transform = rotateOut();
		AffineTransform returnTransform = new AffineTransform(prevTransform);
		returnTransform.concatenate(transform);
		return returnTransform;
	}

	/**
	 * Method to return a transformation that rotates this NodeInst,
	 * combined with a previous transformation.  It transforms points
	 * on this NodeInst to account for the NodeInst's rotation.
	 * The rotation happens about the node's true geometric center.
	 * @param prevTransform the previous transformation to be applied.
	 * @return a transformation that rotates this NodeInst, combined
	 * with a previous transformation..  If this NodeInst is not
	 * rotated, the returned transformation is identity.
	 */
	public AffineTransform rotateOutAboutTrueCenter(AffineTransform prevTransform)
	{
		// if there is no transformation, stop now
		if (d.orient == Orientation.IDENT) return prevTransform;

		AffineTransform transform = rotateOutAboutTrueCenter();
		AffineTransform returnTransform = new AffineTransform(prevTransform);
		returnTransform.concatenate(transform);
		return returnTransform;
	}

	/**
	 * Method to return a Poly that describes the location of a port
	 * on this NodeInst.
	 * @param thePort the port on this NodeInst.
	 * @return a Poly that describes the location of the Export.
	 * The Poly is transformed to account for rotation on this NodeInst.
	 */
	public Poly getShapeOfPort(PortProto thePort)
	{
		return getShapeOfPort(thePort, null, false, -1);
	}

	/**
	 * Method to return a Poly that describes the location of a port
	 * on this NodeInst.
	 * @param thePort the port on this NodeInst.
	 * @param selectPt if not null, it requests a new location on the port,
	 * away from existing arcs, and close to this point.
	 * This is useful for "area" ports such as the left side of AND and OR gates.
     * @param forWiringTool this is an experimental flag that will reduce the *untransformed*
     * port width to zero if the primitive node's width is the default width. Likewise with
     * the port height. It will also take into account the arc width of the connecting arc
     * for contact cuts. These modifications are for the wiring tool.
     * @param arcWidth of connecting arc, for the wiring tool if 'forWiringTool' is true.
     * set to -1 to ignore.
	 * @return a Poly that describes the location of the Export.
	 * The Poly is transformed to account for rotation on this NodeInst.
	 */
	public Poly getShapeOfPort(PortProto thePort, Point2D selectPt, boolean forWiringTool, double arcWidth)
	{
		// look down to the bottom level node/port
		PortOriginal fp = new PortOriginal(this, thePort);
		AffineTransform trans = fp.getTransformToTop();
		NodeInst ni = fp.getBottomNodeInst();
		PortProto pp = fp.getBottomPortProto();

		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		Technology tech = np.getTechnology();
		Poly poly = tech.getShapeOfPort(ni, (PrimitivePort)pp, selectPt);

        // we only compress port if it is a rectangle
        Rectangle2D box = poly.getBox();
        if (forWiringTool && (box != null)) {
            if ((arcWidth != -1) && (np.getFunction() == PrimitiveNode.Function.CONTACT)) {
                // reduce the port size such that the connecting arc's width cannot extend
                // beyond the width of the contact
                SizeOffset so = ((PrimitiveNode)np).getProtoSizeOffset();
                double width = ni.getXSize() - so.getHighXOffset() - so.getLowXOffset();
                double height = ni.getYSize() - so.getHighYOffset() - so.getLowYOffset();
                double newportwidth = width - arcWidth;
                double newportheight = height - arcWidth;
                if (newportwidth < 0) newportwidth = 0; // if arc bigger than contact, make port size 0 so it's centered
                if (newportheight < 0) newportheight = 0;
                double offsetX = 0, offsetY = 0;
                if (newportwidth < box.getWidth()) {
                    // port size needs to be reduced if desired width less than actual width
                    offsetX = 0.5*(newportwidth - box.getWidth());
                    box = new Rectangle2D.Double(box.getX()-offsetX, box.getY(), box.getWidth()+2*offsetX, box.getHeight());
                }
                if (newportheight < box.getHeight()) {
                    // port size needs to be reduced if desired height less than actual height
                    offsetY = 0.5*(newportheight - box.getHeight());
                    box = new Rectangle2D.Double(box.getX(), box.getY()-offsetY, box.getWidth(), box.getHeight()+2*offsetY);
                }
            }
            if (ni.getXSize() == np.getDefWidth()) {
                double x = poly.getCenterX();
                box = new Rectangle2D.Double(x, box.getMinY(), 0, box.getHeight());
            }
            if (ni.getYSize() == np.getDefHeight()) {
                double y = poly.getCenterY();
                box = new Rectangle2D.Double(box.getMinX(), y, box.getWidth(), 0);
            }
            poly = new Poly(box);
        }

        // transform port out to current level
		poly.transform(trans);
		return poly;
	}

	/**
	 * Method to return the "outline" information on this NodeInst.
	 * Outline information is a set of coordinate points that further
	 * refines the NodeInst description.  It is typically used in
	 * Artwork primitives to give them a precise shape.  It is also
	 * used by pure-layer nodes in all layout technologies to allow
	 * them to take any shape.  It is even used by many MOS
	 * transistors to allow a precise gate path to be specified.
	 * @return an array of Point2D in database coordinates.
	 */
	public Point2D [] getTrace()
	{
		Variable var = getVar(TRACE, Point2D[].class);
		if (var == null) return null;
		Object obj = var.getObject();
		if (obj instanceof Object[]) return (Point2D []) obj;
		return null;
	}

	/**
	 * Method to set the "outline" information on this NodeInst.
	 * Outline information is a set of coordinate points that further
	 * refines the NodeInst description.  It is typically used in
	 * Artwork primitives to give them a precise shape.  It is also
	 * used by pure-layer nodes in all layout technologies to allow
	 * them to take any shape.  It is even used by many MOS
	 * transistors to allow a precise gate path to be specified.
	 * @param points an array of Point2D values in database coordinates.
	 */
	public void setTrace(Point2D [] points)
	{
		double lX = points[0].getX();
		double hX = lX;
		double lY = points[0].getY();
		double hY = lY;
		for(int i=1; i<points.length; i++)
		{
			double x = points[i].getX();
			if (x < lX) lX = x;
			if (x > hX) hX = x;
			double y = points[i].getY();
			if (y < lY) lY = y;
			if (y > hY) hY = y;
		}
		double newCX = (lX + hX) / 2;
		double newCY = (lY + hY) / 2;
		double newSX = hX - lX;
		double newSY = hY - lY;
		for(int i=0; i<points.length; i++)
			points[i].setLocation(points[i].getX() - newCX, points[i].getY() - newCY);

		// update the points
		newVar(NodeInst.TRACE, points);
		modifyInstance(newCX-getAnchorCenterX(),newCY-getAnchorCenterY(), newSX-getXSize(),
			newSY-getYSize(), -getAngle());
	}

	/**
	 * Method to tell whether the outline information on this NodeInst wraps.
	 * Wrapping outline information applies to closed figures, such as pure-layer nodes.
	 * Nodes that do not wrap include serpentine transistors, splines, and opened polygons.
	 * @return true if this node's outline information wraps.
	 */
	public boolean traceWraps()
	{
		if (protoType == Artwork.tech.splineNode ||
			protoType == Artwork.tech.openedPolygonNode ||
			protoType == Artwork.tech.openedDottedPolygonNode ||
			protoType == Artwork.tech.openedDashedPolygonNode ||
			protoType == Artwork.tech.openedThickerPolygonNode)
				return false;
		if (isFET()) return false;
		return true;
	}

	/****************************** PORTS ******************************/

	/**
	 * Method to return an Iterator for all PortInsts on this NodeInst.
	 * @return an Iterator for all PortInsts on this NodeInst.
	 */
	public Iterator getPortInsts() { return ArrayIterator.iterator(portInsts); }

	/**
	 * Method to return the number of PortInsts on this NodeInst.
	 * @return the number of PortInsts on this NodeInst.
	 */
	public int getNumPortInsts() { return portInsts.length; }

	/**
	 * Method to return the PortInst at specified position.
	 * @param portIndex specified position of PortInst.
	 * @return the PortProto at specified position..
	 */
	public PortInst getPortInst(int portIndex) { return portInsts[portIndex]; }

	/**
	 * Method to return the only PortInst on this NodeInst.
	 * This is quite useful for vias and pins which have only one PortInst.
	 * @return the only PortInst on this NodeInst.
	 * If there are more than 1 PortInst, then return null.
	 */
	public PortInst getOnlyPortInst()
	{
		int sz = portInsts.length;
		if (sz != 1)
		{
			System.out.println("NodeInst.getOnlyPortInst: " + parent +
				", " + this + " doesn't have just one port, it has " + sz);
			return null;
		}
		return portInsts[0];
	}

	/**
	 * Method to return the named PortInst on this NodeInst.
	 * @param name the name of the PortInst.
	 * @return the selected PortInst.  If the name is not found, return null.
	 */
	public PortInst findPortInst(String name)
	{
		PortProto pp = protoType.findPortProto(name);
		if (pp == null) return null;
		return portInsts[pp.getPortIndex()];
	}

	/**
	 * Method to return the PortInst on this NodeInst that is closest to a point.
	 * @param w the point of interest.
	 * @return the closest PortInst to that point.
	 */
	public PortInst findClosestPortInst(Point2D w)
	{
		double bestDist = Double.MAX_VALUE;
		PortInst bestPi = null;
		for (int i = 0; i < portInsts.length; i++)
		{
			PortInst pi = portInsts[i];
			Poly piPoly = pi.getPoly();
			Point2D piPt = new Point2D.Double(piPoly.getCenterX(), piPoly.getCenterY());
			double thisDist = piPt.distance(w);
			if (thisDist < bestDist)
			{
				bestDist = thisDist;
				bestPi = pi;
			}
		}
		return bestPi;
	}

	/**
	 * Method to return the Portinst on this NodeInst with a given prototype.
	 * @param pp the PortProto to find.
	 * @return the selected PortInst.  If the PortProto is not found,
	 * return null.
	 */
	public PortInst findPortInstFromProto(PortProto pp)
	{
		return portInsts[pp.getPortIndex()];
	}

	/**
	 * Method to create a new PortInst on this NodeInst.
	 * @param pp the prototype of the new PortInst.
	 */
	public void addPortInst(PortProto pp)
	{
		linkPortInst(PortInst.newInstance(pp, this));
	}

	/**
	 * Method to link saved PortInst on this NodeInst.
	 * @param pi saved PortInst.
	 */
	public void linkPortInst(PortInst pi)
	{
		int portIndex = pi.getPortIndex();
		PortInst[] newPortInsts = new PortInst[portInsts.length + 1];
		System.arraycopy(portInsts, 0, newPortInsts, 0, portIndex);
		newPortInsts[portIndex] = pi;
		System.arraycopy(portInsts, portIndex, newPortInsts, portIndex + 1, portInsts.length - portIndex);
		portInsts = newPortInsts;
	}

	/**
	 * Method to move PortInst on this NodeInst.
	 * @param oldPortIndex old port index of the PortInst.
	 */
	public void movePortInst(int oldPortIndex)
	{
		PortInst pi = portInsts[oldPortIndex];
		int portIndex = pi.getPortIndex();
		if (portIndex > oldPortIndex)
			System.arraycopy(portInsts, oldPortIndex + 1, portInsts, oldPortIndex, portIndex - oldPortIndex);
		else if (portIndex < oldPortIndex)
			System.arraycopy(portInsts, portIndex, portInsts, portIndex + 1, oldPortIndex - portIndex);
		portInsts[portIndex] = pi;
	}

	/**
	 * Method to delete a PortInst from this NodeInst.
	 * @param pp the prototype of the PortInst to remove.
	 * @return deleted PortInst
	 */
	public PortInst removePortInst(PortProto pp)
	{
		int portIndex = pp.getPortIndex();
		PortInst pi = portInsts[portIndex];
		PortInst[] newPortInsts = portInsts.length > 1 ? new PortInst[portInsts.length - 1] : NULL_PORT_INST_ARRAY;
		System.arraycopy(portInsts, 0, newPortInsts, 0, portIndex);
		System.arraycopy(portInsts, portIndex + 1, newPortInsts, portIndex, newPortInsts.length - portIndex);
		portInsts = newPortInsts;
		return pi;
	}

    /** 
     * Method to get the Schematic Cell from a NodeInst icon
     * @return the equivalent view of the prototype, or null if none
     * (such as for primitive)
     */
    public Cell getProtoEquivalent()
    {
        if (!(protoType instanceof Cell)) return null;            // primitive
        return ((Cell)protoType).getEquivalent();
    }

	/**
	 * Method to add an Export to this NodeInst.
	 * @param e the Export to add.
	 */
	public void addExport(Export e)
	{
		Export[] newExports = new Export[exports.length + 1];
		System.arraycopy(exports, 0, newExports, 0, exports.length);
		newExports[newExports.length - 1] = e;
		exports = newExports;
		redoGeometric();
	}

	/**
	 * Method to remove an Export from this NodeInst.
	 * @param e the Export to remove.
	 */
	public void removeExport(Export e)
	{
		int i = 0;
		while (i < exports.length && exports[i] != e) i++;
		if (i >= exports.length)
			throw new RuntimeException("Tried to remove a non-existant export");
		Export[] newExports = exports.length > 1 ? new Export[exports.length - 1] : NULL_EXPORT_ARRAY;
		System.arraycopy(exports, 0, newExports, 0, i);
		System.arraycopy(exports, i + 1, newExports, i, newExports.length - i);
		exports = newExports;
		redoGeometric();
	}

	/**
	 * Method to return an Iterator over all Exports on this NodeInst.
	 * @return an Iterator over all Exports on this NodeInst.
	 */
	public Iterator getExports() { return ArrayIterator.iterator(exports); }

	/**
	 * Method to return the number of Exports on this NodeInst.
	 * @return the number of Exports on this NodeInst.
	 */
	public int getNumExports() { return exports.length; }

	/**
	 * Method to associate the ports between two NodeInsts.
	 * @param ni1 the first NodeInst to associate.
	 * @param ni2 the second NodeInst to associate.
	 * @param ignorePortNames true to ignore port names and use only positions.
	 * @return an array of PortAssociation objects that associates ports on this NodeInst
	 * with those on the other one.  returns null if there is an error.
	 */
	private PortAssociation [] portAssociate(NodeInst ni1, NodeInst ni2, boolean ignorePortNames)
	{
		// gather information about NodeInst 1 (ports, Poly, location, association)
		int total1 = ni1.getProto().getNumPorts();
		PortAssociation [] portInfo1 = new PortAssociation[total1];
		int k = 0;
		for(Iterator it1 = ni1.getPortInsts(); it1.hasNext(); )
		{
			PortInst pi1 = (PortInst)it1.next();
			portInfo1[k] = new PortAssociation();
			portInfo1[k].portInst = pi1;
			portInfo1[k].poly = pi1.getPoly();
			portInfo1[k].pos = new Point2D.Double(portInfo1[k].poly.getCenterX(), portInfo1[k].poly.getCenterY());
			portInfo1[k].assn = null;
			k++;
		}

		// gather information about NodeInst 2 (ports, Poly, location, association)
		int total2 = ni2.getProto().getNumPorts();
		PortAssociation [] portInfo2 = new PortAssociation[total2];
		k = 0;
		for(Iterator it2 = ni2.getPortInsts(); it2.hasNext(); )
		{
			PortInst pi2 = (PortInst)it2.next();
			portInfo2[k] = new PortAssociation();
			portInfo2[k].portInst = pi2;
			portInfo2[k].poly = pi2.getPoly();
			portInfo2[k].pos = new Point2D.Double(portInfo2[k].poly.getCenterX(), portInfo2[k].poly.getCenterY());
			portInfo2[k].assn = null;
			k++;
		}

		// associate on port name matches
		if (!ignorePortNames)
		{
			for(int i1 = 0; i1 < total1; i1++)
			{
				PortInst pi1 = portInfo1[i1].portInst;
				for(int i2 = 0; i2 < total2; i2++)
				{
					PortInst pi2 = portInfo2[i2].portInst;
					if (portInfo2[i2].assn != null) continue;

					// stop if the ports have different name
					if (!pi2.getPortProto().getName().equalsIgnoreCase(pi1.getPortProto().getName())) continue;

					// store the correct association of ports
					portInfo1[i1].assn = pi2;
					portInfo2[i2].assn = pi1;
				}
			}
		}

		// make two passes, the first stricter
		for(int pass=0; pass<2; pass++)
		{
			// associate ports that are in the same position
			for(int i1 = 0; i1 < total1; i1++)
			{
				PortInst pi1 = portInfo1[i1].portInst;
				if (portInfo1[i1].assn != null) continue;

				for(int i2 = 0; i2 < total2; i2++)
				{
					// if this port is already associated, ignore it
					PortInst pi2 = portInfo2[i2].portInst;
					if (portInfo2[i2].assn != null) continue;

					// if the port centers are different, go no further
					if (portInfo2[i2].pos.getX() != portInfo1[i1].pos.getX() ||
						portInfo2[i2].pos.getY() != portInfo1[i1].pos.getY()) continue;

					// compare actual polygons to be sure
					if (pass == 0)
					{
						if (!portInfo1[i1].poly.polySame(portInfo2[i2].poly)) continue;
					}

					// handle confusion if multiple ports have the same polygon
//					if (assn[i1] != null)
//					{
//						PortProto mpt = assn[i1];
//
//						// see if one of the associations has the same connectivity
//						for(j=0; mpt->connects[j] != NOARCPROTO && pp1->connects[j] != NOARCPROTO; j++)
//							if (mpt->connects[j] != pp1->connects[j]) break;
//						if (mpt->connects[j] == NOARCPROTO && pp1->connects[j] == NOARCPROTO) continue;
//					}

					// store the correct association of ports
					portInfo1[i1].assn = pi2;
					portInfo2[i2].assn = pi1;
				}
			}
		}
		return portInfo1;
	}

	/****************************** CONNECTIONS ******************************/

	/**
	 * sanity check function, used by Connection.checkobj
	 */
	private boolean containsConnection(Connection c)
	{
		return connections.contains(c);
	}

	/**
	 * Method to recomputes the "Wiped" flag bit on this NodeInst.
	 * The Wiped flag is set if the NodeInst is "wipable" and if it is connected to
	 * ArcInsts that wipe.  Wiping means erasing.  Typically, pin NodeInsts can be wiped.
	 * This means that when an arc connects to the pin, it is no longer drawn.
	 */
	public void computeWipeState()
	{
		clearWiped();
		NodeProto np = getProto();
		if (np instanceof PrimitiveNode && ((PrimitiveNode)np).isArcsWipe())
		{
			for(Iterator it = getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				ArcInst ai = con.getArc();
				if (ai.getProto().isWipable())
				{
					setWiped();
					break;
				}
			}
		}
	}

	/**
	 * Method to determine whether the display of this pin NodeInst should be supressed.
	 * In Schematics technologies, pins are not displayed if there are 1 or 2 connections,
	 * but are shown for 0 or 3 or more connections (called "Steiner points").
	 * @return true if this pin NodeInst should be supressed.
	 */
	public boolean pinUseCount()
	{
		if (connections.size() > 2) return false;
		if (getNumExports() != 0) return true;
		if (connections.size() == 0) return false;
		return true;
	}

	/**
	 * Method to tell whether this NodeInst is a pin that is "inline".
	 * An inline pin is one that connects in the middle between two arcs.
	 * The arcs must line up such that the pin can be removed and the arcs replaced with a single
	 * arc that is in the same place as the former two.
	 * @return true if this NodeInst is an inline pin.
	 */
	public boolean isInlinePin()
	{
		if (protoType.getFunction() != PrimitiveNode.Function.PIN) return false;

		// see if the pin is connected to two arcs along the same slope
		int j = 0;
		ArcInst [] reconAr = new ArcInst[2];
		Point2D [] delta = new Point2D.Double[2];
		for(Iterator it = getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			if (j >= 2) { j = 0;   break; }
			ArcInst ai = con.getArc();
			reconAr[j] = ai;
			EPoint thisLocation = con.getLocation();
            EPoint thatLocation = ai.getLocation(1 - con.getEndIndex());
//			Connection thisCon = ai.getHead();
//			Connection thatCon = ai.getTail();
//			if (thatCon == con)
//			{
//				thisCon = ai.getTail();
//				thatCon = ai.getHead();
//			}
        	delta[j] = new Point2D.Double(thatLocation.getX() - thisLocation.getX(),
				thatLocation.getY() - thisLocation.getY());
			j++;
		}
		if (j != 2) return false;

		// must connect to two arcs of the same type and width
		if (reconAr[0].getProto() != reconAr[1].getProto()) return false;
		if (reconAr[0].getWidth() != reconAr[1].getWidth()) return false;

		// arcs must be along the same angle, and not be curved
		if (delta[0].getX() != 0 || delta[0].getY() != 0 || delta[1].getX() != 0 || delta[1].getY() != 0)
		{
			Point2D zero = new Point2D.Double(0, 0);
			if ((delta[0].getX() != 0 || delta[0].getY() != 0) && (delta[1].getX() != 0 || delta[1].getY() != 0) &&
				DBMath.figureAngle(zero, delta[0]) !=
				DBMath.figureAngle(delta[1], zero)) return false;
		}
		if (reconAr[0].getVar(ArcInst.ARC_RADIUS) != null) return false;
		if (reconAr[1].getVar(ArcInst.ARC_RADIUS) != null) return false;

		// the arcs must not have network names on them
		Name name0 = reconAr[0].getNameKey();
		Name name1 = reconAr[1].getNameKey();
		if (name0 != null && name1 != null)
		{
			if (!name0.isTempname() && !name1.isTempname()) return false;
		}
		return true;
	}

	/**
	 * Method to tell whether this NodeInst can connect to a given ArcProto.
	 * @param arc the type of arc to test for.
	 * @return the first port that can connect to this node, or
	 * null, if no such port on this node exists.
	 */
	public PortProto connectsTo(ArcProto arc)
	{
		for (int i = 0, numPorts = protoType.getNumPorts(); i < numPorts; i++)
		{
			PortProto pp = protoType.getPort(i);
			if (pp.connectsTo(arc))
				return pp;
		}
		return null;
	}

	/**
	 * Method to add an Connection to this NodeInst.
	 * @param c the Connection to add.
	 */
	public void addConnection(Connection c)
	{
		connections.add(c);
		NodeInst ni = c.getPortInst().getNodeInst();
		ni.computeWipeState();
		redoGeometric();
	}

	/**
	 * Method to remove an Connection from this NodeInst.
	 * @param c the Connection to remove.
	 */
	public void removeConnection(Connection c)
	{
		connections.remove(c);
		NodeInst ni = c.getPortInst().getNodeInst();
		ni.computeWipeState();
		redoGeometric();
	}

	/**
	 * Method to return an Iterator over all Connections on this NodeInst.
	 * @return an Iterator over all Connections on this NodeInst.
	 */
	public Iterator getConnections()
	{
		return connections.iterator();
	}

	/**
	 * Method to return the number of Connections on this NodeInst.
	 * @return the number of Connections on this NodeInst.
	 */
	public int getNumConnections() { return connections.size(); }

	/****************************** TEXT ******************************/

	/**
	 * Method to return the name key of this NodeInst.
	 * @return the name key of this NodeInst, null if there is no name.
	 */
	public Name getNameKey()
	{
		return d.name;
	}

	/**
	 * Low-level access method to change name of this NodeInst.
	 * @param name new name of this NodeInst.
	 * @param duplicate new duplicate number of this NodeInst or negative value.
	 */
	public void lowLevelRename(Name name, int duplicate)
	{
		parent.removeNodeName(this);
        d = d.withName(name, parent.fixupNodeDuplicate(name, duplicate));
		parent.addNodeName(this);
		parent.checkInvariants();
	}

	/**
	 * Method to return the duplicate index of this NodeInst.
	 * @return the duplicate index of this NodeInst.
	 */
	public int getDuplicate() { return d.duplicate; }

	/**
	 * Returns the TextDescriptor on this NodeInst selected by name.
	 * This name may be a name of variable on this NodeInst or one of
	 * the special names <code>NodeInst.NODE_NAME_TD</code> or <code>Node.NODE_PROTO_TD</code>.
	 * Other strings are not considered special, even they are equal to the
	 * special name. In other words, special name is compared by "==" other than
	 * by "equals".
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param varName name of variable or special name.
	 * @return the TextDescriptor on this NodeInst.
	 */
	public ImmutableTextDescriptor getTextDescriptor(String varName)
	{
		if (varName == NODE_NAME_TD) return d.nameDescriptor;
		if (varName == NODE_PROTO_TD) return d.protoDescriptor;
		return super.getTextDescriptor(varName);
	}

    /**
	 * Updates the TextDescriptor on this NodeInst selected by varName.
	 * The varName may be a name of variable on this NodeInst or one of
	 * the special names <code>NodeInst.NODE_NAME_TD</code> or <code>NodeInst.NODE_PROTO_ID</code>.
	 * If varName doesn't select any text descriptor, no action is performed.
	 * Other strings are not considered special, even they are equal to the
	 * special name. In other words, special name is compared by "==" other than
	 * by "equals".
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param varName name of variable or special name.
	 * @param td new value TextDescriptor
     * @return old text descriptor or null
     * @throws IllegalArgumentException if TextDescriptor with specified name not found on this NodeInst.
	 */
	public ImmutableTextDescriptor lowLevelSetTextDescriptor(String varName, ImmutableTextDescriptor td)
	{
		if (varName == NODE_NAME_TD)
        {
            ImmutableTextDescriptor oldDescriptor = d.nameDescriptor;
			d = d.withNameDescriptor(td.withDisplayWithoutParamAndCode());
            return oldDescriptor;
        }
		if (varName == NODE_PROTO_TD)
        {
            ImmutableTextDescriptor oldDescriptor = d.protoDescriptor;
			d = d.withProtoDescriptor(td.withDisplayWithoutParamAndCode());
            return oldDescriptor;
        }
		return super.lowLevelSetTextDescriptor(varName, td);
	}

	/**
	 * Method to determine whether a variable key on NodeInst is deprecated.
	 * Deprecated variable keys are those that were used in old versions of Electric,
	 * but are no longer valid.
	 * @param key the key of the variable.
	 * @return true if the variable key is deprecated.
	 */
	public boolean isDeprecatedVariable(Variable.Key key)
	{
		if (key == NODE_NAME) return true;
		return super.isDeprecatedVariable(key);
	}
	
	/**
	 * Method to handle special case side-effects of setting variables on this NodeInst.
	 * Overrides the general method on ElectricObject.
	 * Currently it handles changes to the number-of-degrees on a circle node.
	 * @param key the Variable key that has changed on this NodeInst.
	 */
	public void checkPossibleVariableEffects(Variable.Key key)
	{
		if (key == Artwork.ART_DEGREES || key == TRACE)
			lowLevelModify(0, 0, 0, 0, 0);
	}

	/**
	 * Method to determine whether this is an Invisible Pin with text.
	 * If so, it should not be selected, but its text should be instead.
	 * @return true if this is an Invisible Pin with text.
	 */
	public boolean isInvisiblePinWithText()
	{
		if (getProto() != Generic.tech.invisiblePinNode) return false;
		if (getNumExports() != 0) return true;
		if (numDisplayableVariables(false) != 0) return true;
		return false;
	}

	/**
	 * Method to tell if this NodeInst is an invisible-pin with text that is offset away from the pin center.
	 * Since invisible pins with text are never shown, their text should not be offset.
	 * @param repair true to fix such text by changing its offset to (0,0).
	 * If this is selected, the change is made directly (so this must be called from
	 * inside of a job).
	 * @return the coordinates of the pin, if it has offset text.
	 * Returns null if the pin is valid (or if it isn't a pin or doesn't have text).
	 */
	public Point2D invisiblePinWithOffsetText(boolean repair)
	{
		// look for pins that are invisible and have text in different location
		if (protoType.getFunction() != PrimitiveNode.Function.PIN) return null;
		if (this.getNumConnections() != 0) return null;

		// stop now if this isn't invisible
		if (protoType != Generic.tech.invisiblePinNode)
		{
			Technology tech = protoType.getTechnology();
			Poly [] polyList = tech.getShapeOfNode(this);
			if (polyList.length > 0)
			{
				Poly.Type style = polyList[0].getStyle();
				if (!style.isText()) return null;
			}
		}

		// invisible: look for offset text
		for(Iterator it = getExports(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			TextDescriptor td = pp.getTextDescriptor(Export.EXPORT_NAME_TD);
			if (td.getXOff() != 0 || td.getYOff() != 0)
			{
				Point2D retVal = new Point2D.Double(getAnchorCenterX() + td.getXOff(), getAnchorCenterY() +td.getYOff());
				if (repair) pp.setOff(Export.EXPORT_NAME_TD, 0, 0);
				return retVal;
			}
		}

		for(Iterator it = this.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.isDisplay() && (var.getXOff() != 0 || var.getYOff() != 0))
			{
				Point2D retVal = new Point2D.Double(getAnchorCenterX() + var.getXOff(), getAnchorCenterY() +var.getYOff());
				if (repair) var.setOff(0, 0);
				return retVal;
			}
		}
		return null;
	}

	/**
	 * Method to describe this NodeInst as a string.
     * @param withQuotes to wrap description between quotes
	 * @return a description of this NodeInst as a string.
	 */
	public String describe(boolean withQuotes)
	{
		String description = protoType.describe(false);
		String name = (withQuotes) ? "'"+getName()+"'" : getName();
		if (name != null) description += "[" + name + "]";
		return description;
	}

   /**
     * Compares NodeInsts by their Cells and names.
     * @param obj the other NodeInst.
     * @return a comparison between the NodeInsts.
     */
	public int compareTo(Object obj)
	{
		NodeInst that = (NodeInst)obj;
		int cmp;
		if (this.parent != that.parent)
		{
			cmp = this.parent.compareTo(that.parent);
			if (cmp != 0) return cmp;
		}
		cmp = this.getName().compareTo(that.getName());
		if (cmp != 0) return cmp;
		return this.d.duplicate - that.d.duplicate;
	}

	/**
	 * Returns a printable version of this NodeInst.
	 * @return a printable version of this NodeInst.
	 */
	public String toString()
	{
        if (protoType == null) return "NodeInst no protoType";
//		return "NodeInst '" + protoType.getName() + "'";
        return "node " + describe(true);
	}

	/****************************** MISCELLANEOUS ******************************/

	/**
	 * Method to return the prototype of this NodeInst.
	 * @return the prototype of this NodeInst.
	 */
	public NodeProto getProto() { return protoType; }

	/**
	 * Method to return the number of actual NodeProtos which
	 * produced this Nodable.
	 * @return number of actual NodeProtos.
	 */
	public int getNumActualProtos() { return 1; }

	/**
	 * Method to return the i-th actual NodeProto which produced
	 * this Nodable.
	 * @param i specified index of actual NodeProto.
	 * @return actual NodeProt.
	 */
	public NodeProto getActualProto(int i) { return (i == 0 ? protoType : null); }

    // JKG: trying this out
    /**
     * Implements Nodable.contains(NodeInst ni).
     * True if ni is the same as this.  False otherwise
     */
    public boolean contains(NodeInst ni, int arrayIndex) {
        if (ni == this && arrayIndex == 0) return true;
        return false;
    }

    /**
     * Get the NodeInst associated with this Nodable
     * For NodeInsts, this returns itself.
     * @return the NodeInst associate with this Nodable
     */
    public NodeInst getNodeInst() { return this; }

	/**
	 * Method to return the function of this NodeProto.
	 * The Function is a technology-independent description of the behavior of this NodeProto.
	 * @return function the function of this NodeProto.
	 */
	public PrimitiveNode.Function getFunction()
	{
		if (protoType instanceof Cell) return PrimitiveNode.Function.UNKNOWN;

		PrimitiveNode np = (PrimitiveNode)protoType;
		return np.getTechnology().getPrimitiveFunction(np, getTechSpecific());
	}

    /** 
     * Method to see if this NodeInst is a Primitive Transistor.
     * Use getFunction() to determine what specific transitor type it is,
     * if any.
     * @return true if NodeInst represents Primitive Transistor
     */
    public boolean isPrimitiveTransistor()
    {
        PrimitiveNode.Function func = protoType.getFunction(); // note bypasses ni.getFunction() call
        if (func == PrimitiveNode.Function.TRANS ||         // covers all Schematic trans
            func == PrimitiveNode.Function.TRANS4 ||        // covers all Schematic trans4
            func == PrimitiveNode.Function.TRANMOS ||       // covers all MoCMOS nmos gates
            func == PrimitiveNode.Function.TRAPMOS          // covers all MoCMOS pmos gates
            )
            return true;
        return false;
    }

	/**
	 * Method to return true if a PrimitiveNode is acting as implant.
	 * This is used for coverage implant function.
	 */
	public boolean isPrimtiveSubstrateNode()
	{
		if (getFunction() != PrimitiveNode.Function.NODE) return false;
		PrimitiveNode np = (PrimitiveNode)protoType;
		if (np.getLayers().length != 1) return false;
		return (np.getLayers()[0].getLayer().getFunction().isSubstrate());
	}

    /**
     * Method to see if this NodeInst is a Serpentine Transistor.
     * @return true if NodeInst is a serpentine transistor.
     */
    public boolean isSerpentineTransistor() {
        if (!isPrimitiveTransistor()) return false;
        PrimitiveNode pn = (PrimitiveNode)getProto();
        if (pn.isHoldsOutline() && (getTrace() != null)) return true;
        return false;
    }

	/**
	 * Method to tell whether this NodeInst is a field-effect transtor.
	 * This includes the nMOS, PMOS, and DMOS transistors, as well as the DMES and EMES transistors.
	 * @return true if this NodeInst is a field-effect transtor.
	 */
	public boolean isFET()
	{
		PrimitiveNode.Function fun = getFunction();
		if (fun == PrimitiveNode.Function.TRANMOS  || fun == PrimitiveNode.Function.TRA4NMOS ||
			fun == PrimitiveNode.Function.TRAPMOS  || fun == PrimitiveNode.Function.TRA4PMOS ||
			fun == PrimitiveNode.Function.TRADMOS  || fun == PrimitiveNode.Function.TRA4DMOS ||
			fun == PrimitiveNode.Function.TRANJFET || fun == PrimitiveNode.Function.TRA4NJFET ||
			fun == PrimitiveNode.Function.TRAPJFET || fun == PrimitiveNode.Function.TRA4PJFET ||
			fun == PrimitiveNode.Function.TRADMES  || fun == PrimitiveNode.Function.TRA4DMES ||
			fun == PrimitiveNode.Function.TRAEMES  || fun == PrimitiveNode.Function.TRA4EMES)
				return true;
		return false;
	}

	/**
	 * Method to tell whether this NodeInst is a bipolar transistor.
	 * This includes NPN and PNP transistors.
	 * @return true if this NodeInst is a bipolar transtor.
	 */
	public boolean isBipolar() {
		PrimitiveNode.Function fun = getFunction();
		return fun==PrimitiveNode.Function.TRANPN || fun==PrimitiveNode.Function.TRA4NPN || 
		       fun==PrimitiveNode.Function.TRAPNP || fun==PrimitiveNode.Function.TRA4PNP; 
	}

    /**
	 * Method to return the size of this PrimitiveNode-type NodeInst.
     * @param context the VarContext in which any evaluations take place,
     * pass in VarContext.globalContext if no context needed.
	 * @return the size of the NodeInst if it is a PrimitiveNode
	 */
	public PrimitiveNodeSize getPrimitiveNodeSize(VarContext context)
	{
        PrimitiveNodeSize size = getTransistorSize(context);
        if (size == null) // Not a transistor
            size = getResistorSize(context); // It is a resistor
        return size;
    }

    /**
	 * Method to return the size of this PrimitiveNode-type NodeInst.
     * @param context the VarContext in which any evaluations take place,
     * pass in VarContext.globalContext if no context needed.
	 * @return the size of the NodeInst if it is a PrimitiveNode
	 */
    private PrimitiveNodeSize getResistorSize(VarContext context)
    {
        if (!getFunction().isResistor()) return null;
        PrimitiveNode np = (PrimitiveNode)protoType;
		return np.getTechnology().getResistorSize(this, context);
    }

	/**
	 * Method to return the size of this transistor NodeInst.
     * @param context the VarContext in which any evaluations take place,
     * pass in VarContext.globalContext if no context needed.
	 * @return the size of the NodeInst if it is a transistor
	 */
	public TransistorSize getTransistorSize(VarContext context)
	{
        if (!isPrimitiveTransistor() || !isFET()) return null;
		PrimitiveNode np = (PrimitiveNode)protoType;
		return np.getTechnology().getTransistorSize(this, context);
	}

    /**
     * Method to set the size of this transistor or resistor NodeInst. Does
     * nothing if this is not a transistor NodeInst.
     * @param width the new width of the transistor
     * @param length the new length of the transistor
     */
    public void setPrimitiveNodeSize(double width, double length)
    {
        if (!isPrimitiveTransistor() && !isFET() && !getFunction().isResistor()) return;
		PrimitiveNode np = (PrimitiveNode)protoType;
        Job.checkChanging();
        np.getTechnology().setPrimitiveNodeSize(this, width, length);
    }

    /**
     * Method to set the size of a transistor or resistor NodeInst in this technology.
     * Width may be the area for non-FET transistors, in which case length is ignored.
     * This does nothing if the NodeInst's technology is not Schematics.
     * @param width the new width
     * @param length the new length
     */
    public void setPrimitiveNodeSize(Object width, Object length)
    {
        Technology tech = protoType.getTechnology();
        if (tech != Schematics.tech) return;
        Job.checkChanging();
        Schematics.tech.setPrimitiveNodeSize(this, width, length);
    }

	/**
	 * Method to return the length of this serpentine transistor.
	 * @return the transistor's length
	 * Returns -1 if this is not a serpentine transistor, or if the length cannot be found.
	 */
	public double getSerpentineTransistorLength()
	{
		Variable var = getVar(TRANSISTOR_LENGTH_KEY);
		if (var == null) return -1;
		Object obj = var.getObject();
		if (obj instanceof Integer)
		{
			// C Electric stored this as a "fraction", scaled by 120
			return ((Integer)obj).intValue() / 120;
		}
		if (obj instanceof Double)
		{
			return ((Double)obj).doubleValue();
		}
		if (obj instanceof String)
		{
			return TextUtils.atof((String)obj);
		}
		return -1;
	}

	/**
	 * Method to store a length value on this serpentine transistor.
	 * @param length the new length of the transistor.
	 */
    public void setSerpentineTransistorLength(double length)
    {
        updateVar(TRANSISTOR_LENGTH_KEY, new Double(length));
    }

    /**
     * Method to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.
     * @return a PortInst for the gate of the transistor
     */
    public PortInst getTransistorGatePort()
    {
		PrimitiveNode np = (PrimitiveNode)protoType;
		return np.getTechnology().getTransistorGatePort(this);
    }

    /**
     * Method to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.
     * @return a PortInst for the gate of the transistor
     */
    public PortInst getTransistorSourcePort()
    {
		PrimitiveNode np = (PrimitiveNode)protoType;
		return np.getTechnology().getTransistorSourcePort(this);
    }

	/**
	 * Method to return the emitter port of this transistor.
	 * @return the PortInst of the emitter (presuming that this node is that kind of transistor).
	 */
    public PortInst getTransistorEmitterPort()
	{
		PrimitiveNode np = (PrimitiveNode)protoType;
		return np.getTechnology().getTransistorEmitterPort(this);
    }

	/**
	 * Method to return the base port of this transistor.
	 * @return the PortInst of the base (presuming that this node is that kind of transistor).
	 */
    public PortInst getTransistorBasePort()
	{
		PrimitiveNode np = (PrimitiveNode)protoType;
		return np.getTechnology().getTransistorBasePort(this);
    }

	/**
	 * Method to return the collector port of this transistor.
	 * @return the PortInst of the collector (presuming that this node is that kind of transistor).
	 */
    public PortInst getTransistorCollectorPort()
	{
		PrimitiveNode np = (PrimitiveNode)protoType;
		return np.getTechnology().getTransistorCollectorPort(this);
    }
    
    /**
     * Method to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.
     * @return a PortInst for the gate of the transistor
     */
    public PortInst getTransistorBiasPort()
    {
		PrimitiveNode np = (PrimitiveNode)protoType;
		return np.getTechnology().getTransistorBiasPort(this);
    }
    
    /**
     * Method to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.
     * @return a PortInst for the gate of the transistor
     */
    public PortInst getTransistorDrainPort()
    {
		PrimitiveNode np = (PrimitiveNode)protoType;
		return np.getTechnology().getTransistorDrainPort(this);
    }

	/**
	 * Method to check and repair data structure errors in this NodeInst.
	 */
	public int checkAndRepair(boolean repair, List list, ErrorLogger errorLogger)
	{
		int errorCount = 0;
//		int warningCount = 0;
		double width = getXSize();
		double height = getYSize();
		String sizeMsg = null;
		if (protoType instanceof Cell)
		{
			// make sure the instance is the same size as the cell
			Rectangle2D bounds = ((Cell)protoType).getBounds();
			width = DBMath.round(bounds.getWidth());
			height = DBMath.round(bounds.getHeight());
			if (width != getXSize() || height != getYSize())
				sizeMsg = ", but prototype is ";
		} else
		{
			PrimitiveNode pn = (PrimitiveNode)protoType;
            if (pn.getTechnology().cleanUnusedNodesInLibrary(this, list))
            {
                if (errorLogger != null)
                {
                    String msg = "Prototype of node " + getName() + " is unused";
                    ErrorLogger.MessageLog error = errorLogger.logError(msg, parent, 1);
                    error.addGeom(this, true, parent, null);
                }
                if (list != null) // doesn't do anything when checkAndRepair is called during reading
                {
                    if (repair) list.add(this);
                    // This counts as 1 error, ignoring other errors
                    return 1;
                }
            }
			if (getTrace() != null)
			{
				if (pn.isHoldsOutline())
				{
					Rectangle2D bounds = new Rectangle2D.Double();
					Poly[] polys = pn.getTechnology().getShapeOfNode(this);
					for (int i = 0; i < polys.length; i++)
					{
						Poly poly = polys[i];
						if (i == 0)
							bounds.setRect(poly.getBounds2D());
						else
							Rectangle2D.union(poly.getBounds2D(), bounds, bounds);
					}
					width = DBMath.round(bounds.getWidth());
					height = DBMath.round(bounds.getHeight());
					if (width != getXSize() || height != getYSize())
						sizeMsg = " but has outline of size ";
				} else
				{
					String msg = parent + ", " + this + " has unexpected outline";
					System.out.println(msg);
					if (errorLogger != null)
					{
						ErrorLogger.MessageLog error = errorLogger.logError(msg, parent, 1);
						error.addGeom(this, true, parent, null);
					}
					if (repair)
						delVar(TRACE);
				}
			}
		}
		if (sizeMsg != null)
		{
			sizeMsg = parent + ", " + this +
				" is " + getXSize() + "x" + getYSize() + sizeMsg + width + "x" + height;
			if (repair)
			{
				checkChanging();
				sizeMsg += " (REPAIRED)";
			}
			System.out.println(sizeMsg);
			if (errorLogger != null)
			{
				ErrorLogger.MessageLog error = errorLogger.logWarning(sizeMsg, parent, 1);
				error.addGeom(this, true, parent, null);
			}
			if (repair)
			{
                d = d.withSize(width, height);
				redoGeometric();
			}
//			warningCount++;
		}
		return errorCount;
	}

	/**
	 * Method to check invariants in this NodeInst.
	 * @exception AssertionError if invariants are not valid
	 */
	public void check()
	{
		assert d.name != null;
		assert d.duplicate >= 0;

		assert nodeUsage != null;
		assert nodeUsage.getParent() == parent;
		assert nodeUsage.getProto() == protoType;
		assert nodeUsage.contains(this);
		if (protoType instanceof Cell)
		{
			int foundUsage = 0;
			for (Iterator it = ((Cell)protoType).getUsagesOf(); it.hasNext(); )
			{
				NodeUsage nu = (NodeUsage)it.next();
				if (nu == nodeUsage) foundUsage++;
			}
			assert foundUsage == 1;
		}

		assert portInsts != null;
		assert portInsts.length == protoType.getNumPorts();
		for (int i = 0; i < portInsts.length; i++)
		{
			PortProto pp = protoType.getPort(i);
			assert pp.getPortIndex() == i;
			PortInst pi = portInsts[i];
			assert pi.getPortProto() == pp;
		}
		assert exports != null;
	}

	/**
	 * Returns the basename for autonaming.
	 * @return the basename for autonaming.
	 */
	public Name getBasename()
	{
		return protoType instanceof Cell ? ((Cell)protoType).getBasename() : getFunction().getBasename();
	}

	/**
	 * Method to return the NodeUsage of this NodeInst.
	 * @return the NodeUsage of this NodeInst.
	 */
	public NodeUsage getNodeUsage() { return nodeUsage; }

	/**
	 * Low-level method to get the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the "user bits".
	 */
	public int lowLevelGetUserbits() { return d.userBits; }

	/**
	 * Low-level method to set the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @param userBits the new "user bits".
	 */
	public void lowLevelSetUserbits(int userBits) { checkChanging(); d = d.withUserBits(userBits); Undo.otherChange(this); }

	/**
	 * Method to copy the various state bits from another NodeInst to this NodeInst.
	 * @param ni the other NodeInst to copy.
	 */
	public void copyStateBits(NodeInst ni) { checkChanging(); d = d.withUserBits(ni.d.userBits); Undo.otherChange(this); }

    private void setFlag(int flag) { checkChanging(); d = d.withUserBits(d.userBits | flag); Undo.otherChange(this); }
    private void clearFlag(int flag) { checkChanging(); d = d.withUserBits(d.userBits & ~flag); Undo.otherChange(this); }
   
	/**
	 * Method to set this NodeInst to be expanded.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * Unexpanded Cell instances are shown as boxes with the node prototype names in them.
	 * The state has no meaning for instances of primitive node prototypes.
	 */
	public void setExpanded() { setFlag(ImmutableNodeInst.NEXPAND); }

	/**
	 * Method to set this NodeInst to be unexpanded.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * Unexpanded Cell instances are shown as boxes with the node prototype names in them.
	 * The state has no meaning for instances of primitive node prototypes.
	 */
	public void clearExpanded() { clearFlag(ImmutableNodeInst.NEXPAND); }

	/**
	 * Method to tell whether this NodeInst is expanded.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * Unexpanded Cell instances are shown as boxes with the node prototype names in them.
	 * The state has no meaning for instances of primitive node prototypes.
	 * @return true if this NodeInst is expanded.
	 */
	public boolean isExpanded() { return (d.userBits & ImmutableNodeInst.NEXPAND) != 0; }

	/**
	 * Method to set this NodeInst to be wiped.
	 * Wiped NodeInsts are erased.  Typically, pin NodeInsts can be wiped.
	 * This means that when an arc connects to the pin, it is no longer drawn.
	 * In order for a NodeInst to be wiped, its prototype must have the "setArcsWipe" state,
	 * and the arcs connected to it must have "setWipable" in their prototype.
	 */
	public void setWiped() { setFlag(ImmutableNodeInst.WIPED); }

	/**
	 * Method to set this NodeInst to be not wiped.
	 * Wiped NodeInsts are erased.  Typically, pin NodeInsts can be wiped.
	 * This means that when an arc connects to the pin, it is no longer drawn.
	 * In order for a NodeInst to be wiped, its prototype must have the "setArcsWipe" state,
	 * and the arcs connected to it must have "setWipable" in their prototype.
	 */
	public void clearWiped() { clearFlag(ImmutableNodeInst.WIPED); }

	/**
	 * Method to tell whether this NodeInst is wiped.
	 * Wiped NodeInsts are erased.  Typically, pin NodeInsts can be wiped.
	 * This means that when an arc connects to the pin, it is no longer drawn.
	 * In order for a NodeInst to be wiped, its prototype must have the "setArcsWipe" state,
	 * and the arcs connected to it must have "setWipable" in their prototype.
	 * @return true if this NodeInst is wiped.
	 */
	public boolean isWiped() { return (d.userBits & ImmutableNodeInst.WIPED) != 0; }

// 	/**
// 	 * Method to set this NodeInst to be shortened.
// 	 * Shortened NodeInst have been reduced in size to account for the fact that
// 	 * they are connected at nonManhattan angles and must connect smoothly.
// 	 * This state can only get set if the node's prototype has the "setCanShrink" state.
// 	 */
// 	public void setShortened() { setFlag(ImmutableNodeInst.NSHORT); }

// 	/**
// 	 * Method to set this NodeInst to be not shortened.
// 	 * Shortened NodeInst have been reduced in size to account for the fact that
// 	 * they are connected at nonManhattan angles and must connect smoothly.
// 	 * This state can only get set if the node's prototype has the "setCanShrink" state.
// 	 */
// 	public void clearShortened() { clearFlag(ImmutableNodeInst.NSHORT); }

// 	/**
// 	 * Method to tell whether this NodeInst is shortened.
// 	 * Shortened NodeInst have been reduced in size to account for the fact that
// 	 * they are connected at nonManhattan angles and must connect smoothly.
// 	 * This state can only get set if the node's prototype has the "setCanShrink" state.
// 	 * @return true if this NodeInst is shortened.
// 	 */
// 	public boolean isShortened() { return (d.userBits & ImmutableNodeInst.NSHORT) != 0; }

	/**
	 * Method to set this NodeInst to be hard-to-select.
	 * Hard-to-select NodeInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 */
	public void setHardSelect() { setFlag(ImmutableNodeInst.HARDSELECTN); }

	/**
	 * Method to set this NodeInst to be easy-to-select.
	 * Hard-to-select NodeInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 */
	public void clearHardSelect() { clearFlag(ImmutableNodeInst.HARDSELECTN); }

	/**
	 * Method to tell whether this NodeInst is hard-to-select.
	 * Hard-to-select NodeInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 * @return true if this NodeInst is hard-to-select.
	 */
	public boolean isHardSelect() { return (d.userBits & ImmutableNodeInst.HARDSELECTN) != 0; }

	/**
	 * Method to set this NodeInst to be visible-inside.
	 * A NodeInst that is "visible inside" is only drawn when viewing inside of the Cell.
	 * It is not visible from outside (meaning from higher-up the hierarchy).
	 */
	public void setVisInside() { setFlag(ImmutableNodeInst.NVISIBLEINSIDE); }

	/**
	 * Method to set this NodeInst to be not visible-inside.
	 * A NodeInst that is "visible inside" is only drawn when viewing inside of the Cell.
	 * It is not visible from outside (meaning from higher-up the hierarchy).
	 */
	public void clearVisInside() { clearFlag(ImmutableNodeInst.NVISIBLEINSIDE); }

	/**
	 * Method to tell whether this NodeInst is visible-inside.
	 * A NodeInst that is "visible inside" is only drawn when viewing inside of the Cell.
	 * It is not visible from outside (meaning from higher-up the hierarchy).
	 * @return true if this NodeInst is visible-inside.
	 */
	public boolean isVisInside() { return (d.userBits & ImmutableNodeInst.NVISIBLEINSIDE) != 0; }

	/**
	 * Method to set this NodeInst to be locked.
	 * Locked NodeInsts cannot be modified or deleted.
	 */
	public void setLocked() { setFlag(ImmutableNodeInst.NILOCKED); }

	/**
	 * Method to set this NodeInst to be unlocked.
	 * Locked NodeInsts cannot be modified or deleted.
	 */
	public void clearLocked() { clearFlag(ImmutableNodeInst.NILOCKED); }

	/**
	 * Method to tell whether this NodeInst is locked.
	 * Locked NodeInsts cannot be modified or deleted.
	 * @return true if this NodeInst is locked.
	 */
	public boolean isLocked() { return (d.userBits & ImmutableNodeInst.NILOCKED) != 0; }

	/**
	 * Method to set a Technology-specific value on this NodeInst.
	 * This is mostly used by the Schematics technology which allows variations
	 * on a NodeInst to be stored.
	 * For example, the Transistor primitive uses these bits to distinguish nMOS, pMOS, etc.
	 * @param value the Technology-specific value to store on this NodeInst.
	 */
	public void setTechSpecific(int value) {
        checkChanging();
        d = d.withUserBits((d.userBits & ~ImmutableNodeInst.NTECHBITS) | (value << ImmutableNodeInst.NTECHBITSSH) & ImmutableNodeInst.NTECHBITS);
        Undo.otherChange(this); 
    }

	/**
	 * Method to return the Technology-specific value on this NodeInst.
	 * This is mostly used by the Schematics technology which allows variations
	 * on a NodeInst to be stored.
	 * For example, the Transistor primitive uses these bits to distinguish nMOS, pMOS, etc.
	 * @return the Technology-specific value on this NodeInst.
	 */
	public int getTechSpecific() { return (d.userBits & ImmutableNodeInst.NTECHBITS) >> ImmutableNodeInst.NTECHBITSSH; }

	/**
	 * Return the Essential Bounds of this NodeInst.
	 *
	 * <p>If this is a NodeInst of a Cell, and if that Cell has
	 * Essential Bounds, then map that Cell's Essential Bounds into
	 * the coordinate space of the Cell that contains this NodeInst,
	 * and return the Rectangle2D that contains those
	 * bounds. Otherwise return null.
	 * @return the Rectangle2D containing the essential bounds or null
	 * if the essential bounds don't exist.
	 */
	public Rectangle2D findEssentialBounds() 
	{
		NodeProto np = getProto();
		if (!(np instanceof Cell)) return null;
		Rectangle2D eb = ((Cell)np).findEssentialBounds();
		if (eb==null)  return null;
		AffineTransform xForm = translateOut();
		Point2D ll = new Point2D.Double(eb.getMinX(), eb.getMinY());
		ll = xForm.transform(ll, null);
		Point2D ur = new Point2D.Double(eb.getMaxX(), eb.getMaxY());
		ur = xForm.transform(ur, null);
		double minX = Math.min(ll.getX(), ur.getX());
		double minY = Math.min(ll.getY(), ur.getY());
		double maxX = Math.max(ll.getX(), ur.getX());
		double maxY = Math.max(ll.getY(), ur.getY());
		return new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
	}

	/**
	 * Method to set a timestamp for constraint propagation on this NodeInst.
	 * This is used by the Layout constraint system.
	 * @param changeClock the timestamp for constraint propagation.
	 */
	public void setChangeClock(int changeClock) { this.changeClock = changeClock; }

	/**
	 * Method to get the timestamp for constraint propagation on this NodeInst.
	 * This is used by the Layout constraint system.
	 * @return the timestamp for constraint propagation on this NodeInst.
	 */
	public int getChangeClock() { return changeClock; }

	/**
	 * Method to set a Change object on this NodeInst.
	 * This is used during constraint propagation to tell whether this object has already been changed and by how much.
	 * @param change the Change object to be set on this NodeInst.
	 */
	public void setChange(Undo.Change change) { this.change = change; }

	/**
	 * Method to get the Change object on this NodeInst.
	 * This is used during constraint propagation to tell whether this object has already been changed and by how much.
	 * @return the Change object on this NodeInst.
	 */
	public Undo.Change getChange() { return change; }

    /**
     * This function is to compare NodeInst elements. Initiative CrossLibCopy
     * @param obj Object to compare to
     * @param buffer To store comparison messages in case of failure
     * @return True if objects represent same NodeInst
     */
    public boolean compare(Object obj, StringBuffer buffer)
	{
		if (this == obj) return (true);

        // Better if compare classes? but it will crash with obj=null
        if (obj == null || getClass() != obj.getClass())
            return (false);

        NodeInst no = (NodeInst)obj;
        if (getFunction() != no.getFunction())
        {
	        if (buffer != null)
	            buffer.append("Functions are not the same for '" + getName() + "' and '" + no.getName() + "'\n");
	        return (false);
        }

        NodeProto noProtoType = no.getProto();
        NodeProto protoType = getProto();

        if (protoType.getClass() != noProtoType.getClass())
        {
	        if (buffer != null)
	            buffer.append("Not the same node prototypes for '" + getName() + "' and '" + no.getName() + "'\n");
	        return (false);
        }

        // Comparing transformation
        if (!rotateOut().equals(no.rotateOut()))
        {
	        if (buffer != null)
	            buffer.append("Not the same rotation for '" + getName() + "' and '" + no.getName() + "'\n");
	        return (false);
        }

        // If this is Cell, no is a Cell otherwise class checker would notice
        if (protoType instanceof Cell)
        {
	        // Missing other comparisons
            return (noProtoType instanceof Cell);
        }

        // Technology only valid for PrimitiveNodes?
        PrimitiveNode np = (PrimitiveNode)protoType;
        PrimitiveNode noNp = (PrimitiveNode)noProtoType;
	    PrimitiveNode.Function function = getFunction();
	    PrimitiveNode.Function noFunc = no.getFunction();
        if (function != noFunc)
        {
	        if (buffer != null)
	            buffer.append("Not the same node prototypes for '" + getName() + "' and '" + no.getName() + "':" + function.getName() + " v/s " + noFunc.getName() + "\n");
	        return (false);
        }
        Poly[] polyList = np.getTechnology().getShapeOfNode(this);
        Poly[] noPolyList = noNp.getTechnology().getShapeOfNode(no);

        if (polyList.length != noPolyList.length)
        {
	        if (buffer != null)
	            buffer.append("Not same number of geometries in '" + getName() + "' and '" + no.getName() + "'\n");
	        return (false);
        }

        // Compare variables?
        // Has to be another way more eficient
        // Remove noCheckList if equals is implemented
        // Sort them out by a key so comparison won't be O(n2)
        List noCheckAgain = new ArrayList();
        for (int i = 0; i < polyList.length; i++)
        {
            boolean found = false;
            for (int j = 0; j < noPolyList.length; j++)
            {
                // Already found
                if (noCheckAgain.contains(noPolyList[j])) continue;
                if (polyList[i].compare(noPolyList[j], buffer))
                {
                    found = true;
                    noCheckAgain.add(noPolyList[j]);
                    break;
                }
            }
            // polyList[i] doesn't match any elem in noPolyList
            if (!found)
            {
	            if (buffer != null)
	                buffer.append("No corresponding geometry in '" + getName() + "' found in '" + no.getName() + "'\n");
	            return (false);
            }
        }
        // Ports comparison
        // Not sure if this comparison is necessary
        // @TODO simply these calls by a generic function or template
        noCheckAgain.clear();
        for (Iterator it = getPortInsts(); it.hasNext(); )
        {
            boolean found = false;
            PortInst port = (PortInst)it.next();

            for (Iterator i = no.getPortInsts(); i.hasNext();)
            {
                PortInst p = (PortInst)i.next();

                if (noCheckAgain.contains(p)) continue;

                if (port.compare(p, buffer))
                {
                    found = true;
                    noCheckAgain.add(p);
                    break;
                }
            }
            // No correspoding PortInst found
            if (!found)
            {
                if (buffer != null)
                    buffer.append("No corresponding port '" + port.getPortProto().getName() + "' found in '" + no.getName() + "'\n");
                return (false);
            }
        }

        // Comparing Exports
        noCheckAgain.clear();
		for(Iterator it = getExports(); it.hasNext(); )
		{
			Export export = (Export)it.next();
            boolean found = false;

            for (Iterator i = no.getExports(); i.hasNext();)
            {
                Export p = (Export)i.next();

                if (noCheckAgain.contains(p)) continue;

                if (export.compare(p, buffer))
                {
                    found = true;
                    noCheckAgain.add(p);
                    break;
                }
            }
            // No correspoding Export found
            if (!found)
            {
                if (buffer != null)
                    buffer.append("No corresponding export '" + export.getName() + "' found in '" + no.getName() + "'\n");
                return (false);
            }
		}

        noCheckAgain.clear();
		for(Iterator it = getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
            boolean found = false;

            for (Iterator i = no.getVariables(); i.hasNext();)
            {
                Variable p = (Variable)i.next();

                if (noCheckAgain.contains(p)) continue;

                if (var.compare(p, buffer))
                {
                    found = true;
                    noCheckAgain.add(p);
                    break;
                }
            }
            // No correspoding Variable found
            if (!found)
            {
                if (buffer != null)
                    buffer.append("No corresponding variable '" + var + "' found in '" + no.getName() + "'\n");
                return (false);
            }
		}

        return (true);
    }
}
