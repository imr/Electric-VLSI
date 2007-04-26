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
package com.sun.electric.technology;

import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.text.Pref;
import com.sun.electric.tool.user.User;
import java.awt.geom.Point2D;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * The ArcProto class defines a type of ArcInst.
 * <P>
 * Every arc in the database appears as one <I>prototypical</I> object and many <I>instantiative</I> objects.
 * Thus, for a ArcProto such as the CMOS Metal-1 there is one object (called a ArcProto)
 * that describes the wire prototype and there are many objects (called ArcInsts),
 * one for every instance of a Metal-1 wire that appears in a circuit.
 * ArcProtos are statically created and placed in the Technology objects.
 * <P>
 * The basic ArcProto has a name, default width, function, Layers that describes it graphically and more.
 */
public class ArcProto implements Comparable<ArcProto>
{
	/**
	 * Function is a typesafe enum class that describes the function of an ArcProto.
	 * Functions are technology-independent and include different types of metal,
	 * polysilicon, and other basic wire types.
	 */
	public static enum Function
	{
		/** Describes an arc with unknown type. */
		UNKNOWN("unknown", 0, 0),
		/** Describes an arc on Metal layer 1. */
		METAL1("metal-1", 1, 0),
		/** Describes an arc on Metal layer 2. */
		METAL2("metal-2", 2, 0),
		/** Describes an arc on Metal layer 3. */
		METAL3("metal-3", 3, 0),
		/** Describes an arc on Metal layer 4. */
		METAL4("metal-4", 4, 0),
		/** Describes an arc on Metal layer 5. */
		METAL5("metal-5", 5, 0),
		/** Describes an arc on Metal layer 6. */
		METAL6("metal-6", 6, 0),
		/** Describes an arc on Metal layer 7. */
		METAL7("metal-7", 7, 0),
		/** Describes an arc on Metal layer 8. */
		METAL8("metal-8", 8, 0),
		/** Describes an arc on Metal layer 9. */
		METAL9("metal-9", 9, 0),
		/** Describes an arc on Metal layer 10. */
		METAL10("metal-10", 10, 0),
		/** Describes an arc on Metal layer 11. */
		METAL11("metal-11", 11, 0),
		/** Describes an arc on Metal layer 12. */
		METAL12("metal-12", 12, 0),
		/** Describes an arc on Polysilicon layer 1. */
		POLY1("polysilicon-1", 0, 1),
		/** Describes an arc on Polysilicon layer 2. */
		POLY2("polysilicon-2", 0, 2),
		/** Describes an arc on Polysilicon layer 3. */
		POLY3("polysilicon-3", 0, 3),
		/** Describes an arc on the Diffusion layer. */
		DIFF("diffusion", 0, 0),
		/** Describes an arc on the P-Diffusion layer. */
		DIFFP("p-diffusion", 0, 0),
		/** Describes an arc on the N-Diffusion layer. */
		DIFFN("n-diffusion", 0, 0),
		/** Describes an arc on the Substrate-Diffusion layer. */
		DIFFS("substrate-diffusion", 0, 0),
		/** Describes an arc on the Well-Diffusion layer. */
		DIFFW("well-diffusion", 0, 0),
		/** Describes a bus arc. */
		BUS("bus", 0, 0),
		/** Describes an arc that is unrouted (to be replaced by routers). */
		UNROUTED("unrouted", 0, 0),
		/** Describes an arc that is non-electrical (does not make a circuit connection). */
		NONELEC("nonelectrical", 0, 0);

		private final String printName;
		private final int level;
        private final boolean isMetal;
        private final boolean isPoly;
        private final boolean isDiffusion;
        
        private static final Function[] metalLayers = initMetalLayers(Function.class.getEnumConstants());
        private static final Function[] polyLayers = initPolyLayers(Function.class.getEnumConstants());
        
        private Function(String printName, int metalLevel, int polyLevel) {
            this.printName = printName;
            isMetal = metalLevel != 0;
            isPoly = polyLevel != 0;
            isDiffusion = name().startsWith("DIFF");
            level = isMetal ? metalLevel : isPoly ? polyLevel : 0;
        }
	       
		/**
		 * Returns a printable version of this ArcProto.
		 * @return a printable version of this ArcProto.
		 */
		public String toString() { return printName; }

		/**
		 * Returns the constant name for this Function.
		 * Constant names are used when writing Java code, so they must be the same as the actual symbol name.
		 * @return the constant name for this Function.
		 */
		public String getConstantName() { return name(); }

		/**
		 * Method to return a List of all ArcProto functions.
		 * @return a List of all ArcProto functions.
		 */
		public static List<Function> getFunctions() { return Arrays.asList(Function.class.getEnumConstants()); }

