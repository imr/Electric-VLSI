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

import com.sun.electric.database.text.Pref;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.tool.user.User;

import java.util.HashMap;

/**
 * The ArcProto class defines a type of ArcInst.
 * It is an abstract class that is implemented as PrimitiveArc (basic arcs from Technologies).
 * <P>
 * Every arc in the database appears as one <I>prototypical</I> object and many <I>instantiative</I> objects.
 * Thus, for a PrimitiveArc such as the CMOS Metal-1 there is one object (called a PrimitiveArc, which is a ArcProto)
 * that describes the wire prototype and there are many objects (called ArcInsts),
 * one for every instance of a Metal-1 wire that appears in a circuit.
 * PrimitiveArcs are statically created and placed in the Technology objects.
 * <P>
 * The basic ArcProto has a name, default width, function, and more.
 */
public abstract class ArcProto extends ElectricObject
{
	/**
	 * Function is a typesafe enum class that describes the function of an ArcProto.
	 * Functions are technology-independent and include different types of metal,
	 * polysilicon, and other basic wire types.
	 */
	public static class Function
	{
		private final String name;

		private Function(String name) { this.name = name; }

		public String toString() { return name; }

		/** Describes an arc with unknown type. */
		public static final Function UNKNOWN = new Function("unknown");
		/** Describes an arc on Metal layer 1. */
		public static final Function METAL1 = new Function("metal-1");
		/** Describes an arc on Metal layer 2. */
		public static final Function METAL2 = new Function("metal-2");
		/** Describes an arc on Metal layer 3. */
		public static final Function METAL3 = new Function("metal-3");
		/** Describes an arc on Metal layer 4. */
		public static final Function METAL4 = new Function("metal-4");
		/** Describes an arc on Metal layer 5. */
		public static final Function METAL5 = new Function("metal-5");
		/** Describes an arc on Metal layer 6. */
		public static final Function METAL6 = new Function("metal-6");
		/** Describes an arc on Metal layer 7. */
		public static final Function METAL7 = new Function("metal-7");
		/** Describes an arc on Metal layer 8. */
		public static final Function METAL8 = new Function("metal-8");
		/** Describes an arc on Metal layer 9. */
		public static final Function METAL9 = new Function("metal-9");
		/** Describes an arc on Metal layer 10. */
		public static final Function METAL10 = new Function("metal-10");
		/** Describes an arc on Metal layer 11. */
		public static final Function METAL11 = new Function("metal-11");
		/** Describes an arc on Metal layer 12. */
		public static final Function METAL12 = new Function("metal-12");
		/** Describes an arc on Polysilicon layer 1. */
		public static final Function POLY1 = new Function("polysilicon-1");
		/** Describes an arc on Polysilicon layer 2. */
		public static final Function POLY2 = new Function("polysilicon-2");
		/** Describes an arc on Polysilicon layer 3. */
		public static final Function POLY3 = new Function("polysilicon-3");
		/** Describes an arc on the Diffusion layer. */
		public static final Function DIFF = new Function("diffusion");
		/** Describes an arc on the P-Diffusion layer. */
		public static final Function DIFFP = new Function("p-diffusion");
		/** Describes an arc on the N-Diffusion layer. */
		public static final Function DIFFN = new Function("n-diffusion");
		/** Describes an arc on the Substrate-Diffusion layer. */
		public static final Function DIFFS = new Function("substrate-diffusion");
		/** Describes an arc on the Well-Diffusion layer. */
		public static final Function DIFFW = new Function("well-diffusion");
		/** Describes a bus arc. */
		public static final Function BUS = new Function("bus");
		/** Describes an arc that is unrouted (to be replaced by routers). */
		public static final Function UNROUTED = new Function("unrouted");
		/** Describes an arc that is non-electrical (does not make a circuit connection). */
		public static final Function NONELEC = new Function("nonelectrical");
	}

	// ----------------------- private data -------------------------------

