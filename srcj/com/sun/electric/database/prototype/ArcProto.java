/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ArcProto.java
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

import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Iterator;

/**
 * The ArcProto class contains basic information about an arc type.
 * There is only one ArcProto object for each type of wire or arc.
 */
public abstract class ArcProto extends ElectricObject
{
	/**
	 * ArcProto.Function is a typesafe enum class that describes the function of an arcproto.
	 */
	public static class Function
	{
		private final String name;

		private Function(String name) { this.name = name; }

		public String toString() { return name; }

		public static final Function UNKNOWN = new Function("unknown");
		public static final Function METAL1 = new Function("metal-1");
		public static final Function METAL2 = new Function("metal-2");
		public static final Function METAL3 = new Function("metal-3");
		public static final Function METAL4 = new Function("metal-4");
		public static final Function METAL5 = new Function("metal-5");
		public static final Function METAL6 = new Function("metal-6");
		public static final Function METAL7 = new Function("metal-7");
		public static final Function METAL8 = new Function("metal-8");
		public static final Function METAL9 = new Function("metal-9");
		public static final Function METAL10 = new Function("metal-10");
		public static final Function METAL11 = new Function("metal-11");
		public static final Function METAL12 = new Function("metal-12");
		public static final Function POLY1 = new Function("polysilicon-1");
		public static final Function POLY2 = new Function("polysilicon-2");
		public static final Function POLY3 = new Function("polysilicon-3");
		public static final Function DIFF = new Function("diffusion");
		public static final Function DIFFP = new Function("p-diffusion");
		public static final Function DIFFN = new Function("n-diffusion");
		public static final Function DIFFS = new Function("substrate-diffusion");
		public static final Function DIFFW = new Function("well-diffusion");
		public static final Function BUS = new Function("bus");
		public static final Function UNROUTED = new Function("unrouted");
		public static final Function NONELEC = new Function("nonelectrical");
	}

	// ----------------------- private data -------------------------------

	/** Name of this prototype */						protected String protoName;
	/** Technology of this type of arc */				protected Technology tech;
	/** Default width of this wire */					protected double defaultWidth;
	/** Width of other the material */					protected double widthOffset;
	/** Flags for this arc */							private int userBits;
	/** function of this arc */							private Function function;

	// the meaning of the "userBits" field:
	/** these arcs are fixed-length */					private static final int WANTFIX =            01;
	/** these arcs are fixed-angle */					private static final int WANTFIXANG =         02;
	/** set if arcs should not slide in ports */		private static final int WANTCANTSLIDE =      04;
	/** set if ends do not extend by half width */		private static final int WANTNOEXTEND =      010;
	/** set if arcs should be negated */				private static final int WANTNEGATED =       020;
	/** set if arcs should be directional */			private static final int WANTDIRECTIONAL =   040;
	/** set if arcs can wipe wipable nodes */			private static final int CANWIPE =          0100;
	/** set if arcs can curve */						private static final int CANCURVE =         0200;
	/** arc function (from efunction.h) */				private static final int AFUNCTION =      017400;
	/** right shift for AFUNCTION */					private static final int AFUNCTIONSH =         8;
	/** angle increment for this type of arc */			private static final int AANGLEINC =   017760000;
	/** right shift for AANGLEINC */					private static final int AANGLEINCSH =        13;
	/** set if arc is selectable by edge, not area */	private static final int AEDGESELECT = 020000000;

	// ----------------- protected and private methods -------------------------

	protected ArcProto()
	{
		this.userBits = 0;
		this.function = Function.UNKNOWN;
	}

	// ------------------------ public methods -------------------------------

	public String getProtoName() { return protoName; }

	public Technology getTechnology() { return tech; }

	public double getDefaultWidth() { return defaultWidth; }

	/** Get the default width of this type of arc.
	 *
	 * <p> Exclude the surrounding material. For example, diffusion arcs
	 * are always accompanied by a surrounding well and select. However,
	 * this call returns only the width of the diffusion. */
	public double getWidth()
	{
		return defaultWidth - widthOffset;
	}

	/** Set the Rigid bit */
	public void setRigid() { userBits |= WANTFIX; }
	/** Clear the Rigid bit */
	public void clearRigid() { userBits &= ~WANTFIX; }
	/** Get the Rigid bit */
	public boolean isRigid() { return (userBits & WANTFIX) != 0; }

