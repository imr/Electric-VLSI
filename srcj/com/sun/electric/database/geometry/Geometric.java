package com.sun.electric.database.geometry;

import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.hierarchy.Cell;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

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

	// The internal representation of position and orientation is the
	// 2D transformation matrix:
	// --                                       --
	// |   sX cos(angleJ)    sY sin(angleJ)   0  |
	// |  -sX sin(angleJ)    sY cos(angleJ)   0  |
	// |        cX                cY          1  |
	// --                                       --

	/** Cell containing this Geometric object */	protected Cell parent;
	/** bounds after transformation */				private Rectangle2D.Double visBounds;
	/** center coordinate of this geometric */		protected double cX, cY;
	/** size of this geometric */					protected double sX, sY;
	/** angle of this geometric */					protected double angle;

	// ------------------------ private and protected methods--------------------

	// create a new geometric object
	protected Geometric()
	{
	}

	protected void setParent(Cell parent)
	{
		this.parent = parent;
	}

	/** remove this geometric thing */
	public void remove()
	{
	}

	protected void getInfo()
	{
		System.out.println(" Parent: " + parent.describe());
		System.out.println(" Location: (" + cX + "," + cY + "), size: " + sX + "x" + sY + ", rotated " + angle * 180.0 / Math.PI);
		System.out.println(" Bounds: (" + visBounds.getCenterX() + "," + visBounds.getCenterY() + "), size: " +
			visBounds.getWidth() + "x" + visBounds.getHeight());
	}

	private AffineTransform rotateTranspose = null;

	public AffineTransform rotateAbout(double angle, double sX, double sY, double cX, double cY)
	{
		AffineTransform transform = new AffineTransform();
		if (sX < 0 || sY < 0)
		{
			// must do transposition, so it is trickier
			double cosine = Math.cos(angle);
			double sine = Math.sin(angle);
			if (rotateTranspose == null) rotateTranspose = new AffineTransform();
			rotateTranspose.setTransform(-sine, -cosine, -cosine, sine, 0.0, 0.0);
			transform.setToTranslation(cX, cY);
			transform.concatenate(rotateTranspose);
			transform.translate(-cX, -cY);
		} else
		{
			transform.setToRotation(angle, cX, cY);
		}
		return transform;
	}
	
	public void updateGeometricBounds()
	{
		// start with a unit polygon, centered at the origin
		Poly poly = new Poly(0.0, 0.0, 1.0, 1.0);

		// transform by the relevant amount
		AffineTransform scale = new AffineTransform();
		scale.setToScale(sX, sY);
		AffineTransform rotate = rotateAbout(angle, sX, sY, 0, 0);
		AffineTransform translate = new AffineTransform();
		translate.setToTranslation(cX, cY);
		rotate.concatenate(scale);
		translate.concatenate(rotate);

		poly.transform(translate);

		// return its bounds
		visBounds = poly.getBounds2DDouble();
	}

	// ------------------------ public methods -----------------------------

	/** Cell containing this Geometric object */
	public Cell getParent() { return parent; }

	Point2D.Double getCenter()
	{
		return new Point2D.Double(cX, cY);
	}

	/** Get X coordinate of this object's origin.  If this object is a
	 * NodeInst then get the X coordinate of the NodeProto's
	 * reference point. */
	public double getCenterX() { return cX; }

	/** Get Y coordinate of this object's origin.  If this object is a
	 * NodeInst then get the Y coordinate of the NodeProto's
	 * reference point. */
	public double getCenterY() { return cY; }

	/** get angle of rotation in degrees */
	public double getAngle() { return angle; }
	public double getXSize() { return Math.abs(sX); }
	public double getYSize() { return Math.abs(sY); }
	public Rectangle2D getBounds() { return visBounds; }

}
