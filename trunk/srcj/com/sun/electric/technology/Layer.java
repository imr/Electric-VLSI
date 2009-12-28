/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Layer.java
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
package com.sun.electric.technology;

import com.sun.electric.database.EObjectInputStream;
import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.id.LayerId;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.Setting;
import com.sun.electric.tool.user.UserInterfaceMain;

import java.awt.Color;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The Layer class defines a single layer of material, out of which NodeInst and ArcInst objects are created.
 * The Layers are defined by the PrimitiveNode and ArcProto classes, and are used in the generation of geometry.
 * In addition, layers have extra information that is used for output and behavior.
 */
public class Layer implements Serializable, Comparable
{
    public static final double DEFAULT_THICKNESS = 0; // 3D default thickness
    public static final double DEFAULT_DISTANCE = 0; // 3D default distance

    /** Describes a P-type layer. */												private static final int PTYPE =          0100;
    /** Describes a N-type layer. */												private static final int NTYPE =          0200;
    /** Describes a depletion layer. */												private static final int DEPLETION =      0400;
    /** Describes a enhancement layer. */											private static final int ENHANCEMENT =   01000;
    /** Describes a light doped layer. */											private static final int LIGHT =         02000;
    /** Describes a heavy doped layer. */											private static final int HEAVY =         04000;
//    /** Describes a pseudo layer. */												private static final int PSEUDO =       010000;
    /** Describes a deep layer. */												    private static final int DEEP =         010000;
    /** Describes a nonelectrical layer (does not carry signals). */				private static final int NONELEC =      020000;
    /** Describes a layer that contacts metal (used to identify contacts/vias). */	private static final int CONMETAL =     040000;
    /** Describes a layer that contacts polysilicon (used to identify contacts). */	private static final int CONPOLY =     0100000;
    /** Describes a layer that contacts diffusion (used to identify contacts). */	private static final int CONDIFF =     0200000;
    /** Describes a layer that is native. */	                                    private static final int NATIVE =      0400000;
    /** Describes a layer that is VTH or VTL */								        private static final int HLVT =      010000000;
    /** Describes a layer that is inside transistor. */								private static final int INTRANS =   020000000;
    /** Describes a thick layer. */								                    private static final int THICK =     040000000;
    /** Describes a carbon-nanotube Active layer. */								private static final int CARBNANO = 0100000000;

    private static final LayerNumbers metalLayers = new LayerNumbers();
    private static final LayerNumbers contactLayers = new LayerNumbers();
    private static final LayerNumbers polyLayers = new LayerNumbers();
    private static List<Function> allFunctions;

    private static class LayerNumbers
    {
    	private final ArrayList<Function> list;
    	private int base;

    	LayerNumbers()
    	{
    		list = new ArrayList<Function>();
    		base = 0;
    	}

    	public void addLayer(Function fun, int level)
    	{
    		while (level < base)
    		{
    			base--;
    			list.add(0, null);
    		}
            while (list.size() <= level-base) list.add(null);
            Function oldFunction = list.set(level-base, fun);
            assert oldFunction == null;
    	}

    	public Function get(int level)
    	{
    		return list.get(level-base);
    	}
    }

    public int compareTo(Object other)
    {
		String s = toString();
		String sOther = other.toString();
		return s.compareToIgnoreCase(sOther);
    }