	/** The name of this ArcProto. */							protected String protoName;
	/** The technology in which this ArcProto resides. */		protected Technology tech;
	/** The default width of this ArcProto. */					protected double defaultWidth;
	/** The offset from width to reported/displayed width. */	protected double widthOffset;
	/** Flags bits for this ArcProto. */						private int userBits;
	/** The function of this ArcProto. */						private Function function;
	/** A temporary integer for this ArcProto. */				private int tempInt;
	/** The temporary Object for this ArcProto. */				private Object tempObj;
	/** The temporary flag bits. */								private int flagBits;
	/** The object used to request flag bits. */				private static FlagSet.Generator flagGenerator = new FlagSet.Generator("ArcProto");
	/** Pref map for arc width. */								private static HashMap defaultWidthPrefs = new HashMap();
	/** Pref map for arc angle increment. */					private static HashMap defaultAnglePrefs = new HashMap();
	/** Pref map for arc rigidity. */							private static HashMap defaultRigidPrefs = new HashMap();
	/** Pref map for arc fixed angle. */						private static HashMap defaultFixedAnglePrefs = new HashMap();
	/** Pref map for arc slidable. */							private static HashMap defaultSlidablePrefs = new HashMap();
	/** Pref map for arc end extension. */						private static HashMap defaultExtendedPrefs = new HashMap();
	/** Pref map for arc negation. */							private static HashMap defaultNegatedPrefs = new HashMap();
	/** Pref map for arc directionality. */						private static HashMap defaultDirectionalPrefs = new HashMap();

	// the meaning of the "userBits" field:
//	/** these arcs are fixed-length */					private static final int WANTFIX =            01;
//	/** these arcs are fixed-angle */					private static final int WANTFIXANG =         02;
//	/** set if arcs should not slide in ports */		private static final int WANTCANTSLIDE =      04;
//	/** set if ends do not extend by half width */		private static final int WANTNOEXTEND =      010;
//	/** set if arcs should be negated */				private static final int WANTNEGATED =       020;
//	/** set if arcs should be directional */			private static final int WANTDIRECTIONAL =   040;
	/** set if arcs can wipe wipable nodes */			private static final int CANWIPE =          0100;
	/** set if arcs can curve */						private static final int CANCURVE =         0200;
	/** arc function (from efunction.h) */				private static final int AFUNCTION =      017400;
	/** right shift for AFUNCTION */					private static final int AFUNCTIONSH =         8;
//	/** angle increment for this type of arc */			private static final int AANGLEINC =   017760000;
//	/** right shift for AANGLEINC */					private static final int AANGLEINCSH =        13;
	/** set if arc is selectable by edge, not area */	private static final int AEDGESELECT = 020000000;
	/** set if arc is not used */						private static final int ANOTUSED = 020000000000;

	// ----------------- protected and private methods -------------------------

	/**
	 * This constructor should not be called.
	 * Use the subclass factory methods to create a PrimitiveArc object.
	 */
	protected ArcProto()
	{
		this.userBits = 0;
		this.function = Function.UNKNOWN;
		this.tempObj = null;
	}

	// ------------------------ public methods -------------------------------

	/**
	 * Method to return the name of this ArcProto.
	 * @return the name of this ArcProto.
	 */
	public String getProtoName() { return protoName; }

	/**
	 * Method to return the Technology of this ArcProto.
	 * @return the Technology of this ArcProto.
	 */
	public Technology getTechnology() { return tech; }

	private Pref getArcProtoWidthPref(double factory)
	{
		Pref pref = (Pref)defaultWidthPrefs.get(this);
		if (pref == null)
		{
			pref = Pref.makeDoublePref("DefaultWidthFor" + protoName + "IN" + tech.getTechName(), User.tool.prefs, factory);
			defaultWidthPrefs.put(this, pref);
		}
		return pref;
	}

	/**
	 * Method to set the factory-default width of this ArcProto.
	 * This is only called from PrimitiveArc during construction.
	 * @param cifLayer the factory-default width of this ArcProto.
	 */
	protected void setFactoryDefaultWidth(double defaultWidth) { getArcProtoWidthPref(defaultWidth); }

	/**
	 * Method to return the full default width of this ArcProto.
	 * This is the full width, including nonselectable layers such as implants.
	 * For example, diffusion arcs are always accompanied by a surrounding well and select.
	 * This call returns the width of all of these layers. 
	 * @return the full default width of this ArcProto.
	 */
	public void setDefaultWidth(double defaultWidth) { getArcProtoWidthPref(this.defaultWidth).setDouble(defaultWidth); }

