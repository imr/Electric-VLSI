/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Export.java
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
package com.sun.electric.database.hierarchy;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitivePort;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Iterator;

/**
 * An Export is a PortProto at the Cell level.  It points to the
 * PortInst that got exported, which identifies a NodeInst and a PortProto on that NodeInst.
 * <P>
 * An Export takes a PortInst on a NodeInst and makes it available as a PortInst
 * on instances of this NodeInst, farther up the hierarchy.
 * An Export therefore belongs to the NodeInst that is its source and also to the Cell
 * that the NodeInst belongs to.
 * The data structures look like this:
 * <P>
 * <CENTER><IMG SRC="doc-files/Export-1.gif"></CENTER>
 */
public class Export extends PortProto
{
	/** key of Varible holding reference name. */			public static final Variable.Key EXPORT_REFERENCE_NAME = ElectricObject.newKey("EXPORT_reference_name");

	// -------------------------- private data ---------------------------
	/** the PortInst that the exported port belongs to */	private PortInst originalPort;
	/** The Change object. */								private Undo.Change change;

	// -------------------- protected and private methods --------------

	/**
	 * The constructor is only called by subclassed constructors.
	 */
	protected Export()
	{
		super();
	}

	/****************************** CREATE, DELETE, MODIFY ******************************/

	/**
	 * Method to create an Export with the specified values.
	 * @param parent the Cell in which this Export resides.
	 * @param portInst the PortInst to export
	 * @param protoName the name of this Export.
	 * It may not have unprintable characters, spaces, or tabs in it.
	 * @return the newly created Export.
	 */
	public static Export newInstance(Cell parent, PortInst portInst, String protoName)
	{
		if (parent.findExport(protoName) != null)
		{
			System.out.println("Cell " + parent.describe() + " already has an export called " + protoName);
			return null;
		}
		Export pp = lowLevelAllocate();
		if (pp.lowLevelName(parent, protoName)) return null;
		if (pp.lowLevelPopulate(portInst)) return null;
		if (pp.lowLevelLink(null)) return null;

		// handle change control, constraint, and broadcast
		Undo.newObject(pp);
		return pp;
	}	

	/**
	 * Method to unlink this Export from its Cell.
	 */
	public void kill()
	{
		Collection oldPortInsts = lowLevelUnlink();

		// handle change control, constraint, and broadcast
		Undo.killExport(this, oldPortInsts);
	}

	/**
	 * Method to move this Export to a different PortInst in the Cell.
	 * The method expects both ports to be in the same place and simply shifts
	 * the arcs without re-constraining them.
	 * @param newPi the new PortInst on which to base this Export.
	 * @return true on error.
	 */
	public boolean move(PortInst newPi)
	{
		NodeInst newno = newPi.getNodeInst();
		Export newsubpt = (Export)newPi.getPortProto();
		Cell cell = (Cell)getParent();

		// error checks
		if (newno.getParent() != cell) return true;
		if (newsubpt.getParent() != newno.getProto()) return true;
		if (doesntConnect(newsubpt.getBasePort())) return true;

		// remember old state
		PortInst oldPi = this.getOriginalPort();

		// change the port origin
		lowLevelModify(newPi);

		// handle change control, constraint, and broadcast
		Undo.modifyExport(this, oldPi);
		return false;
	}

	/****************************** LOW-LEVEL IMPLEMENTATION ******************************/

	/**
	 * Low-level access method to create an Export.
	 * @return a newly allocated Export.
	 */
	public static Export lowLevelAllocate()
	{
		Export pp = new Export();
		return pp;
	}

	/**
	 * Low-level access method to fill-in the Cell parent and the name of this Export.
	 * @param parent the Cell in which this Export resides.
	 * @param protoName the name of this Export.
	 * It may not have unprintable characters, spaces, or tabs in it.
	 * @return true on error.
	 */
	public boolean lowLevelName(Cell parent, String protoName)
	{
		// initialize the parent object
		this.parent = parent;
		setProtoName(protoName);
		setParent(parent);
		return false;
	}

	/**
	 * Low-level access method to fill-in the subnode and subport of this Export.
	 * @param originalPort the PortInst that is being exported.
	 * @return true on error.
	 */
	public boolean lowLevelPopulate(PortInst originalPort)
	{
		// initialize this object
		if (originalPort == null)
		{
			System.out.println("Null port on Export " + getProtoName() + " in cell " + parent.describe());
			return true;
		}
		this.originalPort = originalPort;
		
		this.userBits = originalPort.getPortProto().lowLevelGetUserbits();
		return false;
	}

	/**
	 * Low-level access method to link this Export into its cell.
	 * @param oldPortInsts collection which contains portInsts of this Export for Undo or null.
	 * @return true on error.
	 */
	public boolean lowLevelLink(Collection oldPortInsts)
	{
		NodeInst originalNode = originalPort.getNodeInst();
		originalNode.addExport(this);
		getParent().addPort(this, oldPortInsts);
		return false;
	}

