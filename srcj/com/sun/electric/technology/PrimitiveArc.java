package com.sun.electric.technology;

import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Iterator;

/**
 * The PrimitiveArc class contains basic information about an arc type.
 * There is only one PrimitiveArc object for each type of wire or arc.
 */
public class PrimitiveArc extends ArcProto
{
	// ----------------------- private data -------------------------------

	/** function of this arc */							private Function function;
	/** Layers in this arc */							private Technology.ArcLayer [] layers;

	// ----------------- protected and private methods -------------------------

	private PrimitiveArc(Technology tech, String protoName, double defaultWidth, Technology.ArcLayer [] layers)
	{
		this.protoName = protoName;
		this.defaultWidth = defaultWidth;
		this.widthOffset = 0;
		this.tech = tech;
		this.layers = layers;
	}

	// ------------------------ public methods -------------------------------

	public static PrimitiveArc newInstance(Technology tech, String protoName, double defaultWidth, Technology.ArcLayer [] layers)
	{
		// check the arguments
		if (tech.findArcProto(protoName) != null)
		{
			System.out.println("Error: technology " + tech.getTechName() + " has multiple arcs named " + protoName);
			return null;
		}
		if (defaultWidth < 0.0)
		{
			System.out.println("PrimitiveArc " + tech.getTechName() + ":" + protoName + " has negative width");
			return null;
		}

		PrimitiveArc ap = new PrimitiveArc(tech, protoName, defaultWidth, layers);
		tech.addArcProto(ap);
		return ap;
	}

	public Technology.ArcLayer [] getLayers() { return layers; }

	/**
	 * Set the default width of this type of arc.
	 */
	public void setWidthOffset(double widthOffset)
	{
		if (widthOffset < 0.0)
		{
			System.out.println("PrimitiveArc " + tech.getTechName() + ":" + protoName + " has negative width offset");
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

	/**
	 * Find the PrimitiveNode pin corresponding to this PrimitiveArc type.
	 * For example, if this PrimitiveArc is metal-1 then return the metal-1-pin.
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
		return "PrimitiveArc " + describe();
	}

}