	/**
	 * Method to set the full default width of this ArcProto.
	 * This is the full width, including nonselectable layers such as implants.
	 * For example, diffusion arcs are always accompanied by a surrounding well and select.
	 * @param defaultWidth the full default width of this ArcProto.
	 */
	public double getDefaultWidth() { return getArcProtoWidthPref(defaultWidth).getDouble(); }

	/**
	 * Method to set the width offset of this ArcProto.
	 * The width offset excludes the surrounding implang material.
	 * For example, diffusion arcs are always accompanied by a surrounding well and select.
	 * The offset amount is the difference between the diffusion width and the overall width.
	 * @param widthOffset the width offset of this ArcProto.
	 */
	public void setWidthOffset(double widthOffset)
	{
		if (widthOffset < 0.0)
		{
			System.out.println("ArcProto " + tech.getTechName() + ":" + protoName + " has negative width offset");
			return;
		}
		this.widthOffset = widthOffset;
	}

	/**
	 * Method to return the width offset of this ArcProto.
	 * The width offset excludes the surrounding implang material.
	 * For example, diffusion arcs are always accompanied by a surrounding well and select.
	 * The offset amount is the difference between the diffusion width and the overall width.
	 * @return the width offset of this ArcProto.
	 */
	public double getWidthOffset() { return widthOffset; }

	/**
	 * Method to return the default width of this ArcProto.
	 * This is the reported/selected width, which means that it does not include the width offset.
	 * For example, diffusion arcs are always accompanied by a surrounding well and select.
	 * This call returns only the width of the diffusion. 
	 * @return the default width of this ArcProto.
	 */
	public double getWidth()
	{
		return defaultWidth - widthOffset;
	}

	private Pref getArcProtoBitPref(String what, HashMap map, boolean factory)
	{
		Pref pref = (Pref)map.get(this);
		if (pref == null)
		{
			pref = Pref.makeBooleanPref("Default" + what + "For" + protoName + "IN" + tech.getTechName(), User.tool.prefs, factory);
			map.put(this, pref);
		}
		return pref;
	}

	/**
	 * Method to set the "factory default" rigid state of this ArcProto.
	 * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
	 * @param rigid true if this ArcProto should be rigid by factory-default.
	 */
	public void setFactoryRigid(boolean rigid) { getArcProtoBitPref("Rigid", defaultRigidPrefs, rigid); }

	/**
	 * Method to set the rigidity of this ArcProto.
	 * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
	 * @param rigid true if new instances of this ArcProto should be rigid.
	 */
	public void setRigid(boolean rigid) { getArcProtoBitPref("Rigid", defaultRigidPrefs, false).setBoolean(rigid); }

	/**
	 * Method to tell if instances of this ArcProto are rigid.
	 * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
	 * @return true if instances of this ArcProto are rigid.
	 */
	public boolean isRigid() { return getArcProtoBitPref("Rigid", defaultRigidPrefs, false).getBoolean(); }

	/**
	 * Method to set the "factory default" fixed-angle state of this ArcProto.
	 * Fixed-angle arcs cannot change their angle, so if one end moves,
	 * the other may also adjust to keep the arc angle constant.
	 * @param fixed true if this ArcProto should be fixed-angle by factory-default.
	 */
	public void setFactoryFixedAngle(boolean fixed) { getArcProtoBitPref("FixedAngle", defaultFixedAnglePrefs, fixed); }

	/**
	 * Method to set the fixed-angle state of this ArcProto.
	 * Fixed-angle arcs cannot change their angle, so if one end moves,
	 * the other may also adjust to keep the arc angle constant.
	 * @param fixed true if new instances of this ArcProto should be fixed-angle.
	 */
	public void setFixedAngle(boolean fixed) { getArcProtoBitPref("FixedAngle", defaultFixedAnglePrefs, true).setBoolean(fixed); }

