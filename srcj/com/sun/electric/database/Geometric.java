package com.sun.electric.database;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * This class is the superclass for all Electric classes that have visual
 * bounds on the screen.  These include NodeInst and ArcInst, but NOT
 * NodeProto and its ilk, which have size, but aren't visual objects on
 * the screen.
 *
 * The methods in this class will take care of geometry caching: what
 * were R-trees in Electric may turn out to be many-to-many rectangular
 * buckets on the Java side.
 */
public class Geometric extends ElectricObject
{
	// ------------------------------- private data ------------------------------
	private static final SizeOffset ZERO_SIZE_OFFSET =
		new SizeOffset(0, 0, 0, 0);

	protected Cell parent; // Cell containing this Geometric object
	private Rectangle visBounds; // excludes Electric's invisible material

	// Jose's internal representation of position and orientation is the
	// 2D transformation matrix:
	// --                                         --
	// |   sX cos(angleJ)   -sY sin(angleJ)    dX  |
	// |   sX sin(angleJ)    sY cos(angleJ)    dY  |
	// |         0                  0           1  |
	// --                                         --
	// See SML# 2003-0379 for description of Jose position mathematics.
	//
	// NOTE: this information is filled in by NodeInst.updateAspect and
	// ArcInst.updateEnds
	protected double dX, dY;
	protected double sX, sY;
	protected double angle; // Jose's and Electric's angles may differ
	protected double cos, sin; // of angleJ

	// ------------------------ private and protected methods--------------------

	// create a new geometric object
	protected Geometric()
	{
	}

	// INITIALIZE the geometric aspect of this electric thing.
//	protected void init(Cell parent)
//	{
//		this.parent = parent;
//	}

	// The prototype of this Geometric object is either an ArcProto or
	// NodeProto.
	private Object getProto()
	{
		if (this instanceof NodeInst)
			return ((NodeInst) this).getProto();
		else
			return ((ArcInst) this).getProto();
	}

	private Rectangle2D.Double getProtoElecBounds()
	{
		Object proto = getProto();
		if (proto instanceof NodeProto)
			return ((NodeProto) proto).getElecBounds();
		else
			return new Rectangle2D.Double(0, 0, 0, 0);
	}

	private SizeOffset getProtoSizeOffset()
	{
		if (this instanceof ArcInst)
			return ZERO_SIZE_OFFSET;
		return ((NodeProto) getProto()).getSizeOffset();
	}

	Technology getProtoTechnology()
	{
		Object proto = getProto();
		if (proto instanceof NodeProto)
			return ((NodeProto) proto).getTechnology();
		else
			return ((ArcProto) proto).getTechnology();
	}

	/**
	 * Return width and height of this geometric instance after scaling
	 * but before rotation.  This is only needed to properly size ortProto's.
	 */
	Dimension getUnrotatedElecDimensions()
	{
		Rectangle2D.Double pbounds = getProtoElecBounds();
		return new Dimension(
			round(pbounds.width * sX),
			round(pbounds.height * sY));
	}

	/** Something changed, and the bounds of this object may have to
	 * be recalculated.  don't do it yet... */
	void voidBounds()
	{
		visBounds = null;
	}

	// Tricky.  Electric's nonexistant "invisible" surround doesn't
	// scale; it remains the same width regardless of the size of the
	// instance.  However, if I simply transform the visible part of the
	// proto then I'll end up scaling the invisible part to. To
	// circumvent this, first compute the quasi-visible portion by with
	// an unscaled invisible part and then scale the quasi-visible
	// portion.
	//
	// Tricky: We must transform all four corners of the Prototype's
	// bounding box for this to work on rotations by other than
	// multiples of 90 degrees. 
	private Rectangle calcVisBounds()
	{
		Rectangle2D.Double eb = null;
		if (getProto() instanceof Cell)
		{
			eb = ((Cell) getProto()).findEssentialBounds();
		}
		Poly poly = null;
		if (eb != null)
		{
			// NodeInst of Cell with essential-bounds. Use Essential-Bounds
			// intead of Electric's bounds.
			poly = new Poly(null, eb.x, eb.y, eb.width, eb.height);
		} else
		{
			// Hide invisible material included in Electric's bounds
			Rectangle2D.Double r = getProtoElecBounds();
			SizeOffset o = getProtoSizeOffset();

			// RKao debug
//			System.out.println("calcVisBounds: elecBounds: "+r);
//			System.out.println("sX, sY: "+sX+" "+sY);
//			System.out.println(" Internal: sX="+sX+", sY="+sY+", dX="+dX+", dY="+
//					 dY+", cos="+cos+", sin="+sin);

			double asX = Math.abs(sX);
			double asY = Math.abs(sY);
			double x = r.x + o.lx / asX;
			double y = r.y + o.ly / asY;
			double w = r.width - o.lx / asX - o.hx / asX;
			double h = r.height - o.ly / asY - o.hy / asY;
			poly = new Poly(null, x, y, w, h);
		}
		return xformPoly(poly).getBounds();
	}

