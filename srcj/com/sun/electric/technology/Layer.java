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

import com.sun.electric.database.geometry.EGraphics;

import java.util.Iterator;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

/**
 * The Layer class defines a single layer of material, out of which NodeInst and ArcInst objects are created.
 * The Layers are defined by the PrimitiveNode and PrimitiveArc classes, and are used in the generation of geometry.
 * In addition, layers have extra information that is used for output and behavior.
 */
public class Layer
{
	/**
	 * Function is a typesafe enum class that describes the function of a layer.
	 * Functions are technology-independent and describe the nature of the layer (Metal, Polysilicon, etc.)
	 */
	public static class Function
	{
		private final String name;
		private final String constantName;
		private final int extraBits;

		private Function(String name, String constantName, int extraBits)
		{
			this.name = name;
			this.constantName = constantName;
			this.extraBits = extraBits;
		}

		/**
		 * Returns a printable version of this Layer.
		 * @return a printable version of this Layer.
		 */
		public String toString() { return name; }

		/** Describes a P-type layer. */							public static final int PTYPE =          0100;
		/** Describes a N-type layer. */							public static final int NTYPE =          0200;
		/** Describes a depletion layer. */							public static final int DEPLETION =      0400;
		/** Describes a enhancement layer. */						public static final int ENHANCEMENT =   01000;
		/** Describes a light doped layer. */						public static final int LIGHT =         02000;
		/** Describes a heavy doped layer. */						public static final int HEAVY =         04000;
		/** Describes a pseudo layer. */							public static final int PSEUDO =       010000;
		/** Describes a nonelectrical layer (does not carry signals). */						public static final int NONELEC =      020000;
		/** Describes a layer that is contacts metal (used to identify contacts/vias). */		public static final int CONMETAL =     040000;
		/** Describes a layer that is contacts polysilicon (used to identify contacts/vias). */	public static final int CONPOLY =     0100000;
		/** Describes a layer that is contacts diffusion (used to identify contacts/vias). */	public static final int CONDIFF =     0200000;
		/** Describes a layer that is inside transistor. */			public static final int INTRANS =   020000000;

