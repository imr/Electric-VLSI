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
package com.sun.electric.database.variable;

import com.sun.electric.database.geometry.EGraphics;

/**
 * This class describes how variable text appears.
 */
public class MutableTextDescriptor extends AbstractTextDescriptor {

    /** the text descriptor is displayable */
    private Display display;
    /** the bits of the text descriptor */
    private long bits;
    /** the color of the text descriptor */
    private int colorIndex;

    /**
     * The constructor creates copy of anotherTextDescriptor.
     * @param descriptor another descriptor.
     */
    public MutableTextDescriptor(AbstractTextDescriptor descriptor) {
        this.display = descriptor.getDisplay();
        this.bits = descriptor.lowLevelGet();
        this.colorIndex = descriptor.getColorIndex();
    }

    /**
     * The constructor simply creates a TextDescriptor with zero values filled-in.
     * Size is set to relative 1.0
     * isDisplay is true.
     */
    public MutableTextDescriptor() {
        // Size is relative 1.0
        this((4L << Size.TXTQGRIDSH) << VTSIZESH, 0, true);
    }

    /**
     * The constructor simply creates a TextDescriptor with specified values.
     * @param descriptor the bits of the text descriptor.
     * @param colorIndex color index of the text descriptor.
     * @param display true if text descriptor is displayable
     */
    public MutableTextDescriptor(long descriptor, int colorIndex, boolean display) {
        this.display = display ? Display.SHOWN : Display.NONE;
        this.bits = descriptor;
        this.colorIndex = colorIndex;
    }

    /**
     * Set this MutableTextDescriptor to given C Electric bits.
     * @param descriptor0 lower word of the text descriptor.
     * @param descriptor1 higher word of the text descriptor.
     */
    public void setCBits(int descriptor0, int descriptor1) {
        display = Display.SHOWN;
        bits = ((long) descriptor1 << 32) | (descriptor0 & 0xffffffffL);
        colorIndex = 0;
    }

    /**
     * Set this MutableTextDescriptor to given C Electric bits.
     * @param descriptor0 lower word of the text descriptor.
     * @param descriptor1 higher word of the text descriptor.
     * @param cFlags variable flags
     */
    public void setCBits(int descriptor0, int descriptor1, int cFlags) {
        display = (cFlags & VDISPLAY) != 0 ? Display.SHOWN : Display.NONE;
        bits = ((long) descriptor1 << 32) | (descriptor0 & 0xffffffffL);
        colorIndex = 0;
    }

    /**
     * Method to return mode how this TextDescriptor is displayable.
     * @return Display mode how this TextDescriptor is displayable.
     */
    @Override
    public Display getDisplay() {
        return display;
    }

    /**
     * Low-level method to get the bits in the TextDescriptor.
     * These bits are a collection of flags that are more sensibly accessed
     * through special methods.
     * This general access to the bits is required because the ELIB
     * file format stores it as a full integer.
     * This should not normally be called by any other part of the system.
     * @return the bits in the TextDescriptor.
     */
    @Override
    public long lowLevelGet() {
        return bits;
    }

    /**
     * Method to return the color index of the TextDescriptor.
     * Color indices are more general than colors, because they can handle
     * transparent layers, C-Electric-style opaque layers, and full color values.
     * Methods in "EGraphics" manipulate color indices.
     * @return the color index of the TextDescriptor.
     */
    @Override
    public int getColorIndex() {
        return colorIndex;
    }

    private void setField(long mask, int shift, int value) {
        bits = bits & ~mask | ((long) value << shift) & mask;
    }

    private void setFlag(long mask, boolean state) {
        bits = state ? bits | mask : bits & ~mask;
    }

    /**
     * Method to return a displayable TextDescriptor that is a default for Variables on NodeInsts.
     * @return a new TextDescriptor that can be stored in a Variable on a NodeInsts.
     */
    public static MutableTextDescriptor getNodeTextDescriptor() {
        return new MutableTextDescriptor(TextDescriptor.getNodeTextDescriptor());
    }

    /**
     * Method to return a displayable TextDescriptor that is a default for Variables on ArcInsts.
     * @return a new TextDescriptor that can be stored in a Variable on a ArcInsts.
     */
    public static MutableTextDescriptor getArcTextDescriptor() {
        return new MutableTextDescriptor(TextDescriptor.getArcTextDescriptor());
    }

