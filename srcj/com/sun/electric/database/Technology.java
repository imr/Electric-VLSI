package com.sun.electric.database;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A Technology object contains PrimitiveNodes and ArcProtos.  There may
 * be more than one Technology object.  To get a particular Technology,
 * use <code>Electric.findTechnology</code>
 */
public class Technology extends ElectricObject
{
	public class ArcLayer
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

	public class NodeLayer
	{
		private Layer layer;
		private int portNum;
		private int count;
		private Poly.Type style;
		private int representation;
		private EdgeH leftEdge;
		private EdgeV bottomEdge;
		private EdgeH rightEdge;
		private EdgeV topEdge;

		/** list of scalable points */		public static final int POINTS=     0;
		/** a rectangle */					public static final int BOX=        1;
		/** list of absolute points */		public static final int ABSPOINTS=  2;
		/** minimum sized rectangle */		public static final int MINBOX=     3;

		public NodeLayer(Layer layer, int portNum, int count, Poly.Type style, int representation,
			EdgeH leftEdge, EdgeV bottomEdge, EdgeH rightEdge, EdgeV topEdge)
		{
			this.layer = layer;
			this.portNum = portNum;
			this.count = count;
			this.style = style;
			this.representation = representation;
			this.leftEdge = leftEdge;
			this.bottomEdge = bottomEdge;
			this.rightEdge = rightEdge;
			this.topEdge = topEdge;
		}
		public Layer getLayer() { return layer; }
		public int getPortNum() { return portNum; }
		public int getCount() { return count; }
		public Poly.Type getStyle() { return style; }
		public int getRepresentation() { return representation; }
		public EdgeH getLeftEdge() { return leftEdge; }
		public EdgeV getBottomEdge() { return bottomEdge; }
		public EdgeH getRightEdge() { return rightEdge; }
		public EdgeV getTopEdge() { return topEdge; }
	}

	/** name of the technology */						private String techName;
	/** full description of the technology */			private String techDesc;
	/** critical dimensions for the technology */		private double scale;
	/** list of primitive nodes in the technology */	private List nodes;
	/** list of arcs in the technology */				private List arcs;

	/* static list of all Technologies in Electric */	private static List technologies = new ArrayList();
	/* the current technology in Electric */			private static Technology curTech = null;

	protected Technology()
	{
		this.nodes = new ArrayList();
		this.arcs = new ArrayList();
		scale = 1.0;

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

	void addArcProto(ArcProto ap)
	{
		arcs.add(ap);
	}

	/**
	 * Return the current Technology
	 */
	public static Technology getCurrent() { return curTech; }

	/**
	 * Set the current Technology
	 */
	public static void setCurrent(Technology tech) { curTech = tech; }

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
	
	public Poly getPoly(NodeInst ni, PrimitivePort pp)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		int [] specialValues = np.getSpecialValues();
		if (specialValues[0] == PrimitiveNode.SERPTRANS)
		{
			// serpentine transistors use a more complex port determination (tech_filltransport)
			Poly portpoly = new Poly(0, 0, 0, 0);
			return portpoly;
		} else
		{
			// standard port determination, see if there is outline information
			if (np.isHoldsOutline())
			{
				// outline may determinesthe port
				Poly portpoly = new Poly(1, 2, 3, 4);
				return portpoly;
			} else
			{
				// standard port computation
				double halfWidth = ni.getXSize() / 2;
				double lowX = ni.getX() - halfWidth;
				double highX = ni.getX() + halfWidth;
				double halfHeight = ni.getYSize() / 2;
				double lowY = ni.getY() - halfHeight;
				double highY = ni.getY() + halfHeight;
				
				double portLowX = ni.getX() + pp.getLeft().getMultiplier() * ni.getXSize() + pp.getLeft().getAdder();
				double portHighX = ni.getX() + pp.getRight().getMultiplier() * ni.getXSize() + pp.getRight().getAdder();
				double portLowY = ni.getY() + pp.getBottom().getMultiplier() * ni.getYSize() + pp.getBottom().getAdder();
				double portHighY = ni.getY() +pp.getTop().getMultiplier() * ni.getYSize() + pp.getTop().getAdder();
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

	public String toString()
	{
		return "Technology " + techName;
	}

	// *************************** ArcProtos ***************************

	/**
	 * get the ArcProto with a particular name from this technology
	 */
	public ArcProto findArcProto(String name)
	{
		for (int i = 0; i < arcs.size(); i++)
		{
			ArcProto ap = (ArcProto) arcs.get(i);
			if (ap.getProtoName().equalsIgnoreCase(name))
				return ap;
		}
		return null;
	}

	/**
	 * get an iterator over all of the ArcProtos in this technology
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