    /**
	 * Function is a typesafe enum class that describes the function of a layer.
	 * Functions are technology-independent and describe the nature of the layer (Metal, Polysilicon, etc.)
	 */
	public static enum Function
	{
		/** Describes an unknown layer. */						UNKNOWN   ("unknown",        0, 0, 0, 35, 0, 0),
		/** Describes a local interconnect metal layer 2. */	METALNEG2 ("metal-2-local", -2, 0, 0, 13, 0, 0),
		/** Describes a local interconnect metal layer 1. */	METALNEG1 ("metal-1-local", -1, 0, 0, 15, 0, 0),
		/** Describes a metal layer 1. */						METAL1    ("metal-1",        1, 0, 0, 17, 0, 0),
		/** Describes a metal layer 2. */						METAL2    ("metal-2",        2, 0, 0, 19, 0, 0),
		/** Describes a metal layer 3. */						METAL3    ("metal-3",        3, 0, 0, 21, 0, 0),
		/** Describes a metal layer 4. */						METAL4    ("metal-4",        4, 0, 0, 23, 0, 0),
		/** Describes a metal layer 5. */						METAL5    ("metal-5",        5, 0, 0, 25, 0, 0),
		/** Describes a metal layer 6. */						METAL6    ("metal-6",        6, 0, 0, 27, 0, 0),
		/** Describes a metal layer 7. */						METAL7    ("metal-7",        7, 0, 0, 29, 0, 0),
		/** Describes a metal layer 8. */						METAL8    ("metal-8",        8, 0, 0, 31, 0, 0),
		/** Describes a metal layer 9. */						METAL9    ("metal-9",        9, 0, 0, 33, 0, 0),
		/** Describes a metal layer 10. */						METAL10   ("metal-10",      10, 0, 0, 35, 0, 0),
		/** Describes a metal layer 11. */						METAL11   ("metal-11",      11, 0, 0, 37, 0, 0),
		/** Describes a metal layer 12. */						METAL12   ("metal-12",      12, 0, 0, 39, 0, 0),
		/** Describes a polysilicon layer 1. */					POLY1     ("poly-1",         0, 0, 1, 12, 0, 0),
		/** Describes a polysilicon layer 2. */					POLY2     ("poly-2",         0, 0, 2, 13, 0, 0),
		/** Describes a polysilicon layer 3. */					POLY3     ("poly-3",         0, 0, 3, 14, 0, 0),
		/** Describes a polysilicon gate layer. */				GATE      ("gate",           0, 0, 0, 15, INTRANS, 0),
		/** Describes a diffusion layer. */						DIFF      ("diffusion",      0, 0, 0, 11, 0, 0),
		/** Describes a P-diffusion layer. */					DIFFP     ("p-diffusion",    0, 0, 0, 11, PTYPE, 0),
		/** Describes a N-diffusion layer. */					DIFFN     ("n-diffusion",    0, 0, 0, 11, NTYPE, 0),
		/** Describes a N-diffusion carbon nanotube layer. */	DIFFNCN   ("n-diffusion-cn", 0, 0, 0, 11, NTYPE|CARBNANO, 0),
		/** Describes a P-diffusion carbon nanotube layer. */	DIFFPCN   ("n-diffusion-cn", 0, 0, 0, 11, NTYPE|CARBNANO, 0),
		/** Describes an implant layer. */						IMPLANT   ("implant",        0, 0, 0, 2, 0, 0),
		/** Describes a P-implant layer. */						IMPLANTP  ("p-implant",      0, 0, 0, 2, PTYPE, 0),
		/** Describes an N-implant layer. */					IMPLANTN  ("n-implant",      0, 0, 0, 2, NTYPE, 0),
		/** Describes a contact layer 1. */						CONTACT1  ("contact-1",      0, 1, 0, 16, 0, 0),
		/** Describes a contact layer 2. */						CONTACT2  ("contact-2",      0, 2, 0, 18, 0, 0),
		/** Describes a contact layer 3. */						CONTACT3  ("contact-3",      0, 3, 0, 20, 0, 0),
		/** Describes a contact layer 4. */						CONTACT4  ("contact-4",      0, 4, 0, 22, 0, 0),
		/** Describes a contact layer 5. */						CONTACT5  ("contact-5",      0, 5, 0, 24, 0, 0),
		/** Describes a contact layer 6. */						CONTACT6  ("contact-6",      0, 6, 0, 26, 0, 0),
		/** Describes a contact layer 7. */						CONTACT7  ("contact-7",      0, 7, 0, 28, 0, 0),
		/** Describes a contact layer 8. */						CONTACT8  ("contact-8",      0, 8, 0, 30, 0, 0),
		/** Describes a contact layer 9. */						CONTACT9  ("contact-9",      0, 9, 0, 32, 0, 0),
		/** Describes a contact layer 10. */					CONTACT10 ("contact-10",     0,10, 0, 34, 0, 0),
		/** Describes a contact layer 11. */					CONTACT11 ("contact-11",     0,11, 0, 36, 0, 0),
		/** Describes a contact layer 12. */					CONTACT12 ("contact-12",     0,12, 0, 38, 0, 0),
		/** Describes a sinker (diffusion-to-buried plug). */	PLUG      ("plug",           0, 0, 0, 40, 0, 0),
		/** Describes an overglass layer (passivation). */		OVERGLASS ("overglass",      0, 0, 0, 41, 0, 0),
		/** Describes a resistor layer. */						RESISTOR  ("resistor",       0, 0, 0, 4, 0, 0),
		/** Describes a capacitor layer. */						CAP       ("capacitor",      0, 0, 0, 5, 0, 0),
		/** Describes a transistor layer. */					TRANSISTOR("transistor",     0, 0, 0, 3, 0, 0),
		/** Describes an emitter of bipolar transistor. */		EMITTER   ("emitter",        0, 0, 0, 6, 0, 0),
		/** Describes a base of bipolar transistor. */			BASE      ("base",           0, 0, 0, 7, 0, 0),
		/** Describes a collector of bipolar transistor. */		COLLECTOR ("collector",      0, 0, 0, 8, 0, 0),
		/** Describes a substrate layer. */						SUBSTRATE ("substrate",      0, 0, 0, 1, 0, 0),
		/** Describes a well layer. */							WELL      ("well",           0, 0, 0, 0, 0, 0),
		/** Describes a P-well layer. */						WELLP     ("p-well",         0, 0, 0, 0, PTYPE, 0),
		/** Describes a N-well layer. */						WELLN     ("n-well",         0, 0, 0, 0, NTYPE, 0),
		/** Describes a guard layer. */							GUARD     ("guard",          0, 0, 0, 9, 0, 0),
		/** Describes an isolation layer (bipolar). */			ISOLATION ("isolation",      0, 0, 0, 10, 0, 0),
		/** Describes a bus layer. */							BUS       ("bus",            0, 0, 0, 42, 0, 0),
		/** Describes an artwork layer. */						ART       ("art",            0, 0, 0, 43, 0, 0),
		/** Describes a control layer. */						CONTROL   ("control",        0, 0, 0, 44, 0, 0),
        /** Describes a tileNot layer. */						TILENOT   ("tileNot",        0, 0, 0, 45, 0, 0),
        /** Describes a dummy polysilicon layer 1 */            DMYPOLY1  ("dmy-poly-1",     0, 0, 0, POLY1.getHeight(), 0, 0),
        /** Describes a dummy polysilicon layer 2 */            DMYPOLY2  ("dmy-poly-2",     0, 0, 0, POLY2.getHeight(), 0, 0),
        /** Describes a dummy polysilicon layer 3 */            DMYPOLY3  ("dmy-poly-3",     0, 0, 0, POLY3.getHeight(), 0, 0),
        /** Describes a dummy diffusion layer */                DMYDIFF   ("dmy-diffusion",  0, 0, 0, DIFF.getHeight(), 0, 0),
        /** Describes a dummy metal layer 1 */                  DMYMETAL1 ("dmy-metal-1",    0, 0, 0, METAL1.getHeight(), 0, 1),
        /** Describes a dummy metal layer 2 */                  DMYMETAL2 ("dmy-metal-2",    0, 0, 0, METAL2.getHeight(), 0, 2),
        /** Describes a dummy metal layer 3 */                  DMYMETAL3 ("dmy-metal-3",    0, 0, 0, METAL3.getHeight(), 0, 3),
        /** Describes a dummy metal layer 4 */                  DMYMETAL4 ("dmy-metal-4",    0, 0, 0, METAL4.getHeight(), 0, 4),
        /** Describes a dummy metal layer 5 */                  DMYMETAL5 ("dmy-metal-5",    0, 0, 0, METAL5.getHeight(), 0, 5),
        /** Describes a dummy metal layer 6 */                  DMYMETAL6 ("dmy-metal-6",    0, 0, 0, METAL6.getHeight(), 0, 6),
        /** Describes a dummy metal layer 7 */                  DMYMETAL7 ("dmy-metal-7",    0, 0, 0, METAL7.getHeight(), 0, 7),
        /** Describes a dummy metal layer 8 */                  DMYMETAL8 ("dmy-metal-8",    0, 0, 0, METAL8.getHeight(), 0, 8),
        /** Describes a dummy metal layer 9 */                  DMYMETAL9 ("dmy-metal-9",    0, 0, 0, METAL9.getHeight(), 0, 9),
        /** Describes a dummy metal layer 10 */                 DMYMETAL10("dmy-metal-10",   0, 0, 0, METAL10.getHeight(), 0, 10),
        /** Describes a dummy metal layer 11 */                 DMYMETAL11("dmy-metal-11",   0, 0, 0, METAL11.getHeight(), 0, 11),
        /** Describes a dummy metal layer 12 */                 DMYMETAL12("dmy-metal-12",   0, 0, 0, METAL12.getHeight(), 0, 12),
        /** Describes a exclusion polysilicon layer 1 */        DEXCLPOLY1("dexcl-poly-1",   0, 0, 0, POLY1.getHeight(), 0, 0),
        /** Describes a exclusion polysilicon layer 2 */        DEXCLPOLY2("dexcl-poly-2",   0, 0, 0, POLY2.getHeight(), 0, 0),
        /** Describes a exclusion polysilicon layer 3 */        DEXCLPOLY3("dexcl-poly-3",   0, 0, 0, POLY3.getHeight(), 0, 0),
        /** Describes a exclusion diffusion layer */            DEXCLDIFF("dexcl-diffusion", 0, 0, 0, DIFF.getHeight(), 0, 0),
        /** Describes a exclusion metal layer 1 */              DEXCLMETAL1("dexcl-metal-1", 0, 0, 0, METAL1.getHeight(), 0, 1),
        /** Describes a exclusion metal layer 2 */              DEXCLMETAL2("dexcl-metal-2", 0, 0, 0, METAL2.getHeight(), 0, 2),
        /** Describes a exclusion metal layer 3 */              DEXCLMETAL3("dexcl-metal-3", 0, 0, 0, METAL3.getHeight(), 0, 3),
        /** Describes a exclusion metal layer 4 */              DEXCLMETAL4("dexcl-metal-4", 0, 0, 0, METAL4.getHeight(), 0, 4),
        /** Describes a exclusion metal layer 5 */              DEXCLMETAL5("dexcl-metal-5", 0, 0, 0, METAL5.getHeight(), 0, 5),
        /** Describes a exclusion metal layer 6 */              DEXCLMETAL6("dexcl-metal-6", 0, 0, 0, METAL6.getHeight(), 0, 6),
        /** Describes a exclusion metal layer 7 */              DEXCLMETAL7("dexcl-metal-7", 0, 0, 0, METAL7.getHeight(), 0, 7),
        /** Describes a exclusion metal layer 8 */              DEXCLMETAL8("dexcl-metal-8", 0, 0, 0, METAL8.getHeight(), 0, 8),
        /** Describes a exclusion metal layer 9 */              DEXCLMETAL9("dexcl-metal-9", 0, 0, 0, METAL9.getHeight(), 0, 9),
        /** Describes a exclusion metal layer 10 */             DEXCLMETAL10("dexcl-metal-10", 0, 0, 0, METAL10.getHeight(), 0, 10),
        /** Describes a exclusion metal layer 11 */             DEXCLMETAL11("dexcl-metal-11", 0, 0, 0, METAL11.getHeight(), 0, 11),
        /** Describes a exclusion metal layer 12 */             DEXCLMETAL12("dexcl-metal-12", 0, 0, 0, METAL12.getHeight(), 0, 12);

//        /** Describes a P-type layer. */												public static final int PTYPE = Layer.PTYPE;
//        /** Describes a N-type layer. */												public static final int NTYPE = Layer.NTYPE;
        /** Describes a depletion layer. */												public static final int DEPLETION = Layer.DEPLETION;
        /** Describes a enhancement layer. */											public static final int ENHANCEMENT = Layer.ENHANCEMENT;
        /** Describes a light doped layer. */											public static final int LIGHT = Layer.LIGHT;
        /** Describes a heavy doped layer. */											public static final int HEAVY = Layer.HEAVY;
//        /** Describes a pseudo layer. */												public static final int PSEUDO = Layer.PSEUDO;
        /** Describes a nonelectrical layer (does not carry signals). */				public static final int NONELEC = Layer.NONELEC;
        /** Describes a layer that contacts metal (used to identify contacts/vias). */	public static final int CONMETAL = Layer.CONMETAL;
        /** Describes a layer that contacts polysilicon (used to identify contacts). */	public static final int CONPOLY = Layer.CONPOLY;
        /** Describes a layer that contacts diffusion (used to identify contacts). */	public static final int CONDIFF = Layer.CONDIFF;
        /** Describes a layer that is VTH or VTL */								        public static final int HLVT = Layer.HLVT;
//        /** Describes a layer that is inside transistor. */								public static final int INTRANS = Layer.INTRANS;
        /** Describes a thick layer. */								                    public static final int THICK = Layer.THICK;
        /** Describes a native layer. */								                public static final int NATIVE = Layer.NATIVE;
        /** Describes a deep layer. */								                    public static final int DEEP = Layer.DEEP;
//        /** Describes a carbon-nanotube Active layer. */								public static final int CARBNANO = Layer.CARBNANO;