		/** Describes an unknown layer. */							public static final Function UNKNOWN    = new Function("unknown",    "LFUNKNOWN", 0);
		/** Describes a metal layer 1. */							public static final Function METAL1     = new Function("metal-1",    "LFMETAL1", 0);
		/** Describes a metal layer 2. */							public static final Function METAL2     = new Function("metal-2",    "LFMETAL2", 0);
		/** Describes a metal layer 3. */							public static final Function METAL3     = new Function("metal-3",    "LFMETAL3", 0);
		/** Describes a metal layer 4. */							public static final Function METAL4     = new Function("metal-4",    "LFMETAL4", 0);
		/** Describes a metal layer 5. */							public static final Function METAL5     = new Function("metal-5",    "LFMETAL5", 0);
		/** Describes a metal layer 6. */							public static final Function METAL6     = new Function("metal-6",    "LFMETAL6", 0);
		/** Describes a metal layer 7. */							public static final Function METAL7     = new Function("metal-7",    "LFMETAL7", 0);
		/** Describes a metal layer 8. */							public static final Function METAL8     = new Function("metal-8",    "LFMETAL8", 0);
		/** Describes a metal layer 9. */							public static final Function METAL9     = new Function("metal-9",    "LFMETAL9", 0);
		/** Describes a metal layer 10. */							public static final Function METAL10    = new Function("metal-10",   "LFMETAL10", 0);
		/** Describes a metal layer 11. */							public static final Function METAL11    = new Function("metal-11",   "LFMETAL11", 0);
		/** Describes a metal layer 12. */							public static final Function METAL12    = new Function("metal-12",   "LFMETAL12", 0);
		/** Describes a polysilicon layer 1. */						public static final Function POLY1      = new Function("poly-1",     "LFPOLY1", 0);
		/** Describes a polysilicon layer 2. */						public static final Function POLY2      = new Function("poly-2",     "LFPOLY2", 0);
		/** Describes a polysilicon layer 3. */						public static final Function POLY3      = new Function("poly-3",     "LFPOLY3", 0);
		/** Describes a polysilicon gate layer. */					public static final Function GATE       = new Function("gate",       "LFGATE", INTRANS);
		/** Describes a diffusion layer. */							public static final Function DIFF       = new Function("diffusion",  "LFDIFF", 0);
		/** Describes a P-diffusion layer. */						public static final Function DIFFP      = new Function("p-diffusion","LFDIFF", PTYPE);
		/** Describes a N-diffusion layer. */						public static final Function DIFFN      = new Function("n-diffusion","LFDIFF", NTYPE);
		/** Describes an implant layer. */							public static final Function IMPLANT    = new Function("implant",    "LFIMPLANT", 0);
		/** Describes a P-implant layer. */							public static final Function IMPLANTP   = new Function("p-implant",  "LFIMPLANT", PTYPE);
		/** Describes an N-implant layer. */						public static final Function IMPLANTN   = new Function("n-implant",  "LFIMPLANT", NTYPE);
		/** Describes a contact layer 1. */							public static final Function CONTACT1   = new Function("contact-1",  "LFCONTACT1", 0);
		/** Describes a contact layer 2. */							public static final Function CONTACT2   = new Function("contact-2",  "LFCONTACT2", 0);
		/** Describes a contact layer 3. */							public static final Function CONTACT3   = new Function("contact-3",  "LFCONTACT3", 0);
		/** Describes a contact layer 4. */							public static final Function CONTACT4   = new Function("contact-4",  "LFCONTACT4", 0);
		/** Describes a contact layer 5. */							public static final Function CONTACT5   = new Function("contact-5",  "LFCONTACT5", 0);
		/** Describes a contact layer 6. */							public static final Function CONTACT6   = new Function("contact-6",  "LFCONTACT6", 0);
		/** Describes a contact layer 7. */							public static final Function CONTACT7   = new Function("contact-7",  "LFCONTACT7", 0);
		/** Describes a contact layer 8. */							public static final Function CONTACT8   = new Function("contact-8",  "LFCONTACT8", 0);
		/** Describes a contact layer 9. */							public static final Function CONTACT9   = new Function("contact-9",  "LFCONTACT9", 0);
		/** Describes a contact layer 10. */						public static final Function CONTACT10  = new Function("contact-10", "LFCONTACT10", 0);
		/** Describes a contact layer 11. */						public static final Function CONTACT11  = new Function("contact-11", "LFCONTACT11", 0);
		/** Describes a contact layer 12. */						public static final Function CONTACT12  = new Function("contact-12", "LFCONTACT12", 0);
		/** Describes a sinker layer (diffusion-to-buried plug). */	public static final Function PLUG       = new Function("plug",       "LFPLUG", 0);
		/** Describes an overglass layer (passivation). */			public static final Function OVERGLASS  = new Function("overglass",  "LFOVERGLASS", 0);
		/** Describes a resistor layer. */							public static final Function RESISTOR   = new Function("resistor",   "LFRESISTOR", 0);
		/** Describes a capacitor layer. */							public static final Function CAP        = new Function("capacitor",  "LFCAP", 0);
		/** Describes a transistor layer (usually a pseudo-layer). */	public static final Function TRANSISTOR = new Function("transistor", "LFTRANSISTOR", 0);
		/** Describes an emitter layer of a bipolar transistor. */	public static final Function EMITTER    = new Function("emitter",    "LFEMITTER", 0);
		/** Describes a base layer of a bipolar transistor. */		public static final Function BASE       = new Function("base",       "LFBASE", 0);
		/** Describes a collector layer of a bipolar transistor. */	public static final Function COLLECTOR  = new Function("collector",  "LFCOLLECTOR", 0);
		/** Describes a substrate layer. */							public static final Function SUBSTRATE  = new Function("substrate",  "LFSUBSTRATE", 0);
		/** Describes a well layer. */								public static final Function WELL       = new Function("well",       "LFWELL", 0);
		/** Describes a P-well layer. */							public static final Function WELLP      = new Function("p-well",     "LFWELL", PTYPE);
		/** Describes a N-well layer. */							public static final Function WELLN      = new Function("n-well",     "LFWELL", NTYPE);
		/** Describes a guard layer. */								public static final Function GUARD      = new Function("guard",      "LFGUARD", 0);
		/** Describes an isolation layer (bipolar). */				public static final Function ISOLATION  = new Function("isolation",  "LFISOLATION", 0);
		/** Describes a bus layer. */								public static final Function BUS        = new Function("bus",        "LFBUS", 0);
		/** Describes an artwork layer. */							public static final Function ART        = new Function("art",        "LFART", 0);
		/** Describes a control layer. */							public static final Function CONTROL    = new Function("control",    "LFCONTROL", 0);

		/**
		 * Method to tell whether this layer function is metal.
		 * @return true if this layer function is metal.
		 */
		public boolean isMetal()
		{
			if (this == METAL1  || this == METAL2  || this == METAL3 ||
				this == METAL4  || this == METAL5  || this == METAL6 ||
				this == METAL7  || this == METAL8  || this == METAL9 ||
				this == METAL10 || this == METAL11 || this == METAL12) return true;
			return false;
		}

		/**
		 * Method to tell whether this layer function is diffusion (active).
		 * @return true if this layer function is diffusion (active).
		 */
		public boolean isDiff()
		{
			if (this == DIFF || this == DIFFP || this == DIFFN) return true;
			return false;
		}