	/**
	 * Low-level access method to unlink this Export from its cell.
	 * @return collection of deleted PortInsts of this Export.
	 */
	public Collection lowLevelUnlink()
	{
		NodeInst originalNode = originalPort.getNodeInst();
		originalNode.removeExport(this);
		return getParent().removePort(this);
	}

	/**
	 * Method to change the origin of this Export to another place in the Cell.
	 * @param newPi the new PortInst in the cell that will hold this Export.
	 */
	public void lowLevelModify(PortInst newPi)
	{
		// remove the old linkage
		NodeInst origNode = getOriginalPort().getNodeInst();
		origNode.removeExport(this);

		// create the new linkage
		NodeInst newNodeInst = newPi.getNodeInst();
		PortProto newPortProto = newPi.getPortProto();
		newNodeInst.addExport(this);
		originalPort = newNodeInst.findPortInstFromProto(newPortProto);

		// update all port characteristics exported from this one
//		this->userbits = (this->userbits & STATEBITS) |
//			(newPortProto->userbits & (PORTANGLE|PORTARANGE|PORTNET|PORTISOLATED));
		changeallports();
	}

	/****************************** GRAPHICS ******************************/

	/**
	 * Method to return a Poly that describes this Export name.
	 * @return a Poly that describes this Export's name.
	 */
	public Poly getNamePoly()
	{
		Poly poly = getOriginalPort().getPoly();
		double cX = poly.getCenterX();
		double cY = poly.getCenterY();
		TextDescriptor td = getTextDescriptor();
		double offX = td.getXOff();
		double offY = td.getYOff();
		TextDescriptor.Position pos = td.getPos();
		Poly.Type style = pos.getPolyType();
		Point2D [] pointList = new Point2D.Double[1];
		pointList[0] = new Point2D.Double(cX+offX, cY+offY);
		poly = new Poly(pointList);
		poly.setStyle(style);
		poly.setPort(this);
		poly.setString(getProtoName());
		poly.setTextDescriptor(td);
		return poly;
	}

	/****************************** TEXT ******************************/

	/**
	 * Routing to check whether changing of this cell allowed or not.
	 * By default checks whole database change. Overriden in subclasses.
	 */
	public void checkChanging() { Cell parent = (Cell)getParent(); if (parent != null) parent.checkChanging(); }

	/*
	 * Method to determine the appropriate Cell associated with this ElectricObject.
	 * @return the appropriate Cell associated with this ElectricObject.
	 * Returns null if no Cell can be found.
	 */
	public Cell whichCell() { return (Cell)getParent(); };

	/*
	 * Method to write a description of this Export.
	 * Displays the description in the Messages Window.
	 */
	public void getInfo()
	{
		System.out.println(" Original: " + originalPort);
		System.out.println(" Base: " + getBasePort());
		System.out.println(" Cell: " + parent.describe());
		super.getInfo();
	}

	/**
	 * Returns a printable version of this Export.
	 * @return a printable version of this Export.
	 */
	public String toString()
	{
		return "Export " + getProtoName();
	}

	/****************************** MISCELLANEOUS ******************************/

	/**
	 * Method to return the port on the NodeInst inside of the cell that is the origin of this Export.
	 * @return the port on the NodeInst inside of the cell that is the origin of this Export.
	 */
	public PortInst getOriginalPort() { return originalPort; }

	/**
	 * Method to return the base-level port that this PortProto is created from.
	 * Since this is an Export, it returns the base port of its sub-port, the port on the NodeInst
	 * from which the Export was created.
	 * @return the base-level port that this PortProto is created from.
	 */
	public PrimitivePort getBasePort()
	{
		PortProto pp = originalPort.getPortProto();
		return pp.getBasePort();
	}

	/**
	 * Method to set a Change object on this Export.
	 * This is used during constraint propagation to tell whether this object has already been changed and how.
	 * @param change the Change object to be set on this Export.
	 */
	public void setChange(Undo.Change change) { this.change = change; }

	/**
	 * Method to get the Change object on this Export.
	 * This is used during constraint propagation to tell whether this object has already been changed and how.
	 * @return the Change object on this Export.
	 */
	public Undo.Change getChange() { return change; }

	/**
	 * Method to return the PortProto that is equivalent to this in the
	 * corresponding schematic Cell.
	 * It finds the PortProto with the same name on the corresponding Cell.
	 * If there are multiple versions of the Schematic Cell return the latest.
	 * @return the PortProto that is equivalent to this in the corresponding Cell.
	 */
	public PortProto getEquivalent()
	{
		Cell equiv = ((Cell)parent).getEquivalent();
		if (equiv == parent)
			return this;
		if (equiv == null)
			return null;
		return equiv.findPortProto(getProtoNameKey());
	}

