package com.sun.electric.technology;

import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.technologies.TecGeneric;
import com.sun.electric.technology.technologies.TecSchematics;
import com.sun.electric.technology.technologies.TecArtwork;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * A Technology object contains PrimitiveNodes and ArcProtos.  There may
 * be more than one Technology object.  To get a particular Technology,
 * use <code>Electric.findTechnology</code>
 */
public class Technology extends ElectricObject
{
	/** technology is not electrical */									private static final int NONELECTRICAL =       01;
	/** has no directional arcs */										private static final int NODIRECTIONALARCS =   02;
	/**  has no negated arcs */											private static final int NONEGATEDARCS =       04;
	/** nonstandard technology (cannot be edited) */					private static final int NONSTANDARD =        010;
	/** statically allocated (don't deallocate memory) */				private static final int STATICTECHNOLOGY =   020;
	/** no primitives in this technology (don't auto-switch to it) */	private static final int NOPRIMTECHNOLOGY =   040;

	public static class ArcLayer
	{
		Layer layer;
		int offset;
		Poly.Type style;

		public ArcLayer(Layer layer, int offset, Poly.Type style)
		{
			this.layer = layer;
			this.offset = offset;
			this.style = style;
		}
		public Layer getLayer() { return layer; }
		public int getOffset() { return offset; }
		public Poly.Type getStyle() { return style; }
	}

	public static class TechPoint
	{
		private EdgeH x;
		private EdgeV y;
		
