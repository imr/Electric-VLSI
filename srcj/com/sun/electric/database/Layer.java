package com.sun.electric.database;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.util.HashMap;

/**
 * represents the geometric depth info and drawing style for a layer
 * of artwork in Electric.
 */
public class Layer
{
	private String name;
	private Graphics graphics;
	private int function;
	private String cifLayer;
	private String gdsLayer;
	private String skillLayer;
	private double thickness, height;

//	private Paint p;
//	private int style;
//	private static HashMap cache = new HashMap();

	public static final int OUTLINE = 1;
	public static final int FILL = 2;
//********************** SHOULD USE ENUMERATION
	/** number of layers below */				public static final int LFNUMLAYERS=       044;
	/** unknown layer */						public static final int LFUNKNOWN=           0;
	/** metal layer 1 */						public static final int LFMETAL1=           01;
	/** metal layer 2 */						public static final int LFMETAL2=           02;
	/** metal layer 3 */						public static final int LFMETAL3=           03;
	/** metal layer 4 */						public static final int LFMETAL4=           04;
	/** metal layer 5 */						public static final int LFMETAL5=           05;
	/** metal layer 6 */						public static final int LFMETAL6=           06;
	/** metal layer 7 */						public static final int LFMETAL7=           07;
	/** metal layer 8 */						public static final int LFMETAL8=          010;
	/** metal layer 9 */						public static final int LFMETAL9=          011;
	/** metal layer 10 */						public static final int LFMETAL10=         012;
	/** metal layer 11 */						public static final int LFMETAL11=         013;
	/** metal layer 12 */						public static final int LFMETAL12=         014;
	/** polysilicon layer 1 */					public static final int LFPOLY1=           015;
	/** polysilicon layer 2 */					public static final int LFPOLY2=           016;
	/** polysilicon layer 3 */					public static final int LFPOLY3=           017;
	/** polysilicon gate layer */				public static final int LFGATE=            020;
	/** diffusion layer */						public static final int LFDIFF=            021;
	/** implant layer */						public static final int LFIMPLANT=         022;
	/** contact layer 1 */						public static final int LFCONTACT1=        023;
	/** contact layer 2 */						public static final int LFCONTACT2=        024;
	/** contact layer 3 */						public static final int LFCONTACT3=        025;
	/** contact layer 4 */						public static final int LFCONTACT4=        026;
	/** contact layer 5 */						public static final int LFCONTACT5=        027;
	/** contact layer 6 */						public static final int LFCONTACT6=        030;
	/** contact layer 7 */						public static final int LFCONTACT7=        031;
	/** contact layer 8 */						public static final int LFCONTACT8=        032;
	/** contact layer 9 */						public static final int LFCONTACT9=        033;
	/** contact layer 10 */						public static final int LFCONTACT10=       034;
	/** contact layer 11 */						public static final int LFCONTACT11=       035;
	/** contact layer 12 */						public static final int LFCONTACT12=       036;
	/** sinker (diffusion-to-buried plug) */	public static final int LFPLUG=            037;
	/** overglass layer */						public static final int LFOVERGLASS=       040;
	/** resistor layer */						public static final int LFRESISTOR=        041;
	/** capacitor layer */						public static final int LFCAP=             042;
	/** transistor layer */						public static final int LFTRANSISTOR=      043;
	/** emitter layer */						public static final int LFEMITTER=         044;
	/** base layer */							public static final int LFBASE=            045;
	/** collector layer */						public static final int LFCOLLECTOR=       046;
	/** substrate layer */						public static final int LFSUBSTRATE=       047;
	/** well layer */							public static final int LFWELL=            050;
	/** guard layer */							public static final int LFGUARD=           051;
	/** isolation layer */						public static final int LFISOLATION=       052;
	/** bus layer */							public static final int LFBUS=             053;
	/** artwork layer */						public static final int LFART=             054;
	/** control layer */						public static final int LFCONTROL=         055;

	/** all above layers */						public static final int LFTYPE=            077;
	/** layer is P-type */						public static final int LFPTYPE=          0100;
	/** layer is N-type */						public static final int LFNTYPE=          0200;
	/** layer is depletion */					public static final int LFDEPLETION=      0400;
	/** layer is enhancement */					public static final int LFENHANCEMENT=   01000;
	/** layer is light doped */					public static final int LFLIGHT=         02000;
	/** layer is heavy doped */					public static final int LFHEAVY=         04000;
	/** layer is pseudo */						public static final int LFPSEUDO=       010000;
	/** layer is nonelectrical */				public static final int LFNONELEC=      020000;
	/** layer contacts metal */					public static final int LFCONMETAL=     040000;
	/** layer contacts polysilicon */			public static final int LFCONPOLY=     0100000;
	/** layer contacts diffusion */				public static final int LFCONDIFF=     0200000;
	/** layer inside transistor */				public static final int LFINTRANS=   020000000;

	private Layer(String name, Graphics graphics)
	{
		this.name = name;
		this.graphics = graphics;
	}

	public static Layer newLayer(String name, Graphics graphics)
	{
		Layer layer = new Layer(name, graphics);
		return layer;
	}

	public void setLayerFunction(int function)
	{
		this.function = function;
	}

	public void setLayerHeight(double thickness, double height)
	{
		this.thickness = thickness;
		this.height = height;
	}

	public void setCIFLayer(String cifLayer)
	{
		this.cifLayer = cifLayer;
	}

	public void setGDSLayer(String gdsLayer)
	{
		this.gdsLayer = gdsLayer;
	}

	public void setSkillLayer(String skillLayer)
	{
		this.skillLayer = skillLayer;
	}
	/**
	 * Find a layer with a particular name.  This method will create a
	 * random style for the layer if the layer doesn't already exist.
	 */
//	public static Layer findLayer(String name)
//	{
//		Layer l = (Layer) cache.get(name);
//		if (l == null)
//		{
//			if (name.equals("port"))
//			{
//				l = new Layer(name, Color.black, OUTLINE);
//			} else
//			{
//				l =
//					new Layer(
//						name,
//						new Color(
//							(float) Math.random(),
//							(float) Math.random(),
//							(float) Math.random(),
//							0.2f),
//						FILL);
//			}
//			cache.put(name, l);
//		}
//		return l;
//	}

	/**
	 * Find a layer iwth a particular name, assigning a specific style
	 * if the layer doesn't already exist.
	 */
//	public static Layer findLayer(
//		String name,
//		float dr,
//		float dg,
//		float db,
//		float da,
//		int defaultStyle)
//	{
//		Layer l = (Layer) cache.get(name);
//		if (l == null)
//		{
//			l = new Layer(name, new Color(dr, dg, db, da), defaultStyle);
//			cache.put(name, l);
//		}
//		return l;
//	}

	/**
	 * Find a layer iwth a particular name, assigning a specific style
	 * if the layer doesn't already exist.
	 */
//	public static Layer findLayer(String name, Paint paint, int style)
//	{
//		Layer l = (Layer) cache.get(name);
//		if (l == null)
//		{
//			l = new Layer(name, paint, style);
//			cache.put(name, l);
//		}
//		return l;
//	}

	/**
	 * Paint a shape into a particular Graphics2D, using this layer's style
	 */
//	public void paint(Graphics2D g, Shape s)
//	{
//		g.setPaint(p);
//		if (style == FILL)
//		{
//			g.fill(s);
//		}
//		if (style == OUTLINE)
//		{
//			g.draw(s);
//		}
//	}
}