		/**
		 * Method to get the level of this ArcProto.Function.
		 * The level applies to metal and polysilicon functions, and gives the layer number
		 * (i.e. Metal-2 is level 2).
		 * @return the level of this ArcProto.Function.
		 */
		public int getLevel() { return level; }

		/**
		 * Method to find the Function that corresponds to Metal on a given layer.
		 * @param level the layer (starting at 1 for Metal-1).
		 * @return the Function that represents that Metal layer.
		 */
        public static Function getMetal(int level) {
            return level < metalLayers.length ? metalLayers[level] : null;
        }

		/**
		 * Method to find the Function that corresponds to Polysilicon on a given layer.
		 * @param level the layer (starting at 1 for Polysilicon-1).
		 * @return the Function that represents that Polysilicon layer.
		 */
        public static Function getPoly(int level) {
            return level < polyLayers.length ? polyLayers[level] : null;
        }
        
		/**
		 * Method to tell whether this ArcProto.Function is metal.
		 * @return true if this ArcProto.Function is metal.
		 */
		public boolean isMetal() { return isMetal; }

		/**
		 * Method to tell whether this ArcProto.Function is polysilicon.
		 * @return true if this ArcProto.Function is polysilicon.
		 */
		public boolean isPoly() { return isPoly; }

		/**
		 * Method to tell whether this ArcProto.Function is diffusion.
		 * @return true if this ArcProto.Function is diffusion.
		 */
		public boolean isDiffusion() { return isDiffusion; }
        
        private static Function[] initMetalLayers(Function[] allFunctions) {
            int maxLevel = -1;
            for (Function fun: getFunctions()) {
                if (!fun.isMetal()) continue;
                maxLevel = Math.max(maxLevel, fun.level);
            }
            Function[] layers = new Function[maxLevel + 1];
            for (Function fun: getFunctions()) {
                if (!fun.isMetal()) continue;
                assert layers[fun.level] == null;
                layers[fun.level] = fun;
            }
            return layers;
        }
        
        private static Function[] initPolyLayers(Function[] allFunctions) {
            int maxLevel = -1;
            for (Function fun: getFunctions()) {
                if (!fun.isPoly()) continue;
                maxLevel = Math.max(maxLevel, fun.level);
            }
            Function[] layers = new Function[maxLevel + 1];
            for (Function fun: getFunctions()) {
                if (!fun.isPoly()) continue;
                assert layers[fun.level] == null;
                layers[fun.level] = fun;
            }
            return layers;
        }
	}
    

	// ----------------------- private data -------------------------------

	/** Pref map for arc width. */								private static HashMap<ArcProto,Pref> defaultWidthPrefs = new HashMap<ArcProto,Pref>();
	/** Pref map for arc angle increment. */					private static HashMap<ArcProto,Pref> defaultAnglePrefs = new HashMap<ArcProto,Pref>();
	/** Pref map for arc rigidity. */							private static HashMap<ArcProto,Pref> defaultRigidPrefs = new HashMap<ArcProto,Pref>();
	/** Pref map for arc fixed angle. */						private static HashMap<ArcProto,Pref> defaultFixedAnglePrefs = new HashMap<ArcProto,Pref>();
	/** Pref map for arc slidable. */							private static HashMap<ArcProto,Pref> defaultSlidablePrefs = new HashMap<ArcProto,Pref>();
	/** Pref map for arc end extension. */						private static HashMap<ArcProto,Pref> defaultExtendedPrefs = new HashMap<ArcProto,Pref>();
//	/** Pref map for arc negation. */							private static HashMap<ArcProto,Pref> defaultNegatedPrefs = new HashMap<ArcProto,Pref>();
	/** Pref map for arc directionality. */						private static HashMap<ArcProto,Pref> defaultDirectionalPrefs = new HashMap<ArcProto,Pref>();
	/** The name of this ArcProto. */							protected final String protoName;
	/** The technology in which this ArcProto resides. */		protected final Technology tech;
	/** The offset from width to reported/displayed width. */	protected final int gridWidthOffset;
    /** The maximum offset among ArcLayers. */                  private int maxLayerGridOffset;
	/** Flags bits for this ArcProto. */						private int userBits;
	/** The function of this ArcProto. */						final Function function;
	/** Layers in this arc */                                   final Technology.ArcLayer [] layers;
	/** Full name */                                            final String fullName;
	/** Index of this ArcProto. */                              final int primArcIndex;
//	/** A temporary integer for this ArcProto. */				private int tempInt;

