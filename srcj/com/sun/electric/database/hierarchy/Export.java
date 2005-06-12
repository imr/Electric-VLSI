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
import com.sun.electric.technology.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.ImmutableTextDescriptor;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ViewChanges;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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
public class Export extends ElectricObject implements PortProto, Comparable
{
	/** special name for text descriptor of export name */	public static final String EXPORT_NAME_TD = new String("EXPORT_name");

	/** key of Varible holding reference name. */			public static final Variable.Key EXPORT_REFERENCE_NAME = ElectricObject.newKey("EXPORT_reference_name");

	/** set if this port should always be drawn */			private static final int PORTDRAWN =         0400000000;
	/** set to exclude this port from the icon */			private static final int BODYONLY =         01000000000;
	/** input/output/power/ground/clock state */			private static final int STATEBITS =       036000000000;
	/** input/output/power/ground/clock state */			private static final int STATEBITSSHIFTED =         036;
	/** input/output/power/ground/clock state */			private static final int STATEBITSSH =               27;
	public static final int EXPORT_BITS = PORTDRAWN | BODYONLY | STATEBITS;

	// -------------------------- private data ---------------------------
	/** The name of this Export. */							private Name name;
	/** Internal flag bits of this Export. */				private int userBits;
	/** The parent Cell of this Export. */					private Cell parent;
	/** Index of this Export in Cell ports. */				private int portIndex;
	/** The text descriptor of this Export. */				private ImmutableTextDescriptor descriptor;
	/** the PortInst that the exported port belongs to */	private PortInst originalPort;
	/** The Change object. */								private Undo.Change change;

	// -------------------- protected and private methods --------------

	/**
	 * The private constructor of Export
	 * @param parent the Cell in which this Export resides.
	 * @param protoName the name of this Export.
	 * It may not have unprintable characters, spaces, or tabs in it.
     * @param nameTextDescriptor text descriptor of this Export
	 * @param originalPort the PortInst that is being exported.
     * @param userBits flag bits of this Export.
	 * @return true on error.
	 */
	private Export(Cell parent, String protoName, ImmutableTextDescriptor nameTextDescriptor, PortInst originalPort, int userBits)
	{
		// initialize the parent object
		assert parent == originalPort.getNodeInst().getParent();
		this.parent = parent;
		this.name = Name.findName(protoName);
        if (nameTextDescriptor == null) nameTextDescriptor = ImmutableTextDescriptor.getExportTextDescriptor();
		this.descriptor = nameTextDescriptor;
		this.originalPort = originalPort;
        this.userBits = userBits & EXPORT_BITS;
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
		return newInstance(parent, portInst, protoName, true);
	}

	/**
	 * Method to create an Export with the specified values.
	 * @param parent the Cell in which this Export resides.
	 * @param portInst the PortInst to export
	 * @param protoName the name of this Export.
	 * It may not have unprintable characters, spaces, or tabs in it.
	 * @param createOnIcon true to create an equivalent export on any associated icon.
	 * @return the newly created Export.
	 */
	public static Export newInstance(Cell parent, PortInst portInst, String protoName, boolean createOnIcon)
	{
		if (parent.findExport(protoName) != null)
		{
            String oldName = protoName;
            protoName = ElectricObject.uniqueObjectName(protoName, parent, PortProto.class);
			System.out.println("Cell " + parent.describe() + " already has an export named " + oldName +
                    ", making new export named "+protoName);
            assert(parent.findExport(protoName) == null);
		}
        PortProto originalProto = portInst.getPortProto();
        int userBits;
		if (originalProto instanceof Export)
			userBits = ((Export)originalProto).userBits;
		else
			userBits = originalProto.getCharacteristic().getBits() << STATEBITSSH;
		Export pp = newInstance(parent, protoName, smartPlacement(portInst), portInst, userBits);

		if (createOnIcon)
		{
	        // if this was made on a schematic, and an icon exists, make the export on the icon as well
	        Cell icon = parent.iconView();
	        if (icon != null && icon.findExport(protoName) == null)
	        {
	            // find analagous point to create export
	            Rectangle2D bounds = parent.getBounds();
	            double locX = portInst.getPoly().getCenterX();
	            double locY = portInst.getPoly().getCenterY();
	            Rectangle2D iconBounds = icon.getBounds();
				double newlocX = (locX - bounds.getMinX()) / bounds.getWidth() * iconBounds.getWidth() + iconBounds.getMinX();
				double bodyDX = 1;
				double distToXEdge = locX - bounds.getMinX();
				if (locX >= bounds.getCenterX())
				{
					bodyDX = -1;
					distToXEdge = bounds.getMaxX() - locX;
				}
				double newlocY = (locY - bounds.getMinY()) / bounds.getHeight() * iconBounds.getHeight() + iconBounds.getMinY();
				double bodyDY = 1;
				double distToYEdge = locY - bounds.getMinY();
				if (locY >= bounds.getCenterY())
				{
					bodyDY = -1;
					distToYEdge = bounds.getMaxY() - locY;
				}
				if (distToXEdge > distToYEdge) bodyDX = 0; else bodyDY = 0;

	            // round
	            Point2D point = new Point2D.Double(newlocX, newlocY);
	            EditWindow.gridAlign(point);
	            newlocX = point.getX();
	            newlocY = point.getY();

	            // create export in icon
	            if (!ViewChanges.makeIconExport(pp, 0, newlocX, newlocY, newlocX+bodyDX, newlocY+bodyDY, icon))
	            {
	                System.out.println("Warning: Failed to create associated export in icon "+icon.describe());
	            }
	        }
		}

		return pp;
	}	

