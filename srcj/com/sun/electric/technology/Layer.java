package com.sun.electric.technology;

import com.sun.electric.database.geometry.EGraphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;

/**
 * represents the geometric depth info and drawing style for a layer
 * of artwork in Electric.
 */
public class Layer
{
	/**
	 * Function is a typesafe enum class that describes the function of a layer.
	 */
	public static class Function
	{
		private final String name;
		private final String constantName;

		private Function(String name, String constantName)
		{
			this.name = name;
			this.constantName = constantName;
		}

		public String toString() { return name; }

		/** layer is P-type */						public static final int PTYPE =          0100;
		/** layer is N-type */						public static final int NTYPE =          0200;
		/** layer is depletion */					public static final int DEPLETION =      0400;
		/** layer is enhancement */					public static final int ENHANCEMENT =   01000;
		/** layer is light doped */					public static final int LIGHT =         02000;
		/** layer is heavy doped */					public static final int HEAVY =         04000;
		/** layer is pseudo */						public static final int PSEUDO =       010000;
		/** layer is nonelectrical */				public static final int NONELEC =      020000;
		/** layer contacts metal */					public static final int CONMETAL =     040000;
		/** layer contacts polysilicon */			public static final int CONPOLY =     0100000;
		/** layer contacts diffusion */				public static final int CONDIFF =     0200000;
		/** layer inside transistor */				public static final int INTRANS =   020000000;

		/** unknown layer */						public static final Function UNKNOWN    = new Function("unknown",    "LFUNKNOWN");
		/** metal layer 1 */						public static final Function METAL1     = new Function("metal-1",    "LFMETAL1");
		/** metal layer 2 */						public static final Function METAL2     = new Function("metal-2",    "LFMETAL2");
		/** metal layer 3 */						public static final Function METAL3     = new Function("metal-3",    "LFMETAL3");
		/** metal layer 4 */						public static final Function METAL4     = new Function("metal-4",    "LFMETAL4");
		/** metal layer 5 */						public static final Function METAL5     = new Function("metal-5",    "LFMETAL5");
		/** metal layer 6 */						public static final Function METAL6     = new Function("metal-6",    "LFMETAL6");
		/** metal layer 7 */						public static final Function METAL7     = new Function("metal-7",    "LFMETAL7");
		/** metal layer 8 */						public static final Function METAL8     = new Function("metal-8",    "LFMETAL8");
		/** metal layer 9 */						public static final Function METAL9     = new Function("metal-9",    "LFMETAL9");
		/** metal layer 10 */						public static final Function METAL10    = new Function("metal-10",   "LFMETAL10");
		/** metal layer 11 */						public static final Function METAL11    = new Function("metal-11",   "LFMETAL11");
		/** metal layer 12 */						public static final Function METAL12    = new Function("metal-12",   "LFMETAL12");
		/** polysilicon layer 1 */					public static final Function POLY1      = new Function("poly-1",     "LFPOLY1");
		/** polysilicon layer 2 */					public static final Function POLY2      = new Function("poly-2",     "LFPOLY2");
		/** polysilicon layer 3 */					public static final Function POLY3      = new Function("poly-3",     "LFPOLY3");
		/** polysilicon gate layer */				public static final Function GATE       = new Function("gate",       "LFGATE");
		/** diffusion layer */						public static final Function DIFF       = new Function("diffusion",  "LFDIFF");
		/** implant layer */						public static final Function IMPLANT    = new Function("implant",    "LFIMPLANT");
		/** contact layer 1 */						public static final Function CONTACT1   = new Function("contact-1",  "LFCONTACT1");
		/** contact layer 2 */						public static final Function CONTACT2   = new Function("contact-2",  "LFCONTACT2");
		/** contact layer 3 */						public static final Function CONTACT3   = new Function("contact-3",  "LFCONTACT3");
		/** contact layer 4 */						public static final Function CONTACT4   = new Function("contact-4",  "LFCONTACT4");
		/** contact layer 5 */						public static final Function CONTACT5   = new Function("contact-5",  "LFCONTACT5");
		/** contact layer 6 */						public static final Function CONTACT6   = new Function("contact-6",  "LFCONTACT6");
		/** contact layer 7 */						public static final Function CONTACT7   = new Function("contact-7",  "LFCONTACT7");
		/** contact layer 8 */						public static final Function CONTACT8   = new Function("contact-8",  "LFCONTACT8");
		/** contact layer 9 */						public static final Function CONTACT9   = new Function("contact-9",  "LFCONTACT9");
		/** contact layer 10 */						public static final Function CONTACT10  = new Function("contact-10", "LFCONTACT10");
		/** contact layer 11 */						public static final Function CONTACT11  = new Function("contact-11", "LFCONTACT11");
		/** contact layer 12 */						public static final Function CONTACT12  = new Function("contact-12", "LFCONTACT12");
		/** sinker (diffusion-to-buried plug) */	public static final Function PLUG       = new Function("plug",       "LFPLUG");
		/** overglass layer */						public static final Function OVERGLASS  = new Function("overglass",  "LFOVERGLASS");
		/** resistor layer */						public static final Function RESISTOR   = new Function("resistor",   "LFRESISTOR");
		/** capacitor layer */						public static final Function CAP        = new Function("capacitor",  "LFCAP");
		/** transistor layer */						public static final Function TRANSISTOR = new Function("transistor", "LFTRANSISTOR");
		/** emitter layer */						public static final Function EMITTER    = new Function("emitter",    "LFEMITTER");
		/** base layer */							public static final Function BASE       = new Function("base",       "LFBASE");
		/** collector layer */						public static final Function COLLECTOR  = new Function("collector",  "LFCOLLECTOR");
		/** substrate layer */						public static final Function SUBSTRATE  = new Function("substrate",  "LFSUBSTRATE");
		/** well layer */							public static final Function WELL       = new Function("well",       "LFWELL");
		/** guard layer */							public static final Function GUARD      = new Function("guard",      "LFGUARD");
		/** isolation layer */						public static final Function ISOLATION  = new Function("isolation",  "LFISOLATION");
		/** bus layer */							public static final Function BUS        = new Function("bus",        "LFBUS");
		/** artwork layer */						public static final Function ART        = new Function("art",        "LFART");
		/** control layer */						public static final Function CONTROL    = new Function("control",    "LFCONTROL");
	}