		public static final TechPoint [] ATCENTER = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER)};
		public static final TechPoint [] FULLBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)};
		public static final TechPoint [] IN0HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(0.5), EdgeV.fromBottom(0.5)),
					new Technology.TechPoint(EdgeH.fromRight(0.5), EdgeV.fromTop(0.5))};
		public static final TechPoint [] IN1BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
					new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(1))};
		public static final TechPoint [] IN1HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5)),
					new Technology.TechPoint(EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))};
		public static final TechPoint [] IN2BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(2)),
					new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.fromTop(2))};
		public static final TechPoint [] IN2HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5)),
					new Technology.TechPoint(EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))};
		public static final TechPoint [] IN3BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
					new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(3))};
		public static final TechPoint [] IN3HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(3.5), EdgeV.fromBottom(3.5)),
					new Technology.TechPoint(EdgeH.fromRight(3.5), EdgeV.fromTop(3.5))};
		public static final TechPoint [] IN4BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(4)),
					new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(4))};
		public static final TechPoint [] IN4HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(4.5), EdgeV.fromBottom(4.5)),
					new Technology.TechPoint(EdgeH.fromRight(4.5), EdgeV.fromTop(4.5))};
		public static final TechPoint [] IN5BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.fromBottom(5)),
					new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.fromTop(5))};
		public static final TechPoint [] IN5HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(5.5), EdgeV.fromBottom(5.5)),
					new Technology.TechPoint(EdgeH.fromRight(5.5), EdgeV.fromTop(5.5))};
		public static final TechPoint [] IN6BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(6)),
					new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(6))};
		public static final TechPoint [] IN6HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromBottom(6.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromTop(6.5))};
		public static final TechPoint [] IN7BOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7), EdgeV.fromBottom(7)),
					new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromTop(7))};
		public static final TechPoint [] IN7HBOX = new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromBottom(7.5)),
					new Technology.TechPoint(EdgeH.fromRight(7.5), EdgeV.fromTop(7.5))};

		public TechPoint(EdgeH x, EdgeV y)
		{
			this.x = x;
			this.y = y;
		}
		public EdgeH getX() { return x; }
		public EdgeV getY() { return y; }
	}

	public static class NodeLayer
	{
		private Layer layer;
		private int portNum;
		private Poly.Type style;
		private int representation;
		private TechPoint [] points;
		private String message;

		/** list of scalable points */		public static final int POINTS=     0;
		/** a rectangle */					public static final int BOX=        1;
		/** list of absolute points */		public static final int ABSPOINTS=  2;
		/** minimum sized rectangle */		public static final int MINBOX=     3;

		public NodeLayer(Layer layer, int portNum, Poly.Type style, int representation,
			TechPoint [] points)
		{
			this.layer = layer;
			this.portNum = portNum;
			this.style = style;
			this.representation = representation;
			this.points = points;
		}
		public Layer getLayer() { return layer; }
		public int getPortNum() { return portNum; }
		public Poly.Type getStyle() { return style; }
		public int getRepresentation() { return representation; }
		public TechPoint [] getPoints() { return points; }
		public EdgeH getLeftEdge() { return points[0].getX(); }
		public EdgeV getBOTTOMEDGE() { return points[0].getY(); }
		public EdgeH getRIGHTEDGE() { return points[1].getX(); }
		public EdgeV getTOPEDGE() { return points[1].getY(); }
		public String getMessage() { return message; }
		public void setMessage(String message) { this.message = message; }
	}

	/** name of the technology */						private String techName;
	/** full description of the technology */			private String techDesc;
	/** flags for the technology */						private int userBits;
	/** 0-based index of the technology */				private int techIndex;
	/** critical dimensions for the technology */		private double scale;
	/** list of primitive nodes in the technology */	private List nodes;
	/** list of arcs in the technology */				private List arcs;

	/* static list of all Technologies in Electric */	private static List technologies = new ArrayList();
	/* the current technology in Electric */			private static Technology curTech = null;
	/* the current tlayout echnology in Electric */		private static Technology curLayoutTech = null;
	/* counter for enumerating technologies */			private static int techNumber = 0;

	protected Technology()
	{
		this.nodes = new ArrayList();
		this.arcs = new ArrayList();
		this.scale = 1.0;
		this.techIndex = techNumber++;
		userBits = 0;

		// add the technology to the global list
		technologies.add(this);
	}

	protected static boolean validTechnology(String techName)
	{
		if (Technology.findTechnology(techName) != null)
		{
			System.out.println("ERROR: Multiple technologies named " + techName);
			return false;
		}
		return true;
	}

	void addNodeProto(PrimitiveNode pn)
	{
		nodes.add(pn);
	}

	public void addArcProto(PrimitiveArc ap)
	{
		arcs.add(ap);
	}

	/** Set the NonElectrical bit */
	public void setNonElectrical() { userBits |= NONELECTRICAL; }
	/** Get the NonElectrical bit */
	public boolean isNonElectrical() { return (userBits & NONELECTRICAL) != 0; }

	/** Set the No Directional Arcs bit */
	public void setNoDirectionalArcs() { userBits |= NODIRECTIONALARCS; }
	/** Get the No Directional bit */
	public boolean isNoDirectionalArcs() { return (userBits & NODIRECTIONALARCS) != 0; }

	/** Set the No Negated Arcs bit */
	public void setNoNegatedArcs() { userBits |= NONEGATEDARCS; }
	/** Get the No Directional bit */
	public boolean isNoNegatedArcs() { return (userBits & NONEGATEDARCS) != 0; }

	/** Set the NonStandard bit */
	public void setNonStandard() { userBits |= NONSTANDARD; }
	/** Get the No Directional bit */
	public boolean isNonStandard() { return (userBits & NONSTANDARD) != 0; }

	/** Set the Static Technology bit */
	public void setStaticTechnology() { userBits |= NONSTANDARD; }
	/** Get the No Directional bit */
	public boolean isStaticTechnology() { return (userBits & NONSTANDARD) != 0; }

	/** Set the No Primitive Nodes bit */
	public void setNoPrimitiveNodes() { userBits |= NOPRIMTECHNOLOGY; }
	/** Get the No Directional bit */
	public boolean isNoPrimitiveNodes() { return (userBits & NOPRIMTECHNOLOGY) != 0; }

	/**
	 * Return the current Technology
	 */
	public static Technology getCurrent() { return curTech; }

	/**
	 * Set the current Technology
	 */
	public static void setCurrent(Technology tech)
	{
		curTech = tech;
		if (tech != TecGeneric.tech && tech != TecSchematics.tech && tech != TecArtwork.tech)
			curLayoutTech = tech;
	}

	/** 
	 * get the name (short) of this technology
	 */
	public String getTechName() { return techName; }

	/** 
	 * set the name (short) of this technology
	 */
	protected void setTechName(String techName) { this.techName = techName; }

	/**
	 * get the description (long) of this technology
	 */
	public String getTechDesc() { return techDesc; }

	/**
	 * get the description (long) of this technology
	 */
	public void setTechDesc(String techDesc) { this.techDesc = techDesc; }

	/**
	 * get the default scale for this technology.
	 * typically overridden by a library
	 */
	public double getScale() { return scale; }

	/**
	 * set the default scale of this technology.
	 */
	public void setScale(double scale)
	{
		if (scale != 0) this.scale = scale;
	}

	/**
	 * get the 0-based index of this technology.
	 */
	public int getIndex() { return techIndex; }

	public static int numTechnologies()
	{
		return technologies.size();
	}

	/** Find the Technology with a particular name.
	 * @param name the name of the desired Technology
	 * @return the Technology with the same name, or null if no 
	 * Technology matches.
	 */
	public static Technology findTechnology(String name)
	{
		for (int i = 0; i < technologies.size(); i++)
		{
			Technology t = (Technology) technologies.get(i);
			if (t.techName.equalsIgnoreCase(name))
				return t;
		}
		return null;
	}

	/**
	 * get an iterator over all of the Technologies
	 */
	public static Iterator getTechnologyIterator()
	{
		return technologies.iterator();
	}

	/**
	 * Get a list of polygons that describe node "ni"
	 */
	public Poly [] getShape(NodeInst ni)
	{
		NodeProto prototype = ni.getProto();
		if (!(prototype instanceof PrimitiveNode)) return null;

		// see if the node is "wiped" (not drawn)
		if (ni.isWiped()) return null;
		if (prototype.isWipeOn1or2())
		{
			if (ni.pinUseCount()) return null;
		}
		PrimitiveNode np = (PrimitiveNode)prototype;
		Technology.NodeLayer [] primLayers = np.getLayers();
		return getShape(ni, primLayers);
	}

	public Poly [] getShape(NodeInst ni, Technology.NodeLayer [] primLayers)
	{
		// get information about the node
		double halfWidth = ni.getXSize() / 2;
		double lowX = ni.getCenterX() - halfWidth;
		double highX = ni.getCenterX() + halfWidth;
		double halfHeight = ni.getYSize() / 2;
		double lowY = ni.getCenterY() - halfHeight;
		double highY = ni.getCenterY() + halfHeight;

		NodeProto np = ni.getProto();
		if (np.isHoldsOutline())
		{
			Integer [] outline = ni.getTrace();
			if (outline != null)
			{
				int numPolys = ni.numDisplayableVariables() + 1;
				Poly [] polys = new Poly[numPolys];
				int numPoints = outline.length / 2;
				Point2D.Double [] pointList = new Point2D.Double[numPoints];
				for(int i=0; i<numPoints; i++)
				{
					pointList[i] = new Point2D.Double(ni.getCenterX() + outline[i*2].intValue(),
						ni.getCenterY() + outline[i*2+1].intValue());
				}
				polys[0] = new Poly(pointList);
				Technology.NodeLayer primLayer = primLayers[0];
				polys[0].setStyle(primLayer.getStyle());
				polys[0].setLayer(primLayer.getLayer());
				Rectangle2D rect = ni.getBounds();
				ni.addDisplayableVariables(rect, polys, 1);
				return polys;
			}
		}

		// construct the polygons
		int numPolys = ni.numDisplayableVariables() + primLayers.length;
		Poly [] polys = new Poly[numPolys];
		for(int i = 0; i < primLayers.length; i++)
		{
			Technology.NodeLayer primLayer = primLayers[i];
			int representation = primLayer.getRepresentation();
			Poly.Type style = primLayer.getStyle();
			if (representation == Technology.NodeLayer.BOX)
			{
				double portLowX = ni.getCenterX() + primLayer.getLeftEdge().getMultiplier() * ni.getXSize() + primLayer.getLeftEdge().getAdder();
				double portHighX = ni.getCenterX() + primLayer.getRIGHTEDGE().getMultiplier() * ni.getXSize() + primLayer.getRIGHTEDGE().getAdder();
				double portLowY = ni.getCenterY() + primLayer.getBOTTOMEDGE().getMultiplier() * ni.getYSize() + primLayer.getBOTTOMEDGE().getAdder();
				double portHighY = ni.getCenterY() + primLayer.getTOPEDGE().getMultiplier() * ni.getYSize() + primLayer.getTOPEDGE().getAdder();
				double portX = (portLowX + portHighX) / 2;
				double portY = (portLowY + portHighY) / 2;
				polys[i] = new Poly(portX, portY, portHighX-portLowX, portHighY-portLowY);
			} else if (representation == Technology.NodeLayer.POINTS)
			{
				TechPoint [] points = primLayer.getPoints();
				Point2D.Double [] pointList = new Point2D.Double[points.length];
				for(int j=0; j<points.length; j++)
				{
					EdgeH xFactor = points[j].getX();
					EdgeV yFactor = points[j].getY();
					double x = 0, y = 0;
					if (xFactor != null && yFactor != null)
					{
						x = ni.getCenterX() + xFactor.getMultiplier() * ni.getXSize() + xFactor.getAdder();
						y = ni.getCenterY() + yFactor.getMultiplier() * ni.getYSize() + yFactor.getAdder();
					}
					pointList[j] = new Point2D.Double(x, y);
				}
				polys[i] = new Poly(pointList);
			}
			if (style == Poly.Type.TEXTCENT || style == Poly.Type.TEXTTOP || style == Poly.Type.TEXTBOT ||
				style == Poly.Type.TEXTLEFT || style == Poly.Type.TEXTRIGHT || style == Poly.Type.TEXTTOPLEFT ||
				style == Poly.Type.TEXTBOTLEFT || style == Poly.Type.TEXTTOPRIGHT || style == Poly.Type.TEXTBOTRIGHT ||
				style == Poly.Type.TEXTBOX)
			{
				polys[i].setString(primLayer.getMessage());
			}
			polys[i].setStyle(style);
			polys[i].setLayer(primLayer.getLayer());
		}
		Rectangle2D rect = ni.getBounds();
		ni.addDisplayableVariables(rect, polys, primLayers.length);
		return polys;
	}

	/**
	 * Get a list of polygons that describe arc "ai".
	 */
	public Poly [] getShape(ArcInst ai)
	{
		// get information about the arc
		PrimitiveArc ap = (PrimitiveArc)ai.getProto();
		Technology tech = ap.getTechnology();
		Technology.ArcLayer [] primLayers = ap.getLayers();

		// see how many polygons describe this arc
		boolean addArrow = false;
		if (!tech.isNoDirectionalArcs() && ai.isDirectional()) addArrow = true;
		int numDisplayable = ai.numDisplayableVariables();
		int maxPolys = primLayers.length + numDisplayable;
		if (addArrow) maxPolys++;
		Poly [] polys = new Poly[maxPolys];
		int polyNum = 0;

		// construct the polygons that describe the basic arc
		for(int i = 0; i < primLayers.length; i++)
		{
			Technology.ArcLayer primLayer = primLayers[i];
			polys[polyNum] = ai.makearcpoly(ai.getXSize(), ai.getWidth() - primLayer.getOffset(), primLayer.getStyle());
			if (polys[polyNum] == null) return null;
			polys[polyNum].setLayer(primLayer.getLayer());
			polyNum++;
		}

		// add an arrow to the arc description
		if (addArrow)
		{
			Point2D.Double headLoc = ai.getHead().getLocation();
			Point2D.Double tailLoc = ai.getTail().getLocation();
			double headX = headLoc.getX();   double headY = headLoc.getY();
			double tailX = tailLoc.getX();   double tailY = tailLoc.getY();
			double angle = ai.getAngle();
			if (ai.isReverseEnds())
			{
				double swap = headX;   headX = tailX;   tailX = swap;
				swap = headY;   headY = tailY;   tailY = swap;
				angle += Math.PI;
			}
			int numPoints = 6;
			if (ai.isSkipHead()) numPoints = 2;
			Point2D.Double [] points = new Point2D.Double[numPoints];
			points[0] = new Point2D.Double(headX, headY);
			points[1] = new Point2D.Double(tailX, tailY);
			if (!ai.isSkipHead())
			{
				points[2] = points[0];
				double angleOfArrow = Math.PI/6;		// 30 degrees
				double backAngle1 = angle - angleOfArrow;
				double backAngle2 = angle + angleOfArrow;
				points[3] = new Point2D.Double(headX + Math.cos(backAngle1), headY + Math.sin(backAngle1));
				points[4] = points[0];
				points[5] = new Point2D.Double(headX + Math.cos(backAngle2), headY + Math.sin(backAngle2));
			}
			polys[polyNum] = new Poly(points);
			polys[polyNum].setStyle(Poly.Type.VECTORS);
			polys[polyNum].setLayer(null);
			polyNum++;
		}
		
		// add in the displayable variables
		Rectangle2D rect = ai.getBounds();
		ai.addDisplayableVariables(rect, polys, polyNum);

		return polys;
	}

	/**
	 * Get a polygon that describes port "pp" of node "ni"
	 */
	public Poly getPoly(NodeInst ni, PrimitivePort pp)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		int [] specialValues = np.getSpecialValues();