	// the meaning of the "userBits" field:
//	/** these arcs are fixed-length */							private static final int WANTFIX  =            01;
//	/** these arcs are fixed-angle */							private static final int WANTFIXANG  =         02;
//	/** set if arcs should not slide in ports */				private static final int WANTCANTSLIDE  =      04;
//	/** set if ends do not extend by half width */				private static final int WANTNOEXTEND  =      010;
//	/** set if arcs should be negated */						private static final int WANTNEGATED  =       020;
//	/** set if arcs should be directional */					private static final int WANTDIRECTIONAL  =   040;
	/** set if arcs can wipe wipable nodes */					private static final int CANWIPE  =          0100;
	/** set if arcs can curve */								private static final int CANCURVE  =         0200;
//	/** arc function (from efunction.h) */						private static final int AFUNCTION  =      017400;
//	/** right shift for AFUNCTION */							private static final int AFUNCTIONSH  =         8;
//	/** angle increment for this type of arc */					private static final int AANGLEINC  =   017760000;
//	/** right shift for AANGLEINC */							private static final int AANGLEINCSH  =        13;
    /** set if arc is not selectable in palette */			    private static final int ARCSPECIAL  = 010000000;
	/** set if arc is selectable by edge, not area */			private static final int AEDGESELECT  = 020000000;
	/** set if arc is invisible and unselectable */				private static final int AINVISIBLE   = 040000000;
	/** set if arc is not used */								private static final int ANOTUSED  = 020000000000;
	/** set if node will be considered in palette */            private static final int SKIPSIZEINPALETTE =    0400;

	// ----------------- protected and private methods -------------------------

	/**
	 * The constructor is never called.  Use "Technology.newArcProto" instead.
	 */
	ArcProto(Technology tech, String protoName, long gridWidthOffset, double defaultWidth, Function function, Technology.ArcLayer [] layers, int primArcIndex)
	{
		if (!Technology.jelibSafeName(protoName))
			System.out.println("ArcProto name " + protoName + " is not safe to write into JELIB");
		this.protoName = protoName;
		this.fullName = tech.getTechName() + ":" + protoName;
        assert (gridWidthOffset&1) == 0;
        this.gridWidthOffset = (int)gridWidthOffset;
		this.tech = tech;
		this.userBits = 0;
		this.function = function;
		this.layers = layers.clone();
        this.primArcIndex = primArcIndex;
        computeMaxLayerGridOffset();
		setFactoryDefaultLambdaFullWidth(defaultWidth);
	}

	// ------------------------ public methods -------------------------------

	/**
	 * Method to return the name of this ArcProto.
	 * @return the name of this ArcProto.
	 */
	public String getName() { return protoName; }

	/**
	 * Method to return the full name of this ArcProto.
	 * Full name has format "techName:primName"
	 * @return the full name of this ArcProto.
	 */
	public String getFullName() { return fullName; }

	/**
	 * Method to return the Technology of this ArcProto.
	 * @return the Technology of this ArcProto.
	 */
	public Technology getTechnology() { return tech; }

	private Pref getArcProtoWidthPref(double factory)
	{
		Pref pref = defaultWidthPrefs.get(this);
		if (pref == null)
		{
			pref = Pref.makeDoublePref("DefaultWidthFor" + protoName + "IN" + tech.getTechName(), Technology.getTechnologyPreferences(), factory);
			defaultWidthPrefs.put(this, pref);
		}
		return pref;
	}

	/**
	 * Method to set the factory-default width of this ArcProto in lambda units.
	 * This is only called from ArcProto during construction.
	 * @param lambdaWidth the factory-default full width of this ArcProto in lambda units.
	 */
	protected void setFactoryDefaultLambdaFullWidth(double lambdaWidth) {
        long gridWidth = DBMath.lambdaToSizeGrid(lambdaWidth);
        if (gridWidth < 0 || gridWidth > Integer.MAX_VALUE) {
            System.out.println("ArcProto " + tech.getTechName() + ":" + protoName + " has invalid default full width " + lambdaWidth);
            return;
        }
        assert (gridWidth&1) == 0;
        getArcProtoWidthPref(DBMath.gridToLambda(gridWidth));
    }

	/**
     * Method to set the full default width of this ArcProto in lambda units.
     * This is the full width, including nonselectable layers such as implants.
     * For example, diffusion arcs are always accompanied by a surrounding well and select.
     * This call returns the width of all of these layers.
     * @param lambdaWidth the full default width of this ArcProto in lambda units.
     * @return returns true if preference was really changed.
     */
    public boolean setDefaultLambdaFullWidth(double lambdaWidth) {
        long gridWidth = DBMath.lambdaToSizeGrid(lambdaWidth);
        if (gridWidth < 0 || gridWidth > Integer.MAX_VALUE) {
            System.out.println("ArcProto " + tech.getTechName() + ":" + protoName + " has invalid default full width " + lambdaWidth);
            return false;
        }
        assert (gridWidth&1) == 0;
        return getArcProtoWidthPref(0).setDouble(DBMath.gridToLambda(gridWidth));
    }