		/**
		 * Method to tell whether this layer function is polysilicon.
		 * @return true if this layer function is polysilicon.
		 */
		public boolean isPoly()
		{
			if (this == POLY1 || this == POLY2 || this == POLY3 || this == GATE) return true;
			return false;
		}

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
		public boolean isContact()
		{
			if (this == CONTACT1 || this == CONTACT2 || this == CONTACT3 ||
				this == CONTACT4 || this == CONTACT5 || this == CONTACT6 ||
				this == CONTACT7 || this == CONTACT8 || this == CONTACT9 ||
				this == CONTACT10 || this == CONTACT11 || this == CONTACT12)  return true;
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
	}

	private String name;
	private int index;
	private Technology tech;
	private EGraphics graphics;
	private Function function;
	private int functionExtras;
	private String cifLayer;
	private String dxfLayer;
	private String gdsLayer;
	private String skillLayer;
	private double thickness, height;
	private double resistance, capacitance, edgeCapacitance;
	private Layer nonPseudoLayer;
	private boolean visible;

	private Layer(String name, Technology tech, EGraphics graphics)
	{
		this.name = name;
		this.tech = tech;
		this.graphics = graphics;
		this.nonPseudoLayer = this;
		this.visible = true;
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
		Layer layer = new Layer(name, tech, graphics);
		tech.addLayer(layer);
		return layer;
	}

	/**
	 * Method to return the name of this Layer.
	 * @return the name of this Layer.
	 */
	public String getName() { return name; }

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
	 * Method to return the graphics description of this Layer.
	 * @return the graphics description of this Layer.
	 */
	public EGraphics getGraphics() { return graphics; }

	/**
	 * Method to set the Function of this Layer.
	 * @param function the Function of this Layer.
	 */
	public void setFunction(Function function)
	{
		this.function = function;
		this.functionExtras = 0;
	}

