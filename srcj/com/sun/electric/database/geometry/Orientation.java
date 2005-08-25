/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Orientation.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

package com.sun.electric.database.geometry;

import java.awt.geom.AffineTransform;
import java.util.HashMap;

/**
 * Class <code>Orientation</code> represents 2D affine transform which is composition of rotation and possible flip.
 * The C code used an angle (in tenth-degrees) and a "transpose" factor
 * which would flip the object along the major diagonal after rotation.
 * The Java code uses the same angle (in tenth-degrees) but has two mirror
 * options: Mirror X and Mirror Y.
 */
public class Orientation {

	// The internal representation of orientation is the 2D transformation matrix:
	// [   sX*cos(angle)   -sX*sin(angle)   ] = [ sX  0 ] * [ cos(angle) -sin(angle) ]
	// [   sY*sin(angle)    sY*cos(angle)   ]   [  0 sY ]   [ sin(angle)  cos(angle) ]
	// --------------------------------------
	// sX = jMirrorX ? -1 : 1
	// sY = jMirrorY ? -1 : 1
	// 0 <= jAngle < 3600 is in tenth-degrees
	private final short jAngle;
	private final short jOctant;
	private final boolean jMirrorX;
	private final boolean jMirrorY;
	private final String jString;

	private final short cAngle;
	private final boolean cTranspose;

	private final Orientation inverse;
	private final AffineTransform trans;

	private static final HashMap map = new HashMap();
	private static final Orientation[] map45;

	private static final int OCTANT      = 0x07;
	private static final int XMIRROR45   = 0x08;
	private static final int YMIRROR45   = 0x10;

	static {
		Orientation[] m = new Orientation[32];
		for (int i = 0; i < m.length; i++)
		{
			int octant = i & OCTANT;
			boolean jMirrorX = (i&XMIRROR45) != 0;
			boolean jMirrorY = (i&YMIRROR45) != 0;

			Orientation orient = new Orientation(octant*450, jMirrorX, jMirrorY, null);
			m[i] = orient;
			if (orient.inverse == orient) continue;
			m[i + 8 - octant*2] = orient.inverse;
		}
		map45 = m;
	}

	/** Identical Orientation */  public static final Orientation IDENT  = fromJava(0, false, false);
    public static final Orientation R = fromJava(900, false, false);
    public static final Orientation RR = fromJava(1800, false, false);
    public static final Orientation RRR = fromJava(2700, false, false);
    public static final Orientation X = fromJava(0, true, false);
    public static final Orientation XR = fromJava(900, true, false);
    public static final Orientation XRR = fromJava(1800, true, false);
    public static final Orientation XRRR = fromJava(2700, true, false);
    public static final Orientation Y = fromJava(0, false, true);
    public static final Orientation YR = fromJava(900, false, true);
    public static final Orientation YRR = fromJava(1800, false, true);
    public static final Orientation YRRR = fromJava(2700, false, true);
    public static final Orientation XY = fromJava(0, true, true);
    public static final Orientation XYR = fromJava(900, true, true);
    public static final Orientation XYRR = fromJava(1800, true, true);
    public static final Orientation XYRRR = fromJava(2700, true, true);
    