	/**
	 * Method to return the full default width of this ArcProto in lambda units.
	 * This is the full width, including nonselectable layers such as implants.
	 * For example, diffusion arcs are always accompanied by a surrounding well and select.
	 * @return the full default width of this ArcProto in lambda units.
	 */
	public double getDefaultLambdaFullWidth() { return DBMath.gridToLambda(getDefaultGridFullWidth()); }

	/**
	 * Method to return the full default width of this ArcProto in lambda units.
	 * This is the full width, including nonselectable layers such as implants.
	 * For example, diffusion arcs are always accompanied by a surrounding well and select.
	 * @return the full default width of this ArcProto in lambda units.
	 */
	public long getDefaultGridFullWidth() {
        return DBMath.lambdaToSizeGrid(getArcProtoWidthPref(0).getDouble());
    }

	/**
	 * Method to return the default base width of this ArcProto in lambda units.
	 * This is the reported/selected width, which means that it does not include the width offset.
	 * For example, diffusion arcs are always accompanied by a surrounding well and select.
	 * This call returns only the width of the diffusion. 
	 * @return the default base width of this ArcProto in lambda units.
	 */
	public double getDefaultLambdaBaseWidth() { return DBMath.gridToLambda(getDefaultGridBaseWidth()); }

	/**
	 * Method to return the default base width of this ArcProto in grid units.
	 * This is the reported/selected width, which means that it does not include the width offset.
	 * For example, diffusion arcs are always accompanied by a surrounding well and select.
	 * This call returns only the width of the diffusion. 
	 * @return the default base width of this ArcProto in grid units.
	 */
    public long getDefaultGridBaseWidth() { return getDefaultGridFullWidth() - getGridWidthOffset(); }

	/**
	 * Method to return the width offset of this ArcProto in lambda units.
	 * The width offset excludes the surrounding implang material.
	 * For example, diffusion arcs are always accompanied by a surrounding well and select.
	 * The offset amount is the difference between the diffusion width and the overall width.
	 * @return the width offset of this ArcProto in lambda units.
	 */
	public double getLambdaWidthOffset() { return DBMath.gridToLambda(getGridWidthOffset()); }

	/**
	 * Method to return the width offset of this ArcProto in grid units.
	 * The width offset excludes the surrounding implang material.
	 * For example, diffusion arcs are always accompanied by a surrounding well and select.
	 * The offset amount is the difference between the diffusion width and the overall width.
	 * @return the width offset of this ArcProto in grid units.
	 */
	public long getGridWidthOffset() { return gridWidthOffset; }

	/**
	 * Method to return the width offset of this ArcProto in grid units.
	 * The width offset excludes the surrounding implang material.
	 * For example, diffusion arcs are always accompanied by a surrounding well and select.
	 * The offset amount is the difference between the diffusion width and the overall width.
	 * @return the width offset of this ArcProto in grid units.
	 */
    public int getMaxLayerGridOffset() { return maxLayerGridOffset; }
    
    /*
	private Pref getArcProtoAntennaPref()
	{
		Pref pref = defaultAntennaRatioPrefs.get(this);
		if (pref == null)
		{
			double factory = ERCAntenna.DEFPOLYRATIO;
			if (function.isMetal()) factory = ERCAntenna.DEFMETALRATIO;
			pref = Pref.makeDoublePref("DefaultAntennaRatioFor" + protoName + "IN" + tech.getTechName(), ERC.tool.prefs, factory);
			defaultAntennaRatioPrefs.put(this, pref);
		}
		return pref;
	}
    */

	/**
	 * Method to set the antenna ratio of this ArcProto.
	 * Antenna ratios are used in antenna checks that make sure the ratio of the area of a layer is correct.
	 * @param ratio the antenna ratio of this ArcProto.
	 */
	//public void setAntennaRatio(double ratio) { getArcProtoAntennaPref().setDouble(ratio); }

	/**
	 * Method to tell the antenna ratio of this ArcProto.
	 * Antenna ratios are used in antenna checks that make sure the ratio of the area of a layer is correct.
	 * @return the antenna ratio of this ArcProto.
	 */
	//public double getAntennaRatio() { return getArcProtoAntennaPref().getDouble(); }