	/**
	 * Method to tell if instances of this ArcProto are fixed-angle.
	 * Fixed-angle arcs cannot change their angle, so if one end moves,
	 * the other may also adjust to keep the arc angle constant.
	 * @return true if instances of this ArcProto are fixed-angle.
	 */
	public boolean isFixedAngle() { return getArcProtoBitPref("FixedAngle", defaultFixedAnglePrefs, true).getBoolean(); }

	/**
	 * Method to set the "factory default" slidability state of this ArcProto.
	 * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
	 * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
	 * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
	 * @param fixed true if this ArcProto should be slidability by factory-default.
	 */
	public void setFactorySlidable(boolean slidable) { getArcProtoBitPref("Slidable", defaultSlidablePrefs, slidable); }

	/**
	 * Method to set the slidability of this ArcProto.
	 * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
	 * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
	 * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
	 * @param slidable true if new instances of this ArcProto should be slidable.
	 */
	public void setSlidable(boolean slidable) { getArcProtoBitPref("Slidable", defaultSlidablePrefs, true).setBoolean(slidable); }

	/**
	 * Method to tell if instances of this ArcProto are slidable.
	 * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
	 * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
	 * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
	 * @return true if instances of this ArcProto are slidable.
	 */
	public boolean isSlidable() { return getArcProtoBitPref("Slidable", defaultSlidablePrefs, true).getBoolean(); }

	/**
	 * Method to set the "factory default" end-extension state of this ArcProto.
	 * End-extension causes an arc to extend past its endpoint by half of its width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 * @param fixed true if this ArcProto should be end-extended by factory-default.
	 */
	public void setFactoryExtended(boolean extended) { getArcProtoBitPref("Extended", defaultExtendedPrefs, extended); }

	/**
	 * Method to set the end-extension factor of this ArcProto.
	 * End-extension causes an arc to extend past its endpoint by half of its width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 * @param extended true if new instances of this ArcProto should be end-extended.
	 */
	public void setExtended(boolean extended) { getArcProtoBitPref("Extended", defaultExtendedPrefs, true).setBoolean(extended); }

	/**
	 * Method to tell if instances of this ArcProto have their ends extended.
	 * End-extension causes an arc to extend past its endpoint by half of its width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 * @return true if instances of this ArcProto have their ends extended.
	 */
	public boolean isExtended() { return getArcProtoBitPref("Extended", defaultExtendedPrefs, true).getBoolean(); }

	/**
	 * Method to set the negated factor for this ArcProto.
	 * Negated arcs have a bubble drawn on their tail end to indicate negation.
	 * This is used only in Schematics technologies to place negating bubbles on any node.
	 * @param negated true if new instances of this ArcProto should be negated.
	 */
	public void setNegated(boolean negated) { getArcProtoBitPref("Negated", defaultNegatedPrefs, false).setBoolean(negated); }

	/**
	 * Method to tell if instances of this ArcProto are negated.
	 * Negated arcs have a bubble drawn on their tail end to indicate negation.
	 * This is used only in Schematics technologies to place negating bubbles on any node.
	 * @return true if instances of this ArcProto are negated.
	 */
	public boolean isNegated() { return getArcProtoBitPref("Negated", defaultNegatedPrefs, false).getBoolean(); }

	/**
	 * Method to set the directional factor for this ArcProto.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @param directional true if new instances of this ArcProto should be directional.
	 */
	public void setDirectional(boolean directional) { getArcProtoBitPref("Directional", defaultDirectionalPrefs, false).setBoolean(directional); }

	/**
	 * Method to tell if instances of this ArcProto are directional.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @return true if instances of this ArcProto are directional.
	 */
	public boolean isDirectional() { return getArcProtoBitPref("Directional", defaultDirectionalPrefs, false).getBoolean(); }

	/**
	 * Method to set this ArcProto so that it is not used.
	 * Unused arcs do not appear in the component menus and cannot be created by the user.
	 * The state is useful for hiding arcs that the user should not use.
	 */
	public void setNotUsed() { checkChanging(); userBits |= ANOTUSED; }

	/**
	 * Method to set this ArcProto so that it is used.
	 * Unused arcs do not appear in the component menus and cannot be created by the user.
	 * The state is useful for hiding arcs that the user should not use.
	 */
	public void clearNotUsed() { checkChanging(); userBits &= ~ANOTUSED; }