	private Orientation(int jAngle, boolean jMirrorX, boolean jMirrorY, Orientation inverse)
	{
		assert 0 <= jAngle && jAngle < 3600;

		// store Java information
		this.jAngle = (short)jAngle;
		this.jOctant = (short)(jAngle % 450 == 0 ? jAngle/450 : -1);
		this.jMirrorX = jMirrorX;
		this.jMirrorY = jMirrorY;

		// compute C information
		int cAngle = jAngle;
		boolean cTranspose = false;
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
		this.cAngle = (short)cAngle;
		this.cTranspose = cTranspose;

		if (inverse == null)
		{
			if (cTranspose || jAngle == 0 || jAngle == 1800)
				inverse = this;
			else
				inverse = new Orientation(3600 - jAngle, jMirrorX, jMirrorY, this);
		}
		this.inverse = inverse;

		double[] matrix = new double[4];
        int sect = jAngle / 450;
        assert 0 <= sect && sect < 8;
        int ang = jAngle % 450;
        if (sect % 2 != 0) ang = 450 - ang;
        assert 0 <= ang && ang <= 450;
	double cos0, sin0;
	if (ang == 0) {
	    cos0 = 1;
	    sin0 = 0;
	} else if (ang == 450) {
	    cos0 = sin0 = Math.sqrt(0.5);
	} else {
    	    double alpha = ang * Math.PI / 1800.0;
    	    cos0 = Math.cos(alpha);
    	    sin0 = Math.sin(alpha);
	}
        double cos = 0, sin = 0;
        switch (sect) {
            case 0: cos =  cos0; sin =  sin0; break;
            case 1: cos =  sin0; sin =  cos0; break;
            case 2: cos = -sin0; sin =  cos0; break;
            case 3: cos = -cos0; sin =  sin0; break;
            case 4: cos = -cos0; sin = -sin0; break;
            case 5: cos = -sin0; sin = -cos0; break;
            case 6: cos =  sin0; sin = -cos0; break;
            case 7: cos =  cos0; sin = -sin0; break;
            default: assert false;
        }
		matrix[0] = cos * (jMirrorX ? -1 : 1);
		matrix[1] = sin * (jMirrorY ? -1 : 1);
		matrix[2] = sin * (jMirrorX ? 1 : -1);
		matrix[3] = cos * (jMirrorY ? -1 : 1);
		if (jAngle % 900 == 0)
		{
			for (int i = 0; i < matrix.length; i++)
				matrix[i] = Math.round(matrix[i]);
		}
		this.trans = new AffineTransform(matrix);

		// compute Jelib String
		String s = "";
		if (jMirrorX) s += 'X';
		if (jMirrorY) s += 'Y';
		while (jAngle >= 900)
		{
			s += 'R';
			jAngle -= 900;
		}
		if (jAngle != 0) s = s + jAngle;
		this.jString = s;

	}

	/**
	 * Get Orientation by the new Java style parameters.
	 * @param jAngle the angle of rotation (in tenth-degrees)
	 * @param jMirrorX if true, object is flipped over the vertical (mirror in X).
	 * @param jMirrorY if true, object is flipped over the horizontal (mirror in Y).
	 * @return the Orientation.
	 */
	public static Orientation fromJava(int jAngle, boolean jMirrorX, boolean jMirrorY)
	{
		if (jAngle % 450 == 0)
		{
			int index = jAngle/450 & OCTANT;
			if (jMirrorX) index |= XMIRROR45;
			if (jMirrorY) index |= YMIRROR45;
			return map45[index];
		}

		jAngle = jAngle % 3600;
		if (jAngle < 0) jAngle += 3600;
		int index = 0;
		if (jMirrorX) index += 3600;
		if (jMirrorY) index += 3600*2;

		Integer key = new Integer(index + jAngle);
		Orientation orient;
		synchronized (map)
		{
			orient = (Orientation)map.get(key);
			if (orient == null)
			{
				orient = new Orientation(jAngle, jMirrorX, jMirrorY, null);
				map.put(key, orient);
				if (orient.inverse != orient)
				{
					key = new Integer(index + 3600 - jAngle);
					map.put(key, orient.inverse);
				}
			}
		}
		return orient;
	}

	/**
	 * Get Orientation by the old C style parameters.
	 * @param cAngle the angle of rotation (in tenth-degrees)
	 * @param cTranspose if true, object is flipped over the major diagonal after rotation.
	 * @return the Orientation.
	 */
	public static Orientation fromC(int cAngle, boolean cTranspose)
	{
		return fromJava(cTranspose ? cAngle % 3600 + 900 : cAngle, false, cTranspose);
	}