	private Pref getArcProtoBitPref(String what, HashMap<ArcProto,Pref> map, boolean factory)
	{
		Pref pref = map.get(this);
		if (pref == null)
		{
			pref = Pref.makeBooleanPref("Default" + what + "For" + protoName + "IN" + tech.getTechName(), User.getUserTool().prefs, factory);
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
	 * @param slidable true if this ArcProto should be slidability by factory-default.
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
	 * @param extended true if this ArcProto should be end-extended by factory-default.
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
     * @param set
     */
	public void setNotUsed(boolean set)
    {
        /* checkChanging();*/
        if (set) userBits |= ANOTUSED;
        else userBits &= ~ANOTUSED;
    }

	/**
	 * Method to tell if this ArcProto is used.
	 * Unused arcs do not appear in the component menus and cannot be created by the user.
	 * The state is useful for hiding arcs that the user should not use.
	 * @return true if this ArcProto is used.
	 */
	public boolean isNotUsed() { return (userBits & ANOTUSED) != 0; }

	/**
	 * Method to set this ArcProto to be completely invisible, and unselectable.
	 * When all of its layers have been made invisible, the node is flagged to be invisible.
	 * @param invisible true to set this ArcProto to be completely invisible and unselectable.
	 */
	public void setArcInvisible(boolean invisible)
	{
		if (invisible) userBits |= AINVISIBLE; else
			userBits &= ~AINVISIBLE;
	}

	/**
	 * Method to tell if instances of this ArcProto are invisible.
	 * When all of its layers have been made invisible, the node is flagged to be invisible.
	 * @return true if instances of this ArcProto are invisible.
	 */
	public boolean isArcInvisible() { return (userBits & AINVISIBLE) != 0; }

    /**
	 * Method to allow instances of this ArcProto not to be considered in
     * tech palette for the calculation of the largest icon.
	 * Valid for menu display
	 */
	public void setSkipSizeInPalette() { userBits |= SKIPSIZEINPALETTE; }

	/**
	 * Method to tell if instaces of this ArcProto are special (don't appear in menu).
	 * Valid for menu display
	 */
	public boolean isSkipSizeInPalette() { return (userBits & SKIPSIZEINPALETTE) != 0; }
    
    /**
	 * Method to set this ArcProto so that instances of it can wipe nodes.
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Those arc prototypes that can erase their connecting pins have this state set,
	 * and when instances of these arcs connect to the pins, those pins stop being drawn.
	 * It is necessary for the pin node prototype to enable wiping (with setArcsWipe).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 */
	public void setWipable() { userBits |= CANWIPE; }

	/**
	 * Method to set this ArcProto so that instances of it cannot wipe nodes.
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Those arc prototypes that can erase their connecting pins have this state set,
	 * and when instances of these arcs connect to the pins, those pins stop being drawn.
	 * It is necessary for the pin node prototype to enable wiping (with setArcsWipe).
	 * A NodeInst that becomes wiped out has "setWiped" called.
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
	 * Method to allow instances of this ArcProto to be special in menu.
	 * Valid for menu display
	 */
	public void setSpecialArc() { userBits |= ARCSPECIAL; }

	/**
	 * Method to tell if instaces of this ArcProto are special (don't appear in menu).
	 * Valid for menu display
	 */
	public boolean isSpecialArc() { return (userBits & ARCSPECIAL) != 0; }

	/**
	 * Method to get default ArcInst flags with this portoType.
	 */
	public int getDefaultConstraints()
	{
        int flags = ImmutableArcInst.DEFAULT_FLAGS;
        flags = ImmutableArcInst.RIGID.set(flags, isRigid());
        flags = ImmutableArcInst.FIXED_ANGLE.set(flags, isFixedAngle());
        flags = ImmutableArcInst.SLIDABLE.set(flags, isSlidable());
        flags = ImmutableArcInst.HEAD_EXTENDED.set(flags, isExtended());
        flags = ImmutableArcInst.TAIL_EXTENDED.set(flags, isExtended());
        flags = ImmutableArcInst.HEAD_ARROWED.set(flags, isDirectional());
        flags = ImmutableArcInst.BODY_ARROWED.set(flags, isDirectional());
        return flags;
	}

	/**
	 * Method to return the function of this ArcProto.
	 * The Function is a technology-independent description of the behavior of this ArcProto.
	 * @return function the function of this ArcProto.
	 */
	public ArcProto.Function getFunction() { return function; }

	/**
	 * Method to set the factory-default angle of this ArcProto.
	 * This is only called from ArcProto during construction.
	 * @param angle the factory-default angle of this ArcProto.
	 */
	public void setFactoryAngleIncrement(int angle)
	{
		Pref pref = Pref.makeIntPref("DefaultAngleFor" + protoName + "IN" + tech.getTechName(), User.getUserTool().prefs, angle);
		defaultAnglePrefs.put(this, pref);
	}

	/**
	 * Method to set the angle increment on this ArcProto.
	 * The angle increment is the granularity on placement angle for instances
	 * of this ArcProto.  It is in degrees.
	 * For example, a value of 90 requests that instances run at 0, 90, 180, or 270 degrees.
	 * A value of 0 allows arcs to be created at any angle.
	 * @param angle the angle increment on this ArcProto.
	 */
	public void setAngleIncrement(int angle)
	{
		Pref pref = defaultAnglePrefs.get(this);
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
		Pref pref = defaultAnglePrefs.get(this);
		if (pref == null) return 90;
		return pref.getInt();
	}

	HashMap<ArcProto,Pref> arcPinPrefs = new HashMap<ArcProto,Pref>();

	private Pref getArcPinPref()
	{
		Pref pref = arcPinPrefs.get(this);
		if (pref == null)
		{
			pref = Pref.makeStringPref("PinFor" + protoName + "IN" + tech.getTechName(), Technology.getTechnologyPreferences(), "");
			arcPinPrefs.put(this, pref);
		}
		return pref;
	}

	/**
	 * Method to set the default pin node to use for this ArcProto.
	 * The pin node is used for making bends in wires.
	 * It must have just 1 port in the center, and be able to connect
	 * to this type of arc.
	 * @param np the default pin node to use for this ArcProto.
	 */
	public void setPinProto(PrimitiveNode np)
	{
		Pref pref = getArcPinPref();
		pref.setString(np.getName());
	}

	/**
	 * Method to find the PrimitiveNode pin corresponding to this ArcProto type.
	 * Users can override the pin to use, and this method returns the user setting.
	 * For example, if this ArcProto is metal-1 then return the Metal-1-pin,
	 * but the user could set it to Metal-1-Metal-2-Contact.
	 * @return the PrimitiveNode pin to use for arc bends.
	 */
	public PrimitiveNode findOverridablePinProto()
	{
		// see if there is a default on this arc proto
		Pref pref = getArcPinPref();
		String primName = pref.getString();
		if (primName != null && primName.length() > 0)
		{
			PrimitiveNode np = tech.findNodeProto(primName);
			if (np != null) return np;
		}
		return findPinProto();
	}

	/**
	 * Method to find the PrimitiveNode pin corresponding to this ArcProto type.
	 * For example, if this ArcProto is metal-1 then return the Metal-1-pin.
	 * @return the PrimitiveNode pin to use for arc bends.
	 */
	public PrimitiveNode findPinProto()
	{
		// search for an appropriate pin
		Iterator<PrimitiveNode> it = tech.getNodes();
		while (it.hasNext())
		{
			PrimitiveNode pn = it.next();
			if (pn.isPin())
			{
				PrimitivePort pp = (PrimitivePort) pn.getPorts().next();
				if (pp.connectsTo(this)) return pn;
			}
		}
		return null;
	}

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

		ArcProto ap = tech.findArcProto(withoutPrefix);
		if (ap != null) return ap;
		return null;
	}

 	/**
	 * Method to return the number of layers that comprise this ArcProto.
	 * @return the number of layers that comprise this ArcProto.
	 */
    public int getNumArcLayers() { return layers.length; }
    
	/**
	 * Method to return layer that comprises by its index in all layers
     * @param arcLayerIndex layer index
	 * @return specified layer that comprises this ArcProto.
	 */
    public Layer getLayer(int arcLayerIndex) { return layers[arcLayerIndex].getLayer(); }

    /**
     * Returns the extend of specified layer that comprise this ArcProto over base arc width in lambda units.
     * @param arcLayerIndex layer index
     * @return the extend of specified layer that comprise this ArcProto over base arc width in lambda units.
     */
    public double getLayerLambdaExtend(int arcLayerIndex) { return DBMath.gridToLambda(getLayerGridExtend(arcLayerIndex)); }
    
    /**
     * Returns the extend of specified layer that comprise this ArcProto over base arc width in grid units.
     * @param arcLayerIndex layer index
     * @return the extend of specified layer that comprise this ArcProto over base arc width in grid units.
     */
    public long getLayerGridExtend(int arcLayerIndex) { return (gridWidthOffset - layers[arcLayerIndex].getGridOffset()) >> 1; }
    
    /**
     * Returns the Poly.Style of specified layer that comprise this ArcLayer.
     * @param arcLayerIndex layer index
     * @return the Poly.Style of specified layer that comprise this ArcLayer.
     */
    public Poly.Type getLayerStyle(int arcLayerIndex) { return layers[arcLayerIndex].getStyle(); }
    
    /**
     * Returns the extend of specified layer that comprise this ArcProto over base arc width in lambda units.
     * @param layer specified Layer
     * @return the extend of specified layer that comprise this ArcProto over base arc width in lambda units.
     * @throws IndexOutOfBoundsException when specified layer diesn't comprise this ArcProto
     */
    public double getLayerLambdaExtend(Layer layer) { return getLayerLambdaExtend(indexOf(layer)); }
    
    /**
     * Returns the extend of specified layer that comprise this ArcProto over base arc width in grid units.
     * @param layer specified Layer
     * @return the extend of specified layer that comprise this ArcProto over base arc width in grid units.
     * @throws IndexOutOfBoundsException when specified layer diesn't comprise this ArcProto
     */
    public long getLayerGridExtend(Layer layer) { return getLayerGridExtend(indexOf(layer)); }
    
    /**
     * Returns the Poly.Style of specified layer that comprise this ArcLayer.
     * @param layer specified Layer
     * @return the Poly.Style of specified layer that comprise this ArcLayer.
     * @throws IndexOutOfBoundsException when specified layer diesn't comprise this ArcProto
     */
    public Poly.Type getLayerStyle(Layer layer) { return getLayerStyle(indexOf(layer)); }
    
	/**
	 * Method to return specified layer that comprise this ArcProto.
     * @param i layer index
	 * @return specified layer that comprise this ArcProto.
	 */
    Technology.ArcLayer getArcLayer(int i) { return layers[i]; }

//	/**
//	 * Method to return the array of layers that comprise this ArcProto.
//	 * @return the array of layers that comprise this ArcProto.
//	 */
//	public Iterator<Technology.ArcLayer> getArcLayers() { return ArrayIterator.iterator(layers); }

	/**
	 * Method to return an iterator over the layers in this ArcProto.
	 * @return an iterator over the layers in this ArcProto.
	 */
	public Iterator<Layer> getLayerIterator()
	{
		return new LayerIterator(layers);
	}

	/** 
	 * Iterator for Layers on this ArcProto
	 */ 
	private static class LayerIterator implements Iterator<Layer> 
	{ 
		Technology.ArcLayer [] array; 
		int pos; 

		public LayerIterator(Technology.ArcLayer [] a) 
		{ 
			array = a; 
			pos = 0; 
		} 

		public boolean hasNext() 
		{ 
			return pos < array.length; 
		} 

		public Layer next() throws NoSuchElementException 
		{ 
			if (pos >= array.length) 
				throw new NoSuchElementException(); 
			return array[pos++].getLayer(); 
		} 

		public void remove() throws UnsupportedOperationException, IllegalStateException 
		{ 
			throw new UnsupportedOperationException(); 
		}
	}

//	/**
//	 * Method to find the ArcLayer on this ArcProto with a given Layer.
//	 * If there are more than 1 with the given Layer, the first is returned.
//	 * @param layer the Layer to find.
//	 * @return the ArcLayer that has this Layer.
//	 */
//	public Technology.ArcLayer findArcLayer(Layer layer)
//	{
//		for(int j=0; j<layers.length; j++)
//		{
//			Technology.ArcLayer oneLayer = layers[j];
//			if (oneLayer.getLayer() == layer) return oneLayer;
//		}
//		return null;
//	}
    
	/**
	 * Method to find an index of Layer in a list of Layers that comprise this ArcProto.
	 * If this layer is not in the list, return -1
	 * @param layer the Layer to find.
	 * @return an index of Layer in a list of Layers that comprise this ArcProto, or -1.
	 */
    public int indexOf(Layer layer) {
        for (int arcLayerIndex = 0; arcLayerIndex < layers.length; arcLayerIndex++) {
            if (layers[arcLayerIndex].getLayer() == layer)
                return arcLayerIndex;
        }
        return -1;
    }
    
	/**
	 * Method to set the surround distance of layer "outerlayer" from layer "innerlayer"
	 * in arc "aty" to "surround".
	 */
	protected void setArcLayerSurroundLayer(Layer outerLayer, Layer innerLayer,
	                                        double surround)
	{
		// find the inner layer
		int inLayerIndex = indexOf(innerLayer);
		if (inLayerIndex < 0)
		{
		    System.out.println("Internal error in " + tech.getTechDesc() + " surround computation. Arc layer '" +
                    innerLayer.getName() + "' is not valid in '" + getName() + "'");
			return;
		}

		// find the outer layer
        int i = indexOf(outerLayer);
        
		if (i < 0)
		{
            System.out.println("Internal error in " + tech.getTechDesc() + " surround computation. Arc layer '" +
                    outerLayer.getName() + "' is not valid in '" + getName() + "'");
			return;
		}

		// compute the indentation of the outer layer
		long indent = layers[inLayerIndex].getGridOffset() - DBMath.lambdaToGrid(surround)*2;
        layers[i] = layers[i].withGridOffset(indent);
        computeMaxLayerGridOffset();
	}
    
    void computeMaxLayerGridOffset() {
        long max = Long.MIN_VALUE;
        for (int i = 0; i < layers.length; i++) {
            Technology.ArcLayer primLayer = layers[i];
            assert indexOf(primLayer.getLayer()) == i; // layers are unique
            max = Math.max(max, primLayer.getGridOffset());
        }
        assert 0 <= max && max < Integer.MAX_VALUE;
        maxLayerGridOffset = (int)max;
    }

    /**
	 * Method to get MinZ and MaxZ of this ArcProto
	 * @param array array[0] is minZ and array[1] is max
	 */
	public void getZValues(double [] array)
	{
		for(int j=0; j<layers.length; j++)
		{
			Layer layer = layers[j].getLayer();

			double distance = layer.getDistance();
			double thickness = layer.getThickness();
			double z = distance + thickness;

			array[0] = (array[0] > distance) ? distance : array[0];
			array[1] = (array[1] < z) ? z : array[1];
		}
	}

	/**
	 * Returns the polygons that describe dummy arc of this ArcProto
     * with default width and specified length.
	 * @param lambdaLength length of dummy arc in lambda units.
	 * @return an array of Poly objects that describes dummy arc graphically.
	 */
    public Poly[] getShapeOfDummyArc(double lambdaLength) {
        long l2 = DBMath.lambdaToGrid(lambdaLength/2);
        // see how many polygons describe this arc
        Poly [] polys = new Poly[layers.length];
//        Point2D.Double headLocation = new Point2D.Double(lambdaLength/2, 0);
//        Point2D.Double tailLocation = new Point2D.Double(-lambdaLength/2, 0);
        for (int i = 0; i < layers.length; i++) {
            Technology.ArcLayer primLayer = layers[i];
            long gridWidth = getDefaultGridFullWidth() - primLayer.getGridOffset();
            Poly.Type style = primLayer.getStyle();
            Point2D.Double[] points;
            if (gridWidth == 0) {
                points = new Point2D.Double[]{ new Point2D.Double(-l2, 0), new Point2D.Double(l2, 0)};
                if (style == Poly.Type.FILLED) style = Poly.Type.OPENED;
            } else {
                long w2 = gridWidth/2;
                assert w2 > 0;
                points = new Point2D.Double[] {
                    new Point2D.Double(-l2-w2, w2), new Point2D.Double(-l2-w2, -w2), new Point2D.Double(l2+w2, -w2), new Point2D.Double(l2+w2, w2) };
                if (style.isOpened())
                    points = new Point2D.Double[] { points[0], points[1], points[2], points[3], (Point2D.Double)points[0].clone() };
            }
            Poly poly = new Poly(points);
            poly.gridToLambda();
            poly.setStyle(style);
            poly.setLayer(primLayer.getLayer());
            polys[i] = poly;
        }
        return polys;
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
        Technology tech = getTechnology();
        if (Technology.getCurrent() != tech)
            description += tech.getTechName() + ":";
        description += protoName;
        return description;
	}

    /**
     * Compares ArcProtos by their Technologies and definition order.
     * @param that the other ArcProto.
     * @return a comparison between the ArcProto.
     */
	public int compareTo(ArcProto that)
	{
		if (this.tech != that.tech)
		{
			int cmp = this.tech.compareTo(that.tech);
			if (cmp != 0) return cmp;
		}
		return this.primArcIndex - that.primArcIndex;
	}

	/**
	 * Returns a printable version of this ArcProto.
	 * @return a printable version of this ArcProto.
	 */
	public String toString()
	{
		return "arc " + describe();
	}

    void dump(PrintWriter out) {
        out.println("ArcProto " + getName() + " " + getFunction());
        out.println("\tisWipable=" + isWipable());
        out.println("\tisCurvable=" + isCurvable());
        out.println("\tisSpecialArc=" + isSpecialArc());
        out.println("\tisEdgeSelect=" + isEdgeSelect());
        out.println("\tisNotUsed=" + isNotUsed());
        out.println("\tisSkipSizeInPalette=" + isSkipSizeInPalette());
        
        out.println("\twidthOffset=" + getLambdaWidthOffset());
        Technology.printlnPref(out, 1, defaultWidthPrefs.get(this));
        Technology.printlnPref(out, 1, defaultAnglePrefs.get(this));
        Technology.printlnPref(out, 1, defaultRigidPrefs.get(this));
        Technology.printlnPref(out, 1, defaultFixedAnglePrefs.get(this));
        Technology.printlnPref(out, 1, defaultExtendedPrefs.get(this));
        Technology.printlnPref(out, 1, defaultDirectionalPrefs.get(this));
        
        for (int arcLayerIndex = 0; arcLayerIndex < getNumArcLayers(); arcLayerIndex++) {
            out.println("\t\tarcLayer layer=" + getLayer(arcLayerIndex).getName() +
                    " style=" + getLayerStyle(arcLayerIndex).name() +
                    " extend=" + getLayerLambdaExtend(arcLayerIndex));
        }
    }
    
    /**
     * Method to check invariants in this ArcProto.
     * @exception AssertionError if invariants are not valid
     */
    void check() {
        long defaultWidth = getDefaultGridFullWidth();
        for (Technology.ArcLayer primLayer: layers) {
            long gridOffset = primLayer.getGridOffset();
            assert 0 <= gridOffset && gridOffset <= maxLayerGridOffset;
            long width = defaultWidth - gridOffset;
            if (width < 0)
                System.out.println(primLayer + " of " + this + " has invalid width");
        }
        assert 0 <= gridWidthOffset && gridWidthOffset <= maxLayerGridOffset;
    }
}