	private String name;
	private EGraphics graphics;
	private Function function;
	private int functionExtras;
	private String cifLayer;
	private String dxfLayer;
	private String gdsLayer;
	private String skillLayer;
	private double thickness, height;

	private Layer(String name, EGraphics graphics)
	{
		this.name = name;
		this.graphics = graphics;
	}

	public static Layer newInstance(String name, EGraphics graphics)
	{
		Layer layer = new Layer(name, graphics);
		return layer;
	}

	public String getName() { return name; }
	public EGraphics getGraphics() { return graphics; }

	public void setFunction(Function function)
	{
		this.function = function;
		this.functionExtras = 0;
	}

	public void setFunction(Function function, int functionExtras)
	{
		this.function = function;
		this.functionExtras = functionExtras;
	}
	public Function getFunction() { return function; }

	public void setHeight(double thickness, double height)
	{
		this.thickness = thickness;
		this.height = height;
	}
	public double getHeight() { return height; }
	public double getThickness() { return thickness; }

	public void setCIFLayer(String cifLayer) { this.cifLayer = cifLayer; }
	public String getCIFLayer() { return cifLayer; }

	public void setDXFLayer(String dxfLayer) { this.dxfLayer = dxfLayer; }
	public String getDXFLayer() { return dxfLayer; }

	public void setGDSLayer(String gdsLayer) { this.gdsLayer = gdsLayer; }
	public String getGDSLayer() { return gdsLayer; }

	public void setSkillLayer(String skillLayer) { this.skillLayer = skillLayer; }
	public String getSkillLayer() { return skillLayer; }

	/** printable version of this object */
	public String toString()
	{
		return "Layer " + name;
	}
}