        private final String name;
        private final boolean isMetal;
        private final boolean isContact;
        private final boolean isPoly;
		private final int level;
		private final int height;
		private final int extraBits;
		private static final int [] extras = {PTYPE, NTYPE, DEPLETION, ENHANCEMENT, LIGHT, HEAVY, /*PSEUDO,*/ NONELEC, CONMETAL, CONPOLY, CONDIFF, HLVT, INTRANS, THICK, CARBNANO};

        static {
            allFunctions = Arrays.asList(Function.class.getEnumConstants());
        }

		private Function(String name, int metalLevel, int contactLevel, int polyLevel, int height, int extraBits, int genericLevel)
		{
			this.name = name;
			this.height = height;
			this.extraBits = extraBits;
            isMetal = metalLevel != 0;
            isContact = contactLevel != 0;
            isPoly = polyLevel != 0;
            int level = 0;
            if (genericLevel != 0) level = genericLevel;
            if (isMetal) metalLayers.addLayer(this, level = metalLevel);
			if (isContact) contactLayers.addLayer(this, level = contactLevel);
			if (isPoly) polyLayers.addLayer(this, level = polyLevel);
            this.level = level;
		}

//        private void addToLayers(ArrayList<Function> layers, int level) {
//            this.level = level;
//            while (layers.size() <= level) layers.add(null);
//            Function oldFunction = layers.set(level, this);
//            assert oldFunction == null;
//        }

		/**
		 * Returns a printable version of this Function.
		 * @return a printable version of this Function.
		 */
		public String toString()
		{
			String toStr = name;
			for(int i=0; i<extras.length; i++)
			{
				if ((extraBits & extras[i]) == 0) continue;
				toStr += "," + getExtraName(extras[i]);
			}
			return toStr;
		}

		/**
		 * Returns the name for this Function.
		 * @return the name for this Function.
		 */
		public String getName() { return name; }

		/**
		 * Returns the constant name for this Function.
		 * Constant names are used when writing Java code, so they must be the same as the actual symbol name.
		 * @return the constant name for this Function.
		 */
		public String getConstantName() { return name(); }

		/**
		 * Method to return a list of all Layer Functions.
		 * @return a list of all Layer Functions.
		 */
		public static List<Function> getFunctions() { return allFunctions; }

		/**
		 * Method to return an array of the Layer Function "extra bits".
		 * @return an array of the Layer Function "extra bits".
		 * Each entry in the array is a single "extra bit", but they can be ORed together to combine them.
		 */
		public static int [] getFunctionExtras() { return extras; }

		/**
		 * Method to convert an "extra bits" value to a name.
		 * @param extra the extra bits value (must be a single bit, not an ORed combination).
		 * @return the name of that extra bit.
		 */
		public static String getExtraName(int extra)
		{
			if (extra == PTYPE) return "p-type";
			if (extra == NTYPE) return "n-type";
			if (extra == DEPLETION) return "depletion";
			if (extra == ENHANCEMENT) return "enhancement";
			if (extra == LIGHT) return "light";
			if (extra == HEAVY) return "heavy";
//			if (extra == PSEUDO) return "pseudo";
			if (extra == NONELEC) return "nonelectrical";
			if (extra == CONMETAL) return "connects-metal";
			if (extra == CONPOLY) return "connects-poly";
			if (extra == CONDIFF) return "connects-diff";
            if (extra == HLVT) return "vt";
			if (extra == INTRANS) return "inside-transistor";
			if (extra == THICK) return "thick";
            if (extra == NATIVE) return "native";
            if (extra == DEEP) return "deep";
            if (extra == CARBNANO) return "carb-nano";
            return "";
		}

		/**
		 * Method to convert an "extra bits" value to a constant name.
		 * Constant names are used when writing Java code, so they must be the same as the actual symbol name.
		 * @param extra the extra bits value (must be a single bit, not an ORed combination).
		 * @return the name of that extra bit's constant.
		 */
		public static String getExtraConstantName(int extra)
		{
			if (extra == PTYPE) return "PTYPE";
			if (extra == NTYPE) return "NTYPE";
			if (extra == DEPLETION) return "DEPLETION";
			if (extra == ENHANCEMENT) return "ENHANCEMENT";
			if (extra == LIGHT) return "LIGHT";
			if (extra == HEAVY) return "HEAVY";
//			if (extra == PSEUDO) return "PSEUDO";
			if (extra == NONELEC) return "NONELEC";
			if (extra == CONMETAL) return "CONMETAL";
			if (extra == CONPOLY) return "CONPOLY";
			if (extra == CONDIFF) return "CONDIFF";
            if (extra == HLVT) return "HLVT";
			if (extra == INTRANS) return "INTRANS";
			if (extra == THICK) return "THICK";
            if (extra == NATIVE) return "NATIVE";
            if (extra == DEEP) return "DEEP";
            if (extra == CARBNANO) return "CN";
            return "";
		}

