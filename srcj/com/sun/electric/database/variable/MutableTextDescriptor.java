/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TextDescriptor.java
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
package com.sun.electric.database.variable;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * This class describes how variable text appears.
 * <P>
 * This class should be thread-safe
 */
public class MutableTextDescriptor extends TextDescriptor
{

	/**
	 * The constructor simply creates a TextDescriptor with zero values filled-in.
	 * @param owner owner of this TextDescriptor.
	 */
	public MutableTextDescriptor()
	{
		this.descriptor0 = this.descriptor1 = 0;
		this.colorIndex = 0;
	}

	/**
	 * The constructor creates copy of anotherTextDescriptor.
	 * @param owner owner of this TextDescriptor.
	 * @param descriptor another descriptor.
	 */
	public MutableTextDescriptor(TextDescriptor descriptor)
	{
		this.descriptor0 = descriptor.descriptor0;
		this.descriptor1 = descriptor.descriptor1;
		this.colorIndex = descriptor.colorIndex;
	}

	/**
	 * The constructor simply creates a TextDescriptor with specified values.
	 * @param owner owner of this TextDescriptor.
	 * @param descriptor0 lower word of the text descriptor.
	 * @param descriptor1 higher word of the text descriptor.
	 */
	public MutableTextDescriptor(int descriptor0, int descriptor1, int colorIndex)
	{
		this.descriptor0 = descriptor0;
		this.descriptor1 = descriptor1;
		this.colorIndex = colorIndex;
	}

	/**
	 * The constructor simply creates a TextDescriptor with specified values.
	 * @param owner owner of this TextDescriptor.
	 * @param descriptor the bits of the text descriptor.
	 */
	public MutableTextDescriptor(long descriptor, int colorIndex)
	{
		this.descriptor0 = (int)(descriptor >> 32);
		this.descriptor1 = (int)(descriptor & 0xFFFFFFFF);
		this.colorIndex = colorIndex;
	}

	/**
	 * Method to return a TextDescriptor that is a default for Variables on NodeInsts.
	 * @param owner owner of this TextDescriptor.
	 * @return a new TextDescriptor that can be stored in a Variable on a NodeInsts.
	 */
	public static MutableTextDescriptor getNodeTextDescriptor()
	{
		MutableTextDescriptor td = new MutableTextDescriptor(cacheNodeDescriptor.getLong(), 0);
		String fontName = getNodeTextDescriptorFont();
		if (fontName.length() > 0) td.setFace(ActiveFont.findActiveFont(fontName).getIndex());
		return td;
	}

	/**
	 * Method to return a TextDescriptor that is a default for Variables on ArcInsts.
	 * @param owner owner of this TextDescriptor.
	 * @return a new TextDescriptor that can be stored in a Variable on a ArcInsts.
	 */
	public static MutableTextDescriptor getArcTextDescriptor()
	{
		MutableTextDescriptor td = new MutableTextDescriptor(cacheArcDescriptor.getLong(), 0);
		String fontName = getArcTextDescriptorFont();
		if (fontName.length() > 0) td.setFace(ActiveFont.findActiveFont(fontName).getIndex());
		return td;
	}

	/**
	 * Method to return a TextDescriptor that is a default for Variables on Exports.
	 * @param owner owner of this TextDescriptor.
	 * @return a new TextDescriptor that can be stored in a Variable on a Exports.
	 */
	public static MutableTextDescriptor getExportTextDescriptor()
	{
		MutableTextDescriptor td = new MutableTextDescriptor(cacheExportDescriptor.getLong(), 0);
		String fontName = getExportTextDescriptorFont();
		if (fontName.length() > 0) td.setFace(ActiveFont.findActiveFont(fontName).getIndex());
		return td;
	}

	/**
	 * Method to return a TextDescriptor that is a default for Variables on Annotations.
	 * @param owner owner of this TextDescriptor.
	 * @return a new TextDescriptor that can be stored in a Variable on a Annotations.
	 */
	public static MutableTextDescriptor getAnnotationTextDescriptor()
	{
		MutableTextDescriptor td = new MutableTextDescriptor(cacheAnnotationDescriptor.getLong(), 0);
		String fontName = getAnnotationTextDescriptorFont();
		if (fontName.length() > 0) td.setFace(ActiveFont.findActiveFont(fontName).getIndex());
		return td;
	}

	/**
	 * Method to return a TextDescriptor that is a default for Variables on Cell Instance Names.
	 * @param owner owner of this TextDescriptor.
	 * @return a new TextDescriptor that can be stored in a Variable on a Cell Instance Names.
	 */
	public static MutableTextDescriptor getInstanceTextDescriptor()
	{
		MutableTextDescriptor td = new MutableTextDescriptor(cacheInstanceDescriptor.getLong(), 0);
		String fontName = getInstanceTextDescriptorFont();
		if (fontName.length() > 0) td.setFace(ActiveFont.findActiveFont(fontName).getIndex());
		return td;
	}

