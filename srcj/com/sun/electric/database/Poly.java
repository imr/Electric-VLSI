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
	private Layer layer;
	private double xpts[], ypts[];
	private Rectangle2D.Double bounds;

	// polygons ************
	/** closed polygon, filled in */						public static final int FILLED=          0;
	/** closed polygon, outline  */							public static final int CLOSED=          1;
	// rectangles ************
	/** closed rectangle, filled in */						public static final int FILLEDRECT=      2;
	/** closed rectangle, outline */						public static final int CLOSEDRECT=      3;
	/** closed rectangle, outline crossed */				public static final int CROSSED=         4;
	// lines ************
	/** open outline, solid */								public static final int OPENED=          5;
	/** open outline, dotted */								public static final int OPENEDT1=        6;
	/** open outline, dashed  */							public static final int OPENEDT2=        7;
	/** open outline, thicker */							public static final int OPENEDT3=        8;
	/** open outline pushed by 1 */							public static final int OPENEDO1=        9;
	/** vector endpoint pairs, solid */						public static final int VECTORS=        10;
	// curves ************
	/** circle at [0] radius to [1] */						public static final int CIRCLE=         11;
	/** thick circle at [0] radius to [1] */				public static final int THICKCIRCLE=    12;
	/** filled circle */									public static final int DISC=           13;
	/** arc of circle at [0] ends [1] and [2] */			public static final int CIRCLEARC=      14;
	/** thick arc of circle at [0] ends [1] and [2] */		public static final int THICKCIRCLEARC= 15;
	// text ************
	/** text at center */									public static final int TEXTCENT=       16;
	/** text below top edge */								public static final int TEXTTOP=        17;
	/** text above bottom edge */							public static final int TEXTBOT=        18;
	/** text to right of left edge */						public static final int TEXTLEFT=       19;
	/** text to left of right edge */						public static final int TEXTRIGHT=      20;
	/** text to lower-right of top-left corner */			public static final int TEXTTOPLEFT=    21;
	/** text to upper-right of bottom-left corner */		public static final int TEXTBOTLEFT=    22;
	/** text to lower-left of top-right corner */			public static final int TEXTTOPRIGHT=   23;
	/** text to upper-left of bottom-right corner */		public static final int TEXTBOTRIGHT=   24;
	/** text that fits in box (may shrink) */				public static final int TEXTBOX=        25;
	// miscellaneous ************
	/** grid dots in the window */							public static final int GRIDDOTS=       26;
	/** cross */											public static final int CROSS=          27;
	/** big cross */										public static final int BIGCROSS=       28;

	/* text font sizes (in VARIABLE, NODEINST, PORTPROTO, and POLYGON->textdescription) */
	/** points from 1 to TXTMAXPOINTS */					public static final int TXTPOINTS=        077;
	/** right-shift of TXTPOINTS */							public static final int TXTPOINTSSH=        0;
	public static final int TXTMAXPOINTS=      63;
//	public static final int TXTSETPOINTS(p)   ((p)<<TXTPOINTSSH)
//	public static final int TXTGETPOINTS(p)   (((p)&TXTPOINTS)>>TXTPOINTSSH)

	public static final int TXTQLAMBDA=    077700;		
	public static final int TXTQLAMBDASH=       6;		
	public static final int TXTMAXQLAMBDA=    511;
//	public static final int TXTSETQLAMBDA(ql) ((ql)<<TXTQLAMBDASH)
//	public static final int TXTGETQLAMBDA(ql) (((ql)&TXTQLAMBDA)>>TXTQLAMBDASH)
	/** fixed-width text for text editing */			public static final int TXTEDITOR=     077770;
	/** text for menu selection */						public static final int TXTMENU=       077771;

	/** Get a transformed copy of this polygon, including scale, offset,
	 * and rotation.
	 * @param sw factor to scale width
	 * @param sh factor to scale height
	 * @param dx horizontal offset amount
	 * @param dy vertical offset amount
	 * @param cos cosine of the angle of rotation
	 * @param sin sine of the angle of rotation */
	public Poly transform(
		double sw,
		double sh,
		double dx,
		double dy,
		double cos,
		double sin)
	{
		double nx[] = new double[xpts.length];
		double ny[] = new double[ypts.length];
		for (int i = 0; i < nx.length; i++)
		{
			nx[i] = (xpts[i] * sw * cos - ypts[i] * sh * sin + dx);
			ny[i] = (ypts[i] * sh * cos + xpts[i] * sw * sin + dy);
		}
		return new Poly(layer, nx, ny);
	}

	/** Get a transformed copy of this polygon, including offset and rotation.
	 * @param dx horizontal offset amount
	 * @param dy vertical offset amount
	 * @param cos cosine of the angle of rotation
	 * @param sin sine of the angle of rotation */
	public Poly transform(double dx, double dy, double cos, double sin)
	{
		return transform(1, 1, dx, dy, cos, sin);
	}

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
	public Poly(Layer lay, double xpts[], double ypts[])
	{
		this.xpts = xpts;
		this.ypts = ypts;
		this.layer = lay;
	}

	/** Create a new rectangular Poly given a specific Layer */
	public Poly(Layer lay, double x, double y, double w, double h)
	{
		this.xpts = new double[] { x, x + w, x + w, x };
		this.ypts = new double[] { y, y, y + h, y + h };
		this.layer = lay;
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

	/** Get the bounds of this poly, as a Rectangle2D */
	public Rectangle2D getBounds2D()
	{
		if (bounds == null)
			calcBounds();
		return bounds;
	}

	/** Get the x coordinate of the center of this poly */
	public int getCenterX()
	{
		Rectangle b = getBounds();
		return b.x + b.width / 2;
	}

	/** Get the y coordinate of the center of this poly */
	public int getCenterY()
	{
		Rectangle b = getBounds();
		return b.y + b.height / 2;
	}

	/** Get the bounds of this poly, as a Rectangle */
	public Rectangle getBounds()
	{
		Rectangle2D r = getBounds2D();
		return new Rectangle(
			ElectricObject.round(r.getX()),
			ElectricObject.round(r.getY()),
			ElectricObject.round(r.getWidth()),
			ElectricObject.round(r.getHeight()));
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
			if (xpts[i] < lx)
				lx = xpts[i];
			if (xpts[i] > hx)
				hx = xpts[i];
			if (ypts[i] < ly)
				ly = ypts[i];
			if (ypts[i] > hy)
				hy = ypts[i];
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
}