	/**
	 * Factory method to create an Export
	 * @param parent the Cell in which this Export resides.
	 * @param protoName the name of this Export.
	 * It may not have unprintable characters, spaces, or tabs in it.
     * @param nameTextDescriptor text descriptor of this Export
	 * @param originalPort the PortInst that is being exported.
     * @param userBits flag bits of this Export.
	 * @return created Export or null on error.
	 */
    public static Export newInstance(Cell parent, String name, ImmutableTextDescriptor nameTextDescriptor, PortInst originalPort, int userBits)
    {
		// initialize this object
		if (originalPort == null)
		{
			System.out.println("Null port on Export " + name + " in cell " + parent.describe());
			return null;
		}
		NodeInst ni = originalPort.getNodeInst();
		PortProto subpp = originalPort.getPortProto();
		if (ni.getParent() != parent || subpp.getParent() != ni.getProto()/* || doesntConnect(subpp.getBasePort())*/)
		{
			System.out.println("Bad port on Export " + name + " in cell " + parent.describe());
			return null;
		}
        
        Export e = new Export(parent, name, nameTextDescriptor, originalPort, userBits);
		if (e.lowLevelLink(null)) return null;
        
		// handle change control, constraint, and broadcast
		Undo.newObject(e);
        return e;
    }
    