	/**
	 * Method to find the Export on another Cell that is equivalent to this Export.
	 * @param otherCell the other cell to equate.
	 * @return the Export on that other Cell which matches this Export.
	 * Returns null if none can be found.
	 */
	public Export getEquivalentPort(Cell otherCell)
	{
		/* don't waste time searching if the two views are the same */
		Cell thisCell = (Cell)getParent();
		if (thisCell == otherCell) return this;

		// this is the non-cached way to do it
		return otherCell.findExport(getProtoName());

		/* load the cache if not already there */
//		if (otherCell != thisCell->cachedequivcell)
//		{
//			for(Iterator it = thisCell.getPorts(); it.hasNext(); )
//			{
//				Export opp = (Export)it.next();
//				opp->cachedequivport = null;
//			}
//			for(Iterator it = thisCell.getPorts(); it.hasNext(); )
//			{
//				Export opp = (Export)it.next();
//				Export epp = otherCell.findExport(opp.getProtoName());
//				if (epp != null) opp->cachedequivport = epp;
//			}
//			thisCell->cachedequivcell = otherCell;
//		}
//		epp = pp->cachedequivport;
//		if (epp != null) return epp;
//
//		/* don't report errors for global ports not on icons */
//		if (epp == null)
//		{
//			if (otherCell.getView() != View.ICON || !pp.isBodyOnly())
//				System.out.println("Warning: no port in cell %s corresponding to port %s in cell %s"),
//					describenodeproto(otherCell), pp->protoname, describenodeproto(thisCell));
//		}
//		pp->cachedequivport = null;
//		return null;
	}

	/**
	 * helper method to ensure that all arcs connected to Export "pp" at
	 * instances of its Cell (or any of its export sites)
	 * can connect to Export newPP.
	 * @return true if the connection cannot be made.
	 */
	public boolean doesntConnect(PrimitivePort newPP)
	{
		Cell cell = (Cell)getParent();

		// check every instance of this node
		for(Iterator it = cell.getInstancesOf(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();

			// make sure all arcs on this port can connect
			for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
			{
				Connection con = (Connection)cIt.next();
				if (con.getPortInst().getPortProto() != this) continue;
				if (!newPP.connectsTo(con.getArc().getProto()))
				{
					System.out.println(con.getArc().describe() + " arc in cell " + ni.getParent().describe() +
						" cannot connect to port " + getProtoName());
					return true;
				}
			}

			// make sure all further exports are still valid
			for(Iterator eIt = ni.getExports(); eIt.hasNext(); )
			{
				Export oPP = (Export)eIt.next();
				if (oPP.getOriginalPort().getPortProto() != this) continue;
				if (oPP.doesntConnect(newPP)) return true;
			}
		}
		return false;
	}

	/****************************** SUPPORT ******************************/

	/**
	 * Method to change all usage of this Export because it has been moved.
	 * The various state bits are changed to reflect the new Export base.
	 */
	private void changeallports()
	{
		// look at all instances of the cell that had export motion
		recursivelyChangeAllPorts();

		// look at associated cells and change their ports
		Cell np = (Cell)getParent();
		if (np.getView() == View.ICON)
		{
			// changed an export on an icon: find contents and change it there
			Cell onp = np.contentsView();
			if (onp != null)
			{
				Export opp = getEquivalentPort(onp);
				if (opp != null)
				{
					opp.setCharacteristic(getCharacteristic());
					opp.recursivelyChangeAllPorts();
				}
			}
			return;
		}

		// see if there is an icon to change
		Cell onp = np.iconView();
		if (onp != null)
		{
			Export opp = getEquivalentPort(onp);
			if (opp != null)
			{
				opp.setCharacteristic(getCharacteristic());
				opp.recursivelyChangeAllPorts();
			}
		}
	}

	/*
	 * Method to recursively alter the state bit fields of this Export.
	 */
	private void recursivelyChangeAllPorts()
	{
		// look at all instances of the cell that had port motion
		Cell cell = (Cell)getParent();
		for(Iterator it = cell.getInstancesOf(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			// see if an instance reexports the port
			for(Iterator pIt = ni.getExports(); pIt.hasNext(); )
			{
				Export upPP = (Export)pIt.next();
				if (upPP.getOriginalPort().getPortProto() != this) continue;

				// change this port and recurse up the hierarchy
				if (upPP.lowLevelGetUserbits() != lowLevelGetUserbits())
				{
					// Should use change control here !!!
					upPP.lowLevelSetUserbits(lowLevelGetUserbits());
				}
				upPP.recursivelyChangeAllPorts();
			}
		}
	}
}