    /**
     * Method to return a displayable TextDescriptor that is a default for Variables on Exports.
     * @return a new TextDescriptor that can be stored in a Variable on a Exports.
     */
    public static MutableTextDescriptor getExportTextDescriptor() {
        return new MutableTextDescriptor(TextDescriptor.getExportTextDescriptor());
    }

    /**
     * Method to return a displayable TextDescriptor that is a default for Variables on Port instances.
     * @return a new TextDescriptor that can be stored in a Variable on a Port instances.
     */
    public static MutableTextDescriptor getPortTextDescriptor() {
        return new MutableTextDescriptor(TextDescriptor.getPortInstTextDescriptor());
    }

    /**
     * Method to return a displayable TextDescriptor that is a default for Variables on Annotations.
     * @return a new TextDescriptor that can be stored in a Variable on a Annotations.
     */
    public static MutableTextDescriptor getAnnotationTextDescriptor() {
        return new MutableTextDescriptor(TextDescriptor.getAnnotationTextDescriptor());
    }

    /**
     * Method to return a displayable TextDescriptor that is a default for Variables on Cell Instance Names.
     * @return a new TextDescriptor that can be stored in a Variable on a Cell Instance Names.
     */
    public static MutableTextDescriptor getInstanceTextDescriptor() {
        return new MutableTextDescriptor(TextDescriptor.getInstanceTextDescriptor());
    }

    /**
     * Method to return a displayable TextDescriptor that is a default for Variables on Cell Variables.
     * @return a new TextDescriptor that can be stored in a Variable on a Cell Variables.
     */
    public static MutableTextDescriptor getCellTextDescriptor() {
        return new MutableTextDescriptor(TextDescriptor.getCellTextDescriptor());
    }

    /**
     * Method to set this TextDescriptor to be displayable.
     * Displayable TextDescriptors are shown with the object.
     */
    public void setDisplay(boolean state) {
        setDisplay(state ? Display.SHOWN : Display.NONE);
    }

    /**
     * Method to set dispalyable mode of this TextDescriptor.
     * Displayable TextDescriptors are shown with the object.
     * @param display displayable mode
     */
    public void setDisplay(Display display) {
        this.display = display;
    }

    /**
     * Method to set the color index of the TextDescription with an EGraphics index.
     * @param color
     */
    public void setColorWithEGraphicsIndex(int color) {
        int newColorIndex = color; // zero is DEFAULT COLOR
        if (color > 0) {
            int[] colorIndices = EGraphics.getColorIndices();
            newColorIndex = colorIndices[color - 1];
        }
        setColorIndex(newColorIndex);
    }

    /**
     * Method to set the color index of the TextDescriptor.
     * Color indices are more general than colors, because they can handle
     * transparent layers, C-Electric-style opaque layers, and full color values.
     * Methods in "EGraphics" manipulate color indices.
     * @param colorIndex the color index of the TextDescriptor.
     */
    public void setColorIndex(int colorIndex) {
        this.colorIndex = colorIndex;
    }

    /**
     * Method to set the text position of the TextDescriptor.
     * The text position describes the "anchor point" of the text,
     * which is the point on the text that is attached to the object and does not move.
     * @param p the text position of the TextDescriptor.
     */
    public void setPos(Position p) {
        setField(VTPOSITION, VTPOSITIONSH, p.getIndex());
    }

    /**
     * Method to set the text size of this TextDescriptor to an absolute size (in points).
     * The size must be between 1 and 63 points.
     * @param s the point size of this TextDescriptor.
     */
    public void setAbsSize(int s) {
        Size size = Size.newAbsSize(s);
        if (size == null) {
            System.out.println("Invalid absolute size of " + s);
            return;
        }
        setField(VTSIZE, VTSIZESH, size.getBits());
    }

    /**
     * Method to set the text size of this TextDescriptor to a relative size (in units).
     * The size must be between 0.25 and 127.75 grid units (in .25 increments).
     * @param s the unit size of this TextDescriptor.
     */
    public void setRelSize(double s) {
        Size size = Size.newRelSize(s);
        if (size == null) {
            System.out.println("Invalid relative size of " + s);
            return;
        }
        setField(VTSIZE, VTSIZESH, size.getBits());
    }

