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
	// ----------------------- private data -------------------------------

	/** Name of this prototype */						private String protoName;
	/** Default width of this wire */					private double defaultWidth;
	/** Width of other the material */					private double widthOffset;
	/** Technology of this type of arc */				private Technology tech;
	/** Flags for this arc */							private int userBits;
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

	// ----------------------- PUBLIC CONSTANTS -------------------------------

	// Arc functions:
	/** arc is unknown type */							public static final int APUNKNOWN=           0;
	/** arc is metal, layer 1 */						public static final int APMETAL1=            1;
	/** arc is metal, layer 2 */						public static final int APMETAL2=            2;
	/** arc is metal, layer 3 */						public static final int APMETAL3=            3;
	/** arc is metal, layer 4 */						public static final int APMETAL4=            4;
	/** arc is metal, layer 5 */						public static final int APMETAL5=            5;
	/** arc is metal, layer 6 */						public static final int APMETAL6=            6;
	/** arc is metal, layer 7 */						public static final int APMETAL7=            7;
	/** arc is metal, layer 8 */						public static final int APMETAL8=            8;
	/** arc is metal, layer 9 */						public static final int APMETAL9=            9;
	/** arc is metal, layer 10 */						public static final int APMETAL10=          10;
	/** arc is metal, layer 11 */						public static final int APMETAL11=          11;
	/** arc is metal, layer 12 */						public static final int APMETAL12=          12;
	/** arc is polysilicon, layer 1 */					public static final int APPOLY1=            13;
	/** arc is polysilicon, layer 2 */					public static final int APPOLY2=            14;
	/** arc is polysilicon, layer 3 */					public static final int APPOLY3=            15;
	/** arc is diffusion */								public static final int APDIFF=             16;
	/** arc is P-type diffusion */						public static final int APDIFFP=            17;
	/** arc is N-type diffusion */						public static final int APDIFFN=            18;
	/** arc is substrate diffusion */					public static final int APDIFFS=            19;
	/** arc is well diffusion */						public static final int APDIFFW=            20;
	/** arc is multi-wire bus */						public static final int APBUS=              21;
	/** arc is unrouted specification */				public static final int APUNROUTED=         22;
	/** arc is nonelectrical */							public static final int APNONELEC=          23;

	// ----------------- protected and private methods -------------------------

	private ArcProto(Technology tech, String protoName, double defaultWidth, Technology.ArcLayer [] layers)
	{
		this.protoName = protoName;
		this.defaultWidth = defaultWidth;
		this.widthOffset = 0;
		this.tech = tech;
		this.userBits = 0;
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

	public String getProtoName()
	{
		return protoName;
	}

	public double getDefaultWidth()
	{
		return defaultWidth;
	}

	/**
	 * Set the default width of this type of arc in Lambda units.
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
	/** Get the default width of this type of arc in Lambda units.
	 *
	 * <p> Exclude the surrounding material. For example, diffusion arcs
	 * are always accompanied by a surrounding well and select. However,
	 * this call returns only the width of the diffusion. */
	public double getWidthOffset()
	{
		return widthOffset;
	}

	/** Get the default width of this type of arc in Lambda units.
	 *
	 * <p> Exclude the surrounding material. For example, diffusion arcs
	 * are always accompanied by a surrounding well and select. However,
	 * this call returns only the width of the diffusion. */
	public double getWidth()
	{
		return defaultWidth - widthOffset;
	}

	public Technology getTechnology()
	{
		return tech;
	}

	/** Set the Rigid bit */
	public void setRigid() { userBits |= WANTFIX; }
	/** Clear the Rigid bit */
	public void clearRigid() { userBits &= ~WANTFIX; }
	/** Get the Rigid bit */
	public boolean getRigid() { return (userBits & WANTFIX) != 0; }

	/** Set the Fixed-angle bit */
	public void setFixedAngle() { userBits |= WANTFIXANG; }
	/** Clear the Fixed-angle bit */
	public void clearFixedAngle() { userBits &= ~WANTFIXANG; }
	/** Get the Fixed-angle bit */
	public boolean getFixedAngle() { return (userBits & WANTFIXANG) != 0; }

	/** Set the Slidable bit */
	public void setSlidable() { userBits &= ~WANTCANTSLIDE; }
	/** Clear the Slidable bit */
	public void clearSlidable() { userBits |= WANTCANTSLIDE; }
	/** Get the Slidable bit */
	public boolean getSlidable() { return (userBits & WANTCANTSLIDE) == 0; }

	/** Set the End-extension bit */
	public void setEndExtend() { userBits &= ~WANTCANTSLIDE; }
	/** Clear the End-extension bit */
	public void clearEndExtend() { userBits |= WANTCANTSLIDE; }
	/** Get the End-extension bit */
	public boolean getEndExtend() { return (userBits & WANTCANTSLIDE) == 0; }

	/** Set the Negated bit */
	public void setNegated() { userBits |= WANTNEGATED; }
	/** Clear the Negated bit */
	public void clearNegated() { userBits &= ~WANTNEGATED; }
	/** Get the Negated bit */
	public boolean getNegated() { return (userBits & WANTNEGATED) != 0; }

	/** Set the Directional bit */
	public void setDirectional() { userBits |= WANTDIRECTIONAL; }
	/** Clear the Directional bit */
	public void clearDirectional() { userBits &= ~WANTDIRECTIONAL; }
	/** Get the Directional bit */
	public boolean getDirectional() { return (userBits & WANTDIRECTIONAL) != 0; }

	/** Set the Can-Wipe bit */
	public void setCanWipe() { userBits |= CANWIPE; }
	/** Clear the Can-Wipe bit */
	public void clearCanWipe() { userBits &= ~CANWIPE; }
	/** Get the Can-Wipe bit */
	public boolean getCanWipe() { return (userBits & CANWIPE) != 0; }

	/** Set the Can-Curve bit */
	public void setCanCurve() { userBits |= CANCURVE; }
	/** Clear the Can-Curve bit */
	public void clearCanCurve() { userBits &= ~CANCURVE; }
	/** Get the Can-Curve bit */
	public boolean getCanCurve() { return (userBits & CANCURVE) != 0; }

	/** Set the Edge-Select bit */
	public void setEdgeSelect() { userBits |= AEDGESELECT; }
	/** Clear the Edge-Select bit */
	public void cleardgeSelect() { userBits &= ~AEDGESELECT; }
	/** Get the Edge-Select bit */
	public boolean getdgeSelect() { return (userBits & AEDGESELECT) != 0; }

	/** Set the arc function */
	public void setFunction(int function) { userBits = (userBits & ~AFUNCTION) | (function << AFUNCTIONSH); }
	/** Get the arc function */
	public int getFunction() { return (userBits & AFUNCTION) >> AFUNCTIONSH; }

	/** Set the arc angle increment */
	public void setAngleIncrement(int function) { userBits = (userBits & ~AANGLEINC) | (function << AANGLEINCSH); }
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
		return "ArcProto: " + protoName;
	}

}
