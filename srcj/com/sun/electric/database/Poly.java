package com.sun.electric.database;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Represents a transformable Polygon with floating-point
 * coordinates, and a specific Layer. */
public class Poly implements Shape
{
	/**
	 * Function is a typesafe enum class that describes the function of an arcproto.
	 */
	static public class Type
	{
		private Type()
		{
		}

		public String toString() { return "Polygon type"; }

		// polygons ************
		/** closed polygon, filled in */					public static final Type FILLED=         new Type();
		/** closed polygon, outline  */						public static final Type CLOSED=         new Type();
		// rectangles ************
		/** closed rectangle, filled in */					public static final Type FILLEDRECT=     new Type();
		/** closed rectangle, outline */					public static final Type CLOSEDRECT=     new Type();
		/** closed rectangle, outline crossed */			public static final Type CROSSED=        new Type();
		// lines ************
		/** open outline, solid */							public static final Type OPENED=         new Type();
		/** open outline, dotted */							public static final Type OPENEDT1=       new Type();
		/** open outline, dashed  */						public static final Type OPENEDT2=       new Type();
		/** open outline, thicker */						public static final Type OPENEDT3=       new Type();
		/** open outline pushed by 1 */						public static final Type OPENEDO1=       new Type();
		/** vector endpoint pairs, solid */					public static final Type VECTORS=        new Type();
		// curves ************
		/** circle at [0] radius to [1] */					public static final Type CIRCLE=         new Type();
		/** thick circle at [0] radius to [1] */			public static final Type THICKCIRCLE=    new Type();
		/** filled circle */								public static final Type DISC=           new Type();
		/** arc of circle at [0] ends [1] and [2] */		public static final Type CIRCLEARC=      new Type();
		/** thick arc of circle at [0] ends [1] and [2] */	public static final Type THICKCIRCLEARC= new Type();
		// text ************
		/** text at center */								public static final Type TEXTCENT=       new Type();
		/** text below top edge */							public static final Type TEXTTOP=        new Type();
		/** text above bottom edge */						public static final Type TEXTBOT=        new Type();
		/** text to right of left edge */					public static final Type TEXTLEFT=       new Type();
		/** text to left of right edge */					public static final Type TEXTRIGHT=      new Type();
		/** text to lower-right of top-left corner */		public static final Type TEXTTOPLEFT=    new Type();
		/** text to upper-right of bottom-left corner */	public static final Type TEXTBOTLEFT=    new Type();
		/** text to lower-left of top-right corner */		public static final Type TEXTTOPRIGHT=   new Type();
		/** text to upper-left of bottom-right corner */	public static final Type TEXTBOTRIGHT=   new Type();
		/** text that fits in box (may shrink) */			public static final Type TEXTBOX=        new Type();
		// miscellaneous ************
		/** grid dots in the window */						public static final Type GRIDDOTS=       new Type();
		/** cross */										public static final Type CROSS=          new Type();
		/** big cross */									public static final Type BIGCROSS=       new Type();
	}

	private Layer layer;
	private double xpts[], ypts[];
	private Rectangle2D.Double bounds;
	private int style;

	/* text font sizes (in VARIABLE, NODEINST, PORTPROTO, and POLYGON->textdescription) */
	/** points from 1 to TXTMAXPOINTS */					public static final int TXTPOINTS=        077;
	/** right-shift of TXTPOINTS */							public static final int TXTPOINTSSH=        0;
	public static final int TXTMAXPOINTS=      63;
//	public static final int TXTSETPOINTS(p)   ((p)<<TXTPOINTSSH)
//	public static final int TXTGETPOINTS(p)   (((p)&TXTPOINTS)>>TXTPOINTSSH)

	public static final int TXTQGRID=    077700;		
	public static final int TXTQGRIDSH=       6;		
	public static final int TXTMAXQGRID=    511;
//	public static final int TXTSETQGRID(ql) ((ql)<<TXTQGRIDSH)
//	public static final int TXTGETQGRIDql) (((ql)&TXTQGRID)>>TXTQGRIDSH)
	/** fixed-width text for text editing */			public static final int TXTEDITOR=     077770;
	/** text for menu selection */						public static final int TXTMENU=       077771;

	/** Paint this Poly */
//	public void paint(Graphics2D g)
//	{
//		layer.paint(g, this);
//	}

	/** Paint the rectangular bounds of this poly */
//	public void paintBorder(Graphics2D g)
//	{
//		Rectangle r = getBounds();
//		Rectangle2D.Double r2 =
//			new Rectangle2D.Double(
//				r.getX() - 1,
//				r.getY() - 1,
//				r.getWidth() + 2,
//				r.getHeight() + 2);
//		layer.paint(g, r2);
//	}

	/** Create a new Poly given (x,y) points and a specific Layer */
	public Poly(double xpts[], double ypts[])
	{
		this.xpts = xpts;
		this.ypts = ypts;
		layer = null;
		style = 0;
		bounds = null;
	}

	/** Create a new rectangular Poly given a specific Layer */
	public Poly(double cX, double cY, double width, double height)
	{
		double halfWidth = width / 2;
		double halfHeight = height / 2;
		this.xpts = new double[] { cX-halfWidth,  cX+halfWidth,  cX+halfWidth,  cX-halfWidth };
		this.ypts = new double[] { cY-halfHeight, cY-halfHeight, cY+halfHeight, cY+halfHeight };
		layer = null;
		style = 0;
		bounds = null;
	}