	/**
	 * Method to return a TextDescriptor that is a default for Variables on Cell Variables.
	 * @param owner owner of this TextDescriptor.
	 * @return a new TextDescriptor that can be stored in a Variable on a Cell Variables.
	 */
	public static MutableTextDescriptor getCellTextDescriptor()
	{
		MutableTextDescriptor td = new MutableTextDescriptor(cacheCellDescriptor.getLong(), 0);
		String fontName = getCellTextDescriptorFont();
		if (fontName.length() > 0) td.setFace(ActiveFont.findActiveFont(fontName).getIndex());
		return td;
	}

// 	/**
// 	 * Low-level method to set the bits in the TextDescriptor.
// 	 * These bits are a collection of flags that are more sensibly accessed
// 	 * through special methods.
// 	 * This general access to the bits is required because the ELIB
// 	 * file format stores it as a full integer.
// 	 * This should not normally be called by any other part of the system.
// 	 * @param descriptor0 the first word of the new TextDescriptor.
// 	 * @param descriptor1 the second word of the new TextDescriptor.
// 	 */
// 	public synchronized void lowLevelSet(int descriptor0, int descriptor1)
// 	{
// 		this.descriptor0 = descriptor0;
// 		this.descriptor1 = descriptor1;
// 	}

// 	/**
// 	 * Low-level method to set the bits in the TextDescriptor.
// 	 * These bits are a collection of flags that are more sensibly accessed
// 	 * through special methods.
// 	 * This general access to the bits is required because the ELIB
// 	 * file format stores it as a full integer.
// 	 * This should not normally be called by any other part of the system.
// 	 * @param descriptor the bits of the new TextDescriptor.
// 	 */
// 	public synchronized void lowLevelSet(long descriptor)
// 	{
// 		this.descriptor0 = (int)(descriptor >> 32);
// 		this.descriptor1 = (int)(descriptor & 0xFFFFFFFF);
// 	}

// 	/**
// 	 * Method to copy another TextDescriptor to this TextDescriptor.
// 	 * These bits are a collection of flags that are more sensibly accessed
// 	 * through special methods.
// 	 * This general access to the bits is required because the ELIB
// 	 * file format stores it as a full integer.
// 	 * This should not normally be called by any other part of the system.
// 	 * @param descriptor other TextDescriptor.
// 	 */
// 	public void copy(TextDescriptor descriptor)
// 	{
//         int d0, d1, ci;
//         synchronized(descriptor) {
//             d0 = descriptor.descriptor0;
//             d1 = descriptor.descriptor1;
//             ci = descriptor.colorIndex;
//         }
//         synchronized(this) {
//             this.descriptor0 = d0;
//             this.descriptor1 = d1;
//             this.colorIndex = ci;
//         }
// 	}

	/**
	 * Method to set the color index of the TextDescriptor.
	 * Color indices are more general than colors, because they can handle
	 * transparent layers, C-Electric-style opaque layers, and full color values.
	 * Methods in "EGraphics" manipulate color indices.
	 * @param colorIndex the color index of the TextDescriptor.
	 */
	public void setColorIndex(int colorIndex)
	{
		this.colorIndex = colorIndex;
	}

	/**
	 * Method to set the text position of the TextDescriptor.
	 * The text position describes the "anchor point" of the text,
	 * which is the point on the text that is attached to the object and does not move.
	 * @param p the text position of the TextDescriptor.
	 */
	public synchronized void setPos(Position p)
	{
		descriptor0 = (descriptor0 & ~VTPOSITION) | p.getIndex();
	}

	/**
	 * Method to set the text size of this TextDescriptor to an absolute size (in points).
	 * The size must be between 1 and 63 points.
	 * @param s the point size of this TextDescriptor.
	 */
	public synchronized void setAbsSize(int s)
	{
		Size size = Size.newAbsSize(s);
		if (size == null) return;
		descriptor1 = (descriptor1 & ~VTSIZE) | (size.getBits() << VTSIZESH);
	}

	/**
	 * Method to set the text size of this TextDescriptor to a relative size (in units).
	 * The size must be between 0.25 and 127.75 grid units (in .25 increments).
	 * @param s the unit size of this TextDescriptor.
	 */
	public synchronized void setRelSize(double s)
	{
		Size size = Size.newRelSize(s);
		if (size == null) return;
		descriptor1 = (descriptor1 & ~VTSIZE) | (size.getBits() << VTSIZESH);
	}

	/**
	 * Method to set the text font of the TextDescriptor.
	 * @param f the text font of the TextDescriptor.
	 */
	public synchronized void setFace(int f)
	{
		descriptor1 = (descriptor1 & ~VTFACE) | (f << VTFACESH);
	}