		/**
		 * Method to convert an "extra bits" name to its numeric value.
		 * @param name the name of the bit.
		 * @return the numeric equivalent of that bit.
		 */
		public static int parseExtraName(String name)
		{
			if (name.equalsIgnoreCase("p-type")) return PTYPE;
			if (name.equalsIgnoreCase("n-type")) return NTYPE;
			if (name.equalsIgnoreCase("depletion")) return DEPLETION;
			if (name.equalsIgnoreCase("enhancement")) return ENHANCEMENT;
			if (name.equalsIgnoreCase("light")) return LIGHT;
			if (name.equalsIgnoreCase("heavy")) return HEAVY;
//			if (name.equalsIgnoreCase("pseudo")) return PSEUDO;
			if (name.equalsIgnoreCase("nonelectrical")) return NONELEC;
			if (name.equalsIgnoreCase("connects-metal")) return CONMETAL;
			if (name.equalsIgnoreCase("connects-poly")) return CONPOLY;
			if (name.equalsIgnoreCase("connects-diff")) return CONDIFF;
			if (name.equalsIgnoreCase("inside-transistor")) return INTRANS;
			if (name.equalsIgnoreCase("thick")) return THICK;
            if (name.equalsIgnoreCase("vt")) return HLVT;
            if (name.equalsIgnoreCase("native")) return NATIVE;
            if (name.equalsIgnoreCase("deep")) return DEEP;
            if (name.equalsIgnoreCase("carb-nano")) return CARBNANO;
            return 0;
		}

		/**
		 * Method to get the level of this Layer.
		 * The level applies to metal and polysilicon functions, and gives the layer number
		 * (i.e. Metal-2 is level 2).
		 * @return the level of this Layer.
		 */
		public int getLevel() { return level; }

		/**
		 * Method to find the Function that corresponds to Metal on a given layer.
		 * @param level the layer (starting at 1 for Metal-1).
		 * @return the Function that represents that Metal layer. Null if the given layer level is invalid.
		 */
		public static Function getMetal(int level)
		{
            if (level > EGraphics.TRANSPARENT_12)
            {
                System.out.println("Invalid metal layer level:" + level);
                return null;
            }
            Function func = metalLayers.get(level);
			return func;
		}

        /**
		 * Method to find the Function that corresponds to Dummy Metal on a given layer.
		 * @param level the layer (starting at 0 for Metal-1).
		 * @return the Function that represents that Metal layer. Null if the given layer level is invalid.
		 */
		public static Function getDummyMetal(int level)
		{
            if (level > EGraphics.TRANSPARENT_12)
            {
                System.out.println("Invalid metal layer level:" + level);
                return null;
            }

            switch (level)
            {
                case 0: return (Layer.Function.DMYMETAL1);
                case 1: return (Layer.Function.DMYMETAL2);
                case 2: return (Layer.Function.DMYMETAL3);
                case 3: return (Layer.Function.DMYMETAL4);
                case 4: return (Layer.Function.DMYMETAL5);
                case 5: return (Layer.Function.DMYMETAL6);
                case 6: return (Layer.Function.DMYMETAL7);
                case 7: return (Layer.Function.DMYMETAL8);
                case 8: return (Layer.Function.DMYMETAL9);
                case 9: return (Layer.Function.DMYMETAL10);
                case 10: return (Layer.Function.DMYMETAL11);
                case 11: return (Layer.Function.DMYMETAL12);
            }
            // Should never reach this point
            return null;
		}

        /**
		 * Method to find the Function that corresponds to Dummy Exclusion Metal on a given layer.
		 * @param l the layer (starting at 0 for Metal-1).
		 * @return the Function that represents that Metal layer. Null if the given layer level is invalid.
		 */
		public static Function getDummyExclMetal(int l)
		{
            if (l > EGraphics.TRANSPARENT_12)
            {
                System.out.println("Invalid metal layer level:" + l);
                return null;
            }

            switch (l)
            {
                case 0: return (Layer.Function.DEXCLMETAL1);
                case 1: return (Layer.Function.DEXCLMETAL2);
                case 2: return (Layer.Function.DEXCLMETAL3);
                case 3: return (Layer.Function.DEXCLMETAL4);
                case 4: return (Layer.Function.DEXCLMETAL5);
                case 5: return (Layer.Function.DEXCLMETAL6);
                case 6: return (Layer.Function.DEXCLMETAL7);
                case 7: return (Layer.Function.DEXCLMETAL8);
                case 8: return (Layer.Function.DEXCLMETAL9);
                case 9: return (Layer.Function.DEXCLMETAL10);
                case 10: return (Layer.Function.DEXCLMETAL11);
                case 11: return (Layer.Function.DEXCLMETAL12);
            }
            // Should never reach this point
            return null;
		}

        /**
		 * Method to find the Function that corresponds to a contact on a given layer.
		 * @param l the layer (starting at 1 for Contact-1).
		 * @return the Function that represents that Contact layer. Null if the given layer level is invalid.
		 */
		public static Function getContact(int l)
		{
            if (l > EGraphics.TRANSPARENT_12)
            {
                System.out.println("Invalid via layer level:" + l);
                return null;
            }
			Function func = contactLayers.get(l);
			return func;
		}

		/**
		 * Method to find the Function that corresponds to Polysilicon on a given layer.
		 * @param l the layer (starting at 1 for Polysilicon-1).
		 * @return the Function that represents that Polysilicon layer.
		 */
		public static Function getPoly(int l)
		{
			Function func = polyLayers.get(l);
			return func;
		}

		/**
		 * Method to tell whether this layer function is metal.
		 * @return true if this layer function is metal.
		 */
		public boolean isMetal() { return isMetal; }

		/**
		 * Method to tell whether this layer function is diffusion (active).
		 * @return true if this layer function is diffusion (active).
		 */
		public boolean isDiff()
		{
			if (this == DIFF || this == DIFFP || this == DIFFN || this == DIFFNCN || this == DIFFPCN) return true;
			return false;
		}

		/**
		 * Method to tell whether this layer function is polysilicon.
		 * @return true if this layer function is polysilicon.
		 */
		public boolean isPoly() { return isPoly || this == GATE; };

		/**
		 * Method to tell whether this layer function is polysilicon in the gate of a transistor.
		 * @return true if this layer function is the gate of a transistor.
		 */
		public boolean isGatePoly()
		{
			if (isPoly() && (extraBits&INTRANS) != 0) return true;
			return false;
		}

		/**
		 * Method to tell whether this layer function is a contact.
		 * @return true if this layer function is contact.
		 */
		public boolean isContact() { return isContact; }

        /**
		 * Method to tell whether this layer function is a well.
		 * @return true if this layer function is a well.
		 */
		public boolean isWell()
		{
			if (this == WELL || this == WELLP || this == WELLN)  return true;
			return false;
		}

		/**
		 * Method to tell whether this layer function is substrate.
		 * @return true if this layer function is substrate.
		 */
		public boolean isSubstrate()
		{
			if (this == SUBSTRATE ||
				this == WELL || this == WELLP || this == WELLN ||
				this == IMPLANT || this == IMPLANTN || this == IMPLANTP)  return true;
			return false;
		}

		/**
		 * Method to tell whether this layer function is implant.
		 * @return true if this layer function is implant.
		 */
		public boolean isImplant()
		{
			return (this == IMPLANT || this == IMPLANTN || this == IMPLANTP);
		}

        /**
         * Method to tell whether this layer function is a dummy
         * @return true if this layer function is a dummy
         */
        public boolean isDummy()
        {
            return (this == DMYDIFF || this == DMYPOLY1 || this == DMYPOLY2 || this == DMYPOLY3 ||
                    this == DMYMETAL1 || this == DMYMETAL2 || this == DMYMETAL3 || this == DMYMETAL4 ||
                    this == DMYMETAL5 || this == DMYMETAL6 || this == DMYMETAL7 || this == DMYMETAL8 ||
                    this == DMYMETAL9 || this == DMYMETAL10 || this == DMYMETAL11 || this == DMYMETAL12);
        }