    /**
     * Method to set the text font of a MutableTextDescriptor with a given Font.
     * If DEFAULT FONT is the name, set the value to 0.
     * @param fontName the name of the font.
     */
    public void setFaceWithActiveFont(String fontName) {
        if (fontName.equals("DEFAULT FONT")) {
            setFace(0);
        } else {
            TextDescriptor.ActiveFont newFont = TextDescriptor.ActiveFont.findActiveFont(fontName);
            int newFontIndex = newFont != null ? newFont.getIndex() : 0;
            setFace(newFontIndex);
        }
    }

    /**
     * Method to set the text font of the TextDescriptor.
     * @param f the text font of the TextDescriptor.
     */
    public void setFace(int f) {
        setField(VTFACE, VTFACESH, f);
    }

    /**
     * Method to set the text rotation of the TextDescriptor.
     * There are only 4 rotations: 0, 90 degrees, 180 degrees, and 270 degrees.
     * @param r the text rotation of the TextDescriptor.
     */
    public void setRotation(Rotation r) {
        setField(VTROTATION, VTROTATIONSH, r.getIndex());
    }

    /**
     * Method to set the text display part of the TextDescriptor.
     * @param d the text display part of the TextDescriptor.
     */
    public void setDispPart(DispPos d) {
        setField(VTDISPLAYPART, VTDISPLAYPARTSH, d.getIndex());
    }

    /**
     * Method to set the text in the TextDescriptor to be italic.
     */
    public void setItalic(boolean state) {
        setFlag(VTITALIC, state);
    }

    /**
     * Method to set the text in the TextDescriptor to be bold.
     */
    public void setBold(boolean state) {
        setFlag(VTBOLD, state);
    }

    /**
     * Method to set the text in the TextDescriptor to be underlined.
     */
    public void setUnderline(boolean state) {
        setFlag(VTUNDERLINE, state);
    }

    /**
     * Method to set the text in the TextDescriptor to be interior.
     * Interior text is not seen at higher levels of the hierarchy.
     */
    public void setInterior(boolean state) {
        setFlag(VTINTERIOR, state);
    }

    /**
     * Method to set the text in the TextDescriptor to be inheritable.
     * Inheritable variables copy their contents from prototype to instance.
     * Only Variables on NodeProto and PortProto objects can be inheritable.
     * When a NodeInst is created, any inheritable Variables on its NodeProto are automatically
     * created on that NodeInst.
     */
    public void setInherit(boolean state) {
        setFlag(VTINHERIT, state);
    }

    /**
     * Method to set the text in the TextDescriptor to be a parameter.
     * Parameters are those Variables that have values on instances which are
     * passed down the hierarchy into the contents.
     * Parameters can only exist on NodeInst objects.
     */
    public void setParam(boolean state) {
        setFlag(VTISPARAMETER, state);
    }

    /**
     * Method to set the X and Y offsets of the text in the TextDescriptor.
     * The values are scaled by 4, so a value of 3 indicates a shift of 0.75 and a value of 4 shifts by 1.
     * @param xd the X offset of the text in the TextDescriptor.
     * @param yd the Y offset of the text in the TextDescriptor.
     */
    public void setOff(double xd, double yd) {
        boolean xneg = xd < 0;
        setFlag(VTXOFFNEG, xneg);
        if (xneg) {
            xd = -xd;
        }

        boolean yneg = yd < 0;
        setFlag(VTYOFFNEG, yneg);
        if (yneg) {
            yd = -yd;
        }

        int scale = ((int) (Math.max(xd, yd) * 4)) >> VTOFFMASKWID;
        if (scale > VTSCALEMAX) {
            scale = VTSCALEMAX;
        }
        int x = Math.min((int) (xd * 4 / (scale + 1) + 0.5), VTOFFMAX);
        int y = Math.min((int) (yd * 4 / (scale + 1) + 0.5), VTOFFMAX);
        setField(VTXOFF, VTXOFFSH, x);
        setField(VTYOFF, VTYOFFSH, y);
        setField(VTOFFSCALE, VTOFFSCALESH, scale);
    }

    /**
     * Method to set the Unit of the TextDescriptor.
     * Unit describe the type of real-world unit to apply to the value.
     * For example, if this value is in volts, the Unit tells whether the value
     * is volts, millivolts, microvolts, etc.
     * @param u the Unit of the TextDescriptor.
     */
    public void setUnit(Unit u) {
        setField(VTUNITS, VTUNITSSH, u.getIndex());
    }
}