	public Layer getLayer() { return layer; }
	public void setLayer(Layer layer) { this.layer = layer; }

	public int getStyle() { return style; }
	public void setStyle(int style) { this.style = style; }

	/** Get a transformed copy of this polygon, including scale, offset,
	 * and rotation.
	 * @param width factor to scale width
	 * @param height factor to scale height
	 * @param cx horizontal offset amount
	 * @param cy vertical offset amount
	 * @param cos cosine of the angle of rotation
	 * @param sin sine of the angle of rotation */
	public void transform(
		double width, double height,
		double cx, double cy,
		double cos, double sin)
	{
		for (int i = 0; i < xpts.length; i++)
		{
			double newX = ( xpts[i] * width * cos + ypts[i] * height * sin + cx);
			double newY = (-xpts[i] * width * sin + ypts[i] * height * cos + cy);
			xpts[i] = newX;   ypts[i] = newY;
		}
	}

	// SHAPE REQUIREMENTS:
	/** TODO: write contains(double, double); */
	public boolean contains(double x, double y)
	{
		return false;
	}

	/** TODO: write contains(Point2D); */
	public boolean contains(Point2D p)
	{
		return contains(p.getX(), p.getY());
	}

	/** TODO: write contains(double, double, double, double); */
	public boolean contains(double x, double y, double w, double h)
	{
		return false;
	}

	/** TODO: write contains(Rectangle2D); */
	public boolean contains(Rectangle2D r)
	{
		return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	/** TODO: write intersects(double, double, double, double); */
	public boolean intersects(double x, double y, double w, double h)
	{
		return false;
	}

	/** TODO: write intersects(Rectangle2D); */
	public boolean intersects(Rectangle2D r)
	{
		return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	/** Get the x coordinate of the center of this poly */
	public double getCenterX()
	{
		Rectangle2D b = getBounds2D();
		return b.getCenterX();
	}

	/** Get the y coordinate of the center of this poly */
	public double getCenterY()
	{
		Rectangle2D b = getBounds2D();
		return b.getCenterY();
	}

	/** Get the bounds of this poly, as a Rectangle2D */
	public Rectangle2D getBounds2D()
	{
		if (bounds == null) calcBounds();
		return bounds;
	}

	/** Get the bounds of this poly, as a Rectangle2D */
	public Rectangle2D.Double getBounds2DDouble()
	{
		if (bounds == null) calcBounds();
		return bounds;
	}

	/** Get the bounds of this poly, as a Rectangle */
	public Rectangle getBounds()
	{
		if (bounds == null) calcBounds();
		Rectangle2D r = getBounds2D();
		return new Rectangle((int)r.getX(), (int)r.getY(), (int)r.getWidth(), (int)r.getHeight());
	}

	protected void calcBounds()
	{
		double lx, ly, hx, hy;

		if (xpts.length == 0)
		{
			bounds = new Rectangle2D.Double();
			return;
		}
		lx = hx = xpts[0];
		ly = hy = ypts[0];
		for (int i = 1; i < xpts.length; i++)
		{
			if (xpts[i] < lx) lx = xpts[i];
			if (xpts[i] > hx) hx = xpts[i];
			if (ypts[i] < ly) ly = ypts[i];
			if (ypts[i] > hy) hy = ypts[i];
		}
		bounds = new Rectangle2D.Double(lx, ly, hx - lx, hy - ly);
	}

	class PolyPathIterator implements PathIterator
	{
		int idx = 0;
		AffineTransform trans;

		public PolyPathIterator(Poly p, AffineTransform at)
		{
			this.trans = at;
		}

		public int getWindingRule()
		{
			return WIND_EVEN_ODD;
		}

		public boolean isDone()
		{
			return idx > xpts.length;
		}

		public void next()
		{
			idx++;
		}

		public int currentSegment(float[] coords)
		{
			if (idx >= xpts.length)
			{
				return SEG_CLOSE;
			}
			coords[0] = (float) xpts[idx];
			coords[1] = (float) ypts[idx];
			if (trans != null)
			{
				trans.transform(coords, 0, coords, 0, 1);
			}
			return (idx == 0 ? SEG_MOVETO : SEG_LINETO);
		}

		public int currentSegment(double[] coords)
		{
			if (idx >= xpts.length)
			{
				return SEG_CLOSE;
			}
			coords[0] = xpts[idx];
			coords[1] = ypts[idx];
			if (trans != null)
			{
				trans.transform(coords, 0, coords, 0, 1);
			}
			return (idx == 0 ? SEG_MOVETO : SEG_LINETO);
		}
	}

	/** Get a PathIterator for this poly after a transform */
	public PathIterator getPathIterator(AffineTransform at)
	{
		return new PolyPathIterator(this, at);
	}

	/** Get a PathIterator for this poly after a transform, with a particular
	 * flatness */
	public PathIterator getPathIterator(AffineTransform at, double flatness)
	{
		return getPathIterator(at);
	}

//	/** Get a transformed copy of this polygon, including offset and rotation.
//	 * @param dx horizontal offset amount
//	 * @param dy vertical offset amount
//	 * @param cos cosine of the angle of rotation
//	 * @param sin sine of the angle of rotation */
//	public Poly transform(double dx, double dy, double cos, double sin)
//	{
//		return transform(1, 1, dx, dy, cos, sin);
//	}
}