//		if (specialValues[0] == PrimitiveNode.SERPTRANS)
//		{
//			// serpentine transistors use a more complex port determination (tech_filltransport)
//			Poly portpoly = new Poly(0, 0, 0, 0);
//			return portpoly;
//		} else
		{
			// standard port determination, see if there is outline information
//			if (np.isHoldsOutline())
//			{
//				// outline may determinesthe port
//				Poly portpoly = new Poly(1, 2, 3, 4);
//				return portpoly;
//			} else
			{
				// standard port computation
				double halfWidth = ni.getXSize() / 2;
				double lowX = ni.getCenterX() - halfWidth;
				double highX = ni.getCenterX() + halfWidth;
				double halfHeight = ni.getYSize() / 2;
				double lowY = ni.getCenterY() - halfHeight;
				double highY = ni.getCenterY() + halfHeight;
				
				double portLowX = ni.getCenterX() + pp.getLeft().getMultiplier() * ni.getXSize() + pp.getLeft().getAdder();
				double portHighX = ni.getCenterX() + pp.getRight().getMultiplier() * ni.getXSize() + pp.getRight().getAdder();
				double portLowY = ni.getCenterY() + pp.getBottom().getMultiplier() * ni.getYSize() + pp.getBottom().getAdder();
				double portHighY = ni.getCenterY() +pp.getTop().getMultiplier() * ni.getYSize() + pp.getTop().getAdder();
				double portX = (portLowX + portHighX) / 2;
				double portY = (portLowY + portHighY) / 2;
				Poly portpoly = new Poly(portX, portY, portHighX-portLowX, portHighY-portLowY);
				return portpoly;
			}
		}
	}

	protected void getInfo()
	{
		System.out.println(" Name: " + techName);
		System.out.println(" Description: " + techDesc);
		System.out.println(" Nodes (" + nodes.size() + ")");
		for (int i = 0; i < nodes.size(); i++)
		{
			System.out.println("     " + nodes.get(i));
		}
		System.out.println(" Arcs (" + arcs.size() + ")");
		for (int i = 0; i < arcs.size(); i++)
		{
			System.out.println("     " + arcs.get(i));
		}
		super.getInfo();
	}

	/*
	 * Routine to convert old primitive node names to their proper nodeprotos.
	 */
	public PrimitiveNode convertOldNodeName(String name) { return null; }

	/*
	 * Routine to convert old primitive node names to their proper nodeprotos.
	 */
	public PrimitiveArc convertOldArcName(String name) { return null; }

	/*
	 * Routine to convert old primitive port names to their proper portprotos.
	 */
	public PrimitivePort convertOldPortName(String portName, PrimitiveNode np)
	{
//		if (np == sch_sourceprim || np == sch_meterprim)
//		{
//			if (portname == "top") return np->firstportproto;
//			if (portname == "bottom") return np->firstportproto->nextportproto;
//		}
//		if (np == sch_twoportprim)
//		{
//			if (portname == "upperleft") return(np->firstportproto);
//			if (portname == "lowerleft") return(np->firstportproto->nextportproto);
//			if (portname == "upperright") return(np->firstportproto->nextportproto->nextportproto);
//			if (portname == "lowerright") return(np->firstportproto->nextportproto->nextportproto->nextportproto);
//		}

		// some technologies switched from ports ending in "-bot" to the ending "-bottom"
		int len = portName.length() - 4;
		if (len > 0 && portName.substring(len) == "-bot")
		{
			PrimitivePort pp = (PrimitivePort)np.findPortProto(portName + "tom");
			if (pp != null) return pp;
		}
		return null;
	}

	public String toString()
	{
		return "Technology " + techName;
	}

	public static Technology whatTechnology(NodeProto cell, NodeProto [] nodeProtoList, int startNodeProto, int endNodeProto,
		ArcProto [] arcProtoList, int startArcProto, int endArcProto)
	{
		// primitives know their technology
		if (cell instanceof PrimitiveNode) return(((PrimitiveNode)cell).getTechnology());

		// count the number of technologies
		int maxTech = 0;
		for(Iterator it = Technology.getTechnologyIterator(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			if (tech.getIndex() > maxTech) maxTech = tech.getIndex();
		}
		maxTech++;

		// create an array of counts for each technology
		int [] useCount = new int[maxTech];
		for(int i=0; i<maxTech; i++) useCount[i] = 0;

		// count technologies of all primitive nodes in the cell
		if (nodeProtoList != null)
		{
			// iterate over the NodeProtos in the list
			for(int i=startNodeProto; i<endNodeProto; i++)
			{
				NodeProto np = nodeProtoList[i];
				Technology nodeTech = np.getTechnology();
				if (nodeTech != null) useCount[nodeTech.getIndex()]++;
			}
		} else
		{
			for(Iterator it = ((Cell)cell).getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				NodeProto np = ni.getProto();
				Technology nodeTech = np.getTechnology();
				if (nodeTech != null) useCount[nodeTech.getIndex()]++;
			}
		}

		// count technologies of all arcs in the cell
		if (arcProtoList != null)
		{
			// iterate over the arcprotos in the list
			for(int i=startArcProto; i<endArcProto; i++)
			{
				ArcProto ap = arcProtoList[i];
				useCount[ap.getTechnology().getIndex()]++;
			}
		} else
		{
			for(Iterator it = ((Cell)cell).getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ArcProto ap = ai.getProto();
				useCount[ap.getTechnology().getIndex()]++;
			}
		}

		// find a concensus
		int best = 0;         Technology bestTech = null;
		int bestLayout = 0;   Technology bestLayoutTech = null;
		for(Iterator it = Technology.getTechnologyIterator(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();

			// always ignore the generic technology
			if (tech == TecGeneric.tech) continue;

			// find the most popular of ALL technologies
			if (useCount[tech.getIndex()] > best)
			{
				best = useCount[tech.getIndex()];
				bestTech = tech;
			}

			// find the most popular of the layout technologies
			if (tech == TecSchematics.tech || tech == TecArtwork.tech) continue;
			if (useCount[tech.getIndex()] > bestLayout)
			{
				bestLayout = useCount[tech.getIndex()];
				bestLayoutTech = tech;
			}
		}

		Technology retTech = null;
		if (((Cell)cell).getView() == View.ICON)
		{
			// in icons, if there is any artwork, use it
			if (useCount[TecArtwork.tech.getIndex()] > 0) return(TecArtwork.tech);

			// in icons, if there is nothing, presume artwork
			if (bestTech == null) return(TecArtwork.tech);

			// use artwork as a default
			retTech = TecArtwork.tech;
		} else if (((Cell)cell).getView() == View.SCHEMATIC)
		{
			// in schematic, if there are any schematic components, use it
			if (useCount[TecSchematics.tech.getIndex()] > 0) return(TecSchematics.tech);

			// in schematic, if there is nothing, presume schematic
			if (bestTech == null) return(TecSchematics.tech);

			// use schematic as a default
			retTech = TecSchematics.tech;
		} else
		{
			// use the current layout technology as the default
			retTech = curLayoutTech;
		}

		// if a layout technology was voted the most, return it
		if (bestLayoutTech != null) retTech = bestLayoutTech; else
		{
			// if any technology was voted the most, return it
			if (bestTech != null) retTech = bestTech; else
			{
//				// if this is an icon, presume the technology of its contents
//				cv = contentsview(cell);
//				if (cv != NONODEPROTO)
//				{
//					if (cv->tech == NOTECHNOLOGY)
//						cv->tech = whattech(cv);
//					retTech = cv->tech;
//				} else
//				{
//					// look at the contents of the sub-cells
//					foundicons = FALSE;
//					for(ni = cell->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//					{
//						np = ni->proto;
//						if (np == NONODEPROTO) continue;
//						if (np->primindex != 0) continue;
//
//						// ignore recursive references (showing icon in contents)
//						if (isiconof(np, cell)) continue;
//
//						// see if the cell has an icon
//						if (np->cellview == el_iconview) foundicons = TRUE;
//
//						// do not follow into another library
//						if (np->lib != cell->lib) continue;
//						onp = contentsview(np);
//						if (onp != NONODEPROTO) np = onp;
//						tech = whattech(np);
//						if (tech == gen_tech) continue;
//						retTech = tech;
//						break;
//					}
//					if (ni == NONODEINST)
//					{
//						// could not find instances that give information: were there icons?
//						if (foundicons) retTech = sch_tech;
//					}
//				}
			}
		}

		// give up and report the generic technology
		return retTech;
	}

	// *************************** ArcProtos ***************************

	/**
	 * get the PrimitiveArc with a particular name from this technology
	 */
	public PrimitiveArc findArcProto(String name)
	{
		for (int i = 0; i < arcs.size(); i++)
		{
			PrimitiveArc ap = (PrimitiveArc) arcs.get(i);
			if (ap.getProtoName().equalsIgnoreCase(name))
				return ap;
		}
		return null;
	}

	/**
	 * get an iterator over all of the PrimitiveArcs in this technology
	 */
	public Iterator getArcIterator()
	{
		return arcs.iterator();
	}

	// *************************** NodeProtos ***************************

	/**
	 * get the PrimitiveNode with a particular name from this technology
	 */
	public PrimitiveNode findNodeProto(String name)
	{
		for (int i = 0; i < nodes.size(); i++)
		{
			PrimitiveNode pn = (PrimitiveNode) nodes.get(i);
			if (pn.getProtoName().equalsIgnoreCase(name))
				return pn;
		}
		return null;
	}

	/**
	 * get an iterator over all of the PrimitiveNodes in this technology
	 */
	public Iterator getNodeIterator()
	{
		return nodes.iterator();
	}

}