	Point2D.Double getOriginBase()
	{
		return new Point2D.Double(dX, dY);
	}

	/** remove this geometric thing */
	void remove()
	{
	}

	protected void getInfo()
	{
		System.out.println(" Parent: " + getParent());
		Rectangle2D b = getBounds();
		System.out.println(" Bounds: " + b.getX() + "," + b.getY() + ", " + b.getWidth() + "x" + b.getHeight());
		System.out.println(" Internal: sX=" + sX + ", sY=" + sY + ", dX=" + dX + ", dY=" + dY + ", cos=" + cos + ", sin=" + sin);
		super.getInfo();
	}

	// ------------------------ public methods -----------------------------

	/** Get the bounds of this object in the coordinate system of the
	 * Cell containing this object.  Exclude the nonexistant
	 * "invisible" materials that surround the visible materials. Lambda
	 * units.
	 *
	 * <p> If this is a NodeInst of a Cell and that Cell has instances
	 * of two or more Essential-Bounds PrimitiveNodes, then return the
	 * largest bounding box that includes all the Essential-Bounds
	 * instances.
	 *
	 * <p> Note that getBounds() for ArcInsts never worked right.
	 * However I've never needed it and I'm not sure if anyone ever
	 * will.
	 */
	public Rectangle2D getBounds()
	{
		if (visBounds == null)
			visBounds = calcVisBounds();

		Point2D rp = parent.getReferencePoint();
		return new Rectangle2D.Double(
			visBounds.x - rp.getX(),
			visBounds.y - rp.getY(),
			visBounds.width,
			visBounds.height);
	}

	/** Cell containing this Geometric object */
	public Cell getParent()
	{
		return parent;
	}

	/** Get X scale of this object with respect to the prototype. */
	public double getScaleX()
	{
		// Modify scale so it looks like we're only scaling the visible
		// portion of the NodeProto.
		SizeOffset o = getProtoSizeOffset();
		double invisW = o.lx + o.hx;
		double defW = getProtoElecBounds().width;
		double scale = (Math.abs(sX) * defW - invisW) / (defW - invisW);
		return (sX < 0) ? -scale : scale;
	}

	/** Get Y scale of this object with respect to the prototype. */
	public double getScaleY()
	{
		// Modify scale so it looks like we're only scaling the visible
		// portion of the NodeProto.
		SizeOffset o = getProtoSizeOffset();
		double invisH = o.ly + o.hy;
		double defH = getProtoElecBounds().height;
		double scale = (Math.abs(sY) * defH - invisH) / (defH - invisH);
		return sY < 0 ? -scale : scale;
	}

	/** Get X coordinate of this object's origin.  If this object is a
	 * NodeInst then get the X coordinate of the NodeProto's
	 * reference point. Lambda units. */
	public double getX()
	{
		if (this instanceof NodeInst)
		{
			return ((NodeInst) this).getReferencePoint().getX();
		} else
		{
			return dX - parent.getReferencePoint().getX();
		}
	}

	/** Get Y coordinate of this object's origin.  If this object is a
	 * NodeInst then get the Y coordinate of the NodeProto's
	 * reference point. Lambda units. */
	public double getY()
	{
		if (this instanceof NodeInst)
		{
			return ((NodeInst) this).getReferencePoint().getY();
		} else
		{
			return dY - parent.getReferencePoint().getY();
		}
	}

	/** get angle of rotation in degrees */
	public double getAngle()
	{
		return angle;
	}

	/** Transform a Poly shape representing a piece of this instance's
	 * prototype into its final coordinates with respect to this instance's
	 * Cell. */
	public Poly xformPoly(Poly src)
	{
		return src.transform(sX, sY, dX, dY, cos, sin);
	}

	/** get the size of a lambda, in half-millimicrons (base units) for
	 * this geometric thing */
	public double getLambdaSize()
	{
		return getParent().getLibrary().getLambdaSize(getProtoTechnology());
	}

	/** Transform, without scaling, a Poly shape representing a piece
	 * of this instance's prototype into its final coordinates with
	 * respect to this instance's Cell. This is needed because of the
	 * way that PrimitivePorts implement their shapes. */
	public Poly xformPolyNoScale(Poly src)
	{
		return src.transform(dX, dY, cos, sin);
	}

}