        /**
         * Method to tell whether this layer function is a dummy exclusion
         * @return true if this layer function is a dummy exclusion
         */
        public boolean isDummyExclusion()
        {
            return (this == DEXCLDIFF || this == DEXCLPOLY1 || this == DEXCLPOLY2 || this == DEXCLPOLY3 ||
                    this == DEXCLMETAL1 || this == DEXCLMETAL2 || this == DEXCLMETAL3 || this == DEXCLMETAL4 ||
                    this == DEXCLMETAL5 || this == DEXCLMETAL6 || this == DEXCLMETAL7 || this == DEXCLMETAL8 ||
                    this == DEXCLMETAL9 || this == DEXCLMETAL10 || this == DEXCLMETAL11 || this == DEXCLMETAL12);
        }

        /**
         * Method to tell whether this layer function is in subset
         * of layer functions restricted by specified number
         * of metals and polysilicons.
         * @param numMetals number of metals in subset.
         * @param numPolys number of polysilicons in subset
         * @return true if this layer function is in subset.
         */
        public boolean isUsed(int numMetals, int numPolys) {
            if (isMetal || isContact || isDummyExclusion())
                return level <= numMetals;
            else if (isPoly)
                return level <= numPolys;
            else
                return true;
        }

		/**
		 * Method to tell the distance of this layer function.
		 * @return the distance of this layer function.
		 */
		public int getHeight() { return height; }

        /**
         * A set of Layer.Functions
         */
        public static class Set {
            final BitSet bits = new BitSet();
            final int extraBits; // -1 means no check extraBits
            /** Set if all Layer.Functions */
            public static final Set ALL = new Set(Function.class.getEnumConstants());

            /**
             * Constructs Function.Set from a Layer
             * @param l Layer
             */
            public Set(Layer l)
            {
                bits.set(l.getFunction().ordinal());
                extraBits = l.getFunctionExtras();
            }

            /**
             * Constructs Function.Set from varargs Functions.
             * @param funs variable list of Functions.
             */
            public Set(Function ... funs) {
                for (Function f: funs)
                    bits.set(f.ordinal());
                this.extraBits = NO_FUNCTION_EXTRAS; // same value as Layer.extraFunctions
            }

            /**
             * Constructs Function.Set from a collection of Functions.
             * @param funs a Collection of Functions.
             */
            public Set(Collection<Function> funs) {
                for (Function f: funs)
                    bits.set(f.ordinal());
                this.extraBits = NO_FUNCTION_EXTRAS; // same value as Layer.extraFunctions;
            }

            /**
             * Returns true if specified Functions is in this Set.
             * @param f Function to test.
             * @param extraFunction
             * @return true if specified Functions is in this Set.
             */
            public boolean contains(Function f, int extraFunction)
            {
                // Check first if there is a match in the extra bits
                boolean extraBitsM = extraBits == NO_FUNCTION_EXTRAS || (extraBits == extraFunction);
                return extraBitsM && bits.get(f.ordinal());
            }
        }
	}

    /***************************************************************************************************
     * Layer Comparators
     ***************************************************************************************************/

    /**
	 * A comparator object for sorting Layers by their level.
	 * Created once because it is used often.
	 */
    public static final LayerSortByFunctionLevel layerSortByFunctionLevel = new LayerSortByFunctionLevel();

	/**
	 * Comparator class for sorting Layers by their name.
	 */
	public static class LayerSortByFunctionLevel implements Comparator<Layer>
	{
		/**
		 * Method to compare two layers by their name.
		 * @param l1 one layer.
		 * @param l2 another layer.
		 * @return an integer indicating their sorting order.
		 */
		public int compare(Layer l1, Layer l2)
        {
            int level1 = l1.getFunction().getLevel();
            int level2 = l2.getFunction().getLevel();
            return level1 - level2;
        }

//        public static boolean areNeightborLayers(Layer l1, Layer l2)
//        {
//            int level1 = l1.getFunction().getLevel();
//            int level2 = l2.getFunction().getLevel();
//            return Math.abs(getNeighborLevel(l1, l2)) <=1;
//        }

        /**
         * Method to determine level of Layer2 with respect to Layer1.
         * Positive if Layer2 is above Layer1.
         * @param l1 the first Layer.
         * @param l2 the second Layer.
         * @return realtionship of layers.
         */
        public static int getNeighborLevel(Layer l1, Layer l2)
        {
            int level1 = l1.getFunction().getLevel();
            int level2 = l2.getFunction().getLevel();
            return level2 - level1;
        }
    }

    /**
	 * A comparator object for sorting Layers by their name.
	 * Created once because it is used often.
	 */
    public static final LayerSortByName layerSortByName = new LayerSortByName();

	/**
	 * Comparator class for sorting Layers by their name.
	 */
	private static class LayerSortByName implements Comparator<Layer>
	{
		/**
		 * Method to compare two layers by their name.
		 * @param l1 one layer.
		 * @param l2 another layer.
		 * @return an integer indicating their sorting order.
		 */
		public int compare(Layer l1, Layer l2)
        {
			String s1 = l1.getName();
			String s2 = l2.getName();
			return s1.compareToIgnoreCase(s2);
        }
	}

    /***************************************************************************************************
     * End of Layer Comparators
     ***************************************************************************************************/

    private final LayerId layerId;
	private int index = -1; // contains index in technology or -1 for standalone layers
	private final Technology tech;
    private EGraphics factoryGraphics;
	private Function function;
    private static final int NO_FUNCTION_EXTRAS = 0;
    private int functionExtras;
    private final boolean pseudo;
	private Setting cifLayerSetting;
	private Setting dxfLayerSetting;
//	private String gdsLayer;
	private Setting skillLayerSetting;
    private Setting resistanceSetting;
    private Setting capacitanceSetting;
    private Setting edgeCapacitanceSetting;
	private Setting layer3DThicknessSetting;
	private Setting layer3DDistanceSetting;

	/** the pseudo layer (if exists) */                                     private Layer pseudoLayer;
	/** the "real" layer (if this one is pseudo) */							private Layer nonPseudoLayer;
	/** true if dimmed (drawn darker) undimmed layers are highlighted */	private boolean dimmed;
	/** the pure-layer node that contains just this layer */				private PrimitiveNode pureLayerNode;

	private Layer(String name, boolean pseudo, Technology tech, EGraphics graphics)
	{
        layerId = tech.getId().newLayerId(name);
		this.tech = tech;
        if (graphics == null)
            throw new NullPointerException();
		this.factoryGraphics = graphics;
		this.nonPseudoLayer = this;
        this.pseudo = pseudo;

		this.dimmed = false;
		this.function = Function.UNKNOWN;
	}

    protected Object writeReplace() { return new LayerKey(this); }

    private static class LayerKey extends EObjectInputStream.Key<Layer> {
        public LayerKey() {}
        private LayerKey(Layer layer) { super(layer); }

        @Override
        public void writeExternal(EObjectOutputStream out, Layer layer) throws IOException {
            out.writeObject(layer.getTechnology());
            out.writeInt(layer.getId().chronIndex);
        }

        @Override
        public Layer readExternal(EObjectInputStream in) throws IOException, ClassNotFoundException {
            Technology tech = (Technology)in.readObject();
            int chronIndex = in.readInt();
            Layer layer = tech.getLayerByChronIndex(chronIndex);
            if (layer == null)
                throw new InvalidObjectException("arc proto not found");
            return layer;
        }
    }

	/**
	 * Method to create a new layer with the given name and graphics.
	 * @param tech the Technology that this layer belongs to.
	 * @param name the name of the layer.
	 * @param graphics the appearance of the layer.
	 * @return the Layer object.
	 */
	public static Layer newInstance(Technology tech, String name, EGraphics graphics)
	{
        if (tech == null) throw new NullPointerException();
        int transparent = graphics.getTransparentLayer();
        if (transparent != 0) {
            Color colorFromMap = tech.getFactoryTransparentLayerColors()[transparent - 1];
            if ((colorFromMap.getRGB() & 0xFFFFFF) != graphics.getRGB())
                throw new IllegalArgumentException();
        }
		Layer layer = new Layer(name, false, tech, graphics);
		tech.addLayer(layer);
		return layer;
	}

