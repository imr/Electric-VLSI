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
	private String protoName; // Name of this prototype
	private double defaultWidth; // Default width of this wire
	private double widthOffset; // Width of other the material
	private Technology tech; // Technology of this type of arc
	private int userbits;
	private ArcLayer [] layers;

	/** these arcs are fixed-length */					public static final int WANTFIX=            01;
	/** these arcs are fixed-angle */					public static final int WANTFIXANG=         02;
	/** set if arcs should not slide in ports */		public static final int WANTCANTSLIDE=      04;
	/** set if ends do not extend by half width */		public static final int WANTNOEXTEND=      010;
	/** set if arcs should be negated */				public static final int WANTNEGATED=       020;
	/** set if arcs should be directional */			public static final int WANTDIRECTIONAL=   040;
	/** set if arcs can wipe wipable nodes */			public static final int CANWIPE=          0100;
	/** set if arcs can curve */						public static final int CANCURVE=         0200;
	/** arc function (from efunction.h) */				public static final int AFUNCTION=      017400;
	/** right shift for AFUNCTION */					public static final int AFUNCTIONSH=         8;
	/** angle increment for this type of arc */			public static final int AANGLEINC=   017760000;
	/** right shift for AANGLEINC */					public static final int AANGLEINCSH=        13;
	/** set if arc is selectable by edge, not area */	public static final int AEDGESELECT= 020000000;

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
	public ArcProto(String protoName, double defaultWidth, double widthOffset, ArcLayer [] layers, Technology tech, int userbits)
	{
		this.protoName = protoName;
		this.defaultWidth = defaultWidth;
		this.widthOffset = widthOffset;
		this.tech = tech;
		this.userbits = userbits;
		this.layers = layers;
	}

	// ------------------------ public methods -------------------------------

	public String getProtoName()
	{
		return protoName;
	}

	public double getDefaultWidth()
	{
		return defaultWidth;
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

	// text info about this ArcProto
	public String describe()
	{
		String description = "";
		if (Technology.getCurrent() != tech)
			description += tech.getTechName() + ":";
		description += protoName;
		return description;
	}

	/** Create a new ArcInst connecting two PortInsts.
	 *
	 * <p> Arcs are connected to the <i>center</i> of the PortInsts. If
	 * the two PortInst centers don't line up vertically or
	 * horizontally, then generate 'L' shaped wires by first routing
	 * horizontally from PortInst a and then vertically to PortInst b.
	 * @param width the width of this material in Lambda units.  Width
	 * must be >0. A zero width means "use the default width".
	 * @param a start PortInst
	 * @param b end PortInst */
	public ArcInst newInst(double width, PortInst a, PortInst b)
	{
		double aX = a.getCenterX();
		double aY = a.getCenterY();
		double bX = b.getCenterX();
		double bY = b.getCenterY();
		return newInst(width, a, aX, aY, b, bX, bY);
	}

	/** Create an ArcInst connecting two PortInsts at the specified
	 * locations.
	 *
	 * <p> The locations must lie within their respective ports.  The
	 * coordinates are in Lambda units.
	 *
	 * <p> This routine presents the full generality of Electric's
	 * database.  However, I've never found it to be useful. I recommend
	 * using the other newInst method. */
	public ArcInst newInst(double width,
		PortInst a, double aX, double aY,
		PortInst b, double bX, double bY)
	{
		if (width <= 0)
			width = getWidth();

		Cell aF = a.getNodeInst().getParent();
		Cell bF = b.getNodeInst().getParent();
		if (aF != bF)
		{
			System.out.println("ArcProto.newInst: the 2 PortInsts are in different Cells!");
			return null;
		}

		double wid = width + widthOffset;
		Point2D.Double rp = aF.getReferencePoint();

		double ax = aX + rp.getX();
		double ay = aY + rp.getY();
		double bx = bX + rp.getX();
		double by = bY + rp.getY();

//		ArcInst ai = Electric.newArcInst(this.getAddr(), wid,
//				a.getNodeInst().getAddr(), a.getPortProto().getAddr(), ax, ay,
//				b.getNodeInst().getAddr(), b.getPortProto().getAddr(), bx, by,
//				aF.getAddr());
		ArcInst ai = null;

		if (ai == null)
		{
			System.out.println("couldn't create arc:\n"
				+ "-------------following dimensions are Cell-Center relative---------\n"
				+ " NodeInst   A: " + a.getNodeInst() + "\n"
				+ " PortProto  A: " + a.getPortProto() + "\n"
				+ " PortBounds A: " + a.getBounds() + "\n"
				+ " NodeInst   B: " + b.getNodeInst() + "\n"
				+ " PortProto  B: " + b.getPortProto() + "\n"
				+ " PortBounds B: " + b.getBounds() + "\n"
				+ "---------------------following dimensions are absolute-------------\n"
				+ " (ax, ay): (" + ax + "," + ay + ")\n"
				+ " (bx, by): (" + bx + "," + by + ")\n");
			return null;
		}
		return ai;
	}

	/** Find the PrimitiveNode pin corresponding to this ArcProto type.
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

	/** printable version of this object */
	public String toString()
	{
		return "ArcProto: " + protoName;
	}

}