	/**
	 * Get Orientation by the angle without mirrors.
	 * @param angle the angle of rotation (in tenth-degrees)
	 * @return the Orientation.
	 */
	public static Orientation fromAngle(int angle)
	{
		return fromJava(angle, false, false);
	}

    /**
     * Return inverse Orientation to this Orientation.
     * @return inverse Orientation.
     */
    public Orientation inverse() { return inverse; }
    
	/**
	 * Concatenates this Orientation with other Orientation.
	 * In matrix notation returns this * that.
	 * @param that other Orienation.
	 * @return concatenation of this and other Orientations.
	 */
	public Orientation concatenate(Orientation that)
	{
		boolean mirrorX = this.jMirrorX ^ that.jMirrorX;
		boolean mirrorY = this.jMirrorY ^ that.jMirrorY;
		int angle = that.jMirrorX^that.jMirrorY ? that.jAngle - this.jAngle : that.jAngle + this.jAngle;
		return fromJava(angle, mirrorX, mirrorY);
	}

//	/**
//	 * Concatenates other Orientation with this Orientation.
//	 * In matrix notation returns that * this.
//	 * @param that other Orienation.
//	 * @return concatenation of this and other Orientations.
//	 */
//	public Orientation getPreConcatenate(Orientation that)
//	{
//		boolean mirrorX = this.jMirrorX ^ that.jMirrorX;
//		boolean mirrorY = this.jMirrorY ^ that.jMirrorY;
//		int angle = this.jMirrorX^this.jMirrorY ? this.jAngle - that.jAngle : this.jAngle + that.jAngle;
//		return fromJava(angle, mirrorX, mirrorY);
//	}

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
	public int getAngle() { return jAngle; }
	/**
	 * Method to return the new Java style Mirror X factor.
	 * @return true to flip over the vertical axis (mirror in X).
	 */
	public boolean isXMirrored() { return jMirrorX; }
	/**
	 * Method to return the new Java style Mirror Y factor.
	 * @return true to flip over the horizontal axis (mirror in Y).
	 */
	public boolean isYMirrored() { return jMirrorY; }
	
	/**
	 * Method to return a transformation that rotates an object.
	 * @return a transformation that rotates by this Orinetation.
	 */
    public AffineTransform pureRotate() { return (AffineTransform)trans.clone(); }
    
	/**
	 * Method to return a transformation that rotates an object about a point.
	 * @param cX the center X coordinate about which to rotate.
	 * @param cY the center Y coordinate about which to rotate.
	 * @return a transformation that rotates about that point.
	 */
	public AffineTransform rotateAbout(double cX, double cY) {
        return rotateAbout(cX, cY, -cX, -cY);
    }
    
	/**
	 * Method to return a transformation that translate an object then rotates
     * and the again translates.
	 * @param aX the center X coordinate to translate after rotation.
	 * @param aY the center Y coordinate to translate afrer rotation.
	 * @param bX the center X coordinate to translate before rotation.
	 * @param bY the center Y coordinate to translate before rotation.
	 * @return a transformation that rotates about that point.
	 */
    public AffineTransform rotateAbout(double aX, double aY, double bX, double bY) {
        double m00 = trans.getScaleX();
        double m01 = trans.getShearX();
        double m10 = trans.getShearY();
        double m11 = trans.getScaleY();
        if (bX != 0 || bY != 0) {
            aX = aX + m00*bX + m01*bY;
            aY = aY + m11*bY + m10*bX;
        }
        
        return new AffineTransform(m00, m10, m01, m11, aX, aY);
    }
    
    /**
	 * Returns string which represents this Orientation in JELIB format.
	 * @return string in JELIB format.
	 */
	public String toJelibString() { return jString; }

	/**
	 * Returns text representation of this Orientation.
	 * @return text representation of this Orintation.
	 */
	public String toString() { return toJelibString(); }
}
