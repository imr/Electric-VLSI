package com.sun.electric.tool.user;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Layer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.util.*;

public class EditWindow extends JPanel 
	implements MouseMotionListener, MouseListener
{
	/** the window scale */									private double scale;
	/** the window offset */								private double offx, offy;
	/** the size of the window (in pixels) */				private Dimension sz;
	/** the cell that is in the window */					private Cell c;
	/** the offscreen image of the window */				private Image img = null;
	/** true if the window needs to be rerendered */		private boolean needsupdate = false;

	/** an identity transformation */						private static final AffineTransform IDENTITY = new AffineTransform();
	/** the offset of each new window on the screen */		private static int windowOffset = 0;

	EditWindow(int w, int h)
	{
		super();
		this.c = null;
		sz = new Dimension(w, h);
		setPreferredSize(sz);
		addMouseMotionListener(this);
		addMouseListener(this);
	}

	public static EditWindow newInstance(Cell c)
	{
		EditWindow window = new EditWindow(500, 500);
		JFrame jf = new JFrame(c.describe());
		jf.getContentPane().add(window);
		jf.pack();
		jf.show();
		jf.setLocation(windowOffset, windowOffset);
		windowOffset += 100;
		if (windowOffset > 500) windowOffset = 0;
		window.setCell(c);
		return window;
	}

	public void setCell(Cell c)
	{
		this.c = c;
		needsupdate = true;
		repaint();
	}

	void drawImage()
	{
		// set background color
		Graphics2D g2 = (Graphics2D)img.getGraphics();
		g2.setColor(Color.lightGray);
		g2.fillRect(0, 0, sz.width, sz.height);
		if (c == null) return;

		// setup graphics for rendering (start at bottom and work up)
		g2.translate(sz.width/2, sz.height/2);
		g2.scale(scale, -scale);
		g2.translate(-offx, -offy);

		// draw all arcs
		drawCell(g2, c, IDENTITY, true);
	}

	void drawCell(Graphics2D g2, Cell c, AffineTransform prevTrans, boolean topLevel)
	{
		// draw all arcs
		Iterator arcs = c.getArcs();
		while (arcs.hasNext())
		{
			drawArc(g2, (ArcInst)arcs.next(), prevTrans);
		}

		// draw all nodes
		Iterator nodes = c.getNodes();
		while (nodes.hasNext())
		{
			drawNode(g2, (NodeInst)nodes.next(), prevTrans, topLevel);
		}
	}

	void drawNode(Graphics2D g2, NodeInst ni, AffineTransform trans, boolean topLevel)
	{
		NodeProto np = ni.getProto();
		
		// see if the node is completely clipped from the screen
//		Rectangle2D clipBound = ni.getBounds();
//		Poly clipPoly = new Poly(clipBound.getCenterX(), clipBound.getCenterY(), clipBound.getWidth(), clipBound.getHeight());
//		AffineTransform screen = g2.getTransform();
//		clipPoly.transform(screen);
//		clipBound = clipPoly.getBounds();
//System.out.println("node is "+clipBound.getWidth()+"x"+clipBound.getHeight()+" at ("+clipBound.getCenterX()+","+clipBound.getCenterY());

		AffineTransform localTrans = ni.rotateOut(trans);
		if (np instanceof Cell)
		{
			// cell instance
			Cell subCell = (Cell)np;
			
			// two ways to draw a cell instance
			if (ni.isExpanded())
			{
				// show the contents
				AffineTransform subTrans = ni.translateOut(localTrans);
				drawCell(g2, subCell, subTrans, false);
			} else
			{
				// draw the outline
				Rectangle2D cellBounds = ni.getBounds();
				Poly poly = new Poly(cellBounds.getCenterX(), cellBounds.getCenterY(), cellBounds.getWidth(), cellBounds.getHeight());
				g2.setColor(Color.black);
				g2.draw(poly);
				drawTextCentered(g2, poly.getCenterX(), poly.getCenterY(), np.describe(), 1.0, Color.black);

				// show the ports
				int numPorts = ni.getNumPortInsts();
				PortInst [] portlist = new PortInst[numPorts];
				int i=0;
				for(Iterator it = ni.getPortInsts(); it.hasNext();)
				{
					portlist[i++] = (PortInst) it.next();
				}
				for(Iterator it = ni.getConnections(); it.hasNext();)
				{
					Connection con = (Connection) it.next();
					PortInst pi = con.getPortInst();
					portlist[pi.getIndex()] = null;
				}
				for(Iterator it = ni.getExports(); it.hasNext();)
				{
					Export exp = (Export) it.next();
					PortInst pi = exp.getOriginalPort();
					portlist[pi.getIndex()] = null;
				}
				for(i=0; i<numPorts; i++)
				{
					if (portlist[i] == null) continue;
					Poly portPoly = portlist[i].getPoly();
					portPoly.transform(trans);
					drawTextCentered(g2, portPoly.getCenterX(), portPoly.getCenterY(), portlist[i].getPortProto().getProtoName(), 0.25, Color.red);
				}
			}
		} else
		{
			// primitive
			if (topLevel || !ni.isVisInside())
			{
				PrimitiveNode prim = (PrimitiveNode)np;
				Technology tech = prim.getTechnology();
				Poly [] polys = tech.getShape(ni);
				drawPolys(g2, polys, localTrans);
			}
		}

		// draw any exports from the node
		if (topLevel)
		{
			Iterator it = ni.getExports();
			while (it.hasNext())
			{
				Export e = (Export) it.next();
				Poly poly = e.getOriginalPort().getPoly();
				poly.transform(localTrans);
				drawTextCentered(g2, poly.getCenterX(), poly.getCenterY(), e.getProtoName(), 0.25, Color.black);
			}
		}
	}

	void drawArc(Graphics2D g2, ArcInst ai, AffineTransform trans)
	{
		ArcProto ap = ai.getProto();
		Technology tech = ap.getTechnology();
		Poly [] polys = tech.getShape(ai);
		drawPolys(g2, polys, trans);
	}

	void drawPolys(Graphics2D g2, Poly [] polys, AffineTransform trans)
	{
		// set the color
		if (polys == null) return;
		for(int i = 0; i < polys.length; i++)
		{
			Poly poly = polys[i];
			Layer layer = poly.getLayer();
			Color color;
			if (layer == null) color = Color.black; else
			{
				EGraphics graphics = layer.getGraphics();
				color = graphics.getColor();
			}
			g2.setPaint(color);
			poly.transform(trans);
			Poly.Type style = poly.getStyle();
			if (style == Poly.Type.FILLED || style == Poly.Type.FILLEDRECT)
			{
				g2.fill(poly);
			} else if (style == Poly.Type.CLOSED)
			{
				AffineTransform saveAT = g2.getTransform();
				g2.scale(1/scale, 1/scale);
				GeneralPath gp = new GeneralPath();
				Point2D [] points = poly.getPoints();
				gp.moveTo((float)(points[0].getX()*scale), (float)(points[0].getY()*scale));
				for(int j=1; j<points.length; j++)
					gp.lineTo((float)(points[j].getX()*scale), (float)(points[j].getY()*scale));
				gp.lineTo((float)(points[0].getX()*scale), (float)(points[0].getY()*scale));
				g2.draw(gp);
				g2.setTransform(saveAT);
			} else if (style == Poly.Type.CLOSEDRECT)
			{
				System.out.println("Cannot render CLOSEDRECT polygon");
			} else if (style == Poly.Type.CROSSED)
			{
				AffineTransform saveAT = g2.getTransform();
				g2.scale(1/scale, 1/scale);
				GeneralPath gp = new GeneralPath();
				Point2D [] points = poly.getPoints();
				gp.moveTo((float)(points[0].getX()*scale), (float)(points[0].getY()*scale));
				for(int j=1; j<points.length; j++)
					gp.lineTo((float)(points[j].getX()*scale), (float)(points[j].getY()*scale));
				gp.lineTo((float)(points[0].getX()*scale), (float)(points[0].getY()*scale));
				gp.lineTo((float)(points[2].getX()*scale), (float)(points[2].getY()*scale));
				gp.moveTo((float)(points[1].getX()*scale), (float)(points[1].getY()*scale));
				gp.lineTo((float)(points[3].getX()*scale), (float)(points[3].getY()*scale));
				g2.draw(gp);
				g2.setTransform(saveAT);
			} else if (style == Poly.Type.OPENED)
			{
				AffineTransform saveAT = g2.getTransform();
				g2.scale(1/scale, 1/scale);
				GeneralPath gp = new GeneralPath();
				Point2D [] points = poly.getPoints();
				gp.moveTo((float)(points[0].getX()*scale), (float)(points[0].getY()*scale));
				for(int j=1; j<points.length; j++)
					gp.lineTo((float)(points[j].getX()*scale), (float)(points[j].getY()*scale));
				g2.draw(gp);
				g2.setTransform(saveAT);
			} else if (style == Poly.Type.OPENEDT1)
			{
				System.out.println("Cannot render OPENEDT1 polygon");
			} else if (style == Poly.Type.OPENEDT2)
			{
				System.out.println("Cannot render OPENEDT2 polygon");
			} else if (style == Poly.Type.OPENEDT3)
			{
				System.out.println("Cannot render OPENEDT3 polygon");
			} else if (style == Poly.Type.OPENEDO1)
			{
				System.out.println("Cannot render OPENEDO1 polygon");
			} else if (style == Poly.Type.VECTORS)
			{
				AffineTransform saveAT = g2.getTransform();
				g2.scale(1/scale, 1/scale);
				GeneralPath gp = new GeneralPath();
				Point2D [] points = poly.getPoints();
				for(int j=0; j<points.length; j+=2)
				{
					gp.moveTo((float)(points[j].getX()*scale), (float)(points[j].getY()*scale));
					gp.lineTo((float)(points[j+1].getX()*scale), (float)(points[j+1].getY()*scale));
				}
				g2.draw(gp);
				g2.setTransform(saveAT);
			} else if (style == Poly.Type.CIRCLE)
			{
				System.out.println("Cannot render CIRCLE polygon");
			} else if (style == Poly.Type.THICKCIRCLE)
			{
				System.out.println("Cannot render THICKCIRCLE polygon");
			} else if (style == Poly.Type.DISC)
			{
				System.out.println("Cannot render DISC polygon");
			} else if (style == Poly.Type.CIRCLEARC)
			{
				System.out.println("Cannot render CIRCLEARC polygon");
			} else if (style == Poly.Type.THICKCIRCLEARC)
			{
				System.out.println("Cannot render THICKCIRCLEARC polygon");
			} else if (style == Poly.Type.GRIDDOTS)
			{
				System.out.println("Cannot render GRIDDOTS polygon");
			} else if (style == Poly.Type.CROSS)
			{
				// draw the big cross
				AffineTransform saveAT = g2.getTransform();
				float x = (float)(poly.getCenterX()*scale);
				float y = (float)(poly.getCenterY()*scale);
				g2.scale(1/scale, 1/scale);
				g2.setColor(Color.black);
				GeneralPath gp = new GeneralPath();
				float size = 3;
				gp.moveTo(x+size, y);  gp.lineTo(x-size, y);
				gp.moveTo(x, y+size);  gp.lineTo(x, y-size);
				g2.draw(gp);
				g2.setTransform(saveAT);
			} else if (style == Poly.Type.BIGCROSS)
			{
				// draw the big cross
				AffineTransform saveAT = g2.getTransform();
				float x = (float)(poly.getCenterX()*scale);
				float y = (float)(poly.getCenterY()*scale);
				g2.scale(1/scale, 1/scale);
				g2.setColor(Color.black);
				GeneralPath gp = new GeneralPath();
				float size = 5;
				gp.moveTo(x+size, y);  gp.lineTo(x-size, y);
				gp.moveTo(x, y+size);  gp.lineTo(x, y-size);
				g2.draw(gp);
				g2.setTransform(saveAT);
			}
		}
	}

//		/** text at center */								public static final Type TEXTCENT=       new Type();
//		/** text below top edge */							public static final Type TEXTTOP=        new Type();
//		/** text above bottom edge */						public static final Type TEXTBOT=        new Type();
//		/** text to right of left edge */					public static final Type TEXTLEFT=       new Type();
//		/** text to left of right edge */					public static final Type TEXTRIGHT=      new Type();
//		/** text to lower-right of top-left corner */		public static final Type TEXTTOPLEFT=    new Type();
//		/** text to upper-right of bottom-left corner */	public static final Type TEXTBOTLEFT=    new Type();
//		/** text to lower-left of top-right corner */		public static final Type TEXTTOPRIGHT=   new Type();
//		/** text to upper-left of bottom-right corner */	public static final Type TEXTBOTRIGHT=   new Type();
//		/** text that fits in box (may shrink) */			public static final Type TEXTBOX=        new Type();

	void drawTextCentered(Graphics2D g2, double x, double y, String text, double textScale, Color color)
	{
		// make a glyph vector for the desired text
		Font font = g2.getFont();
		FontRenderContext frc = new FontRenderContext(null, false, false);
		GlyphVector gv = font.createGlyphVector(frc, text);

		// adjust to place text in the center
		Rectangle2D glyphBounds = gv.getVisualBounds();
		x -= glyphBounds.getCenterX() * textScale;
		y += glyphBounds.getCenterY() * textScale;

		// draw the text
		AffineTransform saveAT = g2.getTransform();
		g2.translate(x, y);
		g2.scale(textScale, -textScale);
		g2.translate(-x, -y);
		g2.setColor(color);
		g2.drawGlyphVector(gv, (float)x, (float)y);
		g2.setTransform(saveAT);
	}

	public void paint(Graphics g)
	{
		if (img == null || !getSize().equals(sz))
		{
			if (c == null) return;
			sz = getSize();
			img = createImage(sz.width, sz.height);
			Rectangle2D cellBounds = c.getBounds();
			scale = sz.width/cellBounds.getWidth() * 0.8;
			offx = cellBounds.getCenterX();
			offy = cellBounds.getCenterY();
			needsupdate = true;
		}
		if (needsupdate)
		{
			needsupdate = false;
			drawImage();
		}
		g.drawImage(img, 0, 0, this);
	}

	int oldx, oldy;

	public void mousePressed(MouseEvent evt)
	{
		oldx = evt.getX();
		oldy = evt.getY();
	}

	public void mouseReleased(MouseEvent evt) {}
	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mouseMoved(MouseEvent evt) {}

	public void mouseDragged(MouseEvent evt)
	{
		if ((evt.getModifiers()&evt.CTRL_MASK) != 0)
		{
			// control key held: zoom
			scale = scale * Math.exp((oldy-evt.getY()) / 100.0f);
		} else
		{
			// shift key held: pan
			offx -= (evt.getX() - oldx) / scale;
			offy += (evt.getY() - oldy) / scale;
		}
		oldx = evt.getX();
		oldy = evt.getY();
		needsupdate = true;
		repaint();
	}
}
