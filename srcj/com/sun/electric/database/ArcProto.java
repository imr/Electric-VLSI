package com.sun.electric.database;

import com.sun.electric.technologies.*;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Iterator;

/**
 * The ArcProto class contains basic information about an arc type.
 * There is only one ArcProto object for each type of wire or arc.
 */
public class ArcProto extends ElectricObject
{
	/**
	 * ArcProto.Function is a typesafe enum class that describes the function of an arcproto.
	 */
	static public class Function
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

	/** Name of this prototype */						private String protoName;
	/** Default width of this wire */					private double defaultWidth;
	/** Width of other the material */					private double widthOffset;
	/** Technology of this type of arc */				private Technology tech;
	/** Flags for this arc */							private int userBits;
	/** function of this arc */							private Function function;
	/** Layers in this arc */							private Technology.ArcLayer [] layers;

	// the meaning of the "userBits" field:
	/** these arcs are fixed-length */					private static final int WANTFIX=            01;
	/** these arcs are fixed-angle */					private static final int WANTFIXANG=         02;
	/** set if arcs should not slide in ports */		private static final int WANTCANTSLIDE=      04;
	/** set if ends do not extend by half width */		private static final int WANTNOEXTEND=      010;
	/** set if arcs should be negated */				private static final int WANTNEGATED=       020;
	/** set if arcs should be directional */			private static final int WANTDIRECTIONAL=   040;
	/** set if arcs can wipe wipable nodes */			private static final int CANWIPE=          0100;
	/** set if arcs can curve */						private static final int CANCURVE=         0200;
	/** arc function (from efunction.h) */				private static final int AFUNCTION=      017400;
	/** right shift for AFUNCTION */					private static final int AFUNCTIONSH=         8;
	/** angle increment for this type of arc */			private static final int AANGLEINC=   017760000;
	/** right shift for AANGLEINC */					private static final int AANGLEINCSH=        13;
	/** set if arc is selectable by edge, not area */	private static final int AEDGESELECT= 020000000;

	// ----------------- protected and private methods -------------------------

	private ArcProto(Technology tech, String protoName, double defaultWidth, Technology.ArcLayer [] layers)
	{
		this.protoName = protoName;
		this.defaultWidth = defaultWidth;
		this.widthOffset = 0;
		this.tech = tech;
		this.userBits = 0;
		this.function = Function.UNKNOWN;
		this.layers = layers;
	}

	// ------------------------ public methods -------------------------------

	public static ArcProto newInstance(Technology tech, String protoName, double defaultWidth, Technology.ArcLayer [] layers)
	{
		// check the arguments
		if (tech.findArcProto(protoName) != null)
		{
			System.out.println("Technology " + tech.getTechName() + " has multiple arcs named " + protoName);
			return null;
		}
		if (defaultWidth < 0.0)
		{
			System.out.println("ArcProto " + tech.getTechName() + ":" + protoName + " has negative width");
			return null;
		}

		ArcProto ap = new ArcProto(tech, protoName, defaultWidth, layers);
		tech.addArcProto(ap);
		return ap;
	}

	public String getProtoName() { return protoName; }

	public Technology getTechnology() { return tech; }

	public double getDefaultWidth() { return defaultWidth; }

	/**
	 * Set the default width of this type of arc.
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
	/** Get the default width of this type of arc.
	 *
	 * <p> Exclude the surrounding material. For example, diffusion arcs
	 * are always accompanied by a surrounding well and select. However,
	 * this call returns only the width of the diffusion. */
	public double getWidthOffset() { return widthOffset; }

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
	public void setExtended() { userBits &= ~WANTCANTSLIDE; }
	/** Clear the End-extension bit */
	public void clearExtended() { userBits |= WANTCANTSLIDE; }
	/** Get the End-extension bit */
	public boolean isExtended() { return (userBits & WANTCANTSLIDE) == 0; }

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

	/**
	 * Find the PrimitiveNode pin corresponding to this ArcProto type.
	 * For example, if this ArcProto is metal-1 then return the metal-1 pin.
	 * @return the PrimitiveNode pin that matches, or null if there is no match
	 */
	public PrimitiveNode findPinProto()
	{
		Iterator it = tech.getNodeIterator();
		while (it.hasNext())
		{
			PrimitiveNode pn = (PrimitiveNode) it.next();
			if (pn.isPin())
			{
				PrimitivePort pp = (PrimitivePort) pn.getPorts().next();
				Iterator types = pp.getConnectionTypes();
				while (types.hasNext())
				{
					if (types.next() == this)
						return pn;
				}
			}
		}
		return null;
	}

	/** returns a string that describes this ArcProto */
	public String describe()
	{
		String description = "";
		if (Technology.getCurrent() != tech)
			description += tech.getTechName() + ":";
		description += protoName;
		return description;
	}

	/** printable version of this object */
	public String toString()
	{
		return "ArcProto " + describe();
	}

}