	/**
	 * Method to set the text rotation of the TextDescriptor.
	 * There are only 4 rotations: 0, 90 degrees, 180 degrees, and 270 degrees.
	 * @param r the text rotation of the TextDescriptor.
	 */
	public synchronized void setRotation(Rotation r)
	{
		descriptor1 = (descriptor1 & ~VTROTATION) | (r.getIndex() << VTROTATIONSH);
	}

	/**
	 * Method to set the text display part of the TextDescriptor.
	 * @param d the text display part of the TextDescriptor.
	 */
	public synchronized void setDispPart(DispPos d)
	{
		descriptor0 = (descriptor0 & ~VTDISPLAYPART) | (d.getIndex() << VTDISPLAYPARTSH);
	}

	/**
	 * Method to set the text in the TextDescriptor to be italic.
	 */
	public synchronized void setItalic(boolean state)
	{
        if (state)
		    descriptor0 |= VTITALIC;
        else
            descriptor0 &= ~VTITALIC;
	}

	/**
	 * Method to set the text in the TextDescriptor to be bold.
	 */
	public synchronized void setBold(boolean state)
	{
        if (state)
		    descriptor0 |= VTBOLD;
        else
            descriptor0 &= ~VTBOLD;
	}

	/**
	 * Method to set the text in the TextDescriptor to be underlined.
	 */
	public synchronized void setUnderline(boolean state)
	{
		if (state)
            descriptor0 |= VTUNDERLINE;
        else
            descriptor0 &= ~VTUNDERLINE;
	}

	/**
	 * Method to set the text in the TextDescriptor to be interior.
	 * Interior text is not seen at higher levels of the hierarchy.
	 */
	public synchronized void setInterior(boolean state)
	{
		if (state)
            descriptor0 |= VTINTERIOR;
        else
            descriptor0 &= ~VTINTERIOR;
	}

	/**
	 * Method to set the text in the TextDescriptor to be inheritable.
	 * Inheritable variables copy their contents from prototype to instance.
	 * Only Variables on NodeProto and PortProto objects can be inheritable.
	 * When a NodeInst is created, any inheritable Variables on its NodeProto are automatically
	 * created on that NodeInst.
	 */
	public synchronized void setInherit(boolean state)
	{
		if (state)
            descriptor0 |= VTINHERIT;
        else
            descriptor0 &= ~VTINHERIT;
	}

	/**
	 * Method to set the text in the TextDescriptor to be a parameter.
	 * Parameters are those Variables that have values on instances which are
	 * passed down the hierarchy into the contents.
	 * Parameters can only exist on NodeInst objects.
	 */
	public synchronized void setParam(boolean state)
	{
		if (state)
            descriptor0 |= VTISPARAMETER;
        else
            descriptor0 &= ~VTISPARAMETER;
	}

	/**
	 * Method to set the X and Y offsets of the text in the TextDescriptor.
	 * The values are scaled by 4, so a value of 3 indicates a shift of 0.75 and a value of 4 shifts by 1.
	 * @param xd the X offset of the text in the TextDescriptor.
	 * @param yd the Y offset of the text in the TextDescriptor.
	 */
	public synchronized void setOff(double xd, double yd)
	{
		descriptor0 &= ~(VTXOFF|VTYOFF|VTXOFFNEG|VTYOFFNEG);
		if (xd < 0)
		{
			xd = -xd;
			descriptor0 |= VTXOFFNEG;
		}
		if (yd < 0)
		{
			yd = -yd;
			descriptor0 |= VTYOFFNEG;
		}
		int scale = ((int)(Math.max(xd,yd) * 4)) >> VTOFFMASKWID;
		int x = Math.min( (int)(xd * 4/(scale + 1) + 0.5), VTOFFMAX);
		int y = Math.min( (int)(yd * 4/(scale + 1) + 0.5), VTOFFMAX);
		descriptor0 |= (x << VTXOFFSH) & VTXOFF;
		descriptor0 |= (y << VTYOFFSH) & VTYOFF;
		descriptor1 = (descriptor1 & ~VTOFFSCALE) | ((scale << VTOFFSCALESH) & VTOFFSCALE);
	}

	/**
	 * Method to set the Unit of the TextDescriptor.
	 * Unit describe the type of real-world unit to apply to the value.
	 * For example, if this value is in volts, the Unit tells whether the value
	 * is volts, millivolts, microvolts, etc.
	 * @param u the Unit of the TextDescriptor.
	 */
	public synchronized void setUnit(Unit u)
	{
		descriptor1 = (descriptor1 & ~VTUNITS) | (u.getIndex() << VTUNITSSH);
	}
}