	/**
	 * Method to tell if this ArcProto is used.
	 * Unused arcs do not appear in the component menus and cannot be created by the user.
	 * The state is useful for hiding arcs that the user should not use.
	 * @return true if this ArcProto is used.
	 */
	public boolean isNotUsed() { return (userBits & ANOTUSED) != 0; }

	/**
	 * Method to set this ArcProto so that instances of it can wipe nodes.
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Those arc prototypes that can erase their connecting pins have this state set,
	 * and when instances of these arcs connect to the pins, those pins stop being drawn.
	 * It is necessary for the pin node prototype to enable wiping (with setArcsWipe).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 * @see NodeProto#setArcsWipe
	 * @see NodeInst#setWiped
	 */
	public void setWipable() { userBits |= CANWIPE; }

	/**
	 * Method to set this ArcProto so that instances of it cannot wipe nodes.
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Those arc prototypes that can erase their connecting pins have this state set,
	 * and when instances of these arcs connect to the pins, those pins stop being drawn.
	 * It is necessary for the pin node prototype to enable wiping (with setArcsWipe).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 * @see NodeProto#setArcsWipe
	 * @see NodeInst#setWiped
	 */
	public void clearWipable() { userBits &= ~CANWIPE; }

	/**
	 * Method to tell if instances of this ArcProto can wipe nodes.
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Those arc prototypes that can erase their connecting pins have this state set,
	 * and when instances of these arcs connect to the pins, those pins stop being drawn.
	 * It is necessary for the pin node prototype to enable wiping (with setArcsWipe).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 * @return true if instances of this ArcProto can wipe nodes.
	 * @see NodeProto#setArcsWipe
	 * @see NodeInst#setWiped
	 */
	public boolean isWipable() { return (userBits & CANWIPE) != 0; }

	/**
	 * Method to set this ArcProto so that instances of it can curve.
	 * Since arc curvature is complex to draw, arcs with this capability
	 * must be marked this way.
	 * A curved arc has the variable "arc_radius" on it with a curvature factor.
	 */
	public void setCurvable() { userBits |= CANCURVE; }

	/**
	 * Method to set this ArcProto so that instances of it cannot curve.
	 * Since arc curvature is complex to draw, arcs with this capability
	 * must be marked this way.
	 * A curved arc has the variable "arc_radius" on it with a curvature factor.
	 */
	public void clearCurvable() { userBits &= ~CANCURVE; }

	/**
	 * Method to tell if instances of this ArcProto can curve.
	 * Since arc curvature is complex to draw, arcs with this capability
	 * must be marked this way.
	 * A curved arc has the variable "arc_radius" on it with a curvature factor.
	 * @return true if instances of this ArcProto can curve.
	 */
	public boolean isCurvable() { return (userBits & CANCURVE) != 0; }

	/**
	 * Method to set this ArcProto so that instances of it can be selected by their edge.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 */
	public void setEdgeSelect() { userBits |= AEDGESELECT; }

	/**
	 * Method to set this ArcProto so that instances of it cannot be selected by their edge.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 */
	public void clearEdgeSelect() { userBits &= ~AEDGESELECT; }

	/**
	 * Method to tell if instances of this ArcProto can be selected by their edge.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 * @return true if instances of this ArcProto can be selected by their edge.
	 */
	public boolean isEdgeSelect() { return (userBits & AEDGESELECT) != 0; }

	/**
	 * Method to set the function of this ArcProto.
	 * The Function is a technology-independent description of the behavior of this ArcProto.
	 * @param function the new function of this ArcProto.
	 */
	public void setFunction(ArcProto.Function function) { this.function = function; }

	/**
	 * Method to return the function of this ArcProto.
	 * The Function is a technology-independent description of the behavior of this ArcProto.
	 * @return function the function of this ArcProto.
	 */
	public ArcProto.Function getFunction() { return function; }

	/**
	 * Method to set the factory-default angle of this ArcProto.
	 * This is only called from PrimitiveArc during construction.
	 * @param cifLayer the factory-default angle of this ArcProto.
	 */
	public void setFactoryAngleIncrement(int angle)
	{
		Pref pref = Pref.makeIntPref("DefaultAngleFor" + protoName + "IN" + tech.getTechName(), User.tool.prefs, angle);
		defaultAnglePrefs.put(this, pref);
	}