	/**
	 * Method to set the Function of this Layer when the function is complex.
	 * Some layer functions have extra bits of information to describe them.
	 * For example, P-Type Diffusion has the Function DIFF but the extra bits PTYPE.
	 * @param function the Function of this Layer.
	 * @param functionExtras extra bits to describe the Function of this Layer.
	 */
	public void setFunction(Function function, int functionExtras)
	{
		this.function = function;
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
	 * Method to tell whether this layer function is non-electrical.
	 * Non-electrical layers do not carry any signal (for example, artwork, text).
	 * @return true if this layer function is non-electrical.
	 */
	public boolean isNonElectrical()
	{
		return (functionExtras&Function.NONELEC) != 0;
	}

	/**
	 * Method to return the non-pseudo layer associated with this pseudo-Layer.
	 * Pseudo layers are those used in pins, and have no real geometry.
	 * @return the non-pseudo layer associated with this pseudo-Layer.
	 * If this layer is already not pseudo, this layer is returned.
	 */
	public Layer getNonPseudoLayer() { return nonPseudoLayer; }

	/**
	 * Method to set the non-pseudo layer associated with this pseudo-Layer.
	 * Pseudo layers are those used in pins, and have no real geometry.
	 * @param nonPseudoLayer the non-pseudo layer associated with this pseudo-Layer.
	 */
	public void setNonPseudoLayer(Layer nonPseudoLayer) { this.nonPseudoLayer = nonPseudoLayer; }

	/**
	 * Method to tell whether this Layer is visible.
	 * @return true if this Layer is visible.
	 */
	public boolean isVisible() { return visible; }

	/**
	 * Method to set whether this Layer is visible.
	 * @param visible true if this Layer is to be visible.
	 */
	public void setVisible(boolean visible) { this.visible = visible; }

	/**
	 * Method to set the 3D height and thickness of this Layer.
	 * @param thickness the thickness of this layer.
	 * Most layers have a thickness of 0, but contact layers are fatter
	 * because they bridge layers...they typically have a thickness of 1.
	 * @param height the height of this layer above the ground plane.
	 * The higher the height value, the farther from the wafer.
	 */
	public void setHeight(double thickness, double height)
	{
		this.thickness = thickness;
		this.height = height;
	}

	/**
	 * Method to return the height of this layer.
	 * @return the height of this layer above the ground plane.
	 * The higher the height value, the farther from the wafer.
	 */
	public double getHeight() { return height; }

	/**
	 * Method to return the thickness of this layer.
	 * @return the thickness of this layer.
	 * Most layers have a thickness of 0, but contact layers are fatter
	 * because they bridge layers...they typically have a thickness of 1.
	 */
	public double getThickness() { return thickness; }

	/**
	 * Method to set the CIF name of this Layer.
	 * @param cifLayer the CIF name of this Layer.
	 */
	public void setCIFLayer(String cifLayer) { this.cifLayer = cifLayer; }

	/**
	 * Method to return the CIF name of this layer.
	 * @return the CIF name of this layer.
	 */
	public String getCIFLayer() { return cifLayer; }

	/**
	 * Method to set the DXF name of this Layer.
	 * @param dxfLayer the DXF name of this Layer.
	 */
	public void setDXFLayer(String dxfLayer) { this.dxfLayer = dxfLayer; }

	/**
	 * Method to return the DXF name of this layer.
	 * @return the DXF name of this layer.
	 */
	public String getDXFLayer() { return dxfLayer; }

	/**
	 * Method to set the GDS name of this Layer.
	 * @param gdsLayer the GDS name of this Layer.
	 */
	public void setGDSLayer(String gdsLayer) { this.gdsLayer = gdsLayer; }

	/**
	 * Method to return the GDS name of this layer.
	 * @return the GDS name of this layer.
	 */
	public String getGDSLayer() { return gdsLayer; }

	/**
	 * Method to set the Skill name of this Layer.
	 * @param skillLayer the Skill name of this Layer.
	 */
	public void setSkillLayer(String skillLayer) { this.skillLayer = skillLayer; }

	/**
	 * Method to return the Skill name of this layer.
	 * @return the Skill name of this layer.
	 */
	public String getSkillLayer() { return skillLayer; }

	/**
	 * Method to set the Spice parasitics for this Layer.
	 * This is typically called only during initialization.
	 * It does not set the "option" storage, as "setResistance()",
	 * "setCapacitance()", and ""setEdgeCapacitance()" do.
	 * @param resistance the resistance of this Layer.
	 * @param capacitance the capacitance of this Layer.
	 * @param edgeCapacitance the edge capacitance of this Layer.
	 */
	public void setDefaultParasitics(double resistance, double capacitance, double edgeCapacitance)
	{
		this.resistance = resistance;
		this.capacitance = capacitance;
		this.edgeCapacitance = edgeCapacitance;
	}

	/**
	 * Method to return the resistance for this layer.
	 * @return the resistance for this layer.
	 */
	public double getResistance() { tech.gatherParasiticOverrides();   return resistance; }

	/**
	 * Method to set the resistance for this Layer.
	 * Also saves this information in the permanent options.
	 * @param resistance the new resistance for this Layer.
	 */
	public void setResistance(double resistance)
	{
		this.resistance = resistance;
		Preferences prefs = Preferences.userNodeForPackage(tech.getClass());
		prefs.putDouble("LayerResistance_" + tech.getTechName() + "_" + getName(), resistance);
		try
		{
	        prefs.flush();
		} catch (BackingStoreException e)
		{
			System.out.println("Failed to save resistance option for layer " + getName());
		}
	}

	/**
	 * Method to return the capacitance for this layer.
	 * @return the capacitance for this layer.
	 */
	public double getCapacitance() { tech.gatherParasiticOverrides();   return capacitance; }

	/**
	 * Method to set the capacitance for this Layer.
	 * Also saves this information in the permanent options.
	 * @param capacitance the new capacitance for this Layer.
	 */
	public void setCapacitance(double capacitance)
	{
		this.capacitance = capacitance;
		Preferences prefs = Preferences.userNodeForPackage(tech.getClass());
		prefs.putDouble("LayerCapacitance_" + tech.getTechName() + "_" + getName(), capacitance);
		try
		{
	        prefs.flush();
		} catch (BackingStoreException e)
		{
			System.out.println("Failed to save capacitance option for layer " + getName());
		}
	}

	/**
	 * Method to return the edge capacitance for this layer.
	 * @return the edge capacitance for this layer.
	 */
	public double getEdgeCapacitance() { tech.gatherParasiticOverrides();   return edgeCapacitance; }

	/**
	 * Method to set the edge capacitance for this Layer.
	 * Also saves this information in the permanent options.
	 * @param edgeCapacitance the new edge capacitance for this Layer.
	 */
	public void setEdgeCapacitance(double edgeCapacitance)
	{
		this.edgeCapacitance = edgeCapacitance;
		Preferences prefs = Preferences.userNodeForPackage(tech.getClass());
		prefs.putDouble("LayerEdgeCapacitance_" + tech.getTechName() + "_" + getName(), edgeCapacitance);
		try
		{
	        prefs.flush();
		} catch (BackingStoreException e)
		{
			System.out.println("Failed to save edge capacitance option for layer " + getName());
		}
	}

	/**
	 * Returns a printable version of this Layer.
	 * @return a printable version of this Layer.
	 */
	public String toString()
	{
		return "Layer " + name;
	}
}