	/**
	 * Method to create a pseudo-layer for this Layer with a standard name "Pseudo-XXX".
	 * @return the pseudo-layer.
	 */
    public Layer makePseudo() {
        assert pseudoLayer == null;
        String pseudoLayerName = "Pseudo-" + getName();
        pseudoLayer = new Layer(pseudoLayerName, true, tech, factoryGraphics);
        pseudoLayer.setFunction(function, functionExtras);
        pseudoLayer.nonPseudoLayer = this;
        return pseudoLayer;
    }

	/**
	 * Method to return the Id of this Layer.
	 * @return the Id of this Layer.
	 */
	public LayerId getId() { return layerId; }

	/**
	 * Method to return the name of this Layer.
	 * @return the name of this Layer.
	 */
	public String getName() { return layerId.name; }

	/**
	 * Method to return the full name of this Layer.
	 * Full name has format "techName:layerName"
	 * @return the full name of this Layer.
	 */
	public String getFullName() { return layerId.fullName; }

	/**
	 * Method to return the index of this Layer.
	 * The index is 0-based.
	 * @return the index of this Layer.
	 */
	public int getIndex() { return index; }

	/**
	 * Method to set the index of this Layer.
	 * The index is 0-based.
	 * @param index the index of this Layer.
	 */
	public void setIndex(int index) { this.index = index; }

	/**
	 * Method to return the Technology of this Layer.
	 * @return the Technology of this Layer.
	 */
	public Technology getTechnology() { return tech; }

	/**
	 * Method to set the graphics description of this Layer.
	 * @param graphics graphics description of this Layer.
	 */
	public void setGraphics(EGraphics graphics) {
        UserInterfaceMain.setGraphicsPreferences(UserInterfaceMain.getGraphicsPreferences().withGraphics(this, graphics));
    }

	/**
	 * Method to return the graphics description of this Layer.
	 * @return the graphics description of this Layer.
	 */
	public EGraphics getGraphics() { return UserInterfaceMain.getGraphicsPreferences().getGraphics(this); }

	/**
	 * Method to return the graphics description of this Layer by factory default.
	 * @return the factory graphics description of this Layer.
	 */
	public EGraphics getFactoryGraphics() { return factoryGraphics; }

	/**
	 * Method to set the Function of this Layer.
	 * @param function the Function of this Layer.
	 */
	public void setFunction(Function function)
	{
		this.function = function;
		this.functionExtras = NO_FUNCTION_EXTRAS;
	}

	/**
	 * Method to set the Function of this Layer when the function is complex.
	 * Some layer functions have extra bits of information to describe them.
	 * For example, P-Type Diffusion has the Function DIFF but the extra bits PTYPE.
	 * @param function the Function of this Layer.
	 * @param functionExtras extra bits to describe the Function of this Layer.
	 */
	public void setFunction(Function function, int functionExtras) {
		this.function = function;
        int numBits = 0;
        for (int i = 0; i < 32; i++) {
            if ((functionExtras & (1 << i)) != 0)
                numBits++;
        }
        if (numBits >= 2 &&
                functionExtras != (DEPLETION|HEAVY) && functionExtras != (DEPLETION|LIGHT) &&
                functionExtras != (ENHANCEMENT|HEAVY) && functionExtras != (ENHANCEMENT|LIGHT) ||
                numBits == 1 && Function.getExtraConstantName(functionExtras).length() == 0)
            throw new IllegalArgumentException("functionExtras=" + Integer.toHexString(functionExtras));
        this.functionExtras = functionExtras;
	}

	/**
	 * Method to return the Function of this Layer.
	 * @return the Function of this Layer.
	 */
	public Function getFunction() { return function; }

	/**
	 * Method to return the Function "extras" of this Layer.
	 * The "extras" are a set of modifier bits, such as "p-type".
	 * @return the Function extras of this Layer.
	 */
	public int getFunctionExtras() { return functionExtras; }

	/**
	 * Method to set the Pure Layer Node associated with this Layer.
	 * @param pln the Pure Layer PrimitiveNode to use for this Layer.
	 */
	public void setPureLayerNode(PrimitiveNode pln) { pureLayerNode = pln; }

