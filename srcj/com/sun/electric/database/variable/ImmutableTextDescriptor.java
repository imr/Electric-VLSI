/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableTextDescriptor.java
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

import java.util.HashMap;


/**
 * This class describes how variable text appears.
 * <P>
 * This class should be thread-safe
 */
public class ImmutableTextDescriptor extends TextDescriptor
{
    /** the text descriptor is displayable */   private final boolean display;
	/** the bits of the text descriptor */		private final long bits;
	/** the color of the text descriptor */		private final int colorIndex;
    /** the code type of the text descriptor */ private final Code code;
    /** cache of all TextDescriptors */         private static final HashMap allDescriptors = new HashMap();

    /**
	 * The constructor creates canonized copy of anotherTextDescriptor.
	 * @param descriptor another descriptor.
	 */
	private ImmutableTextDescriptor(TextDescriptor descriptor)
	{
        boolean display = descriptor.isDisplay();
        long bits = descriptor.lowLevelGet();

        // Convert invalid VTPOSITION to default VTPOSCENT
        if (((bits & VTPOSITION) >> VTPOSITIONSH) > VTPOSBOXED) bits = (bits & ~VTPOSITION) | (VTPOSCENT << VTPOSITIONSH);
		// Convert VTDISPLAYNAMEVALINH and VTDISPLAYNAMEVALINHALL to VTDISPLAYNAMEVALUE
		if ((bits & VTDISPLAYPART) != 0) bits = (bits & ~VTDISPLAYPART) | (VTDISPLAYNAMEVALUE << VTDISPLAYPARTSH);
		// Convert zero VTSIZE to RelSize(1)
		if ((bits & VTSIZE) == 0) bits |= (4L << Size.TXTQGRIDSH) << VTSIZESH;
        // clear unused bits if non-displayable
        if (!display) bits &= VTSEMANTIC;
       
        this.display = display;
        this.bits = bits;
		this.colorIndex = display ? descriptor.getColorIndex() : 0;
        this.code = descriptor.getCode();
	}
    
    public static ImmutableTextDescriptor newImmutableTextDescriptor(TextDescriptor td)
    {
        if (td instanceof ImmutableTextDescriptor) return (ImmutableTextDescriptor)td;
        ImmutableTextDescriptor cacheTd = (ImmutableTextDescriptor)allDescriptors.get(td);
        if (cacheTd != null) return cacheTd;
        ImmutableTextDescriptor itd = new ImmutableTextDescriptor(td);
        if (!itd.equals(td))
        {
            // is canonized text descriptor already here ?
            cacheTd = (ImmutableTextDescriptor)allDescriptors.get(itd);
            if (cacheTd != null) return cacheTd;
        }
        allDescriptors.put(itd, itd);
        return itd;
    }
  
    public ImmutableTextDescriptor withoutParam() {
        if (!isParam()) return this;
        MutableTextDescriptor mtd = new MutableTextDescriptor(this);
        mtd.setParam(false);
        return newImmutableTextDescriptor(mtd);
    }
    
    public ImmutableTextDescriptor withDisplayWithoutParamAndCode() {
        if (isDisplay() && !isParam() && !isCode()) return this;
        MutableTextDescriptor mtd = new MutableTextDescriptor(this);
        mtd.setDisplay(true);
        mtd.setParam(false);
        mtd.setCode(Code.NONE);
        return newImmutableTextDescriptor(mtd);
    }
    
    public static int cacheSize() { return allDescriptors.size(); }
    
	/**
	 * Method to return true if this TextDescriptor is displayable.
	 * @return true if this TextDescriptor is displayable.
	 */
	public boolean isDisplay() { return display; }
    
	/**
	 * Low-level method to get the bits in the TextDescriptor.
	 * These bits are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the bits in the TextDescriptor.
	 */
	public long lowLevelGet() { return bits; }
    
	/**
	 * Method to return the color index of the TextDescriptor.
	 * Color indices are more general than colors, because they can handle
	 * transparent layers, C-Electric-style opaque layers, and full color values.
	 * Methods in "EGraphics" manipulate color indices.
	 * @return the color index of the TextDescriptor.
	 */
	public int getColorIndex() { return colorIndex; }
    
    /**
     * Return code type of the TextDescriptor.
     * @return code tyoe
     */
    public TextDescriptor.Code getCode() { return code; }
    
	/**
	 * Method to return a displayable TextDescriptor that is a default for Variables on NodeInsts.
	 * @return a new TextDescriptor that can be stored in a Variable on a NodeInsts.
	 */
	public static ImmutableTextDescriptor getNodeTextDescriptor()
	{
		return cacheNodeDescriptor.newTextDescriptor(true);
	}
    
	/**
	 * Method to return a displayable TextDescriptor that is a default for Variables on ArcInsts.
	 * @return a new TextDescriptor that can be stored in a Variable on a ArcInsts.
	 */
	public static ImmutableTextDescriptor getArcTextDescriptor()
	{
		return cacheArcDescriptor.newTextDescriptor(true);
	}
    
	/**
	 * Method to return a displayable TextDescriptor that is a default for Variables on Exports.
	 * @return a new TextDescriptor that can be stored in a Variable on a Exports.
	 */
	public static ImmutableTextDescriptor getExportTextDescriptor()
	{
		return cacheExportDescriptor.newTextDescriptor(true);
	}

	/**
	 * Method to return a displayable TextDescriptor that is a default for Variables on Annotations.
	 * @return a new TextDescriptor that can be stored in a Variable on a Annotations.
	 */
	public static ImmutableTextDescriptor getAnnotationTextDescriptor()
	{
		return cacheAnnotationDescriptor.newTextDescriptor(true);
	}
    
	/**
	 * Method to return a displayable TextDescriptor that is a default for Variables on Cell Instance Names.
	 * @return a new TextDescriptor that can be stored in a Variable on a Cell Instance Names.
	 */
	public static ImmutableTextDescriptor getInstanceTextDescriptor()
	{
		return cacheInstanceDescriptor.newTextDescriptor(true);
	}

	/**
	 * Method to return a displayable TextDescriptor that is a default for Variables on Cell Variables.
	 * @return a new TextDescriptor that can be stored in a Variable on a Cell Variables.
	 */
	public static ImmutableTextDescriptor getCellTextDescriptor()
	{
		return cacheCellDescriptor.newTextDescriptor(true);
	}
}