	/** Set the Fixed-angle bit */
	public void setFixedAngle() { userBits |= WANTFIXANG; }
	/** Clear the Fixed-angle bit */
	public void clearFixedAngle() { userBits &= ~WANTFIXANG; }
	/** Get the Fixed-angle bit */
	public boolean isFixedAngle() { return (userBits & WANTFIXANG) != 0; }

	/** Set the Slidable bit */
	public void setSlidable() { userBits &= ~WANTCANTSLIDE; }
	/** Clear the Slidable bit */
	public void clearSlidable() { userBits |= WANTCANTSLIDE; }
	/** Get the Slidable bit */
	public boolean isSlidable() { return (userBits & WANTCANTSLIDE) == 0; }

	/** Set the End-extension bit */
	public void setExtended() { userBits &= ~WANTNOEXTEND; }
	/** Clear the End-extension bit */
	public void clearExtended() { userBits |= WANTNOEXTEND; }
	/** Get the End-extension bit */
	public boolean isExtended() { return (userBits & WANTNOEXTEND) != 0; }

	/** Set the Negated bit */
	public void setNegated() { userBits |= WANTNEGATED; }
	/** Clear the Negated bit */
	public void clearNegated() { userBits &= ~WANTNEGATED; }
	/** Get the Negated bit */
	public boolean isNegated() { return (userBits & WANTNEGATED) != 0; }

	/** Set the Directional bit */
	public void setDirectional() { userBits |= WANTDIRECTIONAL; }
	/** Clear the Directional bit */
	public void clearDirectional() { userBits &= ~WANTDIRECTIONAL; }
	/** Get the Directional bit */
	public boolean isDirectional() { return (userBits & WANTDIRECTIONAL) != 0; }

	/** Set the Can-Wipe bit */
	public void setWipable() { userBits |= CANWIPE; }
	/** Clear the Can-Wipe bit */
	public void clearWipable() { userBits &= ~CANWIPE; }
	/** Get the Can-Wipe bit */
	public boolean isWipable() { return (userBits & CANWIPE) != 0; }

	/** Set the Can-Curve bit */
	public void setCurvable() { userBits |= CANCURVE; }
	/** Clear the Can-Curve bit */
	public void clearCurvable() { userBits &= ~CANCURVE; }
	/** Get the Can-Curve bit */
	public boolean isCurvable() { return (userBits & CANCURVE) != 0; }

	/** Set the Edge-Select bit */
	public void setEdgeSelect() { userBits |= AEDGESELECT; }
	/** Clear the Edge-Select bit */
	public void clearEdgeSelect() { userBits &= ~AEDGESELECT; }
	/** Get the Edge-Select bit */
	public boolean isEdgeSelect() { return (userBits & AEDGESELECT) != 0; }

	/** Set the arc function */
	public void setFunction(ArcProto.Function function) { this.function = function; }
	/** Get the arc function */
	public ArcProto.Function getFunction() { return function; }

	/** Set the arc angle increment */
	public void setAngleIncrement(int value) { userBits = (userBits & ~AANGLEINC) | (value << AANGLEINCSH); }
	/** Get the arc angle increment */
	public int getAngleIncrement() { return (userBits & AANGLEINC) >> AANGLEINCSH; }

	public static ArcProto findArcProto(String line)
	{
		Technology tech = Technology.getCurrent();
		int colon = line.indexOf(':');
		String withoutPrefix;
		if (colon == -1) withoutPrefix = line; else
		{
			String prefix = line.substring(0, colon);
			Technology t = Technology.findTechnology(prefix);
			if (t != null) tech = t;
			withoutPrefix = line.substring(colon+1);
		}

		PrimitiveArc ap = tech.findArcProto(withoutPrefix);
		if (ap != null) return ap;
		return null;
	}

	/** returns a string that describes this ArcProto */
	public String describe()
	{
		String description = "";
		if (this instanceof PrimitiveArc)
		{
			Technology tech = ((PrimitiveArc)this).getTechnology();
			if (Technology.getCurrent() != tech)
				description += tech.getTechName() + ":";
		}
		description += protoName;
		return description;
	}

	/** printable version of this object */
	public String toString()
	{
		return "ArcProto " + describe();
	}

}