	/**
	 * Method to make the Pure Layer Node associated with this Layer.
	 * @param nodeName the name of the PrimitiveNode.
	 * Primitive names may not contain unprintable characters, spaces, tabs, a colon (:), semicolon (;) or curly braces ({}).
	 * @param size the width and the height of the PrimitiveNode.
     * @param style the Poly.Type this PrimitiveNode will generate (polygon, cross, etc.).
	 * @return the Pure Layer PrimitiveNode to use for this Layer.
	 */
	public PrimitiveNode makePureLayerNode(String nodeName, double size, Poly.Type style, String portName, ArcProto ... connections) {
		PrimitiveNode pln = PrimitiveNode.newInstance0(nodeName, tech, size, size,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(this, 0, style, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		pln.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(tech, pln, connections, portName, 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			}, false);
		pln.setFunction(PrimitiveNode.Function.NODE);
		pln.setHoldsOutline();
		pln.setSpecialType(PrimitiveNode.POLYGONAL);
        pureLayerNode = pln;
        return pln;
    }

	/**
	 * Method to return the Pure Layer Node associated with this Layer.
	 * @return the Pure Layer Node associated with this Layer.
	 */
	public PrimitiveNode getPureLayerNode() { return pureLayerNode; }

	/**
	 * Method to tell whether this layer function is non-electrical.
	 * Non-electrical layers do not carry any signal (for example, artwork, text).
	 * @return true if this layer function is non-electrical.
	 */
	public boolean isNonElectrical()
	{
		return (functionExtras&Function.NONELEC) != 0;
	}

    /**
     * Method to determine if the layer function corresponds to a diffusion layer.
     * Used in parasitic calculation
     * @return true if this Layer is diffusion.
     */
    public boolean isDiffusionLayer()
    {
		return !isPseudoLayer() && getFunction().isDiff();
    }

    /**
     * Method to determine if the layer corresponds to a VT layer. Used in DRC
     * @return true if this layer is a VT layer.
     */
    public boolean isVTImplantLayer()
    {
        return (function.isImplant() && (functionExtras&Layer.Function.HLVT) != 0);
    }

    /**
     * Method to determine if the layer corresponds to a poly cut layer. Used in 3D View
     * @return true if this layer is a poly cut layer.
     */
    public boolean isPolyCutLayer()
    {
        return (function.isContact() && (functionExtras&Layer.Function.CONPOLY) != 0);
    }

    /**
     * Method to determine if the layer corresponds to a poly cut layer. Used in 3D View
     * @return true if this layer is a poly cut layer.
     */
    public boolean isCarbonNanotubeLayer()
    {
        return (functionExtras&Layer.CARBNANO) != 0;
    }

    /**
	 * Method to return true if this is pseudo-Layer.
	 * Pseudo layers are those used in pins, and have no real geometry.
	 * @return true if this is pseudo-layer.
	 */
	public boolean isPseudoLayer() { return pseudo; }
	/**
	 * Method to return the pseudo layer associated with this real-Layer.
	 * Pseudo layers are those used in pins, and have no real geometry.
	 * @return the pseudo layer associated with this read-Layer.
	 * If this layer is hass not pseudo, the null is returned.
	 */
	public Layer getPseudoLayer() { return pseudoLayer; }
	/**
	 * Method to return the non-pseudo layer associated with this pseudo-Layer.
	 * Pseudo layers are those used in pins, and have no real geometry.
	 * @return the non-pseudo layer associated with this pseudo-Layer.
	 * If this layer is already not pseudo, this layer is returned.
	 */
	public Layer getNonPseudoLayer() { return nonPseudoLayer; }

    private Setting makeLayerSetting(String what, String factory) {
        String techName = tech.getTechName();
        return getSubNode(what).makeStringSetting(what + "LayerFor" + getName() + "IN" + techName,
                Technology.TECH_NODE,
                getName(), what + " tab", what + " for layer " + getName() + " in technology " + techName, factory);
    }

    private Setting makeParasiticSetting(String what, double factory)
    {
        return getSubNode(what).makeDoubleSetting(what + "ParasiticFor" + getName() + "IN" + tech.getTechName(),
                Technology.TECH_NODE,
                getName(), "Parasitic tab", "Technology " + tech.getTechName() + ", " + what + " for layer " + getName(), factory);
    }

    private Setting make3DSetting(String what, double factory)
    {
        factory = DBMath.round(factory);
        return getSubNode(what).makeDoubleSetting(what + "Of" + getName() + "IN" + tech.getTechName(),
                Technology.TECH_NODE,
                getName(), "3D tab", "Technology " + tech.getTechName() + ", 3D " + what + " for layer " + getName(), factory);
    }

    private Setting.Group getSubNode(String type) {
        return tech.getProjectSettings().node(type);
    }

	/**
	 * Method to set the 3D distance and thickness of this Layer.
	 * @param thickness the thickness of this layer.
     * @param distance the distance of this layer above the ground plane (silicon).
     * Negative values represent layes in silicon like p++, p well, etc.
     */
	public void setFactory3DInfo(double thickness, double distance)
	{
        assert !isPseudoLayer();

        thickness = DBMath.round(thickness);
        distance = DBMath.round(distance);
        // We don't call setDistance and setThickness directly here due to reflection code.
        layer3DDistanceSetting = make3DSetting("Distance", distance);
		layer3DThicknessSetting = make3DSetting("Thickness", thickness);
    }

	/**
	 * Method to return the distance of this layer, by default.
	 * The higher the distance value, the farther from the wafer.
	 * @return the distance of this layer above the ground plane, by default.
	 */
	public double getDistance() { return layer3DDistanceSetting.getDouble(); }
	/**
	 * Returns project preferences to tell the distance of this layer.
	 * @return project preferences to tell the distance of this layer.
	 */
	public Setting getDistanceSetting() { return layer3DDistanceSetting; }

	/**
	 * Method to return the thickness of this layer, by default.
	 * Layers can have a thickness of 0, which causes them to be rendered flat.
	 * @return the distance of this layer above the ground plane, by default.
	 */
	public double getThickness() { return layer3DThicknessSetting.getDouble(); }
	/**
	 * Returns project preferences to tell the thickness of this layer.
	 * @return project preferences to tell the thickness of this layer.
	 */
	public Setting getThicknessSetting() { return layer3DThicknessSetting; }

	/**
	 * Method to calculate Z value of the upper part of the layer.
	 * Note: not called getHeight to avoid confusion
	 * with getDistance())
     * Don't call distance+thickness because those are factory values.
	 * @return Depth of the layer
	 */
	public double getDepth() { return DBMath.round(getDistance()+getThickness()); }

	/**
	 * Method to set the factory-default CIF name of this Layer.
	 * @param cifLayer the factory-default CIF name of this Layer.
	 */
	public void setFactoryCIFLayer(String cifLayer) {
        assert !isPseudoLayer();
        cifLayerSetting = makeLayerSetting("CIF", cifLayer);
    }
	/**
	 * Method to return the CIF name of this layer.
	 * @return the CIF name of this layer.
	 */
	public String getCIFLayer() { return cifLayerSetting.getString(); }
	/**
	 * Returns project preferences to tell the CIF name of this Layer.
	 * @return project preferences to tell the CIF name of this Layer.
	 */
	public Setting getCIFLayerSetting() { return cifLayerSetting; }


    /**
     * Generate key name for GDS value depending on the foundry
     * @return
     */
//    private String getGDSPrefName(String foundry)
//    {
//        return ("GDS("+foundry+")");
//    }

	/**
	 * Method to set the factory-default GDS name of this Layer.
	 * @param factoryDefault the factory-default GDS name of this Layer.
     * @param foundry
     */
//	public void setFactoryGDSLayer(String factoryDefault, String foundry)
//    {
//        // Getting rid of spaces
//        String value = factoryDefault.replaceAll(", ", ",");
//        getLayerSetting(getGDSPrefName(foundry), gdsLayerPrefs, value);
//    }

	/**
	 * Method to set the GDS name of this Layer.
	 * @param gdsLayer the GDS name of this Layer.
	 */
//	public void setGDSLayer(String gdsLayer)
//    {
//        assert(this.gdsLayer == null);// probing gdsLayer is never used.
//		getLayerSetting(getGDSPrefName(tech.getPrefFoundry()), gdsLayerPrefs, this.gdsLayer).setString(gdsLayer);
//    }

	/**
	 * Method to return the GDS name of this layer.
	 * @return the GDS name of this layer.
	 */
//	public String getGDSLayer()
//    {
//        assert(gdsLayer == null);// probing gdsLayer is never used.
//        return getLayerSetting(getGDSPrefName(tech.getPrefFoundry()), gdsLayerPrefs, gdsLayer).getString();
//    }

	/**
	 * Method to set the factory-default DXF name of this Layer.
	 * @param dxfLayer the factory-default DXF name of this Layer.
	 */
	public void setFactoryDXFLayer(String dxfLayer) {
        assert !isPseudoLayer();
        dxfLayerSetting = makeLayerSetting("DXF", dxfLayer);
    }

	/**
	 * Method to return the DXF name of this layer.
	 * @return the DXF name of this layer.
	 */
	public String getDXFLayer()
	{
		if (dxfLayerSetting == null) return "";
		return dxfLayerSetting.getString();
	}
	/**
	 * Returns project preferences to tell the DXF name of this Layer.
	 * @return project preferences to tell the DXF name of this Layer.
	 */
    public Setting getDXFLayerSetting() { return dxfLayerSetting; }

	/**
	 * Method to set the factory-default Skill name of this Layer.
	 * @param skillLayer the factory-default Skill name of this Layer.
	 */
	public void setFactorySkillLayer(String skillLayer) {
        assert !isPseudoLayer();
        skillLayerSetting = makeLayerSetting("Skill", skillLayer);
    }
	/**
	 * Method to return the Skill name of this layer.
	 * @return the Skill name of this layer.
	 */
	public String getSkillLayer() { return skillLayerSetting.getString(); }
	/**
	 * Returns project preferences to tell the Skill name of this Layer.
	 * @return project preferences to tell the Skill name of this Layer.
	 */
	public Setting getSkillLayerSetting() { return skillLayerSetting; }

	/**
	 * Method to set the Spice parasitics for this Layer.
	 * This is typically called only during initialization.
	 * It does not set the "option" storage, as "setResistance()",
	 * "setCapacitance()", and ""setEdgeCapacitance()" do.
	 * @param resistance the resistance of this Layer.
	 * @param capacitance the capacitance of this Layer.
	 * @param edgeCapacitance the edge capacitance of this Layer.
	 */
	public void setFactoryParasitics(double resistance, double capacitance, double edgeCapacitance)
	{
        assert !isPseudoLayer();
		resistanceSetting = makeParasiticSetting("Resistance", resistance);
		capacitanceSetting = makeParasiticSetting("Capacitance", capacitance);
		edgeCapacitanceSetting = makeParasiticSetting("EdgeCapacitance", edgeCapacitance);
	}

//    /**
//     * Reset this layer's Parasitics to their factory default values
//     */
//    public void resetToFactoryParasitics()
//    {
//        double res = resistanceSetting.getDoubleFactoryValue();
//        double cap = capacitanceSetting.getDoubleFactoryValue();
//        double edgecap = edgeCapacitanceSetting.getDoubleFactoryValue();
//        setResistance(res);
//        setCapacitance(cap);
//        setEdgeCapacitance(edgecap);
//    }

	/**
	 * Method to return the resistance for this layer.
	 * @return the resistance for this layer.
	 */
	public double getResistance() { return resistanceSetting.getDouble(); }
	/**
	 * Returns project preferences to tell the resistance for this Layer.
	 * @return project preferences to tell the resistance for this Layer.
	 */
	public Setting getResistanceSetting() { return resistanceSetting; }

	/**
	 * Method to return the capacitance for this layer.
	 * @return the capacitance for this layer.
	 */
	public double getCapacitance() { return capacitanceSetting.getDouble(); }
	/**
	 * Returns project preferences to tell the capacitance for this Layer.
	 * Returns project preferences to tell the capacitance for this Layer.
	 */
	public Setting getCapacitanceSetting() { return capacitanceSetting; }

	/**
	 * Method to return the edge capacitance for this layer.
	 * @return the edge capacitance for this layer.
	 */
	public double getEdgeCapacitance() { return edgeCapacitanceSetting.getDouble(); }
    /**
     * Returns project preferences to tell the edge capacitance for this Layer.
     * Returns project preferences to tell the edge capacitance for this Layer.
     */
    public Setting getEdgeCapacitanceSetting() { return edgeCapacitanceSetting; }

    /**
     * Method to finish initialization of this Layer.
     */
    void finish() {
		if (resistanceSetting == null || capacitanceSetting == null || edgeCapacitanceSetting == null) {
            setFactoryParasitics(0, 0, 0);
        }
        if (cifLayerSetting == null) {
            setFactoryCIFLayer("");
        }
        if (dxfLayerSetting == null) {
            setFactoryDXFLayer("");
        }
        if (skillLayerSetting == null) {
            setFactorySkillLayer("");
        }
        if (layer3DThicknessSetting == null || layer3DDistanceSetting == null) {
            double thickness = layer3DThicknessSetting != null ? getThickness() : DEFAULT_THICKNESS;
            double distance = layer3DDistanceSetting != null ? getDistance() : DEFAULT_DISTANCE;
            setFactory3DInfo(thickness, distance);
        }
    }

	/**
	 * Returns a printable version of this Layer.
	 * @return a printable version of this Layer.
	 */
	public String toString()
	{
		return "Layer " + getName();
	}

    public void copyState(Layer that) {
        assert getName().equals(that.getName());
        if (pureLayerNode != null) {
            assert pureLayerNode.getId() == that.pureLayerNode.getId();
//            pureLayerNode.setDefSize(that.pureLayerNode.getDefWidth(), that.pureLayerNode.getDefHeight());
        }
    }

    void dump(PrintWriter out, Map<Setting,Object> settings) {
        final String[] layerBits = {
            null, null, null,
            null, null, null,
            "PTYPE", "NTYPE", "DEPLETION",
            "ENHANCEMENT",  "LIGHT", "HEAVY",
            null, "NONELEC", "CONMETAL",
            "CONPOLY", "CONDIFF", null,
            null, null, null,
            "HLVT", "INTRANS", "THICK"
        };
        out.print("Layer " + getName() + " " + getFunction().name());
        Technology.printlnBits(out, layerBits, getFunctionExtras());
        out.print("\t"); Technology.printlnSetting(out, settings, getCIFLayerSetting());
        out.print("\t"); Technology.printlnSetting(out, settings, getDXFLayerSetting());
        out.print("\t"); Technology.printlnSetting(out, settings, getSkillLayerSetting());
        out.print("\t"); Technology.printlnSetting(out, settings, getResistanceSetting());
        out.print("\t"); Technology.printlnSetting(out, settings, getCapacitanceSetting());
        out.print("\t"); Technology.printlnSetting(out, settings, getEdgeCapacitanceSetting());
        // GDS
        EGraphics factoryDesc = getFactoryGraphics();
        EGraphics desc = factoryDesc;
        out.println("\tpatternedOnDisplay=" + desc.isPatternedOnDisplay() + "(" + factoryDesc.isPatternedOnDisplay() + ")");
        out.println("\tpatternedOnPrinter=" + desc.isPatternedOnPrinter() + "(" + factoryDesc.isPatternedOnPrinter() + ")");
        out.println("\toutlined=" + desc.getOutlined() + "(" + factoryDesc.getOutlined() + ")");
        out.println("\ttransparent=" + desc.getTransparentLayer() + "(" + factoryDesc.getTransparentLayer() + ")");
        out.println("\tcolor=" + Integer.toHexString(desc.getColor().getRGB()) + "(" + Integer.toHexString(factoryDesc.getRGB()) + ")");
        out.println("\topacity=" + desc.getOpacity() + "(" + factoryDesc.getOpacity() + ")");
        out.println("\tforeground=" + factoryDesc.getForeground());
        int pattern[] = factoryDesc.getPattern();
        out.print("\tpattern");
        for (int p: pattern)
            out.print(" " + Integer.toHexString(p));
        out.println();
        out.println("\tdistance3D=" + getDistanceSetting().getDoubleFactoryValue());
        out.println("\tthickness3D=" + getThicknessSetting().getDoubleFactoryValue());
        out.println("\tmode3D=" + factoryDesc.getTransparencyMode());
        out.println("\tfactor3D=" + factoryDesc.getTransparencyFactor());

        if (getPseudoLayer() != null)
            out.println("\tpseudoLayer=" + getPseudoLayer().getName());
    }

    /**
     * Method to create XML version of a Layer.
     * @return
     */
    Xml.Layer makeXml() {
        Xml.Layer l = new Xml.Layer();
        l.name = getName();
        l.function = getFunction();
        l.extraFunction = getFunctionExtras();
        l.desc = getFactoryGraphics();
        l.height3D = getDistanceSetting().getDoubleFactoryValue();
        l.thick3D = getThicknessSetting().getDoubleFactoryValue();
        l.cif = (String)getCIFLayerSetting().getFactoryValue();
        l.skill = (String)getSkillLayerSetting().getFactoryValue();
        l.resistance = getResistanceSetting().getDoubleFactoryValue();
        l.capacitance = getCapacitanceSetting().getDoubleFactoryValue();
        l.edgeCapacitance = getEdgeCapacitanceSetting().getDoubleFactoryValue();
//            if (layer.getPseudoLayer() != null)
//                l.pseudoLayer = layer.getPseudoLayer().getName();
        if (pureLayerNode != null) {
            l.pureLayerNode = new Xml.PureLayerNode();
            l.pureLayerNode.name = pureLayerNode.getName();
            for (Map.Entry<String,PrimitiveNode> e: tech.getOldNodeNames().entrySet()) {
                if (e.getValue() != pureLayerNode) continue;
                assert l.pureLayerNode.oldName == null;
                l.pureLayerNode.oldName = e.getKey();
            }
            l.pureLayerNode.style = pureLayerNode.getNodeLayers()[0].getStyle();
            l.pureLayerNode.port = pureLayerNode.getPort(0).getName();
            l.pureLayerNode.size.addLambda(DBMath.gridToLambda(2*pureLayerNode.getFactoryDefaultGridExtendX()));
            for (ArcProto ap: pureLayerNode.getPort(0).getConnections()) {
                if (ap.getTechnology() != tech) continue;
                l.pureLayerNode.portArcs.add(ap.getName());
            }
        }
        return l;
    }
}