	/**
	 * Method to set the angle increment on this ArcProto.
	 * The angle increment is the granularity on placement angle for instances
	 * of this ArcProto.  It is in degrees.
	 * For example, a value of 90 requests that instances run at 0, 90, 180, or 270 degrees.
	 * A value of 0 allows arcs to be created at any angle.
	 * @param value the angle increment on this ArcProto.
	 */
	public void setAngleIncrement(int angle)
	{
		Pref pref = (Pref)defaultAnglePrefs.get(this);
		if (pref == null) return;
		pref.setInt(angle);
	}

	/**
	 * Method to get the angle increment on this ArcProto.
	 * The angle increment is the granularity on placement angle for instances
	 * of this ArcProto.  It is in degrees.
	 * For example, a value of 90 requests that instances run at 0, 90, 180, or 270 degrees.
	 * A value of 0 allows arcs to be created at any angle.
	 * @return the angle increment on this ArcProto.
	 */
	public int getAngleIncrement()
	{
		Pref pref = (Pref)defaultAnglePrefs.get(this);
		if (pref == null) return 90;
		return pref.getInt();
	}

	/**
	 * Method to get access to flag bits on this ArcProto.
	 * Flag bits allow ArcProtos to be marked and examined more conveniently.
	 * However, multiple competing activities may want to mark the nodes at
	 * the same time.  To solve this, each activity that wants to mark nodes
	 * must create a FlagSet that allocates bits in the node.  When done,
	 * the FlagSet must be released.
	 * @param numBits the number of flag bits desired.
	 * @return a FlagSet object that can be used to mark and test the ArcProto.
	 */
	public static FlagSet getFlagSet(int numBits) { return FlagSet.getFlagSet(flagGenerator, numBits); }

	/**
	 * Method to set the specified flag bits on this ArcProto.
	 * @param set the flag bits that are to be set on this ArcProto.
	 */
	public void setBit(FlagSet set) { /*checkChanging();*/ flagBits = flagBits | set.getMask(); }

	/**
	 * Method to set the specified flag bits on this ArcProto.
	 * @param set the flag bits that are to be cleared on this ArcProto.
	 */
	public void clearBit(FlagSet set) { /*checkChanging();*/ flagBits = flagBits & set.getUnmask(); }

	/**
	 * Method to test the specified flag bits on this ArcProto.
	 * @param set the flag bits that are to be tested on this ArcProto.
	 * @return true if the flag bits are set.
	 */
	public boolean isBit(FlagSet set) { return (flagBits & set.getMask()) != 0; }

	/**
	 * Method to set an arbitrary integer in a temporary location on this ArcProto.
	 * @param tempInt the integer to be set on this ArcProto.
	 */
	public void setTempInt(int tempInt) { this.tempInt = tempInt; }

	/**
	 * Method to get the temporary integer on this ArcProto.
	 * @return the temporary integer on this ArcProto.
	 */
	public int getTempInt() { return tempInt; }

	/**
	 * Method to set an arbitrary Object in a temporary location on this ArcProto.
	 * @param tempObj the Object to be set on this ArcProto.
	 */
	public void setTempObj(Object tempObj) { checkChanging(); this.tempObj = tempObj; }

	/**
	 * Method to get the temporary Object on this ArcProto.
	 * @return the temporary Object on this ArcProto.
	 */
	public Object getTempObj() { return tempObj; }

	/**
	 * Method to find the ArcProto with the given name.
	 * This can be prefixed by a Technology name.
	 * @param line the name of the ArcProto.
	 * @return the specified ArcProto, or null if none can be found.
	 */
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

	/**
	 * Method to describe this ArcProto as a string.
	 * Prepends the Technology name if it is
	 * not from the current technology (for example, "mocmos:Polysilicon-1").
	 * @return a String describing this ArcProto.
	 */
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

	/**
	 * Returns a printable version of this ArcProto.
	 * @return a printable version of this ArcProto.
	 */
	public String toString()
	{
		return "ArcProto " + describe();
	}

}