	/**
	 * Method to unlink this Export from its Cell.
	 */
	public void kill()
	{
		if (!isLinked())
		{
			System.out.println("Export already killed");
			return;
		}
		checkChanging();

		// disconnect arcs end exports from PortInsts of this Export
		for(Iterator it = parent.getInstancesOf(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			PortInst pi = ni.findPortInstFromProto(this);
			pi.disconnect();
		}
		
		Collection oldPortInsts = lowLevelUnlink();

		// handle change control, constraint, and broadcast
		Undo.killExport(this, oldPortInsts);
	}

	/**
	 * Method to rename this Export.
	 * @param newName the new name of this Export.
	 */
	public void rename(String newName)
	{
		checkChanging();

        // get unique name
        Cell cell = originalPort.getNodeInst().getParent();

		// special case: if changing case only, allow it
		if (!getName().equalsIgnoreCase(newName) || getName().equals(newName))
		{
			// not changing case
	        String dupName = ElectricObject.uniqueObjectName(newName, cell, PortProto.class);
	        if (!dupName.equals(newName))
	        {
	            System.out.println("Cell " + cell.describe() + " already has an export named " + newName +
	                    ", making new export named "+dupName);
	            newName = dupName;
	        }
		}

		// do the rename
		Name oldName = getNameKey();
		lowLevelRename(Name.findName(newName));

		// handle change control, constraint, and broadcast
		Undo.renameObject(this, oldName);

        // rename associated export in icon, if any
        Cell iconCell = cell.iconView();
        if ((iconCell != null) && (iconCell != cell)) {
            for (Iterator it = iconCell.getPorts(); it.hasNext(); ) {
                Export pp = (Export)it.next();
                if (pp.getName().equals(oldName.toString())) {
                    pp.rename(newName);
                    break;
                }
            }
        }
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
		checkChanging();

		NodeInst newno = newPi.getNodeInst();
		PortProto newsubpt = (PortProto)newPi.getPortProto();

		// error checks
		if (newno.getParent() != parent) return true;
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
	 * Low-level access method to rename this Export.
	 * Unless you know what you are doing, do not use this method...use "rename()" instead.
	 * @param newName the new name of this Export.
	 */
	public void lowLevelRename(Name newName)
	{
		assert isLinked();
		parent.moveExport(portIndex, newName.toString());
		this.name = newName;
	}

	/**
	 * Low-level access method to link this Export into its cell.
	 * @param oldPortInsts collection which contains portInsts of this Export for Undo or null.
	 * @return true on error.
	 */
	public boolean lowLevelLink(Collection oldPortInsts)
	{
		assert parent.isLinked();
		NodeInst originalNode = originalPort.getNodeInst();
		originalNode.addExport(this);
		parent.addExport(this, oldPortInsts);
		return false;
	}

	/**
	 * Low-level access method to unlink this Export from its cell.
	 * @return collection of deleted PortInsts of this Export.
	 */
	public Collection lowLevelUnlink()
	{
		assert isLinked();
		NodeInst originalNode = originalPort.getNodeInst();
		originalNode.removeExport(this);
		return parent.removeExport(this);
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

	/**
	 * Method to set an index of this Export in Cell ports.
	 * This is a zero-based index of ports on the Cell.
	 * @param portIndex an index of this Export in Cell ports.
	 */
	void setPortIndex(int portIndex) { this.portIndex = portIndex; }

	/**
	 * Low-level method to get the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the "user bits".
	 */
	public int lowLevelGetUserbits() { return userBits; }

	/**
	 * Low-level method to set the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @param userBits the new "user bits".
	 */
	public void lowLevelSetUserbits(int userBits) { checkChanging(); this.userBits = userBits & EXPORT_BITS; }

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
		TextDescriptor td = getTextDescriptor(EXPORT_NAME_TD);
		double offX = td.getXOff();
		double offY = td.getYOff();
		TextDescriptor.Position pos = td.getPos();
		Poly.Type style = pos.getPolyType();
		Point2D [] pointList = new Point2D.Double[1];

		// must untransform the node to apply the offset
		NodeInst ni = getOriginalPort().getNodeInst();
		if (ni.getAngle() != 0 || ni.isMirroredAboutXAxis() || ni.isMirroredAboutYAxis())
		{
			pointList[0] = new Point2D.Double(cX, cY);
			AffineTransform trans = ni.rotateIn();
			trans.transform(pointList[0], pointList[0]);
			pointList[0].setLocation(pointList[0].getX() + offX, pointList[0].getY() + offY);
			trans = ni.rotateOut();
			trans.transform(pointList[0], pointList[0]);
		} else
		{
			pointList[0] = new Point2D.Double(cX + offX, cY + offY);
		}

		poly = new Poly(pointList);
		poly.setStyle(style);
		poly.setPort(this);
		poly.setString(getName());
		poly.setTextDescriptor(td);
		return poly;
	}

	/****************************** TEXT ******************************/

	/**
	 * Method to determine the appropriate Cell associated with this ElectricObject.
	 * @return the appropriate Cell associated with this ElectricObject.
	 * Returns null if no Cell can be found.
	 */
	public Cell whichCell() { return parent; };

	/**
	 * Method to return the parent NodeProto of this Export.
	 * @return the parent NodeProto of this Export.
	 */
	public NodeProto getParent() { return parent; }

	/**
	 * Method to get the index of this Export.
	 * This is a zero-based index of ports on the Cell.
	 * @return the index of this Export.
	 */
	public int getPortIndex() { return portIndex; }

	/**
	 * Returns the TextDescriptor on this Export selected by name.
	 * This name may be a name of variable on this Export or
	 * the special name <code>NodeInst.NODE_NAME_TD</code>.
	 * Other strings are not considered special, even they are equal to the
	 * special name. In other words, special name is compared by "==" other than
	 * by "equals".
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param varName name of variable or special name.
	 * @return the TextDescriptor on this Export.
	 */
	public ImmutableTextDescriptor getTextDescriptor(String varName)
	{
		if (varName == EXPORT_NAME_TD) return descriptor;
		return super.getTextDescriptor(varName);
	}

	/**
	 * Updates the TextDescriptor on this Export selected by varName.
	 * The varName may be a name of variable on this Export or
	 * the special name <code>Export.EXPORT_NAME_TD</code>.
	 * If varName doesn't select any text descriptor, no action is performed.
	 * Other strings are not considered special, even they are equal to the
	 * special name. In other words, special name is compared by "==" other than
	 * by "equals".
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param varName name of variable or special name.
	 * @param td new value TextDescriptor
     * @return old text descriptor
     * @throws IllegalArgumentException if TextDescriptor with specified name not found on this Export.
	 */
	public ImmutableTextDescriptor lowLevelSetTextDescriptor(String varName, ImmutableTextDescriptor td)
	{
		if (varName == EXPORT_NAME_TD)
        {
            ImmutableTextDescriptor oldDescriptor = descriptor;
			descriptor = td.withDisplayWithoutParamAndCode();
            return oldDescriptor;
        }
		return super.lowLevelSetTextDescriptor(varName, td);
	}

    /**
	 * Method chooses TextDescriptor with "smart text placement"
     * of Export on specified origianl port.
     * @param originalPort original port for the Export
     * @return Immutable text descriptor with smart text placement
	 */
	private static ImmutableTextDescriptor smartPlacement(PortInst originalPort)
	{
		// handle smart text placement relative to attached object
		int smartVertical = User.getSmartVerticalPlacement();
		int smartHorizontal = User.getSmartHorizontalPlacement();
		if (smartVertical == 0 && smartHorizontal == 0) return ImmutableTextDescriptor.getExportTextDescriptor();

		// figure out location of object relative to environment
		double dx = 0, dy = 0;
		NodeInst ni = originalPort.getNodeInst();
		Rectangle2D nodeBounds = ni.getBounds();
		for(Iterator it = originalPort.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			ArcInst ai = con.getArc();
			Rectangle2D arcBounds = ai.getBounds();
			dx = arcBounds.getCenterX() - nodeBounds.getCenterX();
			dy = arcBounds.getCenterY() - nodeBounds.getCenterY();
		}

		// first move placement horizontally
		if (smartHorizontal == 2)
			// place label outside (away from center)
			dx = -dx;
		else if (smartHorizontal != 1)
			// place label inside (towards center)
			dx = 0;

		// next move placement vertically
		if (smartVertical == 2)
			// place label outside (away from center)
			dy = -dy;
		else if (smartVertical != 1)
			// place label inside (towards center)
			dy = 0;

		MutableTextDescriptor td = MutableTextDescriptor.getExportTextDescriptor();
		td.setPos(td.getPos().align(Double.compare(dx, 0), Double.compare(dy, 0)));
		return ImmutableTextDescriptor.newImmutableTextDescriptor(td);
	}
    
	/**
	 * Method to return the name key of this Export.
	 * @return the Name key of this Export.
	 */
	public Name getNameKey() { return name; }

	/**
	 * Method to return the name of this Export.
	 * @return the name of this Export.
	 */
	public String getName() { return name.toString(); }

	/**
	 * Method to return the short name of this PortProto.
	 * The short name is everything up to the first nonalphabetic character.
	 * @return the short name of this PortProto.
	 */
	public String getShortName()
	{
		String name = getNameKey().toString();
		int len = name.length();
		for(int i=0; i<len; i++)
		{
			char ch = name.charAt(i);
			if (TextUtils.isLetterOrDigit(ch)) continue;
			return name.substring(0, i);
		}
		return name;
	}

    /**
     * Compares Exports by their Cells and names.
     * @param obj the other Export.
     * @return a comparison between the Exports.
     */
	public int compareTo(Object obj)
	{
		Export that = (Export)obj;
		if (parent != that.parent)
		{
			int cmp = parent.compareTo(that.parent);
			if (cmp != 0) return cmp;
		}
		return name.toString().compareTo(that.name.toString());
	}

	/**
	 * Returns a printable version of this Export.
	 * @return a printable version of this Export.
	 */
	public String toString()
	{
		return "Export " + getName();
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
	 * Method to return true if the specified ArcProto can connect to this Export.
	 * @param arc the ArcProto to test for connectivity.
	 * @return true if this Export can connect to the ArcProto, false if it can't.
	 */
	public boolean connectsTo(ArcProto arc)
	{
		return getBasePort().connectsTo(arc);
	}

	/**
	 * Method to return the PortCharacteristic of this Exort.
	 * @return the PortCharacteristic of this Exort.
	 */
	public PortCharacteristic getCharacteristic()
	{
		PortCharacteristic characteristic = PortCharacteristic.findCharacteristic((userBits>>STATEBITSSH)&STATEBITSSHIFTED);
		return characteristic;
	}

	/**
	 * Method to set the PortCharacteristic of this Exort.
	 * @param characteristic the PortCharacteristic of this Exort.
	 */
	public void setCharacteristic(PortCharacteristic characteristic)
	{
		checkChanging();
        if (characteristic == null) characteristic = PortCharacteristic.UNKNOWN;
		userBits = (userBits & ~STATEBITS) | (characteristic.getBits() << STATEBITSSH);
	}

	/**
	 * Method to determine whether this Export is of type Power.
	 * This is determined by either having the proper Characteristic, or by
	 * having the proper name (starting with "vdd", "vcc", "pwr", or "power").
	 * @return true if this Export is of type Power.
	 */
	public boolean isPower()
	{
		PortCharacteristic ch = getCharacteristic();
		if (ch == PortCharacteristic.PWR) return true;
		if (ch != PortCharacteristic.UNKNOWN) return false;
		return isNamedPower();
	}

	/**
	 * Method to determine whether this Export has a name that suggests Power.
	 * This is determined by having a name starting with "vdd", "vcc", "pwr", or "power".
	 * @return true if this Export has a name that suggests Power.
	 */
	public boolean isNamedPower()
	{
		String name = getName().toLowerCase();
		if (name.indexOf("vdd") >= 0) return true;
		if (name.indexOf("vcc") >= 0) return true;
		if (name.indexOf("pwr") >= 0) return true;
		if (name.indexOf("power") >= 0) return true;
		return false;
	}

	/**
	 * Method to determine whether this Export is of type Ground.
	 * This is determined by either having the proper PortCharacteristic, or by
	 * having the proper name (starting with "vss", "gnd", or "ground").
	 * @return true if this Export is of type Ground.
	 */
	public boolean isGround()
	{
		PortCharacteristic ch = getCharacteristic();
		if (ch == PortCharacteristic.GND) return true;
		if (ch != PortCharacteristic.UNKNOWN) return false;
		return isNamedGround();
	}

	/**
	 * Method to determine whether this Export has a name that suggests Ground.
	 * This is determined by either having a name starting with "vss", "gnd", or "ground".
	 * @return true if this Export has a name that suggests Ground.
	 */
	public boolean isNamedGround()
	{
		String name = getName().toLowerCase();
		if (name.indexOf("vss") >= 0) return true;
		if (name.indexOf("gnd") >= 0) return true;
		if (name.indexOf("ground") >= 0) return true;
		return false;
	}

	/**
	 * Returns true if this export has its original port on Global-Partition schematics
	 * primitive.
	 * @return true if this export is Global-Partition export.
	 */
	public boolean isGlobalPartition()
	{
		return originalPort.getNodeInst().getProto() == Schematics.tech.globalPartitionNode;
	}


	/**
	 * Method to set this PortProto to be always drawn.
	 * Ports that are always drawn have their name displayed at all times, even when an arc is connected to them.
	 */
	public void setAlwaysDrawn() { checkChanging(); userBits |= PORTDRAWN; }

	/**
	 * Method to set this PortProto to be not always drawn.
	 * Ports that are always drawn have their name displayed at all times, even when an arc is connected to them.
	 */
	public void clearAlwaysDrawn() { checkChanging(); userBits &= ~PORTDRAWN; }

	/**
	 * Method to tell whether this PortProto is always drawn.
	 * Ports that are always drawn have their name displayed at all times, even when an arc is connected to them.
	 * @return true if this PortProto is always drawn.
	 */
	public boolean isAlwaysDrawn() { return (userBits & PORTDRAWN) != 0; }

	/**
	 * Method to set this PortProto to exist only in the body of a cell.
	 * Ports that exist only in the body do not have an equivalent in the icon.
	 * This is used by simulators and icon generators to recognize less significant ports.
	 */
	public void setBodyOnly() { checkChanging(); userBits |= BODYONLY; }

	/**
	 * Method to set this PortProto to exist in the body and icon of a cell.
	 * Ports that exist only in the body do not have an equivalent in the icon.
	 * This is used by simulators and icon generators to recognize less significant ports.
	 */
	public void clearBodyOnly() { checkChanging(); userBits &= ~BODYONLY; }

	/**
	 * Method to tell whether this PortProto exists only in the body of a cell.
	 * Ports that exist only in the body do not have an equivalent in the icon.
	 * This is used by simulators and icon generators to recognize less significant ports.
	 * @return true if this PortProto exists only in the body of a cell.
	 */
	public boolean isBodyOnly() { return (userBits & BODYONLY) != 0; }

	/**
	 * Parses JELIB string with Export user bits.
     * @param jelibUserBits JELIB string.
	 * @return Export user bust.
     * @throws NumberFormatException
	 */
    public static int parseJelibUserBits(String jelibUserBits) {
        int userBits = 0;
    			// parse state information in field 6
		int slashPos = jelibUserBits.indexOf('/');
        if (slashPos >= 0) {
            String extras = jelibUserBits.substring(slashPos);
            jelibUserBits = jelibUserBits.substring(0, slashPos);
            while (extras.length() > 0) {
                switch (extras.charAt(1)) {
                    case 'A': userBits |= PORTDRAWN; break;
                    case 'B': userBits |= BODYONLY; break;
                }
                extras = extras.substring(2);
            }
        }
        PortCharacteristic ch = PortCharacteristic.findCharacteristicShort(jelibUserBits);
        if (ch != null) userBits |= ch.getBits() << STATEBITSSH;
        return userBits;
    }

    /**
     * Returns true if this Export is linked into database.
     * @return true if this Export is linked into database.
     */
	public boolean isLinked()
	{
		try
		{
			return parent.isLinked() && parent.getPort(portIndex) == this;
		} catch (IndexOutOfBoundsException e)
		{
			return false;
		}
	}

	/**
	 * Method to check invariants in this Export.
	 * @exception AssertionError if invariants are not valid
	 */
	void check()
	{
		assert originalPort != null;
		NodeInst ni = originalPort.getNodeInst();
		PortProto pp = originalPort.getPortProto();
		assert ni.getParent() == parent && ni.isLinked();
		assert ni.getProto() == pp.getParent();
		if (pp instanceof Export) assert ((Export)pp).isLinked();
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
		Cell equiv = parent.getEquivalent();
		if (equiv == parent)
			return this;
		if (equiv == null)
			return null;
		return equiv.findPortProto(getNameKey());
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
		if (parent == otherCell) return this;

		// this is the non-cached way to do it
		return otherCell.findExport(getName());

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
//				Export epp = otherCell.findExport(opp.getName());
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
//			if (!otherCell.isIcon() || !pp.isBodyOnly())
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
		// check every instance of this node
		for(Iterator it = parent.getInstancesOf(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();

			// make sure all arcs on this port can connect
            PortInst pi = ni.findPortInstFromProto(this);
			for(Iterator cIt = pi.getConnections(); cIt.hasNext(); )
			{
				Connection con = (Connection)cIt.next();
//			for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
//			{
//				Connection con = (Connection)cIt.next();
//				if (con.getPortInst().getPortProto() != this) continue;
				if (!newPP.connectsTo(con.getArc().getProto()))
				{
					System.out.println(con.getArc().describe() + " arc in cell " + ni.getParent().describe() +
						" cannot connect to port " + getName());
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
		if (parent.isIcon())
		{
			// changed an export on an icon: find contents and change it there
			Cell onp = parent.contentsView();
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
		Cell onp = parent.iconView();
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

	/**
	 * Method to recursively alter the state bit fields of this Export.
	 */
	private void recursivelyChangeAllPorts()
	{
		// look at all instances of the cell that had port motion
		for(Iterator it = parent.getInstancesOf(); it.hasNext(); )
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

    /**
     * This function is to compare Export elements. Initiative CrossLibCopy
     * @param obj Object to compare to
     * @param buffer To store comparison messages in case of failure
     * @return True if objects represent same Export
     */
    public boolean compare(Object obj, StringBuffer buffer)
    {
        if (this == obj) return (true);

        // Better if compare classes? but it will crash with obj=null
        if (obj == null || getClass() != obj.getClass())
            return (false);

        PortProto no = (PortProto)obj;
        // getNameKey is required to call proper Name.equals()
        if (!getNameKey().equals(no.getNameKey()))
        {
            if (buffer != null)
                buffer.append("'" + this + "' and '" + no + "' do not have same name\n");
            return (false);
        }
        PortCharacteristic noC = no.getCharacteristic();

        if (!getCharacteristic().getName().equals(noC.getName()))
        {
            if (buffer != null)
                buffer.append("'" + this + "' and '" + no + "' do not have same characteristic\n");
            return (false);
        }
        return (true);
    }    
}
